/*
 * Copyright (C) 2011 The Android Open Source Project
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

#ifndef __SHARE_ALLOC_H_
#define __SHARE_ALLOC_H_

#include <limits.h>

// malloc(n floor 1)
static inline void *safe_malloc_(size_t n)
{
    // dlmalloc is already safe
    return malloc(n);
}

// malloc(n1 * n2) then memset to zero
static inline void *safe_calloc_(size_t n1, size_t n2)
{
    // dlcalloc is already safe
    return calloc(n1, n2);
}

// malloc(n1 + n2)
static inline void *safe_malloc_add_2op_(size_t n1, size_t n2)
{
    unsigned long long n = n1 + n2;
    size_t ns = n;
    // check for overflow
    return n == ns ? malloc(ns) : NULL;
}

// malloc(n1 * n2)
static inline void *safe_malloc_mul_2op_(size_t n1, size_t n2)
{
    unsigned long long n = n1 * n2;
    size_t ns = n;
    // check for overflow
    return n == ns ? malloc(ns) : NULL;
}

// malloc(n1 * (n2 + n3))
static inline void *safe_malloc_muladd2_(size_t n1, size_t n2, size_t n3)
{
    unsigned long long n = n1 * (n2 + n3);
    size_t ns = n;
    // check for overflow
    return n == ns ? malloc(ns) : NULL;
}

// realloc(ptr, n1 * n2)
static inline void *safe_realloc_mul_2op_(void *ptr, size_t n1, size_t n2)
{
    unsigned long long n = n1 * n2;
    size_t ns = n;
    // check for overflow
    return n == ns ? realloc(ptr, ns) : NULL;
}

#endif // __SHARE_ALLOC_H_
