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
 * @file        M4AMRR_CoreReader.c
 * @brief       Implementation of AMR parser
 * @note        This file contains the API Implementation for
 *              AMR Parser.
 ******************************************************************************
*/
#include "M4AMRR_CoreReader.h"
#include "M4OSA_Debug.h"
#include "M4OSA_CoreID.h"

/**
 ******************************************************************************
 * Maximum bitrate per amr type
 ******************************************************************************
*/
#define M4AMRR_NB_MAX_BIT_RATE    12200
#define M4AMRR_WB_MAX_BIT_RATE    23850

/**
 ******************************************************************************
 * AMR reader context ID
 ******************************************************************************
*/
#define M4AMRR_CONTEXTID    0x414d5252

/**
 ******************************************************************************
 * An AMR frame is 20ms
 ******************************************************************************
*/
#define M4AMRR_FRAME_LENGTH     20

/**
 ******************************************************************************
 * For the seek, the file is splitted in 40 segments for faster search
 ******************************************************************************
*/
#define    M4AMRR_NUM_SEEK_ENTRIES 40

#define M4AMRR_NB_SAMPLE_FREQUENCY 8000        /**< Narrow band sampling rate */
#define M4AMRR_WB_SAMPLE_FREQUENCY 16000    /**< Wide band sampling rate */

/**
 ******************************************************************************
 * AMR reader version numbers
 ******************************************************************************
*/
/* CHANGE_VERSION_HERE */
#define M4AMRR_VERSION_MAJOR 1
#define M4AMRR_VERSION_MINOR 11
#define M4AMRR_VERSION_REVISION 3

/**
 ******************************************************************************
 * structure    M4_AMRR_Context
 * @brief        Internal AMR reader context structure
 ******************************************************************************
*/
typedef struct
{
    M4OSA_UInt32             m_contextId ;      /* Fixed Id. to check for valid Context*/
    M4OSA_FileReadPointer*   m_pOsaFilePtrFct;  /* File function pointer */
    M4SYS_StreamDescription* m_pStreamHandler;  /* Stream Description */
    M4OSA_UInt32*            m_pSeekIndex;      /* Seek Index Table */
    M4OSA_UInt32             m_seekInterval;    /* Stores the seek Interval stored in the Index */
    M4OSA_UInt32             m_maxAuSize;       /* Stores the max Au Size */
    M4OSA_MemAddr32          m_pdataAddress;    /* Pointer to store AU data */
    M4SYS_StreamType         m_streamType;      /* Stores the stream type AMR NB or WB */
    M4OSA_Context            m_pAMRFile;        /* Data storage */
    M4AMRR_State             m_status;          /* AMR Reader Status */
    M4OSA_Int32              m_structSize;      /* size of structure*/
} M4_AMRR_Context;

/**
 ******************************************************************************
 * Parser internal functions, not usable from outside the reader context
 ******************************************************************************
*/
M4OSA_UInt32    M4AMRR_getAuSize(M4OSA_UInt32 frameType,  M4SYS_StreamType streamType);
M4OSA_UInt32    M4AMRR_getBitrate(M4OSA_UInt32 frameType,  M4SYS_StreamType streamType);

/**
 ******************************************************************************
 * M4OSA_UInt32    M4AMRR_getAuSize(M4OSA_UInt32 frameType,  M4SYS_StreamType streamType)
 * @brief    Internal function to the AMR Parser, returns the AU size of the Frame
 * @note     This function takes the stream type and the frametype and returns the
 *           frame lenght
 * @param    frameType(IN)    : AMR frame type
 * @param    streamType(IN)    : AMR stream type NB or WB
 * @returns  The frame size based on the frame type.
 ******************************************************************************
 */
M4OSA_UInt32    M4AMRR_getAuSize(M4OSA_UInt32 frameType,  M4SYS_StreamType streamType)
{
    const M4OSA_UInt32    M4AMRR_NB_AUSIZE[]={13,14,16,18,20,21,27,32,6,6,6};
    const M4OSA_UInt32    M4AMRR_WB_AUSIZE[]={18,24,33,37,41,47,51,59,61,6};

    if ( streamType == M4SYS_kAMR )
    {
            return M4AMRR_NB_AUSIZE[frameType];
    }
    else /* M4SYS_kAMR_WB */
    {
            return M4AMRR_WB_AUSIZE[frameType];
    }
}

/**
 ******************************************************************************
 * M4OSA_UInt32    M4AMRR_getBitrate(M4OSA_UInt32 frameType,  M4SYS_StreamType streamType)
 * @brief    Internal function to the AMR Parser, returns the Bit rate of the Frame
 * @note     This function takes the stream type and the frametype and returns the
 *           bit rate for the given frame.
 * @param    frameType(IN)    : AMR frame type
 * @param    streamType(IN)    : AMR stream type NB or WB
 * @returns  The frame's bit rate based on the frame type.
 ******************************************************************************
 */
M4OSA_UInt32    M4AMRR_getBitrate(M4OSA_UInt32 frameType,  M4SYS_StreamType streamType)
{
    const M4OSA_UInt32    M4AMRR_NB_BITRATE[]=
        {4750,5150,5900,6700,7400,7950,10200,12200,12200,12200,12200};
    const M4OSA_UInt32    M4AMRR_WB_BITRATE[]=
        {6600,8850,12650,14250,15850,18250,19850,23050,23850,12200};

    if ( streamType == M4SYS_kAMR )
    {
            return M4AMRR_NB_BITRATE[frameType];
    }
    else /* M4SYS_kAMR_WB */
    {
            return M4AMRR_WB_BITRATE[frameType];
    }
}

/*********************************************************/
M4OSA_ERR M4AMRR_openRead(M4OSA_Context* pContext, M4OSA_Void* pFileDescriptor,
                        M4OSA_FileReadPointer* pFileFunction)
/*********************************************************/
{
    M4_AMRR_Context*    pStreamContext;
    M4OSA_FilePosition  filePos;

    M4OSA_ERR err = M4ERR_FILE_NOT_FOUND ;
    M4OSA_UInt32 size ;
    M4OSA_UInt32 data ;
    M4OSA_Char *M4_Token;
    M4OSA_UInt32 *tokenPtr;

    /* Header for AMR NB */
    M4OSA_UInt32 M4_AMR_1       = 0x4d412123;
    M4OSA_UInt32 M4_AMR_NB_2    = 0x00000a52;

    /* Header for AMR WB */
    M4OSA_UInt32 M4_AMR_WB_2    = 0x42572d52;
    M4OSA_UInt32 M4_AMR_WB_3    = 0x0000000a;
    *pContext = M4OSA_NULL ;

    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext),M4ERR_PARAMETER,"Context M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pFileDescriptor),M4ERR_PARAMETER,"File Desc. M4OSA_NULL");

    M4_Token = (M4OSA_Char*)M4OSA_32bitAlignedMalloc(sizeof(M4OSA_MemAddr32)*3, M4AMR_READER,
                 (M4OSA_Char *)("M4_Token"));
    if(M4OSA_NULL == M4_Token)
    {
        M4OSA_DEBUG_IF3((M4OSA_NULL == M4_Token),M4ERR_ALLOC,"Mem Alloc failed - M4_Token");
        return M4ERR_ALLOC ;
    }

    pStreamContext= (M4_AMRR_Context*)M4OSA_32bitAlignedMalloc(sizeof(M4_AMRR_Context), M4AMR_READER,
                     (M4OSA_Char *)("pStreamContext"));
    if(M4OSA_NULL == pStreamContext)
    {
        free(M4_Token);
        *pContext = M4OSA_NULL ;
        return M4ERR_ALLOC ;
    }

    /* Initialize the context */
    pStreamContext->m_contextId = M4AMRR_CONTEXTID;
    pStreamContext->m_structSize=sizeof(M4_AMRR_Context);
    pStreamContext->m_pOsaFilePtrFct=pFileFunction ;
    pStreamContext->m_pStreamHandler = M4OSA_NULL ;
    pStreamContext->m_pAMRFile = M4OSA_NULL ;
    pStreamContext->m_status = M4AMRR_kOpening ;
    pStreamContext->m_pSeekIndex = M4OSA_NULL ;
    pStreamContext->m_seekInterval = 0;
    pStreamContext->m_maxAuSize = 0 ;
    pStreamContext->m_pdataAddress = M4OSA_NULL;
    err=pStreamContext->m_pOsaFilePtrFct->openRead(&pStreamContext->m_pAMRFile,
        (M4OSA_Char*)pFileDescriptor,M4OSA_kFileRead );
    if ( err != M4NO_ERROR )
    {
        /* M4OSA_DEBUG_IF3((err != M4NO_ERROR),err,"File open failed"); */
        free(pStreamContext);
        free(M4_Token);
        *pContext = M4OSA_NULL ;
        return err ;
    }

    pStreamContext->m_status = M4AMRR_kOpening ;

    size = 6;
    pStreamContext->m_pOsaFilePtrFct->readData(pStreamContext->m_pAMRFile,
                (M4OSA_MemAddr8)M4_Token, &size);
    if(size != 6)
    {
        goto cleanup;
    }

    tokenPtr = (M4OSA_UInt32*)M4_Token ;
    /* Check for the first 4 bytes of the header common to WB and NB*/
    if (*tokenPtr != M4_AMR_1)
    {
        goto cleanup;
    }

    tokenPtr++;
    data = *tokenPtr & 0x0000FFFF ;
    /* Check if the next part is Narrow band header */
    if (data!= M4_AMR_NB_2)
    {
        /* Stream is AMR Wide Band */
        filePos = 4;
        pStreamContext->m_pOsaFilePtrFct->seek(pStreamContext->m_pAMRFile,
             M4OSA_kFileSeekBeginning, &filePos);
        size = 5;
        pStreamContext->m_pOsaFilePtrFct->readData(pStreamContext->m_pAMRFile,
             (M4OSA_MemAddr8)M4_Token, &size);
        if(size != 5)
            goto cleanup;
        tokenPtr=(M4OSA_UInt32*)M4_Token;
        /* Check for the Wide band hader */
        if(*tokenPtr!= M4_AMR_WB_2)
            goto cleanup;
        tokenPtr++;
        data = *tokenPtr & 0x000000FF ;
        if(data!= M4_AMR_WB_3)
            goto cleanup;
        pStreamContext->m_streamType = M4SYS_kAMR_WB ;
    }
    else
    {
        /* Stream is a Narrow band stream */
        pStreamContext->m_streamType = M4SYS_kAMR ;
    }
    /*  No Profile level defined */
    pStreamContext->m_status = M4AMRR_kOpened;

    free(M4_Token);
    *pContext = pStreamContext ;
    return M4NO_ERROR;

cleanup:

    if(M4OSA_NULL != pStreamContext->m_pAMRFile)
    {
        pStreamContext->m_pOsaFilePtrFct->closeRead(pStreamContext->m_pAMRFile);
    }

    free(M4_Token);
    free(pStreamContext);

    *pContext = M4OSA_NULL ;

    return (M4OSA_ERR)M4ERR_AMR_NOT_COMPLIANT;
}


/*********************************************************/
M4OSA_ERR M4AMRR_getNextStream(M4OSA_Context Context, M4SYS_StreamDescription* pStreamDesc )
/*********************************************************/
{
    M4_AMRR_Context*    pStreamContext=(M4_AMRR_Context*)Context;
    M4OSA_Char            frameHeader, frameType ;
    M4OSA_UInt32        size, auCount=0;
    M4OSA_FilePosition  filePos;

    M4OSA_DEBUG_IF2((M4OSA_NULL == Context),M4ERR_PARAMETER,"Context M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pStreamDesc),M4ERR_PARAMETER,"Stream Desc. M4OSA_NULL");
    M4OSA_DEBUG_IF2((pStreamContext->m_contextId != M4AMRR_CONTEXTID),M4ERR_BAD_CONTEXT,
         "Bad Context");
    M4OSA_DEBUG_IF1(( pStreamContext->m_status != M4AMRR_kOpened), M4ERR_STATE, "Invalid State");

    if (M4OSA_NULL != pStreamContext->m_pStreamHandler)
    {
        return M4WAR_NO_MORE_STREAM ;
    }

    size = 1;
    pStreamContext->m_pOsaFilePtrFct->readData(pStreamContext->m_pAMRFile,
         (M4OSA_MemAddr8)&frameHeader, &size);

    /* XFFF FXXX -> F is the Frame type */
    frameType = ( frameHeader & 0x78 ) >> 3 ;

    if ( frameType == 15 )
    {
        return M4WAR_NO_DATA_YET ;
    }

    if (( pStreamContext->m_streamType == M4SYS_kAMR ) && ( frameType > 11 ))
    {
        return (M4OSA_ERR)M4ERR_AMR_INVALID_FRAME_TYPE;
    }

    if (( pStreamContext->m_streamType == M4SYS_kAMR_WB ) && ( frameType > 9 ))
    {
        return (M4OSA_ERR)M4ERR_AMR_INVALID_FRAME_TYPE;
    }

    /* Average bit rate is assigned the bitrate of the first frame */
    pStreamDesc->averageBitrate = M4AMRR_getBitrate(frameType,pStreamContext->m_streamType);

    filePos = -1;
    pStreamContext->m_pOsaFilePtrFct->seek(pStreamContext->m_pAMRFile, M4OSA_kFileSeekCurrent,
         &filePos);

    /* Initialize pStreamDesc */
    pStreamDesc->profileLevel = 0xFF ;
    pStreamDesc->decoderSpecificInfoSize = 0 ;
    pStreamDesc->decoderSpecificInfo = M4OSA_NULL ;
    pStreamDesc->maxBitrate = (pStreamContext->m_streamType ==
        M4SYS_kAMR )?M4AMRR_NB_MAX_BIT_RATE:M4AMRR_WB_MAX_BIT_RATE;
    pStreamDesc->profileLevel = 0xFF ;
    pStreamDesc->streamID = 1;
    pStreamDesc->streamType = pStreamContext->m_streamType;

    /* Timescale equals Sampling Frequency: NB-8000 Hz, WB-16000 Hz */
    pStreamDesc->timeScale = (pStreamContext->m_streamType == M4SYS_kAMR )?8000:16000;
    pStreamDesc->duration = M4OSA_TIME_UNKNOWN;

    pStreamContext->m_pStreamHandler =
         (M4SYS_StreamDescription*)M4OSA_32bitAlignedMalloc(sizeof(M4SYS_StreamDescription),
             M4AMR_READER, (M4OSA_Char *)("pStreamContext->m_pStreamHandler"));
    if(M4OSA_NULL == pStreamContext->m_pStreamHandler)
    {
        return M4ERR_ALLOC;
    }

    /* Copy the Stream Desc. into the Context */
    pStreamContext->m_pStreamHandler->averageBitrate = pStreamDesc->averageBitrate;
    pStreamContext->m_pStreamHandler->decoderSpecificInfo = M4OSA_NULL ;
    pStreamContext->m_pStreamHandler->decoderSpecificInfoSize = 0 ;
    pStreamContext->m_pStreamHandler->duration = M4OSA_TIME_UNKNOWN;
    pStreamContext->m_pStreamHandler->profileLevel = 0xFF ;
    pStreamContext->m_pStreamHandler->streamID = 1;
    pStreamContext->m_pStreamHandler->streamType = pStreamDesc->streamType ;
    pStreamContext->m_pStreamHandler->timeScale = pStreamDesc->timeScale ;

    /* Count the number of Access Unit in the File to get the */
    /* duration of the stream = 20 ms * number of access unit */
    while(1)
    {
        size = 1;
        pStreamContext->m_pOsaFilePtrFct->readData(pStreamContext->m_pAMRFile,
             (M4OSA_MemAddr8)&frameHeader, &size);
        if ( size == 0)
            break ;
        frameType = (frameHeader & 0x78) >> 3 ;
        /* Get the frame size and skip so many bytes */
        if(frameType != 15){
            /* GLA 20050628 when frametype is >10 we read over a table */
            if(frameType > 10)
                continue ;

            size = M4AMRR_getAuSize(frameType, pStreamContext->m_streamType);
            if(size > pStreamContext->m_maxAuSize )
            {
                pStreamContext->m_maxAuSize = size ;
            }
            filePos = size-1;
            pStreamContext->m_pOsaFilePtrFct->seek(pStreamContext->m_pAMRFile,
                 M4OSA_kFileSeekCurrent, &filePos);
            auCount++;
        }
    }

    /* Each Frame is 20 m Sec. */
    pStreamContext->m_pStreamHandler->duration = auCount * M4AMRR_FRAME_LENGTH ;
    pStreamDesc->duration = pStreamContext->m_pStreamHandler->duration ;

    /* Put the file pointer back at the first Access unit */
    if( pStreamContext->m_streamType == M4SYS_kAMR )
    {
        filePos = 6;
        pStreamContext->m_pOsaFilePtrFct->seek(pStreamContext->m_pAMRFile,
             M4OSA_kFileSeekBeginning, &filePos);
    }
    if ( pStreamContext->m_streamType == M4SYS_kAMR_WB )
    {
        filePos = 9;
        pStreamContext->m_pOsaFilePtrFct->seek(pStreamContext->m_pAMRFile,
             M4OSA_kFileSeekBeginning, &filePos);
    }
    return M4NO_ERROR ;
}

/*********************************************************/
M4OSA_ERR M4AMRR_startReading(M4OSA_Context Context, M4SYS_StreamID* pStreamIDs )
/*********************************************************/
{
    M4_AMRR_Context* pStreamContext=(M4_AMRR_Context*)Context;
    M4OSA_Int32 size = 0 ;

    M4OSA_DEBUG_IF2((M4OSA_NULL == Context),M4ERR_PARAMETER,"Context M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pStreamIDs),M4ERR_PARAMETER,"Stream Ids. M4OSA_NULL");
    M4OSA_DEBUG_IF2((pStreamContext->m_contextId != M4AMRR_CONTEXTID),M4ERR_BAD_CONTEXT,
         "Bad Context");
    M4OSA_DEBUG_IF1(( pStreamContext->m_status != M4AMRR_kOpened), M4ERR_STATE, "Invalid State");

    while( pStreamIDs[size] != 0 )
    {
        if( pStreamIDs[size++] != 1 )
        {
            return M4ERR_BAD_STREAM_ID ;
        }
    }

    /* Allocate memory for data Address for use in NextAU() */
    if(M4OSA_NULL == pStreamContext->m_pdataAddress)
    {
        size = pStreamContext->m_maxAuSize ;
        /* dataAddress is owned by Parser, application should not delete or free it */
        pStreamContext->m_pdataAddress =(M4OSA_MemAddr32)M4OSA_32bitAlignedMalloc(size + (4 - size % 4),
            M4AMR_READER, (M4OSA_Char *)("pStreamContext->m_pdataAddress"));
        if(M4OSA_NULL == pStreamContext->m_pdataAddress)
        {
                M4OSA_DEBUG_IF3((M4OSA_NULL == pStreamContext->m_pdataAddress),M4ERR_ALLOC,
                    "Mem Alloc failed - dataAddress");
                return M4ERR_ALLOC;
        }
    }

    /* Set the state of context to Reading */
    pStreamContext->m_status = M4AMRR_kReading ;

    return M4NO_ERROR ;
}


/*********************************************************/
M4OSA_ERR M4AMRR_nextAU(M4OSA_Context Context, M4SYS_StreamID StreamID, M4SYS_AccessUnit* pAu)
/*********************************************************/
{
    M4_AMRR_Context* pStreamContext=(M4_AMRR_Context*)Context;
    M4OSA_Char        frameHeader ;
    M4OSA_Char        frameType ;
    M4OSA_Int32        auSize;
    M4OSA_UInt32    size ;
    M4OSA_FilePosition  filePos;

    M4OSA_DEBUG_IF2((M4OSA_NULL == Context),M4ERR_PARAMETER,"Context M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pAu),M4ERR_PARAMETER,"Access Unit . M4OSA_NULL");
    M4OSA_DEBUG_IF2((pStreamContext->m_contextId != M4AMRR_CONTEXTID),M4ERR_BAD_CONTEXT,
         "Bad Context");
    M4OSA_DEBUG_IF1(( pStreamContext->m_status != M4AMRR_kReading), M4ERR_STATE, "Invalid State");

    if ( StreamID != 1 )
    {
            return M4ERR_BAD_STREAM_ID;
    }

    /* Read the frame header byte */
    size = pStreamContext->m_maxAuSize;
    pStreamContext->m_pOsaFilePtrFct->readData(pStreamContext->m_pAMRFile,
         (M4OSA_MemAddr8)pStreamContext->m_pdataAddress, &size);
    if(size != pStreamContext->m_maxAuSize)
    {
        return M4WAR_NO_MORE_AU;
    }

    frameHeader = ((M4OSA_MemAddr8)pStreamContext->m_pdataAddress)[0];

    frameType = ( frameHeader & 0x78 ) >> 3 ;

    if (( pStreamContext->m_streamType == M4SYS_kAMR ) &&
        ( frameType > 11 ) && ( frameType != 15 ))
    {
        return (M4OSA_ERR)M4ERR_AMR_INVALID_FRAME_TYPE;
    }

    if (( pStreamContext->m_streamType == M4SYS_kAMR_WB ) &&
        ( frameType > 9 ) && ( frameType != 15 ))
    {
        return (M4OSA_ERR)M4ERR_AMR_INVALID_FRAME_TYPE;
    }

    /* Get the frame size */
    if(frameType == 15)
    {
        auSize = 1;
    }
    else
    {
        auSize = M4AMRR_getAuSize(frameType, pStreamContext->m_streamType);
    }

    size -= auSize ;
    if(size != 0)
    {
        filePos = -((M4OSA_FilePosition)size);
        pStreamContext->m_pOsaFilePtrFct->seek(pStreamContext->m_pAMRFile,
             M4OSA_kFileSeekCurrent, &filePos);
    }

    pAu->size = auSize ;

    /* even when frameType == 15 (no data frame), ARM core decoder outputs full PCM buffer */
    /*if(frameType == 15 )
    {
        pAu->CTS += 0;
    }*/
    /*else*/
    {
        pAu->CTS += M4AMRR_FRAME_LENGTH ;
    }


    pAu->DTS = pAu->CTS ;
    pAu->attribute = M4SYS_kFragAttrOk;

    pAu->stream = pStreamContext->m_pStreamHandler;
    pAu->dataAddress = pStreamContext->m_pdataAddress ;

    if(frameHeader & 0x80)
    {
        return M4WAR_NO_MORE_AU;
    }

    /* Change the state to implement NextAu->freeAu->NextAu FSM */
    pStreamContext->m_status = M4AMRR_kReading_nextAU ;

    return M4NO_ERROR ;
}

/*********************************************************/
M4OSA_ERR M4AMRR_freeAU(M4OSA_Context Context, M4SYS_StreamID StreamID, M4SYS_AccessUnit* pAu)
/*********************************************************/
{
    M4_AMRR_Context* pStreamContext=(M4_AMRR_Context*)Context;
    M4OSA_DEBUG_IF2((M4OSA_NULL == Context),M4ERR_PARAMETER,"Context M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pAu),M4ERR_PARAMETER,"Access Unit . M4OSA_NULL");
    M4OSA_DEBUG_IF2((pStreamContext->m_contextId != M4AMRR_CONTEXTID),M4ERR_BAD_CONTEXT,
         "Bad Context");
    M4OSA_DEBUG_IF1(( pStreamContext->m_status != M4AMRR_kReading_nextAU), M4ERR_STATE,
         "Invalid State");

    if (( StreamID != 1 ) && ( StreamID != 0))
    {
            return M4ERR_BAD_STREAM_ID;
    }

    /* Change the state to Reading so as to allow access to next AU */
    pStreamContext->m_status = M4AMRR_kReading ;

    return M4NO_ERROR ;
}

/*********************************************************/
M4OSA_ERR M4AMRR_seek(M4OSA_Context Context, M4SYS_StreamID* pStreamID, M4OSA_Time time,
                         M4SYS_SeekAccessMode seekMode, M4OSA_Time* pObtainCTS)
/*********************************************************/
{
    M4_AMRR_Context* pStreamContext=(M4_AMRR_Context*)Context;
    M4OSA_UInt32 count, prevAU, nextAU ;
    M4OSA_UInt32 size ;
    M4OSA_UInt32 auSize ;
    M4OSA_UInt32 position, partSeekTime;
    M4OSA_UInt32 auCount = 0, skipAuCount = 0 ;
    M4OSA_Char    frameHeader ;
    M4OSA_Char    frameType ;
    M4OSA_FilePosition  filePos;
    M4OSA_Double time_double;

    /*Make explicit time cast, but take care that timescale is not used !!!*/
    M4OSA_TIME_TO_MS(time_double, time, 1000);

    *pObtainCTS = 0;

    M4OSA_DEBUG_IF2((M4OSA_NULL == Context),M4ERR_PARAMETER,"Context M4OSA_NULL");
    M4OSA_DEBUG_IF2((pStreamContext->m_contextId != M4AMRR_CONTEXTID),M4ERR_BAD_CONTEXT,
         "Bad Context");
    M4OSA_DEBUG_IF1(( pStreamContext->m_status != M4AMRR_kReading) && \
        ( pStreamContext->m_status != M4AMRR_kOpened), M4ERR_STATE, "Invalid State");
    M4OSA_DEBUG_IF1((time_double < 0),M4ERR_PARAMETER,"negative time");

    /* Coming to seek for the first time, need to build the seekIndex Table */
    if(M4OSA_NULL == pStreamContext->m_pSeekIndex)
    {
        M4OSA_Double duration_double;

        count = 0 ;
        pStreamContext->m_pSeekIndex =
             (M4OSA_UInt32*)M4OSA_32bitAlignedMalloc(M4AMRR_NUM_SEEK_ENTRIES * sizeof(M4OSA_UInt32),
                 M4AMR_READER, (M4OSA_Char *)("pStreamContext->m_pSeekIndex"));

        if(M4OSA_NULL == pStreamContext->m_pSeekIndex)
        {
            M4OSA_DEBUG_IF3((M4OSA_NULL == pStreamContext->m_pSeekIndex),M4ERR_ALLOC,
                "Mem Alloc Failed - SeekIndex");
            return M4ERR_ALLOC ;
        }

        /* point to the first AU */
        if( pStreamContext->m_streamType == M4SYS_kAMR )
        {
            filePos = 6;
        }
        else /*if ( pStreamContext->m_streamType == M4SYS_kAMR_WB )*/
        {
            filePos = 9;
        }

        pStreamContext->m_pOsaFilePtrFct->seek(pStreamContext->m_pAMRFile,
             M4OSA_kFileSeekBeginning, &filePos);

        /* Set the postion to begining of first AU */
        position = (pStreamContext->m_streamType != M4SYS_kAMR)?9:6;

        /*Make explicit time cast, but take care that timescale is not used !!!*/
        M4OSA_TIME_TO_MS(duration_double, pStreamContext->m_pStreamHandler->duration, 1000);

        /* Calculate the seek Interval duration based on total dutation */
        /* Interval = (duration / ENTRIES) in multiples of AU frame length */
        pStreamContext->m_seekInterval =
             (M4OSA_UInt32)(duration_double / M4AMRR_NUM_SEEK_ENTRIES) ;
        pStreamContext->m_seekInterval /= M4AMRR_FRAME_LENGTH ;
        pStreamContext->m_seekInterval *= M4AMRR_FRAME_LENGTH ;
        skipAuCount = pStreamContext->m_seekInterval / M4AMRR_FRAME_LENGTH ;

        pStreamContext->m_pSeekIndex[count++]=position;
        while(count < M4AMRR_NUM_SEEK_ENTRIES )
        {
            size = 1;
            pStreamContext->m_pOsaFilePtrFct->readData(pStreamContext->m_pAMRFile,
                 (M4OSA_MemAddr8)&frameHeader, &size);
            if ( size == 0)
            {
                break ;
            }
            frameType = (frameHeader & 0x78) >> 3 ;
            if(frameType != 15)
            {
                /**< bugfix Ronan Cousyn 05/04/2006: In the core reader AMR, the
                 * function M4AMRR_seek doesn't check the frameType */
                if (( pStreamContext->m_streamType == M4SYS_kAMR ) && ( frameType > 10 ))
                {
                    return M4ERR_AMR_INVALID_FRAME_TYPE;
                }
                if (( pStreamContext->m_streamType == M4SYS_kAMR_WB ) && ( frameType > 9 ))
                {
                    return M4ERR_AMR_INVALID_FRAME_TYPE;
                }
                auSize = M4AMRR_getAuSize(frameType, pStreamContext->m_streamType);
                position += auSize ;
                filePos = auSize-1;
                pStreamContext->m_pOsaFilePtrFct->seek(pStreamContext->m_pAMRFile,
                     M4OSA_kFileSeekCurrent, &filePos);
                auCount++;
            }
            else
            {
                position ++;
            }
            /* Skip the number of AU's as per interval and store in the Index table */
            if ( (skipAuCount != 0) && !(auCount % skipAuCount))
            {
                pStreamContext->m_pSeekIndex[count++] = position;
            }
        }
    }/* End of Building the seek table */

    /* Use the seek table to seek the required time in the stream */

    /* If we are seeking the begining of the file point to first AU */
    if ( seekMode == M4SYS_kBeginning )
    {
        if( pStreamContext->m_streamType == M4SYS_kAMR )
        {
            filePos = 6;
        }
        else /*if ( pStreamContext->m_streamType == M4SYS_kAMR_WB )*/
        {
            filePos = 9;
        }
        pStreamContext->m_pOsaFilePtrFct->seek(pStreamContext->m_pAMRFile,
             M4OSA_kFileSeekBeginning, &filePos );
        return M4NO_ERROR ;
    }

    /* Get the Nearest Second */
    if (0 != pStreamContext->m_seekInterval)
    {
        position = (M4OSA_UInt32)(time_double / pStreamContext->m_seekInterval);
    }
    else
    {
        /*avoid division by 0*/
        position = 0;
    }

    /* We have only 40 seek Index. */
    position=(position >= M4AMRR_NUM_SEEK_ENTRIES)?M4AMRR_NUM_SEEK_ENTRIES-1:position;

    /* SeekIndex will point to nearest Au, we need to search for the
    required time form that position */
    partSeekTime = (M4OSA_UInt32)time_double - position * pStreamContext->m_seekInterval;

    position = pStreamContext->m_pSeekIndex[position];

    if(!position)
    {
        return M4WAR_INVALID_TIME ;
    }

    /* point the file pointer to nearest AU */
    filePos = position;
    pStreamContext->m_pOsaFilePtrFct->seek(pStreamContext->m_pAMRFile, M4OSA_kFileSeekBeginning,
         &filePos );

    if ( partSeekTime == 0)
    {
        *pObtainCTS = time;
        return M4NO_ERROR;
    }

    *pObtainCTS = (M4OSA_Time)(time_double - (M4OSA_Double)partSeekTime);

    switch(seekMode)
    {
        /* Get the AU before the target time */
        case M4SYS_kPreviousRAP:
        case M4SYS_kNoRAPprevious:
            position = partSeekTime / M4AMRR_FRAME_LENGTH ;
            if ( !(partSeekTime % M4AMRR_FRAME_LENGTH) )
            {
                position -- ;
            }
        break;
        /* Get the Closest AU following the target time */
        case M4SYS_kNextRAP:
        case M4SYS_kNoRAPnext:
            position = (partSeekTime + M4AMRR_FRAME_LENGTH )/ M4AMRR_FRAME_LENGTH ;
        break;
        /*  Get the closest AU to target time */
        case M4SYS_kClosestRAP:
        case M4SYS_kNoRAPclosest:
            prevAU = partSeekTime-(partSeekTime/M4AMRR_FRAME_LENGTH)*M4AMRR_FRAME_LENGTH;
            nextAU =
                 ((partSeekTime+M4AMRR_FRAME_LENGTH)/M4AMRR_FRAME_LENGTH)*M4AMRR_FRAME_LENGTH -\
                     partSeekTime ;
            if(prevAU < nextAU)
            {
                position = partSeekTime / M4AMRR_FRAME_LENGTH ;
            }
            else
            {
                position = (partSeekTime + M4AMRR_FRAME_LENGTH )/ M4AMRR_FRAME_LENGTH ;
            }
        break;
        case M4SYS_kBeginning:
        break;
    }

    count = 0 ;
    /* Skip the Access unit in the stream to skip the part seek time,
       to reach the required target time */
    while(count < position )
    {
        size = 1;
        pStreamContext->m_pOsaFilePtrFct->readData(pStreamContext->m_pAMRFile,
             (M4OSA_MemAddr8)&frameHeader, &size);
        if ( size == 0)
        {
            /* If the target time is invalid, point to begining and return */
            *pObtainCTS = 0;
            filePos = pStreamContext->m_pSeekIndex[0];
            pStreamContext->m_pOsaFilePtrFct->seek(pStreamContext->m_pAMRFile,
                 M4OSA_kFileSeekBeginning, &filePos);
            return M4WAR_INVALID_TIME ;
        }
        *pObtainCTS += M4AMRR_FRAME_LENGTH; /*Should use M4OSA_INT64_ADD !!*/
        count++;
        frameType = (frameHeader & 0x78) >> 3 ;
        if(frameType == 15)
        {
            auSize = 1 ;
        }
        else
        {
            auSize = M4AMRR_getAuSize(frameType, pStreamContext->m_streamType);
        }

        filePos = auSize-1;
        pStreamContext->m_pOsaFilePtrFct->seek(pStreamContext->m_pAMRFile,
             M4OSA_kFileSeekCurrent, &filePos);
    }

    return M4NO_ERROR;
}

/*********************************************************/
M4OSA_ERR M4AMRR_closeRead(M4OSA_Context Context)
/*********************************************************/
{
    M4_AMRR_Context* pStreamContext=(M4_AMRR_Context*)Context;
    M4OSA_DEBUG_IF2((M4OSA_NULL == Context),M4ERR_PARAMETER,"Context M4OSA_NULL");

    /* Close the AMR stream */
    pStreamContext->m_pOsaFilePtrFct->closeRead(pStreamContext->m_pAMRFile);

    pStreamContext->m_status=M4AMRR_kClosed ;

    /* Check if AU data Address is allocated memory and free it */
    if(M4OSA_NULL != pStreamContext->m_pdataAddress)
    {
        free(pStreamContext->m_pdataAddress);
    }

    /* Check if the stream handler is allocated memory */
    if(M4OSA_NULL != pStreamContext->m_pStreamHandler)
    {
        free(pStreamContext->m_pStreamHandler);
    }

    /* Seek table is created only when seek is used, so check if memory is allocated */
    if(M4OSA_NULL != pStreamContext->m_pSeekIndex)
    {
        free(pStreamContext->m_pSeekIndex);
    }

    /* Free the context */
    free(pStreamContext);

    return M4NO_ERROR ;
}

/*********************************************************/
M4OSA_ERR M4AMRR_getState(M4OSA_Context Context, M4AMRR_State* pState, M4SYS_StreamID streamId)
/*********************************************************/
{
    M4_AMRR_Context* pStreamContext=(M4_AMRR_Context*)Context;
    M4OSA_DEBUG_IF2((M4OSA_NULL == Context),M4ERR_PARAMETER,"Context M4OSA_NULL");
    M4OSA_DEBUG_IF2((pStreamContext->m_contextId != M4AMRR_CONTEXTID),M4ERR_BAD_CONTEXT,
         "Bad Context");

    if (( streamId != 1 ) && ( streamId != 0))
    {
            return M4ERR_BAD_STREAM_ID;
    }

    *pState = pStreamContext->m_status ;

    return M4NO_ERROR ;
}


/*********************************************************/
M4OSA_ERR M4AMRR_getVersion    (M4_VersionInfo *pVersion)
/*********************************************************/
{
    M4OSA_TRACE1_1("M4AMRR_getVersion called with pVersion: 0x%x\n", pVersion);
    M4OSA_DEBUG_IF1(((M4OSA_UInt32) pVersion == 0),M4ERR_PARAMETER,
         "pVersion is NULL in M4AMRR_getVersion");

    pVersion->m_major = M4AMRR_VERSION_MAJOR;
    pVersion->m_minor = M4AMRR_VERSION_MINOR;
    pVersion->m_revision = M4AMRR_VERSION_REVISION;

    return M4NO_ERROR;
}

/*********************************************************/
M4OSA_ERR M4AMRR_getmaxAUsize(M4OSA_Context Context, M4OSA_UInt32 *pMaxAuSize)
/*********************************************************/
{
    M4_AMRR_Context* pStreamContext=(M4_AMRR_Context*)Context;

    /**
     * Check input parameters */
    M4OSA_DEBUG_IF1((M4OSA_NULL == Context),  M4ERR_PARAMETER,
                "M4AMRR_getmaxAUsize: Context is M4OSA_NULL");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pMaxAuSize),M4ERR_PARAMETER,
                "M4AMRR_getmaxAUsize: pMaxAuSize is M4OSA_NULL");

    *pMaxAuSize = pStreamContext->m_maxAuSize;

    return M4NO_ERROR;
}

