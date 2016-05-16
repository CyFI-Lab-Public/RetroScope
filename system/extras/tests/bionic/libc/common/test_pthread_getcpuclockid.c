/*
 * Copyright (C) 2008 The Android Open Source Project
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
/* this is a small test for pthread_getcpuclockid() and clock_gettime() */

#include <time.h>
#include <pthread.h>
#include <stdio.h>
#include <errno.h>
#include <string.h>

static pthread_mutex_t  lock = PTHREAD_MUTEX_INITIALIZER;

static void*
thread_func( void*  arg )
{
    pthread_t         self = pthread_self();
    struct timespec   ts;
    clockid_t         clock;
    int               e;

    pthread_mutex_lock( &lock );

    e = pthread_getcpuclockid( self, &clock );
    if (e != 0) {
        fprintf(stderr, "pthread_getcpuclockid(%08lx,) returned error %d: %s\n", self, e, strerror(e));
        pthread_mutex_unlock( &lock );
        return NULL;
    }

    ts.tv_sec  = 0;
    ts.tv_nsec = 300000000 + ((int)arg)*50000000;
    nanosleep( &ts, &ts );

    clock_gettime( clock, &ts );
    fprintf(stderr, "thread %08lx: clock_gettime() returned %g nsecs\n", self, ts.tv_sec*1e9 + ts.tv_nsec);

    pthread_mutex_unlock( &lock );

    return NULL;
}

#define  MAX_THREADS   16

int  main( void )
{
    int             nn;
    pthread_attr_t  attr;
    pthread_t       threads[MAX_THREADS];

    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

    for (nn = 0; nn < MAX_THREADS; nn++) {
        pthread_create( &threads[nn], &attr, thread_func, (void*)nn );
    }
    for (nn = 0; nn < MAX_THREADS; nn++) {
        void*  dummy;
        pthread_join( threads[nn], &dummy );
    }
    return 0;
}
