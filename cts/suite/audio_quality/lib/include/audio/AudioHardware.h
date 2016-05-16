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

#ifndef CTSAUDIO_AUDIOHARDWARE_H
#define CTSAUDIO_AUDIOHARDWARE_H

#include <utils/StrongPointer.h>
#include <utils/RefBase.h>
#include "Buffer.h"

class TaskCase;
/**
 * Utility class for H/W detection
 */
class AudioHardware : virtual public android::RefBase {
public:
    /** audio length should be multiple of this */
    static const int SAMPLES_PER_ONE_GO = 4096;

    enum SamplingRate {
        ESamplingRateInvald = 0,
        ESampleRate_16000 = 16000,
        ESampleRate_44100 = 44100
    };
    enum BytesPerSample {
        E2BPS = 2
    };
    enum AudioMode {
        EModeVoice = 0,
        EModeMusic = 1
    };

    /**
     * detect supported audio H/W
     * @return card number of detected H/W. -1 if not found.
     */
    static int detectAudioHw();

    /**
     * Factory method
     * options are : local or remote, playback or recording
     * can return NULL(sp.get() == NULL) if H/W not found
     */
    static android::sp<AudioHardware> createAudioHw(bool local, bool playback,
            TaskCase* testCase = NULL);

    virtual ~AudioHardware();
    /**
     * prepare playback or recording
     */
    virtual bool prepare(SamplingRate samplingRate, int volume, int mode = EModeVoice) = 0;

    /**
     * Convenience API to pass buffer ID. The buffer can be either present in testCase
     * or in remote device (when testCase is NULL)
     */
    virtual bool startPlaybackOrRecordById(const android::String8& id, TaskCase* testCase = NULL);

    /**
     *  Playback / Record with given buffer
     *  @param buffer buffer to play / record
     *  @param numberRepetition How many times to repeat playback / record for given buffer.
     *         For record, it does not have much meaning as the last recording will always
     *         override.
     */
    virtual bool startPlaybackOrRecord(android::sp<Buffer>& buffer,
            int numberRepetition = 1) = 0;
    /**
     * Wait for the playback / recording to complete. return true when successfully finished.
     * Calling waitForCompletion after calling stopPlaybackOrRecord will lead into blocking
     * the calling thread for some time.
     */
    virtual bool waitForCompletion() = 0;
    /// stops the on-going action. The active task can be canceled.
    virtual void stopPlaybackOrRecord() = 0;

protected:
    static int mHwId;
};


#endif // CTSAUDIO_AUDIOHARDWARE_H
