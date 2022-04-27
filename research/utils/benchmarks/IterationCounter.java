package research.utils.benchmarks;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import research.utils.perf.PerfUtils;

public class IterationCounter {

  private int max_iterations = 50;

  private long[] start_times;
  private long[] end_times;

  private double[] start_energy;
  private double[] end_energy;

  private int current_iter = 0;
  private int expid = 0;
  private int skip = 5;

  public void set_skip(int skip) {
    this.skip = skip;
  }

  public IterationCounter(int expid) {
    this.expid = expid;
    start_times = new long[max_iterations];
    end_times = new long[max_iterations];
    start_energy = new double[max_iterations];
    end_energy = new double[max_iterations];
  }

  public void start_iter() {
    start_times[current_iter] = PerfUtils.get_timestamp();
    start_energy[current_iter] = PerfUtils.get_energy();
  }

  public void end_iter() {
    end_times[current_iter] = PerfUtils.get_timestamp();
    end_energy[current_iter] = PerfUtils.get_energy();
    current_iter++;
  }

  public void write_summary() {
    String id_name = ManagementFactory.getRuntimeMXBean().getName();
    int pid = Integer.parseInt(id_name.split("@")[0]);
    String execution_file_name = String.format("execution_time_%d_%d", expid, pid);
    String iteration_file_name = String.format("iteration_time_%d_%d", expid, pid);

    String energy_file_name = String.format("iteration_energy_%d_%d", expid, pid);
    StringBuffer iteration_times = new StringBuffer();
    StringBuffer iteration_energy = new StringBuffer();
    long execution_time = 0;
    int skip_index = skip - 1;

    for (int i = 0; i < current_iter; i++) {
      long duration = end_times[i] - start_times[i];

      String iteration_info = String.format("%d,%d \n", start_times[i], end_times[i]);
      String energy_info = String.format("%f,%f \n", start_energy[i], end_energy[i]);
      iteration_times.append(iteration_info);
      iteration_energy.append(energy_info);
      if (i > skip_index) {
        execution_time += duration;
      }
    }

    write_to_file(execution_file_name, Long.toString(execution_time));
    write_to_file(iteration_file_name, iteration_times.toString());
    write_to_file(energy_file_name, iteration_energy.toString());
  }

  public static void write_to_file(String file_name, String value) {
    try {
      PrintWriter writer = new PrintWriter(file_name);
      writer.println(value);
      writer.flush();
      writer.close();
    } catch (Exception exc) {
      exc.printStackTrace();
    }
  }
}
