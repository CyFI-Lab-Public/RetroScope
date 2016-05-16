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
 * @file        M4VIFI_Defines.h
 * @brief        Macro Definition
 * @note        This file defines all the macro used in the filter library
 ******************************************************************************
*/

#ifndef _M4VIFI_DEFINES_H_
#define _M4VIFI_DEFINES_H_

/**
 *****************************************************************************
 *                    Macros used for color transform RGB565 to YUV
 *****************************************************************************
*/
#define CST_RGB_16_SIZE 2
#define Y16(r, g, b) CLIP(  ( ( (80593 * r)+(77855 * g)+(30728 * b)) >> 15))
#define U16(r, g, b) CLIP(128+ ( ( -(45483 * r)-(43936 * g)+(134771 * b)) >> 15 ))
#define V16(r, g, b) CLIP(128+ ( ( (134771 * r)-(55532 * g)-(21917 * b)) >> 15  ))


/**
 *****************************************************************************
 *    Macros used for color transform YUV to RGB
 *    B = 1.164(Y - 16)                  + 2.018(U - 128)
 *  G = 1.164(Y - 16) - 0.813(V - 128) - 0.391(U - 128)
 *  R = 1.164(Y - 16) + 1.596(V - 128)
 *  Above Conversion Formula is implemented for fixed point operation
 *****************************************************************************
*/

#define CST_RGB_24_SIZE 3

#ifdef __RGB_V1__
#define DEMATRIX(Rx,Gx,Bx,Yx37,Ux,Vx) \
    Rx = CLIP(((Yx37 + (Vx * 51) + 16) >> 5) - 223); \
    Gx = CLIP(((Yx37 - ((Ux+(Vx<<1)) * 13) +16) >> 5) + 135); \
    Bx = CLIP(((Yx37 + (Ux * 65) + 16) >> 5) - 277)
#else
#define DEMATRIX(Rx,Gx,Bx,Yx2568,Ux,Vx) \
    Rx = CLIP(((Yx2568 +                 (Vx * 0x3343) + (M4VIFI_Int32)0xffe40800) >> 13)); \
    Gx = CLIP(((Yx2568 - (Ux * 0x0c92) - (Vx * 0x1a1e) + (M4VIFI_Int32)0x00110180) >> 13)); \
    Bx = CLIP(((Yx2568 + (Ux * 0x40cf)                    + (M4VIFI_Int32)0xffdd4200) >> 13));
#endif /* __RGB_V1__ */

/**
 *****************************************************************************
 *    Packing and Unpacking is different for little and big endian
 *  r, g, b, Rx, Gx, Bx are 8 bit color value
 *    a, data are 16 bit pixel value
 *****************************************************************************
 */

/* Pack computations common for little endian and big endian modes */
#define    PACK_BGR24(rgb_ptr,Rx,Gx,Bx) {rgb_ptr[0] = (M4VIFI_UInt8)Bx; rgb_ptr[1] =\
                         (M4VIFI_UInt8)Gx; rgb_ptr[2] = (M4VIFI_UInt8)Rx;}
#define    PACK_RGB24(rgb_ptr,Rx,Gx,Bx) {rgb_ptr[0] = (M4VIFI_UInt8)Rx; rgb_ptr[1] =\
                         (M4VIFI_UInt8)Gx; rgb_ptr[2] = (M4VIFI_UInt8)Bx;}

#ifdef BIG_ENDIAN
#define    PACK_RGB565(a, Rx, Gx, Bx) (((Rx >> 3) << (11 + (a)))\
                 | ((Gx >> 2) << (5 + (a))) | ((Bx >> 3) << (a)))
#define    PACK_BGR565(a, Rx, Gx, Bx) (((Bx >> 3) << (11 + (a)))\
                 | ((Gx >> 2) << (5 + (a))) | ((Rx >> 3) << (a)))
#define GET_RGB565(r, g, b, data) {b = ((data) & 31); g =\
                     ((data >> 5) & 63); r = ((data >> 11) & 31);}
#define GET_BGR565(b, g, r, data) \
    r = ((data) & 31); \
    g = ((data >> 5) & 63); \
    b = ((data >> 11) & 31 );
#else /* LITTLE endian: 0x12345678 -> 78 56 34 12 */
#define    PACK_RGB565(a, Rx, Gx, Bx) (((Bx >> 3) << (8 + (a))) \
                  | (((Gx >> 2)&0x7) << (13 + (a))) | ((Gx >> 5) << (a)) | ((Rx >> 3) << (3 + a)))
#define    PACK_BGR565(a, Rx, Gx, Bx) (((Rx >> 3) << (11 + (a))) \
                  | ((Gx >> 2) << (5 + (a))) | ((Bx >> 3) << (a)))
#define GET_RGB565(r, g, b, data) { b = (M4VIFI_UInt8)(((data) & 0x1F00) >> 8); g =\
             (M4VIFI_UInt8)((((data) & 0x7) << 3) | (((data) & 0xE000) >> 13)); r =\
             (M4VIFI_UInt8)(((data) & 0xF8) >> 3);}
#define GET_BGR565(b, g, r, data) \
    b = ((data) & 31); \
    g = ((data >> 5) & 63); \
    r = ((data >> 11) & 31 );
#endif /* BIG_ENDIAN */


#define CST_RGB_24_SIZE 3
#define Y24(r,g,b) CLIP(( ( (19595 * r) + (38470 * g) + (9437 * b) ) >>16))
#define U24(r,g,b) CLIP(128 + ( ( -(11059 * r) - (21709 * g) + (32768 * b)) >>16))
#define V24(r,g,b) CLIP(128 + ( ( (32768 * r) - (27426 * g) - (5329 * b))  >>16))
#define GET_RGB24(r,g,b,s,o) r = s[o]; g = s[o + 1]; b = s[o + 2];

/**
 ***********************************************************************************
 *                    Macro for clipping using the clipping matrix for RGB values
 ***********************************************************************************
*/
/** Clip function ensures values with range of 0 and 255 */
#define        CLIP(x)    *(M4VIFI_ClipTable_zero + (x))
#define        CLIP_OVF        500
#define     CLIP_LUT_SIZE     (256 + 2 * CLIP_OVF)
/** Division table for RGB565 to HLS conversion */
#define        DIVCLIP(x)    *(M4VIFI_DivTable_zero + (x))

/**
 *****************************************************************************
 *                    Endianness (default configuration is Little Endian)
 *****************************************************************************
*/
#if (!defined(LITTLE_ENDIAN) && !defined(BIG_ENDIAN))
/** Default endian setting */
#define LITTLE_ENDIAN
#endif

/**
 *****************************************************************************
 *                    Other macros and define
 *****************************************************************************
*/
/** YUV plane index */
#define PLANES    3
#define YPlane    0
#define UPlane    1
#define VPlane    2

/** Check for value is EVEN */
#ifndef IS_EVEN
#define IS_EVEN(a)    (!(a & 0x01))
#endif

/* Used for fixed point implementation */
#ifndef MAX_SHORT
#define MAX_SHORT    0x10000
#endif

#endif /* _M4VIFI_DEFINES_H_ */

/* End of file M4VIFI_Defines.h */

