/**
 * @file op_cache.h
 * Cache definitions for compatibility
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef OP_CACHE_H
#define OP_CACHE_H

#include <asm/cache.h>

#ifndef L1_CACHE_ALIGN
#define L1_CACHE_ALIGN(x) (((x)+(L1_CACHE_BYTES-1))&~(L1_CACHE_BYTES-1))
#endif

#ifndef SMP_CACHE_BYTES
#define SMP_CACHE_BYTES L1_CACHE_BYTES
#endif

/* 2.4.0 introduced __cacheline_aligned */
#ifndef ____cacheline_aligned
#define ____cacheline_aligned __attribute__((__aligned__(SMP_CACHE_BYTES)))
#endif

#ifndef __cacheline_aligned
#ifdef MODULE
#define __cacheline_aligned ____cacheline_aligned
#else
#define __cacheline_aligned					\
	__attribute__((__aligned__(SMP_CACHE_BYTES), 		\
		 __section__(".data.cacheline_aligned")))
#endif
#endif /* __cacheline_aligned */

/* 2.4.10 introduced ___cacheline_aligned_in_smp */
#ifndef ____cacheline_aligned_in_smp
#ifdef CONFIG_SMP
#define ____cacheline_aligned_in_smp ____cacheline_aligned
#else
#define ____cacheline_aligned_in_smp
#endif /* CONFIG_SMP */
#endif

#ifndef __cacheline_aligned_in_smp
#ifdef CONFIG_SMP
#define __cacheline_aligned_in_smp __cacheline_aligned
#else
#define __cacheline_aligned_in_smp
#endif /* CONFIG_SMP */
#endif

#endif /* !OP_CACHE_H */
