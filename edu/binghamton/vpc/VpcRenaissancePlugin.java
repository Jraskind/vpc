package edu.binghamton.vpc;

import org.renaissance.Plugin;

public class VpcRenaissancePlugin
    implements Plugin.BeforeBenchmarkTearDownListener,
        Plugin.AfterOperationSetUpListener,
        Plugin.BeforeOperationTearDownListener {
  private final SampleCollector collector = new SampleCollector();

  @Override
  public void afterOperationSetUp(String benchmark, int opIndex, boolean isLastOp) {
    collector.start();
  }

  public void beforeOperationTearDown(String benchmark, int opIndex, long durationNanos) {
    collector.stop();
  }

  public void beforeBenchmarkTearDown(String benchmark) {
    collector.dump();
  }
}
