package edu.binghamton.vpc;

import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import java.util.List;
// import papi.*;

/**
 * Data collector that produces both a runtime {@code Summary} and a online collection of
 * {@Samples}.
 */
public final class SampleCollectorPapi {
  /** Loads the data from the baseline and computes the mean runtime after {@code warmUp}. */
  // TODO: this doesn't report a missing baseline verbosely. should we fail if it's missing?
    // private ArrayList<String> names = new ArrayList<String>();
    // private ArrayList<Integer> papi_codes = new ArrayList<Integer>();


  // private final ArrayList<PapiSample> samples = new ArrayList<>();
  // private final ExecutorService executor;

  // private boolean isRunning = false;
  // private Future<ArrayList<PapiSample>> sampleFuture;
  // private int iteration = 0;

  public SampleCollectorPapi() { }
  //   executor = Executors.newSingleThreadExecutor();
  //   String[] hpc_names = System.getProperty("vpc.hpc.names").split(",");
  //   for(String name : hpc_names){
	// names.add(name);
	// papi_codes.add(Constants.EVENTS.get(name));
  //   }
  // }

  /**
   * Collects start data for a {@code Summary} and starts collecting {@code Samples} concurrently.
   */
  public void start() {
    // if(System.getProperty("vpc.hpc.names") == null){
	  //   //No PAPI HPCs to track
	  //   return;
    // }
    // isRunning = true;
    // Papi.init();
    // Papi.initMultiplex();
    
    
    // /*int[] COUNTERS_TEST = papi_codes.stream().mapToInt(i -> i).toArray();
		// EventSet future_evset_TEST = EventSet.createMultiplex(COUNTERS_TEST);
		// System.out.println("**************CAN CREATE!************");
		// future_evset_TEST.start();
		// System.out.println("**************CAN START!************");
		// future_evset_TEST.stop();
    // */
    // // TODO: it's possible we can wrap the data collection into a {@link Supplier}
    // sampleFuture =
    //     executor.submit(
    //         () -> {
		// ArrayList<PapiSample> samples = new ArrayList<>();
		// int[] COUNTERS = papi_codes.stream().mapToInt(i -> i).toArray();
		// EventSet future_evset = EventSet.createMultiplex(COUNTERS);
		// System.out.println("**************CAN CREATE!************");
		// future_evset.start();
		// System.out.println("**************CAN START!************");
	  //     int code_set = 0;
	  //     while (isRunning) {
		//   long[] data_arr = new long[names.size()];
		//   future_evset.addAndZero(data_arr);
		//   samples.add(new PapiSample(iteration, getMonotonicTimestamp(), data_arr));
		//   try {
		//       Thread.sleep(0,100000);
    //             } catch (Exception e) {
    //               System.out.println("couldn't sleep!");
    //               e.printStackTrace();
    //             }
    //           }
	  //     future_evset.stop();
    //           return samples;
    //         });
  }

  /** Collects end data for a {@code Summary} and stops collecting {@code Samples} concurrently. */
  public void stop() {
  //   isRunning = false;

  //   try {
	// samples.addAll(sampleFuture.get());
  //   } catch (Exception e) {
  //     System.out.println("couldn't get hpc data!");
  //     e.printStackTrace();
  //   }

  //   iteration++;
  }

  /** Writes the {@link Summary} and {@link Samples} to the underlying directory as csvs. */
  // TODO: do we want a different data type? what happens if we need more data like counters?
  public void dump() {
    // executor.shutdown();
    // try {
    //   PrintWriter writer =
    //       new PrintWriter(
    //           new BufferedWriter(
    //               new FileWriter(
    //                   String.join("/", System.getProperty("vpc.output.directory"), "hpc.csv"))));
    //   int papiCount = names.size();
    //   //TODO: Fix HPC Dump!
    //   writer.println(
    //       String.join(
    //           ",",
	  //     "hpc",
	  //     "events",
    //           "iteration",
    //           "timestamp"));
    //   for (PapiSample s : samples) {
	  // //writer.println(String.format("%s,%d,%d,%d", System.getProperty("vpc.hpc.names"),s.events[0],s.iteration,s.timestamp));
	  // for(int i = 0; i < papiCount;i++){
	  //   writer.println(String.format("%s,%d,%d,%d",names.get(i),s.events[i],s.iteration,s.timestamp));
	  // }
    //   }
    //   writer.close();
    // } catch (IOException e) {
    //   System.out.println("Unable to write VPC energy data!");
    //   e.printStackTrace();
    // }
  }

  /** {@link dumps} data from the collector and marks it as accepted or rejected. */
  public void dumpWithStatus(boolean accepted) {
    // dump();
    // dumpStatus(accepted);
  }

  /** Marks the data as accepted or rejected. */
  public void dumpStatus(boolean accepted) {
    // try {
    //   new File(
    //           String.join(
    //               "/",
    //               System.getProperty("vpc.output.directory"),
    //               accepted ? "accepted" : "rejected"))
    //       .createNewFile();
    // } catch (IOException e) {
    //   System.out.println("Unable to write VPC status!");
    //   e.printStackTrace();
    // }
  }


  // public List<PapiSample> getSamples() {
  //   return samples;
  // }

  // static class PapiSample {
  //   final int iteration;
  //   final long timestamp;
  //   final long[] events;

  //   private PapiSample(int iteration, long timestamp, long[] events) {
  //     this.iteration = iteration;
  //     this.timestamp = timestamp;
  //     this.events = events;
  //   }
  // }

  // // helpers methods that grab/handle data
  // private static long getMonotonicTimestamp() {
  //   return MonotonicTimestamp.getInstance(
  //           String.join("/", System.getProperty("vpc.library.path"), "libMonotonic.so"))
  //       .getMonotonicTimestamp();
  // }

}
