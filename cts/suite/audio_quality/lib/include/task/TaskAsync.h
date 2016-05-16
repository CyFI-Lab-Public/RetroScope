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


#ifndef CTSAUDIO_TASKASYNC_H
#define CTSAUDIO_TASKASYNC_H

#include "audio/AudioHardware.h"
#include "TaskGeneric.h"

class TaskSequential;
/**
 * Common parent class for TaskInput and TaskOutput
 */
class TaskAsync: public TaskGeneric {
public:
    TaskAsync(TaskType type);
    virtual ~TaskAsync();
    virtual TaskGeneric::ExecutionResult run();
    virtual bool parseAttribute(const android::String8& name, const android::String8& value);
    virtual TaskGeneric::ExecutionResult start() = 0;
    virtual TaskGeneric::ExecutionResult complete() = 0;

    bool isAsynchronous() {
        return mAsynchronous;
    }

private:
    void makeAsynchronous() {
        mAsynchronous = true;
    }
    TaskSequential* getParentSequential();

protected:
    android::String8 mId;
    int mVolume;

    enum DeviceType {
        EDeviceHost,
        EDeviceDUT
    };

    DeviceType mDeviceType;

    AudioHardware::AudioMode mMode;

private:
    bool mAsynchronous;

};



#endif // CTSAUDIO_TASKASYNC_H
