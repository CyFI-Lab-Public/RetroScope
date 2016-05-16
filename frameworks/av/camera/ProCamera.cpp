/*
**
** Copyright (C) 2013, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

//#define LOG_NDEBUG 0
#define LOG_TAG "ProCamera"
#include <utils/Log.h>
#include <utils/threads.h>
#include <utils/Mutex.h>

#include <binder/IPCThreadState.h>
#include <binder/IServiceManager.h>
#include <binder/IMemory.h>

#include <camera/ProCamera.h>
#include <camera/IProCameraUser.h>
#include <camera/IProCameraCallbacks.h>

#include <gui/IGraphicBufferProducer.h>

#include <system/camera_metadata.h>

namespace android {

sp<ProCamera> ProCamera::connect(int cameraId)
{
    return CameraBaseT::connect(cameraId, String16(),
                                 ICameraService::USE_CALLING_UID);
}

ProCamera::ProCamera(int cameraId)
    : CameraBase(cameraId)
{
}

CameraTraits<ProCamera>::TCamConnectService CameraTraits<ProCamera>::fnConnectService =
        &ICameraService::connectPro;

ProCamera::~ProCamera()
{

}

/* IProCameraUser's implementation */

// callback from camera service
void ProCamera::notifyCallback(int32_t msgType, int32_t ext1, int32_t ext2)
{
    return CameraBaseT::notifyCallback(msgType, ext1, ext2);
}

void ProCamera::onLockStatusChanged(
                                 IProCameraCallbacks::LockStatus newLockStatus)
{
    ALOGV("%s: newLockStatus = %d", __FUNCTION__, newLockStatus);

    sp<ProCameraListener> listener;
    {
        Mutex::Autolock _l(mLock);
        listener = mListener;
    }
    if (listener != NULL) {
        switch (newLockStatus) {
            case IProCameraCallbacks::LOCK_ACQUIRED:
                listener->onLockAcquired();
                break;
            case IProCameraCallbacks::LOCK_RELEASED:
                listener->onLockReleased();
                break;
            case IProCameraCallbacks::LOCK_STOLEN:
                listener->onLockStolen();
                break;
            default:
                ALOGE("%s: Unknown lock status: %d",
                      __FUNCTION__, newLockStatus);
        }
    }
}

void ProCamera::onResultReceived(int32_t requestId, camera_metadata* result) {
    ALOGV("%s: requestId = %d, result = %p", __FUNCTION__, requestId, result);

    sp<ProCameraListener> listener;
    {
        Mutex::Autolock _l(mLock);
        listener = mListener;
    }

    CameraMetadata tmp(result);

    // Unblock waitForFrame(id) callers
    {
        Mutex::Autolock al(mWaitMutex);
        mMetadataReady = true;
        mLatestMetadata = tmp; // make copy
        mWaitCondition.broadcast();
    }

    result = tmp.release();

    if (listener != NULL) {
        listener->onResultReceived(requestId, result);
    } else {
        free_camera_metadata(result);
    }

}

status_t ProCamera::exclusiveTryLock()
{
    sp <IProCameraUser> c = mCamera;
    if (c == 0) return NO_INIT;

    return c->exclusiveTryLock();
}
status_t ProCamera::exclusiveLock()
{
    sp <IProCameraUser> c = mCamera;
    if (c == 0) return NO_INIT;

    return c->exclusiveLock();
}
status_t ProCamera::exclusiveUnlock()
{
    sp <IProCameraUser> c = mCamera;
    if (c == 0) return NO_INIT;

    return c->exclusiveUnlock();
}
bool ProCamera::hasExclusiveLock()
{
    sp <IProCameraUser> c = mCamera;
    if (c == 0) return NO_INIT;

    return c->hasExclusiveLock();
}

// Note that the callee gets a copy of the metadata.
int ProCamera::submitRequest(const struct camera_metadata* metadata,
                             bool streaming)
{
    sp <IProCameraUser> c = mCamera;
    if (c == 0) return NO_INIT;

    return c->submitRequest(const_cast<struct camera_metadata*>(metadata),
                            streaming);
}

status_t ProCamera::cancelRequest(int requestId)
{
    sp <IProCameraUser> c = mCamera;
    if (c == 0) return NO_INIT;

    return c->cancelRequest(requestId);
}

status_t ProCamera::deleteStream(int streamId)
{
    sp <IProCameraUser> c = mCamera;
    if (c == 0) return NO_INIT;

    status_t s = c->deleteStream(streamId);

    mStreams.removeItem(streamId);

    return s;
}

status_t ProCamera::createStream(int width, int height, int format,
                                 const sp<Surface>& surface,
                                 /*out*/
                                 int* streamId)
{
    *streamId = -1;

    ALOGV("%s: createStreamW %dx%d (fmt=0x%x)", __FUNCTION__, width, height,
                                                                       format);

    if (surface == 0) {
        return BAD_VALUE;
    }

    return createStream(width, height, format,
                        surface->getIGraphicBufferProducer(),
                        streamId);
}

status_t ProCamera::createStream(int width, int height, int format,
                                 const sp<IGraphicBufferProducer>& bufferProducer,
                                 /*out*/
                                 int* streamId) {
    *streamId = -1;

    ALOGV("%s: createStreamT %dx%d (fmt=0x%x)", __FUNCTION__, width, height,
                                                                       format);

    if (bufferProducer == 0) {
        return BAD_VALUE;
    }

    sp <IProCameraUser> c = mCamera;
    status_t stat = c->createStream(width, height, format, bufferProducer,
                                    streamId);

    if (stat == OK) {
        StreamInfo s(*streamId);

        mStreams.add(*streamId, s);
    }

    return stat;
}

status_t ProCamera::createStreamCpu(int width, int height, int format,
                                    int heapCount,
                                    /*out*/
                                    sp<CpuConsumer>* cpuConsumer,
                                    int* streamId) {
    return createStreamCpu(width, height, format, heapCount,
                           /*synchronousMode*/true,
                           cpuConsumer, streamId);
}

status_t ProCamera::createStreamCpu(int width, int height, int format,
                                    int heapCount,
                                    bool synchronousMode,
                                    /*out*/
                                    sp<CpuConsumer>* cpuConsumer,
                                    int* streamId)
{
    ALOGV("%s: createStreamW %dx%d (fmt=0x%x)", __FUNCTION__, width, height,
                                                                        format);

    *cpuConsumer = NULL;

    sp <IProCameraUser> c = mCamera;
    if (c == 0) return NO_INIT;

    sp<BufferQueue> bq = new BufferQueue();
    sp<CpuConsumer> cc = new CpuConsumer(bq, heapCount/*, synchronousMode*/);
    cc->setName(String8("ProCamera::mCpuConsumer"));

    sp<Surface> stc = new Surface(bq);

    status_t s = createStream(width, height, format,
                              stc->getIGraphicBufferProducer(),
                              streamId);

    if (s != OK) {
        ALOGE("%s: Failure to create stream %dx%d (fmt=0x%x)", __FUNCTION__,
                    width, height, format);
        return s;
    }

    sp<ProFrameListener> frameAvailableListener =
        new ProFrameListener(this, *streamId);

    getStreamInfo(*streamId).cpuStream = true;
    getStreamInfo(*streamId).cpuConsumer = cc;
    getStreamInfo(*streamId).synchronousMode = synchronousMode;
    getStreamInfo(*streamId).stc = stc;
    // for lifetime management
    getStreamInfo(*streamId).frameAvailableListener = frameAvailableListener;

    cc->setFrameAvailableListener(frameAvailableListener);

    *cpuConsumer = cc;

    return s;
}

camera_metadata* ProCamera::getCameraInfo(int cameraId) {
    ALOGV("%s: cameraId = %d", __FUNCTION__, cameraId);

    sp <IProCameraUser> c = mCamera;
    if (c == 0) return NULL;

    camera_metadata* ptr = NULL;
    status_t status = c->getCameraInfo(cameraId, &ptr);

    if (status != OK) {
        ALOGE("%s: Failed to get camera info, error = %d", __FUNCTION__, status);
    }

    return ptr;
}

status_t ProCamera::createDefaultRequest(int templateId,
                                             camera_metadata** request) const {
    ALOGV("%s: templateId = %d", __FUNCTION__, templateId);

    sp <IProCameraUser> c = mCamera;
    if (c == 0) return NO_INIT;

    return c->createDefaultRequest(templateId, request);
}

void ProCamera::onFrameAvailable(int streamId) {
    ALOGV("%s: streamId = %d", __FUNCTION__, streamId);

    sp<ProCameraListener> listener = mListener;
    StreamInfo& stream = getStreamInfo(streamId);

    if (listener.get() != NULL) {
        listener->onFrameAvailable(streamId, stream.cpuConsumer);
    }

    // Unblock waitForFrame(id) callers
    {
        Mutex::Autolock al(mWaitMutex);
        getStreamInfo(streamId).frameReady++;
        mWaitCondition.broadcast();
    }
}

int ProCamera::waitForFrameBuffer(int streamId) {
    status_t stat = BAD_VALUE;
    Mutex::Autolock al(mWaitMutex);

    StreamInfo& si = getStreamInfo(streamId);

    if (si.frameReady > 0) {
        int numFrames = si.frameReady;
        si.frameReady = 0;
        return numFrames;
    } else {
        while (true) {
            stat = mWaitCondition.waitRelative(mWaitMutex,
                                                mWaitTimeout);
            if (stat != OK) {
                ALOGE("%s: Error while waiting for frame buffer: %d",
                    __FUNCTION__, stat);
                return stat;
            }

            if (si.frameReady > 0) {
                int numFrames = si.frameReady;
                si.frameReady = 0;
                return numFrames;
            }
            // else it was some other stream that got unblocked
        }
    }

    return stat;
}

int ProCamera::dropFrameBuffer(int streamId, int count) {
    StreamInfo& si = getStreamInfo(streamId);

    if (!si.cpuStream) {
        return BAD_VALUE;
    } else if (count < 0) {
        return BAD_VALUE;
    }

    if (!si.synchronousMode) {
        ALOGW("%s: No need to drop frames on asynchronous streams,"
              " as asynchronous mode only keeps 1 latest frame around.",
              __FUNCTION__);
        return BAD_VALUE;
    }

    int numDropped = 0;
    for (int i = 0; i < count; ++i) {
        CpuConsumer::LockedBuffer buffer;
        if (si.cpuConsumer->lockNextBuffer(&buffer) != OK) {
            break;
        }

        si.cpuConsumer->unlockBuffer(buffer);
        numDropped++;
    }

    return numDropped;
}

status_t ProCamera::waitForFrameMetadata() {
    status_t stat = BAD_VALUE;
    Mutex::Autolock al(mWaitMutex);

    if (mMetadataReady) {
        return OK;
    } else {
        while (true) {
            stat = mWaitCondition.waitRelative(mWaitMutex,
                                               mWaitTimeout);

            if (stat != OK) {
                ALOGE("%s: Error while waiting for metadata: %d",
                        __FUNCTION__, stat);
                return stat;
            }

            if (mMetadataReady) {
                mMetadataReady = false;
                return OK;
            }
            // else it was some other stream or metadata
        }
    }

    return stat;
}

CameraMetadata ProCamera::consumeFrameMetadata() {
    Mutex::Autolock al(mWaitMutex);

    // Destructive: Subsequent calls return empty metadatas
    CameraMetadata tmp = mLatestMetadata;
    mLatestMetadata.clear();

    return tmp;
}

ProCamera::StreamInfo& ProCamera::getStreamInfo(int streamId) {
    return mStreams.editValueFor(streamId);
}

}; // namespace android
