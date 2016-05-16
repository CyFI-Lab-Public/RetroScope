/**
 * @file daemon/opd_trans.h
 * Processing the sample buffer
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 *
 * Modified by Maynard Johnson <maynardj@us.ibm.com>
 * These modifications are:
 * (C) Copyright IBM Corporation 2007
 */

#ifndef OPD_TRANS_H
#define OPD_TRANS_H

#include "opd_cookie.h"
#include "op_types.h"

#include <stdint.h>

struct sfile;
struct anon_mapping;

enum tracing_type {
	TRACING_OFF,
	TRACING_START,
	TRACING_ON
};

/**
 * Transient values used for parsing the event buffer.
 * Note that these are reset for each buffer read, but
 * that should be ok as in the kernel, cpu_buffer_reset()
 * ensures that a correct context starts off the buffer.
 */
struct transient {
	char const * buffer;
	size_t remaining;
	enum tracing_type tracing;
	struct sfile * current;
	struct sfile * last;
	struct anon_mapping * anon;
	struct anon_mapping * last_anon;
	cookie_t cookie;
	cookie_t app_cookie;
	vma_t pc;
	vma_t last_pc;
	unsigned long event;
	int in_kernel;
	unsigned long cpu;
	pid_t tid;
	pid_t tgid;
	uint64_t embedded_offset;
	void * ext;
};

typedef void (*handler_t)(struct transient *);
extern handler_t handlers[];

uint64_t pop_buffer_value(struct transient * trans);
int enough_remaining(struct transient * trans, size_t size);
static inline void update_trans_last(struct transient * trans)
{
	trans->last = trans->current;
	trans->last_anon = trans->anon;
	trans->last_pc = trans->pc;
}

extern size_t kernel_pointer_size;
static inline int is_escape_code(uint64_t code)
{
	return kernel_pointer_size == 4 ? code == ~0LU : code == ~0LLU;
}

void opd_process_samples(char const * buffer, size_t count);

/** used when we need to clear data that's been freed */
void clear_trans_last(struct transient * trans);

/** used when we need to clear data that's been freed */
void clear_trans_current(struct transient * trans);

#endif /* OPD_TRANS_H */
