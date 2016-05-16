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

#define LOG_TAG "GraphicBufferSource"
//#define LOG_NDEBUG 0
#include <utils/Log.h>

#include "GraphicBufferSource.h"

#include <OMX_Core.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>

#include <media/hardware/MetadataBufferType.h>
#include <ui/GraphicBuffer.h>

namespace android {

static const bool EXTRA_CHECK = true;


GraphicBufferSource::GraphicBufferSource(OMXNodeInstance* nodeInstance,
        uint32_t bufferWidth, uint32_t bufferHeight, uint32_t bufferCount) :
    mInitCheck(UNKNOWN_ERROR),
    mNodeInstance(nodeInstance),
    mExecuting(false),
    mSuspended(false),
    mNumFramesAvailable(0),
    mEndOfStream(false),
    mEndOfStreamSent(false),
    mRepeatAfterUs(-1ll),
    mRepeatLastFrameGeneration(0),
    mLatestSubmittedBufferId(-1),
    mLatestSubmittedBufferFrameNum(0),
    mLatestSubmittedBufferUseCount(0),
    mRepeatBufferDeferred(false) {

    ALOGV("GraphicBufferSource w=%u h=%u c=%u",
            bufferWidth, bufferHeight, bufferCount);

    if (bufferWidth == 0 || bufferHeight == 0) {
        ALOGE("Invalid dimensions %ux%u", bufferWidth, bufferHeight);
        mInitCheck = BAD_VALUE;
        return;
    }

    String8 name("GraphicBufferSource");

    mBufferQueue = new BufferQueue();
    mBufferQueue->setConsumerName(name);
    mBufferQueue->setDefaultBufferSize(bufferWidth, bufferHeight);
    mBufferQueue->setConsumerUsageBits(GRALLOC_USAGE_HW_VIDEO_ENCODER |
            GRALLOC_USAGE_HW_TEXTURE);

    mInitCheck = mBufferQueue->setMaxAcquiredBufferCount(bufferCount);
    if (mInitCheck != NO_ERROR) {
        ALOGE("Unable to set BQ max acquired buffer count to %u: %d",
                bufferCount, mInitCheck);
        return;
    }

    // Note that we can't create an sp<...>(this) in a ctor that will not keep a
    // reference once the ctor ends, as that would cause the refcount of 'this'
    // dropping to 0 at the end of the ctor.  Since all we need is a wp<...>
    // that's what we create.
    wp<BufferQueue::ConsumerListener> listener = static_cast<BufferQueue::ConsumerListener*>(this);
    sp<BufferQueue::ProxyConsumerListener> proxy = new BufferQueue::ProxyConsumerListener(listener);

    mInitCheck = mBufferQueue->consumerConnect(proxy, false);
    if (mInitCheck != NO_ERROR) {
        ALOGE("Error connecting to BufferQueue: %s (%d)",
                strerror(-mInitCheck), mInitCheck);
        return;
    }

    CHECK(mInitCheck == NO_ERROR);
}

GraphicBufferSource::~GraphicBufferSource() {
    ALOGV("~GraphicBufferSource");
    if (mBufferQueue != NULL) {
        status_t err = mBufferQueue->consumerDisconnect();
        if (err != NO_ERROR) {
            ALOGW("consumerDisconnect failed: %d", err);
        }
    }
}

void GraphicBufferSource::omxExecuting() {
    Mutex::Autolock autoLock(mMutex);
    ALOGV("--> executing; avail=%d, codec vec size=%zd",
            mNumFramesAvailable, mCodecBuffers.size());
    CHECK(!mExecuting);
    mExecuting = true;

    // Start by loading up as many buffers as possible.  We want to do this,
    // rather than just submit the first buffer, to avoid a degenerate case:
    // if all BQ buffers arrive before we start executing, and we only submit
    // one here, the other BQ buffers will just sit until we get notified
    // that the codec buffer has been released.  We'd then acquire and
    // submit a single additional buffer, repeatedly, never using more than
    // one codec buffer simultaneously.  (We could instead try to submit
    // all BQ buffers whenever any codec buffer is freed, but if we get the
    // initial conditions right that will never be useful.)
    while (mNumFramesAvailable) {
        if (!fillCodecBuffer_l()) {
            ALOGV("stop load with frames available (codecAvail=%d)",
                    isCodecBufferAvailable_l());
            break;
        }
    }

    ALOGV("done loading initial frames, avail=%d", mNumFramesAvailable);

    // If EOS has already been signaled, and there are no more frames to
    // submit, try to send EOS now as well.
    if (mEndOfStream && mNumFramesAvailable == 0) {
        submitEndOfInputStream_l();
    }

    if (mRepeatAfterUs > 0ll && mLooper == NULL) {
        mReflector = new AHandlerReflector<GraphicBufferSource>(this);

        mLooper = new ALooper;
        mLooper->registerHandler(mReflector);
        mLooper->start();

        if (mLatestSubmittedBufferId >= 0) {
            sp<AMessage> msg =
                new AMessage(kWhatRepeatLastFrame, mReflector->id());

            msg->setInt32("generation", ++mRepeatLastFrameGeneration);
            msg->post(mRepeatAfterUs);
        }
    }
}

void GraphicBufferSource::omxIdle() {
    ALOGV("omxIdle");

    Mutex::Autolock autoLock(mMutex);

    if (mExecuting) {
        // We are only interested in the transition from executing->idle,
        // not loaded->idle.
        mExecuting = false;
    }
}

void GraphicBufferSource::omxLoaded(){
    Mutex::Autolock autoLock(mMutex);
    if (!mExecuting) {
        // This can happen if something failed very early.
        ALOGW("Dropped back down to Loaded without Executing");
    }

    if (mLooper != NULL) {
        mLooper->unregisterHandler(mReflector->id());
        mReflector.clear();

        mLooper->stop();
        mLooper.clear();
    }

    ALOGV("--> loaded; avail=%d eos=%d eosSent=%d",
            mNumFramesAvailable, mEndOfStream, mEndOfStreamSent);

    // Codec is no longer executing.  Discard all codec-related state.
    mCodecBuffers.clear();
    // TODO: scan mCodecBuffers to verify that all mGraphicBuffer entries
    //       are null; complain if not

    mExecuting = false;
}

void GraphicBufferSource::addCodecBuffer(OMX_BUFFERHEADERTYPE* header) {
    Mutex::Autolock autoLock(mMutex);

    if (mExecuting) {
        // This should never happen -- buffers can only be allocated when
        // transitioning from "loaded" to "idle".
        ALOGE("addCodecBuffer: buffer added while executing");
        return;
    }

    ALOGV("addCodecBuffer h=%p size=%lu p=%p",
            header, header->nAllocLen, header->pBuffer);
    CodecBuffer codecBuffer;
    codecBuffer.mHeader = header;
    mCodecBuffers.add(codecBuffer);
}

void GraphicBufferSource::codecBufferEmptied(OMX_BUFFERHEADERTYPE* header) {
    Mutex::Autolock autoLock(mMutex);

    if (!mExecuting) {
        return;
    }

    int cbi = findMatchingCodecBuffer_l(header);
    if (cbi < 0) {
        // This should never happen.
        ALOGE("codecBufferEmptied: buffer not recognized (h=%p)", header);
        return;
    }

    ALOGV("codecBufferEmptied h=%p size=%lu filled=%lu p=%p",
            header, header->nAllocLen, header->nFilledLen,
            header->pBuffer);
    CodecBuffer& codecBuffer(mCodecBuffers.editItemAt(cbi));

    // header->nFilledLen may not be the original value, so we can't compare
    // that to zero to see of this was the EOS buffer.  Instead we just
    // see if the GraphicBuffer reference was null, which should only ever
    // happen for EOS.
    if (codecBuffer.mGraphicBuffer == NULL) {
        if (!(mEndOfStream && mEndOfStreamSent)) {
            // This can happen when broken code sends us the same buffer
            // twice in a row.
            ALOGE("ERROR: codecBufferEmptied on non-EOS null buffer "
                    "(buffer emptied twice?)");
        }
        // No GraphicBuffer to deal with, no additional input or output is
        // expected, so just return.
        return;
    }

    if (EXTRA_CHECK) {
        // Pull the graphic buffer handle back out of the buffer, and confirm
        // that it matches expectations.
        OMX_U8* data = header->pBuffer;
        buffer_handle_t bufferHandle;
        memcpy(&bufferHandle, data + 4, sizeof(buffer_handle_t));
        if (bufferHandle != codecBuffer.mGraphicBuffer->handle) {
            // should never happen
            ALOGE("codecBufferEmptied: buffer's handle is %p, expected %p",
                    bufferHandle, codecBuffer.mGraphicBuffer->handle);
            CHECK(!"codecBufferEmptied: mismatched buffer");
        }
    }

    // Find matching entry in our cached copy of the BufferQueue slots.
    // If we find a match, release that slot.  If we don't, the BufferQueue
    // has dropped that GraphicBuffer, and there's nothing for us to release.
    int id = codecBuffer.mBuf;
    if (mBufferSlot[id] != NULL &&
        mBufferSlot[id]->handle == codecBuffer.mGraphicBuffer->handle) {
        ALOGV("cbi %d matches bq slot %d, handle=%p",
                cbi, id, mBufferSlot[id]->handle);

        if (id == mLatestSubmittedBufferId) {
            CHECK_GT(mLatestSubmittedBufferUseCount--, 0);
        } else {
            mBufferQueue->releaseBuffer(id, codecBuffer.mFrameNumber,
                    EGL_NO_DISPLAY, EGL_NO_SYNC_KHR, Fence::NO_FENCE);
        }
    } else {
        ALOGV("codecBufferEmptied: no match for emptied buffer in cbi %d",
                cbi);
    }

    // Mark the codec buffer as available by clearing the GraphicBuffer ref.
    codecBuffer.mGraphicBuffer = NULL;

    if (mNumFramesAvailable) {
        // Fill this codec buffer.
        CHECK(!mEndOfStreamSent);
        ALOGV("buffer freed, %d frames avail (eos=%d)",
                mNumFramesAvailable, mEndOfStream);
        fillCodecBuffer_l();
    } else if (mEndOfStream) {
        // No frames available, but EOS is pending, so use this buffer to
        // send that.
        ALOGV("buffer freed, EOS pending");
        submitEndOfInputStream_l();
    } else if (mRepeatBufferDeferred) {
        bool success = repeatLatestSubmittedBuffer_l();
        if (success) {
            ALOGV("deferred repeatLatestSubmittedBuffer_l SUCCESS");
        } else {
            ALOGV("deferred repeatLatestSubmittedBuffer_l FAILURE");
        }
        mRepeatBufferDeferred = false;
    }

    return;
}

void GraphicBufferSource::suspend(bool suspend) {
    Mutex::Autolock autoLock(mMutex);

    if (suspend) {
        mSuspended = true;

        while (mNumFramesAvailable > 0) {
            BufferQueue::BufferItem item;
            status_t err = mBufferQueue->acquireBuffer(&item, 0);

            if (err == BufferQueue::NO_BUFFER_AVAILABLE) {
                // shouldn't happen.
                ALOGW("suspend: frame was not available");
                break;
            } else if (err != OK) {
                ALOGW("suspend: acquireBuffer returned err=%d", err);
                break;
            }

            --mNumFramesAvailable;

            mBufferQueue->releaseBuffer(item.mBuf, item.mFrameNumber,
                    EGL_NO_DISPLAY, EGL_NO_SYNC_KHR, item.mFence);
        }
        return;
    }

    mSuspended = false;

    if (mExecuting && mNumFramesAvailable == 0 && mRepeatBufferDeferred) {
        if (repeatLatestSubmittedBuffer_l()) {
            ALOGV("suspend/deferred repeatLatestSubmittedBuffer_l SUCCESS");

            mRepeatBufferDeferred = false;
        } else {
            ALOGV("suspend/deferred repeatLatestSubmittedBuffer_l FAILURE");
        }
    }
}

bool GraphicBufferSource::fillCodecBuffer_l() {
    CHECK(mExecuting && mNumFramesAvailable > 0);

    if (mSuspended) {
        return false;
    }

    int cbi = findAvailableCodecBuffer_l();
    if (cbi < 0) {
        // No buffers available, bail.
        ALOGV("fillCodecBuffer_l: no codec buffers, avail now %d",
                mNumFramesAvailable);
        return false;
    }

    ALOGV("fillCodecBuffer_l: acquiring buffer, avail=%d",
            mNumFramesAvailable);
    BufferQueue::BufferItem item;
    status_t err = mBufferQueue->acquireBuffer(&item, 0);
    if (err == BufferQueue::NO_BUFFER_AVAILABLE) {
        // shouldn't happen
        ALOGW("fillCodecBuffer_l: frame was not available");
        return false;
    } else if (err != OK) {
        // now what? fake end-of-stream?
        ALOGW("fillCodecBuffer_l: acquireBuffer returned err=%d", err);
        return false;
    }

    mNumFramesAvailable--;

    // Wait for it to become available.
    err = item.mFence->waitForever("GraphicBufferSource::fillCodecBuffer_l");
    if (err != OK) {
        ALOGW("failed to wait for buffer fence: %d", err);
        // keep going
    }

    // If this is the first time we're seeing this buffer, add it to our
    // slot table.
    if (item.mGraphicBuffer != NULL) {
        ALOGV("fillCodecBuffer_l: setting mBufferSlot %d", item.mBuf);
        mBufferSlot[item.mBuf] = item.mGraphicBuffer;
    }

    err = submitBuffer_l(item, cbi);
    if (err != OK) {
        ALOGV("submitBuffer_l failed, releasing bq buf %d", item.mBuf);
        mBufferQueue->releaseBuffer(item.mBuf, item.mFrameNumber,
                EGL_NO_DISPLAY, EGL_NO_SYNC_KHR, Fence::NO_FENCE);
    } else {
        ALOGV("buffer submitted (bq %d, cbi %d)", item.mBuf, cbi);
        setLatestSubmittedBuffer_l(item);
    }

    return true;
}

bool GraphicBufferSource::repeatLatestSubmittedBuffer_l() {
    CHECK(mExecuting && mNumFramesAvailable == 0);

    if (mLatestSubmittedBufferId < 0 || mSuspended) {
        return false;
    }
    if (mBufferSlot[mLatestSubmittedBufferId] == NULL) {
        // This can happen if the remote side disconnects, causing
        // onBuffersReleased() to NULL out our copy of the slots.  The
        // buffer is gone, so we have nothing to show.
        //
        // To be on the safe side we try to release the buffer.
        ALOGD("repeatLatestSubmittedBuffer_l: slot was NULL");
        mBufferQueue->releaseBuffer(
                mLatestSubmittedBufferId,
                mLatestSubmittedBufferFrameNum,
                EGL_NO_DISPLAY,
                EGL_NO_SYNC_KHR,
                Fence::NO_FENCE);
        mLatestSubmittedBufferId = -1;
        mLatestSubmittedBufferFrameNum = 0;
        return false;
    }

    int cbi = findAvailableCodecBuffer_l();
    if (cbi < 0) {
        // No buffers available, bail.
        ALOGV("repeatLatestSubmittedBuffer_l: no codec buffers.");
        return false;
    }

    BufferQueue::BufferItem item;
    item.mBuf = mLatestSubmittedBufferId;
    item.mFrameNumber = mLatestSubmittedBufferFrameNum;

    status_t err = submitBuffer_l(item, cbi);

    if (err != OK) {
        return false;
    }

    ++mLatestSubmittedBufferUseCount;

    return true;
}

void GraphicBufferSource::setLatestSubmittedBuffer_l(
        const BufferQueue::BufferItem &item) {
    ALOGV("setLatestSubmittedBuffer_l");

    if (mLatestSubmittedBufferId >= 0) {
        if (mLatestSubmittedBufferUseCount == 0) {
            mBufferQueue->releaseBuffer(
                    mLatestSubmittedBufferId,
                    mLatestSubmittedBufferFrameNum,
                    EGL_NO_DISPLAY,
                    EGL_NO_SYNC_KHR,
                    Fence::NO_FENCE);
        }
    }

    mLatestSubmittedBufferId = item.mBuf;
    mLatestSubmittedBufferFrameNum = item.mFrameNumber;
    mLatestSubmittedBufferUseCount = 1;
    mRepeatBufferDeferred = false;

    if (mReflector != NULL) {
        sp<AMessage> msg = new AMessage(kWhatRepeatLastFrame, mReflector->id());
        msg->setInt32("generation", ++mRepeatLastFrameGeneration);
        msg->post(mRepeatAfterUs);
    }
}

status_t GraphicBufferSource::signalEndOfInputStream() {
    Mutex::Autolock autoLock(mMutex);
    ALOGV("signalEndOfInputStream: exec=%d avail=%d eos=%d",
            mExecuting, mNumFramesAvailable, mEndOfStream);

    if (mEndOfStream) {
        ALOGE("EOS was already signaled");
        return INVALID_OPERATION;
    }

    // Set the end-of-stream flag.  If no frames are pending from the
    // BufferQueue, and a codec buffer is available, and we're executing,
    // we initiate the EOS from here.  Otherwise, we'll let
    // codecBufferEmptied() (or omxExecuting) do it.
    //
    // Note: if there are no pending frames and all codec buffers are
    // available, we *must* submit the EOS from here or we'll just
    // stall since no future events are expected.
    mEndOfStream = true;

    if (mExecuting && mNumFramesAvailable == 0) {
        submitEndOfInputStream_l();
    }

    return OK;
}

status_t GraphicBufferSource::submitBuffer_l(
        const BufferQueue::BufferItem &item, int cbi) {
    ALOGV("submitBuffer_l cbi=%d", cbi);
    CodecBuffer& codecBuffer(mCodecBuffers.editItemAt(cbi));
    codecBuffer.mGraphicBuffer = mBufferSlot[item.mBuf];
    codecBuffer.mBuf = item.mBuf;
    codecBuffer.mFrameNumber = item.mFrameNumber;

    OMX_BUFFERHEADERTYPE* header = codecBuffer.mHeader;
    CHECK(header->nAllocLen >= 4 + sizeof(buffer_handle_t));
    OMX_U8* data = header->pBuffer;
    const OMX_U32 type = kMetadataBufferTypeGrallocSource;
    buffer_handle_t handle = codecBuffer.mGraphicBuffer->handle;
    memcpy(data, &type, 4);
    memcpy(data + 4, &handle, sizeof(buffer_handle_t));

    status_t err = mNodeInstance->emptyDirectBuffer(header, 0,
            4 + sizeof(buffer_handle_t), OMX_BUFFERFLAG_ENDOFFRAME,
            item.mTimestamp / 1000);
    if (err != OK) {
        ALOGW("WARNING: emptyDirectBuffer failed: 0x%x", err);
        codecBuffer.mGraphicBuffer = NULL;
        return err;
    }

    ALOGV("emptyDirectBuffer succeeded, h=%p p=%p bufhandle=%p",
            header, header->pBuffer, handle);
    return OK;
}

void GraphicBufferSource::submitEndOfInputStream_l() {
    CHECK(mEndOfStream);
    if (mEndOfStreamSent) {
        ALOGV("EOS already sent");
        return;
    }

    int cbi = findAvailableCodecBuffer_l();
    if (cbi < 0) {
        ALOGV("submitEndOfInputStream_l: no codec buffers available");
        return;
    }

    // We reject any additional incoming graphic buffers, so there's no need
    // to stick a placeholder into codecBuffer.mGraphicBuffer to mark it as
    // in-use.
    CodecBuffer& codecBuffer(mCodecBuffers.editItemAt(cbi));

    OMX_BUFFERHEADERTYPE* header = codecBuffer.mHeader;
    if (EXTRA_CHECK) {
        // Guard against implementations that don't check nFilledLen.
        size_t fillLen = 4 + sizeof(buffer_handle_t);
        CHECK(header->nAllocLen >= fillLen);
        OMX_U8* data = header->pBuffer;
        memset(data, 0xcd, fillLen);
    }

    uint64_t timestamp = 0; // does this matter?

    status_t err = mNodeInstance->emptyDirectBuffer(header, /*offset*/ 0,
            /*length*/ 0, OMX_BUFFERFLAG_ENDOFFRAME | OMX_BUFFERFLAG_EOS,
            timestamp);
    if (err != OK) {
        ALOGW("emptyDirectBuffer EOS failed: 0x%x", err);
    } else {
        ALOGV("submitEndOfInputStream_l: buffer submitted, header=%p cbi=%d",
                header, cbi);
        mEndOfStreamSent = true;
    }
}

int GraphicBufferSource::findAvailableCodecBuffer_l() {
    CHECK(mCodecBuffers.size() > 0);

    for (int i = (int)mCodecBuffers.size() - 1; i>= 0; --i) {
        if (mCodecBuffers[i].mGraphicBuffer == NULL) {
            return i;
        }
    }
    return -1;
}

int GraphicBufferSource::findMatchingCodecBuffer_l(
        const OMX_BUFFERHEADERTYPE* header) {
    for (int i = (int)mCodecBuffers.size() - 1; i>= 0; --i) {
        if (mCodecBuffers[i].mHeader == header) {
            return i;
        }
    }
    return -1;
}

// BufferQueue::ConsumerListener callback
void GraphicBufferSource::onFrameAvailable() {
    Mutex::Autolock autoLock(mMutex);

    ALOGV("onFrameAvailable exec=%d avail=%d",
            mExecuting, mNumFramesAvailable);

    if (mEndOfStream || mSuspended) {
        if (mEndOfStream) {
            // This should only be possible if a new buffer was queued after
            // EOS was signaled, i.e. the app is misbehaving.

            ALOGW("onFrameAvailable: EOS is set, ignoring frame");
        } else {
            ALOGV("onFrameAvailable: suspended, ignoring frame");
        }

        BufferQueue::BufferItem item;
        status_t err = mBufferQueue->acquireBuffer(&item, 0);
        if (err == OK) {
            mBufferQueue->releaseBuffer(item.mBuf, item.mFrameNumber,
                    EGL_NO_DISPLAY, EGL_NO_SYNC_KHR, item.mFence);
        }
        return;
    }

    mNumFramesAvailable++;

    mRepeatBufferDeferred = false;
    ++mRepeatLastFrameGeneration;

    if (mExecuting) {
        fillCodecBuffer_l();
    }
}

// BufferQueue::ConsumerListener callback
void GraphicBufferSource::onBuffersReleased() {
    Mutex::Autolock lock(mMutex);

    uint32_t slotMask;
    if (mBufferQueue->getReleasedBuffers(&slotMask) != NO_ERROR) {
        ALOGW("onBuffersReleased: unable to get released buffer set");
        slotMask = 0xffffffff;
    }

    ALOGV("onBuffersReleased: 0x%08x", slotMask);

    for (int i = 0; i < BufferQueue::NUM_BUFFER_SLOTS; i++) {
        if ((slotMask & 0x01) != 0) {
            mBufferSlot[i] = NULL;
        }
        slotMask >>= 1;
    }
}

status_t GraphicBufferSource::setRepeatPreviousFrameDelayUs(
        int64_t repeatAfterUs) {
    Mutex::Autolock autoLock(mMutex);

    if (mExecuting || repeatAfterUs <= 0ll) {
        return INVALID_OPERATION;
    }

    mRepeatAfterUs = repeatAfterUs;

    return OK;
}

void GraphicBufferSource::onMessageReceived(const sp<AMessage> &msg) {
    switch (msg->what()) {
        case kWhatRepeatLastFrame:
        {
            Mutex::Autolock autoLock(mMutex);

            int32_t generation;
            CHECK(msg->findInt32("generation", &generation));

            if (generation != mRepeatLastFrameGeneration) {
                // stale
                break;
            }

            if (!mExecuting || mNumFramesAvailable > 0) {
                break;
            }

            bool success = repeatLatestSubmittedBuffer_l();

            if (success) {
                ALOGV("repeatLatestSubmittedBuffer_l SUCCESS");
            } else {
                ALOGV("repeatLatestSubmittedBuffer_l FAILURE");
                mRepeatBufferDeferred = true;
            }
            break;
        }

        default:
            TRESPASS();
    }
}

}  // namespace android
