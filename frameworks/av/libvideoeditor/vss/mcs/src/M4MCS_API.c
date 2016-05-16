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
 *************************************************************************
 * @file   M4MCS_API.c
 * @brief  MCS implementation (Video Compressor Service)
 * @note   This file implements the API and the processing of the MCS
 *************************************************************************
 **/

/**
 ********************************************************************
 * Includes
 ********************************************************************
 */
/**
 * OSAL headers */
#include "M4OSA_Memory.h" /**< OSAL memory management */
#include "M4OSA_Debug.h"  /**< OSAL debug management */

/* PCM samples */
#include "VideoEditorResampler.h"
/**
 * Decoder interface */
#include "M4DECODER_Common.h"

/* Encoder interface*/
#include "M4ENCODER_common.h"

/* Enable for DEBUG logging */
//#define MCS_DUMP_PCM_TO_FILE
#ifdef MCS_DUMP_PCM_TO_FILE
#include <stdio.h>
FILE *file_au_reader = NULL;
FILE *file_pcm_decoder = NULL;
FILE *file_pcm_encoder = NULL;
#endif

/* Core headers */
#include "M4MCS_API.h"
#include "M4MCS_ErrorCodes.h"
#include "M4MCS_InternalTypes.h"
#include "M4MCS_InternalConfig.h"
#include "M4MCS_InternalFunctions.h"

#ifdef M4MCS_SUPPORT_STILL_PICTURE
#include "M4MCS_StillPicture.h"
#endif /*M4MCS_SUPPORT_STILL_PICTURE*/

/* Common headers (for aac) */
#include "M4_Common.h"

#include "NXPSW_CompilerSwitches.h"

#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS
#include "M4VD_EXTERNAL_Interface.h"
#endif /* M4VSS_ENABLE_EXTERNAL_DECODERS */

#include "M4AIR_API.h"
#include "OMX_Video.h"

/* Version */
#define M4MCS_VERSION_MAJOR 3
#define M4MCS_VERSION_MINOR 4
#define M4MCS_VERSION_REVISION  3

/**
 ********************************************************************
 * Static local functions
 ********************************************************************
 */

static M4OSA_ERR M4MCS_intStepSet( M4MCS_InternalContext *pC );
static M4OSA_ERR M4MCS_intPrepareVideoDecoder(
                                    M4MCS_InternalContext *pC );
static M4OSA_ERR M4MCS_intPrepareVideoEncoder(
                                    M4MCS_InternalContext *pC );
static M4OSA_ERR M4MCS_intPrepareAudioProcessing(
                                    M4MCS_InternalContext *pC );
static M4OSA_ERR M4MCS_intPrepareWriter( M4MCS_InternalContext *pC );
static M4OSA_ERR M4MCS_intPrepareAudioBeginCut(
                                    M4MCS_InternalContext *pC );
static M4OSA_ERR M4MCS_intStepEncoding(
                                    M4MCS_InternalContext *pC,
                                    M4OSA_UInt8 *pTranscodedTime );
static M4OSA_ERR M4MCS_intStepBeginVideoJump(
                                    M4MCS_InternalContext *pC );
static M4OSA_ERR M4MCS_intStepBeginVideoDecode(
                                    M4MCS_InternalContext *pC );
static M4OSA_ERR M4MCS_intAudioNullEncoding( M4MCS_InternalContext *pC );
static M4OSA_ERR M4MCS_intAudioTranscoding( M4MCS_InternalContext *pC );
static M4OSA_ERR M4MCS_intVideoNullEncoding( M4MCS_InternalContext *pC );
static M4OSA_ERR M4MCS_intVideoTranscoding( M4MCS_InternalContext *pC );
static M4OSA_ERR M4MCS_intGetInputClipProperties(
                                    M4MCS_InternalContext   *pContext );
static M4OSA_UInt32 M4MCS_intGetFrameSize_AMRNB(
                                    M4OSA_MemAddr8 pAudioFrame );
static M4OSA_UInt32 M4MCS_intGetFrameSize_EVRC(
                                    M4OSA_MemAddr8 pAudioFrame );
static M4OSA_ERR M4MCS_intCheckMaxFileSize( M4MCS_Context pContext );
static M4VIDEOEDITING_Bitrate M4MCS_intGetNearestBitrate(
                                    M4OSA_Int32 freebitrate,
                                    M4OSA_Int8 mode );
static M4OSA_ERR M4MCS_intCleanUp_ReadersDecoders(
                                    M4MCS_InternalContext *pC );
static M4OSA_ERR M4MCS_intReallocTemporaryAU(
                                    M4OSA_MemAddr8 *addr,
                                    M4OSA_UInt32 newSize );
static M4OSA_ERR M4MCS_intCheckAndGetCodecProperties(
                                 M4MCS_InternalContext *pC);

static M4OSA_ERR M4MCS_intLimitBitratePerCodecProfileLevel(
                                 M4ENCODER_AdvancedParams* EncParams);
static M4OSA_Int32 M4MCS_intLimitBitrateForH263Enc(M4OSA_Int32 profile,
                                 M4OSA_Int32 level, M4OSA_Int32 bitrate);
static M4OSA_Int32 M4MCS_intLimitBitrateForMpeg4Enc(M4OSA_Int32 profile,
                                 M4OSA_Int32 level, M4OSA_Int32 bitrate);
static M4OSA_Int32 M4MCS_intLimitBitrateForH264Enc(M4OSA_Int32 profile,
                                 M4OSA_Int32 level, M4OSA_Int32 bitrate);

/**
 **********************************************************************
 * External function used only by VideoEditor and that does not appear
 * in the API
 **********************************************************************
 */

M4OSA_ERR M4MCS_open_normalMode( M4MCS_Context pContext,
                                 M4OSA_Void *pFileIn,
                                 M4VIDEOEDITING_FileType InputFileType,
                                 M4OSA_Void *pFileOut,
                                 M4OSA_Void *pTempFile );

/* All errors are fatal in the MCS */
#define M4ERR_CHECK_RETURN(err) if(M4NO_ERROR!=err) return err;

/* A define used with SSRC 1.04 and above to avoid taking blocks smaller
 * that the minimal block size
 */
#define M4MCS_SSRC_MINBLOCKSIZE        100

static M4OSA_UChar Tab_MCS[8] =
{
    17, 5, 3, 3, 1, 1, 1, 1
};

M4OSA_ERR H264MCS_Getinstance( NSWAVC_MCS_t ** instance )
{
    NSWAVC_MCS_t *p_bs = M4OSA_NULL;
    M4OSA_ERR err = M4NO_ERROR;
    p_bs = (NSWAVC_MCS_t *)M4OSA_32bitAlignedMalloc(sizeof(NSWAVC_MCS_t), M4MCS,
        (M4OSA_Char *)"NSWAVC_MCS_t");

    if( M4OSA_NULL == p_bs )
    {
        M4OSA_TRACE1_0("H264MCS_Getinstance: allocation error");
        return M4ERR_ALLOC;
    }

    p_bs->prev_frame_num = 0;
    p_bs->cur_frame_num = 0;
    p_bs->log2_max_frame_num_minus4 = 0;
    p_bs->prev_new_frame_num = 0;
    p_bs->is_done = 0;
    p_bs->is_first = 1;

    p_bs->m_pDecoderSpecificInfo = M4OSA_NULL;
    p_bs->m_decoderSpecificInfoSize = 0;

    p_bs->m_pEncoderSPS = M4OSA_NULL;
    p_bs->m_encoderSPSSize = 0;

    p_bs->m_pEncoderPPS = M4OSA_NULL;
    p_bs->m_encoderPPSSize = 0;

    p_bs->m_pFinalDSI = M4OSA_NULL;
    p_bs->m_pFinalDSISize = 0;

    p_bs->p_clip_sps = M4OSA_NULL;
    p_bs->m_encoder_SPS_Cnt = 0;

    p_bs->p_clip_pps = M4OSA_NULL;
    p_bs->m_encoder_PPS_Cnt = 0;

    p_bs->p_encoder_sps = M4OSA_NULL;
    p_bs->p_encoder_pps = M4OSA_NULL;

    p_bs->encoder_pps.slice_group_id = M4OSA_NULL;

    *instance = (NSWAVC_MCS_t *)p_bs;
    return err;
}

M4OSA_UInt32 H264MCS_getBits( ComBitStreamMCS_t *p_bs, M4OSA_UInt32 numBits )
{
    M4OSA_UInt32 ui32RetBits;
    M4OSA_UInt8 *pbs;
    M4OSA_Int32 bcnt;
    p_bs->i8BitCnt -= numBits;
    bcnt = p_bs->i8BitCnt;

    /* Measure the quantity of bits to be read in ui32TempBuff */
    ui32RetBits = p_bs->ui32TempBuff >> (32 - numBits);

    /* Read numBits in ui32TempBuff */
    p_bs->ui32TempBuff <<= numBits;
    p_bs->bitPos += numBits;

    if( bcnt > 24 )
    {
        return (ui32RetBits);
    }
    else
    { /* at least one byte can be buffered in ui32TempBuff */
        pbs = (M4OSA_UInt8 *)p_bs->pui8BfrPtr;

        if( bcnt < (int)(p_bs->numBitsInBuffer - p_bs->bitPos) )
        { /* not enough remaining bits in ui32TempBuff: need to be filled */
            do
            {
                /* On the fly detection of EPB byte */
                if( ( *(pbs) == 0x03)
                    && (!(( pbs[-1])
                    | (pbs[-2])))) //(p_bs->ui32LastTwoBytes & 0x0000FFFF) == 0)
                {
                    /* EPB byte found: skip it and update bitPos accordingly */
                            (pbs)++;
                            p_bs->bitPos += 8;
                        }

                        p_bs->ui32TempBuff |= *(pbs)++ << (24 - bcnt);
                        bcnt += 8;
            } while ( bcnt <= 24 );

            p_bs->pui8BfrPtr = (M4OSA_Int8 *)pbs;
            p_bs->i8BitCnt = bcnt;
            return (ui32RetBits);
        }
    }

    if( p_bs->bitPos <= p_bs->numBitsInBuffer )
    {
        return (ui32RetBits);
    }
    else
    {
        return (0);
    }
}

M4OSA_Void H264MCS_flushBits( ComBitStreamMCS_t *p_bs, M4OSA_UInt32 numBits )
{
    M4OSA_UInt8 *pbs;
    M4OSA_UInt32 bcnt;
    p_bs->i8BitCnt -= numBits;
    bcnt = p_bs->i8BitCnt;

    p_bs->ui32TempBuff <<= numBits;
    p_bs->bitPos += numBits;

    if( bcnt > 24 )
    {
        return;
    }
    else
    { /* at least one byte can be buffered in ui32TempBuff */
        pbs = (M4OSA_UInt8 *)p_bs->pui8BfrPtr;

        if( bcnt < (p_bs->numBitsInBuffer - p_bs->bitPos) )
        {   /* Not enough remaining bits in ui32TempBuff: need to be filled */
            do
            {
                /*  On the fly detection of EPB byte */
                if( ( *(pbs) == 0x03) && (!(( pbs[-1]) | (pbs[-2]))) )
                { /* JC: EPB byte found: skip it and update bitPos accordingly */
                    (pbs)++;
                    p_bs->bitPos += 8;
                }
                p_bs->ui32TempBuff |= *(pbs)++ << (24 - bcnt);
                bcnt += 8;
            } while ( bcnt <= 24 );

            p_bs->pui8BfrPtr = (M4OSA_Int8 *)pbs;
            p_bs->i8BitCnt = bcnt;
        }
    }

    return;
}

M4OSA_UInt32 H264MCS_DecVLCReadExpGolombCode( ComBitStreamMCS_t *p_bs )
{
    M4OSA_UInt32 code, l0 = 0, l1;
    /* Reading 32 Bits from local cache buffer of Bitstream structure*/
    code = p_bs->ui32TempBuff;

    /* Checking in first 3 bits*/
    if( code >> 29 )
    {
        l0 = Tab_MCS[(code >> 29)];
        code = code >> (32 - l0);
        H264MCS_flushBits(p_bs, l0);
    }
    else
        {
            if( code )
            {
                code <<= 3;

                for ( l0 = 3; code < 0x80000000; code <<= 1, l0++ );

                if( l0 < 16 ) /*all useful bits are inside the 32 bits read */
                {
                    code = code >> (31 - l0);
                    H264MCS_flushBits(p_bs, 2 * l0 + 1);
                }
                else
            { /* Read the useful bits in 2 parts */
                    l1 = ( l0 << 1) - 31;
                    code >>= l0;
                    H264MCS_flushBits(p_bs, 32);
                    code = ( code << l1) | H264MCS_getBits(p_bs, l1);
                }
            }
            else
            {
                H264MCS_flushBits(p_bs, 32);

                if( H264MCS_getBits(p_bs, 1) )
                {
                    /* if number of leading 0's is 32, the only code allowed is 1 followed
                    by 32 0's */

                    /*reading 32 more bits from bitstream buffer*/
                    code = H264MCS_getBits(p_bs, 32);

                    if( code == 0 )
                    {
                        return (code - 1);
                    }
                }
                /*if number of leading 0's is >32, then symbol is >32 bits,
                which is an error */
                //p_bs->state = _BS_ERR;
                //p_bs->flags |= _BF_SYM_ERR;
                return (0);
            }
        }

        if( 1 ) //(p_bs->state == _BS_OK)
        {
            return (code - 1);
        }
        else
        {
            return (0);
        }
    }

M4OSA_Int32 H264MCS_DecVLCReadSignedExpGolombCode( ComBitStreamMCS_t *p_bs )
{
    M4OSA_Int32 codeNo, ret;

    /* read the unsigned code number */
    codeNo = H264MCS_DecVLCReadExpGolombCode(p_bs);

    /* map to the signed value, if value is odd then it's positive,
    if even then it's negative, formula is (-1)^(k+1)*CEIL(k/2) */

    ret = (codeNo & 0x01) ? (( codeNo + 1) >> 1) : (( -codeNo) >> 1);

    return ret;
}

M4OSA_Void DecBitStreamReset_MCS( ComBitStreamMCS_t *p_bs,
                                 M4OSA_UInt32 bytes_read )
{
    p_bs->bitPos = 0;

    p_bs->lastTotalBits = 0;
    p_bs->numBitsInBuffer = bytes_read << 3;
    p_bs->readableBytesInBuffer = bytes_read;
    //p_bs->state = M4NO_ERROR;//_BS_OK;
    //p_bs->flags = 0;

    p_bs->ui32TempBuff = 0;
    p_bs->i8BitCnt = 0;
    p_bs->pui8BfrPtr = (M4OSA_Int8 *)p_bs->Buffer;
    p_bs->ui32LastTwoBytes = 0xFFFFFFFF;
    H264MCS_getBits(p_bs, 0);
}

M4OSA_ERR NSWAVCMCS_initBitstream( NSWAVC_bitStream_t_MCS *bS )
{
    bS->bitPos = 0;
    bS->byteCnt = 0;
    bS->currBuff = 0;
    bS->prevByte = 0xff;
    bS->prevPrevByte = 0xff;

    return M4NO_ERROR;
}

M4OSA_ERR NSWAVCMCS_putBits( NSWAVC_bitStream_t_MCS *bS, M4OSA_UInt32 value,
                            M4OSA_UInt8 length )
{
    M4OSA_UInt32 maskedValue = 0, temp = 0;
    M4OSA_UInt8 byteOne;

    M4OSA_UInt32 len1 = (length == 32) ? 31 : length;

    if( !(length) )
    {
        /* Length = 0, return OK*/
        return M4NO_ERROR;
    }

    maskedValue = (M4OSA_UInt32)(value &(( 1 << len1) - 1));

    if( 32 > (length + bS->bitPos) )
    {
        bS->bitPos += length;
        bS->currBuff |= maskedValue << (32 - bS->bitPos);
    }
    else
    {
        temp = (( bS->bitPos + length) - 32);

        bS->currBuff |= (maskedValue >> (temp));

        byteOne =
            bS->streamBuffer[bS->byteCnt++] = (M4OSA_UInt8)(bS->currBuff >> 24);

        if( (( bS->prevPrevByte
            == 0) & (bS->prevByte == 0) & (!(byteOne & 0xFC))) )
        {
            bS->byteCnt -= 1;
            bS->prevPrevByte = bS->streamBuffer[bS->byteCnt++] = 0x03;
            bS->prevByte = bS->streamBuffer[bS->byteCnt++] = byteOne;
        }
        else
        {
            bS->prevPrevByte = bS->prevByte;
            bS->prevByte = byteOne;
        }
        byteOne = bS->streamBuffer[bS->byteCnt++] =
            (M4OSA_UInt8)(( bS->currBuff >> 16) & 0xff);

        if( (( bS->prevPrevByte
            == 0) & (bS->prevByte == 0) & (!(byteOne & 0xFC))) )
        {
            bS->byteCnt -= 1;
            bS->prevPrevByte = bS->streamBuffer[bS->byteCnt++] = 0x03;
            bS->prevByte = bS->streamBuffer[bS->byteCnt++] = byteOne;
        }
        else
        {
            bS->prevPrevByte = bS->prevByte;
            bS->prevByte = byteOne;
        }
        byteOne = bS->streamBuffer[bS->byteCnt++] =
            (M4OSA_UInt8)(( bS->currBuff >> 8) & 0xff);

        if( (( bS->prevPrevByte
            == 0) & (bS->prevByte == 0) & (!(byteOne & 0xFC))) )
        {
            bS->byteCnt -= 1;
            bS->prevPrevByte = bS->streamBuffer[bS->byteCnt++] = 0x03;
            bS->prevByte = bS->streamBuffer[bS->byteCnt++] = byteOne;
        }
        else
        {
            bS->prevPrevByte = bS->prevByte;
            bS->prevByte = byteOne;
        }
        byteOne = bS->streamBuffer[bS->byteCnt++] =
            (M4OSA_UInt8)((bS->currBuff) &0xff);

        if( (( bS->prevPrevByte
            == 0) & (bS->prevByte == 0) & (!(byteOne & 0xFC))) )
        {
            bS->byteCnt -= 1;
            bS->prevPrevByte = bS->streamBuffer[bS->byteCnt++] = 0x03;
            bS->prevByte = bS->streamBuffer[bS->byteCnt++] = byteOne;
        }
        else
        {
            bS->prevPrevByte = bS->prevByte;
            bS->prevByte = byteOne;
        }

        bS->currBuff = 0;

        bS->currBuff |= ( maskedValue &(( 1 << temp) - 1)) << (32 - temp);

        bS->bitPos = temp;
    }

    return M4NO_ERROR;
}

M4OSA_ERR NSWAVCMCS_putBit( NSWAVC_bitStream_t_MCS *bS, M4OSA_UInt32 value )
{
    M4OSA_UInt32 maskedValue = 0, temp = 0;
    M4OSA_UInt8 byteOne;

    maskedValue = (value ? 1 : 0);

    if( 32 > (1 + bS->bitPos) )
    {
        bS->bitPos += 1;
        bS->currBuff |= maskedValue << (32 - bS->bitPos);
    }
    else
    {
        temp = 0;

        bS->currBuff |= (maskedValue);

        /* writing it to memory*/
        byteOne =
            bS->streamBuffer[bS->byteCnt++] =
            (M4OSA_UInt8)(bS->currBuff >> 24);

        if( (( bS->prevPrevByte
            == 0) & (bS->prevByte == 0) & (!(byteOne & 0xFC))) )
        {
            bS->byteCnt -= 1;
            bS->prevPrevByte = bS->streamBuffer[bS->byteCnt++] = 0x03;
            bS->prevByte = bS->streamBuffer[bS->byteCnt++] = byteOne;
        }
        else
        {
            bS->prevPrevByte = bS->prevByte;
            bS->prevByte = byteOne;
        }
        byteOne = bS->streamBuffer[bS->byteCnt++] =
            (M4OSA_UInt8)(( bS->currBuff >> 16) & 0xff);

        if( (( bS->prevPrevByte
            == 0) & (bS->prevByte == 0) & (!(byteOne & 0xFC))) )
        {
            bS->byteCnt -= 1;
            bS->prevPrevByte = bS->streamBuffer[bS->byteCnt++] = 0x03;
            bS->prevByte = bS->streamBuffer[bS->byteCnt++] = byteOne;
        }
        else
        {
            bS->prevPrevByte = bS->prevByte;
            bS->prevByte = byteOne;
        }
        byteOne = bS->streamBuffer[bS->byteCnt++] =
            (M4OSA_UInt8)(( bS->currBuff >> 8) & 0xff);

        if( (( bS->prevPrevByte
            == 0) & (bS->prevByte == 0) & (!(byteOne & 0xFC))) )
        {
            bS->byteCnt -= 1;
            bS->prevPrevByte = bS->streamBuffer[bS->byteCnt++] = 0x03;
            bS->prevByte = bS->streamBuffer[bS->byteCnt++] = byteOne;
        }
        else
        {
            bS->prevPrevByte = bS->prevByte;
            bS->prevByte = byteOne;
        }
        byteOne = bS->streamBuffer[bS->byteCnt++] =
            (M4OSA_UInt8)((bS->currBuff) &0xff);

        if( (( bS->prevPrevByte
            == 0) & (bS->prevByte == 0) & (!(byteOne & 0xFC))) )
        {
            bS->byteCnt -= 1;
            bS->prevPrevByte = bS->streamBuffer[bS->byteCnt++] = 0x03;
            bS->prevByte = bS->streamBuffer[bS->byteCnt++] = byteOne;
        }
        else
        {
            bS->prevPrevByte = bS->prevByte;
            bS->prevByte = byteOne;
        }
        bS->currBuff = 0;
        bS->bitPos = 0;
    }

    return M4NO_ERROR;
}

M4OSA_Int32 NSWAVCMCS_putRbspTbits( NSWAVC_bitStream_t_MCS *bS )
{
    M4OSA_UInt8 trailBits = 0;
    M4OSA_UInt8 byteCnt = 0;

    trailBits = (M4OSA_UInt8)(bS->bitPos % 8);

    /* Already in the byte aligned position,
    RBSP trailing bits will be 1000 0000 */
    if( 0 == trailBits )
    {
        trailBits = (1 << 7);
        NSWAVCMCS_putBits(bS, trailBits, 8);
    }
    else
    {
        trailBits = (8 - trailBits);
        NSWAVCMCS_putBit(bS, 1);
        trailBits--;

        if( trailBits )
        { /* put trailBits times zeros */
            NSWAVCMCS_putBits(bS, 0, trailBits);
        }
    }

    /* For writting the currBuff in streamBuff 4byte alignment is required*/
    byteCnt = (M4OSA_UInt8)(( bS->bitPos + 4) / 8);

    switch( byteCnt )
    {
        case 1:
            bS->streamBuffer[bS->byteCnt++] = (M4OSA_UInt8)(bS->currBuff >> 24);
            break;

        case 2:
            bS->streamBuffer[bS->byteCnt++] = (M4OSA_UInt8)(bS->currBuff >> 24);
            bS->streamBuffer[bS->byteCnt++] =
                (M4OSA_UInt8)(( bS->currBuff >> 16) & 0xff);
            break;

        case 3:
            bS->streamBuffer[bS->byteCnt++] = (M4OSA_UInt8)(bS->currBuff >> 24);
            bS->streamBuffer[bS->byteCnt++] =
                (M4OSA_UInt8)(( bS->currBuff >> 16) & 0xff);
            bS->streamBuffer[bS->byteCnt++] =
                (M4OSA_UInt8)(( bS->currBuff >> 8) & 0xff);

            break;

        default:
            /* It will not come here */
            break;
    }

    //    bS->bitPos =0;
    //    bS->currBuff = 0;

    return M4NO_ERROR;
}

M4OSA_ERR NSWAVCMCS_uExpVLC( NSWAVC_bitStream_t_MCS *bS, M4OSA_Int32 codeNum )
{

    M4OSA_Int32 loop, temp;
    M4OSA_Int32 data = 0;
    M4OSA_UInt8 codeLen = 0;

    /* The codeNum cannot be less than zero for this ue(v) */
    if( codeNum < 0 )
    {
        return 0;
    }

    /* Implementation for Encoding of the Table 9-1 in the Standard */
    temp = codeNum + 1;

    for ( loop = 0; temp != 0; loop++ )
    {
        temp /= 2;
    }

    codeLen = (( loop * 2) - 1);

    data = codeNum + 1;

    NSWAVCMCS_putBits(bS, data, codeLen);

    return M4NO_ERROR;
}

M4OSA_ERR NSWAVCMCS_sExpVLC( NSWAVC_bitStream_t_MCS *bS, M4OSA_Int32 codeNum )
{

    M4OSA_Int32 loop, temp1, temp2;
    M4OSA_Int32 data = 0;
    M4OSA_UInt8 codeLen = 0, isPositive = 0;
    M4OSA_UInt32 abscodeNum;

    if( codeNum > 0 )
    {
        isPositive = 1;
    }

    if( codeNum > 0 )
    {
        abscodeNum = codeNum;
    }
    else
    {
        abscodeNum = -codeNum;
    }

    temp1 = ( ( ( abscodeNum) << 1) - isPositive) + 1;
    temp2 = temp1;

    for ( loop = 0; loop < 16 && temp2 != 0; loop++ )
    {
        temp2 /= 2;
    }

    codeLen = ( loop * 2) - 1;

    data = temp1;

    NSWAVCMCS_putBits(bS, data, codeLen);

    return M4NO_ERROR;
}

M4OSA_ERR H264MCS_ProcessEncodedNALU(   M4OSA_Void *ainstance,
                                        M4OSA_UInt8 *inbuff,
                                        M4OSA_Int32 inbuf_size,
                                        M4OSA_UInt8 *outbuff,
                                        M4OSA_Int32 *outbuf_size )
{
    ComBitStreamMCS_t *p_bs, bs;
    NSWAVC_MCS_t *instance;
    M4OSA_UInt8 nalu_info;
    M4OSA_Int32 forbidden_bit, nal_ref_idc, nal_unit_type;
    M4OSA_Int32 first_mb_in_slice, slice_type, pic_parameter_set_id, frame_num;
    M4OSA_Int32 seq_parameter_set_id;
    M4OSA_UInt8 temp1, temp2, temp3, temp4;
    M4OSA_Int32 temp_frame_num;
    M4OSA_Int32 bitstoDiacard, bytes;
    M4OSA_UInt32 mask_bits = 0xFFFFFFFF;
    M4OSA_Int32 new_bytes, init_bit_pos;
    M4OSA_UInt32 nal_size;
    M4OSA_UInt32 cnt;
    M4OSA_UInt32 outbuffpos = 0;
    M4OSA_UInt32 nal_size_low16, nal_size_high16;
    M4OSA_UInt32 frame_size = 0;
    M4OSA_UInt32 temp = 0;

    // StageFright encoder does not provide the size in the first 4 bytes of the AU, add it
    M4OSA_Int8 *pTmpBuff1 = M4OSA_NULL;
    M4OSA_Int8 *pTmpBuff2 = M4OSA_NULL;

    p_bs = &bs;
    instance = (NSWAVC_MCS_t *)ainstance;

    M4OSA_TRACE1_2(
        "In  H264MCS_ProcessEncodedNALU with FrameSize = %d  inBuf_Size=%d",
        frame_size, inbuf_size);

    // StageFright codecs may add a start code, make sure it is not present

    if( !memcmp((void *)inbuff,
        "\x00\x00\x00\x01", 4) )
    {
        M4OSA_TRACE1_3(
            "H264MCS_ProcessNALU ERROR : NALU start code has not been removed %d "
            "0x%X 0x%X", inbuf_size, ((M4OSA_UInt32 *)inbuff)[0],
            ((M4OSA_UInt32 *)inbuff)[1]);

        return M4ERR_PARAMETER;
    }

    // StageFright encoder does not provide the size in the first 4 bytes of the AU, add it
    pTmpBuff1 = (M4OSA_Int8 *)M4OSA_32bitAlignedMalloc(inbuf_size + 4, M4MCS,
        (M4OSA_Char *)"tmpNALU");
    memcpy((void *)(pTmpBuff1 + 4), (void *)inbuff,
        inbuf_size);
    pTmpBuff1[3] = ( (M4OSA_UInt32)inbuf_size) & 0x000000FF;
    pTmpBuff1[2] = ( (M4OSA_UInt32)inbuf_size >> 8) & 0x000000FF;
    pTmpBuff1[1] = ( (M4OSA_UInt32)inbuf_size >> 16) & 0x000000FF;
    pTmpBuff1[0] = ( (M4OSA_UInt32)inbuf_size >> 24) & 0x000000FF;
    pTmpBuff2 = (M4OSA_Int8 *)inbuff;
    inbuff = (M4OSA_UInt8 *)pTmpBuff1;
    inbuf_size += 4;

    // Make sure the available size was set
    if( inbuf_size >= *outbuf_size )
    {
        M4OSA_TRACE1_1(
            "!!! H264MCS_ProcessNALU ERROR : specified available size is incorrect %d ",
            *outbuf_size);
        return M4ERR_PARAMETER;
    }



    while( (M4OSA_Int32)frame_size < inbuf_size )
    {
        mask_bits = 0xFFFFFFFF;
        p_bs->Buffer = (M4OSA_UInt8 *)(inbuff + frame_size);

        // Use unsigned value to fix errors due to bit sign extension, this fix should be generic

        nal_size_high16 = ( ( (M4OSA_UInt8 *)p_bs->Buffer)[0] << 8)
            + ((M4OSA_UInt8 *)p_bs->Buffer)[1];
        nal_size_low16 = ( ( (M4OSA_UInt8 *)p_bs->Buffer)[2] << 8)
            + ((M4OSA_UInt8 *)p_bs->Buffer)[3];

        nalu_info = (unsigned char)p_bs->Buffer[4];

        outbuff[outbuffpos] = p_bs->Buffer[4];

        p_bs->Buffer = p_bs->Buffer + 5;

        p_bs->bitPos = 0;
        p_bs->lastTotalBits = 0;
        p_bs->numBitsInBuffer = ( inbuf_size - frame_size - 5) << 3;
        p_bs->readableBytesInBuffer = inbuf_size - frame_size - 5;

        p_bs->ui32TempBuff = 0;
        p_bs->i8BitCnt = 0;
        p_bs->pui8BfrPtr = (M4OSA_Int8 *)p_bs->Buffer;
        p_bs->ui32LastTwoBytes = 0xFFFFFFFF;

        H264MCS_getBits(p_bs, 0);

        nal_size = ( nal_size_high16 << 16) + nal_size_low16;

        frame_size += nal_size + 4;

        forbidden_bit = ( nalu_info >> 7) & 1;
        nal_ref_idc = ( nalu_info >> 5) & 3;
        nal_unit_type = (nalu_info) &0x1f;

        NSWAVCMCS_initBitstream(&instance->encbs);

        instance->encbs.streamBuffer = outbuff + outbuffpos + 1;

        if( nal_unit_type == 8 )
        {
            M4OSA_TRACE1_0("Error : PPS");
            return 0;
        }

        if( nal_unit_type == 7 )
        {
            /*SPS Packet */
            M4OSA_TRACE1_0("Error : SPS");
            return 0;
        }

        if( (nal_unit_type == 5) )
        {
            instance->frame_count = 0;
            instance->POC_lsb = 0;
        }

        if( ( nal_unit_type == 1) || (nal_unit_type == 5) )
        {
            first_mb_in_slice = H264MCS_DecVLCReadExpGolombCode(p_bs);
            slice_type = H264MCS_DecVLCReadExpGolombCode(p_bs);
            pic_parameter_set_id = H264MCS_DecVLCReadExpGolombCode(p_bs);

            /* First MB in slice */
            NSWAVCMCS_uExpVLC(&instance->encbs, first_mb_in_slice);

            /* Slice Type */
            NSWAVCMCS_uExpVLC(&instance->encbs, slice_type);

            /* Picture Parameter set Id */
            pic_parameter_set_id = instance->encoder_pps.pic_parameter_set_id;
            NSWAVCMCS_uExpVLC(&instance->encbs, pic_parameter_set_id);

            temp = H264MCS_getBits(p_bs,
                instance->encoder_sps.log2_max_frame_num_minus4 + 4);
            NSWAVCMCS_putBits(&instance->encbs, instance->frame_count,
                instance->clip_sps.log2_max_frame_num_minus4 + 4);

            // In Baseline Profile: frame_mbs_only_flag should be ON
            if( nal_unit_type == 5 )
            {
                temp = H264MCS_DecVLCReadExpGolombCode(p_bs);
                NSWAVCMCS_uExpVLC(&instance->encbs, temp);
            }

            if( instance->encoder_sps.pic_order_cnt_type == 0 )
            {
                temp = H264MCS_getBits(p_bs,
                    instance->encoder_sps.log2_max_pic_order_cnt_lsb_minus4
                    + 4);

                // in baseline profile field_pic_flag should be off.
                if( instance->encoder_pps.pic_order_present_flag )
                {
                    temp = H264MCS_DecVLCReadSignedExpGolombCode(p_bs);
                }
            }

            if( ( instance->encoder_sps.pic_order_cnt_type == 1)
                && (instance->encoder_sps.delta_pic_order_always_zero_flag) )
            {
                temp = H264MCS_DecVLCReadSignedExpGolombCode(p_bs);

                // in baseline profile field_pic_flag should be off.
                if( instance->encoder_pps.pic_order_present_flag )
                {
                    temp = H264MCS_DecVLCReadSignedExpGolombCode(p_bs);
                }
            }

            if( instance->clip_sps.pic_order_cnt_type == 0 )
            {
                NSWAVCMCS_putBits(&instance->encbs, instance->POC_lsb,
                    instance->clip_sps.log2_max_pic_order_cnt_lsb_minus4 + 4);

                // in baseline profile field_pic_flag should be off.
                if( instance->encoder_pps.pic_order_present_flag )
                {
                    NSWAVCMCS_sExpVLC(&instance->encbs, 0);
                }
            }

            if( ( instance->clip_sps.pic_order_cnt_type == 1)
                && (instance->clip_sps.delta_pic_order_always_zero_flag) )
            {
                NSWAVCMCS_sExpVLC(&instance->encbs, 0);

                // in baseline profile field_pic_flag should be off.
                if( instance->encoder_pps.pic_order_present_flag )
                {
                    NSWAVCMCS_sExpVLC(&instance->encbs, 0);
                }
            }

            cnt = p_bs->bitPos & 0x7;

            if( cnt )
            {
                cnt = 8 - cnt;
                temp = H264MCS_getBits(p_bs, cnt);
                NSWAVCMCS_putBits(&instance->encbs, temp, cnt);
            }

            cnt = p_bs->bitPos >> 3;

            while( cnt < (nal_size - 2) )
            {
                temp = H264MCS_getBits(p_bs, 8);
                NSWAVCMCS_putBits(&instance->encbs, temp, 8);
                cnt = p_bs->bitPos >> 3;
            }

            temp = H264MCS_getBits(p_bs, 8);

            if( temp != 0 )
            {
                cnt = 0;

                while( ( temp & 0x1) == 0 )
                {
                    cnt++;
                    temp = temp >> 1;
                }
                cnt++;
                temp = temp >> 1;

                if( 8 - cnt )
                {
                    NSWAVCMCS_putBits(&instance->encbs, temp, (8 - cnt));
                }

                NSWAVCMCS_putRbspTbits(&instance->encbs);
            }
            else
            {

                M4OSA_TRACE1_1(
                    "H264MCS_ProcessEncodedNALU : 13 temp = 0 trailing bits = %d",
                    instance->encbs.bitPos % 8);

                if( instance->encbs.bitPos % 8 )
                {
                    NSWAVCMCS_putBits(&instance->encbs, 0,
                        (8 - instance->encbs.bitPos % 8));
                }
            }

            temp = instance->encbs.byteCnt;
            temp = temp + 1;

            outbuffpos = outbuffpos + temp;
        }
    }

    *outbuf_size = outbuffpos;

    instance->POC_lsb = instance->POC_lsb + 1;

    if( instance->POC_lsb == instance->POC_lsb_mod )
    {
        instance->POC_lsb = 0;
    }
    instance->frame_count = instance->frame_count + 1;

    if( instance->frame_count == instance->frame_mod_count )
    {
        instance->frame_count = 0;
    }

    // StageFright encoder does not provide the size in the first 4 bytes of the AU, add it

    free(pTmpBuff1);
    pTmpBuff1 = M4OSA_NULL;
    inbuff = (M4OSA_UInt8 *)pTmpBuff2;

    return M4NO_ERROR;
}

M4OSA_Int32 DecSPSMCS( ComBitStreamMCS_t *p_bs,
                      ComSequenceParameterSet_t_MCS *sps )
{
    M4OSA_UInt32 i;
    M4OSA_Int32 temp_max_dpb_size;
    M4OSA_Int32 nb_ignore_bits;
    M4OSA_Int32 error;
    M4OSA_UInt8 profile_idc, level_idc, reserved_zero_4bits,
        seq_parameter_set_id;
    M4OSA_UInt8 constraint_set0_flag, constraint_set1_flag,
        constraint_set2_flag, constraint_set3_flag;

    sps->profile_idc = (M4OSA_UInt8)H264MCS_getBits(p_bs, 8);
    sps->constraint_set0_flag = (M4OSA_Bool)H264MCS_getBits(p_bs, 1);
    sps->constraint_set1_flag = (M4OSA_Bool)H264MCS_getBits(p_bs, 1);
    sps->constraint_set2_flag = (M4OSA_Bool)H264MCS_getBits(p_bs, 1);
    sps->constraint_set3_flag = (M4OSA_Bool)H264MCS_getBits(p_bs, 1);
    reserved_zero_4bits = (M4OSA_UInt8)H264MCS_getBits(p_bs, 4);
    sps->level_idc = (M4OSA_UInt8)H264MCS_getBits(p_bs, 8);
    sps->seq_parameter_set_id =
        (M4OSA_UInt8)H264MCS_DecVLCReadExpGolombCode(p_bs);
    sps->log2_max_frame_num_minus4 =
        (M4OSA_UInt8)H264MCS_DecVLCReadExpGolombCode(p_bs);
    sps->MaxFrameNum = 1 << (sps->log2_max_frame_num_minus4 + 4);
    sps->pic_order_cnt_type =
        (M4OSA_UInt8)H264MCS_DecVLCReadExpGolombCode(p_bs);

    if (sps->pic_order_cnt_type == 0)
    {
        sps->log2_max_pic_order_cnt_lsb_minus4 =
            (M4OSA_UInt8)H264MCS_DecVLCReadExpGolombCode(p_bs);
        sps->MaxPicOrderCntLsb =
            1 << (sps->log2_max_pic_order_cnt_lsb_minus4 + 4);
    }
    else if( sps->pic_order_cnt_type == 1 )
    {
        sps->delta_pic_order_always_zero_flag =
            (M4OSA_Bool)H264MCS_getBits(p_bs, 1);

        // This fix should be generic to remove codec dependency

        sps->offset_for_non_ref_pic =
            H264MCS_DecVLCReadSignedExpGolombCode(p_bs);
        sps->offset_for_top_to_bottom_field =
            H264MCS_DecVLCReadSignedExpGolombCode(p_bs);


        /*num_ref_frames_in_pic_order_cnt_cycle must be in the range 0, 255*/

        sps->num_ref_frames_in_pic_order_cnt_cycle =
            (M4OSA_UInt8)H264MCS_DecVLCReadExpGolombCode(p_bs);

        /* compute deltaPOC */
        sps->expectedDeltaPerPicOrderCntCycle = 0;

        for ( i = 0; i < sps->num_ref_frames_in_pic_order_cnt_cycle; i++ )
        {
            // This fix should be generic to remove codec dependency
            sps->offset_for_ref_frame[i] =
                H264MCS_DecVLCReadSignedExpGolombCode(p_bs);

            sps->expectedDeltaPerPicOrderCntCycle +=
                sps->offset_for_ref_frame[i];
        }
    }

    /* num_ref_frames must be in the range 0,16 */
    sps->num_ref_frames = (M4OSA_UInt8)H264MCS_DecVLCReadExpGolombCode(p_bs);
    sps->gaps_in_frame_num_value_allowed_flag =
        (M4OSA_Bool)H264MCS_getBits(p_bs, 1);

    sps->pic_width_in_mbs_minus1 =
        (M4OSA_UInt16)H264MCS_DecVLCReadExpGolombCode(p_bs);
    sps->pic_height_in_map_units_minus1 =
        (M4OSA_UInt16)H264MCS_DecVLCReadExpGolombCode(p_bs);

    sps->frame_mbs_only_flag = (M4OSA_Bool)H264MCS_getBits(p_bs, 1);

    if (!sps->frame_mbs_only_flag)
    {
        sps->mb_adaptive_frame_field_flag =
            (M4OSA_Bool)H264MCS_getBits(p_bs, 1);
    }
    else
    {
        sps->mb_adaptive_frame_field_flag = 0;
    }

    sps->PicWidthInMbs = sps->pic_width_in_mbs_minus1 + 1;
    sps->FrameHeightInMbs = ( 2 - sps->frame_mbs_only_flag) * \
        (sps->pic_height_in_map_units_minus1 + 1);
#ifdef _CAP_FMO_

    sps->NumSliceGroupMapUnits =
        sps->PicWidthInMbs * (sps->pic_height_in_map_units_minus1 + 1);
    sps->MaxPicSizeInMbs = sps->PicWidthInMbs * sps->FrameHeightInMbs;

#endif /*_CAP_FMO_*/

    sps->direct_8x8_inference_flag = (M4OSA_Bool)H264MCS_getBits(p_bs, 1);

    if( sps->frame_mbs_only_flag == 0 )
        sps->direct_8x8_inference_flag = 1;

    sps->frame_cropping_flag = (M4OSA_Bool)H264MCS_getBits(p_bs, 1);

    if( sps->frame_cropping_flag )
    {
        sps->frame_crop_left_offset = H264MCS_DecVLCReadExpGolombCode(p_bs);
        sps->frame_crop_right_offset = H264MCS_DecVLCReadExpGolombCode(p_bs);
        sps->frame_crop_top_offset = H264MCS_DecVLCReadExpGolombCode(p_bs);
        sps->frame_crop_bottom_offset = H264MCS_DecVLCReadExpGolombCode(p_bs);
    }
    else
    {
        sps->frame_crop_left_offset = 0;
        sps->frame_crop_right_offset = 0;
        sps->frame_crop_top_offset = 0;
        sps->frame_crop_bottom_offset = 0;
    }

    sps->vui_parameters_present_flag = (M4OSA_Bool)H264MCS_getBits(p_bs, 1);

    if (sps->vui_parameters_present_flag) {
        /* no error message as stream can be decoded without VUI messages */
    }

    return M4NO_ERROR;
}

M4OSA_Int32 DecPPSMCS( ComBitStreamMCS_t *p_bs,
                      ComPictureParameterSet_t_MCS *pps )
{
    M4OSA_Int32 error;
    M4OSA_UInt32 pic_parameter_set_id;

#ifdef _CAP_FMO_
    M4OSA_UInt32 i, length, v;
#endif

    M4OSA_Int32 nb_ignore_bits;

    pic_parameter_set_id = H264MCS_DecVLCReadExpGolombCode(p_bs);
    pps->pic_parameter_set_id = (M4OSA_UInt8)pic_parameter_set_id;

    pps->seq_parameter_set_id =
        (M4OSA_UInt8)H264MCS_DecVLCReadExpGolombCode(p_bs);

    /* entropy_coding_mode_flag must be 0 or 1 */
    pps->entropy_coding_mode_flag = (M4OSA_Bool)H264MCS_getBits(p_bs, 1);
    pps->pic_order_present_flag = (M4OSA_Bool)H264MCS_getBits(p_bs, 1);

    pps->num_slice_groups_minus1 =
        (M4OSA_UInt8)H264MCS_DecVLCReadExpGolombCode(p_bs);

#ifdef _CAP_FMO_
    /* FMO stuff begins here */

    pps->map_initialized = FALSE;

    if( pps->num_slice_groups_minus1 > 0 )
    {
        pps->slice_group_map_type =
            (M4OSA_UInt8)H264MCS_DecVLCReadExpGolombCode(p_bs);

        switch( pps->slice_group_map_type )
        {
            case 0:
                for ( i = 0; i <= pps->num_slice_groups_minus1; i++ )
                {
                    pps->run_length_minus1[i] =
                        (M4OSA_UInt16)H264MCS_DecVLCReadExpGolombCode(p_bs);
                }
                break;

            case 2:
                for ( i = 0; i < pps->num_slice_groups_minus1; i++ )
                {
                    pps->top_left[i] =
                        (M4OSA_UInt16)H264MCS_DecVLCReadExpGolombCode(p_bs);
                    pps->bottom_right[i] =
                        (M4OSA_UInt16)H264MCS_DecVLCReadExpGolombCode(p_bs);
                }
                break;

            case 3:
            case 4:
            case 5:
                pps->slice_group_change_direction_flag =
                    (M4OSA_Bool)H264MCS_getBits(p_bs, 1);
                pps->slice_group_change_rate_minus1 =
                    (M4OSA_UInt16)H264MCS_DecVLCReadExpGolombCode(p_bs);
                break;

            case 6:
                pps->pic_size_in_map_units_minus1 =
                    (M4OSA_UInt16)H264MCS_DecVLCReadExpGolombCode(p_bs);

                pps->slice_group_id = (H264UInt8
                    *)M4H264Dec_malloc((pps->pic_size_in_map_units_minus1
                    + 1), M4H264_COREID, (M4OSA_Char *)"PPS");

                if (M4OSA_NULL == pps->slice_group_id)
                {
                    M4OSA_TRACE1_0("DecPPSMCS: allocation error");
                    return M4ERR_ALLOC;
                }

                for ( length = 0, v = pps->num_slice_groups_minus1 + 1; v != 0;
                    v >>= 1, length++ );

                    for ( i = 0; i <= pps->pic_size_in_map_units_minus1; i++ )
                    {
                        pps->slice_group_id[i] =
                            (M4OSA_UInt8)getBits(p_vlc_engine->p_bs, length);
                    }
                    break;
        }
    }
    else
    {
        pps->slice_group_map_type = 0;
    }
    /* End of FMO stuff */

#else

#endif /* _CAP_FMO_ */

    /* num_ref_idx_l0_active_minus1 must be in the range 0, 31 */

    pps->num_ref_idx_l0_active_minus1 =
        (M4OSA_UInt8)H264MCS_DecVLCReadExpGolombCode(p_bs);
    /* num_ref_idx_l1_active_minus1 must be in the range 0, 31 */
    pps->num_ref_idx_l1_active_minus1 =
        (M4OSA_UInt8)H264MCS_DecVLCReadExpGolombCode(p_bs);
    pps->weighted_pred_flag = (M4OSA_Bool)H264MCS_getBits(p_bs, 1);

    /* weighted_bipred_idc must be in the range 0,2 */
    pps->weighted_bipred_idc = (M4OSA_Bool)H264MCS_getBits(p_bs, 2);

    /* pic_init_qp_minus26 must be in the range -26,25 */
    pps->pic_init_qp_minus26 =
        (M4OSA_Int16)H264MCS_DecVLCReadSignedExpGolombCode(p_bs);

    /* pic_init_qs_minus26 must be in the range -26,25 */
    pps->pic_init_qs_minus26 =
        (M4OSA_Int16)H264MCS_DecVLCReadSignedExpGolombCode(p_bs);

    /* chroma_qp_index_offset must be in the range -12,+12 */
    pps->chroma_qp_index_offset =
        (M4OSA_Int16)H264MCS_DecVLCReadSignedExpGolombCode(p_bs);
    pps->deblocking_filter_control_present_flag =
        (M4OSA_Bool)H264MCS_getBits(p_bs, 1);
    pps->constrained_intra_pred_flag = (M4OSA_Bool)H264MCS_getBits(p_bs, 1);
    pps->redundant_pic_cnt_present_flag = (M4OSA_Bool)H264MCS_getBits(p_bs, 1);

    return M4NO_ERROR;
}

M4OSA_ERR H264MCS_ProcessSPS_PPS( NSWAVC_MCS_t *instance, M4OSA_UInt8 *inbuff,
                                 M4OSA_Int32 inbuf_size )
{
    ComBitStreamMCS_t *p_bs, bs;
    ComBitStreamMCS_t *p_bs1, bs1;

    M4OSA_UInt8 nalu_info = 0;
    M4OSA_Int32 forbidden_bit, nal_ref_idc, nal_unit_type;
    M4OSA_Int32 first_mb_in_slice, slice_type, pic_parameter_set_id = 0,
        frame_num;
    M4OSA_Int32 seq_parameter_set_id;
    M4OSA_UInt8 temp1, temp2, temp3, temp4;
    M4OSA_Int32 temp_frame_num;
    M4OSA_Int32 bitstoDiacard, bytes;
    M4OSA_UInt32 mask_bits = 0xFFFFFFFF;
    M4OSA_Int32 new_bytes, init_bit_pos;
    M4OSA_UInt32 nal_size = 0;
    M4OSA_UInt32 cnt, cnt1;
    M4OSA_UInt32 outbuffpos = 0;
    M4OSA_UInt32 nal_size_low16, nal_size_high16;
    M4OSA_UInt32 frame_size = 0;
    M4OSA_UInt32 temp = 0;
    M4OSA_UInt8 *lClipDSI;
    M4OSA_UInt8 *lClipDSI_PPS_start;
    M4OSA_UInt32 lClipDSI_PPS_offset = 0;

    M4OSA_UInt8 *lPPS_Buffer = M4OSA_NULL;
    M4OSA_UInt32 lPPS_Buffer_Size = 0;

    M4OSA_UInt32 lSize, lSize1;
    M4OSA_UInt32 lActiveSPSID_Clip;
    M4OSA_UInt32 lClipPPSRemBits = 0;

    M4OSA_UInt32 lEncoder_SPSID = 0;
    M4OSA_UInt32 lEncoder_PPSID = 0;
    M4OSA_UInt32 lEncoderPPSRemBits = 0;
    M4OSA_UInt32 lFound = 0;
    M4OSA_UInt32 size;

    M4OSA_UInt8 Clip_SPSID[32] = { 0 };
    M4OSA_UInt8 Clip_UsedSPSID[32] = { 0 };
    M4OSA_UInt8 Clip_PPSID[256] = { 0 };
    M4OSA_UInt8 Clip_SPSID_in_PPS[256] = { 0 };
    M4OSA_UInt8 Clip_UsedPPSID[256] = { 0 };
    M4OSA_ERR err = M4NO_ERROR;

    p_bs = &bs;
    p_bs1 = &bs1;

    /* Find the active SPS ID */
    M4OSA_DEBUG_IF2((M4OSA_NULL == instance), M4ERR_PARAMETER,
        "H264MCS_ProcessSPS_PPS: instance is M4OSA_NULL");

    instance->m_Num_Bytes_NALUnitLength =
            (instance->m_pDecoderSpecificInfo[4] & 0x03) + 1;

    instance->m_encoder_SPS_Cnt = instance->m_pDecoderSpecificInfo[5] & 0x1F;

    lClipDSI = instance->m_pDecoderSpecificInfo + 6;

    lClipDSI_PPS_offset = 6;

    for ( cnt = 0; cnt < instance->m_encoder_SPS_Cnt; cnt++ )
    {
        lSize = ( lClipDSI[0] << 8) + lClipDSI[1];
        lClipDSI = lClipDSI + 2;

        p_bs->Buffer = (M4OSA_UInt8 *)(lClipDSI + 4);
        DecBitStreamReset_MCS(p_bs, lSize - 4);

        Clip_SPSID[cnt] = H264MCS_DecVLCReadExpGolombCode(p_bs);
        Clip_UsedSPSID[Clip_SPSID[cnt]] = 1;

        lClipDSI = lClipDSI + lSize;
        lClipDSI_PPS_offset = lClipDSI_PPS_offset + 2 + lSize;
    }

    instance->m_encoder_PPS_Cnt = lClipDSI[0];
    lClipDSI = lClipDSI + 1;

    lClipDSI_PPS_start = lClipDSI;

    for ( cnt = 0; cnt < instance->m_encoder_PPS_Cnt; cnt++ )
    {
        lSize = ( lClipDSI[0] << 8) + lClipDSI[1];
        lClipDSI = lClipDSI + 2;

        p_bs->Buffer = (M4OSA_UInt8 *)(lClipDSI + 1);
        DecBitStreamReset_MCS(p_bs, lSize - 1);

        Clip_PPSID[cnt] = H264MCS_DecVLCReadExpGolombCode(p_bs);
        Clip_UsedPPSID[Clip_PPSID[cnt]] = 1;
        Clip_SPSID_in_PPS[Clip_PPSID[cnt]] =
            H264MCS_DecVLCReadExpGolombCode(p_bs);

        lClipDSI = lClipDSI + lSize;
    }

    /* Find the clip SPS ID used at the cut start frame */
    while( ( (M4OSA_Int32)frame_size) < inbuf_size )
    {
        mask_bits = 0xFFFFFFFF;
        p_bs->Buffer = (M4OSA_UInt8 *)(inbuff + frame_size);

        switch( instance->m_Num_Bytes_NALUnitLength )
        {
            case 1:
                nal_size = (unsigned char)p_bs->Buffer[0];
                nalu_info = (unsigned char)p_bs->Buffer[1];
                p_bs->Buffer = p_bs->Buffer + 2;

                break;

            case 2:
                nal_size_high16 = ( p_bs->Buffer[0] << 8) + p_bs->Buffer[1];
                nal_size = nal_size_high16;
                nalu_info = (unsigned char)p_bs->Buffer[2];
                p_bs->Buffer = p_bs->Buffer + 3;

                break;

            case 4:
                nal_size_high16 = ( p_bs->Buffer[0] << 8) + p_bs->Buffer[1];
                nal_size_low16 = ( p_bs->Buffer[2] << 8) + p_bs->Buffer[3];
                nal_size = ( nal_size_high16 << 16) + nal_size_low16;
                nalu_info = (unsigned char)p_bs->Buffer[4];
                p_bs->Buffer = p_bs->Buffer + 5;

                break;
        }

        if (nal_size == 0) {
            M4OSA_TRACE1_1("0 size nal unit at line %d", __LINE__);
            frame_size += instance->m_Num_Bytes_NALUnitLength;
            continue;
        }

        p_bs->bitPos = 0;
        p_bs->lastTotalBits = 0;
        p_bs->numBitsInBuffer =
            ( inbuf_size - frame_size - instance->m_Num_Bytes_NALUnitLength - 1)
            << 3;
        p_bs->readableBytesInBuffer =
            inbuf_size - frame_size - instance->m_Num_Bytes_NALUnitLength - 1;

        p_bs->ui32TempBuff = 0;
        p_bs->i8BitCnt = 0;
        p_bs->pui8BfrPtr = (M4OSA_Int8 *)p_bs->Buffer;
        p_bs->ui32LastTwoBytes = 0xFFFFFFFF;

        H264MCS_getBits(p_bs, 0);

        frame_size += nal_size + instance->m_Num_Bytes_NALUnitLength;

        forbidden_bit = ( nalu_info >> 7) & 1;
        nal_ref_idc = ( nalu_info >> 5) & 3;
        nal_unit_type = (nalu_info) &0x1f;

        if( nal_unit_type == 8 )
        {
            M4OSA_TRACE1_0("H264MCS_ProcessSPS_PPS() Error: PPS");
            return err;
        }

        if( nal_unit_type == 7 )
        {
            /*SPS Packet */
            M4OSA_TRACE1_0("H264MCS_ProcessSPS_PPS() Error: SPS");
            return err;
        }

        if( ( nal_unit_type == 1) || (nal_unit_type == 5) )
        {
            first_mb_in_slice = H264MCS_DecVLCReadExpGolombCode(p_bs);
            slice_type = H264MCS_DecVLCReadExpGolombCode(p_bs);
            pic_parameter_set_id = H264MCS_DecVLCReadExpGolombCode(p_bs);
            break;
        }
    }

    lActiveSPSID_Clip = Clip_SPSID_in_PPS[pic_parameter_set_id];

    instance->final_SPS_ID = lActiveSPSID_Clip;
    /* Do we need to add encoder PPS to clip PPS */

    lClipDSI = lClipDSI_PPS_start;

    for ( cnt = 0; cnt < instance->m_encoder_PPS_Cnt; cnt++ )
    {
        lSize = ( lClipDSI[0] << 8) + lClipDSI[1];
        lClipDSI = lClipDSI + 2;

        if( lActiveSPSID_Clip == Clip_SPSID_in_PPS[Clip_PPSID[cnt]] )
        {
            lPPS_Buffer = lClipDSI + 1;
            lPPS_Buffer_Size = lSize - 1;

            p_bs->Buffer = (M4OSA_UInt8 *)(lClipDSI + 1);
            DecBitStreamReset_MCS(p_bs, lSize - 1);

            Clip_PPSID[cnt] = H264MCS_DecVLCReadExpGolombCode(p_bs);
            Clip_UsedPPSID[Clip_SPSID[cnt]] = 1;
            Clip_SPSID_in_PPS[cnt] = H264MCS_DecVLCReadExpGolombCode(p_bs);
            lClipPPSRemBits = ( lSize - 1) << 3;
            lClipPPSRemBits -= p_bs->bitPos;

            temp = lClipDSI[lSize - 1];

            cnt1 = 0;

            while( ( temp & 0x1) == 0 )
            {
                cnt1++;
                temp = temp >> 1;
            }
            cnt1++;
            lClipPPSRemBits -= cnt1;

            lSize1 = instance->m_encoderPPSSize - 1;
            p_bs1->Buffer = (M4OSA_UInt8 *)(instance->m_pEncoderPPS + 1);
            DecBitStreamReset_MCS(p_bs1, lSize1);

            lEncoder_PPSID = H264MCS_DecVLCReadExpGolombCode(p_bs1);
            lEncoder_SPSID = H264MCS_DecVLCReadExpGolombCode(p_bs1);

            lEncoderPPSRemBits = ( lSize1) << 3;
            lEncoderPPSRemBits -= p_bs1->bitPos;

            temp = instance->m_pEncoderPPS[lSize1];

            cnt1 = 0;

            while( ( temp & 0x1) == 0 )
            {
                cnt1++;
                temp = temp >> 1;
            }
            cnt1++;
            lEncoderPPSRemBits -= cnt1;

            if( lEncoderPPSRemBits == lClipPPSRemBits )
            {
                while( lEncoderPPSRemBits > 8 )
                {
                    temp1 = H264MCS_getBits(p_bs, 8);
                    temp2 = H264MCS_getBits(p_bs1, 8);
                    lEncoderPPSRemBits = lEncoderPPSRemBits - 8;

                    if( temp1 != temp2 )
                    {
                        break;
                    }
                }

                if( lEncoderPPSRemBits < 8 )
                {
                    if( lEncoderPPSRemBits )
                    {
                        temp1 = H264MCS_getBits(p_bs, lEncoderPPSRemBits);
                        temp2 = H264MCS_getBits(p_bs1, lEncoderPPSRemBits);

                        if( temp1 == temp2 )
                        {
                            lFound = 1;
                        }
                    }
                    else
                    {
                        lFound = 1;
                    }
                }
                break;
            }
        }

        lClipDSI = lClipDSI + lSize;
    }

    /* Form the final SPS and PPS data */

    if( lFound == 1 )
    {
        /* No need to add PPS */
        instance->final_PPS_ID = Clip_PPSID[cnt];

        instance->m_pFinalDSI =
            (M4OSA_UInt8 *)M4OSA_32bitAlignedMalloc(instance->m_decoderSpecificInfoSize,
            M4MCS, (M4OSA_Char *)"instance->m_pFinalDSI");

        if( instance->m_pFinalDSI == M4OSA_NULL )
        {
            M4OSA_TRACE1_0("instance->m_pFinalDSI: allocation error");
            return M4ERR_ALLOC;
        }

        instance->m_pFinalDSISize = instance->m_decoderSpecificInfoSize;
        memcpy((void *)instance->m_pFinalDSI,
            (void *)instance->m_pDecoderSpecificInfo,
            instance->m_decoderSpecificInfoSize);
    }
    else
    {
        /* ADD PPS */
        /* find the free PPS ID */

        cnt = 0;

        while( Clip_UsedPPSID[cnt] )
        {
            cnt++;
        }
        instance->final_PPS_ID = cnt;

        size = instance->m_decoderSpecificInfoSize + instance->m_encoderPPSSize
            + 10;

        instance->m_pFinalDSI = (M4OSA_UInt8 *)M4OSA_32bitAlignedMalloc(size, M4MCS,
            (M4OSA_Char *)"instance->m_pFinalDSI");

        if( instance->m_pFinalDSI == M4OSA_NULL )
        {
            M4OSA_TRACE1_0("instance->m_pFinalDSI: allocation error");
            return M4ERR_ALLOC;
        }

        memcpy((void *)instance->m_pFinalDSI,
            (void *)instance->m_pDecoderSpecificInfo,
            instance->m_decoderSpecificInfoSize);

        temp = instance->m_pFinalDSI[lClipDSI_PPS_offset];
        temp = temp + 1;
        instance->m_pFinalDSI[lClipDSI_PPS_offset] = temp;

        //temp = instance->m_pEncoderPPS[0];
        lSize1 = instance->m_encoderPPSSize - 1;
        p_bs1->Buffer = (M4OSA_UInt8 *)(instance->m_pEncoderPPS + 1);
        DecBitStreamReset_MCS(p_bs1, lSize1);

        lEncoder_PPSID = H264MCS_DecVLCReadExpGolombCode(p_bs1);
        lEncoder_SPSID = H264MCS_DecVLCReadExpGolombCode(p_bs1);

        lEncoderPPSRemBits = ( lSize1) << 3;
        lEncoderPPSRemBits -= p_bs1->bitPos;

        temp = instance->m_pEncoderPPS[lSize1];

        cnt1 = 0;

        while( ( temp & 0x1) == 0 )
        {
            cnt1++;
            temp = temp >> 1;
        }
        cnt1++;
        lEncoderPPSRemBits -= cnt1;

        instance->m_pFinalDSI[instance->m_decoderSpecificInfoSize + 2] =
            instance->m_pEncoderPPS[0];

        NSWAVCMCS_initBitstream(&instance->encbs);
        instance->encbs.streamBuffer =
            &(instance->m_pFinalDSI[instance->m_decoderSpecificInfoSize + 3]);
        lPPS_Buffer = instance->encbs.streamBuffer;

        NSWAVCMCS_uExpVLC(&instance->encbs, instance->final_PPS_ID);
        NSWAVCMCS_uExpVLC(&instance->encbs, instance->final_SPS_ID);

        while( lEncoderPPSRemBits > 8 )
        {
            temp = H264MCS_getBits(p_bs1, 8);
            NSWAVCMCS_putBits(&instance->encbs, temp, 8);
            lEncoderPPSRemBits = lEncoderPPSRemBits - 8;
        }

        if( lEncoderPPSRemBits )
        {
            temp = H264MCS_getBits(p_bs1, lEncoderPPSRemBits);
            NSWAVCMCS_putBits(&instance->encbs, temp, lEncoderPPSRemBits);
        }
        NSWAVCMCS_putRbspTbits(&instance->encbs);

        temp = instance->encbs.byteCnt;
        lPPS_Buffer_Size = temp;
        temp = temp + 1;

        instance->m_pFinalDSI[instance->m_decoderSpecificInfoSize] =
            ( temp >> 8) & 0xFF;
        instance->m_pFinalDSI[instance->m_decoderSpecificInfoSize + 1] =
            (temp) &0xFF;
        instance->m_pFinalDSISize =
            instance->m_decoderSpecificInfoSize + 2 + temp;
    }

    /* Decode the clip SPS */

    lClipDSI = instance->m_pDecoderSpecificInfo + 6;

    lClipDSI_PPS_offset = 6;

    for ( cnt = 0; cnt < instance->m_encoder_SPS_Cnt; cnt++ )
    {
        lSize = ( lClipDSI[0] << 8) + lClipDSI[1];
        lClipDSI = lClipDSI + 2;

        if( Clip_SPSID[cnt] == instance->final_SPS_ID )
        {
            p_bs->Buffer = (M4OSA_UInt8 *)(lClipDSI + 1);
            DecBitStreamReset_MCS(p_bs, lSize - 1);

            err = DecSPSMCS(p_bs, &instance->clip_sps);
            if(err != M4NO_ERROR) {
                return M4ERR_PARAMETER;
            }

            //Clip_SPSID[cnt] = H264MCS_DecVLCReadExpGolombCode(p_bs);
            //Clip_UsedSPSID[Clip_SPSID[cnt]] = 1;
            break;
        }

        lClipDSI = lClipDSI + lSize;
    }

    /* Decode encoder SPS */
    p_bs->Buffer = (M4OSA_UInt8 *)(instance->m_pEncoderSPS + 1);
    DecBitStreamReset_MCS(p_bs, instance->m_encoderSPSSize - 1);
    err = DecSPSMCS(p_bs, &instance->encoder_sps);
    if(err != M4NO_ERROR) {
        return M4ERR_PARAMETER;
    }

    if( instance->encoder_sps.num_ref_frames
    > instance->clip_sps.num_ref_frames )
    {
        return 100; //not supported
    }

    p_bs->Buffer = (M4OSA_UInt8 *)lPPS_Buffer;
    DecBitStreamReset_MCS(p_bs, lPPS_Buffer_Size);
    DecPPSMCS(p_bs, &instance->encoder_pps);

    instance->frame_count = 0;
    instance->frame_mod_count =
        1 << (instance->clip_sps.log2_max_frame_num_minus4 + 4);

    instance->POC_lsb = 0;
    instance->POC_lsb_mod =
        1 << (instance->clip_sps.log2_max_pic_order_cnt_lsb_minus4 + 4);

    return M4NO_ERROR;
}

M4OSA_ERR H264MCS_ProcessNALU( NSWAVC_MCS_t *ainstance, M4OSA_UInt8 *inbuff,
                               M4OSA_Int32 inbuf_size, M4OSA_UInt8 *outbuff,
                               M4OSA_Int32 *outbuf_size )
{
    ComBitStreamMCS_t *p_bs, bs;
    NSWAVC_MCS_t *instance;
    M4OSA_UInt8 nalu_info;
    M4OSA_Int32 forbidden_bit, nal_ref_idc, nal_unit_type;
    M4OSA_Int32 first_mb_in_slice, slice_type, pic_parameter_set_id, frame_num;
    M4OSA_Int32 seq_parameter_set_id;
    M4OSA_UInt8 temp1, temp2, temp3, temp4;
    M4OSA_Int32 temp_frame_num;
    M4OSA_Int32 bitstoDiacard, bytes;
    M4OSA_UInt32 mask_bits = 0xFFFFFFFF;
    M4OSA_Int32 new_bytes, init_bit_pos;
    M4OSA_UInt32 nal_size;
    M4OSA_UInt32 cnt;
    M4OSA_UInt32 outbuffpos = 0;
    //#ifndef DGR_FIX // + new
    M4OSA_UInt32 nal_size_low16, nal_size_high16;
    //#endif // + end new
    M4OSA_UInt32 frame_size = 0;
    M4OSA_UInt32 temp = 0;
    M4OSA_ERR err = M4NO_ERROR;
    M4OSA_UInt8 *buff;

    p_bs = &bs;
    instance = (NSWAVC_MCS_t *)ainstance;
    M4OSA_DEBUG_IF2((M4OSA_NULL == instance), M4ERR_PARAMETER,
        "H264MCS_ProcessNALU: instance is M4OSA_NULL");

    if( instance->is_done )
        return err;

    inbuff[0] = 0x00;
    inbuff[1] = 0x00;
    inbuff[2] = 0x00;
    inbuff[3] = 0x01;


    while( (M4OSA_Int32)frame_size < inbuf_size )
    {
        mask_bits = 0xFFFFFFFF;
        p_bs->Buffer = (M4OSA_UInt8 *)(inbuff + frame_size);


        nalu_info = (unsigned char)p_bs->Buffer[4];

        outbuff[outbuffpos] = p_bs->Buffer[0];
        outbuff[outbuffpos + 1] = p_bs->Buffer[1];
        outbuff[outbuffpos + 2] = p_bs->Buffer[2];
        outbuff[outbuffpos + 3] = p_bs->Buffer[3];
        outbuff[outbuffpos + 4] = p_bs->Buffer[4];

        p_bs->Buffer = p_bs->Buffer + 5;

        p_bs->bitPos = 0;
        p_bs->lastTotalBits = 0;
        p_bs->numBitsInBuffer = ( inbuf_size - frame_size - 5) << 3;
        p_bs->readableBytesInBuffer = inbuf_size - frame_size - 5;

        p_bs->ui32TempBuff = 0;
        p_bs->i8BitCnt = 0;
        p_bs->pui8BfrPtr = (M4OSA_Int8 *)p_bs->Buffer;
        p_bs->ui32LastTwoBytes = 0xFFFFFFFF;

        H264MCS_getBits(p_bs, 0);



        nal_size = inbuf_size - frame_size - 4;
        buff = inbuff + frame_size + 4;

        while( nal_size > 4 )
        {
            if( ( buff[0] == 0x00) && (buff[1] == 0x00) && (buff[2] == 0x00)
                && (buff[3] == 0x01) )
            {
                break;
            }
            buff = buff + 1;
            nal_size = nal_size - 1;
        }

        if( nal_size <= 4 )
        {
            nal_size = 0;
        }
        nal_size = ( inbuf_size - frame_size - 4) - nal_size;

        //      M4OSA_TRACE1_3("H264MCS_ProcessNALU frame  input buff size = %d  current position
        //= %d   nal size = %d",
        //  inbuf_size, frame_size,  nal_size + 4);
        frame_size += nal_size + 4;



        forbidden_bit = ( nalu_info >> 7) & 1;
        nal_ref_idc = ( nalu_info >> 5) & 3;
        nal_unit_type = (nalu_info) &0x1f;

        if( nal_unit_type == 5 )
        {
            /*IDR/PPS Packet - Do nothing*/
            instance->is_done = 1;
            return err;
        }

        NSWAVCMCS_initBitstream(&instance->encbs);
        instance->encbs.streamBuffer = outbuff + outbuffpos + 5;

        if( nal_unit_type == 8 )
        {
            M4OSA_TRACE1_0("H264MCS_ProcessNALU() Error: PPS");
            return err;
        }

        if( nal_unit_type == 7 )
        {
            /*SPS Packet */
            M4OSA_TRACE1_0("H264MCS_ProcessNALU() Error: SPS");
            return 0;
        }

        if( (nal_unit_type == 5) )
        {
            instance->frame_count = 0;
            instance->POC_lsb = 0;
        }

        if( (nal_unit_type == 1) )
        {
            first_mb_in_slice = H264MCS_DecVLCReadExpGolombCode(p_bs);
            NSWAVCMCS_uExpVLC(&instance->encbs, first_mb_in_slice);

            slice_type = H264MCS_DecVLCReadExpGolombCode(p_bs);
            NSWAVCMCS_uExpVLC(&instance->encbs, slice_type);

            pic_parameter_set_id = H264MCS_DecVLCReadExpGolombCode(p_bs);
            NSWAVCMCS_uExpVLC(&instance->encbs, pic_parameter_set_id);

            temp = H264MCS_getBits(p_bs,
                instance->clip_sps.log2_max_frame_num_minus4 + 4);
            NSWAVCMCS_putBits(&instance->encbs, instance->frame_count,
                instance->clip_sps.log2_max_frame_num_minus4 + 4);

            // In Baseline Profile: frame_mbs_only_flag should be ON

            if( nal_unit_type == 5 )
            {
                temp = H264MCS_DecVLCReadExpGolombCode(p_bs);
                NSWAVCMCS_uExpVLC(&instance->encbs, temp);
            }

            if( instance->clip_sps.pic_order_cnt_type == 0 )
            {
                temp = H264MCS_getBits(p_bs,
                    instance->clip_sps.log2_max_pic_order_cnt_lsb_minus4
                    + 4);
                NSWAVCMCS_putBits(&instance->encbs, instance->POC_lsb,
                    instance->clip_sps.log2_max_pic_order_cnt_lsb_minus4 + 4);
            }

            if( ( instance->clip_sps.pic_order_cnt_type == 1)
                && (instance->clip_sps.delta_pic_order_always_zero_flag) )
            {
                temp = H264MCS_DecVLCReadSignedExpGolombCode(p_bs);
                NSWAVCMCS_sExpVLC(&instance->encbs, temp);
            }

            cnt = p_bs->bitPos & 0x7;

            if( cnt )
            {
                cnt = 8 - cnt;
                temp = H264MCS_getBits(p_bs, cnt);
                NSWAVCMCS_putBits(&instance->encbs, temp, cnt);
            }

            cnt = p_bs->bitPos >> 3;

            while( cnt < (nal_size - 2) )
            {
                temp = H264MCS_getBits(p_bs, 8);
                NSWAVCMCS_putBits(&instance->encbs, temp, 8);
                cnt = p_bs->bitPos >> 3;
            }

            temp = H264MCS_getBits(p_bs, 8);

            if( temp != 0 )
            {
                cnt = 0;

                while( ( temp & 0x1) == 0 )
                {
                    cnt++;
                    temp = temp >> 1;
                }
                cnt++;
                temp = temp >> 1;

                if( 8 - cnt )
                {
                    NSWAVCMCS_putBits(&instance->encbs, temp, (8 - cnt));
                }

                NSWAVCMCS_putRbspTbits(&instance->encbs);
            }
            else
            {
                if( instance->encbs.bitPos % 8 )
                {
                    NSWAVCMCS_putBits(&instance->encbs, 0,
                        (8 - instance->encbs.bitPos % 8));
                }
            }

            temp = instance->encbs.byteCnt;
            temp = temp + 1;

            outbuff[outbuffpos] = (M4OSA_UInt8)(( temp >> 24) & 0xFF);
            outbuff[outbuffpos + 1] = (M4OSA_UInt8)(( temp >> 16) & 0xFF);
            outbuff[outbuffpos + 2] = (M4OSA_UInt8)(( temp >> 8) & 0xFF);
            outbuff[outbuffpos + 3] = (M4OSA_UInt8)((temp) &0xFF);
            outbuffpos = outbuffpos + temp + 4;
        }
        else
        {
            p_bs->Buffer = p_bs->Buffer - 5;
            memcpy((void *) &outbuff[outbuffpos],
                (void *)p_bs->Buffer, nal_size + 4);

            outbuff[outbuffpos] = (M4OSA_UInt8)((nal_size >> 24)& 0xFF);
        outbuff[outbuffpos + 1] = (M4OSA_UInt8)((nal_size >> 16)& 0xFF);;
        outbuff[outbuffpos + 2] = (M4OSA_UInt8)((nal_size >> 8)& 0xFF);;
        outbuff[outbuffpos + 3] = (M4OSA_UInt8)((nal_size)& 0xFF);;

            outbuffpos = outbuffpos + nal_size + 4;
        }
    }

    *outbuf_size = outbuffpos;

    instance->POC_lsb = instance->POC_lsb + 1;

    if( instance->POC_lsb == instance->POC_lsb_mod )
    {
        instance->POC_lsb = 0;
    }
    instance->frame_count = instance->frame_count + 1;

    if( instance->frame_count == instance->frame_mod_count )
    {
        instance->frame_count = 0;
    }
    return M4NO_ERROR;
}

M4OSA_ERR   M4MCS_convetFromByteStreamtoNALStream(  M4OSA_UInt8 *inbuff,
                                                    M4OSA_UInt32 inbuf_size )
{
    M4OSA_ERR err = M4NO_ERROR;
    M4OSA_UInt32 framesize = 0;
    M4OSA_UInt32 nal_size =0;
    M4OSA_UInt8 *buff;


    while(framesize < inbuf_size)
    {
            nal_size = inbuf_size - framesize - 4;
            buff =  inbuff + framesize + 4;

            while(nal_size > 4){
                if((buff[0] == 0x00) &&
                (buff[1] == 0x00) &&
                (buff[2] == 0x00) &&
                (buff[3] == 0x01)){
                    break;
                }
                buff = buff + 1;
                nal_size = nal_size -1;
            }

            if(nal_size <= 4){
                nal_size = 0;
            }
            nal_size = (inbuf_size - framesize - 4) - nal_size;

        inbuff[framesize + 0]  = (M4OSA_UInt8)((nal_size >> 24)& 0xFF);
        inbuff[framesize + 1]  = (M4OSA_UInt8)((nal_size >> 16)& 0xFF);
        inbuff[framesize + 2]  = (M4OSA_UInt8)((nal_size >> 8)& 0xFF);
        inbuff[framesize + 3]  = (M4OSA_UInt8)((nal_size )& 0xFF);
        framesize += nal_size + 4;

        M4OSA_TRACE1_2("M4MCS_convetFromByteStreamtoNALStream framesize = %x nalsize = %x",
            framesize, nal_size)
    }

    return  err;
}


M4OSA_ERR H264MCS_Freeinstance( NSWAVC_MCS_t *instance )
{
    M4OSA_ERR err = M4NO_ERROR;
    M4OSA_DEBUG_IF2((M4OSA_NULL == instance), M4ERR_PARAMETER,
        "H264MCS_Freeinstance: instance is M4OSA_NULL");

    if( M4OSA_NULL != instance->encoder_pps.slice_group_id )
    {
        free(instance->encoder_pps.slice_group_id);
    }

    if( M4OSA_NULL != instance->p_encoder_sps )
    {
        free(instance->p_encoder_sps);
        instance->p_encoder_sps = M4OSA_NULL;
    }

    if( M4OSA_NULL != instance->p_encoder_pps )
    {
        free(instance->p_encoder_pps);
        instance->p_encoder_pps = M4OSA_NULL;
    }

    if( M4OSA_NULL != instance->m_pFinalDSI )
    {
        free(instance->m_pFinalDSI);
        instance->m_pFinalDSI = M4OSA_NULL;
    }

    if( M4OSA_NULL != instance )
    {
        free(instance);
        instance = M4OSA_NULL;
    }

    return err;
}
/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_getVersion(M4_VersionInfo* pVersionInfo);
 * @brief    Get the MCS version.
 * @note Can be called anytime. Do not need any context.
 * @param    pVersionInfo        (OUT) Pointer to a version info structure
 * @return   M4NO_ERROR:         No error
 * @return   M4ERR_PARAMETER:    pVersionInfo is M4OSA_NULL (If Debug Level >= 2)
 ******************************************************************************
 */
M4OSA_ERR M4MCS_getVersion( M4_VersionInfo *pVersionInfo )
{
    M4OSA_TRACE3_1("M4MCS_getVersion called with pVersionInfo=0x%x",
        pVersionInfo);

    /**
    * Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pVersionInfo), M4ERR_PARAMETER,
        "M4MCS_getVersion: pVersionInfo is M4OSA_NULL");

    pVersionInfo->m_major = M4MCS_VERSION_MAJOR;
    pVersionInfo->m_minor = M4MCS_VERSION_MINOR;
    pVersionInfo->m_revision = M4MCS_VERSION_REVISION;

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4MCS_getVersion(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * @brief    Initializes the MCS (allocates an execution context).
 * @note
 * @param    pContext            (OUT) Pointer on the MCS context to allocate
 * @param    pFileReadPtrFct     (IN) Pointer to OSAL file reader functions
 * @param    pFileWritePtrFct    (IN) Pointer to OSAL file writer functions
 * @return   M4NO_ERROR:         No error
 * @return   M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (If Debug Level >= 2)
 * @return   M4ERR_ALLOC:        There is no more available memory
 ******************************************************************************
 */

M4OSA_ERR M4MCS_init( M4MCS_Context *pContext,
                     M4OSA_FileReadPointer *pFileReadPtrFct,
                     M4OSA_FileWriterPointer *pFileWritePtrFct )
{
    M4MCS_InternalContext *pC = M4OSA_NULL;
    M4OSA_ERR err;

    M4OSA_TRACE3_3(
        "M4MCS_init called with pContext=0x%x, pFileReadPtrFct=0x%x, pFileWritePtrFct=0x%x",
        pContext, pFileReadPtrFct, pFileWritePtrFct);

    /**
    * Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4MCS_init: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pFileReadPtrFct), M4ERR_PARAMETER,
        "M4MCS_init: pFileReadPtrFct is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pFileWritePtrFct), M4ERR_PARAMETER,
        "M4MCS_init: pFileWritePtrFct is M4OSA_NULL");

    /**
    * Allocate the MCS context and return it to the user */
    pC = (M4MCS_InternalContext *)M4OSA_32bitAlignedMalloc(sizeof(M4MCS_InternalContext),
        M4MCS, (M4OSA_Char *)"M4MCS_InternalContext");
    *pContext = pC;

    if( M4OSA_NULL == pC )
    {
        M4OSA_TRACE1_0(
            "M4MCS_init(): unable to allocate M4MCS_InternalContext, returning M4ERR_ALLOC");
        return M4ERR_ALLOC;
    }

    /**
    * Init the context. All pointers must be initialized to M4OSA_NULL
    * because CleanUp() can be called just after Init(). */
    pC->State = M4MCS_kState_CREATED;
    pC->pOsaFileReadPtr = pFileReadPtrFct;
    pC->pOsaFileWritPtr = pFileWritePtrFct;
    pC->VideoState = M4MCS_kStreamState_NOSTREAM;
    pC->AudioState = M4MCS_kStreamState_NOSTREAM;
    pC->noaudio = M4OSA_FALSE;
    pC->novideo = M4OSA_FALSE;
    pC->uiProgress = 0;

    /**
    * Reader stuff */
    pC->pInputFile = M4OSA_NULL;
    pC->InputFileType = M4VIDEOEDITING_kFileType_Unsupported;
    pC->bFileOpenedInFastMode = M4OSA_FALSE;
    pC->pReaderContext = M4OSA_NULL;
    pC->pReaderVideoStream = M4OSA_NULL;
    pC->pReaderAudioStream = M4OSA_NULL;
    pC->bUnsupportedVideoFound = M4OSA_FALSE;
    pC->bUnsupportedAudioFound = M4OSA_FALSE;
    pC->iAudioCtsOffset = 0;
    /* First temporary video AU to have more precise end video cut*/
    pC->ReaderVideoAU1.m_structSize = 0;
    /* Second temporary video AU to have more precise end video cut*/
    pC->ReaderVideoAU2.m_structSize = 0;
    pC->ReaderAudioAU1.m_structSize = 0;
    pC->ReaderAudioAU2.m_structSize = 0;
    pC->m_audioAUDuration = 0;
    pC->m_pDataAddress1 = M4OSA_NULL;
    pC->m_pDataAddress2 = M4OSA_NULL;
    /* First temporary video AU data to have more precise end video cut*/
    pC->m_pDataVideoAddress1 = M4OSA_NULL;
    /* Second temporary video AU data to have more precise end video cut*/
    pC->m_pDataVideoAddress2 = M4OSA_NULL;

    /**
    * Video decoder stuff */
    pC->pViDecCtxt = M4OSA_NULL;
    pC->dViDecStartingCts = 0.0;
    pC->iVideoBeginDecIncr = 0;
    pC->dViDecCurrentCts = 0.0;
    pC->dCtsIncrement = 0.0;
    pC->isRenderDup = M4OSA_FALSE;

    /**
    * Video encoder stuff */
    pC->pViEncCtxt = M4OSA_NULL;
    pC->pPreResizeFrame = M4OSA_NULL;
    pC->uiEncVideoBitrate = 0;
    pC->encoderState = M4MCS_kNoEncoder;

    /**
    * Audio decoder stuff */
    pC->pAudioDecCtxt = M4OSA_NULL;
    pC->AudioDecBufferIn.m_dataAddress = M4OSA_NULL;
    pC->AudioDecBufferIn.m_bufferSize = 0;
    pC->AudioDecBufferOut.m_dataAddress = M4OSA_NULL;
    pC->AudioDecBufferOut.m_bufferSize = 0;
    pC->pPosInDecBufferOut = M4OSA_NULL;
    /**
    * Ssrc stuff */
    pC->pSsrcBufferIn = M4OSA_NULL;
    pC->pSsrcBufferOut = M4OSA_NULL;
    pC->pPosInSsrcBufferIn = M4OSA_NULL;
    pC->pPosInSsrcBufferOut = M4OSA_NULL;
    pC->iSsrcNbSamplIn = 0;
    pC->iSsrcNbSamplOut = 0;
    pC->SsrcScratch = M4OSA_NULL;
    pC->pLVAudioResampler = M4OSA_NULL;
    /**
    * Audio encoder */
    pC->pAudioEncCtxt = M4OSA_NULL;
    pC->pAudioEncDSI.infoSize = 0;
    pC->pAudioEncDSI.pInfo = M4OSA_NULL;
    pC->pAudioEncoderBuffer = M4OSA_NULL;
    pC->pPosInAudioEncoderBuffer = M4OSA_NULL;
    pC->audioEncoderGranularity = 0;

    /**
    * Writer stuff */
    pC->pOutputFile = M4OSA_NULL;
    pC->pTemporaryFile = M4OSA_NULL;
    pC->pWriterContext = M4OSA_NULL;
    pC->uiVideoAUCount = 0;
    pC->uiVideoMaxAuSize = 0;
    pC->uiVideoMaxChunckSize = 0;
    pC->uiAudioAUCount = 0;
    pC->uiAudioMaxAuSize = 0;

    pC->uiAudioCts = 0;
    pC->b_isRawWriter = M4OSA_FALSE;
    pC->pOutputPCMfile = M4OSA_NULL;

    /* Encoding config */
    pC->EncodingVideoFormat = M4ENCODER_kNULL; /**< No format set yet */
    pC->EncodingWidth = 0;                     /**< No size set yet */
    pC->EncodingHeight = 0;                    /**< No size set yet */
    pC->EncodingVideoFramerate = 0;            /**< No framerate set yet */

    pC->uiBeginCutTime = 0;                    /**< No begin cut */
    pC->uiEndCutTime = 0;                      /**< No end cut */
    pC->uiMaxFileSize = 0;                     /**< No limit */
    pC->uiAudioBitrate =
        M4VIDEOEDITING_kUndefinedBitrate; /**< No bitrate set yet */
    pC->uiVideoBitrate =
        M4VIDEOEDITING_kUndefinedBitrate; /**< No bitrate set yet */

    pC->WriterVideoStream.streamType = M4SYS_kVideoUnknown;
    pC->WriterVideoStreamInfo.Header.pBuf = M4OSA_NULL;
    pC->WriterAudioStream.streamType = M4SYS_kAudioUnknown;

    pC->outputVideoTimescale = 0;

    /*FB 2008/10/20: add media rendering parameter and AIR context to keep media aspect ratio*/
    pC->MediaRendering = M4MCS_kResizing;
    pC->m_air_context = M4OSA_NULL;
    /**/

    /**
    * FlB 2009.03.04: add audio Effects*/
    pC->pEffects = M4OSA_NULL;
    pC->nbEffects = 0;
    pC->pActiveEffectNumber = -1;
    /**/

    /*
    * Reset pointers for media and codecs interfaces */
    err = M4MCS_clearInterfaceTables(pC);
    M4ERR_CHECK_RETURN(err);

    /*
    *  Call the media and codecs subscription module */
    err = M4MCS_subscribeMediaAndCodec(pC);
    M4ERR_CHECK_RETURN(err);

#ifdef M4MCS_SUPPORT_STILL_PICTURE
    /**
    * Initialize the Still picture part of MCS*/

    err = M4MCS_stillPicInit(pC, pFileReadPtrFct, pFileWritePtrFct);
    M4ERR_CHECK_RETURN(err);

    pC->m_bIsStillPicture = M4OSA_FALSE;

#endif /*M4MCS_SUPPORT_STILL_PICTURE*/

    pC->m_pInstance = M4OSA_NULL;
    pC->H264MCSTempBuffer = M4OSA_NULL;
    pC->H264MCSTempBufferSize = 0;
    pC->H264MCSTempBufferDataSize = 0;
    pC->bH264Trim = M4OSA_FALSE;

    /* Flag to get the last decoded frame cts */
    pC->bLastDecodedFrameCTS = M4OSA_FALSE;

    if( pC->m_pInstance == M4OSA_NULL )
    {
        err = H264MCS_Getinstance(&pC->m_pInstance);
    }
    pC->bExtOMXAudDecoder = M4OSA_FALSE;

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4MCS_init(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_open(M4MCS_Context pContext, M4OSA_Void* pFileIn,
 *                         M4OSA_Void* pFileOut, M4OSA_Void* pTempFile);
 * @brief   Set the MCS input and output files.
 * @note    It opens the input file, but the output file is not created yet.
 * @param   pContext            (IN) MCS context
 * @param   pFileIn             (IN) Input file to transcode (The type of this parameter
 *                                 (URL, pipe...) depends on the OSAL implementation).
 * @param   mediaType           (IN) Container type (.3gp,.amr,mp3 ...) of input file.
 * @param   pFileOut            (IN) Output file to create  (The type of this parameter
 *                                    (URL, pipe...) depends on the OSAL implementation).
 * @param   pTempFile           (IN) Temporary file for the constant memory writer to
 *                                     store metadata ("moov.bin").
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 * @return  M4ERR_ALLOC:        There is no more available memory
 * @return  M4ERR_FILE_NOT_FOUND:   The input file has not been found
 * @return  M4MCS_ERR_INVALID_INPUT_FILE:   The input file is not a valid file, or is corrupted
 * @return  M4MCS_ERR_INPUT_FILE_CONTAINS_NO_SUPPORTED_STREAM:  The input file contains no
 *                                supported audio or video stream
 ******************************************************************************
 */
M4OSA_ERR M4MCS_open( M4MCS_Context pContext, M4OSA_Void *pFileIn,
                     M4VIDEOEDITING_FileType InputFileType, M4OSA_Void *pFileOut,
                     M4OSA_Void *pTempFile )
{
    M4MCS_InternalContext *pC = (M4MCS_InternalContext *)(pContext);
    M4OSA_ERR err;

    M4READER_MediaFamily mediaFamily;
    M4_StreamHandler *pStreamHandler;

    M4OSA_TRACE2_3(
        "M4MCS_open called with pContext=0x%x, pFileIn=0x%x, pFileOut=0x%x",
        pContext, pFileIn, pFileOut);

    /**
    * Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4MCS_open: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pFileIn), M4ERR_PARAMETER,
        "M4MCS_open: pFileIn is M4OSA_NULL");

    if( ( InputFileType == M4VIDEOEDITING_kFileType_JPG)
        || (InputFileType == M4VIDEOEDITING_kFileType_PNG)
        || (InputFileType == M4VIDEOEDITING_kFileType_GIF)
        || (InputFileType == M4VIDEOEDITING_kFileType_BMP) )
    {
#ifdef M4MCS_SUPPORT_STILL_PICTURE
        /**
        * Indicate that we must use the still picture functions*/

        pC->m_bIsStillPicture = M4OSA_TRUE;

        /**
        * Call the still picture MCS functions*/
        return M4MCS_stillPicOpen(pC, pFileIn, InputFileType, pFileOut);

#else

        M4OSA_TRACE1_0(
            "M4MCS_open: Still picture is not supported with this version of MCS");
        return M4MCS_ERR_INPUT_FILE_CONTAINS_NO_SUPPORTED_STREAM;

#endif /*M4MCS_SUPPORT_STILL_PICTURE*/

    }

    /**
    * Check state automaton */
    if( M4MCS_kState_CREATED != pC->State )
    {
        M4OSA_TRACE1_1("M4MCS_open(): Wrong State (%d), returning M4ERR_STATE",
            pC->State);
        return M4ERR_STATE;
    }

    /* Copy function input parameters into our context */
    pC->pInputFile = pFileIn;
    pC->InputFileType = InputFileType;
    pC->pOutputFile = pFileOut;
    pC->pTemporaryFile = pTempFile;
    pC->uiProgress = 0;

    /***********************************/
    /* Open input file with the reader */
    /***********************************/

    err = M4MCS_setCurrentReader(pContext, pC->InputFileType);
    M4ERR_CHECK_RETURN(err);

    /**
    * Reset reader related variables */
    pC->VideoState = M4MCS_kStreamState_NOSTREAM;
    pC->AudioState = M4MCS_kStreamState_NOSTREAM;
    pC->pReaderVideoStream = M4OSA_NULL;
    pC->pReaderAudioStream = M4OSA_NULL;

    /*******************************************************/
    /* Initializes the reader shell and open the data file */
    /*******************************************************/
    err = pC->m_pReader->m_pFctCreate(&pC->pReaderContext);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1("M4MCS_open(): m_pReader->m_pFctCreate returns 0x%x",
            err);
        return err;
    }

    /**
    * Link the reader interface to the reader context */
    pC->m_pReaderDataIt->m_readerContext = pC->pReaderContext;

    /**
    * Set the reader shell file access functions */
    err = pC->m_pReader->m_pFctSetOption(pC->pReaderContext,
        M4READER_kOptionID_SetOsaFileReaderFctsPtr,
        (M4OSA_DataOption)pC->pOsaFileReadPtr);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1("M4MCS_open(): m_pReader->m_pFctSetOption returns 0x%x",
            err);
        return err;
    }

#ifdef M4MCS_WITH_FAST_OPEN

    if( M4OSA_FALSE == pC->bFileOpenedInFastMode )
    {
        M4OSA_Bool trueValue = M4OSA_TRUE;

        /* For first call use fast open mode */
        err = pC->m_pReader->m_pFctSetOption(pC->pReaderContext,
            M4READER_3GP_kOptionID_FastOpenMode, &trueValue);

        if( M4NO_ERROR == err )
        {
            pC->bFileOpenedInFastMode = M4OSA_TRUE;
        }
        else
        {
            M4OSA_TRACE1_1(
                "M4MCS_open(): M4READER_3GP_kOptionID_FastOpenMode returns 0x%x",
                err);

            if( ( ( (M4OSA_UInt32)M4ERR_BAD_OPTION_ID) == err)
                || (( (M4OSA_UInt32)M4ERR_PARAMETER) == err) )
            {
                /* Not fatal, some readers may not support fast open mode */
                pC->bFileOpenedInFastMode = M4OSA_FALSE;
            }
            else
                return err;
        }
    }
    else
    {
        M4OSA_Bool falseValue = M4OSA_FALSE;

        /* For second call use normal open mode */
        err = pC->m_pReader->m_pFctSetOption(pC->pReaderContext,
            M4READER_3GP_kOptionID_FastOpenMode, &falseValue);
    }

#endif /* M4MCS_WITH_FAST_OPEN */

    /**
    * Open the input file */

    err = pC->m_pReader->m_pFctOpen(pC->pReaderContext, pC->pInputFile);

    if( M4NO_ERROR != err )
    {
        M4OSA_UInt32 uiDummy, uiCoreId;
        M4OSA_TRACE1_1("M4MCS_open(): m_pReader->m_pFctOpen returns 0x%x", err);

        /**
        * If the error is from the core reader, we change it to a public VXS error */
        M4OSA_ERR_SPLIT(err, uiDummy, uiCoreId, uiDummy);

        if( M4MP4_READER == uiCoreId )
        {
            M4OSA_TRACE1_0(
                "M4MCS_open(): returning M4MCS_ERR_INVALID_INPUT_FILE");
            return M4MCS_ERR_INVALID_INPUT_FILE;
        }
        return err;
    }

    /**
    * Get the streams from the input file */
    while( M4NO_ERROR == err )
    {
        err =
            pC->m_pReader->m_pFctGetNextStream( pC->pReaderContext,
                                                &mediaFamily,
                                                &pStreamHandler);

        /**
        * In case we found a BIFS stream or something else...*/
        if( ( err == ((M4OSA_UInt32)M4ERR_READER_UNKNOWN_STREAM_TYPE))
            || (err == ((M4OSA_UInt32)M4WAR_TOO_MUCH_STREAMS)) )
        {
            err = M4NO_ERROR;
            continue;
        }

        if( M4NO_ERROR == err ) /**< One stream found */
        {
            /**
            * Found the first video stream */
            if( ( M4READER_kMediaFamilyVideo == mediaFamily)
                && (M4OSA_NULL == pC->pReaderVideoStream) )
            {
                if( ( M4DA_StreamTypeVideoH263 == pStreamHandler->m_streamType)
                    || (M4DA_StreamTypeVideoMpeg4
                    == pStreamHandler->m_streamType)
                    || (M4DA_StreamTypeVideoMpeg4Avc
                    == pStreamHandler->m_streamType) )
                {
                    M4OSA_TRACE3_0(
                        "M4MCS_open(): Found a H263 or MPEG-4 video stream in input 3gpp clip");

                    /**
                    * Keep pointer to the video stream */
                    pC->pReaderVideoStream =
                        (M4_VideoStreamHandler *)pStreamHandler;
                    pC->bUnsupportedVideoFound = M4OSA_FALSE;
                    pStreamHandler->m_bStreamIsOK = M4OSA_TRUE;

                    /**
                    * Init our video stream state variable */
                    pC->VideoState = M4MCS_kStreamState_STARTED;

                    /**
                    * Reset the stream reader */
                    err = pC->m_pReader->m_pFctReset(pC->pReaderContext,
                        (M4_StreamHandler *)pC->pReaderVideoStream);

                    if( M4NO_ERROR != err )
                    {
                        M4OSA_TRACE1_1(
                            "M4MCS_open():\
                            m_pReader->m_pFctReset(video) returns 0x%x",
                            err);
                        return err;
                    }

                    /**
                    * Initializes an access Unit */
                    err = pC->m_pReader->m_pFctFillAuStruct(pC->pReaderContext,
                        pStreamHandler, &pC->ReaderVideoAU);

                    if( M4NO_ERROR != err )
                    {
                        M4OSA_TRACE1_1(
                            "M4MCS_open():\
                            m_pReader->m_pFctFillAuStruct(video) returns 0x%x",
                            err);
                        return err;
                    }
                }
                else /**< Not H263 or MPEG-4 (H264, etc.) */
                {
                    M4OSA_TRACE1_1("M4MCS_open(): Found an unsupported video stream (0x%x) in\
                                   input 3gpp clip",
                                   pStreamHandler->m_streamType);

                    pC->bUnsupportedVideoFound = M4OSA_TRUE;
                    pStreamHandler->m_bStreamIsOK = M4OSA_FALSE;
                }
                /* +CRLV6775 -H.264 Trimming */
                if( M4DA_StreamTypeVideoMpeg4Avc
                    == pStreamHandler->m_streamType )
                {

                    // SPS and PPS are storead as per the 3gp file format
                    pC->m_pInstance->m_pDecoderSpecificInfo =
                        pStreamHandler->m_pH264DecoderSpecificInfo;
                    pC->m_pInstance->m_decoderSpecificInfoSize =
                        pStreamHandler->m_H264decoderSpecificInfoSize;
                }
                /* -CRLV6775 -H.264 Trimming */
            }
            /**
            * Found the first audio stream */
            else if( ( M4READER_kMediaFamilyAudio == mediaFamily)
                && (M4OSA_NULL == pC->pReaderAudioStream) )
            {
                if( ( M4DA_StreamTypeAudioAmrNarrowBand
                    == pStreamHandler->m_streamType)
                    || (M4DA_StreamTypeAudioAac == pStreamHandler->m_streamType)
                    || (M4DA_StreamTypeAudioMp3
                    == pStreamHandler->m_streamType)
                    || (M4DA_StreamTypeAudioEvrc
                    == pStreamHandler->m_streamType) )
                {
                    M4OSA_TRACE3_0(
                        "M4MCS_open(): Found an AMR-NB, AAC or MP3 audio stream in input clip");

                    /**
                    * Keep pointer to the audio stream */
                    pC->pReaderAudioStream =
                        (M4_AudioStreamHandler *)pStreamHandler;
                    pStreamHandler->m_bStreamIsOK = M4OSA_TRUE;
                    pC->bUnsupportedAudioFound = M4OSA_FALSE;

                    /**
                    * Init our audio stream state variable */
                    pC->AudioState = M4MCS_kStreamState_STARTED;

                    /**
                    * Reset the stream reader */
                    err = pC->m_pReader->m_pFctReset(pC->pReaderContext,
                        (M4_StreamHandler *)pC->pReaderAudioStream);

                    if( M4NO_ERROR != err )
                    {
                        M4OSA_TRACE1_1(
                            "M4MCS_open():\
                            m_pReader->m_pFctReset(audio) returns 0x%x",
                            err);
                        return err;
                    }

                    /**
                    * Initializes an access Unit */
                    err = pC->m_pReader->m_pFctFillAuStruct(pC->pReaderContext,
                        pStreamHandler, &pC->ReaderAudioAU);

                    if( M4NO_ERROR != err )
                    {
                        M4OSA_TRACE1_1(
                            "M4MCS_open():\
                            m_pReader->m_pFctFillAuStruct(audio) returns 0x%x",
                            err);
                        return err;
                    }

                    /**
                    * Output max AU size is equal to input max AU size (this value
                    * will be changed if there is audio transcoding) */
                    pC->uiAudioMaxAuSize = pStreamHandler->m_maxAUSize;
                }
                else
                {
                    /**< Not AMR-NB, AAC, MP3 nor EVRC (AMR-WB, WAV...) */
                    M4OSA_TRACE1_1("M4MCS_open(): Found an unsupported audio stream (0x%x) in \
                                   input 3gpp clip", pStreamHandler->m_streamType);

                    pC->bUnsupportedAudioFound = M4OSA_TRUE;
                    pStreamHandler->m_bStreamIsOK = M4OSA_FALSE;
                }
            }
        }
    } /**< end of while (M4NO_ERROR == err) */

    /**
    * Check we found at least one supported stream */
    if( ( M4OSA_NULL == pC->pReaderVideoStream)
        && (M4OSA_NULL == pC->pReaderAudioStream) )
    {
        M4OSA_TRACE1_0(
            "M4MCS_open(): returning M4MCS_ERR_INPUT_FILE_CONTAINS_NO_SUPPORTED_STREAM");
        return M4MCS_ERR_INPUT_FILE_CONTAINS_NO_SUPPORTED_STREAM;
    }

    if( pC->VideoState == M4MCS_kStreamState_STARTED )
    {
        err = M4MCS_setCurrentVideoDecoder(pContext,
            pC->pReaderVideoStream->m_basicProperties.m_streamType);
        /*FB 2009-02-09: the error is check and returned only if video codecs are compiled,
        else only audio is used, that is why the editing process can continue*/
#ifndef M4MCS_AUDIOONLY

        M4ERR_CHECK_RETURN(err);

#else

        if( ( M4NO_ERROR != err) && (M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED != err) )
        {
            M4ERR_CHECK_RETURN(err);
        }

#endif /*M4MCS_AUDIOONLY*/

    }

    if( pC->AudioState == M4MCS_kStreamState_STARTED )
    {
        //EVRC
        if( M4DA_StreamTypeAudioEvrc
            != pStreamHandler->
            m_streamType ) /* decoder not supported yet, but allow to do null encoding */
        {
            err = M4MCS_setCurrentAudioDecoder(pContext,
                pC->pReaderAudioStream->m_basicProperties.m_streamType);
            M4ERR_CHECK_RETURN(err);
        }
    }

    /**
    * Get the audio and video stream properties */
    err = M4MCS_intGetInputClipProperties(pC);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_open(): M4MCS_intGetInputClipProperties returns 0x%x", err);
        return err;
    }

    /**
    * Set the begin cut decoding increment according to the input frame rate */
    if( 0. != pC->InputFileProperties.fAverageFrameRate ) /**< sanity check */
    {
        pC->iVideoBeginDecIncr = (M4OSA_Int32)(3000.
            / pC->InputFileProperties.
            fAverageFrameRate); /**< about 3 frames */
    }
    else
    {
        pC->iVideoBeginDecIncr =
            200; /**< default value: 200 milliseconds (3 frames @ 15fps)*/
    }

    /**
    * Update state automaton */
    pC->State = M4MCS_kState_OPENED;

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4MCS_open(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_step(M4MCS_Context pContext, M4OSA_UInt8 *pProgress);
 * @brief   Perform one step of trancoding.
 * @note
 * @param   pContext            (IN) MCS context
 * @param   pProgress           (OUT) Progress percentage (0 to 100) of the transcoding
 * @note    pProgress must be a valid address.
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    One of the parameters is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 * @return  M4MCS_WAR_TRANSCODING_DONE: Transcoding is over, user should now call M4MCS_close()
 * @return  M4MCS_ERR_AUDIO_CONVERSION_FAILED: The audio conversion (AAC to AMR-NB or MP3) failed
 * @return  M4MCS_ERR_INVALID_AAC_SAMPLING_FREQUENCY: The input file contains an AAC audio track
 *                                 with an invalid sampling frequency (should never happen)
 ******************************************************************************
 */
M4OSA_ERR M4MCS_step( M4MCS_Context pContext, M4OSA_UInt8 *pProgress )
{
    M4MCS_InternalContext *pC = (M4MCS_InternalContext *)(pContext);

    M4OSA_TRACE3_1("M4MCS_step called with pContext=0x%x", pContext);

    /**
    * Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4MCS_step: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pProgress), M4ERR_PARAMETER,
        "M4MCS_step: pProgress is M4OSA_NULL");

#ifdef M4MCS_SUPPORT_STILL_PICTURE

    if( pC->m_bIsStillPicture )
    {
        /**
        * Call the still picture MCS functions*/
        return M4MCS_stillPicStep(pC, pProgress);
    }

#endif /*M4MCS_SUPPORT_STILL_PICTURE*/

    /**
    * Check state automaton */

    switch( pC->State )
    {
        case M4MCS_kState_READY:
            *pProgress = 0;
            return M4MCS_intStepSet(pC);
            break;

        case M4MCS_kState_BEGINVIDEOJUMP:
            *pProgress = pC->uiProgress;
            return M4MCS_intStepBeginVideoJump(pC);
            break;

        case M4MCS_kState_BEGINVIDEODECODE:
            *pProgress = pC->uiProgress;
            return M4MCS_intStepBeginVideoDecode(pC);
            break;

        case M4MCS_kState_PROCESSING:
            {
                M4OSA_ERR err = M4NO_ERROR;
                err = M4MCS_intStepEncoding(pC, pProgress);
                /* Save progress info in case of pause */
                pC->uiProgress = *pProgress;
                return err;
            }
            break;

        default: /**< State error */
            M4OSA_TRACE1_1(
                "M4MCS_step(): Wrong State (%d), returning M4ERR_STATE",
                pC->State);
            return M4ERR_STATE;
    }
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_pause(M4MCS_Context pContext);
 * @brief   Pause the transcoding i.e. release the (external hardware) video decoder.
 * @note    This function is not needed if no hardware accelerators are used.
 *          In that case, pausing the MCS is simply achieved by temporarily suspending
 *          the M4MCS_step function calls.
 * @param   pContext            (IN) MCS context
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    pContext is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 ******************************************************************************
 */
M4OSA_ERR M4MCS_pause( M4MCS_Context pContext )
{
    M4MCS_InternalContext *pC = (M4MCS_InternalContext *)(pContext);
    M4OSA_ERR err;

    M4OSA_TRACE2_1("M4MCS_pause called with pContext=0x%x", pContext);

    /**
    * Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4MCS_pause: pContext is M4OSA_NULL");

#ifdef M4MCS_SUPPORT_STILL_PICTURE

    if( pC->m_bIsStillPicture )
    {
        /**
        * Call the corresponding still picture MCS function*/
        return M4MCS_stillPicPause(pC);
    }

#endif /*M4MCS_SUPPORT_STILL_PICTURE*/

    /**
    * Check state automaton */

    switch( pC->State )
    {
        case M4MCS_kState_BEGINVIDEOJUMP: /**< the video decoder has been created,
                                            we must destroy it */
        case M4MCS_kState_BEGINVIDEODECODE: /**< the video is being used, we must destroy it */
        case M4MCS_kState_PROCESSING: /**< the video is being used, we must destroy it */
                    /**< OK, nothing to do here */
            break;

        default: /**< State error */
            M4OSA_TRACE1_1(
                "M4MCS_pause(): Wrong State (%d), returning M4ERR_STATE",
                pC->State);
            return M4ERR_STATE;
    }

    /**
    * Set the CTS at which we will resume the decoding */
    if( pC->dViDecCurrentCts > pC->dViDecStartingCts )
    {
        /**
        * We passed the starting CTS, so the resume target is the current CTS */
        pC->dViDecStartingCts = pC->dViDecCurrentCts;
    }
    else {
        /**
        * We haven't passed the starting CTS yet, so the resume target is still the starting CTS
        * --> nothing to do in the else block */
    }

    /**
    * Free video decoder stuff */
    if( M4OSA_NULL != pC->pViDecCtxt )
    {
        err = pC->m_pVideoDecoder->m_pFctDestroy(pC->pViDecCtxt);
        pC->pViDecCtxt = M4OSA_NULL;

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_pause: m_pVideoDecoder->pFctDestroy returns 0x%x", err);
            return err;
        }
    }

    /**
    * State transition */
    pC->State = M4MCS_kState_PAUSED;

    M4OSA_TRACE3_0("M4MCS_pause(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_resume(M4MCS_Context pContext);
 * @brief   Resume the transcoding after a pause (see M4MCS_pause).
 * @note    This function is not needed if no hardware accelerators are used.
 *          In that case, resuming the MCS is simply achieved by calling
 *          the M4MCS_step function.
 * @param   pContext            (IN) MCS context
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    pContext is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 ******************************************************************************
 */
M4OSA_ERR M4MCS_resume( M4MCS_Context pContext )
{
    M4MCS_InternalContext *pC = (M4MCS_InternalContext *)(pContext);
    M4OSA_ERR err;

    M4OSA_TRACE2_1("M4MCS_resume called with pContext=0x%x", pContext);

    /**
    * Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4MCS_resume: pContext is M4OSA_NULL");

#ifdef M4MCS_SUPPORT_STILL_PICTURE

    if( pC->m_bIsStillPicture )
    {
        /**
        * Call the corresponding still picture MCS function*/
        return M4MCS_stillPicResume(pC);
    }

#endif /*M4MCS_SUPPORT_STILL_PICTURE*/

    /**
    * Check state automaton */

    switch( pC->State )
    {
        case M4MCS_kState_PAUSED: /**< OK, nothing to do here */
            break;

        default:                  /**< State error */
            M4OSA_TRACE1_1(
                "M4MCS_resume(): Wrong State (%d), returning M4ERR_STATE",
                pC->State);
            return M4ERR_STATE;
            break;
    }

    /**
    * Prepare the video decoder */
    err = M4MCS_intPrepareVideoDecoder(pC);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_resume(): M4MCS_intPrepareVideoDecoder() returns 0x%x", err);
        return err;
    }

    /**
    * State transition */
    if( 0.0 == pC->dViDecStartingCts )
    {
        /**
        * We are still at the beginning of the decoded stream, no need to jump, we can proceed */
        pC->State = M4MCS_kState_PROCESSING;
    }
    else
    {
        /**
        * Jumping */
        pC->State = M4MCS_kState_BEGINVIDEOJUMP;
    }

    M4OSA_TRACE3_0("M4MCS_resume(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_close(M4MCS_Context pContext);
 * @brief    Finish the MCS transcoding.
 * @note The output 3GPP file is ready to be played after this call
 * @param    pContext            (IN) MCS context
 * @return   M4NO_ERROR:         No error
 * @return   M4ERR_PARAMETER:    pContext is M4OSA_NULL (If Debug Level >= 2)
 * @return   M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 ******************************************************************************
 */
M4OSA_ERR M4MCS_close( M4MCS_Context pContext )
{
    M4MCS_InternalContext *pC = (M4MCS_InternalContext *)(pContext);
    M4ENCODER_Header *encHeader;
    M4SYS_StreamIDmemAddr streamHeader;

    M4OSA_ERR err = M4NO_ERROR, err2;

    M4OSA_TRACE2_1("M4MCS_close called with pContext=0x%x", pContext);

    /**
    * Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4MCS_close: pContext is M4OSA_NULL");

#ifdef M4MCS_SUPPORT_STILL_PICTURE

    if( pC->m_bIsStillPicture )
    {
        /**
        * Indicate that current file is no longer a still picture*/
        pC->m_bIsStillPicture = M4OSA_FALSE;

        /**
        * Call the corresponding still picture MCS function*/
        return M4MCS_stillPicClose(pC);
    }

#endif /*M4MCS_SUPPORT_STILL_PICTURE*/

    /**
    * Check state automaton */

    if( M4MCS_kState_FINISHED != pC->State )
    {
        M4OSA_TRACE1_1("M4MCS_close(): Wrong State (%d), returning M4ERR_STATE",
            pC->State);
        return M4ERR_STATE;
    }

    /* Close the encoder before the writer to be certain all the AUs have been written and we can
    get the DSI. */

    /* Has the encoder actually been started? Don't stop it if that's not the case. */
    if( M4MCS_kEncoderRunning == pC->encoderState )
    {
        if( pC->pVideoEncoderGlobalFcts->pFctStop != M4OSA_NULL )
        {
            err = pC->pVideoEncoderGlobalFcts->pFctStop(pC->pViEncCtxt);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4MCS_close: pVideoEncoderGlobalFcts->pFctStop returns 0x%x",
                    err);
                /* Well... how the heck do you handle a failed cleanup? */
            }
        }

        pC->encoderState = M4MCS_kEncoderStopped;
    }

    /* Has the encoder actually been opened? Don't close it if that's not the case. */
    if( M4MCS_kEncoderStopped == pC->encoderState )
    {
        err = pC->pVideoEncoderGlobalFcts->pFctClose(pC->pViEncCtxt);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_close: pVideoEncoderGlobalFcts->pFctClose returns 0x%x",
                err);
            /* Well... how the heck do you handle a failed cleanup? */
        }

        pC->encoderState = M4MCS_kEncoderClosed;
    }

    /**********************************/
    /******** Close the writer ********/
    /**********************************/
    if( M4OSA_NULL != pC->pWriterContext ) /* happens in state _SET */
    {
        /* HW encoder: fetch the DSI from the shell video encoder, and feed it to the writer before
        closing it. */

        if( pC->novideo != M4OSA_TRUE )
        {
            if( ( M4ENCODER_kMPEG4 == pC->EncodingVideoFormat)
                || (M4ENCODER_kH264 == pC->EncodingVideoFormat) )
            {
                err = pC->pVideoEncoderGlobalFcts->pFctGetOption(pC->pViEncCtxt,
                    M4ENCODER_kOptionID_EncoderHeader,
                    (M4OSA_DataOption) &encHeader);

                if( ( M4NO_ERROR != err) || (M4OSA_NULL == encHeader->pBuf) )
                {
                    M4OSA_TRACE1_1(
                        "M4MCS_close: failed to get the encoder header (err 0x%x)",
                        err);
                    /**< no return here, we still have stuff to deallocate after close, even
                     if it fails. */
                }
                else
                {
                    /* set this header in the writer */
                    streamHeader.streamID = M4MCS_WRITER_VIDEO_STREAM_ID;
                    streamHeader.size = encHeader->Size;
                    streamHeader.addr = (M4OSA_MemAddr32)encHeader->pBuf;
                }

                M4OSA_TRACE1_0("calling set option");
                err = pC->pWriterGlobalFcts->pFctSetOption(pC->pWriterContext,
                    M4WRITER_kDSI, &streamHeader);
                M4OSA_TRACE1_0("set option done");

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4MCS_close: failed to set the DSI in the writer (err 0x%x)",
                        err);
                }
            }

            if( ( M4OSA_TRUE == pC->bH264Trim)
                && (M4ENCODER_kNULL == pC->EncodingVideoFormat) )
            {
                if(pC->uiBeginCutTime == 0)
                {
                    M4OSA_TRACE1_1("Decoder specific info size = %d",
                        pC->m_pInstance->m_decoderSpecificInfoSize);
                    pC->m_pInstance->m_pFinalDSISize =
                        pC->m_pInstance->m_decoderSpecificInfoSize;
                    M4OSA_TRACE1_1("Decoder specific info pointer = %d",
                        (M4OSA_MemAddr8)pC->m_pInstance->m_pDecoderSpecificInfo);

                    pC->m_pInstance->m_pFinalDSI =
                        (M4OSA_UInt8 *)M4OSA_32bitAlignedMalloc(pC->m_pInstance-> \
                        m_decoderSpecificInfoSize, M4MCS,
                        (M4OSA_Char *)"instance->m_pFinalDSI");

                    if( pC->m_pInstance->m_pFinalDSI == M4OSA_NULL )
                    {
                        M4OSA_TRACE1_0("instance->m_pFinalDSI: allocation error");
                        return M4ERR_ALLOC;
                    }
                    memcpy((void *)pC->m_pInstance->m_pFinalDSI,
                        (void *)pC-> \
                        m_pInstance->m_pDecoderSpecificInfo,
                        pC->m_pInstance->m_decoderSpecificInfoSize);
                }
                streamHeader.streamID = M4MCS_WRITER_VIDEO_STREAM_ID;
                streamHeader.size = pC->m_pInstance->m_pFinalDSISize;
                streamHeader.addr =
                    (M4OSA_MemAddr32)pC->m_pInstance->m_pFinalDSI;
                M4OSA_TRACE1_0("calling set option");
                err = pC->pWriterGlobalFcts->pFctSetOption(pC->pWriterContext,
                    M4WRITER_kDSI, &streamHeader);
                M4OSA_TRACE1_0("set option done");

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4MCS_close: failed to set the DSI in the writer (err 0x%x)",
                        err);
                }
            }
        }
        /* Write and close the 3GP output file */
        err2 = pC->pWriterGlobalFcts->pFctCloseWrite(pC->pWriterContext);
        pC->pWriterContext = M4OSA_NULL;

        if( M4NO_ERROR != err2 )
        {
            M4OSA_TRACE1_1(
                "M4MCS_close: pWriterGlobalFcts->pFctCloseWrite returns 0x%x",
                err2);

            if( M4NO_ERROR == err )
                err = err2;
            /**< no return here, we still have stuff to deallocate after close, even if it fails.*/
        }
    }

    /* Close output PCM file if needed */
    if( pC->pOutputPCMfile != M4OSA_NULL )
    {
        pC->pOsaFileWritPtr->closeWrite(pC->pOutputPCMfile);
        pC->pOutputPCMfile = M4OSA_NULL;
    }

    /*FlB 2009.03.04: add audio effects,
    free effects list*/
    if( M4OSA_NULL != pC->pEffects )
    {
        free(pC->pEffects);
        pC->pEffects = M4OSA_NULL;
    }
    pC->nbEffects = 0;
    pC->pActiveEffectNumber = -1;

    /**
    * State transition */
    pC->State = M4MCS_kState_CLOSED;

    if( M4OSA_NULL != pC->H264MCSTempBuffer )
    {
        free(pC->H264MCSTempBuffer);
    }

    M4OSA_TRACE3_0("M4MCS_close(): returning M4NO_ERROR");
    return err;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_cleanUp(M4MCS_Context pContext);
 * @brief    Free all resources used by the MCS.
 * @note The context is no more valid after this call
 * @param    pContext            (IN) MCS context
 * @return   M4NO_ERROR:         No error
 * @return   M4ERR_PARAMETER:    pContext is M4OSA_NULL (If Debug Level >= 2)
 * @return   M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 ******************************************************************************
 */
M4OSA_ERR M4MCS_cleanUp( M4MCS_Context pContext )
{
    M4OSA_ERR err = M4NO_ERROR;
    M4MCS_InternalContext *pC = (M4MCS_InternalContext *)(pContext);

    M4OSA_TRACE3_1("M4MCS_cleanUp called with pContext=0x%x", pContext);

#ifdef MCS_DUMP_PCM_TO_FILE

    if( file_au_reader )
    {
        fclose(file_au_reader);
        file_au_reader = NULL;
    }

    if( file_pcm_decoder )
    {
        fclose(file_pcm_decoder);
        file_pcm_decoder = NULL;
    }

    if( file_pcm_encoder )
    {
        fclose(file_pcm_encoder);
        file_pcm_encoder = NULL;
    }

#endif

    /**
    * Check input parameter */

    if( M4OSA_NULL == pContext )
    {
        M4OSA_TRACE1_0(
            "M4MCS_cleanUp: pContext is M4OSA_NULL, returning M4ERR_PARAMETER");
        return M4ERR_PARAMETER;
    }

    /**
    * Check state automaton */
    if( M4MCS_kState_CLOSED != pC->State )
    {
        M4OSA_TRACE1_1(
            "M4MCS_cleanUp(): Wrong State (%d), returning M4ERR_STATE",
            pC->State);
        return M4ERR_STATE;
    }

    if( M4OSA_NULL != pC->m_pInstance )
    {
        err = H264MCS_Freeinstance(pC->m_pInstance);
        pC->m_pInstance = M4OSA_NULL;
    }

    /* ----- Free video encoder stuff, if needed ----- */

    if( ( M4OSA_NULL != pC->pViEncCtxt)
        && (M4OSA_NULL != pC->pVideoEncoderGlobalFcts) )
    {
        err = pC->pVideoEncoderGlobalFcts->pFctCleanup(pC->pViEncCtxt);
        pC->pViEncCtxt = M4OSA_NULL;

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_cleanUp: pVideoEncoderGlobalFcts->pFctCleanup returns 0x%x",
                err);
            /**< don't return, we still have stuff to free */
        }

        pC->encoderState = M4MCS_kNoEncoder;
    }

    /**
    * In the H263 case, we allocated our own DSI buffer */
    if( ( M4ENCODER_kH263 == pC->EncodingVideoFormat)
        && (M4OSA_NULL != pC->WriterVideoStreamInfo.Header.pBuf) )
    {
        free(pC->WriterVideoStreamInfo.Header.pBuf);
        pC->WriterVideoStreamInfo.Header.pBuf = M4OSA_NULL;
    }

    if( M4OSA_NULL != pC->pPreResizeFrame )
    {
        if( M4OSA_NULL != pC->pPreResizeFrame[0].pac_data )
        {
            free(pC->pPreResizeFrame[0].pac_data);
            pC->pPreResizeFrame[0].pac_data = M4OSA_NULL;
        }

        if( M4OSA_NULL != pC->pPreResizeFrame[1].pac_data )
        {
            free(pC->pPreResizeFrame[1].pac_data);
            pC->pPreResizeFrame[1].pac_data = M4OSA_NULL;
        }

        if( M4OSA_NULL != pC->pPreResizeFrame[2].pac_data )
        {
            free(pC->pPreResizeFrame[2].pac_data);
            pC->pPreResizeFrame[2].pac_data = M4OSA_NULL;
        }
        free(pC->pPreResizeFrame);
        pC->pPreResizeFrame = M4OSA_NULL;
    }

    /* ----- Free the ssrc stuff ----- */

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

    if( M4OSA_NULL != pC->pSsrcBufferOut )
    {
        free(pC->pSsrcBufferOut);
        pC->pSsrcBufferOut = M4OSA_NULL;
    }

    if (pC->pLVAudioResampler != M4OSA_NULL)
    {
        LVDestroy(pC->pLVAudioResampler);
        pC->pLVAudioResampler = M4OSA_NULL;
    }

    /* ----- Free the audio encoder stuff ----- */

    if( M4OSA_NULL != pC->pAudioEncCtxt )
    {
        err = pC->pAudioEncoderGlobalFcts->pFctClose(pC->pAudioEncCtxt);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_cleanUp: pAudioEncoderGlobalFcts->pFctClose returns 0x%x",
                err);
            /**< don't return, we still have stuff to free */
        }

        err = pC->pAudioEncoderGlobalFcts->pFctCleanUp(pC->pAudioEncCtxt);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_cleanUp: pAudioEncoderGlobalFcts->pFctCleanUp returns 0x%x",
                err);
            /**< don't return, we still have stuff to free */
        }

        pC->pAudioEncCtxt = M4OSA_NULL;
    }

    if( M4OSA_NULL != pC->pAudioEncoderBuffer )
    {
        free(pC->pAudioEncoderBuffer);
        pC->pAudioEncoderBuffer = M4OSA_NULL;
    }

    /* ----- Free all other stuff ----- */

    /**
    * Free the readers and the decoders */
    M4MCS_intCleanUp_ReadersDecoders(pC);

#ifdef M4MCS_SUPPORT_STILL_PICTURE
    /**
    * Free the still picture resources */

    M4MCS_stillPicCleanUp(pC);

#endif /*M4MCS_SUPPORT_STILL_PICTURE*/

    /**
    * Free the shells interfaces */

    M4MCS_unRegisterAllWriters(pContext);
    M4MCS_unRegisterAllEncoders(pContext);
    M4MCS_unRegisterAllReaders(pContext);
    M4MCS_unRegisterAllDecoders(pContext);

    /**
    * Free the context itself */
    free(pC);
    pC = M4OSA_NULL;

    M4OSA_TRACE3_0("M4MCS_cleanUp(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_abort(M4MCS_Context pContext);
 * @brief    Finish the MCS transcoding and free all resources used by the MCS
 *          whatever the state is.
 * @note    The context is no more valid after this call
 * @param    pContext            (IN) MCS context
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pContext is M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR M4MCS_abort( M4MCS_Context pContext )
{
    M4OSA_ERR err = M4NO_ERROR;
    M4OSA_ERR err1 = M4NO_ERROR;
    M4MCS_InternalContext *pC = (M4MCS_InternalContext *)(pContext);

    if( M4OSA_NULL == pContext )
    {
        return M4NO_ERROR;
    }

    if( ( pC->State == M4MCS_kState_CREATED)
        || (pC->State == M4MCS_kState_CLOSED) )
    {
        pC->State = M4MCS_kState_CLOSED;

        err = M4MCS_cleanUp(pContext);

        if( err != M4NO_ERROR )
        {
            M4OSA_TRACE1_1("M4MCS_abort : M4MCS_cleanUp fails err = 0x%x", err);
        }
    }
    else
    {
#ifdef M4MCS_SUPPORT_STILL_PICTURE

        if( pC->m_bIsStillPicture )
        {
            /**
            * Cancel the ongoing processes if any*/
            err = M4MCS_stillPicCancel(pC);

            if( err != M4NO_ERROR )
            {
                M4OSA_TRACE1_1(
                    "M4MCS_abort : M4MCS_stillPicCancel fails err = 0x%x", err);
            }
            /*Still picture process is now stopped; Carry on with close and cleanup*/
        }

#endif /*M4MCS_SUPPORT_STILL_PICTURE*/

        pC->State = M4MCS_kState_FINISHED;

        err = M4MCS_close(pContext);

        if( err != M4NO_ERROR )
        {
            M4OSA_TRACE1_1("M4MCS_abort : M4MCS_close fails err = 0x%x", err);
            err1 = err;
        }

        err = M4MCS_cleanUp(pContext);

        if( err != M4NO_ERROR )
        {
            M4OSA_TRACE1_1("M4MCS_abort : M4MCS_cleanUp fails err = 0x%x", err);
        }
    }
    err = (err1 == M4NO_ERROR) ? err : err1;
    return err;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_getInputFileProperties(M4MCS_Context pContext,
 *                                         M4VIDEOEDITING_ClipProperties* pFileProperties);
 * @brief   Retrieves the properties of the audio and video streams from the input file.
 * @param   pContext            (IN) MCS context
 * @param   pProperties         (OUT) Pointer on an allocated M4VIDEOEDITING_ClipProperties
structure which is filled with the input stream properties.
 * @note    The structure pProperties must be allocated and further de-allocated
by the application. The function must be called in the opened state.
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 ******************************************************************************
 */
M4OSA_ERR M4MCS_getInputFileProperties( M4MCS_Context pContext,
                                       M4VIDEOEDITING_ClipProperties *pFileProperties )
{
    M4MCS_InternalContext *pC = (M4MCS_InternalContext *)(pContext);

    M4OSA_TRACE2_2("M4MCS_getInputFileProperties called with pContext=0x%x, \
                   pFileProperties=0x%x", pContext, pFileProperties);

    /**
    * Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4MCS_getInputFileProperties: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pFileProperties), M4ERR_PARAMETER,
        "M4MCS_getInputFileProperties: pProperties is M4OSA_NULL");

#ifdef M4MCS_SUPPORT_STILL_PICTURE

    if( pC->m_bIsStillPicture )
    {
        /**
        * Call the corresponding still picture MCS function*/
        return M4MCS_stillPicGetInputFileProperties(pC, pFileProperties);
    }

#endif /*M4MCS_SUPPORT_STILL_PICTURE*/

    /**
    * Check state automaton */

    if( M4MCS_kState_OPENED != pC->State )
    {
        M4OSA_TRACE1_1(
            "M4MCS_getInputFileProperties(): Wrong State (%d), returning M4ERR_STATE",
            pC->State);
        return M4ERR_STATE;
    }

    /**
    * Copy previously computed properties into given structure */
    memcpy((void *)pFileProperties,
        (void *) &pC->InputFileProperties,
        sizeof(M4VIDEOEDITING_ClipProperties));

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_setOutputParams(M4MCS_Context pContext, M4MCS_OutputParams* pParams);
 * @brief   Set the MCS video output parameters.
 * @note    Must be called after M4MCS_open. Must be called before M4MCS_step.
 * @param   pContext            (IN) MCS context
 * @param   pParams             (IN/OUT) Transcoding parameters
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 * @return  M4MCS_ERR_INVALID_VIDEO_FRAME_SIZE_FOR_H263 : Output video frame size parameter is
 *                                                        incompatible with H263 encoding
 * @return  M4MCS_ERR_INVALID_VIDEO_FRAME_RATE_FOR_H263 : Output video frame size parameter is
 *                                                        incompatible with H263 encoding
 * @return  M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FORMAT     : Undefined output video format parameter
 * @return  M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FRAME_SIZE : Undefined output video frame size
 * @return  M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FRAME_RATE : Undefined output video frame rate
 * @return  M4MCS_ERR_UNDEFINED_OUTPUT_AUDIO_FORMAT : Undefined output audio format parameter
 * @return  M4MCS_ERR_DURATION_IS_NULL : Specified output parameters define a null duration stream
 *                                         (no audio and video)
 ******************************************************************************
 */
M4OSA_ERR M4MCS_setOutputParams( M4MCS_Context pContext,
                                M4MCS_OutputParams *pParams )
{
    M4MCS_InternalContext *pC = (M4MCS_InternalContext *)(pContext);
    M4OSA_UInt32 uiFrameWidth;
    M4OSA_UInt32 uiFrameHeight;
    M4OSA_ERR err;

    M4OSA_TRACE2_2(
        "M4MCS_setOutputParams called with pContext=0x%x, pParams=0x%x",
        pContext, pParams);

    /**
    * Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4MCS_setOutputParams: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pParams), M4ERR_PARAMETER,
        "M4MCS_setOutputParams: pParam is M4OSA_NULL");

#ifdef M4MCS_SUPPORT_STILL_PICTURE

    if( pC->m_bIsStillPicture )
    {
        /**
        * Call the corresponding still picture MCS function*/
        return M4MCS_stillPicSetOutputParams(pC, pParams);
    }

#endif /*M4MCS_SUPPORT_STILL_PICTURE*/

    /**
    * Check state automaton */

    if( M4MCS_kState_OPENED != pC->State )
    {
        M4OSA_TRACE1_1(
            "M4MCS_setOutputParams(): Wrong State (%d), returning M4ERR_STATE",
            pC->State);
        return M4ERR_STATE;
    }

    /* Ignore audio or video stream if the output do not need it, */
    /* or if the input file does not have any audio or video stream */
    /*FlB 26.02.2009: add mp3 as mcs output format*/
    if( ( pParams->OutputVideoFormat == M4VIDEOEDITING_kNoneVideo)
        || (pC->VideoState == M4MCS_kStreamState_NOSTREAM)
        || (pParams->OutputFileType == M4VIDEOEDITING_kFileType_AMR)
        || (pParams->OutputFileType == M4VIDEOEDITING_kFileType_MP3) )
    {
        pC->novideo = M4OSA_TRUE;
    }

    if( ( pParams->OutputAudioFormat == M4VIDEOEDITING_kNoneAudio)
        || (pC->AudioState == M4MCS_kStreamState_NOSTREAM) )
    {
        pC->noaudio = M4OSA_TRUE;
    }

    if( pC->noaudio && pC->novideo )
    {
        M4OSA_TRACE1_0(
            "!!! M4MCS_setOutputParams : clip is NULL, there is no audio, no video");
        return M4MCS_ERR_DURATION_IS_NULL;
    }

    /* Set writer */
    err = M4MCS_setCurrentWriter(pContext, pParams->OutputFileType);
    M4ERR_CHECK_RETURN(err);

    /* Set video parameters */
    if( pC->novideo == M4OSA_FALSE )
    {
        /**
        * Check Video Format correctness */

        switch( pParams->OutputVideoFormat )
        {
            case M4VIDEOEDITING_kH263:
                if( pParams->OutputFileType == M4VIDEOEDITING_kFileType_MP4 )
                    return M4MCS_ERR_H263_FORBIDDEN_IN_MP4_FILE;

                pC->EncodingVideoFormat = M4ENCODER_kH263;
                err = M4MCS_setCurrentVideoEncoder(pContext,
                    pParams->OutputVideoFormat);
                M4ERR_CHECK_RETURN(err);
                break;

            case M4VIDEOEDITING_kMPEG4:

                pC->EncodingVideoFormat = M4ENCODER_kMPEG4;
                err = M4MCS_setCurrentVideoEncoder(pContext,
                    pParams->OutputVideoFormat);
                M4ERR_CHECK_RETURN(err);
                break;

            case M4VIDEOEDITING_kH264:

                pC->EncodingVideoFormat = M4ENCODER_kH264;
                err = M4MCS_setCurrentVideoEncoder(pContext,
                    pParams->OutputVideoFormat);
                M4ERR_CHECK_RETURN(err);
                break;

            case M4VIDEOEDITING_kNullVideo:
                if( ( pParams->OutputFileType == M4VIDEOEDITING_kFileType_MP4)
                    && (pC->InputFileProperties.VideoStreamType
                    == M4VIDEOEDITING_kH263) )
                    return M4MCS_ERR_H263_FORBIDDEN_IN_MP4_FILE;


                /* Encoder needed for begin cut to generate an I-frame */
                pC->EncodingVideoFormat = M4ENCODER_kNULL;
                err = M4MCS_setCurrentVideoEncoder(pContext,
                    pC->InputFileProperties.VideoStreamType);
                M4ERR_CHECK_RETURN(err);
                break;

            default:
                M4OSA_TRACE1_1("M4MCS_setOutputParams: Undefined output video format (%d),\
                               returning M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FORMAT",
                               pParams->OutputVideoFormat);
                return M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FORMAT;
        }

        /**
        * Check Video frame size correctness */
        if( M4VIDEOEDITING_kNullVideo == pParams->OutputVideoFormat )
        {
            uiFrameWidth =
                pC->EncodingWidth = pC->InputFileProperties.uiVideoWidth;
            uiFrameHeight =
                pC->EncodingHeight = pC->InputFileProperties.uiVideoHeight;

            /**
            * Set output video profile and level */
            pC->encodingVideoProfile = pC->InputFileProperties.uiVideoProfile;
            /** Set the target video level, because input 3gp file may
             *  have wrong video level value (some encoders do not respect
             *  level restrictions like video resolution when content is created).
             **/
            pC->encodingVideoLevel = pParams->outputVideoLevel;

            // Clip's original width and height may not be
            // multiple of 16.
            // Ensure encoding width and height are multiple of 16

            uint32_t remainder = pC->EncodingWidth % 16;
            if (remainder != 0) {
                if (remainder >= 8) {
                    // Roll forward
                    pC->EncodingWidth =
                        pC->EncodingWidth + (16-remainder);
                } else {
                    // Roll backward
                    pC->EncodingWidth =
                        pC->EncodingWidth - remainder;
                }
                uiFrameWidth = pC->EncodingWidth;
            }

            remainder = pC->EncodingHeight % 16;
            if (remainder != 0) {
                if (remainder >= 8) {
                    // Roll forward
                    pC->EncodingHeight =
                        pC->EncodingHeight + (16-remainder);
                } else {
                    // Roll backward
                    pC->EncodingHeight =
                        pC->EncodingHeight - remainder;
                }
                uiFrameHeight = pC->EncodingHeight;
            }

        }
        else
        {
            /**
            * Set output video profile and level */
            pC->encodingVideoProfile = pParams->outputVideoProfile;
            pC->encodingVideoLevel = pParams->outputVideoLevel;

            switch( pParams->OutputVideoFrameSize )
            {
                case M4VIDEOEDITING_kSQCIF:
                    uiFrameWidth = pC->EncodingWidth = M4ENCODER_SQCIF_Width;
                    uiFrameHeight = pC->EncodingHeight = M4ENCODER_SQCIF_Height;
                    break;

                case M4VIDEOEDITING_kQQVGA:
                    uiFrameWidth = pC->EncodingWidth = M4ENCODER_QQVGA_Width;
                    uiFrameHeight = pC->EncodingHeight = M4ENCODER_QQVGA_Height;
                    break;

                case M4VIDEOEDITING_kQCIF:
                    uiFrameWidth = pC->EncodingWidth = M4ENCODER_QCIF_Width;
                    uiFrameHeight = pC->EncodingHeight = M4ENCODER_QCIF_Height;
                    break;

                case M4VIDEOEDITING_kQVGA:
                    uiFrameWidth = pC->EncodingWidth = M4ENCODER_QVGA_Width;
                    uiFrameHeight = pC->EncodingHeight = M4ENCODER_QVGA_Height;
                    break;

                case M4VIDEOEDITING_kCIF:
                    uiFrameWidth = pC->EncodingWidth = M4ENCODER_CIF_Width;
                    uiFrameHeight = pC->EncodingHeight = M4ENCODER_CIF_Height;
                    break;

                case M4VIDEOEDITING_kVGA:
                    uiFrameWidth = pC->EncodingWidth = M4ENCODER_VGA_Width;
                    uiFrameHeight = pC->EncodingHeight = M4ENCODER_VGA_Height;
                    break;
                    /* +PR LV5807 */
                case M4VIDEOEDITING_kWVGA:
                    uiFrameWidth = pC->EncodingWidth = M4ENCODER_WVGA_Width;
                    uiFrameHeight = pC->EncodingHeight = M4ENCODER_WVGA_Height;
                    break;

                case M4VIDEOEDITING_kNTSC:
                    uiFrameWidth = pC->EncodingWidth = M4ENCODER_NTSC_Width;
                    uiFrameHeight = pC->EncodingHeight = M4ENCODER_NTSC_Height;
                    break;
                    /* -PR LV5807*/
                    /* +CR Google */
                case M4VIDEOEDITING_k640_360:
                    uiFrameWidth = pC->EncodingWidth = M4ENCODER_640_360_Width;
                    uiFrameHeight =
                        pC->EncodingHeight = M4ENCODER_640_360_Height;
                    break;

                case M4VIDEOEDITING_k854_480:
                    uiFrameWidth =
                        pC->EncodingWidth = M4ENCODER_854_480_Width;
                    uiFrameHeight =
                        pC->EncodingHeight = M4ENCODER_854_480_Height;
                    break;

                case M4VIDEOEDITING_k1280_720:
                    uiFrameWidth =
                        pC->EncodingWidth = M4ENCODER_1280_720_Width;
                    uiFrameHeight =
                        pC->EncodingHeight = M4ENCODER_1280_720_Height;
                    break;

                case M4VIDEOEDITING_k1080_720:
                    uiFrameWidth =
                        pC->EncodingWidth = M4ENCODER_1080_720_Width;
                    uiFrameHeight =
                        pC->EncodingHeight = M4ENCODER_1080_720_Height;
                    break;

                case M4VIDEOEDITING_k960_720:
                    uiFrameWidth =
                        pC->EncodingWidth = M4ENCODER_960_720_Width;
                    uiFrameHeight =
                        pC->EncodingHeight = M4ENCODER_960_720_Height;
                    break;

                case M4VIDEOEDITING_k1920_1080:
                    uiFrameWidth =
                        pC->EncodingWidth = M4ENCODER_1920_1080_Width;
                    uiFrameHeight =
                        pC->EncodingHeight = M4ENCODER_1920_1080_Height;
                    break;
                    /* -CR Google */
                default:
                    M4OSA_TRACE1_1(
                        "M4MCS_setOutputParams: Undefined output video frame size \
                        (%d), returning M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FRAME_SIZE",
                        pParams->OutputVideoFrameSize);
                    return M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FRAME_SIZE;
            }
        }

        /**
        * Compute video max au size and max chunck size.
        * We do it here because it depends on the frame size only, and
        * because we need it for the file size/video bitrate estimations */
        pC->uiVideoMaxAuSize =
            (M4OSA_UInt32)(1.5F *(M4OSA_Float)(uiFrameWidth * uiFrameHeight) \
            *M4MCS_VIDEO_MIN_COMPRESSION_RATIO);
        pC->uiVideoMaxChunckSize = (M4OSA_UInt32)(pC->uiVideoMaxAuSize       \
            *
            M4MCS_VIDEO_CHUNK_AU_SIZE_RATIO); /**< from max AU size to max Chunck size */

        if( 0 == pC->uiVideoMaxAuSize )
        {
            /* Size may be zero in case of null encoding with unrecognized stream */
            M4OSA_TRACE1_0("M4MCS_setOutputParams: video frame size is 0 returning\
                           M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FRAME_SIZE");
            return M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FRAME_SIZE;
        }


        /**
        * Size check for H263 (only valid sizes are CIF, QCIF and SQCIF) */

        if( M4VIDEOEDITING_kH263 == pParams->OutputVideoFormat )
        {
            switch( pParams->OutputVideoFrameSize )
            {
                case M4VIDEOEDITING_kSQCIF:
                case M4VIDEOEDITING_kQCIF:
                case M4VIDEOEDITING_kCIF:
                    /* OK */
                    break;

                default:
                    M4OSA_TRACE1_0(
                        "M4MCS_setOutputParams():\
                        returning M4MCS_ERR_INVALID_VIDEO_FRAME_SIZE_FOR_H263");
                    return M4MCS_ERR_INVALID_VIDEO_FRAME_SIZE_FOR_H263;
            }
        }

        /**
        * Check Video Frame rate correctness */
        if( M4VIDEOEDITING_kNullVideo != pParams->OutputVideoFormat )
        {
            switch( pParams->OutputVideoFrameRate )
            {
                case M4VIDEOEDITING_k5_FPS:
                    pC->EncodingVideoFramerate = M4ENCODER_k5_FPS;
                    break;

                case M4VIDEOEDITING_k7_5_FPS:
                    pC->EncodingVideoFramerate = M4ENCODER_k7_5_FPS;
                    break;

                case M4VIDEOEDITING_k10_FPS:
                    pC->EncodingVideoFramerate = M4ENCODER_k10_FPS;
                    break;

                case M4VIDEOEDITING_k12_5_FPS:
                    pC->EncodingVideoFramerate = M4ENCODER_k12_5_FPS;
                    break;

                case M4VIDEOEDITING_k15_FPS:
                    pC->EncodingVideoFramerate = M4ENCODER_k15_FPS;
                    break;

                case M4VIDEOEDITING_k20_FPS:
                    pC->EncodingVideoFramerate = M4ENCODER_k20_FPS;
                    break;

                case M4VIDEOEDITING_k25_FPS:
                    pC->EncodingVideoFramerate = M4ENCODER_k25_FPS;
                    break;

                case M4VIDEOEDITING_k30_FPS:
                    pC->EncodingVideoFramerate = M4ENCODER_k30_FPS;
                    break;

                default:
                    M4OSA_TRACE1_1(
                        "M4MCS_setOutputParams: Undefined output video frame rate\
                        (%d), returning M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FRAME_RATE",
                        pParams->OutputVideoFrameRate);
                    return M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FRAME_RATE;
            }
        }

        /**
        * Frame rate check for H263 (only dividers of 30 fps (29.97 actually)) */
        if( M4VIDEOEDITING_kH263 == pParams->OutputVideoFormat )
        {
            switch( pC->EncodingVideoFramerate )
            {
                case M4ENCODER_k5_FPS:
                case M4ENCODER_k7_5_FPS:
                case M4ENCODER_k10_FPS:
                case M4ENCODER_k15_FPS:
                case M4ENCODER_k30_FPS:
                    /* OK */
                    break;

                default:
                    M4OSA_TRACE1_0(
                        "M4MCS_setOutputParams():\
                        returning M4MCS_ERR_INVALID_VIDEO_FRAME_RATE_FOR_H263");
                    return M4MCS_ERR_INVALID_VIDEO_FRAME_RATE_FOR_H263;
            }
        }
    }

    /* Set audio parameters */
    if( pC->noaudio == M4OSA_FALSE )
    {
        /**
        * Check Audio Format correctness */
        switch( pParams->OutputAudioFormat )
        {
            case M4VIDEOEDITING_kAMR_NB:

                err = M4MCS_setCurrentAudioEncoder(pContext,
                    pParams->OutputAudioFormat);
                M4ERR_CHECK_RETURN(err);

                pC->AudioEncParams.Format = M4ENCODER_kAMRNB;
                pC->AudioEncParams.Frequency = M4ENCODER_k8000Hz;
                pC->AudioEncParams.ChannelNum = M4ENCODER_kMono;
                pC->AudioEncParams.SpecifParam.AmrSID = M4ENCODER_kAmrNoSID;
                break;

            case M4VIDEOEDITING_kAAC:

                err = M4MCS_setCurrentAudioEncoder(pContext,
                    pParams->OutputAudioFormat);
                M4ERR_CHECK_RETURN(err);

                pC->AudioEncParams.Format = M4ENCODER_kAAC;
                pC->AudioEncParams.Frequency = M4ENCODER_k16000Hz;

                switch( pParams->OutputAudioSamplingFrequency )
                {
                    case M4VIDEOEDITING_k8000_ASF:
                        pC->AudioEncParams.Frequency = M4ENCODER_k8000Hz;
                        break;

                    case M4VIDEOEDITING_k16000_ASF:
                        pC->AudioEncParams.Frequency = M4ENCODER_k16000Hz;
                        break;

                    case M4VIDEOEDITING_k22050_ASF:
                        pC->AudioEncParams.Frequency = M4ENCODER_k22050Hz;
                        break;

                    case M4VIDEOEDITING_k24000_ASF:
                        pC->AudioEncParams.Frequency = M4ENCODER_k24000Hz;
                        break;

                    case M4VIDEOEDITING_k32000_ASF:
                        pC->AudioEncParams.Frequency = M4ENCODER_k32000Hz;
                        break;

                    case M4VIDEOEDITING_k44100_ASF:
                        pC->AudioEncParams.Frequency = M4ENCODER_k44100Hz;
                        break;

                    case M4VIDEOEDITING_k48000_ASF:
                        pC->AudioEncParams.Frequency = M4ENCODER_k48000Hz;
                        break;

                    case M4VIDEOEDITING_k11025_ASF:
                    case M4VIDEOEDITING_k12000_ASF:
                    case M4VIDEOEDITING_kDefault_ASF:
                        break;
                }
                    pC->AudioEncParams.ChannelNum =
                        (pParams->bAudioMono == M4OSA_TRUE) ? \
                        M4ENCODER_kMono : M4ENCODER_kStereo;
                    pC->AudioEncParams.SpecifParam.AacParam.Regulation =
                        M4ENCODER_kAacRegulNone; //M4ENCODER_kAacBitReservoir
                    /* unused */
                    pC->AudioEncParams.SpecifParam.AacParam.bIS = M4OSA_FALSE;
                    pC->AudioEncParams.SpecifParam.AacParam.bMS = M4OSA_FALSE;
                    pC->AudioEncParams.SpecifParam.AacParam.bPNS = M4OSA_FALSE;
                    pC->AudioEncParams.SpecifParam.AacParam.bTNS = M4OSA_FALSE;
                    /* TODO change into highspeed asap */
                    pC->AudioEncParams.SpecifParam.AacParam.bHighSpeed =
                        M4OSA_FALSE;
                    break;

                    /*FlB 26.02.2009: add mp3 as mcs output format, add mp3 encoder*/
                case M4VIDEOEDITING_kMP3:
                    err = M4MCS_setCurrentAudioEncoder(pContext,
                        pParams->OutputAudioFormat);
                    M4ERR_CHECK_RETURN(err);

                    pC->AudioEncParams.Format = M4ENCODER_kMP3;
                    pC->AudioEncParams.ChannelNum =
                        (pParams->bAudioMono == M4OSA_TRUE) ? \
                        M4ENCODER_kMono : M4ENCODER_kStereo;

                    pC->AudioEncParams.Frequency = M4ENCODER_k16000Hz;

                    switch( pParams->OutputAudioSamplingFrequency )
                    {
                        case M4VIDEOEDITING_k8000_ASF:
                            pC->AudioEncParams.Frequency = M4ENCODER_k8000Hz;
                            break;

                        case M4VIDEOEDITING_k11025_ASF:
                            pC->AudioEncParams.Frequency = M4ENCODER_k11025Hz;
                            break;

                        case M4VIDEOEDITING_k12000_ASF:
                            pC->AudioEncParams.Frequency = M4ENCODER_k12000Hz;
                            break;

                        case M4VIDEOEDITING_k16000_ASF:
                            pC->AudioEncParams.Frequency = M4ENCODER_k16000Hz;
                            break;

                        case M4VIDEOEDITING_k22050_ASF:
                            pC->AudioEncParams.Frequency = M4ENCODER_k22050Hz;
                            break;

                        case M4VIDEOEDITING_k24000_ASF:
                            pC->AudioEncParams.Frequency = M4ENCODER_k24000Hz;
                            break;

                        case M4VIDEOEDITING_k32000_ASF:
                            pC->AudioEncParams.Frequency = M4ENCODER_k32000Hz;
                            break;

                        case M4VIDEOEDITING_k44100_ASF:
                            pC->AudioEncParams.Frequency = M4ENCODER_k44100Hz;
                            break;

                        case M4VIDEOEDITING_k48000_ASF:
                            pC->AudioEncParams.Frequency = M4ENCODER_k48000Hz;
                            break;

                        case M4VIDEOEDITING_kDefault_ASF:
                            break;
                    }

                    break;

                case M4VIDEOEDITING_kNullAudio:
                    if( pParams->pEffects == M4OSA_NULL || pParams->nbEffects == 0 )
                    {
                        /* no encoder needed */
                        pC->AudioEncParams.Format = M4ENCODER_kAudioNULL;
                        pC->AudioEncParams.Frequency =
                            pC->pReaderAudioStream->m_samplingFrequency;
                        pC->AudioEncParams.ChannelNum =
                            (pC->pReaderAudioStream->m_nbChannels == 1) ? \
                            M4ENCODER_kMono : M4ENCODER_kStereo;
                    }
                    else
                    {
                        pC->AudioEncParams.Frequency =
                            pC->pReaderAudioStream->m_samplingFrequency;
                        pC->AudioEncParams.ChannelNum =
                            (pC->pReaderAudioStream->m_nbChannels == 1) ? \
                            M4ENCODER_kMono : M4ENCODER_kStereo;

                        switch( pC->InputFileProperties.AudioStreamType )
                        {
                            case M4VIDEOEDITING_kAMR_NB:
                                M4OSA_TRACE3_0(
                                    "M4MCS_setOutputParams calling \
                                    M4MCS_setCurrentAudioEncoder M4VIDEOEDITING_kNull, AMR");
                                err = M4MCS_setCurrentAudioEncoder(pContext,
                                    pC->InputFileProperties.AudioStreamType);
                                M4ERR_CHECK_RETURN(err);

                                pC->AudioEncParams.Format = M4ENCODER_kAMRNB;
                                pC->AudioEncParams.Frequency = M4ENCODER_k8000Hz;
                                pC->AudioEncParams.ChannelNum = M4ENCODER_kMono;

                                if( pC->pReaderAudioStream->m_samplingFrequency
                                    != 8000 )
                                {
                                    pC->AudioEncParams.Format = M4ENCODER_kAMRNB;
                                }
                                pC->AudioEncParams.SpecifParam.AmrSID =
                                    M4ENCODER_kAmrNoSID;
                                break;

                            case M4VIDEOEDITING_kAAC:
                                M4OSA_TRACE3_0(
                                    "M4MCS_setOutputParams calling \
                                    M4MCS_setCurrentAudioEncoder M4VIDEOEDITING_kNull, AAC");
                                err = M4MCS_setCurrentAudioEncoder(pContext,
                                    pC->InputFileProperties.AudioStreamType);
                                M4ERR_CHECK_RETURN(err);

                                pC->AudioEncParams.Format = M4ENCODER_kAAC;
                                pC->AudioEncParams.SpecifParam.AacParam.Regulation =
                                    M4ENCODER_kAacRegulNone; //M4ENCODER_kAacBitReservoir
                                pC->AudioEncParams.Frequency = M4ENCODER_k16000Hz;
                                pC->AudioEncParams.Frequency = M4ENCODER_k16000Hz;

                                switch( pC->pReaderAudioStream->
                                    m_samplingFrequency )
                                {
                                case 16000:
                                    pC->AudioEncParams.Frequency =
                                        M4ENCODER_k16000Hz;
                                    break;

                                case 22050:
                                    pC->AudioEncParams.Frequency =
                                        M4ENCODER_k22050Hz;
                                    break;

                                case 24000:
                                    pC->AudioEncParams.Frequency =
                                        M4ENCODER_k24000Hz;
                                    break;

                                case 32000:
                                    pC->AudioEncParams.Frequency =
                                        M4ENCODER_k32000Hz;
                                    break;

                                case 44100:
                                    pC->AudioEncParams.Frequency =
                                        M4ENCODER_k44100Hz;
                                    break;

                                case 48000:
                                    pC->AudioEncParams.Frequency =
                                        M4ENCODER_k48000Hz;
                                    break;

                                default:
                                    pC->AudioEncParams.Format = M4ENCODER_kAAC;
                                    break;
                            }
                            /* unused */
                            pC->AudioEncParams.SpecifParam.AacParam.bIS =
                                M4OSA_FALSE;
                            pC->AudioEncParams.SpecifParam.AacParam.bMS =
                                M4OSA_FALSE;
                            pC->AudioEncParams.SpecifParam.AacParam.bPNS =
                                M4OSA_FALSE;
                            pC->AudioEncParams.SpecifParam.AacParam.bTNS =
                                M4OSA_FALSE;
                            /* TODO change into highspeed asap */
                            pC->AudioEncParams.SpecifParam.AacParam.bHighSpeed =
                                M4OSA_FALSE;
                            break;

                        case M4VIDEOEDITING_kMP3:
                            M4OSA_TRACE3_0(
                                "M4MCS_setOutputParams calling\
                                M4MCS_setCurrentAudioEncoder M4VIDEOEDITING_kNull, MP3");
                            err = M4MCS_setCurrentAudioEncoder(pContext,
                                pC->InputFileProperties.AudioStreamType);
                            M4ERR_CHECK_RETURN(err);

                            pC->AudioEncParams.Format = M4ENCODER_kMP3;
                            pC->AudioEncParams.Frequency = M4ENCODER_k16000Hz;

                            switch( pC->pReaderAudioStream->
                                m_samplingFrequency )
                            {
                                case 8000:
                                    pC->AudioEncParams.Frequency =
                                        M4ENCODER_k8000Hz;
                                    break;

                                case 16000:
                                    pC->AudioEncParams.Frequency =
                                        M4ENCODER_k16000Hz;
                                    break;

                                case 22050:
                                    pC->AudioEncParams.Frequency =
                                        M4ENCODER_k22050Hz;
                                    break;

                                case 24000:
                                    pC->AudioEncParams.Frequency =
                                        M4ENCODER_k24000Hz;
                                    break;

                                case 32000:
                                    pC->AudioEncParams.Frequency =
                                        M4ENCODER_k32000Hz;
                                    break;

                                case 44100:
                                    pC->AudioEncParams.Frequency =
                                        M4ENCODER_k44100Hz;
                                    break;

                                case 48000:
                                    pC->AudioEncParams.Frequency =
                                        M4ENCODER_k48000Hz;
                                    break;

                                default:
                                    pC->AudioEncParams.Format = M4ENCODER_kMP3;
                                    break;
                            }
                            break;

                        case M4VIDEOEDITING_kEVRC:
                        case M4VIDEOEDITING_kUnsupportedAudio:
                        default:
                            M4OSA_TRACE1_1(
                                "M4MCS_setOutputParams: Output audio format (%d) is\
                                incompatible with audio effects, returning \
                                M4MCS_ERR_UNDEFINED_OUTPUT_AUDIO_FORMAT",
                                pC->InputFileProperties.AudioStreamType);
                            return M4MCS_ERR_UNDEFINED_OUTPUT_AUDIO_FORMAT;
                        }
                    }
                    break;
                    /* EVRC
                    //            case M4VIDEOEDITING_kEVRC:
                    //
                    //                err = M4MCS_setCurrentAudioEncoder(pContext, pParams->\
                    //                    OutputAudioFormat);
                    //                M4ERR_CHECK_RETURN(err);
                    //
                    //                pC->AudioEncParams.Format = M4ENCODER_kEVRC;
                    //                pC->AudioEncParams.Frequency = M4ENCODER_k8000Hz;
                    //                pC->AudioEncParams.ChannelNum = M4ENCODER_kMono;
                    //                break; */

                default:
                    M4OSA_TRACE1_1("M4MCS_setOutputParams: Undefined output audio format (%d),\
                                   returning M4MCS_ERR_UNDEFINED_OUTPUT_AUDIO_FORMAT",
                                   pParams->OutputAudioFormat);
                    return M4MCS_ERR_UNDEFINED_OUTPUT_AUDIO_FORMAT;
        }
    }

    if( pParams->pOutputPCMfile != M4OSA_NULL )
    {
        pC->pOutputPCMfile = pParams->pOutputPCMfile;

        /* Open output PCM file */
        pC->pOsaFileWritPtr->openWrite(&(pC->pOutputPCMfile),
            pParams->pOutputPCMfile, M4OSA_kFileWrite);
    }
    else
    {
        pC->pOutputPCMfile = M4OSA_NULL;
    }

    /*Store media rendering parameter into the internal context*/
    pC->MediaRendering = pParams->MediaRendering;

    /* Add audio effects*/
    /*Copy MCS effects structure into internal context*/
    if( pParams->nbEffects > 0 )
    {
        M4OSA_UInt32 j = 0;
        pC->nbEffects = pParams->nbEffects;
        pC->pEffects = (M4MCS_EffectSettings *)M4OSA_32bitAlignedMalloc(pC->nbEffects \
            *sizeof(M4MCS_EffectSettings), M4MCS,
            (M4OSA_Char *)"Allocation of effects list");

        if( pC->pEffects == M4OSA_NULL )
        {
            M4OSA_TRACE1_0("M4MCS_setOutputParams(): allocation error");
            return M4ERR_ALLOC;
        }

        for ( j = 0; j < pC->nbEffects; j++ )
        {
            /* Copy effect to "local" structure */
            memcpy((void *) &(pC->pEffects[j]),
                (void *) &(pParams->pEffects[j]),
                sizeof(M4MCS_EffectSettings));

            switch( pC->pEffects[j].AudioEffectType )
            {
                case M4MCS_kAudioEffectType_None:
                    M4OSA_TRACE3_1(
                        "M4MCS_setOutputParams(): effect type %i is None", j);
                    pC->pEffects[j].pExtAudioEffectFctCtxt = M4OSA_NULL;
                    pC->pEffects[j].ExtAudioEffectFct = M4OSA_NULL;
                    break;

                case M4MCS_kAudioEffectType_FadeIn:
                    M4OSA_TRACE3_1(
                        "M4MCS_setOutputParams(): effect type %i is FadeIn", j);
                    pC->pEffects[j].pExtAudioEffectFctCtxt = M4OSA_NULL;
                    pC->pEffects[j].ExtAudioEffectFct =
                        M4MCS_editAudioEffectFct_FadeIn;
                    break;

                case M4MCS_kAudioEffectType_FadeOut:
                    M4OSA_TRACE3_1(
                        "M4MCS_setOutputParams(): effect type %i is FadeOut",
                        j);
                    pC->pEffects[j].pExtAudioEffectFctCtxt = M4OSA_NULL;
                    pC->pEffects[j].ExtAudioEffectFct =
                        M4MCS_editAudioEffectFct_FadeOut;
                    break;

                case M4MCS_kAudioEffectType_External:
                    M4OSA_TRACE3_1(
                        "M4MCS_setOutputParams(): effect type %i is External",
                        j);

                    if( pParams->pEffects != M4OSA_NULL )
                    {
                        if( pParams->pEffects[j].ExtAudioEffectFct
                            == M4OSA_NULL )
                        {
                            M4OSA_TRACE1_1("M4MCS_setOutputParams(): no external effect function\
                                           associated to external effect number %i", j);
                            return M4ERR_PARAMETER;
                        }
                        pC->pEffects[j].pExtAudioEffectFctCtxt =
                            pParams->pEffects[j].pExtAudioEffectFctCtxt;

                        pC->pEffects[j].ExtAudioEffectFct =
                            pParams->pEffects[j].ExtAudioEffectFct;
                    }

                    break;

                default:
                    M4OSA_TRACE1_0(
                        "M4MCS_setOutputParams(): effect type not recognized");
                    return M4ERR_PARAMETER;
            }
        }
    }
    else
    {
        pC->nbEffects = 0;
        pC->pEffects = M4OSA_NULL;
    }

    /**
    * Update state automaton */
    pC->State = M4MCS_kState_SET;

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4MCS_setOutputParams(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_setEncodingParams(M4MCS_Context pContext, M4MCS_EncodingParams* pRates)
 * @brief   Set the values of the encoding parameters
 * @note    Must be called before M4MCS_checkParamsAndStart().
 * @param   pContext           (IN) MCS context
 * @param   pRates             (IN) Transcoding parameters
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 * @return  M4MCS_ERR_AUDIOBITRATE_TOO_HIGH: Audio bitrate too high (we limit to 96 kbps)
 * @return  M4MCS_ERR_AUDIOBITRATE_TOO_LOW: Audio bitrate is too low (16 kbps min for aac, 12.2
 *                                            for amr, 8 for mp3)
 * @return  M4MCS_ERR_BEGIN_CUT_EQUALS_END_CUT: Begin cut and End cut are equals
 * @return  M4MCS_ERR_BEGIN_CUT_LARGER_THAN_DURATION: Begin cut time is larger than the input clip
 *                                                     duration
 * @return  M4MCS_ERR_END_CUT_SMALLER_THAN_BEGIN_CUT: End cut time is smaller than begin cut time
 * @return  M4MCS_ERR_MAXFILESIZE_TOO_SMALL: Not enough space to store whole output file at given
 *                                             bitrates
 * @return  M4MCS_ERR_VIDEOBITRATE_TOO_HIGH: Video bitrate too high (we limit to 800 kbps)
 * @return  M4MCS_ERR_VIDEOBITRATE_TOO_LOW:  Video bitrate too low
 ******************************************************************************
 */
M4OSA_ERR M4MCS_setEncodingParams( M4MCS_Context pContext,
                                  M4MCS_EncodingParams *pRates )
{
    M4MCS_InternalContext *pC = (M4MCS_InternalContext *)(pContext);
    M4OSA_UInt32 j = 0;

    M4OSA_TRACE2_2(
        "M4MCS_setEncodingParams called with pContext=0x%x, pRates=0x%x",
        pContext, pRates);

    /**
    * Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4MCS_setEncodingParams: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pRates), M4ERR_PARAMETER,
        "M4MCS_setEncodingParams: pRates is M4OSA_NULL");

#ifdef M4MCS_SUPPORT_STILL_PICTURE

    if( pC->m_bIsStillPicture )
    {
        /**
        * Call the corresponding still picture MCS function*/
        return M4MCS_stillPicSetEncodingParams(pC, pRates);
    }

#endif /*M4MCS_SUPPORT_STILL_PICTURE*/

    /**
    * Check state automaton */

    if( M4MCS_kState_SET != pC->State )
    {
        M4OSA_TRACE1_1(
            "M4MCS_setEncodingParams(): Wrong State (%d), returning M4ERR_STATE",
            pC->State);
        return M4ERR_STATE;
    }

    /* Set given values */
    pC->uiVideoBitrate = pRates->OutputVideoBitrate;
    pC->uiAudioBitrate = pRates->OutputAudioBitrate;
    pC->uiBeginCutTime = pRates->BeginCutTime;
    pC->uiEndCutTime = pRates->EndCutTime;
    pC->uiMaxFileSize = pRates->OutputFileSize;

    /**
    * Check begin cut time validity */
    if( pC->uiBeginCutTime >= pC->InputFileProperties.uiClipDuration )
    {
        M4OSA_TRACE1_2("M4MCS_setEncodingParams(): Begin cut larger than duration (%d>%d),\
                       returning M4MCS_ERR_BEGIN_CUT_LARGER_THAN_DURATION",
                       pC->uiBeginCutTime, pC->InputFileProperties.uiClipDuration);
        return M4MCS_ERR_BEGIN_CUT_LARGER_THAN_DURATION;
    }

    /**
    * If end cut time is too large, we set it to the clip duration */
    if( pC->uiEndCutTime > pC->InputFileProperties.uiClipDuration )
    {
        pC->uiEndCutTime = pC->InputFileProperties.uiClipDuration;
    }

    /**
    * Check end cut time validity */
    if( pC->uiEndCutTime > 0 )
    {
        if( pC->uiEndCutTime < pC->uiBeginCutTime )
        {
            M4OSA_TRACE1_2("M4MCS_setEncodingParams(): Begin cut greater than end cut (%d,%d), \
                           returning M4MCS_ERR_END_CUT_SMALLER_THAN_BEGIN_CUT",
                           pC->uiBeginCutTime, pC->uiEndCutTime);
            return M4MCS_ERR_END_CUT_SMALLER_THAN_BEGIN_CUT;
        }

        if( pC->uiEndCutTime == pC->uiBeginCutTime )
        {
            M4OSA_TRACE1_2("M4MCS_setEncodingParams(): Begin and End cuts are equal (%d,%d),\
                           returning M4MCS_ERR_BEGIN_CUT_EQUALS_END_CUT",
                           pC->uiBeginCutTime, pC->uiEndCutTime);
            return M4MCS_ERR_BEGIN_CUT_EQUALS_END_CUT;
        }
    }

    /**
    * FlB 2009.03.04: check audio effects start time and duration validity*/
    for ( j = 0; j < pC->nbEffects; j++ )
    {
        M4OSA_UInt32 outputEndCut = pC->uiEndCutTime;

        if( pC->uiEndCutTime == 0 )
        {
            outputEndCut = pC->InputFileProperties.uiClipDuration;
        }

        if( pC->pEffects[j].uiStartTime > (outputEndCut - pC->uiBeginCutTime) )
        {
            M4OSA_TRACE1_2("M4MCS_setEncodingParams(): Effects start time is larger than\
                           duration (%d,%d), returning M4ERR_PARAMETER",
                           pC->pEffects[j].uiStartTime,
                           (pC->uiEndCutTime - pC->uiBeginCutTime));
            return M4ERR_PARAMETER;
        }

        if( pC->pEffects[j].uiStartTime + pC->pEffects[j].uiDuration > \
            (outputEndCut - pC->uiBeginCutTime) )
        {
            /* Re-adjust the effect duration until the end of the output clip*/
            pC->pEffects[j].uiDuration = (outputEndCut - pC->uiBeginCutTime) - \
                pC->pEffects[j].uiStartTime;
        }
    }

    /* Check audio bitrate consistency */
    if( ( pC->noaudio == M4OSA_FALSE)
        && (pC->AudioEncParams.Format != M4ENCODER_kAudioNULL) )
    {
        if( pC->uiAudioBitrate != M4VIDEOEDITING_kUndefinedBitrate )
        {
            if( pC->AudioEncParams.Format == M4ENCODER_kAMRNB )
            {
                if( pC->uiAudioBitrate > M4VIDEOEDITING_k12_2_KBPS )
                    return M4MCS_ERR_AUDIOBITRATE_TOO_HIGH;

                if( pC->uiAudioBitrate < M4VIDEOEDITING_k12_2_KBPS )
                    return M4MCS_ERR_AUDIOBITRATE_TOO_LOW;
            }
            //EVRC
            //            else if(pC->AudioEncParams.Format == M4ENCODER_kEVRC)
            //            {
            //                if(pC->uiAudioBitrate > M4VIDEOEDITING_k9_2_KBPS)
            //                    return M4MCS_ERR_AUDIOBITRATE_TOO_HIGH;
            //                if(pC->uiAudioBitrate < M4VIDEOEDITING_k9_2_KBPS)
            //                     return M4MCS_ERR_AUDIOBITRATE_TOO_LOW;
            //            }
            /*FlB 26.02.2009: add mp3 as mcs output format, add mp3 encoder*/
            else if( pC->AudioEncParams.Format == M4ENCODER_kMP3 )
            {
                if( pC->AudioEncParams.Frequency >= M4ENCODER_k32000Hz )
                {
                    /*Mpeg layer 1*/
                    if( pC->uiAudioBitrate > 320000 )
                        return M4MCS_ERR_AUDIOBITRATE_TOO_HIGH;

                    if( pC->uiAudioBitrate < 32000 )
                        return M4MCS_ERR_AUDIOBITRATE_TOO_LOW;
                }
                else if( pC->AudioEncParams.Frequency >= M4ENCODER_k16000Hz )
                {
                    /*Mpeg layer 2*/
                    if( pC->uiAudioBitrate > 160000 )
                        return M4MCS_ERR_AUDIOBITRATE_TOO_HIGH;

                    if( ( pC->uiAudioBitrate < 8000
                        && pC->AudioEncParams.ChannelNum == M4ENCODER_kMono)
                        || (pC->uiAudioBitrate < 16000
                        && pC->AudioEncParams.ChannelNum
                        == M4ENCODER_kStereo) )
                        return M4MCS_ERR_AUDIOBITRATE_TOO_LOW;
                }
                else if( pC->AudioEncParams.Frequency == M4ENCODER_k8000Hz
                    || pC->AudioEncParams.Frequency == M4ENCODER_k11025Hz
                    || pC->AudioEncParams.Frequency == M4ENCODER_k12000Hz )
                {
                    /*Mpeg layer 2.5*/
                    if( pC->uiAudioBitrate > 64000 )
                        return M4MCS_ERR_AUDIOBITRATE_TOO_HIGH;

                    if( ( pC->uiAudioBitrate < 8000
                        && pC->AudioEncParams.ChannelNum == M4ENCODER_kMono)
                        || (pC->uiAudioBitrate < 16000
                        && pC->AudioEncParams.ChannelNum
                        == M4ENCODER_kStereo) )
                        return M4MCS_ERR_AUDIOBITRATE_TOO_LOW;
                }
                else
                {
                    M4OSA_TRACE1_1("M4MCS_setEncodingParams: MP3 audio sampling frequency error\
                                   (%d)", pC->AudioEncParams.Frequency);
                    return M4ERR_PARAMETER;
                }
            }
            else
            {
                if( pC->uiAudioBitrate > M4VIDEOEDITING_k192_KBPS )
                    return M4MCS_ERR_AUDIOBITRATE_TOO_HIGH;

                if( pC->AudioEncParams.ChannelNum == M4ENCODER_kMono )
                {
                    if( pC->uiAudioBitrate < M4VIDEOEDITING_k16_KBPS )
                        return M4MCS_ERR_AUDIOBITRATE_TOO_LOW;
                }
                else
                {
                    if( pC->uiAudioBitrate < M4VIDEOEDITING_k32_KBPS )
                        return M4MCS_ERR_AUDIOBITRATE_TOO_LOW;
                }
            }
        }
    }
    else
    {
        /* NULL audio : copy input file bitrate */
        pC->uiAudioBitrate = pC->InputFileProperties.uiAudioBitrate;
    }

    /* Check video bitrate consistency */
    if( ( pC->novideo == M4OSA_FALSE)
        && (pC->EncodingVideoFormat != M4ENCODER_kNULL) )
    {
        if( pC->uiVideoBitrate != M4VIDEOEDITING_kUndefinedBitrate )
        {
            if( pC->uiVideoBitrate > M4VIDEOEDITING_k8_MBPS )
                return M4MCS_ERR_VIDEOBITRATE_TOO_HIGH;

            if( pC->uiVideoBitrate < M4VIDEOEDITING_k16_KBPS )
                return M4MCS_ERR_VIDEOBITRATE_TOO_LOW;
        }
    }
    else
    {
        /* NULL video : copy input file bitrate */
        pC->uiVideoBitrate = pC->InputFileProperties.uiVideoBitrate;
    }

    if( pRates->OutputVideoTimescale <= 30000
        && pRates->OutputVideoTimescale > 0 )
    {
        pC->outputVideoTimescale = pRates->OutputVideoTimescale;
    }

    /* Check file size */
    return M4MCS_intCheckMaxFileSize(pC);
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_getExtendedEncodingParams(M4MCS_Context pContext, M4MCS_EncodingParams* pRates)
 * @brief   Get the extended values of the encoding parameters
 * @note    Could be called after M4MCS_setEncodingParams.
 * @param   pContext           (IN) MCS context
 * @param   pRates             (OUT) Transcoding parameters
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 * @return  M4MCS_ERR_BEGIN_CUT_EQUALS_END_CUT: Encoding settings would produce a null duration
 *                                             clip = encoding is impossible
 ******************************************************************************
 */
M4OSA_ERR M4MCS_getExtendedEncodingParams( M4MCS_Context pContext,
                                          M4MCS_EncodingParams *pRates )
{
    M4MCS_InternalContext *pC = (M4MCS_InternalContext *)(pContext);

    M4OSA_Int32 minaudiobitrate;
    M4OSA_Int32 minvideobitrate;
    M4OSA_Int32 maxcombinedbitrate;

    M4OSA_Int32 calcbitrate;

    M4OSA_UInt32 maxduration;
    M4OSA_UInt32 calcduration;

    M4OSA_Bool fixed_audio = M4OSA_FALSE;
    M4OSA_Bool fixed_video = M4OSA_FALSE;

#ifdef M4MCS_SUPPORT_STILL_PICTURE

    if( pC->m_bIsStillPicture )
    {
        /**
        * Call the corresponding still picture MCS function*/
        return M4MCS_stillPicGetExtendedEncodingParams(pC, pRates);
    }

#endif /*M4MCS_SUPPORT_STILL_PICTURE*/

    pRates->OutputVideoBitrate =
        M4MCS_intGetNearestBitrate(pC->uiVideoBitrate, 0);
    pRates->OutputAudioBitrate =
        M4MCS_intGetNearestBitrate(pC->uiAudioBitrate, 0);
    pRates->BeginCutTime = pC->uiBeginCutTime;
    pRates->EndCutTime = pC->uiEndCutTime;
    pRates->OutputFileSize = pC->uiMaxFileSize;

    /**
    * Check state automaton */
    if( M4MCS_kState_SET != pC->State )
    {
        M4OSA_TRACE1_1("M4MCS_getExtendedEncodingParams(): Wrong State (%d),\
                       returning M4ERR_STATE", pC->State);
        return M4ERR_STATE;
    }

    /* Compute min audio bitrate */
    if( pC->noaudio )
    {
        fixed_audio = M4OSA_TRUE;
        pRates->OutputAudioBitrate = 0;
        minaudiobitrate = 0;
    }
    else if( pC->AudioEncParams.Format == M4ENCODER_kAudioNULL )
    {
        fixed_audio = M4OSA_TRUE;
        pRates->OutputAudioBitrate = pC->InputFileProperties.uiAudioBitrate;
        minaudiobitrate = pC->InputFileProperties.uiAudioBitrate;
    }
    else
    {
        if( pC->AudioEncParams.Format == M4ENCODER_kAMRNB )
        {
            fixed_audio = M4OSA_TRUE;
            pRates->OutputAudioBitrate = M4VIDEOEDITING_k12_2_KBPS;
            minaudiobitrate = M4VIDEOEDITING_k12_2_KBPS;
        }
        //EVRC
        //        if(pC->AudioEncParams.Format == M4ENCODER_kEVRC)
        //        {
        //            fixed_audio = M4OSA_TRUE;
        //            pRates->OutputAudioBitrate = M4VIDEOEDITING_k9_2_KBPS;
        //            minaudiobitrate = M4VIDEOEDITING_k9_2_KBPS;
        //        }
        /*FlB 26.02.2009: add mp3 as mcs output format*/
        else if( pC->AudioEncParams.Format == M4ENCODER_kMP3 )
        {
            minaudiobitrate =
                M4VIDEOEDITING_k32_KBPS; /*Default min audio bitrate for MPEG layer 1,
                                             for both mono and stereo channels*/
        }
        else
        {
            minaudiobitrate = (pC->AudioEncParams.ChannelNum == M4ENCODER_kMono)
                ? M4VIDEOEDITING_k16_KBPS : M4VIDEOEDITING_k32_KBPS;
        }
    }

    /* Check audio bitrate is in the correct range */
    if( fixed_audio == M4OSA_FALSE )
    {
        if( ( pC->uiAudioBitrate > 0)
            && (pRates->OutputAudioBitrate < minaudiobitrate) )
        {
            pRates->OutputAudioBitrate = minaudiobitrate;
        }

        if( pRates->OutputAudioBitrate > M4VIDEOEDITING_k96_KBPS )
        {
            pRates->OutputAudioBitrate = M4VIDEOEDITING_k96_KBPS;
        }
    }

    /* Compute min video bitrate */
    if( pC->novideo )
    {
        fixed_video = M4OSA_TRUE;
        pRates->OutputVideoBitrate = 0;
        minvideobitrate = 0;
    }
    else if( pC->EncodingVideoFormat == M4ENCODER_kNULL )
    {
        fixed_video = M4OSA_TRUE;
        pRates->OutputVideoBitrate = pC->InputFileProperties.uiVideoBitrate;
        minvideobitrate = pC->InputFileProperties.uiVideoBitrate;
    }
    else
    {
        minvideobitrate = M4VIDEOEDITING_k16_KBPS;
    }

    /* Check video bitrate is in the correct range */
    if( fixed_video == M4OSA_FALSE )
    {
        if( ( pC->uiVideoBitrate > 0)
            && (pRates->OutputVideoBitrate < minvideobitrate) )
        {
            pRates->OutputVideoBitrate = minvideobitrate;
        }
        /*+ New Encoder bitrates */
        if( pRates->OutputVideoBitrate > M4VIDEOEDITING_k8_MBPS )
        {
            pRates->OutputVideoBitrate = M4VIDEOEDITING_k8_MBPS;
        }
        /*- New Encoder bitrates */
    }

    /* Check cut times are in correct range */
    if( ( pRates->BeginCutTime >= pC->InputFileProperties.uiClipDuration)
        || (( pRates->BeginCutTime >= pRates->EndCutTime)
        && (pRates->EndCutTime > 0)) )
    {
        pRates->BeginCutTime = 0;
        pRates->EndCutTime = 0;
    }

    if( pRates->EndCutTime == 0 )
        calcduration =
        pC->InputFileProperties.uiClipDuration - pRates->BeginCutTime;
    else
        calcduration = pRates->EndCutTime - pRates->BeginCutTime;

    /* priority 1 : max file size */
    if( pRates->OutputFileSize == 0 )
    {
        /* we can put maximum values for all undefined parameters */
        if( pRates->EndCutTime == 0 )
        {
            pRates->EndCutTime = pC->InputFileProperties.uiClipDuration;
        }

        if( ( pRates->OutputAudioBitrate == M4VIDEOEDITING_kUndefinedBitrate)
            && (fixed_audio == M4OSA_FALSE) )
        {
            pRates->OutputAudioBitrate = M4VIDEOEDITING_k96_KBPS;
        }

        if( ( pRates->OutputVideoBitrate == M4VIDEOEDITING_kUndefinedBitrate)
            && (fixed_video == M4OSA_FALSE) )
        {
            /*+ New Encoder bitrates */
            pRates->OutputVideoBitrate = M4VIDEOEDITING_k8_MBPS;
            /*- New Encoder bitrates */
        }
    }
    else
    {
        /* compute max duration */
        maxduration = (M4OSA_UInt32)(pRates->OutputFileSize
            / M4MCS_MOOV_OVER_FILESIZE_RATIO
            / (minvideobitrate + minaudiobitrate) * 8000.0);

        if( maxduration
            + pRates->BeginCutTime > pC->InputFileProperties.uiClipDuration )
        {
            maxduration =
                pC->InputFileProperties.uiClipDuration - pRates->BeginCutTime;
        }

        /* priority 2 : cut times */
        if( ( pRates->BeginCutTime > 0) || (pRates->EndCutTime > 0) )
        {
            if( calcduration > maxduration )
            {
                calcduration = maxduration;
            }

            if( calcduration == 0 )
            {
                return M4MCS_ERR_BEGIN_CUT_EQUALS_END_CUT;
            }

            maxcombinedbitrate = (M4OSA_UInt32)(pRates->OutputFileSize
                / M4MCS_MOOV_OVER_FILESIZE_RATIO / (calcduration / 8000.0));

            /* audio and video bitrates */
            if( ( pRates->OutputAudioBitrate
                == M4VIDEOEDITING_kUndefinedBitrate)
                && (pRates->OutputVideoBitrate
                == M4VIDEOEDITING_kUndefinedBitrate) )
            {
                /* set audio = 1/3 and video = 2/3 */
                if( fixed_audio == M4OSA_FALSE )
                {
                    if( pC->novideo )
                        pRates->OutputAudioBitrate =
                        M4MCS_intGetNearestBitrate(maxcombinedbitrate, 0);
                    else
                        pRates->OutputAudioBitrate =
                        M4MCS_intGetNearestBitrate(maxcombinedbitrate / 3,
                        0);

                    if( pRates->OutputAudioBitrate < minaudiobitrate )
                        pRates->OutputAudioBitrate = minaudiobitrate;

                    if( pRates->OutputAudioBitrate > M4VIDEOEDITING_k96_KBPS )
                        pRates->OutputAudioBitrate = M4VIDEOEDITING_k96_KBPS;
                }

                if( fixed_video == M4OSA_FALSE )
                {
                    pRates->OutputVideoBitrate =
                        M4MCS_intGetNearestBitrate(maxcombinedbitrate
                        - pRates->OutputAudioBitrate, 0);

                    if( pRates->OutputVideoBitrate < minvideobitrate )
                        pRates->OutputVideoBitrate = minvideobitrate;

                    if( pRates->OutputVideoBitrate > M4VIDEOEDITING_k8_MBPS )
                        pRates->OutputVideoBitrate =
                        M4VIDEOEDITING_k8_MBPS; /*+ New Encoder
                                                bitrates */
                }
            }
            else
            {
                /* priority 3 : audio bitrate */
                if( pRates->OutputAudioBitrate
                    != M4VIDEOEDITING_kUndefinedBitrate )
                {
                    while( ( fixed_audio == M4OSA_FALSE)
                        && (pRates->OutputAudioBitrate >= minaudiobitrate)
                        && (pRates->OutputAudioBitrate
                        + minvideobitrate > maxcombinedbitrate) )
                    {
                        pRates->OutputAudioBitrate =
                            M4MCS_intGetNearestBitrate(
                            pRates->OutputAudioBitrate, -1);
                    }

                    if( ( fixed_audio == M4OSA_FALSE)
                        && (pRates->OutputAudioBitrate < minaudiobitrate) )
                    {
                        pRates->OutputAudioBitrate = minaudiobitrate;
                    }

                    calcbitrate = M4MCS_intGetNearestBitrate(
                                    maxcombinedbitrate
                                    - pRates->OutputAudioBitrate, 0);

                    if( calcbitrate < minvideobitrate )
                        calcbitrate = minvideobitrate;

                    if( calcbitrate > M4VIDEOEDITING_k8_MBPS )
                        calcbitrate = M4VIDEOEDITING_k8_MBPS;

                    if( ( fixed_video == M4OSA_FALSE)
                        && (( pRates->OutputVideoBitrate
                        == M4VIDEOEDITING_kUndefinedBitrate)
                        || (pRates->OutputVideoBitrate > calcbitrate)) )
                    {
                        pRates->OutputVideoBitrate = calcbitrate;
                    }
                }
                else
                {
                    /* priority 4 : video bitrate */
                    if( pRates->OutputVideoBitrate
                        != M4VIDEOEDITING_kUndefinedBitrate )
                    {
                        while( ( fixed_video == M4OSA_FALSE)
                            && (pRates->OutputVideoBitrate >= minvideobitrate)
                            && (pRates->OutputVideoBitrate
                            + minaudiobitrate > maxcombinedbitrate) )
                        {
                            pRates->OutputVideoBitrate =
                                M4MCS_intGetNearestBitrate(
                                pRates->OutputVideoBitrate, -1);
                        }

                        if( ( fixed_video == M4OSA_FALSE)
                            && (pRates->OutputVideoBitrate < minvideobitrate) )
                        {
                            pRates->OutputVideoBitrate = minvideobitrate;
                        }

                        calcbitrate =
                            M4MCS_intGetNearestBitrate(maxcombinedbitrate
                            - pRates->OutputVideoBitrate, 0);

                        if( calcbitrate < minaudiobitrate )
                            calcbitrate = minaudiobitrate;

                        if( calcbitrate > M4VIDEOEDITING_k96_KBPS )
                            calcbitrate = M4VIDEOEDITING_k96_KBPS;

                        if( ( fixed_audio == M4OSA_FALSE)
                            && (( pRates->OutputAudioBitrate
                            == M4VIDEOEDITING_kUndefinedBitrate)
                            || (pRates->OutputAudioBitrate > calcbitrate)) )
                        {
                            pRates->OutputAudioBitrate = calcbitrate;
                        }
                    }
                }
            }
        }
        else
        {
            /* priority 3 : audio bitrate */
            if( pRates->OutputAudioBitrate != M4VIDEOEDITING_kUndefinedBitrate )
            {
                /* priority 4 : video bitrate */
                if( pRates->OutputVideoBitrate
                    != M4VIDEOEDITING_kUndefinedBitrate )
                {
                    /* compute max duration */
                    maxduration = (M4OSA_UInt32)(pRates->OutputFileSize
                        / M4MCS_MOOV_OVER_FILESIZE_RATIO
                        / (pRates->OutputVideoBitrate
                        + pRates->OutputAudioBitrate) * 8000.0);

                    if( maxduration + pRates->BeginCutTime
                        > pC->InputFileProperties.uiClipDuration )
                    {
                        maxduration = pC->InputFileProperties.uiClipDuration
                            - pRates->BeginCutTime;
                    }

                    if( calcduration > maxduration )
                    {
                        calcduration = maxduration;
                    }

                    if( calcduration == 0 )
                    {
                        return M4MCS_ERR_BEGIN_CUT_EQUALS_END_CUT;
                    }
                }
                else
                {
                    /* start with min video bitrate */
                    pRates->OutputVideoBitrate = minvideobitrate;

                    /* compute max duration */
                    maxduration = (M4OSA_UInt32)(pRates->OutputFileSize
                        / M4MCS_MOOV_OVER_FILESIZE_RATIO
                        / (pRates->OutputVideoBitrate
                        + pRates->OutputAudioBitrate) * 8000.0);

                    if( maxduration + pRates->BeginCutTime
                        > pC->InputFileProperties.uiClipDuration )
                    {
                        maxduration = pC->InputFileProperties.uiClipDuration
                            - pRates->BeginCutTime;
                    }

                    if( calcduration > maxduration )
                    {
                        calcduration = maxduration;
                    }

                    if( calcduration == 0 )
                    {
                        return M4MCS_ERR_BEGIN_CUT_EQUALS_END_CUT;
                    }

                    /* search max possible video bitrate */
                    maxcombinedbitrate = (M4OSA_UInt32)(pRates->OutputFileSize
                        / M4MCS_MOOV_OVER_FILESIZE_RATIO
                        / (calcduration / 8000.0));

                    while( ( fixed_video == M4OSA_FALSE)
                        && (pRates->OutputVideoBitrate
                        < M4VIDEOEDITING_k8_MBPS) ) /*+ New Encoder bitrates */
                    {
                        calcbitrate = M4MCS_intGetNearestBitrate(
                            pRates->OutputVideoBitrate, +1);

                        if( calcbitrate
                            + pRates->OutputAudioBitrate <= maxcombinedbitrate )
                            pRates->OutputVideoBitrate = calcbitrate;
                        else
                            break;
                    }
                }
            }
            else
            {
                /* priority 4 : video bitrate */
                if( pRates->OutputVideoBitrate
                    != M4VIDEOEDITING_kUndefinedBitrate )
                {
                    /* start with min audio bitrate */
                    pRates->OutputAudioBitrate = minaudiobitrate;

                    /* compute max duration */
                    maxduration = (M4OSA_UInt32)(pRates->OutputFileSize
                        / M4MCS_MOOV_OVER_FILESIZE_RATIO
                        / (pRates->OutputVideoBitrate
                        + pRates->OutputAudioBitrate) * 8000.0);

                    if( maxduration + pRates->BeginCutTime
                        > pC->InputFileProperties.uiClipDuration )
                    {
                        maxduration = pC->InputFileProperties.uiClipDuration
                            - pRates->BeginCutTime;
                    }

                    if( calcduration > maxduration )
                    {
                        calcduration = maxduration;
                    }

                    if( calcduration == 0 )
                    {
                        return M4MCS_ERR_BEGIN_CUT_EQUALS_END_CUT;
                    }

                    /* search max possible audio bitrate */
                    maxcombinedbitrate = (M4OSA_UInt32)(pRates->OutputFileSize
                        / M4MCS_MOOV_OVER_FILESIZE_RATIO
                        / (calcduration / 8000.0));

                    while( ( fixed_audio == M4OSA_FALSE)
                        && (pRates->OutputAudioBitrate
                        < M4VIDEOEDITING_k96_KBPS) )
                    {
                        calcbitrate = M4MCS_intGetNearestBitrate(
                            pRates->OutputAudioBitrate, +1);

                        if( calcbitrate
                            + pRates->OutputVideoBitrate <= maxcombinedbitrate )
                            pRates->OutputAudioBitrate = calcbitrate;
                        else
                            break;
                    }
                }
                else
                {
                    /* compute max duration */
                    maxduration = (M4OSA_UInt32)(pRates->OutputFileSize
                        / M4MCS_MOOV_OVER_FILESIZE_RATIO
                        / (minvideobitrate + minaudiobitrate) * 8000.0);

                    if( maxduration + pRates->BeginCutTime
                        > pC->InputFileProperties.uiClipDuration )
                    {
                        maxduration = pC->InputFileProperties.uiClipDuration
                            - pRates->BeginCutTime;
                    }

                    if( calcduration > maxduration )
                    {
                        calcduration = maxduration;
                    }

                    if( calcduration == 0 )
                    {
                        return M4MCS_ERR_BEGIN_CUT_EQUALS_END_CUT;
                    }

                    /* set audio = 1/3 and video = 2/3 */
                    maxcombinedbitrate = (M4OSA_UInt32)(pRates->OutputFileSize
                        / M4MCS_MOOV_OVER_FILESIZE_RATIO
                        / (calcduration / 8000.0));

                    if( fixed_audio == M4OSA_FALSE )
                    {
                        if( pC->novideo )
                            pRates->OutputAudioBitrate =
                            M4MCS_intGetNearestBitrate(maxcombinedbitrate,
                            0);
                        else
                            pRates->OutputAudioBitrate =
                            M4MCS_intGetNearestBitrate(maxcombinedbitrate
                            / 3, 0);

                        if( pRates->OutputAudioBitrate < minaudiobitrate )
                            pRates->OutputAudioBitrate = minaudiobitrate;

                        if( pRates->OutputAudioBitrate
                        > M4VIDEOEDITING_k96_KBPS )
                        pRates->OutputAudioBitrate =
                        M4VIDEOEDITING_k96_KBPS;
                    }

                    if( fixed_video == M4OSA_FALSE )
                    {
                        pRates->OutputVideoBitrate =
                            M4MCS_intGetNearestBitrate(maxcombinedbitrate
                            - pRates->OutputAudioBitrate, 0);

                        if( pRates->OutputVideoBitrate < minvideobitrate )
                            pRates->OutputVideoBitrate = minvideobitrate;

                        if( pRates->OutputVideoBitrate
                        > M4VIDEOEDITING_k8_MBPS )
                        pRates->OutputVideoBitrate =
                        M4VIDEOEDITING_k8_MBPS; /*+ New Encoder
                                                bitrates */
                    }
                }
            }
        }
    }

    /* recompute max duration with final bitrates */
    if( pRates->OutputFileSize > 0 )
    {
        maxduration = (M4OSA_UInt32)(pRates->OutputFileSize
            / M4MCS_MOOV_OVER_FILESIZE_RATIO
            / (pRates->OutputVideoBitrate + pRates->OutputAudioBitrate)
            * 8000.0);
    }
    else
    {
        maxduration = pC->InputFileProperties.uiClipDuration;
    }

    if( maxduration
        + pRates->BeginCutTime > pC->InputFileProperties.uiClipDuration )
    {
        maxduration =
            pC->InputFileProperties.uiClipDuration - pRates->BeginCutTime;
    }

    if( pRates->EndCutTime == 0 )
    {
        pRates->EndCutTime = pRates->BeginCutTime + maxduration;
    }
    else
    {
        calcduration = pRates->EndCutTime - pRates->BeginCutTime;

        if( calcduration > maxduration )
        {
            pRates->EndCutTime = pRates->BeginCutTime + maxduration;
        }
    }

    /* Should never happen : constraints are too strong */
    if( pRates->EndCutTime == pRates->BeginCutTime )
    {
        return M4MCS_ERR_BEGIN_CUT_EQUALS_END_CUT;
    }

    /* estimated resulting file size */
    pRates->OutputFileSize = (M4OSA_UInt32)(M4MCS_MOOV_OVER_FILESIZE_RATIO
        * (pRates->OutputVideoBitrate + pRates->OutputAudioBitrate)
        * (( pRates->EndCutTime - pRates->BeginCutTime) / 8000.0));

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_checkParamsAndStart(M4MCS_Context pContext)
 * @brief   Check parameters to start
 * @note
 * @param   pContext           (IN) MCS context
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for
 *                              this function to be called
 * @return  M4MCS_ERR_AUDIOBITRATE_TOO_HIGH:
 *                              Audio bitrate too high (we limit to 96 kbps)
 * @return  M4MCS_ERR_AUDIOBITRATE_TOO_LOW:
 *                              Audio bitrate is too low (16 kbps min for aac,
 *                              12.2 for amr, 8 for mp3)
 * @return  M4MCS_ERR_BEGIN_CUT_EQUALS_END_CUT:
 *                              Begin cut and End cut are equals
 * @return  M4MCS_ERR_BEGIN_CUT_LARGER_THAN_DURATION:
 *                              Begin cut time is larger than the input
 *                              clip duration
 * @return  M4MCS_ERR_END_CUT_SMALLER_THAN_BEGIN_CUT:
 *                              End cut time is smaller than begin cut time
 * @return  M4MCS_ERR_MAXFILESIZE_TOO_SMALL:
 *                              Not enough space to store whole output
 *                              file at given bitrates
 * @return  M4MCS_ERR_VIDEOBITRATE_TOO_HIGH:
 *                              Video bitrate too high (we limit to 800 kbps)
 * @return  M4MCS_ERR_VIDEOBITRATE_TOO_LOW:
 *                              Video bitrate too low
 ******************************************************************************
 */
M4OSA_ERR M4MCS_checkParamsAndStart( M4MCS_Context pContext )
{
    M4MCS_InternalContext *pC = (M4MCS_InternalContext *)(pContext);
    M4MCS_EncodingParams VerifyRates;
    M4OSA_ERR err;

    /**
    * Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
        "M4MCS_checkParamsAndStart: pContext is M4OSA_NULL");

#ifdef M4MCS_SUPPORT_STILL_PICTURE

    if( pC->m_bIsStillPicture )
    {
        /**
        * Call the corresponding still picture MCS function*/
        return M4MCS_stillPicCheckParamsAndStart(pC);
    }

#endif /*M4MCS_SUPPORT_STILL_PICTURE*/

    /**
    * Check state automaton */

    if( M4MCS_kState_SET != pC->State )
    {
        M4OSA_TRACE1_1(
            "M4MCS_checkParamsAndStart(): Wrong State (%d), returning M4ERR_STATE",
            pC->State);
        return M4ERR_STATE;
    }

    /* Audio bitrate should not stay undefined at this point */
    if( ( pC->noaudio == M4OSA_FALSE)
        && (pC->AudioEncParams.Format != M4ENCODER_kAudioNULL)
        && (pC->uiAudioBitrate == M4VIDEOEDITING_kUndefinedBitrate) )
    {
        M4OSA_TRACE1_0("M4MCS_checkParamsAndStart : undefined audio bitrate");
        return M4MCS_ERR_AUDIOBITRATE_TOO_LOW;
    }

    /* Video bitrate should not stay undefined at this point */
    if( ( pC->novideo == M4OSA_FALSE)
        && (pC->EncodingVideoFormat != M4ENCODER_kNULL)
        && (pC->uiVideoBitrate == M4VIDEOEDITING_kUndefinedBitrate) )
    {
        M4OSA_TRACE1_0("M4MCS_checkParamsAndStart : undefined video bitrate");
        return M4MCS_ERR_VIDEOBITRATE_TOO_LOW;
    }

    /* Set end cut time if necessary (not an error) */
    if( pC->uiEndCutTime == 0 )
    {
        pC->uiEndCutTime = pC->InputFileProperties.uiClipDuration;
    }

    /* Force a re-set to check validity of parameters */
    VerifyRates.OutputVideoBitrate = pC->uiVideoBitrate;
    VerifyRates.OutputAudioBitrate = pC->uiAudioBitrate;
    VerifyRates.BeginCutTime = pC->uiBeginCutTime;
    VerifyRates.EndCutTime = pC->uiEndCutTime;
    VerifyRates.OutputFileSize = pC->uiMaxFileSize;
    VerifyRates.OutputVideoTimescale = pC->outputVideoTimescale;

    err = M4MCS_setEncodingParams(pContext, &VerifyRates);

    /**
    * Check parameters consistency */
    if( err != M4NO_ERROR )
    {
        M4OSA_TRACE1_0("M4MCS_checkParamsAndStart : invalid parameter found");
        return err;
    }

    /**
    * All is OK : update state automaton */
    pC->uiEncVideoBitrate = pC->uiVideoBitrate;
    pC->AudioEncParams.Bitrate = pC->uiAudioBitrate;

#ifdef M4MCS_WITH_FAST_OPEN
    /**
    * Remake the open if it was done in fast mode */

    if( M4OSA_TRUE == pC->bFileOpenedInFastMode )
    {
        /* Close the file opened in fast mode */
        M4MCS_intCleanUp_ReadersDecoders(pC);

        pC->State = M4MCS_kState_CREATED;

        /* Reopen it in normal mode */
        err = M4MCS_open(pContext, pC->pInputFile, pC->InputFileType,
            pC->pOutputFile, pC->pTemporaryFile);

        if( err != M4NO_ERROR )
        {
            M4OSA_TRACE1_1(
                "M4MCS_checkParamsAndStart : M4MCS_Open returns 0x%x", err);
            return err;
        }
    }

#endif /* M4MCS_WITH_FAST_OPEN */

    pC->State = M4MCS_kState_READY;

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intStepSet(M4MCS_InternalContext* pC)
 ******************************************************************************
 */
static M4OSA_ERR M4MCS_intStepSet( M4MCS_InternalContext *pC )
{
    M4OSA_ERR err;
    M4ENCODER_Header *encHeader;

    /**
    * Prepare the video decoder */
    err = M4MCS_intPrepareVideoDecoder(pC);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intStepSet(): M4MCS_intPrepareVideoDecoder() returns 0x%x",
            err);
        return err;
    }

    if( ( pC->InputFileProperties.VideoStreamType == M4VIDEOEDITING_kH264)
        && (pC->EncodingVideoFormat == M4ENCODER_kNULL) )
    {
        pC->bH264Trim = M4OSA_TRUE;
    }

    /**
    * Prepare the video encoder */
    err = M4MCS_intPrepareVideoEncoder(pC);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intStepSet(): M4MCS_intPrepareVideoEncoder() returns 0x%x",
            err);
        return err;
    }

    if( ( pC->uiBeginCutTime != 0)
        && (pC->InputFileProperties.VideoStreamType == M4VIDEOEDITING_kH264)
        && (pC->EncodingVideoFormat == M4ENCODER_kNULL) )
    {

        err = pC->pVideoEncoderGlobalFcts->pFctSetOption(pC->pViEncCtxt,
            M4ENCODER_kOptionID_H264ProcessNALUContext,
            (M4OSA_DataOption)pC->m_pInstance);

        if( err != M4NO_ERROR )
        {
            M4OSA_TRACE1_1("M4MCS_intStetSet :pFctSetOption failed  (err 0x%x)",
                err);
            return err;
        }

        err = pC->pVideoEncoderGlobalFcts->pFctSetOption(pC->pViEncCtxt,
            M4ENCODER_kOptionID_SetH264ProcessNALUfctsPtr,
            (M4OSA_DataOption) &H264MCS_ProcessEncodedNALU);

        if( err != M4NO_ERROR )
        {
            M4OSA_TRACE1_1("M4MCS_intStetSet :pFctSetOption failed  (err 0x%x)",
                err);
            return err;
        }

        err = pC->pVideoEncoderGlobalFcts->pFctGetOption(pC->pViEncCtxt,
            M4ENCODER_kOptionID_EncoderHeader,
            (M4OSA_DataOption) &encHeader);

        if( ( M4NO_ERROR != err) || (M4OSA_NULL == encHeader->pBuf) )
        {
            M4OSA_TRACE1_1(
                "M4MCS_close: failed to get the encoder header (err 0x%x)",
                err);
            /**< no return here, we still have stuff to deallocate after close, even if it fails.*/
        }
        else
        {
            // Handle DSI first bits
#define SPS_START_POS 6

            pC->m_pInstance->m_encoderSPSSize =
                ( encHeader->pBuf[SPS_START_POS] << 8)
                + encHeader->pBuf[SPS_START_POS + 1];
            pC->m_pInstance->m_pEncoderSPS =
                (M4OSA_UInt8 *)(encHeader->pBuf) + SPS_START_POS + 2;

            pC->m_pInstance->m_encoderPPSSize =
                ( encHeader->pBuf[SPS_START_POS + 3
                + pC->m_pInstance->m_encoderSPSSize] << 8)
                + encHeader->pBuf[SPS_START_POS + 4
                + pC->m_pInstance->m_encoderSPSSize];
            pC->m_pInstance->m_pEncoderPPS = (M4OSA_UInt8 *)encHeader->pBuf + SPS_START_POS + 5
                + pC->m_pInstance->m_encoderSPSSize;

            /* Check the DSI integrity */
            if( encHeader->Size != (pC->m_pInstance->m_encoderSPSSize
                + pC->m_pInstance->m_encoderPPSSize + 5 + SPS_START_POS) )
            {
                M4OSA_TRACE1_3(
                    "!!! M4MCS_intStepSet ERROR : invalid SPS / PPS %d %d %d",
                    encHeader->Size, pC->m_pInstance->m_encoderSPSSize,
                    pC->m_pInstance->m_encoderPPSSize);
                return M4ERR_PARAMETER;
            }
        }
    }

    /**
    * Prepare audio processing */
    err = M4MCS_intPrepareAudioProcessing(pC);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intStepSet(): M4MCS_intPrepareAudioProcessing() returns 0x%x",
            err);
        return err;
    }

    /**
    * Prepare the writer */
    err = M4MCS_intPrepareWriter(pC);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intStepSet(): M4MCS_intPrepareWriter() returns 0x%x", err);
        return err;
    }

    /**
    * Jump the audio stream to the begin cut time (all AUs are RAP)
    * Must be done after the 3gpp writer init, because it may write the first
    * audio AU in some cases */
    err = M4MCS_intPrepareAudioBeginCut(pC);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intStepSet(): M4MCS_intPrepareAudioBeginCut() returns 0x%x",
            err);
        return err;
    }

    /**
    * Update state automaton */
    if( 0 == pC->uiBeginCutTime )
    {
        pC->dViDecStartingCts = 0.0;
        /**
        * No begin cut, do the encoding */
        pC->State = M4MCS_kState_PROCESSING;
    }
    else
    {
        /**
        * Remember that we must start the decode/encode process at the begin cut time */
        pC->dViDecStartingCts = (M4OSA_Double)pC->uiBeginCutTime;

        /**
        * Jumping */
        pC->State = M4MCS_kState_BEGINVIDEOJUMP;
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4MCS_intStepSet(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intPrepareVideoDecoder(M4MCS_InternalContext* pC);
 * @brief    Prepare the video decoder.
 * @param    pC          (IN) MCS private context
 * @return   M4NO_ERROR  No error
 * @return   M4MCS_ERR_H263_PROFILE_NOT_SUPPORTED
 * @return   Any error returned by an underlaying module
 ******************************************************************************
 */
static M4OSA_ERR M4MCS_intPrepareVideoDecoder( M4MCS_InternalContext *pC )
{
    M4OSA_ERR err;
    M4OSA_Void *decoderUserData;
    M4DECODER_OutputFilter FilterOption;

    if( pC->novideo )
        return M4NO_ERROR;

    /**
    * Create the decoder, if it has not been created yet (to get video properties for example) */
    if( M4OSA_NULL == pC->pViDecCtxt )
    {
#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS

        decoderUserData = pC->m_pCurrentVideoDecoderUserData;

#else

        decoderUserData = M4OSA_NULL;

#endif /* M4VSS_ENABLE_EXTERNAL_DECODERS ? */

        err = pC->m_pVideoDecoder->m_pFctCreate(&pC->pViDecCtxt,
            &pC->pReaderVideoStream->m_basicProperties, pC->m_pReader,
            pC->m_pReaderDataIt, &pC->ReaderVideoAU, decoderUserData);

        if( (M4OSA_UInt32)(M4ERR_DECODER_H263_PROFILE_NOT_SUPPORTED) == err )
        {
            /**
            * Our decoder is not compatible with H263 profile other than 0.
            * So it returns this internal error code.
            * We translate it to our own error code */
            M4OSA_TRACE1_0("M4MCS_intPrepareVideoDecoder:\
                           returning M4MCS_ERR_H263_PROFILE_NOT_SUPPORTED");
            return M4MCS_ERR_H263_PROFILE_NOT_SUPPORTED;
        }
        else if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1("M4MCS_intPrepareVideoDecoder:\
                           m_pVideoDecoder->m_pFctCreate returns 0x%x", err);
            return err;
        }

        if( M4VIDEOEDITING_kH264 == pC->InputFileProperties.VideoStreamType )
        {
            FilterOption.m_pFilterFunction =
                (M4OSA_Void *) &M4VIFI_ResizeBilinearYUV420toYUV420;
            FilterOption.m_pFilterUserData = M4OSA_NULL;
            err = pC->m_pVideoDecoder->m_pFctSetOption(pC->pViDecCtxt,
                M4DECODER_kOptionID_OutputFilter,
                (M4OSA_DataOption) &FilterOption);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1("M4MCS_intPrepareVideoDecoder:\
                               m_pVideoDecoder->m_pFctSetOption returns 0x%x", err);
                return err;
            }
        }
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4MCS_intPrepareVideoDecoder(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intPrepareVideoEncoder(M4MCS_InternalContext* pC);
 * @brief    Prepare the video encoder.
 * @param    pC          (IN) MCS private context
 * @return   M4NO_ERROR  No error
 * @return   Any error returned by an underlaying module
 ******************************************************************************
 */
static M4OSA_ERR M4MCS_intPrepareVideoEncoder( M4MCS_InternalContext *pC )
{
    M4OSA_ERR err;
    M4ENCODER_AdvancedParams EncParams; /**< Encoder advanced parameters */
    M4ENCODER_Params EncParams1;
    M4OSA_Double dFrameRate;            /**< tmp variable */

    if( pC->novideo )
        return M4NO_ERROR;

    if( pC->EncodingVideoFormat == M4ENCODER_kNULL )
    {
        /* Approximative cts increment */
        pC->dCtsIncrement = 1000.0 / pC->pReaderVideoStream->m_averageFrameRate;

        if( pC->uiBeginCutTime == 0 )
        {
            M4OSA_TRACE3_0(
                "M4MCS_intPrepareVideoEncoder(): Null encoding, do nothing.");
            return M4NO_ERROR;
        }
        else
        {
            M4OSA_TRACE3_0(
                "M4MCS_intPrepareVideoEncoder(): Null encoding, I-frame defaults.");

            /* Set useful parameters to encode the first I-frame */
            EncParams.InputFormat = M4ENCODER_kIYUV420;
            EncParams.videoProfile = pC->encodingVideoProfile;
            EncParams.videoLevel= pC->encodingVideoLevel;

            switch( pC->InputFileProperties.VideoStreamType )
            {
                case M4VIDEOEDITING_kH263:
                    EncParams.Format = M4ENCODER_kH263;
                    break;

                case M4VIDEOEDITING_kMPEG4:
                    EncParams.Format = M4ENCODER_kMPEG4;
                    break;

                case M4VIDEOEDITING_kH264:
                    EncParams.Format = M4ENCODER_kH264;
                    break;

                default:
                    M4OSA_TRACE1_1("M4MCS_intPrepareVideoEncoder: unknown encoding video format\
                                   (%d), returning M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED",
                                   pC->InputFileProperties.VideoStreamType);
                    return M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED;
            }

            EncParams.FrameWidth = pC->EncodingWidth;
            EncParams.FrameHeight = pC->EncodingHeight;
            EncParams.Bitrate = pC->uiEncVideoBitrate;
            EncParams.bInternalRegulation =
                M4OSA_FALSE; /* do not constrain the I-frame */
            EncParams.FrameRate = pC->EncodingVideoFramerate;

            /* Other encoding settings (quite all dummy...) */
            EncParams.uiHorizontalSearchRange = 0;    /* use default */
            EncParams.uiVerticalSearchRange = 0;      /* use default */
            EncParams.bErrorResilience = M4OSA_FALSE; /* no error resilience */
            EncParams.uiIVopPeriod = 0;               /* use default */
            EncParams.uiMotionEstimationTools =
                0; /* M4V_MOTION_EST_TOOLS_ALL */
            EncParams.bAcPrediction = M4OSA_TRUE;     /* use AC prediction */
            EncParams.uiStartingQuantizerValue = 5;   /* initial QP = 5 */
            EncParams.bDataPartitioning =
                M4OSA_FALSE; /* no data partitioning */

            /* Rate factor */
            EncParams.uiTimeScale = pC->InputFileProperties.uiVideoTimeScale;
            EncParams.uiRateFactor = 1;
        }
    }
    else
    {
        M4OSA_TRACE3_0(
            "M4MCS_intPrepareVideoEncoder(): Normal encoding, set full config.");

        /**
        * Set encoder shell parameters according to MCS settings */
        EncParams.Format = pC->EncodingVideoFormat;
        EncParams.InputFormat = M4ENCODER_kIYUV420;
        EncParams.videoProfile = pC->encodingVideoProfile;
        EncParams.videoLevel= pC->encodingVideoLevel;

        /**
        * Video frame size */
        EncParams.FrameWidth = pC->EncodingWidth;
        EncParams.FrameHeight = pC->EncodingHeight;

        /**
        * Video bitrate has been previously computed */
        EncParams.Bitrate = pC->uiEncVideoBitrate;

        /**
        * MCS use the "true" core internal bitrate regulation */
        EncParams.bInternalRegulation = M4OSA_TRUE;

        /**
        * Other encoder settings */

        EncParams.uiHorizontalSearchRange = 0;    /* use default */
        EncParams.uiVerticalSearchRange = 0;      /* use default */
        EncParams.bErrorResilience = M4OSA_FALSE; /* no error resilience */
        EncParams.uiIVopPeriod = 0;               /* use default */
        EncParams.uiMotionEstimationTools =
            0; /* M4V_MOTION_EST_TOOLS_ALL */
        EncParams.bAcPrediction = M4OSA_TRUE;     /* use AC prediction */
        EncParams.uiStartingQuantizerValue = 10;  /* initial QP = 10 */
        EncParams.bDataPartitioning =
            M4OSA_FALSE; /* no data partitioning */


        /**
        * Video encoder frame rate and rate factor */
        EncParams.FrameRate = pC->EncodingVideoFramerate;
        EncParams.uiTimeScale = pC->outputVideoTimescale;

        switch( pC->EncodingVideoFramerate )
        {
            case M4ENCODER_k5_FPS:
                dFrameRate = 5.0;
                break;

            case M4ENCODER_k7_5_FPS:
                dFrameRate = 7.5;
                break;

            case M4ENCODER_k10_FPS:
                dFrameRate = 10.0;
                break;

            case M4ENCODER_k12_5_FPS:
                dFrameRate = 12.5;
                break;

            case M4ENCODER_k15_FPS:
                dFrameRate = 15.0;
                break;

            case M4ENCODER_k20_FPS: /**< MPEG-4 only */
                dFrameRate = 20.0;
                break;

            case M4ENCODER_k25_FPS: /**< MPEG-4 only */
                dFrameRate = 25.0;
                break;

            case M4ENCODER_k30_FPS:
                dFrameRate = 30.0;
                break;

            default:
                M4OSA_TRACE1_1(
                    "M4MCS_intPrepareVideoEncoder: unknown encoding video frame rate\
                    (0x%x), returning M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FRAME_RATE",
                    pC->EncodingVideoFramerate);
                return M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FRAME_RATE;
        }

        /**
        * Compute the number of milliseconds between two frames */
        if( M4ENCODER_kH263 == EncParams.Format )
        {
            pC->dCtsIncrement = 1001.0 / dFrameRate;
        }
        else /**< MPEG4 or H.264 */
        {
            pC->dCtsIncrement = 1000.0 / dFrameRate;
        }
    }

    /**
     * Limit the video bitrate according to encoder profile
     * and level */
    err = M4MCS_intLimitBitratePerCodecProfileLevel(&EncParams);
    if (M4NO_ERROR != err) {
        M4OSA_TRACE1_1(
            "M4MCS_intPrepareVideoEncoder: limit bitrate returned err \
             0x%x", err);
        return err;
    }

    /**
    * Create video encoder */
    err = pC->pVideoEncoderGlobalFcts->pFctInit(&pC->pViEncCtxt,
        pC->pWriterDataFcts, \
        M4MCS_intApplyVPP, pC, pC->pCurrentVideoEncoderExternalAPI, \
        pC->pCurrentVideoEncoderUserData);

    /**< We put the MCS context in place of the VPP context */
    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intPrepareVideoEncoder: EncoderInt->pFctInit returns 0x%x",
            err);
        return err;
    }

    pC->encoderState = M4MCS_kEncoderClosed;

    if( M4OSA_TRUE == pC->bH264Trim )
        //if((M4ENCODER_kNULL == pC->EncodingVideoFormat)
        //    && (M4VIDEOEDITING_kH264 == pC->InputFileProperties.VideoStreamType))
    {
        EncParams1.InputFormat = EncParams.InputFormat;
        //EncParams1.InputFrameWidth = EncParams.InputFrameWidth;
        //EncParams1.InputFrameHeight = EncParams.InputFrameHeight;
        EncParams1.FrameWidth = EncParams.FrameWidth;
        EncParams1.FrameHeight = EncParams.FrameHeight;
        EncParams1.videoProfile= EncParams.videoProfile;
        EncParams1.videoLevel= EncParams.videoLevel;
        EncParams1.Bitrate = EncParams.Bitrate;
        EncParams1.FrameRate = EncParams.FrameRate;
        EncParams1.Format = M4ENCODER_kH264; //EncParams.Format;
        M4OSA_TRACE1_2("mcs encoder open profile :%d, level %d",
            EncParams1.videoProfile, EncParams1.videoLevel);
        err = pC->pVideoEncoderGlobalFcts->pFctOpen(pC->pViEncCtxt,
            &pC->WriterVideoAU, &EncParams1);
    }
    else
    {
        M4OSA_TRACE1_2("mcs encoder open Adv profile :%d, level %d",
            EncParams.videoProfile, EncParams.videoLevel);
        err = pC->pVideoEncoderGlobalFcts->pFctOpen(pC->pViEncCtxt,
            &pC->WriterVideoAU, &EncParams);
    }

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intPrepareVideoEncoder: EncoderInt->pFctOpen returns 0x%x",
            err);
        return err;
    }

    pC->encoderState = M4MCS_kEncoderStopped;

    if( M4OSA_NULL != pC->pVideoEncoderGlobalFcts->pFctStart )
    {
        err = pC->pVideoEncoderGlobalFcts->pFctStart(pC->pViEncCtxt);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_intPrepareVideoEncoder: EncoderInt->pFctStart returns 0x%x",
                err);
            return err;
        }
    }

    pC->encoderState = M4MCS_kEncoderRunning;

    /******************************/
    /* Video resize management    */
    /******************************/
    /**
    * Compare video input size and video output size to check if resize is needed */
    if( ( (M4OSA_UInt32)EncParams.FrameWidth
        != pC->pReaderVideoStream->m_videoWidth)
        || ((M4OSA_UInt32)EncParams.FrameHeight
        != pC->pReaderVideoStream->m_videoHeight) )
    {
        /**
        * Allocate the intermediate video plane that will receive the decoded image before
         resizing */
        pC->pPreResizeFrame =
            (M4VIFI_ImagePlane *)M4OSA_32bitAlignedMalloc(3 * sizeof(M4VIFI_ImagePlane),
            M4MCS, (M4OSA_Char *)"m_pPreResizeFrame");

        if( M4OSA_NULL == pC->pPreResizeFrame )
        {
            M4OSA_TRACE1_0("M4MCS_intPrepareVideoEncoder():\
                           unable to allocate m_pPreResizeFrame, returning M4ERR_ALLOC");
            return M4ERR_ALLOC;
        }

        pC->pPreResizeFrame[0].pac_data = M4OSA_NULL;
        pC->pPreResizeFrame[1].pac_data = M4OSA_NULL;
        pC->pPreResizeFrame[2].pac_data = M4OSA_NULL;

        /**
        * Allocate the Y plane */
        pC->pPreResizeFrame[0].u_topleft = 0;
        pC->pPreResizeFrame[0].u_width = pC->pReaderVideoStream->
            m_videoWidth; /**< input width */
        pC->pPreResizeFrame[0].u_height = pC->pReaderVideoStream->
            m_videoHeight; /**< input height */
        pC->pPreResizeFrame[0].u_stride = pC->
            pPreResizeFrame[0].u_width; /**< simple case: stride equals width */

        pC->pPreResizeFrame[0].pac_data =
            (M4VIFI_UInt8 *)M4OSA_32bitAlignedMalloc(pC->pPreResizeFrame[0].u_stride \
            *pC->pPreResizeFrame[0].u_height, M4MCS,
            (M4OSA_Char *)"m_pPreResizeFrame[0].pac_data");

        if( M4OSA_NULL == pC->pPreResizeFrame[0].pac_data )
        {
            M4OSA_TRACE1_0(
                "M4MCS_intPrepareVideoEncoder():\
                     unable to allocate m_pPreResizeFrame[0].pac_data, returning M4ERR_ALLOC");
            return M4ERR_ALLOC;
        }

        /**
        * Allocate the U plane */
        pC->pPreResizeFrame[1].u_topleft = 0;
        pC->pPreResizeFrame[1].u_width = pC->pPreResizeFrame[0].u_width
            >> 1; /**< U width is half the Y width */
        pC->pPreResizeFrame[1].u_height = pC->pPreResizeFrame[0].u_height
            >> 1; /**< U height is half the Y height */
        pC->pPreResizeFrame[1].u_stride = pC->
            pPreResizeFrame[1].u_width; /**< simple case: stride equals width */

        pC->pPreResizeFrame[1].pac_data =
            (M4VIFI_UInt8 *)M4OSA_32bitAlignedMalloc(pC->pPreResizeFrame[1].u_stride \
            *pC->pPreResizeFrame[1].u_height, M4MCS,
            (M4OSA_Char *)"m_pPreResizeFrame[1].pac_data");

        if( M4OSA_NULL == pC->pPreResizeFrame[1].pac_data )
        {
            M4OSA_TRACE1_0(
                "M4MCS_intPrepareVideoEncoder():\
                 unable to allocate m_pPreResizeFrame[1].pac_data, returning M4ERR_ALLOC");
            return M4ERR_ALLOC;
        }

        /**
        * Allocate the V plane */
        pC->pPreResizeFrame[2].u_topleft = 0;
        pC->pPreResizeFrame[2].u_width = pC->
            pPreResizeFrame[1].u_width; /**< V width equals U width */
        pC->pPreResizeFrame[2].u_height = pC->
            pPreResizeFrame[1].u_height; /**< V height equals U height */
        pC->pPreResizeFrame[2].u_stride = pC->
            pPreResizeFrame[2].u_width; /**< simple case: stride equals width */

        pC->pPreResizeFrame[2].pac_data =
            (M4VIFI_UInt8 *)M4OSA_32bitAlignedMalloc(pC->pPreResizeFrame[2].u_stride \
            *pC->pPreResizeFrame[2].u_height, M4MCS,
            (M4OSA_Char *)"m_pPreResizeFrame[1].pac_data");

        if( M4OSA_NULL == pC->pPreResizeFrame[2].pac_data )
        {
            M4OSA_TRACE1_0(
                "M4MCS_intPrepareVideoEncoder():\
                 unable to allocate m_pPreResizeFrame[2].pac_data, returning M4ERR_ALLOC");
            return M4ERR_ALLOC;
        }
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4MCS_intPrepareVideoEncoder(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intPrepareAudioProcessing(M4MCS_InternalContext* pC);
 * @brief    Prepare the AAC decoder, the SRC and the AMR-NB encoder and the MP3 encoder.
 * @param    pC          (IN) MCS private context
 * @return   M4NO_ERROR  No error
 * @return   Any error returned by an underlaying module
 ******************************************************************************
 */
static M4OSA_ERR M4MCS_intPrepareAudioProcessing( M4MCS_InternalContext *pC )
{
    M4OSA_ERR err;

    SSRC_ReturnStatus_en
        ReturnStatus; /* Function return status                       */
    LVM_INT16 NrSamplesMin =
        0; /* Minimal number of samples on the input or on the output */
    LVM_INT32 ScratchSize; /* The size of the scratch memory               */
    LVM_INT16
        *pInputInScratch; /* Pointer to input in the scratch buffer       */
    LVM_INT16
        *pOutputInScratch; /* Pointer to the output in the scratch buffer  */
    SSRC_Params_t ssrcParams;          /* Memory for init parameters                    */

#ifdef MCS_DUMP_PCM_TO_FILE

    file_au_reader = fopen("mcs_ReaderOutput.raw", "wb");
    file_pcm_decoder = fopen("mcs_DecoderOutput.pcm", "wb");
    file_pcm_encoder = fopen("mcs_EncoderInput.pcm", "wb");

#endif

    if( pC->noaudio )
        return M4NO_ERROR;

    if( pC->AudioEncParams.Format == M4ENCODER_kAudioNULL )
    {
        M4OSA_TRACE3_0(
            "M4MCS_intPrepareAudioProcessing(): Null encoding, do nothing.");
        return M4NO_ERROR;
    }

    /* ________________________________ */
    /*|                                |*/
    /*| Create and "start" the decoder |*/
    /*|________________________________|*/

    if( M4OSA_NULL == pC->m_pAudioDecoder )
    {
        M4OSA_TRACE1_0(
            "M4MCS_intPrepareAudioProcessing(): Fails to initiate the audio decoder.");
        return M4MCS_ERR_AUDIO_CONVERSION_FAILED;
    }

    if( M4OSA_NULL == pC->pAudioDecCtxt )
    {
        err = pC->m_pAudioDecoder->m_pFctCreateAudioDec(&pC->pAudioDecCtxt,
            pC->pReaderAudioStream, pC->m_pCurrentAudioDecoderUserData);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_intPrepareVideoDecoder: m_pAudioDecoder->m_pFctCreateAudioDec returns 0x%x",
                err);
            return err;
        }
    }

    if( M4VIDEOEDITING_kAMR_NB == pC->InputFileProperties.AudioStreamType ) {
        /* AMR DECODER CONFIGURATION */

        /* nothing specific to do */
    }
    else if( M4VIDEOEDITING_kEVRC == pC->InputFileProperties.AudioStreamType ) {
        /* EVRC DECODER CONFIGURATION */

        /* nothing specific to do */
    }
    else if( M4VIDEOEDITING_kMP3 == pC->InputFileProperties.AudioStreamType ) {
        /* MP3 DECODER CONFIGURATION */

        /* nothing specific to do */
    }
    else
    {
        /* AAC DECODER CONFIGURATION */
        M4_AacDecoderConfig AacDecParam;

        AacDecParam.m_AACDecoderProfile = AAC_kAAC;
        AacDecParam.m_DownSamplingMode = AAC_kDS_OFF;

        if( pC->AudioEncParams.Format == M4ENCODER_kAMRNB )
        {
            AacDecParam.m_OutputMode = AAC_kMono;
        }
        else
        {
            /* For this version, we encode only in AAC */
            if( M4ENCODER_kMono == pC->AudioEncParams.ChannelNum )
            {
                AacDecParam.m_OutputMode = AAC_kMono;
            }
            else
            {
                AacDecParam.m_OutputMode = AAC_kStereo;
            }
        }

        pC->m_pAudioDecoder->m_pFctSetOptionAudioDec(pC->pAudioDecCtxt,
            M4AD_kOptionID_UserParam, (M4OSA_DataOption) &AacDecParam);
    }

    pC->m_pAudioDecoder->m_pFctSetOptionAudioDec(pC->pAudioDecCtxt,
           M4AD_kOptionID_3gpReaderInterface, (M4OSA_DataOption) pC->m_pReaderDataIt);

    pC->m_pAudioDecoder->m_pFctSetOptionAudioDec(pC->pAudioDecCtxt,
           M4AD_kOptionID_AudioAU, (M4OSA_DataOption) &pC->ReaderAudioAU);

    if( pC->m_pAudioDecoder->m_pFctStartAudioDec != M4OSA_NULL )
    {
        /* Not implemented in all decoders */
        err = pC->m_pAudioDecoder->m_pFctStartAudioDec(pC->pAudioDecCtxt);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_intPrepareVideoDecoder: m_pAudioDecoder->m_pFctStartAudioDec returns 0x%x",
                err);
            return err;
        }
    }

    /**
    * Allocate output buffer for the audio decoder */
    pC->InputFileProperties.uiDecodedPcmSize =
        pC->pReaderAudioStream->m_byteFrameLength
        * pC->pReaderAudioStream->m_byteSampleSize
        * pC->pReaderAudioStream->m_nbChannels;

    if( pC->InputFileProperties.uiDecodedPcmSize > 0 )
    {
        pC->AudioDecBufferOut.m_bufferSize =
            pC->InputFileProperties.uiDecodedPcmSize;
        pC->AudioDecBufferOut.m_dataAddress =
            (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(pC->AudioDecBufferOut.m_bufferSize \
            *sizeof(short), M4MCS, (M4OSA_Char *)"AudioDecBufferOut.m_bufferSize");
    }

    if( M4OSA_NULL == pC->AudioDecBufferOut.m_dataAddress )
    {
        M4OSA_TRACE1_0(
            "M4MCS_intPrepareVideoDecoder():\
             unable to allocate AudioDecBufferOut.m_dataAddress, returning M4ERR_ALLOC");
        return M4ERR_ALLOC;
    }

    /* _________________________ */
    /*|                         |*/
    /*| Set the SSRC parameters |*/
    /*|_________________________|*/

    switch( pC->pReaderAudioStream->m_samplingFrequency )
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
                "M4MCS_intPrepareVideoDecoder: invalid input AAC sampling frequency (%d Hz),\
                 returning M4MCS_ERR_INVALID_AAC_SAMPLING_FREQUENCY",
                pC->pReaderAudioStream->m_samplingFrequency);
            return M4MCS_ERR_INVALID_AAC_SAMPLING_FREQUENCY;
    }

    if( 1 == pC->pReaderAudioStream->m_nbChannels )
    {
        ssrcParams.SSRC_NrOfChannels = LVM_MONO;
    }
    else
    {
        ssrcParams.SSRC_NrOfChannels = LVM_STEREO;
    }

    /*FlB 26.02.2009: add mp3 as output format*/
    if( pC->AudioEncParams.Format == M4ENCODER_kAAC
        || pC->AudioEncParams.Format == M4ENCODER_kMP3 )
    {
        switch( pC->AudioEncParams.Frequency )
        {
            case M4ENCODER_k8000Hz:
                ssrcParams.SSRC_Fs_Out = LVM_FS_8000;
                break;

            case M4ENCODER_k11025Hz:
                ssrcParams.SSRC_Fs_Out = LVM_FS_11025;
                break;

            case M4ENCODER_k12000Hz:
                ssrcParams.SSRC_Fs_Out = LVM_FS_12000;
                break;

            case M4ENCODER_k16000Hz:
                ssrcParams.SSRC_Fs_Out = LVM_FS_16000;
                break;

            case M4ENCODER_k22050Hz:
                ssrcParams.SSRC_Fs_Out = LVM_FS_22050;
                break;

            case M4ENCODER_k24000Hz:
                ssrcParams.SSRC_Fs_Out = LVM_FS_24000;
                break;

            case M4ENCODER_k32000Hz:
                ssrcParams.SSRC_Fs_Out = LVM_FS_32000;
                break;

            case M4ENCODER_k44100Hz:
                ssrcParams.SSRC_Fs_Out = LVM_FS_44100;
                break;

            case M4ENCODER_k48000Hz:
                ssrcParams.SSRC_Fs_Out = LVM_FS_48000;
                break;

            default:
                M4OSA_TRACE1_1(
                    "M4MCS_intPrepareAudioProcessing: invalid output AAC sampling frequency \
                    (%d Hz), returning M4MCS_ERR_INVALID_AAC_SAMPLING_FREQUENCY",
                    pC->AudioEncParams.Frequency);
                return M4MCS_ERR_INVALID_AAC_SAMPLING_FREQUENCY;
                break;
        }
    }
    else
    {
        ssrcParams.SSRC_Fs_Out = LVM_FS_8000;
    }



    ReturnStatus = 0;

    switch( ssrcParams.SSRC_Fs_In )
    {
        case LVM_FS_8000:
            ssrcParams.NrSamplesIn = 320;
            break;

        case LVM_FS_11025:
            ssrcParams.NrSamplesIn = 441;
            break;

        case LVM_FS_12000:
            ssrcParams.NrSamplesIn = 480;
            break;

        case LVM_FS_16000:
            ssrcParams.NrSamplesIn = 640;
            break;

        case LVM_FS_22050:
            ssrcParams.NrSamplesIn = 882;
            break;

        case LVM_FS_24000:
            ssrcParams.NrSamplesIn = 960;
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

    switch( ssrcParams.SSRC_Fs_Out )
    {
        case LVM_FS_8000:
            ssrcParams.NrSamplesOut = 320;
            break;

        case LVM_FS_11025:
            ssrcParams.NrSamplesOut = 441;
            break;

        case LVM_FS_12000:
            ssrcParams.NrSamplesOut = 480;
            break;

        case LVM_FS_16000:
            ssrcParams.NrSamplesOut = 640;
            break;

        case LVM_FS_22050:
            ssrcParams.NrSamplesOut = 882;
            break;

        case LVM_FS_24000:
            ssrcParams.NrSamplesOut = 960;
            break;

        case LVM_FS_32000:
            ssrcParams.NrSamplesOut = 1280;
            break;

        case LVM_FS_44100:
            ssrcParams.NrSamplesOut = 1764;
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
            "M4MCS_intPrepareAudioProcessing:\
             Error code %d returned by the SSRC_GetNrSamples function",
            ReturnStatus);
        return M4MCS_ERR_AUDIO_CONVERSION_FAILED;
    }

    NrSamplesMin =
        (LVM_INT16)((ssrcParams.NrSamplesIn > ssrcParams.NrSamplesOut)
        ? ssrcParams.NrSamplesOut : ssrcParams.NrSamplesIn);

    while( NrSamplesMin < M4MCS_SSRC_MINBLOCKSIZE )
    { /* Don't take blocks smaller that the minimal block size */
        ssrcParams.NrSamplesIn = (LVM_INT16)(ssrcParams.NrSamplesIn << 1);
        ssrcParams.NrSamplesOut = (LVM_INT16)(ssrcParams.NrSamplesOut << 1);
        NrSamplesMin = (LVM_INT16)(NrSamplesMin << 1);
    }


    pC->iSsrcNbSamplIn = (LVM_INT16)(
        ssrcParams.
        NrSamplesIn); /* multiplication by NrOfChannels is done below */
    pC->iSsrcNbSamplOut = (LVM_INT16)(ssrcParams.NrSamplesOut);

    /**
    * Allocate buffer for the input of the SSRC */
    pC->pSsrcBufferIn =
        (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(pC->iSsrcNbSamplIn * sizeof(short) \
        *pC->pReaderAudioStream->m_nbChannels, M4MCS,
        (M4OSA_Char *)"pSsrcBufferIn");

    if( M4OSA_NULL == pC->pSsrcBufferIn )
    {
        M4OSA_TRACE1_0(
            "M4MCS_intPrepareVideoDecoder():\
             unable to allocate pSsrcBufferIn, returning M4ERR_ALLOC");
        return M4ERR_ALLOC;
    }
    pC->pPosInSsrcBufferIn = (M4OSA_MemAddr8)pC->pSsrcBufferIn;

    /**
    * Allocate buffer for the output of the SSRC */
    pC->pSsrcBufferOut =
        (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(pC->iSsrcNbSamplOut * sizeof(short) \
        *pC->pReaderAudioStream->m_nbChannels, M4MCS,
        (M4OSA_Char *)"pSsrcBufferOut");

    if( M4OSA_NULL == pC->pSsrcBufferOut )
    {
        M4OSA_TRACE1_0(
            "M4MCS_intPrepareVideoDecoder():\
             unable to allocate pSsrcBufferOut, returning M4ERR_ALLOC");
        return M4ERR_ALLOC;
    }


    pC->pLVAudioResampler = LVAudioResamplerCreate(
        16, /*gInputParams.lvBTChannelCount*/
        (M4OSA_Int16)pC->InputFileProperties.uiNbChannels/*ssrcParams.SSRC_NrOfChannels*/,
        (M4OSA_Int32)(pC->AudioEncParams.Frequency)/*ssrcParams.SSRC_Fs_Out*/, 1);

     if( M4OSA_NULL == pC->pLVAudioResampler)
     {
         return M4ERR_ALLOC;
     }

    LVAudiosetSampleRate(pC->pLVAudioResampler,
        /*gInputParams.lvInSampleRate*/
        /*pC->pAddedClipCtxt->pSettings->ClipProperties.uiSamplingFrequency*/
        pC->InputFileProperties.uiSamplingFrequency/*ssrcParams.SSRC_Fs_In*/);

    LVAudiosetVolume(pC->pLVAudioResampler, (M4OSA_Int16)(0x1000 /* 0x7fff */),
        (M4OSA_Int16)(0x1000/*0x7fff*/));


    /* ________________________ */
    /*|                        |*/
    /*| Init the audio encoder |*/
    /*|________________________|*/

    /* Initialise the audio encoder */

    err = pC->pAudioEncoderGlobalFcts->pFctInit(&pC->pAudioEncCtxt,
        pC->pCurrentAudioEncoderUserData);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intPrepareAudioProcessing: pAudioEncoderGlobalFcts->pFctInit returns 0x%x",
            err);
        return err;
    }

    /* Open the audio encoder */
    err = pC->pAudioEncoderGlobalFcts->pFctOpen(pC->pAudioEncCtxt,
        &pC->AudioEncParams, &pC->pAudioEncDSI,
        M4OSA_NULL /* no grabbing */);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intPrepareAudioProcessing: pAudioEncoderGlobalFcts->pFctOpen returns 0x%x",
            err);
        return err;
    }

    /* Allocate the input buffer for the audio encoder */
    switch( pC->AudioEncParams.Format )
    {
        case M4ENCODER_kAMRNB:
            pC->audioEncoderGranularity = M4MCS_PCM_AMR_GRANULARITY_SAMPLES;
            break;

        case M4ENCODER_kAAC:
            pC->audioEncoderGranularity = M4MCS_PCM_AAC_GRANULARITY_SAMPLES;
            break;

            /*FlB 26.02.2009: add mp3 as output format*/
        case M4ENCODER_kMP3:
            pC->audioEncoderGranularity = M4MCS_PCM_MP3_GRANULARITY_SAMPLES;
            break;

         default:
         break;
    }

    if( M4ENCODER_kMono == pC->AudioEncParams.ChannelNum )
        pC->audioEncoderGranularity *= sizeof(short);
    else
        pC->audioEncoderGranularity *= sizeof(short) * 2;

    pC->pPosInAudioEncoderBuffer = M4OSA_NULL;
    pC->pAudioEncoderBuffer =
        (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(pC->audioEncoderGranularity, M4MCS,
        (M4OSA_Char *)"pC->pAudioEncoderBuffer");

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4MCS_intPrepareAudioProcessing(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intPrepareWriter(M4MCS_InternalContext* pC);
 * @brief    Prepare the writer.
 * @param    pC          (IN) MCS private context
 * @return   M4NO_ERROR  No error
 * @return   Any error returned by an underlaying module
 ******************************************************************************
 */
static M4OSA_ERR M4MCS_intPrepareWriter( M4MCS_InternalContext *pC )
{
    M4OSA_ERR err;
    M4OSA_UInt32 uiVersion; /**< To write component version in 3gp writer */
    M4OSA_MemAddr8 pDSI = M4OSA_NULL; /**< To create the Decoder Specific Info */
    M4SYS_StreamIDValue optionValue; /**< For the setoption calls */
    M4OSA_UInt32 TargetedFileSize;
    M4OSA_Bool bMULPPSSPS = M4OSA_FALSE;

    /**
    * Init the writer */
    err = pC->pWriterGlobalFcts->pFctOpen(&pC->pWriterContext, pC->pOutputFile,
        pC->pOsaFileWritPtr, pC->pTemporaryFile, pC->pOsaFileReadPtr);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intPrepareWriter: pWriterGlobalFcts->pFctOpen returns 0x%x",
            err);
        return err;
    }

    /**
    * Link to the writer context in the writer interface */
    pC->pWriterDataFcts->pWriterContext = pC->pWriterContext;

    /**
    * Set the product description string in the written file */
    err = pC->pWriterGlobalFcts->pFctSetOption(pC->pWriterContext,
        M4WRITER_kEmbeddedString, (M4OSA_DataOption)"NXP-SW : MCS    ");

    if( ( M4NO_ERROR != err) && (( (M4OSA_UInt32)M4ERR_BAD_OPTION_ID)
        != err) ) /* this option may not be implemented by some writers */
    {
        M4OSA_TRACE1_1(
            "M4MCS_intPrepareWriter:\
             pWriterGlobalFcts->pFctSetOption(M4WRITER_kEmbeddedString) returns 0x%x",
            err);
        return err;
    }

    /**
    * Set the product version in the written file */
    uiVersion =
        M4VIDEOEDITING_VERSION_MAJOR * 100 + M4VIDEOEDITING_VERSION_MINOR * 10
        + M4VIDEOEDITING_VERSION_REVISION;
    err = pC->pWriterGlobalFcts->pFctSetOption(pC->pWriterContext,
        M4WRITER_kEmbeddedVersion, (M4OSA_DataOption) &uiVersion);

    if( ( M4NO_ERROR != err) && (( (M4OSA_UInt32)M4ERR_BAD_OPTION_ID)
        != err) ) /* this option may not be implemented by some writers */
    {
        M4OSA_TRACE1_1(
            "M4MCS_intPrepareWriter: \
            pWriterGlobalFcts->pFctSetOption(M4WRITER_kEmbeddedVersion) returns 0x%x",
            err);
        return err;
    }

    /**
    * If there is a video input, allocate and fill the video stream structures for the writer */
    if( pC->novideo == M4OSA_FALSE )
    {
        /**
        * Fill Video properties structure for the AddStream method */
        pC->WriterVideoStreamInfo.height = pC->EncodingHeight;
        pC->WriterVideoStreamInfo.width = pC->EncodingWidth;
        pC->WriterVideoStreamInfo.fps =
            0; /**< Not used by the shell/core writer */
        pC->WriterVideoStreamInfo.Header.pBuf =
            M4OSA_NULL; /**< Will be updated later */
        pC->WriterVideoStreamInfo.Header.Size = 0; /**< Will be updated later */

        /**
        * Fill Video stream description structure for the AddStream method */
        switch( pC->EncodingVideoFormat )
        {
            case M4ENCODER_kMPEG4:
                pC->WriterVideoStream.streamType = M4SYS_kMPEG_4;
                break;

            case M4ENCODER_kH263:
                pC->WriterVideoStream.streamType = M4SYS_kH263;
                break;

            case M4ENCODER_kH264:
                pC->WriterVideoStream.streamType = M4SYS_kH264;
                break;

            case M4ENCODER_kNULL:
                switch( pC->InputFileProperties.VideoStreamType )
                {
                    case M4VIDEOEDITING_kMPEG4:
                        pC->WriterVideoStream.streamType = M4SYS_kMPEG_4;
                        break;

                    case M4VIDEOEDITING_kH263:
                        pC->WriterVideoStream.streamType = M4SYS_kH263;
                        break;

                    case M4VIDEOEDITING_kH264:
                        pC->WriterVideoStream.streamType = M4SYS_kH264;
                        break;

                    default:
                        M4OSA_TRACE1_1(
                            "M4MCS_intPrepareWriter: case input=M4ENCODER_kNULL, \
                            unknown format (0x%x),\
                             returning M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FORMAT",
                            pC->EncodingVideoFormat);
                        return M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FORMAT;
                }
                break;

            default: /**< It should never happen, already tested */
                M4OSA_TRACE1_1(
                    "M4MCS_intPrepareWriter: unknown format (0x%x),\
                     returning M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FORMAT",
                    pC->EncodingVideoFormat);
                return M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FORMAT;
        }

        /**
        * Video bitrate value will be the real value */
        pC->WriterVideoStream.averageBitrate =
            (M4OSA_Int32)pC->uiEncVideoBitrate;
        pC->WriterVideoStream.maxBitrate = (M4OSA_Int32)pC->uiEncVideoBitrate;

        /**
        * most other parameters are "dummy" */
        pC->WriterVideoStream.streamID = M4MCS_WRITER_VIDEO_STREAM_ID;
        pC->WriterVideoStream.timeScale =
            0; /**< Not used by the shell/core writer */
        pC->WriterVideoStream.profileLevel =
            0; /**< Not used by the shell/core writer */
        pC->WriterVideoStream.duration =
            0; /**< Not used by the shell/core writer */
        pC->WriterVideoStream.decoderSpecificInfoSize =
            sizeof(M4WRITER_StreamVideoInfos);
        pC->WriterVideoStream.decoderSpecificInfo =
            (M4OSA_MemAddr32) &(pC->WriterVideoStreamInfo);

        /**
        * Update Encoder Header properties for Video stream if needed */
        if( M4ENCODER_kH263 == pC->EncodingVideoFormat )
        {
            /**
            * Creates the H263 DSI */
            pC->WriterVideoStreamInfo.Header.Size =
                7; /**< H263 output DSI is always 7 bytes */
            pDSI = (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(7, M4MCS, (M4OSA_Char
                *)"pC->WriterVideoStreamInfo.Header.pBuf (DSI H263)");

            if( M4OSA_NULL == pDSI )
            {
                M4OSA_TRACE1_0("M4MCS_intPrepareWriter(): unable to allocate pDSI (H263),\
                               returning M4ERR_ALLOC");
                return M4ERR_ALLOC;
            }

            /**
            * Vendor is NXP Software: N, X, P, S. */
            pDSI[0] = 'N';
            pDSI[1] = 'X';
            pDSI[2] = 'P';
            pDSI[3] = 'S';

            /**
            * Decoder version is 0 */
            pDSI[4] = 0;

            /**
            * Level is the sixth byte of the DSI. */
            switch( pC->EncodingWidth )
            {
                case M4ENCODER_SQCIF_Width:
                case M4ENCODER_QCIF_Width:
                    if( ( pC->uiEncVideoBitrate <= M4ENCODER_k64_KBPS)
                        && (pC->EncodingVideoFramerate <= M4ENCODER_k15_FPS) )
                    {
                        pDSI[5] = 10;
                    }
                    else if( ( pC->uiEncVideoBitrate <= M4ENCODER_k128_KBPS)
                        && (pC->EncodingVideoFramerate <= M4ENCODER_k15_FPS) )
                    {
                        pDSI[5] = 45;
                    }
                    else if( ( pC->uiEncVideoBitrate <= M4ENCODER_k128_KBPS)
                        && (pC->EncodingVideoFramerate <= M4ENCODER_k30_FPS) )
                    {
                        pDSI[5] = 20;
                    }
                    else if( ( pC->uiEncVideoBitrate <= M4ENCODER_k384_KBPS)
                        && (pC->EncodingVideoFramerate <= M4ENCODER_k30_FPS) )
                    {
                        pDSI[5] = 30;
                    }
                    else if( ( pC->uiEncVideoBitrate
                        <= M4ENCODER_k800_KBPS/*2048*/)
                        && (pC->EncodingVideoFramerate <= M4ENCODER_k30_FPS) )
                    {
                        pDSI[5] = 40;
                    }
                    break;

                case M4ENCODER_CIF_Width:
                    if( ( pC->uiEncVideoBitrate <= M4ENCODER_k128_KBPS)
                        && (pC->EncodingVideoFramerate <= M4ENCODER_k15_FPS) )
                    {
                        pDSI[5] = 20;
                    }
                    else if( ( pC->uiEncVideoBitrate <= M4ENCODER_k384_KBPS)
                        && (pC->EncodingVideoFramerate <= M4ENCODER_k30_FPS) )
                    {
                        pDSI[5] = 30;
                    }
                    else if( ( pC->uiEncVideoBitrate
                        <= M4ENCODER_k800_KBPS/*2048*/)
                        && (pC->EncodingVideoFramerate <= M4ENCODER_k30_FPS) )
                    {
                        pDSI[5] = 40;
                    }
                    break;

                    default:
                    break;
            }

            /**
            * Profile is the seventh byte of the DSI. */
            pDSI[6] = 0;

            pC->WriterVideoStreamInfo.Header.pBuf = pDSI;
        }
        else if( M4ENCODER_kNULL == pC->EncodingVideoFormat )
        {
            /* If we copy the stream from the input, we copy its DSI */

            pC->WriterVideoStreamInfo.Header.Size = pC->pReaderVideoStream->
                m_basicProperties.m_decoderSpecificInfoSize;
            pC->WriterVideoStreamInfo.Header.pBuf =
                (M4OSA_MemAddr8)pC->pReaderVideoStream->
                m_basicProperties.m_pDecoderSpecificInfo;

        }
        /* otherwise (MPEG4), the DSI will be recovered from the encoder later on. */

        /*+CRLV6775 - H.264 Trimming  */
        if( pC->bH264Trim == M4OSA_TRUE )
        {
            bMULPPSSPS = M4OSA_TRUE;
            err = pC->pWriterGlobalFcts->pFctSetOption(pC->pWriterContext,
                (M4OSA_UInt32)M4WRITER_kMUL_PPS_SPS,
                (M4OSA_DataOption) &bMULPPSSPS);

            if( ( M4NO_ERROR != err)
                && (( (M4OSA_UInt32)M4ERR_BAD_OPTION_ID)
                != err) ) /* this option may not be implemented by some writers */
            {
                M4OSA_TRACE1_1(
                    "M4MCS_intPrepareWriter:\
                     pWriterGlobalFcts->pFctSetOption(M4WRITER_kMUL_PPS_SPS) returns 0x%x",
                    err);
                return err;
            }
        }
        /*-CRLV6775 - H.264 Trimming  */
        /**
        * Add the video stream */
        err = pC->pWriterGlobalFcts->pFctAddStream(pC->pWriterContext,
            &pC->WriterVideoStream);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_intPrepareWriter: pWriterGlobalFcts->pFctAddStream(video) returns 0x%x!",
                err);
            return err;
        }

        /**
        * Update AU properties for video stream */
        pC->WriterVideoAU.stream = &(pC->WriterVideoStream);
        pC->WriterVideoAU.dataAddress = M4OSA_NULL;
        pC->WriterVideoAU.size = 0;
        pC->WriterVideoAU.CTS = 0; /** Reset time */
        pC->WriterVideoAU.DTS = 0;
        pC->WriterVideoAU.attribute = AU_RAP;
        pC->WriterVideoAU.nbFrag = 0; /** No fragment */
        pC->WriterVideoAU.frag = M4OSA_NULL;

        /**
        * Set the writer max video AU size */
        optionValue.streamID = M4MCS_WRITER_VIDEO_STREAM_ID;
        optionValue.value = pC->uiVideoMaxAuSize;
        err = pC->pWriterGlobalFcts->pFctSetOption(pC->pWriterContext,
            (M4OSA_UInt32)M4WRITER_kMaxAUSize,
            (M4OSA_DataOption) &optionValue);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_intPrepareWriter: \
                pWriterGlobalFcts->pFctSetOption(M4WRITER_kMaxAUSize, video) returns 0x%x!",
                err);
            return err;
        }

        /**
        * Set the writer max video chunk size */
        optionValue.value = pC->uiVideoMaxChunckSize;
        err = pC->pWriterGlobalFcts->pFctSetOption(pC->pWriterContext,
            (M4OSA_UInt32)M4WRITER_kMaxChunckSize,
            (M4OSA_DataOption) &optionValue);

        if( ( M4NO_ERROR != err) && (( (M4OSA_UInt32)M4ERR_BAD_OPTION_ID)
            != err) ) /* this option may not be implemented by some writers */
        {
            M4OSA_TRACE1_1(
                "M4MCS_intPrepareWriter:\
                 pWriterGlobalFcts->pFctSetOption(M4WRITER_kMaxAUSize, video) returns 0x%x!",
                err);
            return err;
        }
    }

    /**
    * If there is an audio input, allocate and fill the audio stream structures for the writer */
    if( pC->noaudio == M4OSA_FALSE )
    {
        M4WRITER_StreamAudioInfos streamAudioInfo;

        streamAudioInfo.nbSamplesPerSec = 0; /**< unused by our shell writer */
        streamAudioInfo.nbBitsPerSample = 0; /**< unused by our shell writer */
        streamAudioInfo.nbChannels = 1;      /**< unused by our shell writer */

        pC->WriterAudioStream.averageBitrate =
            0; /**< It is not used by the shell, the DSI is taken into account instead */
        pC->WriterAudioStream.maxBitrate =
            0; /**< Not used by the shell/core writer */

        /**
        * Fill Audio stream description structure for the AddStream method */
        switch( pC->AudioEncParams.Format )
        {
            case M4ENCODER_kAMRNB:
                pC->WriterAudioStream.streamType = M4SYS_kAMR;
                break;

            case M4ENCODER_kAAC:
                pC->WriterAudioStream.streamType = M4SYS_kAAC;
                pC->WriterAudioStream.averageBitrate =
                    pC->AudioEncParams.Bitrate;
                pC->WriterAudioStream.maxBitrate = pC->AudioEncParams.Bitrate;
                break;

                /*FlB 26.02.2009: add mp3 as output format*/
            case M4ENCODER_kMP3:
                pC->WriterAudioStream.streamType = M4SYS_kMP3;
                break;

            case M4ENCODER_kAudioNULL:
                switch( pC->InputFileProperties.AudioStreamType )
                {
                case M4VIDEOEDITING_kAMR_NB:
                    pC->WriterAudioStream.streamType = M4SYS_kAMR;
                    break;
                    /*FlB 26.02.2009: add mp3 as output format*/
                case M4VIDEOEDITING_kMP3:
                    pC->WriterAudioStream.streamType = M4SYS_kMP3;
                    break;

                case M4VIDEOEDITING_kAAC:
                case M4VIDEOEDITING_kAACplus:
                case M4VIDEOEDITING_keAACplus:
                    pC->WriterAudioStream.streamType = M4SYS_kAAC;
                    pC->WriterAudioStream.averageBitrate =
                        pC->AudioEncParams.Bitrate;
                    pC->WriterAudioStream.maxBitrate =
                        pC->AudioEncParams.Bitrate;
                    break;

                case M4VIDEOEDITING_kEVRC:
                    pC->WriterAudioStream.streamType = M4SYS_kEVRC;
                    break;

                case M4VIDEOEDITING_kNoneAudio:
                case M4VIDEOEDITING_kPCM:
                case M4VIDEOEDITING_kNullAudio:
                case M4VIDEOEDITING_kUnsupportedAudio:
                    break;
                }
                break;

            default: /**< It should never happen, already tested */
                M4OSA_TRACE1_1(
                    "M4MCS_intPrepareWriter: \
                    unknown format (0x%x), returning M4MCS_ERR_UNDEFINED_OUTPUT_AUDIO_FORMAT",
                    pC->AudioEncParams.Format);
                return M4MCS_ERR_UNDEFINED_OUTPUT_AUDIO_FORMAT;
        }

        /**
        * MCS produces only AMR-NB output */
        pC->WriterAudioStream.streamID = M4MCS_WRITER_AUDIO_STREAM_ID;
        pC->WriterAudioStream.duration =
            0; /**< Not used by the shell/core writer */
        pC->WriterAudioStream.profileLevel =
            0; /**< Not used by the shell/core writer */
        pC->WriterAudioStream.timeScale = pC->AudioEncParams.Frequency;

        if( pC->AudioEncParams.Format == M4ENCODER_kAudioNULL )
        {
            /* If we copy the stream from the input, we copy its DSI */
            streamAudioInfo.Header.Size = pC->pReaderAudioStream->
                m_basicProperties.m_decoderSpecificInfoSize;
            streamAudioInfo.Header.pBuf =
                (M4OSA_MemAddr8)pC->pReaderAudioStream->
                m_basicProperties.m_pDecoderSpecificInfo;
        }
        else
        {
            if( pC->pAudioEncDSI.pInfo != M4OSA_NULL )
            {
                /* Use the DSI given by the encoder open() */
                streamAudioInfo.Header.Size = pC->pAudioEncDSI.infoSize;
                streamAudioInfo.Header.pBuf = pC->pAudioEncDSI.pInfo;
            }
            else
            {
                /* Writer will put a default Philips DSI */
                streamAudioInfo.Header.Size = 0;
                streamAudioInfo.Header.pBuf = M4OSA_NULL;
            }
        }

        /**
        * Our writer shell interface is a little tricky: we put M4WRITER_StreamAudioInfos
         in the DSI pointer... */
        pC->WriterAudioStream.decoderSpecificInfo =
            (M4OSA_MemAddr32) &streamAudioInfo;

        /**
        * Add the audio stream to the writer */
        err = pC->pWriterGlobalFcts->pFctAddStream(pC->pWriterContext,
            &pC->WriterAudioStream);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_intPrepareWriter: pWriterGlobalFcts->pFctAddStream(audio) returns 0x%x",
                err);
            return err;
        }

        /**
        * Link the AU and the stream */
        pC->WriterAudioAU.stream = &(pC->WriterAudioStream);
        pC->WriterAudioAU.dataAddress = M4OSA_NULL;
        pC->WriterAudioAU.size = 0;
        pC->WriterAudioAU.CTS = 0; /** Reset time */
        pC->WriterAudioAU.DTS = 0;
        pC->WriterAudioAU.attribute = 0;
        pC->WriterAudioAU.nbFrag = 0; /** No fragment */
        pC->WriterAudioAU.frag = M4OSA_NULL;

        /**
        * Set the writer audio max AU size */
        /* As max bitrate is now 320kbps instead of 128kbps, max AU
         * size has to be increased adapt the max AU size according to the stream type and the
         * channels numbers*/
        /* After tests, a margin of 3 is taken (2 was not enough and raises to memory overwrite)
         */
        //pC->uiAudioMaxAuSize = M4MCS_AUDIO_MAX_AU_SIZE;
        switch( pC->WriterAudioStream.streamType )
        {
            case M4SYS_kAMR:
                pC->uiAudioMaxAuSize = M4MCS_PCM_AMR_GRANULARITY_SAMPLES
                    * (( pC->InputFileProperties.uiNbChannels
                    * sizeof(short)) + 3);
                break;

            case M4SYS_kMP3:
                pC->uiAudioMaxAuSize = M4MCS_PCM_MP3_GRANULARITY_SAMPLES
                    * (( pC->InputFileProperties.uiNbChannels
                    * sizeof(short)) + 3);
                break;

            case M4SYS_kAAC:
                pC->uiAudioMaxAuSize = M4MCS_PCM_AAC_GRANULARITY_SAMPLES
                    * (( pC->InputFileProperties.uiNbChannels
                    * sizeof(short)) + 3);
                break;
                /*case M4SYS_kEVRC:
                pC->uiAudioMaxAuSize = M4MCS_PCM_EVRC_GRANULARITY_SAMPLES*
                ((pC->InputFileProperties.uiNbChannels * sizeof(short))+3);
                break;*/
            default: /**< It should never happen, already tested */
                M4OSA_TRACE1_1(
                    "M4MCS_intPrepareWriter: unknown format (0x%x),\
                     returning M4MCS_ERR_UNDEFINED_OUTPUT_AUDIO_FORMAT",
                    pC->WriterAudioStream.streamType);
                return M4MCS_ERR_UNDEFINED_OUTPUT_AUDIO_FORMAT;
        }

        optionValue.streamID = M4MCS_WRITER_AUDIO_STREAM_ID;
        optionValue.value = pC->uiAudioMaxAuSize;
        err = pC->pWriterGlobalFcts->pFctSetOption(pC->pWriterContext,
            (M4OSA_UInt32)M4WRITER_kMaxAUSize,
            (M4OSA_DataOption) &optionValue);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_intPrepareWriter: pWriterGlobalFcts->pFctSetOption(audio,\
                M4WRITER_kMaxAUSize) returns 0x%x",
                err);
            return err;
        }

        optionValue.value = M4MCS_AUDIO_MAX_CHUNK_SIZE;
        err = pC->pWriterGlobalFcts->pFctSetOption(pC->pWriterContext,
            (M4OSA_UInt32)M4WRITER_kMaxChunckSize,
            (M4OSA_DataOption) &optionValue);

        if( ( M4NO_ERROR != err) && (( (M4OSA_UInt32)M4ERR_BAD_OPTION_ID)
            != err) ) /* this option may not be implemented by some writers */
        {
            M4OSA_TRACE1_1(
                "M4MCS_intPrepareWriter: pWriterGlobalFcts->pFctSetOption(audio,\
                M4WRITER_kMaxChunckSize) returns 0x%x",
                err);
            return err;
        }
    }

    /*
    * Set the limitation size of the writer */
    TargetedFileSize = pC->uiMaxFileSize;
    /* add 1 kB margin */
    if( TargetedFileSize > 8192 )
        TargetedFileSize -= 1024;

    err = pC->pWriterGlobalFcts->pFctSetOption(pC->pWriterContext,
        (M4OSA_UInt32)M4WRITER_kMaxFileSize,
        (M4OSA_DataOption) &TargetedFileSize);

    if( ( M4NO_ERROR != err) && (( (M4OSA_UInt32)M4ERR_BAD_OPTION_ID)
        != err) ) /* this option may not be implemented by some writers */
    {
        M4OSA_TRACE1_1(
            "M4MCS_intPrepareWriter: pWriterGlobalFcts->pFctSetOption\
            (M4WRITER_kMaxFileSize) returns 0x%x!",
            err);
        return err;
    }

    /**
    * Close the stream registering in order to be ready to write data */
    err = pC->pWriterGlobalFcts->pFctStartWriting(pC->pWriterContext);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intPrepareWriter: pWriterGlobalFcts->pFctStartWriting returns 0x%x",
            err);
        return err;
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4MCS_intPrepareWriter(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intPrepareAudioBeginCut(M4MCS_InternalContext* pC);
 * @brief    DO the audio begin cut.
 * @param    pC          (IN) MCS private context
 * @return   M4NO_ERROR  No error
 * @return   Any error returned by an underlaying module
 ******************************************************************************
 */
static M4OSA_ERR M4MCS_intPrepareAudioBeginCut( M4MCS_InternalContext *pC )
{
    M4OSA_ERR err;
    M4OSA_Int32 iCts;
    M4OSA_UInt32 uiFrameSize;

    if( pC->noaudio )
        return M4NO_ERROR;

    /**
    * Check if an audio begin cut is needed */
    if( ( M4OSA_NULL == pC->pReaderAudioStream) || (0 == pC->uiBeginCutTime) )
    {
        /**
        * Return with no error */
        M4OSA_TRACE3_0(
            "M4MCS_intPrepareAudioBeginCut(): returning M4NO_ERROR (a)");
        return M4NO_ERROR;
    }

    /**
    * Jump at the begin cut time */
    iCts = pC->uiBeginCutTime;
    err = pC->m_pReader->m_pFctJump(pC->pReaderContext,
        (M4_StreamHandler *)pC->pReaderAudioStream, &iCts);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intPrepareAudioBeginCut: m_pFctJump(Audio) returns 0x%x!",
            err);
        return err;
    }

    /**
    * Remember audio begin cut offset */
    pC->iAudioCtsOffset = iCts;

    /**
    * AMR-NB & EVRC: there may be many frames per AU.
    * In that case we need to slice the first AU to keep the 20 ms cut precision */
    if( ( M4DA_StreamTypeAudioAmrNarrowBand
        == pC->pReaderAudioStream->m_basicProperties.m_streamType)
        || (M4DA_StreamTypeAudioEvrc
        == pC->pReaderAudioStream->m_basicProperties.m_streamType) )
    {
        /**
        * If the next frame CTS is lower than the begin cut time,
        * we must read the AU and parse its frames to reach the
        * nearest to the begin cut */
        if( ( iCts + 20) < (M4OSA_Int32)pC->uiBeginCutTime )
        {
            /**
            * Read the first audio AU after the jump */
            err = pC->m_pReaderDataIt->m_pFctGetNextAu(pC->pReaderContext,
                (M4_StreamHandler *)pC->pReaderAudioStream,
                &pC->ReaderAudioAU);

            if( M4WAR_NO_MORE_AU == err )
            {
                M4OSA_TRACE1_0(
                    "M4MCS_intPrepareAudioBeginCut(): m_pReaderDataIt->m_pFctGetNextAu(audio)\
                     returns M4WAR_NO_MORE_AU! Returning M4NO_ERROR");
                return
                    M4NO_ERROR; /**< no fatal error here, we should be able to pursue */
            }
            else if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4MCS_intPrepareAudioBeginCut(): m_pReaderDataIt->m_pFctGetNextAu(Audio)\
                     returns 0x%x",
                    err);
                return err;
            }

            /**
            * While the next AU has a lower CTS than the begin cut time, we advance to
            the next frame */
            while( ( iCts + 20) <= (M4OSA_Int32)pC->uiBeginCutTime )
            {
                /**
                * Get the size of the frame */
                switch( pC->pReaderAudioStream->m_basicProperties.m_streamType )
                {
                    case M4DA_StreamTypeAudioAmrNarrowBand:
                        uiFrameSize = M4MCS_intGetFrameSize_AMRNB(
                            pC->ReaderAudioAU.m_dataAddress);
                        break;

                    case M4DA_StreamTypeAudioEvrc:
                        uiFrameSize = M4MCS_intGetFrameSize_EVRC(
                            pC->ReaderAudioAU.m_dataAddress);
                        break;

                    default:
                        uiFrameSize = 0;
                        break;
                }

                if( 0 == uiFrameSize )
                {
                    /**
                    * Corrupted frame! We get out of this mess!
                    * We don't want to crash here... */
                    M4OSA_TRACE1_0(
                        "M4MCS_intPrepareAudioBeginCut(): \
                        M4MCS_intGetFrameSize_xxx returns 0! Returning M4NO_ERROR");
                    return
                        M4NO_ERROR; /**< no fatal error here, we should be able to pursue */
                }

                /**
                * Go to the next frame */
                pC->ReaderAudioAU.m_dataAddress += uiFrameSize;
                pC->ReaderAudioAU.m_size -= uiFrameSize;

                /**
                * Get the CTS of the next frame */
                iCts += 20; /**< AMR, EVRC frame duration is always 20 ms */
                pC->ReaderAudioAU.m_CTS = iCts;
                pC->ReaderAudioAU.m_DTS = iCts;
            }

            /**
            * Update the audio begin cut offset */
            pC->iAudioCtsOffset = iCts;
        }
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4MCS_intPrepareAudioBeginCut(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intStepEncoding(M4MCS_InternalContext* pC, M4OSA_UInt8* pProgress)
 ******************************************************************************
 */
static M4OSA_ERR M4MCS_intStepEncoding( M4MCS_InternalContext *pC,
                                       M4OSA_UInt8 *pProgress )
{
    M4OSA_ERR err;
    M4OSA_UInt32 uiAudioStepCount = 0;

    /* ---------- VIDEO TRANSCODING ---------- */

    if( ( pC->novideo == M4OSA_FALSE) && (M4MCS_kStreamState_STARTED
        == pC->VideoState) ) /**< If the video encoding is going on */
    {
        if( pC->EncodingVideoFormat == M4ENCODER_kNULL )
        {
            err = M4MCS_intVideoNullEncoding(pC);
        }
        else
        {
            err = M4MCS_intVideoTranscoding(pC);
        }

        /**
        * No more space, quit properly */
        if( M4WAR_WRITER_STOP_REQ == err )
        {
            *pProgress = (M4OSA_UInt8)(( ( (M4OSA_UInt32)pC->dViDecCurrentCts
                - pC->uiBeginCutTime) * 100)
                / (pC->uiEndCutTime - pC->uiBeginCutTime));

            pC->State = M4MCS_kState_FINISHED;

            /* bad file produced on very short 3gp file */
            if( pC->dViDecCurrentCts - pC->uiBeginCutTime == 0 )
            {
                /* Nothing has been encoded -> bad produced file -> error returned */
                M4OSA_TRACE2_0(
                    "M4MCS_intStepEncoding(): video transcoding returns\
                     M4MCS_ERR_OUTPUT_FILE_SIZE_TOO_SMALL");
                return M4MCS_ERR_OUTPUT_FILE_SIZE_TOO_SMALL;
            }
            else
            {
#ifndef M4MCS_AUDIOONLY
                /* clean AIR context needed to keep media aspect ratio*/

                if( M4OSA_NULL != pC->m_air_context )
                {
                    err = M4AIR_cleanUp(pC->m_air_context);

                    if( err != M4NO_ERROR )
                    {
                        M4OSA_TRACE1_1(
                            "M4xVSS_PictureCallbackFct: Error when cleaning AIR: 0x%x",
                            err);
                        return err;
                    }
                    pC->m_air_context = M4OSA_NULL;
                }

#endif /*M4MCS_AUDIOONLY*/

                M4OSA_TRACE2_0(
                    "M4MCS_intStepEncoding(): video transcoding returns M4MCS_ERR_NOMORE_SPACE");
                return M4MCS_ERR_NOMORE_SPACE;
            }
        }

        /**< The input plane is null because the input image will be obtained by the
        VPP filter from the context */
        if( ( M4NO_ERROR != err) && (M4WAR_NO_MORE_AU != err) )
        {
            M4OSA_TRACE1_1(
                "M4MCS_intStepEncoding(): video transcoding returns 0x%x!",
                err);
            return err;
        }
    }

    /* ---------- AUDIO TRANSCODING ---------- */

    if( ( pC->noaudio == M4OSA_FALSE) && (M4MCS_kStreamState_STARTED
        == pC->AudioState) ) /**< If there is an audio stream */
    {
        while(
            /**< If the video encoding is running, encode audio until we reach video time */
            ( ( pC->novideo == M4OSA_FALSE)
            && (M4MCS_kStreamState_STARTED == pC->VideoState)
            && (pC->ReaderAudioAU.m_CTS
            + pC->m_audioAUDuration < pC->ReaderVideoAU.m_CTS)) ||
            /**< If the video encoding is not running, perform 1 step of audio encoding */
            (( M4MCS_kStreamState_STARTED == pC->AudioState)
            && (uiAudioStepCount < 1)) )
        {
            uiAudioStepCount++;

            /**< check if an adio effect has to be applied*/
            err = M4MCS_intCheckAudioEffects(pC);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4MCS_intStepEncoding(): M4MCS_intCheckAudioEffects returns err: 0x%x",
                    err);
                return err;
            }

            if( pC->AudioEncParams.Format == M4ENCODER_kAudioNULL )
            {
                err = M4MCS_intAudioNullEncoding(pC);
            }
            else /**< Audio transcoding */
            {
                err = M4MCS_intAudioTranscoding(pC);
            }

            /**
            * No more space, quit properly */
            if( M4WAR_WRITER_STOP_REQ == err )
            {
                *pProgress =
                    (M4OSA_UInt8)(( ( (M4OSA_UInt32)pC->ReaderAudioAU.m_CTS
                    - pC->uiBeginCutTime) * 100)
                    / (pC->uiEndCutTime - pC->uiBeginCutTime));

                pC->State = M4MCS_kState_FINISHED;

                /* bad file produced on very short 3gp file */
                if( pC->ReaderAudioAU.m_CTS - pC->uiBeginCutTime == 0 )
                {
                    /* Nothing has been encoded -> bad produced file -> error returned */
                    M4OSA_TRACE2_0(
                        "M4MCS_intStepEncoding():\
                         audio transcoding returns M4MCS_ERR_OUTPUT_FILE_SIZE_TOO_SMALL");
                    return M4MCS_ERR_OUTPUT_FILE_SIZE_TOO_SMALL;
                }
                else
                {
#ifndef M4MCS_AUDIOONLY
                    /* clean AIR context needed to keep media aspect ratio*/

                    if( M4OSA_NULL != pC->m_air_context )
                    {
                        err = M4AIR_cleanUp(pC->m_air_context);

                        if( err != M4NO_ERROR )
                        {
                            M4OSA_TRACE1_1(
                                "M4xVSS_PictureCallbackFct: Error when cleaning AIR: 0x%x",
                                err);
                            return err;
                        }
                        pC->m_air_context = M4OSA_NULL;
                    }

#endif /*M4MCS_AUDIOONLY*/

                    M4OSA_TRACE2_0(
                        "M4MCS_intStepEncoding(): \
                        audio transcoding returns M4MCS_ERR_NOMORE_SPACE");
                    return M4MCS_ERR_NOMORE_SPACE;
                }
            }

            if( M4WAR_NO_MORE_AU == err )
            {
                pC->AudioState = M4MCS_kStreamState_FINISHED;
                M4OSA_TRACE3_0(
                    "M4MCS_intStepEncoding(): audio transcoding returns M4WAR_NO_MORE_AU");
                break;
            }
            else if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4MCS_intStepEncoding(): audio transcoding returns 0x%x",
                    err);
                return err;
            }

            /**
            * Check for end cut */
            /* We absolutely want to have less or same audio duration as video ->
            (2*pC->m_audioAUDuration) */
            if( (M4OSA_UInt32)pC->ReaderAudioAU.m_CTS
                + (2 *pC->m_audioAUDuration) > pC->uiEndCutTime )
            {
                pC->AudioState = M4MCS_kStreamState_FINISHED;
                break;
            }
        }
    }

    /* ---------- PROGRESS MANAGEMENT ---------- */

    /**
    * Compute progress */
    if( pC->novideo )
    {
        if( pC->ReaderAudioAU.m_CTS < pC->uiBeginCutTime )
        {
            *pProgress = 0;
        }
        else
        {
            *pProgress = (M4OSA_UInt8)(( ( (M4OSA_UInt32)pC->ReaderAudioAU.m_CTS
                - pC->uiBeginCutTime) * 100)
                / (pC->uiEndCutTime - pC->uiBeginCutTime));
        }
        //printf(": %6.0f\b\b\b\b\b\b\b\b", pC->ReaderAudioAU.m_CTS);

    }
    else
    {
        if( pC->dViDecCurrentCts < pC->uiBeginCutTime )
        {
            *pProgress = 0;
        }
        else
        {
            *pProgress = (M4OSA_UInt8)(( ( (M4OSA_UInt32)pC->dViDecCurrentCts
                - pC->uiBeginCutTime) * 100)
                / (pC->uiEndCutTime - pC->uiBeginCutTime));
        }
        //printf(": %6.0f\b\b\b\b\b\b\b\b", pC->dViDecCurrentCts);
    }

    /**
    * Sanity check */
    if( *pProgress > 99 )
    {
        *pProgress = 99;
    }

    /**
    * Increment CTS for next step */
    if( pC->novideo == M4OSA_FALSE )
    {
        if( pC->EncodingVideoFormat == M4ENCODER_kNULL )
        {
           pC->dViDecCurrentCts +=  1;
        }
        else
        {
            pC->dViDecCurrentCts += pC->dCtsIncrement;
        }
    }

    /**
    * The transcoding is finished when no stream is being encoded anymore */
    if( ( ( pC->novideo) || (M4MCS_kStreamState_FINISHED == pC->VideoState))
        && (( pC->noaudio) || (M4MCS_kStreamState_FINISHED == pC->AudioState)) )
    {
        /* the AIR part can only be used when video codecs are compiled*/
#ifndef M4MCS_AUDIOONLY
        /* clean AIR context needed to keep media aspect ratio*/

        if( M4OSA_NULL != pC->m_air_context )
        {
            err = M4AIR_cleanUp(pC->m_air_context);

            if( err != M4NO_ERROR )
            {
                M4OSA_TRACE1_1(
                    "M4xVSS_PictureCallbackFct: Error when cleaning AIR: 0x%x",
                    err);
                return err;
            }
            pC->m_air_context = M4OSA_NULL;
        }

#endif /*M4MCS_AUDIOONLY*/
        /**/

        *pProgress = 100;
        pC->State = M4MCS_kState_FINISHED;
        M4OSA_TRACE2_0(
            "M4MCS_intStepEncoding(): transcoding finished, returning M4MCS_WAR_TRANSCODING_DONE");
        return M4MCS_WAR_TRANSCODING_DONE;
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4MCS_intStepEncoding(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intStepBeginVideoJump(M4MCS_InternalContext* pC)
 ******************************************************************************
 */
static M4OSA_ERR M4MCS_intStepBeginVideoJump( M4MCS_InternalContext *pC )
{
    M4OSA_ERR err;
    M4OSA_Int32 iCts;

    if( pC->novideo )
    {
        pC->State = M4MCS_kState_BEGINVIDEODECODE;
        return M4NO_ERROR;
    }

    /**
    * Jump to the previous RAP in the clip (first get the time, then jump) */
    iCts = (M4OSA_Int32)pC->dViDecStartingCts;
    err = pC->m_pReader->m_pFctGetPrevRapTime(pC->pReaderContext,
        (M4_StreamHandler *)pC->pReaderVideoStream, &iCts);

    if( M4WAR_READER_INFORMATION_NOT_PRESENT == err )
    {
        /* No RAP table, jump backward and predecode */
        iCts = (M4OSA_Int32)pC->dViDecStartingCts - M4MCS_NO_STSS_JUMP_POINT;

        if( iCts < 0 )
            iCts = 0;
    }
    else if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intStepBeginVideoJump: m_pFctGetPrevRapTime returns 0x%x!",
            err);
        return err;
    }

    /* + CRLV6775 -H.264 Trimming */

    if( M4OSA_TRUE == pC->bH264Trim )
    {

        // Save jump time for safety, this fix should be generic

        M4OSA_Int32 iCtsOri = iCts;


        err = pC->m_pReader->m_pFctJump(pC->pReaderContext,
            (M4_StreamHandler *)pC->pReaderVideoStream, &iCts);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_intStepBeginVideoJump: m_pFctJump(V) returns 0x%x!",
                err);
            return err;
        }

        if( pC->ReaderVideoAU1.m_structSize == 0 )
        {
            /**
            * Initializes an access Unit */
            err = pC->m_pReader->m_pFctFillAuStruct(pC->pReaderContext,
                (M4_StreamHandler *)pC->pReaderVideoStream,
                &pC->ReaderVideoAU1);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4MCS_open(): m_pReader->m_pFctFillAuStruct(video) returns 0x%x",
                    err);
                return err;
            }
            err = pC->m_pReaderDataIt->m_pFctGetNextAu(pC->pReaderContext,
                (M4_StreamHandler *)pC->pReaderVideoStream,
                &pC->ReaderVideoAU1);

            if( M4WAR_NO_MORE_AU == err )
            {
                M4OSA_TRACE2_0(
                    "M4MCS_intVideoNullEncoding(): \
                    m_pReaderDataIt->m_pFctGetNextAu(video) returns M4WAR_NO_MORE_AU");
                /* The audio transcoding is finished */
                pC->VideoState = M4MCS_kStreamState_FINISHED;
                return err;
            }
            else if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4MCS_intVideoNullEncoding():\
                     m_pReaderDataIt->m_pFctGetNextAu(video) returns 0x%x",
                    err);
                return err;
            }

            pC->ReaderVideoAU1.m_structSize = 0;
        }

        err = H264MCS_ProcessSPS_PPS(pC->m_pInstance,
            (M4OSA_UInt8 *)pC->ReaderVideoAU1.m_dataAddress, pC->ReaderVideoAU1.m_size);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_intStepBeginVideoJump: H264MCS_ProcessSPS_PPS returns 0x%x!",
                err);
            return err;
        }


        // Restore jump time for safety, this fix should be generic

        iCts = iCtsOri;


    }
    /* - CRLV6775 -H.264 Trimming */

    /**
    * Decode one step */
    pC->dViDecCurrentCts = (M4OSA_Double)(iCts + pC->iVideoBeginDecIncr);

    /**
    * Be sure we don't decode too far */
    if( pC->dViDecCurrentCts > pC->dViDecStartingCts )
    {
        pC->dViDecCurrentCts = pC->dViDecStartingCts;
    }

    /**
    * Decode at least once with the bJump flag to true */
    M4OSA_TRACE3_1(
        "M4VSS3GPP_intClipDecodeVideoUpToCts: Decoding upTo CTS %.3f",
        pC->dViDecCurrentCts);
    pC->isRenderDup = M4OSA_FALSE;
    err =
        pC->m_pVideoDecoder->m_pFctDecode(pC->pViDecCtxt, &pC->dViDecCurrentCts,
        M4OSA_TRUE, 0);

    if( ( M4NO_ERROR != err) && (M4WAR_NO_MORE_AU != err)
        && (err != M4WAR_VIDEORENDERER_NO_NEW_FRAME) )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intStepBeginVideoJump: m_pFctDecode returns 0x%x!", err);
        return err;
    }

    if( err == M4WAR_VIDEORENDERER_NO_NEW_FRAME )
    {
        M4OSA_TRACE2_0("Decoding output the same frame as before 1");
        pC->isRenderDup = M4OSA_TRUE;
    }

    /**
    * Increment decoding cts for the next step */
    pC->dViDecCurrentCts += (M4OSA_Double)pC->iVideoBeginDecIncr;

    /**
    * Update state automaton */
    if( pC->dViDecCurrentCts > pC->dViDecStartingCts )
    {
        /**
        * Be sure we don't decode too far */
        pC->dViDecCurrentCts = pC->dViDecStartingCts;
        pC->State = M4MCS_kState_PROCESSING;
    }
    else
    {
        pC->State = M4MCS_kState_BEGINVIDEODECODE;
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4MCS_intStepBeginVideoJump(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intStepBeginVideoDecode(M4MCS_InternalContext* pC)
 ******************************************************************************
 */
static M4OSA_ERR M4MCS_intStepBeginVideoDecode( M4MCS_InternalContext *pC )
{
    M4OSA_ERR err;
    M4_MediaTime dDecTarget;

    if( pC->novideo )
    {
        pC->State = M4MCS_kState_PROCESSING;
        return M4NO_ERROR;
    }

    /**
    * Decode */
    dDecTarget = pC->dViDecCurrentCts;
    M4OSA_TRACE3_1("M4MCS_intStepBeginDecode: Decoding upTo CTS %.3f",
        pC->dViDecCurrentCts);
    pC->isRenderDup = M4OSA_FALSE;
    err = pC->m_pVideoDecoder->m_pFctDecode(pC->pViDecCtxt, &dDecTarget,
        M4OSA_FALSE, 0);

    if( ( M4NO_ERROR != err) && (M4WAR_NO_MORE_AU != err)
        && (err != M4WAR_VIDEORENDERER_NO_NEW_FRAME) )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intStepBeginVideoDecode: m_pFctDecode returns 0x%x!", err);
        return err;
    }

    if( err == M4WAR_VIDEORENDERER_NO_NEW_FRAME )
    {
        M4OSA_TRACE2_0("Decoding output the same frame as before 2");
        pC->isRenderDup = M4OSA_TRUE;
    }

    /**
    * Increment decoding cts for the next step */
    pC->dViDecCurrentCts += (M4OSA_Double)pC->iVideoBeginDecIncr;

    /**
    * Update state automaton, if needed */
    if( ( (M4OSA_UInt32)pC->dViDecCurrentCts > pC->dViDecStartingCts)
        || (M4WAR_NO_MORE_AU == err) )
    {
        /**
        * Be sure we don't decode too far */
        pC->dViDecCurrentCts = (M4OSA_Double)pC->dViDecStartingCts;
        pC->State = M4MCS_kState_PROCESSING;
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4MCS_intStepBeginVideoDecode(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/*****************************/
/* define AMR silence frames */
/*****************************/

#define M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_SIZE 13
#define M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_DURATION 160

#ifdef M4VSS3GPP_SILENCE_FRAMES

const M4OSA_UInt8 M4VSS3GPP_AMR_AU_SILENCE_FRAME_048[
    M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_SIZE] =
    {
        0x04, 0xFF, 0x18, 0xC7, 0xF0, 0x0D, 0x04, 0x33, 0xFF, 0xE0, 0x00, 0x00, 0x00
    };
#else

extern
const
M4OSA_UInt8
M4VSS3GPP_AMR_AU_SILENCE_FRAME_048[M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_SIZE];

#endif

/*****************************/
/* define AAC silence frames */
/*****************************/

#define M4VSS3GPP_AAC_AU_SILENCE_MONO_SIZE      4

#ifdef M4VSS3GPP_SILENCE_FRAMES

const M4OSA_UInt8 M4VSS3GPP_AAC_AU_SILENCE_MONO[
    M4VSS3GPP_AAC_AU_SILENCE_MONO_SIZE] =
    {
        0x00, 0xC8, 0x20, 0x07
    };
#else

extern const M4OSA_UInt8
M4VSS3GPP_AAC_AU_SILENCE_MONO[M4VSS3GPP_AAC_AU_SILENCE_MONO_SIZE];

#endif

#define M4VSS3GPP_AAC_AU_SILENCE_STEREO_SIZE        6

#ifdef M4VSS3GPP_SILENCE_FRAMES

const M4OSA_UInt8 M4VSS3GPP_AAC_AU_SILENCE_STEREO[
    M4VSS3GPP_AAC_AU_SILENCE_STEREO_SIZE] =
    {
        0x21, 0x10, 0x03, 0x20, 0x54, 0x1C
    };
#else

extern const
M4OSA_UInt8
M4VSS3GPP_AAC_AU_SILENCE_STEREO[M4VSS3GPP_AAC_AU_SILENCE_STEREO_SIZE];

#endif

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intAudioNullEncoding(M4MCS_InternalContext* pC)
 * @return   M4NO_ERROR:         No error
 ******************************************************************************
 */

static M4OSA_ERR M4MCS_intAudioNullEncoding( M4MCS_InternalContext *pC )
{
    M4OSA_ERR err;

    if( pC->noaudio )
        return M4NO_ERROR;

    /* Check if all audio frame has been written (happens at begin cut) */
    if( pC->ReaderAudioAU.m_size == 0 )
    {
        /**
        * Initializes a new AU if needed */
        if( pC->ReaderAudioAU1.m_structSize == 0 )
        {
            /**
            * Initializes an access Unit */
            err = pC->m_pReader->m_pFctFillAuStruct(pC->pReaderContext,
                (M4_StreamHandler *)pC->pReaderAudioStream,
                &pC->ReaderAudioAU1);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4MCS_open(): m_pReader->m_pFctFillAuStruct(audio) returns 0x%x",
                    err);
                return err;
            }

            pC->m_pDataAddress1 =
                (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(pC->ReaderAudioAU1.m_maxsize,
                M4MCS, (M4OSA_Char *)"Temporary AU1 buffer");

            if( pC->m_pDataAddress1 == M4OSA_NULL )
            {
                M4OSA_TRACE1_0(
                    "M4MCS_intAudioNullEncoding(): allocation error");
                return M4ERR_ALLOC;
            }

            err = pC->m_pReaderDataIt->m_pFctGetNextAu(pC->pReaderContext,
                (M4_StreamHandler *)pC->pReaderAudioStream,
                &pC->ReaderAudioAU1);

            if( M4WAR_NO_MORE_AU == err )
            {
                M4OSA_TRACE2_0(
                    "M4MCS_intAudioNullEncoding():\
                     m_pReaderDataIt->m_pFctGetNextAu(audio) returns M4WAR_NO_MORE_AU");
                /* The audio transcoding is finished */
                pC->AudioState = M4MCS_kStreamState_FINISHED;
                return err;
            }
            else if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4MCS_intAudioNullEncoding(): \
                    m_pReaderDataIt->m_pFctGetNextAu(Audio) returns 0x%x",
                    err);
                return err;
            }
            /*FB 2009.04.02: PR surnxp#616: Crash in MCS while Audio AU copying ,
             constant memory reader case*/
            if( pC->ReaderAudioAU1.m_maxsize
        > pC->pReaderAudioStream->m_basicProperties.m_maxAUSize )
            {
                /* Constant memory reader case, we need to reallocate the temporary buffers */
                M4MCS_intReallocTemporaryAU((M4OSA_MemAddr8
                    *) &(pC->m_pDataAddress1), pC->ReaderAudioAU1.m_maxsize);
                /* pC->m_pDataAddress1 and
                pC->m_pDataAddress2 must be reallocated at the same time */
                /* because pC->pReaderAudioStream->m_basicProperties.m_maxAUSize take
                 maximum value. Then the test "if(pC->ReaderAudioAU?.m_maxsize >
                  pC->pReaderAudioStream->m_basicProperties.m_maxAUSize)" is never true */
                /* and the size of the second buffer is never changed. */
                M4MCS_intReallocTemporaryAU((M4OSA_MemAddr8
                    *) &(pC->m_pDataAddress2), pC->ReaderAudioAU1.m_maxsize);
                /* pC->m_pDataAddress1 and
                pC->m_pDataAddress2 must be reallocated at the same time */
                /* Update stream properties */
                pC->pReaderAudioStream->m_basicProperties.m_maxAUSize =
                    pC->ReaderAudioAU1.m_maxsize;
            }
            /**/
            memcpy((void *)pC->m_pDataAddress1,
                (void *)pC->ReaderAudioAU1.m_dataAddress,
                pC->ReaderAudioAU1.m_size);
        }

        if( pC->ReaderAudioAU2.m_structSize == 0 )
        {
            /**
            * Initializes an access Unit */
            err = pC->m_pReader->m_pFctFillAuStruct(pC->pReaderContext,
                (M4_StreamHandler *)pC->pReaderAudioStream,
                &pC->ReaderAudioAU2);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4MCS_open(): m_pReader->m_pFctFillAuStruct(audio) returns 0x%x",
                    err);
                return err;
            }
            pC->m_pDataAddress2 =
                (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(pC->ReaderAudioAU2.m_maxsize,
                M4MCS, (M4OSA_Char *)"Temporary AU buffer");

            if( pC->m_pDataAddress2 == M4OSA_NULL )
            {
                M4OSA_TRACE1_0(
                    "M4MCS_intAudioNullEncoding(): allocation error");
                return M4ERR_ALLOC;
            }
        }
        /**
        * Read the next audio AU in the input file */
        if( pC->ReaderAudioAU2.m_CTS > pC->ReaderAudioAU1.m_CTS )
        {
            memcpy((void *) &pC->ReaderAudioAU,
                (void *) &pC->ReaderAudioAU2, sizeof(M4_AccessUnit));
            err = pC->m_pReaderDataIt->m_pFctGetNextAu(pC->pReaderContext,
                (M4_StreamHandler *)pC->pReaderAudioStream,
                &pC->ReaderAudioAU1);

            if( pC->ReaderAudioAU1.m_maxsize
                > pC->pReaderAudioStream->m_basicProperties.m_maxAUSize )
            {
                /* Constant memory reader case, we need to reallocate the temporary buffers */
                M4MCS_intReallocTemporaryAU((M4OSA_MemAddr8
                    *) &(pC->m_pDataAddress1), pC->ReaderAudioAU1.m_maxsize);
                /*   pC->m_pDataAddress1
                 * and pC->m_pDataAddress2 must be reallocated at the same time *
                 * because pC->pReaderAudioStream->m_basicProperties.m_maxAUSize take
                 * maximum value. Then the test "if(pC->ReaderAudioAU?.m_maxsize >
                 * pC->pReaderAudioStream->m_basicProperties.m_maxAUSize)" is never true *
                 * and the size of the second buffer is never changed.
                 */
                M4MCS_intReallocTemporaryAU((M4OSA_MemAddr8
                    *) &(pC->m_pDataAddress2), pC->ReaderAudioAU1.m_maxsize);
                /* pC->m_pDataAddress1 and
                 * pC->m_pDataAddress2 must be reallocated at the same time
                 * Update stream properties
                 */
                pC->pReaderAudioStream->m_basicProperties.m_maxAUSize =
                    pC->ReaderAudioAU1.m_maxsize;
            }
            /**/
            memcpy((void *)pC->m_pDataAddress1,
                (void *)pC->ReaderAudioAU1.m_dataAddress,
                pC->ReaderAudioAU1.m_size);
            pC->m_audioAUDuration =
                pC->ReaderAudioAU1.m_CTS - pC->ReaderAudioAU2.m_CTS;
            pC->ReaderAudioAU.m_dataAddress = pC->m_pDataAddress2;
        }
        else
        {
            memcpy((void *) &pC->ReaderAudioAU,
                (void *) &pC->ReaderAudioAU1, sizeof(M4_AccessUnit));
            err = pC->m_pReaderDataIt->m_pFctGetNextAu(pC->pReaderContext,
                (M4_StreamHandler *)pC->pReaderAudioStream,
                &pC->ReaderAudioAU2);
            /* Crash in MCS while Audio AU copying ,
             * constant memory reader case
             */
            if( pC->ReaderAudioAU2.m_maxsize
                > pC->pReaderAudioStream->m_basicProperties.m_maxAUSize )
            {
                /* Constant memory reader case, we need to reallocate the temporary buffers */
                M4MCS_intReallocTemporaryAU((M4OSA_MemAddr8
                    *) &(pC->m_pDataAddress2), pC->ReaderAudioAU2.m_maxsize);
                /* pC->m_pDataAddress1 and
                 * pC->m_pDataAddress2 must be reallocated at the same time
                 * because pC->pReaderAudioStream->m_basicProperties.m_maxAUSize take maximum
                 * value. Then the test "if(pC->ReaderAudioAU?.m_maxsize > pC->pReaderAudioStream->
                 * m_basicProperties.m_maxAUSize)" is never true
                 * and the size of the second buffer is never changed.
                 */
                M4MCS_intReallocTemporaryAU((M4OSA_MemAddr8
                    *) &(pC->m_pDataAddress1), pC->ReaderAudioAU2.m_maxsize);
                /* [ END ] 20091008  JFV PR fix surnxpsw#1071: pC->m_pDataAddress1 and
                 pC->m_pDataAddress2 must be reallocated at the same time */
                /* Update stream properties */
                pC->pReaderAudioStream->m_basicProperties.m_maxAUSize =
                    pC->ReaderAudioAU2.m_maxsize;
            }
            /**/
            memcpy((void *)pC->m_pDataAddress2,
                (void *)pC->ReaderAudioAU2.m_dataAddress,
                pC->ReaderAudioAU2.m_size);
            pC->m_audioAUDuration =
                pC->ReaderAudioAU2.m_CTS - pC->ReaderAudioAU1.m_CTS;
            pC->ReaderAudioAU.m_dataAddress = pC->m_pDataAddress1;
        }

        if( M4WAR_NO_MORE_AU == err )
        {
            M4OSA_TRACE2_0(
                "M4MCS_intAudioNullEncoding(): \
                m_pReaderDataIt->m_pFctGetNextAu(audio) returns M4WAR_NO_MORE_AU");
            /* The audio transcoding is finished */
            pC->AudioState = M4MCS_kStreamState_FINISHED;
            return err;
        }
        else if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_intAudioNullEncoding(): \
                m_pReaderDataIt->m_pFctGetNextAu(Audio) returns 0x%x",
                err);
            return err;
        }
    }

    /**
    * Prepare the writer AU */
    err = pC->pWriterDataFcts->pStartAU(pC->pWriterContext,
        M4MCS_WRITER_AUDIO_STREAM_ID, &pC->WriterAudioAU);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intAudioNullEncoding(): pWriterDataFcts->pStartAU(Audio) returns 0x%x",
            err);
        return err;
    }

    if( pC->uiAudioAUCount
        == 0 ) /* If it is the first AU, we set it to silence
        (else, errors 0x3841, 0x3847 in our AAC decoder) */
    {
        if( pC->InputFileProperties.AudioStreamType == M4VIDEOEDITING_kAAC
            || pC->InputFileProperties.AudioStreamType
            == M4VIDEOEDITING_kAACplus
            || pC->InputFileProperties.AudioStreamType
            == M4VIDEOEDITING_keAACplus )
        {
            if( pC->InputFileProperties.uiNbChannels == 1 )
            {
                pC->WriterAudioAU.size = M4VSS3GPP_AAC_AU_SILENCE_MONO_SIZE;
                memcpy((void *)pC->WriterAudioAU.dataAddress,
                    (void *)M4VSS3GPP_AAC_AU_SILENCE_MONO,
                    pC->WriterAudioAU.size);
            }
            else if( pC->InputFileProperties.uiNbChannels == 2 )
            {
                pC->WriterAudioAU.size = M4VSS3GPP_AAC_AU_SILENCE_STEREO_SIZE;
                memcpy((void *)pC->WriterAudioAU.dataAddress,
                    (void *)M4VSS3GPP_AAC_AU_SILENCE_STEREO,
                    pC->WriterAudioAU.size);
            }
            else
            {
                /* Must never happen ...*/
                M4OSA_TRACE1_0(
                    "M4MCS_intAudioNullEncoding: Bad number of channels in audio input");
                return M4MCS_ERR_INVALID_INPUT_FILE;
            }
        }
        else if( pC->InputFileProperties.AudioStreamType
            == M4VIDEOEDITING_kAMR_NB )
        {
            pC->WriterAudioAU.size = M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_SIZE;
            memcpy((void *)pC->WriterAudioAU.dataAddress,
                (void *)M4VSS3GPP_AMR_AU_SILENCE_FRAME_048,
                pC->WriterAudioAU.size);
            /* Some remaining AMR AU needs to be copied */
            if( pC->ReaderAudioAU.m_size != 0 )
            {
                /* Update Writer AU */
                pC->WriterAudioAU.size += pC->ReaderAudioAU.m_size;
                memcpy((void *)(pC->WriterAudioAU.dataAddress
                    + M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_SIZE),
                    (void *)pC->ReaderAudioAU.m_dataAddress,
                    pC->ReaderAudioAU.m_size);
            }
        }
        else
        {
            /*MP3 case: copy the AU*/
            M4OSA_TRACE3_1(
                "M4MCS_intAudioNullEncoding(): Copying audio AU: size=%d",
                pC->ReaderAudioAU.m_size);
            memcpy((void *)pC->WriterAudioAU.dataAddress,
                (void *)pC->ReaderAudioAU.m_dataAddress,
                pC->ReaderAudioAU.m_size);
            pC->WriterAudioAU.size = pC->ReaderAudioAU.m_size;
        }
    }
    else
    {
        /**
        * Copy audio data from reader AU to writer AU */
        M4OSA_TRACE3_1(
            "M4MCS_intAudioNullEncoding(): Copying audio AU: size=%d",
            pC->ReaderAudioAU.m_size);
        memcpy((void *)pC->WriterAudioAU.dataAddress,
            (void *)pC->ReaderAudioAU.m_dataAddress,
            pC->ReaderAudioAU.m_size);
        pC->WriterAudioAU.size = pC->ReaderAudioAU.m_size;
    }

    /**
    * Convert CTS unit from milliseconds to timescale */
    pC->WriterAudioAU.CTS =
        (M4OSA_Time)((( pC->ReaderAudioAU.m_CTS - pC->iAudioCtsOffset)
        * (pC->WriterAudioStream.timeScale / 1000.0)));

    if( pC->InputFileProperties.AudioStreamType == M4VIDEOEDITING_kAMR_NB
        && pC->uiAudioAUCount == 0 )
    {
        pC->iAudioCtsOffset -=
            20; /* Duration of a silence AMR AU, to handle the duration of the added
                silence frame */
    }
    pC->WriterAudioAU.nbFrag = 0;
    M4OSA_TRACE3_1("M4MCS_intAudioNullEncoding(): audio AU: CTS=%d ms",
        pC->WriterAudioAU.CTS);

    /**
    * Write it to the output file */
    pC->uiAudioAUCount++;
    err = pC->pWriterDataFcts->pProcessAU(pC->pWriterContext,
        M4MCS_WRITER_AUDIO_STREAM_ID, &pC->WriterAudioAU);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intAudioNullEncoding(): pWriterDataFcts->pProcessAU(Audio) returns 0x%x",
            err);
        return err;
    }

    /* All the audio has been written */
    pC->ReaderAudioAU.m_size = 0;

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4MCS_intAudioNullEncoding(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * @brief    Init Audio Transcoding
 * @return   M4NO_ERROR:         No error
 ******************************************************************************
 */
static M4OSA_ERR M4MCS_intAudioTranscoding( M4MCS_InternalContext *pC )
{
    M4OSA_ERR err;                        /**< General error */

    M4OSA_UInt32
        uiBytesDec; /**< Nb of bytes available in the decoder OUT buffer */
    M4OSA_UInt32
        uiDecoder2Ssrc_NbBytes; /**< Nb of bytes copied into the ssrc IN buffer */

    int ssrcErr;                          /**< Error while ssrc processing */
    M4OSA_UInt32 uiSsrcInSize; /**< Size in bytes of ssrc intput buffer */
    M4OSA_UInt32
        uiSsrcInRoom; /**< Nb of bytes available in the ssrc IN buffer */
    M4OSA_MemAddr8
        pSsrcInput; /**< Pointer to the good buffer location for ssrc input */
    M4OSA_UInt32 uiSsrcOutSize; /**< Size in bytes of ssrc output buffer */
    M4OSA_UInt32
        uiBytesSsrc; /**< Nb of bytes available in the ssrc OUT buffer */

    M4OSA_UInt8
        needChannelConversion; /**< Flag to indicate if a stereo <-> mono conversion is needed */
    M4OSA_UInt32
        uiChannelConvertorCoeff; /**< Multiplicative coefficient if stereo
                                    <-> mono conversion is applied */
    M4OSA_MemAddr8 pChannelConvertorInput =
        M4OSA_NULL; /**< Pointer to the good buffer location for channel convertor input */
    M4OSA_UInt32 uiChannelConvertorNbSamples =
        0; /**< Nb of pcm samples to convert in channel convertor */
    M4OSA_MemAddr8 pChannelConvertorOutput =
        M4OSA_NULL; /**< Pointer to the good buffer location for channel convertor output */

    M4OSA_Time
        frameTimeDelta; /**< Duration of the encoded (then written) data */
    M4OSA_UInt32
        uiEncoderInRoom; /**< Nb of bytes available in the encoder IN buffer */
    M4OSA_UInt32
        uiSsrc2Encoder_NbBytes; /**< Nb of bytes copied from the ssrc OUT buffer */
    M4OSA_MemAddr8
        pEncoderInput; /**< Pointer to the good buffer location for encoder input */
    M4ENCODER_AudioBuffer pEncInBuffer;   /**< Encoder input buffer for api */
    M4ENCODER_AudioBuffer pEncOutBuffer;  /**< Encoder output buffer for api */

    M4OSA_Int16 *tempBuffOut = M4OSA_NULL;
    /*FlB 2009.03.04: apply audio effects if an effect is active*/
    M4OSA_Int8 *pActiveEffectNumber = &(pC->pActiveEffectNumber);

    uint32_t errCode = M4NO_ERROR;

    if( pC->noaudio )
        return M4NO_ERROR;

    /* _________________ */
    /*|                 |*/
    /*| READ AND DECODE |*/
    /*|_________________|*/

    /* Check if we have to empty the decoder out buffer first */
    if( M4OSA_NULL != pC->pPosInDecBufferOut )
    {
        goto m4mcs_intaudiotranscoding_feed_resampler;
    }

    err = pC->m_pAudioDecoder->m_pFctStepAudioDec(pC->pAudioDecCtxt,
        M4OSA_NULL, &pC->AudioDecBufferOut, M4OSA_FALSE);


    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intAudioTranscoding(): m_pAudioDecoder->m_pFctStepAudio returns 0x%x",
            err);
        return err;
    }

#ifdef MCS_DUMP_PCM_TO_FILE

    fwrite(pC->AudioDecBufferOut.m_dataAddress,
        pC->AudioDecBufferOut.m_bufferSize, 1, file_pcm_decoder);

#endif

    pC->m_pAudioDecoder->m_pFctGetOptionAudioDec(pC->pAudioDecCtxt,
           M4AD_kOptionID_GetAudioAUErrCode, (M4OSA_DataOption) &errCode);

    if ( M4WAR_NO_MORE_AU == errCode ) {
        pC->AudioState = M4MCS_kStreamState_FINISHED;
            M4OSA_TRACE2_0(
                "M4MCS_intAudioTranscoding():\
                 m_pReaderDataIt->m_pFctGetNextAu(audio) returns M4WAR_NO_MORE_AU");
            return errCode;
   }

    /* Set the current position in the decoder out buffer */
    pC->pPosInDecBufferOut = pC->AudioDecBufferOut.m_dataAddress;

    /* ________________ */
    /*|                |*/
    /*| FEED RESAMPLER |*/
    /*|________________|*/

m4mcs_intaudiotranscoding_feed_resampler:

    /* Check if we have to empty the ssrc out buffer first */
    if( M4OSA_NULL != pC->pPosInSsrcBufferOut )
    {
        goto m4mcs_intaudiotranscoding_prepare_input_buffer;
    }

    /* Compute number of bytes remaining in the decoder buffer */
    uiSsrcInSize = pC->iSsrcNbSamplIn * sizeof(short)
        * pC->pReaderAudioStream->m_nbChannels;
    uiBytesDec = ( pC->AudioDecBufferOut.m_dataAddress
        + pC->AudioDecBufferOut.m_bufferSize) - pC->pPosInDecBufferOut;

    /* Check if we can feed directly the Ssrc with the decoder out buffer */
    if( ( pC->pPosInSsrcBufferIn == pC->pSsrcBufferIn)
        && (uiBytesDec >= uiSsrcInSize) )
    {
        pSsrcInput = pC->pPosInDecBufferOut;

        /* update data consumed into decoder buffer after resampling */
        if( uiBytesDec == uiSsrcInSize )
            pC->pPosInDecBufferOut = M4OSA_NULL;
        else
            pC->pPosInDecBufferOut += uiSsrcInSize;

        goto m4mcs_intaudiotranscoding_do_resampling;
    }

    /**
    * Compute remaining space in Ssrc buffer in */
    uiSsrcInRoom = ( pC->pSsrcBufferIn + uiSsrcInSize) - pC->pPosInSsrcBufferIn;

    /**
    * Nb of bytes copied is the minimum between nb of bytes remaining in
    * decoder out buffer and space remaining in ssrc in buffer */
    uiDecoder2Ssrc_NbBytes =
        (uiSsrcInRoom < uiBytesDec) ? uiSsrcInRoom : uiBytesDec;

    /**
    * Copy from the decoder out buffer into the Ssrc in buffer */
    memcpy((void *)pC->pPosInSsrcBufferIn, (void *)pC->pPosInDecBufferOut,
        uiDecoder2Ssrc_NbBytes);

    /**
    * Update the position in the decoder out buffer */
    pC->pPosInDecBufferOut += uiDecoder2Ssrc_NbBytes;

    /**
    * Update the position in the Ssrc in buffer */
    pC->pPosInSsrcBufferIn += uiDecoder2Ssrc_NbBytes;

    /**
    * Check if the decoder buffer out is empty */
    if( ( pC->pPosInDecBufferOut - pC->AudioDecBufferOut.m_dataAddress)
        == (M4OSA_Int32)pC->AudioDecBufferOut.m_bufferSize )
    {
        pC->pPosInDecBufferOut = M4OSA_NULL;
    }

    /* Check if the Ssrc in buffer is ready (= full) */
    if( ( pC->pPosInSsrcBufferIn - pC->pSsrcBufferIn)
        < (M4OSA_Int32)uiSsrcInSize )
    {
        goto m4mcs_intaudiotranscoding_end;
    }

    pSsrcInput = pC->pSsrcBufferIn;

    /* update data consumed into ssrc buffer in after resampling (empty) */
    pC->pPosInSsrcBufferIn = pC->pSsrcBufferIn;

    /* ___________________ */
    /*|                   |*/
    /*| DO THE RESAMPLING |*/
    /*|___________________|*/

m4mcs_intaudiotranscoding_do_resampling:

    /**
    * No need for memcopy, we can feed Ssrc directly with the data in the audio
    decoder out buffer*/

    ssrcErr = 0;

    if( pC->pReaderAudioStream->m_nbChannels == 1 )
    {
        tempBuffOut =
            (short *)M4OSA_32bitAlignedMalloc((pC->iSsrcNbSamplOut * sizeof(short) * 2
            * ((*pC).InputFileProperties).uiNbChannels),
            M4VSS3GPP,(M4OSA_Char *) "tempBuffOut");
        memset((void *)tempBuffOut, 0,(pC->iSsrcNbSamplOut * sizeof(short) * 2
            * ((*pC).InputFileProperties).uiNbChannels));

        LVAudioresample_LowQuality((short *)tempBuffOut, (short *)pSsrcInput,
            pC->iSsrcNbSamplOut, pC->pLVAudioResampler);
    }
    else
    {
        memset((void *)pC->pSsrcBufferOut, 0, (pC->iSsrcNbSamplOut * sizeof(short)
            * ((*pC).InputFileProperties).uiNbChannels));

        LVAudioresample_LowQuality((short *)pC->pSsrcBufferOut,
            (short *)pSsrcInput, pC->iSsrcNbSamplOut, pC->pLVAudioResampler);
    }

    if( pC->pReaderAudioStream->m_nbChannels == 1 )
    {
        From2iToMono_16((short *)tempBuffOut, (short *)pC->pSsrcBufferOut,
            (short)pC->iSsrcNbSamplOut);
        free(tempBuffOut);
    }


    if( 0 != ssrcErr )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intAudioTranscoding: SSRC_Process returns 0x%x, \
            returning M4MCS_ERR_AUDIO_CONVERSION_FAILED",
            ssrcErr);
        return M4MCS_ERR_AUDIO_CONVERSION_FAILED;
    }

    pC->pPosInSsrcBufferOut = pC->pSsrcBufferOut;

    /* ______________________ */
    /*|                      |*/
    /*| PREPARE INPUT BUFFER |*/
    /*|______________________|*/

m4mcs_intaudiotranscoding_prepare_input_buffer:

    /* Set the flag for channel conversion requirement */
    if( ( pC->AudioEncParams.ChannelNum == M4ENCODER_kMono)
        && (pC->pReaderAudioStream->m_nbChannels == 2) )
    {
        needChannelConversion = 1;
        uiChannelConvertorCoeff = 4;
    }
    else if( ( pC->AudioEncParams.ChannelNum == M4ENCODER_kStereo)
        && (pC->pReaderAudioStream->m_nbChannels == 1) )
    {
        needChannelConversion = 2;
        uiChannelConvertorCoeff = 1;
    }
    else
    {
        needChannelConversion = 0;
        uiChannelConvertorCoeff = 2;
    }

    /* Compute number of bytes remaining in the Ssrc buffer */
    uiSsrcOutSize = pC->iSsrcNbSamplOut * sizeof(short)
        * pC->pReaderAudioStream->m_nbChannels;
    uiBytesSsrc =
        ( pC->pSsrcBufferOut + uiSsrcOutSize) - pC->pPosInSsrcBufferOut;

    /* Check if the ssrc buffer is full */
    if( pC->pPosInSsrcBufferOut == pC->pSsrcBufferOut )
    {
        uiSsrc2Encoder_NbBytes =
            pC->audioEncoderGranularity * uiChannelConvertorCoeff / 2;

        /* Check if we can feed directly the encoder with the ssrc out buffer */
        if( ( pC->pPosInAudioEncoderBuffer == M4OSA_NULL)
            && (uiBytesSsrc >= uiSsrc2Encoder_NbBytes) )
        {
            /* update position in ssrc out buffer after encoding */
            if( uiBytesSsrc == uiSsrc2Encoder_NbBytes )
                pC->pPosInSsrcBufferOut = M4OSA_NULL;
            else
                pC->pPosInSsrcBufferOut += uiSsrc2Encoder_NbBytes;

            /* mark the encoder buffer ready (= full) */
            pC->pPosInAudioEncoderBuffer =
                pC->pAudioEncoderBuffer + pC->audioEncoderGranularity;

            if( needChannelConversion > 0 )
            {
                /* channel convertor writes directly into encoder buffer */
                pEncoderInput = pC->pAudioEncoderBuffer;

                pChannelConvertorInput = pC->pSsrcBufferOut;
                pChannelConvertorOutput = pC->pAudioEncoderBuffer;
                uiChannelConvertorNbSamples =
                    uiSsrc2Encoder_NbBytes / sizeof(short);

                goto m4mcs_intaudiotranscoding_channel_convertor;
            }
            else
            {
                /* encode directly from ssrc out buffer */
                pEncoderInput = pC->pSsrcBufferOut;

                goto m4mcs_intaudiotranscoding_encode_and_write;
            }
        }
    }

    /**
    * Compute remaining space in encoder buffer in */
    if( pC->pPosInAudioEncoderBuffer == M4OSA_NULL )
    {
        pC->pPosInAudioEncoderBuffer = pC->pAudioEncoderBuffer;
    }

    uiEncoderInRoom = ( pC->pAudioEncoderBuffer + pC->audioEncoderGranularity)
        - pC->pPosInAudioEncoderBuffer;
    pEncoderInput = pC->pAudioEncoderBuffer;

    /**
    * Nb of bytes copied is the minimum between nb of bytes remaining in
    * decoder out buffer and space remaining in ssrc in buffer */
    uiSsrc2Encoder_NbBytes =
        (( uiEncoderInRoom * uiChannelConvertorCoeff / 2) < uiBytesSsrc)
        ? (uiEncoderInRoom * uiChannelConvertorCoeff / 2) : uiBytesSsrc;

    if( needChannelConversion > 0 )
    {
        /* channel convertor writes directly into encoder buffer */
        pChannelConvertorInput = pC->pPosInSsrcBufferOut;
        pChannelConvertorOutput = pC->pPosInAudioEncoderBuffer;
        uiChannelConvertorNbSamples = uiSsrc2Encoder_NbBytes / sizeof(short);
    }
    else
    {
        /* copy from the ssrc out buffer into the encoder in buffer */
        memcpy((void *)pC->pPosInAudioEncoderBuffer, (void *)pC->pPosInSsrcBufferOut,
            uiSsrc2Encoder_NbBytes);
    }

    /* Update position in ssrc out buffer after encoding */
    pC->pPosInSsrcBufferOut += uiSsrc2Encoder_NbBytes;

    /* Update the position in the encoder in buffer */
    pC->pPosInAudioEncoderBuffer +=
        uiSsrc2Encoder_NbBytes * 2 / uiChannelConvertorCoeff;

    /* Check if the ssrc buffer out is empty */
    if( ( pC->pPosInSsrcBufferOut - pC->pSsrcBufferOut)
        == (M4OSA_Int32)uiSsrcOutSize )
    {
        pC->pPosInSsrcBufferOut = M4OSA_NULL;
    }

    /* go to next statement */
    if( needChannelConversion > 0 )
        goto m4mcs_intaudiotranscoding_channel_convertor;
    else
        goto m4mcs_intaudiotranscoding_encode_and_write;

    /* _________________ */
    /*|                 |*/
    /*| STEREO <-> MONO |*/
    /*|_________________|*/

m4mcs_intaudiotranscoding_channel_convertor:

    /* convert the input pcm stream to mono or to stereo */
    switch( needChannelConversion )
    {
        case 1: /* stereo to mono */
            From2iToMono_16((short *)pChannelConvertorInput,
                (short *)pChannelConvertorOutput,
                (short)(uiChannelConvertorNbSamples / 2));
            break;

        case 2: /* mono to stereo */
            MonoTo2I_16((short *)pChannelConvertorInput,
                (short *)pChannelConvertorOutput,
                (short)uiChannelConvertorNbSamples);
            break;
    }

    /* __________________ */
    /*|                  |*/
    /*| ENCODE AND WRITE |*/
    /*|__________________|*/

m4mcs_intaudiotranscoding_encode_and_write:

    /* Check if the encoder in buffer is ready (= full) */
    if( ( pC->pPosInAudioEncoderBuffer - pC->pAudioEncoderBuffer)
        < (M4OSA_Int32)pC->audioEncoderGranularity )
    {
        goto m4mcs_intaudiotranscoding_end;
    }

    /* [Mono] or [Stereo interleaved] : all is in one buffer */
    pEncInBuffer.pTableBuffer[0] = pEncoderInput;
    pEncInBuffer.pTableBufferSize[0] = pC->audioEncoderGranularity;
    pEncInBuffer.pTableBuffer[1] = M4OSA_NULL;
    pEncInBuffer.pTableBufferSize[1] = 0;

    /* Time in ms from data size, because it is PCM16 samples */
    frameTimeDelta =
        ( pEncInBuffer.pTableBufferSize[0] * uiChannelConvertorCoeff / 2)
        / sizeof(short) / pC->pReaderAudioStream->m_nbChannels;

    /**
    * Prepare the writer AU */
    err = pC->pWriterDataFcts->pStartAU(pC->pWriterContext,
        M4MCS_WRITER_AUDIO_STREAM_ID, &pC->WriterAudioAU);

    if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intAudioTranscoding(): pWriterDataFcts->pStartAU(Audio) returns 0x%x",
            err);
        return err;
    }

    /*FlB 2009.03.04: apply audio effects if an effect is active*/
    if( *pActiveEffectNumber >= 0 && *pActiveEffectNumber < pC->nbEffects )
    {
        if( pC->pEffects[*pActiveEffectNumber].ExtAudioEffectFct != M4OSA_NULL )
        {
            M4MCS_ExternalProgress pProgress;
            M4OSA_UInt32 tempProgress = 0;
            pProgress.uiClipTime = (M4OSA_UInt32)pC->ReaderAudioAU.m_CTS;

            pProgress.uiOutputTime = ( pC->WriterAudioAU.CTS * 1000)
                / pC->WriterAudioStream.timeScale;
            tempProgress = ( (M4OSA_UInt32)pC->ReaderAudioAU.m_CTS
                - pC->pEffects[*pActiveEffectNumber].uiStartTime
                - pC->uiBeginCutTime) * 1000;
            pProgress.uiProgress =
                (M4OSA_UInt32)(tempProgress / (M4OSA_UInt32)pC->pEffects[
                    *pActiveEffectNumber].uiDuration);

                    err = pC->pEffects[*pActiveEffectNumber].ExtAudioEffectFct(
                        pC->pEffects[*pActiveEffectNumber].pExtAudioEffectFctCtxt,
                        (M4OSA_Int16 *)pEncInBuffer.pTableBuffer[0],
                        pEncInBuffer.pTableBufferSize[0], &pProgress);

                    if( err != M4NO_ERROR )
                    {
                        M4OSA_TRACE1_1(
                            "M4MCS_intAudioTranscoding(): ExtAudioEffectFct() returns 0x%x",
                            err);
                        return err;
                    }
        }
    }

    /**
    * Prepare output buffer */
    pEncOutBuffer.pTableBuffer[0] =
        (M4OSA_MemAddr8)pC->WriterAudioAU.dataAddress;
    pEncOutBuffer.pTableBufferSize[0] = 0;

#ifdef MCS_DUMP_PCM_TO_FILE

    fwrite(pEncInBuffer.pTableBuffer[0], pEncInBuffer.pTableBufferSize[0], 1,
        file_pcm_encoder);

#endif

    if( M4OSA_FALSE == pC->b_isRawWriter )
    {
        /* This allow to write PCM data to file and to encode AMR data,
         when output file is not RAW */
        if( pC->pOutputPCMfile != M4OSA_NULL )
        {
            pC->pOsaFileWritPtr->writeData(pC->pOutputPCMfile,
                pEncInBuffer.pTableBuffer[0], pEncInBuffer.pTableBufferSize[0]);
        }

        /**
        * Encode the PCM audio */
        err = pC->pAudioEncoderGlobalFcts->pFctStep(pC->pAudioEncCtxt,
            &pEncInBuffer, &pEncOutBuffer);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_intAudioTranscoding(): pAudioEncoderGlobalFcts->pFctStep returns 0x%x",
                err);
            return err;
        }

        /* update data consumed into encoder buffer in after encoding (empty) */
        pC->pPosInAudioEncoderBuffer = M4OSA_NULL;

        /**
        * Set AU cts and size */
        pC->WriterAudioAU.size =
            pEncOutBuffer.
            pTableBufferSize[0]; /**< Get the size of encoded data */
        pC->WriterAudioAU.CTS += frameTimeDelta;

        /**
        * Update duration of the encoded AU */
        pC->m_audioAUDuration =
            ( frameTimeDelta * 1000) / pC->WriterAudioStream.timeScale;

        /**
        * Write the encoded AU to the output file */
        pC->uiAudioAUCount++;
        err = pC->pWriterDataFcts->pProcessAU(pC->pWriterContext,
            M4MCS_WRITER_AUDIO_STREAM_ID, &pC->WriterAudioAU);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_intAudioTranscoding(): pWriterDataFcts->pProcessAU(Audio) returns 0x%x",
                err);
            return err;
        }
    }
    else
    {
        /* update data consumed into encoder buffer in after encoding (empty) */
        pC->pPosInAudioEncoderBuffer = M4OSA_NULL;

        pC->WriterAudioAU.dataAddress =
            (M4OSA_MemAddr32)
            pEncoderInput; /* will be converted back to u8* in file write */
        pC->WriterAudioAU.size = pC->audioEncoderGranularity;
        pC->uiAudioAUCount++;

        err = pC->pWriterDataFcts->pProcessAU(pC->pWriterContext,
            M4MCS_WRITER_AUDIO_STREAM_ID, &pC->WriterAudioAU);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_intAudioTranscoding(): pWriterDataFcts->pProcessAU(Audio) returns 0x%x",
                err);
            return err;
        }
    }

    /* _______________ */
    /*|               |*/
    /*| ONE PASS DONE |*/
    /*|_______________|*/

m4mcs_intaudiotranscoding_end:

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4MCS_intAudioTranscoding(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intReallocTemporaryAU(M4OSA_MemAddr8* addr, M4OSA_UInt32 newSize)
 * Used only in case of 3GP constant memory reader, to be able to realloc temporary AU
 * because max AU size can be reevaluated during reading
 * @return   M4NO_ERROR:         No error
 ******************************************************************************
 */
static M4OSA_ERR M4MCS_intReallocTemporaryAU( M4OSA_MemAddr8 *addr,
                                             M4OSA_UInt32 newSize )
{
    if( *addr != M4OSA_NULL )
    {
        free(*addr);
        *addr = (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(newSize, M4MCS,
            (M4OSA_Char *)"Reallocation of temporary AU buffer");

        if( *addr == M4OSA_NULL )
        {
            return M4ERR_ALLOC;
        }
    }

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intVideoNullEncoding(M4MCS_InternalContext* pC)
 * @author   Alexis Vapillon (NXP Software Vision)
 * @return   M4NO_ERROR:         No error
 ******************************************************************************
 */
static M4OSA_ERR M4MCS_intVideoNullEncoding( M4MCS_InternalContext *pC )
{
    M4OSA_ERR err = M4NO_ERROR;
    /* Duration of the AU (find the next AU duration
     * to obtain a more precise video end cut)
     */
    M4OSA_UInt32 videoAUDuration = 0;

    M4OSA_MemAddr8 WritebufferAdd = M4OSA_NULL;
    M4OSA_Int32 lastdecodedCTS = 0;
    M4_AccessUnit lReaderVideoAU; /**< Read video access unit */

    if( pC->novideo )
        return M4NO_ERROR;

    /* H.264 Trimming */
    if( ( ( pC->bH264Trim == M4OSA_TRUE)
        && (pC->uiVideoAUCount < pC->m_pInstance->clip_sps.num_ref_frames)
        && (pC->uiBeginCutTime > 0))
        || (( pC->uiVideoAUCount == 0) && (pC->uiBeginCutTime > 0)) )
    {
        err = M4MCS_intVideoTranscoding(pC);
        return err;
    }


    if((pC->bLastDecodedFrameCTS == M4OSA_FALSE) && (pC->uiBeginCutTime > 0))
    {
        // StageFright encoder does prefetch, the one frame we requested will not be written until
        // the encoder is closed, so do it now rather than in MCS_close
        if( ( M4NO_ERROR != err)
            || (M4MCS_kEncoderRunning != pC->encoderState) )
        {
            M4OSA_TRACE1_2(
                "!!! M4MCS_intVideoNullEncoding ERROR : M4MCS_intVideoTranscoding "
                "returns 0x%X w/ encState=%d", err, pC->encoderState);

            return err;
        }

        /* Stop and close the encoder now to flush the frame (prefetch) */
        if( pC->pVideoEncoderGlobalFcts->pFctStop != M4OSA_NULL )
        {
            err = pC->pVideoEncoderGlobalFcts->pFctStop(pC->pViEncCtxt);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "!!! M4MCS_intVideoNullEncoding ERROR : encoder stop returns 0x%X",
                    err);
                return err;
            }
        }
        pC->encoderState = M4MCS_kEncoderStopped;
        err = pC->pVideoEncoderGlobalFcts->pFctClose(pC->pViEncCtxt);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "!!! M4MCS_intVideoNullEncoding ERROR : encoder close returns 0x%X",
                err);
            return err;
        }
        pC->encoderState = M4MCS_kEncoderClosed;
    }


    if ((pC->EncodingVideoFormat == M4ENCODER_kNULL)
        && (pC->bLastDecodedFrameCTS == M4OSA_FALSE)
        && (pC->uiBeginCutTime > 0)) {

        pC->bLastDecodedFrameCTS = M4OSA_TRUE;
        err = pC->m_pVideoDecoder->m_pFctGetOption(pC->pViDecCtxt,
            M4DECODER_kOptionID_AVCLastDecodedFrameCTS, &lastdecodedCTS);

        if (M4NO_ERROR != err) {
            M4OSA_TRACE1_1(
                "M4MCS_intVideoNullEncoding: m_pVideoDecoder->m_pFctGetOption returns 0x%x!",
                err);
            return err;
        }
        /* Do not need video decoder any more, need to destroy it. Otherwise it
         * will call reader function which will cause frame lost during triming,
         * since the 3gp reader is shared between MCS and decoder.*/
        if (M4OSA_NULL != pC->pViDecCtxt) {
            err = pC->m_pVideoDecoder->m_pFctDestroy(pC->pViDecCtxt);
            pC->pViDecCtxt = M4OSA_NULL;

            if (M4NO_ERROR != err) {
                M4OSA_TRACE1_1(
                    "M4MCS_intVideoNullEncoding: decoder pFctDestroy returns 0x%x",
                    err);
                return err;
            }
        }

        err = pC->m_pReader->m_pFctJump(pC->pReaderContext,
            (M4_StreamHandler *)pC->pReaderVideoStream, &lastdecodedCTS);

        if (M4NO_ERROR != err) {
            M4OSA_TRACE1_1(
                "M4MCS_intVideoNullEncoding: m_pFctJump(V) returns 0x%x!",
                err);
            return err;
        }


        /* Initializes an access Unit */

        err = pC->m_pReader->m_pFctFillAuStruct(pC->pReaderContext,
            (M4_StreamHandler *)pC->pReaderVideoStream, &lReaderVideoAU);

        if (M4NO_ERROR != err) {
            M4OSA_TRACE1_1(
                "M4MCS_intVideoNullEncoding:m_pReader->m_pFctFillAuStruct(video)\
                returns 0x%x", err);
            return err;
        }

        err = pC->m_pReaderDataIt->m_pFctGetNextAu(pC->pReaderContext,
            (M4_StreamHandler *)pC->pReaderVideoStream, &lReaderVideoAU);

        if (M4WAR_NO_MORE_AU == err) {
            M4OSA_TRACE2_0(
                "M4MCS_intVideoNullEncoding():\
                 m_pReaderDataIt->m_pFctGetNextAu(video) returns M4WAR_NO_MORE_AU");
            /* The audio transcoding is finished */
            pC->VideoState = M4MCS_kStreamState_FINISHED;
            return err;
        }
        else if (M4NO_ERROR != err) {
            M4OSA_TRACE1_1(
                "M4MCS_intVideoNullEncoding():\
                 m_pReaderDataIt->m_pFctGetNextAu(video) returns 0x%x",
                err);
            return err;
        }

        M4OSA_TRACE1_1(
            "### [TS_CHECK] M4MCS_intVideoNullEncoding  video AU CTS: %d ",
            lReaderVideoAU.m_CTS);


    }


    pC->bLastDecodedFrameCTS = M4OSA_TRUE;


    /* Find the next AU duration to obtain a more precise video end cut*/
    /**
    * Initializes a new AU if needed */

    if (pC->ReaderVideoAU1.m_structSize == 0) {
        /**
        * Initializes an access Unit */
        err = pC->m_pReader->m_pFctFillAuStruct(pC->pReaderContext,
            (M4_StreamHandler *)pC->pReaderVideoStream,
            &pC->ReaderVideoAU1);

        if (M4NO_ERROR != err) {
            M4OSA_TRACE1_1(
                "M4MCS_open(): m_pReader->m_pFctFillAuStruct(video) returns 0x%x",
                err);
            return err;
        }

        pC->m_pDataVideoAddress1 =
            (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(pC->ReaderVideoAU1.m_maxsize, M4MCS,
            (M4OSA_Char *)"Temporary video AU1 buffer");

        if (pC->m_pDataVideoAddress1 == M4OSA_NULL) {
            M4OSA_TRACE1_0("M4MCS_intVideoNullEncoding(): allocation error");
            return M4ERR_ALLOC;
        }

        err = pC->m_pReaderDataIt->m_pFctGetNextAu(pC->pReaderContext,
            (M4_StreamHandler *)pC->pReaderVideoStream,
            &pC->ReaderVideoAU1);

        if( M4WAR_NO_MORE_AU == err )
        {
            M4OSA_TRACE2_0(
                "M4MCS_intVideoNullEncoding():\
                 m_pReaderDataIt->m_pFctGetNextAu(video) returns M4WAR_NO_MORE_AU");
            /* The audio transcoding is finished */
            pC->VideoState = M4MCS_kStreamState_FINISHED;
            return err;
        }
        else if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_intVideoNullEncoding(): m_pReaderDataIt->m_pFctGetNextAu(video)\
                 returns 0x%x", err);
            return err;
        }

        if( pC->ReaderVideoAU1.m_maxsize
            > pC->pReaderVideoStream->m_basicProperties.m_maxAUSize )
        {
            /* Constant memory reader case, we need to reallocate the temporary buffers */
            M4MCS_intReallocTemporaryAU((M4OSA_MemAddr8
                *) &(pC->m_pDataVideoAddress1), pC->ReaderVideoAU1.m_maxsize);
            /* pC->m_pDataVideoAddress1
            and pC->m_pDataVideoAddress2 must be reallocated at the same time */
            /* because pC->pReaderVideoStream->m_basicProperties.m_maxAUSize take maximum value.
             Then the test "if(pC->ReaderVideoAU?.m_maxsize > pC->pReaderVideoStream->
             m_basicProperties.m_maxAUSize)" is never true */
            /* and the size of the second buffer is never changed. */
            M4MCS_intReallocTemporaryAU((M4OSA_MemAddr8
                *) &(pC->m_pDataVideoAddress2), pC->ReaderVideoAU1.m_maxsize);
            /* pC->m_pDataVideoAddress1 and
            pC->m_pDataVideoAddress2 must be reallocated at the same time */
            /* Update stream properties */
            pC->pReaderVideoStream->m_basicProperties.m_maxAUSize =
                pC->ReaderVideoAU1.m_maxsize;
        }
        memcpy((void *)pC->m_pDataVideoAddress1,
            (void *)pC->ReaderVideoAU1.m_dataAddress,
            pC->ReaderVideoAU1.m_size);
    }

    if( pC->ReaderVideoAU2.m_structSize == 0 )
    {
        /**
        * Initializes an access Unit */
        err = pC->m_pReader->m_pFctFillAuStruct(pC->pReaderContext,
            (M4_StreamHandler *)pC->pReaderVideoStream,
            &pC->ReaderVideoAU2);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_open(): m_pReader->m_pFctFillAuStruct(video) returns 0x%x",
                err);
            return err;
        }
        pC->m_pDataVideoAddress2 =
            (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(pC->ReaderVideoAU2.m_maxsize, M4MCS,
            (M4OSA_Char *)"Temporary video AU buffer");

        if( pC->m_pDataVideoAddress2 == M4OSA_NULL )
        {
            M4OSA_TRACE1_0("M4MCS_intVideoNullEncoding(): allocation error");
            return M4ERR_ALLOC;
        }
    }
    /**
    * Read the next video AU in the input file */
    if( pC->ReaderVideoAU2.m_CTS > pC->ReaderVideoAU1.m_CTS )
    {
        memcpy((void *) &pC->ReaderVideoAU,
            (void *) &pC->ReaderVideoAU2, sizeof(M4_AccessUnit));
        err = pC->m_pReaderDataIt->m_pFctGetNextAu(pC->pReaderContext,
            (M4_StreamHandler *)pC->pReaderVideoStream,
            &pC->ReaderVideoAU1);

        if( pC->ReaderVideoAU1.m_maxsize
            > pC->pReaderVideoStream->m_basicProperties.m_maxAUSize )
        {
            /* Constant memory reader case, we need to reallocate the temporary buffers */
            M4MCS_intReallocTemporaryAU((M4OSA_MemAddr8
                *) &(pC->m_pDataVideoAddress1), pC->ReaderVideoAU1.m_maxsize);
            /* pC->m_pDataVideoAddress1 and
             pC->m_pDataVideoAddress2 must be reallocated at the same time */
            /* because pC->pReaderVideoStream->m_basicProperties.m_maxAUSize take maximum value.
             Then the test "if(pC->ReaderVideoAU?.m_maxsize > pC->pReaderVideoStream->
             m_basicProperties.m_maxAUSize)" is never true */
            /* and the size of the second buffer is never changed. */
            M4MCS_intReallocTemporaryAU((M4OSA_MemAddr8
                *) &(pC->m_pDataVideoAddress2), pC->ReaderVideoAU1.m_maxsize);
            /* pC->m_pDataVideoAddress1 and
            pC->m_pDataVideoAddress2 must be reallocated at the same time */
            /* Update stream properties */
            pC->pReaderVideoStream->m_basicProperties.m_maxAUSize =
                pC->ReaderVideoAU1.m_maxsize;
        }
        memcpy((void *)pC->m_pDataVideoAddress1,
            (void *)pC->ReaderVideoAU1.m_dataAddress,
            pC->ReaderVideoAU1.m_size);
        videoAUDuration = pC->ReaderVideoAU1.m_CTS - pC->ReaderVideoAU2.m_CTS;
        pC->ReaderVideoAU.m_dataAddress = pC->m_pDataVideoAddress2;
    }
    else
    {
        memcpy((void *) &pC->ReaderVideoAU,
            (void *) &pC->ReaderVideoAU1, sizeof(M4_AccessUnit));
        err = pC->m_pReaderDataIt->m_pFctGetNextAu(pC->pReaderContext,
            (M4_StreamHandler *)pC->pReaderVideoStream,
            &pC->ReaderVideoAU2);

        if( pC->ReaderVideoAU2.m_maxsize
            > pC->pReaderVideoStream->m_basicProperties.m_maxAUSize )
        {
            /* Constant memory reader case, we need to reallocate the temporary buffers */
            M4MCS_intReallocTemporaryAU((M4OSA_MemAddr8
                *) &(pC->m_pDataVideoAddress2), pC->ReaderVideoAU2.m_maxsize);
            /* pC->m_pDataVideoAddress1 and
             pC->m_pDataVideoAddress2 must be reallocated at the same time */
            /* because pC->pReaderVideoStream->m_basicProperties.m_maxAUSize take maximum value.
             Then the test "if(pC->ReaderVideoAU?.m_maxsize > pC->pReaderVideoStream->
             m_basicProperties.m_maxAUSize)" is never true */
            /* and the size of the second buffer is never changed. */
            M4MCS_intReallocTemporaryAU((M4OSA_MemAddr8
                *) &(pC->m_pDataVideoAddress1), pC->ReaderVideoAU2.m_maxsize);
            /* pC->m_pDataVideoAddress1 and
            pC->m_pDataVideoAddress2 must be reallocated at the same time */
            /* Update stream properties */
            pC->pReaderVideoStream->m_basicProperties.m_maxAUSize =
                pC->ReaderVideoAU2.m_maxsize;
        }
        memcpy((void *)pC->m_pDataVideoAddress2,
            (void *)pC->ReaderVideoAU2.m_dataAddress,
            pC->ReaderVideoAU2.m_size);
        videoAUDuration = pC->ReaderVideoAU2.m_CTS - pC->ReaderVideoAU1.m_CTS;
        pC->ReaderVideoAU.m_dataAddress = pC->m_pDataVideoAddress1;
    }

    if( M4WAR_NO_MORE_AU == err )
    {
        M4OSA_TRACE2_0(
            "M4MCS_intVideoNullEncoding():\
             m_pReaderDataIt->m_pFctGetNextAu(video) returns M4WAR_NO_MORE_AU");
        /* The video transcoding is finished */
        pC->VideoState = M4MCS_kStreamState_FINISHED;
        return err;
    }
    else if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intVideoNullEncoding(): m_pReaderDataIt->m_pFctGetNextAu(Video) returns 0x%x",
            err);
        return err;
    }
    else
    {
        /**
        * Prepare the writer AU */
        err = pC->pWriterDataFcts->pStartAU(pC->pWriterContext,
            M4MCS_WRITER_VIDEO_STREAM_ID, &pC->WriterVideoAU);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_intVideoNullEncoding(): pWriterDataFcts->pStartAU(Video) returns 0x%x",
                err);
            return err;
        }
            /**
            * Copy video data from reader AU to writer AU */
            M4OSA_TRACE3_1(
                "M4MCS_intVideoNullEncoding(): Copying video AU: size=%d",
                pC->ReaderVideoAU.m_size);
            /* + CRLV6775 -H.264 Trimming */
            if( M4OSA_TRUE == pC->bH264Trim )
            {
                if( pC->H264MCSTempBufferSize
                    < (pC->ReaderVideoAU.m_size + 2048) )
                {
                    pC->H264MCSTempBufferSize =
                        (pC->ReaderVideoAU.m_size + 2048);

                    if( pC->H264MCSTempBuffer != M4OSA_NULL )
                    {
                        free(pC->H264MCSTempBuffer);
                    }
                    pC->H264MCSTempBuffer =
                        (M4OSA_UInt8 *)M4OSA_32bitAlignedMalloc(pC->H264MCSTempBufferSize,
                        M4MCS, (M4OSA_Char *)"pC->H264MCSTempBuffer");

                    if( pC->H264MCSTempBuffer == M4OSA_NULL )
                    {
                        M4OSA_TRACE1_0(
                            "M4MCS_intVideoNullEncoding(): allocation error");
                        return M4ERR_ALLOC;
                    }
                }

                pC->H264MCSTempBufferDataSize = pC->H264MCSTempBufferSize;

                err = H264MCS_ProcessNALU(pC->m_pInstance,
                    (M4OSA_UInt8 *)pC->ReaderVideoAU.m_dataAddress,
                    pC->ReaderVideoAU.m_size, pC->H264MCSTempBuffer,
                    (M4OSA_Int32 *)&pC->H264MCSTempBufferDataSize);

                if( pC->m_pInstance->is_done == 1 )
                {
                    M4MCS_convetFromByteStreamtoNALStream(
                        (M4OSA_UInt8 *)pC->ReaderVideoAU.m_dataAddress ,
                        pC->ReaderVideoAU.m_size);

                    memcpy((void *)pC->WriterVideoAU.dataAddress,
                        (void *)(pC->ReaderVideoAU.m_dataAddress + 4),
                        pC->ReaderVideoAU.m_size - 4);
                    pC->WriterVideoAU.size = pC->ReaderVideoAU.m_size - 4;
                    WritebufferAdd =
                        (M4OSA_MemAddr8)pC->WriterVideoAU.dataAddress;
                }
                else
                {
                    memcpy((void *)pC->WriterVideoAU.dataAddress,
                        (void *)(pC->H264MCSTempBuffer + 4),
                        pC->H264MCSTempBufferDataSize - 4);
                    pC->WriterVideoAU.size = pC->H264MCSTempBufferDataSize - 4;
                    WritebufferAdd =
                        (M4OSA_MemAddr8)pC->WriterVideoAU.dataAddress;
                }
            }
            /* H.264 Trimming */
            else
            {
                memcpy((void *)pC->WriterVideoAU.dataAddress,
                    (void *)pC->ReaderVideoAU.m_dataAddress,
                    pC->ReaderVideoAU.m_size);
                pC->WriterVideoAU.size = pC->ReaderVideoAU.m_size;
            }
            /**
            * Convert CTS unit from milliseconds to timescale */
            pC->WriterVideoAU.CTS =
                (M4OSA_Time)((( pC->ReaderVideoAU.m_CTS - pC->dViDecStartingCts)
                * (pC->WriterVideoStream.timeScale / 1000.0)));
            pC->WriterVideoAU.nbFrag = 0;
            pC->WriterVideoAU.attribute = pC->ReaderVideoAU.m_attribute;

            M4OSA_TRACE3_1("M4MCS_intVideoNullEncoding(): video AU: CTS=%d ms",
                pC->WriterVideoAU.CTS);

        /**
        * Write it to the output file */
        pC->uiVideoAUCount++;
        err = pC->pWriterDataFcts->pProcessAU(pC->pWriterContext,
            M4MCS_WRITER_VIDEO_STREAM_ID, &pC->WriterVideoAU);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_intVideoNullEncoding(): pWriterDataFcts->pProcessAU(Video) returns 0x%x",
                err);
            return err;
        }
        /* + CRLV6775 -H.264 Trimming */
        if( M4OSA_TRUE == pC->bH264Trim )
        {
            if( pC->m_pInstance->is_done == 1 )
            {
                memcpy((void *)(WritebufferAdd - 4),
                    (void *)(pC->ReaderVideoAU.m_dataAddress), 4);
            }
            else
            {
                memcpy((void *)(WritebufferAdd - 4),
                    (void *)(pC->H264MCSTempBuffer), 4);
            }
        } /* H.264 Trimming */
    }
    /**
    * Check for end cut. */
    /* Bug fix 11/12/2008: We absolutely want to have less or same video duration ->
    (2*videoAUDuration) to have a more precise end cut*/
    if( pC->ReaderVideoAU.m_CTS + (2 *videoAUDuration) > pC->uiEndCutTime )
    {
        pC->VideoState = M4MCS_kStreamState_FINISHED;
    }

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4MCS_intVideoNullEncoding(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intVideoTranscoding(M4MCS_InternalContext* pC)
 * @author   Alexis Vapillon (NXP Software Vision)
 * @return   M4NO_ERROR:         No error
 ******************************************************************************
 */
static M4OSA_ERR M4MCS_intVideoTranscoding( M4MCS_InternalContext *pC )
{
    M4OSA_ERR err = M4NO_ERROR;
    M4_MediaTime mtTranscodedTime = 0.0;
    M4ENCODER_FrameMode FrameMode;
    M4OSA_Int32 derive = 0;

    /**
    * Get video CTS to decode */
    mtTranscodedTime = pC->dViDecCurrentCts;
    FrameMode = M4ENCODER_kNormalFrame;

    /**
    * Decode video */
    M4OSA_TRACE3_1(
        "M4MCS_intVideoTranscoding(): Calling m_pVideoDecoder->m_pFctDecode(%.2f)",
        mtTranscodedTime);
    pC->isRenderDup = M4OSA_FALSE;
    err = pC->m_pVideoDecoder->m_pFctDecode(pC->pViDecCtxt, &mtTranscodedTime,
        M4OSA_FALSE, 0);

    if( M4WAR_NO_MORE_AU == err )
    {
        FrameMode =
            M4ENCODER_kLastFrame; /**< We will give this value to the encoder to
            ask for the end of the encoding */
        pC->VideoState = M4MCS_kStreamState_FINISHED;
    }
    else if( err == M4WAR_VIDEORENDERER_NO_NEW_FRAME )
    {
        M4OSA_TRACE2_0("Decoding output the same frame as before 3");
        pC->isRenderDup = M4OSA_TRUE;
    }
    else if( M4NO_ERROR != err )
    {
        M4OSA_TRACE1_1(
            "M4MCS_intVideoTranscoding(): m_pVideoDecoder->m_pFctDecode returns 0x%x!",
            err);
        return err;
    }

    /**
    * Check for end cut.
    * We must check here if the end cut is reached, because in that case we must
    * call the last encode step (-> bLastFrame set to true) */
    if( ( pC->dViDecCurrentCts + pC->dCtsIncrement ) >= (pC->uiEndCutTime
        + M4MCS_ABS(pC->dViDecStartingCts - pC->uiBeginCutTime)) )
    {
        FrameMode =
            M4ENCODER_kLastFrame; /**< We will give this value to the encoder to
            ask for the end of the encoding */
        pC->VideoState = M4MCS_kStreamState_FINISHED;
        derive = (M4OSA_Int32)(( pC->dViDecCurrentCts + pC->dCtsIncrement + 0.5)
            - (pC->uiEndCutTime
            + M4MCS_ABS(pC->dViDecStartingCts - pC->uiBeginCutTime)));
    }

    /* Update starting CTS to have a more precise value (
    the begin cut is not a real CTS)*/
    if( pC->uiVideoAUCount == 0 )
    {
        pC->dViDecStartingCts = mtTranscodedTime;
        pC->dViDecCurrentCts = pC->dViDecStartingCts;
    }

    /**
    * Encode video */
    M4OSA_TRACE3_1(
        "M4MCS_intVideoTranscoding(): Calling pVideoEncoderGlobalFcts->pFctEncode with videoCts\
         = %.2f",pC->ReaderVideoAU.m_CTS);
    pC->uiVideoAUCount++;
    /* update the given duration (the begin cut is not a real CTS)*/
    err = pC->pVideoEncoderGlobalFcts->pFctEncode(pC->pViEncCtxt, M4OSA_NULL,
        (pC->dViDecCurrentCts - pC->dViDecStartingCts - (derive >> 1)),
        FrameMode);

    return err;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intGetInputClipProperties(M4MCS_InternalContext* pContext)
 * @author   Dounya Manai (NXP Software Vision)
 * @brief    Retrieve the properties of the audio and video streams from the input file.
 * @param    pContext            (IN) MCS context
 * @return   M4NO_ERROR:         No error
 * @return   M4ERR_PARAMETER:    pContext is M4OSA_NULL (If Debug Level >= 2)
 ******************************************************************************
 */
static M4OSA_ERR M4MCS_intGetInputClipProperties( M4MCS_InternalContext *pC )
{
    M4DECODER_MPEG4_DecoderConfigInfo DecConfInfo;
    M4READER_3GP_H263Properties H263prop;
    M4OSA_ERR err;
    M4OSA_UInt32 videoBitrate;
    M4DECODER_VideoSize videoSize;
    M4_AACType iAacType = 0;

    /**
    * Check input parameters */
    M4OSA_DEBUG_IF2(M4OSA_NULL == pC, M4ERR_PARAMETER,
        "M4MCS_intGetInputClipProperties: pC is M4OSA_NULL");

    /**
    * Reset common characteristics */
    pC->InputFileProperties.bAnalysed = M4OSA_FALSE;
    pC->InputFileProperties.FileType = 0;
    pC->InputFileProperties.Version[0] = M4VIDEOEDITING_VERSION_MAJOR;
    pC->InputFileProperties.Version[1] = M4VIDEOEDITING_VERSION_MINOR;
    pC->InputFileProperties.Version[2] = M4VIDEOEDITING_VERSION_REVISION;
    pC->InputFileProperties.uiClipDuration = 0;

    memset((void *) &pC->InputFileProperties.ftyp,
        0, sizeof(M4VIDEOEDITING_FtypBox));

    /**
    * Reset video characteristics */
    pC->InputFileProperties.VideoStreamType = M4VIDEOEDITING_kNoneVideo;
    pC->InputFileProperties.uiClipVideoDuration = 0;
    pC->InputFileProperties.uiVideoBitrate = 0;
    pC->InputFileProperties.uiVideoMaxAuSize = 0;
    pC->InputFileProperties.uiVideoWidth = 0;
    pC->InputFileProperties.uiVideoHeight = 0;
    pC->InputFileProperties.uiVideoTimeScale = 0;
    pC->InputFileProperties.fAverageFrameRate = 0.0;
    pC->InputFileProperties.uiVideoLevel =
        M4VIDEOEDITING_VIDEO_UNKNOWN_LEVEL;
    pC->InputFileProperties.uiVideoProfile =
        M4VIDEOEDITING_VIDEO_UNKNOWN_PROFILE;
    pC->InputFileProperties.bMPEG4dataPartition = M4OSA_FALSE;
    pC->InputFileProperties.bMPEG4rvlc = M4OSA_FALSE;
    pC->InputFileProperties.bMPEG4resynchMarker = M4OSA_FALSE;

    /**
    * Reset audio characteristics */
    pC->InputFileProperties.AudioStreamType = M4VIDEOEDITING_kNoneAudio;
    pC->InputFileProperties.uiClipAudioDuration = 0;
    pC->InputFileProperties.uiAudioBitrate = 0;
    pC->InputFileProperties.uiAudioMaxAuSize = 0;
    pC->InputFileProperties.uiNbChannels = 0;
    pC->InputFileProperties.uiSamplingFrequency = 0;
    pC->InputFileProperties.uiExtendedSamplingFrequency = 0;
    pC->InputFileProperties.uiDecodedPcmSize = 0;

    /* Reset compatibility chart (not used in MCS) */
    pC->InputFileProperties.bVideoIsEditable = M4OSA_FALSE;
    pC->InputFileProperties.bAudioIsEditable = M4OSA_FALSE;
    pC->InputFileProperties.bVideoIsCompatibleWithMasterClip = M4OSA_FALSE;
    pC->InputFileProperties.bAudioIsCompatibleWithMasterClip = M4OSA_FALSE;

    /**
    * Video stream properties */
    if( M4OSA_NULL != pC->pReaderVideoStream )
    {
        switch( pC->pReaderVideoStream->m_basicProperties.m_streamType )
        {
            case M4DA_StreamTypeVideoMpeg4:
                pC->InputFileProperties.VideoStreamType = M4VIDEOEDITING_kMPEG4;
                break;

            case M4DA_StreamTypeVideoH263:
                pC->InputFileProperties.VideoStreamType = M4VIDEOEDITING_kH263;
                break;

            case M4DA_StreamTypeVideoMpeg4Avc:
                pC->InputFileProperties.VideoStreamType = M4VIDEOEDITING_kH264;
                break;

            case M4DA_StreamTypeUnknown:
            default:
                pC->InputFileProperties.VideoStreamType =
                    M4VIDEOEDITING_kUnsupportedVideo;
                break;
        }

        /* if bitrate not available retrieve an estimation of the overall bitrate */
        pC->InputFileProperties.uiVideoBitrate =
            pC->pReaderVideoStream->m_basicProperties.m_averageBitRate;

        if( 0 == pC->InputFileProperties.uiVideoBitrate )
        {
            pC->m_pReader->m_pFctGetOption(pC->pReaderContext,
                M4READER_kOptionID_Bitrate, &videoBitrate);

            if( M4OSA_NULL != pC->pReaderAudioStream )
            {
                /* we get the overall bitrate, substract the audio bitrate if any */
                videoBitrate -=
                    pC->pReaderAudioStream->m_basicProperties.m_averageBitRate;
            }
            pC->InputFileProperties.uiVideoBitrate = videoBitrate;
        }

        /**
        * Retrieve the Profile & Level */
        if( ( M4VIDEOEDITING_kH263 != pC->InputFileProperties.VideoStreamType)
            && (M4VIDEOEDITING_kH264
            != pC->InputFileProperties.VideoStreamType) )
        {
            /* Use the DSI parsing function from the external video shell decoder.
            See the comments in M4VSS3GPP_ClipAnalysis.c, it's pretty much the
            same issue. */

            err = M4DECODER_EXTERNAL_ParseVideoDSI(pC->pReaderVideoStream->
                m_basicProperties.m_pDecoderSpecificInfo,
                pC->pReaderVideoStream->
                m_basicProperties.m_decoderSpecificInfoSize,
                &DecConfInfo, &videoSize);

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4MCS_intGetInputClipProperties():\
                     M4DECODER_EXTERNAL_ParseVideoDSI returns 0x%08X",
                    err);
                return err;
            }

            pC->pReaderVideoStream->m_videoWidth = videoSize.m_uiWidth;
            pC->pReaderVideoStream->m_videoHeight = videoSize.m_uiHeight;
            pC->InputFileProperties.uiVideoTimeScale = DecConfInfo.uiTimeScale;
            pC->InputFileProperties.bMPEG4dataPartition =
                DecConfInfo.bDataPartition;
            pC->InputFileProperties.bMPEG4rvlc = DecConfInfo.bUseOfRVLC;
            pC->InputFileProperties.bMPEG4resynchMarker =
                DecConfInfo.uiUseOfResynchMarker;

            err = getMPEG4ProfileAndLevel(DecConfInfo.uiProfile,
                        &(pC->InputFileProperties.uiVideoProfile),
                        &(pC->InputFileProperties.uiVideoLevel));
            if ( M4NO_ERROR != err ) {
                M4OSA_TRACE1_1("M4MCS_intGetInputClipProperties():\
                    getMPEG4ProfileAndLevel returns 0x%08X", err);
                return err;
            }
        }
        else if( M4VIDEOEDITING_kH263 ==
            pC->InputFileProperties.VideoStreamType ) {

            err = getH263ProfileAndLevel(pC->pReaderVideoStream->
                        m_basicProperties.m_pDecoderSpecificInfo,
                        pC->pReaderVideoStream->m_basicProperties.m_decoderSpecificInfoSize,
                        &(pC->InputFileProperties.uiVideoProfile),
                        &(pC->InputFileProperties.uiVideoLevel));
            if ( M4NO_ERROR != err ) {
                M4OSA_TRACE1_1("M4MCS_intGetInputClipProperties():\
                    getH263ProfileAndLevel returns 0x%08X", err);
                return err;
            }
            /* For h263 set default timescale : 30000:1001 */
            pC->InputFileProperties.uiVideoTimeScale = 30000;
        }
        else if ( M4VIDEOEDITING_kH264 ==
            pC->InputFileProperties.VideoStreamType ) {

            pC->InputFileProperties.uiVideoTimeScale = 30000;
            err = getAVCProfileAndLevel(pC->pReaderVideoStream->
                        m_basicProperties.m_pDecoderSpecificInfo,
                        pC->pReaderVideoStream->m_basicProperties.m_decoderSpecificInfoSize,
                        &(pC->InputFileProperties.uiVideoProfile),
                        &(pC->InputFileProperties.uiVideoLevel));
            if ( M4NO_ERROR != err ) {
                M4OSA_TRACE1_1("M4MCS_intGetInputClipProperties():\
                    getAVCProfileAndLevel returns 0x%08X", err);
                return err;
            }
        }

        /* Here because width x height is correct only after dsi parsing
        (done in create decoder) */
        pC->InputFileProperties.uiVideoHeight =
            pC->pReaderVideoStream->m_videoHeight;
        pC->InputFileProperties.uiVideoWidth =
            pC->pReaderVideoStream->m_videoWidth;
        pC->InputFileProperties.uiClipVideoDuration =
            (M4OSA_UInt32)pC->pReaderVideoStream->m_basicProperties.m_duration;
        pC->InputFileProperties.fAverageFrameRate =
            pC->pReaderVideoStream->m_averageFrameRate;
        pC->InputFileProperties.uiVideoMaxAuSize =
            pC->pReaderVideoStream->m_basicProperties.m_maxAUSize;
        pC->InputFileProperties.videoRotationDegrees =
            pC->pReaderVideoStream->videoRotationDegrees;
    }
    else
    {
        if( M4OSA_TRUE == pC->bUnsupportedVideoFound )
        {
            pC->InputFileProperties.VideoStreamType =
                M4VIDEOEDITING_kUnsupportedVideo;
        }
        else
        {
            pC->InputFileProperties.VideoStreamType = M4VIDEOEDITING_kNoneVideo;
        }
    }

    /**
    * Audio stream properties */
    if( M4OSA_NULL != pC->pReaderAudioStream )
    {
        switch( pC->pReaderAudioStream->m_basicProperties.m_streamType )
        {
            case M4DA_StreamTypeAudioAmrNarrowBand:
                pC->InputFileProperties.AudioStreamType =
                    M4VIDEOEDITING_kAMR_NB;
                break;

            case M4DA_StreamTypeAudioAac:
                pC->InputFileProperties.AudioStreamType = M4VIDEOEDITING_kAAC;
                break;

            case M4DA_StreamTypeAudioMp3:
                pC->InputFileProperties.AudioStreamType = M4VIDEOEDITING_kMP3;
                break;

            case M4DA_StreamTypeAudioEvrc:
                pC->InputFileProperties.AudioStreamType = M4VIDEOEDITING_kEVRC;
                break;

            case M4DA_StreamTypeUnknown:
            default:
                pC->InputFileProperties.AudioStreamType =
                    M4VIDEOEDITING_kUnsupportedAudio;
                break;
        }

        if( ( M4OSA_NULL != pC->m_pAudioDecoder)
            && (M4OSA_NULL == pC->pAudioDecCtxt) )
        {
            M4OSA_TRACE3_1(
                "M4MCS_intGetInputClipProperties: calling CreateAudioDecoder, userData= 0x%x",
                pC->m_pCurrentAudioDecoderUserData);

            if( M4OSA_FALSE == pC->bExtOMXAudDecoder ) {
                err = M4MCS_intCheckAndGetCodecProperties(pC);
            }
            else
            {
                err = pC->m_pAudioDecoder->m_pFctCreateAudioDec(
                    &pC->pAudioDecCtxt, pC->pReaderAudioStream,
                    pC->m_pCurrentAudioDecoderUserData);

                if( M4NO_ERROR == err )
                {
                    /* AAC properties*/
                    //get from Reader; temporary, till Audio decoder shell API available to
                    //get the AAC properties
                    pC->AacProperties.aNumChan =
                        pC->pReaderAudioStream->m_nbChannels;
                    pC->AacProperties.aSampFreq =
                        pC->pReaderAudioStream->m_samplingFrequency;

                    err = pC->m_pAudioDecoder->m_pFctGetOptionAudioDec(
                        pC->pAudioDecCtxt, M4AD_kOptionID_StreamType,
                        (M4OSA_DataOption) &iAacType);

                    if( M4NO_ERROR != err )
                    {
                        M4OSA_TRACE1_1(
                            "M4MCS_intGetInputClipProperties:\
                             m_pAudioDecoder->m_pFctGetOptionAudioDec returns err 0x%x",
                            err);
                        iAacType = M4_kAAC; //set to default
                        err = M4NO_ERROR;
                    }
                    else
                    {
                        M4OSA_TRACE3_1(
                            "M4MCS_intGetInputClipProperties:\
                             m_pAudioDecoder->m_pFctGetOptionAudioDec returns streamType %d",
                            iAacType);
                    }

                    switch( iAacType )
                    {
                        case M4_kAAC:
                            pC->AacProperties.aSBRPresent = 0;
                            pC->AacProperties.aPSPresent = 0;
                            break;

                        case M4_kAACplus:
                            pC->AacProperties.aSBRPresent = 1;
                            pC->AacProperties.aPSPresent = 0;
                            pC->AacProperties.aExtensionSampFreq =
                                pC->pReaderAudioStream->
                                m_samplingFrequency; //TODO
                            break;

                        case M4_keAACplus:
                            pC->AacProperties.aSBRPresent = 1;
                            pC->AacProperties.aPSPresent = 1;
                            pC->AacProperties.aExtensionSampFreq =
                                pC->pReaderAudioStream->
                                m_samplingFrequency; //TODO
                            break;
                          case M4_kUnknown:
                          break;
                          default:
                          break;
                        }
                        M4OSA_TRACE3_2(
                            "M4MCS_intGetInputClipProperties: AAC NBChans=%d, SamplFreq=%d",
                            pC->AacProperties.aNumChan,
                            pC->AacProperties.aSampFreq);
                }
            }

            if( M4NO_ERROR != err )
            {
                M4OSA_TRACE1_1(
                    "M4MCS_intGetInputClipProperties:\
                     m_pAudioDecoder->m_pFctCreateAudioDec returns 0x%x",
                    err);
                return err;
            }
        }

        //EVRC
        if( pC->pReaderAudioStream->m_basicProperties.m_streamType
            == M4DA_StreamTypeAudioEvrc )
        {
            /* decoder not implemented yet, provide some default values for the null encoding */
            pC->pReaderAudioStream->m_nbChannels = 1;
            pC->pReaderAudioStream->m_samplingFrequency = 8000;
        }

        /**
        * Bugfix P4ME00001128: With some IMTC files, the AMR bit rate is 0 kbps according
         the GetProperties function */
        if( 0 == pC->pReaderAudioStream->m_basicProperties.m_averageBitRate )
        {
            if( M4VIDEOEDITING_kAMR_NB
                == pC->InputFileProperties.AudioStreamType )
            {
                /**
                * Better returning a guessed 12.2 kbps value than a sure-to-be-false
                0 kbps value! */
                pC->InputFileProperties.uiAudioBitrate =
                    M4VIDEOEDITING_k12_2_KBPS;
            }
            else if( M4VIDEOEDITING_kEVRC
                == pC->InputFileProperties.AudioStreamType )
            {
                /**
                * Better returning a guessed 8.5 kbps value than a sure-to-be-false
                0 kbps value! */
                pC->InputFileProperties.uiAudioBitrate =
                    M4VIDEOEDITING_k9_2_KBPS;
            }
            else
            {
                M4OSA_UInt32 FileBitrate;

                /* Can happen also for aac, in this case we calculate an approximative */
                /* value from global bitrate and video bitrate */
                err = pC->m_pReader->m_pFctGetOption(pC->pReaderContext,
                    M4READER_kOptionID_Bitrate,
                    (M4OSA_DataOption) &FileBitrate);

                if( M4NO_ERROR != err )
                {
                    M4OSA_TRACE1_1(
                        "M4MCS_intGetInputClipProperties: M4READER_kOptionID_Bitrate returns 0x%x",
                        err);
                    return err;
                }
                pC->InputFileProperties.uiAudioBitrate =
                    FileBitrate
                    - pC->
                    InputFileProperties.
                    uiVideoBitrate /* normally setted to 0, if no video */;
            }
        }
        else
        {
            pC->InputFileProperties.uiAudioBitrate =
                pC->pReaderAudioStream->m_basicProperties.m_averageBitRate;
        }

        pC->InputFileProperties.uiNbChannels =
            pC->pReaderAudioStream->m_nbChannels;
        pC->InputFileProperties.uiSamplingFrequency =
            pC->pReaderAudioStream->m_samplingFrequency;
        pC->InputFileProperties.uiClipAudioDuration =
            (M4OSA_UInt32)pC->pReaderAudioStream->m_basicProperties.m_duration;
        pC->InputFileProperties.uiAudioMaxAuSize =
            pC->pReaderAudioStream->m_basicProperties.m_maxAUSize;

        /* Bug: with aac, value is 0 until decoder start() is called */
        pC->InputFileProperties.uiDecodedPcmSize =
            pC->pReaderAudioStream->m_byteFrameLength
            * pC->pReaderAudioStream->m_byteSampleSize
            * pC->pReaderAudioStream->m_nbChannels;

        /* New aac properties */
        if( M4DA_StreamTypeAudioAac
            == pC->pReaderAudioStream->m_basicProperties.m_streamType )
        {
            pC->InputFileProperties.uiNbChannels = pC->AacProperties.aNumChan;
            pC->InputFileProperties.uiSamplingFrequency =
                pC->AacProperties.aSampFreq;

            if( pC->AacProperties.aSBRPresent )
            {
                pC->InputFileProperties.AudioStreamType =
                    M4VIDEOEDITING_kAACplus;
                pC->InputFileProperties.uiExtendedSamplingFrequency =
                    pC->AacProperties.aExtensionSampFreq;
            }

            if( pC->AacProperties.aPSPresent )
            {
                pC->InputFileProperties.AudioStreamType =
                    M4VIDEOEDITING_keAACplus;
            }
        }
    }
    else
    {
        if( M4OSA_TRUE == pC->bUnsupportedAudioFound )
        {
            pC->InputFileProperties.AudioStreamType =
                M4VIDEOEDITING_kUnsupportedAudio;
        }
        else
        {
            pC->InputFileProperties.AudioStreamType = M4VIDEOEDITING_kNoneAudio;
        }
    }

    /* Get 'ftyp' atom */
    err = pC->m_pReader->m_pFctGetOption(pC->pReaderContext,
        M4READER_kOptionID_3gpFtypBox, &pC->InputFileProperties.ftyp);

    /* Analysis is successful */
    if( pC->InputFileProperties.uiClipVideoDuration
        > pC->InputFileProperties.uiClipAudioDuration )
        pC->InputFileProperties.uiClipDuration =
        pC->InputFileProperties.uiClipVideoDuration;
    else
        pC->InputFileProperties.uiClipDuration =
        pC->InputFileProperties.uiClipAudioDuration;

    pC->InputFileProperties.FileType = pC->InputFileType;
    pC->InputFileProperties.bAnalysed = M4OSA_TRUE;

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_UInt32 M4MCS_intGetFrameSize_AMRNB(M4OSA_MemAddr8 pAudioFrame)
 * @brief   Return the length, in bytes, of the AMR Narrow-Band frame contained in the given buffer
 * @note
 * @param   pCpAudioFrame   (IN) AMRNB frame
 * @return  M4NO_ERROR: No error
 ******************************************************************************
 */
static M4OSA_UInt32 M4MCS_intGetFrameSize_AMRNB( M4OSA_MemAddr8 pAudioFrame )
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
                "M4MCS_intGetFrameSize_AMRNB(): Corrupted AMR frame! returning 0.");
            return 0;
    }

    return (1 + (( frameSize + 7) / 8));
}

/**
 ******************************************************************************
 * M4OSA_UInt32 M4MCS_intGetFrameSize_EVRC(M4OSA_MemAddr8 pAudioFrame)
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
static M4OSA_UInt32 M4MCS_intGetFrameSize_EVRC( M4OSA_MemAddr8 pAudioFrame )
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
                "M4MCS_intGetFrameSize_EVRC(): Corrupted EVRC frame! returning 0.");
            return 0;
    }

    return (1 + (( frameSize + 7) / 8));
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intCheckMaxFileSize(M4MCS_Context pContext)
 * @brief    Check if max file size is greater enough to encode a file with the
 *           current selected bitrates and duration.
 * @param    pContext            (IN) MCS context
 * @return   M4NO_ERROR
 * @return   M4MCS_ERR_MAXFILESIZE_TOO_SMALL
 ******************************************************************************
 */
static M4OSA_ERR M4MCS_intCheckMaxFileSize( M4MCS_Context pContext )
{
    M4MCS_InternalContext *pC = (M4MCS_InternalContext *)(pContext);

    M4OSA_UInt32 duration;
    M4OSA_UInt32 audiobitrate;
    M4OSA_UInt32 videobitrate;

    /* free file size : OK */
    if( pC->uiMaxFileSize == 0 )
        return M4NO_ERROR;

    /* duration */
    if( pC->uiEndCutTime == 0 )
    {
        duration = pC->InputFileProperties.uiClipDuration - pC->uiBeginCutTime;
    }
    else
    {
        duration = pC->uiEndCutTime - pC->uiBeginCutTime;
    }

    /* audio bitrate */
    if( pC->noaudio )
    {
        audiobitrate = 0;
    }
    else if( pC->AudioEncParams.Format == M4ENCODER_kAudioNULL )
    {
        audiobitrate = pC->InputFileProperties.uiAudioBitrate;
    }
    else if( pC->uiAudioBitrate == M4VIDEOEDITING_kUndefinedBitrate )
    {
        switch( pC->AudioEncParams.Format )
        {
            case M4ENCODER_kAMRNB:
                audiobitrate = M4VIDEOEDITING_k12_2_KBPS;
                break;
                //EVRC
                //            case M4ENCODER_kEVRC:
                //                audiobitrate = M4VIDEOEDITING_k9_2_KBPS;
                //                break;

            default: /* AAC and MP3*/
                audiobitrate =
                    (pC->AudioEncParams.ChannelNum == M4ENCODER_kMono)
                    ? M4VIDEOEDITING_k16_KBPS : M4VIDEOEDITING_k32_KBPS;
                break;
        }
    }
    else
    {
        audiobitrate = pC->uiAudioBitrate;
    }

    /* video bitrate */
    if( pC->novideo )
    {
        videobitrate = 0;
    }
    else if( pC->EncodingVideoFormat == M4ENCODER_kNULL )
    {
        videobitrate = pC->InputFileProperties.uiVideoBitrate;
    }
    else if( pC->uiVideoBitrate == M4VIDEOEDITING_kUndefinedBitrate )
    {
        videobitrate = M4VIDEOEDITING_k16_KBPS;
    }
    else
    {
        videobitrate = pC->uiVideoBitrate;
    }

    /* max file size */
    if( (M4OSA_UInt32)pC->uiMaxFileSize
        < (M4OSA_UInt32)(M4MCS_MOOV_OVER_FILESIZE_RATIO
        * (audiobitrate + videobitrate) * (duration / 8000.0)) )
        return M4MCS_ERR_MAXFILESIZE_TOO_SMALL;
    else
        return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4VIDEOEDITING_Bitrate M4MCS_intGetNearestBitrate(M4OSA_UInt32 freebitrate, M4OSA_Int8 mode)
 * @brief    Returns the closest bitrate value from the enum list of type M4VIDEOEDITING_Bitrate
 * @param    freebitrate: unsigned int value
 * @param    mode: -1:previous,0:current,1:next
 * @return   bitrate value in enum list M4VIDEOEDITING_Bitrate
 ******************************************************************************
 */
static M4VIDEOEDITING_Bitrate
M4MCS_intGetNearestBitrate( M4OSA_Int32 freebitrate, M4OSA_Int8 mode )
{
    M4OSA_Int32 bitarray [] =
    {
        0, M4VIDEOEDITING_k16_KBPS, M4VIDEOEDITING_k24_KBPS,
        M4VIDEOEDITING_k32_KBPS, M4VIDEOEDITING_k48_KBPS,
        M4VIDEOEDITING_k64_KBPS, M4VIDEOEDITING_k96_KBPS,
        M4VIDEOEDITING_k128_KBPS, M4VIDEOEDITING_k192_KBPS,
        M4VIDEOEDITING_k256_KBPS, M4VIDEOEDITING_k288_KBPS,
        M4VIDEOEDITING_k384_KBPS, M4VIDEOEDITING_k512_KBPS,
        M4VIDEOEDITING_k800_KBPS, M4VIDEOEDITING_k2_MBPS,
        M4VIDEOEDITING_k5_MBPS,
        M4VIDEOEDITING_k8_MBPS, /*+ New Encoder bitrates */
        M4OSA_INT32_MAX
    };

    const M4OSA_UInt32 nbbitrates = 14;
    M4OSA_UInt32 i;

    for ( i = 0; freebitrate >= bitarray[i]; i++ );

    switch( mode )
    {
        case -1: /* previous */
            if( i <= 2 )
                return 0;
            else
                return bitarray[i - 2];
            break;

        case 0: /* current */
            if( i <= 1 )
                return 0;
            else
                return bitarray[i - 1];
            break;

        case 1: /* next */
            if( i >= nbbitrates )
                return M4OSA_INT32_MAX;
            else
                return bitarray[i];
            break;
    }

    return 0;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intCleanUp_ReadersDecoders(M4MCS_InternalContext* pC);
 * @brief    Free all resources allocated by M4MCS_open()
 * @param    pContext            (IN) MCS context
 * @return   M4NO_ERROR:         No error
 ******************************************************************************
 */
static M4OSA_ERR M4MCS_intCleanUp_ReadersDecoders( M4MCS_InternalContext *pC )
{
    M4OSA_ERR err = M4NO_ERROR;

    M4OSA_TRACE2_1("M4MCS_intCleanUp_ReadersDecoders called with pC=0x%x", pC);

    /**/
    /* ----- Free video decoder stuff, if needed ----- */

    if( M4OSA_NULL != pC->pViDecCtxt )
    {
        err = pC->m_pVideoDecoder->m_pFctDestroy(pC->pViDecCtxt);
        pC->pViDecCtxt = M4OSA_NULL;

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_cleanUp: m_pVideoDecoder->pFctDestroy returns 0x%x",
                err);
            /**< don't return, we still have stuff to free */
        }
    }

    /* ----- Free the audio decoder stuff ----- */

    if( M4OSA_NULL != pC->pAudioDecCtxt )
    {
        err = pC->m_pAudioDecoder->m_pFctDestroyAudioDec(pC->pAudioDecCtxt);
        pC->pAudioDecCtxt = M4OSA_NULL;

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_cleanUp: m_pAudioDecoder->m_pFctDestroyAudioDec returns 0x%x",
                err);
            /**< don't return, we still have stuff to free */
        }
    }

    if( M4OSA_NULL != pC->AudioDecBufferOut.m_dataAddress )
    {
        free(pC->AudioDecBufferOut.m_dataAddress);
        pC->AudioDecBufferOut.m_dataAddress = M4OSA_NULL;
    }

    /* ----- Free reader stuff, if needed ----- */
    // We cannot free the reader before decoders because the decoders may read
    // from the reader (in another thread) before being stopped.

    if( M4OSA_NULL != pC->
        pReaderContext ) /**< may be M4OSA_NULL if M4MCS_open was not called */
    {
        err = pC->m_pReader->m_pFctClose(pC->pReaderContext);

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1("M4MCS_cleanUp: m_pReader->m_pFctClose returns 0x%x",
                err);
            /**< don't return, we still have stuff to free */
        }

        err = pC->m_pReader->m_pFctDestroy(pC->pReaderContext);
        pC->pReaderContext = M4OSA_NULL;

        if( M4NO_ERROR != err )
        {
            M4OSA_TRACE1_1(
                "M4MCS_cleanUp: m_pReader->m_pFctDestroy returns 0x%x", err);
            /**< don't return, we still have stuff to free */
        }
    }

    if( pC->m_pDataAddress1 != M4OSA_NULL )
    {
        free(pC->m_pDataAddress1);
        pC->m_pDataAddress1 = M4OSA_NULL;
    }

    if( pC->m_pDataAddress2 != M4OSA_NULL )
    {
        free(pC->m_pDataAddress2);
        pC->m_pDataAddress2 = M4OSA_NULL;
    }
    /*Bug fix 11/12/2008 (to obtain more precise video end cut)*/
    if( pC->m_pDataVideoAddress1 != M4OSA_NULL )
    {
        free(pC->m_pDataVideoAddress1);
        pC->m_pDataVideoAddress1 = M4OSA_NULL;
    }

    if( pC->m_pDataVideoAddress2 != M4OSA_NULL )
    {
        free(pC->m_pDataVideoAddress2);
        pC->m_pDataVideoAddress2 = M4OSA_NULL;
    }

    return M4NO_ERROR;
}


/**

 ******************************************************************************
 * M4OSA_ERR M4MCS_open_normalMode(M4MCS_Context pContext, M4OSA_Void* pFileIn,
 *                             M4OSA_Void* pFileOut, M4OSA_Void* pTempFile);
 * @brief   Set the MCS input and output files. It is the same as M4MCS_open without
 *                                M4MCS_WITH_FAST_OPEN flag
It is used in VideoArtist
 * @note    It opens the input file, but the output file is not created yet.
 * @param   pContext            (IN) MCS context
 * @param   pFileIn             (IN) Input file to transcode (The type of this parameter
 *                                    (URL, pipe...) depends on the OSAL implementation).
 * @param   mediaType           (IN) Container type (.3gp,.amr, ...) of input file.
 * @param   pFileOut            (IN) Output file to create  (The type of this parameter
 *                                (URL, pipe...) depends on the OSAL implementation).
 * @param   pTempFile           (IN) Temporary file for the constant memory writer to store
 *                                 metadata ("moov.bin").
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 * @return  M4ERR_ALLOC:        There is no more available memory
 * @return  M4ERR_FILE_NOT_FOUND:   The input file has not been found
 * @return  M4MCS_ERR_INVALID_INPUT_FILE:   The input file is not a valid file, or is corrupted
 * @return  M4MCS_ERR_INPUT_FILE_CONTAINS_NO_SUPPORTED_STREAM:  The input file contains no
 *                                                         supported audio or video stream
 ******************************************************************************
 */
M4OSA_ERR M4MCS_open_normalMode(M4MCS_Context pContext, M4OSA_Void* pFileIn,
                                 M4VIDEOEDITING_FileType InputFileType,
                                  M4OSA_Void* pFileOut, M4OSA_Void* pTempFile)
{
    M4MCS_InternalContext *pC = (M4MCS_InternalContext*)(pContext);
    M4OSA_ERR err;

    M4READER_MediaFamily mediaFamily;
    M4_StreamHandler* pStreamHandler;

    M4OSA_TRACE2_3("M4MCS_open_normalMode called with pContext=0x%x, pFileIn=0x%x,\
     pFileOut=0x%x", pContext, pFileIn, pFileOut);

    /**
    * Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL == pContext), M4ERR_PARAMETER,
     "M4MCS_open_normalMode: pContext is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL == pFileIn) , M4ERR_PARAMETER,
     "M4MCS_open_normalMode: pFileIn is M4OSA_NULL");

    if ((InputFileType == M4VIDEOEDITING_kFileType_JPG)
        ||(InputFileType == M4VIDEOEDITING_kFileType_PNG)
        ||(InputFileType == M4VIDEOEDITING_kFileType_GIF)
        ||(InputFileType == M4VIDEOEDITING_kFileType_BMP))
    {
        M4OSA_TRACE1_0("M4MCS_open_normalMode: Still picture is not\
             supported with this function");
        return M4MCS_ERR_INPUT_FILE_CONTAINS_NO_SUPPORTED_STREAM;
    }

    /**
    * Check state automaton */
    if (M4MCS_kState_CREATED != pC->State)
    {
        M4OSA_TRACE1_1("M4MCS_open_normalMode(): Wrong State (%d), returning M4ERR_STATE",
             pC->State);
        return M4ERR_STATE;
    }

    /* Copy function input parameters into our context */
    pC->pInputFile     = pFileIn;
    pC->InputFileType  = InputFileType;
    pC->pOutputFile    = pFileOut;
    pC->pTemporaryFile = pTempFile;

    /***********************************/
    /* Open input file with the reader */
    /***********************************/

    err = M4MCS_setCurrentReader(pContext, pC->InputFileType);
    M4ERR_CHECK_RETURN(err);

    /**
    * Reset reader related variables */
    pC->VideoState          = M4MCS_kStreamState_NOSTREAM;
    pC->AudioState          = M4MCS_kStreamState_NOSTREAM;
    pC->pReaderVideoStream  = M4OSA_NULL;
    pC->pReaderAudioStream  = M4OSA_NULL;

    /*******************************************************/
    /* Initializes the reader shell and open the data file */
    /*******************************************************/
    err = pC->m_pReader->m_pFctCreate(&pC->pReaderContext);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("M4MCS_open_normalMode(): m_pReader->m_pFctCreate returns 0x%x", err);
        return err;
    }

    /**
    * Link the reader interface to the reader context */
    pC->m_pReaderDataIt->m_readerContext = pC->pReaderContext;

    /**
    * Set the reader shell file access functions */
    err = pC->m_pReader->m_pFctSetOption(pC->pReaderContext,
         M4READER_kOptionID_SetOsaFileReaderFctsPtr,
        (M4OSA_DataOption)pC->pOsaFileReadPtr);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("M4MCS_open_normalMode(): m_pReader->m_pFctSetOption returns 0x%x", err);
        return err;
    }

    /**
    * Open the input file */
    err = pC->m_pReader->m_pFctOpen(pC->pReaderContext, pC->pInputFile);
    if (M4NO_ERROR != err)
    {
        M4OSA_UInt32 uiDummy, uiCoreId;
        M4OSA_TRACE1_1("M4MCS_open_normalMode(): m_pReader->m_pFctOpen returns 0x%x", err);

        if (err == ((M4OSA_UInt32)M4ERR_UNSUPPORTED_MEDIA_TYPE)) {
            M4OSA_TRACE1_0("M4MCS_open_normalMode(): returning M4MCS_ERR_FILE_DRM_PROTECTED");
            return M4MCS_ERR_FILE_DRM_PROTECTED;
        } else {
            /**
            * If the error is from the core reader, we change it to a public VXS error */
            M4OSA_ERR_SPLIT(err, uiDummy, uiCoreId, uiDummy);
            if (M4MP4_READER == uiCoreId)
            {
                M4OSA_TRACE1_0("M4MCS_open_normalMode(): returning M4MCS_ERR_INVALID_INPUT_FILE");
                return M4MCS_ERR_INVALID_INPUT_FILE;
            }
        }
        return err;
    }

    /**
    * Get the streams from the input file */
    while (M4NO_ERROR == err)
    {
        err = pC->m_pReader->m_pFctGetNextStream(pC->pReaderContext, &mediaFamily,
            &pStreamHandler);

        /**
        * In case we found a BIFS stream or something else...*/
        if((err == ((M4OSA_UInt32)M4ERR_READER_UNKNOWN_STREAM_TYPE))
            || (err == ((M4OSA_UInt32)M4WAR_TOO_MUCH_STREAMS)))
        {
            err = M4NO_ERROR;
            continue;
        }

        if (M4NO_ERROR == err) /**< One stream found */
        {
            /**
            * Found the first video stream */
            if ((M4READER_kMediaFamilyVideo == mediaFamily) \
                && (M4OSA_NULL == pC->pReaderVideoStream))
            {
                if ((M4DA_StreamTypeVideoH263==pStreamHandler->m_streamType) ||
                    (M4DA_StreamTypeVideoMpeg4==pStreamHandler->m_streamType)
#ifdef M4VSS_SUPPORT_VIDEO_AVC
                    ||(M4DA_StreamTypeVideoMpeg4Avc==pStreamHandler->m_streamType))
#else
                    ||((M4DA_StreamTypeVideoMpeg4Avc==pStreamHandler->m_streamType)
                    &&(pC->m_pVideoDecoderItTable[M4DECODER_kVideoTypeAVC] != M4OSA_NULL)))
#endif
                {
                    M4OSA_TRACE3_0("M4MCS_open_normalMode():\
                     Found a H263 or MPEG-4 video stream in input 3gpp clip");

                    /**
                    * Keep pointer to the video stream */
                    pC->pReaderVideoStream = (M4_VideoStreamHandler*)pStreamHandler;
                    pC->bUnsupportedVideoFound = M4OSA_FALSE;
                    pStreamHandler->m_bStreamIsOK = M4OSA_TRUE;

                    /**
                    * Init our video stream state variable */
                    pC->VideoState = M4MCS_kStreamState_STARTED;

                    /**
                    * Reset the stream reader */
                    err = pC->m_pReader->m_pFctReset(pC->pReaderContext,
                         (M4_StreamHandler*)pC->pReaderVideoStream);
                    if (M4NO_ERROR != err)
                    {
                        M4OSA_TRACE1_1("M4MCS_open_normalMode():\
                             m_pReader->m_pFctReset(video) returns 0x%x", err);
                        return err;
                    }

                    /**
                    * Initializes an access Unit */
                    err = pC->m_pReader->m_pFctFillAuStruct(pC->pReaderContext, pStreamHandler,
                         &pC->ReaderVideoAU);
                    if (M4NO_ERROR != err)
                    {
                        M4OSA_TRACE1_1("M4MCS_open_normalMode():\
                             m_pReader->m_pFctFillAuStruct(video) returns 0x%x", err);
                        return err;
                    }
                }
                else /**< Not H263 or MPEG-4 (H264, etc.) */
                {
                    M4OSA_TRACE1_1("M4MCS_open_normalMode():\
                         Found an unsupported video stream (0x%x) in input 3gpp clip",
                             pStreamHandler->m_streamType);

                    pC->bUnsupportedVideoFound = M4OSA_TRUE;
                    pStreamHandler->m_bStreamIsOK = M4OSA_FALSE;
                }
            }
            /**
            * Found the first audio stream */
            else if ((M4READER_kMediaFamilyAudio == mediaFamily)
                && (M4OSA_NULL == pC->pReaderAudioStream))
            {
                if ((M4DA_StreamTypeAudioAmrNarrowBand==pStreamHandler->m_streamType) ||
                    (M4DA_StreamTypeAudioAac==pStreamHandler->m_streamType) ||
                    (M4DA_StreamTypeAudioMp3==pStreamHandler->m_streamType) ||
                    (M4DA_StreamTypeAudioEvrc==pStreamHandler->m_streamType) )
                {
                    M4OSA_TRACE3_0("M4MCS_open_normalMode(): Found an AMR-NB, AAC \
                        or MP3 audio stream in input clip");

                    /**
                    * Keep pointer to the audio stream */
                    pC->pReaderAudioStream = (M4_AudioStreamHandler*)pStreamHandler;
                    pStreamHandler->m_bStreamIsOK = M4OSA_TRUE;
                    pC->bUnsupportedAudioFound = M4OSA_FALSE;

                    /**
                    * Init our audio stream state variable */
                    pC->AudioState = M4MCS_kStreamState_STARTED;

                    /**
                    * Reset the stream reader */
                    err = pC->m_pReader->m_pFctReset(pC->pReaderContext,
                         (M4_StreamHandler*)pC->pReaderAudioStream);
                    if (M4NO_ERROR != err)
                    {
                        M4OSA_TRACE1_1("M4MCS_open_normalMode():\
                             m_pReader->m_pFctReset(audio) returns 0x%x", err);
                        return err;
                    }

                    /**
                    * Initializes an access Unit */
                    err = pC->m_pReader->m_pFctFillAuStruct(pC->pReaderContext, pStreamHandler,
                         &pC->ReaderAudioAU);
                    if (M4NO_ERROR != err)
                    {
                        M4OSA_TRACE1_1("M4MCS_open_normalMode(): \
                            m_pReader->m_pFctFillAuStruct(audio) returns 0x%x", err);
                        return err;
                    }

                    /**
                    * Output max AU size is equal to input max AU size (this value
                    * will be changed if there is audio transcoding) */
                    pC->uiAudioMaxAuSize = pStreamHandler->m_maxAUSize;

                }
                else
                {
                    /**< Not AMR-NB, AAC, MP3 nor EVRC (AMR-WB, WAV...) */
                    M4OSA_TRACE1_1("M4MCS_open_normalMode(): Found an unsupported audio stream\
                         (0x%x) in input 3gpp clip", pStreamHandler->m_streamType);

                    pC->bUnsupportedAudioFound = M4OSA_TRUE;
                    pStreamHandler->m_bStreamIsOK = M4OSA_FALSE;
                }
            }
        }
    } /**< end of while (M4NO_ERROR == err) */

    /**
    * Check we found at least one supported stream */
    if((M4OSA_NULL == pC->pReaderVideoStream) && (M4OSA_NULL == pC->pReaderAudioStream))
    {
        M4OSA_TRACE1_0("M4MCS_open_normalMode(): returning \
            M4MCS_ERR_INPUT_FILE_CONTAINS_NO_SUPPORTED_STREAM");
        return M4MCS_ERR_INPUT_FILE_CONTAINS_NO_SUPPORTED_STREAM;
    }

#ifndef M4VSS_ENABLE_EXTERNAL_DECODERS
    if(pC->VideoState == M4MCS_kStreamState_STARTED)
    {
        err = M4MCS_setCurrentVideoDecoder(pContext,
            pC->pReaderVideoStream->m_basicProperties.m_streamType);
        M4ERR_CHECK_RETURN(err);
    }
#endif

    if(pC->AudioState == M4MCS_kStreamState_STARTED)
    {
        //EVRC
        if(M4DA_StreamTypeAudioEvrc != pStreamHandler->m_streamType)
         /* decoder not supported yet, but allow to do null encoding */
        {
            err = M4MCS_setCurrentAudioDecoder(pContext,
                 pC->pReaderAudioStream->m_basicProperties.m_streamType);
            M4ERR_CHECK_RETURN(err);
        }
    }

    /**
    * Get the audio and video stream properties */
    err = M4MCS_intGetInputClipProperties(pC);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_1("M4MCS_open_normalMode():\
             M4MCS_intGetInputClipProperties returns 0x%x", err);
        return err;
    }

    /**
    * Set the begin cut decoding increment according to the input frame rate */
    if (0. != pC->InputFileProperties.fAverageFrameRate) /**< sanity check */
    {
        pC->iVideoBeginDecIncr = (M4OSA_Int32)(3000. \
            / pC->InputFileProperties.fAverageFrameRate); /**< about 3 frames */
    }
    else
    {
        pC->iVideoBeginDecIncr = 200; /**< default value: 200 milliseconds (3 frames @ 15fps)*/
    }

    /**
    * Update state automaton */
    pC->State = M4MCS_kState_OPENED;

    /**
    * Return with no error */
    M4OSA_TRACE3_0("M4MCS_open_normalMode(): returning M4NO_ERROR");
    return M4NO_ERROR;
}

M4OSA_ERR M4MCS_intCheckAndGetCodecProperties(
                                 M4MCS_InternalContext *pC) {

    M4OSA_ERR err = M4NO_ERROR;
    M4AD_Buffer outputBuffer;
    uint32_t optionValue =0;

    M4OSA_TRACE3_0("M4MCS_intCheckAndGetCodecProperties :start");

    // Decode first audio frame from clip to get properties from codec

    if (M4DA_StreamTypeAudioAac ==
            pC->pReaderAudioStream->m_basicProperties.m_streamType) {

        err = pC->m_pAudioDecoder->m_pFctCreateAudioDec(
                    &pC->pAudioDecCtxt,
                    pC->pReaderAudioStream, &(pC->AacProperties));
    } else {
        err = pC->m_pAudioDecoder->m_pFctCreateAudioDec(
                    &pC->pAudioDecCtxt,
                    pC->pReaderAudioStream,
                    pC->m_pCurrentAudioDecoderUserData);
    }
    if (M4NO_ERROR != err) {

        M4OSA_TRACE1_1(
            "M4MCS_intCheckAndGetCodecProperties: m_pFctCreateAudioDec \
             returns 0x%x", err);
        return err;
    }

    pC->m_pAudioDecoder->m_pFctSetOptionAudioDec(pC->pAudioDecCtxt,
           M4AD_kOptionID_3gpReaderInterface, (M4OSA_DataOption) pC->m_pReaderDataIt);

    pC->m_pAudioDecoder->m_pFctSetOptionAudioDec(pC->pAudioDecCtxt,
           M4AD_kOptionID_AudioAU, (M4OSA_DataOption) &pC->ReaderAudioAU);

    if( pC->m_pAudioDecoder->m_pFctStartAudioDec != M4OSA_NULL ) {

        err = pC->m_pAudioDecoder->m_pFctStartAudioDec(pC->pAudioDecCtxt);
        if( M4NO_ERROR != err ) {

            M4OSA_TRACE1_1(
                "M4MCS_intCheckAndGetCodecProperties: m_pFctStartAudioDec \
                 returns 0x%x", err);
            return err;
        }
    }

    /**
    * Allocate output buffer for the audio decoder */
    outputBuffer.m_bufferSize =
        pC->pReaderAudioStream->m_byteFrameLength
        * pC->pReaderAudioStream->m_byteSampleSize
        * pC->pReaderAudioStream->m_nbChannels;

    if( outputBuffer.m_bufferSize > 0 ) {

        outputBuffer.m_dataAddress =
            (M4OSA_MemAddr8)M4OSA_32bitAlignedMalloc(outputBuffer.m_bufferSize \
            *sizeof(short), M4MCS, (M4OSA_Char *)"outputBuffer.m_bufferSize");

        if( M4OSA_NULL == outputBuffer.m_dataAddress ) {

            M4OSA_TRACE1_0(
                "M4MCS_intCheckAndGetCodecProperties():\
                 unable to allocate outputBuffer.m_dataAddress, returning M4ERR_ALLOC");
            return M4ERR_ALLOC;
        }
    }

    err = pC->m_pAudioDecoder->m_pFctStepAudioDec(pC->pAudioDecCtxt,
        M4OSA_NULL, &outputBuffer, M4OSA_FALSE);

    if ( err == M4WAR_INFO_FORMAT_CHANGE ) {

        // Get the properties from codec node
        pC->m_pAudioDecoder->m_pFctGetOptionAudioDec(pC->pAudioDecCtxt,
           M4AD_kOptionID_AudioNbChannels, (M4OSA_DataOption) &optionValue);

        // Reset Reader structure value also
        pC->pReaderAudioStream->m_nbChannels = optionValue;

        pC->m_pAudioDecoder->m_pFctGetOptionAudioDec(pC->pAudioDecCtxt,
         M4AD_kOptionID_AudioSampFrequency, (M4OSA_DataOption) &optionValue);

        // Reset Reader structure value also
        pC->pReaderAudioStream->m_samplingFrequency = optionValue;

        if (M4DA_StreamTypeAudioAac ==
            pC->pReaderAudioStream->m_basicProperties.m_streamType) {

            pC->AacProperties.aNumChan =
                pC->pReaderAudioStream->m_nbChannels;
            pC->AacProperties.aSampFreq =
                pC->pReaderAudioStream->m_samplingFrequency;

        }

    } else if( err != M4NO_ERROR) {
        M4OSA_TRACE1_1("M4MCS_intCheckAndGetCodecProperties:\
            m_pFctStepAudioDec returns err = 0x%x", err);
    }

    free(outputBuffer.m_dataAddress);

    // Reset the stream reader
    err = pC->m_pReader->m_pFctReset(pC->pReaderContext,
                 (M4_StreamHandler *)pC->pReaderAudioStream);

    if (M4NO_ERROR != err) {
        M4OSA_TRACE1_1("M4MCS_intCheckAndGetCodecProperties\
            Error in reseting reader: 0x%x", err);
    }

    return err;

}

M4OSA_ERR M4MCS_intLimitBitratePerCodecProfileLevel(
                                 M4ENCODER_AdvancedParams* EncParams) {

    M4OSA_ERR err = M4NO_ERROR;

    switch (EncParams->Format) {
        case M4ENCODER_kH263:
            EncParams->Bitrate = M4MCS_intLimitBitrateForH263Enc(
                                     EncParams->videoProfile,
                                     EncParams->videoLevel, EncParams->Bitrate);
            break;

        case M4ENCODER_kMPEG4:
            EncParams->Bitrate = M4MCS_intLimitBitrateForMpeg4Enc(
                                     EncParams->videoProfile,
                                     EncParams->videoLevel, EncParams->Bitrate);
            break;

        case M4ENCODER_kH264:
            EncParams->Bitrate = M4MCS_intLimitBitrateForH264Enc(
                                     EncParams->videoProfile,
                                     EncParams->videoLevel, EncParams->Bitrate);
            break;

        default:
            M4OSA_TRACE1_1("M4MCS_intLimitBitratePerCodecProfileLevel: \
                Wrong enc format %d", EncParams->Format);
            err = M4ERR_PARAMETER;
            break;
    }

    return err;

}

M4OSA_Int32 M4MCS_intLimitBitrateForH264Enc(M4OSA_Int32 profile,
                                    M4OSA_Int32 level, M4OSA_Int32 bitrate) {

    M4OSA_Int32 vidBitrate = 0;

    switch (profile) {
        case OMX_VIDEO_AVCProfileBaseline:
        case OMX_VIDEO_AVCProfileMain:

            switch (level) {

                case OMX_VIDEO_AVCLevel1:
                    vidBitrate = (bitrate > 64000) ? 64000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel1b:
                    vidBitrate = (bitrate > 128000) ? 128000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel11:
                    vidBitrate = (bitrate > 192000) ? 192000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel12:
                    vidBitrate = (bitrate > 384000) ? 384000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel13:
                    vidBitrate = (bitrate > 768000) ? 768000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel2:
                    vidBitrate = (bitrate > 2000000) ? 2000000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel21:
                    vidBitrate = (bitrate > 4000000) ? 4000000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel22:
                    vidBitrate = (bitrate > 4000000) ? 4000000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel3:
                    vidBitrate = (bitrate > 10000000) ? 10000000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel31:
                    vidBitrate = (bitrate > 14000000) ? 14000000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel32:
                    vidBitrate = (bitrate > 20000000) ? 20000000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel4:
                    vidBitrate = (bitrate > 20000000) ? 20000000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel41:
                    vidBitrate = (bitrate > 50000000) ? 50000000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel42:
                    vidBitrate = (bitrate > 50000000) ? 50000000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel5:
                    vidBitrate = (bitrate > 135000000) ? 135000000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel51:
                    vidBitrate = (bitrate > 240000000) ? 240000000 : bitrate;
                    break;

                default:
                    vidBitrate = bitrate;
                    break;
            }
            break;

        case OMX_VIDEO_AVCProfileHigh:
            switch (level) {
                case OMX_VIDEO_AVCLevel1:
                    vidBitrate = (bitrate > 80000) ? 80000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel1b:
                    vidBitrate = (bitrate > 160000) ? 160000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel11:
                    vidBitrate = (bitrate > 240000) ? 240000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel12:
                    vidBitrate = (bitrate > 480000) ? 480000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel13:
                    vidBitrate = (bitrate > 960000) ? 960000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel2:
                    vidBitrate = (bitrate > 2500000) ? 2500000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel21:
                    vidBitrate = (bitrate > 5000000) ? 5000000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel22:
                    vidBitrate = (bitrate > 5000000) ? 5000000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel3:
                    vidBitrate = (bitrate > 12500000) ? 12500000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel31:
                    vidBitrate = (bitrate > 17500000) ? 17500000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel32:
                    vidBitrate = (bitrate > 25000000) ? 25000000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel4:
                    vidBitrate = (bitrate > 25000000) ? 25000000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel41:
                    vidBitrate = (bitrate > 62500000) ? 62500000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel42:
                    vidBitrate = (bitrate > 62500000) ? 62500000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel5:
                    vidBitrate = (bitrate > 168750000) ? 168750000 : bitrate;
                    break;

                case OMX_VIDEO_AVCLevel51:
                    vidBitrate = (bitrate > 300000000) ? 300000000 : bitrate;
                    break;

                default:
                    vidBitrate = bitrate;
                    break;
            }
            break;

        default:
            // We do not handle any other AVC profile for now.
            // Return input bitrate
            vidBitrate = bitrate;
            break;
    }

    return vidBitrate;
}

M4OSA_Int32 M4MCS_intLimitBitrateForMpeg4Enc(M4OSA_Int32 profile,
                                    M4OSA_Int32 level, M4OSA_Int32 bitrate) {

    M4OSA_Int32 vidBitrate = 0;

    switch (profile) {
        case OMX_VIDEO_MPEG4ProfileSimple:
            switch (level) {

                case OMX_VIDEO_MPEG4Level0:
                    vidBitrate = (bitrate > 64000) ? 64000 : bitrate;
                    break;

                case OMX_VIDEO_MPEG4Level0b:
                    vidBitrate = (bitrate > 128000) ? 128000 : bitrate;
                    break;

                case OMX_VIDEO_MPEG4Level1:
                    vidBitrate = (bitrate > 64000) ? 64000 : bitrate;
                    break;

                case OMX_VIDEO_MPEG4Level2:
                    vidBitrate = (bitrate > 128000) ? 128000 : bitrate;
                    break;

                case OMX_VIDEO_MPEG4Level3:
                    vidBitrate = (bitrate > 384000) ? 384000 : bitrate;
                    break;

                default:
                    vidBitrate = bitrate;
                    break;
            }
            break;

        default:
            // We do not handle any other MPEG4 profile for now.
            // Return input bitrate
            vidBitrate = bitrate;
            break;
    }

    return vidBitrate;
}

M4OSA_Int32 M4MCS_intLimitBitrateForH263Enc(M4OSA_Int32 profile,
                                    M4OSA_Int32 level, M4OSA_Int32 bitrate) {

    M4OSA_Int32 vidBitrate = 0;

    switch (profile) {
        case OMX_VIDEO_H263ProfileBaseline:
            switch (level) {

                case OMX_VIDEO_H263Level10:
                    vidBitrate = (bitrate > 64000) ? 64000 : bitrate;
                    break;

                case OMX_VIDEO_H263Level20:
                    vidBitrate = (bitrate > 128000) ? 128000 : bitrate;
                    break;

                case OMX_VIDEO_H263Level30:
                    vidBitrate = (bitrate > 384000) ? 384000 : bitrate;
                    break;

                default:
                    vidBitrate = bitrate;
                    break;
            }
            break;

        default:
            // We do not handle any other H263 profile for now.
            // Return input bitrate
            vidBitrate = bitrate;
            break;
    }

    return vidBitrate;
}
