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
 * @file    M4_Utils.h
 * @brief    Utilities
 * @note    This file defines utility macros
*************************************************************************
*/
#ifndef __M4_UTILS_H__
#define __M4_UTILS_H__

#ifdef __cplusplus
extern "C" {
#endif

/*    M4_MediaTime definition
    This type is used internally by some shell components */
#include "M4OSA_Types.h"
typedef M4OSA_Double    M4_MediaTime;

/*    GET_MEMORY32 macro definition
    This macro is used by the 3GP reader*/
#ifdef __BIG_ENDIAN
#define GET_MEMORY32(x) (x)
#else
#define GET_MEMORY32(x) ( (((x)&0xff)<<24) | (((x)&0xff00)<<8) |\
     (((x)&0xff0000)>>8) | (((x)&0xff000000)>>24) )
#endif /*__BIG_ENDIAN*/

#ifdef __cplusplus
}
#endif

#endif /* __M4_UTILS_H__*/

