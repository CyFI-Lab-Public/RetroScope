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

// OpenMAX AL MediaPlayer command-line player

#include <assert.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#include <OMXAL/OpenMAXAL.h>
#include <OMXAL/OpenMAXAL_Android.h>
#include "nativewindow.h"

#define MPEG2TS_PACKET_SIZE 188  // MPEG-2 transport stream packet size in bytes
#define PACKETS_PER_BUFFER 20    // Number of MPEG-2 transport stream packets per buffer

#define NB_BUFFERS 2    // Number of buffers in Android buffer queue

// MPEG-2 transport stream packet
typedef struct {
    char data[MPEG2TS_PACKET_SIZE];
} MPEG2TS_Packet;

// Globals shared between main thread and buffer queue callback
MPEG2TS_Packet *packets;
size_t totalPackets;    // total number of packets in input file
size_t numPackets;      // number of packets to play, defaults to totalPackets - firstPacket
size_t curPacket;       // current packet index
size_t discPacket;      // discontinuity packet index, defaults to no discontinuity requested
size_t afterDiscPacket; // packet index to switch to after the discontinuity
size_t firstPacket;     // first packet index to be played, defaults to zero
size_t lastPacket;      // last packet index to be played
size_t formatPacket;    // format change packet index, defaults to no format change requested
XAmillisecond seekPos = XA_TIME_UNKNOWN;    // seek to this position initially
int pauseMs = -1;       // pause after this many ms into playback
XAboolean forceCallbackFailure = XA_BOOLEAN_FALSE;  // force callback failures occasionally
XAboolean sentEOS = XA_BOOLEAN_FALSE;   // whether we have enqueued EOS yet

// These are extensions to OpenMAX AL 1.0.1 values

#define PREFETCHSTATUS_UNKNOWN ((XAuint32) 0)
#define PREFETCHSTATUS_ERROR   ((XAuint32) (-1))

// Mutex and condition shared with main program to protect prefetch_status

static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t cond = PTHREAD_COND_INITIALIZER;
XAuint32 prefetch_status = PREFETCHSTATUS_UNKNOWN;

/* used to detect errors likely to have occured when the OpenMAX AL framework fails to open
 * a resource, for instance because a file URI is invalid, or an HTTP server doesn't respond.
 */
#define PREFETCHEVENT_ERROR_CANDIDATE \
        (XA_PREFETCHEVENT_STATUSCHANGE | XA_PREFETCHEVENT_FILLLEVELCHANGE)

// stream event change callback
void streamEventChangeCallback(XAStreamInformationItf caller, XAuint32 eventId,
        XAuint32 streamIndex, void *pEventData, void *pContext)
{
    // context parameter is specified as NULL and is unused here
    assert(NULL == pContext);
    switch (eventId) {
    case XA_STREAMCBEVENT_PROPERTYCHANGE:
        printf("XA_STREAMCBEVENT_PROPERTYCHANGE on stream index %u, pEventData %p\n", streamIndex,
                pEventData);
        break;
    default:
        printf("Unknown stream event ID %u\n", eventId);
        break;
    }
}

// prefetch status callback
void prefetchStatusCallback(XAPrefetchStatusItf caller,  void *pContext, XAuint32 event)
{
    // pContext is unused here, so we pass NULL
    assert(pContext == NULL);
    XApermille level = 0;
    XAresult result;
    result = (*caller)->GetFillLevel(caller, &level);
    assert(XA_RESULT_SUCCESS == result);
    XAuint32 status;
    result = (*caller)->GetPrefetchStatus(caller, &status);
    assert(XA_RESULT_SUCCESS == result);
    if (event & XA_PREFETCHEVENT_FILLLEVELCHANGE) {
        printf("PrefetchEventCallback: Buffer fill level is = %d\n", level);
    }
    if (event & XA_PREFETCHEVENT_STATUSCHANGE) {
        printf("PrefetchEventCallback: Prefetch Status is = %u\n", status);
    }
    XAuint32 new_prefetch_status;
    if ((PREFETCHEVENT_ERROR_CANDIDATE == (event & PREFETCHEVENT_ERROR_CANDIDATE))
            && (level == 0) && (status == XA_PREFETCHSTATUS_UNDERFLOW)) {
        printf("PrefetchEventCallback: Error while prefetching data, exiting\n");
        new_prefetch_status = PREFETCHSTATUS_ERROR;
    } else if (event == XA_PREFETCHEVENT_STATUSCHANGE) {
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

// playback event callback
void playEventCallback(XAPlayItf caller, void *pContext, XAuint32 event)
{
    // pContext is unused here, so we pass NULL
    assert(NULL == pContext);

    XAresult result;
    XAmillisecond position;
    result = (*caller)->GetPosition(caller, &position);
    assert(XA_RESULT_SUCCESS == result);

    if (XA_PLAYEVENT_HEADATEND & event) {
        printf("XA_PLAYEVENT_HEADATEND current position=%u ms\n", position);
    }

    if (XA_PLAYEVENT_HEADATNEWPOS & event) {
        printf("XA_PLAYEVENT_HEADATNEWPOS current position=%u ms\n", position);
    }

    if (XA_PLAYEVENT_HEADATMARKER & event) {
        printf("XA_PLAYEVENT_HEADATMARKER current position=%u ms\n", position);
    }
}

// Android buffer queue callback
XAresult bufferQueueCallback(
        XAAndroidBufferQueueItf caller,
        void *pCallbackContext,
        void *pBufferContext,
        void *pBufferData,
        XAuint32 dataSize,
        XAuint32 dataUsed,
        const XAAndroidBufferItem *pItems,
        XAuint32 itemsLength)
{
    XAPlayItf playerPlay = (XAPlayItf) pCallbackContext;
    // enqueue the .ts data directly from mapped memory, so ignore the empty buffer pBufferData
    if (curPacket <= lastPacket) {
        static const XAAndroidBufferItem discontinuity = {XA_ANDROID_ITEMKEY_DISCONTINUITY, 0};
        static const XAAndroidBufferItem eos = {XA_ANDROID_ITEMKEY_EOS, 0};
        static const XAAndroidBufferItem formatChange = {XA_ANDROID_ITEMKEY_FORMAT_CHANGE, 0};
        const XAAndroidBufferItem *items;
        XAuint32 itemSize;
        // compute number of packets to be enqueued in this buffer
        XAuint32 packetsThisBuffer = lastPacket - curPacket;
        if (packetsThisBuffer > PACKETS_PER_BUFFER) {
            packetsThisBuffer = PACKETS_PER_BUFFER;
        }
        // last packet? this should only happen once
        if (curPacket == lastPacket) {
            if (sentEOS) {
                printf("buffer completion callback after EOS\n");
                return XA_RESULT_SUCCESS;
            }
            printf("sending EOS\n");
            items = &eos;
            itemSize = sizeof(eos);
            sentEOS = XA_BOOLEAN_TRUE;
        // discontinuity requested?
        } else if (curPacket == discPacket) {
            printf("sending discontinuity at packet %zu, then resuming at packet %zu\n", discPacket,
                    afterDiscPacket);
            items = &discontinuity;
            itemSize = sizeof(discontinuity);
            curPacket = afterDiscPacket;
        // format change requested?
        } else if (curPacket == formatPacket) {
            printf("sending format change");
            items = &formatChange;
            itemSize = sizeof(formatChange);
        // pure data with no items
        } else {
            items = NULL;
            itemSize = 0;
        }
        XAresult result;
        // enqueue the optional data and optional items; there is always at least one or the other
        assert(packetsThisBuffer > 0 || itemSize > 0);
        result = (*caller)->Enqueue(caller, NULL, &packets[curPacket],
                sizeof(MPEG2TS_Packet) * packetsThisBuffer, items, itemSize);
        assert(XA_RESULT_SUCCESS == result);
        curPacket += packetsThisBuffer;
        // display position periodically
        if (curPacket % 1000 == 0) {
            XAmillisecond position;
            result = (*playerPlay)->GetPosition(playerPlay, &position);
            assert(XA_RESULT_SUCCESS == result);
            printf("Position after enqueueing packet %u: %u ms\n", curPacket, position);
        }
    }
    if (forceCallbackFailure && (curPacket % 1230 == 0)) {
        return (XAresult) curPacket;
    } else {
        return XA_RESULT_SUCCESS;
    }
}

// convert a domain type to string
static const char *domainToString(XAuint32 domain)
{
    switch (domain) {
    case 0: // FIXME There's a private declaration '#define XA_DOMAINTYPE_CONTAINER 0' in src/data.h
            // but we don't have access to it. Plan to file a bug with Khronos about this symbol.
        return "media container";
#define _(x) case x: return #x;
    _(XA_DOMAINTYPE_AUDIO)
    _(XA_DOMAINTYPE_VIDEO)
    _(XA_DOMAINTYPE_IMAGE)
    _(XA_DOMAINTYPE_TIMEDTEXT)
    _(XA_DOMAINTYPE_MIDI)
    _(XA_DOMAINTYPE_VENDOR)
    _(XA_DOMAINTYPE_UNKNOWN)
#undef _
    default:
        return "unknown";
    }
}

// main program
int main(int argc, char **argv)
{
    const char *prog = argv[0];
    int i;

    XAboolean abq = XA_BOOLEAN_FALSE;   // use AndroidBufferQueue, default is URI
    XAboolean looping = XA_BOOLEAN_FALSE;
    for (i = 1; i < argc; ++i) {
        const char *arg = argv[i];
        if (arg[0] != '-')
            break;
        switch (arg[1]) {
        case 'a':
            abq = XA_BOOLEAN_TRUE;
            break;
        case 'c':
            forceCallbackFailure = XA_BOOLEAN_TRUE;
            break;
        case 'd':
            discPacket = atoi(&arg[2]);
            break;
        case 'D':
            afterDiscPacket = atoi(&arg[2]);
            break;
        case 'f':
            firstPacket = atoi(&arg[2]);
            break;
        case 'F':
            formatPacket = atoi(&arg[2]);
            break;
        case 'l':
            looping = XA_BOOLEAN_TRUE;
            break;
        case 'n':
            numPackets = atoi(&arg[2]);
            break;
        case 'p':
            pauseMs = atoi(&arg[2]);
            break;
        case 's':
            seekPos = atoi(&arg[2]);
            break;
        default:
            fprintf(stderr, "%s: unknown option %s\n", prog, arg);
            break;
        }
    }

    // check that exactly one URI was specified
    if (argc - i != 1) {
        fprintf(stderr, "usage: %s [-a] [-c] [-d#] [-D#] [-f#] [-F#] [-l] [-n#] [-p#] [-s#] uri\n",
                prog);
        fprintf(stderr, "    -a  Use Android buffer queue to supply data, default is URI\n");
        fprintf(stderr, "    -c  Force callback to return an error randomly, for debugging only\n");
        fprintf(stderr, "    -d# Packet index to insert a discontinuity, default is none\n");
        fprintf(stderr, "    -D# Packet index to switch to after the discontinuity\n");
        fprintf(stderr, "    -f# First packet index, defaults to 0\n");
        fprintf(stderr, "    -F# Packet index to insert a format change, default is none\n");
        fprintf(stderr, "    -l  Enable looping, for URI only\n");
        fprintf(stderr, "    -n# Number of packets to enqueue\n");
        fprintf(stderr, "    -p# Pause playback for 5 seconds after this many milliseconds\n");
        fprintf(stderr, "    -s# Seek position in milliseconds, for URI only\n");
        return EXIT_FAILURE;
    }
    const char *uri = argv[i];

    // for AndroidBufferQueue, interpret URI as a filename and open
    int fd = -1;
    if (abq) {
        fd = open(uri, O_RDONLY);
        if (fd < 0) {
            perror(uri);
            goto close;
        }
        int ok;
        struct stat statbuf;
        ok = fstat(fd, &statbuf);
        if (ok < 0) {
            perror(uri);
            goto close;
        }
        if (!S_ISREG(statbuf.st_mode)) {
            fprintf(stderr, "%s: not an ordinary file\n", uri);
            goto close;
        }
        void *ptr;
        ptr = mmap(NULL, statbuf.st_size, PROT_READ, MAP_FILE | MAP_PRIVATE, fd, (off_t) 0);
        if (ptr == MAP_FAILED) {
            perror(uri);
            goto close;
        }
        size_t filelen = statbuf.st_size;
        if ((filelen % MPEG2TS_PACKET_SIZE) != 0) {
            fprintf(stderr, "%s: warning file length %zu is not a multiple of %d\n", uri, filelen,
                    MPEG2TS_PACKET_SIZE);
        }
        packets = (MPEG2TS_Packet *) ptr;
        totalPackets = filelen / MPEG2TS_PACKET_SIZE;
        printf("%s has %zu total packets\n", uri, totalPackets);
        if (firstPacket >= totalPackets) {
            fprintf(stderr, "-f%zu ignored\n", firstPacket);
            firstPacket = 0;
        }
        if (numPackets == 0) {
            numPackets = totalPackets - firstPacket;
        } else if (firstPacket + numPackets > totalPackets) {
            fprintf(stderr, "-n%zu ignored\n", numPackets);
            numPackets = totalPackets - firstPacket;
        }
        lastPacket = firstPacket + numPackets;
        if (discPacket != 0 && (discPacket < firstPacket || discPacket >= lastPacket)) {
            fprintf(stderr, "-d%zu ignored\n", discPacket);
            discPacket = 0;
        }
        if (afterDiscPacket < firstPacket || afterDiscPacket >= lastPacket) {
            fprintf(stderr, "-D%zu ignored\n", afterDiscPacket);
            afterDiscPacket = 0;
        }
        if (formatPacket != 0 && (formatPacket < firstPacket || formatPacket >= lastPacket)) {
            fprintf(stderr, "-F%zu ignored\n", formatPacket);
            formatPacket = 0;
        }
    }

    ANativeWindow *nativeWindow;

    XAresult result;
    XAObjectItf engineObject;

    // create engine
    result = xaCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    assert(XA_RESULT_SUCCESS == result);
    result = (*engineObject)->Realize(engineObject, XA_BOOLEAN_FALSE);
    assert(XA_RESULT_SUCCESS == result);
    XAEngineItf engineEngine;
    result = (*engineObject)->GetInterface(engineObject, XA_IID_ENGINE, &engineEngine);
    assert(XA_RESULT_SUCCESS == result);

    // create output mix
    XAObjectItf outputMixObject;
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, NULL, NULL);
    assert(XA_RESULT_SUCCESS == result);
    result = (*outputMixObject)->Realize(outputMixObject, XA_BOOLEAN_FALSE);
    assert(XA_RESULT_SUCCESS == result);

    // configure media source
    XADataLocator_URI locUri;
    locUri.locatorType = XA_DATALOCATOR_URI;
    locUri.URI = (XAchar *) uri;
    XADataFormat_MIME fmtMime;
    fmtMime.formatType = XA_DATAFORMAT_MIME;
    if (abq) {
        fmtMime.mimeType = (XAchar *) XA_ANDROID_MIME_MP2TS;
        fmtMime.containerType = XA_CONTAINERTYPE_MPEG_TS;
    } else {
        fmtMime.mimeType = NULL;
        fmtMime.containerType = XA_CONTAINERTYPE_UNSPECIFIED;
    }
    XADataLocator_AndroidBufferQueue locABQ;
    locABQ.locatorType = XA_DATALOCATOR_ANDROIDBUFFERQUEUE;
    locABQ.numBuffers = NB_BUFFERS;
    XADataSource dataSrc;
    if (abq) {
        dataSrc.pLocator = &locABQ;
    } else {
        dataSrc.pLocator = &locUri;
    }
    dataSrc.pFormat = &fmtMime;

    // configure audio sink
    XADataLocator_OutputMix locOM;
    locOM.locatorType = XA_DATALOCATOR_OUTPUTMIX;
    locOM.outputMix = outputMixObject;
    XADataSink audioSnk;
    audioSnk.pLocator = &locOM;
    audioSnk.pFormat = NULL;

    // configure video sink
    nativeWindow = getNativeWindow();
    XADataLocator_NativeDisplay locND;
    locND.locatorType = XA_DATALOCATOR_NATIVEDISPLAY;
    locND.hWindow = nativeWindow;
    locND.hDisplay = NULL;
    XADataSink imageVideoSink;
    imageVideoSink.pLocator = &locND;
    imageVideoSink.pFormat = NULL;

    // create media player
    XAObjectItf playerObject;
    XAInterfaceID ids[4] = {XA_IID_STREAMINFORMATION, XA_IID_PREFETCHSTATUS, XA_IID_SEEK,
            XA_IID_ANDROIDBUFFERQUEUESOURCE};
    XAboolean req[4] = {XA_BOOLEAN_TRUE, XA_BOOLEAN_TRUE, XA_BOOLEAN_FALSE, XA_BOOLEAN_TRUE};
    result = (*engineEngine)->CreateMediaPlayer(engineEngine, &playerObject, &dataSrc, NULL,
            &audioSnk, nativeWindow != NULL ? &imageVideoSink : NULL, NULL, NULL, abq ? 4 : 3, ids,
            req);
    assert(XA_RESULT_SUCCESS == result);

    // realize the player
    result = (*playerObject)->Realize(playerObject, XA_BOOLEAN_FALSE);
    assert(XA_RESULT_SUCCESS == result);

    // get the play interface
    XAPlayItf playerPlay;
    result = (*playerObject)->GetInterface(playerObject, XA_IID_PLAY, &playerPlay);
    assert(XA_RESULT_SUCCESS == result);

    if (abq) {

        // get the Android buffer queue interface
        XAAndroidBufferQueueItf playerAndroidBufferQueue;
        result = (*playerObject)->GetInterface(playerObject, XA_IID_ANDROIDBUFFERQUEUESOURCE,
                &playerAndroidBufferQueue);
        assert(XA_RESULT_SUCCESS == result);

        // register the buffer queue callback
        result = (*playerAndroidBufferQueue)->RegisterCallback(playerAndroidBufferQueue,
                bufferQueueCallback, (void *) playerPlay);
        assert(XA_RESULT_SUCCESS == result);
        result = (*playerAndroidBufferQueue)->SetCallbackEventsMask(playerAndroidBufferQueue,
                XA_ANDROIDBUFFERQUEUEEVENT_PROCESSED);
        assert(XA_RESULT_SUCCESS == result);

        // set the player's state to paused, to start prefetching
        printf("start early prefetch\n");
        result = (*playerPlay)->SetPlayState(playerPlay, XA_PLAYSTATE_PAUSED);
        assert(XA_RESULT_SUCCESS == result);

        // enqueue the initial buffers until buffer queue is full
        XAuint32 packetsThisBuffer;
        for (curPacket = firstPacket; curPacket < lastPacket; curPacket += packetsThisBuffer) {
            // handle the unlikely case of a very short .ts
            packetsThisBuffer = lastPacket - curPacket;
            if (packetsThisBuffer > PACKETS_PER_BUFFER) {
                packetsThisBuffer = PACKETS_PER_BUFFER;
            }
            result = (*playerAndroidBufferQueue)->Enqueue(playerAndroidBufferQueue, NULL,
                    &packets[curPacket], MPEG2TS_PACKET_SIZE * packetsThisBuffer, NULL, 0);
            if (XA_RESULT_BUFFER_INSUFFICIENT == result) {
                printf("Enqueued initial %u packets in %u buffers\n", curPacket - firstPacket,
                        (curPacket - firstPacket + PACKETS_PER_BUFFER - 1) / PACKETS_PER_BUFFER);
                break;
            }
            assert(XA_RESULT_SUCCESS == result);
        }

    }

    // get the stream information interface
    XAStreamInformationItf playerStreamInformation;
    result = (*playerObject)->GetInterface(playerObject, XA_IID_STREAMINFORMATION,
            &playerStreamInformation);
    assert(XA_RESULT_SUCCESS == result);

    // register the stream event change callback
    result = (*playerStreamInformation)->RegisterStreamChangeCallback(playerStreamInformation,
            streamEventChangeCallback, NULL);
    assert(XA_RESULT_SUCCESS == result);

    // get the prefetch status interface
    XAPrefetchStatusItf playerPrefetchStatus;
    result = (*playerObject)->GetInterface(playerObject, XA_IID_PREFETCHSTATUS,
            &playerPrefetchStatus);
    assert(XA_RESULT_SUCCESS == result);

    // register prefetch status callback
    result = (*playerPrefetchStatus)->RegisterCallback(playerPrefetchStatus, prefetchStatusCallback,
            NULL);
    assert(XA_RESULT_SUCCESS == result);
    result = (*playerPrefetchStatus)->SetCallbackEventsMask(playerPrefetchStatus,
            XA_PREFETCHEVENT_FILLLEVELCHANGE | XA_PREFETCHEVENT_STATUSCHANGE);
    assert(XA_RESULT_SUCCESS == result);

    // get the seek interface for seeking and/or looping
    if (looping || seekPos != XA_TIME_UNKNOWN) {
        XASeekItf playerSeek;
        result = (*playerObject)->GetInterface(playerObject, XA_IID_SEEK, &playerSeek);
        assert(XA_RESULT_SUCCESS == result);
        if (seekPos != XA_TIME_UNKNOWN) {
            result = (*playerSeek)->SetPosition(playerSeek, seekPos, XA_SEEKMODE_ACCURATE);
            if (XA_RESULT_FEATURE_UNSUPPORTED == result) {
                fprintf(stderr, "-s%u (seek to initial position) is unsupported\n", seekPos);
            } else {
                assert(XA_RESULT_SUCCESS == result);
            }
        }
        if (looping) {
            result = (*playerSeek)->SetLoop(playerSeek, XA_BOOLEAN_TRUE, (XAmillisecond) 0,
                    XA_TIME_UNKNOWN);
            if (XA_RESULT_FEATURE_UNSUPPORTED) {
                fprintf(stderr, "-l (looping) is unsupported\n");
            } else {
                assert(XA_RESULT_SUCCESS == result);
            }
        }
    }

    // register play event callback
    result = (*playerPlay)->RegisterCallback(playerPlay, playEventCallback, NULL);
    assert(XA_RESULT_SUCCESS == result);
    result = (*playerPlay)->SetCallbackEventsMask(playerPlay,
            XA_PLAYEVENT_HEADATEND | XA_PLAYEVENT_HEADATMARKER | XA_PLAYEVENT_HEADATNEWPOS);
    assert(XA_RESULT_SUCCESS == result);

    // set a marker
    result = (*playerPlay)->SetMarkerPosition(playerPlay, 5000);
    assert(XA_RESULT_SUCCESS == result);

    // set position update period
    result = (*playerPlay)->SetPositionUpdatePeriod(playerPlay, 2000);
    assert(XA_RESULT_SUCCESS == result);

    // get the position before prefetch
    XAmillisecond position;
    result = (*playerPlay)->GetPosition(playerPlay, &position);
    assert(XA_RESULT_SUCCESS == result);
    printf("Position before prefetch: %u ms\n", position);

    // get the duration before prefetch
    XAmillisecond duration;
    result = (*playerPlay)->GetDuration(playerPlay, &duration);
    assert(XA_RESULT_SUCCESS == result);
    if (XA_TIME_UNKNOWN == duration)
        printf("Duration before prefetch: unknown as expected\n");
    else
        printf("Duration before prefetch: %.1f (surprise!)\n", duration / 1000.0f);

    // set the player's state to paused, to start prefetching
    printf("start prefetch\n");
    result = (*playerPlay)->SetPlayState(playerPlay, XA_PLAYSTATE_PAUSED);
    assert(XA_RESULT_SUCCESS == result);

    // wait for prefetch status callback to indicate either sufficient data or error
    pthread_mutex_lock(&mutex);
    while (prefetch_status == PREFETCHSTATUS_UNKNOWN) {
        pthread_cond_wait(&cond, &mutex);
    }
    pthread_mutex_unlock(&mutex);
    if (prefetch_status == PREFETCHSTATUS_ERROR) {
        fprintf(stderr, "Error during prefetch, exiting\n");
        goto destroyRes;
    }

    // get the position after prefetch
    result = (*playerPlay)->GetPosition(playerPlay, &position);
    assert(XA_RESULT_SUCCESS == result);
    printf("Position after prefetch: %u ms\n", position);

    // get duration again, now it should be known for the file source or unknown for TS
    result = (*playerPlay)->GetDuration(playerPlay, &duration);
    assert(XA_RESULT_SUCCESS == result);
    if (duration == XA_TIME_UNKNOWN) {
        printf("Duration after prefetch: unknown (expected for TS, unexpected for file)\n");
    } else {
        printf("Duration after prefetch: %u ms (expected for file, unexpected for TS)\n", duration);
    }

    // query for media container information
    result = (*playerStreamInformation)->QueryMediaContainerInformation(playerStreamInformation,
            NULL);
    assert(XA_RESULT_PARAMETER_INVALID == result);
    XAMediaContainerInformation mediaContainerInformation;
    // this verifies it is filling in all the fields
    memset(&mediaContainerInformation, 0x55, sizeof(XAMediaContainerInformation));
    result = (*playerStreamInformation)->QueryMediaContainerInformation(playerStreamInformation,
            &mediaContainerInformation);
    assert(XA_RESULT_SUCCESS == result);
    printf("Media container information:\n");
    printf("  containerType = %u\n", mediaContainerInformation.containerType);
    printf("  mediaDuration = %u\n", mediaContainerInformation.mediaDuration);
    printf("  numStreams = %u\n", mediaContainerInformation.numStreams);

    // Now query for each the streams.  Note that stream indices go up to and including
    // mediaContainerInformation.numStreams, because stream 0 is the container itself,
    // while stream 1 to mediaContainerInformation.numStreams are the contained streams.
    XAuint32 numStreams = mediaContainerInformation.numStreams;
    XAuint32 streamIndex;
    for (streamIndex = 0; streamIndex <= mediaContainerInformation.numStreams; ++streamIndex) {
        XAuint32 domain;
        XAuint16 nameSize;
        XAchar name[64];
        printf("stream[%u]:\n", streamIndex);
        if (streamIndex == 0) {
            result = (*playerStreamInformation)->QueryStreamType(playerStreamInformation,
                    streamIndex, &domain);
            assert(XA_RESULT_PARAMETER_INVALID == result);
            result = (*playerStreamInformation)->QueryStreamInformation(playerStreamInformation,
                    streamIndex, &mediaContainerInformation);
            //assert(XA_RESULT_PARAMETER_INVALID == result);
            nameSize = sizeof(name);
            result = (*playerStreamInformation)->QueryStreamName(playerStreamInformation,
streamIndex, &nameSize, name);
            //assert(XA_RESULT_PARAMETER_INVALID == result);
            continue;
        }
        result = (*playerStreamInformation)->QueryStreamType(playerStreamInformation, streamIndex,
                NULL);
        assert(XA_RESULT_PARAMETER_INVALID == result);
        domain = 12345;
        result = (*playerStreamInformation)->QueryStreamType(playerStreamInformation, streamIndex,
                &domain);
        assert(XA_RESULT_SUCCESS == result);
        printf(" QueryStreamType: domain = 0x%X (%s)\n", domain, domainToString(domain));
        nameSize = sizeof(name);
        result = (*playerStreamInformation)->QueryStreamName(playerStreamInformation, streamIndex,
                &nameSize, name);
#if 0
        assert(XA_RESULT_SUCCESS == result);
        assert(sizeof(name) >= nameSize);
        if (sizeof(name) != nameSize) {
            assert('\0' == name[nameSize]);
        }
        printf(" QueryStreamName: nameSize=%u, name=\"%.*s\"\n", nameSize, nameSize, name);
        result = (*playerStreamInformation)->QueryStreamInformation(playerStreamInformation,
                streamIndex, NULL);
        assert(XA_RESULT_PARAMETER_INVALID == result);
#endif

        printf(" QueryStreamInformation:\n");
        switch (domain) {
#if 0
        case 0: // FIXME container
            result = (*playerStreamInformation)->QueryStreamInformation(playerStreamInformation,
streamIndex, &mediaContainerInformation);
            assert(XA_RESULT_SUCCESS == result);
            printf("  containerType = %u (1=unspecified)\n",
                    mediaContainerInformation.containerType);
            printf("  mediaDuration = %u\n", mediaContainerInformation.mediaDuration);
            printf("  numStreams = %u\n", mediaContainerInformation.numStreams);
            break;
#endif
        case XA_DOMAINTYPE_AUDIO: {
            XAAudioStreamInformation audioStreamInformation;
            memset(&audioStreamInformation, 0x55, sizeof(XAAudioStreamInformation));
            result = (*playerStreamInformation)->QueryStreamInformation(playerStreamInformation,
                    streamIndex, &audioStreamInformation);
            assert(XA_RESULT_PARAMETER_INVALID == result);
            printf("  codecId = %u\n", audioStreamInformation.codecId);
            printf("  channels = %u\n", audioStreamInformation.channels);
            printf("  sampleRate = %u\n", audioStreamInformation.sampleRate);
            printf("  bitRate = %u\n", audioStreamInformation.bitRate);
            printf("  langCountry = \"%s\"\n", audioStreamInformation.langCountry);
            printf("  duration = %u\n", audioStreamInformation.duration);
            } break;
        case XA_DOMAINTYPE_VIDEO: {
            XAVideoStreamInformation videoStreamInformation;
            result = (*playerStreamInformation)->QueryStreamInformation(playerStreamInformation,
                    streamIndex, &videoStreamInformation);
            assert(XA_RESULT_SUCCESS == result);
            printf("  codecId = %u\n", videoStreamInformation.codecId);
            printf("  width = %u\n", videoStreamInformation.width);
            printf("  height = %u\n", videoStreamInformation.height);
            printf("  frameRate = %u\n", videoStreamInformation.frameRate);
            printf("  bitRate = %u\n", videoStreamInformation.bitRate);
            printf("  duration = %u\n", videoStreamInformation.duration);
            } break;
        case XA_DOMAINTYPE_IMAGE: {
            XAImageStreamInformation imageStreamInformation;
            result = (*playerStreamInformation)->QueryStreamInformation(playerStreamInformation,
                    streamIndex, &imageStreamInformation);
            assert(XA_RESULT_SUCCESS == result);
            printf("  codecId = %u\n", imageStreamInformation.codecId);
            printf("  width = %u\n", imageStreamInformation.width);
            printf("  height = %u\n", imageStreamInformation.height);
            printf("  presentationDuration = %u\n", imageStreamInformation.presentationDuration);
            } break;
        case XA_DOMAINTYPE_TIMEDTEXT: {
            XATimedTextStreamInformation timedTextStreamInformation;
            result = (*playerStreamInformation)->QueryStreamInformation(playerStreamInformation,
                    streamIndex, &timedTextStreamInformation);
            assert(XA_RESULT_SUCCESS == result);
            printf("  layer = %u\n", timedTextStreamInformation.layer);
            printf("  width = %u\n", timedTextStreamInformation.width);
            printf("  height = %u\n", timedTextStreamInformation.height);
            printf("  tx = %u\n", timedTextStreamInformation.tx);
            printf("  ty = %u\n", timedTextStreamInformation.ty);
            printf("  bitrate = %u\n", timedTextStreamInformation.bitrate);
            printf("  langCountry = \"%s\"\n", timedTextStreamInformation.langCountry);
            printf("  duration = %u\n", timedTextStreamInformation.duration);
            } break;
        case XA_DOMAINTYPE_MIDI: {
            XAMIDIStreamInformation midiStreamInformation;
            result = (*playerStreamInformation)->QueryStreamInformation(playerStreamInformation,
                    streamIndex, &midiStreamInformation);
            assert(XA_RESULT_SUCCESS == result);
            printf("  channels = %u\n", midiStreamInformation.channels);
            printf("  tracks = %u\n", midiStreamInformation.tracks);
            printf("  bankType = %u\n", midiStreamInformation.bankType);
            printf("  langCountry = \"%s\"\n", midiStreamInformation.langCountry);
            printf("  duration = %u\n", midiStreamInformation.duration);
            } break;
        case XA_DOMAINTYPE_VENDOR: {
            XAVendorStreamInformation vendorStreamInformation;
            result = (*playerStreamInformation)->QueryStreamInformation(playerStreamInformation,
                    streamIndex, &vendorStreamInformation);
            assert(XA_RESULT_SUCCESS == result);
            printf("  VendorStreamInfo = %p\n", vendorStreamInformation.VendorStreamInfo);
            } break;
        case XA_DOMAINTYPE_UNKNOWN: {
            // "It is not possible to query Information for streams identified as
            // XA_DOMAINTYPE_UNKNOWN, any attempt to do so shall return a result of
            // XA_RESULT_CONTENT_UNSUPPORTED."
            char big[256];
            result = (*playerStreamInformation)->QueryStreamInformation(playerStreamInformation,
                    streamIndex, &big);
            assert(XA_RESULT_CONTENT_UNSUPPORTED == result);
            } break;
        default:
            break;
        }

    }
    // Try one more stream index beyond the valid range
    XAuint32 domain;
    result = (*playerStreamInformation)->QueryStreamType(playerStreamInformation, streamIndex,
            &domain);
    assert(XA_RESULT_PARAMETER_INVALID == result);
    XATimedTextStreamInformation big;
    result = (*playerStreamInformation)->QueryStreamInformation(playerStreamInformation,
            streamIndex, &big);
    assert(XA_RESULT_PARAMETER_INVALID == result);

    printf("QueryActiveStreams:\n");
    result = (*playerStreamInformation)->QueryActiveStreams(playerStreamInformation, NULL, NULL);
    assert(XA_RESULT_PARAMETER_INVALID == result);
    XAuint32 numStreams1 = 0x12345678;
    result = (*playerStreamInformation)->QueryActiveStreams(playerStreamInformation, &numStreams1,
            NULL);
    assert(XA_RESULT_SUCCESS == result);
    printf("  numStreams = %u\n", numStreams1);
    XAboolean *activeStreams = calloc(numStreams1 + 1, sizeof(XAboolean));
    assert(NULL != activeStreams);
    printf("  active stream(s) =");
    XAuint32 numStreams2 = numStreams1;
    result = (*playerStreamInformation)->QueryActiveStreams(playerStreamInformation, &numStreams2,
            activeStreams);
    assert(XA_RESULT_SUCCESS == result);
    assert(numStreams2 == numStreams1);
    for (streamIndex = 0; streamIndex <= numStreams1; ++streamIndex) {
        if (activeStreams[streamIndex])
            printf(" %u", streamIndex);
    }
    printf("\n");

    // SetActiveStream is untested

    // start playing
    printf("starting to play\n");
    result = (*playerPlay)->SetPlayState(playerPlay, XA_PLAYSTATE_PLAYING);
    assert(XA_RESULT_SUCCESS == result);

    // continue playing until end of media
    for (;;) {
        XAuint32 status;
        result = (*playerPlay)->GetPlayState(playerPlay, &status);
        assert(XA_RESULT_SUCCESS == result);
        if (status == XA_PLAYSTATE_PAUSED)
            break;
        assert(status == XA_PLAYSTATE_PLAYING);
        usleep(100000);
        if (pauseMs >= 0) {
            result = (*playerPlay)->GetPosition(playerPlay, &position);
            assert(XA_RESULT_SUCCESS == result);
            if (position >= pauseMs) {
                printf("Pausing for 5 seconds at position %u\n", position);
                result = (*playerPlay)->SetPlayState(playerPlay, XA_PLAYSTATE_PAUSED);
                assert(XA_RESULT_SUCCESS == result);
                sleep(5);
                // FIXME clear ABQ queue here
                result = (*playerPlay)->SetPlayState(playerPlay, XA_PLAYSTATE_PLAYING);
                assert(XA_RESULT_SUCCESS == result);
                pauseMs = -1;
            }
        }
    }

    // wait a bit more in case of additional callbacks
    printf("end of media\n");
    sleep(3);

    // get final position
    result = (*playerPlay)->GetPosition(playerPlay, &position);
    assert(XA_RESULT_SUCCESS == result);
    printf("Position at end: %u ms\n", position);

    // get duration again, now it should be known
    result = (*playerPlay)->GetDuration(playerPlay, &duration);
    assert(XA_RESULT_SUCCESS == result);
    if (duration == XA_TIME_UNKNOWN) {
        printf("Duration at end: unknown\n");
    } else {
        printf("Duration at end: %u ms\n", duration);
    }

destroyRes:

    // destroy the player
    (*playerObject)->Destroy(playerObject);

    // destroy the output mix
    (*outputMixObject)->Destroy(outputMixObject);

    // destroy the engine
    (*engineObject)->Destroy(engineObject);

#if 0
    if (nativeWindow != NULL) {
        ANativeWindow_release(nativeWindow);
    }
#endif

close:
    if (fd >= 0) {
        (void) close(fd);
    }

    disposeNativeWindow();

    return EXIT_SUCCESS;
}
