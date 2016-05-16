/**
 * @file daemon/opd_trans.c
 * Processing the sample buffer
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 * Modified by Aravind Menon for Xen
 * These modifications are:
 * Copyright (C) 2005 Hewlett-Packard Co.
 *
 * Modified by Maynard Johnson <maynardj@us.ibm.com>
 * These modifications are:
 * (C) Copyright IBM Corporation 2007
 */

#include "opd_trans.h"
#include "opd_kernel.h"
#include "opd_sfile.h"
#include "opd_anon.h"
#include "opd_stats.h"
#include "opd_printf.h"
#include "opd_interface.h"
 
#include <limits.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>
#include <errno.h>

extern size_t kernel_pointer_size;


void clear_trans_last(struct transient * trans)
{
	trans->last = NULL;
	trans->last_anon = NULL;
}


void clear_trans_current(struct transient * trans)
{
	trans->current = NULL;
	trans->anon = NULL;
}


uint64_t pop_buffer_value(struct transient * trans)
{
	uint64_t val;

	if (!trans->remaining) {
		fprintf(stderr, "BUG: popping empty buffer !\n");
		abort();
	}

	if (kernel_pointer_size == 4) {
		uint32_t const * lbuf = (void const *)trans->buffer;
		val = *lbuf;
	} else {
		uint64_t const * lbuf = (void const *)trans->buffer;
		val = *lbuf;
	}

	trans->remaining--;
	trans->buffer += kernel_pointer_size;
	return val;
}


int enough_remaining(struct transient * trans, size_t size)
{
	if (trans->remaining >= size)
		return 1;

	verbprintf(vmisc, "Dangling ESCAPE_CODE.\n");
	opd_stats[OPD_DANGLING_CODE]++;
	return 0;
}


static void opd_put_sample(struct transient * trans, unsigned long long pc)
{
	unsigned long long event;

	if (!enough_remaining(trans, 1)) {
		trans->remaining = 0;
		return;
	}

	event = pop_buffer_value(trans);

	if (trans->tracing != TRACING_ON)
		trans->event = event;

	trans->pc = pc;

	/* sfile can change at each sample for kernel */
	if (trans->in_kernel != 0)
		clear_trans_current(trans);

	if (!trans->in_kernel && trans->cookie == NO_COOKIE)
		trans->anon = find_anon_mapping(trans);

	/* get the current sfile if needed */
	if (!trans->current)
		trans->current = sfile_find(trans);

	/*
	 * can happen if kernel sample falls through the cracks, or if
	 * it's a sample from an anon region we couldn't find
	 */
	if (!trans->current)
		goto out;

	/* FIXME: this logic is perhaps too harsh? */
	if (trans->current->ignored || (trans->last && trans->last->ignored))
		goto out;

	/* log the sample or arc */
	sfile_log_sample(trans);

out:
	/* switch to trace mode */
	if (trans->tracing == TRACING_START)
		trans->tracing = TRACING_ON;

	update_trans_last(trans);
}


static void code_unknown(struct transient * trans __attribute__((unused)))
{
	fprintf(stderr, "Unknown code !\n");
	abort();
}


static void code_ctx_switch(struct transient * trans)
{
	clear_trans_current(trans);

	if (!enough_remaining(trans, 5)) {
		trans->remaining = 0;
		return;
	}

	trans->tid = pop_buffer_value(trans);
	trans->app_cookie = pop_buffer_value(trans);
	/* must be ESCAPE_CODE, CTX_TGID_CODE, tgid. Like this
	 * because tgid was added later in a compatible manner.
	 */
	pop_buffer_value(trans);
	pop_buffer_value(trans);
	trans->tgid = pop_buffer_value(trans);

	if (vmisc) {
		char const * app = find_cookie(trans->app_cookie);
		printf("CTX_SWITCH to tid %lu, tgid %lu, cookie %llx(%s)\n",
		       (unsigned long)trans->tid, (unsigned long)trans->tgid,
		       trans->app_cookie, app ? app : "none");
	}
}


static void code_cpu_switch(struct transient * trans)
{
	clear_trans_current(trans);

	if (!enough_remaining(trans, 1)) {
		trans->remaining = 0;
		return;
	}

	trans->cpu = pop_buffer_value(trans);
	verbprintf(vmisc, "CPU_SWITCH to %lu\n", trans->cpu);
}


static void code_cookie_switch(struct transient * trans)
{
	clear_trans_current(trans);

	if (!enough_remaining(trans, 1)) {
		trans->remaining = 0;
		return;
	}

	trans->cookie = pop_buffer_value(trans);

	if (vmisc) {
		char const * name = verbose_cookie(trans->cookie);
		verbprintf(vmisc, "COOKIE_SWITCH to cookie %s(%llx)\n",
			   name, trans->cookie);
	}
}


static void code_kernel_enter(struct transient * trans)
{
	verbprintf(vmisc, "KERNEL_ENTER_SWITCH to kernel\n");
	trans->in_kernel = 1;
	clear_trans_current(trans);
	/* subtlety: we must keep trans->cookie cached,
	 * even though it's meaningless for the kernel -
	 * we won't necessarily get a cookie switch on
	 * kernel exit. See comments in opd_sfile.c
	 */
}


static void code_user_enter(struct transient * trans)
{
	verbprintf(vmisc, "USER_ENTER_SWITCH to user-space\n");
	trans->in_kernel = 0;
	clear_trans_current(trans);
	clear_trans_last(trans);
}


static void code_module_loaded(struct transient * trans __attribute__((unused)))
{
	verbprintf(vmodule, "MODULE_LOADED_CODE\n");
	opd_reread_module_info();
	clear_trans_current(trans);
	clear_trans_last(trans);
}


/*
 * This also implicitly signals the end of the previous
 * trace, so we never explicitly set TRACING_OFF when
 * processing a buffer.
 */
static void code_trace_begin(struct transient * trans)
{
	verbprintf(varcs, "TRACE_BEGIN\n");
	trans->tracing = TRACING_START;
}

static void code_xen_enter(struct transient * trans)
{
	verbprintf(vmisc, "XEN_ENTER_SWITCH to xen\n");
	trans->in_kernel = 1;
	trans->current = NULL;
	/* subtlety: we must keep trans->cookie cached, even though it's
	 * meaningless for Xen - we won't necessarily get a cookie switch
	 * on Xen exit. See comments in opd_sfile.c. It seems that we can
	 * get away with in_kernel = 1 as long as we supply the correct
	 * Xen image, and its address range in startup find_kernel_image
	 * is modified to look in the Xen image also
	 */
}

extern void code_spu_profiling(struct transient * trans);
extern void code_spu_ctx_switch(struct transient * trans);

extern void code_ibs_fetch_sample(struct transient * trans);
extern void code_ibs_op_sample(struct transient * trans);

handler_t handlers[LAST_CODE + 1] = {
	&code_unknown,
	&code_ctx_switch,
	&code_cpu_switch,
	&code_cookie_switch,
	&code_kernel_enter,
	&code_user_enter,
	&code_module_loaded,
	/* tgid handled differently */
	&code_unknown,
	&code_trace_begin,
	&code_unknown,
	&code_xen_enter,
#if defined(__powerpc__)
	&code_spu_profiling,
	&code_spu_ctx_switch,
#else
	&code_unknown,
	&code_unknown,
#endif
	&code_ibs_fetch_sample,
	&code_ibs_op_sample,
};

extern void (*special_processor)(struct transient *);

void opd_process_samples(char const * buffer, size_t count)
{
	struct transient trans = {
		.buffer = buffer,
		.remaining = count,
		.tracing = TRACING_OFF,
		.current = NULL,
		.last = NULL,
		.cookie = INVALID_COOKIE,
		.app_cookie = INVALID_COOKIE,
		.anon = NULL,
		.last_anon = NULL,
		.pc = 0,
		.last_pc = 0,
		.event = 0,
		.in_kernel = -1,
		.cpu = -1,
		.tid = -1,
		.embedded_offset = UNUSED_EMBEDDED_OFFSET,
		.tgid = -1,
		.ext = NULL
	};

	/* FIXME: was uint64_t but it can't compile on alpha where uint64_t
	 * is an unsigned long and below the printf("..." %llu\n", code)
	 * generate a warning, this look like a stopper to use c98 types :/
	 */
	unsigned long long code;

	if (special_processor) {
		special_processor(&trans);
		return;
	}

	while (trans.remaining) {
		code = pop_buffer_value(&trans);

		if (!is_escape_code(code)) {
			opd_put_sample(&trans, code);
			continue;
		}

		if (!trans.remaining) {
			verbprintf(vmisc, "Dangling ESCAPE_CODE.\n");
			opd_stats[OPD_DANGLING_CODE]++;
			break;
		}

		// started with ESCAPE_CODE, next is type
		code = pop_buffer_value(&trans);
	
		if (code >= LAST_CODE) {
			fprintf(stderr, "Unknown code %llu\n", code);
			abort();
		}

		handlers[code](&trans);
	}
}
