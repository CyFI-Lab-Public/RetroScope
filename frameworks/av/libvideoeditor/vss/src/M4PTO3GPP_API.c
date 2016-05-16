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
 * @file    M4PTO3GPP_API.c
 * @brief   Picture to 3gpp Service implementation.
 * @note
 ******************************************************************************
*/

/*16 bytes signature to be written in the generated 3gp files */
#define M4PTO3GPP_SIGNATURE     "NXP-SW : PTO3GPP"

/****************/
/*** Includes ***/
/****************/

/**
 *  Our header */
#include "M4PTO3GPP_InternalTypes.h"
#include "M4PTO3GPP_API.h"

/**
 *  Our errors */
#include "M4PTO3GPP_ErrorCodes.h"

#ifdef M4VSS_SUPPORT_ENCODER_MPEG4
#include "VideoEditorVideoEncoder.h"
#endif


/**
 *  OSAL headers */
#include "M4OSA_Memory.h"       /* OSAL memory management */
#include "M4OSA_Debug.h"        /* OSAL debug management */


/************************/
/*** Various Magicals ***/
/************************/

#define M4PTO3GPP_WRITER_AUDIO_STREAM_ID                1
#define M4PTO3GPP_WRITER_VIDEO_STREAM_ID                2
#define M4PTO3GPP_QUANTIZER_STEP                        4       /**< Quantizer step */
#define M4PTO3GPP_WRITER_AUDIO_PROFILE_LEVEL            0xFF    /**< No specific profile and
                                                                     level */
#define M4PTO3GPP_WRITER_AUDIO_AMR_TIME_SCALE           8000    /**< AMR */
#define M4PTO3GPP_BITRATE_REGULATION_CTS_PERIOD_IN_MS   500     /**< MAGICAL */
#define M4PTO3GPP_MARGE_OF_FILE_SIZE                    25000   /**< MAGICAL */
/**
 ******************************************************************************
 * define   AMR 12.2 kbps silence frame
 ******************************************************************************
*/
#define M4PTO3GPP_AMR_AU_SILENCE_FRAME_122_SIZE     32
#define M4PTO3GPP_AMR_AU_SILENCE_FRAME_122_DURATION 20
const M4OSA_UInt8 M4PTO3GPP_AMR_AU_SILENCE_122_FRAME[M4PTO3GPP_AMR_AU_SILENCE_FRAME_122_SIZE]=
{ 0x3C, 0x91, 0x17, 0x16, 0xBE, 0x66, 0x78, 0x00, 0x00, 0x01, 0xE7, 0xAF,
  0x00, 0x00, 0x00, 0x00, 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
  0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

#define M4PTO3GPP_AMR_AU_SILENCE_FRAME_048_SIZE     13
#define M4PTO3GPP_AMR_AU_SILENCE_FRAME_048_DURATION 20
const M4OSA_UInt8 M4PTO3GPP_AMR_AU_SILENCE_048_FRAME[M4PTO3GPP_AMR_AU_SILENCE_FRAME_048_SIZE] =
{ 0x04, 0xFF, 0x18, 0xC7, 0xF0, 0x0D, 0x04, 0x33, 0xFF, 0xE0, 0x00, 0x00, 0x00 };

/***************************/
/*** "Private" functions ***/
/***************************/
static M4OSA_ERR M4PTO3GPP_Ready4Processing(M4PTO3GPP_InternalContext* pC);

/****************************/
/*** "External" functions ***/
/****************************/
extern M4OSA_ERR M4WRITER_3GP_getInterfaces(M4WRITER_OutputFileType* pType,
                                            M4WRITER_GlobalInterface** SrcGlobalInterface,
                                            M4WRITER_DataInterface** SrcDataInterface);
extern M4OSA_ERR M4READER_AMR_getInterfaces(M4READER_MediaType *pMediaType,
                                            M4READER_GlobalInterface **pRdrGlobalInterface,
                                            M4READER_DataInterface **pRdrDataInterface);
extern M4OSA_ERR M4READER_3GP_getInterfaces(M4READER_MediaType *pMediaType,
                                            M4READER_GlobalInterface **pRdrGlobalInterface,
                                            M4READER_DataInterface **pRdrDataInterface);

/****************************/
/*** "Static" functions ***/
/****************************/
static M4OSA_ERR M4PTO3GPP_writeAmrSilence122Frame(
                                    M4WRITER_DataInterface* pWriterDataIntInterface,
                                    M4WRITER_Context* pWriterContext,
                                    M4SYS_AccessUnit* pWriterAudioAU,
                                    M4OSA_Time mtIncCts);
static M4OSA_ERR M4PTO3GPP_writeAmrSilence048Frame(
                                   M4WRITER_DataInterface* pWriterDataIntInterface,
                                   M4WRITER_Context* pWriterContext,
                                   M4SYS_AccessUnit* pWriterAudioAU,
                                   M4OSA_Time mtIncCts);
/**
 ******************************************************************************
 * M4OSA_ERR M4PTO3GPP_GetVersion(M4_VersionInfo* pVersionInfo);
 * @brief   Get the M4PTO3GPP version.
 * @note    Can be called anytime. Do not need any context.
 * @param   pVersionInfo        (OUT) Pointer to a version info structure
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    pVersionInfo is M4OSA_NULL (If Debug Level >= 2)
 ******************************************************************************
*/

/*********************************************************/
M4OSA_ERR M4PTO3GPP_GetVersion(M4_VersionInfo* pVersionInfo)
/*********************************************************/
{
    M4OSA_TRACE3_1("M4PTO3GPP_GetVersion called with pVersionInfo=0x%x", pVersionInfo);

    /**
     *  Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL==pVersionInfo),M4ERR_PARAMETER,
            "M4PTO3GPP_GetVersion: pVersionInfo is M4OSA_NULL");

    pVersionInfo->m_major       = M4PTO3GPP_VERSION_MAJOR;
    pVersionInfo->m_minor       = M4PTO3GPP_VERSION_MINOR;
    pVersionInfo->m_revision    = M4PTO3GPP_VERSION_REVISION;

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4PTO3GPP_Init(M4PTO3GPP_Context* pContext);
 * @brief   Initializes the M4PTO3GPP (allocates an execution context).
 * @note
 * @param   pContext            (OUT) Pointer on the M4PTO3GPP context to allocate
 * @param   pFileReadPtrFct     (IN) Pointer to OSAL file reader functions
 * @param   pFileWritePtrFct    (IN) Pointer to OSAL file writer functions
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (If Debug Level >= 2)
 * @return  M4ERR_ALLOC:        There is no more available memory
 ******************************************************************************
*/
/*********************************************************/
M4OSA_ERR M4PTO3GPP_Init(   M4PTO3GPP_Context* pContext,
                            M4OSA_FileReadPointer* pFileReadPtrFct,
                            M4OSA_FileWriterPointer* pFileWritePtrFct)
/*********************************************************/
{
    M4PTO3GPP_InternalContext *pC;
    M4OSA_UInt32 i;

    M4OSA_TRACE3_1("M4PTO3GPP_Init called with pContext=0x%x", pContext);

    /**
     *  Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
            "M4PTO3GPP_Init: pContext is M4OSA_NULL");

    /**
     *  Allocate the M4PTO3GPP context and return it to the user */
    pC = (M4PTO3GPP_InternalContext*)M4OSA_32bitAlignedMalloc(sizeof(M4PTO3GPP_InternalContext), M4PTO3GPP,
        (M4OSA_Char *)"M4PTO3GPP_InternalContext");
    *pContext = pC;
    if (M4OSA_NULL == pC)
    {
        M4OSA_TRACE1_0("M4PTO3GPP_Step(): unable to allocate M4PTO3GPP_InternalContext,\
                       returning M4ERR_ALLOC");
        return M4ERR_ALLOC;
    }

    /**
     *  Init the context. All pointers must be initialized to M4OSA_NULL because CleanUp()
        can be called just after Init(). */
    pC->m_State = M4PTO3GPP_kState_CREATED;
    pC->m_VideoState = M4PTO3GPP_kStreamState_NOSTREAM;
    pC->m_AudioState = M4PTO3GPP_kStreamState_NOSTREAM;

    /**
     *  Reader stuff */
    pC->m_pReaderAudioAU        = M4OSA_NULL;
    pC->m_pReaderAudioStream    = M4OSA_NULL;

    /**
     *  Writer stuff */
    pC->m_pEncoderHeader        = M4OSA_NULL;
    pC->m_pWriterVideoStream    = M4OSA_NULL;
    pC->m_pWriterAudioStream    = M4OSA_NULL;
    pC->m_pWriterVideoStreamInfo= M4OSA_NULL;
    pC->m_pWriterAudioStreamInfo= M4OSA_NULL;

    /**
     *  Contexts of the used modules  */
    pC->m_pAudioReaderContext    = M4OSA_NULL;
    pC->m_p3gpWriterContext  = M4OSA_NULL;
    pC->m_pMp4EncoderContext = M4OSA_NULL;
    pC->m_eEncoderState = M4PTO3GPP_kNoEncoder;

    /**
     *  Interfaces of the used modules */
    pC->m_pReaderGlobInt    = M4OSA_NULL;
    pC->m_pReaderDataInt    = M4OSA_NULL;
    pC->m_pWriterGlobInt    = M4OSA_NULL;
    pC->m_pWriterDataInt    = M4OSA_NULL;
    pC->m_pEncoderInt       = M4OSA_NULL;
    pC->m_pEncoderExternalAPI = M4OSA_NULL;
    pC->m_pEncoderUserData = M4OSA_NULL;

    /**
     * Fill the OSAL file function set */
    pC->pOsalFileRead = pFileReadPtrFct;
    pC->pOsalFileWrite = pFileWritePtrFct;

    /**
     *  Video rate control stuff */
    pC->m_mtCts             = 0.0F;
    pC->m_mtNextCts         = 0.0F;
    pC->m_mtAudioCts        = 0.0F;
    pC->m_AudioOffSet       = 0.0F;
    pC->m_dLastVideoRegulCts= 0.0F;
    pC->m_PrevAudioCts      = 0.0F;
    pC->m_DeltaAudioCts     = 0.0F;

    pC->m_MaxFileSize       = 0;
    pC->m_CurrentFileSize   = 0;

    pC->m_IsLastPicture         = M4OSA_FALSE;
    pC->m_bAudioPaddingSilence  = M4OSA_FALSE;
    pC->m_bLastInternalCallBack = M4OSA_FALSE;
    pC->m_NbCurrentFrame        = 0;

    pC->pSavedPlane = M4OSA_NULL;
    pC->uiSavedDuration = 0;

    M4OSA_TRACE3_0("M4PTO3GPP_Init(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4PTO3GPP_Open(M4PTO3GPP_Context pContext, M4PTO3GPP_Params* pParams);
 * @brief   Set the M4PTO3GPP input and output files.
 * @note    It opens the input file, but the output file may not be created yet.
 * @param   pContext            (IN) M4PTO3GPP context
 * @param   pParams             (IN) Pointer to the parameters for the PTO3GPP.
 * @note    The pointed structure can be de-allocated after this function returns because
 *          it is internally copied by the PTO3GPP
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return  M4ERR_STATE:        M4PTO3GPP is not in an appropriate state for this function to be
                                 called
 * @return  M4ERR_ALLOC:        There is no more available memory
 * @return  ERR_PTO3GPP_INVALID_VIDEO_FRAME_SIZE_FOR_H263 The output video frame
 *                              size parameter is incompatible with H263 encoding
 * @return  ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_FORMAT       The output video format
                                                            parameter is undefined
 * @return  ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_BITRATE      The output video bit-rate parameter
                                                            is undefined
 * @return  ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_FRAME_SIZE   The output video frame size parameter
                                                            is undefined
 * @return  ERR_PTO3GPP_UNDEFINED_OUTPUT_FILE_SIZE          The output file size parameter
                                                            is undefined
 * @return  ERR_PTO3GPP_UNDEFINED_AUDIO_PADDING             The output audio padding parameter
                                                            is undefined
 * @return  ERR_PTO3GPP_UNHANDLED_AUDIO_TRACK_INPUT_FILE    The input audio file contains
                                                            a track format not handled by PTO3GPP
 ******************************************************************************
*/
/*********************************************************/
M4OSA_ERR M4PTO3GPP_Open(M4PTO3GPP_Context pContext, M4PTO3GPP_Params* pParams)
/*********************************************************/
{
    M4PTO3GPP_InternalContext   *pC = (M4PTO3GPP_InternalContext*)(pContext);
    M4OSA_ERR                   err = M4NO_ERROR;

    M4READER_MediaFamily    mediaFamily;
    M4_StreamHandler*       pStreamHandler;
    M4READER_MediaType      readerMediaType;

    M4OSA_TRACE2_2("M4PTO3GPP_Open called with pContext=0x%x, pParams=0x%x", pContext, pParams);

    /**
     *  Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER, \
                    "M4PTO3GPP_Open: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pParams),  M4ERR_PARAMETER, \
                    "M4PTO3GPP_Open: pParams is M4OSA_NULL");

    /**
     *  Check parameters correctness */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pParams->pPictureCallbackFct),
               M4ERR_PARAMETER, "M4PTO3GPP_Open: pC->m_Params.pPictureCallbackFct is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pParams->pPictureCallbackCtxt),
                M4ERR_PARAMETER,
                 "M4PTO3GPP_Open: pC->m_Params.pPictureCallbackCtxt is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pParams->pOutput3gppFile),
                M4ERR_PARAMETER, "M4PTO3GPP_Open: pC->m_Params.pOutput3gppFile is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pParams->pTemporaryFile),
                M4ERR_PARAMETER, "M4PTO3GPP_Open: pC->m_Params.pTemporaryFile is M4OSA_NULL");

    /**
     * Video Format */
    if( (M4VIDEOEDITING_kH263 != pParams->OutputVideoFormat) &&
        (M4VIDEOEDITING_kMPEG4 != pParams->OutputVideoFormat) &&
        (M4VIDEOEDITING_kH264 != pParams->OutputVideoFormat)) {
        M4OSA_TRACE1_0("M4PTO3GPP_Open: Undefined output video format");
        return ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_FORMAT;
     }

     /**
     * Video Bitrate */
    if(!((M4VIDEOEDITING_k16_KBPS       == pParams->OutputVideoBitrate) ||
         (M4VIDEOEDITING_k24_KBPS       == pParams->OutputVideoBitrate) ||
         (M4VIDEOEDITING_k32_KBPS       == pParams->OutputVideoBitrate) ||
         (M4VIDEOEDITING_k48_KBPS       == pParams->OutputVideoBitrate) ||
         (M4VIDEOEDITING_k64_KBPS       == pParams->OutputVideoBitrate) ||
         (M4VIDEOEDITING_k96_KBPS       == pParams->OutputVideoBitrate) ||
         (M4VIDEOEDITING_k128_KBPS      == pParams->OutputVideoBitrate) ||
         (M4VIDEOEDITING_k192_KBPS      == pParams->OutputVideoBitrate) ||
         (M4VIDEOEDITING_k256_KBPS      == pParams->OutputVideoBitrate) ||
         (M4VIDEOEDITING_k288_KBPS      == pParams->OutputVideoBitrate) ||
         (M4VIDEOEDITING_k384_KBPS      == pParams->OutputVideoBitrate) ||
         (M4VIDEOEDITING_k512_KBPS      == pParams->OutputVideoBitrate) ||
         (M4VIDEOEDITING_k800_KBPS      == pParams->OutputVideoBitrate) ||
         /*+ New Encoder bitrates */
         (M4VIDEOEDITING_k2_MBPS        == pParams->OutputVideoBitrate) ||
         (M4VIDEOEDITING_k5_MBPS        == pParams->OutputVideoBitrate) ||
         (M4VIDEOEDITING_k8_MBPS        == pParams->OutputVideoBitrate) ||
         (M4VIDEOEDITING_kVARIABLE_KBPS == pParams->OutputVideoBitrate))) {
        M4OSA_TRACE1_0("M4PTO3GPP_Open: Undefined output video bitrate");
        return ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_BITRATE;
    }

    /**
     * Video frame size */
    if (!((M4VIDEOEDITING_kSQCIF == pParams->OutputVideoFrameSize) ||
          (M4VIDEOEDITING_kQQVGA == pParams->OutputVideoFrameSize) ||
          (M4VIDEOEDITING_kQCIF == pParams->OutputVideoFrameSize) ||
          (M4VIDEOEDITING_kQVGA == pParams->OutputVideoFrameSize) ||
          (M4VIDEOEDITING_kCIF  == pParams->OutputVideoFrameSize) ||
          (M4VIDEOEDITING_kVGA  == pParams->OutputVideoFrameSize) ||

          (M4VIDEOEDITING_kNTSC == pParams->OutputVideoFrameSize) ||
          (M4VIDEOEDITING_kWVGA == pParams->OutputVideoFrameSize) ||

          (M4VIDEOEDITING_k640_360 == pParams->OutputVideoFrameSize) ||
          (M4VIDEOEDITING_k854_480 == pParams->OutputVideoFrameSize) ||
          (M4VIDEOEDITING_k1280_720 == pParams->OutputVideoFrameSize) ||
          (M4VIDEOEDITING_k1080_720 == pParams->OutputVideoFrameSize) ||
          (M4VIDEOEDITING_k960_720 == pParams->OutputVideoFrameSize) ||
          (M4VIDEOEDITING_k1920_1080 == pParams->OutputVideoFrameSize))) {
        M4OSA_TRACE1_0("M4PTO3GPP_Open: Undefined output video frame size");
        return ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_FRAME_SIZE;
    }

    /**
     * Maximum size of the output 3GPP file */
    if (!((M4PTO3GPP_k50_KB     == pParams->OutputFileMaxSize) ||
          (M4PTO3GPP_k75_KB     == pParams->OutputFileMaxSize) ||
          (M4PTO3GPP_k100_KB    == pParams->OutputFileMaxSize) ||
          (M4PTO3GPP_k150_KB    == pParams->OutputFileMaxSize) ||
          (M4PTO3GPP_k200_KB    == pParams->OutputFileMaxSize) ||
          (M4PTO3GPP_k300_KB    == pParams->OutputFileMaxSize) ||
          (M4PTO3GPP_k400_KB    == pParams->OutputFileMaxSize) ||
          (M4PTO3GPP_k500_KB    == pParams->OutputFileMaxSize) ||
          (M4PTO3GPP_kUNLIMITED == pParams->OutputFileMaxSize))) {
        M4OSA_TRACE1_0("M4PTO3GPP_Open: Undefined output 3GPP file size");
        return ERR_PTO3GPP_UNDEFINED_OUTPUT_FILE_SIZE;
    }

    /* Audio padding */
    if (M4OSA_NULL != pParams->pInputAudioTrackFile) {
        if ((!( (M4PTO3GPP_kAudioPaddingMode_None   == pParams->AudioPaddingMode) ||
                (M4PTO3GPP_kAudioPaddingMode_Silence== pParams->AudioPaddingMode) ||
                (M4PTO3GPP_kAudioPaddingMode_Loop   == pParams->AudioPaddingMode)))) {
            M4OSA_TRACE1_0("M4PTO3GPP_Open: Undefined audio padding");
            return ERR_PTO3GPP_UNDEFINED_AUDIO_PADDING;
        }
    }

    /**< Size check for H263 (only valid sizes are CIF, QCIF and SQCIF) */
    if ((M4VIDEOEDITING_kH263 == pParams->OutputVideoFormat) &&
        (M4VIDEOEDITING_kSQCIF != pParams->OutputVideoFrameSize) &&
        (M4VIDEOEDITING_kQCIF != pParams->OutputVideoFrameSize) &&
        (M4VIDEOEDITING_kCIF != pParams->OutputVideoFrameSize)) {
        M4OSA_TRACE1_0("M4PTO3GPP_Open():\
             returning ERR_PTO3GPP_INVALID_VIDEO_FRAME_SIZE_FOR_H263");
        return ERR_PTO3GPP_INVALID_VIDEO_FRAME_SIZE_FOR_H263;
    }

    /**
     *  Check state automaton */
    if (M4PTO3GPP_kState_CREATED != pC->m_State) {
        M4OSA_TRACE1_1("M4PTO3GPP_Open(): Wrong State (%d), returning M4ERR_STATE", pC->m_State);
        return M4ERR_STATE;
    }

    /**
     * Copy the M4PTO3GPP_Params structure */
    memcpy((void *)(&pC->m_Params),
                (void *)pParams, sizeof(M4PTO3GPP_Params));
    M4OSA_TRACE1_1("M4PTO3GPP_Open: outputVideoBitrate = %d", pC->m_Params.OutputVideoBitrate);

    /***********************************/
    /* Open input file with the reader */
    /***********************************/
    if (M4OSA_NULL != pC->m_Params.pInputAudioTrackFile) {
        /**
         * Get the reader interface according to the input audio file type */
        switch(pC->m_Params.AudioFileFormat)
        {
#ifdef M4VSS_SUPPORT_READER_AMR
        case M4VIDEOEDITING_kFileType_AMR:
        err = M4READER_AMR_getInterfaces( &readerMediaType, &pC->m_pReaderGlobInt,
                &pC->m_pReaderDataInt);
        if (M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("M4PTO3GPP_Open(): M4READER_AMR_getInterfaces returns 0x%x", err);
            return err;
        }
            break;
#endif

#ifdef AAC_SUPPORTED
        case M4VIDEOEDITING_kFileType_3GPP:
            err = M4READER_3GP_getInterfaces( &readerMediaType, &pC->m_pReaderGlobInt,
                    &pC->m_pReaderDataInt);
            if (M4NO_ERROR != err)
            {
                M4OSA_TRACE1_1("M4PTO3GPP_Open(): M4READER_3GP_getInterfaces returns 0x%x", err);
                return err;
            }
            break;
#endif

        default:
            return ERR_PTO3GPP_UNHANDLED_AUDIO_TRACK_INPUT_FILE;
        }

        /**
         *  Initializes the reader shell */
        err = pC->m_pReaderGlobInt->m_pFctCreate(&pC->m_pAudioReaderContext);
        if (M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("M4PTO3GPP_Open(): pReaderGlobInt->m_pFctCreate returns 0x%x", err);
            return err;
        }

        pC->m_pReaderDataInt->m_readerContext = pC->m_pAudioReaderContext;
        /**< Link the reader interface to the reader context */

        /**
         *  Set the reader shell file access functions */
        err = pC->m_pReaderGlobInt->m_pFctSetOption(pC->m_pAudioReaderContext,
            M4READER_kOptionID_SetOsaFileReaderFctsPtr,  (M4OSA_DataOption)pC->pOsalFileRead);
        if (M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("M4PTO3GPP_Open(): pReaderGlobInt->m_pFctSetOption returns 0x%x", err);
            return err;
        }

        /**
         *  Open the input audio file */
        err = pC->m_pReaderGlobInt->m_pFctOpen(pC->m_pAudioReaderContext,
            pC->m_Params.pInputAudioTrackFile);
        if (M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("M4PTO3GPP_Open(): pReaderGlobInt->m_pFctOpen returns 0x%x", err);
            pC->m_pReaderGlobInt->m_pFctDestroy(pC->m_pAudioReaderContext);
            pC->m_pAudioReaderContext = M4OSA_NULL;
            return err;
        }

        /**
         *  Get the audio streams from the input file */
        err = M4NO_ERROR;
        while (M4NO_ERROR == err)
        {
            err = pC->m_pReaderGlobInt->m_pFctGetNextStream(pC->m_pAudioReaderContext,
                &mediaFamily, &pStreamHandler);

            if((err == ((M4OSA_UInt32)M4ERR_READER_UNKNOWN_STREAM_TYPE)) ||
                   (err == ((M4OSA_UInt32)M4WAR_TOO_MUCH_STREAMS)))
            {
                err = M4NO_ERROR;
                continue;
            }

            if (M4NO_ERROR == err) /**< One stream found */
            {
                /**< Found an audio stream */
                if ((M4READER_kMediaFamilyAudio == mediaFamily)
                    && (M4OSA_NULL == pC->m_pReaderAudioStream))
                {
                    pC->m_pReaderAudioStream = (M4_AudioStreamHandler*)pStreamHandler;
                    /**< Keep pointer to the audio stream */
                    M4OSA_TRACE3_0("M4PTO3GPP_Open(): Found an audio stream in input");
                    pStreamHandler->m_bStreamIsOK = M4OSA_TRUE;

                    /**
                     *  Allocate audio AU used for read operations */
                    pC->m_pReaderAudioAU = (M4_AccessUnit*)M4OSA_32bitAlignedMalloc(sizeof(M4_AccessUnit),
                        M4PTO3GPP,(M4OSA_Char *)"pReaderAudioAU");
                    if (M4OSA_NULL == pC->m_pReaderAudioAU)
                    {
                        M4OSA_TRACE1_0("M4PTO3GPP_Open(): unable to allocate pReaderAudioAU, \
                                       returning M4ERR_ALLOC");
                        return M4ERR_ALLOC;
                    }

                    /**
                     *  Initializes an access Unit */
                    err = pC->m_pReaderGlobInt->m_pFctFillAuStruct(pC->m_pAudioReaderContext,
                            pStreamHandler, pC->m_pReaderAudioAU);
                    if (M4NO_ERROR != err)
                    {
                        M4OSA_TRACE1_1("M4PTO3GPP_Open():\
                         pReaderGlobInt->m_pFctFillAuStruct(audio)returns 0x%x", err);
                        return err;
                    }
                }
                else
                {
                    pStreamHandler->m_bStreamIsOK = M4OSA_FALSE;
                }
            }
            else if (M4WAR_NO_MORE_STREAM != err) /**< Unexpected error code */
            {
                M4OSA_TRACE1_1("M4PTO3GPP_Open():\
                     pReaderGlobInt->m_pFctGetNextStream returns 0x%x",
                    err);
                return err;
            }
        } /* while*/
    } /*if (M4OSA_NULL != pC->m_Params.pInputAudioTrackFile)*/

    pC->m_VideoState = M4PTO3GPP_kStreamState_STARTED;

    /**
     * Init the audio stream */
    if (M4OSA_NULL != pC->m_pReaderAudioStream)
    {
        pC->m_AudioState = M4PTO3GPP_kStreamState_STARTED;
        err = pC->m_pReaderGlobInt->m_pFctReset(pC->m_pAudioReaderContext,
            (M4_StreamHandler*)pC->m_pReaderAudioStream);
        if (M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("M4PTO3GPP_Open(): pReaderDataInt->m_pFctReset(audio returns 0x%x",
                 err);
            return err;
        }
    }

    /**
     *  Update state automaton */
    pC->m_State = M4PTO3GPP_kState_OPENED;

    /**
     * Get the max File size */
    switch(pC->m_Params.OutputFileMaxSize)
    {
    case M4PTO3GPP_k50_KB:  pC->m_MaxFileSize = 50000;  break;
    case M4PTO3GPP_k75_KB:  pC->m_MaxFileSize = 75000;  break;
    case M4PTO3GPP_k100_KB: pC->m_MaxFileSize = 100000; break;
    case M4PTO3GPP_k150_KB: pC->m_MaxFileSize = 150000; break;
    case M4PTO3GPP_k200_KB: pC->m_MaxFileSize = 200000; break;
    case M4PTO3GPP_k300_KB: pC->m_MaxFileSize = 300000; break;
    case M4PTO3GPP_k400_KB: pC->m_MaxFileSize = 400000; break;
    case M4PTO3GPP_k500_KB: pC->m_MaxFileSize = 500000; break;
    case M4PTO3GPP_kUNLIMITED:
    default:                                            break;
    }

    M4OSA_TRACE3_0("M4PTO3GPP_Open(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4PTO3GPP_Step(M4PTO3GPP_Context pContext);
 * @brief   Perform one step of trancoding.
 * @note
 * @param   pContext            (IN) M4PTO3GPP context
 * @return  M4NO_ERROR          No error
 * @return  M4ERR_PARAMETER     pContext is M4OSA_NULL
 * @return  M4ERR_STATE:    M4PTO3GPP is not in an appropriate state for this function
 *                           to be called
 * @return  M4PTO3GPP_WAR_END_OF_PROCESSING Encoding completed
 ******************************************************************************
*/
/*********************************************************/
M4OSA_ERR M4PTO3GPP_Step(M4PTO3GPP_Context pContext)
/*********************************************************/
{
    M4PTO3GPP_InternalContext *pC = (M4PTO3GPP_InternalContext*)(pContext);
    M4OSA_ERR err = M4NO_ERROR;
    M4OSA_UInt32 l_uiAudioStepCount = 0;
    M4OSA_Int32  JumpToTime = 0;
    M4OSA_Time  mtIncCts;

    /**
     *  Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL==pContext), M4ERR_PARAMETER,
                "M4PTO3GPP_Step: pContext is M4OSA_NULL");

    /**
     *  Check state automaton */
    if ( !((M4PTO3GPP_kState_OPENED == pC->m_State) || (M4PTO3GPP_kState_READY == pC->m_State)) )
    {
        M4OSA_TRACE1_1("M4PTO3GPP_Step(): Wrong State (%d), returning M4ERR_STATE", pC->m_State);
        return M4ERR_STATE;
    }

    /******************************************************************/
    /**
     *  In case this is the first step, we prepare the decoder, the encoder and the writer */
    if (M4PTO3GPP_kState_OPENED == pC->m_State)
    {
        M4OSA_TRACE2_0("M4PTO3GPP_Step(): This is the first step, \
                       calling M4PTO3GPP_Ready4Processing");

        /**
         *  Prepare the reader, the decoder, the encoder, the writer... */
        err = M4PTO3GPP_Ready4Processing(pC);
        if (M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("M4PTO3GPP_Step(): M4PTO3GPP_Ready4Processing() returns 0x%x", err);
            return err;
        }

        /**
         *  Update state automaton */
        pC->m_State = M4PTO3GPP_kState_READY;

        M4OSA_TRACE3_0("M4PTO3GPP_Step(): returning M4NO_ERROR (a)");
        return M4NO_ERROR; /**< we only do that in the first step, \
                           first REAL step will be the next one */
    }


    /*
     * Check if we reached the targeted file size.
     * We do that before the encoding, because the core encoder has to know if this is
     * the last frame to encode */
    err = pC->m_pWriterGlobInt->pFctGetOption(pC->m_p3gpWriterContext,
        M4WRITER_kFileSizeAudioEstimated, (M4OSA_DataOption) &pC->m_CurrentFileSize);
    if ((0 != pC->m_MaxFileSize) &&
        /**< Add a marge to the file size in order to never exceed the max file size */
       ((pC->m_CurrentFileSize + M4PTO3GPP_MARGE_OF_FILE_SIZE) >= pC->m_MaxFileSize))
    {
        pC->m_IsLastPicture = M4OSA_TRUE;
    }

    /******************************************************************
    *  At that point we are in M4PTO3GPP_kState_READY state
    *  We perform one step of video encoding
    ******************************************************************/

    /************* VIDEO ENCODING ***************/
    if (M4PTO3GPP_kStreamState_STARTED == pC->m_VideoState) /**<If the video encoding is going on*/
    {   /**
         * Call the encoder  */
        pC->m_NbCurrentFrame++;

        /* Check if it is the last frame the to encode */
        if((pC->m_Params.NbVideoFrames > 0) \
            && (pC->m_NbCurrentFrame >= pC->m_Params.NbVideoFrames))
        {
            pC->m_IsLastPicture = M4OSA_TRUE;
        }

        M4OSA_TRACE2_2("M4PTO3GPP_Step(): Calling pEncoderInt->pFctEncode with videoCts = %.2f\
                       nb = %lu", pC->m_mtCts, pC->m_NbCurrentFrame);

        err = pC->m_pEncoderInt->pFctEncode(pC->m_pMp4EncoderContext, M4OSA_NULL,
            /**< The input plane is null because the input Picture will be obtained by the\
            VPP filter from the context */
                                        pC->m_mtCts,
                                        (pC->m_IsLastPicture ?
                                        M4ENCODER_kLastFrame : M4ENCODER_kNormalFrame) );
        /**< Last param set to M4OSA_TRUE signals that this is the last frame to be encoded,\
        M4OSA_FALSE else */

        M4OSA_TRACE3_2("M4PTO3GPP_Step(): pEncoderInt->pFctEncode returns 0x%x, vidFormat =0x%x",
            err, pC->m_Params.OutputVideoFormat);
        if((M4NO_ERROR == err) && (M4VIDEOEDITING_kH264 == pC->m_Params.OutputVideoFormat))
        {
            /* Check if last frame.*
            *                  */
            if(M4OSA_TRUE == pC->m_IsLastPicture)
            {
                M4OSA_TRACE3_0("M4PTO3GPP_Step(): Last picture");
                pC->m_VideoState = M4PTO3GPP_kStreamState_FINISHED;
            }

        }

        if (M4WAR_NO_MORE_AU == err) /**< The video encoding is finished */
        {
            M4OSA_TRACE3_0("M4PTO3GPP_Step(): pEncoderInt->pFctEncode returns M4WAR_NO_MORE_AU");
            pC->m_VideoState = M4PTO3GPP_kStreamState_FINISHED;
        }
        else if (M4NO_ERROR != err)     /**< Unexpected error code */
        {
            if( (((M4OSA_UInt32)M4WAR_WRITER_STOP_REQ) == err) ||
                    (((M4OSA_UInt32)M4ERR_ALLOC) == err) )
            {
                M4OSA_TRACE1_0("M4PTO3GPP_Step: returning ERR_PTO3GPP_ENCODER_ACCES_UNIT_ERROR");
                return ERR_PTO3GPP_ENCODER_ACCES_UNIT_ERROR;
            }
            else
            {
                M4OSA_TRACE1_1("M4PTO3GPP_Step(): pEncoderInt->pFctEncode(last) (a) returns 0x%x",
                    err);
                return err;
            }
        }
    } /**< End of video encoding */


    /****** AUDIO TRANSCODING (read + null encoding + write) ******/
    if (M4PTO3GPP_kStreamState_STARTED == pC->m_AudioState)
    {
        while ( (M4PTO3GPP_kStreamState_STARTED == pC->m_AudioState) &&
                (pC->m_mtAudioCts < pC->m_mtNextCts))

        {
            l_uiAudioStepCount++;
            if (M4OSA_FALSE == pC->m_bAudioPaddingSilence)
            {
                /**< Read the next audio AU in the input Audio file */
                err = pC->m_pReaderDataInt->m_pFctGetNextAu(pC->m_pAudioReaderContext,
                    (M4_StreamHandler*)pC->m_pReaderAudioStream, pC->m_pReaderAudioAU);
                pC->m_mtAudioCts = pC->m_pReaderAudioAU->m_CTS + pC->m_AudioOffSet;

                if (M4WAR_NO_MORE_AU == err)    /* The audio transcoding is finished */
                {
                    M4OSA_TRACE2_0("M4PTO3GPP_Step():\
                                  pReaderDataInt->m_pFctGetNextAu(audio) returns \
                                    M4WAR_NO_MORE_AU");
                    switch(pC->m_Params.AudioPaddingMode)
                    {
                        case M4PTO3GPP_kAudioPaddingMode_None:

                            pC->m_AudioState = M4PTO3GPP_kStreamState_FINISHED;
                            break;

                        case M4PTO3GPP_kAudioPaddingMode_Silence:

                            if (M4DA_StreamTypeAudioAmrNarrowBand
                                != pC->m_pReaderAudioStream->m_basicProperties.m_streamType)
                                /**< Do nothing if the input audio file format is not AMR */
                            {
                                pC->m_AudioState = M4PTO3GPP_kStreamState_FINISHED;
                            }
                            else
                            {
                                pC->m_bAudioPaddingSilence = M4OSA_TRUE;
                            }
                            break;

                        case M4PTO3GPP_kAudioPaddingMode_Loop:

                            /**< Jump to the beginning of the audio file */
                            err = pC->m_pReaderGlobInt->m_pFctJump(pC->m_pAudioReaderContext,
                                (M4_StreamHandler*)pC->m_pReaderAudioStream, &JumpToTime);

                            if (M4NO_ERROR != err)
                            {
                                M4OSA_TRACE1_1("M4PTO3GPP_Step(): \
                                              pReaderDataInt->m_pFctReset(audio returns 0x%x",
                                               err);
                                return err;
                            }

                            if (M4DA_StreamTypeAudioAmrNarrowBand
                                == pC->m_pReaderAudioStream->m_basicProperties.m_streamType)
                            {
                                pC->m_mtAudioCts += 20; /*< SEMC bug fixed at Lund */
                                pC->m_AudioOffSet = pC->m_mtAudioCts;

                                /**
                                 * 'BZZZ' bug fix:
                                 * add a silence frame */
                                mtIncCts = (M4OSA_Time)((pC->m_mtAudioCts) *
                                    (pC->m_pWriterAudioStream->timeScale / 1000.0));
                                err = M4PTO3GPP_writeAmrSilence122Frame(pC->m_pWriterDataInt,
                                    pC->m_p3gpWriterContext, &pC->m_WriterAudioAU, mtIncCts);

                                if (M4NO_ERROR != err)
                                {
                                    M4OSA_TRACE1_1("M4PTO3GPP_Step(): \
                                                   M4PTO3GPP_AddAmrSilenceSid returns 0x%x", err);
                                    return err;
                                }/**< Add => no audio cts increment...*/
                            }
                            else
                            {
                                pC->m_AudioOffSet = pC->m_mtAudioCts + pC->m_DeltaAudioCts;
                            }
                            break;
                    } /* end of: switch */
                }
                else if (M4NO_ERROR != err)
                {
                    M4OSA_TRACE1_1("M4PTO3GPP_Step(): pReaderDataInt->m_pFctGetNextAu(Audio)\
                                   returns 0x%x", err);
                    return err;
                }
                else
                {
                    /**
                     * Save the delta Cts (AAC only) */
                    pC->m_DeltaAudioCts = pC->m_pReaderAudioAU->m_CTS - pC->m_PrevAudioCts;
                    pC->m_PrevAudioCts  = pC->m_pReaderAudioAU->m_CTS;

                    /**
                     *  Prepare the writer AU */
                    err = pC->m_pWriterDataInt->pStartAU(pC->m_p3gpWriterContext, 1,
                        &pC->m_WriterAudioAU);
                    if (M4NO_ERROR != err)
                    {
                        M4OSA_TRACE1_1("M4PTO3GPP_Step(): pWriterDataInt->pStartAU(Audio)\
                                       returns 0x%x", err);
                        return err;
                    }

                    /**
                     *  Copy audio data from reader AU to writer AU */
                    M4OSA_TRACE2_1("M4PTO3GPP_Step(): Copying audio AU: size=%d",
                        pC->m_pReaderAudioAU->m_size);
                    memcpy((void *)pC->m_WriterAudioAU.dataAddress,
                        (void *)pC->m_pReaderAudioAU->m_dataAddress,
                        pC->m_pReaderAudioAU->m_size);
                    pC->m_WriterAudioAU.size = pC->m_pReaderAudioAU->m_size;

                    /**
                     *  Convert CTS unit from milliseconds to timescale */
                    if (M4DA_StreamTypeAudioAmrNarrowBand
                        != pC->m_pReaderAudioStream->m_basicProperties.m_streamType)
                    {
                        pC->m_WriterAudioAU.CTS  = (M4OSA_Time)
                            ((pC->m_AudioOffSet + pC->m_pReaderAudioAU->m_CTS)
                            * pC->m_pWriterAudioStream->timeScale / 1000.0);
                    }
                    else
                    {
                        pC->m_WriterAudioAU.CTS = (M4OSA_Time)(pC->m_mtAudioCts *
                            (pC->m_pWriterAudioStream->timeScale / 1000.0));
                    }
                    pC->m_WriterAudioAU.nbFrag = 0;
                    M4OSA_TRACE2_1("M4PTO3GPP_Step(): audio AU: CTS=%d ms", pC->m_mtAudioCts
                        /*pC->m_pReaderAudioAU->m_CTS*/);

                    /**
                     *  Write it to the output file */
                    err = pC->m_pWriterDataInt->pProcessAU(pC->m_p3gpWriterContext, 1,
                        &pC->m_WriterAudioAU);

                    if (M4NO_ERROR != err)
                    {
                        M4OSA_TRACE1_1("M4PTO3GPP_Step(): pWriterDataInt->pProcessAU(Audio)\
                                       returns 0x%x", err);
                        return err;
                    }
                }
            }
            else /**< M4OSA_TRUE == pC->m_bAudioPaddingSilence */
            {
                if (M4DA_StreamTypeAudioAmrNarrowBand ==
                    pC->m_pReaderAudioStream->m_basicProperties.m_streamType)
                {
                    /**
                     * Fill in audio au with silence */
                    pC->m_mtAudioCts += 20;

                    /**
                     * Padd with silence */
                    mtIncCts = (M4OSA_Time)(pC->m_mtAudioCts
                        * (pC->m_pWriterAudioStream->timeScale / 1000.0));
                    err = M4PTO3GPP_writeAmrSilence048Frame(pC->m_pWriterDataInt,
                        pC->m_p3gpWriterContext, &pC->m_WriterAudioAU, mtIncCts);

                    if (M4NO_ERROR != err)
                    {
                        M4OSA_TRACE1_1("M4PTO3GPP_Step(): M4PTO3GPP_AddAmrSilenceSid returns 0x%x",
                            err);
                        return err;
                    }
                }
                else /**< Do nothing if the input audio file format is not AMR */
                {
                    pC->m_AudioState = M4PTO3GPP_kStreamState_FINISHED;
                }

            }
        } /**< while */
    } /**< End of audio encoding */

    pC->m_mtCts = pC->m_mtNextCts;

    /**
     *  The transcoding is finished when no stream is being encoded anymore */
    if (M4PTO3GPP_kStreamState_FINISHED == pC->m_VideoState)
    {
        pC->m_State = M4PTO3GPP_kState_FINISHED;
        M4OSA_TRACE2_0("M4PTO3GPP_Step(): transcoding finished, returning M4WAR_NO_MORE_AU");
        return M4PTO3GPP_WAR_END_OF_PROCESSING;
    }

    M4OSA_TRACE3_0("M4PTO3GPP_Step(): returning M4NO_ERROR (b)");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4PTO3GPP_Close(M4PTO3GPP_Context pContext);
 * @brief   Finish the M4PTO3GPP transcoding.
 * @note    The output 3GPP file is ready to be played after this call
 * @param   pContext            (IN) M4PTO3GPP context
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    pContext is M4OSA_NULL (If Debug Level >= 2)
 * @return  M4ERR_STATE:    M4PTO3GPP is not in an appropriate state for this function to be called
 ******************************************************************************
*/
/*********************************************************/
M4OSA_ERR M4PTO3GPP_Close(M4PTO3GPP_Context pContext)
/*********************************************************/
{
    M4PTO3GPP_InternalContext *pC = (M4PTO3GPP_InternalContext*)(pContext);
    M4OSA_ERR    osaErr = M4NO_ERROR;
    M4OSA_UInt32 lastCTS;
    M4ENCODER_Header* encHeader;
    M4SYS_StreamIDmemAddr streamHeader;

    M4OSA_TRACE3_1("M4PTO3GPP_Close called with pContext=0x%x", pContext);

    /**
     *  Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL==pContext), M4ERR_PARAMETER, "M4PTO3GPP_Close:\
                                                             pContext is M4OSA_NULL");

    /* Check state automaton */
    if ((pC->m_State != M4PTO3GPP_kState_OPENED) &&
        (pC->m_State != M4PTO3GPP_kState_READY) &&
        (pC->m_State != M4PTO3GPP_kState_FINISHED))
    {
        M4OSA_TRACE1_1("M4PTO3GPP_Close(): Wrong State (%d), returning M4ERR_STATE", pC->m_State);
        return M4ERR_STATE;
    }

    /*************************************/
    /******** Finish the encoding ********/
    /*************************************/
    if (M4PTO3GPP_kState_READY == pC->m_State)
    {
        pC->m_State = M4PTO3GPP_kState_FINISHED;
    }

    if (M4PTO3GPP_kEncoderRunning == pC->m_eEncoderState)
    {
        if (pC->m_pEncoderInt->pFctStop != M4OSA_NULL)
        {
            osaErr = pC->m_pEncoderInt->pFctStop(pC->m_pMp4EncoderContext);
            if (M4NO_ERROR != osaErr)
            {
                M4OSA_TRACE1_1("M4PTO3GPP_close: m_pEncoderInt->pFctStop returns 0x%x", osaErr);
                /* Well... how the heck do you handle a failed cleanup? */
            }
        }

        pC->m_eEncoderState = M4PTO3GPP_kEncoderStopped;
    }

    /* Has the encoder actually been opened? Don't close it if that's not the case. */
    if (M4PTO3GPP_kEncoderStopped == pC->m_eEncoderState)
    {
        osaErr = pC->m_pEncoderInt->pFctClose(pC->m_pMp4EncoderContext);
        if (M4NO_ERROR != osaErr)
        {
            M4OSA_TRACE1_1("M4PTO3GPP_close: m_pEncoderInt->pFctClose returns 0x%x", osaErr);
            /* Well... how the heck do you handle a failed cleanup? */
        }

        pC->m_eEncoderState = M4PTO3GPP_kEncoderClosed;
    }

    /*******************************/
    /******** Close 3GP out ********/
    /*******************************/

    if (M4OSA_NULL != pC->m_p3gpWriterContext)  /* happens in state _SET */
    {
        /* HW encoder: fetch the DSI from the shell video encoder, and feed it to the writer before
        closing it. */
        if ((M4VIDEOEDITING_kMPEG4 == pC->m_Params.OutputVideoFormat)
            || (M4VIDEOEDITING_kH264 == pC->m_Params.OutputVideoFormat))
        {
            osaErr = pC->m_pEncoderInt->pFctGetOption(pC->m_pMp4EncoderContext,
                M4ENCODER_kOptionID_EncoderHeader,
                                                            (M4OSA_DataOption)&encHeader);
            if ( (M4NO_ERROR != osaErr) || (M4OSA_NULL == encHeader->pBuf) )
            {
                M4OSA_TRACE1_1("M4PTO3GPP_close: failed to get the encoder header (err 0x%x)",
                    osaErr);
                /**< no return here, we still have stuff to deallocate after close, even if \
                it fails. */
            }
            else
            {
                /* set this header in the writer */
                streamHeader.streamID = M4PTO3GPP_WRITER_VIDEO_STREAM_ID;
                streamHeader.size = encHeader->Size;
                streamHeader.addr = (M4OSA_MemAddr32)encHeader->pBuf;
                osaErr = pC->m_pWriterGlobInt->pFctSetOption(pC->m_p3gpWriterContext,
                    M4WRITER_kDSI, &streamHeader);
                if (M4NO_ERROR != osaErr)
                {
                    M4OSA_TRACE1_1("M4PTO3GPP_close: failed to set the DSI in the writer \
                                (err 0x%x)   ", osaErr);
                }
            }
        }

        /* Update last Video CTS */
        lastCTS = (M4OSA_UInt32)pC->m_mtCts;

        osaErr = pC->m_pWriterGlobInt->pFctSetOption(pC->m_p3gpWriterContext,
            (M4OSA_UInt32)M4WRITER_kMaxFileDuration, &lastCTS);
        if (M4NO_ERROR != osaErr)
        {
            M4OSA_TRACE1_1("M4PTO3GPP_Close: SetOption(M4WRITER_kMaxFileDuration) returns 0x%x",
                osaErr);
        }

        /* Write and close the 3GP output file */
        osaErr = pC->m_pWriterGlobInt->pFctCloseWrite(pC->m_p3gpWriterContext);
        if (M4NO_ERROR != osaErr)
        {
            M4OSA_TRACE1_1("M4PTO3GPP_Close: pWriterGlobInt->pFctCloseWrite returns 0x%x", osaErr);
            /**< don't return yet, we have to close other things */
        }
        pC->m_p3gpWriterContext = M4OSA_NULL;
    }

    /**
     * State transition */
    pC->m_State = M4PTO3GPP_kState_CLOSED;

    M4OSA_TRACE3_1("M4PTO3GPP_Close(): returning 0x%x", osaErr);
    return osaErr;
}


/**
 ******************************************************************************
 * M4OSA_ERR M4PTO3GPP_CleanUp(M4PTO3GPP_Context pContext);
 * @brief   Free all resources used by the M4PTO3GPP.
 * @note    The context is no more valid after this call
 * @param   pContext            (IN) M4PTO3GPP context
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    pContext is M4OSA_NULL (If Debug Level >= 2)
 ******************************************************************************
*/
/*********************************************************/
M4OSA_ERR M4PTO3GPP_CleanUp(M4PTO3GPP_Context pContext)
/*********************************************************/
{
    M4OSA_ERR err = M4NO_ERROR;
    M4PTO3GPP_InternalContext *pC = (M4PTO3GPP_InternalContext*)(pContext);

    M4OSA_TRACE3_1("M4PTO3GPP_CleanUp called with pContext=0x%x", pContext);

    /**
     *  Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL==pContext),M4ERR_PARAMETER, "M4PTO3GPP_CleanUp: pContext \
                                                            is M4OSA_NULL");

    /**
     *  First call Close, if needed, to clean the video encoder */

    if ((M4PTO3GPP_kState_OPENED == pC->m_State) || (M4PTO3GPP_kState_READY == pC->m_State)
        || (M4PTO3GPP_kState_FINISHED == pC->m_State))
    {
        err = M4PTO3GPP_Close(pContext);
        if (M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("M4PTO3GPP_CleanUp: M4PTO3GPP_Close returns 0x%x", err);
            /**< don't return, we have to free other components */
        }
    }

    /**
     *  Free Audio reader stuff, if needed */

    if (M4OSA_NULL != pC->m_pAudioReaderContext) /**< may be M4OSA_NULL if M4PTO3GPP_Open was not\
                                                 called */
    {

        err = pC->m_pReaderGlobInt->m_pFctClose(pC->m_pAudioReaderContext);
        if (M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("M4PTO3GPP_CleanUp: pReaderGlobInt->m_pFctClose returns 0x%x", err);
            /**< don't return, we have to free other components */
        }
        err = pC->m_pReaderGlobInt->m_pFctDestroy(pC->m_pAudioReaderContext);
        pC->m_pAudioReaderContext = M4OSA_NULL;
        if (M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("M4PTO3GPP_CleanUp: pReaderGlobInt->m_pFctDestroy returns 0x%x", err);
            /**< don't return, we have to free other components */
        }
    }

    if (M4OSA_NULL != pC->m_pReaderAudioAU)
    {
        free(pC->m_pReaderAudioAU);
        pC->m_pReaderAudioAU = M4OSA_NULL;
    }

    /**
     *  Free video encoder stuff, if needed */
    if (M4OSA_NULL != pC->m_pMp4EncoderContext)
    {
        err = pC->m_pEncoderInt->pFctCleanup(pC->m_pMp4EncoderContext);
        pC->m_pMp4EncoderContext = M4OSA_NULL;
        if (M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("M4PTO3GPP_CleanUp: pEncoderInt->pFctDestroy returns 0x%x", err);
            /**< don't return, we have to free other components */
        }
    }

    if (M4OSA_NULL != pC->m_pWriterVideoStream)
    {
        free(pC->m_pWriterVideoStream);
        pC->m_pWriterVideoStream = M4OSA_NULL;
    }
    if (M4OSA_NULL != pC->m_pWriterAudioStream)
    {
        free(pC->m_pWriterAudioStream);
        pC->m_pWriterAudioStream = M4OSA_NULL;
    }
    if (M4OSA_NULL != pC->m_pWriterVideoStreamInfo)
    {
        free(pC->m_pWriterVideoStreamInfo);
        pC->m_pWriterVideoStreamInfo = M4OSA_NULL;
    }
    if (M4OSA_NULL != pC->m_pWriterAudioStreamInfo)
    {
        free(pC->m_pWriterAudioStreamInfo);
        pC->m_pWriterAudioStreamInfo = M4OSA_NULL;
    }


    /**
     *  Free the shells interfaces */
    if (M4OSA_NULL != pC->m_pReaderGlobInt)
    {
        free(pC->m_pReaderGlobInt);
        pC->m_pReaderGlobInt = M4OSA_NULL;
    }
    if (M4OSA_NULL != pC->m_pReaderDataInt)
    {
        free(pC->m_pReaderDataInt);
        pC->m_pReaderDataInt = M4OSA_NULL;
    }

    if(M4OSA_NULL != pC->m_pEncoderInt)
    {
        free(pC->m_pEncoderInt);
        pC->m_pEncoderInt = M4OSA_NULL;
    }
    if(M4OSA_NULL != pC->m_pWriterGlobInt)
    {
        free(pC->m_pWriterGlobInt);
        pC->m_pWriterGlobInt = M4OSA_NULL;
    }
    if(M4OSA_NULL != pC->m_pWriterDataInt)
    {
        free(pC->m_pWriterDataInt);
        pC->m_pWriterDataInt = M4OSA_NULL;
    }
    /**< Do not free pC->pOsaMemoryPtrFct and pC->pOsaMemoryPtrFct, because it's owned by the \
    application */

    /**
     *  Free the context itself */
    free(pC);
    pC = M4OSA_NULL;

    M4OSA_TRACE3_0("M4PTO3GPP_CleanUp(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/********************* INTERNAL FUNCTIONS *********************/

/**
 ******************************************************************************
 * M4OSA_ERR M4PTO3GPP_Ready4Processing(M4PTO3GPP_InternalContext* pC);
 * @brief   Prepare all resources and interfaces for the transcoding.
 * @note    It is called by the first M4OSA_Step() call
 * @param   pC          (IN) M4PTO3GPP private context
 * @return  M4NO_ERROR: No error
 * @return  Any error returned by an underlaying module
 ******************************************************************************
*/
/******************************************************/
M4OSA_ERR M4PTO3GPP_Ready4Processing(M4PTO3GPP_InternalContext* pC)
/******************************************************/
{
    M4OSA_ERR               err = M4NO_ERROR;
    M4WRITER_OutputFileType outputFileType;
    M4OSA_UInt32            uiVersion;
    M4ENCODER_Format        encFormat;
    M4ENCODER_AdvancedParams   EncParams;    /**< Encoder advanced parameters */
    M4SYS_StreamIDValue     optionValue;

    M4OSA_TRACE3_1("M4PTO3GPP_Ready4Processing called with pC=0x%x", pC);

    /******************************/
    /******************************/

    /********************************************/
    /********                            ********/
    /******** Video Encoder Parames init ********/
    /********                            ********/
    /********************************************/

    /**
     *  Get the correct encoder interface */
    switch(pC->m_Params.OutputVideoFormat)
    {
        case M4VIDEOEDITING_kMPEG4:
#ifdef M4VSS_SUPPORT_ENCODER_MPEG4
                err = VideoEditorVideoEncoder_getInterface_MPEG4(&encFormat, &pC->m_pEncoderInt,
                    M4ENCODER_OPEN_ADVANCED);
#else /* software MPEG4 encoder not available! */
                M4OSA_TRACE1_0("No MPEG4 encoder available! Did you forget to register one?");
                err = M4ERR_STATE;
#endif /* software MPEG4 encoder available? */
            break;
        case M4VIDEOEDITING_kH263:
#ifdef M4VSS_SUPPORT_ENCODER_MPEG4
                err = VideoEditorVideoEncoder_getInterface_H263(&encFormat, &pC->m_pEncoderInt,
                    M4ENCODER_OPEN_ADVANCED);
#else /* software H263 encoder not available! */
                M4OSA_TRACE1_0("No H263 encoder available! Did you forget to register one?");
                err = M4ERR_STATE;
#endif /* software H263 encoder available? */
            break;
        case M4VIDEOEDITING_kH264:
#ifdef M4VSS_SUPPORT_ENCODER_AVC
                err = VideoEditorVideoEncoder_getInterface_H264(&encFormat, &pC->m_pEncoderInt,
                    M4ENCODER_OPEN_ADVANCED);
#else /* software H264 encoder not available! */
                M4OSA_TRACE1_0("M4PTO3GPP_Ready4Processing: No H264 encoder available!\
                               Did you forget to register one?");
                err = M4ERR_STATE;
#endif /* software H264 encoder available? */
            break;
        default:
            M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: unknown format 0x%x returning \
                           ERR_M4PTO3GPP_UNDEFINED_OUTPUT_VIDEO_FORMAT",
                           pC->m_Params.OutputVideoFormat);
            return ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_FORMAT;
    }
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("switch(pC->m_Params.OutputVideoFormat): getInterfaces returns 0x%x", err);
        return err;
    }

    /**
     *  Fill encoder parameters according to M4PTO3GPP settings */

    /**
     * Video frame size */
    switch(pC->m_Params.OutputVideoFrameSize)
    {
        case M4VIDEOEDITING_kSQCIF :
            EncParams.FrameHeight = M4ENCODER_SQCIF_Height;
            EncParams.FrameWidth  = M4ENCODER_SQCIF_Width;
            break;
        case M4VIDEOEDITING_kQQVGA :
            EncParams.FrameHeight = M4ENCODER_QQVGA_Height;
            EncParams.FrameWidth  = M4ENCODER_QQVGA_Width;
            break;
        case M4VIDEOEDITING_kQCIF :
            EncParams.FrameHeight = M4ENCODER_QCIF_Height;
            EncParams.FrameWidth  = M4ENCODER_QCIF_Width;
            break;
        case M4VIDEOEDITING_kQVGA :
            EncParams.FrameHeight = M4ENCODER_QVGA_Height;
            EncParams.FrameWidth  = M4ENCODER_QVGA_Width;
            break;
        case M4VIDEOEDITING_kCIF :
            EncParams.FrameHeight = M4ENCODER_CIF_Height;
            EncParams.FrameWidth  = M4ENCODER_CIF_Width;
            break;
        case M4VIDEOEDITING_kVGA :
            EncParams.FrameHeight = M4ENCODER_VGA_Height;
            EncParams.FrameWidth  = M4ENCODER_VGA_Width;
            break;
/* +PR LV5807 */
        case M4VIDEOEDITING_kWVGA :
            EncParams.FrameHeight = M4ENCODER_WVGA_Height;
            EncParams.FrameWidth  = M4ENCODER_WVGA_Width;
            break;
        case M4VIDEOEDITING_kNTSC:
            EncParams.FrameHeight = M4ENCODER_NTSC_Height;
            EncParams.FrameWidth  = M4ENCODER_NTSC_Width;
            break;
/* -PR LV5807 */
/* +CR Google */
        case M4VIDEOEDITING_k640_360:
            EncParams.FrameHeight = M4ENCODER_640_360_Height;
            EncParams.FrameWidth  = M4ENCODER_640_360_Width;
            break;

        case M4VIDEOEDITING_k854_480:
            EncParams.FrameHeight = M4ENCODER_854_480_Height;
            EncParams.FrameWidth  = M4ENCODER_854_480_Width;
            break;

        case M4VIDEOEDITING_k1280_720:
            EncParams.FrameHeight = M4ENCODER_1280_720_Height;
            EncParams.FrameWidth  = M4ENCODER_1280_720_Width;
            break;

        case M4VIDEOEDITING_k1080_720:
            EncParams.FrameHeight = M4ENCODER_1080_720_Height;
            EncParams.FrameWidth  = M4ENCODER_1080_720_Width;
            break;

        case M4VIDEOEDITING_k960_720:
            EncParams.FrameHeight = M4ENCODER_960_720_Height;
            EncParams.FrameWidth  = M4ENCODER_960_720_Width;
            break;

        case M4VIDEOEDITING_k1920_1080:
            EncParams.FrameHeight = M4ENCODER_1920_1080_Height;
            EncParams.FrameWidth  = M4ENCODER_1920_1080_Width;
            break;
/* -CR Google */
        default :
            M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: unknown format 0x%x returning \
                           ERR_M4PTO3GPP_UNDEFINED_OUTPUT_VIDEO_FRAME_SIZE",
                           pC->m_Params.OutputVideoFrameSize);
            return ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_FRAME_SIZE;
    }

    EncParams.InputFormat = M4ENCODER_kIYUV420;

    /**
     * Video bitrate */
    switch(pC->m_Params.OutputVideoBitrate)
    {
        case M4VIDEOEDITING_k16_KBPS:
        case M4VIDEOEDITING_k24_KBPS:
        case M4VIDEOEDITING_k32_KBPS:
        case M4VIDEOEDITING_k48_KBPS:
        case M4VIDEOEDITING_k64_KBPS:
        case M4VIDEOEDITING_k96_KBPS:
        case M4VIDEOEDITING_k128_KBPS:
        case M4VIDEOEDITING_k192_KBPS:
        case M4VIDEOEDITING_k256_KBPS:
        case M4VIDEOEDITING_k288_KBPS:
        case M4VIDEOEDITING_k384_KBPS:
        case M4VIDEOEDITING_k512_KBPS:
        case M4VIDEOEDITING_k800_KBPS:
/*+ New Encoder bitrates */
        case M4VIDEOEDITING_k2_MBPS:
        case M4VIDEOEDITING_k5_MBPS:
        case M4VIDEOEDITING_k8_MBPS:
/*- New Encoder bitrates */
            EncParams.Bitrate = pC->m_Params.OutputVideoBitrate;
            break;

        case M4VIDEOEDITING_kVARIABLE_KBPS:
/*+ New Encoder bitrates */
            EncParams.Bitrate = M4VIDEOEDITING_k8_MBPS;
/*- New Encoder bitrates */
            break;

        default :
            M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: unknown format 0x%x returning\
                           ERR_M4PTO3GPP_UNDEFINED_OUTPUT_VIDEO_BITRATE",
                           pC->m_Params.OutputVideoBitrate);
            return ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_BITRATE;
    }

    /**
     * Video format */
    switch(pC->m_Params.OutputVideoFormat)
    {
        case M4VIDEOEDITING_kMPEG4 :
            EncParams.Format    = M4ENCODER_kMPEG4;
            break;
        case M4VIDEOEDITING_kH263 :
            EncParams.Format    = M4ENCODER_kH263;
            break;
        case M4VIDEOEDITING_kH264:
            EncParams.Format    = M4ENCODER_kH264;
            break;
        default :
            M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: unknown format 0x%x returning\
                           ERR_M4PTO3GPP_UNDEFINED_OUTPUT_VIDEO_FORMAT",
                           pC->m_Params.OutputVideoFormat);
            return ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_FORMAT;
    }

    /**
     * Video frame rate (set it to max = 30 fps) */
    EncParams.uiTimeScale = 30;
    EncParams.uiRateFactor = 1;

    EncParams.FrameRate = M4ENCODER_k30_FPS;


    /******************************/
    /******** 3GP out init ********/
    /******************************/

    /* Get the 3GPP writer interface */
    err = M4WRITER_3GP_getInterfaces(&outputFileType, &pC->m_pWriterGlobInt, &pC->m_pWriterDataInt);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("M4WRITER_3GP_getInterfaces: M4WRITER_3GP_getInterfaces returns 0x%x", err);
        return err;
    }

    /* Init the 3GPP writer */
    err = pC->m_pWriterGlobInt->pFctOpen(&pC->m_p3gpWriterContext, pC->m_Params.pOutput3gppFile,
        pC->pOsalFileWrite, pC->m_Params.pTemporaryFile, pC->pOsalFileRead);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: pWriterGlobInt->pFctOpen returns 0x%x", err);
        return err;
    }

    /**
     *  Link to the writer context in the writer interface */
    pC->m_pWriterDataInt->pWriterContext = pC->m_p3gpWriterContext;

    /**
     *  Set the product description string in the written file */
    err = pC->m_pWriterGlobInt->pFctSetOption(pC->m_p3gpWriterContext, M4WRITER_kEmbeddedString,
        (M4OSA_DataOption)M4PTO3GPP_SIGNATURE);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: \
                       pWriterGlobInt->pFctSetOption(M4WRITER_kEmbeddedString) returns 0x%x", err);
        return err;
    }

    /**
     *  Set the product version in the written file */
    uiVersion = M4VIDEOEDITING_VERSION_MAJOR*100 + M4VIDEOEDITING_VERSION_MINOR*10
        + M4VIDEOEDITING_VERSION_REVISION;
    err = pC->m_pWriterGlobInt->pFctSetOption(pC->m_p3gpWriterContext, M4WRITER_kEmbeddedVersion,
        (M4OSA_DataOption)&uiVersion);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: \
                       pWriterGlobInt->pFctSetOption(M4WRITER_kEmbeddedVersion) returns 0x%x", err);
        return err;
    }

    /**
     *  Allocate and fill the video stream structures for the writer */
    pC->m_pWriterVideoStream =
        (M4SYS_StreamDescription*)M4OSA_32bitAlignedMalloc(sizeof(M4SYS_StreamDescription), M4PTO3GPP,
        (M4OSA_Char *)"pWriterVideoStream");
    if (M4OSA_NULL == pC->m_pWriterVideoStream)
    {
        M4OSA_TRACE1_0("M4PTO3GPP_Ready4Processing(): unable to allocate pWriterVideoStream, \
                       returning M4ERR_ALLOC");
        return M4ERR_ALLOC;
    }
    pC->m_pWriterVideoStreamInfo =
        (M4WRITER_StreamVideoInfos*)M4OSA_32bitAlignedMalloc(sizeof(M4WRITER_StreamVideoInfos), M4PTO3GPP,
        (M4OSA_Char *)"pWriterVideoStreamInfo");
    if (M4OSA_NULL == pC->m_pWriterVideoStreamInfo)
    {
        M4OSA_TRACE1_0("M4PTO3GPP_Ready4Processing(): unable to allocate pWriterVideoStreamInfo,\
                       returning M4ERR_ALLOC");
        return M4ERR_ALLOC;
    }

    /**
     * Fill Video properties structure for the AddStream method */
    pC->m_pWriterVideoStreamInfo->height        = EncParams.FrameHeight;
    pC->m_pWriterVideoStreamInfo->width         = EncParams.FrameWidth;
    pC->m_pWriterVideoStreamInfo->fps           = 0;        /**< Not used by the core writer */
    pC->m_pWriterVideoStreamInfo->Header.pBuf   = M4OSA_NULL;
    /** No header, will be set by setOption */
    pC->m_pWriterVideoStreamInfo->Header.Size   = 0;

    /**
     *  Fill Video stream description structure for the AddStream method */
    pC->m_pWriterVideoStream->streamID = M4PTO3GPP_WRITER_VIDEO_STREAM_ID;

    /**
     * Video format */
    switch(pC->m_Params.OutputVideoFormat)
    {
        case M4VIDEOEDITING_kMPEG4:
            pC->m_pWriterVideoStream->streamType = M4SYS_kMPEG_4;   break;
        case M4VIDEOEDITING_kH263:
            pC->m_pWriterVideoStream->streamType = M4SYS_kH263;     break;
        case M4VIDEOEDITING_kH264:
            pC->m_pWriterVideoStream->streamType = M4SYS_kH264;     break;
        default :
            M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: unknown format 0x%x returning \
                           ERR_M4PTO3GPP_UNDEFINED_OUTPUT_VIDEO_FORMAT",
                           pC->m_Params.OutputVideoFormat);
            return ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_FORMAT;
    }

    /**
     * Video bitrate */
    switch(pC->m_Params.OutputVideoBitrate)
    {
        case M4VIDEOEDITING_k16_KBPS:
        case M4VIDEOEDITING_k24_KBPS:
        case M4VIDEOEDITING_k32_KBPS:
        case M4VIDEOEDITING_k48_KBPS:
        case M4VIDEOEDITING_k64_KBPS:
        case M4VIDEOEDITING_k96_KBPS:
        case M4VIDEOEDITING_k128_KBPS:
        case M4VIDEOEDITING_k192_KBPS:
        case M4VIDEOEDITING_k256_KBPS:
        case M4VIDEOEDITING_k288_KBPS:
        case M4VIDEOEDITING_k384_KBPS:
        case M4VIDEOEDITING_k512_KBPS:
        case M4VIDEOEDITING_k800_KBPS:
/*+ New Encoder bitrates */
        case M4VIDEOEDITING_k2_MBPS:
        case M4VIDEOEDITING_k5_MBPS:
        case M4VIDEOEDITING_k8_MBPS:
/*- New Encoder bitrates */
            pC->m_pWriterVideoStream->averageBitrate = pC->m_Params.OutputVideoBitrate;
            break;

        case M4VIDEOEDITING_kVARIABLE_KBPS :
            pC->m_pWriterVideoStream->averageBitrate = 0;
            break;

        default :
            M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: unknown format 0x%x returning\
                           ERR_M4PTO3GPP_UNDEFINED_OUTPUT_VIDEO_BITRATE",
                           pC->m_Params.OutputVideoBitrate);
            return ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_BITRATE;
    }

    pC->m_pWriterVideoStream->duration                  = 0;        /**< Duration is not known */
    pC->m_pWriterVideoStream->timeScale                 = 0;    /**< Not used by the core writer */
    pC->m_pWriterVideoStream->maxBitrate                = pC->m_pWriterVideoStream->averageBitrate;
    pC->m_pWriterVideoStream->profileLevel              = 0;    /**< Not used by the core writer */
    pC->m_pWriterVideoStream->decoderSpecificInfo       = (M4OSA_MemAddr32)
                                                            (pC->m_pWriterVideoStreamInfo);
    pC->m_pWriterVideoStream->decoderSpecificInfoSize   = sizeof(M4WRITER_StreamVideoInfos);

    /**
     * Update AU properties for video stream */
    pC->m_WriterVideoAU.CTS         = pC->m_WriterVideoAU.DTS = 0;  /** Reset time */
    pC->m_WriterVideoAU.size        = 0;
    pC->m_WriterVideoAU.frag        = M4OSA_NULL;
    pC->m_WriterVideoAU.nbFrag      = 0;                            /** No fragment */
    pC->m_WriterVideoAU.stream      = pC->m_pWriterVideoStream;
    pC->m_WriterVideoAU.attribute   = AU_RAP;
    pC->m_WriterVideoAU.dataAddress = M4OSA_NULL;

    /**
     *  If there is an audio input, allocate and fill the audio stream structures for the writer */
    if(M4OSA_NULL != pC->m_pReaderAudioStream)
    {
        pC->m_pWriterAudioStream =
            (M4SYS_StreamDescription*)M4OSA_32bitAlignedMalloc(sizeof(M4SYS_StreamDescription), M4PTO3GPP,
            (M4OSA_Char *)"pWriterAudioStream");
        if (M4OSA_NULL == pC->m_pWriterAudioStream)
        {
            M4OSA_TRACE1_0("M4PTO3GPP_Ready4Processing(): unable to allocate pWriterAudioStream, \
                           returning M4ERR_ALLOC");
            return M4ERR_ALLOC;
        }
        pC->m_pWriterAudioStreamInfo =
            (M4WRITER_StreamAudioInfos*)M4OSA_32bitAlignedMalloc(sizeof(M4WRITER_StreamAudioInfos), M4PTO3GPP,
            (M4OSA_Char *)"pWriterAudioStreamInfo");
        if (M4OSA_NULL == pC->m_pWriterAudioStreamInfo)
        {
            M4OSA_TRACE1_0("M4PTO3GPP_Ready4Processing(): unable to allocate \
                           pWriterAudioStreamInfo, returning M4ERR_ALLOC");
            return M4ERR_ALLOC;
        }

        pC->m_pWriterAudioStreamInfo->nbSamplesPerSec = 0; /**< unused by our shell writer */
        pC->m_pWriterAudioStreamInfo->nbBitsPerSample = 0; /**< unused by our shell writer */
        pC->m_pWriterAudioStreamInfo->nbChannels = 1;      /**< unused by our shell writer */

        if( (M4OSA_NULL != pC->m_pReaderAudioStream) && /* audio could have been discarded */
            (M4OSA_NULL != pC->m_pReaderAudioStream->m_basicProperties.m_pDecoderSpecificInfo) )
        {
            /* If we copy the stream from the input, we copy its DSI */
            pC->m_pWriterAudioStreamInfo->Header.Size =
                pC->m_pReaderAudioStream->m_basicProperties.m_decoderSpecificInfoSize;
            pC->m_pWriterAudioStreamInfo->Header.pBuf =
                (M4OSA_MemAddr8)pC->m_pReaderAudioStream->m_basicProperties.m_pDecoderSpecificInfo;
        }
        else
        {
            /* Writer will put a default DSI */
            pC->m_pWriterAudioStreamInfo->Header.Size = 0;
            pC->m_pWriterAudioStreamInfo->Header.pBuf = M4OSA_NULL;
        }

        /**
         * Add the audio stream */
        switch (pC->m_pReaderAudioStream->m_basicProperties.m_streamType)
        {
            case M4DA_StreamTypeAudioAmrNarrowBand:
                pC->m_pWriterAudioStream->streamType = M4SYS_kAMR;
                break;
            case M4DA_StreamTypeAudioAac:
                pC->m_pWriterAudioStream->streamType = M4SYS_kAAC;
                break;
            case M4DA_StreamTypeAudioEvrc:
                pC->m_pWriterAudioStream->streamType = M4SYS_kEVRC;
                break;
            default:
                M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: unhandled audio format (0x%x),\
                               returning ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_BITRATE",
                               pC->m_pReaderAudioStream->m_basicProperties.m_streamType);
                return ERR_PTO3GPP_UNDEFINED_OUTPUT_AUDIO_FORMAT;
        }

        /*
         * Fill Audio stream description structure for the AddStream method */
        pC->m_pWriterAudioStream->streamID                  = M4PTO3GPP_WRITER_AUDIO_STREAM_ID;
        pC->m_pWriterAudioStream->duration                  = 0;/**< Duration is not known yet */
        pC->m_pWriterAudioStream->timeScale                 = M4PTO3GPP_WRITER_AUDIO_AMR_TIME_SCALE;
        pC->m_pWriterAudioStream->profileLevel              = M4PTO3GPP_WRITER_AUDIO_PROFILE_LEVEL;
        pC->m_pWriterAudioStream->averageBitrate            =
                                pC->m_pReaderAudioStream->m_basicProperties.m_averageBitRate;
        pC->m_pWriterAudioStream->maxBitrate                =
                                pC->m_pWriterAudioStream->averageBitrate;

        /**
         * Our writer shell interface is a little tricky: we put M4WRITER_StreamAudioInfos \
            in the DSI pointer... */
        pC->m_pWriterAudioStream->decoderSpecificInfo =
                    (M4OSA_MemAddr32)pC->m_pWriterAudioStreamInfo;

        /**
         * Update AU properties for audio stream */
        pC->m_WriterAudioAU.CTS         = pC->m_WriterAudioAU.DTS = 0;  /** Reset time */
        pC->m_WriterAudioAU.size        = 0;
        pC->m_WriterAudioAU.frag        = M4OSA_NULL;
        pC->m_WriterAudioAU.nbFrag      = 0;                            /** No fragment */
        pC->m_WriterAudioAU.stream      = pC->m_pWriterAudioStream;
        pC->m_WriterAudioAU.attribute   = AU_RAP;
        pC->m_WriterAudioAU.dataAddress = M4OSA_NULL;
    }

    /************************************/
    /******** Video Encoder Init ********/
    /************************************/

    /**
     * PTO uses its own bitrate regulation, not the "true" core regulation */
    EncParams.bInternalRegulation = M4OSA_TRUE; //M4OSA_FALSE;
    EncParams.uiStartingQuantizerValue = M4PTO3GPP_QUANTIZER_STEP;

    EncParams.videoProfile = pC->m_Params.videoProfile;
    EncParams.videoLevel = pC->m_Params.videoLevel;

    /**
     * Other encoder settings */

    EncParams.uiHorizontalSearchRange  = 0;             /* use default */
    EncParams.uiVerticalSearchRange    = 0;             /* use default */
    EncParams.bErrorResilience         = M4OSA_FALSE;   /* no error resilience */
    EncParams.uiIVopPeriod             = 15;             /* use default */
    EncParams.uiMotionEstimationTools  = 0;             /* M4V_MOTION_EST_TOOLS_ALL */
    EncParams.bAcPrediction            = M4OSA_TRUE;    /* use AC prediction */
    EncParams.bDataPartitioning        = M4OSA_FALSE;   /* no data partitioning */


    /**
     * Create video encoder */
    err = pC->m_pEncoderInt->pFctInit(&pC->m_pMp4EncoderContext, pC->m_pWriterDataInt,
                                    M4PTO3GPP_applyVPP, pC, pC->m_pEncoderExternalAPI,
                                    pC->m_pEncoderUserData);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: EncoderInt->pFctInit returns 0x%x", err);
        return err;
    }

    pC->m_eEncoderState = M4PTO3GPP_kEncoderClosed;

    err = pC->m_pEncoderInt->pFctOpen(pC->m_pMp4EncoderContext, &pC->m_WriterVideoAU, &EncParams);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: EncoderInt->pFctOpen returns 0x%x", err);
        return err;
    }

    pC->m_eEncoderState = M4PTO3GPP_kEncoderStopped;

    if (M4OSA_NULL != pC->m_pEncoderInt->pFctStart)
    {
        err = pC->m_pEncoderInt->pFctStart(pC->m_pMp4EncoderContext);

        if (M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: EncoderInt->pFctStart returns 0x%x", err);
            return err;
        }
    }

    pC->m_eEncoderState = M4PTO3GPP_kEncoderRunning;

    /**
     * No more  setoption on "M4ENCODER_kVideoFragmentSize" here.
     * It is now automaticly and "smartly" set in the encoder shell. */

    /**************************************/
    /******** 3GP out add streams  ********/
    /**************************************/

    err = pC->m_pWriterGlobInt->pFctAddStream(pC->m_p3gpWriterContext, pC->m_pWriterVideoStream);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: pWriterGlobInt->pFctAddStream(video) returns\
                       0x%x", err);
        return err;
    }

    /**
     * Set video max au size */
    optionValue.streamID    = M4PTO3GPP_WRITER_VIDEO_STREAM_ID;
    optionValue.value = (M4OSA_UInt32)(1.5F * (M4OSA_Float)(pC->m_pWriterVideoStreamInfo->width
                                                * pC->m_pWriterVideoStreamInfo->height)
                                                * M4PTO3GPP_VIDEO_MIN_COMPRESSION_RATIO);
    M4OSA_TRACE3_1("M4PTO3GPP_Ready4Processing,M4WRITER_kMaxAUSize: %u",optionValue.value);
    err = pC->m_pWriterGlobInt->pFctSetOption(pC->m_p3gpWriterContext,
                                (M4OSA_UInt32)M4WRITER_kMaxAUSize,(M4OSA_DataOption) &optionValue);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: pWriterGlobInt->pFctSetOption(video,\
                       M4WRITER_kMaxAUSize) returns 0x%x", err);
        return err;
    }

    /**
     * Set video max chunck size */
    optionValue.value = (M4OSA_UInt32)((M4OSA_Float)optionValue.value
                        * M4PTO3GPP_VIDEO_AU_SIZE_TO_CHUNCK_SIZE_RATIO);
    M4OSA_TRACE3_1("M4PTO3GPP_Ready4Processing,M4WRITER_kMaxChunckSize: %u",optionValue.value);
    err = pC->m_pWriterGlobInt->pFctSetOption(pC->m_p3gpWriterContext,
                        (M4OSA_UInt32)M4WRITER_kMaxChunckSize,(M4OSA_DataOption) &optionValue);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: pWriterGlobInt->pFctSetOption(video,\
                       M4WRITER_kMaxChunckSize) returns 0x%x", err);
        return err;
    }

    if (M4OSA_NULL != pC->m_pReaderAudioStream)
    {
        err = pC->m_pWriterGlobInt->pFctAddStream(pC->m_p3gpWriterContext, pC->m_pWriterAudioStream);
        if (M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: pWriterGlobInt->pFctAddStream(audio) \
                           returns 0x%x", err);
            return err;
        }

        /**
         * Set audio max au size */
        optionValue.value       = M4PTO3GPP_AUDIO_MAX_AU_SIZE;
        optionValue.streamID    = M4PTO3GPP_WRITER_AUDIO_STREAM_ID;
        err = pC->m_pWriterGlobInt->pFctSetOption(pC->m_p3gpWriterContext,
            (M4OSA_UInt32)M4WRITER_kMaxAUSize,(M4OSA_DataOption) &optionValue);
        if (M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: pWriterGlobInt->pFctSetOption(audio,\
                           M4WRITER_kMaxAUSize) returns 0x%x", err);
            return err;
        }

        /**
         * Set audio max chunck size */
        optionValue.value = M4PTO3GPP_AUDIO_MAX_CHUNK_SIZE; /**< Magical */
        err = pC->m_pWriterGlobInt->pFctSetOption(pC->m_p3gpWriterContext,
                        (M4OSA_UInt32)M4WRITER_kMaxChunckSize,(M4OSA_DataOption) &optionValue);
        if (M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: pWriterGlobInt->pFctSetOption(audio,\
                           M4WRITER_kMaxChunckSize) returns 0x%x", err);
            return err;
        }
    }

    /*
     * Close the stream registering in order to be ready to write data */
    err = pC->m_pWriterGlobInt->pFctStartWriting(pC->m_p3gpWriterContext);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("M4PTO3GPP_Ready4Processing: pWriterGlobInt->pFctStartWriting returns 0x%x",
                        err);
        return err;
    }


    M4OSA_TRACE3_0("M4PTO3GPP_Ready4Processing: returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 M4OSA_ERR M4PTO3GPP_writeAmrSilence122Frame(M4WRITER_DataInterface* pWriterDataIntInterface,
                            M4WRITER_Context* pWriterContext,
                                      M4SYS_AccessUnit* pWriterAudioAU, M4OSA_Time mtIncCts)
 * @brief   Write an AMR 12.2kbps silence FRAME into the writer
 * @note    Mainly used to fix the 'bzz' bug...
 * @param   pWriterDataIntInterface (IN)    writer data interfaces
 *          pWriterContext          (IN/OUT)writer context
 *          pWriterAudioAU          (OUT)   writer audio access unit
 *          mtIncCts                (IN)    writer CTS
 * @return  M4NO_ERROR: No error
 ******************************************************************************
*/
static M4OSA_ERR M4PTO3GPP_writeAmrSilence122Frame(M4WRITER_DataInterface* pWriterDataIntInterface,
                                                   M4WRITER_Context* pWriterContext,
                                                    M4SYS_AccessUnit* pWriterAudioAU,
                                                    M4OSA_Time mtIncCts)
{
    M4OSA_ERR err;

    err = pWriterDataIntInterface->pStartAU(pWriterContext, M4PTO3GPP_WRITER_AUDIO_STREAM_ID,
                                        pWriterAudioAU);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("M4PTO3GPP_writeAmrSilence122Frame: pWriterDataInt->pStartAU(audio) returns \
                                                    0x%x!", err);
        return err;
    }

    memcpy((void *)pWriterAudioAU->dataAddress,
     (void *)M4PTO3GPP_AMR_AU_SILENCE_122_FRAME, M4PTO3GPP_AMR_AU_SILENCE_FRAME_122_SIZE);
    pWriterAudioAU->size    = M4PTO3GPP_AMR_AU_SILENCE_FRAME_122_SIZE;
    pWriterAudioAU->CTS     = mtIncCts;
    pWriterAudioAU->nbFrag  = 0;

    err = pWriterDataIntInterface->pProcessAU(pWriterContext, M4PTO3GPP_WRITER_AUDIO_STREAM_ID,
                                                pWriterAudioAU);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("M4PTO3GPP_writeAmrSilence122Frame: pWriterDataInt->pProcessAU(silence) \
                       returns 0x%x!", err);
        return err;
    }

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 M4OSA_ERR M4PTO3GPP_writeAmrSilence048Frame(M4WRITER_DataInterface* pWriterDataIntInterface,
                                        M4WRITER_Context* pWriterContext,
                                      M4SYS_AccessUnit* pWriterAudioAU, M4OSA_Time mtIncCts)
 * @brief   Write an AMR 12.2kbps silence FRAME into the writer
 * @note    Mainly used to fix the 'bzz' bug...
 * @param   pWriterDataIntInterface (IN)    writer data interfaces
 *          pWriterContext          (IN/OUT)writer context
 *          pWriterAudioAU          (OUT)   writer audio access unit
 *          mtIncCts                (IN)    writer CTS
 * @return  M4NO_ERROR: No error
 ******************************************************************************
*/
static M4OSA_ERR M4PTO3GPP_writeAmrSilence048Frame(M4WRITER_DataInterface* pWriterDataIntInterface,
                                                   M4WRITER_Context* pWriterContext,
                                                M4SYS_AccessUnit* pWriterAudioAU,
                                                M4OSA_Time mtIncCts)
{
    M4OSA_ERR err;

    err = pWriterDataIntInterface->pStartAU(pWriterContext, M4PTO3GPP_WRITER_AUDIO_STREAM_ID,
                                                        pWriterAudioAU);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("M4PTO3GPP_writeAmrSilence048Frame: pWriterDataInt->pStartAU(audio)\
                       returns 0x%x!", err);
        return err;
    }

    memcpy((void *)pWriterAudioAU->dataAddress,
                (void *)M4PTO3GPP_AMR_AU_SILENCE_048_FRAME,
                M4PTO3GPP_AMR_AU_SILENCE_FRAME_048_SIZE);
    pWriterAudioAU->size    = M4PTO3GPP_AMR_AU_SILENCE_FRAME_048_SIZE;
    pWriterAudioAU->CTS     = mtIncCts;
    pWriterAudioAU->nbFrag  = 0;

    err = pWriterDataIntInterface->pProcessAU(pWriterContext,
                    M4PTO3GPP_WRITER_AUDIO_STREAM_ID, pWriterAudioAU);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("M4PTO3GPP_writeAmrSilence048Frame: \
                       pWriterDataInt->pProcessAU(silence) returns 0x%x!", err);
        return err;
    }

    return M4NO_ERROR;
}


