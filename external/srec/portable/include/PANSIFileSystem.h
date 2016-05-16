/*---------------------------------------------------------------------------*
 *  PANSIFileSystem.h  *
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

#ifndef __PANSIFILESYSTEM_H
#define __PANSIFILESYSTEM_H



#include "ESR_ReturnCode.h"
#include "PortPrefix.h"
#include "PFileSystem.h"
#include "ptypes.h"

/**
 * @addtogroup PANSIFileSystemModule PANSIFileSystem API functions
 * Portable file-system API.
 *
 * Must call pmemInit() before using this module.
 * If threads are to be used, ptrdInit() must be called before using this module as well.
 *
 * NOTE: It is technically impossible to provide a cross-platform version of scanf() and its
 *       variants (since vscanf() does not exist). As a result, this module does not provide this
 *       functionality. End-users are encourages to do their own parsing.
 *
 * @{
 */

/**
 * Portable ANSI file-system.
 */
typedef struct PANSIFileSystem_t
{
  /**
   * Superinterface.
   */
  PFileSystem super;
  
  /**
   * Mounts an ANSI path.
   *
   * For example, if "c:/" is mounted as "/dev/c", then any file referenced under "/dev/c" will access
   * "c:/" under the hood.
   *
   * @param self PFileSystem handle
   * @param virtualPath PFileSystem path
   * @param realPath ANSI path
   * @return ESR_INVALID_ARGUMENT if self or virtualPath or realPath is null or realPath is not a valid path;
   * ESR_OUT_OF_MEMORY if the system is out of memory; ESR_IDENTIFIER_COLLISION if virtualPath is already mounted.
   */
  ESR_ReturnCode(*addPath)(PFileSystem* self, const LCHAR* virtualPath, const LCHAR* realPath);
  
  /**
   * Deassociates the file-system from a base path.
   *
   * @param self PFileSystem handle
   * @param virtualPath PFileSystem path
   * @return ESR_INVALID_ARGUMENT if self or virtualPath is null or virtualPath is not mounted
   */
  ESR_ReturnCode(*removePath)(PFileSystem* self, const LCHAR* virtualPath);
  /**
   * Returns the current working directory from the operating-system's point of view.
   * This differs from PFileSystemGetcwd() in that the latter returns the current working
   * directory on the virtual file-system while this function returns the native working directory.
   *
   * @param self PFileSystem handle
   * @param cwd [out] Current working directory
   * @param len [in/out] Length of path argument. If the return code is ESR_BUFFER_OVERFLOW,
   *            the required length is returned in this variable.
   * @return ESR_INVALID_ARGUMENT if self or cwd is null; ESR_BUFFER_OVERFLOW if cwd is not large enough to contain result;
   * ESR_INVALID_STATE if operating-system returns unexpected value.
   */
  ESR_ReturnCode(*getcwd)(PFileSystem* self, LCHAR* cwd, size_t* len);
}
PANSIFileSystem;

/**
 * Initializes the ANSI file-system module.
 *
 * @return ESR_OUT_OF_MEMORY if system is out of memory; ESR_INVALID_STATE if mutex could not be created or if this
 * function is called after the Ptrd module has been shut down.
 */
PORTABLE_API ESR_ReturnCode PANSIFileSystemCreate(void);

/**
 * Shuts down the ANSI file-system module.
 *
 * @return ESR_SUCCESS
 */
PORTABLE_API ESR_ReturnCode PANSIFileSystemDestroy(void);

/**
 * Mounts an ANSI path.
 *
 * For example, if "c:/" is mounted as "/dev/c", then any file referenced under "/dev/c" will access
 * "c:/" under the hood.
 *
 * @param virtualPath PFileSystem path
 * @param realPath ANSI path
 * @return ESR_INVALID_ARGUMENT if self or virtualPath or realPath is null or realPath is not a valid path;
 * ESR_OUT_OF_MEMORY if the system is out of memory; ESR_IDENTIFIER_COLLISION if virtualPath is already mounted.
 */
PORTABLE_API ESR_ReturnCode PANSIFileSystemAddPath(const LCHAR* virtualPath, const LCHAR* realPath);

/**
 * Deassociates the file-system from a base path.
 *
 * @param virtualPath PFileSystem path
 * @return ESR_INVALID_ARGUMENT if self or virtualPath is null or virtualPath is not mounted
 */
PORTABLE_API ESR_ReturnCode PANSIFileSystemRemovePath(const LCHAR* virtualPath);

/**
 * Returns a virtual path associated with the current ANSI directory.
 *
 * For example, if "/dev/ansi" is mapped to "/" and the current ANSI directory is "/usr/bin" then
 * this function will return "/dev/ansi/usr/bin".
 *
 * If multiple virtual paths correspond to the current ANSI directory, the first one will be returned.
 *
 * @param cwd [out] Current working directory
 * @param len [in/out] Length of path argument. If the return code is ESR_BUFFER_OVERFLOW,
 *            the required length is returned in this variable.
 * @return ESR_INVALID_ARGUMENT if self or cwd is null; ESR_BUFFER_OVERFLOW if cwd is not large enough to contain result;
 * ESR_INVALID_STATE if operating-system returns unexpected value.
 */
PORTABLE_API ESR_ReturnCode PANSIFileSystemGetcwd(LCHAR* cwd, size_t* len);

/**
 * Indicates if this file-system should act as the default file-system.
 * If a path is specified which does not match any other file-system, it is resolved using this one.
 *
 * @param isDefault True if the file-system should be the default file-system
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
PORTABLE_API ESR_ReturnCode PANSIFileSystemSetDefault(ESR_BOOL isDefault);

/**
 * @}
 */
#endif /* __PANSIFILESYSTEM_H */
