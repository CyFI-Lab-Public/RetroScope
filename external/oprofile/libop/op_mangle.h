/**
 * @file op_mangle.h
 * Mangling of sample file names
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OP_MANGLE_H
#define OP_MANGLE_H

#include <sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

enum mangle_flags {
	MANGLE_NONE      = 0,
	MANGLE_CPU       = (1 << 0),
	MANGLE_TGID      = (1 << 1),
	MANGLE_TID       = (1 << 2),
	MANGLE_KERNEL    = (1 << 3),
	MANGLE_CALLGRAPH = (1 << 4),
	MANGLE_ANON      = (1 << 5),
	MANGLE_CG_ANON   = (1 << 6),
};

/**
 * Temporary structure for passing parameters to
 * op_mangle_filename.
 */
struct mangle_values {
	int flags;

	char const * image_name;
	char const * anon_name;
	char const * dep_name;
	char const * cg_image_name;
	char const * event_name;
	int count;
	unsigned int unit_mask;
	pid_t tgid;
	pid_t tid;
	int cpu;
};

/**
 * op_mangle_filename - mangle a sample filename
 * @param values  parameters to use as mangling input
 *
 * See also PP:3 for the encoding scheme
 *
 * Returns a char* pointer to the mangled string. Caller
 * is responsible for freeing this string.
 */
char * op_mangle_filename(struct mangle_values const * values);

#ifdef __cplusplus
}
#endif

#endif /* OP_MANGLE_H */
