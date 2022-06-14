package edu.binghamton.vpc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import jrapl.EnergyCheckUtils;
import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

public class VpcDacapoCallback extends Callback {
  private static long getMonotonicTimestamp() {
    Instant timestamp = Instant.now();
    return timestamp.getEpochSecond() * 1000000000 + timestamp.getNano();
  }

  private static long getEnergy() {
    double total = 0;
    for (double[] socket : EnergyCheckUtils.readEnergyStats()) {
      for (double energy : socket) {
        total += energy;
      }
    }
    return total;
  }

  private final ArrayList<Sample> samples = new ArrayList<>();

  private Sample lastSample;

  public VpcDacapoCallback(CommandLineArgs args) {
    super(args);
  }

  @Override
  public void start(String benchmark) {
    super.start(benchmark);
    lastSample = new Sample(iteration, getMonotonicTimestamp(), getEnergy());
  }

  @Override
  public void stop(long w) {
    super.stop(w);
    samples.add(
        new Sample(
            iteration,
            getMonotonicTimestamp() - lastSample.timestamp,
            getEnergy() - lastSample.energy));
  }

  @Override
  public boolean runAgain() {
    if (!super.runAgain()) {
      try {
        PrintWriter writer =
            new PrintWriter(new BufferedWriter(new FileWriter(String.format("energy.csv"))));
        writer.write("iteration,execution_time,energy");
        for (Sample sample : samples) {
          writer.writeln(
              String.format("%d,%d,%f", sample.iteration, sample.timestamp, sample.energy));
        }
        writer.close();
      } catch (IOException e) {
        System.out.println("Unable to write VPC energy data!");
        e.printStackTrace();
      }
      return false;
    } else {
      return true;
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
