/*---------------------------------------------------------------------------*
 *  pstream.c  *
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

#include <stdarg.h>
#include <stdlib.h>
#include <stdio.h>

#include <string.h>
#include "passert.h"
#include "pstdio.h"
#include "pmemory.h"
#include "plog.h"

#ifdef __cplusplus
extern "C"
{
#endif

#ifdef PFILE_VIRTUAL_SUPPORT

#define FILETEXTMODE    0x00
#define FILEBINARYMODE   0x01
#define FILEREADMODE    0x00
#define FILEWRITEMODE    0x02

  /* Open a existed writable file (i.e., the file is not closed yet).
     At some cases user knows the filename only but does not know the file handle (1),
     the user could call fopen to open this file again with another file handle (2).
     He/She could get all the information before the last fflush was called via file handle (1).
   */
#define FILEREOPENMODE 0x08
  
#define ISWRITEMODE(mode)  (((mode)&FILEWRITEMODE)  == FILEWRITEMODE)
  
#define ISBINARYMODE(mode) (((mode)&FILEBINARYMODE) == FILEBINARYMODE)
#define ISREOPENMODE(mode) (((mode)&FILEREOPENMODE) == FILEREOPENMODE)
  
  /*
    use a double link list to store the data of the writable file.
    Each entry has 4k space (FILEBUFFERSIZE).
  */
#define FILEBUFFERSIZE 4096     /* 4k for each file buffer entry */
  
  typedef struct FileBufferFrame
  {
    unsigned char          *buffer;       /* do not use pointer here and set it the first */
    size_t                  size;
    size_t                index;                        /* nth buffer, from 0, 1, ... */
    struct FileBufferFrame  *prev;
    struct FileBufferFrame  *next;
    BOOL                    bMalloc;        /* flag, if the buffer malloced here ? */
  }
  FileBufferFrame;
  
  FileRecord pWritableFileRecTable[] =
    {
      {"", 0, 0, 0, 3},
      {"", 0, 0, 0, 3},
      {"", 0, 0, 0, 3},
      {"", 0, 0, 0, 3},
      {"", 0, 0, 0, 3},
      {"", 0, 0, 0, 3},
      {"", 0, 0, 0, 3}
    };
  const nWritableFiles = sizeof(pWritableFileRecTable) / sizeof(pWritableFileRecTable[0]);
  
#ifdef WIN32
  extern const FileRecord pFileRecTable[];
  extern const unsigned char pFileStart0[];
#endif
  
  const FileRecord *pReadOnlyFileRecTable = NULL;
  const unsigned char *g_pFirstFile = NULL;
  
  void SetFileTable(VirtualFileTable *table)
  {
#ifdef WIN32
    pReadOnlyFileRecTable = pFileRecTable;
    g_pFirstFile = pFileStart0;
#else
    if (table)
    {
      pReadOnlyFileRecTable = table->pFileTable;
      g_pFirstFile = table->pFirstFile;
    }
#endif
  }
  
  /*
    size: size of buffer.
    buffer: is NULL, allocate here and set bMalloc as TRUE; otherwise use the external buffer
  */
  FileBufferFrame* CreateFileBufferFrame(size_t size, unsigned char *buffer)
  {
    FileBufferFrame *fb = NULL;
    
    /* create FileBufferFrame */
    fb = (FileBufferFrame *)MALLOC(sizeof(FileBufferFrame), "FileBufferFrame");
    if (fb)
    {
      fb->next = NULL;
      fb->prev = NULL;
      fb->index = 0;
      fb->size = size;
      fb->bMalloc = FALSE;
      
      if (buffer)
        fb->buffer = buffer;
      else
      {
        /* create one buffer frame */
        if ((fb->buffer = (unsigned char *)MALLOC(size, "FileBufferFrame Buffer")) == NULL)
        {
          FREE(fb);
          return NULL;
        }
        fb->bMalloc = TRUE;
      }
    }
    return fb;
  }
  
  /* free FileBufferFrames
     header should be the header of the FileBufferFrame link list
   */
  void DeleteFileBuffers(FileBufferFrame *header)
  {
    FileBufferFrame *next, *curr;
    
    passert(header && header->prev == NULL);    /* delete from the beginning */
    
    curr = header;
    do
    {
      next = curr->next;
      if (curr->bMalloc)
        FREE(curr->buffer);
      FREE(curr);
      curr = next;
    }
    while (next != NULL);
  }
  
  void PortFileInit(void)
  {
    /* No gPortStdin, gPortStdout, and gPortStderr to initialize. */
#ifdef WIN32
    pReadOnlyFileRecTable = pFileRecTable;
    g_pFirstFile = pFileStart0;
#endif
  }
  
  /* Assume that all files have at least one byte in them, that is length is > 0. */
  PORT_FILE PortFopen(const char *filename, const char *mode)
  {
    char *pfn;
    const unsigned char *start;
    int size;
    int text_mode;
    int access_mode;
    int m = 0;
    PORT_FILE PortFile;
    FileBufferFrame *curFrame;
    size_t end;
    
    passert(filename);
    passert(mode);
    
    if (pReadOnlyFileRecTable == NULL)
    {
      passert("File Table is not initialized!" == NULL);
      return NULL;
    }
    
    /* support read and write. */
    if (mode[0] == 'r' || mode[0] == 'w' || mode[0] == 'R') /* w means w+, attaching text */
    {
      char fname[80];
      FileRecord *pCurRec;
      
      access_mode  = (mode[0] == 'r') ? FILEREADMODE   : FILEWRITEMODE;
      
      /* access mode: b/t */
      if (mode[1] == '+')
        text_mode = (mode[2] == 'b') ? FILEBINARYMODE : FILETEXTMODE;
      else
        text_mode = (mode[1] == 'b') ? FILEBINARYMODE : FILETEXTMODE;
        
      /* Remove the directory path from the filename. */
      if ((pfn = strrchr(filename, '/')) != NULL || (pfn = strrchr(filename, '\\')) != NULL)
        strcpy(fname, pfn + 1);
      else
        strcpy(fname, filename);
        
        
      /* Locate the start of the file, by looking through the file record table. */
      if (access_mode == FILEREADMODE)
      {
        pCurRec = (FileRecord *)pReadOnlyFileRecTable;
        start = g_pFirstFile;
      }
      else
      {
        pCurRec = (FileRecord *)pWritableFileRecTable;
      }
      
      while (pCurRec->size > 0 && strcmp(pCurRec->name, fname) != 0)
      {
        /* have to count the read-only file address in order to be best portable */
        start += pCurRec->size;
        pCurRec++;
#ifndef NDEBUG
        /* just for our internal test for read-only files.
        if (pCurRec->start != NULL)
        passert(start == pCurRec->start);
        */
#endif
      }
      
      m = access_mode | text_mode;
      /* Do not support reopen the writable file now. */
      if (access_mode == FILEREADMODE)
      {
        if (pCurRec->size == 0)
        {
          return NULL;
        }
        
        /* Found the file, record it's starting offset and length. */
        end = pCurRec->end;
        size = pCurRec->size;
      }
      /* writable file, open it the first time; could be text or binary */
      else if (ISWRITEMODE(access_mode))
      {
        /* set the name and mode */
        strcpy(pCurRec->name, fname);
        pCurRec->mode = m;
        
        start = pCurRec->start;
        passert(start == NULL);
        end = size = FILEBUFFERSIZE;
      }
      else
      {
        /* File doesn't exist. */
        return NULL;
      }
      pfn = pCurRec->name;
    }
    else
    {
      /* Unknown mode */
      return NULL;
    }
    
    /* Open file */
    /* Create new file handle */
    PortFile = (PORT_FILE)MALLOC(sizeof(PORT_FILE_HANDLE), "PortFile");
    if (PortFile == NULL)
    {
      return NULL;
    }
    
    /* this mode is not tested yet */
    if (ISREOPENMODE(m))
    {
      PortFile->startFrame = (FileBufferFrame *)start;
    }
    else
    {
      PortFile->startFrame = CreateFileBufferFrame(size, (unsigned char *)start);
      if (ISWRITEMODE(m))
      {
        start = (const unsigned char *)PortFile->startFrame;
      }
    }
    
    if (PortFile->startFrame == NULL)
    {
      FREE(PortFile);
      return NULL;
    }
    
    PortFile->endFrame = PortFile->curFrame = PortFile->startFrame;
    
    /* Mark that this file handle is for flash memory */
    PortFile->filename = pfn;
    PortFile->curPos = PortFile->curFrame->buffer;
    PortFile->endPos = PortFile->curPos + end;
    
    /* set the PortFile->endPos */
    curFrame = PortFile->curFrame;
    while (end > 0)
    {
      if (end > curFrame->size)
      {
        curFrame = curFrame->next;
        passert(end > curFrame->size);
        end -= curFrame->size;
        passert(curFrame);
      }
      else
      {
        /* only reopen the writable file comes here */
        PortFile->endPos = curFrame->buffer + end;
        break;
      }
    }
    
    PortFile->eof = 0; /* File must have at least one byte in it. */
    PortFile->size =  size;
    PortFile->mode =  m;
    
    return PortFile;
  }
  
  int PortFclose(PORT_FILE PortFile)
  {
    passert(PortFile);
    
    /* for reopen mode, do not delete the FileBufferFrame. Delete it by who created it */
    if (ISWRITEMODE(PortFile->mode) && !ISREOPENMODE(PortFile->mode))  /* writable file */
    {
      int i = 0;
      FileRecord *pCurRec = (FileRecord *)pWritableFileRecTable;
      
      /* find the file record in memory */
      for (i = 0; i < nWritableFiles; i++)
      {
        if (PortFile->size > 0 &&
            PortFile->filename[0] != '\0' &&
            strcmp(pCurRec->name, PortFile->filename) == 0
           )
        {
          /* The parameter SREC.Recognizer.osi_log_level in par file control the output
            # BIT 0 -> BASIC logging
            # BIT 1 -> AUDIO waveform logging
            # BIT 2 -> ADD WORD logging
            # e.g. value is 3 = BASIC+AUDIO logging, no ADDWORD
            SREC.Recognizer.osi_log_level = 7;
          
            Do not control here       
          */
          /*
          SaveFileToDisk(PortFile);
          */
          
          pCurRec->name[0] = '\0';
          pCurRec->start = NULL;
          pCurRec->end = 0;
          pCurRec->size = 0;
          
          break;
        }
        pCurRec++;
      }
    }
    
    DeleteFileBuffers(PortFile->startFrame);
    FREE(PortFile);
    return 0;
  }
  
  /*
   * Returns the number of items read
   */
  size_t PortFread(void *buffer, size_t size, size_t count, PORT_FILE PortFile)
  {
    unsigned char *bufferPtr = (unsigned char *)buffer;
    int cbRemain = size * count;
    int cbAvail, minSize;
    FileBufferFrame *curFrame = PortFile->curFrame;
    
    passert(buffer);
    passert(PortFile);
    
    if (PortFile->eof == 1)
    {
      return 0;
    }
    
    while (cbRemain > 0)
    {
      if (PortFile->endPos == PortFile->curPos)  /* end of file */
        break;
        
      if (PortFile->curPos == curFrame->buffer + curFrame->size) /* end of this frame */
      {
        /* go to next frame */
        curFrame = PortFile->curFrame = curFrame->next;
        PortFile->curPos = curFrame->buffer;
      }
      
      if (curFrame == PortFile->endFrame)  /* last frame */
        cbAvail = PortFile->endPos - PortFile->curPos;
      else
        cbAvail = curFrame->size - (PortFile->curPos - curFrame->buffer);
        
      minSize = cbRemain < cbAvail ? cbRemain : cbAvail;
      passert(minSize >= 0);
      
      cbRemain -= minSize;
      while (minSize-- > 0)
        *bufferPtr++ = *PortFile->curPos++;
    }
    
    if (PortFile->curPos == PortFile->endPos)
    {
      PortFile->eof = 1;
    }
    /*
    #ifdef __BIG_ENDIAN
     if (!bNativeEnding)
     {
      swap_byte_order((char *)buffer, count, size);
     }
    #endif
    */
    return count - cbRemain / size;
  }
  
  /*
   * Returns the number of items written
   */
  size_t PortFwrite(const void *data, size_t size, size_t count, PORT_FILE PortFile)
  {
    int cbWrite = size * count;
    int cbAvail, minSize;
    unsigned char *buffer = (unsigned char *)data;
    FileBufferFrame *curFrame;
    
    if (PortFile == NULL)
      return 0;
      
    curFrame = PortFile->curFrame;
    
    /* write data until the end of the internal buffer */
    if (PortFile->eof == 1)
    {
      /* TODO: should return 0, but it will cause infinite loop */
      return 0;
    }
    
    /* why sub 1 ? */
    while (cbWrite > 0)
    {
      if (PortFile->curPos == curFrame->buffer + curFrame->size) /* end of this frame */
      {
        if (curFrame->next == NULL)
        {
          /* assign a new space */
          FileBufferFrame *nextFrame = CreateFileBufferFrame(FILEBUFFERSIZE, NULL);
          if (nextFrame)
          {
            curFrame->next = nextFrame;
            nextFrame->prev = curFrame;
            nextFrame->index = curFrame->index + 1;
            
            curFrame = PortFile->curFrame = nextFrame;
            PortFile->endFrame = nextFrame;
            PortFile->curPos = PortFile->endPos = nextFrame->buffer;
            
            PortFile->size += FILEBUFFERSIZE;
          }
          else
          {
            return count -cbWrite / size;
          }
        }
        else
          curFrame = curFrame->next;
      }
      
      /* available space in current frame */
      cbAvail = curFrame->size - (PortFile->curPos - curFrame->buffer);
      minSize = cbWrite < cbAvail ? cbWrite : cbAvail;
      
      memcpy((char *)PortFile->curPos, buffer, minSize);
      buffer += minSize;
      PortFile->curPos += minSize;
      /* in case the write is not adding to the end */
      if (curFrame == PortFile->endFrame && PortFile->endPos < PortFile->curPos)
        PortFile->endPos = PortFile->curPos;
      cbWrite -= minSize;
    }
    
    return count;
  }
  
  /*
   * Returns 0 on success, non-zero on failure
   */
  int PortFseek(PORT_FILE PortFile, long offset, int origin)
  {
    int retval = 0;
    int cbAvail, minSize;
    FileBufferFrame *curFrame;
    
    passert(PortFile);
    
    /* Clear eof flag */
    PortFile->eof = 0;
    
    switch (origin)
    {
      case SEEK_CUR:
        break;
      case SEEK_END:
        PortFile->curFrame = PortFile->endFrame;
        PortFile->curPos = PortFile->endPos;
        break;
      case SEEK_SET:
        PortFile->curFrame = PortFile->startFrame;
        PortFile->curPos = PortFile->startFrame->buffer;
        break;
      default:
        retval = 0; /* Error, unknown origin type */
        break;
    }
    
    curFrame = PortFile->curFrame;
    
    while (offset != 0)
    {
      if (offset > 0)
      {
        if (PortFile->endPos <= PortFile->curPos)  /* end of file */
          break;
          
        if (PortFile->curPos == curFrame->buffer + curFrame->size) /* end of this frame */
        {
          /* go to next frame */
          curFrame = curFrame->next;
          if (curFrame == NULL)
            break;
          PortFile->curFrame = curFrame->next;
          PortFile->curPos = curFrame->buffer;
        }
        if (curFrame == PortFile->endFrame)  /* last frame */
          cbAvail = PortFile->endPos - PortFile->curPos;
        else
          cbAvail = curFrame->size - (PortFile->curPos - curFrame->buffer);
          
        minSize = offset < cbAvail ? offset : cbAvail;
        
        PortFile->curPos += minSize;
        offset -= minSize;
      }
      else
      {
        if (PortFile->startFrame->buffer == PortFile->curPos)  /* start of file */
          break;
          
        if (PortFile->curPos <= curFrame->buffer) /* start of this frame */
        {
          /* go to next frame */
          curFrame = curFrame->next;
          if (curFrame == NULL)
            break;
          PortFile->curFrame = curFrame;
          PortFile->curPos = curFrame->buffer + curFrame->size;
        }
        cbAvail = PortFile->curPos - curFrame->buffer;
        
        minSize = -offset < cbAvail ? -offset : cbAvail;
        
        PortFile->curPos -= minSize;
        offset += minSize;
      }
    }
    return retval;
  }
  
  /*
   * Returns current file position
   */
  long PortFtell(PORT_FILE PortFile)
  {
    int size;
    FileBufferFrame *curFrame = PortFile->curFrame;
    
    passert(PortFile);
    
    /* current Frame size */
    size = PortFile->curPos - curFrame->buffer;
    
    /* previous frame size */
    while (curFrame = curFrame->prev)
      size += curFrame->size;
      
    return size;
  }
  
  int PortVfprintf(PORT_FILE PortFile, const char *format, va_list argptr)
  {
    char message[2*2048] = "";
    
    /* Print formatted message to buffer */
    vsprintf(message, format, argptr);
    
    if (PortFile == NULL)
    {
      /* TODO: HECK to screen */
#ifndef NDEBUG
      printf(message);
#endif
      return 0;
    }
    
    passert(strlen(message) < 2*2048);
    /* TO DO, seems at case fprintf(pf, "whatever"), message is empty! */
    if (strlen(message) == 0)
      return 0;
    else
      return PortFwrite(message, sizeof(char), strlen(message), PortFile);
  }
  
  /*
   * Returns current file position
   */
  int PortFprintf(PORT_FILE PortFile, const char* format, ...)
  {
    va_list log_va_list;
    int ret = 0;
    
    /* Start variable argument list */
    va_start(log_va_list, format);
    
    /* Print formatted message to buffer */
    ret = PortVfprintf(PortFile, format, log_va_list);
    
    /* End variable argument list */
    va_end(log_va_list);
    
    return ret;
  }
  
  /*
   * Returns string or NULL if error
   */
  char *PortFgets(char *string, int n, PORT_FILE PortFile)
  {
    int cbToRead = n - 1;
    BOOL done = FALSE;
    char *retString = NULL;
    int i;
    
    passert(string);
    passert(n);
    passert(PortFile);
    
    
    if (PortFile->eof == 1)
    {
      return NULL;
    }
    
    
    /* Search for \n only! */
    for (i = 0; i < cbToRead && !done; i++)
    {
      if (PortFile->curPos >= PortFile->endPos)
      {
        PortFile->eof = 1;
        done = TRUE;
        break;
      }
      else if (*PortFile->curPos == '\n')
      {
        if (retString == NULL)
        {
          retString = string;
        }
        retString[i] = '\n';
        PortFile->curPos++;
        done = TRUE;
      }
      else
      {
        if (retString == NULL)
        {
          retString = string;
        }
        retString[i] = *PortFile->curPos++;
      }
    }
    if (retString != NULL)
    {
      retString[i] = '\0';
    }
    return retString;
  }
  
  /*
   * Returns string or NULL if error
   */
  int PortFflush(PORT_FILE PortFile)
  {
    if (PortFile == NULL)
    {
      return -1;
    }
    
    
    /* call fflush before reopen a writable file */
    if (ISWRITEMODE(PortFile->mode))  /* writable file */
    {
      FileRecord *pCurRec = (FileRecord *)pWritableFileRecTable;
      
      /* find the file record in memory */
      do
      {
        if (strcmp(pCurRec->name, PortFile->filename) == 0)
        {
          /* assgin it as startFrame, so others could get information when reopen it */
          pCurRec->start = (unsigned char *)PortFile->startFrame;
          pCurRec->end = PortFile->size - PortFile->endFrame->size +
                         (PortFile->endPos - PortFile->endFrame->buffer);
          pCurRec->size = PortFile->size;
          pCurRec->mode = PortFile->mode;
          
          break;
        }
        pCurRec++;
      }
      while (pCurRec->size > 0);
    }
    return 0;
  }
  
  
  int PortFeof(PORT_FILE PortFile)
  {
    passert(PortFile);
    
    return PortFile->eof;
  }
  
  /*
   * Returns character or EOF
   */
  int PortFgetc(PORT_FILE PortFile)
  {
    int c;
    
    passert(PortFile);
    
    if (PortFile->eof == 1)
    {
      return EOF;
    }
    else
    {
      c = (int) * PortFile->curPos++;
      
      if (PortFile->curPos >= PortFile->endPos)
      {
        PortFile->eof = 1;
      }
    }
    return c;
  }
  
  /*
   * Returns 0 if no error
   */
  int PortFerror(PORT_FILE PortFile)
  {
    passert(PortFile);
    
    return 0;
  }
  
  void PortClearerr(PORT_FILE PortFile)
  {
    PortFile->eof = 0;
  }
  
  /*
   * Returns current file position
   */
  int PortFscanf(PORT_FILE PortFile, const char* format, ...)
  {
    passert(PortFile);
    
    (void)format;
    
    /* Not supported. */
    passert(FALSE);
    return 0;
  }
  
  void PortRewind(PORT_FILE PortFile)
  {
    passert(PortFile);
    
    PortFile->curFrame = PortFile->startFrame;
    PortFile->curPos = PortFile->startFrame->buffer;
    
    PortFile->eof = 0;
  }
  
  /*
   * NULL if it fails, otherwise a valid file pointer
   */
  PORT_FILE PortFreopen(const char *path, const char *mode, PORT_FILE PortFile)
  {
    /* does not support reopen writable file */
    if (PortFclose(PortFile) == 0)
    {
      PortFile = PortFopen(path, mode);
      return PortFile;
    }
    return NULL;
  }
  
  char* PortGetcwd(char *buffer, int maxlen)
  {
    if (maxlen >= 1)
      buffer[0] = '\0';
    else
      return NULL;
      
    return buffer;
  }
  
  int PortMkdir(const char *dirname)
  {
    return 0;
  }
  
#ifdef XANAVI_PROJECT
  
  int PortSaveFileToDisk(PORT_FILE PortFile, const char *path, const char *fname)
  {
    /* ### start mod */
    
    FILE *fp = NULL; /* real file handle */
    char fullfname[256], data[256];
    char mode[3];
    const char *file;
    int size;
    
    if (fname == NULL)
      file = PortFile->filename;
    else
      file = fname;
      
    if (path == NULL)
    {
      PLogMessage("trying to save file %s...\n", file);
      sprintf(fullfname, "%s", file);
    }
    else
    {
      PLogMessage("trying to save file %s to %s...\n", file, path);
      sprintf(fullfname, "%s/%s", path, file);
    }
    
    if (ISBINARYMODE(PortFile->mode))  /* binary file, the wav file */
    {
      sprintf(mode, "wb");
    }
    else
    {
      sprintf(mode, "w");
    }
    
    if ((fp = fopen(fullfname, mode)) != NULL)
    {
      PortRewind(PortFile);
      
      while ((size = PortFread(data, 1, 256, PortFile)) > 0)
      {
        fwrite(data, 1, size, fp);
      }
      fclose(fp);
    }
    else
    {
      PLogError(L("Error to fopen %s with mode %s\n\n"), fullfname, mode);
      return -1;
    }
    return 0;
  }
  
  int PortLoadFileFromDisk(PORT_FILE PortFile, const char *filename, const char *mode)
  {
    FILE *fp;
    int size;
    char data[256];
    
    passert(PortFile);
    
    if (filename == NULL)
      filename = PortFile->filename;
      
    if (mode == NULL)
    {
      data[0] = 'r';
      if (ISBINARYMODE(PortFile->mode))
        data[1] = 'b';
      else
        data[1] = '\0';
      data[2] = '\0';
      mode = data;
    }
    
    fp = fopen(filename, mode);
    
    if (fp == NULL)   /* do not have the file, it is fine */
      return 0;
      
    while ((size = fread(data, 1, 256, fp)) > 0)
      PortFwrite(data, 1, size, PortFile);
      
    fclose(fp);
    /* go to the beginning of the file */
    PortFseek(PortFile, 0, SEEK_SET);
    
    return 0;
  }
  
  int XanaviSaveFileToDisk(PORT_FILE PortFile)
  {
    const char *tail;
    int lenny;
    
    passert(PortFile);
    
    /* UG has to be 8.3 format! */
    lenny = strlen(PortFile->filename);
    if (lenny > 10)
      tail = PortFile->filename + (lenny - 11);
    else
      tail = PortFile->filename;
    /* printf( "8.3 filename is %s.\n", tail ); */
    
    /* the 8.3 format has truncated the path in PortFile->filename,
       should get the direcotry from par file
       cmdline.DataCaptureDirectory                  = /CFC
       TODO: here use /CFC directly to save time
    */
    return PortSaveFileToDisk(PortFile, "/CFC", tail);
  }
  
  int XanaviLoadFileFromDisk(PORT_FILE PortFile)
  {
    char fname[256];
    char mode[3];
    
    passert(PortFile);
    
    sprintf(fname, "/CFC/%s", PortFile->filename);
    
    mode[0] = 'r';
    if (ISBINARYMODE(PortFile->mode))
      mode[1] = 'b';
    else
      mode[1] = '\0';
      
    mode[2] = '\0';
    
    return PortLoadFileFromDisk(PortFile, fname, mode);
  }
  
#endif
#endif /* STATIC_FILE_SYSTME */
  
#ifdef __cplusplus
}
#endif

