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
 * @file         M4OSA_FileReader.h
 * @ingroup      OSAL
 * @brief        File reader
 * @note         This file declares functions and types to read a file.
 ************************************************************************
*/


#ifndef M4OSA_FILEREADER_H
#define M4OSA_FILEREADER_H


#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4OSA_FileCommon.h"
#include "M4OSA_Memory.h"



/** This enum defines the option ID to be used in M4OSA_FileReadGetOption()
    and M4OSA_FileReadSetOption()*/
typedef enum M4OSA_FileReadOptionID
{
   /** Get the file size (M4OSA_fpos*)*/
   M4OSA_kFileReadGetFileSize
                  = M4OSA_OPTION_ID_CREATE(M4_READ, M4OSA_FILE_READER, 0x01),

      /** Get the file attributes (M4OSA_FileAttribute*)*/
   M4OSA_kFileReadGetFileAttribute
                  = M4OSA_OPTION_ID_CREATE(M4_READ, M4OSA_FILE_READER, 0x02),

   /** Get the file URL, provided by the M4OSA_FileReadOpen (M4OSA_Char*)*/
   M4OSA_kFileReadGetURL
                  = M4OSA_OPTION_ID_CREATE(M4_READ, M4OSA_FILE_READER, 0x03),

   /** Get the file position (M4OSA_fpos*)*/
   M4OSA_kFileReadGetFilePosition
                  = M4OSA_OPTION_ID_CREATE(M4_READ, M4OSA_FILE_READER, 0x04),

   /** Check end of file: TRUE if the EOF has been reached, FALSE else
       (M4OSA_Bool*)*/
   M4OSA_kFileReadIsEOF
                  = M4OSA_OPTION_ID_CREATE(M4_READ, M4OSA_FILE_READER, 0x05),

   /** Check lock of file */
   M4OSA_kFileReadLockMode
                  = M4OSA_OPTION_ID_CREATE(M4_READWRITE, M4OSA_FILE_READER, 0x06)

} M4OSA_FileReadOptionID;





/** This structure stores the set of the function pointer to access to a
    file in read mode*/
typedef struct
{
   M4OSA_ERR (*openRead)   (M4OSA_Context* context,
                            M4OSA_Void* fileDescriptor,
                            M4OSA_UInt32 fileModeAccess);

   M4OSA_ERR (*readData)   (M4OSA_Context context,
                            M4OSA_MemAddr8 buffer,
                            M4OSA_UInt32* size);

   M4OSA_ERR (*seek)       (M4OSA_Context context,
                            M4OSA_FileSeekAccessMode seekMode,
                            M4OSA_FilePosition* position);

   M4OSA_ERR (*closeRead)  (M4OSA_Context context);

   M4OSA_ERR (*setOption)  (M4OSA_Context context,
                            M4OSA_FileReadOptionID optionID,
                            M4OSA_DataOption optionValue);

   M4OSA_ERR (*getOption)  (M4OSA_Context context,
                            M4OSA_FileReadOptionID optionID,
                            M4OSA_DataOption *optionValue);
} M4OSA_FileReadPointer;

#ifdef __cplusplus
extern "C"
{
#endif

M4OSAL_FILE_EXPORT_TYPE M4OSA_ERR M4OSA_fileReadOpen        (M4OSA_Context* context,
                                     M4OSA_Void* fileDescriptor,
                                     M4OSA_UInt32 fileModeAccess);

M4OSAL_FILE_EXPORT_TYPE M4OSA_ERR M4OSA_fileReadData        (M4OSA_Context context,
                                     M4OSA_MemAddr8 buffer,
                                     M4OSA_UInt32* size);

M4OSAL_FILE_EXPORT_TYPE M4OSA_ERR M4OSA_fileReadSeek        (M4OSA_Context context,
                                     M4OSA_FileSeekAccessMode seekMode,
                                     M4OSA_FilePosition* position);

M4OSAL_FILE_EXPORT_TYPE M4OSA_ERR M4OSA_fileReadClose       (M4OSA_Context context);

M4OSAL_FILE_EXPORT_TYPE M4OSA_ERR M4OSA_fileReadGetOption   (M4OSA_Context context,
                                     M4OSA_FileReadOptionID optionID,
                                     M4OSA_DataOption *optionValue);

M4OSAL_FILE_EXPORT_TYPE M4OSA_ERR M4OSA_fileReadSetOption   (M4OSA_Context context,
                                     M4OSA_FileReadOptionID optionID,
                                     M4OSA_DataOption optionValue);

#ifdef __cplusplus
}
#endif

#endif   /*M4OSA_FILEREADER_H*/

