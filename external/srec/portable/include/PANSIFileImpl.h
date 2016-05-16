/*---------------------------------------------------------------------------*
 *  PANSIFileImpl.h  *
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

#ifndef __PANSIFILEIMPL_H
#define __PANSIFILEIMPL_H



#include "PFile.h"
#include "PFileImpl.h"
#ifdef USE_THREAD
#include "ptrd.h"
#endif

/**
 * Portable file, ANSI implementation.
 */
typedef struct PANSIFileImpl_t
{
  /**
   * Superinterface.
   */
  PFileImpl Interface;
  
  /**
   * Underlying file.
   */
  FILE* value;
}
PANSIFileImpl;


/**
 * ANSI implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileCreateImpl(const LCHAR* filename, ESR_BOOL isLittleEndian, PFile** self);

/**
 * ANSI implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileDestroyImpl(PFile* self);

/**
 * ANSI implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileOpenImpl(PFile* self, const LCHAR* mode);

/**
 * ANSI implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileCloseImpl(PFile* self);

/**
 * ANSI implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileReadImpl(PFile* self, void* buffer, size_t size, size_t* count);

/**
 * ANSI implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileWriteImpl(PFile* self, void* buffer, size_t size, size_t* count);

/**
 * ANSI implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileFlushImpl(PFile* self);

/**
 * ANSI implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileSeekImpl(PFile* self, long offset, int origin);

/**
 * ANSI implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileGetPositionImpl(PFile* self, size_t* position);

/**
 * ANSI implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileIsOpenImpl(PFile* self, ESR_BOOL* isOpen);

/**
 * ANSI implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileIsEOFImpl(PFile* self, ESR_BOOL* isEof);

/**
 * ANSI implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileIsErrorSetImpl(PFile* self, ESR_BOOL* isError);

/**
 * ANSI implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileClearErrorImpl(PFile* self);

/**
 * ANSI implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileVfprintfImpl(PFile* self, int* result, const LCHAR* format, va_list args);

/**
 * ANSI implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileFgetcImpl(PFile* self, LINT* result);

/**
 * ANSI implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileFgetsImpl(PFile* self, LCHAR* string, int n, LCHAR** result);

/**
 * ANSI implementation.
 */
PORTABLE_API ESR_ReturnCode PANSIFileHideMemoryAllocation(PFile* self);

#endif /* __PANSIFILEIMPL_H */
