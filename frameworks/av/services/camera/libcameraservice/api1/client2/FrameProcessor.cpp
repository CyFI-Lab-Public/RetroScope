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

#define LOG_TAG "Camera2-FrameProcessor"
#define ATRACE_TAG ATRACE_TAG_CAMERA
//#define LOG_NDEBUG 0

#include <utils/Log.h>
#include <utils/Trace.h>

#include "common/CameraDeviceBase.h"
#include "api1/Camera2Client.h"
#include "api1/client2/FrameProcessor.h"

namespace android {
namespace camera2 {

FrameProcessor::FrameProcessor(wp<CameraDeviceBase> device,
                               sp<Camera2Client> client) :
    FrameProcessorBase(device),
    mClient(client),
    mLastFrameNumberOfFaces(0),
    mLast3AFrameNumber(-1) {

    sp<CameraDeviceBase> d = device.promote();
    mSynthesize3ANotify = !(d->willNotify3A());

    {
        SharedParameters::Lock l(client->getParameters());
        mUsePartialQuirk = l.mParameters.quirks.partialResults;

        // Initialize starting 3A state
        m3aState.afTriggerId = l.mParameters.afTriggerCounter;
        m3aState.aeTriggerId = l.mParameters.precaptureTriggerCounter;
        // Check if lens is fixed-focus
        if (l.mParameters.focusMode == Parameters::FOCUS_MODE_FIXED) {
            m3aState.afMode = ANDROID_CONTROL_AF_MODE_OFF;
        }
    }
}

FrameProcessor::~FrameProcessor() {
}

bool FrameProcessor::processSingleFrame(CameraMetadata &frame,
                                        const sp<CameraDeviceBase> &device) {

    sp<Camera2Client> client = mClient.promote();
    if (!client.get()) {
        return false;
    }

    bool partialResult = false;
    if (mUsePartialQuirk) {
        camera_metadata_entry_t entry;
        entry = frame.find(ANDROID_QUIRKS_PARTIAL_RESULT);
        if (entry.count > 0 &&
                entry.data.u8[0] == ANDROID_QUIRKS_PARTIAL_RESULT_PARTIAL) {
            partialResult = true;
        }
    }

    if (!partialResult && processFaceDetect(frame, client) != OK) {
        return false;
    }

    if (mSynthesize3ANotify) {
        process3aState(frame, client);
    }

    return FrameProcessorBase::processSingleFrame(frame, device);
}

status_t FrameProcessor::processFaceDetect(const CameraMetadata &frame,
        const sp<Camera2Client> &client) {
    status_t res = BAD_VALUE;
    ATRACE_CALL();
    camera_metadata_ro_entry_t entry;
    bool enableFaceDetect;

    {
        SharedParameters::Lock l(client->getParameters());
        enableFaceDetect = l.mParameters.enableFaceDetect;
    }
    entry = frame.find(ANDROID_STATISTICS_FACE_DETECT_MODE);

    // TODO: This should be an error once implementations are compliant
    if (entry.count == 0) {
        return OK;
    }

    uint8_t faceDetectMode = entry.data.u8[0];

    camera_frame_metadata metadata;
    Vector<camera_face_t> faces;
    metadata.number_of_faces = 0;

    if (enableFaceDetect &&
        faceDetectMode != ANDROID_STATISTICS_FACE_DETECT_MODE_OFF) {

        SharedParameters::Lock l(client->getParameters());
        entry = frame.find(ANDROID_STATISTICS_FACE_RECTANGLES);
        if (entry.count == 0) {
            // No faces this frame
            /* warning: locks SharedCameraCallbacks */
            callbackFaceDetection(client, metadata);
            return OK;
        }
        metadata.number_of_faces = entry.count / 4;
        if (metadata.number_of_faces >
                l.mParameters.fastInfo.maxFaces) {
            ALOGE("%s: Camera %d: More faces than expected! (Got %d, max %d)",
                    __FUNCTION__, client->getCameraId(),
                    metadata.number_of_faces, l.mParameters.fastInfo.maxFaces);
            return res;
        }
        const int32_t *faceRects = entry.data.i32;

        entry = frame.find(ANDROID_STATISTICS_FACE_SCORES);
        if (entry.count == 0) {
            ALOGE("%s: Camera %d: Unable to read face scores",
                    __FUNCTION__, client->getCameraId());
            return res;
        }
        const uint8_t *faceScores = entry.data.u8;

        const int32_t *faceLandmarks = NULL;
        const int32_t *faceIds = NULL;

        if (faceDetectMode == ANDROID_STATISTICS_FACE_DETECT_MODE_FULL) {
            entry = frame.find(ANDROID_STATISTICS_FACE_LANDMARKS);
            if (entry.count == 0) {
                ALOGE("%s: Camera %d: Unable to read face landmarks",
                        __FUNCTION__, client->getCameraId());
                return res;
            }
            faceLandmarks = entry.data.i32;

            entry = frame.find(ANDROID_STATISTICS_FACE_IDS);

            if (entry.count == 0) {
                ALOGE("%s: Camera %d: Unable to read face IDs",
                        __FUNCTION__, client->getCameraId());
                return res;
            }
            faceIds = entry.data.i32;
        }

        faces.setCapacity(metadata.number_of_faces);

        size_t maxFaces = metadata.number_of_faces;
        for (size_t i = 0; i < maxFaces; i++) {
            if (faceScores[i] == 0) {
                metadata.number_of_faces--;
                continue;
            }
            if (faceScores[i] > 100) {
                ALOGW("%s: Face index %d with out of range score %d",
                        __FUNCTION__, i, faceScores[i]);
            }

            camera_face_t face;

            face.rect[0] = l.mParameters.arrayXToNormalized(faceRects[i*4 + 0]);
            face.rect[1] = l.mParameters.arrayYToNormalized(faceRects[i*4 + 1]);
            face.rect[2] = l.mParameters.arrayXToNormalized(faceRects[i*4 + 2]);
            face.rect[3] = l.mParameters.arrayYToNormalized(faceRects[i*4 + 3]);

            face.score = faceScores[i];
            if (faceDetectMode == ANDROID_STATISTICS_FACE_DETECT_MODE_FULL) {
                face.id = faceIds[i];
                face.left_eye[0] =
                    l.mParameters.arrayXToNormalized(faceLandmarks[i*6 + 0]);
                face.left_eye[1] =
                    l.mParameters.arrayYToNormalized(faceLandmarks[i*6 + 1]);
                face.right_eye[0] =
                    l.mParameters.arrayXToNormalized(faceLandmarks[i*6 + 2]);
                face.right_eye[1] =
                    l.mParameters.arrayYToNormalized(faceLandmarks[i*6 + 3]);
                face.mouth[0] =
                    l.mParameters.arrayXToNormalized(faceLandmarks[i*6 + 4]);
                face.mouth[1] =
                    l.mParameters.arrayYToNormalized(faceLandmarks[i*6 + 5]);
            } else {
                face.id = 0;
                face.left_eye[0] = face.left_eye[1] = -2000;
                face.right_eye[0] = face.right_eye[1] = -2000;
                face.mouth[0] = face.mouth[1] = -2000;
            }
            faces.push_back(face);
        }

        metadata.faces = faces.editArray();
    }

    /* warning: locks SharedCameraCallbacks */
    callbackFaceDetection(client, metadata);

    return OK;
}

status_t FrameProcessor::process3aState(const CameraMetadata &frame,
        const sp<Camera2Client> &client) {

    ATRACE_CALL();
    camera_metadata_ro_entry_t entry;
    int cameraId = client->getCameraId();

    entry = frame.find(ANDROID_REQUEST_FRAME_COUNT);
    int32_t frameNumber = entry.data.i32[0];

    // Don't send 3A notifications for the same frame number twice
    if (frameNumber <= mLast3AFrameNumber) {
        ALOGV("%s: Already sent 3A for frame number %d, skipping",
                __FUNCTION__, frameNumber);
        return OK;
    }

    mLast3AFrameNumber = frameNumber;

    // Get 3A states from result metadata
    bool gotAllStates = true;

    AlgState new3aState;

    // TODO: Also use AE mode, AE trigger ID

    gotAllStates &= get3aResult<uint8_t>(frame, ANDROID_CONTROL_AF_MODE,
            &new3aState.afMode, frameNumber, cameraId);

    gotAllStates &= get3aResult<uint8_t>(frame, ANDROID_CONTROL_AWB_MODE,
            &new3aState.awbMode, frameNumber, cameraId);

    gotAllStates &= get3aResult<uint8_t>(frame, ANDROID_CONTROL_AE_STATE,
            &new3aState.aeState, frameNumber, cameraId);

    gotAllStates &= get3aResult<uint8_t>(frame, ANDROID_CONTROL_AF_STATE,
            &new3aState.afState, frameNumber, cameraId);

    gotAllStates &= get3aResult<uint8_t>(frame, ANDROID_CONTROL_AWB_STATE,
            &new3aState.awbState, frameNumber, cameraId);

    gotAllStates &= get3aResult<int32_t>(frame, ANDROID_CONTROL_AF_TRIGGER_ID,
            &new3aState.afTriggerId, frameNumber, cameraId);

    gotAllStates &= get3aResult<int32_t>(frame, ANDROID_CONTROL_AE_PRECAPTURE_ID,
            &new3aState.aeTriggerId, frameNumber, cameraId);

    if (!gotAllStates) return BAD_VALUE;

    if (new3aState.aeState != m3aState.aeState) {
        ALOGV("%s: Camera %d: AE state %d->%d",
                __FUNCTION__, cameraId,
                m3aState.aeState, new3aState.aeState);
        client->notifyAutoExposure(new3aState.aeState, new3aState.aeTriggerId);
    }

    if (new3aState.afState != m3aState.afState ||
        new3aState.afMode != m3aState.afMode ||
        new3aState.afTriggerId != m3aState.afTriggerId) {
        ALOGV("%s: Camera %d: AF state %d->%d. AF mode %d->%d. Trigger %d->%d",
                __FUNCTION__, cameraId,
                m3aState.afState, new3aState.afState,
                m3aState.afMode, new3aState.afMode,
                m3aState.afTriggerId, new3aState.afTriggerId);
        client->notifyAutoFocus(new3aState.afState, new3aState.afTriggerId);
    }
    if (new3aState.awbState != m3aState.awbState ||
        new3aState.awbMode != m3aState.awbMode) {
        ALOGV("%s: Camera %d: AWB state %d->%d. AWB mode %d->%d",
                __FUNCTION__, cameraId,
                m3aState.awbState, new3aState.awbState,
                m3aState.awbMode, new3aState.awbMode);
        client->notifyAutoWhitebalance(new3aState.awbState,
                new3aState.aeTriggerId);
    }

    m3aState = new3aState;

    return OK;
}

template<typename Src, typename T>
bool FrameProcessor::get3aResult(const CameraMetadata& result, int32_t tag,
        T* value, int32_t frameNumber, int cameraId) {
    camera_metadata_ro_entry_t entry;
    if (value == NULL) {
        ALOGE("%s: Camera %d: Value to write to is NULL",
                __FUNCTION__, cameraId);
        return false;
    }

    entry = result.find(tag);
    if (entry.count == 0) {
        ALOGE("%s: Camera %d: No %s provided by HAL for frame %d!",
                __FUNCTION__, cameraId,
                get_camera_metadata_tag_name(tag), frameNumber);
        return false;
    } else {
        switch(sizeof(Src)){
            case sizeof(uint8_t):
                *value = static_cast<T>(entry.data.u8[0]);
                break;
            case sizeof(int32_t):
                *value = static_cast<T>(entry.data.i32[0]);
                break;
            default:
                ALOGE("%s: Camera %d: Unsupported source",
                        __FUNCTION__, cameraId);
                return false;
        }
    }
    return true;
}


void FrameProcessor::callbackFaceDetection(sp<Camera2Client> client,
                                     const camera_frame_metadata &metadata) {

    camera_frame_metadata *metadata_ptr =
        const_cast<camera_frame_metadata*>(&metadata);

    /**
     * Filter out repeated 0-face callbacks,
     * but not when the last frame was >0
     */
    if (metadata.number_of_faces != 0 ||
        mLastFrameNumberOfFaces != metadata.number_of_faces) {

        Camera2Client::SharedCameraCallbacks::Lock
            l(client->mSharedCameraCallbacks);
        if (l.mRemoteCallback != NULL) {
            l.mRemoteCallback->dataCallback(CAMERA_MSG_PREVIEW_METADATA,
                                            NULL,
                                            metadata_ptr);
        }
    }

    mLastFrameNumberOfFaces = metadata.number_of_faces;
}

}; // namespace camera2
}; // namespace android
