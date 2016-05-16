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

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/time.h>


#include <SLES/OpenSLES.h>


#define MAX_NUMBER_INTERFACES 3

/* Local storage for Audio data in 16 bit words */
#define AUDIO_DATA_STORAGE_SIZE 4096 * 100
/* Audio data buffer size in 16 bit words. 8 data segments are used in
this simple example */
#define AUDIO_DATA_BUFFER_SIZE 4096/8

/* Checks for error. If any errors exit the application! */
void CheckErr( SLresult res )
{
    if ( res != SL_RESULT_SUCCESS )
        {
            fprintf(stdout, "%u SL failure, exiting\n", res);
            exit(EXIT_FAILURE);
        }
    else {
        //fprintf(stdout, "%d SL success, proceeding...\n", res);
    }
}

/* Structure for passing information to callback function */
typedef struct CallbackCntxt_ {
    SLPlayItf  playItf;
    SLint16*   pDataBase;    // Base adress of local audio data storage
    SLint16*   pData;        // Current adress of local audio data storage
    SLuint32   size;
} CallbackCntxt;

/* Local storage for Audio data */
SLint16 pcmData[AUDIO_DATA_STORAGE_SIZE];

/* Callback for Buffer Queue events */
void BufferQueueCallback(
        SLBufferQueueItf queueItf,
        void *pContext)
{
    //fprintf(stdout, "BufferQueueCallback called\n");
    SLresult res;
    //fprintf(stdout, " pContext=%p\n", pContext);
    CallbackCntxt *pCntxt = (CallbackCntxt*)pContext;

    if(pCntxt->pData < (pCntxt->pDataBase + pCntxt->size))
        {
            //fprintf(stdout, "callback: before enqueue\n");
            res = (*queueItf)->Enqueue(queueItf, (void*) pCntxt->pData,
                    2 * AUDIO_DATA_BUFFER_SIZE); /* Size given in bytes. */
            CheckErr(res);
            /* Increase data pointer by buffer size */
            pCntxt->pData += AUDIO_DATA_BUFFER_SIZE;
        }
    //fprintf(stdout, "end of BufferQueueCallback()\n");
}

/* Play some audio from a buffer queue  */
void TestPlaySawtoothBufferQueue( SLObjectItf sl )
{
    SLEngineItf                EngineItf;

    SLint32                    numOutputs = 0;
    SLuint32                   deviceID = 0;

    SLresult                   res;

    SLDataSource               audioSource;
    SLDataLocator_BufferQueue  bufferQueue;
    SLDataFormat_PCM           pcm;

    SLDataSink                 audioSink;
    SLDataLocator_OutputMix    locator_outputmix;

    SLObjectItf                player;
    SLPlayItf                  playItf;
    SLBufferQueueItf           bufferQueueItf;
    SLBufferQueueState         state;

    SLObjectItf                OutputMix;
    SLVolumeItf                volumeItf;

    int                        i;

    SLboolean required[MAX_NUMBER_INTERFACES];
    SLInterfaceID iidArray[MAX_NUMBER_INTERFACES];

    /* Callback context for the buffer queue callback function */
    CallbackCntxt cntxt;

    /* Get the SL Engine Interface which is implicit */
    res = (*sl)->GetInterface(sl, SL_IID_ENGINE, (void*)&EngineItf);
    CheckErr(res);

    /* Initialize arrays required[] and iidArray[] */
    for (i=0;i<MAX_NUMBER_INTERFACES;i++)
        {
            required[i] = SL_BOOLEAN_FALSE;
            iidArray[i] = SL_IID_NULL;
        }

    // Set arrays required[] and iidArray[] for VOLUME interface
    required[0] = SL_BOOLEAN_TRUE;
    iidArray[0] = SL_IID_VOLUME;
    // Create Output Mix object to be used by player
    res = (*EngineItf)->CreateOutputMix(EngineItf, &OutputMix, 0,
            iidArray, required); CheckErr(res);

    // Realizing the Output Mix object in synchronous mode.
    res = (*OutputMix)->Realize(OutputMix, SL_BOOLEAN_FALSE);
    CheckErr(res);

#if 0
    res = (*OutputMix)->GetInterface(OutputMix, SL_IID_VOLUME,
            (void*)&volumeItf); CheckErr(res);
#endif

    /* Setup the data source structure for the buffer queue */
    bufferQueue.locatorType = SL_DATALOCATOR_BUFFERQUEUE;
    bufferQueue.numBuffers = 4;  /* Four buffers in our buffer queue */

    /* Setup the format of the content in the buffer queue */
    pcm.formatType = SL_DATAFORMAT_PCM;
    pcm.numChannels = 1;//2;
    pcm.samplesPerSec = SL_SAMPLINGRATE_44_1;
    pcm.bitsPerSample = SL_PCMSAMPLEFORMAT_FIXED_16;
    pcm.containerSize = 16;
    pcm.channelMask = SL_SPEAKER_FRONT_LEFT;// | SL_SPEAKER_FRONT_RIGHT;
    pcm.endianness = SL_BYTEORDER_LITTLEENDIAN;

    audioSource.pFormat      = (void *)&pcm;
    audioSource.pLocator     = (void *)&bufferQueue;

    /* Setup the data sink structure */
    locator_outputmix.locatorType   = SL_DATALOCATOR_OUTPUTMIX;
    locator_outputmix.outputMix    = OutputMix;
    audioSink.pLocator           = (void *)&locator_outputmix;
    audioSink.pFormat            = NULL;

    /* Initialize the audio data to play */
    unsigned int j;
    for (j = 0; j < sizeof(pcmData)/sizeof(pcmData[0]); ++j) {
        pcmData[j] = j*(100 + j / 200);// % 1000;
    }

    /* Initialize the context for Buffer queue callbacks */
    cntxt.pDataBase = /*(void*)&*/pcmData;
    cntxt.pData = cntxt.pDataBase;
    cntxt.size = sizeof(pcmData) / 2;

    /* Set arrays required[] and iidArray[] for SEEK interface
          (PlayItf is implicit) */
    required[0] = SL_BOOLEAN_TRUE;
    iidArray[0] = SL_IID_BUFFERQUEUE;

    /* Create the music player */
    res = (*EngineItf)->CreateAudioPlayer(EngineItf, &player,
            &audioSource, &audioSink, 1, iidArray, required); CheckErr(res);
    fprintf(stdout, "bufferQueue example: after CreateAudioPlayer\n");

    /* Realizing the player in synchronous mode. */
    res = (*player)->Realize(player, SL_BOOLEAN_FALSE); CheckErr(res);
    fprintf(stdout, "bufferQueue example: after Realize\n");

    /* Get seek and play interfaces */
    res = (*player)->GetInterface(player, SL_IID_PLAY, (void*)&playItf);
    CheckErr(res);
    fprintf(stdout, "bufferQueue example: after GetInterface(PLAY)\n");

    res = (*player)->GetInterface(player, SL_IID_BUFFERQUEUE,
            (void*)&bufferQueueItf); CheckErr(res);

    /* Setup to receive buffer queue event callbacks */
    res = (*bufferQueueItf)->RegisterCallback(bufferQueueItf,
            BufferQueueCallback, &cntxt); CheckErr(res);

#if 0
    /* Before we start set volume to -3dB (-300mB) */
    res = (*volumeItf)->SetVolumeLevel(volumeItf, -300); CheckErr(res);
#endif

    /* Enqueue a few buffers to get the ball rolling */
    res = (*bufferQueueItf)->Enqueue(bufferQueueItf, cntxt.pData,
            2 * AUDIO_DATA_BUFFER_SIZE); /* Size given in bytes. */
    CheckErr(res);
    cntxt.pData += AUDIO_DATA_BUFFER_SIZE;

    res = (*bufferQueueItf)->Enqueue(bufferQueueItf, cntxt.pData,
            2 * AUDIO_DATA_BUFFER_SIZE); /* Size given in bytes. */
    CheckErr(res);
    cntxt.pData += AUDIO_DATA_BUFFER_SIZE;

    res = (*bufferQueueItf)->Enqueue(bufferQueueItf, cntxt.pData,
            2 * AUDIO_DATA_BUFFER_SIZE); /* Size given in bytes. */
    CheckErr(res);
    cntxt.pData += AUDIO_DATA_BUFFER_SIZE;

    /* Play the PCM samples using a buffer queue */
    fprintf(stdout, "bufferQueue example: starting to play\n");
    res = (*playItf)->SetPlayState( playItf, SL_PLAYSTATE_PLAYING );
    CheckErr(res);

    /* Wait until the PCM data is done playing, the buffer queue callback
           will continue to queue buffers until the entire PCM data has been
           played. This is indicated by waiting for the count member of the
           SLBufferQueueState to go to zero.
     */
    res = (*bufferQueueItf)->GetState(bufferQueueItf, &state);
    CheckErr(res);

    // while (state.playIndex < 100) {
    while (state.count) {
        usleep(10000);
        (*bufferQueueItf)->GetState(bufferQueueItf, &state);
    }

    /* Make sure player is stopped */
    res = (*playItf)->SetPlayState(playItf, SL_PLAYSTATE_STOPPED);
    CheckErr(res);
    /* Destroy the player */
    (*player)->Destroy(player);

    /* Destroy Output Mix object */
    (*OutputMix)->Destroy(OutputMix);
}



int main(int argc, char* const argv[])
{
    SLresult    res;
    SLObjectItf sl;

    SLEngineOption EngineOption[] = {
            {(SLuint32) SL_ENGINEOPTION_THREADSAFE,
            (SLuint32) SL_BOOLEAN_TRUE}};

    res = slCreateEngine( &sl, 1, EngineOption, 0, NULL, NULL);
    CheckErr(res);
    /* Realizing the SL Engine in synchronous mode. */
    res = (*sl)->Realize(sl, SL_BOOLEAN_FALSE); CheckErr(res);

    /* Run the test */
    TestPlaySawtoothBufferQueue(sl);

    /* Shutdown OpenSL ES */
    (*sl)->Destroy(sl);

    return EXIT_SUCCESS;
}
