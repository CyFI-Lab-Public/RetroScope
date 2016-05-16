/**
 * @file opd_perfmon.c
 * perfmonctl() handling
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 */

#ifdef __ia64__

/* need this for sched_setaffinity() in <sched.h> */
#define _GNU_SOURCE

#include "oprofiled.h"
#include "opd_perfmon.h"
#include "opd_events.h"

#include "op_cpu_type.h"
#include "op_libiberty.h"
#include "op_hw_config.h"

#include <sys/syscall.h>
#include <sys/wait.h>
#include <unistd.h>
#include <limits.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#ifdef HAVE_SCHED_SETAFFINITY
#include <sched.h>
#endif

extern op_cpu cpu_type;

#ifndef HAVE_SCHED_SETAFFINITY

/* many glibc's are not yet up to date */
#ifndef __NR_sched_setaffinity
#define __NR_sched_setaffinity 1231
#endif

/* Copied from glibc's <sched.h> and <bits/sched.h> and munged */
#define CPU_SETSIZE	1024
#define __NCPUBITS	(8 * sizeof (unsigned long))
typedef struct
{
	unsigned long __bits[CPU_SETSIZE / __NCPUBITS];
} cpu_set_t;

#define CPU_SET(cpu, cpusetp) \
	((cpusetp)->__bits[(cpu)/__NCPUBITS] |= (1UL << ((cpu) % __NCPUBITS)))
#define CPU_ZERO(cpusetp) \
	memset((cpusetp), 0, sizeof(cpu_set_t))

static int
sched_setaffinity(pid_t pid, size_t len, cpu_set_t const * cpusetp)
{
	return syscall(__NR_sched_setaffinity, pid, len, cpusetp);
}
#endif


#ifndef HAVE_PERFMONCTL
#ifndef __NR_perfmonctl
#define __NR_perfmonctl 1175
#endif

static int perfmonctl(int fd, int cmd, void * arg, int narg)
{
	return syscall(__NR_perfmonctl, fd, cmd, arg, narg);
}
#endif


static unsigned char uuid[16] = {
	0x77, 0x7a, 0x6e, 0x61, 0x20, 0x65, 0x73, 0x69,
	0x74, 0x6e, 0x72, 0x20, 0x61, 0x65, 0x0a, 0x6c
};


static size_t nr_cpus;

struct child {
	pid_t pid;
	int up_pipe[2];
	int ctx_fd;
	sig_atomic_t sigusr1;
	sig_atomic_t sigusr2;
	sig_atomic_t sigterm;
};

static struct child * children;

static void perfmon_start_child(int ctx_fd)
{
	if (perfmonctl(ctx_fd, PFM_START, 0, 0) == -1) {
		exit(EXIT_FAILURE);
	}
}


static void perfmon_stop_child(int ctx_fd)
{
	if (perfmonctl(ctx_fd, PFM_STOP, 0, 0) == -1) {
		exit(EXIT_FAILURE);
	}
}


static void child_sigusr1(int val __attribute__((unused)))
{
	size_t i;

	for (i = 0; i < nr_cpus; ++i) {
		if (children[i].pid == getpid()) {
			children[i].sigusr1 = 1;
			return;
		}
	}
}


static void child_sigusr2(int val __attribute__((unused)))
{
	size_t i;

	for (i = 0; i < nr_cpus; ++i) {
		if (children[i].pid == getpid()) {
			children[i].sigusr2 = 1;
			return;
		}
	}
}


static void child_sigterm(int val __attribute__((unused)))
{
	kill(getppid(), SIGTERM);
}


static void set_affinity(size_t cpu)
{
	cpu_set_t set;
	int err;

	CPU_ZERO(&set);
	CPU_SET(cpu, &set);

	err = sched_setaffinity(getpid(), sizeof(set), &set);

	if (err == -1) {
		perror("Failed to set affinity");
		exit(EXIT_FAILURE);
	}
}


static void setup_signals(void)
{
	struct sigaction act;
	sigset_t mask;

	sigemptyset(&mask);
	sigaddset(&mask, SIGUSR1);
	sigaddset(&mask, SIGUSR2);
	sigprocmask(SIG_BLOCK, &mask, NULL);

	act.sa_handler = child_sigusr1;
	act.sa_flags = 0;
	sigemptyset(&act.sa_mask);

	if (sigaction(SIGUSR1, &act, NULL)) {
		perror("oprofiled: install of SIGUSR1 handler failed");
		exit(EXIT_FAILURE);
	}

	act.sa_handler = child_sigusr2;
	act.sa_flags = 0;
	sigemptyset(&act.sa_mask);

	if (sigaction(SIGUSR2, &act, NULL)) {
		perror("oprofiled: install of SIGUSR2 handler failed");
		exit(EXIT_FAILURE);
	}

	act.sa_handler = child_sigterm;
	act.sa_flags = 0;
	sigemptyset(&act.sa_mask);

	if (sigaction(SIGTERM, &act, NULL)) {
		perror("oprofiled: install of SIGTERM handler failed");
		exit(EXIT_FAILURE);
	}
}


/** create the per-cpu context */
static void create_context(struct child * self)
{
	pfarg_context_t ctx;
	int err;

	memset(&ctx, 0, sizeof(pfarg_context_t));
	memcpy(&ctx.ctx_smpl_buf_id, &uuid, 16);
	ctx.ctx_flags = PFM_FL_SYSTEM_WIDE;

	err = perfmonctl(0, PFM_CREATE_CONTEXT, &ctx, 1);
	if (err == -1) {
		perror("CREATE_CONTEXT failed");
		exit(EXIT_FAILURE);
	}

	self->ctx_fd = ctx.ctx_fd;
}


/** program the perfmon counters */
static void write_pmu(struct child * self)
{
	pfarg_reg_t pc[OP_MAX_COUNTERS];
	pfarg_reg_t pd[OP_MAX_COUNTERS];
	int err;
	size_t i;

	memset(pc, 0, sizeof(pc));
	memset(pd, 0, sizeof(pd));

#define PMC_GEN_INTERRUPT (1UL << 5)
#define PMC_PRIV_MONITOR (1UL << 6)
/* McKinley requires pmc4 to have bit 23 set (enable PMU).
 * It is supposedly ignored in other pmc registers.
 */
#define PMC_MANDATORY (1UL << 23)
#define PMC_USER (1UL << 3)
#define PMC_KERNEL (1UL << 0)
	for (i = 0; i < op_nr_counters && opd_events[i].name; ++i) {
		struct opd_event * event = &opd_events[i];
		pc[i].reg_num = event->counter + 4;
		pc[i].reg_value = PMC_GEN_INTERRUPT;
		pc[i].reg_value |= PMC_PRIV_MONITOR;
		pc[i].reg_value |= PMC_MANDATORY;
		(event->user) ? (pc[i].reg_value |= PMC_USER)
		              : (pc[i].reg_value &= ~PMC_USER);
		(event->kernel) ? (pc[i].reg_value |= PMC_KERNEL)
		                : (pc[i].reg_value &= ~PMC_KERNEL);
		pc[i].reg_value &= ~(0xff << 8);
		pc[i].reg_value |= ((event->value & 0xff) << 8);
		pc[i].reg_value &= ~(0xf << 16);
		pc[i].reg_value |= ((event->um & 0xf) << 16);
		pc[i].reg_smpl_eventid = event->counter;
	}

	for (i = 0; i < op_nr_counters && opd_events[i].name; ++i) {
		struct opd_event * event = &opd_events[i];
		pd[i].reg_value = ~0UL - event->count + 1;
		pd[i].reg_short_reset = ~0UL - event->count + 1;
		pd[i].reg_num = event->counter + 4;
	}

	err = perfmonctl(self->ctx_fd, PFM_WRITE_PMCS, pc, i);
	if (err == -1) {
		perror("Couldn't write PMCs");
		exit(EXIT_FAILURE);
	}

	err = perfmonctl(self->ctx_fd, PFM_WRITE_PMDS, pd, i);
	if (err == -1) {
		perror("Couldn't write PMDs");
		exit(EXIT_FAILURE);
	}
}


static void load_context(struct child * self)
{
	pfarg_load_t load_args;
	int err;

	memset(&load_args, 0, sizeof(load_args));
	load_args.load_pid = self->pid;

	err = perfmonctl(self->ctx_fd, PFM_LOAD_CONTEXT, &load_args, 1);
	if (err == -1) {
		perror("Couldn't load context");
		exit(EXIT_FAILURE);
	}
}


static void notify_parent(struct child * self, size_t cpu)
{
	for (;;) {
		ssize_t ret;
		ret = write(self->up_pipe[1], &cpu, sizeof(size_t));
		if (ret == sizeof(size_t))
			break;
		if (ret < 0 && errno != EINTR) {
			perror("Failed to write child pipe:");
			exit(EXIT_FAILURE);
		}
	}
}

static struct child * inner_child;
void close_pipe(void)
{
	close(inner_child->up_pipe[1]);
}

static void run_child(size_t cpu)
{
	struct child * self = &children[cpu];

	self->pid = getpid();
	self->sigusr1 = 0;
	self->sigusr2 = 0;
	self->sigterm = 0;

	inner_child = self;
	if (atexit(close_pipe)){
		close_pipe();
		exit(EXIT_FAILURE);
	}

	umask(0);
	/* Change directory to allow directory to be removed */
	if (chdir("/") < 0) {
		perror("Unable to chdir to \"/\"");
		exit(EXIT_FAILURE);
	}

	setup_signals();

	set_affinity(cpu);

	create_context(self);

	write_pmu(self);

	load_context(self);

	notify_parent(self, cpu);

	/* Redirect standard files to /dev/null */
	freopen( "/dev/null", "r", stdin);
	freopen( "/dev/null", "w", stdout);
	freopen( "/dev/null", "w", stderr);

	for (;;) {
		sigset_t sigmask;
		sigfillset(&sigmask);
		sigdelset(&sigmask, SIGUSR1);
		sigdelset(&sigmask, SIGUSR2);
		sigdelset(&sigmask, SIGTERM);

		if (self->sigusr1) {
			perfmon_start_child(self->ctx_fd);
			self->sigusr1 = 0;
		}

		if (self->sigusr2) {
			perfmon_stop_child(self->ctx_fd);
			self->sigusr2 = 0;
		}

		sigsuspend(&sigmask);
	}
}


static void wait_for_child(struct child * child)
{
	size_t tmp;
	for (;;) {
		ssize_t ret;
		ret = read(child->up_pipe[0], &tmp, sizeof(size_t));
		if (ret == sizeof(size_t))
			break;
		if ((ret < 0 && errno != EINTR) || ret == 0 ) {
			perror("Failed to read child pipe");
			exit(EXIT_FAILURE);
		}
	}
	printf("Perfmon child up on CPU%d\n", (int)tmp);
	fflush(stdout);

	close(child->up_pipe[0]);
}

static struct child* xen_ctx;

void perfmon_init(void)
{
	size_t i;
	long nr;

	if (cpu_type == CPU_TIMER_INT)
		return;

	if (!no_xen) {
		xen_ctx = xmalloc(sizeof(struct child));
		xen_ctx->pid = getpid();
		xen_ctx->up_pipe[0] = -1;
		xen_ctx->up_pipe[1] = -1;
		xen_ctx->sigusr1 = 0;
		xen_ctx->sigusr2 = 0;
		xen_ctx->sigterm = 0;

		create_context(xen_ctx);

		write_pmu(xen_ctx);
		
		load_context(xen_ctx);
		return;
	}
	

	nr = sysconf(_SC_NPROCESSORS_ONLN);
	if (nr == -1) {
		fprintf(stderr, "Couldn't determine number of CPUs.\n");
		exit(EXIT_FAILURE);
	}

	nr_cpus = nr;

	children = xmalloc(sizeof(struct child) * nr_cpus);
	bzero(children, sizeof(struct child) * nr_cpus);

	for (i = 0; i < nr_cpus; ++i) {
		int ret;

		if (pipe(children[i].up_pipe)) {
			perror("Couldn't create child pipe");
			exit(EXIT_FAILURE);
		}

		ret = fork();
		if (ret == -1) {
			perror("Couldn't fork perfmon child");
			exit(EXIT_FAILURE);
		} else if (ret == 0) {
			close(children[i].up_pipe[0]);
			run_child(i);
		} else {
			children[i].pid = ret;
			close(children[i].up_pipe[1]);
			printf("Waiting on CPU%d\n", (int)i);
			wait_for_child(&children[i]);
		}
	}
}


void perfmon_exit(void)
{
	size_t i;

	if (cpu_type == CPU_TIMER_INT)
		return;

	if (!no_xen)
		return;

	for (i = 0; i < nr_cpus; ++i) {
		if (children[i].pid) {
			int c_pid = children[i].pid;
			children[i].pid = 0;
			if (kill(c_pid, SIGKILL)==0)
				waitpid(c_pid, NULL, 0);
		}
	}
}


void perfmon_start(void)
{
	size_t i;

	if (cpu_type == CPU_TIMER_INT)
		return;

	if (!no_xen) {
		perfmon_start_child(xen_ctx->ctx_fd);
		return;
	}

	for (i = 0; i < nr_cpus; ++i) {
		if (kill(children[i].pid, SIGUSR1)) {
			perror("Unable to start perfmon");
			exit(EXIT_FAILURE);
		}
	}
}


void perfmon_stop(void)
{
	size_t i;

	if (cpu_type == CPU_TIMER_INT)
		return;

	if (!no_xen) {
		perfmon_stop_child(xen_ctx->ctx_fd);
		return;
	}
	
	for (i = 0; i < nr_cpus; ++i)
		if (kill(children[i].pid, SIGUSR2)) {
			perror("Unable to stop perfmon");
			exit(EXIT_FAILURE);
		}
}

#endif /* __ia64__ */
