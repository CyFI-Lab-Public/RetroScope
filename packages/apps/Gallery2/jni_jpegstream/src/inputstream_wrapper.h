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

#ifndef INPUTSTREAM_WRAPPER_H_
#define INPUTSTREAM_WRAPPER_H_

#include "jni_defines.h"
#include "stream_wrapper.h"

#include <stdint.h>

class InputStreamWrapper : public StreamWrapper {
public:
    virtual int32_t read(int32_t length, int32_t offset);
    virtual int64_t skip(int64_t count);
    virtual int32_t forceReadEOI();

    // Call this in JNI_OnLoad to cache read/skip method IDs
    static void setReadSkipMethodIDs(jmethodID readID, jmethodID skipID);
protected:
    static jmethodID sReadID;
    static jmethodID sSkipID;
};

#endif // INPUTSTREAM_WRAPPER_H_
