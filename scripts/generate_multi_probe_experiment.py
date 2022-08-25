#!/usr/bin/python
#The idea here is to generate scripts that can be run later on in the pipeline
#Some things are still hard-coded for convenience, but should be changed later
#       i.e. library_path, script_path, dacapo_path, renaissance_path, renaissance_jar, when calling the renaissance jar, & the references to vpc.jar
import argparse
import os
import json
parser = argparse.ArgumentParser(description="Probes Parser", formatter_class=argparse.RawDescriptionHelpFormatter)
parser.add_argument("-iters","--iters", type=int,help="Number of iterations")
parser.add_argument("-exp_path","--exp_path", type=str,help="Path where benchmarks file is stored")
parser.add_argument("-java_path", "--java_path", type=str, help="Path to specified java version", default="java")
parser.set_defaults(verbose=False)
args = parser.parse_args()
exp_path = args.exp_path
java_path = args.java_path
iters = args.iters
launch_path = exp_path + "/launch/"
library_path = "/home/jraskin3/timur_vpc/bin/"
dacapo_path = "/home/jraskin3/timur_vpc/lib/dacapo.jar:/home/jraskin3/timur_vpc/vpc.jar"
renaissance_path = "/home/jraskin3/timur_vpc/lib/renaissance-gpl-0.14.1.jar:/home/jraskin3/timur_vpc/vpc.jar"
renaissance_jar = "/home/jraskin3/timur_vpc/lib/renaissance-gpl-0.14.1.jar"
script_path = "/home/jraskin3/timur_vpc/scripts/"
def create_dir(str_dir):
    try:
        os.mkdir(str_dir)
    except FileExistsError:
        print("Directory already exists...skipping creation")
    except:
        print("OS error! Could not create %s"%(str_dir))
        quit();

create_dir(launch_path)
with open(exp_path + "/benchmarks.json") as fp:
    #cluster_count and bench_count are used to enforce a naming convention for the experimental folders
    # <cluster #>_<bench #>_benchmark
    cluster_count = 0
    tests = json.load(fp)
    for test in tests:
        #    <benchmark suite letter> <benchmarks separated by commas> <probes>
        benchmark = test["benchmark"]
        probes = test["probes"]
        suite = test["suite"]
        if(suite == "dacapo"):
            size = test["size"]
        callback = test["callback"]
        filter_status = test["filter"]
        if(filter_status == "yes"):
            window = test["window"]
            variance = test["variance"]
            baseline_path = test["baseline_path"]
        output_file = open(launch_path + "cluster_" + str(cluster_count) + ".sh", "w")
        if(suite == "dacapo"):
            if(filter_status == "yes"):
                output_file.write(f'{java_path} -XX:+ExtendedDTraceProbes -Dvpc.library.path={library_path} -Dvpc.output.directory={exp_path}/{cluster_count}_{benchmark} -Dvpc.baseline.path={baseline_path} -cp {dacapo_path} Harness {benchmark} -s {size} -no-validation --iterations {iters} --window {window} --variance {variance} -c edu.binghamton.vpc.{callback} &\n')
            else:
                output_file.write(f'{java_path} -XX:+ExtendedDTraceProbes -Dvpc.library.path={library_path} -Dvpc.output.directory={exp_path}/{cluster_count}_{benchmark} -cp {dacapo_path} Harness {benchmark} -s {size} -no-validation --iterations {iters} -c edu.binghamton.vpc.{callback} &\n')
        else:
            if(filter_status == "yes"):
                output_file.write(f'{java_path} -XX:+ExtendedDTraceProbes -Dvpc.library.path={library_path} -Dvpc.output.directory={exp_path}/{cluster_count}_{benchmark} -Dvpc.baseline.path={baseline_path} -Dvpc.renaissance.args={iters},{window},{variance} -cp {renaissance_path} -jar {renaissance_jar} --plugin /home/jraskin3/timur_vpc/vpc.jar!edu.binghamton.vpc.{callback} --policy /home/jraskin3/timur_vpc/vpc.jar!edu.binghamton.vpc.{callback} {benchmark} &\n')
            else:
                output_file.write(f'{java_path} -XX:+ExtendedDTraceProbes -Dvpc.library.path={library_path} -Dvpc.output.directory={exp_path}/{cluster_count}_{benchmark} -cp {renaissance_path} -jar {renaissance_jar} -r {iters} --plugin /home/jraskin3/timur_vpc/vpc.jar!edu.binghamton.vpc.{callback} {benchmark} &\n')
        if probes != "none":
                output_file.write(f"python3 {script_path}java_multi_probe.py --pid $! --probes={probes} --output_directory={exp_path}/{cluster_count}_{benchmark} \n")
        else:
            output_file.write("tail --pid=$! -f /dev/null \n")
        create_dir("%s/%d_%s"%(exp_path,cluster_count,benchmark))
        cluster_count = cluster_count + 1
        output_file.close()
fp.close()
