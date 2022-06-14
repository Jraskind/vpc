# !/bin/bash

java -cp dacapo.jar:third_party/jrapl/jrapl.jar:java/edu/binghamton/vpc/VpcCallback.class Harness sunflow -c edu.binghamton.vpc.VpcCallback &
python3 java_multi_probe.py --pid $! --probes='gc__begin,gc__end'
