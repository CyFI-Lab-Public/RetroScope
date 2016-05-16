/*
 * harfbuzz-debug.c
 *
 */
#include "harfbuzz-debug.h"

#define LOG_TAG "Harfbuzz"
#include <utils/Log.h>

void Android_Debug(const char* file, int line,
                   const char* function, const char* format, ...) {
    if (format[0] == '\n' && format[1] == '\0')
        return;
    va_list args;
    va_start(args, format);
    android_vprintLog(ANDROID_LOG_DEBUG, NULL, LOG_TAG, format, args);
    va_end(args);
}



