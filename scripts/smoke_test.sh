# !/bin/bash

CURRENT_DIR=$(dirname $(realpath $0))/..

java -Dvpc.library.path="${CURRENT_DIR}"/bin -Dvpc.output.directory="${CURRENT_DIR}" \
  -cp lib/dacapo.jar:vpc.jar Harness sunflow -c edu.binghamton.vpc.VpcDacapoCallback

# TODO(timur): this is a hard path; we should direct it to the third_party built version
TRACEABLE_JAVA=/home/jraskin3/VPC/open-jdk/build/linux-x86_64-server-fastdebug/jdk/bin/java
$TRACEABLE_JAVA -XX:+ExtendedDTraceProbes -Dvpc.library.path="${CURRENT_DIR}"/bin -Dvpc.output.directory="${CURRENT_DIR}" \
  -cp lib/dacapo.jar:vpc.jar Harness sunflow -s small -n 2 -c edu.binghamton.vpc.VpcDacapoCallback &
python3 "${CURRENT_DIR}/scripts/java_multi_probe.py" --pid $! --probes='gc__begin,gc__end' --output_directory="${CURRENT_DIR}"
