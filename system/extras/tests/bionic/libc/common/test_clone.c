/*
 * Copyright (C) 2010 The Android Open Source Project
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
/* Check that clone() is implemented and properly works
 */
#define _GNU_SOURCE 1
#include <stdio.h>
#include <errno.h>
#include <sched.h>
#include <unistd.h>
#include <signal.h>
#include <stdlib.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <stdarg.h>
#include <string.h>

static int
clone_child (void *arg)
{
 errno = 0;
 ptrace (PTRACE_TRACEME, 0, 0, 0);
 if (errno != 0)
   perror ("ptrace");
 if (kill (getpid (), SIGSTOP) < 0)
   perror ("kill");
 return 0;
}

#define PAGE_SIZE 4096
#define STACK_SIZE (4 * PAGE_SIZE)

char clone_stack[STACK_SIZE] __attribute__ ((aligned (PAGE_SIZE)));

int
main ()
{
 int pid,child;
 int status;

 pid = clone (clone_child, clone_stack + 3 * PAGE_SIZE,
              CLONE_VM | SIGCHLD, NULL);
 if (pid < 0)
   {
     perror ("clone");
     exit (1);
   }
 printf ("child pid %d\n", pid);

 //sleep(20);
 child = waitpid (pid, &status, 0);
 printf("waitpid returned %d\n", child);
 if (child < 0) {
   perror ("waitpid");
   return 1;
 }
 printf ("child %d, status 0x%x\n", child, status);
 return 0;
}
