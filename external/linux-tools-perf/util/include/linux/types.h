#ifndef _PERF_LINUX_TYPES_H_
#define _PERF_LINUX_TYPES_H_

/* ANDROID_CHANGE_BEGIN */
#ifndef __APPLE__
#include <asm/types.h>
#endif
/* ANDROID_CHANGE_END */

#define DECLARE_BITMAP(name,bits) \
	unsigned long name[BITS_TO_LONGS(bits)]

struct list_head {
	struct list_head *next, *prev;
};

struct hlist_head {
	struct hlist_node *first;
};

struct hlist_node {
	struct hlist_node *next, **pprev;
};

#endif
