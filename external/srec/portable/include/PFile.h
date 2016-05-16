/*---------------------------------------------------------------------------*
 *  PFile.h  *
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

#ifndef __PFILE_H
#define __PFILE_H



#include <stdarg.h>
#include <stddef.h>

#include "ESR_ReturnCode.h"
#include "PortPrefix.h"
#include "ptypes.h"
#include "pstdio.h"


/**
 * @addtogroup PFileModule PFile API functions
 * Portable file API.
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

#define USE_LIGHT_WEIGHT_PANSI_FILE_WRAPPERS        1


#ifdef USE_NARROW_CHAR

/**
 * Portable EOF constant
 */
#define PEOF EOF

#else

/**
* Portable EOF constant
*/
#define PEOF WEOF

#endif /* USE_NARROW_CHAR */

/**
 * Portable file.
 */

#ifdef USE_LIGHT_WEIGHT_PANSI_FILE_WRAPPERS

typedef FILE PFile;

#else
typedef struct PFile_t
{
  /**
  * Closes the PFile and destroys it.
  *
  * @param self PFile handle
  * @return ESR_INVALID_ARGUMENT if self is null
  */
  ESR_ReturnCode(*destroy)(struct PFile_t* self);

  
  ESR_ReturnCode(*open)(struct PFile_t* self, const LCHAR* mode);

  /**
   * Closes a PFile.
   *
   * @param self PFile handle
    * @return ESR_CLOSE_ERROR if file cannot be closed
   */
  ESR_ReturnCode(*close)(struct PFile_t* self);

  /**
   * Reads from a PFile.
   *
   * @param self PFile handle
   * @param buffer Storage location for data
   * @param size Item size in bytes
   * @param count [in/out] Maximum number of items to be read. On output, contains the
   *              number of full items actually read, which may be less than count if
   *              an error occurs or if the end of the file is encountered before reaching
   *              count.
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_READ_ERROR if a reading error occurs
   */
  ESR_ReturnCode(*read)(struct PFile_t* self, void* buffer, size_t size, size_t* count);

  /**
   * Writes data to a PFile.
   *
   * @param self PFile handle
   * @param buffer Pointer to data to be written
   * @param size Item size in bytes
   * @param count [in/out] Maximum number of items to be read. On output, contains the
   *              number of full items actually written, which may be less than count if
   *              an error occurs.
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_WRITE_ERROR if a writing error occurs
   */
  ESR_ReturnCode(*write)(struct PFile_t* self, const void* buffer, size_t size, size_t* count);

  /**
   * Flushes a PFile.
   *
   * @param self PFile handle
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_FLUSH_ERROR if a flushing error occurs
   */
  ESR_ReturnCode(*flush)(struct PFile_t* self);

  /**
   * Flushes a PFile.
   *
   * @param self PFile handle
   * @param offset Number of bytes from <code>origin</code>
   * @param origin Initial position
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_SEEK_ERROR if a seeking error occurs
   */
  ESR_ReturnCode(*seek)(struct PFile_t* self, long offset, int origin);

  /**
   * Gets the current position of a PFile.
   *
   * @param self PFile handle
   * @param position [out] The position
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_STATE if an internal error occurs
   */
  ESR_ReturnCode(*getPosition)(struct PFile_t* self, size_t* position);

  /**
   * Indicates if the PFile is open.
   *
   * @param self PFile handle
   * @param isOpen [out] True if file is open
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*isOpen)(struct PFile_t* self, ESR_BOOL* isOpen);

  /**
   * Indicates if the PFile is at the end of file.
   *
   * @param self PFile handle
   * @param isEof [out] True if end of file has been reached
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*isEOF)(struct PFile_t* self, ESR_BOOL* isEof);

  /**
   * Returns filename associated with PFile.
   *
   * @param self PFile handle
   * @param filename [out] The filename
   * @param len [in/out] Length of filename argument. If the return code is ESR_BUFFER_OVERFLOW,
   *            the required length is returned in this variable.
   * @return ESR_INVALID_ARGUMENT if self or filename are null; ESR_BUFFER_OVERFLOW if buffer is too small
   * to contain results
    */
  ESR_ReturnCode(*getFilename)(struct PFile_t* self, LCHAR* filename, size_t* len);

  /**
   * Indicates if the error-flag is set in the PFile. This functionality is provided solely
   * for backwards-compatibility reasons with ANSI-C ferror().
   *
   * @param self PFile handle
   * @param isError [out] True if the error-flag is set
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*isErrorSet)(struct PFile_t* self, ESR_BOOL* isError);

  /**
   * Clears the error-flag in the PFile. This functionality is provided solely
   * for backwards-compatibility reasons with ANSI-C ferror().
   *
   * @param self PFile handle
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*clearError)(struct PFile_t* self);

  /**
   * vfprintf() implementation for PFile.
   *
   * @param self PFile handle
   * @param result Function result
   * @param format see vfprintf()
   * @param args see vfprintf()
   * @return see vfprintf()
   */
  ESR_ReturnCode(*vfprintf)(struct PFile_t* self, int* result, const LCHAR* format, va_list args);
  /**
   * fgetc() implementation for PFile.
   * In case the underlying function returns an error, it will be propegated by the wrapper.
   *
   * @param self PFile handle
   * @param result Function result
   * @return see fgetc()
   */
  ESR_ReturnCode(*fgetc)(struct PFile_t* self, LINT* result);
  /**
   * fgets() implementation for PFile.
   * In case the underlying function returns an error, it will be propegated by the wrapper.
   *
   * @param self PFile handle
   * @param string See fgets()
   * @param n See fgets()
   * @param result Function result
   * @return see fgets()
   */
  ESR_ReturnCode(*fgets)(struct PFile_t* self, LCHAR* string, int n, LCHAR** result);
  /**
   * Hide the memory footprint of this object from the PMemory module.
   *
   * NOTE: Because this function may be called by PMemory on shutdown,
   *       no PLog (which is shutdown before PMemory) functions should
   *       be used.
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*hideMemoryAllocation)(struct PFile_t* self);
}
PFile;

#endif



/*
 * Expose functions only if use wrappers is not defined, otherwize only expose wrapper functions.
 */

#ifndef USE_LIGHT_WEIGHT_PANSI_FILE_WRAPPERS

/**
 * Closes the PFile and destroys it.
 *
 * @param self PFile handle
 * @return ESR_INVALID_ARGUMENT if self is null
 */
PORTABLE_API ESR_ReturnCode PFileDestroy(PFile* self);


PORTABLE_API ESR_ReturnCode PFileOpen(PFile* self, const LCHAR* mode);

/**
 * Closes a PFile.
 *
 * @param self PFile handle
 * @return ESR_CLOSE_ERROR if file cannot be closed
 */
PORTABLE_API ESR_ReturnCode PFileClose(PFile* self);

/**
 * Reads from a PFile.
 *
 * @param self PFile handle
 * @param buffer Storage location for data
 * @param size Item size in bytes
 * @param count [in/out] Maximum number of items to be read. On output, contains the
 *              number of full items actually read, which may be less than count if
 *              an error occurs or if the end of the file is encountered before reaching
 *              count.
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_READ_ERROR if a reading error occurs
 */
PORTABLE_API ESR_ReturnCode PFileRead(PFile* self, void* buffer, size_t size, size_t* count);

/**
 * Writes data to a PFile.
 *
 * @param self PFile handle
 * @param buffer Pointer to data to be written
 * @param size Item size in bytes
 * @param count [in/out] Maximum number of items to be read. On output, contains the
 *              number of full items actually written, which may be less than count if
 *              an error occurs.
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_WRITE_ERROR if a writing error occurs
 */
PORTABLE_API ESR_ReturnCode PFileWrite(PFile* self, void* buffer, size_t size, size_t* count);

/**
 * Flushes a PFile.
 *
 * @param self PFile handle
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_FLUSH_ERROR if a flushing error occurs
 */
PORTABLE_API ESR_ReturnCode PFileFlush(PFile* self);

/**
 * Flushes a PFile.
 *
 * @param self PFile handle
 * @param offset Number of bytes from <code>origin</code>
 * @param origin Initial position
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_SEEK_ERROR if a seeking error occurs
 */
PORTABLE_API ESR_ReturnCode PFileSeek(PFile* self, long offset, int origin);

/**
 * Gets the current position of a PFile.
 *
 * @param self PFile handle
 * @param position [out] The position
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_STATE if an internal error occurs
 */
PORTABLE_API ESR_ReturnCode PFileGetPosition(PFile* self, size_t* position);

/**
 * Indicates if the PFile is open.
 *
 * @param self PFile handle
 * @param isOpen [out] True if file is open
 * @return ESR_INVALID_ARGUMENT if self is null
 */
PORTABLE_API ESR_ReturnCode PFileIsOpen(PFile* self, ESR_BOOL* isOpen);


/**
 * Indicates if the PFile is at the end of file.
 *
 * @param self PFile handle
 * @param isEof [out] True if end of file has been reached
 * @return ESR_INVALID_ARGUMENT if self is null
 */
PORTABLE_API ESR_ReturnCode PFileIsEOF(PFile* self, ESR_BOOL* isEof);

/**
 * Indicates if the error-flag is set in the PFile. This functionality is provided solely
 * for backwards-compatibility reasons with ANSI-C ferror().
 *
 * @param self PFile handle
 * @param isError [out] True if the error-flag is set
 * @return ESR_INVALID_ARGUMENT if self is null
 */
PORTABLE_API ESR_ReturnCode PFileIsErrorSet(PFile* self, ESR_BOOL* isError);

/**
 * Clears the error-flag in the PFile. This functionality is provided solely
 * for backwards-compatibility reasons with ANSI-C ferror().
 *
 * @param self PFile handle
 * @return ESR_INVALID_ARGUMENT if self is null
 */
PORTABLE_API ESR_ReturnCode PFileClearError(PFile* self);

/**
 * fprintf() implementation for PFile.
 *
 * @param self PFile handle
 * @param result Function result
 * @param format see fprintf()
 * @param args see fprintf()
 * @return see fprintf()
 */
PORTABLE_API ESR_ReturnCode PFileFprintf(PFile* self, int* result, const LCHAR* format, va_list args);

/**
 * vfprintf() implementation for PFile.
 *
 * @param self PFile handle
 * @param result Function result
 * @param format see vfprintf()
 * @param args see vfprintf()
 * @return see vfprintf()
 */
PORTABLE_API ESR_ReturnCode PFileVfprintf(PFile* self, int* result, const LCHAR* format, va_list args);
/**
 * fgetc() implementation for PFile.
 *
 * @param self PFile handle
 * @param result Function result
 * @return see fgetc()
 */
PORTABLE_API ESR_ReturnCode PFileFgetc(PFile* self, LINT* result);
/**
 * fgets() implementation for PFile.
 *
 * @param self PFile handle
 * @param string See fgets()
 * @param n See fgets()
 * @param result Function result
 * @return see fgets()
 */
PORTABLE_API ESR_ReturnCode PFileFgets(PFile* self, LCHAR* string, int n, LCHAR** result);

/**
 * Reads an integer from the PFile.
 *
 * @param self PFile handle
 * @param value [out] Integer that was read
 * @return ESR_READ_ERROR if a reading error occurs; ESR_SEEK_ERROR if a seeking error occurs;
 * ESR_INVALID_STATE if no EOF is reached before an integer
 */
PORTABLE_API ESR_ReturnCode PFileReadInt(PFile* self, int* value);

/**
 * Reads a string token from the PFile.
 *
 * @param self PFile handle
 * @param value [out] String that was read
 * @param len Size of value argument.
 * @return ESR_BUFFER_OVERFLOW if the value argument is too small; ESR_READ_ERROR if a reading error occurs;
 * ESR_SEEK_ERROR if a seeking error occurs; ESR_INVALID_STATE if no EOF is reached before an LCHAR*
 */
PORTABLE_API ESR_ReturnCode PFileReadLCHAR(PFile* self, LCHAR* value, size_t len);

/**
 * Returns filename associated with PFile.
 *
 * @param self PFile handle
 * @param filename [out] The filename
 * @param len [in/out] Length of filename argument. If the return code is ESR_BUFFER_OVERFLOW,
 *            the required length is returned in this variable.
 * @return ESR_INVALID_ARGUMENT if self or filename are null; ESR_BUFFER_OVERFLOW if buffer is too small
 * to contain results
 */
PORTABLE_API ESR_ReturnCode PFileGetFilename(PFile* self, LCHAR* filename, size_t* len);

#endif /* USE_LIGHT_WEIGHT_PANSI_FILE_WRAPPERS */

/**
 * Backwards compatible fopen().
 *
 * @param filename See fopen()
 * @param mode See fopen()
 * @return see fopen()
 */
PORTABLE_API PFile* pfopen(const LCHAR* filename, const LCHAR* mode);

/**
 * Backwards compatible fread().
 *
 * @param buffer See fread()
 * @param size See fread()
 * @param count See fread()
 * @param stream See fread()
 * @return see fread()
 */
PORTABLE_API size_t pfread(void* buffer, size_t size, size_t count, PFile* stream);

/**
 * Backwards compatible fwrite().
 *
 * @param buffer See fwrite()
 * @param size See fwrite()
 * @param count See fwrite()
 * @param stream See fwrite()
 * @return see fwrite()
 */
PORTABLE_API size_t pfwrite(const void* buffer, size_t size, size_t count, PFile* stream);

/**
 * Backwards compatible fclose().
 *
 * @param stream See fclose()
 * @return see fclose()
 */
PORTABLE_API int pfclose(PFile* stream);

/**
 * Backwards compatible rewind()
 *
 * @param stream See rewind()
 * @return see rewind()
 */
PORTABLE_API void prewind(PFile* stream);

/**
 * Backwards compatible fseek().
 *
 * @param stream See fseek()
 * @param offset See fseek()
 * @param origin See fseek()
 * @return see fseek()
 */
PORTABLE_API int pfseek(PFile* stream, long offset, int origin);

/**
 * Backwards compatible ftell().
 *
 * @param stream See ftell()
 * @return see ftell()
 */
PORTABLE_API long pftell(PFile* stream);

/**
 * Backwards compatible fgets().
 *
 * @param string See fgets()
 * @param n See fgets()
 * @param stream See fgets()
 * @return see fgets()
 */
PORTABLE_API LCHAR* pfgets(LCHAR* string, int n, PFile* stream);

/**
 * Backwards compatible feof().
 *
 * @param stream See feof()
 * @return see feof()
 */
PORTABLE_API int pfeof(PFile* stream);

/**
 * Backwards compatible ferror().
 *
 * @param stream See ferror()
 * @return see ferror()
 */
PORTABLE_API int pferror(PFile* stream);

/**
 * Backwards compatible clearerr().
 *
 * @param stream See clearerr()
 */
PORTABLE_API void pclearerr(PFile* stream);

/**
 * Backwards compatible fgetc().
 *
 * @param stream See clearerr()
 * @return see clearerr()
 */
PORTABLE_API LINT pfgetc(PFile* stream);

/**
 * Backwards compatible fflush().
 *
 * @param stream See fflush()
 * @return see fflush()
 */
PORTABLE_API int pfflush(PFile* stream);

/**
 * Backwards compatible vfprintf().
 *
 * @param stream See vfprintf()
 * @param format See vfprintf()
 * @param args See vfprintf()
 * @return see vfprintf()
 */
PORTABLE_API int pvfprintf(PFile* stream, const LCHAR* format, va_list args);

/**
 * Backwards compatible fprintf().
 *
 * @param stream See fprintf()
 * @param format See fprintf()
 * @return see fprintf()
 */
PORTABLE_API int pfprintf(PFile* stream, const LCHAR* format, ...);

/**
 * Backwards compatible printf().
 *
 * @param format See printf()
 * @return see printf()
 */

#ifndef USE_LIGHT_WEIGHT_PANSI_FILE_WRAPPERS
PORTABLE_API int pprintf(const LCHAR* format, ...);
#endif

/*
 * The following are only defined when using pfile wrappers.
 */

#ifdef USE_LIGHT_WEIGHT_PANSI_FILE_WRAPPERS
PORTABLE_API ESR_ReturnCode pf_convert_backslashes_to_forwardslashes ( LCHAR *string_to_convert );
PORTABLE_API ESR_ReturnCode pf_is_path_absolute ( const LCHAR* input_path, ESR_BOOL* isAbsolute );
PORTABLE_API ESR_ReturnCode pf_make_dir ( const LCHAR* path );
PORTABLE_API ESR_ReturnCode pf_get_cwd ( LCHAR* path, size_t *len );
PORTABLE_API ESR_ReturnCode pf_change_dir ( const LCHAR* path );
#endif

/**
 * @}
 */
#endif /* __PFILE_H */
