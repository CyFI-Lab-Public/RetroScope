/*
 * Copyright (C) 2009 The Android Open Source Project
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

#include <cutils/log.h>
#include <jni.h>
#include <stdlib.h>

/*
 * This function is called automatically by the system when this
 * library is loaded. We use it to register all our native functions,
 * which is the recommended practice for Android.
 */
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;

    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return JNI_ERR;
    }

    extern int register_InstanceNonce(JNIEnv *);
    if (register_InstanceNonce(env)) {
        ALOGE("failed to register InstanceNonce");
        return JNI_ERR;
    }

    extern int register_StaticNonce(JNIEnv *);
    if (register_StaticNonce(env)) {
        ALOGE("failed to register StaticNonce");
        return JNI_ERR;
    }

    extern int register_JniCTest(JNIEnv *);
    if (register_JniCTest(env)) {
        ALOGE("failed to register JniCTest");
        return JNI_ERR;
    }

    extern int register_JniCppTest(JNIEnv *);
    if (register_JniCppTest(env)) {
        ALOGE("failed to register JniCppTest");
        return JNI_ERR;
    }

    return JNI_VERSION_1_4;
}
