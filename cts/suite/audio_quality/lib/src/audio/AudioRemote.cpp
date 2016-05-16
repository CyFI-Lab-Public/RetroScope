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

#include "Log.h"
#include "audio/AudioRemote.h"
#include "audio/RemoteAudio.h"

bool AudioRemote::prepare(AudioHardware::SamplingRate samplingRate, int volume, int mode)
{
    if (mRemote == NULL) {
        LOGE("AudioRemote::prepare mRemote NULL");
        return false;
    }
    mSamplingRate = samplingRate;
    mVolume = volume;
    mMode = mode;
    return true;
}

AudioRemote::AudioRemote(android::sp<RemoteAudio>& remote)
    : mRemote(remote)
{

}

AudioRemotePlayback::AudioRemotePlayback(android::sp<RemoteAudio>& remote)
    : AudioRemote(remote)
{

}

bool AudioRemotePlayback::startPlaybackOrRecord(android::sp<Buffer>& buffer, int numberRepetition)
{
    //TODO not supported for the moment
    return false;
}

bool AudioRemotePlayback::waitForCompletion()
{
    return mRemote->waitForPlaybackCompletion();
}

void AudioRemotePlayback::stopPlaybackOrRecord()
{
    mRemote->stopPlayback();
}

bool AudioRemotePlayback::startPlaybackForRemoteData(int id, bool stereo, int numberRepetition)
{
    return mRemote->startPlayback(stereo, mSamplingRate, mMode, mVolume, id, numberRepetition);
}

AudioRemoteRecording::AudioRemoteRecording(android::sp<RemoteAudio>& remote)
    : AudioRemote(remote)
{

}

bool AudioRemoteRecording::startPlaybackOrRecord(android::sp<Buffer>& buffer,
        int /*numberRepetition*/)
{
    bool stereo = buffer->isStereo();
    return mRemote->startRecording(stereo, mSamplingRate, mMode, mVolume, buffer);
}

bool AudioRemoteRecording::waitForCompletion()
{
    return mRemote->waitForRecordingCompletion();
}

void AudioRemoteRecording::stopPlaybackOrRecord()
{
    mRemote->stopRecording();
}



