#ifndef DALVIK_ZOMBIE_LOG_H_
#define DALVIK_ZOMBIE_LOG_H_

#include <unistd.h>

#define LOG_ON
#ifdef LOG_ON
#  ifdef ALOGE
#    define ZMB_LOG(fmt, ...) ALOGE("[ZMB: %s] "fmt, __PRETTY_FUNCTION__ , ##__VA_ARGS__)
#    define RS_LOG(fmt, ...) ALOGE("[RetroScope: %s] "fmt, __PRETTY_FUNCTION__ , ##__VA_ARGS__)
#  else
#    define ZMB_LOG(fmt, ...) \
      __libc_format_log(ANDROID_LOG_FATAL, "zombie", "[ZMB: %s] "fmt"\n", __PRETTY_FUNCTION__ , ##__VA_ARGS__ )
#    define RS_LOG(fmt, ...) \
      __libc_format_log(ANDROID_LOG_FATAL, "RetroScope", "[RetroScope: %s] "fmt"\n", __PRETTY_FUNCTION__ , ##__VA_ARGS__ )
#  endif
#else
#  define ZMB_LOG(fmt, ...)
#  define RS_LOG(fmt, ...)
#endif

#endif // DALVIK_ZOMBIE_LOG_H_
