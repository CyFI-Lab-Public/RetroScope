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

#include <stdlib.h>
#include "Log.h"
#include "StringUtil.h"
#include "task/TaskAll.h"

TaskAsync::TaskAsync(TaskType type)
    : TaskGeneric(type),
      mVolume(-1),
      mDeviceType(EDeviceHost),
      mMode(AudioHardware::EModeVoice),
      mAsynchronous(false)
{
    // nothing to do
}

TaskAsync::~TaskAsync()
{

}

TaskGeneric::ExecutionResult TaskAsync::run()
{
    // id is mandatory
    if (mId.length() == 0) {
        LOGE(" TaskAsync::run no id attribute");
        return TaskGeneric::EResultError;
    }
    TaskGeneric::ExecutionResult result = start();
    if (result == TaskGeneric::EResultOK) {
        if (!isAsynchronous()) {
            return complete();
        } else {
            if (!getParentSequential()->queueAsyncTask(const_cast<TaskAsync*>(this))) {
                LOGE("TaskAsync::run queueAsyncTask failed");
                return TaskGeneric::EResultError;
            }
        }
    }
    return result;
}

bool TaskAsync::parseAttribute(const android::String8& name, const android::String8& value)
{
    bool result = true;
    if (StringUtil::compare(name, "id") == 0) {
        mId.append(value);
    } else if (StringUtil::compare(name, "gain") == 0) {
        mVolume = atoi(value.string());
        if ((mVolume < 1) || (mVolume > 100)) {
            LOGE("TaskGeneric::parseAttribute gain out of range %d", mVolume);
            return false;
        }
    } else if (StringUtil::compare(name, "sync") == 0) {
        if (StringUtil::compare(value, "start") == 0) { // async
            makeAsynchronous();
        }
    } else if (StringUtil::compare(name, "device") == 0) {
        if (StringUtil::compare(value, "host") == 0) {
            mDeviceType = EDeviceHost;
        } else if (StringUtil::compare(value, "DUT") == 0) {
            mDeviceType = EDeviceDUT;
        } else {
            return false;
        }
    } else if (StringUtil::compare(name, "mode") == 0) {
            if (StringUtil::compare(value, "voice") == 0) {
                mMode = AudioHardware::EModeVoice;
            } else if (StringUtil::compare(value, "music") == 0) {
                mMode = AudioHardware::EModeMusic;
            } else {
                return false;
            }
    } else {
        result = TaskGeneric::parseAttribute(name, value);
    }
    return result;
}

TaskSequential* TaskAsync::getParentSequential()
{
    ASSERT(getParent()->getType() == TaskGeneric::ETaskSequential);
    return reinterpret_cast<TaskSequential*>(getParent());
}

