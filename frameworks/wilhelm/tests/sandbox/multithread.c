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

// Multiple threads create and destroy objects

#include <SLES/OpenSLES.h>
#include <assert.h>
#include <pthread.h>
//#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

typedef struct {
    SLuint32 mObjectID;
    SLchar *mURI;
    SLEngineItf mEngineEngine;
    SLObjectItf mMixObject;
    SLuint32 mCounter;
} ThreadArgument;

volatile int timeToExit = 0;
#define MAX_THREAD 10
pthread_t threads[MAX_THREAD];
ThreadArgument thread_args[MAX_THREAD];

pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

void *thread_start(void *param)
{
    //pthread_mutex_lock(&mutex);
    //pthread_mutex_unlock(&mutex);
    ThreadArgument *ta = (ThreadArgument *) param;

    while (!timeToExit) {
        SLresult result;

        ++ta->mCounter;
        switch (ta->mObjectID) {
        case SL_OBJECTID_OUTPUTMIX:
            {
            SLObjectItf myMixObject;
            result = (*ta->mEngineEngine)->CreateOutputMix(ta->mEngineEngine, &myMixObject, 0, NULL,
                    NULL);
            assert(SL_RESULT_SUCCESS == result);
            result = (*myMixObject)->Realize(myMixObject, SL_BOOLEAN_FALSE);
            assert(SL_RESULT_SUCCESS == result);
            (*myMixObject)->Destroy(myMixObject);
            }
            break;

        case SL_OBJECTID_AUDIOPLAYER:
            {
            SLDataLocator_URI locURI = {SL_DATALOCATOR_URI, ta->mURI};
            SLDataFormat_MIME dfMIME = {SL_DATAFORMAT_MIME, NULL, SL_CONTAINERTYPE_UNSPECIFIED};
            SLDataSource audioSrc = {&locURI, &dfMIME};
            SLDataLocator_OutputMix locOutputMix = {SL_DATALOCATOR_OUTPUTMIX, ta->mMixObject};
            SLDataSink audioSnk = {&locOutputMix, NULL};
            SLObjectItf myPlayerObject;
            result = (*ta->mEngineEngine)->CreateAudioPlayer(ta->mEngineEngine, &myPlayerObject,
                    &audioSrc, &audioSnk, 0, NULL, NULL);
            assert(SL_RESULT_SUCCESS == result);
            result = (*myPlayerObject)->Realize(myPlayerObject, SL_BOOLEAN_FALSE);
            assert(SL_RESULT_SUCCESS == result);
            SLPlayItf playerPlay;
            result = (*myPlayerObject)->GetInterface(myPlayerObject, SL_IID_PLAY, &playerPlay);
            assert(SL_RESULT_SUCCESS == result);
            result = (*playerPlay)->SetPlayState(playerPlay, SL_PLAYSTATE_PAUSED);
            assert(SL_RESULT_SUCCESS == result);
            usleep(1000 + (rand() & 0xFFF));
            //usleep(1000);
            //sleep(1);
            (*myPlayerObject)->Destroy(myPlayerObject);
            }
            break;

        default:
            break;

        }
        //usleep(100000);
        //break;
    }

    return NULL;
}


//const char * const uris[4] = {"wav/frog.wav", "wav/bach.wav", "wav/8days.wav", "wav/help16.wav"};
const char * const uris[4] = {"wav/frog.wav", "wav/frog.wav", "wav/frog.wav", "wav/frog.wav"};

// Main program

int main(int argc, char **argv)
{
    SLresult result;

    // create engine
    SLObjectItf engineObject;
    result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    SLEngineItf engineEngine;
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    assert(SL_RESULT_SUCCESS == result);

    // create output mix
    SLObjectItf mixObject;
    result = (*engineEngine)->CreateOutputMix(engineEngine, &mixObject, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);
    result = (*mixObject)->Realize(mixObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);

    // create threads
    int i;
    int ok;
    for (i = 0; i < MAX_THREAD; ++i) {
        ThreadArgument *ta = &thread_args[i];
        int r = rand();
        switch (r & 1) {
#if 0
        case 0:
            ta->mObjectID = SL_OBJECTID_OUTPUTMIX;
            ta->mURI = NULL;
            ta->mEngineEngine = engineEngine;
            ta->mMixObject = NULL;
            ta->mCounter = 0;
            break;
        case 1:
#endif
        default:
            ta->mObjectID = SL_OBJECTID_AUDIOPLAYER;
            ta->mURI = (SLchar *) uris[(r >> 1) & 3];
            ta->mEngineEngine = engineEngine;
            ta->mMixObject = mixObject;
            ta->mCounter = 0;
            break;
        }
        //pthread_mutex_lock(&mutex);
        //pthread_mutex_unlock(&mutex);
        ok = pthread_create(&threads[i], (const pthread_attr_t *) NULL, thread_start,
                &thread_args[i]);
        assert(0 == ok);
    }

    // let it run for a while
    int j;
    for (j = 0; j < 100; ++j) {
        sleep(1);
        for (i = 0; i < MAX_THREAD; ++i) {
            ThreadArgument *ta = &thread_args[i];
            printf("[%d]=%u ", j, ta->mCounter);
        }
        printf("\n");
    }

    // signal threads that they should exit
    timeToExit = 1;

    for (j = 0; j < 3; ++j) {
        sleep(1);
        for (i = 0; i < MAX_THREAD; ++i) {
            ThreadArgument *ta = &thread_args[i];
            printf("[%d]=%u ", j, ta->mCounter);
        }
        printf("\n");
    }

    // now wait for the threads to actually exit
    for (i = 0; i < MAX_THREAD; ++i) {
        ok = pthread_join(threads[i], NULL);
        assert(0 == ok);
    }

    // tear down objects
    (*mixObject)->Destroy(mixObject);
    (*engineObject)->Destroy(engineObject);

    return EXIT_SUCCESS;
}
