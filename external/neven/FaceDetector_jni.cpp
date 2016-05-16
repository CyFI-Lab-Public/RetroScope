/*
 * Copyright (C) 2006 The Android Open Source Project
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

#include <stdlib.h>
#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>

#include <utils/misc.h>
#include <utils/String8.h>
#include <utils/Log.h>

#include <core/SkBitmap.h>

#include "jni.h"
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"

using namespace android;

extern "C"
{
    #include <fd_emb_sdk.h>
}

struct FaceData
{
    float confidence;
    float midpointx;
    float midpointy;
    float eyedist;
};

struct FaceOffsets
{
    jfieldID    confidence;
    jfieldID    midpointx;
    jfieldID    midpointy;
    jfieldID    eyedist;
    jfieldID    eulerx;
    jfieldID    eulery;
    jfieldID    eulerz;
} gFaceOffsets;

struct FaceDetectorOffsets
{
    jfieldID    fd;
    jfieldID    sdk;
    jfieldID    dcr;
    jfieldID    width;
    jfieldID    height;
    jfieldID    maxFaces;
    jfieldID    bwbuffer;
} gFaceDetectorOffsets;

jfieldID nativeBitmapID;

// ---------------------------------------------------------------------------

static void getFaceData(btk_HDCR hdcr, FaceData* fdata)
{
    btk_Node leftEye, rightEye;

    btk_DCR_getNode(hdcr, 0, &leftEye);
    btk_DCR_getNode(hdcr, 1, &rightEye);

    fdata->eyedist = (float)(rightEye.x - leftEye.x) / (1 << 16);
    fdata->midpointx = (float)(rightEye.x + leftEye.x) / (1 << 17);
    fdata->midpointy = (float)(rightEye.y + leftEye.y) / (1 << 17);
    fdata->confidence = (float)btk_DCR_confidence(hdcr) / (1 << 24);
}

// ---------------------------------------------------------------------------

static void doThrow(JNIEnv* env, const char* exc, const char* msg = NULL)
{
    jclass npeClazz = env->FindClass(exc);
    env->ThrowNew(npeClazz, msg);
}

static void
nativeClassInit
(JNIEnv *_env, jclass _this)
{
    gFaceDetectorOffsets.fd             = _env->GetFieldID(_this, "mFD", "I");
    gFaceDetectorOffsets.sdk            = _env->GetFieldID(_this, "mSDK", "I");
    gFaceDetectorOffsets.dcr            = _env->GetFieldID(_this, "mDCR", "I");
    gFaceDetectorOffsets.width          = _env->GetFieldID(_this, "mWidth", "I");
    gFaceDetectorOffsets.height         = _env->GetFieldID(_this, "mHeight", "I");
    gFaceDetectorOffsets.maxFaces       = _env->GetFieldID(_this, "mMaxFaces", "I");
    gFaceDetectorOffsets.bwbuffer       = _env->GetFieldID(_this, "mBWBuffer", "[B");

    jclass faceClass = _env->FindClass("android/media/FaceDetector$Face");
    gFaceOffsets.confidence  = _env->GetFieldID(faceClass, "mConfidence", "F");
    gFaceOffsets.midpointx   = _env->GetFieldID(faceClass, "mMidPointX", "F");
    gFaceOffsets.midpointy   = _env->GetFieldID(faceClass, "mMidPointY", "F");
    gFaceOffsets.eyedist     = _env->GetFieldID(faceClass, "mEyesDist", "F");
    gFaceOffsets.eulerx      = _env->GetFieldID(faceClass, "mPoseEulerX", "F");
    gFaceOffsets.eulery      = _env->GetFieldID(faceClass, "mPoseEulerY", "F");
    gFaceOffsets.eulerz      = _env->GetFieldID(faceClass, "mPoseEulerZ", "F");

    jclass bitmapClass = _env->FindClass("android/graphics/Bitmap");
    nativeBitmapID = _env->GetFieldID(bitmapClass, "mNativeBitmap", "I");
}

// ---------------------------------------------------------------------------

static jint
initialize(JNIEnv *_env, jobject _this,
     jint w, jint h, jint maxFaces)
{
    // load the configuration file
    const char* root = getenv("ANDROID_ROOT");
    String8 path(root);
    path.appendPath("usr/share/bmd/RFFstd_501.bmd");
    // path.appendPath("usr/share/bmd/RFFspeed_501.bmd");

    const int MAX_FILE_SIZE = 65536;
    void* initData = malloc( MAX_FILE_SIZE ); /* enough to fit entire file */
    int filedesc = open(path.string(), O_RDONLY);
    int initDataSize = read(filedesc, initData, MAX_FILE_SIZE);
    close(filedesc);

    // --------------------------------------------------------------------
    btk_HSDK sdk = NULL;
    btk_SDKCreateParam sdkParam = btk_SDK_defaultParam();
    sdkParam.fpMalloc = malloc;
    sdkParam.fpFree = free;
    sdkParam.maxImageWidth = w;
    sdkParam.maxImageHeight = h;

    btk_Status status = btk_SDK_create(&sdkParam, &sdk);
    // make sure everything went well
    if (status != btk_STATUS_OK) {
        // XXX: be more precise about what went wrong
        doThrow(_env, "java/lang/OutOfMemoryError", NULL);
        return 0;
    }

    btk_HDCR dcr = NULL;
    btk_DCRCreateParam dcrParam = btk_DCR_defaultParam();
    btk_DCR_create( sdk, &dcrParam, &dcr );

    btk_HFaceFinder fd = NULL;
    btk_FaceFinderCreateParam fdParam = btk_FaceFinder_defaultParam();
    fdParam.pModuleParam = initData;
    fdParam.moduleParamSize = initDataSize;
    fdParam.maxDetectableFaces = maxFaces;
    status = btk_FaceFinder_create( sdk, &fdParam, &fd );
    btk_FaceFinder_setRange(fd, 20, w/2); /* set eye distance range */

    // make sure everything went well
    if (status != btk_STATUS_OK) {
        // XXX: be more precise about what went wrong
        doThrow(_env, "java/lang/OutOfMemoryError", NULL);
        return 0;
    }

    // free the configuration file
    free(initData);

    // initialize the java object
    _env->SetIntField(_this, gFaceDetectorOffsets.fd,  (jint)fd);
    _env->SetIntField(_this, gFaceDetectorOffsets.sdk, (jint)sdk);
    _env->SetIntField(_this, gFaceDetectorOffsets.dcr, (jint)dcr);

    return 1;
}

static void
destroy(JNIEnv *_env, jobject _this)
{
    btk_HFaceFinder hfd =
        (btk_HFaceFinder)(_env->GetIntField(_this, gFaceDetectorOffsets.fd));
    btk_FaceFinder_close( hfd );

    btk_HDCR hdcr = (btk_HDCR)(_env->GetIntField(_this, gFaceDetectorOffsets.dcr));
    btk_DCR_close( hdcr );

    btk_HSDK hsdk = (btk_HSDK)(_env->GetIntField(_this, gFaceDetectorOffsets.sdk));
    btk_SDK_close( hsdk );
}

static jint
detect(JNIEnv *_env, jobject _this,
     jobject bitmap)
{
    // get the fields we need
    btk_HDCR hdcr = (btk_HDCR)(_env->GetIntField(_this, gFaceDetectorOffsets.dcr));
    btk_HFaceFinder hfd =
        (btk_HFaceFinder)(_env->GetIntField(_this, gFaceDetectorOffsets.fd));
    u32 maxFaces = _env->GetIntField(_this, gFaceDetectorOffsets.maxFaces);
    u32 width = _env->GetIntField(_this, gFaceDetectorOffsets.width);
    u32 height = _env->GetIntField(_this, gFaceDetectorOffsets.height);

    jbyteArray bwbufferObject = (jbyteArray)
            _env->GetObjectField(_this, gFaceDetectorOffsets.bwbuffer);

    // get to the native bitmap
    SkBitmap const * nativeBitmap =
            (SkBitmap const *)_env->GetIntField(bitmap, nativeBitmapID);

    // get to our BW temporary buffer
    jbyte* bwbuffer = _env->GetByteArrayElements(bwbufferObject, 0);

    // convert the image to B/W
    uint8_t* dst = (uint8_t*)bwbuffer;

    // manage the life-time of locking our pixels
    SkAutoLockPixels alp(*nativeBitmap);

    uint16_t const* src = (uint16_t const*)nativeBitmap->getPixels();
    int wpr = nativeBitmap->rowBytes() / 2;
    for (u32 y=0 ; y<height; y++) {
        for (u32 x=0 ; x<width ; x++) {
            uint16_t rgb = src[x];
            int r  = rgb >> 11;
            int g2 = (rgb >> 5) & 0x3F;
            int b  = rgb & 0x1F;
            // L coefficients 0.299 0.587 0.11
            int L = (r<<1) + (g2<<1) + (g2>>1) + b;
            *dst++ = L;
        }
        src += wpr;
    }

    // run detection
    btk_DCR_assignGrayByteImage(hdcr, bwbuffer, width, height);

    int numberOfFaces = 0;
    if (btk_FaceFinder_putDCR(hfd, hdcr) == btk_STATUS_OK) {
        numberOfFaces = btk_FaceFinder_faces(hfd);
    } else {
        ALOGE("ERROR: Return 0 faces because error exists in btk_FaceFinder_putDCR.\n");
    }

    // release the arrays we're using
    _env->ReleaseByteArrayElements(bwbufferObject, bwbuffer, 0);
    return numberOfFaces;
}

static void
get_face(JNIEnv *_env, jobject _this,
     jobject face, jint index)
{
    btk_HDCR hdcr = (btk_HDCR)(_env->GetIntField(_this, gFaceDetectorOffsets.dcr));
    btk_HFaceFinder hfd =
        (btk_HFaceFinder)(_env->GetIntField(_this, gFaceDetectorOffsets.fd));

    FaceData faceData;
    btk_FaceFinder_getDCR(hfd, hdcr);
    getFaceData(hdcr, &faceData);

    const float X2F = 1.0f / 65536.0f;
    _env->SetFloatField(face, gFaceOffsets.confidence,  faceData.confidence);
    _env->SetFloatField(face, gFaceOffsets.midpointx,   faceData.midpointx);
    _env->SetFloatField(face, gFaceOffsets.midpointy,   faceData.midpointy);
    _env->SetFloatField(face, gFaceOffsets.eyedist,     faceData.eyedist);
    _env->SetFloatField(face, gFaceOffsets.eulerx,      0);
    _env->SetFloatField(face, gFaceOffsets.eulery,      0);
    _env->SetFloatField(face, gFaceOffsets.eulerz,      0);
}

// ---------------------------------------------------------------------------

static const char *classPathName = "android/media/FaceDetector";

static JNINativeMethod methods[] = {
{"nativeClassInit", "()V",                                  (void*)nativeClassInit },
{"fft_initialize",  "(III)I",                               (void*)initialize },
{"fft_detect",      "(Landroid/graphics/Bitmap;)I",         (void*)detect },
{"fft_get_face",    "(Landroid/media/FaceDetector$Face;I)V",(void*)get_face },
{"fft_destroy",     "()V",                                  (void*)destroy },
};

int register_android_media_FaceDetector(JNIEnv *_env)
{
    return android::AndroidRuntime::registerNativeMethods(
            _env, classPathName, methods, NELEM(methods));
}

// ---------------------------------------------------------------------------

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("ERROR: GetEnv failed\n");
        goto bail;
    }
    assert(env != NULL);

    if (register_android_media_FaceDetector(env) < 0) {
        ALOGE("ERROR: MediaPlayer native registration failed\n");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}
