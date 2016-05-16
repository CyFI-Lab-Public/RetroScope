/*
 * Copyright (C) 2013 The Android Open Source Project
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

#ifndef ANDROID_EVENTCONTROLTHREAD_H
#define ANDROID_EVENTCONTROLTHREAD_H

#include <stddef.h>

#include <utils/Mutex.h>
#include <utils/Thread.h>

namespace android {

class SurfaceFlinger;

class EventControlThread: public Thread {
public:

    EventControlThread(const sp<SurfaceFlinger>& flinger);
    virtual ~EventControlThread() {}

    void setVsyncEnabled(bool enabled);
    virtual bool threadLoop();

private:
    sp<SurfaceFlinger> mFlinger;
    bool mVsyncEnabled;

    Mutex mMutex;
    Condition mCond;
};

}

#endif // ANDROID_DISPSYNC_H
