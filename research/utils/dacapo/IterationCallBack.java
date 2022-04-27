package research.utils.dacapo;

import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;
import research.utils.benchmarks.IterationCounter;
import research.utils.instrumentation.profiling.ProfilingAgent;

public class IterationCallBack extends Callback {

  int iter = 0;
  public static int WARM_UP_ITERS = 2;
  public static int expid = 0;
  public static int TOTAL_ITERS = 10;
  public static String SCRIPT_CMD =
      "sudo python /home/kmahmou1/bcc_energy/java_multi_probes.py -p %d -probes %s";
  public static String DTRACE_EVENTS = null;
  public static boolean READ_RAPL = false;
  public static IterationCounter iter_counter;

  public static void vm_termination() {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              public void run() {
                if (READ_RAPL) {
                  ProfilingAgent.stop_recording(expid);
                }

                iter_counter.write_summary();
              }
            });
  }

  public IterationCallBack(CommandLineArgs args) {
    super(args);
    System.out.println("Thank you for your service to the country!");
    System.out.println("[IterationCallBack] Construction Start");
    IterationCallBack.expid = 1; // Integer.parseInt(System.getProperty("expid"));
    vm_termination();

    try {
      READ_RAPL = Boolean.parseBoolean(System.getenv("READ_RAPL"));
    } catch (Exception exception) {
      exception.printStackTrace();
    }

    System.out.println("Benchmark Parameters ........................");
    System.out.println(String.format("TOTAL_ITERS %d", TOTAL_ITERS));
    System.out.println(String.format("READ_RAPL %s", READ_RAPL));
    iter_counter = new IterationCounter(expid);
    iter_counter.set_skip(5);
    if (READ_RAPL) {
      ProfilingAgent.start_recording();
    }
  }

  @Override
  public void start(String benchmark) {
    super.start(benchmark);
    System.out.println("Completing");
    iter_counter.start_iter();
  }

  @Override
  public void complete(String benchmark, boolean valid) {
    System.out.println("Completing....");
    super.complete(benchmark, valid);
  };

  public void stop(long w) {
    super.stop(w);
    System.out.println("Completing....");
    iter_counter.end_iter();
  }
}
