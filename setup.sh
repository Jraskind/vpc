#!/bin/bash

LIBRARY_PATH="${PWD}/lib"
if [ ! -d "${LIBRARY_PATH}" ]; then
  mkdir -p lib
fi

if [ ! -f "${LIBRARY_PATH}/dacapo.jar" ]; then
  wget https://sourceforge.net/projects/dacapobench/files/evaluation/dacapo-evaluation-git%2B309e1fa.jar/download
  mv download lib/dacapo.jar
fi

if [ ! -f "${LIBRARY_PATH}/renaissance-gpl-0.14.1.jar" ]; then
  wget https://github.com/renaissance-benchmarks/renaissance/releases/download/v0.14.1/renaissance-gpl-0.14.1.jar
  mv renaissance-gpl-0.14.1.jar lib/renaissance-gpl-0.14.1.jar
fi

# install jars as maven repos
mvn install:install-file               \
  -Dfile="${LIBRARY_PATH}/dacapo.jar"  \
  -DgroupId=org.dacapo                 \
  -DartifactId=dacapo                  \
  -Dversion=1.0.0                      \
  -Dpackaging=jar

mvn install:install-file                              \
  -Dfile="${LIBRARY_PATH}/renaissance-gpl-0.14.1.jar" \
  -DgroupId=org.renaissance                           \
  -DartifactId=renaissance                            \
  -Dversion=0.14.1                                    \
  -Dpackaging=jar

# need to add papi in a better way; this is a hard path atm
PAPI_JAR=/home/jraskin3/papi-java/papi.jar
mvn install:install-file               \
  -Dfile="${PAPI_JAR}"                 \
  -DgroupId=papi                       \
  -DartifactId=papi                    \
  -Dversion=1.0.0                      \
  -Dpackaging=jar
