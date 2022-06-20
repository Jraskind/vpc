package edu.binghamton.vpc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class SampleCollector {
  private static long getMonotonicTimestamp() {
    return MonotonicTimestamp.getInstance(
            String.join("/", System.getProperty("vpc.library.path"), "libMonotonic.so"))
        .getMonotonicTimestamp();
  }

  private static double getEnergy() {
    return Rapl.getInstance(String.join("/", System.getProperty("vpc.library.path"), "libRapl.so"))
        .getEnergy();
  }

  private final ArrayList<Summary> summary = new ArrayList<>();
  private final ArrayList<Sample> samples = new ArrayList<>();
  private final ExecutorService executor;

  private boolean isRunning = false;
  private Future<ArrayList<Sample>> sampleFuture;

  private Summary summaryStart;
  private int iteration = 0;

  public SampleCollector() {
    executor = Executors.newSingleThreadExecutor();
  }

  public void start() {
    isRunning = true;

    summaryStart = new Summary(iteration, getMonotonicTimestamp(), getEnergy());
    sampleFuture =
        executor.submit(
            () -> {
              ArrayList<Sample> samples = new ArrayList<>();
              while (isRunning) {
                samples.add(new Sample(iteration, getMonotonicTimestamp(), getEnergy()));
                try {
                  Thread.sleep(1);
                } catch (Exception e) {
                  System.out.println("couldn't sleep!");
                  e.printStackTrace();
                }
              }

              return samples;
            });
  }

  public void stop() {
    isRunning = false;

    summary.add(
        new Summary(
            iteration,
            summaryStart.timestamp,
            getMonotonicTimestamp() - summaryStart.timestamp,
            getEnergy() - summaryStart.energy));
    try {
      samples.addAll(sampleFuture.get());
    } catch (Exception e) {
      System.out.println("couldn't get energy data!");
      e.printStackTrace();
    }

    iteration++;
    summaryStart = null;
  }

  public void dump() {
    try {
      PrintWriter writer =
          new PrintWriter(
              new BufferedWriter(
                  new FileWriter(
                      String.join(
                          "/", System.getProperty("vpc.output.directory"), "summary.csv"))));
      writer.println("iteration,timestamp,duration,energy");
      for (Summary s : summary) {
        writer.println(
            String.format("%d,%d,%d,%f", s.iteration, s.timestamp, s.duration, s.energy));
      }
      writer.close();
    } catch (IOException e) {
      System.out.println("Unable to write VPC summary data!");
      e.printStackTrace();
    }

    try {
      PrintWriter writer =
          new PrintWriter(
              new BufferedWriter(
                  new FileWriter(
                      String.join("/", System.getProperty("vpc.output.directory"), "energy.csv"))));
      writer.println("iteration,timestamp,energy");
      for (Sample s : samples) {
        writer.println(String.format("%d,%d,%f", s.iteration, s.timestamp, s.energy));
      }
      writer.close();
    } catch (IOException e) {
      System.out.println("Unable to write VPC energy data!");
      e.printStackTrace();
    }
  }

  // TODO(timur): this is a poor practice; we should use something like a builder.
  private static class Summary {
    private final int iteration;
    private final long timestamp;
    private final long duration;
    private final double energy;

    private Summary(int iteration, long timestamp, double energy) {
      this.iteration = iteration;
      this.timestamp = timestamp;
      this.duration = 0;
      this.energy = energy;
    }

    private Summary(int iteration, long timestamp, long duration, double energy) {
      this.iteration = iteration;
      this.timestamp = timestamp;
      this.duration = duration;
      this.energy = energy;
    }
  }

  private static class Sample {
    private final int iteration;
    private final long timestamp;
    private final double energy;

    private Sample(int iteration, long timestamp, double energy) {
      this.iteration = iteration;
      this.timestamp = timestamp;
      this.energy = energy;
    }
  }
}
