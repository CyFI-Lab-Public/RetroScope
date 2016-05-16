/*
 * Copyright (C) 2011 The Android Open Source Project
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

/* AAC ADTS Decode Test

First run the program from shell:
  # slesTestDecodeAac /sdcard/myFile.adts

Expected output:
  OpenSL ES test slesTestDecodeAac: decodes a file containing AAC ADTS data
  Player created
  Player realized
  Enqueueing initial empty buffers to receive decoded PCM data 0 1
  Enqueueing initial full buffers of encoded ADTS data 0 1
  Starting to decode
  Frame counters: encoded=4579 decoded=4579

These use adb on host to retrieve the decoded file:
  % adb pull /sdcard/myFile.adts.raw myFile.raw

How to examine the output with Audacity:
 Project / Import raw data
 Select myFile.raw file, then click Open button
 Choose these options:
  Signed 16-bit PCM
  Little-endian
  1 Channel (Mono) / 2 Channels (Stereo) based on the PCM information obtained when decoding
  Sample rate based on the PCM information obtained when decoding
 Click Import button

*/

#define QUERY_METADATA

#include <assert.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/time.h>
#include <fcntl.h>
#include <pthread.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#include <cpustats/CentralTendencyStatistics.h>

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

/* Explicitly requesting SL_IID_ANDROIDBUFFERQUEUE and SL_IID_ANDROIDSIMPLEBUFFERQUEUE
 * on the AudioPlayer object for decoding, and
 * SL_IID_METADATAEXTRACTION for retrieving the format of the decoded audio.
 */
#define NUM_EXPLICIT_INTERFACES_FOR_PLAYER 4

/* Number of decoded samples produced by one AAC frame; defined by the standard */
#define SAMPLES_PER_AAC_FRAME 1024
/* Size of the encoded AAC ADTS buffer queue */
#define NB_BUFFERS_IN_ADTS_QUEUE 2 // 2 to 4 is typical

/* Size of the decoded PCM buffer queue */
#define NB_BUFFERS_IN_PCM_QUEUE 2  // 2 to 4 is typical
/* Size of each PCM buffer in the queue */
#define BUFFER_SIZE_IN_BYTES   (2*sizeof(short)*SAMPLES_PER_AAC_FRAME)

/* Local storage for decoded PCM audio data */
int8_t pcmData[NB_BUFFERS_IN_PCM_QUEUE * BUFFER_SIZE_IN_BYTES];

/* destination for decoded data */
static FILE* outputFp;

#ifdef QUERY_METADATA
/* metadata key index for the PCM format information we want to retrieve */
static int channelCountKeyIndex = -1;
static int sampleRateKeyIndex = -1;
static int bitsPerSampleKeyIndex = -1;
static int containerSizeKeyIndex = -1;
static int channelMaskKeyIndex = -1;
static int endiannessKeyIndex = -1;
/* size of the struct to retrieve the PCM format metadata values: the values we're interested in
 * are SLuint32, but it is saved in the data field of a SLMetadataInfo, hence the larger size.
 * Note that this size is queried and displayed at l.XXX for demonstration/test purposes.
 *  */
#define PCM_METADATA_VALUE_SIZE 32
/* used to query metadata values */
static SLMetadataInfo *pcmMetaData = NULL;
/* we only want to query / display the PCM format once */
static bool formatQueried = false;
#endif

/* to signal to the test app that the end of the encoded ADTS stream has been reached */
bool eos = false;
bool endOfEncodedStream = false;

void *ptr;
unsigned char *frame;
size_t filelen;
size_t encodedFrames = 0;
size_t encodedSamples = 0;
size_t decodedFrames = 0;
size_t decodedSamples = 0;
size_t totalEncodeCompletions = 0;     // number of Enqueue completions received
CentralTendencyStatistics frameStats;
size_t pauseFrame = 0;              // pause after this many decoded frames, zero means don't pause
SLboolean createRaw = SL_BOOLEAN_TRUE; // whether to create a .raw file containing PCM data

/* constant to identify a buffer context which is the end of the stream to decode */
static const int kEosBufferCntxt = 1980; // a magic value we can compare against

/* protects shared variables */
pthread_mutex_t eosLock = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t eosCondition = PTHREAD_COND_INITIALIZER;

// These are extensions to OpenMAX AL 1.0.1 values

#define PREFETCHSTATUS_UNKNOWN ((SLuint32) 0)
#define PREFETCHSTATUS_ERROR   ((SLuint32) (-1))

// Mutex and condition shared with main program to protect prefetch_status

static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t cond = PTHREAD_COND_INITIALIZER;
SLuint32 prefetch_status = PREFETCHSTATUS_UNKNOWN;

/* used to detect errors likely to have occured when the OpenSL ES framework fails to open
 * a resource, for instance because a file URI is invalid, or an HTTP server doesn't respond.
 */
#define PREFETCHEVENT_ERROR_CANDIDATE \
        (SL_PREFETCHEVENT_STATUSCHANGE | SL_PREFETCHEVENT_FILLLEVELCHANGE)

//-----------------------------------------------------------------
/* Exits the application if an error is encountered */
#define ExitOnError(x) ExitOnErrorFunc(x,__LINE__)

void ExitOnErrorFunc( SLresult result , int line)
{
    if (SL_RESULT_SUCCESS != result) {
        fprintf(stderr, "Error code %u encountered at line %d, exiting\n", result, line);
        exit(EXIT_FAILURE);
    }
}

//-----------------------------------------------------------------
/* Callback for "prefetch" events, here used to detect audio resource opening errors */
void PrefetchEventCallback(SLPrefetchStatusItf caller, void *pContext, SLuint32 event)
{
    // pContext is unused here, so we pass NULL
    assert(pContext == NULL);
    SLpermille level = 0;
    SLresult result;
    result = (*caller)->GetFillLevel(caller, &level);
    ExitOnError(result);
    SLuint32 status;
    result = (*caller)->GetPrefetchStatus(caller, &status);
    ExitOnError(result);
    printf("prefetch level=%d status=0x%x event=%d\n", level, status, event);
    SLuint32 new_prefetch_status;
    if ((PREFETCHEVENT_ERROR_CANDIDATE == (event & PREFETCHEVENT_ERROR_CANDIDATE))
            && (level == 0) && (status == SL_PREFETCHSTATUS_UNDERFLOW)) {
        printf("PrefetchEventCallback: Error while prefetching data, exiting\n");
        new_prefetch_status = PREFETCHSTATUS_ERROR;
    } else if (event == SL_PREFETCHEVENT_STATUSCHANGE) {
        new_prefetch_status = status;
    } else {
        return;
    }
    int ok;
    ok = pthread_mutex_lock(&mutex);
    assert(ok == 0);
    prefetch_status = new_prefetch_status;
    ok = pthread_cond_signal(&cond);
    assert(ok == 0);
    ok = pthread_mutex_unlock(&mutex);
    assert(ok == 0);
}

//-----------------------------------------------------------------
/* Structure for passing information to callback function */
typedef struct CallbackCntxt_ {
#ifdef QUERY_METADATA
    SLMetadataExtractionItf metaItf;
#endif
    SLPlayItf playItf;
    SLint8*   pDataBase;    // Base address of local audio data storage
    SLint8*   pData;        // Current address of local audio data storage
} CallbackCntxt;

// used to notify when SL_PLAYEVENT_HEADATEND event is received
static pthread_mutex_t head_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t head_cond = PTHREAD_COND_INITIALIZER;
static SLboolean head_atend = SL_BOOLEAN_FALSE;

//-----------------------------------------------------------------
/* Callback for SLPlayItf through which we receive the SL_PLAYEVENT_HEADATEND event */
void PlayCallback(SLPlayItf caller, void *pContext, SLuint32 event) {
    SLmillisecond position;
    SLresult res = (*caller)->GetPosition(caller, &position);
    ExitOnError(res);
    if (event & SL_PLAYEVENT_HEADATMARKER) {
        printf("SL_PLAYEVENT_HEADATMARKER position=%u ms\n", position);
    }
    if (event & SL_PLAYEVENT_HEADATNEWPOS) {
        printf("SL_PLAYEVENT_HEADATNEWPOS position=%u ms\n", position);
    }
    if (event & SL_PLAYEVENT_HEADATEND) {
        printf("SL_PLAYEVENT_HEADATEND position=%u ms, all decoded data has been received\n",
                position);
        pthread_mutex_lock(&head_mutex);
        head_atend = SL_BOOLEAN_TRUE;
        pthread_cond_signal(&head_cond);
        pthread_mutex_unlock(&head_mutex);
    }
}

//-----------------------------------------------------------------
/* Callback for AndroidBufferQueueItf through which we supply ADTS buffers */
SLresult AndroidBufferQueueCallback(
        SLAndroidBufferQueueItf caller,
        void *pCallbackContext,        /* input */
        void *pBufferContext,          /* input */
        void *pBufferData,             /* input */
        SLuint32 dataSize,             /* input */
        SLuint32 dataUsed,             /* input */
        const SLAndroidBufferItem *pItems,/* input */
        SLuint32 itemsLength           /* input */)
{
    // mutex on all global variables
    pthread_mutex_lock(&eosLock);
    SLresult res;

    // for demonstration purposes:
    // verify what type of information was enclosed in the processed buffer
    if (NULL != pBufferContext) {
        if (&kEosBufferCntxt == pBufferContext) {
            fprintf(stdout, "EOS was processed\n");
        }
    }

    ++totalEncodeCompletions;
    if (endOfEncodedStream) {
        // we continue to receive acknowledgement after each buffer was processed
        if (pBufferContext == (void *) &kEosBufferCntxt) {
            printf("Received EOS completion after EOS\n");
        } else if (pBufferContext == NULL) {
            printf("Received ADTS completion after EOS\n");
        } else {
            fprintf(stderr, "Received acknowledgement after EOS with unexpected context %p\n",
                    pBufferContext);
        }
    } else if (filelen == 0) {
        // signal EOS to the decoder rather than just starving it
        printf("Enqueue EOS: encoded frames=%u, decoded frames=%u\n", encodedFrames, decodedFrames);
        printf("You should now see %u ADTS completion%s followed by 1 EOS completion\n",
                NB_BUFFERS_IN_ADTS_QUEUE - 1, NB_BUFFERS_IN_ADTS_QUEUE != 2 ? "s" : "");
        SLAndroidBufferItem msgEos;
        msgEos.itemKey = SL_ANDROID_ITEMKEY_EOS;
        msgEos.itemSize = 0;
        // EOS message has no parameters, so the total size of the message is the size of the key
        //   plus the size of itemSize, both SLuint32
        res = (*caller)->Enqueue(caller, (void *)&kEosBufferCntxt /*pBufferContext*/,
                NULL /*pData*/, 0 /*dataLength*/,
                &msgEos /*pMsg*/,
                sizeof(SLuint32)*2 /*msgLength*/);
        ExitOnError(res);
        endOfEncodedStream = true;
    // verify that we are at start of an ADTS frame
    } else if (!(filelen < 7 || frame[0] != 0xFF || (frame[1] & 0xF0) != 0xF0)) {
        if (pBufferContext != NULL) {
            fprintf(stderr, "Received acknowledgement before EOS with unexpected context %p\n",
                    pBufferContext);
        }
        unsigned framelen = ((frame[3] & 3) << 11) | (frame[4] << 3) | (frame[5] >> 5);
        if (framelen <= filelen) {
            // push more data to the queue
            res = (*caller)->Enqueue(caller, NULL /*pBufferContext*/,
                    frame, framelen, NULL, 0);
            ExitOnError(res);
            frame += framelen;
            filelen -= framelen;
            ++encodedFrames;
            encodedSamples += SAMPLES_PER_AAC_FRAME;
            frameStats.sample(framelen);
        } else {
            fprintf(stderr,
                    "partial ADTS frame at EOF discarded; offset=%u, framelen=%u, filelen=%u\n",
                    frame - (unsigned char *) ptr, framelen, filelen);
            frame += filelen;
            filelen = 0;
        }
    } else {
        fprintf(stderr, "corrupt ADTS frame encountered; offset=%u, filelen=%u\n",
                frame - (unsigned char *) ptr, filelen);
        frame += filelen;
        filelen = 0;
    }
    pthread_mutex_unlock(&eosLock);

    return SL_RESULT_SUCCESS;
}

//-----------------------------------------------------------------
/* Callback for decoding buffer queue events */
void DecPlayCallback(
        SLAndroidSimpleBufferQueueItf queueItf,
        void *pContext)
{
    // mutex on all global variables
    pthread_mutex_lock(&eosLock);

    CallbackCntxt *pCntxt = (CallbackCntxt*)pContext;

    /* Save the decoded data to output file */
    if (outputFp != NULL && fwrite(pCntxt->pData, 1, BUFFER_SIZE_IN_BYTES, outputFp)
                < BUFFER_SIZE_IN_BYTES) {
        fprintf(stderr, "Error writing to output file");
    }

    /* Re-enqueue the now empty buffer */
    SLresult res;
    res = (*queueItf)->Enqueue(queueItf, pCntxt->pData, BUFFER_SIZE_IN_BYTES);
    ExitOnError(res);

    /* Increase data pointer by buffer size, with circular wraparound */
    pCntxt->pData += BUFFER_SIZE_IN_BYTES;
    if (pCntxt->pData >= pCntxt->pDataBase + (NB_BUFFERS_IN_PCM_QUEUE * BUFFER_SIZE_IN_BYTES)) {
        pCntxt->pData = pCntxt->pDataBase;
    }

    // Note: adding a sleep here or any sync point is a way to slow down the decoding, or
    //  synchronize it with some other event, as the OpenSL ES framework will block until the
    //  buffer queue callback return to proceed with the decoding.

#ifdef QUERY_METADATA
    /* Example: query of the decoded PCM format */
    if (!formatQueried) {
        /* memory to receive the PCM format metadata */
        union {
            SLMetadataInfo pcmMetaData;
            char withData[PCM_METADATA_VALUE_SIZE];
        } u;
        res = (*pCntxt->metaItf)->GetValue(pCntxt->metaItf, sampleRateKeyIndex,
                PCM_METADATA_VALUE_SIZE, &u.pcmMetaData);  ExitOnError(res);
        // Note: here we could verify the following:
        //         u.pcmMetaData->encoding == SL_CHARACTERENCODING_BINARY
        //         u.pcmMetaData->size == sizeof(SLuint32)
        //      but the call was successful for the PCM format keys, so those conditions are implied
        printf("sample rate = %d\n", *((SLuint32*)u.pcmMetaData.data));
        res = (*pCntxt->metaItf)->GetValue(pCntxt->metaItf, channelCountKeyIndex,
                PCM_METADATA_VALUE_SIZE, &u.pcmMetaData);  ExitOnError(res);
        printf("channel count = %d\n", *((SLuint32*)u.pcmMetaData.data));
        res = (*pCntxt->metaItf)->GetValue(pCntxt->metaItf, bitsPerSampleKeyIndex,
                PCM_METADATA_VALUE_SIZE, &u.pcmMetaData);  ExitOnError(res);
        printf("bits per sample = %d bits\n", *((SLuint32*)u.pcmMetaData.data));
        res = (*pCntxt->metaItf)->GetValue(pCntxt->metaItf, containerSizeKeyIndex,
                PCM_METADATA_VALUE_SIZE, &u.pcmMetaData);  ExitOnError(res);
        printf("container size = %d bits\n", *((SLuint32*)u.pcmMetaData.data));
        res = (*pCntxt->metaItf)->GetValue(pCntxt->metaItf, channelMaskKeyIndex,
                PCM_METADATA_VALUE_SIZE, &u.pcmMetaData);  ExitOnError(res);
        printf("channel mask = 0x%X (0x3=front left | front right, 0x4=front center)\n",
                *((SLuint32*)u.pcmMetaData.data));
        res = (*pCntxt->metaItf)->GetValue(pCntxt->metaItf, endiannessKeyIndex,
                PCM_METADATA_VALUE_SIZE, &u.pcmMetaData);  ExitOnError(res);
        printf("endianness = %d (1=big, 2=little)\n", *((SLuint32*)u.pcmMetaData.data));
        formatQueried = true;
    }
#endif

    ++decodedFrames;
    decodedSamples += SAMPLES_PER_AAC_FRAME;

    /* Periodically ask for position and duration */
    if ((decodedFrames % 1000 == 0) || endOfEncodedStream) {
        SLmillisecond position;
        res = (*pCntxt->playItf)->GetPosition(pCntxt->playItf, &position);
        ExitOnError(res);
        SLmillisecond duration;
        res = (*pCntxt->playItf)->GetDuration(pCntxt->playItf, &duration);
        ExitOnError(res);
        if (duration == SL_TIME_UNKNOWN) {
            printf("After %u encoded %u decoded frames: position is %u ms, duration is "
                    "unknown as expected\n",
                    encodedFrames, decodedFrames, position);
        } else {
            printf("After %u encoded %u decoded frames: position is %u ms, duration is "
                    "surprisingly %u ms\n",
                    encodedFrames, decodedFrames, position, duration);
        }
    }

    if (endOfEncodedStream && decodedSamples >= encodedSamples) {
        eos = true;
        pthread_cond_signal(&eosCondition);
    }
    pthread_mutex_unlock(&eosLock);
}

//-----------------------------------------------------------------

/* Decode an audio path by opening a file descriptor on that path  */
void TestDecToBuffQueue( SLObjectItf sl, const char *path, int fd)
{
    // check what kind of object it is
    int ok;
    struct stat statbuf;
    ok = fstat(fd, &statbuf);
    if (ok < 0) {
        perror(path);
        return;
    }

    // verify that's it is a file
    if (!S_ISREG(statbuf.st_mode)) {
        fprintf(stderr, "%s: not an ordinary file\n", path);
        return;
    }

    // map file contents into memory to make it easier to access the ADTS frames directly
    ptr = mmap(NULL, statbuf.st_size, PROT_READ, MAP_FILE | MAP_PRIVATE, fd, (off_t) 0);
    if (ptr == MAP_FAILED) {
        perror(path);
        return;
    }
    frame = (unsigned char *) ptr;
    filelen = statbuf.st_size;

    // create PCM .raw file
    if (createRaw) {
        size_t len = strlen((const char *) path);
        char* outputPath = (char*) malloc(len + 4 + 1); // save room to concatenate ".raw"
        if (NULL == outputPath) {
            ExitOnError(SL_RESULT_RESOURCE_ERROR);
        }
        memcpy(outputPath, path, len + 1);
        strcat(outputPath, ".raw");
        outputFp = fopen(outputPath, "w");
        if (NULL == outputFp) {
            // issue an error message, but continue the decoding anyway
            perror(outputPath);
        }
    } else {
        outputFp = NULL;
    }

    SLresult res;
    SLEngineItf EngineItf;

    /* Objects this application uses: one audio player */
    SLObjectItf  player;

    /* Interfaces for the audio player */
    SLPlayItf                     playItf;
#ifdef QUERY_METADATA
    /*   to retrieve the decoded PCM format */
    SLMetadataExtractionItf       mdExtrItf;
#endif
    /*   to retrieve the PCM samples */
    SLAndroidSimpleBufferQueueItf decBuffQueueItf;
    /*   to queue the AAC data to decode */
    SLAndroidBufferQueueItf       aacBuffQueueItf;
    /*   for prefetch status */
    SLPrefetchStatusItf           prefetchItf;

    SLboolean required[NUM_EXPLICIT_INTERFACES_FOR_PLAYER];
    SLInterfaceID iidArray[NUM_EXPLICIT_INTERFACES_FOR_PLAYER];

    /* Get the SL Engine Interface which is implicit */
    res = (*sl)->GetInterface(sl, SL_IID_ENGINE, (void*)&EngineItf);
    ExitOnError(res);

    /* Initialize arrays required[] and iidArray[] */
    unsigned int i;
    for (i=0 ; i < NUM_EXPLICIT_INTERFACES_FOR_PLAYER ; i++) {
        required[i] = SL_BOOLEAN_FALSE;
        iidArray[i] = SL_IID_NULL;
    }

    /* ------------------------------------------------------ */
    /* Configuration of the player  */

    /* Request the AndroidSimpleBufferQueue interface */
    required[0] = SL_BOOLEAN_TRUE;
    iidArray[0] = SL_IID_ANDROIDSIMPLEBUFFERQUEUE;
    /* Request the AndroidBufferQueue interface */
    required[1] = SL_BOOLEAN_TRUE;
    iidArray[1] = SL_IID_ANDROIDBUFFERQUEUESOURCE;
    /* Request the PrefetchStatus interface */
    required[2] = SL_BOOLEAN_TRUE;
    iidArray[2] = SL_IID_PREFETCHSTATUS;
#ifdef QUERY_METADATA
    /* Request the MetadataExtraction interface */
    required[3] = SL_BOOLEAN_TRUE;
    iidArray[3] = SL_IID_METADATAEXTRACTION;
#endif

    /* Setup the data source for queueing AAC buffers of ADTS data */
    SLDataLocator_AndroidBufferQueue loc_srcAbq = {
            SL_DATALOCATOR_ANDROIDBUFFERQUEUE /*locatorType*/,
            NB_BUFFERS_IN_ADTS_QUEUE          /*numBuffers*/};
    SLDataFormat_MIME format_srcMime = {
            SL_DATAFORMAT_MIME         /*formatType*/,
            SL_ANDROID_MIME_AACADTS    /*mimeType*/,
            SL_CONTAINERTYPE_RAW       /*containerType*/};
    SLDataSource decSource = {&loc_srcAbq /*pLocator*/, &format_srcMime /*pFormat*/};

    /* Setup the data sink, a buffer queue for buffers of PCM data */
    SLDataLocator_AndroidSimpleBufferQueue loc_destBq = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE/*locatorType*/,
            NB_BUFFERS_IN_PCM_QUEUE                /*numBuffers*/ };

    /*    declare we're decoding to PCM, the parameters after that need to be valid,
          but are ignored, the decoded format will match the source */
    SLDataFormat_PCM format_destPcm = { /*formatType*/ SL_DATAFORMAT_PCM, /*numChannels*/ 1,
            /*samplesPerSec*/ SL_SAMPLINGRATE_8, /*pcm.bitsPerSample*/ SL_PCMSAMPLEFORMAT_FIXED_16,
            /*/containerSize*/ 16, /*channelMask*/ SL_SPEAKER_FRONT_LEFT,
            /*endianness*/ SL_BYTEORDER_LITTLEENDIAN };
    SLDataSink decDest = {&loc_destBq /*pLocator*/, &format_destPcm /*pFormat*/};

    /* Create the audio player */
    res = (*EngineItf)->CreateAudioPlayer(EngineItf, &player, &decSource, &decDest,
#ifdef QUERY_METADATA
            NUM_EXPLICIT_INTERFACES_FOR_PLAYER,
#else
            NUM_EXPLICIT_INTERFACES_FOR_PLAYER - 1,
#endif
            iidArray, required);
    ExitOnError(res);
    printf("Player created\n");

    /* Realize the player in synchronous mode. */
    res = (*player)->Realize(player, SL_BOOLEAN_FALSE);
    ExitOnError(res);
    printf("Player realized\n");

    /* Get the play interface which is implicit */
    res = (*player)->GetInterface(player, SL_IID_PLAY, (void*)&playItf);
    ExitOnError(res);

    /* Enable callback when position passes through a marker (SL_PLAYEVENT_HEADATMARKER) */
    res = (*playItf)->SetMarkerPosition(playItf, 5000);
    ExitOnError(res);

    /* Enable callback for periodic position updates (SL_PLAYEVENT_HEADATNEWPOS) */
    res = (*playItf)->SetPositionUpdatePeriod(playItf, 3000);
    ExitOnError(res);

    /* Use the play interface to set up a callback for the SL_PLAYEVENT_HEAD* events */
    res = (*playItf)->SetCallbackEventsMask(playItf,
            SL_PLAYEVENT_HEADATMARKER | SL_PLAYEVENT_HEADATNEWPOS | SL_PLAYEVENT_HEADATEND);
    ExitOnError(res);
    res = (*playItf)->RegisterCallback(playItf, PlayCallback /*callback*/, NULL /*pContext*/);
    ExitOnError(res);

    /* Get the position before prefetch; should be zero */
    SLmillisecond position;
    res = (*playItf)->GetPosition(playItf, &position);
    ExitOnError(res);
    if (position == 0) {
        printf("The position before prefetch is zero as expected\n");
    } else if (position == SL_TIME_UNKNOWN) {
        printf("That's surprising the position before prefetch is unknown");
    } else {
        printf("That's surprising the position before prefetch is %u ms\n", position);
    }

    /* Get the duration before prefetch; should be unknown */
    SLmillisecond duration;
    res = (*playItf)->GetDuration(playItf, &duration);
    ExitOnError(res);
    if (duration == SL_TIME_UNKNOWN) {
        printf("The duration before prefetch is unknown as expected\n");
    } else {
        printf("That's surprising the duration before prefetch is %u ms\n", duration);
    }

    /* Get the buffer queue interface which was explicitly requested */
    res = (*player)->GetInterface(player, SL_IID_ANDROIDSIMPLEBUFFERQUEUE, (void*)&decBuffQueueItf);
    ExitOnError(res);

    /* Get the Android buffer queue interface which was explicitly requested */
    res = (*player)->GetInterface(player, SL_IID_ANDROIDBUFFERQUEUESOURCE, (void*)&aacBuffQueueItf);
    ExitOnError(res);

    /* Get the prefetch status interface which was explicitly requested */
    res = (*player)->GetInterface(player, SL_IID_PREFETCHSTATUS, (void*)&prefetchItf);
    ExitOnError(res);

#ifdef QUERY_METADATA
    /* Get the metadata extraction interface which was explicitly requested */
    res = (*player)->GetInterface(player, SL_IID_METADATAEXTRACTION, (void*)&mdExtrItf);
    ExitOnError(res);
#endif

    /* ------------------------------------------------------ */
    /* Initialize the callback and its context for the buffer queue of the decoded PCM */
    CallbackCntxt sinkCntxt;
    sinkCntxt.playItf = playItf;
#ifdef QUERY_METADATA
    sinkCntxt.metaItf = mdExtrItf;
#endif
    sinkCntxt.pDataBase = (int8_t*)&pcmData;
    sinkCntxt.pData = sinkCntxt.pDataBase;
    res = (*decBuffQueueItf)->RegisterCallback(decBuffQueueItf, DecPlayCallback, &sinkCntxt);
    ExitOnError(res);

    /* Enqueue buffers to map the region of memory allocated to store the decoded data */
    printf("Enqueueing initial empty buffers to receive decoded PCM data");
    for(i = 0 ; i < NB_BUFFERS_IN_PCM_QUEUE ; i++) {
        printf(" %d", i);
        res = (*decBuffQueueItf)->Enqueue(decBuffQueueItf, sinkCntxt.pData, BUFFER_SIZE_IN_BYTES);
        ExitOnError(res);
        sinkCntxt.pData += BUFFER_SIZE_IN_BYTES;
        if (sinkCntxt.pData >= sinkCntxt.pDataBase +
                (NB_BUFFERS_IN_PCM_QUEUE * BUFFER_SIZE_IN_BYTES)) {
            sinkCntxt.pData = sinkCntxt.pDataBase;
        }
    }
    printf("\n");

    /* ------------------------------------------------------ */
    /* Initialize the callback for prefetch errors, if we can't open the resource to decode */
    res = (*prefetchItf)->RegisterCallback(prefetchItf, PrefetchEventCallback, NULL);
    ExitOnError(res);
    res = (*prefetchItf)->SetCallbackEventsMask(prefetchItf, PREFETCHEVENT_ERROR_CANDIDATE);
    ExitOnError(res);

    /* Initialize the callback for the Android buffer queue of the encoded data */
    res = (*aacBuffQueueItf)->RegisterCallback(aacBuffQueueItf, AndroidBufferQueueCallback, NULL);
    ExitOnError(res);

    /* Enqueue the content of our encoded data before starting to play,
       we don't want to starve the player initially */
    printf("Enqueueing initial full buffers of encoded ADTS data");
    for (i=0 ; i < NB_BUFFERS_IN_ADTS_QUEUE ; i++) {
        if (filelen < 7 || frame[0] != 0xFF || (frame[1] & 0xF0) != 0xF0) {
            printf("\ncorrupt ADTS frame encountered; offset %zu bytes\n",
                    frame - (unsigned char *) ptr);
            // Note that prefetch will detect this error soon when it gets a premature EOF
            break;
        }
        unsigned framelen = ((frame[3] & 3) << 11) | (frame[4] << 3) | (frame[5] >> 5);
        printf(" %d (%u bytes)", i, framelen);
        res = (*aacBuffQueueItf)->Enqueue(aacBuffQueueItf, NULL /*pBufferContext*/,
                frame, framelen, NULL, 0);
        ExitOnError(res);
        frame += framelen;
        filelen -= framelen;
        ++encodedFrames;
        encodedSamples += SAMPLES_PER_AAC_FRAME;
        frameStats.sample(framelen);
    }
    printf("\n");

#ifdef QUERY_METADATA
    /* ------------------------------------------------------ */
    /* Get and display the metadata key names for the decoder */
    //   This is for test / demonstration purposes only where we discover the key and value sizes
    //   of a PCM decoder. An application that would want to directly get access to those values
    //   can make assumptions about the size of the keys and their matching values (all SLuint32),
    //   but it should not make assumptions about the key indices as these are subject to change.
    //   Note that we don't get the metadata values yet; that happens in the first decode callback.
    SLuint32 itemCount;
    res = (*mdExtrItf)->GetItemCount(mdExtrItf, &itemCount);
    ExitOnError(res);
    printf("itemCount=%u\n", itemCount);
    SLuint32 keySize, valueSize;
    SLMetadataInfo *keyInfo, *value;
    for(i=0 ; i<itemCount ; i++) {
        keyInfo = NULL; keySize = 0;
        value = NULL;   valueSize = 0;
        res = (*mdExtrItf)->GetKeySize(mdExtrItf, i, &keySize);
        ExitOnError(res);
        res = (*mdExtrItf)->GetValueSize(mdExtrItf, i, &valueSize);
        ExitOnError(res);
        keyInfo = (SLMetadataInfo*) malloc(keySize);
        if (NULL != keyInfo) {
            res = (*mdExtrItf)->GetKey(mdExtrItf, i, keySize, keyInfo);
            ExitOnError(res);
            printf("key[%d] size=%d, name=%s \tvalue size=%d encoding=0x%X langCountry=%s\n",
                    i, keyInfo->size, keyInfo->data, valueSize, keyInfo->encoding,
                    keyInfo->langCountry);
            /* find out the key index of the metadata we're interested in */
            if (!strcmp((char*)keyInfo->data, ANDROID_KEY_PCMFORMAT_NUMCHANNELS)) {
                channelCountKeyIndex = i;
            } else if (!strcmp((char*)keyInfo->data, ANDROID_KEY_PCMFORMAT_SAMPLERATE)) {
                sampleRateKeyIndex = i;
            } else if (!strcmp((char*)keyInfo->data, ANDROID_KEY_PCMFORMAT_BITSPERSAMPLE)) {
                bitsPerSampleKeyIndex = i;
            } else if (!strcmp((char*)keyInfo->data, ANDROID_KEY_PCMFORMAT_CONTAINERSIZE)) {
                containerSizeKeyIndex = i;
            } else if (!strcmp((char*)keyInfo->data, ANDROID_KEY_PCMFORMAT_CHANNELMASK)) {
                channelMaskKeyIndex = i;
            } else if (!strcmp((char*)keyInfo->data, ANDROID_KEY_PCMFORMAT_ENDIANNESS)) {
                endiannessKeyIndex = i;
            } else {
                printf("Unknown key %s ignored\n", (char *)keyInfo->data);
            }
            free(keyInfo);
        }
    }
    if (channelCountKeyIndex != -1) {
        printf("Key %s is at index %d\n",
                ANDROID_KEY_PCMFORMAT_NUMCHANNELS, channelCountKeyIndex);
    } else {
        fprintf(stderr, "Unable to find key %s\n", ANDROID_KEY_PCMFORMAT_NUMCHANNELS);
    }
    if (sampleRateKeyIndex != -1) {
        printf("Key %s is at index %d\n",
                ANDROID_KEY_PCMFORMAT_SAMPLERATE, sampleRateKeyIndex);
    } else {
        fprintf(stderr, "Unable to find key %s\n", ANDROID_KEY_PCMFORMAT_SAMPLERATE);
    }
    if (bitsPerSampleKeyIndex != -1) {
        printf("Key %s is at index %d\n",
                ANDROID_KEY_PCMFORMAT_BITSPERSAMPLE, bitsPerSampleKeyIndex);
    } else {
        fprintf(stderr, "Unable to find key %s\n", ANDROID_KEY_PCMFORMAT_BITSPERSAMPLE);
    }
    if (containerSizeKeyIndex != -1) {
        printf("Key %s is at index %d\n",
                ANDROID_KEY_PCMFORMAT_CONTAINERSIZE, containerSizeKeyIndex);
    } else {
        fprintf(stderr, "Unable to find key %s\n", ANDROID_KEY_PCMFORMAT_CONTAINERSIZE);
    }
    if (channelMaskKeyIndex != -1) {
        printf("Key %s is at index %d\n",
                ANDROID_KEY_PCMFORMAT_CHANNELMASK, channelMaskKeyIndex);
    } else {
        fprintf(stderr, "Unable to find key %s\n", ANDROID_KEY_PCMFORMAT_CHANNELMASK);
    }
    if (endiannessKeyIndex != -1) {
        printf("Key %s is at index %d\n",
                ANDROID_KEY_PCMFORMAT_ENDIANNESS, endiannessKeyIndex);
    } else {
        fprintf(stderr, "Unable to find key %s\n", ANDROID_KEY_PCMFORMAT_ENDIANNESS);
    }
#endif

    // set the player's state to paused, to start prefetching
    printf("Setting play state to PAUSED\n");
    res = (*playItf)->SetPlayState(playItf, SL_PLAYSTATE_PAUSED);
    ExitOnError(res);

    // wait for prefetch status callback to indicate either sufficient data or error
    printf("Awaiting prefetch complete\n");
    pthread_mutex_lock(&mutex);
    while (prefetch_status == PREFETCHSTATUS_UNKNOWN) {
        pthread_cond_wait(&cond, &mutex);
    }
    pthread_mutex_unlock(&mutex);
    if (prefetch_status == PREFETCHSTATUS_ERROR) {
        fprintf(stderr, "Error during prefetch, exiting\n");
        goto destroyRes;
    }
    printf("Prefetch is complete\n");

    /* ------------------------------------------------------ */
    /* Start decoding */
    printf("Starting to decode\n");
    res = (*playItf)->SetPlayState(playItf, SL_PLAYSTATE_PLAYING);
    ExitOnError(res);

    /* Decode until the end of the stream is reached */
    printf("Awaiting notification that all encoded buffers have been enqueued\n");
    pthread_mutex_lock(&eosLock);
    while (!eos) {
        if (pauseFrame > 0) {
            if (decodedFrames >= pauseFrame) {
                pauseFrame = 0;
                printf("Pausing after decoded frame %u for 10 seconds\n", decodedFrames);
                pthread_mutex_unlock(&eosLock);
                res = (*playItf)->SetPlayState(playItf, SL_PLAYSTATE_PAUSED);
                ExitOnError(res);
                sleep(10);
                printf("Resuming\n");
                res = (*playItf)->SetPlayState(playItf, SL_PLAYSTATE_PLAYING);
                ExitOnError(res);
                pthread_mutex_lock(&eosLock);
            } else {
                pthread_mutex_unlock(&eosLock);
                usleep(10*1000);
                pthread_mutex_lock(&eosLock);
            }
        } else {
            pthread_cond_wait(&eosCondition, &eosLock);
        }
    }
    pthread_mutex_unlock(&eosLock);
    printf("All encoded buffers have now been enqueued, but there's still more to do\n");

    /* This just means done enqueueing; there may still more data in decode queue! */
    pthread_mutex_lock(&head_mutex);
    while (!head_atend) {
        pthread_cond_wait(&head_cond, &head_mutex);
    }
    pthread_mutex_unlock(&head_mutex);
    printf("Decode is now finished\n");

    pthread_mutex_lock(&eosLock);
    printf("Frame counters: encoded=%u decoded=%u\n", encodedFrames, decodedFrames);
    printf("Sample counters: encoded=%u decoded=%u\n", encodedSamples, decodedSamples);
    printf("Total encode completions received: actual=%u, expected=%u\n",
            totalEncodeCompletions, encodedFrames+1/*EOS*/);
    pthread_mutex_unlock(&eosLock);

    /* Get the final position and duration */
    res = (*playItf)->GetPosition(playItf, &position);
    ExitOnError(res);
    res = (*playItf)->GetDuration(playItf, &duration);
    ExitOnError(res);
    if (duration == SL_TIME_UNKNOWN) {
        printf("The final position is %u ms, duration is unknown\n", position);
    } else {
        printf("The final position is %u ms, duration is %u ms\n", position, duration);
    }

    printf("Frame length statistics:\n");
    printf("  n = %u frames\n", frameStats.n());
    printf("  mean = %.1f bytes\n", frameStats.mean());
    printf("  minimum = %.1f bytes\n", frameStats.minimum());
    printf("  maximum = %.1f bytes\n", frameStats.maximum());
    printf("  stddev = %.1f bytes\n", frameStats.stddev());

    /* ------------------------------------------------------ */
    /* End of decoding */

destroyRes:
    /* Destroy the AudioPlayer object */
    (*player)->Destroy(player);

    if (outputFp != NULL) {
        fclose(outputFp);
    }

    // unmap the ADTS AAC file from memory
    ok = munmap(ptr, statbuf.st_size);
    if (0 != ok) {
        perror(path);
    }
}

//-----------------------------------------------------------------
int main(int argc, char* const argv[])
{
    SLresult    res;
    SLObjectItf sl;

    printf("OpenSL ES test %s: decodes a file containing AAC ADTS data\n", argv[0]);

    if (argc != 2) {
        printf("Usage: \t%s source_file\n", argv[0]);
        printf("Example: \"%s /sdcard/myFile.adts\n", argv[0]);
        exit(EXIT_FAILURE);
    }

    // open pathname of encoded ADTS AAC file to get a file descriptor
    int fd;
    fd = open(argv[1], O_RDONLY);
    if (fd < 0) {
        perror(argv[1]);
        return EXIT_FAILURE;
    }

    SLEngineOption EngineOption[] = {
            {(SLuint32) SL_ENGINEOPTION_THREADSAFE, (SLuint32) SL_BOOLEAN_TRUE}
    };

    res = slCreateEngine( &sl, 1, EngineOption, 0, NULL, NULL);
    ExitOnError(res);

    /* Realizing the SL Engine in synchronous mode. */
    res = (*sl)->Realize(sl, SL_BOOLEAN_FALSE);
    ExitOnError(res);

    TestDecToBuffQueue(sl, argv[1], fd);

    /* Shutdown OpenSL ES */
    (*sl)->Destroy(sl);

    // close the file
    (void) close(fd);

    return EXIT_SUCCESS;
}
