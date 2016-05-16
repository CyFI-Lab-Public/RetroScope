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

// Test audio player configurations with URI data source and MIME data format

#include <assert.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#include <SLES/OpenSLES.h>
#ifdef ANDROID
#include <SLES/OpenSLES_Android.h>
#endif

int main(int argc, char **argv)
{
    if (argc != 2) {
        fprintf(stderr, "usage: %s URI\n", argv[0]);
        return EXIT_FAILURE;
    }

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

    // configure audio source
    SLDataLocator_URI loc_uri;
    loc_uri.locatorType = SL_DATALOCATOR_URI;
    loc_uri.URI = (SLchar *) argv[1];
    SLDataFormat_MIME format_mime;
    format_mime.formatType = SL_DATAFORMAT_MIME;
    format_mime.mimeType = NULL;
    format_mime.containerType = SL_CONTAINERTYPE_UNSPECIFIED;
    SLDataSource audioSrc;
    audioSrc.pLocator = &loc_uri;
    audioSrc.pFormat = &format_mime;

    // configure audio sink
    SLDataLocator_OutputMix loc_outmix;
    loc_outmix.locatorType = SL_DATALOCATOR_OUTPUTMIX;
    loc_outmix.outputMix = outputMixObject;
    SLDataSink audioSnk;
    audioSnk.pLocator = &loc_outmix;
    audioSnk.pFormat = NULL;

    // create audio player, requesting a buffer queue interface
    SLuint32 numInterfaces = 1;
    SLInterfaceID ids[1];
    SLboolean req[1];
    ids[0] = SL_IID_BUFFERQUEUE;
    req[0] = SL_BOOLEAN_TRUE;
    SLObjectItf playerObject;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, numInterfaces, ids, req);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
    assert(NULL == playerObject);
#ifdef ANDROID
    ids[0] = SL_IID_ANDROIDSIMPLEBUFFERQUEUE;
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, numInterfaces, ids, req);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
    assert(NULL == playerObject);
#endif

    // create audio player, without requesting a buffer queue interface
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audioSrc,
            &audioSnk, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);

    // realize the player
    result = (*playerObject)->Realize(playerObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);

    // get the play interface
    SLPlayItf playerPlay;
    result = (*playerObject)->GetInterface(playerObject, SL_IID_PLAY, &playerPlay);
    assert(SL_RESULT_SUCCESS == result);

    // get the buffer queue interface
    SLBufferQueueItf playerBufferQueue;
    result = (*playerObject)->GetInterface(playerObject, SL_IID_BUFFERQUEUE, &playerBufferQueue);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
    assert(NULL == playerBufferQueue);
#ifdef ANDROID
    SLAndroidSimpleBufferQueueItf playerAndroidSimpleBufferQueue;
    result = (*playerObject)->GetInterface(playerObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
            &playerAndroidSimpleBufferQueue);
    assert(SL_RESULT_FEATURE_UNSUPPORTED == result);
    assert(NULL == playerAndroidSimpleBufferQueue);
#endif

    // get the player duration
    SLmillisecond duration;
    result = (*playerPlay)->GetDuration(playerPlay, &duration);
    assert(SL_RESULT_SUCCESS == result);
    if (SL_TIME_UNKNOWN == duration)
        printf("Duration: unknown\n");
    else
        printf("Duration: %.1f\n", duration / 1000.0f);

    // set the player's state to playing
    result = (*playerPlay)->SetPlayState(playerPlay, SL_PLAYSTATE_PLAYING);
    assert(SL_RESULT_SUCCESS == result);

    // wait for the playback to finish
    for (;;) {
        SLuint32 playState;
        result = (*playerPlay)->GetPlayState(playerPlay, &playState);
        assert(SL_RESULT_SUCCESS == result);
        if (SL_PLAYSTATE_PLAYING != playState) {
            break;
        }
        usleep(10000);
    }

    // get the player duration
    result = (*playerPlay)->GetDuration(playerPlay, &duration);
    assert(SL_RESULT_SUCCESS == result);
    if (SL_TIME_UNKNOWN == duration)
        printf("Duration: unknown\n");
    else
        printf("Duration: %.1f\n", duration / 1000.0f);

    // destroy audio player
    (*playerObject)->Destroy(playerObject);

    // destroy output mix
    (*outputMixObject)->Destroy(outputMixObject);

    // destroy engine
    (*engineObject)->Destroy(engineObject);

    return EXIT_SUCCESS;
}
