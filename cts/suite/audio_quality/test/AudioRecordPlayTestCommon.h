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


#ifndef CTSAUDIO_AUDIORECORDPLAYTESTCOMMON_H
#define CTSAUDIO_AUDIORECORDPLAYTESTCOMMON_H

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

class AudioRecordPlayTestCommon : public testing::Test {
protected:
    android::sp<Buffer> mBufferRecording;
    android::sp<Buffer> mBufferPlayback;
    android::sp<AudioHardware> mAudioRecordingHw;
    android::sp<AudioHardware> mAudioPlaybackHw;

    static const int MAX_POSITIVE_AMPLITUDE = 10000;
    static const int SIGNAL_FREQ = 1000;
    static const int NUMBER_SAMPLES = AudioHardware::SAMPLES_PER_ONE_GO * 4;
    static const int DEFAULT_VOLUME = 10;
protected:
    virtual void SetUp() {
        mAudioPlaybackHw = createPlaybackHw();
        ASSERT_TRUE(mAudioPlaybackHw.get() != NULL);
        mAudioRecordingHw = createRecordingHw();
        ASSERT_TRUE(mAudioRecordingHw.get() != NULL);
        mBufferPlayback = AudioSignalFactory::generateSineWave(AudioHardware::E2BPS,
                MAX_POSITIVE_AMPLITUDE, AudioHardware::ESampleRate_44100,
                SIGNAL_FREQ, NUMBER_SAMPLES);
        ASSERT_TRUE(mBufferPlayback.get() != NULL);
        mBufferRecording = new Buffer(NUMBER_SAMPLES * 4, NUMBER_SAMPLES * 4);
        ASSERT_TRUE(mBufferRecording.get() != NULL);
    }

    virtual void TearDown() {
        mAudioRecordingHw->stopPlaybackOrRecord();
        mAudioPlaybackHw->stopPlaybackOrRecord();
        mAudioRecordingHw.clear();
        mAudioPlaybackHw.clear();
    }

    void PlayAndRecord(int numberRepetition) {
        ASSERT_TRUE(mAudioPlaybackHw->prepare(AudioHardware::ESampleRate_44100, DEFAULT_VOLUME));
        ASSERT_TRUE(mAudioRecordingHw->prepare(AudioHardware::ESampleRate_44100, DEFAULT_VOLUME));
        ASSERT_TRUE(mAudioRecordingHw->startPlaybackOrRecord(mBufferRecording,
                numberRepetition));
        ASSERT_TRUE(mAudioPlaybackHw->startPlaybackOrRecord(mBufferPlayback,
                numberRepetition));

        ASSERT_TRUE(mAudioRecordingHw->waitForCompletion());
        ASSERT_TRUE(mAudioPlaybackHw->waitForCompletion());
        mAudioPlaybackHw->stopPlaybackOrRecord();
        mAudioRecordingHw->stopPlaybackOrRecord();
        LOGV("Audio playback buffer size %d, handled %d", mBufferPlayback->getSize(),
                mBufferPlayback->amountHandled());
        ASSERT_TRUE(mBufferPlayback->amountHandled() == mBufferPlayback->getSize());
        LOGV("Audio recording buffer size %d, handled %d", mBufferRecording->getSize(),
                mBufferRecording->amountHandled());
        ASSERT_TRUE(mBufferRecording->amountHandled() == mBufferRecording->getSize());
    }

    virtual android::sp<AudioHardware> createRecordingHw() = 0;
    virtual android::sp<AudioHardware> createPlaybackHw() = 0;
};




#endif // CTSAUDIO_AUDIORECORDPLAYTESTCOMMON_H
