/*
 * Copyright (C) 2012 The Android Open Source Project
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
#include <unistd.h>

#include <gtest/gtest.h>
#include <utils/threads.h>
#include <utils/StrongPointer.h>

#include <audio/AudioLocal.h>
#include <audio/Buffer.h>
#include <Log.h>

#include "AudioPlayTestCommon.h"

class AudioPlayerDummy: public AudioLocal {
public:
    AudioHardware::SamplingRate mSamplingRate;
    android::sp<Buffer> mBufferPassed;
    bool mPrepareCalled;
    bool mdoStartPlaybackOrRecordCalled;
    bool mdoContinuePlaybackOrRecordCalled;
    bool mdoStopCalled;
    int mPlaybackUnit;

    AudioPlayerDummy()
        : mSamplingRate(AudioHardware::ESamplingRateInvald),
          mPrepareCalled(false),
          mdoStartPlaybackOrRecordCalled(false),
          mdoContinuePlaybackOrRecordCalled(false),
          mdoStopCalled(false)
    {

    }

    virtual bool doPrepare(AudioHardware::SamplingRate samplingRate, int samplesInOneGo) {
        mPlaybackUnit = samplesInOneGo * 4;
        LOGV("doPrepare");
        return true;
    };

    virtual bool doPlaybackOrRecord(android::sp<Buffer>& buffer) {
        buffer->increaseHandled(mPlaybackUnit);
        return true;
    };

    virtual void doStop() {
        LOGV("doStop");
    };


};

class AudioLocalTest : public AudioPlayTestCommon {
public:
    virtual ~AudioLocalTest() {};

protected:
    android::sp<AudioHardware> createAudioHw() {
        android::sp<AudioHardware> hw(new AudioPlayerDummy());
        return hw;
    }
};

TEST_F(AudioLocalTest, PlayAllTest) {
    playAll(1);
}

TEST_F(AudioLocalTest, PlayAllRepeatTest) {
    playAll(4);
}

TEST_F(AudioLocalTest, StartStopTest) {
    repeatPlayStop();
}

TEST_F(AudioLocalTest, WrongUsageTest) {
    playWrongUsage();
}

