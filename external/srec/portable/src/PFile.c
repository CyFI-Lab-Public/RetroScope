/*---------------------------------------------------------------------------*
 *  PFile.c  *
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

#include "LCHAR.h"
#include "pendian.h"
#include "PFile.h"
#include "PFileSystem.h"
#include "plog.h"
#include "pstdio.h"


ESR_ReturnCode PFileDestroy(PFile* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->destroy(self);
}

ESR_ReturnCode PFileOpen(PFile* self, const LCHAR* mode)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->open(self, mode);
}

ESR_ReturnCode PFileClose(PFile* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->close(self);
}

ESR_ReturnCode PFileRead(PFile* self, void* buffer, size_t size, size_t* count)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->read(self, buffer, size, count);
}

ESR_ReturnCode PFileWrite(PFile* self, void* buffer, size_t size, size_t* count)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->write(self, buffer, size, count);
}

ESR_ReturnCode PFileFlush(PFile* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->flush(self);
}

ESR_ReturnCode PFileSeek(PFile* self, long offset, int origin)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->seek(self, offset, origin);
}


ESR_ReturnCode PFileGetPosition(PFile* self, size_t* position)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getPosition(self, position);
}

ESR_ReturnCode PFileIsOpen(PFile* self, ESR_BOOL* isOpen)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->isOpen(self, isOpen);
}

ESR_ReturnCode PFileIsEOF(PFile* self, ESR_BOOL* isEof)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->isEOF(self, isEof);
}

ESR_ReturnCode PFileGetFilename(PFile* self, LCHAR* filename, size_t* len)
{
  ESR_ReturnCode rc;
  
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  rc = self->getFilename(self, filename, len);
  return rc;
}

ESR_ReturnCode PFileIsErrorSet(PFile* self, ESR_BOOL* isError)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->isErrorSet(self, isError);
}

ESR_ReturnCode PFileClearError(PFile* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->clearError(self);
}

ESR_ReturnCode PFileVfprintf(PFile* self, int* result, const LCHAR* format, va_list args)
{
  ESR_ReturnCode rc;
  
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  rc = self->vfprintf(self, result, format, args);
  return rc;
}

ESR_ReturnCode PFileFgetc(PFile* self, LINT* result)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->fgetc(self, result);
}

ESR_ReturnCode PFileFgets(PFile* self, LCHAR* string, int n, LCHAR** result)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->fgets(self, string, n, result);
}

ESR_ReturnCode PFileReadInt(PFile* self, int* value)
{
  LCHAR number[MAX_INT_DIGITS+1];
  size_t i, bufferSize, count, totalRead = 0;
  ESR_ReturnCode rc;
  
  /* Skip whitespace before token */
  do
  {
    count = pfread(number, sizeof(LCHAR), MAX_INT_DIGITS, self);
    totalRead += count;
    if (count < MAX_INT_DIGITS)
    {
      if (pferror(self))
      {
        rc = ESR_READ_ERROR;
        PLogError(ESR_rc2str(rc));
        goto CLEANUP;
      }
      else
      {
        rc = ESR_INVALID_STATE;
        PLogError(L("%s: reached end of file before finding token"), ESR_rc2str(rc));
        goto CLEANUP;
      }
    }
    /* locate first non-whitespace character */
    for (i = 0; i < count && LISSPACE(number[i]); ++i);
  }
  while (i == count);
  bufferSize = count - i;
  
  /* Fill remainder of buffer */
  if (bufferSize < MAX_INT_DIGITS)
  {
    count = pfread(number + bufferSize, sizeof(LCHAR), MAX_INT_DIGITS - bufferSize, self);
    bufferSize += count;
    totalRead += count;
    if (count < MAX_INT_DIGITS - bufferSize && pferror(self))
    {
      rc = ESR_READ_ERROR;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
  }
  
  /* locate first whitespace character */
  for (i = 0; i < bufferSize && !LISSPACE(number[i]); ++i);
  if (i < bufferSize)
  {
    /* unread anything after the token */
    if (PFileSeek(self, - (int)(bufferSize - i), SEEK_CUR))
    {
      rc = ESR_SEEK_ERROR;
      PLogError(ESR_rc2str(rc));
    }
    totalRead -= bufferSize - i;
    number[i] = L('\0');
  }
  
  if (number[0] != L('-') && !LISDIGIT(number[0]))
  {
    rc = ESR_INVALID_STATE;
    PLogError(L("%s: token was not number (%s)"), ESR_rc2str(rc), number);
    goto CLEANUP;
  }
  
  CHKLOG(rc, lstrtoi(number, value, 10));
  return rc;
CLEANUP:
  if (PFileSeek(self,  - (int) count, SEEK_CUR))
    PLogError(L("ESR_SEEK_ERROR"));
  return rc;
}

ESR_ReturnCode PFileReadLCHAR(PFile* self, LCHAR* value, size_t len)
{
  size_t i, bufferSize, count, totalRead = 0;
  ESR_ReturnCode rc = ESR_SUCCESS;
  
  /* Skip whitespace before token */
  do
  {
    count = pfread(value, sizeof(LCHAR), len, self);
    totalRead += count;
    if (count < len)
    {
      if (pferror(self))
      {
        rc = ESR_READ_ERROR;
        PLogError(ESR_rc2str(rc));
        goto CLEANUP;
      }
      else
      {
        rc = ESR_INVALID_STATE;
        PLogError(L("%s: reached end of file before finding token"), ESR_rc2str(rc));
        goto CLEANUP;
      }
    }
    /* locate first non-whitespace character */
    for (i = 0; i < count && LISSPACE(value[i]); ++i);
  }
  while (i == count);
  bufferSize = count - i;
  
  /* Fill remainder of buffer */
  if (bufferSize < len)
  {
    count = pfread(value + bufferSize, sizeof(LCHAR), len - bufferSize, self);
    bufferSize += count;
    totalRead += count;
    if (count < len - bufferSize && pferror(self))
    {
      rc = ESR_READ_ERROR;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
  }
  
  /* locate first whitespace character */
  for (i = 0; i < bufferSize && !LISSPACE(value[i]); ++i);
  if (i < bufferSize)
  {
    /* unread anything after the token */
    if (PFileSeek(self, -(int)(bufferSize - i), SEEK_CUR))
    {
      rc = ESR_SEEK_ERROR;
      PLogError(ESR_rc2str(rc));
    }
    totalRead -= bufferSize - i;
    value[i] = L('\0');
  }
  return rc;
CLEANUP:
  if (PFileSeek(self, - (int) count, SEEK_CUR))
    PLogError(L("ESR_SEEK_ERROR"));
  return rc;
}

PFile* pfopen(const LCHAR* filename, const LCHAR* mode)
{
  PFile* result;
  ESR_ReturnCode rc;
  ESR_BOOL isLittleEndian;
  
#if __BYTE_ORDER==__LITTLE_ENDIAN
  isLittleEndian = ESR_TRUE;
#else
  isLittleEndian = ESR_FALSE;
#endif
  
  rc = PFileSystemCreatePFile(filename, isLittleEndian, &result);
  if (rc != ESR_SUCCESS)
    return NULL;
  rc = result->open(result, mode);
  if (rc != ESR_SUCCESS)
  {
    result->destroy(result);
    return NULL;
  }
  return result;
}

size_t pfread(void* buffer, size_t size, size_t count, PFile* stream)
{
  ESR_ReturnCode rc;
  
  rc = PFileRead(stream, buffer, size, &count);
  if (rc != ESR_SUCCESS)
    return 0;
  return count;
}

size_t pfwrite(const void* buffer, size_t size, size_t count, PFile* stream)
{
  ESR_ReturnCode rc;
  
  rc = PFileWrite(stream, buffer, size, &count);
  if (rc != ESR_SUCCESS)
    return 0;
  return count;
}

int pfclose(PFile* stream)
{
  ESR_ReturnCode rc;
  
  rc = PFileDestroy(stream);
  if (rc != ESR_SUCCESS)
    return PEOF;
  return 0;
}

void prewind(PFile* stream)
{
  PFileSeek(stream, 0, SEEK_SET);
}

int pfseek(PFile* stream, long offset, int origin)
{
  ESR_ReturnCode rc;
  
  rc = PFileSeek(stream, offset, origin);
  if (rc != ESR_SUCCESS)
    return 1;
  return 0;
}

long pftell(PFile* stream)
{
  size_t result;
  ESR_ReturnCode rc;
  
  rc = PFileGetPosition(stream, &result);
  if (rc != ESR_SUCCESS)
    return -1;
  return result;
}

int pfeof(PFile* stream)
{
  ESR_BOOL eof;
  
  PFileIsEOF(stream, &eof);
  if (!eof)
    return 0;
  return 1;
}

int pferror(PFile* stream)
{
  ESR_BOOL error;
  
  PFileIsErrorSet(stream, &error);
  if (!error)
    return 0;
  return 1;
}

void pclearerr(PFile* stream)
{
  PFileClearError(stream);
}

int pfflush(PFile* stream)
{
  ESR_ReturnCode rc;
  
  rc = PFileFlush(stream);
  if (rc != ESR_SUCCESS)
    return PEOF;
  return 0;
}

LCHAR* pfgets(LCHAR* string, int n, PFile* self)
{
  LCHAR* result;
  ESR_ReturnCode rc;
  
  rc = PFileFgets(self, string, n, &result);
  if (rc != ESR_SUCCESS)
    return NULL;
  return result;
}

LINT pfgetc(PFile* self)
{
  LINT result;
  ESR_ReturnCode rc;
  
  rc = PFileFgetc(self, &result);
  if (rc != ESR_SUCCESS)
    return PEOF;
  return result;
}

int pfprintf(PFile* stream, const LCHAR* format, ...)
{
#ifdef FINAL_RELEASE
  return 0;
#else
  va_list args;
  int result;
  ESR_ReturnCode rc;
  
  va_start(args, format);
  rc = PFileVfprintf(stream, &result, format, args);
  va_end(args);
  if (rc != ESR_SUCCESS)
    return -1;
  return result;
#endif
}

int pvfprintf(PFile* stream, const LCHAR* format, va_list argptr)
{
#ifdef FINAL_RELEASE
  return 0;
#else
  int result;
  ESR_ReturnCode rc;
  
  rc = PFileVfprintf(stream, &result, format, argptr);
  if (rc != ESR_SUCCESS)
    return -1;
  return result;
#endif
}

int pprintf(const LCHAR* format, ...)
{
#ifdef FINAL_RELEASE
  return 0;
#else
  va_list args;
  int result;
  ESR_ReturnCode rc;
  
  va_start(args, format);
  rc = PFileVfprintf(PSTDOUT, &result, format, args);
  va_end(args);
  if (rc != ESR_SUCCESS)
    return -1;
  return result;
#endif
}
