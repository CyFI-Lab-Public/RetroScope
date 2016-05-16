/*---------------------------------------------------------------------------*
 *  PANSIFileSystem.c  *
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
#include "PANSIFileSystemImpl.h"
#include "plog.h"

extern PFileSystem* PANSIFileSystemSingleton;

ESR_ReturnCode PANSIFileSystemAddPath(const LCHAR* virtualPath, const LCHAR* realPath)
{
  return ((PANSIFileSystem*) PANSIFileSystemSingleton)->addPath(PANSIFileSystemSingleton, virtualPath, realPath);
}

ESR_ReturnCode PANSIFileSystemRemovePath(const LCHAR* virtualPath)
{
  return ((PANSIFileSystem*) PANSIFileSystemSingleton)->removePath(PANSIFileSystemSingleton, virtualPath);
}

ESR_ReturnCode PANSIFileSystemGetcwd(LCHAR* path, size_t* len)
{
  return ((PANSIFileSystem*) PANSIFileSystemSingleton)->getcwd(PANSIFileSystemSingleton, path, len);
}

ESR_ReturnCode PANSIFileSystemDestroy(void)
{
  ESR_ReturnCode rc;
  
  if (PANSIFileSystemSingleton == NULL)
    return ESR_SUCCESS;
  CHKLOG(rc, PANSIFileSystemSingleton->destroy(PANSIFileSystemSingleton));
  PANSIFileSystemSingleton = NULL;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}
