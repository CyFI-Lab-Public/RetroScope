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

/* Audio Record Test

First run the program from shell:
  # slesTest_recBuffQueue /sdcard/myrec.raw 4

These use adb on host to retrive the file:
  % adb pull /sdcard/myrec.raw myrec.raw

How to examine the output with Audacity:
 Project / Import raw data
 Select myrec.raw file, then click Open button
 Choose these options:
  Signed 16-bit PCM
  Little-endian
  1 Channel (Mono)
  Sample rate 22050 Hz
 Click Import button

*/


#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/time.h>
#include <fcntl.h>

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

/* Preset number to use for recording */
SLuint32 presetValue = SL_ANDROID_RECORDING_PRESET_NONE;

/* Explicitly requesting SL_IID_ANDROIDSIMPLEBUFFERQUEUE and SL_IID_ANDROIDCONFIGURATION
 * on the AudioRecorder object */
#define NUM_EXPLICIT_INTERFACES_FOR_RECORDER 2

/* Size of the recording buffer queue */
#define NB_BUFFERS_IN_QUEUE 1
/* Size of each buffer in the queue */
#define BUFFER_SIZE_IN_SAMPLES 1024
#define BUFFER_SIZE_IN_BYTES   (2*BUFFER_SIZE_IN_SAMPLES)

/* Local storage for Audio data */
int8_t pcmData[NB_BUFFERS_IN_QUEUE * BUFFER_SIZE_IN_BYTES];

/* destination for recorded data */
static FILE* gFp;

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
/* Structure for passing information to callback function */
typedef struct CallbackCntxt_ {
    SLPlayItf  playItf;
    SLuint32   size;
    SLint8*   pDataBase;    // Base address of local audio data storage
    SLint8*   pData;        // Current address of local audio data storage
} CallbackCntxt;


//-----------------------------------------------------------------
/* Callback for recording buffer queue events */
void RecCallback(
        SLRecordItf caller,
        void *pContext,
        SLuint32 event)
{
    if (SL_RECORDEVENT_HEADATNEWPOS & event) {
        SLmillisecond pMsec = 0;
        (*caller)->GetPosition(caller, &pMsec);
        fprintf(stdout, "SL_RECORDEVENT_HEADATNEWPOS current position=%ums\n", pMsec);
    }

    if (SL_RECORDEVENT_HEADATMARKER & event) {
        SLmillisecond pMsec = 0;
        (*caller)->GetPosition(caller, &pMsec);
        fprintf(stdout, "SL_RECORDEVENT_HEADATMARKER current position=%ums\n", pMsec);
    }
}

//-----------------------------------------------------------------
/* Callback for recording buffer queue events */
void RecBufferQueueCallback(
        SLAndroidSimpleBufferQueueItf queueItf,
        void *pContext)
{
    //fprintf(stdout, "RecBufferQueueCallback called\n");

    CallbackCntxt *pCntxt = (CallbackCntxt*)pContext;

    /* Save the recorded data  */
    fwrite(pCntxt->pDataBase, BUFFER_SIZE_IN_BYTES, 1, gFp);

    /* Increase data pointer by buffer size */
    pCntxt->pData += BUFFER_SIZE_IN_BYTES;

    if (pCntxt->pData >= pCntxt->pDataBase + (NB_BUFFERS_IN_QUEUE * BUFFER_SIZE_IN_BYTES)) {
        pCntxt->pData = pCntxt->pDataBase;
    }

    ExitOnError( (*queueItf)->Enqueue(queueItf, pCntxt->pDataBase, BUFFER_SIZE_IN_BYTES) );

    SLAndroidSimpleBufferQueueState recQueueState;
    ExitOnError( (*queueItf)->GetState(queueItf, &recQueueState) );

    /*fprintf(stderr, "\tRecBufferQueueCallback now has pCntxt->pData=%p queue: "
            "count=%u playIndex=%u\n",
            pCntxt->pData, recQueueState.count, recQueueState.index);*/
}

//-----------------------------------------------------------------

/* Play an audio path by opening a file descriptor on that path  */
void TestRecToBuffQueue( SLObjectItf sl, const char* path, SLAint64 durationInSeconds)
{
    gFp = fopen(path, "w");
    if (NULL == gFp) {
        ExitOnError(SL_RESULT_RESOURCE_ERROR);
    }

    SLresult  result;
    SLEngineItf EngineItf;

    /* Objects this application uses: one audio recorder */
    SLObjectItf  recorder;

    /* Interfaces for the audio recorder */
    SLAndroidSimpleBufferQueueItf recBuffQueueItf;
    SLRecordItf               recordItf;
    SLAndroidConfigurationItf configItf;

    /* Source of audio data for the recording */
    SLDataSource           recSource;
    SLDataLocator_IODevice ioDevice;

    /* Data sink for recorded audio */
    SLDataSink                recDest;
    SLDataLocator_AndroidSimpleBufferQueue recBuffQueue;
    SLDataFormat_PCM          pcm;

    SLboolean required[NUM_EXPLICIT_INTERFACES_FOR_RECORDER];
    SLInterfaceID iidArray[NUM_EXPLICIT_INTERFACES_FOR_RECORDER];

    /* Get the SL Engine Interface which is implicit */
    result = (*sl)->GetInterface(sl, SL_IID_ENGINE, (void*)&EngineItf);
    ExitOnError(result);

    /* Initialize arrays required[] and iidArray[] */
    for (int i=0 ; i < NUM_EXPLICIT_INTERFACES_FOR_RECORDER ; i++) {
        required[i] = SL_BOOLEAN_FALSE;
        iidArray[i] = SL_IID_NULL;
    }


    /* ------------------------------------------------------ */
    /* Configuration of the recorder  */

    /* Request the AndroidSimpleBufferQueue and AndroidConfiguration interfaces */
    required[0] = SL_BOOLEAN_TRUE;
    iidArray[0] = SL_IID_ANDROIDSIMPLEBUFFERQUEUE;
    required[1] = SL_BOOLEAN_TRUE;
    iidArray[1] = SL_IID_ANDROIDCONFIGURATION;

    /* Setup the data source */
    ioDevice.locatorType = SL_DATALOCATOR_IODEVICE;
    ioDevice.deviceType = SL_IODEVICE_AUDIOINPUT;
    ioDevice.deviceID = SL_DEFAULTDEVICEID_AUDIOINPUT;
    ioDevice.device = NULL;
    recSource.pLocator = (void *) &ioDevice;
    recSource.pFormat = NULL;

    /* Setup the data sink */
    recBuffQueue.locatorType = SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE;
    recBuffQueue.numBuffers = NB_BUFFERS_IN_QUEUE;
    /*    set up the format of the data in the buffer queue */
    pcm.formatType = SL_DATAFORMAT_PCM;
    pcm.numChannels = 1;
    pcm.samplesPerSec = SL_SAMPLINGRATE_22_05;
    pcm.bitsPerSample = SL_PCMSAMPLEFORMAT_FIXED_16;
    pcm.containerSize = 16;
    pcm.channelMask = SL_SPEAKER_FRONT_LEFT;
    pcm.endianness = SL_BYTEORDER_LITTLEENDIAN;

    recDest.pLocator = (void *) &recBuffQueue;
    recDest.pFormat = (void * ) &pcm;

    /* Create the audio recorder */
    result = (*EngineItf)->CreateAudioRecorder(EngineItf, &recorder, &recSource, &recDest,
            NUM_EXPLICIT_INTERFACES_FOR_RECORDER, iidArray, required);
    ExitOnError(result);
    fprintf(stdout, "Recorder created\n");

    /* Get the Android configuration interface which is explicit */
    result = (*recorder)->GetInterface(recorder, SL_IID_ANDROIDCONFIGURATION, (void*)&configItf);
    ExitOnError(result);

    /* Use the configuration interface to configure the recorder before it's realized */
    if (presetValue != SL_ANDROID_RECORDING_PRESET_NONE) {
        result = (*configItf)->SetConfiguration(configItf, SL_ANDROID_KEY_RECORDING_PRESET,
                &presetValue, sizeof(SLuint32));
        ExitOnError(result);
        fprintf(stdout, "Recorder parameterized with preset %u\n", presetValue);
    } else {
        printf("Using default record preset\n");
    }

    SLuint32 presetRetrieved = SL_ANDROID_RECORDING_PRESET_NONE;
    SLuint32 presetSize = 2*sizeof(SLuint32); // intentionally too big
    result = (*configItf)->GetConfiguration(configItf, SL_ANDROID_KEY_RECORDING_PRESET,
            &presetSize, (void*)&presetRetrieved);
    ExitOnError(result);
    if (presetValue == SL_ANDROID_RECORDING_PRESET_NONE) {
        printf("The default record preset appears to be %u\n", presetRetrieved);
    } else if (presetValue != presetRetrieved) {
        fprintf(stderr, "Error retrieving recording preset as %u instead of %u\n", presetRetrieved,
                presetValue);
        ExitOnError(SL_RESULT_INTERNAL_ERROR);
    }

    /* Realize the recorder in synchronous mode. */
    result = (*recorder)->Realize(recorder, SL_BOOLEAN_FALSE);
    ExitOnError(result);
    fprintf(stdout, "Recorder realized\n");

    /* Get the record interface which is implicit */
    result = (*recorder)->GetInterface(recorder, SL_IID_RECORD, (void*)&recordItf);
    ExitOnError(result);

    /* Set up the recorder callback to get events during the recording */
    result = (*recordItf)->SetMarkerPosition(recordItf, 2000);
    ExitOnError(result);
    result = (*recordItf)->SetPositionUpdatePeriod(recordItf, 500);
    ExitOnError(result);
    result = (*recordItf)->SetCallbackEventsMask(recordItf,
            SL_RECORDEVENT_HEADATMARKER | SL_RECORDEVENT_HEADATNEWPOS);
    ExitOnError(result);
    result = (*recordItf)->RegisterCallback(recordItf, RecCallback, NULL);
    ExitOnError(result);
    fprintf(stdout, "Recorder callback registered\n");

    /* Get the buffer queue interface which was explicitly requested */
    result = (*recorder)->GetInterface(recorder, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
            (void*)&recBuffQueueItf);
    ExitOnError(result);

    /* ------------------------------------------------------ */
    /* Initialize the callback and its context for the recording buffer queue */
    CallbackCntxt cntxt;
    cntxt.pDataBase = (int8_t*)&pcmData;
    cntxt.pData = cntxt.pDataBase;
    cntxt.size = sizeof(pcmData);
    result = (*recBuffQueueItf)->RegisterCallback(recBuffQueueItf, RecBufferQueueCallback, &cntxt);
    ExitOnError(result);

    /* Enqueue buffers to map the region of memory allocated to store the recorded data */
    fprintf(stdout,"Enqueueing buffer ");
    for(int i = 0 ; i < NB_BUFFERS_IN_QUEUE ; i++) {
        fprintf(stdout,"%d ", i);
        result = (*recBuffQueueItf)->Enqueue(recBuffQueueItf, cntxt.pData, BUFFER_SIZE_IN_BYTES);
        ExitOnError(result);
        cntxt.pData += BUFFER_SIZE_IN_BYTES;
    }
    fprintf(stdout,"\n");
    cntxt.pData = cntxt.pDataBase;

    /* ------------------------------------------------------ */
    /* Start recording */
    result = (*recordItf)->SetRecordState(recordItf, SL_RECORDSTATE_RECORDING);
    ExitOnError(result);
    fprintf(stdout, "Starting to record\n");

    /* Record for at least a second */
    if (durationInSeconds < 1) {
        durationInSeconds = 1;
    }
    usleep(durationInSeconds * 1000 * 1000);

    /* ------------------------------------------------------ */
    /* End of recording */

    /* Stop recording */
    result = (*recordItf)->SetRecordState(recordItf, SL_RECORDSTATE_STOPPED);
    ExitOnError(result);
    fprintf(stdout, "Stopped recording\n");

    /* Destroy the AudioRecorder object */
    (*recorder)->Destroy(recorder);

    fclose(gFp);
}

//-----------------------------------------------------------------
int main(int argc, char* const argv[])
{
    SLresult    result;
    SLObjectItf sl;

    const char *prog = argv[0];
    fprintf(stdout, "OpenSL ES test %s: exercises SLRecordItf and SLAndroidSimpleBufferQueueItf ",
            prog);
    fprintf(stdout, "on an AudioRecorder object\n");

    int i;
    for (i = 1; i < argc; ++i) {
        const char *arg = argv[i];
        if (arg[0] != '-') {
            break;
        }
        switch (arg[1]) {
        case 'p':   // preset number
            presetValue = atoi(&arg[2]);
            break;
        default:
            fprintf(stderr, "%s: unknown option %s\n", prog, arg);
            break;
        }
    }

    if (argc-i < 2) {
        printf("Usage: \t%s [-p#] destination_file duration_in_seconds\n", prog);
        printf("  -p# is the preset value which defaults to SL_ANDROID_RECORDING_PRESET_NONE\n");
        printf("  possible values are:\n");
        printf("    -p%d SL_ANDROID_RECORDING_PRESET_NONE\n",
                SL_ANDROID_RECORDING_PRESET_NONE);
        printf("    -p%d SL_ANDROID_RECORDING_PRESET_GENERIC\n",
                SL_ANDROID_RECORDING_PRESET_GENERIC);
        printf("    -p%d SL_ANDROID_RECORDING_PRESET_CAMCORDER\n",
                SL_ANDROID_RECORDING_PRESET_CAMCORDER);
        printf("    -p%d SL_ANDROID_RECORDING_PRESET_VOICE_RECOGNITION\n",
                SL_ANDROID_RECORDING_PRESET_VOICE_RECOGNITION);
        printf("    -p%d SL_ANDROID_RECORDING_PRESET_VOICE_COMMUNICATION\n",
                SL_ANDROID_RECORDING_PRESET_VOICE_COMMUNICATION);
        printf("Example: \"%s /sdcard/myrec.raw 4\" \n", prog);
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

    TestRecToBuffQueue(sl, argv[i], (SLAint64)atoi(argv[i+1]));

    /* Shutdown OpenSL ES */
    (*sl)->Destroy(sl);

    return EXIT_SUCCESS;
}
