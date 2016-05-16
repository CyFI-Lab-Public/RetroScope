/**
 * @file daemon/opd_kernel.h
 * Dealing with the kernel and kernel module images
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 * Modified by Aravind Menon for Xen
 * These modifications are:
 * Copyright (C) 2005 Hewlett-Packard Co.
 */

#ifndef OPD_KERNEL_H
#define OPD_KERNEL_H

#include "op_types.h"
#include "op_list.h"

struct transient;

/** create the kernel image */
void opd_create_vmlinux(char const * name, char const * arg);

void opd_create_xen(char const * name, char const * arg);

/** opd_reread_module_info - parse /proc/modules for kernel modules */
void opd_reread_module_info(void);

/** Describes a kernel module or vmlinux itself */
struct kernel_image {
	char * name;
	vma_t start;
	vma_t end;
	struct list_head list;
};

/** Find a kernel_image based upon the given parameters in trans. */
struct kernel_image *
find_kernel_image(struct transient const * trans);

#endif /* OPD_KERNEL_H */
