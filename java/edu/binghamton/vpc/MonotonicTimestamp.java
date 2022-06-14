package edu.binghamton.vpc;


public final class MonotonicTimestamp {
  static {
    System.load(String.join("/", System.getProperty("user.dir"), "libMonotonic.so"));
  }

  public static native long getMonotonicTimestamp();

  public static void main(String[] args) {
    System.out.println("The time is " + getMonotonicTimestamp() + "!");
  }
}
