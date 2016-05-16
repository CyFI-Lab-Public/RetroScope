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
 * @file   M4READER_Amr.h
 * @brief  Generic encapsulation of the core amr reader
 * @note   This file declares the generic shell interface retrieving function
 *         of the AMR reader
 ************************************************************************
*/
#ifndef __M4READER_AMR_H__
#define __M4READER_AMR_H__

#include "M4READER_Common.h"

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/**
*************************************************************************
* @brief Retrieves the generic interfaces implemented by the reader
*
* @param pMediaType             : Pointer on a M4READER_MediaType (allocated by the caller)
*                              that will be filled with the media type supported by this reader
* @param pRdrGlobalInterface : Address of a pointer that will be set to the global interface
*                              implemented by this reader. The interface is a structure allocated
*                              by the function and must be un-allocated by the caller.
* @param pRdrDataInterface   : Address of a pointer that will be set to the data interface
*                              implemented by this reader. The interface is a structure allocated
*                              by the function and must be un-allocated by the caller.
*
* @returns : M4NO_ERROR     if OK
*             ERR_ALLOC      if an allocation failed
*            ERR_PARAMETER  at least one parameter is not properly set (in DEBUG only)
*************************************************************************
*/
M4OSA_ERR M4READER_AMR_getInterfaces(M4READER_MediaType *pMediaType,
                                      M4READER_GlobalInterface **pRdrGlobalInterface,
                                      M4READER_DataInterface **pRdrDataInterface);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /*__M4READER_AMR_H__*/

