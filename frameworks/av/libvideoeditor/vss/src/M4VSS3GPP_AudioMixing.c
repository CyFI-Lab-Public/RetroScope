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
 * @file    M4VSS3GPP_AudioMixing.c
 * @brief    Video Studio Service 3GPP audio mixing implementation.
 * @note
 ******************************************************************************
 */

/****************/
/*** Includes ***/
/****************/

#include "NXPSW_CompilerSwitches.h"
/**
 * Our headers */
#include "M4VSS3GPP_API.h"
#include "M4VSS3GPP_InternalTypes.h"
#include "M4VSS3GPP_InternalFunctions.h"
#include "M4VSS3GPP_ErrorCodes.h"

/* Put the definition of silence frames here */
#define M4VSS3GPP_SILENCE_FRAMES
#include "M4VSS3GPP_InternalConfig.h"

/**
 * OSAL headers */
#include "M4OSA_Memory.h" /**< OSAL memory management */
#include "M4OSA_Debug.h"  /**< OSAL debug management */


#include "VideoEditorResampler.h"
/**
 ******************************************************************************
 * @brief    Static functions
 ******************************************************************************
 */
static M4OSA_ERR
M4VSS3GPP_intAudioMixingOpen( M4VSS3GPP_InternalAudioMixingContext *pC,
                             M4VSS3GPP_AudioMixingSettings *pSettings );
static M4OSA_ERR M4VSS3GPP_intAudioMixingStepVideo(
    M4VSS3GPP_InternalAudioMixingContext *pC );
static M4OSA_ERR M4VSS3GPP_intAudioMixingStepAudioMix(
    M4VSS3GPP_InternalAudioMixingContext *pC );
static M4OSA_ERR M4VSS3GPP_intAudioMixingStepAudioReplace(
    M4VSS3GPP_InternalAudioMixingContext *pC );
static M4OSA_ERR M4VSS3GPP_intAudioMixingCopyOrig(
    M4VSS3GPP_InternalAudioMixingContext *pC );
static M4OSA_ERR M4VSS3GPP_intAudioMixingCopyAdded(
    M4VSS3GPP_InternalAudioMixingContext *pC );
static M4OSA_ERR M4VSS3GPP_intAudioMixingConvert(
    M4VSS3GPP_InternalAudioMixingContext *pC );
static M4OSA_ERR M4VSS3GPP_intAudioMixingDoMixing(
    M4VSS3GPP_InternalAudioMixingContext *pC );
static M4OSA_ERR M4VSS3GPP_intAudioMixingWriteSilence(
    M4VSS3GPP_InternalAudioMixingContext *pC );
static M4OSA_ERR M4VSS3GPP_intAudioMixingTransition(
    M4VSS3GPP_InternalAudioMixingContext *pC );
static M4OSA_ERR M4VSS3GPP_intAudioMixingCreateVideoEncoder(
    M4VSS3GPP_InternalAudioMixingContext *pC );
static M4OSA_ERR M4VSS3GPP_intAudioMixingDestroyVideoEncoder(
    M4VSS3GPP_InternalAudioMixingContext *pC );
static M4OSA_Bool M4VSS3GPP_isThresholdBreached( M4OSA_Int32 *averageValue,
                                                M4OSA_Int32 storeCount,
                                                M4OSA_Int32 thresholdValue );
/**
 *    Internal warning */
#define M4VSS3GPP_WAR_END_OF_ADDED_AUDIO    M4OSA_ERR_CREATE( M4_WAR, M4VSS3GPP, 0x0030)

/* A define used with SSRC 1.04 and above to avoid taking
blocks smaller that the minimal block size */
#define M4VSS_SSRC_MINBLOCKSIZE        600

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_audioMixingInit(M4VSS3GPP_AudioMixingContext* pContext,
 *                                     M4VSS3GPP_AudioMixingSettings* pSettings)
 * @brief    Initializes the VSS audio mixing operation (allocates an execution context).
 * @note
 * @param    pContext        (OUT) Pointer on the VSS audio mixing context to allocate
 * @param    pSettings        (IN) Pointer to valid audio mixing settings
 * @param    pFileReadPtrFct        (IN) Pointer to OSAL file reader functions
 * @param   pFileWritePtrFct    (IN) Pointer to OSAL file writer functions
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 * @return    M4ERR_ALLOC:        There is no more available memory
 ******************************************************************************
 */

M4OSA_ERR M4VSS3GPP_audioMixingInit( M4VSS3GPP_AudioMixingContext *pContext,
                                    M4VSS3GPP_AudioMixingSettings *pSettings,
                                    M4OSA_FileReadPointer *pFileReadPtrFct,
                                    M4OSA_FileWriterPointer *pFileWritePtrFct )
{
    M4VSS3GPP_InternalAudioMixingContext *pC;
    M4OSA_ERR err;

    M4OSA_TRACE3_2(
        "M4VSS3GPP_audioMixingInit called with pContext=0x%x, pSettings=0x%x",
        pContext, pSettings);

    /**
    * Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4VSS3GPP_audioMixingInit: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pSettings), M4ERR_PARAMETER,
        "M4VSS3GPP_audioMixingInit: pSettings is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pFileReadPtrFct), M4ERR_PARAMETER,
        "M4VSS3GPP_audioMixingInit: pFileReadPtrFct is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pFileWritePtrFct), M4ERR_PARAMETER,
        "M4VSS3GPP_audioMixingInit: pFileWritePtrFct is M4OSA_NULL");

    if( pSettings->uiBeginLoop > pSettings->uiEndLoop )
    {
        M4OSA_TRACE1_0(
            "M4VSS3GPP_audioMixingInit: Begin loop time is higher than end loop time!");
        return M4VSS3GPP_ERR_BEGINLOOP_HIGHER_ENDLOOP;
    }

    /**
    * Allocate the VSS audio mixing context and return it to the user */
    pC = (M4VSS3GPP_InternalAudioMixingContext
        *)M4OSA_32bitAlignedMalloc(sizeof(M4VSS3GPP_InternalAudioMixingContext),
        M4VSS3GPP,(M4OSA_Char *)"M4VSS3GPP_InternalAudioMixingContext");
    *pContext = pC;

    if( M4OSA_NULL == pC )
    {
        M4OSA_TRACE1_0(
            "M4VSS3GPP_audioMixingInit(): unable to allocate \
            M4VSS3GPP_InternalAudioMixingContext,returning M4ERR_ALLOC");
        return M4ERR_ALLOC;
    }

    /* Initialization of context Variables */
    memset((void *)pC ,0,
                 sizeof(M4VSS3GPP_InternalAudioMixingContext));
    /**
    * Copy this setting in context */
    pC->iAddCts = pSettings->uiAddCts;
    pC->bRemoveOriginal = pSettings->bRemoveOriginal;
    pC->b_DuckingNeedeed = pSettings->b_DuckingNeedeed;
    pC->InDucking_threshold = pSettings->InDucking_threshold;
    pC->fBTVolLevel = pSettings->fBTVolLevel;
    pC->fPTVolLevel = pSettings->fPTVolLevel;
    pC->InDucking_lowVolume = pSettings->InDucking_lowVolume;
    pC->bDoDucking = M4OSA_FALSE;
    pC->bLoop = pSettings->bLoop;
    pC->bNoLooping = M4OSA_FALSE;
    pC->bjumpflag = M4OSA_TRUE;
    /**
    * Init some context variables */

    pC->pInputClipCtxt = M4OSA_NULL;
    pC->pAddedClipCtxt = M4OSA_NULL;
    pC->fOrigFactor = 1.0F;
    pC->fAddedFactor = 0.0F;
    pC->bSupportSilence = M4OSA_FALSE;
    pC->bHasAudio = M4OSA_FALSE;
    pC->bAudioMixingIsNeeded = M4OSA_FALSE;

    /* Init PC->ewc members */
    // Decorrelate input and output encoding timestamp to handle encoder prefetch
    pC->ewc.VideoStreamType = M4SYS_kVideoUnknown;
    pC->ewc.bVideoDataPartitioning = M4OSA_FALSE;
    pC->ewc.pVideoOutputDsi = M4OSA_NULL;
    pC->ewc.AudioStreamType = M4SYS_kAudioUnknown;
    pC->ewc.uiNbChannels = 1;
    pC->ewc.pAudioOutputDsi = M4OSA_NULL;
    pC->ewc.pAudioEncCtxt = M4OSA_NULL;
    pC->ewc.pAudioEncDSI.pInfo = M4OSA_NULL;
    pC->ewc.pSilenceFrameData = M4OSA_NULL;
    pC->ewc.pEncContext = M4OSA_NULL;
    pC->ewc.pDummyAuBuffer = M4OSA_NULL;
    pC->ewc.p3gpWriterContext = M4OSA_NULL;
    pC->pLVAudioResampler = M4OSA_NULL;
    /**
    * Set the OSAL filesystem function set */
    pC->pOsaFileReadPtr = pFileReadPtrFct;
    pC->pOsaFileWritPtr = pFileWritePtrFct;

    /**
    * Ssrc stuff */
    pC->b_SSRCneeded = M4OSA_FALSE;
    pC->pSsrcBufferIn = M4OSA_NULL;
    pC->pSsrcBufferOut = M4OSA_NULL;
    pC->pTempBuffer = M4OSA_NULL;
    pC->pPosInTempBuffer = M4OSA_NULL;
    pC->pPosInSsrcBufferIn = M4OSA_NULL;
    pC->pPosInSsrcBufferOut = M4OSA_NULL;
    pC->SsrcScratch = M4OSA_NULL;
    pC->uiBeginLoop = pSettings->uiBeginLoop;
    pC->uiEndLoop = pSettings->uiEndLoop;

    /*
    * Reset pointers for media and codecs interfaces */
    err = M4VSS3GPP_clearInterfaceTables(&pC->ShellAPI);
    M4ERR_CHECK_RETURN(err);

    /*  Call the media and codecs subscription module */
    err = M4VSS3GPP_subscribeMediaAndCodec(&pC->ShellAPI);
    M4ERR_CHECK_RETURN(err);

    /**
    * Open input clip, added clip and output clip and proceed with the settings */
    err = M4VSS3GPP_intAudioMixingOpen(pC, pSettings);
    M4ERR_CHECK_RETURN(err);

    /**
    * Update main state automaton */
    if( M4OSA_NULL != pC->pInputClipCtxt->pVideoStream )
        pC->State = M4VSS3GPP_kAudioMixingState_VIDEO;
    else
        pC->State = M4VSS3GPP_kAudioMixingState_AUDIO_FIRST_SEGMENT;

    pC->ewc.iOutputDuration = (M4OSA_Int32)pC->pInputClipCtxt->pSettings->
        ClipProperties.uiClipDuration;
    /*gInputParams.lvBTChannelCount*/
    pC->pLVAudioResampler = LVAudioResamplerCreate(16,
        pC->pAddedClipCtxt->pSettings->ClipProperties.uiNbChannels,
        /* gInputParams.lvOutSampleRate*/(M4OSA_Int32)pSettings->outputASF, 1);
     if( M4OSA_NULL == pC->pLVAudioResampler )
     {
         return M4ERR_ALLOC;
     }
        LVAudiosetSampleRate(pC->pLVAudioResampler,
        /*gInputParams.lvInSampleRate*/
        pC->pAddedClipCtxt->pSettings->ClipProperties.uiSamplingFrequency);

    LVAudiosetVolume(pC->pLVAudioResampler,
                    (M4OSA_Int16)(0x1000 ),
                    (M4OSA_Int16)(0x1000 ));

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_audioMixingInit(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_audioMixingStep(M4VSS3GPP_AudioMixingContext pContext)
 * @brief    Perform one step of audio mixing.
 * @note
 * @param     pContext          (IN) VSS audio mixing context
 * @return    M4NO_ERROR:       No error
 * @return    M4ERR_PARAMETER:  pContext is M4OSA_NULL (debug only)
 * @param     pProgress         (OUT) Progress percentage (0 to 100) of the finalization operation
 * @return    M4ERR_STATE:      VSS is not in an appropriate state for this function to be called
 * @return    M4VSS3GPP_WAR_END_OF_AUDIO_MIXING: Audio mixing is over, user should now call
 *                                               M4VSS3GPP_audioMixingCleanUp()
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_audioMixingStep( M4VSS3GPP_AudioMixingContext pContext,
                                    M4OSA_UInt8 *pProgress )
{
    M4OSA_ERR err;
    M4VSS3GPP_InternalAudioMixingContext *pC =
        (M4VSS3GPP_InternalAudioMixingContext *)pContext;

    M4OSA_TRACE3_1("M4VSS3GPP_audioMixingStep called with pContext=0x%x",
        pContext);

    /**
    * Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4VSS3GPP_audioMixingStep: pContext is M4OSA_NULL");

    /**
    * State automaton */
    switch( pC->State )
    {
        case M4VSS3GPP_kAudioMixingState_VIDEO:
            err = M4VSS3GPP_intAudioMixingStepVideo(pC);

            /**
            * Compute the progress percentage
            * Note: audio and video CTS are not initialized before
            * the call of M4VSS3GPP_intAudioMixingStepVideo */

            /* P4ME00003276: First 0-50% segment is dedicated to state :
               M4VSS3GPP_kAudioMixingState_VIDEO */
            *pProgress = (M4OSA_UInt8)(50 * (pC->ewc.WriterVideoAU.CTS)
                / pC->pInputClipCtxt->pVideoStream->
                m_basicProperties.m_duration);

            /**
            * There may be no audio track (Remove audio track feature).
            * In that case we double the current percentage */
            if( M4SYS_kAudioUnknown == pC->ewc.WriterAudioStream.streamType )
            {
                ( *pProgress) <<= 1; /**< x2 */
            }
            else if( *pProgress >= 50 )
            {
                *pProgress =
                    49; /**< Video processing is not greater than 50% */
            }

            if( M4WAR_NO_MORE_AU == err )
            {
                if( pC->bHasAudio )
                {
                    /**
                    * Video is over, state transition to audio and return OK */
                    if( pC->iAddCts > 0 )
                        pC->State =
                        M4VSS3GPP_kAudioMixingState_AUDIO_FIRST_SEGMENT;
                    else
                        pC->State =
                        M4VSS3GPP_kAudioMixingState_AUDIO_SECOND_SEGMENT;
                }
                else
                {
                    /**
                    * No audio, state transition to FINISHED */
                    pC->State = M4VSS3GPP_kAudioMixingState_FINISHED;
                }

                return M4NO_ERROR;
            }
            else if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_audioMixingStep: M4VSS3GPP_intAudioMixingStepVideo returns 0x%x!",
                    err);
                return err;
            }
            else
            {
                return M4NO_ERROR;
            }
            break;

        case M4VSS3GPP_kAudioMixingState_AUDIO_FIRST_SEGMENT:
        case M4VSS3GPP_kAudioMixingState_AUDIO_SECOND_SEGMENT:
        case M4VSS3GPP_kAudioMixingState_AUDIO_THIRD_SEGMENT:
            if( pC->pAddedClipCtxt->iAudioFrameCts
                != -pC->pAddedClipCtxt->iSilenceFrameDuration
                && (pC->pAddedClipCtxt->iAudioFrameCts - 0.5)
                / pC->pAddedClipCtxt->scale_audio > pC->uiEndLoop
                && pC->uiEndLoop > 0 )
            {
            if(pC->bLoop == M4OSA_FALSE)
            {
                pC->bNoLooping = M4OSA_TRUE;
            }
            else
            {
                M4OSA_Int32 jumpCTS = (M4OSA_Int32)(pC->uiBeginLoop);

                err = pC->pAddedClipCtxt->ShellAPI.m_pReader->m_pFctJump(
                    pC->pAddedClipCtxt->pReaderContext,
                    (M4_StreamHandler *)pC->pAddedClipCtxt->
                    pAudioStream, &jumpCTS);

                if( err != M4NO_ERROR )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_audioMixingStep: error when jumping in added audio clip: 0x%x",
                        err);
                    return err;
                }
                /**
                * Use offset to give a correct CTS ... */
                pC->pAddedClipCtxt->iAoffset =
                    (M4OSA_Int32)(pC->ewc.dATo * pC->ewc.scale_audio + 0.5);
            }

            }

            if( M4OSA_FALSE == pC->bRemoveOriginal )
            {
                err = M4VSS3GPP_intAudioMixingStepAudioMix(pC);
            }
            else
            {
                err = M4VSS3GPP_intAudioMixingStepAudioReplace(pC);
            }

            /**
            * Compute the progress percentage
            * Note: audio and video CTS are not initialized before
            * the call of M4VSS3GPP_intAudioMixingStepAudio */
            if( 0 != pC->ewc.iOutputDuration )
            {
                /* P4ME00003276: Second 50-100% segment is dedicated to states :
                M4VSS3GPP_kAudioMixingState_AUDIO... */
                /* For Audio the progress computation is based on dAto and offset,
                   it is more accurate */
                *pProgress = (M4OSA_UInt8)(50
                    + (50 * pC->ewc.dATo - pC->pInputClipCtxt->iVoffset)
                    / (pC->ewc.iOutputDuration)); /**< 50 for 100/2 **/

                if( *pProgress >= 100 )
                {
                    *pProgress =
                        99; /**< It's not really finished, I prefer to return less than 100% */
                }
            }
            else
            {
                *pProgress = 99;
            }

            if( M4WAR_NO_MORE_AU == err )
            {
                /**
                * Audio is over, state transition to FINISHED */
                pC->State = M4VSS3GPP_kAudioMixingState_FINISHED;
                return M4NO_ERROR;
            }
            else if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_audioMixingStep: M4VSS3GPP_intAudioMixingStepAudio returns 0x%x!",
                    err);
                return err;
            }
            else
            {
                return M4NO_ERROR;
            }
            break;

        case M4VSS3GPP_kAudioMixingState_FINISHED:

            /**
            * Progress percentage: finalize finished -> 100% */
            *pProgress = 100;

            /**
            * Audio mixing is finished, return correct warning */
            return M4VSS3GPP_WAR_END_OF_AUDIO_MIXING;

        default:
            M4OSA_TRACE1_1(
                "M4VSS3GPP_audioMixingStep: State error (0x%x)! Returning M4ERR_STATE",
                pC->State);
            return M4ERR_STATE;
    }
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_audioMixingCleanUp(M4VSS3GPP_AudioMixingContext pContext)
 * @brief    Free all resources used by the VSS audio mixing operation.
 * @note    The context is no more valid after this call
 * @param    pContext            (IN) VSS audio mixing context
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pContext is M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_audioMixingCleanUp( M4VSS3GPP_AudioMixingContext pContext )
{
    M4VSS3GPP_InternalAudioMixingContext *pC =
        (M4VSS3GPP_InternalAudioMixingContext *)pContext;
    M4OSA_ERR err;
    M4OSA_UInt32 lastCTS;

    M4OSA_TRACE3_1("M4VSS3GPP_audioMixingCleanUp called with pContext=0x%x",
        pContext);

    /**
    * Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4VSS3GPP_audioMixingCleanUp: pContext is M4OSA_NULL");

    /**
    * Check input parameter */
    if( M4OSA_NULL == pContext )
    {
        M4OSA_TRACE1_0(
            "M4VSS3GPP_audioMixingCleanUp(): M4VSS3GPP_audioMixingCleanUp: pContext is\
             M4OSA_NULL, returning M4ERR_PARAMETER");
        return M4ERR_PARAMETER;
    }

    /**
    * Close Input 3GPP file */
    if( M4OSA_NULL != pC->pInputClipCtxt )
    {
        M4VSS3GPP_intClipCleanUp(pC->pInputClipCtxt);
        pC->pInputClipCtxt = M4OSA_NULL;
    }

    /**
    * Close Added 3GPP file */
    if( M4OSA_NULL != pC->pAddedClipCtxt )
    {
        M4VSS3GPP_intClipCleanUp(pC->pAddedClipCtxt);
        pC->pAddedClipCtxt = M4OSA_NULL;
    }

    /**
    * Close the 3GP writer. In normal use case it has already been closed,
      but not in abort use case */
    if( M4OSA_NULL != pC->ewc.p3gpWriterContext )
    {
        /* Update last Video CTS */
        lastCTS = pC->ewc.iOutputDuration;

        err = pC->ShellAPI.pWriterGlobalFcts->pFctSetOption(
            pC->ewc.p3gpWriterContext,
            (M4OSA_UInt32)M4WRITER_kMaxFileDuration, &lastCTS);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_audioMixingCleanUp: SetOption(M4WRITER_kMaxFileDuration) returns 0x%x",
                err);
        }

        err = pC->ShellAPI.pWriterGlobalFcts->pFctCloseWrite(
            pC->ewc.p3gpWriterContext);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_audioMixingCleanUp: pWriterGlobalFcts->pFctCloseWrite returns 0x%x!",
                err);
            /**< don't return the error because we have other things to free! */
        }
        pC->ewc.p3gpWriterContext = M4OSA_NULL;
    }

    /**
    * Free the Audio encoder context */
    if( M4OSA_NULL != pC->ewc.pAudioEncCtxt )
    {
        err = pC->ShellAPI.pAudioEncoderGlobalFcts->pFctClose(
            pC->ewc.pAudioEncCtxt);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_audioMixingCleanUp: pAudioEncoderGlobalFcts->pFctClose returns 0x%x",
                err);
            /**< don't return, we still have stuff to free */
        }

        err = pC->ShellAPI.pAudioEncoderGlobalFcts->pFctCleanUp(
            pC->ewc.pAudioEncCtxt);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_audioMixingCleanUp: pAudioEncoderGlobalFcts->pFctCleanUp returns 0x%x",
                err);
            /**< don't return, we still have stuff to free */
        }

        pC->ewc.pAudioEncCtxt = M4OSA_NULL;
    }

    /**
    * Free the ssrc stuff */

    if( M4OSA_NULL != pC->SsrcScratch )
    {
        free(pC->SsrcScratch);
        pC->SsrcScratch = M4OSA_NULL;
    }

    if( M4OSA_NULL != pC->pSsrcBufferIn )
    {
        free(pC->pSsrcBufferIn);
        pC->pSsrcBufferIn = M4OSA_NULL;
    }

    if( M4OSA_NULL != pC->pSsrcBufferOut
        && (M4OSA_TRUE == pC->b_SSRCneeded || pC->ChannelConversion > 0) )
    {
        free(pC->pSsrcBufferOut);
        pC->pSsrcBufferOut = M4OSA_NULL;
    }

    if( M4OSA_NULL != pC->pTempBuffer )
    {
        free(pC->pTempBuffer);
        pC->pTempBuffer = M4OSA_NULL;
    }

    if (pC->pLVAudioResampler != M4OSA_NULL)
    {
        LVDestroy(pC->pLVAudioResampler);
        pC->pLVAudioResampler = M4OSA_NULL;
    }

    /**
    * Free the shells interfaces */
    M4VSS3GPP_unRegisterAllWriters(&pC->ShellAPI);
    M4VSS3GPP_unRegisterAllEncoders(&pC->ShellAPI);
    M4VSS3GPP_unRegisterAllReaders(&pC->ShellAPI);
    M4VSS3GPP_unRegisterAllDecoders(&pC->ShellAPI);

    /**
    * Free the context */
    free(pContext);
    pContext = M4OSA_NULL;

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_audioMixingCleanUp(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/******************************************************************************/
/******************************************************************************/
/*********                  STATIC FUNCTIONS                         **********/
/******************************************************************************/
/******************************************************************************/

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intAudioMixingOpen()
 * @brief    Initializes the VSS audio mixing operation (allocates an execution context).
 * @note
 * @param    pContext        (OUT) Pointer on the VSS audio mixing context to allocate
 * @param    pSettings        (IN) Pointer to valid audio mixing settings
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 * @return    M4ERR_ALLOC:        There is no more available memory
 ******************************************************************************
 */
static M4OSA_ERR
M4VSS3GPP_intAudioMixingOpen( M4VSS3GPP_InternalAudioMixingContext *pC,
                             M4VSS3GPP_AudioMixingSettings *pSettings )
{
    M4OSA_ERR err;
    M4OSA_UInt32 outputASF = 0;
    M4ENCODER_Header *encHeader;

    M4OSA_TRACE3_2(
        "M4VSS3GPP_intAudioMixingOpen called with pContext=0x%x, pSettings=0x%x",
        pC, pSettings);

    /**
    * The Add Volume must be (strictly) superior than zero */
    if( pSettings->uiAddVolume == 0 )
    {
        M4OSA_TRACE1_0(
            "M4VSS3GPP_intAudioMixingOpen(): AddVolume is zero,\
            returning M4VSS3GPP_ERR_ADDVOLUME_EQUALS_ZERO");
        return M4VSS3GPP_ERR_ADDVOLUME_EQUALS_ZERO;
    }
    /*
    else if(pSettings->uiAddVolume >= 100) // If volume is set to 100, no more original audio ...
    {
    pC->bRemoveOriginal = M4OSA_TRUE;
    }
    */
    /**
    * Build the input clip settings */
    pC->InputClipSettings.pFile =
        pSettings->pOriginalClipFile; /**< Input 3GPP file descriptor */
    pC->InputClipSettings.FileType = M4VIDEOEDITING_kFileType_3GPP;
    pC->InputClipSettings.uiBeginCutTime =
        0; /**< No notion of cut for the audio mixing feature */
    pC->InputClipSettings.uiEndCutTime =
        0; /**< No notion of cut for the audio mixing feature */

    /**
    * Open the original Audio/Video 3GPP clip */
    err = M4VSS3GPP_intClipInit(&pC->pInputClipCtxt, pC->pOsaFileReadPtr);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intAudioMixingOpen(): M4VSS3GPP_intClipInit(orig) returns 0x%x",
            err);
        return err;
    }

    err = M4VSS3GPP_intClipOpen(pC->pInputClipCtxt, &pC->InputClipSettings,
        M4OSA_FALSE, M4OSA_FALSE, M4OSA_TRUE);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intAudioMixingOpen(): M4VSS3GPP_intClipOpen(orig) returns 0x%x",
            err);
        return err;
    }

    if( M4OSA_NULL == pC->pInputClipCtxt->pAudioStream )
        {
        pC->bRemoveOriginal = M4OSA_TRUE;
        }
    /**
    * If there is no video, it's an error */
    if( M4OSA_NULL == pC->pInputClipCtxt->pVideoStream )
    {
        M4OSA_TRACE1_0(
            "M4VSS3GPP_intAudioMixingOpen(): no video stream in clip,\
            returning M4VSS3GPP_ERR_NO_SUPPORTED_STREAM_IN_FILE");
        return M4VSS3GPP_ERR_NO_SUPPORTED_STREAM_IN_FILE;
    }

    /**
    * Compute clip properties */
    err = M4VSS3GPP_intBuildAnalysis(pC->pInputClipCtxt,
        &pC->pInputClipCtxt->pSettings->ClipProperties);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intAudioMixingOpen(): M4VSS3GPP_intBuildAnalysis(orig) returns 0x%x",
            err);
        return err;
    }

    /**
    * Build the added clip settings */
    pC->AddedClipSettings.pFile =
        pSettings->pAddedAudioTrackFile; /**< Added file descriptor */
    pC->AddedClipSettings.FileType = pSettings->AddedAudioFileType;
    pC->AddedClipSettings.uiBeginCutTime =
        0; /**< No notion of cut for the audio mixing feature */
    pC->AddedClipSettings.uiEndCutTime   = 0;/**< No notion of cut for the audio mixing feature */
    pC->AddedClipSettings.ClipProperties.uiNbChannels=
        pSettings->uiNumChannels;
    pC->AddedClipSettings.ClipProperties.uiSamplingFrequency=    pSettings->uiSamplingFrequency;

    if( M4OSA_NULL != pC->AddedClipSettings.pFile )
    {
        /**
        * Open the added Audio clip */
        err = M4VSS3GPP_intClipInit(&pC->pAddedClipCtxt, pC->pOsaFileReadPtr);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingOpen(): M4VSS3GPP_intClipInit(added) returns 0x%x",
                err);
            return err;
        }

        err = M4VSS3GPP_intClipOpen(pC->pAddedClipCtxt, &pC->AddedClipSettings,
            M4OSA_FALSE, M4OSA_FALSE, M4OSA_TRUE);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingOpen(): M4VSS3GPP_intClipOpen(added) returns 0x%x",
                err);
            return err;
        }

        /**
        * If there is no audio, it's an error */
        if( M4OSA_NULL == pC->pAddedClipCtxt->pAudioStream )
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_intAudioMixingOpen(): no audio nor video stream in clip,\
                returning M4VSS3GPP_ERR_NO_SUPPORTED_STREAM_IN_FILE");
            return M4VSS3GPP_ERR_NO_SUPPORTED_STREAM_IN_FILE;
        }

        /**
        * Compute clip properties */
        err = M4VSS3GPP_intBuildAnalysis(pC->pAddedClipCtxt,
            &pC->pAddedClipCtxt->pSettings->ClipProperties);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingOpen(): M4VSS3GPP_intBuildAnalysis(added) returns 0x%x",
                err);
            return err;
        }

        switch( pSettings->outputASF )
        {
            case M4VIDEOEDITING_k8000_ASF:
                outputASF = 8000;
                break;

            case M4VIDEOEDITING_k16000_ASF:
                outputASF = 16000;
                break;

            case M4VIDEOEDITING_k22050_ASF:
                outputASF = 22050;
                break;

            case M4VIDEOEDITING_k24000_ASF:
                outputASF = 24000;
                break;

            case M4VIDEOEDITING_k32000_ASF:
                outputASF = 32000;
                break;

            case M4VIDEOEDITING_k44100_ASF:
                outputASF = 44100;
                break;

            case M4VIDEOEDITING_k48000_ASF:
                outputASF = 48000;
                break;

            default:
                M4OSA_TRACE1_0("Bad parameter in output ASF ");
                return M4ERR_PARAMETER;
                break;
        }

        if( pC->bRemoveOriginal == M4OSA_TRUE
            && (pC->pAddedClipCtxt->pSettings->ClipProperties.AudioStreamType
            == M4VIDEOEDITING_kMP3 || pC->pAddedClipCtxt->pSettings->
            ClipProperties.AudioStreamType == M4VIDEOEDITING_kPCM
            || pC->pAddedClipCtxt->pSettings->
            ClipProperties.AudioStreamType
            != pSettings->outputAudioFormat
            || pC->pAddedClipCtxt->pSettings->
            ClipProperties.uiSamplingFrequency != outputASF
            || pC->pAddedClipCtxt->pSettings->
            ClipProperties.uiNbChannels
            != pSettings->outputNBChannels) )
        {

            if( pSettings->outputAudioFormat == M4VIDEOEDITING_kAMR_NB )
            {
                pSettings->outputASF = M4VIDEOEDITING_k8000_ASF;
                pSettings->outputNBChannels = 1;
                pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize = 320;
            }
            else if( pSettings->outputAudioFormat == M4VIDEOEDITING_kAAC )
            {
                pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize =
                    2048 * pSettings->outputNBChannels;
            }

            pC->pInputClipCtxt->pSettings->ClipProperties.uiSamplingFrequency =
                outputASF;

            if( outputASF != pC->pAddedClipCtxt->pSettings->
                ClipProperties.uiSamplingFrequency )
            {
                /* We need to call SSRC in order to align ASF and/or nb of channels */
                /* Moreover, audio encoder may be needed in case of audio replacing... */
                pC->b_SSRCneeded = M4OSA_TRUE;
            }

            if( pSettings->outputNBChannels
                < pC->pAddedClipCtxt->pSettings->ClipProperties.uiNbChannels )
            {
                /* Stereo to Mono */
                pC->ChannelConversion = 1;
            }
            else if( pSettings->outputNBChannels
                > pC->pAddedClipCtxt->pSettings->ClipProperties.uiNbChannels )
            {
                /* Mono to Stereo */
                pC->ChannelConversion = 2;
            }

            pC->pInputClipCtxt->pSettings->ClipProperties.uiNbChannels =
                pSettings->outputNBChannels;
        }

        /**
        * Check compatibility chart */
        err = M4VSS3GPP_intAudioMixingCompatibility(pC,
            &pC->pInputClipCtxt->pSettings->ClipProperties,
            &pC->pAddedClipCtxt->pSettings->ClipProperties);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingOpen():\
                M4VSS3GPP_intAudioMixingCompatibility returns 0x%x",
                err);
            return err;
        }

        /**
        * Check loop parameters */
        if( pC->uiBeginLoop > pC->pAddedClipCtxt->pSettings->
            ClipProperties.uiClipAudioDuration )
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_intAudioMixingOpen():\
                begin loop time is higher than added clip audio duration");
            return M4VSS3GPP_ERR_BEGINLOOP_HIGHER_ENDLOOP;
        }

        /**
        * Ok, let's go with this audio track */
        pC->bHasAudio = M4OSA_TRUE;
    }
    else
    {
        /* No added file, force remove original */
        pC->AddedClipSettings.FileType = M4VIDEOEDITING_kFileType_Unsupported;
        pC->bRemoveOriginal = M4OSA_TRUE;
        pC->bHasAudio = M4OSA_FALSE;
    }

    /**
    * Copy the video properties of the input clip to the output properties */
    pC->ewc.uiVideoBitrate =
        pC->pInputClipCtxt->pSettings->ClipProperties.uiVideoBitrate;
    pC->ewc.uiVideoWidth =
        pC->pInputClipCtxt->pSettings->ClipProperties.uiVideoWidth;
    pC->ewc.uiVideoHeight =
        pC->pInputClipCtxt->pSettings->ClipProperties.uiVideoHeight;
    pC->ewc.uiVideoTimeScale =
        pC->pInputClipCtxt->pSettings->ClipProperties.uiVideoTimeScale;
    pC->ewc.bVideoDataPartitioning =
        pC->pInputClipCtxt->pSettings->ClipProperties.bMPEG4dataPartition;
    pC->ewc.outputVideoProfile =
        pC->pInputClipCtxt->pSettings->ClipProperties.uiVideoProfile;
    pC->ewc.outputVideoLevel =
        pC->pInputClipCtxt->pSettings->ClipProperties.uiVideoLevel;
    switch( pC->pInputClipCtxt->pSettings->ClipProperties.VideoStreamType )
    {
        case M4VIDEOEDITING_kH263:
            pC->ewc.VideoStreamType = M4SYS_kH263;
            break;

        case M4VIDEOEDITING_kMPEG4:
            pC->ewc.VideoStreamType = M4SYS_kMPEG_4;
            break;

        case M4VIDEOEDITING_kH264:
            pC->ewc.VideoStreamType = M4SYS_kH264;
            break;

        default:
            pC->ewc.VideoStreamType = M4SYS_kVideoUnknown;
            break;
    }

    /* Add a link to video dsi */
    if( M4SYS_kH264 == pC->ewc.VideoStreamType )
    {

        /* For H.264 encoder case
        * Fetch the DSI from the shell video encoder, and feed it to the writer */

        M4OSA_TRACE3_0("M4VSS3GPP_intAudioMixingOpen: get DSI for H264 stream");

        if( M4OSA_NULL == pC->ewc.pEncContext )
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_intAudioMixingOpen: pC->ewc.pEncContext is NULL");
            err = M4VSS3GPP_intAudioMixingCreateVideoEncoder(pC);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intAudioMixingOpen:\
                    M4VSS3GPP_intAudioMixingCreateVideoEncoder returned error 0x%x",
                    err);
            }
        }

        if( M4OSA_NULL != pC->ewc.pEncContext )
        {
            err = pC->ShellAPI.pVideoEncoderGlobalFcts->pFctGetOption(
                pC->ewc.pEncContext, M4ENCODER_kOptionID_EncoderHeader,
                (M4OSA_DataOption) &encHeader);

            if( ( M4NO_ERROR != err) || (M4OSA_NULL == encHeader->pBuf) )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intAudioMixingOpen: failed to get the encoder header (err 0x%x)",
                    err);
                M4OSA_TRACE1_2(
                    "M4VSS3GPP_intAudioMixingOpen: encHeader->pBuf=0x%x, size=0x%x",
                    encHeader->pBuf, encHeader->Size);
            }
            else
            {
                M4OSA_TRACE1_0(
                    "M4VSS3GPP_intAudioMixingOpen: send DSI for H264 stream to 3GP writer");

                /**
                * Allocate and copy the new DSI */
                pC->ewc.pVideoOutputDsi =
                    (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(encHeader->Size, M4VSS3GPP,
                    (M4OSA_Char *)"pC->ewc.pVideoOutputDsi (H264)");

                if( M4OSA_NULL == pC->ewc.pVideoOutputDsi )
                {
                    M4OSA_TRACE1_0(
                        "M4VSS3GPP_intAudioMixingOpen():\
                        unable to allocate pVideoOutputDsi (H264), returning M4ERR_ALLOC");
                    return M4ERR_ALLOC;
                }
                pC->ewc.uiVideoOutputDsiSize = (M4OSA_UInt16)encHeader->Size;
                memcpy((void *)pC->ewc.pVideoOutputDsi, (void *)encHeader->pBuf,
                    encHeader->Size);
            }

            err = M4VSS3GPP_intAudioMixingDestroyVideoEncoder(pC);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intAudioMixingOpen:\
                    M4VSS3GPP_intAudioMixingDestroyVideoEncoder returned error 0x%x",
                    err);
            }
        }
        else
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_intAudioMixingOpen: pC->ewc.pEncContext is NULL, cannot get the DSI");
        }
    }
    else
    {
        M4OSA_TRACE3_1(
            "M4VSS3GPP_intAudioMixingOpen: input clip video stream type = 0x%x",
            pC->ewc.VideoStreamType);
        pC->ewc.uiVideoOutputDsiSize =
            (M4OSA_UInt16)pC->pInputClipCtxt->pVideoStream->
            m_basicProperties.m_decoderSpecificInfoSize;
        pC->ewc.pVideoOutputDsi = (M4OSA_MemAddr8)pC->pInputClipCtxt->pVideoStream->
            m_basicProperties.m_pDecoderSpecificInfo;
    }

    /**
    * Copy the audio properties of the added clip to the output properties */
    if( pC->bHasAudio )
    {
        if( pC->bRemoveOriginal == M4OSA_TRUE )
        {
            pC->ewc.uiNbChannels =
                pC->pAddedClipCtxt->pSettings->ClipProperties.uiNbChannels;
            pC->ewc.uiAudioBitrate =
                pC->pAddedClipCtxt->pSettings->ClipProperties.uiAudioBitrate;
            pC->ewc.uiSamplingFrequency = pC->pAddedClipCtxt->pSettings->
                ClipProperties.uiSamplingFrequency;
            pC->ewc.uiSilencePcmSize =
                pC->pAddedClipCtxt->pSettings->ClipProperties.uiDecodedPcmSize;
            pC->ewc.scale_audio = pC->ewc.uiSamplingFrequency / 1000.0;

            /* if output settings are differents from added clip settings,
            we need to reencode BGM */
            if( pC->pAddedClipCtxt->pSettings->ClipProperties.AudioStreamType
                != pSettings->outputAudioFormat
                || pC->pAddedClipCtxt->pSettings->
                ClipProperties.uiSamplingFrequency != outputASF
                || pC->pAddedClipCtxt->pSettings->
                ClipProperties.uiNbChannels
                != pSettings->outputNBChannels
                || pC->pAddedClipCtxt->pSettings->
                ClipProperties.AudioStreamType == M4VIDEOEDITING_kMP3 )
            {
                /* Set reader DSI to NULL (unknown), we will use encoder DSI later */
                if( pC->pAddedClipCtxt->pAudioStream->
                    m_basicProperties.m_pDecoderSpecificInfo != M4OSA_NULL )
                {

                    /*
                     free(pC->pAddedClipCtxt->pAudioStream->\
                       m_basicProperties.m_pDecoderSpecificInfo);
                       */
                    pC->pAddedClipCtxt->pAudioStream->
                        m_basicProperties.m_decoderSpecificInfoSize = 0;
                    pC->pAddedClipCtxt->pAudioStream->
                        m_basicProperties.m_pDecoderSpecificInfo = M4OSA_NULL;
                }

                pC->ewc.uiNbChannels =
                    pC->pInputClipCtxt->pSettings->ClipProperties.uiNbChannels;
                pC->ewc.uiSamplingFrequency = pC->pInputClipCtxt->pSettings->
                    ClipProperties.uiSamplingFrequency;
                pC->ewc.scale_audio = pC->ewc.uiSamplingFrequency / 1000.0;

                if( pSettings->outputAudioFormat == M4VIDEOEDITING_kAMR_NB )
                {
                    pC->ewc.AudioStreamType = M4SYS_kAMR;
                    pC->ewc.pSilenceFrameData =
                        (M4OSA_UInt8 *)M4VSS3GPP_AMR_AU_SILENCE_FRAME_048;
                    pC->ewc.uiSilenceFrameSize =
                        M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_SIZE;
                    pC->ewc.iSilenceFrameDuration =
                        M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_DURATION;
                    pC->ewc.uiAudioBitrate = 12200;
                    pC->ewc.uiSamplingFrequency = 8000;
                    pC->ewc.uiSilencePcmSize = 320;
                    pC->ewc.scale_audio = pC->ewc.uiSamplingFrequency / 1000.0;
                }
                else if( pSettings->outputAudioFormat == M4VIDEOEDITING_kAAC )
                {
                    pC->ewc.AudioStreamType = M4SYS_kAAC;

                    if( pSettings->outputAudioBitrate
                        == M4VIDEOEDITING_kUndefinedBitrate )
                    {
                        switch( pC->ewc.uiSamplingFrequency )
                        {
                            case 16000:
                                pC->ewc.uiAudioBitrate =
                                    M4VIDEOEDITING_k24_KBPS;
                                break;

                            case 22050:
                            case 24000:
                                pC->ewc.uiAudioBitrate =
                                    M4VIDEOEDITING_k32_KBPS;
                                break;

                            case 32000:
                                pC->ewc.uiAudioBitrate =
                                    M4VIDEOEDITING_k48_KBPS;
                                break;

                            case 44100:
                            case 48000:
                                pC->ewc.uiAudioBitrate =
                                    M4VIDEOEDITING_k64_KBPS;
                                break;

                            default:
                                pC->ewc.uiAudioBitrate =
                                    M4VIDEOEDITING_k64_KBPS;
                                break;
                        }

                        if( pC->ewc.uiNbChannels == 2 )
                        {
                            /* Output bitrate have to be doubled */
                            pC->ewc.uiAudioBitrate += pC->ewc.uiAudioBitrate;
                        }
                    }
                    else
                    {
                        pC->ewc.uiAudioBitrate = pSettings->outputAudioBitrate;
                    }

                    if( pC->ewc.uiNbChannels == 1 )
                    {
                        pC->ewc.pSilenceFrameData =
                            (M4OSA_UInt8 *)M4VSS3GPP_AAC_AU_SILENCE_MONO;
                        pC->ewc.uiSilenceFrameSize =
                            M4VSS3GPP_AAC_AU_SILENCE_MONO_SIZE;
                    }
                    else
                    {
                        pC->ewc.pSilenceFrameData =
                            (M4OSA_UInt8 *)M4VSS3GPP_AAC_AU_SILENCE_STEREO;
                        pC->ewc.uiSilenceFrameSize =
                            M4VSS3GPP_AAC_AU_SILENCE_STEREO_SIZE;
                    }
                    pC->ewc.iSilenceFrameDuration =
                        1024; /* AAC is always 1024/Freq sample duration */
                }
            }
            else
            {
                switch( pC->pAddedClipCtxt->pSettings->
                    ClipProperties.AudioStreamType )
                {
                    case M4VIDEOEDITING_kAMR_NB:
                        pC->ewc.AudioStreamType = M4SYS_kAMR;
                        pC->ewc.pSilenceFrameData =
                            (M4OSA_UInt8 *)M4VSS3GPP_AMR_AU_SILENCE_FRAME_048;
                        pC->ewc.uiSilenceFrameSize =
                            M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_SIZE;
                        pC->ewc.iSilenceFrameDuration =
                            M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_DURATION;
                        break;

                    case M4VIDEOEDITING_kAAC:
                    case M4VIDEOEDITING_kAACplus:
                    case M4VIDEOEDITING_keAACplus:
                        pC->ewc.AudioStreamType = M4SYS_kAAC;

                        if( pC->ewc.uiNbChannels == 1 )
                        {
                            pC->ewc.pSilenceFrameData =
                                (M4OSA_UInt8 *)M4VSS3GPP_AAC_AU_SILENCE_MONO;
                            pC->ewc.uiSilenceFrameSize =
                                M4VSS3GPP_AAC_AU_SILENCE_MONO_SIZE;
                        }
                        else
                        {
                            pC->ewc.pSilenceFrameData =
                                (M4OSA_UInt8 *)M4VSS3GPP_AAC_AU_SILENCE_STEREO;
                            pC->ewc.uiSilenceFrameSize =
                                M4VSS3GPP_AAC_AU_SILENCE_STEREO_SIZE;
                        }
                        pC->ewc.iSilenceFrameDuration =
                            1024; /* AAC is always 1024/Freq sample duration */
                        break;

                    case M4VIDEOEDITING_kEVRC:
                        pC->ewc.AudioStreamType = M4SYS_kEVRC;
                        pC->ewc.pSilenceFrameData = M4OSA_NULL;
                        pC->ewc.uiSilenceFrameSize = 0;
                        pC->ewc.iSilenceFrameDuration = 160; /* EVRC frames are 20 ms at 8000 Hz
                                            (makes it easier to factorize amr and evrc code) */
                        break;

                    case M4VIDEOEDITING_kPCM:
                        /* Set reader DSI to NULL (unknown), we will use encoder DSI later */
                        pC->pAddedClipCtxt->pAudioStream->
                            m_basicProperties.m_decoderSpecificInfoSize = 0;
                        pC->pAddedClipCtxt->pAudioStream->
                            m_basicProperties.m_pDecoderSpecificInfo =
                            M4OSA_NULL;

                        if( pC->pAddedClipCtxt->pSettings->
                            ClipProperties.uiSamplingFrequency == 8000 )
                        {
                            pC->ewc.AudioStreamType = M4SYS_kAMR;
                            pC->ewc.pSilenceFrameData = (M4OSA_UInt8
                                *)M4VSS3GPP_AMR_AU_SILENCE_FRAME_048;
                            pC->ewc.uiSilenceFrameSize =
                                M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_SIZE;
                            pC->ewc.iSilenceFrameDuration =
                                M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_DURATION;
                            pC->ewc.uiAudioBitrate = M4VIDEOEDITING_k12_2_KBPS;
                        }
                        else if( pC->pAddedClipCtxt->pSettings->
                            ClipProperties.uiSamplingFrequency == 16000 )
                        {
                            if( pC->ewc.uiNbChannels == 1 )
                            {
                                pC->ewc.AudioStreamType = M4SYS_kAAC;
                                pC->ewc.pSilenceFrameData = (M4OSA_UInt8
                                    *)M4VSS3GPP_AAC_AU_SILENCE_MONO;
                                pC->ewc.uiSilenceFrameSize =
                                    M4VSS3GPP_AAC_AU_SILENCE_MONO_SIZE;
                                pC->ewc.iSilenceFrameDuration =
                                    1024; /* AAC is always 1024/Freq sample duration */
                                pC->ewc.uiAudioBitrate =
                                    M4VIDEOEDITING_k32_KBPS;
                            }
                            else
                            {
                                pC->ewc.AudioStreamType = M4SYS_kAAC;
                                pC->ewc.pSilenceFrameData = (M4OSA_UInt8
                                    *)M4VSS3GPP_AAC_AU_SILENCE_STEREO;
                                pC->ewc.uiSilenceFrameSize =
                                    M4VSS3GPP_AAC_AU_SILENCE_STEREO_SIZE;
                                pC->ewc.iSilenceFrameDuration =
                                    1024; /* AAC is always 1024/Freq sample duration */
                                pC->ewc.uiAudioBitrate =
                                    M4VIDEOEDITING_k64_KBPS;
                            }
                        }
                        else
                        {
                            pC->ewc.AudioStreamType = M4SYS_kAudioUnknown;
                        }
                        break;

                    default:
                        pC->ewc.AudioStreamType = M4SYS_kAudioUnknown;
                        break;
                }
            }

            /* Add a link to audio dsi */
            pC->ewc.uiAudioOutputDsiSize =
                (M4OSA_UInt16)pC->pAddedClipCtxt->pAudioStream->
                m_basicProperties.m_decoderSpecificInfoSize;
            pC->ewc.pAudioOutputDsi = (M4OSA_MemAddr8)pC->pAddedClipCtxt->pAudioStream->
                m_basicProperties.m_pDecoderSpecificInfo;
        }
        else
        {
            pC->ewc.uiNbChannels =
                pC->pInputClipCtxt->pSettings->ClipProperties.uiNbChannels;
            pC->ewc.uiAudioBitrate =
                pC->pInputClipCtxt->pSettings->ClipProperties.uiAudioBitrate;
            pC->ewc.uiSamplingFrequency = pC->pInputClipCtxt->pSettings->
                ClipProperties.uiSamplingFrequency;
            pC->ewc.uiSilencePcmSize =
                pC->pInputClipCtxt->pSettings->ClipProperties.uiDecodedPcmSize;
            pC->ewc.scale_audio = pC->ewc.uiSamplingFrequency / 1000.0;

            switch( pC->pInputClipCtxt->pSettings->
                ClipProperties.AudioStreamType )
            {
                case M4VIDEOEDITING_kAMR_NB:
                    pC->ewc.AudioStreamType = M4SYS_kAMR;
                    pC->ewc.pSilenceFrameData =
                        (M4OSA_UInt8 *)M4VSS3GPP_AMR_AU_SILENCE_FRAME_048;
                    pC->ewc.uiSilenceFrameSize =
                        M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_SIZE;
                    pC->ewc.iSilenceFrameDuration =
                        M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_DURATION;
                    break;

                case M4VIDEOEDITING_kAAC:
                case M4VIDEOEDITING_kAACplus:
                case M4VIDEOEDITING_keAACplus:
                    pC->ewc.AudioStreamType = M4SYS_kAAC;

                    if( pC->ewc.uiNbChannels == 1 )
                    {
                        pC->ewc.pSilenceFrameData =
                            (M4OSA_UInt8 *)M4VSS3GPP_AAC_AU_SILENCE_MONO;
                        pC->ewc.uiSilenceFrameSize =
                            M4VSS3GPP_AAC_AU_SILENCE_MONO_SIZE;
                    }
                    else
                    {
                        pC->ewc.pSilenceFrameData =
                            (M4OSA_UInt8 *)M4VSS3GPP_AAC_AU_SILENCE_STEREO;
                        pC->ewc.uiSilenceFrameSize =
                            M4VSS3GPP_AAC_AU_SILENCE_STEREO_SIZE;
                    }
                    pC->ewc.iSilenceFrameDuration =
                        1024; /* AAC is always 1024/Freq sample duration */
                    break;

                default:
                    pC->ewc.AudioStreamType = M4SYS_kAudioUnknown;
                    M4OSA_TRACE1_0(
                        "M4VSS3GPP_intAudioMixingOpen: No audio track in input file.");
                    return M4VSS3GPP_ERR_AUDIO_CANNOT_BE_MIXED;
                    break;
            }

            /* Add a link to audio dsi */
            pC->ewc.uiAudioOutputDsiSize =
                (M4OSA_UInt16)pC->pInputClipCtxt->pAudioStream->
                m_basicProperties.m_decoderSpecificInfoSize;
            pC->ewc.pAudioOutputDsi = (M4OSA_MemAddr8)pC->pInputClipCtxt->pAudioStream->
                m_basicProperties.m_pDecoderSpecificInfo;
        }
    }

    /**
    * Copy common 'silence frame stuff' to ClipContext */
    pC->pInputClipCtxt->uiSilencePcmSize = pC->ewc.uiSilencePcmSize;
    pC->pInputClipCtxt->pSilenceFrameData = pC->ewc.pSilenceFrameData;
    pC->pInputClipCtxt->uiSilenceFrameSize = pC->ewc.uiSilenceFrameSize;
    pC->pInputClipCtxt->iSilenceFrameDuration = pC->ewc.iSilenceFrameDuration;
    pC->pInputClipCtxt->scale_audio = pC->ewc.scale_audio;

    pC->pInputClipCtxt->iAudioFrameCts =
        -pC->pInputClipCtxt->iSilenceFrameDuration; /* Reset time */

    /**
    * Copy common 'silence frame stuff' to ClipContext */
    if( pC->bHasAudio )
    {
        pC->pAddedClipCtxt->uiSilencePcmSize = pC->ewc.uiSilencePcmSize;
        pC->pAddedClipCtxt->pSilenceFrameData = pC->ewc.pSilenceFrameData;
        pC->pAddedClipCtxt->uiSilenceFrameSize = pC->ewc.uiSilenceFrameSize;
        pC->pAddedClipCtxt->iSilenceFrameDuration =
            pC->ewc.iSilenceFrameDuration;
        pC->pAddedClipCtxt->scale_audio = pC->ewc.scale_audio;

        pC->pAddedClipCtxt->iAudioFrameCts =
            -pC->pAddedClipCtxt->iSilenceFrameDuration; /* Reset time */
    }

    /**
    * Check AddCts is lower than original clip duration */
    if( ( M4OSA_NULL != pC->pInputClipCtxt->pVideoStream)
        && (pC->iAddCts > (M4OSA_Int32)pC->pInputClipCtxt->pVideoStream->
        m_basicProperties.m_duration) )
    {
        M4OSA_TRACE1_0(
            "M4VSS3GPP_intAudioMixingOpen(): uiAddCts is larger than video duration,\
            returning M4VSS3GPP_ERR_ADDCTS_HIGHER_THAN_VIDEO_DURATION");
        return M4VSS3GPP_ERR_ADDCTS_HIGHER_THAN_VIDEO_DURATION;
    }

    /**
    * If the audio tracks are not compatible, replace input track by silence */
    if( M4OSA_FALSE == pC->pInputClipCtxt->pSettings->
        ClipProperties.bAudioIsCompatibleWithMasterClip )
    {
        M4VSS3GPP_intClipDeleteAudioTrack(pC->pInputClipCtxt);
    }

    /**
    * Check if audio mixing is required */
    if( ( ( pC->bHasAudio) && (M4OSA_FALSE
        == pC->pAddedClipCtxt->pSettings->ClipProperties.bAudioIsEditable))
        || (M4OSA_TRUE == pC->bRemoveOriginal) ) /*||
                                                 (pSettings->uiAddVolume >= 100)) */
    {
        pC->bAudioMixingIsNeeded = M4OSA_FALSE;
    }
    else
    {
        pC->bAudioMixingIsNeeded = M4OSA_TRUE;
    }

    /**
    * Check if output audio can support silence frames
    Trick i use bAudioIsCompatibleWithMasterClip filed to store that  */
    if( pC->bHasAudio )
    {
        pC->bSupportSilence = pC->pAddedClipCtxt->pSettings->
            ClipProperties.bAudioIsCompatibleWithMasterClip;

        if( M4OSA_FALSE == pC->bSupportSilence )
        {
            if( pC->iAddCts > 0 )
            {
                M4OSA_TRACE1_0(
                    "M4VSS3GPP_intAudioMixingOpen():\
                    iAddCts should be set to 0 with this audio track !");
                return M4VSS3GPP_ERR_FEATURE_UNSUPPORTED_WITH_AUDIO_TRACK;
            }

            if( 0 < pC->uiEndLoop )
            {
                M4OSA_TRACE1_0(
                    "M4VSS3GPP_intAudioMixingOpen():\
                    uiEndLoop should be set to 0 with this audio track !");
                return M4VSS3GPP_ERR_FEATURE_UNSUPPORTED_WITH_AUDIO_TRACK;
            }
        }
    }
    if( pC->b_DuckingNeedeed == M4OSA_FALSE)
    {
        /**
        * Compute the factor to apply to sample to do the mixing */
        pC->fAddedFactor = 0.50F;
        pC->fOrigFactor = 0.50F;
    }


    /**
    * Check if SSRC is needed */
    if( M4OSA_TRUE == pC->b_SSRCneeded )
    {
        M4OSA_UInt32 numerator, denominator, ratio, ratioBuffer;

        /**
        * Init the SSRC module */
        SSRC_ReturnStatus_en
            ReturnStatus; /* Function return status                       */
        LVM_INT16 NrSamplesMin =
            0; /* Minimal number of samples on the input or on the output */
        LVM_INT32
            ScratchSize; /* The size of the scratch memory               */
        LVM_INT16
            *pInputInScratch; /* Pointer to input in the scratch buffer       */
        LVM_INT16
            *
            pOutputInScratch; /* Pointer to the output in the scratch buffer  */
        SSRC_Params_t ssrcParams;          /* Memory for init parameters                    */

        switch( pC->pAddedClipCtxt->pSettings->
            ClipProperties.uiSamplingFrequency )
        {
            case 8000:
                ssrcParams.SSRC_Fs_In = LVM_FS_8000;
                break;

            case 11025:
                ssrcParams.SSRC_Fs_In = LVM_FS_11025;
                break;

            case 12000:
                ssrcParams.SSRC_Fs_In = LVM_FS_12000;
                break;

            case 16000:
                ssrcParams.SSRC_Fs_In = LVM_FS_16000;
                break;

            case 22050:
                ssrcParams.SSRC_Fs_In = LVM_FS_22050;
                break;

            case 24000:
                ssrcParams.SSRC_Fs_In = LVM_FS_24000;
                break;

            case 32000:
                ssrcParams.SSRC_Fs_In = LVM_FS_32000;
                break;

            case 44100:
                ssrcParams.SSRC_Fs_In = LVM_FS_44100;
                break;

            case 48000:
                ssrcParams.SSRC_Fs_In = LVM_FS_48000;
                break;

            default:
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intAudioMixingOpen: invalid added clip sampling frequency (%d Hz),\
                    returning M4VSS3GPP_ERR_UNSUPPORTED_ADDED_AUDIO_STREAM",
                    pC->pAddedClipCtxt->pSettings->
                    ClipProperties.uiSamplingFrequency);
                return M4VSS3GPP_ERR_UNSUPPORTED_ADDED_AUDIO_STREAM;
        }

        if( 1 == pC->pAddedClipCtxt->pSettings->ClipProperties.uiNbChannels )
        {
            ssrcParams.SSRC_NrOfChannels = LVM_MONO;
        }
        else
        {
            ssrcParams.SSRC_NrOfChannels = LVM_STEREO;
        }

        switch( pC->ewc.uiSamplingFrequency )
        {
            case 8000:
                ssrcParams.SSRC_Fs_Out = LVM_FS_8000;
                break;

            case 16000:
                ssrcParams.SSRC_Fs_Out = LVM_FS_16000;
                break;

            case 22050:
                ssrcParams.SSRC_Fs_Out = LVM_FS_22050;
                break;

            case 24000:
                ssrcParams.SSRC_Fs_Out = LVM_FS_24000;
                break;

            case 32000:
                ssrcParams.SSRC_Fs_Out = LVM_FS_32000;
                break;

            case 44100:
                ssrcParams.SSRC_Fs_Out = LVM_FS_44100;
                break;

            case 48000:
                ssrcParams.SSRC_Fs_Out = LVM_FS_48000;
                break;

            default:
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intAudioMixingOpen: invalid output sampling frequency (%d Hz),\
                    returning M4VSS3GPP_ERR_AUDIO_CANNOT_BE_MIXED",
                    pC->ewc.uiSamplingFrequency);
                return M4VSS3GPP_ERR_AUDIO_CANNOT_BE_MIXED;
                break;
        }
        ReturnStatus = 0;

        switch (ssrcParams.SSRC_Fs_In){
        case LVM_FS_8000:
            ssrcParams.NrSamplesIn = 320;
            break;
        case LVM_FS_11025:
            ssrcParams.NrSamplesIn =441;
            break;
        case LVM_FS_12000:
            ssrcParams.NrSamplesIn =    480;
            break;
        case LVM_FS_16000:
            ssrcParams.NrSamplesIn =    640;
            break;
        case LVM_FS_22050:
            ssrcParams.NrSamplesIn =    882;
            break;
        case LVM_FS_24000:
            ssrcParams.NrSamplesIn =    960;
            break;
        case LVM_FS_32000:
            ssrcParams.NrSamplesIn = 1280;
            break;
        case LVM_FS_44100:
            ssrcParams.NrSamplesIn = 1764;
            break;
        case LVM_FS_48000:
            ssrcParams.NrSamplesIn = 1920;
            break;
        default:
            ReturnStatus = -1;
            break;
        }

        switch (ssrcParams.SSRC_Fs_Out){
        case LVM_FS_8000:
            ssrcParams.NrSamplesOut= 320;
            break;
        case LVM_FS_11025:
            ssrcParams.NrSamplesOut =441;
            break;
        case LVM_FS_12000:
            ssrcParams.NrSamplesOut=    480;
            break;
        case LVM_FS_16000:
            ssrcParams.NrSamplesOut=    640;
            break;
        case LVM_FS_22050:
            ssrcParams.NrSamplesOut=    882;
            break;
        case LVM_FS_24000:
            ssrcParams.NrSamplesOut=    960;
            break;
        case LVM_FS_32000:
            ssrcParams.NrSamplesOut = 1280;
            break;
        case LVM_FS_44100:
            ssrcParams.NrSamplesOut= 1764;
            break;
        case LVM_FS_48000:
            ssrcParams.NrSamplesOut = 1920;
            break;
        default:
            ReturnStatus = -1;
            break;
        }
        if( ReturnStatus != SSRC_OK )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingOpen:\
                Error code %d returned by the SSRC_GetNrSamples function",
                ReturnStatus);
            return M4VSS3GPP_ERR_AUDIO_CANNOT_BE_MIXED;
        }

        NrSamplesMin =
            (LVM_INT16)((ssrcParams.NrSamplesIn > ssrcParams.NrSamplesOut)
            ? ssrcParams.NrSamplesOut : ssrcParams.NrSamplesIn);

        while( NrSamplesMin < M4VSS_SSRC_MINBLOCKSIZE )
        { /* Don't take blocks smaller that the minimal block size */
            ssrcParams.NrSamplesIn = (LVM_INT16)(ssrcParams.NrSamplesIn << 1);
            ssrcParams.NrSamplesOut = (LVM_INT16)(ssrcParams.NrSamplesOut << 1);
            NrSamplesMin = (LVM_INT16)(NrSamplesMin << 1);
        }
        pC->iSsrcNbSamplIn = (LVM_INT16)(
            ssrcParams.
            NrSamplesIn); /* multiplication by NrOfChannels is done below */
        pC->iSsrcNbSamplOut = (LVM_INT16)(ssrcParams.NrSamplesOut);

        numerator =
            pC->pAddedClipCtxt->pSettings->ClipProperties.uiSamplingFrequency
            * pC->pAddedClipCtxt->pSettings->ClipProperties.uiNbChannels;
        denominator =
            pC->pInputClipCtxt->pSettings->ClipProperties.uiSamplingFrequency
            * pC->pInputClipCtxt->pSettings->ClipProperties.uiNbChannels;

        if( numerator % denominator == 0 )
        {
            ratioBuffer = (M4OSA_UInt32)(numerator / denominator);
        }
        else
        {
            ratioBuffer = (M4OSA_UInt32)(numerator / denominator) + 1;
        }

        ratio =
            (M4OSA_UInt32)(( pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize
            * ratioBuffer) / (pC->iSsrcNbSamplIn * sizeof(short)
            * pC->pAddedClipCtxt->pSettings->
            ClipProperties.uiNbChannels));

        if( ratio == 0 )
        {
            /* It means that the input size of SSRC bufferIn is bigger than the asked buffer */
            pC->minimumBufferIn = pC->iSsrcNbSamplIn * sizeof(short)
                * pC->pAddedClipCtxt->pSettings->
                ClipProperties.uiNbChannels;
        }
        else
        {
            ratio++; /* We use the immediate superior integer */
            pC->minimumBufferIn = ratio * (pC->iSsrcNbSamplIn * sizeof(short)
                * pC->pAddedClipCtxt->pSettings->
                ClipProperties.uiNbChannels);
        }

        /**
        * Allocate buffer for the input of the SSRC */
        pC->pSsrcBufferIn =
            (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(pC->minimumBufferIn
            + pC->pAddedClipCtxt->
            AudioDecBufferOut.
            m_bufferSize,
            M4VSS3GPP, (M4OSA_Char *)"pSsrcBufferIn");

        if( M4OSA_NULL == pC->pSsrcBufferIn )
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_intAudioMixingOpen():\
                unable to allocate pSsrcBufferIn, returning M4ERR_ALLOC");
            return M4ERR_ALLOC;
        }
        pC->pPosInSsrcBufferIn = (M4OSA_MemAddr8)pC->pSsrcBufferIn;

        /**
        * Allocate buffer for the output of the SSRC */
        /* The "3" value below should be optimized ... one day ... */
        pC->pSsrcBufferOut =
            (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(3 * pC->iSsrcNbSamplOut * sizeof(short)
            * pC->ewc.uiNbChannels, M4VSS3GPP, (M4OSA_Char *)"pSsrcBufferOut");

        if( M4OSA_NULL == pC->pSsrcBufferOut )
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_intAudioMixingOpen():\
                unable to allocate pSsrcBufferOut, returning M4ERR_ALLOC");
            return M4ERR_ALLOC;
        }
        pC->pPosInSsrcBufferOut = pC->pSsrcBufferOut;

        /**
        * Allocate temporary buffer needed in case of channel conversion */
        if( pC->ChannelConversion > 0 )
        {
            /* The "3" value below should be optimized ... one day ... */
            pC->pTempBuffer =
                (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(3 * pC->iSsrcNbSamplOut
                * sizeof(short) * pC->pAddedClipCtxt->pSettings->
                ClipProperties.uiNbChannels, M4VSS3GPP, (M4OSA_Char *)"pSsrcBufferOut");

            if( M4OSA_NULL == pC->pTempBuffer )
            {
                M4OSA_TRACE1_0(
                    "M4VSS3GPP_intAudioMixingOpen():\
                    unable to allocate pTempBuffer, returning M4ERR_ALLOC");
                return M4ERR_ALLOC;
            }
            pC->pPosInTempBuffer = pC->pTempBuffer;
        }
    }
    else if( pC->ChannelConversion > 0 )
    {
        pC->minimumBufferIn =
            pC->pAddedClipCtxt->AudioDecBufferOut.m_bufferSize;

        /**
        * Allocate buffer for the input of the SSRC */
        pC->pSsrcBufferIn =
            (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(pC->minimumBufferIn
            + pC->pAddedClipCtxt->
            AudioDecBufferOut.
            m_bufferSize,
            M4VSS3GPP, (M4OSA_Char *)"pSsrcBufferIn");

        if( M4OSA_NULL == pC->pSsrcBufferIn )
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_intAudioMixingOpen(): \
                unable to allocate pSsrcBufferIn, returning M4ERR_ALLOC");
            return M4ERR_ALLOC;
        }
        pC->pPosInSsrcBufferIn = (M4OSA_MemAddr8)pC->pSsrcBufferIn;

        /**
        * Allocate buffer for the output of the SSRC */
        /* The "3" value below should be optimized ... one day ... */
        pC->pSsrcBufferOut = (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(
            pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize,
            M4VSS3GPP, (M4OSA_Char *)"pSsrcBufferOut");

        if( M4OSA_NULL == pC->pSsrcBufferOut )
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_intAudioMixingOpen():\
                unable to allocate pSsrcBufferOut, returning M4ERR_ALLOC");
            return M4ERR_ALLOC;
        }
        pC->pPosInSsrcBufferOut = pC->pSsrcBufferOut;
    }
    else if( (pC->pAddedClipCtxt->pSettings->ClipProperties.AudioStreamType == M4VIDEOEDITING_kMP3)||
         (pC->pAddedClipCtxt->pSettings->ClipProperties.AudioStreamType == M4VIDEOEDITING_kPCM))
    {
        M4OSA_UInt32 minbuffer = 0;

        if( pSettings->outputAudioFormat == M4VIDEOEDITING_kAAC )
        {
            pC->minimumBufferIn = 2048 * pC->ewc.uiNbChannels;
            minbuffer = pC->minimumBufferIn;
        }
        else if( pSettings->outputAudioFormat == M4VIDEOEDITING_kAMR_NB )
        {
            pC->minimumBufferIn = 320;

            if( pC->pAddedClipCtxt->AudioDecBufferOut.m_bufferSize > 320 )
            {
                minbuffer = pC->pAddedClipCtxt->AudioDecBufferOut.m_bufferSize;
            }
            else
            {
                minbuffer = pC->minimumBufferIn; /* Not really possible ...*/
            }
        }
        else
        {
            M4OSA_TRACE1_0("Bad output audio format, in case of MP3 replacing");
            return M4ERR_PARAMETER;
        }

        /**
        * Allocate buffer for the input of the SSRC */
        pC->pSsrcBufferIn =
            (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(2 * minbuffer, M4VSS3GPP,
            (M4OSA_Char *)"pSsrcBufferIn");

        if( M4OSA_NULL == pC->pSsrcBufferIn )
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_intAudioMixingOpen(): unable to allocate pSsrcBufferIn,\
                returning M4ERR_ALLOC");
            return M4ERR_ALLOC;
        }
        pC->pPosInSsrcBufferIn = (M4OSA_MemAddr8)pC->pSsrcBufferIn;

        pC->pPosInSsrcBufferOut = pC->pPosInSsrcBufferIn;
        pC->pSsrcBufferOut = pC->pSsrcBufferIn;
    }

    /**
    * Check if audio encoder is needed to do audio mixing or audio resampling */
    if( M4OSA_TRUE == pC->bAudioMixingIsNeeded || M4VIDEOEDITING_kPCM
        == pC->pAddedClipCtxt->pSettings->ClipProperties.AudioStreamType
        || M4VIDEOEDITING_kMP3
        == pC->pAddedClipCtxt->pSettings->ClipProperties.AudioStreamType
        || pC->pAddedClipCtxt->pSettings->ClipProperties.AudioStreamType
        != pSettings->outputAudioFormat
        || pC->pAddedClipCtxt->pSettings->ClipProperties.uiSamplingFrequency
        != outputASF
        || pC->pAddedClipCtxt->pSettings->ClipProperties.uiNbChannels
        != pSettings->outputNBChannels )
    {
        /**
        * Init the audio encoder */
        err = M4VSS3GPP_intCreateAudioEncoder(&pC->ewc, &pC->ShellAPI,
            pC->ewc.uiAudioBitrate);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingOpen(): M4VSS3GPP_intCreateAudioEncoder() returns 0x%x",
                err);
            return err;
        }

        /* In case of PCM, MP3 or audio replace with reencoding, use encoder DSI */
        if( pC->ewc.uiAudioOutputDsiSize == 0 && (M4VIDEOEDITING_kPCM
            == pC->pAddedClipCtxt->pSettings->ClipProperties.AudioStreamType
            || M4VIDEOEDITING_kMP3 == pC->pAddedClipCtxt->pSettings->
            ClipProperties.AudioStreamType
            || pC->pAddedClipCtxt->pSettings->
            ClipProperties.AudioStreamType
            != pSettings->outputAudioFormat
            || pC->pAddedClipCtxt->pSettings->
            ClipProperties.uiSamplingFrequency != outputASF
            || pC->pAddedClipCtxt->pSettings->
            ClipProperties.uiNbChannels
            != pSettings->outputNBChannels) )
        {
            pC->ewc.uiAudioOutputDsiSize =
                (M4OSA_UInt16)pC->ewc.pAudioEncDSI.infoSize;
            pC->ewc.pAudioOutputDsi = pC->ewc.pAudioEncDSI.pInfo;
        }
    }

    /**
    * Init the output 3GPP file */
    /*11/12/2008 CR3283 add the max output file size for the MMS use case in VideoArtist*/
    err = M4VSS3GPP_intCreate3GPPOutputFile(&pC->ewc, &pC->ShellAPI,
        pC->pOsaFileWritPtr, pSettings->pOutputClipFile,
        pC->pOsaFileReadPtr, pSettings->pTemporaryFile, 0);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intAudioMixingOpen(): M4VSS3GPP_intCreate3GPPOutputFile() returns 0x%x",
            err);
        return err;
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_intAudioMixingOpen(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intAudioMixingWriteSilence()
 * @brief    Write an audio silence frame into the writer
 * @note    Mainly used when padding with silence
 * @param    pC            (IN) VSS audio mixing internal context
 * @return    M4NO_ERROR:    No error
 ******************************************************************************
 */
static M4OSA_ERR M4VSS3GPP_intAudioMixingWriteSilence(
    M4VSS3GPP_InternalAudioMixingContext *pC )
{
    M4OSA_ERR err;

    err = pC->ShellAPI.pWriterDataFcts->pStartAU(pC->ewc.p3gpWriterContext,
        M4VSS3GPP_WRITER_AUDIO_STREAM_ID, &pC->ewc.WriterAudioAU);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1("M4VSS3GPP_intAudioMixingWriteSilence:\
         pWriterDataFcts->pStartAU(audio) returns 0x%x!", err);
        return err;
    }

    M4OSA_TRACE2_0("A #### silence AU");

    memcpy((void *)pC->ewc.WriterAudioAU.dataAddress,
        (void *)pC->ewc.pSilenceFrameData, pC->ewc.uiSilenceFrameSize);

    pC->ewc.WriterAudioAU.size = pC->ewc.uiSilenceFrameSize;
    pC->ewc.WriterAudioAU.CTS =
        (M4OSA_Time)(pC->ewc.dATo * pC->ewc.scale_audio + 0.5);

    M4OSA_TRACE2_2("B ---- write : cts  = %ld [ 0x%x ]",
        (M4OSA_Int32)(pC->ewc.dATo), pC->ewc.WriterAudioAU.size);

    err = pC->ShellAPI.pWriterDataFcts->pProcessAU(pC->ewc.p3gpWriterContext,
        M4VSS3GPP_WRITER_AUDIO_STREAM_ID, &pC->ewc.WriterAudioAU);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intAudioMixingWriteSilence:\
            pWriterDataFcts->pProcessAU(silence) returns 0x%x!",
            err);
        return err;
    }

    pC->ewc.dATo += pC->ewc.iSilenceFrameDuration / pC->ewc.scale_audio;

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intAudioMixingStepVideo(M4VSS3GPP_InternalAudioMixingContext *pC)
 * @brief    Perform one step of video.
 * @note
 * @param    pC            (IN) VSS audio mixing internal context
 * @return    M4NO_ERROR:    No error
 ******************************************************************************
 */
static M4OSA_ERR M4VSS3GPP_intAudioMixingStepVideo(
    M4VSS3GPP_InternalAudioMixingContext *pC )
{
    M4OSA_ERR err;
    M4OSA_UInt16 offset;

    M4OSA_TRACE2_3("  VIDEO step : dVTo = %f  state = %d  offset = %ld",
        pC->ewc.dOutputVidCts, pC->State, pC->pInputClipCtxt->iVoffset);

    /**
    * Read the input video AU */
    err = pC->pInputClipCtxt->ShellAPI.m_pReaderDataIt->m_pFctGetNextAu(
        pC->pInputClipCtxt->pReaderContext,
        (M4_StreamHandler *)pC->pInputClipCtxt->pVideoStream,
        &pC->pInputClipCtxt->VideoAU);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE3_1(
            "M4VSS3GPP_intAudioMixingStepVideo(): m_pFctGetNextAu(video) returns 0x%x",
            err);
        return err;
    }

    M4OSA_TRACE2_3("C .... read  : cts  = %.0f + %ld [ 0x%x ]",
        pC->pInputClipCtxt->VideoAU.m_CTS, pC->pInputClipCtxt->iVoffset,
        pC->pInputClipCtxt->VideoAU.m_size);

    /**
    * Get the output AU to write into */
    err = pC->ShellAPI.pWriterDataFcts->pStartAU(pC->ewc.p3gpWriterContext,
        M4VSS3GPP_WRITER_VIDEO_STREAM_ID, &pC->ewc.WriterVideoAU);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intAudioMixingStepVideo: pWriterDataFcts->pStartAU(Video) returns 0x%x!",
            err);
        return err;
    }

    offset = 0;
    /* for h.264 stream do not read the 1st 4 bytes as they are header indicators */
    if( pC->pInputClipCtxt->pVideoStream->m_basicProperties.m_streamType
        == M4DA_StreamTypeVideoMpeg4Avc )
    {
        M4OSA_TRACE3_0(
            "M4VSS3GPP_intAudioMixingStepVideo(): input stream type H264");
        offset = 4;
    }
    pC->pInputClipCtxt->VideoAU.m_size  -=  offset;
    /**
    * Check that the video AU is not larger than expected */
    if( pC->pInputClipCtxt->VideoAU.m_size > pC->ewc.uiVideoMaxAuSize )
    {
        M4OSA_TRACE1_2(
            "M4VSS3GPP_intAudioMixingStepVideo: AU size greater than MaxAuSize (%d>%d)!\
            returning M4VSS3GPP_ERR_INPUT_VIDEO_AU_TOO_LARGE",
            pC->pInputClipCtxt->VideoAU.m_size, pC->ewc.uiVideoMaxAuSize);
        return M4VSS3GPP_ERR_INPUT_VIDEO_AU_TOO_LARGE;
    }

    /**
    * Copy the input AU payload to the output AU */
    memcpy((void *)pC->ewc.WriterVideoAU.dataAddress,
        (void *)(pC->pInputClipCtxt->VideoAU.m_dataAddress + offset),
        (pC->pInputClipCtxt->VideoAU.m_size));

    /**
    * Copy the input AU parameters to the output AU */
    pC->ewc.WriterVideoAU.size = pC->pInputClipCtxt->VideoAU.m_size;
    pC->ewc.WriterVideoAU.CTS =
        (M4OSA_UInt32)(pC->pInputClipCtxt->VideoAU.m_CTS + 0.5);
    pC->ewc.WriterVideoAU.attribute = pC->pInputClipCtxt->VideoAU.m_attribute;

    /**
    * Write the AU */
    M4OSA_TRACE2_2("D ---- write : cts  = %lu [ 0x%x ]",
        pC->ewc.WriterVideoAU.CTS, pC->ewc.WriterVideoAU.size);

    err = pC->ShellAPI.pWriterDataFcts->pProcessAU(pC->ewc.p3gpWriterContext,
        M4VSS3GPP_WRITER_VIDEO_STREAM_ID, &pC->ewc.WriterVideoAU);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intAudioMixingStepVideo: pWriterDataFcts->pProcessAU(Video) returns 0x%x!",
            err);
        return err;
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_intAudioMixingStepVideo(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intAudioMixingStepAudioMix(M4VSS3GPP_InternalAudioMixingContext *pC)
 * @brief    Perform one step of audio.
 * @note
 * @param    pC            (IN) VSS audio mixing internal context
 * @return    M4NO_ERROR:    No error
 ******************************************************************************
 */
static M4OSA_ERR M4VSS3GPP_intAudioMixingStepAudioMix(
    M4VSS3GPP_InternalAudioMixingContext *pC )
{
    M4OSA_ERR err;

    M4OSA_TRACE2_3("  AUDIO mix  : dATo = %f  state = %d  offset = %ld",
        pC->ewc.dATo, pC->State, pC->pInputClipCtxt->iAoffset);

    switch( pC->State )
    {
        /**********************************************************/
        case M4VSS3GPP_kAudioMixingState_AUDIO_FIRST_SEGMENT:
            {
                err = M4VSS3GPP_intAudioMixingCopyOrig(pC);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intAudioMixingStepAudioMix:\
                        M4VSS3GPP_intAudioMixingCopyOrig(1) returns 0x%x!",
                        err);
                    return err;
                }

                /**
                * Check if we reached the AddCts */
                if( pC->ewc.dATo >= pC->iAddCts )
                {
                    /**
                    * First segment is over, state transition to second and return OK */
                    pC->State = M4VSS3GPP_kAudioMixingState_AUDIO_SECOND_SEGMENT;

                    /* Transition from reading state to encoding state */
                    err = M4VSS3GPP_intAudioMixingTransition(pC);

                    if( M4NO_ERROR != err )
                    {
                        M4OSA_TRACE1_1(
                            "M4VSS3GPP_intAudioMixingStepAudioMix(): pre-encode fails err = 0x%x",
                            err);
                        return err;
                    }

                    /**
                    * Return with no error so the step function will be called again */
                    pC->pAddedClipCtxt->iAoffset =
                        (M4OSA_Int32)(pC->ewc.dATo * pC->ewc.scale_audio + 0.5);

                    M4OSA_TRACE2_0(
                        "M4VSS3GPP_intAudioMixingStepAudioMix(): returning M4NO_ERROR (1->2)");

                    return M4NO_ERROR;
                }
            }
            break;

            /**********************************************************/
        case M4VSS3GPP_kAudioMixingState_AUDIO_SECOND_SEGMENT:
            {
                if( M4OSA_TRUE == pC->bAudioMixingIsNeeded ) /**< Mix */
                {
                    /**
                    * Read the added audio AU */
                    if( pC->ChannelConversion > 0 || pC->b_SSRCneeded == M4OSA_TRUE
                        || pC->pAddedClipCtxt->pSettings->
                        ClipProperties.AudioStreamType == M4VIDEOEDITING_kMP3 )
                    {
                        /* In case of sampling freq conversion and/or channel conversion,
                           the read next AU will be    called by the
                           M4VSS3GPP_intAudioMixingDoMixing function */
                    }
                    else
                    {
                        err =
                            M4VSS3GPP_intClipReadNextAudioFrame(pC->pAddedClipCtxt);

                        M4OSA_TRACE2_3("E .... read  : cts  = %.0f + %.0f [ 0x%x ]",
                            pC->pAddedClipCtxt->iAudioFrameCts
                            / pC->pAddedClipCtxt->scale_audio,
                            pC->pAddedClipCtxt->iAoffset
                            / pC->pAddedClipCtxt->scale_audio,
                            pC->pAddedClipCtxt->uiAudioFrameSize);

                        if( M4WAR_NO_MORE_AU == err )
                        {
                            /**
                            * Decide what to do when audio is over */
                            if( pC->uiEndLoop > 0 )
                            {
                                /**
                                * Jump at the Begin loop time */
                                M4OSA_Int32 time = (M4OSA_Int32)(pC->uiBeginLoop);

                                err = pC->pAddedClipCtxt->ShellAPI.m_pReader->
                                    m_pFctJump(
                                    pC->pAddedClipCtxt->pReaderContext,
                                    (M4_StreamHandler
                                    *)pC->pAddedClipCtxt->pAudioStream,
                                    &time);

                                if( M4NO_ERROR != err )
                                {
                                    M4OSA_TRACE1_1(
                                        "M4VSS3GPP_intAudioMixingStepAudioMix():\
                                        m_pReader->m_pFctJump(audio returns 0x%x",
                                        err);
                                    return err;
                                }
                            }
                            else
                            {
                                /* Transition from encoding state to reading state */
                                err = M4VSS3GPP_intAudioMixingTransition(pC);

                                if( M4NO_ERROR != err )
                                {
                                    M4OSA_TRACE1_1(
                                        "M4VSS3GPP_intAudioMixingStepAudioMix():\
                                        pre-encode fails err = 0x%x",
                                        err);
                                    return err;
                                }

                                /**
                                * Second segment is over, state transition to third and
                                 return OK */
                                pC->State =
                                    M4VSS3GPP_kAudioMixingState_AUDIO_THIRD_SEGMENT;

                                /**
                                * Return with no error so the step function will be
                                 called again */
                                M4OSA_TRACE2_0(
                                    "M4VSS3GPP_intAudioMixingStepAudioMix():\
                                    returning M4NO_ERROR (2->3) a");
                                return M4NO_ERROR;
                            }
                        }
                        else if( M4NO_ERROR != err )
                        {
                            M4OSA_TRACE1_1(
                                "M4VSS3GPP_intAudioMixingStepAudioMix():\
                                m_pFctGetNextAu(audio) returns 0x%x",
                                err);
                            return err;
                        }
                    }

                    /**
                    * Read the original audio AU */
                    err = M4VSS3GPP_intClipReadNextAudioFrame(pC->pInputClipCtxt);

                    M4OSA_TRACE2_3("F .... read  : cts  = %.0f + %.0f [ 0x%x ]",
                        pC->pInputClipCtxt->iAudioFrameCts
                        / pC->pInputClipCtxt->scale_audio,
                        pC->pInputClipCtxt->iAoffset
                        / pC->pInputClipCtxt->scale_audio,
                        pC->pInputClipCtxt->uiAudioFrameSize);

                    if( M4NO_ERROR != err )
                    {
                        M4OSA_TRACE3_1(
                            "M4VSS3GPP_intAudioMixingStepAudioMix():\
                            m_pFctGetNextAu(audio) returns 0x%x",
                            err);
                        return err;
                    }

                    if( pC->ChannelConversion == 0
                        && pC->b_SSRCneeded == M4OSA_FALSE
                        && pC->pAddedClipCtxt->pSettings->
                        ClipProperties.AudioStreamType != M4VIDEOEDITING_kMP3 )
                    {
                        /**
                        * Get the output AU to write into */
                        err = pC->ShellAPI.pWriterDataFcts->pStartAU(
                            pC->ewc.p3gpWriterContext,
                            M4VSS3GPP_WRITER_AUDIO_STREAM_ID,
                            &pC->ewc.WriterAudioAU);

                        if( M4NO_ERROR != err )
                        {
                            M4OSA_TRACE1_1(
                                "M4VSS3GPP_intAudioMixingStepAudioMix:\
                                pWriterDataFcts->pStartAU(audio) returns 0x%x!",
                                err);
                            return err;
                        }
                    }

                    /**
                    * Perform the audio mixing */
                    err = M4VSS3GPP_intAudioMixingDoMixing(pC);

                    if( err == M4VSS3GPP_WAR_END_OF_ADDED_AUDIO )
                    {
                        return M4NO_ERROR;
                    }

                    if( M4NO_ERROR != err )
                    {
                        M4OSA_TRACE1_1(
                            "M4VSS3GPP_intAudioMixingStepAudioMix:\
                            M4VSS3GPP_intAudioMixingDoMixing returns 0x%x!",
                            err);
                        return err;
                    }
                }
                else /**< No mix, just copy added audio */
                {
                    err = M4VSS3GPP_intAudioMixingCopyAdded(pC);

                    if( M4WAR_NO_MORE_AU == err )
                    {
                        /**
                        * Decide what to do when audio is over */
                        if( pC->uiEndLoop > 0 )
                        {
                            /**
                            * Jump at the Begin loop time */
                            M4OSA_Int32 time = (M4OSA_Int32)(pC->uiBeginLoop);

                            err =
                                pC->pAddedClipCtxt->ShellAPI.m_pReader->m_pFctJump(
                                pC->pAddedClipCtxt->pReaderContext,
                                (M4_StreamHandler
                                *)pC->pAddedClipCtxt->pAudioStream,
                                &time);

                            if( M4NO_ERROR != err )
                            {
                                M4OSA_TRACE1_1(
                                    "M4VSS3GPP_intAudioMixingStepAudioMix():\
                                    m_pReader->m_pFctJump(audio returns 0x%x",
                                    err);
                                return err;
                            }

                            /**
                            * 'BZZZ' bug fix:
                            * add a silence frame */
                            err = M4VSS3GPP_intAudioMixingWriteSilence(pC);

                            if( M4NO_ERROR != err )
                            {
                                M4OSA_TRACE1_1(
                                    "M4VSS3GPP_intAudioMixingStepAudioMix():\
                                    M4VSS3GPP_intAudioMixingWriteSilence returns 0x%x",
                                    err);
                                return err;
                            }

                            /**
                            * Return with no error so the step function will be called again to
                              read audio data */
                            pC->pAddedClipCtxt->iAoffset =
                                (M4OSA_Int32)(pC->ewc.dATo * pC->ewc.scale_audio
                                + 0.5);

                            M4OSA_TRACE2_0(
                                "M4VSS3GPP_intAudioMixingStepAudioMix():\
                                    returning M4NO_ERROR (loop)");
                            return M4NO_ERROR;
                        }
                        else
                        {
                            /* Transition to begin cut */
                            err = M4VSS3GPP_intAudioMixingTransition(pC);

                            if( M4NO_ERROR != err )
                            {
                                M4OSA_TRACE1_1(
                                    "M4VSS3GPP_intAudioMixingStepAudioMix():\
                                    pre-encode fails err = 0x%x",
                                    err);
                                return err;
                            }

                            /**
                            * Second segment is over, state transition to third */
                            pC->State =
                                M4VSS3GPP_kAudioMixingState_AUDIO_THIRD_SEGMENT;

                            /**
                            * Return with no error so the step function will be called again */
                            M4OSA_TRACE2_0(
                                "M4VSS3GPP_intAudioMixingStepAudioMix():\
                                returning M4NO_ERROR (2->3) b");
                            return M4NO_ERROR;
                        }
                    }
                    else if( M4NO_ERROR != err )
                    {
                        M4OSA_TRACE1_1(
                            "M4VSS3GPP_intAudioMixingStepAudioMix():\
                            M4VSS3GPP_intAudioMixingCopyOrig(2) returns 0x%x",
                            err);
                        return err;
                    }
                }

                /**
                * Check if we reached the end of the video */
                if( pC->ewc.dATo >= pC->ewc.iOutputDuration )
                {
                    M4OSA_TRACE3_0(
                        "M4VSS3GPP_intAudioMixingStepAudioMix(): Video duration reached,\
                        returning M4WAR_NO_MORE_AU");
                    return M4WAR_NO_MORE_AU; /**< Simulate end of file error */
                }
            }
            break;

            /**********************************************************/
        case M4VSS3GPP_kAudioMixingState_AUDIO_THIRD_SEGMENT:
            {
                err = M4VSS3GPP_intAudioMixingCopyOrig(pC);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intAudioMixingStepAudioMix:\
                        M4VSS3GPP_intAudioMixingCopyOrig(3) returns 0x%x!",
                        err);
                    return err;
                }

                /**
                * Check if we reached the end of the video */
                if( pC->ewc.dATo >= pC->ewc.iOutputDuration )
                {
                    M4OSA_TRACE3_0(
                        "M4VSS3GPP_intAudioMixingStepAudioMix():\
                        Video duration reached, returning M4WAR_NO_MORE_AU");
                    return M4WAR_NO_MORE_AU; /**< Simulate end of file error */
                }
            }
            break;
       default:
            break;
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0(
        "M4VSS3GPP_intAudioMixingStepAudioMix(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intAudioMixingStepAudioReplace(M4VSS3GPP_InternalAudioMixingContext *pC)
 * @brief    Perform one step of audio.
 * @note
 * @param    pC            (IN) VSS audio mixing internal context
 * @return    M4NO_ERROR:    No error
 ******************************************************************************
 */
static M4OSA_ERR M4VSS3GPP_intAudioMixingStepAudioReplace(
    M4VSS3GPP_InternalAudioMixingContext *pC )
{
    M4OSA_ERR err;

    M4OSA_TRACE2_3("  AUDIO repl : dATo = %f  state = %d  offset = %ld",
        pC->ewc.dATo, pC->State, pC->pInputClipCtxt->iAoffset);

    switch( pC->State )
    {
        /**********************************************************/
        case M4VSS3GPP_kAudioMixingState_AUDIO_FIRST_SEGMENT:
            {
                /**
                * Replace the SID (silence) payload in the writer AU */
                err = M4VSS3GPP_intAudioMixingWriteSilence(pC);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intAudioMixingStepAudioMix():\
                        M4VSS3GPP_intAudioMixingWriteSilence returns 0x%x",
                        err);
                    return err;
                }

                /**
                * Check if we reached the AddCts */
                if( pC->ewc.dATo >= pC->iAddCts )
                {
                    /**
                    * First segment is over, state transition to second and return OK */
                    pC->State = M4VSS3GPP_kAudioMixingState_AUDIO_SECOND_SEGMENT;

                    /**
                    * Return with no error so the step function will be called again */
                    pC->pAddedClipCtxt->iAoffset =
                        (M4OSA_Int32)(pC->ewc.dATo * pC->ewc.scale_audio + 0.5);

                    M4OSA_TRACE2_0("M4VSS3GPP_intAudioMixingStepAudioReplace():\
                         returning M4NO_ERROR (1->2)");
                    return M4NO_ERROR;
                }
            }
            break;

            /**********************************************************/
        case M4VSS3GPP_kAudioMixingState_AUDIO_SECOND_SEGMENT:
            {
                err = M4VSS3GPP_intAudioMixingCopyAdded(pC);

                if( M4WAR_NO_MORE_AU == err )
                {
                    /**
                    * Decide what to do when audio is over */

                    if( pC->uiEndLoop > 0 )
                    {
                        /**
                        * Jump at the Begin loop time */
                        M4OSA_Int32 time = (M4OSA_Int32)(pC->uiBeginLoop);

                        err = pC->pAddedClipCtxt->ShellAPI.m_pReader->m_pFctJump(
                            pC->pAddedClipCtxt->pReaderContext,
                            (M4_StreamHandler
                            *)pC->pAddedClipCtxt->pAudioStream, &time);

                        if( M4NO_ERROR != err )
                        {
                            M4OSA_TRACE1_1(
                                "M4VSS3GPP_intAudioMixingStepAudioReplace():\
                                m_pReader->m_pFctJump(audio returns 0x%x",
                                err);
                            return err;
                        }

                        /**
                        * 'BZZZ' bug fix:
                        * add a silence frame */
                        err = M4VSS3GPP_intAudioMixingWriteSilence(pC);

                        if( M4NO_ERROR != err )
                        {
                            M4OSA_TRACE1_1(
                                "M4VSS3GPP_intAudioMixingStepAudioMix():\
                                M4VSS3GPP_intAudioMixingWriteSilence returns 0x%x",
                                err);
                            return err;
                        }

                        /**
                        * Return with no error so the step function will be called again to
                          read audio data */
                        pC->pAddedClipCtxt->iAoffset =
                            (M4OSA_Int32)(pC->ewc.dATo * pC->ewc.scale_audio + 0.5);

                        M4OSA_TRACE2_0(
                            "M4VSS3GPP_intAudioMixingStepAudioReplace():\
                            returning M4NO_ERROR (loop)");

                        return M4NO_ERROR;
                    }
                    else if( M4OSA_TRUE == pC->bSupportSilence )
                    {
                        /**
                        * Second segment is over, state transition to third and return OK */
                        pC->State = M4VSS3GPP_kAudioMixingState_AUDIO_THIRD_SEGMENT;

                        /**
                        * Return with no error so the step function will be called again */
                        M4OSA_TRACE2_0(
                            "M4VSS3GPP_intAudioMixingStepAudioReplace():\
                                 returning M4NO_ERROR (2->3)");
                        return M4NO_ERROR;
                    }
                    else
                    {
                        /**
                        * The third segment (silence) is only done if supported.
                        * In other case, we finish here. */
                        pC->State = M4VSS3GPP_kAudioMixingState_FINISHED;

                        /**
                        * Return with no error so the step function will be called again */
                        M4OSA_TRACE2_0(
                            "M4VSS3GPP_intAudioMixingStepAudioReplace():\
                                 returning M4NO_ERROR (2->F)");
                        return M4NO_ERROR;
                    }
                }
                else if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intAudioMixingStepAudioReplace():\
                        M4VSS3GPP_intAudioMixingCopyOrig(2) returns 0x%x",
                        err);
                    return err;
                }

                /**
                * Check if we reached the end of the clip */
                if( pC->ewc.dATo >= pC->ewc.iOutputDuration )
                {
                    M4OSA_TRACE3_0(
                        "M4VSS3GPP_intAudioMixingStepAudioReplace(): Clip duration reached,\
                        returning M4WAR_NO_MORE_AU");
                    return M4WAR_NO_MORE_AU; /**< Simulate end of file error */
                }
            }
            break;

            /**********************************************************/
        case M4VSS3GPP_kAudioMixingState_AUDIO_THIRD_SEGMENT:
            {
                /**
                * Replace the SID (silence) payload in the writer AU */
                err = M4VSS3GPP_intAudioMixingWriteSilence(pC);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intAudioMixingStepAudioMix():\
                        M4VSS3GPP_intAudioMixingWriteSilence returns 0x%x",
                        err);
                    return err;
                }

                /**
                * Check if we reached the end of the video */
                if( pC->ewc.dATo >= pC->ewc.iOutputDuration )
                {
                    M4OSA_TRACE3_0(
                        "M4VSS3GPP_intAudioMixingStepAudioReplace():\
                        Video duration reached, returning M4WAR_NO_MORE_AU");
                    return M4WAR_NO_MORE_AU; /**< Simulate end of file error */
                }
            }
            break;
        default:
            break;
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0(
        "M4VSS3GPP_intAudioMixingStepAudioReplace(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intAudioMixingCopyOrig(M4VSS3GPP_InternalAudioMixingContext *pC)
 * @brief    Read one AU from the original audio file and write it to the output
 * @note
 * @param    pC    (IN) VSS audio mixing internal context
 ******************************************************************************
 */
static M4OSA_ERR M4VSS3GPP_intAudioMixingCopyOrig(
    M4VSS3GPP_InternalAudioMixingContext *pC )
{
    M4OSA_ERR err;

    /**
    * Read the input original audio AU */
    err = M4VSS3GPP_intClipReadNextAudioFrame(pC->pInputClipCtxt);

    M4OSA_TRACE2_3("G .... read  : cts  = %.0f + %.0f [ 0x%x ]",
        pC->pInputClipCtxt->iAudioFrameCts / pC->pInputClipCtxt->scale_audio,
        pC->pInputClipCtxt->iAoffset / pC->pInputClipCtxt->scale_audio,
        pC->pInputClipCtxt->uiAudioFrameSize);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE3_1(
            "M4VSS3GPP_intAudioMixingCopyOrig(): m_pFctGetNextAu(audio) returns 0x%x",
            err);
        return err;
    }

    /**
    * Get the output AU to write into */
    err = pC->ShellAPI.pWriterDataFcts->pStartAU(pC->ewc.p3gpWriterContext,
        M4VSS3GPP_WRITER_AUDIO_STREAM_ID, &pC->ewc.WriterAudioAU);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intAudioMixingCopyOrig: pWriterDataFcts->pStartAU(audio) returns 0x%x!",
            err);
        return err;
    }

    /**
    * Copy the input AU properties to the output AU */
    pC->ewc.WriterAudioAU.size = pC->pInputClipCtxt->uiAudioFrameSize;
    pC->ewc.WriterAudioAU.CTS =
        pC->pInputClipCtxt->iAudioFrameCts + pC->pInputClipCtxt->iAoffset;

    /**
    * Copy the AU itself */
    memcpy((void *)pC->ewc.WriterAudioAU.dataAddress,
        (void *)pC->pInputClipCtxt->pAudioFramePtr, pC->ewc.WriterAudioAU.size);

    /**
    * Write the mixed AU */
    M4OSA_TRACE2_2("H ---- write : cts  = %ld [ 0x%x ]",
        (M4OSA_Int32)(pC->ewc.WriterAudioAU.CTS / pC->ewc.scale_audio),
        pC->ewc.WriterAudioAU.size);

    err = pC->ShellAPI.pWriterDataFcts->pProcessAU(pC->ewc.p3gpWriterContext,
        M4VSS3GPP_WRITER_AUDIO_STREAM_ID, &pC->ewc.WriterAudioAU);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intAudioMixingCopyOrig: pWriterDataFcts->pProcessAU(audio) returns 0x%x!",
            err);
        return err;
    }

    /**
    * Increment the audio CTS for the next step */
    pC->ewc.dATo += pC->ewc.iSilenceFrameDuration / pC->ewc.scale_audio;

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_intAudioMixingCopyOrig(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intAudioMixingCopyAdded(M4VSS3GPP_InternalAudioMixingContext *pC)
 * @brief    Read one AU from the added audio file and write it to the output
 * @note
 * @param    pC    (IN) VSS audio mixing internal context
 ******************************************************************************
 */
static M4OSA_ERR M4VSS3GPP_intAudioMixingCopyAdded(
    M4VSS3GPP_InternalAudioMixingContext *pC )
{
    M4OSA_ERR err;

    if(pC->pAddedClipCtxt->pSettings->ClipProperties.AudioStreamType == M4VIDEOEDITING_kMP3 ||
        pC->pAddedClipCtxt->pSettings->ClipProperties.AudioStreamType == M4VIDEOEDITING_kPCM ||
        pC->b_SSRCneeded == M4OSA_TRUE ||
        pC->ChannelConversion > 0)
    {
        M4ENCODER_AudioBuffer pEncInBuffer; /**< Encoder input buffer for api */
        M4ENCODER_AudioBuffer
            pEncOutBuffer; /**< Encoder output buffer for api */
        M4OSA_Time
            frameTimeDelta; /**< Duration of the encoded (then written) data */
        M4OSA_MemAddr8 tempPosBuffer;

        err = M4VSS3GPP_intAudioMixingConvert(pC);

        if( err == M4VSS3GPP_WAR_END_OF_ADDED_AUDIO )
        {
            M4OSA_TRACE2_0(
                "M4VSS3GPP_intAudioMixingCopyAdded:\
                M4VSS3GPP_intAudioMixingConvert end of added file");
            return M4NO_ERROR;
        }
        else if( err != M4NO_ERROR )
        {
            M4OSA_TRACE1_1("M4VSS3GPP_intAudioMixingCopyAdded:\
                M4VSS3GPP_intAudioMixingConvert returned 0x%x", err);
            return err;
        }

        /**
        * Get the output AU to write into */
        err = pC->ShellAPI.pWriterDataFcts->pStartAU(pC->ewc.p3gpWriterContext,
            M4VSS3GPP_WRITER_AUDIO_STREAM_ID, &pC->ewc.WriterAudioAU);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingStepAudioMix:\
                pWriterDataFcts->pStartAU(audio) returns 0x%x!",
                err);
            return err;
        }

        /* [Mono] or [Stereo interleaved] : all is in one buffer */
        pEncInBuffer.pTableBuffer[0] = pC->pSsrcBufferOut;
        pEncInBuffer.pTableBufferSize[0] =
            pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize;
        pEncInBuffer.pTableBuffer[1] = M4OSA_NULL;
        pEncInBuffer.pTableBufferSize[1] = 0;

        /* Time in ms from data size, because it is PCM16 samples */
        frameTimeDelta = pEncInBuffer.pTableBufferSize[0] / sizeof(short)
            / pC->ewc.uiNbChannels;

        /**
        * Prepare output buffer */
        pEncOutBuffer.pTableBuffer[0] =
            (M4OSA_MemAddr8)pC->ewc.WriterAudioAU.dataAddress;
        pEncOutBuffer.pTableBufferSize[0] = 0;

        M4OSA_TRACE2_0("K **** blend AUs");
        /**
        * Encode the PCM audio */

        err = pC->ShellAPI.pAudioEncoderGlobalFcts->pFctStep(
            pC->ewc.pAudioEncCtxt, &pEncInBuffer, &pEncOutBuffer);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingDoMixing():\
                pAudioEncoderGlobalFcts->pFctStep returns 0x%x",
                err);
            return err;
        }

        /**
        * Set AU cts and size */
        pC->ewc.WriterAudioAU.size =
            pEncOutBuffer.
            pTableBufferSize[0]; /**< Get the size of encoded data */
        pC->ewc.WriterAudioAU.CTS += frameTimeDelta;

        /* Update decoded buffer here */
        if( M4OSA_TRUE == pC->b_SSRCneeded || pC->ChannelConversion > 0 )
        {
            tempPosBuffer = pC->pSsrcBufferOut
                + pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize;
            memmove((void *)pC->pSsrcBufferOut, (void *)tempPosBuffer,
                pC->pPosInSsrcBufferOut - tempPosBuffer);
            pC->pPosInSsrcBufferOut -=
                pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize;
        }
        else
        {
            tempPosBuffer = pC->pSsrcBufferIn + pC->minimumBufferIn;
            memmove((void *)pC->pSsrcBufferIn, (void *)tempPosBuffer,
                pC->pPosInSsrcBufferIn - tempPosBuffer);
            pC->pPosInSsrcBufferIn -= pC->minimumBufferIn;
        }

        /**
        * Write the mixed AU */
        M4OSA_TRACE2_2("J ---- write : cts  = %ld [ 0x%x ]",
            (M4OSA_Int32)(pC->ewc.WriterAudioAU.CTS / pC->ewc.scale_audio),
            pC->ewc.WriterAudioAU.size);

        err =
            pC->ShellAPI.pWriterDataFcts->pProcessAU(pC->ewc.p3gpWriterContext,
            M4VSS3GPP_WRITER_AUDIO_STREAM_ID, &pC->ewc.WriterAudioAU);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingCopyAdded:\
                pWriterDataFcts->pProcessAU(audio) returns 0x%x!",
                err);
            return err;
        }

        /**
        * Increment the audio CTS for the next step */
        pC->ewc.dATo += frameTimeDelta / pC->ewc.scale_audio;
    }
    else
    {
        /**
        * Read the added audio AU */
        err = M4VSS3GPP_intClipReadNextAudioFrame(pC->pAddedClipCtxt);

        M4OSA_TRACE2_3("I .... read  : cts  = %.0f + %.0f [ 0x%x ]",
            pC->pAddedClipCtxt->iAudioFrameCts
            / pC->pAddedClipCtxt->scale_audio,
            pC->pAddedClipCtxt->iAoffset / pC->pAddedClipCtxt->scale_audio,
            pC->pAddedClipCtxt->uiAudioFrameSize);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE3_1(
                "M4VSS3GPP_intAudioMixingCopyAdded(): m_pFctGetNextAu(audio) returns 0x%x",
                err);
            return err;
        }

        /**
        * Get the output AU to write into */
        err = pC->ShellAPI.pWriterDataFcts->pStartAU(pC->ewc.p3gpWriterContext,
            M4VSS3GPP_WRITER_AUDIO_STREAM_ID, &pC->ewc.WriterAudioAU);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingCopyAdded:\
                pWriterDataFcts->pStartAU(audio) returns 0x%x!",
                err);
            return err;
        }

        /**
        * Copy the input AU properties to the output AU */

        /** THE CHECK BELOW IS ADDED TO PREVENT ISSUES LINKED TO PRE-ALLOCATED MAX AU SIZE
        max AU size is set based on M4VSS3GPP_AUDIO_MAX_AU_SIZE defined in file
        M4VSS3GPP_InternalConfig.h, If this error occurs increase the limit set in this file
        */
        if( pC->pAddedClipCtxt->uiAudioFrameSize > pC->ewc.WriterAudioAU.size )
        {
            M4OSA_TRACE1_2(
                "ERROR: audio AU size (%d) to copy larger than allocated one (%d) => abort",
                pC->pAddedClipCtxt->uiAudioFrameSize,
                pC->ewc.WriterAudioAU.size);
            M4OSA_TRACE1_0(
                "PLEASE CONTACT SUPPORT TO EXTEND MAX AU SIZE IN THE PRODUCT LIBRARY");
            err = M4ERR_UNSUPPORTED_MEDIA_TYPE;
            return err;
        }
        pC->ewc.WriterAudioAU.size = pC->pAddedClipCtxt->uiAudioFrameSize;
        pC->ewc.WriterAudioAU.CTS =
            pC->pAddedClipCtxt->iAudioFrameCts + pC->pAddedClipCtxt->iAoffset;

        /**
        * Copy the AU itself */
        memcpy((void *)pC->ewc.WriterAudioAU.dataAddress,
            (void *)pC->pAddedClipCtxt->pAudioFramePtr, pC->ewc.WriterAudioAU.size);

        /**
        * Write the mixed AU */
        M4OSA_TRACE2_2("J ---- write : cts  = %ld [ 0x%x ]",
            (M4OSA_Int32)(pC->ewc.WriterAudioAU.CTS / pC->ewc.scale_audio),
            pC->ewc.WriterAudioAU.size);

        err =
            pC->ShellAPI.pWriterDataFcts->pProcessAU(pC->ewc.p3gpWriterContext,
            M4VSS3GPP_WRITER_AUDIO_STREAM_ID, &pC->ewc.WriterAudioAU);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingCopyAdded:\
                pWriterDataFcts->pProcessAU(audio) returns 0x%x!",
                err);
            return err;
        }

        /**
        * Increment the audio CTS for the next step */
        pC->ewc.dATo += pC->ewc.iSilenceFrameDuration / pC->ewc.scale_audio;
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_intAudioMixingCopyAdded(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR  M4VSS3GPP_intAudioMixingConvert(M4VSS3GPP_InternalAudioMixingContext *pC)
 * @brief    Convert PCM of added track to the right ASF / nb of Channels
 * @note
 * @param    pC    (IN) VSS audio mixing internal context
 * @return    M4NO_ERROR:    No error
 ******************************************************************************
 */
static M4OSA_ERR M4VSS3GPP_intAudioMixingConvert(
    M4VSS3GPP_InternalAudioMixingContext *pC )
{
    M4OSA_ERR err;
    int ssrcErr; /**< Error while ssrc processing */
    M4OSA_UInt32 uiChannelConvertorNbSamples =
        pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize / sizeof(short)
        / pC->pInputClipCtxt->pSettings->ClipProperties.uiNbChannels;
    M4OSA_MemAddr8 tempPosBuffer;

    M4OSA_UInt32 outFrameCount = uiChannelConvertorNbSamples;
    /* Do we need to feed SSRC buffer In ? */
    /**
    * RC: This is not really optimum (memmove). We should handle this with linked list. */
    while( pC->pPosInSsrcBufferIn - pC->pSsrcBufferIn < (M4OSA_Int32)pC->minimumBufferIn )
    {
        /* We need to get more PCM data */
        if (pC->bNoLooping == M4OSA_TRUE)
        {
            err = M4WAR_NO_MORE_AU;
        }
        else
        {
        err = M4VSS3GPP_intClipReadNextAudioFrame(pC->pAddedClipCtxt);
        }
        if(pC->bjumpflag)
        {
        /**
            * Jump at the Begin loop time */
            M4OSA_Int32 time = (M4OSA_Int32)(pC->uiBeginLoop);

            err =
                pC->pAddedClipCtxt->ShellAPI.m_pReader->m_pFctJump\
                    (pC->pAddedClipCtxt->pReaderContext,
                     (M4_StreamHandler*)pC->pAddedClipCtxt->pAudioStream, &time);
            if (M4NO_ERROR != err)
            {
                M4OSA_TRACE1_1("M4VSS3GPP_intAudioMixingConvert():\
                     m_pReader->m_pFctJump(audio returns 0x%x", err);
                return err;
            }
            pC->bjumpflag = M4OSA_FALSE;
        }
        M4OSA_TRACE2_3("E .... read  : cts  = %.0f + %.0f [ 0x%x ]",
             pC->pAddedClipCtxt->iAudioFrameCts / pC->pAddedClipCtxt->scale_audio,
                 pC->pAddedClipCtxt->iAoffset / pC->pAddedClipCtxt->scale_audio,
                     pC->pAddedClipCtxt->uiAudioFrameSize);
        if( M4WAR_NO_MORE_AU == err )
        {
            if(pC->bNoLooping == M4OSA_TRUE)
            {
                pC->uiEndLoop =0; /* Value 0 means no looping is required */
            }
            /**
            * Decide what to do when audio is over */
            if( pC->uiEndLoop > 0 )
            {
                /**
                * Jump at the Begin loop time */
                M4OSA_Int32 time = (M4OSA_Int32)(pC->uiBeginLoop);

                err = pC->pAddedClipCtxt->ShellAPI.m_pReader->m_pFctJump(
                    pC->pAddedClipCtxt->pReaderContext,
                    (M4_StreamHandler *)pC->pAddedClipCtxt->
                    pAudioStream, &time);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intAudioMixingConvert():\
                        m_pReader->m_pFctJump(audio returns 0x%x",
                        err);
                    return err;
                }
            }
            else
            {
                /* Transition from encoding state to reading state */
                err = M4VSS3GPP_intAudioMixingTransition(pC);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intAudioMixingStepAudioMix(): pre-encode fails err = 0x%x",
                        err);
                    return err;
                }

                /**
                * Second segment is over, state transition to third and return OK */
                pC->State = M4VSS3GPP_kAudioMixingState_AUDIO_THIRD_SEGMENT;

                /**
                * Return with no error so the step function will be called again */
                M4OSA_TRACE2_0(
                    "M4VSS3GPP_intAudioMixingConvert():\
                    returning M4VSS3GPP_WAR_END_OF_ADDED_AUDIO (2->3) a");
                return M4VSS3GPP_WAR_END_OF_ADDED_AUDIO;
            }
        }
        else if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingConvert(): m_pFctGetNextAu(audio) returns 0x%x",
                err);
            return err;
        }

        err = M4VSS3GPP_intClipDecodeCurrentAudioFrame(pC->pAddedClipCtxt);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingDoMixing:\
                M4VSS3GPP_intClipDecodeCurrentAudioFrame(added) returns 0x%x",
                err);
            return M4VSS3GPP_ERR_INPUT_AUDIO_CORRUPTED_AU;
        }

        /* Copy decoded data into SSRC buffer in */
        memcpy((void *)pC->pPosInSsrcBufferIn,
            (void *)pC->pAddedClipCtxt->AudioDecBufferOut.m_dataAddress,
            pC->pAddedClipCtxt->AudioDecBufferOut.m_bufferSize);
        /* Update position pointer into SSRC buffer In */

        pC->pPosInSsrcBufferIn +=
            pC->pAddedClipCtxt->AudioDecBufferOut.m_bufferSize;
    }

    /* Do the resampling / channel conversion if needed (=feed buffer out) */
    if( pC->b_SSRCneeded == M4OSA_TRUE )
    {
        pC->ChannelConversion = 0;
        if( pC->ChannelConversion > 0 )
        {
            while( pC->pPosInTempBuffer - pC->pTempBuffer
                < (M4OSA_Int32)(pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize
                *pC->pAddedClipCtxt->pSettings->ClipProperties.uiNbChannels)
                / pC->ChannelConversion )
                /* We use ChannelConversion variable because in case 2, we need twice less data */
            {
                ssrcErr = 0;
                memset((void *)pC->pPosInTempBuffer,0,
                    (pC->iSsrcNbSamplOut * sizeof(short) * pC->ewc.uiNbChannels));

                LVAudioresample_LowQuality((short*)pC->pPosInTempBuffer,
                    (short*)pC->pSsrcBufferIn,
                    pC->iSsrcNbSamplOut,
                    pC->pLVAudioResampler);
                if( 0 != ssrcErr )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intAudioMixingConvert: SSRC_Process returns 0x%x, returning ",
                        ssrcErr);
                    return ssrcErr;
                }

                pC->pPosInTempBuffer += pC->iSsrcNbSamplOut * sizeof(short)
                    * pC->pAddedClipCtxt->pSettings->
                    ClipProperties.uiNbChannels;

                /* Update SSRC bufferIn */
                tempPosBuffer =
                    pC->pSsrcBufferIn + (pC->iSsrcNbSamplIn * sizeof(short)
                    * pC->pAddedClipCtxt->pSettings->
                    ClipProperties.uiNbChannels);
                memmove((void *)pC->pSsrcBufferIn, (void *)tempPosBuffer,
                    pC->pPosInSsrcBufferIn - tempPosBuffer);
                pC->pPosInSsrcBufferIn -= pC->iSsrcNbSamplIn * sizeof(short)
                    * pC->pAddedClipCtxt->pSettings->
                    ClipProperties.uiNbChannels;
            }
        }
        else
        {
            while( pC->pPosInSsrcBufferOut - pC->pSsrcBufferOut
                < (M4OSA_Int32)pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize )
            {
                ssrcErr = 0;
                memset((void *)pC->pPosInSsrcBufferOut,0,
                    (pC->iSsrcNbSamplOut * sizeof(short) * pC->ewc.uiNbChannels));

                LVAudioresample_LowQuality((short*)pC->pPosInSsrcBufferOut,
                    (short*)pC->pSsrcBufferIn,
                    pC->iSsrcNbSamplOut,
                    pC->pLVAudioResampler);
                if( 0 != ssrcErr )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intAudioMixingConvert: SSRC_Process returns 0x%x, returning ",
                        ssrcErr);
                    return ssrcErr;
                }
                pC->pPosInSsrcBufferOut +=
                    pC->iSsrcNbSamplOut * sizeof(short) * pC->ewc.uiNbChannels;

                /* Update SSRC bufferIn */
                tempPosBuffer =
                    pC->pSsrcBufferIn + (pC->iSsrcNbSamplIn * sizeof(short)
                    * pC->pAddedClipCtxt->pSettings->
                    ClipProperties.uiNbChannels);
                memmove((void *)pC->pSsrcBufferIn, (void *)tempPosBuffer,
                    pC->pPosInSsrcBufferIn - tempPosBuffer);
                pC->pPosInSsrcBufferIn -= pC->iSsrcNbSamplIn * sizeof(short)
                    * pC->pAddedClipCtxt->pSettings->
                    ClipProperties.uiNbChannels;
            }
        }

        /* Convert Stereo<->Mono */
        switch( pC->ChannelConversion )
        {
            case 0: /* No channel conversion */
                break;

            case 1: /* stereo to mono */
                if( pC->pPosInSsrcBufferOut - pC->pSsrcBufferOut
                    < (M4OSA_Int32)pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize )
                {
                    From2iToMono_16((short *)pC->pTempBuffer,
                        (short *)pC->pSsrcBufferOut,
                        (short)(uiChannelConvertorNbSamples));
                    /* Update pTempBuffer */
                    tempPosBuffer = pC->pTempBuffer
                        + (uiChannelConvertorNbSamples * sizeof(short)
                        * pC->pAddedClipCtxt->pSettings->
                        ClipProperties.
                        uiNbChannels); /* Buffer is in bytes */
                    memmove((void *)pC->pTempBuffer, (void *)tempPosBuffer,
                        pC->pPosInTempBuffer - tempPosBuffer);
                    pC->pPosInTempBuffer -=
                        (uiChannelConvertorNbSamples * sizeof(short)
                        * pC->pAddedClipCtxt->pSettings->
                        ClipProperties.uiNbChannels);
                    pC->pPosInSsrcBufferOut +=
                        pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize;
                }
                break;

            case 2: /* mono to stereo */
                if( pC->pPosInSsrcBufferOut - pC->pSsrcBufferOut
                    < (M4OSA_Int32)pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize )
                {
                    MonoTo2I_16((short *)pC->pTempBuffer,
                        (short *)pC->pSsrcBufferOut,
                        (short)uiChannelConvertorNbSamples);
                    tempPosBuffer = pC->pTempBuffer
                        + (uiChannelConvertorNbSamples * sizeof(short)
                        * pC->pAddedClipCtxt->pSettings->
                        ClipProperties.uiNbChannels);
                    memmove((void *)pC->pTempBuffer, (void *)tempPosBuffer,
                        pC->pPosInTempBuffer - tempPosBuffer);
                    pC->pPosInTempBuffer -=
                        (uiChannelConvertorNbSamples * sizeof(short)
                        * pC->pAddedClipCtxt->pSettings->
                        ClipProperties.uiNbChannels);
                    pC->pPosInSsrcBufferOut +=
                        pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize;
                }
                break;
        }
    }
    else if( pC->ChannelConversion > 0 )
    {
        //M4OSA_UInt32 uiChannelConvertorNbSamples =
        // pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize / sizeof(short) /
        // pC->pInputClipCtxt->pSettings->ClipProperties.uiNbChannels;
        /* Convert Stereo<->Mono */
        switch( pC->ChannelConversion )
        {
            case 0: /* No channel conversion */
                break;

            case 1: /* stereo to mono */
                if( pC->pPosInSsrcBufferOut - pC->pSsrcBufferOut
                    < (M4OSA_Int32)pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize )
                {
                    From2iToMono_16((short *)pC->pSsrcBufferIn,
                        (short *)pC->pSsrcBufferOut,
                        (short)(uiChannelConvertorNbSamples));
                    /* Update pTempBuffer */
                    tempPosBuffer = pC->pSsrcBufferIn
                        + (uiChannelConvertorNbSamples * sizeof(short)
                        * pC->pAddedClipCtxt->pSettings->
                        ClipProperties.
                        uiNbChannels); /* Buffer is in bytes */
                    memmove((void *)pC->pSsrcBufferIn, (void *)tempPosBuffer,
                        pC->pPosInSsrcBufferIn - tempPosBuffer);
                    pC->pPosInSsrcBufferIn -=
                        (uiChannelConvertorNbSamples * sizeof(short)
                        * pC->pAddedClipCtxt->pSettings->
                        ClipProperties.uiNbChannels);
                    pC->pPosInSsrcBufferOut +=
                        pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize;
                }
                break;

            case 2: /* mono to stereo */
                if( pC->pPosInSsrcBufferOut - pC->pSsrcBufferOut
                    < (M4OSA_Int32)pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize )
                {
                    MonoTo2I_16((short *)pC->pSsrcBufferIn,
                        (short *)pC->pSsrcBufferOut,
                        (short)uiChannelConvertorNbSamples);
                    tempPosBuffer = pC->pSsrcBufferIn
                        + (uiChannelConvertorNbSamples * sizeof(short)
                        * pC->pAddedClipCtxt->pSettings->
                        ClipProperties.uiNbChannels);
                    memmove((void *)pC->pSsrcBufferIn, (void *)tempPosBuffer,
                        pC->pPosInSsrcBufferIn - tempPosBuffer);
                    pC->pPosInSsrcBufferIn -=
                        (uiChannelConvertorNbSamples * sizeof(short)
                        * pC->pAddedClipCtxt->pSettings->
                        ClipProperties.uiNbChannels);
                    pC->pPosInSsrcBufferOut +=
                        pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize;
                }
                break;
        }
    }
    else
    {
        /* No channel conversion nor sampl. freq. conversion needed, just buffer management */
        pC->pPosInSsrcBufferOut = pC->pPosInSsrcBufferIn;
    }

    return M4NO_ERROR;
}

M4OSA_Int32 M4VSS3GPP_getDecibelSound( M4OSA_UInt32 value )
    {
    int dbSound = 1;

    if( value == 0 )
        return 0;

    if( value > 0x4000 && value <= 0x8000 )      // 32768
        dbSound = 90;

    else if( value > 0x2000 && value <= 0x4000 ) // 16384
        dbSound = 84;

    else if( value > 0x1000 && value <= 0x2000 ) // 8192
        dbSound = 78;

    else if( value > 0x0800 && value <= 0x1000 ) // 4028
        dbSound = 72;

    else if( value > 0x0400 && value <= 0x0800 ) // 2048
        dbSound = 66;

    else if( value > 0x0200 && value <= 0x0400 ) // 1024
        dbSound = 60;

    else if( value > 0x0100 && value <= 0x0200 ) // 512
        dbSound = 54;

    else if( value > 0x0080 && value <= 0x0100 ) // 256
        dbSound = 48;

    else if( value > 0x0040 && value <= 0x0080 ) // 128
        dbSound = 42;

    else if( value > 0x0020 && value <= 0x0040 ) // 64
        dbSound = 36;

    else if( value > 0x0010 && value <= 0x0020 ) // 32
        dbSound = 30;

    else if( value > 0x0008 && value <= 0x0010 ) //16
        dbSound = 24;

    else if( value > 0x0007 && value <= 0x0008 ) //8
        dbSound = 24;

    else if( value > 0x0003 && value <= 0x0007 ) // 4
        dbSound = 18;

    else if( value > 0x0001 && value <= 0x0003 ) //2
        dbSound = 12;

    else if( value > 0x000 && value <= 0x0001 )  // 1
        dbSound = 6;

    else
        dbSound = 0;

    return dbSound;
    }
/**
 ******************************************************************************
 * M4OSA_ERR  M4VSS3GPP_intAudioMixingDoMixing(M4VSS3GPP_InternalAudioMixingContext *pC)
 * @brief    Mix the current audio AUs (decoder, mix, encode)
 * @note
 * @param    pC    (IN) VSS audio mixing internal context
 * @return    M4NO_ERROR:    No error
 ******************************************************************************
 */
static M4OSA_ERR M4VSS3GPP_intAudioMixingDoMixing(
    M4VSS3GPP_InternalAudioMixingContext *pC )
{
    M4OSA_ERR err;
    M4OSA_Int16 *pPCMdata1;
    M4OSA_Int16 *pPCMdata2;
    M4OSA_UInt32 uiPCMsize;

    M4ENCODER_AudioBuffer pEncInBuffer;  /**< Encoder input buffer for api */
    M4ENCODER_AudioBuffer pEncOutBuffer; /**< Encoder output buffer for api */
    M4OSA_Time
        frameTimeDelta; /**< Duration of the encoded (then written) data */
    M4OSA_MemAddr8 tempPosBuffer;
    /* ducking variable */
    M4OSA_UInt16 loopIndex = 0;
    M4OSA_Int16 *pPCM16Sample = M4OSA_NULL;
    M4OSA_Int32 peakDbValue = 0;
    M4OSA_Int32 previousDbValue = 0;
    M4OSA_UInt32 i;

    /**
    * Decode original audio track AU */

    err = M4VSS3GPP_intClipDecodeCurrentAudioFrame(pC->pInputClipCtxt);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intAudioMixingDoMixing:\
            M4VSS3GPP_intClipDecodeCurrentAudioFrame(orig) returns 0x%x",
            err);
        return M4VSS3GPP_ERR_INPUT_AUDIO_CORRUPTED_AU;
    }

    if( M4OSA_TRUE == pC->b_SSRCneeded || pC->ChannelConversion > 0
        || pC->pAddedClipCtxt->pSettings->ClipProperties.AudioStreamType
        == M4VIDEOEDITING_kMP3 )
    {
        err = M4VSS3GPP_intAudioMixingConvert(pC);

        if( err == M4VSS3GPP_WAR_END_OF_ADDED_AUDIO )
        {
            return err;
        }

        if( err != M4NO_ERROR )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingDoMixing: M4VSS3GPP_intAudioMixingConvert returned 0x%x",
                err);
            return M4VSS3GPP_ERR_AUDIO_DECODED_PCM_SIZE_ISSUE;
        }

        /**
        * Get the output AU to write into */
        err = pC->ShellAPI.pWriterDataFcts->pStartAU(pC->ewc.p3gpWriterContext,
            M4VSS3GPP_WRITER_AUDIO_STREAM_ID, &pC->ewc.WriterAudioAU);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingStepAudioMix:\
                pWriterDataFcts->pStartAU(audio) returns 0x%x!",
                err);
            return err;
        }

        pPCMdata2 = (M4OSA_Int16 *)pC->pSsrcBufferOut;
    }
    else
    {
        /**
        * Decode added audio track AU */
        err = M4VSS3GPP_intClipDecodeCurrentAudioFrame(pC->pAddedClipCtxt);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingDoMixing:\
                M4VSS3GPP_intClipDecodeCurrentAudioFrame(added) returns 0x%x",
                err);
            return M4VSS3GPP_ERR_INPUT_AUDIO_CORRUPTED_AU;
        }

        /**
        * Check both clips decoded the same amount of PCM samples */
        if( pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize
            != pC->pAddedClipCtxt->AudioDecBufferOut.m_bufferSize )
        {
            M4OSA_TRACE1_0(
                "M4VSS3GPP_intAudioMixingDoMixing:\
                both clips AU must have the same decoded PCM size!");
            return M4VSS3GPP_ERR_AUDIO_DECODED_PCM_SIZE_ISSUE;
        }
        pPCMdata2 = (M4OSA_Int16 *)pC->pAddedClipCtxt->AudioDecBufferOut.m_dataAddress;
    }

    /**
    * Mix the two decoded PCM audios */
    pPCMdata1 =
        (M4OSA_Int16 *)pC->pInputClipCtxt->AudioDecBufferOut.m_dataAddress;
    uiPCMsize = pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize
        / 2; /*buffer size (bytes) to number of sample (int16)*/

    if( pC->b_DuckingNeedeed )
    {
        loopIndex = 0;
        peakDbValue = 0;
        previousDbValue = peakDbValue;

        pPCM16Sample = (M4OSA_Int16 *)pC->pInputClipCtxt->
            AudioDecBufferOut.m_dataAddress;

        //Calculate the peak value
         while( loopIndex
             < pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize
            / sizeof(M4OSA_Int16) )
        {
            if( pPCM16Sample[loopIndex] >= 0 )
            {
                peakDbValue = previousDbValue > pPCM16Sample[loopIndex]
                ? previousDbValue : pPCM16Sample[loopIndex];
                previousDbValue = peakDbValue;
            }
            else
            {
                peakDbValue = previousDbValue > -pPCM16Sample[loopIndex]
                ? previousDbValue : -pPCM16Sample[loopIndex];
                previousDbValue = peakDbValue;
            }
            loopIndex++;
        }

        pC->audioVolumeArray[pC->audVolArrIndex] =
            M4VSS3GPP_getDecibelSound(peakDbValue);

        /* WINDOW_SIZE is 10 by default and check for threshold is done after 10 cycles */
        if( pC->audVolArrIndex >= WINDOW_SIZE - 1 )
        {
            pC->bDoDucking =
                M4VSS3GPP_isThresholdBreached((M4OSA_Int32 *)&(pC->audioVolumeArray),
                pC->audVolArrIndex, pC->InDucking_threshold);

            pC->audVolArrIndex = 0;
        }
        else
        {
            pC->audVolArrIndex++;
        }

        /*
        *Below logic controls the mixing weightage for Background Track and Primary Track
        *for the duration of window under analysis to give fade-out for Background and fade-in
        *for primary
        *
        *Current fading factor is distributed in equal range over the defined window size.
        *
        *For a window size = 25 (500 ms (window under analysis) / 20 ms (sample duration))
        *
        */

        if( pC->bDoDucking )
        {
            if( pC->duckingFactor
                > pC->InDucking_lowVolume ) // FADE OUT BG Track
            {
                    // decrement ducking factor in total steps in factor of low volume steps to reach
                    // low volume level
                pC->duckingFactor -= (pC->InDucking_lowVolume);
            }
            else
            {
                pC->duckingFactor = pC->InDucking_lowVolume;
            }
        }
        else
        {
            if( pC->duckingFactor < 1.0 ) // FADE IN BG Track
            {
                // increment ducking factor in total steps of low volume factor to reach
                // orig.volume level
                pC->duckingFactor += (pC->InDucking_lowVolume);
            }
        else
           {
                pC->duckingFactor = 1.0;
            }
        }
        /* endif - ducking_enable */

        /* Mixing Logic */

        while( uiPCMsize-- > 0 )
        {
            M4OSA_Int32 temp;

           /* set vol factor for BT and PT */
            *pPCMdata2 = (M4OSA_Int16)(*pPCMdata2 * pC->fBTVolLevel);

            *pPCMdata1 = (M4OSA_Int16)(*pPCMdata1 * pC->fPTVolLevel);

            /* mix the two samples */

            *pPCMdata2 = (M4OSA_Int16)(( *pPCMdata2) * (pC->duckingFactor));
            *pPCMdata1 = (M4OSA_Int16)(*pPCMdata2 / 2 + *pPCMdata1 / 2);


            if( *pPCMdata1 < 0 )
            {
                temp = -( *pPCMdata1)
                    * 2; // bring to same Amplitude level as it was original

                if( temp > 32767 )
                {
                    *pPCMdata1 = -32766; // less then max allowed value
                }
                else
                {
                    *pPCMdata1 = (M4OSA_Int16)(-temp);
               }
        }
        else
        {
            temp = ( *pPCMdata1)
                * 2; // bring to same Amplitude level as it was original

            if( temp > 32768 )
            {
                *pPCMdata1 = 32767; // less than max allowed value
            }
            else
            {
                *pPCMdata1 = (M4OSA_Int16)temp;
            }
        }

            pPCMdata2++;
            pPCMdata1++;
        }
    }
    else
    {
        while( uiPCMsize-- > 0 )
       {
        /* mix the two samples */
            *pPCMdata1 = (M4OSA_Int16)(*pPCMdata1 * pC->fOrigFactor * pC->fPTVolLevel
               + *pPCMdata2 * pC->fAddedFactor * pC->fBTVolLevel );

            pPCMdata1++;
            pPCMdata2++;
        }
    }

    /* Update pC->pSsrcBufferOut buffer */

    if( M4OSA_TRUE == pC->b_SSRCneeded || pC->ChannelConversion > 0 )
    {
        tempPosBuffer = pC->pSsrcBufferOut
            + pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize;
        memmove((void *)pC->pSsrcBufferOut, (void *)tempPosBuffer,
            pC->pPosInSsrcBufferOut - tempPosBuffer);
        pC->pPosInSsrcBufferOut -=
            pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize;
    }
    else if( pC->pAddedClipCtxt->pSettings->ClipProperties.AudioStreamType
        == M4VIDEOEDITING_kMP3 )
    {
        tempPosBuffer = pC->pSsrcBufferIn
            + pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize;
        memmove((void *)pC->pSsrcBufferIn, (void *)tempPosBuffer,
            pC->pPosInSsrcBufferIn - tempPosBuffer);
        pC->pPosInSsrcBufferIn -=
            pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize;
    }

    /* [Mono] or [Stereo interleaved] : all is in one buffer */
    pEncInBuffer.pTableBuffer[0] =
        pC->pInputClipCtxt->AudioDecBufferOut.m_dataAddress;
    pEncInBuffer.pTableBufferSize[0] =
        pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize;
    pEncInBuffer.pTableBuffer[1] = M4OSA_NULL;
    pEncInBuffer.pTableBufferSize[1] = 0;

    /* Time in ms from data size, because it is PCM16 samples */
    frameTimeDelta =
        pEncInBuffer.pTableBufferSize[0] / sizeof(short) / pC->ewc.uiNbChannels;

    /**
    * Prepare output buffer */
    pEncOutBuffer.pTableBuffer[0] =
        (M4OSA_MemAddr8)pC->ewc.WriterAudioAU.dataAddress;
    pEncOutBuffer.pTableBufferSize[0] = 0;

    M4OSA_TRACE2_0("K **** blend AUs");

    /**
    * Encode the PCM audio */
    err = pC->ShellAPI.pAudioEncoderGlobalFcts->pFctStep(pC->ewc.pAudioEncCtxt,
        &pEncInBuffer, &pEncOutBuffer);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intAudioMixingDoMixing(): pAudioEncoderGlobalFcts->pFctStep returns 0x%x",
            err);
        return err;
    }

    /**
    * Set AU cts and size */
    pC->ewc.WriterAudioAU.size =
        pEncOutBuffer.pTableBufferSize[0]; /**< Get the size of encoded data */
    pC->ewc.WriterAudioAU.CTS += frameTimeDelta;

    /**
    * Write the AU */
    M4OSA_TRACE2_2("L ---- write : cts  = %ld [ 0x%x ]",
        (M4OSA_Int32)(pC->ewc.WriterAudioAU.CTS / pC->ewc.scale_audio),
        pC->ewc.WriterAudioAU.size);

    err = pC->ShellAPI.pWriterDataFcts->pProcessAU(pC->ewc.p3gpWriterContext,
        M4VSS3GPP_WRITER_AUDIO_STREAM_ID, &pC->ewc.WriterAudioAU);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intAudioMixingDoMixing: pWriterDataFcts->pProcessAU returns 0x%x!",
            err);
        return err;
    }

    /**
    * Increment the audio CTS for the next step */
    pC->ewc.dATo += frameTimeDelta / pC->ewc.scale_audio;

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_intAudioMixingDoMixing(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR  M4VSS3GPP_intAudioMixingTransition(M4VSS3GPP_InternalAudioMixingContext *pC)
 * @brief    Decode/encode a few AU backward to initiate the encoder for later Mix segment.
 * @note
 * @param    pC    (IN) VSS audio mixing internal context
 * @return    M4NO_ERROR:    No error
 ******************************************************************************
 */
static M4OSA_ERR M4VSS3GPP_intAudioMixingTransition(
    M4VSS3GPP_InternalAudioMixingContext *pC )
{
    M4OSA_ERR err;

    M4ENCODER_AudioBuffer pEncInBuffer;  /**< Encoder input buffer for api */
    M4ENCODER_AudioBuffer pEncOutBuffer; /**< Encoder output buffer for api */
    M4OSA_Time
        frameTimeDelta = 0; /**< Duration of the encoded (then written) data */

    M4OSA_Int32 iTargetCts, iCurrentCts;

    /**
    * 'BZZZ' bug fix:
    * add a silence frame */
    err = M4VSS3GPP_intAudioMixingWriteSilence(pC);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intAudioMixingTransition():\
            M4VSS3GPP_intAudioMixingWriteSilence returns 0x%x",
            err);
        return err;
    }

    iCurrentCts = (M4OSA_Int32)(pC->ewc.dATo * pC->ewc.scale_audio + 0.5);

    /* Do not do pre-encode step if there is no mixing (remove, 100 %, or not editable) */
    if( M4OSA_FALSE == pC->bAudioMixingIsNeeded )
    {
        /**
        * Advance in the original audio stream to reach the current time
        * (We don't want iAudioCTS to be modified by the jump function,
        * so we have to use a local variable). */
        err = M4VSS3GPP_intClipJumpAudioAt(pC->pInputClipCtxt, &iCurrentCts);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1("M4VSS3GPP_intAudioMixingTransition:\
             M4VSS3GPP_intClipJumpAudioAt() returns 0x%x!", err);
            return err;
        }
    }
    else
    {
        /**< don't try to pre-decode if clip is at its beginning... */
        if( iCurrentCts > 0 )
        {
            /**
            * Get the output AU to write into */
            err = pC->ShellAPI.pWriterDataFcts->pStartAU(
                pC->ewc.p3gpWriterContext, M4VSS3GPP_WRITER_AUDIO_STREAM_ID,
                &pC->ewc.WriterAudioAU);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intAudioMixingTransition:\
                    pWriterDataFcts->pStartAU(audio) returns 0x%x!",
                    err);
                return err;
            }

            /**
            * Jump a few AUs backward */
            iTargetCts = iCurrentCts - M4VSS3GPP_NB_AU_PREFETCH
                * pC->ewc.iSilenceFrameDuration;

            if( iTargetCts < 0 )
            {
                iTargetCts = 0; /**< Sanity check */
            }

            err = M4VSS3GPP_intClipJumpAudioAt(pC->pInputClipCtxt, &iTargetCts);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intAudioMixingTransition: DECODE_ENCODE-prefetch:\
                    M4VSS3GPP_intClipJumpAudioAt returns 0x%x!",
                    err);
                return err;
            }

            /**
            * Decode/encode up to the wanted position */
            while( pC->pInputClipCtxt->iAudioFrameCts < iCurrentCts )
            {
                err = M4VSS3GPP_intClipReadNextAudioFrame(pC->pInputClipCtxt);

                M4OSA_TRACE2_3("M .... read  : cts  = %.0f + %.0f [ 0x%x ]",
                    pC->pInputClipCtxt->iAudioFrameCts
                    / pC->pInputClipCtxt->scale_audio,
                    pC->pInputClipCtxt->iAoffset
                    / pC->pInputClipCtxt->scale_audio,
                    pC->pInputClipCtxt->uiAudioFrameSize);

                if( M4OSA_ERR_IS_ERROR(err) )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intAudioMixingTransition: DECODE_ENCODE-prefetch:\
                        M4VSS3GPP_intClipReadNextAudioFrame(b) returns 0x%x!",
                        err);
                    return err;
                }

                err = M4VSS3GPP_intClipDecodeCurrentAudioFrame(
                    pC->pInputClipCtxt);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intAudioMixingTransition: DECODE_ENCODE-prefetch:\
                        M4VSS3GPP_intClipDecodeCurrentAudioFrame returns 0x%x!",
                        err);
                    return err;
                }

                /* [Mono] or [Stereo interleaved] : all is in one buffer */
                pEncInBuffer.pTableBuffer[0] =
                    pC->pInputClipCtxt->AudioDecBufferOut.m_dataAddress;
                pEncInBuffer.pTableBufferSize[0] =
                    pC->pInputClipCtxt->AudioDecBufferOut.m_bufferSize;
                pEncInBuffer.pTableBuffer[1] = M4OSA_NULL;
                pEncInBuffer.pTableBufferSize[1] = 0;

                /* Time in ms from data size, because it is PCM16 samples */
                frameTimeDelta =
                    pEncInBuffer.pTableBufferSize[0] / sizeof(short)
                    / pC->ewc.uiNbChannels;

                /**
                * Prepare output buffer */
                pEncOutBuffer.pTableBuffer[0] =
                    (M4OSA_MemAddr8)pC->ewc.WriterAudioAU.dataAddress;
                pEncOutBuffer.pTableBufferSize[0] = 0;

                M4OSA_TRACE2_0("N **** pre-encode");

                /**
                * Encode the PCM audio */
                err = pC->ShellAPI.pAudioEncoderGlobalFcts->pFctStep(
                    pC->ewc.pAudioEncCtxt, &pEncInBuffer, &pEncOutBuffer);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intAudioMixingTransition():\
                        pAudioEncoderGlobalFcts->pFctStep returns 0x%x",
                        err);
                    return err;
                }
            }

            /**
            * Set AU cts and size */
            pC->ewc.WriterAudioAU.size = pEncOutBuffer.pTableBufferSize[
                0]; /**< Get the size of encoded data */
                pC->ewc.WriterAudioAU.CTS += frameTimeDelta;

                /**
                * Write the AU */
                M4OSA_TRACE2_2("O ---- write : cts  = %ld [ 0x%x ]",
                    (M4OSA_Int32)(pC->ewc.WriterAudioAU.CTS / pC->ewc.scale_audio),
                    pC->ewc.WriterAudioAU.size);

                err = pC->ShellAPI.pWriterDataFcts->pProcessAU(
                    pC->ewc.p3gpWriterContext, M4VSS3GPP_WRITER_AUDIO_STREAM_ID,
                    &pC->ewc.WriterAudioAU);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intAudioMixingTransition:\
                        pWriterDataFcts->pProcessAU returns 0x%x!",    err);
                    return err;
                }

                /**
                * Increment the audio CTS for the next step */
                pC->ewc.dATo += pC->ewc.iSilenceFrameDuration / pC->ewc.scale_audio;
        }
    }

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intAudioMixingCreateVideoEncoder()
 * @brief    Creates the video encoder
 * @note
 ******************************************************************************
 */
static M4OSA_ERR M4VSS3GPP_intAudioMixingCreateVideoEncoder(
    M4VSS3GPP_InternalAudioMixingContext *pC )
{
    M4OSA_ERR err;
    M4ENCODER_AdvancedParams EncParams;

    /**
    * Simulate a writer interface with our specific function */
    pC->ewc.OurWriterDataInterface.pProcessAU =
        M4VSS3GPP_intProcessAU; /**< This function is VSS 3GPP specific,
                                but it follow the writer interface */
    pC->ewc.OurWriterDataInterface.pStartAU =
        M4VSS3GPP_intStartAU; /**< This function is VSS 3GPP specific,
                              but it follow the writer interface */
    pC->ewc.OurWriterDataInterface.pWriterContext =
        (M4WRITER_Context)
        pC; /**< We give the internal context as writer context */

    /**
    * Get the encoder interface, if not already done */
    if( M4OSA_NULL == pC->ShellAPI.pVideoEncoderGlobalFcts )
    {
        err = M4VSS3GPP_setCurrentVideoEncoder(&pC->ShellAPI,
            pC->ewc.VideoStreamType);
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intAudioMixingCreateVideoEncoder: setCurrentEncoder returns 0x%x",
            err);
        M4ERR_CHECK_RETURN(err);
    }

    /**
    * Set encoder shell parameters according to VSS settings */

    /* Common parameters */
    EncParams.InputFormat = M4ENCODER_kIYUV420;
    EncParams.FrameWidth = pC->ewc.uiVideoWidth;
    EncParams.FrameHeight = pC->ewc.uiVideoHeight;
    EncParams.uiTimeScale = pC->ewc.uiVideoTimeScale;
    EncParams.videoProfile = pC->ewc.outputVideoProfile;
    EncParams.videoLevel = pC->ewc.outputVideoLevel;

    /* No strict regulation in video editor */
    /* Because of the effects and transitions we should allow more flexibility */
    /* Also it prevents to drop important frames
      (with a bad result on sheduling and block effetcs) */
    EncParams.bInternalRegulation = M4OSA_FALSE;
    EncParams.FrameRate = M4ENCODER_kVARIABLE_FPS;

    /**
    * Other encoder settings (defaults) */
    EncParams.uiHorizontalSearchRange = 0;     /* use default */
    EncParams.uiVerticalSearchRange = 0;       /* use default */
    EncParams.bErrorResilience = M4OSA_FALSE;  /* no error resilience */
    EncParams.uiIVopPeriod = 0;                /* use default */
    EncParams.uiMotionEstimationTools = 0;     /* M4V_MOTION_EST_TOOLS_ALL */
    EncParams.bAcPrediction = M4OSA_TRUE;      /* use AC prediction */
    EncParams.uiStartingQuantizerValue = 10;   /* initial QP = 10 */
    EncParams.bDataPartitioning = M4OSA_FALSE; /* no data partitioning */

    switch( pC->ewc.VideoStreamType )
    {
        case M4SYS_kH263:

            EncParams.Format = M4ENCODER_kH263;

            EncParams.uiStartingQuantizerValue = 10;
            EncParams.uiRateFactor = 1; /* default */

            EncParams.bErrorResilience = M4OSA_FALSE;
            EncParams.bDataPartitioning = M4OSA_FALSE;
            break;

        case M4SYS_kMPEG_4:

            EncParams.Format = M4ENCODER_kMPEG4;

            EncParams.uiStartingQuantizerValue = 8;
            EncParams.uiRateFactor = 1;

            if( M4OSA_FALSE == pC->ewc.bVideoDataPartitioning )
            {
                EncParams.bErrorResilience = M4OSA_FALSE;
                EncParams.bDataPartitioning = M4OSA_FALSE;
            }
            else
            {
                EncParams.bErrorResilience = M4OSA_TRUE;
                EncParams.bDataPartitioning = M4OSA_TRUE;
            }
            break;

        case M4SYS_kH264:
            M4OSA_TRACE1_0(
                "M4VSS3GPP_intAudioMixingCreateVideoEncoder: M4SYS_H264");

            EncParams.Format = M4ENCODER_kH264;

            EncParams.uiStartingQuantizerValue = 10;
            EncParams.uiRateFactor = 1; /* default */

            EncParams.bErrorResilience = M4OSA_FALSE;
            EncParams.bDataPartitioning = M4OSA_FALSE;
            break;

        default:
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingCreateVideoEncoder: Unknown videoStreamType 0x%x",
                pC->ewc.VideoStreamType);
            return M4VSS3GPP_ERR_EDITING_UNSUPPORTED_VIDEO_FORMAT;
    }

    EncParams.Bitrate =
        pC->pInputClipCtxt->pSettings->ClipProperties.uiVideoBitrate;

    M4OSA_TRACE1_0(
        "M4VSS3GPP_intAudioMixingCreateVideoEncoder: calling encoder pFctInit");
    /**
    * Init the video encoder (advanced settings version of the encoder Open function) */
    err = pC->ShellAPI.pVideoEncoderGlobalFcts->pFctInit(&pC->ewc.pEncContext,
        &pC->ewc.OurWriterDataInterface, M4VSS3GPP_intVPP, pC,
        pC->ShellAPI.pCurrentVideoEncoderExternalAPI,
        pC->ShellAPI.pCurrentVideoEncoderUserData);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intAudioMixingCreateVideoEncoder:\
            pVideoEncoderGlobalFcts->pFctInit returns 0x%x",
            err);
        return err;
    }

    pC->ewc.encoderState = M4VSS3GPP_kEncoderClosed;
    M4OSA_TRACE1_0(
        "M4VSS3GPP_intAudioMixingCreateVideoEncoder: calling encoder pFctOpen");
    M4OSA_TRACE1_2("vss: audio mix encoder open profile :%d, level %d",
        EncParams.videoProfile, EncParams.videoLevel);
    err = pC->ShellAPI.pVideoEncoderGlobalFcts->pFctOpen(pC->ewc.pEncContext,
        &pC->ewc.WriterVideoAU, &EncParams);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intAudioMixingCreateVideoEncoder:\
            pVideoEncoderGlobalFcts->pFctOpen returns 0x%x",
            err);
        return err;
    }

    pC->ewc.encoderState = M4VSS3GPP_kEncoderStopped;
    M4OSA_TRACE1_0(
        "M4VSS3GPP_intAudioMixingCreateVideoEncoder: calling encoder pFctStart");

    if( M4OSA_NULL != pC->ShellAPI.pVideoEncoderGlobalFcts->pFctStart )
    {
        err = pC->ShellAPI.pVideoEncoderGlobalFcts->pFctStart(
            pC->ewc.pEncContext);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingCreateVideoEncoder:\
                pVideoEncoderGlobalFcts->pFctStart returns 0x%x",
                err);
            return err;
        }
    }

    pC->ewc.encoderState = M4VSS3GPP_kEncoderRunning;

    /**
    *    Return */
    M4OSA_TRACE3_0(
        "M4VSS3GPP_intAudioMixingCreateVideoEncoder: returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intAudioMixingDestroyVideoEncoder()
 * @brief    Destroy the video encoder
 * @note
 ******************************************************************************
 */
static M4OSA_ERR M4VSS3GPP_intAudioMixingDestroyVideoEncoder(
    M4VSS3GPP_InternalAudioMixingContext *pC )
{
    M4OSA_ERR err = M4NO_ERROR;

    if( M4OSA_NULL != pC->ewc.pEncContext )
    {
        if( M4VSS3GPP_kEncoderRunning == pC->ewc.encoderState )
        {
            if( pC->ShellAPI.pVideoEncoderGlobalFcts->pFctStop != M4OSA_NULL )
            {
                err = pC->ShellAPI.pVideoEncoderGlobalFcts->pFctStop(
                    pC->ewc.pEncContext);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intAudioMixingDestroyVideoEncoder:\
                        pVideoEncoderGlobalFcts->pFctStop returns 0x%x",
                        err);
                }
            }

            pC->ewc.encoderState = M4VSS3GPP_kEncoderStopped;
        }

        /* Has the encoder actually been opened? Don't close it if that's not the case. */
        if( M4VSS3GPP_kEncoderStopped == pC->ewc.encoderState )
        {
            err = pC->ShellAPI.pVideoEncoderGlobalFcts->pFctClose(
                pC->ewc.pEncContext);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intAudioMixingDestroyVideoEncoder:\
                    pVideoEncoderGlobalFcts->pFctClose returns 0x%x",
                    err);
            }

            pC->ewc.encoderState = M4VSS3GPP_kEncoderClosed;
        }

        err = pC->ShellAPI.pVideoEncoderGlobalFcts->pFctCleanup(
            pC->ewc.pEncContext);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioMixingDestroyVideoEncoder:\
                pVideoEncoderGlobalFcts->pFctCleanup returns 0x%x!",
                err);
            /**< We do not return the error here because we still have stuff to free */
        }

        pC->ewc.encoderState = M4VSS3GPP_kNoEncoder;
        /**
        * Reset variable */
        pC->ewc.pEncContext = M4OSA_NULL;
    }

    M4OSA_TRACE3_1(
        "M4VSS3GPP_intAudioMixingDestroyVideoEncoder: returning 0x%x", err);
    return err;
}

M4OSA_Bool M4VSS3GPP_isThresholdBreached( M4OSA_Int32 *averageValue,
                                         M4OSA_Int32 storeCount, M4OSA_Int32 thresholdValue )
{
    M4OSA_Bool result = 0;
    int i;
    int finalValue = 0;

    for ( i = 0; i < storeCount; i++ )
        finalValue += averageValue[i];

    finalValue = finalValue / storeCount;


    if( finalValue > thresholdValue )
        result = M4OSA_TRUE;
    else
        result = M4OSA_FALSE;

    return result;
}
