/*
 * Copyright (C) 2012 The Android Open Source Project
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

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>

double currentTimeMillis()
{
    struct timeval tv;
    gettimeofday(&tv, (struct timezone *) NULL);
    return tv.tv_sec * 1000.0 + tv.tv_usec / 1000.0;
}

extern "C" JNIEXPORT jdouble JNICALL Java_com_android_cts_dram_MemoryNative_runMemcpy(JNIEnv* env,
        jclass clazz, jint bufferSize, jint repetition)
{
    char* src = new char[bufferSize];
    char* dst = new char[bufferSize];
    if ((src == NULL) || (dst == NULL)) {
        delete[] src;
        delete[] dst;
        env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"), "No memory");
        return -1;
    }
    memset(src, 0, bufferSize);
    memset(dst, 0, bufferSize);
    double start = currentTimeMillis();
    for (int i = 0; i < repetition; i++) {
        memcpy(dst, src, bufferSize);
        src[bufferSize - 1] = i & 0xff;
    }
    double end = currentTimeMillis();
    delete[] src;
    delete[] dst;
    return end - start;
}

extern "C" JNIEXPORT jdouble JNICALL Java_com_android_cts_dram_MemoryNative_runMemset(JNIEnv* env,
        jclass clazz, jint bufferSize, jint repetition, jint c)
{
    char* dst = new char[bufferSize];
    if (dst == NULL) {
        delete[] dst;
        env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"), "No memory");
        return -1;
    }
    memset(dst, 0, bufferSize);
    double start = currentTimeMillis();
    for (int i = 0; i < repetition; i++) {
        memset(dst, (c + i) & 0xff, bufferSize);
    }
    double end = currentTimeMillis();
    delete[] dst;
    return end - start;
}

