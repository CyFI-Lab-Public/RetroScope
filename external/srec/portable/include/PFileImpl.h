/*---------------------------------------------------------------------------*
 *  PFileImpl.h  *
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

#ifndef __PFILEIMPL_H
#define __PFILEIMPL_H



#include "PFile.h"
#include "ptrd.h"

/**
 * Portable file implementation.
 */
typedef struct PFileImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  PFile Interface;
  
  /**
   * File name relative to the file-system path.
   */
  LCHAR* filename;
  
  /**
   * True if file is in little endian format.
   */
  ESR_BOOL littleEndian;
  
#ifdef USE_THREAD
  /**
   * Used to lock underlying file and provide atomic read/write operations.
   */
  PtrdMonitor* lock;
#endif
}
PFileImpl;

/**
 * Initializes variables declared in the superinterface.
 *
 * @param self PFile handle
 * @param filename Name of the file
 * @param littleEndian True if file is little-endian
 * @return ESR_OUT_OF_MEMORY if system is out of memory
 */
PORTABLE_API ESR_ReturnCode PFileCreateImpl(PFile* self, const LCHAR* filename, ESR_BOOL littleEndian);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode PFileDestroyImpl(PFile* self);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode PFileGetFilenameImpl(PFile* self, LCHAR* filename, size_t* len);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode PFileVfprintfImpl(PFile* self, int* result, const LCHAR* format, va_list args);

#endif /* __PFILEIMPL_H */
