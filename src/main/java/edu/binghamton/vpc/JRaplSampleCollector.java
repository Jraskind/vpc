package edu.binghamton.vpc;

import static com.google.protobuf.util.Timestamps.fromMicros;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import jrapl.JRaplDifference;
import jrapl.JRaplSample;
import jrapl.Powercap;

public class JRaplSampleCollector {
  private static final MonotonicTimestamp MONOTONIC_CLOCK =
      MonotonicTimestamp.getInstance(
          String.join("/", System.getProperty("vpc.library.path"), "libMonotonic.so"));
  private final ArrayList<JRaplDifference> data = new ArrayList<>();
  private final ScheduledExecutorService executor =
      newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r, "jrapl-sampling-thread");
            t.setDaemon(true);
            return t;
          });

  private SamplingFuture<JRaplSample> samplingFuture;

  /** Starts collecting {@code JRaplSample}s concurrently. */
  public void start() {
    if (samplingFuture == null) {
      samplingFuture =
          SamplingFuture.fixedPeriodMillis(
              () ->
                  Powercap.sample().toBuilder()
                      .setTimestamp(fromMicros(MONOTONIC_CLOCK.getMonotonicTimestamp()))
                      .build(),
              1,
              executor);
    }
  }

  /** Stops collecting {@code JRaplSample}s concurrently. */
  public void stop() {
    if (samplingFuture != null) {
      List<JRaplSample> samples = samplingFuture.get();
      ArrayList<JRaplDifference> diffs = new ArrayList<>();
      Optional<JRaplSample> last = Optional.empty();
      for (JRaplSample sample : samples) {
        if (sample.equals(JRaplSample.getDefaultInstance())) {
          if (last.isPresent()) {
            diffs.add(Powercap.difference(last.get(), sample));
          }
          last = Optional.of(sample);
        }
      }
      samplingFuture = null;
      data.addAll(diffs);
    }
  }

  /** Returns the {@code JRaplSample}s. */
  public List<JRaplDifference> read() {
    return data;
  }
}
