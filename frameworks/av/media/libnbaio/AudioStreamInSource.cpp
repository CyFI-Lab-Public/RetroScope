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

#define LOG_TAG "AudioStreamInSource"
//#define LOG_NDEBUG 0

#include <cutils/compiler.h>
#include <utils/Log.h>
#include <media/nbaio/AudioStreamInSource.h>

namespace android {

AudioStreamInSource::AudioStreamInSource(audio_stream_in *stream) :
        NBAIO_Source(),
        mStream(stream),
        mStreamBufferSizeBytes(0),
        mFramesOverrun(0),
        mOverruns(0)
{
    ALOG_ASSERT(stream != NULL);
}

AudioStreamInSource::~AudioStreamInSource()
{
}

ssize_t AudioStreamInSource::negotiate(const NBAIO_Format offers[], size_t numOffers,
                                      NBAIO_Format counterOffers[], size_t& numCounterOffers)
{
    if (mFormat == Format_Invalid) {
        mStreamBufferSizeBytes = mStream->common.get_buffer_size(&mStream->common);
        audio_format_t streamFormat = mStream->common.get_format(&mStream->common);
        if (streamFormat == AUDIO_FORMAT_PCM_16_BIT) {
            uint32_t sampleRate = mStream->common.get_sample_rate(&mStream->common);
            audio_channel_mask_t channelMask =
                    (audio_channel_mask_t) mStream->common.get_channels(&mStream->common);
            mFormat = Format_from_SR_C(sampleRate, popcount(channelMask));
            mBitShift = Format_frameBitShift(mFormat);
        }
    }
    return NBAIO_Source::negotiate(offers, numOffers, counterOffers, numCounterOffers);
}

size_t AudioStreamInSource::framesOverrun()
{
    uint32_t framesOverrun = mStream->get_input_frames_lost(mStream);
    if (framesOverrun > 0) {
        mFramesOverrun += framesOverrun;
        // FIXME only increment for contiguous ranges
        ++mOverruns;
    }
    return mFramesOverrun;
}

ssize_t AudioStreamInSource::read(void *buffer, size_t count)
{
    if (CC_UNLIKELY(mFormat == Format_Invalid)) {
        return NEGOTIATE;
    }
    ssize_t bytesRead = mStream->read(mStream, buffer, count << mBitShift);
    if (bytesRead > 0) {
        size_t framesRead = bytesRead >> mBitShift;
        mFramesRead += framesRead;
        return framesRead;
    } else {
        return bytesRead;
    }
}

}   // namespace android
