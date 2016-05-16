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

// Test program to record from default audio input and playback to default audio output.
// It will generate feedback (Larsen effect) if played through on-device speakers,
// or acts as a delay if played through headset.

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <assert.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define ASSERT_EQ(x, y) do { if ((x) == (y)) ; else { fprintf(stderr, "0x%x != 0x%x\n", \
    (unsigned) (x), (unsigned) (y)); assert((x) == (y)); } } while (0)

// default values
static SLuint32 rxBufCount = 2;     // -r#
static SLuint32 txBufCount = 2;     // -t#
static SLuint32 bufSizeInFrames = 512;  // -f#
static SLuint32 channels = 1;       // -c#
static SLuint32 sampleRate = 44100; // -s#
static SLuint32 exitAfterSeconds = 60; // -e#
static SLuint32 freeBufCount = 0;   // calculated
static SLuint32 bufSizeInBytes = 0; // calculated

// Storage area for the buffer queues
static char **rxBuffers;
static char **txBuffers;
static char **freeBuffers;

// Buffer indices
static SLuint32 rxFront;    // oldest recording
static SLuint32 rxRear;     // next to be recorded
static SLuint32 txFront;    // oldest playing
static SLuint32 txRear;     // next to be played
static SLuint32 freeFront;  // oldest free
static SLuint32 freeRear;   // next to be freed

static SLAndroidSimpleBufferQueueItf recorderBufferQueue;
static SLBufferQueueItf playerBufferQueue;

static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

// Called after audio recorder fills a buffer with data
static void recorderCallback(SLAndroidSimpleBufferQueueItf caller, void *context)
{
    SLresult result;

    pthread_mutex_lock(&mutex);

    // We should only be called when a recording buffer is done
    assert(rxFront <= rxBufCount);
    assert(rxRear <= rxBufCount);
    assert(rxFront != rxRear);
    char *buffer = rxBuffers[rxFront];

    // Remove buffer from record queue
    if (++rxFront > rxBufCount) {
        rxFront = 0;
    }

    // Enqueue the just-filled buffer for the player
    result = (*playerBufferQueue)->Enqueue(playerBufferQueue, buffer, bufSizeInBytes);
    if (SL_RESULT_SUCCESS == result) {

        // There was room in the play queue, update our model of it
        assert(txFront <= txBufCount);
        assert(txRear <= txBufCount);
        SLuint32 txRearNext = txRear+1;
        if (txRearNext > txBufCount) {
            txRearNext = 0;
        }
        assert(txRearNext != txFront);
        txBuffers[txRear] = buffer;
        txRear = txRearNext;

    } else {

        // Here if record has a filled buffer to play, but play queue is full.
        assert(SL_RESULT_BUFFER_INSUFFICIENT == result);
        write(1, "?", 1);

        // We could either try again later, or discard. For now we discard and re-use buffer.
        // Enqueue this same buffer for the recorder to fill again.
        result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue, buffer, bufSizeInBytes);
        ASSERT_EQ(SL_RESULT_SUCCESS, result);

        // Update our model of the record queue
        SLuint32 rxRearNext = rxRear+1;
        if (rxRearNext > rxBufCount) {
            rxRearNext = 0;
        }
        assert(rxRearNext != rxFront);
        rxBuffers[rxRear] = buffer;
        rxRear = rxRearNext;

    }

    pthread_mutex_unlock(&mutex);
}


// Called after audio player empties a buffer of data
static void playerCallback(SLBufferQueueItf caller, void *context)
{
    SLresult result;

    pthread_mutex_lock(&mutex);

    // Get the buffer that just finished playing
    assert(txFront <= txBufCount);
    assert(txRear <= txBufCount);
    assert(txFront != txRear);
    char *buffer = txBuffers[txFront];
    if (++txFront > txBufCount) {
        txFront = 0;
    }

    // First try to enqueue the free buffer for recording
    result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue, buffer, bufSizeInBytes);
    if (SL_RESULT_SUCCESS == result) {

        // There was room in the record queue, update our model of it
        assert(rxFront <= rxBufCount);
        assert(rxRear <= rxBufCount);
        SLuint32 rxRearNext = rxRear+1;
        if (rxRearNext > rxBufCount) {
            rxRearNext = 0;
        }
        assert(rxRearNext != rxFront);
        rxBuffers[rxRear] = buffer;
        rxRear = rxRearNext;

    } else {

        // Here if record queue is full
        assert(SL_RESULT_BUFFER_INSUFFICIENT == result);

        // Instead enqueue the free buffer on the free queue
        assert(freeFront <= freeBufCount);
        assert(freeRear <= freeBufCount);
        SLuint32 freeRearNext = freeRear+1;
        if (freeRearNext > freeBufCount) {
            freeRearNext = 0;
        }
        // There must always be room in the free queue
        assert(freeRearNext != freeFront);
        freeBuffers[freeRear] = buffer;
        freeRear = freeRearNext;

    }

    pthread_mutex_unlock(&mutex);
}

// Main program
int main(int argc, char **argv)
{
    // process command-line options
    int i;
    for (i = 1; i < argc; ++i) {
        char *arg = argv[i];
        if (arg[0] != '-') {
            break;
        }
        // -r# number of slots in receive buffer queue
        if (!strncmp(arg, "-r", 2)) {
            rxBufCount = atoi(&arg[2]);
            if (rxBufCount < 1 || rxBufCount > 16) {
                fprintf(stderr, "%s: unusual receive buffer queue size (%u buffers)\n", argv[0],
                    (unsigned) rxBufCount);
            }
        // -t# number of slots in transmit buffer queue
        } else if (!strncmp(arg, "-t", 2)) {
            txBufCount = atoi(&arg[2]);
            if (txBufCount < 1 || txBufCount > 16) {
                fprintf(stderr, "%s: unusual transmit buffer queue size (%u buffers)\n", argv[0],
                    (unsigned) txBufCount);
            }
        // -f# size of each buffer in frames
        } else if (!strncmp(arg, "-f", 2)) {
            bufSizeInFrames = atoi(&arg[2]);
            if (bufSizeInFrames == 0) {
                fprintf(stderr, "%s: unusual buffer size (%u frames)\n", argv[0],
                    (unsigned) bufSizeInFrames);
            }
        // -c1 mono or -c2 stereo
        } else if (!strncmp(arg, "-c", 2)) {
            channels = atoi(&arg[2]);
            if (channels < 1 || channels > 2) {
                fprintf(stderr, "%s: unusual channel count ignored (%u)\n", argv[0],
                    (unsigned) channels);
                channels = 2;
            }
        // -s# sample rate in Hz
        } else if (!strncmp(arg, "-s", 2)) {
            sampleRate = atoi(&arg[2]);
            switch (sampleRate) {
            case 8000:
            case 11025:
            case 12000:
            case 16000:
            case 22050:
            case 24000:
            case 32000:
            case 44100:
            case 48000:
                break;
            default:
                fprintf(stderr, "%s: unusual sample rate (%u Hz)\n", argv[0],
                    (unsigned) sampleRate);
                break;
            }
        // -e# exit after this many seconds
        } else if (!strncmp(arg, "-e", 2)) {
            exitAfterSeconds = atoi(&arg[2]);
        } else
            fprintf(stderr, "%s: unknown option %s\n", argv[0], arg);
    }
    // no other arguments allowed
    if (i < argc) {
        fprintf(stderr, "usage: %s -r# -t# -f# -s# -c#\n", argv[0]);
        fprintf(stderr, "  -r# receive buffer queue count for microphone input, default 1\n");
        fprintf(stderr, "  -t# transmit buffer queue count for speaker output, default 2\n");
        fprintf(stderr, "  -f# number of frames per buffer, default 512\n");
        fprintf(stderr, "  -s# sample rate in Hz, default 44100\n");
        fprintf(stderr, "  -c1 mono\n");
        fprintf(stderr, "  -c2 stereo, default\n");
    }
    // compute total free buffers as -r plus -t
    freeBufCount = rxBufCount + txBufCount;
    // compute buffer size
    bufSizeInBytes = channels * bufSizeInFrames * sizeof(short);

    // Initialize free buffers
    freeBuffers = (char **) calloc(freeBufCount+1, sizeof(char *));
    unsigned j;
    for (j = 0; j < freeBufCount; ++j) {
        freeBuffers[j] = (char *) malloc(bufSizeInBytes);
    }
    freeFront = 0;
    freeRear = freeBufCount;
    freeBuffers[j] = NULL;

    // Initialize record queue
    rxBuffers = (char **) calloc(rxBufCount+1, sizeof(char *));
    rxFront = 0;
    rxRear = 0;

    // Initialize play queue
    txBuffers = (char **) calloc(txBufCount+1, sizeof(char *));
    txFront = 0;
    txRear = 0;

    SLresult result;

    // create engine
    SLObjectItf engineObject;
    result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    SLEngineItf engineEngine;
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);

    // create output mix
    SLObjectItf outputmixObject;
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputmixObject, 0, NULL, NULL);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    result = (*outputmixObject)->Realize(outputmixObject, SL_BOOLEAN_FALSE);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);

    // create an audio player with buffer queue source and output mix sink
    SLDataSource audiosrc;
    SLDataSink audiosnk;
    SLDataFormat_PCM pcm;
    SLDataLocator_OutputMix locator_outputmix;
    SLDataLocator_BufferQueue locator_bufferqueue_tx;
    locator_bufferqueue_tx.locatorType = SL_DATALOCATOR_BUFFERQUEUE;
    locator_bufferqueue_tx.numBuffers = txBufCount;
    locator_outputmix.locatorType = SL_DATALOCATOR_OUTPUTMIX;
    locator_outputmix.outputMix = outputmixObject;
    pcm.formatType = SL_DATAFORMAT_PCM;
    pcm.numChannels = channels;
    pcm.samplesPerSec = sampleRate * 1000;
    pcm.bitsPerSample = SL_PCMSAMPLEFORMAT_FIXED_16;
    pcm.containerSize = 16;
    pcm.channelMask = channels == 1 ? SL_SPEAKER_FRONT_CENTER :
        (SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT);
    pcm.endianness = SL_BYTEORDER_LITTLEENDIAN;
    audiosrc.pLocator = &locator_bufferqueue_tx;
    audiosrc.pFormat = &pcm;
    audiosnk.pLocator = &locator_outputmix;
    audiosnk.pFormat = NULL;
    SLObjectItf playerObject = NULL;
    SLObjectItf recorderObject = NULL;
    SLInterfaceID ids_tx[1] = {SL_IID_BUFFERQUEUE};
    SLboolean flags_tx[1] = {SL_BOOLEAN_TRUE};
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audiosrc, &audiosnk,
        1, ids_tx, flags_tx);
    if (SL_RESULT_CONTENT_UNSUPPORTED == result) {
        fprintf(stderr, "Could not create audio player (result %x), check sample rate\n", result);
        goto cleanup;
    }
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    result = (*playerObject)->Realize(playerObject, SL_BOOLEAN_FALSE);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    SLPlayItf playerPlay;
    result = (*playerObject)->GetInterface(playerObject, SL_IID_PLAY, &playerPlay);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    result = (*playerObject)->GetInterface(playerObject, SL_IID_BUFFERQUEUE, &playerBufferQueue);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    result = (*playerBufferQueue)->RegisterCallback(playerBufferQueue, playerCallback, NULL);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    result = (*playerPlay)->SetPlayState(playerPlay, SL_PLAYSTATE_PLAYING);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);

    // Create an audio recorder with microphone device source and buffer queue sink.
    // The buffer queue as sink is an Android-specific extension.

    SLDataLocator_IODevice locator_iodevice;
    SLDataLocator_AndroidSimpleBufferQueue locator_bufferqueue_rx;
    locator_iodevice.locatorType = SL_DATALOCATOR_IODEVICE;
    locator_iodevice.deviceType = SL_IODEVICE_AUDIOINPUT;
    locator_iodevice.deviceID = SL_DEFAULTDEVICEID_AUDIOINPUT;
    locator_iodevice.device = NULL;
    audiosrc.pLocator = &locator_iodevice;
    audiosrc.pFormat = NULL;
    locator_bufferqueue_rx.locatorType = SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE;
    locator_bufferqueue_rx.numBuffers = rxBufCount;
    audiosnk.pLocator = &locator_bufferqueue_rx;
    audiosnk.pFormat = &pcm;
    {
    SLInterfaceID ids_rx[1] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
    SLboolean flags_rx[1] = {SL_BOOLEAN_TRUE};
    result = (*engineEngine)->CreateAudioRecorder(engineEngine, &recorderObject, &audiosrc,
        &audiosnk, 1, ids_rx, flags_rx);
    if (SL_RESULT_SUCCESS != result) {
        fprintf(stderr, "Could not create audio recorder (result %x), "
                "check sample rate and channel count\n", result);
        goto cleanup;
    }
    }
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    result = (*recorderObject)->Realize(recorderObject, SL_BOOLEAN_FALSE);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    SLRecordItf recorderRecord;
    result = (*recorderObject)->GetInterface(recorderObject, SL_IID_RECORD, &recorderRecord);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    result = (*recorderObject)->GetInterface(recorderObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
        &recorderBufferQueue);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    result = (*recorderBufferQueue)->RegisterCallback(recorderBufferQueue, recorderCallback, NULL);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);

    // Enqueue some empty buffers for the recorder
    for (j = 0; j < rxBufCount; ++j) {

        // allocate a free buffer
        assert(freeFront != freeRear);
        char *buffer = freeBuffers[freeFront];
        if (++freeFront > freeBufCount) {
            freeFront = 0;
        }

        // put on record queue
        SLuint32 rxRearNext = rxRear + 1;
        if (rxRearNext > rxBufCount) {
            rxRearNext = 0;
        }
        assert(rxRearNext != rxFront);
        rxBuffers[rxRear] = buffer;
        rxRear = rxRearNext;
        result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue,
            buffer, bufSizeInBytes);
        ASSERT_EQ(SL_RESULT_SUCCESS, result);
    }

    // Kick off the recorder
    result = (*recorderRecord)->SetRecordState(recorderRecord, SL_RECORDSTATE_RECORDING);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);

    // Wait patiently
    do {
        usleep(1000000);
        write(1, ".", 1);
        SLBufferQueueState playerBQState;
        result = (*playerBufferQueue)->GetState(playerBufferQueue, &playerBQState);
        ASSERT_EQ(SL_RESULT_SUCCESS, result);
        SLAndroidSimpleBufferQueueState recorderBQState;
        result = (*recorderBufferQueue)->GetState(recorderBufferQueue, &recorderBQState);
        ASSERT_EQ(SL_RESULT_SUCCESS, result);
    } while (--exitAfterSeconds);

    // Tear down the objects and exit
cleanup:
    if (NULL != playerObject) {
        (*playerObject)->Destroy(playerObject);
    }
    if (NULL != recorderObject) {
        (*recorderObject)->Destroy(recorderObject);
    }
    (*outputmixObject)->Destroy(outputmixObject);
    (*engineObject)->Destroy(engineObject);

    return EXIT_SUCCESS;
}
