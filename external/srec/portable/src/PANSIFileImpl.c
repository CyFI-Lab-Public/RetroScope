/*---------------------------------------------------------------------------*
 *  PANSIFileImpl.c  *
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

#include "errno.h"
#include "passert.h"
#include "pendian.h"
#include "PFileImpl.h"
#include "PANSIFileImpl.h"
#include "PFileSystem.h"
#include "ESR_ReturnCode.h"
#include "plog.h"
#include "pmemory.h"
#include "pstdio.h"
#include "ptypes.h"

#define MTAG NULL

ESR_ReturnCode PANSIFileCreateImpl(const LCHAR* filename, ESR_BOOL isLittleEndian, PFile** self)
{
  PANSIFileImpl* impl = NULL;
  ESR_ReturnCode rc;
  
  impl = NEW(PANSIFileImpl, MTAG);
  if (impl == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  
  PFileCreateImpl(&impl->Interface.Interface, filename, isLittleEndian);
  impl->Interface.Interface.close = &PANSIFileCloseImpl;
  impl->Interface.Interface.clearError = &PANSIFileClearErrorImpl;
  impl->Interface.Interface.destroy = &PANSIFileDestroyImpl;
  impl->Interface.Interface.fgetc = &PANSIFileFgetcImpl;
  impl->Interface.Interface.fgets = &PANSIFileFgetsImpl;
  impl->Interface.Interface.getPosition = &PANSIFileGetPositionImpl;
  impl->Interface.Interface.hideMemoryAllocation = &PANSIFileHideMemoryAllocation;
  impl->Interface.Interface.isEOF = &PANSIFileIsEOFImpl;
  impl->Interface.Interface.isErrorSet = &PANSIFileIsErrorSetImpl;
  impl->Interface.Interface.isOpen = &PANSIFileIsOpenImpl;
  impl->Interface.Interface.open = &PANSIFileOpenImpl;
  impl->Interface.Interface.read = &PANSIFileReadImpl;
  impl->Interface.Interface.seek = &PANSIFileSeekImpl;
  impl->Interface.Interface.flush = &PANSIFileFlushImpl;
  impl->Interface.Interface.write = &PANSIFileWriteImpl;
  
  impl->Interface.filename[0] = 0;
  impl->value = NULL;
  
  LSTRCAT(impl->Interface.filename, filename);
  *self = &impl->Interface.Interface;
  return ESR_SUCCESS;
CLEANUP:
  if (impl != NULL)
    impl->Interface.Interface.destroy(&impl->Interface.Interface);
  return rc;
}

ESR_ReturnCode PANSIFileDestroyImpl(PFile* self)
{
  ESR_ReturnCode rc;
  
  CHK(rc, PFileDestroyImpl(self));
  FREE(self);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}


#ifdef USE_THREAD
#define LOCK_MUTEX(rc, impl) \
  if (impl->Interface.lock != NULL) \
    CHKLOG(rc, PtrdMonitorLock(impl->Interface.lock));
#else
#define LOCK_MUTEX(rc, impl)
#endif


#ifdef USE_THREAD
#define CLEANUP_AND_RETURN(rc, impl) \
  if (impl->Interface.lock!=NULL) \
    CHKLOG(rc, PtrdMonitorUnlock(impl->Interface.lock)); \
  return ESR_SUCCESS; \
  CLEANUP: \
  if (impl->Interface.lock!=NULL) \
    PtrdMonitorUnlock(impl->Interface.lock); \
  return rc;
#else
#define CLEANUP_AND_RETURN(rc, impl) \
  return ESR_SUCCESS; \
  CLEANUP: \
  return rc;
#endif


ESR_ReturnCode PANSIFileOpenImpl(PFile* self, const LCHAR* mode)
{
  PANSIFileImpl* impl = (PANSIFileImpl*) self;
  ESR_ReturnCode rc;
  
  LOCK_MUTEX(rc, impl);
  if (impl->value != NULL)
  {
    rc = ESR_ALREADY_OPEN;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  impl->value = fopen(impl->Interface.filename, mode);
  
  if (impl->value == NULL)
  {
    LCHAR path[P_PATH_MAX];
    size_t len;
    
    len = P_PATH_MAX;
    CHKLOG(rc, PFileSystemGetcwd(path, &len));
    rc = ESR_OPEN_ERROR;
    /* PLOG_DBG_TRACE((L("%s: filename=%s, cwd=%s"), ESR_rc2str(rc), impl->Interface.filename, path)); */
    PLogError(L("%s: filename=%s, cwd=%s"), ESR_rc2str(rc), impl->Interface.filename, path);
    goto CLEANUP;
  }
  CLEANUP_AND_RETURN(rc, impl);
}

ESR_ReturnCode PANSIFileCloseImpl(PFile* self)
{
  PANSIFileImpl* impl = (PANSIFileImpl*) self;
  ESR_ReturnCode rc;
  
  LOCK_MUTEX(rc, impl);
  if (fclose(impl->value) != 0)
  {
    rc = ESR_CLOSE_ERROR;
    PLogMessage(L("%s: file %s, handle"), ESR_rc2str(rc), impl->Interface.filename, impl->value);
    goto CLEANUP;
  }
  impl->value = NULL;
  CLEANUP_AND_RETURN(rc, impl);
}

ESR_ReturnCode PANSIFileReadImpl(PFile* self, void* buffer, size_t size, size_t* count)
{
  PANSIFileImpl* impl = (PANSIFileImpl*) self;
  ESR_ReturnCode rc;
  
  LOCK_MUTEX(rc, impl);
  if (count == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  
  if (size != 0 && *count != 0)
  {
    ESR_BOOL needToSwap;
    
    *count = fread(buffer, size, *count, impl->value);
    if (*count == 0 && ferror(impl->value))
    {
      rc = ESR_READ_ERROR;
      PLogMessage(ESR_rc2str(rc));
      goto CLEANUP;
    }
    
#ifdef __LITTLE_ENDIAN
    needToSwap = !impl->Interface.littleEndian;
#else
    needToSwap = impl->Interface.littleEndian;
#endif
    
    if (needToSwap)
      swap_byte_order(buffer, *count, size);
  }
  else
    *count = 0;
  CLEANUP_AND_RETURN(rc, impl);
}

ESR_ReturnCode PANSIFileWriteImpl(PFile* self, void* buffer, size_t size, size_t* count)
{
  PANSIFileImpl* impl = (PANSIFileImpl*) self;
  ESR_ReturnCode rc;
  size_t requested = *count;
  
  LOCK_MUTEX(rc, impl);
  if (count == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  if (size != 0 && *count != 0)
  {
    ESR_BOOL needToSwap;
    void* temp;
    
#ifdef __LITTLE_ENDIAN
    needToSwap = !impl->Interface.littleEndian;
#else
    needToSwap = impl->Interface.littleEndian;
#endif
    if (needToSwap)
    {
      temp = MALLOC(*count * size, MTAG);
      if (temp == NULL)
      {
        rc = ESR_OUT_OF_MEMORY;
        PLogError(ESR_rc2str(rc));
        goto CLEANUP;
      }
      memcpy(temp, buffer, *count * size);
      
      swap_byte_order(temp, *count, size);
    }
    else
      temp = buffer;
      
    *count = fwrite(temp, size, *count, impl->value);
    if (needToSwap)
    {
      FREE(temp);
      temp = NULL;
    }
    
    if (*count < requested)
    {
      rc = ESR_WRITE_ERROR;
      PLogMessage(ESR_rc2str(rc));
      goto CLEANUP;
    }
  }
  else
    *count = 0;
  CLEANUP_AND_RETURN(rc, impl);
}

ESR_ReturnCode PANSIFileFlushImpl(PFile* self)
{
  PANSIFileImpl* impl = (PANSIFileImpl*) self;
  ESR_ReturnCode rc;
  
  LOCK_MUTEX(rc, impl);
  if (fflush(impl->value) != 0)
  {
    rc = ESR_FLUSH_ERROR;
    PLogMessage(ESR_rc2str(rc));
    goto CLEANUP;
  }
  CLEANUP_AND_RETURN(rc, impl);
}

ESR_ReturnCode PANSIFileSeekImpl(PFile* self, long offset, int origin)
{
  PANSIFileImpl* impl = (PANSIFileImpl*) self;
  ESR_ReturnCode rc;
  
  LOCK_MUTEX(rc, impl);
  if (fseek(impl->value, offset, origin) != 0)
  {
    rc = ESR_SEEK_ERROR;
    PLogMessage(ESR_rc2str(rc));
    goto CLEANUP;
  }
  CLEANUP_AND_RETURN(rc, impl);
}

ESR_ReturnCode PANSIFileGetPositionImpl(PFile* self, size_t* position)
{
  PANSIFileImpl* impl = (PANSIFileImpl*) self;
  ESR_ReturnCode rc;
  long pos;
  
  LOCK_MUTEX(rc, impl);
  pos = ftell(impl->value);
  if (pos == -1)
  {
    switch (errno)
    {
      case EBADF:
        rc = ESR_INVALID_STATE;
        PLogError(L("%s: Got EBADF"), rc);
        goto CLEANUP;
      case EINVAL:
        rc = ESR_INVALID_STATE;
        PLogError(L("%s: Got EINVAL"), rc);
        goto CLEANUP;
      default:
        rc = ESR_INVALID_STATE;
        PLogError(ESR_rc2str(rc));
        goto CLEANUP;
    }
  }
  *position = pos;
  CLEANUP_AND_RETURN(rc, impl);
}

ESR_ReturnCode PANSIFileIsOpenImpl(PFile* self, ESR_BOOL* isOpen)
{
  PANSIFileImpl* impl = (PANSIFileImpl*) self;
  ESR_ReturnCode rc;
  
  LOCK_MUTEX(rc, impl);
  if (isOpen == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  *isOpen = impl->value != NULL;
  CLEANUP_AND_RETURN(rc, impl);
}

ESR_ReturnCode PANSIFileIsEOFImpl(PFile* self, ESR_BOOL* isEof)
{
  PANSIFileImpl* impl = (PANSIFileImpl*) self;
  ESR_ReturnCode rc;
  
  LOCK_MUTEX(rc, impl);
  if (isEof == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
#ifdef NO_FEOF
  {
    long posCur;	/* remember current file position */
    long posEnd;	/* end of file position */
		
    posCur = ftell(impl->value);
    fseek(impl->value, 0, SEEK_END);
    posEnd = ftell(impl->value);
    *isEof = (posCur == posEnd);
    fseek(impl->value, posCur, SEEK_SET);  /* restore position in file */
  }
#else	
  *isEof = feof(impl->value) != 0;
#endif
  CLEANUP_AND_RETURN(rc, impl);
}

ESR_ReturnCode PANSIFileIsErrorSetImpl(PFile* self, ESR_BOOL* isError)
{
  PANSIFileImpl* impl = (PANSIFileImpl*) self;
  ESR_ReturnCode rc;
  
  LOCK_MUTEX(rc, impl);
  if (isError == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  *isError = ferror(impl->value) != 0;
  CLEANUP_AND_RETURN(rc, impl);
}

ESR_ReturnCode PANSIFileClearErrorImpl(PFile* self)
{
  PANSIFileImpl* impl = (PANSIFileImpl*) self;
  ESR_ReturnCode rc;
  
  LOCK_MUTEX(rc, impl);
  clearerr(impl->value);
  CLEANUP_AND_RETURN(rc, impl);
}

ESR_ReturnCode PANSIFileFgetsImpl(PFile* self, LCHAR* string, int n, LCHAR** result)
{
  PANSIFileImpl* impl = (PANSIFileImpl*) self;
  ESR_ReturnCode rc;
  LCHAR* temp;
  
  LOCK_MUTEX(rc, impl);
  temp = fgets(string, n, impl->value);
  if (result != NULL)
    *result = temp;
  if (temp == NULL && ferror(impl->value))
  {
    rc = ESR_INVALID_STATE;
    PLogMessage(ESR_rc2str(rc));
    goto CLEANUP;
  }
  CLEANUP_AND_RETURN(rc, impl);
}

ESR_ReturnCode PANSIFileFgetcImpl(PFile* self, LINT* result)
{
  PANSIFileImpl* impl = (PANSIFileImpl*) self;
  ESR_ReturnCode rc;
  
  LOCK_MUTEX(rc, impl);
  *result = fgetc(impl->value);
  if (*result == PEOF && ferror(impl->value))
  {
    rc = ESR_INVALID_STATE;
    PLogMessage(ESR_rc2str(rc));
    goto CLEANUP;
  }
  CLEANUP_AND_RETURN(rc, impl);
}

ESR_ReturnCode PANSIFileHideMemoryAllocation(PFile* self)
{
  PANSIFileImpl* impl = (PANSIFileImpl*) self;
  ESR_ReturnCode rc;
  
  LOCK_MUTEX(rc, impl);
  rc = PMemLogFree(self);
  if (rc != ESR_SUCCESS)
  {
    pfprintf(PSTDERR, L("%s: PMemDumpLogFile() at %s:%d"), ESR_rc2str(rc), __FILE__, __LINE__);
    goto CLEANUP;
  }
  rc = PMemLogFree(impl->Interface.filename);
  if (rc != ESR_SUCCESS)
  {
    pfprintf(PSTDERR, L("%s: PMemDumpLogFile() at %s:%d"), ESR_rc2str(rc), __FILE__, __LINE__);
    goto CLEANUP;
  }
#ifdef USE_THREAD
  rc = PMemLogFree(impl->Interface.lock);
  if (rc != ESR_SUCCESS)
  {
    pfprintf(PSTDERR, L("%s: PMemDumpLogFile() at %s:%d"), ESR_rc2str(rc), __FILE__, __LINE__);
    goto CLEANUP;
  }
#endif
  CLEANUP_AND_RETURN(rc, impl);
}
