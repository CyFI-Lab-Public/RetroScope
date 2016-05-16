/*---------------------------------------------------------------------------*
 *  AudioHardwareRecord.c  *
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
#include "audioinwrapper.h"

#define SAMPLING_RATE       44100

#define N_FRAMES_PER_BUFFER   512  /* low-level driver counts in terms of frames, not samples */
#define N_TUPLES_PER_FRAME      1  /* tuple: a set of samples (set of 1 if mono, set of 2 if stereo */

#define N_CHANNELS_PER_TUPLE    1  /* 1: mono; 2: stereo */

#define N_TUPLES_PER_BUFFER   (N_FRAMES_PER_BUFFER * N_TUPLES_PER_FRAME)
#define N_SAMPLES_PER_BUFFER  (N_TUPLES_PER_BUFFER * N_CHANNELS_PER_TUPLE)

#define N_SECONDS_TO_RECORD    10
#define N_SAMPLES_TO_RECORD   (SAMPLING_RATE * N_SECONDS_TO_RECORD * N_CHANNELS_PER_TUPLE)

typedef short typeSample;

/* store incoming samples here, then write to file at the end */
typeSample recordedSamples[N_SAMPLES_TO_RECORD];


int main(int argc, char* argv[])
{
    int           rc;
    unsigned int  i;

    memset(recordedSamples, 0, N_SAMPLES_TO_RECORD * sizeof(typeSample));

    rc = AudioSetInputFormat(SAMPLING_RATE, N_CHANNELS_PER_TUPLE);
    if (rc != 0)
    {
        printf("ERROR: AudioSetInputFormat() returns %d\n", rc);
        exit(1);
    }

    rc = AudioOpen();
    if (rc < 0)
    {
        printf("ERROR: AudioOpen() returns %d (device handle/ID)\n", rc);
        exit(1);
    }
        
    i = 0;
    while (i <= N_SAMPLES_TO_RECORD - N_SAMPLES_PER_BUFFER)
    {
        rc = AudioRead(&(recordedSamples[i]), N_FRAMES_PER_BUFFER);
        if (rc > 0)
            i += (rc * N_TUPLES_PER_FRAME * N_CHANNELS_PER_TUPLE);
        else
            printf("ERROR: AudioRead() returns %d\n", rc);
    }

    rc = AudioClose();
    if (rc != 0)
    {
        printf("ERROR: AudioClose() returns %d\n", rc);
        exit(1);
    }

    /* write to file  */
    {
        FILE *fpOutput;
        char *szFilename = "output_AudioHardwareRecord.pcm";

        fpOutput = fopen(szFilename, "wb");
        if (fpOutput == NULL)
        {
            printf("ERROR: cannot create '%s'\n", szFilename);
            exit(1);
        }
        fwrite(recordedSamples, sizeof(typeSample), i, fpOutput);
        fclose(fpOutput);
    }

    return 0;
}
