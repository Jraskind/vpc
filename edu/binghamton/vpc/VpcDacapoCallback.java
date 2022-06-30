package edu.binghamton.vpc;

import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

public class VpcDacapoCallback extends Callback {
  private final SampleCollector collector = new SampleCollector();

  public VpcDacapoCallback(CommandLineArgs args) {
    super(args);
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
      System.out.println("dumping data");
      collector.dump();
      return false;
    } else {
      return true;
    }
  }
}
