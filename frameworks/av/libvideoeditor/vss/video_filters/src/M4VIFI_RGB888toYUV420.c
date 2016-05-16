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
#include    "M4VIFI_FiltersAPI.h"

#include    "M4VIFI_Defines.h"

#include    "M4VIFI_Clip.h"

/***************************************************************************
Proto:
M4VIFI_UInt8    M4VIFI_RGB888toYUV420(void *pUserData, M4VIFI_ImagePlane *PlaneIn,
                                     M4VIFI_ImagePlane PlaneOut[3]);
Purpose:    filling of the YUV420 plane from a BGR24 plane
Abstract:    Loop on each row ( 2 rows by 2 rows )
                Loop on each column ( 2 col by 2 col )
                    Get 4 BGR samples from input data and build 4 output Y samples and
                    each single U & V data
                end loop on col
            end loop on row

In:            RGB24 plane
InOut:        none
Out:        array of 3 M4VIFI_ImagePlane structures
Modified:    ML: RGB function modified to BGR.
***************************************************************************/
M4VIFI_UInt8 M4VIFI_RGB888toYUV420(void *pUserData, M4VIFI_ImagePlane *PlaneIn,
                                    M4VIFI_ImagePlane PlaneOut[3])
{
    M4VIFI_UInt32    u32_width, u32_height;
    M4VIFI_UInt32    u32_stride_Y, u32_stride2_Y, u32_stride_U, u32_stride_V, u32_stride_rgb,\
                     u32_stride_2rgb;
    M4VIFI_UInt32    u32_col, u32_row;

    M4VIFI_Int32    i32_r00, i32_r01, i32_r10, i32_r11;
    M4VIFI_Int32    i32_g00, i32_g01, i32_g10, i32_g11;
    M4VIFI_Int32    i32_b00, i32_b01, i32_b10, i32_b11;
    M4VIFI_Int32    i32_y00, i32_y01, i32_y10, i32_y11;
    M4VIFI_Int32    i32_u00, i32_u01, i32_u10, i32_u11;
    M4VIFI_Int32    i32_v00, i32_v01, i32_v10, i32_v11;
    M4VIFI_UInt8    *pu8_yn, *pu8_ys, *pu8_u, *pu8_v;
    M4VIFI_UInt8    *pu8_y_data, *pu8_u_data, *pu8_v_data;
    M4VIFI_UInt8    *pu8_rgbn_data, *pu8_rgbn;

    /* check sizes */
    if( (PlaneIn->u_height != PlaneOut[0].u_height)            ||
        (PlaneOut[0].u_height != (PlaneOut[1].u_height<<1))    ||
        (PlaneOut[0].u_height != (PlaneOut[2].u_height<<1)))
        return M4VIFI_ILLEGAL_FRAME_HEIGHT;

    if( (PlaneIn->u_width != PlaneOut[0].u_width)        ||
        (PlaneOut[0].u_width != (PlaneOut[1].u_width<<1))    ||
        (PlaneOut[0].u_width != (PlaneOut[2].u_width<<1)))
        return M4VIFI_ILLEGAL_FRAME_WIDTH;


    /* set the pointer to the beginning of the output data buffers */
    pu8_y_data    = PlaneOut[0].pac_data + PlaneOut[0].u_topleft;
    pu8_u_data    = PlaneOut[1].pac_data + PlaneOut[1].u_topleft;
    pu8_v_data    = PlaneOut[2].pac_data + PlaneOut[2].u_topleft;

    /* idem for input buffer */
    pu8_rgbn_data    = PlaneIn->pac_data + PlaneIn->u_topleft;

    /* get the size of the output image */
    u32_width    = PlaneOut[0].u_width;
    u32_height    = PlaneOut[0].u_height;

    /* set the size of the memory jumps corresponding to row jump in each output plane */
    u32_stride_Y = PlaneOut[0].u_stride;
    u32_stride2_Y= u32_stride_Y << 1;
    u32_stride_U = PlaneOut[1].u_stride;
    u32_stride_V = PlaneOut[2].u_stride;

    /* idem for input plane */
    u32_stride_rgb = PlaneIn->u_stride;
    u32_stride_2rgb = u32_stride_rgb << 1;

    /* loop on each row of the output image, input coordinates are estimated from output ones */
    /* two YUV rows are computed at each pass */
    for    (u32_row = u32_height ;u32_row != 0; u32_row -=2)
    {
        /* update working pointers */
        pu8_yn    = pu8_y_data;
        pu8_ys    = pu8_yn + u32_stride_Y;

        pu8_u    = pu8_u_data;
        pu8_v    = pu8_v_data;

        pu8_rgbn= pu8_rgbn_data;

        /* loop on each column of the output image*/
        for    (u32_col = u32_width; u32_col != 0 ; u32_col -=2)
        {
            /* get RGB samples of 4 pixels */
            GET_RGB24(i32_r00, i32_g00, i32_b00, pu8_rgbn, 0);
            GET_RGB24(i32_r10, i32_g10, i32_b10, pu8_rgbn, CST_RGB_24_SIZE);
            GET_RGB24(i32_r01, i32_g01, i32_b01, pu8_rgbn, u32_stride_rgb);
            GET_RGB24(i32_r11, i32_g11, i32_b11, pu8_rgbn, u32_stride_rgb + CST_RGB_24_SIZE);

            i32_u00    = U24(i32_r00, i32_g00, i32_b00);
            i32_v00    = V24(i32_r00, i32_g00, i32_b00);
            i32_y00    = Y24(i32_r00, i32_g00, i32_b00);        /* matrix luminance */
            pu8_yn[0]= (M4VIFI_UInt8)i32_y00;

            i32_u10    = U24(i32_r10, i32_g10, i32_b10);
            i32_v10    = V24(i32_r10, i32_g10, i32_b10);
            i32_y10    = Y24(i32_r10, i32_g10, i32_b10);
            pu8_yn[1]= (M4VIFI_UInt8)i32_y10;

            i32_u01    = U24(i32_r01, i32_g01, i32_b01);
            i32_v01    = V24(i32_r01, i32_g01, i32_b01);
            i32_y01    = Y24(i32_r01, i32_g01, i32_b01);
            pu8_ys[0]= (M4VIFI_UInt8)i32_y01;

            i32_u11    = U24(i32_r11, i32_g11, i32_b11);
            i32_v11    = V24(i32_r11, i32_g11, i32_b11);
            i32_y11    = Y24(i32_r11, i32_g11, i32_b11);
            pu8_ys[1] = (M4VIFI_UInt8)i32_y11;

            *pu8_u    = (M4VIFI_UInt8)((i32_u00 + i32_u01 + i32_u10 + i32_u11 + 2) >> 2);
            *pu8_v    = (M4VIFI_UInt8)((i32_v00 + i32_v01 + i32_v10 + i32_v11 + 2) >> 2);

            pu8_rgbn    +=  (CST_RGB_24_SIZE<<1);
            pu8_yn        += 2;
            pu8_ys        += 2;

            pu8_u ++;
            pu8_v ++;
        } /* end of horizontal scanning */

        pu8_y_data        += u32_stride2_Y;
        pu8_u_data        += u32_stride_U;
        pu8_v_data        += u32_stride_V;
        pu8_rgbn_data    += u32_stride_2rgb;


    } /* End of vertical scanning */

    return M4VIFI_OK;
}
