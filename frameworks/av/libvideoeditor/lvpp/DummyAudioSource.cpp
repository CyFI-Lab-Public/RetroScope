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

// #define LOG_NDEBUG 0
#define LOG_TAG "DummyAudioSource"
#include <utils/Log.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaBufferGroup.h>
#include <media/stagefright/MetaData.h>
#include "DummyAudioSource.h"


namespace android {

//static
sp<DummyAudioSource> DummyAudioSource::Create(
        int32_t samplingRate, int32_t channelCount,
        int64_t frameDurationUs, int64_t audioDurationUs) {

    ALOGV("Create ");
    return new DummyAudioSource(samplingRate,
                                channelCount,
                                frameDurationUs,
                                audioDurationUs);

}

DummyAudioSource::DummyAudioSource(
        int32_t samplingRate, int32_t channelCount,
        int64_t frameDurationUs, int64_t audioDurationUs)
    : mSamplingRate(samplingRate),
      mChannelCount(channelCount),
      mFrameDurationUs(frameDurationUs),
      mNumberOfSamplePerFrame(0),
      mAudioDurationUs(audioDurationUs),
      mTimeStampUs(0),
      mBufferGroup(NULL) {

    mNumberOfSamplePerFrame = (int32_t)
            ((1L * mSamplingRate * mFrameDurationUs)/1000000);
    mNumberOfSamplePerFrame = mNumberOfSamplePerFrame  * mChannelCount;

    ALOGV("Constructor: E");
    ALOGV("samplingRate = %d", samplingRate);
    ALOGV("channelCount = %d", channelCount);
    ALOGV("frameDurationUs = %lld", frameDurationUs);
    ALOGV("audioDurationUs = %lld", audioDurationUs);
    ALOGV("mNumberOfSamplePerFrame = %d", mNumberOfSamplePerFrame);
    ALOGV("Constructor: X");
}

DummyAudioSource::~DummyAudioSource() {
    /* Do nothing here? */
    ALOGV("~DummyAudioSource");
}

void DummyAudioSource::setDuration(int64_t audioDurationUs) {
    ALOGV("setDuration: %lld us added to %lld us",
        audioDurationUs, mAudioDurationUs);

    Mutex::Autolock autoLock(mLock);
    mAudioDurationUs += audioDurationUs;
}

status_t DummyAudioSource::start(MetaData *params) {
    ALOGV("start: E");
    status_t err = OK;

    mTimeStampUs = 0;

    mBufferGroup = new MediaBufferGroup;
    mBufferGroup->add_buffer(
            new MediaBuffer(mNumberOfSamplePerFrame * sizeof(int16_t)));

    ALOGV("start: X");

    return err;
}

status_t DummyAudioSource::stop() {
    ALOGV("stop");

    delete mBufferGroup;
    mBufferGroup = NULL;

    return OK;
}


sp<MetaData> DummyAudioSource::getFormat() {
    ALOGV("getFormat");

    sp<MetaData> meta = new MetaData;
    meta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_AUDIO_RAW);
    meta->setInt32(kKeyChannelCount, mChannelCount);
    meta->setInt32(kKeySampleRate, mSamplingRate);
    meta->setInt64(kKeyDuration, mFrameDurationUs);
    meta->setCString(kKeyDecoderComponent, "DummyAudioSource");

    return meta;
}

status_t DummyAudioSource::read(
        MediaBuffer **out, const MediaSource::ReadOptions *options) {

    ALOGV("read: E");

    int64_t seekTimeUs;
    ReadOptions::SeekMode mode;

    if (options && options->getSeekTo(&seekTimeUs, &mode)) {
        CHECK(seekTimeUs >= 0);
        mTimeStampUs = seekTimeUs;
    }

    {
        Mutex::Autolock autoLock(mLock);
        if (mTimeStampUs >= mAudioDurationUs) {
            ALOGI("read: EOS reached %lld > %lld",
                mTimeStampUs, mAudioDurationUs);

            *out = NULL;
            return ERROR_END_OF_STREAM;
        }
    }

    MediaBuffer *buffer;
    status_t err = mBufferGroup->acquire_buffer(&buffer);
    if (err != OK) {
        ALOGE("Failed to acquire buffer from mBufferGroup: %d", err);
        return err;
    }

    memset((uint8_t *) buffer->data() + buffer->range_offset(),
            0, mNumberOfSamplePerFrame << 1);
    buffer->set_range(buffer->range_offset(), (mNumberOfSamplePerFrame << 1));
    buffer->meta_data()->setInt64(kKeyTime, mTimeStampUs);

    ALOGV("read: offset  = %d, size = %d, mTimeStampUs = %lld",
             buffer->range_offset(), buffer->size(), mTimeStampUs);

    mTimeStampUs = mTimeStampUs + mFrameDurationUs;
    *out = buffer;

    return OK;
}

}// namespace android
