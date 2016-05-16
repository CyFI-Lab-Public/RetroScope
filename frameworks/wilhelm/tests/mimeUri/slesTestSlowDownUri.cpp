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

#include <assert.h>
#include <pthread.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/time.h>

#include <SLES/OpenSLES.h>


#define MAX_NUMBER_INTERFACES 3

#define REPETITIONS 4  // 4 repetitions, but will stop the looping before the end

#define INITIAL_RATE 2000 // 2x normal playback speed

// These are extensions to OpenSL ES 1.0.1 values

#define SL_PREFETCHSTATUS_UNKNOWN 0
#define SL_PREFETCHSTATUS_ERROR   (-1)

// Mutex and condition shared with main program to protect prefetch_status

static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t cond = PTHREAD_COND_INITIALIZER;
SLuint32 prefetch_status = SL_PREFETCHSTATUS_UNKNOWN;

/* used to detect errors likely to have occured when the OpenSL ES framework fails to open
 * a resource, for instance because a file URI is invalid, or an HTTP server doesn't respond.
 */
#define PREFETCHEVENT_ERROR_CANDIDATE \
        (SL_PREFETCHEVENT_STATUSCHANGE | SL_PREFETCHEVENT_FILLLEVELCHANGE)

//-----------------------------------------------------------------
//* Exits the application if an error is encountered */
#define CheckErr(x) ExitOnErrorFunc(x,__LINE__)

void ExitOnErrorFunc( SLresult result , int line)
{
    if (SL_RESULT_SUCCESS != result) {
        fprintf(stderr, "%u error code encountered at line %d, exiting\n", result, line);
        exit(EXIT_FAILURE);
    }
}

//-----------------------------------------------------------------
/* PlayItf callback for an audio player */
void PlayEventCallback( SLPlayItf caller,  void *pContext, SLuint32 event)
{
    fprintf(stdout, "PlayEventCallback event = ");
    if (event & SL_PLAYEVENT_HEADATEND) {
        fprintf(stdout, "SL_PLAYEVENT_HEADATEND \n");
        /* slow playback down by 2x for next loop,  if possible */
        SLpermille minRate, maxRate, stepSize, rate = 1000;
        SLuint32 capa;
        assert(NULL != pContext);
        SLPlaybackRateItf pRateItf = (SLPlaybackRateItf)pContext;
        SLresult res = (*pRateItf)->GetRate(pRateItf, &rate); CheckErr(res);
        res = (*pRateItf)->GetRateRange(pRateItf, 0, &minRate, &maxRate, &stepSize, &capa);
        CheckErr(res);
        fprintf(stdout, "old rate = %d, minRate=%d, maxRate=%d\n", rate, minRate, maxRate);
        rate /= 2;
        if (rate < minRate) {
            rate = minRate;
        }
        fprintf(stdout, "new rate = %d\n", rate);
        res = (*pRateItf)->SetRate(pRateItf, rate); CheckErr(res);
        if (res == SL_RESULT_FEATURE_UNSUPPORTED) {
            fprintf(stderr, "new playback rate %d per mille is unsupported\n", rate);
        } else {
            CheckErr(res);
        }
    }
    if (event & SL_PLAYEVENT_HEADATMARKER) {
        fprintf(stdout, "SL_PLAYEVENT_HEADATMARKER ");
    }
    if (event & SL_PLAYEVENT_HEADATNEWPOS) {
        fprintf(stdout, "SL_PLAYEVENT_HEADATNEWPOS ");
    }
    if (event & SL_PLAYEVENT_HEADMOVING) {
        fprintf(stdout, "SL_PLAYEVENT_HEADMOVING ");
    }
    if (event & SL_PLAYEVENT_HEADSTALLED) {
        fprintf(stdout, "SL_PLAYEVENT_HEADSTALLED");
    }
    fprintf(stdout, "\n");
}

//-----------------------------------------------------------------
/* PrefetchStatusItf callback for an audio player */
void PrefetchEventCallback( SLPrefetchStatusItf caller,  void *pContext, SLuint32 event)
{
    //fprintf(stdout, "\t\tPrefetchEventCallback: received event %u\n", event);
    SLresult result;
    assert(pContext == NULL);
    SLpermille level = 0;
    result = (*caller)->GetFillLevel(caller, &level);
    CheckErr(result);
    SLuint32 status;
    result = (*caller)->GetPrefetchStatus(caller, &status);
    CheckErr(result);
    if (event & SL_PREFETCHEVENT_FILLLEVELCHANGE) {
        fprintf(stdout, "\t\tPrefetchEventCallback: Buffer fill level is = %d\n", level);
    }
    if (event & SL_PREFETCHEVENT_STATUSCHANGE) {
        fprintf(stdout, "\t\tPrefetchEventCallback: Prefetch Status is = %u\n", status);
    }
    SLuint32 new_prefetch_status;
    if ((event & PREFETCHEVENT_ERROR_CANDIDATE) == PREFETCHEVENT_ERROR_CANDIDATE
            && level == 0 && status == SL_PREFETCHSTATUS_UNDERFLOW) {
        fprintf(stdout, "\t\tPrefetchEventCallback: Error while prefetching data, exiting\n");
        new_prefetch_status = SL_PREFETCHSTATUS_ERROR;
    } else if (event == SL_PREFETCHEVENT_STATUSCHANGE &&
            status == SL_PREFETCHSTATUS_SUFFICIENTDATA) {
        new_prefetch_status = status;
    } else {
        return;
    }
    int ok;
    ok = pthread_mutex_lock(&mutex);
    assert(ok == 0);
    prefetch_status = new_prefetch_status;
    ok = pthread_cond_signal(&cond);
    assert(ok == 0);
    ok = pthread_mutex_unlock(&mutex);
    assert(ok == 0);
}

// Display rate capabilities in a nicely formatted way

void printCapabilities(SLuint32 capabilities)
{
    bool needBar = false;
    printf("0x%x (", capabilities);
#define _(x)                             \
    if (capabilities & SL_RATEPROP_##x) { \
        if (needBar)                     \
            printf("|");                 \
        printf("SL_RATEPROP_" #x);        \
        needBar = true;                  \
        capabilities &= ~SL_RATEPROP_##x; \
    }
    _(SILENTAUDIO)
    _(STAGGEREDAUDIO)
    _(NOPITCHCORAUDIO)
    _(PITCHCORAUDIO)
    if (capabilities != 0) {
        if (needBar)
            printf("|");
        printf("0x%x", capabilities);
        needBar = true;
    }
    if (!needBar)
        printf("N/A");
    printf(")");
}

//-----------------------------------------------------------------

/* Play some music from a URI  */
void TestSlowDownUri( SLObjectItf sl, const char* path)
{
    SLEngineItf                EngineItf;

    SLint32                    numOutputs = 0;
    SLuint32                   deviceID = 0;

    SLresult                   res;

    SLDataSource               audioSource;
    SLDataLocator_URI          uri;
    SLDataFormat_MIME          mime;

    SLDataSink                 audioSink;
    SLDataLocator_OutputMix    locator_outputmix;

    SLObjectItf                player;
    SLPlayItf                  playItf;
    SLSeekItf                  seekItf;
    SLPrefetchStatusItf        prefetchItf;
    SLPlaybackRateItf          rateItf;

    SLObjectItf                OutputMix;

    SLboolean required[MAX_NUMBER_INTERFACES];
    SLInterfaceID iidArray[MAX_NUMBER_INTERFACES];

    /* Get the SL Engine Interface which is implicit */
    res = (*sl)->GetInterface(sl, SL_IID_ENGINE, (void*)&EngineItf);   CheckErr(res);

    /* Initialize arrays required[] and iidArray[] */
    for (int i=0 ; i < MAX_NUMBER_INTERFACES ; i++) {
        required[i] = SL_BOOLEAN_FALSE;
        iidArray[i] = SL_IID_NULL;
    }

    required[0] = SL_BOOLEAN_TRUE;
    iidArray[0] = SL_IID_VOLUME;
    // Create Output Mix object to be used by player
    res = (*EngineItf)->CreateOutputMix(EngineItf, &OutputMix, 0,
            iidArray, required);  CheckErr(res);

    // Realizing the Output Mix object in synchronous mode.
    res = (*OutputMix)->Realize(OutputMix, SL_BOOLEAN_FALSE);  CheckErr(res);

    /* Setup the data source structure for the URI */
    uri.locatorType = SL_DATALOCATOR_URI;
    uri.URI         =  (SLchar*) path;
    mime.formatType    = SL_DATAFORMAT_MIME;
    mime.mimeType      = (SLchar*)NULL;
    mime.containerType = SL_CONTAINERTYPE_UNSPECIFIED;

    audioSource.pFormat  = (void *)&mime;
    audioSource.pLocator = (void *)&uri;

    /* Setup the data sink structure */
    locator_outputmix.locatorType = SL_DATALOCATOR_OUTPUTMIX;
    locator_outputmix.outputMix   = OutputMix;
    audioSink.pLocator            = (void *)&locator_outputmix;
    audioSink.pFormat             = NULL;

    /******************************************************/
    /* Create the audio player */
    required[0] = SL_BOOLEAN_TRUE;
    iidArray[0] = SL_IID_SEEK;
    required[1] = SL_BOOLEAN_TRUE;
    iidArray[1] = SL_IID_PREFETCHSTATUS;
    required[2] = SL_BOOLEAN_TRUE;
    iidArray[2] = SL_IID_PLAYBACKRATE;
    res = (*EngineItf)->CreateAudioPlayer(EngineItf, &player, &audioSource, &audioSink,
            MAX_NUMBER_INTERFACES, iidArray, required); CheckErr(res);

    /* Realizing the player in synchronous mode. */
    res = (*player)->Realize(player, SL_BOOLEAN_FALSE); CheckErr(res);
    fprintf(stdout, "URI example: after Realize\n");

    /* Get interfaces */
    res = (*player)->GetInterface(player, SL_IID_PLAY, (void*)&playItf);  CheckErr(res);

    res = (*player)->GetInterface(player, SL_IID_SEEK,  (void*)&seekItf);  CheckErr(res);

    res = (*player)->GetInterface(player, SL_IID_PLAYBACKRATE, (void*)&rateItf);  CheckErr(res);

    res = (*player)->GetInterface(player, SL_IID_PREFETCHSTATUS, (void*)&prefetchItf);
    CheckErr(res);
    res = (*prefetchItf)->RegisterCallback(prefetchItf, PrefetchEventCallback, NULL);
    CheckErr(res);
    res = (*prefetchItf)->SetCallbackEventsMask(prefetchItf,
            SL_PREFETCHEVENT_FILLLEVELCHANGE | SL_PREFETCHEVENT_STATUSCHANGE);  CheckErr(res);

    /* Configure fill level updates every 5 percent */
    (*prefetchItf)->SetFillUpdatePeriod(prefetchItf, 50);  CheckErr(res);

    /* Display duration */
    SLmillisecond durationInMsec = SL_TIME_UNKNOWN;
    res = (*playItf)->GetDuration(playItf, &durationInMsec);  CheckErr(res);
    if (durationInMsec == SL_TIME_UNKNOWN) {
        fprintf(stdout, "Content duration is unknown (before starting to prefetch)\n");
    } else {
        fprintf(stdout, "Content duration is %u ms (before starting to prefetch)\n",
                durationInMsec);
    }

    /* Loop on the whole of the content */
    res = (*seekItf)->SetLoop(seekItf, SL_BOOLEAN_TRUE, 0, SL_TIME_UNKNOWN);  CheckErr(res);

    /* Set up marker and position callbacks */
    res = (*playItf)->RegisterCallback(playItf, PlayEventCallback, (void *) rateItf);
            CheckErr(res);
    res = (*playItf)->SetCallbackEventsMask(playItf,
            SL_PLAYEVENT_HEADATEND | SL_PLAYEVENT_HEADATMARKER | SL_PLAYEVENT_HEADATNEWPOS);
    res = (*playItf)->SetMarkerPosition(playItf, 1500); CheckErr(res);
    res = (*playItf)->SetPositionUpdatePeriod(playItf, 500); CheckErr(res);

    /* Get the default rate */
    SLpermille rate = 1234;
    res = (*rateItf)->GetRate(rateItf, &rate); CheckErr(res);
    printf("default rate = %d per mille\n", rate);
    assert(1000 == rate);

    /* Get the default rate properties */
    SLuint32 properties = 0;
    res = (*rateItf)->GetProperties(rateItf, &properties); CheckErr(res);
    printf("default rate properties: ");
    printCapabilities(properties);
    printf("\n");
    assert(SL_RATEPROP_NOPITCHCORAUDIO == properties);

    /* Get all supported playback rate ranges */
    SLuint8 index;
    for (index = 0; ; ++index) {
        SLpermille minRate, maxRate, stepSize;
        SLuint32 capabilities;
        res = (*rateItf)->GetRateRange(rateItf, index, &minRate, &maxRate, &stepSize,
                &capabilities);
        if (res == SL_RESULT_PARAMETER_INVALID) {
            if (index == 0) {
                fprintf(stderr, "implementation supports no rate ranges\n");
            }
            break;
        }
        CheckErr(res);
        if (index == 255) {
            fprintf(stderr, "implementation supports way too many rate ranges, I'm giving up\n");
            break;
        }
        printf("range[%u]: min=%d, max=%d, capabilities=", index, minRate, maxRate);
        printCapabilities(capabilities);
        printf("\n");
    }

    /* Change the playback rate before playback */
    res = (*rateItf)->SetRate(rateItf, INITIAL_RATE);
    if (res == SL_RESULT_FEATURE_UNSUPPORTED || res == SL_RESULT_PARAMETER_INVALID) {
        fprintf(stderr, "initial playback rate %d per mille is unsupported\n", INITIAL_RATE);
    } else {
        CheckErr(res);
    }

    /******************************************************/
    /* Play the URI */
    /*     first cause the player to prefetch the data */
    res = (*playItf)->SetPlayState( playItf, SL_PLAYSTATE_PAUSED ); CheckErr(res);

    // wait for prefetch status callback to indicate either sufficient data or error
    pthread_mutex_lock(&mutex);
    while (prefetch_status == SL_PREFETCHSTATUS_UNKNOWN) {
        pthread_cond_wait(&cond, &mutex);
    }
    pthread_mutex_unlock(&mutex);
    if (prefetch_status == SL_PREFETCHSTATUS_ERROR) {
        fprintf(stderr, "Error during prefetch, exiting\n");
        goto destroyRes;
    }

    /* Display duration again, */
    res = (*playItf)->GetDuration(playItf, &durationInMsec); CheckErr(res);
    if (durationInMsec == SL_TIME_UNKNOWN) {
        fprintf(stdout, "Content duration is unknown (after prefetch completed)\n");
    } else {
        fprintf(stdout, "Content duration is %u ms (after prefetch completed)\n", durationInMsec);
    }

    /* Start playing */
    fprintf(stdout, "starting to play\n");
    res = (*playItf)->SetPlayState( playItf, SL_PLAYSTATE_PLAYING ); CheckErr(res);

    /* Wait as long as the duration of the content, times the repetitions,
     * before stopping the loop */
#if 1
    usleep( (REPETITIONS-1) * durationInMsec * 1100);
#else
    int ii;
    for (ii = 0; ii < REPETITIONS; ++ii) {
        usleep(durationInMsec * 1100);
        PlayEventCallback(playItf, (void *) rateItf, SL_PLAYEVENT_HEADATEND);
    }
#endif

    res = (*seekItf)->SetLoop(seekItf, SL_BOOLEAN_FALSE, 0, SL_TIME_UNKNOWN); CheckErr(res);
    fprintf(stdout, "As of now, stopped looping (sound shouldn't repeat from now on)\n");
    /* wait some more to make sure it doesn't repeat */
    usleep(durationInMsec * 1000);

    /* Stop playback */
    fprintf(stdout, "stopping playback\n");
    res = (*playItf)->SetPlayState(playItf, SL_PLAYSTATE_STOPPED); CheckErr(res);

destroyRes:

    /* Destroy the player */
    (*player)->Destroy(player);

    /* Destroy Output Mix object */
    (*OutputMix)->Destroy(OutputMix);
}

//-----------------------------------------------------------------
int main(int argc, char* const argv[])
{
    SLresult    res;
    SLObjectItf sl;

    fprintf(stdout, "OpenSL ES test %s: exercises SLPlayItf, SLSeekItf, SLPlaybackRateItf\n",
            argv[0]);
    fprintf(stdout, "and AudioPlayer with SLDataLocator_URI source / OutputMix sink\n");
    fprintf(stdout, "Plays a sound and loops it %d times while changing the \n", REPETITIONS);
    fprintf(stdout, "playback rate each time.\n\n");

    if (argc == 1) {
        fprintf(stdout, "Usage: \n\t%s path \n\t%s url\n", argv[0], argv[0]);
        fprintf(stdout, "Example: \"%s /sdcard/my.mp3\"  or \"%s file:///sdcard/my.mp3\"\n",
                argv[0], argv[0]);
        return EXIT_FAILURE;
    }

    SLEngineOption EngineOption[] = {
            {(SLuint32) SL_ENGINEOPTION_THREADSAFE,
            (SLuint32) SL_BOOLEAN_TRUE}};

    res = slCreateEngine( &sl, 1, EngineOption, 0, NULL, NULL);
    CheckErr(res);
    /* Realizing the SL Engine in synchronous mode. */
    res = (*sl)->Realize(sl, SL_BOOLEAN_FALSE);
    CheckErr(res);

    TestSlowDownUri(sl, argv[1]);

    /* Shutdown OpenSL ES */
    (*sl)->Destroy(sl);

    return EXIT_SUCCESS;
}
