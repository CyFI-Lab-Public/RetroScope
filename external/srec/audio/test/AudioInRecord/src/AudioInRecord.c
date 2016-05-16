/*---------------------------------------------------------------------------*
 *  AudioInRecord.c  *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                               *
 *                                                                           *
 *  Licensed under the Apache License, Version 2.0 (the 'License');          *
 *  you may not use this file except in compliance with the License.         *
 *                                                                           *
 *  You may obtain a copy of the License at                                  *
 *      http://www.apache.org/licenses/LICENSE-2.0                           *
 *                                                                           *
 *  Unless required by applicable law or agreed to in writing, software      *
 *  distributed under the License is distributed on an 'AS IS' BASIS,        *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * 
 *  See the License for the specific language governing permissions and      *
 *  limitations under the License.                                           *
 *                                                                           *
 *---------------------------------------------------------------------------*/

#include <stdlib.h>
#include <stdio.h>
#include <limits.h>
#include <string.h>
#ifdef WIN32
#include <windows.h>
#include <mmsystem.h>
#endif
#include "audioin.h"

#define SAMPLING_RATE       11025

#define N_FRAMES_PER_BUFFER   512  /* low-level driver counts in terms of frames, not samples */
#define N_TUPLES_PER_FRAME      1  /* tuple: a set of samples (set of 1 if mono, set of 2 if stereo */
#define N_CHANNELS_PER_TUPLE    1  /* 1: mono; 2: stereo */

#define N_TUPLES_PER_BUFFER   (N_FRAMES_PER_BUFFER * N_TUPLES_PER_FRAME)
#define N_SAMPLES_PER_BUFFER  (N_TUPLES_PER_BUFFER * N_CHANNELS_PER_TUPLE)

#define N_SECONDS_TO_RECORD    10
#define N_SAMPLES_TO_RECORD   (N_SECONDS_TO_RECORD * SAMPLING_RATE * N_CHANNELS_PER_TUPLE)

#define OUTPUT_FILENAME       "output_AudioInRecord.pcm"

typedef short typeSample;

/* store incoming samples here, then write to file at the end */
typeSample recordedSamples[N_SAMPLES_TO_RECORD];


int main(int argc, char* argv[])
{
    AUDIOIN_H         hAudioIn;
    AUDIOIN_INFO      AudioInInfo;
    LHS_AUDIOIN_ERROR lhsErr;
    unsigned int      nSamples;

    printf("\nAudioTestRecord: capturing %u seconds of audio at %u Hz\n\n", N_SECONDS_TO_RECORD, SAMPLING_RATE);

    memset(recordedSamples, 0, N_SAMPLES_TO_RECORD * sizeof(typeSample));

    printf("Opening the AudioIn device:  ");
    lhsErr = lhs_audioinOpen(WAVE_MAPPER, SAMPLING_RATE, &hAudioIn);
    printf("lhs_audioinOpen()  returns %ld\n", lhsErr);
    if (lhsErr != LHS_AUDIOIN_OK)
    {
        printf("ERROR: Unable to open audio device\n\n");
        return 1;
    }

    printf("Starting the AudioIn device: ");
    lhsErr = lhs_audioinStart(hAudioIn);
    printf("lhs_audioinStart() returns %ld\n", lhsErr);
    if (lhsErr != LHS_AUDIOIN_OK)
    {
        printf("ERROR: Unable to start audio device\n\n");
        printf("Closing the AudioIn device: ");
        lhsErr = lhs_audioinClose(&hAudioIn);
        printf("lhs_audioinClose() returns %ld\n", lhsErr);
        if (lhsErr != LHS_AUDIOIN_OK)
        {
            printf("ERROR: Unable to close audio device\n\n");
            return 1;
        }
        return 1;
    }

    printf("... Start Speaking ...\n");

    nSamples = 0;
    while (nSamples <= N_SAMPLES_TO_RECORD - N_SAMPLES_PER_BUFFER)
    {
        unsigned long u32NbrOfSamples;

        u32NbrOfSamples = N_SAMPLES_PER_BUFFER / N_CHANNELS_PER_TUPLE;  /* audioin only does mono */
        lhsErr = lhs_audioinGetSamples(hAudioIn, &u32NbrOfSamples, &(recordedSamples[nSamples]), &AudioInInfo);
        if (lhsErr == LHS_AUDIOIN_OK)
            nSamples += u32NbrOfSamples;
        else
            printf("ERROR: lhs_audioinGetSamples() returns %ld\n", lhsErr);
    }

    printf("Stopping the AudioIn device: ");
    lhsErr = lhs_audioinStop(hAudioIn);
    printf("lhs_audioinStop()  returns %ld\n", lhsErr);
    if (lhsErr != LHS_AUDIOIN_OK)
    {
        printf("ERROR: Unable to stop audio device\n\n");
        printf("Closing the AudioIn device: ");
        lhsErr = lhs_audioinClose(&hAudioIn);
        printf("lhs_audioinClose() returns %ld\n", lhsErr);
        if (lhsErr != LHS_AUDIOIN_OK)
        {
            printf("ERROR: Unable to close audio device\n\n");
            return 1;
        }
        return 1;
    }

    printf("Closing the AudioIn device:  ");
    lhsErr = lhs_audioinClose(&hAudioIn);
    printf("lhs_audioinClose() returns %ld\n", lhsErr);
    if (lhsErr != LHS_AUDIOIN_OK)
    {
        printf("ERROR: Unable to close audio device\n\n");
        return 1;
    }

    /* write to file  */
    {
        FILE *fpOutput;
        char *szFilename = OUTPUT_FILENAME;

        fpOutput = fopen(szFilename, "wb");
        if (fpOutput == NULL)
        {
            printf("ERROR: cannot create output file: '%s'\n", szFilename);
            return 1;
        }
        fwrite(recordedSamples, sizeof(typeSample), nSamples, fpOutput);
        fclose(fpOutput);

        printf("\nOutput audio saved to '%s'\n\n", szFilename);
    }


    return 0;
}
