#./generate_experiment.sh <experiment path> <java path> <# of iterations>
# We expect a file named "benchmarks" to exist within the given file path
# benchmarks should contain:
#    <benchmark suite letter> <benchmarks separated by commas> <probes>
# i.e. d h2,avrora,jython gc__begin,gc__end
exp_path="$1"
#java_path="$2"
java_path="/home/jraskin3/VPC/open-jdk/build/linux-x86_64-server-release/jdk/bin/java"
iters="$2"
python3 /home/jraskin3/timur_vpc/scripts/generate_multi_probe_experiment.py -exp_path $exp_path -iters $iters -java_path $java_path
