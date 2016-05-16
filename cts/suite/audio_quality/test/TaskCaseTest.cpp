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

#include "Log.h"
#include "StringUtil.h"
#include "task/TaskAll.h"


class TaskCaseTest : public testing::Test {
public:
    TaskCase* mTaskCase;
    virtual void SetUp() {
        mTaskCase = new TaskCase();
        ASSERT_TRUE(mTaskCase != NULL);
    }

    virtual void TearDown() {
        delete mTaskCase;
    }
};


TEST_F(TaskCaseTest, DataMapTest) {
    android::sp<Buffer> buffer1(new Buffer(4, 4, true));
    android::sp<Buffer> buffer2(new Buffer(4, 4, true));
    android::sp<Buffer> buffer3(new Buffer(4, 4, true));
    android::sp<Buffer> buffer4(new Buffer(4, 4, true));

    const android::String8 BUFFER1("buffer1");
    const android::String8 BUFFER2("buffer2");
    const android::String8 BUFFER3("buffer3");
    const android::String8 BUFFER4("buffer4");
    ASSERT_TRUE(mTaskCase->registerBuffer(BUFFER1, buffer1));
    ASSERT_TRUE(mTaskCase->registerBuffer(BUFFER2, buffer2));
    ASSERT_TRUE(mTaskCase->registerBuffer(BUFFER3, buffer3));
    ASSERT_TRUE(mTaskCase->registerBuffer(BUFFER4, buffer4));

    android::sp<Buffer> buffer1f = mTaskCase->findBuffer(BUFFER1);
    //LOGI("buffer1 %x, buffer1f %x", &buffer1, buffer1f);
    ASSERT_TRUE(buffer1.get() == buffer1f.get());
    const android::String8 NO_SUCH_BUFFER("no_such_buffer");
    buffer1f = mTaskCase->findBuffer(NO_SUCH_BUFFER);
    ASSERT_TRUE(buffer1f.get() == NULL);
    const android::String8 RE("buffer[1-2]");
    std::list<TaskCase::BufferPair>* list = mTaskCase->findAllBuffers(RE);
    ASSERT_TRUE(list != NULL);
    ASSERT_TRUE(((list->front().second.get() == buffer1.get()) &&
                    (list->back().second.get() == buffer2.get())) ||
                ((list->front().second.get() == buffer2.get()) &&
                    (list->back().second.get() == buffer1.get())));
    delete list;
}

TEST_F(TaskCaseTest, ValueMapTest) {
    TaskCase::Value val1(1.0f);
    TaskCase::Value val2(2.0f);
    TaskCase::Value val3((int64_t)1);
    TaskCase::Value val4((int64_t)2);
    TaskCase::Value val2_copy(2.0f);
    ASSERT_TRUE(!(val1 == val2));
    ASSERT_TRUE(!(val2 == val3));
    ASSERT_TRUE(val2 == val2_copy);
    ASSERT_TRUE(val1.getDouble() == 1.0f);
    ASSERT_TRUE(val3.getInt64() == 1);
    const android::String8 V1("v1");
    const android::String8 V2("v2");
    const android::String8 V3("v3");
    const android::String8 V4("v4");
    const android::String8 V5("v5");
    ASSERT_TRUE(mTaskCase->registerValue(V1, val1));
    ASSERT_TRUE(mTaskCase->registerValue(V2, val2));
    ASSERT_TRUE(mTaskCase->registerValue(V3, val3));
    ASSERT_TRUE(mTaskCase->registerValue(V4, val4));

    TaskCase::Value valRead;
    ASSERT_TRUE(mTaskCase->findValue(V4, valRead));
    ASSERT_TRUE(valRead.getInt64() == 2);
    TaskCase::Value val4_2((int64_t)3);
    ASSERT_TRUE(mTaskCase->updateValue(V4, val4_2));
    ASSERT_TRUE(mTaskCase->findValue(V4, valRead));
    ASSERT_TRUE(valRead.getInt64() == 3);
    ASSERT_TRUE(!mTaskCase->updateValue(V5, val4));
    ASSERT_TRUE(!mTaskCase->findValue(V5, valRead));

    const android::String8 RE("v[2-3]");
    std::list<TaskCase::ValuePair>* list = mTaskCase->findAllValues(RE);
    ASSERT_TRUE(list != NULL);
    ASSERT_TRUE(((list->front().second == val2) && (list->back().second == val3)) ||
                ((list->front().second == val3) && (list->back().second == val4)));
    delete list;
}

TEST_F(TaskCaseTest, IndexMapTest) {
    Buffer buffer1(4, 4, true);
    Buffer buffer2(4, 4, true);
    Buffer buffer3(4, 4, true);
    Buffer buffer4(4, 4, true);

    int i = 0;
    int j = 1;
    const android::String8 I("i");
    const android::String8 J("j");
    const android::String8 K("k");
    ASSERT_TRUE(mTaskCase->registerIndex(I));
    ASSERT_TRUE(mTaskCase->registerIndex(J));
    ASSERT_TRUE(mTaskCase->updateIndex(I, i));
    ASSERT_TRUE(mTaskCase->updateIndex(J, j));
    int i_read, j_read, k_read;
    ASSERT_TRUE(mTaskCase->findIndex(I, i_read));
    ASSERT_TRUE(mTaskCase->findIndex(J, j_read));
    ASSERT_TRUE(!mTaskCase->findIndex(K, k_read));
    ASSERT_TRUE(i == i_read);
    ASSERT_TRUE(j == j_read);
    //TODO add findAll test
}

TEST_F(TaskCaseTest, VarTranslateTest) {
    const android::String8 I("i");
    const android::String8 J("j");
    const android::String8 K("k");
    ASSERT_TRUE(mTaskCase->registerIndex(I, 1));
    ASSERT_TRUE(mTaskCase->registerIndex(J, 2));
    ASSERT_TRUE(mTaskCase->registerIndex(K, 3));

    android::String8 orig1("hello_$i_$j");
    android::String8 result1;
    ASSERT_TRUE(mTaskCase->translateVarName(orig1, result1));
    ASSERT_TRUE(StringUtil::compare(result1, "hello_1_2") == 0);

    android::String8 orig2("hello_$i_$j_$k_there");
    android::String8 result2;
    ASSERT_TRUE(mTaskCase->translateVarName(orig2, result2));
    ASSERT_TRUE(StringUtil::compare(result2, "hello_1_2_3_there") == 0);

    // should fail as there is no such var
    android::String8 orig3("$n");
    android::String8 result3;
    ASSERT_TRUE(!mTaskCase->translateVarName(orig3, result3));

    android::String8 orig4("hello_there");
    android::String8 result4;
    ASSERT_TRUE(mTaskCase->translateVarName(orig4, result4));
    ASSERT_TRUE(StringUtil::compare(result4, "hello_there") == 0);
}
