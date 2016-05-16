/*
 * I/O monitor based on block queue trace data
 *
 * Copyright IBM Corp. 2008
 *
 * Author(s): Martin Peschke <mp3@de.ibm.com>
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

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <getopt.h>
#include <errno.h>
#include <locale.h>
#include <libgen.h>
#include <sys/msg.h>
#include <pthread.h>
#include <time.h>

#include "blktrace.h"
#include "rbtree.h"
#include "jhash.h"
#include "blkiomon.h"

struct trace {
	struct blk_io_trace bit;
	struct rb_node node;
	struct trace *next;
	long sequence;
};

struct rb_search {
	struct rb_node **node_ptr;
	struct rb_node *parent;
};

struct dstat_msg {
	long mtype;
	struct blkiomon_stat stat;
};

struct dstat {
	struct dstat_msg msg;
	struct rb_node node;
	struct dstat *next;
};

struct output {
	char *fn;
	FILE *fp;
	char *buf;
	int pipe;
};

static char blkiomon_version[] = "0.3";

static FILE *ifp;
static int interval = -1;

static struct trace *vacant_traces_list = NULL;
static int vacant_traces = 0;

#define TRACE_HASH_SIZE 128
struct trace *thash[TRACE_HASH_SIZE] = {};

static struct dstat *vacant_dstats_list = NULL;
static struct rb_root dstat_tree[2] = { RB_ROOT, RB_ROOT };
static struct dstat *dstat_list[2] = {};
static int dstat_curr = 0;

static struct output drvdata, human, binary, debug;

static char *msg_q_name = NULL;
static int msg_q_id = -1, msg_q = -1;
static long msg_id = -1;

static pthread_t interval_thread;
static pthread_mutex_t dstat_mutex = PTHREAD_MUTEX_INITIALIZER;

int data_is_native = -1;

static int up = 1;

/* debugging */
static long leftover = 0, driverdata = 0, match = 0, mismatch = 0, sequence = 0;

static void dump_bit(struct trace *t, const char *descr)
{
	struct blk_io_trace *bit = &t->bit;

	if (!debug.fn)
		return;

	fprintf(debug.fp, "--- %s ---\n", descr);
	fprintf(debug.fp, "magic    %16d\n", bit->magic);
	fprintf(debug.fp, "sequence %16d\n", bit->sequence);
	fprintf(debug.fp, "time     %16ld\n", (unsigned long)bit->time);
	fprintf(debug.fp, "sector   %16ld\n", (unsigned long)bit->sector);
	fprintf(debug.fp, "bytes    %16d\n", bit->bytes);
	fprintf(debug.fp, "action   %16x\n", bit->action);
	fprintf(debug.fp, "pid      %16d\n", bit->pid);
	fprintf(debug.fp, "device   %16d\n", bit->device);
	fprintf(debug.fp, "cpu      %16d\n", bit->cpu);
	fprintf(debug.fp, "error    %16d\n", bit->error);
	fprintf(debug.fp, "pdu_len  %16d\n", bit->pdu_len);

	fprintf(debug.fp, "order    %16ld\n", t->sequence);
}

static void dump_bits(struct trace *t1, struct trace *t2, const char *descr)
{
	struct blk_io_trace *bit1 = &t1->bit;
	struct blk_io_trace *bit2 = &t2->bit;

	if (!debug.fn)
		return;

	fprintf(debug.fp, "--- %s ---\n", descr);
	fprintf(debug.fp, "magic    %16d %16d\n", bit1->magic, bit2->magic);
	fprintf(debug.fp, "sequence %16d %16d\n",
		bit1->sequence, bit2->sequence);
	fprintf(debug.fp, "time     %16ld %16ld\n",
		(unsigned long)bit1->time, (unsigned long)bit2->time);
	fprintf(debug.fp, "sector   %16ld %16ld\n",
		(unsigned long)bit1->sector, (unsigned long)bit2->sector);
	fprintf(debug.fp, "bytes    %16d %16d\n", bit1->bytes, bit2->bytes);
	fprintf(debug.fp, "action   %16x %16x\n", bit1->action, bit2->action);
	fprintf(debug.fp, "pid      %16d %16d\n", bit1->pid, bit2->pid);
	fprintf(debug.fp, "device   %16d %16d\n", bit1->device, bit2->device);
	fprintf(debug.fp, "cpu      %16d %16d\n", bit1->cpu, bit2->cpu);
	fprintf(debug.fp, "error    %16d %16d\n", bit1->error, bit2->error);
	fprintf(debug.fp, "pdu_len  %16d %16d\n", bit1->pdu_len, bit2->pdu_len);

	fprintf(debug.fp, "order    %16ld %16ld\n", t1->sequence, t2->sequence);
}

static struct dstat *blkiomon_alloc_dstat(void)
{
	struct dstat *dstat;

	if (vacant_dstats_list) {
		dstat = vacant_dstats_list;
		vacant_dstats_list = dstat->next;
	} else
		dstat = malloc(sizeof(*dstat));
	if (!dstat) {
		fprintf(stderr,
			"blkiomon: could not allocate device statistic");
		return NULL;
	}

	blkiomon_stat_init(&dstat->msg.stat);
	return dstat;
}

static struct dstat *blkiomon_find_dstat(struct rb_search *search, __u32 device)
{
	struct rb_node **p = &(dstat_tree[dstat_curr].rb_node);
	struct rb_node *parent = NULL;
	struct dstat *dstat;

	while (*p) {
		parent = *p;

		dstat = rb_entry(parent, struct dstat, node);

		if (dstat->msg.stat.device < device)
			p = &(*p)->rb_left;
		else if (dstat->msg.stat.device > device)
			p = &(*p)->rb_right;
		else
			return dstat;
	}
	search->node_ptr = p;
	search->parent = parent;
	return NULL;
}

static struct dstat *blkiomon_get_dstat(__u32 device)
{
	struct dstat *dstat;
	struct rb_search search;

	pthread_mutex_lock(&dstat_mutex);

	dstat = blkiomon_find_dstat(&search, device);
	if (dstat)
		goto out;

	dstat = blkiomon_alloc_dstat();
	if (!dstat)
		goto out;

	dstat->msg.stat.device = device;

	rb_link_node(&dstat->node, search.parent, search.node_ptr);
	rb_insert_color(&dstat->node, &dstat_tree[dstat_curr]);

	dstat->next = dstat_list[dstat_curr];
	dstat_list[dstat_curr] = dstat;

out:
	pthread_mutex_unlock(&dstat_mutex);
	return dstat;
}

static int blkiomon_output_msg_q(struct dstat *dstat)
{
	if (!msg_q_name)
		return 0;

	dstat->msg.mtype = msg_id;
	return msgsnd(msg_q, &dstat->msg, sizeof(struct blkiomon_stat), 0);
}

static int blkiomon_output_binary(struct dstat *dstat)
{
	struct blkiomon_stat *p = &dstat->msg.stat;

	if (!binary.fn)
		return 0;

	if (fwrite(p, sizeof(*p), 1, binary.fp) != 1)
		goto failed;
	if (binary.pipe && fflush(binary.fp))
		goto failed;
	return 0;

failed:
	fprintf(stderr, "blkiomon: could not write to %s\n", binary.fn);
	fclose(binary.fp);
	binary.fn = NULL;
	return 1;
}

static struct dstat *blkiomon_output(struct dstat *head, struct timespec *ts)
{
	struct dstat *dstat, *tail = NULL;

	for (dstat = head; dstat; dstat = dstat->next) {
		dstat->msg.stat.time = ts->tv_sec;
		blkiomon_stat_print(human.fp, &dstat->msg.stat);
		blkiomon_stat_to_be(&dstat->msg.stat);
		blkiomon_output_binary(dstat);
		blkiomon_output_msg_q(dstat);
		tail = dstat;
	}
	return tail;
}

static void *blkiomon_interval(void *data)
{
	struct timespec wake, r;
	struct dstat *head, *tail;
	int finished;

	clock_gettime(CLOCK_REALTIME, &wake);

	while (1) {
		wake.tv_sec += interval;
		if (clock_nanosleep(CLOCK_REALTIME, TIMER_ABSTIME, &wake, &r)) {
			fprintf(stderr, "blkiomon: interrupted sleep");
			continue;
		}

		/* grab tree and make data gatherer build up another tree */
		pthread_mutex_lock(&dstat_mutex);
		finished = dstat_curr;
		dstat_curr = dstat_curr ? 0 : 1;
		pthread_mutex_unlock(&dstat_mutex);

		head = dstat_list[finished];
		if (!head)
			continue;
		dstat_list[finished] = NULL;
		dstat_tree[finished] = RB_ROOT;
		tail = blkiomon_output(head, &wake);

		pthread_mutex_lock(&dstat_mutex);
		tail->next = vacant_dstats_list;
		vacant_dstats_list = head;
		pthread_mutex_unlock(&dstat_mutex);
	}
	return data;
}

#define BLK_DATADIR(a) (((a) >> BLK_TC_SHIFT) & (BLK_TC_READ | BLK_TC_WRITE))

static int blkiomon_account(struct blk_io_trace *bit_d,
			    struct blk_io_trace *bit_c)
{
	struct dstat *dstat;
	struct blkiomon_stat *p;
	__u64 d2c = (bit_c->time - bit_d->time) / 1000; /* ns -> us */
	__u32 size = bit_d->bytes;
	__u64 thrput = size * 1000 / d2c;

	dstat = blkiomon_get_dstat(bit_d->device);
	if (!dstat)
		return 1;
	p = &dstat->msg.stat;

	if (BLK_DATADIR(bit_c->action) & BLK_TC_READ) {
		minmax_account(&p->thrput_r, thrput);
		minmax_account(&p->size_r, size);
		minmax_account(&p->d2c_r, d2c);
	} else if (BLK_DATADIR(bit_c->action) & BLK_TC_WRITE) {
		minmax_account(&p->thrput_w, thrput);
		minmax_account(&p->size_w, size);
		minmax_account(&p->d2c_w, d2c);
	} else
		p->bidir++;

	histlog2_account(p->size_hist, size, &size_hist);
	histlog2_account(p->d2c_hist, d2c, &d2c_hist);
	return 0;
}

static struct trace *blkiomon_alloc_trace(void)
{
	struct trace *t = vacant_traces_list;
	if (t) {
		vacant_traces_list = t->next;
		vacant_traces--;
	} else
		t = malloc(sizeof(*t));
	memset(t, 0, sizeof(*t));
	return t;
}

static void blkiomon_free_trace(struct trace *t)
{
	if (vacant_traces < 256) {
		t->next = vacant_traces_list;
		vacant_traces_list = t;
		vacant_traces++;
	} else
		free(t);
}

static int action(int a)
{
	int bits = BLK_TC_WRITE | BLK_TC_READ | BLK_TC_FS | BLK_TC_PC;
	return a & (BLK_TC_ACT(bits));
}

static void blkiomon_store_trace(struct trace *t)
{
	int i = t->bit.sector % TRACE_HASH_SIZE;

	t->next = thash[i];
	thash[i] = t;
}

static struct trace *blkiomon_fetch_trace(struct blk_io_trace *bit)
{
	int i = bit->sector % TRACE_HASH_SIZE;
	struct trace *t, *prev = NULL;

	for (t = thash[i]; t; t = t->next) {
		if (t->bit.device == bit->device &&
		    t->bit.sector == bit->sector &&
		    action(t->bit.action) == action(bit->action)) {
			if (prev)
				prev->next = t->next;
			else
				thash[i] = t->next;
			return t;
		}
		prev = t;
	}
	return NULL;
}

static struct trace *blkiomon_do_trace(struct trace *t)
{
	struct trace *t_stored, *t_old, *t_young;

	/* store trace if there is no match yet */
	t_stored = blkiomon_fetch_trace(&t->bit);
	if (!t_stored) {
		blkiomon_store_trace(t);
		return blkiomon_alloc_trace();
	}

	/* figure out older trace and younger trace */
	if (t_stored->bit.time < t->bit.time) {
		t_old = t_stored;
		t_young = t;
	} else {
		t_old = t;
		t_young = t_stored;
	}

	/* we need an older D trace and a younger C trace */
	if (t_old->bit.action & BLK_TC_ACT(BLK_TC_ISSUE) &&
	    t_young->bit.action & BLK_TC_ACT(BLK_TC_COMPLETE)) {
		/* matching D and C traces - update statistics */
		match++;
		blkiomon_account(&t_old->bit, &t_young->bit);
		blkiomon_free_trace(t_stored);
		return t;
	}

	/* no matching D and C traces - keep more recent trace */
	dump_bits(t_old, t_young, "mismatch");
	mismatch++;
	blkiomon_store_trace(t_young);
	return t_old;
}

static int blkiomon_dump_drvdata(struct blk_io_trace *bit, void *pdu_buf)
{
	if (!drvdata.fn)
		return 0;

	if (fwrite(bit, sizeof(*bit), 1, drvdata.fp) != 1)
		goto failed;
	if (fwrite(pdu_buf, bit->pdu_len, 1, drvdata.fp) != 1)
		goto failed;
	if (drvdata.pipe && fflush(drvdata.fp))
		goto failed;
	return 0;

failed:
	fprintf(stderr, "blkiomon: could not write to %s\n", drvdata.fn);
	fclose(drvdata.fp);
	drvdata.fn = NULL;
	return 1;
}

static int blkiomon_do_fifo(void)
{
	struct trace *t;
	struct blk_io_trace *bit;
	void *pdu_buf = NULL;

	t = blkiomon_alloc_trace();
	if (!t)
		return 1;
	bit = &t->bit;

	while (up) {
		if (fread(bit, sizeof(*bit), 1, ifp) != 1) {
			if (!feof(ifp))
				fprintf(stderr,
					"blkiomon: could not read trace");
			break;
		}
		if (ferror(ifp)) {
			clearerr(ifp);
			fprintf(stderr, "blkiomon: error while reading trace");
			break;
		}

		if (data_is_native == -1 && check_data_endianness(bit->magic)) {
			fprintf(stderr, "blkiomon: endianess problem\n");
			break;
		}

		/* endianess */
		trace_to_cpu(bit);
		if (verify_trace(bit)) {
			fprintf(stderr, "blkiomon: bad trace\n");
			break;
		}

		/* read additional trace payload */
		if (bit->pdu_len) {
			pdu_buf = realloc(pdu_buf, bit->pdu_len);
			if (fread(pdu_buf, bit->pdu_len, 1, ifp) != 1) {
				clearerr(ifp);
				fprintf(stderr, "blkiomon: could not read payload\n");
				break;
			}
		}

		t->sequence = sequence++;

		/* forward low-level device driver trace to other tool */
		if (bit->action & BLK_TC_ACT(BLK_TC_DRV_DATA)) {
			driverdata++;
			if (blkiomon_dump_drvdata(bit, pdu_buf)) {
				fprintf(stderr, "blkiomon: could not send trace\n");
				break;
			}
			continue;
		}

		if (!(bit->action & BLK_TC_ACT(BLK_TC_ISSUE | BLK_TC_COMPLETE)))
			continue;

		/* try to find matching trace and update statistics */
		t = blkiomon_do_trace(t);
		if (!t) {
			fprintf(stderr, "blkiomon: could not alloc trace\n");
			break;
		}
		bit = &t->bit;
		/* t and bit will be recycled for next incoming trace */
	}
	blkiomon_free_trace(t);
	free(pdu_buf);
	return 0;
}

static int blkiomon_open_output(struct output *out)
{
	int mode, vbuf_size;

	if (!out->fn)
		return 0;

	if (!strcmp(out->fn, "-")) {
		out->fp = fdopen(STDOUT_FILENO, "w");
		mode = _IOLBF;
		vbuf_size = 4096;
		out->pipe = 1;
	} else {
		out->fp = fopen(out->fn, "w");
		mode = _IOFBF;
		vbuf_size = 128 * 1024;
		out->pipe = 0;
	}
	if (!out->fp)
		goto failed;
	out->buf = malloc(128 * 1024);
	if (setvbuf(out->fp, out->buf, mode, vbuf_size))
		goto failed;
	return 0;

failed:
	fprintf(stderr, "blkiomon: could not write to %s\n", out->fn);
	out->fn = NULL;
	free(out->buf);
	return 1;
}

static int blkiomon_open_msg_q(void)
{
	key_t key;

	if (!msg_q_name)
		return 0;
	if (!msg_q_id || msg_id <= 0)
		return 1;
	key = ftok(msg_q_name, msg_q_id);
	if (key == -1)
		return 1;
	while (up) {
		msg_q = msgget(key, S_IRWXU);
		if (msg_q >= 0)
			break;
	}
	return (msg_q >= 0 ? 0 : -1);
}

static void blkiomon_debug(void)
{
	int i;
	struct trace *t;

	if (!debug.fn)
		return;

	for (i = 0; i < TRACE_HASH_SIZE; i++)
		for (t = thash[i]; t; t = t->next) {
			dump_bit(t, "leftover");
			leftover++;
		}

	fprintf(debug.fp, "%ld leftover, %ld match, %ld mismatch, "
		"%ld driverdata, %ld overall\n",
		leftover, match, mismatch, driverdata, sequence);
}

#define S_OPTS "b:d:D:h:I:Q:q:m:V"

static char usage_str[] = "\n\nblkiomon " \
	"-I <interval>       | --interval=<interval>\n" \
	"[ -h <file>         | --human-readable=<file> ]\n" \
	"[ -b <file>         | --binary=<file> ]\n" \
	"[ -D <file>         | --debug=<file> ]\n" \
	"[ -Q <path name>    | --msg-queue=<path name>]\n" \
	"[ -q <msg queue id> | --msg-queue-id=<msg queue id>]\n" \
	"[ -m <msg id>       | --msg-id=<msg id>]\n" \
	"[ -V                | --version ]\n\n" \
	"\t-I   Sample interval.\n" \
	"\t-h   Human-readable output file.\n" \
	"\t-b   Binary output file.\n" \
	"\t-d   Output file for data emitted by low level device driver.\n" \
	"\t-D   Output file for debugging data.\n" \
	"\t-Qqm Output to message queue using given ID for messages.\n" \
	"\t-V   Print program version.\n\n";

static struct option l_opts[] = {
	{
		.name = "human-readable",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'h'
	},
	{
		.name = "binary",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'b'
	},
	{
		.name = "dump-lldd",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'd'
	},
	{
		.name = "debug",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'D'
	},
	{
		.name = "interval",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'I'
	},
	{
		.name = "msg-queue",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'Q'
	},
	{
		.name = "msg-queue-id",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'q'
	},
	{
		.name = "msg-id",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'm'
	},
	{
		.name = "version",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'V'
	},
	{
		.name = NULL,
	}
};

static void blkiomon_signal(int signal)
{
	fprintf(stderr, "blkiomon: terminated by signal\n");
	up = signal & 0;
}

int main(int argc, char *argv[])
{
	int c;

	signal(SIGALRM, blkiomon_signal);
	signal(SIGINT, blkiomon_signal);
	signal(SIGTERM, blkiomon_signal);
	signal(SIGQUIT, blkiomon_signal);

	while ((c = getopt_long(argc, argv, S_OPTS, l_opts, NULL)) != -1) {
		switch (c) {
		case 'h':
			human.fn = optarg;
			break;
		case 'b':
			binary.fn = optarg;
			break;
		case 'd':
			drvdata.fn = optarg;
			break;
		case 'D':
			debug.fn = optarg;
			break;
		case 'I':
			interval = atoi(optarg);
			break;
		case 'Q':
			msg_q_name = optarg;
			break;
		case 'q':
			msg_q_id = atoi(optarg);
			break;
		case 'm':
			msg_id = atoi(optarg);
			break;
		case 'V':
			printf("%s version %s\n", argv[0], blkiomon_version);
			return 0;
		default:
			fprintf(stderr, "Usage: %s", usage_str);
			return 1;
		}
	}

	if (interval <= 0) {
		fprintf(stderr, "Usage: %s", usage_str);
		return 1;
	}

	ifp = fdopen(STDIN_FILENO, "r");
	if (!ifp) {
		perror("blkiomon: could not open stdin for reading");
		return 1;
	}

	if (blkiomon_open_output(&human))
		return 1;
	if (blkiomon_open_output(&binary))
		return 1;
	if (blkiomon_open_output(&drvdata))
		return 1;
	if (blkiomon_open_output(&debug))
		return 1;
	if (blkiomon_open_msg_q())
		return 1;

	if (pthread_create(&interval_thread, NULL, blkiomon_interval, NULL)) {
		fprintf(stderr, "blkiomon: could not create thread");
		return 1;
	}

	blkiomon_do_fifo();

	blkiomon_debug();
	return 0;
}
