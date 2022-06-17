package edu.binghamton.vpc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

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

  private final ArrayList<Sample> samples = new ArrayList<>();

  private Sample lastSample;
  private int iteration = 0;

  public SampleCollector() {}

  public void start() {
    lastSample = new Sample(iteration, getMonotonicTimestamp(), getEnergy());
  }

  public void stop() {
    samples.add(
        new Sample(
            iteration,
            lastSample.timestamp,
            getMonotonicTimestamp() - lastSample.timestamp,
            getEnergy() - lastSample.energy));
    iteration++;
    lastSample = null;
  }

  public void dump() {
    try {
      PrintWriter writer =
          new PrintWriter(
              new BufferedWriter(
                  new FileWriter(
                      String.join("/", System.getProperty("vpc.output.directory"), "energy.csv"))));
      writer.write("iteration,timestamp,duration,energy");
      for (Sample sample : samples) {
        writer.write(
            String.format(
                "%d,%d,%f%f", sample.iteration, sample.timestamp, sample.duration, sample.energy));
      }
      writer.close();
    } catch (IOException e) {
      System.out.println("Unable to write VPC energy data!");
      e.printStackTrace();
    }
  }

  // TODO(timur): this is a poor practice; we should use something like a builder.
  private static class Sample {
    private final int iteration;
    private final long timestamp;
    private final long duration;
    private final double energy;

    private Sample(int iteration, long timestamp, double energy) {
      this.iteration = iteration;
      this.timestamp = timestamp;
      this.duration = 0;
      this.energy = energy;
    }

    private Sample(int iteration, long timestamp, long duration, double energy) {
      this.iteration = iteration;
      this.timestamp = timestamp;
      this.duration = duration;
      this.energy = energy;
    }
  }
}
