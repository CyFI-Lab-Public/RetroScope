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

/* This program tries to benchmark stdio operations like fread() and
 * fwrite() with various chunk sizes. We always read/write from /dev/zero
 * to ensure that disk speed and caching don't change our results.
 *
 * We really do this to measure improvements in the low-level stdio
 * features.
 */
 
#include <stdio.h>
#include <time.h>
#include <string.h>
#include <errno.h>

static char buffer[1024*1024];

/* Return current time in milli-seconds, as a double */
static double
now_ms(void)
{
    struct timespec  ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return ts.tv_sec*1000. + ts.tv_nsec*1e-6;
}

void read_file(FILE* fp, int chunkSize)
{
    int totalSize = sizeof(buffer);
    for ( ; totalSize > 0; totalSize -= chunkSize) {
        fread(buffer, 1, chunkSize, fp);
    }
}

void write_file(FILE* fp, int chunkSize)
{
    int totalSize = sizeof(buffer);
    for ( ; totalSize > 0; totalSize -= chunkSize) {
        fwrite(buffer, 1, chunkSize, fp);
    }
}

#define  BENCH(op,...) \
    do { \
	double  time_ms = now_ms(); \
	op ; \
	time_ms = now_ms() - time_ms; \
	double  bandwidth = sizeof(buffer)*1000./1024./time_ms; \
	printf("bench %-30s %8.2f ms  (%.1f KB/s) \n", #op, time_ms, bandwidth ); \
    } while (0)

int main(void)
{
    FILE* fp = fopen("/dev/zero", "rw");

    if (fp == NULL) {
	fprintf(stderr,"Could not open /dev/zero: %s\n", strerror(errno));
	return 1;
    }

    BENCH(read_file(fp,1));
    BENCH(read_file(fp,2));
    BENCH(read_file(fp,3));
    BENCH(read_file(fp,4));
    BENCH(read_file(fp,8));
    BENCH(read_file(fp,16));
    BENCH(read_file(fp,32));
    BENCH(read_file(fp,64));
    BENCH(read_file(fp,256));
    BENCH(read_file(fp,1024));
    BENCH(read_file(fp,4096));
    BENCH(read_file(fp,16384));
    BENCH(read_file(fp,65536));

    BENCH(write_file(fp,1));
    BENCH(write_file(fp,2));
    BENCH(write_file(fp,3));
    BENCH(write_file(fp,4));
    BENCH(write_file(fp,8));
    BENCH(write_file(fp,16));
    BENCH(write_file(fp,32));
    BENCH(write_file(fp,64));
    BENCH(write_file(fp,256));
    BENCH(write_file(fp,1024));
    BENCH(write_file(fp,4096));
    BENCH(write_file(fp,16384));
    BENCH(write_file(fp,65536));

    fclose(fp);
    return 0;
}
