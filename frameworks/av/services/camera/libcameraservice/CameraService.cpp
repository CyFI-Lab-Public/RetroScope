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

#define LOG_TAG "CameraService"
//#define LOG_NDEBUG 0

#include <stdio.h>
#include <sys/types.h>
#include <pthread.h>

#include <binder/AppOpsManager.h>
#include <binder/IPCThreadState.h>
#include <binder/IServiceManager.h>
#include <binder/MemoryBase.h>
#include <binder/MemoryHeapBase.h>
#include <cutils/atomic.h>
#include <cutils/properties.h>
#include <gui/Surface.h>
#include <hardware/hardware.h>
#include <media/AudioSystem.h>
#include <media/mediaplayer.h>
#include <utils/Errors.h>
#include <utils/Log.h>
#include <utils/String16.h>

#include "CameraService.h"
#include "api1/CameraClient.h"
#include "api1/Camera2Client.h"
#include "api_pro/ProCamera2Client.h"
#include "api2/CameraDeviceClient.h"
#include "utils/CameraTraces.h"
#include "CameraDeviceFactory.h"

namespace android {

// ----------------------------------------------------------------------------
// Logging support -- this is for debugging only
// Use "adb shell dumpsys media.camera -v 1" to change it.
volatile int32_t gLogLevel = 0;

#define LOG1(...) ALOGD_IF(gLogLevel >= 1, __VA_ARGS__);
#define LOG2(...) ALOGD_IF(gLogLevel >= 2, __VA_ARGS__);

static void setLogLevel(int level) {
    android_atomic_write(level, &gLogLevel);
}

// ----------------------------------------------------------------------------

static int getCallingPid() {
    return IPCThreadState::self()->getCallingPid();
}

static int getCallingUid() {
    return IPCThreadState::self()->getCallingUid();
}

extern "C" {
static void camera_device_status_change(
        const struct camera_module_callbacks* callbacks,
        int camera_id,
        int new_status) {
    sp<CameraService> cs = const_cast<CameraService*>(
                                static_cast<const CameraService*>(callbacks));

    cs->onDeviceStatusChanged(
        camera_id,
        new_status);
}
} // extern "C"

// ----------------------------------------------------------------------------

// This is ugly and only safe if we never re-create the CameraService, but
// should be ok for now.
static CameraService *gCameraService;

CameraService::CameraService()
    :mSoundRef(0), mModule(0)
{
    ALOGI("CameraService started (pid=%d)", getpid());
    gCameraService = this;

    for (size_t i = 0; i < MAX_CAMERAS; ++i) {
        mStatusList[i] = ICameraServiceListener::STATUS_PRESENT;
    }

    this->camera_device_status_change = android::camera_device_status_change;
}

void CameraService::onFirstRef()
{
    LOG1("CameraService::onFirstRef");

    BnCameraService::onFirstRef();

    if (hw_get_module(CAMERA_HARDWARE_MODULE_ID,
                (const hw_module_t **)&mModule) < 0) {
        ALOGE("Could not load camera HAL module");
        mNumberOfCameras = 0;
    }
    else {
        ALOGI("Loaded \"%s\" camera module", mModule->common.name);
        mNumberOfCameras = mModule->get_number_of_cameras();
        if (mNumberOfCameras > MAX_CAMERAS) {
            ALOGE("Number of cameras(%d) > MAX_CAMERAS(%d).",
                    mNumberOfCameras, MAX_CAMERAS);
            mNumberOfCameras = MAX_CAMERAS;
        }
        for (int i = 0; i < mNumberOfCameras; i++) {
            setCameraFree(i);
        }

        if (mModule->common.module_api_version >=
                CAMERA_MODULE_API_VERSION_2_1) {
            mModule->set_callbacks(this);
        }

        CameraDeviceFactory::registerService(this);
    }
}

CameraService::~CameraService() {
    for (int i = 0; i < mNumberOfCameras; i++) {
        if (mBusy[i]) {
            ALOGE("camera %d is still in use in destructor!", i);
        }
    }

    gCameraService = NULL;
}

void CameraService::onDeviceStatusChanged(int cameraId,
                                          int newStatus)
{
    ALOGI("%s: Status changed for cameraId=%d, newStatus=%d", __FUNCTION__,
          cameraId, newStatus);

    if (cameraId < 0 || cameraId >= MAX_CAMERAS) {
        ALOGE("%s: Bad camera ID %d", __FUNCTION__, cameraId);
        return;
    }

    if ((int)getStatus(cameraId) == newStatus) {
        ALOGE("%s: State transition to the same status 0x%x not allowed",
              __FUNCTION__, (uint32_t)newStatus);
        return;
    }

    /* don't do this in updateStatus
       since it is also called from connect and we could get into a deadlock */
    if (newStatus == CAMERA_DEVICE_STATUS_NOT_PRESENT) {
        Vector<sp<BasicClient> > clientsToDisconnect;
        {
           Mutex::Autolock al(mServiceLock);

           /* Find all clients that we need to disconnect */
           sp<BasicClient> client = mClient[cameraId].promote();
           if (client.get() != NULL) {
               clientsToDisconnect.push_back(client);
           }

           int i = cameraId;
           for (size_t j = 0; j < mProClientList[i].size(); ++j) {
               sp<ProClient> cl = mProClientList[i][j].promote();
               if (cl != NULL) {
                   clientsToDisconnect.push_back(cl);
               }
           }
        }

        /* now disconnect them. don't hold the lock
           or we can get into a deadlock */

        for (size_t i = 0; i < clientsToDisconnect.size(); ++i) {
            sp<BasicClient> client = clientsToDisconnect[i];

            client->disconnect();
            /**
             * The remote app will no longer be able to call methods on the
             * client since the client PID will be reset to 0
             */
        }

        ALOGV("%s: After unplug, disconnected %d clients",
              __FUNCTION__, clientsToDisconnect.size());
    }

    updateStatus(
            static_cast<ICameraServiceListener::Status>(newStatus), cameraId);

}

int32_t CameraService::getNumberOfCameras() {
    return mNumberOfCameras;
}

status_t CameraService::getCameraInfo(int cameraId,
                                      struct CameraInfo* cameraInfo) {
    if (!mModule) {
        return -ENODEV;
    }

    if (cameraId < 0 || cameraId >= mNumberOfCameras) {
        return BAD_VALUE;
    }

    struct camera_info info;
    status_t rc = mModule->get_camera_info(cameraId, &info);
    cameraInfo->facing = info.facing;
    cameraInfo->orientation = info.orientation;
    return rc;
}

status_t CameraService::getCameraCharacteristics(int cameraId,
                                                CameraMetadata* cameraInfo) {
    if (!cameraInfo) {
        ALOGE("%s: cameraInfo is NULL", __FUNCTION__);
        return BAD_VALUE;
    }

    if (!mModule) {
        ALOGE("%s: camera hardware module doesn't exist", __FUNCTION__);
        return -ENODEV;
    }

    if (mModule->common.module_api_version < CAMERA_MODULE_API_VERSION_2_0) {
        // TODO: Remove this check once HAL1 shim is in place.
        ALOGE("%s: Only HAL module version V2 or higher supports static metadata", __FUNCTION__);
        return BAD_VALUE;
    }

    if (cameraId < 0 || cameraId >= mNumberOfCameras) {
        ALOGE("%s: Invalid camera id: %d", __FUNCTION__, cameraId);
        return BAD_VALUE;
    }

    int facing;
    if (getDeviceVersion(cameraId, &facing) == CAMERA_DEVICE_API_VERSION_1_0) {
        // TODO: Remove this check once HAL1 shim is in place.
        ALOGE("%s: HAL1 doesn't support static metadata yet", __FUNCTION__);
        return BAD_VALUE;
    }

    if (getDeviceVersion(cameraId, &facing) <= CAMERA_DEVICE_API_VERSION_2_1) {
        // Disable HAL2.x support for camera2 API for now.
        ALOGW("%s: HAL2.x doesn't support getCameraCharacteristics for now", __FUNCTION__);
        return BAD_VALUE;
    }

    struct camera_info info;
    status_t ret = mModule->get_camera_info(cameraId, &info);
    *cameraInfo = info.static_camera_characteristics;

    return ret;
}

int CameraService::getDeviceVersion(int cameraId, int* facing) {
    struct camera_info info;
    if (mModule->get_camera_info(cameraId, &info) != OK) {
        return -1;
    }

    int deviceVersion;
    if (mModule->common.module_api_version >= CAMERA_MODULE_API_VERSION_2_0) {
        deviceVersion = info.device_version;
    } else {
        deviceVersion = CAMERA_DEVICE_API_VERSION_1_0;
    }

    if (facing) {
        *facing = info.facing;
    }

    return deviceVersion;
}

bool CameraService::isValidCameraId(int cameraId) {
    int facing;
    int deviceVersion = getDeviceVersion(cameraId, &facing);

    switch(deviceVersion) {
      case CAMERA_DEVICE_API_VERSION_1_0:
      case CAMERA_DEVICE_API_VERSION_2_0:
      case CAMERA_DEVICE_API_VERSION_2_1:
      case CAMERA_DEVICE_API_VERSION_3_0:
        return true;
      default:
        return false;
    }

    return false;
}

status_t CameraService::validateConnect(int cameraId,
                                    /*inout*/
                                    int& clientUid) const {

    int callingPid = getCallingPid();

    if (clientUid == USE_CALLING_UID) {
        clientUid = getCallingUid();
    } else {
        // We only trust our own process to forward client UIDs
        if (callingPid != getpid()) {
            ALOGE("CameraService::connect X (pid %d) rejected (don't trust clientUid)",
                    callingPid);
            return PERMISSION_DENIED;
        }
    }

    if (!mModule) {
        ALOGE("Camera HAL module not loaded");
        return -ENODEV;
    }

    if (cameraId < 0 || cameraId >= mNumberOfCameras) {
        ALOGE("CameraService::connect X (pid %d) rejected (invalid cameraId %d).",
            callingPid, cameraId);
        return -ENODEV;
    }

    char value[PROPERTY_VALUE_MAX];
    property_get("sys.secpolicy.camera.disabled", value, "0");
    if (strcmp(value, "1") == 0) {
        // Camera is disabled by DevicePolicyManager.
        ALOGI("Camera is disabled. connect X (pid %d) rejected", callingPid);
        return -EACCES;
    }

    ICameraServiceListener::Status currentStatus = getStatus(cameraId);
    if (currentStatus == ICameraServiceListener::STATUS_NOT_PRESENT) {
        ALOGI("Camera is not plugged in,"
               " connect X (pid %d) rejected", callingPid);
        return -ENODEV;
    } else if (currentStatus == ICameraServiceListener::STATUS_ENUMERATING) {
        ALOGI("Camera is enumerating,"
               " connect X (pid %d) rejected", callingPid);
        return -EBUSY;
    }
    // Else don't check for STATUS_NOT_AVAILABLE.
    //  -- It's done implicitly in canConnectUnsafe /w the mBusy array

    return OK;
}

bool CameraService::canConnectUnsafe(int cameraId,
                                     const String16& clientPackageName,
                                     const sp<IBinder>& remoteCallback,
                                     sp<BasicClient> &client) {
    String8 clientName8(clientPackageName);
    int callingPid = getCallingPid();

    if (mClient[cameraId] != 0) {
        client = mClient[cameraId].promote();
        if (client != 0) {
            if (remoteCallback == client->getRemote()) {
                LOG1("CameraService::connect X (pid %d) (the same client)",
                     callingPid);
                return true;
            } else {
                // TODOSC: need to support 1 regular client,
                // multiple shared clients here
                ALOGW("CameraService::connect X (pid %d) rejected"
                      " (existing client).", callingPid);
                return false;
            }
        }
        mClient[cameraId].clear();
    }

    /*
    mBusy is set to false as the last step of the Client destructor,
    after which it is guaranteed that the Client destructor has finished (
    including any inherited destructors)

    We only need this for a Client subclasses since we don't allow
    multiple Clents to be opened concurrently, but multiple BasicClient
    would be fine
    */
    if (mBusy[cameraId]) {
        ALOGW("CameraService::connect X (pid %d, \"%s\") rejected"
                " (camera %d is still busy).", callingPid,
                clientName8.string(), cameraId);
        return false;
    }

    return true;
}

status_t CameraService::connect(
        const sp<ICameraClient>& cameraClient,
        int cameraId,
        const String16& clientPackageName,
        int clientUid,
        /*out*/
        sp<ICamera>& device) {

    String8 clientName8(clientPackageName);
    int callingPid = getCallingPid();

    LOG1("CameraService::connect E (pid %d \"%s\", id %d)", callingPid,
            clientName8.string(), cameraId);

    status_t status = validateConnect(cameraId, /*inout*/clientUid);
    if (status != OK) {
        return status;
    }


    sp<Client> client;
    {
        Mutex::Autolock lock(mServiceLock);
        sp<BasicClient> clientTmp;
        if (!canConnectUnsafe(cameraId, clientPackageName,
                              cameraClient->asBinder(),
                              /*out*/clientTmp)) {
            return -EBUSY;
        } else if (client.get() != NULL) {
            device = static_cast<Client*>(clientTmp.get());
            return OK;
        }

        int facing = -1;
        int deviceVersion = getDeviceVersion(cameraId, &facing);

        // If there are other non-exclusive users of the camera,
        //  this will tear them down before we can reuse the camera
        if (isValidCameraId(cameraId)) {
            // transition from PRESENT -> NOT_AVAILABLE
            updateStatus(ICameraServiceListener::STATUS_NOT_AVAILABLE,
                         cameraId);
        }

        switch(deviceVersion) {
          case CAMERA_DEVICE_API_VERSION_1_0:
            client = new CameraClient(this, cameraClient,
                    clientPackageName, cameraId,
                    facing, callingPid, clientUid, getpid());
            break;
          case CAMERA_DEVICE_API_VERSION_2_0:
          case CAMERA_DEVICE_API_VERSION_2_1:
          case CAMERA_DEVICE_API_VERSION_3_0:
            client = new Camera2Client(this, cameraClient,
                    clientPackageName, cameraId,
                    facing, callingPid, clientUid, getpid(),
                    deviceVersion);
            break;
          case -1:
            ALOGE("Invalid camera id %d", cameraId);
            return BAD_VALUE;
          default:
            ALOGE("Unknown camera device HAL version: %d", deviceVersion);
            return INVALID_OPERATION;
        }

        status_t status = connectFinishUnsafe(client, client->getRemote());
        if (status != OK) {
            // this is probably not recoverable.. maybe the client can try again
            // OK: we can only get here if we were originally in PRESENT state
            updateStatus(ICameraServiceListener::STATUS_PRESENT, cameraId);
            return status;
        }

        mClient[cameraId] = client;
        LOG1("CameraService::connect X (id %d, this pid is %d)", cameraId,
             getpid());
    }
    // important: release the mutex here so the client can call back
    //    into the service from its destructor (can be at the end of the call)

    device = client;
    return OK;
}

status_t CameraService::connectFinishUnsafe(const sp<BasicClient>& client,
                                            const sp<IBinder>& remoteCallback) {
    status_t status = client->initialize(mModule);
    if (status != OK) {
        return status;
    }

    remoteCallback->linkToDeath(this);

    return OK;
}

status_t CameraService::connectPro(
                                        const sp<IProCameraCallbacks>& cameraCb,
                                        int cameraId,
                                        const String16& clientPackageName,
                                        int clientUid,
                                        /*out*/
                                        sp<IProCameraUser>& device)
{
    String8 clientName8(clientPackageName);
    int callingPid = getCallingPid();

    LOG1("CameraService::connectPro E (pid %d \"%s\", id %d)", callingPid,
            clientName8.string(), cameraId);
    status_t status = validateConnect(cameraId, /*inout*/clientUid);
    if (status != OK) {
        return status;
    }

    sp<ProClient> client;
    {
        Mutex::Autolock lock(mServiceLock);
        {
            sp<BasicClient> client;
            if (!canConnectUnsafe(cameraId, clientPackageName,
                                  cameraCb->asBinder(),
                                  /*out*/client)) {
                return -EBUSY;
            }
        }

        int facing = -1;
        int deviceVersion = getDeviceVersion(cameraId, &facing);

        switch(deviceVersion) {
          case CAMERA_DEVICE_API_VERSION_1_0:
            ALOGE("Camera id %d uses HALv1, doesn't support ProCamera",
                  cameraId);
            return -EOPNOTSUPP;
            break;
          case CAMERA_DEVICE_API_VERSION_2_0:
          case CAMERA_DEVICE_API_VERSION_2_1:
          case CAMERA_DEVICE_API_VERSION_3_0:
            client = new ProCamera2Client(this, cameraCb, String16(),
                    cameraId, facing, callingPid, USE_CALLING_UID, getpid());
            break;
          case -1:
            ALOGE("Invalid camera id %d", cameraId);
            return BAD_VALUE;
          default:
            ALOGE("Unknown camera device HAL version: %d", deviceVersion);
            return INVALID_OPERATION;
        }

        status_t status = connectFinishUnsafe(client, client->getRemote());
        if (status != OK) {
            return status;
        }

        mProClientList[cameraId].push(client);

        LOG1("CameraService::connectPro X (id %d, this pid is %d)", cameraId,
                getpid());
    }
    // important: release the mutex here so the client can call back
    //    into the service from its destructor (can be at the end of the call)
    device = client;
    return OK;
}

status_t CameraService::connectDevice(
        const sp<ICameraDeviceCallbacks>& cameraCb,
        int cameraId,
        const String16& clientPackageName,
        int clientUid,
        /*out*/
        sp<ICameraDeviceUser>& device)
{

    String8 clientName8(clientPackageName);
    int callingPid = getCallingPid();

    LOG1("CameraService::connectDevice E (pid %d \"%s\", id %d)", callingPid,
            clientName8.string(), cameraId);

    status_t status = validateConnect(cameraId, /*inout*/clientUid);
    if (status != OK) {
        return status;
    }

    sp<CameraDeviceClient> client;
    {
        Mutex::Autolock lock(mServiceLock);
        {
            sp<BasicClient> client;
            if (!canConnectUnsafe(cameraId, clientPackageName,
                                  cameraCb->asBinder(),
                                  /*out*/client)) {
                return -EBUSY;
            }
        }

        int facing = -1;
        int deviceVersion = getDeviceVersion(cameraId, &facing);

        // If there are other non-exclusive users of the camera,
        //  this will tear them down before we can reuse the camera
        if (isValidCameraId(cameraId)) {
            // transition from PRESENT -> NOT_AVAILABLE
            updateStatus(ICameraServiceListener::STATUS_NOT_AVAILABLE,
                         cameraId);
        }

        switch(deviceVersion) {
          case CAMERA_DEVICE_API_VERSION_1_0:
            ALOGW("Camera using old HAL version: %d", deviceVersion);
            return -EOPNOTSUPP;
           // TODO: don't allow 2.0  Only allow 2.1 and higher
          case CAMERA_DEVICE_API_VERSION_2_0:
          case CAMERA_DEVICE_API_VERSION_2_1:
          case CAMERA_DEVICE_API_VERSION_3_0:
            client = new CameraDeviceClient(this, cameraCb, String16(),
                    cameraId, facing, callingPid, USE_CALLING_UID, getpid());
            break;
          case -1:
            ALOGE("Invalid camera id %d", cameraId);
            return BAD_VALUE;
          default:
            ALOGE("Unknown camera device HAL version: %d", deviceVersion);
            return INVALID_OPERATION;
        }

        status_t status = connectFinishUnsafe(client, client->getRemote());
        if (status != OK) {
            // this is probably not recoverable.. maybe the client can try again
            // OK: we can only get here if we were originally in PRESENT state
            updateStatus(ICameraServiceListener::STATUS_PRESENT, cameraId);
            return status;
        }

        LOG1("CameraService::connectDevice X (id %d, this pid is %d)", cameraId,
                getpid());

        mClient[cameraId] = client;
    }
    // important: release the mutex here so the client can call back
    //    into the service from its destructor (can be at the end of the call)

    device = client;
    return OK;
}


status_t CameraService::addListener(
                                const sp<ICameraServiceListener>& listener) {
    ALOGV("%s: Add listener %p", __FUNCTION__, listener.get());

    Mutex::Autolock lock(mServiceLock);

    Vector<sp<ICameraServiceListener> >::iterator it, end;
    for (it = mListenerList.begin(); it != mListenerList.end(); ++it) {
        if ((*it)->asBinder() == listener->asBinder()) {
            ALOGW("%s: Tried to add listener %p which was already subscribed",
                  __FUNCTION__, listener.get());
            return ALREADY_EXISTS;
        }
    }

    mListenerList.push_back(listener);

    /* Immediately signal current status to this listener only */
    {
        Mutex::Autolock m(mStatusMutex) ;
        int numCams = getNumberOfCameras();
        for (int i = 0; i < numCams; ++i) {
            listener->onStatusChanged(mStatusList[i], i);
        }
    }

    return OK;
}
status_t CameraService::removeListener(
                                const sp<ICameraServiceListener>& listener) {
    ALOGV("%s: Remove listener %p", __FUNCTION__, listener.get());

    Mutex::Autolock lock(mServiceLock);

    Vector<sp<ICameraServiceListener> >::iterator it;
    for (it = mListenerList.begin(); it != mListenerList.end(); ++it) {
        if ((*it)->asBinder() == listener->asBinder()) {
            mListenerList.erase(it);
            return OK;
        }
    }

    ALOGW("%s: Tried to remove a listener %p which was not subscribed",
          __FUNCTION__, listener.get());

    return BAD_VALUE;
}

void CameraService::removeClientByRemote(const wp<IBinder>& remoteBinder) {
    int callingPid = getCallingPid();
    LOG1("CameraService::removeClientByRemote E (pid %d)", callingPid);

    // Declare this before the lock to make absolutely sure the
    // destructor won't be called with the lock held.
    Mutex::Autolock lock(mServiceLock);

    int outIndex;
    sp<BasicClient> client = findClientUnsafe(remoteBinder, outIndex);

    if (client != 0) {
        // Found our camera, clear and leave.
        LOG1("removeClient: clear camera %d", outIndex);
        mClient[outIndex].clear();

        client->getRemote()->unlinkToDeath(this);
    } else {

        sp<ProClient> clientPro = findProClientUnsafe(remoteBinder);

        if (clientPro != NULL) {
            // Found our camera, clear and leave.
            LOG1("removeClient: clear pro %p", clientPro.get());

            clientPro->getRemoteCallback()->asBinder()->unlinkToDeath(this);
        }
    }

    LOG1("CameraService::removeClientByRemote X (pid %d)", callingPid);
}

sp<CameraService::ProClient> CameraService::findProClientUnsafe(
                        const wp<IBinder>& cameraCallbacksRemote)
{
    sp<ProClient> clientPro;

    for (int i = 0; i < mNumberOfCameras; ++i) {
        Vector<size_t> removeIdx;

        for (size_t j = 0; j < mProClientList[i].size(); ++j) {
            wp<ProClient> cl = mProClientList[i][j];

            sp<ProClient> clStrong = cl.promote();
            if (clStrong != NULL && clStrong->getRemote() == cameraCallbacksRemote) {
                clientPro = clStrong;
                break;
            } else if (clStrong == NULL) {
                // mark to clean up dead ptr
                removeIdx.push(j);
            }
        }

        // remove stale ptrs (in reverse so the indices dont change)
        for (ssize_t j = (ssize_t)removeIdx.size() - 1; j >= 0; --j) {
            mProClientList[i].removeAt(removeIdx[j]);
        }

    }

    return clientPro;
}

sp<CameraService::BasicClient> CameraService::findClientUnsafe(
                        const wp<IBinder>& cameraClient, int& outIndex) {
    sp<BasicClient> client;

    for (int i = 0; i < mNumberOfCameras; i++) {

        // This happens when we have already disconnected (or this is
        // just another unused camera).
        if (mClient[i] == 0) continue;

        // Promote mClient. It can fail if we are called from this path:
        // Client::~Client() -> disconnect() -> removeClientByRemote().
        client = mClient[i].promote();

        // Clean up stale client entry
        if (client == NULL) {
            mClient[i].clear();
            continue;
        }

        if (cameraClient == client->getRemote()) {
            // Found our camera
            outIndex = i;
            return client;
        }
    }

    outIndex = -1;
    return NULL;
}

CameraService::BasicClient* CameraService::getClientByIdUnsafe(int cameraId) {
    if (cameraId < 0 || cameraId >= mNumberOfCameras) return NULL;
    return mClient[cameraId].unsafe_get();
}

Mutex* CameraService::getClientLockById(int cameraId) {
    if (cameraId < 0 || cameraId >= mNumberOfCameras) return NULL;
    return &mClientLock[cameraId];
}

sp<CameraService::BasicClient> CameraService::getClientByRemote(
                                const wp<IBinder>& cameraClient) {

    // Declare this before the lock to make absolutely sure the
    // destructor won't be called with the lock held.
    sp<BasicClient> client;

    Mutex::Autolock lock(mServiceLock);

    int outIndex;
    client = findClientUnsafe(cameraClient, outIndex);

    return client;
}

status_t CameraService::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags) {
    // Permission checks
    switch (code) {
        case BnCameraService::CONNECT:
        case BnCameraService::CONNECT_PRO:
            const int pid = getCallingPid();
            const int self_pid = getpid();
            if (pid != self_pid) {
                // we're called from a different process, do the real check
                if (!checkCallingPermission(
                        String16("android.permission.CAMERA"))) {
                    const int uid = getCallingUid();
                    ALOGE("Permission Denial: "
                         "can't use the camera pid=%d, uid=%d", pid, uid);
                    return PERMISSION_DENIED;
                }
            }
            break;
    }

    return BnCameraService::onTransact(code, data, reply, flags);
}

// The reason we need this busy bit is a new CameraService::connect() request
// may come in while the previous Client's destructor has not been run or is
// still running. If the last strong reference of the previous Client is gone
// but the destructor has not been finished, we should not allow the new Client
// to be created because we need to wait for the previous Client to tear down
// the hardware first.
void CameraService::setCameraBusy(int cameraId) {
    android_atomic_write(1, &mBusy[cameraId]);

    ALOGV("setCameraBusy cameraId=%d", cameraId);
}

void CameraService::setCameraFree(int cameraId) {
    android_atomic_write(0, &mBusy[cameraId]);

    ALOGV("setCameraFree cameraId=%d", cameraId);
}

// We share the media players for shutter and recording sound for all clients.
// A reference count is kept to determine when we will actually release the
// media players.

MediaPlayer* CameraService::newMediaPlayer(const char *file) {
    MediaPlayer* mp = new MediaPlayer();
    if (mp->setDataSource(file, NULL) == NO_ERROR) {
        mp->setAudioStreamType(AUDIO_STREAM_ENFORCED_AUDIBLE);
        mp->prepare();
    } else {
        ALOGE("Failed to load CameraService sounds: %s", file);
        return NULL;
    }
    return mp;
}

void CameraService::loadSound() {
    Mutex::Autolock lock(mSoundLock);
    LOG1("CameraService::loadSound ref=%d", mSoundRef);
    if (mSoundRef++) return;

    mSoundPlayer[SOUND_SHUTTER] = newMediaPlayer("/system/media/audio/ui/camera_click.ogg");
    mSoundPlayer[SOUND_RECORDING] = newMediaPlayer("/system/media/audio/ui/VideoRecord.ogg");
}

void CameraService::releaseSound() {
    Mutex::Autolock lock(mSoundLock);
    LOG1("CameraService::releaseSound ref=%d", mSoundRef);
    if (--mSoundRef) return;

    for (int i = 0; i < NUM_SOUNDS; i++) {
        if (mSoundPlayer[i] != 0) {
            mSoundPlayer[i]->disconnect();
            mSoundPlayer[i].clear();
        }
    }
}

void CameraService::playSound(sound_kind kind) {
    LOG1("playSound(%d)", kind);
    Mutex::Autolock lock(mSoundLock);
    sp<MediaPlayer> player = mSoundPlayer[kind];
    if (player != 0) {
        player->seekTo(0);
        player->start();
    }
}

// ----------------------------------------------------------------------------

CameraService::Client::Client(){
}
CameraService::Client::Client(const sp<CameraService>& cameraService,
        const sp<ICameraClient>& cameraClient,
        const String16& clientPackageName,
        int cameraId, int cameraFacing,
        int clientPid, uid_t clientUid,
        int servicePid) :
        CameraService::BasicClient(cameraService, cameraClient->asBinder(),
                clientPackageName,
                cameraId, cameraFacing,
                clientPid, clientUid,
                servicePid)
{
    int callingPid = getCallingPid();
    LOG1("Client::Client E (pid %d, id %d)", callingPid, cameraId);

    mRemoteCallback = cameraClient;

    cameraService->setCameraBusy(cameraId);
    cameraService->loadSound();

    LOG1("Client::Client X (pid %d, id %d)", callingPid, cameraId);
}

// tear down the client
CameraService::Client::~Client() {
    ALOGV("~Client");
    mDestructionStarted = true;

    mCameraService->releaseSound();
    // unconditionally disconnect. function is idempotent
    Client::disconnect();
}

CameraService::BasicClient::BasicClient(){
   mOpsActive = false;
}

CameraService::BasicClient::BasicClient(const sp<CameraService>& cameraService,
        const sp<IBinder>& remoteCallback,
        const String16& clientPackageName,
        int cameraId, int cameraFacing,
        int clientPid, uid_t clientUid,
        int servicePid):
        mClientPackageName(clientPackageName)
{
    mCameraService = cameraService;
    mRemoteBinder = remoteCallback;
    mCameraId = cameraId;
    mCameraFacing = cameraFacing;
    mClientPid = clientPid;
    mClientUid = clientUid;
    mServicePid = servicePid;
    mOpsActive = false;
    mDestructionStarted = false;
}

CameraService::BasicClient::~BasicClient() {
    ALOGV("~BasicClient");
    mDestructionStarted = true;
}

void CameraService::BasicClient::disconnect() {
    ALOGV("BasicClient::disconnect");
    mCameraService->removeClientByRemote(mRemoteBinder);
    // client shouldn't be able to call into us anymore
    mClientPid = 0;
}

status_t CameraService::BasicClient::startCameraOps() {
    int32_t res;

    mOpsCallback = new OpsCallback(this);

    {
        ALOGV("%s: Start camera ops, package name = %s, client UID = %d",
              __FUNCTION__, String8(mClientPackageName).string(), mClientUid);
    }

    mAppOpsManager.startWatchingMode(AppOpsManager::OP_CAMERA,
            mClientPackageName, mOpsCallback);
    res = mAppOpsManager.startOp(AppOpsManager::OP_CAMERA,
            mClientUid, mClientPackageName);

    if (res != AppOpsManager::MODE_ALLOWED) {
        ALOGI("Camera %d: Access for \"%s\" has been revoked",
                mCameraId, String8(mClientPackageName).string());
        return PERMISSION_DENIED;
    }
    mOpsActive = true;
    return OK;
}

status_t CameraService::BasicClient::finishCameraOps() {
    if (mOpsActive) {
        mAppOpsManager.finishOp(AppOpsManager::OP_CAMERA, mClientUid,
                mClientPackageName);
        mOpsActive = false;
    }
    mAppOpsManager.stopWatchingMode(mOpsCallback);
    mOpsCallback.clear();

    return OK;
}

void CameraService::BasicClient::opChanged(int32_t op, const String16& packageName) {
    String8 name(packageName);
    String8 myName(mClientPackageName);

    if (op != AppOpsManager::OP_CAMERA) {
        ALOGW("Unexpected app ops notification received: %d", op);
        return;
    }

    int32_t res;
    res = mAppOpsManager.checkOp(AppOpsManager::OP_CAMERA,
            mClientUid, mClientPackageName);
    ALOGV("checkOp returns: %d, %s ", res,
            res == AppOpsManager::MODE_ALLOWED ? "ALLOWED" :
            res == AppOpsManager::MODE_IGNORED ? "IGNORED" :
            res == AppOpsManager::MODE_ERRORED ? "ERRORED" :
            "UNKNOWN");

    if (res != AppOpsManager::MODE_ALLOWED) {
        ALOGI("Camera %d: Access for \"%s\" revoked", mCameraId,
                myName.string());
        // Reset the client PID to allow server-initiated disconnect,
        // and to prevent further calls by client.
        mClientPid = getCallingPid();
        notifyError();
        disconnect();
    }
}

// ----------------------------------------------------------------------------

Mutex* CameraService::Client::getClientLockFromCookie(void* user) {
    return gCameraService->getClientLockById((int) user);
}

// Provide client pointer for callbacks. Client lock returned from getClientLockFromCookie should
// be acquired for this to be safe
CameraService::Client* CameraService::Client::getClientFromCookie(void* user) {
    BasicClient *basicClient = gCameraService->getClientByIdUnsafe((int) user);
    // OK: only CameraClient calls this, and they already cast anyway.
    Client* client = static_cast<Client*>(basicClient);

    // This could happen if the Client is in the process of shutting down (the
    // last strong reference is gone, but the destructor hasn't finished
    // stopping the hardware).
    if (client == NULL) return NULL;

    // destruction already started, so should not be accessed
    if (client->mDestructionStarted) return NULL;

    return client;
}

void CameraService::Client::notifyError() {
    mRemoteCallback->notifyCallback(CAMERA_MSG_ERROR, CAMERA_ERROR_RELEASED, 0);
}

// NOTE: function is idempotent
void CameraService::Client::disconnect() {
    ALOGV("Client::disconnect");
    BasicClient::disconnect();
    mCameraService->setCameraFree(mCameraId);

    StatusVector rejectSourceStates;
    rejectSourceStates.push_back(ICameraServiceListener::STATUS_NOT_PRESENT);
    rejectSourceStates.push_back(ICameraServiceListener::STATUS_ENUMERATING);

    // Transition to PRESENT if the camera is not in either of above 2 states
    mCameraService->updateStatus(ICameraServiceListener::STATUS_PRESENT,
                                 mCameraId,
                                 &rejectSourceStates);
}

CameraService::Client::OpsCallback::OpsCallback(wp<BasicClient> client):
        mClient(client) {
}

void CameraService::Client::OpsCallback::opChanged(int32_t op,
        const String16& packageName) {
    sp<BasicClient> client = mClient.promote();
    if (client != NULL) {
        client->opChanged(op, packageName);
    }
}

// ----------------------------------------------------------------------------
//                  IProCamera
// ----------------------------------------------------------------------------

CameraService::ProClient::ProClient(){
}

CameraService::ProClient::ProClient(const sp<CameraService>& cameraService,
        const sp<IProCameraCallbacks>& remoteCallback,
        const String16& clientPackageName,
        int cameraId,
        int cameraFacing,
        int clientPid,
        uid_t clientUid,
        int servicePid)
        : CameraService::BasicClient(cameraService, remoteCallback->asBinder(),
                clientPackageName, cameraId, cameraFacing,
                clientPid,  clientUid, servicePid)
{
    mRemoteCallback = remoteCallback;
}

CameraService::ProClient::~ProClient() {
}

void CameraService::ProClient::notifyError() {
    mRemoteCallback->notifyCallback(CAMERA_MSG_ERROR, CAMERA_ERROR_RELEASED, 0);
}

// ----------------------------------------------------------------------------

static const int kDumpLockRetries = 50;
static const int kDumpLockSleep = 60000;

static bool tryLock(Mutex& mutex)
{
    bool locked = false;
    for (int i = 0; i < kDumpLockRetries; ++i) {
        if (mutex.tryLock() == NO_ERROR) {
            locked = true;
            break;
        }
        usleep(kDumpLockSleep);
    }
    return locked;
}

status_t CameraService::dump(int fd, const Vector<String16>& args) {
    String8 result;
    if (checkCallingPermission(String16("android.permission.DUMP")) == false) {
        result.appendFormat("Permission Denial: "
                "can't dump CameraService from pid=%d, uid=%d\n",
                getCallingPid(),
                getCallingUid());
        write(fd, result.string(), result.size());
    } else {
        bool locked = tryLock(mServiceLock);
        // failed to lock - CameraService is probably deadlocked
        if (!locked) {
            result.append("CameraService may be deadlocked\n");
            write(fd, result.string(), result.size());
        }

        bool hasClient = false;
        if (!mModule) {
            result = String8::format("No camera module available!\n");
            write(fd, result.string(), result.size());
            return NO_ERROR;
        }

        result = String8::format("Camera module HAL API version: 0x%x\n",
                mModule->common.hal_api_version);
        result.appendFormat("Camera module API version: 0x%x\n",
                mModule->common.module_api_version);
        result.appendFormat("Camera module name: %s\n",
                mModule->common.name);
        result.appendFormat("Camera module author: %s\n",
                mModule->common.author);
        result.appendFormat("Number of camera devices: %d\n\n", mNumberOfCameras);
        write(fd, result.string(), result.size());
        for (int i = 0; i < mNumberOfCameras; i++) {
            result = String8::format("Camera %d static information:\n", i);
            camera_info info;

            status_t rc = mModule->get_camera_info(i, &info);
            if (rc != OK) {
                result.appendFormat("  Error reading static information!\n");
                write(fd, result.string(), result.size());
            } else {
                result.appendFormat("  Facing: %s\n",
                        info.facing == CAMERA_FACING_BACK ? "BACK" : "FRONT");
                result.appendFormat("  Orientation: %d\n", info.orientation);
                int deviceVersion;
                if (mModule->common.module_api_version <
                        CAMERA_MODULE_API_VERSION_2_0) {
                    deviceVersion = CAMERA_DEVICE_API_VERSION_1_0;
                } else {
                    deviceVersion = info.device_version;
                }
                result.appendFormat("  Device version: 0x%x\n", deviceVersion);
                if (deviceVersion >= CAMERA_DEVICE_API_VERSION_2_0) {
                    result.appendFormat("  Device static metadata:\n");
                    write(fd, result.string(), result.size());
                    dump_indented_camera_metadata(info.static_camera_characteristics,
                            fd, 2, 4);
                } else {
                    write(fd, result.string(), result.size());
                }
            }

            sp<BasicClient> client = mClient[i].promote();
            if (client == 0) {
                result = String8::format("  Device is closed, no client instance\n");
                write(fd, result.string(), result.size());
                continue;
            }
            hasClient = true;
            result = String8::format("  Device is open. Client instance dump:\n");
            write(fd, result.string(), result.size());
            client->dump(fd, args);
        }
        if (!hasClient) {
            result = String8::format("\nNo active camera clients yet.\n");
            write(fd, result.string(), result.size());
        }

        if (locked) mServiceLock.unlock();

        // Dump camera traces if there were any
        write(fd, "\n", 1);
        camera3::CameraTraces::dump(fd, args);

        // change logging level
        int n = args.size();
        for (int i = 0; i + 1 < n; i++) {
            String16 verboseOption("-v");
            if (args[i] == verboseOption) {
                String8 levelStr(args[i+1]);
                int level = atoi(levelStr.string());
                result = String8::format("\nSetting log level to %d.\n", level);
                setLogLevel(level);
                write(fd, result.string(), result.size());
            }
        }

    }
    return NO_ERROR;
}

/*virtual*/void CameraService::binderDied(
    const wp<IBinder> &who) {

    /**
      * While tempting to promote the wp<IBinder> into a sp,
      * it's actually not supported by the binder driver
      */

    ALOGV("java clients' binder died");

    sp<BasicClient> cameraClient = getClientByRemote(who);

    if (cameraClient == 0) {
        ALOGV("java clients' binder death already cleaned up (normal case)");
        return;
    }

    ALOGW("Disconnecting camera client %p since the binder for it "
          "died (this pid %d)", cameraClient.get(), getCallingPid());

    cameraClient->disconnect();

}

void CameraService::updateStatus(ICameraServiceListener::Status status,
                                 int32_t cameraId,
                                 const StatusVector *rejectSourceStates) {
    // do not lock mServiceLock here or can get into a deadlock from
    //  connect() -> ProClient::disconnect -> updateStatus
    Mutex::Autolock lock(mStatusMutex);

    ICameraServiceListener::Status oldStatus = mStatusList[cameraId];

    mStatusList[cameraId] = status;

    if (oldStatus != status) {
        ALOGV("%s: Status has changed for camera ID %d from 0x%x to 0x%x",
              __FUNCTION__, cameraId, (uint32_t)oldStatus, (uint32_t)status);

        if (oldStatus == ICameraServiceListener::STATUS_NOT_PRESENT &&
            (status != ICameraServiceListener::STATUS_PRESENT &&
             status != ICameraServiceListener::STATUS_ENUMERATING)) {

            ALOGW("%s: From NOT_PRESENT can only transition into PRESENT"
                  " or ENUMERATING", __FUNCTION__);
            mStatusList[cameraId] = oldStatus;
            return;
        }

        if (rejectSourceStates != NULL) {
            const StatusVector &rejectList = *rejectSourceStates;
            StatusVector::const_iterator it = rejectList.begin();

            /**
             * Sometimes we want to conditionally do a transition.
             * For example if a client disconnects, we want to go to PRESENT
             * only if we weren't already in NOT_PRESENT or ENUMERATING.
             */
            for (; it != rejectList.end(); ++it) {
                if (oldStatus == *it) {
                    ALOGV("%s: Rejecting status transition for Camera ID %d, "
                          " since the source state was was in one of the bad "
                          " states.", __FUNCTION__, cameraId);
                    mStatusList[cameraId] = oldStatus;
                    return;
                }
            }
        }

        /**
          * ProClients lose their exclusive lock.
          * - Done before the CameraClient can initialize the HAL device,
          *   since we want to be able to close it before they get to initialize
          */
        if (status == ICameraServiceListener::STATUS_NOT_AVAILABLE) {
            Vector<wp<ProClient> > proClients(mProClientList[cameraId]);
            Vector<wp<ProClient> >::const_iterator it;

            for (it = proClients.begin(); it != proClients.end(); ++it) {
                sp<ProClient> proCl = it->promote();
                if (proCl.get() != NULL) {
                    proCl->onExclusiveLockStolen();
                }
            }
        }

        Vector<sp<ICameraServiceListener> >::const_iterator it;
        for (it = mListenerList.begin(); it != mListenerList.end(); ++it) {
            (*it)->onStatusChanged(status, cameraId);
        }
    }
}

ICameraServiceListener::Status CameraService::getStatus(int cameraId) const {
    if (cameraId < 0 || cameraId >= MAX_CAMERAS) {
        ALOGE("%s: Invalid camera ID %d", __FUNCTION__, cameraId);
        return ICameraServiceListener::STATUS_UNKNOWN;
    }

    Mutex::Autolock al(mStatusMutex);
    return mStatusList[cameraId];
}

}; // namespace android
