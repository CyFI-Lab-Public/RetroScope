/*
 * block queue tracing application
 *
 * Copyright (C) 2005 Jens Axboe <axboe@suse.de>
 * Copyright (C) 2006 Jens Axboe <axboe@kernel.dk>
 *
 * Rewrite to have a single thread per CPU (managing all devices on that CPU)
 *	Alan D. Brunelle <alan.brunelle@hp.com> - January 2009
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
 *
 */

#include <errno.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <getopt.h>
#include <sched.h>
#include <unistd.h>
#include <poll.h>
#include <signal.h>
#include <pthread.h>
#include <locale.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/vfs.h>
#include <sys/mman.h>
#include <sys/param.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <sys/sendfile.h>

#include "btt/list.h"
#include "blktrace.h"

/*
 * You may want to increase this even more, if you are logging at a high
 * rate and see skipped/missed events
 */
#define BUF_SIZE		(512 * 1024)
#define BUF_NR			(4)

#define FILE_VBUF_SIZE		(128 * 1024)

#define DEBUGFS_TYPE		(0x64626720)
#define TRACE_NET_PORT		(8462)

enum {
	Net_none = 0,
	Net_server,
	Net_client,
};

enum thread_status {
	Th_running,
	Th_leaving,
	Th_error
};

/*
 * Generic stats collected: nevents can be _roughly_ estimated by data_read
 * (discounting pdu...)
 *
 * These fields are updated w/ pdc_dr_update & pdc_nev_update below.
 */
struct pdc_stats {
	unsigned long long data_read;
	unsigned long long nevents;
};

struct devpath {
	struct list_head head;
	char *path;			/* path to device special file */
	char *buts_name;		/* name returned from bt kernel code */
	struct pdc_stats *stats;
	int fd, idx, ncpus;
	unsigned long long drops;

	/*
	 * For piped output only:
	 *
	 * Each tracer will have a tracer_devpath_head that it will add new
	 * data onto. It's list is protected above (tracer_devpath_head.mutex)
	 * and it will signal the processing thread using the dp_cond,
	 * dp_mutex & dp_entries variables above.
	 */
	struct tracer_devpath_head *heads;

	/*
	 * For network server mode only:
	 */
	struct cl_host *ch;
	u32 cl_id;
	time_t cl_connect_time;
	struct io_info *ios;
};

/*
 * For piped output to stdout we will have each tracer thread (one per dev)
 * tack buffers read from the relay queues on a per-device list.
 *
 * The main thread will then collect trace buffers from each of lists in turn.
 *
 * We will use a mutex to guard each of the trace_buf list. The tracers
 * can then signal the main thread using <dp_cond,dp_mutex> and
 * dp_entries. (When dp_entries is 0, and a tracer adds an entry it will
 * signal. When dp_entries is 0, the main thread will wait for that condition
 * to be signalled.)
 *
 * adb: It may be better just to have a large buffer per tracer per dev,
 * and then use it as a ring-buffer. This would certainly cut down a lot
 * of malloc/free thrashing, at the cost of more memory movements (potentially).
 */
struct trace_buf {
	struct list_head head;
	struct devpath *dpp;
	void *buf;
	int cpu, len;
};

struct tracer_devpath_head {
	pthread_mutex_t mutex;
	struct list_head head;
	struct trace_buf *prev;
};

/*
 * Used to handle the mmap() interfaces for output file (containing traces)
 */
struct mmap_info {
	void *fs_buf;
	unsigned long long fs_size, fs_max_size, fs_off, fs_buf_len;
	unsigned long buf_size, buf_nr;
	int pagesize;
};

/*
 * Each thread doing work on a (client) side of blktrace will have one
 * of these. The ios array contains input/output information, pfds holds
 * poll() data. The volatile's provide flags to/from the main executing
 * thread.
 */
struct tracer {
	struct list_head head;
	struct io_info *ios;
	struct pollfd *pfds;
	pthread_t thread;
	int cpu, nios;
	volatile int status, is_done;
};

/*
 * networking stuff follows. we include a magic number so we know whether
 * to endianness convert or not.
 *
 * The len field is overloaded:
 *	0 - Indicates an "open" - allowing the server to set up for a dev/cpu
 *	1 - Indicates a "close" - Shut down connection orderly
 *
 * The cpu field is overloaded on close: it will contain the number of drops.
 */
struct blktrace_net_hdr {
	u32 magic;		/* same as trace magic */
	char buts_name[32];	/* trace name */
	u32 cpu;		/* for which cpu */
	u32 max_cpus;
	u32 len;		/* length of following trace data */
	u32 cl_id;		/* id for set of client per-cpu connections */
	u32 buf_size;		/* client buf_size for this trace  */
	u32 buf_nr;		/* client buf_nr for this trace  */
	u32 page_size;		/* client page_size for this trace  */
};

/*
 * Each host encountered has one of these. The head is used to link this
 * on to the network server's ch_list. Connections associated with this
 * host are linked on conn_list, and any devices traced on that host
 * are connected on the devpaths list.
 */
struct cl_host {
	struct list_head head;
	struct list_head conn_list;
	struct list_head devpaths;
	struct net_server_s *ns;
	char *hostname;
	struct in_addr cl_in_addr;
	int connects, ndevs, cl_opens;
};

/*
 * Each connection (client to server socket ('fd')) has one of these. A
 * back reference to the host ('ch'), and lists headers (for the host
 * list, and the network server conn_list) are also included.
 */
struct cl_conn {
	struct list_head ch_head, ns_head;
	struct cl_host *ch;
	int fd, ncpus;
	time_t connect_time;
};

/*
 * The network server requires some poll structures to be maintained -
 * one per conection currently on conn_list. The nchs/ch_list values
 * are for each host connected to this server. The addr field is used
 * for scratch as new connections are established.
 */
struct net_server_s {
	struct list_head conn_list;
	struct list_head ch_list;
	struct pollfd *pfds;
	int listen_fd, connects, nchs;
	struct sockaddr_in addr;
};

/*
 * This structure is (generically) used to providide information
 * for a read-to-write set of values.
 *
 * ifn & ifd represent input information
 *
 * ofn, ofd, ofp, obuf & mmap_info are used for output file (optionally).
 */
struct io_info {
	struct devpath *dpp;
	FILE *ofp;
	char *obuf;
	struct cl_conn *nc;	/* Server network connection */

	/*
	 * mmap controlled output files
	 */
	struct mmap_info mmap_info;

	/*
	 * Client network fields
	 */
	unsigned int ready;
	unsigned long long data_queued;

	/*
	 * Input/output file descriptors & names
	 */
	int ifd, ofd;
	char ifn[MAXPATHLEN + 64];
	char ofn[MAXPATHLEN + 64];
};

static char blktrace_version[] = "2.0.0";

/*
 * Linkage to blktrace helper routines (trace conversions)
 */
int data_is_native = -1;

static int ndevs;
static int ncpus;
static int pagesize;
static int act_mask = ~0U;
static int kill_running_trace;
static int stop_watch;
static int piped_output;

static char *debugfs_path = "/sys/kernel/debug";
static char *output_name;
static char *output_dir;

static unsigned long buf_size = BUF_SIZE;
static unsigned long buf_nr = BUF_NR;

static FILE *pfp;

static LIST_HEAD(devpaths);
static LIST_HEAD(tracers);

static volatile int done;

/*
 * tracer threads add entries, the main thread takes them off and processes
 * them. These protect the dp_entries variable.
 */
static pthread_cond_t dp_cond = PTHREAD_COND_INITIALIZER;
static pthread_mutex_t dp_mutex = PTHREAD_MUTEX_INITIALIZER;
static volatile int dp_entries;

/*
 * These synchronize master / thread interactions.
 */
static pthread_cond_t mt_cond = PTHREAD_COND_INITIALIZER;
static pthread_mutex_t mt_mutex = PTHREAD_MUTEX_INITIALIZER;
static volatile int nthreads_running;
static volatile int nthreads_leaving;
static volatile int nthreads_error;
static volatile int tracers_run;

/*
 * network cmd line params
 */
static struct sockaddr_in hostname_addr;
static char hostname[MAXHOSTNAMELEN];
static int net_port = TRACE_NET_PORT;
static int net_use_sendfile = 1;
static int net_mode;
static int *cl_fds;

static int (*handle_pfds)(struct tracer *, int, int);
static int (*handle_list)(struct tracer_devpath_head *, struct list_head *);

#define S_OPTS	"d:a:A:r:o:kw:vVb:n:D:lh:p:sI:"
static struct option l_opts[] = {
	{
		.name = "dev",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'd'
	},
	{
		.name = "input-devs",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'I'
	},
	{
		.name = "act-mask",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'a'
	},
	{
		.name = "set-mask",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'A'
	},
	{
		.name = "relay",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'r'
	},
	{
		.name = "output",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'o'
	},
	{
		.name = "kill",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'k'
	},
	{
		.name = "stopwatch",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'w'
	},
	{
		.name = "version",
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
		.name = "buffer-size",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'b'
	},
	{
		.name = "num-sub-buffers",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'n'
	},
	{
		.name = "output-dir",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'D'
	},
	{
		.name = "listen",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'l'
	},
	{
		.name = "host",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'h'
	},
	{
		.name = "port",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'p'
	},
	{
		.name = "no-sendfile",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 's'
	},
	{
		.name = NULL,
	}
};

static char usage_str[] = \
	"-d <dev> [ -r debugfs path ] [ -o <output> ] [-k ] [ -w time ]\n" \
	"[ -a action ] [ -A action mask ] [ -I  <devs file> ] [ -v ]\n\n" \
	"\t-d Use specified device. May also be given last after options\n" \
	"\t-r Path to mounted debugfs, defaults to /sys/kernel/debug\n" \
	"\t-o File(s) to send output to\n" \
	"\t-D Directory to prepend to output file names\n" \
	"\t-k Kill a running trace\n" \
	"\t-w Stop after defined time, in seconds\n" \
	"\t-a Only trace specified actions. See documentation\n" \
	"\t-A Give trace mask as a single value. See documentation\n" \
	"\t-b Sub buffer size in KiB\n" \
	"\t-n Number of sub buffers\n" \
	"\t-l Run in network listen mode (blktrace server)\n" \
	"\t-h Run in network client mode, connecting to the given host\n" \
	"\t-p Network port to use (default 8462)\n" \
	"\t-s Make the network client NOT use sendfile() to transfer data\n" \
	"\t-I Add devices found in <devs file>\n" \
	"\t-V Print program version info\n\n";

static void clear_events(struct pollfd *pfd)
{
	pfd->events = 0;
	pfd->revents = 0;
}

static inline int net_client_use_sendfile(void)
{
	return net_mode == Net_client && net_use_sendfile;
}

static inline int net_client_use_send(void)
{
	return net_mode == Net_client && !net_use_sendfile;
}

static inline int use_tracer_devpaths(void)
{
	return piped_output || net_client_use_send();
}

static inline int in_addr_eq(struct in_addr a, struct in_addr b)
{
	return a.s_addr == b.s_addr;
}

static inline void pdc_dr_update(struct devpath *dpp, int cpu, int data_read)
{
	dpp->stats[cpu].data_read += data_read;
}

static inline void pdc_nev_update(struct devpath *dpp, int cpu, int nevents)
{
	dpp->stats[cpu].nevents += nevents;
}

static void show_usage(char *prog)
{
	fprintf(stderr, "Usage: %s %s %s", prog, blktrace_version, usage_str);
}

/*
 * Create a timespec 'msec' milliseconds into the future
 */
static inline void make_timespec(struct timespec *tsp, long delta_msec)
{
	struct timeval now;

	gettimeofday(&now, NULL);
	tsp->tv_sec = now.tv_sec;
	tsp->tv_nsec = 1000L * now.tv_usec;

	tsp->tv_nsec += (delta_msec * 1000000L);
	if (tsp->tv_nsec > 1000000000L) {
		long secs = tsp->tv_nsec / 1000000000L;

		tsp->tv_sec += secs;
		tsp->tv_nsec -= (secs * 1000000000L);
	}
}

/*
 * Add a timer to ensure wait ends
 */
static void t_pthread_cond_wait(pthread_cond_t *cond, pthread_mutex_t *mutex)
{
	struct timespec ts;

	make_timespec(&ts, 50);
	pthread_cond_timedwait(cond, mutex, &ts);
}

static void unblock_tracers(void)
{
	pthread_mutex_lock(&mt_mutex);
	tracers_run = 1;
	pthread_cond_broadcast(&mt_cond);
	pthread_mutex_unlock(&mt_mutex);
}

static void tracer_wait_unblock(struct tracer *tp)
{
	pthread_mutex_lock(&mt_mutex);
	while (!tp->is_done && !tracers_run)
		pthread_cond_wait(&mt_cond, &mt_mutex);
	pthread_mutex_unlock(&mt_mutex);
}

static void tracer_signal_ready(struct tracer *tp,
				enum thread_status th_status,
				int status)
{
	pthread_mutex_lock(&mt_mutex);
	tp->status = status;

	if (th_status == Th_running)
		nthreads_running++;
	else if (th_status == Th_error)
		nthreads_error++;
	else
		nthreads_leaving++;

	pthread_cond_signal(&mt_cond);
	pthread_mutex_unlock(&mt_mutex);
}

static void wait_tracers_ready(int ncpus_started)
{
	pthread_mutex_lock(&mt_mutex);
	while ((nthreads_running + nthreads_error) < ncpus_started)
		t_pthread_cond_wait(&mt_cond, &mt_mutex);
	pthread_mutex_unlock(&mt_mutex);
}

static void wait_tracers_leaving(void)
{
	pthread_mutex_lock(&mt_mutex);
	while (nthreads_leaving < nthreads_running)
		t_pthread_cond_wait(&mt_cond, &mt_mutex);
	pthread_mutex_unlock(&mt_mutex);
}

static void init_mmap_info(struct mmap_info *mip)
{
	mip->buf_size = buf_size;
	mip->buf_nr = buf_nr;
	mip->pagesize = pagesize;
}

static void net_close_connection(int *fd)
{
	shutdown(*fd, SHUT_RDWR);
	close(*fd);
	*fd = -1;
}

static void dpp_free(struct devpath *dpp)
{
	if (dpp->stats)
		free(dpp->stats);
	if (dpp->ios)
		free(dpp->ios);
	if (dpp->path)
		free(dpp->path);
	if (dpp->buts_name)
		free(dpp->buts_name);
	free(dpp);
}

static int lock_on_cpu(int cpu)
{
#ifndef _ANDROID_
	cpu_set_t cpu_mask;

	CPU_ZERO(&cpu_mask);
	CPU_SET(cpu, &cpu_mask);
	if (sched_setaffinity(0, sizeof(cpu_mask), &cpu_mask) < 0)
		return errno;
#endif

	return 0;
}

#ifndef _ANDROID_
static int increase_limit(int resource, rlim_t increase)
{
	struct rlimit rlim;
	int save_errno = errno;

	if (!getrlimit(resource, &rlim)) {
		rlim.rlim_cur += increase;
		if (rlim.rlim_cur >= rlim.rlim_max)
			rlim.rlim_max = rlim.rlim_cur + increase;

		if (!setrlimit(resource, &rlim))
			return 1;
	}

	errno = save_errno;
	return 0;
}
#endif

static int handle_open_failure(void)
{
	if (errno == ENFILE || errno == EMFILE)
#ifndef _ANDROID_
		return increase_limit(RLIMIT_NOFILE, 16);
#else
		return -ENOSYS;
#endif
	return 0;
}

static int handle_mem_failure(size_t length)
{
	if (errno == ENFILE)
		return handle_open_failure();
	else if (errno == ENOMEM)
#ifndef _ANDROID_
		return increase_limit(RLIMIT_MEMLOCK, 2 * length);
#else
		return -ENOSYS;
#endif
	return 0;
}

static FILE *my_fopen(const char *path, const char *mode)
{
	FILE *fp;

	do {
		fp = fopen(path, mode);
	} while (fp == NULL && handle_open_failure());

	return fp;
}

static int my_open(const char *path, int flags)
{
	int fd;

	do {
		fd = open(path, flags);
	} while (fd < 0 && handle_open_failure());

	return fd;
}

static int my_socket(int domain, int type, int protocol)
{
	int fd;

	do {
		fd = socket(domain, type, protocol);
	} while (fd < 0 && handle_open_failure());

	return fd;
}

static int my_accept(int sockfd, struct sockaddr *addr, socklen_t *addrlen)
{
	int fd;

	do {
		fd = accept(sockfd, addr, addrlen);
	} while (fd < 0 && handle_open_failure());

	return fd;
}

static void *my_mmap(void *addr, size_t length, int prot, int flags, int fd,
		     off_t offset)
{
	void *new;

	do {
		new = mmap(addr, length, prot, flags, fd, offset);
	} while (new == MAP_FAILED && handle_mem_failure(length));

	return new;
}

static int my_mlock(const void *addr, size_t len)
{
	int ret;

	do {
		ret = mlock(addr, len);
	} while (ret < 0 && handle_mem_failure(len));

	return ret;
}

static int setup_mmap(int fd, unsigned int maxlen, struct mmap_info *mip)
{
	if (mip->fs_off + maxlen > mip->fs_buf_len) {
		unsigned long nr = max(16, mip->buf_nr);

		if (mip->fs_buf) {
			munlock(mip->fs_buf, mip->fs_buf_len);
			munmap(mip->fs_buf, mip->fs_buf_len);
			mip->fs_buf = NULL;
		}

		mip->fs_off = mip->fs_size & (mip->pagesize - 1);
		mip->fs_buf_len = (nr * mip->buf_size) - mip->fs_off;
		mip->fs_max_size += mip->fs_buf_len;

		if (ftruncate(fd, mip->fs_max_size) < 0) {
			perror("setup_mmap: ftruncate");
			return 1;
		}

		mip->fs_buf = my_mmap(NULL, mip->fs_buf_len, PROT_WRITE,
				      MAP_SHARED, fd,
				      mip->fs_size - mip->fs_off);
		if (mip->fs_buf == MAP_FAILED) {
			perror("setup_mmap: mmap");
			return 1;
		}
		my_mlock(mip->fs_buf, mip->fs_buf_len);
	}

	return 0;
}

static int __stop_trace(int fd)
{
	/*
	 * Should be stopped, don't complain if it isn't
	 */
	ioctl(fd, BLKTRACESTOP);
	return ioctl(fd, BLKTRACETEARDOWN);
}

static int write_data(char *buf, int len)
{
	int ret;

rewrite:
	ret = fwrite(buf, len, 1, pfp);
	if (ferror(pfp) || ret != 1) {
		if (errno == EINTR) {
			clearerr(pfp);
			goto rewrite;
		}

		if (!piped_output || (errno != EPIPE && errno != EBADF)) {
			fprintf(stderr, "write(%d) failed: %d/%s\n",
				len, errno, strerror(errno));
		}
		goto err;
	}

	fflush(pfp);
	return 0;

err:
	clearerr(pfp);
	return 1;
}

/*
 * Returns the number of bytes read (successfully)
 */
static int __net_recv_data(int fd, void *buf, unsigned int len)
{
	unsigned int bytes_left = len;

	while (bytes_left && !done) {
		int ret = recv(fd, buf, bytes_left, MSG_WAITALL);

		if (ret == 0)
			break;
		else if (ret < 0) {
			if (errno == EAGAIN) {
				usleep(50);
				continue;
			}
			perror("server: net_recv_data: recv failed");
			break;
		} else {
			buf += ret;
			bytes_left -= ret;
		}
	}

	return len - bytes_left;
}

static int net_recv_data(int fd, void *buf, unsigned int len)
{
	return __net_recv_data(fd, buf, len);
}

/*
 * Returns number of bytes written
 */
static int net_send_data(int fd, void *buf, unsigned int buf_len)
{
	int ret;
	unsigned int bytes_left = buf_len;

	while (bytes_left) {
		ret = send(fd, buf, bytes_left, 0);
		if (ret < 0) {
			perror("send");
			break;
		}

		buf += ret;
		bytes_left -= ret;
	}

	return buf_len - bytes_left;
}

static int net_send_header(int fd, int cpu, char *buts_name, int len)
{
	struct blktrace_net_hdr hdr;

	memset(&hdr, 0, sizeof(hdr));

	hdr.magic = BLK_IO_TRACE_MAGIC;
	strncpy(hdr.buts_name, buts_name, sizeof(hdr.buts_name));
	hdr.buts_name[sizeof(hdr.buts_name)-1] = '\0';
	hdr.cpu = cpu;
	hdr.max_cpus = ncpus;
	hdr.len = len;
	hdr.cl_id = getpid();
	hdr.buf_size = buf_size;
	hdr.buf_nr = buf_nr;
	hdr.page_size = pagesize;

	return net_send_data(fd, &hdr, sizeof(hdr)) != sizeof(hdr);
}

static void net_send_open_close(int fd, int cpu, char *buts_name, int len)
{
	struct blktrace_net_hdr ret_hdr;

	net_send_header(fd, cpu, buts_name, len);
	net_recv_data(fd, &ret_hdr, sizeof(ret_hdr));
}

static void net_send_open(int fd, int cpu, char *buts_name)
{
	net_send_open_close(fd, cpu, buts_name, 0);
}

static void net_send_close(int fd, char *buts_name, int drops)
{
	/*
	 * Overload CPU w/ number of drops
	 *
	 * XXX: Need to clear/set done around call - done=1 (which
	 * is true here) stops reads from happening... :-(
	 */
	done = 0;
	net_send_open_close(fd, drops, buts_name, 1);
	done = 1;
}

static void ack_open_close(int fd, char *buts_name)
{
	net_send_header(fd, 0, buts_name, 2);
}

static void net_send_drops(int fd)
{
	struct list_head *p;

	__list_for_each(p, &devpaths) {
		struct devpath *dpp = list_entry(p, struct devpath, head);

		net_send_close(fd, dpp->buts_name, dpp->drops);
	}
}

/*
 * Returns:
 *	 0: "EOF"
 *	 1: OK
 *	-1: Error
 */
static int net_get_header(struct cl_conn *nc, struct blktrace_net_hdr *bnh)
{
	int bytes_read;
	int fl = fcntl(nc->fd, F_GETFL);

	fcntl(nc->fd, F_SETFL, fl | O_NONBLOCK);
	bytes_read = __net_recv_data(nc->fd, bnh, sizeof(*bnh));
	fcntl(nc->fd, F_SETFL, fl & ~O_NONBLOCK);

	if (bytes_read == sizeof(*bnh))
		return 1;
	else if (bytes_read == 0)
		return 0;
	else
		return -1;
}

static int net_setup_addr(void)
{
	struct sockaddr_in *addr = &hostname_addr;

	memset(addr, 0, sizeof(*addr));
	addr->sin_family = AF_INET;
	addr->sin_port = htons(net_port);

	if (inet_aton(hostname, &addr->sin_addr) != 1) {
		struct hostent *hent;
retry:
		hent = gethostbyname(hostname);
		if (!hent) {
			if (h_errno == TRY_AGAIN) {
				usleep(100);
				goto retry;
			} else if (h_errno == NO_RECOVERY) {
				fprintf(stderr, "gethostbyname(%s)"
					"non-recoverable error encountered\n",
					hostname);
			} else {
				/*
				 * HOST_NOT_FOUND, NO_ADDRESS or NO_DATA
				 */
				fprintf(stderr, "Host %s not found\n",
					hostname);
			}
			return 1;
		}

		memcpy(&addr->sin_addr, hent->h_addr, 4);
		strcpy(hostname, hent->h_name);
	}

	return 0;
}

static int net_setup_client(void)
{
	int fd;
	struct sockaddr_in *addr = &hostname_addr;

	fd = my_socket(AF_INET, SOCK_STREAM, 0);
	if (fd < 0) {
		perror("client: socket");
		return -1;
	}

	if (connect(fd, (struct sockaddr *)addr, sizeof(*addr)) < 0) {
		if (errno == ECONNREFUSED)
			fprintf(stderr,
				"\nclient: Connection to %s refused, "
				"perhaps the server is not started?\n\n",
				hostname);
		else
			perror("client: connect");

		close(fd);
		return -1;
	}

	return fd;
}

static int open_client_connections(void)
{
	int cpu;

	cl_fds = calloc(ncpus, sizeof(*cl_fds));
	for (cpu = 0; cpu < ncpus; cpu++) {
		cl_fds[cpu] = net_setup_client();
		if (cl_fds[cpu] < 0)
			goto err;
	}
	return 0;

err:
	while (cpu > 0)
		close(cl_fds[cpu--]);
	free(cl_fds);
	return 1;
}

static void close_client_connections(void)
{
	if (cl_fds) {
		int cpu, *fdp;

		for (cpu = 0, fdp = cl_fds; cpu < ncpus; cpu++, fdp++) {
			if (*fdp >= 0) {
				net_send_drops(*fdp);
				net_close_connection(fdp);
			}
		}
		free(cl_fds);
	}
}

static void setup_buts(void)
{
	struct list_head *p;

	__list_for_each(p, &devpaths) {
		struct blk_user_trace_setup buts;
		struct devpath *dpp = list_entry(p, struct devpath, head);

		memset(&buts, 0, sizeof(buts));
		buts.buf_size = buf_size;
		buts.buf_nr = buf_nr;
		buts.act_mask = act_mask;
		if (ioctl(dpp->fd, BLKTRACESETUP, &buts) >= 0) {
			dpp->ncpus = ncpus;
			dpp->buts_name = strdup(buts.name);
			if (dpp->stats)
				free(dpp->stats);
			dpp->stats = calloc(dpp->ncpus, sizeof(*dpp->stats));
			memset(dpp->stats, 0, dpp->ncpus * sizeof(*dpp->stats));
		} else
			fprintf(stderr, "BLKTRACESETUP(2) %s failed: %d/%s\n",
				dpp->path, errno, strerror(errno));
	}
}

static void start_buts(void)
{
	struct list_head *p;

	__list_for_each(p, &devpaths) {
		struct devpath *dpp = list_entry(p, struct devpath, head);

		if (ioctl(dpp->fd, BLKTRACESTART) < 0) {
			fprintf(stderr, "BLKTRACESTART %s failed: %d/%s\n",
				dpp->path, errno, strerror(errno));
		}
	}
}

static int get_drops(struct devpath *dpp)
{
	int fd, drops = 0;
	char fn[MAXPATHLEN + 64], tmp[256];

	snprintf(fn, sizeof(fn), "%s/block/%s/dropped", debugfs_path,
		 dpp->buts_name);

	fd = my_open(fn, O_RDONLY);
	if (fd < 0) {
		/*
		 * This may be ok: the kernel may not support
		 * dropped counts.
		 */
		if (errno != ENOENT)
			fprintf(stderr, "Could not open %s: %d/%s\n",
				fn, errno, strerror(errno));
		return 0;
	} else if (read(fd, tmp, sizeof(tmp)) < 0) {
		fprintf(stderr, "Could not read %s: %d/%s\n",
			fn, errno, strerror(errno));
	} else
		drops = atoi(tmp);
	close(fd);

	return drops;
}

static void get_all_drops(void)
{
	struct list_head *p;

	__list_for_each(p, &devpaths) {
		struct devpath *dpp = list_entry(p, struct devpath, head);

		dpp->drops = get_drops(dpp);
	}
}

static inline struct trace_buf *alloc_trace_buf(int cpu, int bufsize)
{
	struct trace_buf *tbp;

	tbp = malloc(sizeof(*tbp) + bufsize);
	INIT_LIST_HEAD(&tbp->head);
	tbp->len = 0;
	tbp->buf = (void *)(tbp + 1);
	tbp->cpu = cpu;
	tbp->dpp = NULL;	/* Will be set when tbp is added */

	return tbp;
}

static void free_tracer_heads(struct devpath *dpp)
{
	int cpu;
	struct tracer_devpath_head *hd;

	for (cpu = 0, hd = dpp->heads; cpu < ncpus; cpu++, hd++) {
		if (hd->prev)
			free(hd->prev);

		pthread_mutex_destroy(&hd->mutex);
	}
	free(dpp->heads);
}

static int setup_tracer_devpaths(void)
{
	struct list_head *p;

	if (net_client_use_send())
		if (open_client_connections())
			return 1;

	__list_for_each(p, &devpaths) {
		int cpu;
		struct tracer_devpath_head *hd;
		struct devpath *dpp = list_entry(p, struct devpath, head);

		dpp->heads = calloc(ncpus, sizeof(struct tracer_devpath_head));
		for (cpu = 0, hd = dpp->heads; cpu < ncpus; cpu++, hd++) {
			INIT_LIST_HEAD(&hd->head);
			pthread_mutex_init(&hd->mutex, NULL);
			hd->prev = NULL;
		}
	}

	return 0;
}

static inline void add_trace_buf(struct devpath *dpp, int cpu,
						struct trace_buf **tbpp)
{
	struct trace_buf *tbp = *tbpp;
	struct tracer_devpath_head *hd = &dpp->heads[cpu];

	tbp->dpp = dpp;

	pthread_mutex_lock(&hd->mutex);
	list_add_tail(&tbp->head, &hd->head);
	pthread_mutex_unlock(&hd->mutex);

	*tbpp = alloc_trace_buf(cpu, buf_size);
}

static inline void incr_entries(int entries_handled)
{
	pthread_mutex_lock(&dp_mutex);
	if (dp_entries == 0)
		pthread_cond_signal(&dp_cond);
	dp_entries += entries_handled;
	pthread_mutex_unlock(&dp_mutex);
}

static void decr_entries(int handled)
{
	pthread_mutex_lock(&dp_mutex);
	dp_entries -= handled;
	pthread_mutex_unlock(&dp_mutex);
}

static int wait_empty_entries(void)
{
	pthread_mutex_lock(&dp_mutex);
	while (!done && dp_entries == 0)
		t_pthread_cond_wait(&dp_cond, &dp_mutex);
	pthread_mutex_unlock(&dp_mutex);

	return !done;
}

static int add_devpath(char *path)
{
	int fd;
	struct devpath *dpp;

	/*
	 * Verify device is valid before going too far
	 */
	fd = my_open(path, O_RDONLY | O_NONBLOCK);
	if (fd < 0) {
		fprintf(stderr, "Invalid path %s specified: %d/%s\n",
			path, errno, strerror(errno));
		return 1;
	}

	dpp = malloc(sizeof(*dpp));
	memset(dpp, 0, sizeof(*dpp));
	dpp->path = strdup(path);
	dpp->fd = fd;
	dpp->idx = ndevs++;
	list_add_tail(&dpp->head, &devpaths);

	return 0;
}

static void rel_devpaths(void)
{
	struct list_head *p, *q;

	list_for_each_safe(p, q, &devpaths) {
		struct devpath *dpp = list_entry(p, struct devpath, head);

		list_del(&dpp->head);
		__stop_trace(dpp->fd);
		close(dpp->fd);

		if (dpp->heads)
			free_tracer_heads(dpp);

		dpp_free(dpp);
		ndevs--;
	}
}

static int flush_subbuf_net(struct trace_buf *tbp)
{
	int fd = cl_fds[tbp->cpu];
	struct devpath *dpp = tbp->dpp;

	if (net_send_header(fd, tbp->cpu, dpp->buts_name, tbp->len))
		return 1;
	else if (net_send_data(fd, tbp->buf, tbp->len) != tbp->len)
		return 1;

	return 0;
}

static int
handle_list_net(__attribute__((__unused__))struct tracer_devpath_head *hd,
		struct list_head *list)
{
	struct trace_buf *tbp;
	struct list_head *p, *q;
	int entries_handled = 0;

	list_for_each_safe(p, q, list) {
		tbp = list_entry(p, struct trace_buf, head);

		list_del(&tbp->head);
		entries_handled++;

		if (cl_fds[tbp->cpu] >= 0) {
			if (flush_subbuf_net(tbp)) {
				close(cl_fds[tbp->cpu]);
				cl_fds[tbp->cpu] = -1;
			}
		}

		free(tbp);
	}

	return entries_handled;
}

/*
 * Tack 'tbp's buf onto the tail of 'prev's buf
 */
static struct trace_buf *tb_combine(struct trace_buf *prev,
				    struct trace_buf *tbp)
{
	unsigned long tot_len;

	tot_len = prev->len + tbp->len;
	if (tot_len > buf_size) {
		/*
		 * tbp->head isn't connected (it was 'prev'
		 * so it had been taken off of the list
		 * before). Therefore, we can realloc
		 * the whole structures, as the other fields
		 * are "static".
		 */
		prev = realloc(prev->buf, sizeof(*prev) + tot_len);
		prev->buf = (void *)(prev + 1);
	}

	memcpy(prev->buf + prev->len, tbp->buf, tbp->len);
	prev->len = tot_len;

	free(tbp);
	return prev;
}

static int handle_list_file(struct tracer_devpath_head *hd,
			    struct list_head *list)
{
	int off, t_len, nevents;
	struct blk_io_trace *t;
	struct list_head *p, *q;
	int entries_handled = 0;
	struct trace_buf *tbp, *prev;

	prev = hd->prev;
	list_for_each_safe(p, q, list) {
		tbp = list_entry(p, struct trace_buf, head);
		list_del(&tbp->head);
		entries_handled++;

		/*
		 * If there was some leftover before, tack this new
		 * entry onto the tail of the previous one.
		 */
		if (prev)
			tbp = tb_combine(prev, tbp);

		/*
		 * See how many whole traces there are - send them
		 * all out in one go.
		 */
		off = 0;
		nevents = 0;
		while (off + (int)sizeof(*t) <= tbp->len) {
			t = (struct blk_io_trace *)(tbp->buf + off);
			t_len = sizeof(*t) + t->pdu_len;
			if (off + t_len > tbp->len)
				break;

			off += t_len;
			nevents++;
		}
		if (nevents)
			pdc_nev_update(tbp->dpp, tbp->cpu, nevents);

		/*
		 * Write any full set of traces, any remaining data is kept
		 * for the next pass.
		 */
		if (off) {
			if (write_data(tbp->buf, off) || off == tbp->len) {
				free(tbp);
				prev = NULL;
			}
			else {
				/*
				 * Move valid data to beginning of buffer
				 */
				tbp->len -= off;
				memmove(tbp->buf, tbp->buf + off, tbp->len);
				prev = tbp;
			}
		} else
			prev = tbp;
	}
	hd->prev = prev;

	return entries_handled;
}

static void __process_trace_bufs(void)
{
	int cpu;
	struct list_head *p;
	struct list_head list;
	int handled = 0;

	__list_for_each(p, &devpaths) {
		struct devpath *dpp = list_entry(p, struct devpath, head);
		struct tracer_devpath_head *hd = dpp->heads;

		for (cpu = 0; cpu < ncpus; cpu++, hd++) {
			pthread_mutex_lock(&hd->mutex);
			if (list_empty(&hd->head)) {
				pthread_mutex_unlock(&hd->mutex);
				continue;
			}

			list_replace_init(&hd->head, &list);
			pthread_mutex_unlock(&hd->mutex);

			handled += handle_list(hd, &list);
		}
	}

	if (handled)
		decr_entries(handled);
}

static void process_trace_bufs(void)
{
	while (wait_empty_entries())
		__process_trace_bufs();
}

static void clean_trace_bufs(void)
{
	/*
	 * No mutex needed here: we're only reading from the lists,
	 * tracers are done
	 */
	while (dp_entries)
		__process_trace_bufs();
}

static inline void read_err(int cpu, char *ifn)
{
	if (errno != EAGAIN)
		fprintf(stderr, "Thread %d failed read of %s: %d/%s\n",
			cpu, ifn, errno, strerror(errno));
}

static int net_sendfile(struct io_info *iop)
{
	int ret;

	ret = sendfile(iop->ofd, iop->ifd, NULL, iop->ready);
	if (ret < 0) {
		perror("sendfile");
		return 1;
	} else if (ret < (int)iop->ready) {
		fprintf(stderr, "short sendfile send (%d of %d)\n",
			ret, iop->ready);
		return 1;
	}

	return 0;
}

static inline int net_sendfile_data(struct tracer *tp, struct io_info *iop)
{
	struct devpath *dpp = iop->dpp;

	if (net_send_header(iop->ofd, tp->cpu, dpp->buts_name, iop->ready))
		return 1;
	return net_sendfile(iop);
}

static int fill_ofname(struct io_info *iop, int cpu)
{
	int len;
	struct stat sb;
	char *dst = iop->ofn;

	if (output_dir)
		len = snprintf(iop->ofn, sizeof(iop->ofn), "%s/", output_dir);
	else
		len = snprintf(iop->ofn, sizeof(iop->ofn), "./");

	if (net_mode == Net_server) {
		struct cl_conn *nc = iop->nc;

		len += sprintf(dst + len, "%s-", nc->ch->hostname);
		len += strftime(dst + len, 64, "%F-%T/",
				gmtime(&iop->dpp->cl_connect_time));
	}

	if (stat(iop->ofn, &sb) < 0) {
		if (errno != ENOENT) {
			fprintf(stderr,
				"Destination dir %s stat failed: %d/%s\n",
				iop->ofn, errno, strerror(errno));
			return 1;
		}
		/*
		 * There is no synchronization between multiple threads
		 * trying to create the directory at once.  It's harmless
		 * to let them try, so just detect the problem and move on.
		 */
		if (mkdir(iop->ofn, 0755) < 0 && errno != EEXIST) {
			fprintf(stderr,
				"Destination dir %s can't be made: %d/%s\n",
				iop->ofn, errno, strerror(errno));
			return 1;
		}
	}

	if (output_name)
		snprintf(iop->ofn + len, sizeof(iop->ofn), "%s.blktrace.%d",
			 output_name, cpu);
	else
		snprintf(iop->ofn + len, sizeof(iop->ofn), "%s.blktrace.%d",
			 iop->dpp->buts_name, cpu);

	return 0;
}

static int set_vbuf(struct io_info *iop, int mode, size_t size)
{
	iop->obuf = malloc(size);
	if (setvbuf(iop->ofp, iop->obuf, mode, size) < 0) {
		fprintf(stderr, "setvbuf(%s, %d) failed: %d/%s\n",
			iop->dpp->path, (int)size, errno,
			strerror(errno));
		free(iop->obuf);
		return 1;
	}

	return 0;
}

static int iop_open(struct io_info *iop, int cpu)
{
	iop->ofd = -1;
	if (fill_ofname(iop, cpu))
		return 1;

	iop->ofp = my_fopen(iop->ofn, "w+");
	if (iop->ofp == NULL) {
		fprintf(stderr, "Open output file %s failed: %d/%s\n",
			iop->ofn, errno, strerror(errno));
		return 1;
	}

	if (set_vbuf(iop, _IOLBF, FILE_VBUF_SIZE)) {
		fprintf(stderr, "set_vbuf for file %s failed: %d/%s\n",
			iop->ofn, errno, strerror(errno));
		fclose(iop->ofp);
		return 1;
	}

	iop->ofd = fileno(iop->ofp);
	return 0;
}

static void close_iop(struct io_info *iop)
{
	struct mmap_info *mip = &iop->mmap_info;

	if (mip->fs_buf)
		munmap(mip->fs_buf, mip->fs_buf_len);

	if (!piped_output) {
		if (ftruncate(fileno(iop->ofp), mip->fs_size) < 0) {
			fprintf(stderr,
				"Ignoring err: ftruncate(%s): %d/%s\n",
				iop->ofn, errno, strerror(errno));
		}
	}

	if (iop->ofp)
		fclose(iop->ofp);
	if (iop->obuf)
		free(iop->obuf);
}

static void close_ios(struct tracer *tp)
{
	while (tp->nios > 0) {
		struct io_info *iop = &tp->ios[--tp->nios];

		iop->dpp->drops = get_drops(iop->dpp);
		if (iop->ifd >= 0)
			close(iop->ifd);

		if (iop->ofp)
			close_iop(iop);
		else if (iop->ofd >= 0) {
			struct devpath *dpp = iop->dpp;

			net_send_close(iop->ofd, dpp->buts_name, dpp->drops);
			net_close_connection(&iop->ofd);
		}
	}

	free(tp->ios);
	free(tp->pfds);
}

static int open_ios(struct tracer *tp)
{
	struct pollfd *pfd;
	struct io_info *iop;
	struct list_head *p;

	tp->ios = calloc(ndevs, sizeof(struct io_info));
	memset(tp->ios, 0, ndevs * sizeof(struct io_info));

	tp->pfds = calloc(ndevs, sizeof(struct pollfd));
	memset(tp->pfds, 0, ndevs * sizeof(struct pollfd));

	tp->nios = 0;
	iop = tp->ios;
	pfd = tp->pfds;
	__list_for_each(p, &devpaths) {
		struct devpath *dpp = list_entry(p, struct devpath, head);

		iop->dpp = dpp;
		iop->ofd = -1;
		snprintf(iop->ifn, sizeof(iop->ifn), "%s/block/%s/trace%d",
			debugfs_path, dpp->buts_name, tp->cpu);

		iop->ifd = my_open(iop->ifn, O_RDONLY | O_NONBLOCK);
		if (iop->ifd < 0) {
			fprintf(stderr, "Thread %d failed open %s: %d/%s\n",
				tp->cpu, iop->ifn, errno, strerror(errno));
			return 1;
		}

		init_mmap_info(&iop->mmap_info);

		pfd->fd = iop->ifd;
		pfd->events = POLLIN;

		if (piped_output)
			;
		else if (net_client_use_sendfile()) {
			iop->ofd = net_setup_client();
			if (iop->ofd < 0)
				goto err;
			net_send_open(iop->ofd, tp->cpu, dpp->buts_name);
		} else if (net_mode == Net_none) {
			if (iop_open(iop, tp->cpu))
				goto err;
		} else {
			/*
			 * This ensures that the server knows about all
			 * connections & devices before _any_ closes
			 */
			net_send_open(cl_fds[tp->cpu], tp->cpu, dpp->buts_name);
		}

		pfd++;
		iop++;
		tp->nios++;
	}

	return 0;

err:
	close(iop->ifd);	/* tp->nios _not_ bumped */
	close_ios(tp);
	return 1;
}

static int handle_pfds_file(struct tracer *tp, int nevs, int force_read)
{
	struct mmap_info *mip;
	int i, ret, nentries = 0;
	struct pollfd *pfd = tp->pfds;
	struct io_info *iop = tp->ios;

	for (i = 0; nevs > 0 && i < ndevs; i++, pfd++, iop++) {
		if (pfd->revents & POLLIN || force_read) {
			mip = &iop->mmap_info;

			ret = setup_mmap(iop->ofd, buf_size, mip);
			if (ret < 0) {
				pfd->events = 0;
				break;
			}

			ret = read(iop->ifd, mip->fs_buf + mip->fs_off,
				   buf_size);
			if (ret > 0) {
				pdc_dr_update(iop->dpp, tp->cpu, ret);
				mip->fs_size += ret;
				mip->fs_off += ret;
				nentries++;
			} else if (ret == 0) {
				/*
				 * Short reads after we're done stop us
				 * from trying reads.
				 */
				if (tp->is_done)
					clear_events(pfd);
			} else {
				read_err(tp->cpu, iop->ifn);
				if (errno != EAGAIN || tp->is_done)
					clear_events(pfd);
			}
			nevs--;
		}
	}

	return nentries;
}

static int handle_pfds_netclient(struct tracer *tp, int nevs, int force_read)
{
	struct stat sb;
	int i, nentries = 0;
	struct pdc_stats *sp;
	struct pollfd *pfd = tp->pfds;
	struct io_info *iop = tp->ios;

	for (i = 0; i < ndevs; i++, pfd++, iop++, sp++) {
		if (pfd->revents & POLLIN || force_read) {
			if (fstat(iop->ifd, &sb) < 0) {
				perror(iop->ifn);
				pfd->events = 0;
			} else if (sb.st_size > (off_t)iop->data_queued) {
				iop->ready = sb.st_size - iop->data_queued;
				iop->data_queued = sb.st_size;

				if (!net_sendfile_data(tp, iop)) {
					pdc_dr_update(iop->dpp, tp->cpu,
						      iop->ready);
					nentries++;
				} else
					clear_events(pfd);
			}
			if (--nevs == 0)
				break;
		}
	}

	if (nentries)
		incr_entries(nentries);

	return nentries;
}

static int handle_pfds_entries(struct tracer *tp, int nevs, int force_read)
{
	int i, nentries = 0;
	struct trace_buf *tbp;
	struct pollfd *pfd = tp->pfds;
	struct io_info *iop = tp->ios;

	tbp = alloc_trace_buf(tp->cpu, buf_size);
	for (i = 0; i < ndevs; i++, pfd++, iop++) {
		if (pfd->revents & POLLIN || force_read) {
			tbp->len = read(iop->ifd, tbp->buf, buf_size);
			if (tbp->len > 0) {
				pdc_dr_update(iop->dpp, tp->cpu, tbp->len);
				add_trace_buf(iop->dpp, tp->cpu, &tbp);
				nentries++;
			} else if (tbp->len == 0) {
				/*
				 * Short reads after we're done stop us
				 * from trying reads.
				 */
				if (tp->is_done)
					clear_events(pfd);
			} else {
				read_err(tp->cpu, iop->ifn);
				if (errno != EAGAIN || tp->is_done)
					clear_events(pfd);
			}
			if (!piped_output && --nevs == 0)
				break;
		}
	}
	free(tbp);

	if (nentries)
		incr_entries(nentries);

	return nentries;
}

static void *thread_main(void *arg)
{
	int ret, ndone, to_val;
	struct tracer *tp = arg;

	ret = lock_on_cpu(tp->cpu);
	if (ret)
		goto err;

	ret = open_ios(tp);
	if (ret)
		goto err;

	if (piped_output)
		to_val = 50;		/* Frequent partial handles */
	else
		to_val = 500;		/* 1/2 second intervals */


	tracer_signal_ready(tp, Th_running, 0);
	tracer_wait_unblock(tp);

	while (!tp->is_done) {
		ndone = poll(tp->pfds, ndevs, to_val);
		if (ndone || piped_output)
			(void)handle_pfds(tp, ndone, piped_output);
		else if (ndone < 0 && errno != EINTR)
			fprintf(stderr, "Thread %d poll failed: %d/%s\n",
				tp->cpu, errno, strerror(errno));
	}

	/*
	 * Trace is stopped, pull data until we get a short read
	 */
	while (handle_pfds(tp, ndevs, 1) > 0)
		;

	close_ios(tp);
	tracer_signal_ready(tp, Th_leaving, 0);
	return NULL;

err:
	tracer_signal_ready(tp, Th_error, ret);
	return NULL;
}

static int start_tracer(int cpu)
{
	struct tracer *tp;

	tp = malloc(sizeof(*tp));
	memset(tp, 0, sizeof(*tp));

	INIT_LIST_HEAD(&tp->head);
	tp->status = 0;
	tp->cpu = cpu;

	if (pthread_create(&tp->thread, NULL, thread_main, tp)) {
		fprintf(stderr, "FAILED to start thread on CPU %d: %d/%s\n",
			cpu, errno, strerror(errno));
		free(tp);
		return 1;
	}

	list_add_tail(&tp->head, &tracers);
	return 0;
}

static void start_tracers(void)
{
	int cpu;
	struct list_head *p;

	for (cpu = 0; cpu < ncpus; cpu++)
		if (start_tracer(cpu))
			break;

	wait_tracers_ready(cpu);

	__list_for_each(p, &tracers) {
		struct tracer *tp = list_entry(p, struct tracer, head);
		if (tp->status)
			fprintf(stderr,
				"FAILED to start thread on CPU %d: %d/%s\n",
				tp->cpu, tp->status, strerror(tp->status));
	}
}

static void stop_tracers(void)
{
	struct list_head *p;

	/*
	 * Stop the tracing - makes the tracer threads clean up quicker.
	 */
	__list_for_each(p, &devpaths) {
		struct devpath *dpp = list_entry(p, struct devpath, head);
		(void)ioctl(dpp->fd, BLKTRACESTOP);
	}

	/*
	 * Tell each tracer to quit
	 */
	__list_for_each(p, &tracers) {
		struct tracer *tp = list_entry(p, struct tracer, head);
		tp->is_done = 1;
	}
}

static void del_tracers(void)
{
	struct list_head *p, *q;

	list_for_each_safe(p, q, &tracers) {
		struct tracer *tp = list_entry(p, struct tracer, head);

		list_del(&tp->head);
		free(tp);
	}
}

static void wait_tracers(void)
{
	struct list_head *p;

	if (use_tracer_devpaths())
		process_trace_bufs();

	wait_tracers_leaving();

	__list_for_each(p, &tracers) {
		int ret;
		struct tracer *tp = list_entry(p, struct tracer, head);

		ret = pthread_join(tp->thread, NULL);
		if (ret)
			fprintf(stderr, "Thread join %d failed %d\n",
				tp->cpu, ret);
	}

	if (use_tracer_devpaths())
		clean_trace_bufs();

	get_all_drops();
}

static void exit_tracing(void)
{
	signal(SIGINT, SIG_IGN);
	signal(SIGHUP, SIG_IGN);
	signal(SIGTERM, SIG_IGN);
	signal(SIGALRM, SIG_IGN);

	stop_tracers();
	wait_tracers();
	del_tracers();
	rel_devpaths();
}

static void handle_sigint(__attribute__((__unused__)) int sig)
{
	done = 1;
	stop_tracers();
}

static void show_stats(struct list_head *devpaths)
{
	FILE *ofp;
	struct list_head *p;
	unsigned long long nevents, data_read;
	unsigned long long total_drops = 0;
	unsigned long long total_events = 0;

	if (piped_output)
		ofp = my_fopen("/dev/null", "w");
	else
		ofp = stdout;

	__list_for_each(p, devpaths) {
		int cpu;
		struct pdc_stats *sp;
		struct devpath *dpp = list_entry(p, struct devpath, head);

		if (net_mode == Net_server)
			printf("server: end of run for %s:%s\n",
				dpp->ch->hostname, dpp->buts_name);

		data_read = 0;
		nevents = 0;

		fprintf(ofp, "=== %s ===\n", dpp->buts_name);
		for (cpu = 0, sp = dpp->stats; cpu < dpp->ncpus; cpu++, sp++) {
			/*
			 * Estimate events if not known...
			 */
			if (sp->nevents == 0) {
				sp->nevents = sp->data_read /
						sizeof(struct blk_io_trace);
			}

			fprintf(ofp,
				"  CPU%3d: %20llu events, %8llu KiB data\n",
				cpu, sp->nevents, (sp->data_read + 1023) >> 10);

			data_read += sp->data_read;
			nevents += sp->nevents;
		}

		fprintf(ofp, "  Total:  %20llu events (dropped %llu),"
			     " %8llu KiB data\n", nevents,
			     dpp->drops, (data_read + 1024) >> 10);

		total_drops += dpp->drops;
		total_events += (nevents + dpp->drops);
	}

	fflush(ofp);
	if (piped_output)
		fclose(ofp);

	if (total_drops) {
		double drops_ratio = 1.0;

		if (total_events)
			drops_ratio = (double)total_drops/(double)total_events;

		fprintf(stderr, "\nYou have %llu (%5.1lf%%) dropped events\n"
				"Consider using a larger buffer size (-b) "
				"and/or more buffers (-n)\n",
			total_drops, 100.0 * drops_ratio);
	}
}

static int handle_args(int argc, char *argv[])
{
	int c, i;
	struct statfs st;
	int act_mask_tmp = 0;

	while ((c = getopt_long(argc, argv, S_OPTS, l_opts, NULL)) >= 0) {
		switch (c) {
		case 'a':
			i = find_mask_map(optarg);
			if (i < 0) {
				fprintf(stderr, "Invalid action mask %s\n",
					optarg);
				return 1;
			}
			act_mask_tmp |= i;
			break;

		case 'A':
			if ((sscanf(optarg, "%x", &i) != 1) ||
							!valid_act_opt(i)) {
				fprintf(stderr,
					"Invalid set action mask %s/0x%x\n",
					optarg, i);
				return 1;
			}
			act_mask_tmp = i;
			break;

		case 'd':
			if (add_devpath(optarg) != 0)
				return 1;
			break;

		case 'I': {
			char dev_line[256];
			FILE *ifp = my_fopen(optarg, "r");

			if (!ifp) {
				fprintf(stderr,
					"Invalid file for devices %s\n",
					optarg);
				return 1;
			}

			while (fscanf(ifp, "%s\n", dev_line) == 1)
				if (add_devpath(dev_line) != 0)
					return 1;
			break;
		}

		case 'r':
			debugfs_path = optarg;
			break;

		case 'o':
			output_name = optarg;
			break;
		case 'k':
			kill_running_trace = 1;
			break;
		case 'w':
			stop_watch = atoi(optarg);
			if (stop_watch <= 0) {
				fprintf(stderr,
					"Invalid stopwatch value (%d secs)\n",
					stop_watch);
				return 1;
			}
			break;
		case 'V':
		case 'v':
			printf("%s version %s\n", argv[0], blktrace_version);
			exit(0);
			/*NOTREACHED*/
		case 'b':
			buf_size = strtoul(optarg, NULL, 10);
			if (buf_size <= 0 || buf_size > 16*1024) {
				fprintf(stderr, "Invalid buffer size (%lu)\n",
					buf_size);
				return 1;
			}
			buf_size <<= 10;
			break;
		case 'n':
			buf_nr = strtoul(optarg, NULL, 10);
			if (buf_nr <= 0) {
				fprintf(stderr,
					"Invalid buffer nr (%lu)\n", buf_nr);
				return 1;
			}
			break;
		case 'D':
			output_dir = optarg;
			break;
		case 'h':
			net_mode = Net_client;
			strcpy(hostname, optarg);
			break;
		case 'l':
			net_mode = Net_server;
			break;
		case 'p':
			net_port = atoi(optarg);
			break;
		case 's':
			net_use_sendfile = 0;
			break;
		default:
			show_usage(argv[0]);
			exit(1);
			/*NOTREACHED*/
		}
	}

	while (optind < argc)
		if (add_devpath(argv[optind++]) != 0)
			return 1;

	if (net_mode != Net_server && ndevs == 0) {
		show_usage(argv[0]);
		return 1;
	}

	if (statfs(debugfs_path, &st) < 0 || st.f_type != (long)DEBUGFS_TYPE) {
		fprintf(stderr, "Invalid debug path %s: %d/%s\n",
			debugfs_path, errno, strerror(errno));
		return 1;
	}

	if (act_mask_tmp != 0)
		act_mask = act_mask_tmp;

	if (net_mode == Net_client && net_setup_addr())
		return 1;

	/*
	 * Set up for appropriate PFD handler based upon output name.
	 */
	if (net_client_use_sendfile())
		handle_pfds = handle_pfds_netclient;
	else if (net_client_use_send())
		handle_pfds = handle_pfds_entries;
	else if (output_name && (strcmp(output_name, "-") == 0)) {
		piped_output = 1;
		handle_pfds = handle_pfds_entries;
		pfp = stdout;
		setvbuf(pfp, NULL, _IONBF, 0);
	} else
		handle_pfds = handle_pfds_file;
	return 0;
}

static void ch_add_connection(struct net_server_s *ns, struct cl_host *ch,
			      int fd)
{
	struct cl_conn *nc;

	nc = malloc(sizeof(*nc));
	memset(nc, 0, sizeof(*nc));

	time(&nc->connect_time);
	nc->ch = ch;
	nc->fd = fd;
	nc->ncpus = -1;

	list_add_tail(&nc->ch_head, &ch->conn_list);
	ch->connects++;

	list_add_tail(&nc->ns_head, &ns->conn_list);
	ns->connects++;
	ns->pfds = realloc(ns->pfds, (ns->connects+1) * sizeof(struct pollfd));
}

static void ch_rem_connection(struct net_server_s *ns, struct cl_host *ch,
			      struct cl_conn *nc)
{
	net_close_connection(&nc->fd);

	list_del(&nc->ch_head);
	ch->connects--;

	list_del(&nc->ns_head);
	ns->connects--;
	ns->pfds = realloc(ns->pfds, (ns->connects+1) * sizeof(struct pollfd));

	free(nc);
}

static struct cl_host *net_find_client_host(struct net_server_s *ns,
					    struct in_addr cl_in_addr)
{
	struct list_head *p;

	__list_for_each(p, &ns->ch_list) {
		struct cl_host *ch = list_entry(p, struct cl_host, head);

		if (in_addr_eq(ch->cl_in_addr, cl_in_addr))
			return ch;
	}

	return NULL;
}

static struct cl_host *net_add_client_host(struct net_server_s *ns,
					   struct sockaddr_in *addr)
{
	struct cl_host *ch;

	ch = malloc(sizeof(*ch));
	memset(ch, 0, sizeof(*ch));

	ch->ns = ns;
	ch->cl_in_addr = addr->sin_addr;
	list_add_tail(&ch->head, &ns->ch_list);
	ns->nchs++;

	ch->hostname = strdup(inet_ntoa(addr->sin_addr));
	printf("server: connection from %s\n", ch->hostname);

	INIT_LIST_HEAD(&ch->conn_list);
	INIT_LIST_HEAD(&ch->devpaths);

	return ch;
}

static void device_done(struct devpath *dpp, int ncpus)
{
	int cpu;
	struct io_info *iop;

	for (cpu = 0, iop = dpp->ios; cpu < ncpus; cpu++, iop++)
		close_iop(iop);

	list_del(&dpp->head);
	dpp_free(dpp);
}

static void net_ch_remove(struct cl_host *ch, int ncpus)
{
	struct list_head *p, *q;
	struct net_server_s *ns = ch->ns;

	list_for_each_safe(p, q, &ch->devpaths) {
		struct devpath *dpp = list_entry(p, struct devpath, head);
		device_done(dpp, ncpus);
	}

	list_for_each_safe(p, q, &ch->conn_list) {
		struct cl_conn *nc = list_entry(p, struct cl_conn, ch_head);

		ch_rem_connection(ns, ch, nc);
	}

	list_del(&ch->head);
	ns->nchs--;

	if (ch->hostname)
		free(ch->hostname);
	free(ch);
}

static void net_add_connection(struct net_server_s *ns)
{
	int fd;
	struct cl_host *ch;
	socklen_t socklen = sizeof(ns->addr);

	fd = my_accept(ns->listen_fd, (struct sockaddr *)&ns->addr, &socklen);
	if (fd < 0) {
		/*
		 * This is OK: we just won't accept this connection,
		 * nothing fatal.
		 */
		perror("accept");
	} else {
		ch = net_find_client_host(ns, ns->addr.sin_addr);
		if (!ch)
			ch = net_add_client_host(ns, &ns->addr);

		ch_add_connection(ns, ch, fd);
	}
}

static struct devpath *nc_add_dpp(struct cl_conn *nc,
				  struct blktrace_net_hdr *bnh,
				  time_t connect_time)
{
	int cpu;
	struct io_info *iop;
	struct devpath *dpp;

	dpp = malloc(sizeof(*dpp));
	memset(dpp, 0, sizeof(*dpp));

	dpp->buts_name = strdup(bnh->buts_name);
	dpp->path = strdup(bnh->buts_name);
	dpp->fd = -1;
	dpp->ch = nc->ch;
	dpp->cl_id = bnh->cl_id;
	dpp->cl_connect_time = connect_time;
	dpp->ncpus = nc->ncpus;
	dpp->stats = calloc(dpp->ncpus, sizeof(*dpp->stats));
	memset(dpp->stats, 0, dpp->ncpus * sizeof(*dpp->stats));

	list_add_tail(&dpp->head, &nc->ch->devpaths);
	nc->ch->ndevs++;

	dpp->ios = calloc(nc->ncpus, sizeof(*iop));
	memset(dpp->ios, 0, ndevs * sizeof(*iop));

	for (cpu = 0, iop = dpp->ios; cpu < nc->ncpus; cpu++, iop++) {
		iop->dpp = dpp;
		iop->nc = nc;
		init_mmap_info(&iop->mmap_info);

		if (iop_open(iop, cpu))
			goto err;
	}

	return dpp;

err:
	/*
	 * Need to unravel what's been done...
	 */
	while (cpu >= 0)
		close_iop(&dpp->ios[cpu--]);
	dpp_free(dpp);

	return NULL;
}

static struct devpath *nc_find_dpp(struct cl_conn *nc,
				   struct blktrace_net_hdr *bnh)
{
	struct list_head *p;
	time_t connect_time = nc->connect_time;

	__list_for_each(p, &nc->ch->devpaths) {
		struct devpath *dpp = list_entry(p, struct devpath, head);

		if (!strcmp(dpp->buts_name, bnh->buts_name))
			return dpp;

		if (dpp->cl_id == bnh->cl_id)
			connect_time = dpp->cl_connect_time;
	}

	return nc_add_dpp(nc, bnh, connect_time);
}

static void net_client_read_data(struct cl_conn *nc, struct devpath *dpp,
				 struct blktrace_net_hdr *bnh)
{
	int ret;
	struct io_info *iop = &dpp->ios[bnh->cpu];
	struct mmap_info *mip = &iop->mmap_info;

	if (setup_mmap(iop->ofd, bnh->len, &iop->mmap_info)) {
		fprintf(stderr, "ncd(%s:%d): mmap failed\n",
			nc->ch->hostname, nc->fd);
		exit(1);
	}

	ret = net_recv_data(nc->fd, mip->fs_buf + mip->fs_off, bnh->len);
	if (ret > 0) {
		pdc_dr_update(dpp, bnh->cpu, ret);
		mip->fs_size += ret;
		mip->fs_off += ret;
	} else if (ret < 0)
		exit(1);
}

/*
 * Returns 1 if we closed a host - invalidates other polling information
 * that may be present.
 */
static int net_client_data(struct cl_conn *nc)
{
	int ret;
	struct devpath *dpp;
	struct blktrace_net_hdr bnh;

	ret = net_get_header(nc, &bnh);
	if (ret == 0)
		return 0;

	if (ret < 0) {
		fprintf(stderr, "ncd(%d): header read failed\n", nc->fd);
		exit(1);
	}

	if (data_is_native == -1 && check_data_endianness(bnh.magic)) {
		fprintf(stderr, "ncd(%d): received data is bad\n", nc->fd);
		exit(1);
	}

	if (!data_is_native) {
		bnh.magic = be32_to_cpu(bnh.magic);
		bnh.cpu = be32_to_cpu(bnh.cpu);
		bnh.max_cpus = be32_to_cpu(bnh.max_cpus);
		bnh.len = be32_to_cpu(bnh.len);
		bnh.cl_id = be32_to_cpu(bnh.cl_id);
		bnh.buf_size = be32_to_cpu(bnh.buf_size);
		bnh.buf_nr = be32_to_cpu(bnh.buf_nr);
		bnh.page_size = be32_to_cpu(bnh.page_size);
	}

	if ((bnh.magic & 0xffffff00) != BLK_IO_TRACE_MAGIC) {
		fprintf(stderr, "ncd(%s:%d): bad data magic\n",
			nc->ch->hostname, nc->fd);
		exit(1);
	}

	if (nc->ncpus == -1)
		nc->ncpus = bnh.max_cpus;

	/*
	 * len == 0 means the other end is sending us a new connection/dpp
	 * len == 1 means that the other end signalled end-of-run
	 */
	dpp = nc_find_dpp(nc, &bnh);
	if (bnh.len == 0) {
		/*
		 * Just adding in the dpp above is enough
		 */
		ack_open_close(nc->fd, dpp->buts_name);
		nc->ch->cl_opens++;
	} else if (bnh.len == 1) {
		/*
		 * overload cpu count with dropped events
		 */
		dpp->drops = bnh.cpu;

		ack_open_close(nc->fd, dpp->buts_name);
		if (--nc->ch->cl_opens == 0) {
			show_stats(&nc->ch->devpaths);
			net_ch_remove(nc->ch, nc->ncpus);
			return 1;
		}
	} else
		net_client_read_data(nc, dpp, &bnh);

	return 0;
}

static void handle_client_data(struct net_server_s *ns, int events)
{
	struct cl_conn *nc;
	struct pollfd *pfd;
	struct list_head *p, *q;

	pfd = &ns->pfds[1];
	list_for_each_safe(p, q, &ns->conn_list) {
		if (pfd->revents & POLLIN) {
			nc = list_entry(p, struct cl_conn, ns_head);

			if (net_client_data(nc) || --events == 0)
				break;
		}
		pfd++;
	}
}

static void net_setup_pfds(struct net_server_s *ns)
{
	struct pollfd *pfd;
	struct list_head *p;

	ns->pfds[0].fd = ns->listen_fd;
	ns->pfds[0].events = POLLIN;

	pfd = &ns->pfds[1];
	__list_for_each(p, &ns->conn_list) {
		struct cl_conn *nc = list_entry(p, struct cl_conn, ns_head);

		pfd->fd = nc->fd;
		pfd->events = POLLIN;
		pfd++;
	}
}

static int net_server_handle_connections(struct net_server_s *ns)
{
	int events;

	printf("server: waiting for connections...\n");

	while (!done) {
		net_setup_pfds(ns);
		events = poll(ns->pfds, ns->connects + 1, -1);
		if (events < 0) {
			if (errno != EINTR) {
				perror("FATAL: poll error");
				return 1;
			}
		} else if (events > 0) {
			if (ns->pfds[0].revents & POLLIN) {
				net_add_connection(ns);
				events--;
			}

			if (events)
				handle_client_data(ns, events);
		}
	}

	return 0;
}

static int net_server(void)
{
	int fd, opt;
	int ret = 1;
	struct net_server_s net_server;
	struct net_server_s *ns = &net_server;

	memset(ns, 0, sizeof(*ns));
	INIT_LIST_HEAD(&ns->ch_list);
	INIT_LIST_HEAD(&ns->conn_list);
	ns->pfds = malloc(sizeof(struct pollfd));

	fd = my_socket(AF_INET, SOCK_STREAM, 0);
	if (fd < 0) {
		perror("server: socket");
		goto out;
	}

	opt = 1;
	if (setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) < 0) {
		perror("setsockopt");
		goto out;
	}

	memset(&ns->addr, 0, sizeof(ns->addr));
	ns->addr.sin_family = AF_INET;
	ns->addr.sin_addr.s_addr = htonl(INADDR_ANY);
	ns->addr.sin_port = htons(net_port);

	if (bind(fd, (struct sockaddr *) &ns->addr, sizeof(ns->addr)) < 0) {
		perror("bind");
		goto out;
	}

	if (listen(fd, 1) < 0) {
		perror("listen");
		goto out;
	}

	/*
	 * The actual server looping is done here:
	 */
	ns->listen_fd = fd;
	ret = net_server_handle_connections(ns);

	/*
	 * Clean up and return...
	 */
out:
	free(ns->pfds);
	return ret;
}

static int run_tracers(void)
{
	atexit(exit_tracing);
	if (net_mode == Net_client)
		printf("blktrace: connecting to %s\n", hostname);

	setup_buts();

	if (use_tracer_devpaths()) {
		if (setup_tracer_devpaths())
			return 1;

		if (piped_output)
			handle_list = handle_list_file;
		else
			handle_list = handle_list_net;
	}

	start_tracers();
	if (nthreads_running == ncpus) {
		unblock_tracers();
		start_buts();
		if (net_mode == Net_client)
			printf("blktrace: connected!\n");
		if (stop_watch)
			alarm(stop_watch);
	} else
		stop_tracers();

	wait_tracers();
	if (nthreads_running == ncpus)
		show_stats(&devpaths);
	if (net_client_use_send())
		close_client_connections();
	del_tracers();

	return 0;
}

int main(int argc, char *argv[])
{
	int ret = 0;

	setlocale(LC_NUMERIC, "en_US");
	pagesize = getpagesize();
	ncpus = sysconf(_SC_NPROCESSORS_ONLN);
	if (ncpus < 0) {
		fprintf(stderr, "sysconf(_SC_NPROCESSORS_ONLN) failed %d/%s\n",
			errno, strerror(errno));
		ret = 1;
		goto out;
	} else if (handle_args(argc, argv)) {
		ret = 1;
		goto out;
	}

	signal(SIGINT, handle_sigint);
	signal(SIGHUP, handle_sigint);
	signal(SIGTERM, handle_sigint);
	signal(SIGALRM, handle_sigint);
	signal(SIGPIPE, SIG_IGN);

	if (kill_running_trace) {
		struct devpath *dpp;
		struct list_head *p;

		__list_for_each(p, &devpaths) {
			dpp = list_entry(p, struct devpath, head);
			if (__stop_trace(dpp->fd)) {
				fprintf(stderr,
					"BLKTRACETEARDOWN %s failed: %d/%s\n",
					dpp->path, errno, strerror(errno));
			}
		}
	} else if (net_mode == Net_server) {
		if (output_name) {
			fprintf(stderr, "-o ignored in server mode\n");
			output_name = NULL;
		}
		ret = net_server();
	} else
		ret = run_tracers();

out:
	if (pfp)
		fclose(pfp);
	rel_devpaths();
	return ret;
}
