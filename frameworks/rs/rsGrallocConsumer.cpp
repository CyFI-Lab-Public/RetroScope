/*
 * Copyright (C) 2013 The Android Open Source Project
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

#define ATRACE_TAG ATRACE_TAG_RS

#include "rsContext.h"
#include "rsAllocation.h"
#include "rsAdapter.h"
#include "rs_hal.h"

#include <cutils/compiler.h>
#include <utils/Log.h>
#include "rsGrallocConsumer.h"
#include <ui/GraphicBuffer.h>


namespace android {
namespace renderscript {

GrallocConsumer::GrallocConsumer(Allocation *a, const sp<IGraphicBufferConsumer>& bq) :
    ConsumerBase(bq, true)
{
    mAlloc = a;
    mConsumer->setConsumerUsageBits(GRALLOC_USAGE_SW_READ_OFTEN);
    mConsumer->setMaxAcquiredBufferCount(2);

    uint32_t y = a->mHal.drvState.lod[0].dimY;
    if (y < 1) y = 1;
    mConsumer->setDefaultBufferSize(a->mHal.drvState.lod[0].dimX, y);

    if (a->mHal.state.yuv) {
        bq->setDefaultBufferFormat(a->mHal.state.yuv);
    }
    //mBufferQueue->setConsumerName(name);
}

GrallocConsumer::~GrallocConsumer() {
    // ConsumerBase destructor does all the work.
}



status_t GrallocConsumer::lockNextBuffer() {
    Mutex::Autolock _l(mMutex);
    status_t err;

    if (mAcquiredBuffer.mSlot != BufferQueue::INVALID_BUFFER_SLOT) {
        err = releaseAcquiredBufferLocked();
        if (err) {
            return err;
        }
    }

    BufferQueue::BufferItem b;

    err = acquireBufferLocked(&b, 0);
    if (err != OK) {
        if (err == BufferQueue::NO_BUFFER_AVAILABLE) {
            return BAD_VALUE;
        } else {
            ALOGE("Error acquiring buffer: %s (%d)", strerror(err), err);
            return err;
        }
    }

    int buf = b.mBuf;

    if (b.mFence.get()) {
        err = b.mFence->waitForever("GrallocConsumer::lockNextBuffer");
        if (err != OK) {
            ALOGE("Failed to wait for fence of acquired buffer: %s (%d)",
                    strerror(-err), err);
            return err;
        }
    }

    void *bufferPointer = NULL;
    android_ycbcr ycbcr = android_ycbcr();

    if (mSlots[buf].mGraphicBuffer->getPixelFormat() ==
            HAL_PIXEL_FORMAT_YCbCr_420_888) {
        err = mSlots[buf].mGraphicBuffer->lockYCbCr(
            GraphicBuffer::USAGE_SW_READ_OFTEN,
            b.mCrop,
            &ycbcr);

        if (err != OK) {
            ALOGE("Unable to lock YCbCr buffer for CPU reading: %s (%d)",
                    strerror(-err), err);
            return err;
        }
        bufferPointer = ycbcr.y;
    } else {
        err = mSlots[buf].mGraphicBuffer->lock(
            GraphicBuffer::USAGE_SW_READ_OFTEN,
            b.mCrop,
            &bufferPointer);

        if (err != OK) {
            ALOGE("Unable to lock buffer for CPU reading: %s (%d)",
                    strerror(-err), err);
            return err;
        }
    }

    size_t lockedIdx = 0;
    assert(mAcquiredBuffer.mSlot == BufferQueue::INVALID_BUFFER_SLOT);

    mAcquiredBuffer.mSlot = buf;
    mAcquiredBuffer.mBufferPointer = bufferPointer;
    mAcquiredBuffer.mGraphicBuffer = mSlots[buf].mGraphicBuffer;

    mAlloc->mHal.drvState.lod[0].mallocPtr = reinterpret_cast<uint8_t*>(bufferPointer);
    mAlloc->mHal.drvState.lod[0].stride = mSlots[buf].mGraphicBuffer->getStride() *
            mAlloc->mHal.state.type->getElementSizeBytes();
    mAlloc->mHal.state.nativeBuffer = mAcquiredBuffer.mGraphicBuffer->getNativeBuffer();
    mAlloc->mHal.state.timestamp = b.mTimestamp;

    assert(mAlloc->mHal.drvState.lod[0].dimX ==
           mSlots[buf].mGraphicBuffer->getWidth());
    assert(mAlloc->mHal.drvState.lod[0].dimY ==
           mSlots[buf].mGraphicBuffer->getHeight());

    //mAlloc->format = mSlots[buf].mGraphicBuffer->getPixelFormat();

    //mAlloc->crop        = b.mCrop;
    //mAlloc->transform   = b.mTransform;
    //mAlloc->scalingMode = b.mScalingMode;
    //mAlloc->frameNumber = b.mFrameNumber;

    if (mAlloc->mHal.state.yuv) {
        mAlloc->mHal.drvState.lod[1].mallocPtr = ycbcr.cr;
        mAlloc->mHal.drvState.lod[2].mallocPtr = ycbcr.cb;

        mAlloc->mHal.drvState.lod[0].stride = ycbcr.ystride;
        mAlloc->mHal.drvState.lod[1].stride = ycbcr.cstride;
        mAlloc->mHal.drvState.lod[2].stride = ycbcr.cstride;

        mAlloc->mHal.drvState.yuv.shift = 1;
        mAlloc->mHal.drvState.yuv.step = ycbcr.chroma_step;
    }

    return OK;
}

status_t GrallocConsumer::unlockBuffer() {
    Mutex::Autolock _l(mMutex);
    return releaseAcquiredBufferLocked();
}

status_t GrallocConsumer::releaseAcquiredBufferLocked() {
    status_t err;

    err = mAcquiredBuffer.mGraphicBuffer->unlock();
    if (err != OK) {
        ALOGE("%s: Unable to unlock graphic buffer", __FUNCTION__);
        return err;
    }
    int buf = mAcquiredBuffer.mSlot;

    // release the buffer if it hasn't already been freed by the BufferQueue.
    // This can happen, for example, when the producer of this buffer
    // disconnected after this buffer was acquired.
    if (CC_LIKELY(mAcquiredBuffer.mGraphicBuffer ==
            mSlots[buf].mGraphicBuffer)) {
        releaseBufferLocked(
                buf, mAcquiredBuffer.mGraphicBuffer,
                EGL_NO_DISPLAY, EGL_NO_SYNC_KHR);
    }

    mAcquiredBuffer.mSlot = BufferQueue::INVALID_BUFFER_SLOT;
    mAcquiredBuffer.mBufferPointer = NULL;
    mAcquiredBuffer.mGraphicBuffer.clear();
    return OK;
}

} // namespace renderscript
} // namespace android

