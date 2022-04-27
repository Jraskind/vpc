package edu.binghamton.vpc;

import java.io.File;

public class PerfUtils {

  public static Object stats_synch = new Object();

  static {
    System.load(new File("java/edu/binghamton/vpc/libPerfUtils.so").getAbsolutePath());
  }

  /** ************************************************************************** */
  public static native double get_energy();

  public static native void timer_start_recording();

  public static native void timer_read_counters();

  public static native void start_recording();

  public static native void stop_recording();

  public static native void register_thread_stat(); // Synchronized

  public static native void read_counters_reset();

  public static native void init_papi_lib();

  public static native void set_energy_enabled(int flag);

  public static native void timer_init_events(String[] names);

  public static native void init_papi(String[] names);

  public static native void init_event_names(
      int length, byte[][] counter_bytes, int THREAD_COUNTER_NAMES);

  public static native void print_thread_counters();

  public static native void print_energy_counters();

  public static native void print_energy_counters_expid(int expid);

  public static native long get_timestamp();
  /** ************************************************************************** */
  public static void sync_register_stat() {
    synchronized (stats_synch) {
      register_thread_stat();
    }
  }

  public static void main(String[] args) {
    System.out.println("Just test calling the C functions");
    init_papi_lib();
    get_energy();
    System.out.println("Main ... PAPI Library Inialized");
  }
}
