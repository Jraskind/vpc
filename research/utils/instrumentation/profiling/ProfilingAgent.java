package research.utils.instrumentation.profiling;


import java.lang.instrument.Instrumentation;
import research.utils.perf.PerfUtils;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;

public class ProfilingAgent {

	public static final String[] CORE_COUNTERS="PAPI_SR_INS,PAPI_TOT_CYC,PAPI_TOT_INS,PAPI_L1_TCM".split(",");
	public static final String[] PKG_COUNTERS= "PAPI_L3_TCM".split(",");
	public static byte[][] 	CORE_COUNTER_BYTES;
	public static byte[][]	PKG_COUNTER_BYTES;
	public static long start = 0;
	public static long end   = 0;
	public static String execution_file_name;

	public static void init_perf() {
		PerfUtils.set_energy_enabled(1);
		CORE_COUNTER_BYTES = new byte[CORE_COUNTERS.length][];	
		
		for(int pci=0; pci < CORE_COUNTERS.length; pci++) {
			try {			
				CORE_COUNTER_BYTES[pci] = (CORE_COUNTERS[pci] + "\0").getBytes();
			} catch(Exception exception) {
									
			}
		}

		PKG_COUNTER_BYTES = new byte[PKG_COUNTERS.length][];

		for(int pci=0; pci < PKG_COUNTERS.length; pci++) {
			try {			
				PKG_COUNTER_BYTES[pci] = (PKG_COUNTERS[pci] + "\0").getBytes();
			} catch(Exception exception) {
									
			}
		}


		PerfUtils.set_energy_enabled(1);
		PerfUtils.init_papi(CORE_COUNTERS);
		PerfUtils.timer_init_events(PKG_COUNTERS);

	}

	public static void write_to_file(String file_name, String value) {
			try {
				//execution_file_name = "execution_time_"+ProcessHandle.current().pid();
				PrintWriter writer = new PrintWriter(file_name);
				writer.println(value);
				writer.flush();
				writer.close();
			} catch(Exception exc) {
				exc.printStackTrace();
			}
	}

	public static void stop_recording(int expid) {
		System.out.println("Printing Energy Counters");
		System.out.println(expid);
		System.out.println("I am stopping recording ... I am stopping recording ... I am stopping recording");
		PerfUtils.print_energy_counters_expid(expid);
	}

	public static void start_recording() {
		init_perf();
		Thread socket_profiling_timer = new Thread(new SocketProfilingTimer());
		socket_profiling_timer.setDaemon(true);
		socket_profiling_timer.start();
	}

	public static void premain(String agentArgs, Instrumentation inst) {
		init_perf();
		execution_file_name = System.getProperty("exec_file");
		int expid = Integer.parseInt(System.getProperty("expid"));
		System.out.println("expid is " + expid);
		Thread socket_profiling_timer = new Thread(new SocketProfilingTimer());
		socket_profiling_timer.setDaemon(true);
		socket_profiling_timer.start();
		
		Runtime.getRuntime().addShutdownHook(
			new Thread() {
				public void run() {
					long end = System.currentTimeMillis();
					PerfUtils.print_energy_counters_expid(expid);
					long execution_time = end - start;
					String id_name = ManagementFactory.getRuntimeMXBean().getName();
					int pid = Integer.parseInt(id_name.split("@")[0]);
					execution_file_name = "execution_time_"+pid;

					write_to_file(execution_file_name,execution_time+"");
				}
			}
		);

		start = System.currentTimeMillis();
	}


	public static String get_env(String name ,String def) {
		String value = def;
		try {
			value = System.getenv(name);	
			if(value==null) value=def;
		} catch(Exception exception) {
			exception.printStackTrace();
		}

		return value;
	}
}
