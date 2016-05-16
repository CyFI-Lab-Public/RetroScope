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

#ifndef OUTPUTSTREAM_WRAPPER_H_
#define OUTPUTSTREAM_WRAPPER_H_

#include "jni_defines.h"
#include "stream_wrapper.h"

#include <stdint.h>

class OutputStreamWrapper : public StreamWrapper {
public:
    virtual int32_t write(int32_t length, int32_t offset);

    // Call this in JNI_OnLoad to cache write method
    static void setWriteMethodID(jmethodID id);
protected:
    static jmethodID sWriteID;
};

#endif // OUTPUTSTREAM_WRAPPER_H_
