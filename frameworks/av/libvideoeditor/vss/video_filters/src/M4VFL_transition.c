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
 * @file        M4TRAN_transition.c
 * @brief
 ******************************************************************************
*/

/**
 * OSAL (memset and memcpy) ***/
#include "M4OSA_Memory.h"

#include "M4VFL_transition.h"

#include <string.h>

#ifdef LITTLE_ENDIAN
#define M4VFL_SWAP_SHORT(a) a = ((a & 0xFF) << 8) | ((a & 0xFF00) >> 8)
#else
#define M4VFL_SWAP_SHORT(a)
#endif

#define LUM_FACTOR_MAX 10


unsigned char M4VFL_modifyLumaByStep(M4ViComImagePlane *plane_in, M4ViComImagePlane *plane_out,
                                     M4VFL_ModifLumParam *lum_param, void *user_data)
{
    unsigned short *p_src, *p_dest, *p_src_line, *p_dest_line;
    unsigned long pix_src;
    unsigned long u_outpx, u_outpx2;
    unsigned long u_width, u_stride, u_stride_out,u_height, pix;
    unsigned long lf1, lf2, lf3;
    long i, j;

    if (lum_param->copy_chroma != 0)
    {
        /* copy chroma plane */

    }

    /* apply luma factor */
    u_width = plane_in[0].u_width;
    u_height = plane_in[0].u_height;
    u_stride = (plane_in[0].u_stride >> 1);
    u_stride_out = (plane_out[0].u_stride >> 1);
    p_dest = (unsigned short *) &plane_out[0].pac_data[plane_out[0].u_topleft];
    p_src = (unsigned short *) &plane_in[0].pac_data[plane_in[0].u_topleft];
    p_dest_line = p_dest;
    p_src_line = p_src;

    switch(lum_param->lum_factor)
    {
    case 0:
        /* very specific case : set luma plane to 16 */
        for (j = u_height; j != 0; j--)
        {
            memset((void *)p_dest,16, u_width);
            p_dest += u_stride_out;
        }
        return 0;

    case 1:
        /* 0.25 */
        lf1 = 6; lf2 = 6; lf3 = 7;
        break;
    case 2:
        /* 0.375 */
        lf1 = 7; lf2 = 7; lf3 = 7;
        break;
    case 3:
        /* 0.5 */
        lf1 = 7; lf2 = 7; lf3 = 8;
        break;
    case 4:
        /* 0.625 */
        lf1 = 7; lf2 = 8; lf3 = 8;
        break;
    case 5:
        /* 0.75 */
        lf1 = 8; lf2 = 8; lf3 = 8;
        break;
    case 6:
        /* 0.875 */
        lf1 = 9; lf2 = 8; lf3 = 7;
        break;
    default:
        lf1 = 8; lf2 = 8; lf3 = 9;
        break;
    }

    for (j = u_height; j != 0; j--)
    {
        p_dest = p_dest_line;
        p_src = p_src_line;
        for (i = (u_width >> 1); i != 0; i--)
        {
            pix_src = (unsigned long) *p_src++;
            pix = pix_src & 0xFF;
            u_outpx = (((pix << lf1) + (pix << lf2) + (pix << lf3) ) >> LUM_FACTOR_MAX);
            pix = ((pix_src & 0xFF00) >> 8);
            u_outpx2 = ((((pix << lf1) + (pix << lf2) + (pix << lf3) ) >> LUM_FACTOR_MAX)<< 8) ;
            *p_dest++ = (unsigned short) (u_outpx2 | u_outpx);
        }
        p_dest_line += u_stride_out;
        p_src_line += u_stride;
    }
    return 0;
}


unsigned char M4VFL_modifyLumaWithScale(M4ViComImagePlane *plane_in,
                                         M4ViComImagePlane *plane_out,
                                         unsigned long lum_factor,
                                         void *user_data)
{
    unsigned short *p_src, *p_dest, *p_src_line, *p_dest_line;
    unsigned char *p_csrc, *p_cdest, *p_csrc_line, *p_cdest_line;
    unsigned long pix_src;
    unsigned long u_outpx, u_outpx2;
    unsigned long u_width, u_stride, u_stride_out,u_height, pix;
    long i, j;

    /* copy or filter chroma */
    u_width = plane_in[1].u_width;
    u_height = plane_in[1].u_height;
    u_stride = plane_in[1].u_stride;
    u_stride_out = plane_out[1].u_stride;
    p_cdest_line = (unsigned char *) &plane_out[1].pac_data[plane_out[1].u_topleft];
    p_csrc_line = (unsigned char *) &plane_in[1].pac_data[plane_in[1].u_topleft];

    if (lum_factor > 256)
    {
        p_cdest = (unsigned char *) &plane_out[2].pac_data[plane_out[2].u_topleft];
        p_csrc = (unsigned char *) &plane_in[2].pac_data[plane_in[2].u_topleft];
        /* copy chroma */
        for (j = u_height; j != 0; j--)
        {
            for (i = u_width; i != 0; i--)
            {
                memcpy((void *)p_cdest_line, (void *)p_csrc_line, u_width);
                memcpy((void *)p_cdest,(void *) p_csrc, u_width);
            }
            p_cdest_line += u_stride_out;
            p_cdest += u_stride_out;
            p_csrc_line += u_stride;
            p_csrc += u_stride;
        }
    }
    else
    {
        /* filter chroma */
        pix = (1024 - lum_factor) << 7;
        for (j = u_height; j != 0; j--)
        {
            p_cdest = p_cdest_line;
            p_csrc = p_csrc_line;
            for (i = u_width; i != 0; i--)
            {
                *p_cdest++ = ((pix + (*p_csrc++ & 0xFF) * lum_factor) >> LUM_FACTOR_MAX);
            }
            p_cdest_line += u_stride_out;
            p_csrc_line += u_stride;
        }
        p_cdest_line = (unsigned char *) &plane_out[2].pac_data[plane_out[2].u_topleft];
        p_csrc_line = (unsigned char *) &plane_in[2].pac_data[plane_in[2].u_topleft];
        for (j = u_height; j != 0; j--)
        {
            p_cdest = p_cdest_line;
            p_csrc = p_csrc_line;
            for (i = u_width; i != 0; i--)
            {
                *p_cdest++ = ((pix + (*p_csrc & 0xFF) * lum_factor) >> LUM_FACTOR_MAX);
            }
            p_cdest_line += u_stride_out;
            p_csrc_line += u_stride;
        }
    }
    /* apply luma factor */
    u_width = plane_in[0].u_width;
    u_height = plane_in[0].u_height;
    u_stride = (plane_in[0].u_stride >> 1);
    u_stride_out = (plane_out[0].u_stride >> 1);
    p_dest = (unsigned short *) &plane_out[0].pac_data[plane_out[0].u_topleft];
    p_src = (unsigned short *) &plane_in[0].pac_data[plane_in[0].u_topleft];
    p_dest_line = p_dest;
    p_src_line = p_src;

    for (j = u_height; j != 0; j--)
    {
        p_dest = p_dest_line;
        p_src = p_src_line;
        for (i = (u_width >> 1); i != 0; i--)
        {
            pix_src = (unsigned long) *p_src++;
            pix = pix_src & 0xFF;
            u_outpx = ((pix * lum_factor) >> LUM_FACTOR_MAX);
            pix = ((pix_src & 0xFF00) >> 8);
            u_outpx2 = (((pix * lum_factor) >> LUM_FACTOR_MAX)<< 8) ;
            *p_dest++ = (unsigned short) (u_outpx2 | u_outpx);
        }
        p_dest_line += u_stride_out;
        p_src_line += u_stride;
    }

    return 0;
}

/**
 *************************************************************************************************
 * M4OSA_ERR M4VIFI_ImageBlendingonYUV420 (void *pUserData,
 *                                                  M4VIFI_ImagePlane *pPlaneIn1,
 *                                                  M4VIFI_ImagePlane *pPlaneIn2,
 *                                                  M4VIFI_ImagePlane *pPlaneOut,
 *                                                  UInt32 Progress)
 * @brief   Blends two YUV 4:2:0 Planar images.
 * @note    Blends YUV420 planar images,
 *          Map the value of progress from (0 - 1000) to (0 - 1024)
 *          Set the range of blendingfactor,
 *                  1. from 0 to (Progress << 1)            ;for Progress <= 512
 *                  2. from (( Progress - 512)<< 1) to 1024 ;otherwise
 *          Set the increment of blendingfactor for each element in the image row by the factor,
 *                  =  (Range-1) / (image width-1)  ;for width >= range
 *                  =  (Range) / (image width)      ;otherwise
 *          Loop on each(= i) row of output Y plane (steps of 2)
 *              Loop on each(= j) column of output Y plane (steps of 2)
 *                  Get four Y samples and one U & V sample from two input YUV4:2:0 images and
 *                  Compute four Y sample and one U & V sample for output YUV4:2:0 image
 *                      using the following,
 *                  Out(i,j) = blendingfactor(i,j) * In1(i,j)+ (l - blendingfactor(i,j)) *In2(i,j)
 *              end loop column
 *          end loop row.
 * @param   pUserData: (IN)  User Specific Parameter
 * @param   pPlaneIn1: (IN)  Pointer to an array of image plane structures maintained
 *           for Y, U and V planes.
 * @param   pPlaneIn2: (IN)  Pointer to an array of image plane structures maintained
 *           for Y, U and V planes.
 * @param   pPlaneOut: (OUT) Pointer to an array of image plane structures maintained
 *           for Y, U and V planes.
 * @param   Progress:  (IN)  Progress value (varies between 0 and 1000)
 * @return  M4VIFI_OK: No error
 * @return  M4VIFI_ILLEGAL_FRAME_HEIGHT: Error in height
 * @return  M4VIFI_ILLEGAL_FRAME_WIDTH:  Error in width
 *************************************************************************************************
*/

/** Check for value is EVEN */
#ifndef IS_EVEN
#define IS_EVEN(a)  (!(a & 0x01))
#endif

/** Used for fixed point implementation */
#ifndef MAX_SHORT
#define MAX_SHORT   0x10000
#endif

#ifndef NULL
#define NULL    0
#endif

#ifndef FALSE
#define FALSE   0
#define TRUE    !FALSE
#endif

unsigned char M4VIFI_ImageBlendingonYUV420 (void *pUserData,
                                            M4ViComImagePlane *pPlaneIn1,
                                            M4ViComImagePlane *pPlaneIn2,
                                            M4ViComImagePlane *pPlaneOut,
                                            UInt32 Progress)
{
    UInt8    *pu8_data_Y_start1,*pu8_data_U_start1,*pu8_data_V_start1;
    UInt8    *pu8_data_Y_start2,*pu8_data_U_start2,*pu8_data_V_start2;
    UInt8    *pu8_data_Y_start3,*pu8_data_U_start3,*pu8_data_V_start3;
    UInt8    *pu8_data_Y_current1, *pu8_data_Y_next1, *pu8_data_U1, *pu8_data_V1;
    UInt8    *pu8_data_Y_current2, *pu8_data_Y_next2, *pu8_data_U2, *pu8_data_V2;
    UInt8    *pu8_data_Y_current3,*pu8_data_Y_next3, *pu8_data_U3, *pu8_data_V3;
    UInt32   u32_stride_Y1, u32_stride2_Y1, u32_stride_U1, u32_stride_V1;
    UInt32   u32_stride_Y2, u32_stride2_Y2, u32_stride_U2, u32_stride_V2;
    UInt32   u32_stride_Y3, u32_stride2_Y3, u32_stride_U3, u32_stride_V3;
    UInt32   u32_height,  u32_width;
    UInt32   u32_blendfactor, u32_startA, u32_endA, u32_blend_inc, u32_x_accum;
    UInt32   u32_col, u32_row, u32_rangeA, u32_progress;
    UInt32   u32_U1,u32_V1,u32_U2,u32_V2, u32_Y1, u32_Y2;


    /* Check the Y plane height is EVEN and image plane heights are same */
    if( (IS_EVEN(pPlaneIn1[0].u_height) == FALSE)                ||
        (IS_EVEN(pPlaneIn2[0].u_height) == FALSE)                ||
        (IS_EVEN(pPlaneOut[0].u_height) == FALSE)                ||
        (pPlaneIn1[0].u_height != pPlaneOut[0].u_height)         ||
        (pPlaneIn2[0].u_height != pPlaneOut[0].u_height) )
    {
        return M4VIFI_ILLEGAL_FRAME_HEIGHT;
    }

    /* Check the Y plane width is EVEN and image plane widths are same */
    if( (IS_EVEN(pPlaneIn1[0].u_width) == FALSE)                 ||
        (IS_EVEN(pPlaneIn2[0].u_width) == FALSE)                 ||
        (IS_EVEN(pPlaneOut[0].u_width) == FALSE)                 ||
        (pPlaneIn1[0].u_width  != pPlaneOut[0].u_width)          ||
        (pPlaneIn2[0].u_width  != pPlaneOut[0].u_width)  )
    {
        return M4VIFI_ILLEGAL_FRAME_WIDTH;
    }

    /* Set the pointer to the beginning of the input1 YUV420 image planes */
    pu8_data_Y_start1 = pPlaneIn1[0].pac_data + pPlaneIn1[0].u_topleft;
    pu8_data_U_start1 = pPlaneIn1[1].pac_data + pPlaneIn1[1].u_topleft;
    pu8_data_V_start1 = pPlaneIn1[2].pac_data + pPlaneIn1[2].u_topleft;

    /* Set the pointer to the beginning of the input2 YUV420 image planes */
    pu8_data_Y_start2 = pPlaneIn2[0].pac_data + pPlaneIn2[0].u_topleft;
    pu8_data_U_start2 = pPlaneIn2[1].pac_data + pPlaneIn2[1].u_topleft;
    pu8_data_V_start2 = pPlaneIn2[2].pac_data + pPlaneIn2[2].u_topleft;

    /* Set the pointer to the beginning of the output YUV420 image planes */
    pu8_data_Y_start3 = pPlaneOut[0].pac_data + pPlaneOut[0].u_topleft;
    pu8_data_U_start3 = pPlaneOut[1].pac_data + pPlaneOut[1].u_topleft;
    pu8_data_V_start3 = pPlaneOut[2].pac_data + pPlaneOut[2].u_topleft;

    /* Set the stride for the next row in each input1 YUV420 plane */
    u32_stride_Y1 = pPlaneIn1[0].u_stride;
    u32_stride_U1 = pPlaneIn1[1].u_stride;
    u32_stride_V1 = pPlaneIn1[2].u_stride;

    /* Set the stride for the next row in each input2 YUV420 plane */
    u32_stride_Y2 = pPlaneIn2[0].u_stride;
    u32_stride_U2 = pPlaneIn2[1].u_stride;
    u32_stride_V2 = pPlaneIn2[2].u_stride;

    /* Set the stride for the next row in each output YUV420 plane */
    u32_stride_Y3 = pPlaneOut[0].u_stride;
    u32_stride_U3 = pPlaneOut[1].u_stride;
    u32_stride_V3 = pPlaneOut[2].u_stride;

    u32_stride2_Y1   = u32_stride_Y1 << 1;
    u32_stride2_Y2   = u32_stride_Y2 << 1;
    u32_stride2_Y3   = u32_stride_Y3 << 1;

    /* Get the size of the output image */
    u32_height = pPlaneOut[0].u_height;
    u32_width  = pPlaneOut[0].u_width;

    /* User Specified Progress value */
    u32_progress = Progress;

    /* Map Progress value from (0 - 1000) to (0 - 1024) -> for optimisation */
    if(u32_progress < 1000)
        u32_progress = ((u32_progress << 10) / 1000);
    else
        u32_progress = 1024;

    /* Set the range of blendingfactor */
    if(u32_progress <= 512)
    {
        u32_startA = 0;
        u32_endA   = (u32_progress << 1);
    }
    else /* u32_progress > 512 */
    {
        u32_startA = (u32_progress - 512) << 1;
        u32_endA   =  1024;
    }
    u32_rangeA = u32_endA - u32_startA;

    /* Set the increment of blendingfactor for each element in the image row */
    if ((u32_width >= u32_rangeA) && (u32_rangeA > 0) )
    {
        u32_blend_inc   = ((u32_rangeA-1) * MAX_SHORT) / (u32_width - 1);
    }
    else /* (u32_width < u32_rangeA) || (u32_rangeA < 0) */
    {
        u32_blend_inc   = (u32_rangeA * MAX_SHORT) / (u32_width);
    }

    /* Two YUV420 rows are computed at each pass */
    for (u32_row = u32_height; u32_row != 0; u32_row -=2)
    {
        /* Set pointers to the beginning of the row for each input image1 plane */
        pu8_data_Y_current1 = pu8_data_Y_start1;
        pu8_data_U1 = pu8_data_U_start1;
        pu8_data_V1 = pu8_data_V_start1;

        /* Set pointers to the beginning of the row for each input image2 plane */
        pu8_data_Y_current2 = pu8_data_Y_start2;
        pu8_data_U2 = pu8_data_U_start2;
        pu8_data_V2 = pu8_data_V_start2;

        /* Set pointers to the beginning of the row for each output image plane */
        pu8_data_Y_current3 = pu8_data_Y_start3;
        pu8_data_U3 = pu8_data_U_start3;
        pu8_data_V3 = pu8_data_V_start3;

        /* Set pointers to the beginning of the next row for image luma plane */
        pu8_data_Y_next1 = pu8_data_Y_current1 + u32_stride_Y1;
        pu8_data_Y_next2 = pu8_data_Y_current2 + u32_stride_Y2;
        pu8_data_Y_next3 = pu8_data_Y_current3 + u32_stride_Y3;

        /* Initialise blendfactor */
        u32_blendfactor   = u32_startA;
        /* Blendfactor Increment accumulator */
        u32_x_accum = 0;

        /* Loop on each column of the output image */
        for (u32_col = u32_width; u32_col != 0 ; u32_col -=2)
        {
            /* Update the blending factor */
            u32_blendfactor = u32_startA + (u32_x_accum >> 16);

            /* Get Luma value (x,y) of input Image1 */
            u32_Y1 = *pu8_data_Y_current1++;

            /* Get chrominance2 value */
            u32_U1 = *pu8_data_U1++;
            u32_V1 = *pu8_data_V1++;

            /* Get Luma value (x,y) of input Image2 */
            u32_Y2 = *pu8_data_Y_current2++;

            /* Get chrominance2 value */
            u32_U2 = *pu8_data_U2++;
            u32_V2 = *pu8_data_V2++;

            /* Compute Luma value (x,y) of Output image */
            *pu8_data_Y_current3++  = (UInt8)((u32_blendfactor * u32_Y2 +
                                                     (1024 - u32_blendfactor)*u32_Y1) >> 10);
            /* Compute chroma(U) value of Output image */
            *pu8_data_U3++          = (UInt8)((u32_blendfactor * u32_U2 +
                                                     (1024 - u32_blendfactor)*u32_U1) >> 10);
            /* Compute chroma(V) value of Output image */
            *pu8_data_V3++          = (UInt8)((u32_blendfactor * u32_V2 +
                                                     (1024 - u32_blendfactor)*u32_V1) >> 10);

            /* Get Luma value (x,y+1) of input Image1 */
            u32_Y1 = *pu8_data_Y_next1++;

             /* Get Luma value (x,y+1) of input Image2 */
            u32_Y2 = *pu8_data_Y_next2++;

            /* Compute Luma value (x,y+1) of Output image*/
            *pu8_data_Y_next3++ = (UInt8)((u32_blendfactor * u32_Y2 +
                                                    (1024 - u32_blendfactor)*u32_Y1) >> 10);
            /* Update accumulator */
            u32_x_accum += u32_blend_inc;

            /* Update the blending factor */
            u32_blendfactor = u32_startA + (u32_x_accum >> 16);

            /* Get Luma value (x+1,y) of input Image1 */
            u32_Y1 = *pu8_data_Y_current1++;

            /* Get Luma value (x+1,y) of input Image2 */
            u32_Y2 = *pu8_data_Y_current2++;

            /* Compute Luma value (x+1,y) of Output image*/
            *pu8_data_Y_current3++ = (UInt8)((u32_blendfactor * u32_Y2 +
                                                 (1024 - u32_blendfactor)*u32_Y1) >> 10);

            /* Get Luma value (x+1,y+1) of input Image1 */
            u32_Y1 = *pu8_data_Y_next1++;

            /* Get Luma value (x+1,y+1) of input Image2 */
            u32_Y2 = *pu8_data_Y_next2++;

            /* Compute Luma value (x+1,y+1) of Output image*/
            *pu8_data_Y_next3++ = (UInt8)((u32_blendfactor * u32_Y2 +
                                                 (1024 - u32_blendfactor)*u32_Y1) >> 10);
            /* Update accumulator */
            u32_x_accum += u32_blend_inc;

            /* Working pointers are incremented just after each storage */

        }/* End of row scanning */

        /* Update working pointer of input image1 for next row */
        pu8_data_Y_start1 += u32_stride2_Y1;
        pu8_data_U_start1 += u32_stride_U1;
        pu8_data_V_start1 += u32_stride_V1;

        /* Update working pointer of input image2 for next row */
        pu8_data_Y_start2 += u32_stride2_Y2;
        pu8_data_U_start2 += u32_stride_U2;
        pu8_data_V_start2 += u32_stride_V2;

        /* Update working pointer of output image for next row */
        pu8_data_Y_start3 += u32_stride2_Y3;
        pu8_data_U_start3 += u32_stride_U3;
        pu8_data_V_start3 += u32_stride_V3;

    }/* End of column scanning */

    return M4VIFI_OK;
}
/* End of file M4VIFI_ImageBlendingonYUV420.c */

