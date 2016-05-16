/*
 * Blktrace replay utility - Play traces back
 *
 * Copyright (C) 2007 Alan D. Brunelle <Alan.Brunelle@hp.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

static char build_date[] = __DATE__ " at "__TIME__;

#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <libaio.h>
#include <pthread.h>
#include <sched.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <sys/param.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/types.h>
#include <dirent.h>
#include <stdarg.h>

#if !defined(_GNU_SOURCE)
#	define _GNU_SOURCE
#endif
#include <getopt.h>

#include "list.h"
#include "btrecord.h"

/* 
 * ========================================================================
 * ==== STRUCTURE DEFINITIONS =============================================
 * ========================================================================
 */

/**
 * Each device map has one of these:
 * 
 * @head:	Linked on to map_devs
 * @from_dev:	Device name as seen on recorded system
 * @to_dev:	Device name to be used on replay system
 */
struct map_dev {
	struct list_head head;
	char *from_dev, *to_dev;
};

/**
 * Each device name specified has one of these (until threads are created)
 *
 * @head: 	Linked onto input_devs
 * @devnm: 	Device name -- 'sd*'
 */
struct dev_info {
	struct list_head head;
	char *devnm;
};

/*
 * Per input file information
 *
 * @head: 	Used to link up on input_files
 * @free_iocbs: List of free iocb's available for use
 * @used_iocbs: List of iocb's currently outstanding
 * @mutex: 	Mutex used with condition variable to protect volatile values
 * @cond: 	Condition variable used when waiting on a volatile value change
 * @naios_out: 	Current number of AIOs outstanding on this context
 * @naios_free: Number of AIOs on the free list (short cut for list_len)
 * @send_wait: 	Boolean: When true, the sub thread is waiting on free IOCBs
 * @reap_wait: 	Boolean: When true, the rec thread is waiting on used IOCBs
 * @send_done: 	Boolean: When true, the sub thread has completed work
 * @reap_done: 	Boolean: When true, the rec thread has completed work
 * @sub_thread: Thread used to submit IOs.
 * @rec_thread: Thread used to reclaim IOs.
 * @ctx: 	IO context
 * @devnm: 	Copy of the device name being managed by this thread
 * @file_name: 	Full name of the input file
 * @cpu: 	CPU this thread is pinned to
 * @ifd: 	Input file descriptor
 * @ofd: 	Output file descriptor
 * @iterations: Remaining iterations to process
 * @vfp:	For verbose dumping of actions performed
 */
struct thr_info {
	struct list_head head, free_iocbs, used_iocbs;
	pthread_mutex_t mutex;
	pthread_cond_t cond;
	volatile long naios_out, naios_free;
	volatile int send_wait, reap_wait, send_done, reap_done;
	pthread_t sub_thread, rec_thread;
	io_context_t ctx;
	char *devnm, *file_name;
	int cpu, ifd, ofd, iterations;
	FILE *vfp;
};

/*
 * Every Asynchronous IO used has one of these (naios per file/device).
 *
 * @iocb:	IOCB sent down via io_submit
 * @head:	Linked onto file_list.free_iocbs or file_list.used_iocbs
 * @tip:	Pointer to per-thread information this IO is associated with
 * @nbytes:	Number of bytes in buffer associated with iocb
 */
struct iocb_pkt {
	struct iocb iocb;
	struct list_head head;
	struct thr_info *tip;
	int nbytes;
};

/* 
 * ========================================================================
 * ==== GLOBAL VARIABLES ==================================================
 * ========================================================================
 */

static volatile int signal_done = 0;	// Boolean: Signal'ed, need to quit

static char *ibase = "replay";		// Input base name
static char *idir = ".";		// Input directory base
static int cpus_to_use = -1;		// Number of CPUs to use
static int def_iterations = 1;		// Default number of iterations
static int naios = 512;			// Number of AIOs per thread
static int ncpus = 0;			// Number of CPUs in the system
static int verbose = 0;			// Boolean: Output some extra info
static int write_enabled = 0;		// Boolean: Enable writing
static __u64 genesis = ~0;		// Earliest time seen
static __u64 rgenesis;			// Our start time
static size_t pgsize;			// System Page size
static int nb_sec = 512;		// Number of bytes per sector
static LIST_HEAD(input_devs);		// List of devices to handle
static LIST_HEAD(input_files);		// List of input files to handle
static LIST_HEAD(map_devs);		// List of device maps
static int nfiles = 0;			// Number of files to handle
static int no_stalls = 0;		// Boolean: Disable pre-stalls
static unsigned acc_factor = 1;		// Int: Acceleration factor
static int find_records = 0;		// Boolean: Find record files auto

/*
 * Variables managed under control of condition variables.
 *
 * n_reclaims_done: 	Counts number of reclaim threads that have completed.
 * n_replays_done:	Counts number of replay threads that have completed.
 * n_replays_ready:	Counts number of replay threads ready to start.
 * n_iters_done:	Counts number of replay threads done one iteration.
 * iter_start:		Starts an iteration for the replay threads.
 */
static volatile int n_reclaims_done = 0;
static pthread_mutex_t reclaim_done_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t reclaim_done_cond = PTHREAD_COND_INITIALIZER;

static volatile int n_replays_done = 0;
static pthread_mutex_t replay_done_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t replay_done_cond = PTHREAD_COND_INITIALIZER;

static volatile int n_replays_ready = 0;
static pthread_mutex_t replay_ready_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t replay_ready_cond = PTHREAD_COND_INITIALIZER;

static volatile int n_iters_done = 0;
static pthread_mutex_t iter_done_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t iter_done_cond = PTHREAD_COND_INITIALIZER;

static volatile int iter_start = 0;
static pthread_mutex_t iter_start_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t iter_start_cond = PTHREAD_COND_INITIALIZER;

/* 
 * ========================================================================
 * ==== FORWARD REFERENECES ===============================================
 * ========================================================================
 */

static void *replay_sub(void *arg);
static void *replay_rec(void *arg);
static char usage_str[];

/* 
 * ========================================================================
 * ==== INLINE ROUTINES ===================================================
 * ========================================================================
 */

/*
 * The 'fatal' macro will output a perror message (if errstring is !NULL)
 * and display a string (with variable arguments) and then exit with the 
 * specified exit value.
 */
#define ERR_ARGS			1
#define ERR_SYSCALL			2
static inline void fatal(const char *errstring, const int exitval,
			 const char *fmt, ...)
{
	va_list ap;

	if (errstring)
		perror(errstring);

	va_start(ap, fmt);
	vfprintf(stderr, fmt, ap);
	va_end(ap);

	exit(exitval);
	/*NOTREACHED*/
}

static inline long long unsigned du64_to_sec(__u64 du64)
{
	return (long long unsigned)du64 / (1000 * 1000 * 1000);
}

static inline long long unsigned du64_to_nsec(__u64 du64)
{
	return llabs((long long)du64) % (1000 * 1000 * 1000);
}

/**
 * min - Return minimum of two integers
 */
static inline int min(int a, int b)
{ 
	return a < b ? a : b;
}

/**
 * minl - Return minimum of two longs
 */
static inline long minl(long a, long b)
{ 
	return a < b ? a : b;
}

/**
 * usage - Display usage string and version
 */
static inline void usage(void)
{
	fprintf(stderr, "Usage: btreplay -- version %s\n%s", 
		my_btversion, usage_str);
}

/**
 * is_send_done - Returns true if sender should quit early
 * @tip: Per-thread information
 */
static inline int is_send_done(struct thr_info *tip)
{
	return signal_done || tip->send_done;
}

/**
 * is_reap_done - Returns true if reaper should quit early
 * @tip: Per-thread information
 */
static inline int is_reap_done(struct thr_info *tip)
{
	return tip->send_done && tip->naios_out == 0;
}

/**
 * ts2ns - Convert timespec values to a nanosecond value
 */
#define NS_TICKS		((__u64)1000 * (__u64)1000 * (__u64)1000)
static inline __u64 ts2ns(struct timespec *ts)
{
	return ((__u64)(ts->tv_sec) * NS_TICKS) + (__u64)(ts->tv_nsec);
}

/**
 * ts2ns - Convert timeval values to a nanosecond value
 */
static inline __u64 tv2ns(struct timeval *tp)
{
	return ((__u64)(tp->tv_sec)) + ((__u64)(tp->tv_usec) * (__u64)1000);
}

/**
 * touch_memory - Force physical memory to be allocating it
 * 
 * For malloc()ed memory we need to /touch/ it to make it really
 * exist. Otherwise, for write's (to storage) things may not work
 * as planned - we see Linux just use a single area to /read/ from
 * (as there isn't any memory that has been associated with the 
 * allocated virtual addresses yet).
 */
static inline void touch_memory(char *buf, size_t bsize)
{
#if defined(PREP_BUFS)
	memset(buf, 0, bsize);
#else
	size_t i;

	for (i = 0; i < bsize; i += pgsize)
		buf[i] = 0;
#endif
}

/**
 * buf_alloc - Returns a page-aligned buffer of the specified size
 * @nbytes: Number of bytes to allocate
 */
static inline void *buf_alloc(size_t nbytes)
{
	void *buf;

	if (posix_memalign(&buf, pgsize, nbytes)) {
		fatal("posix_memalign", ERR_SYSCALL, "Allocation failed\n");
		/*NOTREACHED*/
	}

	return buf;
}

/**
 * gettime - Returns current time 
 */
static inline __u64 gettime(void)
{
	static int use_clock_gettime = -1;		// Which clock to use

	if (use_clock_gettime < 0) {
		use_clock_gettime = clock_getres(CLOCK_MONOTONIC, NULL) == 0;
		if (use_clock_gettime) {
			struct timespec ts = {
				.tv_sec = 0,
				.tv_nsec = 0
			};
			clock_settime(CLOCK_MONOTONIC, &ts);
		}
	}

	if (use_clock_gettime) {
		struct timespec ts;
		clock_gettime(CLOCK_MONOTONIC, &ts);
		return ts2ns(&ts);
	}
	else {
		struct timeval tp;
		gettimeofday(&tp, NULL);
		return tv2ns(&tp);
	}
}

/**
 * setup_signal - Set up a signal handler for the specified signum
 */
static inline void setup_signal(int signum, sighandler_t handler)
{
	if (signal(signum, handler) == SIG_ERR) {
		fatal("signal", ERR_SYSCALL, "Failed to set signal %d\n",
			signum);
		/*NOTREACHED*/
	}
}

/* 
 * ========================================================================
 * ==== CONDITION VARIABLE ROUTINES =======================================
 * ========================================================================
 */

/**
 * __set_cv - Increments a variable under condition variable control.
 * @pmp: 	Pointer to the associated mutex
 * @pcp: 	Pointer to the associated condition variable
 * @vp: 	Pointer to the variable being incremented
 * @mxv: 	Max value for variable (Used only when ASSERTS are on)
 */
static inline void __set_cv(pthread_mutex_t *pmp, pthread_cond_t *pcp,
			    volatile int *vp, 
			    __attribute__((__unused__))int mxv)
{
	pthread_mutex_lock(pmp);
	assert(*vp < mxv);
	*vp += 1;
	pthread_cond_signal(pcp);
	pthread_mutex_unlock(pmp);
}

/**
 * __wait_cv - Waits for a variable under cond var control to hit a value
 * @pmp: 	Pointer to the associated mutex
 * @pcp: 	Pointer to the associated condition variable
 * @vp: 	Pointer to the variable being incremented
 * @mxv: 	Value to wait for
 */
static inline void __wait_cv(pthread_mutex_t *pmp, pthread_cond_t *pcp,
			     volatile int *vp, int mxv)
{
	pthread_mutex_lock(pmp);
	while (*vp < mxv)
		pthread_cond_wait(pcp, pmp);
	*vp = 0;
	pthread_mutex_unlock(pmp);
}

static inline void set_reclaim_done(void)
{
	__set_cv(&reclaim_done_mutex, &reclaim_done_cond, &n_reclaims_done,
		 nfiles);
}

static inline void wait_reclaims_done(void)
{
	__wait_cv(&reclaim_done_mutex, &reclaim_done_cond, &n_reclaims_done,
		  nfiles);
}

static inline void set_replay_ready(void)
{
	__set_cv(&replay_ready_mutex, &replay_ready_cond, &n_replays_ready,
		 nfiles);
}

static inline void wait_replays_ready(void)
{
	__wait_cv(&replay_ready_mutex, &replay_ready_cond, &n_replays_ready,
		  nfiles);
}

static inline void set_replay_done(void)
{
	__set_cv(&replay_done_mutex, &replay_done_cond, &n_replays_done,
		nfiles);
}

static inline void wait_replays_done(void)
{
	__wait_cv(&replay_done_mutex, &replay_done_cond, &n_replays_done,
		  nfiles);
}

static inline void set_iter_done(void)
{
	__set_cv(&iter_done_mutex, &iter_done_cond, &n_iters_done,
		nfiles);
}

static inline void wait_iters_done(void)
{
	__wait_cv(&iter_done_mutex, &iter_done_cond, &n_iters_done,
		  nfiles);
}

/**
 * wait_iter_start - Wait for an iteration to start 
 * 
 * This is /slightly/ different: we are waiting for a value to become
 * non-zero, and then we decrement it and go on. 
 */
static inline void wait_iter_start(void)
{
	pthread_mutex_lock(&iter_start_mutex);
	while (iter_start == 0)
		pthread_cond_wait(&iter_start_cond, &iter_start_mutex);
	assert(1 <= iter_start && iter_start <= nfiles);
	iter_start--;
	pthread_mutex_unlock(&iter_start_mutex);
}

/**
 * start_iter - Start an iteration at the replay thread level
 */
static inline void start_iter(void)
{
	pthread_mutex_lock(&iter_start_mutex);
	assert(iter_start == 0);
	iter_start = nfiles;
	pthread_cond_broadcast(&iter_start_cond);
	pthread_mutex_unlock(&iter_start_mutex);
}

/* 
 * ========================================================================
 * ==== CPU RELATED ROUTINES ==============================================
 * ========================================================================
 */

/**
 * get_ncpus - Sets up the global 'ncpus' value
 */
static void get_ncpus(void)
{
	cpu_set_t cpus;

	if (sched_getaffinity(getpid(), sizeof(cpus), &cpus)) {
		fatal("sched_getaffinity", ERR_SYSCALL, "Can't get CPU info\n");
		/*NOTREACHED*/
	}

	/*
	 * XXX This assumes (perhaps wrongly) that there are no /holes/ 
	 * XXX in the mask.
	 */
	for (ncpus = 0; ncpus < CPU_SETSIZE && CPU_ISSET(ncpus, &cpus); ncpus++)
		;
	if (ncpus == 0) {
		fatal(NULL, ERR_SYSCALL, "Insufficient number of CPUs\n");
		/*NOTREACHED*/
	}
}

/**
 * pin_to_cpu - Pin this thread to a specific CPU
 * @tip: Thread information
 */
static void pin_to_cpu(struct thr_info *tip)
{
	cpu_set_t cpus;

	assert(0 <= tip->cpu && tip->cpu < ncpus);

	CPU_ZERO(&cpus);
	CPU_SET(tip->cpu, &cpus);
	if (sched_setaffinity(getpid(), sizeof(cpus), &cpus)) {
		fatal("sched_setaffinity", ERR_SYSCALL, "Failed to pin CPU\n");
		/*NOTREACHED*/
	}

	if (verbose > 1) {
		int i;
		cpu_set_t now;

		(void)sched_getaffinity(getpid(), sizeof(now), &now);
		fprintf(tip->vfp, "Pinned to CPU %02d ", tip->cpu);
		for (i = 0; i < ncpus; i++)
			fprintf(tip->vfp, "%1d", CPU_ISSET(i, &now));
		fprintf(tip->vfp, "\n");
	}
}

/* 
 * ========================================================================
 * ==== INPUT DEVICE HANDLERS =============================================
 * ========================================================================
 */

/**
 * add_input_dev - Add a device ('sd*') to the list of devices to handle
 */
static void add_input_dev(char *devnm)
{
	struct list_head *p;
	struct dev_info *dip;

	__list_for_each(p, &input_devs) {
		dip = list_entry(p, struct dev_info, head);
		if (strcmp(dip->devnm, devnm) == 0)
			return;
	}

	dip = malloc(sizeof(*dip));
	dip->devnm = strdup(devnm);
	list_add_tail(&dip->head, &input_devs);
}

/**
 * rem_input_dev - Remove resources associated with this device
 */
static void rem_input_dev(struct dev_info *dip)
{
	list_del(&dip->head);
	free(dip->devnm);
	free(dip);
}

static void find_input_devs(char *idir)
{
	struct dirent *ent;
	DIR *dir = opendir(idir);

	if (dir == NULL) {
		fatal(idir, ERR_ARGS, "Unable to open %s\n", idir);
		/*NOTREACHED*/
	}

	while ((ent = readdir(dir)) != NULL) {
		char *p, *dsf = malloc(256);

		if (strstr(ent->d_name, ".replay.") == NULL)
			continue;

		dsf = strdup(ent->d_name);
		p = index(dsf, '.');
		assert(p != NULL);
		*p = '\0';
		add_input_dev(dsf);
		free(dsf);
	}

	closedir(dir);
}

/* 
 * ========================================================================
 * ==== MAP DEVICE INTERFACES =============================================
 * ========================================================================
 */

/**
 * read_map_devs - Read in a set of device mapping from the provided file.
 * @file_name:	File containing device maps
 *
 * We support the notion of multiple such files being specifed on the cmd line
 */
static void read_map_devs(char *file_name)
{
	FILE *fp;
	char *from_dev, *to_dev;

	fp = fopen(file_name, "r");
	if (!fp) {
		fatal(file_name, ERR_SYSCALL, "Could not open map devs file\n");
		/*NOTREACHED*/
	}

	while (fscanf(fp, "%as %as", &from_dev, &to_dev) == 2) {
		struct map_dev *mdp = malloc(sizeof(*mdp));

		mdp->from_dev = from_dev;
		mdp->to_dev = to_dev;
		list_add_tail(&mdp->head, &map_devs);
	}

	fclose(fp);
}

/**
 * release_map_devs - Release resources associated with device mappings.
 */
static void release_map_devs(void)
{
	struct list_head *p, *q;

	list_for_each_safe(p, q, &map_devs) {
		struct map_dev *mdp = list_entry(p, struct map_dev, head);

		list_del(&mdp->head);

		free(mdp->from_dev);
		free(mdp->to_dev);
		free(mdp);
	}
}

/**
 * map_dev - Return the mapped device for that specified
 * @from_dev:	Device name as seen on recorded system
 *
 * Note: If there is no such mapping, we return the same name.
 */
static char *map_dev(char *from_dev)
{
	struct list_head *p;

	__list_for_each(p, &map_devs) {
		struct map_dev *mdp = list_entry(p, struct map_dev, head);

		if (strcmp(from_dev, mdp->from_dev) == 0)
			return mdp->to_dev;
	}

	return from_dev;
}

/* 
 * ========================================================================
 * ==== IOCB MANAGEMENT ROUTINES ==========================================
 * ========================================================================
 */

/**
 * iocb_init - Initialize the fields of an IOCB
 * @tip: Per-thread information
 * iocbp: IOCB pointer to update
 */
static void iocb_init(struct thr_info *tip, struct iocb_pkt *iocbp)
{
	iocbp->tip = tip;
	iocbp->nbytes = 0;
	iocbp->iocb.u.c.buf = NULL;
}

/**
 * iocb_setup - Set up an iocb with this AIOs information
 * @iocbp: IOCB pointer to update
 * @rw: Direction (0 == write, 1 == read)
 * @n: Number of bytes to transfer
 * @off: Offset (in bytes)
 */
static void iocb_setup(struct iocb_pkt *iocbp, int rw, int n, long long off)
{
	char *buf;
	struct iocb *iop = &iocbp->iocb;

	assert(rw == 0 || rw == 1);
	assert(0 < n && (n % nb_sec) == 0);
	assert(0 <= off);

	if (iocbp->nbytes) {
		if (iocbp->nbytes >= n) {
			buf = iop->u.c.buf;
			goto prep;
		}

		assert(iop->u.c.buf);
		free(iop->u.c.buf);
	}

	buf = buf_alloc(n);
	iocbp->nbytes = n;

prep:
	if (rw)
		io_prep_pread(iop, iocbp->tip->ofd, buf, n, off);
	else {
		assert(write_enabled);
		io_prep_pwrite(iop, iocbp->tip->ofd, buf, n, off);
		touch_memory(buf, n);
	}

	iop->data = iocbp;
}

/* 
 * ========================================================================
 * ==== PER-THREAD SET UP & TEAR DOWN =====================================
 * ========================================================================
 */

/**
 * tip_init - Per thread initialization function
 */
static void tip_init(struct thr_info *tip)
{
	int i;

	INIT_LIST_HEAD(&tip->free_iocbs);
	INIT_LIST_HEAD(&tip->used_iocbs);

	pthread_mutex_init(&tip->mutex, NULL);
	pthread_cond_init(&tip->cond, NULL);

	if (io_setup(naios, &tip->ctx)) {
		fatal("io_setup", ERR_SYSCALL, "io_setup failed\n");
		/*NOTREACHED*/
	}

	tip->ofd = -1;
	tip->naios_out = 0;
	tip->send_done = tip->reap_done = 0;
	tip->send_wait = tip->reap_wait = 0;

	memset(&tip->sub_thread, 0, sizeof(tip->sub_thread));
	memset(&tip->rec_thread, 0, sizeof(tip->rec_thread));

	for (i = 0; i < naios; i++) {
		struct iocb_pkt *iocbp = buf_alloc(sizeof(*iocbp));

		iocb_init(tip, iocbp);
		list_add_tail(&iocbp->head, &tip->free_iocbs);
	}
	tip->naios_free = naios;

	if (verbose > 1) {
		char fn[MAXPATHLEN];

		sprintf(fn, "%s/%s.%s.%d.rep", idir, tip->devnm, ibase, 
			tip->cpu);
		tip->vfp = fopen(fn, "w");
		if (!tip->vfp) {
			fatal(fn, ERR_SYSCALL, "Failed to open report\n");
			/*NOTREACHED*/
		}

		setlinebuf(tip->vfp);
	}

	if (pthread_create(&tip->sub_thread, NULL, replay_sub, tip)) {
		fatal("pthread_create", ERR_SYSCALL, 
			"thread create failed\n");
		/*NOTREACHED*/
	}

	if (pthread_create(&tip->rec_thread, NULL, replay_rec, tip)) {
		fatal("pthread_create", ERR_SYSCALL, 
			"thread create failed\n");
		/*NOTREACHED*/
	}
}

/**
 * tip_release - Release resources associated with this thread
 */
static void tip_release(struct thr_info *tip)
{
	struct list_head *p, *q;

	assert(tip->send_done);
	assert(tip->reap_done);
	assert(list_len(&tip->used_iocbs) == 0);
	assert(tip->naios_free == naios);

	if (pthread_join(tip->sub_thread, NULL)) {
		fatal("pthread_join", ERR_SYSCALL, "pthread sub join failed\n");
		/*NOTREACHED*/
	}
	if (pthread_join(tip->rec_thread, NULL)) {
		fatal("pthread_join", ERR_SYSCALL, "pthread rec join failed\n");
		/*NOTREACHED*/
	}

	io_destroy(tip->ctx);

	list_splice(&tip->used_iocbs, &tip->free_iocbs);
	list_for_each_safe(p, q, &tip->free_iocbs) {
		struct iocb_pkt *iocbp = list_entry(p, struct iocb_pkt, head);

		list_del(&iocbp->head);
		if (iocbp->nbytes) 
			free(iocbp->iocb.u.c.buf);
		free(iocbp);
	}

	pthread_cond_destroy(&tip->cond);
	pthread_mutex_destroy(&tip->mutex);
}

/**
 * add_input_file - Allocate and initialize per-input file structure
 * @cpu: CPU for this file
 * @devnm: Device name for this file
 * @file_name: Fully qualifed input file name
 */
static void add_input_file(int cpu, char *devnm, char *file_name)
{
	struct stat buf;
	struct io_file_hdr hdr;
	struct thr_info *tip = buf_alloc(sizeof(*tip));
	__u64 my_version = mk_btversion(btver_mjr, btver_mnr, btver_sub);

	assert(0 <= cpu && cpu < ncpus);

	memset(&hdr, 0, sizeof(hdr));
	memset(tip, 0, sizeof(*tip));
	tip->cpu = cpu % cpus_to_use;
	tip->iterations = def_iterations;

	tip->ifd = open(file_name, O_RDONLY);
	if (tip->ifd < 0) {
		fatal(file_name, ERR_ARGS, "Unable to open\n");
		/*NOTREACHED*/
	}
	if (fstat(tip->ifd, &buf) < 0) {
		fatal(file_name, ERR_SYSCALL, "fstat failed\n");
		/*NOTREACHED*/
	}
	if (buf.st_size < (off_t)sizeof(hdr)) {
		if (verbose)
			fprintf(stderr, "\t%s empty\n", file_name);
		goto empty_file;
	}

	if (read(tip->ifd, &hdr, sizeof(hdr)) != sizeof(hdr)) {
		fatal(file_name, ERR_ARGS, "Header read failed\n");
		/*NOTREACHED*/
	}

	if (hdr.version != my_version) {
		fprintf(stderr, "%llx %llx %llx %llx\n", 
			(long long unsigned)hdr.version,
			(long long unsigned)hdr.genesis,
			(long long unsigned)hdr.nbunches,
			(long long unsigned)hdr.total_pkts);
		fatal(NULL, ERR_ARGS, 
			"BT version mismatch: %lx versus my %lx\n",
			(long)hdr.version, (long)my_version);
			
	}

	if (hdr.nbunches == 0) {
empty_file:
		close(tip->ifd);
		free(tip);
		return;
	}

	if (hdr.genesis < genesis) {
		if (verbose > 1)
			fprintf(stderr, "Setting genesis to %llu.%llu\n",
				du64_to_sec(hdr.genesis),
				du64_to_nsec(hdr.genesis));
		genesis = hdr.genesis;
	}

	tip->devnm = strdup(devnm);
	tip->file_name = strdup(file_name);

	list_add_tail(&tip->head, &input_files);

	if (verbose)
		fprintf(stderr, "Added %s %llu\n", file_name, 
			(long long)hdr.genesis);
}

/**
 * rem_input_file - Release resources associated with an input file
 * @tip: Per-input file information
 */
static void rem_input_file(struct thr_info *tip)
{
	list_del(&tip->head);

	tip_release(tip);

	close(tip->ofd);
	close(tip->ifd);
	free(tip->file_name);
	free(tip->devnm);
	free(tip);
}

/**
 * rem_input_files - Remove all input files
 */
static void rem_input_files(void)
{
	struct list_head *p, *q;

	list_for_each_safe(p, q, &input_files) {
		rem_input_file(list_entry(p, struct thr_info, head));
	}
}

/**
 * __find_input_files - Find input files associated with this device (per cpu)
 */
static void __find_input_files(struct dev_info *dip)
{
	int cpu = 0;

	for (;;) {
		char full_name[MAXPATHLEN];

		sprintf(full_name, "%s/%s.%s.%d", idir, dip->devnm, ibase, cpu);
		if (access(full_name, R_OK) != 0)
			break;

		add_input_file(cpu, dip->devnm, full_name);
		cpu++;
	}

	if (!cpu) {
		fatal(NULL, ERR_ARGS, "No traces found for %s\n", dip->devnm);
		/*NOTREACHED*/
	}

	rem_input_dev(dip);
}


/**
 * find_input_files - Find input files for all devices
 */
static void find_input_files(void)
{
	struct list_head *p, *q;

	list_for_each_safe(p, q, &input_devs) {
		__find_input_files(list_entry(p, struct dev_info, head));
	}
}

/* 
 * ========================================================================
 * ==== RECLAIM ROUTINES ==================================================
 * ========================================================================
 */

/**
 * reap_wait_aios - Wait for and return number of outstanding AIOs
 *
 * Will return 0 if we are done
 */
static int reap_wait_aios(struct thr_info *tip)
{
	int naios = 0;

	if (!is_reap_done(tip)) {
		pthread_mutex_lock(&tip->mutex);
		while (tip->naios_out == 0) {
			tip->reap_wait = 1;
			if (pthread_cond_wait(&tip->cond, &tip->mutex)) {
				fatal("pthread_cond_wait", ERR_SYSCALL, 
					"nfree_current cond wait failed\n");
				/*NOTREACHED*/
			}
		}
		naios = tip->naios_out;
		pthread_mutex_unlock(&tip->mutex);
	}
	assert(is_reap_done(tip) || naios > 0);

	return is_reap_done(tip) ? 0 : naios;
}

/**
 * reclaim_ios - Reclaim AIOs completed, recycle IOCBs
 * @tip: Per-thread information
 * @naios_out: Number of AIOs we have outstanding (min)
 */
static void reclaim_ios(struct thr_info *tip, long naios_out)
{
	long i, ndone;
	struct io_event *evp, events[naios_out];

again:
	assert(naios > 0);
	for (;;) {
		ndone = io_getevents(tip->ctx, 1, naios_out, events, NULL);
		if (ndone > 0)
			break;

		if (errno && errno != EINTR) {
			fatal("io_getevents", ERR_SYSCALL, 
				"io_getevents failed\n");
			/*NOTREACHED*/
		}
	}
	assert(0 < ndone && ndone <= naios_out);

	pthread_mutex_lock(&tip->mutex);
	for (i = 0, evp = events; i < ndone; i++, evp++) {
		struct iocb_pkt *iocbp = evp->data;

                if (evp->res != iocbp->iocb.u.c.nbytes) {
                        fatal(NULL, ERR_SYSCALL,
                              "Event failure %ld/%ld\t(%ld + %ld)\n",
                              (long)evp->res, (long)evp->res2,
                              (long)iocbp->iocb.u.c.offset / nb_sec, 
			      (long)iocbp->iocb.u.c.nbytes / nb_sec);
                        /*NOTREACHED*/
                }

		list_move_tail(&iocbp->head, &tip->free_iocbs);
	}

	tip->naios_free += ndone;
	tip->naios_out -= ndone;
	naios_out = minl(naios_out, tip->naios_out);

	if (tip->send_wait) {
		tip->send_wait = 0;
		pthread_cond_signal(&tip->cond);
	}
	pthread_mutex_unlock(&tip->mutex);

	/*
	 * Short cut: If we /know/ there are some more AIOs, go handle them
	 */
	if (naios_out)
		goto again;
}

/**
 * replay_rec - Worker thread to reclaim AIOs
 * @arg: Pointer to thread information
 */
static void *replay_rec(void *arg)
{
	long naios_out;
	struct thr_info *tip = arg;

	while ((naios_out = reap_wait_aios(tip)) > 0) 
		reclaim_ios(tip, naios_out);

	assert(tip->send_done);
	tip->reap_done = 1;
	set_reclaim_done();

	return NULL;
}

/* 
 * ========================================================================
 * ==== REPLAY ROUTINES ===================================================
 * ========================================================================
 */

/**
 * next_bunch - Retrieve next bunch of AIOs to process
 * @tip: Per-thread information
 * @bunch: Bunch information
 *
 * Returns TRUE if we recovered a bunch of IOs, else hit EOF
 */
static int next_bunch(struct thr_info *tip, struct io_bunch *bunch)
{
	size_t count, result;
	
	result = read(tip->ifd, &bunch->hdr, sizeof(bunch->hdr));
	if (result != sizeof(bunch->hdr)) {
		if (result == 0)
			return 0;

		fatal(tip->file_name, ERR_SYSCALL, "Short hdr(%ld)\n", 
			(long)result);
		/*NOTREACHED*/
	}
	assert(bunch->hdr.npkts <= BT_MAX_PKTS);

	count = bunch->hdr.npkts * sizeof(struct io_pkt);
	result = read(tip->ifd, &bunch->pkts, count);
	if (result != count) {
		fatal(tip->file_name, ERR_SYSCALL, "Short pkts(%ld/%ld)\n", 
			(long)result, (long)count);
		/*NOTREACHED*/
	}

	return 1;
}

/**
 * nfree_current - Returns current number of AIOs that are free
 *
 * Will wait for available ones...
 *
 * Returns 0 if we have some condition that causes us to exit
 */
static int nfree_current(struct thr_info *tip)
{
	int nfree = 0;

	pthread_mutex_lock(&tip->mutex);
	while (!is_send_done(tip) && ((nfree = tip->naios_free) == 0)) {
		tip->send_wait = 1;
		if (pthread_cond_wait(&tip->cond, &tip->mutex)) {
			fatal("pthread_cond_wait", ERR_SYSCALL, 
				"nfree_current cond wait failed\n");
			/*NOTREACHED*/
		}
	}
	pthread_mutex_unlock(&tip->mutex);

	return nfree;
}

/**
 * stall - Stall for the number of nanoseconds requested
 *
 * We may be late, in which case we just return.
 */
static void stall(struct thr_info *tip, long long oclock)
{
	struct timespec req;
	long long dreal, tclock = gettime() - rgenesis;

	oclock /= acc_factor;
	
	if (verbose > 1)
		fprintf(tip->vfp, "   stall(%lld.%09lld, %lld.%09lld)\n",
			du64_to_sec(oclock), du64_to_nsec(oclock),
			du64_to_sec(tclock), du64_to_nsec(tclock));

	while (!is_send_done(tip) && tclock < oclock) {
		dreal = oclock - tclock;
		req.tv_sec = dreal / (1000 * 1000 * 1000);
		req.tv_nsec = dreal % (1000 * 1000 * 1000);

		if (verbose > 1) {
			fprintf(tip->vfp, "++ stall(%lld.%09lld) ++\n",
				(long long)req.tv_sec,
				(long long)req.tv_nsec);
		}

		if (nanosleep(&req, NULL) < 0 && signal_done)
			break;

		tclock = gettime() - rgenesis;
	}
}

/**
 * iocbs_map - Map a set of AIOs onto a set of IOCBs
 * @tip: Per-thread information
 * @list: List of AIOs created
 * @pkts: AIOs to map
 * @ntodo: Number of AIOs to map
 */
static void iocbs_map(struct thr_info *tip, struct iocb **list, 
					     struct io_pkt *pkts, int ntodo)
{
	int i;
	struct io_pkt *pkt;

	assert(0 < ntodo && ntodo <= naios);

	pthread_mutex_lock(&tip->mutex);
	assert(ntodo <= list_len(&tip->free_iocbs));
	for (i = 0, pkt = pkts; i < ntodo; i++, pkt++) {
		__u32 rw = pkt->rw;
		struct iocb_pkt *iocbp;

		if (!pkt->rw && !write_enabled)
			rw = 1;

		if (verbose > 1)
			fprintf(tip->vfp, "\t%10llu + %10llu %c%c\n",
				(unsigned long long)pkt->sector, 
				(unsigned long long)pkt->nbytes / nb_sec,
				rw ? 'R' : 'W', 
				(rw == 1 && pkt->rw == 0) ? '!' : ' ');
		
		iocbp = list_entry(tip->free_iocbs.next, struct iocb_pkt, head);
		iocb_setup(iocbp, rw, pkt->nbytes, pkt->sector * nb_sec);

		list_move_tail(&iocbp->head, &tip->used_iocbs);
		list[i] = &iocbp->iocb;
	}

	tip->naios_free -= ntodo;
	assert(tip->naios_free >= 0);
	pthread_mutex_unlock(&tip->mutex);
}

/**
 * process_bunch - Process a bunch of requests
 * @tip: Per-thread information
 * @bunch: Bunch to process
 */
static void process_bunch(struct thr_info *tip, struct io_bunch *bunch)
{
	__u64 i = 0;
	struct iocb *list[bunch->hdr.npkts];

	assert(0 < bunch->hdr.npkts && bunch->hdr.npkts <= BT_MAX_PKTS);
	while (!is_send_done(tip) && (i < bunch->hdr.npkts)) {
		long ndone;
		int ntodo = min(nfree_current(tip), bunch->hdr.npkts - i);

		assert(0 < ntodo && ntodo <= naios);
		iocbs_map(tip, list, &bunch->pkts[i], ntodo);
		if (!no_stalls)
			stall(tip, bunch->hdr.time_stamp - genesis);

		if (ntodo) {
			if (verbose > 1)
				fprintf(tip->vfp, "submit(%d)\n", ntodo);
			ndone = io_submit(tip->ctx, ntodo, list);
			if (ndone != (long)ntodo) {
				fatal("io_submit", ERR_SYSCALL,
					"%d: io_submit(%d:%ld) failed (%s)\n", 
					tip->cpu, ntodo, ndone, 
					strerror(labs(ndone)));
				/*NOTREACHED*/
			}

			pthread_mutex_lock(&tip->mutex);
			tip->naios_out += ndone;
			assert(tip->naios_out <= naios);
			if (tip->reap_wait) {
				tip->reap_wait = 0;
				pthread_cond_signal(&tip->cond);
			}
			pthread_mutex_unlock(&tip->mutex);

			i += ndone;
			assert(i <= bunch->hdr.npkts);
		}
	}
}

/**
 * reset_input_file - Reset the input file for the next iteration
 * @tip: Thread information
 *
 * We also do a dummy read of the file header to get us to the first bunch.
 */
static void reset_input_file(struct thr_info *tip)
{
	struct io_file_hdr hdr;

	lseek(tip->ifd, 0, 0);

	if (read(tip->ifd, &hdr, sizeof(hdr)) != sizeof(hdr)) {
		fatal(tip->file_name, ERR_ARGS, "Header reread failed\n");
		/*NOTREACHED*/
	}
}

/**
 * replay_sub - Worker thread to submit AIOs that are being replayed
 */
static void *replay_sub(void *arg)
{
	char path[MAXPATHLEN];
	struct io_bunch bunch;
	struct thr_info *tip = arg;
	int oflags;

	pin_to_cpu(tip);

	sprintf(path, "/dev/%s", map_dev(tip->devnm));

#ifdef O_NOATIME
	oflags = O_NOATIME;
#else
	oflags = 0;
#endif
	tip->ofd = open(path, O_RDWR | O_DIRECT | oflags);
	if (tip->ofd < 0) {
		fatal(path, ERR_SYSCALL, "Failed device open\n");
		/*NOTREACHED*/
	}

	set_replay_ready();
	while (!is_send_done(tip) && tip->iterations--) {
		wait_iter_start();
		if (verbose > 1)
			fprintf(tip->vfp, "\n=== %d ===\n", tip->iterations);
		while (!is_send_done(tip) && next_bunch(tip, &bunch))
			process_bunch(tip, &bunch);
		set_iter_done();
		reset_input_file(tip);
	}
	tip->send_done = 1;
	set_replay_done();

	return NULL;
}

/* 
 * ========================================================================
 * ==== COMMAND LINE ARGUMENT HANDLING ====================================
 * ========================================================================
 */

static char usage_str[] = 						\
        "\n"								\
        "\t[ -c <cpus> : --cpus=<cpus>           ] Default: 1\n"        \
        "\t[ -d <dir>  : --input-directory=<dir> ] Default: .\n"        \
	"\t[ -F        : --find-records          ] Default: Off\n"	\
        "\t[ -h        : --help                  ] Default: Off\n"      \
        "\t[ -i <base> : --input-base=<base>     ] Default: replay\n"   \
        "\t[ -I <iters>: --iterations=<iters>    ] Default: 1\n"        \
        "\t[ -M <file> : --map-devs=<file>       ] Default: None\n"     \
        "\t[ -N        : --no-stalls             ] Default: Off\n"      \
        "\t[ -x        : --acc-factor            ] Default: 1\n"	\
        "\t[ -v        : --verbose               ] Default: Off\n"      \
        "\t[ -V        : --version               ] Default: Off\n"      \
        "\t[ -W        : --write-enable          ] Default: Off\n"      \
        "\t<dev...>                                Default: None\n"     \
        "\n";

#define S_OPTS	"c:d:Fhi:I:M:Nx:t:vVW"
static struct option l_opts[] = {
	{
		.name = "cpus",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'c'
	},
	{
		.name = "input-directory",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'd'
	},
	{
		.name = "find-records",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'F'
	},
	{
		.name = "help",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'h'
	},
	{
		.name = "input-base",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'i'
	},
	{
		.name = "iterations",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'I'
	},
	{
		.name = "map-devs",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'M'
	},
	{
		.name = "no-stalls",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'N'
	},
	{
		.name = "acc-factor",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'x'
	},
	{
		.name = "verbose",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'v'
	},
	{
		.name = "version",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'V'
	},
	{
		.name = "write-enable",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'W'
	},
	{
		.name = NULL
	}
};

/**
 * handle_args: Parse passed in argument list
 * @argc: Number of arguments in argv
 * @argv: Arguments passed in
 *
 * Does rudimentary parameter verification as well.
 */
static void handle_args(int argc, char *argv[])
{
	int c;
	int r;

	while ((c = getopt_long(argc, argv, S_OPTS, l_opts, NULL)) != -1) {
		switch (c) {
		case 'c': 
			cpus_to_use = atoi(optarg);
			if (cpus_to_use <= 0 || cpus_to_use > ncpus) {
				fatal(NULL, ERR_ARGS, 
				      "Invalid number of cpus %d (0<x<%d)\n",
				      cpus_to_use, ncpus);
				/*NOTREACHED*/
			}
			break;

		case 'd':
			idir = optarg;
			if (access(idir, R_OK | X_OK) != 0) {
				fatal(idir, ERR_ARGS, 
				      "Invalid input directory specified\n");
				/*NOTREACHED*/
			}
			break;

		case 'F': 
			find_records = 1;
			break;

		case 'h': 
			usage(); 
			exit(0);
			/*NOTREACHED*/

		case 'i': 
			ibase = optarg;
			break;

		case 'I':
			def_iterations = atoi(optarg);
			if (def_iterations <= 0) {
				fprintf(stderr, 
					"Invalid number of iterations %d\n",
					def_iterations);
				exit(ERR_ARGS);
				/*NOTREACHED*/
			}
			break;

		case 'M':
			read_map_devs(optarg);
			break;

		case 'N':
			no_stalls = 1;
			break;

		case 'x':
			r = sscanf(optarg,"%u",&acc_factor);
			if (r!=1) {
				fprintf(stderr,
					"Invalid acceleration factor\n");
				exit(ERR_ARGS);
				/*NOTREACHED*/
			}
			break;

		case 'V':
			fprintf(stderr, "btreplay -- version %s\n", 
				my_btversion);
			fprintf(stderr, "            Built on %s\n", 
				build_date);
			exit(0);
			/*NOTREACHED*/

		case 'v':
			verbose++;
			break;

		case 'W':
			write_enabled = 1;
			break;

		default:
			usage();
			fatal(NULL, ERR_ARGS, 
			      "Invalid command line argument %c\n", c);
			/*NOTREACHED*/
		}
	}

	while (optind < argc)
		add_input_dev(argv[optind++]);

	if (find_records)
		find_input_devs(idir);

	if (list_len(&input_devs) == 0) {
		fatal(NULL, ERR_ARGS, "Missing required input dev name(s)\n");
		/*NOTREACHED*/
	}

	if (cpus_to_use < 0)
		cpus_to_use = ncpus;
}

/* 
 * ========================================================================
 * ==== MAIN ROUTINE ======================================================
 * ========================================================================
 */

/**
 * set_signal_done - Signal handler, catches signals & sets signal_done
 */
static void set_signal_done(__attribute__((__unused__))int signum)
{
	signal_done = 1;
}

/**
 * main - 
 * @argc: Number of arguments
 * @argv: Array of arguments
 */
int main(int argc, char *argv[])
{
	int i;
	struct list_head *p;

	pgsize = getpagesize();
	assert(pgsize > 0);

	setup_signal(SIGINT, set_signal_done);
	setup_signal(SIGTERM, set_signal_done);

	get_ncpus();
	handle_args(argc, argv);
	find_input_files();

	nfiles = list_len(&input_files);
	__list_for_each(p, &input_files) {
		tip_init(list_entry(p, struct thr_info, head));
	}

	wait_replays_ready();
	for (i = 0; i < def_iterations; i++) {
		rgenesis = gettime();
		start_iter();
		if (verbose)
			fprintf(stderr, "I");
		wait_iters_done();
	}

	wait_replays_done();
	wait_reclaims_done();

	if (verbose)
		fprintf(stderr, "\n");

	rem_input_files();
	release_map_devs();

	return 0;
}
