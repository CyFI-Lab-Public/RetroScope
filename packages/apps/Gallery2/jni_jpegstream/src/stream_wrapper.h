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

#ifndef STREAM_WRAPPER_H_
#define STREAM_WRAPPER_H_

#include "jni_defines.h"

#include <stdint.h>

class StreamWrapper {
public:
    StreamWrapper();
    virtual ~StreamWrapper();
    virtual void updateEnv(JNIEnv *env);
    virtual bool init(JNIEnv *env, jobject stream);
    virtual void cleanup();
    virtual int32_t getBufferSize();
    virtual jbyte* getBufferPtr();

    const static int32_t DEFAULT_BUFFER_SIZE;
    const static int32_t END_OF_STREAM;
protected:
    JNIEnv *mEnv;
    jobject mStream;
    jbyteArray mByteArray;
    jbyte* mBytes;
    int32_t mByteArrayLen;
};

#endif // STREAM_WRAPPER_H_
