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


#ifndef CTSAUDIO_AUDIOREMOTE_H
#define CTSAUDIO_AUDIOREMOTE_H
#include <utils/StrongPointer.h>
#include "AudioHardware.h"
// real implementation
#include "RemoteAudio.h"


// wrapper in AudioHardware interface
class AudioRemote: public AudioHardware {
public:
    virtual bool prepare(AudioHardware::SamplingRate samplingRate, int volume,
            int mode = AudioHardware::EModeVoice);

protected:
    AudioRemote(android::sp<RemoteAudio>& remote);
    virtual ~AudioRemote() {};

protected:
    android::sp<RemoteAudio> mRemote;
    AudioHardware::SamplingRate mSamplingRate;
    int mVolume;
    int mMode;
};

class AudioRemotePlayback: public AudioRemote {
public:
    AudioRemotePlayback(android::sp<RemoteAudio>& remote);
    virtual ~AudioRemotePlayback() {};
    virtual bool startPlaybackOrRecord(android::sp<Buffer>& buffer, int numberRepetition = 1);
    virtual bool waitForCompletion();
    virtual void stopPlaybackOrRecord();
    bool startPlaybackForRemoteData(int id, bool stereo,  int numberRepetition = 1);
};

class AudioRemoteRecording: public AudioRemote {
public:
    AudioRemoteRecording(android::sp<RemoteAudio>& remote);
    virtual ~AudioRemoteRecording() {};
    virtual bool startPlaybackOrRecord(android::sp<Buffer>& buffer, int numberRepetition = 1);
    virtual bool waitForCompletion();
    virtual void stopPlaybackOrRecord();
};


#endif // CTSAUDIO_AUDIOREMOTE_H
