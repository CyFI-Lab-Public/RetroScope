/*---------------------------------------------------------------------------*
 *  PANSIFileSystemImpl.h  *
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

#ifndef __PANSIFILESYSTEMIMPL_H
#define __PANSIFILESYSTEMIMPL_H



#include "ESR_ReturnCode.h"
#include "PortPrefix.h"
#include "PANSIFileSystem.h"
#include "phashtable.h"

/**
 * Portable file-system implementation.
 */
typedef struct PANSIFileSystemImpl_t
{
  /**
   * Superinterface.
   */
  PANSIFileSystem super;
  
  /**
   * [virtualPath, realPath] mapping.
   */
  PHashTable* directoryMap;
}
PANSIFileSystemImpl;


/**
 * ANSI file system, singleton instance.
 */
PORTABLE_API PFileSystem* PANSIFileSystemSingleton;

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileSystemDestroyImpl(PFileSystem* self);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileSystemCreatePFileImpl(PFileSystem* self, const LCHAR* path, ESR_BOOL littleEndian, PFile** file);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileSystemAddPathImpl(PFileSystem* self, const LCHAR* virtualPath, const LCHAR* realPath);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileSystemRemovePathImpl(PFileSystem* self, const LCHAR* virtualPath);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileSystemMkdirImpl(PFileSystem* self, const LCHAR* path);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileSystemChdirImpl(PFileSystem* self, const LCHAR* path);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileSystemGetcwdImpl(PFileSystem* self, LCHAR* cwd, size_t* len);

/**
 * Given a virtual-path, convert it to a real-path.
 *
 * @param self PFileSystem handle
 * @param path Virtual-path
 * @param len [out] Size of path argument. If the return code is ESR_BUFFER_OVERFLOW,
 *            the required length is returned in this variable.
 */
PORTABLE_API ESR_ReturnCode PANSIFileSystemGetRealPathImpl(PFileSystem* self, LCHAR* path, size_t* len);

/**
 * Given a real-path, convert it to a virtual-path.
 *
 * @param self PFileSystem handle
 * @param path Real path
 * @param len [out] Size of path argument. If the return code is ESR_BUFFER_OVERFLOW,
 *            the required length is returned in this variable.
 */
PORTABLE_API ESR_ReturnCode PANSIFileSystemGetVirtualPathImpl(PFileSystem* self, LCHAR* path, size_t* len);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode PFileSystemShutdownStreams(void);

#endif /* __PANSIFILESYSTEMIMPL_H */
