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

#define ATRACE_TAG ATRACE_TAG_GRAPHICS
//#define LOG_NDEBUG 0

#include "SurfaceFlingerConsumer.h"

#include <private/gui/SyncFeatures.h>

#include <utils/Trace.h>
#include <utils/Errors.h>

namespace android {

// ---------------------------------------------------------------------------

status_t SurfaceFlingerConsumer::updateTexImage(BufferRejecter* rejecter)
{
    ATRACE_CALL();
    ALOGV("updateTexImage");
    Mutex::Autolock lock(mMutex);

    if (mAbandoned) {
        ALOGE("updateTexImage: GLConsumer is abandoned!");
        return NO_INIT;
    }

    // Make sure the EGL state is the same as in previous calls.
    status_t err = checkAndUpdateEglStateLocked();
    if (err != NO_ERROR) {
        return err;
    }

    BufferQueue::BufferItem item;

    // Acquire the next buffer.
    // In asynchronous mode the list is guaranteed to be one buffer
    // deep, while in synchronous mode we use the oldest buffer.
    err = acquireBufferLocked(&item, computeExpectedPresent());
    if (err != NO_ERROR) {
        if (err == BufferQueue::NO_BUFFER_AVAILABLE) {
            err = NO_ERROR;
        } else if (err == BufferQueue::PRESENT_LATER) {
            // return the error, without logging
        } else {
            ALOGE("updateTexImage: acquire failed: %s (%d)",
                strerror(-err), err);
        }
        return err;
    }


    // We call the rejecter here, in case the caller has a reason to
    // not accept this buffer.  This is used by SurfaceFlinger to
    // reject buffers which have the wrong size
    int buf = item.mBuf;
    if (rejecter && rejecter->reject(mSlots[buf].mGraphicBuffer, item)) {
        releaseBufferLocked(buf, mSlots[buf].mGraphicBuffer, EGL_NO_SYNC_KHR);
        return NO_ERROR;
    }

    // Release the previous buffer.
    err = updateAndReleaseLocked(item);
    if (err != NO_ERROR) {
        return err;
    }

    if (!SyncFeatures::getInstance().useNativeFenceSync()) {
        // Bind the new buffer to the GL texture.
        //
        // Older devices require the "implicit" synchronization provided
        // by glEGLImageTargetTexture2DOES, which this method calls.  Newer
        // devices will either call this in Layer::onDraw, or (if it's not
        // a GL-composited layer) not at all.
        err = bindTextureImageLocked();
    }

    return err;
}

status_t SurfaceFlingerConsumer::bindTextureImage()
{
    Mutex::Autolock lock(mMutex);

    return bindTextureImageLocked();
}

status_t SurfaceFlingerConsumer::acquireBufferLocked(
        BufferQueue::BufferItem *item, nsecs_t presentWhen) {
    status_t result = GLConsumer::acquireBufferLocked(item, presentWhen);
    if (result == NO_ERROR) {
        mTransformToDisplayInverse = item->mTransformToDisplayInverse;
    }
    return result;
}

bool SurfaceFlingerConsumer::getTransformToDisplayInverse() const {
    return mTransformToDisplayInverse;
}

// We need to determine the time when a buffer acquired now will be
// displayed.  This can be calculated:
//   time when previous buffer's actual-present fence was signaled
//    + current display refresh rate * HWC latency
//    + a little extra padding
//
// Buffer producers are expected to set their desired presentation time
// based on choreographer time stamps, which (coming from vsync events)
// will be slightly later then the actual-present timing.  If we get a
// desired-present time that is unintentionally a hair after the next
// vsync, we'll hold the frame when we really want to display it.  We
// want to use an expected-presentation time that is slightly late to
// avoid this sort of edge case.
nsecs_t SurfaceFlingerConsumer::computeExpectedPresent()
{
    // Don't yet have an easy way to get actual buffer flip time for
    // the specific display, so use the current time.  This is typically
    // 1.3ms past the vsync event time.
    const nsecs_t prevVsync = systemTime(CLOCK_MONOTONIC);

    // Given a SurfaceFlinger reference, and information about what display
    // we're destined for, we could query the HWC for the refresh rate.  This
    // could change over time, e.g. we could switch to 24fps for a movie.
    // For now, assume 60fps.
    //const nsecs_t vsyncPeriod =
    //        getHwComposer().getRefreshPeriod(HWC_DISPLAY_PRIMARY);
    const nsecs_t vsyncPeriod = 16700000;

    // The HWC doesn't currently have a way to report additional latency.
    // Assume that whatever we submit now will appear on the next flip,
    // i.e. 1 frame of latency w.r.t. the previous flip.
    const uint32_t hwcLatency = 1;

    // A little extra padding to compensate for slack between actual vsync
    // time and vsync event receipt.  Currently not needed since we're
    // using "now" instead of a vsync time.
    const nsecs_t extraPadding = 0;

    // Total it up.
    return prevVsync + hwcLatency * vsyncPeriod + extraPadding;
}

// ---------------------------------------------------------------------------
}; // namespace android

