import os
import sys

import numpy as np
import pandas as pd

WRAP_AROUND_VALUE = 262143
BUCKET_SIZE_MS = 1
WARM_UP = 3


def maybe_apply_wrap_around(value):
    if value < 0:
        return value + WRAP_AROUND_VALUE
    else:
        return value


def align_probes(path, normalize_timestamps_fn=None, warm_up=WARM_UP):
    # expects header: iteration,timestamp,energy_component_0,energy_component_1
    # consult edu.binghamton.vpc.SampleCollector for more information
    energy = pd.read_csv(os.path.join(path, 'energy.csv'))
    energy = energy[energy.iteration > WARM_UP]
    if normalize_timestamps_fn is None:
        energy['ts'] = normalize_timestamps_fn(energy.timestamp)
    else:
        energy['ts'] = normalize_timestamps(energy.timestamp)
    energy = energy.groupby(['ts']).min()

    df = energy.diff()
    d_t = df.timestamp / 10 ** 9
    components = [col for col in energy.columns if 'energy_component' in col]
    d_e = df[components].apply(lambda s: s.map(maybe_apply_wrap_around))
    d_e = d_e.sum(axis=1)
    power = d_e / d_t
    power.name = 'power'

    # TODO: this is wrong; it doesn't synthesize probes
    probes = pd.read_csv(os.path.join(path, 'probes.csv'))
    probes['ts'] = normalize_timestamps(probes.event_time)
    probes = probes.groupby(['ts', 'probe']).event_time.count().unstack()

    df = pd.concat([probes, power], axis=1)

    return df.sort_index().dropna(subset=['power'])


def compute_metrics(aligned):
    aligned = aligned.copy(deep=True)
    aligned['probe_count'] = 0
    for col in aligned.columns:
        if col not in ('power', 'probe_count'):
            continue
        aligned['probe_count'] += aligned[col].fillna(0).astype(int)

    event_present = (aligned.probe_count > 0).sum()
    event_count = aligned.probe_count.sum()
    intervals = len(aligned)

    corr = aligned.corr()
    ratio = event_present / intervals
    rate = event_count / intervals
    acrate = event_count / event_present if event_present > 0 else np.nan
    np.seterr(divide='ignore')
    entropy = aligned.probe_count / event_count
    entropy = (- entropy * np.log(entropy)).sum()
    np.seterr(divide='warn')

    return [event_count, intervals, corr, ratio, rate, acrate, entropy]


def normalize_timestamps(timestamps, bucket_size=BUCKET_SIZE_MS):
    return timestamps // 10**6 // BUCKET_SIZE_MS


def main():
    path = sys.argv[2]

    baseline_summary = []
    for benchmark in os.listdir(path):
        if '_' in benchmark:
            bench = benchmark.split('_')[-1]

            df = pd.read_csv(os.path.join(path, benchmark, 'summary.csv'))
            df = df[df.iteration > WARM_UP]
            df['benchmark'] = bench
            baseline_summary.append(df)
    baseline_summary = pd.concat(baseline_summary)

    path = sys.argv[1]

    output_dir = os.path.join(path, 'processed')
    if not os.path.exists(output_dir):
        os.mkdir(output_dir)

    probing_summary = []
    metrics = []
    for case in os.listdir(path):
        for benchmark in os.listdir(os.path.join(path, case)):
            if '_' in benchmark:
                print('{}-{}'.format(case, benchmark))

                bench = benchmark.split('_')[-1]
                data_dir = os.path.join(path, case, benchmark)

                df = pd.read_csv(os.path.join(data_dir, 'summary.csv'))
                df = df[df.iteration > WARM_UP]
                df['benchmark'] = bench
                df['case'] = case
                probing_summary.append(df)

                if not os.path.exists(os.path.join(data_dir, 'energy.csv')):
                    print('no energy data found!')
                    continue

                if not os.path.exists(os.path.join(data_dir, 'probes.csv')):
                    print('no probing data found!')
                    continue

                if not os.path.exists(os.path.join(data_dir, 'aligned.csv')):
                    print('aligning probes')
                    df = align_probes(os.path.join(data_dir))
                    df.to_csv(os.path.join(data_dir, 'aligned.csv'))
                else:
                    print('loading aligned probe')
                    df = pd.read_csv(os.path.join(data_dir, 'aligned.csv'))
                metrics.append([bench, case] + compute_metrics(df))

    probing_summary = pd.concat(probing_summary)
    metrics = pd.DataFrame(
        metrics,
        columns=['benchmark', 'probe', 'events', 'intervals', 'pcc', 'ratio', 'rate', 'acrate', 'entropy']
    )

    ref = baseline_summary.copy(deep=True)
    ref.duration /= 10**9
    ref['power'] = ref.energy / ref.duration
    ref = ref.groupby('benchmark')[['duration', 'energy', 'power']].agg(('mean', 'std'))
    ref.columns = ref.columns.swaplevel()

    probe = probing_summary.copy(deep=True)
    probe.duration /= 10**9
    probe['power'] = probe.energy / probe.duration
    probe = probe.groupby(['probe', 'benchmark'])[['duration', 'energy', 'power']].agg(('mean', 'std'))
    probe.columns = probe.columns.swaplevel()

    overhead = 100 * (probe['mean'] / ref['mean'] - 1)
    overhead_err = 10 * np.sqrt((probe['std'] / probe['mean'])**2 + (ref['std'] / ref['mean'])**2)
    overhead_err.columns = [col + '_err' for col in overhead_err.columns]
    overhead = pd.concat([overhead, overhead_err], axis=1)

    baseline_summary.to_csv(os.path.join(output_dir, 'baseline.csv'), index=False)
    probing_summary.to_csv(os.path.join(output_dir, 'probing.csv'), index=False)
    metrics.to_csv(os.path.join(output_dir, 'metrics.csv'), index=False)
    overhead.to_csv(os.path.join(output_dir, 'overhead.csv'), index=False)


if __name__ == '__main__':
    main()
