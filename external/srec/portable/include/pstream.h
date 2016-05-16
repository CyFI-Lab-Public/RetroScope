/*---------------------------------------------------------------------------*
 *  pstream.h  *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                               *
 *                                                                           *
 *  Licensed under the Apache License, Version 2.0 (the 'License');          *
 *  you may not use this file except in compliance with the License.         *
 *                                                                           *
 *  You may obtain a copy of the License at                                  *
 *      http://www.apache.org/licenses/LICENSE-2.0                           *
 *                                                                           *
 *  Unless required by applicable law or agreed to in writing, software      *
 *  distributed under the License is distributed on an 'AS IS' BASIS,        *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * 
 *  See the License for the specific language governing permissions and      *
 *  limitations under the License.                                           *
 *                                                                           *
 *---------------------------------------------------------------------------*/



#ifndef _PORTSTREAM_H_
#define _PORTSTREAM_H_

#ifdef __cplusplus
extern "C"
{
#endif

#include <stdio.h>
#include "ptypes.h"

#ifdef PFILE_VIRTUAL_SUPPORT

  struct FileBufferFrame;
  typedef struct PORT_FILE_HANDLE
  {
    const char              *filename;
    struct FileBufferFrame  *startFrame;
    struct FileBufferFrame  *endFrame;
    struct FileBufferFrame  *curFrame;  /* current buffer; useful for writable file */
    const unsigned char     *curPos;
    const unsigned char     *endPos;
    unsigned int            size;       /* total buffer size; useful for writable file */
    unsigned int            frame_size; /* buffer size in current frame; useful for writable file */
    int                     eof;
    int                     mode;      /* 0 readonly text; 1 readonly binary; 2 writable text; 3 writalbe binary */
  }
  PORT_FILE_HANDLE;
  
  typedef PORT_FILE_HANDLE* PORT_FILE;
  
  typedef struct _FileRecord
  {
    char name[80];
    unsigned char *start;
    int end;              /* offset of the end of the file */
    int size;             /* total buffer size */
    int mode;
  }
  FileRecord;
  
  typedef struct VirtualFileTable_t
  {
    const FileRecord* pFileTable;
    const unsigned char* pFirstFile;
  }
  VirtualFileTable;
  
  /* Function prototypes */
  PORTABLE_API void    PortFileInit(void);
  PORTABLE_API PORT_FILE PortFopen(const char* filename, const char* mode);
  PORTABLE_API int   PortFclose(PORT_FILE PortFile);
  PORTABLE_API size_t  PortFread(void* buffer, size_t size, size_t count, PORT_FILE PortFile);
  PORTABLE_API size_t  PortFwrite(const void* buffer, size_t size, size_t count, PORT_FILE PortFile);
  PORTABLE_API int   PortFseek(PORT_FILE PortFile, long offset, int origin);
  PORTABLE_API long    PortFtell(PORT_FILE PortFile);
  PORTABLE_API int   PortFprintf(PORT_FILE PortFile, const char* format, ...);
  PORTABLE_API char*  PortFgets(char* string, int n, PORT_FILE PortFile);
  PORTABLE_API int   PortFflush(PORT_FILE PortFile);
  PORTABLE_API int   PortFeof(PORT_FILE PortFile);
  PORTABLE_API int   PortFgetc(PORT_FILE PortFile);
  PORTABLE_API int   PortFscanf(PORT_FILE PortFile, const char *format, ...);
  PORTABLE_API int   PortFerror(PORT_FILE PortFile);
  PORTABLE_API void   PortClearerr(PORT_FILE PortFile);
  PORTABLE_API void    PortRewind(PORT_FILE PortFile);
  PORTABLE_API PORT_FILE PortFreopen(const char *path, const char *mode, PORT_FILE PortFile);
  PORTABLE_API char*    PortGetcwd(char *buffer, int maxlen);
  PORTABLE_API int      PortMkdir(const char *dirname);
  
  /* this function is to create a file with the limit size */
  PORTABLE_API int      PortFcreate(const char *fname, void *pBuffer, int size);
  PORTABLE_API void     PortFdelete(const char *fname);
  
  PORTABLE_API void     PortSetFileTable(const FileRecord* pFileTable, const unsigned char* pFirstFile);
  
  void     SetFileTable(VirtualFileTable *table);
  
#endif /* #ifdef PFILE_VIRTUAL_SUPPORT */
  
#ifdef __cplusplus
}
#endif

#endif /* _PORTSTREAM_H */
