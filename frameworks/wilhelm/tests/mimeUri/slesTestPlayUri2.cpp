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

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>

#include <SLES/OpenSLES.h>


#define MAX_NUMBER_INTERFACES 3


//-----------------------------------------------------------------
/* Exits the application if an error is encountered */
void ExitOnError( SLresult result )
{
    if (SL_RESULT_SUCCESS != result) {
        fprintf(stdout, "%u error code encountered, exiting\n", result);
        exit(EXIT_FAILURE);
    }
}

//-----------------------------------------------------------------
/* PlayItf callback for an audio player */
void PlayEventCallback( SLPlayItf caller,  void *pContext, SLuint32 event)
{
    fprintf(stdout, "PlayEventCallback event = ");
    if (event & SL_PLAYEVENT_HEADATEND) {
        fprintf(stdout, "SL_PLAYEVENT_HEADATEND ");
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

/* Play two audio URIs, pan them left and right  */
void TestPlayUri( SLObjectItf sl, const char* path, const char* path2)
{
    SLresult  result;
    SLEngineItf EngineItf;

    /* Objects this application uses: two players and an ouput mix */
    SLObjectItf  player, player2, outputMix;

    /* Source of audio data to play, we'll reuse the same source for two different players */
    SLDataSource      audioSource;
    SLDataLocator_URI uri;
    SLDataFormat_MIME mime;

    /* Data sinks for the two audio players */
    SLDataSink               audioSink;
    SLDataLocator_OutputMix  locator_outputmix;

    /* Play, Volume and PrefetchStatus interfaces for the audio players */
    SLPlayItf           playItf, playItf2;
    SLVolumeItf         volItf, volItf2;
    SLPrefetchStatusItf prefetchItf, prefetchItf2;

    SLboolean required[MAX_NUMBER_INTERFACES];
    SLInterfaceID iidArray[MAX_NUMBER_INTERFACES];

    /* Get the SL Engine Interface which is implicit */
    result = (*sl)->GetInterface(sl, SL_IID_ENGINE, (void*)&EngineItf);
    ExitOnError(result);

    /* Initialize arrays required[] and iidArray[] */
    for (int i=0 ; i < MAX_NUMBER_INTERFACES ; i++) {
        required[i] = SL_BOOLEAN_FALSE;
        iidArray[i] = SL_IID_NULL;
    }
    /* Set arrays required[] and iidArray[] for SLVolumeItf and SLPrefetchStatusItf interfaces */
    /*  (SLPlayItf is implicit) */
    required[0] = SL_BOOLEAN_TRUE;
    iidArray[0] = SL_IID_VOLUME;
    required[1] = SL_BOOLEAN_TRUE;
    iidArray[1] = SL_IID_PREFETCHSTATUS;

    /* ------------------------------------------------------ */
    /* Configuration of the output mix  */

    /* Create Output Mix object to be used each player */
     result = (*EngineItf)->CreateOutputMix(EngineItf, &outputMix, 0, iidArray, required);
     ExitOnError(result);

    /* Realize the Output Mix object in synchronous mode */
    result = (*outputMix)->Realize(outputMix, SL_BOOLEAN_FALSE);
    ExitOnError(result);

    /* Setup the data sink structure */
    locator_outputmix.locatorType = SL_DATALOCATOR_OUTPUTMIX;
    locator_outputmix.outputMix   = outputMix;
    audioSink.pLocator            = (void *)&locator_outputmix;
    audioSink.pFormat             = NULL;

    /* ------------------------------------------------------ */
    /* Configuration of the players  */

    /* Setup the data source structure for the first URI */
    uri.locatorType = SL_DATALOCATOR_URI;
    uri.URI         =  (SLchar*) path;
    mime.formatType    = SL_DATAFORMAT_MIME;
    /*     this is how ignored mime information is specified, according to OpenSL ES spec
     *     in 9.1.6 SLDataFormat_MIME and 8.23 SLMetadataTraversalItf GetChildInfo */
    mime.mimeType      = (SLchar*)NULL;
    mime.containerType = SL_CONTAINERTYPE_UNSPECIFIED;

    audioSource.pFormat      = (void *)&mime;
    audioSource.pLocator     = (void *)&uri;

    /* Create the first audio player */
    result = (*EngineItf)->CreateAudioPlayer(EngineItf, &player, &audioSource, &audioSink, 2,
            iidArray, required);
    ExitOnError(result);

    /* Create the second audio player with a different path for its data source */
    uri.URI =  (SLchar*) path2;
    audioSource.pLocator = (void *)&uri;
    result = (*EngineItf)->CreateAudioPlayer(EngineItf, &player2, &audioSource, &audioSink, 2,
            iidArray, required);
    ExitOnError(result);

    /* Realize the players in synchronous mode. */
    result = (*player)->Realize(player, SL_BOOLEAN_FALSE); ExitOnError(result);
    result = (*player)->Realize(player2, SL_BOOLEAN_FALSE); ExitOnError(result);
    //fprintf(stdout, "URI example: after Realize\n");

    /* Get the SLPlayItf, SLVolumeItf and SLPrefetchStatusItf interfaces for each player */
    result = (*player)->GetInterface(player, SL_IID_PLAY, (void*)&playItf);
    ExitOnError(result);
    result = (*player)->GetInterface(player2, SL_IID_PLAY, (void*)&playItf2);
    ExitOnError(result);

    result = (*player)->GetInterface(player, SL_IID_VOLUME, (void*)&volItf);
    ExitOnError(result);
    result = (*player2)->GetInterface(player2, SL_IID_VOLUME, (void*)&volItf2);
    ExitOnError(result);

    result = (*player)->GetInterface(player, SL_IID_PREFETCHSTATUS, (void*)&prefetchItf);
    ExitOnError(result);
    result = (*player2)->GetInterface(player2, SL_IID_PREFETCHSTATUS, (void*)&prefetchItf2);
    ExitOnError(result);

    /*  Setup to receive playback events */
    result = (*playItf)->RegisterCallback(playItf, PlayEventCallback, &playItf);
    ExitOnError(result);
    result = (*playItf)->SetCallbackEventsMask(playItf,
            SL_PLAYEVENT_HEADATEND| SL_PLAYEVENT_HEADATMARKER | SL_PLAYEVENT_HEADATNEWPOS
            | SL_PLAYEVENT_HEADMOVING | SL_PLAYEVENT_HEADSTALLED);
    ExitOnError(result);

    /* Set the player volume */
    result = (*volItf)->SetVolumeLevel( volItf, -300);
    ExitOnError(result);
    /* Pan the first player to the left */
    result = (*volItf)->EnableStereoPosition( volItf, SL_BOOLEAN_TRUE); ExitOnError(result);
    result = (*volItf)->SetStereoPosition( volItf, -1000); ExitOnError(result);
    /* Pan the second player to the right */
    result = (*volItf2)->EnableStereoPosition( volItf2, SL_BOOLEAN_TRUE); ExitOnError(result);
    result = (*volItf2)->SetStereoPosition( volItf2, 1000); ExitOnError(result);

    /* ------------------------------------------------------ */
    /* Playback */

    /* Start the data prefetching by setting the players to the paused state */
    result = (*playItf)->SetPlayState( playItf, SL_PLAYSTATE_PAUSED );
    ExitOnError(result);
    result = (*playItf2)->SetPlayState( playItf2, SL_PLAYSTATE_PAUSED );
    ExitOnError(result);

    /*     wait until there's data to play */
    SLuint32 prefetchStatus = SL_PREFETCHSTATUS_UNDERFLOW;
    while (prefetchStatus != SL_PREFETCHSTATUS_SUFFICIENTDATA) {
        usleep(100 * 1000);
        (*prefetchItf)->GetPrefetchStatus(prefetchItf, &prefetchStatus);
    }
    prefetchStatus = SL_PREFETCHSTATUS_UNDERFLOW;
    while (prefetchStatus != SL_PREFETCHSTATUS_SUFFICIENTDATA) {
        usleep(100 * 1000);
        (*prefetchItf2)->GetPrefetchStatus(prefetchItf2, &prefetchStatus);
    }

    result = (*playItf)->SetPlayState( playItf, SL_PLAYSTATE_PLAYING );
    ExitOnError(result);

    /* Wait 2s before starting the second player */
    usleep(2000 * 1000);
    fprintf(stdout, "URI example: starting to play %s\n", path2);
    result = (*playItf2)->SetPlayState( playItf2, SL_PLAYSTATE_PLAYING );
    ExitOnError(result);

    /* Display duration */
    SLmillisecond durationInMsec = SL_TIME_UNKNOWN;
    result = (*playItf)->GetDuration(playItf, &durationInMsec);
    ExitOnError(result);
    if (durationInMsec == SL_TIME_UNKNOWN) {
        fprintf(stdout, "Content duration of first URI is unknown\n");
    } else {
        fprintf(stdout, "Content duration of first URI is %u ms\n", durationInMsec);
    }

    /* Wait as long as the duration of the first URI + 2s before stopping */
    if (durationInMsec == SL_TIME_UNKNOWN) {
        durationInMsec = 5000; /* arbitrary time when duration is unknown */
    }
    usleep((durationInMsec + 2000) * 1000);

    /* Make sure player is stopped */
    fprintf(stdout, "URI example: stopping playback\n");
    result = (*playItf)->SetPlayState(playItf, SL_PLAYSTATE_STOPPED);
    ExitOnError(result);
    result = (*playItf2)->SetPlayState(playItf2, SL_PLAYSTATE_STOPPED);
    ExitOnError(result);

    /* Destroy the players */
    (*player)->Destroy(player);
    (*player2)->Destroy(player2);

    /* Destroy Output Mix object */
    (*outputMix)->Destroy(outputMix);
}

//-----------------------------------------------------------------
int main(int argc, char* const argv[])
{
    SLresult    result;
    SLObjectItf sl;

    fprintf(stdout, "OpenSL ES test %s: exercises SLPlayItf, SLVolumeItf (incl. stereo position) ",
            argv[0]);
    fprintf(stdout, "and AudioPlayer with SLDataLocator_URI source / OutputMix sink\n");
    fprintf(stdout, "Plays two sounds (or twice the same) and pans them left and right.");
    fprintf(stdout, "Stops after the end of the first + 2s\n");

    if (argc == 1) {
        fprintf(stdout, "Usage: \n\t%s url1 url2 \n\t%s url\n", argv[0], argv[0]);
        fprintf(stdout, "Example: \"%s /sdcard/my.mp3 http://blabla/my.wav\" ", argv[0]);
        fprintf(stdout, "or \"%s file:///sdcard/my.mp3\"\n", argv[0]);
        exit(EXIT_FAILURE);
    }

    SLEngineOption EngineOption[] = {
            {(SLuint32) SL_ENGINEOPTION_THREADSAFE, (SLuint32) SL_BOOLEAN_TRUE}
    };

    result = slCreateEngine( &sl, 1, EngineOption, 0, NULL, NULL);
    ExitOnError(result);

    /* Realizing the SL Engine in synchronous mode. */
    result = (*sl)->Realize(sl, SL_BOOLEAN_FALSE);
    ExitOnError(result);

    if (argc == 2) {
        TestPlayUri(sl, argv[1], argv[1]);
    } else if (argc == 3) {
        TestPlayUri(sl, argv[1], argv[2]);
    }

    /* Shutdown OpenSL ES */
    (*sl)->Destroy(sl);

    return EXIT_SUCCESS;
}
