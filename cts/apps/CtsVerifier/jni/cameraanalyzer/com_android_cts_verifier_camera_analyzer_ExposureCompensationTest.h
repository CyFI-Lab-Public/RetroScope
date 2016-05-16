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

#ifndef JNI_CAMERAANALYZER_EXPOSURECOMPENSATIONTEST_H
#define JNI_CAMERAANALYZER_EXPOSURECOMPENSATIONTEST_H

#include <jni.h>
#include <stdio.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_com_android_cts_verifier_camera_analyzer_ExposureCompensationTest_createExposureCompensationTest(
        JNIEnv*      env,
        jobject      thiz,
        jint         debugHeight,
        jint         debugWidth);

JNIEXPORT void JNICALL
Java_com_android_cts_verifier_camera_analyzer_ExposureCompensationTest_createExposureCompensationClass(
        JNIEnv*      env,
        jobject      thiz,
        jlong        inputImageAddress,
        jlong        inputHandlerAddress,
        jlong        checkerCenterAddress,
        jlong        checkerRadiusAddress,
        jfloat       exposureValue);


JNIEXPORT jstring JNICALL
Java_com_android_cts_verifier_camera_analyzer_ExposureCompensationTest_processExposureCompensationTest(
        JNIEnv*      env,
        jobject      thiz,
        jlong        inputHandlerAddress);
#ifdef __cplusplus
}
#endif

#endif
