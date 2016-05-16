/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

#ifndef CTSAUDIO_BUFFER_H
#define CTSAUDIO_BUFFER_H

#include <stdlib.h>
#include <string.h>
#include <utils/String8.h>

#include <utils/RefBase.h>

#include <Log.h>

/**
 * Buffer passed for audio playback and recording
 * The buffer is supposed to be used with sp to guarantee that audio thread can
 * access it even if the client thread is dead.
 */
class Buffer: public virtual android::RefBase {
public:
    Buffer(size_t capacity, size_t size = 0, bool stereo = true);

    virtual ~Buffer();

    inline size_t getCapacity() {
        return mCapacity;
    };

    inline size_t getSize() {
        return mSize;
    };

    inline size_t getSamples() {
        return (getSize() / (isStereo() ? 4 : 2));
    };

    inline void setSize(size_t size) {
        mSize = size;
    };

    inline void increaseSize(size_t size) {
        mSize += size;
    }
    inline char* getData() {
        return mData;
    };

    inline void setData(char* data, size_t len) {
        ASSERT(len <= mCapacity);
        memcpy(mData, data, len);
        mSize = len;
    };

    inline char* getUnhanledData() {
        return mData + mHandled;
    };

    inline bool bufferHandled() {
        return mSize <= mHandled;
    };

    inline void restart() {
        mHandled = 0;
    };
    /// size was recorded
    inline void increaseHandled(size_t size) {
        mHandled += size;
    };

    inline void setHandled(size_t size) {
        mHandled = size;
    }
    /// amount recorded
    inline size_t amountHandled() {
        return mHandled;
    };

    inline size_t amountToHandle() {
        return mSize - mHandled;
    };

    inline bool isStereo() {
        return mStereo;
    };
    enum ConvertOption {
        EKeepCh0 = 0,
        EKeepCh1 = 1,
        EAverage = 2
    };
    /// change stereo buffer to mono
    void changeToMono(ConvertOption option);
    /// change mono buffer to stereo. This does not increase allocated memory.
    /// So it will fail if capacity is not big enough.
    bool changeToStereo();

    /// save the buffer to file
    /// extension appropriate for the data type will be appended to file name
    bool saveToFile(const android::String8& filename);

    bool operator ==(const Buffer& b) const;

    /// load raw data from given file.
    /// data format is decided by extension
    /// .r2s: 16 bps, stereo
    /// .r2m: 16bps, mono
    static Buffer* loadFromFile(const android::String8& filename);
private:
    // max data that can be hold
    size_t mCapacity;
    // data stored for playback / to store for recording
    size_t mSize;
    // how much data was handled / recorded
    size_t mHandled;
    // stereo or mono
    bool mStereo;
    // payload
    char* mData;
};



#endif // CTSAUDIO_BUFFER_H
