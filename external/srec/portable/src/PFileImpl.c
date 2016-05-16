/*---------------------------------------------------------------------------*
 *  PFileImpl.c  *
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

#include "passert.h"
#include "pendian.h"
#include "PFileImpl.h"
#include "PFileSystem.h"
#include "plog.h"
#include "pmemory.h"
#include "pstdio.h"
#include "ptypes.h"

#define MTAG NULL


/**
 * Initializes variables declared in the superinterface.
 */
ESR_ReturnCode PFileCreateImpl(PFile* self, const LCHAR* filename, ESR_BOOL isLittleEndian)
{
  PFileImpl* impl = (PFileImpl*) self;
  ESR_ReturnCode rc;
#ifdef USE_THREAD
  ESR_BOOL threadingEnabled;
#endif
  
#ifdef USE_THREAD
  impl->lock = NULL;
#endif
  impl->littleEndian = isLittleEndian;
  
  impl->Interface.destroy = &PFileDestroyImpl;
  impl->Interface.getFilename = &PFileGetFilenameImpl;
  impl->Interface.vfprintf = &PFileVfprintfImpl;
  impl->filename = MALLOC(sizeof(LCHAR) * (LSTRLEN(filename) + 1), MTAG);
  
  if (impl->filename == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  LSTRCPY(impl->filename, filename);
  
#ifdef USE_THREAD
  rc = PtrdIsEnabled(&threadingEnabled);
  if (rc != ESR_SUCCESS)
  {
    pfprintf(PSTDERR, L("[%s:%d] PtrdIsEnabled failed with %s\n"), __FILE__, __LINE__, ESR_rc2str(rc));
    goto CLEANUP;
  }
  if (threadingEnabled)
  {
    rc = PtrdMonitorCreate(&impl->lock);
    if (rc != ESR_SUCCESS)
      goto CLEANUP;
  }
#endif
  return ESR_SUCCESS;
CLEANUP:
  self->destroy(self);
  return rc;
}


#ifdef USE_THREAD
#define LOCK_MUTEX(rc, impl) \
  if (impl->lock != NULL) \
    CHKLOG(rc, PtrdMonitorLock(impl->lock));
#else
#define LOCK_MUTEX(rc, impl)
#endif


#ifdef USE_THREAD
#define CLEANUP_AND_RETURN(rc, impl) \
  if (impl->lock!=NULL) \
    CHKLOG(rc, PtrdMonitorUnlock(impl->lock)); \
  return ESR_SUCCESS; \
  CLEANUP: \
  if (impl->lock!=NULL) \
    PtrdMonitorUnlock(impl->lock); \
  return rc;
#else
#define CLEANUP_AND_RETURN(rc, impl) \
  return ESR_SUCCESS; \
  CLEANUP: \
  return rc;
#endif


ESR_ReturnCode PFileDestroyImpl(PFile* self)
{
  PFileImpl* impl = (PFileImpl*) self;
  ESR_ReturnCode rc;
  ESR_BOOL isOpen;
  
  LOCK_MUTEX(rc, impl);
  CHKLOG(rc, self->isOpen(self, &isOpen));
  if (isOpen)
    CHKLOG(rc, self->close(self));
  if (impl->filename)
  {
    FREE(impl->filename);
    impl->filename = NULL;
  }
#ifdef USE_THREAD
  if (impl->lock != NULL)
  {
    PtrdMonitorUnlock(impl->lock);
    rc = PtrdMonitorDestroy(impl->lock);
    if (rc != ESR_SUCCESS)
      goto CLEANUP;
  }
#endif
  return ESR_SUCCESS;
CLEANUP:
#ifdef USE_THREAD
  if (impl->lock != NULL)
    PtrdMonitorUnlock(impl->lock);
#endif
  return rc;
}

ESR_ReturnCode PFileGetFilenameImpl(PFile* self, LCHAR* filename, size_t* len)
{
  PFileImpl* impl = (PFileImpl*) self;
  ESR_ReturnCode rc;
  
  if (self == NULL || len == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  LOCK_MUTEX(rc, impl);
  if (LSTRLEN(impl->filename) + 1 > *len)
  {
    *len = LSTRLEN(impl->filename) + 1;
    rc = ESR_BUFFER_OVERFLOW;
    goto CLEANUP;
  }
  LSTRCPY(filename, impl->filename);
  CLEANUP_AND_RETURN(rc, impl);
}

ESR_ReturnCode PFileVfprintfImpl(PFile* self, int* result, const LCHAR* format, va_list args)
{
  ESR_ReturnCode rc;
  ESR_BOOL isOpen;
#define BUFFER_SIZE 5120
  static LCHAR buffer[BUFFER_SIZE];
  size_t len;
  
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  
  CHKLOG(rc, self->isOpen(self, &isOpen));
  if (!isOpen)
  {
    rc = ESR_OPEN_ERROR;
    PLogError(L("%s: cannot operate on closed file"), ESR_rc2str(rc));
    goto CLEANUP;
  }
  
  /*
   * fprintf() is computationally expensive, so we compute its output without grabbing a lock
   * and only lock while actually writing the results into the file.
   */
  if (result != NULL)
    *result = vsprintf(buffer, format, args);
  else
    vsprintf(buffer, format, args);
  len = LSTRLEN(buffer);
  passert(len < BUFFER_SIZE);
  
  CHKLOG(rc, self->write(self, buffer, sizeof(LCHAR), &len));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}
