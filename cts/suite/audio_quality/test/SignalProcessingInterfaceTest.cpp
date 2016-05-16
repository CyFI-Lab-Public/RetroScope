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

#include <gtest/gtest.h>
#include <utils/String8.h>

#include <audio/AudioSignalFactory.h>
#include <SignalProcessingInterface.h>
#include <SignalProcessingImpl.h>
#include <task/TaskAll.h>

class SignalProcessingInterfaceTest : public testing::Test {
protected:
    SignalProcessingImpl* mSp;

protected:
    virtual void SetUp() {
        mSp = new SignalProcessingImpl();
        ASSERT_TRUE(mSp != NULL);
        ASSERT_TRUE(mSp->init(SignalProcessingImpl::MAIN_PROCESSING_SCRIPT));
    }

    virtual void TearDown() {
        delete mSp;
        mSp = NULL;
    }
};

TEST_F(SignalProcessingInterfaceTest, InitTest) {
    // SetUp do all the work, nothing to do
}

TEST_F(SignalProcessingInterfaceTest, EchoTest) {
    android::String8 functionName("echo");
    int nInputs = 4;
    int nOutputs = 4;
    bool inputTypes[4] = { true, true, false, false };
    bool outputTypes[4] = { true, true, false, false };

    android::sp<Buffer> in0(new Buffer(160000, 160000, true));
    char* data0 = in0->getData();
    for (size_t i = 0; i < in0->getSize(); i++) {
        data0[i] = i;
    }
    android::sp<Buffer> in1(new Buffer(8, 8, false));
    char* data1 = in1->getData();
    for (size_t i = 0; i < in1->getSize(); i++) {
        data1[i] = i;
    }
    TaskCase::Value in2(1.0f);
    TaskCase::Value in3((int64_t)100);
    void* inputs[4] = { &in0, &in1, &in2, &in3 };

    android::sp<Buffer> out0(new Buffer(160000, 160000, true));
    char* outdata0 = out0->getData();
    for (size_t i = 0; i < out0->getSize(); i++) {
        outdata0[i] = 0xaa;
    }
    android::sp<Buffer> out1(new Buffer(8, 8, false));
    char* outdata1 = out1->getData();
    for (size_t i = 0; i < out1->getSize(); i++) {
        outdata1[i] = 0xbb;
    }
    TaskCase::Value out2(-1.0f);
    TaskCase::Value out3((int64_t)1000);
    void *outputs[4] = { &out0, &out1, &out2, &out3 };

    ASSERT_TRUE(mSp->run( functionName,
            nInputs, inputTypes, inputs,
            nOutputs, outputTypes, outputs) == TaskGeneric::EResultOK);
    ASSERT_TRUE(*(in0.get()) == *(out0.get()));
    ASSERT_TRUE(*(in1.get()) == *(out1.get()));
    ASSERT_TRUE(in2 == out2);
    ASSERT_TRUE(in3 == out3);
}

TEST_F(SignalProcessingInterfaceTest, intsumTest) {
    android::String8 functionName("intsum");
    int nInputs = 2;
    int nOutputs = 1;
    bool inputTypes[2] = { false, false };
    bool outputTypes[1] = { false };

    TaskCase::Value in0((int64_t)10);
    TaskCase::Value in1((int64_t)100);
    void* inputs[2] = { &in0, &in1 };

    TaskCase::Value out0((int64_t)0);
    void *outputs[1] = { &out0 };

    ASSERT_TRUE(mSp->run( functionName,
            nInputs, inputTypes, inputs,
            nOutputs, outputTypes, outputs) == TaskGeneric::EResultOK);
    ASSERT_TRUE(out0.getInt64() == (in0.getInt64() + in1.getInt64()));
}

// two instances of python processing processes should work
TEST_F(SignalProcessingInterfaceTest, TwoInstanceTest) {
    SignalProcessingImpl* sp2 = new SignalProcessingImpl();
    ASSERT_TRUE(sp2 != NULL);
    ASSERT_TRUE(sp2->init(SignalProcessingImpl::MAIN_PROCESSING_SCRIPT));

    android::String8 functionName("intsum");
    int nInputs = 2;
    int nOutputs = 1;
    bool inputTypes[2] = { false, false };
    bool outputTypes[1] = { false };

    TaskCase::Value in0((int64_t)10);
    TaskCase::Value in1((int64_t)100);
    void* inputs[2] = { &in0, &in1 };

    TaskCase::Value out0((int64_t)0);
    void *outputs[1] = { &out0 };

    ASSERT_TRUE(mSp->run( functionName,
            nInputs, inputTypes, inputs,
            nOutputs, outputTypes, outputs) == TaskGeneric::EResultOK);
    ASSERT_TRUE(out0.getInt64() == (in0.getInt64() + in1.getInt64()));
    out0.setInt64(0);
    ASSERT_TRUE(sp2->run( functionName,
                nInputs, inputTypes, inputs,
                nOutputs, outputTypes, outputs) == TaskGeneric::EResultOK);
    ASSERT_TRUE(out0.getInt64() == (in0.getInt64() + in1.getInt64()));
    delete sp2;
}

// test to run processing/example.py
TEST_F(SignalProcessingInterfaceTest, exampleTest) {
    android::String8 functionName("example");
    int nInputs = 8;
    int nOutputs = 4;
    bool inputTypes[8] = { true, true, true, true, false, false, false, false };
    bool outputTypes[4] = { true, true, false, false };

    android::sp<Buffer> in0(new Buffer(16, 16, true));
    char* data0 = in0->getData();
    for (size_t i = 0; i < in0->getSize(); i++) {
        data0[i] = i;
    }
    android::sp<Buffer> in1(new Buffer(16, 16, true));
    char* data1 = in1->getData();
    for (size_t i = 0; i < in1->getSize(); i++) {
        data1[i] = i;
    }
    android::sp<Buffer> in2(new Buffer(8, 8, false));
    char* data2 = in2->getData();
    for (size_t i = 0; i < in2->getSize(); i++) {
        data2[i] = i;
    }
    android::sp<Buffer> in3(new Buffer(8, 8, false));
    char* data3 = in3->getData();
    for (size_t i = 0; i < in3->getSize(); i++) {
        data3[i] = i;
    }
    TaskCase::Value in4((int64_t)100);
    TaskCase::Value in5((int64_t)100);
    TaskCase::Value in6(1.0f);
    TaskCase::Value in7(1.0f);
    void* inputs[8] = { &in0, &in1, &in2, &in3, &in4, &in5, &in6, &in7 };

    android::sp<Buffer> out0(new Buffer(16, 16, true));
    char* outdata0 = out0->getData();
    for (size_t i = 0; i < out0->getSize(); i++) {
        outdata0[i] = 0xaa;
    }
    android::sp<Buffer> out1(new Buffer(8, 8, false));
    char* outdata1 = out1->getData();
    for (size_t i = 0; i < out1->getSize(); i++) {
        outdata1[i] = 0xbb;
    }
    TaskCase::Value out2((int64_t)1000);
    TaskCase::Value out3(-1.0f);
    void *outputs[4] = { &out0, &out1, &out2, &out3 };

    ASSERT_TRUE(mSp->run( functionName,
            nInputs, inputTypes, inputs,
            nOutputs, outputTypes, outputs) == TaskGeneric::EResultOK);
    ASSERT_TRUE(*(in0.get()) == *(out0.get()));
    ASSERT_TRUE(*(in2.get()) == *(out1.get()));
    ASSERT_TRUE(in4 == out2);
    ASSERT_TRUE(in6 == out3);
}
