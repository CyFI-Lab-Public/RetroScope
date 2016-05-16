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
 * @file     M4VIFI_RGB565toYUV420.c
 * @brief    Contain video library function
 * @note     Color Conversion Filter
 *           -# Contains the format conversion filters from RGB565 to YUV420
 ******************************************************************************
*/

/* Prototypes of functions, and type definitions */
#include    "M4VIFI_FiltersAPI.h"
/* Macro definitions */
#include    "M4VIFI_Defines.h"
/* Clip table declaration */
#include    "M4VIFI_Clip.h"


/**
 ******************************************************************************
 * M4VIFI_UInt8 M4VIFI_RGB565toYUV420 (void *pUserData,
 *                                     M4VIFI_ImagePlane *pPlaneIn,
 *                                   M4VIFI_ImagePlane *pPlaneOut)
 * @brief   transform RGB565 image to a YUV420 image.
 * @note    Convert RGB565 to YUV420,
 *          Loop on each row ( 2 rows by 2 rows )
 *              Loop on each column ( 2 col by 2 col )
 *                  Get 4 RGB samples from input data and build 4 output Y samples
 *                  and each single U & V data
 *              end loop on col
 *          end loop on row
 * @param   pUserData: (IN) User Specific Data
 * @param   pPlaneIn: (IN) Pointer to RGB565 Plane
 * @param   pPlaneOut: (OUT) Pointer to  YUV420 buffer Plane
 * @return  M4VIFI_OK: there is no error
 * @return  M4VIFI_ILLEGAL_FRAME_HEIGHT: YUV Plane height is ODD
 * @return  M4VIFI_ILLEGAL_FRAME_WIDTH:  YUV Plane width is ODD
 ******************************************************************************
*/
M4VIFI_UInt8    M4VIFI_xVSS_RGB565toYUV420(void *pUserData, M4VIFI_ImagePlane *pPlaneIn,
                                                      M4VIFI_ImagePlane *pPlaneOut)
{
    M4VIFI_UInt32   u32_width, u32_height;
    M4VIFI_UInt32   u32_stride_Y, u32_stride2_Y, u32_stride_U, u32_stride_V;
    M4VIFI_UInt32   u32_stride_rgb, u32_stride_2rgb;
    M4VIFI_UInt32   u32_col, u32_row;

    M4VIFI_Int32    i32_r00, i32_r01, i32_r10, i32_r11;
    M4VIFI_Int32    i32_g00, i32_g01, i32_g10, i32_g11;
    M4VIFI_Int32    i32_b00, i32_b01, i32_b10, i32_b11;
    M4VIFI_Int32    i32_y00, i32_y01, i32_y10, i32_y11;
    M4VIFI_Int32    i32_u00, i32_u01, i32_u10, i32_u11;
    M4VIFI_Int32    i32_v00, i32_v01, i32_v10, i32_v11;
    M4VIFI_UInt8    *pu8_yn, *pu8_ys, *pu8_u, *pu8_v;
    M4VIFI_UInt8    *pu8_y_data, *pu8_u_data, *pu8_v_data;
    M4VIFI_UInt8    *pu8_rgbn_data, *pu8_rgbn;
    M4VIFI_UInt16   u16_pix1, u16_pix2, u16_pix3, u16_pix4;
    M4VIFI_UInt8 count_null=0;

    /* Check planes height are appropriate */
    if( (pPlaneIn->u_height != pPlaneOut[0].u_height)           ||
        (pPlaneOut[0].u_height != (pPlaneOut[1].u_height<<1))   ||
        (pPlaneOut[0].u_height != (pPlaneOut[2].u_height<<1)))
    {
        return M4VIFI_ILLEGAL_FRAME_HEIGHT;
    }

    /* Check planes width are appropriate */
    if( (pPlaneIn->u_width != pPlaneOut[0].u_width)         ||
        (pPlaneOut[0].u_width != (pPlaneOut[1].u_width<<1)) ||
        (pPlaneOut[0].u_width != (pPlaneOut[2].u_width<<1)))
    {
        return M4VIFI_ILLEGAL_FRAME_WIDTH;
    }

    /* Set the pointer to the beginning of the output data buffers */
    pu8_y_data = pPlaneOut[0].pac_data + pPlaneOut[0].u_topleft;
    pu8_u_data = pPlaneOut[1].pac_data + pPlaneOut[1].u_topleft;
    pu8_v_data = pPlaneOut[2].pac_data + pPlaneOut[2].u_topleft;

    /* Set the pointer to the beginning of the input data buffers */
    pu8_rgbn_data   = pPlaneIn->pac_data + pPlaneIn->u_topleft;

    /* Get the size of the output image */
    u32_width = pPlaneOut[0].u_width;
    u32_height = pPlaneOut[0].u_height;

    /* Set the size of the memory jumps corresponding to row jump in each output plane */
    u32_stride_Y = pPlaneOut[0].u_stride;
    u32_stride2_Y = u32_stride_Y << 1;
    u32_stride_U = pPlaneOut[1].u_stride;
    u32_stride_V = pPlaneOut[2].u_stride;

    /* Set the size of the memory jumps corresponding to row jump in input plane */
    u32_stride_rgb = pPlaneIn->u_stride;
    u32_stride_2rgb = u32_stride_rgb << 1;


    /* Loop on each row of the output image, input coordinates are estimated from output ones */
    /* Two YUV rows are computed at each pass */
    for (u32_row = u32_height ;u32_row != 0; u32_row -=2)
    {
        /* Current Y plane row pointers */
        pu8_yn = pu8_y_data;
        /* Next Y plane row pointers */
        pu8_ys = pu8_yn + u32_stride_Y;
        /* Current U plane row pointer */
        pu8_u = pu8_u_data;
        /* Current V plane row pointer */
        pu8_v = pu8_v_data;

        pu8_rgbn = pu8_rgbn_data;

        /* Loop on each column of the output image */
        for (u32_col = u32_width; u32_col != 0 ; u32_col -=2)
        {
            /* Get four RGB 565 samples from input data */
            u16_pix1 = *( (M4VIFI_UInt16 *) pu8_rgbn);
            u16_pix2 = *( (M4VIFI_UInt16 *) (pu8_rgbn + CST_RGB_16_SIZE));
            u16_pix3 = *( (M4VIFI_UInt16 *) (pu8_rgbn + u32_stride_rgb));
            u16_pix4 = *( (M4VIFI_UInt16 *) (pu8_rgbn + u32_stride_rgb + CST_RGB_16_SIZE));

            /* Unpack RGB565 to 8bit R, G, B */
            /* (x,y) */
            GET_RGB565(i32_b00,i32_g00,i32_r00,u16_pix1);
            /* (x+1,y) */
            GET_RGB565(i32_b10,i32_g10,i32_r10,u16_pix2);
            /* (x,y+1) */
            GET_RGB565(i32_b01,i32_g01,i32_r01,u16_pix3);
            /* (x+1,y+1) */
            GET_RGB565(i32_b11,i32_g11,i32_r11,u16_pix4);
            /* If RGB is transparent color (0, 63, 0), we transform it to white (31,63,31) */
            if(i32_b00 == 0 && i32_g00 == 63 && i32_r00 == 0)
            {
                i32_b00 = 31;
                i32_r00 = 31;
            }
            if(i32_b10 == 0 && i32_g10 == 63 && i32_r10 == 0)
            {
                i32_b10 = 31;
                i32_r10 = 31;
            }
            if(i32_b01 == 0 && i32_g01 == 63 && i32_r01 == 0)
            {
                i32_b01 = 31;
                i32_r01 = 31;
            }
            if(i32_b11 == 0 && i32_g11 == 63 && i32_r11 == 0)
            {
                i32_b11 = 31;
                i32_r11 = 31;
            }
            /* Convert RGB value to YUV */
            i32_u00 = U16(i32_r00, i32_g00, i32_b00);
            i32_v00 = V16(i32_r00, i32_g00, i32_b00);
            /* luminance value */
            i32_y00 = Y16(i32_r00, i32_g00, i32_b00);

            i32_u10 = U16(i32_r10, i32_g10, i32_b10);
            i32_v10 = V16(i32_r10, i32_g10, i32_b10);
            /* luminance value */
            i32_y10 = Y16(i32_r10, i32_g10, i32_b10);

            i32_u01 = U16(i32_r01, i32_g01, i32_b01);
            i32_v01 = V16(i32_r01, i32_g01, i32_b01);
            /* luminance value */
            i32_y01 = Y16(i32_r01, i32_g01, i32_b01);

            i32_u11 = U16(i32_r11, i32_g11, i32_b11);
            i32_v11 = V16(i32_r11, i32_g11, i32_b11);
            /* luminance value */
            i32_y11 = Y16(i32_r11, i32_g11, i32_b11);

            /* Store luminance data */
            pu8_yn[0] = (M4VIFI_UInt8)i32_y00;
            pu8_yn[1] = (M4VIFI_UInt8)i32_y10;
            pu8_ys[0] = (M4VIFI_UInt8)i32_y01;
            pu8_ys[1] = (M4VIFI_UInt8)i32_y11;
            *pu8_u = (M4VIFI_UInt8)((i32_u00 + i32_u01 + i32_u10 + i32_u11 + 2) >> 2);
            *pu8_v = (M4VIFI_UInt8)((i32_v00 + i32_v01 + i32_v10 + i32_v11 + 2) >> 2);
            /* Prepare for next column */
            pu8_rgbn += (CST_RGB_16_SIZE<<1);
            /* Update current Y plane line pointer*/
            pu8_yn += 2;
            /* Update next Y plane line pointer*/
            pu8_ys += 2;
            /* Update U plane line pointer*/
            pu8_u ++;
            /* Update V plane line pointer*/
            pu8_v ++;
        } /* End of horizontal scanning */

        /* Prepare pointers for the next row */
        pu8_y_data += u32_stride2_Y;
        pu8_u_data += u32_stride_U;
        pu8_v_data += u32_stride_V;
        pu8_rgbn_data += u32_stride_2rgb;


    } /* End of vertical scanning */

    return M4VIFI_OK;
}
/* End of file M4VIFI_RGB565toYUV420.c */

