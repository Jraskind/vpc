package research.utils.instrumentation.profiling;

import java.util.Set;
import research.utils.perf.PerfUtils;

public class SocketProfilingTimer implements Runnable {

	public void run() {
		PerfUtils.timer_start_recording();
		while(true) {
			try {
				Thread.sleep(1);
			} catch(Exception exception) {
				System.out.println("Timer cannot sleep! He seems to be too worried");
				exception.printStackTrace();
				System.exit(0);
			}

			PerfUtils.timer_read_counters();
		}
     }
}
