/**
 * @file opd_cookie.c
 * cookie -> name cache
 *
 * @remark Copyright 2002, 2005 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 */

#include "opd_cookie.h"
#include "oprofiled.h"
#include "op_list.h"
#include "op_libiberty.h"

#include <sys/syscall.h>
#include <unistd.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>

#ifndef __NR_lookup_dcookie
#if defined(__i386__)
#define __NR_lookup_dcookie 253
#elif defined(__x86_64__)
#define __NR_lookup_dcookie 212
#elif defined(__powerpc__)
#define __NR_lookup_dcookie 235
#elif defined(__alpha__)
#define __NR_lookup_dcookie 406
#elif defined(__hppa__)
#define __NR_lookup_dcookie 223
#elif defined(__ia64__)
#define __NR_lookup_dcookie 1237
#elif defined(__sparc__)
/* untested */
#define __NR_lookup_dcookie 208
#elif defined(__s390__) || defined (__s390x__)
#define __NR_lookup_dcookie 110
#elif defined(__arm__)
#define __NR_lookup_dcookie (__NR_SYSCALL_BASE+249)
#elif defined(__mips__)
#include <sgidefs.h>
/* O32 */
#if _MIPS_SIM == _MIPS_SIM_ABI32
#define __NR_lookup_dcookie 4247
/* N64 */
#elif _MIPS_SIM == _MIPS_SIM_ABI64
#define __NR_lookup_dcookie 5206
/* N32 */
#elif _MIPS_SIM == _MIPS_SIM_NABI32
#define __NR_lookup_dcookie 6206
#else
#error Unknown MIPS ABI: Dunno __NR_lookup_dcookie
#endif
#else
#error Please define __NR_lookup_dcookie for your architecture
#endif
#endif /* __NR_lookup_dcookie */

#if (defined(__powerpc__) && !defined(__powerpc64__)) || defined(__hppa__)\
	|| (defined(__s390__) && !defined(__s390x__)) \
	|| (defined(__mips__) && (_MIPS_SIM == _MIPS_SIM_ABI32) \
	    && defined(__MIPSEB__)) \
        || (defined(__arm__) && defined(__ARM_EABI__) \
            && defined(__ARMEB__))
static inline int lookup_dcookie(cookie_t cookie, char * buf, size_t size)
{
	return syscall(__NR_lookup_dcookie, (unsigned long)(cookie >> 32),
		       (unsigned long)(cookie & 0xffffffff), buf, size);
}
#elif (defined(__mips__) && (_MIPS_SIM == _MIPS_SIM_ABI32)) \
	|| (defined(__arm__) && defined(__ARM_EABI__))
static inline int lookup_dcookie(cookie_t cookie, char * buf, size_t size)
{
	return syscall(__NR_lookup_dcookie,
		       (unsigned long)(cookie & 0xffffffff),
		       (unsigned long)(cookie >> 32), buf, size);
}
#else
static inline int lookup_dcookie(cookie_t cookie, char * buf, size_t size)
{
	return syscall(__NR_lookup_dcookie, cookie, buf, size);
}
#endif


struct cookie_entry {
	cookie_t value;
	char * name;
	int ignored;
	struct list_head list;
};


#define HASH_SIZE 512
#define HASH_BITS (HASH_SIZE - 1)

static struct list_head hashes[HASH_SIZE];

static struct cookie_entry * create_cookie(cookie_t cookie)
{
	int err;
	struct cookie_entry * entry = xmalloc(sizeof(struct cookie_entry));

	entry->value = cookie;
	entry->name = xmalloc(PATH_MAX + 1);

	err = lookup_dcookie(cookie, entry->name, PATH_MAX);

	if (err < 0) {
		fprintf(stderr, "Lookup of cookie %llx failed, errno=%d\n",
		       cookie, errno); 
		free(entry->name);
		entry->name = NULL;
		entry->ignored = 0;
	} else {
		entry->ignored = is_image_ignored(entry->name);
	}

	return entry;
}


/* Cookie monster want cookie! */
static unsigned long hash_cookie(cookie_t cookie)
{
	return (cookie >> DCOOKIE_SHIFT) & (HASH_SIZE - 1);
}
 

char const * find_cookie(cookie_t cookie)
{
	unsigned long hash = hash_cookie(cookie);
	struct list_head * pos;
	struct cookie_entry * entry;

	if (cookie == INVALID_COOKIE || cookie == NO_COOKIE)
		return NULL;

	list_for_each(pos, &hashes[hash]) {
		entry = list_entry(pos, struct cookie_entry, list);
		if (entry->value == cookie)
			goto out;
	}

	/* not sure this can ever happen due to is_cookie_ignored */
	entry = create_cookie(cookie);
	list_add(&entry->list, &hashes[hash]);
out:
	return entry->name;
}


int is_cookie_ignored(cookie_t cookie)
{
	unsigned long hash = hash_cookie(cookie);
	struct list_head * pos;
	struct cookie_entry * entry;

	if (cookie == INVALID_COOKIE || cookie == NO_COOKIE)
		return 1;

	list_for_each(pos, &hashes[hash]) {
		entry = list_entry(pos, struct cookie_entry, list);
		if (entry->value == cookie)
			goto out;
	}

	entry = create_cookie(cookie);
	list_add(&entry->list, &hashes[hash]);
out:
	return entry->ignored;
}


char const * verbose_cookie(cookie_t cookie)
{
	unsigned long hash = hash_cookie(cookie);
	struct list_head * pos;
	struct cookie_entry * entry;

	if (cookie == INVALID_COOKIE)
		return "invalid";

	if (cookie == NO_COOKIE)
		return "anonymous";

	list_for_each(pos, &hashes[hash]) {
		entry = list_entry(pos, struct cookie_entry, list);
		if (entry->value == cookie) {
			if (!entry->name)
				return "failed lookup";
			return entry->name;
		}
	}

	return "not hashed";
}


void cookie_init(void)
{
	size_t i;

	for (i = 0; i < HASH_SIZE; ++i)
		list_init(&hashes[i]);
}
