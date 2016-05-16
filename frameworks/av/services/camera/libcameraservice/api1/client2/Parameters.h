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

#ifndef ANDROID_SERVERS_CAMERA_CAMERA2PARAMETERS_H
#define ANDROID_SERVERS_CAMERA_CAMERA2PARAMETERS_H

#include <system/graphics.h>

#include <utils/Errors.h>
#include <utils/Mutex.h>
#include <utils/String8.h>
#include <utils/Vector.h>
#include <utils/KeyedVector.h>
#include <camera/CameraParameters.h>
#include <camera/CameraMetadata.h>

namespace android {
namespace camera2 {

/**
 * Current camera state; this is the full state of the Camera under the old
 * camera API (contents of the CameraParameters object in a more-efficient
 * format, plus other state). The enum values are mostly based off the
 * corresponding camera2 enums, not the camera1 strings. A few are defined here
 * if they don't cleanly map to camera2 values.
 */
struct Parameters {
    /**
     * Parameters and other state
     */
    int cameraId;
    int cameraFacing;

    int previewWidth, previewHeight;
    int32_t previewFpsRange[2];
    int previewFormat;

    int previewTransform; // set by CAMERA_CMD_SET_DISPLAY_ORIENTATION

    int pictureWidth, pictureHeight;

    int32_t jpegThumbSize[2];
    uint8_t jpegQuality, jpegThumbQuality;
    int32_t jpegRotation;

    bool gpsEnabled;
    double gpsCoordinates[3];
    int64_t gpsTimestamp;
    String8 gpsProcessingMethod;

    uint8_t wbMode;
    uint8_t effectMode;
    uint8_t antibandingMode;
    uint8_t sceneMode;

    enum flashMode_t {
        FLASH_MODE_OFF = 0,
        FLASH_MODE_AUTO,
        FLASH_MODE_ON,
        FLASH_MODE_TORCH,
        FLASH_MODE_RED_EYE = ANDROID_CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE,
        FLASH_MODE_INVALID = -1
    } flashMode;

    enum focusMode_t {
        FOCUS_MODE_AUTO = ANDROID_CONTROL_AF_MODE_AUTO,
        FOCUS_MODE_MACRO = ANDROID_CONTROL_AF_MODE_MACRO,
        FOCUS_MODE_CONTINUOUS_VIDEO = ANDROID_CONTROL_AF_MODE_CONTINUOUS_VIDEO,
        FOCUS_MODE_CONTINUOUS_PICTURE = ANDROID_CONTROL_AF_MODE_CONTINUOUS_PICTURE,
        FOCUS_MODE_EDOF = ANDROID_CONTROL_AF_MODE_EDOF,
        FOCUS_MODE_INFINITY,
        FOCUS_MODE_FIXED,
        FOCUS_MODE_INVALID = -1
    } focusMode;

    uint8_t focusState; // Latest focus state from HAL

    // For use with triggerAfWithAuto quirk
    focusMode_t shadowFocusMode;

    struct Area {
        int left, top, right, bottom;
        int weight;
        Area() {}
        Area(int left, int top, int right, int bottom, int weight):
                left(left), top(top), right(right), bottom(bottom),
                weight(weight) {}
        bool isEmpty() const {
            return (left == 0) && (top == 0) && (right == 0) && (bottom == 0);
        }
    };
    Vector<Area> focusingAreas;

    struct Size {
        int32_t width;
        int32_t height;
    };

    int32_t exposureCompensation;
    bool autoExposureLock;
    bool autoWhiteBalanceLock;

    Vector<Area> meteringAreas;

    int zoom;

    int videoWidth, videoHeight;

    bool recordingHint;
    bool videoStabilization;

    enum lightFxMode_t {
        LIGHTFX_NONE = 0,
        LIGHTFX_LOWLIGHT,
        LIGHTFX_HDR
    } lightFx;

    CameraParameters params;
    String8 paramsFlattened;

    // These parameters are also part of the camera API-visible state, but not
    // directly listed in Camera.Parameters
    bool storeMetadataInBuffers;
    bool playShutterSound;
    bool enableFaceDetect;

    bool enableFocusMoveMessages;
    int afTriggerCounter;
    int afStateCounter;
    int currentAfTriggerId;
    bool afInMotion;

    int precaptureTriggerCounter;

    int takePictureCounter;

    uint32_t previewCallbackFlags;
    bool previewCallbackOneShot;
    bool previewCallbackSurface;

    bool zslMode;

    // Overall camera state
    enum State {
        DISCONNECTED,
        STOPPED,
        WAITING_FOR_PREVIEW_WINDOW,
        PREVIEW,
        RECORD,
        STILL_CAPTURE,
        VIDEO_SNAPSHOT
    } state;

    // Number of zoom steps to simulate
    static const unsigned int NUM_ZOOM_STEPS = 100;
    // Max preview size allowed
    static const unsigned int MAX_PREVIEW_WIDTH = 1920;
    static const unsigned int MAX_PREVIEW_HEIGHT = 1080;
    // Aspect ratio tolerance
    static const float ASPECT_RATIO_TOLERANCE = 0.001;

    // Full static camera info, object owned by someone else, such as
    // Camera2Device.
    const CameraMetadata *info;

    // Fast-access static device information; this is a subset of the
    // information available through the staticInfo() method, used for
    // frequently-accessed values or values that have to be calculated from the
    // static information.
    struct DeviceInfo {
        int32_t arrayWidth;
        int32_t arrayHeight;
        int32_t bestStillCaptureFpsRange[2];
        uint8_t bestFaceDetectMode;
        int32_t maxFaces;
        struct OverrideModes {
            flashMode_t flashMode;
            uint8_t     wbMode;
            focusMode_t focusMode;
            OverrideModes():
                    flashMode(FLASH_MODE_INVALID),
                    wbMode(ANDROID_CONTROL_AWB_MODE_OFF),
                    focusMode(FOCUS_MODE_INVALID) {
            }
        };
        DefaultKeyedVector<uint8_t, OverrideModes> sceneModeOverrides;
        float minFocalLength;
        bool useFlexibleYuv;
    } fastInfo;

    // Quirks information; these are short-lived flags to enable workarounds for
    // incomplete HAL implementations
    struct Quirks {
        bool triggerAfWithAuto;
        bool useZslFormat;
        bool meteringCropRegion;
        bool partialResults;
    } quirks;

    /**
     * Parameter manipulation and setup methods
     */

    Parameters(int cameraId, int cameraFacing);
    ~Parameters();

    // Sets up default parameters
    status_t initialize(const CameraMetadata *info);

    // Build fast-access device static info from static info
    status_t buildFastInfo();
    // Query for quirks from static info
    status_t buildQuirks();

    // Get entry from camera static characteristics information. min/maxCount
    // are used for error checking the number of values in the entry. 0 for
    // max/minCount means to do no bounds check in that direction. In case of
    // error, the entry data pointer is null and the count is 0.
    camera_metadata_ro_entry_t staticInfo(uint32_t tag,
            size_t minCount=0, size_t maxCount=0, bool required=true) const;

    // Validate and update camera parameters based on new settings
    status_t set(const String8 &paramString);

    // Retrieve the current settings
    String8 get() const;

    // Update passed-in request for common parameters
    status_t updateRequest(CameraMetadata *request) const;

    // Add/update JPEG entries in metadata
    status_t updateRequestJpeg(CameraMetadata *request) const;

    // Calculate the crop region rectangle based on current stream sizes
    struct CropRegion {
        float left;
        float top;
        float width;
        float height;

        enum Outputs {
            OUTPUT_PREVIEW         = 0x01,
            OUTPUT_VIDEO           = 0x02,
            OUTPUT_JPEG_THUMBNAIL  = 0x04,
            OUTPUT_PICTURE         = 0x08,
        };
    };
    CropRegion calculateCropRegion(CropRegion::Outputs outputs) const;

    // Calculate the field of view of the high-resolution JPEG capture
    status_t calculatePictureFovs(float *horizFov, float *vertFov) const;

    // Static methods for debugging and converting between camera1 and camera2
    // parameters

    static const char *getStateName(State state);

    static int formatStringToEnum(const char *format);
    static const char *formatEnumToString(int format);

    static int wbModeStringToEnum(const char *wbMode);
    static const char* wbModeEnumToString(uint8_t wbMode);
    static int effectModeStringToEnum(const char *effectMode);
    static int abModeStringToEnum(const char *abMode);
    static int sceneModeStringToEnum(const char *sceneMode);
    static flashMode_t flashModeStringToEnum(const char *flashMode);
    static const char* flashModeEnumToString(flashMode_t flashMode);
    static focusMode_t focusModeStringToEnum(const char *focusMode);
    static const char* focusModeEnumToString(focusMode_t focusMode);
    static lightFxMode_t lightFxStringToEnum(const char *lightFxMode);

    static status_t parseAreas(const char *areasCStr,
            Vector<Area> *areas);

    enum AreaKind
    {
        AREA_KIND_FOCUS,
        AREA_KIND_METERING
    };
    status_t validateAreas(const Vector<Area> &areas,
                                  size_t maxRegions,
                                  AreaKind areaKind) const;
    static bool boolFromString(const char *boolStr);

    // Map from camera orientation + facing to gralloc transform enum
    static int degToTransform(int degrees, bool mirror);

    // API specifies FPS ranges are done in fixed point integer, with LSB = 0.001.
    // Note that this doesn't apply to the (deprecated) single FPS value.
    static const int kFpsToApiScale = 1000;

    // Transform between (-1000,-1000)-(1000,1000) normalized coords from camera
    // API and HAL2 (0,0)-(activePixelArray.width/height) coordinates
    int arrayXToNormalized(int width) const;
    int arrayYToNormalized(int height) const;
    int normalizedXToArray(int x) const;
    int normalizedYToArray(int y) const;

    struct Range {
        int min;
        int max;
    };

    int32_t fpsFromRange(int32_t min, int32_t max) const;

private:

    // Convert between HAL2 sensor array coordinates and
    // viewfinder crop-region relative array coordinates
    int cropXToArray(int x) const;
    int cropYToArray(int y) const;
    int arrayXToCrop(int x) const;
    int arrayYToCrop(int y) const;

    // Convert between viewfinder crop-region relative array coordinates
    // and camera API (-1000,1000)-(1000,1000) normalized coords
    int cropXToNormalized(int x) const;
    int cropYToNormalized(int y) const;
    int normalizedXToCrop(int x) const;
    int normalizedYToCrop(int y) const;

    Vector<Size> availablePreviewSizes;
    // Get size list (that are no larger than limit) from static metadata.
    status_t getFilteredPreviewSizes(Size limit, Vector<Size> *sizes);
    // Get max size (from the size array) that matches the given aspect ratio.
    Size getMaxSizeForRatio(float ratio, const int32_t* sizeArray, size_t count);
};

// This class encapsulates the Parameters class so that it can only be accessed
// by constructing a Lock object, which locks the SharedParameter's mutex.
class SharedParameters {
  public:
    SharedParameters(int cameraId, int cameraFacing):
            mParameters(cameraId, cameraFacing) {
    }

    template<typename S, typename P>
    class BaseLock {
      public:
        BaseLock(S &p):
                mParameters(p.mParameters),
                mSharedParameters(p) {
            mSharedParameters.mLock.lock();
        }

        ~BaseLock() {
            mSharedParameters.mLock.unlock();
        }
        P &mParameters;
      private:
        // Disallow copying, default construction
        BaseLock();
        BaseLock(const BaseLock &);
        BaseLock &operator=(const BaseLock &);
        S &mSharedParameters;
    };
    typedef BaseLock<SharedParameters, Parameters> Lock;
    typedef BaseLock<const SharedParameters, const Parameters> ReadLock;

    // Access static info, read-only and immutable, so no lock needed
    camera_metadata_ro_entry_t staticInfo(uint32_t tag,
            size_t minCount=0, size_t maxCount=0) const {
        return mParameters.staticInfo(tag, minCount, maxCount);
    }

    // Only use for dumping or other debugging
    const Parameters &unsafeAccess() {
        return mParameters;
    }
  private:
    Parameters mParameters;
    mutable Mutex mLock;
};


}; // namespace camera2
}; // namespace android

#endif
