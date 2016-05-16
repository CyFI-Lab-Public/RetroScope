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
 * @file         M4OSA_FileWriter.h
 * @ingroup      OSAL
 * @brief        File writer
 * @note         This file declares functions and types to write in a file.
 ************************************************************************
*/


#ifndef M4OSA_FILEWRITER_H
#define M4OSA_FILEWRITER_H

#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4OSA_FileCommon.h"
#include "M4OSA_Memory.h"


/** This enum defines the option ID to be used in M4OSA_FileWriteGetOption()
and M4OSA_FileWriteSetOption()*/
typedef enum
{
   /** Get the file URL, provided by the M4OSA_FileWriteOpen (M4OSA_Char*)*/
   M4OSA_kFileWriteGetURL
               = M4OSA_OPTION_ID_CREATE(M4_READ, M4OSA_FILE_WRITER, 0x01),

   /** Get the file attributes (M4OSA_FileAttribute*)*/
   M4OSA_kFileWriteGetAttribute
               = M4OSA_OPTION_ID_CREATE(M4_READ, M4OSA_FILE_WRITER, 0x02),

   /** Get the reader context for read & write file. (M4OSA_Context*)*/
   M4OSA_kFileWriteGetReaderContext
               = M4OSA_OPTION_ID_CREATE(M4_READ, M4OSA_FILE_WRITER, 0x03),

   M4OSA_kFileWriteGetFilePosition
               = M4OSA_OPTION_ID_CREATE(M4_READ, M4OSA_FILE_WRITER, 0x04),

   M4OSA_kFileWriteGetFileSize
               = M4OSA_OPTION_ID_CREATE(M4_READ, M4OSA_FILE_WRITER, 0x05),


    M4OSA_kFileWriteLockMode
               = M4OSA_OPTION_ID_CREATE(M4_READWRITE, M4OSA_FILE_WRITER, 0x06),


   /** Check lock of file */
   M4OSA_kFileWriteDescMode
                = M4OSA_OPTION_ID_CREATE(M4_READWRITE, M4OSA_FILE_WRITER, 0x07)
} M4OSA_FileWriteOptionID;


/** This structure stores the set of the function pointer to access to a file
    in read mode*/
typedef struct
{
   M4OSA_ERR (*openWrite)   (M4OSA_Context* context,
                             M4OSA_Void* fileDescriptor,
                             M4OSA_UInt32 fileModeAccess);

   M4OSA_ERR (*writeData)   (M4OSA_Context context,
                             M4OSA_MemAddr8 data,
                             M4OSA_UInt32 size);

   M4OSA_ERR (*seek)        (M4OSA_Context context,
                             M4OSA_FileSeekAccessMode seekMode,
                             M4OSA_FilePosition* position);

   M4OSA_ERR (*Flush)       (M4OSA_Context context);
   M4OSA_ERR (*closeWrite)  (M4OSA_Context context);
   M4OSA_ERR (*setOption)   (M4OSA_Context context,
                             M4OSA_OptionID optionID,
                             M4OSA_DataOption optionValue);

   M4OSA_ERR (*getOption)   (M4OSA_Context context,
                             M4OSA_OptionID optionID,
                             M4OSA_DataOption* optionValue);
} M4OSA_FileWriterPointer;

#ifdef __cplusplus
extern "C"
{
#endif

M4OSAL_FILE_EXPORT_TYPE M4OSA_ERR M4OSA_fileWriteOpen       (M4OSA_Context* context,
                                     M4OSA_Void* fileDescriptor,
                                     M4OSA_UInt32 fileModeAccess);

M4OSAL_FILE_EXPORT_TYPE M4OSA_ERR M4OSA_fileWriteData       (M4OSA_Context context,
                                     M4OSA_MemAddr8 data,
                                     M4OSA_UInt32 size);

/* Pierre Lebeaupin 2008/04/29: WARNING! the feature of file*Seek which returns
the position in the file (from the beginning) after the seek in the "position"
pointer has been found to be unreliably (or sometimes not at all) implemented
in some OSALs, so relying on it is strongly discouraged, unless you really want
to have a pizza evening. */
M4OSAL_FILE_EXPORT_TYPE M4OSA_ERR M4OSA_fileWriteSeek       (M4OSA_Context context,
                                     M4OSA_FileSeekAccessMode seekMode,
                                     M4OSA_FilePosition* position);

M4OSAL_FILE_EXPORT_TYPE M4OSA_ERR M4OSA_fileWriteClose      (M4OSA_Context context);

M4OSAL_FILE_EXPORT_TYPE M4OSA_ERR M4OSA_fileWriteFlush      (M4OSA_Context context);

M4OSAL_FILE_EXPORT_TYPE M4OSA_ERR M4OSA_fileWriteGetOption  (M4OSA_Context context,
                                     M4OSA_OptionID optionID,
                                     M4OSA_DataOption* optionValue);

M4OSAL_FILE_EXPORT_TYPE M4OSA_ERR M4OSA_fileWriteSetOption  (M4OSA_Context context,
                                     M4OSA_OptionID optionID,
                                     M4OSA_DataOption optionValue);

#ifdef __cplusplus
}
#endif


#endif /*M4OSA_FILEWRITER_H*/

