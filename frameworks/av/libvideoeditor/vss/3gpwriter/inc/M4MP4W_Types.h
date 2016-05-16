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
 * @file    M4MP4W_Types.h
 * @brief   Definition of types for the core MP4 writer
 ******************************************************************************
 */

#ifndef M4MP4W_TYPES_H
#define M4MP4W_TYPES_H

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#include "NXPSW_CompilerSwitches.h"

#ifndef _M4MP4W_USE_CST_MEMORY_WRITER

/* includes */
#include "M4OSA_Types.h"
#include "M4OSA_FileWriter.h"
#include "M4OSA_FileReader.h"
#include "M4SYS_Stream.h"

/**
 ******************************************************************************
 * structure    M4MP4C_FtypBox
 * @brief       Information to build the 'ftyp' atom
 ******************************************************************************
 */
#define M4MPAC_FTYP_TAG 0x66747970 /* 'ftyp' */
#define M4MPAC_MAX_COMPATIBLE_BRANDS 10
typedef struct
{
    /* All brand fields are actually char[4] stored in big-endian integer format */

    M4OSA_UInt32    major_brand;         /* generally '3gp4'            */
    M4OSA_UInt32    minor_version;       /* generally '0000' or 'x.x '  */
    M4OSA_UInt32    nbCompatibleBrands;  /* number of compatible brands */
    M4OSA_UInt32    compatible_brands[M4MPAC_MAX_COMPATIBLE_BRANDS];   /* array of max compatible
                                                                       brands */
} M4MP4C_FtypBox;


/**
 ******************************************************************************
 * structure    M4MP4W_memAddr
 * @brief        Buffer structure for the MP4 writer
 ******************************************************************************
 */
typedef struct
{
    M4OSA_UInt32        size;
    M4OSA_MemAddr32    addr;
} M4MP4W_memAddr;

/**
 ******************************************************************************
 * Time type for the core MP4 writer
 ******************************************************************************
 */
typedef M4OSA_UInt32 M4MP4W_Time32;

/**
 ******************************************************************************
 * enumeration   M4MP4W_State
 * @brief        This enum defines the core MP4 writer states
 * @note         These states are used internaly, but can be retrieved from outside
 *               the writer.
 ******************************************************************************
 */
typedef enum
{
    M4MP4W_opened            = 0x100,
    M4MP4W_ready             = 0x200,
    M4MP4W_writing           = 0x300,
    M4MP4W_writing_startAU   = 0x301,
    M4MP4W_closed            = 0x400
} M4MP4W_State;

/**
 ******************************************************************************
 * enumeration    M4MP4W_OptionID
 * @brief        This enum defines the core MP4 writer options
 * @note        These options give parameters for the core MP4 writer
 ******************************************************************************
 */
typedef enum
{
    M4MP4W_maxAUperChunk        = 0xC101,
    M4MP4W_maxChunkSize         = 0xC102,
    M4MP4W_maxChunkInter        = 0xC103,
    M4MP4W_preWriteCallBack     = 0xC104,
    M4MP4W_postWriteCallBack    = 0xC105,
    M4MP4W_maxAUsize            = 0xC106,
    M4MP4W_IOD                  = 0xC111,
    M4MP4W_ESD                  = 0xC112,
    M4MP4W_SDP                  = 0xC113,
    M4MP4W_trackSize            = 0xC114,
    M4MP4W_MOOVfirst            = 0xC121,
    M4MP4W_V2_MOOF              = 0xC131,
    M4MP4W_V2_tblCompres        = 0xC132,
    /*warning: unspecified options:*/
    M4MP4W_maxFileSize          = 0xC152,
    M4MP4W_CamcoderVersion      = 0xC153, /*000 to 999 !*/
    M4MP4W_estimateAudioSize    = 0xC154, /*audio AUs are processed after the video, */
    /*this option MUST NOT be set if non constant audio
    frame size (e.g. if SID)*/
    M4MP4W_embeddedString       = 0xC155,
    M4MP4W_integrationTag       = 0xC156,
    M4MP4W_maxFileDuration      = 0xC157,
    M4MP4W_setFtypBox           = 0xC158,
    M4MP4W_DSI                  = 0xC159,
    /* H.264 trimming */
    M4MP4W_MUL_PPS_SPS          = 0xC160,
    /* H.264 trimming */
} M4MP4W_OptionID;

/**
 ******************************************************************************
 * Audio & video stream IDs
 ******************************************************************************
 */
#define AudioStreamID 1
#define VideoStreamID 2

/**
 ******************************************************************************
 * Default parameters values, that can be modified by M4MP4W_setOption
 ******************************************************************************
 */
#define M4MP4W_DefaultWidth 320
#define M4MP4W_DefaultHeight 240
#define M4MP4W_DefaultMaxAuSize  4096 /*bytes*/
#define M4MP4W_DefaultMaxChunkSize 100000 /*bytes*/
#define M4MP4W_DefaultInterleaveDur 0 /*bytes*/


/**
 ******************************************************************************
 * structure    M4MP4W_StreamIDsize
 * @brief        Video plane size
 ******************************************************************************
 */
typedef struct
{
    M4SYS_StreamID streamID;
    M4OSA_UInt16    height;
    M4OSA_UInt16    width;
} M4MP4W_StreamIDsize;

/**
 ******************************************************************************
 * structure    M4MP4W_TrackData
 * @brief       Internal core MP4 writer track structure
 ******************************************************************************
 */
typedef struct
{
    M4SYS_StreamType    trackType;
    M4OSA_UInt32        timescale;          /* T (video=1000), (AMR8=8000), (AMR16=16000)*/
    M4OSA_UInt32        sampleSize;         /* S (video=0)*/
    M4OSA_UInt32        sttsTableEntryNb;   /* J (audio=1)*/
    M4MP4W_Time32        lastCTS;           /* CTS of the previous AU,
                                               init to 0.Gives duration at the end.*/
    M4OSA_UInt32        sampleNb;           /* K (audio=F)*/
} M4MP4W_TrackData;

/**
 ******************************************************************************
 * structure    M4MP4W_AudioTrackData
 * @brief       Internal core MP4 writer audio specific structure
 ******************************************************************************
 */
typedef struct
{
    M4MP4W_State            microState;
    M4MP4W_TrackData        CommonData;
    M4OSA_UChar**           Chunk;
    M4OSA_UInt32*           chunkSizeTable;
#ifndef _M4MP4W_MOOV_FIRST
    M4OSA_UInt32*           chunkOffsetTable;
#endif /*_M4MP4W_MOOV_FIRST*/
    M4OSA_UInt32*           chunkSampleNbTable;
    M4OSA_UInt32*           chunkTimeMsTable;
    M4OSA_UInt32            currentChunk;       /* Init to 0*/
    M4OSA_UInt32            currentPos;         /* Init to 0 */
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE
    M4OSA_UInt32            currentStsc;        /* Init to 0 */
#endif
    M4MP4W_Time32           sampleDuration;     /* Check (AMR8=160), (AMR16=320)*/
    M4OSA_UInt32            MaxChunkSize;       /* Init to M4MP4W_Mp4FileData.MaxChunkSize*/
    M4OSA_UInt32            MaxAUSize;          /* Init to M4MP4W_Mp4FileData.MaxAUSize*/
    M4OSA_UInt32            LastAllocatedChunk;
    /* previously, audio au size was supposed constant,
     * which is actually not the case if silences (sid).*/
    /* at first audio au, sampleSize is set. It is later reset to 0 if non constant size.*/
    /* So sampleSize should be tested to know weither or not there is a TABLE_STSZ. */
    M4OSA_UInt32*           TABLE_STSZ; /* table size is 4K*/
    M4OSA_UInt32            nbOfAllocatedStszBlocks;
    M4OSA_UInt32*           TABLE_STTS;
    M4OSA_UInt32            nbOfAllocatedSttsBlocks;
    M4OSA_UInt32            maxBitrate;     /*not used in amr case*/
    M4OSA_UInt32            avgBitrate;     /*not used in amr case*/
    M4OSA_UChar*            DSI;            /* Decoder Specific Info: May be M4OSA_NULL
                                            (defaulted) for AMR */
    M4OSA_UInt8             dsiSize;        /* DSI size, always 9 bytes for AMR */
} M4MP4W_AudioTrackData;


/**
 ******************************************************************************
 * structure    M4MP4W_VideoTrackData
 * @brief        Internal core MP4 writer video specific structure
 ******************************************************************************
 */
typedef struct
{
    M4MP4W_State            microState;
    M4MP4W_TrackData        CommonData;
    M4OSA_UChar**           Chunk;
    M4OSA_UInt32*           chunkSizeTable;
#ifndef _M4MP4W_MOOV_FIRST
    M4OSA_UInt32*           chunkOffsetTable;
#endif /*_M4MP4W_MOOV_FIRST*/
    M4OSA_UInt32*           chunkSampleNbTable;
    M4MP4W_Time32*          chunkTimeMsTable;
    M4OSA_UInt32            currentChunk;            /* Init to 0*/
    M4OSA_UInt32            currentPos ;             /* Init to 0*/
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE
    M4OSA_UInt32            currentStsc;             /* Init to 0*/
#endif
    M4OSA_UInt32            stssTableEntryNb ;       /* N*/
    M4OSA_UInt16            width;                   /* X*/
    M4OSA_UInt16            height;                  /* Y*/
    M4OSA_UInt32*           TABLE_STTS;              /* table size is J*/
    M4OSA_UInt32            nbOfAllocatedSttsBlocks;
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE
    M4OSA_UInt16*           TABLE_STSZ;              /* table size is 2K*/
#else
    M4OSA_UInt32*           TABLE_STSZ;              /* table size is 4K*/
#endif
    M4OSA_UInt32            nbOfAllocatedStszBlocks;
    M4OSA_UInt32*           TABLE_STSS;              /* table size is N*/
    M4OSA_UInt32            nbOfAllocatedStssBlocks;
#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE
    M4OSA_UInt32            MaxAUperChunk;           /*Init to 0, i.e. not used*/
#endif
    M4OSA_UInt32            MaxChunkSize;            /*Init to M4MP4W_Mp4FileData.MaxChunkSize*/
    M4OSA_UInt32            MaxAUSize;               /*Init to M4MP4W_Mp4FileData.MaxAUSize*/
    M4OSA_UInt32            LastAllocatedChunk;
    M4OSA_UInt32            maxBitrate;
    M4OSA_UInt32            avgBitrate;
    M4OSA_UChar*            DSI;            /* Decoder Specific Info: May be M4OSA_NULL
                                            (defaulted) for H263*/
    M4OSA_UInt8             dsiSize;        /* DSI size, always 7 bytes for H263 */
} M4MP4W_VideoTrackData;

/**
 ******************************************************************************
 * structure    M4MP4W_Mp4FileData
 * @brief       Internal core MP4 writer private context structure
 ******************************************************************************
 */
typedef struct
{
    M4MP4W_State                  state;
    M4OSA_Char*                   url;
    M4OSA_UInt32                  duration;    /* D in ms, max duration of audio&video*/
    M4OSA_UInt32                  filesize;    /* actual filesize in bytes*/
    M4MP4W_AudioTrackData*        audioTrackPtr;
    M4OSA_Bool                    hasAudio;
    M4MP4W_VideoTrackData*        videoTrackPtr;
    M4OSA_Bool                    hasVideo;
    M4OSA_UInt32                  MaxChunkSize;       /* Init to 100000*/
    M4OSA_UInt32                  MaxAUSize;          /* Init to 4096*/
    M4OSA_UInt32                  MaxFileSize;        /* Init to 0, i.e. not used*/
    M4MP4W_Time32                 InterleaveDur;      /* Init to 0, i.e. not used, ms*/
    /* M4MP4W_WriteCallBack            PreWriteCallBack;*/    /*Init to M4OSA_NULL*/
    /* M4MP4W_WriteCallBack            PostWriteCallBack;*/ /*Init to M4OSA_NULL*/
    M4OSA_FileWriterPointer*      fileWriterFunctions;
    M4OSA_FileReadPointer*        fileReaderFunctions;
    M4OSA_UInt32                  camcoderVersion;
    M4OSA_Bool                    estimateAudioSize;  /* default is false*/
    M4OSA_UInt32                  audioMsChunkDur;    /* in ms, set only if estimateAudioSize
                                                         is true*/
    M4OSA_UInt32                  audioMsStopTime;    /* time to stop audio, set only if
                                                         estimateAudioSize is true*/
    M4OSA_Context                 fileWriterContext;
#ifndef _M4MP4W_MOOV_FIRST
    M4OSA_UInt32                  absoluteCurrentPos; /* new field for offset update*/
#endif /*_M4MP4W_MOOV_FIRST*/
    M4OSA_UChar*                  embeddedString;     /* 16 bytes string, default value
                                                         writen if NULL*/
    M4OSA_UChar*                  integrationTag;     /* 60 bytes string, memset to 0 if NULL */
    M4OSA_UInt32                  MaxFileDuration;    /* Init to 0, i.e. not used*/
    M4MP4C_FtypBox                ftyp;               /* ftyp atom, if not defined set major_brand
                                                            = 0, will use default box */
#ifdef _M4MP4W_RESERVED_MOOV_DISK_SPACE
    M4OSA_Char*                    safetyFileUrl;
    M4OSA_Bool                        cleanSafetyFile;
#endif /* _M4MP4W_RESERVED_MOOV_DISK_SPACE */
    M4OSA_Bool                               bMULPPSSPS;
} M4MP4W_Mp4FileData;

#endif /* _M4MP4W_USE_CST_MEMORY_WRITER */

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /*M4MP4W_TYPES_H*/

