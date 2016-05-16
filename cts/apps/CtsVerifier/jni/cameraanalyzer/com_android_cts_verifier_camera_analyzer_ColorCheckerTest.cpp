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

#define LOG_TAG "FindColorCheckerJNI"
#include <utils/Log.h>
#include "com_android_cts_verifier_camera_analyzer_ColorCheckerTest.h"

#include <string.h>
#include "android/bitmap.h"
#include "colorcheckertest.h"
#include "testingimage.h"

jlong Java_com_android_cts_verifier_camera_analyzer_ColorCheckerTest_createColorCheckerTest(
        JNIEnv*      env,
        jobject      thiz,
        jint         debugHeight,
        jint         debugWidth) {
    ColorCheckerTest* testHandler = new ColorCheckerTest(debugHeight,
                                                         debugWidth);
    long testHandlerAddress = (long)testHandler;
    return testHandlerAddress;
}

void Java_com_android_cts_verifier_camera_analyzer_ColorCheckerTest_createColorCheckerClass(
        JNIEnv*      env,
        jobject      thiz,
        jlong        inputImageAddress,
        jlong        inputHandlerAddress) {
    ALOGV("JNI createColorCheckerClass starts!");

    TestingImage *testImage = (TestingImage*) (long) inputImageAddress;
    ColorCheckerTest *testHandler = (ColorCheckerTest*)
            (long) inputHandlerAddress;

    testHandler->addTestingImage(testImage);
}

jboolean Java_com_android_cts_verifier_camera_analyzer_ColorCheckerTest_processColorCheckerTest(
        JNIEnv*      env,
        jobject      thiz,
        jlong        inputHandlerAddress) {

    ColorCheckerTest *testHandler = (ColorCheckerTest*)
            (long) inputHandlerAddress;
    testHandler->processData();
    return testHandler->getSuccess();
}

jlong Java_com_android_cts_verifier_camera_analyzer_ColorCheckerTest_getColorCheckerRadiusAdd(
        JNIEnv*      env,
        jobject      thiz,
        jlong        inputHandlerAddress) {

    ColorCheckerTest *testHandler = (ColorCheckerTest*)
            (long) inputHandlerAddress;
    long rtn = (long) testHandler->getCheckerRadiusAdd();
    return rtn;
}

jlong Java_com_android_cts_verifier_camera_analyzer_ColorCheckerTest_getColorCheckerCenterAdd(
        JNIEnv*      env,
        jobject      thiz,
        jlong        inputHandlerAddress) {

    ColorCheckerTest *testHandler = (ColorCheckerTest*)
            (long) inputHandlerAddress;

    long rtn = (long) testHandler->getCheckerCenterAdd();
    return rtn;
}
