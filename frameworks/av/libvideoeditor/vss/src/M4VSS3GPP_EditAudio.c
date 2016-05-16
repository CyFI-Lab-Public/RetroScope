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
 * @file    M4VSS3GPP_EditAudio.c
 * @brief    Video Studio Service 3GPP edit API implementation.
 * @note
 ******************************************************************************
 */

/****************/
/*** Includes ***/
/****************/

#include "NXPSW_CompilerSwitches.h"
/**
 * Our header */
#include "M4VSS3GPP_API.h"
#include "M4VSS3GPP_InternalTypes.h"
#include "M4VSS3GPP_InternalFunctions.h"
#include "M4VSS3GPP_InternalConfig.h"
#include "M4VSS3GPP_ErrorCodes.h"

/**
 * OSAL headers */
#include "M4OSA_Memory.h" /**< OSAL memory management */
#include "M4OSA_Debug.h"  /**< OSAL debug management */

#define PWR_FXP_FRACT_MAX            (32768)

/************************************************************************/
/* Static local functions                                               */
/************************************************************************/
static M4OSA_ERR M4VSS3GPP_intCheckAudioMode( M4VSS3GPP_InternalEditContext
                                             *pC );
static M4OSA_Void M4VSS3GPP_intCheckAudioEffects( M4VSS3GPP_InternalEditContext
                                                 *pC, M4OSA_UInt8 uiClipNumber );
static M4OSA_ERR M4VSS3GPP_intApplyAudioEffect( M4VSS3GPP_InternalEditContext
                                               *pC, M4OSA_UInt8 uiClip1orClip2,
                                               M4OSA_Int16 *pPCMdata,
                                               M4OSA_UInt32 uiPCMsize );
static M4OSA_ERR M4VSS3GPP_intAudioTransition( M4VSS3GPP_InternalEditContext
                                              *pC, M4OSA_Int16 *pPCMdata1,
                                              M4OSA_Int16 *pPCMdata2,
                                              M4OSA_UInt32 uiPCMsize );

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intEditJumpMP3()
 * @brief    One step of jumping processing for the MP3 clip.
 * @note    On one step, the jump of several AU is done
 * @param   pC    (IN/OUT) Internal edit context
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_intEditJumpMP3( M4VSS3GPP_InternalEditContext *pC )
{
    M4OSA_ERR err;
    M4VSS3GPP_ClipContext *pClip = pC->pC1; /**< shortcut */
    M4OSA_Int32 JumpCts;

    JumpCts = pClip->iActualAudioBeginCut;

    err = M4VSS3GPP_intClipJumpAudioAt(pClip, &JumpCts);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intOpenClip: M4VSS3GPP_intClipJumpAudioAt(A) returns 0x%x!",
            err);
        return err;
    }

    if( JumpCts >= pClip->iActualAudioBeginCut )
    {
        pC->State = M4VSS3GPP_kEditState_MP3;

        /**
        * Update clip offset with the audio begin cut */
        pClip->iAoffset = -JumpCts;

        /**
        * The audio is currently in reading mode */
        pClip->Astatus = M4VSS3GPP_kClipStatus_READ;
    }
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intEditStepMP3()
 * @brief    One step of audio processing for the MP3 clip
 * @param   pC    (IN/OUT) Internal edit context
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_intEditStepMP3( M4VSS3GPP_InternalEditContext *pC )
{
    M4OSA_ERR err;
    M4VSS3GPP_ClipContext *pClip = pC->pC1; /**< shortcut */

    /**
    * Copy the input AU to the output AU */
    err = pC->pOsaFileWritPtr->writeData(pC->ewc.p3gpWriterContext,
        pClip->pAudioFramePtr, (M4OSA_UInt32)pClip->uiAudioFrameSize);

    /**
    * Read the next audio frame */
    err = M4VSS3GPP_intClipReadNextAudioFrame(pClip);

    if( M4OSA_ERR_IS_ERROR(err) )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intEditStepMP3: READ_WRITE:\
            M4VSS3GPP_intClipReadNextAudioFrame returns 0x%x!",    err);
        return err;
    }
    else
    {
        /**
        * Update current time (to=tc+T) */
        pC->ewc.dATo =
            ( pClip->iAudioFrameCts + pClip->iAoffset) / pClip->scale_audio;

        if( (M4OSA_Int32)(pClip->iAudioFrameCts / pClip->scale_audio + 0.5)
            >= pClip->iEndTime )
        {
            M4READER_Buffer mp3tagBuffer;

            /**
            * The duration is better respected if the first AU and last AU are both above
            the cut time */
            err = pC->pOsaFileWritPtr->writeData(pC->ewc.p3gpWriterContext,
                pClip->pAudioFramePtr,
                (M4OSA_UInt32)pClip->uiAudioFrameSize);

            /* The ID3v1 tag is always at the end of the mp3 file so the end of the cutting
            process is waited */
            /* before writing the metadata in the output file*/

            /* Retrieve the data of the ID3v1 Tag */
            err = pClip->ShellAPI.m_pReader->m_pFctGetOption(
                pClip->pReaderContext, M4READER_kOptionID_Mp3Id3v1Tag,
                (M4OSA_DataOption) &mp3tagBuffer);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intEditStepMP3: M4MP3R_getOption returns 0x%x",
                    err);
                return err;
            }

            /* Write the data of the ID3v1 Tag in the output file */
            if( 0 != mp3tagBuffer.m_uiBufferSize )
            {
                err = pC->pOsaFileWritPtr->writeData(pC->ewc.p3gpWriterContext,
                    (M4OSA_MemAddr8)mp3tagBuffer.m_pData, mp3tagBuffer.m_uiBufferSize);
                /**
                * Free before the error checking anyway */
                free(mp3tagBuffer.m_pData);

                /**
                * Error checking */
                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intEditStepMP3:\
                        pOsaFileWritPtr->writeData(ID3v1Tag) returns 0x%x",    err);
                    return err;
                }

                mp3tagBuffer.m_uiBufferSize = 0;
                mp3tagBuffer.m_pData = M4OSA_NULL;
            }

            /* The End Cut has been reached */
            err = M4VSS3GPP_intReachedEndOfAudio(pC);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intEditStepMP3 : M4VSS3GPP_intReachedEndOfAudio returns 0x%x",
                    err);
                return err;
            }
        }

        if( ( M4WAR_NO_MORE_AU == err) && (M4OSA_FALSE
            == pC->bSupportSilence) ) /**< Reached end of clip */
        {
            err = M4VSS3GPP_intReachedEndOfAudio(
                pC); /**< Clip done, do the next one */

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4VSS3GPP_intEditStepMP3: READ_WRITE:\
                    M4VSS3GPP_intReachedEndOfAudio returns 0x%x",
                    err);
                return err;
            }
        }
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_intEditStepMP3: returning M4NO_ERROR");
    return M4NO_ERROR;
}
/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intEditStepAudio()
 * @brief    One step of audio processing
 * @param   pC    (IN/OUT) Internal edit context
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_intEditStepAudio( M4VSS3GPP_InternalEditContext *pC )
{
    M4OSA_ERR err;
    int32_t auTimeStamp = -1;

    M4ENCODER_AudioBuffer pEncInBuffer;  /**< Encoder input buffer for api */
    M4ENCODER_AudioBuffer pEncOutBuffer; /**< Encoder output buffer for api */
    M4OSA_Time
        frameTimeDelta; /**< Duration of the encoded (then written) data */
    M4OSA_Bool bStopAudio;

    /**
    * Check if we reached end cut */
    if( ( pC->ewc.dATo - pC->pC1->iAoffset / pC->pC1->scale_audio + 0.5)
        >= pC->pC1->iEndTime )
    {
        /**
        * Audio is done for this clip */
        err = M4VSS3GPP_intReachedEndOfAudio(pC);

        /* RC: to know when a file has been processed */
        if( M4NO_ERROR != err && err != M4VSS3GPP_WAR_SWITCH_CLIP )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intEditStepAudio: M4VSS3GPP_intReachedEndOfAudio returns 0x%x",
                err);
        }

        return err;
    }

    /**
    * Check Audio Mode, depending on the current output CTS */
    err = M4VSS3GPP_intCheckAudioMode(
        pC); /**< This function change the pC->Astate variable! */

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intEditStepAudio: M4VSS3GPP_intCheckAudioMode returns 0x%x!",
            err);
        return err;
    }

    M4OSA_TRACE2_3("  AUDIO step : dATo = %f  state = %d  offset = %ld",
        pC->ewc.dATo, pC->Astate, pC->pC1->iAoffset);

    bStopAudio = M4OSA_FALSE;

    switch( pC->Astate )
    {
            /* _________________ */
            /*|                 |*/
            /*| READ_WRITE MODE |*/
            /*|_________________|*/

        case M4VSS3GPP_kEditAudioState_READ_WRITE:
            {
                M4OSA_TRACE3_0("M4VSS3GPP_intEditStepAudio READ_WRITE");

                /**
                * Get the output AU to write into */
                err = pC->ShellAPI.pWriterDataFcts->pStartAU(
                    pC->ewc.p3gpWriterContext, M4VSS3GPP_WRITER_AUDIO_STREAM_ID,
                    &pC->ewc.WriterAudioAU);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intEditStepAudio:\
                        READ_WRITE: pWriterDataFcts->pStartAU returns 0x%x!",
                        err);
                    return err;
                }

                /**
                * Compute output audio CTS */
                pC->ewc.WriterAudioAU.CTS =
                    pC->pC1->iAudioFrameCts + pC->pC1->iAoffset;

                /**
                * BZZZ bug fix (read-write case):
                * Replace the first AMR AU of the stream with a silence AU.
                * It removes annoying "BZZZ" audio glitch.
                * It is not needed if there is a begin cut.
                * It is not needed for the first clip.
                * Because of another bugfix (2005-03-24), the first AU written may be
                * the second one which CTS is 20. Hence the cts<21 test.
                * (the BZZZ effect occurs even with the second AU!) */
                if( ( M4OSA_FALSE == pC->pC1->bFirstAuWritten)
                    && (0 != pC->uiCurrentClip) && (pC->pC1->iAudioFrameCts
                    < (pC->ewc.iSilenceFrameDuration + 1)) )
                {
                    /**
                    * Copy a silence AU to the output */
                    pC->ewc.WriterAudioAU.size = pC->ewc.uiSilenceFrameSize;
                    memcpy((void *)pC->ewc.WriterAudioAU.dataAddress,
                        (void *)pC->ewc.pSilenceFrameData, pC->ewc.uiSilenceFrameSize);
                    M4OSA_TRACE2_0("A #### silence AU");
                }
                else if( (M4OSA_UInt32)pC->pC1->uiAudioFrameSize
                    < pC->ewc.uiAudioMaxAuSize )
                {
                    /**
                    * Copy the input AU to the output AU */
                    pC->ewc.WriterAudioAU.size =
                        (M4OSA_UInt32)pC->pC1->uiAudioFrameSize;
                    memcpy((void *)pC->ewc.WriterAudioAU.dataAddress,
                        (void *)pC->pC1->pAudioFramePtr, pC->ewc.WriterAudioAU.size);
                }
                else
                {
                    M4OSA_TRACE1_2(
                        "M4VSS3GPP_intEditStepAudio: READ_WRITE: AU size greater than MaxAuSize \
                        (%d>%d)! returning M4VSS3GPP_ERR_INPUT_AUDIO_AU_TOO_LARGE",
                        pC->pC1->uiAudioFrameSize, pC->ewc.uiAudioMaxAuSize);
                    return M4VSS3GPP_ERR_INPUT_AUDIO_AU_TOO_LARGE;
                }

                /**
                * This boolean is only used to fix the BZZ bug... */
                pC->pC1->bFirstAuWritten = M4OSA_TRUE;

                M4OSA_TRACE2_2("B ---- write : cts  = %ld [ 0x%x ]",
                    (M4OSA_Int32)(pC->ewc.WriterAudioAU.CTS / pC->ewc.scale_audio),
                    pC->ewc.WriterAudioAU.size);

                /**
                * Write the AU */
                err = pC->ShellAPI.pWriterDataFcts->pProcessAU(
                    pC->ewc.p3gpWriterContext, M4VSS3GPP_WRITER_AUDIO_STREAM_ID,
                    &pC->ewc.WriterAudioAU);

                if( M4NO_ERROR != err )
                {
                    /*11/12/2008 CR 3283 MMS use case for VideoArtist
                    the warning M4WAR_WRITER_STOP_REQ is returned when the targeted output file
                    size is reached
                    The editing is then finished,
                     the warning M4VSS3GPP_WAR_EDITING_DONE is returned*/
                    if( M4WAR_WRITER_STOP_REQ == err )
                    {
                        M4OSA_TRACE1_0(
                            "M4VSS3GPP_intEditStepAudio: File was cut to avoid oversize");
                        return M4VSS3GPP_WAR_EDITING_DONE;
                    }
                    else
                    {
                        M4OSA_TRACE1_1(
                            "M4VSS3GPP_intEditStepAudio:\
                            READ_WRITE: pWriterDataFcts->pProcessAU returns 0x%x!",
                            err);
                        return err;
                    }
                }

                /**
                * Audio is now in read mode (there may be a "if(status!=READ)" here,
                but it is removed for optimization) */
                pC->pC1->Astatus = M4VSS3GPP_kClipStatus_READ;

                /**
                * Read the next audio frame */
                err = M4VSS3GPP_intClipReadNextAudioFrame(pC->pC1);

                M4OSA_TRACE2_3("C .... read  : cts  = %.0f + %.0f [ 0x%x ]",
                    pC->pC1->iAudioFrameCts / pC->pC1->scale_audio,
                    pC->pC1->iAoffset / pC->pC1->scale_audio,
                    pC->pC1->uiAudioFrameSize);

                if( M4OSA_ERR_IS_ERROR(err) )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intEditStepAudio: READ_WRITE:\
                        M4VSS3GPP_intClipReadNextAudioFrame returns 0x%x!",
                        err);
                    return err;
                }
                else
                {
                    /**
                    * Update current time (to=tc+T) */
                    pC->ewc.dATo = ( pC->pC1->iAudioFrameCts + pC->pC1->iAoffset)
                        / pC->pC1->scale_audio;

                    if( ( M4WAR_NO_MORE_AU == err)
                        && (M4OSA_FALSE == pC->bSupportSilence) )
                    {
                        /**
                        * If output is other than AMR or AAC
                        (i.e. EVRC,we can't write silence into it)
                        * So we simply end here.*/
                        bStopAudio = M4OSA_TRUE;
                    }
                }
            }
            break;

            /* ____________________ */
            /*|                    |*/
            /*| DECODE_ENCODE MODE |*/
            /*|____________________|*/

        case M4VSS3GPP_kEditAudioState_DECODE_ENCODE:
            {
                M4OSA_TRACE3_0("M4VSS3GPP_intEditStepAudio DECODE_ENCODE");

                /**
                * Get the output AU to write into */
                err = pC->ShellAPI.pWriterDataFcts->pStartAU(
                    pC->ewc.p3gpWriterContext, M4VSS3GPP_WRITER_AUDIO_STREAM_ID,
                    &pC->ewc.WriterAudioAU);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intEditStepAudio: DECODE_ENCODE:\
                        pWriterDataFcts->pStartAU returns 0x%x!",
                        err);
                    return err;
                }

                /**
                * If we were reading the clip, we must jump a few AU backward to decode/encode
                (without writing result) from that point. */
                if( M4VSS3GPP_kClipStatus_READ == pC->pC1->Astatus )
                {
                    M4OSA_Int32 iTargetCts, iCurrentCts;

                    if( 0
                        != pC->pC1->
                        iAudioFrameCts ) /**<don't try to pre-decode if clip is at its beginning. */
                    {
                        /**
                        * Jump a few AUs backward */
                        iCurrentCts = pC->pC1->iAudioFrameCts;
                        iTargetCts = iCurrentCts - M4VSS3GPP_NB_AU_PREFETCH
                            * pC->ewc.iSilenceFrameDuration;

                        if( iTargetCts < 0 )
                        {
                            iTargetCts = 0; /**< Sanity check */
                        }

                        err = M4VSS3GPP_intClipJumpAudioAt(pC->pC1, &iTargetCts);

                        if( M4NO_ERROR != err )
                        {
                            M4OSA_TRACE1_1(
                                "M4VSS3GPP_intEditStepAudio: DECODE_ENCODE-prefetch:\
                                M4VSS3GPP_intClipJumpAudioAt returns 0x%x!",
                                err);
                            return err;
                        }

                        err = M4VSS3GPP_intClipReadNextAudioFrame(
                            pC->pC1); /**< read AU where we jumped */

                        M4OSA_TRACE2_3("D .... read  : cts  = %.0f + %.0f [ 0x%x ]",
                            pC->pC1->iAudioFrameCts / pC->pC1->scale_audio,
                            pC->pC1->iAoffset / pC->pC1->scale_audio,
                            pC->pC1->uiAudioFrameSize);

                        if( M4OSA_ERR_IS_ERROR(err) )
                        {
                            M4OSA_TRACE1_1(
                                "M4VSS3GPP_intEditStepAudio: DECODE_ENCODE-prefetch:\
                                M4VSS3GPP_intClipReadNextAudioFrame(a) returns 0x%x!",
                                err);
                            return err;
                        }

                        /**
                        * Decode/encode up to the wanted position */
                        while( pC->pC1->iAudioFrameCts < iCurrentCts )
                        {
                            err = M4VSS3GPP_intClipDecodeCurrentAudioFrame(pC->pC1);

                            if( M4NO_ERROR != err )
                            {
                                M4OSA_TRACE1_1(
                                    "M4VSS3GPP_intEditStepAudio: DECODE_ENCODE-prefetch: \
                                    M4VSS3GPP_intClipDecodeCurrentAudioFrame returns 0x%x!",
                                    err);
                                return err;
                            }

                            /* [Mono] or [Stereo interleaved] : all is in one buffer */
                            pEncInBuffer.pTableBuffer[0] =
                                pC->pC1->AudioDecBufferOut.m_dataAddress;
                            pEncInBuffer.pTableBufferSize[0] =
                                pC->pC1->AudioDecBufferOut.m_bufferSize;
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

                            M4OSA_TRACE2_0("E **** pre-encode");
#ifdef M4VSS_SUPPORT_OMX_CODECS
                            /*OMX Audio decoder used.
                            * OMX Audio dec shell does internal buffering and hence does not return
                            a PCM buffer for every decodeStep call.*
                            * So PCM buffer sizes might be 0. In this case donot call encode Step*/

                            if( 0 != pEncInBuffer.pTableBufferSize[0] )
                            {
#endif
                                /**
                                * Encode the PCM audio */

                                err =
                                    pC->ShellAPI.pAudioEncoderGlobalFcts->pFctStep(
                                    pC->ewc.pAudioEncCtxt,
                                    &pEncInBuffer, &pEncOutBuffer);

                                if( ( M4NO_ERROR != err)
                                    && (M4WAR_NO_MORE_AU != err) )
                                {
                                    M4OSA_TRACE1_1(
                                        "M4VSS3GPP_intEditStepAudio():\
                                        pAudioEncoderGlobalFcts->pFctStep returns 0x%x",
                                        err);
                                    return err;
                                }
#ifdef M4VSS_SUPPORT_OMX_CODECS

                            } //if(0 != pEncInBuffer.pTableBufferSize[0])

#endif
                            pC->pC1->pAudioFramePtr = M4OSA_NULL;

                            // Get timestamp of last read AU
                            pC->pC1->ShellAPI.m_pAudioDecoder->m_pFctGetOptionAudioDec(
                             pC->pC1->pAudioDecCtxt, M4AD_kOptionID_AuCTS,
                             (M4OSA_DataOption) &auTimeStamp);

                            if (auTimeStamp == -1) {
                                M4OSA_TRACE1_0("M4VSS3GPP_intEditStepAudio: \
                                 invalid audio timestamp returned");
                                return M4WAR_INVALID_TIME;
                            }

                            pC->pC1->iAudioFrameCts = auTimeStamp;

                        }
                    }

                    /**
                    * Audio is now OK for decoding */
                    pC->pC1->Astatus = M4VSS3GPP_kClipStatus_DECODE;
                }

                /**
                * Decode the input audio */
                err = M4VSS3GPP_intClipDecodeCurrentAudioFrame(pC->pC1);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intEditStepAudio: DECODE_ENCODE:\
                        M4VSS3GPP_intClipDecodeCurrentAudioFrame returns 0x%x",
                        err);
                    return err;
                }

                pC->pC1->pAudioFramePtr = M4OSA_NULL;

                // Get timestamp of last read AU
                pC->pC1->ShellAPI.m_pAudioDecoder->m_pFctGetOptionAudioDec(
                 pC->pC1->pAudioDecCtxt, M4AD_kOptionID_AuCTS,
                 (M4OSA_DataOption) &auTimeStamp);

                if (auTimeStamp == -1) {
                    M4OSA_TRACE1_0("M4VSS3GPP_intEditStepAudio: invalid audio \
                     timestamp returned");
                    return M4WAR_INVALID_TIME;
                }

                pC->pC1->iAudioFrameCts = auTimeStamp;

                /**
                * Apply the effect */
                if( pC->iClip1ActiveEffect >= 0 )
                {
                    err = M4VSS3GPP_intApplyAudioEffect(pC, 1, (M4OSA_Int16
                        *)pC->pC1->AudioDecBufferOut.m_dataAddress,
                        pC->pC1->AudioDecBufferOut.m_bufferSize);

                    if( M4NO_ERROR != err )
                    {
                        M4OSA_TRACE1_1(
                            "M4VSS3GPP_intEditStepAudio: DECODE_ENCODE:\
                            M4VSS3GPP_intEndAudioEffect returns 0x%x",
                            err);
                        return err;
                    }
                }

                /**
                * Compute output audio CTS */
                pC->ewc.WriterAudioAU.CTS =
                    pC->pC1->iAudioFrameCts + pC->pC1->iAoffset;

                /* May happen with corrupted input files (which have stts entries not
                multiple of SilenceFrameDuration) */
                if( pC->ewc.WriterAudioAU.CTS < 0 )
                {
                    pC->ewc.WriterAudioAU.CTS = 0;
                }

                /**
                * BZZZ bug fix (decode-encode case):
                * (Yes, the Bzz bug may also occur when we re-encode. It doesn't
                *  occur at the decode before the encode, but at the playback!)
                * Replace the first AMR AU of the encoded stream with a silence AU.
                * It removes annoying "BZZZ" audio glitch.
                * It is not needed if there is a begin cut.
                * It is not needed for the first clip.
                * Because of another bugfix (2005-03-24), the first AU written may be
                * the second one which CTS is 20. Hence the cts<21 test.
                * (the BZZZ effect occurs even with the second AU!) */
                if( ( M4OSA_FALSE == pC->pC1->bFirstAuWritten)
                    && (0 != pC->uiCurrentClip) && (pC->pC1->iAudioFrameCts
                    < (pC->ewc.iSilenceFrameDuration + 1)) )
                {
                    /**
                    * Copy a silence AMR AU to the output */
                    pC->ewc.WriterAudioAU.size = pC->ewc.uiSilenceFrameSize;
                    memcpy((void *)pC->ewc.WriterAudioAU.dataAddress,
                        (void *)pC->ewc.pSilenceFrameData, pC->ewc.uiSilenceFrameSize);
                    M4OSA_TRACE2_0("G #### silence AU");
                }
                else
                {
                    /**
                    * Encode the filtered PCM audio directly into the output AU */

                    /* [Mono] or [Stereo interleaved] : all is in one buffer */
                    pEncInBuffer.pTableBuffer[0] =
                        pC->pC1->AudioDecBufferOut.m_dataAddress;
                    pEncInBuffer.pTableBufferSize[0] =
                        pC->pC1->AudioDecBufferOut.m_bufferSize;
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

                    M4OSA_TRACE2_0("H ++++ encode AU");

#ifdef M4VSS_SUPPORT_OMX_CODECS
                    /*OMX Audio decoder used.
                    * OMX Audio dec shell does internal buffering and hence does not return
                    a PCM buffer for every decodeStep call.*
                    * So PCM buffer sizes might be 0. In this case donot call encode Step*/

                    if( 0 != pEncInBuffer.pTableBufferSize[0] )
                    {

#endif

                        /**
                        * Encode the PCM audio */

                        err = pC->ShellAPI.pAudioEncoderGlobalFcts->pFctStep(
                            pC->ewc.pAudioEncCtxt,
                            &pEncInBuffer, &pEncOutBuffer);

                        if( ( M4NO_ERROR != err) && (M4WAR_NO_MORE_AU != err) )
                        {
                            M4OSA_TRACE1_1(
                                "M4VSS3GPP_intEditStepAudio():\
                                pAudioEncoderGlobalFcts->pFctStep returns 0x%x",
                                err);
                            return err;
                        }
#ifdef M4VSS_SUPPORT_OMX_CODECS

                    }

#endif

                    /**
                    * Set AU size */

                    pC->ewc.WriterAudioAU.size = pEncOutBuffer.pTableBufferSize[
                        0]; /**< Get the size of encoded data */
                }

                /**
                * This boolean is only used to fix the BZZ bug... */
                pC->pC1->bFirstAuWritten = M4OSA_TRUE;

                M4OSA_TRACE2_2("I ---- write : cts  = %ld [ 0x%x ]",
                    (M4OSA_Int32)(pC->ewc.WriterAudioAU.CTS / pC->ewc.scale_audio),
                    pC->ewc.WriterAudioAU.size);

                /**
                * Write the AU */
                err = pC->ShellAPI.pWriterDataFcts->pProcessAU(
                    pC->ewc.p3gpWriterContext, M4VSS3GPP_WRITER_AUDIO_STREAM_ID,
                    &pC->ewc.WriterAudioAU);

                if( M4NO_ERROR != err )
                {
                    /*11/12/2008 CR 3283 MMS use case for VideoArtist
                    the warning M4WAR_WRITER_STOP_REQ is returned when the targeted output file
                     size is reached
                    The editing is then finished,
                     the warning M4VSS3GPP_WAR_EDITING_DONE is returned*/
                    if( M4WAR_WRITER_STOP_REQ == err )
                    {
                        M4OSA_TRACE1_0(
                            "M4VSS3GPP_intEditStepAudio: File was cut to avoid oversize");
                        return M4VSS3GPP_WAR_EDITING_DONE;
                    }
                    else
                    {
                        M4OSA_TRACE1_1(
                            "M4VSS3GPP_intEditStepAudio: DECODE_ENCODE:\
                            pWriterDataFcts->pProcessAU returns 0x%x!",
                            err);
                        return err;
                    }
                }

                /**
                * Read the next audio frame */
                err = M4VSS3GPP_intClipReadNextAudioFrame(pC->pC1);

                M4OSA_TRACE2_3("J .... read  : cts  = %.0f + %.0f [ 0x%x ]",
                    pC->pC1->iAudioFrameCts / pC->pC1->scale_audio,
                    pC->pC1->iAoffset / pC->pC1->scale_audio,
                    pC->pC1->uiAudioFrameSize);

                if( M4OSA_ERR_IS_ERROR(err) )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intEditStepAudio: DECODE_ENCODE:\
                        M4VSS3GPP_intClipReadNextAudioFrame returns 0x%x!",
                        err);
                    return err;
                }
                else
                {
                    /**
                    * Update current time (to=tc+T) */
                    pC->ewc.dATo = ( pC->pC1->iAudioFrameCts + pC->pC1->iAoffset)
                        / pC->pC1->scale_audio;

                    if( ( M4WAR_NO_MORE_AU == err)
                        && (M4OSA_FALSE == pC->bSupportSilence) )
                    {
                        /**
                        * If output is other than AMR or AAC
                        (i.e. EVRC,we can't write silence into it)
                        * So we simply end here.*/
                        bStopAudio = M4OSA_TRUE;
                    }
                }
            }
            break;

            /* _________________ */
            /*|                 |*/
            /*| TRANSITION MODE |*/
            /*|_________________|*/

        case M4VSS3GPP_kEditAudioState_TRANSITION:
            {
                M4OSA_TRACE3_0("M4VSS3GPP_intEditStepAudio TRANSITION");

                /**
                * Get the output AU to write into */
                err = pC->ShellAPI.pWriterDataFcts->pStartAU(
                    pC->ewc.p3gpWriterContext, M4VSS3GPP_WRITER_AUDIO_STREAM_ID,
                    &pC->ewc.WriterAudioAU);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intEditStepAudio: TRANSITION:\
                        pWriterDataFcts->pStartAU returns 0x%x!",
                        err);
                    return err;
                }

                /**
                * If we were reading the clip, we must jump a few AU backward to decode/encode
                (without writing result) from that point. */
                if( M4VSS3GPP_kClipStatus_READ == pC->pC1->Astatus )
                {
                    M4OSA_Int32 iTargetCts, iCurrentCts;

                    if( 0
                        != pC->pC1->
                        iAudioFrameCts ) /**<don't try to pre-decode if clip is at its beginning.*/
                    {
                        /**
                        * Jump a few AUs backward */
                        iCurrentCts = pC->pC1->iAudioFrameCts;
                        iTargetCts = iCurrentCts - M4VSS3GPP_NB_AU_PREFETCH
                            * pC->ewc.iSilenceFrameDuration;

                        if( iTargetCts < 0 )
                        {
                            iTargetCts = 0; /**< Sanity check */
                        }

                        err = M4VSS3GPP_intClipJumpAudioAt(pC->pC1, &iTargetCts);

                        if( M4NO_ERROR != err )
                        {
                            M4OSA_TRACE1_1(
                                "M4VSS3GPP_intEditStepAudio: TRANSITION-prefetch:\
                                M4VSS3GPP_intClipJumpAudioAt returns 0x%x!",
                                err);
                            return err;
                        }

                        err = M4VSS3GPP_intClipReadNextAudioFrame(
                            pC->pC1); /**< read AU where we jumped */

                        M4OSA_TRACE2_3("K .... read  : cts  = %.0f + %.0f [ 0x%x ]",
                            pC->pC1->iAudioFrameCts / pC->pC1->scale_audio,
                            pC->pC1->iAoffset / pC->pC1->scale_audio,
                            pC->pC1->uiAudioFrameSize);

                        if( M4OSA_ERR_IS_ERROR(err) )
                        {
                            M4OSA_TRACE1_1(
                                "M4VSS3GPP_intEditStepAudio: TRANSITION-prefetch:\
                                M4VSS3GPP_intClipReadNextAudioFrame(a) returns 0x%x!",
                                err);
                            return err;
                        }

                        /**
                        * Decode/encode up to the wanted position */
                        while( pC->pC1->iAudioFrameCts < iCurrentCts )
                        {
                            err = M4VSS3GPP_intClipDecodeCurrentAudioFrame(pC->pC1);

                            if( M4NO_ERROR != err )
                            {
                                M4OSA_TRACE1_1(
                                    "M4VSS3GPP_intEditStepAudio: TRANSITION-prefetch:\
                                    M4VSS3GPP_intClipDecodeCurrentAudioFrame returns 0x%x!",
                                    err);
                                return err;
                            }

                            /* [Mono] or [Stereo interleaved] : all is in one buffer */
                            pEncInBuffer.pTableBuffer[0] =
                                pC->pC1->AudioDecBufferOut.m_dataAddress;
                            pEncInBuffer.pTableBufferSize[0] =
                                pC->pC1->AudioDecBufferOut.m_bufferSize;
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

                            M4OSA_TRACE2_0("L **** pre-encode");
#ifdef M4VSS_SUPPORT_OMX_CODECS
                            /*OMX Audio decoder used.
                            * OMX Audio dec shell does internal buffering and hence does not return
                            a PCM buffer for every decodeStep call.*
                            * So PCM buffer sizes might be 0. In this case donot call encode Step*/

                            if( 0 != pEncInBuffer.pTableBufferSize[0] )
                            {

#endif
                                /**
                                * Encode the PCM audio */

                                err =
                                    pC->ShellAPI.pAudioEncoderGlobalFcts->pFctStep(
                                    pC->ewc.pAudioEncCtxt,
                                    &pEncInBuffer, &pEncOutBuffer);

                                if( ( M4NO_ERROR != err)
                                    && (M4WAR_NO_MORE_AU != err) )
                                {
                                    M4OSA_TRACE1_1(
                                        "M4VSS3GPP_intEditStepAudio():\
                                        pAudioEncoderGlobalFcts->pFctStep returns 0x%x",
                                        err);
                                    return err;
                                }
#ifdef M4VSS_SUPPORT_OMX_CODECS

                            }

#endif

                            err = M4VSS3GPP_intClipReadNextAudioFrame(pC->pC1);

                            M4OSA_TRACE2_3(
                                "M .... read  : cts  = %.0f + %.0f [ 0x%x ]",
                                pC->pC1->iAudioFrameCts / pC->pC1->scale_audio,
                                pC->pC1->iAoffset / pC->pC1->scale_audio,
                                pC->pC1->uiAudioFrameSize);

                            if( M4OSA_ERR_IS_ERROR(err) )
                            {
                                M4OSA_TRACE1_1(
                                    "M4VSS3GPP_intEditStepAudio: TRANSITION-prefetch:\
                                    M4VSS3GPP_intClipReadNextAudioFrame(b) returns 0x%x!",
                                    err);
                                return err;
                            }
                        }
                    }

                    /**
                    * Audio is now OK for decoding */
                    pC->pC1->Astatus = M4VSS3GPP_kClipStatus_DECODE;
                }

                /**
                * Decode the first input audio */
                err = M4VSS3GPP_intClipDecodeCurrentAudioFrame(pC->pC1);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intEditStepAudio: TRANSITION:\
                        M4VSS3GPP_intClipDecodeCurrentAudioFrame(C1) returns 0x%x",
                        err);
                    return err;
                }

                /**
                * Decode the second input audio */
                err = M4VSS3GPP_intClipDecodeCurrentAudioFrame(pC->pC2);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intEditStepAudio: TRANSITION:\
                        M4VSS3GPP_intClipDecodeCurrentAudioFrame(C2) returns 0x%x",
                        err);
                    return err;
                }

                /**
                * Check both clips decoded the same amount of PCM samples */
                if( pC->pC1->AudioDecBufferOut.m_bufferSize
                    != pC->pC2->AudioDecBufferOut.m_bufferSize )
                {
                    M4OSA_TRACE1_2(
                        "ERR : AudioTransition: both clips AU must have the same decoded\
                        PCM size! pc1 size=0x%x, pC2 size = 0x%x",
                        pC->pC1->AudioDecBufferOut.m_bufferSize,
                        pC->pC2->AudioDecBufferOut.m_bufferSize);
#ifdef M4VSS_SUPPORT_OMX_CODECS
                    /*OMX Audio decoder used.
                    * OMX Audio dec shell does internal buffering and hence does not return
                    a PCM buffer for every decodeStep call.*
                    * So PCM buffer sizes might be 0 or different for clip1 and clip2.
                    * So no need to return error in this case */

                    M4OSA_TRACE1_2(
                        "M4VSS3GPP_intEditStepAudio: , pc1 AudBuff size=0x%x,\
                         pC2 AudBuff size = 0x%x",
                        pC->pC1->AudioDecBufferOut.m_bufferSize,
                        pC->pC2->AudioDecBufferOut.m_bufferSize);

#else

                    return M4VSS3GPP_ERR_AUDIO_DECODED_PCM_SIZE_ISSUE;

#endif // M4VSS_SUPPORT_OMX_CODECS

                }

                /**
                * Apply the audio effect on clip1 */
                if( pC->iClip1ActiveEffect >= 0 )
                {
                    err = M4VSS3GPP_intApplyAudioEffect(pC, 1, (M4OSA_Int16
                        *)pC->pC1->AudioDecBufferOut.m_dataAddress,
                        pC->pC1->AudioDecBufferOut.m_bufferSize);

                    if( M4NO_ERROR != err )
                    {
                        M4OSA_TRACE1_1(
                            "M4VSS3GPP_intEditStepAudio: TRANSITION:\
                            M4VSS3GPP_intApplyAudioEffect(C1) returns 0x%x",
                            err);
                        return err;
                    }
                }

                /**
                * Apply the audio effect on clip2 */
                if( pC->iClip2ActiveEffect >= 0 )
                {
                    err = M4VSS3GPP_intApplyAudioEffect(pC, 2, (M4OSA_Int16
                        *)pC->pC2->AudioDecBufferOut.m_dataAddress,
                        pC->pC2->AudioDecBufferOut.m_bufferSize);

                    if( M4NO_ERROR != err )
                    {
                        M4OSA_TRACE1_1(
                            "M4VSS3GPP_intEditStepAudio: TRANSITION:\
                            M4VSS3GPP_intApplyAudioEffect(C2) returns 0x%x",
                            err);
                        return err;
                    }
                }

                /**
                * Apply the transition effect */
                err = M4VSS3GPP_intAudioTransition(pC,
                    (M4OSA_Int16 *)pC->pC1->AudioDecBufferOut.m_dataAddress,
                    (M4OSA_Int16 *)pC->pC2->AudioDecBufferOut.m_dataAddress,
                    pC->pC1->AudioDecBufferOut.m_bufferSize);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intEditStepAudio: TRANSITION:\
                        M4VSS3GPP_intAudioTransition returns 0x%x",
                        err);
                    return err;
                }

                /* [Mono] or [Stereo interleaved] : all is in one buffer */
                pEncInBuffer.pTableBuffer[0] =
                    pC->pC1->AudioDecBufferOut.m_dataAddress;
                pEncInBuffer.pTableBufferSize[0] =
                    pC->pC1->AudioDecBufferOut.m_bufferSize;
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

                M4OSA_TRACE2_0("N **** blend AUs");

#ifdef M4VSS_SUPPORT_OMX_CODECS
                /*OMX Audio decoder used.
                * OMX Audio dec shell does internal buffering and hence does not return
                a PCM buffer for every decodeStep call.*
                * So PCM buffer sizes might be 0. In this case donot call encode Step*/

                if( 0 != pEncInBuffer.pTableBufferSize[0] )
                {

#endif

                    /**
                    * Encode the PCM audio */

                    err = pC->ShellAPI.pAudioEncoderGlobalFcts->pFctStep(
                        pC->ewc.pAudioEncCtxt, &pEncInBuffer, &pEncOutBuffer);

                    if( ( M4NO_ERROR != err) && (M4WAR_NO_MORE_AU != err) )
                    {
                        M4OSA_TRACE1_1(
                            "M4VSS3GPP_intEditStepAudio():\
                            pAudioEncoderGlobalFcts->pFctStep returns 0x%x",
                            err);
                        return err;
                    }
#ifdef M4VSS_SUPPORT_OMX_CODECS

                }

#endif

                /**
                * Set AU cts and size */

                pC->ewc.WriterAudioAU.size = pEncOutBuffer.pTableBufferSize[
                    0]; /**< Get the size of encoded data */
                    pC->ewc.WriterAudioAU.CTS += frameTimeDelta;

                    M4OSA_TRACE2_2("O ---- write : cts  = %ld [ 0x%x ]",
                        (M4OSA_Int32)(pC->ewc.WriterAudioAU.CTS / pC->ewc.scale_audio),
                        pC->ewc.WriterAudioAU.size);

                    /**
                    * Write the AU */
                    err = pC->ShellAPI.pWriterDataFcts->pProcessAU(
                        pC->ewc.p3gpWriterContext, M4VSS3GPP_WRITER_AUDIO_STREAM_ID,
                        &pC->ewc.WriterAudioAU);

                    if( M4NO_ERROR != err )
                    {
                        /*11/12/2008 CR 3283 MMS use case for VideoArtist
                        the warning M4WAR_WRITER_STOP_REQ is returned when the targeted output
                         file size is reached
                        The editing is then finished,the warning M4VSS3GPP_WAR_EDITING_DONE
                        is returned*/
                        if( M4WAR_WRITER_STOP_REQ == err )
                        {
                            M4OSA_TRACE1_0(
                                "M4VSS3GPP_intEditStepAudio: File was cut to avoid oversize");
                            return M4VSS3GPP_WAR_EDITING_DONE;
                        }
                        else
                        {
                            M4OSA_TRACE1_1(
                                "M4VSS3GPP_intEditStepAudio: TRANSITION:\
                                pWriterDataFcts->pProcessAU returns 0x%x!",
                                err);
                            return err;
                        }
                    }

                    /**
                    * Read the next audio frame */
                    err = M4VSS3GPP_intClipReadNextAudioFrame(pC->pC1);

                    M4OSA_TRACE2_3("P .... read  : cts  = %.0f + %.0f [ 0x%x ]",
                        pC->pC1->iAudioFrameCts / pC->pC1->scale_audio,
                        pC->pC1->iAoffset / pC->pC1->scale_audio,
                        pC->pC1->uiAudioFrameSize);

                    if( M4OSA_ERR_IS_ERROR(err) )
                    {
                        M4OSA_TRACE1_1(
                            "M4VSS3GPP_intEditStepAudio: TRANSITION:\
                            M4VSS3GPP_intClipReadNextAudioFrame(C1) returns 0x%x!",
                            err);
                        return err;
                    }
                    else
                    {
                        M4OSA_ERR secondaryError;

                        /**
                        * Update current time (to=tc+T) */
                        pC->ewc.dATo = ( pC->pC1->iAudioFrameCts + pC->pC1->iAoffset)
                            / pC->pC1->scale_audio;

                        /**
                        * Read the next audio frame in the second clip */
                        secondaryError = M4VSS3GPP_intClipReadNextAudioFrame(pC->pC2);

                        M4OSA_TRACE2_3("Q .... read  : cts  = %.0f + %.0f [ 0x%x ]",
                            pC->pC2->iAudioFrameCts / pC->pC2->scale_audio,
                            pC->pC2->iAoffset / pC->pC2->scale_audio,
                            pC->pC2->uiAudioFrameSize);

                        if( M4OSA_ERR_IS_ERROR(secondaryError) )
                        {
                            M4OSA_TRACE1_1(
                                "M4VSS3GPP_intEditStepAudio: TRANSITION:\
                                M4VSS3GPP_intClipReadNextAudioFrame(C2) returns 0x%x!",
                                secondaryError);
                            return err;
                        }

                        if( ( ( M4WAR_NO_MORE_AU == err)
                            || (M4WAR_NO_MORE_AU == secondaryError))
                            && (M4OSA_FALSE == pC->bSupportSilence) )
                        {
                            /**
                            * If output is other than AMR or AAC
                              (i.e. EVRC,we can't write silence into it)
                            * So we simply end here.*/
                            bStopAudio = M4OSA_TRUE;
                        }
                    }
            }
            break;

            /* ____________ */
            /*|            |*/
            /*| ERROR CASE |*/
            /*|____________|*/

        default:

            M4OSA_TRACE3_1(
                "M4VSS3GPP_intEditStepAudio: invalid internal state (0x%x), \
                returning M4VSS3GPP_ERR_INTERNAL_STATE",
                pC->Astate);
            return M4VSS3GPP_ERR_INTERNAL_STATE;
    }

    /**
    * Check if we are forced to stop audio */
    if( M4OSA_TRUE == bStopAudio )
    {
        /**
        * Audio is done for this clip */
        err = M4VSS3GPP_intReachedEndOfAudio(pC);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intEditStepAudio: M4VSS3GPP_intReachedEndOfAudio returns 0x%x",
                err);
            return err;
        }
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_intEditStepAudio: returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intCheckAudioMode()
 * @brief    Check which audio process mode we must use, depending on the output CTS.
 * @param   pC    (IN/OUT) Internal edit context
 ******************************************************************************
 */
static M4OSA_ERR M4VSS3GPP_intCheckAudioMode( M4VSS3GPP_InternalEditContext
                                             *pC )
{
    M4OSA_ERR err;
    const M4OSA_Int32 TD = pC->pTransitionList[pC->
        uiCurrentClip].uiTransitionDuration; /**< Transition duration */

    const M4VSS3GPP_EditAudioState previousAstate = pC->Astate;

    /**
    * Check if Clip1 is on its begin cut, or in its begin effect or end effect zone */
    M4VSS3GPP_intCheckAudioEffects(pC, 1);

    /**
    * Check if we are in the transition with next clip */
    if( ( TD > 0) && ((M4OSA_Int32)(pC->ewc.dATo - pC->pC1->iAoffset
        / pC->pC1->scale_audio + 0.5) >= (pC->pC1->iEndTime - TD)) )
    {
        /**
        * We are in a transition */
        pC->Astate = M4VSS3GPP_kEditAudioState_TRANSITION;
        pC->bTransitionEffect = M4OSA_TRUE;

        /**
        * Do we enter the transition section ? */
        if( M4VSS3GPP_kEditAudioState_TRANSITION != previousAstate )
        {
            /**
            * Open second clip for transition, if not yet opened */
            if( M4OSA_NULL == pC->pC2 )
            {
                err = M4VSS3GPP_intOpenClip(pC, &pC->pC2,
                    &pC->pClipList[pC->uiCurrentClip + 1]);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intCheckAudioMode: M4VSS3GPP_intOpenClip() returns 0x%x!",
                        err);
                    return err;
                }

                /**
                * In case of short transition and bad luck (...), there may be no video AU
                * in the transition. In that case, the second clip has not been opened.
                * So we must update the video offset here. */
                // Decorrelate input and output encoding timestamp to handle encoder prefetch
                /**< Add current video output CTS to the clip offset */
                pC->pC2->iVoffset += (M4OSA_UInt32)pC->ewc.dInputVidCts;
            }

            /**
            * Add current audio output CTS to the clip offset
            * (video offset has already been set when doing the video transition) */
            pC->pC2->iAoffset +=
                (M4OSA_UInt32)(pC->ewc.dATo * pC->ewc.scale_audio + 0.5);

            /**
            * 2005-03-24: BugFix for audio-video synchro:
            * There may be a portion of the duration of an audio AU of desynchro at each assembly.
            * It leads to an audible desynchro when there are a lot of clips assembled.
            * This bug fix allows to resynch the audio track when the delta is higher
            * than one audio AU duration.
            * We Step one AU in the second clip and we change the audio offset accordingly. */
            if( ( pC->pC2->iAoffset
                - (M4OSA_Int32)(pC->pC2->iVoffset *pC->pC2->scale_audio + 0.5))
                    > pC->ewc.iSilenceFrameDuration )
            {
                /**
                * Advance one AMR frame */
                err = M4VSS3GPP_intClipReadNextAudioFrame(pC->pC2);

                M4OSA_TRACE2_3("Z .... read  : cts  = %.0f + %.0f [ 0x%x ]",
                    pC->pC2->iAudioFrameCts / pC->pC2->scale_audio,
                    pC->pC2->iAoffset / pC->pC2->scale_audio,
                    pC->pC2->uiAudioFrameSize);

                if( M4OSA_ERR_IS_ERROR(err) )
                {
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intCheckAudioMode:\
                        M4VSS3GPP_intClipReadNextAudioFrame returns 0x%x!",
                        err);
                    return err;
                }
                /**
                * Update audio offset accordingly*/
                pC->pC2->iAoffset -= pC->ewc.iSilenceFrameDuration;
            }
        }

        /**
        * Check begin and end effects for clip2 */
        M4VSS3GPP_intCheckAudioEffects(pC, 2);
    }
    else
    {
        /**
        * We are not in a transition */
        pC->bTransitionEffect = M4OSA_FALSE;

        /**
        * Check if current mode is Read/Write or Decode/Encode */
        if( pC->iClip1ActiveEffect >= 0 )
        {
            pC->Astate = M4VSS3GPP_kEditAudioState_DECODE_ENCODE;
        }
        else
        {
            pC->Astate = M4VSS3GPP_kEditAudioState_READ_WRITE;
        }
    }

    /**
    * Check if we create/destroy an encoder */
    if( ( M4VSS3GPP_kEditAudioState_READ_WRITE == previousAstate)
        && /**< read mode */
        (M4VSS3GPP_kEditAudioState_READ_WRITE != pC->Astate) ) /**< encode mode */
    {
        M4OSA_UInt32 uiAudioBitrate;

        /* Compute max bitrate depending on input files bitrates and transitions */
        if( pC->Astate == M4VSS3GPP_kEditAudioState_TRANSITION )
        {
            /* Max of the two blended files */
            if( pC->pC1->pSettings->ClipProperties.uiAudioBitrate
                > pC->pC2->pSettings->ClipProperties.uiAudioBitrate )
                uiAudioBitrate =
                pC->pC1->pSettings->ClipProperties.uiAudioBitrate;
            else
                uiAudioBitrate =
                pC->pC2->pSettings->ClipProperties.uiAudioBitrate;
        }
        else
        {
            /* Same as input file */
            uiAudioBitrate = pC->pC1->pSettings->ClipProperties.uiAudioBitrate;
        }

        /**
        * Create the encoder */
        err = M4VSS3GPP_intCreateAudioEncoder(&pC->ewc, &pC->ShellAPI,
            uiAudioBitrate);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intCheckAudioMode: M4VSS3GPP_intResetAudioEncoder() returns 0x%x!",
                err);
            return err;
        }
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_intCheckAudioMode(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_Void M4VSS3GPP_intCheckAudioEffects()
 * @brief    Check which audio effect must be applied at the current time
 ******************************************************************************
 */
static M4OSA_Void M4VSS3GPP_intCheckAudioEffects( M4VSS3GPP_InternalEditContext
                                                 *pC, M4OSA_UInt8 uiClipNumber )
{
    M4OSA_UInt8 uiClipIndex;
    M4OSA_UInt8 uiFxIndex;
    M4VSS3GPP_ClipContext *pClip;
    M4VSS3GPP_EffectSettings *pFx;
    M4OSA_Int32 BC, EC;
    M4OSA_Int8 *piClipActiveEffect;
    M4OSA_Int32 t;

    if( 1 == uiClipNumber )
    {
        uiClipIndex = pC->uiCurrentClip;
        pClip = pC->pC1;
        piClipActiveEffect = &(pC->iClip1ActiveEffect);
    }
    else /**< (2 == uiClipNumber) */
    {
        uiClipIndex = pC->uiCurrentClip + 1;
        pClip = pC->pC2;
        piClipActiveEffect = &(pC->iClip2ActiveEffect);
    }

    /**
    * Shortcuts for code readability */
    BC = pClip->iActualAudioBeginCut;
    EC = pClip->iEndTime;

    /**
    Change the absolut time to clip related time
     RC t = (M4OSA_Int32)(pC->ewc.dATo - pClip->iAoffset/pClip->scale_audio + 0.5);
    < rounding */;
    t = (M4OSA_Int32)(pC->ewc.dATo/*- pClip->iAoffset/pClip->scale_audio*/
        + 0.5); /**< rounding */
    ;

    /**
    * Default: no effect active */
    *piClipActiveEffect = -1;

    /**
    * Check the three effects */
    // RC    for (uiFxIndex=0; uiFxIndex<pC->pClipList[uiClipIndex].nbEffects; uiFxIndex++)
    for ( uiFxIndex = 0; uiFxIndex < pC->nbEffects; uiFxIndex++ )
    {
        /** Shortcut, reverse order because of priority between effects
        ( EndEffect always clean ) */
        pFx = &(pC->pEffectsList[pC->nbEffects - 1 - uiFxIndex]);

        if( M4VSS3GPP_kAudioEffectType_None != pFx->AudioEffectType )
        {
            /**
            * Check if there is actually a video effect */
            if( ( t >= (M4OSA_Int32)(/*BC +*/pFx->uiStartTime))
                && /**< Are we after the start time of the effect? */
                (t < (M4OSA_Int32)(/*BC +*/pFx->uiStartTime + pFx->
                uiDuration)) ) /**< Are we into the effect duration? */
            {
                /**
                * Set the active effect */
                *piClipActiveEffect = pC->nbEffects - 1 - uiFxIndex;

                /**
                * The first effect has the highest priority, then the second one,
                  then the thirs one.
                * Hence, as soon as we found an active effect, we can get out of this loop */
                uiFxIndex = pC->nbEffects; /** get out of the for loop */
            }
            /**
            * Bugfix: The duration of the end effect has been set according to the
                      announced clip duration.
            * If the announced duration is smaller than the real one, the end effect
                      won't be applied at
            * the very end of the clip. To solve this issue we force the end effect. */

        }
    }

    return;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intApplyAudioEffect()
 * @brief    Apply audio effect to pPCMdata
 * @param   pC            (IN/OUT) Internal edit context
 * @param   uiClip1orClip2    (IN/OUT) 1 for first clip, 2 for second clip
 * @param    pPCMdata    (IN/OUT) Input and Output PCM audio data
 * @param    uiPCMsize    (IN)     Size of pPCMdata
 * @return    M4NO_ERROR:                        No error
 ******************************************************************************
 */
static M4OSA_ERR M4VSS3GPP_intApplyAudioEffect( M4VSS3GPP_InternalEditContext
                                               *pC, M4OSA_UInt8 uiClip1orClip2,
                                               M4OSA_Int16 *pPCMdata,
                                               M4OSA_UInt32 uiPCMsize )
{
    M4VSS3GPP_ClipContext *pClip;
    M4VSS3GPP_ClipSettings *pClipSettings;
    M4VSS3GPP_EffectSettings *pFx;
    M4OSA_Int32
        i32sample; /**< we will cast each Int16 sample into this Int32 variable */
    M4OSA_Int32 iPos;
    M4OSA_Int32 iDur;

    M4OSA_DEBUG_IF2(( 1 != uiClip1orClip2) && (2 != uiClip1orClip2),
        M4ERR_PARAMETER,
        "M4VSS3GPP_intBeginAudioEffect: uiClip1orClip2 invalid");

    if( 1 == uiClip1orClip2 )
    {
        pClip = pC->pC1;
        pClipSettings = &(pC->pClipList[pC->
            uiCurrentClip]); /**< Get a shortcut to the clip settings */
        // RC        pFx = &(pClipSettings->Effects[pC->iClip1ActiveEffect]);/**< Get a shortcut
        //                                                                to the active effect */
        pFx = &(pC->
            pEffectsList[pC->
            iClip1ActiveEffect]); /**< Get a shortcut to the active effect */
        M4OSA_DEBUG_IF2(( pC->iClip1ActiveEffect < 0)
            || (pC->iClip1ActiveEffect > 2), M4ERR_PARAMETER,
            "M4VSS3GPP_intApplyAudioEffect: iClip1ActiveEffect invalid");
    }
    else /**< if (2==uiClip1orClip2) */
    {
        pClip = pC->pC2;
        pClipSettings = &(pC->pClipList[pC->uiCurrentClip
            + 1]); /**< Get a shortcut to the clip settings */
        // RC        pFx = &(pClipSettings->Effects[pC->iClip2ActiveEffect]);/**< Get a shortcut
        //                                                                to the active effect */
        pFx = &(pC->
            pEffectsList[pC->
            iClip2ActiveEffect]); /**< Get a shortcut to the active effect */
        M4OSA_DEBUG_IF2(( pC->iClip2ActiveEffect < 0)
            || (pC->iClip2ActiveEffect > 2), M4ERR_PARAMETER,
            "M4VSS3GPP_intApplyAudioEffect: iClip2ActiveEffect invalid");
    }

    iDur = (M4OSA_Int32)pFx->uiDuration;

    /**
    * Compute how far from the beginning of the effect we are, in clip-base time.
    * It is done with integers because the offset and begin cut have been rounded already. */
    iPos =
        (M4OSA_Int32)(pC->ewc.dATo + 0.5 - pClip->iAoffset / pClip->scale_audio)
        - pClip->iActualAudioBeginCut - pFx->uiStartTime;

    /**
    * Sanity check */
    if( iPos > iDur )
    {
        iPos = iDur;
    }
    else if( iPos < 0 )
    {
        iPos = 0;
    }

    /**
    * At this point, iPos is the effect progress, in a 0 to iDur base */
    switch( pFx->AudioEffectType )
    {
        case M4VSS3GPP_kAudioEffectType_FadeIn:

            /**
            * Original samples are signed 16bits.
            * We convert it to signed 32bits and multiply it by iPos.
            * So we must assure that iPos is not higher that 16bits max.
            * iPos max value is iDur, so we test iDur. */
            while( iDur > PWR_FXP_FRACT_MAX )
            {
                iDur >>=
                    2; /**< divide by 2 would be more logical (instead of 4),
                       but we have enough dynamic..) */
                iPos >>= 2; /**< idem */
            }

            /**
            * From buffer size (bytes) to number of sample (int16): divide by two */
            uiPCMsize >>= 1;

            /**
            * Loop on samples */
            while( uiPCMsize-- > 0 ) /**< decrementing to optimize */
            {
                i32sample = *pPCMdata;
                i32sample *= iPos;
                i32sample /= iDur;
                *pPCMdata++ = (M4OSA_Int16)i32sample;
            }

            break;

        case M4VSS3GPP_kAudioEffectType_FadeOut:

            /**
            * switch from 0->Dur to Dur->0 in order to do fadeOUT instead of fadeIN */
            iPos = iDur - iPos;

            /**
            * Original samples are signed 16bits.
            * We convert it to signed 32bits and multiply it by iPos.
            * So we must assure that iPos is not higher that 16bits max.
            * iPos max value is iDur, so we test iDur. */
            while( iDur > PWR_FXP_FRACT_MAX )
            {
                iDur >>=
                    2; /**< divide by 2 would be more logical (instead of 4),
                       but we have enough dynamic..) */
                iPos >>= 2; /**< idem */
            }

            /**
            * From buffer size (bytes) to number of sample (int16): divide by two */
            uiPCMsize >>= 1;

            /**
            * Loop on samples, apply the fade factor on each */
            while( uiPCMsize-- > 0 ) /**< decrementing counter to optimize */
            {
                i32sample = *pPCMdata;
                i32sample *= iPos;
                i32sample /= iDur;
                *pPCMdata++ = (M4OSA_Int16)i32sample;
            }

            break;

        default:
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intApplyAudioEffect: unknown audio effect type (0x%x),\
                returning M4VSS3GPP_ERR_INVALID_AUDIO_EFFECT_TYPE",
                pFx->AudioEffectType);
            return M4VSS3GPP_ERR_INVALID_AUDIO_EFFECT_TYPE;
    }

    /**
    *    Return */
    M4OSA_TRACE3_0("M4VSS3GPP_intApplyAudioEffect: returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intAudioTransition()
 * @brief    Apply transition effect to two PCM buffer
 * @note    The result of the transition is put in the first buffer.
 *          I know it's not beautiful, but it fits my current needs, and it's efficient!
 *          So why bother with a third output buffer?
 * @param   pC            (IN/OUT) Internal edit context
 * @param    pPCMdata1    (IN/OUT) First input and Output PCM audio data
 * @param    pPCMdata2    (IN) Second input PCM audio data
 * @param    uiPCMsize    (IN) Size of both PCM buffers
 * @return    M4NO_ERROR:                        No error
 ******************************************************************************
 */
static M4OSA_ERR M4VSS3GPP_intAudioTransition( M4VSS3GPP_InternalEditContext
                                              *pC, M4OSA_Int16 *pPCMdata1,
                                              M4OSA_Int16 *pPCMdata2,
                                              M4OSA_UInt32 uiPCMsize )
{
    M4OSA_Int32 i32sample1,
        i32sample2; /**< we will cast each Int16 sample into this Int32 variable */
    M4OSA_Int32 iPos1, iPos2;
    M4OSA_Int32 iDur = (M4OSA_Int32)pC->
        pTransitionList[pC->uiCurrentClip].uiTransitionDuration;

    /**
    * Compute how far from the end cut we are, in clip-base time.
    * It is done with integers because the offset and begin cut have been rounded already. */
    iPos1 = pC->pC1->iEndTime - (M4OSA_Int32)(pC->ewc.dATo
        + 0.5 - pC->pC1->iAoffset / pC->pC1->scale_audio);

    /**
    * Sanity check */
    if( iPos1 > iDur )
    {
        iPos1 = iDur;
    }
    else if( iPos1 < 0 )
    {
        iPos1 = 0;
    }

    /**
    * Position of second clip in the transition */
    iPos2 = iDur - iPos1;

    /**
    * At this point, iPos2 is the transition progress, in a 0 to iDur base.
    * iPos1 is the transition progress, in a iDUr to 0 base. */
    switch( pC->pTransitionList[pC->uiCurrentClip].AudioTransitionType )
    {
        case M4VSS3GPP_kAudioTransitionType_CrossFade:

            /**
            * Original samples are signed 16bits.
            * We convert it to signed 32bits and multiply it by iPos.
            * So we must assure that iPos is not higher that 16bits max.
            * iPos max value is iDur, so we test iDur. */
            while( iDur > PWR_FXP_FRACT_MAX )
            {
                iDur >>=
                    2; /**< divide by 2 would be more logical (instead of 4),
                       but we have enough dynamic..) */
                iPos1 >>= 2; /**< idem */
                iPos2 >>= 2; /**< idem */
            }

            /**
            * From buffer size (bytes) to number of sample (int16): divide by two */
            uiPCMsize >>= 1;

            /**
            * Loop on samples, apply the fade factor on each */
            while( uiPCMsize-- > 0 ) /**< decrementing counter to optimize */
            {
                i32sample1 = *pPCMdata1; /**< Get clip1 sample */
                i32sample1 *= iPos1;     /**< multiply by fade numerator */
                i32sample1 /= iDur;      /**< divide by fade denominator */

                i32sample2 = *pPCMdata2; /**< Get clip2 sample */
                i32sample2 *= iPos2;     /**< multiply by fade numerator */
                i32sample2 /= iDur;      /**< divide by fade denominator */

                *pPCMdata1++ = (M4OSA_Int16)(i32sample1
                    + i32sample2); /**< mix the two samples */
                pPCMdata2++; /**< don't forget to increment the second buffer */
            }
            break;

        case M4VSS3GPP_kAudioTransitionType_None:
            /**
            * This is a stupid-non optimized version of the None transition...
            * We copy the PCM frames */
            if( iPos1 < (iDur >> 1) ) /**< second half of transition */
            {
                /**
                * Copy the input PCM to the output buffer */
                memcpy((void *)pPCMdata1,
                    (void *)pPCMdata2, uiPCMsize);
            }
            /**
            * the output must be put in the first buffer.
            * For the first half of the non-transition it's already the case!
            * So we have nothing to do here...
            */

            break;

        default:
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intAudioTransition: unknown transition type (0x%x),\
                returning M4VSS3GPP_ERR_INVALID_AUDIO_TRANSITION_TYPE",
                pC->pTransitionList[pC->uiCurrentClip].AudioTransitionType);
            return M4VSS3GPP_ERR_INVALID_AUDIO_TRANSITION_TYPE;
    }

    /**
    *    Return */
    M4OSA_TRACE3_0("M4VSS3GPP_intAudioTransition: returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intCreateAudioEncoder()
 * @brief    Reset the audio encoder (Create it if needed)
 * @note
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_intCreateAudioEncoder( M4VSS3GPP_EncodeWriteContext *pC_ewc,
                                          M4VSS3GPP_MediaAndCodecCtxt *pC_ShellAPI,
                                          M4OSA_UInt32 uiAudioBitrate )
{
    M4OSA_ERR err;

    /**
    * If an encoder already exist, we destroy it */
    if( M4OSA_NULL != pC_ewc->pAudioEncCtxt )
    {
        err = pC_ShellAPI->pAudioEncoderGlobalFcts->pFctClose(
            pC_ewc->pAudioEncCtxt);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intResetAudioEncoder: pAudioEncoderGlobalFcts->pFctClose returns 0x%x",
                err);
            /**< don't return, we still have stuff to free */
        }

        err = pC_ShellAPI->pAudioEncoderGlobalFcts->pFctCleanUp(
            pC_ewc->pAudioEncCtxt);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intResetAudioEncoder:\
                pAudioEncoderGlobalFcts->pFctCleanUp returns 0x%x",    err);
            /**< don't return, we still have stuff to free */
        }

        pC_ewc->pAudioEncCtxt = M4OSA_NULL;
    }

    /**
    * Creates a new encoder  */
    switch( pC_ewc->AudioStreamType )
    {
            //EVRC
            //        case M4SYS_kEVRC:
            //
            //            err = M4VSS3GPP_setCurrentAudioEncoder(&pC->ShellAPI,
            //                                                   pC_ewc->AudioStreamType);
            //            M4ERR_CHECK_RETURN(err);
            //
            //            pC_ewc->AudioEncParams.Format = M4ENCODER_kEVRC;
            //            pC_ewc->AudioEncParams.Frequency = M4ENCODER_k8000Hz;
            //            pC_ewc->AudioEncParams.ChannelNum = M4ENCODER_kMono;
            //            pC_ewc->AudioEncParams.Bitrate = M4VSS3GPP_EVRC_DEFAULT_BITRATE;
            //            break;

        case M4SYS_kAMR:

            err = M4VSS3GPP_setCurrentAudioEncoder(pC_ShellAPI,
                pC_ewc->AudioStreamType);
            M4ERR_CHECK_RETURN(err);

            pC_ewc->AudioEncParams.Format = M4ENCODER_kAMRNB;
            pC_ewc->AudioEncParams.Frequency = M4ENCODER_k8000Hz;
            pC_ewc->AudioEncParams.ChannelNum = M4ENCODER_kMono;
            pC_ewc->AudioEncParams.Bitrate = M4VSS3GPP_AMR_DEFAULT_BITRATE;
            pC_ewc->AudioEncParams.SpecifParam.AmrSID = M4ENCODER_kAmrNoSID;
            break;

        case M4SYS_kAAC:

            err = M4VSS3GPP_setCurrentAudioEncoder(pC_ShellAPI,
                pC_ewc->AudioStreamType);
            M4ERR_CHECK_RETURN(err);

            pC_ewc->AudioEncParams.Format = M4ENCODER_kAAC;

            switch( pC_ewc->uiSamplingFrequency )
            {
                case 8000:
                    pC_ewc->AudioEncParams.Frequency = M4ENCODER_k8000Hz;
                    break;

                case 16000:
                    pC_ewc->AudioEncParams.Frequency = M4ENCODER_k16000Hz;
                    break;

                case 22050:
                    pC_ewc->AudioEncParams.Frequency = M4ENCODER_k22050Hz;
                    break;

                case 24000:
                    pC_ewc->AudioEncParams.Frequency = M4ENCODER_k24000Hz;
                    break;

                case 32000:
                    pC_ewc->AudioEncParams.Frequency = M4ENCODER_k32000Hz;
                    break;

                case 44100:
                    pC_ewc->AudioEncParams.Frequency = M4ENCODER_k44100Hz;
                    break;

                case 48000:
                    pC_ewc->AudioEncParams.Frequency = M4ENCODER_k48000Hz;
                    break;

                default:
                    M4OSA_TRACE1_1(
                        "M4VSS3GPP_intCreateAudioEncoder: invalid input AAC sampling frequency\
                        (%d Hz), returning M4VSS3GPP_ERR_AUDIO_DECODER_INIT_FAILED",
                        pC_ewc->uiSamplingFrequency);
                    return M4VSS3GPP_ERR_AUDIO_DECODER_INIT_FAILED;
            }
            pC_ewc->AudioEncParams.ChannelNum = (pC_ewc->uiNbChannels == 1)
                ? M4ENCODER_kMono : M4ENCODER_kStereo;
            pC_ewc->AudioEncParams.SpecifParam.AacParam.Regulation =
                M4ENCODER_kAacRegulNone; //M4ENCODER_kAacBitReservoir
            /* unused */
            pC_ewc->AudioEncParams.SpecifParam.AacParam.bIS = M4OSA_FALSE;
            pC_ewc->AudioEncParams.SpecifParam.AacParam.bMS = M4OSA_FALSE;
            pC_ewc->AudioEncParams.SpecifParam.AacParam.bPNS = M4OSA_FALSE;
            pC_ewc->AudioEncParams.SpecifParam.AacParam.bTNS = M4OSA_FALSE;
            /* TODO change into highspeed asap */
            pC_ewc->AudioEncParams.SpecifParam.AacParam.bHighSpeed =
                M4OSA_FALSE;

            /* Quantify value (ceil one) */
            if( uiAudioBitrate <= 16000 )
                pC_ewc->AudioEncParams.Bitrate = 16000;

            else if( uiAudioBitrate <= 24000 )
                pC_ewc->AudioEncParams.Bitrate = 24000;

            else if( uiAudioBitrate <= 32000 )
                pC_ewc->AudioEncParams.Bitrate = 32000;

            else if( uiAudioBitrate <= 48000 )
                pC_ewc->AudioEncParams.Bitrate = 48000;

            else if( uiAudioBitrate <= 64000 )
                pC_ewc->AudioEncParams.Bitrate = 64000;

            else
                pC_ewc->AudioEncParams.Bitrate = 96000;

            /* Special requirement of our encoder */
            if( ( pC_ewc->uiNbChannels == 2)
                && (pC_ewc->AudioEncParams.Bitrate < 32000) )
                pC_ewc->AudioEncParams.Bitrate = 32000;

            break;

        default:
            M4OSA_TRACE1_1(
                "M4VSS3GPP_intResetAudioEncoder: Undefined output audio format (%d),\
                returning M4VSS3GPP_ERR_EDITING_UNSUPPORTED_AUDIO_FORMAT",
                pC_ewc->AudioStreamType);
            return M4VSS3GPP_ERR_EDITING_UNSUPPORTED_AUDIO_FORMAT;
    }

    /* Initialise the audio encoder */
#ifdef M4VSS_SUPPORT_OMX_CODECS

    M4OSA_TRACE3_1(
        "M4VSS3GPP_intResetAudioEncoder:\
        pAudioEncoderGlobalFcts->pFctInit called with userdata 0x%x",
        pC_ShellAPI->pCurrentAudioEncoderUserData);
    err = pC_ShellAPI->pAudioEncoderGlobalFcts->pFctInit(&pC_ewc->pAudioEncCtxt,
        pC_ShellAPI->pCurrentAudioEncoderUserData);

#else

    err = pC_ShellAPI->pAudioEncoderGlobalFcts->pFctInit(&pC_ewc->pAudioEncCtxt,
        M4OSA_NULL /* no HW encoder */);

#endif

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intResetAudioEncoder: pAudioEncoderGlobalFcts->pFctInit returns 0x%x",
            err);
        return err;
    }

    /* Open the audio encoder */
    err = pC_ShellAPI->pAudioEncoderGlobalFcts->pFctOpen(pC_ewc->pAudioEncCtxt,
        &pC_ewc->AudioEncParams, &pC_ewc->pAudioEncDSI,
        M4OSA_NULL /* no grabbing */);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4VSS3GPP_intResetAudioEncoder: pAudioEncoderGlobalFcts->pFctOpen returns 0x%x",
            err);
        return err;
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4VSS3GPP_intResetAudioEncoder: returning M4NO_ERROR");
    return M4NO_ERROR;
}
