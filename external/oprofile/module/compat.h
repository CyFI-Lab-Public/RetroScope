/**
 * @file compat.h
 * This file is intended to be up-to-date with the last linux version and
 * provide work-arounds for missing features in previous kernel version
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */


#ifndef COMPAT_H
#define COMPAT_H

#include <linux/version.h>
#include <linux/module.h>
#ifdef HAVE_LINUX_SPINLOCK_HEADER
/* FIXME do we need this still ? */
#include <linux/spinlock.h>
#endif

#define V_BEFORE(a, b, c) (LINUX_VERSION_CODE < KERNEL_VERSION(a, b, c))
#define V_AT_LEAST(a, b, c) (LINUX_VERSION_CODE >= KERNEL_VERSION(a, b, c))

#if V_BEFORE(2, 4, 0)
	#include "compat22.h"
#else
	#include "compat24.h"
#endif

#include "op_cache.h"

/* Things that cannot rely on a particular linux version or are needed between
 * major release */

#ifndef BUG_ON
#define BUG_ON(p) do { if (p) BUG(); } while (0)
#endif

#ifndef MODULE_LICENSE
#define MODULE_LICENSE(x)
#endif

/* Compiler work-around */

/* branch prediction */
#ifndef likely
	#ifdef EXPECT_OK
		#define likely(a) __builtin_expect((a), 1)
	#else
		#define likely(a) (a)
	#endif
#endif
#ifndef unlikely
	#ifdef EXPECT_OK
		#define unlikely(a) __builtin_expect((a), 0)
	#else
		#define unlikely(a) (a)
	#endif
#endif

#ifndef CONFIG_X86_64
#define VMALLOC_32(sz) vmalloc_32(sz)
#else /* CONFIG_X86_64 */
#define VMALLOC_32(sz) vmalloc(sz)
#endif /* CONFIG_X86_64 */

#endif /* COMPAT_H */
