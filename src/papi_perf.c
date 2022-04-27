#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include "papi.h"
#include <semaphore.h>
#include <unistd.h> 
#include <sys/types.h>
#include <sys/syscall.h>
#include <string.h>
#include <sched.h>

#ifndef K_PERF_UTIL
        #include "perf_utils.h"
#endif

#define THRESHOLD 1000
#define PACKAGE_ENERGY_PACKAGE0 1073741881
#define DRAM_ENERGY_PACKAGE0 1073741882
#define NA -1

void handle_error (int retval);


//At any given time, a thread can only have four counters enabled.
__thread long long totals[MAX_COUNTERS];
__thread long long total_overflow=0;
__thread int EventSet=PAPI_NULL;

//This method will be called one time when the VM is booting.
/**
* This method does the following:
* 1- Initialize the PAPI Library
* 2- Convert Performance Event Names into Codes
* 3- Convert Energy Event Names into Codes
*/
EXTERNAL void init_papi(int p_num_events, char** p_event_names) {
	int retval=0;
	//init_g();
	//printf("[papi_perf.c] [int_papi] .... \n");	
        init_papi_lib();	
	//init_event_names(p_num_events, p_event_names, CORE_EVENTS);
	papi_energy_init();
	//Convert PAPI Event Names Into Codes ....
	/*for(int ev=0; ev<p_num_events;ev++) {
		if (PAPI_event_name_to_code(event_names[ev],&event_codes[ev]) != PAPI_OK) {
			printf("[init_papi] Cannot Find Code For Event %s \n", event_names[ev]);
			exit(0);
		}
	}*/
	

	//Query Events One By One
	/*for(int ev=0;ev<p_num_events;ev++) {
		if(PAPI_query_event(event_codes[ev]) != PAPI_OK) {
			printf("Unable to find event %s \n", event_names[ev]);
			exit(0);
		}
	}*/

	//retval=PAPI_thread_init((unsigned long(*)(void))(pthread_self));
	/*if (retval != PAPI_OK) {
		printf("Unable to Initialize PAPI PTHREAD :( \n");
		handle_error(retval);
	}*/

	application_start_time = PAPI_get_real_usec();
		
	printf("[init_papi] Done!");	
	initialized=1;
}

/**
* Since the Timer thread will be monitoring different events
* than other threads, it will have separate event names and codes.
* This method will be called by VM while parsing parameters.
*/
EXTERNAL void timer_init_events(int p_num_events, char** p_event_names) {
	int retval=0;
	init_event_names(p_num_events, p_event_names, GLOBAL_EVENTS);
	
	//Convert PAPI Event Names Into Codes ....
	for(int ev=0; ev<p_num_events;ev++) {
		if (PAPI_event_name_to_code(timer_event_names[ev],&timer_event_codes[ev]) != PAPI_OK) {
			printf("[init_timer_events] Cannot Find Code For Event %s \n", timer_event_names[ev]);
			exit(0);
		}
	}
}

EXTERNAL void timer_start_recording() {
	int retval=0;	
	is_timer = 1;
	printf("[timer_start_recording] .... \n");
	if(!initialized) {return;}
	
	printf("[timer_start_recording] Is it started ? \n");
	printf("[timer_start_recording] Doing Timer Stuff \n");

	timer_allocate_stats();
	/* Create the event set */
	if(started) goto energy_code; 
	if ((retval = PAPI_create_eventset(&EventSet))!=PAPI_OK) {
		printf("[timer_start_recording] Cannot Create Event Set %d \n", retval);
		handle_error(retval);
	}
	/* Add the events to the event set and allocate space for samples */
	printf("[timer_start_recording] Number of timer events is %d \n", timer_num_events);
	
	for(int ev=0; ev<timer_num_events ; ev++) {
		if ((retval = PAPI_add_event(EventSet, timer_event_codes[ev])) != PAPI_OK) {
			printf("[timer_start_recording] Cannot add event to event set %s \n", timer_event_names[ev]);
			handle_error(retval);
		}			
	}

	energy_code:	
	printf("energy_enabed:%d \n", energy_enabled);	
	if(energy_enabled) {
		if ((retval = PAPI_create_eventset(&Energy_EventSet))!=PAPI_OK) {
			printf("[timer_start_recording] Cannot Create Energy Event Set %d \n", retval);
			handle_error(retval);
		
		}
		add_energy_counters();
	}

	if(!started) {
		if((retval = PAPI_start(EventSet))!=PAPI_OK) {	
			printf("[timer_start_recording] Unable to start counting ... Will do soon ... Stay Tuned! \n");
			handle_error(retval);
		}
	}
	
	if(energy_enabled) {
		if((retval = PAPI_start(Energy_EventSet))!=PAPI_OK) {
			printf("[timer_start_recording] Unable to start energy counting ... Exiting System! \n");
			handle_error(retval);
		}
	}

	printf("[timer_start_recording] Done! \n");
	started=1;	
}

EXTERNAL void timer_stop_recording() {
	int retval =0;
	if ((retval = PAPI_stop(EventSet, local_totals)) != PAPI_OK) handle_error(retval);
	stopped=1;
	printf("[stop_recording] Thread ID %d  Number of logs %d \n",syscall(SYS_gettid),log_no);
}

//This function read global counters as well as energy
EXTERNAL void timer_read_counters_reset() {
	if(!initialized) return;
	if(!started) 	 return;

	if(stopped) {
		return;
	}

	int retval = 0;
		
	if ((retval = PAPI_read(EventSet, local_totals)) != PAPI_OK) {
		printf("%s \n","[timer_read_counters_reset] Heinous Error .... \n");
		handle_error(retval);
	}

	struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC,&ts);
	long long total_time = (ts.tv_sec*1000000000) + ts.tv_nsec;
	//long long total_time = rdtsc();
	//timer_timestamp = PAPI_get_real_usec();
	timer_timestamp = total_time;
	
	if(energy_enabled) read_energy();
	
	retval = PAPI_reset(EventSet);
	if(retval!=PAPI_OK) {
		printf("%s \n", "[timer_read_counters_reset] Cannot reset counters!");
		handle_error(retval);
	}
	
	timer_log_stat();
}

EXTERNAL void read_counters_reset() {
	if(!initialized) return;
	if(!started) 	 return;

	if(stopped) {
		return;
	}

	int retval = 0;
	
	timestamp = PAPI_get_real_usec();		
	if ((retval = PAPI_read(EventSet, local_totals)) != PAPI_OK) {
		printf("%s \n","[read_counters_reset] Heinous Error .... \n");
		handle_error(retval);
	}

	log_stat();
	
	retval = PAPI_reset(EventSet);
	
	if(retval!=PAPI_OK) {
		printf("%s \n", "[read_counters_reset] Cannot reset counters!");
		handle_error(retval);
	}
}


EXTERNAL void start_recording() {
		if(is_timer) return;
		if(!initialized) return;
		total_user_threads++;
		printf("[start_recording] Thread Already Started. Number of user threads %d \n", total_user_threads);
		if(started) return;
		//printf("[start_recording] Start \n");	
		int retval=0;
		
		//Get Virtual CPU ID and set affinity to it upon startup!
		//vcpu_id = sched_getcpu();
		//cpu_set_t  mask;
		//CPU_ZERO(&mask);
		//CPU_SET(vcpu_id, &mask);
		//int affinity_result = sched_setaffinity(0, sizeof(mask), &mask);
		/*if(affinity_result) {
		    printf("Setting CPU Affinity Failed! \n");
		    exit(0);
		}*/	
		
		/* Create the event set */
		if ((retval = PAPI_create_eventset(&EventSet))!=PAPI_OK) {
			printf("Cannot Create Event Set %d \n", retval);
			handle_error(retval);
		}

		//printf("[start_recording] EventSet created successfully \n");

		/* Add the events to the event set */
		for(int ev=0; ev<num_events ; ev++) {
			if ((retval = PAPI_add_event(EventSet, event_codes[ev])) != PAPI_OK) {
				printf("Unable to add event to event set \n");
				handle_error(retval);
			}
		}
		
		allocate_stats();
		
		/* start counting */
		if((retval = PAPI_start(EventSet))!=PAPI_OK) {
			printf("Unable to start recording. PAPI_start(EventSet) failed \n");
			handle_error(retval);
		}
		
		started=1;
	}


	/* overflow handler */
void handler(int EventSet, void *address, long long overflow_vector, void *context) {
	int Events[MAX_COUNTERS], number=MAX_COUNTERS, i;
	int retval;
	retval = PAPI_get_overflow_event_index(EventSet,overflow_vector, Events, &number);
	
	if(retval == PAPI_OK)
		for(i=0; i<number; i++) {
			//if(Events[i]) totals[i]++;
			totals[Events[i]]++;
		}		
	else {		
		printf("Can Read PAPI Events Index :%d", retval);
		exit(0);
	}
}

EXTERNAL void stop_recording() {
	if(is_timer) return;
	total_user_threads--;
	if(total_user_threads!=0) {printf("[stop_recording] User thread calling ... exit function \n");return;}
	int retval = 0;
	if(stopped) {printf("%s","Recording already stopped! \n");return;}
	//printf("[stop_recording] Start ... \n");
	read_counters_reset();
	//printf("[stop_recording] read counters done! \n");
	if ((retval = PAPI_stop(EventSet, local_totals)) != PAPI_OK)
		handle_error(retval);

	//printf("[stop_recording] Done! \n");
	stopped=1;
	printf("[stop_recording] Thread ID %d  Number of logs %d \n",syscall(SYS_gettid),log_no);
}



EXTERNAL void start_overflow() {
	if(!initialized) return;
	if(started) return;

	memset(totals, 0, sizeof(totals));

	int retval=0;

	/* Create the event set */
	if ((retval = PAPI_create_eventset(&EventSet))!=PAPI_OK) {
		printf("Cannot Create Event Set %d \n", retval);
		exit(0);
	}

	/* Add the events to the event set */
	for(int ev=0; ev<num_events ; ev++) {
		if ((retval = PAPI_add_event(EventSet, event_codes[ev])) != PAPI_OK) {
			printf("Cannot add event to event set %s \n", event_names[ev]);
			exit(0);
		}
	}

	//Mark events as overflow ...
	for(int ev=0; ev<num_events;ev++) {
		retval = PAPI_overflow(EventSet, event_codes[ev], THRESHOLD, 0, handler);
	}

	/* start counting */
	if((retval = PAPI_start(EventSet))!=PAPI_OK) {
		printf("Unable to sart counting ... Will do soon ... Stay Tuned! \n");
	}

	started=1;
}

EXTERNAL void stop_overflow() {
	int retval = 0;
	long long values[4];

	if ((retval = PAPI_stop(EventSet, values))!=PAPI_OK) {
			exit(retval);
	}

	for(int i=0;i<num_events;i++) {
		retval = PAPI_overflow(EventSet, event_codes[i],0, 0, handler);
	}

	for(int i=0; i<num_events; i++) {
		printf("%s:%lld \n", event_names[i], totals[i]);
	}
}


//This method is added to check the cost of resetting and reading performance counters
//PoC Method to analyze the cost
EXTERNAL void profile_read_reset() {
	if(!initialized) return;
	if(!started) 	 return;
	
	if(stopped) {
		return;
	}

	int retval = 0;

	long long t_read_start = PAPI_get_real_cyc();
	if ((retval = PAPI_read(EventSet, local_totals)) != PAPI_OK) {
		printf("%s \n","[read_counters_reset] Heinous Error .... \n");
		exit(0);
	}
	long long t_read_end = PAPI_get_real_cyc();
	long long t_read = t_read_end - t_read_start;
	
	long long t_reset_start = PAPI_get_real_cyc();
	retval = PAPI_reset(EventSet);
	if(retval!=PAPI_OK) {
		printf("%s \n", "[read_counters_reset] Cannot reset counters!");
		exit(0);
	}
	long long t_reset_end = PAPI_get_real_cyc();
	long long t_reset = t_reset_end - t_reset_start;

	printf("Read  Counters  Time %lld \n", t_read);
	printf("Reset Counters Time %lld \n", t_reset);
}

