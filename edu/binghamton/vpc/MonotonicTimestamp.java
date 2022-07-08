package edu.binghamton.vpc;

/** Class that provides a singleton interface to c's monotonic timestamp. */
public final class MonotonicTimestamp {
  private static MonotonicTimestamp instance;

  public static MonotonicTimestamp getInstance() {
    return getInstance(null);
  }

  /** Returns the rapl instance, creating a new one if necessary. */
  public static MonotonicTimestamp getInstance(String path) {
    synchronized (MonotonicTimestamp.class) {
      if (instance != null) {
        return instance;
      }

      if (path == null) {
        System.loadLibrary("Monotonic");
      } else {
        System.load(path);
      }

      instance = new MonotonicTimestamp();
      return instance;
    }
  }

  public native long getMonotonicTimestamp();

  public static void main(String[] args) {
    System.out.println(
        "The time is "
            + getInstance(String.join("/", System.getProperty("user.dir"), args[0]))
                .getMonotonicTimestamp()
            + "!");
  }
}
