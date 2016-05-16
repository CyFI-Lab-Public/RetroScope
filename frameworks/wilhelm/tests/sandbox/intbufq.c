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

/* interactive buffer queue test program */

#ifdef ANDROID
#define USE_ANDROID_SIMPLE_BUFFER_QUEUE     // change to #undef for compatibility testing
#endif

#include <assert.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <SLES/OpenSLES.h>
#ifdef USE_ANDROID_SIMPLE_BUFFER_QUEUE
#include <SLES/OpenSLES_Android.h>
#endif
#include "getch.h"

#ifdef USE_ANDROID_SIMPLE_BUFFER_QUEUE
#define DATALOCATOR_BUFFERQUEUE SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE
#define IID_BUFFERQUEUE SL_IID_ANDROIDSIMPLEBUFFERQUEUE
#define BufferQueueItf SLAndroidSimpleBufferQueueItf
#define BufferQueueState SLAndroidSimpleBufferQueueState
#define INDEX index
#else
#define DATALOCATOR_BUFFERQUEUE SL_DATALOCATOR_BUFFERQUEUE
#define IID_BUFFERQUEUE SL_IID_BUFFERQUEUE
#define BufferQueueItf SLBufferQueueItf
#define BufferQueueState SLBufferQueueState
#define INDEX playIndex
#endif

#define checkResult(r) do { if ((r) != SL_RESULT_SUCCESS) fprintf(stderr, "error %d at %s:%d\n", \
    (int) r, __FILE__, __LINE__); } while (0)

typedef struct {
    short left;
    short right;
} frame_t;

#define SINE_FRAMES (44100*5)
frame_t sine[SINE_FRAMES];

#define SQUARE_FRAMES (44100*5)
frame_t square[SQUARE_FRAMES];

#define SAWTOOTH_FRAMES (44100*5)
frame_t sawtooth[SAWTOOTH_FRAMES];

#define HALF_FRAMES (44100*5)
frame_t half[HALF_FRAMES];

BufferQueueItf expectedCaller = NULL;
void *expectedContext = NULL;

static void callback(BufferQueueItf caller, void *context)
{
    putchar('.');
    if (caller != expectedCaller)
        printf("caller %p expected %p\r\n", caller, expectedCaller);
    if (context != expectedContext)
        printf("context %p expected %p\r\n", context, expectedContext);
    fflush(stdout);
}

int main(int argc, char **argv)
{
    SLresult result;

    // create engine
    SLObjectItf engineObject;
    result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    checkResult(result);
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    checkResult(result);
    SLEngineItf engineEngine;
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    checkResult(result);

    // create output mix
    SLObjectItf outputmixObject;
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputmixObject, 0, NULL, NULL);
    checkResult(result);
    result = (*outputmixObject)->Realize(outputmixObject, SL_BOOLEAN_FALSE);
    checkResult(result);

    // create audio player
    SLDataSource audiosrc;
    SLDataSink audiosnk;
    SLDataFormat_PCM pcm;
    SLDataLocator_OutputMix locator_outputmix;
    SLDataLocator_BufferQueue locator_bufferqueue;
    locator_bufferqueue.locatorType = DATALOCATOR_BUFFERQUEUE;
    locator_bufferqueue.numBuffers = 255;
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
    SLObjectItf playerObject;
    SLInterfaceID ids[2] = {IID_BUFFERQUEUE, SL_IID_MUTESOLO};
    SLboolean flags[2] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audiosrc, &audiosnk,
            2, ids, flags);
    checkResult(result);
    result = (*playerObject)->Realize(playerObject, SL_BOOLEAN_FALSE);
    checkResult(result);
    SLPlayItf playerPlay;
    result = (*playerObject)->GetInterface(playerObject, SL_IID_PLAY, &playerPlay);
    checkResult(result);
    BufferQueueItf playerBufferqueue;
    result = (*playerObject)->GetInterface(playerObject, IID_BUFFERQUEUE, &playerBufferqueue);
    checkResult(result);
    SLMuteSoloItf playerMuteSolo;
    result = (*playerObject)->GetInterface(playerObject, SL_IID_MUTESOLO, &playerMuteSolo);
    checkResult(result);
    SLuint8 numChannels = 123;
    result = (*playerMuteSolo)->GetNumChannels(playerMuteSolo, &numChannels);
    assert(2 == numChannels);
    SLuint32 state;
    state = SL_PLAYSTATE_PLAYING;
    result = (*playerPlay)->SetPlayState(playerPlay, state);
    checkResult(result);

    unsigned i;
    float pi2 = 3.14*2;
    float hz = 441;
    float sr = 44100;
    for (i = 0; i < SINE_FRAMES; ++i) {
        sine[i].left = sin((float) (i  / (sr / hz)) * pi2 ) * 32000.0;
        sine[i].right = sine[i].left;
    }
    for (i = 0; i < SQUARE_FRAMES; ++i) {
        square[i].left = (i % (unsigned) (sr / hz)) < 50 ? 32767 : -32768;
        square[i].right = square[i].left;
    }
    for (i = 0; i < SAWTOOTH_FRAMES; ++i) {
        sawtooth[i].left = ((((int) (i % (unsigned) (sr / hz))) - 50) / 100.0) * 60000.0 - 30000.0;
        sawtooth[i].right = sawtooth[i].left;
    }
    for (i = 0; i < HALF_FRAMES; ++i) {
        half[i].left = sine[i].left;
        half[i].right = sawtooth[i].right / 2;
    }

    set_conio_terminal_mode();
    int in_count = 0;
    unsigned count = 0;
    for (;;) {
        usleep(10000);
        if (kbhit()) {
            frame_t *buffer;
            unsigned size;
            BufferQueueState bufqstate;
            int ch = getch();
            switch (ch) {
            case '0' ... '9':
                if (in_count) {
                    count = count * 10 + (ch - '0');
                } else {
                    count = ch - '0';
                    in_count = 1;
                }
                continue;
            case 'i':
                buffer = sine;
                size = sizeof(sine);
                goto enqueue;
            case 'q':
                buffer = square;
                size = sizeof(square);
                goto enqueue;
            case 'h':
                buffer = half;
                size = sizeof(half);
                goto enqueue;
            case 'r':
                if (in_count) {
                    expectedCaller = playerBufferqueue;
                    expectedContext = (void *) count;
                } else {
                    expectedCaller = NULL;
                    expectedContext = (void *) NULL;
                }
                result = (*playerBufferqueue)->RegisterCallback(playerBufferqueue, in_count ?
                    callback : NULL, expectedContext);
                checkResult(result);
                break;
            case 'a':
                buffer = sawtooth;
                size = sizeof(sawtooth);
enqueue:
                for (i = 0; i < (in_count ? count : 1); ++i) {
                    result = (*playerBufferqueue)->Enqueue(playerBufferqueue, buffer, size);
                    checkResult(result);
                }
                break;
            case 'c':
                result = (*playerBufferqueue)->Clear(playerBufferqueue);
                checkResult(result);
                putchar('\r');
                result = (*playerBufferqueue)->GetState(playerBufferqueue, &bufqstate);
                checkResult(result);
                if (bufqstate.count != 0)
                    printf("\rcount=%u\r\n", (unsigned) bufqstate.count);
#if 0
                putchar('\r');
                putchar('\n');
#endif
                fflush(stdout);
                break;
            case 'g':
                result = (*playerBufferqueue)->GetState(playerBufferqueue, &bufqstate);
                checkResult(result);
                printf("\rplayIndex=%u\r\n", (unsigned) bufqstate.INDEX);
                printf("count=%u\r\n", (unsigned) bufqstate.count);
                break;
            case 'p':
                state = SL_PLAYSTATE_PAUSED;
                goto setplaystate;
            case 's':
                state = SL_PLAYSTATE_STOPPED;
                goto setplaystate;
            case 'P':
                state = SL_PLAYSTATE_PLAYING;
setplaystate:
                result = (*playerPlay)->SetPlayState(playerPlay, state);
                checkResult(result);
                SLuint32 newstate;
                result = (*playerPlay)->GetPlayState(playerPlay, &newstate);
                checkResult(result);
                if (newstate != state)
                    printf("\rSetPlayState(%u) -> GetPlayState(%u)\r\n", (unsigned) state,
                        (unsigned) newstate);
#if 0
                putchar('\r');
                putchar('\n');
                fflush(stdout);
#endif
                checkResult(result);
                break;
            case 'x':
                goto out;
            default:
                putchar('?');
                fflush(stdout);
                break;
            }
            in_count = 0;
        }
    }

out:
    (*playerObject)->Destroy(playerObject);
    (*outputmixObject)->Destroy(outputmixObject);
    (*engineObject)->Destroy(engineObject);
    return EXIT_SUCCESS;
}
