/*
 * Copyright 2010, The Android Open Source Project
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef ANDROID_CHROMIUM_PREFIX_H
#define ANDROID_CHROMIUM_PREFIX_H

// C++ specific changes
#ifdef __cplusplus
#include <unistd.h>
#include <sys/prctl.h>
// chromium refers to stl functions without std::
#include <algorithm>
using std::find;
using std::reverse;
using std::search;

// Called by command_line.cc to shorten the process name. Not needed for
// network stack.
#define prctl() (0)

namespace std {
// our new does not trigger oom
inline void set_new_handler(void (*p)()) {}
}

// Chromium expects size_t to be a signed int on linux but Android defines it
// as unsigned.
inline size_t abs(size_t x) { return x; }
#endif

// Needed by base_paths.cc for close() function.
#include <unistd.h>
// Need to define assert before logging.h undefines it.
#include <assert.h>
// logging.cc needs pthread_mutex_t
#include <pthread.h>
// needed for isalpha
#include <ctype.h>
// needed for sockaddr_in
#include <netinet/in.h>

// Implemented in bionic but not exposed.
extern char* mkdtemp(char* path);

#define FRIEND_TEST(test_case_name, test_name)\
friend class test_case_name##_##test_name##_Test

// This will probably need a real implementation.
//#define F_ULOCK 0
//#define F_LOCK 1
//inline int lockf(int fd, int cmd, off_t len) { return -1; }

// Disable langinfo in icu
#define U_GAVE_NL_LANGINFO_CODESET 0

// We use the C99 style, but this is not defined
#define HAVE_UINT16_T 1

#endif
