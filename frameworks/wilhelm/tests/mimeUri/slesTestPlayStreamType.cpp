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
#include <string.h>
#include <unistd.h>
#include <sys/time.h>

#include <SLES/OpenSLES.h>
#ifdef ANDROID
#include <SLES/OpenSLES_Android.h>
#endif


#define MAX_NUMBER_INTERFACES 2


//-----------------------------------------------------------------
/* Exits the application if an error is encountered */
#define ExitOnError(x) ExitOnErrorFunc(x,__LINE__)

void ExitOnErrorFunc( SLresult result , int line)
{
    if (SL_RESULT_SUCCESS != result) {
        fprintf(stdout, "%u error code encountered at line %d, exiting\n", result, line);
        exit(EXIT_FAILURE);
    }
}


//-----------------------------------------------------------------

/* Play an audio URIs on the given stream type  */
void TestStreamTypeConfiguration( SLObjectItf sl, const char* path, const SLint32 type)
{
    SLresult  result;
    SLEngineItf EngineItf;

    /* Objects this application uses: one player and an ouput mix */
    SLObjectItf  player, outputMix;

    /* Source of audio data to play */
    SLDataSource      audioSource;
    SLDataLocator_URI uri;
    SLDataFormat_MIME mime;

    /* Data sinks for the audio player */
    SLDataSink               audioSink;
    SLDataLocator_OutputMix  locator_outputmix;

    /* Play, Volume and AndroidStreamType interfaces for the audio player */
    SLPlayItf              playItf;
    SLPrefetchStatusItf    prefetchItf;
#ifdef ANDROID
    SLAndroidConfigurationItf configItf;
#endif

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

    /* ------------------------------------------------------ */
    /* Configuration of the output mix  */

    /* Create Output Mix object to be used by the player */
     result = (*EngineItf)->CreateOutputMix(EngineItf, &outputMix, 0, iidArray, required);
     ExitOnError(result);

    /* Realize the Output Mix object in synchronous mode */
    result = (*outputMix)->Realize(outputMix, SL_BOOLEAN_FALSE);
    ExitOnError(result);

    /* Setup the data sink structure */
    locator_outputmix.locatorType = SL_DATALOCATOR_OUTPUTMIX;
    locator_outputmix.outputMix   = outputMix;
    audioSink.pLocator            = (void*)&locator_outputmix;
    audioSink.pFormat             = NULL;

    /* ------------------------------------------------------ */
    /* Configuration of the player  */

    /* Set arrays required[] and iidArray[] for SLAndroidConfigurationItf interfaces */
    /*  (SLPlayItf is implicit) */
    required[0] = SL_BOOLEAN_TRUE;
    iidArray[0] = SL_IID_PREFETCHSTATUS;
#ifdef ANDROID
    required[1] = SL_BOOLEAN_TRUE;
    iidArray[1] = SL_IID_ANDROIDCONFIGURATION;
#endif


    /* Setup the data source structure for the URI */
    uri.locatorType = SL_DATALOCATOR_URI;
    uri.URI         =  (SLchar*) path;
    mime.formatType = SL_DATAFORMAT_MIME;
    /*     this is how ignored mime information is specified, according to OpenSL ES spec
     *     in 9.1.6 SLDataFormat_MIME and 8.23 SLMetadataTraversalItf GetChildInfo */
    mime.mimeType      = (SLchar*)NULL;
    mime.containerType = SL_CONTAINERTYPE_UNSPECIFIED;

    audioSource.pFormat  = (void*)&mime;
    audioSource.pLocator = (void*)&uri;

    /* Create the audio player */
    result = (*EngineItf)->CreateAudioPlayer(EngineItf, &player, &audioSource, &audioSink,
            MAX_NUMBER_INTERFACES, iidArray, required);
    ExitOnError(result);

    /* Retrieve the configuration interface before the player is realized so its resources
     * can be configured.
     */
#ifdef ANDROID
    result = (*player)->GetInterface(player, SL_IID_ANDROIDCONFIGURATION, (void*)&configItf);
    ExitOnError(result);

    /* Set the Android audio stream type on the player */
    result = (*configItf)->SetConfiguration(configItf,
            SL_ANDROID_KEY_STREAM_TYPE, &type, sizeof(SLint32));
    if (SL_RESULT_PARAMETER_INVALID == result) {
        fprintf(stderr, "invalid stream type %d\n", type);
    } else {
        ExitOnError(result);
    }
#endif

    /* Realize the player in synchronous mode. */
    result = (*player)->Realize(player, SL_BOOLEAN_FALSE); ExitOnError(result);
    fprintf(stdout, "URI example: after Realize\n");

    /* Get the SLPlayItf, SLPrefetchStatusItf and SLAndroidConfigurationItf interfaces for player */
    result = (*player)->GetInterface(player, SL_IID_PLAY, (void*)&playItf);
    ExitOnError(result);

    result = (*player)->GetInterface(player, SL_IID_PREFETCHSTATUS, (void*)&prefetchItf);
    ExitOnError(result);

    fprintf(stdout, "Player configured\n");

    /* ------------------------------------------------------ */
    /* Playback and test */

    /* Start the data prefetching by setting the player to the paused state */
    result = (*playItf)->SetPlayState( playItf, SL_PLAYSTATE_PAUSED );
    ExitOnError(result);

    /* Wait until there's data to play */
    SLuint32 prefetchStatus = SL_PREFETCHSTATUS_UNDERFLOW;
    while (prefetchStatus != SL_PREFETCHSTATUS_SUFFICIENTDATA) {
        usleep(100 * 1000);
        (*prefetchItf)->GetPrefetchStatus(prefetchItf, &prefetchStatus);
        ExitOnError(result);
    }

    /* Get duration */
    SLmillisecond durationInMsec = SL_TIME_UNKNOWN;
    result = (*playItf)->GetDuration(playItf, &durationInMsec);
    ExitOnError(result);
    if (durationInMsec == SL_TIME_UNKNOWN) {
        durationInMsec = 5000;
    }

    /* Start playback */
    result = (*playItf)->SetPlayState( playItf, SL_PLAYSTATE_PLAYING );
    ExitOnError(result);

    usleep((durationInMsec/2) * 1000);

#ifdef ANDROID
    /* Get the stream type during playback  */
    SLint32 currentType = -1;
    SLuint32 valueSize = sizeof(SLint32) * 2; // trying too big on purpose
    result = (*configItf)->GetConfiguration(configItf,
            SL_ANDROID_KEY_STREAM_TYPE, &valueSize, NULL);
    ExitOnError(result);
    if (valueSize != sizeof(SLint32)) {
        fprintf(stderr, "ERROR: size for stream type is %u, should be %u\n",
                valueSize, sizeof(SLint32));
    }
    result = (*configItf)->GetConfiguration(configItf,
                SL_ANDROID_KEY_STREAM_TYPE, &valueSize, &currentType);
    ExitOnError(result);
    if (currentType != type) {
        fprintf(stderr, "ERROR: stream type is %u, should be %u\n", currentType, type);
    }
#endif

    usleep((durationInMsec/2) * 1000);

    /* Make sure player is stopped */
    fprintf(stdout, "Stopping playback\n");
    result = (*playItf)->SetPlayState(playItf, SL_PLAYSTATE_STOPPED);
    ExitOnError(result);

#ifdef ANDROID
    /* Try again to get the stream type, just in case it changed behind our back */
    result = (*configItf)->GetConfiguration(configItf,
            SL_ANDROID_KEY_STREAM_TYPE, &valueSize, &currentType);
    ExitOnError(result);
    if (currentType != type) {
        fprintf(stderr, "ERROR: stream type is %u, should be %u\n", currentType, type);
    }
#endif

    /* Destroy the player */
    (*player)->Destroy(player);

    /* Destroy Output Mix object */
    (*outputMix)->Destroy(outputMix);
}

//-----------------------------------------------------------------
int main(int argc, char* const argv[])
{
    SLresult    result;
    SLObjectItf sl;

    fprintf(stdout, "OpenSL ES test %s: exercises SLPlayItf, SLAndroidConfigurationItf\n",
            argv[0]);
    fprintf(stdout, "and AudioPlayer with SLDataLocator_URI source / OutputMix sink\n");
    fprintf(stdout, "Plays a sound on the specified android stream type\n");

    if (argc < 3) {
        fprintf(stdout, "Usage: \t%s url stream_type\n", argv[0]);
        fprintf(stdout, " where stream_type is one of the SL_ANDROID_STREAM_ constants.\n");
        fprintf(stdout, "Example: \"%s /sdcard/my.mp3 5\" \n", argv[0]);
        fprintf(stdout, "Stream type %d is the default (media or music), %d is notifications\n",
            SL_ANDROID_STREAM_MEDIA, SL_ANDROID_STREAM_NOTIFICATION);
        return EXIT_FAILURE;
    }

    SLEngineOption EngineOption[] = {
            {(SLuint32) SL_ENGINEOPTION_THREADSAFE, (SLuint32) SL_BOOLEAN_TRUE}
    };

    result = slCreateEngine( &sl, 1, EngineOption, 0, NULL, NULL);
    ExitOnError(result);

    /* Realizing the SL Engine in synchronous mode. */
    result = (*sl)->Realize(sl, SL_BOOLEAN_FALSE);
    ExitOnError(result);

    TestStreamTypeConfiguration(sl, argv[1], (SLint32)atoi(argv[2]));

    /* Shutdown OpenSL ES */
    (*sl)->Destroy(sl);

    return EXIT_SUCCESS;
}
