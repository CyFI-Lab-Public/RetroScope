/*
 * Copyright (C) 2013 The Android Open Source Project
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
 *
 */
#include <jni.h>
#include <sys/prctl.h>

jint android_os_cts_OSFeatures_getNoNewPrivs(JNIEnv* env, jobject thiz)
{
    return prctl(PR_GET_NO_NEW_PRIVS, 0, 0, 0, 0);
}

jint android_os_cts_OSFeatures_prctlCapBsetRead(JNIEnv* env, jobject thiz, jint i)
{
    return prctl(PR_CAPBSET_READ, i, 0, 0, 0);
}

static JNINativeMethod gMethods[] = {
    {  "getNoNewPrivs", "()I",
            (void *) android_os_cts_OSFeatures_getNoNewPrivs  },
    {  "prctlCapBsetRead", "(I)I",
            (void *) android_os_cts_OSFeatures_prctlCapBsetRead },
};

int register_android_os_cts_OSFeatures(JNIEnv* env)
{
    jclass clazz = env->FindClass("android/os/cts/OSFeatures");

    return env->RegisterNatives(clazz, gMethods,
            sizeof(gMethods) / sizeof(JNINativeMethod));
}
