/*
* Copyright (C) 2011 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

#include <stdio.h>
#include "ThreadInfo.h"

//#define TRACE_THREADINFO
#ifdef TRACE_THREADINFO
#define LOG_THREADINFO(x...) fprintf(stderr, x)
#else
#define LOG_THREADINFO(x...)
#endif

void ThreadInfo::updateInfo(ContextPtr eglCtx,
                            EglDisplay* dpy,
                            GLEScontext* glesCtx,
                            ShareGroupPtr share,
                            ObjectNameManager* manager) {

    eglContext  = eglCtx;
    eglDisplay  = dpy;
    glesContext = glesCtx;
    shareGroup  = share;
    objManager  = manager;
}

#include <cutils/threads.h>
static thread_store_t s_tls = THREAD_STORE_INITIALIZER;
static int active_instance = 0;
static void tlsDestruct(void *ptr)
{
    active_instance--;
    LOG_THREADINFO("tlsDestruct EGL %lx %d\n", (long)ptr, active_instance);
    if (ptr) {
        ThreadInfo *ti = (ThreadInfo *)ptr;
        delete ti;
    }
}

ThreadInfo *getThreadInfo()
{
    ThreadInfo *ti = (ThreadInfo *)thread_store_get(&s_tls);
    if (!ti) {
        ti = new ThreadInfo();
        thread_store_set(&s_tls, ti, tlsDestruct);
        active_instance++;
        LOG_THREADINFO("getThreadInfo EGL %lx %d\n", (long)ti, active_instance);
    }
    return ti;
}
