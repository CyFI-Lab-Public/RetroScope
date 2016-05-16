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

#ifndef ANDROID_LAYER_H
#define ANDROID_LAYER_H

#include <stdint.h>
#include <sys/types.h>

#include <EGL/egl.h>
#include <EGL/eglext.h>

#include <utils/RefBase.h>
#include <utils/String8.h>
#include <utils/Timers.h>

#include <ui/GraphicBuffer.h>
#include <ui/PixelFormat.h>
#include <ui/Region.h>

#include <gui/ISurfaceComposerClient.h>

#include <private/gui/LayerState.h>

#include "FrameTracker.h"
#include "Client.h"
#include "SurfaceFlinger.h"
#include "SurfaceFlingerConsumer.h"
#include "SurfaceTextureLayer.h"
#include "Transform.h"

#include "DisplayHardware/HWComposer.h"
#include "DisplayHardware/FloatRect.h"
#include "RenderEngine/Mesh.h"
#include "RenderEngine/Texture.h"

namespace android {

// ---------------------------------------------------------------------------

class Client;
class Colorizer;
class DisplayDevice;
class GraphicBuffer;
class SurfaceFlinger;

// ---------------------------------------------------------------------------

/*
 * A new BufferQueue and a new SurfaceFlingerConsumer are created when the
 * Layer is first referenced.
 *
 * This also implements onFrameAvailable(), which notifies SurfaceFlinger
 * that new data has arrived.
 */
class Layer : public SurfaceFlingerConsumer::FrameAvailableListener {
    static int32_t sSequence;

public:
    mutable bool contentDirty;
    // regions below are in window-manager space
    Region visibleRegion;
    Region coveredRegion;
    Region visibleNonTransparentRegion;
    int32_t sequence;

    enum { // flags for doTransaction()
        eDontUpdateGeometryState = 0x00000001,
        eVisibleRegion = 0x00000002,
    };

    struct Geometry {
        uint32_t w;
        uint32_t h;
        Rect crop;
        inline bool operator ==(const Geometry& rhs) const {
            return (w == rhs.w && h == rhs.h && crop == rhs.crop);
        }
        inline bool operator !=(const Geometry& rhs) const {
            return !operator ==(rhs);
        }
    };

    struct State {
        Geometry active;
        Geometry requested;
        uint32_t z;
        uint32_t layerStack;
        uint8_t alpha;
        uint8_t flags;
        uint8_t reserved[2];
        int32_t sequence; // changes when visible regions can change
        Transform transform;
        // the transparentRegion hint is a bit special, it's latched only
        // when we receive a buffer -- this is because it's "content"
        // dependent.
        Region activeTransparentRegion;
        Region requestedTransparentRegion;
    };

    // -----------------------------------------------------------------------

    Layer(SurfaceFlinger* flinger, const sp<Client>& client,
            const String8& name, uint32_t w, uint32_t h, uint32_t flags);

    virtual ~Layer();

    // the this layer's size and format
    status_t setBuffers(uint32_t w, uint32_t h, PixelFormat format, uint32_t flags);

    // modify current state
    bool setPosition(float x, float y);
    bool setLayer(uint32_t z);
    bool setSize(uint32_t w, uint32_t h);
    bool setAlpha(uint8_t alpha);
    bool setMatrix(const layer_state_t::matrix22_t& matrix);
    bool setTransparentRegionHint(const Region& transparent);
    bool setFlags(uint8_t flags, uint8_t mask);
    bool setCrop(const Rect& crop);
    bool setLayerStack(uint32_t layerStack);

    uint32_t getTransactionFlags(uint32_t flags);
    uint32_t setTransactionFlags(uint32_t flags);

    void computeGeometry(const sp<const DisplayDevice>& hw, Mesh& mesh) const;
    Rect computeBounds() const;

    sp<IBinder> getHandle();
    sp<IGraphicBufferProducer> getBufferQueue() const;
    const String8& getName() const;

    // -----------------------------------------------------------------------
    // Virtuals

    virtual const char* getTypeId() const { return "Layer"; }

    /*
     * isOpaque - true if this surface is opaque
     */
    virtual bool isOpaque() const;

    /*
     * isSecure - true if this surface is secure, that is if it prevents
     * screenshots or VNC servers.
     */
    virtual bool isSecure() const           { return mSecure; }

    /*
     * isProtected - true if the layer may contain protected content in the
     * GRALLOC_USAGE_PROTECTED sense.
     */
    virtual bool isProtected() const;

    /*
     * isVisible - true if this layer is visible, false otherwise
     */
    virtual bool isVisible() const;

    /*
     * isFixedSize - true if content has a fixed size
     */
    virtual bool isFixedSize() const;

protected:
    /*
     * onDraw - draws the surface.
     */
    virtual void onDraw(const sp<const DisplayDevice>& hw, const Region& clip) const;

public:
    // -----------------------------------------------------------------------

    void setGeometry(const sp<const DisplayDevice>& hw,
            HWComposer::HWCLayerInterface& layer);
    void setPerFrameData(const sp<const DisplayDevice>& hw,
            HWComposer::HWCLayerInterface& layer);
    void setAcquireFence(const sp<const DisplayDevice>& hw,
            HWComposer::HWCLayerInterface& layer);

    /*
     * called after page-flip
     */
    void onLayerDisplayed(const sp<const DisplayDevice>& hw,
            HWComposer::HWCLayerInterface* layer);

    /*
     * called before composition.
     * returns true if the layer has pending updates.
     */
    bool onPreComposition();

    /*
     *  called after composition.
     */
    void onPostComposition();

    /*
     * draw - performs some global clipping optimizations
     * and calls onDraw().
     */
    void draw(const sp<const DisplayDevice>& hw, const Region& clip) const;
    void draw(const sp<const DisplayDevice>& hw);

    /*
     * doTransaction - process the transaction. This is a good place to figure
     * out which attributes of the surface have changed.
     */
    uint32_t doTransaction(uint32_t transactionFlags);

    /*
     * setVisibleRegion - called to set the new visible region. This gives
     * a chance to update the new visible region or record the fact it changed.
     */
    void setVisibleRegion(const Region& visibleRegion);

    /*
     * setCoveredRegion - called when the covered region changes. The covered
     * region corresponds to any area of the surface that is covered
     * (transparently or not) by another surface.
     */
    void setCoveredRegion(const Region& coveredRegion);

    /*
     * setVisibleNonTransparentRegion - called when the visible and
     * non-transparent region changes.
     */
    void setVisibleNonTransparentRegion(const Region&
            visibleNonTransparentRegion);

    /*
     * latchBuffer - called each time the screen is redrawn and returns whether
     * the visible regions need to be recomputed (this is a fairly heavy
     * operation, so this should be set only if needed). Typically this is used
     * to figure out if the content or size of a surface has changed.
     */
    Region latchBuffer(bool& recomputeVisibleRegions);

    /*
     * called with the state lock when the surface is removed from the
     * current list
     */
    void onRemoved();


    // Updates the transform hint in our SurfaceFlingerConsumer to match
    // the current orientation of the display device.
    void updateTransformHint(const sp<const DisplayDevice>& hw) const;

    /*
     * returns the rectangle that crops the content of the layer and scales it
     * to the layer's size.
     */
    Rect getContentCrop() const;

    // -----------------------------------------------------------------------

    void clearWithOpenGL(const sp<const DisplayDevice>& hw, const Region& clip) const;
    void setFiltering(bool filtering);
    bool getFiltering() const;

    // only for debugging
    inline const sp<GraphicBuffer>& getActiveBuffer() const { return mActiveBuffer; }

    inline  const State&    getDrawingState() const { return mDrawingState; }
    inline  const State&    getCurrentState() const { return mCurrentState; }
    inline  State&          getCurrentState()       { return mCurrentState; }


    /* always call base class first */
    void dump(String8& result, Colorizer& colorizer) const;
    void dumpStats(String8& result) const;
    void clearStats();
    void logFrameStats();

protected:
    // constant
    sp<SurfaceFlinger> mFlinger;

    virtual void onFirstRef();

    /*
     * Trivial class, used to ensure that mFlinger->onLayerDestroyed(mLayer)
     * is called.
     */
    class LayerCleaner {
        sp<SurfaceFlinger> mFlinger;
        wp<Layer> mLayer;
    protected:
        ~LayerCleaner();
    public:
        LayerCleaner(const sp<SurfaceFlinger>& flinger, const sp<Layer>& layer);
    };


private:
    // Interface implementation for SurfaceFlingerConsumer::FrameAvailableListener
    virtual void onFrameAvailable();

    void commitTransaction();

    // needsLinearFiltering - true if this surface's state requires filtering
    bool needsFiltering(const sp<const DisplayDevice>& hw) const;

    uint32_t getEffectiveUsage(uint32_t usage) const;
    FloatRect computeCrop(const sp<const DisplayDevice>& hw) const;
    bool isCropped() const;
    static bool getOpacityForFormat(uint32_t format);

    // drawing
    void clearWithOpenGL(const sp<const DisplayDevice>& hw, const Region& clip,
            float r, float g, float b, float alpha) const;
    void drawWithOpenGL(const sp<const DisplayDevice>& hw, const Region& clip) const;


    // -----------------------------------------------------------------------

    // constants
    sp<SurfaceFlingerConsumer> mSurfaceFlingerConsumer;
    sp<BufferQueue> mBufferQueue;
    uint32_t mTextureName;
    bool mPremultipliedAlpha;
    String8 mName;
    mutable bool mDebug;
    PixelFormat mFormat;
    bool mOpaqueLayer;

    // these are protected by an external lock
    State mCurrentState;
    State mDrawingState;
    volatile int32_t mTransactionFlags;

    // thread-safe
    volatile int32_t mQueuedFrames;
    FrameTracker mFrameTracker;

    // main thread
    sp<GraphicBuffer> mActiveBuffer;
    Rect mCurrentCrop;
    uint32_t mCurrentTransform;
    uint32_t mCurrentScalingMode;
    bool mCurrentOpacity;
    bool mRefreshPending;
    bool mFrameLatencyNeeded;
    // Whether filtering is forced on or not
    bool mFiltering;
    // Whether filtering is needed b/c of the drawingstate
    bool mNeedsFiltering;
    // The mesh used to draw the layer in GLES composition mode
    mutable Mesh mMesh;
    // The mesh used to draw the layer in GLES composition mode
    mutable Texture mTexture;

    // page-flip thread (currently main thread)
    bool mSecure; // no screenshots
    bool mProtectedByApp; // application requires protected path to external sink

    // protected by mLock
    mutable Mutex mLock;
    // Set to true once we've returned this surface's handle
    mutable bool mHasSurface;
    const wp<Client> mClientRef;
};

// ---------------------------------------------------------------------------

}; // namespace android

#endif // ANDROID_LAYER_H
