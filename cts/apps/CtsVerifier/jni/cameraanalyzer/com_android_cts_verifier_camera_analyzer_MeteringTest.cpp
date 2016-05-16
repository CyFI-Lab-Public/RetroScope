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

#define LOG_TAG "MeteringJNI"
#include <utils/Log.h>
#include "com_android_cts_verifier_camera_analyzer_MeteringTest.h"

#include <vector>
#include <string>
#include <string.h>
#include <math.h>

#include "testingimage.h"
#include "meteringtest.h"
#include "vec2.h"
#include "android/bitmap.h"

jlong Java_com_android_cts_verifier_camera_analyzer_MeteringTest_createMeteringTest(
        JNIEnv*      env,
        jobject      thiz) {

    MeteringTest* testHandler = new MeteringTest();
    long handlerAddress = (long)testHandler;
    return handlerAddress;
}

void Java_com_android_cts_verifier_camera_analyzer_MeteringTest_createMeteringClass(
        JNIEnv*      env,
        jobject      thiz,
        jlong        inputImageAddress,
        jlong        inputHandlerAddress,
        jlong        checkercenterAddress,
        jlong        checkerradiusAddress){

    ALOGV("JNI createMeteringClass starts!");
    long imageAddress = (long)inputImageAddress;
    long handlerAddress = (long)inputHandlerAddress;

    TestingImage *image = (TestingImage*) imageAddress;
    MeteringTest *testHandler = (MeteringTest*) handlerAddress;

    std::vector<std::vector< Vec2f > >* checkerCenter =
            (std::vector<std::vector< Vec2f > >*) (long) checkercenterAddress;
    std::vector<std::vector< float > >* checkerRadius =
            (std::vector<std::vector< float > >*) (long) checkerradiusAddress;
    ALOGV("Classes recovered");

    testHandler->addDataToList(image->getColorChecker(3, 4, 0, 6,
                                                      checkerCenter,
                                                      checkerRadius));

    delete image;
}

void Java_com_android_cts_verifier_camera_analyzer_MeteringTest_processMeteringTest(
        JNIEnv*          env,
        jobject          thiz,
        jlong            inputHandlerAddress,
        jbooleanArray    tempArray) {

    ALOGV("Processing Auto Lock data!");

    long handlerAddress = (long) inputHandlerAddress;
    MeteringTest *testHandler = (MeteringTest*) handlerAddress;

    testHandler->processData();

    const std::vector<bool>* nativeComparisonResults =
            testHandler->getComparisonResults();
    jboolean jComparisonResults[nativeComparisonResults->size()];

    for (int i = 0; i < nativeComparisonResults->size(); ++i) {
        jComparisonResults[i] = (jboolean) (*nativeComparisonResults)[i];
    }

    env->SetBooleanArrayRegion(tempArray,
                               0, nativeComparisonResults->size(),
                               jComparisonResults);
    testHandler->clearData();
}

// Find the gray checker borders from the native array of checker center and
// radius. Convert the coordinate to the coordinates accepted by Android
// Camera.Area type, which defines the top left corner to (-1000, -1000) and
// bottom right corner to (1000, 1000).
void Java_com_android_cts_verifier_camera_analyzer_MeteringTest_findGreyCoordinates(
        JNIEnv*      env,
        jobject      thiz,
        jintArray    greyCoordinates,
        jlong        checkercenterAddress,
        jlong        checkerradiusAddress){

    ALOGV("Start finding grey coordinates");

    std::vector<std::vector< Vec2f > >* checkerCenter =
            (std::vector<std::vector< Vec2f > >*) (long) checkercenterAddress;
    std::vector<std::vector< float > >* checkerRadius =
            (std::vector<std::vector< float > >*) (long) checkerradiusAddress;

    ALOGV("Checker recovered!");
    int nativeGreyCoordinates[24];

    for (int i = 0; i < 6; ++i) {
        float radius = sqrt((*checkerRadius)[3][i]);
        nativeGreyCoordinates[i * 4] = static_cast<int>(
                ((*checkerCenter)[3][i].y() - radius)
                / 160.0 * 2000.0 - 1000.0);
        nativeGreyCoordinates[i * 4 + 1] = static_cast<int>(
                ((*checkerCenter)[3][i].x() - radius)
                / 120.0 * 2000.0 - 1000.0);
        nativeGreyCoordinates[i * 4 + 2] = static_cast<int>(
                ((*checkerCenter)[3][i].y() + radius)
                / 160.0 * 2000.0 - 1000.0);
        nativeGreyCoordinates[i * 4 + 3] = static_cast<int>(
                ((*checkerCenter)[3][i].x() + radius)
                / 120.0 * 2000.0 - 1000.0);

        ALOGV("checker is bounded by %f, %f, %f",
             (*checkerCenter)[3][i].x(), (*checkerCenter)[3][i].y(), radius);

        ALOGV("Square is bounded by %d, %d, %d, %d",
             nativeGreyCoordinates[i * 4], nativeGreyCoordinates[i * 4 + 1],
             nativeGreyCoordinates[i * 4 + 2],
             nativeGreyCoordinates[i * 4 + 3]);
    }

    env->SetIntArrayRegion(greyCoordinates, 0, 24, nativeGreyCoordinates);
}
