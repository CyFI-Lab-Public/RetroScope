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
 * @file    M4AD_Null.h
 * @brief    Implementation of the decoder public interface that do nothing
 * @note    This file defines the getInterface function.
*************************************************************************
*/
#ifndef __M4AD_NULL_H__
#define __M4AD_NULL_H__

#include "M4AD_Common.h"

#ifdef __cplusplus
extern "C" {
#endif


/**
 ************************************************************************
 * @brief Retrieves the interface implemented by the decoder
 * @param pDecoderType        : pointer on an M4AD_Type (allocated by the caller)
 *                              that will be filled with the decoder type supported by this decoder
 * @param pDecoderInterface   : address of a pointer that will be set to the interface implemented
 *                              by this decoder. The interface is a structure allocated by the
 *                              function and must be un-allocated by the caller.
 *
 * @return : M4NO_ERROR  if OK
 *           M4ERR_ALLOC if allocation failed
 ************************************************************************
*/
M4OSA_ERR M4AD_NULL_getInterface( M4AD_Type *pDecoderType, M4AD_Interface **pDecoderInterface);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /*__M4AD_NULL_H__*/

