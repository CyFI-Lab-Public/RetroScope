/*
 * block queue tracing parse application
 *
 * Copyright (C) 2005 Jens Axboe <axboe@suse.de>
 * Copyright (C) 2006 Jens Axboe <axboe@kernel.dk>
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
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdio.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <getopt.h>
#include <errno.h>
#include <signal.h>
#include <locale.h>
#include <libgen.h>

#include "blktrace.h"
#include "rbtree.h"
#include "jhash.h"

static char blkparse_version[] = "1.0.1";

struct skip_info {
	unsigned long start, end;
	struct skip_info *prev, *next;
};

struct per_dev_info {
	dev_t dev;
	char *name;

	int backwards;
	unsigned long long events;
	unsigned long long first_reported_time;
	unsigned long long last_reported_time;
	unsigned long long last_read_time;
	struct io_stats io_stats;
	unsigned long skips;
	unsigned long long seq_skips;
	unsigned int max_depth[2];
	unsigned int cur_depth[2];

	struct rb_root rb_track;

	int nfiles;
	int ncpus;

	unsigned long *cpu_map;
	unsigned int cpu_map_max;

	struct per_cpu_info *cpus;
};

/*
 * some duplicated effort here, we can unify this hash and the ppi hash later
 */
struct process_pid_map {
	pid_t pid;
	char comm[16];
	struct process_pid_map *hash_next, *list_next;
};

#define PPM_HASH_SHIFT	(8)
#define PPM_HASH_SIZE	(1 << PPM_HASH_SHIFT)
#define PPM_HASH_MASK	(PPM_HASH_SIZE - 1)
static struct process_pid_map *ppm_hash_table[PPM_HASH_SIZE];

struct per_process_info {
	struct process_pid_map *ppm;
	struct io_stats io_stats;
	struct per_process_info *hash_next, *list_next;
	int more_than_one;

	/*
	 * individual io stats
	 */
	unsigned long long longest_allocation_wait[2];
	unsigned long long longest_dispatch_wait[2];
	unsigned long long longest_completion_wait[2];
};

#define PPI_HASH_SHIFT	(8)
#define PPI_HASH_SIZE	(1 << PPI_HASH_SHIFT)
#define PPI_HASH_MASK	(PPI_HASH_SIZE - 1)
static struct per_process_info *ppi_hash_table[PPI_HASH_SIZE];
static struct per_process_info *ppi_list;
static int ppi_list_entries;

static struct option l_opts[] = {
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
		.name = "batch",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'b'
	},
	{
		.name = "input-directory",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'D'
	},
	{
		.name = "dump-binary",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'd'
	},
	{
		.name = "format",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'f'
	},
	{
		.name = "format-spec",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'F'
	},
	{
		.name = "hash-by-name",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'h'
	},
	{
		.name = "input",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'i'
	},
	{
		.name = "no-msgs",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'M'
	},
	{
		.name = "output",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'o'
	},
	{
		.name = "no-text-output",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'O'
	},
	{
		.name = "quiet",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'q'
	},
	{
		.name = "per-program-stats",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 's'
	},
	{
		.name = "track-ios",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 't'
	},
	{
		.name = "stopwatch",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'w'
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
		.name = NULL,
	}
};

/*
 * for sorting the displayed output
 */
struct trace {
	struct blk_io_trace *bit;
	struct rb_node rb_node;
	struct trace *next;
	unsigned long read_sequence;
};

static struct rb_root rb_sort_root;
static unsigned long rb_sort_entries;

static struct trace *trace_list;

/*
 * allocation cache
 */
static struct blk_io_trace *bit_alloc_list;
static struct trace *t_alloc_list;

/*
 * for tracking individual ios
 */
struct io_track {
	struct rb_node rb_node;

	struct process_pid_map *ppm;
	__u64 sector;
	unsigned long long allocation_time;
	unsigned long long queue_time;
	unsigned long long dispatch_time;
	unsigned long long completion_time;
};

static int ndevices;
static struct per_dev_info *devices;
static char *get_dev_name(struct per_dev_info *, char *, int);
static int trace_rb_insert_last(struct per_dev_info *, struct trace *);

FILE *ofp = NULL;
static char *output_name;
static char *input_dir;

static unsigned long long genesis_time;
static unsigned long long last_allowed_time;
static unsigned long long stopwatch_start;	/* start from zero by default */
static unsigned long long stopwatch_end = -1ULL;	/* "infinity" */
static unsigned long read_sequence;

static int per_process_stats;
static int per_device_and_cpu_stats = 1;
static int track_ios;
static int ppi_hash_by_pid = 1;
static int verbose;
static unsigned int act_mask = -1U;
static int stats_printed;
static int bin_output_msgs = 1;
int data_is_native = -1;

static FILE *dump_fp;
static char *dump_binary;

static unsigned int t_alloc_cache;
static unsigned int bit_alloc_cache;

#define RB_BATCH_DEFAULT	(512)
static unsigned int rb_batch = RB_BATCH_DEFAULT;

static int pipeline;
static char *pipename;

static int text_output = 1;

#define is_done()	(*(volatile int *)(&done))
static volatile int done;

struct timespec		abs_start_time;
static unsigned long long start_timestamp;

static int have_drv_data = 0;

#define JHASH_RANDOM	(0x3af5f2ee)

#define CPUS_PER_LONG	(8 * sizeof(unsigned long))
#define CPU_IDX(cpu)	((cpu) / CPUS_PER_LONG)
#define CPU_BIT(cpu)	((cpu) & (CPUS_PER_LONG - 1))

static void output_binary(void *buf, int len)
{
	if (dump_binary) {
		size_t n = fwrite(buf, len, 1, dump_fp);
		if (n != 1) {
			perror(dump_binary);
			fclose(dump_fp);
			dump_binary = NULL;
		}
	}
}

static void resize_cpu_info(struct per_dev_info *pdi, int cpu)
{
	struct per_cpu_info *cpus = pdi->cpus;
	int ncpus = pdi->ncpus;
	int new_count = cpu + 1;
	int new_space, size;
	char *new_start;

	size = new_count * sizeof(struct per_cpu_info);
	cpus = realloc(cpus, size);
	if (!cpus) {
		char name[20];
		fprintf(stderr, "Out of memory, CPU info for device %s (%d)\n",
			get_dev_name(pdi, name, sizeof(name)), size);
		exit(1);
	}

	new_start = (char *)cpus + (ncpus * sizeof(struct per_cpu_info));
	new_space = (new_count - ncpus) * sizeof(struct per_cpu_info);
	memset(new_start, 0, new_space);

	pdi->ncpus = new_count;
	pdi->cpus = cpus;

	for (new_count = 0; new_count < pdi->ncpus; new_count++) {
		struct per_cpu_info *pci = &pdi->cpus[new_count];

		if (!pci->fd) {
			pci->fd = -1;
			memset(&pci->rb_last, 0, sizeof(pci->rb_last));
			pci->rb_last_entries = 0;
			pci->last_sequence = -1;
		}
	}
}

static struct per_cpu_info *get_cpu_info(struct per_dev_info *pdi, int cpu)
{
	struct per_cpu_info *pci;

	if (cpu >= pdi->ncpus)
		resize_cpu_info(pdi, cpu);

	pci = &pdi->cpus[cpu];
	pci->cpu = cpu;
	return pci;
}


static int resize_devices(char *name)
{
	int size = (ndevices + 1) * sizeof(struct per_dev_info);

	devices = realloc(devices, size);
	if (!devices) {
		fprintf(stderr, "Out of memory, device %s (%d)\n", name, size);
		return 1;
	}
	memset(&devices[ndevices], 0, sizeof(struct per_dev_info));
	devices[ndevices].name = name;
	ndevices++;
	return 0;
}

static struct per_dev_info *get_dev_info(dev_t dev)
{
	struct per_dev_info *pdi;
	int i;

	for (i = 0; i < ndevices; i++) {
		if (!devices[i].dev)
			devices[i].dev = dev;
		if (devices[i].dev == dev)
			return &devices[i];
	}

	if (resize_devices(NULL))
		return NULL;

	pdi = &devices[ndevices - 1];
	pdi->dev = dev;
	pdi->first_reported_time = 0;
	pdi->last_read_time = 0;

	return pdi;
}

static void insert_skip(struct per_cpu_info *pci, unsigned long start,
			unsigned long end)
{
	struct skip_info *sip;

	for (sip = pci->skips_tail; sip != NULL; sip = sip->prev) {
		if (end == (sip->start - 1)) {
			sip->start = start;
			return;
		} else if (start == (sip->end + 1)) {
			sip->end = end;
			return;
		}
	}

	sip = malloc(sizeof(struct skip_info));
	sip->start = start;
	sip->end = end;
	sip->prev = sip->next = NULL;
	if (pci->skips_tail == NULL)
		pci->skips_head = pci->skips_tail = sip;
	else {
		sip->prev = pci->skips_tail;
		pci->skips_tail->next = sip;
		pci->skips_tail = sip;
	}
}

static void remove_sip(struct per_cpu_info *pci, struct skip_info *sip)
{
	if (sip->prev == NULL) {
		if (sip->next == NULL)
			pci->skips_head = pci->skips_tail = NULL;
		else {
			pci->skips_head = sip->next;
			sip->next->prev = NULL;
		}
	} else if (sip->next == NULL) {
		pci->skips_tail = sip->prev;
		sip->prev->next = NULL;
	} else {
		sip->prev->next = sip->next;
		sip->next->prev = sip->prev;
	}

	sip->prev = sip->next = NULL;
	free(sip);
}

#define IN_SKIP(sip,seq) (((sip)->start <= (seq)) && ((seq) <= sip->end))
static int check_current_skips(struct per_cpu_info *pci, unsigned long seq)
{
	struct skip_info *sip;

	for (sip = pci->skips_tail; sip != NULL; sip = sip->prev) {
		if (IN_SKIP(sip, seq)) {
			if (sip->start == seq) {
				if (sip->end == seq)
					remove_sip(pci, sip);
				else
					sip->start += 1;
			} else if (sip->end == seq)
				sip->end -= 1;
			else {
				sip->end = seq - 1;
				insert_skip(pci, seq + 1, sip->end);
			}
			return 1;
		}
	}

	return 0;
}

static void collect_pdi_skips(struct per_dev_info *pdi)
{
	struct skip_info *sip;
	int cpu;

	pdi->skips = 0;
	pdi->seq_skips = 0;

	for (cpu = 0; cpu < pdi->ncpus; cpu++) {
		struct per_cpu_info *pci = &pdi->cpus[cpu];

		for (sip = pci->skips_head; sip != NULL; sip = sip->next) {
			pdi->skips++;
			pdi->seq_skips += (sip->end - sip->start + 1);
			if (verbose)
				fprintf(stderr,"(%d,%d): skipping %lu -> %lu\n",
					MAJOR(pdi->dev), MINOR(pdi->dev),
					sip->start, sip->end);
		}
	}
}

static void cpu_mark_online(struct per_dev_info *pdi, unsigned int cpu)
{
	if (cpu >= pdi->cpu_map_max || !pdi->cpu_map) {
		int new_max = (cpu + CPUS_PER_LONG) & ~(CPUS_PER_LONG - 1);
		unsigned long *map = malloc(new_max / sizeof(long));

		memset(map, 0, new_max / sizeof(long));

		if (pdi->cpu_map) {
			memcpy(map, pdi->cpu_map, pdi->cpu_map_max / sizeof(long));
			free(pdi->cpu_map);
		}

		pdi->cpu_map = map;
		pdi->cpu_map_max = new_max;
	}

	pdi->cpu_map[CPU_IDX(cpu)] |= (1UL << CPU_BIT(cpu));
}

static inline void cpu_mark_offline(struct per_dev_info *pdi, int cpu)
{
	pdi->cpu_map[CPU_IDX(cpu)] &= ~(1UL << CPU_BIT(cpu));
}

static inline int cpu_is_online(struct per_dev_info *pdi, int cpu)
{
	return (pdi->cpu_map[CPU_IDX(cpu)] & (1UL << CPU_BIT(cpu))) != 0;
}

static inline int ppm_hash_pid(pid_t pid)
{
	return jhash_1word(pid, JHASH_RANDOM) & PPM_HASH_MASK;
}

static struct process_pid_map *find_ppm(pid_t pid)
{
	const int hash_idx = ppm_hash_pid(pid);
	struct process_pid_map *ppm;

	ppm = ppm_hash_table[hash_idx];
	while (ppm) {
		if (ppm->pid == pid)
			return ppm;

		ppm = ppm->hash_next;
	}

	return NULL;
}

static struct process_pid_map *add_ppm_hash(pid_t pid, const char *name)
{
	const int hash_idx = ppm_hash_pid(pid);
	struct process_pid_map *ppm;

	ppm = find_ppm(pid);
	if (!ppm) {
		ppm = malloc(sizeof(*ppm));
		memset(ppm, 0, sizeof(*ppm));
		ppm->pid = pid;
		strcpy(ppm->comm, name);
		ppm->hash_next = ppm_hash_table[hash_idx];
		ppm_hash_table[hash_idx] = ppm;
	}

	return ppm;
}

static void handle_notify(struct blk_io_trace *bit)
{
	void	*payload = (caddr_t) bit + sizeof(*bit);
	__u32	two32[2];

	switch (bit->action) {
	case BLK_TN_PROCESS:
		add_ppm_hash(bit->pid, payload);
		break;

	case BLK_TN_TIMESTAMP:
		if (bit->pdu_len != sizeof(two32))
			return;
		memcpy(two32, payload, sizeof(two32));
		if (!data_is_native) {
			two32[0] = be32_to_cpu(two32[0]);
			two32[1] = be32_to_cpu(two32[1]);
		}
		start_timestamp = bit->time;
		abs_start_time.tv_sec  = two32[0];
		abs_start_time.tv_nsec = two32[1];
		if (abs_start_time.tv_nsec < 0) {
			abs_start_time.tv_sec--;
			abs_start_time.tv_nsec += 1000000000;
		}

		break;

	case BLK_TN_MESSAGE:
		if (bit->pdu_len > 0) {
			char msg[bit->pdu_len+1];

			memcpy(msg, (char *)payload, bit->pdu_len);
			msg[bit->pdu_len] = '\0';

			fprintf(ofp,
				"%3d,%-3d %2d %8s %5d.%09lu %5u %2s %3s %s\n",
				MAJOR(bit->device), MINOR(bit->device),
				bit->cpu, "0", (int) SECONDS(bit->time),
				(unsigned long) NANO_SECONDS(bit->time),
				0, "m", "N", msg);
		}
		break;

	default:
		/* Ignore unknown notify events */
		;
	}
}

char *find_process_name(pid_t pid)
{
	struct process_pid_map *ppm = find_ppm(pid);

	if (ppm)
		return ppm->comm;

	return NULL;
}

static inline int ppi_hash_pid(pid_t pid)
{
	return jhash_1word(pid, JHASH_RANDOM) & PPI_HASH_MASK;
}

static inline int ppi_hash_name(const char *name)
{
	return jhash(name, 16, JHASH_RANDOM) & PPI_HASH_MASK;
}

static inline int ppi_hash(struct per_process_info *ppi)
{
	struct process_pid_map *ppm = ppi->ppm;

	if (ppi_hash_by_pid)
		return ppi_hash_pid(ppm->pid);

	return ppi_hash_name(ppm->comm);
}

static inline void add_ppi_to_hash(struct per_process_info *ppi)
{
	const int hash_idx = ppi_hash(ppi);

	ppi->hash_next = ppi_hash_table[hash_idx];
	ppi_hash_table[hash_idx] = ppi;
}

static inline void add_ppi_to_list(struct per_process_info *ppi)
{
	ppi->list_next = ppi_list;
	ppi_list = ppi;
	ppi_list_entries++;
}

static struct per_process_info *find_ppi_by_name(char *name)
{
	const int hash_idx = ppi_hash_name(name);
	struct per_process_info *ppi;

	ppi = ppi_hash_table[hash_idx];
	while (ppi) {
		struct process_pid_map *ppm = ppi->ppm;

		if (!strcmp(ppm->comm, name))
			return ppi;

		ppi = ppi->hash_next;
	}

	return NULL;
}

static struct per_process_info *find_ppi_by_pid(pid_t pid)
{
	const int hash_idx = ppi_hash_pid(pid);
	struct per_process_info *ppi;

	ppi = ppi_hash_table[hash_idx];
	while (ppi) {
		struct process_pid_map *ppm = ppi->ppm;

		if (ppm->pid == pid)
			return ppi;

		ppi = ppi->hash_next;
	}

	return NULL;
}

static struct per_process_info *find_ppi(pid_t pid)
{
	struct per_process_info *ppi;
	char *name;

	if (ppi_hash_by_pid)
		return find_ppi_by_pid(pid);

	name = find_process_name(pid);
	if (!name)
		return NULL;

	ppi = find_ppi_by_name(name);
	if (ppi && ppi->ppm->pid != pid)
		ppi->more_than_one = 1;

	return ppi;
}

/*
 * struct trace and blktrace allocation cache, we do potentially
 * millions of mallocs for these structures while only using at most
 * a few thousand at the time
 */
static inline void t_free(struct trace *t)
{
	if (t_alloc_cache < 1024) {
		t->next = t_alloc_list;
		t_alloc_list = t;
		t_alloc_cache++;
	} else
		free(t);
}

static inline struct trace *t_alloc(void)
{
	struct trace *t = t_alloc_list;

	if (t) {
		t_alloc_list = t->next;
		t_alloc_cache--;
		return t;
	}

	return malloc(sizeof(*t));
}

static inline void bit_free(struct blk_io_trace *bit)
{
	if (bit_alloc_cache < 1024 && !bit->pdu_len) {
		/*
		 * abuse a 64-bit field for a next pointer for the free item
		 */
		bit->time = (__u64) (unsigned long) bit_alloc_list;
		bit_alloc_list = (struct blk_io_trace *) bit;
		bit_alloc_cache++;
	} else
		free(bit);
}

static inline struct blk_io_trace *bit_alloc(void)
{
	struct blk_io_trace *bit = bit_alloc_list;

	if (bit) {
		bit_alloc_list = (struct blk_io_trace *) (unsigned long) \
				 bit->time;
		bit_alloc_cache--;
		return bit;
	}

	return malloc(sizeof(*bit));
}

static inline void __put_trace_last(struct per_dev_info *pdi, struct trace *t)
{
	struct per_cpu_info *pci = get_cpu_info(pdi, t->bit->cpu);

	rb_erase(&t->rb_node, &pci->rb_last);
	pci->rb_last_entries--;

	bit_free(t->bit);
	t_free(t);
}

static void put_trace(struct per_dev_info *pdi, struct trace *t)
{
	rb_erase(&t->rb_node, &rb_sort_root);
	rb_sort_entries--;

	trace_rb_insert_last(pdi, t);
}

static inline int trace_rb_insert(struct trace *t, struct rb_root *root)
{
	struct rb_node **p = &root->rb_node;
	struct rb_node *parent = NULL;
	struct trace *__t;

	while (*p) {
		parent = *p;

		__t = rb_entry(parent, struct trace, rb_node);

		if (t->bit->time < __t->bit->time)
			p = &(*p)->rb_left;
		else if (t->bit->time > __t->bit->time)
			p = &(*p)->rb_right;
		else if (t->bit->device < __t->bit->device)
			p = &(*p)->rb_left;
		else if (t->bit->device > __t->bit->device)
			p = &(*p)->rb_right;
		else if (t->bit->sequence < __t->bit->sequence)
			p = &(*p)->rb_left;
		else	/* >= sequence */
			p = &(*p)->rb_right;
	}

	rb_link_node(&t->rb_node, parent, p);
	rb_insert_color(&t->rb_node, root);
	return 0;
}

static inline int trace_rb_insert_sort(struct trace *t)
{
	if (!trace_rb_insert(t, &rb_sort_root)) {
		rb_sort_entries++;
		return 0;
	}

	return 1;
}

static int trace_rb_insert_last(struct per_dev_info *pdi, struct trace *t)
{
	struct per_cpu_info *pci = get_cpu_info(pdi, t->bit->cpu);

	if (trace_rb_insert(t, &pci->rb_last))
		return 1;

	pci->rb_last_entries++;

	if (pci->rb_last_entries > rb_batch * pdi->nfiles) {
		struct rb_node *n = rb_first(&pci->rb_last);

		t = rb_entry(n, struct trace, rb_node);
		__put_trace_last(pdi, t);
	}

	return 0;
}

static struct trace *trace_rb_find(dev_t device, unsigned long sequence,
				   struct rb_root *root, int order)
{
	struct rb_node *n = root->rb_node;
	struct rb_node *prev = NULL;
	struct trace *__t;

	while (n) {
		__t = rb_entry(n, struct trace, rb_node);
		prev = n;

		if (device < __t->bit->device)
			n = n->rb_left;
		else if (device > __t->bit->device)
			n = n->rb_right;
		else if (sequence < __t->bit->sequence)
			n = n->rb_left;
		else if (sequence > __t->bit->sequence)
			n = n->rb_right;
		else
			return __t;
	}

	/*
	 * hack - the list may not be sequence ordered because some
	 * events don't have sequence and time matched. so we end up
	 * being a little off in the rb lookup here, because we don't
	 * know the time we are looking for. compensate by browsing
	 * a little ahead from the last entry to find the match
	 */
	if (order && prev) {
		int max = 5;

		while (((n = rb_next(prev)) != NULL) && max--) {
			__t = rb_entry(n, struct trace, rb_node);

			if (__t->bit->device == device &&
			    __t->bit->sequence == sequence)
				return __t;

			prev = n;
		}
	}

	return NULL;
}

static inline struct trace *trace_rb_find_last(struct per_dev_info *pdi,
					       struct per_cpu_info *pci,
					       unsigned long seq)
{
	return trace_rb_find(pdi->dev, seq, &pci->rb_last, 0);
}

static inline int track_rb_insert(struct per_dev_info *pdi,struct io_track *iot)
{
	struct rb_node **p = &pdi->rb_track.rb_node;
	struct rb_node *parent = NULL;
	struct io_track *__iot;

	while (*p) {
		parent = *p;
		__iot = rb_entry(parent, struct io_track, rb_node);

		if (iot->sector < __iot->sector)
			p = &(*p)->rb_left;
		else if (iot->sector > __iot->sector)
			p = &(*p)->rb_right;
		else {
			fprintf(stderr,
				"sector alias (%Lu) on device %d,%d!\n",
				(unsigned long long) iot->sector,
				MAJOR(pdi->dev), MINOR(pdi->dev));
			return 1;
		}
	}

	rb_link_node(&iot->rb_node, parent, p);
	rb_insert_color(&iot->rb_node, &pdi->rb_track);
	return 0;
}

static struct io_track *__find_track(struct per_dev_info *pdi, __u64 sector)
{
	struct rb_node *n = pdi->rb_track.rb_node;
	struct io_track *__iot;

	while (n) {
		__iot = rb_entry(n, struct io_track, rb_node);

		if (sector < __iot->sector)
			n = n->rb_left;
		else if (sector > __iot->sector)
			n = n->rb_right;
		else
			return __iot;
	}

	return NULL;
}

static struct io_track *find_track(struct per_dev_info *pdi, pid_t pid,
				   __u64 sector)
{
	struct io_track *iot;

	iot = __find_track(pdi, sector);
	if (!iot) {
		iot = malloc(sizeof(*iot));
		iot->ppm = find_ppm(pid);
		if (!iot->ppm)
			iot->ppm = add_ppm_hash(pid, "unknown");
		iot->sector = sector;
		track_rb_insert(pdi, iot);
	}

	return iot;
}

static void log_track_frontmerge(struct per_dev_info *pdi,
				 struct blk_io_trace *t)
{
	struct io_track *iot;

	if (!track_ios)
		return;

	iot = __find_track(pdi, t->sector + t_sec(t));
	if (!iot) {
		if (verbose)
			fprintf(stderr, "merge not found for (%d,%d): %llu\n",
				MAJOR(pdi->dev), MINOR(pdi->dev),
				(unsigned long long) t->sector + t_sec(t));
		return;
	}

	rb_erase(&iot->rb_node, &pdi->rb_track);
	iot->sector -= t_sec(t);
	track_rb_insert(pdi, iot);
}

static void log_track_getrq(struct per_dev_info *pdi, struct blk_io_trace *t)
{
	struct io_track *iot;

	if (!track_ios)
		return;

	iot = find_track(pdi, t->pid, t->sector);
	iot->allocation_time = t->time;
}

static inline int is_remapper(struct per_dev_info *pdi)
{
	int major = MAJOR(pdi->dev);

	return (major == 253 || major == 9);
}

/*
 * for md/dm setups, the interesting cycle is Q -> C. So track queueing
 * time here, as dispatch time
 */
static void log_track_queue(struct per_dev_info *pdi, struct blk_io_trace *t)
{
	struct io_track *iot;

	if (!track_ios)
		return;
	if (!is_remapper(pdi))
		return;

	iot = find_track(pdi, t->pid, t->sector);
	iot->dispatch_time = t->time;
}

/*
 * return time between rq allocation and insertion
 */
static unsigned long long log_track_insert(struct per_dev_info *pdi,
					   struct blk_io_trace *t)
{
	unsigned long long elapsed;
	struct io_track *iot;

	if (!track_ios)
		return -1;

	iot = find_track(pdi, t->pid, t->sector);
	iot->queue_time = t->time;

	if (!iot->allocation_time)
		return -1;

	elapsed = iot->queue_time - iot->allocation_time;

	if (per_process_stats) {
		struct per_process_info *ppi = find_ppi(iot->ppm->pid);
		int w = (t->action & BLK_TC_ACT(BLK_TC_WRITE)) != 0;

		if (ppi && elapsed > ppi->longest_allocation_wait[w])
			ppi->longest_allocation_wait[w] = elapsed;
	}

	return elapsed;
}

/*
 * return time between queue and issue
 */
static unsigned long long log_track_issue(struct per_dev_info *pdi,
					  struct blk_io_trace *t)
{
	unsigned long long elapsed;
	struct io_track *iot;

	if (!track_ios)
		return -1;
	if ((t->action & BLK_TC_ACT(BLK_TC_FS)) == 0)
		return -1;

	iot = __find_track(pdi, t->sector);
	if (!iot) {
		if (verbose)
			fprintf(stderr, "issue not found for (%d,%d): %llu\n",
				MAJOR(pdi->dev), MINOR(pdi->dev),
				(unsigned long long) t->sector);
		return -1;
	}

	iot->dispatch_time = t->time;
	elapsed = iot->dispatch_time - iot->queue_time;

	if (per_process_stats) {
		struct per_process_info *ppi = find_ppi(iot->ppm->pid);
		int w = (t->action & BLK_TC_ACT(BLK_TC_WRITE)) != 0;

		if (ppi && elapsed > ppi->longest_dispatch_wait[w])
			ppi->longest_dispatch_wait[w] = elapsed;
	}

	return elapsed;
}

/*
 * return time between dispatch and complete
 */
static unsigned long long log_track_complete(struct per_dev_info *pdi,
					     struct blk_io_trace *t)
{
	unsigned long long elapsed;
	struct io_track *iot;

	if (!track_ios)
		return -1;

	iot = __find_track(pdi, t->sector);
	if (!iot) {
		if (verbose)
			fprintf(stderr,"complete not found for (%d,%d): %llu\n",
				MAJOR(pdi->dev), MINOR(pdi->dev),
				(unsigned long long) t->sector);
		return -1;
	}

	iot->completion_time = t->time;
	elapsed = iot->completion_time - iot->dispatch_time;

	if (per_process_stats) {
		struct per_process_info *ppi = find_ppi(iot->ppm->pid);
		int w = (t->action & BLK_TC_ACT(BLK_TC_WRITE)) != 0;

		if (ppi && elapsed > ppi->longest_completion_wait[w])
			ppi->longest_completion_wait[w] = elapsed;
	}

	/*
	 * kill the trace, we don't need it after completion
	 */
	rb_erase(&iot->rb_node, &pdi->rb_track);
	free(iot);

	return elapsed;
}


static struct io_stats *find_process_io_stats(pid_t pid)
{
	struct per_process_info *ppi = find_ppi(pid);

	if (!ppi) {
		ppi = malloc(sizeof(*ppi));
		memset(ppi, 0, sizeof(*ppi));
		ppi->ppm = find_ppm(pid);
		if (!ppi->ppm)
			ppi->ppm = add_ppm_hash(pid, "unknown");
		add_ppi_to_hash(ppi);
		add_ppi_to_list(ppi);
	}

	return &ppi->io_stats;
}

static char *get_dev_name(struct per_dev_info *pdi, char *buffer, int size)
{
	if (pdi->name)
		snprintf(buffer, size, "%s", pdi->name);
	else
		snprintf(buffer, size, "%d,%d",MAJOR(pdi->dev),MINOR(pdi->dev));
	return buffer;
}

static void check_time(struct per_dev_info *pdi, struct blk_io_trace *bit)
{
	unsigned long long this = bit->time;
	unsigned long long last = pdi->last_reported_time;

	pdi->backwards = (this < last) ? 'B' : ' ';
	pdi->last_reported_time = this;
}

static inline void __account_m(struct io_stats *ios, struct blk_io_trace *t,
			       int rw)
{
	if (rw) {
		ios->mwrites++;
		ios->mwrite_kb += t_kb(t);
	} else {
		ios->mreads++;
		ios->mread_kb += t_kb(t);
	}
}

static inline void account_m(struct blk_io_trace *t, struct per_cpu_info *pci,
			     int rw)
{
	__account_m(&pci->io_stats, t, rw);

	if (per_process_stats) {
		struct io_stats *ios = find_process_io_stats(t->pid);

		__account_m(ios, t, rw);
	}
}

static inline void __account_pc_queue(struct io_stats *ios,
				      struct blk_io_trace *t, int rw)
{
	if (rw) {
		ios->qwrites_pc++;
		ios->qwrite_kb_pc += t_kb(t);
	} else {
		ios->qreads_pc++;
		ios->qread_kb += t_kb(t);
	}
}

static inline void account_pc_queue(struct blk_io_trace *t,
				    struct per_cpu_info *pci, int rw)
{
	__account_pc_queue(&pci->io_stats, t, rw);

	if (per_process_stats) {
		struct io_stats *ios = find_process_io_stats(t->pid);

		__account_pc_queue(ios, t, rw);
	}
}

static inline void __account_pc_issue(struct io_stats *ios, int rw,
				      unsigned int bytes)
{
	if (rw) {
		ios->iwrites_pc++;
		ios->iwrite_kb_pc += bytes >> 10;
	} else {
		ios->ireads_pc++;
		ios->iread_kb_pc += bytes >> 10;
	}
}

static inline void account_pc_issue(struct blk_io_trace *t,
				    struct per_cpu_info *pci, int rw)
{
	__account_pc_issue(&pci->io_stats, rw, t->bytes);

	if (per_process_stats) {
		struct io_stats *ios = find_process_io_stats(t->pid);

		__account_pc_issue(ios, rw, t->bytes);
	}
}

static inline void __account_pc_requeue(struct io_stats *ios,
					struct blk_io_trace *t, int rw)
{
	if (rw) {
		ios->wrqueue_pc++;
		ios->iwrite_kb_pc -= t_kb(t);
	} else {
		ios->rrqueue_pc++;
		ios->iread_kb_pc -= t_kb(t);
	}
}

static inline void account_pc_requeue(struct blk_io_trace *t,
				      struct per_cpu_info *pci, int rw)
{
	__account_pc_requeue(&pci->io_stats, t, rw);

	if (per_process_stats) {
		struct io_stats *ios = find_process_io_stats(t->pid);

		__account_pc_requeue(ios, t, rw);
	}
}

static inline void __account_pc_c(struct io_stats *ios, int rw)
{
	if (rw)
		ios->cwrites_pc++;
	else
		ios->creads_pc++;
}

static inline void account_pc_c(struct blk_io_trace *t,
				struct per_cpu_info *pci, int rw)
{
	__account_pc_c(&pci->io_stats, rw);

	if (per_process_stats) {
		struct io_stats *ios = find_process_io_stats(t->pid);

		__account_pc_c(ios, rw);
	}
}

static inline void __account_queue(struct io_stats *ios, struct blk_io_trace *t,
				   int rw)
{
	if (rw) {
		ios->qwrites++;
		ios->qwrite_kb += t_kb(t);
	} else {
		ios->qreads++;
		ios->qread_kb += t_kb(t);
	}
}

static inline void account_queue(struct blk_io_trace *t,
				 struct per_cpu_info *pci, int rw)
{
	__account_queue(&pci->io_stats, t, rw);

	if (per_process_stats) {
		struct io_stats *ios = find_process_io_stats(t->pid);

		__account_queue(ios, t, rw);
	}
}

static inline void __account_c(struct io_stats *ios, int rw, int bytes)
{
	if (rw) {
		ios->cwrites++;
		ios->cwrite_kb += bytes >> 10;
	} else {
		ios->creads++;
		ios->cread_kb += bytes >> 10;
	}
}

static inline void account_c(struct blk_io_trace *t, struct per_cpu_info *pci,
			     int rw, int bytes)
{
	__account_c(&pci->io_stats, rw, bytes);

	if (per_process_stats) {
		struct io_stats *ios = find_process_io_stats(t->pid);

		__account_c(ios, rw, bytes);
	}
}

static inline void __account_issue(struct io_stats *ios, int rw,
				   unsigned int bytes)
{
	if (rw) {
		ios->iwrites++;
		ios->iwrite_kb += bytes >> 10;
	} else {
		ios->ireads++;
		ios->iread_kb += bytes >> 10;
	}
}

static inline void account_issue(struct blk_io_trace *t,
				 struct per_cpu_info *pci, int rw)
{
	__account_issue(&pci->io_stats, rw, t->bytes);

	if (per_process_stats) {
		struct io_stats *ios = find_process_io_stats(t->pid);

		__account_issue(ios, rw, t->bytes);
	}
}

static inline void __account_unplug(struct io_stats *ios, int timer)
{
	if (timer)
		ios->timer_unplugs++;
	else
		ios->io_unplugs++;
}

static inline void account_unplug(struct blk_io_trace *t,
				  struct per_cpu_info *pci, int timer)
{
	__account_unplug(&pci->io_stats, timer);

	if (per_process_stats) {
		struct io_stats *ios = find_process_io_stats(t->pid);

		__account_unplug(ios, timer);
	}
}

static inline void __account_requeue(struct io_stats *ios,
				     struct blk_io_trace *t, int rw)
{
	if (rw) {
		ios->wrqueue++;
		ios->iwrite_kb -= t_kb(t);
	} else {
		ios->rrqueue++;
		ios->iread_kb -= t_kb(t);
	}
}

static inline void account_requeue(struct blk_io_trace *t,
				   struct per_cpu_info *pci, int rw)
{
	__account_requeue(&pci->io_stats, t, rw);

	if (per_process_stats) {
		struct io_stats *ios = find_process_io_stats(t->pid);

		__account_requeue(ios, t, rw);
	}
}

static void log_complete(struct per_dev_info *pdi, struct per_cpu_info *pci,
			 struct blk_io_trace *t, char *act)
{
	process_fmt(act, pci, t, log_track_complete(pdi, t), 0, NULL);
}

static void log_insert(struct per_dev_info *pdi, struct per_cpu_info *pci,
		       struct blk_io_trace *t, char *act)
{
	process_fmt(act, pci, t, log_track_insert(pdi, t), 0, NULL);
}

static void log_queue(struct per_cpu_info *pci, struct blk_io_trace *t,
		      char *act)
{
	process_fmt(act, pci, t, -1, 0, NULL);
}

static void log_issue(struct per_dev_info *pdi, struct per_cpu_info *pci,
		      struct blk_io_trace *t, char *act)
{
	process_fmt(act, pci, t, log_track_issue(pdi, t), 0, NULL);
}

static void log_merge(struct per_dev_info *pdi, struct per_cpu_info *pci,
		      struct blk_io_trace *t, char *act)
{
	if (act[0] == 'F')
		log_track_frontmerge(pdi, t);

	process_fmt(act, pci, t, -1ULL, 0, NULL);
}

static void log_action(struct per_cpu_info *pci, struct blk_io_trace *t,
			char *act)
{
	process_fmt(act, pci, t, -1ULL, 0, NULL);
}

static void log_generic(struct per_cpu_info *pci, struct blk_io_trace *t,
			char *act)
{
	process_fmt(act, pci, t, -1ULL, 0, NULL);
}

static void log_unplug(struct per_cpu_info *pci, struct blk_io_trace *t,
		      char *act)
{
	process_fmt(act, pci, t, -1ULL, 0, NULL);
}

static void log_split(struct per_cpu_info *pci, struct blk_io_trace *t,
		      char *act)
{
	process_fmt(act, pci, t, -1ULL, 0, NULL);
}

static void log_pc(struct per_cpu_info *pci, struct blk_io_trace *t, char *act)
{
	unsigned char *buf = (unsigned char *) t + sizeof(*t);

	process_fmt(act, pci, t, -1ULL, t->pdu_len, buf);
}

static void dump_trace_pc(struct blk_io_trace *t, struct per_dev_info *pdi,
			  struct per_cpu_info *pci)
{
	int w = (t->action & BLK_TC_ACT(BLK_TC_WRITE)) != 0;
	int act = t->action & 0xffff;

	switch (act) {
		case __BLK_TA_QUEUE:
			log_generic(pci, t, "Q");
			account_pc_queue(t, pci, w);
			break;
		case __BLK_TA_GETRQ:
			log_generic(pci, t, "G");
			break;
		case __BLK_TA_SLEEPRQ:
			log_generic(pci, t, "S");
			break;
		case __BLK_TA_REQUEUE:
			/*
			 * can happen if we miss traces, don't let it go
			 * below zero
			 */
			if (pdi->cur_depth[w])
				pdi->cur_depth[w]--;
			account_pc_requeue(t, pci, w);
			log_generic(pci, t, "R");
			break;
		case __BLK_TA_ISSUE:
			account_pc_issue(t, pci, w);
			pdi->cur_depth[w]++;
			if (pdi->cur_depth[w] > pdi->max_depth[w])
				pdi->max_depth[w] = pdi->cur_depth[w];
			log_pc(pci, t, "D");
			break;
		case __BLK_TA_COMPLETE:
			if (pdi->cur_depth[w])
				pdi->cur_depth[w]--;
			log_pc(pci, t, "C");
			account_pc_c(t, pci, w);
			break;
		case __BLK_TA_INSERT:
			log_pc(pci, t, "I");
			break;
		default:
			fprintf(stderr, "Bad pc action %x\n", act);
			break;
	}
}

static void dump_trace_fs(struct blk_io_trace *t, struct per_dev_info *pdi,
			  struct per_cpu_info *pci)
{
	int w = (t->action & BLK_TC_ACT(BLK_TC_WRITE)) != 0;
	int act = t->action & 0xffff;

	switch (act) {
		case __BLK_TA_QUEUE:
			log_track_queue(pdi, t);
			account_queue(t, pci, w);
			log_queue(pci, t, "Q");
			break;
		case __BLK_TA_INSERT:
			log_insert(pdi, pci, t, "I");
			break;
		case __BLK_TA_BACKMERGE:
			account_m(t, pci, w);
			log_merge(pdi, pci, t, "M");
			break;
		case __BLK_TA_FRONTMERGE:
			account_m(t, pci, w);
			log_merge(pdi, pci, t, "F");
			break;
		case __BLK_TA_GETRQ:
			log_track_getrq(pdi, t);
			log_generic(pci, t, "G");
			break;
		case __BLK_TA_SLEEPRQ:
			log_generic(pci, t, "S");
			break;
		case __BLK_TA_REQUEUE:
			/*
			 * can happen if we miss traces, don't let it go
			 * below zero
			 */
			if (pdi->cur_depth[w])
				pdi->cur_depth[w]--;
			account_requeue(t, pci, w);
			log_queue(pci, t, "R");
			break;
		case __BLK_TA_ISSUE:
			account_issue(t, pci, w);
			pdi->cur_depth[w]++;
			if (pdi->cur_depth[w] > pdi->max_depth[w])
				pdi->max_depth[w] = pdi->cur_depth[w];
			log_issue(pdi, pci, t, "D");
			break;
		case __BLK_TA_COMPLETE:
			if (pdi->cur_depth[w])
				pdi->cur_depth[w]--;
			account_c(t, pci, w, t->bytes);
			log_complete(pdi, pci, t, "C");
			break;
		case __BLK_TA_PLUG:
			log_action(pci, t, "P");
			break;
		case __BLK_TA_UNPLUG_IO:
			account_unplug(t, pci, 0);
			log_unplug(pci, t, "U");
			break;
		case __BLK_TA_UNPLUG_TIMER:
			account_unplug(t, pci, 1);
			log_unplug(pci, t, "UT");
			break;
		case __BLK_TA_SPLIT:
			log_split(pci, t, "X");
			break;
		case __BLK_TA_BOUNCE:
			log_generic(pci, t, "B");
			break;
		case __BLK_TA_REMAP:
			log_generic(pci, t, "A");
			break;
		case __BLK_TA_DRV_DATA:
			have_drv_data = 1;
			/* dump to binary file only */
			break;
		default:
			fprintf(stderr, "Bad fs action %x\n", t->action);
			break;
	}
}

static void dump_trace(struct blk_io_trace *t, struct per_cpu_info *pci,
		       struct per_dev_info *pdi)
{
	if (text_output) {
		if (t->action == BLK_TN_MESSAGE)
			handle_notify(t);
		else if (t->action & BLK_TC_ACT(BLK_TC_PC))
			dump_trace_pc(t, pdi, pci);
		else
			dump_trace_fs(t, pdi, pci);
	}

	if (!pdi->events)
		pdi->first_reported_time = t->time;

	pdi->events++;

	if (bin_output_msgs ||
			    !(t->action & BLK_TC_ACT(BLK_TC_NOTIFY) &&
			      t->action == BLK_TN_MESSAGE))
		output_binary(t, sizeof(*t) + t->pdu_len);
}

/*
 * print in a proper way, not too small and not too big. if more than
 * 1000,000K, turn into M and so on
 */
static char *size_cnv(char *dst, unsigned long long num, int in_kb)
{
	char suff[] = { '\0', 'K', 'M', 'G', 'P' };
	unsigned int i = 0;

	if (in_kb)
		i++;

	while (num > 1000 * 1000ULL && (i < sizeof(suff) - 1)) {
		i++;
		num /= 1000;
	}

	sprintf(dst, "%'8Lu%c", num, suff[i]);
	return dst;
}

static void dump_io_stats(struct per_dev_info *pdi, struct io_stats *ios,
			  char *msg)
{
	static char x[256], y[256];

	fprintf(ofp, "%s\n", msg);

	fprintf(ofp, " Reads Queued:    %s, %siB\t", size_cnv(x, ios->qreads, 0), size_cnv(y, ios->qread_kb, 1));
	fprintf(ofp, " Writes Queued:    %s, %siB\n", size_cnv(x, ios->qwrites, 0), size_cnv(y, ios->qwrite_kb, 1));
	fprintf(ofp, " Read Dispatches: %s, %siB\t", size_cnv(x, ios->ireads, 0), size_cnv(y, ios->iread_kb, 1));
	fprintf(ofp, " Write Dispatches: %s, %siB\n", size_cnv(x, ios->iwrites, 0), size_cnv(y, ios->iwrite_kb, 1));
	fprintf(ofp, " Reads Requeued:  %s\t\t", size_cnv(x, ios->rrqueue, 0));
	fprintf(ofp, " Writes Requeued:  %s\n", size_cnv(x, ios->wrqueue, 0));
	fprintf(ofp, " Reads Completed: %s, %siB\t", size_cnv(x, ios->creads, 0), size_cnv(y, ios->cread_kb, 1));
	fprintf(ofp, " Writes Completed: %s, %siB\n", size_cnv(x, ios->cwrites, 0), size_cnv(y, ios->cwrite_kb, 1));
	fprintf(ofp, " Read Merges:     %s, %siB\t", size_cnv(x, ios->mreads, 0), size_cnv(y, ios->mread_kb, 1));
	fprintf(ofp, " Write Merges:     %s, %siB\n", size_cnv(x, ios->mwrites, 0), size_cnv(y, ios->mwrite_kb, 1));
	if (pdi) {
		fprintf(ofp, " Read depth:      %'8u%8c\t", pdi->max_depth[0], ' ');
		fprintf(ofp, " Write depth:      %'8u\n", pdi->max_depth[1]);
	}
	if (ios->qreads_pc || ios->qwrites_pc || ios->ireads_pc || ios->iwrites_pc ||
	    ios->rrqueue_pc || ios->wrqueue_pc || ios->creads_pc || ios->cwrites_pc) {
		fprintf(ofp, " PC Reads Queued: %s, %siB\t", size_cnv(x, ios->qreads_pc, 0), size_cnv(y, ios->qread_kb_pc, 1));
		fprintf(ofp, " PC Writes Queued: %s, %siB\n", size_cnv(x, ios->qwrites_pc, 0), size_cnv(y, ios->qwrite_kb_pc, 1));
		fprintf(ofp, " PC Read Disp.:   %s, %siB\t", size_cnv(x, ios->ireads_pc, 0), size_cnv(y, ios->iread_kb_pc, 1));
		fprintf(ofp, " PC Write Disp.:   %s, %siB\n", size_cnv(x, ios->iwrites_pc, 0), size_cnv(y, ios->iwrite_kb_pc, 1));
		fprintf(ofp, " PC Reads Req.:   %s\t\t", size_cnv(x, ios->rrqueue_pc, 0));
		fprintf(ofp, " PC Writes Req.:   %s\n", size_cnv(x, ios->wrqueue_pc, 0));
		fprintf(ofp, " PC Reads Compl.: %s\t\t", size_cnv(x, ios->creads_pc, 0));
		fprintf(ofp, " PC Writes Compl.: %s\n", size_cnv(x, ios->cwrites, 0));
	}
	fprintf(ofp, " IO unplugs:      %'8lu%8c\t", ios->io_unplugs, ' ');
	fprintf(ofp, " Timer unplugs:    %'8lu\n", ios->timer_unplugs);
}

static void dump_wait_stats(struct per_process_info *ppi)
{
	unsigned long rawait = ppi->longest_allocation_wait[0] / 1000;
	unsigned long rdwait = ppi->longest_dispatch_wait[0] / 1000;
	unsigned long rcwait = ppi->longest_completion_wait[0] / 1000;
	unsigned long wawait = ppi->longest_allocation_wait[1] / 1000;
	unsigned long wdwait = ppi->longest_dispatch_wait[1] / 1000;
	unsigned long wcwait = ppi->longest_completion_wait[1] / 1000;

	fprintf(ofp, " Allocation wait: %'8lu%8c\t", rawait, ' ');
	fprintf(ofp, " Allocation wait:  %'8lu\n", wawait);
	fprintf(ofp, " Dispatch wait:   %'8lu%8c\t", rdwait, ' ');
	fprintf(ofp, " Dispatch wait:    %'8lu\n", wdwait);
	fprintf(ofp, " Completion wait: %'8lu%8c\t", rcwait, ' ');
	fprintf(ofp, " Completion wait:  %'8lu\n", wcwait);
}

static int ppi_name_compare(const void *p1, const void *p2)
{
	struct per_process_info *ppi1 = *((struct per_process_info **) p1);
	struct per_process_info *ppi2 = *((struct per_process_info **) p2);
	int res;

	res = strverscmp(ppi1->ppm->comm, ppi2->ppm->comm);
	if (!res)
		res = ppi1->ppm->pid > ppi2->ppm->pid;

	return res;
}

static void sort_process_list(void)
{
	struct per_process_info **ppis;
	struct per_process_info *ppi;
	int i = 0;

	ppis = malloc(ppi_list_entries * sizeof(struct per_process_info *));

	ppi = ppi_list;
	while (ppi) {
		ppis[i++] = ppi;
		ppi = ppi->list_next;
	}

	qsort(ppis, ppi_list_entries, sizeof(ppi), ppi_name_compare);

	i = ppi_list_entries - 1;
	ppi_list = NULL;
	while (i >= 0) {
		ppi = ppis[i];

		ppi->list_next = ppi_list;
		ppi_list = ppi;
		i--;
	}

	free(ppis);
}

static void show_process_stats(void)
{
	struct per_process_info *ppi;

	sort_process_list();

	ppi = ppi_list;
	while (ppi) {
		struct process_pid_map *ppm = ppi->ppm;
		char name[64];

		if (ppi->more_than_one)
			sprintf(name, "%s (%u, ...)", ppm->comm, ppm->pid);
		else
			sprintf(name, "%s (%u)", ppm->comm, ppm->pid);

		dump_io_stats(NULL, &ppi->io_stats, name);
		dump_wait_stats(ppi);
		ppi = ppi->list_next;
	}

	fprintf(ofp, "\n");
}

static void show_device_and_cpu_stats(void)
{
	struct per_dev_info *pdi;
	struct per_cpu_info *pci;
	struct io_stats total, *ios;
	unsigned long long rrate, wrate, msec;
	int i, j, pci_events;
	char line[3 + 8/*cpu*/ + 2 + 32/*dev*/ + 3];
	char name[32];
	double ratio;

	for (pdi = devices, i = 0; i < ndevices; i++, pdi++) {

		memset(&total, 0, sizeof(total));
		pci_events = 0;

		if (i > 0)
			fprintf(ofp, "\n");

		for (pci = pdi->cpus, j = 0; j < pdi->ncpus; j++, pci++) {
			if (!pci->nelems)
				continue;

			ios = &pci->io_stats;
			total.qreads += ios->qreads;
			total.qwrites += ios->qwrites;
			total.creads += ios->creads;
			total.cwrites += ios->cwrites;
			total.mreads += ios->mreads;
			total.mwrites += ios->mwrites;
			total.ireads += ios->ireads;
			total.iwrites += ios->iwrites;
			total.rrqueue += ios->rrqueue;
			total.wrqueue += ios->wrqueue;
			total.qread_kb += ios->qread_kb;
			total.qwrite_kb += ios->qwrite_kb;
			total.cread_kb += ios->cread_kb;
			total.cwrite_kb += ios->cwrite_kb;
			total.iread_kb += ios->iread_kb;
			total.iwrite_kb += ios->iwrite_kb;
			total.mread_kb += ios->mread_kb;
			total.mwrite_kb += ios->mwrite_kb;

			total.qreads_pc += ios->qreads_pc;
			total.qwrites_pc += ios->qwrites_pc;
			total.creads_pc += ios->creads_pc;
			total.cwrites_pc += ios->cwrites_pc;
			total.ireads_pc += ios->ireads_pc;
			total.iwrites_pc += ios->iwrites_pc;
			total.rrqueue_pc += ios->rrqueue_pc;
			total.wrqueue_pc += ios->wrqueue_pc;
			total.qread_kb_pc += ios->qread_kb_pc;
			total.qwrite_kb_pc += ios->qwrite_kb_pc;
			total.iread_kb_pc += ios->iread_kb_pc;
			total.iwrite_kb_pc += ios->iwrite_kb_pc;

			total.timer_unplugs += ios->timer_unplugs;
			total.io_unplugs += ios->io_unplugs;

			snprintf(line, sizeof(line) - 1, "CPU%d (%s):",
				 j, get_dev_name(pdi, name, sizeof(name)));
			dump_io_stats(pdi, ios, line);
			pci_events++;
		}

		if (pci_events > 1) {
			fprintf(ofp, "\n");
			snprintf(line, sizeof(line) - 1, "Total (%s):",
				 get_dev_name(pdi, name, sizeof(name)));
			dump_io_stats(NULL, &total, line);
		}

		wrate = rrate = 0;
		msec = (pdi->last_reported_time - pdi->first_reported_time) / 1000000;
		if (msec) {
			rrate = 1000 * total.cread_kb / msec;
			wrate = 1000 * total.cwrite_kb / msec;
		}

		fprintf(ofp, "\nThroughput (R/W): %'LuKiB/s / %'LuKiB/s\n",
			rrate, wrate);
		fprintf(ofp, "Events (%s): %'Lu entries\n",
			get_dev_name(pdi, line, sizeof(line)), pdi->events);

		collect_pdi_skips(pdi);
		if (!pdi->skips && !pdi->events)
			ratio = 0.0;
		else
			ratio = 100.0 * ((double)pdi->seq_skips /
					(double)(pdi->events + pdi->seq_skips));
		fprintf(ofp, "Skips: %'lu forward (%'llu - %5.1lf%%)\n",
			pdi->skips, pdi->seq_skips, ratio);
	}
}

static void find_genesis(void)
{
	struct trace *t = trace_list;

	genesis_time = -1ULL;
	while (t != NULL) {
		if (t->bit->time < genesis_time)
			genesis_time = t->bit->time;

		t = t->next;
	}

	/* The time stamp record will usually be the first
	 * record in the trace, but not always.
	 */
	if (start_timestamp
	 && start_timestamp != genesis_time) {
		long delta = genesis_time - start_timestamp;

		abs_start_time.tv_sec  += SECONDS(delta);
		abs_start_time.tv_nsec += NANO_SECONDS(delta);
		if (abs_start_time.tv_nsec < 0) {
			abs_start_time.tv_nsec += 1000000000;
			abs_start_time.tv_sec -= 1;
		} else
		if (abs_start_time.tv_nsec > 1000000000) {
			abs_start_time.tv_nsec -= 1000000000;
			abs_start_time.tv_sec += 1;
		}
	}
}

static inline int check_stopwatch(struct blk_io_trace *bit)
{
	if (bit->time < stopwatch_end &&
	    bit->time >= stopwatch_start)
		return 0;

	return 1;
}

/*
 * return youngest entry read
 */
static int sort_entries(unsigned long long *youngest)
{
	struct per_dev_info *pdi = NULL;
	struct per_cpu_info *pci = NULL;
	struct trace *t;

	if (!genesis_time)
		find_genesis();

	*youngest = 0;
	while ((t = trace_list) != NULL) {
		struct blk_io_trace *bit = t->bit;

		trace_list = t->next;

		bit->time -= genesis_time;

		if (bit->time < *youngest || !*youngest)
			*youngest = bit->time;

		if (!pdi || pdi->dev != bit->device) {
			pdi = get_dev_info(bit->device);
			pci = NULL;
		}

		if (!pci || pci->cpu != bit->cpu)
			pci = get_cpu_info(pdi, bit->cpu);

		if (bit->sequence < pci->smallest_seq_read)
			pci->smallest_seq_read = bit->sequence;

		if (check_stopwatch(bit)) {
			bit_free(bit);
			t_free(t);
			continue;
		}

		if (trace_rb_insert_sort(t))
			return -1;
	}

	return 0;
}

/*
 * to continue, we must have traces from all online cpus in the tree
 */
static int check_cpu_map(struct per_dev_info *pdi)
{
	unsigned long *cpu_map;
	struct rb_node *n;
	struct trace *__t;
	unsigned int i;
	int ret, cpu;

	/*
	 * create a map of the cpus we have traces for
	 */
	cpu_map = malloc(pdi->cpu_map_max / sizeof(long));
	n = rb_first(&rb_sort_root);
	while (n) {
		__t = rb_entry(n, struct trace, rb_node);
		cpu = __t->bit->cpu;

		cpu_map[CPU_IDX(cpu)] |= (1UL << CPU_BIT(cpu));
		n = rb_next(n);
	}

	/*
	 * we can't continue if pdi->cpu_map has entries set that we don't
	 * have in the sort rbtree. the opposite is not a problem, though
	 */
	ret = 0;
	for (i = 0; i < pdi->cpu_map_max / CPUS_PER_LONG; i++) {
		if (pdi->cpu_map[i] & ~(cpu_map[i])) {
			ret = 1;
			break;
		}
	}

	free(cpu_map);
	return ret;
}

static int check_sequence(struct per_dev_info *pdi, struct trace *t, int force)
{
	struct blk_io_trace *bit = t->bit;
	unsigned long expected_sequence;
	struct per_cpu_info *pci;
	struct trace *__t;

	pci = get_cpu_info(pdi, bit->cpu);
	expected_sequence = pci->last_sequence + 1;

	if (!expected_sequence) {
		/*
		 * 1 should be the first entry, just allow it
		 */
		if (bit->sequence == 1)
			return 0;
		if (bit->sequence == pci->smallest_seq_read)
			return 0;

		return check_cpu_map(pdi);
	}

	if (bit->sequence == expected_sequence)
		return 0;

	/*
	 * we may not have seen that sequence yet. if we are not doing
	 * the final run, break and wait for more entries.
	 */
	if (expected_sequence < pci->smallest_seq_read) {
		__t = trace_rb_find_last(pdi, pci, expected_sequence);
		if (!__t)
			goto skip;

		__put_trace_last(pdi, __t);
		return 0;
	} else if (!force) {
		return 1;
	} else {
skip:
		if (check_current_skips(pci, bit->sequence))
			return 0;

		if (expected_sequence < bit->sequence)
			insert_skip(pci, expected_sequence, bit->sequence - 1);
		return 0;
	}
}

static void show_entries_rb(int force)
{
	struct per_dev_info *pdi = NULL;
	struct per_cpu_info *pci = NULL;
	struct blk_io_trace *bit;
	struct rb_node *n;
	struct trace *t;

	while ((n = rb_first(&rb_sort_root)) != NULL) {
		if (is_done() && !force && !pipeline)
			break;

		t = rb_entry(n, struct trace, rb_node);
		bit = t->bit;

		if (read_sequence - t->read_sequence < 1 && !force)
			break;

		if (!pdi || pdi->dev != bit->device) {
			pdi = get_dev_info(bit->device);
			pci = NULL;
		}

		if (!pdi) {
			fprintf(stderr, "Unknown device ID? (%d,%d)\n",
				MAJOR(bit->device), MINOR(bit->device));
			break;
		}

		if (check_sequence(pdi, t, force))
			break;

		if (!force && bit->time > last_allowed_time)
			break;

		check_time(pdi, bit);

		if (!pci || pci->cpu != bit->cpu)
			pci = get_cpu_info(pdi, bit->cpu);

		pci->last_sequence = bit->sequence;

		pci->nelems++;

		if (bit->action & (act_mask << BLK_TC_SHIFT))
			dump_trace(bit, pci, pdi);

		put_trace(pdi, t);
	}
}

static int read_data(int fd, void *buffer, int bytes, int block, int *fdblock)
{
	int ret, bytes_left, fl;
	void *p;

	if (block != *fdblock) {
		fl = fcntl(fd, F_GETFL);

		if (!block) {
			*fdblock = 0;
			fcntl(fd, F_SETFL, fl | O_NONBLOCK);
		} else {
			*fdblock = 1;
			fcntl(fd, F_SETFL, fl & ~O_NONBLOCK);
		}
	}

	bytes_left = bytes;
	p = buffer;
	while (bytes_left > 0) {
		ret = read(fd, p, bytes_left);
		if (!ret)
			return 1;
		else if (ret < 0) {
			if (errno != EAGAIN) {
				perror("read");
				return -1;
			}

			/*
			 * never do partial reads. we can return if we
			 * didn't read anything and we should not block,
			 * otherwise wait for data
			 */
			if ((bytes_left == bytes) && !block)
				return 1;

			usleep(10);
			continue;
		} else {
			p += ret;
			bytes_left -= ret;
		}
	}

	return 0;
}

static inline __u16 get_pdulen(struct blk_io_trace *bit)
{
	if (data_is_native)
		return bit->pdu_len;

	return __bswap_16(bit->pdu_len);
}

static inline __u32 get_magic(struct blk_io_trace *bit)
{
	if (data_is_native)
		return bit->magic;

	return __bswap_32(bit->magic);
}

static int read_events(int fd, int always_block, int *fdblock)
{
	struct per_dev_info *pdi = NULL;
	unsigned int events = 0;

	while (!is_done() && events < rb_batch) {
		struct blk_io_trace *bit;
		struct trace *t;
		int pdu_len, should_block, ret;
		__u32 magic;

		bit = bit_alloc();

		should_block = !events || always_block;

		ret = read_data(fd, bit, sizeof(*bit), should_block, fdblock);
		if (ret) {
			bit_free(bit);
			if (!events && ret < 0)
				events = ret;
			break;
		}

		/*
		 * look at first trace to check whether we need to convert
		 * data in the future
		 */
		if (data_is_native == -1 && check_data_endianness(bit->magic))
			break;

		magic = get_magic(bit);
		if ((magic & 0xffffff00) != BLK_IO_TRACE_MAGIC) {
			fprintf(stderr, "Bad magic %x\n", magic);
			break;
		}

		pdu_len = get_pdulen(bit);
		if (pdu_len) {
			void *ptr = realloc(bit, sizeof(*bit) + pdu_len);

			if (read_data(fd, ptr + sizeof(*bit), pdu_len, 1, fdblock)) {
				bit_free(ptr);
				break;
			}

			bit = ptr;
		}

		trace_to_cpu(bit);

		if (verify_trace(bit)) {
			bit_free(bit);
			continue;
		}

		/*
		 * not a real trace, so grab and handle it here
		 */
		if (bit->action & BLK_TC_ACT(BLK_TC_NOTIFY) && bit->action != BLK_TN_MESSAGE) {
			handle_notify(bit);
			output_binary(bit, sizeof(*bit) + bit->pdu_len);
			continue;
		}

		t = t_alloc();
		memset(t, 0, sizeof(*t));
		t->bit = bit;
		t->read_sequence = read_sequence;

		t->next = trace_list;
		trace_list = t;

		if (!pdi || pdi->dev != bit->device)
			pdi = get_dev_info(bit->device);

		if (bit->time > pdi->last_read_time)
			pdi->last_read_time = bit->time;

		events++;
	}

	return events;
}

/*
 * Managing input streams
 */

struct ms_stream {
	struct ms_stream *next;
	struct trace *first, *last;
	struct per_dev_info *pdi;
	unsigned int cpu;
};

#define MS_HASH(d, c) ((MAJOR(d) & 0xff) ^ (MINOR(d) & 0xff) ^ (cpu & 0xff))

struct ms_stream *ms_head;
struct ms_stream *ms_hash[256];

static void ms_sort(struct ms_stream *msp);
static int ms_prime(struct ms_stream *msp);

static inline struct trace *ms_peek(struct ms_stream *msp)
{
	return (msp == NULL) ? NULL : msp->first;
}

static inline __u64 ms_peek_time(struct ms_stream *msp)
{
	return ms_peek(msp)->bit->time;
}

static inline void ms_resort(struct ms_stream *msp)
{
	if (msp->next && ms_peek_time(msp) > ms_peek_time(msp->next)) {
		ms_head = msp->next;
		msp->next = NULL;
		ms_sort(msp);
	}
}

static inline void ms_deq(struct ms_stream *msp)
{
	msp->first = msp->first->next;
	if (!msp->first) {
		msp->last = NULL;
		if (!ms_prime(msp)) {
			ms_head = msp->next;
			msp->next = NULL;
			return;
		}
	}

	ms_resort(msp);
}

static void ms_sort(struct ms_stream *msp)
{
	__u64 msp_t = ms_peek_time(msp);
	struct ms_stream *this_msp = ms_head;

	if (this_msp == NULL)
		ms_head = msp;
	else if (msp_t < ms_peek_time(this_msp)) {
		msp->next = this_msp;
		ms_head = msp;
	}
	else {
		while (this_msp->next && ms_peek_time(this_msp->next) < msp_t)
			this_msp = this_msp->next;

		msp->next = this_msp->next;
		this_msp->next = msp;
	}
}

static int ms_prime(struct ms_stream *msp)
{
	__u32 magic;
	unsigned int i;
	struct trace *t;
	struct per_dev_info *pdi = msp->pdi;
	struct per_cpu_info *pci = get_cpu_info(pdi, msp->cpu);
	struct blk_io_trace *bit = NULL;
	int ret, pdu_len, ndone = 0;

	for (i = 0; !is_done() && pci->fd >= 0 && i < rb_batch; i++) {
		bit = bit_alloc();
		ret = read_data(pci->fd, bit, sizeof(*bit), 1, &pci->fdblock);
		if (ret)
			goto err;

		if (data_is_native == -1 && check_data_endianness(bit->magic))
			goto err;

		magic = get_magic(bit);
		if ((magic & 0xffffff00) != BLK_IO_TRACE_MAGIC) {
			fprintf(stderr, "Bad magic %x\n", magic);
			goto err;

		}

		pdu_len = get_pdulen(bit);
		if (pdu_len) {
			void *ptr = realloc(bit, sizeof(*bit) + pdu_len);
			ret = read_data(pci->fd, ptr + sizeof(*bit), pdu_len,
							     1, &pci->fdblock);
			if (ret) {
				free(ptr);
				bit = NULL;
				goto err;
			}

			bit = ptr;
		}

		trace_to_cpu(bit);
		if (verify_trace(bit))
			goto err;

		if (bit->action & BLK_TC_ACT(BLK_TC_NOTIFY) && bit->action != BLK_TN_MESSAGE) {
			handle_notify(bit);
			output_binary(bit, sizeof(*bit) + bit->pdu_len);
			bit_free(bit);

			i -= 1;
			continue;
		}

		if (bit->time > pdi->last_read_time)
			pdi->last_read_time = bit->time;

		t = t_alloc();
		memset(t, 0, sizeof(*t));
		t->bit = bit;

		if (msp->first == NULL)
			msp->first = msp->last = t;
		else {
			msp->last->next = t;
			msp->last = t;
		}

		ndone++;
	}

	return ndone;

err:
	if (bit) bit_free(bit);

	cpu_mark_offline(pdi, pci->cpu);
	close(pci->fd);
	pci->fd = -1;

	return ndone;
}

static struct ms_stream *ms_alloc(struct per_dev_info *pdi, int cpu)
{
	struct ms_stream *msp = malloc(sizeof(*msp));

	msp->next = NULL;
	msp->first = msp->last = NULL;
	msp->pdi = pdi;
	msp->cpu = cpu;

	if (ms_prime(msp))
		ms_sort(msp);

	return msp;
}

static int setup_file(struct per_dev_info *pdi, int cpu)
{
	int len = 0;
	struct stat st;
	char *p, *dname;
	struct per_cpu_info *pci = get_cpu_info(pdi, cpu);

	pci->cpu = cpu;
	pci->fdblock = -1;

	p = strdup(pdi->name);
	dname = dirname(p);
	if (strcmp(dname, ".")) {
		input_dir = dname;
		p = strdup(pdi->name);
		strcpy(pdi->name, basename(p));
	}
	free(p);

	if (input_dir)
		len = sprintf(pci->fname, "%s/", input_dir);

	snprintf(pci->fname + len, sizeof(pci->fname)-1-len,
		 "%s.blktrace.%d", pdi->name, pci->cpu);
	if (stat(pci->fname, &st) < 0)
		return 0;
	if (!st.st_size)
		return 1;

	pci->fd = open(pci->fname, O_RDONLY);
	if (pci->fd < 0) {
		perror(pci->fname);
		return 0;
	}

	printf("Input file %s added\n", pci->fname);
	cpu_mark_online(pdi, pci->cpu);

	pdi->nfiles++;
	ms_alloc(pdi, pci->cpu);

	return 1;
}

static int handle(struct ms_stream *msp)
{
	struct trace *t;
	struct per_dev_info *pdi;
	struct per_cpu_info *pci;
	struct blk_io_trace *bit;

	t = ms_peek(msp);

	bit = t->bit;
	pdi = msp->pdi;
	pci = get_cpu_info(pdi, msp->cpu);
	pci->nelems++;
	bit->time -= genesis_time;

	if (t->bit->time > stopwatch_end)
		return 0;

	pdi->last_reported_time = bit->time;
	if ((bit->action & (act_mask << BLK_TC_SHIFT))&&
	    t->bit->time >= stopwatch_start)
		dump_trace(bit, pci, pdi);

	ms_deq(msp);

	if (text_output)
		trace_rb_insert_last(pdi, t);
	else {
		bit_free(t->bit);
		t_free(t);
	}

	return 1;
}

/*
 * Check if we need to sanitize the name. We allow 'foo', or if foo.blktrace.X
 * is given, then strip back down to 'foo' to avoid missing files.
 */
static int name_fixup(char *name)
{
	char *b;

	if (!name)
		return 1;

	b = strstr(name, ".blktrace.");
	if (b)
		*b = '\0';

	return 0;
}

static int do_file(void)
{
	int i, cpu, ret;
	struct per_dev_info *pdi;

	/*
	 * first prepare all files for reading
	 */
	for (i = 0; i < ndevices; i++) {
		pdi = &devices[i];
		ret = name_fixup(pdi->name);
		if (ret)
			return ret;

		for (cpu = 0; setup_file(pdi, cpu); cpu++)
			;
	}

	/*
	 * Get the initial time stamp
	 */
	if (ms_head)
		genesis_time = ms_peek_time(ms_head);

	/*
	 * Keep processing traces while any are left
	 */
	while (!is_done() && ms_head && handle(ms_head))
		;

	return 0;
}

static void do_pipe(int fd)
{
	unsigned long long youngest;
	int events, fdblock;

	last_allowed_time = -1ULL;
	fdblock = -1;
	while ((events = read_events(fd, 0, &fdblock)) > 0) {
		read_sequence++;
	
#if 0
		smallest_seq_read = -1U;
#endif

		if (sort_entries(&youngest))
			break;

		if (youngest > stopwatch_end)
			break;

		show_entries_rb(0);
	}

	if (rb_sort_entries)
		show_entries_rb(1);
}

static int do_fifo(void)
{
	int fd;

	if (!strcmp(pipename, "-"))
		fd = dup(STDIN_FILENO);
	else
		fd = open(pipename, O_RDONLY);

	if (fd == -1) {
		perror("dup stdin");
		return -1;
	}

	do_pipe(fd);
	close(fd);
	return 0;
}

static void show_stats(void)
{
	if (!ofp)
		return;
	if (stats_printed)
		return;

	stats_printed = 1;

	if (per_process_stats)
		show_process_stats();

	if (per_device_and_cpu_stats)
		show_device_and_cpu_stats();

	fflush(ofp);
}

static void handle_sigint(__attribute__((__unused__)) int sig)
{
	done = 1;
}

/*
 * Extract start and duration times from a string, allowing
 * us to specify a time interval of interest within a trace.
 * Format: "duration" (start is zero) or "start:duration".
 */
static int find_stopwatch_interval(char *string)
{
	double value;
	char *sp;

	value = strtod(string, &sp);
	if (sp == string) {
		fprintf(stderr,"Invalid stopwatch timer: %s\n", string);
		return 1;
	}
	if (*sp == ':') {
		stopwatch_start = DOUBLE_TO_NANO_ULL(value);
		string = sp + 1;
		value = strtod(string, &sp);
		if (sp == string || *sp != '\0') {
			fprintf(stderr,"Invalid stopwatch duration time: %s\n",
				string);
			return 1;
		}
	} else if (*sp != '\0') {
		fprintf(stderr,"Invalid stopwatch start timer: %s\n", string);
		return 1;
	}
	stopwatch_end = DOUBLE_TO_NANO_ULL(value);
	if (stopwatch_end <= stopwatch_start) {
		fprintf(stderr, "Invalid stopwatch interval: %Lu -> %Lu\n",
			stopwatch_start, stopwatch_end);
		return 1;
	}

	return 0;
}

static int is_pipe(const char *str)
{
	struct stat st;

	if (!strcmp(str, "-"))
		return 1;
	if (!stat(str, &st) && S_ISFIFO(st.st_mode))
		return 1;

	return 0;
}

#define S_OPTS  "a:A:b:D:d:f:F:hi:o:Oqstw:vVM"
static char usage_str[] =    "\n\n" \
	"-i <file>           | --input=<file>\n" \
	"[ -a <action field> | --act-mask=<action field> ]\n" \
	"[ -A <action mask>  | --set-mask=<action mask> ]\n" \
	"[ -b <traces>       | --batch=<traces> ]\n" \
	"[ -d <file>         | --dump-binary=<file> ]\n" \
	"[ -D <dir>          | --input-directory=<dir> ]\n" \
	"[ -f <format>       | --format=<format> ]\n" \
	"[ -F <spec>         | --format-spec=<spec> ]\n" \
	"[ -h                | --hash-by-name ]\n" \
	"[ -o <file>         | --output=<file> ]\n" \
	"[ -O                | --no-text-output ]\n" \
	"[ -q                | --quiet ]\n" \
	"[ -s                | --per-program-stats ]\n" \
	"[ -t                | --track-ios ]\n" \
	"[ -w <time>         | --stopwatch=<time> ]\n" \
	"[ -M                | --no-msgs\n" \
	"[ -v                | --verbose ]\n" \
	"[ -V                | --version ]\n\n" \
	"\t-b stdin read batching\n" \
	"\t-d Output file. If specified, binary data is written to file\n" \
	"\t-D Directory to prepend to input file names\n" \
	"\t-f Output format. Customize the output format. The format field\n" \
	"\t   identifies can be found in the documentation\n" \
	"\t-F Format specification. Can be found in the documentation\n" \
	"\t-h Hash processes by name, not pid\n" \
	"\t-i Input file containing trace data, or '-' for stdin\n" \
	"\t-o Output file. If not given, output is stdout\n" \
	"\t-O Do NOT output text data\n" \
	"\t-q Quiet. Don't display any stats at the end of the trace\n" \
	"\t-s Show per-program io statistics\n" \
	"\t-t Track individual ios. Will tell you the time a request took\n" \
	"\t   to get queued, to get dispatched, and to get completed\n" \
	"\t-w Only parse data between the given time interval in seconds.\n" \
	"\t   If 'start' isn't given, blkparse defaults the start time to 0\n" \
	"\t-M Do not output messages to binary file\n" \
	"\t-v More verbose for marginal errors\n" \
	"\t-V Print program version info\n\n";

static void usage(char *prog)
{
	fprintf(stderr, "Usage: %s %s %s", prog, blkparse_version, usage_str);
}

int main(int argc, char *argv[])
{
	int i, c, ret, mode;
	int act_mask_tmp = 0;
	char *ofp_buffer = NULL;
	char *bin_ofp_buffer = NULL;

	while ((c = getopt_long(argc, argv, S_OPTS, l_opts, NULL)) != -1) {
		switch (c) {
		case 'a':
			i = find_mask_map(optarg);
			if (i < 0) {
				fprintf(stderr,"Invalid action mask %s\n",
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
		case 'i':
			if (is_pipe(optarg) && !pipeline) {
				pipeline = 1;
				pipename = strdup(optarg);
			} else if (resize_devices(optarg) != 0)
				return 1;
			break;
		case 'D':
			input_dir = optarg;
			break;
		case 'o':
			output_name = optarg;
			break;
		case 'O':
			text_output = 0;
			break;
		case 'b':
			rb_batch = atoi(optarg);
			if (rb_batch <= 0)
				rb_batch = RB_BATCH_DEFAULT;
			break;
		case 's':
			per_process_stats = 1;
			break;
		case 't':
			track_ios = 1;
			break;
		case 'q':
			per_device_and_cpu_stats = 0;
			break;
		case 'w':
			if (find_stopwatch_interval(optarg) != 0)
				return 1;
			break;
		case 'f':
			set_all_format_specs(optarg);
			break;
		case 'F':
			if (add_format_spec(optarg) != 0)
				return 1;
			break;
		case 'h':
			ppi_hash_by_pid = 0;
			break;
		case 'v':
			verbose++;
			break;
		case 'V':
			printf("%s version %s\n", argv[0], blkparse_version);
			return 0;
		case 'd':
			dump_binary = optarg;
			break;
		case 'M':
			bin_output_msgs = 0;
			break;
		default:
			usage(argv[0]);
			return 1;
		}
	}

	while (optind < argc) {
		if (is_pipe(argv[optind]) && !pipeline) {
			pipeline = 1;
			pipename = strdup(argv[optind]);
		} else if (resize_devices(argv[optind]) != 0)
			return 1;
		optind++;
	}

	if (!pipeline && !ndevices) {
		usage(argv[0]);
		return 1;
	}

	if (act_mask_tmp != 0)
		act_mask = act_mask_tmp;

	memset(&rb_sort_root, 0, sizeof(rb_sort_root));

	signal(SIGINT, handle_sigint);
	signal(SIGHUP, handle_sigint);
	signal(SIGTERM, handle_sigint);

	setlocale(LC_NUMERIC, "en_US");

	if (text_output) {
		if (!output_name) {
			ofp = fdopen(STDOUT_FILENO, "w");
			mode = _IOLBF;
		} else {
			char ofname[128];

			snprintf(ofname, sizeof(ofname) - 1, "%s", output_name);
			ofp = fopen(ofname, "w");
			mode = _IOFBF;
		}

		if (!ofp) {
			perror("fopen");
			return 1;
		}

		ofp_buffer = malloc(4096);
		if (setvbuf(ofp, ofp_buffer, mode, 4096)) {
			perror("setvbuf");
			return 1;
		}
	}

	if (dump_binary) {
		dump_fp = fopen(dump_binary, "w");
		if (!dump_fp) {
			perror(dump_binary);
			dump_binary = NULL;
			return 1;
		}
		bin_ofp_buffer = malloc(128 * 1024);
		if (setvbuf(dump_fp, bin_ofp_buffer, _IOFBF, 128 * 1024)) {
			perror("setvbuf binary");
			return 1;
		}
	}

	if (pipeline)
		ret = do_fifo();
	else
		ret = do_file();

	if (!ret)
		show_stats();

	if (have_drv_data && !dump_binary)
		printf("\ndiscarded traces containing low-level device driver "
		       "specific data (only available in binary output)\n");

	if (ofp_buffer) {
		fflush(ofp);
		free(ofp_buffer);
	}
	if (bin_ofp_buffer) {
		fflush(dump_fp);
		free(bin_ofp_buffer);
	}
	return ret;
}
