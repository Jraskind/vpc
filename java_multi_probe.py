# !/usr/bin/python

import argparse
import os

from bcc import BPF, USDT

BPF_HEADER = """
#include <uapi/linux/ptrace.h>
#include <linux/types.h>
BPF_ARRAY(counts, u64, 400);

struct data_t {
    u32 pid;
    u64 ts;
    char comm[100];
};

BPF_PERF_OUTPUT(vm_shutdown);

int notify_shutdown(void *ctx) {
     struct data_t data = {};
     data.pid = bpf_get_current_pid_tgid();
     data.ts = bpf_ktime_get_ns();
     bpf_get_current_comm(&data.comm, sizeof(data.comm));
     vm_shutdown.perf_submit(ctx, &data, sizeof(data));
     return 0;
}
"""

BPF_PROBE_HOOK = """
BPF_PERF_OUTPUT(%s);

int notify_%s(void *ctx) {
    struct data_t data = {};
    data.pid = bpf_get_current_pid_tgid();
    data.ts = bpf_ktime_get_ns();
    bpf_get_current_comm(&data.comm, sizeof(data.comm));
    %s.perf_submit(ctx, &data, sizeof(data));
    return 0;
}
"""

IS_RUNNING = True
PROBE_DATA = []
DATA_HEADER = 'probe,event_time,sample_time'


def generate_probe_tracing_program(probes):
    return '\n'.join([BPF_HEADER] + [BPF_PROBE_HOOK % (x, x, x) for x in probes])


def shutdown_hook(output_path, cpu, data, size):
    with open(os.path.join(output_path, 'probes.csv'), 'w') as fp:
        fp.write('\n'.join([DATA_HEADER] + PROBE_DATA) + '\n')

    global IS_RUNNING
    IS_RUNNING = False


def tracing_hook(bpf, probe, cpu, data, size):
    event = bpf[probe].event(data)
    PROBE_DATA.append('%s,%d,%d' % (probe, event.ts, BPF.monotonic_time()))


def add_tracing_hook(bpf, probe):
    bpf[probe].open_perf_buffer(
        lambda cpu, data, size: tracing_hook(
            bpf,
            probe,
            cpu,
            data,
            size
        )
    )


def parse_args():
    parser = argparse.ArgumentParser(description='jvm probe tracer')
    parser.add_argument('-p', '--pid', type=int, help='java process to trace')
    parser.add_argument(
        '--probes',
        default='monitor__wait',
        type=str,
        help='jvm probes to trace'
    )
    parser.add_argument(
        '--output_directory',
        default='.',
        type=str,
        help='location to write the log'
    )

    return parser.parse_args()


def main():
    args = parse_args()

    probes = args.probes.split(',')

    usdt = USDT(pid=args.pid)
    usdt.enable_probe(probe='vm__shutdown', fn_name='notify_shutdown')
    for probe in probes:
        usdt.enable_probe(probe=probe, fn_name='notify_%s' % probe)

    code = generate_probe_tracing_program(args.probes.split(','))
    bpf = BPF(text=code, usdt_contexts=[usdt])
    bpf['vm_shutdown'].open_perf_buffer(lambda cpu, data, size: shutdown_hook(
        args.output_directory,
        cpu,
        data,
        size
    ))
    for probe in probes:
        add_tracing_hook(bpf, probe)

    while IS_RUNNING:
        bpf.perf_buffer_poll(timeout=1)


if __name__ == '__main__':
    main()
