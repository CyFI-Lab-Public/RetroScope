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

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <utils/threads.h>

#include <SLES/OpenSLES.h>

/* tolerance in ms for this test in time difference between reported position and time since
 * playback was requested to start. This is reasonable for a local file.
 */
#define TIME_TOLERANCE_MS 600

/* explicitly requesting SL_IID_VOLUME and SL_IID_PREFETCHSTATUS
 * on the AudioPlayer object */
#define NUM_EXPLICIT_INTERFACES_FOR_PLAYER 2

/* used to detect errors likely to have occured when the OpenSL ES framework fails to open
 * a resource, for instance because a file URI is invalid, or an HTTP server doesn't respond. */
#define PREFETCHEVENT_ERROR_CANDIDATE \
        (SL_PREFETCHEVENT_STATUSCHANGE | SL_PREFETCHEVENT_FILLLEVELCHANGE)

/* to signal to the test app the end of the stream to decode has been reached */
bool eos = false;
android::Mutex eosLock;
android::Condition eosCondition;

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

bool prefetchError = false;

//-----------------------------------------------------------------
void SignalEos() {
    android::Mutex::Autolock autoLock(eosLock);
    eos = true;
    eosCondition.signal();
}

//-----------------------------------------------------------------
/* PrefetchStatusItf callback for an audio player */
void PrefetchEventCallback( SLPrefetchStatusItf caller,  void *pContext, SLuint32 event)
{
    SLpermille level = 0;
    SLresult res = (*caller)->GetFillLevel(caller, &level); CheckErr(res);
    SLuint32 status;
    //fprintf(stdout, "PrefetchEventCallback: received event %u\n", event);
    res = (*caller)->GetPrefetchStatus(caller, &status); CheckErr(res);
    if ((PREFETCHEVENT_ERROR_CANDIDATE == (event & PREFETCHEVENT_ERROR_CANDIDATE))
            && (level == 0) && (status == SL_PREFETCHSTATUS_UNDERFLOW)) {
        fprintf(stdout, "PrefetchEventCallback: Error while prefetching data, exiting\n");
        prefetchError = true;
        return;
    }
    if (event & SL_PREFETCHEVENT_FILLLEVELCHANGE) {
        fprintf(stdout, "PrefetchEventCallback: Buffer fill level is = %d\n", level);
    }
    if (event & SL_PREFETCHEVENT_STATUSCHANGE) {
        fprintf(stdout, "PrefetchEventCallback: Prefetch Status is = %u\n", status);
    }
}


//-----------------------------------------------------------------
/* PlayItf callback for playback events */
void PlayEventCallback(
        SLPlayItf caller,
        void *pContext,
        SLuint32 event)
{
    SLmillisecond posMsec = SL_TIME_UNKNOWN;
    SLresult res;
    if (SL_PLAYEVENT_HEADATEND & event) {
        fprintf(stdout, "SL_PLAYEVENT_HEADATEND reached\n");
#if 0
        res = (*caller)->GetPosition(caller, &posMsec); CheckErr(res);
        fprintf(stdout, "after getPosition in SL_PLAYEVENT_HEADATEND handler\n");
        if (posMsec == SL_TIME_UNKNOWN) {
            fprintf(stderr, "Error: position is SL_TIME_UNKNOWN at SL_PLAYEVENT_HEADATEND\n");
        } else {
            fprintf(stdout, "position is %d at SL_PLAYEVENT_HEADATEND\n", posMsec);
        }
        // FIXME compare position against duration
#endif
        SignalEos();
    }

    if (SL_PLAYEVENT_HEADATNEWPOS & event) {
        res = (*caller)->GetPosition(caller, &posMsec); CheckErr(res);
        fprintf(stdout, "SL_PLAYEVENT_HEADATNEWPOS current position=%ums\n", posMsec);
    }

    if (SL_PLAYEVENT_HEADATMARKER & event) {
        res = (*caller)->GetPosition(caller, &posMsec); CheckErr(res);
        fprintf(stdout, "SL_PLAYEVENT_HEADATMARKER current position=%ums\n", posMsec);
    }
}


//-----------------------------------------------------------------

/* Play some audio from a URI and regularly query the position */
void TestGetPositionUri( SLObjectItf sl, const char* path)
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
    SLVolumeItf                volItf;
    SLPrefetchStatusItf        prefetchItf;

    SLObjectItf                OutputMix;

    /* variables for the duration and position tests */
    SLuint16      counter = 0;
    SLmillisecond posInMsec = SL_TIME_UNKNOWN;
    SLmillisecond durationInMsec = SL_TIME_UNKNOWN;

    SLboolean required[NUM_EXPLICIT_INTERFACES_FOR_PLAYER];
    SLInterfaceID iidArray[NUM_EXPLICIT_INTERFACES_FOR_PLAYER];

    /* Get the SL Engine Interface which is implicit */
    res = (*sl)->GetInterface(sl, SL_IID_ENGINE, (void*)&EngineItf);
    CheckErr(res);

    /* Initialize arrays required[] and iidArray[] */
    for (int i=0 ; i < NUM_EXPLICIT_INTERFACES_FOR_PLAYER ; i++) {
        required[i] = SL_BOOLEAN_FALSE;
        iidArray[i] = SL_IID_NULL;
    }

    // Set arrays required[] and iidArray[] for VOLUME and PREFETCHSTATUS interface
    required[0] = SL_BOOLEAN_TRUE;
    iidArray[0] = SL_IID_VOLUME;
    required[1] = SL_BOOLEAN_TRUE;
    iidArray[1] = SL_IID_PREFETCHSTATUS;
    // Create Output Mix object to be used by player
    res = (*EngineItf)->CreateOutputMix(EngineItf, &OutputMix, 0,
            iidArray, required); CheckErr(res);

    // Realizing the Output Mix object in synchronous mode.
    res = (*OutputMix)->Realize(OutputMix, SL_BOOLEAN_FALSE);
    CheckErr(res);

    /* Setup the data source structure for the URI */
    uri.locatorType = SL_DATALOCATOR_URI;
    uri.URI         =  (SLchar*) path;
    mime.formatType    = SL_DATAFORMAT_MIME;
    mime.mimeType      = (SLchar*)NULL;
    mime.containerType = SL_CONTAINERTYPE_UNSPECIFIED;

    audioSource.pFormat      = (void *)&mime;
    audioSource.pLocator     = (void *)&uri;

    /* Setup the data sink structure */
    locator_outputmix.locatorType   = SL_DATALOCATOR_OUTPUTMIX;
    locator_outputmix.outputMix    = OutputMix;
    audioSink.pLocator           = (void *)&locator_outputmix;
    audioSink.pFormat            = NULL;

    /* Create the audio player */
    res = (*EngineItf)->CreateAudioPlayer(EngineItf, &player, &audioSource, &audioSink,
            NUM_EXPLICIT_INTERFACES_FOR_PLAYER, iidArray, required); CheckErr(res);

    /* Realizing the player in synchronous mode. */
    res = (*player)->Realize(player, SL_BOOLEAN_FALSE); CheckErr(res);
    fprintf(stdout, "URI example: after Realize\n");

    /* Get interfaces */
    res = (*player)->GetInterface(player, SL_IID_PLAY, (void*)&playItf);
    CheckErr(res);

    res = (*player)->GetInterface(player, SL_IID_VOLUME,  (void*)&volItf);
    CheckErr(res);

    res = (*player)->GetInterface(player, SL_IID_PREFETCHSTATUS, (void*)&prefetchItf);
    CheckErr(res);
    res = (*prefetchItf)->RegisterCallback(prefetchItf, PrefetchEventCallback, &prefetchItf);
    CheckErr(res);
    res = (*prefetchItf)->SetCallbackEventsMask(prefetchItf,
            SL_PREFETCHEVENT_FILLLEVELCHANGE | SL_PREFETCHEVENT_STATUSCHANGE);
    CheckErr(res);

    /* Configure fill level updates every 5 percent */
    res = (*prefetchItf)->SetFillUpdatePeriod(prefetchItf, 50); CheckErr(res);

    /* Set up the player callback to get events during the decoding */
    res = (*playItf)->SetMarkerPosition(playItf, 2000);
    CheckErr(res);
    res = (*playItf)->SetPositionUpdatePeriod(playItf, 500);
    CheckErr(res);
    res = (*playItf)->SetCallbackEventsMask(playItf,
            SL_PLAYEVENT_HEADATMARKER | SL_PLAYEVENT_HEADATNEWPOS | SL_PLAYEVENT_HEADATEND);
    CheckErr(res);
    res = (*playItf)->RegisterCallback(playItf, PlayEventCallback, NULL);
    CheckErr(res);

    /* Set the player volume */
    res = (*volItf)->SetVolumeLevel( volItf, -300);
    CheckErr(res);

    /* Play the URI */
    /*     first cause the player to prefetch the data */
    fprintf(stdout, "Setting the player to PAUSED to cause it to prefetch the data\n");
    res = (*playItf)->SetPlayState( playItf, SL_PLAYSTATE_PAUSED ); CheckErr(res);

    usleep(100 * 1000);
    /*     wait until there's data to play */
    SLuint32 prefetchStatus = SL_PREFETCHSTATUS_UNDERFLOW;
    SLuint32 timeOutIndex = 100; // 10s
    while ((prefetchStatus != SL_PREFETCHSTATUS_SUFFICIENTDATA) && (timeOutIndex > 0) &&
            !prefetchError) {
        usleep(100 * 1000);
        (*prefetchItf)->GetPrefetchStatus(prefetchItf, &prefetchStatus);
        timeOutIndex--;
    }

    if (timeOutIndex == 0 || prefetchError) {
        fprintf(stderr, "We're done waiting, failed to prefetch data in time, exiting\n");
        goto destroyRes;
    }

    /* Display duration */
    res = (*playItf)->GetDuration(playItf, &durationInMsec); CheckErr(res);
    if (durationInMsec == SL_TIME_UNKNOWN) {
        fprintf(stderr, "Error: Content duration is unknown after prefetch completed, exiting\n");
        goto destroyRes;
    } else {
        fprintf(stdout, "Content duration is %u ms (after prefetch completed)\n", durationInMsec);
    }

    fprintf(stdout, "Setting the player to PLAYING\n");
    res = (*playItf)->SetPlayState( playItf, SL_PLAYSTATE_PLAYING ); CheckErr(res);

    /* Test GetPosition every second */
    while ((counter*1000) < durationInMsec) {
        counter++;
        usleep(1 * 1000 * 1000); //1s
        res = (*playItf)->GetPosition(playItf, &posInMsec); CheckErr(res);
        if (posInMsec == SL_TIME_UNKNOWN) {
            fprintf(stderr, "Error: position is SL_TIME_UNKNOWN %ds after start, exiting\n",
                    counter);
            goto destroyRes;
        } else {
            fprintf(stderr, "position is %dms %ds after start\n", posInMsec, counter);
        }
        // this test would probably deserve to be improved by relying on drift relative to
        // a clock, as the operations between two consecutive sleep() are taking time as well
        // and can add up
        if (((SLint32)posInMsec > (counter*1000 + TIME_TOLERANCE_MS)) ||
                ((SLint32)posInMsec < (counter*1000 - TIME_TOLERANCE_MS))) {
            fprintf(stderr, "Error: position drifted too much, exiting\n");
            goto destroyRes;
        }
    }

    /* Play until the end of file is reached */
    {
        android::Mutex::Autolock autoLock(eosLock);
        while (!eos) {
            eosCondition.wait(eosLock);
        }
    }
    fprintf(stdout, "EOS signaled, stopping playback\n");
    res = (*playItf)->SetPlayState(playItf, SL_PLAYSTATE_STOPPED); CheckErr(res);

destroyRes:

    /* Destroy the player */
    fprintf(stdout, "Destroying the player\n");
    (*player)->Destroy(player);

    /* Destroy Output Mix object */
    (*OutputMix)->Destroy(OutputMix);
}

//-----------------------------------------------------------------
int main(int argc, char* const argv[])
{
    SLresult    res;
    SLObjectItf sl;

    fprintf(stdout, "OpenSL ES test %s: exercises SLPlayItf", argv[0]);
    fprintf(stdout, "and AudioPlayer with SLDataLocator_URI source / OutputMix sink\n");
    fprintf(stdout, "Plays a sound and requests position at various times\n\n");

    if (argc == 1) {
        fprintf(stdout, "Usage: %s path \n\t%s url\n", argv[0], argv[0]);
        fprintf(stdout, "Example: \"%s /sdcard/my.mp3\"  or \"%s file:///sdcard/my.mp3\"\n",
                argv[0], argv[0]);
        exit(EXIT_FAILURE);
    }

    SLEngineOption EngineOption[] = {
            {(SLuint32) SL_ENGINEOPTION_THREADSAFE,
            (SLuint32) SL_BOOLEAN_TRUE}};

    res = slCreateEngine( &sl, 1, EngineOption, 0, NULL, NULL);
    CheckErr(res);
    /* Realizing the SL Engine in synchronous mode. */
    res = (*sl)->Realize(sl, SL_BOOLEAN_FALSE);
    CheckErr(res);

    TestGetPositionUri(sl, argv[1]);

    /* Shutdown OpenSL ES */
    (*sl)->Destroy(sl);

    return EXIT_SUCCESS;
}
