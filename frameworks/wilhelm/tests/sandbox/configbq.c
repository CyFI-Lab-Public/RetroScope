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

// Test various buffer queue configurations

#include <assert.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#include <SLES/OpenSLES.h>

typedef struct {
    SLuint8 numChannels;
    SLuint32 milliHz;
    SLuint8 bitsPerSample;
} PCM;

PCM formats[] = {
    {1, SL_SAMPLINGRATE_8,      8},
    {2, SL_SAMPLINGRATE_8,      8},
    {1, SL_SAMPLINGRATE_8,      16},
    {2, SL_SAMPLINGRATE_8,      16},
    {1, SL_SAMPLINGRATE_11_025, 8},
    {2, SL_SAMPLINGRATE_11_025, 8},
    {1, SL_SAMPLINGRATE_11_025, 16},
    {2, SL_SAMPLINGRATE_11_025, 16},
    {1, SL_SAMPLINGRATE_12,     8},
    {2, SL_SAMPLINGRATE_12,     8},
    {1, SL_SAMPLINGRATE_12,     16},
    {2, SL_SAMPLINGRATE_12,     16},
    {1, SL_SAMPLINGRATE_16,     8},
    {2, SL_SAMPLINGRATE_16,     8},
    {1, SL_SAMPLINGRATE_16,     16},
    {2, SL_SAMPLINGRATE_16,     16},
    {1, SL_SAMPLINGRATE_22_05,  8},
    {2, SL_SAMPLINGRATE_22_05,  8},
    {1, SL_SAMPLINGRATE_22_05,  16},
    {2, SL_SAMPLINGRATE_22_05,  16},
    {1, SL_SAMPLINGRATE_24,     8},
    {2, SL_SAMPLINGRATE_24,     8},
    {1, SL_SAMPLINGRATE_24,     16},
    {2, SL_SAMPLINGRATE_24,     16},
    {1, SL_SAMPLINGRATE_32,     8},
    {2, SL_SAMPLINGRATE_32,     8},
    {1, SL_SAMPLINGRATE_32,     16},
    {2, SL_SAMPLINGRATE_32,     16},
    {1, SL_SAMPLINGRATE_44_1,   8},
    {2, SL_SAMPLINGRATE_44_1,   8},
    {1, SL_SAMPLINGRATE_44_1,   16},
    {2, SL_SAMPLINGRATE_44_1,   16},
    {1, SL_SAMPLINGRATE_48,     8},
    {2, SL_SAMPLINGRATE_48,     8},
    {1, SL_SAMPLINGRATE_48,     16},
    {2, SL_SAMPLINGRATE_48,     16},
    {0, 0,                      0}
};

int main(int argc, char **argv)
{
    SLresult result;
    SLObjectItf engineObject;

    // create engine
    result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);
    SLEngineItf engineEngine;
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    assert(SL_RESULT_SUCCESS == result);

    // create output mix
    SLObjectItf outputMixObject;
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);

    // loop over all formats
    PCM *format;
    float hzLeft = 440.0;   // A440 (Concert A)
    float hzRight = 440.0;
    for (format = formats; format->numChannels; ++format) {

        printf("Channels: %d, sample rate: %u, bits: %u\n", format->numChannels,
                format->milliHz / 1000, format->bitsPerSample);

        // configure audio source
        SLDataLocator_BufferQueue loc_bufq;
        loc_bufq.locatorType = SL_DATALOCATOR_BUFFERQUEUE;
        loc_bufq.numBuffers = 1;
        SLDataFormat_PCM format_pcm;
        format_pcm.formatType = SL_DATAFORMAT_PCM;
        format_pcm.numChannels = format->numChannels;
        format_pcm.samplesPerSec = format->milliHz;
        format_pcm.bitsPerSample = format->bitsPerSample;
        format_pcm.containerSize = format->bitsPerSample;
        format_pcm.channelMask = 0;
        format_pcm.endianness = SL_BYTEORDER_LITTLEENDIAN;
        SLDataSource audioSrc;
        audioSrc.pLocator = &loc_bufq;
        audioSrc.pFormat = &format_pcm;

        // configure audio sink
        SLDataLocator_OutputMix loc_outmix;
        loc_outmix.locatorType = SL_DATALOCATOR_OUTPUTMIX;
        loc_outmix.outputMix = outputMixObject;
        SLDataSink audioSnk;
        audioSnk.pLocator = &loc_outmix;
        audioSnk.pFormat = NULL;

        // create audio player
        SLuint32 numInterfaces = 1;
        SLInterfaceID ids[1];
        SLboolean req[1];
        ids[0] = SL_IID_BUFFERQUEUE;
        req[0] = SL_BOOLEAN_TRUE;
        SLObjectItf playerObject;
        result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
                &audioSnk, numInterfaces, ids, req);
        if (SL_RESULT_SUCCESS != result) {
            printf("failed %u\n", result);
            continue;
        }

        // realize the player
        result = (*playerObject)->Realize(playerObject, SL_BOOLEAN_FALSE);
        assert(SL_RESULT_SUCCESS == result);

        // generate a sine wave buffer, ascending in half-steps for each format
#define N (44100*4)
        static unsigned char buffer[N];
        unsigned i;
        for (i = 0; i < N; ) {
            float seconds = (((i * 8) / (format->bitsPerSample * format->numChannels)) * 1000.0) /
                    format->milliHz;
            short sampleLeft = sin(seconds * M_PI_2 * hzLeft) * 32767.0;
            short sampleRight = sin(seconds * M_PI_2 * hzRight) * 32767.0;
            if (2 == format->numChannels) {
                if (8 == format->bitsPerSample) {
                    buffer[i++] = (sampleLeft + 32768) >> 8;
                    buffer[i++] = (sampleRight + 32768) >> 8;
                } else {
                    assert(16 == format->bitsPerSample);
                    buffer[i++] = sampleLeft & 0xFF;
                    buffer[i++] = sampleLeft >> 8;
                    buffer[i++] = sampleRight & 0xFF;
                    buffer[i++] = sampleRight >> 8;
                }
            } else {
                assert(1 == format->numChannels);
                // cast to int and divide by 2 are needed to prevent overflow
                short sampleMono = ((int) sampleLeft + (int) sampleRight) / 2;
                if (8 == format->bitsPerSample) {
                    buffer[i++] = (sampleMono + 32768) >> 8;
                } else {
                    assert(16 == format->bitsPerSample);
                    buffer[i++] = sampleMono & 0xFF;
                    buffer[i++] = sampleMono >> 8;
                }
            }
            if (seconds >= 1.0f)
                break;
        }

        // get the buffer queue interface and enqueue a buffer
        SLBufferQueueItf playerBufferQueue;
        result = (*playerObject)->GetInterface(playerObject, SL_IID_BUFFERQUEUE,
                &playerBufferQueue);
        assert(SL_RESULT_SUCCESS == result);
        result = (*playerBufferQueue)->Enqueue(playerBufferQueue, buffer, i);
        assert(SL_RESULT_SUCCESS == result);

        // get the play interface
        SLPlayItf playerPlay;
        result = (*playerObject)->GetInterface(playerObject, SL_IID_PLAY, &playerPlay);
        assert(SL_RESULT_SUCCESS == result);

        // set the player's state to playing
        result = (*playerPlay)->SetPlayState(playerPlay, SL_PLAYSTATE_PLAYING);
        assert(SL_RESULT_SUCCESS == result);

        // wait for the buffer to be played
        for (;;) {
            SLBufferQueueState state;
            result = (*playerBufferQueue)->GetState(playerBufferQueue, &state);
            assert(SL_RESULT_SUCCESS == result);
            if (state.count == 0)
                break;
            usleep(20000);
        }

        // destroy audio player
        (*playerObject)->Destroy(playerObject);

        //usleep(1000000);
        hzLeft *= 1.05946309; // twelfth root of 2
        hzRight /= 1.05946309;
    }

    // destroy output mix
    (*outputMixObject)->Destroy(outputMixObject);

    // destroy engine
    (*engineObject)->Destroy(engineObject);

    return EXIT_SUCCESS;
}
