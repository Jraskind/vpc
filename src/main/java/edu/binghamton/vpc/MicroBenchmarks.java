package edu.binghamton.vpc;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Fork(value = 1)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 1, time = 1)
@State(Scope.Benchmark)
public class MicroBenchmarks {
  private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
  private static final int[] NUMBERS = new Random().ints(1000000).toArray();

  private static int fibHelper(int n) {
    if (n == 0 || n == 1) {
      return n;
    } else {
      return fibHelper(n - 1) + fibHelper(n - 2);
    }
  }

  private static void dispatchWorkload(Runnable workload) throws Exception {
    ExecutorService executor = newFixedThreadPool(CPU_COUNT);
    for (int i = 0; i < CPU_COUNT; i++) {
      executor.execute(workload);
    }
    executor.shutdown();
    while (!executor.awaitTermination(1, MILLISECONDS)) {}
  }

  @Benchmark
  public void sleep() throws Exception {
    dispatchWorkload(
        () -> {
          try {
            MILLISECONDS.sleep(100);
          } catch (Exception e) {
          }
        });
  }

  @Benchmark
  public void div() throws Exception {
    dispatchWorkload(
        () -> {
          for (int i = 0; i < 2000000; i++) {
            Blackhole.consumeCPU(i / (i + 1));
          }
        });
  }

  @Benchmark
  public void sort() throws Exception {
    dispatchWorkload(() -> Arrays.sort(NUMBERS));
  }

  @Benchmark
  public void fib() throws Exception {
    dispatchWorkload(() -> Blackhole.consumeCPU(fibHelper(20)));
  }
}
