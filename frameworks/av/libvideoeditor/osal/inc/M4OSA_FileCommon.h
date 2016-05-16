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
 * @file         M4OSA_FileCommon.h
 * @ingroup      OSAL
 * @brief        File common
 * @note         This file declares functions and types used by both the file
 *               writer and file reader.
 ************************************************************************
*/


#ifndef M4OSA_FILECOMMON_H
#define M4OSA_FILECOMMON_H

#include "M4OSA_Types.h"
#include "M4OSA_Time.h"
#include "M4OSA_Error.h"
#include "M4OSA_OptionID.h"


typedef M4OSA_Int32 M4OSA_FilePosition;

/** This enum defines the application mode access.
 *  ie, the application uses a file descriptor to read or to write  or
 *  both to read and write at the same time.
 *  This structure is used for MM project only. It enables to read and write to a file
 *  with one descriptor.
 */
typedef enum
{
   M4OSA_kDescNoneAccess    = 0x00,
   M4OSA_kDescReadAccess    = 0x01,    /** The Descriptor reads only from the file */
   M4OSA_kDescWriteAccess    = 0x02,    /** The Descriptor writes only from the file*/
   M4OSA_kDescRWAccess        = 0x03    /** The Descriptor reads and writes from/in the file*/
} M4OSA_DescrModeAccess;


/** This enum defines the file mode access. Both text mode as binary mode
    cannot be set together.*/
typedef enum
{
   /** The file must be accessed in read only mode*/
   M4OSA_kFileRead             = 0x01,
   /** The file must be accessed in write only mode*/
   M4OSA_kFileWrite            = 0x02,
   /** The file must be accessed in append mode (An existing file must
       be available to append data)*/
   M4OSA_kFileAppend           = 0x04,
   /** If the file does not exist, it will be created*/
   M4OSA_kFileCreate           = 0x08,
   /** Data are processed as binary one, there is no data management*/
   M4OSA_kFileIsTextMode       = 0x10
} M4OSA_FileModeAccess;


/** This type is used to store a date.*/
typedef struct
{
   /** Time scale (tick number per second)*/
   M4OSA_UInt32 timeScale;
   /** Date expressed in the time scale*/
   M4OSA_Time   time;
   /** Year of the absolute time (1900, 1970 or 2000)*/
   M4OSA_UInt32 referenceYear;
} M4OSA_Date;


/** This strucure defines the file attributes*/
typedef struct
{
   /** The file mode access*/
   M4OSA_FileModeAccess    modeAccess;
   /** The creation date*/
   M4OSA_Date              creationDate;
   /** The last modification date*/
   M4OSA_Date              modifiedDate;
   /** The last access date (read)*/
   M4OSA_Date              lastAccessDate;
} M4OSA_FileAttribute;



/** This enum defines the seek behavior*/
typedef enum M4OSA_FileSeekAccessMode
{
   /** Relative to the beginning of the file*/
   M4OSA_kFileSeekBeginning            = 0x01,
   /** Relative to the end of the file*/
   M4OSA_kFileSeekEnd                  = 0x02,
   /** Relative to the current file position*/
   M4OSA_kFileSeekCurrent              = 0x03
} M4OSA_FileSeekAccessMode;


/* Error codes */
#define M4ERR_FILE_NOT_FOUND         M4OSA_ERR_CREATE(M4_ERR, M4OSA_FILE_COMMON, 0x000001)
#define M4ERR_FILE_LOCKED            M4OSA_ERR_CREATE(M4_ERR, M4OSA_FILE_COMMON, 0x000002)
#define M4ERR_FILE_BAD_MODE_ACCESS   M4OSA_ERR_CREATE(M4_ERR, M4OSA_FILE_COMMON, 0x000003)
#define M4ERR_FILE_INVALID_POSITION  M4OSA_ERR_CREATE(M4_ERR, M4OSA_FILE_COMMON, 0x000004)


#endif /*M4OSA_FILECOMMON_H*/

