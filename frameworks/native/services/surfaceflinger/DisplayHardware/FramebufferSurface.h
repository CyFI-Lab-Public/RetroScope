/*
 * Copyright (C) 2007 The Android Open Source Project
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

#ifndef ANDROID_SF_FRAMEBUFFER_SURFACE_H
#define ANDROID_SF_FRAMEBUFFER_SURFACE_H

#include <stdint.h>
#include <sys/types.h>

#include <gui/ConsumerBase.h>

#include "DisplaySurface.h"

// ---------------------------------------------------------------------------
namespace android {
// ---------------------------------------------------------------------------

class Rect;
class String8;
class HWComposer;

// ---------------------------------------------------------------------------

class FramebufferSurface : public ConsumerBase,
                           public DisplaySurface {
public:
    FramebufferSurface(HWComposer& hwc, int disp, const sp<IGraphicBufferConsumer>& consumer);

    virtual status_t beginFrame();
    virtual status_t prepareFrame(CompositionType compositionType);
    virtual status_t compositionComplete();
    virtual status_t advanceFrame();
    virtual void onFrameCommitted();

    // Implementation of DisplaySurface::dump(). Note that ConsumerBase also
    // has a non-virtual dump() with the same signature.
    virtual void dump(String8& result) const;

private:
    virtual ~FramebufferSurface() { }; // this class cannot be overloaded

    virtual void onFrameAvailable();
    virtual void freeBufferLocked(int slotIndex);

    virtual void dumpLocked(String8& result, const char* prefix) const;

    // nextBuffer waits for and then latches the next buffer from the
    // BufferQueue and releases the previously latched buffer to the
    // BufferQueue.  The new buffer is returned in the 'buffer' argument.
    status_t nextBuffer(sp<GraphicBuffer>& outBuffer, sp<Fence>& outFence);

    // mDisplayType must match one of the HWC display types
    int mDisplayType;

    // mCurrentBufferIndex is the slot index of the current buffer or
    // INVALID_BUFFER_SLOT to indicate that either there is no current buffer
    // or the buffer is not associated with a slot.
    int mCurrentBufferSlot;

    // mCurrentBuffer is the current buffer or NULL to indicate that there is
    // no current buffer.
    sp<GraphicBuffer> mCurrentBuffer;

    // Hardware composer, owned by SurfaceFlinger.
    HWComposer& mHwc;
};

// ---------------------------------------------------------------------------
}; // namespace android
// ---------------------------------------------------------------------------

#endif // ANDROID_SF_FRAMEBUFFER_SURFACE_H

