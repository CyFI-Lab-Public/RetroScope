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
#include <unistd.h>

#include <gtest/gtest.h>
#include <utils/threads.h>
#include <utils/StrongPointer.h>

#include <audio/AudioHardware.h>
#include <GenericFactory.h>
#include <audio/AudioPlaybackLocal.h>

#include <Log.h>

#include "AudioPlayTestCommon.h"

class AudioPlaybackLocalTest : public AudioPlayTestCommon {
public:
    virtual ~AudioPlaybackLocalTest() {};
protected:

    android::sp<AudioHardware> createAudioHw() {
        return AudioHardware::createAudioHw(true, true);
    }
};


TEST_F(AudioPlaybackLocalTest, PlayAllTest) {
    playAll(1);
}

TEST_F(AudioPlaybackLocalTest, PlayAllRepeatTest) {
    playAll(4);
}

TEST_F(AudioPlaybackLocalTest, StartStopTest) {
    repeatPlayStop();
}

TEST_F(AudioPlaybackLocalTest, WrongUsageTest) {
    playWrongUsage();
}


