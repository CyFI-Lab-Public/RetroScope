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
/**
 ******************************************************************************
 * @file    M4MP4W_Writer.c
 * @brief   Implementation of the core MP4 writer
 ******************************************************************************
 */

#include "NXPSW_CompilerSwitches.h"

#ifndef _M4MP4W_USE_CST_MEMORY_WRITER

#include "M4OSA_Error.h"
#include "M4OSA_Debug.h"
#include "M4MP4W_Writer.h"
#include "M4MP4W_Utils.h"

/* Check optimisation flags : BEGIN */
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE
#ifdef _M4MP4W_MOOV_FIRST
#error "_M4MP4W_OPTIMIZE_FOR_PHONE should not be used with _M4MP4W_MOOV_FIRST"

#endif

#endif

#ifdef _M4MP4W_UNBUFFERED_VIDEO
#ifndef _M4MP4W_OPTIMIZE_FOR_PHONE
#error "_M4MP4W_UNBUFFERED_VIDEO should be used with _M4MP4W_OPTIMIZE_FOR_PHONE"

#endif

#endif
/* Check optimisation flags : END */

#ifndef _M4MP4W_DONT_USE_TIME_H
#include <time.h>

#endif /*_M4MP4W_DONT_USE_TIME_H*/

/*MACROS*/
#define MAJOR_VERSION 3
#define MINOR_VERSION 3
#define REVISION 0

#define ERR_CHECK(exp, err) if (!(exp)) { return err; }
#define CLEANUPonERR(func) if ((err = func) != M4NO_ERROR) goto cleanup

#define max(a,b) (((a) > (b)) ? (a) : (b))

/***************/
/*Static blocks*/
/***************/

/*CommonBlocks*/

const M4OSA_UChar Default_ftyp [] =
{
    0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', '3', 'g', 'p', '7', 0x00, 0x00,
    0x03, 0x00, '3', 'g', 'p', '7', 'i', 's', 'o', 'm'
};

const M4OSA_UChar CommonBlock2 [] =
{
    'm', 'd', 'a', 't'
};

const M4OSA_UChar CommonBlock3 [] =
{
    'm', 'o', 'o', 'v', 0x00, 0x00, 0x00, 0x6C, 'm', 'v', 'h', 'd', 0x00,
    0x00, 0x00, 0x00
};

const M4OSA_UChar CommonBlock4 [] =
{
    0x00, 0x00, 0x03, 0xE8
};

const M4OSA_UChar CommonBlock5 [] =
{
    0x00, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03
};

const M4OSA_UChar CommonBlock6 [] =
{
    't', 'r', 'a', 'k', 0x00, 0x00, 0x00, 0x5C, 't', 'k', 'h', 'd', 0x00,
    0x00, 0x00, 0x01
};

const M4OSA_UChar CommonBlock7 [] =
{
    0x00, 0x00, 0x00, 0x00
};

const M4OSA_UChar CommonBlock7bis [] =
{
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x01, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x40, 0x00, 0x00, 0x00
};

const M4OSA_UChar CommonBlock8 [] =
{
    'm', 'd', 'i', 'a', 0x00, 0x00, 0x00, 0x20, 'm', 'd', 'h', 'd', 0x00,
    0x00, 0x00, 0x00
};

const M4OSA_UChar CommonBlock9 [] =
{
    0x55, 0xC4, 0x00, 0x00
};

const M4OSA_UChar CommonBlock10 [] =
{
    'm', 'i', 'n', 'f', 0x00, 0x00, 0x00, 0x24, 'd', 'i', 'n', 'f', 0x00,
    0x00, 0x00, 0x1C, 'd', 'r', 'e', 'f', 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x01, 0x00, 0x00, 0x00, 0x0C, 'u', 'r', 'l', ' ', 0x00, 0x00, 0x00,
    0x01
};

const M4OSA_UChar CommonBlock11 [] =
{
    's', 't', 'b', 'l'
};

const M4OSA_UChar CommonBlock12 [] =
{
    's', 't', 't', 's', 0x00, 0x00, 0x00, 0x00
};

const M4OSA_UChar SampleDescriptionHeader [] =
{
    's', 't', 's', 'd', 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
};

const M4OSA_UChar SampleDescriptionEntryStart [] =
{
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00
};

const M4OSA_UChar CommonBlock15 [] =
{
    's', 't', 's', 'z', 0x00, 0x00, 0x00, 0x00
};

const M4OSA_UChar CommonBlock16 [] =
{
    's', 't', 's', 'c', 0x00, 0x00, 0x00, 0x00
};

const M4OSA_UChar CommonBlock17 [] =
{
    's', 't', 'c', 'o', 0x00, 0x00, 0x00, 0x00
};

const M4OSA_UChar BlockSignatureSkipHeader [] =
{
    0x00, 0x00, 0x00, 0x5E, 's', 'k', 'i', 'p'
};
/* due to current limitations, size must be 16 */
const M4OSA_UChar BlockSignatureSkipDefaultEmbeddedString [] =
{
    'N', 'X', 'P', 'S', 'W', ' ', 'C', 'A', 'M', 'C', 'O', 'R', 'D', 'E',
    'R', ' '
};
/* follows the version (like " 3.0.2"), then " -- " */
/* due to current limitations, size must be 60 */
const M4OSA_UChar BlockSignatureSkipDefaultIntegrationTag [] =
{
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
};

/*VideoBlocks*/
/* 320*240, now no longer hardcoded */
/* const M4OSA_UChar VideoBlock1[] =
    { 0x01, 0x40, 0x00, 0x00, 0x00, 0xF0, 0x00, 0x00 }; */
const M4OSA_UChar VideoBlock1_1 [] =
{
    0x00, 0x00, 0x00, 0x21, 'h', 'd', 'l', 'r', 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 'v', 'i', 'd', 'e', 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
};

const M4OSA_UChar SampleDescriptionEntryVideoBoilerplate1 [] =
{
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
};

const M4OSA_UChar SampleDescriptionEntryVideoBoilerplate2 [] =
{
    0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x18, 0xFF, 0xFF
};

const M4OSA_UChar VideoBlock4 [] =
{
    's', 't', 's', 's', 0x00, 0x00, 0x00, 0x00
}; /*STSS*/

const M4OSA_UChar VideoBlock5 [] =
{
    0x00, 0x00, 0x00, 0x14, 'v', 'm', 'h', 'd', 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
};

const M4OSA_UChar VideoResolutions [] =
{
    0x00, 0x48, 0x00, 0x00, 0x00, 0x48, 0x00, 0x00
};

/*Mp4vBlocks*/
const M4OSA_UChar Mp4vBlock1 [] =
{
    'm', 'p', '4', 'v'
};

const M4OSA_UChar Mp4vBlock3 [] =
{
    0x20, 0x11
};

/*H263Blocks*/
const M4OSA_UChar H263Block1 [] =
{
    's', '2', '6', '3'
};

const M4OSA_UChar H263Block2 [] =
{
    0x00, 0x00, 0x00, 0x0F, 'd', '2', '6', '3'
};

const M4OSA_UChar H263Block2_bitr [] =
{
    0x00, 0x00, 0x00, 0x1F, 'd', '2', '6', '3'
};

const M4OSA_UChar H263Block3 [] =
{
    'P', 'H', 'L', 'P', 0x00, 0x0A, 0x00
};

const M4OSA_UChar H263Block4 [] =
{
    0x00, 0x00, 0x00, 0x10, 'b', 'i', 't', 'r'
};

/*H264Blocks*/
const M4OSA_UChar H264Block1 [] =
{
    'a', 'v', 'c', '1'
};

/* Store the avcC field, the version (=1),
    the profile (=66), the compatibility (=0), */

/* the level (=10),111111 + NAL field Size (= 4 - 1),
    111 + number of PPS (=1) */

const M4OSA_UChar H264Block2 [] =
{
        // Remove the hardcoded DSI values of H264Block2
        'a' , 'v' , 'c' , 'C'
};

/*AMRBlocks*/
const M4OSA_UChar AMRBlock1 [] =
{
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
};

const M4OSA_UChar AMRBlock1_1 [] =
{
    0x00, 0x00, 0x00, 0x21, 'h', 'd', 'l', 'r', 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 's', 'o', 'u', 'n', 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
};

const M4OSA_UChar AudioSampleDescEntryBoilerplate [] =
{
    0x00, 0x02, 0x00, 0x10, 0x00, 0x00, 0x00, 0x00
};

const M4OSA_UChar AMRDSIHeader [] =
{
    0x00, 0x00, 0x00, 0x11, 'd', 'a', 'm', 'r'
};

const M4OSA_UChar AMRDefaultDSI [] =
{
    'P', 'H', 'L', 'P', 0x00, 0x00, 0x80, 0x00, 0x01
};

const M4OSA_UChar AMRBlock4 [] =
{
    0x00, 0x00, 0x00, 0x10, 's', 'm', 'h', 'd', 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00
};

/*AMR8Blocks*/
const M4OSA_UChar AMR8Block1 [] =
{
    's', 'a', 'm', 'r'
};

/*AMR16Blocks*/
/*const M4OSA_UChar AMR16Block1[] = { 's', 'a', 'w', 'b'};*/

/*AACBlocks*/
const M4OSA_UChar AACBlock1 [] =
{
    'm', 'p', '4', 'a'
};

const M4OSA_UChar AACBlock2 [] =
{
    0x40, 0x15
};

/*MPEGConfigBlocks (AAC & MP4V)*/
const M4OSA_UChar MPEGConfigBlock0 [] =
{
    'e', 's', 'd', 's', 0x00, 0x00, 0x00, 0x00, 0x03
};

const M4OSA_UChar MPEGConfigBlock1 [] =
{
    0x00, 0x00, 0x00, 0x04
};

const M4OSA_UChar MPEGConfigBlock2 [] = { 0x05 };
const M4OSA_UChar MPEGConfigBlock3 [] =
{
    0x06, 0x01, 0x02
};

/*EVRCBlocks*/
const M4OSA_UChar EVRCBlock3_1 [] =
{
    0x00, 0x00, 0x00, 0x0E, 'd', 'e', 'v', 'c'
};

const M4OSA_UChar EVRCBlock3_2 [] =
{
    'P', 'H', 'L', 'P', 0x00, 0x00
};

/*EVRC8Blocks*/
const M4OSA_UChar EVRC8Block1 [] =
{
    's', 'e', 'v', 'c'
};

/***********/
/* Methods */
/***********/

/*******************************************************************************/
M4OSA_ERR M4MP4W_getVersion(M4OSA_UInt8 *major, M4OSA_UInt8 *minor,
                            M4OSA_UInt8 *revision )
/*******************************************************************************/
{
    ERR_CHECK(M4OSA_NULL != major, M4ERR_PARAMETER);
    ERR_CHECK(M4OSA_NULL != minor, M4ERR_PARAMETER);
    ERR_CHECK(M4OSA_NULL != revision, M4ERR_PARAMETER);

    *major = MAJOR_VERSION;
    *minor = MINOR_VERSION;
    *revision = REVISION;

    return M4NO_ERROR;
}

static M4OSA_UInt32 M4MP4W_STTS_ALLOC_SIZE;
static M4OSA_UInt32 M4MP4W_STSZ_ALLOC_SIZE;
static M4OSA_UInt32 M4MP4W_STSS_ALLOC_SIZE;
static M4OSA_UInt32 M4MP4W_CHUNK_ALLOC_NB;
static M4OSA_UInt32 M4MP4W_STTS_AUDIO_ALLOC_SIZE;
static M4OSA_UInt32 M4MP4W_STSZ_AUDIO_ALLOC_SIZE;
static M4OSA_UInt32 M4MP4W_CHUNK_AUDIO_ALLOC_NB;

#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE
#ifdef _M4MP4W_UNBUFFERED_VIDEO
/* stsc[ ] table is splitted at 12 bits */
#define M4MP4W_VIDEO_MAX_AU_PER_CHUNK 4095 /* 0=notused */

#else
#define M4MP4W_VIDEO_MAX_AU_PER_CHUNK 10   /* 0=notused */

#endif

#endif

/*******************************************************************************/

M4OSA_ERR M4MP4W_initializeAllocationParameters(M4MP4W_Mp4FileData *Ptr )
/*******************************************************************************/
{
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

    M4OSA_UInt32 maxMemory, vesMemory;
    M4OSA_UInt32 nbVideoFrames, nbAudioFrames;
    M4OSA_UInt32 averageVideoChunk;

    /*-----------*/
    /* NB_FRAMES */
    /*-----------*/

    /* magical formula : memory = vesMemory + 12 * framerate * duration */

#ifdef _M4MP4W_UNBUFFERED_VIDEO

    vesMemory = 0x32000; /* 200 kB */

#else

    vesMemory = 0x3E800; /* 250 kB */

#endif

#define VIDEO_POOL_MEMORY 1000000

    maxMemory = VIDEO_POOL_MEMORY;

    if (maxMemory < vesMemory) {
        return M4ERR_ALLOC;
    }

    nbVideoFrames = ( maxMemory - vesMemory) / 12;

    M4OSA_TRACE1_1("M4MP4W: %d images max", nbVideoFrames);

    /* VIDEO */
#ifdef _M4MP4W_UNBUFFERED_VIDEO
    /* assume an average of 25 fpc : reference = 15 fps * 2s * 0.8 */

    averageVideoChunk = 2500;

#else

    if (M4MP4W_VIDEO_MAX_AU_PER_CHUNK > 0)
    {
        averageVideoChunk = 100 * M4MP4W_VIDEO_MAX_AU_PER_CHUNK - 20
            * (M4MP4W_VIDEO_MAX_AU_PER_CHUNK - 1); /* margin 20% */
    }
    else
    {
        /* assume an average of 50 fpc */
        averageVideoChunk = 5000;
    }

#endif

    M4MP4W_STTS_ALLOC_SIZE = nbVideoFrames * sizeof(M4OSA_UInt32);
    M4MP4W_STSZ_ALLOC_SIZE = nbVideoFrames * sizeof(M4OSA_UInt16);
    M4MP4W_STSS_ALLOC_SIZE = nbVideoFrames * sizeof(
        M4OSA_UInt32); /* very conservative (all images are intra) */

    M4MP4W_CHUNK_ALLOC_NB = ( nbVideoFrames * 100) / averageVideoChunk + 1;

    /* AUDIO */

    nbAudioFrames = nbVideoFrames;
    /* audio is 5 fps, which is the smallest framerate for video */

    M4MP4W_STTS_AUDIO_ALLOC_SIZE = 100; /* compressed */
    M4MP4W_STSZ_AUDIO_ALLOC_SIZE = 100; /* compressed */

#ifdef _M4MP4W_UNBUFFERED_VIDEO

    M4MP4W_CHUNK_AUDIO_ALLOC_NB = nbAudioFrames / 10 + 1;

#else

    M4MP4W_CHUNK_AUDIO_ALLOC_NB = nbAudioFrames / 38 + 1;

#endif

    return M4NO_ERROR;

#else

    /* VIDEO 5 min at 25 fps null-enc */

    M4MP4W_STTS_ALLOC_SIZE = 20000;
    M4MP4W_STSZ_ALLOC_SIZE = 18000;
    M4MP4W_STSS_ALLOC_SIZE = 5000;
    M4MP4W_CHUNK_ALLOC_NB = 500;

    /* AUDIO 2 min aac+ null-enc */

    M4MP4W_STTS_AUDIO_ALLOC_SIZE = 32000;
    M4MP4W_STSZ_AUDIO_ALLOC_SIZE = 20000;
    M4MP4W_CHUNK_AUDIO_ALLOC_NB = 1000;

    return M4NO_ERROR;

#endif /*_M4MP4W_OPTIMIZE_FOR_PHONE*/

}

/*******************************************************************************/
M4OSA_ERR M4MP4W_openWrite(M4OSA_Context *contextPtr,
                           void *outputFileDescriptor,
                           M4OSA_FileWriterPointer *fileWriterFunction,
                           void *tempFileDescriptor,
                           M4OSA_FileReadPointer *fileReaderFunction )
/*******************************************************************************/
{
    M4OSA_ERR err = M4NO_ERROR;
    M4MP4W_Mp4FileData *mMp4FileDataPtr = M4OSA_NULL;

    ERR_CHECK(M4OSA_NULL != contextPtr, M4ERR_PARAMETER);
    ERR_CHECK(M4OSA_NULL != outputFileDescriptor, M4ERR_PARAMETER);
    ERR_CHECK(M4OSA_NULL != fileWriterFunction, M4ERR_PARAMETER);
#ifdef _M4MP4W_RESERVED_MOOV_DISK_SPACE
    /* Optional, feature won't be used if NULL */

    M4OSA_TRACE2_1("tempFileDescriptor = %p", tempFileDescriptor);

    if (M4OSA_NULL == tempFileDescriptor)
    {
        M4OSA_TRACE1_0(
            "tempFileDescriptor is NULL, RESERVED_MOOV_DISK_SPACE feature not used");
    }

#else /* _M4MP4W_RESERVED_MOOV_DISK_SPACE */
    /* Not used : ERR_CHECK(M4OSA_NULL != tempFileDescriptor, M4ERR_PARAMETER); */
#endif /* _M4MP4W_RESERVED_MOOV_DISK_SPACE */
    /* Not used : ERR_CHECK(M4OSA_NULL != fileReaderFunction, M4ERR_PARAMETER); */

    /* The context reuse mode was suppressed*/

    mMp4FileDataPtr =
        (M4MP4W_Mp4FileData *)M4OSA_32bitAlignedMalloc(sizeof(M4MP4W_Mp4FileData),
        M4MP4_WRITER, (M4OSA_Char *)"MP4 writer context");
    ERR_CHECK(mMp4FileDataPtr != M4OSA_NULL, M4ERR_ALLOC);
    mMp4FileDataPtr->url = outputFileDescriptor;
    mMp4FileDataPtr->audioTrackPtr = M4OSA_NULL;
    mMp4FileDataPtr->videoTrackPtr = M4OSA_NULL;
    mMp4FileDataPtr->MaxChunkSize = M4MP4W_DefaultMaxChunkSize; /*default  */
    mMp4FileDataPtr->MaxAUSize = M4MP4W_DefaultMaxAuSize;       /*default  */
    mMp4FileDataPtr->InterleaveDur =
        M4MP4W_DefaultInterleaveDur; /*default = 0, i.e. not used*/
    mMp4FileDataPtr->MaxFileSize = 0; /*default = 0, i.e. not used*/
    mMp4FileDataPtr->camcoderVersion = 0; /*default is " 0.0.0"*/
    mMp4FileDataPtr->embeddedString =
        M4OSA_NULL; /*default is in BlockSignatureSkipDefaultEmbeddedString */
    mMp4FileDataPtr->integrationTag = M4OSA_NULL; /*default is 0 */
    mMp4FileDataPtr->MaxFileDuration = 0; /*default = 0, i.e. not used*/

    mMp4FileDataPtr->fileWriterFunctions = fileWriterFunction;
    mMp4FileDataPtr->hasAudio = M4OSA_FALSE;
    mMp4FileDataPtr->hasVideo = M4OSA_FALSE;
    mMp4FileDataPtr->state = M4MP4W_opened;
    mMp4FileDataPtr->duration = 0; /*i*/
    /*patch for integrationTag 174 -> 238 (+64)*/
    mMp4FileDataPtr->filesize =
        238; /*initialization with constant part in ftyp+mdat+moov+skip*/

    mMp4FileDataPtr->estimateAudioSize = M4OSA_FALSE;
    mMp4FileDataPtr->audioMsChunkDur =
        0; /*set and used only when estimateAudioSize is true*/
    mMp4FileDataPtr->audioMsStopTime =
        0; /*set and used only when estimateAudioSize is true*/

    mMp4FileDataPtr->fileWriterContext = M4OSA_NULL;
    /* + CRLV6775 -H.264 trimming */
    mMp4FileDataPtr->bMULPPSSPS = M4OSA_FALSE;
    /* - CRLV6775 -H.264 trimming */

#ifndef _M4MP4W_MOOV_FIRST

    mMp4FileDataPtr->absoluteCurrentPos =
        32; /*init with ftyp + beginning of mdat size*/

#endif

#ifdef _M4MP4W_RESERVED_MOOV_DISK_SPACE

    mMp4FileDataPtr->safetyFileUrl = tempFileDescriptor;
    mMp4FileDataPtr->cleanSafetyFile =
        M4OSA_FALSE; /* No need to clean it just yet. */

#endif               /* _M4MP4W_RESERVED_MOOV_DISK_SPACE */

    /* ftyp atom */

    memset((void *) &mMp4FileDataPtr->ftyp,0,
        sizeof(mMp4FileDataPtr->ftyp));

    *contextPtr = mMp4FileDataPtr;

    M4MP4W_initializeAllocationParameters(mMp4FileDataPtr);

    return err;
}

/*******************************************************************************/
M4OSA_ERR M4MP4W_addStream(M4OSA_Context context,
                           M4SYS_StreamDescription *streamDescPtr )
/*******************************************************************************/
{
    M4OSA_ERR err = M4NO_ERROR;

    M4MP4W_Mp4FileData *mMp4FileDataPtr = (M4MP4W_Mp4FileData *)context;

    ERR_CHECK(M4OSA_NULL != context, M4ERR_PARAMETER);

    ERR_CHECK(( mMp4FileDataPtr->state == M4MP4W_opened)
        || (mMp4FileDataPtr->state == M4MP4W_ready), M4ERR_STATE);
    mMp4FileDataPtr->state = M4MP4W_ready;

    switch (streamDescPtr->streamType)
    {
        case M4SYS_kAMR:
        case M4SYS_kAAC:
        case M4SYS_kEVRC:
            /*Audio*/
            ERR_CHECK(streamDescPtr->streamID == AudioStreamID,
                M4ERR_PARAMETER);

            /*check if an audio track has already been added*/
            ERR_CHECK(mMp4FileDataPtr->hasAudio == M4OSA_FALSE,
                M4ERR_BAD_CONTEXT);

            /*check if alloc need to be done*/
            if (mMp4FileDataPtr->audioTrackPtr == M4OSA_NULL)
            {
                mMp4FileDataPtr->audioTrackPtr = (M4MP4W_AudioTrackData
                    *)M4OSA_32bitAlignedMalloc(sizeof(M4MP4W_AudioTrackData),
                    M4MP4_WRITER, (M4OSA_Char *)"M4MP4W_AudioTrackData");
                ERR_CHECK(mMp4FileDataPtr->audioTrackPtr != M4OSA_NULL,
                    M4ERR_ALLOC);

                /**
                * We must init these pointers in case an alloc bellow fails */
                mMp4FileDataPtr->audioTrackPtr->Chunk = M4OSA_NULL;
                mMp4FileDataPtr->audioTrackPtr->chunkOffsetTable = M4OSA_NULL;
                mMp4FileDataPtr->audioTrackPtr->chunkSizeTable = M4OSA_NULL;
                mMp4FileDataPtr->audioTrackPtr->chunkSampleNbTable = M4OSA_NULL;
                mMp4FileDataPtr->audioTrackPtr->chunkTimeMsTable = M4OSA_NULL;
                mMp4FileDataPtr->audioTrackPtr->TABLE_STTS = M4OSA_NULL;
                mMp4FileDataPtr->audioTrackPtr->TABLE_STSZ = M4OSA_NULL;
                mMp4FileDataPtr->audioTrackPtr->DSI = M4OSA_NULL;

                /*now dynamic*/

#ifdef _M4MP4W_MOOV_FIRST

                mMp4FileDataPtr->audioTrackPtr->Chunk =
                    (M4OSA_UChar ** )M4OSA_32bitAlignedMalloc(M4MP4W_CHUNK_AUDIO_ALLOC_NB
                    * sizeof(M4OSA_UChar *),
                    M4MP4_WRITER, (M4OSA_Char *)"audioTrackPtr->Chunk");
                ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->Chunk != M4OSA_NULL,
                    M4ERR_ALLOC);

#else

                mMp4FileDataPtr->audioTrackPtr->Chunk =
                    (M4OSA_UChar ** )M4OSA_32bitAlignedMalloc(sizeof(M4OSA_UChar *),
                    M4MP4_WRITER, (M4OSA_Char *)"audioTrackPtr->Chunk");
                ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->Chunk != M4OSA_NULL,
                    M4ERR_ALLOC);
                mMp4FileDataPtr->audioTrackPtr->Chunk[0] = M4OSA_NULL;

                mMp4FileDataPtr->audioTrackPtr->chunkOffsetTable =
                    (M4OSA_UInt32 *)M4OSA_32bitAlignedMalloc(M4MP4W_CHUNK_AUDIO_ALLOC_NB
                    * sizeof(M4OSA_UInt32),
                    M4MP4_WRITER, (M4OSA_Char *)"audioTrackPtr->chunkOffsetTable");
                ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->chunkOffsetTable
                    != M4OSA_NULL, M4ERR_ALLOC);

#endif /*_M4MP4W_MOOV_FIRST*/

                mMp4FileDataPtr->audioTrackPtr->TABLE_STTS =
                    (M4OSA_UInt32 *)M4OSA_32bitAlignedMalloc(M4MP4W_STTS_AUDIO_ALLOC_SIZE,
                    M4MP4_WRITER, (M4OSA_Char *)"audioTrackPtr->TABLE_STTS");
                ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->TABLE_STTS
                    != M4OSA_NULL, M4ERR_ALLOC);
                mMp4FileDataPtr->audioTrackPtr->nbOfAllocatedSttsBlocks = 1;

                mMp4FileDataPtr->audioTrackPtr->chunkSizeTable =
                    (M4OSA_UInt32 *)M4OSA_32bitAlignedMalloc(M4MP4W_CHUNK_AUDIO_ALLOC_NB
                    * sizeof(M4OSA_UInt32),
                    M4MP4_WRITER, (M4OSA_Char *)"audioTrackPtr->chunkSizeTable");
                ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->chunkSizeTable
                    != M4OSA_NULL, M4ERR_ALLOC);
                mMp4FileDataPtr->audioTrackPtr->chunkSampleNbTable =
                    (M4OSA_UInt32 *)M4OSA_32bitAlignedMalloc(M4MP4W_CHUNK_AUDIO_ALLOC_NB
                    * sizeof(M4OSA_UInt32),
                    M4MP4_WRITER, (M4OSA_Char *)"audioTrackPtr->chunkSampleNbTable");
                ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->chunkSampleNbTable
                    != M4OSA_NULL, M4ERR_ALLOC);
                mMp4FileDataPtr->audioTrackPtr->chunkTimeMsTable =
                    (M4OSA_UInt32 *)M4OSA_32bitAlignedMalloc(M4MP4W_CHUNK_AUDIO_ALLOC_NB
                    * sizeof(M4OSA_UInt32),
                    M4MP4_WRITER, (M4OSA_Char *)"audioTrackPtr->chunkTimeMsTable");
                ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->chunkTimeMsTable
                    != M4OSA_NULL, M4ERR_ALLOC);

                mMp4FileDataPtr->audioTrackPtr->LastAllocatedChunk = 0;
            }
            mMp4FileDataPtr->hasAudio = M4OSA_TRUE;
            mMp4FileDataPtr->filesize += 402;
            mMp4FileDataPtr->audioTrackPtr->MaxChunkSize =
                mMp4FileDataPtr->MaxChunkSize; /* init value */
            mMp4FileDataPtr->audioTrackPtr->MaxAUSize =
                mMp4FileDataPtr->MaxAUSize;
            mMp4FileDataPtr->audioTrackPtr->CommonData.lastCTS = 0;
            mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb = 0;
            mMp4FileDataPtr->audioTrackPtr->CommonData.sampleSize = 0;
            mMp4FileDataPtr->audioTrackPtr->CommonData.sttsTableEntryNb = 1;
            mMp4FileDataPtr->audioTrackPtr->CommonData.timescale =
                streamDescPtr->timeScale;
            mMp4FileDataPtr->audioTrackPtr->chunkSizeTable[0] = 0;     /*init*/
            mMp4FileDataPtr->audioTrackPtr->chunkSampleNbTable[0] = 0; /*init*/
            mMp4FileDataPtr->audioTrackPtr->chunkTimeMsTable[0] = 0;   /*init*/
            mMp4FileDataPtr->audioTrackPtr->currentChunk =
                0; /*1st chunk is Chunk[0]*/
            mMp4FileDataPtr->audioTrackPtr->currentPos = 0;
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

            mMp4FileDataPtr->audioTrackPtr->currentStsc = 0;

#endif

            mMp4FileDataPtr->audioTrackPtr->microState = M4MP4W_ready;
            mMp4FileDataPtr->audioTrackPtr->nbOfAllocatedStszBlocks = 0;
            mMp4FileDataPtr->audioTrackPtr->TABLE_STSZ = M4OSA_NULL;

            mMp4FileDataPtr->audioTrackPtr->avgBitrate =
                streamDescPtr->averageBitrate;
            mMp4FileDataPtr->audioTrackPtr->maxBitrate =
                streamDescPtr->maxBitrate;

            if (streamDescPtr->streamType == M4SYS_kAMR)
            {

                mMp4FileDataPtr->audioTrackPtr->CommonData.trackType =
                    M4SYS_kAMR;
                ERR_CHECK(streamDescPtr->timeScale == 8000, M4ERR_PARAMETER);
                mMp4FileDataPtr->audioTrackPtr->sampleDuration =
                    160; /*AMR8+timescale=8000 => sample duration 160 constant*/

                /*Use given DSI if passed, else use default value*/
                if (streamDescPtr->decoderSpecificInfoSize != 0)
                {
                    /*amr DSI is 9 bytes long !*/
                    mMp4FileDataPtr->audioTrackPtr->dsiSize =
                        9; /*always 9 for amr*/
                    ERR_CHECK(streamDescPtr->decoderSpecificInfoSize == 9,
                        M4ERR_PARAMETER);
                    mMp4FileDataPtr->audioTrackPtr->DSI =
                        (M4OSA_UChar *)M4OSA_32bitAlignedMalloc(9, M4MP4_WRITER,
                        (M4OSA_Char *)"audioTrackPtr->DSI");
                    ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->DSI != M4OSA_NULL,
                        M4ERR_ALLOC);
                    memcpy(
                        (void *)mMp4FileDataPtr->audioTrackPtr->DSI,
                        (void *)streamDescPtr->decoderSpecificInfo,
                        9);
                }
                else
                {
                    mMp4FileDataPtr->audioTrackPtr->DSI =
                        M4OSA_NULL; /*default static block will be used*/
                    mMp4FileDataPtr->audioTrackPtr->dsiSize =
                        0; /*but the actual static dsi is 9 bytes !*/
                }
            }
            else if (streamDescPtr->streamType == M4SYS_kEVRC)
            {

                mMp4FileDataPtr->audioTrackPtr->CommonData.trackType =
                    M4SYS_kEVRC;
                ERR_CHECK(streamDescPtr->timeScale == 8000, M4ERR_PARAMETER);
                mMp4FileDataPtr->audioTrackPtr->sampleDuration =
                    160; /*EVRC+timescale=8000 => sample duration 160 constant*/

                /*Use given DSI if passed, else use default value*/
                if (streamDescPtr->decoderSpecificInfoSize != 0)
                {
                    /*evrc DSI is 6 bytes long !*/
                    mMp4FileDataPtr->audioTrackPtr->dsiSize =
                        6; /*always 6 for evrc*/
                    ERR_CHECK(streamDescPtr->decoderSpecificInfoSize == 6,
                        M4ERR_PARAMETER);
                    mMp4FileDataPtr->audioTrackPtr->DSI =
                        (M4OSA_UChar *)M4OSA_32bitAlignedMalloc(6, M4MP4_WRITER,
                        (M4OSA_Char *)"audioTrackPtr->DSI");
                    ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->DSI != M4OSA_NULL,
                        M4ERR_ALLOC);
                    memcpy(
                        (void *)mMp4FileDataPtr->audioTrackPtr->DSI,
                        (void *)streamDescPtr->decoderSpecificInfo,
                        6);
                }
                else
                {
                    mMp4FileDataPtr->audioTrackPtr->DSI =
                        M4OSA_NULL; /*default static block will be used*/
                    mMp4FileDataPtr->audioTrackPtr->dsiSize =
                        0; /*but the actual static dsi is 6 bytes !*/
                }
            }
            else /*M4SYS_kAAC*/
            {
                /*avg bitrate should be set*/
                ERR_CHECK(streamDescPtr->averageBitrate != -1, M4ERR_PARAMETER);
                ERR_CHECK(streamDescPtr->maxBitrate != -1, M4ERR_PARAMETER);

                mMp4FileDataPtr->audioTrackPtr->CommonData.trackType =
                    M4SYS_kAAC;
                mMp4FileDataPtr->audioTrackPtr->sampleDuration =
                    0; /*don't know for aac, so set 0*/

                mMp4FileDataPtr->audioTrackPtr->dsiSize =
                    (M4OSA_UInt8)streamDescPtr->decoderSpecificInfoSize;

                if (mMp4FileDataPtr->audioTrackPtr->dsiSize != 0)
                {
                    mMp4FileDataPtr->audioTrackPtr->DSI =
                        (M4OSA_UChar *)M4OSA_32bitAlignedMalloc(
                        streamDescPtr->decoderSpecificInfoSize,
                        M4MP4_WRITER, (M4OSA_Char *)"audioTrackPtr->DSI");
                    ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->DSI != M4OSA_NULL,
                        M4ERR_ALLOC);
                    memcpy(
                        (void *)mMp4FileDataPtr->audioTrackPtr->DSI,
                        (void *)streamDescPtr->decoderSpecificInfo,
                        streamDescPtr->decoderSpecificInfoSize);
                }
                else
                {
                    /*no dsi: return bad parameter ?*/
                    return M4ERR_PARAMETER;
                }
            }

            break;

        case (M4SYS_kMPEG_4):
        case (M4SYS_kH264):
        case (M4SYS_kH263):
            /*Video*/
            ERR_CHECK(streamDescPtr->streamID == VideoStreamID,
                M4ERR_PARAMETER);

            /*check if a video track has already been added*/
            ERR_CHECK(mMp4FileDataPtr->hasVideo == M4OSA_FALSE,
                M4ERR_BAD_CONTEXT);

            /*check if alloc need to be done*/
            if (mMp4FileDataPtr->videoTrackPtr == M4OSA_NULL)
            {
                mMp4FileDataPtr->videoTrackPtr = (M4MP4W_VideoTrackData
                    *)M4OSA_32bitAlignedMalloc(sizeof(M4MP4W_VideoTrackData),
                    M4MP4_WRITER, (M4OSA_Char *)"M4MP4W_VideoTrackData");
                ERR_CHECK(mMp4FileDataPtr->videoTrackPtr != M4OSA_NULL,
                    M4ERR_ALLOC);

                /**
                * We must init these pointers in case an alloc bellow fails */
                mMp4FileDataPtr->videoTrackPtr->Chunk = M4OSA_NULL;
                mMp4FileDataPtr->videoTrackPtr->chunkOffsetTable = M4OSA_NULL;
                mMp4FileDataPtr->videoTrackPtr->chunkSizeTable = M4OSA_NULL;
                mMp4FileDataPtr->videoTrackPtr->chunkSampleNbTable = M4OSA_NULL;
                mMp4FileDataPtr->videoTrackPtr->chunkTimeMsTable = M4OSA_NULL;
                mMp4FileDataPtr->videoTrackPtr->TABLE_STTS = M4OSA_NULL;
                mMp4FileDataPtr->videoTrackPtr->TABLE_STSZ = M4OSA_NULL;
                mMp4FileDataPtr->videoTrackPtr->TABLE_STSS = M4OSA_NULL;
                mMp4FileDataPtr->videoTrackPtr->DSI = M4OSA_NULL;

                /*now dynamic*/

#ifdef _M4MP4W_MOOV_FIRST

                mMp4FileDataPtr->videoTrackPtr->Chunk =
                    (M4OSA_UChar ** )M4OSA_32bitAlignedMalloc(M4MP4W_CHUNK_ALLOC_NB
                    * sizeof(M4OSA_UChar *),
                    M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->Chunk");
                ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->Chunk != M4OSA_NULL,
                    M4ERR_ALLOC);

#else
                /*re-use the same chunk and flush it when full*/

                mMp4FileDataPtr->videoTrackPtr->Chunk =
                    (M4OSA_UChar ** )M4OSA_32bitAlignedMalloc(sizeof(M4OSA_UChar *),
                    M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->Chunk");
                ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->Chunk != M4OSA_NULL,
                    M4ERR_ALLOC);
                mMp4FileDataPtr->videoTrackPtr->Chunk[0] = M4OSA_NULL;

                mMp4FileDataPtr->videoTrackPtr->chunkOffsetTable =
                    (M4OSA_UInt32 *)M4OSA_32bitAlignedMalloc(M4MP4W_CHUNK_ALLOC_NB
                    * sizeof(M4OSA_UInt32),
                    M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->chunkOffsetTable");
                ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->chunkOffsetTable
                    != M4OSA_NULL, M4ERR_ALLOC);

#endif /*_M4MP4W_MOOV_FIRST*/

                ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->Chunk != M4OSA_NULL,
                    M4ERR_ALLOC);
                mMp4FileDataPtr->videoTrackPtr->chunkSizeTable =
                    (M4OSA_UInt32 *)M4OSA_32bitAlignedMalloc(M4MP4W_CHUNK_ALLOC_NB
                    * sizeof(M4OSA_UInt32),
                    M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->chunkSizeTable");
                ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->chunkSizeTable
                    != M4OSA_NULL, M4ERR_ALLOC);
                mMp4FileDataPtr->videoTrackPtr->chunkSampleNbTable =
                    (M4OSA_UInt32 *)M4OSA_32bitAlignedMalloc(M4MP4W_CHUNK_ALLOC_NB
                    * sizeof(M4OSA_UInt32),
                    M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->chunkSampleNbTable");
                ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->chunkSampleNbTable
                    != M4OSA_NULL, M4ERR_ALLOC);
                mMp4FileDataPtr->videoTrackPtr->chunkTimeMsTable =
                    (M4MP4W_Time32 *)M4OSA_32bitAlignedMalloc(M4MP4W_CHUNK_ALLOC_NB
                    * sizeof(M4MP4W_Time32),
                    M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->chunkTimeMsTable");
                ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->chunkTimeMsTable
                    != M4OSA_NULL, M4ERR_ALLOC);

                mMp4FileDataPtr->videoTrackPtr->LastAllocatedChunk = 0;
                /*tables are now dynamic*/
                mMp4FileDataPtr->videoTrackPtr->TABLE_STTS =
                    (M4OSA_UInt32 *)M4OSA_32bitAlignedMalloc(M4MP4W_STTS_ALLOC_SIZE,
                    M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->TABLE_STTS");
                ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->TABLE_STTS
                    != M4OSA_NULL, M4ERR_ALLOC);
                mMp4FileDataPtr->videoTrackPtr->nbOfAllocatedSttsBlocks = 1;
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

                mMp4FileDataPtr->videoTrackPtr->TABLE_STSZ =
                    (M4OSA_UInt16 *)M4OSA_32bitAlignedMalloc(M4MP4W_STSZ_ALLOC_SIZE,
                    M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->TABLE_STSZ");

#else

                mMp4FileDataPtr->videoTrackPtr->TABLE_STSZ =
                    (M4OSA_UInt32 *)M4OSA_32bitAlignedMalloc(M4MP4W_STSZ_ALLOC_SIZE,
                    M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->TABLE_STSZ");

#endif

                ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->TABLE_STSZ
                    != M4OSA_NULL, M4ERR_ALLOC);
                mMp4FileDataPtr->videoTrackPtr->nbOfAllocatedStszBlocks = 1;
                mMp4FileDataPtr->videoTrackPtr->TABLE_STSS =
                    (M4OSA_UInt32 *)M4OSA_32bitAlignedMalloc(M4MP4W_STSS_ALLOC_SIZE,
                    M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->TABLE_STSS");
                ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->TABLE_STSS
                    != M4OSA_NULL, M4ERR_ALLOC);
                mMp4FileDataPtr->videoTrackPtr->nbOfAllocatedStssBlocks = 1;
            }
            mMp4FileDataPtr->hasVideo = M4OSA_TRUE;
            mMp4FileDataPtr->filesize += 462;
            mMp4FileDataPtr->videoTrackPtr->width = M4MP4W_DefaultWidth;
            mMp4FileDataPtr->videoTrackPtr->height = M4MP4W_DefaultHeight;
            mMp4FileDataPtr->videoTrackPtr->MaxAUSize =
                mMp4FileDataPtr->MaxAUSize;
            mMp4FileDataPtr->videoTrackPtr->CommonData.trackType =
                streamDescPtr->streamType;
            mMp4FileDataPtr->videoTrackPtr->MaxChunkSize =
                mMp4FileDataPtr->MaxChunkSize; /* init value */
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

            mMp4FileDataPtr->videoTrackPtr->MaxAUperChunk =
                M4MP4W_VIDEO_MAX_AU_PER_CHUNK;

#endif

            ERR_CHECK(streamDescPtr->timeScale == 1000, M4ERR_PARAMETER);
            mMp4FileDataPtr->videoTrackPtr->CommonData.timescale = 1000;
            mMp4FileDataPtr->videoTrackPtr->CommonData.lastCTS = 0;
            mMp4FileDataPtr->videoTrackPtr->CommonData.sampleNb = 0;
            mMp4FileDataPtr->videoTrackPtr->CommonData.sampleSize = 0;
            mMp4FileDataPtr->videoTrackPtr->CommonData.sttsTableEntryNb = 1;
            mMp4FileDataPtr->videoTrackPtr->chunkSizeTable[0] = 0;     /*init*/
            mMp4FileDataPtr->videoTrackPtr->chunkSampleNbTable[0] = 0; /*init*/
            mMp4FileDataPtr->videoTrackPtr->chunkTimeMsTable[0] = 0;   /*init*/
            mMp4FileDataPtr->videoTrackPtr->currentChunk =
                0; /*1st chunk is Chunk[0]*/
            mMp4FileDataPtr->videoTrackPtr->currentPos = 0;
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

            mMp4FileDataPtr->videoTrackPtr->currentStsc = 0;

#endif

            mMp4FileDataPtr->videoTrackPtr->stssTableEntryNb = 0;
            mMp4FileDataPtr->videoTrackPtr->microState = M4MP4W_ready;

            if (streamDescPtr->streamType == M4SYS_kH263)
            {
                if (( streamDescPtr->averageBitrate == -1)
                    || (streamDescPtr->maxBitrate == -1))
                {
                    /*the bitrate will not be written if the bitrate information
                     is not fully set */
                    mMp4FileDataPtr->videoTrackPtr->avgBitrate = -1;
                    mMp4FileDataPtr->videoTrackPtr->maxBitrate = -1;
                }
                else
                {
                    /*proprietary storage of h263 bitrate.
                     Warning: not the actual bitrate (bit set to 1).*/
                    mMp4FileDataPtr->videoTrackPtr->avgBitrate =
                        streamDescPtr->averageBitrate;
                    mMp4FileDataPtr->videoTrackPtr->maxBitrate =
                        streamDescPtr->maxBitrate;
                }

                if (( 0 != streamDescPtr->decoderSpecificInfoSize)
                    && (M4OSA_NULL != streamDescPtr->decoderSpecificInfo))
                {
                    /*decoder specific info size is supposed to be always 7 bytes long */
                    ERR_CHECK(streamDescPtr->decoderSpecificInfoSize == 7,
                        M4ERR_PARAMETER);
                    mMp4FileDataPtr->videoTrackPtr->dsiSize =
                        (M4OSA_UInt8)streamDescPtr->decoderSpecificInfoSize;
                    mMp4FileDataPtr->videoTrackPtr->DSI =
                        (M4OSA_UChar *)M4OSA_32bitAlignedMalloc(
                        streamDescPtr->decoderSpecificInfoSize,
                        M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->DSI");
                    ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->DSI != M4OSA_NULL,
                        M4ERR_ALLOC);
                    memcpy(
                        (void *)mMp4FileDataPtr->videoTrackPtr->DSI,
                        (void *)streamDescPtr->decoderSpecificInfo,
                        streamDescPtr->decoderSpecificInfoSize);
                }
                else
                {
                    /*use the default dsi*/
                    mMp4FileDataPtr->videoTrackPtr->DSI = M4OSA_NULL;
                    mMp4FileDataPtr->videoTrackPtr->dsiSize = 0;
                }
            }

            if (streamDescPtr->streamType == M4SYS_kMPEG_4)
            {
                mMp4FileDataPtr->filesize += 22; /*extra bytes (from h263)*/
                /* allow DSI to be M4OSA_NULL, in which case the actual DSI will be
                 set by setOption. */
                if (( 0 == streamDescPtr->decoderSpecificInfoSize)
                    || (M4OSA_NULL == streamDescPtr->decoderSpecificInfo))
                {
                    mMp4FileDataPtr->videoTrackPtr->DSI = M4OSA_NULL;
                    mMp4FileDataPtr->videoTrackPtr->dsiSize = 0;
                }
                else
                {
                    /*MP4V specific*/
                    /*decoder specific info size is supposed to be always <
                        105 so that ESD size can be coded with 1 byte*/
                    /*(this should not be restrictive because dsi is always shorter !)*/
                    ERR_CHECK(streamDescPtr->decoderSpecificInfoSize < 105,
                        M4ERR_PARAMETER);
                    mMp4FileDataPtr->videoTrackPtr->dsiSize =
                        (M4OSA_UInt8)streamDescPtr->decoderSpecificInfoSize;
                    mMp4FileDataPtr->videoTrackPtr->DSI =
                        (M4OSA_UChar *)M4OSA_32bitAlignedMalloc(
                        streamDescPtr->decoderSpecificInfoSize,
                        M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->DSI");
                    ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->DSI != M4OSA_NULL,
                        M4ERR_ALLOC);
                    memcpy(
                        (void *)mMp4FileDataPtr->videoTrackPtr->DSI,
                        (void *)streamDescPtr->decoderSpecificInfo,
                        streamDescPtr->decoderSpecificInfoSize);
                    mMp4FileDataPtr->filesize +=
                        streamDescPtr->decoderSpecificInfoSize;
                }
                /*avg bitrate should be set*/
                ERR_CHECK(streamDescPtr->averageBitrate != -1, M4ERR_PARAMETER);
                mMp4FileDataPtr->videoTrackPtr->avgBitrate =
                    streamDescPtr->averageBitrate;
                mMp4FileDataPtr->videoTrackPtr->maxBitrate =
                    streamDescPtr->averageBitrate;
            }

            if (streamDescPtr->streamType == M4SYS_kH264)
            {
                /* H264 specific information */
                mMp4FileDataPtr->videoTrackPtr->avgBitrate =
                    streamDescPtr->averageBitrate;
                mMp4FileDataPtr->videoTrackPtr->maxBitrate =
                    streamDescPtr->maxBitrate;

                if ((0 != streamDescPtr->decoderSpecificInfoSize)
                    && (M4OSA_NULL != streamDescPtr->decoderSpecificInfo))
                {
                    /* + H.264 trimming */
                    if (M4OSA_TRUE == mMp4FileDataPtr->bMULPPSSPS)
                    {
                        M4OSA_UInt16 SPSLength, PPSLength;
                        M4OSA_UInt16 *DSI;
                        /* Store the DSI size */
                        mMp4FileDataPtr->videoTrackPtr->dsiSize =
                            (M4OSA_UInt8)streamDescPtr->decoderSpecificInfoSize
                            - 24;

                        /* Copy the DSI (SPS + PPS) */
                        mMp4FileDataPtr->videoTrackPtr->DSI =
                            (M4OSA_UChar *)M4OSA_32bitAlignedMalloc(
                            streamDescPtr->decoderSpecificInfoSize,
                            M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->DSI");
                        ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->DSI
                            != M4OSA_NULL, M4ERR_ALLOC);

                        DSI =
                            (M4OSA_UInt16 *)streamDescPtr->decoderSpecificInfo;
                        SPSLength = DSI[6];
                        PPSLength = DSI[10];
                        memcpy(
                            (void *)mMp4FileDataPtr->videoTrackPtr->DSI,
                            (void *)((streamDescPtr->
                            decoderSpecificInfo)+12), 2);
                        memcpy(
                            (void *)((mMp4FileDataPtr->videoTrackPtr->
                            DSI)+2), (void *)((streamDescPtr->
                            decoderSpecificInfo)+28), SPSLength);

                        memcpy(
                            (void *)((mMp4FileDataPtr->videoTrackPtr->
                            DSI)+2 + SPSLength),
                            (void *)((streamDescPtr->
                            decoderSpecificInfo)+20), 2);
                        memcpy(
                            (void *)((mMp4FileDataPtr->videoTrackPtr->
                            DSI)+4 + SPSLength),
                            (void *)((streamDescPtr->
                            decoderSpecificInfo)+28 + SPSLength),
                            PPSLength);
                        /* - H.264 trimming */
                    }
                    else
                    {
                        /* Store the DSI size */
                        mMp4FileDataPtr->videoTrackPtr->dsiSize =
                            (M4OSA_UInt8)streamDescPtr->decoderSpecificInfoSize;

                        /* Copy the DSI (SPS + PPS) */
                        mMp4FileDataPtr->videoTrackPtr->DSI =
                            (M4OSA_UChar *)M4OSA_32bitAlignedMalloc(
                            streamDescPtr->decoderSpecificInfoSize,
                            M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->DSI");
                        ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->DSI
                            != M4OSA_NULL, M4ERR_ALLOC);
                        memcpy(
                            (void *)mMp4FileDataPtr->videoTrackPtr->DSI,
                            (void *)streamDescPtr->
                            decoderSpecificInfo,
                            streamDescPtr->decoderSpecificInfoSize);
                    }
                }
                else
                {
                    /*use the default dsi*/
                    mMp4FileDataPtr->videoTrackPtr->DSI = M4OSA_NULL;
                    mMp4FileDataPtr->videoTrackPtr->dsiSize = 0;
                }
            }
            break;

        default:
            err = M4ERR_PARAMETER;
    }

    return err;
}

/*******************************************************************************/
M4OSA_ERR M4MP4W_startWriting( M4OSA_Context context )
/*******************************************************************************/
{
    M4OSA_ERR err = M4NO_ERROR;
    M4OSA_UInt32 fileModeAccess = M4OSA_kFileWrite | M4OSA_kFileCreate;
    M4OSA_UInt32 i;
    M4MP4W_Mp4FileData *mMp4FileDataPtr = (M4MP4W_Mp4FileData *)context;
    ERR_CHECK(context != M4OSA_NULL, M4ERR_PARAMETER);

    ERR_CHECK((mMp4FileDataPtr->state == M4MP4W_ready), M4ERR_STATE);
    mMp4FileDataPtr->state = M4MP4W_writing;

    /*audio microstate */
    /*    if (mMp4FileDataPtr->audioTrackPtr != M4OSA_NULL)*/
    if (mMp4FileDataPtr->hasAudio)
    {
        ERR_CHECK((mMp4FileDataPtr->audioTrackPtr->microState == M4MP4W_ready),
            M4ERR_STATE);
        mMp4FileDataPtr->audioTrackPtr->microState = M4MP4W_writing;

        /* First audio chunk allocation */
        mMp4FileDataPtr->audioTrackPtr->Chunk[0] = (M4OSA_UChar
            *)M4OSA_32bitAlignedMalloc(mMp4FileDataPtr->audioTrackPtr->MaxChunkSize,
            M4MP4_WRITER, (M4OSA_Char *)"audioTrackPtr->Chunk[0]");
        ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->Chunk[0] != M4OSA_NULL,
            M4ERR_ALLOC);
    }

    /*video microstate*/
    /*    if (mMp4FileDataPtr->videoTrackPtr != M4OSA_NULL)*/
    if (mMp4FileDataPtr->hasVideo)
    {
        ERR_CHECK((mMp4FileDataPtr->videoTrackPtr->microState == M4MP4W_ready),
            M4ERR_STATE);
        mMp4FileDataPtr->videoTrackPtr->microState = M4MP4W_writing;

        /* First video chunk allocation */
        mMp4FileDataPtr->videoTrackPtr->Chunk[0] = (M4OSA_UChar
            *)M4OSA_32bitAlignedMalloc(mMp4FileDataPtr->videoTrackPtr->MaxChunkSize,
            M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->Chunk[0]");
        ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->Chunk[0] != M4OSA_NULL,
            M4ERR_ALLOC);
    }

    if (mMp4FileDataPtr->estimateAudioSize == M4OSA_TRUE)
    {
        /*set audioMsChunkDur (duration in ms before a new chunk is created)
         for audio size estimation*/
        ERR_CHECK(mMp4FileDataPtr->hasVideo, M4ERR_BAD_CONTEXT);
        ERR_CHECK(mMp4FileDataPtr->hasAudio, M4ERR_BAD_CONTEXT);

        mMp4FileDataPtr->audioMsChunkDur =
            20 * mMp4FileDataPtr->audioTrackPtr->MaxChunkSize
            / mMp4FileDataPtr->audioTrackPtr->MaxAUSize;

        if (( mMp4FileDataPtr->InterleaveDur != 0)
            && (mMp4FileDataPtr->InterleaveDur
            < 20 *mMp4FileDataPtr->audioTrackPtr->MaxChunkSize
            / mMp4FileDataPtr->audioTrackPtr->MaxAUSize))
        {
            mMp4FileDataPtr->audioMsChunkDur = mMp4FileDataPtr->InterleaveDur;
        }
    }

#ifndef _M4MP4W_MOOV_FIRST

    /*open file in write binary mode*/

    err = mMp4FileDataPtr->fileWriterFunctions->openWrite(
        &mMp4FileDataPtr->fileWriterContext,
        mMp4FileDataPtr->url, fileModeAccess);
    ERR_CHECK((M4NO_ERROR == err), err);

    /*ftyp atom*/
    if (mMp4FileDataPtr->ftyp.major_brand != 0)
    {
        /* Put customized ftyp box */
        err =
            M4MP4W_putBE32(16 + (mMp4FileDataPtr->ftyp.nbCompatibleBrands * 4),
            mMp4FileDataPtr->fileWriterFunctions,
            mMp4FileDataPtr->fileWriterContext);
        ERR_CHECK((M4NO_ERROR == err), err);
        err = M4MP4W_putBE32(M4MPAC_FTYP_TAG,
            mMp4FileDataPtr->fileWriterFunctions,
            mMp4FileDataPtr->fileWriterContext);
        ERR_CHECK((M4NO_ERROR == err), err);
        err = M4MP4W_putBE32(mMp4FileDataPtr->ftyp.major_brand,
            mMp4FileDataPtr->fileWriterFunctions,
            mMp4FileDataPtr->fileWriterContext);
        ERR_CHECK((M4NO_ERROR == err), err);
        err = M4MP4W_putBE32(mMp4FileDataPtr->ftyp.minor_version,
            mMp4FileDataPtr->fileWriterFunctions,
            mMp4FileDataPtr->fileWriterContext);
        ERR_CHECK((M4NO_ERROR == err), err);

        for ( i = 0; i < mMp4FileDataPtr->ftyp.nbCompatibleBrands; i++ )
        {
            err = M4MP4W_putBE32(mMp4FileDataPtr->ftyp.compatible_brands[i],
                mMp4FileDataPtr->fileWriterFunctions,
                mMp4FileDataPtr->fileWriterContext);
            ERR_CHECK((M4NO_ERROR == err), err);
        }
    }
    else
    {
        /* Put default ftyp box */
        err = M4MP4W_putBlock(Default_ftyp, sizeof(Default_ftyp),
            mMp4FileDataPtr->fileWriterFunctions,
            mMp4FileDataPtr->fileWriterContext);
        ERR_CHECK((M4NO_ERROR == err), err);
    }

    /*init mdat value with 0 but the right value is set just before the file is closed*/
    err = M4MP4W_putBE32(0, mMp4FileDataPtr->fileWriterFunctions,
        mMp4FileDataPtr->fileWriterContext);
    ERR_CHECK((M4NO_ERROR == err), err);
    err = M4MP4W_putBlock(CommonBlock2, sizeof(CommonBlock2),
        mMp4FileDataPtr->fileWriterFunctions,
        mMp4FileDataPtr->fileWriterContext);
    ERR_CHECK((M4NO_ERROR == err), err);

#endif /*_M4MP4W_MOOV_FIRST*/

#ifdef _M4MP4W_RESERVED_MOOV_DISK_SPACE

    if (0 != mMp4FileDataPtr->MaxFileSize
        && M4OSA_NULL != mMp4FileDataPtr->safetyFileUrl)
    {
        M4OSA_ERR err2 = M4NO_ERROR;
        M4OSA_Context safetyFileContext = M4OSA_NULL;
        M4OSA_UInt32 safetyFileSize = 0, addendum = 0;
        M4OSA_UChar dummyData[100]; /* To fill the safety file with */

        err =
            mMp4FileDataPtr->fileWriterFunctions->openWrite(&safetyFileContext,
            mMp4FileDataPtr->safetyFileUrl, fileModeAccess);
        ERR_CHECK((M4NO_ERROR == err), err);

        mMp4FileDataPtr->cleanSafetyFile = M4OSA_TRUE;

        /* 10% seems to be a reasonable worst case, but also provision for 1kb of moov overhead.*/
        safetyFileSize = 1000 + (mMp4FileDataPtr->MaxFileSize * 10 + 99) / 100;

        /* Here we add space to take into account the fact we have to flush any pending
        chunk in closeWrite, this space is the sum of the maximum chunk sizes, for each
        track. */

#ifndef _M4MP4W_UNBUFFERED_VIDEO

        if (mMp4FileDataPtr->hasVideo)
        {
            safetyFileSize += mMp4FileDataPtr->videoTrackPtr->MaxChunkSize;
        }

#endif

        if (mMp4FileDataPtr->hasAudio)
        {
            safetyFileSize += mMp4FileDataPtr->audioTrackPtr->MaxChunkSize;
        }

        memset((void *)dummyData, 0xCA,sizeof(dummyData)); /* For extra safety. */

        for ( i = 0;
            i < (safetyFileSize + sizeof(dummyData) - 1) / sizeof(dummyData);
            i++ )
        {
            err = mMp4FileDataPtr->fileWriterFunctions->writeData(
                safetyFileContext, dummyData, sizeof(dummyData));

            if (M4NO_ERROR != err)
                break;
            /* Don't return from the function yet, as we need to close the file first. */
        }

        /* I don't need to keep it open. */
        err2 =
            mMp4FileDataPtr->fileWriterFunctions->closeWrite(safetyFileContext);

        if (M4NO_ERROR != err)
        {
            return err;
        }
        else
            ERR_CHECK((M4NO_ERROR == err2), err2);

        M4OSA_TRACE1_0("Safety file correctly created");
    }
#endif /* _M4MP4W_RESERVED_MOOV_DISK_SPACE */

    return err;
}

/*******************************************************************************/
M4OSA_ERR M4MP4W_newAudioChunk( M4OSA_Context context,
                               M4OSA_UInt32 *leftSpaceInChunk )
/*******************************************************************************/
{
    M4OSA_ERR err = M4NO_ERROR;

    M4MP4W_Mp4FileData *mMp4FileDataPtr = (M4MP4W_Mp4FileData *)context;
    M4OSA_Double scale_audio;

#ifndef _M4MP4W_OPTIMIZE_FOR_PHONE

    M4OSA_UInt32 reallocNb;

#endif

    /* video only */

    if (mMp4FileDataPtr->audioTrackPtr == M4OSA_NULL)
        return M4NO_ERROR;

    M4OSA_TRACE1_0(" M4MP4W_newAudioChunk - flush audio");
    M4OSA_TRACE1_2("current chunk = %d  offset = 0x%x",
        mMp4FileDataPtr->audioTrackPtr->currentChunk,
        mMp4FileDataPtr->absoluteCurrentPos);

    scale_audio = 1000.0 / mMp4FileDataPtr->audioTrackPtr->CommonData.timescale;

#ifndef _M4MP4W_MOOV_FIRST
    /*flush chunk*/

    err = M4MP4W_putBlock(mMp4FileDataPtr->audioTrackPtr->Chunk[0],
        mMp4FileDataPtr->audioTrackPtr->currentPos,
        mMp4FileDataPtr->fileWriterFunctions,
        mMp4FileDataPtr->fileWriterContext);

    if (M4NO_ERROR != err)
    {
        M4OSA_FilePosition temp = mMp4FileDataPtr->absoluteCurrentPos;
        M4OSA_TRACE2_1(
            "M4MP4W_newAudioChunk: putBlock error when flushing chunk: %#X",
            err);
        /* Ouch, we got an error writing to the file, but we need to properly react so that the
         state is still consistent and we can properly close the file so that what has been
         recorded so far is not lost. Yay error recovery! */

        /* First, we do not know where we are in the file. Put us back at where we were before
        attempting to write the data. That way, we're consistent with the chunk state data. */
        err = mMp4FileDataPtr->fileWriterFunctions->seek(
            mMp4FileDataPtr->fileWriterContext,
            M4OSA_kFileSeekBeginning, &temp);

        M4OSA_TRACE2_3(
            "Backtracking to position 0x%08X, seek returned %d and position %08X",
            mMp4FileDataPtr->absoluteCurrentPos, err, temp);

        /* Then, do not update any info whatsoever in the writing state. This will have the
         consequence that it will be as if the chunk has not been flushed yet, and therefore
         it will be done as part of closeWrite (where there could be room to do so,
         if some emergency room is freed for that purpose). */

        /* And lastly (for here), return that we've reached the limit of available space. */

        return M4WAR_MP4W_OVERSIZE;
    }

    /*update chunk offset*/
    mMp4FileDataPtr->audioTrackPtr->
        chunkOffsetTable[mMp4FileDataPtr->audioTrackPtr->currentChunk] =
        mMp4FileDataPtr->absoluteCurrentPos;

    /*add chunk size to absoluteCurrentPos*/
    mMp4FileDataPtr->absoluteCurrentPos +=
        mMp4FileDataPtr->audioTrackPtr->currentPos;

#endif /*_M4MP4W_MOOV_FIRST*/

    /*update chunk info */

    mMp4FileDataPtr->audioTrackPtr->
        chunkSizeTable[mMp4FileDataPtr->audioTrackPtr->currentChunk] =
        mMp4FileDataPtr->audioTrackPtr->currentPos;
    mMp4FileDataPtr->audioTrackPtr->
        chunkTimeMsTable[mMp4FileDataPtr->audioTrackPtr->currentChunk] =
        mMp4FileDataPtr->audioTrackPtr->CommonData.lastCTS;

    mMp4FileDataPtr->audioTrackPtr->currentChunk += 1;
    /*if audio amount of data is not estimated*/
    if (mMp4FileDataPtr->estimateAudioSize == M4OSA_FALSE)
        mMp4FileDataPtr->filesize += 16;

    /*alloc new chunk*/
    /*only if not already allocated*/
    if (mMp4FileDataPtr->audioTrackPtr->currentChunk
            > mMp4FileDataPtr->audioTrackPtr->LastAllocatedChunk)
    {
        /*update LastAllocatedChunk ( -> = currentChunk)*/
        mMp4FileDataPtr->audioTrackPtr->LastAllocatedChunk += 1;

        /*max nb of chunk is now dynamic*/
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

        if (mMp4FileDataPtr->audioTrackPtr->LastAllocatedChunk
            + 3 > M4MP4W_CHUNK_AUDIO_ALLOC_NB)
        {
            M4OSA_TRACE1_0("M4MP4W_newAudioChunk : audio chunk table is full");
            return M4WAR_MP4W_OVERSIZE;
        }

#else

        if (((mMp4FileDataPtr->audioTrackPtr->LastAllocatedChunk)
            % M4MP4W_CHUNK_AUDIO_ALLOC_NB) == 0)
        {
            reallocNb = mMp4FileDataPtr->audioTrackPtr->LastAllocatedChunk
                + M4MP4W_CHUNK_AUDIO_ALLOC_NB;

#ifdef _M4MP4W_MOOV_FIRST

            mMp4FileDataPtr->audioTrackPtr->Chunk =
                (M4OSA_UChar ** )M4MP4W_realloc(
                (M4OSA_MemAddr32)mMp4FileDataPtr->audioTrackPtr->Chunk,
                ( reallocNb - M4MP4W_CHUNK_AUDIO_ALLOC_NB)
                * sizeof(M4OSA_UChar *),
                reallocNb * sizeof(M4OSA_UChar *));
            ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->Chunk != M4OSA_NULL,
                M4ERR_ALLOC);

#else

            mMp4FileDataPtr->audioTrackPtr->chunkOffsetTable =
                (M4OSA_UInt32 *)M4MP4W_realloc(
                (M4OSA_MemAddr32)mMp4FileDataPtr->audioTrackPtr->
                chunkOffsetTable,
                ( reallocNb - M4MP4W_CHUNK_AUDIO_ALLOC_NB)
                * sizeof(M4OSA_UInt32),
                reallocNb * sizeof(M4OSA_UInt32));
            ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->chunkOffsetTable
                != M4OSA_NULL, M4ERR_ALLOC);

#endif /*_M4MP4W_MOOV_FIRST*/

            mMp4FileDataPtr->audioTrackPtr->chunkSizeTable =
                (M4OSA_UInt32 *)M4MP4W_realloc(
                (M4OSA_MemAddr32)mMp4FileDataPtr->audioTrackPtr->
                chunkSizeTable,
                ( reallocNb - M4MP4W_CHUNK_AUDIO_ALLOC_NB)
                * sizeof(M4OSA_UInt32),
                reallocNb * sizeof(M4OSA_UInt32));
            ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->chunkSizeTable
                != M4OSA_NULL, M4ERR_ALLOC);

            mMp4FileDataPtr->audioTrackPtr->chunkSampleNbTable =
                (M4OSA_UInt32 *)M4MP4W_realloc(
                (M4OSA_MemAddr32)mMp4FileDataPtr->audioTrackPtr->
                chunkSampleNbTable,
                ( reallocNb - M4MP4W_CHUNK_AUDIO_ALLOC_NB)
                * sizeof(M4OSA_UInt32),
                reallocNb * sizeof(M4OSA_UInt32));
            ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->chunkSampleNbTable
                != M4OSA_NULL, M4ERR_ALLOC);

            mMp4FileDataPtr->audioTrackPtr->chunkTimeMsTable =
                (M4MP4W_Time32 *)M4MP4W_realloc(
                (M4OSA_MemAddr32)mMp4FileDataPtr->audioTrackPtr->
                chunkTimeMsTable,
                ( reallocNb - M4MP4W_CHUNK_AUDIO_ALLOC_NB)
                * sizeof(M4MP4W_Time32),
                reallocNb * sizeof(M4MP4W_Time32));
            ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->chunkTimeMsTable
                != M4OSA_NULL, M4ERR_ALLOC);
        }
#endif /*_M4MP4W_OPTIMIZE_FOR_PHONE*/

#ifdef _M4MP4W_MOOV_FIRST

        mMp4FileDataPtr->audioTrackPtr->
            Chunk[mMp4FileDataPtr->audioTrackPtr->currentChunk] = (M4OSA_UChar
            *)M4OSA_32bitAlignedMalloc(mMp4FileDataPtr->audioTrackPtr->MaxChunkSize,
            M4MP4_WRITER, (M4OSA_Char *)"audioTrackPtr->currentChunk");
        ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->
            Chunk[mMp4FileDataPtr->audioTrackPtr->currentChunk]
        != M4OSA_NULL, M4ERR_ALLOC);

#endif /*_M4MP4W_MOOV_FIRST*/

    }

    /*update leftSpaceInChunk, currentPos and currentChunkDur*/
    *leftSpaceInChunk = mMp4FileDataPtr->audioTrackPtr->MaxChunkSize;
    mMp4FileDataPtr->audioTrackPtr->currentPos = 0;

#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE
    /* check wether to use a new stsc or not */

    if (mMp4FileDataPtr->audioTrackPtr->currentStsc > 0)
    {
        if (( mMp4FileDataPtr->audioTrackPtr->
            chunkSampleNbTable[mMp4FileDataPtr->audioTrackPtr->
            currentStsc] & 0xFFF) != (mMp4FileDataPtr->audioTrackPtr->
            chunkSampleNbTable[mMp4FileDataPtr->audioTrackPtr->currentStsc
            - 1] & 0xFFF))
            mMp4FileDataPtr->audioTrackPtr->currentStsc += 1;
    }
    else
        mMp4FileDataPtr->audioTrackPtr->currentStsc += 1;

    /* max nb of chunk is now dynamic */
    if (mMp4FileDataPtr->audioTrackPtr->currentStsc
        + 3 > M4MP4W_CHUNK_AUDIO_ALLOC_NB)
    {
        M4OSA_TRACE1_0("M4MP4W_newAudioChunk : audio stsc table is full");
        return M4WAR_MP4W_OVERSIZE;
    }

    /* set nb of samples in the new chunk to 0 */
    mMp4FileDataPtr->audioTrackPtr->
        chunkSampleNbTable[mMp4FileDataPtr->audioTrackPtr->currentStsc] =
        0 + (mMp4FileDataPtr->audioTrackPtr->currentChunk << 12);

#else
    /*set nb of samples in the new chunk to 0*/

    mMp4FileDataPtr->audioTrackPtr->
        chunkSampleNbTable[mMp4FileDataPtr->audioTrackPtr->currentChunk] = 0;

#endif

    /*set time of the new chunk to lastCTS (for initialization, but updated further to the
    CTS of the last sample in the chunk)*/

    mMp4FileDataPtr->audioTrackPtr->
        chunkTimeMsTable[mMp4FileDataPtr->audioTrackPtr->currentChunk] =
        (M4OSA_UInt32)(mMp4FileDataPtr->audioTrackPtr->CommonData.lastCTS
        * scale_audio);

    return err;
}

/*******************************************************************************/
M4OSA_ERR M4MP4W_newVideoChunk( M4OSA_Context context,
                               M4OSA_UInt32 *leftSpaceInChunk )
/*******************************************************************************/
{
    M4OSA_ERR err = M4NO_ERROR;

    M4MP4W_Mp4FileData *mMp4FileDataPtr = (M4MP4W_Mp4FileData *)context;
    M4OSA_Double scale_video;

#ifndef _M4MP4W_OPTIMIZE_FOR_PHONE

    M4OSA_UInt32 reallocNb;

#endif

    /* audio only */

    if (mMp4FileDataPtr->videoTrackPtr == M4OSA_NULL)
        return M4NO_ERROR;

    M4OSA_TRACE1_0("M4MP4W_newVideoChunk - flush video");
    M4OSA_TRACE1_2("current chunk = %d  offset = 0x%x",
        mMp4FileDataPtr->videoTrackPtr->currentChunk,
        mMp4FileDataPtr->absoluteCurrentPos);

    scale_video = 1000.0 / mMp4FileDataPtr->videoTrackPtr->CommonData.timescale;

#ifndef _M4MP4W_MOOV_FIRST

#ifdef _M4MP4W_UNBUFFERED_VIDEO
    /* samples are already written to file */
#else
    /*flush chunk*/

    err = M4MP4W_putBlock(mMp4FileDataPtr->videoTrackPtr->Chunk[0],
        mMp4FileDataPtr->videoTrackPtr->currentPos,
        mMp4FileDataPtr->fileWriterFunctions,
        mMp4FileDataPtr->fileWriterContext);

    if (M4NO_ERROR != err)
    {
        M4OSA_FilePosition temp = mMp4FileDataPtr->absoluteCurrentPos;
        M4OSA_TRACE2_1(
            "M4MP4W_newVideoChunk: putBlock error when flushing chunk: %#X",
            err);
        /* Ouch, we got an error writing to the file, but we need to properly react so that the
         state is still consistent and we can properly close the file so that what has been
         recorded so far is not lost. Yay error recovery! */

        /* First, we do not know where we are in the file. Put us back at where we were before
        attempting to write the data. That way, we're consistent with the chunk state data. */
        err = mMp4FileDataPtr->fileWriterFunctions->seek(
            mMp4FileDataPtr->fileWriterContext,
            M4OSA_kFileSeekBeginning, &temp);

        M4OSA_TRACE2_3(
            "Backtracking to position 0x%08X, seek returned %d and position %08X",
            mMp4FileDataPtr->absoluteCurrentPos, err, temp);
        /* Then, do not update any info whatsoever in the writing state. This will have the
         consequence that it will be as if the chunk has not been flushed yet, and therefore it
         will be done as part of closeWrite (where there could be room to do so, if some
         emergency room is freed for that purpose). */

        /* And lastly (for here), return that we've reached the limit of available space.
         We don't care about the error originally returned by putBlock. */

        return M4WAR_MP4W_OVERSIZE;
    }

#endif

    /*update chunk offset*/

    mMp4FileDataPtr->videoTrackPtr->
        chunkOffsetTable[mMp4FileDataPtr->videoTrackPtr->currentChunk] =
        mMp4FileDataPtr->absoluteCurrentPos;

    /*add chunk size to absoluteCurrentPos*/
    mMp4FileDataPtr->absoluteCurrentPos +=
        mMp4FileDataPtr->videoTrackPtr->currentPos;

#endif /*_M4MP4W_MOOV_FIRST*/

    /*update chunk info before to go for a new one*/

    mMp4FileDataPtr->videoTrackPtr->
        chunkSizeTable[mMp4FileDataPtr->videoTrackPtr->currentChunk] =
        mMp4FileDataPtr->videoTrackPtr->currentPos;
    mMp4FileDataPtr->videoTrackPtr->
        chunkTimeMsTable[mMp4FileDataPtr->videoTrackPtr->currentChunk] =
        (M4OSA_UInt32)(mMp4FileDataPtr->videoTrackPtr->CommonData.lastCTS
        * scale_video);

    mMp4FileDataPtr->videoTrackPtr->currentChunk += 1;
    mMp4FileDataPtr->filesize += 16;

    /*alloc new chunk*/
    /*only if not already allocated*/
    if (mMp4FileDataPtr->videoTrackPtr->currentChunk
        > mMp4FileDataPtr->videoTrackPtr->LastAllocatedChunk)
    {
        /*update LastAllocatedChunk ( -> = currentChunk)*/
        mMp4FileDataPtr->videoTrackPtr->LastAllocatedChunk += 1;

        /*max nb of chunk is now dynamic*/
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

        if ( mMp4FileDataPtr->videoTrackPtr->LastAllocatedChunk
            + 3 > M4MP4W_CHUNK_ALLOC_NB)
        {
            M4OSA_TRACE1_0("M4MP4W_newVideoChunk : video chunk table is full");
            return M4WAR_MP4W_OVERSIZE;
        }

#else

        if (((mMp4FileDataPtr->videoTrackPtr->LastAllocatedChunk)
            % M4MP4W_CHUNK_ALLOC_NB) == 0)
        {
            reallocNb = mMp4FileDataPtr->videoTrackPtr->LastAllocatedChunk
                + M4MP4W_CHUNK_ALLOC_NB;

#ifdef _M4MP4W_MOOV_FIRST

            mMp4FileDataPtr->videoTrackPtr->Chunk =
                (M4OSA_UChar ** )M4MP4W_realloc(
                (M4OSA_MemAddr32)mMp4FileDataPtr->videoTrackPtr->Chunk,
                ( reallocNb
                - M4MP4W_CHUNK_ALLOC_NB) * sizeof(M4OSA_UChar *),
                reallocNb * sizeof(M4OSA_UChar *));
            ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->Chunk != M4OSA_NULL,
                M4ERR_ALLOC);

#else

            mMp4FileDataPtr->videoTrackPtr->chunkOffsetTable =
                (M4OSA_UInt32 *)M4MP4W_realloc(
                (M4OSA_MemAddr32)mMp4FileDataPtr->videoTrackPtr->
                chunkOffsetTable, ( reallocNb - M4MP4W_CHUNK_ALLOC_NB)
                * sizeof(M4OSA_UInt32),
                reallocNb * sizeof(M4OSA_UInt32));
            ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->chunkOffsetTable
                != M4OSA_NULL, M4ERR_ALLOC);

#endif /*_M4MP4W_MOOV_FIRST*/

            mMp4FileDataPtr->videoTrackPtr->chunkSizeTable =
                (M4OSA_UInt32 *)M4MP4W_realloc(
                (M4OSA_MemAddr32)mMp4FileDataPtr->videoTrackPtr->
                chunkSizeTable, ( reallocNb - M4MP4W_CHUNK_ALLOC_NB)
                * sizeof(M4OSA_UInt32),
                reallocNb * sizeof(M4OSA_UInt32));
            ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->chunkSizeTable
                != M4OSA_NULL, M4ERR_ALLOC);

            mMp4FileDataPtr->videoTrackPtr->chunkSampleNbTable =
                (M4OSA_UInt32 *)M4MP4W_realloc(
                (M4OSA_MemAddr32)mMp4FileDataPtr->videoTrackPtr->
                chunkSampleNbTable, ( reallocNb - M4MP4W_CHUNK_ALLOC_NB)
                * sizeof(M4OSA_UInt32),
                reallocNb * sizeof(M4OSA_UInt32));
            ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->chunkSampleNbTable
                != M4OSA_NULL, M4ERR_ALLOC);

            mMp4FileDataPtr->videoTrackPtr->chunkTimeMsTable =
                (M4MP4W_Time32 *)M4MP4W_realloc(
                (M4OSA_MemAddr32)mMp4FileDataPtr->videoTrackPtr->
                chunkTimeMsTable, ( reallocNb
                - M4MP4W_CHUNK_ALLOC_NB) * sizeof(M4MP4W_Time32),
                reallocNb * sizeof(M4MP4W_Time32));
            ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->chunkTimeMsTable
                != M4OSA_NULL, M4ERR_ALLOC);
        }
#endif /*_M4MP4W_OPTIMIZE_FOR_PHONE*/

#ifdef _M4MP4W_MOOV_FIRST

        mMp4FileDataPtr->videoTrackPtr->
            Chunk[mMp4FileDataPtr->videoTrackPtr->currentChunk] = (M4OSA_UChar
            *)M4OSA_32bitAlignedMalloc(mMp4FileDataPtr->videoTrackPtr->MaxChunkSize,
            M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->MaxChunkSize");
        ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->
            Chunk[mMp4FileDataPtr->videoTrackPtr->currentChunk]
        != M4OSA_NULL, M4ERR_ALLOC);

#endif /*_M4MP4W_MOOV_FIRST*/

    }

    /*update leftSpaceInChunk, currentPos and currentChunkDur*/
    *leftSpaceInChunk = mMp4FileDataPtr->videoTrackPtr->MaxChunkSize;
    mMp4FileDataPtr->videoTrackPtr->currentPos = 0;

#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE
    /* check wether to use a new stsc or not */

    if (mMp4FileDataPtr->videoTrackPtr->currentStsc > 0)
    {
        if ((mMp4FileDataPtr->videoTrackPtr->
            chunkSampleNbTable[mMp4FileDataPtr->videoTrackPtr->
            currentStsc] & 0xFFF) != (mMp4FileDataPtr->videoTrackPtr->
            chunkSampleNbTable[mMp4FileDataPtr->videoTrackPtr->currentStsc
            - 1] & 0xFFF))
            mMp4FileDataPtr->videoTrackPtr->currentStsc += 1;
    }
    else
        mMp4FileDataPtr->videoTrackPtr->currentStsc += 1;

    /* max nb of chunk is now dynamic */
    if (mMp4FileDataPtr->videoTrackPtr->currentStsc
        + 3 > M4MP4W_CHUNK_ALLOC_NB)
    {
        M4OSA_TRACE1_0("M4MP4W_newVideoChunk : video stsc table is full");
        return M4WAR_MP4W_OVERSIZE;
    }

    /* set nb of samples in the new chunk to 0 */
    mMp4FileDataPtr->videoTrackPtr->
        chunkSampleNbTable[mMp4FileDataPtr->videoTrackPtr->currentStsc] =
        0 + (mMp4FileDataPtr->videoTrackPtr->currentChunk << 12);

#else
    /*set nb of samples in the new chunk to 0*/

    mMp4FileDataPtr->videoTrackPtr->
        chunkSampleNbTable[mMp4FileDataPtr->videoTrackPtr->currentChunk] = 0;

#endif

    /*set time of the new chunk to lastCTS (for initialization, but updated further to the
    CTS of the last sample in the chunk)*/

    mMp4FileDataPtr->videoTrackPtr->
        chunkTimeMsTable[mMp4FileDataPtr->videoTrackPtr->currentChunk] =
        (M4OSA_UInt32)(mMp4FileDataPtr->videoTrackPtr->CommonData.lastCTS
        * scale_video);

    return err;
}

/*******************************************************************************/
M4OSA_ERR M4MP4W_startAU( M4OSA_Context context, M4SYS_StreamID streamID,
                         M4SYS_AccessUnit *auPtr )
/*******************************************************************************/
{
    M4OSA_ERR err = M4NO_ERROR;

    M4MP4W_Mp4FileData *mMp4FileDataPtr = (M4MP4W_Mp4FileData *)context;

    M4OSA_UInt32 leftSpaceInChunk;
    M4MP4W_Time32 chunkDurMs;

    M4OSA_Double scale_audio;
    M4OSA_Double scale_video;

    ERR_CHECK(context != M4OSA_NULL, M4ERR_PARAMETER);
    ERR_CHECK(auPtr != M4OSA_NULL, M4ERR_PARAMETER);

    M4OSA_TRACE2_0("----- M4MP4W_startAU -----");

    /*check macro state*/
    ERR_CHECK((mMp4FileDataPtr->state == M4MP4W_writing), M4ERR_STATE);

    if (streamID == AudioStreamID) /*audio stream*/
    {
        M4OSA_TRACE2_0("M4MP4W_startAU -> audio");

        scale_audio =
            1000.0 / mMp4FileDataPtr->audioTrackPtr->CommonData.timescale;

        /*audio microstate*/
        ERR_CHECK((mMp4FileDataPtr->audioTrackPtr->microState
            == M4MP4W_writing), M4ERR_STATE);
        mMp4FileDataPtr->audioTrackPtr->microState = M4MP4W_writing_startAU;

        leftSpaceInChunk = mMp4FileDataPtr->audioTrackPtr->MaxChunkSize
            - mMp4FileDataPtr->audioTrackPtr->currentPos;

        M4OSA_TRACE2_2("audio %d  %d",
            mMp4FileDataPtr->audioTrackPtr->currentPos, leftSpaceInChunk);

        chunkDurMs =
            (M4OSA_UInt32)(( mMp4FileDataPtr->audioTrackPtr->CommonData.lastCTS
            * scale_audio) - mMp4FileDataPtr->audioTrackPtr->
            chunkTimeMsTable[mMp4FileDataPtr->audioTrackPtr->
            currentChunk]);

        if ((leftSpaceInChunk < mMp4FileDataPtr->audioTrackPtr->MaxAUSize)
            || (( mMp4FileDataPtr->InterleaveDur != 0)
            && (chunkDurMs >= mMp4FileDataPtr->InterleaveDur)))
        {
#ifdef _M4MP4W_UNBUFFERED_VIDEO
            /* only if there is at least 1 video sample in the chunk */

            if ((mMp4FileDataPtr->videoTrackPtr != M4OSA_NULL)
                && (mMp4FileDataPtr->videoTrackPtr->currentPos > 0))
            {
                /* close the opened video chunk before creating a new audio one */
                err = M4MP4W_newVideoChunk(context, &leftSpaceInChunk);

                if (err != M4NO_ERROR)
                    return err;
            }

#endif
            /* not enough space in current chunk: create a new one */

            err = M4MP4W_newAudioChunk(context, &leftSpaceInChunk);

            if (err != M4NO_ERROR)
                return err;
        }

        auPtr->size = leftSpaceInChunk;

#ifdef _M4MP4W_MOOV_FIRST

        auPtr->dataAddress = (M4OSA_MemAddr32)(mMp4FileDataPtr->audioTrackPtr->
            Chunk[mMp4FileDataPtr->audioTrackPtr->currentChunk]
        + mMp4FileDataPtr->audioTrackPtr->currentPos);

#else

        auPtr->dataAddress =
            (M4OSA_MemAddr32)(mMp4FileDataPtr->audioTrackPtr->Chunk[0]
        + mMp4FileDataPtr->audioTrackPtr->currentPos);

#endif                                   /*_M4MP4W_MOOV_FIRST*/

    }
    else if (streamID == VideoStreamID) /*video stream*/
    {
        M4OSA_TRACE2_0("M4MP4W_startAU -> video");

        scale_video =
            1000.0 / mMp4FileDataPtr->videoTrackPtr->CommonData.timescale;

        /*video microstate*/
        ERR_CHECK((mMp4FileDataPtr->videoTrackPtr->microState
            == M4MP4W_writing), M4ERR_STATE);
        mMp4FileDataPtr->videoTrackPtr->microState = M4MP4W_writing_startAU;

        leftSpaceInChunk = mMp4FileDataPtr->videoTrackPtr->MaxChunkSize
            - mMp4FileDataPtr->videoTrackPtr->currentPos;

        chunkDurMs =
            (M4OSA_UInt32)(( mMp4FileDataPtr->videoTrackPtr->CommonData.lastCTS
            * scale_video) - mMp4FileDataPtr->videoTrackPtr->
            chunkTimeMsTable[mMp4FileDataPtr->videoTrackPtr->
            currentChunk]);

#ifdef _M4MP4W_UNBUFFERED_VIDEO

        leftSpaceInChunk = mMp4FileDataPtr->videoTrackPtr->MaxChunkSize;

#endif

        M4OSA_TRACE2_2("video %d  %d",
            mMp4FileDataPtr->videoTrackPtr->currentPos, leftSpaceInChunk);

        if (( leftSpaceInChunk < mMp4FileDataPtr->videoTrackPtr->MaxAUSize)
            || (( mMp4FileDataPtr->InterleaveDur != 0)
            && (chunkDurMs >= mMp4FileDataPtr->InterleaveDur))
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

            || (( mMp4FileDataPtr->videoTrackPtr->MaxAUperChunk != 0)
            && (( mMp4FileDataPtr->videoTrackPtr->
            chunkSampleNbTable[mMp4FileDataPtr->videoTrackPtr->
            currentStsc] & 0xFFF)
            == mMp4FileDataPtr->videoTrackPtr->MaxAUperChunk))

#endif

            )
        {
            /*not enough space in current chunk: create a new one*/
            err = M4MP4W_newVideoChunk(context, &leftSpaceInChunk);

            if (err != M4NO_ERROR)
                return err;
        }

        M4OSA_TRACE2_3("startAU: size 0x%x pos 0x%x chunk %u", auPtr->size,
            mMp4FileDataPtr->videoTrackPtr->currentPos,
            mMp4FileDataPtr->videoTrackPtr->currentChunk);

        M4OSA_TRACE3_1("adr = 0x%p", auPtr->dataAddress);

        if (auPtr->dataAddress)
        {
            M4OSA_TRACE3_3(" data = %08X %08X %08X", auPtr->dataAddress[0],
                auPtr->dataAddress[1], auPtr->dataAddress[2]);
        }

        auPtr->size = leftSpaceInChunk;
#ifdef _M4MP4W_MOOV_FIRST

        if (mMp4FileDataPtr->videoTrackPtr->CommonData.trackType
            == M4SYS_kH264)
            auPtr->dataAddress =
            (M4OSA_MemAddr32)(mMp4FileDataPtr->videoTrackPtr->
            Chunk[mMp4FileDataPtr->videoTrackPtr->currentChunk]
        + mMp4FileDataPtr->videoTrackPtr->currentPos + 4);
        else
            auPtr->dataAddress =
            (M4OSA_MemAddr32)(mMp4FileDataPtr->videoTrackPtr->
            Chunk[mMp4FileDataPtr->videoTrackPtr->currentChunk]
        + mMp4FileDataPtr->videoTrackPtr->currentPos);

#else
#ifdef _M4MP4W_UNBUFFERED_VIDEO

        if (mMp4FileDataPtr->videoTrackPtr->CommonData.trackType
            == M4SYS_kH264)
            auPtr->dataAddress =
            (M4OSA_MemAddr32)(mMp4FileDataPtr->videoTrackPtr->Chunk[0] + 4);
        else
            auPtr->dataAddress =
            (M4OSA_MemAddr32)(mMp4FileDataPtr->videoTrackPtr->Chunk[0]);

#else

        if (mMp4FileDataPtr->videoTrackPtr->CommonData.trackType
            == M4SYS_kH264)
            auPtr->dataAddress =
            (M4OSA_MemAddr32)(mMp4FileDataPtr->videoTrackPtr->Chunk[0]
        + mMp4FileDataPtr->videoTrackPtr->currentPos
            + 4); /* In H264, we must start by the length of the NALU, coded in 4 bytes */
        else
            auPtr->dataAddress =
            (M4OSA_MemAddr32)(mMp4FileDataPtr->videoTrackPtr->Chunk[0]
        + mMp4FileDataPtr->videoTrackPtr->currentPos);

#endif /*_M4MP4W_UNBUFFERED_VIDEO*/

#endif /*_M4MP4W_MOOV_FIRST*/

    }
    else
        return M4ERR_BAD_STREAM_ID;

    M4OSA_TRACE1_3("M4MPW_startAU: start address:%p, size:%lu, stream:%d",
        auPtr->dataAddress, auPtr->size, streamID);

    return err;
}

/*******************************************************************************/
M4OSA_ERR M4MP4W_processAU( M4OSA_Context context, M4SYS_StreamID streamID,
                           M4SYS_AccessUnit *auPtr )
/*******************************************************************************/
{
    M4OSA_ERR err = M4NO_ERROR;
    M4MP4W_Time32 delta;
    M4MP4W_Time32 lastSampleDur;
    M4OSA_UInt32 i;
    /*expectedSize is the max filesize to forecast when adding a new AU:*/
    M4OSA_UInt32 expectedSize =
        32; /*initialized with an estimation of the max metadata space needed for an AU.*/
    M4OSA_Double scale_audio = 0.0;
    M4OSA_Double scale_video = 0.0;

    M4MP4W_Mp4FileData *mMp4FileDataPtr = (M4MP4W_Mp4FileData *)context;
    ERR_CHECK(context != M4OSA_NULL, M4ERR_PARAMETER);

    /*check macro state*/
    ERR_CHECK((mMp4FileDataPtr->state == M4MP4W_writing), M4ERR_STATE);

    M4OSA_TRACE2_0("M4MP4W_processAU");

    if (streamID == AudioStreamID)
        scale_audio =
        1000.0 / mMp4FileDataPtr->audioTrackPtr->CommonData.timescale;

    if (streamID == VideoStreamID)
        scale_video =
        1000.0 / mMp4FileDataPtr->videoTrackPtr->CommonData.timescale;

    /* PL 27/10/2008: after the resurgence of the AAC 128 bug, I added a debug check that
     the encoded data didn't overflow the available space in the AU */

    switch( streamID )
    {
        case AudioStreamID:
            M4OSA_DEBUG_IF1(auPtr->size
                + mMp4FileDataPtr->audioTrackPtr->currentPos
            > mMp4FileDataPtr->audioTrackPtr->MaxChunkSize,
            M4ERR_CONTEXT_FAILED,
            "Uh oh. Buffer overflow in the writer. Abandon ship!");
            M4OSA_DEBUG_IF2(auPtr->size
                > mMp4FileDataPtr->audioTrackPtr->MaxAUSize,
                M4ERR_CONTEXT_FAILED,
                "Oops. An AU went over the declared Max AU size.\
                 You might wish to investigate that.");
            break;

        case VideoStreamID:
            M4OSA_DEBUG_IF1(auPtr->size
                + mMp4FileDataPtr->videoTrackPtr->currentPos
                    > mMp4FileDataPtr->videoTrackPtr->MaxChunkSize,
                    M4ERR_CONTEXT_FAILED,
                    "Uh oh. Buffer overflow in the writer. Abandon ship!");
            M4OSA_DEBUG_IF2(auPtr->size
                    > mMp4FileDataPtr->videoTrackPtr->MaxAUSize,
                    M4ERR_CONTEXT_FAILED,
                    "Oops. An AU went over the declared Max AU size.\
                     You might wish to investigate that.");
            break;
    }

    /*only if not in the case audio with estimateAudioSize
    (else, size already estimated at this point)*/
    if ((mMp4FileDataPtr->estimateAudioSize == M4OSA_FALSE)
        || (streamID == VideoStreamID))
    {
        /*check filesize if needed*/
        if (mMp4FileDataPtr->MaxFileSize != 0)
        {
            expectedSize += mMp4FileDataPtr->filesize + auPtr->size;

            if ((streamID == VideoStreamID)
                && (mMp4FileDataPtr->videoTrackPtr->CommonData.trackType
                == M4SYS_kH264))
            {
                expectedSize += 4;
            }

            if (expectedSize > mMp4FileDataPtr->MaxFileSize)
            {
                M4OSA_TRACE1_0("processAU : !! FILESIZE EXCEEDED !!");

                /* patch for autostop is MaxFileSize exceeded */
                M4OSA_TRACE1_0("M4MP4W_processAU : stop at targeted filesize");
                return M4WAR_MP4W_OVERSIZE;
            }
        }
    }

    /*case audioMsStopTime has already been set during video processing,
     and now check it for audio*/
    if ((mMp4FileDataPtr->estimateAudioSize == M4OSA_TRUE)
        && (streamID == AudioStreamID))
    {
        if (mMp4FileDataPtr->audioMsStopTime <= (auPtr->CTS *scale_audio))
        {
            /* bugfix: if a new chunk was just created, cancel it before to close */
            if ((mMp4FileDataPtr->audioTrackPtr->currentChunk != 0)
                && (mMp4FileDataPtr->audioTrackPtr->currentPos == 0))
            {
                mMp4FileDataPtr->audioTrackPtr->currentChunk--;
            }
            M4OSA_TRACE1_0("M4MP4W_processAU : audio stop time reached");
            return M4WAR_MP4W_OVERSIZE;
        }
    }

    if (streamID == AudioStreamID) /*audio stream*/
    {
        M4OSA_TRACE2_0("M4MP4W_processAU -> audio");

        /*audio microstate*/
        ERR_CHECK((mMp4FileDataPtr->audioTrackPtr->microState
            == M4MP4W_writing_startAU), M4ERR_STATE);
        mMp4FileDataPtr->audioTrackPtr->microState = M4MP4W_writing;

        mMp4FileDataPtr->audioTrackPtr->currentPos += auPtr->size;
        /* Warning: time conversion cast 64to32! */
        delta = (M4MP4W_Time32)auPtr->CTS
            - mMp4FileDataPtr->audioTrackPtr->CommonData.lastCTS;

        /* DEBUG stts entries which are equal to 0 */
        M4OSA_TRACE2_1("A_DELTA = %ld\n", delta);

        if (mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb
            == 0) /*test if first AU*/
        {
            /*set au size*/
            mMp4FileDataPtr->audioTrackPtr->CommonData.sampleSize = auPtr->size;

            /*sample duration is a priori constant in audio case, */
            /*but if an Au at least has different size, a stsz table will be created */

            /*mMp4FileDataPtr->audioTrackPtr->sampleDuration = delta; */
            /*TODO test sample duration? (should be 20ms in AMR8, 160 tics with timescale 8000) */
        }
        else
        {
            /*check if au size is constant (audio) */
            /*0 sample size means non constant size*/
            if (mMp4FileDataPtr->audioTrackPtr->CommonData.sampleSize != 0)
            {
                if (mMp4FileDataPtr->audioTrackPtr->CommonData.sampleSize
                    != auPtr->size)
                {
                    /*first AU with different size => non constant size => STSZ table needed*/
                    /*computation of the nb of block of size M4MP4W_STSZ_ALLOC_SIZE to allocate*/
                    mMp4FileDataPtr->audioTrackPtr->nbOfAllocatedStszBlocks =
                        1 + mMp4FileDataPtr->audioTrackPtr->
                        CommonData.sampleNb
                        * 4 / M4MP4W_STSZ_AUDIO_ALLOC_SIZE;
                    mMp4FileDataPtr->audioTrackPtr->TABLE_STSZ =
                        (M4OSA_UInt32 *)M4OSA_32bitAlignedMalloc(
                        mMp4FileDataPtr->audioTrackPtr->
                        nbOfAllocatedStszBlocks
                        * M4MP4W_STSZ_AUDIO_ALLOC_SIZE,
                        M4MP4_WRITER, (M4OSA_Char *)"audioTrackPtr->TABLE_STSZ");
                    ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->TABLE_STSZ
                        != M4OSA_NULL, M4ERR_ALLOC);

                    for ( i = 0;
                        i < mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb;
                        i++ )
                    {
                        mMp4FileDataPtr->audioTrackPtr->TABLE_STSZ[i] =
                            mMp4FileDataPtr->audioTrackPtr->
                            CommonData.sampleSize;
                    }
                    mMp4FileDataPtr->audioTrackPtr->
                        TABLE_STSZ[mMp4FileDataPtr->audioTrackPtr->
                        CommonData.sampleNb] = auPtr->size;
                    mMp4FileDataPtr->audioTrackPtr->CommonData.sampleSize =
                        0; /*used as a flag in that case*/
                    /*more bytes in the file in that case:*/
                    if (mMp4FileDataPtr->estimateAudioSize == M4OSA_FALSE)
                        mMp4FileDataPtr->filesize +=
                        4 * mMp4FileDataPtr->audioTrackPtr->
                        CommonData.sampleNb;
                }
            }
            /*else table already exists*/
            else
            {
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

                if (4 *(mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb + 3)
                    >= mMp4FileDataPtr->audioTrackPtr->nbOfAllocatedStszBlocks
                    *M4MP4W_STSZ_AUDIO_ALLOC_SIZE)
                {
                    M4OSA_TRACE1_0(
                        "M4MP4W_processAU : audio stsz table is full");
                    return M4WAR_MP4W_OVERSIZE;
                }

#else

                if (4 *mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb
                    >= mMp4FileDataPtr->audioTrackPtr->nbOfAllocatedStszBlocks
                    *M4MP4W_STSZ_AUDIO_ALLOC_SIZE)
                {
                    mMp4FileDataPtr->audioTrackPtr->nbOfAllocatedStszBlocks +=
                        1;
                    mMp4FileDataPtr->audioTrackPtr->TABLE_STSZ =
                        (M4OSA_UInt32 *)M4MP4W_realloc(
                        (M4OSA_MemAddr32)mMp4FileDataPtr->audioTrackPtr->
                        TABLE_STSZ, ( mMp4FileDataPtr->audioTrackPtr->
                        nbOfAllocatedStszBlocks - 1)
                        * M4MP4W_STSZ_AUDIO_ALLOC_SIZE,
                        mMp4FileDataPtr->audioTrackPtr->
                        nbOfAllocatedStszBlocks
                        * M4MP4W_STSZ_AUDIO_ALLOC_SIZE);
                    ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->TABLE_STSZ
                        != M4OSA_NULL, M4ERR_ALLOC);
                }

#endif /*_M4MP4W_OPTIMIZE_FOR_PHONE*/

                mMp4FileDataPtr->audioTrackPtr->
                    TABLE_STSZ[mMp4FileDataPtr->audioTrackPtr->
                    CommonData.sampleNb] = auPtr->size;

                if (mMp4FileDataPtr->estimateAudioSize == M4OSA_FALSE)
                    mMp4FileDataPtr->filesize += 4;
            }
        }

        if (delta > mMp4FileDataPtr->audioTrackPtr->sampleDuration)
        {
            /* keep track of real sample duration*/
            mMp4FileDataPtr->audioTrackPtr->sampleDuration = delta;
        }

        if (mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb
            == 0) /*test if first AU*/
        {
            mMp4FileDataPtr->audioTrackPtr->TABLE_STTS[0] = 1;
            mMp4FileDataPtr->audioTrackPtr->TABLE_STTS[1] = 0;
            mMp4FileDataPtr->audioTrackPtr->CommonData.sttsTableEntryNb = 1;
            mMp4FileDataPtr->filesize += 8;
        }
        else if (mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb
            == 1) /*test if second AU*/
        {
#ifndef DUPLICATE_STTS_IN_LAST_AU

            mMp4FileDataPtr->audioTrackPtr->TABLE_STTS[0] += 1;

#endif /*DUPLICATE_STTS_IN_LAST_AU*/

            mMp4FileDataPtr->audioTrackPtr->TABLE_STTS[1] = delta;
            mMp4FileDataPtr->audioTrackPtr->CommonData.sttsTableEntryNb += 1;
            mMp4FileDataPtr->filesize += 8;
        }
        else
        {
            /*retrieve last sample delta*/
            lastSampleDur = mMp4FileDataPtr->audioTrackPtr->TABLE_STTS[2
                * (mMp4FileDataPtr->audioTrackPtr->
                CommonData.sttsTableEntryNb - 1) - 1];

#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

            if (8 *(mMp4FileDataPtr->audioTrackPtr->CommonData.sttsTableEntryNb
                + 3) >= mMp4FileDataPtr->audioTrackPtr->nbOfAllocatedSttsBlocks
                *M4MP4W_STTS_AUDIO_ALLOC_SIZE)
            {
                M4OSA_TRACE1_0("M4MP4W_processAU : audio stts table is full");
                return M4WAR_MP4W_OVERSIZE;
            }

#else

            if (8 *mMp4FileDataPtr->audioTrackPtr->CommonData.sttsTableEntryNb
                >= mMp4FileDataPtr->audioTrackPtr->nbOfAllocatedSttsBlocks
                *M4MP4W_STTS_AUDIO_ALLOC_SIZE)
            {
                mMp4FileDataPtr->audioTrackPtr->nbOfAllocatedSttsBlocks += 1;
                mMp4FileDataPtr->audioTrackPtr->TABLE_STTS =
                    (M4OSA_UInt32 *)M4MP4W_realloc(
                    (M4OSA_MemAddr32)mMp4FileDataPtr->audioTrackPtr->
                    TABLE_STTS, ( mMp4FileDataPtr->audioTrackPtr->
                    nbOfAllocatedSttsBlocks
                    - 1) * M4MP4W_STTS_AUDIO_ALLOC_SIZE,
                    mMp4FileDataPtr->audioTrackPtr->
                    nbOfAllocatedSttsBlocks
                    * M4MP4W_STTS_AUDIO_ALLOC_SIZE);
                ERR_CHECK(mMp4FileDataPtr->audioTrackPtr->TABLE_STTS
                    != M4OSA_NULL, M4ERR_ALLOC);
            }

#endif                                   /*_M4MP4W_OPTIMIZE_FOR_PHONE*/

            if (delta != lastSampleDur) /*new entry in the table*/
            {
                mMp4FileDataPtr->audioTrackPtr->TABLE_STTS[2 *(
                    mMp4FileDataPtr->audioTrackPtr->
                    CommonData.sttsTableEntryNb - 1)] = 1;
                mMp4FileDataPtr->audioTrackPtr->TABLE_STTS[2 *(
                    mMp4FileDataPtr->audioTrackPtr->
                    CommonData.sttsTableEntryNb - 1) + 1] = delta;
                mMp4FileDataPtr->audioTrackPtr->CommonData.sttsTableEntryNb +=
                    1;
                mMp4FileDataPtr->filesize += 8;
            }
            else
            {
                /*increase of 1 the number of consecutive AUs with same duration*/
                mMp4FileDataPtr->audioTrackPtr->TABLE_STTS[2 *(
                    mMp4FileDataPtr->audioTrackPtr->
                    CommonData.sttsTableEntryNb - 1) - 2] += 1;
            }
        }
        mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb += 1;
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

        mMp4FileDataPtr->audioTrackPtr->
            chunkSampleNbTable[mMp4FileDataPtr->audioTrackPtr->currentStsc] +=
            1;

#else

        mMp4FileDataPtr->audioTrackPtr->
            chunkSampleNbTable[mMp4FileDataPtr->audioTrackPtr->currentChunk] +=
            1;

#endif
        /* Warning: time conversion cast 64to32! */

        mMp4FileDataPtr->audioTrackPtr->CommonData.lastCTS =
            (M4MP4W_Time32)auPtr->CTS;
    }
    else if (streamID == VideoStreamID) /*video stream*/
    {
        M4OSA_TRACE2_0("M4MP4W_processAU -> video");

        /* In h264, the size of the AU must be added to the data */
        if (mMp4FileDataPtr->videoTrackPtr->CommonData.trackType
            == M4SYS_kH264)
        {
            /* Add the size of the NALU in BE */
            M4OSA_MemAddr8 pTmpDataAddress = M4OSA_NULL;
            auPtr->dataAddress -= 1;
            pTmpDataAddress = (M4OSA_MemAddr8)auPtr->dataAddress;

            // bit manipulation
            *pTmpDataAddress++ = (M4OSA_UInt8)((auPtr->size >> 24) & 0x000000FF);
            *pTmpDataAddress++ = (M4OSA_UInt8)((auPtr->size >> 16) & 0x000000FF);
            *pTmpDataAddress++ = (M4OSA_UInt8)((auPtr->size >> 8)  & 0x000000FF);
            *pTmpDataAddress++ = (M4OSA_UInt8)((auPtr->size)       & 0x000000FF);

            auPtr->size += 4;
        }

        /*video microstate*/
        ERR_CHECK((mMp4FileDataPtr->videoTrackPtr->microState
            == M4MP4W_writing_startAU), M4ERR_STATE);
        mMp4FileDataPtr->videoTrackPtr->microState = M4MP4W_writing;

#ifdef _M4MP4W_UNBUFFERED_VIDEO
        /* samples are written to file now */

        err = M4MP4W_putBlock((M4OSA_UChar *)auPtr->dataAddress, auPtr->size,
            mMp4FileDataPtr->fileWriterFunctions,
            mMp4FileDataPtr->fileWriterContext);

        if (err != M4NO_ERROR)
        {
            M4OSA_FilePosition temp = mMp4FileDataPtr->absoluteCurrentPos
                + mMp4FileDataPtr->videoTrackPtr->currentPos;
            M4OSA_TRACE2_1(
                "M4MP4W_processAU: putBlock error when writing unbuffered video sample: %#X",
                err);
            /* Ouch, we got an error writing to the file, but we need to properly react so that
             the state is still consistent and we can properly close the file so that what has
              been recorded so far is not lost. Yay error recovery! */

            /* First, we do not know where we are in the file. Put us back at where we were before
            attempting to write the data. That way, we're consistent with the chunk and sample
             state data.absoluteCurrentPos is only updated for chunks, it points to the beginning
             of the chunk,therefore we need to add videoTrackPtr->currentPos to know where we
             were in the file. */
            err = mMp4FileDataPtr->fileWriterFunctions->seek(
                mMp4FileDataPtr->fileWriterContext,
                M4OSA_kFileSeekBeginning, &temp);

            M4OSA_TRACE2_3(
                "Backtracking to position 0x%08X, seek returned %d and position %08X",
                mMp4FileDataPtr->absoluteCurrentPos
                + mMp4FileDataPtr->videoTrackPtr->currentPos, err, temp);

            /* Then, do not update any info whatsoever in the writing state. This will have the
             consequence that it will be as if the sample has never been written, so the chunk
             will be merely closed after the previous sample (the sample we attempted to write
             here is lost). */

            /* And lastly (for here), return that we've reached the limit of available space.
             We don't care about the error originally returned by putBlock. */

            return M4WAR_MP4W_OVERSIZE;
        }

#endif

        if ((M4MP4W_Time32)auPtr->CTS < mMp4FileDataPtr->videoTrackPtr->CommonData.lastCTS) {
            // Do not report as error, it will abort the entire filewrite. Just skip this frame.
            M4OSA_TRACE1_0("Skip frame. Video frame has too old timestamp.");
            return M4NO_ERROR;
        }

        mMp4FileDataPtr->videoTrackPtr->currentPos += auPtr->size;

        /* Warning: time conversion cast 64to32! */
        delta = (M4MP4W_Time32)auPtr->CTS
            - mMp4FileDataPtr->videoTrackPtr->CommonData.lastCTS;

        /* DEBUG stts entries which are equal to 0 */
        M4OSA_TRACE2_1("V_DELTA = %ld\n", delta);

#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

        if (2 *(mMp4FileDataPtr->videoTrackPtr->CommonData.sampleNb + 3)
            >= mMp4FileDataPtr->videoTrackPtr->nbOfAllocatedStszBlocks
            *M4MP4W_STSZ_ALLOC_SIZE)
        {
            M4OSA_TRACE1_0("M4MP4W_processAU : video stsz table is full");
            return M4WAR_MP4W_OVERSIZE;
        }

        mMp4FileDataPtr->videoTrackPtr->
            TABLE_STSZ[mMp4FileDataPtr->videoTrackPtr->CommonData.sampleNb] =
            (M4OSA_UInt16)auPtr->size;
        mMp4FileDataPtr->filesize += 4;

#else

        if (4 *mMp4FileDataPtr->videoTrackPtr->CommonData.sampleNb
            >= mMp4FileDataPtr->videoTrackPtr->nbOfAllocatedStszBlocks
            *M4MP4W_STSZ_ALLOC_SIZE)
        {
            mMp4FileDataPtr->videoTrackPtr->nbOfAllocatedStszBlocks += 1;

            mMp4FileDataPtr->videoTrackPtr->TABLE_STSZ =
                (M4OSA_UInt32 *)M4MP4W_realloc(
                (M4OSA_MemAddr32)mMp4FileDataPtr->videoTrackPtr->TABLE_STSZ,
                ( mMp4FileDataPtr->videoTrackPtr->
                nbOfAllocatedStszBlocks
                - 1) * M4MP4W_STSZ_ALLOC_SIZE,
                mMp4FileDataPtr->videoTrackPtr->nbOfAllocatedStszBlocks
                * M4MP4W_STSZ_ALLOC_SIZE);

            ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->TABLE_STSZ != M4OSA_NULL,
                M4ERR_ALLOC);
        }

        mMp4FileDataPtr->videoTrackPtr->
            TABLE_STSZ[mMp4FileDataPtr->videoTrackPtr->CommonData.sampleNb] =
            auPtr->size;
        mMp4FileDataPtr->filesize += 4;

#endif

        if (mMp4FileDataPtr->videoTrackPtr->CommonData.sampleNb
            == 0) /*test if first AU*/
        {
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

            M4MP4W_put32_Lo(&mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[0], 1);
            M4MP4W_put32_Hi(&mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[0], 0);

#else

            mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[0] = 1;
            mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[1] = 0;

#endif

            mMp4FileDataPtr->videoTrackPtr->CommonData.sttsTableEntryNb = 1;
            mMp4FileDataPtr->filesize += 8;
        }
        else if (mMp4FileDataPtr->videoTrackPtr->CommonData.sampleNb
            == 1 ) /*test if second AU*/
        {
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

            M4MP4W_put32_Hi(&mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[0],
                (M4OSA_UInt16)delta);

#else

            mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[1] = delta;

#endif

        }
        else
        {
            /*retrieve last sample delta*/
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

            lastSampleDur = M4MP4W_get32_Hi(&mMp4FileDataPtr->videoTrackPtr->
                TABLE_STTS[mMp4FileDataPtr->videoTrackPtr->
                CommonData.sttsTableEntryNb - 1]);

            if (4 *(mMp4FileDataPtr->videoTrackPtr->CommonData.sttsTableEntryNb
                + 3) >= mMp4FileDataPtr->videoTrackPtr->nbOfAllocatedSttsBlocks
                *M4MP4W_STTS_ALLOC_SIZE)
            {
                M4OSA_TRACE1_0("M4MP4W_processAU : video stts table is full");
                return M4WAR_MP4W_OVERSIZE;
            }

#else

            lastSampleDur = mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[2
                * (mMp4FileDataPtr->videoTrackPtr->
                CommonData.sttsTableEntryNb - 1) + 1];

            if (8 *mMp4FileDataPtr->videoTrackPtr->CommonData.sttsTableEntryNb
                >= mMp4FileDataPtr->videoTrackPtr->nbOfAllocatedSttsBlocks
                *M4MP4W_STTS_ALLOC_SIZE)
            {
                mMp4FileDataPtr->videoTrackPtr->nbOfAllocatedSttsBlocks += 1;
                mMp4FileDataPtr->videoTrackPtr->TABLE_STTS =
                    (M4OSA_UInt32 *)M4MP4W_realloc(
                    (M4OSA_MemAddr32)mMp4FileDataPtr->videoTrackPtr->
                    TABLE_STTS, ( mMp4FileDataPtr->videoTrackPtr->
                    nbOfAllocatedSttsBlocks
                    - 1) * M4MP4W_STTS_ALLOC_SIZE,
                    mMp4FileDataPtr->videoTrackPtr->
                    nbOfAllocatedSttsBlocks
                    * M4MP4W_STTS_ALLOC_SIZE);
                ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->TABLE_STTS
                    != M4OSA_NULL, M4ERR_ALLOC);
            }

#endif                                   /*_M4MP4W_OPTIMIZE_FOR_PHONE*/

            if (delta != lastSampleDur) /*new entry in the table*/
            {
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

                M4MP4W_put32_Lo(&mMp4FileDataPtr->videoTrackPtr->
                    TABLE_STTS[mMp4FileDataPtr->videoTrackPtr->
                    CommonData.sttsTableEntryNb], 1);
                M4MP4W_put32_Hi(&mMp4FileDataPtr->videoTrackPtr->
                    TABLE_STTS[mMp4FileDataPtr->videoTrackPtr->
                    CommonData.sttsTableEntryNb], (M4OSA_UInt16)delta);

#else

                mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[2 *(
                    mMp4FileDataPtr->videoTrackPtr->
                    CommonData.sttsTableEntryNb)] = 1;
                mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[2
                    *(mMp4FileDataPtr->videoTrackPtr->
                    CommonData.sttsTableEntryNb)+1] = delta;

#endif

                mMp4FileDataPtr->videoTrackPtr->CommonData.sttsTableEntryNb +=
                    1;
                mMp4FileDataPtr->filesize += 8;
            }
            else
            {
                /*increase of 1 the number of consecutive AUs with same duration*/
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

                mMp4FileDataPtr->videoTrackPtr->
                    TABLE_STTS[mMp4FileDataPtr->videoTrackPtr->
                    CommonData.sttsTableEntryNb - 1] += 1;

#else

                mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[2 *(
                    mMp4FileDataPtr->videoTrackPtr->
                    CommonData.sttsTableEntryNb - 1)] += 1;

#endif

            }
        }

        mMp4FileDataPtr->videoTrackPtr->CommonData.sampleNb += 1;
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

        mMp4FileDataPtr->videoTrackPtr->
            chunkSampleNbTable[mMp4FileDataPtr->videoTrackPtr->currentStsc] +=
            1;

#else

        mMp4FileDataPtr->videoTrackPtr->
            chunkSampleNbTable[mMp4FileDataPtr->videoTrackPtr->currentChunk] +=
            1;

#endif

        if (auPtr->attribute == AU_RAP)
        {
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

            if (4 *(mMp4FileDataPtr->videoTrackPtr->stssTableEntryNb + 3)
                >= mMp4FileDataPtr->videoTrackPtr->nbOfAllocatedStssBlocks
                *M4MP4W_STSS_ALLOC_SIZE)
            {
                M4OSA_TRACE1_0("M4MP4W_processAU : video stss table is full");
                return M4WAR_MP4W_OVERSIZE;
            }

#else

            if (4 *mMp4FileDataPtr->videoTrackPtr->stssTableEntryNb
                >= mMp4FileDataPtr->videoTrackPtr->nbOfAllocatedStssBlocks
                *M4MP4W_STSS_ALLOC_SIZE)
            {
                mMp4FileDataPtr->videoTrackPtr->nbOfAllocatedStssBlocks += 1;
                mMp4FileDataPtr->videoTrackPtr->TABLE_STSS =
                    (M4OSA_UInt32 *)M4MP4W_realloc(
                    (M4OSA_MemAddr32)mMp4FileDataPtr->videoTrackPtr->
                    TABLE_STSS, ( mMp4FileDataPtr->videoTrackPtr->
                    nbOfAllocatedStssBlocks
                    - 1) * M4MP4W_STSS_ALLOC_SIZE,
                    mMp4FileDataPtr->videoTrackPtr->
                    nbOfAllocatedStssBlocks
                    * M4MP4W_STSS_ALLOC_SIZE);
                ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->TABLE_STSS
                    != M4OSA_NULL, M4ERR_ALLOC);
            }

#endif /*_M4MP4W_OPTIMIZE_FOR_PHONE*/

            mMp4FileDataPtr->videoTrackPtr->
                TABLE_STSS[mMp4FileDataPtr->videoTrackPtr->stssTableEntryNb] =
                mMp4FileDataPtr->videoTrackPtr->CommonData.sampleNb;
            mMp4FileDataPtr->videoTrackPtr->stssTableEntryNb += 1;
            mMp4FileDataPtr->filesize += 4;
        }

        /* Warning: time conversion cast 64to32! */
        mMp4FileDataPtr->videoTrackPtr->CommonData.lastCTS =
            (M4MP4W_Time32)auPtr->CTS;
    }
    else
        return M4ERR_BAD_STREAM_ID;

    /* I moved some state modification to after we know the sample has been written correctly. */
    if ((mMp4FileDataPtr->estimateAudioSize == M4OSA_TRUE)
        && (streamID == VideoStreamID))
    {
        mMp4FileDataPtr->audioMsStopTime =
            (M4MP4W_Time32)(auPtr->CTS * scale_video);
    }

    if ((mMp4FileDataPtr->estimateAudioSize == M4OSA_FALSE)
        || (streamID == VideoStreamID))
    {
        /*update fileSize*/
        mMp4FileDataPtr->filesize += auPtr->size;
    }

    if ((mMp4FileDataPtr->estimateAudioSize == M4OSA_TRUE)
        && (streamID == VideoStreamID))
    {
        /*update filesize with estimated audio data that will be added later.    */
        /*Warning: Assumption is made that:                                     */
        /* - audio samples have constant size (e.g. no sid).                    */
        /* - max audio sample size has been set, and is the actual sample size. */

        ERR_CHECK(mMp4FileDataPtr->audioMsChunkDur != 0,
            M4WAR_MP4W_NOT_EVALUABLE);
        mMp4FileDataPtr->filesize -=
            (M4OSA_UInt32)(( mMp4FileDataPtr->videoTrackPtr->CommonData.lastCTS
            * scale_video) * (0.05/*always 50 AMR samples per second*/
            *(M4OSA_Double)mMp4FileDataPtr->audioTrackPtr->MaxAUSize
            + 16/*additional data for a new chunk*/
            / (M4OSA_Double)mMp4FileDataPtr->audioMsChunkDur));

        mMp4FileDataPtr->filesize += (M4OSA_UInt32)(( auPtr->CTS * scale_video)
            * (0.05/*always 50 AMR samples per second*/
            *(M4OSA_Double)mMp4FileDataPtr->audioTrackPtr->MaxAUSize
            + 16/*additional data for a new chunk*/
            / (M4OSA_Double)mMp4FileDataPtr->audioMsChunkDur));
    }

    M4OSA_TRACE1_4("processAU : size 0x%x mode %d filesize %lu limit %lu",
        auPtr->size, auPtr->attribute, mMp4FileDataPtr->filesize,
        mMp4FileDataPtr->MaxFileSize);

    return err;
}

/*******************************************************************************/
M4OSA_ERR M4MP4W_closeWrite( M4OSA_Context context )
/*******************************************************************************/
{
    M4OSA_ERR err = M4NO_ERROR;
    M4OSA_ERR err2 = M4NO_ERROR, err3 = M4NO_ERROR;

    /*Warning: test should be done here to ensure context->pContext is not M4OSA_NULL,
     but C is not C++...*/
    M4MP4W_Mp4FileData *mMp4FileDataPtr = (M4MP4W_Mp4FileData *)context;

    M4OSA_UChar camcoder_maj, camcoder_min, camcoder_rev; /*camcoder version*/
    M4OSA_Bool bAudio =
        (( mMp4FileDataPtr->hasAudio)
        && (mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb
        != 0)); /*((mMp4FileDataPtr->audioTrackPtr != M4OSA_NULL) &&
                    (mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb != 0));*/
    M4OSA_Bool bVideo =
        (( mMp4FileDataPtr->hasVideo)
        && (mMp4FileDataPtr->videoTrackPtr->CommonData.sampleNb
        != 0)); /*((mMp4FileDataPtr->videoTrackPtr != M4OSA_NULL) &&
                    (mMp4FileDataPtr->videoTrackPtr->CommonData.sampleNb != 0));*/
    M4OSA_Bool bH263 = M4OSA_FALSE;
    M4OSA_Bool bH264 = M4OSA_FALSE;
    M4OSA_Bool bMP4V = M4OSA_FALSE;
    M4OSA_Bool bAAC = M4OSA_FALSE;
    M4OSA_Bool bEVRC = M4OSA_FALSE;

    /*intermediate variables*/
    M4OSA_UInt32 A, B, N, AB4N;

    /*Trak variables*/
    M4OSA_UInt32 a_trakId = AudioStreamID; /*     (audio=1)*/
    /* first trak offset is 32+moovSize, second equals 32+moovSize+1st_track_size*/
    M4OSA_UInt32 a_trakOffset = 32;
    M4OSA_UInt32 a_sttsSize = 24;          /* A (audio=24)*/
    M4OSA_UInt32 a_stszSize = 20;          /* B (audio=20)*/
    M4OSA_UInt32 a_trakSize = 402;         /*     (audio=402)*/
    M4OSA_UInt32 a_mdiaSize = 302;         /*     (audio=302)*/
    M4OSA_UInt32 a_minfSize = 229;         /*     (audio=229)*/
    M4OSA_UInt32 a_stblSize = 169;         /*     (audio=169)*/
    M4OSA_UInt32 a_stsdSize = 69;          /*     (audio=69 )*/
    M4OSA_UInt32 a_esdSize = 53;           /*     (audio=53 )*/
    M4OSA_UInt32 a_dataSize = 0;           /* temp: At the end, = currentPos*/
    M4MP4W_Time32 a_trakDuration = 0;      /* equals lastCTS*/
    M4MP4W_Time32 a_msTrakDuration = 0;
    M4OSA_UInt32 a_stscSize = 28;          /* 16+12*nbchunksaudio*/
    M4OSA_UInt32 a_stcoSize = 20;          /* 16+4*nbchunksaudio*/

    M4OSA_UInt32 v_trakId = VideoStreamID; /* (video=2)*/
    /* first trak offset is 32+moovSize, second equals 32+moovSize+1st_track_size*/
    M4OSA_UInt32 v_trakOffset = 32;
    M4OSA_UInt32 v_sttsSize = 0;      /* A (video=16+8J)*/
    M4OSA_UInt32 v_stszSize = 0;      /* B (video=20+4K)*/
    M4OSA_UInt32 v_trakSize = 0; /* (h263=A+B+4N+426), (mp4v=A+B+dsi+4N+448) */
    M4OSA_UInt32 v_mdiaSize = 0; /* (h263=A+B+4N+326), (mp4v=A+B+dsi+4N+348) */
    M4OSA_UInt32 v_minfSize = 0; /* (h263=A+B+4N+253), (mp4v=A+B+dsi+4N+275) */
    M4OSA_UInt32 v_stblSize = 0; /* (h263=A+B+4N+189), (mp4v=A+B+dsi+4N+211) */
    M4OSA_UInt32 v_stsdSize = 0;      /* (h263=117)        , (mp4v=139+dsi    )*/
    M4OSA_UInt32 v_esdSize = 0;       /* (h263=101)        , (mp4v=153+dsi    )*/
    M4OSA_UInt32 v_dataSize = 0;      /* temp: At the end, = currentPos*/
    M4MP4W_Time32 v_trakDuration = 0; /* equals lastCTS*/
    M4MP4W_Time32 v_msTrakDuration = 0;
    M4OSA_UInt32 v_stscSize = 28;     /* 16+12*nbchunksvideo*/
    M4OSA_UInt32 v_stcoSize = 20;     /* 16+4*nbchunksvideo*/

    /*video variables*/
    M4OSA_UInt32 v_stssSize = 0; /* 4*N+16     STSS*/

    /*aac & mp4v temp variable*/
    M4OSA_UInt8 dsi = 0;

    /*H264 variables*/
    M4OSA_UInt32 v_avcCSize = 0; /* dsi+15*/

    /*MP4V variables*/
    M4OSA_UInt32 v_esdsSize = 0;        /* dsi+37*/
    M4OSA_UInt8 v_ESDescriptorSize =
        0; /* dsi+23 (warning: check dsi<105 for coding size on 1 byte)*/
    M4OSA_UInt8 v_DCDescriptorSize = 0; /* dsi+15*/

    /*AAC variables*/
    M4OSA_UInt32 a_esdsSize = 0;        /* dsi+37*/
    M4OSA_UInt8 a_ESDescriptorSize =
        0; /* dsi+23 (warning: check dsi<105 for coding size on 1 byte)*/
    M4OSA_UInt8 a_DCDescriptorSize = 0; /* dsi+15*/

    /*General variables*/

    /* audio chunk size + video chunk size*/
    M4OSA_UInt32 mdatSize = 8;
    M4OSA_UInt32 moovSize = 116; /* 116 + 402(audio) +    (A+B+4N+426)(h263) or */
    /*                        (A+B+dsi+4N+448)(mp4v)    */
    M4OSA_UInt32 creationTime; /* C */

    /*flag to set up the chunk interleave strategy*/
    M4OSA_Bool bInterleaveAV =
        (bAudio && bVideo && (mMp4FileDataPtr->InterleaveDur != 0));

    M4OSA_Context fileWriterContext = mMp4FileDataPtr->fileWriterContext;

    M4OSA_UInt32 i;

    M4OSA_Double scale_audio = 0.0;
    M4OSA_Double scale_video = 0.0;
    M4MP4W_Time32 delta;

#ifndef _M4MP4W_MOOV_FIRST

    M4OSA_FilePosition moovPos, mdatPos;

#endif /*_M4MP4W_MOOV_FIRST*/

    ERR_CHECK(context != M4OSA_NULL, M4ERR_PARAMETER);

    /*macro state */
    mMp4FileDataPtr->state = M4MP4W_closed;

    /*if no data !*/
    if ((!bAudio) && (!bVideo))
    {
        err = M4NO_ERROR; /*would be better to return a warning ?*/
        goto cleanup;
    }

#ifdef _M4MP4W_RESERVED_MOOV_DISK_SPACE
    /* Remove safety file to make room for what needs to be written out here
    (chunk flushing and moov). */

    if (M4OSA_TRUE == mMp4FileDataPtr->cleanSafetyFile)
    {
        M4OSA_Context tempContext;
        err = mMp4FileDataPtr->fileWriterFunctions->openWrite(&tempContext,
            mMp4FileDataPtr->safetyFileUrl,
            M4OSA_kFileWrite | M4OSA_kFileCreate);

        if (M4NO_ERROR != err)
            goto cleanup;
        err = mMp4FileDataPtr->fileWriterFunctions->closeWrite(tempContext);

        if (M4NO_ERROR != err)
            goto cleanup;
        mMp4FileDataPtr->safetyFileUrl = M4OSA_NULL;
        mMp4FileDataPtr->cleanSafetyFile = M4OSA_FALSE;
    }

#endif /* _M4MP4W_RESERVED_MOOV_DISK_SPACE */

    if (bVideo)
    {
        if ((M4OSA_NULL == mMp4FileDataPtr->videoTrackPtr->chunkOffsetTable)
            || (M4OSA_NULL == mMp4FileDataPtr->videoTrackPtr->chunkSizeTable)
            || (M4OSA_NULL
            == mMp4FileDataPtr->videoTrackPtr->chunkSampleNbTable)
            || (M4OSA_NULL
            == mMp4FileDataPtr->videoTrackPtr->chunkTimeMsTable)
            || (M4OSA_NULL == mMp4FileDataPtr->videoTrackPtr->TABLE_STSZ)
            || (M4OSA_NULL == mMp4FileDataPtr->videoTrackPtr->TABLE_STTS)
            || (M4OSA_NULL == mMp4FileDataPtr->videoTrackPtr->TABLE_STSS))
        {
            mMp4FileDataPtr->fileWriterFunctions->closeWrite(
                fileWriterContext); /**< close the stream anyway */
            M4MP4W_freeContext(context); /**< Free the context content */
            return M4ERR_ALLOC;
        }

        /*video microstate*/
        mMp4FileDataPtr->videoTrackPtr->microState = M4MP4W_closed;

        /*current chunk is the last one and gives the total number of video chunks (-1)*/
        for ( i = 0; i < mMp4FileDataPtr->videoTrackPtr->currentChunk; i++ )
        {
            v_dataSize += mMp4FileDataPtr->videoTrackPtr->chunkSizeTable[i];
        }

#ifndef _M4MP4W_MOOV_FIRST
#ifndef _M4MP4W_UNBUFFERED_VIDEO
        /*flush chunk*/

        if (mMp4FileDataPtr->videoTrackPtr->currentPos > 0)
        {
            err = M4MP4W_putBlock(mMp4FileDataPtr->videoTrackPtr->Chunk[0],
                mMp4FileDataPtr->videoTrackPtr->currentPos,
                mMp4FileDataPtr->fileWriterFunctions,
                mMp4FileDataPtr->fileWriterContext);

            if (M4NO_ERROR != err)
                goto cleanup;
        }

#endif

        M4OSA_TRACE1_0("flush video | CLOSE");
        M4OSA_TRACE1_3("current chunk = %d  offset = 0x%x size = 0x%08X",
            mMp4FileDataPtr->videoTrackPtr->currentChunk,
            mMp4FileDataPtr->absoluteCurrentPos,
            mMp4FileDataPtr->videoTrackPtr->currentPos);

        /*update chunk offset*/
        mMp4FileDataPtr->videoTrackPtr->
            chunkOffsetTable[mMp4FileDataPtr->videoTrackPtr->currentChunk] =
            mMp4FileDataPtr->absoluteCurrentPos;

        /*add chunk size to absoluteCurrentPos*/
        mMp4FileDataPtr->absoluteCurrentPos +=
            mMp4FileDataPtr->videoTrackPtr->currentPos;
#endif /*_M4MP4W_MOOV_FIRST*/

        /*update last chunk size, and add this value to v_dataSize*/

        mMp4FileDataPtr->videoTrackPtr->
            chunkSizeTable[mMp4FileDataPtr->videoTrackPtr->currentChunk] =
            mMp4FileDataPtr->videoTrackPtr->currentPos;
        v_dataSize +=
            mMp4FileDataPtr->videoTrackPtr->currentPos; /*add last chunk size*/

        v_trakDuration = mMp4FileDataPtr->videoTrackPtr->
            CommonData.lastCTS; /* equals lastCTS*/

        /* bugfix: if a new chunk was just created, cancel it before to close */
        if ((mMp4FileDataPtr->videoTrackPtr->currentChunk != 0)
            && (mMp4FileDataPtr->videoTrackPtr->currentPos == 0))
        {
            mMp4FileDataPtr->videoTrackPtr->currentChunk--;
        }
#ifdef _M4MP4W_UNBUFFERED_VIDEO

        if ((mMp4FileDataPtr->videoTrackPtr->
            chunkSampleNbTable[mMp4FileDataPtr->videoTrackPtr->
            currentStsc] & 0xFFF) == 0)
        {
            mMp4FileDataPtr->videoTrackPtr->currentStsc--;
        }

#endif /*_M4MP4W_UNBUFFERED_VIDEO*/

        /* Last sample duration */
        /* If we have the file duration we use it, else we duplicate the last AU */

        if (mMp4FileDataPtr->MaxFileDuration > 0)
        {
            /* use max file duration to calculate delta of last AU */
            delta = mMp4FileDataPtr->MaxFileDuration
                - mMp4FileDataPtr->videoTrackPtr->CommonData.lastCTS;
            v_trakDuration = mMp4FileDataPtr->MaxFileDuration;

            if (mMp4FileDataPtr->videoTrackPtr->CommonData.sampleNb > 1)
            {
                /* if more than 1 frame, create a new stts entry (else already created) */
                mMp4FileDataPtr->videoTrackPtr->CommonData.sttsTableEntryNb++;
            }

#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

            M4MP4W_put32_Lo(&mMp4FileDataPtr->videoTrackPtr->
                TABLE_STTS[mMp4FileDataPtr->videoTrackPtr->
                CommonData.sttsTableEntryNb - 1], 1);
            M4MP4W_put32_Hi(&mMp4FileDataPtr->videoTrackPtr->
                TABLE_STTS[mMp4FileDataPtr->videoTrackPtr->
                CommonData.sttsTableEntryNb - 1], delta);

#else

            mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[2
                *(mMp4FileDataPtr->videoTrackPtr->CommonData.sttsTableEntryNb
                - 1)] = 1;
            mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[2
                *(mMp4FileDataPtr->videoTrackPtr->CommonData.sttsTableEntryNb
                - 1) + 1] = delta;

#endif

        }
        else
        {
            /* duplicate the delta of the previous frame */
            if (mMp4FileDataPtr->videoTrackPtr->CommonData.sampleNb > 1)
            {
                /* if more than 1 frame, duplicate the stts entry (else already exists) */
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

                v_trakDuration +=
                    M4MP4W_get32_Hi(&mMp4FileDataPtr->videoTrackPtr->
                    TABLE_STTS[mMp4FileDataPtr->videoTrackPtr->
                    CommonData.sttsTableEntryNb - 1]);
                mMp4FileDataPtr->videoTrackPtr->
                    TABLE_STTS[mMp4FileDataPtr->videoTrackPtr->
                    CommonData.sttsTableEntryNb - 1] += 1;

#else

                v_trakDuration += mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[2
                    * (mMp4FileDataPtr->videoTrackPtr->
                    CommonData.sttsTableEntryNb - 1) + 1];
                mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[2 *(
                    mMp4FileDataPtr->videoTrackPtr->
                    CommonData.sttsTableEntryNb - 1)] += 1;

#endif

            }
            else
            {
                M4OSA_TRACE1_0("M4MP4W_closeWrite : ! videoTrackPtr,\
                     cannot know the duration of the unique AU !");
                /* If there is an audio track, we use it as a file duration
                (and so, as AU duration...) */
                if (mMp4FileDataPtr->audioTrackPtr != M4OSA_NULL)
                {
                    M4OSA_TRACE1_0(
                        "M4MP4W_closeWrite : ! Let's use the audio track duration !");
                    mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[1] =
                        (M4OSA_UInt32)(
                        mMp4FileDataPtr->audioTrackPtr->CommonData.lastCTS
                        * (1000.0 / mMp4FileDataPtr->audioTrackPtr->
                        CommonData.timescale));
                    v_trakDuration =
                        mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[1];
                }
                /* Else, we use a MAGICAL value (66 ms) */
                else
                {
                    M4OSA_TRACE1_0(
                        "M4MP4W_closeWrite : ! No audio track -> use magical value (66) !"); /*    */
                    mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[1] = 66;
                    v_trakDuration = 66;
                }
            }
        }

        /* Calculate table sizes */
        A = v_sttsSize = 16 + 8 * mMp4FileDataPtr->videoTrackPtr->
            CommonData.sttsTableEntryNb; /* A (video=16+8J)*/
        B = v_stszSize = 20 + 4 * mMp4FileDataPtr->videoTrackPtr->
            CommonData.sampleNb; /* B (video=20+4K)*/
        N = mMp4FileDataPtr->videoTrackPtr->stssTableEntryNb;
        AB4N = A + B + 4 * N;

        scale_video =
            1000.0 / mMp4FileDataPtr->videoTrackPtr->CommonData.timescale;
        v_msTrakDuration = (M4OSA_UInt32)(v_trakDuration * scale_video);

        /*Convert integers in the table from LE into BE*/
#ifndef _M4MP4W_OPTIMIZE_FOR_PHONE

        M4MP4W_table32ToBE(mMp4FileDataPtr->videoTrackPtr->TABLE_STSZ,
            mMp4FileDataPtr->videoTrackPtr->CommonData.sampleNb);
        M4MP4W_table32ToBE(mMp4FileDataPtr->videoTrackPtr->TABLE_STTS,
            2 * (mMp4FileDataPtr->videoTrackPtr->CommonData.sttsTableEntryNb));

#endif

        M4MP4W_table32ToBE(mMp4FileDataPtr->videoTrackPtr->TABLE_STSS,
            mMp4FileDataPtr->videoTrackPtr->stssTableEntryNb);

        if (mMp4FileDataPtr->videoTrackPtr->CommonData.trackType
            == M4SYS_kH263)
        {
            bH263 = M4OSA_TRUE;
            v_trakSize = AB4N + 426; /* (h263=A+B+4N+426)*/
            v_mdiaSize = AB4N + 326; /* (h263=A+B+4N+326)*/
            v_minfSize = AB4N + 253; /* (h263=A+B+4N+253)*/
            v_stblSize = AB4N + 189; /* (h263=A+B+4N+189)*/
            v_stsdSize = 117;        /* (h263=117)*/
            v_esdSize = 101;         /* (h263=101)*/

            moovSize += AB4N + 426;

            if (((M4OSA_Int32)mMp4FileDataPtr->videoTrackPtr->avgBitrate) != -1)
            {
                /*the optional 'bitr' atom is appended to the dsi,so filesize is 16 bytes bigger*/
                v_trakSize += 16;
                v_mdiaSize += 16;
                v_minfSize += 16;
                v_stblSize += 16;
                v_stsdSize += 16;
                v_esdSize += 16;
                moovSize += 16;
            }
        }
        else if (mMp4FileDataPtr->videoTrackPtr->CommonData.trackType
            == M4SYS_kH264)
        {
            bH264 = M4OSA_TRUE;
            /* For H264 there is no default DSI, and its presence is mandatory,
            so check the DSI has been set*/
            if (0 == mMp4FileDataPtr->videoTrackPtr->dsiSize
                || M4OSA_NULL == mMp4FileDataPtr->videoTrackPtr->DSI)
            {
                M4OSA_TRACE1_0(
                    "M4MP4W_closeWrite: error, no H264 DSI has been set!");
                err = M4ERR_STATE;
                goto cleanup;
            }

            /*H264 sizes of the atom*/

            // Remove the hardcoded DSI values of H264Block2
            // TODO: check bMULPPSSPS case
            v_avcCSize = sizeof(M4OSA_UInt32) + sizeof(H264Block2) +
                mMp4FileDataPtr->videoTrackPtr->dsiSize;

            v_trakSize = AB4N + v_avcCSize + 411;
            v_mdiaSize = AB4N + v_avcCSize + 311;
            v_minfSize = AB4N + v_avcCSize + 238;
            v_stblSize = AB4N + v_avcCSize + 174;
            v_stsdSize =        v_avcCSize + 102;
            v_esdSize  =        v_avcCSize + 86;

            moovSize   += AB4N + v_avcCSize + 411;

        }
        else if (mMp4FileDataPtr->videoTrackPtr->CommonData.trackType
            == M4SYS_kMPEG_4)
        {
            bMP4V = M4OSA_TRUE;
            /* For MPEG4 there is no default DSI, and its presence is mandatory,
            so check the DSI has been set*/
            if (0 == mMp4FileDataPtr->videoTrackPtr->dsiSize
                || M4OSA_NULL == mMp4FileDataPtr->videoTrackPtr->DSI)
            {
                M4OSA_TRACE1_0(
                    "M4MP4W_closeWrite: error, no MPEG4 DSI has been set!");
                err = M4ERR_STATE;
                goto cleanup;
            }

            /*MP4V variables*/
            dsi = mMp4FileDataPtr->videoTrackPtr->dsiSize;
            v_esdsSize = 37 + dsi;         /* dsi+37*/
            v_ESDescriptorSize =
                23
                + dsi; /* dsi+23 (warning: check dsi<105 for coding size on 1 byte)*/
            v_DCDescriptorSize = 15 + dsi; /* dsi+15*/

            v_trakSize = AB4N + dsi + 448; /* (mp4v=A+B+dsi+4N+448)    */
            v_mdiaSize = AB4N + dsi + 348; /* (mp4v=A+B+dsi+4N+348)    */
            v_minfSize = AB4N + dsi + 275; /* (mp4v=A+B+dsi+4N+275)    */
            v_stblSize = AB4N + dsi + 211; /* (mp4v=A+B+dsi+4N+211)    */
            v_stsdSize = dsi + 139;        /* (mp4v=139+dsi)*/
            v_esdSize = dsi + 123;         /* (mp4v=123+dsi)*/

            moovSize += AB4N + dsi + 448;
        }

        /*video variables*/
        v_stssSize = 16 + 4 * N; /* 4*N+16     STSS*/

#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE
        /* stsc update */

        v_stscSize += 12 * mMp4FileDataPtr->videoTrackPtr->currentStsc;
        v_stblSize += 12 * mMp4FileDataPtr->videoTrackPtr->currentStsc;
        v_minfSize += 12 * mMp4FileDataPtr->videoTrackPtr->currentStsc;
        v_mdiaSize += 12 * mMp4FileDataPtr->videoTrackPtr->currentStsc;
        v_trakSize += 12 * mMp4FileDataPtr->videoTrackPtr->currentStsc;
        moovSize += 12 * mMp4FileDataPtr->videoTrackPtr->currentStsc;

        /* stco update */
        v_stcoSize += 4 * mMp4FileDataPtr->videoTrackPtr->currentChunk;
        v_stblSize += 4 * mMp4FileDataPtr->videoTrackPtr->currentChunk;
        v_minfSize += 4 * mMp4FileDataPtr->videoTrackPtr->currentChunk;
        v_mdiaSize += 4 * mMp4FileDataPtr->videoTrackPtr->currentChunk;
        v_trakSize += 4 * mMp4FileDataPtr->videoTrackPtr->currentChunk;
        moovSize += 4 * mMp4FileDataPtr->videoTrackPtr->currentChunk;

#else
        /*stsc/stco update*/

        v_stscSize += 12 * mMp4FileDataPtr->videoTrackPtr->currentChunk;
        v_stcoSize += 4 * mMp4FileDataPtr->videoTrackPtr->currentChunk;
        v_stblSize += 16 * mMp4FileDataPtr->videoTrackPtr->currentChunk;
        v_minfSize += 16 * mMp4FileDataPtr->videoTrackPtr->currentChunk;
        v_mdiaSize += 16 * mMp4FileDataPtr->videoTrackPtr->currentChunk;
        v_trakSize += 16 * mMp4FileDataPtr->videoTrackPtr->currentChunk;
        moovSize += 16 * mMp4FileDataPtr->videoTrackPtr->currentChunk;

#endif

        /*update last chunk time*/

        mMp4FileDataPtr->videoTrackPtr->
            chunkTimeMsTable[mMp4FileDataPtr->videoTrackPtr->currentChunk] =
            v_msTrakDuration;
    }

    if (bAudio)
    {
        if ((M4OSA_NULL == mMp4FileDataPtr->audioTrackPtr->chunkOffsetTable)
            || (M4OSA_NULL == mMp4FileDataPtr->audioTrackPtr->chunkSizeTable)
            || (M4OSA_NULL
            == mMp4FileDataPtr->audioTrackPtr->chunkSampleNbTable)
            || (M4OSA_NULL
            == mMp4FileDataPtr->audioTrackPtr->chunkTimeMsTable)
            || (M4OSA_NULL == mMp4FileDataPtr->audioTrackPtr->TABLE_STTS))
        {
            mMp4FileDataPtr->fileWriterFunctions->closeWrite(
                fileWriterContext); /**< close the stream anyway */
            M4MP4W_freeContext(context); /**< Free the context content */
            return M4ERR_ALLOC;
        }

        /*audio microstate*/
        mMp4FileDataPtr->audioTrackPtr->microState = M4MP4W_closed;

        if (mMp4FileDataPtr->audioTrackPtr->CommonData.trackType == M4SYS_kAAC)
        {
            bAAC =
                M4OSA_TRUE; /*else, audio is implicitely amr in the following*/
            dsi = mMp4FileDataPtr->audioTrackPtr->dsiSize; /*variable size*/

            a_esdsSize = 37 + dsi;                         /* dsi+37*/
            a_ESDescriptorSize =
                23
                + dsi; /* dsi+23 (warning: check dsi<105 for coding size on 1 byte)*/
            a_DCDescriptorSize = 15 + dsi;                 /* dsi+15*/

            a_esdSize = dsi + 73; /*overwrite a_esdSize with aac value*/
            /*add dif. between amr & aac sizes: (- 53 + dsi + 37)*/
            a_stsdSize += dsi + 20;
            a_stblSize += dsi + 20;
            a_minfSize += dsi + 20;
            a_mdiaSize += dsi + 20;
            a_trakSize += dsi + 20;
            moovSize += dsi + 20;
        }

        if (mMp4FileDataPtr->audioTrackPtr->CommonData.trackType
            == M4SYS_kEVRC)
        {
            bEVRC =
                M4OSA_TRUE; /*else, audio is implicitely amr in the following*/

            /* evrc dsi is only 6 bytes while amr dsi is 9 bytes,all other blocks are unchanged */
            a_esdSize -= 3;
            a_stsdSize -= 3;
            a_stblSize -= 3;
            a_minfSize -= 3;
            a_mdiaSize -= 3;
            a_trakSize -= 3;
            moovSize -= 3;
        }

        if (mMp4FileDataPtr->audioTrackPtr->CommonData.sampleSize == 0)
        {
            if (M4OSA_NULL == mMp4FileDataPtr->audioTrackPtr->TABLE_STSZ)
            {
                mMp4FileDataPtr->fileWriterFunctions->closeWrite(
                    fileWriterContext); /**< close the stream anyway */
                M4MP4W_freeContext(context); /**< Free the context content */
                return M4ERR_ALLOC;
            }
            /*Convert integers in the table from LE into BE*/
            M4MP4W_table32ToBE(mMp4FileDataPtr->audioTrackPtr->TABLE_STSZ,
                mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb);
            a_stszSize +=
                4 * mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb;
            a_stblSize +=
                4 * mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb;
            a_minfSize +=
                4 * mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb;
            a_mdiaSize +=
                4 * mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb;
            a_trakSize +=
                4 * mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb;
            moovSize += 4 * mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb;
        }

        moovSize += 402;

        /*current chunk is the last one and gives the total number of audio chunks (-1)*/
        for ( i = 0; i < mMp4FileDataPtr->audioTrackPtr->currentChunk; i++ )
        {
            a_dataSize += mMp4FileDataPtr->audioTrackPtr->chunkSizeTable[i];
        }

#ifndef _M4MP4W_MOOV_FIRST
        /*flush chunk*/

        if (mMp4FileDataPtr->audioTrackPtr->currentPos > 0)
        {
            err = M4MP4W_putBlock(mMp4FileDataPtr->audioTrackPtr->Chunk[0],
                mMp4FileDataPtr->audioTrackPtr->currentPos,
                mMp4FileDataPtr->fileWriterFunctions,
                mMp4FileDataPtr->fileWriterContext);

            if (M4NO_ERROR != err)
                goto cleanup;
        }

        M4OSA_TRACE1_0("flush audio | CLOSE");
        M4OSA_TRACE1_2("current chunk = %d  offset = 0x%x",
            mMp4FileDataPtr->audioTrackPtr->currentChunk,
            mMp4FileDataPtr->absoluteCurrentPos);

        /*update chunk offset*/
        mMp4FileDataPtr->audioTrackPtr->
            chunkOffsetTable[mMp4FileDataPtr->audioTrackPtr->currentChunk] =
            mMp4FileDataPtr->absoluteCurrentPos;

        /*add chunk size to absoluteCurrentPos*/
        mMp4FileDataPtr->absoluteCurrentPos +=
            mMp4FileDataPtr->audioTrackPtr->currentPos;

#endif /*_M4MP4W_MOOV_FIRST*/

        /*update last chunk size, and add this value to a_dataSize*/

        mMp4FileDataPtr->audioTrackPtr->
            chunkSizeTable[mMp4FileDataPtr->audioTrackPtr->currentChunk] =
            mMp4FileDataPtr->audioTrackPtr->currentPos;
        a_dataSize +=
            mMp4FileDataPtr->audioTrackPtr->currentPos; /*add last chunk size*/

        /* bugfix: if a new chunk was just created, cancel it before to close */
        if ((mMp4FileDataPtr->audioTrackPtr->currentChunk != 0)
            && (mMp4FileDataPtr->audioTrackPtr->currentPos == 0))
        {
            mMp4FileDataPtr->audioTrackPtr->currentChunk--;
        }
#ifdef _M4MP4W_UNBUFFERED_VIDEO

        if ((mMp4FileDataPtr->audioTrackPtr->
            chunkSampleNbTable[mMp4FileDataPtr->audioTrackPtr->
            currentStsc] & 0xFFF) == 0)
        {
            mMp4FileDataPtr->audioTrackPtr->currentStsc--;
        }

#endif                                                          /*_M4MP4W_UNBUFFERED_VIDEO*/

        a_trakDuration = mMp4FileDataPtr->audioTrackPtr->
            CommonData.lastCTS; /* equals lastCTS*/
        /* add last sample dur */

        if (mMp4FileDataPtr->audioTrackPtr->CommonData.sttsTableEntryNb != 1)
        {
#ifdef DUPLICATE_STTS_IN_LAST_AU
            /*increase of 1 the number of consecutive AUs with same duration*/

            mMp4FileDataPtr->audioTrackPtr->TABLE_STTS[2
                *(mMp4FileDataPtr->audioTrackPtr->CommonData.sttsTableEntryNb
                - 1) - 2] += 1;

#endif /*DUPLICATE_STTS_IN_LAST_AU*/

            a_trakDuration += mMp4FileDataPtr->audioTrackPtr->TABLE_STTS[2
                * (mMp4FileDataPtr->audioTrackPtr->
                CommonData.sttsTableEntryNb - 1) - 1];
        }
        else if (0 == mMp4FileDataPtr->audioTrackPtr->CommonData.lastCTS)
        {
            if (mMp4FileDataPtr->audioTrackPtr->CommonData.trackType
                == M4SYS_kAMR)
            {
                if (12200 == mMp4FileDataPtr->audioTrackPtr->avgBitrate)
                {
                    a_trakDuration = a_dataSize / 32
                        * mMp4FileDataPtr->audioTrackPtr->sampleDuration;
                }
                else if (10200 == mMp4FileDataPtr->audioTrackPtr->avgBitrate)
                {
                    a_trakDuration = a_dataSize / 27
                        * mMp4FileDataPtr->audioTrackPtr->sampleDuration;
                }
                else if (7950 == mMp4FileDataPtr->audioTrackPtr->avgBitrate)
                {
                    a_trakDuration = a_dataSize / 21
                        * mMp4FileDataPtr->audioTrackPtr->sampleDuration;
                }
                else if (7400 == mMp4FileDataPtr->audioTrackPtr->avgBitrate)
                {
                    a_trakDuration = a_dataSize / 20
                        * mMp4FileDataPtr->audioTrackPtr->sampleDuration;
                }
                else if (6700 == mMp4FileDataPtr->audioTrackPtr->avgBitrate)
                {
                    a_trakDuration = a_dataSize / 18
                        * mMp4FileDataPtr->audioTrackPtr->sampleDuration;
                }
                else if (5900 == mMp4FileDataPtr->audioTrackPtr->avgBitrate)
                {
                    a_trakDuration = a_dataSize / 16
                        * mMp4FileDataPtr->audioTrackPtr->sampleDuration;
                }
                else if (5150 == mMp4FileDataPtr->audioTrackPtr->avgBitrate)
                {
                    a_trakDuration = a_dataSize / 14
                        * mMp4FileDataPtr->audioTrackPtr->sampleDuration;
                }
                else if (4750 == mMp4FileDataPtr->audioTrackPtr->avgBitrate)
                {
                    a_trakDuration = a_dataSize / 13
                        * mMp4FileDataPtr->audioTrackPtr->sampleDuration;
                }
            }
        }

        scale_audio =
            1000.0 / mMp4FileDataPtr->audioTrackPtr->CommonData.timescale;
        a_msTrakDuration = (M4OSA_UInt32)(a_trakDuration * scale_audio);

#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE
        /* stsc update */

        a_stscSize += 12 * mMp4FileDataPtr->audioTrackPtr->currentStsc;
        a_stblSize += 12 * mMp4FileDataPtr->audioTrackPtr->currentStsc;
        a_minfSize += 12 * mMp4FileDataPtr->audioTrackPtr->currentStsc;
        a_mdiaSize += 12 * mMp4FileDataPtr->audioTrackPtr->currentStsc;
        a_trakSize += 12 * mMp4FileDataPtr->audioTrackPtr->currentStsc;
        moovSize += 12 * mMp4FileDataPtr->audioTrackPtr->currentStsc;

        /* stso update */
        a_stcoSize += 4 * mMp4FileDataPtr->audioTrackPtr->currentChunk;
        a_stblSize += 4 * mMp4FileDataPtr->audioTrackPtr->currentChunk;
        a_minfSize += 4 * mMp4FileDataPtr->audioTrackPtr->currentChunk;
        a_mdiaSize += 4 * mMp4FileDataPtr->audioTrackPtr->currentChunk;
        a_trakSize += 4 * mMp4FileDataPtr->audioTrackPtr->currentChunk;
        moovSize += 4 * mMp4FileDataPtr->audioTrackPtr->currentChunk;

#else
        /*stsc/stco update*/

        a_stscSize += 12 * mMp4FileDataPtr->audioTrackPtr->currentChunk;
        a_stcoSize += 4 * mMp4FileDataPtr->audioTrackPtr->currentChunk;
        a_stblSize += 16 * mMp4FileDataPtr->audioTrackPtr->currentChunk;
        a_minfSize += 16 * mMp4FileDataPtr->audioTrackPtr->currentChunk;
        a_mdiaSize += 16 * mMp4FileDataPtr->audioTrackPtr->currentChunk;
        a_trakSize += 16 * mMp4FileDataPtr->audioTrackPtr->currentChunk;
        moovSize += 16 * mMp4FileDataPtr->audioTrackPtr->currentChunk;

#endif

        /* compute the new size of stts*/

        a_sttsSize = 16 + 8 * (mMp4FileDataPtr->audioTrackPtr->
            CommonData.sttsTableEntryNb - 1);

        moovSize += a_sttsSize - 24;
        a_mdiaSize += a_sttsSize - 24;
        a_minfSize += a_sttsSize - 24;
        a_stblSize += a_sttsSize - 24;
        a_trakSize += a_sttsSize - 24;

        /*update last chunk time*/
        mMp4FileDataPtr->audioTrackPtr->
            chunkTimeMsTable[mMp4FileDataPtr->audioTrackPtr->currentChunk] =
            a_msTrakDuration;
    }

    /* changing the way the mdat size is computed.
    The real purpose of the mdat size is to know the amount to skip to get to the next
    atom, which is the moov; the size of media in the mdat is almost secondary. Therefore,
    it is of utmost importance that the mdat size "points" to where the moov actually
    begins. Now, the moov begins right after the last data we wrote, so how could the sum
    of all chunk sizes be different from the total size of what has been written? Well, it
    can happen when the writing was unexpectedly stopped (because of lack of disk space,
    for instance), in this case a chunk may be partially written (the partial write is not
    necessarily erased) but it may not be reflected in the chunk size list (which may
    believe it hasn't been written or on the contrary that it has been fully written). In
    the case of such a mismatch, there is either unused data in the mdat (not very good,
    but tolerable) or when reading the last chunk it will read the beginning of the moov
    as part of the chunk (which means the last chunk won't be correctly decoded), both of
    which are still better than losing the whole recording. In the long run it'll probably
    be attempted to always clean up back to a consistent state, but at any rate it is
    always safer to have the mdat size be computed using the position where the moov
    actually begins, rather than using the size it is thought the mdat has.

    Therefore, I will record where we are just before writing the moov, to serve when
    updating the mdat size. */

    /* mdatSize += a_dataSize + v_dataSize; *//*TODO allow for multiple chunks*/

    /* End of Pierre Lebeaupin 19/12/2007: changing the way the mdat size is computed. */

    /* first trak offset is 32+moovSize, second equals 32+moovSize+1st_track_size*/
    a_trakOffset += moovSize;
    v_trakOffset += moovSize/*+ a_dataSize*/;

    if (bInterleaveAV == M4OSA_FALSE)
        v_trakOffset += a_dataSize;

    /*system time since 1970 */
#ifndef _M4MP4W_DONT_USE_TIME_H

    time((time_t *)&creationTime);
    /*convert into time since 1/1/1904 00h00 (normative)*/
    creationTime += 2082841761; /*nb of sec between 1904 and 1970*/

#else                                            /*_M4MP4W_DONT_USE_TIME_H*/

    creationTime =
        0xBBD09100; /* = 7/11/2003 00h00 ; in hexa because of code scrambler limitation with
                                           large integers */

#endif                                           /*_M4MP4W_DONT_USE_TIME_H*/

    mMp4FileDataPtr->duration =
        max(a_msTrakDuration, v_msTrakDuration); /*max audio/video*/

#ifdef _M4MP4W_MOOV_FIRST
    /*open file in write binary mode*/

    err = mMp4FileDataPtr->fileWriterFunctions->openWrite(&fileWriterContext,
        mMp4FileDataPtr->url, 0x22);
    ERR_CHECK(err == M4NO_ERROR, err);

    /*ftyp atom*/
    if (mMp4FileDataPtr->ftyp.major_brand != 0)
    {
        M4OSA_UInt32 i;

        /* Put customized ftyp box */
        CLEANUPonERR(M4MP4W_putBE32(16
            + (mMp4FileDataPtr->ftyp.nbCompatibleBrands * 4),
            mMp4FileDataPtr->fileWriterFunctions,
            mMp4FileDataPtr->fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(M4MPAC_FTYP_TAG,
            mMp4FileDataPtr->fileWriterFunctions,
            mMp4FileDataPtr->fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(mMp4FileDataPtr->ftyp.major_brand,
            mMp4FileDataPtr->fileWriterFunctions,
            mMp4FileDataPtr->fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(mMp4FileDataPtr->ftyp.minor_version,
            mMp4FileDataPtr->fileWriterFunctions,
            mMp4FileDataPtr->fileWriterContext));

        for ( i = 0; i < mMp4FileDataPtr->ftyp.nbCompatibleBrands; i++ )
        {
            CLEANUPonERR(
                M4MP4W_putBE32(mMp4FileDataPtr->ftyp.compatible_brands[i],
                mMp4FileDataPtr->fileWriterFunctions,
                mMp4FileDataPtr->fileWriterContext));
        }
    }
    else
    {
        /* Put default ftyp box */
        CLEANUPonERR(M4MP4W_putBlock(Default_ftyp, sizeof(Default_ftyp),
            mMp4FileDataPtr->fileWriterFunctions,
            mMp4FileDataPtr->fileWriterContext));
    }

#endif /*_M4MP4W_MOOV_FIRST*/

#ifndef _M4MP4W_MOOV_FIRST
    /* seek is used to get the current position relative to the start of the file. */
    /* ... or rather, seek used to be used for that, but it has been found this functionality
    is not reliably, or sometimes not at all, implemented in the various OSALs, so we now avoid
    using it. */
    /* Notice this new method assumes we're at the end of the file, this will break if ever we
    are overwriting a larger file. */

    CLEANUPonERR(mMp4FileDataPtr->fileWriterFunctions->getOption(
        mMp4FileDataPtr->fileWriterContext,
        M4OSA_kFileWriteGetFileSize, (M4OSA_DataOption *) &moovPos));
    /* moovPos will be used after writing the moov. */

#endif /*_M4MP4W_MOOV_FIRST*/

    CLEANUPonERR(M4MP4W_putBE32(moovSize, mMp4FileDataPtr->fileWriterFunctions,
        fileWriterContext));
    CLEANUPonERR(M4MP4W_putBlock(CommonBlock3, sizeof(CommonBlock3),
        mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
    CLEANUPonERR(M4MP4W_putBE32(creationTime,
        mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
    CLEANUPonERR(M4MP4W_putBE32(creationTime,
        mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
    CLEANUPonERR(M4MP4W_putBlock(CommonBlock4, sizeof(CommonBlock4),
        mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
    CLEANUPonERR(M4MP4W_putBE32(mMp4FileDataPtr->duration,
        mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
    CLEANUPonERR(M4MP4W_putBlock(CommonBlock5, sizeof(CommonBlock5),
        mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

    if (bAudio)
    {
        CLEANUPonERR(M4MP4W_putBE32(a_trakSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock6, sizeof(CommonBlock6),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(creationTime,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(creationTime,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(a_trakId,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock7, sizeof(CommonBlock7),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(a_msTrakDuration,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock7bis, sizeof(CommonBlock7bis),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(AMRBlock1, sizeof(AMRBlock1),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*audio*/
        CLEANUPonERR(M4MP4W_putBE32(a_mdiaSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock8, sizeof(CommonBlock8),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(creationTime,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(creationTime,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(
            M4MP4W_putBE32(mMp4FileDataPtr->audioTrackPtr->CommonData.timescale,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(a_trakDuration,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock9, sizeof(CommonBlock9),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(AMRBlock1_1, sizeof(AMRBlock1_1),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*audio*/
        CLEANUPonERR(M4MP4W_putBE32(a_minfSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock10, sizeof(CommonBlock10),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(a_stblSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock11, sizeof(CommonBlock11),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(a_sttsSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock12, sizeof(CommonBlock12),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

        CLEANUPonERR(M4MP4W_putBE32(
            mMp4FileDataPtr->audioTrackPtr->CommonData.sttsTableEntryNb - 1,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        /*invert the table data to bigendian*/
        M4MP4W_table32ToBE(mMp4FileDataPtr->audioTrackPtr->TABLE_STTS,
            2 * (mMp4FileDataPtr->audioTrackPtr->CommonData.sttsTableEntryNb
            - 1));
        CLEANUPonERR(M4MP4W_putBlock((const M4OSA_UChar
            *)mMp4FileDataPtr->audioTrackPtr->TABLE_STTS,
            ( mMp4FileDataPtr->audioTrackPtr->CommonData.sttsTableEntryNb - 1)
            * 8,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*audio*/

        /* stsd */
        CLEANUPonERR(M4MP4W_putBE32(a_stsdSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(SampleDescriptionHeader,
            sizeof(SampleDescriptionHeader),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(a_esdSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

        /* sample desc entry inside stsd */
        if (bAAC)
        {
            CLEANUPonERR(M4MP4W_putBlock(AACBlock1, sizeof(AACBlock1),
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*aac*/
        }
        else if (bEVRC)
        {
            CLEANUPonERR(M4MP4W_putBlock(EVRC8Block1, sizeof(EVRC8Block1),
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*evrc*/
        }
        else                         /*AMR8*/
        {
            CLEANUPonERR(M4MP4W_putBlock(AMR8Block1, sizeof(AMR8Block1),
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*amr8*/
        }
        CLEANUPonERR(M4MP4W_putBlock(SampleDescriptionEntryStart,
            sizeof(SampleDescriptionEntryStart),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(AudioSampleDescEntryBoilerplate,
            sizeof(AudioSampleDescEntryBoilerplate),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*audio*/
        CLEANUPonERR(
            M4MP4W_putBE32(mMp4FileDataPtr->audioTrackPtr->CommonData.timescale
            << 16,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

        /* DSI inside sample desc entry */
        if (bAAC)
        {
            CLEANUPonERR(M4MP4W_putBE32(a_esdsSize,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*aac*/
            CLEANUPonERR(M4MP4W_putBlock(MPEGConfigBlock0,
                sizeof(MPEGConfigBlock0), mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*aac*/
            CLEANUPonERR(M4MP4W_putByte(a_ESDescriptorSize,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*aac*/
            CLEANUPonERR(M4MP4W_putBlock(MPEGConfigBlock1,
                sizeof(MPEGConfigBlock1), mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*aac*/
            CLEANUPonERR(M4MP4W_putByte(a_DCDescriptorSize,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*aac*/
            CLEANUPonERR(M4MP4W_putBlock(AACBlock2, sizeof(AACBlock2),
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*aac*/
            CLEANUPonERR(
                M4MP4W_putBE24(mMp4FileDataPtr->audioTrackPtr->avgBitrate * 5,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*aac*/
            CLEANUPonERR(
                M4MP4W_putBE32(mMp4FileDataPtr->audioTrackPtr->maxBitrate,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*aac*/
            CLEANUPonERR(
                M4MP4W_putBE32(mMp4FileDataPtr->audioTrackPtr->avgBitrate,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*aac*/
            CLEANUPonERR(M4MP4W_putBlock(MPEGConfigBlock2,
                sizeof(MPEGConfigBlock2), mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*aac*/
            CLEANUPonERR(M4MP4W_putByte(mMp4FileDataPtr->audioTrackPtr->dsiSize,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*aac*/
            CLEANUPonERR(M4MP4W_putBlock(mMp4FileDataPtr->audioTrackPtr->DSI,
                mMp4FileDataPtr->audioTrackPtr->dsiSize,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*aac*/
            CLEANUPonERR(M4MP4W_putBlock(MPEGConfigBlock3,
                sizeof(MPEGConfigBlock3), mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*aac*/
        }
        else if (bEVRC)
        {
            M4OSA_UInt8 localDsi[6];
            M4OSA_UInt32 localI;

            CLEANUPonERR(M4MP4W_putBlock(EVRCBlock3_1, sizeof(EVRCBlock3_1),
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*audio*/

            /* copy the default block in a local variable*/
            for ( localI = 0; localI < 6; localI++ )
            {
                localDsi[localI] = EVRCBlock3_2[localI];
            }
            /* computes the number of sample per au */
            /* and stores it in the DSI*/
            /* assumes a char is enough to store the data*/
            localDsi[5] =
                (M4OSA_UInt8)(mMp4FileDataPtr->audioTrackPtr->sampleDuration
                / 160)/*EVRC 1 frame duration*/;

            if (mMp4FileDataPtr->audioTrackPtr->DSI != M4OSA_NULL)
            {
                /* copy vendor name */
                for ( localI = 0; localI < 4; localI++ )
                {
                    localDsi[localI] = (M4OSA_UInt8)(
                        mMp4FileDataPtr->audioTrackPtr->DSI[localI]);
                }
            }
            CLEANUPonERR(M4MP4W_putBlock(localDsi, 6,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*audio*/
        }
        else                         /*AMR8*/
        {
            M4OSA_UInt8 localDsi[9];
            M4OSA_UInt32 localI;

            CLEANUPonERR(M4MP4W_putBlock(AMRDSIHeader, sizeof(AMRDSIHeader),
                mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

            /* copy the default block in a local variable*/
            for ( localI = 0; localI < 9; localI++ )
            {
                localDsi[localI] = AMRDefaultDSI[localI];
            }
            /* computes the number of sample per au */
            /* and stores it in the DSI*/
            /* assumes a char is enough to store the data*/
            /* ALERT! The potential of the following line of code to explode in our face
            is enormous when anything (sample rate or whatever) will change. This
            calculation would be MUCH better handled by the VES or whatever deals with
            the encoder more directly. */
            localDsi[8] =
                (M4OSA_UInt8)(mMp4FileDataPtr->audioTrackPtr->sampleDuration
                / 160)/*AMR NB 1 frame duration*/;

            if (mMp4FileDataPtr->audioTrackPtr->DSI != M4OSA_NULL)
            {
                /* copy vendor name */
                for ( localI = 0; localI < 4; localI++ )
                {
                    localDsi[localI] = (M4OSA_UInt8)(
                        mMp4FileDataPtr->audioTrackPtr->DSI[localI]);
                }

                /* copy the Mode Set */
                for ( localI = 5; localI < 7; localI++ )
                {
                    localDsi[localI] = (M4OSA_UInt8)(
                        mMp4FileDataPtr->audioTrackPtr->DSI[localI]);
                }
            }
            CLEANUPonERR(M4MP4W_putBlock(localDsi, 9,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*audio*/
        }

        /*end trak*/
        CLEANUPonERR(M4MP4W_putBE32(a_stszSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock15, sizeof(CommonBlock15),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(
            mMp4FileDataPtr->audioTrackPtr->CommonData.sampleSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(
            M4MP4W_putBE32(mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

        /*0 value for samplesize means not constant AU size*/
        if (mMp4FileDataPtr->audioTrackPtr->CommonData.sampleSize == 0)
        {
            CLEANUPonERR(M4MP4W_putBlock((const M4OSA_UChar
                *)mMp4FileDataPtr->audioTrackPtr->TABLE_STSZ,
                mMp4FileDataPtr->audioTrackPtr->CommonData.sampleNb * 4,
                mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        }

        CLEANUPonERR(M4MP4W_putBE32(a_stscSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock16, sizeof(CommonBlock16),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

        CLEANUPonERR(M4MP4W_putBE32(mMp4FileDataPtr->audioTrackPtr->currentStsc
            + 1, mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

        for ( i = 0; i <= mMp4FileDataPtr->audioTrackPtr->currentStsc; i++ )
        {
            CLEANUPonERR(M4MP4W_putBE32(
                ( mMp4FileDataPtr->audioTrackPtr->chunkSampleNbTable[i]
            >> 12) + 1, mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext));
            CLEANUPonERR(M4MP4W_putBE32((mMp4FileDataPtr->audioTrackPtr->
                chunkSampleNbTable[i] & 0xFFF),
                mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
            CLEANUPonERR(M4MP4W_putBE32(1, mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext));
        }

#else

        CLEANUPonERR(M4MP4W_putBE32(mMp4FileDataPtr->audioTrackPtr->currentChunk
            + 1, mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

        for ( i = 0; i <= mMp4FileDataPtr->audioTrackPtr->currentChunk; i++ )
        {
            CLEANUPonERR(M4MP4W_putBE32(i + 1,
                mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
            CLEANUPonERR(M4MP4W_putBE32(
                mMp4FileDataPtr->audioTrackPtr->chunkSampleNbTable[i],
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext));
            CLEANUPonERR(M4MP4W_putBE32(1, mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext));
        }

#endif

        CLEANUPonERR(M4MP4W_putBE32(a_stcoSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock17, sizeof(CommonBlock17),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(mMp4FileDataPtr->audioTrackPtr->currentChunk
            + 1, mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

#ifdef _M4MP4W_MOOV_FIRST

        for ( i = 0; i <= mMp4FileDataPtr->audioTrackPtr->currentChunk; i++ )
        {
            CLEANUPonERR(M4MP4W_putBE32(a_trakOffset,
                mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
            a_trakOffset += mMp4FileDataPtr->audioTrackPtr->chunkSizeTable[i];

            if (( bInterleaveAV == M4OSA_TRUE)
                && (mMp4FileDataPtr->videoTrackPtr->currentChunk >= i))
            {
                a_trakOffset +=
                    mMp4FileDataPtr->videoTrackPtr->chunkSizeTable[i];
            }
        }

#else

        for ( i = 0; i <= mMp4FileDataPtr->audioTrackPtr->currentChunk; i++ )
        {
            CLEANUPonERR(M4MP4W_putBE32(
                mMp4FileDataPtr->audioTrackPtr->chunkOffsetTable[i],
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext));
        }

#endif                                                                 /*_M4MP4W_MOOV_FIRST*/

        CLEANUPonERR(M4MP4W_putBlock(AMRBlock4, sizeof(AMRBlock4),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*audio*/
    }

    if (bVideo)
    {
        /*trak*/
        CLEANUPonERR(M4MP4W_putBE32(v_trakSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock6, sizeof(CommonBlock6),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(creationTime,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(creationTime,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(v_trakId,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock7, sizeof(CommonBlock7),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(v_msTrakDuration,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock7bis, sizeof(CommonBlock7bis),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

        /* In the track header width and height are 16.16 fixed point values,
        so shift left the regular integer value by 16. */
        CLEANUPonERR(M4MP4W_putBE32(mMp4FileDataPtr->videoTrackPtr->width << 16,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*video*/
        CLEANUPonERR(M4MP4W_putBE32(mMp4FileDataPtr->videoTrackPtr->height
            << 16,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*video*/

        CLEANUPonERR(M4MP4W_putBE32(v_mdiaSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock8, sizeof(CommonBlock8),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(creationTime,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(creationTime,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(
            M4MP4W_putBE32(mMp4FileDataPtr->videoTrackPtr->CommonData.timescale,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(v_trakDuration,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock9, sizeof(CommonBlock9),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(VideoBlock1_1, sizeof(VideoBlock1_1),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*video*/
        CLEANUPonERR(M4MP4W_putBE32(v_minfSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock10, sizeof(CommonBlock10),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(v_stblSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock11, sizeof(CommonBlock11),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(v_sttsSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock12, sizeof(CommonBlock12),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(
            mMp4FileDataPtr->videoTrackPtr->CommonData.sttsTableEntryNb,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

        for ( i = 0;
            i < mMp4FileDataPtr->videoTrackPtr->CommonData.sttsTableEntryNb;
            i++ )
        {
            CLEANUPonERR(M4MP4W_putBE32(M4MP4W_get32_Lo(
                &mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[i]),
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*video*/
            CLEANUPonERR(M4MP4W_putBE32(M4MP4W_get32_Hi(
                &mMp4FileDataPtr->videoTrackPtr->TABLE_STTS[i]),
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*video*/
        }

#else

        CLEANUPonERR(M4MP4W_putBlock((const M4OSA_UChar
            *)mMp4FileDataPtr->videoTrackPtr->TABLE_STTS,
            ( mMp4FileDataPtr->videoTrackPtr->CommonData.sttsTableEntryNb) * 8,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*video*/

#endif

        /* stsd */

        CLEANUPonERR(M4MP4W_putBE32(v_stsdSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(SampleDescriptionHeader,
            sizeof(SampleDescriptionHeader),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(v_esdSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

        /* sample desc entry inside stsd */
        if (bMP4V)
        {
            CLEANUPonERR(M4MP4W_putBlock(Mp4vBlock1, sizeof(Mp4vBlock1),
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*mp4v*/
        }

        if (bH263)
        {
            CLEANUPonERR(M4MP4W_putBlock(H263Block1, sizeof(H263Block1),
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*h263*/
        }

        if (bH264)
        {
            CLEANUPonERR(M4MP4W_putBlock(H264Block1, sizeof(H264Block1),
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*h264*/
        }
        CLEANUPonERR(M4MP4W_putBlock(SampleDescriptionEntryStart,
            sizeof(SampleDescriptionEntryStart),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(SampleDescriptionEntryVideoBoilerplate1,
            sizeof(SampleDescriptionEntryVideoBoilerplate1),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*video*/
        CLEANUPonERR(M4MP4W_putBE16(mMp4FileDataPtr->videoTrackPtr->width,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*video*/
        CLEANUPonERR(M4MP4W_putBE16(mMp4FileDataPtr->videoTrackPtr->height,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*video*/
        CLEANUPonERR(M4MP4W_putBlock(VideoResolutions, sizeof(VideoResolutions),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*mp4v*/
        CLEANUPonERR(M4MP4W_putBlock(SampleDescriptionEntryVideoBoilerplate2,
            sizeof(SampleDescriptionEntryVideoBoilerplate2),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*video*/

        /* DSI inside sample desc entry */
        if (bH263)
        {
            /* The h263 dsi given through the api must be 7 bytes, that is, it shall not include
             the optional bitrate box. However, if the bitrate information is set in the stream
             handler, a bitrate box is appended here to the dsi */
            if (((M4OSA_Int32)mMp4FileDataPtr->videoTrackPtr->avgBitrate) != -1)
            {
                CLEANUPonERR(M4MP4W_putBlock(H263Block2_bitr,
                    sizeof(H263Block2_bitr),
                    mMp4FileDataPtr->fileWriterFunctions,
                    fileWriterContext)); /* d263 box with bitr atom */

                if (M4OSA_NULL == mMp4FileDataPtr->videoTrackPtr->DSI)
                {
                    CLEANUPonERR(M4MP4W_putBlock(H263Block3, sizeof(H263Block3),
                        mMp4FileDataPtr->fileWriterFunctions,
                        fileWriterContext)); /*h263*/
                }
                else
                {
                    CLEANUPonERR(
                        M4MP4W_putBlock(mMp4FileDataPtr->videoTrackPtr->DSI,
                        mMp4FileDataPtr->videoTrackPtr->dsiSize,
                        mMp4FileDataPtr->fileWriterFunctions,
                        fileWriterContext));
                }

                CLEANUPonERR(M4MP4W_putBlock(H263Block4, sizeof(H263Block4),
                    mMp4FileDataPtr->fileWriterFunctions,
                    fileWriterContext)); /*h263*/
                /* Pierre Lebeaupin 2008/04/29: the two following lines used to be swapped;
                I changed to this order in order to conform to 3GPP. */
                CLEANUPonERR(
                    M4MP4W_putBE32(mMp4FileDataPtr->videoTrackPtr->avgBitrate,
                    mMp4FileDataPtr->fileWriterFunctions,
                    fileWriterContext)); /*h263*/
                CLEANUPonERR(
                    M4MP4W_putBE32(mMp4FileDataPtr->videoTrackPtr->maxBitrate,
                    mMp4FileDataPtr->fileWriterFunctions,
                    fileWriterContext)); /*h263*/
            }
            else
            {
                CLEANUPonERR(M4MP4W_putBlock(H263Block2, sizeof(H263Block2),
                    mMp4FileDataPtr->fileWriterFunctions,
                    fileWriterContext)); /* d263 box */

                if (M4OSA_NULL == mMp4FileDataPtr->videoTrackPtr->DSI)
                {
                    CLEANUPonERR(M4MP4W_putBlock(H263Block3, sizeof(H263Block3),
                        mMp4FileDataPtr->fileWriterFunctions,
                        fileWriterContext)); /*h263*/
                }
                else
                {
                    CLEANUPonERR(
                        M4MP4W_putBlock(mMp4FileDataPtr->videoTrackPtr->DSI,
                        mMp4FileDataPtr->videoTrackPtr->dsiSize,
                        mMp4FileDataPtr->fileWriterFunctions,
                        fileWriterContext));
                }
            }
        }

        if (bMP4V)
        {
            M4OSA_UInt32 bufferSizeDB = 5 * mMp4FileDataPtr->videoTrackPtr->
                avgBitrate; /*bufferSizeDB set to 5 times the bitrate*/

            CLEANUPonERR(M4MP4W_putBE32(v_esdsSize,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*mp4v*/
            CLEANUPonERR(M4MP4W_putBlock(MPEGConfigBlock0,
                sizeof(MPEGConfigBlock0), mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*mp4v*/
            CLEANUPonERR(M4MP4W_putByte(v_ESDescriptorSize,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*mp4v*/
            CLEANUPonERR(M4MP4W_putBlock(MPEGConfigBlock1,
                sizeof(MPEGConfigBlock1), mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*mp4v*/
            CLEANUPonERR(M4MP4W_putByte(v_DCDescriptorSize,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*mp4v*/
            CLEANUPonERR(M4MP4W_putBlock(Mp4vBlock3, sizeof(Mp4vBlock3),
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*mp4v*/
            CLEANUPonERR(M4MP4W_putBE24(bufferSizeDB,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*mp4v*/
            CLEANUPonERR(
                M4MP4W_putBE32(mMp4FileDataPtr->videoTrackPtr->maxBitrate,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*mp4v*/
            CLEANUPonERR(
                M4MP4W_putBE32(mMp4FileDataPtr->videoTrackPtr->avgBitrate,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*mp4v*/
            CLEANUPonERR(M4MP4W_putBlock(MPEGConfigBlock2,
                sizeof(MPEGConfigBlock2), mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*mp4v*/
            CLEANUPonERR(M4MP4W_putByte(mMp4FileDataPtr->videoTrackPtr->dsiSize,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*mp4v*/
            CLEANUPonERR(M4MP4W_putBlock(mMp4FileDataPtr->videoTrackPtr->DSI,
                mMp4FileDataPtr->videoTrackPtr->dsiSize,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*mp4v*/
            CLEANUPonERR(M4MP4W_putBlock(MPEGConfigBlock3,
                sizeof(MPEGConfigBlock3), mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*mp4v*/
        }

        if (bH264)
        {
            M4OSA_UInt16 ppsLentgh = 0; /* PPS length */
            M4OSA_UInt16 spsLentgh = 0; /* SPS length */
            M4OSA_UChar *tmpDSI = mMp4FileDataPtr->videoTrackPtr->DSI; /* DSI */
            M4OSA_UInt16 NumberOfPPS;
            M4OSA_UInt16 lCntPPS;

            /* Put the avcC (header + DSI) size */
            CLEANUPonERR(M4MP4W_putBE32(v_avcCSize,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*h264*/
            /* Put the avcC header */
            CLEANUPonERR(M4MP4W_putBlock(H264Block2, sizeof(H264Block2),
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*h264*/
            /* Put the DSI (SPS + PPS) int the 3gp format*/
            /* SPS length in BE */

            if ((0x01 != mMp4FileDataPtr->videoTrackPtr->DSI[0]) ||
                 (0x42 != mMp4FileDataPtr->videoTrackPtr->DSI[1]))
            {
                M4OSA_TRACE1_2("!!! M4MP4W_closeWrite ERROR : invalid AVCC 0x%X 0x%X",
                    mMp4FileDataPtr->videoTrackPtr->DSI[0],
                    mMp4FileDataPtr->videoTrackPtr->DSI[1]);
                return M4ERR_PARAMETER;
            }
            // Do not strip the DSI
            CLEANUPonERR( M4MP4W_putBlock(mMp4FileDataPtr->videoTrackPtr->DSI,
                mMp4FileDataPtr->videoTrackPtr->dsiSize,
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext) );/*h264*/

        }

        /*end trak*/
        CLEANUPonERR(M4MP4W_putBE32(v_stszSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock15, sizeof(CommonBlock15),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(
            mMp4FileDataPtr->videoTrackPtr->CommonData.sampleSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(
            M4MP4W_putBE32(mMp4FileDataPtr->videoTrackPtr->CommonData.sampleNb,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

        for ( i = 0; i < mMp4FileDataPtr->videoTrackPtr->CommonData.sampleNb;
            i++ )
        {
            CLEANUPonERR(
                M4MP4W_putBE32(mMp4FileDataPtr->videoTrackPtr->TABLE_STSZ[i],
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext)); /*video*/
        }

#else

        CLEANUPonERR(M4MP4W_putBlock((const M4OSA_UChar
            *)mMp4FileDataPtr->videoTrackPtr->TABLE_STSZ,
            mMp4FileDataPtr->videoTrackPtr->CommonData.sampleNb * 4,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*video*/

#endif

        CLEANUPonERR(M4MP4W_putBE32(v_stscSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock16, sizeof(CommonBlock16),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE

        CLEANUPonERR(M4MP4W_putBE32(mMp4FileDataPtr->videoTrackPtr->currentStsc
            + 1, mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

        for ( i = 0; i <= mMp4FileDataPtr->videoTrackPtr->currentStsc; i++ )
        {
            CLEANUPonERR(M4MP4W_putBE32(
                ( mMp4FileDataPtr->videoTrackPtr->chunkSampleNbTable[i]
            >> 12) + 1, mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext));
            CLEANUPonERR(M4MP4W_putBE32((mMp4FileDataPtr->videoTrackPtr->
                chunkSampleNbTable[i] & 0xFFF),
                mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
            CLEANUPonERR(M4MP4W_putBE32(1, mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext));
        }

#else

        CLEANUPonERR(M4MP4W_putBE32(mMp4FileDataPtr->videoTrackPtr->currentChunk
            + 1, mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

        for (i = 0; i <= mMp4FileDataPtr->videoTrackPtr->currentChunk; i++)
        {
            CLEANUPonERR(M4MP4W_putBE32(i + 1,
                mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
            CLEANUPonERR(M4MP4W_putBE32(
                mMp4FileDataPtr->videoTrackPtr->chunkSampleNbTable[i],
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext));
            CLEANUPonERR(M4MP4W_putBE32(1, mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext));
        }

#endif

        CLEANUPonERR(M4MP4W_putBE32(v_stcoSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBlock(CommonBlock17, sizeof(CommonBlock17),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
        CLEANUPonERR(M4MP4W_putBE32(mMp4FileDataPtr->videoTrackPtr->currentChunk
            + 1, mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

#ifdef _M4MP4W_MOOV_FIRST

        for (i = 0; i <= mMp4FileDataPtr->videoTrackPtr->currentChunk; i++)
        {
            if (( bInterleaveAV == M4OSA_TRUE)
                && (mMp4FileDataPtr->audioTrackPtr->currentChunk >= i))
            {
                v_trakOffset +=
                    mMp4FileDataPtr->audioTrackPtr->chunkSizeTable[i];
            }
            CLEANUPonERR(M4MP4W_putBE32(v_trakOffset,
                mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
            v_trakOffset += mMp4FileDataPtr->videoTrackPtr->chunkSizeTable[i];
        }

#else

        for ( i = 0; i <= mMp4FileDataPtr->videoTrackPtr->currentChunk; i++ )
        {
            CLEANUPonERR(M4MP4W_putBE32(
                mMp4FileDataPtr->videoTrackPtr->chunkOffsetTable[i],
                mMp4FileDataPtr->fileWriterFunctions,
                fileWriterContext));
        }

#endif                                                                 /*_M4MP4W_MOOV_FIRST*/

        CLEANUPonERR(M4MP4W_putBE32(v_stssSize,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*video*/
        CLEANUPonERR(M4MP4W_putBlock(VideoBlock4, sizeof(VideoBlock4),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*video*/
        CLEANUPonERR(
            M4MP4W_putBE32(mMp4FileDataPtr->videoTrackPtr->stssTableEntryNb,
            mMp4FileDataPtr->fileWriterFunctions,
            fileWriterContext)); /*video*/
        CLEANUPonERR(M4MP4W_putBlock((const M4OSA_UChar
            *)mMp4FileDataPtr->videoTrackPtr->TABLE_STSS,
            mMp4FileDataPtr->videoTrackPtr->stssTableEntryNb * 4,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*video*/
        CLEANUPonERR(M4MP4W_putBlock(VideoBlock5, sizeof(VideoBlock5),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext)); /*video*/
    }
#ifdef _M4MP4W_MOOV_FIRST
    /*mdat*/

    CLEANUPonERR(M4MP4W_putBE32(mdatSize, mMp4FileDataPtr->fileWriterFunctions,
        fileWriterContext));
    CLEANUPonERR(M4MP4W_putBlock(CommonBlock2, sizeof(CommonBlock2),
        mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

    /*write data, according to the interleave mode (default is not interleaved)*/
    if (bInterleaveAV == M4OSA_FALSE)
    {
        if (bAudio)
        {
            for ( i = 0; i <= mMp4FileDataPtr->audioTrackPtr->currentChunk;
                i++ )
            {
                CLEANUPonERR(
                    M4MP4W_putBlock(mMp4FileDataPtr->audioTrackPtr->Chunk[i],
                    mMp4FileDataPtr->audioTrackPtr->chunkSizeTable[i],
                    mMp4FileDataPtr->fileWriterFunctions,
                    fileWriterContext)); /*audio (previously a_dataSize)*/
            }
        }

        if (bVideo)
        {
            for ( i = 0; i <= mMp4FileDataPtr->videoTrackPtr->currentChunk;
                i++ )
            {
                CLEANUPonERR(
                    M4MP4W_putBlock(mMp4FileDataPtr->videoTrackPtr->Chunk[i],
                    mMp4FileDataPtr->videoTrackPtr->chunkSizeTable[i],
                    mMp4FileDataPtr->fileWriterFunctions,
                    fileWriterContext)); /*video (previously a_dataSize)*/
            }
        }
    }
    else /*in this mode, we have audio and video to interleave*/
    {
        for ( i = 0; i <= max(mMp4FileDataPtr->audioTrackPtr->currentChunk,
            mMp4FileDataPtr->videoTrackPtr->currentChunk); i++ )
        {
            if (i <= mMp4FileDataPtr->audioTrackPtr->currentChunk)
            {
                CLEANUPonERR(
                    M4MP4W_putBlock(mMp4FileDataPtr->audioTrackPtr->Chunk[i],
                    mMp4FileDataPtr->audioTrackPtr->chunkSizeTable[i],
                    mMp4FileDataPtr->fileWriterFunctions,
                    fileWriterContext)); /*audio (previously a_dataSize)*/
            }

            if (i <= mMp4FileDataPtr->videoTrackPtr->currentChunk)
            {
                CLEANUPonERR(
                    M4MP4W_putBlock(mMp4FileDataPtr->videoTrackPtr->Chunk[i],
                    mMp4FileDataPtr->videoTrackPtr->chunkSizeTable[i],
                    mMp4FileDataPtr->fileWriterFunctions,
                    fileWriterContext)); /*video (previously a_dataSize)*/
            }
        }
    }

#endif /*_M4MP4W_MOOV_FIRST*/

    /*skip*/

    CLEANUPonERR(M4MP4W_putBlock(BlockSignatureSkipHeader,
        sizeof(BlockSignatureSkipHeader), mMp4FileDataPtr->fileWriterFunctions,
        fileWriterContext));

    /* Write embedded string */
    if (mMp4FileDataPtr->embeddedString == M4OSA_NULL)
    {
        CLEANUPonERR(M4MP4W_putBlock(BlockSignatureSkipDefaultEmbeddedString,
            sizeof(BlockSignatureSkipDefaultEmbeddedString),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
    }
    else
    {
        CLEANUPonERR(M4MP4W_putBlock(mMp4FileDataPtr->embeddedString, 16,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
    }

    /* Write ves core version */
    camcoder_maj = (M4OSA_UChar)(mMp4FileDataPtr->camcoderVersion / 100);
    camcoder_min =
        (M4OSA_UChar)(( mMp4FileDataPtr->camcoderVersion - 100 * camcoder_maj)
        / 10);
    camcoder_rev =
        (M4OSA_UChar)(mMp4FileDataPtr->camcoderVersion - 100 * camcoder_maj - 10
        * camcoder_min);

    CLEANUPonERR(M4MP4W_putByte(' ', mMp4FileDataPtr->fileWriterFunctions,
        fileWriterContext));
    CLEANUPonERR(M4MP4W_putByte((M4OSA_UChar)(camcoder_maj + '0'),
        mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
    CLEANUPonERR(M4MP4W_putByte('.', mMp4FileDataPtr->fileWriterFunctions,
        fileWriterContext));
    CLEANUPonERR(M4MP4W_putByte((M4OSA_UChar)(camcoder_min + '0'),
        mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
    CLEANUPonERR(M4MP4W_putByte('.', mMp4FileDataPtr->fileWriterFunctions,
        fileWriterContext));
    CLEANUPonERR(M4MP4W_putByte((M4OSA_UChar)(camcoder_rev + '0'),
        mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

    /* Write integration tag */
    CLEANUPonERR(M4MP4W_putBlock((const M4OSA_UChar *)" -- ", 4,
        mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));

    if (mMp4FileDataPtr->integrationTag == M4OSA_NULL)
    {
        CLEANUPonERR(M4MP4W_putBlock(BlockSignatureSkipDefaultIntegrationTag,
            sizeof(BlockSignatureSkipDefaultIntegrationTag),
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
    }
    else
    {
        CLEANUPonERR(M4MP4W_putBlock(mMp4FileDataPtr->integrationTag, 60,
            mMp4FileDataPtr->fileWriterFunctions, fileWriterContext));
    }

#ifndef _M4MP4W_MOOV_FIRST
    /*overwrite mdat size*/

    if (mMp4FileDataPtr->ftyp.major_brand != 0)
        mdatPos= 16 + mMp4FileDataPtr->ftyp.nbCompatibleBrands * 4;
    else
        mdatPos = 24;

    moovPos = moovPos - mdatPos;
    mdatSize = moovPos;

    CLEANUPonERR(mMp4FileDataPtr->fileWriterFunctions->seek(fileWriterContext,
        M4OSA_kFileSeekBeginning, &mdatPos)); /*seek after ftyp...*/
    CLEANUPonERR(M4MP4W_putBE32(mdatSize, mMp4FileDataPtr->fileWriterFunctions,
        fileWriterContext));

#endif                                        /*_M4MP4W_MOOV_FIRST*/

cleanup:

    /**
    * Close the file even if an error occured */
    if (M4OSA_NULL != mMp4FileDataPtr->fileWriterContext)
    {
        err2 =
            mMp4FileDataPtr->fileWriterFunctions->closeWrite(mMp4FileDataPtr->
            fileWriterContext); /**< close the stream anyway */

        if (M4NO_ERROR != err2)
        {
            M4OSA_TRACE1_1(
                "M4MP4W_closeWrite: fileWriterFunctions->closeWrite returns 0x%x",
                err2);
        }
        mMp4FileDataPtr->fileWriterContext = M4OSA_NULL;
    }

#ifdef _M4MP4W_RESERVED_MOOV_DISK_SPACE
    /* Remove safety file if still present (here it is cleanup in case of error and NOT the normal
    removal of the safety file to free emergency space for the moov). */

    if (M4OSA_TRUE == mMp4FileDataPtr->cleanSafetyFile)
    {
        M4OSA_Context tempContext;
        err3 = mMp4FileDataPtr->fileWriterFunctions->openWrite(&tempContext,
            mMp4FileDataPtr->safetyFileUrl,
            M4OSA_kFileWrite | M4OSA_kFileCreate);

        if (M4NO_ERROR != err2)
            err2 = err3;

        if (M4NO_ERROR
            != err3) /* No sense closing if we couldn't open in the first place. */
        {
            err3 =
                mMp4FileDataPtr->fileWriterFunctions->closeWrite(tempContext);

            if (M4NO_ERROR != err2)
                err2 = err3;
        }
        mMp4FileDataPtr->safetyFileUrl = M4OSA_NULL;
        mMp4FileDataPtr->cleanSafetyFile = M4OSA_FALSE;
    }

#endif /* _M4MP4W_RESERVED_MOOV_DISK_SPACE */

    /* Delete embedded string */

    if (M4OSA_NULL != mMp4FileDataPtr->embeddedString)
    {
        free(mMp4FileDataPtr->embeddedString);
        mMp4FileDataPtr->embeddedString = M4OSA_NULL;
    }

    /* Delete integration tag */
    if (M4OSA_NULL != mMp4FileDataPtr->integrationTag)
    {
        free(mMp4FileDataPtr->integrationTag);
        mMp4FileDataPtr->integrationTag = M4OSA_NULL;
    }

    /**
    * M4MP4W_freeContext() is now a private method, called only from here*/
    err3 = M4MP4W_freeContext(context);

    if (M4NO_ERROR != err3)
    {
        M4OSA_TRACE1_1("M4MP4W_closeWrite: M4MP4W_freeContext returns 0x%x",
            err3);
    }

    /**
    * Choose which error code to return */
    if (M4NO_ERROR != err)
    {
        /**
        * We give priority to main error */
        M4OSA_TRACE1_1("M4MP4W_closeWrite: returning err=0x%x", err);
        return err;
    }
    else if (M4NO_ERROR != err2)
    {
        /**
        * Error from closeWrite is returned if there is no main error */
        M4OSA_TRACE1_1("M4MP4W_closeWrite: returning err2=0x%x", err2);
        return err2;
    }
    else
    {
        /**
        * Error from M4MP4W_freeContext is returned only if there is no main error and
          no close error */
        M4OSA_TRACE1_1("M4MP4W_closeWrite: returning err3=0x%x", err3);
        return err3;
    }
}

/*******************************************************************************/
M4OSA_ERR M4MP4W_getOption( M4OSA_Context context, M4OSA_OptionID option,
                           M4OSA_DataOption *valuePtr )
/*******************************************************************************/
{
    M4OSA_ERR err = M4NO_ERROR;

    M4SYS_StreamIDValue *streamIDvaluePtr = M4OSA_NULL;
    M4MP4W_StreamIDsize *streamIDsizePtr = M4OSA_NULL;
    M4MP4W_memAddr *memAddrPtr = M4OSA_NULL;
    /*    M4MP4W_WriteCallBack*    callBackPtr = M4OSA_NULL;*/

    M4MP4W_Mp4FileData *mMp4FileDataPtr = (M4MP4W_Mp4FileData *)context;
    ERR_CHECK(context != M4OSA_NULL, M4ERR_PARAMETER);

    ERR_CHECK(( mMp4FileDataPtr->state == M4MP4W_opened)
        || (mMp4FileDataPtr->state == M4MP4W_ready), M4ERR_STATE);

    switch( option )
    {
        case (M4MP4W_maxAUperChunk):
            return M4ERR_NOT_IMPLEMENTED;

        case (M4MP4W_maxChunkSize):

            streamIDvaluePtr = (M4SYS_StreamIDValue *)(*valuePtr);

            switch( streamIDvaluePtr->streamID )
            {
                case (AudioStreamID):
                    if (mMp4FileDataPtr->hasAudio == M4OSA_FALSE)
                        return M4ERR_BAD_STREAM_ID;
                    else
                        streamIDvaluePtr->value =
                        mMp4FileDataPtr->audioTrackPtr->MaxChunkSize;
                    break;

                case (VideoStreamID):
                    if (mMp4FileDataPtr->hasVideo == M4OSA_FALSE)
                        return M4ERR_BAD_STREAM_ID;
                    else
                        streamIDvaluePtr->value =
                        mMp4FileDataPtr->videoTrackPtr->MaxChunkSize;
                    break;

                case (0): /*all streams*/
                    streamIDvaluePtr->value = mMp4FileDataPtr->MaxChunkSize;
                    break;

                default:
                    return M4ERR_BAD_STREAM_ID;
        }

        break;

    case (M4MP4W_maxChunkInter):

        streamIDvaluePtr = (M4SYS_StreamIDValue *)(*valuePtr);

        switch( streamIDvaluePtr->streamID )
        {
            case (0): /*all streams*/
                streamIDvaluePtr->value = (M4OSA_UInt32)mMp4FileDataPtr->
                    InterleaveDur; /*time conversion !*/
                break;

            default:
                return M4ERR_BAD_STREAM_ID;
        }
        break;

    case (M4MP4W_embeddedString):
        memAddrPtr = (M4MP4W_memAddr *)(*valuePtr);
        /*memAddrPtr must have been already allocated by the caller
        and memAddrPtr->size initialized with the max possible length in bytes*/
        ERR_CHECK(memAddrPtr->size >= 16, M4ERR_PARAMETER);
        ERR_CHECK(memAddrPtr->addr != M4OSA_NULL, M4ERR_PARAMETER);
        /*memAddrPtr->size is updated with the actual size of the string*/
        memAddrPtr->size = 16;
        /*if no value was set, return the default string */
        if (mMp4FileDataPtr->embeddedString != M4OSA_NULL)
            memcpy((void *)memAddrPtr->addr,
            (void *)mMp4FileDataPtr->embeddedString, 16);
        else
            memcpy((void *)memAddrPtr->addr,
            (void *)BlockSignatureSkipDefaultEmbeddedString,
            16);
        break;

    case (M4MP4W_integrationTag):
        memAddrPtr = (M4MP4W_memAddr *)(*valuePtr);
        /*memAddrPtr must have been already allocated by the caller
        and memAddrPtr->size initialized with the max possible length in bytes*/
        ERR_CHECK(memAddrPtr->size >= 60, M4ERR_PARAMETER);
        ERR_CHECK(memAddrPtr->addr != M4OSA_NULL, M4ERR_PARAMETER);
        /*memAddrPtr->size is updated with the actual size of the string*/
        memAddrPtr->size = 60;
        /*if no value was set, return the default string 0 */
        if (mMp4FileDataPtr->integrationTag != M4OSA_NULL)
            memcpy((void *)memAddrPtr->addr,
            (void *)mMp4FileDataPtr->integrationTag, 60);
        else
            memcpy((void *)memAddrPtr->addr,
            (void *)BlockSignatureSkipDefaultIntegrationTag,
            60);
        break;

    case (M4MP4W_CamcoderVersion):

        streamIDvaluePtr = (M4SYS_StreamIDValue *)(*valuePtr);

        switch( streamIDvaluePtr->streamID )
        {
            case (0): /*all streams*/
                streamIDvaluePtr->value = mMp4FileDataPtr->camcoderVersion;
                break;

            default:
                return M4ERR_BAD_STREAM_ID;
        }
        break;

    case (M4MP4W_preWriteCallBack):
        return M4ERR_NOT_IMPLEMENTED;
        /*callBackPtr = (M4MP4W_WriteCallBack*)(*valuePtr);
        *callBackPtr = mMp4FileDataPtr->PreWriteCallBack;
        break;*/

    case (M4MP4W_postWriteCallBack):
        return M4ERR_NOT_IMPLEMENTED;
        /*callBackPtr = (M4MP4W_WriteCallBack*)(*valuePtr);
        *callBackPtr = mMp4FileDataPtr->PostWriteCallBack;
        break;*/

    case (M4MP4W_maxAUsize):

        streamIDvaluePtr = (M4SYS_StreamIDValue *)(*valuePtr);

        switch( streamIDvaluePtr->streamID )
        {
            case (AudioStreamID):
                if (mMp4FileDataPtr->hasAudio == M4OSA_FALSE)
                    return M4ERR_BAD_STREAM_ID;
                else
                    streamIDvaluePtr->value =
                    mMp4FileDataPtr->audioTrackPtr->MaxAUSize;
                break;

            case (VideoStreamID):
                if (mMp4FileDataPtr->hasVideo == M4OSA_FALSE)
                    return M4ERR_BAD_STREAM_ID;
                else
                    streamIDvaluePtr->value =
                    mMp4FileDataPtr->videoTrackPtr->MaxAUSize;
                break;

            case (0): /*all streams*/
                streamIDvaluePtr->value = mMp4FileDataPtr->MaxAUSize;
                break;

            default:
                return M4ERR_BAD_STREAM_ID;
        }

        break;

    case (M4MP4W_IOD):
        return M4ERR_NOT_IMPLEMENTED;

    case (M4MP4W_ESD):
        return M4ERR_NOT_IMPLEMENTED;

    case (M4MP4W_SDP):
        return M4ERR_NOT_IMPLEMENTED;

    case (M4MP4W_trackSize):
        streamIDsizePtr = (M4MP4W_StreamIDsize *)(*valuePtr);
        streamIDsizePtr->width = mMp4FileDataPtr->videoTrackPtr->width;
        streamIDsizePtr->height = mMp4FileDataPtr->videoTrackPtr->height;
        break;

    case (M4MP4W_estimateAudioSize):
        streamIDvaluePtr = (M4SYS_StreamIDValue *)(*valuePtr);
        streamIDvaluePtr->value =
            (M4OSA_UInt32)mMp4FileDataPtr->estimateAudioSize;
        break;

    case (M4MP4W_MOOVfirst):
        return M4ERR_NOT_IMPLEMENTED;

    case (M4MP4W_V2_MOOF):
        return M4ERR_NOT_IMPLEMENTED;

    case (M4MP4W_V2_tblCompres):
        return M4ERR_NOT_IMPLEMENTED;

    default:
        return M4ERR_BAD_OPTION_ID;
    }

    return err;
}

/*******************************************************************************/
M4OSA_ERR M4MP4W_setOption( M4OSA_Context context, M4OSA_OptionID option,
                           M4OSA_DataOption value )
/*******************************************************************************/
{
    M4OSA_ERR err = M4NO_ERROR;

    M4SYS_StreamIDValue *streamIDvaluePtr = M4OSA_NULL;
    M4MP4W_StreamIDsize *streamIDsizePtr = M4OSA_NULL;
    M4MP4W_memAddr *memAddrPtr = M4OSA_NULL;
    M4SYS_StreamIDmemAddr *streamIDmemAddrPtr;

    M4MP4W_Mp4FileData *mMp4FileDataPtr = (M4MP4W_Mp4FileData *)context;
    ERR_CHECK(context != M4OSA_NULL, M4ERR_PARAMETER);

    /* Verify state */
    switch( option )
    {
        case M4MP4W_maxFileDuration:
        case M4MP4W_DSI:
            /* this param can be set at the end of a recording */
            ERR_CHECK((mMp4FileDataPtr->state != M4MP4W_closed), M4ERR_STATE);
            break;

        case M4MP4W_setFtypBox:
            /* this param can only be set before starting any write */
            ERR_CHECK(mMp4FileDataPtr->state == M4MP4W_opened, M4ERR_STATE);
            break;

        default:
            /* in general params can be set at open or ready stage */
            ERR_CHECK(( mMp4FileDataPtr->state == M4MP4W_opened)
                || (mMp4FileDataPtr->state == M4MP4W_ready), M4ERR_STATE);
    }

    /* Set option */
    switch( option )
    {
        case (M4MP4W_maxAUperChunk):
            return M4ERR_NOT_IMPLEMENTED;

        case (M4MP4W_maxChunkSize):

            streamIDvaluePtr = (M4SYS_StreamIDValue *)value;

            switch( streamIDvaluePtr->streamID )
            {
                case (AudioStreamID):
                    if (mMp4FileDataPtr->hasAudio == M4OSA_FALSE)
                        return
                        M4ERR_BAD_STREAM_ID; /*maybe the stream has not been added yet*/
                    else
                    {
                        mMp4FileDataPtr->audioTrackPtr->MaxChunkSize =
                            streamIDvaluePtr->value;
                    }

                    break;

                case (VideoStreamID):
                    if (mMp4FileDataPtr->hasVideo == M4OSA_FALSE)
                        return
                        M4ERR_BAD_STREAM_ID; /*maybe the stream has not been added yet*/
                    else
                    {
                        mMp4FileDataPtr->videoTrackPtr->MaxChunkSize =
                            streamIDvaluePtr->value;
                    }
                    break;

                case (0): /*all streams*/

                    /*In M4MP4W_opened state, no stream is present yet, so only global value
                    needs to be updated.*/
                    mMp4FileDataPtr->MaxChunkSize = streamIDvaluePtr->value;

                    if (mMp4FileDataPtr->hasAudio == M4OSA_TRUE)
                    {
                        mMp4FileDataPtr->audioTrackPtr->MaxChunkSize =
                            streamIDvaluePtr->value;
                    }

                    if (mMp4FileDataPtr->hasVideo == M4OSA_TRUE)
                    {
                        mMp4FileDataPtr->videoTrackPtr->MaxChunkSize =
                            streamIDvaluePtr->value;
                    }
                    break;

                default:
                    return M4ERR_BAD_STREAM_ID;
            }
            break;

        case (M4MP4W_maxChunkInter):

            streamIDvaluePtr = (M4SYS_StreamIDValue *)value;

            switch( streamIDvaluePtr->streamID )
            {
                case (0):                                       /*all streams*/
                    mMp4FileDataPtr->InterleaveDur =
                        (M4MP4W_Time32)streamIDvaluePtr->
                        value; /*time conversion!*/
                    break;

                default:
                    return M4ERR_BAD_STREAM_ID;
                    /*not meaningfull to set this parameter on a streamID basis*/
            }
            break;

        case (M4MP4W_maxFileSize):
            mMp4FileDataPtr->MaxFileSize = *(M4OSA_UInt32 *)value;
            break;

        case (M4MP4W_embeddedString):
            memAddrPtr = (M4MP4W_memAddr *)value;
            /*
            * If memAddrPtr->size > 16 bytes, then the string will be truncated.
            * If memAddrPtr->size < 16 bytes, then return M4ERR_PARAMETER
            */
            ERR_CHECK(memAddrPtr->size >= 16, M4ERR_PARAMETER);

            if (mMp4FileDataPtr->embeddedString == M4OSA_NULL)
            {
                mMp4FileDataPtr->embeddedString =
                    (M4OSA_UChar *)M4OSA_32bitAlignedMalloc(16, M4MP4_WRITER,
                    (M4OSA_Char *)"embeddedString");
                ERR_CHECK(mMp4FileDataPtr->embeddedString != M4OSA_NULL,
                    M4ERR_ALLOC);
            }
            /*else, just overwrite the previously set string*/
            memcpy((void *)mMp4FileDataPtr->embeddedString,
                (void *)memAddrPtr->addr, 16);
            break;

        case (M4MP4W_integrationTag):
            memAddrPtr = (M4MP4W_memAddr *)value;
            /*
            * If memAddrPtr->size > 60 bytes, then the string will be truncated.
            * If memAddrPtr->size < 60 bytes, then pad with 0
            */
            if (mMp4FileDataPtr->integrationTag == M4OSA_NULL)
            {
                mMp4FileDataPtr->integrationTag =
                    (M4OSA_UChar *)M4OSA_32bitAlignedMalloc(60, M4MP4_WRITER,
                    (M4OSA_Char *)"integrationTag");
                ERR_CHECK(mMp4FileDataPtr->integrationTag != M4OSA_NULL,
                    M4ERR_ALLOC);
            }
            /*else, just overwrite the previously set string*/
            if (memAddrPtr->size < 60)
            {
                memcpy((void *)mMp4FileDataPtr->integrationTag,
                    (void *)BlockSignatureSkipDefaultIntegrationTag,
                    60);
                memcpy((void *)mMp4FileDataPtr->integrationTag,
                    (void *)memAddrPtr->addr, memAddrPtr->size);
            }
            else
            {
                memcpy((void *)mMp4FileDataPtr->integrationTag,
                    (void *)memAddrPtr->addr, 60);
            }
            break;

        case (M4MP4W_CamcoderVersion):

            streamIDvaluePtr = (M4SYS_StreamIDValue *)value;

            switch( streamIDvaluePtr->streamID )
            {
                case (0): /*all streams*/
                    mMp4FileDataPtr->camcoderVersion = streamIDvaluePtr->value;
                    break;

                default:
                    return M4ERR_BAD_STREAM_ID;
                    /*not meaningfull to set this parameter on a streamID basis*/
            }
            break;

        case (M4MP4W_preWriteCallBack):
            return M4ERR_NOT_IMPLEMENTED;
            /*mMp4FileDataPtr->PreWriteCallBack = *(M4MP4W_WriteCallBack*)value;
            break;*/

        case (M4MP4W_postWriteCallBack):
            return M4ERR_NOT_IMPLEMENTED;
            /*mMp4FileDataPtr->PostWriteCallBack = *(M4MP4W_WriteCallBack*)value;
            break;*/

        case (M4MP4W_maxAUsize):

            streamIDvaluePtr = (M4SYS_StreamIDValue *)value;

            switch( streamIDvaluePtr->streamID )
            {
                case (AudioStreamID):

                    /*if (mMp4FileDataPtr->audioTrackPtr == M4OSA_NULL)*/
                    if (mMp4FileDataPtr->hasAudio == M4OSA_FALSE)
                        return M4ERR_BAD_STREAM_ID;
                    else
                        mMp4FileDataPtr->audioTrackPtr->MaxAUSize =
                        streamIDvaluePtr->value;
                    break;

                case (VideoStreamID):

                    /*if (mMp4FileDataPtr->videoTrackPtr == M4OSA_NULL)*/
                    if (mMp4FileDataPtr->hasVideo == M4OSA_FALSE)
                        return M4ERR_BAD_STREAM_ID;
                    else
                        mMp4FileDataPtr->videoTrackPtr->MaxAUSize =
                        streamIDvaluePtr->value;
                    break;

                case (0): /*all streams*/

                    mMp4FileDataPtr->MaxAUSize = streamIDvaluePtr->value;

                    if (mMp4FileDataPtr->hasAudio == M4OSA_TRUE)
                        mMp4FileDataPtr->audioTrackPtr->MaxAUSize =
                        streamIDvaluePtr->value;

                    if (mMp4FileDataPtr->hasVideo == M4OSA_TRUE)
                        mMp4FileDataPtr->videoTrackPtr->MaxAUSize =
                        streamIDvaluePtr->value;

                    break;

                default:
                    return M4ERR_BAD_STREAM_ID;
            }
            break;

        case (M4MP4W_IOD):
            return M4ERR_NOT_IMPLEMENTED;

        case (M4MP4W_ESD):
            return M4ERR_NOT_IMPLEMENTED;

        case (M4MP4W_SDP):
            return M4ERR_NOT_IMPLEMENTED;

        case (M4MP4W_trackSize):

            streamIDsizePtr = (M4MP4W_StreamIDsize *)value;

            if ((streamIDsizePtr->streamID != VideoStreamID)
                || (mMp4FileDataPtr->hasVideo == M4OSA_FALSE))
                return M4ERR_BAD_STREAM_ID;
            else
            {
                mMp4FileDataPtr->videoTrackPtr->width = streamIDsizePtr->width;
                mMp4FileDataPtr->videoTrackPtr->height =
                    streamIDsizePtr->height;
            }
            break;

        case (M4MP4W_estimateAudioSize):

            streamIDvaluePtr = (M4SYS_StreamIDValue *)value;

            /*shall not set this option before audio and video streams were added*/
            /*nonsense to set this option if not in case audio+video*/
            if ((mMp4FileDataPtr->hasAudio == M4OSA_FALSE)
                || (mMp4FileDataPtr->hasVideo == M4OSA_FALSE))
                return M4ERR_STATE;

            mMp4FileDataPtr->estimateAudioSize =
                (M4OSA_Bool)streamIDvaluePtr->value;
            break;

        case (M4MP4W_MOOVfirst):
            return M4ERR_NOT_IMPLEMENTED;

        case (M4MP4W_V2_MOOF):
            return M4ERR_NOT_IMPLEMENTED;

        case (M4MP4W_V2_tblCompres):
            return M4ERR_NOT_IMPLEMENTED;

        case (M4MP4W_maxFileDuration):
            mMp4FileDataPtr->MaxFileDuration = *(M4OSA_UInt32 *)value;
            break;

        case (M4MP4W_setFtypBox):
            {
                M4OSA_UInt32 size;

                ERR_CHECK(( (M4MP4C_FtypBox *)value)->major_brand != 0,
                    M4ERR_PARAMETER);

                /* Copy structure */
                mMp4FileDataPtr->ftyp = *(M4MP4C_FtypBox *)value;

                /* Update global position variables with the difference between common and
                 user block */
                size =
                    mMp4FileDataPtr->ftyp.nbCompatibleBrands * sizeof(M4OSA_UInt32);

                mMp4FileDataPtr->absoluteCurrentPos = 8/*mdat*/ + 16 + size;
                mMp4FileDataPtr->filesize = 218/*mdat+moov+skip*/ + 16 + size;
            }
            break;

        case (M4MP4W_DSI):
            {
                streamIDmemAddrPtr = (M4SYS_StreamIDmemAddr *)value;

                /* Nested switch! Whee! */
                switch( streamIDmemAddrPtr->streamID )
                {
                    case (AudioStreamID):
                        return M4ERR_NOT_IMPLEMENTED;

                    case (VideoStreamID):

                        /* Protect DSI setting : only once allowed on a given stream */

                        switch( mMp4FileDataPtr->videoTrackPtr->
                            CommonData.trackType )
                        {
                            case M4SYS_kH263:
                                if ((0 != mMp4FileDataPtr->videoTrackPtr->dsiSize)
                                    || (M4OSA_NULL
                                    != mMp4FileDataPtr->videoTrackPtr->DSI))
                                {
                                    M4OSA_TRACE1_0(
                                        "M4MP4W_setOption: dsi already set !");
                                    return M4ERR_STATE;
                                }

                                if ((0 == streamIDmemAddrPtr->size)
                                    || (M4OSA_NULL == streamIDmemAddrPtr->addr))
                                {
                                    M4OSA_TRACE1_0(
                                        "M4MP4W_setOption: Bad H263 dsi!");
                                    return M4ERR_PARAMETER;
                                }

                                /*decoder specific info size is supposed to be always 7
                                 bytes long */
                                ERR_CHECK(streamIDmemAddrPtr->size == 7,
                                    M4ERR_PARAMETER);
                                mMp4FileDataPtr->videoTrackPtr->dsiSize =
                                    (M4OSA_UInt8)streamIDmemAddrPtr->size;
                                mMp4FileDataPtr->videoTrackPtr->DSI = (M4OSA_UChar
                                    *)M4OSA_32bitAlignedMalloc(streamIDmemAddrPtr->size,
                                    M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->DSI");
                                ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->DSI
                                    != M4OSA_NULL, M4ERR_ALLOC);
                                memcpy(
                                    (void *)mMp4FileDataPtr->videoTrackPtr->
                                    DSI,
                                    (void *)streamIDmemAddrPtr->addr,
                                    streamIDmemAddrPtr->size);

                                break;

                            case M4SYS_kMPEG_4:
                                if ((0 != mMp4FileDataPtr->videoTrackPtr->dsiSize)
                                    || (M4OSA_NULL
                                    != mMp4FileDataPtr->videoTrackPtr->DSI))
                                {
                                    M4OSA_TRACE1_0(
                                        "M4MP4W_setOption: dsi already set !");
                                    return M4ERR_STATE;
                                }

                                if ((0 == streamIDmemAddrPtr->size)
                                    || (M4OSA_NULL == streamIDmemAddrPtr->addr))
                                {
                                    M4OSA_TRACE1_0(
                                        "M4MP4W_setOption: Bad MPEG4 dsi!");
                                    return M4ERR_PARAMETER;
                                }

                                /*MP4V specific*/
                                ERR_CHECK(streamIDmemAddrPtr->size < 105,
                                    M4ERR_PARAMETER);
                                mMp4FileDataPtr->videoTrackPtr->dsiSize =
                                    (M4OSA_UInt8)streamIDmemAddrPtr->size;
                                mMp4FileDataPtr->videoTrackPtr->DSI = (M4OSA_UChar
                                    *)M4OSA_32bitAlignedMalloc(streamIDmemAddrPtr->size,
                                    M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->DSI");
                                ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->DSI
                                    != M4OSA_NULL, M4ERR_ALLOC);
                                memcpy(
                                    (void *)mMp4FileDataPtr->videoTrackPtr->
                                    DSI,
                                    (void *)streamIDmemAddrPtr->addr,
                                    streamIDmemAddrPtr->size);
                                mMp4FileDataPtr->filesize +=
                                    streamIDmemAddrPtr->size;

                                break;

                            case M4SYS_kH264:
                                if ((0 != mMp4FileDataPtr->videoTrackPtr->dsiSize)
                                    || (M4OSA_NULL
                                    != mMp4FileDataPtr->videoTrackPtr->DSI))
                                {
                                    /* + H.264 trimming */
                                    if (M4OSA_TRUE == mMp4FileDataPtr->bMULPPSSPS)
                                    {
                                        free(mMp4FileDataPtr->videoTrackPtr->DSI);

                                        // Do not strip the DSI
                                        /* Store the DSI size */
                                        mMp4FileDataPtr->videoTrackPtr->dsiSize =
                                            (M4OSA_UInt8)streamIDmemAddrPtr->size;
                                             M4OSA_TRACE1_1("M4MP4W_setOption: in set option DSI size =%d"\
                                            ,mMp4FileDataPtr->videoTrackPtr->dsiSize);
                                        /* Copy the DSI (SPS + PPS) */
                                        mMp4FileDataPtr->videoTrackPtr->DSI =
                                            (M4OSA_UChar*)M4OSA_32bitAlignedMalloc(
                                            streamIDmemAddrPtr->size, M4MP4_WRITER,
                                            (M4OSA_Char *)"videoTrackPtr->DSI");
                                        ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->DSI !=
                                             M4OSA_NULL, M4ERR_ALLOC);
                                        memcpy(
                                            (void *)mMp4FileDataPtr->videoTrackPtr->DSI,
                                            (void *)streamIDmemAddrPtr->addr,
                                            streamIDmemAddrPtr->size);

                                        break;
                                        /* - H.264 trimming */
                                    }
                                    else
                                    {
                                        M4OSA_TRACE1_0(
                                            "M4MP4W_setOption: dsi already set !");
                                        return M4ERR_STATE;
                                    }
                                }

                                if (( 0 == streamIDmemAddrPtr->size)
                                    || (M4OSA_NULL == streamIDmemAddrPtr->addr))
                                {
                                    M4OSA_TRACE1_0(
                                        "M4MP4W_setOption: Bad H264 dsi!");
                                    return M4ERR_PARAMETER;
                                }

                                /* Store the DSI size */
                                mMp4FileDataPtr->videoTrackPtr->dsiSize =
                                    (M4OSA_UInt8)streamIDmemAddrPtr->size;

                                /* Copy the DSI (SPS + PPS) */
                                mMp4FileDataPtr->videoTrackPtr->DSI = (M4OSA_UChar
                                    *)M4OSA_32bitAlignedMalloc(streamIDmemAddrPtr->size,
                                    M4MP4_WRITER, (M4OSA_Char *)"videoTrackPtr->DSI");
                                ERR_CHECK(mMp4FileDataPtr->videoTrackPtr->DSI
                                    != M4OSA_NULL, M4ERR_ALLOC);
                                memcpy(
                                    (void *)mMp4FileDataPtr->videoTrackPtr->
                                    DSI,
                                    (void *)streamIDmemAddrPtr->addr,
                                    streamIDmemAddrPtr->size);
                                break;

                            default:
                                return M4ERR_BAD_STREAM_ID;
                        }
                    break;

                default:
                    return M4ERR_BAD_STREAM_ID;
                }
            }
            break;
            /* H.264 Trimming  */
        case M4MP4W_MUL_PPS_SPS:
            mMp4FileDataPtr->bMULPPSSPS = *(M4OSA_Int8 *)value;
            /* H.264 Trimming  */
            break;

        default:
            return M4ERR_BAD_OPTION_ID;
    }

    return err;
}

/*******************************************************************************/
M4OSA_ERR M4MP4W_getState( M4OSA_Context context, M4MP4W_State *state,
                          M4SYS_StreamID streamID )
/*******************************************************************************/
{
    M4OSA_ERR err = M4NO_ERROR;

    M4MP4W_Mp4FileData *mMp4FileDataPtr = (M4MP4W_Mp4FileData *)context;
    ERR_CHECK(context != M4OSA_NULL, M4ERR_PARAMETER);

    switch( streamID )
    {
        case (0):
            *state = mMp4FileDataPtr->state;
            break;

        case (AudioStreamID):
            if (mMp4FileDataPtr->hasAudio == M4OSA_TRUE)
            {
                *state = mMp4FileDataPtr->audioTrackPtr->microState;
            }
            else
            {
                return M4ERR_BAD_STREAM_ID;
            }
            break;

        case (VideoStreamID):
            if (mMp4FileDataPtr->hasVideo == M4OSA_TRUE)
            {
                *state = mMp4FileDataPtr->videoTrackPtr->microState;
            }
            else
            {
                return M4ERR_BAD_STREAM_ID;
            }
            break;

        default:
            return M4ERR_BAD_STREAM_ID;
    }

    return err;
}

/*******************************************************************************/
M4OSA_ERR M4MP4W_getCurrentFileSize( M4OSA_Context context,
                                    M4OSA_UInt32 *pCurrentFileSize )
/*******************************************************************************/
{
    M4OSA_ERR err = M4NO_ERROR;

    M4MP4W_Mp4FileData *mMp4FileDataPtr = (M4MP4W_Mp4FileData *)context;
    ERR_CHECK(context != M4OSA_NULL, M4ERR_PARAMETER);

    ERR_CHECK(pCurrentFileSize != M4OSA_NULL, M4ERR_PARAMETER);
    *pCurrentFileSize = mMp4FileDataPtr->filesize;

    return err;
}

#endif /* _M4MP4W_USE_CST_MEMORY_WRITER */
