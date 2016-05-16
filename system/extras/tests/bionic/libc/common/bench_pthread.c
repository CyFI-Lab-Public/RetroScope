/*
 * Copyright (C) 2011 The Android Open Source Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

/* This program is used to benchmark various pthread operations
 * Note that we want to be able to build it with GLibc, both on
 *  a Linux host and an Android device. For example, on ARM, one
 * can build it manually with:
 *
 *     arm-linux-none-gnueabi-gcc -static -o bench_pthread_gnueabi \
 *           bench_pthread.c -O2 -lpthread -lrt
 */
#define _GNU_SOURCE 1
#include <time.h>
#include <stdio.h>
#include <stdint.h>
#include <limits.h>
#include <pthread.h>
#include <semaphore.h>
#include <stdlib.h>
#include <unistd.h>

#define S(x)  S_(x)
#define S_(x) #x

#define C(x,y)  C_(x,y)
#define C_(x,y) x ## y

#ifndef PTHREAD_ERRORCHECK_MUTEX_INITIALIZER
#define PTHREAD_ERRORCHECK_MUTEX_INITIALIZER  PTHREAD_ERRORCHECK_MUTEX_INITIALIZER_NP
#endif

#ifndef PTHREAD_RECURSIVE_MUTEX_INITIALIZER
#define PTHREAD_RECURSIVE_MUTEX_INITIALIZER  PTHREAD_RECURSIVE_MUTEX_INITIALIZER_NP
#endif

static int64_t now_ns(void)
{
    struct timespec ts;
    /* NOTE: get thread-specific CPU-time clock to ensure
     *       we don't measure stuff like kernel thread preemptions
     *       that might happen during the benchmark
     */
    clock_gettime(CLOCK_THREAD_CPUTIME_ID,&ts);
    return ts.tv_sec*1000000000LL + ts.tv_nsec;
}

#define SUBCOUNT   10000
#define MAX_STATS  1000000

/* Maximum time we'll wait for a single bench run */
#define MAX_WAIT_MS  1000

static int64_t  stats[MAX_STATS];

static int
compare_stats(const void* a, const void* b)
{
    uint64_t sa = *(const uint64_t*)a;
    uint64_t sb = *(const uint64_t*)b;
    if (sa < sb)
        return -1;
    if (sa > sb)
        return +1;
    else
        return 0;
}

static void
filter_stats(int count, const char* statement)
{
    int64_t  min, max, avg, median;

    /* sort the array in increasing order */
    qsort(stats, count, sizeof(stats[0]), compare_stats);

    /* trim 10% to remove outliers */
    int min_index = count*0.05;
    int max_index = count - min_index;
    if (max_index >= count)
        max_index = count-1;

    count = (max_index - min_index)+1;

    /* the median is the center item */
    median = stats[(min_index+max_index)/2];

    /* the minimum is the first, the max the last */
    min = stats[min_index];
    max = stats[max_index];

    /* compute the average */
    int nn;
    int64_t  total = 0;
    for (nn = min_index; nn <= max_index; nn++) {
        total += stats[nn];
    }

    printf("BENCH: %5.1f %5.1f %5.1f, %s\n",
           min*1./SUBCOUNT,
           max*1./SUBCOUNT,
           median*1./SUBCOUNT,
           statement);
    if (0) {
        for (nn = min_index; nn <= max_index; nn++) {
            printf(" %lld", (long long)stats[nn]);
        }
        printf("\n");
    }
}

#define BENCH_COUNT(stmnt,total) do { \
        int64_t  count = total; \
        int      num_stats = 0; \
        int64_t  bench_start = now_ns(); \
        while (num_stats < MAX_STATS && count >= SUBCOUNT) { \
            int      tries = SUBCOUNT; \
            int64_t  sub_start = now_ns(); \
            count -= tries; \
            for ( ; tries > 0; tries-- ) {\
                stmnt;\
            }\
            int64_t  sub_end = now_ns(); \
            stats[num_stats++] = sub_end - sub_start; \
            if (sub_end - bench_start >= MAX_WAIT_MS*1e6) \
                break; \
        } \
        filter_stats(num_stats, #stmnt); \
    } while (0)

#define DEFAULT_COUNT 10000000

#define BENCH(stmnt) BENCH_COUNT(stmnt,DEFAULT_COUNT)

/* Will be called by pthread_once() for benchmarking */
static void _dummy_init(void)
{
    /* nothing */
}

/* Used when creating the key */
static void key_destroy(void* param)
{
    /* nothing */
}

int main(void)
{
    pthread_once_t  once = PTHREAD_ONCE_INIT;
    pthread_once(&once, _dummy_init);

    pthread_key_t   key;
    pthread_key_create(&key, key_destroy);
    pthread_setspecific(key, (void*)(int)100);

    BENCH(getpid());
    BENCH(pthread_self());
    BENCH(pthread_getspecific(key));
    BENCH(pthread_once(&once, _dummy_init));

    pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
    BENCH(pthread_mutex_lock(&mutex); pthread_mutex_unlock(&mutex));

    pthread_mutex_t errorcheck_mutex = PTHREAD_ERRORCHECK_MUTEX_INITIALIZER;
    BENCH(pthread_mutex_lock(&errorcheck_mutex); pthread_mutex_unlock(&errorcheck_mutex));

    pthread_mutex_t recursive_mutex = PTHREAD_RECURSIVE_MUTEX_INITIALIZER;
    BENCH(pthread_mutex_lock(&recursive_mutex); pthread_mutex_unlock(&recursive_mutex));

	/* TODO: Benchmark pshared mutexes */

    sem_t semaphore;
    int dummy;
    sem_init(&semaphore, 1, 1);
    BENCH(sem_getvalue(&semaphore,&dummy));
    BENCH(sem_wait(&semaphore); sem_post(&semaphore));
    return 0;
}
