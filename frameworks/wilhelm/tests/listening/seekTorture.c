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

// This test program tortures the seek APIs by positioning "randomly" in a file.
// It needs as input a permuted .wav and .map produced by the permute tool.

#include <SLES/OpenSLES.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#define ASSERT_EQ(x, y) assert((x) == (y))

int main(int argc, char **argv)
{
    if (argc != 3) {
        fprintf(stderr, "usage: %s file.wav file.map\n", argv[0]);
        fprintf(stderr, "  where file.wav and file.map are created by the permute tool\n");
        return EXIT_FAILURE;
    }

    SLresult result;

    // create engine
    SLObjectItf engineObject;
    result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    SLEngineItf engineEngine;
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);

    // create output mix
    SLObjectItf outputmixObject;
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputmixObject, 0, NULL, NULL);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    result = (*outputmixObject)->Realize(outputmixObject, SL_BOOLEAN_FALSE);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);

    // create an audio player with URI source and output mix sink
    SLDataSource audiosrc;
    SLDataSink audiosnk;
    SLDataLocator_OutputMix locator_outputmix;
    SLDataLocator_URI locator_uri;
    SLDataFormat_MIME mime;
    locator_uri.locatorType = SL_DATALOCATOR_URI;
    locator_uri.URI = (SLchar *) argv[1];
    locator_outputmix.locatorType = SL_DATALOCATOR_OUTPUTMIX;
    locator_outputmix.outputMix = outputmixObject;
    mime.formatType = SL_DATAFORMAT_MIME;
    mime.mimeType = (SLchar *) NULL;
    mime.containerType = SL_CONTAINERTYPE_UNSPECIFIED;
    audiosrc.pLocator = &locator_uri;
    audiosrc.pFormat = &mime;
    audiosnk.pLocator = &locator_outputmix;
    audiosnk.pFormat = NULL;
    SLObjectItf playerObject;
    SLInterfaceID ids[1] = {SL_IID_SEEK};
    SLboolean flags[1] = {SL_BOOLEAN_TRUE};
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &playerObject, &audiosrc, &audiosnk,
            1, ids, flags);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    result = (*playerObject)->Realize(playerObject, SL_BOOLEAN_FALSE);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    SLPlayItf playerPlay;
    result = (*playerObject)->GetInterface(playerObject, SL_IID_PLAY, &playerPlay);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    SLSeekItf playerSeek;
    result = (*playerObject)->GetInterface(playerObject, SL_IID_SEEK, &playerSeek);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    SLmillisecond duration;
    result = (*playerPlay)->GetDuration(playerPlay, &duration);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    result = (*playerPlay)->SetPlayState(playerPlay, SL_PLAYSTATE_PAUSED);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    result = (*playerPlay)->GetDuration(playerPlay, &duration);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);
    result = (*playerPlay)->SetPlayState(playerPlay, SL_PLAYSTATE_PLAYING);
    ASSERT_EQ(SL_RESULT_SUCCESS, result);

#if 1
    // play back a file in permuted order using the seek map
    FILE *fp_map = fopen(argv[2], "r");
    if (fp_map != NULL) {
        unsigned position, duration;
        while (fscanf(fp_map, "%u %u", &position, &duration) == 2) {
            printf("%u %u\n", position, duration);
            result = (*playerSeek)->SetPosition(playerSeek, (SLmillisecond) position,
                    SL_SEEKMODE_ACCURATE);
            ASSERT_EQ(SL_RESULT_SUCCESS, result);
            if (duration > 0)
                usleep(duration * 1000);
        }
    }
#else
    set_conio_terminal_mode();

    // loop repeatedly, inflicting seek pain each cycle
    for (;;) {
        if (kbhit()) {
            switch (getch()) {
            case 'q':
                goto out;
            }
        }
        SLmillisecond delay = 100 + (rand() & 8191);
        printf("sleep %u\n", (unsigned) delay);
        usleep(delay * 1000);
        SLmillisecond newPos = duration * ((rand() & 65535) / 65536.0);
        printf("seek %u\n", (unsigned) newPos);
        result = (*playerSeek)->SetPosition(playerSeek, newPos, SL_SEEKMODE_ACCURATE);
        ASSERT_EQ(SL_RESULT_SUCCESS, result);
        SLmillisecond nowPos;
        result = (*playerPlay)->GetPosition(playerPlay, &nowPos);
        ASSERT_EQ(SL_RESULT_SUCCESS, result);
        printf("now %u\n", (unsigned) newPos);
    }
out:
#endif

    return EXIT_SUCCESS;
}
