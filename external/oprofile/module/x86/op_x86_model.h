/**
 * @file op_x86_model.h
 * interface to x86 model-specific MSR operations
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Graydon Hoare
 */

#ifndef OP_X86_MODEL_H
#define OP_X86_MODEL_H

#include "oprofile.h"

struct op_saved_msr {
	uint high;
	uint low;
};

struct op_msr_group {
	uint * addrs;
	struct op_saved_msr * saved;
};

struct op_msrs {
	struct op_msr_group counters;
	struct op_msr_group controls;
};

struct pt_regs;

struct op_x86_model_spec {
	uint const num_counters;
	uint const num_controls;
	void (*fill_in_addresses)(struct op_msrs * const msrs);
	void (*setup_ctrs)(struct op_msrs const * const msrs);
	void (*check_ctrs)(uint const cpu, 
		struct op_msrs const * const msrs,
		struct pt_regs * const regs);
	void (*start)(struct op_msrs const * const msrs);
	void (*stop)(struct op_msrs const * const msrs);
};

extern struct op_x86_model_spec const op_ppro_spec;
extern struct op_x86_model_spec const op_athlon_spec;
extern struct op_x86_model_spec const op_p4_spec;
#ifdef HT_SUPPORT
extern struct op_x86_model_spec const op_p4_ht2_spec;
#endif

#endif /* OP_X86_MODEL_H */
