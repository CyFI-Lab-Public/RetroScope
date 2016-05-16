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


#ifndef CTSAUDIO_AUDIORECORDINGLOCAL_H
#define CTSAUDIO_AUDIORECORDINGLOCAL_H

#include <utils/String8.h>

#include <tinyalsa/asoundlib.h>

#include "AudioLocal.h"


class AudioRecordingLocal: public AudioLocal {
public:
    AudioRecordingLocal(int hwId);
    virtual ~AudioRecordingLocal();
protected:
    bool doPrepare(AudioHardware::SamplingRate, int samplesInOneGo);
    bool doPlaybackOrRecord(android::sp<Buffer>& buffer);
    void doStop();
    void releaseHw();

private:
    int mHwId;
    struct pcm* mPcmHandle;
    // unit recording samples
    int mSamples;
    // unit recording sizes
    int mSizes;
    // alsa buffer size
    int mBufferSize;
};


#endif // CTSAUDIO_AUDIORECORDINGLOCAL_H
