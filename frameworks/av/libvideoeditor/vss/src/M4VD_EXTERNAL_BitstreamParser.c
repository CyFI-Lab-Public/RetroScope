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
#include "utils/Log.h"
#include "M4OSA_Types.h"
#include "M4OSA_Debug.h"

#include "M4VD_EXTERNAL_Interface.h"
#include "M4VD_Tools.h"
#include "M4_VideoEditingCommon.h"
#include "OMX_Video.h"
/**
 ************************************************************************
 * @file   M4VD_EXTERNAL_BitstreamParser.c
 * @brief
 * @note   This file implements external Bitstream parser
 ************************************************************************
 */

typedef struct {
    M4OSA_UInt8 code;
    M4OSA_Int32 profile;
    M4OSA_Int32 level;
} codeProfileLevel;

static codeProfileLevel mpeg4ProfileLevelTable[] = {
    {0x01, OMX_VIDEO_MPEG4ProfileSimple, OMX_VIDEO_MPEG4Level1},
    {0x02, OMX_VIDEO_MPEG4ProfileSimple, OMX_VIDEO_MPEG4Level2},
    {0x03, OMX_VIDEO_MPEG4ProfileSimple, OMX_VIDEO_MPEG4Level3},
    {0x04, OMX_VIDEO_MPEG4ProfileSimple, OMX_VIDEO_MPEG4Level4a},
    {0x05, OMX_VIDEO_MPEG4ProfileSimple, OMX_VIDEO_MPEG4Level5},
    {0x08, OMX_VIDEO_MPEG4ProfileSimple, OMX_VIDEO_MPEG4Level0},
    {0x11, OMX_VIDEO_MPEG4ProfileSimpleScalable,OMX_VIDEO_MPEG4Level1},
    {0x12, OMX_VIDEO_MPEG4ProfileSimpleScalable,OMX_VIDEO_MPEG4Level2},
    {0x21, OMX_VIDEO_MPEG4ProfileCore, OMX_VIDEO_MPEG4Level1},
    {0x22, OMX_VIDEO_MPEG4ProfileCore, OMX_VIDEO_MPEG4Level2},
    {0x32, OMX_VIDEO_MPEG4ProfileMain, OMX_VIDEO_MPEG4Level2},
    {0x33, OMX_VIDEO_MPEG4ProfileMain, OMX_VIDEO_MPEG4Level3},
    {0x34, OMX_VIDEO_MPEG4ProfileMain, OMX_VIDEO_MPEG4Level4},
    {0x42, OMX_VIDEO_MPEG4ProfileNbit, OMX_VIDEO_MPEG4Level2},
    {0x51, OMX_VIDEO_MPEG4ProfileScalableTexture, OMX_VIDEO_MPEG4Level1},
    {0x61, OMX_VIDEO_MPEG4ProfileSimpleFace, OMX_VIDEO_MPEG4Level1},
    {0x62, OMX_VIDEO_MPEG4ProfileSimpleFace, OMX_VIDEO_MPEG4Level2},
    {0x71, OMX_VIDEO_MPEG4ProfileBasicAnimated, OMX_VIDEO_MPEG4Level1},
    {0x72, OMX_VIDEO_MPEG4ProfileBasicAnimated, OMX_VIDEO_MPEG4Level2},
    {0x81, OMX_VIDEO_MPEG4ProfileHybrid, OMX_VIDEO_MPEG4Level1},
    {0x82, OMX_VIDEO_MPEG4ProfileHybrid, OMX_VIDEO_MPEG4Level2},
    {0x91, OMX_VIDEO_MPEG4ProfileAdvancedRealTime, OMX_VIDEO_MPEG4Level1},
    {0x92, OMX_VIDEO_MPEG4ProfileAdvancedRealTime, OMX_VIDEO_MPEG4Level2},
    {0x93, OMX_VIDEO_MPEG4ProfileAdvancedRealTime, OMX_VIDEO_MPEG4Level3},
    {0x94, OMX_VIDEO_MPEG4ProfileAdvancedRealTime, OMX_VIDEO_MPEG4Level4},
    {0xa1, OMX_VIDEO_MPEG4ProfileCoreScalable, OMX_VIDEO_MPEG4Level1},
    {0xa2, OMX_VIDEO_MPEG4ProfileCoreScalable, OMX_VIDEO_MPEG4Level2},
    {0xa3, OMX_VIDEO_MPEG4ProfileCoreScalable, OMX_VIDEO_MPEG4Level3},
    {0xb1, OMX_VIDEO_MPEG4ProfileAdvancedCoding, OMX_VIDEO_MPEG4Level1},
    {0xb2, OMX_VIDEO_MPEG4ProfileAdvancedCoding, OMX_VIDEO_MPEG4Level2},
    {0xb3, OMX_VIDEO_MPEG4ProfileAdvancedCoding, OMX_VIDEO_MPEG4Level3},
    {0xb4, OMX_VIDEO_MPEG4ProfileAdvancedCoding, OMX_VIDEO_MPEG4Level4},
    {0xc1, OMX_VIDEO_MPEG4ProfileAdvancedCore, OMX_VIDEO_MPEG4Level1},
    {0xc2, OMX_VIDEO_MPEG4ProfileAdvancedCore, OMX_VIDEO_MPEG4Level2},
    {0xd1, OMX_VIDEO_MPEG4ProfileAdvancedScalable, OMX_VIDEO_MPEG4Level1},
    {0xd2, OMX_VIDEO_MPEG4ProfileAdvancedScalable, OMX_VIDEO_MPEG4Level2},
    {0xd3, OMX_VIDEO_MPEG4ProfileAdvancedScalable, OMX_VIDEO_MPEG4Level3},
    {0xf0, OMX_VIDEO_MPEG4ProfileAdvancedSimple, OMX_VIDEO_MPEG4Level0},
    {0xf1, OMX_VIDEO_MPEG4ProfileAdvancedSimple, OMX_VIDEO_MPEG4Level1},
    {0xf2, OMX_VIDEO_MPEG4ProfileAdvancedSimple, OMX_VIDEO_MPEG4Level2},
    {0xf3, OMX_VIDEO_MPEG4ProfileAdvancedSimple, OMX_VIDEO_MPEG4Level3},
    {0xf4, OMX_VIDEO_MPEG4ProfileAdvancedSimple, OMX_VIDEO_MPEG4Level4},
    {0xf5, OMX_VIDEO_MPEG4ProfileAdvancedSimple, OMX_VIDEO_MPEG4Level5}
};

M4OSA_UInt32 M4VD_EXTERNAL_GetBitsFromMemory(M4VS_Bitstream_ctxt* parsingCtxt,
     M4OSA_UInt32 nb_bits)
{
        return(M4VD_Tools_GetBitsFromMemory(parsingCtxt,nb_bits));
}

M4OSA_ERR M4VD_EXTERNAL_WriteBitsToMemory(M4OSA_UInt32 bitsToWrite,
                                                 M4OSA_MemAddr32 dest_bits,
                                                 M4OSA_UInt8 offset, M4OSA_UInt8 nb_bits)
{
        return (M4VD_Tools_WriteBitsToMemory( bitsToWrite,dest_bits,
                                                offset,  nb_bits));
}

M4OSA_ERR M4DECODER_EXTERNAL_ParseVideoDSI(M4OSA_UInt8* pVol, M4OSA_Int32 aVolSize,
                                             M4DECODER_MPEG4_DecoderConfigInfo* pDci,
                                             M4DECODER_VideoSize* pVideoSize)
{
    M4VS_Bitstream_ctxt parsingCtxt;
    M4OSA_UInt32 code, j;
    M4OSA_MemAddr8 start;
    M4OSA_UInt8 i;
    M4OSA_UInt32 time_incr_length;
    M4OSA_UInt8 vol_verid=0, b_hierarchy_type;

    /* Parsing variables */
    M4OSA_UInt8 video_object_layer_shape = 0;
    M4OSA_UInt8 sprite_enable = 0;
    M4OSA_UInt8 reduced_resolution_vop_enable = 0;
    M4OSA_UInt8 scalability = 0;
    M4OSA_UInt8 enhancement_type = 0;
    M4OSA_UInt8 complexity_estimation_disable = 0;
    M4OSA_UInt8 interlaced = 0;
    M4OSA_UInt8 sprite_warping_points = 0;
    M4OSA_UInt8 sprite_brightness_change = 0;
    M4OSA_UInt8 quant_precision = 0;

    /* Fill the structure with default parameters */
    pVideoSize->m_uiWidth              = 0;
    pVideoSize->m_uiHeight             = 0;

    pDci->uiTimeScale          = 0;
    pDci->uiProfile            = 0;
    pDci->uiUseOfResynchMarker = 0;
    pDci->bDataPartition       = M4OSA_FALSE;
    pDci->bUseOfRVLC           = M4OSA_FALSE;

    /* Reset the bitstream context */
    parsingCtxt.stream_byte = 0;
    parsingCtxt.stream_index = 8;
    parsingCtxt.in = (M4OSA_Int8 *)pVol;

    start = (M4OSA_Int8 *)pVol;

    /* Start parsing */
    while (parsingCtxt.in - start < aVolSize)
    {
        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 8);
        if (code == 0)
        {
            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 8);
            if (code == 0)
            {
                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 8);
                if (code == 1)
                {
                    /* start code found */
                    code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 8);

                    /* ----- 0x20..0x2F : video_object_layer_start_code ----- */

                    if ((code > 0x1F) && (code < 0x30))
                    {
                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                 1);/* random accessible vol */
                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                 8);/* video object type indication */
                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                 1);/* is object layer identifier */
                        if (code == 1)
                        {
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     4); /* video object layer verid */
                            vol_verid = (M4OSA_UInt8)code;
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     3); /* video object layer priority */
                        }
                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                 4);/* aspect ratio */
                        if (code == 15)
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     16); /* par_width and par_height (8+8) */
                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                 1);/* vol control parameters */
                        if (code == 1)
                        {
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     3);/* chroma format + low delay (3+1) */
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     1);/* vbv parameters */
                            if (code == 1)
                            {
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         32);/* first and latter half bitrate + 2 marker bits
                                            (15 + 1 + 15 + 1) */
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         31);/* first and latter half vbv buffer size + first
                                          half vbv occupancy + marker bits (15+1+3+11+1)*/
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         16);/* first half vbv occupancy + marker bits (15+1)*/
                            }
                        }
                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                 2); /* video object layer shape */
                        /* Need to save it for vop parsing */
                        video_object_layer_shape = (M4OSA_UInt8)code;

                        if (code != 0) return 0; /* only rectangular case supported */

                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                 1); /* Marker bit */
                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                 16); /* VOP time increment resolution */
                        pDci->uiTimeScale = code;

                        /* Computes time increment length */
                        j    = code - 1;
                        for (i = 0; (i < 32) && (j != 0); j >>=1)
                        {
                            i++;
                        }
                        time_incr_length = (i == 0) ? 1 : i;

                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                 1);/* Marker bit */
                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                 1);/* Fixed VOP rate */
                        if (code == 1)
                        {
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     time_incr_length);/* Fixed VOP time increment */
                        }

                        if(video_object_layer_shape != 1) /* 1 = Binary */
                        {
                            if(video_object_layer_shape == 0) /* 0 = rectangular */
                            {
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         1);/* Marker bit */
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         13);/* Width */
                                pVideoSize->m_uiWidth = code;
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         1);/* Marker bit */
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         13);/* Height */
                                pVideoSize->m_uiHeight = code;
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         1);/* Marker bit */
                            }
                        }

                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                 1);/* interlaced */
                        interlaced = (M4OSA_UInt8)code;
                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                 1);/* OBMC disable */

                        if(vol_verid == 1)
                        {
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     1);/* sprite enable */
                            sprite_enable = (M4OSA_UInt8)code;
                        }
                        else
                        {
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     2);/* sprite enable */
                            sprite_enable = (M4OSA_UInt8)code;
                        }
                        if ((sprite_enable == 1) || (sprite_enable == 2))
                        /* Sprite static = 1 and Sprite GMC = 2 */
                        {
                            if (sprite_enable != 2)
                            {

                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         13);/* sprite width */
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         1);/* Marker bit */
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         13);/* sprite height */
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         1);/* Marker bit */
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         13);/* sprite l coordinate */
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         1);/* Marker bit */
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         13);/* sprite top coordinate */
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         1);/* Marker bit */
                            }

                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     6);/* sprite warping points */
                            sprite_warping_points = (M4OSA_UInt8)code;
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     2);/* sprite warping accuracy */
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     1);/* sprite brightness change */
                            sprite_brightness_change = (M4OSA_UInt8)code;
                            if (sprite_enable != 2)
                            {
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                             1);/* low latency sprite enable */
                            }
                        }
                        if ((vol_verid != 1) && (video_object_layer_shape != 0))
                        {
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         1);/* sadct disable */
                        }

                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 1); /* not 8 bits */
                        if (code)
                        {
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     4);/* quant precision */
                            quant_precision = (M4OSA_UInt8)code;
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         4);/* bits per pixel */
                        }

                        /* greyscale not supported */
                        if(video_object_layer_shape == 3)
                        {
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     3); /* nogray quant update + composition method +
                                            linear composition */
                        }

                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     1);/* quant type */
                        if (code)
                        {
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         1);/* load intra quant mat */
                            if (code)
                            {
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 8);/* */
                                 i    = 1;
                                while (i < 64)
                                {
                                    code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 8);
                                    if (code == 0)
                                        break;
                                    i++;
                                }
                            }

                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                         1);/* load non intra quant mat */
                            if (code)
                            {
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 8);/* */
                                 i    = 1;
                                while (i < 64)
                                {
                                    code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 8);
                                    if (code == 0)
                                        break;
                                    i++;
                                }
                            }
                        }

                        if (vol_verid != 1)
                        {
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     1);/* quarter sample */
                        }

                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     1);/* complexity estimation disable */
                        complexity_estimation_disable = (M4OSA_UInt8)code;
                        if (!code)
                        {
                            //return M4ERR_NOT_IMPLEMENTED;
                        }

                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     1);/* resync marker disable */
                        pDci->uiUseOfResynchMarker = (code) ? 0 : 1;

                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt,
                                     1);/* data partitionned */
                        pDci->bDataPartition = (code) ? M4OSA_TRUE : M4OSA_FALSE;
                        if (code)
                        {
                            /* reversible VLC */
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 1);
                            pDci->bUseOfRVLC = (code) ? M4OSA_TRUE : M4OSA_FALSE;
                        }

                        if (vol_verid != 1)
                        {
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 1);/* newpred */
                            if (code)
                            {
                                //return M4ERR_PARAMETER;
                            }
                            /* reduced resolution vop enable */
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 1);
                            reduced_resolution_vop_enable = (M4OSA_UInt8)code;
                        }

                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 1);/* scalability */
                        scalability = (M4OSA_UInt8)code;
                        if (code)
                        {
                            /* hierarchy type */
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 1);
                            b_hierarchy_type = (M4OSA_UInt8)code;
                            /* ref layer id */
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 4);
                            /* ref sampling direct */
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 1);
                            /* hor sampling factor N */
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 5);
                            /* hor sampling factor M */
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 5);
                            /* vert sampling factor N */
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 5);
                            /* vert sampling factor M */
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 5);
                            /* enhancement type */
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 1);
                            enhancement_type = (M4OSA_UInt8)code;
                            if ((!b_hierarchy_type) && (video_object_layer_shape == 1))
                            {
                                /* use ref shape */
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 1);
                                /* use ref texture */
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 1);
                                /* shape hor sampling factor N */
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 5);
                                /* shape hor sampling factor M */
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 5);
                                /* shape vert sampling factor N */
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 5);
                                /* shape vert sampling factor M */
                                code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 5);
                            }
                        }
                        break;
                    }

                    /* ----- 0xB0 : visual_object_sequence_start_code ----- */

                    else if(code == 0xB0)
                    {
                        /* profile_and_level_indication */
                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 8);
                        pDci->uiProfile = (M4OSA_UInt8)code;
                    }

                    /* ----- 0xB5 : visual_object_start_code ----- */

                    else if(code == 0xB5)
                    {
                        /* is object layer identifier */
                        code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 1);
                        if (code == 1)
                        {
                             /* visual object verid */
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 4);
                            vol_verid = (M4OSA_UInt8)code;
                             /* visual object layer priority */
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 3);
                        }
                        else
                        {
                             /* Realign on byte */
                            code = M4VD_EXTERNAL_GetBitsFromMemory(&parsingCtxt, 7);
                            vol_verid = 1;
                        }
                    }

                    /* ----- end ----- */
                }
                else
                {
                    if ((code >> 2) == 0x20)
                    {
                        /* H263 ...-> wrong*/
                        break;
                    }
                }
            }
        }
    }

    return M4NO_ERROR;
}

M4OSA_ERR getAVCProfileAndLevel(M4OSA_UInt8* pDSI, M4OSA_Int32 DSISize,
                      M4OSA_Int32 *pProfile, M4OSA_Int32 *pLevel) {

    M4OSA_UInt16 index = 28; /* the 29th byte is SPS start */
    M4OSA_Bool constraintSet3;

    if ((pProfile == M4OSA_NULL) || (pLevel == M4OSA_NULL)) {
        return M4ERR_PARAMETER;
    }

    if ((DSISize <= index) || (pDSI == M4OSA_NULL)) {
        ALOGE("getAVCProfileAndLevel: DSI is invalid");
        *pProfile = M4VIDEOEDITING_VIDEO_UNKNOWN_PROFILE;
        *pLevel = M4VIDEOEDITING_VIDEO_UNKNOWN_LEVEL;
        return M4ERR_PARAMETER;
    }

    constraintSet3 = (pDSI[index+2] & 0x10);
    ALOGV("getAVCProfileAndLevel profile_byte %d, level_byte: %d constrain3flag",
          pDSI[index+1], pDSI[index+3], constraintSet3);

    switch (pDSI[index+1]) {
        case 66:
            *pProfile = OMX_VIDEO_AVCProfileBaseline;
            break;
        case 77:
            *pProfile = OMX_VIDEO_AVCProfileMain;
            break;
        case 88:
            *pProfile = OMX_VIDEO_AVCProfileExtended;
            break;
        case 100:
            *pProfile = OMX_VIDEO_AVCProfileHigh;
            break;
        case 110:
            *pProfile = OMX_VIDEO_AVCProfileHigh10;
            break;
        case 122:
            *pProfile = OMX_VIDEO_AVCProfileHigh422;
            break;
        case 244:
            *pProfile = OMX_VIDEO_AVCProfileHigh444;
            break;
        default:
            *pProfile = M4VIDEOEDITING_VIDEO_UNKNOWN_PROFILE;
    }

    switch (pDSI[index+3]) {
        case 10:
            *pLevel = OMX_VIDEO_AVCLevel1;
            break;
        case 11:
            if (constraintSet3)
                *pLevel = OMX_VIDEO_AVCLevel1b;
            else
                *pLevel = OMX_VIDEO_AVCLevel11;
            break;
        case 12:
            *pLevel = OMX_VIDEO_AVCLevel12;
            break;
        case 13:
            *pLevel = OMX_VIDEO_AVCLevel13;
            break;
        case 20:
            *pLevel = OMX_VIDEO_AVCLevel2;
            break;
        case 21:
            *pLevel = OMX_VIDEO_AVCLevel21;
            break;
        case 22:
            *pLevel = OMX_VIDEO_AVCLevel22;
            break;
        case 30:
            *pLevel = OMX_VIDEO_AVCLevel3;
            break;
        case 31:
            *pLevel = OMX_VIDEO_AVCLevel31;
            break;
        case 32:
            *pLevel = OMX_VIDEO_AVCLevel32;
            break;
        case 40:
            *pLevel = OMX_VIDEO_AVCLevel4;
            break;
        case 41:
            *pLevel = OMX_VIDEO_AVCLevel41;
            break;
        case 42:
            *pLevel = OMX_VIDEO_AVCLevel42;
            break;
        case 50:
            *pLevel = OMX_VIDEO_AVCLevel5;
            break;
        case 51:
            *pLevel = OMX_VIDEO_AVCLevel51;
            break;
        default:
            *pLevel = M4VIDEOEDITING_VIDEO_UNKNOWN_LEVEL;
    }
    ALOGV("getAVCProfileAndLevel profile %ld level %ld", *pProfile, *pLevel);
    return M4NO_ERROR;
}

M4OSA_ERR getH263ProfileAndLevel(M4OSA_UInt8* pDSI, M4OSA_Int32 DSISize,
                      M4OSA_Int32 *pProfile, M4OSA_Int32 *pLevel) {

    M4OSA_UInt16 index = 7; /* the 5th and 6th bytes contain the level and profile */

    if ((pProfile == M4OSA_NULL) || (pLevel == M4OSA_NULL)) {
        ALOGE("getH263ProfileAndLevel invalid pointer for pProfile");
        return M4ERR_PARAMETER;
    }

    if ((DSISize < index) || (pDSI == M4OSA_NULL)) {
        ALOGE("getH263ProfileAndLevel: DSI is invalid");
        *pProfile = M4VIDEOEDITING_VIDEO_UNKNOWN_PROFILE;
        *pLevel = M4VIDEOEDITING_VIDEO_UNKNOWN_LEVEL;
        return M4ERR_PARAMETER;
    }
    ALOGV("getH263ProfileAndLevel profile_byte %d, level_byte",
          pDSI[6], pDSI[5]);
    /* get the H263 level */
    switch (pDSI[5]) {
        case 10:
            *pLevel = OMX_VIDEO_H263Level10;
            break;
        case 20:
            *pLevel = OMX_VIDEO_H263Level20;
            break;
        case 30:
            *pLevel = OMX_VIDEO_H263Level30;
            break;
        case 40:
            *pLevel = OMX_VIDEO_H263Level40;
            break;
        case 45:
            *pLevel = OMX_VIDEO_H263Level45;
            break;
        case 50:
            *pLevel = OMX_VIDEO_H263Level50;
            break;
        case 60:
            *pLevel = OMX_VIDEO_H263Level60;
            break;
        case 70:
            *pLevel = OMX_VIDEO_H263Level70;
            break;
        default:
           *pLevel = M4VIDEOEDITING_VIDEO_UNKNOWN_LEVEL;
    }

    /* get H263 profile */
    switch (pDSI[6]) {
        case 0:
            *pProfile = OMX_VIDEO_H263ProfileBaseline;
            break;
        case 1:
            *pProfile = OMX_VIDEO_H263ProfileH320Coding;
            break;
        case 2:
            *pProfile = OMX_VIDEO_H263ProfileBackwardCompatible;
            break;
        case 3:
            *pProfile = OMX_VIDEO_H263ProfileISWV2;
            break;
        case 4:
            *pProfile = OMX_VIDEO_H263ProfileISWV3;
            break;
        case 5:
            *pProfile = OMX_VIDEO_H263ProfileHighCompression;
            break;
        case 6:
            *pProfile = OMX_VIDEO_H263ProfileInternet;
            break;
        case 7:
            *pProfile = OMX_VIDEO_H263ProfileInterlace;
            break;
        case 8:
            *pProfile = OMX_VIDEO_H263ProfileHighLatency;
            break;
        default:
           *pProfile = M4VIDEOEDITING_VIDEO_UNKNOWN_PROFILE;
    }
    ALOGV("getH263ProfileAndLevel profile %ld level %ld", *pProfile, *pLevel);
    return M4NO_ERROR;
}

M4OSA_ERR getMPEG4ProfileAndLevel(M4OSA_UInt8 profileAndLevel,
                      M4OSA_Int32 *pProfile, M4OSA_Int32 *pLevel) {

    M4OSA_UInt32 i = 0;
    M4OSA_UInt32 length = 0;
    if ((pProfile == M4OSA_NULL) || (pLevel == M4OSA_NULL)) {
        return M4ERR_PARAMETER;
    }
    ALOGV("getMPEG4ProfileAndLevel profileAndLevel %d", profileAndLevel);
    length = sizeof(mpeg4ProfileLevelTable) /sizeof(mpeg4ProfileLevelTable[0]);
    *pProfile = M4VIDEOEDITING_VIDEO_UNKNOWN_PROFILE;
    *pLevel = M4VIDEOEDITING_VIDEO_UNKNOWN_LEVEL;
    for (i = 0; i < length; i++) {
        if (mpeg4ProfileLevelTable[i].code == profileAndLevel) {
            *pProfile = mpeg4ProfileLevelTable[i].profile;
            *pLevel = mpeg4ProfileLevelTable[i].level;
            break;
        }
    }
    ALOGV("getMPEG4ProfileAndLevel profile %ld level %ld", *pProfile, *pLevel);
    return M4NO_ERROR;
}
