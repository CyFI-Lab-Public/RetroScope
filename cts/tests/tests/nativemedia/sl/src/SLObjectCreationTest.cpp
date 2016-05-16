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
 * Test for testing the creation of OpenSL ES objects under various configurations (determined
 * by their data source and sink types) that are expected to be supported.
 * The tests verify the creation and completion of the call to Realize() for the following objects:
 *   - Engine
 *   - OutputMix
 *   - AudioPlayer:
 *       * source is URI
 *       * source is FD
 *       * source is BufferQueue of PCM buffers
 *       * source is AndroidBufferQueue of MP2TS buffers
 *       * source is URI, sink is BufferQueue of PCM buffers
 *       * source is FD, sink is BufferQueue of PCM buffers
 *       * source is AndroidBufferQueue of AAC ADTS buffers, sink is BufferQueue of PCM buffers
 *   - AudioRecorder
 *       * source is IO device, sink is BufferQueue of PCM buffers
 */

#define LOG_NDEBUG 0
#define LOG_TAG "SLObjectCreationTest"

#include <utils/Log.h>
#include "SLES/OpenSLES.h"
#include "SLES/OpenSLES_Android.h"
#include "OpenSLESUT.h"
#include <gtest/gtest.h>

//-----------------------------------------------------------------
/* Checks for error and displays the error code if any */
bool IsOk(SLresult res) {
    if (SL_RESULT_SUCCESS != res) {
        const char *str = slesutResultToString(res);
        if (NULL == str)
            str = "unknown";
        fprintf(stderr, "IsOk failure: %s (0x%x), exiting\n", str, res);
        return false;
    }
    return true;
}

//-----------------------------------------------------------------
class SLObjectCreationTest : public ::testing::Test {

protected:
    SLresult res;
    SLObjectItf engineObj, outputMixObj, audioPlayerObj;
    SLEngineItf engineItf;

    SLDataSource audioSource;
    SLDataSink   audioSink;
    SLDataLocator_URI locatorUriSrc;
    SLDataLocator_AndroidBufferQueue locatorAbqSrc;
    SLDataLocator_AndroidFD locatorFdSrc;
    SLDataFormat_MIME formatMimeSrc;

    SLDataLocator_OutputMix locatorOutputmixSnk;
    SLDataLocator_AndroidSimpleBufferQueue locatorBqSnk;
    SLDataFormat_PCM formatPcmSnk;

    SLObjectCreationTest() { }

    virtual ~SLObjectCreationTest() { }

    /* Test setup*/
    virtual void SetUp() {
        ALOGV("Test Setup()");
        res = SL_RESULT_UNKNOWN_ERROR;
        engineItf = NULL;
        engineObj = NULL;
        outputMixObj = NULL;
        audioPlayerObj = NULL;
        // Engine creation
        res = slCreateEngine(&engineObj, 0, NULL, 0, NULL, NULL);
        ASSERT_TRUE(IsOk(res));
        res = (*engineObj)->Realize(engineObj, SL_BOOLEAN_FALSE);
        ASSERT_TRUE(IsOk(res));
        res = (*engineObj)->GetInterface(engineObj, SL_IID_ENGINE, &engineItf);
        ASSERT_TRUE(IsOk(res));
        ASSERT_TRUE(NULL != engineItf);
    }

    virtual void TearDown() {
        ALOGV("Test TearDown()");
        if (audioPlayerObj) {
            (*audioPlayerObj)->Destroy(audioPlayerObj);
            audioPlayerObj = NULL;
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
    // Test implementation convenience methods (to avoid code duplication)

    void AudioPlayerCreation() {
        res = (*engineItf)->CreateAudioPlayer(engineItf, &audioPlayerObj,
                &audioSource, &audioSink, 0, NULL/*iidArray*/, NULL/*required*/);
        ASSERT_TRUE(IsOk(res));
        ASSERT_TRUE(NULL != audioPlayerObj);
        res = (*audioPlayerObj)->Realize(audioPlayerObj, SL_BOOLEAN_FALSE);
        ASSERT_TRUE(IsOk(res));
    }

    void OutputMixSinkInitialization() {
        locatorOutputmixSnk.locatorType = SL_DATALOCATOR_OUTPUTMIX;
        locatorOutputmixSnk.outputMix = outputMixObj; // created in OutputMixCreation()
        audioSink.pLocator = &locatorOutputmixSnk;
        audioSink.pFormat = NULL;
    }

    void UriSourceInitialization() {
        locatorUriSrc.locatorType = SL_DATALOCATOR_URI;
        locatorUriSrc.URI = (SLchar*) "/dummyPath/dummyFile.mp3";
        formatMimeSrc.formatType = SL_DATAFORMAT_MIME;
        formatMimeSrc.mimeType = (SLchar *) NULL;
        formatMimeSrc.containerType = SL_CONTAINERTYPE_UNSPECIFIED;
        audioSource.pLocator = &locatorUriSrc;
        audioSource.pFormat = &formatMimeSrc;
    }

    void FdSourceInitialization() {
        locatorFdSrc.locatorType = SL_DATALOCATOR_ANDROIDFD;
        locatorFdSrc.fd = (SLint32) 1;// a positive value to fake a valid FD
        locatorFdSrc.length = 10;
        locatorFdSrc.offset = 0;
        formatMimeSrc.formatType = SL_DATAFORMAT_MIME;
        formatMimeSrc.mimeType = (SLchar *) NULL;
        formatMimeSrc.containerType = SL_CONTAINERTYPE_UNSPECIFIED;
        audioSource.pLocator = &locatorFdSrc;
        audioSource.pFormat = &formatMimeSrc;
    }

    void PcmBqSinkInitialization() {
        locatorBqSnk.locatorType = SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE;
        locatorBqSnk.numBuffers = 16;
        formatPcmSnk.formatType = SL_DATAFORMAT_PCM;
        formatPcmSnk.numChannels = 1;
        formatPcmSnk.samplesPerSec = SL_SAMPLINGRATE_8;
        formatPcmSnk.bitsPerSample = SL_PCMSAMPLEFORMAT_FIXED_16;
        formatPcmSnk.containerSize = 16;
        formatPcmSnk.channelMask = SL_SPEAKER_FRONT_LEFT;
        formatPcmSnk.endianness = SL_BYTEORDER_LITTLEENDIAN;
        audioSink.pLocator = (void *) &locatorBqSnk;
        audioSink.pFormat  = (void *) &formatPcmSnk;
    }

    //---------------------------------------------------------------------------------------------
    // Tests

    /* Test case for creating an AudioPlayer object */
    void OutputMixCreation() {
        res = (*engineItf)->CreateOutputMix(engineItf, &outputMixObj,
                0, NULL/*iidArray*/, NULL/*required*/);
        ASSERT_TRUE(IsOk(res));
        ASSERT_TRUE(NULL != outputMixObj);
        res = (*outputMixObj)->Realize(outputMixObj, SL_BOOLEAN_FALSE);
        ASSERT_TRUE(IsOk(res));
    }

    /* Test case for creating an AudioPlayer object that plays from a URI */
    void AudioPlayerFromUriCreation() {
        // source: URI
        UriSourceInitialization();
        // sink: OutputMix
        OutputMixSinkInitialization();
        // AudioPlayer creation
        AudioPlayerCreation();
    }

    /* Test case for creating an AudioPlayer object that plays from a FD */
    void AudioPlayerFromFdCreation() {
        // source: FD
        FdSourceInitialization();
        // sink: OutputMix
        OutputMixSinkInitialization();
        // AudioPlayer creation
        AudioPlayerCreation();
    }

    /* Test case for creating an AudioPlayer object that plays from a PCM BufferQueue */
    void AudioPlayerFromPcmBqCreation() {
        // source: PCM BufferQueue
        SLDataLocator_BufferQueue locatorBufferQueue;
        locatorBufferQueue.locatorType = SL_DATALOCATOR_BUFFERQUEUE;
        locatorBufferQueue.numBuffers = 16;
        SLDataFormat_PCM pcm;
        pcm.formatType = SL_DATAFORMAT_PCM;
        pcm.numChannels = 2;
        pcm.samplesPerSec = SL_SAMPLINGRATE_44_1;
        pcm.bitsPerSample = SL_PCMSAMPLEFORMAT_FIXED_16;
        pcm.containerSize = 16;
        pcm.channelMask = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
        pcm.endianness  = SL_BYTEORDER_LITTLEENDIAN;
        audioSource.pLocator = &locatorBufferQueue;
        audioSource.pFormat = &pcm;

        // sink: OutputMix
        OutputMixSinkInitialization();
        // AudioPlayer creation
        AudioPlayerCreation();
    }

    /* Test case for creating an AudioPlayer object that plays from Transport Stream ABQ */
    void AudioPlayerFromTsAbqCreation() {
        // source: transport stream in an AndroidBufferQueue
        locatorAbqSrc.locatorType  = SL_DATALOCATOR_ANDROIDBUFFERQUEUE;
        locatorAbqSrc.numBuffers   = 16;
        formatMimeSrc.formatType    = SL_DATAFORMAT_MIME;
        formatMimeSrc.mimeType      = (SLchar *) "video/mp2ts";
        formatMimeSrc.containerType = SL_CONTAINERTYPE_MPEG_TS;
        audioSource.pFormat  = (void *)&formatMimeSrc;
        audioSource.pLocator = (void *)&locatorAbqSrc;

        // sink: OutputMix
        OutputMixSinkInitialization();
        // AudioPlayer creation
        AudioPlayerCreation();
    }

    /* Test case for creating an AudioPlayer object that decodes from a URI to a PCM BQ */
    void AudioPlayerFromUriToPcmBqCreation() {
        // source: URI
        UriSourceInitialization();
        // sink: PCM BufferQueue
        PcmBqSinkInitialization();
        // AudioPlayer creation
        AudioPlayerCreation();
    }

    /* Test case for creating an AudioPlayer object that decodes from a FD to a PCM BQ */
    void AudioPlayerFromFdToPcmBqCreation() {
        // source: FD
        FdSourceInitialization();
        // sink: PCM BufferQueue
        PcmBqSinkInitialization();
        // AudioPlayer creation
        AudioPlayerCreation();
    }

    /* Test case for creating an AudioPlayer object that decodes from a ADTS ABQ to a PCM BQ */
    void AudioPlayerFromAdtsAbqToPcmBqCreation() {
        // source: ADTS AndroidBufferQueue
        locatorAbqSrc.locatorType = SL_DATALOCATOR_ANDROIDBUFFERQUEUE;
        locatorAbqSrc.numBuffers  = 16;
        formatMimeSrc.formatType    =  SL_DATAFORMAT_MIME;
        formatMimeSrc.mimeType      = (SLchar *)"audio/aac-adts";
        formatMimeSrc.containerType = SL_CONTAINERTYPE_RAW;
        audioSource.pLocator = (void *) &locatorAbqSrc;
        audioSource.pFormat  = (void *) &formatMimeSrc;

        // sink: PCM BufferQueue
        PcmBqSinkInitialization();
        // AudioPlayer creation
        AudioPlayerCreation();
    }

    /* Test case for creating an AudioRecorder object */
    void AudioRecorderCreation(bool doNotRealize = false) {
        // source: IO device
        SLDataLocator_IODevice locatorIoDeviceSrc;
        locatorIoDeviceSrc.locatorType = SL_DATALOCATOR_IODEVICE;
        locatorIoDeviceSrc.deviceType = SL_IODEVICE_AUDIOINPUT;
        locatorIoDeviceSrc.deviceID = SL_DEFAULTDEVICEID_AUDIOINPUT;
        locatorIoDeviceSrc.device = NULL;
        audioSource.pLocator = (void *) &locatorIoDeviceSrc;
        audioSource.pFormat  = NULL;

        // sink: PCM BufferQueue
        PcmBqSinkInitialization();

        // AudioRecorder creation
        SLObjectItf audioRecorderObj = NULL;
        res = (*engineItf)->CreateAudioRecorder(engineItf, &audioRecorderObj,
                &audioSource, &audioSink, 0, NULL/*iidArray*/, NULL/*required*/);
        ASSERT_TRUE(IsOk(res));
        ASSERT_TRUE(NULL != audioRecorderObj);
        if (!doNotRealize) {
            res = (*audioRecorderObj)->Realize(audioRecorderObj, SL_BOOLEAN_FALSE);
            ASSERT_TRUE(IsOk(res));
        }

        // AudioRecorder destruction
        (*audioRecorderObj)->Destroy(audioRecorderObj);
    }
};

//-------------------------------------------------------------------------------------------------
TEST_F(SLObjectCreationTest, testEngineCreation) {
    ALOGV("Test Fixture: EngineCreation");
    // nothing to do here that isn't done in SetUp()
}

TEST_F(SLObjectCreationTest, testOutputMixCreation) {
    ALOGV("Test Fixture: OutputMixCreation");
    OutputMixCreation();
}

TEST_F(SLObjectCreationTest, testAudioPlayerFromUriCreation) {
    ALOGV("Test Fixture: AudioPlayerFromUriCreation");
    // required for AudioPlayer creation
    OutputMixCreation();
    AudioPlayerFromUriCreation();
}

TEST_F(SLObjectCreationTest, testAudioPlayerFromFdCreation) {
    ALOGV("Test Fixture: AudioPlayerFromFdCreation");
    // required for AudioPlayer creation
    OutputMixCreation();
    AudioPlayerFromFdCreation();
}

TEST_F(SLObjectCreationTest, testAudioPlayerFromPcmBqCreation) {
    ALOGV("Test Fixture: AudioPlayerFromPcmBqCreation");
    // required for AudioPlayer creation
    OutputMixCreation();
    AudioPlayerFromPcmBqCreation();
}

TEST_F(SLObjectCreationTest, testAudioPlayerFromTsAbqCreation) {
    ALOGV("Test Fixture: AudioPlayerFromTsAbqCreation");
    // required for AudioPlayer creation
    OutputMixCreation();
    AudioPlayerFromTsAbqCreation();
}

TEST_F(SLObjectCreationTest, testAudioPlayerFromUriToPcmBqCreation) {
    ALOGV("Test Fixture: AudioPlayerFromUriToPcmBqCreation");
    AudioPlayerFromUriToPcmBqCreation();
}

TEST_F(SLObjectCreationTest, testAudioPlayerFromFdToPcmBqCreation) {
    ALOGV("Test Fixture: AudioPlayerFromFdToPcmBqCreation");
    AudioPlayerFromFdToPcmBqCreation();
}

TEST_F(SLObjectCreationTest, testAudioPlayerFromAdtsAbqToPcmBqCreation) {
    ALOGV("Test Fixture: AudioPlayerFromAdtsAbqToPcmBqCreation");
    AudioPlayerFromAdtsAbqToPcmBqCreation();
}

TEST_F(SLObjectCreationTest, testAudioRecorderCreation) {
    ALOGV("Test Fixture: AudioRecorderCreation");
    // cannot Realize as native test cannot have necessary permission.
    AudioRecorderCreation(true);
}

int main(int argc, char **argv) {
    testing::InitGoogleTest(&argc, argv);

    return RUN_ALL_TESTS();
}
