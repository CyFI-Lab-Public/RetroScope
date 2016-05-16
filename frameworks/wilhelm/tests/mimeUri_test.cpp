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

/*
 * Copyright (c) 2009 The Khronos Group Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and /or associated documentation files (the "Materials "), to deal in the
 * Materials without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Materials,
 * and to permit persons to whom the Materials are furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Materials.
 *
 * THE MATERIALS ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE MATERIALS OR THE USE OR OTHER DEALINGS IN THE
 * MATERIALS.
 */

#define LOG_NDEBUG 0
#define LOG_TAG "slesTestPlayUri"

#include <utils/Log.h>
#include <getopt.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/time.h>
#include <gtest/gtest.h>
#include <SLES/OpenSLES.h>


#define MAX_NUMBER_INTERFACES 3
#define MAX_NUMBER_OUTPUT_DEVICES 6

//The expected playback duration
const int MP3_DURATION = 71030; //71 secs


//-----------------------------------------------------------------
/* Checks for error. If any errors exit the application! */
void CheckErr( SLresult res )
{
    if ( res != SL_RESULT_SUCCESS )  {
        fprintf(stderr, "%u SL failure, exiting\n", res);
        //Fail the test case
        ASSERT_TRUE(false);
    }
}

//-----------------------------------------------------------------
/* PrefetchStatusItf callback for an audio player */
void PrefetchEventCallback( SLPrefetchStatusItf caller,  void *pContext, SLuint32 event)
{
    SLpermille level = 0;
    (*caller)->GetFillLevel(caller, &level);
    SLuint32 status;
    fprintf(stdout, "\t\tPrefetchEventCallback: received event %u\n", event);
    (*caller)->GetPrefetchStatus(caller, &status);
    if ((event & (SL_PREFETCHEVENT_STATUSCHANGE|SL_PREFETCHEVENT_FILLLEVELCHANGE))
            && (level == 0) && (status == SL_PREFETCHSTATUS_UNDERFLOW)) {
        fprintf(stderr, "\t\tPrefetchEventCallback: Error while prefetching data, exiting\n");
        ASSERT_TRUE(false);
    }
    if (event & SL_PREFETCHEVENT_FILLLEVELCHANGE) {
        fprintf(stdout, "\t\tPrefetchEventCallback: Buffer fill level is = %d\n", level);
    }
    if (event & SL_PREFETCHEVENT_STATUSCHANGE) {
        fprintf(stdout, "\t\tPrefetchEventCallback: Prefetch Status is = %u\n", status);
    }

}


//-----------------------------------------------------------------

/* Play some music from a URI  */
void TestPlayUri( SLObjectItf sl, const char* path)
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

    SLboolean required[MAX_NUMBER_INTERFACES];
    SLInterfaceID iidArray[MAX_NUMBER_INTERFACES];

    /* Get the SL Engine Interface which is implicit */
    res = (*sl)->GetInterface(sl, SL_IID_ENGINE, (void*)&EngineItf);
    CheckErr(res);

    /* Initialize arrays required[] and iidArray[] */
    for (int i=0 ; i < MAX_NUMBER_INTERFACES ; i++) {
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
    res = (*EngineItf)->CreateAudioPlayer(EngineItf, &player,
            &audioSource, &audioSink, 2, iidArray, required); CheckErr(res);

    /* Realizing the player in synchronous mode. */
    res = (*player)->Realize(player, SL_BOOLEAN_FALSE); CheckErr(res);

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

    /* Display duration */
    SLmillisecond durationInMsec = SL_TIME_UNKNOWN;
    res = (*playItf)->GetDuration(playItf, &durationInMsec);
    CheckErr(res);

    /* Set the player volume */
    res = (*volItf)->SetVolumeLevel( volItf, -300);
    CheckErr(res);

    /* Play the URI */
    /*     first cause the player to prefetch the data */
    res = (*playItf)->SetPlayState( playItf, SL_PLAYSTATE_PAUSED );
    CheckErr(res);

    /*     wait until there's data to play */
    //SLpermille fillLevel = 0;
    SLuint32 prefetchStatus = SL_PREFETCHSTATUS_UNDERFLOW;
    SLuint32 timeOutIndex = 100; // 10s
    while ((prefetchStatus != SL_PREFETCHSTATUS_SUFFICIENTDATA) && (timeOutIndex > 0)) {
        usleep(100 * 1000);
        (*prefetchItf)->GetPrefetchStatus(prefetchItf, &prefetchStatus);
        timeOutIndex--;
    }

    if (timeOutIndex == 0) {
        fprintf(stderr, "Error: Failed to prefetch data in time, exiting\n");
        ASSERT_TRUE(false);
       // goto destroyRes;
    }

    /* Display duration again, */
    res = (*playItf)->GetDuration(playItf, &durationInMsec);
    CheckErr(res);
    if (durationInMsec == SL_TIME_UNKNOWN) {
        fprintf(stderr, "Error: GetDuration returned SL_TIME_UNKNOWN (after prefetch completed)\n");
        ASSERT_TRUE(false);
    }
    SLint32 durationDiffMsec = durationInMsec - MP3_DURATION;
    if (durationDiffMsec < 0) { durationDiffMsec *= -1; }
    if (durationDiffMsec > (MP3_DURATION/20)) {
        fprintf(stderr, "Error: GetDuration returned %d, more than 5percent off from expected %d\n",
                durationInMsec, MP3_DURATION);
        ASSERT_TRUE(false);
    }

    res = (*playItf)->SetPlayState( playItf, SL_PLAYSTATE_PLAYING );
    CheckErr(res);

    /* Play for the song duration*/
    usleep(MP3_DURATION * 1000);

    /* Validate the play position*/
    SLmillisecond currentPositionInMsec = SL_TIME_UNKNOWN;
           res = (*playItf)->GetPosition(playItf, &currentPositionInMsec);
           CheckErr(res);
    if (currentPositionInMsec == SL_TIME_UNKNOWN) {
      fprintf(stderr, "Error: GetPosition returns SL_TIME_UNKNOWN after expected duration\n");
      ASSERT_TRUE(false);
    } else if ( currentPositionInMsec <= 0 ||
        currentPositionInMsec > (MP3_DURATION * 1.1) ){
        fprintf(stderr, "Error: GetPosition returns %i, should be expected duration for test\n",
                (int) currentPositionInMsec);
        ASSERT_TRUE(false);
    }

    /* Make sure player is stopped */
    res = (*playItf)->SetPlayState(playItf, SL_PLAYSTATE_STOPPED);
    CheckErr(res);

destroyRes:

    /* Destroy the player */
    (*player)->Destroy(player);

    /* Destroy Output Mix object */
    (*OutputMix)->Destroy(OutputMix);

    fprintf(stdout, "End of test reached\n");
}


// The fixture for testing class MimeUri
class MimeUri: public ::testing::Test {
public:
    SLresult res;
    SLObjectItf sl;

protected:
    MimeUri() {
        // You can do set-up work for each test here.
        SLEngineOption EngineOption[] = { { (SLuint32) SL_ENGINEOPTION_THREADSAFE,
            (SLuint32) SL_BOOLEAN_TRUE } };

        res = slCreateEngine(&sl, 1, EngineOption, 0, NULL, NULL);
        CheckErr(res);

        /* Realizing the SL Engine in synchronous mode. */
        res = (*sl)->Realize(sl, SL_BOOLEAN_FALSE);
        CheckErr(res);
    }

    virtual ~MimeUri() {
        // You can do clean-up work that doesn't throw exceptions here.
        (*sl)->Destroy(sl);
    }

    virtual void SetUp() {
        // Code here will be called immediately after the constructor (right
        // before each test).

    }

    virtual void TearDown() {
        // Code here will be called immediately after each test (right
        // before the destructor).

    }
};

TEST_F(MimeUri, testPlayAbsPath){
    TestPlayUri(sl, "/sdcard/media_api/music/MP3_256kbps_2ch.mp3");
}

TEST_F(MimeUri, testPlayfilePath){
    TestPlayUri(sl, "file:///sdcard/media_api/music/MP3_256kbps_2ch.mp3");
}

//-----------------------------------------------------------------
int main(int argc, char **argv)
{
    testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}
