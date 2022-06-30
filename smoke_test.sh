# !/bin/bash

BENCHMARK=sunflow
ITERATIONS=6
SIZE=small
WINDOW=2
VARIANCE=100.0

CURRENT_DIR=$(dirname $(realpath $0))
rm -r "${CURRENT_DIR}"/data
mkdir -p "${CURRENT_DIR}"/data
mkdir -p "${CURRENT_DIR}"/data/dacapo

DATA_DIR="${CURRENT_DIR}"/data/dacapo/reference
mkdir -p "${DATA_DIR}"
java -Dvpc.library.path="${CURRENT_DIR}"/bin -Dvpc.output.directory="${DATA_DIR}" \
-cp lib/dacapo.jar:vpc.jar Harness "${BENCHMARK}" -n "${ITERATIONS}" -s "${SIZE}" -c edu.binghamton.vpc.VpcDacapoCallback

REF_DIR="${DATA_DIR}"
DATA_DIR="${CURRENT_DIR}"/data/dacapo/filtered
mkdir -p "${DATA_DIR}"
java -Dvpc.library.path="${CURRENT_DIR}"/bin -Dvpc.output.directory="${DATA_DIR}" -Dvpc.baseline.path="${REF_DIR}" \
-cp lib/dacapo.jar:vpc.jar Harness "${BENCHMARK}" -n "${ITERATIONS}" -s "${SIZE}" --window "${WINDOW}" --variance "${VARIANCE}" -c edu.binghamton.vpc.FilteringVpcDacapoCallback

# TODO(timur): this is a hard path; we should direct it to the third_party built version
# TRACEABLE_JAVA=/home/jraskin3/VPC/open-jdk/build/linux-x86_64-server-fastdebug/jdk/bin/java
# $TRACEABLE_JAVA -Dvpc.library.path="${CURRENT_DIR}"/bin -Dvpc.output.directory="${CURRENT_DIR}" \
#   -cp lib/dacapo.jar:vpc.jar Harness sunflow -n 2 -c edu.binghamton.vpc.VpcDacapoCallback &
# python3 java_multi_probe.py --pid $! --probes='gc__begin,gc__end' --output_directory="${CURRENT_DIR}"

# java -Dvpc.library.path="${CURRENT_DIR}"/bin -Dvpc.output.directory="${CURRENT_DIR}" \
#   -cp lib/renaissance-gpl-0.11.0.jar:vpc.jar -jar lib/renaissance-gpl-0.11.0.jar --plugin vpc.jar!edu.binghamton.vpc.VpcRenaissancePlugin -r 2 scrabble #&
# python3 java_multi_probe.py --pid $! --probes='gc__begin,gc__end' --output_directory="${CURRENT_DIR}"

mkdir -p "${CURRENT_DIR}"/data/renaissance

DATA_DIR="${CURRENT_DIR}"/data/renaissance/reference
mkdir -p "${DATA_DIR}"
java -Dvpc.library.path="${CURRENT_DIR}"/bin -Dvpc.output.directory="${CURRENT_DIR}"/data/reference  -Dvpc.baseline.path="${REF_DIR}" \
  -cp lib/renaissance-gpl-0.11.0.jar:vpc.jar -jar lib/renaissance-gpl-0.11.0.jar \
  --plugin vpc.jar!edu.binghamton.vpc.VpcRenaissancePlugin \
  -r "${ITERATIONS}" \
  scrabble #&

REF_DIR="${DATA_DIR}"
DATA_DIR="${CURRENT_DIR}"/data/renaissance/filtered
mkdir -p "${DATA_DIR}"
java -Dvpc.library.path="${CURRENT_DIR}"/bin -Dvpc.output.directory="${CURRENT_DIR}"/data/filtered  -Dvpc.baseline.path="${REF_DIR}" \
  -Dvpc.renaissance.args="${ITERATIONS},${WINDOW},${VARIANCE}" \
  -cp lib/renaissance-gpl-0.11.0.jar:vpc.jar -jar lib/renaissance-gpl-0.11.0.jar \
  --plugin vpc.jar!edu.binghamton.vpc.FilteringVpcRenaissancePlugin \
  --policy vpc.jar!edu.binghamton.vpc.FilteringVpcRenaissancePlugin \
  scrabble #&
