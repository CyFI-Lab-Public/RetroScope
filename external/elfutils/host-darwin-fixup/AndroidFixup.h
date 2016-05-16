/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ANDROID_FIXUP_H
#define ANDROID_FIXUP_H

#define loff_t off_t
#define off64_t off_t

#include <string.h>
#include <stdarg.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <locale.h> //LC_MESSAGES

#ifndef TEMP_FAILURE_RETRY
#define TEMP_FAILURE_RETRY(exp) ({         \
    typeof (exp) _rc;                      \
    do {                                   \
        _rc = (exp);                       \
    } while (_rc == -1 && errno == EINTR); \
    _rc; })
#endif

#if __MAC_OS_X_VERSION_MIN_REQUIRED < 1070
static inline size_t strnlen (const char *__string, size_t __maxlen)
{
        int len = 0;
        while (__maxlen-- && *__string++)
                len++;
        return len;
}
#endif

static inline void *mempcpy (void * __dest, const void * __src, size_t __n)
{
        memcpy(__dest, __src, __n);
        return ((char *)__dest) + __n;
}

#define __mempcpy mempcpy

#define dgettext(domainname, msgid)  dcgettext (domainname, msgid, LC_MESSAGES)

static inline void __attribute__((noreturn)) error(int status, int errnum, const char *fmt, ...)
{
        va_list lst;
        va_start(lst, fmt);
        vfprintf(stderr, fmt, lst);
        fprintf(stderr, "error %d: %s\n", errnum, strerror(errno));
        va_end(lst);
        exit(status);
}

static inline char *dcgettext (char *__domainname, char *__msgid, int __category)
{
        error(EXIT_FAILURE, 0, "%s not implemented!", __FUNCTION__);
        return NULL;
}

/* workaround for canonicalize_file_name */
#define canonicalize_file_name(path) realpath(path, NULL)

/* workaround for open64 */
#define open64(path, flags)     open(path, flags)

/* rawmemchr */
static inline void *rawmemchr(const void *s, int c)
{
    const unsigned char *ptr = s;
    while (1) {
        if (*ptr == c) return (void *) ptr;
        ptr++;
    }
}

#define strndup(str, size) strdup(str)

static void tdestroy(void *root, void (*free_node)(void *nodep))
{
}

#endif /* ANDROID_FIXUP_H */
