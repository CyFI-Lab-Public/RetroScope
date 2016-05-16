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

#define LOG_TAG "WhiteBalanceJNI"
#include <utils/Log.h>
#include "com_android_cts_verifier_camera_analyzer_WhiteBalanceTest.h"

#include <vector>
#include <string>
#include <string.h>

#include "testingimage.h"
#include "whitebalancetest.h"
#include "vec2.h"
#include "android/bitmap.h"

jlong Java_com_android_cts_verifier_camera_analyzer_WhiteBalanceTest_createWhiteBalanceTest(
        JNIEnv*      env,
        jobject      thiz) {

    WhiteBalanceTest* testHandler = new WhiteBalanceTest();
    long handlerAddress = (long)testHandler;
    return handlerAddress;
}

void Java_com_android_cts_verifier_camera_analyzer_WhiteBalanceTest_createWhiteBalanceClass(
        JNIEnv*      env,
        jobject      thiz,
        jlong        inputImageAddress,
        jlong        inputHandlerAddress,
        jlong        checkercenterAddress,
        jlong        checkerradiusAddress,
        jstring      whiteBalance){

    ALOGV("JNI createWhiteBalanceClass starts!");
    long imageAddress = (long)inputImageAddress;
    long handlerAddress = (long)inputHandlerAddress;

    TestingImage *image = (TestingImage*) imageAddress;
    WhiteBalanceTest *testHandler = (WhiteBalanceTest*) handlerAddress;

    std::vector<std::vector< Vec2f > >* checkerCenter =
        (std::vector<std::vector< Vec2f > >*) (long) checkercenterAddress;
    std::vector<std::vector< float > >* checkerRadius =
        (std::vector<std::vector< float > >*) (long) checkerradiusAddress;
    ALOGV("Classes recovered");

    jboolean isCopy;
    const char* stringWhiteBalance =
            env->GetStringUTFChars(whiteBalance, &isCopy);
    ALOGV("White Balance is %s", stringWhiteBalance);

    // Adds the gray checker's RGB values to the test handler.
    testHandler->addDataToList(stringWhiteBalance,
                               image->getColorChecker(3, 4, 0, 6,
                                                      checkerCenter,
                                                      checkerRadius));

    env->ReleaseStringUTFChars(whiteBalance, stringWhiteBalance);
    delete image;
}

jint Java_com_android_cts_verifier_camera_analyzer_WhiteBalanceTest_processWhiteBalanceTest(
    JNIEnv*      env,
    jobject      thiz,
    jlong        inputHandlerAddress) {
  ALOGV("Processing white balance test");

  long handlerAddress = (long) inputHandlerAddress;
  WhiteBalanceTest *testHandler = (WhiteBalanceTest*) handlerAddress;

  testHandler->processData();

  ALOGV("CCT is %d", testHandler->getCorrelatedColorTemp());
  return testHandler->getCorrelatedColorTemp();
}
