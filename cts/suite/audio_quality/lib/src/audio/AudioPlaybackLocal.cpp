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

// TODO remove all the headers upto asound.h after removing pcm_drain hack
#include <errno.h>
#include <unistd.h>
#include <poll.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/time.h>
#include <limits.h>
#include <linux/ioctl.h>
#define __force
#define __bitwise
#define __user
#include <sound/asound.h>

#include <string.h>
#include <tinyalsa/asoundlib.h>

#include "audio/AudioHardware.h"
#include "audio/Buffer.h"
#include "Log.h"

#include "audio/AudioPlaybackLocal.h"


AudioPlaybackLocal::AudioPlaybackLocal(int hwId)
    : mHwId(hwId),
      mPcmHandle(NULL)
{
    LOGV("AudioPlaybackLocal %x", (unsigned int)this);
}

AudioPlaybackLocal::~AudioPlaybackLocal()
{
    LOGV("~AudioPlaybackLocal %x", (unsigned int)this);
    releaseHw();
}

bool AudioPlaybackLocal::doPrepare(AudioHardware::SamplingRate samplingRate, int samplesInOneGo)
{
    releaseHw();

    struct pcm_config config;

    memset(&config, 0, sizeof(config));
    config.channels = 2;
    config.rate = samplingRate;
    config.period_size = 1024;
    config.period_count = 64;
    config.format = PCM_FORMAT_S16_LE;
    config.start_threshold = 0;
    config.stop_threshold =  0;
    config.silence_threshold = 0;

    mPcmHandle = pcm_open(mHwId, 0, PCM_OUT, &config);
    if (!mPcmHandle || !pcm_is_ready(mPcmHandle)) {
       LOGE("Unable to open PCM device(%d) (%s)\n", mHwId, pcm_get_error(mPcmHandle));
       return false;
    }

    mSamples = samplesInOneGo;
    mSizes = samplesInOneGo * 4; // stereo, 16bit

    return true;
}

bool AudioPlaybackLocal::doPlaybackOrRecord(android::sp<Buffer>& buffer)
{
    if (buffer->amountToHandle() < (size_t)mSizes) {
        mSizes = buffer->amountToHandle();
    }
    if (pcm_write(mPcmHandle, buffer->getUnhanledData(), mSizes)) {
        LOGE("AudioPlaybackLocal error %s", pcm_get_error(mPcmHandle));
        return false;
    }
    buffer->increaseHandled(mSizes);
    LOGV("AudioPlaybackLocal::doPlaybackOrRecord %d", buffer->amountHandled());
    return true;
}

void AudioPlaybackLocal::doStop()
{
    // TODO: remove when pcm_stop does pcm_drain
    // hack to have snd_pcm_drain equivalent
    struct pcm_ {
        int fd;
    };
    pcm_* pcm = (pcm_*)mPcmHandle;
    ioctl(pcm->fd, SNDRV_PCM_IOCTL_DRAIN);
    pcm_stop(mPcmHandle);
}

void AudioPlaybackLocal::releaseHw()
{
    if (mPcmHandle != NULL) {
        LOGV("releaseHw %x", (unsigned int)this);
        doStop();
        pcm_close(mPcmHandle);
        mPcmHandle = NULL;
    }
}
