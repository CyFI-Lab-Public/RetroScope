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
 * @file         M4OSA_Memory.h
 * @ingroup      OSAL
 * @brief        Memory allocation
 * @note         This file defines function prototypes to allocate
 *               and free memory.
 ************************************************************************
*/

#ifndef M4OSA_MEMORY_H
#define M4OSA_MEMORY_H


#include "M4OSA_Types.h"
#include "M4OSA_Error.h" /*for M4OSA_CoreID definition*/

typedef M4OSA_Int32* M4OSA_MemAddr32;
typedef M4OSA_Int8*  M4OSA_MemAddr8;

#ifdef __cplusplus
extern "C"
{
#endif

M4OSAL_MEMORY_EXPORT_TYPE extern M4OSA_MemAddr32 M4OSA_32bitAlignedMalloc (M4OSA_UInt32 size,
                                                               M4OSA_CoreID coreID,
                                                               M4OSA_Char* string);

M4OSAL_MEMORY_EXPORT_TYPE extern M4OSA_ERR M4OSA_randInit(void);


M4OSAL_MEMORY_EXPORT_TYPE extern M4OSA_ERR M4OSA_rand(M4OSA_Int32* out_value,
                                                      M4OSA_UInt32 max_value);


#ifdef __cplusplus
}
#endif

#endif

