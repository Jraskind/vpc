#!/usr/bin/python
#The idea here is to generate scripts that can be run later on in the pipeline
#Some things are still hard-coded for convenience, but should be changed later
#       i.e. library_path, script_path, dacapo_path, renaissance_path, renaissance_jar, when calling the renaissance jar, & the references to vpc.jar
import argparse
import os
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
with open(exp_path + "/benchmarks") as fp:
    #cluster_count and bench_count are used to enforce a naming convention for the experimental folders
    # <cluster #>_<bench #>_benchmark
    cluster_count = 0
    line = fp.readline()
    while(line and line != "\n"):
        line_segments = line.split(" ")
        #    <benchmark suite letter> <benchmarks separated by commas> <probes>
        benchmark_list = line_segments[1]
        probes_list = line_segments[2].strip()
        is_dacapo = (line_segments[0] == 'd')
        output_file = open(launch_path + "cluster_" + str(cluster_count) + ".sh", "w")
        bench_count = 0
        for benchmark in benchmark_list.split(","):
            if(is_dacapo):
                #Dacapo benchmarks need the dacapo_jars
                output_file.write("%s -XX:+ExtendedDTraceProbes -Dvpc.library.path=%s -Dvpc.output.directory=%s/%d_%d_%s -cp %s Harness %s -s small -no-validation --iterations %d -c edu.binghamton.vpc.VpcDacapoCallback &\n"%(java_path,library_path,exp_path,cluster_count,bench_count,benchmark,dacapo_path,benchmark, iters))
            else:
                #Renaissance benchmarks need the ren_jars
                output_file.write("%s -XX:+ExtendedDTraceProbes -Dvpc.library.path=%s -Dvpc.output.directory=%s/%d_%d_%s -cp %s -jar %s -r %d --plugin /home/jraskin3/timur_vpc/vpc.jar!edu.binghamton.vpc.VpcRenaissancePlugin %s &\n"%(java_path,library_path,exp_path,cluster_count,bench_count,benchmark,renaissance_path, renaissance_jar, iters, benchmark))
            #Both Dacapo & Renaissance have the same java_multi_probes step 
            if probes_list != "none":
                output_file.write("python3 %sjava_multi_probe.py --pid $! --probes=%s --output_directory=%s/%d_%d_%s \n"%(script_path,probes_list,exp_path,cluster_count,bench_count,benchmark))
            else:
                output_file.write("tail --pid=$! -f /dev/null \n")
            create_dir("%s/%d_%d_%s"%(exp_path,cluster_count,bench_count,benchmark))
            bench_count = bench_count + 1
        cluster_count = cluster_count + 1
        output_file.close()
        line = fp.readline()
