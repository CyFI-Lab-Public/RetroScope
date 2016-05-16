/*
**
** Copyright (C) 2008, The Android Open Source Project
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

#ifndef ANDROID_SERVERS_CAMERA_CAMERASERVICE_H
#define ANDROID_SERVERS_CAMERA_CAMERASERVICE_H

#include <utils/Vector.h>
#include <binder/AppOpsManager.h>
#include <binder/BinderService.h>
#include <binder/IAppOpsCallback.h>
#include <camera/ICameraService.h>
#include <hardware/camera.h>

#include <camera/ICamera.h>
#include <camera/ICameraClient.h>
#include <camera/IProCameraUser.h>
#include <camera/IProCameraCallbacks.h>
#include <camera/camera2/ICameraDeviceUser.h>
#include <camera/camera2/ICameraDeviceCallbacks.h>

#include <camera/ICameraServiceListener.h>

/* This needs to be increased if we can have more cameras */
#define MAX_CAMERAS 2

namespace android {

extern volatile int32_t gLogLevel;

class MemoryHeapBase;
class MediaPlayer;

class CameraService :
    public BinderService<CameraService>,
    public BnCameraService,
    public IBinder::DeathRecipient,
    public camera_module_callbacks_t
{
    friend class BinderService<CameraService>;
public:
    class Client;
    class BasicClient;

    // Implementation of BinderService<T>
    static char const* getServiceName() { return "media.camera"; }

                        CameraService();
    virtual             ~CameraService();

    /////////////////////////////////////////////////////////////////////
    // HAL Callbacks
    virtual void        onDeviceStatusChanged(int cameraId,
                                              int newStatus);

    /////////////////////////////////////////////////////////////////////
    // ICameraService
    virtual int32_t     getNumberOfCameras();
    virtual status_t    getCameraInfo(int cameraId,
                                      struct CameraInfo* cameraInfo);
    virtual status_t    getCameraCharacteristics(int cameraId,
                                                 CameraMetadata* cameraInfo);

    virtual status_t connect(const sp<ICameraClient>& cameraClient, int cameraId,
            const String16& clientPackageName, int clientUid,
            /*out*/
            sp<ICamera>& device);

    virtual status_t connectPro(const sp<IProCameraCallbacks>& cameraCb,
            int cameraId, const String16& clientPackageName, int clientUid,
            /*out*/
            sp<IProCameraUser>& device);

    virtual status_t connectDevice(
            const sp<ICameraDeviceCallbacks>& cameraCb,
            int cameraId,
            const String16& clientPackageName,
            int clientUid,
            /*out*/
            sp<ICameraDeviceUser>& device);

    virtual status_t    addListener(const sp<ICameraServiceListener>& listener);
    virtual status_t    removeListener(
                                    const sp<ICameraServiceListener>& listener);

    // Extra permissions checks
    virtual status_t    onTransact(uint32_t code, const Parcel& data,
                                   Parcel* reply, uint32_t flags);

    virtual status_t    dump(int fd, const Vector<String16>& args);

    /////////////////////////////////////////////////////////////////////
    // Client functionality
    virtual void        removeClientByRemote(const wp<IBinder>& remoteBinder);

    enum sound_kind {
        SOUND_SHUTTER = 0,
        SOUND_RECORDING = 1,
        NUM_SOUNDS
    };

    void                loadSound();
    void                playSound(sound_kind kind);
    void                releaseSound();

    /////////////////////////////////////////////////////////////////////
    // CameraDeviceFactory functionality
    int                 getDeviceVersion(int cameraId, int* facing = NULL);


    /////////////////////////////////////////////////////////////////////
    // CameraClient functionality

    // returns plain pointer of client. Note that mClientLock should be acquired to
    // prevent the client from destruction. The result can be NULL.
    virtual BasicClient* getClientByIdUnsafe(int cameraId);
    virtual Mutex*      getClientLockById(int cameraId);

    class BasicClient : public virtual RefBase {
    public:
        virtual status_t initialize(camera_module_t *module) = 0;

        virtual void          disconnect() = 0;

        // because we can't virtually inherit IInterface, which breaks
        // virtual inheritance
        virtual sp<IBinder> asBinderWrapper() = 0;

        // Return the remote callback binder object (e.g. IProCameraCallbacks)
        sp<IBinder>     getRemote() {
            return mRemoteBinder;
        }

        virtual status_t      dump(int fd, const Vector<String16>& args) = 0;

    protected:
        BasicClient(const sp<CameraService>& cameraService,
                const sp<IBinder>& remoteCallback,
                const String16& clientPackageName,
                int cameraId,
                int cameraFacing,
                int clientPid,
                uid_t clientUid,
                int servicePid);
	BasicClient();

        virtual ~BasicClient();

        // the instance is in the middle of destruction. When this is set,
        // the instance should not be accessed from callback.
        // CameraService's mClientLock should be acquired to access this.
        // - subclasses should set this to true in their destructors.
        bool                            mDestructionStarted;

        // these are initialized in the constructor.
        sp<CameraService>               mCameraService;  // immutable after constructor
        int                             mCameraId;       // immutable after constructor
        int                             mCameraFacing;   // immutable after constructor
        const String16                  mClientPackageName;
        pid_t                           mClientPid;
        uid_t                           mClientUid;      // immutable after constructor
        pid_t                           mServicePid;     // immutable after constructor

        // - The app-side Binder interface to receive callbacks from us
        sp<IBinder>                     mRemoteBinder;   // immutable after constructor

        // permissions management
        status_t                        startCameraOps();
        status_t                        finishCameraOps();

        // Notify client about a fatal error
        virtual void                    notifyError() = 0;
    private:
        AppOpsManager                   mAppOpsManager;

        class OpsCallback : public BnAppOpsCallback {
        public:
            OpsCallback(wp<BasicClient> client);
            virtual void opChanged(int32_t op, const String16& packageName);

        private:
            wp<BasicClient> mClient;

        }; // class OpsCallback

        sp<OpsCallback> mOpsCallback;
        // Track whether startCameraOps was called successfully, to avoid
        // finishing what we didn't start.
        bool            mOpsActive;

        // IAppOpsCallback interface, indirected through opListener
        virtual void opChanged(int32_t op, const String16& packageName);
    }; // class BasicClient

    class Client : public BnCamera, public BasicClient
    {
    public:
        typedef ICameraClient TCamCallbacks;

        // ICamera interface (see ICamera for details)
        virtual void          disconnect();
        virtual status_t      connect(const sp<ICameraClient>& client) = 0;
        virtual status_t      lock() = 0;
        virtual status_t      unlock() = 0;
        virtual status_t      setPreviewTarget(const sp<IGraphicBufferProducer>& bufferProducer)=0;
        virtual void          setPreviewCallbackFlag(int flag) = 0;
        virtual status_t      setPreviewCallbackTarget(
                const sp<IGraphicBufferProducer>& callbackProducer) = 0;
        virtual status_t      startPreview() = 0;
        virtual void          stopPreview() = 0;
        virtual bool          previewEnabled() = 0;
        virtual status_t      storeMetaDataInBuffers(bool enabled) = 0;
        virtual status_t      startRecording() = 0;
        virtual void          stopRecording() = 0;
        virtual bool          recordingEnabled() = 0;
        virtual void          releaseRecordingFrame(const sp<IMemory>& mem) = 0;
        virtual status_t      autoFocus() = 0;
        virtual status_t      cancelAutoFocus() = 0;
        virtual status_t      takePicture(int msgType) = 0;
        virtual status_t      setParameters(const String8& params) = 0;
        virtual String8       getParameters() const = 0;
        virtual status_t      sendCommand(int32_t cmd, int32_t arg1, int32_t arg2) = 0;

        // Interface used by CameraService
        Client();
        Client(const sp<CameraService>& cameraService,
                const sp<ICameraClient>& cameraClient,
                const String16& clientPackageName,
                int cameraId,
                int cameraFacing,
                int clientPid,
                uid_t clientUid,
                int servicePid);
        ~Client();

        // return our camera client
        const sp<ICameraClient>&    getRemoteCallback() {
            return mRemoteCallback;
        }

        virtual sp<IBinder> asBinderWrapper() {
            return asBinder();
        }

    protected:
        static Mutex*        getClientLockFromCookie(void* user);
        // convert client from cookie. Client lock should be acquired before getting Client.
        static Client*       getClientFromCookie(void* user);

        virtual void         notifyError();

        // Initialized in constructor

        // - The app-side Binder interface to receive callbacks from us
        sp<ICameraClient>               mRemoteCallback;

    }; // class Client

    class ProClient : public BnProCameraUser, public BasicClient {
    public:
        typedef IProCameraCallbacks TCamCallbacks;

        ProClient();

        ProClient(const sp<CameraService>& cameraService,
                const sp<IProCameraCallbacks>& remoteCallback,
                const String16& clientPackageName,
                int cameraId,
                int cameraFacing,
                int clientPid,
                uid_t clientUid,
                int servicePid);

        virtual ~ProClient();

        const sp<IProCameraCallbacks>& getRemoteCallback() {
            return mRemoteCallback;
        }

        /***
            IProCamera implementation
         ***/
        virtual status_t      connect(const sp<IProCameraCallbacks>& callbacks)
                                                                            = 0;
        virtual status_t      exclusiveTryLock() = 0;
        virtual status_t      exclusiveLock() = 0;
        virtual status_t      exclusiveUnlock() = 0;

        virtual bool          hasExclusiveLock() = 0;

        // Note that the callee gets a copy of the metadata.
        virtual int           submitRequest(camera_metadata_t* metadata,
                                            bool streaming = false) = 0;
        virtual status_t      cancelRequest(int requestId) = 0;

        // Callbacks from camera service
        virtual void          onExclusiveLockStolen() = 0;

    protected:
        virtual void          notifyError();

        sp<IProCameraCallbacks> mRemoteCallback;
    }; // class ProClient

private:

    // Delay-load the Camera HAL module
    virtual void onFirstRef();

    // Step 1. Check if we can connect, before we acquire the service lock.
    status_t            validateConnect(int cameraId,
                                        /*inout*/
                                        int& clientUid) const;

    // Step 2. Check if we can connect, after we acquire the service lock.
    bool                canConnectUnsafe(int cameraId,
                                         const String16& clientPackageName,
                                         const sp<IBinder>& remoteCallback,
                                         /*out*/
                                         sp<BasicClient> &client);

    // When connection is successful, initialize client and track its death
    status_t            connectFinishUnsafe(const sp<BasicClient>& client,
                                            const sp<IBinder>& remoteCallback);

    virtual sp<BasicClient>  getClientByRemote(const wp<IBinder>& cameraClient);

    Mutex               mServiceLock;
    // either a Client or CameraDeviceClient
    wp<BasicClient>     mClient[MAX_CAMERAS];  // protected by mServiceLock
    Mutex               mClientLock[MAX_CAMERAS]; // prevent Client destruction inside callbacks
    int                 mNumberOfCameras;

    typedef wp<ProClient> weak_pro_client_ptr;
    Vector<weak_pro_client_ptr> mProClientList[MAX_CAMERAS];

    // needs to be called with mServiceLock held
    sp<BasicClient>     findClientUnsafe(const wp<IBinder>& cameraClient, int& outIndex);
    sp<ProClient>       findProClientUnsafe(
                                     const wp<IBinder>& cameraCallbacksRemote);

    // atomics to record whether the hardware is allocated to some client.
    volatile int32_t    mBusy[MAX_CAMERAS];
    void                setCameraBusy(int cameraId);
    void                setCameraFree(int cameraId);

    // sounds
    MediaPlayer*        newMediaPlayer(const char *file);

    Mutex               mSoundLock;
    sp<MediaPlayer>     mSoundPlayer[NUM_SOUNDS];
    int                 mSoundRef;  // reference count (release all MediaPlayer when 0)

    camera_module_t *mModule;

    Vector<sp<ICameraServiceListener> >
                        mListenerList;

    // guard only mStatusList and the broadcasting of ICameraServiceListener
    mutable Mutex       mStatusMutex;
    ICameraServiceListener::Status
                        mStatusList[MAX_CAMERAS];

    // Read the current status (locks mStatusMutex)
    ICameraServiceListener::Status
                        getStatus(int cameraId) const;

    typedef Vector<ICameraServiceListener::Status> StatusVector;
    // Broadcast the new status if it changed (locks the service mutex)
    void                updateStatus(
                            ICameraServiceListener::Status status,
                            int32_t cameraId,
                            const StatusVector *rejectSourceStates = NULL);

    // IBinder::DeathRecipient implementation
    virtual void        binderDied(const wp<IBinder> &who);

    // Helpers

    bool                isValidCameraId(int cameraId);
};

} // namespace android

#endif
