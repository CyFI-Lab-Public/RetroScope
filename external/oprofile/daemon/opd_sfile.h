/**
 * @file daemon/opd_sfile.h
 * Management of sample files
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OPD_SFILE_H
#define OPD_SFILE_H

#include "opd_cookie.h"

#include "odb.h"
#include "op_hw_config.h"
#include "op_types.h"
#include "op_list.h"

#include <sys/types.h>

struct kernel_image;
struct transient;

#define CG_HASH_SIZE 16
#define UNUSED_EMBEDDED_OFFSET ~0LLU

/**
 * Each set of sample files (where a set is over the physical counter
 * types) will have one of these for it. We match against the
 * descriptions here to find which sample DB file we need to modify.
 *
 * cg files are stored in the hash.
 */
struct sfile {
	/** hash value for this sfile */
	unsigned long hashval;
	/** cookie value for the binary profiled */
	cookie_t cookie;
	/** cookie value for the application owner, INVALID_COOKIE if not set */
	cookie_t app_cookie;
	/** thread ID, -1 if not set */
	pid_t tid;
	/** thread group ID, -1 if not set */
	pid_t tgid;
	/** CPU number */
	unsigned int cpu;
	/** kernel image if applicable */
	struct kernel_image * kernel;
	/** anonymous mapping */
	struct anon_mapping * anon;
	/** embedded offset for Cell BE SPU */
	uint64_t embedded_offset;

	/** hash table link */
	struct list_head hash;
	/** lru list */
	struct list_head lru;
	/** true if this file should be ignored in profiles */
	int ignored;
	/** opened sample files */
	odb_t files[OP_MAX_COUNTERS];
	/** extended sample files */
	odb_t * ext_files;
	/** hash table of opened cg sample files */
	struct list_head cg_hash[CG_HASH_SIZE];
};

/** a call-graph entry */
struct cg_entry {
	/** where arc is to */
	struct sfile to;
	/** next in the hash slot */
	struct list_head hash;
};

/** clear any sfiles that are for the kernel */
void sfile_clear_kernel(void);

struct anon_mapping;

/** clear any sfiles for the given anon mapping */
void sfile_clear_anon(struct anon_mapping *);

/** sync sample files */
void sfile_sync_files(void);

/** close sample files */
void sfile_close_files(void);

/** clear out a certain amount of LRU entries
 * return non-zero if the lru is already empty */
int sfile_lru_clear(void);

/** remove a sfile from the lru list, protecting it from sfile_lru_clear() */
void sfile_get(struct sfile * sf);

/** add this sfile to lru list */
void sfile_put(struct sfile * sf);

/**
 * Find the sfile for the current parameters. Note that is required
 * that the PC value be set appropriately (needed for kernel images)
 */
struct sfile * sfile_find(struct transient const * trans);

/** Log the sample in a previously located sfile. */
void sfile_log_sample(struct transient const * trans);

/** Log the event/cycle count in a previously located sfile */
void sfile_log_sample_count(struct transient const * trans,
                            unsigned long int count);

/** initialise hashes */
void sfile_init(void);

#endif /* OPD_SFILE_H */
