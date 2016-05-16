/*---------------------------------------------------------------------------*
 *  PFileSystemImpl.h  *
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

#ifndef __PFILESYSTEMIMPL_H
#define __PFILESYSTEMIMPL_H



#include "ESR_ReturnCode.h"
#include "PortPrefix.h"
#include "PFileSystem.h"
#include "phashtable.h"

/**
 * Portable file-system implementation.
 */
typedef struct PFileSystemImpl_t
{
  /**
   * Superinterface.
   */
  PFileSystem super;
  
}
PFileSystemImpl;


/**
 * [file path, PFileSystem*] mapping.
 */
PORTABLE_API PHashTable* PFileSystemPathMap;

/**
 * Current working directory.
 */
PORTABLE_API LCHAR PFileSystemCurrentDirectory[P_PATH_MAX];

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode PFileSystemDestroyImpl(PFileSystem* self);

/**
 * Initialize PSTDIN, PSTDOUT, PSTDERR.
 *
 * @return ESR_OUT_OF_MEMORY if system is out of memory; ESR_INVALID_STATE if mutex could not be created or if this
 * function is called after the Ptrd module has been shut down.
 */
PORTABLE_API ESR_ReturnCode PFileSystemInitializeStreamsImpl(void);
/**
 * Shutdown PSTDIN, PSTDOUT, PSTDERR.
 */
PORTABLE_API ESR_ReturnCode PFileSystemShutdownStreamsImpl(void);

/**
 * Associates a base path with the file system.
 *
 * For example, if "/dev/cdrom" is specified, then any file under that path
 * must make use of this file-system.
 *
 * @param basePath Base path for files associated with this filesystem
 * @return ESR_INVALID_ARGUMENT if self or virtualPath or realPath is null or realPath is not a valid path;
 * ESR_OUT_OF_MEMORY if the system is out of memory; ESR_IDENTIFIER_COLLISION if virtualPath is already mounted.
 */
PORTABLE_API ESR_ReturnCode PFileSystemAddPathImpl(PFileSystem* self, const LCHAR* basePath);

/**
 * Deassociates the file-system from a base path.
 *
 * @param self PFileSystem handle
 * @param basePath Base path for files associated with this filesystem
 * @return ESR_INVALID_ARGUMENT if self or virtualPath is null or virtualPath is not mounted
 */
PORTABLE_API ESR_ReturnCode PFileSystemRemovePathImpl(PFileSystem* self, const LCHAR* basePath);

/**
 * Given a path, returns the associated file-system and relative path.
 *
 * @param path Path to look up
 * @param fileSystem [out] File-system which matches the path
 * @param relativePath [out] Relative path associated with match. Set to NULL if this value shouldn't be returned.
 *                           Otherwise, the buffer must be of size P_PATH_MAX.
 * @return ESR_INVALID_ARGUMENT if path, fileSystem or relativePath is null; ESR_INVALID_STATE if no
 * file-system handles the path
 */
PORTABLE_API ESR_ReturnCode PFileSystemGetFS(const LCHAR* path, PFileSystem** fileSystem, LCHAR* relativePath);

/**
 * Indicates if the specified path refers to a directory. This function does not actually
 * try resolving the path using a file-system to see if it exists. The result is based purely on the contents
 * of the string.
 *
 * @param path Path to look up
 * @param isDirectory [out] TRUE if path refers to a directory
 * @return ESR_INVALID_ARGUMENT if path or isDirectory is null
 */
PORTABLE_API ESR_ReturnCode PFileSystemIsDirectoryPath(const LCHAR* path, ESR_BOOL* isDirectory);

#endif /* __PFILESYSTEMIMPL_H */
