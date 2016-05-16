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
#include <stdio.h>
#include <time.h>
#include <signal.h>
#include <pthread.h>
#include <string.h>
#include <unistd.h>

void
handle(sigval_t v)
{
    time_t  t;
    char    p[32];

    time(&t);
    strftime(p, sizeof(p), "%T", localtime(&t));
    printf("%s thread %d, val = %d, signal captured.\n", 
            p,  (int)pthread_self(), v.sival_int);
    return;
}

int
create(int seconds, int id)
{
    timer_t tid;
    struct sigevent se;
    struct itimerspec ts, ots;

    memset(&se, 0, sizeof (se));
    se.sigev_notify = SIGEV_THREAD;
    se.sigev_notify_function = handle;
    se.sigev_value.sival_int = id;

    if (timer_create (CLOCK_REALTIME, &se, &tid) < 0)
    {
        perror ("timer_creat");
        return -1;
    }
    puts ("timer_create successfully.");
    ts.it_value.tv_sec = 3;
    ts.it_value.tv_nsec = 0;
    ts.it_interval.tv_sec = seconds;
    ts.it_interval.tv_nsec = 0;
    if (timer_settime (tid, TIMER_ABSTIME, &ts, &ots) < 0)
    {
        perror ("timer_settime");
        return -1;
    }
    return 0;
}

int
main (void)
{
    if (create (3, 1) < 0) return 1;
    if (create (5, 2) < 0) return 1;
    for (;;)
    {
        sleep (10);
    }
    return 0;
}
