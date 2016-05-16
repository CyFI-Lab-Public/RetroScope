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
#include <gtest/gtest.h>

#include <UniquePtr.h>

#include "audio/Buffer.h"


class BufferTest : public testing::Test {
public:

    virtual void SetUp() {

    }

    virtual void TearDown() {

    }
};


TEST_F(BufferTest, saveLoadStereoTest) {
    const int BUFFER_SIZE = 32;

    UniquePtr<Buffer> buffer(new Buffer(BUFFER_SIZE, BUFFER_SIZE, true));
    ASSERT_TRUE(buffer.get() != NULL);
    int16_t* data = (int16_t*)buffer->getData();
    ASSERT_TRUE(data != NULL);
    for (int i = 0; i < BUFFER_SIZE/4; i++) {
        data[2*i] = i;
        data[2*i+1] = i;
    }
    android::String8 file("/tmp/cts_audio_temp");
    ASSERT_TRUE(buffer->saveToFile(file));
    file.append(".r2s");
    UniquePtr<Buffer> bufferRead(Buffer::loadFromFile(file));
    ASSERT_TRUE(bufferRead.get() != NULL);
    ASSERT_TRUE(bufferRead->getSize() == (size_t)BUFFER_SIZE);
    ASSERT_TRUE(bufferRead->isStereo());
    int16_t* dataRead = (int16_t*)bufferRead->getData();
    for (int i = 0; i < BUFFER_SIZE/4; i++) {
        ASSERT_TRUE(data[2*i] == dataRead[2*i]);
        ASSERT_TRUE(data[2*i+1] == dataRead[2*i+1]);
    }
}

TEST_F(BufferTest, monoLTest) {
    const int BUFFER_SIZE = 8;

    UniquePtr<Buffer> buffer(new Buffer(BUFFER_SIZE, BUFFER_SIZE, true));
    ASSERT_TRUE(buffer.get() != NULL);
    int16_t* data = (int16_t*)buffer->getData();
    ASSERT_TRUE(data != NULL);
    for (int i = 0; i < BUFFER_SIZE/2; i++) {
        data[i] = i;
    }
    UniquePtr<Buffer> bufferl(new Buffer(BUFFER_SIZE/2, BUFFER_SIZE/2, false));
    ASSERT_TRUE(bufferl.get() != NULL);
    data = (int16_t*)bufferl->getData();
    ASSERT_TRUE(data != NULL);
    for (int i = 0; i < BUFFER_SIZE/4; i++) {
        data[i] = 2 * i;
    }
    buffer->changeToMono(Buffer::EKeepCh0);
    ASSERT_TRUE((*buffer) == (*bufferl));
}
