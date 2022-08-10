# !/usr/bin/python

import argparse
import os

import numpy as np
import pandas as pd

# default values for the experiments run on jolteon
WRAP_AROUND_VALUE = 262143
WARM_UP = 5
BUCKET_SIZE_MS = 1


def maybe_apply_wrap_around(value):
    ''' applies the rapl wrap around if a diff is negative '''
    if value < 0:
        return value + WRAP_AROUND_VALUE
    else:
        return value


def normalize_timestamps(timestamps, bucket_size_ms=BUCKET_SIZE_MS):
    ''' normalizes ns timestamps to ms-bucketed timestamps '''
    # TODO: this is producing strange behavior due to int division:
    #   2938450289096200 // 10**6 = 2938450288
    return timestamps // 10**6 // bucket_size_ms


def samples_to_power(samples, normalize_timestamps_fn=None):
    ''' computes the power of each ts bucket '''
    samples = samples.copy(deep=True)
    if normalize_timestamps_fn is not None:
        samples['ts'] = normalize_timestamps_fn(samples.timestamp)
    else:
        samples['ts'] = normalize_timestamps(samples.timestamp)
    # TODO: i've prefered interval beginning. is there a best practice?
    samples = samples.groupby(['ts']).min()

    df = samples.diff()
    d_t = df.timestamp / 10 ** 9
    components = [col for col in samples.columns if 'energy_component' in col]
    d_e = df[components].apply(lambda s: s.map(maybe_apply_wrap_around))
    d_e = d_e.sum(axis=1)
    power = d_e / d_t
    power.name = 'power'

    return power


def bucket_probes(probes, normalize_timestamps_fn=None):
    ''' sums the number of probe events for each probe into ts buckets '''
    probes = probes.copy(deep=True)
    if normalize_timestamps_fn is not None:
        probes['ts'] = normalize_timestamps_fn(probes.event_time)
    else:
        probes['ts'] = normalize_timestamps(probes.event_time)
    probes = probes.groupby(['ts', 'probe']).event_time.count()
    probes.name = 'events'

    return probes


# TODO: the synthesis and metric computations are a little crude
def synthesize_probes(probes, normalize_timestamps_fn=None, debug=False):
    # diff will do begin - end, so we have to flip things a little bit
    # "events" is the synthesized depth column, so "no_synth" is the raw count for the bucket
    ret_probes = pd.DataFrame(columns=["events", "no_synth"])
    probes = probes.unstack(fill_value=0)
    if(len(probes.columns) == 1):
        #Don't synthesize, single probe
        ret_probes.events = probes[probes.columns[0]]
        ret_probes.no_synth = probes[probes.columns[0]]
    else:
        depth_probes = pd.DataFrame(columns=["depth"])
        #there may be missing probes events, so the output might be strange
        depth_probes["depth"] = -probes.cumsum().diff(axis=1).iloc[:, -1]
        depth_probes.loc[depth_probes.depth == 0, 'depth'] = probes.min(axis=1)
        ret_probes.events = depth_probes["depth"]
        ret_probes.no_synth = probes[probes.columns[0]] + probes[probes.columns[1]]
        #probes = probes - probes.min()
    if debug == True:
        print(ret_probes)
    return ret_probes


def compute_metrics(aligned):
    present = (aligned.events > 0).sum()
    events = aligned.no_synth.sum()
    intervals = len(aligned)
    if events == 0:
        return [np.nan, 0, 0, np.nan]

    corr = aligned.corr().iloc[0, 1]
    ratio = present / intervals
    rate = events / intervals
    entropy = aligned.events - aligned.events.min() + 1
    entropy = entropy / events
    entropy = -entropy * np.log(entropy)
    entropy = entropy.sum()

    return [corr, ratio, rate, entropy]


def norm_with_buckets(bucket_size_ms=BUCKET_SIZE_MS):
    return lambda timestamps: normalize_timestamps(timestamps, bucket_size_ms)


def parse_args():
    parser = argparse.ArgumentParser(description='jvm probe tracer')
    parser.add_argument('data', type=str, help='path to probing data')
    parser.add_argument(
        '--warm_up',
        type=int,
        default=WARM_UP,
        help='number of warm ups',
    )
    parser.add_argument(
        '--bucket',
        type=int,
        default=BUCKET_SIZE_MS,
        help='size of the ms timestamp buckets',
    )
    parser.add_argument(
        '--output_directory',
        default=None,
        help='location to write the summary and metrics'
    )

    args = parser.parse_args()
    if args.output_directory is None:
        args.output_directory = os.path.join(args.data)

    return args


def main():
    args = parse_args()

    if os.path.exists(os.path.join(args.data, 'summary.csv')):
        print('checking overhead summary for valid probes')
        summary = pd.read_csv(os.path.join(args.data, 'summary.csv'))

        s = summary.groupby('probe')[['duration', 'energy', 'has_data']].mean()
        mask = s.duration > 10
        mask |= s.energy > 10
        mask |= s.has_data == 0
        probes = list(s[~mask].index)
    else:
        print('no summary found, defaulting to all probes')
        probes = list(filter(lambda f: os.path.isdir(os.path.join(args.data, f)), os.listdir(args.data)))

    print('accepted probes:')
    print(probes)

    metrics = []
    for probe in probes:
        print('aligning data and computing metrics for {}'.format(probe))
        for benchmark in os.listdir(os.path.join(args.data, probe)):
            if '_' not in benchmark:
                continue
            bench = benchmark.split('_')[-1]
            data_dir = os.path.join(args.data, probe, benchmark)

            # TODO: we should be able handle all the potential failures but
            #       i got frustrated
            try:
                probes = bucket_probes(
                    pd.read_csv(os.path.join(data_dir, 'probes.csv')),
                    norm_with_buckets(args.bucket),
                )
                if probes.sum() > 0:
                    probes = synthesize_probes(probes)
                    df = pd.read_csv(os.path.join(data_dir, 'energy.csv'))
                    df = df[df.iteration > args.warm_up]
                    power = samples_to_power(df, norm_with_buckets(args.bucket))

                    # merge the power and probes along the timestamp and drop
                    # all records that have no power and are before the first
                    # recorded probe event
                    df = pd.concat([power, probes], axis=1).sort_index()
                    df = df.dropna(subset=['power']).ffill().dropna()
                    df.events = df.events.astype(int)
                    metrics.append([probe, bench] + compute_metrics(df))
                else:
                    print('no probe events for {}!'.format(benchmark))
            except KeyboardInterrupt:
                return
            except e:
                print('error handling data for {}!'.format(benchmark))
                print(e)


    metrics = pd.DataFrame(
        metrics,
        columns=['probe', 'benchmark', 'pcc', 'ratio', 'rate', 'entropy']
    ).set_index(['probe', 'benchmark']).sort_index()

    if not os.path.exists(args.output_directory):
        os.mkdir(args.output_directory)
    metrics.to_csv(os.path.join(args.output_directory, 'metrics.csv'))

    print('wrote metric data to {}'.format(
        os.path.join(args.output_directory, 'metrics.csv')))
    print(metrics.groupby('probe').mean())
    print(metrics.sort_values(by=['rate'], ascending=False).head(50))


if __name__ == '__main__':
    main()
