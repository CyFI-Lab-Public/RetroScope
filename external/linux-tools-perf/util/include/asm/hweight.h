#ifndef PERF_HWEIGHT_H
#define PERF_HWEIGHT_H

/* ANDROID_CHANGE_BEGIN */
#ifndef __APPLE__
/* Suppress kernel-name space pollution in <linux/types.h> below */
#include <features.h>
#include <linux/types.h>
#endif
/* ANDROID_CHANGE_END */
unsigned int hweight32(unsigned int w);
unsigned long hweight64(__u64 w);

#endif /* PERF_HWEIGHT_H */
