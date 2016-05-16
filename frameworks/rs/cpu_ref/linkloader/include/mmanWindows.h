/*
 * Copyright 2013, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef MMANWINDOWS_H
#define MMANWINDOWS_H

#ifdef _WIN32

#include <stdlib.h>
#include <sys/types.h>

// From bionic/libc/include/sys/mman.h
#ifndef MAP_ANON
#define MAP_ANON  MAP_ANONYMOUS
#endif

#define MAP_FAILED ((void *)-1)

#define MREMAP_MAYMOVE  1
#define MREMAP_FIXED    2

extern void*  mmap(void *, size_t, int, int, int, off_t);
extern int    munmap(void *, size_t);
extern int    msync(const void *, size_t, int);
extern int    mprotect(const void *, size_t, int);
extern void*  mremap(void *, size_t, size_t, unsigned long);

extern int    mlockall(int);
extern int    munlockall(void);
extern int    mlock(const void *, size_t);
extern int    munlock(const void *, size_t);
extern int    madvise(const void *, size_t, int);

extern int    mlock(const void *addr, size_t len);
extern int    munlock(const void *addr, size_t len);

extern int    mincore(void*  start, size_t  length, unsigned char*  vec);

// From bionic/libc/kernel/common/asm-generic/mman-common.h
#define PROT_READ 0x1
#define PROT_WRITE 0x2
#define PROT_EXEC 0x4
#define PROT_SEM 0x8
#define PROT_NONE 0x0

#define MAP_PRIVATE 0x02
#define MAP_ANONYMOUS 0x20

#endif  // _WIN32

#endif  // MMANWINDOWS_H
