/*---------------------------------------------------------------------------*
 *  PANSIFileSystemImpl.c  *
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
#include "PFileSystemImpl.h"
#include "PANSIFileSystemImpl.h"
#include "PANSIFileImpl.h"
#include "plog.h"
#include "pmemory.h"

//extern PFileSystem* PANSIFileSystemSingleton;
PFileSystem* PANSIFileSystemSingleton = (PFileSystem*)NULL;

#define MTAG NULL


#ifdef USE_THREAD
/* Prototype of private function */
PORTABLE_API ESR_ReturnCode PtrdFlush();
#endif

/**
 * [file path, PFileSystem*] mapping.
 */
extern PHashTable* PFileSystemPathMap;


ESR_ReturnCode PANSIFileSystemCreate(void)
{
  PANSIFileSystemImpl* impl;
  ESR_ReturnCode rc;
  
  if (PANSIFileSystemSingleton != NULL)
    return ESR_SUCCESS;
  impl = NEW(PANSIFileSystemImpl, MTAG);
  if (impl == NULL)
    return ESR_OUT_OF_MEMORY;
  impl->super.super.destroy = &PANSIFileSystemDestroyImpl;
  impl->super.super.createPFile = &PANSIFileSystemCreatePFileImpl;
  impl->super.addPath = &PANSIFileSystemAddPathImpl;
  impl->super.removePath = &PANSIFileSystemRemovePathImpl;
  impl->super.getcwd = &PANSIFileSystemGetcwdImpl;
  impl->super.super.mkdir = &PANSIFileSystemMkdirImpl;
  impl->super.super.chdir = &PANSIFileSystemChdirImpl;
  
  CHKLOG(rc, PHashTableCreate(NULL, MTAG, &impl->directoryMap));
  PANSIFileSystemSingleton = &impl->super.super;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode PANSIFileSystemDestroyImpl(PFileSystem* self)
{
  PANSIFileSystemImpl* impl = (PANSIFileSystemImpl*) self;
  PHashTableEntry* entry;
  PHashTableEntry* oldEntry;
  LCHAR* key;
  LCHAR* value;
  ESR_ReturnCode rc;
  
  if (impl->directoryMap != NULL)
  {
    CHKLOG(rc, PHashTableEntryGetFirst(impl->directoryMap, &entry));
    while (entry != NULL)
    {
      CHKLOG(rc, PHashTableEntryGetKeyValue(entry, (void **)&key, (void **)&value));
      oldEntry = entry;
      CHKLOG(rc, PHashTableEntryAdvance(&entry));
      CHKLOG(rc, PHashTableEntryRemove(oldEntry));
      CHKLOG(rc, PHashTableRemoveValue(PFileSystemPathMap, key, NULL));
      FREE(key);
      FREE(value);
    }
    CHKLOG(rc, PHashTableDestroy(impl->directoryMap));
    impl->directoryMap = NULL;
  }
  FREE(self);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode PANSIFileSystemAddPathImpl(PFileSystem* self, const LCHAR* virtualPath, const LCHAR* realPath)
{
  PANSIFileSystemImpl* impl = (PANSIFileSystemImpl*) self;
  ESR_BOOL exists;
  LCHAR* key = NULL;
  LCHAR* value = NULL;
  ESR_ReturnCode rc;
  size_t len;
  
  if (virtualPath == NULL || realPath == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  
  len = LSTRLEN(virtualPath) + 1;
  if (virtualPath[LSTRLEN(virtualPath)-1] != L('/'))
    ++len;
  key = MALLOC(sizeof(LCHAR) * len, MTAG);
  if (key == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  LSTRCPY(key, virtualPath);
  /* Make sure paths end with '/' */
  CHKLOG(rc, PFileSystemCanonicalSlashes(key));
  if (key[LSTRLEN(key)-1] != L('/'))
    LSTRCAT(key, L("/"));
  value = MALLOC(sizeof(LCHAR) * (LSTRLEN(realPath) + 1), MTAG);
  if (value == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  LSTRCPY(value, realPath);
  
  /* Make sure realPath is not an empty string */
  lstrtrim(value);
  if (LSTRLEN(value) == 0)
  {
    FREE(value);
    value = NULL;
    rc = ESR_INVALID_ARGUMENT;
    PLogError(L("%s: realPath cannot be empty"), ESR_rc2str(rc));
    goto CLEANUP;
  }
  
  /* Make sure paths end with '/' */
  CHKLOG(rc, PFileSystemCanonicalSlashes(value));
  if (value[LSTRLEN(value)-1] != L('/'))
    LSTRCAT(value, L("/"));
    
  CHKLOG(rc, PHashTableContainsKey(impl->directoryMap, key, &exists));
  if (exists)
  {
    LCHAR* oldValue;
    
    CHKLOG(rc, PHashTableGetValue(impl->directoryMap, key, (void **)&oldValue));
    if (LSTRCMP(oldValue, value) != 0)
    {
      rc = ESR_IDENTIFIER_COLLISION;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
  }
  CHKLOG(rc, PHashTablePutValue(impl->directoryMap, key, value, NULL));
  CHKLOG(rc, PHashTablePutValue(PFileSystemPathMap, key, self, NULL));
  return ESR_SUCCESS;
CLEANUP:
  FREE(key);
  FREE(value);
  return rc;
}

ESR_ReturnCode PANSIFileSystemRemovePathImpl(PFileSystem* self, const LCHAR* virtualPath)
{
  PANSIFileSystemImpl* impl = (PANSIFileSystemImpl*) self;
  LCHAR path[P_PATH_MAX];
  LCHAR* key;
  LCHAR* value;
  PHashTableEntry* entry;
  ESR_ReturnCode rc;
  
  if (virtualPath == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  /* Make sure paths end with '/' */
  LSTRCPY(path, virtualPath);
  CHKLOG(rc, PFileSystemCanonicalSlashes(path));
  if (path[LSTRLEN(path)-1] != L('/'))
    LSTRCAT(path, L("/"));
  CHKLOG(rc, PHashTableGetEntry(impl->directoryMap, path, &entry));
  CHKLOG(rc, PHashTableEntryGetKeyValue(entry, (void **)&key, (void **)&value));
  CHKLOG(rc, PHashTableEntryRemove(entry));
  CHKLOG(rc, PHashTableRemoveValue(PFileSystemPathMap, key, NULL));
  FREE(key);
  FREE(value);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode PANSIFileSystemGetRealPathImpl(PFileSystem* self, LCHAR* path, size_t* len)
{
  PANSIFileSystemImpl* impl = (PANSIFileSystemImpl*) self;
  PHashTableEntry* entry;
  LCHAR* key;
  LCHAR* value;
  LCHAR* bestKey = NULL;
  LCHAR* bestValue = NULL;
  ESR_BOOL isAbsolute;
  ESR_ReturnCode rc;
  
  CHKLOG(rc, PFileSystemGetAbsolutePath(path, len));
  CHKLOG(rc, PHashTableEntryGetFirst(impl->directoryMap, &entry));
  while (entry != NULL)
  {
    CHKLOG(rc, PHashTableEntryGetKeyValue(entry, (void**)&key, (void**)&value));
    if (LSTRNCMP(path, key, LSTRLEN(key)) == 0)
    {
      /* File-system handles file path */
      if (bestKey == NULL || LSTRLEN(key) > LSTRLEN(bestKey))
      {
        /* Found a better match -- the new key is a subdirectory of the previous bestKey */
        bestKey = key;
        bestValue = value;
      }
    }
    CHKLOG(rc, PHashTableEntryAdvance(&entry));
  }
  if (bestKey == NULL)
  {
    rc = ESR_INVALID_STATE;
    PLogError(L("PANSIFileSystem does not handle the specified path: %s"), path);
    goto CLEANUP;
  }
  
  if (LSTRLEN(bestValue) + 1 > *len)
  {
    *len = LSTRLEN(bestValue) + 1;
    rc = ESR_BUFFER_OVERFLOW;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  /* Delete the virtual-path */
  LSTRCPY(path, path + LSTRLEN(bestKey));
  
  CHKLOG(rc, PFileSystemIsAbsolutePath(path, &isAbsolute));
  if (LSTRCMP(bestValue, L("/")) == 0 && isAbsolute)
  {
    /* do nothing */
  }
  else
  {
    /* Insert the key-path */
    CHKLOG(rc, lstrinsert(bestValue, path, 0, len));
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode PANSIFileSystemCreatePFileImpl(PFileSystem* self, const LCHAR* path, ESR_BOOL littleEndian, PFile** file)
{
  LCHAR realPath[P_PATH_MAX];
  size_t len;
  ESR_ReturnCode rc;
  
  LSTRCPY(realPath, path);
  len = P_PATH_MAX;
  CHKLOG(rc, PANSIFileSystemGetRealPathImpl(self, realPath, &len));
  return PANSIFileCreateImpl(realPath, littleEndian, file);
CLEANUP:
  return rc;
}

ESR_ReturnCode PANSIFileSystemSetDefault(ESR_BOOL isDefault)
{
  PANSIFileSystemImpl* impl = (PANSIFileSystemImpl*) PANSIFileSystemSingleton;
  ESR_BOOL exists;
  LCHAR* key = NULL;
  LCHAR* value = NULL;
  PHashTableEntry* entry;
  ESR_ReturnCode rc;
  
  if (isDefault)
  {

		key = MALLOC(sizeof(LCHAR), MTAG);
    if (key == NULL)
    {
      rc = ESR_OUT_OF_MEMORY;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
    LSTRCPY(key, L(""));
    value = MALLOC(sizeof(LCHAR), MTAG);
    if (value == NULL)
    {
      rc = ESR_OUT_OF_MEMORY;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
    LSTRCPY(value, L(""));
    
    CHKLOG(rc, PHashTableContainsKey(impl->directoryMap, key, &exists));
    if (exists)
    {
      LCHAR* key;
      LCHAR* value;
      
      CHKLOG(rc, PHashTableGetEntry(impl->directoryMap, L(""), &entry));
      CHKLOG(rc, PHashTableEntryGetKeyValue(entry, (void **)&key, (void **)&value));
      CHKLOG(rc, PHashTableEntryRemove(entry));
      CHKLOG(rc, PHashTableRemoveValue(PFileSystemPathMap, key, NULL));
      FREE(key);
      FREE(value);
    }
    CHKLOG(rc, PHashTablePutValue(impl->directoryMap, key, value, NULL));
    CHKLOG(rc, PHashTablePutValue(PFileSystemPathMap, key, PANSIFileSystemSingleton, NULL));

		/* Set virtual current working directory to native current working directory */
  }
  else
  {
    CHKLOG(rc, PHashTableContainsKey(impl->directoryMap, L(""), &exists));
    if (exists)
    {
      LCHAR* key;
      LCHAR* value;
      
      CHKLOG(rc, PHashTableGetEntry(impl->directoryMap, L(""), &entry));
      CHKLOG(rc, PHashTableEntryGetKeyValue(entry, (void **)&key, (void **)&value));

      CHKLOG(rc, PHashTableContainsKey(PFileSystemPathMap, L(""), &exists));
      if (exists)
      {
        LCHAR* key;
        PFileSystem* value;
        PHashTableEntry* entry;
        
        CHKLOG(rc, PHashTableGetEntry(PFileSystemPathMap, L(""), &entry));
        CHKLOG(rc, PHashTableEntryGetKeyValue(entry, (void **)&key, (void **)&value));
        if (value == PANSIFileSystemSingleton)
          CHKLOG(rc, PHashTableEntryRemove(entry));
      }

      CHKLOG(rc, PHashTableEntryRemove(entry));
      FREE(key);
      FREE(value);
    }
  }
  return ESR_SUCCESS;
CLEANUP:
  FREE(key);
  FREE(value);
  return rc;
}
