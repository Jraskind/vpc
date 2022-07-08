import os
import sys

import pandas as pd

WRAP_AROUND_VALUE = 262143
BUCKET_SIZE_MS = 1
WARM_UP = 4


def maybe_apply_wrap_around(value):
    if value < 0:
        return value + WRAP_AROUND_VALUE
    else:
        return value


def align_probes(path, normalize_timestamp):
    energy = pd.read_csv(os.path.join(path, 'energy.csv'))
    energy['ts'] = normalize_timestamps(energy.timestamp)
    energy = energy.groupby(['ts']).min()

    df = energy.diff()
    d_t = df.timestamp / 10 ** 9
    d_e = df[[col for col in energy.columns if 'energy_component' in col]].apply(lambda s: s.map(maybe_apply_wrap_around))
    d_e = d_e.sum(axis=1)
    power = d_e / d_t
    power.name = 'power'

    try:
        probes = pd.read_csv(os.path.join(path, 'probes.csv'))
        probes['ts'] = normalize_timestamps(probes.event_time)
        probes = probes.groupby(['ts', 'probe']).event_time.count().unstack()

        df = pd.concat([probes, power], axis=1).sort_index().dropna(subset=['power'])
        df['probe_count'] = 0
        for col in df.columns:
            if col == 'power':
                continue
            df['probe_count'] = df[col].fillna(0).astype(int)

        return df[['power', 'probe_count']]
    except:
        print('no probing data found!')
        df = power.to_frame()
        df['probe_count'] = 0
        return df


def normalize_timestamps(timestamps, bucket_size=BUCKET_SIZE_MS):
    return timestamps // 10**6 // BUCKET_SIZE_MS


def main():
    if not os.path.exists('summary'):
        os.mkdir('summary')

    path = sys.argv[2]
    ref_summary = []
    for benchmark in os.listdir(path):
        if '_' in benchmark:
            ref_summary.append(pd.read_csv(
                os.path.join(path, benchmark, 'summary.csv')
            ).assign(benchmark=benchmark.split('_')[-1]))
    ref_summary = pd.concat(ref_summary)

    path = sys.argv[1]
    profile_summary = []
    for probe in os.listdir(path):
        for benchmark in os.listdir(os.path.join(path, probe)):
            if '_' in benchmark:
                print('{}-{}'.format(probe, benchmark))
                profile_summary.append(pd.read_csv(
                    os.path.join(path, probe, benchmark, 'summary.csv')
                ).assign(benchmark=benchmark.split('_')[-1], probe=probe))
                # TODO(timur): i hacked this in very quickly, we probably just want to compute the metrics right away
                if not os.path.exists('summary/{}_{}.csv'.format(benchmark.split('_')[-1], probe)):
                    df = align_probes(
                        os.path.join(path, probe, benchmark),
                        normalize_timestamps
                    )[['power', 'probe_count']].assign(benchmark=benchmark.split('_')[-1], probe=probe).reset_index()
                    df.to_csv('summary/{}_{}.csv'.format(benchmark.split('_')[-1], probe), index=False)

    profile_summary = pd.concat(profile_summary)

    ref_summary.to_csv('summary/reference.csv', index=False)
    profile_summary.to_csv('summary/summarized.csv', index=False)

    return


if __name__ == '__main__':
    main()
