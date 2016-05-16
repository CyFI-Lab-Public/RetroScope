/*
 * Copyright (C) 2011 The Android Open Source Project
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
#define LOG_NDEBUG 0

#define LOG_TAG "CameraTestsJNI"
#include <utils/Log.h>
#include "com_android_cts_verifier_camera_analyzer_CameraTests.h"
#include "android/bitmap.h"
#include "testingimage.h"
#include "imagetesthandler.h"

#include <string.h>

jlong Java_com_android_cts_verifier_camera_analyzer_CameraTests_findNative(
        JNIEnv*      env,
        jobject      thiz,
        jobject      inputBitmap) {

    ALOGV("JNI findNative starts!");

    // Verify that we can handle the input bitmap
    AndroidBitmapInfo inputInfo;
    AndroidBitmap_getInfo(env, inputBitmap, &inputInfo);
    if (inputInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888 &&
        inputInfo.format != ANDROID_BITMAP_FORMAT_RGB_565) {
        ALOGE("Only RGBA_8888 and RGB_565 bitmaps are supported, type was %d.",
             inputInfo.format);
    }

    // Get some references to the fields and class type of ColorChecker
    jclass thizCls = env->GetObjectClass(thiz);
    ALOGV("ColorChecker field and classes reference finished!");

    // Get raw inputs and outputs ready
    uint8_t *inputBuffer = NULL;
    int result = AndroidBitmap_lockPixels(
            env,
            inputBitmap,
            reinterpret_cast<void**>(&inputBuffer));

    if (result != ANDROID_BITMAP_RESULT_SUCCESS) {
        ALOGE("Unable to lock input bitmap");
    }

    uint8_t *outputImage = NULL;
    int outputWidth, outputHeight;

    ALOGV("Input and output images created!");

    // Find the color checker
    bool success;
    uint8_t *inputBufferRGBA = NULL;
    int inputStride;
    bool freeInputRGBA = false;
    switch (inputInfo.format) {
        case ANDROID_BITMAP_FORMAT_RGB_565: {
            // First convert to RGBA_8888
            inputBufferRGBA = new uint8_t[inputInfo.width *
                                          inputInfo.height *
                                          4];
            inputStride = inputInfo.width * 4;
            uint8_t *outP = inputBufferRGBA;
            for (int y = 0; y < inputInfo.height; y++ ) {
                uint16_t *inP = (uint16_t*)(&inputBuffer[y * inputInfo.stride]);
                for (int x = 0; x < inputInfo.width; x++) {
                    *(outP++) = ( ((*inP) >> 0) & 0x001F) << 3;
                    *(outP++) = ( ((*inP) >> 5) & 0x003F) << 2;
                    *(outP++) = ( ((*inP) >> 11) & 0x001F) << 3;
                    outP++;
                    inP++;
                }
            }
            freeInputRGBA = true;

            ALOGV("RGB_565 Format with width, height and stride as %d, %d, %d",
                 inputInfo.width, inputInfo.height, inputStride);
            break;
        }
        case ANDROID_BITMAP_FORMAT_RGBA_8888: {
            // Already in RGBA
            inputBufferRGBA = inputBuffer;
            inputStride = inputInfo.stride;
            ALOGV("RGB_8888 Format with width, height and stride as %d, %d, %d",
                 inputInfo.width, inputInfo.height, inputStride);
            break;
        }
    }

    TestingImage *input_testing_image =
            new TestingImage(inputBufferRGBA, inputInfo.height, inputInfo.width,
                             4, inputStride, 120, 160);

    long lp = (long)input_testing_image;

    result = AndroidBitmap_unlockPixels(env, inputBitmap);
    if (result != ANDROID_BITMAP_RESULT_SUCCESS) {
        ALOGE("Unable to unlock input bitmap");
    }

    if (freeInputRGBA) {
        ALOGV("Deleteing inputbufferRGBA");
        delete[] inputBufferRGBA;
    }

    return lp;
    ALOGV("Input format switched!");
}

jlong Java_com_android_cts_verifier_camera_analyzer_CameraTests_createImageTestHandler(
        JNIEnv*      env,
        jobject      thiz,
        jint         debugHeight,
        jint         debugWidth) {

    ImageTestHandler* testHandler =
            new ImageTestHandler(debugHeight, debugWidth);
    long handlerAddress = (long)testHandler;
    return handlerAddress;
}

void Java_com_android_cts_verifier_camera_analyzer_CameraTests_cleanUpHandler(
        JNIEnv*      env,
        jobject      thiz,
        jlong        inputHandlerAddress) {

    ImageTestHandler* testHandler = (ImageTestHandler*) (long) inputHandlerAddress;
    delete testHandler;
}

void Java_com_android_cts_verifier_camera_analyzer_CameraTests_displayHandlerDebugOutput(
        JNIEnv*      env,
        jobject      thiz,
        jlong        inputHandlerAddress) {

    jclass thizCls = env->GetObjectClass(thiz);
    jfieldID outputId = env->GetFieldID(thizCls, "mDebugOutput",
                                        "Landroid/graphics/Bitmap;");

    ImageTestHandler* testHandler = (ImageTestHandler*) (long) inputHandlerAddress;
    uint8_t *outputImage =  new uint8_t[testHandler->getDebugHeight() *
                                        testHandler->getDebugWidth() * 4];

    const unsigned char *debugoutput = testHandler->debug_output();
    memcpy(outputImage, debugoutput, testHandler->getDebugHeight() *
            testHandler->getDebugWidth() * 4);

    int outputWidth = testHandler->getDebugWidth();
    int outputHeight = testHandler->getDebugHeight();
    bool success = false;

    if (outputImage == NULL) {
        ALOGV("output Image is null!");
    } else {
        ALOGV("output Image is ready!");
    }

    // Create debug bitmap from output image data
    if (outputImage != NULL) {
        // Get method handles, create inputs to createBitmap
        jclass bitmapClass =
                env->FindClass("android/graphics/Bitmap");
        jclass bitmapConfigClass =
                env->FindClass("android/graphics/Bitmap$Config");

        jmethodID createBitmap = env->GetStaticMethodID(
            bitmapClass, "createBitmap",
            "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");

        jmethodID getConfig = env->GetStaticMethodID(
            bitmapConfigClass, "valueOf",
            "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");

        // Create bitmap config and bitmap
        jstring bitmapConfigValue = env->NewStringUTF("ARGB_8888");
        jobject rgbaConfig = env->CallStaticObjectMethod(bitmapConfigClass,
                                                         getConfig,
                                                         bitmapConfigValue);
        jobject outputBitmap = env->CallStaticObjectMethod(bitmapClass,
                                                           createBitmap,
                                                           outputWidth,
                                                           outputHeight,
                                                           rgbaConfig);
        // Copy output image into it
        uint8_t *outputBuffer;
        int result = AndroidBitmap_lockPixels(
                env,
                outputBitmap,
                reinterpret_cast<void**>(&outputBuffer) );

        if (result != ANDROID_BITMAP_RESULT_SUCCESS) {
            ALOGE("Unable to lock output bitmap");
        }

        memcpy(outputBuffer, outputImage, outputWidth * outputHeight * 4);

        result = AndroidBitmap_unlockPixels(env, outputBitmap);
        if (result != ANDROID_BITMAP_RESULT_SUCCESS) {
            ALOGE("Unable to unlock output bitmap");
        }

        // Write new Bitmap reference into mDebugOutput class member
        env->SetObjectField(thiz, outputId, outputBitmap);
        ALOGV("Copied to outputBitmap");
        delete [] outputImage;
        env->DeleteLocalRef(outputBitmap);
        env->DeleteLocalRef(rgbaConfig);
        env->DeleteLocalRef(bitmapClass);
        env->DeleteLocalRef(bitmapConfigClass);
    }
}
