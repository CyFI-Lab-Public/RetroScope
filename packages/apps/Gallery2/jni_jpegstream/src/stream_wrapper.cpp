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

#include "stream_wrapper.h"

const int32_t StreamWrapper::END_OF_STREAM = -1;
const int32_t StreamWrapper::DEFAULT_BUFFER_SIZE = 1 << 16;  // 64Kb

StreamWrapper::StreamWrapper() : mEnv(NULL),
                                 mStream(NULL),
                                 mByteArray(NULL),
                                 mBytes(NULL),
                                 mByteArrayLen(0) {}

StreamWrapper::~StreamWrapper() {
    cleanup();
}

void StreamWrapper::updateEnv(JNIEnv *env) {
    if (env == NULL) {
        LOGE("Cannot update StreamWrapper with a null JNIEnv pointer!");
        return;
    }
    mEnv = env;
}

bool StreamWrapper::init(JNIEnv *env, jobject stream) {
    if (mEnv != NULL) {
        LOGW("StreamWrapper already initialized!");
        return false;
    }
    mEnv = env;
    mStream = env->NewGlobalRef(stream);
    if (mStream == NULL || env->ExceptionCheck()) {
        cleanup();
        return false;
    }
    mByteArrayLen = DEFAULT_BUFFER_SIZE;
    jbyteArray tmp = env->NewByteArray(getBufferSize());
    if (tmp == NULL || env->ExceptionCheck()){
        cleanup();
        return false;
    }
    mByteArray = reinterpret_cast<jbyteArray>(env->NewGlobalRef(tmp));
    if (mByteArray == NULL || env->ExceptionCheck()){
        cleanup();
        return false;
    }
    mBytes = env->GetByteArrayElements(mByteArray, NULL);
    if (mBytes == NULL || env->ExceptionCheck()){
        cleanup();
        return false;
    }
    return true;
}

void StreamWrapper::cleanup() {
    if (mEnv != NULL) {
        if (mStream != NULL) {
            mEnv->DeleteGlobalRef(mStream);
            mStream = NULL;
        }
        if (mByteArray != NULL) {
            if (mBytes != NULL) {
                mEnv->ReleaseByteArrayElements(mByteArray, mBytes, JNI_ABORT);
                mBytes = NULL;
            }
            mEnv->DeleteGlobalRef(mByteArray);
            mByteArray = NULL;
        } else {
            mBytes = NULL;
        }
        mByteArrayLen = 0;
        mEnv = NULL;
    }
}

int32_t StreamWrapper::getBufferSize() {
    return mByteArrayLen;
}

jbyte* StreamWrapper::getBufferPtr() {
    return mBytes;
}
