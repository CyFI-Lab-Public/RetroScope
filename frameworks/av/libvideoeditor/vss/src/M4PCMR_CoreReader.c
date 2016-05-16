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
 ************************************************************************
 * @file    M4PCM_PCMReader.c
 * @brief   PCM reader implementation
 * @note    This file implements functions of the PCM reader
 ************************************************************************
 */
#include "M4OSA_CharStar.h"
#include "M4PCMR_CoreReader.h"
#include "M4OSA_Debug.h"
#include "M4OSA_CharStar.h"
/**
 ******************************************************************************
 * PCM reader version numbers
 ******************************************************************************
 */
/* CHANGE_VERSION_HERE */
#define M4PCMR_VERSION_MAJOR 1
#define M4PCMR_VERSION_MINOR 0
#define M4PCMR_VERSION_REVISION 0

/**
 ************************************************************************
 * M4OSA_ERR M4PCMR_openRead(M4OSA_Context* pContext, M4OSA_Void* pUrl,
 *                             M4OSA_FileReaderPointer* pFileFunction)
 * @brief   This function opens a PCM file
 * @note    This function :
 *          - opens a PCM file
 *          - initializes PCM context,
 *          - verifies PCM file format
 *          - Fill decoder config structure
 *          - Changes state of the reader in 'Opening'
 * @param   pContext: (OUT) Pointer on the PCM Reader context
 * @param   pUrl: (IN) Name of the PCM file
 * @param   pFileFunctions: (IN) Pointer on the file access functions
 * @return  M4NO_ERROR                      there is no error during the opening
 * @return  M4ERR_PARAMETER                 pContext and/or pUrl and/or pFileFunction is NULL
 * @return  M4ERR_ALLOC                     there is no more memory available
 * @return  M4ERR_FILE_NOT_FOUND            the file cannot be found
 * @return  M4PCMC_ERR_PCM_NOT_COMPLIANT    the file does not seem to be compliant, no RIFF,
 *                                             or lack of any mandatory chunk.
 * @return  M4PCMC_ERR_PCM_NOT_SUPPORTED    the PCM format of this file is not supported by the
 *                                           reader
 * @return  Any M4OSA_FILE errors           see OSAL File specification for detailed errors
 ************************************************************************
 */
M4OSA_ERR M4PCMR_openRead(M4OSA_Context* pContext, M4OSA_Void* pUrl,
                             M4OSA_FileReadPointer* pFileFunction)
{
    M4OSA_ERR       err;
    M4PCMR_Context *context;
    M4OSA_Char*        pTempURL;
    M4OSA_Char        value[6];

    /* Check parameters */
    if((M4OSA_NULL == pContext)|| (M4OSA_NULL == pUrl) ||(M4OSA_NULL == pFileFunction))
    {
        return M4ERR_PARAMETER;
    }

    /* Allocates the context */
    context = M4OSA_NULL;
    context = (M4PCMR_Context *)M4OSA_32bitAlignedMalloc(sizeof(M4PCMR_Context), M4WAV_READER,
         (M4OSA_Char *)"M4PCMR_openRead");
    if (M4OSA_NULL == context)
    {
        return M4ERR_ALLOC;
    }
    *pContext = (M4OSA_Context)context;

    /* Initialize the context */
    context->m_offset = 0;

    context->m_state            = M4PCMR_kInit;
    context->m_microState       = M4PCMR_kInit;
    context->m_pFileReadFunc    = M4OSA_NULL;
    context->m_fileContext      = M4OSA_NULL;
    context->m_pAuBuffer        = M4OSA_NULL;
    context->m_pDecoderSpecInfo = M4OSA_NULL;

    /* Set sample frequency */
    pTempURL = (M4OSA_Char*)pUrl + (strlen((const char *)pUrl)-11);
    M4OSA_chrNCopy(value, pTempURL, 5);
    M4OSA_chrGetUInt32(pTempURL, &(context->m_decoderConfig.SampleFrequency),
         M4OSA_NULL, M4OSA_kchrDec);

    /* Set number of channels */
    pTempURL += 6;
    M4OSA_chrNCopy(value, pTempURL, 1);
    M4OSA_chrGetUInt16(pTempURL, &(context->m_decoderConfig.nbChannels),
         M4OSA_NULL, M4OSA_kchrDec);

    M4OSA_chrNCopy(pUrl,pUrl, (strlen((const char *)pUrl)-12));
    /* Open the file */
    context->m_fileContext = M4OSA_NULL;
    err = pFileFunction->openRead(&(context->m_fileContext), pUrl, M4OSA_kFileRead);
    if(M4NO_ERROR != err)
    {
        return err;
    }
    context->m_decoderConfig.BitsPerSample = 16;
    context->m_decoderConfig.AvgBytesPerSec = context->m_decoderConfig.SampleFrequency * 2 \
        * context->m_decoderConfig.nbChannels;
    err = pFileFunction->getOption(context->m_fileContext, M4OSA_kFileReadGetFileSize,
         (M4OSA_DataOption*)&(context->m_decoderConfig.DataLength));
    if(M4NO_ERROR != err)
    {
        return err;
    }
    context->m_blockSize = 2048 * context->m_decoderConfig.nbChannels;  // Raw PCM.  Hence, get a
                                                                        // chunk of data

    if(context->m_decoderConfig.SampleFrequency == 8000)
    {
        /* AMR case, no pb */
        context->m_blockSize = context->m_decoderConfig.nbChannels *\
             (context->m_decoderConfig.SampleFrequency / 50) * \
                (context->m_decoderConfig.BitsPerSample / 8);
    }
    if(context->m_decoderConfig.SampleFrequency == 16000)
    {
        /* AAC case, we can't read only 20 ms blocks */
        context->m_blockSize = 2048 * context->m_decoderConfig.nbChannels;
    }
    context->m_dataStartOffset = 0;
    context->m_pFileReadFunc = pFileFunction;

    context->m_pAuBuffer = (M4OSA_MemAddr32)M4OSA_32bitAlignedMalloc(context->m_blockSize, M4WAV_READER,
         (M4OSA_Char *)"Core PCM reader Access Unit");
    if (M4OSA_NULL == context->m_pAuBuffer)
    {
        err = M4ERR_ALLOC;
        goto cleanup;
    }

    /* Change state */
    context->m_state = M4PCMR_kOpening;

    return M4NO_ERROR;

cleanup:

    /* Close the file */
    if(context->m_pFileReadFunc != M4OSA_NULL)
        context->m_pFileReadFunc->closeRead(context->m_fileContext);

    /* Free internal context */
    free(context);
    *pContext = M4OSA_NULL;

    return err;
}

/**
 ************************************************************************
 * M4OSA_ERR M4PCMR_getNextStream(M4OSA_Context context, M4SYS_StreamDescription* pStreamDesc)
 * @brief   This function get the (unique) stream of a PCM file
 * @note    This function :
 *          - Allocates and fills the decoder specific info structure
 *          - Fills decoder specific infos structure
 *          - Fills pStreamDesc structure allocated by the caller
 * @param   context: (IN/OUT) PCM Reader context
 * @param   pStreamDesc: (IN) Stream Description context
 * @return  M4NO_ERROR          there is no error
 * @return  M4ERR_PARAMETER     at least one parameter is NULL
 * @return  M4ERR_ALLOC         there is no more memory available
 * @return  M4ERR_STATE         this function cannot be called now
 * @return  Any M4OSA_FILE      errors see OSAL File specification for detailed errors
 ************************************************************************
 */
M4OSA_ERR M4PCMR_getNextStream(M4OSA_Context context, M4SYS_StreamDescription* pStreamDesc)
{
    M4PCMR_Context *c = (M4PCMR_Context *)context;

    /* Check parameters */
    if((M4OSA_NULL == context)|| (M4OSA_NULL == pStreamDesc))
    {
        return M4ERR_PARAMETER;
    }

    if (c->m_state == M4PCMR_kOpening_streamRetrieved)
    {
        return M4WAR_NO_MORE_STREAM;
    }
    /* Check Reader's m_state */
    if(c->m_state != M4PCMR_kOpening)
    {
        return M4ERR_STATE;
    }

    /* Only one stream is contained in PCM file */
    pStreamDesc->streamID = 1;
    /* Not used */
    pStreamDesc->profileLevel = 0;
    pStreamDesc->decoderSpecificInfoSize = sizeof(M4PCMC_DecoderSpecificInfo);

    /* Allocates decoder specific info structure */
    pStreamDesc->decoderSpecificInfo = M4OSA_NULL;
    pStreamDesc->decoderSpecificInfo =
        (M4OSA_MemAddr32)M4OSA_32bitAlignedMalloc( sizeof(M4PCMC_DecoderSpecificInfo), M4WAV_READER,
             (M4OSA_Char *)"M4PCMR_getNextStream");
    if(pStreamDesc->decoderSpecificInfo == M4OSA_NULL)
    {
        return M4ERR_ALLOC;
    }
    /* Fill decoderSpecificInfo structure, with decoder config structure filled in 'openread'
         function */
    memcpy((void *)pStreamDesc->decoderSpecificInfo,
         (void *)&c->m_decoderConfig, sizeof(M4PCMC_DecoderSpecificInfo));

    /* Fill other fields of pStreamDesc structure */
    pStreamDesc->timeScale = 1000;
    pStreamDesc->duration = (M4OSA_Time)(((M4OSA_Double)(c->m_decoderConfig.DataLength)\
         / (M4OSA_Double)(c->m_decoderConfig.AvgBytesPerSec))*pStreamDesc->timeScale);
    pStreamDesc->averageBitrate = c->m_decoderConfig.AvgBytesPerSec * 8;/* in bits, multiply by 8*/
    pStreamDesc->maxBitrate = pStreamDesc->averageBitrate; /* PCM stream has constant bitrate */

    /* Determines Stream type */
    switch(c->m_decoderConfig.BitsPerSample)
    {
        case 8:
            switch(c->m_decoderConfig.nbChannels)
            {
                case 1:
                    pStreamDesc->streamType = M4SYS_kPCM_8bitsU;
                    break;
//                case 2:
//                    pStreamDesc->streamType = M4SYS_kPCM_8bitsS; /* ??? 8bits stereo not
                                                                  //   defined ? */
//                    break;
                default:
                    pStreamDesc->streamType = M4SYS_kAudioUnknown;
            }
            break;

        case 16:
            switch(c->m_decoderConfig.nbChannels)
            {
                case 1:
                    pStreamDesc->streamType = M4SYS_kPCM_16bitsU;
                    break;
                case 2:
                    pStreamDesc->streamType = M4SYS_kPCM_16bitsS;
                    break;
                default:
                    pStreamDesc->streamType = M4SYS_kAudioUnknown;
            }
            break;

        default:
            pStreamDesc->streamType = M4SYS_kAudioUnknown;
    }

    c->m_pDecoderSpecInfo = pStreamDesc->decoderSpecificInfo;

    c->m_state = M4PCMR_kOpening_streamRetrieved;

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR M4PCMR_startReading(M4OSA_Context context, M4SYS_StreamID* pStreamIDs)
 * @brief   This function starts reading the unique stream of a PCM file
 * @note    This function :
 *          - Verifies that the current reader's state allows to start reading a stream
 *          - Check that provided StreamId is correct (always true, only one stream...)
 *            In the player application, a StreamId table is initialized as follow:
 *              M4SYS_StreamID pStreamID[2]={1,0};
 *          - Change state of the reader in 'Reading'
 * @param   context: (IN/OUT) PCM Reader context
 * @param   streamID: (IN) Stream selection
 * @return  M4NO_ERROR          there is no error
 * @return  M4ERR_PARAMETER     at least one parameter is NULL
 * @return  M4ERR_STATE         this function cannot be called now
 * @return  M4ERR_BAD_STREAM_ID at least one of the streamID does not exist
 *          (should never happen if table pStreamID is correctly initialized as above)
 ************************************************************************
 */
M4OSA_ERR M4PCMR_startReading(M4OSA_Context context, M4SYS_StreamID* pStreamIDs)
{
    M4PCMR_Context *c = (M4PCMR_Context *)context;

    /* Check parameters */
    if((M4OSA_NULL == context) || (M4OSA_NULL == pStreamIDs))
    {
        return M4ERR_PARAMETER;
    }

    /* Check Reader's state */
    if(c->m_state != M4PCMR_kOpening_streamRetrieved)
    {
        return M4ERR_STATE;
    }

    /* Check pStreamID and if they're OK, change reader's state */
    if(pStreamIDs[0] == 1 || pStreamIDs[0] == 0)
    /* First and unique stream contained in PCM file */
    {
        c->m_state = M4PCMR_kReading;
        c->m_microState = M4PCMR_kReading;
    }
    else
    {
        return M4ERR_BAD_STREAM_ID;
    }

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR M4PCMR_nextAU(M4OSA_Context context, M4SYS_StreamID streamID, M4SYS_AccessUnit* pAU)
 * @brief   This function reads the next AU contained in the PCM file
 * @note    This function :
 *          - Verifies that the current reader's state allows to read an AU
 *          - Allocates memory to store read AU
 *          - Read data from file and store them into previously allocated memory
 *          - Fill AU structure fileds (CTS...)
 *          - Change state of the reader in 'Reading' (not useful...)
 *          - Change Micro state 'Reading' in M4PCMR_kReading_nextAU
 *            (AU is read and can be deleted)
 *          - Check if the last AU has been read or if we're about to read it
 * @param   context: (IN/OUT) PCM Reader context
 * @param   streamID: (IN) Stream selection
 * @param   pAU: (IN/OUT) Acces Unit Structure
 * @return  M4NO_ERROR          there is no error
 * @return  M4ERR_PARAMETER     at least one parameter is NULL
 * @return  M4ERR_ALLOC         there is no more memory available
 * @return  M4ERR_STATE         this function cannot be called now
 * @return  M4M4WAR_NO_DATA_YET there is no enough data in the file to provide a new access unit.
 * @return  M4WAR_END_OF_STREAM There is no more access unit in the stream,
 *                              or the sample number is bigger the maximum one.
 ************************************************************************
 */
M4OSA_ERR M4PCMR_nextAU(M4OSA_Context context, M4SYS_StreamID streamID, M4SYS_AccessUnit* pAU)
{
    M4PCMR_Context *c = (M4PCMR_Context *)context;
    M4OSA_ERR err = M4NO_ERROR;
    M4OSA_UInt32 size_read;

    /* Check parameters */
    if((M4OSA_NULL == context) || (M4OSA_NULL == pAU))
    {
        return M4ERR_PARAMETER;
    }

    /* Check Reader's state */
    if(c->m_state != M4PCMR_kReading && c->m_microState != M4PCMR_kReading)
    {
        return M4ERR_STATE;
    }

    /* Allocates AU dataAdress */
    pAU->dataAddress = c->m_pAuBuffer;
    size_read        = c->m_blockSize;

    if((c->m_offset + size_read) >= c->m_decoderConfig.DataLength)
    {
        size_read = c->m_decoderConfig.DataLength - c->m_offset;
    }

    /* Read data in file, and copy it to AU Structure */
    err = c->m_pFileReadFunc->readData(c->m_fileContext, (M4OSA_MemAddr8)pAU->dataAddress,
         (M4OSA_UInt32 *)&size_read);
    if(M4NO_ERROR != err)
    {
        return err;
    }

    /* Calculates the new m_offset, used to determine whether we're at end of reading or not */
    c->m_offset = c->m_offset + size_read;

    /* Fill others parameters of AU structure */
    pAU->CTS =
         (M4OSA_Time)(((M4OSA_Double)c->m_offset/(M4OSA_Double)c->m_decoderConfig.AvgBytesPerSec)\
            *1000);
    pAU->DTS = pAU->CTS;

    pAU->attribute  = 0;
    pAU->frag       = M4OSA_NULL;
    pAU->nbFrag     = 0;
    pAU->stream     = M4OSA_NULL;
    pAU->size       = size_read;

    /* Change states */
    c->m_state = M4PCMR_kReading; /* Not changed ... */
    c->m_microState = M4PCMR_kReading_nextAU; /* AU is read and can be deleted */

    /* Check if there is another AU to read */
    /* ie: if decoded nb of bytes = nb of bytes to decode,
         it means there is no more AU to decode */
    if(c->m_offset >= c->m_decoderConfig.DataLength)
    {
        return M4WAR_NO_MORE_AU;
    }

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR M4PCMR_freeAU(M4OSA_Context context, M4SYS_StreamID streamID, M4SYS_AccessUnit* pAU)
 * @brief   This function frees the AU provided in parameter
 * @note    This function :
 *          - Verifies that the current reader's state allows to free an AU
 *          - Free dataAddress field of AU structure
 *          - Change state of the reader in 'Reading' (not useful...)
 *          - Change Micro state 'Reading' in M4PCMR_kReading (another AU can be read)
 * @param   context: (IN/OUT) PCM Reader context
 * @param   streamID: (IN) Stream selection
 * @param   pAU: (IN) Acces Unit Structure
 * @return  M4NO_ERROR  there is no error
 * @return  M4ERR_PARAMETER at least one parameter is NULL
 * @return  M4ERR_STATE this function cannot be called now
 ************************************************************************
 */
M4OSA_ERR M4PCMR_freeAU(M4OSA_Context context, M4SYS_StreamID streamID, M4SYS_AccessUnit* pAU)
{
    M4PCMR_Context *c = (M4PCMR_Context *)context;

    /* Check parameters */
    if((M4OSA_NULL == context ) || (M4OSA_NULL == pAU))
    {
        return M4ERR_PARAMETER;
    }

    /* Check Reader's state */
    if(c->m_state != M4PCMR_kReading && c->m_microState != M4PCMR_kReading_nextAU)
    {
        return M4ERR_STATE;
    }

    pAU->dataAddress = M4OSA_NULL;

    /* Change states */
    c->m_state = M4PCMR_kReading; /* Not changed ... */
    c->m_microState = M4PCMR_kReading; /* AU is deleted, another AU can be read */

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR M4PCMR_seek(M4OSA_Context context, M4SYS_StreamID* pStreamID,
                         M4OSA_Time time, M4SYS_seekAccessMode seekAccessMode,
                         M4OSA_Time* pObtainCTS[])
 * @brief   This function seeks into the PCM file at the provided time
 * @note    This function :
 *          - Verifies that the current reader's state allows to seek
 *          - Determines from provided time m_offset to seek in file
 *          - If m_offset is correct, seek in file
 *          - Update new m_offset in PCM reader context
 * @param   context: (IN/OUT) PCM Reader context
 * @param   pStreamID: (IN) Stream selection (not used, only 1 stream)
 * @param   time: (IN) Targeted time
 * @param   seekMode: (IN) Selects the seek access mode
 * @param   pObtainCTS[]: (OUT) Returned Time (not used)
 * @return  M4NO_ERROR              there is no error
 * @return  M4ERR_PARAMETER         at least one parameter is NULL
 * @return  M4ERR_ALLOC             there is no more memory available
 * @return  M4ERR_STATE             this function cannot be called now
 * @return  M4WAR_INVALID_TIME      Specified time is not reachable
 * @param   M4ERR_NOT_IMPLEMENTED   This seek mode is not implemented yet
 ************************************************************************
 */
M4OSA_ERR M4PCMR_seek(M4OSA_Context context, M4SYS_StreamID* pStreamID, M4OSA_Time time,
                      M4SYS_SeekAccessMode seekAccessMode, M4OSA_Time* pObtainCTS)
{
    M4PCMR_Context *c = (M4PCMR_Context *)context;
    M4OSA_ERR err = M4NO_ERROR;
    M4OSA_UInt32 offset;
    M4OSA_UInt32 alignment;
    M4OSA_UInt32 size_read;

    /* Check parameters */
    if((M4OSA_NULL == context) || (M4OSA_NULL == pStreamID))
    {
        return M4ERR_PARAMETER;
    }

    /* Check Reader's state */
    if(c->m_state != M4PCMR_kOpening_streamRetrieved && c->m_state != M4PCMR_kReading)
    {
        return M4ERR_STATE;
    }

    switch(seekAccessMode)
    {
        case M4SYS_kBeginning:
            /* Determine m_offset from time*/
            offset =
                (M4OSA_UInt32)(time * ((M4OSA_Double)(c->m_decoderConfig.AvgBytesPerSec) / 1000));
            /** check the alignment on sample boundary */
            alignment = c->m_decoderConfig.nbChannels*c->m_decoderConfig.BitsPerSample/8;
            if (offset%alignment != 0)
            {
                offset -= offset%alignment;
            }
            /*add the header offset*/
            offset += c->m_dataStartOffset;
            /* If m_offset is over file size -> Invalid time */
            if (offset > (c->m_dataStartOffset + c->m_decoderConfig.DataLength))
            {
                return M4WAR_INVALID_TIME;
            }
            else
            {
                /* Seek file */
                size_read = offset;
                err = c->m_pFileReadFunc->seek(c->m_fileContext, M4OSA_kFileSeekBeginning,
                    (M4OSA_FilePosition *) &size_read);
                if(M4NO_ERROR != err)
                {
                    return err;
                }
                /* Update m_offset in M4PCMR_context */
                c->m_offset = offset - c->m_dataStartOffset;
            }
            break;

        default:
            return M4ERR_NOT_IMPLEMENTED;
    }

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR M4PCMR_closeRead(M4OSA_Context context)
 * @brief   This function closes PCM file, and frees context
 * @note    This function :
 *          - Verifies that the current reader's state allows close the PCM file
 *          - Closes the file
 *          - Free structures
 * @param   context: (IN/OUT) PCM Reader context
 * @return  M4NO_ERROR              there is no error
 * @return  M4ERR_PARAMETER         at least one parameter is NULL
 * @return  M4ERR_STATE             this function cannot be called now
 ************************************************************************
 */
M4OSA_ERR M4PCMR_closeRead(M4OSA_Context context)
{
    M4PCMR_Context *c = (M4PCMR_Context *)context;
    M4OSA_ERR err = M4NO_ERROR;

    /* Check parameters */
    if(M4OSA_NULL == context)
    {
        return M4ERR_PARAMETER;
    }

    if(c->m_pDecoderSpecInfo != M4OSA_NULL)
    {
        free(c->m_pDecoderSpecInfo);
    }

    /* Check Reader's state */
    if(c->m_state != M4PCMR_kReading)
    {
        return M4ERR_STATE;
    }
    else if(c->m_microState == M4PCMR_kReading_nextAU)
    {
        return M4ERR_STATE;
    }

    if (M4OSA_NULL != c->m_pAuBuffer)
    {
        free(c->m_pAuBuffer);
    }

    /* Close the file */
    if (M4OSA_NULL != c->m_pFileReadFunc)
    {
        err = c->m_pFileReadFunc->closeRead(c->m_fileContext);
    }

    /* Free internal context */
    if (M4OSA_NULL != c)
    {
        free(c);
    }

    return err;
}

/**
 ************************************************************************
 * M4OSA_ERR M4PCMR_getOption(M4OSA_Context context, M4PCMR_OptionID optionID,
 *                                M4OSA_DataOption* pValue)
 * @brief   This function get option of the PCM Reader
 * @note    This function :
 *          - Verifies that the current reader's state allows to get an option
 *          - Return corresponding option value
 * @param   context: (IN/OUT) PCM Reader context
 * @param   optionID: (IN) ID of the option to get
 * @param   pValue: (OUT) Variable where the option value is returned
 * @return  M4NO_ERROR              there is no error.
 * @return  M4ERR_PARAMETER         at least one parameter is NULL.
 * @return  M4ERR_BAD_OPTION_ID     the optionID is not a valid one.
 * @return  M4ERR_STATE             this option is not available now.
 * @return  M4ERR_NOT_IMPLEMENTED   this option is not implemented
 ************************************************************************
 */
M4OSA_ERR M4PCMR_getOption(M4OSA_Context context, M4PCMR_OptionID optionID,
                             M4OSA_DataOption* pValue)
{
    M4PCMR_Context *c =(M4PCMR_Context *)context;

    /* Check parameters */
    if(M4OSA_NULL == context)
    {
        return M4ERR_PARAMETER;
    }

    /* Check reader's state */
    if((c->m_state != M4PCMR_kOpening) && (c->m_state != M4PCMR_kOpening_streamRetrieved)\
         && (c->m_state != M4PCMR_kReading))
    {
        return M4ERR_STATE;
    }

    /* Depend of the OptionID, the value to return is different */
    switch(optionID)
    {
        case M4PCMR_kPCMblockSize:
            *pValue = &c->m_blockSize;
            break;

        default:
            return M4ERR_BAD_OPTION_ID;
    }

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR M4PCMR_setOption(M4OSA_Context context, M4PCMR_OptionID optionID,
 *                                 M4OSA_DataOption Value)
 * @brief   This function set option of the PCM Reader
 * @note    This function :
 *          - Verifies that the current reader's state allows to set an option
 *          - Set corresponding option value
 * @param   context: (IN/OUT) PCM Reader context
 * @param   optionID: (IN) ID of the option to get
 * @param   Value: (IN) Variable where the option value is stored
 * @return  M4NO_ERROR              there is no error.
 * @return  M4ERR_PARAMETER         at least one parameter is NULL.
 * @return  M4ERR_BAD_OPTION_ID     the optionID is not a valid one.
 * @return  M4ERR_STATE             this option is not available now.
 * @return  M4ERR_NOT_IMPLEMENTED   this option is not implemented
 ************************************************************************
 */
M4OSA_ERR M4PCMR_setOption(M4OSA_Context context, M4PCMR_OptionID optionID, M4OSA_DataOption Value)
{
    M4PCMR_Context *c =(M4PCMR_Context *)context;

    /* Check parameters */
    if(context == M4OSA_NULL)
    {
        return M4ERR_PARAMETER;
    }

    /* Check reader's state */
    if((c->m_state != M4PCMR_kOpening) && (c->m_state != M4PCMR_kOpening_streamRetrieved)\
         && (c->m_state != M4PCMR_kReading))
    {
        return M4ERR_STATE;
    }

    /* Depend of the OptionID, the value to set is different */
    switch(optionID)
    {
        case M4PCMR_kPCMblockSize:
            c->m_blockSize = (M4OSA_UInt32)Value;
            break;

        default:
            return M4ERR_BAD_OPTION_ID;
    }

    return M4NO_ERROR;
}

/*********************************************************/
M4OSA_ERR M4PCMR_getVersion (M4_VersionInfo *pVersion)
/*********************************************************/
{
    M4OSA_TRACE1_1("M4PCMR_getVersion called with pVersion: 0x%x", pVersion);
    M4OSA_DEBUG_IF1(((M4OSA_UInt32) pVersion == 0),M4ERR_PARAMETER,
         "pVersion is NULL in M4PCMR_getVersion");

    pVersion->m_major = M4PCMR_VERSION_MAJOR;
    pVersion->m_minor = M4PCMR_VERSION_MINOR;
    pVersion->m_revision = M4PCMR_VERSION_REVISION;

    return M4NO_ERROR;
}
