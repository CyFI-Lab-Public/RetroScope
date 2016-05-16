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
 * @file    M4VSS3GPP_Clip.c
 * @brief    Implementation of functions related to input clip management.
 * @note    All functions in this file are static, i.e. non public
 ******************************************************************************
 */

/****************/
/*** Includes ***/
/****************/

#include "NXPSW_CompilerSwitches.h"
/**
 *    Our headers */
#include "M4VSS3GPP_API.h"
#include "M4VSS3GPP_ErrorCodes.h"
#include "M4VSS3GPP_InternalTypes.h"
#include "M4VSS3GPP_InternalFunctions.h"
#include "M4VSS3GPP_InternalConfig.h"

/**
 *    OSAL headers */
#include "M4OSA_Memory.h" /* OSAL memory management */
#include "M4OSA_Debug.h"  /* OSAL debug management */


/**
 * Common headers (for aac) */
#include "M4_Common.h"

#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS
#include "M4VD_EXTERNAL_Interface.h"

#endif /* M4VSS_ENABLE_EXTERNAL_DECODERS */

/* Osal header fileno */
#include "M4OSA_CharStar.h"

/**
 ******************************************************************************
 * define    Static function prototypes
 ******************************************************************************
 */

static M4OSA_ERR M4VSS3GPP_intClipPrepareAudioDecoder(
    M4VSS3GPP_ClipContext *pClipCtxt );

static M4OSA_ERR M4VSS3GPP_intCheckAndGetCodecAacProperties(
        M4VSS3GPP_ClipContext *pClipCtxt);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intClipOpen()
 * @brief    Open a clip. Creates a clip context.
 * @note
 * @param   hClipCtxt            (OUT) Return the internal clip context
 * @param   pClipSettings        (IN) Edit settings of this clip. The module will keep a
 *                               reference to this pointer
 * @param    pFileReadPtrFct        (IN) Pointer to OSAL file reader functions
 * @param    bSkipAudioTrack        (IN) If true, do not open the audio
 * @param    bFastOpenMode        (IN) If true, use the fast mode of the 3gpp reader
 *                             (only the first AU is read)
 * @return    M4NO_ERROR:                No error
 * @return    M4ERR_ALLOC:            There is no more available memory
 ******************************************************************************
 */

M4OSA_ERR M4VSS3GPP_intClipInit( M4VSS3GPP_ClipContext ** hClipCtxt,
                                M4OSA_FileReadPointer *pFileReadPtrFct )
{
    M4VSS3GPP_ClipContext *pClipCtxt;
    M4OSA_ERR err;

    M4OSA_DEBUG_IF2((M4OSA_NULL == hClipCtxt), M4ERR_PARAMETER,
        "M4VSS3GPP_intClipInit: hClipCtxt is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pFileReadPtrFct), M4ERR_PARAMETER,
        "M4VSS3GPP_intClipInit: pFileReadPtrFct is M4OSA_NULL");

    /**
    * Allocate the clip context */
    *hClipCtxt =
        (M4VSS3GPP_ClipContext *)M4OSA_32bitAlignedMalloc(sizeof(M4VSS3GPP_ClipContext),
        M4VSS3GPP, (M4OSA_Char *)"M4VSS3GPP_ClipContext");

    if( M4OSA_NULL == *hClipCtxt )
    {
        M4OSA_TRACE1_0(
            "M4VSS3GPP_intClipInit(): unable to allocate M4VSS3GPP_ClipContext,\
            returning M4ERR_ALLOC");
        return M4ERR_ALLOC;
    }
    M4OSA_TRACE3_1("M4VSS3GPP_intClipInit(): clipCtxt=0x%x", *hClipCtxt);


    /**
    * Use this shortcut to simplify the code */
    pClipCtxt = *hClipCtxt;

    /* Inialization of context Variables */
    memset((void *)pClipCtxt, 0,sizeof(M4VSS3GPP_ClipContext));

    pClipCtxt->pSettings = M4OSA_NULL;

    /**
    * Init the clip context */
    pClipCtxt->iVoffset = 0;
    pClipCtxt->iAoffset = 0;
    pClipCtxt->Vstatus = M4VSS3GPP_kClipStatus_READ;
    pClipCtxt->Astatus = M4VSS3GPP_kClipStatus_READ;

    pClipCtxt->pReaderContext = M4OSA_NULL;
    pClipCtxt->pVideoStream = M4OSA_NULL;
    pClipCtxt->pAudioStream = M4OSA_NULL;
    pClipCtxt->VideoAU.m_dataAddress = M4OSA_NULL;
    pClipCtxt->AudioAU.m_dataAddress = M4OSA_NULL;

    pClipCtxt->pViDecCtxt = M4OSA_NULL;
    pClipCtxt->iVideoDecCts = 0;
    pClipCtxt->iVideoRenderCts = 0;
    pClipCtxt->lastDecodedPlane = M4OSA_NULL;
    pClipCtxt->iActualVideoBeginCut = 0;
    pClipCtxt->iActualAudioBeginCut = 0;
    pClipCtxt->bVideoAuAvailable = M4OSA_FALSE;
    pClipCtxt->bFirstAuWritten = M4OSA_FALSE;

    pClipCtxt->bMpeg4GovState = M4OSA_FALSE;

    pClipCtxt->bAudioFrameAvailable = M4OSA_FALSE;
    pClipCtxt->pAudioFramePtr = M4OSA_NULL;
    pClipCtxt->iAudioFrameCts = 0;
    pClipCtxt->pAudioDecCtxt = 0;
    pClipCtxt->AudioDecBufferOut.m_bufferSize = 0;
    pClipCtxt->AudioDecBufferOut.m_dataAddress = M4OSA_NULL;

    pClipCtxt->pFileReadPtrFct = pFileReadPtrFct;
    pClipCtxt->pPlaneYuv   = M4OSA_NULL;
    pClipCtxt->pPlaneYuvWithEffect = M4OSA_NULL;
    pClipCtxt->m_pPreResizeFrame = M4OSA_NULL;
    pClipCtxt->bGetYuvDataFromDecoder = M4OSA_TRUE;

    /*
    * Reset pointers for media and codecs interfaces */
    err = M4VSS3GPP_clearInterfaceTables(&pClipCtxt->ShellAPI);
    M4ERR_CHECK_RETURN(err);

    /*
    *  Call the media and codecs subscription module */
    err = M4VSS3GPP_subscribeMediaAndCodec(&pClipCtxt->ShellAPI);
    M4ERR_CHECK_RETURN(err);

    return M4NO_ERROR;
}

// This method maps the frequency value to a string.
static const char* freqToString(int freq) {
    switch (freq) {
    case 8000:
        return "_8000";
    case 11025:
        return "_11025";
    case 12000:
        return "_12000";
    case 16000:
        return "_16000";
    case 22050:
        return "_22050";
    case 24000:
        return "_24000";
    case 32000:
        return "_32000";
    case 44100:
        return "_44100";
    case 48000:
        return "_48000";
    default:
        M4OSA_TRACE1_1("Unsupported sampling rate: %d Hz", freq);
        return NULL;
    }
}

// This method maps the number of channel value to
// a string that will be part of a file name extension
static const char* channelToStringAndFileExt(int channels) {
    switch (channels) {
    case 1:
        return "_1.pcm";
    case 2:
        return "_2.pcm";
    default:
        M4OSA_TRACE1_1("Unsupported %d channels", channels);
        return NULL;
    }
}

/* Note: if the clip is opened in fast mode, it can only be used for analysis and nothing else. */
M4OSA_ERR M4VSS3GPP_intClipOpen( M4VSS3GPP_ClipContext *pClipCtxt,
                                M4VSS3GPP_ClipSettings *pClipSettings, M4OSA_Bool bSkipAudioTrack,
                                M4OSA_Bool bFastOpenMode, M4OSA_Bool bAvoidOpeningVideoDec )
{
    M4OSA_ERR err;
    M4READER_MediaFamily mediaFamily;
    M4_StreamHandler *pStreamHandler;
    M4_StreamHandler  dummyStreamHandler;
    M4OSA_Int32 iDuration;
    M4OSA_Void *decoderUserData;
#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS

    M4DECODER_MPEG4_DecoderConfigInfo dummy;
    M4DECODER_VideoSize videoSizeFromDSI;
#endif /* M4VSS_ENABLE_EXTERNAL_DECODERS */

    M4DECODER_OutputFilter FilterOption;

    /**
    *    Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pClipCtxt), M4ERR_PARAMETER,
        "M4VSS3GPP_intClipOpen: pClipCtxt is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pClipSettings), M4ERR_PARAMETER,
        "M4VSS3GPP_intClipOpen: pClipSettings is M4OSA_NULL");

    M4OSA_TRACE3_2(
        "M4VSS3GPP_intClipOpen: called with pClipCtxt: 0x%x, bAvoidOpeningVideoDec=0x%x",
        pClipCtxt, bAvoidOpeningVideoDec);
    /**
    * Keep a pointer to the clip settings. Remember that we don't possess it! */
    pClipCtxt->pSettings = pClipSettings;
    if(M4VIDEOEDITING_kFileType_ARGB8888 == pClipCtxt->pSettings->FileType) {
        M4OSA_TRACE3_0("M4VSS3GPP_intClipOpen: Image stream; set current vid dec");
        err = M4VSS3GPP_setCurrentVideoDecoder(
                  &pClipCtxt->ShellAPI, M4DA_StreamTypeVideoARGB8888);
        M4ERR_CHECK_RETURN(err);

        decoderUserData = M4OSA_NULL;

        err = pClipCtxt->ShellAPI.m_pVideoDecoder->m_pFctCreate(
                  &pClipCtxt->pViDecCtxt,
                  &dummyStreamHandler,
                  pClipCtxt->ShellAPI.m_pReader,
                  pClipCtxt->ShellAPI.m_pReaderDataIt,
                  &pClipCtxt->VideoAU,
                  decoderUserData);

        if (M4NO_ERROR != err) {
            M4OSA_TRACE1_1("M4VSS3GPP_intClipOpen: \
                m_pVideoDecoder->m_pFctCreate returns 0x%x", err);
            return err;
        }
        M4OSA_TRACE3_1("M4VSS3GPP_intClipOpen: \
            Vid dec started; pViDecCtxt=0x%x", pClipCtxt->pViDecCtxt);

        return M4NO_ERROR;
    }

    /**
    * Get the correct reader interface */
    err = M4VSS3GPP_setCurrentReader(&pClipCtxt->ShellAPI,
        pClipCtxt->pSettings->FileType);
    M4ERR_CHECK_RETURN(err);

    /**
    * Init the 3GPP or MP3 reader */
    err =
        pClipCtxt->ShellAPI.m_pReader->m_pFctCreate(&pClipCtxt->pReaderContext);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intClipOpen(): m_pReader->m_pFctCreate returns 0x%x",
            err);
        return err;
    }

    /**
    * Link the reader interface to the reader context (used by the decoder to know the reader) */
    pClipCtxt->ShellAPI.m_pReaderDataIt->m_readerContext =
        pClipCtxt->pReaderContext;

    /**
    * Set the OSAL read function set */
    err = pClipCtxt->ShellAPI.m_pReader->m_pFctSetOption(
        pClipCtxt->pReaderContext,
        M4READER_kOptionID_SetOsaFileReaderFctsPtr,
        (M4OSA_DataOption)(pClipCtxt->pFileReadPtrFct));

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intClipOpen(): m_pReader->m_pFctSetOption returns 0x%x",
            err);
        return err;
    }

    /**
    * Set the fast open mode if asked (3GPP only) */
    if( M4VIDEOEDITING_kFileType_3GPP == pClipCtxt->pSettings->FileType )
    {
        if( M4OSA_TRUE == bFastOpenMode )
        {
            err = pClipCtxt->ShellAPI.m_pReader->m_pFctSetOption(
                pClipCtxt->pReaderContext,
                M4READER_3GP_kOptionID_FastOpenMode, M4OSA_NULL);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intClipOpen():\
                    m_pReader->m_pFctSetOption(FastOpenMode) returns 0x%x",
                    err);
                return err;
            }
        }

        /**
        * Set the skip audio option if asked */
        if( M4OSA_TRUE == bSkipAudioTrack )
        {
            err = pClipCtxt->ShellAPI.m_pReader->m_pFctSetOption(
                pClipCtxt->pReaderContext,
                M4READER_3GP_kOptionID_VideoOnly, M4OSA_NULL);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intClipOpen(): m_pReader->m_pFctSetOption(VideoOnly) returns 0x%x",
                    err);
                return err;
            }
        }
    }
    if (pClipCtxt->pSettings->FileType == M4VIDEOEDITING_kFileType_PCM) {
        // Compose the temp filename with sample rate and channel information.
        const char* freqStr = freqToString(
                    pClipCtxt->pSettings->ClipProperties.uiSamplingFrequency);

        if (freqStr == NULL) {
            return M4VSS3GPP_WAR_INCOMPATIBLE_AUDIO_SAMPLING_FREQUENCY;
        }

        const char* chanStr = channelToStringAndFileExt(
                    pClipCtxt->pSettings->ClipProperties.uiNbChannels);

        if (chanStr == NULL) {
                return M4VSS3GPP_WAR_INCOMPATIBLE_AUDIO_NB_OF_CHANNELS;
        }

        // Allocate one byte more to hold the null terminator
        M4OSA_UInt32 length =
            strlen(pClipSettings->pFile) + strlen(freqStr) + strlen(chanStr) + 1;

        char* pTempFile = (char *) malloc(length);
        if (pTempFile == NULL) {
            M4OSA_TRACE1_1("M4VSS3GPP_intClipOpen(): malloc %d bytes fail",length);
            return M4ERR_ALLOC;
        }
        memset(pTempFile, 0, length);
        memcpy(pTempFile, pClipSettings->pFile, strlen(pClipSettings->pFile));
        strncat(pTempFile, freqStr, strlen(freqStr));
        strncat(pTempFile, chanStr, strlen(chanStr));

        err = pClipCtxt->ShellAPI.m_pReader->m_pFctOpen( pClipCtxt->pReaderContext, pTempFile);
        if (pTempFile != NULL) {
            free(pTempFile);
            pTempFile = NULL;
        }
        if ( M4NO_ERROR != err ) {
            M4OSA_TRACE1_1("M4VSS3GPP_intClipOpen(): open pcm file returns error : 0x%x", err);
            return err;
        }
    }
    else
    {
    /**
        * Open the 3GPP/MP3 clip file */
        err = pClipCtxt->ShellAPI.m_pReader->m_pFctOpen( pClipCtxt->pReaderContext,
             pClipSettings->pFile);
    }
    if( M4NO_ERROR != err )
    {
        M4OSA_UInt32 uiDummy, uiCoreId;
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intClipOpen(): m_pReader->m_pFctOpen returns 0x%x", err);

        /**
        * If the error is from the core reader, we change it to a public VSS3GPP error */
        M4OSA_ERR_SPLIT(err, uiDummy, uiCoreId, uiDummy);

        if( M4MP4_READER == uiCoreId )
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_intClipOpen(): returning M4VSS3GPP_ERR_INVALID_3GPP_FILE");
            return M4VSS3GPP_ERR_INVALID_3GPP_FILE;
        }
        return err;
    }

    /**
    * Get the audio and video streams */
    while( err == M4NO_ERROR )
    {
        err = pClipCtxt->ShellAPI.m_pReader->m_pFctGetNextStream(
            pClipCtxt->pReaderContext, &mediaFamily, &pStreamHandler);

        /*in case we found a BIFS stream or something else...*/
        if( ( err == ((M4OSA_UInt32)M4ERR_READER_UNKNOWN_STREAM_TYPE))
            || (err == ((M4OSA_UInt32)M4WAR_TOO_MUCH_STREAMS)) )
        {
            err = M4NO_ERROR;
            continue;
        }

        if( M4NO_ERROR == err ) /**< One stream found */
        {
            /**
            * Found a video stream */
            if( ( mediaFamily == M4READER_kMediaFamilyVideo)
                && (M4OSA_NULL == pClipCtxt->pVideoStream) )
            {
                if( ( M4DA_StreamTypeVideoH263 == pStreamHandler->m_streamType)
                    || (M4DA_StreamTypeVideoMpeg4
                    == pStreamHandler->m_streamType)
                    || (M4DA_StreamTypeVideoMpeg4Avc
                    == pStreamHandler->m_streamType) )
                {
                    M4OSA_TRACE3_1(
                        "M4VSS3GPP_intClipOpen():\
                        Found a H263 or MPEG-4 or H264 video stream in input 3gpp clip; %d",
                        pStreamHandler->m_streamType);

                    /**
                    * Keep pointer to the video stream */
                    pClipCtxt->pVideoStream =
                        (M4_VideoStreamHandler *)pStreamHandler;
                    pStreamHandler->m_bStreamIsOK = M4OSA_TRUE;

                    /**
                    * Reset the stream reader */
                    err = pClipCtxt->ShellAPI.m_pReader->m_pFctReset(
                        pClipCtxt->pReaderContext,
                        (M4_StreamHandler *)pClipCtxt->pVideoStream);

                    if( M4NO_ERROR != err )
                    {
                        M4OSA_TRACE1_1(
                            "M4VSS3GPP_intClipOpen(): m_pReader->m_pFctReset(video) returns 0x%x",
                            err);
                        return err;
                    }

                    /**
                    * Initializes an access Unit */
                    err = pClipCtxt->ShellAPI.m_pReader->m_pFctFillAuStruct(
                        pClipCtxt->pReaderContext,
                        (M4_StreamHandler *)pClipCtxt->pVideoStream,
                        &pClipCtxt->VideoAU);

                    if( M4NO_ERROR != err )
                    {
                        M4OSA_TRACE1_1(
                            "M4VSS3GPP_intClipOpen():\
                            m_pReader->m_pFctFillAuStruct(video) returns 0x%x",
                            err);
                        return err;
                    }
                }
                else /**< Not H263 or MPEG-4 (H264, etc.) */
                {
                    M4OSA_TRACE1_1(
                        "M4VSS_editClipOpen():\
                        Found an unsupported video stream (0x%x) in input 3gpp clip",
                        pStreamHandler->m_streamType);

                    pStreamHandler->m_bStreamIsOK = M4OSA_FALSE;
                }
            }
            /**
            * Found an audio stream */
            else if( ( mediaFamily == M4READER_kMediaFamilyAudio)
                && (M4OSA_NULL == pClipCtxt->pAudioStream) )
            {
                if( ( M4DA_StreamTypeAudioAmrNarrowBand
                    == pStreamHandler->m_streamType)
                    || (M4DA_StreamTypeAudioAac == pStreamHandler->m_streamType)
                    || (M4DA_StreamTypeAudioMp3
                    == pStreamHandler->m_streamType)
                    || (M4DA_StreamTypeAudioEvrc
                    == pStreamHandler->m_streamType)
                    || (M4DA_StreamTypeAudioPcm
                    == pStreamHandler->m_streamType) )
                {
                    M4OSA_TRACE3_1(
                        "M4VSS3GPP_intClipOpen(): \
                        Found an AMR-NB or AAC or MP3 audio stream in input clip; %d",
                        pStreamHandler->m_streamType);

                    /**
                    * Keep pointer to the audio stream */
                    pClipCtxt->pAudioStream =
                        (M4_AudioStreamHandler *)pStreamHandler;
                    pStreamHandler->m_bStreamIsOK = M4OSA_TRUE;

                    /**
                    * Reset the stream reader */
                    err = pClipCtxt->ShellAPI.m_pReader->m_pFctReset(
                        pClipCtxt->pReaderContext,
                        (M4_StreamHandler *)pClipCtxt->pAudioStream);

                    if( M4NO_ERROR != err )
                    {
                        M4OSA_TRACE1_1(
                            "M4VSS3GPP_intClipOpen(): m_pReader->m_pFctReset(audio) returns 0x%x",
                            err);
                        return err;
                    }

                    /**
                    * Initializes an access Unit */
                    err = pClipCtxt->ShellAPI.m_pReader->m_pFctFillAuStruct(
                        pClipCtxt->pReaderContext,
                        (M4_StreamHandler *)pClipCtxt->pAudioStream,
                        &pClipCtxt->AudioAU);

                    if( M4NO_ERROR != err )
                    {
                        M4OSA_TRACE1_1(
                            "M4VSS3GPP_intClipOpen():\
                            m_pReader->m_pFctFillAuStruct(audio) returns 0x%x",
                            err);
                        return err;
                    }
                }
                else /**< Not AMR-NB or AAC (AMR-WB...) */
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intClipOpen():\
                        Found an unsupported audio stream (0x%x) in input 3gpp/mp3 clip",
                        pStreamHandler->m_streamType);

                    pStreamHandler->m_bStreamIsOK = M4OSA_FALSE;
                }
            }
        }
        else if( M4OSA_ERR_IS_ERROR(err) )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intClipOpen(): m_pReader->m_pFctGetNextStream() returns 0x%x!",
                err);
            return err;
        }
    }

    /**
    * Init Video decoder */
    if( M4OSA_NULL != pClipCtxt->pVideoStream )
    {
#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS
  /* If external decoders are possible, it's best to avoid opening the decoder if the clip is only
  going to be used for analysis, as we're not going to use it for the analysis in the case of a
  possible external decoder anyway, and either there could be no decoder at this point or the HW
  decoder could be present, which we want to avoid opening for that. See comments in
  intBuildAnalysis for more details. */

  /* CHANGEME Temporarily only do this for MPEG4, since for now only MPEG4 external decoders are
  supported, and the following wouldn't work for H263 so a release where external decoders are
  possible, but not used, wouldn't work with H263 stuff. */

        if( bAvoidOpeningVideoDec && M4DA_StreamTypeVideoMpeg4
            == pClipCtxt->pVideoStream->m_basicProperties.m_streamType )
        {
            /* Oops! The mere act of opening the decoder also results in the image size being
            filled in the video stream! Compensate for this by using ParseVideoDSI to fill
            this info. */
            M4OSA_TRACE3_0(
                "M4VSS3GPP_intClipOpen: Mpeg4 stream; vid dec not started");
            err = M4DECODER_EXTERNAL_ParseVideoDSI(pClipCtxt->pVideoStream->
                m_basicProperties.m_pDecoderSpecificInfo,
                pClipCtxt->pVideoStream->
                m_basicProperties.m_decoderSpecificInfoSize,
                &dummy, &videoSizeFromDSI);

            pClipCtxt->pVideoStream->m_videoWidth = videoSizeFromDSI.m_uiWidth;
            pClipCtxt->pVideoStream->m_videoHeight =
                videoSizeFromDSI.m_uiHeight;
        }
        else
        {

#endif

            M4OSA_TRACE3_0(
                "M4VSS3GPP_intClipOpen: Mp4/H263/H264 stream; set current vid dec");
            err = M4VSS3GPP_setCurrentVideoDecoder(&pClipCtxt->ShellAPI,
                pClipCtxt->pVideoStream->m_basicProperties.m_streamType);
            M4ERR_CHECK_RETURN(err);

#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS

            decoderUserData =
                pClipCtxt->ShellAPI.m_pCurrentVideoDecoderUserData;

#else

            decoderUserData = M4OSA_NULL;

#endif

            err = pClipCtxt->ShellAPI.m_pVideoDecoder->m_pFctCreate(
                &pClipCtxt->pViDecCtxt,
                &pClipCtxt->pVideoStream->m_basicProperties,
                pClipCtxt->ShellAPI.m_pReader,
                pClipCtxt->ShellAPI.m_pReaderDataIt,
                &pClipCtxt->VideoAU, decoderUserData);

            if( ( ((M4OSA_UInt32)M4ERR_DECODER_H263_PROFILE_NOT_SUPPORTED) == err)
                || (((M4OSA_UInt32)M4ERR_DECODER_H263_NOT_BASELINE) == err) )
            {
                /**
                * Our decoder is not compatible with H263 profile other than 0.
                * So it returns this internal error code.
                * We translate it to our own error code */
                return M4VSS3GPP_ERR_H263_PROFILE_NOT_SUPPORTED;
            }
            else if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intClipOpen: m_pVideoDecoder->m_pFctCreate returns 0x%x",
                    err);
                return err;
            }
            M4OSA_TRACE3_1(
                "M4VSS3GPP_intClipOpen: Vid dec started; pViDecCtxt=0x%x",
                pClipCtxt->pViDecCtxt);

            if( M4DA_StreamTypeVideoMpeg4Avc
                == pClipCtxt->pVideoStream->m_basicProperties.m_streamType )
            {
                FilterOption.m_pFilterFunction =
                    (M4OSA_Void *) &M4VIFI_ResizeBilinearYUV420toYUV420;
                FilterOption.m_pFilterUserData = M4OSA_NULL;
                err = pClipCtxt->ShellAPI.m_pVideoDecoder->m_pFctSetOption(
                    pClipCtxt->pViDecCtxt, M4DECODER_kOptionID_OutputFilter,
                    (M4OSA_DataOption) &FilterOption);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intClipOpen: m_pVideoDecoder->m_pFctSetOption returns 0x%x",
                        err);
                    return err;
                }
                else
                {
                    M4OSA_TRACE3_0(
                        "M4VSS3GPP_intClipOpen: m_pVideoDecoder->m_pFctSetOption\
                        M4DECODER_kOptionID_OutputFilter OK");
                }
            }
#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS

        }

#endif

    }

    /**
    * Init Audio decoder */
    if( M4OSA_NULL != pClipCtxt->pAudioStream )
    {
        err = M4VSS3GPP_intClipPrepareAudioDecoder(pClipCtxt);
        M4ERR_CHECK_RETURN(err);
        M4OSA_TRACE3_1("M4VSS3GPP_intClipOpen: Audio dec started; context=0x%x",
            pClipCtxt->pAudioDecCtxt);
    }
    else
    {
        pClipCtxt->AudioAU.m_streamID = 0;
        pClipCtxt->AudioAU.m_dataAddress = M4OSA_NULL;
        pClipCtxt->AudioAU.m_size = 0;
        pClipCtxt->AudioAU.m_CTS = 0;
        pClipCtxt->AudioAU.m_DTS = 0;
        pClipCtxt->AudioAU.m_attribute = 0;
        pClipCtxt->AudioAU.m_maxsize = 0;
        pClipCtxt->AudioAU.m_structSize = sizeof(pClipCtxt->AudioAU);
    }

    /**
    * Get the duration of the longest stream */
    if( M4OSA_TRUE == pClipCtxt->pSettings->ClipProperties.bAnalysed )
    {
        /* If already calculated set it to previous value */
        /* Because fast open and full open can return a different value,
           it can mismatch user settings */
        /* Video track is more important than audio track (if video track is shorter than
           audio track, it can led to cut larger than expected) */
        iDuration = pClipCtxt->pSettings->ClipProperties.uiClipVideoDuration;

        if( iDuration == 0 )
        {
            iDuration = pClipCtxt->pSettings->ClipProperties.uiClipDuration;
        }
    }
    else
    {
        /* Else compute it from streams */
        iDuration = 0;

        if( M4OSA_NULL != pClipCtxt->pVideoStream )
        {
            iDuration = (M4OSA_Int32)(
                pClipCtxt->pVideoStream->m_basicProperties.m_duration);
        }

        if( ( M4OSA_NULL != pClipCtxt->pAudioStream) && ((M4OSA_Int32)(
            pClipCtxt->pAudioStream->m_basicProperties.m_duration)
            > iDuration) && iDuration == 0 )
        {
            iDuration = (M4OSA_Int32)(
                pClipCtxt->pAudioStream->m_basicProperties.m_duration);
        }
    }

    /**
    * If end time is not used, we set it to the video track duration */
    if( 0 == pClipCtxt->pSettings->uiEndCutTime )
    {
        pClipCtxt->pSettings->uiEndCutTime = (M4OSA_UInt32)iDuration;
    }

    pClipCtxt->iEndTime = (M4OSA_Int32)pClipCtxt->pSettings->uiEndCutTime;

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_intClipOpen(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_Void M4VSS3GPP_intClipDeleteAudioTrack()
 * @brief    Delete the audio track. Clip will be like if it had no audio track
 * @note
 * @param   pClipCtxt            (IN) Internal clip context
 ******************************************************************************
 */
M4OSA_Void M4VSS3GPP_intClipDeleteAudioTrack( M4VSS3GPP_ClipContext *pClipCtxt )
{
    /**
    * But we don't have to free the audio stream. It will be freed by the reader when closing it*/
    pClipCtxt->pAudioStream = M4OSA_NULL;

    /**
    * We will return a constant silence AMR AU.
    * We set it here once, instead of at each read audio step. */
    pClipCtxt->pAudioFramePtr = (M4OSA_MemAddr8)pClipCtxt->pSilenceFrameData;
    pClipCtxt->uiAudioFrameSize = pClipCtxt->uiSilenceFrameSize;

    /**
    * Free the decoded audio buffer (it needs to be re-allocated to store silence
      frame eventually)*/
    if( M4OSA_NULL != pClipCtxt->AudioDecBufferOut.m_dataAddress )
    {
        free(pClipCtxt->AudioDecBufferOut.m_dataAddress);
        pClipCtxt->AudioDecBufferOut.m_dataAddress = M4OSA_NULL;
    }

    return;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intClipDecodeVideoUpToCurrentTime()
 * @brief    Jump to the previous RAP and decode up to the current video time
 * @param   pClipCtxt    (IN) Internal clip context
 * @param   iCts        (IN) Target CTS
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_intClipDecodeVideoUpToCts( M4VSS3GPP_ClipContext *pClipCtxt,
                                              M4OSA_Int32 iCts )
{
    M4OSA_Int32 iRapCts, iClipCts;
    M4_MediaTime dDecodeTime;
    M4OSA_Bool bClipJump = M4OSA_FALSE;
    M4OSA_ERR err;

    /**
    * Compute the time in the clip base */
    iClipCts = iCts - pClipCtxt->iVoffset;

    /**
    * If we were reading the clip, we must jump to the previous RAP
    * to decode from that point. */
    if( M4VSS3GPP_kClipStatus_READ == pClipCtxt->Vstatus )
    {
        /**
        * The decoder must be told to jump */
        bClipJump = M4OSA_TRUE;
        pClipCtxt->iVideoDecCts = iClipCts;

        /**
        * Remember the clip reading state */
        pClipCtxt->Vstatus = M4VSS3GPP_kClipStatus_DECODE_UP_TO;
    }

    /**
    * If we are in decodeUpTo() process, check if we need to do
    one more step or if decoding is finished */
    if( M4VSS3GPP_kClipStatus_DECODE_UP_TO == pClipCtxt->Vstatus )
    {
        /* Do a step of 500 ms decoding */
        pClipCtxt->iVideoDecCts += 500;

        if( pClipCtxt->iVideoDecCts > iClipCts )
        {
            /* Target time reached, we switch back to DECODE mode */
            pClipCtxt->iVideoDecCts = iClipCts;
            pClipCtxt->Vstatus = M4VSS3GPP_kClipStatus_DECODE;
        }

        M4OSA_TRACE2_1("c ,,,, decode up to : %ld", pClipCtxt->iVideoDecCts);
    }
    else
    {
        /* Just decode at current clip cts */
        pClipCtxt->iVideoDecCts = iClipCts;

        M4OSA_TRACE2_1("d ,,,, decode up to : %ld", pClipCtxt->iVideoDecCts);
    }

    /**
    * Decode up to the target */
    M4OSA_TRACE3_2(
        "M4VSS3GPP_intClipDecodeVideoUpToCts: Decoding upTo CTS %.3f, pClipCtxt=0x%x",
        dDecodeTime, pClipCtxt);

    dDecodeTime = (M4OSA_Double)pClipCtxt->iVideoDecCts;
    pClipCtxt->isRenderDup = M4OSA_FALSE;
    err =
        pClipCtxt->ShellAPI.m_pVideoDecoder->m_pFctDecode(pClipCtxt->pViDecCtxt,
        &dDecodeTime, bClipJump, 0);

    if( ( M4NO_ERROR != err) && (M4WAR_NO_MORE_AU != err)
        && (err != M4WAR_VIDEORENDERER_NO_NEW_FRAME) )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intClipDecodeVideoUpToCts: m_pFctDecode returns 0x%x!",
            err);
        return err;
    }

    if( err == M4WAR_VIDEORENDERER_NO_NEW_FRAME )
    {
        pClipCtxt->isRenderDup = M4OSA_TRUE;
    }

    /**
    * Return */
    M4OSA_TRACE3_0("M4VSS3GPP_intClipDecodeVideoUpToCts: returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intClipReadNextAudioFrame()
 * @brief    Read one AU frame in the clip
 * @note
 * @param   pClipCtxt            (IN) Internal clip context
 * @return    M4NO_ERROR:            No error
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_intClipReadNextAudioFrame(
    M4VSS3GPP_ClipContext *pClipCtxt )
{
    M4OSA_ERR err;

    /* ------------------------------ */
    /* ---------- NO AUDIO ---------- */
    /* ------------------------------ */

    if( M4OSA_NULL == pClipCtxt->pAudioStream )
    {
        /* If there is no audio track, we return silence AUs */
        pClipCtxt->pAudioFramePtr =
            (M4OSA_MemAddr8)pClipCtxt->pSilenceFrameData;
        pClipCtxt->uiAudioFrameSize = pClipCtxt->uiSilenceFrameSize;
        pClipCtxt->iAudioFrameCts += pClipCtxt->iSilenceFrameDuration;

        M4OSA_TRACE2_0("b #### blank track");
    }

    /* ---------------------------------- */
    /* ---------- AMR-NB, EVRC ---------- */
    /* ---------------------------------- */

    else if( ( M4VIDEOEDITING_kAMR_NB
        == pClipCtxt->pSettings->ClipProperties.AudioStreamType)
        || (M4VIDEOEDITING_kEVRC
        == pClipCtxt->pSettings->ClipProperties.AudioStreamType) )
    {
        if( M4OSA_FALSE == pClipCtxt->bAudioFrameAvailable )
        {
            /**
            * No AU available, so we must must read one from the original track reader */
            err = pClipCtxt->ShellAPI.m_pReaderDataIt->m_pFctGetNextAu(
                pClipCtxt->pReaderContext,
                (M4_StreamHandler *)pClipCtxt->pAudioStream,
                &pClipCtxt->AudioAU);

            if( M4NO_ERROR == err )
            {
                /**
                * Set the current AMR frame position at the beginning of the read AU */
                pClipCtxt->pAudioFramePtr = pClipCtxt->AudioAU.m_dataAddress;

                /**
                * Set the AMR frame CTS */
                pClipCtxt->iAudioFrameCts =
                    (M4OSA_Int32)(pClipCtxt->AudioAU.m_CTS
                    * pClipCtxt->scale_audio + 0.5);
            }
            else if( ( M4WAR_NO_MORE_AU == err) && (M4VIDEOEDITING_kAMR_NB
                == pClipCtxt->pSettings->ClipProperties.AudioStreamType) )
            {
                /**
                * If there is less audio than the stream duration indicated,
                * we return silence at the end of the stream. */
                pClipCtxt->pAudioFramePtr =
                    (M4OSA_MemAddr8)pClipCtxt->pSilenceFrameData;
                pClipCtxt->uiAudioFrameSize = pClipCtxt->uiSilenceFrameSize;
                pClipCtxt->iAudioFrameCts += pClipCtxt->iSilenceFrameDuration;

                M4OSA_TRACE2_0("a #### silence AU");

                /**
                * Return with M4WAR_NO_MORE_AU */
                M4OSA_TRACE3_0(
                    "M4VSS3GPP_intClipReadNextAudioFrame()-AMR: \
                    returning M4WAR_NO_MORE_AU (silence)");
                return M4WAR_NO_MORE_AU;
            }
            else /**< fatal error (or no silence in EVRC) */
            {
                M4OSA_TRACE3_1(
                    "M4VSS3GPP_intClipReadNextAudioFrame()-AMR: m_pFctGetNextAu() returns 0x%x",
                    err);
                return err;
            }
        }
        else /* bAudioFrameAvailable */
        {
            /**
            * Go to the next AMR frame in the AU */
            pClipCtxt->pAudioFramePtr += pClipCtxt->uiAudioFrameSize;

            /**
            * Increment CTS: one AMR frame is 20 ms long */
            pClipCtxt->iAudioFrameCts += pClipCtxt->iSilenceFrameDuration;
        }

        /**
        * Get the size of the pointed AMR frame */
        switch( pClipCtxt->pSettings->ClipProperties.AudioStreamType )
        {
            case M4VIDEOEDITING_kAMR_NB:
                pClipCtxt->uiAudioFrameSize =
                    (M4OSA_UInt16)M4VSS3GPP_intGetFrameSize_AMRNB(
                    pClipCtxt->pAudioFramePtr);
                break;

            case M4VIDEOEDITING_kEVRC:
                pClipCtxt->uiAudioFrameSize =
                    (M4OSA_UInt16)M4VSS3GPP_intGetFrameSize_EVRC(
                    pClipCtxt->pAudioFramePtr);
                break;
            default:
                break;
        }

        if( 0 == pClipCtxt->uiAudioFrameSize )
        {
            M4OSA_TRACE3_0(
                "M4VSS3GPP_intClipReadNextAudioFrame()-AMR: AU frame size == 0,\
                returning M4VSS3GPP_ERR_INPUT_AUDIO_CORRUPTED_AMR_AU");
            return M4VSS3GPP_ERR_INPUT_AUDIO_CORRUPTED_AU;
        }
        else if( pClipCtxt->uiAudioFrameSize > pClipCtxt->AudioAU.m_size )
        {
            M4OSA_TRACE3_0(
                "M4VSS3GPP_intClipReadNextAudioFrame()-AMR: AU frame size greater than AU size!,\
                returning M4VSS3GPP_ERR_INPUT_AUDIO_CORRUPTED_AMR_AU");
            return M4VSS3GPP_ERR_INPUT_AUDIO_CORRUPTED_AU;
        }

        /**
        * Check if the end of the current AU has been reached or not */
        if( ( pClipCtxt->pAudioFramePtr + pClipCtxt->uiAudioFrameSize)
            < (pClipCtxt->AudioAU.m_dataAddress + pClipCtxt->AudioAU.m_size) )
        {
            pClipCtxt->bAudioFrameAvailable = M4OSA_TRUE;
        }
        else
        {
            pClipCtxt->bAudioFrameAvailable =
                M4OSA_FALSE; /**< will be used for next call */
        }
    }

    /* ------------------------- */
    /* ---------- AAC ---------- */
    /* ------------------------- */

    else if( ( M4VIDEOEDITING_kAAC
        == pClipCtxt->pSettings->ClipProperties.AudioStreamType)
        || (M4VIDEOEDITING_kAACplus
        == pClipCtxt->pSettings->ClipProperties.AudioStreamType)
        || (M4VIDEOEDITING_keAACplus
        == pClipCtxt->pSettings->ClipProperties.AudioStreamType) )
    {
        err = pClipCtxt->ShellAPI.m_pReaderDataIt->m_pFctGetNextAu(
            pClipCtxt->pReaderContext,
            (M4_StreamHandler *)pClipCtxt->pAudioStream,
            &pClipCtxt->AudioAU);

        if( M4NO_ERROR == err )
        {
            pClipCtxt->pAudioFramePtr = pClipCtxt->AudioAU.m_dataAddress;
            pClipCtxt->uiAudioFrameSize =
                (M4OSA_UInt16)pClipCtxt->AudioAU.m_size;
            pClipCtxt->iAudioFrameCts =
                (M4OSA_Int32)(pClipCtxt->AudioAU.m_CTS * pClipCtxt->scale_audio
                + 0.5);

            /* Patch because m_CTS is unfortunately rounded in 3gp reader shell */
            /* (cts is not an integer with frequency 24 kHz for example) */
            pClipCtxt->iAudioFrameCts = ( ( pClipCtxt->iAudioFrameCts
                + pClipCtxt->iSilenceFrameDuration / 2)
                / pClipCtxt->iSilenceFrameDuration)
                * pClipCtxt->iSilenceFrameDuration;
        }
        else if( M4WAR_NO_MORE_AU == err )
        {
            /**
            * If there is less audio than the stream duration indicated,
            * we return silence at the end of the stream. */
            pClipCtxt->pAudioFramePtr =
                (M4OSA_MemAddr8)pClipCtxt->pSilenceFrameData;
            pClipCtxt->uiAudioFrameSize = pClipCtxt->uiSilenceFrameSize;
            pClipCtxt->iAudioFrameCts += pClipCtxt->iSilenceFrameDuration;

            M4OSA_TRACE2_0("a #### silence AU");

            /**
            * Return with M4WAR_NO_MORE_AU */
            M4OSA_TRACE3_0(
                "M4VSS3GPP_intClipReadNextAudioFrame()-AAC:\
                returning M4WAR_NO_MORE_AU (silence)");
            return M4WAR_NO_MORE_AU;
        }
        else /**< fatal error */
        {
            M4OSA_TRACE3_1(
                "M4VSS3GPP_intClipReadNextAudioFrame()-AAC: m_pFctGetNextAu() returns 0x%x",
                err);
            return err;
        }
    }

    /* --------------------------------- */
    /* ---------- MP3, others ---------- */
    /* --------------------------------- */

    else
    {
        err = pClipCtxt->ShellAPI.m_pReaderDataIt->m_pFctGetNextAu(
            pClipCtxt->pReaderContext,
            (M4_StreamHandler *)pClipCtxt->pAudioStream,
            &pClipCtxt->AudioAU);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE3_1(
                "M4VSS3GPP_intClipReadNextAudioFrame()-MP3: m_pFctGetNextAu() returns 0x%x",
                err);
            return err;
        }

        pClipCtxt->pAudioFramePtr = pClipCtxt->AudioAU.m_dataAddress;
        pClipCtxt->uiAudioFrameSize = (M4OSA_UInt16)pClipCtxt->AudioAU.m_size;
        pClipCtxt->iAudioFrameCts =
            (M4OSA_Int32)(pClipCtxt->AudioAU.m_CTS * pClipCtxt->scale_audio
            + 0.5);
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0(
        "M4VSS3GPP_intClipReadNextAudioFrame(): returning M4NO_ERROR");

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intClipPrepareAudioDecoder()
 * @brief    Creates and initialize the audio decoder for the clip.
 * @note
 * @param   pClipCtxt        (IN) internal clip context
 * @return    M4NO_ERROR:            No error
 ******************************************************************************
 */
static M4OSA_ERR M4VSS3GPP_intClipPrepareAudioDecoder(
    M4VSS3GPP_ClipContext *pClipCtxt )
{
    M4OSA_ERR err = M4NO_ERROR;
    M4_StreamType audiotype;
#ifdef M4VSS_SUPPORT_OMX_CODECS

    M4_AACType iAacType = 0;

#endif

    /**
    * Set the proper audio decoder */

    audiotype = pClipCtxt->pAudioStream->m_basicProperties.m_streamType;

    //EVRC
    if( M4DA_StreamTypeAudioEvrc
        != audiotype ) /* decoder not supported yet, but allow to do null encoding */

        err = M4VSS3GPP_setCurrentAudioDecoder(&pClipCtxt->ShellAPI, audiotype);
    M4ERR_CHECK_RETURN(err);

    /**
    * Creates the audio decoder */
    if( M4OSA_NULL == pClipCtxt->ShellAPI.m_pAudioDecoder )
    {
        M4OSA_TRACE1_0(
            "M4VSS3GPP_intClipPrepareAudioDecoder(): Fails to initiate the audio decoder.");
        return M4VSS3GPP_ERR_AUDIO_DECODER_INIT_FAILED;
    }

    if( M4OSA_NULL == pClipCtxt->pAudioDecCtxt )
    {
#ifdef M4VSS_SUPPORT_OMX_CODECS

        if( M4OSA_TRUE == pClipCtxt->ShellAPI.bAllowFreeingOMXCodecInterface )
        {
            if( M4DA_StreamTypeAudioAac == audiotype ) {
                err = M4VSS3GPP_intCheckAndGetCodecAacProperties(
                       pClipCtxt);
            } else if (M4DA_StreamTypeAudioPcm != audiotype) {
                err = pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctCreateAudioDec(
                &pClipCtxt->pAudioDecCtxt, pClipCtxt->pAudioStream,
                M4OSA_NULL);
            } else {
                err = M4NO_ERROR;
            }
            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intClipPrepareAudioDecoder: m_pAudioDecoder->m_pFctCreateAudioDec\
                    returns 0x%x", err);
                return err;
            }
        }
        else
        {
            M4OSA_TRACE3_1(
                "M4VSS3GPP_intClipPrepareAudioDecoder:\
                Creating external audio decoder of type 0x%x", audiotype);
            /* External OMX codecs are used*/
            if( M4DA_StreamTypeAudioAac == audiotype )
            {
                err = pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctCreateAudioDec(
                    &pClipCtxt->pAudioDecCtxt, pClipCtxt->pAudioStream,
                    pClipCtxt->ShellAPI.pCurrentAudioDecoderUserData);

                if( M4NO_ERROR == err )
                {
                    /* AAC properties*/
                    /*get from Reader; temporary, till Audio decoder shell API
                      available to get the AAC properties*/
                    pClipCtxt->AacProperties.aNumChan =
                        pClipCtxt->pAudioStream->m_nbChannels;
                    pClipCtxt->AacProperties.aSampFreq =
                        pClipCtxt->pAudioStream->m_samplingFrequency;

                    err = pClipCtxt->ShellAPI.m_pAudioDecoder->
                        m_pFctGetOptionAudioDec(pClipCtxt->pAudioDecCtxt,
                        M4AD_kOptionID_StreamType,
                        (M4OSA_DataOption) &iAacType);

                    if( M4NO_ERROR != err )
                    {
                        M4OSA_TRACE1_1(
                            "M4VSS3GPP_intClipPrepareAudioDecoder:\
                            m_pAudioDecoder->m_pFctGetOptionAudioDec returns err 0x%x", err);
                        iAacType = M4_kAAC; //set to default
                        err = M4NO_ERROR;
                    }
                    else {
                        M4OSA_TRACE3_1(
                        "M4VSS3GPP_intClipPrepareAudioDecoder: \
                        m_pAudioDecoder->m_pFctGetOptionAudioDec returns streamType %d",
                        iAacType);
                       }
                    switch( iAacType )
                    {
                        case M4_kAAC:
                            pClipCtxt->AacProperties.aSBRPresent = 0;
                            pClipCtxt->AacProperties.aPSPresent = 0;
                            break;

                        case M4_kAACplus:
                            pClipCtxt->AacProperties.aSBRPresent = 1;
                            pClipCtxt->AacProperties.aPSPresent = 0;
                            pClipCtxt->AacProperties.aExtensionSampFreq =
                                pClipCtxt->pAudioStream->m_samplingFrequency;
                            break;

                        case M4_keAACplus:
                            pClipCtxt->AacProperties.aSBRPresent = 1;
                            pClipCtxt->AacProperties.aPSPresent = 1;
                            pClipCtxt->AacProperties.aExtensionSampFreq =
                                pClipCtxt->pAudioStream->m_samplingFrequency;
                            break;
                        default:
                            break;
                    }
                    M4OSA_TRACE3_2(
                        "M4VSS3GPP_intClipPrepareAudioDecoder: AAC NBChans=%d, SamplFreq=%d",
                        pClipCtxt->AacProperties.aNumChan,
                        pClipCtxt->AacProperties.aSampFreq);
                }
            }
            else
                err = pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctCreateAudioDec(
                &pClipCtxt->pAudioDecCtxt, pClipCtxt->pAudioStream,
                pClipCtxt->ShellAPI.pCurrentAudioDecoderUserData);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intClipPrepareAudioDecoder:\
                    m_pAudioDecoder->m_pFctCreateAudioDec returns 0x%x",
                    err);
                return err;
            }
        }

#else
        /* Trick, I use pUserData to retrieve aac properties,
           waiting for some better implementation... */

        if( M4DA_StreamTypeAudioAac == audiotype )
            err = pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctCreateAudioDec(
            &pClipCtxt->pAudioDecCtxt,
            pClipCtxt->pAudioStream, &(pClipCtxt->AacProperties));
        else
            err = pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctCreateAudioDec(
            &pClipCtxt->pAudioDecCtxt, pClipCtxt->pAudioStream,
            M4OSA_NULL /* to be changed with HW interfaces */);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intClipPrepareAudioDecoder:\
                m_pAudioDecoder->m_pFctCreateAudioDec returns 0x%x",
                err);
            return err;
        }

#endif

    }

    if( M4DA_StreamTypeAudioAmrNarrowBand == audiotype ) {
        /* AMR DECODER CONFIGURATION */

        /* nothing specific to do */
    }
    else if( M4DA_StreamTypeAudioEvrc == audiotype ) {
        /* EVRC DECODER CONFIGURATION */

        /* nothing specific to do */
    }
    else if( M4DA_StreamTypeAudioMp3 == audiotype ) {
        /* MP3 DECODER CONFIGURATION */

        /* nothing specific to do */
    }
    else if( M4DA_StreamTypeAudioAac == audiotype )
    {
        /* AAC DECODER CONFIGURATION */

        /* Decode high quality aac but disable PS and SBR */
        /* Because we have to mix different kind of AAC so we must take the lowest capability */
        /* In MCS it was not needed because there is only one stream */
        M4_AacDecoderConfig AacDecParam;

        AacDecParam.m_AACDecoderProfile = AAC_kAAC;
        AacDecParam.m_DownSamplingMode = AAC_kDS_OFF;

        if( M4ENCODER_kMono == pClipCtxt->pAudioStream->m_nbChannels )
        {
            AacDecParam.m_OutputMode = AAC_kMono;
        }
        else
        {
            AacDecParam.m_OutputMode = AAC_kStereo;
        }

        err = pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctSetOptionAudioDec(
            pClipCtxt->pAudioDecCtxt,
            M4AD_kOptionID_UserParam, (M4OSA_DataOption) &AacDecParam);
    }

    if( M4OSA_NULL != pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctSetOptionAudioDec ) {
        pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctSetOptionAudioDec(
         pClipCtxt->pAudioDecCtxt, M4AD_kOptionID_3gpReaderInterface,
         (M4OSA_DataOption) pClipCtxt->ShellAPI.m_pReaderDataIt);

        pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctSetOptionAudioDec(
         pClipCtxt->pAudioDecCtxt, M4AD_kOptionID_AudioAU,
         (M4OSA_DataOption) &pClipCtxt->AudioAU);
    }

    if( M4OSA_NULL != pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctStartAudioDec )
    {
        /* Not implemented in all decoders */
        err = pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctStartAudioDec(
            pClipCtxt->pAudioDecCtxt);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intClipPrepareAudioDecoder:\
                m_pAudioDecoder->m_pFctStartAudioDec returns 0x%x",
                err);
            return err;
        }
    }

    /**
    * Allocate output buffer for the audio decoder */
    pClipCtxt->AudioDecBufferOut.m_bufferSize =
        pClipCtxt->pAudioStream->m_byteFrameLength
        * pClipCtxt->pAudioStream->m_byteSampleSize
        * pClipCtxt->pAudioStream->m_nbChannels;
    pClipCtxt->AudioDecBufferOut.m_dataAddress =
        (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(pClipCtxt->AudioDecBufferOut.m_bufferSize
        * sizeof(M4OSA_Int16),
        M4VSS3GPP, (M4OSA_Char *)"AudioDecBufferOut.m_bufferSize");

    if( M4OSA_NULL == pClipCtxt->AudioDecBufferOut.m_dataAddress )
    {
        M4OSA_TRACE1_0(
            "M4VSS3GPP_intClipPrepareAudioDecoder():\
            unable to allocate AudioDecBufferOut.m_dataAddress, returning M4ERR_ALLOC");
        return M4ERR_ALLOC;
    }

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intClipDecodeCurrentAudioFrame()
 * @brief    Decode the current AUDIO frame.
 * @note
 * @param   pClipCtxt        (IN) internal clip context
 * @return    M4NO_ERROR:            No error
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_intClipDecodeCurrentAudioFrame(
    M4VSS3GPP_ClipContext *pClipCtxt )
{
    M4OSA_ERR err;

    /**
    * Silence mode */
    if( pClipCtxt->pSilenceFrameData
        == (M4OSA_UInt8 *)pClipCtxt->pAudioFramePtr )
    {
        if( pClipCtxt->AudioDecBufferOut.m_dataAddress == M4OSA_NULL )
        {
            /**
            * Allocate output buffer for the audio decoder */
            pClipCtxt->AudioDecBufferOut.m_bufferSize =
                pClipCtxt->uiSilencePcmSize;
            pClipCtxt->AudioDecBufferOut.m_dataAddress =
                (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(
                pClipCtxt->AudioDecBufferOut.m_bufferSize
                * sizeof(M4OSA_Int16),
                M4VSS3GPP,(M4OSA_Char *) "AudioDecBufferOut.m_bufferSize");

            if( M4OSA_NULL == pClipCtxt->AudioDecBufferOut.m_dataAddress )
            {
                M4OSA_TRACE1_0(
                    "M4VSS3GPP_intClipDecodeCurrentAudioFrame():\
                    unable to allocate AudioDecBufferOut.m_dataAddress, returning M4ERR_ALLOC");
                return M4ERR_ALLOC;
            }
        }

        /* Fill it with 0 (= pcm silence) */
        memset(pClipCtxt->AudioDecBufferOut.m_dataAddress,0,
             pClipCtxt->AudioDecBufferOut.m_bufferSize * sizeof(M4OSA_Int16));
    }
    else if (pClipCtxt->pSettings->FileType == M4VIDEOEDITING_kFileType_PCM)
    {
        pClipCtxt->AudioDecBufferIn.m_dataAddress = (M4OSA_MemAddr8) pClipCtxt->pAudioFramePtr;
        pClipCtxt->AudioDecBufferIn.m_bufferSize  = pClipCtxt->uiAudioFrameSize;

        memcpy((void *)pClipCtxt->AudioDecBufferOut.m_dataAddress,
            (void *)pClipCtxt->AudioDecBufferIn.m_dataAddress, pClipCtxt->AudioDecBufferIn.m_bufferSize);
        pClipCtxt->AudioDecBufferOut.m_bufferSize = pClipCtxt->AudioDecBufferIn.m_bufferSize;
        /**
        * Return with no error */

        M4OSA_TRACE3_0("M4VSS3GPP_intClipDecodeCurrentAudioFrame(): returning M4NO_ERROR");
        return M4NO_ERROR;
    }
    /**
    * Standard decoding mode */
    else
    {
        /**
        * Decode current AMR frame */
        if ( pClipCtxt->pAudioFramePtr != M4OSA_NULL ) {
            pClipCtxt->AudioDecBufferIn.m_dataAddress =
             (M4OSA_MemAddr8)pClipCtxt->pAudioFramePtr;
            pClipCtxt->AudioDecBufferIn.m_bufferSize =
             pClipCtxt->uiAudioFrameSize;
            pClipCtxt->AudioDecBufferIn.m_timeStampUs =
             (int64_t) (pClipCtxt->iAudioFrameCts * 1000LL);

            err = pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctStepAudioDec(
             pClipCtxt->pAudioDecCtxt,
             &pClipCtxt->AudioDecBufferIn, &pClipCtxt->AudioDecBufferOut,
             M4OSA_FALSE);
        } else {
            // Pass Null input buffer
            // Reader invoked from Audio decoder source
            err = pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctStepAudioDec(
             pClipCtxt->pAudioDecCtxt,
             M4OSA_NULL, &pClipCtxt->AudioDecBufferOut,
             M4OSA_FALSE);
        }

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intClipDecodeCurrentAudioFrame():\
                m_pAudioDecoder->m_pFctStepAudio returns 0x%x",
                err);
            return err;
        }
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0(
        "M4VSS3GPP_intClipDecodeCurrentAudioFrame(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intClipJumpAudioAt()
 * @brief    Jump in the audio track of the clip.
 * @note
 * @param   pClipCtxt            (IN) internal clip context
 * @param   pJumpCts            (IN/OUT) in:target CTS, out: reached CTS
 * @return    M4NO_ERROR:            No error
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_intClipJumpAudioAt( M4VSS3GPP_ClipContext *pClipCtxt,
                                       M4OSA_Int32 *pJumpCts )
{
    M4OSA_ERR err;
    M4OSA_Int32 iTargetCts;
    M4OSA_Int32 iJumpCtsMs;

    /**
    *    Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pClipCtxt), M4ERR_PARAMETER,
        "M4VSS3GPP_intClipJumpAudioAt: pClipCtxt is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pJumpCts), M4ERR_PARAMETER,
        "M4VSS3GPP_intClipJumpAudioAt: pJumpCts is M4OSA_NULL");

    iTargetCts = *pJumpCts;

    /**
    * If there is no audio stream, we simulate a jump at the target jump CTS */
    if( M4OSA_NULL == pClipCtxt->pAudioStream )
    {
        /**
        * the target CTS will be reached at next ReadFrame call (thus the -20) */
        *pJumpCts = iTargetCts - pClipCtxt->iSilenceFrameDuration;

        /* Patch because m_CTS is unfortunately rounded in 3gp reader shell */
        /* (cts is not an integer with frequency 24 kHz for example) */
        *pJumpCts = ( ( *pJumpCts + pClipCtxt->iSilenceFrameDuration / 2)
            / pClipCtxt->iSilenceFrameDuration)
            * pClipCtxt->iSilenceFrameDuration;
        pClipCtxt->iAudioFrameCts =
            *
            pJumpCts; /* simulate a read at jump position for later silence AUs */
    }
    else
    {
        M4OSA_Int32 current_time = 0;
        M4OSA_Int32 loop_counter = 0;

        if( (M4DA_StreamTypeAudioMp3
            == pClipCtxt->pAudioStream->m_basicProperties.m_streamType) )
        {
            while( ( loop_counter < M4VSS3GPP_MP3_JUMPED_AU_NUMBER_MAX)
                && (current_time < iTargetCts) )
            {
                err = pClipCtxt->ShellAPI.m_pReaderDataIt->m_pFctGetNextAu(
                    pClipCtxt->pReaderContext,
                    (M4_StreamHandler *)pClipCtxt->pAudioStream,
                    &pClipCtxt->AudioAU);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE3_1(
                        "M4VSS3GPP_intClipJumpAudioAt: m_pFctGetNextAu() returns 0x%x",
                        err);
                    return err;
                }

                current_time = (M4OSA_Int32)pClipCtxt->AudioAU.m_CTS;
                loop_counter++;
            }

            /**
            * The current AU is stored */
            pClipCtxt->pAudioFramePtr = pClipCtxt->AudioAU.m_dataAddress;
            pClipCtxt->uiAudioFrameSize =
                (M4OSA_UInt16)pClipCtxt->AudioAU.m_size;
            pClipCtxt->iAudioFrameCts =
                (M4OSA_Int32)(pClipCtxt->AudioAU.m_CTS * pClipCtxt->scale_audio
                + 0.5);

            *pJumpCts = pClipCtxt->iAudioFrameCts;
        }
        else
        {
            /**
            * Jump in the audio stream */
            iJumpCtsMs =
                (M4OSA_Int32)(*pJumpCts / pClipCtxt->scale_audio + 0.5);

            err = pClipCtxt->ShellAPI.m_pReader->m_pFctJump(
                pClipCtxt->pReaderContext,
                (M4_StreamHandler *)pClipCtxt->pAudioStream,
                &iJumpCtsMs);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intClipJumpAudioAt(): m_pFctJump() returns 0x%x",
                    err);
                return err;
            }

            *pJumpCts =
                (M4OSA_Int32)(iJumpCtsMs * pClipCtxt->scale_audio + 0.5);

            /* Patch because m_CTS is unfortunately rounded in 3gp reader shell */
            /* (cts is not an integer with frequency 24 kHz for example) */
            *pJumpCts = ( ( *pJumpCts + pClipCtxt->iSilenceFrameDuration / 2)
                / pClipCtxt->iSilenceFrameDuration)
                * pClipCtxt->iSilenceFrameDuration;
            pClipCtxt->iAudioFrameCts = 0; /* No frame read yet */

            /**
            * To detect some may-be bugs, I prefer to reset all these after a jump */
            pClipCtxt->bAudioFrameAvailable = M4OSA_FALSE;
            pClipCtxt->pAudioFramePtr = M4OSA_NULL;

            /**
            * In AMR, we have to manage multi-framed AUs,
            but also in AAC the jump can be 1 AU too much backward */
            if( *pJumpCts < iTargetCts )
            {
                /**
                * Jump doesn't read any AU, we must read at least one */
                err = M4VSS3GPP_intClipReadNextAudioFrame(pClipCtxt);

                if( M4OSA_ERR_IS_ERROR(err) )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intClipJumpAudioAt():\
                        M4VSS3GPP_intClipReadNextAudioFrame(a) returns 0x%x",
                        err);
                    return err;
                }

                /**
                * Read AU frames as long as we reach the AU before the target CTS
                * (so the target will be reached when the user call ReadNextAudioFrame). */
                while( pClipCtxt->iAudioFrameCts
                    < (iTargetCts - pClipCtxt->iSilenceFrameDuration) )
                {
                    err = M4VSS3GPP_intClipReadNextAudioFrame(pClipCtxt);

                    if( M4OSA_ERR_IS_ERROR(err) )
                    {
                        M4OSA_TRACE1_1(
                            "M4VSS3GPP_intClipJumpAudioAt():\
                            M4VSS3GPP_intClipReadNextAudioFrame(b) returns 0x%x",
                            err);
                        return err;
                    }
                }

                /**
                * Return the CTS that will be reached at next ReadFrame */
                *pJumpCts = pClipCtxt->iAudioFrameCts
                    + pClipCtxt->iSilenceFrameDuration;
            }
        }
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_intClipJumpAudioAt(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intClipClose()
 * @brief    Close a clip. Destroy the context.
 * @note
 * @param   pClipCtxt            (IN) Internal clip context
 * @return    M4NO_ERROR:            No error
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_intClipClose( M4VSS3GPP_ClipContext *pClipCtxt )
{
    M4OSA_ERR err;

    /**
    *    Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pClipCtxt), M4ERR_PARAMETER,
        "M4VSS3GPP_intClipClose: pClipCtxt is M4OSA_NULL");

    /**
    * Free the video decoder context */
    if( M4OSA_NULL != pClipCtxt->pViDecCtxt )
    {
        pClipCtxt->ShellAPI.m_pVideoDecoder->m_pFctDestroy(
            pClipCtxt->pViDecCtxt);
        pClipCtxt->pViDecCtxt = M4OSA_NULL;
    }

    /**
    * Free the audio decoder context  */
    if( M4OSA_NULL != pClipCtxt->pAudioDecCtxt )
    {
        err = pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctDestroyAudioDec(
            pClipCtxt->pAudioDecCtxt);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intClipClose: m_pAudioDecoder->m_pFctDestroyAudioDec returns 0x%x",
                err);
            /**< don't return, we still have stuff to free */
        }

        pClipCtxt->pAudioDecCtxt = M4OSA_NULL;
    }

    /**
    * Free the decoded audio buffer */
    if( M4OSA_NULL != pClipCtxt->AudioDecBufferOut.m_dataAddress )
    {
        free(pClipCtxt->AudioDecBufferOut.m_dataAddress);
        pClipCtxt->AudioDecBufferOut.m_dataAddress = M4OSA_NULL;
    }

    /**
    * Audio AU is allocated by reader.
    * If no audio track, audio AU is set at 'silent' (SID) by VSS.
    * As a consequence, if audio AU is set to 'silent' (static)
    it can't be free unless it is set to NULL */
    if( ( (M4OSA_MemAddr8)M4VSS3GPP_AMR_AU_SILENCE_FRAME_048
        == pClipCtxt->AudioAU.m_dataAddress)
        || ((M4OSA_MemAddr8)pClipCtxt->pSilenceFrameData
        == pClipCtxt->AudioAU.m_dataAddress) )
    {
        pClipCtxt->AudioAU.m_dataAddress = M4OSA_NULL;
    }

    if( M4OSA_NULL != pClipCtxt->pReaderContext )
    {
        /**
        * Close the 3GPP or MP3 reader */
        err = pClipCtxt->ShellAPI.m_pReader->m_pFctClose(
            pClipCtxt->pReaderContext);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intClipClose(): m_pReader->m_pFctClose returns 0x%x",
                err);
        }

        /**
        * Destroy the 3GPP or MP3 reader context */
        err = pClipCtxt->ShellAPI.m_pReader->m_pFctDestroy(
            pClipCtxt->pReaderContext);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intClipClose(): m_pReader->m_pFctDestroy returns 0x%x",
                err);
        }

        pClipCtxt->pReaderContext = M4OSA_NULL;
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_1("M4VSS3GPP_intClipClose(Ctxt=0x%x): returning M4NO_ERROR",
        pClipCtxt);
    return M4NO_ERROR;
}

M4OSA_ERR M4VSS3GPP_intClipCleanUp( M4VSS3GPP_ClipContext *pClipCtxt )
{
    M4OSA_ERR err = M4NO_ERROR, err2;

    /**
    *    Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pClipCtxt), M4ERR_PARAMETER,
        "M4VSS3GPP_intClipCleanUp: pClipCtxt is M4OSA_NULL");

    /**
    * Free the video decoder context */
    if( M4OSA_NULL != pClipCtxt->pViDecCtxt )
    {
        pClipCtxt->ShellAPI.m_pVideoDecoder->m_pFctDestroy(
            pClipCtxt->pViDecCtxt);
        pClipCtxt->pViDecCtxt = M4OSA_NULL;
    }

    /**
    * Free the audio decoder context  */
    if( M4OSA_NULL != pClipCtxt->pAudioDecCtxt )
    {
        err2 = pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctDestroyAudioDec(
            pClipCtxt->pAudioDecCtxt);

        if( M4NO_ERROR != err2 )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intClipCleanUp: m_pAudioDecoder->m_pFctDestroyAudioDec returns 0x%x",
                err);
            /**< don't return, we still have stuff to free */
            if( M4NO_ERROR != err )
                err = err2;
        }

        pClipCtxt->pAudioDecCtxt = M4OSA_NULL;
    }

    /**
    * Free the decoded audio buffer */
    if( M4OSA_NULL != pClipCtxt->AudioDecBufferOut.m_dataAddress )
    {
        free(pClipCtxt->AudioDecBufferOut.m_dataAddress);
        pClipCtxt->AudioDecBufferOut.m_dataAddress = M4OSA_NULL;
    }

    /**
    * Audio AU is allocated by reader.
    * If no audio track, audio AU is set at 'silent' (SID) by VSS.
    * As a consequence, if audio AU is set to 'silent' (static)
    it can't be free unless it is set to NULL */
    if( ( (M4OSA_MemAddr8)M4VSS3GPP_AMR_AU_SILENCE_FRAME_048
        == pClipCtxt->AudioAU.m_dataAddress)
        || ((M4OSA_MemAddr8)pClipCtxt->pSilenceFrameData
        == pClipCtxt->AudioAU.m_dataAddress) )
    {
        pClipCtxt->AudioAU.m_dataAddress = M4OSA_NULL;
    }

    if( M4OSA_NULL != pClipCtxt->pReaderContext )
    {
        /**
        * Close the 3GPP or MP3 reader */
        err2 = pClipCtxt->ShellAPI.m_pReader->m_pFctClose(
            pClipCtxt->pReaderContext);

        if( M4NO_ERROR != err2 )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intClipCleanUp(): m_pReader->m_pFctClose returns 0x%x",
                err);

            if( M4NO_ERROR != err )
                err = err2;
        }

        /**
        * Destroy the 3GPP or MP3 reader context */
        err2 = pClipCtxt->ShellAPI.m_pReader->m_pFctDestroy(
            pClipCtxt->pReaderContext);

        if( M4NO_ERROR != err2 )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intClipCleanUp(): m_pReader->m_pFctDestroy returns 0x%x",
                err);

            if( M4NO_ERROR != err )
                err = err2;
        }

        pClipCtxt->pReaderContext = M4OSA_NULL;
    }

    if(pClipCtxt->pPlaneYuv != M4OSA_NULL) {
        if(pClipCtxt->pPlaneYuv[0].pac_data != M4OSA_NULL) {
            free(pClipCtxt->pPlaneYuv[0].pac_data);
            pClipCtxt->pPlaneYuv[0].pac_data = M4OSA_NULL;
        }
        free(pClipCtxt->pPlaneYuv);
        pClipCtxt->pPlaneYuv = M4OSA_NULL;
    }

    if(pClipCtxt->pPlaneYuvWithEffect != M4OSA_NULL) {
        if(pClipCtxt->pPlaneYuvWithEffect[0].pac_data != M4OSA_NULL) {
            free(pClipCtxt->pPlaneYuvWithEffect[0].pac_data);
            pClipCtxt->pPlaneYuvWithEffect[0].pac_data = M4OSA_NULL;
        }
        free(pClipCtxt->pPlaneYuvWithEffect);
        pClipCtxt->pPlaneYuvWithEffect = M4OSA_NULL;
    }
    /**
    * Free the shells interfaces */
    M4VSS3GPP_unRegisterAllWriters(&pClipCtxt->ShellAPI);
    M4VSS3GPP_unRegisterAllEncoders(&pClipCtxt->ShellAPI);
    M4VSS3GPP_unRegisterAllReaders(&pClipCtxt->ShellAPI);
    M4VSS3GPP_unRegisterAllDecoders(&pClipCtxt->ShellAPI);

    M4OSA_TRACE3_1("M4VSS3GPP_intClipCleanUp: pClipCtxt=0x%x", pClipCtxt);
    /**
    * Free the clip context */
    free(pClipCtxt);

    return err;
}

/**
 ******************************************************************************
 * M4OSA_UInt32 M4VSS3GPP_intGetFrameSize_AMRNB()
 * @brief   Return the length, in bytes, of the AMR Narrow-Band frame contained in the given buffer
 * @note
 * @param   pAudioFrame   (IN) AMRNB frame
 * @return  M4NO_ERROR: No error
 ******************************************************************************
 */

M4OSA_UInt32 M4VSS3GPP_intGetFrameSize_AMRNB( M4OSA_MemAddr8 pAudioFrame )
{
    M4OSA_UInt32 frameSize = 0;
    M4OSA_UInt32 frameType = ( ( *pAudioFrame) &(0xF << 3)) >> 3;

    switch( frameType )
    {
        case 0:
            frameSize = 95;
            break; /*  4750 bps */

        case 1:
            frameSize = 103;
            break; /*  5150 bps */

        case 2:
            frameSize = 118;
            break; /*  5900 bps */

        case 3:
            frameSize = 134;
            break; /*  6700 bps */

        case 4:
            frameSize = 148;
            break; /*  7400 bps */

        case 5:
            frameSize = 159;
            break; /*  7950 bps */

        case 6:
            frameSize = 204;
            break; /* 10200 bps */

        case 7:
            frameSize = 244;
            break; /* 12000 bps */

        case 8:
            frameSize = 39;
            break; /* SID (Silence) */

        case 15:
            frameSize = 0;
            break; /* No data */

        default:
            M4OSA_TRACE3_0(
                "M4VSS3GPP_intGetFrameSize_AMRNB(): Corrupted AMR frame! returning 0.");
            return 0;
    }

    return (1 + (( frameSize + 7) / 8));
}

/**
 ******************************************************************************
 * M4OSA_UInt32 M4VSS3GPP_intGetFrameSize_EVRC()
 * @brief   Return the length, in bytes, of the EVRC frame contained in the given buffer
 * @note
 *     0 1 2 3
 *    +-+-+-+-+
 *    |fr type|              RFC 3558
 *    +-+-+-+-+
 *
 * Frame Type: 4 bits
 *    The frame type indicates the type of the corresponding codec data
 *    frame in the RTP packet.
 *
 * For EVRC and SMV codecs, the frame type values and size of the
 * associated codec data frame are described in the table below:
 *
 * Value   Rate      Total codec data frame size (in octets)
 * ---------------------------------------------------------
 *   0     Blank      0    (0 bit)
 *   1     1/8        2    (16 bits)
 *   2     1/4        5    (40 bits; not valid for EVRC)
 *   3     1/2       10    (80 bits)
 *   4     1         22    (171 bits; 5 padded at end with zeros)
 *   5     Erasure    0    (SHOULD NOT be transmitted by sender)
 *
 * @param   pCpAudioFrame   (IN) EVRC frame
 * @return  M4NO_ERROR: No error
 ******************************************************************************
 */
M4OSA_UInt32 M4VSS3GPP_intGetFrameSize_EVRC( M4OSA_MemAddr8 pAudioFrame )
{
    M4OSA_UInt32 frameSize = 0;
    M4OSA_UInt32 frameType = ( *pAudioFrame) &0x0F;

    switch( frameType )
    {
        case 0:
            frameSize = 0;
            break; /*  blank */

        case 1:
            frameSize = 16;
            break; /*  1/8 */

        case 2:
            frameSize = 40;
            break; /*  1/4 */

        case 3:
            frameSize = 80;
            break; /*  1/2 */

        case 4:
            frameSize = 171;
            break; /*  1 */

        case 5:
            frameSize = 0;
            break; /*  erasure */

        default:
            M4OSA_TRACE3_0(
                "M4VSS3GPP_intGetFrameSize_EVRC(): Corrupted EVRC frame! returning 0.");
            return 0;
    }

    return (1 + (( frameSize + 7) / 8));
}

M4OSA_ERR M4VSS3GPP_intCheckAndGetCodecAacProperties(
                                 M4VSS3GPP_ClipContext *pClipCtxt) {

    M4OSA_ERR err = M4NO_ERROR;
    M4AD_Buffer outputBuffer;
    uint32_t optionValue =0;

    // Decode first audio frame from clip to get properties from codec

    err = pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctCreateAudioDec(
                    &pClipCtxt->pAudioDecCtxt,
                    pClipCtxt->pAudioStream, &(pClipCtxt->AacProperties));

    pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctSetOptionAudioDec(
     pClipCtxt->pAudioDecCtxt, M4AD_kOptionID_3gpReaderInterface,
     (M4OSA_DataOption) pClipCtxt->ShellAPI.m_pReaderDataIt);

    pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctSetOptionAudioDec(
     pClipCtxt->pAudioDecCtxt, M4AD_kOptionID_AudioAU,
     (M4OSA_DataOption) &pClipCtxt->AudioAU);

    if( pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctStartAudioDec != M4OSA_NULL ) {

        err = pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctStartAudioDec(
         pClipCtxt->pAudioDecCtxt);
        if( M4NO_ERROR != err ) {

            M4OSA_TRACE1_1(
                "M4VSS3GPP_intCheckAndGetCodecAacProperties: \
                 m_pFctStartAudioDec returns 0x%x", err);
            return err;
        }
    }

    /**
    * Allocate output buffer for the audio decoder */
    outputBuffer.m_bufferSize =
        pClipCtxt->pAudioStream->m_byteFrameLength
        * pClipCtxt->pAudioStream->m_byteSampleSize
        * pClipCtxt->pAudioStream->m_nbChannels;

    if( outputBuffer.m_bufferSize > 0 ) {

        outputBuffer.m_dataAddress =
            (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(outputBuffer.m_bufferSize \
            *sizeof(short), M4VSS3GPP, (M4OSA_Char *)"outputBuffer.m_bufferSize");

        if( M4OSA_NULL == outputBuffer.m_dataAddress ) {

            M4OSA_TRACE1_0(
                "M4VSS3GPP_intCheckAndGetCodecAacProperties():\
                 unable to allocate outputBuffer.m_dataAddress");
            return M4ERR_ALLOC;
        }
    }

    err = pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctStepAudioDec(
            pClipCtxt->pAudioDecCtxt, M4OSA_NULL, &outputBuffer, M4OSA_FALSE);

    if ( err == M4WAR_INFO_FORMAT_CHANGE ) {

        // Get the properties from codec node
        pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctGetOptionAudioDec(
         pClipCtxt->pAudioDecCtxt,
           M4AD_kOptionID_AudioNbChannels, (M4OSA_DataOption) &optionValue);

        pClipCtxt->AacProperties.aNumChan = optionValue;
        // Reset Reader structure value also
        pClipCtxt->pAudioStream->m_nbChannels = optionValue;

        pClipCtxt->ShellAPI.m_pAudioDecoder->m_pFctGetOptionAudioDec(
         pClipCtxt->pAudioDecCtxt,
          M4AD_kOptionID_AudioSampFrequency, (M4OSA_DataOption) &optionValue);

        pClipCtxt->AacProperties.aSampFreq = optionValue;
        // Reset Reader structure value also
        pClipCtxt->pAudioStream->m_samplingFrequency = optionValue;

    } else if( err != M4NO_ERROR) {
        M4OSA_TRACE1_1("M4VSS3GPP_intCheckAndGetCodecAacProperties:\
            m_pFctStepAudioDec returns err = 0x%x", err);
    }

    free(outputBuffer.m_dataAddress);

    // Reset the stream reader
    err = pClipCtxt->ShellAPI.m_pReader->m_pFctReset(
     pClipCtxt->pReaderContext,
     (M4_StreamHandler *)pClipCtxt->pAudioStream);

    if (M4NO_ERROR != err) {
        M4OSA_TRACE1_1("M4VSS3GPP_intCheckAndGetCodecAacProperties\
            Error in reseting reader: 0x%x", err);
    }

    return err;

}
