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

#include "inputstream_wrapper.h"
#include "error_codes.h"

jmethodID InputStreamWrapper::sReadID = NULL;
jmethodID InputStreamWrapper::sSkipID = NULL;

int32_t InputStreamWrapper::read(int32_t length, int32_t offset) {
    if (offset < 0 || length < 0 || (offset + length) > getBufferSize()) {
        return J_ERROR_BAD_ARGS;
    }
    int32_t bytesRead = 0;
    mEnv->ReleaseByteArrayElements(mByteArray, mBytes, JNI_COMMIT);
    mBytes = NULL;
    if (mEnv->ExceptionCheck()) {
        return J_EXCEPTION;
    }
    bytesRead = static_cast<int32_t>(mEnv->CallIntMethod(mStream, sReadID,
            mByteArray, offset, length));
    if (mEnv->ExceptionCheck()) {
        return J_EXCEPTION;
    }
    mBytes = mEnv->GetByteArrayElements(mByteArray, NULL);
    if (mBytes == NULL || mEnv->ExceptionCheck()) {
        return J_EXCEPTION;
    }
    if (bytesRead == END_OF_STREAM) {
        return J_DONE;
    }
    return bytesRead;
}

int64_t InputStreamWrapper::skip(int64_t count) {
    int64_t bytesSkipped = 0;
    bytesSkipped = static_cast<int64_t>(mEnv->CallLongMethod(mStream, sSkipID,
            static_cast<jlong>(count)));
    if (mEnv->ExceptionCheck() || bytesSkipped < 0) {
        return J_EXCEPTION;
    }
    return bytesSkipped;
}

// Acts like a read call that returns the End Of Image marker for a JPEG file.
int32_t InputStreamWrapper::forceReadEOI() {
    mBytes[0] = (jbyte) 0xFF;
    mBytes[1] = (jbyte) 0xD9;
    return 2;
}

void InputStreamWrapper::setReadSkipMethodIDs(jmethodID readID,
        jmethodID skipID) {
    sReadID = readID;
    sSkipID = skipID;
}
