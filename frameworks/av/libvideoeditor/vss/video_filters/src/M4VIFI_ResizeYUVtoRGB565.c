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
 * @file     M4VIFI_ResizeYUV420toRGB565RotatedRight.c
 * @brief    Contain video library function
 * @note     This file has a Combo filter function
 *           -# Resizes YUV420 and converts to RGR565 with rotation
 * @date
 *           - 2004/08/11: Creation
 ******************************************************************************
*/

/* Prototypes of functions, and type definitions */
#include    "M4VIFI_FiltersAPI.h"
/* Macro definitions */
#include    "M4VIFI_Defines.h"
/* Clip table declaration */
#include    "M4VIFI_Clip.h"

/**
 ********************************************************************************************
 * M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565RotatedRight(void *pContext,
 *                                                              M4VIFI_ImagePlane *pPlaneIn,
 *                                                              M4VIFI_ImagePlane *pPlaneOut)
 * @brief   Resize YUV420 plane and converts to RGB565 with +90 rotation.
 * @note    Basic sturture of the function
 *          Loop on each row (step 2)
 *              Loop on each column (step 2)
 *                  Get four Y samples and 1 u & V sample
 *                  Resize the Y with corresponing U and V samples
 *                  Compute the four corresponding R G B values
 *                  Place the R G B in the ouput plane in rotated fashion
 *              end loop column
 *          end loop row
 *          For resizing bilinear interpolation linearly interpolates along
 *          each row, and then uses that result in a linear interpolation down each column.
 *          Each estimated pixel in the output image is a weighted
 *          combination of its four neighbours. The ratio of compression
 *          or dilatation is estimated using input and output sizes.
 * @param   pPlaneIn: (IN) Pointer to YUV plane buffer
 * @param   pContext: (IN) Context Pointer
 * @param   pPlaneOut: (OUT) Pointer to BGR565 Plane
 * @return  M4VIFI_OK: there is no error
 * @return  M4VIFI_ILLEGAL_FRAME_HEIGHT: YUV Plane height is ODD
 * @return  M4VIFI_ILLEGAL_FRAME_WIDTH:  YUV Plane width is ODD
 ********************************************************************************************
*/
M4VIFI_UInt8    M4VIFI_ResizeBilinearYUV420toRGB565(void* pContext,
                                                    M4VIFI_ImagePlane *pPlaneIn,
                                                    M4VIFI_ImagePlane *pPlaneOut)
{
    M4VIFI_UInt8    *pu8_data_in[PLANES], *pu8_data_in1[PLANES],*pu8_data_out;
    M4VIFI_UInt32   *pu32_rgb_data_current, *pu32_rgb_data_next, *pu32_rgb_data_start;

    M4VIFI_UInt32   u32_width_in[PLANES], u32_width_out, u32_height_in[PLANES], u32_height_out;
    M4VIFI_UInt32   u32_stride_in[PLANES];
    M4VIFI_UInt32   u32_stride_out, u32_stride2_out, u32_width2_RGB, u32_height2_RGB;
    M4VIFI_UInt32   u32_x_inc[PLANES], u32_y_inc[PLANES];
    M4VIFI_UInt32   u32_x_accum_Y, u32_x_accum_U, u32_x_accum_start;
    M4VIFI_UInt32   u32_y_accum_Y, u32_y_accum_U;
    M4VIFI_UInt32   u32_x_frac_Y, u32_x_frac_U, u32_y_frac_Y,u32_y_frac_U;
    M4VIFI_Int32    U_32, V_32, Y_32, Yval_32;
    M4VIFI_UInt8    u8_Red, u8_Green, u8_Blue;
    M4VIFI_UInt32   u32_row, u32_col;

    M4VIFI_UInt32   u32_plane;
    M4VIFI_UInt32   u32_rgb_temp1, u32_rgb_temp2;
    M4VIFI_UInt32   u32_rgb_temp3,u32_rgb_temp4;
    M4VIFI_UInt32   u32_check_size;

    M4VIFI_UInt8    *pu8_src_top_Y,*pu8_src_top_U,*pu8_src_top_V ;
    M4VIFI_UInt8    *pu8_src_bottom_Y, *pu8_src_bottom_U, *pu8_src_bottom_V;

    /* Check for the  width and height are even */
    u32_check_size = IS_EVEN(pPlaneIn[0].u_height);
    if( u32_check_size == FALSE )
    {
        return M4VIFI_ILLEGAL_FRAME_HEIGHT;
    }
    u32_check_size = IS_EVEN(pPlaneIn[0].u_width);
    if (u32_check_size == FALSE )
    {
        return M4VIFI_ILLEGAL_FRAME_WIDTH;

    }
    /* Make the ouput width and height as even */
    pPlaneOut->u_height = pPlaneOut->u_height & 0xFFFFFFFE;
    pPlaneOut->u_width = pPlaneOut->u_width & 0xFFFFFFFE;
    pPlaneOut->u_stride = pPlaneOut->u_stride & 0xFFFFFFFC;

    /* Assignment of output pointer */
    pu8_data_out    = pPlaneOut->pac_data + pPlaneOut->u_topleft;
    /* Assignment of output width(rotated) */
    u32_width_out   = pPlaneOut->u_width;
    /* Assignment of output height(rotated) */
    u32_height_out  = pPlaneOut->u_height;

    /* Set the bounds of the active image */
    u32_width2_RGB  = pPlaneOut->u_width >> 1;
    u32_height2_RGB = pPlaneOut->u_height >> 1;
    /* Get the memory jump corresponding to a row jump */
    u32_stride_out = pPlaneOut->u_stride >> 1;
    u32_stride2_out = pPlaneOut->u_stride >> 2;

    for(u32_plane = 0; u32_plane < PLANES; u32_plane++)
    {
        /* Set the working pointers at the beginning of the input/output data field */
        pu8_data_in[u32_plane] = pPlaneIn[u32_plane].pac_data + pPlaneIn[u32_plane].u_topleft;

        /* Get the memory jump corresponding to a row jump */
        u32_stride_in[u32_plane] = pPlaneIn[u32_plane].u_stride;

        /* Set the bounds of the active image */
        u32_width_in[u32_plane] = pPlaneIn[u32_plane].u_width;
        u32_height_in[u32_plane] = pPlaneIn[u32_plane].u_height;
    }
    /* Compute horizontal ratio between src and destination width for Y Plane.*/
    if (u32_width_out >= u32_width_in[YPlane])
    {
        u32_x_inc[YPlane]   = ((u32_width_in[YPlane]-1) * MAX_SHORT) / (u32_width_out-1);
    }
    else
    {
        u32_x_inc[YPlane]   = (u32_width_in[YPlane] * MAX_SHORT) / (u32_width_out);
    }

    /* Compute vertical ratio between src and destination height for Y Plane.*/
    if (u32_height_out >= u32_height_in[YPlane])
    {
        u32_y_inc[YPlane]   = ((u32_height_in[YPlane]-1) * MAX_SHORT) / (u32_height_out-1);
    }
    else
    {
        u32_y_inc[YPlane] = (u32_height_in[YPlane] * MAX_SHORT) / (u32_height_out);
    }

    /* Compute horizontal ratio between src and destination width for U and V Planes.*/
    if (u32_width2_RGB >= u32_width_in[UPlane])
    {
        u32_x_inc[UPlane]   = ((u32_width_in[UPlane]-1) * MAX_SHORT) / (u32_width2_RGB-1);
    }
    else
    {
        u32_x_inc[UPlane]   = (u32_width_in[UPlane] * MAX_SHORT) / (u32_width2_RGB);
    }

    /* Compute vertical ratio between src and destination height for U and V Planes.*/

    if (u32_height2_RGB >= u32_height_in[UPlane])
    {
        u32_y_inc[UPlane]   = ((u32_height_in[UPlane]-1) * MAX_SHORT) / (u32_height2_RGB-1);
    }
    else
    {
        u32_y_inc[UPlane]  = (u32_height_in[UPlane] * MAX_SHORT) / (u32_height2_RGB);
    }

    u32_y_inc[VPlane] = u32_y_inc[UPlane];
    u32_x_inc[VPlane] = u32_x_inc[UPlane];

    /*
    Calculate initial accumulator value : u32_y_accum_start.
    u32_y_accum_start is coded on 15 bits, and represents a value between 0 and 0.5
    */
    if (u32_y_inc[YPlane] > MAX_SHORT)
    {
        /*
        Keep the fractionnal part, assimung that integer  part is coded on the 16 high bits,
        and the fractionnal on the 15 low bits
        */
        u32_y_accum_Y = u32_y_inc[YPlane] & 0xffff;
        u32_y_accum_U = u32_y_inc[UPlane] & 0xffff;

        if (!u32_y_accum_Y)
        {
            u32_y_accum_Y = MAX_SHORT;
            u32_y_accum_U = MAX_SHORT;
        }
        u32_y_accum_Y >>= 1;
        u32_y_accum_U >>= 1;
    }
    else
    {
        u32_y_accum_Y = 0;
        u32_y_accum_U = 0;

    }

    /*
    Calculate initial accumulator value : u32_x_accum_start.
    u32_x_accum_start is coded on 15 bits, and represents a value between 0 and 0.5
    */
    if (u32_x_inc[YPlane] > MAX_SHORT)
    {
        u32_x_accum_start = u32_x_inc[YPlane] & 0xffff;

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
    /* Intialise the RGB pointer */
    pu32_rgb_data_start = (M4VIFI_UInt32*)pu8_data_out;

    /*
        Bilinear interpolation linearly interpolates along each row, and then uses that
        result in a linear interpolation donw each column. Each estimated pixel in the
        output image is a weighted combination of its four neighbours according to the formula :
        F(p',q')=f(p,q)R(-a)R(b)+f(p,q-1)R(-a)R(b-1)+f(p+1,q)R(1-a)R(b)+f(p+&,q+1)R(1-a)R(b-1)
        with  R(x) = / x+1  -1 =< x =< 0 \ 1-x  0 =< x =< 1 and a (resp. b) weighting coefficient
        is the distance from the nearest neighbor in the p (resp. q) direction
    */
    for (u32_row = u32_height_out; u32_row != 0; u32_row -= 2)
    {
        u32_x_accum_Y = u32_x_accum_start;
        u32_x_accum_U = u32_x_accum_start;

        /* Vertical weight factor */
        u32_y_frac_Y = (u32_y_accum_Y >> 12) & 15;
        u32_y_frac_U = (u32_y_accum_U >> 12) & 15;

        /* RGB current line Position Pointer */
        pu32_rgb_data_current = pu32_rgb_data_start ;

        /* RGB next line position pointer */
        pu32_rgb_data_next    = pu32_rgb_data_current + (u32_stride2_out);

        /* Y Plane next row pointer */
        pu8_data_in1[YPlane] = pu8_data_in[YPlane];

        u32_rgb_temp3 = u32_y_accum_Y + (u32_y_inc[YPlane]);
        if (u32_rgb_temp3 >> 16)
        {
            pu8_data_in1[YPlane] =  pu8_data_in[YPlane] +
                                                (u32_rgb_temp3 >> 16) * (u32_stride_in[YPlane]);
            u32_rgb_temp3 &= 0xffff;
        }
        u32_rgb_temp4 = (u32_rgb_temp3 >> 12) & 15;

        for (u32_col = u32_width_out; u32_col != 0; u32_col -= 2)
        {

            /* Input Y plane elements */
            pu8_src_top_Y = pu8_data_in[YPlane] + (u32_x_accum_Y >> 16);
            pu8_src_bottom_Y = pu8_src_top_Y + u32_stride_in[YPlane];

            /* Input U Plane elements */
            pu8_src_top_U = pu8_data_in[UPlane] + (u32_x_accum_U >> 16);
            pu8_src_bottom_U = pu8_src_top_U + u32_stride_in[UPlane];

            pu8_src_top_V = pu8_data_in[VPlane] + (u32_x_accum_U >> 16);
            pu8_src_bottom_V = pu8_src_top_V + u32_stride_in[VPlane];

            /* Horizontal weight factor for Y Plane */
            u32_x_frac_Y = (u32_x_accum_Y >> 12)&15;
            /* Horizontal weight factor for U and V Planes */
            u32_x_frac_U = (u32_x_accum_U >> 12)&15;

            /* Weighted combination */
            U_32 = (((pu8_src_top_U[0]*(16-u32_x_frac_U) + pu8_src_top_U[1]*u32_x_frac_U)
                    *(16-u32_y_frac_U) + (pu8_src_bottom_U[0]*(16-u32_x_frac_U)
                    + pu8_src_bottom_U[1]*u32_x_frac_U)*u32_y_frac_U ) >> 8);

            V_32 = (((pu8_src_top_V[0]*(16-u32_x_frac_U) + pu8_src_top_V[1]*u32_x_frac_U)
                    *(16-u32_y_frac_U) + (pu8_src_bottom_V[0]*(16-u32_x_frac_U)
                    + pu8_src_bottom_V[1]*u32_x_frac_U)*u32_y_frac_U ) >> 8);

            Y_32 = (((pu8_src_top_Y[0]*(16-u32_x_frac_Y) + pu8_src_top_Y[1]*u32_x_frac_Y)
                    *(16-u32_y_frac_Y) + (pu8_src_bottom_Y[0]*(16-u32_x_frac_Y)
                    + pu8_src_bottom_Y[1]*u32_x_frac_Y)*u32_y_frac_Y ) >> 8);

            u32_x_accum_U += (u32_x_inc[UPlane]);

            /* YUV to RGB */
            #ifdef __RGB_V1__
                    Yval_32 = Y_32*37;
            #else   /* __RGB_V1__v */
                    Yval_32 = Y_32*0x2568;
            #endif /* __RGB_V1__v */

                    DEMATRIX(u8_Red,u8_Green,u8_Blue,Yval_32,U_32,V_32);

            /* Pack 8 bit R,G,B to RGB565 */
            #ifdef  LITTLE_ENDIAN
                    u32_rgb_temp1 = PACK_RGB565(0,u8_Red,u8_Green,u8_Blue);
            #else   /* LITTLE_ENDIAN */
                    u32_rgb_temp1 = PACK_RGB565(16,u8_Red,u8_Green,u8_Blue);
            #endif  /* LITTLE_ENDIAN */


            pu8_src_top_Y = pu8_data_in1[YPlane]+(u32_x_accum_Y >> 16);
            pu8_src_bottom_Y = pu8_src_top_Y + u32_stride_in[YPlane];

            /* Weighted combination */
            Y_32 = (((pu8_src_top_Y[0]*(16-u32_x_frac_Y) + pu8_src_top_Y[1]*u32_x_frac_Y)
                    *(16-u32_rgb_temp4) + (pu8_src_bottom_Y[0]*(16-u32_x_frac_Y)
                    + pu8_src_bottom_Y[1]*u32_x_frac_Y)*u32_rgb_temp4 ) >> 8);

            u32_x_accum_Y += u32_x_inc[YPlane];
            /* Horizontal weight factor */
            u32_x_frac_Y = (u32_x_accum_Y >> 12)&15;
            /* YUV to RGB */
            #ifdef __RGB_V1__
                    Yval_32 = Y_32*37;
            #else   /* __RGB_V1__v */
                    Yval_32 = Y_32*0x2568;
            #endif  /* __RGB_V1__v */

            DEMATRIX(u8_Red,u8_Green,u8_Blue,Yval_32,U_32,V_32);

            /* Pack 8 bit R,G,B to RGB565 */
            #ifdef  LITTLE_ENDIAN
                    u32_rgb_temp2 = PACK_RGB565(0,u8_Red,u8_Green,u8_Blue);
            #else   /* LITTLE_ENDIAN */
                    u32_rgb_temp2 = PACK_RGB565(16,u8_Red,u8_Green,u8_Blue);
            #endif  /* LITTLE_ENDIAN */


            pu8_src_top_Y = pu8_data_in[YPlane] + (u32_x_accum_Y >> 16) ;
            pu8_src_bottom_Y = pu8_src_top_Y + u32_stride_in[YPlane];

            /* Weighted combination */
            Y_32 = (((pu8_src_top_Y[0]*(16-u32_x_frac_Y) + pu8_src_top_Y[1]*u32_x_frac_Y)
                    *(16-u32_y_frac_Y) + (pu8_src_bottom_Y[0]*(16-u32_x_frac_Y)
                    + pu8_src_bottom_Y[1]*u32_x_frac_Y)*u32_y_frac_Y ) >> 8);
            /* YUV to RGB */
            #ifdef __RGB_V1__
                    Yval_32 = Y_32*37;
            #else   /* __RGB_V1__v */
                    Yval_32 = Y_32*0x2568;
            #endif  /* __RGB_V1__v */

            DEMATRIX(u8_Red,u8_Green,u8_Blue,Yval_32,U_32,V_32);

            /* Pack 8 bit R,G,B to RGB565 */
            #ifdef  LITTLE_ENDIAN
                    *(pu32_rgb_data_current)++ = u32_rgb_temp1 |
                                                        PACK_RGB565(16,u8_Red,u8_Green,u8_Blue);
            #else   /* LITTLE_ENDIAN */
                    *(pu32_rgb_data_current)++ = u32_rgb_temp1 |
                                                        PACK_RGB565(0,u8_Red,u8_Green,u8_Blue);
            #endif  /* LITTLE_ENDIAN */


            pu8_src_top_Y = pu8_data_in1[YPlane]+ (u32_x_accum_Y >> 16);
            pu8_src_bottom_Y = pu8_src_top_Y + u32_stride_in[YPlane];

            /* Weighted combination */
            Y_32 = (((pu8_src_top_Y[0]*(16-u32_x_frac_Y) + pu8_src_top_Y[1]*u32_x_frac_Y)
                    *(16-u32_rgb_temp4) + (pu8_src_bottom_Y[0]*(16-u32_x_frac_Y)
                    + pu8_src_bottom_Y[1]*u32_x_frac_Y)*u32_rgb_temp4 )>>8);

            u32_x_accum_Y += u32_x_inc[YPlane];
            /* YUV to RGB */
            #ifdef __RGB_V1__
                    Yval_32=Y_32*37;
            #else   /* __RGB_V1__v */
                    Yval_32=Y_32*0x2568;
            #endif  /* __RGB_V1__v */

            DEMATRIX(u8_Red,u8_Green,u8_Blue,Yval_32,U_32,V_32);

            /* Pack 8 bit R,G,B to RGB565 */
            #ifdef  LITTLE_ENDIAN
                    *(pu32_rgb_data_next)++ = u32_rgb_temp2 |
                                                        PACK_RGB565(16,u8_Red,u8_Green,u8_Blue);
            #else   /* LITTLE_ENDIAN */
                    *(pu32_rgb_data_next)++ = u32_rgb_temp2 |
                                                        PACK_RGB565(0,u8_Red,u8_Green,u8_Blue);
            #endif  /* LITTLE_ENDIAN */

        }   /* End of horizontal scanning */

        u32_y_accum_Y  =  u32_rgb_temp3 + (u32_y_inc[YPlane]);
        u32_y_accum_U += (u32_y_inc[UPlane]);

        /* Y plane row update */
        if (u32_y_accum_Y >> 16)
        {
            pu8_data_in[YPlane] =  pu8_data_in1[YPlane] +
                                                ((u32_y_accum_Y >> 16) * (u32_stride_in[YPlane]));
            u32_y_accum_Y &= 0xffff;
        }
        else
        {
            pu8_data_in[YPlane] = pu8_data_in1[YPlane];
        }
        /* U and V planes row update */
        if (u32_y_accum_U >> 16)
        {
            pu8_data_in[UPlane] =  pu8_data_in[UPlane] +
                                                (u32_y_accum_U >> 16) * (u32_stride_in[UPlane]);
            pu8_data_in[VPlane] =  pu8_data_in[VPlane] +
                                                (u32_y_accum_U >> 16) * (u32_stride_in[VPlane]);
            u32_y_accum_U &= 0xffff;
        }

        pu32_rgb_data_start += u32_stride_out;

    }   /* End of vertical scanning */
    return M4VIFI_OK;
}

