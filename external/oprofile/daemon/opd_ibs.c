/**
 * @file daemon/opd_ibs.c
 * AMD Family10h Instruction Based Sampling (IBS) handling.
 *
 * @remark Copyright 2007-2010 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Jason Yeh <jason.yeh@amd.com>
 * @author Paul Drongowski <paul.drongowski@amd.com>
 * @author Suravee Suthikulpanit <suravee.suthikulpanit@amd.com>
 * Copyright (c) 2008 Advanced Micro Devices, Inc.
 */

#include "op_hw_config.h"
#include "op_events.h"
#include "op_string.h"
#include "op_libiberty.h"
#include "opd_printf.h"
#include "opd_trans.h"
#include "opd_events.h"
#include "opd_kernel.h"
#include "opd_anon.h"
#include "opd_sfile.h"
#include "opd_interface.h"
#include "opd_mangling.h"
#include "opd_extended.h"
#include "opd_ibs.h"
#include "opd_ibs_trans.h"
#include "opd_ibs_macro.h"

#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <limits.h>

extern op_cpu cpu_type;
extern int no_event_ok;
extern int sfile_equal(struct sfile const * sf, struct sfile const * sf2);
extern void sfile_dup(struct sfile * to, struct sfile * from);
extern char * session_dir;

/* IBS Select Counters */
static unsigned int ibs_selected_size;

/* These flags store the IBS-derived events selection. */
static unsigned int ibs_fetch_selected_flag;
static unsigned int ibs_op_selected_flag;
static unsigned int ibs_op_ls_selected_flag;
static unsigned int ibs_op_nb_selected_flag;

/* IBS Statistics */
static unsigned long ibs_fetch_sample_stats;
static unsigned long ibs_fetch_incomplete_stats;
static unsigned long ibs_op_sample_stats;
static unsigned long ibs_op_incomplete_stats;
static unsigned long ibs_derived_event_stats;

/*
 * IBS Virtual Counter
 */
struct opd_event ibs_vc[OP_MAX_IBS_COUNTERS];

/* IBS Virtual Counter Index(VCI) Map*/
unsigned int ibs_vci_map[OP_MAX_IBS_COUNTERS];

/* CPUID information */
unsigned int ibs_family;
unsigned int ibs_model;
unsigned int ibs_stepping;

/* IBS Extended MSRs */
static unsigned long ibs_bta_enabled;

/* IBS log files */
FILE * memaccess_log;
FILE * bta_log;

/**
 * This function converts IBS fetch event flags and values into
 * derived events. If the tagged (sampled) fetched caused a derived
 * event, the derived event is tallied.
 */
static void opd_log_ibs_fetch(struct transient * trans)
{
	struct ibs_fetch_sample * trans_fetch = ((struct ibs_sample*)(trans->ext))->fetch;
	if (!trans_fetch)
		return;

	trans_ibs_fetch(trans, ibs_fetch_selected_flag);
}


/**
 * This function translates the IBS op event flags and values into
 * IBS op derived events. If an op derived event occured, it's tallied.
 */
static void opd_log_ibs_op(struct transient * trans)
{
	struct ibs_op_sample * trans_op = ((struct ibs_sample*)(trans->ext))->op;
	if (!trans_op)
		return;

	trans_ibs_op_mask_reserved(ibs_family, trans);

	if (trans_ibs_op_rip_invalid(trans) != 0)
		return;

	trans_ibs_op(trans, ibs_op_selected_flag);
	trans_ibs_op_ls(trans, ibs_op_ls_selected_flag);
	trans_ibs_op_nb(trans, ibs_op_nb_selected_flag);
	trans_ibs_op_ls_memaccess(trans);
	trans_ibs_op_bta(trans);
}


static void opd_put_ibs_sample(struct transient * trans)
{
	unsigned long long event = 0;
	struct kernel_image * k_image = NULL;
	struct ibs_fetch_sample * trans_fetch = ((struct ibs_sample*)(trans->ext))->fetch;

	if (!enough_remaining(trans, 1)) {
		trans->remaining = 0;
		return;
	}

	/* IBS can generate samples with invalid dcookie and
	 * in kernel address range. Map such samples to vmlinux
	 * only if the user either specifies a range, or vmlinux.
	 */
	if (trans->cookie == INVALID_COOKIE
	    && (k_image = find_kernel_image(trans)) != NULL
	    && (k_image->start != 0 && k_image->end != 0)
	    && trans->in_kernel == 0)
		trans->in_kernel = 1;

	if (trans->tracing != TRACING_ON)
		trans->event = event;

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

	if (trans_fetch)
		opd_log_ibs_fetch(trans);
	else
		opd_log_ibs_op(trans);
out:
	/* switch to trace mode */
	if (trans->tracing == TRACING_START)
		trans->tracing = TRACING_ON;

	update_trans_last(trans);
}


static void get_ibs_bta_status()
{
	FILE * fp = NULL;
	char buf[PATH_MAX];

	/* Default to disable */
	ibs_bta_enabled = 0;

	snprintf(buf, PATH_MAX, "/dev/oprofile/ibs_op/branch_target");
	fp = fopen(buf, "r");
	if (!fp)
		return;

	while (fgets(buf, PATH_MAX, fp) != NULL)
		ibs_bta_enabled = strtoul(buf, NULL, 10);	

	fclose(fp);
}


void code_ibs_fetch_sample(struct transient * trans)
{
	struct ibs_fetch_sample * trans_fetch = NULL;

	if (!enough_remaining(trans, 7)) {
		verbprintf(vext, "not enough remaining\n");
		trans->remaining = 0;
		ibs_fetch_incomplete_stats++;
		return;
	}

	ibs_fetch_sample_stats++;

	trans->ext = xmalloc(sizeof(struct ibs_sample));
	((struct ibs_sample*)(trans->ext))->fetch = xmalloc(sizeof(struct ibs_fetch_sample));
	trans_fetch = ((struct ibs_sample*)(trans->ext))->fetch;

	trans_fetch->rip = pop_buffer_value(trans);

	trans_fetch->ibs_fetch_lin_addr_low   = pop_buffer_value(trans);
	trans_fetch->ibs_fetch_lin_addr_high  = pop_buffer_value(trans);

	trans_fetch->ibs_fetch_ctl_low        = pop_buffer_value(trans);
	trans_fetch->ibs_fetch_ctl_high       = pop_buffer_value(trans);
	trans_fetch->ibs_fetch_phys_addr_low  = pop_buffer_value(trans);
	trans_fetch->ibs_fetch_phys_addr_high = pop_buffer_value(trans);

	verbprintf(vsamples,
		"FETCH_X CPU:%ld PID:%ld RIP:%lx CTL_H:%x LAT:%d P_HI:%x P_LO:%x L_HI:%x L_LO:%x\n",
		trans->cpu,
		(long)trans->tgid,
		trans_fetch->rip,
		(trans_fetch->ibs_fetch_ctl_high >> 16) & 0x3ff,
		(trans_fetch->ibs_fetch_ctl_high) & 0xffff,
		trans_fetch->ibs_fetch_phys_addr_high,
		trans_fetch->ibs_fetch_phys_addr_low,
		trans_fetch->ibs_fetch_lin_addr_high,
		trans_fetch->ibs_fetch_lin_addr_low) ;

	/* Overwrite the trans->pc with the more accurate trans_fetch->rip */
	trans->pc = trans_fetch->rip;

	opd_put_ibs_sample(trans);

	free(trans_fetch);
	free(trans->ext);
	trans->ext = NULL;
}


static void get_ibs_op_bta_sample(struct transient * trans,
				    struct ibs_op_sample * trans_op)
{
	// Check remaining
	if (!enough_remaining(trans, 2)) {
		verbprintf(vext, "not enough remaining\n");
		trans->remaining = 0;
		ibs_op_incomplete_stats++;
		return;
	}

	if (ibs_bta_enabled == 1) {
		trans_op->ibs_op_brtgt_addr = pop_buffer_value(trans);
	
		// Check if branch target address is valid (MSRC001_1035[37] == 1]
		if ((trans_op->ibs_op_data1_high & (0x00000001 << 5)) == 0) {
			trans_op->ibs_op_brtgt_addr = 0;
		}
	} else {
		trans_op->ibs_op_brtgt_addr = 0;
	}
}


void code_ibs_op_sample(struct transient * trans)
{
	struct ibs_op_sample * trans_op= NULL;

	if (!enough_remaining(trans, 13)) {
		verbprintf(vext, "not enough remaining\n");
		trans->remaining = 0;
		ibs_op_incomplete_stats++;
		return;
	}

	ibs_op_sample_stats++;

	trans->ext = xmalloc(sizeof(struct ibs_sample));
	((struct ibs_sample*)(trans->ext))->op = xmalloc(sizeof(struct ibs_op_sample));
	trans_op = ((struct ibs_sample*)(trans->ext))->op;

	trans_op->rip = pop_buffer_value(trans);

	trans_op->ibs_op_lin_addr_low = pop_buffer_value(trans);
	trans_op->ibs_op_lin_addr_high = pop_buffer_value(trans);

	trans_op->ibs_op_data1_low         = pop_buffer_value(trans);
	trans_op->ibs_op_data1_high        = pop_buffer_value(trans);
	trans_op->ibs_op_data2_low         = pop_buffer_value(trans);
	trans_op->ibs_op_data2_high        = pop_buffer_value(trans);
	trans_op->ibs_op_data3_low         = pop_buffer_value(trans);
	trans_op->ibs_op_data3_high        = pop_buffer_value(trans);
	trans_op->ibs_op_ldst_linaddr_low  = pop_buffer_value(trans);
	trans_op->ibs_op_ldst_linaddr_high = pop_buffer_value(trans);
	trans_op->ibs_op_phys_addr_low     = pop_buffer_value(trans);
	trans_op->ibs_op_phys_addr_high    = pop_buffer_value(trans);

	get_ibs_op_bta_sample(trans, trans_op);

	verbprintf(vsamples,
	   "IBS_OP_X CPU:%ld PID:%d RIP:%lx D1HI:%x D1LO:%x D2LO:%x D3HI:%x D3LO:%x L_LO:%x P_LO:%x\n",
		   trans->cpu,
		   trans->tgid,
		   trans_op->rip,
		   trans_op->ibs_op_data1_high,
		   trans_op->ibs_op_data1_low,
		   trans_op->ibs_op_data2_low,
		   trans_op->ibs_op_data3_high,
		   trans_op->ibs_op_data3_low,
		   trans_op->ibs_op_ldst_linaddr_low,
		   trans_op->ibs_op_phys_addr_low);

	/* Overwrite the trans->pc with the more accurate trans_op->rip */
	trans->pc = trans_op->rip;

	opd_put_ibs_sample(trans);

	free(trans_op);
	free(trans->ext);
	trans->ext = NULL;
}


/** Convert IBS event to value used for data structure indexing */
static unsigned long ibs_event_to_counter(unsigned long x)
{
	unsigned long ret = ~0UL;

	if (IS_IBS_FETCH(x))
		ret = (x - IBS_FETCH_BASE);
	else if (IS_IBS_OP(x))
		ret = (x - IBS_OP_BASE + IBS_FETCH_MAX);
	else if (IS_IBS_OP_LS(x))
		ret = (x - IBS_OP_LS_BASE + IBS_OP_MAX + IBS_FETCH_MAX);
	else if (IS_IBS_OP_NB(x))
		ret = (x - IBS_OP_NB_BASE + IBS_OP_LS_MAX + IBS_OP_MAX + IBS_FETCH_MAX);

	return (ret != ~0UL) ? ret + OP_MAX_COUNTERS : ret;
}


void opd_log_ibs_event(unsigned int event,
	struct transient * trans)
{
	ibs_derived_event_stats++;
	trans->event = event;
	sfile_log_sample_count(trans, 1);
}


void opd_log_ibs_count(unsigned int event,
			struct transient * trans,
			unsigned int count)
{
	ibs_derived_event_stats++;
	trans->event = event;
	sfile_log_sample_count(trans, count);
}


static unsigned long get_ibs_vci_key(unsigned int event)
{
	unsigned long key = ibs_event_to_counter(event);
	if (key == ~0UL || key < OP_MAX_COUNTERS)
		return ~0UL;

	key = key - OP_MAX_COUNTERS;

	return key;
}


static int ibs_parse_and_set_events(char * str)
{
	char * tmp, * ptr, * tok1, * tok2 = NULL;
	int is_done = 0;
	struct op_event * event = NULL;
	op_cpu cpu_type = CPU_NO_GOOD;
	unsigned long key;

	if (!str)
		return -1;

	cpu_type = op_get_cpu_type();
	op_events(cpu_type);

	tmp = op_xstrndup(str, strlen(str));
	ptr = tmp;

	while (is_done != 1
		&& (tok1 = strtok_r(ptr, ",", &tok2)) != NULL) {

		if ((ptr = strstr(tok1, ":")) != NULL) {
			*ptr = '\0';
			is_done = 1;
		}

		// Resove event number
		event = find_event_by_name(tok1, 0, 0);
		if (!event)
			return -1;

		// Grouping
		if (IS_IBS_FETCH(event->val)) {
			ibs_fetch_selected_flag |= 1 << IBS_FETCH_OFFSET(event->val);
		} else if (IS_IBS_OP(event->val)) {
			ibs_op_selected_flag |= 1 << IBS_OP_OFFSET(event->val);
		} else if (IS_IBS_OP_LS(event->val)) {
			ibs_op_ls_selected_flag |= 1 << IBS_OP_LS_OFFSET(event->val);
		} else if (IS_IBS_OP_NB(event->val)) {
			ibs_op_nb_selected_flag |= 1 << IBS_OP_NB_OFFSET(event->val);
		} else {
			return -1;
		}

		key = get_ibs_vci_key(event->val);
		if (key == ~0UL)
			return -1;

		ibs_vci_map[key] = ibs_selected_size;

		/* Initialize part of ibs_vc */
		ibs_vc[ibs_selected_size].name    = tok1;
		ibs_vc[ibs_selected_size].value   = event->val;
		ibs_vc[ibs_selected_size].counter = ibs_selected_size + OP_MAX_COUNTERS;
		ibs_vc[ibs_selected_size].kernel  = 1;
		ibs_vc[ibs_selected_size].user    = 1;

		ibs_selected_size++;

		ptr = NULL;
	}

	return 0;
}


static int ibs_parse_counts(char * str, unsigned long int * count)
{
	char * tmp, * tok1, * tok2 = NULL, *end = NULL;
	if (!str)
		return -1;

	tmp = op_xstrndup(str, strlen(str));
	tok1 = strtok_r(tmp, ":", &tok2);
	*count = strtoul(tok1, &end, 10);
	if ((end && *end) || *count == 0
	    || errno == EINVAL || errno == ERANGE) {
		fprintf(stderr,"Invalid count (%s)\n", str);
		return -1;
	}

	return 0;
}


static int ibs_parse_and_set_um_fetch(char const * str)
{
	if (!str)
		return -1;
	return 0;
}


static int ibs_parse_and_set_um_op(char const * str, unsigned long int * ibs_op_um)
{
	char * end = NULL;
	if (!str)
		return -1;

	*ibs_op_um = strtoul(str, &end, 16);
	if ((end && *end) || errno == EINVAL || errno == ERANGE) {
		fprintf(stderr,"Invalid unitmaks (%s)\n", str);
		return -1;
	}
	return 0;
}


static void check_cpuid_family_model_stepping()
{
#if defined(__i386__) || defined(__x86_64__) 
       union {
                unsigned eax;
                struct {
                        unsigned stepping : 4;
                        unsigned model : 4;
                        unsigned family : 4;
                        unsigned res : 4;
                        unsigned ext_model : 4;
                        unsigned ext_family : 8;
                        unsigned res2 : 4;
                };
        } v;
	unsigned ebx, ecx, edx;

	/* CPUID Fn0000_0001_EAX Family, Model, Stepping */
#ifdef __PIC__
	__asm__ __volatile__ (
		"pushl %%ebx\n"
		"cpuid\n"
		"mov %%ebx, %1\n"
		"popl %%ebx"
		: "=a" (v.eax), "=r" (ebx), "=c" (ecx), "=d" (edx) : "0" (1)
	);
#else
	asm ("cpuid" : "=a" (v.eax), "=b" (ebx), "=c" (ecx), "=d" (edx) : "0" (1));
#endif

	ibs_family   = v.family + v.ext_family;
	ibs_model    = v.model + v.ext_model;
	ibs_stepping = v.stepping;
#else
	ibs_family   = 0;
	ibs_model    = 0;
	ibs_stepping = 0;
#endif
}


static int ibs_init(char const * argv)
{
	char * tmp, * ptr, * tok1, * tok2 = NULL;
	unsigned int i = 0;
	unsigned long int ibs_fetch_count = 0;
	unsigned long int ibs_op_count = 0;
	unsigned long int ibs_op_um = 0;

	if (!argv)
		return -1;

	if (empty_line(argv) != 0)
		return -1;

	tmp = op_xstrndup(argv, strlen(argv));
	ptr = (char *) skip_ws(tmp);

	// "fetch:event1,event2,....:count:um|op:event1,event2,.....:count:um"
	tok1 = strtok_r(ptr, "|", &tok2);

	while (tok1 != NULL) {

		if (!strncmp("fetch:", tok1, strlen("fetch:"))) {
			// Get to event section
			tok1 = tok1 + strlen("fetch:");
			if (ibs_parse_and_set_events(tok1) == -1)
				return -1;

			// Get to count section
			while (tok1) {
				if (*tok1 == '\0')
					return -1;
				if (*tok1 != ':') {
					tok1++;
				} else {
					tok1++;
					break;
				}
			}

			if (ibs_parse_counts(tok1, &ibs_fetch_count) == -1)
				return -1;

			// Get to um section
			while (tok1) {
				if (*tok1 == '\0')
					return -1;
				if (*tok1 != ':') {
					tok1++;
				} else {
					tok1++;
					break;
				}
			}

			if (ibs_parse_and_set_um_fetch(tok1) == -1)
				return -1;

		} else if (!strncmp("op:", tok1, strlen("op:"))) {
			// Get to event section
			tok1 = tok1 + strlen("op:");
			if (ibs_parse_and_set_events(tok1) == -1)
				return -1;

			// Get to count section
			while (tok1) {
				if (*tok1 == '\0')
					return -1;
				if (*tok1 != ':') {
					tok1++;
				} else {
					tok1++;
					break;
				}
			}

			if (ibs_parse_counts(tok1, &ibs_op_count) == -1)
				return -1;

			// Get to um section
			while (tok1) {
				if (*tok1 == '\0')
					return -1;
				if (*tok1 != ':') {
					tok1++;
				} else {
					tok1++;
					break;
				}
			}

			if (ibs_parse_and_set_um_op(tok1, &ibs_op_um))
				return -1;

		} else
			return -1;

		tok1 = strtok_r(NULL, "|", &tok2);
	}

	/* Initialize ibs_vc */
	for (i = 0 ; i < ibs_selected_size ; i++)
	{
		if (IS_IBS_FETCH(ibs_vc[i].value)) {
			ibs_vc[i].count   = ibs_fetch_count;
			ibs_vc[i].um      = 0;
		} else {
			ibs_vc[i].count   = ibs_op_count;
			ibs_vc[i].um      = ibs_op_um;
		}
	}

	// Allow no event
	no_event_ok = 1;

	check_cpuid_family_model_stepping();

	get_ibs_bta_status();

	/* Create IBS memory access log */
	memaccess_log = NULL;
	if (ibs_op_um & 0x2) {
		char filename[1024];
		strncpy(filename, session_dir, 1023);
		strncat(filename, "/samples/ibs_memaccess.log", 1024);
		if ((memaccess_log = fopen(filename, "w")) == NULL) {
			verbprintf(vext, "Warning: Cannot create file %s\n", filename);
			
		} else {
			fprintf (memaccess_log, "# IBS Memory Access Log\n\n");
			fprintf (memaccess_log, "# Format: app_cookie,cookie,cpu,tgid,tid,pc,branch-target-address,\n");
			fprintf (memaccess_log, "#         phy-hi:phy-low,lin-hi:lin-low,accese-type,latency\n\n");
		}
	}

	// Create IBS Branch Target Address (BTA) log	
	bta_log = NULL;
	if (ibs_bta_enabled) {
		char filename[1024];
		strncpy(filename, session_dir, 1023);
		strncat(filename, "/samples/ibs_bta.log", 1024);
		if ((bta_log = fopen(filename, "w")) == NULL) {
			verbprintf(vext, "Warning: Cannot create file %s\n", filename);
		} else {
			fprintf (bta_log, "# IBS Memory Access Log\n\n");
			fprintf (bta_log, "# Format: app_cookie,cookie,cpu,tgid,tid,pc,branch-target-address\n\n");
		}
	}

	return 0;
}


static int ibs_deinit()
{
	if (memaccess_log) {
		fclose (memaccess_log);
		memaccess_log = NULL;
	}
	
	if (bta_log) {
		fclose (bta_log);
		bta_log = NULL;
	}
	return 0;
}


static int ibs_print_stats()
{
	printf("Nr. IBS Fetch samples     : %lu (%lu entries)\n", 
		ibs_fetch_sample_stats, (ibs_fetch_sample_stats * 7));
	printf("Nr. IBS Fetch incompletes : %lu\n", ibs_fetch_incomplete_stats);
	printf("Nr. IBS Op samples        : %lu (%lu entries)\n", 
		ibs_op_sample_stats, (ibs_op_sample_stats * 13));
	printf("Nr. IBS Op incompletes    : %lu\n", ibs_op_incomplete_stats);
	printf("Nr. IBS derived events    : %lu\n", ibs_derived_event_stats);
	return 0;
}


static int ibs_sfile_create(struct sfile * sf)
{
	unsigned int i;
	sf->ext_files = xmalloc(ibs_selected_size * sizeof(odb_t));
	for (i = 0 ; i < ibs_selected_size ; ++i)
		odb_init(&sf->ext_files[i]);

	return 0;
}


static int ibs_sfile_dup (struct sfile * to, struct sfile * from)
{
	unsigned int i;
	if (from->ext_files != NULL) {
		to->ext_files = xmalloc(ibs_selected_size * sizeof(odb_t));
		for (i = 0 ; i < ibs_selected_size ; ++i)
			odb_init(&to->ext_files[i]);
	} else {
		to->ext_files = NULL;
	}
	return 0;
}

static int ibs_sfile_close(struct sfile * sf)
{
	unsigned int i;
	if (sf->ext_files != NULL) {
		for (i = 0; i < ibs_selected_size ; ++i)
			odb_close(&sf->ext_files[i]);

		free(sf->ext_files);
		sf->ext_files= NULL;
	}
	return 0;
}

static int ibs_sfile_sync(struct sfile * sf)
{
	unsigned int i;
	if (sf->ext_files != NULL) {
		for (i = 0; i < ibs_selected_size ; ++i)
			odb_sync(&sf->ext_files[i]);
	}
	return 0;
}

static odb_t * ibs_sfile_get(struct transient const * trans, int is_cg)
{
	struct sfile * sf = trans->current;
	struct sfile * last = trans->last;
	struct cg_entry * cg;
	struct list_head * pos;
	unsigned long hash;
	odb_t * file;
	unsigned long counter, ibs_vci, key;

	/* Note: "trans->event" for IBS is not the same as traditional
 	 * events.  Here, it has the actual event (0xfxxx), while the
 	 * traditional event has the event index.
 	 */
	key = get_ibs_vci_key(trans->event);
	if (key == ~0UL) {
		fprintf(stderr, "%s: Invalid IBS event %lu\n", __func__, trans->event);
		abort();
	}
	ibs_vci = ibs_vci_map[key];
	counter = ibs_vci + OP_MAX_COUNTERS;

	/* Creating IBS sfile if it not already exists */
	if (sf->ext_files == NULL)
		ibs_sfile_create(sf);

	file = &(sf->ext_files[ibs_vci]);
	if (!is_cg)
		goto open;

	hash = last->hashval & (CG_HASH_SIZE - 1);

	/* Need to look for the right 'to'. Since we're looking for
	 * 'last', we use its hash.
	 */
	list_for_each(pos, &sf->cg_hash[hash]) {
		cg = list_entry(pos, struct cg_entry, hash);
		if (sfile_equal(last, &cg->to)) {
			file = &(cg->to.ext_files[ibs_vci]);
			goto open;
		}
	}

	cg = xmalloc(sizeof(struct cg_entry));
	sfile_dup(&cg->to, last);
	list_add(&cg->hash, &sf->cg_hash[hash]);
	file = &(cg->to.ext_files[ibs_vci]);

open:
	if (!odb_open_count(file))
		opd_open_sample_file(file, last, sf, counter, is_cg);

	/* Error is logged by opd_open_sample_file */
	if (!odb_open_count(file))
		return NULL;

	return file;
}


/** Filled opd_event structure with IBS derived event information
 *  from the given counter value.
 */
static struct opd_event * ibs_sfile_find_counter_event(unsigned long counter)
{
	unsigned long ibs_vci;

	if (counter >= OP_MAX_COUNTERS + OP_MAX_IBS_COUNTERS
	    || counter < OP_MAX_COUNTERS) {
		fprintf(stderr,"Error: find_ibs_counter_event : "
				"invalid counter value %lu.\n", counter);
		abort();
	}

	ibs_vci = counter - OP_MAX_COUNTERS;
	return &ibs_vc[ibs_vci];
}


struct opd_ext_sfile_handlers ibs_sfile_handlers =
{
	.create = &ibs_sfile_create,
	.dup    = &ibs_sfile_dup,
	.close  = &ibs_sfile_close,
	.sync   = &ibs_sfile_sync,
	.get    = &ibs_sfile_get,
	.find_counter_event = &ibs_sfile_find_counter_event
};


struct opd_ext_handlers ibs_handlers =
{
	.ext_init        = &ibs_init,
	.ext_deinit      = &ibs_deinit,
	.ext_print_stats = &ibs_print_stats,
	.ext_sfile       = &ibs_sfile_handlers
};
