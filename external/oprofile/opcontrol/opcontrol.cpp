/*
 * Copyright 2008, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Binary implementation of the original opcontrol script due to missing tools
 * like awk, test, etc.
 */

#include <unistd.h>
#include <getopt.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <dirent.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>

#include "op_config.h"

#define verbose(fmt...) if (verbose_print) printf(fmt)

struct event_info {
    int id;
    int counters;
    int um;
    const char *name;
    const char *explanation;
};

#define CTR(n)  (1<<(n))

#if defined(__i386__) || defined(__x86_64__)
struct event_info event_info_arch_perfmon[] = {
    #include "../events/i386/arch_perfmon/events.h"
};

#define MAX_EVENTS 2
int min_count[MAX_EVENTS] = {60000, 100000};

const char *default_event = "CPU_CLK_UNHALTED";
#endif

#if defined(__arm__)
#if !defined(WITH_ARM_V7_A)
struct event_info event_info_armv6[] = {
    #include "../events/arm/armv6/events.h"
};

#define MAX_EVENTS 3
int min_count[MAX_EVENTS] = {150000, 200000, 250000};

#else
struct event_info event_info_armv7[] = {
    #include "../events/arm/armv7/events.h"
};

#define MAX_EVENTS 5
int min_count[MAX_EVENTS] = {150000, 20000, 25000, 30000, 35000};
#endif

const char *default_event = "CPU_CYCLES";
#endif

#if defined(__mips__)
struct event_info event_info_24K[] = {
    #include "../events/mips/24K/events.h"
};
struct event_info event_info_34K[] = {
    #include "../events/mips/34K/events.h"
};
struct event_info event_info_74K[] = {
    #include "../events/mips/74K/events.h"
};
struct event_info event_info_1004K[] = {
    #include "../events/mips/1004K/events.h"
};

#define MAX_EVENTS 4
int min_count[MAX_EVENTS] = {150000, 20000, 25000, 30000};

const char *default_event = "CYCLES";
#endif /* defined(__mips__) */

#define ARRAYSZ(x) (sizeof(x)/sizeof((x)[0]))

struct cpuevents {
    const char *cpu;
    struct event_info *event_info;
    unsigned int nevents;
} cpuevents[] = {
#if defined(__i386__) || defined(__x86_64__)
    {"i386/arch_perfmon", event_info_arch_perfmon, ARRAYSZ(event_info_arch_perfmon)},
#endif /* defined(__i386__) || defined(__x86_64__) */
#if defined(__arm__)
#if !defined(WITH_ARM_V7_A)
    {"arm/armv6", event_info_armv6, ARRAYSZ(event_info_armv6)},
#else
    {"arm/armv7", event_info_armv7, ARRAYSZ(event_info_armv7)},
#endif
#endif /* defined(__arm__) */
#if defined(__mips__)
    {"mips/24K", event_info_24K, ARRAYSZ(event_info_24K)},
    {"mips/34K", event_info_34K, ARRAYSZ(event_info_34K)},
    {"mips/74K", event_info_74K, ARRAYSZ(event_info_74K)},
    {"mips/1004K", event_info_1004K, ARRAYSZ(event_info_1004K)},
#endif /* defined(__mips__) */
};

struct cpuevents *cpuevent;
#define event_info cpuevent->event_info
#define NEVENTS cpuevent->nevents

int verbose_print;
int list_events; 
int show_usage;
int setup;
int quick;
int timer;
int num_events;
int start;
int stop;
int reset;

int selected_events[MAX_EVENTS];
int selected_counts[MAX_EVENTS];
int max_events;

char callgraph[8];
char kernel_range[512];
char vmlinux[512];

struct option long_options[] = {
    {"help", 0, &show_usage, 1},
    {"list-events", 0, &list_events, 1},
    {"reset", 0, &reset, 1},
    {"setup", 0, &setup, 1},
    {"quick", 0, &quick, 1},
    {"timer", 0, &timer, 1},
    {"callgraph", 1, 0, 'c'},
    {"event", 1, 0, 'e'},
    {"vmlinux", 1, 0, 'v'},
    {"kernel-range", 1, 0, 'r'},
    {"start", 0, &start, 1},
    {"stop", 0, &stop, 1},
    {"dump", 0, 0, 'd'},
    {"shutdown", 0, 0, 'h'},
    {"status", 0, 0, 't'},
    {"verbose", 0, 0, 'V'},
    {"verbose-log", 1, 0, 'l'},
    {0, 0, 0, 0},
};


void usage()
{
    printf("\nopcontrol: usage:\n"
           "   --list-events    list event types\n"
           "   --help           this message\n"
           "   --verbose        show extra status\n"
           "   --verbose-log=lvl set daemon logging verbosity during setup\n"
           "                    levels are: all,sfile,arcs,samples,module,misc\n"
           "   --setup          setup directories\n"
#if defined(__i386__) || defined(__x86_64__)
           "   --quick          setup and select CPU_CLK_UNHALTED:60000\n"
#elif defined(__arm__)
           "   --quick          setup and select CPU_CYCLES:150000\n"
#elif defined(__mips__)
           "   --quick          setup and select CYCLES:150000\n"
#endif
           "   --timer          timer-based profiling\n"
           "   --status         show configuration\n"
           "   --start          start data collection\n"
           "   --stop           stop data collection\n"
           "   --reset          clears out data from current session\n"
           "   --shutdown       kill the oprofile daemon\n"
           "   --callgraph=depth callgraph depth\n"
           "   --event=eventspec\n"
           "      Choose an event. May be specified multiple times.\n"
           "      eventspec is in the form of name[:count], where :\n"
           "        name:  event name, see \"opcontrol --list-events\"\n"
           "        count: reset counter value\n" 
           "   --vmlinux=file   vmlinux kernel image\n"
           "   --kernel-range=start,end\n"
           "                    kernel range vma address in hexadecimal\n"
          );
}

int setup_device(void)
{
    if (mkdir(OP_DRIVER_BASE, 0755)) {
        if (errno != EEXIST) {
            fprintf(stderr, "Cannot create directory "OP_DRIVER_BASE": %s\n",
                    strerror(errno));
            return -1;
        }
    }

    if (access(OP_DRIVER_BASE"/stats", F_OK)) {
        if (system("mount -t oprofilefs nodev "OP_DRIVER_BASE)) {
            return -1;
        }
    }

    /* Selecting the event information by cpu_type has only been tested on MIPS */
#if defined(__mips__)
    /* Use cpu_type to select the events */
    int fd = open(OP_DRIVER_BASE "/cpu_type", O_RDONLY);
    if (fd < 0) {
	fprintf(stderr, OP_DRIVER_BASE "/cpu_type: %s\n",
		strerror(errno));
	return -1;
    }

    char buf[512];
    int n = read(fd, buf, sizeof(buf)-1);
    close(fd);
    if (n < 0) {
	fprintf(stderr, OP_DRIVER_BASE "/cpu_type: %s\n",
		strerror(errno));
	return -1;
    }
    buf[n] = '\0';
    for (unsigned int i = 0; i < ARRAYSZ(cpuevents); i++) {
	if (strcmp(buf, cpuevents[i].cpu) == 0) {
	    cpuevent = &cpuevents[i];
	}
    }
    if (cpuevent == NULL) {
	fprintf(stderr, "Unrecognised CPU type %s\n", buf);
	return -1;
    }
    for (max_events = 0; max_events < MAX_EVENTS; max_events++) {
	snprintf(buf, sizeof(buf), OP_DRIVER_BASE"/%d", max_events);
	if (access(buf, F_OK) < 0)
	    break;
    }
#else
    max_events = MAX_EVENTS;
    cpuevent = &cpuevents[0];
#endif
    return 0;
}

void setup_session_dir()
{
    if (access(OP_DATA_DIR, F_OK) == 0)
        system("rm -r "OP_DATA_DIR);

    if (mkdir(OP_DATA_DIR, 0755)) {
        fprintf(stderr, "Cannot create directory \"%s\": %s\n",
                OP_DATA_DIR, strerror(errno));
    }
    if (mkdir(OP_DATA_DIR"/samples", 0755)) {
        fprintf(stderr, "Cannot create directory \"%s\": %s\n",
                OP_DATA_DIR"/samples", strerror(errno));
    }
}

int read_num(const char* file)
{
    char buffer[256];
    int fd = open(file, O_RDONLY);
    if (fd<0) return -1;
    int rd = read(fd, buffer, sizeof(buffer)-1);
    buffer[rd] = 0;
    close(fd);
    return atoi(buffer);
}

int do_setup()
{
    char dir[1024];

    /*
     * Kill the old daemon so that setup can be done more than once to achieve
     * the same effect as reset.
     */
    int num = read_num(OP_DATA_DIR"/lock");
    if (num >= 0) {
        printf("Terminating the old daemon...\n");
        kill(num, SIGTERM);
        sleep(5);
    }

    setup_session_dir();

    return 0;
}

void stringify_counters(char *ctr_string, int ctr_mask)
{
    int i, n, len;
    char *p = ctr_string;

    *p = '\0';
    for (i=0; i<32; ++i) {
        if (ctr_mask & (1<<i)) {
	    p += sprintf(p, "%d,", i);
	}
    }
    if (p != ctr_string) {
        *(p-1) = '\0';  /* erase the final comma */
    }
}

void do_list_events()
{
    unsigned int i;
    char ctrs[32*3+1];

    printf("%-12s | %-30s: %s\n", "counter", "name", "meaning");
    printf("----------------------------------------"
           "--------------------------------------\n");
    for (i = 0; i < NEVENTS; i++) {
        stringify_counters(ctrs, event_info[i].counters);
        printf("%-12s | %-30s: %s\n", ctrs, event_info[i].name, event_info[i].explanation);
    }
}

int find_event_idx_from_name(const char *name)
{
    unsigned int i;

    for (i = 0; i < NEVENTS; i++) {
        if (!strcmp(name, event_info[i].name)) {
            return i;
        }
    }
    return -1;
}

const char * find_event_name_from_id(int id, int mask)
{
    unsigned int i;

    for (i = 0; i < NEVENTS; i++) {
	if (event_info[i].id == id && (event_info[i].counters == 0 || (event_info[i].counters & mask))) {
            return event_info[i].name;
        }
    }
    return "Undefined Event";
}

int process_event(const char *event_spec)
{
    char event_name[512];
    char count_name[512];
    unsigned int i;
    int event_idx;
    int count_val;

    strncpy(event_name, event_spec, 512);
    count_name[0] = 0;

    /* First, check if the name is followed by ":" */
    for (i = 0; i < strlen(event_name); i++) {
        if (event_name[i] == 0) {
            break;
        }
        if (event_name[i] == ':') {
            strncpy(count_name, event_name+i+1, 512);
            event_name[i] = 0;
            break;
        }
    }
    event_idx = find_event_idx_from_name(event_name);
    if (event_idx == -1) {
        fprintf(stderr, "Unknown event name: %s\n", event_name);
        return -1;
    }

    /*
     * check that the named event is valid for this event counter
     * 'num_events' represents the cpu internal counter number
     */
    verbose("idx: %d, name: %s, mask: %02x, ctr#: %d\n",
            event_idx, event_info[event_idx].name,
            event_info[event_idx].counters, num_events);
    if (event_info[event_idx].counters != 0 &&
	(event_info[event_idx].counters & CTR(num_events)) == 0) {
	fprintf(stderr, "Bad event name: %s for counter %d, see --list\n",
		event_name, num_events);
	return -1;
    }

    /* Use default count */
    if (count_name[0] == 0) {
        count_val = min_count[0];
    } else {
        count_val = atoi(count_name);
    }

    selected_events[num_events] = event_idx;
    selected_counts[num_events++] = count_val;
    verbose("event_id is %d\n", event_info[event_idx].id);
    verbose("count_val is %d\n", count_val);
    return 0;
}

int echo_dev(const char* str, int val, const char* file, int counter)
{
    char fullname[512];
    char content[128];
    int fd;
    
    if (counter >= 0) {
        snprintf(fullname, 512, OP_DRIVER_BASE"/%d/%s", counter, file);
    }
    else {
        snprintf(fullname, 512, OP_DRIVER_BASE"/%s", file);
    }
    fd = open(fullname, O_WRONLY);
    if (fd<0) {
        fprintf(stderr, "Cannot open %s: %s\n", fullname, strerror(errno));
        return fd;
    }
    if (str == 0) {
        sprintf(content, "%d", val);
    }
    else {
        strncpy(content, str, 128);
    }
    verbose("Configure %s (%s)\n", fullname, content);
    write(fd, content, strlen(content));
    close(fd);
    return 0;
}

void do_status()
{
    int num;
    char fullname[512];
    int i;

    printf("Driver directory: %s\n", OP_DRIVER_BASE);
    printf("Session directory: %s\n", OP_DATA_DIR);
    for (i = 0; i < max_events; i++) {
        sprintf(fullname, OP_DRIVER_BASE"/%d/enabled", i);
        num = read_num(fullname);
        if (num > 0) {
            printf("Counter %d:\n", i);

            /* event name */
            sprintf(fullname, OP_DRIVER_BASE"/%d/event", i);
            num = read_num(fullname);
            printf("    name: %s\n", find_event_name_from_id(num, CTR(i)));

            /* profile interval */
            sprintf(fullname, OP_DRIVER_BASE"/%d/count", i);
            num = read_num(fullname);
            printf("    count: %d\n", num);
        }
        else {
            printf("Counter %d disabled\n", i);
        }
    }

    num = read_num(OP_DATA_DIR"/lock");
    if (num >= 0) {
        /* Still needs to check if this lock is left-over */
        sprintf(fullname, "/proc/%d", num);
        if (access(fullname, R_OK) != 0) {
            printf("OProfile daemon exited prematurely - redo setup"
                   " before you continue\n");
            return;
        }
        else {

            printf("oprofiled pid: %d\n", num);
            num = read_num(OP_DRIVER_BASE"/enable");

            printf("profiler is%s running\n", num == 0 ? " not" : "");

            DIR* dir = opendir(OP_DRIVER_BASE"/stats");
            if (dir) {
                for (struct dirent* dirent; !!(dirent = readdir(dir));) {
                    if (strlen(dirent->d_name) >= 4 && memcmp(dirent->d_name, "cpu", 3) == 0) {
                        char cpupath[256];
                        strcpy(cpupath, OP_DRIVER_BASE"/stats/");
                        strcat(cpupath, dirent->d_name);

                        strcpy(fullname, cpupath);
                        strcat(fullname, "/sample_received");
                        num = read_num(fullname);
                        printf("  %s %9u samples received\n", dirent->d_name, num);

                        strcpy(fullname, cpupath);
                        strcat(fullname, "/sample_lost_overflow");
                        num = read_num(fullname);
                        printf("  %s %9u samples lost overflow\n", dirent->d_name, num);

                        strcpy(fullname, cpupath);
                        strcat(fullname, "/sample_invalid_eip");
                        num = read_num(fullname);
                        printf("  %s %9u samples invalid eip\n", dirent->d_name, num);

                        strcpy(fullname, cpupath);
                        strcat(fullname, "/backtrace_aborted");
                        num = read_num(fullname);
                        printf("  %s %9u backtrace aborted\n", dirent->d_name, num);
                    }
                }
                closedir(dir);
            }

            num = read_num(OP_DRIVER_BASE"/backtrace_depth");
            printf("backtrace_depth: %u\n", num);
        }
    }
    else {
        printf("oprofiled is not running\n");
    }
}

void do_reset() 
{
    /*
     * Sending SIGHUP will result in the following crash in oprofiled when
     * profiling subsequent runs:
     * Stack Trace:
     * RELADDR   FUNCTION                         FILE:LINE
     *   00008cd8  add_node+12                    oprofilelibdb/db_insert.c:32
     *   00008d69  odb_update_node_with_offset+60 oprofilelibdb/db_insert.c:102
     *
     * However without sending SIGHUP oprofile cannot be restarted successfully.
     * As a temporary workaround, change do_reset into a no-op for now and kill
     * the old daemon in do_setup to start all over again as a heavy-weight
     * reset.
     */
#if 0
    int pid = read_num(OP_DATA_DIR"/lock");
    if (pid >= 0)
        kill(pid, SIGHUP);  /* HUP makes oprofiled close its sample files */

    if (access(OP_DATA_DIR"/samples/current", R_OK) == 0)
      system("rm -r "OP_DATA_DIR"/samples/current");
#endif
}

int main(int argc, char * const argv[])
{
    int option_index;
    bool show_status = false;
    char* verbose_log = NULL;

    /* Initialize default strings */
    strcpy(vmlinux, "--no-vmlinux");
    strcpy(kernel_range, "");

    setup_device();

    while (1) {
        int c = getopt_long(argc, argv, "c:e:v:r:dhVtl:", long_options, &option_index);
        if (c == -1) {
            break;
        }
        switch (c) {
            case 0:
                break;
            /* --callgraph */
            case 'c':
                strncpy(callgraph, optarg, sizeof(callgraph));
                break;
            /* --event */
            case 'e':   
                if (num_events == MAX_EVENTS) {
                    fprintf(stderr, "More than %d events specified\n",
                            MAX_EVENTS);
                    exit(1);
                }
                if (process_event(optarg)) {
                    exit(1);
                }
                break;
            /* --vmlinux */
            case 'v':
                sprintf(vmlinux, "-k %s", optarg);
                break;
            /* --kernel-range */
            case 'r':
                sprintf(kernel_range, "-r %s", optarg);
                break;
            case 'd':
            /* --dump */ {
                int pid = read_num(OP_DATA_DIR"/lock");
                echo_dev("1", 0, "dump", -1);
                break;
            }
            /* --shutdown */
            case 'h': {
                int pid = read_num(OP_DATA_DIR"/lock");
                if (pid >= 0) {
                    kill(pid, SIGHUP); /* Politely ask the daemon to close files */
                    sleep(1);
                    kill(pid, SIGTERM);/* Politely ask the daemon to die */
                    sleep(1);
                    kill(pid, SIGKILL);
                }
                setup_session_dir();
                break;
            }
            /* --verbose */
            case 'V':
                verbose_print++;
                break;
            /* --verbose-log */
            case 'l':
                verbose_log = strdup(optarg);
                break;
            /* --status */
            case 't':
                show_status = true;
                break;
            default:
                usage();
                exit(1);
        }
    }
    verbose("list_events = %d\n", list_events);
    verbose("setup = %d\n", setup);

    if (list_events) {
        do_list_events();
    }

    if (quick) {
        process_event(default_event);
        setup = 1;
    }

    if (timer) {
        setup = 1;
    }

    if (reset) {
        do_reset();
    }

    if (show_usage) {
        usage();
    }

    if (setup) {
        if (do_setup()) {
            fprintf(stderr, "do_setup failed");
            exit(1);
        }
    }

    if (strlen(callgraph)) {
        echo_dev(callgraph, 0, "backtrace_depth", -1);
    }

    if (num_events != 0 || timer != 0) {
        char command[1024];
        int i;

        strcpy(command, argv[0]);
        char* slash = strrchr(command, '/');
        strcpy(slash ? slash + 1 : command, "oprofiled --session-dir="OP_DATA_DIR);

#if defined(__arm__) && !defined(WITH_ARM_V7_A)
        /* Since counter #3 can only handle CPU_CYCLES, check and shuffle the 
         * order a bit so that the maximal number of events can be profiled
         * simultaneously
         */
        if (num_events == 3) {
            for (i = 0; i < num_events; i++) {
                int event_idx = selected_events[i];

                if (event_info[event_idx].id == 0xff) {
                    break;
                }
            }

            /* No CPU_CYCLES is found */
            if (i == 3) {
                fprintf(stderr, "You can only specify three events if one of "
                                "them is CPU_CYCLES\n");
                exit(1);
            }
            /* Swap CPU_CYCLES to counter #2 (starting from #0)*/
            else if (i != 2) {
                int temp;

                temp = selected_events[2];
                selected_events[2] = selected_events[i];
                selected_events[i] = temp;

                temp = selected_counts[2];
                selected_counts[2] = selected_counts[i];
                selected_counts[i] = temp;
            }
        }
#endif

        /* Configure the counters and enable them */
        for (i = 0; i < num_events; i++) {
            int event_idx = selected_events[i];
            int setup_result = 0;

            if (i == 0) {
                snprintf(command + strlen(command), sizeof(command) - strlen(command),
                        " --events=");
            } else {
                snprintf(command + strlen(command), sizeof(command) - strlen(command), ",");
            }
            /* Compose name:id:count:unit_mask:kernel:user, something like
             * --events=CYCLES_DATA_STALL:2:0:200000:0:1:1,....
             */
            snprintf(command + strlen(command), sizeof(command) - strlen(command),
                     "%s:%d:%d:%d:%d:1:1",
                     event_info[event_idx].name,
                     event_info[event_idx].id,
                     i,
                     selected_counts[i],
                     event_info[event_idx].um);

            setup_result |= echo_dev("1", 0, "user", i);
            setup_result |= echo_dev("1", 0, "kernel", i);
            setup_result |= echo_dev(NULL, event_info[event_idx].um, "unit_mask", i);
            setup_result |= echo_dev("1", 0, "enabled", i);
            setup_result |= echo_dev(NULL, selected_counts[i], "count", i);
            setup_result |= echo_dev(NULL, event_info[event_idx].id, 
                                     "event", i);
            if (setup_result) {
                fprintf(stderr, "Counter configuration failed for %s\n",
                        event_info[event_idx].name);
                fprintf(stderr, "Did you do \"opcontrol --setup\" first?\n");
                exit(1);
            }
        }

        if (timer == 0) {
            /* If not in timer mode, disable unused counters */
            for (i = num_events; i < max_events; i++) {
                echo_dev("0", 0, "enabled", i);
            }
        } else {
            /* Timer mode uses empty event list */
            snprintf(command + strlen(command), sizeof(command) - strlen(command),
                    " --events=");
        }

        snprintf(command + strlen(command), sizeof(command) - strlen(command),
                " %s", vmlinux);
        if (kernel_range[0]) {
            snprintf(command + strlen(command), sizeof(command) - strlen(command),
                    " %s", kernel_range);
        }

        if (verbose_log) {
            snprintf(command + strlen(command), sizeof(command) - strlen(command),
                    " --verbose=%s", verbose_log);
        }

        printf("Starting oprofiled...\n");
        verbose("command: %s\n", command);

        int rc = system(command);
        if (rc) {
            fprintf(stderr, "Failed, oprofile returned exit code: %d\n", rc);
        } else {
            sleep(2);
            printf("Ready\n");
        }
    }

    if (start) {
        echo_dev("1", 0, "enable", -1);
        int num = read_num(OP_DATA_DIR"/lock");

        if (num >= 0) {
            kill(num, SIGUSR1);
        }
    }

    if (stop) {
        echo_dev("1", 0, "dump", -1);
        echo_dev("0", 0, "enable", -1);
    }

    if (show_status) {
        do_status();
    }
}
