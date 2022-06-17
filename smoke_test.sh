# !/bin/bash

CURRENT_DIR=$(dirname $0)

java -Dvpc.library.path="${CURRENT_DIR}"/bin -cp lib/dacapo.jar:vpc.jar Harness sunflow -c edu.binghamton.vpc.VpcDacapoCallback

# java -cp lib/dacapo.jar:vpc.jar Harness sunflow -c edu.binghamton.vpc.VpcCallback &
# python3 java_multi_probe.py --pid $! --probes='gc__begin,gc__end'
