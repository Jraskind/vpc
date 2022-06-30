package edu.binghamton.vpc;

import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

public class FilteringVpcDacapoCallback extends Callback {
  private final SampleCollector collector = new SampleCollector();
  private final int batchSize;
  private final double threshold;
  private final double baselineRuntime;

  public FilteringVpcDacapoCallback(CommandLineArgs args) throws Exception {
    super(args);
    batchSize = args.getWindow();
    threshold = args.getTargetVar();
    baselineRuntime = SampleCollector.getBaselineRuntime(batchSize);
  }

  @Override
  public void start(String benchmark) {
    super.start(benchmark);
    collector.start();
  }

  @Override
  public void stop(long w) {
    super.stop(w);
    collector.stop();
  }

  @Override
  public boolean runAgain() {
    if (!super.runAgain()) {
      collector.dumpWithStatus(true);
      return false;
    } else if (iterations % batchSize == 0 && iterations > batchSize) {
      // filter by metric
      double runtime =
          collector
              .getSummary()
              .stream()
              .skip(batchSize)
              .mapToDouble(s -> s.duration)
              .average()
              .orElse(0);
      if (runtime / baselineRuntime > threshold) {
        System.out.println(
            String.format(
                "last %d runs exceeded threshold (%f / %f = %f > %f)",
                iterations, runtime, baselineRuntime, runtime / baselineRuntime, threshold));
        collector.dumpWithStatus(false);
        return false;
      }
    }

    return true;
  }
}
