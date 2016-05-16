/*---------------------------------------------------------------------------*
 *  PFileSystem.h  *
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

#ifndef __PFILESYSTEM_H
#define __PFILESYSTEM_H



#include "ESR_ReturnCode.h"
#include "PortPrefix.h"
#include "PFile.h"
#include "ptypes.h"

/**
 * @addtogroup PFileSystemModule PFileSystem API functions
 * Portable file-system API.
 *
 * Must call pmemInit() before using this module.
 * If threads are to be used, ptrdInit() must be called before using this module as well.
 *
 * @{
 */

/**
 * Portable standard input.
 */
/*PORTABLE_API PFile* PSTDIN;*/
/**
 * Portable standard output.
 */
/*PORTABLE_API PFile* PSTDOUT;*/
/**
 * Portable standard error.
 */
/*PORTABLE_API PFile* PSTDERR;*/

#define PSTDIN 	stdin
#define PSTDOUT stdout
#define PSTDERR stderr
/**
 * Portable file-system.
 */
typedef struct PFileSystem_t
{
  /**
  * Destroys the PFileSystem.
  *
  * @param self PFileSystem handle
  * @return ESR_INVALID_ARGUMENT if self is null
  */
  ESR_ReturnCode(*destroy)(struct PFileSystem_t* self);
  
  /**
    * Creates a new PFile using this file-system.
   *
   * @param self PFileSystem handle
   * @param path Fully qualified file path
   * @param littleEndian True if file is in little-endian format
   * @param file [out] Resulting PFile
   */
  ESR_ReturnCode(*createPFile)(struct PFileSystem_t* self, const LCHAR* path, ESR_BOOL littleEndian, PFile** file);
  
  /**
   * Creates a new directory.
   *
   * @param self PFileSystem handle
   * @param path Fully qualified directory path
    * @return ESR_INVALID_ARGUMENT if path is null; ESR_IDENTIFIER_COLLISION if directory already exists;
    * ESR_NO_MATCH_ERROR if parent directory does not exist; ESR_INVALID_STATE if an internal error occurs
   */
  ESR_ReturnCode(*mkdir)(struct PFileSystem_t* self, const LCHAR* path);
  
  /**
   * Sets the current working directory.
   *
   * @param self PFileSystem handle
   * @param path Fully qualified file path
   * @return ESR_SUCCESS if change of directory is allowed
   */
  ESR_ReturnCode(*chdir)(struct PFileSystem_t* self, const LCHAR* path);
}
PFileSystem;


/**
 * Initializes the portable file-system module.
 *
 * @return ESR_INVALID_STATE if calling StackTraceCreate() fails (see its documentation for more detail).
 */
PORTABLE_API ESR_ReturnCode PFileSystemCreate(void);

/**
 * Indicates if the portable file-system module is initialized.
 *
 * @param isCreated [in/out] True if the module is initialized.
 * @return ESR_INVALID_ARGUMENT if isCreated is null
 */
PORTABLE_API ESR_ReturnCode PFileSystemIsCreated(ESR_BOOL* isCreated);

/**
 * Shuts down the portable file-system module.
 *
 * @return ESR_INVALID_ARGUMENT if self is null
 */
PORTABLE_API ESR_ReturnCode PFileSystemDestroy(void);

/**
 * Creates a new PFile using this file-system.
 *
 * @param path Fully qualified file path
 * @param littleEndian True if file is in little-endian format
 * @param file [out] Resulting PFile
 * @return ESR_OUT_OF_MEMORY if system is out of memory; ESR_INVALID_STATE if mutex could not be created
 */
PORTABLE_API ESR_ReturnCode PFileSystemCreatePFile(const LCHAR* path, ESR_BOOL littleEndian, PFile** file);

/**
 * Indicates if path is absolute.
 *
 * @param path Path to be processed
 * @param isAbsolute True if path is absolute
 * @return ESR_INVALID_ARGUMENT if path or isAbsolute are null
 */
PORTABLE_API ESR_ReturnCode PFileSystemIsAbsolutePath(const LCHAR* path, ESR_BOOL* isAbsolute);

/**
 * Returns the canonical pathname string of this abstract pathname.
 *
 * A canonical pathname is both absolute and unique. The precise definition of canonical
 * form is system-dependent. This method first converts this pathname to absolute form.
 * This typically involves removing redundant names such as "." and ".." from the pathname,
 * resolving symbolic links (on UNIX platforms), and converting drive letters to
 * a standard case (on Microsoft Windows platforms).
 *
 * POST-CONDITION: Path will contain only canonical slashes
 *
 * @param path Path to process
 * @param len [in/out] Length of path argument. If the return code is ESR_BUFFER_OVERFLOW,
 *            the required length is returned in this variable.
 * @return ESR_INVALID_ARGUMENT if path or len are null
 */
PORTABLE_API ESR_ReturnCode PFileSystemGetAbsolutePath(LCHAR* path, size_t* len);

/**
 * Converts all slashes in path to '/'.
 *
 * @param path [in/out] Path to process
 * @return ESR_INVALID_ARGUMENT if path is null
 */
PORTABLE_API ESR_ReturnCode PFileSystemCanonicalSlashes(LCHAR* path);

/**
 * Returns parent directory of specified path.
 * If the path ends with a filename, its directory is returned.
 * If the path ends with a directory, its parent directory is returned.
 *
 * PRECONDITION: Directory names must end with '/'
 *
 * @param path [in/out] Path to process
 * @param len [in/out] Length of path argument. If the return code is ESR_BUFFER_OVERFLOW,
 *            the required length is returned in this variable.
 * @return ESR_INVALID_ARGUMENT if path or len are null; ESR_BUFFER_OVERFLOW if path is too small to contain the result
 */
PORTABLE_API ESR_ReturnCode PFileSystemGetParentDirectory(LCHAR* path, size_t* len);

/**
 * Creates a new directory.
 *
 * @param path Directory path
 * @return ESR_INVALID_ARGUMENT if path is null; ESR_IDENTIFIER_COLLISION if directory already exists;
 * ESR_NO_MATCH_ERROR if parent directory does not exist; ESR_INVALID_STATE if an internal error occurs
 */
PORTABLE_API ESR_ReturnCode PFileSystemMkdir(const LCHAR* path);

/**
 * Returns the current working directory (always ends with '/').
 *
 * @param path [out] The current working directory
 * @param len [in/out] Length of path argument. If the return code is ESR_BUFFER_OVERFLOW,
 *            the required length is returned in this variable.
 * @return ESR_INVALID_ARGUMENT if path or len are null; ESR_BUFFER_OVERFLOW if path is too small to contain the result
 */
PORTABLE_API ESR_ReturnCode PFileSystemGetcwd(LCHAR* path, size_t* len);

/**
 * Sets the current working directory.
 *
 * @param path Fully qualified file path
 * @return ESR_SUCCESS if change of directory is allowed
 */
PORTABLE_API ESR_ReturnCode PFileSystemChdir(const LCHAR* path);

/**
 * Converts a linear path string to an array of path tokens.
 * Tokens ending with a '/' denote a directory, otherwise they are a file.
 *
 * POST-CONDITION: The array is allocated internally, but must be freed by the caller.
 *
 * @param path Command-line string to parse
 * @param tokenArray [out] The array used to hold the tokens
 * @param count [out] The number of tokens found
 * @return ESR_INVALID_ARGUMENT if path, tokenArray or count are null; ESR_OUT_OF_MEMORY if system is out of memory
 */
PORTABLE_API ESR_ReturnCode PFileSystemLinearToPathTokens(const LCHAR* path, LCHAR*** tokenArray, size_t* count);

/**
 * @}
 */
#endif /* __PFILESYSTEM_H */
