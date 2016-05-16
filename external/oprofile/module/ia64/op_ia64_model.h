/**
 * @file op_ia64_model.h
 * interface to ia64 model-specific MSR operations
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Graydon Hoare
 * @author Will Cohen
 */

#ifndef OP_IA64_MODEL_H
#define OP_IA64_MODEL_H

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

#endif /* OP_IA64_MODEL_H */
