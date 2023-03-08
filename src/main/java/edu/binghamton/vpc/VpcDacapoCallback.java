package edu.binghamton.vpc;

import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

/** {@link Callback} for dacapo that wraps usage of the {@link SampleCollector}. */
public class VpcDacapoCallback extends Callback {
  private final SampleCollector collector = new SampleCollector();
  private final SampleCollectorPapi papiCollector = new SampleCollectorPapi();
    
  public VpcDacapoCallback(CommandLineArgs args) {
    super(args);
  }

  @Override
  public void start(String benchmark) {
    super.start(benchmark);
    collector.start();
    papiCollector.start();
  }

  @Override
  public void stop(long w) {
    super.stop(w);
    collector.stop();
    papiCollector.stop();
  }

  @Override
  public boolean runAgain() {
    // if we have run every iteration, dump the data and terminate
    if (!super.runAgain()) {
      System.out.println("dumping data");
      collector.dump();
      papiCollector.dump();
      return false;
    } else {
      return true;
    }
  }
}
