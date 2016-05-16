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
#include <fcntl.h>

#include <SLES/OpenSLES.h>
#ifdef ANDROID
#include <SLES/OpenSLES_Android.h>
#endif


#define MAX_NUMBER_INTERFACES 3

#define TIME_S_BETWEEN_EQ_ON_OFF 3

//-----------------------------------------------------------------
/* Exits the application if an error is encountered */
#define ExitOnError(x) ExitOnErrorFunc(x,__LINE__)

void ExitOnErrorFunc( SLresult result , int line)
{
    if (SL_RESULT_SUCCESS != result) {
        fprintf(stderr, "%u error code encountered at line %d, exiting\n", result, line);
        exit(EXIT_FAILURE);
    }
}


//-----------------------------------------------------------------

/* Play an audio path by opening a file descriptor on that path  */
void TestEQPathFromFD( SLObjectItf sl, const char* path
#ifdef ANDROID
    , SLAint64 offset, SLAint64 size
#endif
    , bool alwaysOn
    )
{
    SLresult  result;
    SLEngineItf EngineItf;

    /* Objects this application uses: one player and an ouput mix */
    SLObjectItf  player, outputMix;

    /* Source of audio data to play */
    SLDataSource            audioSource;
#ifdef ANDROID
    SLDataLocator_AndroidFD locatorFd;
#else
    SLDataLocator_URI       locatorUri;
#endif
    SLDataFormat_MIME       mime;

    /* Data sinks for the audio player */
    SLDataSink               audioSink;
    SLDataLocator_OutputMix  locator_outputmix;

    /* Play and PrefetchStatus interfaces for the audio player */
    SLPlayItf              playItf;
    SLPrefetchStatusItf    prefetchItf;

    /* Effect interface for the output mix */
    SLEqualizerItf         eqOutputItf;

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

    /* Set arrays required[] and iidArray[] for SLEqualizerItf interface */
    required[0] = SL_BOOLEAN_TRUE;
    iidArray[0] = SL_IID_EQUALIZER;

    /* Create Output Mix object to be used by the player */
     result = (*EngineItf)->CreateOutputMix(EngineItf, &outputMix, 1, iidArray, required);
     ExitOnError(result);

    /* Realize the Output Mix object in synchronous mode */
    result = (*outputMix)->Realize(outputMix, SL_BOOLEAN_FALSE);
    ExitOnError(result);

    /* Get the SLEqualizerItf interface */
    result = (*outputMix)->GetInterface(outputMix, SL_IID_EQUALIZER, (void*)&eqOutputItf);

    /* Setup the data sink structure */
    locator_outputmix.locatorType = SL_DATALOCATOR_OUTPUTMIX;
    locator_outputmix.outputMix   = outputMix;
    audioSink.pLocator            = (void*)&locator_outputmix;
    audioSink.pFormat             = NULL;

    /* ------------------------------------------------------ */
    /* Configuration of the player  */

    /* Set arrays required[] and iidArray[] for SLPrefetchStatusItf interfaces */
    /*  (SLPlayItf is implicit) */
    required[0] = SL_BOOLEAN_TRUE;
    iidArray[0] = SL_IID_PREFETCHSTATUS;

    /* Setup the data source structure for the URI */
#ifdef ANDROID
    locatorFd.locatorType = SL_DATALOCATOR_ANDROIDFD;
    int fd = open(path, O_RDONLY);
    if (fd == -1) {
        ExitOnError(SL_RESULT_RESOURCE_ERROR);
    }
    locatorFd.fd = (SLint32) fd;
    locatorFd.length = size;
    locatorFd.offset = offset;
#else
    locatorUri.locatorType = SL_DATALOCATOR_URI;
    locatorUri.URI = (SLchar *) path;
#endif

    mime.formatType = SL_DATAFORMAT_MIME;
    /*     this is how ignored mime information is specified, according to OpenSL ES spec
     *     in 9.1.6 SLDataFormat_MIME and 8.23 SLMetadataTraversalItf GetChildInfo */
    mime.mimeType      = (SLchar*)NULL;
    mime.containerType = SL_CONTAINERTYPE_UNSPECIFIED;

    audioSource.pFormat  = (void*)&mime;
#ifdef ANDROID
    audioSource.pLocator = (void*)&locatorFd;
#else
    audioSource.pLocator = (void*)&locatorUri;
#endif

    /* Create the audio player */
    result = (*EngineItf)->CreateAudioPlayer(EngineItf, &player, &audioSource, &audioSink, 1,
            iidArray, required);
    ExitOnError(result);

    /* Realize the player in synchronous mode. */
    result = (*player)->Realize(player, SL_BOOLEAN_FALSE); ExitOnError(result);
    fprintf(stdout, "URI example: after Realize\n");

    /* Get the SLPlayItf, SLPrefetchStatusItf and SLAndroidStreamTypeItf interfaces for the player*/
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
    fprintf(stdout, "Starting to play\n");
    result = (*playItf)->SetPlayState(playItf, SL_PLAYSTATE_PLAYING );
    ExitOnError(result);

    /* Configure EQ */
    SLuint16 nbPresets, preset, nbBands = 0;
    result = (*eqOutputItf)->GetNumberOfBands(eqOutputItf, &nbBands);
    ExitOnError(result);
    result = (*eqOutputItf)->GetNumberOfPresets(eqOutputItf, &nbPresets);
    ExitOnError(result);
    /*    Start from a preset  */
    preset = nbPresets > 2 ?  2 : 0;
    result = (*eqOutputItf)->UsePreset(eqOutputItf, preset);

    preset = 1977;
    result = (*eqOutputItf)->GetCurrentPreset(eqOutputItf, &preset);
    ExitOnError(result);
    if (SL_EQUALIZER_UNDEFINED == preset) {
        fprintf(stderr, "Using SL_EQUALIZER_UNDEFINED preset, unexpected here!\n");
    } else {
        fprintf(stdout, "Using preset %d\n", preset);
    }

    /*    Tweak it so it's obvious it gets turned on/off later */
    SLmillibel minLevel, maxLevel = 0;
    result = (*eqOutputItf)->GetBandLevelRange(eqOutputItf, &minLevel, &maxLevel);
    ExitOnError(result);
    fprintf(stdout, "Band level range = %dmB to %dmB\n", minLevel, maxLevel);

    SLuint16 b = 0;
    for(b = 0 ; b < nbBands/2 ; b++) {
        result = (*eqOutputItf)->SetBandLevel(eqOutputItf, b, minLevel);
        ExitOnError(result);
    }
    for(b = nbBands/2 ; b < nbBands ; b++) {
        result = (*eqOutputItf)->SetBandLevel(eqOutputItf, b, maxLevel);
        ExitOnError(result);
    }

    SLmillibel level = 0;
    for(b = 0 ; b < nbBands ; b++) {
        result = (*eqOutputItf)->GetBandLevel(eqOutputItf, b, &level);
        ExitOnError(result);
        fprintf(stdout, "Band %d level = %dmB\n", b, level);
    }

    /* Switch EQ on/off every TIME_S_BETWEEN_EQ_ON_OFF seconds unless always on */
    SLboolean previousEnabled = SL_BOOLEAN_FALSE;
    for(unsigned int j=0 ; j<(durationInMsec/(1000*TIME_S_BETWEEN_EQ_ON_OFF)) ; j++) {
        SLboolean enabled;
        result = (*eqOutputItf)->IsEnabled(eqOutputItf, &enabled);
        ExitOnError(result);
        enabled = alwaysOn || !enabled;
        if (enabled != previousEnabled) {
            result = (*eqOutputItf)->SetEnabled(eqOutputItf, enabled);
            ExitOnError(result);
            previousEnabled = enabled;
            if (SL_BOOLEAN_TRUE == enabled) {
                fprintf(stdout, "EQ on\n");
            } else {
                fprintf(stdout, "EQ off\n");
            }
        }
        usleep(TIME_S_BETWEEN_EQ_ON_OFF * 1000 * 1000);
    }

    /* Make sure player is stopped */
    fprintf(stdout, "Stopping playback\n");
    result = (*playItf)->SetPlayState(playItf, SL_PLAYSTATE_STOPPED);
    ExitOnError(result);

    /* Destroy the player */
    (*player)->Destroy(player);

    /* Destroy Output Mix object */
    (*outputMix)->Destroy(outputMix);

#ifdef ANDROID
    close(fd);
#endif
}

//-----------------------------------------------------------------
int main(int argc, char* const argv[])
{
    const char *programName = argv[0];
    SLresult    result;
    SLObjectItf sl;

    fprintf(stdout, "OpenSL ES test %s: exercises SLEqualizerItf ", programName);
    fprintf(stdout, "on an OutputMix object\n");
    fprintf(stdout, "Plays the sound file designated by the given path, ");
    fprintf(stdout, "starting at the specified offset, and using the specified length.\n");
    fprintf(stdout, "Omit the length of the file for it to be computed by the system.\n");
    fprintf(stdout, "Every %d seconds, the EQ will be turned on and off,\n",
            TIME_S_BETWEEN_EQ_ON_OFF);
    fprintf(stdout, "unless the --always-on option is specified before the path.\n");

    bool alwaysOn = false;
    if (argc >= 2 && !strcmp(argv[1], "--always-on")) {
        alwaysOn = true;
        --argc;
        ++argv;
    }

#ifdef ANDROID
    if (argc < 3) {
        fprintf(stdout, "Usage: \t%s [--always-on] path offsetInBytes [sizeInBytes]\n",
                programName);
        fprintf(stdout, "Example: \"%s /sdcard/my.mp3 0 344460\" \n", programName);
        exit(EXIT_FAILURE);
    }
#endif

    SLEngineOption EngineOption[] = {
            {(SLuint32) SL_ENGINEOPTION_THREADSAFE, (SLuint32) SL_BOOLEAN_TRUE}
    };

    result = slCreateEngine( &sl, 1, EngineOption, 0, NULL, NULL);
    ExitOnError(result);

    /* Realizing the SL Engine in synchronous mode. */
    result = (*sl)->Realize(sl, SL_BOOLEAN_FALSE);
    ExitOnError(result);

#ifdef ANDROID
    if (argc == 3) {
        fprintf(stdout, "\nno file size given, using SL_DATALOCATOR_ANDROIDFD_USE_FILE_SIZE\n\n");
        TestEQPathFromFD(sl, argv[1], (SLAint64)atoi(argv[2]),
                SL_DATALOCATOR_ANDROIDFD_USE_FILE_SIZE, alwaysOn);
    } else {
        TestEQPathFromFD(sl, argv[1], (SLAint64)atoi(argv[2]), (SLAint64)atoi(argv[3]), alwaysOn);
    }
#else
    TestEQPathFromFD(sl, argv[1], alwaysOn);
#endif

    /* Shutdown OpenSL ES */
    (*sl)->Destroy(sl);

    return EXIT_SUCCESS;
}
