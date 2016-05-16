/*
 * Copyright (C) 2011 The Android Open Source Project
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

/**
 * Test for testing the creation of OpenMAX AL objects.
 * The tests verify the creation and completion of the call to Realize() for the following objects:
 *   - Engine
 *   - OutputMix
 */

#define LOG_NDEBUG 0
#define LOG_TAG "XAObjectCreationTest"

#include <utils/Log.h>
#include "OMXAL/OpenMAXAL.h"
#include "OMXAL/OpenMAXAL_Android.h"
//#include <android/native_window_jni.h>
#include <gtest/gtest.h>

//-----------------------------------------------------------------
/* Checks for error and displays the error code if any */
bool IsOk(XAresult res) {
    if (XA_RESULT_SUCCESS != res) {
        fprintf(stderr, "IsOk failure: 0x%x, exiting\n", res);
        return false;
    }
    return true;
}

//-----------------------------------------------------------------
class XAObjectCreationTest : public ::testing::Test {

protected:
    XAresult res;
    XAObjectItf engineObj, outputMixObj, mediaPlayerObj;
    XAEngineItf engineItf;

    XADataSource mediaSource;
    XADataSink   audioSink;
    XADataLocator_URI locatorUriSrc;
    XADataLocator_AndroidBufferQueue locatorAbqSrc;
    XADataLocator_AndroidFD locatorFdSrc;
    XADataFormat_MIME formatMimeSrc;

    XADataLocator_OutputMix locatorOutputmixSink;
    XADataFormat_PCM formatPcmSink;

    XADataLocator_NativeDisplay locatorVideoSink;
    XADataSink imageSink;

    //ANativeWindow* pNativeWindow;

    XAObjectCreationTest() { }

    virtual ~XAObjectCreationTest() { }

    /* Test setup*/
    virtual void SetUp() {
        ALOGV("Test Setup()");
        res = XA_RESULT_UNKNOWN_ERROR;
        engineItf = NULL;
        engineObj = NULL;
        outputMixObj = NULL;
        mediaPlayerObj = NULL;
        // Engine creation
        res = xaCreateEngine(&engineObj, 0, NULL, 0, NULL, NULL);
        ASSERT_TRUE(IsOk(res));
        res = (*engineObj)->Realize(engineObj, XA_BOOLEAN_FALSE);
        ASSERT_TRUE(IsOk(res));
        res = (*engineObj)->GetInterface(engineObj, XA_IID_ENGINE, &engineItf);
        ASSERT_TRUE(IsOk(res));
        ASSERT_TRUE(NULL != engineItf);
    }

    virtual void TearDown() {
        ALOGV("Test TearDown()");
        if (mediaPlayerObj) {
            (*mediaPlayerObj)->Destroy(mediaPlayerObj);
            mediaPlayerObj = NULL;
        }
        if (outputMixObj) {
            (*outputMixObj)->Destroy(outputMixObj);
            outputMixObj = NULL;
        }
        if (engineObj){
            (*engineObj)->Destroy(engineObj);
            engineObj = NULL;
        }
    }

    //---------------------------------------------------------------------------------------------
    // Tests

    /* Test case for creating an MediaPlayer object */
    void OutputMixCreation() {
        res = (*engineItf)->CreateOutputMix(engineItf, &outputMixObj,
                0, NULL/*iidArray*/, NULL/*required*/);
        ASSERT_TRUE(IsOk(res));
        ASSERT_TRUE(NULL != outputMixObj);
        res = (*outputMixObj)->Realize(outputMixObj, XA_BOOLEAN_FALSE);
        ASSERT_TRUE(IsOk(res));
    }

};

//-------------------------------------------------------------------------------------------------
TEST_F(XAObjectCreationTest, testEngineCreation) {
    ALOGV("Test Fixture: EngineCreation");
    // nothing to do here that isn't done in SetUp()
}

TEST_F(XAObjectCreationTest, testOutputMixCreation) {
    ALOGV("Test Fixture: OutputMixCreation");
    OutputMixCreation();
}

int main(int argc, char **argv) {
    testing::InitGoogleTest(&argc, argv);

    return RUN_ALL_TESTS();
}

