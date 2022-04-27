#include "perf_utils.h"



//TODO:: Remove all "EXTERNAL" in this file. It is defined as empty space.
EXTERNAL int energy_enabled = 0;

EXTERNAL __thread  long  		log_no 	  = 0;
EXTERNAL __thread  long 		allocated = 20000;
EXTERNAL __thread  long long 		*counters[MAX_COUNTERS];
EXTERNAL __thread  long long		*time_stamps;
EXTERNAL __thread  long long 		local_totals[MAX_COUNTERS];
EXTERNAL __thread  long long 		timestamp;
EXTERNAL __thread  int 	   		is_timer=0;
EXTERNAL __thread  int 	    		*core_ids;


//Thread Local Related States : Counting Start, Counted Stopped.
EXTERNAL __thread int started=0;
EXTERNAL __thread int total_user_threads=0;
EXTERNAL __thread int stopped=0;

#define MAX_RAPL_EVENTS 64

/* These variables will store pointers to the thread local data structures.
 * Modifications to these variables must be only performed in thread safe context.
 * C Code will not handle the thread safety. JikesRVM calls register_thread_stat in a synchronized context.
 */
EXTERNAL int allocated_g = 100;
EXTERNAL thread_stats** thread_stats_g;
EXTERNAL int number_of_threads=0;

//All threads will monitor the same events except for threads that monitor package events(Ex:Last Level Cache)
EXTERNAL int  num_events=0;
EXTERNAL int  initialized = 0;
EXTERNAL char event_names[MAX_COUNTERS][MAX_NAME_LEN];
EXTERNAL int  event_codes[MAX_COUNTERS];

//One thread will monitor CPU level based. Additionally, it might monitor its own events as well or other global events.
//Because in the current implementation, Jikes TimeThread will own these variables, they will be prefixed with "timer_"
EXTERNAL int  timer_num_events;
EXTERNAL int  timer_initialized=0;
EXTERNAL char timer_event_names[MAX_COUNTERS][MAX_NAME_LEN];
EXTERNAL int  timer_event_codes[MAX_COUNTERS];

//Energy Related Variables
EXTERNAL long long *energy_time_stamps;
EXTERNAL long long *energy_readings[2*NUMBER_OF_PACKAGES];
EXTERNAL long no_energy_records=0;
EXTERNAL long long energy_measure[2*NUMBER_OF_PACKAGES];
EXTERNAL long allocated_energy_records=1;

EXTERNAL long timer_log_no 	  = 0;
EXTERNAL long timer_allocated 	  = 20000;
EXTERNAL long long *timer_counters[4];
EXTERNAL long long *timer_time_stamps;
EXTERNAL long long timer_timestamp;

//General Configuration 
EXTERNAL int include_core_info=0;

EXTERNAL long long application_start_time=0;


//Energy Related Variables
//long long *energy_time_stamps;
//long allocated_energy_records=1;
EXTERNAL int Energy_EventSet=PAPI_NULL;

EXTERNAL char rapl_event_names[MAX_RAPL_EVENTS][PAPI_MAX_STR_LEN];
EXTERNAL char units[MAX_RAPL_EVENTS][PAPI_MAX_STR_LEN];
EXTERNAL int  rapl_event_codes[MAX_RAPL_EVENTS];
EXTERNAL int  rapl_num_events=0;

int dram_energy_event_code      =  -1;
int pkg_energy_event_code       =  -1;
int dram_energy_event_code_two = -1;
int pkg_energy_event_code_two = -1;

EXTERNAL void handle_error(int retval) {
	printf("PAPI error %d: %s\n", retval, PAPI_strerror(retval));
	exit(retval);
}

//This method must be called in a thread safe context.
//It is the responsibility of the caller to guarantee that only one thread at a time will call this method.
//This method is called by RVMThread#run() after execution.
EXTERNAL void register_thread_stat() {
	
	if(is_timer) return;
	if(total_user_threads>0) {printf("Some user threads are still using the current system thread \n"); return;}
	if(number_of_threads>=allocated_g) {
		thread_stats_g = realloc(thread_stats_g, sizeof(thread_stats*)*(allocated_g*2));
		check_malloc(thread_stats_g,"realloc g_thread_stats");
		allocated_g = allocated_g * 2;	
	}
	
	thread_stats* my_stats = malloc(sizeof(thread_stats));
	my_stats->thread_id=syscall(SYS_gettid);
	my_stats->time_stamps=time_stamps;
	my_stats->log_no=log_no;
	my_stats->core_ids=core_ids;
	
	for(int ev=0; ev<num_events;ev++) {
		my_stats->counters[ev]=counters[ev];
	}
	
	thread_stats_g[number_of_threads]=my_stats;
	printf("[register_thread_stat] Thred Index : thread_stats_g Thread ID:%d Number of records %d \n", thread_stats_g[number_of_threads]->thread_id, thread_stats_g[number_of_threads]->log_no);
	number_of_threads++;
	printf("Number of threads : %d \n", number_of_threads);
}

EXTERNAL void check_malloc(void *address, char* message) {
	if(!address) {
		printf("****** malloc failed %s *******\n \n", message);
		exit(0);
	}
}

EXTERNAL void inc_number_of_threads() {
	number_of_threads++;
}

//Some really nice and neat utility functions
inline pid_t get_tid() {
	syscall(SYS_gettid);
}

EXTERNAL void init_g() {
	thread_stats_g = malloc(sizeof(thread_stats*)*allocated_g);
	check_malloc(thread_stats_g,"malloc g_thread_stats");
}

//@which: 0:Global Package Events, 1:Core Events
EXTERNAL void init_event_names(int p_num_events, char** p_event_names, int which) {
	
	printf("[perf_utils.c] [init_event_names] Number of events : %d \n", p_num_events);
	if(which) {
		memset(event_names,'\0', sizeof(event_names));
		num_events = p_num_events;
	} else {
		memset(timer_event_names,'\0', sizeof(timer_event_names));
		timer_num_events = p_num_events;
	}
	
	for(int ev=0; ev<p_num_events; ev++) {	
		if(which) {
			strcpy(event_names[ev], p_event_names[ev]);	
		} else {
			strcpy(timer_event_names[ev], p_event_names[ev]);	
		}		
		
		printf("Adding %s to list of events ... \n", p_event_names[ev]);
	}

	printf("[init_event_names] p_num_events %d, num_events %d \n", p_num_events,num_events);
}

EXTERNAL void allocate_stats() {
	
	for(int ev=0; ev<num_events ; ev++) {			
		counters[ev] = malloc(allocated * sizeof(long long));
		check_malloc(counters[ev],"Performance Counters");
	}
	
	time_stamps = malloc(allocated * sizeof(long long));
	check_malloc(time_stamps, "Time Stamps");
	
	if(include_core_info) {
		core_ids = malloc(allocated * sizeof(long long));
		check_malloc(core_ids,"Core IDs");
	}
}


EXTERNAL void timer_allocate_stats() {
	for(int ev=0; ev<timer_num_events ; ev++) {		
		timer_counters[ev] = malloc(timer_allocated * sizeof(long long));
		check_malloc(timer_counters[ev],"Performance Counters");	
	}
	
	//Allocate space for energy samples
	for(int eng_index=0; eng_index<NUMBER_OF_PACKAGES * 2; eng_index++) {
		energy_readings[eng_index] = malloc(timer_allocated * sizeof(long long));
		check_malloc(energy_readings[eng_index],"Energy Allocation");		
	}

	
	timer_time_stamps=malloc(timer_allocated*sizeof(long long));
	check_malloc(timer_time_stamps,"[time_start_recording] ... timer_time_stamps");
	printf("[timer_allocate_stats] Done \n");
}

EXTERNAL void print_counters() {
	pid_t tid = syscall(SYS_gettid);
	printf("[print_counters] number of logs is %d \n", log_no);
	for (int log_idx=0; log_idx < log_no; log_idx++) {
		char* stats_log="";
		asprintf(&stats_log,"%s%s",stats_log,"stats,");
		asprintf(&stats_log, "%s%d,", stats_log, tid);
		asprintf(&stats_log,"%s%lld,", stats_log, time_stamps[log_idx]);
		
		//Add Performance Counters to the String
		for(int j=0;j<num_events;j++) {
			asprintf(&stats_log,"%s%s,",stats_log,event_names[j]);
			asprintf(&stats_log,"%s%d,", stats_log, counters[j][log_idx]);
		}
		asprintf(&stats_log,"%s end \n", stats_log);	
		printf("%s", stats_log);
	}
}

EXTERNAL void print_counters_g() {
	printf("[print_counters_g] .... Number of threads is %d \n", number_of_threads);
	for(int thread_idx=0;thread_idx<number_of_threads;thread_idx++) {
		int log_no_t  =  thread_stats_g[thread_idx]->log_no;
		int stats_tid =  thread_stats_g[thread_idx]->thread_id;
		//int stats_vcpu_id = thread_stats_g[thread_idx]->vcpu_id;

		printf("[print_counters_g] Thread %d, Number of logs %d, Number of Counters is %d \n", stats_tid, log_no_t, num_events);
		printf("[print_counters_g] thread_idx  %d \n", thread_idx);
		long long *stats_timestamps = thread_stats_g[thread_idx]->time_stamps; 
		int* stats_core_ids = thread_stats_g[thread_idx]->core_ids;
		for (int log_idx=0; log_idx < log_no_t; log_idx++) {
			char* stats_log="";
			asprintf(&stats_log,"%s%s",stats_log,"stats,");
			asprintf(&stats_log, "%s%d,", stats_log,  stats_tid);
			
			if(include_core_info) {
				int current_core_id = stats_core_ids[log_idx]; 
				asprintf(&stats_log, "%s%d,", stats_log, current_core_id);
			}

			asprintf(&stats_log,"%s%lld,", stats_log, (stats_timestamps[log_idx] - application_start_time));
			
			//Add Performance Counters to the String
			for(int j=0;j<num_events;j++) {
				asprintf(&stats_log,"%s%s,",stats_log,event_names[j]);
				asprintf(&stats_log,"%s%d,", stats_log, thread_stats_g[thread_idx]->counters[j][log_idx]);
			}
			asprintf(&stats_log,"%s end \n", stats_log);	
			printf("%s", stats_log);
		}
	}
}

EXTERNAL void set_include_core_info(int core_info) {
	include_core_info = core_info;
}


EXTERNAL void allocate_extra_space() {
	if(log_no >= allocated) {
		int extra_space = allocated;
		int total_space = allocated+extra_space;

		time_stamps = realloc(time_stamps,total_space*sizeof(long long));
		check_malloc(time_stamps, "Counters Energy Realloc");	

		if(include_core_info) {
			core_ids = realloc(core_ids, total_space*sizeof(long long));
			check_malloc(core_ids,"Core IDs - ReAlloc");
		}

		for (int ev=0; ev< num_events; ev++) {
			counters[ev] = realloc(counters[ev],total_space*sizeof(long long));
			check_malloc(counters[ev],"Counters realloc");
		}

		allocated = total_space;
	}
}

EXTERNAL void timer_allocate_extra_space() {
	if(timer_log_no >= timer_allocated) {
		int extra_space = timer_allocated;
		int total_space = timer_allocated+extra_space;

		timer_time_stamps = realloc(timer_time_stamps,total_space*sizeof(long long));
		check_malloc(timer_time_stamps, "Timer Timestamps Re-alloc");	
		
		for (int ev=0; ev<timer_num_events; ev++) {
			timer_counters[ev] = realloc(timer_counters[ev],total_space*sizeof(long long));
			check_malloc(timer_counters[ev],"Timer Counters re-alloc");
		}
	
		for(int eng_index = 0; eng_index < 2 * NUMBER_OF_PACKAGES; eng_index++) {
			energy_readings[eng_index] = realloc(energy_readings[eng_index],total_space*sizeof(long long));
			check_malloc(energy_readings[eng_index],"Energy Allocate Extra Space");
		}		
		
		timer_allocated = total_space;
	}
}



int experiment_id=-1;
EXTERNAL void print_timer_counters_exp(int expid) {
	experiment_id=expid;
	print_timer_counters();
}


EXTERNAL void print_timer_counters() {
	printf("[print_timer_counters] Number of Timer Samples is %d \n", timer_log_no);	
	int pid = getpid();
	char file_name[100];
	if(experiment_id==-1) {
		sprintf(file_name, "energy_%d.data", pid); // puts string into buffer
	} else {

		sprintf(file_name, "energy_%d_%d.data", experiment_id,pid); // puts string into buffer
	}
	
	FILE *fp = fopen (file_name, "a");
	for (int log_idx=0; log_idx < timer_log_no; log_idx++) {
			char* stats_log="";
			asprintf(&stats_log,"%s%s",stats_log,"timer_stats,");
			asprintf(&stats_log,"%s%lld,", stats_log, (timer_time_stamps[log_idx]));
		
		//Add Performance Counters to the String
		for(int j=0;j<timer_num_events;j++) {
			asprintf(&stats_log,"%s%s,", stats_log ,timer_event_names[j]);
			asprintf(&stats_log,"%s%d,", stats_log ,timer_counters[j][log_idx]);
		}
		
		long long pkg   = energy_readings[0][log_idx];
		long long dram  = energy_readings[1][log_idx];
		long long pkg1  = energy_readings[2][log_idx];
		long long dram2 = energy_readings[3][log_idx]; 
		long long total_pkg = pkg + pkg1;
		long long total_dram = dram + dram2;

		asprintf(&stats_log,"%s%12.8f,%12.8f",stats_log,(double) total_pkg/1.0e9, (double) total_dram/1.0e9);
		asprintf(&stats_log,"%s,end\n", stats_log);
        	fprintf(fp, "%s", stats_log);
		//printf("%s", stats_log);
	}


	fclose(fp);
}

EXTERNAL void log_stat() {
	//Allocate extra space if necessary
	allocate_extra_space();
	time_stamps[log_no] = timestamp;

	for (int ev=0; ev< num_events; ev++) {
			counters[ev][log_no] = local_totals[ev];
	}

	if(include_core_info) {
		int current_core_id = sched_getcpu(); 
		core_ids[log_no] = current_core_id;
	}

	log_no++;
}

EXTERNAL void timer_log_stat() {
	timer_allocate_extra_space();
	timer_time_stamps[timer_log_no] = timer_timestamp;
	
	for (int ev=0; ev < timer_num_events; ev++) {
			timer_counters[ev][timer_log_no] = local_totals[ev];
	}
	
	//TODO::Refactor:: Just put in a for loop
	for(int eng_index=0; eng_index < 2*NUMBER_OF_PACKAGES; eng_index++) {
		energy_readings[eng_index][timer_log_no] = energy_measure[eng_index];
	}

	/*energy_readings[0][timer_log_no] = energy_measure[0];
	energy_readings[1][timer_log_no] = energy_measure[1];
	energy_readings[2][timer_log_no] = energy_measure[2];
	energy_readings[3][timer_log_no] = energy_measure[3];*/
	
	timer_log_no++;	
}

EXTERNAL void read_energy() {
	int retval=0;
	if ((retval = PAPI_read(Energy_EventSet, energy_measure)) != PAPI_OK) {
		printf("[read_energy]  ... Unable to read energy \n");
		handle_error(retval);
	}
}


EXTERNAL double get_energy() {

    long long p_energy_reading[2*NUMBER_OF_PACKAGES];
	int retval=0;
	if ((retval = PAPI_read(Energy_EventSet, p_energy_reading)) != PAPI_OK) {
		printf("[read_energy]  ... Unable to read energy \n");
		handle_error(retval);
	}

    long long pkg_energy = p_energy_reading[0] + p_energy_reading[2];
    double ret = (double) (pkg_energy / 1.0e9);
    return ret;
}


EXTERNAL void papi_energy_init() {
	printf("[papi_energy_init] ..... \n");
	int retval = 0;
	int numcmp = PAPI_num_components();
	int rapl_cid=-1;
	const PAPI_component_info_t *cmpinfo = NULL;
	
	for(int cid=0; cid<numcmp; cid++) {
		cmpinfo = PAPI_get_component_info(cid);
		if (strstr(cmpinfo->name,"rapl")) {
			rapl_cid=cid;
		}
	}
	
	int code = PAPI_NATIVE_MASK;
	int r = PAPI_enum_cmp_event(&code, PAPI_ENUM_FIRST, rapl_cid);
	if(r!=PAPI_OK) printf("%s \n" , "PAPI_enum_cmp_event not very well \n");
	PAPI_event_info_t evinfo;
	
	while (r==PAPI_OK) {
		retval = PAPI_event_code_to_name(code, rapl_event_names[rapl_num_events]);
		if ( retval != PAPI_OK ) {
			printf("Unable to get enent name for code %d \n", code);
			exit(0);
		}
		
		retval = PAPI_get_event_info(code, &evinfo);
		if (retval != PAPI_OK) {
			printf("Unable to get event info for event code %d \n", code);
			exit(0);
		}

		printf("#rapl_event %d -- %s \n",code  ,rapl_event_names[rapl_num_events]);
		rapl_num_events++;
		r = PAPI_enum_cmp_event(&code, PAPI_ENUM_EVENTS, rapl_cid);
	}	
	
	int ret = PAPI_event_name_to_code("rapl:::PACKAGE_ENERGY:PACKAGE0", &pkg_energy_event_code);
	if(ret) {printf("Unable to get code for PACKAGE_ENERGY_PACKAGE0 %s \n", PAPI_strerror(ret));handle_error(retval);}
	ret = PAPI_event_name_to_code("rapl:::DRAM_ENERGY:PACKAGE0",&dram_energy_event_code); 
	if(ret) {printf("Unable to get code for DRAM_ENERGY_PACKAGE0 %s \n", PAPI_strerror(ret));handle_error(retval);}


	ret = PAPI_event_name_to_code("rapl:::PACKAGE_ENERGY:PACKAGE1", &pkg_energy_event_code_two);
	if(ret) {printf("Unable to get code for PACKAGE_ENERGY_PACKAGE1 %s \n", PAPI_strerror(ret));handle_error(retval);}
	ret = PAPI_event_name_to_code("rapl:::DRAM_ENERGY:PACKAGE1",&dram_energy_event_code_two); 
	if(ret) {printf("Unable to get code for DRAM_ENERGY_PACKAGE1 %s \n", PAPI_strerror(ret));handle_error(retval);}

	ret=PAPI_query_event(pkg_energy_event_code);	
	if(ret != PAPI_OK) {printf("Cannot query event code related to package energy : %d \n", pkg_energy_event_code);handle_error(retval);}
	
	ret=PAPI_query_event(dram_energy_event_code);
	if(ret!=PAPI_OK) {printf("Cannot query event code related to dram energy %d \n",dram_energy_event_code);handle_error(retval);}



	ret=PAPI_query_event(pkg_energy_event_code_two);	
	if(ret != PAPI_OK) {printf("Cannot query event code related to package energy : %d \n", pkg_energy_event_code_two);handle_error(retval);}
	
	ret=PAPI_query_event(dram_energy_event_code_two);
	if(ret!=PAPI_OK) {printf("Cannot query event code related to dram energy %d \n",dram_energy_event_code_two);handle_error(retval);}
	printf("[papi_energy_init] PAPI Event Names Successfully Converted to Codes \n");
}


EXTERNAL void add_energy_counters() {
	int retval=0;
	if ((retval = PAPI_add_event(Energy_EventSet, pkg_energy_event_code)) != PAPI_OK) {
		printf("Cannot add event to event set %s %d \n", "PACKAGE_ENERGY_PACKAGE0", retval);
			handle_error(retval);
	 }

	 if ((retval = PAPI_add_event(Energy_EventSet, dram_energy_event_code)) != PAPI_OK) {
		 printf("Cannot add event to event set %s %d \n", "DRAM_ENERGY_PACKAGE0", retval);
			 handle_error(retval);
	 }


	if ((retval = PAPI_add_event(Energy_EventSet, pkg_energy_event_code_two)) != PAPI_OK) {
		printf("Cannot add event to event set %s %d \n", "PACKAGE_ENERGY_PACKAGE1", retval);
			handle_error(retval);
	}

	if ((retval = PAPI_add_event(Energy_EventSet, dram_energy_event_code_two)) != PAPI_OK) {
		 printf("Cannot add event to event set %s %d \n", "DRAM_ENERGY_PACKAGE1", retval);
			 handle_error(retval);
	}

	printf("Energy events successfully added to EventSet \n");
}

EXTERNAL void set_energy_enabled(int flag) {
	energy_enabled = flag;
}

//Papi Library will always be initialized
EXTERNAL void init_papi_lib() {
	int retval=0;
	printf("Initializing PAPI : %d \n", PAPI_VER_CURRENT);
	if((retval=PAPI_library_init(PAPI_VER_CURRENT)) != PAPI_VER_CURRENT) {
		printf("[sysInitPerf] Library Initialization Error! %d \n", retval);
		handle_error(retval);
	}
}

EXTERNAL long long get_time_in_micro() {
	long long ctime = PAPI_get_real_usec();
	return ctime;
}


EXTERNAL long long get_cycles() {
	long long ctime = PAPI_get_real_cyc();
	return ctime;
}



EXTERNAL u_int64_t rdtsc(){
	unsigned long int lo,hi;
	__asm__ __volatile__ ("rdtsc" : "=a" (lo), "=d" (hi));
	return ((u_int64_t)hi << 32) | lo;
}
