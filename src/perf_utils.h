#ifdef K_PERF_UTIL
//Empty Space Here
#else

#define  K_PERF_UTIL
/****************************************************************************************/
//Maximum Number of events that can be measured by a Virtual CPU (Virtual Thread on a Core)
#define MAX_COUNTERS 4
 
#define NUMBER_OF_PACKAGES 2
#define CORE_EVENTS 	1
#define GLOBAL_EVENTS 	0

#define MAX_NAME_LEN 256

#include <unistd.h> 
#include <sys/types.h>
#include <sys/syscall.h>
#include <stdio.h>
#include <stdlib.h>
#include "papi.h"
#define EXTERNAL 

typedef struct {
    int thread_id;
    long long* time_stamps;
    long log_no;
    long long *counters[4];
    int* core_ids;
} thread_stats;

//Maintain Thread Specific Performance Counters and Stats
extern __thread  long  		log_no;
extern __thread  long 		allocated;
extern __thread  long long 	*counters[MAX_COUNTERS];
extern __thread  long long	*time_stamps;
extern __thread  long long 	local_totals[MAX_COUNTERS];
extern __thread  long long 	timestamp;
extern __thread  int 	   		is_timer;
extern __thread  int 	    	*core_ids;


//Thread Local Related States : Counting Start, Counted Stopped.
extern __thread int started;
extern __thread int total_user_threads;
extern __thread int stopped;

#define MAX_RAPL_EVENTS 64

/* These variables will store pointers to the thread local data structures.
 * Modifications to these variables must be only performed in thread safe context.
 * C Code will not handle the thread safety. JikesRVM calls register_thread_stat in a synchronized context.
 */
extern int allocated_g;
extern thread_stats** thread_stats_g;
extern int number_of_threads;

//All threads will monitor the same events except for threads that monitor package events(Ex:Last Level Cache)
extern int  num_events;
extern int  initialized ;
extern char event_names[MAX_COUNTERS][MAX_NAME_LEN];
extern int  event_codes[MAX_COUNTERS];

//One thread will monitor CPU level based. Additionally, it might monitor its own events as well or other global events.
//Because in the current implementation, Jikes TimeThread will own these variables, they will be prefixed with "timer_"
extern int  timer_num_events;
extern int  timer_initialized;
extern char timer_event_names[MAX_COUNTERS][MAX_NAME_LEN];
extern int  timer_event_codes[MAX_COUNTERS];

//Energy Related Variables
extern long long *energy_time_stamps;
extern long long *energy_readings[NUMBER_OF_PACKAGES*2];
extern long no_energy_records;
extern long long energy_measure[NUMBER_OF_PACKAGES*2];
extern long allocated_energy_records;

extern long timer_log_no;
extern long timer_allocated;
extern long long *timer_counters[4];
extern long long *timer_time_stamps;
extern long long timer_timestamp;

//General Configuration 
extern int print_mode;
extern int include_core_info;


//Energy Related Variables
//long long *energy_time_stamps;
//extern long long *energy_readings[2];
//long no_energy_records=0;
//extern long long energy_measure[2];
//long allocated_energy_records=1;
extern int Energy_EventSet;

extern char rapl_event_names[MAX_RAPL_EVENTS][PAPI_MAX_STR_LEN];
extern char units[MAX_RAPL_EVENTS][PAPI_MAX_STR_LEN];
extern int  rapl_event_codes[MAX_RAPL_EVENTS];
extern int  rapl_num_events;

int dram_energy_event_code;
int pkg_energy_event_code;
int dram_energy_event_code_two;
int pkg_energy_event_code_two;


extern long long application_start_time;
extern int energy_enabled;

//Function Prototypes
void handle_error(int);
void register_thread_stat();
void check_malloc();
void inc_number_of_threads();
void init_g();
void allocate_stats();
void print_counters();
void print_counters_g();
void set_include_core_info(int);
void counters_print_mode(int);
void allocate_extra_space();
void timer_allocate_extra_space();
void print_timer_counters();
void print_timer_counters_exp(int);
void add_energy_counters();
void papi_energy_init();
void read_energy();
double get_energy();
void timer_log_stat();
void log_stat();
void timer_allocate_stats();
void init_event_names(int,char**,int);
void set_energy_enabled(int);
void init_papi_lib();
long long get_time_in_micro();
long long get_cycles();
long get_timestamp();
u_int64_t rdtsc();
	
#endif
