/*---------------------------------------------------------------------------*
 *  PFileSystemImpl.c  *
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
#include "plog.h"
#include "pmemory.h"

#define MTAG NULL

ESR_BOOL PFileSystemCreated = ESR_FALSE;

/**
 * [file path, PFileSystem*] mapping.
 */
PHashTable* PFileSystemPathMap = NULL;

/**
 * Portable standard input.
 */
PFile* PSTDIN = NULL;
/**
 * Portable standard output.
 */
PFile* PSTDOUT = NULL;
/**
 * Portable standard error.
 */
PFile* PSTDERR = NULL;

/**
 * Current working directory.
 */
LCHAR PFileSystemCurrentDirectory[P_PATH_MAX] = L("/");

#ifdef USE_THREAD
/* Prototype of private function */
PORTABLE_API ESR_ReturnCode PtrdFlush();
#endif


ESR_ReturnCode PFileSystemCreate(void)
{
  ESR_ReturnCode rc;
  
  if (PFileSystemCreated)
    return ESR_SUCCESS;
    
#ifdef USE_STACKTRACE
  CHKLOG(rc, PStackTraceCreate());
#endif
  CHKLOG(rc, PHashTableCreate(NULL, MTAG, &PFileSystemPathMap));
  CHKLOG(rc, PFileSystemInitializeStreamsImpl());
  PFileSystemCreated = ESR_TRUE;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode PFileSystemDestroy(void)
{
  ESR_ReturnCode rc;
  LCHAR* key;
  PHashTableEntry* entry;
  PHashTableEntry* oldEntry;
  
  if (!PFileSystemCreated)
    return ESR_SUCCESS;
  PFileSystemCreated = ESR_FALSE;
  if (PFileSystemPathMap != NULL)
  {
    CHKLOG(rc, PHashTableEntryGetFirst(PFileSystemPathMap, &entry));
    while (entry != NULL)
    {
      CHKLOG(rc, PHashTableEntryGetKeyValue(entry, (void **)&key, (void **)NULL));
      oldEntry = entry;
      CHKLOG(rc, PHashTableEntryAdvance(&entry));
      CHKLOG(rc, PHashTableEntryRemove(oldEntry));
      FREE(key);
    }
    CHKLOG(rc, PHashTableDestroy(PFileSystemPathMap));
    PFileSystemPathMap = NULL;
  }
  CHKLOG(rc, PFileSystemShutdownStreamsImpl());
#ifdef USE_STACKTRACE
  CHKLOG(rc, PStackTraceDestroy());
#endif
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}
