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

// Test various combinations of data sources and sinks

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <SLES/OpenSLES.h>

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

    // configure a typical audio source of 44.1 kHz stereo 16-bit little endian
    SLDataLocator_BufferQueue loc_bufq;
    loc_bufq.locatorType = SL_DATALOCATOR_BUFFERQUEUE;
    loc_bufq.numBuffers = 1;
    SLDataFormat_PCM format_pcm;
    format_pcm.formatType = SL_DATAFORMAT_PCM;
    format_pcm.numChannels = 2;
    format_pcm.samplesPerSec = SL_SAMPLINGRATE_44_1;
    format_pcm.bitsPerSample = 16;
    format_pcm.containerSize = 16;
    format_pcm.channelMask = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
    format_pcm.endianness = SL_BYTEORDER_LITTLEENDIAN;
    SLDataSource audioSrc;
    audioSrc.pLocator = &loc_bufq;
    audioSrc.pFormat = &format_pcm;

    // configure audio sink
    SLDataLocator_OutputMix loc_outmix;
    loc_outmix.locatorType = SL_DATALOCATOR_OUTPUTMIX;
    loc_outmix.outputMix = NULL;
    SLDataSink audioSnk;
    audioSnk.pLocator = &loc_outmix;
    audioSnk.pFormat = NULL;

    // create audio player using a NULL output mix
    SLInterfaceID ids[1] = {SL_IID_BUFFERQUEUE};
    SLboolean req[1] = {SL_BOOLEAN_TRUE};
    SLObjectItf playerObject;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    assert(NULL == playerObject);

    // create audio player using an engine as the output mix
    loc_outmix.outputMix = engineObject;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    assert(NULL == playerObject);

    // create output mix but don't realize it yet
    SLObjectItf outputMixObject;
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);

    // create audio player using the unrealized output mix
    loc_outmix.outputMix = outputMixObject;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_PRECONDITIONS_VIOLATED == result);
    assert(NULL == playerObject);

    // now realize the output mix
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);

    // create audio player using the realized output mix
    // and a bogus data format for the sink (ignored per spec)
    audioSnk.pFormat = (void *) 0xDEADBEEF;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_SUCCESS == result);
    assert(NULL != playerObject);
    audioSnk.pFormat = NULL;

    // destroy player
    (*playerObject)->Destroy(playerObject);

    // now try to create audio player using various unusual parameters

    // number of channels
    format_pcm.numChannels = 0;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    assert(NULL == playerObject);
    format_pcm.numChannels = 3;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_CONTENT_UNSUPPORTED == result);
    assert(NULL == playerObject);
    format_pcm.numChannels = 2;

    // sample rate
    format_pcm.samplesPerSec = 0;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    assert(NULL == playerObject);
    format_pcm.samplesPerSec = 1000;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_CONTENT_UNSUPPORTED == result);
    assert(NULL == playerObject);
    format_pcm.samplesPerSec = SL_SAMPLINGRATE_44_1;

    // bits per sample
    format_pcm.bitsPerSample = 17;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    assert(NULL == playerObject);
    format_pcm.bitsPerSample = 24;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_CONTENT_UNSUPPORTED == result);
    assert(NULL == playerObject);
    format_pcm.bitsPerSample = 16;

    // container size
    format_pcm.containerSize = 8;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    assert(NULL == playerObject);
    format_pcm.containerSize = 32;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_CONTENT_UNSUPPORTED == result);
    assert(NULL == playerObject);
    format_pcm.containerSize = 16;

    // channel mask
    format_pcm.channelMask = 0;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_SUCCESS == result);
    assert(NULL != playerObject);
    (*playerObject)->Destroy(playerObject);
    format_pcm.channelMask = SL_SPEAKER_FRONT_CENTER;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    assert(NULL == playerObject);
    format_pcm.channelMask = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT |
            SL_SPEAKER_FRONT_CENTER;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    assert(NULL == playerObject);
    format_pcm.numChannels = 1;
    format_pcm.channelMask = 0;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_SUCCESS == result);
    assert(NULL != playerObject);
    (*playerObject)->Destroy(playerObject);
    format_pcm.channelMask = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    assert(NULL == playerObject);
    format_pcm.numChannels = 2;

    // endianness
    format_pcm.endianness = SL_BYTEORDER_BIGENDIAN;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
#ifdef ANDROID // known bug on SDL
    assert(SL_RESULT_CONTENT_UNSUPPORTED == result);
    assert(NULL == playerObject);
#else
    if (SL_RESULT_CONTENT_UNSUPPORTED != result) {
        printf("ERROR: expected SL_RESULT_CONTENT_UNSUPPORTED\n");
        if (NULL != playerObject)
            (*playerObject)->Destroy(playerObject);
    }
#endif
    format_pcm.endianness = 0;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 1, ids, req);
    assert(SL_RESULT_PARAMETER_INVALID == result);
    assert(NULL == playerObject);
    format_pcm.endianness = SL_BYTEORDER_LITTLEENDIAN;

    // destroy output mix
    (*outputMixObject)->Destroy(outputMixObject);

    // destroy engine
    (*engineObject)->Destroy(engineObject);

    return EXIT_SUCCESS;
}
