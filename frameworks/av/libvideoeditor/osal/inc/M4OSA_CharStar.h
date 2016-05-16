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
 * @file         M4OSA_CharStar.h
 * @ingroup
 * @brief        external API of the Char Star set of functions.
 ************************************************************************
*/

#ifndef M4OSA_CHARSTAR_H
#define M4OSA_CHARSTAR_H

/* general OSAL types and prototypes inclusion                      */
#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4OSA_Time.h"
#include "M4OSA_FileCommon.h"

/* types definition                                                          */
typedef enum
{
   M4OSA_kchrDec  = 0x01,
   M4OSA_kchrHexa = 0x02,
   M4OSA_kchrOct  = 0x03
} M4OSA_chrNumBase;

/* error and warning codes                                                   */
#define M4ERR_CHR_STR_OVERFLOW M4OSA_ERR_CREATE(M4_ERR,M4OSA_CHARSTAR,0x000001)
#define M4ERR_CHR_CONV_FAILED  M4OSA_ERR_CREATE(M4_ERR,M4OSA_CHARSTAR,0x000002)
#define M4WAR_CHR_NOT_FOUND    M4OSA_ERR_CREATE(M4_WAR,M4OSA_CHARSTAR,0x000001)
#define M4WAR_CHR_NUM_RANGE    M4OSA_ERR_CREATE(M4_WAR,M4OSA_CHARSTAR,0x000002)
#define M4WAR_CHR_NEGATIVE     M4OSA_ERR_CREATE(M4_WAR,M4OSA_CHARSTAR,0x000003)

/* prototypes of the Char Star functions                                     */
#ifdef __cplusplus
extern "C"
{
#endif

M4OSAL_CHARSTAR_EXPORT_TYPE M4OSA_ERR M4OSA_chrNCopy          (M4OSA_Char   *strOut,
                                   M4OSA_Char   *strIn,
                                   M4OSA_UInt32 len2Copy);
M4OSAL_CHARSTAR_EXPORT_TYPE M4OSA_ERR M4OSA_chrAreIdentical   (M4OSA_Char   *strIn1,
                                   M4OSA_Char   *strIn2,
                                   M4OSA_Bool  *result);
M4OSAL_CHARSTAR_EXPORT_TYPE M4OSA_ERR M4OSA_chrGetUInt32      (M4OSA_Char   *strIn,
                                   M4OSA_UInt32 *val,
                                   M4OSA_Char   **strOut,
                                   M4OSA_chrNumBase base);
M4OSAL_CHARSTAR_EXPORT_TYPE M4OSA_ERR M4OSA_chrGetUInt16      (M4OSA_Char   *strIn,
                                   M4OSA_UInt16 *val,
                                   M4OSA_Char   **strOut,
                                   M4OSA_chrNumBase base);
M4OSAL_CHARSTAR_EXPORT_TYPE M4OSA_ERR M4OSA_chrSPrintf         (M4OSA_Char  *strOut,
                                   M4OSA_UInt32 strOutMaxLen,
                                   M4OSA_Char   *format,
                                   ...);

#ifdef __cplusplus
}
#endif

#endif

