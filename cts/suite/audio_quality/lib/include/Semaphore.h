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


#ifndef CTSAUDIO_SEMAPHORE_H
#define CTSAUDIO_SEMAPHORE_H

#include <semaphore.h>

#include <Log.h>

/**
 * Simple semaphore interface for synchronization between client and server
 */
class Semaphore {
public:
    Semaphore(int count = 0);

    ~Semaphore();

    /// down semaphore if it is already positive.
    void tryWait();

    bool wait();

    bool timedWait(int timeInMSec);

    void post();

private:
    sem_t mSem;
};


#endif // CTSAUDIO_SEMAPHORE_H
