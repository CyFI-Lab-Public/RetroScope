/*
 * harfbuzz-debug.h
 *
 */

#ifndef HARFBUZZ_DEBUG_H_
#define HARFBUZZ_DEBUG_H_

#define ANDROID_DEBUG 0

#if ANDROID_DEBUG
#define HBDebug(...) Android_Debug(__FILE__, __LINE__, \
                                   __FUNCTION__, __VA_ARGS__)
#else
#define HBDebug
#endif

void Android_Debug(const char* file, int line, const char* function, const char* format, ...)
        __attribute__((format(printf, 4, 5)));  /* 4=format 5=params */;

#endif /* HARFBUZZ_DEBUG_H_ */
