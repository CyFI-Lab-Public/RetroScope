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
 * @file    M4VD_Null.h
 * @brief   Implementation of the a "null" video decoder,i.e. a decoder
 *          that does not do actual decoding.
 * @note    This file defines the getInterface function.
*************************************************************************
*/
#ifndef __M4DECODER_NULL_H__
#define __M4DECODER_NULL_H__

#include "M4DECODER_Common.h"

#ifdef __cplusplus
extern "C" {
#endif


/**
 ************************************************************************
 * @brief Retrieves the interface implemented by the decoder
 * @param pDecoderType        : Pointer to a M4DECODER_VideoType
 *                             (allocated by the caller)
 *                             that will be filled with the decoder type
 * @param pDecoderInterface   : Address of a pointer that will be set to
 *                              the interface implemented by this decoder.
 *                              The interface is a structure allocated by
 *                              this function and must be freed by the caller.
 *
 * @returns : M4NO_ERROR  if OK
 *            M4ERR_ALLOC if allocation failed
 ************************************************************************
*/
M4OSA_ERR M4DECODER_NULL_getInterface( M4DECODER_VideoType *pDecoderType,
                                 M4DECODER_VideoInterface **pDecoderInterface);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /*__M4DECODER_NULL_H__*/

