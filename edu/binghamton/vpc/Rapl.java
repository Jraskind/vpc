package edu.binghamton.vpc;

/** Class that provides a singleton interface to RAPL measurements. */
public final class Rapl {
  private static Rapl instance;

  public static Rapl getInstance() {
    return getInstance(null);
  }

  /** Returns the rapl instance, creating a new one if necessary. */
  public static Rapl getInstance(String path) {
    synchronized (Rapl.class) {
      if (instance != null) {
        return instance;
      }

      if (path == null) {
        System.loadLibrary("Rapl");
      } else {
        System.load(path);
      }

      instance = new Rapl();
      return instance;
    }
  }

  /** Shuts down the instance. */
  public static void shutdown() {
    synchronized (Rapl.class) {
      if (instance == null) {
        return;
      }
      instance.ProfileDealloc();
      instance = null;
    }
  }

  private native int ProfileInit();

  private native int ProfileDealloc();

  private native int GetSocketNum();

  private native String EnergyStatCheck();

  private final int socketCount;
  private final double wrapAroundEnergy;

  private Rapl() {
    wrapAroundEnergy = ProfileInit();
    socketCount = GetSocketNum();
  }

  /**
   * @return an array of arrays of the current energy information by socket.
   *     <p>subarray structure is architecture dependent. Typically, 0 -> dram, 1 -> cpu, 2 ->
   *     package.
   */
  public synchronized double[][] getEnergyStats() {
    // guard if CPUScaler isn't available
    if (socketCount < 0) {
      return new double[0][0];
    }
    String EnergyInfo = EnergyStatCheck();
    if (socketCount == 1) {
      /*One Socket*/
      double[][] stats = new double[1][3];
      String[] energy = EnergyInfo.split("#");

      stats[0][0] = Double.parseDouble(energy[0]);
      stats[0][1] = Double.parseDouble(energy[1]);
      stats[0][2] = Double.parseDouble(energy[2]);

      return stats;
    } else {
      /*Multiple sockets*/
      String[] perSockEner = EnergyInfo.split("@");
      double[][] stats = new double[socketCount][3];
      int count = 0;

      for (int i = 0; i < perSockEner.length; i++) {
        String[] energy = perSockEner[i].split("#");
        for (int j = 0; j < energy.length; j++) {
          stats[i][j] = Double.parseDouble(energy[j]);
        }
      }

      return stats;
    }
  }

  /** @returns the sum of all energy counters */
  public synchronized double getEnergy() {
    // guard if CPUScaler isn't available
    if (socketCount < 0) {
      return -1;
    }

    String EnergyInfo = EnergyStatCheck();
    if (socketCount == 1) {
      // single socket
      double energy = 0;
      for (String e : EnergyInfo.split("#")) {
        energy += Double.parseDouble(e);
      }
      return energy;
    } else {
      // multi-socket
      double energy = 0;
      for (String estring : EnergyInfo.split("@")) {
        for (String e : EnergyInfo.split("#")) {
          energy += Double.parseDouble(e);
        }
      }
      return energy;
    }
  }

  public int getSocketCount() {
    return socketCount;
  }

  public double getWrapAroundEnergy() {
    return wrapAroundEnergy;
  }

  public static void main(String[] args) {
    for (double[] socketEnergy :
        getInstance(String.join("/", System.getProperty("user.dir"), args[0])).getEnergyStats()) {
      for (double componentEnergy : socketEnergy) {
        System.out.println(componentEnergy);
      }
    }
  }
}
