/*
 * Copyright (C) 2011 The Android Open Source Project
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

//#define LOG_NDEBUG 0
#define LOG_TAG "VideoEditorSRC"

#include <stdlib.h>
#include <utils/Log.h>
#include <audio_utils/primitives.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MediaDefs.h>
#include "VideoEditorSRC.h"


namespace android {

VideoEditorSRC::VideoEditorSRC(const sp<MediaSource> &source) {
    ALOGV("VideoEditorSRC %p(%p)", this, source.get());
    static const int32_t kDefaultSamplingFreqencyHz = kFreq32000Hz;
    mSource = source;
    mResampler = NULL;
    mChannelCnt = 0;
    mSampleRate = 0;
    mOutputSampleRate = kDefaultSamplingFreqencyHz;
    mStarted = false;
    mInitialTimeStampUs = -1;
    mAccuOutBufferSize  = 0;
    mSeekTimeUs = -1;
    mBuffer = NULL;
    mLeftover = 0;
    mFormatChanged = false;
    mStopPending = false;
    mSeekMode = ReadOptions::SEEK_PREVIOUS_SYNC;

    // Input Source validation
    sp<MetaData> format = mSource->getFormat();
    const char *mime;
    CHECK(format->findCString(kKeyMIMEType, &mime));
    CHECK(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_RAW));

    // Set the metadata of the output after resampling.
    mOutputFormat = new MetaData;
    mOutputFormat->setCString(kKeyMIMEType, MEDIA_MIMETYPE_AUDIO_RAW);
    mOutputFormat->setInt32(kKeySampleRate, kDefaultSamplingFreqencyHz);
    mOutputFormat->setInt32(kKeyChannelCount, 2);  // always stereo
}

VideoEditorSRC::~VideoEditorSRC() {
    ALOGV("~VideoEditorSRC %p(%p)", this, mSource.get());
    stop();
}

status_t VideoEditorSRC::start(MetaData *params) {
    ALOGV("start %p(%p)", this, mSource.get());
    CHECK(!mStarted);

    // Set resampler if required
    checkAndSetResampler();

    mSeekTimeUs = -1;
    mSeekMode = ReadOptions::SEEK_PREVIOUS_SYNC;
    mStarted = true;
    mSource->start();

    return OK;
}

status_t VideoEditorSRC::stop() {
    ALOGV("stop %p(%p)", this, mSource.get());
    if (!mStarted) {
        return OK;
    }

    if (mBuffer) {
        mBuffer->release();
        mBuffer = NULL;
    }
    mSource->stop();
    if (mResampler != NULL) {
        delete mResampler;
        mResampler = NULL;
    }

    mStarted = false;
    mInitialTimeStampUs = -1;
    mAccuOutBufferSize = 0;
    mLeftover = 0;

    return OK;
}

sp<MetaData> VideoEditorSRC::getFormat() {
    ALOGV("getFormat");
    return mOutputFormat;
}

status_t VideoEditorSRC::read(
        MediaBuffer **buffer_out, const ReadOptions *options) {
    ALOGV("read %p(%p)", this, mSource.get());
    *buffer_out = NULL;

    if (!mStarted) {
        return ERROR_END_OF_STREAM;
    }

    if (mResampler) {
        // Store the seek parameters
        int64_t seekTimeUs;
        ReadOptions::SeekMode mode = ReadOptions::SEEK_PREVIOUS_SYNC;
        if (options && options->getSeekTo(&seekTimeUs, &mode)) {
            ALOGV("read Seek %lld", seekTimeUs);
            mSeekTimeUs = seekTimeUs;
            mSeekMode = mode;
        }

        // We ask for 1024 frames in output
        // resampler output is always 2 channels and 32 bits
        const size_t kOutputFrameCount = 1024;
        const size_t kBytes = kOutputFrameCount * 2 * sizeof(int32_t);
        int32_t *pTmpBuffer = (int32_t *)calloc(1, kBytes);
        if (!pTmpBuffer) {
            ALOGE("calloc failed to allocate memory: %d bytes", kBytes);
            return NO_MEMORY;
        }

        // Resample to target quality
        mResampler->resample(pTmpBuffer, kOutputFrameCount, this);

        if (mStopPending) {
            stop();
            mStopPending = false;
        }

        // Change resampler and retry if format change happened
        if (mFormatChanged) {
            mFormatChanged = false;
            checkAndSetResampler();
            free(pTmpBuffer);
            return read(buffer_out, NULL);
        }

        // Create a new MediaBuffer
        int32_t outBufferSize = kOutputFrameCount * 2 * sizeof(int16_t);
        MediaBuffer* outBuffer = new MediaBuffer(outBufferSize);

        // Convert back to 2 channels and 16 bits
        ditherAndClamp(
                (int32_t *)((uint8_t*)outBuffer->data() + outBuffer->range_offset()),
                pTmpBuffer, kOutputFrameCount);
        free(pTmpBuffer);

        // Compute and set the new timestamp
        sp<MetaData> to = outBuffer->meta_data();
        int64_t totalOutDurationUs = (mAccuOutBufferSize * 1000000) / (mOutputSampleRate * 2 * 2);
        int64_t timeUs = mInitialTimeStampUs + totalOutDurationUs;
        to->setInt64(kKeyTime, timeUs);

        // update the accumulate size
        mAccuOutBufferSize += outBufferSize;
        *buffer_out = outBuffer;
    } else {
        // Resampling not required. Read and pass-through.
        MediaBuffer *aBuffer;
        status_t err = mSource->read(&aBuffer, options);
        if (err != OK) {
            ALOGV("read returns err = %d", err);
        }

        if (err == INFO_FORMAT_CHANGED) {
            checkAndSetResampler();
            return read(buffer_out, NULL);
        }

        // EOS or some other error
        if(err != OK) {
            stop();
            *buffer_out = NULL;
            return err;
        }
        *buffer_out = aBuffer;
    }

    return OK;
}

status_t VideoEditorSRC::getNextBuffer(AudioBufferProvider::Buffer *pBuffer, int64_t pts) {
    ALOGV("getNextBuffer %d, chan = %d", pBuffer->frameCount, mChannelCnt);
    uint32_t done = 0;
    uint32_t want = pBuffer->frameCount * mChannelCnt * 2;
    pBuffer->raw = malloc(want);

    while (mStarted && want > 0) {
        // If we don't have any data left, read a new buffer.
        if (!mBuffer) {
            // if we seek, reset the initial time stamp and accumulated time
            ReadOptions options;
            if (mSeekTimeUs >= 0) {
                ALOGV("%p cacheMore_l Seek requested = %lld", this, mSeekTimeUs);
                ReadOptions::SeekMode mode = mSeekMode;
                options.setSeekTo(mSeekTimeUs, mode);
                mSeekTimeUs = -1;
                mInitialTimeStampUs = -1;
                mAccuOutBufferSize = 0;
            }

            status_t err = mSource->read(&mBuffer, &options);

            if (err != OK) {
                free(pBuffer->raw);
                pBuffer->raw = NULL;
                pBuffer->frameCount = 0;
            }

            if (err == INFO_FORMAT_CHANGED) {
                ALOGV("getNextBuffer: source read returned INFO_FORMAT_CHANGED");
                // At this point we cannot switch to a new AudioResampler because
                // we are in a callback called by the AudioResampler itself. So
                // just remember the fact that the format has changed, and let
                // read() handles this.
                mFormatChanged = true;
                return err;
            }

            // EOS or some other error
            if (err != OK) {
                ALOGV("EOS or some err: %d", err);
                // We cannot call stop() here because stop() will release the
                // AudioResampler, and we are in a callback of the AudioResampler.
                // So just remember the fact and let read() call stop().
                mStopPending = true;
                return err;
            }

            CHECK(mBuffer);
            mLeftover = mBuffer->range_length();
            if (mInitialTimeStampUs == -1) {
                int64_t curTS;
                sp<MetaData> from = mBuffer->meta_data();
                from->findInt64(kKeyTime, &curTS);
                ALOGV("setting mInitialTimeStampUs to %lld", mInitialTimeStampUs);
                mInitialTimeStampUs = curTS;
            }
        }

        // Now copy data to the destination
        uint32_t todo = mLeftover;
        if (todo > want) {
            todo = want;
        }

        uint8_t* end = (uint8_t*)mBuffer->data() + mBuffer->range_offset()
                + mBuffer->range_length();
        memcpy((uint8_t*)pBuffer->raw + done, end - mLeftover, todo);
        done += todo;
        want -= todo;
        mLeftover -= todo;

        // Release MediaBuffer as soon as possible.
        if (mLeftover == 0) {
            mBuffer->release();
            mBuffer = NULL;
        }
    }

    pBuffer->frameCount = done / (mChannelCnt * 2);
    ALOGV("getNextBuffer done %d", pBuffer->frameCount);
    return OK;
}


void VideoEditorSRC::releaseBuffer(AudioBufferProvider::Buffer *pBuffer) {
    ALOGV("releaseBuffer: %p", pBuffers);
    free(pBuffer->raw);
    pBuffer->raw = NULL;
    pBuffer->frameCount = 0;
}

void VideoEditorSRC::checkAndSetResampler() {
    ALOGV("checkAndSetResampler");

    static const uint16_t kUnityGain = 0x1000;
    sp<MetaData> format = mSource->getFormat();
    const char *mime;
    CHECK(format->findCString(kKeyMIMEType, &mime));
    CHECK(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_RAW));

    CHECK(format->findInt32(kKeySampleRate, &mSampleRate));
    CHECK(format->findInt32(kKeyChannelCount, &mChannelCnt));

    // If a resampler exists, delete it first
    if (mResampler != NULL) {
        delete mResampler;
        mResampler = NULL;
    }

    // Clear previous buffer
    if (mBuffer) {
        mBuffer->release();
        mBuffer = NULL;
    }

    if (mSampleRate != mOutputSampleRate || mChannelCnt != 2) {
        ALOGV("Resampling required (%d => %d Hz, # channels = %d)",
            mSampleRate, mOutputSampleRate, mChannelCnt);

        mResampler = AudioResampler::create(
                        16 /* bit depth */,
                        mChannelCnt,
                        mOutputSampleRate);
        CHECK(mResampler);
        mResampler->setSampleRate(mSampleRate);
        mResampler->setVolume(kUnityGain, kUnityGain);
    } else {
        ALOGV("Resampling not required (%d => %d Hz, # channels = %d)",
            mSampleRate, mOutputSampleRate, mChannelCnt);
    }
}

} //namespce android
