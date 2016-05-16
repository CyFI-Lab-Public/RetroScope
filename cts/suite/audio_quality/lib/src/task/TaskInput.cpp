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
#include "audio/AudioHardware.h"
#include "task/TaskCase.h"
#include "task/TaskInput.h"

TaskInput::TaskInput()
    : TaskAsync(TaskGeneric::ETaskInput),
      mRecordingTimeInMs(0)
{

}

TaskInput::~TaskInput()
{

}

bool TaskInput::parseAttribute(const android::String8& name, const android::String8& value)
{
    if (strcmp(name, "time") == 0) {
        mRecordingTimeInMs = atoi(value);
        if (mRecordingTimeInMs < 0) {
            LOGE("TaskInput::parseAttribute invalid recording time %d", mRecordingTimeInMs);
            return false;
        }
        return true;
    }
    return TaskAsync::parseAttribute(name, value);
}

TaskGeneric::ExecutionResult TaskInput::start()
{
    bool localDevice = (mDeviceType == TaskAsync::EDeviceHost);
    android::sp<AudioHardware> hw = AudioHardware::createAudioHw(localDevice, false,
            getTestCase());
    if (hw.get() == NULL) {
        LOGE("createAudioHw failed");
        return TaskGeneric::EResultError;
    }
    // TODO support stereo mode in local later
    //     for now, local is captured in stereo, and it is stored to mono
    //     by keeping only channel 1.
    // local : stereo only, remote : mono only
    size_t bufferSize = mRecordingTimeInMs * AudioHardware::ESampleRate_44100 / 1000 *
            (localDevice ? 4 : 2);
    android::sp<Buffer> buffer(new Buffer(bufferSize, bufferSize, localDevice));
    if (buffer.get() == NULL) {
        LOGE("buffer alloc failed");
        return TaskGeneric::EResultError;
    }
    if (!hw->prepare(AudioHardware::ESampleRate_44100, mVolume, mMode)) {
        LOGE("prepare failed");
        return TaskGeneric::EResultError;
    }
    if (!hw->startPlaybackOrRecord(buffer)) {
        LOGE("record failed");
        return TaskGeneric::EResultError;
    }
    // now store sp
    mHw = hw;
    mBuffer = buffer;
    return TaskGeneric::EResultOK;
}

TaskGeneric::ExecutionResult TaskInput::complete()
{
    bool result = mHw->waitForCompletion();
    mHw->stopPlaybackOrRecord();
    mHw.clear();
    if (!result) {
        LOGE("waitForComletion failed");
        return TaskGeneric::EResultError;
    }
    // TODO: need to keep stereo for local if in stereo mode
    // For now, convert to mono if it is stereo
    if (mBuffer->isStereo()) {
        mBuffer->changeToMono(Buffer::EKeepCh0);
    }
    if (!getTestCase()->registerBuffer(mId, mBuffer)) {
        if (!getTestCase()->updateBuffer(mId, mBuffer)) {
            LOGE("cannot register/update buffer %s", mId.string());
            return TaskGeneric::EResultError;
        }
    }
    return TaskGeneric::EResultOK;
}


