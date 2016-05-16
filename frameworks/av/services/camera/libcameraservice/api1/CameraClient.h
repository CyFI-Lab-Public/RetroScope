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

#ifndef ANDROID_SERVERS_CAMERA_CAMERACLIENT_H
#define ANDROID_SERVERS_CAMERA_CAMERACLIENT_H

#include "CameraService.h"

namespace android {

class MemoryHeapBase;
class CameraHardwareInterface;

/**
 * Interface between android.hardware.Camera API and Camera HAL device for version
 * CAMERA_DEVICE_API_VERSION_1_0.
 */

class CameraClient : public CameraService::Client
{
public:
    // ICamera interface (see ICamera for details)
    virtual void            disconnect();
    virtual status_t        connect(const sp<ICameraClient>& client);
    virtual status_t        lock();
    virtual status_t        unlock();
    virtual status_t        setPreviewTarget(const sp<IGraphicBufferProducer>& bufferProducer);
    virtual void            setPreviewCallbackFlag(int flag);
    virtual status_t        setPreviewCallbackTarget(
            const sp<IGraphicBufferProducer>& callbackProducer);
    virtual status_t        startPreview();
    virtual void            stopPreview();
    virtual bool            previewEnabled();
    virtual status_t        storeMetaDataInBuffers(bool enabled);
    virtual status_t        startRecording();
    virtual void            stopRecording();
    virtual bool            recordingEnabled();
    virtual void            releaseRecordingFrame(const sp<IMemory>& mem);
    virtual status_t        autoFocus();
    virtual status_t        cancelAutoFocus();
    virtual status_t        takePicture(int msgType);
    virtual status_t        setParameters(const String8& params);
    virtual String8         getParameters() const;
    virtual status_t        sendCommand(int32_t cmd, int32_t arg1, int32_t arg2);

    // Interface used by CameraService
    CameraClient(const sp<CameraService>& cameraService,
            const sp<ICameraClient>& cameraClient,
            const String16& clientPackageName,
            int cameraId,
            int cameraFacing,
            int clientPid,
            int clientUid,
            int servicePid);
    ~CameraClient();

    status_t initialize(camera_module_t *module);

    status_t dump(int fd, const Vector<String16>& args);

private:

    // check whether the calling process matches mClientPid.
    status_t                checkPid() const;
    status_t                checkPidAndHardware() const;  // also check mHardware != 0

    // these are internal functions used to set up preview buffers
    status_t                registerPreviewBuffers();

    // camera operation mode
    enum camera_mode {
        CAMERA_PREVIEW_MODE   = 0,  // frame automatically released
        CAMERA_RECORDING_MODE = 1,  // frame has to be explicitly released by releaseRecordingFrame()
    };
    // these are internal functions used for preview/recording
    status_t                startCameraMode(camera_mode mode);
    status_t                startPreviewMode();
    status_t                startRecordingMode();

    // internal function used by sendCommand to enable/disable shutter sound.
    status_t                enableShutterSound(bool enable);

    // these are static callback functions
    static void             notifyCallback(int32_t msgType, int32_t ext1, int32_t ext2, void* user);
    static void             dataCallback(int32_t msgType, const sp<IMemory>& dataPtr,
            camera_frame_metadata_t *metadata, void* user);
    static void             dataCallbackTimestamp(nsecs_t timestamp, int32_t msgType, const sp<IMemory>& dataPtr, void* user);
    // handlers for messages
    void                    handleShutter(void);
    void                    handlePreviewData(int32_t msgType, const sp<IMemory>& mem,
            camera_frame_metadata_t *metadata);
    void                    handlePostview(const sp<IMemory>& mem);
    void                    handleRawPicture(const sp<IMemory>& mem);
    void                    handleCompressedPicture(const sp<IMemory>& mem);
    void                    handleGenericNotify(int32_t msgType, int32_t ext1, int32_t ext2);
    void                    handleGenericData(int32_t msgType, const sp<IMemory>& dataPtr,
            camera_frame_metadata_t *metadata);
    void                    handleGenericDataTimestamp(nsecs_t timestamp, int32_t msgType, const sp<IMemory>& dataPtr);

    void                    copyFrameAndPostCopiedFrame(
        int32_t msgType,
        const sp<ICameraClient>& client,
        const sp<IMemoryHeap>& heap,
        size_t offset, size_t size,
        camera_frame_metadata_t *metadata);

    int                     getOrientation(int orientation, bool mirror);

    status_t                setPreviewWindow(
        const sp<IBinder>& binder,
        const sp<ANativeWindow>& window);


    // these are initialized in the constructor.
    sp<CameraHardwareInterface>     mHardware;       // cleared after disconnect()
    int                             mPreviewCallbackFlag;
    int                             mOrientation;     // Current display orientation
    bool                            mPlayShutterSound;

    // Ensures atomicity among the public methods
    mutable Mutex                   mLock;
    // This is a binder of Surface or Surface.
    sp<IBinder>                     mSurface;
    sp<ANativeWindow>               mPreviewWindow;

    // If the user want us to return a copy of the preview frame (instead
    // of the original one), we allocate mPreviewBuffer and reuse it if possible.
    sp<MemoryHeapBase>              mPreviewBuffer;

    // We need to avoid the deadlock when the incoming command thread and
    // the CameraHardwareInterface callback thread both want to grab mLock.
    // An extra flag is used to tell the callback thread that it should stop
    // trying to deliver the callback messages if the client is not
    // interested in it anymore. For example, if the client is calling
    // stopPreview(), the preview frame messages do not need to be delivered
    // anymore.

    // This function takes the same parameter as the enableMsgType() and
    // disableMsgType() functions in CameraHardwareInterface.
    void                    enableMsgType(int32_t msgType);
    void                    disableMsgType(int32_t msgType);
    volatile int32_t        mMsgEnabled;

    // This function keeps trying to grab mLock, or give up if the message
    // is found to be disabled. It returns true if mLock is grabbed.
    bool                    lockIfMessageWanted(int32_t msgType);
};

}

#endif
