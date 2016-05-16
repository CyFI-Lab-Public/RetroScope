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
 */

#include <errno.h>
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>

/*
 * Native methods used by
 * cts/tests/tests/permission/src/android/security/cts/LinuxRngTest.java
 */

static void throwIOException(JNIEnv* env, const char *format, ...) {
    va_list ap;
    va_start(ap, format);

    char *message;
    vasprintf(&message, format, ap);

    va_end(ap);

    jclass cls = env->FindClass("java/io/IOException");
    env->ThrowNew(cls, message);

    free(message);
}

jint android_security_cts_LinuxRngTest_getCharDeviceMajor(JNIEnv* env,
        jobject thiz, jstring name)
{
    const char* nameStr = env->GetStringUTFChars(name, NULL);

    jint result = -1;
    struct stat st;
    if (stat(nameStr, &st) == -1) {
        throwIOException(env, "Failed to stat %s: %s", nameStr, strerror(errno));
        goto ret;
    }

    if (!S_ISCHR(st.st_mode)) {
        throwIOException(env, "%s is not a character device: mode is 0%o", nameStr, st.st_mode);
        goto ret;
    }

    result = major(st.st_rdev);

ret:
    if (nameStr != NULL) {
        env->ReleaseStringUTFChars(name, nameStr);
    }
    return result;
}

jint android_security_cts_LinuxRngTest_getCharDeviceMinor(JNIEnv* env,
        jobject thiz, jstring name)
{
    const char* nameStr = env->GetStringUTFChars(name, NULL);

    jint result = -1;
    struct stat st;
    if (stat(nameStr, &st) == -1) {
        throwIOException(env, "Failed to stat %s: %s", nameStr, strerror(errno));
        goto ret;
    }

    if (!S_ISCHR(st.st_mode)) {
        throwIOException(env, "%s is not a character device: mode is 0%o", nameStr, st.st_mode);
        goto ret;
    }

    result = minor(st.st_rdev);

ret:
    if (nameStr != NULL) {
        env->ReleaseStringUTFChars(name, nameStr);
    }
    return result;
}

static JNINativeMethod gMethods[] = {
    {  "getCharDeviceMajor", "(Ljava/lang/String;)I",
            (void *) android_security_cts_LinuxRngTest_getCharDeviceMajor },
    {  "getCharDeviceMinor", "(Ljava/lang/String;)I",
            (void *) android_security_cts_LinuxRngTest_getCharDeviceMinor },
};

int register_android_security_cts_LinuxRngTest(JNIEnv* env)
{
    jclass clazz = env->FindClass("android/security/cts/LinuxRngTest");
    return env->RegisterNatives(clazz, gMethods,
            sizeof(gMethods) / sizeof(JNINativeMethod));
}
