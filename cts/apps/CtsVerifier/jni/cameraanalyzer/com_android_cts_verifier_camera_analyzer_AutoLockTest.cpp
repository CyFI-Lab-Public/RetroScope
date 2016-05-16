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

#define LOG_TAG "AutoLockJNI"
#include <utils/Log.h>
#include "com_android_cts_verifier_camera_analyzer_AutoLockTest.h"

#include <vector>
#include <string>
#include <string.h>

#include "testingimage.h"
#include "autolocktest.h"
#include "vec2.h"
#include "android/bitmap.h"

jlong Java_com_android_cts_verifier_camera_analyzer_AutoLockTest_createAutoLockTest(
        JNIEnv*      env,
        jobject      thiz) {

    AutoLockTest* testHandler = new AutoLockTest();
    long handlerAddress = (long)testHandler;
    return handlerAddress;
}

void Java_com_android_cts_verifier_camera_analyzer_AutoLockTest_createAutoLockClass(
        JNIEnv*      env,
        jobject      thiz,
        jlong        inputImageAddress,
        jlong        inputHandlerAddress,
        jlong        checkercenterAddress,
        jlong        checkerradiusAddress) {

    ALOGV("JNI createAutoLockClass starts!");
    long imageAddress = (long)inputImageAddress;
    long handlerAddress = (long)inputHandlerAddress;

    TestingImage *image = (TestingImage*) imageAddress;
    AutoLockTest *testHandler = (AutoLockTest*) handlerAddress;

    std::vector<std::vector< Vec2f > >* checkerCenter =
            (std::vector<std::vector< Vec2f > >*) (long) checkercenterAddress;
    std::vector<std::vector< float > >* checkerRadius =
            (std::vector<std::vector< float > >*) (long) checkerradiusAddress;
    ALOGV("Classes recovered");

    // Uses only the gray patches on the color checker for comparison.
    testHandler->addDataToList(image->getColorChecker(3, 4, 0, 6,
                                                      checkerCenter,
                                                      checkerRadius));

    delete image;
}

void Java_com_android_cts_verifier_camera_analyzer_AutoLockTest_processAutoLockTest(
        JNIEnv*          env,
        jobject          thiz,
        jlong            inputHandlerAddress,
        jbooleanArray    tempArray) {

    ALOGV("Processing Auto Lock data!");

    long handlerAddress = (long) inputHandlerAddress;
    AutoLockTest *testHandler = (AutoLockTest*) handlerAddress;

    testHandler->processData();

    // Converts the native boolean array into a java boolean array.
    const std::vector<bool>* nativeComparisonResults =
            testHandler->getComparisonResults();
    jboolean comparisonResults[nativeComparisonResults->size()];

    for (int i = 0; i < nativeComparisonResults->size(); ++i) {
        comparisonResults[i] = (jboolean) (*nativeComparisonResults)[i];
    }

    env->SetBooleanArrayRegion(tempArray,
                               0, nativeComparisonResults->size(),
                               comparisonResults);
    testHandler->clearData();
}
