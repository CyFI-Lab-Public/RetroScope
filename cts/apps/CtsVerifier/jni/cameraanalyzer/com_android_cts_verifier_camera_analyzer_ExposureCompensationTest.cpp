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

#define LOG_TAG "ExposureCompensationJNI"
#include <utils/Log.h>
#include <vector>
#include <string.h>

#include "android/bitmap.h"
#include "testingimage.h"
#include "exposurecompensationtest.h"
#include "vec2.h"

#include "com_android_cts_verifier_camera_analyzer_ExposureCompensationTest.h"

jlong Java_com_android_cts_verifier_camera_analyzer_ExposureCompensationTest_createExposureCompensationTest(
      JNIEnv*      env,
      jobject      thiz,
      jint         debugHeight,
      jint         debugWidth) {

    ExposureCompensationTest* testHandler =
            new ExposureCompensationTest(debugHeight, debugWidth);
    long handlerAddress = (long)testHandler;

    return handlerAddress;
}

void Java_com_android_cts_verifier_camera_analyzer_ExposureCompensationTest_createExposureCompensationClass(
        JNIEnv*      env,
        jobject      thiz,
        jlong        inputImageAddress,
        jlong        inputHandlerAddress,
        jlong        checkerCenterAddress,
        jlong        checkerRadiusAddress,
        jfloat       exposureValue) {

    ALOGV("JNI createExposureCompensationClass starts!");

    long imageAddress = (long)inputImageAddress;
    long handlerAddress = (long)inputHandlerAddress;

    TestingImage *inputImage = (TestingImage*) imageAddress;
    ExposureCompensationTest *testHandler =
            (ExposureCompensationTest*) handlerAddress;

    std::vector<std::vector< Vec2f > >* checkerCenter =
            (std::vector<std::vector< Vec2f > >*) (long) checkerCenterAddress;
    std::vector<std::vector< float > >* checkerRadius =
            (std::vector<std::vector< float > >*) (long) checkerRadiusAddress;

    const std::vector<Vec3f>* checkerValue =
            inputImage->getColorChecker(3, 4, 0, 6,
                                        checkerCenter, checkerRadius);
    testHandler->addDataToList((float) exposureValue, checkerValue);
    delete inputImage;
    delete checkerValue;
}

jstring Java_com_android_cts_verifier_camera_analyzer_ExposureCompensationTest_processExposureCompensationTest(
        JNIEnv*      env,
        jobject      thiz,
        jlong        inputHandlerAddress) {

    long handlerAddress = (long) inputHandlerAddress;
    ExposureCompensationTest *testHandler =
            (ExposureCompensationTest*) handlerAddress;

    testHandler->processData();

    const char* nativeDebugText = testHandler->getDebugText();
    ALOGV("%s", nativeDebugText);
    return env->NewStringUTF(nativeDebugText);
}
