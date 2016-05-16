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

#include <stdint.h>
#include <string.h>
#include <tinyalsa/asoundlib.h>

#include "audio/AudioHardware.h"
#include "audio/Buffer.h"
#include "Log.h"

#include "audio/AudioRecordingLocal.h"


AudioRecordingLocal::AudioRecordingLocal(int hwId)
    : mHwId(hwId),
      mPcmHandle(NULL)
{
    LOGV("AudioRecordingLocal %x", (unsigned int)this);
}

AudioRecordingLocal::~AudioRecordingLocal()
{
    LOGV("~AudioRecordingLocal %x", (unsigned int)this);
    releaseHw();
}

bool AudioRecordingLocal::doPrepare(AudioHardware::SamplingRate samplingRate, int samplesInOneGo)
{
    releaseHw();

    struct pcm_config config;

    memset(&config, 0, sizeof(config));
    config.channels = 2;
    config.rate = samplingRate;
    config.period_size = 1024;
    config.period_count = 32;
    config.format = PCM_FORMAT_S16_LE;
    config.start_threshold = 0;
    config.stop_threshold = 0;
    config.silence_threshold = 0;

    mPcmHandle = pcm_open(mHwId, 0, PCM_IN, &config);
    if (!mPcmHandle || !pcm_is_ready(mPcmHandle)) {
       LOGE("Unable to open PCM device(%d) (%s)\n", mHwId, pcm_get_error(mPcmHandle));
       return false;
    }

    mSamples = samplesInOneGo;
    mSizes = samplesInOneGo * 4; // stereo, 16bit

    mBufferSize = pcm_get_buffer_size(mPcmHandle);
    LOGD("buffer size %d, read size %d", mBufferSize, mSizes);
    return true;
}

bool AudioRecordingLocal::doPlaybackOrRecord(android::sp<Buffer>& buffer)
{
    int toRead = mSizes;
    if (buffer->amountToHandle() < (size_t)mSizes) {
        toRead = buffer->amountToHandle();
    }
    LOGD("recording will read %d", toRead);

    while (toRead > 0) {
        int readSize = (toRead > mBufferSize) ? mBufferSize : toRead;
        if (pcm_read(mPcmHandle, buffer->getUnhanledData(), readSize)) {
            LOGE("AudioRecordingLocal error %s", pcm_get_error(mPcmHandle));
            return false;
        }
        buffer->increaseHandled(readSize);
        toRead -= readSize;
    }
    LOGV("AudioRecordingLocal::doPlaybackOrRecord %d", buffer->amountHandled());
    return true;
}

void AudioRecordingLocal::doStop()
{
    pcm_stop(mPcmHandle);
}

void AudioRecordingLocal::releaseHw()
{
    if (mPcmHandle != NULL) {
        LOGV("releaseHw %x", (unsigned int)this);
        doStop();
        pcm_close(mPcmHandle);
        mPcmHandle = NULL;
    }
}
