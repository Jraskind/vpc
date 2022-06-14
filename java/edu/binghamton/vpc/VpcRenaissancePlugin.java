package research.utils.renaissance;

import org.renaissance.Plugin;
import java.io.PrintWriter;
import research.utils.instrumentation.profiling.ProfilingAgent;
import java.lang.management.ManagementFactory;
import research.utils.perf.PerfUtils;
import research.utils.benchmarks.IterationCounter;

public class RenCallback implements Plugin.AfterBenchmarkSetUpListener,
       Plugin.BeforeBenchmarkTearDownListener,Plugin.AfterOperationSetUpListener, Plugin.BeforeOperationTearDownListener  {

    public static long start_ts;
    public static long end_ts;
    public static boolean READ_RAPL = false;
    public static int expid = 0;
    public static final int MAX_ITERS=50;

    public static IterationCounter iter_counter;

    public static void main(String[] args) {
        System.out.println("How Ya Do'In ?");
    }


    public void afterOperationSetUp(String benchmark, int opIndex, boolean isLastOp) {
        iter_counter.start_iter();
    }

    public  void beforeOperationTearDown(String benchmark, int opIndex, long durationNanos) {
        iter_counter.end_iter();
    }





    public static void init_profiling() {
        try {
    	    READ_RAPL = Boolean.parseBoolean(System.getenv("READ_RAPL"));
        } catch(Exception exception) {
		    exception.printStackTrace();
        }


        expid = Integer.parseInt(System.getProperty("expid"));
        System.out.println("READ_RAPL:" + READ_RAPL);
        iter_counter = new IterationCounter(expid);
        iter_counter.set_skip(5);
        if(READ_RAPL) {
	        ProfilingAgent.start_recording();
        }

    }




        public static void end_profiling() {
           iter_counter.write_summary();
           if(READ_RAPL) {
                ProfilingAgent.stop_recording(expid);
        }

    }


    public void afterBenchmarkSetUp(String benchmark) {
         System.out.println("[RenCallback] After Benchmark Setup :" + benchmark);
         init_profiling();
    }

    public void beforeBenchmarkTearDown(String benchmark) {
        System.out.println("Before Benchmark Teardown :" + benchmark);
        end_profiling();
    }



  public static void write_to_file(String file_name, String value) {
		try {
			PrintWriter writer = new PrintWriter(file_name);
			writer.println(value);
			writer.flush();
			writer.close();
		} catch(Exception exc) {
			exc.printStackTrace();
		}
   }
}
