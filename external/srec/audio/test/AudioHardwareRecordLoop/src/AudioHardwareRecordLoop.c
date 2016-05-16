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
#include <unistd.h>
#include <sys/select.h>
#include "audioinwrapper.h"

#define SAMPLING_RATE        8000 

#define N_FRAMES_PER_BUFFER   512  /* low-level driver counts in terms of frames, not samples */
#define N_TUPLES_PER_FRAME      1  /* tuple: a set of samples (set of 1 if mono, set of 2 if stereo */

#define N_CHANNELS_PER_TUPLE    1  /* 1: mono; 2: stereo */

#define N_TUPLES_PER_BUFFER   (N_FRAMES_PER_BUFFER * N_TUPLES_PER_FRAME)
#define N_SAMPLES_PER_BUFFER  (N_TUPLES_PER_BUFFER * N_CHANNELS_PER_TUPLE)

#define N_RECORDINGS         1000
#define N_SECONDS_TO_RECORD    5
#define N_SAMPLES_TO_RECORD   (SAMPLING_RATE * N_SECONDS_TO_RECORD * N_CHANNELS_PER_TUPLE)

typedef short typeSample;

/* store incoming samples here, then write to file at the end */
typeSample recordedSamples[N_SAMPLES_TO_RECORD];

//#define AUDIO_SET_FORMAT_ONCE_ONLY

int main(int argc, char* argv[])
{
    int           rc;
    unsigned int  iFile;
    
    const unsigned short delay_ms = 2000; //1800 //340;
    
    printf("For debugging, this is configured to sleep for %u milliseconds before AudioSetInputFormat(%u)\n\n", delay_ms, SAMPLING_RATE);

#if defined(AUDIO_SET_FORMAT_ONCE_ONLY)
    rc = AudioSetInputFormat(SAMPLING_RATE, N_CHANNELS_PER_TUPLE);
    if (rc != 0)
    {
        printf("ERROR: AudioSetInputFormat() returns %d\n", rc);
        exit(1);
    }
#endif

    for (iFile = 1; iFile <= N_RECORDINGS; iFile++)
    {
        unsigned int  i;
                    
#if !defined(AUDIO_SET_FORMAT_ONCE_ONLY)
        {
          	// see how much of a delay is needed to get rid of error when calling 
          	// AudioSetInputFormat() immediately after AudioClose()          	
          	struct timeval  sleep_time_struct;
          	sleep_time_struct.tv_sec = 0;
          	sleep_time_struct.tv_usec = delay_ms*1000; // microseconds
     				select(0, NULL, NULL, NULL, &sleep_time_struct);
        }

        rc = AudioSetInputFormat(SAMPLING_RATE, N_CHANNELS_PER_TUPLE);
        if (rc != 0)
        {
            printf("ERROR: AudioSetInputFormat() returns %d\n", rc);
            exit(1);
        }
#endif
    
        printf("Recording: %3d of %3d\n", iFile, N_RECORDINGS);

        memset(recordedSamples, 0, N_SAMPLES_TO_RECORD * sizeof(typeSample));

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
#if 0        
        {
            FILE *fpOutput;
            char szFilename[256];
            
            sprintf(szFilename, "output_AudioHardwareRecordLoop_%03d.pcm", iFile);
        
            fpOutput = fopen(szFilename, "wb");
            if (fpOutput == NULL)
            {
                printf("ERROR: cannot create '%s'\n", szFilename);
                exit(1);
            }
            fwrite(recordedSamples, sizeof(typeSample), i, fpOutput);
            fclose(fpOutput);
            
            printf("Recording: saved '%s'\n", szFilename);
        }
#endif
    }
    
    return 0;
}
