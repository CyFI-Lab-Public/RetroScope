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

#include "task/TaskCase.h"
#include "StringUtil.h"
#include "task/TaskOutput.h"
#include "audio/AudioRemote.h"
#include "audio/RemoteAudio.h"


TaskOutput::TaskOutput()
    : TaskAsync(TaskGeneric::ETaskOutput),
      mWaitForCompletion(false)
{

}

TaskOutput::~TaskOutput()
{

}
bool TaskOutput::parseAttribute(const android::String8& name, const android::String8& value)
{
    if (StringUtil::compare(name, "waitforcompletion") == 0) {
        if (StringUtil::compare(value, "1") == 0) {
            mWaitForCompletion = true;
        }
        return true;
    }
    return TaskAsync::parseAttribute(name, value);
}
TaskGeneric::ExecutionResult TaskOutput::start()
{
    bool localDevice = (mDeviceType == TaskAsync::EDeviceHost);
    android::sp<AudioHardware> hw = AudioHardware::createAudioHw(localDevice, true, getTestCase());
    if (hw.get() == NULL) {
        LOGE("cannot create Audio HW");
        return TaskGeneric::EResultError;
    }
    if (!hw->prepare(AudioHardware::ESampleRate_44100, mVolume, mMode)) {
        LOGE("prepare failed");
        return TaskGeneric::EResultError;
    }
    android::sp<Buffer> buffer = getTestCase()->findBuffer(mId);
    if (buffer.get() == NULL) {
        LOGE("cannot find buffer %s", mId.string());
        return TaskGeneric::EResultError;
    }
    buffer->restart(); // reset to play from beginning
    if (localDevice) {
        if (!hw->startPlaybackOrRecord(buffer)) {
            LOGE("play failed");
            return TaskGeneric::EResultError;
        }
    } else {
        int id = getTestCase()->getRemoteAudio()->getDataId(mId);
        if (id < 0) {
            return TaskGeneric::EResultError;
        }
        AudioRemotePlayback* remote = reinterpret_cast<AudioRemotePlayback*>(hw.get());
        if (!remote->startPlaybackForRemoteData(id, buffer->isStereo())) {
            return TaskGeneric::EResultError;
        }
    }
    // now store sp
    mHw = hw;

    return TaskGeneric::EResultOK;
}

TaskGeneric::ExecutionResult TaskOutput::complete()
{
    bool result = true;
    if (mWaitForCompletion) {
        result = mHw->waitForCompletion();
    }
    mHw->stopPlaybackOrRecord();
    mHw.clear();
    if (!result) {
        LOGE("waitForCompletion failed");
        return TaskGeneric::EResultError;
    }
    return TaskGeneric::EResultOK;
}


