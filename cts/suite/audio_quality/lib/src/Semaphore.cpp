/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

#include "Semaphore.h"


Semaphore::Semaphore(int count)
{
    if (sem_init(&mSem, 0, count) != 0) {
        ASSERT(false);
    }
}

Semaphore::~Semaphore()
{
    sem_destroy(&mSem);
}

void Semaphore::tryWait()
{
    sem_trywait(&mSem);
}

bool Semaphore::wait()
{
    if (sem_wait(&mSem) == 0) {
        return true;
    } else {
        return false;
    }
}

bool Semaphore::timedWait(int timeInMSec)
{
    const int ONE_SEC_IN_NANOSEC = 1000000000;
    const int ONE_MSEC_IN_NANOSEC = 1000000;
    const int ONE_SEC_IN_MSEC = 1000;
    struct timespec timeOld;
    if (clock_gettime(CLOCK_REALTIME, &timeOld) != 0) {
            return false;
    }
    int secToGo = timeInMSec / ONE_SEC_IN_MSEC;
    int msecToGo = timeInMSec - (ONE_SEC_IN_MSEC * secToGo);
    int nanoSecToGo = ONE_MSEC_IN_NANOSEC * msecToGo;
    struct timespec timeNew = timeOld;
    int nanoTotal = timeOld.tv_nsec + nanoSecToGo;
    //LOGI("secToGo %d, msecToGo %d, nanoTotal %d", secToGo, msecToGo, nanoTotal);
    if (nanoTotal > ONE_SEC_IN_NANOSEC) {
        nanoTotal -= ONE_SEC_IN_NANOSEC;
        secToGo += 1;
    }
    timeNew.tv_sec += secToGo;
    timeNew.tv_nsec = nanoTotal;
    //LOGV("Semaphore::timedWait now %d-%d until %d-%d for %d msecs",
    //        timeOld.tv_sec, timeOld.tv_nsec, timeNew.tv_sec, timeNew.tv_nsec, timeInMSec);
    if (sem_timedwait(&mSem, &timeNew) == 0) {
        return true;
    } else {
        return false;
    }
}

void Semaphore::post()
{
    sem_post(&mSem);
}
