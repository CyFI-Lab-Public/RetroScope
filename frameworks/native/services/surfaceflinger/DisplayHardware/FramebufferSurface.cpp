/*
 **
 ** Copyright 2012 The Android Open Source Project
 **
 ** Licensed under the Apache License Version 2.0(the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing software
 ** distributed under the License is distributed on an "AS IS" BASIS
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>

#include <cutils/log.h>

#include <utils/String8.h>

#include <ui/Rect.h>

#include <EGL/egl.h>

#include <hardware/hardware.h>
#include <gui/Surface.h>
#include <gui/GraphicBufferAlloc.h>
#include <ui/GraphicBuffer.h>

#include "FramebufferSurface.h"
#include "HWComposer.h"

#ifndef NUM_FRAMEBUFFER_SURFACE_BUFFERS
#define NUM_FRAMEBUFFER_SURFACE_BUFFERS (2)
#endif

// ----------------------------------------------------------------------------
namespace android {
// ----------------------------------------------------------------------------

/*
 * This implements the (main) framebuffer management. This class is used
 * mostly by SurfaceFlinger, but also by command line GL application.
 *
 */

FramebufferSurface::FramebufferSurface(HWComposer& hwc, int disp,
        const sp<IGraphicBufferConsumer>& consumer) :
    ConsumerBase(consumer),
    mDisplayType(disp),
    mCurrentBufferSlot(-1),
    mCurrentBuffer(0),
    mHwc(hwc)
{
    mName = "FramebufferSurface";
    mConsumer->setConsumerName(mName);
    mConsumer->setConsumerUsageBits(GRALLOC_USAGE_HW_FB |
                                       GRALLOC_USAGE_HW_RENDER |
                                       GRALLOC_USAGE_HW_COMPOSER);
    mConsumer->setDefaultBufferFormat(mHwc.getFormat(disp));
    mConsumer->setDefaultBufferSize(mHwc.getWidth(disp),  mHwc.getHeight(disp));
    mConsumer->setDefaultMaxBufferCount(NUM_FRAMEBUFFER_SURFACE_BUFFERS);
}

status_t FramebufferSurface::beginFrame() {
    return NO_ERROR;
}

status_t FramebufferSurface::prepareFrame(CompositionType compositionType) {
    return NO_ERROR;
}

status_t FramebufferSurface::advanceFrame() {
    // Once we remove FB HAL support, we can call nextBuffer() from here
    // instead of using onFrameAvailable(). No real benefit, except it'll be
    // more like VirtualDisplaySurface.
    return NO_ERROR;
}

status_t FramebufferSurface::nextBuffer(sp<GraphicBuffer>& outBuffer, sp<Fence>& outFence) {
    Mutex::Autolock lock(mMutex);

    BufferQueue::BufferItem item;
    status_t err = acquireBufferLocked(&item, 0);
    if (err == BufferQueue::NO_BUFFER_AVAILABLE) {
        outBuffer = mCurrentBuffer;
        return NO_ERROR;
    } else if (err != NO_ERROR) {
        ALOGE("error acquiring buffer: %s (%d)", strerror(-err), err);
        return err;
    }

    // If the BufferQueue has freed and reallocated a buffer in mCurrentSlot
    // then we may have acquired the slot we already own.  If we had released
    // our current buffer before we call acquireBuffer then that release call
    // would have returned STALE_BUFFER_SLOT, and we would have called
    // freeBufferLocked on that slot.  Because the buffer slot has already
    // been overwritten with the new buffer all we have to do is skip the
    // releaseBuffer call and we should be in the same state we'd be in if we
    // had released the old buffer first.
    if (mCurrentBufferSlot != BufferQueue::INVALID_BUFFER_SLOT &&
        item.mBuf != mCurrentBufferSlot) {
        // Release the previous buffer.
        err = releaseBufferLocked(mCurrentBufferSlot, mCurrentBuffer,
                EGL_NO_DISPLAY, EGL_NO_SYNC_KHR);
        if (err < NO_ERROR) {
            ALOGE("error releasing buffer: %s (%d)", strerror(-err), err);
            return err;
        }
    }
    mCurrentBufferSlot = item.mBuf;
    mCurrentBuffer = mSlots[mCurrentBufferSlot].mGraphicBuffer;
    outFence = item.mFence;
    outBuffer = mCurrentBuffer;
    return NO_ERROR;
}

// Overrides ConsumerBase::onFrameAvailable(), does not call base class impl.
void FramebufferSurface::onFrameAvailable() {
    sp<GraphicBuffer> buf;
    sp<Fence> acquireFence;
    status_t err = nextBuffer(buf, acquireFence);
    if (err != NO_ERROR) {
        ALOGE("error latching nnext FramebufferSurface buffer: %s (%d)",
                strerror(-err), err);
        return;
    }
    err = mHwc.fbPost(mDisplayType, acquireFence, buf);
    if (err != NO_ERROR) {
        ALOGE("error posting framebuffer: %d", err);
    }
}

void FramebufferSurface::freeBufferLocked(int slotIndex) {
    ConsumerBase::freeBufferLocked(slotIndex);
    if (slotIndex == mCurrentBufferSlot) {
        mCurrentBufferSlot = BufferQueue::INVALID_BUFFER_SLOT;
    }
}

void FramebufferSurface::onFrameCommitted() {
    sp<Fence> fence = mHwc.getAndResetReleaseFence(mDisplayType);
    if (fence->isValid() &&
            mCurrentBufferSlot != BufferQueue::INVALID_BUFFER_SLOT) {
        status_t err = addReleaseFence(mCurrentBufferSlot,
                mCurrentBuffer, fence);
        ALOGE_IF(err, "setReleaseFenceFd: failed to add the fence: %s (%d)",
                strerror(-err), err);
    }
}

status_t FramebufferSurface::compositionComplete()
{
    return mHwc.fbCompositionComplete();
}

// Since DisplaySurface and ConsumerBase both have a method with this
// signature, results will vary based on the static pointer type the caller is
// using:
//   void dump(FrameBufferSurface* fbs, String8& s) {
//       // calls FramebufferSurface::dump()
//       fbs->dump(s);
//
//       // calls ConsumerBase::dump() since it is non-virtual
//       static_cast<ConsumerBase*>(fbs)->dump(s);
//
//       // calls FramebufferSurface::dump() since it is virtual
//       static_cast<DisplaySurface*>(fbs)->dump(s);
//   }
// To make sure that all of these end up doing the same thing, we just redirect
// to ConsumerBase::dump() here. It will take the internal lock, and then call
// virtual dumpLocked(), which is where the real work happens.
void FramebufferSurface::dump(String8& result) const {
    ConsumerBase::dump(result);
}

void FramebufferSurface::dumpLocked(String8& result, const char* prefix) const
{
    mHwc.fbDump(result);
    ConsumerBase::dumpLocked(result, prefix);
}

// ----------------------------------------------------------------------------
}; // namespace android
// ----------------------------------------------------------------------------
