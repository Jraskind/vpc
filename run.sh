#!/bin/bash

NATIVE_LIBRARY_PATH="${PWD}/bin"
LIBRARY_PATH="${PWD}/lib"
VPC_JAR="${PWD}/target/vpc-0.1.0-jar-with-dependencies.jar"

DATA_DIR=test-data
rm "${DATA_DIR}"
mkdir "${DATA_DIR}"

sudo LD_LIBRARY_PATH="/home/jraskin3/papi-java/.:${LD_LIBRARY_PATH}" \
  java -Dvpc.library.path="${NATIVE_LIBRARY_PATH}" \
      -Dvpc.output.directory="${DATA_DIR}" \
      -Dvpc.hpc.names="PAPI_SR_INS" \
      -cp "${VPC_JAR}" \
      Harness sunflow -n 1 \
      -c edu.binghamton.vpc.VpcDacapoCallback

sudo LD_LIBRARY_PATH="/home/jraskin3/papi-java/.:${LD_LIBRARY_PATH}" \
  java -Dvpc.library.path="${NATIVE_LIBRARY_PATH}" \
      -Dvpc.output.directory="${DATA_DIR}" \
      -Dvpc.hpc.names="PAPI_SR_INS" \
      -cp "${VPC_JAR}" \
      -jar "${LIBRARY_PATH}/renaissance-gpl-0.14.1.jar" \
      scrabble -r 1 \
      --plugin "${VPC_JAR}!edu.binghamton.vpc.VpcRenaissancePlugin"
