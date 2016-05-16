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
 * @file     M4VIFI_ResizeYUV420toYUV420.c
 * @brief    Contain video library function
 * @note     This file has a Resize filter function
 *           -# Generic resizing of YUV420 (Planar) image
 ******************************************************************************
*/

/* Prototypes of functions, and type definitions */
#include    "M4VIFI_FiltersAPI.h"
/* Macro definitions */
#include    "M4VIFI_Defines.h"
/* Clip table declaration */
#include    "M4VIFI_Clip.h"

/**
 ***********************************************************************************************
 * M4VIFI_UInt8 M4VIFI_ResizeBilinearRGB888toRGB888(void *pUserData, M4VIFI_ImagePlane *pPlaneIn,
 *                                                                  M4VIFI_ImagePlane *pPlaneOut)
 * @brief   Resizes YUV420 Planar plane.
 * @note    Basic structure of the function
 *          Loop on each row (step 2)
 *              Loop on each column (step 2)
 *                  Get four Y samples and 1 U & V sample
 *                  Resize the Y with corresponing U and V samples
 *                  Place the YUV in the ouput plane
 *              end loop column
 *          end loop row
 *          For resizing bilinear interpolation linearly interpolates along
 *          each row, and then uses that result in a linear interpolation down each column.
 *          Each estimated pixel in the output image is a weighted
 *          combination of its four neighbours. The ratio of compression
 *          or dilatation is estimated using input and output sizes.
 * @param   pUserData: (IN) User Data
 * @param   pPlaneIn: (IN) Pointer to YUV420 (Planar) plane buffer
 * @param   pPlaneOut: (OUT) Pointer to YUV420 (Planar) plane
 * @return  M4VIFI_OK: there is no error
 * @return  M4VIFI_ILLEGAL_FRAME_HEIGHT: Error in height
 * @return  M4VIFI_ILLEGAL_FRAME_WIDTH:  Error in width
 ***********************************************************************************************
*/
M4VIFI_UInt8    M4VIFI_ResizeBilinearRGB888toRGB888(void *pUserData,
                                                                M4VIFI_ImagePlane *pPlaneIn,
                                                                M4VIFI_ImagePlane *pPlaneOut)
{
    M4VIFI_UInt8    *pu8_data_in;
    M4VIFI_UInt8    *pu8_data_out;
    M4VIFI_UInt32   u32_width_in, u32_width_out, u32_height_in, u32_height_out;
    M4VIFI_UInt32   u32_stride_in, u32_stride_out;
    M4VIFI_UInt32   u32_x_inc, u32_y_inc;
    M4VIFI_UInt32   u32_x_accum, u32_y_accum, u32_x_accum_start;
    M4VIFI_UInt32   u32_width, u32_height;
    M4VIFI_UInt32   u32_y_frac;
    M4VIFI_UInt32   u32_x_frac;
    M4VIFI_UInt32   u32_Rtemp_value,u32_Gtemp_value,u32_Btemp_value;
    M4VIFI_UInt8    *pu8_src_top;
    M4VIFI_UInt8    *pu8_src_bottom;
    M4VIFI_UInt32    i32_b00, i32_g00, i32_r00;
    M4VIFI_UInt32    i32_b01, i32_g01, i32_r01;
    M4VIFI_UInt32    i32_b02, i32_g02, i32_r02;
    M4VIFI_UInt32    i32_b03, i32_g03, i32_r03;

    /* Check for the YUV width and height are even */
    if ((IS_EVEN(pPlaneIn->u_height) == FALSE)    ||
        (IS_EVEN(pPlaneOut->u_height) == FALSE))
    {
        return M4VIFI_ILLEGAL_FRAME_HEIGHT;
    }

    if ((IS_EVEN(pPlaneIn->u_width) == FALSE) ||
        (IS_EVEN(pPlaneOut->u_width) == FALSE))
    {
        return M4VIFI_ILLEGAL_FRAME_WIDTH;
    }


        /* Set the working pointers at the beginning of the input/output data field */
        pu8_data_in     = (M4VIFI_UInt8*)(pPlaneIn->pac_data + pPlaneIn->u_topleft);
        pu8_data_out    = (M4VIFI_UInt8*)(pPlaneOut->pac_data + pPlaneOut->u_topleft);

        /* Get the memory jump corresponding to a row jump */
        u32_stride_in   = pPlaneIn->u_stride;
        u32_stride_out  = pPlaneOut->u_stride;

        /* Set the bounds of the active image */
        u32_width_in    = pPlaneIn->u_width;
        u32_height_in   = pPlaneIn->u_height;

        u32_width_out   = pPlaneOut->u_width;
        u32_height_out  = pPlaneOut->u_height;

        /* Compute horizontal ratio between src and destination width.*/
        if (u32_width_out >= u32_width_in)
        {
            u32_x_inc   = ((u32_width_in-1) * MAX_SHORT) / (u32_width_out-1);
        }
        else
        {
            u32_x_inc   = (u32_width_in * MAX_SHORT) / (u32_width_out);
        }

        /* Compute vertical ratio between src and destination height.*/
        if (u32_height_out >= u32_height_in)
        {
            u32_y_inc   = ((u32_height_in - 1) * MAX_SHORT) / (u32_height_out-1);
        }
        else
        {
            u32_y_inc = (u32_height_in * MAX_SHORT) / (u32_height_out);
        }

        /*
        Calculate initial accumulator value : u32_y_accum_start.
        u32_y_accum_start is coded on 15 bits, and represents a value between 0 and 0.5
        */
        if (u32_y_inc >= MAX_SHORT)
        {
            /*
                Keep the fractionnal part, assimung that integer  part is coded
                on the 16 high bits and the fractionnal on the 15 low bits
            */
            u32_y_accum = u32_y_inc & 0xffff;

            if (!u32_y_accum)
            {
                u32_y_accum = MAX_SHORT;
            }

            u32_y_accum >>= 1;
        }
        else
        {
            u32_y_accum = 0;
        }


        /*
            Calculate initial accumulator value : u32_x_accum_start.
            u32_x_accum_start is coded on 15 bits, and represents a value between 0 and 0.5
        */
        if (u32_x_inc >= MAX_SHORT)
        {
            u32_x_accum_start = u32_x_inc & 0xffff;

            if (!u32_x_accum_start)
            {
                u32_x_accum_start = MAX_SHORT;
            }

            u32_x_accum_start >>= 1;
        }
        else
        {
            u32_x_accum_start = 0;
        }

        u32_height = u32_height_out;

        /*
        Bilinear interpolation linearly interpolates along each row, and then uses that
        result in a linear interpolation donw each column. Each estimated pixel in the
        output image is a weighted combination of its four neighbours according to the formula:
        F(p',q')=f(p,q)R(-a)R(b)+f(p,q-1)R(-a)R(b-1)+f(p+1,q)R(1-a)R(b)+f(p+&,q+1)R(1-a)R(b-1)
        with  R(x) = / x+1  -1 =< x =< 0 \ 1-x  0 =< x =< 1 and a (resp. b)weighting coefficient
        is the distance from the nearest neighbor in the p (resp. q) direction
        */

        do { /* Scan all the row */

            /* Vertical weight factor */
            u32_y_frac = (u32_y_accum>>12)&15;

            /* Reinit accumulator */
            u32_x_accum = u32_x_accum_start;

            u32_width = u32_width_out;

            do { /* Scan along each row */
                pu8_src_top = pu8_data_in + (u32_x_accum >> 16)*3;
                pu8_src_bottom = pu8_src_top + (u32_stride_in);
                u32_x_frac = (u32_x_accum >> 12)&15; /* Horizontal weight factor */

                if ((u32_width == 1) && (u32_width_in == u32_width_out)) {
                    /*
                       When input height is equal to output height and input width
                       equal to output width, replicate the corner pixels for
                       interpolation
                    */
                    if ((u32_height == 1) && (u32_height_in == u32_height_out)) {
                        GET_RGB24(i32_b00,i32_g00,i32_r00,pu8_src_top,0);
                        GET_RGB24(i32_b01,i32_g01,i32_r01,pu8_src_top,0);
                        GET_RGB24(i32_b02,i32_g02,i32_r02,pu8_src_top,0);
                        GET_RGB24(i32_b03,i32_g03,i32_r03,pu8_src_top,0);
                    }
                    /*
                       When input height is not equal to output height and
                       input width equal to output width, replicate the
                       column for interpolation
                    */
                    else {
                        GET_RGB24(i32_b00,i32_g00,i32_r00,pu8_src_top,0);
                        GET_RGB24(i32_b01,i32_g01,i32_r01,pu8_src_top,0);
                        GET_RGB24(i32_b02,i32_g02,i32_r02,pu8_src_bottom,0);
                        GET_RGB24(i32_b03,i32_g03,i32_r03,pu8_src_bottom,0);
                    }
                } else {
                    /*
                       When input height is equal to output height and
                       input width not equal to output width, replicate the
                       row for interpolation
                    */
                    if ((u32_height == 1) && (u32_height_in == u32_height_out)) {
                        GET_RGB24(i32_b00,i32_g00,i32_r00,pu8_src_top,0);
                        GET_RGB24(i32_b01,i32_g01,i32_r01,pu8_src_top,3);
                        GET_RGB24(i32_b02,i32_g02,i32_r02,pu8_src_top,0);
                        GET_RGB24(i32_b03,i32_g03,i32_r03,pu8_src_top,3);
                    } else {
                        GET_RGB24(i32_b00,i32_g00,i32_r00,pu8_src_top,0);
                        GET_RGB24(i32_b01,i32_g01,i32_r01,pu8_src_top,3);
                        GET_RGB24(i32_b02,i32_g02,i32_r02,pu8_src_bottom,0);
                        GET_RGB24(i32_b03,i32_g03,i32_r03,pu8_src_bottom,3);
                    }
                }
                u32_Rtemp_value = (M4VIFI_UInt8)(((i32_r00*(16-u32_x_frac) +
                                 i32_r01*u32_x_frac)*(16-u32_y_frac) +
                                (i32_r02*(16-u32_x_frac) +
                                 i32_r03*u32_x_frac)*u32_y_frac )>>8);

                u32_Gtemp_value = (M4VIFI_UInt8)(((i32_g00*(16-u32_x_frac) +
                                 i32_g01*u32_x_frac)*(16-u32_y_frac) +
                                (i32_g02*(16-u32_x_frac) +
                                 i32_g03*u32_x_frac)*u32_y_frac )>>8);

                u32_Btemp_value =  (M4VIFI_UInt8)(((i32_b00*(16-u32_x_frac) +
                                 i32_b01*u32_x_frac)*(16-u32_y_frac) +
                                (i32_b02*(16-u32_x_frac) +
                                 i32_b03*u32_x_frac)*u32_y_frac )>>8);

                *pu8_data_out++ = u32_Btemp_value ;
                *pu8_data_out++ = u32_Gtemp_value ;
                *pu8_data_out++ = u32_Rtemp_value ;

                /* Update horizontal accumulator */
                u32_x_accum += u32_x_inc;

            } while(--u32_width);

            //pu16_data_out = pu16_data_out + (u32_stride_out>>1) - (u32_width_out);

            /* Update vertical accumulator */
            u32_y_accum += u32_y_inc;
            if (u32_y_accum>>16)
            {
                pu8_data_in = pu8_data_in + (u32_y_accum >> 16) * (u32_stride_in) ;
                u32_y_accum &= 0xffff;
            }
        } while(--u32_height);

    return M4VIFI_OK;
}
/* End of file M4VIFI_ResizeRGB565toRGB565.c */

