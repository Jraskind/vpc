# !/usr/bin/python

import argparse
import os

import numpy as np
import pandas as pd

# default values for the experiments run on jolteon
WARM_UP = 3


def compute_overhead(baseline, experiment):
    ''' computes the time, energy, and power diff of the base and probes '''
    ref = baseline.copy(deep=True)
    ref.duration /= 10**9
    ref['power'] = ref.energy / ref.duration
    ref = ref.groupby('benchmark')[['duration', 'energy', 'power']].agg(('mean', 'std'))
    ref.columns = ref.columns.swaplevel()

    probe = experiment.copy(deep=True)
    probe.duration /= 10**9
    probe['power'] = probe.energy / probe.duration
    probe = probe.groupby(['probe', 'benchmark'])[['duration', 'energy', 'power']].agg(('mean', 'std'))
    probe.columns = probe.columns.swaplevel()

    has_data = experiment.groupby(['probe', 'benchmark']).has_data.max()

    overhead = 100 * (probe['mean'] / ref['mean'] - 1)
    overhead_err = 10 * np.sqrt((probe['std'] / probe['mean'])**2 + (ref['std'] / ref['mean'])**2)
    overhead_err.columns = [col + '_err' for col in overhead_err.columns]
    overhead = pd.concat([overhead, overhead_err, has_data], axis=1)

    return overhead


def is_valid_file(file):
    ''' makes sure that a file is a csv with at least one record '''
    return os.path.exists(file) and \
        os.path.isfile(file) and \
        os.path.getsize(file) > 0 and \
        len(pd.read_csv(file, nrows=1)) > 0


def parse_args():
    parser = argparse.ArgumentParser(description='jvm probe tracer')
    parser.add_argument('data', type=str, help='path to probing data')
    parser.add_argument('ref', type=str, help='path to reference data')
    parser.add_argument(
        '--warm_up',
        type=int,
        default=WARM_UP,
        help='number of warm ups',
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

    print('loading reference data')
    baseline_summary = []
    for benchmark in os.listdir(args.ref):
        if '_' not in benchmark:
            continue
        bench = benchmark.split('_')[-1]

        df = pd.read_csv(os.path.join(args.ref, benchmark, 'summary.csv'))
        df = df[df.iteration > args.warm_up]
        df['benchmark'] = bench
        baseline_summary.append(df)

    baseline_summary = pd.concat(baseline_summary)

    probing_summary = []
    for probe in os.listdir(args.data):
        if os.path.isfile(os.path.join(args.data, probe)):
            continue
        print('computing overhead for ' + probe)
        for benchmark in os.listdir(os.path.join(args.data, probe)):
            if '_' not in benchmark:
                continue
            bench = benchmark.split('_')[-1]
            data_dir = os.path.join(args.data, probe, benchmark)

            df = pd.read_csv(os.path.join(data_dir, 'summary.csv'))
            df = df[df.iteration > args.warm_up]
            df['benchmark'] = bench
            df['probe'] = probe

            if os.path.exists(os.path.join(data_dir, 'rejected')):
                print('{} was rejected!'.format(benchmark))
                df['has_data'] = False
            elif not is_valid_file(os.path.join(data_dir, 'energy.csv')):
                print('no energy data found for {}!'.format(benchmark))
                df['has_data'] = False
            elif not is_valid_file(os.path.join(data_dir, 'probes.csv')):
                print('no probing data found for {}!'.format(benchmark))
                df['has_data'] = False
            else:
                df['has_data'] = True

            probing_summary.append(df)

    probing_summary = pd.concat(probing_summary)
    summary = compute_overhead(baseline_summary, probing_summary)
    has_data = probing_summary.groupby(['probe', 'benchmark']).has_data.max()
    summary['has_data'] = has_data

    if not os.path.exists(args.output_directory):
        os.mkdir(args.output_directory)
    summary.to_csv(os.path.join(args.output_directory, 'summary.csv'))

    print('wrote overhead summary data to {}'.format(
        os.path.join(args.output_directory, 'summary.csv')))
    print(summary.groupby('probe')[['duration', 'energy']].mean().sort_values(
        by='duration', ascending=False).head(50))


if __name__ == '__main__':
    main()
