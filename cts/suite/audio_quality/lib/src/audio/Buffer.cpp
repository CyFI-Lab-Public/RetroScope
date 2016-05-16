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
#include <stdint.h>
#include <iostream>
#include <fstream>
#include <utils/String8.h>
#include "Log.h"
#include "StringUtil.h"
#include "audio/Buffer.h"

Buffer::Buffer(size_t capacity, size_t size, bool stereo)
    : mCapacity(capacity),
      mSize(size),
      mHandled(0),
      mStereo(stereo)
{
    mData = new char[capacity];
    //LOGV("Buffer %d data %x", capacity, (unsigned int)mData);
    // assume 4bytes alignment
    ASSERT(((long)mData & 0x3) == 0);
    // filling with zero just to make valgrind happy.
    // Otherwise, valgrind will complain about uninitialized data for all captured data
    memset(mData, capacity, 0);
};

Buffer::~Buffer()
{
    delete[] mData;
    //LOGV("~Buffer %d", mCapacity);
}

void Buffer::changeToMono(ConvertOption option)
{
    size_t newSize = mSize/2;
    int16_t* data = reinterpret_cast<int16_t*>(mData);
    if (option == EKeepCh0) {
        for (size_t i = 0; i < newSize/2; i++) { //16bpp only
            int16_t l = data[i * 2];
            data[i] = l;
        }
    } else if (option == EKeepCh1) {
        for (size_t i = 0; i < newSize/2; i++) { //16bpp only
            int16_t r = data[i * 2 + 1];
            data[i] = r;
        }
    } else { // average
        for (size_t i = 0; i < newSize/2; i++) { //16bpp only
            int16_t l = data[i * 2];
            int16_t r = data[i * 2 + 1];
            int16_t avr = (int16_t)(((int32_t)l + (int32_t)r)/2);
            data[i] = avr;
        }
    }
    mSize = newSize;
    mHandled /= 2;
    mStereo = false;
}

bool Buffer::changeToStereo()
{
    //TODO ChangeToStereo
    return false;
}

const char* EXTENSION_S16_STEREO = ".r2s";
const char* EXTENSION_S16_MONO = ".r2m";
Buffer* Buffer::loadFromFile(const android::String8& filename)
{
    bool stereo;
    if (StringUtil::endsWith(filename, EXTENSION_S16_STEREO)) {
        stereo = true;
    } else if (StringUtil::endsWith(filename, EXTENSION_S16_MONO)) {
        stereo = false;
    } else {
        LOGE("Buffer::loadFromFile specified file %s has unknown extension.", filename.string());
        return NULL;
    }
    std::ifstream file(filename.string(),  std::ios::in | std::ios::binary |
            std::ios::ate);
    if (!file.is_open()) {
        LOGE("Buffer::loadFromFile cannot open file %s.", filename.string());
        return NULL;
    }
    size_t size = file.tellg();
    Buffer* buffer = new Buffer(size, size, stereo);
    if (buffer == NULL) {
        return NULL;
    }
    file.seekg(0, std::ios::beg);
    file.read(buffer->mData, size); //TODO handle read error
    file.close();
    return buffer;
}

bool Buffer::saveToFile(const android::String8& filename)
{
    android::String8 filenameWithExtension(filename);
    if (isStereo()) {
        filenameWithExtension.append(EXTENSION_S16_STEREO);
    } else {
        filenameWithExtension.append(EXTENSION_S16_MONO);
    }
    std::ofstream file(filenameWithExtension.string(),  std::ios::out | std::ios::binary |
            std::ios::trunc);
    if (!file.is_open()) {
        LOGE("Buffer::saveToFile cannot create file %s.",
                filenameWithExtension.string());
        return false;
    }
    file.write(mData, mSize);
    bool writeOK = true;
    if (file.rdstate() != std::ios_base::goodbit) {
        LOGE("Got error while writing file %s %x", filenameWithExtension.string(), file.rdstate());
        writeOK = false;
    }
    file.close();
    return writeOK;
}

bool Buffer::operator == (const Buffer& b) const
{
    if (mStereo != b.mStereo) {
        LOGD("stereo mismatch %d %d", mStereo, b.mStereo);
        return false;
    }
    if (mSize != b.mSize) {
        LOGD("size mismatch %d %d", mSize, b.mSize);
        return false;
    }
    for (size_t i = 0; i < mSize; i++) {
        if (mData[i] != b.mData[i]) {
            LOGD("%d %x vs %x", i, mData[i], b.mData[i]);
            return false;
        }
    }
    return true;
}
