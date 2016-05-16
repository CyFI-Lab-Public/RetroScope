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

#include <UniquePtr.h>
#include "Log.h"
#include "audio/AudioSignalFactory.h"
#include "audio/RemoteAudio.h"
#include "StringUtil.h"
#include "task/TaskCase.h"
#include "task/TaskSound.h"

static const android::String8 STR_ID("id");
static const android::String8 STR_TYPE("type");

TaskSound::TaskSound()
    : TaskGeneric(TaskGeneric::ETaskSound),
      mPreload(false)
{
    const android::String8* list[] = {&STR_ID, &STR_TYPE, NULL};
    registerSupportedStringAttributes(list);
}

TaskSound::~TaskSound()
{

}

bool TaskSound::parseAttribute(const android::String8& name, const android::String8& value)
{
    if (StringUtil::compare(name, "preload") == 0) {
            if (StringUtil::compare(value, "1") == 0) {
                mPreload = true;
            }
            return true;
    }
    return TaskGeneric::parseAttribute(name, value);
}

TaskGeneric::ExecutionResult TaskSound::run()
{
    android::String8 id;
    if (!findStringAttribute(STR_ID, id)) {
        LOGE("TaskSound::run %s string not found", STR_ID.string());
        return TaskGeneric::EResultError;
    }
    android::String8 type;
    if (!findStringAttribute(STR_TYPE, type)) {
        LOGE("TaskSound::run %s string not found", STR_TYPE.string());
        return TaskGeneric::EResultError;
    }
    UniquePtr<std::vector<android::String8> > tokens(StringUtil::split(type, ':'));
    if (tokens.get() == NULL) {
        LOGE("alloc failed");
        return TaskGeneric::EResultError;
    }
    android::sp<Buffer> buffer;
    if (StringUtil::compare(tokens->at(0), "file") == 0) {
        if (tokens->size() != 2) {
            LOGE("Wrong number of parameters %d", tokens->size());
        }
        buffer = Buffer::loadFromFile(tokens->at(1));
    } else if (StringUtil::compare(tokens->at(0), "sin") == 0) {
        if (tokens->size() != 4) {
            LOGE("Wrong number of parameters %d", tokens->size());
        }
        int amplitude = atoi(tokens->at(1).string());
        int freq = atoi(tokens->at(2).string());
        int time = atoi(tokens->at(3).string());
        int samples = time * AudioHardware::ESampleRate_44100 / 1000;
        buffer = AudioSignalFactory::generateSineWave(AudioHardware::E2BPS, amplitude,
                AudioHardware::ESampleRate_44100, freq, samples, true);
    } else if (StringUtil::compare(tokens->at(0), "random") == 0) {
        // TODO FIXME it does not seem to work well.
        if (tokens->size() != 3) {
            LOGE("Wrong number of parameters %d", tokens->size());
        }
        int amplitude = atoi(tokens->at(1).string());
        int time = atoi(tokens->at(2).string());
        int samples = time * AudioHardware::ESampleRate_44100 / 1000;
        buffer = AudioSignalFactory::generateWhiteNoise(AudioHardware::E2BPS, amplitude,
                samples, true);
    } else { // unknown word
        LOGE("TaskSound::run unknown word in type %s", type.string());
        // next buffer check will return
    }

    if (buffer.get() == NULL) {
        return TaskGeneric::EResultError;
    }
    if (!getTestCase()->registerBuffer(id, buffer)) {
        LOGE("TaskSound::run registering buffer %s failed", id.string());
        return TaskGeneric::EResultError;
    }
    if (mPreload) {
        int downloadId;
        if (!getTestCase()->getRemoteAudio()->downloadData(id, buffer, downloadId)) {
            return TaskGeneric::EResultError;
        }
        LOGI("Downloaded buffer %s to DUT with id %d", id.string(), downloadId);
    }
    return TaskGeneric::EResultOK;
}



