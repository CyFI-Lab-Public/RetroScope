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
 * @file    M4MP4W_Utils.h
 * @brief   Utilities and private functions declaration for the MP4 writer
 ******************************************************************************
 */

#ifndef M4MP4W_UTILS_H
#define M4MP4W_UTILS_H

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#include "NXPSW_CompilerSwitches.h"

#ifndef _M4MP4W_USE_CST_MEMORY_WRITER

/* includes */
#include "M4OSA_Types.h"
#include "M4OSA_FileWriter.h"


/**
 ******************************************************************************
 * Utility functions to write data in big endian
 ******************************************************************************
 */
M4OSA_ERR M4MP4W_putByte(M4OSA_UChar c,    M4OSA_FileWriterPointer* fileFunction,
                         M4OSA_Context context);
M4OSA_ERR M4MP4W_putBE16(M4OSA_UInt32 val, M4OSA_FileWriterPointer* fileFunction,
                         M4OSA_Context context);
M4OSA_ERR M4MP4W_putBE24(M4OSA_UInt32 val, M4OSA_FileWriterPointer* fileFunction,
                         M4OSA_Context context);
M4OSA_ERR M4MP4W_putBE32(M4OSA_UInt32 val, M4OSA_FileWriterPointer* fileFunction,
                         M4OSA_Context context);

/**
 ******************************************************************************
 * Write a bulk of data into the specified file, size is given in bytes
 ******************************************************************************
 */
M4OSA_ERR M4MP4W_putBlock(const M4OSA_UChar* Block, M4OSA_UInt32 size,
                          M4OSA_FileWriterPointer* fileFunction, M4OSA_Context context);

/**
 ******************************************************************************
 * Convert the 'nb' unsigned integers in 'tab' table from LE into BE
 ******************************************************************************
 */
void M4MP4W_table32ToBE(M4OSA_UInt32* tab, M4OSA_UInt32 nb);

/**
 ******************************************************************************
 * Convert an unsigned 32 bits integer from LE into BE
 ******************************************************************************
 */
void M4MP4W_convertInt32BE(M4OSA_UInt32* valPtr);

/**
 ******************************************************************************
 * Re-allocation function
 ******************************************************************************
 */
void* M4MP4W_realloc(M4OSA_MemAddr32 ptr, M4OSA_UInt32 oldSize, M4OSA_UInt32 newSize);

/**
 ******************************************************************************
 * De-allocate the context
 * This method is no longer in the writer external interface, but is called from
 * the function M4MP4W_closeWrite
 ******************************************************************************
 */
M4OSA_ERR M4MP4W_freeContext(M4OSA_Context context);


#ifdef _M4MP4W_OPTIMIZE_FOR_PHONE
/**
 ******************************************************************************
 * Put Hi and Lo u16 part in a u32 variable
 ******************************************************************************
 */
M4OSA_Void M4MP4W_put32_Hi(M4OSA_UInt32* tab, M4OSA_UInt16 Hi);
M4OSA_Void M4MP4W_put32_Lo(M4OSA_UInt32* tab, M4OSA_UInt16 Lo);
M4OSA_UInt16 M4MP4W_get32_Hi(M4OSA_UInt32* tab);
M4OSA_UInt16 M4MP4W_get32_Lo(M4OSA_UInt32* tab);
#endif

#endif /* _M4MP4W_USE_CST_MEMORY_WRITER */

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /*M4MP4W_UTILS_H*/

