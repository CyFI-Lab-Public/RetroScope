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
 ************************************************************************
 * @file         M4OSA_Types.h
 * @ingroup      OSAL
 * @brief        Abstraction types for Android
 * @note         This file redefines basic types which must be
 *               used to declare any variable.
************************************************************************
*/


#ifndef M4OSA_TYPES_H
#define M4OSA_TYPES_H

#include <ctype.h>
#include <stdio.h>
#include <string.h>
#include "M4OSA_Export.h"
#ifdef __cplusplus
extern "C" {
#endif


typedef signed char     M4OSA_Bool;
typedef unsigned char   M4OSA_UInt8;
typedef signed char     M4OSA_Int8;
typedef unsigned short  M4OSA_UInt16;
typedef signed short    M4OSA_Int16;
typedef unsigned long   M4OSA_UInt32;
typedef signed long     M4OSA_Int32;

typedef signed char     M4OSA_Char;
typedef unsigned char   M4OSA_UChar;

typedef double          M4OSA_Double;
typedef float           M4OSA_Float;

typedef unsigned char   M4OSA_WChar;

typedef void            M4OSA_Void;

/* Min & max definitions*/
#define M4OSA_UINT8_MIN                  0
#define M4OSA_UINT8_MAX                255

#define M4OSA_UINT16_MIN                 0
#define M4OSA_UINT16_MAX             65535

#define M4OSA_UINT32_MIN                 0
#define M4OSA_UINT32_MAX        0xFFFFFFFF

#define M4OSA_INT8_MIN                -128
#define M4OSA_INT8_MAX                 127

#define M4OSA_INT16_MIN             -32768
#define M4OSA_INT16_MAX              32767

#define M4OSA_INT32_MIN       (-0x7FFFFFFF-1)
#define M4OSA_INT32_MAX         0x7FFFFFFF

#define M4OSA_CHAR_MIN                -128
#define M4OSA_CHAR_MAX                 127

#define M4OSA_UCHAR_MIN                  0
#define M4OSA_UCHAR_MAX                255

#define M4OSA_NULL                     0x00
#define M4OSA_TRUE                     0x01
#define M4OSA_FALSE                    0x00
#define M4OSA_WAIT_FOREVER       0xffffffff

#define M4OSA_CONST                   const
#define M4OSA_INLINE                 inline

/* Rollover offset of the clock */
/* This value must be the one of M4OSA_clockGetTime */
#define M4OSA_CLOCK_ROLLOVER           M4OSA_INT32_MAX

typedef void*                M4OSA_Context;

/** It is a unique ID for each core component*/
typedef  M4OSA_UInt16 M4OSA_CoreID;

#ifdef __cplusplus
}
#endif

#endif /*M4OSA_TYPES_H*/

