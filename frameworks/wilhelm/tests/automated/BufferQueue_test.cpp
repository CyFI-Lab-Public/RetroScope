/*
 * Copyright (C) 2010 The Android Open Source Project
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

/** \file BufferQueue_test.cpp */

#define LOG_NDEBUG 0
#define LOG_TAG "BufferQueue_test"

#ifdef ANDROID
#include <utils/Log.h>
#else
#define ALOGV printf
#endif

#include <assert.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <SLES/OpenSLES.h>
#include "OpenSLESUT.h"
#include <gtest/gtest.h>

typedef struct {
    short left;
    short right;
} stereo;

// volume of sine wave in range 0.0 to 1.0
static float gVolume = 1.0f;

// 1 second of stereo audio at 44.1 kHz
static stereo stereoBuffer1[44100 * 1];
static const SLuint32 invalidNumBuffers[] = { 0, 0xFFFFFFFF, 0x80000000, 0x10002, 0x102,
        0x101, 0x100 };
static const SLuint32 validNumBuffers[] = { 1, 2, 3, 4, 5, 6, 7, 8, 255 };

//-----------------------------------------------------------------
/* Checks for error. If any errors exit the application! */
void CheckErr(SLresult res) {
    if (SL_RESULT_SUCCESS != res) {
        const char *str = slesutResultToString(res);
        if (NULL == str)
            str = "unknown";
        fprintf(stderr, "CheckErr failure: %s (0x%x), exiting\n", str, res);
        //Fail the test case
        FAIL();
    }
}

static const SLInterfaceID ids[1] = { SL_IID_BUFFERQUEUE };
static const SLboolean flags[1] = { SL_BOOLEAN_TRUE };
static const SLInterfaceID ids_mutesolo[2] = { SL_IID_BUFFERQUEUE, SL_IID_MUTESOLO };
static const SLboolean flags_mutesolo[2] = { SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE };
static const SLInterfaceID ids_seek[2] = { SL_IID_BUFFERQUEUE, SL_IID_SEEK };
static const SLboolean flags_seek[2] = { SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE };

// The fixture for testing class BufferQueue
class TestBufferQueue: public ::testing::Test {
public:
    SLresult res;
    SLObjectItf outputmixObject;
    SLObjectItf engineObject;

    SLDataSource audiosrc;
    SLDataSink audiosnk;
    SLDataFormat_PCM pcm;
    SLDataLocator_OutputMix locator_outputmix;
    SLDataLocator_BufferQueue locator_bufferqueue;
    SLBufferQueueItf playerBufferQueue;
    SLBufferQueueState bufferqueueState;
    SLPlayItf playerPlay;
    SLObjectItf playerObject;
    SLEngineItf engineEngine;
    SLuint32 playerState;

protected:
    TestBufferQueue() {
    }

    virtual ~TestBufferQueue() {

    }

    /*Test setup*/
    virtual void SetUp() {

        // create engine
        res = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
        CheckErr(res);
        res = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
        CheckErr(res);
        res = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
        CheckErr(res);

        // create output mix
        res = (*engineEngine)->CreateOutputMix(engineEngine, &outputmixObject, 0, NULL, NULL);
        CheckErr(res);
        res = (*outputmixObject)->Realize(outputmixObject, SL_BOOLEAN_FALSE);
        CheckErr(res);

        locator_bufferqueue.locatorType = SL_DATALOCATOR_BUFFERQUEUE;
        locator_bufferqueue.numBuffers = 0;
        locator_outputmix.locatorType = SL_DATALOCATOR_OUTPUTMIX;
        locator_outputmix.outputMix = outputmixObject;

        pcm.formatType = SL_DATAFORMAT_PCM;
        pcm.numChannels = 2;
        pcm.samplesPerSec = SL_SAMPLINGRATE_44_1;
        pcm.bitsPerSample = SL_PCMSAMPLEFORMAT_FIXED_16;
        pcm.containerSize = 16;
        pcm.channelMask = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
        pcm.endianness = SL_BYTEORDER_LITTLEENDIAN;

        audiosrc.pLocator = &locator_bufferqueue;
        audiosrc.pFormat = &pcm;
        audiosnk.pLocator = &locator_outputmix;
        audiosnk.pFormat = NULL;

        // initialize the test tone to be a sine sweep from 441 Hz to 882 Hz
        unsigned nframes = sizeof(stereoBuffer1) / sizeof(stereoBuffer1[0]);
        float nframes_ = (float) nframes;
        SLuint32 i;
        for (i = 0; i < nframes; ++i) {
            float i_ = (float) i;
            float pcm_ = sin((i_ * (1.0f + 0.5f * (i_ / nframes_)) * 0.01 * M_PI * 2.0));
            int pcm = (int) (pcm_ * 32766.0 * gVolume);
            ASSERT_TRUE(-32768 <= pcm && pcm <= 32767) << "pcm out of bound " << pcm;
            stereoBuffer1[i].left = pcm;
            stereoBuffer1[nframes - 1 - i].right = pcm;
        }
    }

    virtual void TearDown() {
        // Clean up the mixer and the engine
        // (must be done in that order, and after player destroyed)
        if (outputmixObject){
            (*outputmixObject)->Destroy(outputmixObject);
            outputmixObject = NULL;
        }
        if (engineObject){
            (*engineObject)->Destroy(engineObject);
            engineObject = NULL;
        }
    }

    void DestroyPlayer() {
        if (playerObject){
            //printf("destroy player\n");
            (*playerObject)->Destroy(playerObject);
            playerObject = NULL;
        }
    }

    /* Test case for creating audio player with various invalid values for numBuffers*/
    void InvalidBuffer() {

        for (unsigned i = 0; i < sizeof(invalidNumBuffers) / sizeof(invalidNumBuffers[0]); ++i) {
            SLuint32 numBuffers = invalidNumBuffers[i];

            locator_bufferqueue.numBuffers = numBuffers;
            //printf("create audio player - invalid\n");
            SLresult result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject,
                                                            &audiosrc, &audiosnk, 1, ids, flags);
            ASSERT_EQ(SL_RESULT_PARAMETER_INVALID, result);
            ASSERT_EQ(NULL, playerObject);

        }
    }

    /*Prepare the buffer*/
    void PrepareValidBuffer(SLuint32 numBuffers) {

        locator_bufferqueue.numBuffers = numBuffers;
        //printf("create audio player - valid\n");
        res = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audiosrc, &audiosnk,
                                                1, ids, flags);
        CheckErr(res);
        res = (*playerObject)->Realize(playerObject, SL_BOOLEAN_FALSE);
        CheckErr(res);
        // get the play interface
        res = (*playerObject)->GetInterface(playerObject, SL_IID_PLAY, &playerPlay);
        CheckErr(res);
        // verify that player is initially stopped
        res = (*playerPlay)->GetPlayState(playerPlay, &playerState);
        CheckErr(res);
        ASSERT_EQ(SL_PLAYSTATE_STOPPED, playerState);

        // get the buffer queue interface
        res = (*playerObject)->GetInterface(playerObject, SL_IID_BUFFERQUEUE, &playerBufferQueue);
        CheckErr(res);

        // verify that buffer queue is initially empty
        res = (*playerBufferQueue)->GetState(playerBufferQueue, &bufferqueueState);
        CheckErr(res);
        ASSERT_EQ((SLuint32) 0, bufferqueueState.count);
        ASSERT_EQ((SLuint32) 0, bufferqueueState.playIndex);
    }

    void EnqueueMaxBuffer(SLuint32 numBuffers) {
        SLuint32 j;

        for (j = 0; j < numBuffers; ++j) {
            res = (*playerBufferQueue)->Enqueue(playerBufferQueue, "test", 4);
            CheckErr(res);
            // verify that each buffer is enqueued properly and increments the buffer count
            res = (*playerBufferQueue)->GetState(playerBufferQueue, &bufferqueueState);
            CheckErr(res);
            ASSERT_EQ(j + 1, bufferqueueState.count);
            ASSERT_EQ((SLuint32) 0, bufferqueueState.playIndex);
        }
    }

    void EnqueueExtraBuffer(SLuint32 numBuffers) {
        // enqueue one more buffer and make sure it fails
        res = (*playerBufferQueue)->Enqueue(playerBufferQueue, "test", 4);
        ASSERT_EQ(SL_RESULT_BUFFER_INSUFFICIENT, res);
        // verify that the failed enqueue did not affect the buffer count
        res = (*playerBufferQueue)->GetState(playerBufferQueue, &bufferqueueState);
        CheckErr(res);
        ASSERT_EQ(numBuffers, bufferqueueState.count);
        ASSERT_EQ((SLuint32) 0, bufferqueueState.playIndex);
    }

    void SetPlayerState(SLuint32 state) {
        res = (*playerPlay)->SetPlayState(playerPlay, state);
        CheckErr(res);
        //verify the state can set correctly
        GetPlayerState(state);
    }

    void GetPlayerState(SLuint32 state) {
        res = (*playerPlay)->GetPlayState(playerPlay, &playerState);
        CheckErr(res);
        ASSERT_EQ(state, playerState);
    }

    void ClearQueue() {
        // now clear the buffer queue
        res = (*playerBufferQueue)->Clear(playerBufferQueue);
        CheckErr(res);
        // make sure the clear works
        res = (*playerBufferQueue)->GetState(playerBufferQueue, &bufferqueueState);
        CheckErr(res);
        ASSERT_EQ((SLuint32) 0, bufferqueueState.count);
        ASSERT_EQ((SLuint32) 0, bufferqueueState.playIndex);
    }

    void CheckBufferCount(SLuint32 ExpectedCount, SLuint32 ExpectedPlayIndex) {
        // changing the play state should not affect the buffer count
        res = (*playerBufferQueue)->GetState(playerBufferQueue, &bufferqueueState);
        CheckErr(res);
        ASSERT_EQ(ExpectedCount, bufferqueueState.count);
        ASSERT_EQ(ExpectedPlayIndex, bufferqueueState.playIndex);
    }

    void PlayBufferQueue() {
        // enqueue a buffer
        res = (*playerBufferQueue)->Enqueue(playerBufferQueue, stereoBuffer1,
            sizeof(stereoBuffer1));
        CheckErr(res);
        // set play state to playing
        res = (*playerPlay)->SetPlayState(playerPlay, SL_PLAYSTATE_PLAYING);
        CheckErr(res);
        // state should be playing immediately after enqueue
        res = (*playerPlay)->GetPlayState(playerPlay, &playerState);
        CheckErr(res);
        ASSERT_EQ(SL_PLAYSTATE_PLAYING, playerState);
        // buffer should still be on the queue
        res = (*playerBufferQueue)->GetState(playerBufferQueue, &bufferqueueState);
        CheckErr(res);
        ASSERT_EQ((SLuint32) 1, bufferqueueState.count);
        ASSERT_EQ((SLuint32) 0, bufferqueueState.playIndex);
        //ALOGV("Before 1.5 sec");
        // wait 1.5 seconds
        usleep(1500000);
        //ALOGV("After 1.5 sec");
        // state should still be playing
        res = (*playerPlay)->GetPlayState(playerPlay, &playerState);
        //ALOGV("GetPlayState");
        CheckErr(res);
        ASSERT_EQ(SL_PLAYSTATE_PLAYING, playerState);
        // buffer should be removed from the queue
        res = (*playerBufferQueue)->GetState(playerBufferQueue, &bufferqueueState);
        CheckErr(res);
        ASSERT_EQ((SLuint32) 0, bufferqueueState.count);
        ASSERT_EQ((SLuint32) 1, bufferqueueState.playIndex);
        //ALOGV("TestEnd");
    }
};

TEST_F(TestBufferQueue, testInvalidBuffer){
    //ALOGV("Test Fixture: InvalidBuffer");
    InvalidBuffer();
}

TEST_F(TestBufferQueue, testMuteSolo) {
    // create audio player with buffer queue data source in stereo PCM format and ask for mute solo
    locator_bufferqueue.numBuffers = 1;
    SLresult result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audiosrc,
            &audiosnk, 2, ids_mutesolo, flags_mutesolo);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    ASSERT_TRUE(NULL != playerObject);
    DestroyPlayer();
    // create audio player with buffer queue data source in mono PCM format and ask for mute solo
    pcm.numChannels = 1;
    pcm.channelMask = SL_SPEAKER_FRONT_CENTER;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audiosrc, &audiosnk,
            2, ids_mutesolo, flags_mutesolo);
    ASSERT_EQ(SL_RESULT_FEATURE_UNSUPPORTED, result);
    ASSERT_EQ(NULL, playerObject);
    DestroyPlayer();
}

TEST_F(TestBufferQueue, testSeek) {
    // can create audio player with buffer queue data source and ask for seek
    locator_bufferqueue.numBuffers = 1;
    SLresult result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject,
                                                    &audiosrc, &audiosnk, 2, ids_seek, flags_seek);
    ASSERT_EQ(SL_RESULT_FEATURE_UNSUPPORTED, result);
    ASSERT_EQ(NULL, playerObject);
    DestroyPlayer();
}

TEST_F(TestBufferQueue, testValidBuffer) {
    for (unsigned i = 0; i < sizeof(validNumBuffers) / sizeof(validNumBuffers[0]); ++i) {
        SLuint32 numBuffers = validNumBuffers[i];
        PrepareValidBuffer(numBuffers);
        DestroyPlayer();
    }
}

TEST_F(TestBufferQueue, testEnqueueMaxBuffer) {
    for (unsigned i = 0; i < sizeof(validNumBuffers) / sizeof(validNumBuffers[0]); ++i) {
        SLuint32 numBuffers = validNumBuffers[i];
        PrepareValidBuffer(numBuffers);
        EnqueueMaxBuffer(numBuffers);
        DestroyPlayer();
    }
}

TEST_F(TestBufferQueue, testEnqueueExtraBuffer) {
    for (unsigned i = 0; i < sizeof(validNumBuffers) / sizeof(validNumBuffers[0]); ++i) {
        SLuint32 numBuffers = validNumBuffers[i];
        PrepareValidBuffer(numBuffers);
        EnqueueMaxBuffer(numBuffers);
        EnqueueExtraBuffer(numBuffers);
        GetPlayerState(SL_PLAYSTATE_STOPPED);
        DestroyPlayer();
    }
}

TEST_F(TestBufferQueue, testEnqueueAtStopped) {
    for (unsigned i = 0; i < sizeof(validNumBuffers) / sizeof(validNumBuffers[0]); ++i) {
        SLuint32 numBuffers = validNumBuffers[i];
        PrepareValidBuffer(numBuffers);
        SetPlayerState(SL_PLAYSTATE_STOPPED);
        EnqueueMaxBuffer(numBuffers);
        CheckBufferCount(numBuffers, (SLuint32) 0);
        DestroyPlayer();
    }
}

TEST_F(TestBufferQueue, testEnqueueAtPaused) {
    for (unsigned i = 0; i < sizeof(validNumBuffers) / sizeof(validNumBuffers[0]); ++i) {
        SLuint32 numBuffers = validNumBuffers[i];
        PrepareValidBuffer(numBuffers);
        SetPlayerState(SL_PLAYSTATE_PAUSED);
        EnqueueMaxBuffer(numBuffers);
        CheckBufferCount(numBuffers, (SLuint32) 0);
        DestroyPlayer();
    }
}

TEST_F(TestBufferQueue, testClearQueue) {
    for (unsigned i = 0; i < sizeof(validNumBuffers) / sizeof(validNumBuffers[0]); ++i) {
        SLuint32 numBuffers = validNumBuffers[i];
        PrepareValidBuffer(numBuffers);
        EnqueueMaxBuffer(numBuffers);
        ClearQueue();
        DestroyPlayer();
    }
}

TEST_F(TestBufferQueue, testStateTransitionEmptyQueue) {
    static const SLuint32 newStates[] = {
        SL_PLAYSTATE_PAUSED,    // paused -> paused
        SL_PLAYSTATE_STOPPED,   // paused -> stopped
        SL_PLAYSTATE_PAUSED,    // stopped -> paused
        SL_PLAYSTATE_PLAYING,   // paused -> playing
        SL_PLAYSTATE_PLAYING,   // playing -> playing
        SL_PLAYSTATE_STOPPED,   // playing -> stopped
        SL_PLAYSTATE_STOPPED,   // stopped -> stopped
        SL_PLAYSTATE_PLAYING,   // stopped -> playing
        SL_PLAYSTATE_PAUSED     // playing -> paused
    };

    for (unsigned i = 0; i < sizeof(validNumBuffers) / sizeof(validNumBuffers[0]); ++i) {
        SLuint32 numBuffers = validNumBuffers[i];
        SLuint32 j;

        PrepareValidBuffer(numBuffers);
        /* Set initial state to paused*/
        SetPlayerState(SL_PLAYSTATE_PAUSED);

        for (j = 0; j < sizeof(newStates) / sizeof(newStates[0]); ++j) {
            SetPlayerState(newStates[j]);
            CheckBufferCount((SLuint32) 0, (SLuint32) 0);
        }
        DestroyPlayer();
    }
}

TEST_F(TestBufferQueue, testStateTransitionNonEmptyQueue) {
    static const SLuint32 newStates[] = {
        SL_PLAYSTATE_PAUSED,    // paused -> paused
        SL_PLAYSTATE_STOPPED,   // paused -> stopped
        SL_PLAYSTATE_STOPPED,   // stopped -> stopped
        SL_PLAYSTATE_PAUSED     // stopped -> paused
    };

    for (unsigned i = 0; i < sizeof(validNumBuffers) / sizeof(validNumBuffers[0]); ++i) {
        SLuint32 numBuffers = validNumBuffers[i];
        SLuint32 j;

        /* Prepare the player */
        PrepareValidBuffer(numBuffers);
        EnqueueMaxBuffer(numBuffers);
        SetPlayerState(SL_PLAYSTATE_PAUSED);

        for (j = 0; j < sizeof(newStates) / sizeof(newStates[0]); ++j) {
            SetPlayerState(newStates[j]);
            CheckBufferCount(numBuffers, (SLuint32) 0);
        }
        DestroyPlayer();
    }
}

TEST_F(TestBufferQueue, testStatePlayBuffer){
    for (unsigned i = 0; i < sizeof(validNumBuffers) / sizeof(validNumBuffers[0]); ++i) {
        SLuint32 numBuffers = validNumBuffers[i];
        PrepareValidBuffer(numBuffers);
        PlayBufferQueue();
        DestroyPlayer();
    }
}

int main(int argc, char **argv) {
    testing::InitGoogleTest(&argc, argv);
#if 1   // temporary workaround if hardware volume control is not working
    const char *VOLUME = getenv("BufferQueue_test_VOLUME");
    if (NULL != VOLUME) {
        float volume = atof(VOLUME);
        if (volume >= 0.0f && volume <= 1.0f) {
            gVolume = volume;
        }
    }
#endif
    return RUN_ALL_TESTS();
}
