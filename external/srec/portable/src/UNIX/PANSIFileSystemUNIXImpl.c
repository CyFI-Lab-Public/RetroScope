/*---------------------------------------------------------------------------*
 *  PANSIFileSystemUNIXImpl.c  *
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

#include <sys/types.h>
#include <sys/stat.h>

#include "errno.h"
#include "PFileSystemImpl.h"
#include "PANSIFileSystem.h"
#include "PANSIFileSystemImpl.h"
#include "phashtable.h"
#include "LCHAR.h"
#include "plog.h"

ESR_ReturnCode PANSIFileSystemGetVirtualPathImpl(PFileSystem* self, LCHAR* path, size_t* len)
{
  PANSIFileSystemImpl* impl = (PANSIFileSystemImpl*) self;
  PHashTableEntry* entry;
  LCHAR driveLetter = 0;
  LCHAR* key;
  LCHAR* value;
  LCHAR* bestKey = NULL;
  LCHAR* bestValue = NULL;
  ESR_BOOL isAbsolute;
  ESR_ReturnCode rc;

  CHKLOG(rc, lstrtrim(path));
  CHKLOG(rc, PFileSystemCanonicalSlashes(path));
  CHKLOG(rc, PFileSystemIsAbsolutePath(path, &isAbsolute));
  if (isAbsolute && path[0] != L('/'))
  {
    /* Skip drive letters in absolute paths */
    driveLetter = path[0];
    LSTRCPY(path, path + 2);
  }
  CHKLOG(rc, PHashTableEntryGetFirst(impl->directoryMap, &entry));
  while (entry!=NULL)
  {
    CHKLOG(rc, PHashTableEntryGetKeyValue(entry, (void **)&key, (void **)&value));
    if (LSTRSTR(path, value)==path)
    {
      /* File-system handles file path */

      if (bestValue==NULL || LSTRLEN(value) > LSTRLEN(bestValue))
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

  /* Delete the real-path */
  LSTRCPY(path, path + LSTRLEN(bestValue));
  /* Insert the virtual-path */
  CHKLOG(rc, lstrinsert(bestKey, path, 0, len));

  /* Bring back the drive letter */
  if (driveLetter!=0)
  {
    CHKLOG(rc, lstrinsert(L("X:/"), path, LSTRLEN(bestKey), len));
    path[LSTRLEN(bestKey)] = driveLetter;
  }
  return ESR_SUCCESS;
 CLEANUP:
  return rc;
}
ESR_ReturnCode PANSIFileSystemMkdirImpl(PFileSystem* self, const LCHAR* path)
{
  LCHAR realPath[P_PATH_MAX];
  size_t len;
  ESR_ReturnCode rc;

  passert(path!=NULL);
  LSTRCPY(realPath, path);
  len = P_PATH_MAX;
  CHKLOG(rc, PANSIFileSystemGetRealPathImpl(self, realPath, &len));

  if (mkdir(realPath, S_IRWXU|S_IRWXG|S_IRWXO ) != 0)
    {
      switch (errno)
      {
       case EEXIST:
         return ESR_IDENTIFIER_COLLISION;
       case ENOENT:
         return ESR_NO_MATCH_ERROR;
       default:
         PLogError(L("ESR_INVALID_STATE"));
         return ESR_INVALID_STATE;
      }
    }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode PANSIFileSystemGetcwdImpl(PFileSystem* self, LCHAR* path, size_t* len)
{
	ESR_ReturnCode rc;

	if (path==NULL)
	{
		rc = ESR_INVALID_ARGUMENT;
		PLogError(ESR_rc2str(rc));
		goto CLEANUP;
	}
    if (getcwd(path, *len) == NULL)
    {
      switch (errno)
      {
       case ERANGE:
         *len = P_PATH_MAX;
         return ESR_BUFFER_OVERFLOW;
       case ENOMEM:
       default:
         PLogError(L("ESR_INVALID_STATE"));
         return ESR_INVALID_STATE;
      }
    }

	CHKLOG(rc, PANSIFileSystemGetVirtualPathImpl(self, path, len));
	return ESR_SUCCESS;
CLEANUP:
	return rc;
}

ESR_ReturnCode PANSIFileSystemChdirImpl(PFileSystem* self, const LCHAR* path)
{
	LCHAR realPath[P_PATH_MAX];
	size_t len;
	ESR_ReturnCode rc;

	passert(path!=NULL);
	LSTRCPY(realPath, path);
	len = P_PATH_MAX;
	CHKLOG(rc, PANSIFileSystemGetRealPathImpl(self, realPath, &len));

	if ((*path != '\0') && (chdir(realPath) != 0))
		return ESR_NO_MATCH_ERROR;
	return ESR_SUCCESS;
CLEANUP:
	return rc;
}
