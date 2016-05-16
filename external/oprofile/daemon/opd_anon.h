/**
 * @file opd_anon.h
 * Anonymous region handling.
 *
 * @remark Copyright 2005 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 */

#ifndef OPD_ANON_H
#define OPD_ANON_H

#include "op_types.h"
#include "op_list.h"

#include "opd_cookie.h"

#include <sys/types.h>

struct transient;

/**
 * Shift useful bits into play for VMA hashing.
 */
#define VMA_SHIFT 13 

/* Maximum size of the image name considered */
#define MAX_IMAGE_NAME_SIZE 20

struct anon_mapping {
	/** start of the mapping */
	vma_t start;
	/** end of the mapping */
	vma_t end;
	/** tgid of the app */
	pid_t tgid;
	/** cookie of the app */
	cookie_t app_cookie;
	/** hash list */
	struct list_head list;
	/** lru list */
	struct list_head lru_list;
	char name[MAX_IMAGE_NAME_SIZE+1];
};

/**
 * Try to find an anonymous mapping for the given pc/tgid pair.
 */
struct anon_mapping * find_anon_mapping(struct transient *);

void anon_init(void);

#endif /* OPD_ANON_H */
