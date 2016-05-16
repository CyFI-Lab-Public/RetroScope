/**
 * @file daemon/opd_spu.c
 * Processing the sample buffer for Cell BE SPU profile
 *
 * @remark Copyright 2007 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Maynard Johnson
 * (C) Copyright IBM Corporation 2007
 */

#include "opd_interface.h"
#include "opd_printf.h"
#include "opd_sfile.h"
#include "opd_stats.h"
#include "opd_trans.h"
#include "op_libiberty.h"

#include <stdlib.h>
#include <stdio.h>

struct spu_context_info {
	pid_t tid;
	pid_t tgid;
	cookie_t app_cookie;
	uint64_t embedded_offset;
	cookie_t spu_cookie;
};

static struct spu_context_info * spu_context_cache;

/* Forward declaration */
static void process_spu_samples(struct transient * trans);

void (*special_processor)(struct transient *);

/*
 * This function is called when the first value found in the
 * buffer (after the beginning ESCAPE_CODE) is SPU_PROFILING_CODE.
 * Once we get here, the rest of the processing of the buffer is
 * Cell-SPU-specific, so we do not need to return until the
 * trans.buffer is empty.
 */
void code_spu_profiling(struct transient * trans)
{
	/* Next value in buffer is the number of SPUs. */
	unsigned long long num_spus = pop_buffer_value(trans);
	/* Free the cache from previous run */
	free(spu_context_cache);
	spu_context_cache = xmalloc(sizeof(struct spu_context_info) * num_spus);
	special_processor = process_spu_samples;
	process_spu_samples(trans);
}

void code_spu_ctx_switch(struct transient * trans)
{
	clear_trans_current(trans);

	if (!enough_remaining(trans, 6)) {
		trans->remaining = 0;
		return;
	}

	/* First value in the buffer for an SPU context switch is
	 * the SPU number.  For SPU profiling, 'cpu' = 'spu'.
	 */
	trans->cpu = pop_buffer_value(trans);
	trans->tid = pop_buffer_value(trans);
	trans->tgid = pop_buffer_value(trans);
	trans->app_cookie = pop_buffer_value(trans);

	if (vmisc) {
		char const * app = find_cookie(trans->app_cookie);
		printf("SPU_CTX_SWITCH to tid %lu, tgid %lu, cookie %llx(%s)\n",
		       (unsigned long)trans->tid, (unsigned long)trans->tgid,
		       trans->app_cookie, app ? app : "none");
	}

	/* The trans->cookie will point to the binary file where the SPU ELF
	 * can be found.  If the SPU ELF is embedded, it may be embedded in
	 * either the executable application binary or a shared lib.  If shared
	 * library, then trans->cookie will differ from the previously obtained
	 * trans->app_cookie.  For the non-embedded case, trans->cookie always
	 * points to a separate binary file.
	 */
	trans->cookie = pop_buffer_value(trans);
	trans->embedded_offset = pop_buffer_value(trans);
}


static void cache_spu_context_info(struct transient * trans)
{
	int i = trans->cpu;
	spu_context_cache[i].tid = trans->tid;
	spu_context_cache[i].tgid = trans->tgid;
	spu_context_cache[i].app_cookie = trans->app_cookie;
	spu_context_cache[i].embedded_offset = trans->embedded_offset;
	spu_context_cache[i].spu_cookie = trans->cookie;
}

static void update_trans_for_spu(struct transient * trans)
{
	int i = trans->cpu;
	trans->tid = spu_context_cache[i].tid;
	trans->tgid = spu_context_cache[i].tgid;
	trans->app_cookie = spu_context_cache[i].app_cookie;
	trans->embedded_offset = spu_context_cache[i].embedded_offset;
	trans->cookie = spu_context_cache[i].spu_cookie;
}
#define SPU_NUM_MASK 0xFFFFFFFF00000000ULL
#define SPU_CYCLES_COUNTER 0

static void opd_put_spu_sample
(struct transient * trans, unsigned long long pc)
{
	unsigned long spu_number = (pc & SPU_NUM_MASK) >> 32;
	if (trans->cpu != spu_number) {
		trans->cpu = spu_number;
	        clear_trans_current(trans);
		update_trans_for_spu(trans);
	}
	/* get the current sfile if needed */
	if (!trans->current)
		trans->current = sfile_find(trans);

	if (trans->tracing != TRACING_ON)
		trans->event = SPU_CYCLES_COUNTER;

	trans->pc = (pc & ~SPU_NUM_MASK);
	/* log the sample or arc */
	sfile_log_sample(trans);

	/* switch to trace mode */
	if (trans->tracing == TRACING_START)
		trans->tracing = TRACING_ON;

	update_trans_last(trans);
}

/*
 * This function processes SPU context switches and
 * SPU program counter samples.  After processing a
 * context switch (via handlers[code)), we cache the
 * SPU context information that has been temporarily
 * stored in trans.
 */
static void process_spu_samples(struct transient * trans)
{
	unsigned long long code;
	trans->in_kernel = 0;
	while (trans->remaining) {
		code = pop_buffer_value(trans);

		if (!is_escape_code(code)) {
			opd_put_spu_sample(trans, code);
			continue;
		}

		if (!trans->remaining) {
			verbprintf(vmisc, "Dangling ESCAPE_CODE.\n");
			opd_stats[OPD_DANGLING_CODE]++;
			break;
		}

		/* started with ESCAPE_CODE, next is type */
		code = pop_buffer_value(trans);

		if (code >= LAST_CODE) {
			fprintf(stderr, "Unknown code %llu\n", code);
			abort();
		}

		handlers[code](trans);
		cache_spu_context_info(trans);
	}
}
