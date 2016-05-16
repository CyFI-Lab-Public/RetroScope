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

#ifndef CTSAUDIO_AUDIOPLAYTESTCOMMON_H
#define CTSAUDIO_AUDIOPLAYTESTCOMMON_H

#include <gtest/gtest.h>
#include <utils/threads.h>
#include <utils/StrongPointer.h>

#include <audio/AudioHardware.h>
#include <audio/AudioPlaybackLocal.h>
#include <audio/AudioRecordingLocal.h>
#include <audio/AudioSignalFactory.h>
#include <audio/AudioLocal.h>
#include <audio/Buffer.h>


#include <Log.h>


class AudioPlayTestCommon : public testing::Test {
protected:
    android::sp<Buffer> mBuffer;
    android::sp<AudioHardware> mAudioHw;

    static const int MAX_POSITIVE_AMPLITUDE = 1000;
    static const int SIGNAL_FREQ = 1000;
    static const int SIGNAL_LENGTH = AudioHardware::SAMPLES_PER_ONE_GO * 2;
    static const int DEFAULT_VOLUME = 10;

protected:
    virtual ~AudioPlayTestCommon() {
        LOGV("~AudioPlayTestCommon");
    }
    virtual void SetUp() {
        mAudioHw = createAudioHw();
        ASSERT_TRUE(mAudioHw.get() != NULL);
        mBuffer = AudioSignalFactory::generateSineWave(AudioHardware::E2BPS,
                MAX_POSITIVE_AMPLITUDE, AudioHardware::ESampleRate_44100,
                SIGNAL_FREQ, SIGNAL_LENGTH);
        ASSERT_TRUE(mBuffer.get() != NULL);
    }

    virtual void TearDown() {
        LOGV("AudioPlayTestCommon::TearDown");
        mAudioHw->stopPlaybackOrRecord(); // this stops the thread
        mAudioHw.clear();
    }

    void playAll(int numberRepetition) {
        ASSERT_TRUE(mAudioHw->prepare(AudioHardware::ESampleRate_44100, DEFAULT_VOLUME));
        ASSERT_TRUE(mAudioHw->startPlaybackOrRecord(mBuffer, numberRepetition));
        ASSERT_TRUE(mAudioHw->waitForCompletion());
        mAudioHw->stopPlaybackOrRecord();
        LOGV("size %d, handled %d", mBuffer->getSize(), mBuffer->amountHandled());
        ASSERT_TRUE(mBuffer->amountHandled() == mBuffer->getSize());
    }

    void repeatPlayStop() {
        for (int i = 0; i < 2; i++) {
            ASSERT_TRUE(mAudioHw->prepare(AudioHardware::ESampleRate_44100, DEFAULT_VOLUME));
            mBuffer->restart();
            ASSERT_TRUE(mAudioHw->startPlaybackOrRecord(mBuffer, 10));
            mAudioHw->stopPlaybackOrRecord();
        }
    }

    void playWrongUsage() {
        ASSERT_FALSE(mAudioHw->startPlaybackOrRecord(mBuffer));
        ASSERT_TRUE(mAudioHw->prepare(AudioHardware::ESampleRate_44100, DEFAULT_VOLUME));
        playAll(1);
    }

    virtual android::sp<AudioHardware> createAudioHw() = 0;
};

#endif // CTSAUDIO_AUDIOPLAYTESTCOMMON_H
