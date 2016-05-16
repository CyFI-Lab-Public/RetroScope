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
#define LOG_TAG "DummyVideoSource"
#include <stdlib.h>
#include <utils/Log.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MediaBufferGroup.h>
#include <media/stagefright/MetaData.h>
#include "VideoEditorTools.h"
#include "DummyVideoSource.h"


namespace android {

sp<DummyVideoSource> DummyVideoSource::Create(
        uint32_t width, uint32_t height,
        uint64_t clipDuration, const char *imageUri) {

    ALOGV("Create");
    return new DummyVideoSource(
                    width, height, clipDuration, imageUri);

}


DummyVideoSource::DummyVideoSource(
        uint32_t width, uint32_t height,
        uint64_t clipDuration, const char *imageUri) {

    ALOGV("Constructor: E");

    mFrameWidth = width;
    mFrameHeight = height;
    mImageClipDuration = clipDuration;
    mUri = imageUri;
    mImageBuffer = NULL;

    ALOGV("%s", mUri);
    ALOGV("Constructor: X");
}


DummyVideoSource::~DummyVideoSource() {
    /* Do nothing here? */
    ALOGV("~DummyVideoSource");
}



status_t DummyVideoSource::start(MetaData *params) {
    ALOGV("start: E");

    // Get the frame buffer from the rgb file, mUri,
    // and store its content into a MediaBuffer
    status_t err = LvGetImageThumbNail(
                    (const char *)mUri,
                    mFrameHeight, mFrameWidth,
                    (M4OSA_Void **) &mImageBuffer);
    if (err != OK) {
        ALOGE("LvGetImageThumbNail failed: %d", err);
        return err;
    }

    mIsFirstImageFrame = true;
    mImageSeekTime = 0;
    mImagePlayStartTime = 0;
    mFrameTimeUs = 0;

    ALOGV("start: X");
    return OK;
}


status_t DummyVideoSource::stop() {
    ALOGV("stop");
    status_t err = OK;

    if (mImageBuffer != NULL) {
        free(mImageBuffer);
        mImageBuffer = NULL;
    }

    return err;
}


sp<MetaData> DummyVideoSource::getFormat() {
    ALOGV("getFormat");

    sp<MetaData> meta = new MetaData;
    meta->setInt32(kKeyColorFormat, OMX_COLOR_FormatYUV420Planar);
    meta->setInt32(kKeyWidth, mFrameWidth);
    meta->setInt32(kKeyHeight, mFrameHeight);
    meta->setInt64(kKeyDuration, mImageClipDuration);
    meta->setCString(kKeyDecoderComponent, "DummyVideoSource");

    return meta;
}

status_t DummyVideoSource::read(
        MediaBuffer **out,
        const MediaSource::ReadOptions *options) {

    ALOGV("read: E");

    const int32_t kTimeScale = 1000;  /* time scale in ms */
    bool seeking = false;
    int64_t seekTimeUs;
    ReadOptions::SeekMode seekMode;
    if (options && options->getSeekTo(&seekTimeUs, &seekMode)) {
        seeking = true;
        mImageSeekTime = seekTimeUs;
        M4OSA_clockGetTime(&mImagePlayStartTime, kTimeScale);
    }

    if ((mImageSeekTime == mImageClipDuration) ||
        (mFrameTimeUs == (int64_t)mImageClipDuration)) {
        ALOGV("read: EOS reached");
        *out = NULL;
        return ERROR_END_OF_STREAM;
    }

    status_t err = OK;
    MediaBuffer *buffer = new MediaBuffer(
            mImageBuffer, (mFrameWidth * mFrameHeight * 1.5));

    // Set timestamp of buffer
    if (mIsFirstImageFrame) {
        M4OSA_clockGetTime(&mImagePlayStartTime, kTimeScale);
        mFrameTimeUs =  (mImageSeekTime + 1);
        ALOGV("read: jpg 1st frame timeUs = %lld, begin cut time = %ld",
            mFrameTimeUs, mImageSeekTime);

        mIsFirstImageFrame = false;
    } else {
        M4OSA_Time  currentTimeMs;
        M4OSA_clockGetTime(&currentTimeMs, kTimeScale);

        mFrameTimeUs = mImageSeekTime +
            (currentTimeMs - mImagePlayStartTime) * 1000LL;

        ALOGV("read: jpg frame timeUs = %lld", mFrameTimeUs);
    }

    buffer->meta_data()->setInt64(kKeyTime, mFrameTimeUs);
    buffer->set_range(buffer->range_offset(),
                mFrameWidth * mFrameHeight * 1.5);

    *out = buffer;
    return err;
}

}// namespace android
