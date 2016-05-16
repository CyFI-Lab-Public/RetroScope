/**
 * @file compat.c
 * This file is intended to be up-to-date with the last linux version and
 * provide work-arounds for missing features in previous kernel version
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include "op_dcache.h"
#include <linux/ioport.h>

#ifdef NEED_2_2_DENTRIES

/* note - assumes you only test for NULL, and not
 * actually care about the return value */
void * compat_request_region(unsigned long start, unsigned long n, char const * name)
{
	if (check_region(start, n) != 0)
		return NULL;
	request_region(start, n, name);
        return (void *)1;
}

int wind_dentries_2_2(struct dentry * dentry)
{
	struct dentry * root = current->fs->root;

	if (dentry->d_parent != dentry && list_empty(&dentry->d_hash))
		return 0;

	for (;;) {
		struct dentry * parent;

		if (dentry == root)
			break;

		dentry = dentry->d_covers;
		parent = dentry->d_parent;

		if (dentry == parent)
			break;

		push_dname(&dentry->d_name);

		dentry = parent;
	}

	return 1;
}

/* called with note_lock held */
uint do_path_hash_2_2(struct dentry * dentry)
{
	/* BKL is already taken */

	return do_hash(dentry, 0, 0, 0);
}

#endif /* NEED_2_2_DENTRIES */
