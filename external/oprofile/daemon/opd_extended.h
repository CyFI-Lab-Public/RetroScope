/**
 * @file opd_extended.h
 * OProfile Extended Feature
 *
 * @remark Copyright 2007-2009 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Suravee Suthikulpanit <suravee.suthikulpanit@amd.com>
 * Copyright (c) 2009 Advanced Micro Devices, Inc.
 */

#ifndef OPD_EXTENDED_H
#define OPD_EXTENDED_H

#include "opd_trans.h"
#include "odb.h"

#include <stdlib.h>
#include <stdint.h>


/**
 * OProfile Extended Feature Table Entry
 */
struct opd_ext_feature {
	// Feature name
	const char* feature;
	// Feature handlers
	struct opd_ext_handlers * handlers;
};

/**
 * OProfile Extended handlers
 */
struct opd_ext_handlers {
	// Extended init
	int (*ext_init)(char const *);
	// Extended deinit 
	int (*ext_deinit)();
	// Extended statistics
	int (*ext_print_stats)();
	// Extended sfile handlers
	struct opd_ext_sfile_handlers * ext_sfile;
};

/**
 * OProfile Extended sub-handlers (sfile)
 */
struct opd_ext_sfile_handlers {
	int (*create)(struct sfile *);
	int (*dup)(struct sfile *, struct sfile *);
	int (*close)(struct sfile *);
	int (*sync)(struct sfile *);
	odb_t * (*get)(struct transient const *, int);
	struct opd_event * (*find_counter_event)(unsigned long);
};

/**
 * @param value: commandline input option string
 *
 * Parse the specified extended feature
 */
extern int opd_ext_initialize(char const * value);

/**
 * @param value: commandline input option string
 *
 * Deinitialize
 */
extern int opd_ext_deinitialize();

/**
 * Print out extended feature statistics in oprofiled.log file
 */
extern void opd_ext_print_stats();

/**
 * opd_sfile extended sfile handling functions
 */
extern void opd_ext_sfile_create(struct sfile * sf);
extern void opd_ext_sfile_dup (struct sfile * to, struct sfile * from);
extern void opd_ext_sfile_close(struct sfile * sf);
extern void opd_ext_sfile_sync(struct sfile * sf);
extern odb_t * opd_ext_sfile_get(struct transient const * trans, int is_cg);

/**
 * @param counter: counter index
 *
 * Get event struct opd_event from the counter index value.
 */
extern struct opd_event * opd_ext_find_counter_event(unsigned long counter);


#endif
