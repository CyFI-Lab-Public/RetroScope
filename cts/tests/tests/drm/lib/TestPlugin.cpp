/*
 * Copyright (C) 2010 The Android Open Source Project
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

//#define LOG_NDEBUG 0
#define LOG_TAG "TestPlugin"
#include <utils/Log.h>

#include <drm/DrmRights.h>
#include <drm/DrmConstraints.h>
#include <drm/DrmMetadata.h>
#include <drm/DrmInfo.h>
#include <drm/DrmInfoEvent.h>
#include <drm/DrmInfoStatus.h>
#include <drm/DrmConvertedStatus.h>
#include <drm/DrmInfoRequest.h>
#include <drm/DrmSupportInfo.h>
#include <TestPlugin.h>

using namespace android;


// This extern "C" is mandatory to be managed by TPlugInManager
extern "C" IDrmEngine* create() {
    return new TestPlugIn();
}

// This extern "C" is mandatory to be managed by TPlugInManager
extern "C" void destroy(IDrmEngine* pPlugIn) {
    delete pPlugIn;
    pPlugIn = NULL;
}

TestPlugIn::TestPlugIn()
    : DrmEngineBase() {

}

TestPlugIn::~TestPlugIn() {

}

DrmMetadata* TestPlugIn::onGetMetadata(int uniqueId, const String8* path) {
    return NULL;
}

DrmConstraints* TestPlugIn::onGetConstraints(
        int uniqueId, const String8* path, int action) {
    return NULL;
}

DrmInfoStatus* TestPlugIn::onProcessDrmInfo(int uniqueId, const DrmInfo* drmInfo) {
    return NULL;
}

status_t TestPlugIn::onSetOnInfoListener(
            int uniqueId, const IDrmEngine::OnInfoListener* infoListener) {
    return DRM_NO_ERROR;
}

status_t TestPlugIn::onInitialize(int uniqueId) {
    return DRM_NO_ERROR;
}

status_t TestPlugIn::onTerminate(int uniqueId) {
    return DRM_NO_ERROR;
}

DrmSupportInfo* TestPlugIn::onGetSupportInfo(int uniqueId) {
    DrmSupportInfo* drmSupportInfo = new DrmSupportInfo();
    return drmSupportInfo;
}

status_t TestPlugIn::onSaveRights(int uniqueId, const DrmRights& drmRights,
            const String8& rightsPath, const String8& contentPath) {
    return DRM_NO_ERROR;
}

DrmInfo* TestPlugIn::onAcquireDrmInfo(int uniqueId, const DrmInfoRequest* drmInfoRequest) {
    return NULL;
}

bool TestPlugIn::onCanHandle(int uniqueId, const String8& path) {
    return false;
}

String8 TestPlugIn::onGetOriginalMimeType(int uniqueId, const String8& path, int fd) {
    return String8("video/none");
}

int TestPlugIn::onGetDrmObjectType(
            int uniqueId, const String8& path, const String8& mimeType) {
    return DrmObjectType::UNKNOWN;
}

int TestPlugIn::onCheckRightsStatus(int uniqueId, const String8& path, int action) {
    int rightsStatus = RightsStatus::RIGHTS_VALID;
    return rightsStatus;
}

status_t TestPlugIn::onConsumeRights(int uniqueId, DecryptHandle* decryptHandle,
            int action, bool reserve) {
    return DRM_NO_ERROR;
}

status_t TestPlugIn::onSetPlaybackStatus(int uniqueId, DecryptHandle* decryptHandle,
            int playbackStatus, int64_t position) {
    return DRM_NO_ERROR;
}

bool TestPlugIn::onValidateAction(int uniqueId, const String8& path,
            int action, const ActionDescription& description) {
    return true;
}

status_t TestPlugIn::onRemoveRights(int uniqueId, const String8& path) {
    return DRM_NO_ERROR;
}

status_t TestPlugIn::onRemoveAllRights(int uniqueId) {
    return DRM_NO_ERROR;
}

status_t TestPlugIn::onOpenConvertSession(int uniqueId, int convertId) {
    return DRM_NO_ERROR;
}

DrmConvertedStatus* TestPlugIn::onConvertData(
            int uniqueId, int convertId, const DrmBuffer* inputData) {
    return new DrmConvertedStatus(DrmConvertedStatus::STATUS_OK, NULL, 0);
}

DrmConvertedStatus* TestPlugIn::onCloseConvertSession(int uniqueId, int convertId) {
    return new DrmConvertedStatus(DrmConvertedStatus::STATUS_OK, NULL, 0);
}

status_t TestPlugIn::onOpenDecryptSession(
            int uniqueId, DecryptHandle* decryptHandle, int fd, off64_t offset, off64_t length) {
    return DRM_ERROR_CANNOT_HANDLE;
}

status_t TestPlugIn::onOpenDecryptSession(
            int uniqueId, DecryptHandle* decryptHandle, const char* uri) {
    return DRM_ERROR_CANNOT_HANDLE;
}

status_t TestPlugIn::onCloseDecryptSession(int uniqueId, DecryptHandle* decryptHandle) {
    return DRM_NO_ERROR;
}

status_t TestPlugIn::onInitializeDecryptUnit(int uniqueId, DecryptHandle* decryptHandle,
            int decryptUnitId, const DrmBuffer* headerInfo) {
    return DRM_NO_ERROR;
}

status_t TestPlugIn::onDecrypt(int uniqueId, DecryptHandle* decryptHandle,
            int decryptUnitId, const DrmBuffer* encBuffer, DrmBuffer** decBuffer, DrmBuffer* IV) {
    return DRM_NO_ERROR;
}

status_t TestPlugIn::onFinalizeDecryptUnit(
            int uniqueId, DecryptHandle* decryptHandle, int decryptUnitId) {
    return DRM_NO_ERROR;
}

ssize_t TestPlugIn::onPread(int uniqueId, DecryptHandle* decryptHandle,
            void* buffer, ssize_t numBytes, off64_t offset) {
    return 0;
}

