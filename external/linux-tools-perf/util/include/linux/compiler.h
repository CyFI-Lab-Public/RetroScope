#ifndef _PERF_LINUX_COMPILER_H_
#define _PERF_LINUX_COMPILER_H_

#ifndef __always_inline
#define __always_inline	inline
#endif
#define __user
#define __attribute_const__

/* ANDROID_CHANGE_BEGIN */
#ifndef __BIONIC__
#define __used		__attribute__((__unused__))
#endif
/* ANDROID_CHANGE_END */

#endif
