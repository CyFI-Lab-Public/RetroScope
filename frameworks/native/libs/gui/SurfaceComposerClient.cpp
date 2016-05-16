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

#define LOG_TAG "SurfaceComposerClient"

#include <stdint.h>
#include <sys/types.h>

#include <utils/Errors.h>
#include <utils/Log.h>
#include <utils/Singleton.h>
#include <utils/SortedVector.h>
#include <utils/String8.h>
#include <utils/threads.h>

#include <binder/IMemory.h>
#include <binder/IServiceManager.h>

#include <ui/DisplayInfo.h>

#include <gui/CpuConsumer.h>
#include <gui/IGraphicBufferProducer.h>
#include <gui/ISurfaceComposer.h>
#include <gui/ISurfaceComposerClient.h>
#include <gui/SurfaceComposerClient.h>

#include <private/gui/ComposerService.h>
#include <private/gui/LayerState.h>

namespace android {
// ---------------------------------------------------------------------------

ANDROID_SINGLETON_STATIC_INSTANCE(ComposerService);

ComposerService::ComposerService()
: Singleton<ComposerService>() {
    Mutex::Autolock _l(mLock);
    connectLocked();
}

void ComposerService::connectLocked() {
    const String16 name("SurfaceFlinger");
    while (getService(name, &mComposerService) != NO_ERROR) {
        usleep(250000);
    }
    assert(mComposerService != NULL);

    // Create the death listener.
    class DeathObserver : public IBinder::DeathRecipient {
        ComposerService& mComposerService;
        virtual void binderDied(const wp<IBinder>& who) {
            ALOGW("ComposerService remote (surfaceflinger) died [%p]",
                  who.unsafe_get());
            mComposerService.composerServiceDied();
        }
     public:
        DeathObserver(ComposerService& mgr) : mComposerService(mgr) { }
    };

    mDeathObserver = new DeathObserver(*const_cast<ComposerService*>(this));
    mComposerService->asBinder()->linkToDeath(mDeathObserver);
}

/*static*/ sp<ISurfaceComposer> ComposerService::getComposerService() {
    ComposerService& instance = ComposerService::getInstance();
    Mutex::Autolock _l(instance.mLock);
    if (instance.mComposerService == NULL) {
        ComposerService::getInstance().connectLocked();
        assert(instance.mComposerService != NULL);
        ALOGD("ComposerService reconnected");
    }
    return instance.mComposerService;
}

void ComposerService::composerServiceDied()
{
    Mutex::Autolock _l(mLock);
    mComposerService = NULL;
    mDeathObserver = NULL;
}

// ---------------------------------------------------------------------------

static inline
int compare_type(const ComposerState& lhs, const ComposerState& rhs) {
    if (lhs.client < rhs.client)  return -1;
    if (lhs.client > rhs.client)  return 1;
    if (lhs.state.surface < rhs.state.surface)  return -1;
    if (lhs.state.surface > rhs.state.surface)  return 1;
    return 0;
}

static inline
int compare_type(const DisplayState& lhs, const DisplayState& rhs) {
    return compare_type(lhs.token, rhs.token);
}

class Composer : public Singleton<Composer>
{
    friend class Singleton<Composer>;

    mutable Mutex               mLock;
    SortedVector<ComposerState> mComposerStates;
    SortedVector<DisplayState > mDisplayStates;
    uint32_t                    mForceSynchronous;
    uint32_t                    mTransactionNestCount;
    bool                        mAnimation;

    Composer() : Singleton<Composer>(),
        mForceSynchronous(0), mTransactionNestCount(0),
        mAnimation(false)
    { }

    void openGlobalTransactionImpl();
    void closeGlobalTransactionImpl(bool synchronous);
    void setAnimationTransactionImpl();

    layer_state_t* getLayerStateLocked(
            const sp<SurfaceComposerClient>& client, const sp<IBinder>& id);

    DisplayState& getDisplayStateLocked(const sp<IBinder>& token);

public:
    sp<IBinder> createDisplay(const String8& displayName, bool secure);
    void destroyDisplay(const sp<IBinder>& display);
    sp<IBinder> getBuiltInDisplay(int32_t id);

    status_t setPosition(const sp<SurfaceComposerClient>& client, const sp<IBinder>& id,
            float x, float y);
    status_t setSize(const sp<SurfaceComposerClient>& client, const sp<IBinder>& id,
            uint32_t w, uint32_t h);
    status_t setLayer(const sp<SurfaceComposerClient>& client, const sp<IBinder>& id,
            int32_t z);
    status_t setFlags(const sp<SurfaceComposerClient>& client, const sp<IBinder>& id,
            uint32_t flags, uint32_t mask);
    status_t setTransparentRegionHint(
            const sp<SurfaceComposerClient>& client, const sp<IBinder>& id,
            const Region& transparentRegion);
    status_t setAlpha(const sp<SurfaceComposerClient>& client, const sp<IBinder>& id,
            float alpha);
    status_t setMatrix(const sp<SurfaceComposerClient>& client, const sp<IBinder>& id,
            float dsdx, float dtdx, float dsdy, float dtdy);
    status_t setOrientation(int orientation);
    status_t setCrop(const sp<SurfaceComposerClient>& client, const sp<IBinder>& id,
            const Rect& crop);
    status_t setLayerStack(const sp<SurfaceComposerClient>& client,
            const sp<IBinder>& id, uint32_t layerStack);

    void setDisplaySurface(const sp<IBinder>& token,
            const sp<IGraphicBufferProducer>& bufferProducer);
    void setDisplayLayerStack(const sp<IBinder>& token, uint32_t layerStack);
    void setDisplayProjection(const sp<IBinder>& token,
            uint32_t orientation,
            const Rect& layerStackRect,
            const Rect& displayRect);

    static void setAnimationTransaction() {
        Composer::getInstance().setAnimationTransactionImpl();
    }

    static void openGlobalTransaction() {
        Composer::getInstance().openGlobalTransactionImpl();
    }

    static void closeGlobalTransaction(bool synchronous) {
        Composer::getInstance().closeGlobalTransactionImpl(synchronous);
    }
};

ANDROID_SINGLETON_STATIC_INSTANCE(Composer);

// ---------------------------------------------------------------------------

sp<IBinder> Composer::createDisplay(const String8& displayName, bool secure) {
    return ComposerService::getComposerService()->createDisplay(displayName,
            secure);
}

void Composer::destroyDisplay(const sp<IBinder>& display) {
    return ComposerService::getComposerService()->destroyDisplay(display);
}

sp<IBinder> Composer::getBuiltInDisplay(int32_t id) {
    return ComposerService::getComposerService()->getBuiltInDisplay(id);
}

void Composer::openGlobalTransactionImpl() {
    { // scope for the lock
        Mutex::Autolock _l(mLock);
        mTransactionNestCount += 1;
    }
}

void Composer::closeGlobalTransactionImpl(bool synchronous) {
    sp<ISurfaceComposer> sm(ComposerService::getComposerService());

    Vector<ComposerState> transaction;
    Vector<DisplayState> displayTransaction;
    uint32_t flags = 0;

    { // scope for the lock
        Mutex::Autolock _l(mLock);
        mForceSynchronous |= synchronous;
        if (!mTransactionNestCount) {
            ALOGW("At least one call to closeGlobalTransaction() was not matched by a prior "
                    "call to openGlobalTransaction().");
        } else if (--mTransactionNestCount) {
            return;
        }

        transaction = mComposerStates;
        mComposerStates.clear();

        displayTransaction = mDisplayStates;
        mDisplayStates.clear();

        if (mForceSynchronous) {
            flags |= ISurfaceComposer::eSynchronous;
        }
        if (mAnimation) {
            flags |= ISurfaceComposer::eAnimation;
        }

        mForceSynchronous = false;
        mAnimation = false;
    }

   sm->setTransactionState(transaction, displayTransaction, flags);
}

void Composer::setAnimationTransactionImpl() {
    Mutex::Autolock _l(mLock);
    mAnimation = true;
}

layer_state_t* Composer::getLayerStateLocked(
        const sp<SurfaceComposerClient>& client, const sp<IBinder>& id) {

    ComposerState s;
    s.client = client->mClient;
    s.state.surface = id;

    ssize_t index = mComposerStates.indexOf(s);
    if (index < 0) {
        // we don't have it, add an initialized layer_state to our list
        index = mComposerStates.add(s);
    }

    ComposerState* const out = mComposerStates.editArray();
    return &(out[index].state);
}

status_t Composer::setPosition(const sp<SurfaceComposerClient>& client,
        const sp<IBinder>& id, float x, float y) {
    Mutex::Autolock _l(mLock);
    layer_state_t* s = getLayerStateLocked(client, id);
    if (!s)
        return BAD_INDEX;
    s->what |= layer_state_t::ePositionChanged;
    s->x = x;
    s->y = y;
    return NO_ERROR;
}

status_t Composer::setSize(const sp<SurfaceComposerClient>& client,
        const sp<IBinder>& id, uint32_t w, uint32_t h) {
    Mutex::Autolock _l(mLock);
    layer_state_t* s = getLayerStateLocked(client, id);
    if (!s)
        return BAD_INDEX;
    s->what |= layer_state_t::eSizeChanged;
    s->w = w;
    s->h = h;

    // Resizing a surface makes the transaction synchronous.
    mForceSynchronous = true;

    return NO_ERROR;
}

status_t Composer::setLayer(const sp<SurfaceComposerClient>& client,
        const sp<IBinder>& id, int32_t z) {
    Mutex::Autolock _l(mLock);
    layer_state_t* s = getLayerStateLocked(client, id);
    if (!s)
        return BAD_INDEX;
    s->what |= layer_state_t::eLayerChanged;
    s->z = z;
    return NO_ERROR;
}

status_t Composer::setFlags(const sp<SurfaceComposerClient>& client,
        const sp<IBinder>& id, uint32_t flags,
        uint32_t mask) {
    Mutex::Autolock _l(mLock);
    layer_state_t* s = getLayerStateLocked(client, id);
    if (!s)
        return BAD_INDEX;
    s->what |= layer_state_t::eVisibilityChanged;
    s->flags &= ~mask;
    s->flags |= (flags & mask);
    s->mask |= mask;
    return NO_ERROR;
}

status_t Composer::setTransparentRegionHint(
        const sp<SurfaceComposerClient>& client, const sp<IBinder>& id,
        const Region& transparentRegion) {
    Mutex::Autolock _l(mLock);
    layer_state_t* s = getLayerStateLocked(client, id);
    if (!s)
        return BAD_INDEX;
    s->what |= layer_state_t::eTransparentRegionChanged;
    s->transparentRegion = transparentRegion;
    return NO_ERROR;
}

status_t Composer::setAlpha(const sp<SurfaceComposerClient>& client,
        const sp<IBinder>& id, float alpha) {
    Mutex::Autolock _l(mLock);
    layer_state_t* s = getLayerStateLocked(client, id);
    if (!s)
        return BAD_INDEX;
    s->what |= layer_state_t::eAlphaChanged;
    s->alpha = alpha;
    return NO_ERROR;
}

status_t Composer::setLayerStack(const sp<SurfaceComposerClient>& client,
        const sp<IBinder>& id, uint32_t layerStack) {
    Mutex::Autolock _l(mLock);
    layer_state_t* s = getLayerStateLocked(client, id);
    if (!s)
        return BAD_INDEX;
    s->what |= layer_state_t::eLayerStackChanged;
    s->layerStack = layerStack;
    return NO_ERROR;
}

status_t Composer::setMatrix(const sp<SurfaceComposerClient>& client,
        const sp<IBinder>& id, float dsdx, float dtdx,
        float dsdy, float dtdy) {
    Mutex::Autolock _l(mLock);
    layer_state_t* s = getLayerStateLocked(client, id);
    if (!s)
        return BAD_INDEX;
    s->what |= layer_state_t::eMatrixChanged;
    layer_state_t::matrix22_t matrix;
    matrix.dsdx = dsdx;
    matrix.dtdx = dtdx;
    matrix.dsdy = dsdy;
    matrix.dtdy = dtdy;
    s->matrix = matrix;
    return NO_ERROR;
}

status_t Composer::setCrop(const sp<SurfaceComposerClient>& client,
        const sp<IBinder>& id, const Rect& crop) {
    Mutex::Autolock _l(mLock);
    layer_state_t* s = getLayerStateLocked(client, id);
    if (!s)
        return BAD_INDEX;
    s->what |= layer_state_t::eCropChanged;
    s->crop = crop;
    return NO_ERROR;
}

// ---------------------------------------------------------------------------

DisplayState& Composer::getDisplayStateLocked(const sp<IBinder>& token) {
    DisplayState s;
    s.token = token;
    ssize_t index = mDisplayStates.indexOf(s);
    if (index < 0) {
        // we don't have it, add an initialized layer_state to our list
        s.what = 0;
        index = mDisplayStates.add(s);
    }
    return mDisplayStates.editItemAt(index);
}

void Composer::setDisplaySurface(const sp<IBinder>& token,
        const sp<IGraphicBufferProducer>& bufferProducer) {
    Mutex::Autolock _l(mLock);
    DisplayState& s(getDisplayStateLocked(token));
    s.surface = bufferProducer;
    s.what |= DisplayState::eSurfaceChanged;
}

void Composer::setDisplayLayerStack(const sp<IBinder>& token,
        uint32_t layerStack) {
    Mutex::Autolock _l(mLock);
    DisplayState& s(getDisplayStateLocked(token));
    s.layerStack = layerStack;
    s.what |= DisplayState::eLayerStackChanged;
}

void Composer::setDisplayProjection(const sp<IBinder>& token,
        uint32_t orientation,
        const Rect& layerStackRect,
        const Rect& displayRect) {
    Mutex::Autolock _l(mLock);
    DisplayState& s(getDisplayStateLocked(token));
    s.orientation = orientation;
    s.viewport = layerStackRect;
    s.frame = displayRect;
    s.what |= DisplayState::eDisplayProjectionChanged;
    mForceSynchronous = true; // TODO: do we actually still need this?
}

// ---------------------------------------------------------------------------

SurfaceComposerClient::SurfaceComposerClient()
    : mStatus(NO_INIT), mComposer(Composer::getInstance())
{
}

void SurfaceComposerClient::onFirstRef() {
    sp<ISurfaceComposer> sm(ComposerService::getComposerService());
    if (sm != 0) {
        sp<ISurfaceComposerClient> conn = sm->createConnection();
        if (conn != 0) {
            mClient = conn;
            mStatus = NO_ERROR;
        }
    }
}

SurfaceComposerClient::~SurfaceComposerClient() {
    dispose();
}

status_t SurfaceComposerClient::initCheck() const {
    return mStatus;
}

sp<IBinder> SurfaceComposerClient::connection() const {
    return (mClient != 0) ? mClient->asBinder() : 0;
}

status_t SurfaceComposerClient::linkToComposerDeath(
        const sp<IBinder::DeathRecipient>& recipient,
        void* cookie, uint32_t flags) {
    sp<ISurfaceComposer> sm(ComposerService::getComposerService());
    return sm->asBinder()->linkToDeath(recipient, cookie, flags);
}

void SurfaceComposerClient::dispose() {
    // this can be called more than once.
    sp<ISurfaceComposerClient> client;
    Mutex::Autolock _lm(mLock);
    if (mClient != 0) {
        client = mClient; // hold ref while lock is held
        mClient.clear();
    }
    mStatus = NO_INIT;
}

sp<SurfaceControl> SurfaceComposerClient::createSurface(
        const String8& name,
        uint32_t w,
        uint32_t h,
        PixelFormat format,
        uint32_t flags)
{
    sp<SurfaceControl> sur;
    if (mStatus == NO_ERROR) {
        sp<IBinder> handle;
        sp<IGraphicBufferProducer> gbp;
        status_t err = mClient->createSurface(name, w, h, format, flags,
                &handle, &gbp);
        ALOGE_IF(err, "SurfaceComposerClient::createSurface error %s", strerror(-err));
        if (err == NO_ERROR) {
            sur = new SurfaceControl(this, handle, gbp);
        }
    }
    return sur;
}

sp<IBinder> SurfaceComposerClient::createDisplay(const String8& displayName,
        bool secure) {
    return Composer::getInstance().createDisplay(displayName, secure);
}

void SurfaceComposerClient::destroyDisplay(const sp<IBinder>& display) {
    Composer::getInstance().destroyDisplay(display);
}

sp<IBinder> SurfaceComposerClient::getBuiltInDisplay(int32_t id) {
    return Composer::getInstance().getBuiltInDisplay(id);
}

status_t SurfaceComposerClient::destroySurface(const sp<IBinder>& sid) {
    if (mStatus != NO_ERROR)
        return mStatus;
    status_t err = mClient->destroySurface(sid);
    return err;
}

inline Composer& SurfaceComposerClient::getComposer() {
    return mComposer;
}

// ----------------------------------------------------------------------------

void SurfaceComposerClient::openGlobalTransaction() {
    Composer::openGlobalTransaction();
}

void SurfaceComposerClient::closeGlobalTransaction(bool synchronous) {
    Composer::closeGlobalTransaction(synchronous);
}

void SurfaceComposerClient::setAnimationTransaction() {
    Composer::setAnimationTransaction();
}

// ----------------------------------------------------------------------------

status_t SurfaceComposerClient::setCrop(const sp<IBinder>& id, const Rect& crop) {
    return getComposer().setCrop(this, id, crop);
}

status_t SurfaceComposerClient::setPosition(const sp<IBinder>& id, float x, float y) {
    return getComposer().setPosition(this, id, x, y);
}

status_t SurfaceComposerClient::setSize(const sp<IBinder>& id, uint32_t w, uint32_t h) {
    return getComposer().setSize(this, id, w, h);
}

status_t SurfaceComposerClient::setLayer(const sp<IBinder>& id, int32_t z) {
    return getComposer().setLayer(this, id, z);
}

status_t SurfaceComposerClient::hide(const sp<IBinder>& id) {
    return getComposer().setFlags(this, id,
            layer_state_t::eLayerHidden,
            layer_state_t::eLayerHidden);
}

status_t SurfaceComposerClient::show(const sp<IBinder>& id) {
    return getComposer().setFlags(this, id,
            0,
            layer_state_t::eLayerHidden);
}

status_t SurfaceComposerClient::setFlags(const sp<IBinder>& id, uint32_t flags,
        uint32_t mask) {
    return getComposer().setFlags(this, id, flags, mask);
}

status_t SurfaceComposerClient::setTransparentRegionHint(const sp<IBinder>& id,
        const Region& transparentRegion) {
    return getComposer().setTransparentRegionHint(this, id, transparentRegion);
}

status_t SurfaceComposerClient::setAlpha(const sp<IBinder>& id, float alpha) {
    return getComposer().setAlpha(this, id, alpha);
}

status_t SurfaceComposerClient::setLayerStack(const sp<IBinder>& id, uint32_t layerStack) {
    return getComposer().setLayerStack(this, id, layerStack);
}

status_t SurfaceComposerClient::setMatrix(const sp<IBinder>& id, float dsdx, float dtdx,
        float dsdy, float dtdy) {
    return getComposer().setMatrix(this, id, dsdx, dtdx, dsdy, dtdy);
}

// ----------------------------------------------------------------------------

void SurfaceComposerClient::setDisplaySurface(const sp<IBinder>& token,
        const sp<IGraphicBufferProducer>& bufferProducer) {
    Composer::getInstance().setDisplaySurface(token, bufferProducer);
}

void SurfaceComposerClient::setDisplayLayerStack(const sp<IBinder>& token,
        uint32_t layerStack) {
    Composer::getInstance().setDisplayLayerStack(token, layerStack);
}

void SurfaceComposerClient::setDisplayProjection(const sp<IBinder>& token,
        uint32_t orientation,
        const Rect& layerStackRect,
        const Rect& displayRect) {
    Composer::getInstance().setDisplayProjection(token, orientation,
            layerStackRect, displayRect);
}

// ----------------------------------------------------------------------------

status_t SurfaceComposerClient::getDisplayInfo(
        const sp<IBinder>& display, DisplayInfo* info)
{
    return ComposerService::getComposerService()->getDisplayInfo(display, info);
}

void SurfaceComposerClient::blankDisplay(const sp<IBinder>& token) {
    ComposerService::getComposerService()->blank(token);
}

void SurfaceComposerClient::unblankDisplay(const sp<IBinder>& token) {
    ComposerService::getComposerService()->unblank(token);
}

// ----------------------------------------------------------------------------

status_t ScreenshotClient::capture(
        const sp<IBinder>& display,
        const sp<IGraphicBufferProducer>& producer,
        uint32_t reqWidth, uint32_t reqHeight,
        uint32_t minLayerZ, uint32_t maxLayerZ) {
    sp<ISurfaceComposer> s(ComposerService::getComposerService());
    if (s == NULL) return NO_INIT;
    return s->captureScreen(display, producer,
            reqWidth, reqHeight, minLayerZ, maxLayerZ);
}

ScreenshotClient::ScreenshotClient()
    : mHaveBuffer(false) {
    memset(&mBuffer, 0, sizeof(mBuffer));
}

ScreenshotClient::~ScreenshotClient() {
    ScreenshotClient::release();
}

sp<CpuConsumer> ScreenshotClient::getCpuConsumer() const {
    if (mCpuConsumer == NULL) {
        mBufferQueue = new BufferQueue();
        mCpuConsumer = new CpuConsumer(mBufferQueue, 1);
        mCpuConsumer->setName(String8("ScreenshotClient"));
    }
    return mCpuConsumer;
}

status_t ScreenshotClient::update(const sp<IBinder>& display,
        uint32_t reqWidth, uint32_t reqHeight,
        uint32_t minLayerZ, uint32_t maxLayerZ) {
    sp<ISurfaceComposer> s(ComposerService::getComposerService());
    if (s == NULL) return NO_INIT;
    sp<CpuConsumer> cpuConsumer = getCpuConsumer();

    if (mHaveBuffer) {
        mCpuConsumer->unlockBuffer(mBuffer);
        memset(&mBuffer, 0, sizeof(mBuffer));
        mHaveBuffer = false;
    }

    status_t err = s->captureScreen(display, mBufferQueue,
            reqWidth, reqHeight, minLayerZ, maxLayerZ);

    if (err == NO_ERROR) {
        err = mCpuConsumer->lockNextBuffer(&mBuffer);
        if (err == NO_ERROR) {
            mHaveBuffer = true;
        }
    }
    return err;
}

status_t ScreenshotClient::update(const sp<IBinder>& display) {
    return ScreenshotClient::update(display, 0, 0, 0, -1UL);
}

status_t ScreenshotClient::update(const sp<IBinder>& display,
        uint32_t reqWidth, uint32_t reqHeight) {
    return ScreenshotClient::update(display, reqWidth, reqHeight, 0, -1UL);
}

void ScreenshotClient::release() {
    if (mHaveBuffer) {
        mCpuConsumer->unlockBuffer(mBuffer);
        memset(&mBuffer, 0, sizeof(mBuffer));
        mHaveBuffer = false;
    }
    mCpuConsumer.clear();
}

void const* ScreenshotClient::getPixels() const {
    return mBuffer.data;
}

uint32_t ScreenshotClient::getWidth() const {
    return mBuffer.width;
}

uint32_t ScreenshotClient::getHeight() const {
    return mBuffer.height;
}

PixelFormat ScreenshotClient::getFormat() const {
    return mBuffer.format;
}

uint32_t ScreenshotClient::getStride() const {
    return mBuffer.stride;
}

size_t ScreenshotClient::getSize() const {
    return mBuffer.stride * mBuffer.height * bytesPerPixel(mBuffer.format);
}

// ----------------------------------------------------------------------------
}; // namespace android
