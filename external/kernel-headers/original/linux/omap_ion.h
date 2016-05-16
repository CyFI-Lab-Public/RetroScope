/*
 * include/linux/omap_ion.h
 *
 * Copyright (C) 2011 Google, Inc.
 *
 * This software is licensed under the terms of the GNU General Public
 * License version 2, as published by the Free Software Foundation, and
 * may be copied, distributed, and modified under those terms.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */

#ifndef _LINUX_OMAP_ION_H
#define _LINUX_OMAP_ION_H

#include <linux/types.h>

/**
 * struct omap_ion_tiler_alloc_data - metadata passed from userspace for allocations
 * @w:		width of the allocation
 * @h:		height of the allocation
 * @fmt:	format of the data (8, 16, 32bit or page)
 * @flags:	flags passed to heap
 * @stride:	stride of the allocation, returned to caller from kernel
 * @handle:	pointer that will be populated with a cookie to use to refer
 *		to this allocation
 *
 * Provided by userspace as an argument to the ioctl
 */
struct omap_ion_tiler_alloc_data {
	size_t w;
	size_t h;
	int fmt;
	unsigned int flags;
	struct ion_handle *handle;
	size_t stride;
	size_t offset;
};

#ifdef __KERNEL__
int omap_ion_tiler_alloc(struct ion_client *client,
			 struct omap_ion_tiler_alloc_data *data);
/* given a handle in the tiler, return a list of tiler pages that back it */
int omap_tiler_pages(struct ion_client *client, struct ion_handle *handle,
		     int *n, u32 ** tiler_pages);
#endif /* __KERNEL__ */

/* additional heaps used only on omap */
enum {
	OMAP_ION_HEAP_TYPE_TILER = ION_HEAP_TYPE_CUSTOM + 1,
};

#define OMAP_ION_HEAP_TILER_MASK (1 << OMAP_ION_HEAP_TYPE_TILER)

enum {
	OMAP_ION_TILER_ALLOC,
};

/**
 * These should match the defines in the tiler driver
 */
enum {
	TILER_PIXEL_FMT_MIN   = 0,
	TILER_PIXEL_FMT_8BIT  = 0,
	TILER_PIXEL_FMT_16BIT = 1,
	TILER_PIXEL_FMT_32BIT = 2,
	TILER_PIXEL_FMT_PAGE  = 3,
	TILER_PIXEL_FMT_MAX   = 3
};

/**
 * List of heaps in the system
 */
enum {
	OMAP_ION_HEAP_LARGE_SURFACES,
	OMAP_ION_HEAP_TILER,
	OMAP_ION_HEAP_SECURE_INPUT,
};

#endif /* _LINUX_ION_H */

