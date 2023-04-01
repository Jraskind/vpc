package edu.binghamton.vpc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import jrapl.JRaplUtils;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;

public class JRaplProfiler implements ExternalProfiler {
  private JRaplSampleCollector collector = new JRaplSampleCollector();

  public JRaplProfiler() {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              @Override
              public void run() {
                try (PrintWriter writer =
                    new PrintWriter(
                        new BufferedWriter(
                            new FileWriter(
                                String.join(
                                    "/",
                                    System.getProperty("vpc.output.directory"),
                                    "energy.csv"))))) {
                  writer.println(JRaplUtils.diffsToCsv(collector.read()));
                } catch (IOException e) {
                  System.out.println("Unable to write energy data!");
                  e.printStackTrace();
                }
              }
            });
  }

  /** Starts the collector. */
  @Override
  public final void beforeTrial(BenchmarkParams benchmarkParams) {
    collector.start();
  }

  /** Stops the collector. */
  @Override
  public final Collection<? extends Result> afterTrial(
      BenchmarkResult br, long pid, File stdOut, File stdErr) {
    collector.stop();
    return Collections.emptyList();
  }

  // Default implementations for ExternalProfiler interface
  @Override
  public String getDescription() {
    return getClass().getSimpleName();
  }

  @Override
  public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
    return Collections.emptyList();
  }

  @Override
  public Collection<String> addJVMOptions(BenchmarkParams params) {
    return Collections.emptyList();
  }

  @Override
  public boolean allowPrintOut() {
    return true;
  }

  @Override
  public boolean allowPrintErr() {
    return false;
  }
}
