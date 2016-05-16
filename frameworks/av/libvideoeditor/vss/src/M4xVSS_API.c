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
 * @file    M4xVSS_API.c
 * @brief    API of eXtended Video Studio Service (Video Studio 2.1)
 * @note
 ******************************************************************************
 */

/**
 * OSAL main types and errors ***/
#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4OSA_Memory.h"
#include "M4OSA_Debug.h"
#include "M4OSA_FileReader.h"
#include "M4OSA_FileWriter.h"
#include "M4OSA_CoreID.h"
#include "M4OSA_CharStar.h"
// StageFright encoders require %16 resolution
#include "M4ENCODER_common.h"
#include "M4DECODER_Common.h"
#include "VideoEditorVideoDecoder.h"

/**
 * VSS 3GPP API definition */
#include "M4VSS3GPP_ErrorCodes.h"

/*************************
Begin of xVSS API
 **************************/

#include "M4xVSS_API.h"
#include "M4xVSS_Internal.h"

/* RC: to delete unecessary temp files on the fly */
#include "M4VSS3GPP_InternalTypes.h"
#include <utils/Log.h>

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_Init(M4OSA_Context* pContext, M4xVSS_InitParams* pParams)
 * @brief        This function initializes the xVSS
 * @note        Initializes the xVSS edit operation (allocates an execution context).
 *
 * @param    pContext            (OUT) Pointer on the xVSS edit context to allocate
 * @param    params                (IN) Parameters mandatory for xVSS
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_ALLOC:        Memory allocation has failed
 ******************************************************************************
 */

M4OSA_ERR M4xVSS_Init( M4OSA_Context *pContext, M4xVSS_InitParams *pParams )
{
    M4xVSS_Context *xVSS_context;
    M4OSA_UInt32 length = 0, i;

    if( pParams == M4OSA_NULL )
    {
        M4OSA_TRACE1_0("Parameter structure for M4xVSS_Init function is NULL");
        return M4ERR_PARAMETER;
    }

    if( pParams->pFileReadPtr == M4OSA_NULL
        || pParams->pFileWritePtr == M4OSA_NULL )
    {
        M4OSA_TRACE1_0(
            "pFileReadPtr or pFileWritePtr in M4xVSS_InitParams structure is NULL");
        return M4ERR_PARAMETER;
    }

    xVSS_context = (M4xVSS_Context *)M4OSA_32bitAlignedMalloc(sizeof(M4xVSS_Context), M4VS,
        (M4OSA_Char *)"Context of the xVSS layer");

    if( xVSS_context == M4OSA_NULL )
    {
        M4OSA_TRACE1_0("Allocation error in M4xVSS_Init");
        return M4ERR_ALLOC;
    }

    /* Initialize file read/write functions pointers */
    xVSS_context->pFileReadPtr = pParams->pFileReadPtr;
    xVSS_context->pFileWritePtr = pParams->pFileWritePtr;

    /*UTF Conversion support: copy conversion functions pointers and allocate the temporary
     buffer*/
    if( pParams->pConvFromUTF8Fct != M4OSA_NULL )
    {
        if( pParams->pConvToUTF8Fct != M4OSA_NULL )
        {
            xVSS_context->UTFConversionContext.pConvFromUTF8Fct =
                pParams->pConvFromUTF8Fct;
            xVSS_context->UTFConversionContext.pConvToUTF8Fct =
                pParams->pConvToUTF8Fct;
            xVSS_context->UTFConversionContext.m_TempOutConversionSize =
                UTF_CONVERSION_BUFFER_SIZE;
            xVSS_context->UTFConversionContext.pTempOutConversionBuffer =
                (M4OSA_Void *)M4OSA_32bitAlignedMalloc(UTF_CONVERSION_BUFFER_SIZE
                * sizeof(M4OSA_UInt8),
                M4VA, (M4OSA_Char *)"M4xVSS_Init: UTF conversion buffer");

            if( M4OSA_NULL
                == xVSS_context->UTFConversionContext.pTempOutConversionBuffer )
            {
                M4OSA_TRACE1_0("Allocation error in M4xVSS_Init");
                free(xVSS_context->pTempPath);
                xVSS_context->pTempPath = M4OSA_NULL;
                free(xVSS_context);
                xVSS_context = M4OSA_NULL;
                return M4ERR_ALLOC;
            }
        }
        else
        {
            M4OSA_TRACE1_0("M4xVSS_Init: one UTF conversion pointer is null and the other\
                           is not null");
            free(xVSS_context->pTempPath);
            xVSS_context->pTempPath = M4OSA_NULL;
            free(xVSS_context);
            xVSS_context = M4OSA_NULL;
            return M4ERR_PARAMETER;
        }
    }
    else
    {
        xVSS_context->UTFConversionContext.pConvFromUTF8Fct = M4OSA_NULL;
        xVSS_context->UTFConversionContext.pConvToUTF8Fct = M4OSA_NULL;
        xVSS_context->UTFConversionContext.m_TempOutConversionSize = 0;
        xVSS_context->UTFConversionContext.pTempOutConversionBuffer =
            M4OSA_NULL;
    }

    if( pParams->pTempPath != M4OSA_NULL )
    {
        /*No need to convert into UTF8 as all input of xVSS are in UTF8
        (the conversion customer format into UTF8
        is done in VA/VAL)*/
        xVSS_context->pTempPath =
            (M4OSA_Void *)M4OSA_32bitAlignedMalloc(strlen(pParams->pTempPath) + 1,
            M4VS, (M4OSA_Char *)"xVSS Path for temporary files");

        if( xVSS_context->pTempPath == M4OSA_NULL )
        {
            M4OSA_TRACE1_0("Allocation error in M4xVSS_Init");
            return M4ERR_ALLOC;
        }
        memcpy((void *)xVSS_context->pTempPath, (void *)pParams->pTempPath,
            strlen(pParams->pTempPath) + 1);
        /* TODO: Check that no previous xVSS temporary files are present ? */
    }
    else
    {
        M4OSA_TRACE1_0("Path for temporary files is NULL");
        free(xVSS_context);
        xVSS_context = M4OSA_NULL;
        return M4ERR_PARAMETER;
    }

    xVSS_context->pSettings =
        (M4VSS3GPP_EditSettings *)M4OSA_32bitAlignedMalloc(sizeof(M4VSS3GPP_EditSettings),
        M4VS, (M4OSA_Char *)"Copy of VSS structure");

    if( xVSS_context->pSettings == M4OSA_NULL )
    {
        M4OSA_TRACE1_0("Allocation error in M4xVSS_Init");
        free(xVSS_context->pTempPath);
        xVSS_context->pTempPath = M4OSA_NULL;
        free(xVSS_context);
        xVSS_context = M4OSA_NULL;
        return M4ERR_ALLOC;
    }

    /* Initialize pointers in pSettings */
    xVSS_context->pSettings->pClipList = M4OSA_NULL;
    xVSS_context->pSettings->pTransitionList = M4OSA_NULL;
    xVSS_context->pSettings->Effects = M4OSA_NULL; /* RC */
    xVSS_context->pSettings->xVSS.pBGMtrack = M4OSA_NULL;

    /* This is used to know if the user has added or removed some medias */
    xVSS_context->previousClipNumber = 0;

    /* "State machine" */
    xVSS_context->editingStep = 0;
    xVSS_context->analyseStep = 0;

    xVSS_context->pcmPreviewFile = M4OSA_NULL;

    /* Initialize Pto3GPP and MCS lists */
    xVSS_context->pMCSparamsList = M4OSA_NULL;
    xVSS_context->pPTo3GPPparamsList = M4OSA_NULL;
    xVSS_context->pPTo3GPPcurrentParams = M4OSA_NULL;
    xVSS_context->pMCScurrentParams = M4OSA_NULL;

    xVSS_context->tempFileIndex = 0;

    xVSS_context->targetedBitrate = 0;

    xVSS_context->targetedTimescale = 0;

    xVSS_context->pAudioMixContext = M4OSA_NULL;
    xVSS_context->pAudioMixSettings = M4OSA_NULL;

    /*FB: initialize to avoid crash when error during the editing*/
    xVSS_context->pCurrentEditSettings = M4OSA_NULL;

    /* Initialize state if all initializations are corrects */
    xVSS_context->m_state = M4xVSS_kStateInitialized;

    /* initialize MCS context*/
    xVSS_context->pMCS_Ctxt = M4OSA_NULL;

    *pContext = xVSS_context;

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * prototype    M4xVSS_ReduceTranscode
 * @brief        This function changes the given editing structure in order to
 *                minimize the transcoding time.
 * @note        The xVSS analyses this structure, and if needed, changes the
 *                output parameters (Video codec, video size, audio codec,
 *                audio nb of channels) to minimize the transcoding time.
 *
 * @param    pContext            (OUT) Pointer on the xVSS edit context to allocate
 * @param    pSettings            (IN) Edition settings (allocated by the user)
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_ALLOC:        Memory allocation has failed
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_ReduceTranscode( M4OSA_Context pContext,
                                 M4VSS3GPP_EditSettings *pSettings )
{
    M4xVSS_Context *xVSS_context = (M4xVSS_Context *)pContext;
    M4OSA_ERR err = M4NO_ERROR;
    M4VIDEOEDITING_ClipProperties fileProperties;
    M4OSA_UInt8 i, j;
    M4OSA_Bool bAudioTransition = M4OSA_FALSE;
    M4OSA_Bool bIsBGMReplace = M4OSA_FALSE;
    M4OSA_Bool bFound;
    M4OSA_UInt32 videoConfig[9] =
    {
        0, 0, 0, 0, 0, 0, 0, 0, 0
    };
    /** Index <-> Video config **/
    /* 0:        H263  SQCIF        */
    /* 1:        H263  QCIF        */
    /* 2:        H263  CIF        */
    /* 3:        MPEG4 SQCIF        */
    /* 4:        MPEG4 QQVGA        */
    /* 5:        MPEG4 QCIF        */
    /* 6:        MPEG4 QVGA        */
    /* 7:        MPEG4 CIF        */
    /* 8:        MPEG4 VGA        */
    /****************************/
    M4OSA_UInt32 audioConfig[3] =
    {
        0, 0, 0
    };
    /** Index <-> Audio config **/
    /* 0:    AMR                    */
    /* 1:    AAC    16kHz mono        */
    /* 2:    AAC 16kHz stereo    */
    /****************************/

    /* Check state */
    if( xVSS_context->m_state != M4xVSS_kStateInitialized \
        && xVSS_context->m_state != M4xVSS_kStateOpened )
    {
        M4OSA_TRACE1_1(
            "Bad state when calling M4xVSS_ReduceTranscode function! State is %d",
            xVSS_context->m_state);
        return M4ERR_STATE;
    }

    /* Check number of clips */
    if( pSettings->uiClipNumber == 0 )
    {
        M4OSA_TRACE1_0("The number of input clip must be greater than 0 !");
        return M4ERR_PARAMETER;
    }

    /* Check if there is a background music, and if its audio will replace input clip audio */
    if( pSettings->xVSS.pBGMtrack != M4OSA_NULL )
    {
        if( pSettings->xVSS.pBGMtrack->uiAddVolume == 100 )
        {
            bIsBGMReplace = M4OSA_TRUE;
        }
    }

    /* Parse all clips, and give occurences of each combination */
    for ( i = 0; i < pSettings->uiClipNumber; i++ )
    {
        /* We ignore JPG input files as they are always transcoded */
        if( pSettings->pClipList[i]->FileType == M4VIDEOEDITING_kFileType_3GPP )
        {
            /**
            * UTF conversion: convert into the customer format*/
            M4OSA_Void *pDecodedPath = pSettings->pClipList[i]->pFile;
            M4OSA_UInt32 ConvertedSize = 0;

            if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct
                != M4OSA_NULL && xVSS_context->
                UTFConversionContext.pTempOutConversionBuffer
                != M4OSA_NULL )
            {
                err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                    (M4OSA_Void *)pSettings->pClipList[i]->pFile,
                    (M4OSA_Void *)xVSS_context->
                    UTFConversionContext.pTempOutConversionBuffer,
                    &ConvertedSize);

                if( err != M4NO_ERROR )
                {
                    M4OSA_TRACE1_1("M4xVSS_ReduceTranscode:\
                                   M4xVSS_internalConvertFromUTF8 returns err: 0x%x", err);
                    return err;
                }
                pDecodedPath =
                    xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
            }
            /**
            * End of the utf conversion, now use the converted path*/
            err = M4xVSS_internalGetProperties(xVSS_context, pDecodedPath,
                &fileProperties);

            //err = M4xVSS_internalGetProperties(xVSS_context, pSettings->pClipList[i]->pFile,
            //     &fileProperties);
            if( err != M4NO_ERROR )
            {
                M4OSA_TRACE1_1(
                    "M4xVSS_sendCommand: M4xVSS_internalGetProperties returned 0x%x",
                    err);
                /* TODO: Translate error code of MCS to an xVSS error code ? */
                return err;
            }

            /* Check best video settings */
            if( fileProperties.uiVideoWidth == 128
                && fileProperties.uiVideoHeight == 96 )
            {
                if( fileProperties.VideoStreamType == M4VIDEOEDITING_kH263 )
                {
                    videoConfig[0] += fileProperties.uiClipVideoDuration;
                }
                else if( ( fileProperties.VideoStreamType
                    == M4VIDEOEDITING_kMPEG4) \
                    || (fileProperties.VideoStreamType == M4VIDEOEDITING_kH264) )
                {
                    videoConfig[3] += fileProperties.uiClipVideoDuration;
                }
            }
            else if( fileProperties.uiVideoWidth == 160
                && fileProperties.uiVideoHeight == 120 )
            {
                if( ( fileProperties.VideoStreamType == M4VIDEOEDITING_kMPEG4) \
                    || (fileProperties.VideoStreamType == M4VIDEOEDITING_kH264) )
                {
                    videoConfig[4] += fileProperties.uiClipVideoDuration;
                }
            }
            else if( fileProperties.uiVideoWidth == 176
                && fileProperties.uiVideoHeight == 144 )
            {
                if( fileProperties.VideoStreamType == M4VIDEOEDITING_kH263 )
                {
                    videoConfig[1] += fileProperties.uiClipVideoDuration;
                }
                else if( ( fileProperties.VideoStreamType
                    == M4VIDEOEDITING_kMPEG4) \
                    || (fileProperties.VideoStreamType == M4VIDEOEDITING_kH264) )
                {
                    videoConfig[5] += fileProperties.uiClipVideoDuration;
                }
            }
            else if( fileProperties.uiVideoWidth == 320
                && fileProperties.uiVideoHeight == 240 )
            {
                if( ( fileProperties.VideoStreamType == M4VIDEOEDITING_kMPEG4) \
                    || (fileProperties.VideoStreamType == M4VIDEOEDITING_kH264) )
                {
                    videoConfig[6] += fileProperties.uiClipVideoDuration;
                }
            }
            else if( fileProperties.uiVideoWidth == 352
                && fileProperties.uiVideoHeight == 288 )
            {
                if( fileProperties.VideoStreamType == M4VIDEOEDITING_kH263 )
                {
                    videoConfig[2] += fileProperties.uiClipVideoDuration;
                }
                else if( ( fileProperties.VideoStreamType
                    == M4VIDEOEDITING_kMPEG4) \
                    || (fileProperties.VideoStreamType == M4VIDEOEDITING_kH264) )
                {
                    videoConfig[7] += fileProperties.uiClipVideoDuration;
                }
            }
            else if( fileProperties.uiVideoWidth == 640
                && fileProperties.uiVideoHeight == 480 )
            {
                if( ( fileProperties.VideoStreamType == M4VIDEOEDITING_kMPEG4) \
                    || (fileProperties.VideoStreamType == M4VIDEOEDITING_kH264) )
                {
                    videoConfig[8] += fileProperties.uiClipVideoDuration;
                }
            }

            /* If there is a BGM that replaces existing audio track, we do not care about
            audio track as it will be replaced */
            /* If not, we try to minimize audio reencoding */
            if( bIsBGMReplace == M4OSA_FALSE )
            {
                if( fileProperties.AudioStreamType == M4VIDEOEDITING_kAAC )
                {
                    if( fileProperties.uiSamplingFrequency == 16000 && \
                        fileProperties.uiNbChannels == 1 )
                    {
                        audioConfig[1] += fileProperties.uiClipAudioDuration;
                    }
                    else if( fileProperties.uiSamplingFrequency == 16000 && \
                        fileProperties.uiNbChannels == 2 )
                    {
                        audioConfig[2] += fileProperties.uiClipAudioDuration;
                    }
                }
                else if( fileProperties.AudioStreamType
                    == M4VIDEOEDITING_kAMR_NB )
                {
                    audioConfig[0] += fileProperties.uiClipAudioDuration;
                }
            }
        }
    }

    /* Find best output video format (the most occuring combination) */
    j = 0;
    bFound = M4OSA_FALSE;

    for ( i = 0; i < 9; i++ )
    {
        if( videoConfig[i] >= videoConfig[j] )
        {
            j = i;
            bFound = M4OSA_TRUE;
        }
    }

    if( bFound )
    {
        switch( j )
        {
            case 0:
                pSettings->xVSS.outputVideoFormat = M4VIDEOEDITING_kH263;
                pSettings->xVSS.outputVideoSize = M4VIDEOEDITING_kSQCIF;
                break;

            case 1:
                pSettings->xVSS.outputVideoFormat = M4VIDEOEDITING_kH263;
                pSettings->xVSS.outputVideoSize = M4VIDEOEDITING_kQCIF;
                break;

            case 2:
                pSettings->xVSS.outputVideoFormat = M4VIDEOEDITING_kH263;
                pSettings->xVSS.outputVideoSize = M4VIDEOEDITING_kCIF;
                break;

            case 3:
                pSettings->xVSS.outputVideoFormat =
                    (fileProperties.VideoStreamType == M4VIDEOEDITING_kMPEG4)
                    ? M4VIDEOEDITING_kMPEG4 : M4VIDEOEDITING_kH264;
                pSettings->xVSS.outputVideoSize = M4VIDEOEDITING_kSQCIF;
                break;

            case 4:
                pSettings->xVSS.outputVideoFormat =
                    (fileProperties.VideoStreamType == M4VIDEOEDITING_kMPEG4)
                    ? M4VIDEOEDITING_kMPEG4 : M4VIDEOEDITING_kH264;
                pSettings->xVSS.outputVideoSize = M4VIDEOEDITING_kQQVGA;
                break;

            case 5:
                pSettings->xVSS.outputVideoFormat =
                    (fileProperties.VideoStreamType == M4VIDEOEDITING_kMPEG4)
                    ? M4VIDEOEDITING_kMPEG4 : M4VIDEOEDITING_kH264;
                pSettings->xVSS.outputVideoSize = M4VIDEOEDITING_kQCIF;
                break;

            case 6:
                pSettings->xVSS.outputVideoFormat =
                    (fileProperties.VideoStreamType == M4VIDEOEDITING_kMPEG4)
                    ? M4VIDEOEDITING_kMPEG4 : M4VIDEOEDITING_kH264;
                pSettings->xVSS.outputVideoSize = M4VIDEOEDITING_kQVGA;
                break;

            case 7:
                pSettings->xVSS.outputVideoFormat =
                    (fileProperties.VideoStreamType == M4VIDEOEDITING_kMPEG4)
                    ? M4VIDEOEDITING_kMPEG4 : M4VIDEOEDITING_kH264;
                pSettings->xVSS.outputVideoSize = M4VIDEOEDITING_kCIF;
                break;

            case 8:
                pSettings->xVSS.outputVideoFormat =
                    (fileProperties.VideoStreamType == M4VIDEOEDITING_kMPEG4)
                    ? M4VIDEOEDITING_kMPEG4 : M4VIDEOEDITING_kH264;
                pSettings->xVSS.outputVideoSize = M4VIDEOEDITING_kVGA;
                break;
        }
    }

    /* Find best output audio format (the most occuring combination) */
    j = 0;
    bFound = M4OSA_FALSE;

    for ( i = 0; i < 3; i++ )
    {
        if( audioConfig[i] >= audioConfig[j] )
        {
            j = i;
            bFound = M4OSA_TRUE;
        }
    }

    if( bFound )
    {
        switch( j )
        {
            case 0:
                pSettings->xVSS.outputAudioFormat = M4VIDEOEDITING_kAMR_NB;
                pSettings->xVSS.bAudioMono = M4OSA_TRUE;
                break;

            case 1:
                pSettings->xVSS.outputAudioFormat = M4VIDEOEDITING_kAAC;
                pSettings->xVSS.bAudioMono = M4OSA_TRUE;
                break;

            case 2:
                pSettings->xVSS.outputAudioFormat = M4VIDEOEDITING_kAAC;
                pSettings->xVSS.bAudioMono = M4OSA_FALSE;
                break;
        }
    }

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_SendCommand(M4OSA_Context pContext,
 *                                         M4VSS3GPP_EditSettings* pSettings)
 * @brief        This function gives to the xVSS an editing structure
 * @note        The xVSS analyses this structure, and prepare edition
 *                This function must be called after M4xVSS_Init, after
 *                M4xVSS_CloseCommand, or after M4xVSS_PreviewStop.
 *                After this function, the user must call M4xVSS_Step until
 *                it returns another error than M4NO_ERROR.
 *
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @param    pSettings            (IN) Edition settings (allocated by the user)
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_ALLOC:        Memory allocation has failed
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_SendCommand( M4OSA_Context pContext,
                             M4VSS3GPP_EditSettings *pSettings )
{
    M4xVSS_Context *xVSS_context = (M4xVSS_Context *)pContext;
    M4OSA_UInt8 i, j;
    M4OSA_UInt8 nbOfSameClip = 0;
    M4OSA_ERR err;
    M4OSA_Bool isNewBGM = M4OSA_TRUE;
    M4xVSS_Pto3GPP_params *pPto3GPP_last = M4OSA_NULL;
    M4xVSS_MCS_params *pMCS_last = M4OSA_NULL;
    M4OSA_UInt32 width, height, samplingFreq;
    M4OSA_Bool bIsTranscoding = M4OSA_FALSE;
    M4OSA_Int32 totalDuration;
    M4OSA_UInt32 outputSamplingFrequency = 0;
    M4OSA_UInt32 length = 0;
    M4OSA_Int8 masterClip = -1;

    i = 0;
    /* Check state */
    if( xVSS_context->m_state != M4xVSS_kStateInitialized \
        && xVSS_context->m_state != M4xVSS_kStateOpened )
    {
        M4OSA_TRACE1_1(
            "Bad state when calling M4xVSS_SendCommand function! State is %d",
            xVSS_context->m_state);
        return M4ERR_STATE;
    }

    /* State is back to initialized to allow call of cleanup function in case of error */
    xVSS_context->m_state = M4xVSS_kStateInitialized;

    /* Check if a previous sendCommand has been called */
    if( xVSS_context->previousClipNumber != 0 )
    {
        M4OSA_UInt32 pCmpResult = 0;

        /* Compare BGM input */
        if( xVSS_context->pSettings->xVSS.pBGMtrack != M4OSA_NULL \
            && pSettings->xVSS.pBGMtrack != M4OSA_NULL )
        {
            pCmpResult = strcmp((const char *)xVSS_context->pSettings->xVSS.pBGMtrack->pFile,
                (const char *)pSettings->xVSS.pBGMtrack->pFile);

            if( pCmpResult == 0 )
            {
                /* Check if audio output parameters have changed */
                if( xVSS_context->pSettings->xVSS.outputAudioFormat ==
                    pSettings->xVSS.outputAudioFormat
                    && xVSS_context->pSettings->xVSS.bAudioMono
                    == pSettings->xVSS.bAudioMono )
                {
                    /* It means that BGM is the same as before, so, no need to redecode it */
                    M4OSA_TRACE2_0(
                        "BGM is the same as before, nothing to decode");
                    isNewBGM = M4OSA_FALSE;
                }
                else
                {
                    /* We need to unallocate PCM preview file path in internal context */
                    if( xVSS_context->pcmPreviewFile != M4OSA_NULL )
                    {
                        free(xVSS_context->pcmPreviewFile);
                        xVSS_context->pcmPreviewFile = M4OSA_NULL;
                    }
                }
            }
            else
            {
                /* We need to unallocate PCM preview file path in internal context */
                if( xVSS_context->pcmPreviewFile != M4OSA_NULL )
                {
                    free(xVSS_context->pcmPreviewFile);
                    xVSS_context->pcmPreviewFile = M4OSA_NULL;
                }
            }
        }

        /* Check if output settings have changed */
        if( xVSS_context->pSettings->xVSS.outputVideoSize
            != pSettings->xVSS.outputVideoSize
            || xVSS_context->pSettings->xVSS.outputVideoFormat
            != pSettings->xVSS.outputVideoFormat
            || xVSS_context->pSettings->xVSS.outputVideoProfile
            != pSettings->xVSS.outputVideoProfile
            || xVSS_context->pSettings->xVSS.outputVideoLevel
            != pSettings->xVSS.outputVideoLevel
            || xVSS_context->pSettings->xVSS.outputAudioFormat
            != pSettings->xVSS.outputAudioFormat
            || xVSS_context->pSettings->xVSS.bAudioMono
            != pSettings->xVSS.bAudioMono
            || xVSS_context->pSettings->xVSS.outputAudioSamplFreq
            != pSettings->xVSS.outputAudioSamplFreq )
        {
            /* If it is the case, we can't reuse already transcoded/converted files */
            /* so, we delete these files and remove them from chained list */
            if( xVSS_context->pPTo3GPPparamsList != M4OSA_NULL )
            {
                M4xVSS_Pto3GPP_params *pParams =
                    xVSS_context->pPTo3GPPparamsList;
                M4xVSS_Pto3GPP_params *pParams_sauv;

                while( pParams != M4OSA_NULL )
                {
                    if( pParams->pFileIn != M4OSA_NULL )
                    {
                        free(pParams->pFileIn);
                        pParams->pFileIn = M4OSA_NULL;
                    }

                    if( pParams->pFileOut != M4OSA_NULL )
                    {
                        /* Delete temporary file */
                        remove((const char *)pParams->pFileOut);
                        free(pParams->pFileOut);
                        pParams->pFileOut = M4OSA_NULL;
                    }

                    if( pParams->pFileTemp != M4OSA_NULL )
                    {
                        /* Delete temporary file */
#ifdef M4xVSS_RESERVED_MOOV_DISK_SPACE

                        remove((const char *)pParams->pFileTemp);
                        free(pParams->pFileTemp);

#endif /*M4xVSS_RESERVED_MOOV_DISK_SPACE*/

                        pParams->pFileTemp = M4OSA_NULL;
                    }
                    pParams_sauv = pParams;
                    pParams = pParams->pNext;
                    free(pParams_sauv);
                    pParams_sauv = M4OSA_NULL;
                }
                xVSS_context->pPTo3GPPparamsList = M4OSA_NULL;
            }

            if( xVSS_context->pMCSparamsList != M4OSA_NULL )
            {
                M4xVSS_MCS_params *pParams = xVSS_context->pMCSparamsList;
                M4xVSS_MCS_params *pParams_sauv;
                M4xVSS_MCS_params *pParams_bgm = M4OSA_NULL;

                while( pParams != M4OSA_NULL )
                {
                    /* Here, we do not delete BGM */
                    if( pParams->isBGM != M4OSA_TRUE )
                    {
                        if( pParams->pFileIn != M4OSA_NULL )
                        {
                            free(pParams->pFileIn);
                            pParams->pFileIn = M4OSA_NULL;
                        }

                        if( pParams->pFileOut != M4OSA_NULL )
                        {
                            /* Delete temporary file */
                            remove((const char *)pParams->pFileOut);
                            free(pParams->pFileOut);
                            pParams->pFileOut = M4OSA_NULL;
                        }

                        if( pParams->pFileTemp != M4OSA_NULL )
                        {
                            /* Delete temporary file */
#ifdef M4xVSS_RESERVED_MOOV_DISK_SPACE

                            remove((const char *)pParams->pFileTemp);
                            free(pParams->pFileTemp);

#endif /*M4xVSS_RESERVED_MOOV_DISK_SPACE*/

                            pParams->pFileTemp = M4OSA_NULL;
                        }
                        pParams_sauv = pParams;
                        pParams = pParams->pNext;
                        free(pParams_sauv);
                        pParams_sauv = M4OSA_NULL;
                    }
                    else
                    {
                        pParams_bgm = pParams;
                        pParams = pParams->pNext;
                        /*PR P4ME00003182 initialize this pointer because the following params
                        element will be deallocated*/
                        if( pParams != M4OSA_NULL
                            && pParams->isBGM != M4OSA_TRUE )
                        {
                            pParams_bgm->pNext = M4OSA_NULL;
                        }
                    }
                }
                xVSS_context->pMCSparamsList = pParams_bgm;
            }
            /* Maybe need to implement framerate changing */
            //xVSS_context->pSettings->videoFrameRate;
        }

        /* Unallocate previous xVSS_context->pSettings structure */
        M4xVSS_freeSettings(xVSS_context->pSettings);

        /*Unallocate output file path*/
        if( xVSS_context->pSettings->pOutputFile != M4OSA_NULL )
        {
            free(xVSS_context->pSettings->pOutputFile);
            xVSS_context->pSettings->pOutputFile = M4OSA_NULL;
        }
        xVSS_context->pSettings->uiOutputPathSize = 0;
        xVSS_context->pOutputFile = M4OSA_NULL;
    }

    /**********************************
    Clips registering
    **********************************/

    /* Copy settings from user given structure to our "local" structure */
    xVSS_context->pSettings->xVSS.outputVideoFormat =
        pSettings->xVSS.outputVideoFormat;
    xVSS_context->pSettings->xVSS.outputVideoProfile =
        pSettings->xVSS.outputVideoProfile;
    xVSS_context->pSettings->xVSS.outputVideoLevel =
        pSettings->xVSS.outputVideoLevel;
    xVSS_context->pSettings->xVSS.outputVideoSize =
        pSettings->xVSS.outputVideoSize;
    xVSS_context->pSettings->xVSS.outputAudioFormat =
        pSettings->xVSS.outputAudioFormat;
    xVSS_context->pSettings->xVSS.bAudioMono = pSettings->xVSS.bAudioMono;
    xVSS_context->pSettings->xVSS.outputAudioSamplFreq =
        pSettings->xVSS.outputAudioSamplFreq;
    /*xVSS_context->pSettings->pOutputFile = pSettings->pOutputFile;*/
    /*FB: VAL CR P4ME00003076
    The output video and audio bitrate are given by the user in the edition settings structure*/
    xVSS_context->pSettings->xVSS.outputVideoBitrate =
        pSettings->xVSS.outputVideoBitrate;
    xVSS_context->pSettings->xVSS.outputAudioBitrate =
        pSettings->xVSS.outputAudioBitrate;
    xVSS_context->pSettings->PTVolLevel = pSettings->PTVolLevel;

    /*FB: bug fix if the output path is given in M4xVSS_sendCommand*/

    if( pSettings->pOutputFile != M4OSA_NULL
        && pSettings->uiOutputPathSize > 0 )
    {
        M4OSA_Void *pDecodedPath = pSettings->pOutputFile;
        /*As all inputs of the xVSS are in UTF8, convert the output file path into the
        customer format*/
        if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct != M4OSA_NULL
            && xVSS_context->UTFConversionContext.pTempOutConversionBuffer
            != M4OSA_NULL )
        {
            err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                (M4OSA_Void *)pSettings->pOutputFile,
                (M4OSA_Void *)xVSS_context->
                UTFConversionContext.pTempOutConversionBuffer, &length);

            if( err != M4NO_ERROR )
            {
                M4OSA_TRACE1_1("M4xVSS_SendCommand:\
                               M4xVSS_internalConvertFromUTF8 returns err: 0x%x", err);
                return err;
            }
            pDecodedPath =
                xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
            pSettings->uiOutputPathSize = length;
        }

        xVSS_context->pSettings->pOutputFile = (M4OSA_Void *)M4OSA_32bitAlignedMalloc \
            (pSettings->uiOutputPathSize + 1, M4VS,
            (M4OSA_Char *)"output file path");

        if( xVSS_context->pSettings->pOutputFile == M4OSA_NULL )
        {
            M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
            /*FB: to avoid leaks when there is an error in the send command*/
            /* Free Send command */
            M4xVSS_freeCommand(xVSS_context);
            /**/
            return M4ERR_ALLOC;
        }
        memcpy((void *)xVSS_context->pSettings->pOutputFile,
            (void *)pDecodedPath, pSettings->uiOutputPathSize + 1);
        xVSS_context->pSettings->uiOutputPathSize = pSettings->uiOutputPathSize;
        xVSS_context->pOutputFile = xVSS_context->pSettings->pOutputFile;
    }
    else
    {
        xVSS_context->pSettings->pOutputFile = M4OSA_NULL;
        xVSS_context->pSettings->uiOutputPathSize = 0;
        xVSS_context->pOutputFile = M4OSA_NULL;
    }
    xVSS_context->pSettings->pTemporaryFile = pSettings->pTemporaryFile;
    xVSS_context->pSettings->uiClipNumber = pSettings->uiClipNumber;
    xVSS_context->pSettings->videoFrameRate = pSettings->videoFrameRate;
    xVSS_context->pSettings->uiMasterClip =
        0; /* With VSS 2.0, this new param is mandatory */
    xVSS_context->pSettings->xVSS.pTextRenderingFct =
        pSettings->xVSS.pTextRenderingFct; /* CR text handling */
    xVSS_context->pSettings->xVSS.outputFileSize =
        pSettings->xVSS.outputFileSize;

    if( pSettings->xVSS.outputFileSize != 0 \
        && pSettings->xVSS.outputAudioFormat != M4VIDEOEDITING_kAMR_NB )
    {
        M4OSA_TRACE1_0("M4xVSS_SendCommand: Impossible to limit file\
                       size with other audio output than AAC");
        return M4ERR_PARAMETER;
    }
    xVSS_context->nbStepTotal = 0;
    xVSS_context->currentStep = 0;

    if( xVSS_context->pSettings->xVSS.outputVideoFormat != M4VIDEOEDITING_kMPEG4
        && xVSS_context->pSettings->xVSS.outputVideoFormat
        != M4VIDEOEDITING_kH263
        && xVSS_context->pSettings->xVSS.outputVideoFormat
        != M4VIDEOEDITING_kH264 )
    {
        xVSS_context->pSettings->xVSS.outputVideoFormat =
            M4VIDEOEDITING_kNoneVideo;
    }

    /* Get output width/height */
    switch( xVSS_context->pSettings->xVSS.outputVideoSize )
    {
        case M4VIDEOEDITING_kSQCIF:
            width = 128;
            height = 96;
            break;

        case M4VIDEOEDITING_kQQVGA:
            width = 160;
            height = 120;
            break;

        case M4VIDEOEDITING_kQCIF:
            width = 176;
            height = 144;
            break;

        case M4VIDEOEDITING_kQVGA:
            width = 320;
            height = 240;
            break;

        case M4VIDEOEDITING_kCIF:
            width = 352;
            height = 288;
            break;

        case M4VIDEOEDITING_kVGA:
            width = 640;
            height = 480;
            break;
            /* +PR LV5807 */
        case M4VIDEOEDITING_kWVGA:
            width = 800;
            height = 480;
            break;

        case M4VIDEOEDITING_kNTSC:
            width = 720;
            height = 480;
            break;
            /* -PR LV5807 */
            /* +CR Google */
        case M4VIDEOEDITING_k640_360:
            width = 640;
            height = 360;
            break;

        case M4VIDEOEDITING_k854_480:

            // StageFright encoders require %16 resolution

            width = M4ENCODER_854_480_Width;

            height = 480;
            break;

        case M4VIDEOEDITING_k1280_720:
            width = 1280;
            height = 720;
            break;

        case M4VIDEOEDITING_k1080_720:
            // StageFright encoders require %16 resolution
            width = M4ENCODER_1080_720_Width;
            height = 720;
            break;

        case M4VIDEOEDITING_k960_720:
            width = 960;
            height = 720;
            break;

        case M4VIDEOEDITING_k1920_1080:
            width = 1920;
            height = M4ENCODER_1920_1080_Height;
            break;

            /* -CR Google */
        default: /* If output video size is not given, we take QCIF size */
            width = 176;
            height = 144;
            xVSS_context->pSettings->xVSS.outputVideoSize =
                M4VIDEOEDITING_kQCIF;
            break;
    }

    /* Get output Sampling frequency */
    switch( xVSS_context->pSettings->xVSS.outputAudioSamplFreq )
    {
        case M4VIDEOEDITING_k8000_ASF:
            samplingFreq = 8000;
            break;

        case M4VIDEOEDITING_k16000_ASF:
            samplingFreq = 16000;
            break;

        case M4VIDEOEDITING_k22050_ASF:
            samplingFreq = 22050;
            break;

        case M4VIDEOEDITING_k24000_ASF:
            samplingFreq = 24000;
            break;

        case M4VIDEOEDITING_k32000_ASF:
            samplingFreq = 32000;
            break;

        case M4VIDEOEDITING_k44100_ASF:
            samplingFreq = 44100;
            break;

        case M4VIDEOEDITING_k48000_ASF:
            samplingFreq = 48000;
            break;

        case M4VIDEOEDITING_kDefault_ASF:
        default:
            if( xVSS_context->pSettings->xVSS.outputAudioFormat
                == M4VIDEOEDITING_kAMR_NB )
            {
                samplingFreq = 8000;
            }
            else if( xVSS_context->pSettings->xVSS.outputAudioFormat
                == M4VIDEOEDITING_kAAC )
            {
                samplingFreq = 16000;
            }
            else
            {
                samplingFreq = 0;
            }
            break;
    }

    /* Allocate clip/transitions if clip number is not null ... */
    if( 0 < xVSS_context->pSettings->uiClipNumber )
    {
        if( xVSS_context->pSettings->pClipList != M4OSA_NULL )
        {
            free((xVSS_context->pSettings->pClipList));
            xVSS_context->pSettings->pClipList = M4OSA_NULL;
        }

        if( xVSS_context->pSettings->pTransitionList != M4OSA_NULL )
        {
            free(xVSS_context->pSettings->pTransitionList);
            xVSS_context->pSettings->pTransitionList = M4OSA_NULL;
        }

        xVSS_context->pSettings->pClipList =
            (M4VSS3GPP_ClipSettings ** )M4OSA_32bitAlignedMalloc \
            (sizeof(M4VSS3GPP_ClipSettings *)*xVSS_context->pSettings->uiClipNumber,
            M4VS, (M4OSA_Char *)"xVSS, copy of pClipList");

        if( xVSS_context->pSettings->pClipList == M4OSA_NULL )
        {
            M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
            /*FB: to avoid leaks when there is an error in the send command*/
            /* Free Send command */
            M4xVSS_freeCommand(xVSS_context);
            /**/
            return M4ERR_ALLOC;
        }
        /* Set clip list to NULL */
        memset((void *)xVSS_context->pSettings->pClipList,0,
            sizeof(M4VSS3GPP_ClipSettings *)
            *xVSS_context->pSettings->uiClipNumber);

        if( xVSS_context->pSettings->uiClipNumber > 1 )
        {
            xVSS_context->pSettings->pTransitionList =
                (M4VSS3GPP_TransitionSettings ** ) \
                M4OSA_32bitAlignedMalloc(sizeof(M4VSS3GPP_TransitionSettings *)                \
                *(xVSS_context->pSettings->uiClipNumber - 1), M4VS, (M4OSA_Char *) \
                "xVSS, copy of pTransitionList");

            if( xVSS_context->pSettings->pTransitionList == M4OSA_NULL )
            {
                M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                /*FB: to avoid leaks when there is an error in the send command*/
                /* Free Send command */
                M4xVSS_freeCommand(xVSS_context);
                /**/
                return M4ERR_ALLOC;
            }
            /* Set transition list to NULL */
            memset(
                (void *)xVSS_context->pSettings->pTransitionList,0,
                sizeof(M4VSS3GPP_TransitionSettings *)
                *(xVSS_context->pSettings->uiClipNumber - 1));
        }
        else
        {
            xVSS_context->pSettings->pTransitionList = M4OSA_NULL;
        }
    }
    /* else, there is a pb in the input settings structure */
    else
    {
        M4OSA_TRACE1_0("No clip in this settings list !!");
        /*FB: to avoid leaks when there is an error in the send command*/
        /* Free Send command */
        M4xVSS_freeCommand(xVSS_context);
        /**/
        return M4ERR_PARAMETER;
    }

    /* RC Allocate effects settings */
    xVSS_context->pSettings->nbEffects = pSettings->nbEffects;

    if( 0 < xVSS_context->pSettings->nbEffects )
    {
        xVSS_context->pSettings->Effects =
            (M4VSS3GPP_EffectSettings *)M4OSA_32bitAlignedMalloc \
            (xVSS_context->pSettings->nbEffects * sizeof(M4VSS3GPP_EffectSettings),
            M4VS, (M4OSA_Char *)"effects settings");

        if( xVSS_context->pSettings->Effects == M4OSA_NULL )
        {
            M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
            /*FB: to avoid leaks when there is an error in the send command*/
            /* Free Send command */
            M4xVSS_freeCommand(xVSS_context);
            /**/
            return M4ERR_ALLOC;
        }
        /*FB bug fix 19.03.2008: these pointers were not initialized -> crash when free*/
        for ( i = 0; i < xVSS_context->pSettings->nbEffects; i++ )
        {
            xVSS_context->pSettings->Effects[i].xVSS.pFramingFilePath =
                M4OSA_NULL;
            xVSS_context->pSettings->Effects[i].xVSS.pFramingBuffer =
                M4OSA_NULL;
            xVSS_context->pSettings->Effects[i].xVSS.pTextBuffer = M4OSA_NULL;
        }
        /**/
    }

    if( xVSS_context->targetedTimescale == 0 )
    {
        M4OSA_UInt32 pTargetedTimeScale = 0;

        err = M4xVSS_internalGetTargetedTimeScale(xVSS_context, pSettings,
            &pTargetedTimeScale);

        if( M4NO_ERROR != err || pTargetedTimeScale == 0 )
        {
            M4OSA_TRACE1_1("M4xVSS_SendCommand: M4xVSS_internalGetTargetedTimeScale\
                           returned 0x%x", err);
            /*FB: to avoid leaks when there is an error in the send command*/
            /* Free Send command */
            M4xVSS_freeCommand(xVSS_context);
            /**/
            return err;
        }
        xVSS_context->targetedTimescale = pTargetedTimeScale;
    }

    /* Initialize total duration variable */
    totalDuration = 0;

    /* Parsing list of clips given by application, and prepare analyzing */
    for ( i = 0; i < xVSS_context->pSettings->uiClipNumber; i++ )
    {
        /* Allocate current clip */
        xVSS_context->pSettings->pClipList[i] =
            (M4VSS3GPP_ClipSettings *)M4OSA_32bitAlignedMalloc \
            (sizeof(M4VSS3GPP_ClipSettings), M4VS, (M4OSA_Char *)"clip settings");

        if( xVSS_context->pSettings->pClipList[i] == M4OSA_NULL )
        {
            M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
            /*FB: to avoid leaks when there is an error in the send command*/
            /* Free Send command */
            M4xVSS_freeCommand(xVSS_context);
            /**/
            return M4ERR_ALLOC;
        }

        /* Copy clip settings from given structure to our xVSS_context structure */
        err =
            M4xVSS_DuplicateClipSettings(xVSS_context->pSettings->pClipList[i],
            pSettings->pClipList[i], M4OSA_TRUE);

        if( err != M4NO_ERROR )
        {
            M4OSA_TRACE1_1(
                "M4xVSS_SendCommand: M4xVSS_DuplicateClipSettings return error 0x%x",
                err);
            /*FB: to avoid leaks when there is an error in the send command*/
            /* Free Send command */
            M4xVSS_freeCommand(xVSS_context);
            /**/
            return err;
        }

        xVSS_context->pSettings->pClipList[i]->bTranscodingRequired =
            M4OSA_FALSE;

        /* Because there is 1 less transition than clip number */
        if( i < xVSS_context->pSettings->uiClipNumber - 1 )
        {
            xVSS_context->pSettings->pTransitionList[i] =
                (M4VSS3GPP_TransitionSettings
                *)M4OSA_32bitAlignedMalloc(sizeof(M4VSS3GPP_TransitionSettings),
                M4VS, (M4OSA_Char *)"transition settings");

            if( xVSS_context->pSettings->pTransitionList[i] == M4OSA_NULL )
            {
                M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                /*FB: to avoid leaks when there is an error in the send command*/
                /* Free Send command */
                M4xVSS_freeCommand(xVSS_context);
                /**/
                return M4ERR_ALLOC;
            }

            memcpy(
                (void *)xVSS_context->pSettings->pTransitionList[i],
                (void *)pSettings->pTransitionList[i],
                sizeof(M4VSS3GPP_TransitionSettings));
            /* Initialize external effect context to NULL, to know if input jpg has already been
            decoded or not */
            xVSS_context->pSettings->pTransitionList[i]->
                pExtVideoTransitionFctCtxt = M4OSA_NULL;

            switch( xVSS_context->pSettings->
                pTransitionList[i]->VideoTransitionType )
            {
                    /* If transition type is alpha magic, we need to decode input file */
                case M4xVSS_kVideoTransitionType_AlphaMagic:
                    /* Allocate our alpha magic settings structure to have a copy of the
                    provided one */
                    xVSS_context->pSettings->pTransitionList[i]->      \
                     xVSS.transitionSpecific.pAlphaMagicSettings =
                        (M4xVSS_AlphaMagicSettings *)M4OSA_32bitAlignedMalloc \
                        (sizeof(M4xVSS_AlphaMagicSettings), M4VS,
                        (M4OSA_Char *)"Input Alpha magic settings structure");

                    if( xVSS_context->pSettings->pTransitionList[i]-> \
                        xVSS.transitionSpecific.pAlphaMagicSettings == M4OSA_NULL )
                    {
                        M4OSA_TRACE1_0(
                            "Allocation error in M4xVSS_SendCommand");
                        /*FB: to avoid leaks when there is an error in the send command*/
                        /* Free Send command */
                        M4xVSS_freeCommand(xVSS_context);
                        /**/
                        return M4ERR_ALLOC;
                    }
                    /* Copy data from the provided alpha magic settings structure tou our
                    structure */
                    memcpy((void *)xVSS_context->pSettings->
                        pTransitionList[i]-> \
                        xVSS.transitionSpecific.pAlphaMagicSettings,
                        (void *)pSettings->pTransitionList[i]-> \
                        xVSS.transitionSpecific.pAlphaMagicSettings,
                        sizeof(M4xVSS_AlphaMagicSettings));

                    /* Allocate our alpha magic input filename */
                    xVSS_context->pSettings->pTransitionList[i]-> \
                        xVSS.transitionSpecific.pAlphaMagicSettings->
                        pAlphaFilePath = M4OSA_32bitAlignedMalloc(
                        (strlen(pSettings->pTransitionList[i]-> \
                        xVSS.transitionSpecific.pAlphaMagicSettings->pAlphaFilePath)
                        + 1), M4VS, (M4OSA_Char *)"Alpha magic file path");

                    if( xVSS_context->pSettings->pTransitionList[i]-> \
                        xVSS.transitionSpecific.pAlphaMagicSettings->pAlphaFilePath
                        == M4OSA_NULL )
                    {
                        M4OSA_TRACE1_0(
                            "Allocation error in M4xVSS_SendCommand");
                        /*FB: to avoid leaks when there is an error in the send command*/
                        /* Free Send command */
                        M4xVSS_freeCommand(xVSS_context);
                        /**/
                        return M4ERR_ALLOC;
                    }
                    /* Copy data from the provided alpha magic filename to our */
                    M4OSA_chrNCopy(
                        xVSS_context->pSettings->pTransitionList[i]->xVSS.
                        transitionSpecific.pAlphaMagicSettings->
                        pAlphaFilePath,
                        pSettings->pTransitionList[i]->xVSS.
                        transitionSpecific.pAlphaMagicSettings->
                        pAlphaFilePath, strlen(
                        pSettings->pTransitionList[i]->xVSS.
                        transitionSpecific.pAlphaMagicSettings->
                        pAlphaFilePath) + 1);

                    /* Parse all transition to know if the input jpg has already been decoded */
                    for ( j = 0; j < i; j++ )
                    {
                        if( xVSS_context->pSettings->
                            pTransitionList[j]->VideoTransitionType
                            == M4xVSS_kVideoTransitionType_AlphaMagic )
                        {
                            M4OSA_UInt32 pCmpResult = 0;
                            pCmpResult = strcmp((const char *)xVSS_context->pSettings->
                                pTransitionList[i]->xVSS.
                                transitionSpecific.pAlphaMagicSettings->
                                pAlphaFilePath, (const char *)xVSS_context->pSettings->
                                pTransitionList[j]->xVSS.
                                transitionSpecific.
                                pAlphaMagicSettings->pAlphaFilePath);

                            if( pCmpResult == 0 )
                            {
                                M4xVSS_internal_AlphaMagicSettings
                                    *alphaSettings;

                                alphaSettings =
                                    (M4xVSS_internal_AlphaMagicSettings
                                    *)M4OSA_32bitAlignedMalloc(
                                    sizeof(
                                    M4xVSS_internal_AlphaMagicSettings),
                                    M4VS,
                                    (M4OSA_Char
                                    *)
                                    "Alpha magic settings structure 1");

                                if( alphaSettings == M4OSA_NULL )
                                {
                                    M4OSA_TRACE1_0(
                                        "Allocation error in M4xVSS_SendCommand");
                                    /*FB: to avoid leaks when there is an error in the send
                                     command*/
                                    /* Free Send command */
                                    M4xVSS_freeCommand(xVSS_context);
                                    /**/
                                    return M4ERR_ALLOC;
                                }
                                alphaSettings->pPlane =
                                    ((M4xVSS_internal_AlphaMagicSettings *)(
                                    xVSS_context->pSettings->
                                    pTransitionList[j]->
                                    pExtVideoTransitionFctCtxt))->
                                    pPlane;

                                if( xVSS_context->pSettings->
                                    pTransitionList[i]->xVSS.transitionSpecific.
                                    pAlphaMagicSettings->blendingPercent > 0
                                    && xVSS_context->pSettings->
                                    pTransitionList[i]->xVSS.
                                    transitionSpecific.
                                    pAlphaMagicSettings->blendingPercent
                                    <= 100 )
                                {
                                    alphaSettings->blendingthreshold =
                                        ( xVSS_context->pSettings->
                                        pTransitionList[i]->xVSS.
                                        transitionSpecific.
                                        pAlphaMagicSettings->
                                        blendingPercent) * 255 / 200;
                                }
                                else
                                {
                                    alphaSettings->blendingthreshold = 0;
                                }
                                alphaSettings->isreverse =
                                    xVSS_context->pSettings->
                                    pTransitionList[i]->xVSS.
                                    transitionSpecific.
                                    pAlphaMagicSettings->isreverse;
                                /* It means that the input jpg file for alpha magic has already
                                 been decoded -> no nedd to decode it again */
                                if( alphaSettings->blendingthreshold == 0 )
                                {
                                    xVSS_context->pSettings->
                                        pTransitionList[i]->
                                        ExtVideoTransitionFct =
                                        M4xVSS_AlphaMagic;
                                }
                                else
                                {
                                    xVSS_context->pSettings->
                                        pTransitionList[i]->
                                        ExtVideoTransitionFct =
                                        M4xVSS_AlphaMagicBlending;
                                }
                                xVSS_context->pSettings->pTransitionList[i]->
                                    pExtVideoTransitionFctCtxt = alphaSettings;
                                break;
                            }
                        }
                    }

                    /* If the jpg has not been decoded yet ... */
                    if( xVSS_context->pSettings->
                        pTransitionList[i]->pExtVideoTransitionFctCtxt
                        == M4OSA_NULL )
                    {
                        M4VIFI_ImagePlane *outputPlane;
                        M4xVSS_internal_AlphaMagicSettings *alphaSettings;
                        /*UTF conversion support*/
                        M4OSA_Void *pDecodedPath = M4OSA_NULL;

                        /*To support ARGB8888 : get the width and height */
                        M4OSA_UInt32 width_ARGB888 =
                            xVSS_context->pSettings->pTransitionList[i]->xVSS.
                            transitionSpecific.pAlphaMagicSettings->width;
                        M4OSA_UInt32 height_ARGB888 =
                            xVSS_context->pSettings->pTransitionList[i]->xVSS.
                            transitionSpecific.pAlphaMagicSettings->height;
                        M4OSA_TRACE1_1(
                            " TransitionListM4xVSS_SendCommand width State is %d",
                            width_ARGB888);
                        M4OSA_TRACE1_1(
                            " TransitionListM4xVSS_SendCommand height! State is %d",
                            height_ARGB888);
                        /* Allocate output plane */
                        outputPlane = (M4VIFI_ImagePlane *)M4OSA_32bitAlignedMalloc(3
                            * sizeof(M4VIFI_ImagePlane), M4VS, (M4OSA_Char
                            *)
                            "Output plane for Alpha magic transition");

                        if( outputPlane == M4OSA_NULL )
                        {
                            M4OSA_TRACE1_0(
                                "Allocation error in M4xVSS_SendCommand");
                            /*FB: to avoid leaks when there is an error in the send command*/
                            /* Free Send command */
                            M4xVSS_freeCommand(xVSS_context);
                            /**/
                            return M4ERR_ALLOC;
                        }

                        outputPlane[0].u_width = width;
                        outputPlane[0].u_height = height;
                        outputPlane[0].u_topleft = 0;
                        outputPlane[0].u_stride = width;
                        outputPlane[0].pac_data = (M4VIFI_UInt8
                            *)M4OSA_32bitAlignedMalloc(( width * height * 3)
                            >> 1,
                            M4VS,
                            (M4OSA_Char
                            *)
                            "Alloc for the Alpha magic pac_data output YUV");
                        ;

                        if( outputPlane[0].pac_data == M4OSA_NULL )
                        {
                            free(outputPlane);
                            outputPlane = M4OSA_NULL;
                            M4OSA_TRACE1_0(
                                "Allocation error in M4xVSS_SendCommand");
                            /*FB: to avoid leaks when there is an error in the send command*/
                            /* Free Send command */
                            M4xVSS_freeCommand(xVSS_context);
                            /**/
                            return M4ERR_ALLOC;
                        }
                        outputPlane[1].u_width = width >> 1;
                        outputPlane[1].u_height = height >> 1;
                        outputPlane[1].u_topleft = 0;
                        outputPlane[1].u_stride = width >> 1;
                        outputPlane[1].pac_data = outputPlane[0].pac_data
                            + outputPlane[0].u_width * outputPlane[0].u_height;
                        outputPlane[2].u_width = width >> 1;
                        outputPlane[2].u_height = height >> 1;
                        outputPlane[2].u_topleft = 0;
                        outputPlane[2].u_stride = width >> 1;
                        outputPlane[2].pac_data = outputPlane[1].pac_data
                            + outputPlane[1].u_width * outputPlane[1].u_height;

                        pDecodedPath =
                            xVSS_context->pSettings->pTransitionList[i]->xVSS.
                            transitionSpecific.pAlphaMagicSettings->
                            pAlphaFilePath;
                        /**
                        * UTF conversion: convert into the customer format, before being used*/
                        if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct
                            != M4OSA_NULL && xVSS_context->
                            UTFConversionContext.
                            pTempOutConversionBuffer != M4OSA_NULL )
                        {
                            err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                                (M4OSA_Void *)xVSS_context->pSettings->
                                pTransitionList[i]->xVSS.
                                transitionSpecific.
                                pAlphaMagicSettings->pAlphaFilePath,
                                (M4OSA_Void *)xVSS_context->
                                UTFConversionContext.
                                pTempOutConversionBuffer, &length);

                            if( err != M4NO_ERROR )
                            {
                                M4OSA_TRACE1_1(
                                    "M4xVSS_SendCommand: pConvFromUTF8Fct returns err: 0x%x",
                                    err);
                                /* Free Send command */
                                M4xVSS_freeCommand(xVSS_context);
                                return err;
                            }
                            pDecodedPath =
                                xVSS_context->UTFConversionContext.
                                pTempOutConversionBuffer;
                        }
                        /**
                        End of the conversion, use the decoded path*/
                        /*To support ARGB8888 : convert + resizing from ARGB8888 to yuv420 */

                        err = M4xVSS_internalConvertAndResizeARGB8888toYUV420(
                            pDecodedPath,
                            xVSS_context->pFileReadPtr, outputPlane,
                            width_ARGB888, height_ARGB888);

                        if( err != M4NO_ERROR )
                        {
                            free(outputPlane[0].pac_data);
                            outputPlane[0].pac_data = M4OSA_NULL;
                            free(outputPlane);
                            outputPlane = M4OSA_NULL;
                            M4xVSS_freeCommand(xVSS_context);
                            M4OSA_TRACE1_1(
                                "M4xVSS_SendCommand: error when decoding alpha magic JPEG: 0x%x",
                                err);
                            return err;
                        }

                        /* Allocate alpha settings structure */
                        alphaSettings =
                            (M4xVSS_internal_AlphaMagicSettings *)M4OSA_32bitAlignedMalloc(
                            sizeof(M4xVSS_internal_AlphaMagicSettings),
                            M4VS, (M4OSA_Char
                            *)"Alpha magic settings structure 2");

                        if( alphaSettings == M4OSA_NULL )
                        {
                            M4OSA_TRACE1_0(
                                "Allocation error in M4xVSS_SendCommand");
                            /*FB: to avoid leaks when there is an error in the send command*/
                            /* Free Send command */
                            M4xVSS_freeCommand(xVSS_context);
                            /**/
                            return M4ERR_ALLOC;
                        }
                        alphaSettings->pPlane = outputPlane;

                        if( xVSS_context->pSettings->pTransitionList[i]->xVSS.
                            transitionSpecific.pAlphaMagicSettings->
                            blendingPercent > 0 && xVSS_context->pSettings->
                            pTransitionList[i]->xVSS.
                            transitionSpecific.pAlphaMagicSettings->
                            blendingPercent <= 100 )
                        {
                            alphaSettings->blendingthreshold =
                                ( xVSS_context->pSettings->
                                pTransitionList[i]->xVSS.
                                transitionSpecific.pAlphaMagicSettings->
                                blendingPercent) * 255 / 200;
                        }
                        else
                        {
                            alphaSettings->blendingthreshold = 0;
                        }
                        alphaSettings->isreverse =
                            xVSS_context->pSettings->pTransitionList[i]->xVSS.
                            transitionSpecific.pAlphaMagicSettings->
                            isreverse;

                        if( alphaSettings->blendingthreshold == 0 )
                        {
                            xVSS_context->pSettings->pTransitionList[i]->
                                ExtVideoTransitionFct = M4xVSS_AlphaMagic;
                        }
                        else
                        {
                            xVSS_context->pSettings->pTransitionList[i]->
                                ExtVideoTransitionFct =
                                M4xVSS_AlphaMagicBlending;
                        }
                        xVSS_context->pSettings->pTransitionList[i]->
                            pExtVideoTransitionFctCtxt = alphaSettings;
                    }

                    break;

                case M4xVSS_kVideoTransitionType_SlideTransition:
                    {
                        M4xVSS_internal_SlideTransitionSettings *slideSettings;
                        slideSettings =
                            (M4xVSS_internal_SlideTransitionSettings *)M4OSA_32bitAlignedMalloc(
                            sizeof(M4xVSS_internal_SlideTransitionSettings),
                            M4VS, (M4OSA_Char
                            *)"Internal slide transition settings");

                        if( M4OSA_NULL == slideSettings )
                        {
                            M4OSA_TRACE1_0(
                                "Allocation error in M4xVSS_SendCommand");
                            /*FB: to avoid leaks when there is an error in the send command*/
                            /* Free Send command */
                            M4xVSS_freeCommand(xVSS_context);
                            /**/
                            return M4ERR_ALLOC;
                        }
                        /* Just copy the lone parameter from the input settings to the internal
                         context. */

                        slideSettings->direction =
                            pSettings->pTransitionList[i]->xVSS.transitionSpecific.
                            pSlideTransitionSettings->direction;

                        /* No need to keep our copy of the settings. */
                        xVSS_context->pSettings->pTransitionList[i]->
                            xVSS.transitionSpecific.pSlideTransitionSettings =
                            M4OSA_NULL;
                        xVSS_context->pSettings->pTransitionList[i]->
                            ExtVideoTransitionFct = &M4xVSS_SlideTransition;
                        xVSS_context->pSettings->pTransitionList[i]->
                            pExtVideoTransitionFctCtxt = slideSettings;
                    }
                    break;

                case M4xVSS_kVideoTransitionType_FadeBlack:
                    {
                        xVSS_context->pSettings->pTransitionList[i]->
                            ExtVideoTransitionFct = &M4xVSS_FadeBlackTransition;
                    }
                    break;

                case M4xVSS_kVideoTransitionType_External:
                    {
                        xVSS_context->pSettings->pTransitionList[i]->
                            ExtVideoTransitionFct =
                            pSettings->pTransitionList[i]->ExtVideoTransitionFct;
                        xVSS_context->pSettings->pTransitionList[i]->
                            pExtVideoTransitionFctCtxt =
                            pSettings->pTransitionList[i]->
                            pExtVideoTransitionFctCtxt;
                        xVSS_context->pSettings->pTransitionList[i]->
                            VideoTransitionType =
                            M4VSS3GPP_kVideoTransitionType_External;
                    }
                    break;

                default:
                    break;
                } // switch

            /* Update total_duration with transition duration */
            totalDuration -= xVSS_context->pSettings->
                pTransitionList[i]->uiTransitionDuration;
        }


        if( xVSS_context->pSettings->pClipList[i]->FileType
            == M4VIDEOEDITING_kFileType_ARGB8888 )
        {
            if(M4OSA_TRUE ==
                   xVSS_context->pSettings->pClipList[i]->xVSS.isPanZoom) {
                M4OSA_Char out_img[M4XVSS_MAX_PATH_LEN];
                M4OSA_Char out_img_tmp[M4XVSS_MAX_PATH_LEN];
                M4xVSS_Pto3GPP_params *pParams = M4OSA_NULL;
                M4OSA_Context pARGBFileIn;
                /*UTF conversion support*/
                M4OSA_Void *pDecodedPath = pSettings->pClipList[i]->pFile;

                /* Parse Pto3GPP params chained list to know if input file has already been
                converted */
                if( xVSS_context->pPTo3GPPparamsList != M4OSA_NULL )
                {
                    M4OSA_UInt32 pCmpResult = 0;

                    pParams = xVSS_context->pPTo3GPPparamsList;
                    /* We parse all Pto3gpp Param chained list */
                    while( pParams != M4OSA_NULL )
                    {
                        pCmpResult = strcmp((const char *)pSettings->pClipList[i]->pFile,
                            (const char *)pParams->pFileIn);

                        if( pCmpResult == 0
                            && (pSettings->pClipList[i]->uiEndCutTime
                            == pParams->duration
                            || pSettings->pClipList[i]->xVSS.uiDuration
                            == pParams->duration)
                            && pSettings->pClipList[i]->xVSS.MediaRendering
                            == pParams->MediaRendering )



                        {
                            /* Replace JPG filename with existing 3GP filename */
                            goto replaceARGB_3GP;
                        }
                        /* We need to update this variable, in case some pictures have been
                         added between two */
                        /* calls to M4xVSS_sendCommand */
                        pPto3GPP_last = pParams;
                        pParams = pParams->pNext;
                    }
                }

                /* Construct output temporary 3GP filename */
                err = M4OSA_chrSPrintf(out_img, M4XVSS_MAX_PATH_LEN - 1, (M4OSA_Char *)"%simg%d.3gp",
                    xVSS_context->pTempPath, xVSS_context->tempFileIndex);

                if( err != M4NO_ERROR )
                {
                    M4OSA_TRACE1_1("Error in M4OSA_chrSPrintf: 0x%x", err);
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return err;
                }

    #ifdef M4xVSS_RESERVED_MOOV_DISK_SPACE

                err = M4OSA_chrSPrintf(out_img_tmp, M4XVSS_MAX_PATH_LEN - 1, "%simg%d.tmp",
                    xVSS_context->pTempPath, xVSS_context->tempFileIndex);

                if( err != M4NO_ERROR )
                {
                    M4OSA_TRACE1_1("Error in M4OSA_chrSPrintf: 0x%x", err);
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return err;
                }

    #endif /*M4xVSS_RESERVED_MOOV_DISK_SPACE*/

                xVSS_context->tempFileIndex++;

                /* Allocate last element Pto3GPP params structure */
                pParams = (M4xVSS_Pto3GPP_params
                    *)M4OSA_32bitAlignedMalloc(sizeof(M4xVSS_Pto3GPP_params),
                    M4VS, (M4OSA_Char *)"Element of Pto3GPP Params");

                if( pParams == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0(
                        "M4xVSS_sendCommand: Problem when allocating one element Pto3GPP Params");
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return M4ERR_ALLOC;
                }

                /* Initializes pfilexxx members of pParams to be able to free them correctly */
                pParams->pFileIn = M4OSA_NULL;
                pParams->pFileOut = M4OSA_NULL;
                pParams->pFileTemp = M4OSA_NULL;
                pParams->pNext = M4OSA_NULL;
                pParams->MediaRendering = M4xVSS_kResizing;

                /*To support ARGB8888 :get the width and height */
                pParams->height = pSettings->pClipList[
                    i]->ClipProperties.uiStillPicHeight; //ARGB_Height;
                    pParams->width = pSettings->pClipList[
                        i]->ClipProperties.uiStillPicWidth; //ARGB_Width;
                        M4OSA_TRACE3_1("CLIP M4xVSS_SendCommand ARGB8888 H = %d", pParams->height);
                        M4OSA_TRACE3_1("CLIP M4xVSS_SendCommand ARGB8888 W = %d", pParams->width);

                        if( xVSS_context->pPTo3GPPparamsList
                            == M4OSA_NULL ) /* Means it is the first element of the list */
                        {
                            /* Initialize the xVSS context with the first element of the list */
                            xVSS_context->pPTo3GPPparamsList = pParams;

                            /* Save this element in case of other file to convert */
                            pPto3GPP_last = pParams;
                        }
                        else
                        {
                            /* Update next pointer of the previous last element of the chain */
                            pPto3GPP_last->pNext = pParams;

                            /* Update save of last element of the chain */
                            pPto3GPP_last = pParams;
                        }

                        /* Fill the last M4xVSS_Pto3GPP_params element */
                        pParams->duration =
                            xVSS_context->pSettings->pClipList[i]->uiEndCutTime;
                        /* If duration is filled, let's use it instead of EndCutTime */
                        if( xVSS_context->pSettings->pClipList[i]->xVSS.uiDuration != 0 )
                        {
                            pParams->duration =
                                xVSS_context->pSettings->pClipList[i]->xVSS.uiDuration;
                        }

                        pParams->InputFileType = M4VIDEOEDITING_kFileType_ARGB8888;

                        /**
                        * UTF conversion: convert into the customer format, before being used*/
                        pDecodedPath = xVSS_context->pSettings->pClipList[i]->pFile;
                        length = strlen(pDecodedPath);

                        /**
                        * UTF conversion: convert into the customer format, before being used*/
                        if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct
                            != M4OSA_NULL && xVSS_context->
                            UTFConversionContext.pTempOutConversionBuffer
                            != M4OSA_NULL )
                        {
                            err = M4xVSS_internalConvertFromUTF8(xVSS_context, (M4OSA_Void
                                *)xVSS_context->pSettings->pClipList[i]->pFile,
                                (M4OSA_Void *)xVSS_context->
                                UTFConversionContext.pTempOutConversionBuffer,
                                &length);

                            if( err != M4NO_ERROR )
                            {
                                M4OSA_TRACE1_1(
                                    "M4xVSS_SendCommand: pConvFromUTF8Fct returns err: 0x%x",
                                    err);
                                /* Free Send command */
                                M4xVSS_freeCommand(xVSS_context);
                                return err;
                            }
                            pDecodedPath =
                                xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
                        }

                        /**
                        * End of the UTF conversion, use the converted file path*/
                        pParams->pFileIn = (M4OSA_Void *)M4OSA_32bitAlignedMalloc(length + 1, M4VS,
                            (M4OSA_Char *)"Pto3GPP Params: file in");

                        if( pParams->pFileIn == M4OSA_NULL )
                        {
                            M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                            /*FB: to avoid leaks when there is an error in the send command*/
                            /* Free Send command */
                            M4xVSS_freeCommand(xVSS_context);
                            /**/
                            return M4ERR_ALLOC;
                        }
                        memcpy((void *)pParams->pFileIn, (void *)pDecodedPath,
                            (length + 1)); /* Copy input file path */

                        /* Check that JPG file is present on the FS (P4ME00002974) by just opening
                         and closing it */
                        err =
                            xVSS_context->pFileReadPtr->openRead(&pARGBFileIn, pDecodedPath,
                            M4OSA_kFileRead);

                        if( err != M4NO_ERROR )
                        {
                            M4OSA_TRACE1_2("Can't open input jpg file %s, error: 0x%x\n",
                                pDecodedPath, err);
                            /* Free Send command */
                            M4xVSS_freeCommand(xVSS_context);
                            return err;
                        }
                        err = xVSS_context->pFileReadPtr->closeRead(pARGBFileIn);

                        if( err != M4NO_ERROR )
                        {
                            M4OSA_TRACE1_2("Can't close input jpg file %s, error: 0x%x\n",
                                pDecodedPath, err);
                            /* Free Send command */
                            M4xVSS_freeCommand(xVSS_context);
                            return err;
                        }

                        /**
                        * UTF conversion: convert into the customer format, before being used*/
                        pDecodedPath = out_img;
                        length = strlen(pDecodedPath);

                        if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct
                            != M4OSA_NULL && xVSS_context->
                            UTFConversionContext.pTempOutConversionBuffer
                            != M4OSA_NULL )
                        {
                            err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                                (M4OSA_Void *)out_img, (M4OSA_Void *)xVSS_context->
                                UTFConversionContext.pTempOutConversionBuffer, &length);

                            if( err != M4NO_ERROR )
                            {
                                M4OSA_TRACE1_1(
                                    "M4xVSS_SendCommand: pConvFromUTF8Fct returns err: 0x%x",
                                    err);
                                /* Free Send command */
                                M4xVSS_freeCommand(xVSS_context);
                                return err;
                            }
                            pDecodedPath =
                                xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
                        }

                        /**
                        * End of the UTF conversion, use the converted file path*/
                        pParams->pFileOut = (M4OSA_Void *)M4OSA_32bitAlignedMalloc((length + 1), M4VS,
                            (M4OSA_Char *)"Pto3GPP Params: file out");

                        if( pParams->pFileOut == M4OSA_NULL )
                        {
                            M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                            /*FB: to avoid leaks when there is an error in the send command*/
                            /* Free Send command */
                            M4xVSS_freeCommand(xVSS_context);
                            /**/
                            return M4ERR_ALLOC;
                        }
                        memcpy((void *)pParams->pFileOut, (void *)pDecodedPath,
                            (length + 1)); /* Copy output file path */

    #ifdef M4xVSS_RESERVED_MOOV_DISK_SPACE
                        /**
                        * UTF conversion: convert into the customer format, before being used*/

                        pDecodedPath = out_img_tmp;
                        length = strlen(pDecodedPath);

                        if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct
                            != M4OSA_NULL && xVSS_context->
                            UTFConversionContext.pTempOutConversionBuffer
                            != M4OSA_NULL )
                        {
                            err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                                (M4OSA_Void *)out_img_tmp, (M4OSA_Void *)xVSS_context->
                                UTFConversionContext.pTempOutConversionBuffer, &length);

                            if( err != M4NO_ERROR )
                            {
                                M4OSA_TRACE1_1("M4xVSS_SendCommand: M4xVSS_internalConvertFromUTF8\
                                     returns err: 0x%x",
                                    err);
                                /* Free Send command */
                                M4xVSS_freeCommand(xVSS_context);
                                return err;
                            }
                            pDecodedPath =
                                xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
                        }

                        /**
                        * End of the UTF conversion, use the converted file path*/
                        pParams->pFileTemp = (M4OSA_Void *)M4OSA_32bitAlignedMalloc((length + 1), M4VS,
                            (M4OSA_Char *)"Pto3GPP Params: file temp");

                        if( pParams->pFileTemp == M4OSA_NULL )
                        {
                            M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                            /*FB: to avoid leaks when there is an error in the send command*/
                            /* Free Send command */
                            M4xVSS_freeCommand(xVSS_context);
                            /**/
                            return M4ERR_ALLOC;
                        }
                        memcpy((void *)pParams->pFileTemp, (void *)pDecodedPath,
                            (length + 1)); /* Copy temporary file path */

    #endif                         /*M4xVSS_RESERVED_MOOV_DISK_SPACE*/

                        /* Fill PanAndZoom settings if needed */

                        if( M4OSA_TRUE
                            == xVSS_context->pSettings->pClipList[i]->xVSS.isPanZoom )
                        {
                            pParams->isPanZoom =
                                xVSS_context->pSettings->pClipList[i]->xVSS.isPanZoom;
                            /* Check that Pan & Zoom parameters are corrects */
                            if( xVSS_context->pSettings->pClipList[i]->xVSS.PanZoomXa > 1000
                                || xVSS_context->pSettings->pClipList[i]->xVSS.PanZoomXa
                                <= 0 || xVSS_context->pSettings->pClipList[i]->xVSS.
                                PanZoomTopleftXa > 1000
                                || xVSS_context->pSettings->pClipList[i]->xVSS.
                                PanZoomTopleftYa > 1000
                                || xVSS_context->pSettings->pClipList[i]->xVSS.PanZoomXb
                                > 1000
                                || xVSS_context->pSettings->pClipList[i]->xVSS.PanZoomXb
                                <= 0 || xVSS_context->pSettings->pClipList[i]->xVSS.
                                PanZoomTopleftXb > 1000
                                || xVSS_context->pSettings->pClipList[i]->xVSS.
                                PanZoomTopleftYb > 1000)
                            {
                                M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                                M4xVSS_freeCommand(xVSS_context);
                                return M4ERR_PARAMETER;
                            }

                            pParams->PanZoomXa =
                                xVSS_context->pSettings->pClipList[i]->xVSS.PanZoomXa;
                            pParams->PanZoomTopleftXa =
                                xVSS_context->pSettings->
                                pClipList[i]->xVSS.PanZoomTopleftXa;
                            pParams->PanZoomTopleftYa =
                                xVSS_context->pSettings->
                                pClipList[i]->xVSS.PanZoomTopleftYa;
                            pParams->PanZoomXb =
                                xVSS_context->pSettings->pClipList[i]->xVSS.PanZoomXb;
                            pParams->PanZoomTopleftXb =
                                xVSS_context->pSettings->
                                pClipList[i]->xVSS.PanZoomTopleftXb;
                            pParams->PanZoomTopleftYb =
                                xVSS_context->pSettings->
                                pClipList[i]->xVSS.PanZoomTopleftYb;
                        }
                        else
                        {
                            pParams->isPanZoom = M4OSA_FALSE;
                        }
                        /*+ PR No: blrnxpsw#223*/
                        /*Intializing the Video Frame Rate as it may not be intialized*/
                        /*Other changes made is @ M4xVSS_Internal.c @ line 1518 in
                        M4xVSS_internalStartConvertPictureTo3gp*/
                        switch( xVSS_context->pSettings->videoFrameRate )
                        {
                            case M4VIDEOEDITING_k30_FPS:
                                pParams->framerate = 33;
                                break;

                            case M4VIDEOEDITING_k25_FPS:
                                pParams->framerate = 40;
                                break;

                            case M4VIDEOEDITING_k20_FPS:
                                pParams->framerate = 50;
                                break;

                            case M4VIDEOEDITING_k15_FPS:
                                pParams->framerate = 66;
                                break;

                            case M4VIDEOEDITING_k12_5_FPS:
                                pParams->framerate = 80;
                                break;

                            case M4VIDEOEDITING_k10_FPS:
                                pParams->framerate = 100;
                                break;

                            case M4VIDEOEDITING_k7_5_FPS:
                                pParams->framerate = 133;
                                break;

                            case M4VIDEOEDITING_k5_FPS:
                                pParams->framerate = 200;
                                break;

                            default:
                                /*Making Default Frame Rate @ 15 FPS*/
                                pParams->framerate = 66;
                                break;
                        }
                        /*-PR No: blrnxpsw#223*/
                        if( xVSS_context->pSettings->pClipList[i]->xVSS.MediaRendering
                            == M4xVSS_kCropping
                            || xVSS_context->pSettings->pClipList[i]->xVSS.
                            MediaRendering == M4xVSS_kBlackBorders
                            || xVSS_context->pSettings->pClipList[i]->xVSS.
                            MediaRendering == M4xVSS_kResizing )
                        {
                            pParams->MediaRendering =
                                xVSS_context->pSettings->pClipList[i]->xVSS.MediaRendering;
                        }

                        pParams->pNext = M4OSA_NULL;
                        pParams->isCreated = M4OSA_FALSE;
                        xVSS_context->nbStepTotal++;
                       /* Set bTranscodingRequired to TRUE to indicate the kenburn video has
                        * been generated in analysis phase, and does not need to be tanscoded again
                        * in saving phase */
                        xVSS_context->pSettings->pClipList[i]->bTranscodingRequired =
                           M4OSA_TRUE;

    replaceARGB_3GP:
                        /* Update total duration */
                        totalDuration += pParams->duration;

                        /* Replacing in VSS structure the JPG file by the 3gp file */
                        xVSS_context->pSettings->pClipList[i]->FileType =
                            M4VIDEOEDITING_kFileType_3GPP;

                        if( xVSS_context->pSettings->pClipList[i]->pFile != M4OSA_NULL )
                        {
                            free(xVSS_context->pSettings->pClipList[i]->pFile);
                            xVSS_context->pSettings->pClipList[i]->pFile = M4OSA_NULL;
                        }

                        /**
                        * UTF conversion: convert into UTF8, before being used*/
                        pDecodedPath = pParams->pFileOut;

                        if( xVSS_context->UTFConversionContext.pConvToUTF8Fct != M4OSA_NULL
                            && xVSS_context->UTFConversionContext.pTempOutConversionBuffer
                            != M4OSA_NULL )
                        {
                            err = M4xVSS_internalConvertToUTF8(xVSS_context,
                                (M4OSA_Void *)pParams->pFileOut,
                                (M4OSA_Void *)xVSS_context->
                                UTFConversionContext.pTempOutConversionBuffer,
                                &length);

                            if( err != M4NO_ERROR )
                            {
                                M4OSA_TRACE1_1(
                                    "M4xVSS_SendCommand: M4xVSS_internalConvertToUTF8 returns err: \
                                    0x%x",err);
                                /* Free Send command */
                                M4xVSS_freeCommand(xVSS_context);
                                return err;
                            }
                            pDecodedPath =
                                xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
                        }
                        else
                        {
                            length = strlen(pDecodedPath);
                        }
                        /**
                        * End of the UTF conversion, use the converted file path*/
                        xVSS_context->pSettings->pClipList[i]->pFile = M4OSA_32bitAlignedMalloc((length
                            + 1), M4VS, (M4OSA_Char *)"xVSS file path of ARGB to 3gp");

                        if( xVSS_context->pSettings->pClipList[i]->pFile == M4OSA_NULL )
                        {
                            M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                            /*FB: to avoid leaks when there is an error in the send command*/
                            /* Free Send command */
                            M4xVSS_freeCommand(xVSS_context);
                            /**/
                            return M4ERR_ALLOC;
                        }
                        memcpy((void *)xVSS_context->pSettings->pClipList[i]->pFile,
                            (void *)pDecodedPath, (length + 1));
                        /*FB: add file path size because of UTF16 conversion*/
                        xVSS_context->pSettings->pClipList[i]->filePathSize = length+1;
            }
        }
        /************************
        3GP input file type case
        *************************/
        else if( xVSS_context->pSettings->pClipList[i]->FileType
            == M4VIDEOEDITING_kFileType_3GPP
            || xVSS_context->pSettings->pClipList[i]->FileType
            == M4VIDEOEDITING_kFileType_MP4
            || xVSS_context->pSettings->pClipList[i]->FileType
            == M4VIDEOEDITING_kFileType_M4V )
        {
            /*UTF conversion support*/
            M4OSA_Void *pDecodedPath = M4OSA_NULL;

            /* Need to call MCS in case 3GP video/audio types are not compatible
            (H263/MPEG4 or AMRNB/AAC) */
            /* => Need to fill MCS_Params structure with the right parameters ! */
            /* Need also to parse MCS params struct to check if file has already been transcoded */

            M4VIDEOEDITING_ClipProperties fileProperties;
            M4xVSS_MCS_params *pParams;
            M4OSA_Bool audioIsDifferent = M4OSA_FALSE;
            M4OSA_Bool videoIsDifferent = M4OSA_FALSE;
            M4OSA_Bool bAudioMono;
            /* Initialize file properties structure */

            memset((void *) &fileProperties,0,
                sizeof(M4VIDEOEDITING_ClipProperties));

            //fileProperties.AudioStreamType = M4VIDEOEDITING_kNoneAudio;

            /* Prevent from bad initializing of percentage cut time */
            if( xVSS_context->pSettings->pClipList[i]->xVSS.uiEndCutPercent
                            > 100 || xVSS_context->pSettings->pClipList[i]->xVSS.
                            uiBeginCutPercent > 100 )
            {
                /* These percentage cut time have probably not been initialized */
                /* Let's not use them by setting them to 0 */
                xVSS_context->pSettings->pClipList[i]->xVSS.uiEndCutPercent = 0;
                xVSS_context->pSettings->pClipList[i]->xVSS.uiBeginCutPercent =
                    0;
            }

            /**
            * UTF conversion: convert into the customer format, before being used*/
            pDecodedPath = xVSS_context->pSettings->pClipList[i]->pFile;

            if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct
                != M4OSA_NULL && xVSS_context->
                UTFConversionContext.pTempOutConversionBuffer
                != M4OSA_NULL )
            {
                err = M4xVSS_internalConvertFromUTF8(xVSS_context, (M4OSA_Void
                    *)xVSS_context->pSettings->pClipList[i]->pFile,
                    (M4OSA_Void *)xVSS_context->
                    UTFConversionContext.pTempOutConversionBuffer,
                    &length);

                if( err != M4NO_ERROR )
                {
                    M4OSA_TRACE1_1(
                        "M4xVSS_SendCommand: M4xVSS_internalConvertFromUTF8 returns err: 0x%x",
                        err);
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    return err;
                }
                pDecodedPath =
                    xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
            }
            /**
            * End of the UTF conversion, use the converted file path*/
            err = M4xVSS_internalGetProperties(xVSS_context, pDecodedPath,
                &fileProperties);

            if( err != M4NO_ERROR )
            {
                M4xVSS_freeCommand(xVSS_context);
                M4OSA_TRACE1_1(
                    "M4xVSS_sendCommand: M4xVSS_internalGetProperties returned 0x%x",
                    err);
                /* TODO: Translate error code of MCS to an xVSS error code */
                return err;
            }

            /* Parse MCS params chained list to know if input file has already been converted */
            if( xVSS_context->pMCSparamsList != M4OSA_NULL )
            {
                M4OSA_UInt32 pCmpResult = 0;

                pParams = xVSS_context->pMCSparamsList;
                /* We parse all MCS Param chained list */
                while( pParams != M4OSA_NULL )
                {

                    /**
                    * UTF conversion: convert into UTF8, before being used*/
                    pDecodedPath = pParams->pFileIn;

                    if( xVSS_context->UTFConversionContext.pConvToUTF8Fct
                        != M4OSA_NULL && xVSS_context->
                        UTFConversionContext.pTempOutConversionBuffer
                        != M4OSA_NULL )
                    {
                        err = M4xVSS_internalConvertToUTF8(xVSS_context,
                            (M4OSA_Void *)pParams->pFileIn,
                            (M4OSA_Void *)xVSS_context->
                            UTFConversionContext.
                            pTempOutConversionBuffer, &length);

                        if( err != M4NO_ERROR )
                        {
                            M4OSA_TRACE1_1(
                                "M4xVSS_SendCommand: M4xVSS_internalConvertToUTF8 returns err:\
                                 0x%x", err);
                            /* Free Send command */
                            M4xVSS_freeCommand(xVSS_context);
                            return err;
                        }
                        pDecodedPath = xVSS_context->
                            UTFConversionContext.pTempOutConversionBuffer;
                    }

                    /**
                    * End of the UTF conversion, use the converted file path*/
                    pCmpResult = strcmp((const char *)pSettings->pClipList[i]->pFile,
                        (const char *)pDecodedPath);

                    /* If input filenames are the same, and if this is not a BGM, we can reuse
                    the transcoded file */
                    if( pCmpResult == 0 && pParams->isBGM == M4OSA_FALSE
                        && pParams->BeginCutTime
                        == pSettings->pClipList[i]->uiBeginCutTime
                        && (pParams->EndCutTime
                        == pSettings->pClipList[i]->uiEndCutTime
                        || pParams->EndCutTime
                        == pSettings->pClipList[i]->uiBeginCutTime
                        + pSettings->pClipList[i]->xVSS.uiDuration)
                        && pSettings->pClipList[i]->xVSS.MediaRendering
                        == pParams->MediaRendering )
                    {
                        if( pSettings->xVSS.pBGMtrack != M4OSA_NULL )
                        {
                            if( pSettings->xVSS.pBGMtrack->uiAddVolume == 100
                                || (pParams->OutputAudioFormat
                                == M4VIDEOEDITING_kNullAudio
                                && fileProperties.AudioStreamType
                                == pSettings->xVSS.outputAudioFormat)
                                || pParams->OutputAudioFormat
                                == pSettings->xVSS.outputAudioFormat
                                || fileProperties.AudioStreamType
                                == M4VIDEOEDITING_kNoneAudio )
                            {
                                /* Replace 3GP filename with transcoded 3GP filename */
                                goto replace3GP_3GP;
                            }
                        }
                        else if( ( pParams->OutputAudioFormat
                            == M4VIDEOEDITING_kNullAudio
                            && fileProperties.AudioStreamType
                            == pSettings->xVSS.outputAudioFormat)
                            || pParams->OutputAudioFormat
                            == pSettings->xVSS.outputAudioFormat
                            || fileProperties.AudioStreamType
                            == M4VIDEOEDITING_kNoneAudio )
                        {
                            /* Replace 3GP filename with transcoded 3GP filename */
                            goto replace3GP_3GP;
                        }
                    }

                    /* We need to update this variable, in case some 3GP files have been added
                    between two */
                    /* calls to M4xVSS_sendCommand */
                    pMCS_last = pParams;
                    pParams = pParams->pNext;
                }
            }

            /* If we have percentage information let's use it... */
            if( xVSS_context->pSettings->pClipList[i]->xVSS.uiEndCutPercent != 0
                || xVSS_context->pSettings->pClipList[i]->xVSS.uiBeginCutPercent
                != 0 )
            {
                /* If percentage information are not correct and if duration field is not filled */
                if( ( xVSS_context->pSettings->pClipList[i]->xVSS.
                    uiEndCutPercent
                    <= xVSS_context->pSettings->pClipList[i]->xVSS.
                    uiBeginCutPercent)
                    && xVSS_context->pSettings->pClipList[i]->xVSS.uiDuration
                    == 0 )
                {
                    M4OSA_TRACE1_0(
                        "M4xVSS_sendCommand: Bad percentage for begin and end cut time !");
                    M4xVSS_freeCommand(xVSS_context);
                    return M4ERR_PARAMETER;
                }

                /* We transform the percentage into absolute time */
                xVSS_context->pSettings->pClipList[i]->uiBeginCutTime
                    = (M4OSA_UInt32)(
                    xVSS_context->pSettings->pClipList[i]->xVSS.
                    uiBeginCutPercent
                    * fileProperties.uiClipDuration / 100);
                xVSS_context->pSettings->pClipList[i]->uiEndCutTime
                    = (M4OSA_UInt32)(
                    xVSS_context->pSettings->pClipList[i]->xVSS.
                    uiEndCutPercent
                    * fileProperties.uiClipDuration / 100);
            }
            /* ...Otherwise, we use absolute time. */
            else
            {
                /* If endCutTime == 0, it means all the file is taken. Let's change to the file
                duration, to accurate preview. */
                if( xVSS_context->pSettings->pClipList[i]->uiEndCutTime == 0
                    || xVSS_context->pSettings->pClipList[i]->uiEndCutTime
                    > fileProperties.uiClipDuration )
                {
                    xVSS_context->pSettings->pClipList[i]->uiEndCutTime =
                        fileProperties.uiClipDuration;
                }
            }

            /* If duration field is filled, it has priority on other fields on EndCutTime,
             so let's use it */
            if( xVSS_context->pSettings->pClipList[i]->xVSS.uiDuration != 0 )
            {
                xVSS_context->pSettings->pClipList[i]->uiEndCutTime =
                    xVSS_context->pSettings->pClipList[i]->uiBeginCutTime
                    +xVSS_context->pSettings->pClipList[i]->xVSS.uiDuration;

                if( xVSS_context->pSettings->pClipList[i]->uiEndCutTime
                    > fileProperties.uiClipDuration )
                {
                    xVSS_context->pSettings->pClipList[i]->uiEndCutTime =
                        fileProperties.uiClipDuration;
                }
            }

            /* If output video format is not set, we take video format of the first 3GP video */
            if( xVSS_context->pSettings->xVSS.outputVideoFormat
                == M4VIDEOEDITING_kNoneVideo )
            {
                //xVSS_context->pSettings->xVSS.outputVideoFormat = fileProperties.VideoStreamType;
                //M4OSA_TRACE2_1("Output video format is not set, set it to current clip: %d",
                // xVSS_context->pSettings->xVSS.outputVideoFormat);
                M4OSA_TRACE1_0(
                    "Output video format is not set, an error parameter is returned.");
                M4xVSS_freeCommand(xVSS_context);
                return M4ERR_PARAMETER;
            }

            if( xVSS_context->pSettings->xVSS.outputAudioFormat
                == M4VIDEOEDITING_kNoneAudio )
            {
                //xVSS_context->pSettings->xVSS.outputAudioFormat = fileProperties.AudioStreamType;
                M4OSA_TRACE2_1(
                    "Output audio format is not set -> remove audio track of clip: %d",
                    i);
            }

            if( fileProperties.uiNbChannels == 1 )
            {
                bAudioMono = M4OSA_TRUE;
            }
            else
            {
                bAudioMono = M4OSA_FALSE;
            }

            if( fileProperties.AudioStreamType
                != xVSS_context->pSettings->xVSS.outputAudioFormat
                || (fileProperties.AudioStreamType == M4VIDEOEDITING_kAAC
                && (fileProperties.uiSamplingFrequency != samplingFreq
                || bAudioMono
                != xVSS_context->pSettings->xVSS.bAudioMono)) )
            {
                audioIsDifferent = M4OSA_TRUE;
                /* If we want to replace audio, there is no need to transcode audio */
                if( pSettings->xVSS.pBGMtrack != M4OSA_NULL )
                {
                    /* temp fix :PT volume not herad in the second clip */
                    if( /*(pSettings->xVSS.pBGMtrack->uiAddVolume == 100
                        && xVSS_context->pSettings->xVSS.outputFileSize == 0)
                        ||*/
                        fileProperties.AudioStreamType
                        == M4VIDEOEDITING_kNoneAudio ) /*11/12/2008 CR 3283 VAL for the MMS
                        use case, we need to transcode except the media without audio*/
                    {
                        audioIsDifferent = M4OSA_FALSE;
                    }
                }
                else if( fileProperties.AudioStreamType
                    == M4VIDEOEDITING_kNoneAudio )
                {
                    audioIsDifferent = M4OSA_FALSE;
                }
            }
            /* Here check the clip video profile and level, if it exceeds
             * the profile and level of export file, then the file needs
             * to be transcoded(do not do compress domain trim).
             * Also for MPEG4 fomart, always do transcoding since HW encoder
             * may use different time scale value than the input clip*/
           if ((fileProperties.uiVideoProfile >
                     xVSS_context->pSettings->xVSS.outputVideoProfile) ||
                (fileProperties.uiVideoLevel >
                     xVSS_context->pSettings->xVSS.outputVideoLevel) ||
                (fileProperties.VideoStreamType == M4VIDEOEDITING_kMPEG4)) {
               /* Set bTranscodingRequired to TRUE to indicate the video will be
                * transcoded in MCS. */
               xVSS_context->pSettings->pClipList[i]->bTranscodingRequired =
                   M4OSA_TRUE;
               videoIsDifferent = M4OSA_TRUE;
           }

            if( videoIsDifferent == M4OSA_TRUE || audioIsDifferent == M4OSA_TRUE)
            {
                M4OSA_Char out_3gp[M4XVSS_MAX_PATH_LEN];
                M4OSA_Char out_3gp_tmp[M4XVSS_MAX_PATH_LEN];

                /* Construct output temporary 3GP filename */
                err = M4OSA_chrSPrintf(out_3gp, M4XVSS_MAX_PATH_LEN - 1, (M4OSA_Char *)"%svid%d.3gp",
                    xVSS_context->pTempPath, xVSS_context->tempFileIndex);

                if( err != M4NO_ERROR )
                {
                    M4OSA_TRACE1_1("Error in M4OSA_chrSPrintf: 0x%x", err);
                    return err;
                }

#ifdef M4xVSS_RESERVED_MOOV_DISK_SPACE

                err = M4OSA_chrSPrintf(out_3gp_tmp, M4XVSS_MAX_PATH_LEN - 1, "%svid%d.tmp",
                    xVSS_context->pTempPath, xVSS_context->tempFileIndex);

                if( err != M4NO_ERROR )
                {
                    M4OSA_TRACE1_1("Error in M4OSA_chrSPrintf: 0x%x", err);
                    return err;
                }

#endif /*M4xVSS_RESERVED_MOOV_DISK_SPACE*/

                xVSS_context->tempFileIndex++;

                pParams =
                    (M4xVSS_MCS_params *)M4OSA_32bitAlignedMalloc(sizeof(M4xVSS_MCS_params),
                    M4VS, (M4OSA_Char *)"Element of MCS Params (for 3GP)");

                if( pParams == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0(
                        "M4xVSS_sendCommand: Problem when allocating one element MCS Params");
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return M4ERR_ALLOC;
                }
                pParams->MediaRendering = M4xVSS_kResizing;
                pParams->videoclipnumber = i; // Indicates video clip index

                if( xVSS_context->pMCSparamsList
                    == M4OSA_NULL ) /* Means it is the first element of the list */
                {
                    /* Initialize the xVSS context with the first element of the list */
                    xVSS_context->pMCSparamsList = pParams;
                }
                else
                {
                    /* Update next pointer of the previous last element of the chain */
                    pMCS_last->pNext = pParams;
                }

                /* Save this element in case of other file to convert */
                pMCS_last = pParams;

                /* Fill the last M4xVSS_MCS_params element */
                pParams->InputFileType = M4VIDEOEDITING_kFileType_3GPP;
                pParams->OutputFileType = M4VIDEOEDITING_kFileType_3GPP;

                pParams->OutputVideoTimescale = xVSS_context->targetedTimescale;

                /* We do not need to reencode video if its parameters do not differ */
                /* from output settings parameters */
                if( videoIsDifferent == M4OSA_TRUE )
                {
                    pParams->OutputVideoFormat =
                        xVSS_context->pSettings->xVSS.outputVideoFormat;
                    pParams->outputVideoProfile =
                        xVSS_context->pSettings->xVSS.outputVideoProfile;
                    pParams->outputVideoLevel =
                        xVSS_context->pSettings->xVSS.outputVideoLevel;
                    pParams->OutputVideoFrameRate =
                        xVSS_context->pSettings->videoFrameRate;
                    pParams->OutputVideoFrameSize =
                        xVSS_context->pSettings->xVSS.outputVideoSize;

                    /*FB: VAL CR P4ME00003076
                    The output video bitrate is now directly given by the user in the edition
                    settings structure If the bitrate given by the user is irrelevant
                    (the MCS minimum and maximum video bitrate are used),
                    the output video bitrate is hardcoded according to the output video size*/
                    if( xVSS_context->pSettings->xVSS.outputVideoBitrate
                        >= M4VIDEOEDITING_k16_KBPS
                        && xVSS_context->pSettings->xVSS.outputVideoBitrate
                        <= M4VIDEOEDITING_k8_MBPS ) /*+ New Encoder bitrates */
                    {
                        pParams->OutputVideoBitrate =
                            xVSS_context->pSettings->xVSS.outputVideoBitrate;
                    }
                    else
                    {
                        switch( xVSS_context->pSettings->xVSS.outputVideoSize )
                        {
                            case M4VIDEOEDITING_kSQCIF:
                                pParams->OutputVideoBitrate =
                                    M4VIDEOEDITING_k48_KBPS;
                                break;

                            case M4VIDEOEDITING_kQQVGA:
                                pParams->OutputVideoBitrate =
                                    M4VIDEOEDITING_k64_KBPS;
                                break;

                            case M4VIDEOEDITING_kQCIF:
                                pParams->OutputVideoBitrate =
                                    M4VIDEOEDITING_k128_KBPS;
                                break;

                            case M4VIDEOEDITING_kQVGA:
                                pParams->OutputVideoBitrate =
                                    M4VIDEOEDITING_k384_KBPS;
                                break;

                            case M4VIDEOEDITING_kCIF:
                                pParams->OutputVideoBitrate =
                                    M4VIDEOEDITING_k384_KBPS;
                                break;

                            case M4VIDEOEDITING_kVGA:
                                pParams->OutputVideoBitrate =
                                    M4VIDEOEDITING_k512_KBPS;
                                break;

                            default: /* Should not happen !! */
                                pParams->OutputVideoBitrate =
                                    M4VIDEOEDITING_k64_KBPS;
                                break;
                        }
                    }
                }
                else
                {
                    pParams->outputVideoProfile =
                        xVSS_context->pSettings->xVSS.outputVideoProfile;
                    pParams->outputVideoLevel =
                        xVSS_context->pSettings->xVSS.outputVideoLevel;
                    pParams->OutputVideoFormat = M4VIDEOEDITING_kNullVideo;
                    pParams->OutputVideoFrameRate =
                        M4VIDEOEDITING_k15_FPS; /* Must be set, otherwise, MCS returns an error */
                }

                if( audioIsDifferent == M4OSA_TRUE )
                {
                    pParams->OutputAudioFormat =
                        xVSS_context->pSettings->xVSS.outputAudioFormat;

                    switch( xVSS_context->pSettings->xVSS.outputAudioFormat )
                    {
                        case M4VIDEOEDITING_kNoneAudio:
                            break;

                        case M4VIDEOEDITING_kAMR_NB:
                            pParams->OutputAudioBitrate =
                                M4VIDEOEDITING_k12_2_KBPS;
                            pParams->bAudioMono = M4OSA_TRUE;
                            pParams->OutputAudioSamplingFrequency =
                                M4VIDEOEDITING_kDefault_ASF;
                            break;

                        case M4VIDEOEDITING_kAAC:
                            {
                                /*FB: VAL CR P4ME00003076
                                The output audio bitrate in the AAC case is now directly given by
                                the user in the edition settings structure
                                If the bitrate given by the user is irrelevant or undefined
                                (the MCS minimum and maximum audio bitrate are used),
                                the output audio bitrate is hard coded according to the output
                                audio sampling frequency*/

                                /*Check if the audio bitrate is correctly defined*/

                                /*Mono
                                MCS values for AAC Mono are min: 16kbps and max: 192 kbps*/
                                if( xVSS_context->pSettings->xVSS.outputAudioBitrate
                                    >= M4VIDEOEDITING_k16_KBPS
                                    && xVSS_context->pSettings->
                                    xVSS.outputAudioBitrate
                                    <= M4VIDEOEDITING_k192_KBPS
                                    && xVSS_context->pSettings->xVSS.bAudioMono
                                    == M4OSA_TRUE )
                                {
                                    pParams->OutputAudioBitrate =
                                        xVSS_context->pSettings->
                                        xVSS.outputAudioBitrate;
                                }
                                /*Stereo
                                MCS values for AAC Mono are min: 32kbps and max: 192 kbps*/
                                else if( xVSS_context->pSettings->
                                    xVSS.outputAudioBitrate
                                    >= M4VIDEOEDITING_k32_KBPS
                                    && xVSS_context->pSettings->
                                    xVSS.outputAudioBitrate
                                    <= M4VIDEOEDITING_k192_KBPS
                                    && xVSS_context->pSettings->xVSS.bAudioMono
                                    == M4OSA_FALSE )
                                {
                                    pParams->OutputAudioBitrate =
                                        xVSS_context->pSettings->
                                        xVSS.outputAudioBitrate;
                                }

                                /*The audio bitrate is hard coded according to the output audio
                                 sampling frequency*/
                                else
                                {
                                    switch( xVSS_context->pSettings->
                                        xVSS.outputAudioSamplFreq )
                                    {
                                        case M4VIDEOEDITING_k16000_ASF:
                                            pParams->OutputAudioBitrate =
                                                M4VIDEOEDITING_k24_KBPS;
                                            break;

                                        case M4VIDEOEDITING_k22050_ASF:
                                        case M4VIDEOEDITING_k24000_ASF:
                                            pParams->OutputAudioBitrate =
                                                M4VIDEOEDITING_k32_KBPS;
                                            break;

                                        case M4VIDEOEDITING_k32000_ASF:
                                            pParams->OutputAudioBitrate =
                                                M4VIDEOEDITING_k48_KBPS;
                                            break;

                                        case M4VIDEOEDITING_k44100_ASF:
                                        case M4VIDEOEDITING_k48000_ASF:
                                            pParams->OutputAudioBitrate =
                                                M4VIDEOEDITING_k64_KBPS;
                                            break;

                                        default:
                                            pParams->OutputAudioBitrate =
                                                M4VIDEOEDITING_k64_KBPS;
                                            break;
                                    }

                                    if( xVSS_context->pSettings->xVSS.bAudioMono
                                        == M4OSA_FALSE )
                                    {
                                        /* Output bitrate have to be doubled */
                                        pParams->OutputAudioBitrate +=
                                            pParams->OutputAudioBitrate;
                                    }
                                }

                                pParams->bAudioMono =
                                    xVSS_context->pSettings->xVSS.bAudioMono;

                                if( xVSS_context->pSettings->
                                    xVSS.outputAudioSamplFreq
                                    == M4VIDEOEDITING_k8000_ASF )
                                {
                                    /* Prevent from unallowed sampling frequencies */
                                    pParams->OutputAudioSamplingFrequency =
                                        M4VIDEOEDITING_kDefault_ASF;
                                }
                                else
                                {
                                    pParams->OutputAudioSamplingFrequency =
                                        xVSS_context->pSettings->
                                        xVSS.outputAudioSamplFreq;
                                }
                                break;
                            }

                        default: /* Should not happen !! */
                            pParams->OutputAudioFormat = M4VIDEOEDITING_kAMR_NB;
                            pParams->OutputAudioBitrate =
                                M4VIDEOEDITING_k12_2_KBPS;
                            pParams->bAudioMono = M4OSA_TRUE;
                            pParams->OutputAudioSamplingFrequency =
                                M4VIDEOEDITING_kDefault_ASF;
                            break;
                        }
                }
                else
                {
                    pParams->OutputAudioFormat = M4VIDEOEDITING_kNullAudio;
                }

                /**
                * UTF conversion: convert into the customer format, before being used*/
                pDecodedPath = xVSS_context->pSettings->pClipList[i]->pFile;
                length = strlen(pDecodedPath);

                if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct
                    != M4OSA_NULL && xVSS_context->
                    UTFConversionContext.pTempOutConversionBuffer
                    != M4OSA_NULL )
                {
                    err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                        (M4OSA_Void *)xVSS_context->pSettings->
                        pClipList[i]->pFile,
                        (M4OSA_Void *)xVSS_context->
                        UTFConversionContext.pTempOutConversionBuffer,
                        &length);

                    if( err != M4NO_ERROR )
                    {
                        M4OSA_TRACE1_1(
                            "M4xVSS_SendCommand: M4xVSS_internalConvertFromUTF8 returns err: 0x%x",
                            err);
                        /* Free Send command */
                        M4xVSS_freeCommand(xVSS_context);
                        return err;
                    }
                    pDecodedPath = xVSS_context->
                        UTFConversionContext.pTempOutConversionBuffer;
                }

                /**
                * End of the UTF conversion, use the converted file path*/
                pParams->pFileIn =
                    (M4OSA_Void *)M4OSA_32bitAlignedMalloc((length + 1), M4VS,
                    (M4OSA_Char *)"MCS 3GP Params: file in");

                if( pParams->pFileIn == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return M4ERR_ALLOC;
                }
                memcpy((void *)pParams->pFileIn, (void *)pDecodedPath,
                    (length + 1)); /* Copy input file path */

                /**
                * UTF conversion: convert into the customer format, before being used*/
                pDecodedPath = out_3gp;
                length = strlen(pDecodedPath);

                if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct
                    != M4OSA_NULL && xVSS_context->
                    UTFConversionContext.pTempOutConversionBuffer
                    != M4OSA_NULL )
                {
                    err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                        (M4OSA_Void *)out_3gp, (M4OSA_Void *)xVSS_context->
                        UTFConversionContext.pTempOutConversionBuffer,
                        &length);

                    if( err != M4NO_ERROR )
                    {
                        M4OSA_TRACE1_1(
                            "M4xVSS_SendCommand: M4xVSS_internalConvertFromUTF8 returns err: 0x%x",
                            err);
                        /* Free Send command */
                        M4xVSS_freeCommand(xVSS_context);
                        return err;
                    }
                    pDecodedPath = xVSS_context->
                        UTFConversionContext.pTempOutConversionBuffer;
                }

                /**
                * End of the UTF conversion, use the converted file path*/
                pParams->pFileOut =
                    (M4OSA_Void *)M4OSA_32bitAlignedMalloc((length + 1), M4VS,
                    (M4OSA_Char *)"MCS 3GP Params: file out");

                if( pParams->pFileOut == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return M4ERR_ALLOC;
                }
                memcpy((void *)pParams->pFileOut, (void *)pDecodedPath,
                    (length + 1)); /* Copy output file path */

#ifdef M4xVSS_RESERVED_MOOV_DISK_SPACE
                /**
                * UTF conversion: convert into the customer format, before being used*/

                pDecodedPath = out_3gp_tmp;
                length = strlen(pDecodedPath);

                if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct
                    != M4OSA_NULL && xVSS_context->
                    UTFConversionContext.pTempOutConversionBuffer
                    != M4OSA_NULL )
                {
                    err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                        (M4OSA_Void *)out_3gp_tmp,
                        (M4OSA_Void *)xVSS_context->
                        UTFConversionContext.pTempOutConversionBuffer,
                        &length);

                    if( err != M4NO_ERROR )
                    {
                        M4OSA_TRACE1_1(
                            "M4xVSS_SendCommand: M4xVSS_internalConvertFromUTF8 returns err: 0x%x",
                            err);
                        /* Free Send command */
                        M4xVSS_freeCommand(xVSS_context);
                        return err;
                    }
                    pDecodedPath = xVSS_context->
                        UTFConversionContext.pTempOutConversionBuffer;
                }

                /**
                * End of the UTF conversion, use the converted file path*/
                pParams->pFileTemp =
                    (M4OSA_Void *)M4OSA_32bitAlignedMalloc((length + 1), M4VS,
                    (M4OSA_Char *)"MCS 3GP Params: file temp");

                if( pParams->pFileTemp == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return M4ERR_ALLOC;
                }
                memcpy((void *)pParams->pFileTemp, (void *)pDecodedPath,
                    (length + 1)); /* Copy temporary file path */

#else

                pParams->pFileTemp = M4OSA_NULL;

#endif /*M4xVSS_RESERVED_MOOV_DISK_SPACE*/

                /*FB 2008/10/20 keep media aspect ratio, add media rendering parameter*/

                if( xVSS_context->pSettings->pClipList[i]->xVSS.MediaRendering
                    == M4xVSS_kCropping
                    || xVSS_context->pSettings->pClipList[i]->xVSS.
                    MediaRendering == M4xVSS_kBlackBorders
                    || xVSS_context->pSettings->pClipList[i]->xVSS.
                    MediaRendering == M4xVSS_kResizing )
                {
                    pParams->MediaRendering =
                        xVSS_context->pSettings->pClipList[i]->xVSS.
                        MediaRendering;
                }

                /*FB: transcoding per parts*/
                pParams->BeginCutTime =
                    xVSS_context->pSettings->pClipList[i]->uiBeginCutTime;
                pParams->EndCutTime =
                    xVSS_context->pSettings->pClipList[i]->uiEndCutTime;

                pParams->pNext = M4OSA_NULL;
                pParams->isBGM = M4OSA_FALSE;
                pParams->isCreated = M4OSA_FALSE;
                xVSS_context->nbStepTotal++;
                bIsTranscoding = M4OSA_TRUE;

replace3GP_3GP:
                /* Update total duration */
                totalDuration +=
                    xVSS_context->pSettings->pClipList[i]->uiEndCutTime
                    - xVSS_context->pSettings->pClipList[i]->uiBeginCutTime;

                /* Replacing in VSS structure the original 3GP file by the transcoded 3GP file */
                xVSS_context->pSettings->pClipList[i]->FileType =
                    M4VIDEOEDITING_kFileType_3GPP;

                if( xVSS_context->pSettings->pClipList[i]->pFile != M4OSA_NULL )
                {
                    free(xVSS_context->pSettings->pClipList[i]->pFile);
                    xVSS_context->pSettings->pClipList[i]->pFile = M4OSA_NULL;
                }

                /**
                * UTF conversion: convert into the customer format, before being used*/
                pDecodedPath = pParams->pFileOut;

                if( xVSS_context->UTFConversionContext.pConvToUTF8Fct
                    != M4OSA_NULL && xVSS_context->
                    UTFConversionContext.pTempOutConversionBuffer
                    != M4OSA_NULL )
                {
                    err = M4xVSS_internalConvertToUTF8(xVSS_context,
                        (M4OSA_Void *)pParams->pFileOut,
                        (M4OSA_Void *)xVSS_context->
                        UTFConversionContext.pTempOutConversionBuffer,
                        &length);

                    if( err != M4NO_ERROR )
                    {
                        M4OSA_TRACE1_1(
                            "M4xVSS_SendCommand: M4xVSS_internalConvertToUTF8 returns err: 0x%x",
                            err);
                        /* Free Send command */
                        M4xVSS_freeCommand(xVSS_context);
                        return err;
                    }
                    pDecodedPath = xVSS_context->
                        UTFConversionContext.pTempOutConversionBuffer;
                }
                else
                {
                    length = strlen(pDecodedPath);
                }
                /**
                * End of the UTF conversion, use the converted file path*/
                xVSS_context->pSettings->pClipList[i]->pFile = M4OSA_32bitAlignedMalloc(
                    (length + 1),
                    M4VS, (M4OSA_Char *)"xVSS file path of 3gp to 3gp");

                if( xVSS_context->pSettings->pClipList[i]->pFile == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return M4ERR_ALLOC;
                }
                memcpy((void *)xVSS_context->pSettings->pClipList[i]->pFile,
                    (void *)pDecodedPath, (length + 1));
                /*FB: add file path size because of UTF 16 conversion*/
                xVSS_context->pSettings->pClipList[i]->filePathSize = length+1;

                /* We define master clip as first 3GP input clip */
                /*if(xVSS_context->pSettings->uiMasterClip == 0 && fileProperties.
                AudioStreamType != M4VIDEOEDITING_kNoneAudio)
                {
                xVSS_context->pSettings->uiMasterClip = i;
                }*/
            }
            else
            {
                /* Update total duration */
                totalDuration +=
                    xVSS_context->pSettings->pClipList[i]->uiEndCutTime
                    - xVSS_context->pSettings->pClipList[i]->uiBeginCutTime;
            }
            /* We define master clip as first 3GP input clip */
            if( masterClip == -1
                && fileProperties.AudioStreamType != M4VIDEOEDITING_kNoneAudio )
            {
                masterClip = i;
                xVSS_context->pSettings->uiMasterClip = i;
            }

        }
        /**************************
        Other input file type case
        ***************************/
        else
        {
            M4OSA_TRACE1_0("Bad file type as input clip");
            /*FB: to avoid leaks when there is an error in the send command*/
            /* Free Send command */
            M4xVSS_freeCommand(xVSS_context);
            /**/
            return M4ERR_PARAMETER;
        }
    }

    /*********************************************************
    * Parse all effects to make some adjustment for framing, *
    * text and to transform relative time into absolute time *
    **********************************************************/
    for ( j = 0; j < xVSS_context->pSettings->nbEffects; j++ )
    {
        /* Copy effect to "local" structure */
        memcpy((void *) &(xVSS_context->pSettings->Effects[j]),
            (void *) &(pSettings->Effects[j]),
            sizeof(M4VSS3GPP_EffectSettings));

        /* Prevent from bad initializing of effect percentage time */
        if( xVSS_context->pSettings->Effects[j].xVSS.uiDurationPercent > 100
            || xVSS_context->pSettings->Effects[j].xVSS.uiStartPercent > 100 )
        {
            /* These percentage time have probably not been initialized */
            /* Let's not use them by setting them to 0 */
            xVSS_context->pSettings->Effects[j].xVSS.uiDurationPercent = 0;
            xVSS_context->pSettings->Effects[j].xVSS.uiStartPercent = 0;
        }

        /* If we have percentage information let's use it... Otherwise, we use absolute time. */
        if( xVSS_context->pSettings->Effects[j].xVSS.uiDurationPercent != 0 )
        {
            xVSS_context->pSettings->
                Effects[j].uiStartTime = (M4OSA_UInt32)(totalDuration
                * xVSS_context->pSettings->Effects[j].xVSS.uiStartPercent
                / 100);
            /* The percentage of effect duration is based on the duration of the clip -
            start time */
            xVSS_context->pSettings->
                Effects[j].uiDuration = (M4OSA_UInt32)(totalDuration
                * xVSS_context->pSettings->Effects[j].xVSS.uiDurationPercent
                / 100);
        }

        /* If there is a framing effect, we need to allocate framing effect structure */
        if( xVSS_context->pSettings->Effects[j].VideoEffectType
            == M4xVSS_kVideoEffectType_Framing )
        {
#ifdef DECODE_GIF_ON_SAVING

            M4xVSS_FramingContext *framingCtx;
            /*UTF conversion support*/
            M4OSA_Void *pDecodedPath = M4OSA_NULL;

#else

            M4xVSS_FramingStruct *framingCtx;

#endif /*DECODE_GIF_ON_SAVING*/

            M4OSA_Char *pExt2 = M4OSA_NULL;
            M4VIFI_ImagePlane *pPlane =
                xVSS_context->pSettings->Effects[j].xVSS.pFramingBuffer;
            M4OSA_Int32 result1, result2;

            /* Copy framing file path */
            if( pSettings->Effects[j].xVSS.pFramingFilePath != M4OSA_NULL )
            {
                xVSS_context->pSettings->
                    Effects[j].xVSS.pFramingFilePath = M4OSA_32bitAlignedMalloc(
                    strlen(pSettings->Effects[j].xVSS.pFramingFilePath)
                    + 1, M4VS, (M4OSA_Char *)"Local Framing file path");

                if( xVSS_context->pSettings->Effects[j].xVSS.pFramingFilePath
                    == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return M4ERR_ALLOC;
                }
                memcpy((void *)xVSS_context->pSettings->
                    Effects[j].xVSS.pFramingFilePath,
                    (void *)pSettings->
                    Effects[j].xVSS.pFramingFilePath, strlen(
                    pSettings->Effects[j].xVSS.pFramingFilePath) + 1);

                pExt2 =
                    xVSS_context->pSettings->Effects[j].xVSS.pFramingFilePath;
            }

#ifdef DECODE_GIF_ON_SAVING

            framingCtx = (M4xVSS_FramingContext
                *)M4OSA_32bitAlignedMalloc(sizeof(M4xVSS_FramingContext),
                M4VS, (M4OSA_Char *)"Context of the framing effect");

            if( framingCtx == M4OSA_NULL )
            {
                M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                /*FB: to avoid leaks when there is an error in the send command*/
                /* Free Send command */
                M4xVSS_freeCommand(xVSS_context);
                /**/
                return M4ERR_ALLOC;
            }
            framingCtx->aFramingCtx = M4OSA_NULL;
            framingCtx->aFramingCtx_last = M4OSA_NULL;
            framingCtx->pSPSContext = M4OSA_NULL;
            framingCtx->outputVideoSize =
                xVSS_context->pSettings->xVSS.outputVideoSize;
            framingCtx->topleft_x =
                xVSS_context->pSettings->Effects[j].xVSS.topleft_x;
            framingCtx->topleft_y =
                xVSS_context->pSettings->Effects[j].xVSS.topleft_y;
            framingCtx->bEffectResize =
                xVSS_context->pSettings->Effects[j].xVSS.bResize;
            framingCtx->pEffectFilePath =
                xVSS_context->pSettings->Effects[j].xVSS.pFramingFilePath;
            framingCtx->pFileReadPtr = xVSS_context->pFileReadPtr;
            framingCtx->pFileWritePtr = xVSS_context->pFileWritePtr;
            framingCtx->effectDuration =
                xVSS_context->pSettings->Effects[j].uiDuration;
            framingCtx->b_IsFileGif = M4OSA_FALSE;
            framingCtx->alphaBlendingStruct = M4OSA_NULL;
            framingCtx->b_animated = M4OSA_FALSE;

            /* Output ratio for the effect is stored in uiFiftiesOutFrameRate parameters of the
            extended xVSS effects structure */
            if( xVSS_context->pSettings->Effects[j].xVSS.uiFiftiesOutFrameRate
                != 0 )
            {
                framingCtx->frameDurationRatio =
                    (M4OSA_Float)(( xVSS_context->pSettings->
                    Effects[j].xVSS.uiFiftiesOutFrameRate) / 1000.0);
            }
            else
            {
                framingCtx->frameDurationRatio = 1.0;
            }

            /*Alpha blending*/
            /*Check if the alpha blending parameters are corrects*/
            if( pSettings->Effects[j].xVSS.uialphaBlendingFadeInTime > 100 )
            {
                pSettings->Effects[j].xVSS.uialphaBlendingFadeInTime = 0;
            }

            if( pSettings->Effects[j].xVSS.uialphaBlendingFadeOutTime > 100 )
            {
                pSettings->Effects[j].xVSS.uialphaBlendingFadeOutTime = 0;
            }

            if( pSettings->Effects[j].xVSS.uialphaBlendingEnd > 100 )
            {
                pSettings->Effects[j].xVSS.uialphaBlendingEnd = 100;
            }

            if( pSettings->Effects[j].xVSS.uialphaBlendingMiddle > 100 )
            {
                pSettings->Effects[j].xVSS.uialphaBlendingMiddle = 100;
            }

            if( pSettings->Effects[j].xVSS.uialphaBlendingStart > 100 )
            {
                pSettings->Effects[j].xVSS.uialphaBlendingStart = 100;
            }

            if( pSettings->Effects[j].xVSS.uialphaBlendingFadeInTime > 0
                || pSettings->Effects[j].xVSS.uialphaBlendingFadeOutTime > 0 )
            {
                /*Allocate the alpha blending structure*/
                framingCtx->alphaBlendingStruct =
                    (M4xVSS_internalEffectsAlphaBlending *)M4OSA_32bitAlignedMalloc(
                    sizeof(M4xVSS_internalEffectsAlphaBlending),
                    M4VS, (M4OSA_Char *)"alpha blending structure");

                if( framingCtx->alphaBlendingStruct == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                    M4xVSS_freeCommand(xVSS_context);
                    return M4ERR_ALLOC;
                }
                /*Fill the alpha blending structure*/
                framingCtx->alphaBlendingStruct->m_fadeInTime =
                    pSettings->Effects[j].xVSS.uialphaBlendingFadeInTime;
                framingCtx->alphaBlendingStruct->m_fadeOutTime =
                    pSettings->Effects[j].xVSS.uialphaBlendingFadeOutTime;
                framingCtx->alphaBlendingStruct->m_end =
                    pSettings->Effects[j].xVSS.uialphaBlendingEnd;
                framingCtx->alphaBlendingStruct->m_middle =
                    pSettings->Effects[j].xVSS.uialphaBlendingMiddle;
                framingCtx->alphaBlendingStruct->m_start =
                    pSettings->Effects[j].xVSS.uialphaBlendingStart;

                if( pSettings->Effects[j].xVSS.uialphaBlendingFadeInTime
                    + pSettings->Effects[j].xVSS.uialphaBlendingFadeOutTime
                        > 100 )
                {
                    framingCtx->alphaBlendingStruct->m_fadeOutTime =
                        100 - framingCtx->alphaBlendingStruct->m_fadeInTime;
                }
            }

            /**
            * UTF conversion: convert into the customer format, before being used*/
            pDecodedPath =
                xVSS_context->pSettings->Effects[j].xVSS.pFramingFilePath;
            length = strlen(pDecodedPath);

            if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct
                != M4OSA_NULL && xVSS_context->
                UTFConversionContext.pTempOutConversionBuffer
                != M4OSA_NULL )
            {
                err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                    (M4OSA_Void *)xVSS_context->pSettings->
                    Effects[j].xVSS.pFramingFilePath,
                    (M4OSA_Void *)xVSS_context->
                    UTFConversionContext.pTempOutConversionBuffer,
                    &length);

                if( err != M4NO_ERROR )
                {
                    M4OSA_TRACE1_1(
                        "M4xVSS_SendCommand: M4xVSS_internalConvertFromUTF8 returns err: 0x%x",
                        err);
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    return err;
                }
                pDecodedPath =
                    xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
            }

            /**
            * End of the UTF conversion, use the converted file path*/
            framingCtx->pEffectFilePath = M4OSA_32bitAlignedMalloc(length + 1, M4VS,
                (M4OSA_Char *)"Local Framing file path");

            if( framingCtx->pEffectFilePath == M4OSA_NULL )
            {
                M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                /*FB: to avoid leaks when there is an error in the send command*/
                /* Free Send command */
                M4xVSS_freeCommand(xVSS_context);
                /**/
                return M4ERR_ALLOC;
            }
            memcpy((void *)framingCtx->pEffectFilePath,
                (void *)pDecodedPath, length + 1);

            /* Save framing structure associated with corresponding effect */
            xVSS_context->pSettings->Effects[j].pExtVideoEffectFctCtxt =
                framingCtx;

#else

            framingCtx = (M4xVSS_FramingStruct
                *)M4OSA_32bitAlignedMalloc(sizeof(M4xVSS_FramingStruct),
                M4VS, (M4OSA_Char *)"Context of the framing effect");

            if( framingCtx == M4OSA_NULL )
            {
                M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                /*FB: to avoid leaks when there is an error in the send command*/
                /* Free Send command */
                M4xVSS_freeCommand(xVSS_context);
                /**/
                return M4ERR_ALLOC;
            }

            framingCtx->topleft_x =
                xVSS_context->pSettings->Effects[j].xVSS.topleft_x;
            framingCtx->topleft_y =
                xVSS_context->pSettings->Effects[j].xVSS.topleft_y;

            /* BugFix 1.2.0: Leak when decoding error */
            framingCtx->FramingRgb = M4OSA_NULL;
            framingCtx->FramingYuv = M4OSA_NULL;
            framingCtx->pNext = framingCtx;
            /* Save framing structure associated with corresponding effect */
            xVSS_context->pSettings->Effects[j].pExtVideoEffectFctCtxt =
                framingCtx;

#endif /*DECODE_GIF_ON_SAVING*/

            if( pExt2 != M4OSA_NULL )
            {
                /* Decode the image associated to the effect, and fill framing structure */
                pExt2 += (strlen((const char *)pExt2) - 4);

                result1 = strcmp((const char *)pExt2,(const char *)".rgb");
                result2 = strcmp((const char *)pExt2,(const char *)".RGB");

                if( 0 == result1 || 0 == result2 )
                {
#ifdef DECODE_GIF_ON_SAVING

                    framingCtx->aFramingCtx =
                        (M4xVSS_FramingStruct
                        *)M4OSA_32bitAlignedMalloc(sizeof(M4xVSS_FramingStruct),
                        M4VS,
                        (M4OSA_Char
                        *)
                        "M4xVSS_internalDecodeGIF: Context of the framing effect");

                    if( framingCtx->aFramingCtx == M4OSA_NULL )
                    {
                        M4OSA_TRACE1_0(
                            "Allocation error in M4xVSS_SendCommand");
                        /* TODO: Translate error code of SPS to an xVSS error code */
                        M4xVSS_freeCommand(xVSS_context);
                        return M4ERR_ALLOC;
                    }
                    framingCtx->aFramingCtx->pCurrent =
                        M4OSA_NULL; /* Only used by the first element of the chain */
                    framingCtx->aFramingCtx->previousClipTime = -1;
                    framingCtx->aFramingCtx->FramingYuv = M4OSA_NULL;
                    framingCtx->aFramingCtx->FramingRgb = M4OSA_NULL;
                    framingCtx->aFramingCtx->topleft_x =
                        xVSS_context->pSettings->Effects[j].xVSS.topleft_x;
                    framingCtx->aFramingCtx->topleft_y =
                        xVSS_context->pSettings->Effects[j].xVSS.topleft_y;
                    /*To support ARGB8888 : get the width and height */

                    framingCtx->aFramingCtx->width =
                        xVSS_context->pSettings->Effects[j].xVSS.width;
                    framingCtx->aFramingCtx->height =
                        xVSS_context->pSettings->Effects[j].xVSS.height;
                    M4OSA_TRACE1_1("FRAMMING BEFORE M4xVSS_SendCommand  %d",
                        framingCtx->aFramingCtx->width);
                    M4OSA_TRACE1_1("FRAMMING BEFORE M4xVSS_SendCommand  %d",
                        framingCtx->aFramingCtx->height);

#endif

                    err = M4xVSS_internalConvertARGB888toYUV420_FrammingEffect(
                        xVSS_context,
                        &(xVSS_context->pSettings->Effects[j]),
                        framingCtx->aFramingCtx,xVSS_context->pSettings->xVSS.outputVideoSize);
                    M4OSA_TRACE3_1("FRAMING WIDTH BEFORE M4xVSS_SendCommand  %d",
                        framingCtx->aFramingCtx->width);
                    M4OSA_TRACE3_1("FRAMING HEIGHT BEFORE M4xVSS_SendCommand  %d",
                        framingCtx->aFramingCtx->height);

                    if( err != M4NO_ERROR )
                    {
                        M4OSA_TRACE1_1(
                            "M4xVSS_SendCommand: M4xVSS_internalDecodePNG returned 0x%x",
                            err);
                        /* TODO: Translate error code of SPS to an xVSS error code */
                        M4xVSS_freeCommand(xVSS_context);
                        return err;
                    }
                }
                else
                {
                    M4OSA_TRACE1_1(
                        "M4xVSS_SendCommand: Not supported still picture format 0x%x",
                        err);
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return M4ERR_PARAMETER;
                }
            }
            else if( pPlane != M4OSA_NULL )
            {
#ifdef DECODE_GIF_ON_SAVING

                framingCtx->aFramingCtx = (M4xVSS_FramingStruct
                    *)M4OSA_32bitAlignedMalloc(sizeof(M4xVSS_FramingStruct),
                    M4VS, (M4OSA_Char *)"Context of the framing effect");

                if( framingCtx->aFramingCtx == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return M4ERR_ALLOC;
                }

                framingCtx->aFramingCtx->topleft_x =
                    xVSS_context->pSettings->Effects[j].xVSS.topleft_x;
                framingCtx->aFramingCtx->topleft_y =
                    xVSS_context->pSettings->Effects[j].xVSS.topleft_y;

                /* BugFix 1.2.0: Leak when decoding error */
                framingCtx->aFramingCtx->FramingRgb = M4OSA_NULL;
                framingCtx->aFramingCtx->FramingYuv = M4OSA_NULL;
                framingCtx->aFramingCtx->pNext = framingCtx->aFramingCtx;
                framingCtx->aFramingCtx->pCurrent = framingCtx->aFramingCtx;
                framingCtx->aFramingCtx->duration = 0;
                framingCtx->aFramingCtx->previousClipTime = -1;
                framingCtx->aFramingCtx->FramingRgb =
                    xVSS_context->pSettings->Effects[j].xVSS.pFramingBuffer;
                /* Force input RGB buffer to even size to avoid errors in YUV conversion */
                framingCtx->aFramingCtx->FramingRgb->u_width =
                    framingCtx->aFramingCtx->FramingRgb->u_width & ~1;
                framingCtx->aFramingCtx->FramingRgb->u_height =
                    framingCtx->aFramingCtx->FramingRgb->u_height & ~1;
                /* Input RGB plane is provided, let's convert it to YUV420, and update framing
                structure  */
                err = M4xVSS_internalConvertRGBtoYUV(framingCtx->aFramingCtx);

#else

                framingCtx->FramingRgb =
                    xVSS_context->pSettings->Effects[j].xVSS.pFramingBuffer;
                /* Force input RGB buffer to even size to avoid errors in YUV conversion */
                framingCtx->FramingRgb.u_width =
                    framingCtx->FramingRgb.u_width & ~1;
                framingCtx->FramingRgb.u_height =
                    framingCtx->FramingRgb.u_height & ~1;
                /* Input RGB plane is provided, let's convert it to YUV420, and update framing
                 structure  */
                err = M4xVSS_internalConvertRGBtoYUV(framingCtx);

#endif

                if( err != M4NO_ERROR )
                {
                    M4OSA_TRACE1_1(
                        "M4xVSS_sendCommand: error when converting RGB to YUV: 0w%x",
                        err);
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return err;
                }
            }
            else
            {
                M4OSA_TRACE1_0(
                    "M4xVSS_sendCommand: No input image/plane provided for framing effect.");
                /*FB: to avoid leaks when there is an error in the send command*/
                /* Free Send command */
                M4xVSS_freeCommand(xVSS_context);
                /**/
                return M4ERR_PARAMETER;
            }
        }
        /* CR: Add text handling with external text interface */
        /* If effect type is text, we call external text function to get RGB 565 buffer */
        if( xVSS_context->pSettings->Effects[j].VideoEffectType
            == M4xVSS_kVideoEffectType_Text )
        {
            /* Call the font engine function pointer to get RGB565 buffer */
            /* We transform text effect into framing effect from buffer */
            if( xVSS_context->pSettings->xVSS.pTextRenderingFct != M4OSA_NULL )
            {
                /*FB: add UTF convertion for text buffer*/
                M4OSA_Void *pDecodedPath = M4OSA_NULL;
#ifdef DECODE_GIF_ON_SAVING

                M4xVSS_FramingContext *framingCtx;

#else

                M4xVSS_FramingStruct *framingCtx;

#endif /*DECODE_GIF_ON_SAVING*/

#ifdef DECODE_GIF_ON_SAVING

                framingCtx = (M4xVSS_FramingContext
                    *)M4OSA_32bitAlignedMalloc(sizeof(M4xVSS_FramingContext),
                    M4VS, (M4OSA_Char *)"Context of the framing effect");

                if( framingCtx == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return M4ERR_ALLOC;
                }
                framingCtx->aFramingCtx = M4OSA_NULL;
                framingCtx->aFramingCtx_last = M4OSA_NULL;
                framingCtx->pSPSContext = M4OSA_NULL;
                framingCtx->outputVideoSize =
                    xVSS_context->pSettings->xVSS.outputVideoSize;
                framingCtx->topleft_x =
                    xVSS_context->pSettings->Effects[j].xVSS.topleft_x;
                framingCtx->topleft_y =
                    xVSS_context->pSettings->Effects[j].xVSS.topleft_y;
                framingCtx->bEffectResize =
                    xVSS_context->pSettings->Effects[j].xVSS.bResize;
                framingCtx->pEffectFilePath =
                    xVSS_context->pSettings->Effects[j].xVSS.pFramingFilePath;
                framingCtx->pFileReadPtr = xVSS_context->pFileReadPtr;
                framingCtx->pFileWritePtr = xVSS_context->pFileWritePtr;
                framingCtx->effectDuration =
                    xVSS_context->pSettings->Effects[j].uiDuration;
                framingCtx->b_IsFileGif = M4OSA_FALSE;
                framingCtx->b_animated = M4OSA_FALSE;
                framingCtx->alphaBlendingStruct = M4OSA_NULL;

                /* Save framing structure associated with corresponding effect */
                xVSS_context->pSettings->Effects[j].pExtVideoEffectFctCtxt =
                    framingCtx;

                framingCtx->aFramingCtx = (M4xVSS_FramingStruct
                    *)M4OSA_32bitAlignedMalloc(sizeof(M4xVSS_FramingStruct),
                    M4VS, (M4OSA_Char *)"Context of the framing effect");

                if( framingCtx->aFramingCtx == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return M4ERR_ALLOC;
                }

                framingCtx->aFramingCtx->topleft_x =
                    xVSS_context->pSettings->Effects[j].xVSS.topleft_x;
                framingCtx->aFramingCtx->topleft_y =
                    xVSS_context->pSettings->Effects[j].xVSS.topleft_y;

                /* BugFix 1.2.0: Leak when decoding error */
                framingCtx->aFramingCtx->FramingRgb = M4OSA_NULL;
                framingCtx->aFramingCtx->FramingYuv = M4OSA_NULL;
                framingCtx->aFramingCtx->pNext = framingCtx->aFramingCtx;
                framingCtx->aFramingCtx->pCurrent = framingCtx->aFramingCtx;
                framingCtx->aFramingCtx->duration = 0;
                framingCtx->aFramingCtx->previousClipTime = -1;

                /*Alpha blending*/
                /*Check if the alpha blending parameters are corrects*/
                if( pSettings->Effects[j].xVSS.uialphaBlendingFadeInTime > 100 )
                {
                    pSettings->Effects[j].xVSS.uialphaBlendingFadeInTime = 0;
                }

                if( pSettings->Effects[j].xVSS.uialphaBlendingFadeOutTime > 100 )
                {
                    pSettings->Effects[j].xVSS.uialphaBlendingFadeOutTime = 0;
                }

                if( pSettings->Effects[j].xVSS.uialphaBlendingEnd > 100 )
                {
                    pSettings->Effects[j].xVSS.uialphaBlendingEnd = 100;
                }

                if( pSettings->Effects[j].xVSS.uialphaBlendingMiddle > 100 )
                {
                    pSettings->Effects[j].xVSS.uialphaBlendingMiddle = 100;
                }

                if( pSettings->Effects[j].xVSS.uialphaBlendingStart > 100 )
                {
                    pSettings->Effects[j].xVSS.uialphaBlendingStart = 100;
                }

                if( pSettings->Effects[j].xVSS.uialphaBlendingFadeInTime > 0
                    || pSettings->Effects[j].xVSS.uialphaBlendingFadeOutTime
                    > 0 )
                {
                    /*Allocate the alpha blending structure*/
                    framingCtx->alphaBlendingStruct =
                        (M4xVSS_internalEffectsAlphaBlending *)M4OSA_32bitAlignedMalloc(
                        sizeof(M4xVSS_internalEffectsAlphaBlending),
                        M4VS, (M4OSA_Char *)"alpha blending structure");

                    if( framingCtx->alphaBlendingStruct == M4OSA_NULL )
                    {
                        M4OSA_TRACE1_0(
                            "Allocation error in M4xVSS_SendCommand");
                        M4xVSS_freeCommand(xVSS_context);
                        return M4ERR_ALLOC;
                    }
                    /*Fill the alpha blending structure*/
                    framingCtx->alphaBlendingStruct->m_fadeInTime =
                        pSettings->Effects[j].xVSS.uialphaBlendingFadeInTime;
                    framingCtx->alphaBlendingStruct->m_fadeOutTime =
                        pSettings->Effects[j].xVSS.uialphaBlendingFadeOutTime;
                    framingCtx->alphaBlendingStruct->m_end =
                        pSettings->Effects[j].xVSS.uialphaBlendingEnd;
                    framingCtx->alphaBlendingStruct->m_middle =
                        pSettings->Effects[j].xVSS.uialphaBlendingMiddle;
                    framingCtx->alphaBlendingStruct->m_start =
                        pSettings->Effects[j].xVSS.uialphaBlendingStart;

                    if( pSettings->Effects[j].xVSS.uialphaBlendingFadeInTime
                        + pSettings->Effects[j].xVSS.uialphaBlendingFadeOutTime
                            > 100 )
                    {
                        framingCtx->alphaBlendingStruct->m_fadeOutTime =
                            100 - framingCtx->alphaBlendingStruct->m_fadeInTime;
                    }
                }
#else

                framingCtx = (M4xVSS_FramingStruct
                    *)M4OSA_32bitAlignedMalloc(sizeof(M4xVSS_FramingStruct),
                    M4VS, (M4OSA_Char
                    *)"Context of the framing effect (for text)");

                if( framingCtx == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return M4ERR_ALLOC;
                }

                framingCtx->topleft_x =
                    xVSS_context->pSettings->Effects[j].xVSS.topleft_x;
                framingCtx->topleft_y =
                    xVSS_context->pSettings->Effects[j].xVSS.topleft_y;
                framingCtx->FramingRgb = M4OSA_NULL;

                /* BugFix 1.2.0: Leak when decoding error */
                framingCtx->FramingYuv = M4OSA_NULL;
                framingCtx->pNext = framingCtx;

#endif
                /* Save framing structure associated with corresponding effect */

                xVSS_context->pSettings->Effects[j].pExtVideoEffectFctCtxt =
                    framingCtx;

                /* FB: changes for Video Artist: memcopy pTextBuffer so that it can be changed
                after a complete analysis*/
                if( pSettings->Effects[j].xVSS.pTextBuffer == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0("M4xVSS_SendCommand: pTextBuffer is null");
                    M4xVSS_freeCommand(xVSS_context);
                    return M4ERR_PARAMETER;
                }

                /*Convert text buffer into customer format before being used*/
                /**
                * UTF conversion: convert into the customer format, before being used*/
                pDecodedPath = pSettings->Effects[j].xVSS.pTextBuffer;
                xVSS_context->pSettings->Effects[j].xVSS.textBufferSize =
                    pSettings->Effects[j].xVSS.textBufferSize;

                if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct
                    != M4OSA_NULL && xVSS_context->
                    UTFConversionContext.pTempOutConversionBuffer
                    != M4OSA_NULL )
                {
                    err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                        (M4OSA_Void *)pSettings->
                        Effects[j].xVSS.pTextBuffer,
                        (M4OSA_Void *)xVSS_context->
                        UTFConversionContext.pTempOutConversionBuffer,
                        &length);

                    if( err != M4NO_ERROR )
                    {
                        M4OSA_TRACE1_1(
                            "M4xVSS_SendCommand: M4xVSS_internalConvertFromUTF8 returns err: 0x%x",
                            err);
                        /* Free Send command */
                        M4xVSS_freeCommand(xVSS_context);
                        return err;
                    }
                    pDecodedPath = xVSS_context->
                        UTFConversionContext.pTempOutConversionBuffer;
                    xVSS_context->pSettings->Effects[j].xVSS.textBufferSize =
                        length;
                }
                /**
                * End of the UTF conversion, use the converted file path*/

                xVSS_context->pSettings->
                    Effects[j].xVSS.pTextBuffer = M4OSA_32bitAlignedMalloc(
                    xVSS_context->pSettings->Effects[j].xVSS.textBufferSize + 1,
                    M4VS, (M4OSA_Char *)"Local text buffer effect");

                //xVSS_context->pSettings->Effects[j].xVSS.pTextBuffer =
                // M4OSA_32bitAlignedMalloc(strlen(pSettings->Effects[j].xVSS.pTextBuffer)+1,
                // M4VS, "Local text buffer effect");
                if( xVSS_context->pSettings->Effects[j].xVSS.pTextBuffer
                    == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return M4ERR_ALLOC;
                }

                if( pSettings->Effects[j].xVSS.pTextBuffer != M4OSA_NULL )
                {
                    //memcpy((M4OSA_MemAddr8)xVSS_context->pSettings->Effects[j]
                    //.xVSS.pTextBuffer, (M4OSA_MemAddr8)pSettings->Effects[j].xVSS.pTextBuffer,
                    // strlen(pSettings->Effects[j].xVSS.pTextBuffer)+1);
                    memcpy((void *)xVSS_context->pSettings->
                        Effects[j].xVSS.pTextBuffer,
                        (void *)pDecodedPath, xVSS_context->pSettings->
                        Effects[j].xVSS.textBufferSize + 1);
                }

                /*Allocate the text RGB buffer*/
                framingCtx->aFramingCtx->FramingRgb =
                    (M4VIFI_ImagePlane *)M4OSA_32bitAlignedMalloc(sizeof(M4VIFI_ImagePlane),
                    M4VS,
                    (M4OSA_Char *)"RGB structure for the text effect");

                if( framingCtx->aFramingCtx->FramingRgb == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return M4ERR_ALLOC;
                }

                if( xVSS_context->pSettings->Effects[j].xVSS.uiTextBufferWidth
                    == 0 || xVSS_context->pSettings->
                    Effects[j].xVSS.uiTextBufferHeight == 0 )
                {
                    M4OSA_TRACE1_0(
                        "M4xVSS_SendCommand: text plane width and height are not defined");
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return M4ERR_PARAMETER;
                }
                /* Allocate input RGB text buffer and force it to even size to avoid errors in
                 YUV conversion */
                framingCtx->aFramingCtx->FramingRgb->u_width =
                    xVSS_context->pSettings->
                    Effects[j].xVSS.uiTextBufferWidth & ~1;
                framingCtx->aFramingCtx->FramingRgb->u_height =
                    xVSS_context->pSettings->
                    Effects[j].xVSS.uiTextBufferHeight & ~1;
                framingCtx->aFramingCtx->FramingRgb->u_stride =
                    2 * framingCtx->aFramingCtx->FramingRgb->u_width;
                framingCtx->aFramingCtx->FramingRgb->u_topleft = 0;
                framingCtx->aFramingCtx->FramingRgb->pac_data =
                    (M4VIFI_UInt8 *)M4OSA_32bitAlignedMalloc(
                    framingCtx->aFramingCtx->FramingRgb->u_height
                    * framingCtx->aFramingCtx->FramingRgb->u_stride,
                    M4VS, (M4OSA_Char *)"Text RGB plane->pac_data");

                if( framingCtx->aFramingCtx->FramingRgb->pac_data
                    == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                    /*FB: to avoid leaks when there is an error in the send command*/
                    /* Free Send command */
                    M4xVSS_freeCommand(xVSS_context);
                    /**/
                    return M4ERR_ALLOC;
                }

#ifdef DECODE_GIF_ON_SAVING
                /**/
                /* Call text rendering function */

                err = xVSS_context->pSettings->xVSS.pTextRenderingFct(
                    xVSS_context->pSettings->Effects[j].xVSS.pRenderingData,
                    xVSS_context->pSettings->
                    Effects[j].xVSS.pTextBuffer,
                    xVSS_context->pSettings->
                    Effects[j].xVSS.textBufferSize,
                    &(framingCtx->aFramingCtx->FramingRgb));

                if( err != M4NO_ERROR )
                {
                    M4OSA_TRACE1_0("Text rendering external function failed\n");
                    M4xVSS_freeCommand(xVSS_context);
                    return err;
                }

                /* Check that RGB buffer is set */
                if( framingCtx->aFramingCtx->FramingRgb == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0(
                        "Text rendering function did not set RGB buffer correctly !");
                    M4xVSS_freeCommand(xVSS_context);
                    return M4ERR_PARAMETER;
                }

                /* Convert RGB plane to YUV420 and update framing structure */
                err = M4xVSS_internalConvertRGBtoYUV(framingCtx->aFramingCtx);

                if( err != M4NO_ERROR )
                {
                    M4OSA_TRACE1_1(
                        "M4xVSS_sendCommand: error when converting RGB to YUV: 0w%x",
                        err);
                    M4xVSS_freeCommand(xVSS_context);
                    return err;
                }

#else
                /**/
                /* Call text rendering function */

                err = xVSS_context->pSettings->xVSS.pTextRenderingFct(
                    xVSS_context->pSettings->Effects[j].xVSS.pRenderingData,
                    xVSS_context->pSettings->
                    Effects[j].xVSS.pTextBuffer,
                    xVSS_context->pSettings->
                    Effects[j].xVSS.textBufferSize,
                    &(framingCtx->FramingRgb));

                if( err != M4NO_ERROR )
                {
                    M4OSA_TRACE1_0("Text rendering external function failed\n");
                    M4xVSS_freeCommand(xVSS_context);
                    return err;
                }

                /* Check that RGB buffer is set */
                if( framingCtx->FramingRgb == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0(
                        "Text rendering function did not set RGB buffer correctly !");
                    M4xVSS_freeCommand(xVSS_context);
                    return M4ERR_PARAMETER;
                }

                /* Convert RGB plane to YUV420 and update framing structure */
                err = M4xVSS_internalConvertRGBtoYUV(framingCtx);

                if( err != M4NO_ERROR )
                {
                    M4OSA_TRACE1_1(
                        "M4xVSS_sendCommand: error when converting RGB to YUV: 0w%x",
                        err);
                    M4xVSS_freeCommand(xVSS_context);
                    return err;
                }

#endif /*DECODE_GIF_ON_SAVING*/

                /* Change internally effect type from "text" to framing */

                xVSS_context->pSettings->Effects[j].VideoEffectType =
                    M4xVSS_kVideoEffectType_Framing;
                xVSS_context->pSettings->Effects[j].xVSS.bResize = M4OSA_FALSE;
            }
            else
            {
                M4OSA_TRACE1_0(
                    "M4xVSS_sendCommand: No text rendering function set !!");
                M4xVSS_freeCommand(xVSS_context);
                return M4ERR_PARAMETER;
            }
        }

        /* Allocate the structure to store the data needed by the Fifties effect */
        else if( xVSS_context->pSettings->Effects[j].VideoEffectType
            == M4xVSS_kVideoEffectType_Fifties )
        {
            M4xVSS_FiftiesStruct *fiftiesCtx;

            /* Check the expected frame rate for the fifties effect (must be above 0) */
            if( 0 == xVSS_context->pSettings->
                Effects[j].xVSS.uiFiftiesOutFrameRate )
            {
                M4OSA_TRACE1_0(
                    "The frame rate for the fifties effect must be greater than 0 !");
                M4xVSS_freeCommand(xVSS_context);
                return M4ERR_PARAMETER;
            }

            fiftiesCtx = (M4xVSS_FiftiesStruct
                *)M4OSA_32bitAlignedMalloc(sizeof(M4xVSS_FiftiesStruct),
                M4VS, (M4OSA_Char *)"Context of the fifties effect");

            if( fiftiesCtx == M4OSA_NULL )
            {
                M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                /* Free Send command */
                M4xVSS_freeCommand(xVSS_context);
                return M4ERR_ALLOC;
            }

            fiftiesCtx->previousClipTime = -1;
            fiftiesCtx->fiftiesEffectDuration = 1000 / xVSS_context->pSettings->
                Effects[j].xVSS.uiFiftiesOutFrameRate;
            fiftiesCtx->shiftRandomValue = 0;
            fiftiesCtx->stripeRandomValue = 0;

            /* Save the structure associated with corresponding effect */
            xVSS_context->pSettings->Effects[j].pExtVideoEffectFctCtxt =
                fiftiesCtx;
        }

        /* Allocate the structure to store the data needed by the Color effect */
        else if( xVSS_context->pSettings->Effects[j].VideoEffectType
            == M4xVSS_kVideoEffectType_ColorRGB16
            || xVSS_context->pSettings->Effects[j].VideoEffectType
            == M4xVSS_kVideoEffectType_BlackAndWhite
            || xVSS_context->pSettings->Effects[j].VideoEffectType
            == M4xVSS_kVideoEffectType_Pink
            || xVSS_context->pSettings->Effects[j].VideoEffectType
            == M4xVSS_kVideoEffectType_Green
            || xVSS_context->pSettings->Effects[j].VideoEffectType
            == M4xVSS_kVideoEffectType_Sepia
            || xVSS_context->pSettings->Effects[j].VideoEffectType
            == M4xVSS_kVideoEffectType_Negative
            || xVSS_context->pSettings->Effects[j].VideoEffectType
            == M4xVSS_kVideoEffectType_Gradient )
        {
            M4xVSS_ColorStruct *ColorCtx;

            ColorCtx =
                (M4xVSS_ColorStruct *)M4OSA_32bitAlignedMalloc(sizeof(M4xVSS_ColorStruct),
                M4VS, (M4OSA_Char *)"Context of the color effect");

            if( ColorCtx == M4OSA_NULL )
            {
                M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
                /* Free Send command */
                M4xVSS_freeCommand(xVSS_context);
                return M4ERR_ALLOC;
            }

            ColorCtx->colorEffectType =
                xVSS_context->pSettings->Effects[j].VideoEffectType;

            if( xVSS_context->pSettings->Effects[j].VideoEffectType
                == M4xVSS_kVideoEffectType_ColorRGB16
                || xVSS_context->pSettings->Effects[j].VideoEffectType
                == M4xVSS_kVideoEffectType_Gradient )
            {
                ColorCtx->rgb16ColorData =
                    xVSS_context->pSettings->Effects[j].xVSS.uiRgb16InputColor;
            }
            else
            {
                ColorCtx->rgb16ColorData = 0;
            }

            /* Save the structure associated with corresponding effect */
            xVSS_context->pSettings->Effects[j].pExtVideoEffectFctCtxt =
                ColorCtx;
        }
    }

    /**********************************
    Background music registering
    **********************************/
    if( pSettings->xVSS.pBGMtrack != M4OSA_NULL && isNewBGM == M4OSA_TRUE )
    {
#ifdef PREVIEW_ENABLED

        M4xVSS_MCS_params *pParams;
        M4OSA_Char *out_pcm;
        /*UTF conversion support*/
        M4OSA_Void *pDecodedPath = M4OSA_NULL;

#endif

        /* We save output file pointer, because we will need to use it when saving audio mixed
         file (last save step) */

        xVSS_context->pOutputFile = xVSS_context->pSettings->pOutputFile;
        xVSS_context->pTemporaryFile = xVSS_context->pSettings->pTemporaryFile;

        /* If a previous BGM has already been registered, delete it */
        /* Here can be implemented test to know if the same BGM is registered */
        if( xVSS_context->pSettings->xVSS.pBGMtrack != M4OSA_NULL )
        {
            if( xVSS_context->pSettings->xVSS.pBGMtrack->pFile != M4OSA_NULL )
            {
                free(xVSS_context->pSettings->xVSS.pBGMtrack->
                    pFile);
                xVSS_context->pSettings->xVSS.pBGMtrack->pFile = M4OSA_NULL;
            }
            free(xVSS_context->pSettings->xVSS.pBGMtrack);
            xVSS_context->pSettings->xVSS.pBGMtrack = M4OSA_NULL;
        }

        /* Allocate BGM */
        xVSS_context->pSettings->xVSS.pBGMtrack =
            (M4xVSS_BGMSettings *)M4OSA_32bitAlignedMalloc(sizeof(M4xVSS_BGMSettings), M4VS,
            (M4OSA_Char *)"xVSS_context->pSettings->xVSS.pBGMtrack");

        if( xVSS_context->pSettings->xVSS.pBGMtrack == M4OSA_NULL )
        {
            M4xVSS_freeCommand(xVSS_context);
            M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
            return M4ERR_ALLOC;
        }

        /* Copy input structure to our structure */
        memcpy((void *)xVSS_context->pSettings->xVSS.pBGMtrack,
            (void *)pSettings->xVSS.pBGMtrack,
            sizeof(M4xVSS_BGMSettings));
        /* Allocate file name, and copy file name buffer to our structure */
        xVSS_context->pSettings->xVSS.pBGMtrack->pFile =
            M4OSA_32bitAlignedMalloc((strlen(pSettings->xVSS.pBGMtrack->pFile)
            + 1), M4VS, (M4OSA_Char *)"xVSS BGM file path");

        if( xVSS_context->pSettings->xVSS.pBGMtrack->pFile == M4OSA_NULL )
        {
            M4xVSS_freeCommand(xVSS_context);
            M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
            return M4ERR_ALLOC;
        }
        memcpy((void *)xVSS_context->pSettings->xVSS.pBGMtrack->pFile,
            (void *)pSettings->xVSS.pBGMtrack->pFile,
            strlen(pSettings->xVSS.pBGMtrack->pFile) + 1);

#ifdef PREVIEW_ENABLED
        /* Decode BGM track to pcm output file */

        pParams =
            (M4xVSS_MCS_params *)M4OSA_32bitAlignedMalloc(sizeof(M4xVSS_MCS_params), M4VS,
            (M4OSA_Char *)"Element of MCS Params (for BGM)");

        if( pParams == M4OSA_NULL )
        {
            M4xVSS_freeCommand(xVSS_context);
            M4OSA_TRACE1_0(
                "M4xVSS_sendCommand: Problem when allocating one element MCS Params");
            return M4ERR_ALLOC;
        }

        /* Initialize the pointers in case of problem (PR 2273) */
        pParams->pFileIn = M4OSA_NULL;
        pParams->pFileOut = M4OSA_NULL;
        pParams->pFileTemp = M4OSA_NULL;
        pParams->pNext = M4OSA_NULL;
        pParams->BeginCutTime = 0;
        pParams->EndCutTime = 0;

        if( xVSS_context->pMCSparamsList
            == M4OSA_NULL ) /* Means it is the first element of the list */
        {
            /* Initialize the xVSS context with the first element of the list */
            xVSS_context->pMCSparamsList = pParams;

        }
        else
        {
            M4xVSS_MCS_params *pParams_temp = xVSS_context->pMCSparamsList;
            M4xVSS_MCS_params *pParams_prev = M4OSA_NULL;

            /* Parse MCS params chained list to find and delete BGM element */
            while( pParams_temp != M4OSA_NULL )
            {
                if( pParams_temp->isBGM == M4OSA_TRUE )
                {
                    /* Remove this element */
                    if( pParams_temp->pFileIn != M4OSA_NULL )
                    {
                        free(pParams_temp->pFileIn);
                        pParams_temp->pFileIn = M4OSA_NULL;
                    }

                    if( pParams_temp->pFileOut != M4OSA_NULL )
                    {
                        /* Remove PCM temporary file */
                        remove((const char *)pParams_temp->pFileOut);
                        free(pParams_temp->pFileOut);
                        pParams_temp->pFileOut = M4OSA_NULL;
                    }
                    /* Chain previous element with next element = remove BGM chained
                         list element */
                    if( pParams_prev != M4OSA_NULL )
                    {
                        pParams_prev->pNext = pParams_temp->pNext;
                    }
                    /* If current pointer is the first of the chained list and next pointer of
                    the chained list is NULL */
                    /* it means that there was only one element in the list */
                    /* => we put the context variable to NULL to reaffect the first chained list
                     element */
                    if( pParams_temp == xVSS_context->pMCSparamsList
                        && pParams_temp->pNext == M4OSA_NULL )
                    {
                        xVSS_context->pMCSparamsList = M4OSA_NULL;
                    }
                    /* In that case, BGM pointer is the first one, but there are others elements
                     after it */
                    /* So, we need to change first chained list element */
                    else if( pParams_temp->pNext != M4OSA_NULL
                        && pParams_prev == M4OSA_NULL )
                    {
                        xVSS_context->pMCSparamsList = pParams_temp->pNext;
                    }

                    if( pParams_temp->pNext != M4OSA_NULL )
                    {
                        pParams_prev = pParams_temp->pNext;
                        free(pParams_temp);
                        pParams_temp = M4OSA_NULL;
                        pParams_temp = pParams_prev;
                    }
                    else
                    {
                        free(pParams_temp);
                        pParams_temp = M4OSA_NULL;
                    }
                }
                else
                {
                    pParams_prev = pParams_temp;
                    pParams_temp = pParams_temp->pNext;
                }
            }
            /* We need to initialize the last element of the chained list to be able to add new
             BGM element */
            pMCS_last = pParams_prev;

            if( xVSS_context->pMCSparamsList == M4OSA_NULL )
            {
                /* In that case, it means that there was only one element in the chained list */
                /* So, we need to save the new params*/
                xVSS_context->pMCSparamsList = pParams;
            }
            else
            {
                /* Update next pointer of the previous last element of the chain */
                pMCS_last->pNext = pParams;
            }

        }

        /* Fill the last M4xVSS_MCS_params element */
        pParams->InputFileType =
            xVSS_context->pSettings->xVSS.pBGMtrack->FileType;
        pParams->OutputFileType = M4VIDEOEDITING_kFileType_PCM;
        pParams->OutputVideoFormat = M4VIDEOEDITING_kNoneVideo;
        pParams->OutputVideoFrameSize = M4VIDEOEDITING_kQCIF;
        pParams->OutputVideoFrameRate = M4VIDEOEDITING_k15_FPS;

        if( xVSS_context->pSettings->xVSS.outputAudioFormat
            == M4VIDEOEDITING_kAAC )
        {
            pParams->OutputAudioFormat = M4VIDEOEDITING_kAAC;
            pParams->OutputAudioSamplingFrequency = M4VIDEOEDITING_kDefault_ASF;

            /*FB: VAL CR P4ME00003076
            The output audio bitrate in the AAC case is now directly given by the user*/
            /*Check if the audio bitrate is correctly defined*/
            /*Mono
            MCS values for AAC Mono are min: 16kbps and max: 192 kbps*/
            if( xVSS_context->pSettings->xVSS.outputAudioBitrate
                >= M4VIDEOEDITING_k16_KBPS
                && xVSS_context->pSettings->xVSS.outputAudioBitrate
                <= M4VIDEOEDITING_k192_KBPS
                && xVSS_context->pSettings->xVSS.bAudioMono == M4OSA_TRUE )
            {
                pParams->OutputAudioBitrate =
                    xVSS_context->pSettings->xVSS.outputAudioBitrate;
            }
            /*Stereo
            MCS values for AAC Mono are min: 32kbps and max: 192 kbps*/
            else if( xVSS_context->pSettings->xVSS.outputAudioBitrate
                >= M4VIDEOEDITING_k32_KBPS
                && xVSS_context->pSettings->xVSS.outputAudioBitrate
                <= M4VIDEOEDITING_k192_KBPS
                && xVSS_context->pSettings->xVSS.bAudioMono == M4OSA_FALSE )
            {
                pParams->OutputAudioBitrate =
                    xVSS_context->pSettings->xVSS.outputAudioBitrate;
            }
            else
            {
                pParams->OutputAudioBitrate = M4VIDEOEDITING_k32_KBPS;
            }
            pParams->bAudioMono = xVSS_context->pSettings->xVSS.bAudioMono;
        }
        else
        {
            pParams->OutputAudioFormat = M4VIDEOEDITING_kAMR_NB;
            pParams->OutputAudioSamplingFrequency = M4VIDEOEDITING_kDefault_ASF;
            pParams->OutputAudioBitrate = M4VIDEOEDITING_k12_2_KBPS;
            pParams->bAudioMono = M4OSA_TRUE;
        }
        pParams->OutputVideoBitrate = M4VIDEOEDITING_kUndefinedBitrate;

        /* Prepare output filename */
        /* 21 is the size of "preview_16000_2.pcm" + \0 */
        out_pcm =
            (M4OSA_Char *)M4OSA_32bitAlignedMalloc(strlen(xVSS_context->pTempPath)
            + 21, M4VS, (M4OSA_Char *)"Temp char* for pcmPreviewFile");

        if( out_pcm == M4OSA_NULL )
        {
            M4xVSS_freeCommand(xVSS_context);
            M4OSA_TRACE1_0("Allocation error in M4xVSS_Init");
            return M4ERR_ALLOC;
        }

        /* Copy temporary path to final preview path string */
        M4OSA_chrNCopy(out_pcm, xVSS_context->pTempPath,
            strlen(xVSS_context->pTempPath) + 1);

        /* Depending of the output sample frequency and nb of channels, we construct preview
        output filename */
        if( xVSS_context->pSettings->xVSS.outputAudioFormat
            == M4VIDEOEDITING_kAAC )
        {
            /* Construct output temporary PCM filename */
            if( xVSS_context->pSettings->xVSS.bAudioMono == M4OSA_TRUE )
            {
                strncat((char *)out_pcm, (const char *)"preview_16000_1.pcm\0",
                    20);
            }
            else
            {
                strncat((char *)out_pcm, (const char *)"preview_16000_2.pcm\0",
                    20);
            }
        }
        else if( xVSS_context->pSettings->xVSS.outputAudioFormat
            == M4VIDEOEDITING_kAMR_NB )
        {
            /* Construct output temporary PCM filename */
            strncat((char *)out_pcm, (const char *)"preview_08000_1.pcm\0", 20);
        }
        else
        {
            if( out_pcm != M4OSA_NULL )
            {
                free(out_pcm);
                out_pcm = M4OSA_NULL;
            }
            M4xVSS_freeCommand(xVSS_context);
            M4OSA_TRACE1_0("Bad audio output format \n");
            return M4ERR_PARAMETER;
        }

        xVSS_context->pcmPreviewFile = out_pcm;

        /**
        * UTF conversion: convert into the customer format, before being used*/
        pDecodedPath = out_pcm;
        length = strlen(pDecodedPath);

        if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct != M4OSA_NULL
            && xVSS_context->UTFConversionContext.pTempOutConversionBuffer
            != M4OSA_NULL )
        {
            err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                (M4OSA_Void *)out_pcm, (M4OSA_Void *)xVSS_context->
                UTFConversionContext.pTempOutConversionBuffer, &length);

            if( err != M4NO_ERROR )
            {
                M4OSA_TRACE1_1(
                    "M4xVSS_SendCommand: M4xVSS_internalConvertFromUTF8 returns err: 0x%x",
                    err);
                /* Free Send command */
                M4xVSS_freeCommand(xVSS_context);
                return err;
            }
            pDecodedPath =
                xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
        }

        /**
        * End of the UTF conversion, use the converted file path*/
        xVSS_context->pcmPreviewFile =
            (M4OSA_Void *)M4OSA_32bitAlignedMalloc(length + 1, M4VS,
            (M4OSA_Char *)"pcmPreviewFile");

        if( xVSS_context->pcmPreviewFile == M4OSA_NULL )
        {
            M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
            free(out_pcm);
            out_pcm = M4OSA_NULL;
            /*FB: to avoid leaks when there is an error in the send command*/
            /* Free Send command */
            M4xVSS_freeCommand(xVSS_context);
            /**/
            return M4ERR_ALLOC;
        }
        memcpy((void *)xVSS_context->pcmPreviewFile, (void *)pDecodedPath, length + 1);

        /* Free temporary output filename */
        if( out_pcm != M4OSA_NULL )
        {
            free(out_pcm);
            out_pcm = M4OSA_NULL;
        }

        pParams->pFileOut = M4OSA_32bitAlignedMalloc((length + 1), M4VS,
            (M4OSA_Char *)"MCS BGM Params: file out");

        if( pParams->pFileOut == M4OSA_NULL )
        {
            M4xVSS_freeCommand(xVSS_context);
            M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
            return M4ERR_ALLOC;
        }
        pParams->pFileTemp = M4OSA_NULL;

        memcpy((void *)pParams->pFileOut,(void *) xVSS_context->pcmPreviewFile,
            (length + 1)); /* Copy output file path */

        /**
        * UTF conversion: convert into the customer format, before being used*/

        pDecodedPath = xVSS_context->pSettings->xVSS.pBGMtrack->pFile;
        length = strlen(pDecodedPath);

        if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct != M4OSA_NULL
            && xVSS_context->UTFConversionContext.pTempOutConversionBuffer
            != M4OSA_NULL )
        {
            err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                (M4OSA_Void *)xVSS_context->pSettings->xVSS.pBGMtrack->
                pFile, (M4OSA_Void *)xVSS_context->
                UTFConversionContext.pTempOutConversionBuffer, &length);

            if( err != M4NO_ERROR )
            {
                M4OSA_TRACE1_1(
                    "M4xVSS_SendCommand: M4xVSS_internalConvertFromUTF8 returns err: 0x%x",
                    err);
                /* Free Send command */
                M4xVSS_freeCommand(xVSS_context);
                return err;
            }
            pDecodedPath =
                xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
        }

        /**
        * End of the UTF conversion, use the converted file path*/
        pParams->pFileIn = (M4OSA_Void *)M4OSA_32bitAlignedMalloc((length + 1), M4VS,
            (M4OSA_Char *)"MCS BGM Params: file in");

        if( pParams->pFileIn == M4OSA_NULL )
        {
            M4xVSS_freeCommand(xVSS_context);
            M4OSA_TRACE1_0("Allocation error in M4xVSS_SendCommand");
            return M4ERR_ALLOC;
        }
        memcpy((void *)pParams->pFileIn, (void *)pDecodedPath,
            (length + 1)); /* Copy input file path */

        pParams->isBGM = M4OSA_TRUE;
        pParams->isCreated = M4OSA_FALSE;
        xVSS_context->nbStepTotal++;
        bIsTranscoding = M4OSA_TRUE;
#endif /* PREVIEW_ENABLED */

    }
    else if( pSettings->xVSS.pBGMtrack != M4OSA_NULL
        && isNewBGM == M4OSA_FALSE )
    {
#ifdef PREVIEW_ENABLED
        /* BGM is the same as previously, no need to redecode audio */
        /* Need to update MCS params chained list, to signal M4xVSS_step function to skip
        BGM decoding */

        M4xVSS_MCS_params *pParams_temp = xVSS_context->pMCSparamsList;
        M4xVSS_MCS_params *pParams_prev = M4OSA_NULL;

#endif /* PREVIEW_ENABLED */
        /* We save output file pointer, because we will need to use it when saving audio
         mixed file (last save step) */

        xVSS_context->pOutputFile = xVSS_context->pSettings->pOutputFile;
        xVSS_context->pTemporaryFile = xVSS_context->pSettings->pTemporaryFile;

        /* Re-write BGM settings in case they have changed between two sendCommand */
        xVSS_context->pSettings->xVSS.pBGMtrack->uiAddCts =
            pSettings->xVSS.pBGMtrack->uiAddCts;
        xVSS_context->pSettings->xVSS.pBGMtrack->uiAddVolume =
            pSettings->xVSS.pBGMtrack->uiAddVolume;
        xVSS_context->pSettings->xVSS.pBGMtrack->uiBeginLoop =
            pSettings->xVSS.pBGMtrack->uiBeginLoop;
        xVSS_context->pSettings->xVSS.pBGMtrack->uiEndLoop =
            pSettings->xVSS.pBGMtrack->uiEndLoop;

#ifdef PREVIEW_ENABLED
        /* Parse MCS params chained list to find and delete BGM element */

        while( pParams_temp != M4OSA_NULL )
        {
            if( pParams_temp->isBGM == M4OSA_TRUE )
            {
                pParams_temp->isCreated = M4OSA_TRUE;
                break;
            }
            pParams_prev = pParams_temp;
            pParams_temp = pParams_temp->pNext;
        }

#endif /* PREVIEW_ENABLED */

        M4OSA_TRACE2_0("M4xVSS_SendCommand has been recalled, BGM is the same");
    }
    else
    {
        M4OSA_TRACE1_0("No BGM in this xVSS command");

        if( xVSS_context->pSettings->xVSS.pBGMtrack != M4OSA_NULL )
        {
#ifdef PREVIEW_ENABLED
            /* Need to remove MCS previous params chained list */

            M4xVSS_MCS_params *pParams_temp = xVSS_context->pMCSparamsList;
            M4xVSS_MCS_params *pParams_prev = M4OSA_NULL;

            /* Parse MCS params chained list to find and delete BGM element */
            while( pParams_temp != M4OSA_NULL )
            {
                if( pParams_temp->isBGM == M4OSA_TRUE )
                {
                    /* Remove this element */
                    if( pParams_temp->pFileIn != M4OSA_NULL )
                    {
                        free(pParams_temp->pFileIn);
                        pParams_temp->pFileIn = M4OSA_NULL;
                    }

                    if( pParams_temp->pFileOut != M4OSA_NULL )
                    {
                        free(pParams_temp->pFileOut);
                        pParams_temp->pFileOut = M4OSA_NULL;
                    }
                    /* Chain previous element with next element */
                    if( pParams_prev != M4OSA_NULL )
                    {
                        pParams_prev->pNext = pParams_temp->pNext;
                    }
                    /* If current pointer is the first of the chained list and next pointer
                     of the chained list is NULL */
                    /* it means that there was only one element in the list */
                    /* => we put the context variable to NULL */
                    if( pParams_temp == xVSS_context->pMCSparamsList
                        && pParams_temp->pNext == M4OSA_NULL )
                    {
                        free(pParams_temp);
                        xVSS_context->pMCSparamsList = M4OSA_NULL;
                    }
                    /* In that case, BGM pointer is the first one, but there are others
                     elements after it */
                    /* So, we need to change first chained list element */
                    else if( pParams_temp->pNext != M4OSA_NULL )
                    {
                        xVSS_context->pMCSparamsList = pParams_temp->pNext;
                        free(pParams_temp);
                        pParams_temp = M4OSA_NULL;
                    }
                    /* In all other cases, nothing else to do except freeing the chained
                    list element */
                    else
                    {
                        free(pParams_temp);
                        pParams_temp = M4OSA_NULL;
                    }
                    break;
                }
                pParams_prev = pParams_temp;
                pParams_temp = pParams_temp->pNext;
            }

#endif /* PREVIEW_ENABLED */
            /* Here, we unallocate all BGM components and put xVSS_context->pSettings->
            xVSS.pBGMtrack to NULL */

            if( xVSS_context->pSettings->xVSS.pBGMtrack != M4OSA_NULL )
            {
                if( xVSS_context->pSettings->xVSS.pBGMtrack->pFile
                    != M4OSA_NULL )
                {
                    free(xVSS_context->pSettings->xVSS.pBGMtrack->pFile);
                    xVSS_context->pSettings->xVSS.pBGMtrack->pFile = M4OSA_NULL;
                }
                free(xVSS_context->pSettings->xVSS.pBGMtrack);
                xVSS_context->pSettings->xVSS.pBGMtrack = M4OSA_NULL;
            }
        }
    }

    /* Changed to be able to mix with video only files -> in case no master clip is found
    (i.e only JPG input or video only input) */
    /* and if there is a BGM, we force the added volume to 100 (i.e replace audio) */

    if( masterClip == -1
        && xVSS_context->pSettings->xVSS.pBGMtrack != M4OSA_NULL )
    {
        /* In that case, it means that no input 3GP file has a video track.
        Therefore, if a mixing is asked, it will fail. Thus, we force replace audio. */
        xVSS_context->pSettings->xVSS.pBGMtrack->uiAddVolume = 100;
    }

    /* Save clip number to know if a M4xVSS_sendCommand has already been called */
    xVSS_context->previousClipNumber = xVSS_context->pSettings->uiClipNumber;

    /* Change state */
    xVSS_context->m_state = M4xVSS_kStateAnalyzing;

    /* In case of MMS use case, we compute here the max video bitrate */
    /* In case of too low bitrate, a specific warning is returned */
    if( xVSS_context->pSettings->xVSS.outputFileSize != 0 && totalDuration > 0 )
    {
        M4OSA_UInt32 targetedBitrate = 0;
        M4VIDEOEDITING_ClipProperties fileProperties;
        M4OSA_Double ratio;

        if( xVSS_context->pSettings->xVSS.pBGMtrack != M4OSA_NULL )
        {
            if( xVSS_context->pSettings->xVSS.pBGMtrack->uiAddVolume
                == 100 ) /* We are in "replace audio mode, need to check the filetype */
            {
                if( xVSS_context->pSettings->xVSS.pBGMtrack->FileType
                    == M4VIDEOEDITING_kFileType_3GPP )
                {
                    M4OSA_Void *pDecodedPath;
                    /**
                    * UTF conversion: convert into the customer format, before being used*/
                    pDecodedPath =
                        xVSS_context->pSettings->xVSS.pBGMtrack->pFile;
                    length = strlen(pDecodedPath);

                    if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct
                        != M4OSA_NULL && xVSS_context->
                        UTFConversionContext.pTempOutConversionBuffer
                        != M4OSA_NULL )
                    {
                        err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                            (M4OSA_Void *)xVSS_context->pSettings->
                            xVSS.pBGMtrack->pFile,
                            (M4OSA_Void *)xVSS_context->
                            UTFConversionContext.
                            pTempOutConversionBuffer, &length);

                        if( err != M4NO_ERROR )
                        {
                            M4OSA_TRACE1_1("M4xVSS_SendCommand: \
                                M4xVSS_internalConvertFromUTF8 returns err: 0x%x",
                                err);
                            /* Free Send command */
                            M4xVSS_freeCommand(xVSS_context);
                            return err;
                        }
                        pDecodedPath = xVSS_context->
                            UTFConversionContext.pTempOutConversionBuffer;
                    }

                    /**
                    * End of the UTF conversion, use the converted file path*/
                    err =
                        M4xVSS_internalGetProperties(xVSS_context, pDecodedPath,
                        &fileProperties);

                    /* Get the properties of the BGM track */
                    /*err = M4xVSS_internalGetProperties(xVSS_context, xVSS_context->pSettings->
                    xVSS.pBGMtrack->pFile, &fileProperties);*/
                    if( err != M4NO_ERROR )
                    {
                        M4OSA_TRACE1_1(
                            "M4xVSS_sendCommand: M4xVSS_internalGetProperties returned an error:\
                             0x%x", err);
                        return err;
                    }

                    if( fileProperties.AudioStreamType
                        != M4VIDEOEDITING_kAMR_NB )
                    {
                        M4OSA_TRACE1_0(
                            "M4xVSS_sendCommand: Impossible to use MMS mode with BGM != AMR-NB");
                        return M4ERR_PARAMETER;
                    }
                }
                else if( xVSS_context->pSettings->xVSS.pBGMtrack->FileType
                    != M4VIDEOEDITING_kFileType_AMR
                    && xVSS_context->pSettings->xVSS.pBGMtrack->FileType
                    != M4VIDEOEDITING_kFileType_MP3 )
                {
                    M4OSA_TRACE1_0("M4xVSS_sendCommand: Bad input BGM file");
                    return M4ERR_PARAMETER;
                }
            }
        }

        /* Compute targeted bitrate, with 8% margin (moov) */
        if( totalDuration > 1000 )
        {
            targetedBitrate =
                (M4OSA_UInt32)(( xVSS_context->pSettings->xVSS.outputFileSize
                * 8 * 0.84) / (totalDuration / 1000));
        }
        else
        {
            targetedBitrate = 0;
        }

        /* Remove audio bitrate */
        if( targetedBitrate >= 12200 )
        {
            targetedBitrate -= 12200; /* Only AMR is supported in MMS case */
        }
        else
        {
            targetedBitrate = 0;
        }

        /* Compute an indicator of "complexity" depending on nb of sequences and total duration */
        /* The highest is the number of sequences, the more there are some I frames */
        /* In that case, it is necessary to reduce the target bitrate */
        ratio =
            (M4OSA_Double)((M4OSA_Double)(xVSS_context->pSettings->uiClipNumber
            * 100000) / (M4OSA_Double)(totalDuration));
        M4OSA_TRACE2_3(
            "Ratio clip_nb/duration = %f\nTargeted bitrate = %d\nTotal duration: %d",
            (M4OSA_Double)((M4OSA_Double)(xVSS_context->pSettings->uiClipNumber
            * 100000) / (M4OSA_Double)(totalDuration)),
            targetedBitrate, totalDuration);

        if( ratio > 50 && ratio <= 75 )
        {
            /* It means that there is a potential risk of having a higher file size
            than specified */
            targetedBitrate -= (M4OSA_UInt32)(targetedBitrate * 0.1);
            M4OSA_TRACE2_2(
                "New bitrate1 !!\nRatio clip_nb/duration = %f\nTargeted bitrate = %d",
                ratio, targetedBitrate);
        }
        else if( ratio > 75 )
        {
            targetedBitrate -= (M4OSA_UInt32)(targetedBitrate * 0.15);
            M4OSA_TRACE2_2(
                "New bitrate2 !!\nRatio clip_nb/duration = %f\nTargeted bitrate = %d",
                ratio, targetedBitrate);
        }

        /*CR 3283 MMS use case for VAL:
        Decrease the output file size to keep a margin of 5%
        The writer will stop when the targeted output file size will be reached*/
        xVSS_context->pSettings->xVSS.outputFileSize -=
            (M4OSA_UInt32)(xVSS_context->pSettings->xVSS.outputFileSize * 0.05);

        switch( xVSS_context->pSettings->xVSS.outputVideoSize )
        {
            case M4VIDEOEDITING_kSQCIF:
                if( targetedBitrate < 32000 )
                {
                    xVSS_context->targetedBitrate = 32000;
                    return M4VSS3GPP_WAR_OUTPUTFILESIZE_EXCEED;
                }
                break;

            case M4VIDEOEDITING_kQQVGA:
                if( targetedBitrate < 32000 )              /*48000)*/
                {
                    xVSS_context->targetedBitrate = 32000; /*48000;*/
                    return M4VSS3GPP_WAR_OUTPUTFILESIZE_EXCEED;
                }
                break;

            case M4VIDEOEDITING_kQCIF:
                if( targetedBitrate < 48000 )              /*64000)*/
                {
                    xVSS_context->targetedBitrate = 48000; /*64000;*/
                    return M4VSS3GPP_WAR_OUTPUTFILESIZE_EXCEED;
                }
                break;

            case M4VIDEOEDITING_kQVGA:
                if( targetedBitrate < 64000 )              /*128000)*/
                {
                    xVSS_context->targetedBitrate = 64000; /*128000;*/
                    return M4VSS3GPP_WAR_OUTPUTFILESIZE_EXCEED;
                }
                break;

            case M4VIDEOEDITING_kCIF:
                if( targetedBitrate < 128000 )
                {
                    xVSS_context->targetedBitrate = 128000;
                    return M4VSS3GPP_WAR_OUTPUTFILESIZE_EXCEED;
                }
                break;

            case M4VIDEOEDITING_kVGA:
                if( targetedBitrate < 192000 )
                {
                    xVSS_context->targetedBitrate = 192000;
                    return M4VSS3GPP_WAR_OUTPUTFILESIZE_EXCEED;
                }
                break;

            default:
                /* Cannot happen */
                M4OSA_TRACE1_0(
                    "M4xVSS_sendCommand: Error in output fileSize !");
                return M4ERR_PARAMETER;
                break;
        }
        xVSS_context->targetedBitrate = (M4OSA_UInt32)targetedBitrate;
    }

    if( bIsTranscoding )
    {
        return M4VSS3GPP_WAR_TRANSCODING_NECESSARY;
    }
    else
    {
        return M4NO_ERROR;
    }
}

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_SaveStart(M4OSA_Context pContext, M4OSA_Char* pFilePath)
 * @brief        This function prepare the save
 * @note        The xVSS create 3GP edited final file
 *                This function must be called once M4xVSS_Step has returned
 *                M4VSS3GPP_WAR_ANALYZING_DONE
 *                After this function, the user must call M4xVSS_Step until
 *                it returns another error than M4NO_ERROR.
 *
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @param    pFilePath            (IN) If the user wants to provide a different
 *                                output filename, else can be NULL (allocated by the user)
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_ALLOC:        Memory allocation has failed
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_SaveStart( M4OSA_Context pContext, M4OSA_Void *pFilePath,
                           M4OSA_UInt32 filePathSize )
{
    M4xVSS_Context *xVSS_context = (M4xVSS_Context *)pContext;
    M4OSA_ERR err;

    /*Add for UTF conversion: copy the pSettings structure into a new pCurrentEditSettings*/
    M4VSS3GPP_EditSettings *pEditSavingSettings = M4OSA_NULL;
    M4OSA_UInt8 i, j;
    M4OSA_UInt32 offset = 0;
    M4OSA_UInt8 nbEffects = 0;
    /*only for UTF conversion support*/
    M4OSA_Void *pDecodedPath = M4OSA_NULL;
    M4OSA_UInt32 length = 0;
    /**/

    /* Check state */
    if( xVSS_context->m_state != M4xVSS_kStateOpened )
    {
        M4OSA_TRACE1_1(
            "Bad state when calling M4xVSS_SaveStart function! State is %d",
            xVSS_context->m_state);
        return M4ERR_STATE;
    }

    /* RC: to temporary handle changing of output filepath */
    /* TO BE CHANGED CLEANLY WITH A MALLOC/MEMCPY !!!! */
    if( pFilePath != M4OSA_NULL )
    {
        if( xVSS_context->pSettings->pOutputFile != M4OSA_NULL )
        {
            /*it means that pOutputFile has been allocated in M4xVSS_sendCommand()*/
            free(xVSS_context->pSettings->pOutputFile);
            xVSS_context->pSettings->pOutputFile = M4OSA_NULL;
            xVSS_context->pSettings->uiOutputPathSize = 0;
        }

        pDecodedPath = pFilePath;
        /*As all inputs of the xVSS are in UTF8, convert the output file path into the customer
         format*/
        if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct != M4OSA_NULL
            && xVSS_context->UTFConversionContext.pTempOutConversionBuffer
            != M4OSA_NULL )
        {
            err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                (M4OSA_Void *)pFilePath, (M4OSA_Void *)xVSS_context->
                UTFConversionContext.pTempOutConversionBuffer, &length);

            if( err != M4NO_ERROR )
            {
                M4OSA_TRACE1_1(
                    "M4xVSS_SaveStart: M4xVSS_internalConvertFromUTF8 returns err: 0x%x",
                    err);
                return err;
            }
            pDecodedPath =
                xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
            filePathSize = length;
        }

        xVSS_context->pOutputFile =
            (M4OSA_Void *)M4OSA_32bitAlignedMalloc(filePathSize + 1, M4VS,
            (M4OSA_Char *)"M4xVSS_SaveStart: output file");

        if( xVSS_context->pOutputFile == M4OSA_NULL )
        {
            M4OSA_TRACE1_0("Allocation error in M4xVSS_SaveStart");
            return M4ERR_ALLOC;
        }
        memcpy((void *)xVSS_context->pOutputFile, (void *)pDecodedPath, filePathSize + 1);
        xVSS_context->pOutputFile[filePathSize] = '\0';
        xVSS_context->pSettings->pOutputFile = xVSS_context->pOutputFile;
        xVSS_context->pSettings->uiOutputPathSize = filePathSize;
    }

    /**
    ***/

    /*FB: Add for UTF conversion: copy the pSettings structure into a new pCurrentEditSettings*/
    /*It is the same principle as in the PreviewStart()*/
    pEditSavingSettings =
        (M4VSS3GPP_EditSettings *)M4OSA_32bitAlignedMalloc(sizeof(M4VSS3GPP_EditSettings),
        M4VS, (M4OSA_Char *)"Saving, copy of VSS structure");

    if( pEditSavingSettings == M4OSA_NULL )
    {
        M4OSA_TRACE1_0("Allocation error in M4xVSS_SaveStart");

        if( xVSS_context->pOutputFile != M4OSA_NULL )
        {
            free(xVSS_context->pOutputFile);
            xVSS_context->pOutputFile = M4OSA_NULL;
        }
        return M4ERR_ALLOC;
    }

    /* Copy settings from input structure */
    memcpy((void *) &(pEditSavingSettings->xVSS),
        (void *) &(xVSS_context->pSettings->xVSS),
        sizeof(M4xVSS_EditSettings));

    /* Initialize pEditSavingSettings structure */
    pEditSavingSettings->xVSS.pBGMtrack = M4OSA_NULL;

    pEditSavingSettings->videoFrameRate =
        xVSS_context->pSettings->videoFrameRate;
    pEditSavingSettings->uiClipNumber = xVSS_context->pSettings->uiClipNumber;
    pEditSavingSettings->uiMasterClip =
        xVSS_context->pSettings->uiMasterClip; /* VSS2.0 mandatory parameter */

    /* Allocate savingSettings.pClipList/pTransitions structure */
    pEditSavingSettings->pClipList = (M4VSS3GPP_ClipSettings *
        * )M4OSA_32bitAlignedMalloc(sizeof(M4VSS3GPP_ClipSettings *)
        *pEditSavingSettings->uiClipNumber,
        M4VS, (M4OSA_Char *)"xVSS, saving , copy of pClipList");

    if( pEditSavingSettings->pClipList == M4OSA_NULL )
    {
        M4OSA_TRACE1_0("Allocation error in M4xVSS_SaveStart");

        if( xVSS_context->pOutputFile != M4OSA_NULL )
        {
            free(xVSS_context->pOutputFile);
            xVSS_context->pOutputFile = M4OSA_NULL;
        }
        return M4ERR_ALLOC;
    }

    if( pEditSavingSettings->uiClipNumber > 1 )
    {
        pEditSavingSettings->pTransitionList = (M4VSS3GPP_TransitionSettings *
            * )M4OSA_32bitAlignedMalloc(sizeof(M4VSS3GPP_TransitionSettings *)
            *(pEditSavingSettings->uiClipNumber - 1),
            M4VS, (M4OSA_Char *)"xVSS, saving, copy of pTransitionList");

        if( pEditSavingSettings->pTransitionList == M4OSA_NULL )
        {
            M4OSA_TRACE1_0("Allocation error in M4xVSS_SaveStart");

            if( xVSS_context->pOutputFile != M4OSA_NULL )
            {
                free(xVSS_context->pOutputFile);
                xVSS_context->pOutputFile = M4OSA_NULL;
            }
            return M4ERR_ALLOC;
        }
    }
    else
    {
        pEditSavingSettings->pTransitionList = M4OSA_NULL;
    }

    for ( i = 0; i < pEditSavingSettings->uiClipNumber; i++ )
    {
        pEditSavingSettings->pClipList[i] = (M4VSS3GPP_ClipSettings
            *)M4OSA_32bitAlignedMalloc(sizeof(M4VSS3GPP_ClipSettings),
            M4VS, (M4OSA_Char *)"saving clip settings");

        if( pEditSavingSettings->pClipList[i] == M4OSA_NULL )
        {
            M4OSA_TRACE1_0("Allocation error in M4xVSS_SaveStart");

            if( xVSS_context->pOutputFile != M4OSA_NULL )
            {
                free(xVSS_context->pOutputFile);
                xVSS_context->pOutputFile = M4OSA_NULL;
            }
            return M4ERR_ALLOC;
        }

        if( i < pEditSavingSettings->uiClipNumber
            - 1 ) /* Because there is 1 less transition than clip number */
        {
            pEditSavingSettings->pTransitionList[i] =
                (M4VSS3GPP_TransitionSettings
                *)M4OSA_32bitAlignedMalloc(sizeof(M4VSS3GPP_TransitionSettings),
                M4VS, (M4OSA_Char *)"saving transition settings");

            if( pEditSavingSettings->pTransitionList[i] == M4OSA_NULL )
            {
                M4OSA_TRACE1_0("Allocation error in M4xVSS_SaveStart");

                if( xVSS_context->pOutputFile != M4OSA_NULL )
                {
                    free(xVSS_context->pOutputFile);
                    xVSS_context->pOutputFile = M4OSA_NULL;
                }
                return M4ERR_ALLOC;
            }
        }
    }

    for ( i = 0; i < xVSS_context->pSettings->uiClipNumber; i++ )
    {
        // Add MP4 file support

        if( ( xVSS_context->pSettings->pClipList[i]->FileType
            == M4VIDEOEDITING_kFileType_3GPP)
            || (xVSS_context->pSettings->pClipList[i]->FileType
            == M4VIDEOEDITING_kFileType_MP4)
            || (xVSS_context->pSettings->pClipList[i]->FileType
            == M4VIDEOEDITING_kFileType_M4V)
            || (xVSS_context->pSettings->pClipList[i]->FileType
            == M4VIDEOEDITING_kFileType_ARGB8888))

        {
            /* Copy data from given structure to our saving structure */
            M4xVSS_DuplicateClipSettings(pEditSavingSettings->pClipList[i],
                xVSS_context->pSettings->pClipList[i],
                M4OSA_FALSE /* remove effects */);

            /**
            * UTF conversion: convert into the customer format, before being used*/
            pDecodedPath = pEditSavingSettings->pClipList[i]->pFile;
            length = strlen(pDecodedPath);

            if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct
                != M4OSA_NULL && xVSS_context->
                UTFConversionContext.pTempOutConversionBuffer
                != M4OSA_NULL )
            {
                err =
                    M4xVSS_internalConvertFromUTF8(xVSS_context, (M4OSA_Void
                    *)pEditSavingSettings->pClipList[i]->pFile,
                    (M4OSA_Void *)xVSS_context->
                    UTFConversionContext.pTempOutConversionBuffer,
                    &length);

                if( err != M4NO_ERROR )
                {
                    M4OSA_TRACE1_1(
                        "M4xVSS_SaveStart: M4xVSS_internalConvertFromUTF8 returns err: 0x%x",
                        err);

                    if( xVSS_context->pOutputFile != M4OSA_NULL )
                    {
                        free(xVSS_context->pOutputFile);
                        xVSS_context->pOutputFile = M4OSA_NULL;
                    }
                    return err;
                }
                pDecodedPath = xVSS_context->
                    UTFConversionContext.pTempOutConversionBuffer;

                /**
                * End of the UTF conversion, use the converted file path*/
                free(
                    pEditSavingSettings->pClipList[i]->pFile);
                pEditSavingSettings->pClipList[i]->pFile = (M4OSA_Void
                    *)M4OSA_32bitAlignedMalloc((length + 1),
                    M4VS, (M4OSA_Char *)"saving transition settings");

                if( pEditSavingSettings->pClipList[i]->pFile == M4OSA_NULL )
                {
                    M4OSA_TRACE1_0("Allocation error in M4xVSS_SaveStart");

                    if( xVSS_context->pOutputFile != M4OSA_NULL )
                    {
                        free(xVSS_context->pOutputFile);
                        xVSS_context->pOutputFile = M4OSA_NULL;
                    }
                    return M4ERR_ALLOC;
                }
                memcpy((void *)pEditSavingSettings->pClipList[i]->pFile,
                    (void *)pDecodedPath, length + 1);
            }
            /*FB: add file path size because of UTF 16 conversion*/
            pEditSavingSettings->pClipList[i]->filePathSize = length+1;

            if( i
                < xVSS_context->pSettings->uiClipNumber
                - 1 ) /* Because there is 1 less transition than clip number */
            {
                memcpy(
                    (void *)pEditSavingSettings->pTransitionList[i],
                    (void *)xVSS_context->pSettings->
                    pTransitionList[i],
                    sizeof(M4VSS3GPP_TransitionSettings));
            }
        }
        else
        {
            M4OSA_TRACE1_0(
                "M4xVSS_SaveStart: Error when parsing xVSS_context->pSettings->pClipList[i]:\
                 Bad file type");

            if( xVSS_context->pOutputFile != M4OSA_NULL )
            {
                free(xVSS_context->pOutputFile);
                xVSS_context->pOutputFile = M4OSA_NULL;
            }
            return M4ERR_PARAMETER;
        }
    }

    /* Count the number of video effects, used to know how much memory is needed to allocate*/
    /* FB 2008/10/15: removed : not compatible with M4VSS3GPP_kVideoEffectType_None
    for(j=0;j<xVSS_context->pSettings->nbEffects;j++)
    {
    if(xVSS_context->pSettings->Effects[j].VideoEffectType != M4VSS3GPP_kVideoEffectType_None)
    {
    nbEffects++;
    }
    }*/
    nbEffects = xVSS_context->pSettings->nbEffects;

    /* Allocate effects saving structure with correct number of effects */
    if( nbEffects != 0 )
    {
        pEditSavingSettings->Effects =
            (M4VSS3GPP_EffectSettings *)M4OSA_32bitAlignedMalloc(nbEffects
            * sizeof(M4VSS3GPP_EffectSettings), M4VS, (M4OSA_Char
            *)"Saving settings, effects table of structure settings");

        if( pEditSavingSettings->Effects == M4OSA_NULL )
        {
            M4OSA_TRACE1_0("Allocation error in M4xVSS_SaveStart");

            if( xVSS_context->pOutputFile != M4OSA_NULL )
            {
                free(xVSS_context->pOutputFile);
                xVSS_context->pOutputFile = M4OSA_NULL;
            }
            return M4ERR_ALLOC;
        }

        /* Just copy effect structure to saving structure, as effects time are now */
        /* relative to output clip time*/
        memcpy((void *)pEditSavingSettings->Effects,
            (void *)xVSS_context->pSettings->Effects,
            nbEffects * sizeof(M4VSS3GPP_EffectSettings));
    }
    else
    {
        pEditSavingSettings->Effects = M4OSA_NULL;
        pEditSavingSettings->nbEffects = 0;
    }
    pEditSavingSettings->nbEffects = nbEffects;

    if( pFilePath != M4OSA_NULL )
    {
        pEditSavingSettings->pOutputFile = pFilePath;
    }

    /* Save pointer of saving video editor to use in step function */
    xVSS_context->pCurrentEditSettings = pEditSavingSettings;

    /* Change output file name to temporary output file name, because final file will be
     generated by audio mixer */
    if( xVSS_context->pSettings->xVSS.pBGMtrack != M4OSA_NULL )
    {

        M4OSA_Char out_3gp[M4XVSS_MAX_PATH_LEN];
        M4OSA_Char out_3gp_tmp[M4XVSS_MAX_PATH_LEN];

        /**/
        pEditSavingSettings->xVSS.pBGMtrack =
            (M4xVSS_BGMSettings *)M4OSA_32bitAlignedMalloc(sizeof(M4xVSS_BGMSettings), M4VS,
            (M4OSA_Char
            *)"Saving settings, effects table of structure settings");

        if( pEditSavingSettings->xVSS.pBGMtrack == M4OSA_NULL )
        {
            M4OSA_TRACE1_0("Allocation error in M4xVSS_SaveStart");

            if( xVSS_context->pOutputFile != M4OSA_NULL )
            {
                free(xVSS_context->pOutputFile);
                xVSS_context->pOutputFile = M4OSA_NULL;
            }
            return M4ERR_ALLOC;
        }

        /* Just copy effect structure to saving structure, as effects time are now */
        /* relative to output clip time*/
        memcpy((void *)pEditSavingSettings->xVSS.pBGMtrack,
            (void *)xVSS_context->pSettings->xVSS.pBGMtrack,
            sizeof(M4xVSS_BGMSettings));

        /* Allocate file name, and copy file name buffer to our structure */
        pEditSavingSettings->xVSS.pBGMtrack->pFile = M4OSA_32bitAlignedMalloc(
            (strlen(xVSS_context->pSettings->xVSS.pBGMtrack->pFile)
            + 1),
            M4VS, (M4OSA_Char *)"Saving struct xVSS BGM file path");

        if( pEditSavingSettings->xVSS.pBGMtrack->pFile == M4OSA_NULL )
        {
            M4xVSS_freeCommand(xVSS_context);
            M4OSA_TRACE1_0("Allocation error in M4xVSS_SaveStart");

            if( xVSS_context->pOutputFile != M4OSA_NULL )
            {
                free(xVSS_context->pOutputFile);
                xVSS_context->pOutputFile = M4OSA_NULL;
            }
            return M4ERR_ALLOC;
        }
        memcpy((void *)pEditSavingSettings->xVSS.pBGMtrack->pFile,
            (void *)xVSS_context->pSettings->xVSS.pBGMtrack->pFile,
            strlen(xVSS_context->pSettings->xVSS.pBGMtrack->pFile)
            + 1);

        /*Copy BGM track file path*/

        /**
        * UTF conversion*/
        if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct != M4OSA_NULL
            && xVSS_context->UTFConversionContext.pTempOutConversionBuffer
            != M4OSA_NULL )
        {
            err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                (M4OSA_Void *)pEditSavingSettings->xVSS.pBGMtrack->pFile,
                (M4OSA_Void *)xVSS_context->
                UTFConversionContext.pTempOutConversionBuffer, &length);

            if( err != M4NO_ERROR )
            {
                M4OSA_TRACE1_1(
                    "M4xVSS_SaveStart: M4xVSS_internalConvertFromUTF8 returns err: 0x%x",
                    err);

                if( xVSS_context->pOutputFile != M4OSA_NULL )
                {
                    free(xVSS_context->pOutputFile);
                    xVSS_context->pOutputFile = M4OSA_NULL;
                }
                return err;
            }
            pDecodedPath =
                xVSS_context->UTFConversionContext.pTempOutConversionBuffer;

            free(pEditSavingSettings->xVSS.pBGMtrack->pFile);
            pEditSavingSettings->xVSS.pBGMtrack->pFile =
                (M4OSA_Void *)M4OSA_32bitAlignedMalloc(length + 1, M4VS, (M4OSA_Char
                *)"M4xVSS_SaveStart: Temp filename in case of BGM");

            if( pEditSavingSettings->xVSS.pBGMtrack->pFile == M4OSA_NULL )
            {
                M4OSA_TRACE1_0("Allocation error in M4xVSS_SaveStart");

                if( xVSS_context->pOutputFile != M4OSA_NULL )
                {
                    free(xVSS_context->pOutputFile);
                    xVSS_context->pOutputFile = M4OSA_NULL;
                }
                return M4ERR_ALLOC;
            }
            memcpy((void *)pEditSavingSettings->xVSS.pBGMtrack->pFile,
                (void *)pDecodedPath, length + 1);
        }

        /**/

        M4OSA_chrNCopy(out_3gp, xVSS_context->pTempPath, M4XVSS_MAX_PATH_LEN - 1);
        M4OSA_chrNCopy(out_3gp_tmp, xVSS_context->pTempPath, M4XVSS_MAX_PATH_LEN - 1);

        /* Construct output temporary 3GP filename */
        strncat((char *)out_3gp, (const char *)"savetemp.3gp\0", 13);
        strncat((char *)out_3gp_tmp, (const char *)"savetemp.tmp\0", 13);

        /**
        * UTF conversion: convert into the customer format, before being used*/
        pDecodedPath = out_3gp;
        length = strlen(pDecodedPath);

        if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct != M4OSA_NULL
            && xVSS_context->UTFConversionContext.pTempOutConversionBuffer
            != M4OSA_NULL )
        {
            err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                (M4OSA_Void *)out_3gp, (M4OSA_Void *)xVSS_context->
                UTFConversionContext.pTempOutConversionBuffer, &length);

            if( err != M4NO_ERROR )
            {
                M4OSA_TRACE1_1(
                    "M4xVSS_SaveStart: M4xVSS_internalConvertFromUTF8 returns err: 0x%x",
                    err);

                if( xVSS_context->pOutputFile != M4OSA_NULL )
                {
                    free(xVSS_context->pOutputFile);
                    xVSS_context->pOutputFile = M4OSA_NULL;
                }
                return err;
            }
            pDecodedPath =
                xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
        }

        /**
        * End of the UTF conversion, use the converted file path*/
        xVSS_context->pCurrentEditSettings->pOutputFile =
            (M4OSA_Void *)M4OSA_32bitAlignedMalloc(length + 1, M4VS,
            (M4OSA_Char *)"M4xVSS_SaveStart: Temp filename in case of BGM");

        if( xVSS_context->pCurrentEditSettings->pOutputFile == M4OSA_NULL )
        {
            M4OSA_TRACE1_0("Allocation error in M4xVSS_SaveStart");

            if( xVSS_context->pOutputFile != M4OSA_NULL )
            {
                free(xVSS_context->pOutputFile);
                xVSS_context->pOutputFile = M4OSA_NULL;
            }
            return M4ERR_ALLOC;
        }
        memcpy((void *)xVSS_context->pCurrentEditSettings->pOutputFile,
            (void *)pDecodedPath, length + 1);
        xVSS_context->pCurrentEditSettings->uiOutputPathSize = length + 1;

        /**
        * UTF conversion: convert into the customer format, before being used*/
        pDecodedPath = out_3gp_tmp;
        length = strlen(pDecodedPath);

        if( xVSS_context->UTFConversionContext.pConvFromUTF8Fct != M4OSA_NULL
            && xVSS_context->UTFConversionContext.pTempOutConversionBuffer
            != M4OSA_NULL )
        {
            err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                (M4OSA_Void *)out_3gp_tmp, (M4OSA_Void *)xVSS_context->
                UTFConversionContext.pTempOutConversionBuffer, &length);

            if( err != M4NO_ERROR )
            {
                M4OSA_TRACE1_1(
                    "M4xVSS_SaveStart: M4xVSS_internalConvertFromUTF8 returns err: 0x%x",
                    err);

                if( xVSS_context->pOutputFile != M4OSA_NULL )
                {
                    free(xVSS_context->pOutputFile);
                    xVSS_context->pOutputFile = M4OSA_NULL;
                }
                return err;
            }
            pDecodedPath =
                xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
        }

        /**
        * End of the UTF conversion, use the converted file path*/
        xVSS_context->pCurrentEditSettings->pTemporaryFile =
            (M4OSA_Void *)M4OSA_32bitAlignedMalloc(length + 1, M4VS,
            (M4OSA_Char *)"M4xVSS_SaveStart: Temporary file");

        if( xVSS_context->pCurrentEditSettings->pTemporaryFile == M4OSA_NULL )
        {
            M4OSA_TRACE1_0("Allocation error in M4xVSS_SaveStart");

            if( xVSS_context->pOutputFile != M4OSA_NULL )
            {
                free(xVSS_context->pOutputFile);
                xVSS_context->pOutputFile = M4OSA_NULL;
            }
            return M4ERR_ALLOC;
        }
        memcpy((void *)xVSS_context->pCurrentEditSettings->pTemporaryFile,
            (void *)pDecodedPath, length + 1);

        /* Put nb of step for progression monitoring to 2, because audio mixing is needed */
        xVSS_context->nbStepTotal = 2;
    }
    else
    {
        xVSS_context->pCurrentEditSettings->pOutputFile =
            xVSS_context->pOutputFile;
        xVSS_context->pCurrentEditSettings->pTemporaryFile = M4OSA_NULL;

        /* Put nb of step for progression monitoring to 1, because no audio mixing is needed */
        xVSS_context->nbStepTotal = 1;
    }

    /**
    ***/

    err = M4xVSS_internalGenerateEditedFile(xVSS_context);

    if( err != M4NO_ERROR )
    {
        M4OSA_TRACE1_1(
            "M4xVSS_SaveStart: M4xVSS_internalGenerateEditedFile returned an error: 0x%x",
            err);

        /**/
        if( xVSS_context->pCurrentEditSettings->pOutputFile != M4OSA_NULL
            && xVSS_context->pSettings->xVSS.pBGMtrack == M4OSA_NULL )
        {
            free(xVSS_context->pCurrentEditSettings->
                pOutputFile);
            xVSS_context->pCurrentEditSettings->pOutputFile = M4OSA_NULL;
            xVSS_context->pOutputFile = M4OSA_NULL;
        }

        if( xVSS_context->pCurrentEditSettings->pTemporaryFile != M4OSA_NULL
            && xVSS_context->pSettings->xVSS.pBGMtrack != M4OSA_NULL )
        {
            free(xVSS_context->pCurrentEditSettings->
                pTemporaryFile);
            xVSS_context->pCurrentEditSettings->pTemporaryFile = M4OSA_NULL;
        }

        if( xVSS_context->pOutputFile != M4OSA_NULL )
        {
            free(xVSS_context->pOutputFile);
            xVSS_context->pOutputFile = M4OSA_NULL;
        }
        /* TODO: Translate error code of VSS to an xVSS error code */
        return err;
    }

    /* Reinitialize current step number for progression monitoring */
    xVSS_context->currentStep = 0;

    /* Change xVSS state */
    xVSS_context->m_state = M4xVSS_kStateSaving;

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_SaveStop(M4OSA_Context pContext)
 * @brief        This function unallocate save ressources and change xVSS
 *                internal state.
 * @note        This function must be called once M4xVSS_Step has returned
 *                M4VSS3GPP_WAR_SAVING_DONE
 *
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_SaveStop( M4OSA_Context pContext )
{
    M4xVSS_Context *xVSS_context = (M4xVSS_Context *)pContext;
    M4OSA_ERR err = M4NO_ERROR;

    /* Check state */
    if( xVSS_context->m_state != M4xVSS_kStateSaving )
    {
        M4OSA_TRACE1_1(
            "Bad state when calling M4xVSS_SaveStop function! State is %d",
            xVSS_context->m_state);
        return M4ERR_STATE;
    }

    /* Free saving structures */
    M4xVSS_internalFreeSaving(xVSS_context);

    if( xVSS_context->pOutputFile != M4OSA_NULL )
    {
        free(xVSS_context->pOutputFile);
        xVSS_context->pOutputFile = M4OSA_NULL;
    }

    /* Change xVSS state */
    xVSS_context->m_state = M4xVSS_kStateSaved;

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_Step(M4OSA_Context pContext, M4OSA_UInt8 *pProgress)
 * @brief        This function executes differents tasks, depending of xVSS
 *                internal state.
 * @note        This function:
 *                    - analyses editing structure if called after M4xVSS_SendCommand
 *                    - generates preview file if called after M4xVSS_PreviewStart
 *                    - generates final edited file if called after M4xVSS_SaveStart
 *
 * @param    pContext                        (IN) Pointer on the xVSS edit context
 * @param    pProgress                        (IN/OUT) Pointer on an integer giving a
 *                                            progress indication (between 0-100)
 * @return    M4NO_ERROR:                        No error, the user must call M4xVSS_Step again
 * @return    M4ERR_PARAMETER:                At least one parameter is M4OSA_NULL
 * @return    M4ERR_STATE:                    This function cannot not be called at this time
 * @return    M4VSS3GPP_WAR_PREVIEW_READY:    Preview file is generated
 * @return    M4VSS3GPP_WAR_SAVING_DONE:        Final edited file is generated
 * @return    M4VSS3GPP_WAR_ANALYZING_DONE:    Analyse is done
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_Step( M4OSA_Context pContext, M4OSA_UInt8 *pProgress )
{
    M4xVSS_Context *xVSS_context = (M4xVSS_Context *)pContext;
    M4VSS3GPP_EditContext pVssCtxt = xVSS_context->pCurrentEditContext;
    M4VSS3GPP_AudioMixingContext pAudioMixingCtxt =
        xVSS_context->pAudioMixContext;
    M4OSA_ERR err = M4NO_ERROR;
    M4OSA_UInt8 uiProgress = 0;

    switch( xVSS_context->m_state )
    {
        case M4xVSS_kStateSaving:
        //case M4xVSS_kStateGeneratingPreview:
            {
                if( xVSS_context->editingStep
                    == M4xVSS_kMicroStateEditing ) /* VSS -> creating effects, transitions ... */
                {
                    /* RC: to delete unecessary temp files on the fly */
                    M4VSS3GPP_InternalEditContext *pVSSContext =
                        (M4VSS3GPP_InternalEditContext *)pVssCtxt;

                    err = M4VSS3GPP_editStep(pVssCtxt, &uiProgress);

                    if( ( err != M4NO_ERROR) && (err != M4VSS3GPP_WAR_EDITING_DONE)
                        && (err != M4VSS3GPP_WAR_SWITCH_CLIP) )
                    {
                        M4OSA_TRACE1_1(
                            "M4xVSS_Step: M4VSS3GPP_editStep returned 0x%x\n", err);
                        M4VSS3GPP_editCleanUp(pVssCtxt);
                        /* TODO ? : Translate error code of VSS to an xVSS error code ? */
                        xVSS_context->pCurrentEditContext = M4OSA_NULL;
                        return err;
                    }

                    /* RC: to delete unecessary temp files on the fly */
                    if( err == M4VSS3GPP_WAR_SWITCH_CLIP )
                    {
#ifndef DO_NOT_REMOVE_TEMP_FILES
                        /* It means we can delete the temporary file */
                        /* First step, check the temp file is not use somewhere else after */

                        M4OSA_UInt32 i;
                        M4OSA_Int32 cmpResult = -1;

                        for ( i = pVSSContext->uiCurrentClip;
                            i < pVSSContext->uiClipNumber; i++ )
                        {
                            if( pVSSContext->pClipList[pVSSContext->uiCurrentClip
                                - 1].filePathSize
                                == pVSSContext->pClipList[i].filePathSize )
                            {
                                cmpResult = memcmp((void *)pVSSContext->
                                    pClipList[pVSSContext->uiCurrentClip
                                    - 1].pFile, (void *)pVSSContext->pClipList[i].pFile,
                                    pVSSContext->
                                    pClipList[pVSSContext->uiCurrentClip
                                    - 1].filePathSize);

                                if( cmpResult == 0 )
                                {
                                    /* It means we found a corresponding file, we do not delete
                                    this temporary file */
                                    break;
                                }
                            }
                        }

                        if( cmpResult != 0 )
                        {
                            M4OSA_UInt32 ConvertedSize = 0;
                            M4OSA_Char *toto;
                            M4OSA_Char *pTmpStr;

                            /* Convert result in UTF8 to check if we can delete it or not */
                            if( xVSS_context->UTFConversionContext.pConvToUTF8Fct
                                != M4OSA_NULL && xVSS_context->
                                UTFConversionContext.
                                pTempOutConversionBuffer != M4OSA_NULL )
                            {
                                M4xVSS_internalConvertToUTF8(xVSS_context,
                                    (M4OSA_Void *)pVSSContext->
                                    pClipList[pVSSContext->uiCurrentClip
                                    - 1].pFile, (M4OSA_Void *)xVSS_context->
                                    UTFConversionContext.
                                    pTempOutConversionBuffer, &ConvertedSize);
                                toto = (M4OSA_Char *)strstr((const char *)xVSS_context->
                                    UTFConversionContext.
                                    pTempOutConversionBuffer,
                                    (const char *)xVSS_context->pTempPath);
                                pTmpStr =
                                    xVSS_context->UTFConversionContext.
                                    pTempOutConversionBuffer;
                            }
                            else
                            {
                                toto = (M4OSA_Char *)strstr((const char *)pVSSContext->
                                    pClipList[pVSSContext->uiCurrentClip
                                    - 1].pFile, (const char *)xVSS_context->pTempPath);
                                pTmpStr = pVSSContext->
                                    pClipList[pVSSContext->uiCurrentClip
                                    - 1].pFile;
                            }

                            if( toto != M4OSA_NULL )
                            {
                                /* As temporary files can be imgXXX.3gp or vidXXX.3gp */
                                pTmpStr +=
                                    (strlen((const char *)pTmpStr)
                                    - 10); /* Because temporary files have a length at most of
                                    10 bytes */
                                toto = (M4OSA_Char *)strstr((const char *)pTmpStr,
                                    (const char *)"img");

                                if( toto != M4OSA_NULL )
                                {
                                    toto = (M4OSA_Char *)strstr((const char *)pTmpStr,
                                        (const char *)"vid");
                                }

                                if( err
                                    == M4NO_ERROR ) /* It means the file is a temporary file, we
                                    can delete it */
                                {
                                    remove((const char *)pVSSContext->
                                        pClipList[pVSSContext->uiCurrentClip
                                        - 1].pFile);
                                }
                            }
                        }

#endif /* DO_NOT_REMOVE_TEMP_FILES*/
                        /* */

                        err = M4NO_ERROR;
                    }

                    if( err == M4VSS3GPP_WAR_EDITING_DONE )
                    {
                        xVSS_context->currentStep++;
                        /* P4ME00003276: When a step is complete, increment currentStep and reset
                        uiProgress unless progress would be wrong */
                        uiProgress = 0;
                        err = M4xVSS_internalCloseEditedFile(xVSS_context);
                        /* Fix for  blrnxpsw#234---> */
                        if( err != M4NO_ERROR )
                        {
                            if( err == ((M4OSA_UInt32)M4ERR_FILE_INVALID_POSITION) )
                            {
                                err = M4xVSSERR_NO_MORE_SPACE;
                            }
                            M4OSA_TRACE1_1(
                                "M4xVSS_internalCloseEditedFile returned an error: 0x%x",
                                err);
                            return err;
                        }
                        /*<---- Fix for  blrnxpsw#234 */
                        if( xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack
                            != M4OSA_NULL )
                        {
                            xVSS_context->editingStep =
                                M4xVSS_kMicroStateAudioMixing;
                            /* Open Audio mixing component */
                            err = M4xVSS_internalGenerateAudioMixFile(xVSS_context);

                            if( err != M4NO_ERROR )
                            {
                                M4OSA_TRACE1_1(
                                    "M4xVSS_internalGenerateAudioMixFile returned an error: 0x%x",
                                    err);
                                /* TODO ? : Translate error code of VSS to an xVSS error code */
                                return err;
                            }
                            err = M4NO_ERROR;
                            goto end_step;
                        }
                        else
                        {

                            err = M4VSS3GPP_WAR_SAVING_DONE;
                            goto end_step;

                        }
                    }
                }
                else if( xVSS_context->editingStep
                    == M4xVSS_kMicroStateAudioMixing ) /* Audio mixing: mix/replace audio track
                    with given BGM */
                {
                    err = M4VSS3GPP_audioMixingStep(pAudioMixingCtxt, &uiProgress);

                    if( ( err != M4NO_ERROR)
                        && (err != M4VSS3GPP_WAR_END_OF_AUDIO_MIXING) )
                    {
                        M4OSA_TRACE1_1(
                            "M4VSS3GPP_audioMixingMain: M4VSS3GPP_audioMixingStep returned 0x%x\n",
                            err);
                        /* TODO ? : Translate error code of VSS to an xVSS error code */
                        return err;
                    }

                    if( err == M4VSS3GPP_WAR_END_OF_AUDIO_MIXING )
                    {
                        xVSS_context->currentStep++;
                        /* P4ME00003276: When a step is complete, increment currentStep and reset
                        uiProgress unless progress would be wrong */
                        uiProgress = 0;
                        err = M4xVSS_internalCloseAudioMixedFile(xVSS_context);

                        if( err != M4NO_ERROR )
                        {
                            M4OSA_TRACE1_1(
                                "M4xVSS_internalCloseAudioMixedFile returned an error: 0x%x",
                                err);
                            /* TODO ? : Translate error code of VSS to an xVSS error code */
                            return err;
                        }

                            err = M4VSS3GPP_WAR_SAVING_DONE;
                            goto end_step;

                    }
                }
                else
                {
                    M4OSA_TRACE1_0("Bad state in step function !");
                    return M4ERR_STATE;
                }
            }
            break;

        case M4xVSS_kStateAnalyzing:
            {
                if( xVSS_context->analyseStep
                    == M4xVSS_kMicroStateAnalysePto3GPP ) /* Pto3GPP, analysing input parameters */
                {
                    if( xVSS_context->pPTo3GPPcurrentParams == M4OSA_NULL
                        && xVSS_context->pPTo3GPPparamsList != M4OSA_NULL )
                    {
                        xVSS_context->pPTo3GPPcurrentParams =
                            xVSS_context->
                            pPTo3GPPparamsList; /* Current Pto3GPP Parameter is the first element
                            of the list */
                    }
                    else if( xVSS_context->pPTo3GPPcurrentParams != M4OSA_NULL
                        && xVSS_context->pPTo3GPPparamsList != M4OSA_NULL )
                    {
                        xVSS_context->pPTo3GPPcurrentParams =
                            xVSS_context->pPTo3GPPcurrentParams->
                            pNext; /* Current Pto3GPP Parameter is the next element of the list */

                        if( xVSS_context->pPTo3GPPcurrentParams
                            == M4OSA_NULL ) /* It means there is no next image to convert */
                        {
                            /* We step to MCS phase */
                            xVSS_context->analyseStep =
                                M4xVSS_kMicroStateAnalyzeMCS;
                            err = M4NO_ERROR;
                            goto end_step;
                        }
                    }
                    else if( xVSS_context->pPTo3GPPparamsList == M4OSA_NULL )
                    {
                        xVSS_context->analyseStep =
                            M4xVSS_kMicroStateAnalyzeMCS; /* Change Analyzing micro state to
                             MCS phase */
                        err = M4NO_ERROR;
                        goto end_step;
                    }

                    /* Check if this file has to be converted or not */
                    /* If not, we just return M4NO_ERROR, and go to next file */
                    if( xVSS_context->pPTo3GPPcurrentParams->isCreated
                        == M4OSA_FALSE )
                    {
                        /* Opening Pto3GPP */
                        err = M4xVSS_internalStartConvertPictureTo3gp(xVSS_context);

                        if( err != M4NO_ERROR )
                        {
                            M4OSA_TRACE1_1("M4xVSS_Step: M4xVSS_internalStartConvertPictureTo3gp \
                            returned error: 0x%x",
                                err)
                                /* TODO ? : Translate error code of VSS to an xVSS error code */
                                return err;
                        }
                        xVSS_context->analyseStep =
                            M4xVSS_kMicroStateConvertPto3GPP;
                    }
                }
                else if( xVSS_context->analyseStep
                    == M4xVSS_kMicroStateConvertPto3GPP ) /* Pto3GPP, converting */
                {
                    err = M4PTO3GPP_Step(xVSS_context->pM4PTO3GPP_Ctxt);
                    /* update progress bar */
                    if(xVSS_context->pCallBackCtxt->m_NbImage > 1)
                    {
                        uiProgress = (xVSS_context->pCallBackCtxt->m_ImageCounter * 100) / (xVSS_context->pCallBackCtxt->m_NbImage -1);
                    }

                    if( ( err != M4NO_ERROR) && (err
                        != ((M4OSA_UInt32)M4PTO3GPP_WAR_END_OF_PROCESSING)) )
                    {
                        /* TO BE CHECKED NO LEAKS  !!!!! */
                        M4OSA_TRACE1_1(
                            "M4xVSS_Step: M4PTO3GPP_Step returned 0x%x\n", err);
                        /* TODO ? : Translate error code of VSS to an xVSS error code */
                        return err;
                    }
                    else if( err
                        == ((M4OSA_UInt32)M4PTO3GPP_WAR_END_OF_PROCESSING) )
                    {
                        xVSS_context->currentStep++;
                        /* P4ME00003276: When a step is complete, increment currentStep and reset
                         uiProgress unless progress would be wrong */
                        uiProgress = 0;
                        xVSS_context->analyseStep =
                            M4xVSS_kMicroStateAnalysePto3GPP; /* We go back to analyze parameters
                            to see if there is a next file to convert */
                        /* RC !!!!!!!! */
                        xVSS_context->pPTo3GPPcurrentParams->isCreated =
                            M4OSA_TRUE; /* To avoid reconverting it if another SendCommand is
                            called */
                        err = M4xVSS_internalStopConvertPictureTo3gp(xVSS_context);
                        /*SS:blrnxpsw#  234 */
                        if( err == ((M4OSA_UInt32)M4ERR_FILE_INVALID_POSITION) )
                        {
                            err = M4xVSSERR_NO_MORE_SPACE;
                        }

                        if( err != M4NO_ERROR )
                        {
                            M4OSA_TRACE1_1("M4xVSS_Step:\
                                           M4xVSS_internalStopConvertPictureTo3gp returned 0x%x",
                                            err);
                            /* TODO ? : Translate error code of VSS to an xVSS error code */
                            return err;
                        }
                    }
                }
                else if( xVSS_context->analyseStep
                    ==
                    M4xVSS_kMicroStateAnalyzeMCS ) /* MCS: analyzing input parameters */
                {
                    if( xVSS_context->pMCScurrentParams == M4OSA_NULL \
                        && xVSS_context->pMCSparamsList != M4OSA_NULL )
                    {
                        xVSS_context->pMCScurrentParams = xVSS_context->
                            pMCSparamsList; /* Current MCS Parameter is the first
                                            element of the list */
                    }
                    else if( xVSS_context->pMCScurrentParams != M4OSA_NULL \
                        && xVSS_context->pMCSparamsList != M4OSA_NULL )
                    {
                        xVSS_context->pMCScurrentParams =
                            xVSS_context->pMCScurrentParams->
                            pNext; /* Current MCS Parameter
                                   is the next element of the list */

                        if( xVSS_context->pMCScurrentParams == M4OSA_NULL )
                            /* It means there is no next image to convert */
                        {
                            xVSS_context->analyseStep =
                                M4xVSS_kMicroStateAnalysePto3GPP; /* Reinit Analyzing micro state */
                            xVSS_context->m_state =
                                M4xVSS_kStateOpened; /* Change xVSS state */
                            err = M4VSS3GPP_WAR_ANALYZING_DONE;
                            goto end_step; /* End of Analysis */
                        }
                    }
                    else if( xVSS_context->pMCSparamsList == M4OSA_NULL )
                    {
                        xVSS_context->analyseStep =
                            M4xVSS_kMicroStateAnalysePto3GPP; /* Reinit Analyzing micro state */
                        xVSS_context->m_state =
                            M4xVSS_kStateOpened; /* Change xVSS state */
                        err = M4VSS3GPP_WAR_ANALYZING_DONE;
                        goto end_step;                        /* End of Analysis */
                    }

                    /* Check if this file has to be transcoded or not */
                    /* If not, we just return M4NO_ERROR, and go to next file */
                    if( xVSS_context->pMCScurrentParams->isCreated == M4OSA_FALSE )
                    {
                        /* Opening MCS */
                        M4OSA_UInt32 rotationDegree = 0;
                        err = M4xVSS_internalStartTranscoding(xVSS_context, &rotationDegree);

                        if( err != M4NO_ERROR )
                        {
                            M4OSA_TRACE1_1("M4xVSS_Step: M4xVSS_internalStartTranscoding returned\
                                 error: 0x%x", err);
                            return err;
                        }
                        int32_t index = xVSS_context->pMCScurrentParams->videoclipnumber;

                        /* The cuts are done in the MCS, so we need to replace
                           the beginCutTime and endCutTime to keep the entire video*/
                        xVSS_context->pSettings->pClipList[index]->uiBeginCutTime = 0;
                        xVSS_context->pSettings->pClipList[index]->uiEndCutTime = 0;


                        M4OSA_TRACE1_1("M4xVSS_Step: \
                            M4xVSS_internalStartTranscoding returned \
                                success; MCS context: 0x%x",
                                 xVSS_context->pMCS_Ctxt);
                        xVSS_context->analyseStep =
                            M4xVSS_kMicroStateTranscodeMCS;

                        // Retain rotation info of trimmed / transcoded file
                        xVSS_context->pSettings->pClipList[index]->\
                            ClipProperties.videoRotationDegrees = rotationDegree;
                    }
                }
                else if( xVSS_context->analyseStep
                    == M4xVSS_kMicroStateTranscodeMCS )
                    /* MCS: transcoding file */
                {
                    err = M4MCS_step(xVSS_context->pMCS_Ctxt, &uiProgress);
                    /*SS:blrnxpsw#  234 */
                    if( err == ((M4OSA_UInt32)M4MCS_ERR_NOMORE_SPACE) )
                    {
                        err = M4xVSSERR_NO_MORE_SPACE;
                    }

                    if( ( err != M4NO_ERROR)
                        && (err != M4MCS_WAR_TRANSCODING_DONE) )
                    {
                        /* TO BE CHECKED NO LEAKS  !!!!! */
                        M4OSA_TRACE1_1("M4xVSS_Step: M4MCS_step returned 0x%x\n",
                            err);
                        /* TODO ? : Translate error code of MCS to an xVSS error code ? */
                        return err;
                    }
                    else if( err == M4MCS_WAR_TRANSCODING_DONE )
                    {
                        xVSS_context->currentStep++;
                        /* P4ME00003276: When a step is complete, increment currentStep and reset
                        uiProgress unless progress would be wrong */
                        uiProgress = 0;
                        xVSS_context->analyseStep =
                            M4xVSS_kMicroStateAnalyzeMCS; /* We go back to
                                                          analyze parameters to see if there is
                                                           a next file to transcode */
                        /* RC !!!!!!!!!*/
                        xVSS_context->pMCScurrentParams->isCreated =
                            M4OSA_TRUE; /* To avoid
                                        reconverting it if another SendCommand is called */
                        err = M4xVSS_internalStopTranscoding(xVSS_context);

                        if( err != M4NO_ERROR )
                        {
                            M4OSA_TRACE1_1("M4xVSS_Step:\
                                           M4xVSS_internalStopTranscoding returned 0x%x", err);
                            /* TODO ? : Translate error code of MCS to an xVSS error code ? */
                            return err;
                        }
                    }
                }
                else
                {
                    M4OSA_TRACE1_0("Bad micro state in analyzing state")
                        return M4ERR_STATE;
                }
            }
            break;

        default:
            M4OSA_TRACE1_1(
                "Bad state when calling M4xVSS_Step function! State is %d",
                xVSS_context->m_state);
            return M4ERR_STATE;
    }

end_step:
    /* Compute progression */
    if( xVSS_context->nbStepTotal != 0 )
    {
        *pProgress = (M4OSA_UInt8)(( ( xVSS_context->currentStep * 100) \
            / (xVSS_context->nbStepTotal))
            + (uiProgress / (xVSS_context->nbStepTotal)));

        if( *pProgress > 100 )
        {
            *pProgress = 100;
        }
    }
    else
    {
        *pProgress = 100;
    }

    return err;
}

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_CloseCommand(M4OSA_Context pContext)
 * @brief        This function deletes current editing profile, unallocate
 *                ressources and change xVSS internal state.
 * @note        After this function, the user can call a new M4xVSS_SendCommand
 *
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_CloseCommand( M4OSA_Context pContext )
{
    M4xVSS_Context *xVSS_context = (M4xVSS_Context *)pContext;
    M4OSA_ERR err = M4NO_ERROR;

    /* Check state */
    /* Depending of the state, differents things have to be done */
    switch( xVSS_context->m_state )
    {
        case M4xVSS_kStateOpened:
            /* Nothing to do here */
            err = M4xVSS_internalFreeSaving(xVSS_context);
            break;

        case M4xVSS_kStateSaving:
            {
                if( xVSS_context->editingStep == M4xVSS_kMicroStateEditing )
                {
                    err = M4xVSS_internalCloseEditedFile(xVSS_context);

                    if( err != M4NO_ERROR )
                    {
                        /* Fix for blrnxpsw#234---->*/
                        if( err == ((M4OSA_UInt32)M4ERR_FILE_INVALID_POSITION) )
                        {
                            err = M4xVSSERR_NO_MORE_SPACE;
                        }
                        M4OSA_TRACE1_1("M4xVSS_CloseCommand:\
                                       M4xVSS_internalCloseEditedFile returned an error: 0x%x",
                                        err);
                        /* we are retaining error here and returning error  in the end of the
                        function  as to aviod memory leak*/
                        //return err;
                    }
                }
                else if( xVSS_context->editingStep
                    == M4xVSS_kMicroStateAudioMixing )
                {
                    err = M4xVSS_internalCloseAudioMixedFile(xVSS_context);

                    if( err != M4NO_ERROR )
                    {
                        /* Fix for blrnxpsw#234---->*/
                        if( err == ((M4OSA_UInt32)M4ERR_FILE_INVALID_POSITION) )
                        {
                            err = M4xVSSERR_NO_MORE_SPACE;
                        }
                        M4OSA_TRACE1_1("M4xVSS_CloseCommand: \
                                M4xVSS_internalCloseAudioMixedFile returned an error: 0x%x", err);
                        /* we are retaining error here and returning error  in the end of
                        the function  as to aviod memory leak*/
                        //return err;
                        /* <----Fix for blrnxpsw#234*/
                    }
                }
                err = M4xVSS_internalFreeSaving(xVSS_context);
                /* We free this pointer only if a BGM track is present, because in that case,
                this pointer owns to us */
                if( xVSS_context->pSettings->xVSS.pBGMtrack != M4OSA_NULL ) {
                    /*if(M4OSA_NULL != xVSS_context->pSettings->pOutputFile)
                    {
                    free(xVSS_context->pSettings->pOutputFile);
                    xVSS_context->pSettings->pOutputFile = M4OSA_NULL;
                    }*/
                    /*if(M4OSA_NULL != xVSS_context->pSettings->pTemporaryFile)
                    {
                    free(xVSS_context->pSettings->pTemporaryFile);
                    xVSS_context->pSettings->pTemporaryFile = M4OSA_NULL;
                    }*/
                }
            }
            break;

        case M4xVSS_kStateSaved:
            break;

        case M4xVSS_kStateAnalyzing:
            {
                if( xVSS_context->analyseStep == M4xVSS_kMicroStateConvertPto3GPP )
                {
                    /* Free Pto3GPP module */
                    err = M4xVSS_internalStopConvertPictureTo3gp(xVSS_context);
                    /* Fix for blrnxpsw#234---->*/
                    if( err != M4NO_ERROR )
                    {
                        if( err == ((M4OSA_UInt32)M4ERR_FILE_INVALID_POSITION) )
                        {
                            err = M4xVSSERR_NO_MORE_SPACE;
                        }
                        M4OSA_TRACE1_1("M4xVSS_Step: \
                                       M4xVSS_internalStopConvertPictureTo3gp returned 0x%x", err);
                        /* we are retaining error here and returning error  in the end of the
                        function  as to aviod memory leak*/
                        //return err;
                    }
                    /* <-----Fix for blrnxpsw#234>*/
                }
                else if( xVSS_context->analyseStep
                    == M4xVSS_kMicroStateTranscodeMCS )
                {
                    /* Free MCS module */
                    err = M4MCS_abort(xVSS_context->pMCS_Ctxt);
                    /* Fix for blrnxpsw#234---->*/
                    if( err != M4NO_ERROR )
                    {
                        if( err == ((M4OSA_UInt32)M4ERR_FILE_INVALID_POSITION) )
                        {
                            err = M4xVSSERR_NO_MORE_SPACE;
                        }
                        M4OSA_TRACE1_1("M4xVSS_Step: M4MCS_abort returned 0x%x",
                            err);
                        /* we are retaining error here and returning error  in the end of the
                        function  as to aviod memory leak*/
                        //return err;
                    }
                    /* <---Fix for blrnxpsw#234*/
                }
            }
            break;

        default:
            M4OSA_TRACE1_1(
                "Bad state when calling M4xVSS_CloseCommand function! State is %d",
                xVSS_context->m_state);
            return M4ERR_STATE;
    }

    /* Free Send command */
    M4xVSS_freeCommand(xVSS_context);

    xVSS_context->m_state = M4xVSS_kStateInitialized; /* Change xVSS state */

    return err;
}

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_CleanUp(M4OSA_Context pContext)
 * @brief        This function deletes all xVSS ressources
 * @note        This function must be called after M4xVSS_CloseCommand.
 *
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_CleanUp( M4OSA_Context pContext )
{
    M4xVSS_Context *xVSS_context = (M4xVSS_Context *)pContext;
    M4OSA_TRACE3_0("M4xVSS_CleanUp:entering");

    /* Check state */
    if( xVSS_context->m_state != M4xVSS_kStateInitialized )
    {
        M4OSA_TRACE1_1(\
            "Bad state when calling M4xVSS_CleanUp function! State is %d",\
            xVSS_context->m_state);
        return M4ERR_STATE;
    }

    /**
    * UTF conversion: free temporary buffer*/
    if( xVSS_context->UTFConversionContext.pTempOutConversionBuffer
        != M4OSA_NULL )
    {
        free(xVSS_context->
            UTFConversionContext.pTempOutConversionBuffer);
        xVSS_context->UTFConversionContext.pTempOutConversionBuffer =
            M4OSA_NULL;
    }

    free(xVSS_context->pTempPath);
    xVSS_context->pTempPath = M4OSA_NULL;

    free(xVSS_context->pSettings);
    xVSS_context->pSettings = M4OSA_NULL;

    free(xVSS_context);
    xVSS_context = M4OSA_NULL;
    M4OSA_TRACE3_0("M4xVSS_CleanUp:leaving ");

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * prototype    M4xVSS_GetVersion(M4_VersionInfo *pVersion)
 * @brief        This function get the version of the Video Studio 2.1
 *
 * @param    pVersion            (IN) Pointer on the version info struct
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_GetVersion( M4_VersionInfo *pVersion )
{
    /* Just used for a grep in code */
    /* CHANGE_VERSION_HERE */
    static const M4OSA_Char cVersion[26] = "NXPSW_VideoStudio21_1_3_0";

    if( M4OSA_NULL == pVersion )
    {
        return M4ERR_PARAMETER;
    }

    pVersion->m_major = M4_xVSS_MAJOR;
    pVersion->m_minor = M4_xVSS_MINOR;
    pVersion->m_revision = M4_xVSS_REVISION;
    pVersion->m_structSize = sizeof(M4_VersionInfo);

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4xVSS_CreateClipSettings()
 * @brief    Allows filling a clip settings structure with default values
 *
 * @note    WARNING: pClipSettings->Effects[ ] will be allocated in this function.
 *                   pClipSettings->pFile      will be allocated in this function.
 *
 * @param    pClipSettings        (IN) Pointer to a valid M4VSS3GPP_ClipSettings structure
 * @param   pFile               (IN) Clip file name
 * @param   filePathSize        (IN) Size of the clip path (needed for the UTF16 conversion)
 * @param    nbEffects           (IN) Nb of effect settings to allocate
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pClipSettings is M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_CreateClipSettings( M4VSS3GPP_ClipSettings *pClipSettings,
                                    M4OSA_Void *pFile, M4OSA_UInt32 filePathSize,
                                     M4OSA_UInt8 nbEffects )
{
    M4OSA_ERR err = M4NO_ERROR;

    M4OSA_TRACE3_1("M4xVSS_CreateClipSettings called with pClipSettings=0x%p",
        pClipSettings);

    /**
    *    Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pClipSettings), M4ERR_PARAMETER,
        "M4xVSS_CreateClipSettings: pClipSettings is NULL");

    /* Create inherited VSS3GPP stuff */
    /*err = M4VSS3GPP_editCreateClipSettings(pClipSettings, pFile,nbEffects);*/
    /*FB: add clip path size (needed for UTF 16 conversion)*/
    err = M4VSS3GPP_editCreateClipSettings(pClipSettings, pFile, filePathSize,
        nbEffects);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1("M4xVSS_CreateClipSettings :\
                       ERROR in M4VSS3GPP_editCreateClipSettings = 0x%x", err);
        return err;
    }

    /* Set the clip settings to default */
    pClipSettings->xVSS.uiBeginCutPercent = 0;
    pClipSettings->xVSS.uiEndCutPercent = 0;
    pClipSettings->xVSS.uiDuration = 0;
    pClipSettings->xVSS.isPanZoom = M4OSA_FALSE;
    pClipSettings->xVSS.PanZoomTopleftXa = 0;
    pClipSettings->xVSS.PanZoomTopleftYa = 0;
    pClipSettings->xVSS.PanZoomTopleftXb = 0;
    pClipSettings->xVSS.PanZoomTopleftYb = 0;
    pClipSettings->xVSS.PanZoomXa = 0;
    pClipSettings->xVSS.PanZoomXb = 0;

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4xVSS_CreateClipSettings(): returning M4NO_ERROR");

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4xVSS_DuplicateClipSettings()
 * @brief    Duplicates a clip settings structure, performing allocations if required
 *
 * @param    pClipSettingsDest    (IN) Pointer to a valid M4VSS3GPP_ClipSettings structure
 * @param    pClipSettingsOrig    (IN) Pointer to a valid M4VSS3GPP_ClipSettings structure
 * @param   bCopyEffects        (IN) Flag to know if we have to duplicate effects
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pClipSettings is M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_DuplicateClipSettings( M4VSS3GPP_ClipSettings
                                       *pClipSettingsDest,
                                       M4VSS3GPP_ClipSettings *pClipSettingsOrig,
                                        M4OSA_Bool bCopyEffects )
{
    M4OSA_ERR err = M4NO_ERROR;

    M4OSA_TRACE3_2(
        "M4xVSS_DuplicateClipSettings called with dest=0x%p src=0x%p",
        pClipSettingsDest, pClipSettingsOrig);

    /* Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pClipSettingsDest), M4ERR_PARAMETER,
        "M4xVSS_DuplicateClipSettings: pClipSettingsDest is NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pClipSettingsOrig), M4ERR_PARAMETER,
        "M4xVSS_DuplicateClipSettings: pClipSettingsOrig is NULL");

    /* Call inherited VSS3GPP duplication */
    err = M4VSS3GPP_editDuplicateClipSettings(pClipSettingsDest,
        pClipSettingsOrig, bCopyEffects);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1("M4xVSS_CreateClipSettings :\
                       ERROR in M4VSS3GPP_editDuplicateClipSettings = 0x%x", err);
        return err;
    }

    /* Return with no error */
    M4OSA_TRACE3_0("M4xVSS_DuplicateClipSettings(): returning M4NO_ERROR");

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4xVSS_FreeClipSettings()
 * @brief    Free the pointers allocated in the ClipSetting structure (pFile, Effects, ...).
 *
 * @param    pClipSettings        (IN) Pointer to a valid M4VSS3GPP_ClipSettings structure
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pClipSettings is M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_FreeClipSettings( M4VSS3GPP_ClipSettings *pClipSettings )
{
    /**
    *    Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pClipSettings), M4ERR_PARAMETER,
        "M4xVSS_FreeClipSettings: pClipSettings is NULL");

    /* Free inherited VSS3GPP stuff */
    M4VSS3GPP_editFreeClipSettings(pClipSettings);

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_getMCSContext(M4OSA_Context pContext, M4OSA_Context* mcsContext)
 * @brief        This function returns the MCS context within the xVSS internal context
 * @note        This function must be called only after VSS state has moved to analyzing state or
 * beyond
 *
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @param    mcsContext        (OUT) Pointer to pointer of mcs context to return
 * @return    M4NO_ERROR:        No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_getMCSContext( M4OSA_Context pContext,
                               M4OSA_Context *mcsContext )
{
    M4xVSS_Context *xVSS_context = (M4xVSS_Context *)pContext;
    M4OSA_ERR err = M4NO_ERROR;

    /**
    *    Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4xVSS_getMCSContext: pContext is NULL");

    if( xVSS_context->m_state == M4xVSS_kStateInitialized )
    {
        M4OSA_TRACE1_1("M4xVSS_getMCSContext: Bad state! State is %d",\
            xVSS_context->m_state);
        return M4ERR_STATE;
    }

    *mcsContext = xVSS_context->pMCS_Ctxt;

    return err;
}

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_getVSS3GPPContext(M4OSA_Context pContext,
 *                                                   M4OSA_Context* mcsContext)
 * @brief        This function returns the VSS3GPP context within the xVSS internal context
 * @note        This function must be called only after VSS state has moved to Generating preview
 *              or beyond
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @param    vss3gppContext        (OUT) Pointer to pointer of vss3gpp context to return
 * @return    M4NO_ERROR:        No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_getVSS3GPPContext( M4OSA_Context pContext,
                                   M4OSA_Context *vss3gppContext )
{
    M4xVSS_Context *xVSS_context = (M4xVSS_Context *)pContext;
    M4OSA_ERR err = M4NO_ERROR;

    /**
    *    Check input parameter */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4xVSS_getVSS3GPPContext: pContext is NULL");

    if( xVSS_context->m_state < M4xVSS_kStateSaving )
    {
        M4OSA_TRACE1_1("M4xVSS_getVSS3GPPContext: Bad state! State is %d",\
            xVSS_context->m_state);
        return M4ERR_STATE;
    }

    *vss3gppContext = xVSS_context->pCurrentEditContext;

    return err;
}

M4OSA_ERR M4xVSS_getVideoDecoderCapabilities(M4DECODER_VideoDecoders **decoders) {
    M4OSA_ERR err = M4NO_ERROR;

    // Call the decoder api directly
    // to get all the video decoder capablities.
    err = VideoEditorVideoDecoder_getVideoDecodersAndCapabilities(decoders);
    return err;
}
