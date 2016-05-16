#ifndef BLKTRACE_H
#define BLKTRACE_H

#include <stdio.h>
#include <byteswap.h>
#include <endian.h>

#include "blktrace_api.h"
#include "rbtree.h"

#define MINORBITS	20
#define MINORMASK	((1U << MINORBITS) - 1)
#define MAJOR(dev)	((unsigned int) ((dev) >> MINORBITS))
#define MINOR(dev)	((unsigned int) ((dev) & MINORMASK))

#define SECONDS(x) 		((unsigned long long)(x) / 1000000000)
#define NANO_SECONDS(x)		((unsigned long long)(x) % 1000000000)
#define DOUBLE_TO_NANO_ULL(d)	((unsigned long long)((d) * 1000000000))

#define min(a, b)	((a) < (b) ? (a) : (b))
#define max(a, b)	((a) > (b) ? (a) : (b))

#define t_sec(t)	((t)->bytes >> 9)
#define t_kb(t)		((t)->bytes >> 10)

typedef __u32 u32;
typedef __u8 u8;

struct io_stats {
	unsigned long qreads, qwrites, creads, cwrites, mreads, mwrites;
	unsigned long ireads, iwrites, rrqueue, wrqueue;
	unsigned long long qread_kb, qwrite_kb, cread_kb, cwrite_kb;
	unsigned long long iread_kb, iwrite_kb;
	unsigned long long mread_kb, mwrite_kb;
	unsigned long qreads_pc, qwrites_pc, ireads_pc, iwrites_pc;
	unsigned long rrqueue_pc, wrqueue_pc, creads_pc, cwrites_pc;
	unsigned long long qread_kb_pc, qwrite_kb_pc, iread_kb_pc, iwrite_kb_pc;
	unsigned long io_unplugs, timer_unplugs;
};

struct per_cpu_info {
	unsigned int cpu;
	unsigned int nelems;

	int fd;
	int fdblock;
	char fname[128];

	struct io_stats io_stats;

	struct rb_root rb_last;
	unsigned long rb_last_entries;
	unsigned long last_sequence;
	unsigned long smallest_seq_read;

	struct skip_info *skips_head;
	struct skip_info *skips_tail;
};

extern FILE *ofp;
extern int data_is_native;
extern struct timespec abs_start_time;

#define CHECK_MAGIC(t)		(((t)->magic & 0xffffff00) == BLK_IO_TRACE_MAGIC)
#define SUPPORTED_VERSION	(0x07)

#define __bswap_16 bswap_16
#define __bswap_32 bswap_32
#define __bswap_64 bswap_64

#if __BYTE_ORDER == __LITTLE_ENDIAN
#define be16_to_cpu(x)		__bswap_16(x)
#define be32_to_cpu(x)		__bswap_32(x)
#define be64_to_cpu(x)		__bswap_64(x)
#define cpu_to_be16(x)		__bswap_16(x)
#define cpu_to_be32(x)		__bswap_32(x)
#define cpu_to_be64(x)		__bswap_64(x)
#elif __BYTE_ORDER == __BIG_ENDIAN
#define be16_to_cpu(x)		(x)
#define be32_to_cpu(x)		(x)
#define be64_to_cpu(x)		(x)
#define cpu_to_be16(x)		(x)
#define cpu_to_be32(x)		(x)
#define cpu_to_be64(x)		(x)
#else
#error "Bad arch"
#endif

static inline int verify_trace(struct blk_io_trace *t)
{
	if (!CHECK_MAGIC(t)) {
		fprintf(stderr, "bad trace magic %x\n", t->magic);
		return 1;
	}
	if ((t->magic & 0xff) != SUPPORTED_VERSION) {
		fprintf(stderr, "unsupported trace version %x\n", 
			t->magic & 0xff);
		return 1;
	}

	return 0;
}

static inline void trace_to_cpu(struct blk_io_trace *t)
{
	if (data_is_native)
		return;

	t->magic	= be32_to_cpu(t->magic);
	t->sequence	= be32_to_cpu(t->sequence);
	t->time		= be64_to_cpu(t->time);
	t->sector	= be64_to_cpu(t->sector);
	t->bytes	= be32_to_cpu(t->bytes);
	t->action	= be32_to_cpu(t->action);
	t->pid		= be32_to_cpu(t->pid);
	t->device	= be32_to_cpu(t->device);
	t->cpu		= be16_to_cpu(t->cpu);
	t->error	= be16_to_cpu(t->error);
	t->pdu_len	= be16_to_cpu(t->pdu_len);
}

/*
 * check whether data is native or not
 */
static inline int check_data_endianness(u32 magic)
{
	if ((magic & 0xffffff00) == BLK_IO_TRACE_MAGIC) {
		data_is_native = 1;
		return 0;
	}

	magic = __bswap_32(magic);
	if ((magic & 0xffffff00) == BLK_IO_TRACE_MAGIC) {
		data_is_native = 0;
		return 0;
	}

	return 1;
}

extern void set_all_format_specs(char *);
extern int add_format_spec(char *);
extern void process_fmt(char *, struct per_cpu_info *, struct blk_io_trace *,
			unsigned long long, int, unsigned char *);
extern int valid_act_opt(int);
extern int find_mask_map(char *);
extern char *find_process_name(pid_t);

#endif
