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
 * @file         M4OSA_FileCommon_priv.h
 * @ingroup      OSAL
 * @brief        File common private for Android
 * @note         This file declares functions and types used by both the file
 *               writer and file reader.
 ************************************************************************
*/

#ifndef M4OSA_FILECOMMON_PRIV_H
#define M4OSA_FILECOMMON_PRIV_H


#include "M4OSA_FileCommon.h"
#include <stdio.h>

#define M4OSA_isAccessModeActived(compound_mode_access,elementary_mode_access)\
        (((compound_mode_access)&(elementary_mode_access))? 1:0)


typedef enum M4OSA_LastSeek
{
   SeekNone,
   SeekRead,
   SeekWrite
} M4OSA_LastSeek;

/** This structure defines the file context*/
typedef struct {
   M4OSA_UInt32         coreID_read;
   M4OSA_UInt32         coreID_write;
   FILE*                file_desc;
   /** The name of the URL */
   M4OSA_Char*          url_name;
   /** The name of the file */
   M4OSA_Char*          file_name;
   /** The size in bytes of the file */
   M4OSA_FilePosition   file_size;
   /** The file mode access used to open the file */
   M4OSA_FileModeAccess access_mode;
   M4OSA_LastSeek       current_seek;
   M4OSA_FilePosition   read_position;
   M4OSA_FilePosition   write_position;
   M4OSA_Bool           b_is_end_of_file;

#ifdef M4OSA_FILE_BLOCK_WITH_SEMAPHORE
   M4OSA_Context        semaphore_context;
#endif /* M4OSA_FILE_BLOCK_WITH_SEMAPHORE */


   /* These two variables were added to manage case where a file
    * is opened in read and write mode with one descriptor */
    M4OSA_DescrModeAccess    m_DescrModeAccess;
    M4OSA_UInt32            m_uiLockMode;


} M4OSA_FileContext;



M4OSA_ERR M4OSA_fileCommonOpen(M4OSA_UInt16 core_id,
                               M4OSA_Context* context,
                               M4OSA_Char* URL,
                               M4OSA_FileModeAccess fileModeAccess);

M4OSA_ERR M4OSA_fileCommonClose(M4OSA_UInt16 core_id,
                                M4OSA_Context context);

M4OSA_ERR M4OSA_fileCommonGetAttribute(M4OSA_Context context,
                                       M4OSA_FileAttribute* attribute);

M4OSA_ERR M4OSA_fileCommonGetURL(M4OSA_Context context,
                                 M4OSA_Char** url);

M4OSA_ERR M4OSA_fileCommonGetFilename(M4OSA_Char* url,
                                      M4OSA_Char** filename);

M4OSA_ERR M4OSA_fileCommonSeek(M4OSA_Context context,
                               M4OSA_FileSeekAccessMode seekMode,
                               M4OSA_FilePosition* position);

#ifdef UTF_CONVERSION
M4OSA_ERR M4OSA_ToUTF8_OSAL (M4OSA_Void   *pBufferIn,
                             M4OSA_UInt8  *pBufferOut,
                             M4OSA_UInt32 *bufferOutSize);
#endif /*UTF_CONVERSION*/


#endif /*M4OSA_FILECOMMON_PRIV_H*/

