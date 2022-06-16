package edu.binghamton.vpc;

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
        System.loadLibrary("CPUScaler");
      } else {
        System.load(path);
      }

      instance = new MonotonicTimestamp();
      return instance;
    }
  }

  public native long getMonotonicTimestamp();

  public static void main(String[] args) {
    System.out.println("The time is " + getInstance(args[0]).getMonotonicTimestamp() + "!");
  }
}
