/*---------------------------------------------------------------------------*
 *  pmemory.h  *
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

#ifndef PMEMORY_H
#define PMEMORY_H



/* #define PMEM_MAP_TRACE */

#include "PortPrefix.h"
#include "ptypes.h"
#include "pstdio.h"
#include <stddef.h>
#include <stdlib.h>

/**
 * @addtogroup PmemoryModule PMemory API functions
 * Library for basic memory management.
 * Call PMemInit() to initialize and PmemShutdown() to shutdown module.
 *
 * @{
 */

/**
 * Returns macro to string format.
 */
#define _VAL(x) #x
/**
 * Converts a digit to a string.
 */
#define _STR(x) _VAL(x)

#ifndef offsetof
#define offsetof(type, member) ((size_t) &(((type *)0)->member))
#endif

/**
 * \<static_cast\> implementation for C.
 */
#define STATIC_CAST(self, subClass, member) ((subClass*) (((char*) self) - (offsetof(subClass, member))))


#define USE_STDLIB_MALLOC

#ifdef USE_STDLIB_MALLOC

#define MALLOC(n, tag) malloc(n)
#define CALLOC(m, n, tag) calloc(m, n)
#define CALLOC_CLR(m, n, tag) calloc(m, n)
#define REALLOC(p, n) realloc(p, n)
#define FREE(p) free(p)
#define NEW(type, tag) ((type*)MALLOC(sizeof(type), tag))
#define NEW_ARRAY(type, n, tag) ((type*)CALLOC(n, sizeof(type), tag))

#define PMemInit() ESR_SUCCESS
#define PMemShutdown() ESR_SUCCESS
#define PMemSetLogFile(f) ESR_NOT_SUPPORTED
#define PMemDumpLogFile() ESR_NOT_SUPPORTED
#define PMemSetLogEnabled(b) ESR_NOT_SUPPORTED
#define PMemLogFree(p) (free(p), ESR_SUCCESS)
#define PMemReport(f) ESR_NOT_SUPPORTED
#define PMemorySetPoolSize(n) ESR_NOT_SUPPORTED
#define PMemoryGetPoolSize(p) ESR_NOT_SUPPORTED

#else

#ifdef DISABLE_MALLOC
#define malloc #error
#define calloc #error
#define realloc #error
#define free #error
#endif

/*
 * PMEM_MAP_TRACE is not defined by default.
 * It is up to user to define PMEM_MAP_TRACE;
 * define in either makefile or here for test purpose.
 */

#ifdef PMEM_MAP_TRACE
/**
 * Portable malloc()
 */
#define MALLOC(nbBytes, tag) (pmalloc(nbBytes, tag, L(__FILE__), __LINE__))
#else
/**
 * Portable malloc()
 */
#define MALLOC(nbBytes, tag) (pmalloc(nbBytes))
#endif

#ifdef PMEM_MAP_TRACE
/**
 * Portable calloc()
 */
#define CALLOC(nbElem, elemSize, tag) (pcalloc(nbElem,  elemSize  , tag, L(__FILE__), __LINE__))
#define CALLOC_CLR(nbElem, elemSize, tag) (pcalloc(nbElem,  elemSize  , tag, L(__FILE__), __LINE__))
#else
/**
 * Portable calloc()
 */
#define CALLOC(nbElem, elemSize, tag) (pcalloc(nbElem,  elemSize))
#define CALLOC_CLR(nbElem, elemSize, tag) (pcalloc(nbElem,  elemSize))
#endif

#ifdef PMEM_MAP_TRACE
/**
 * Portable realloc()
 */
#define REALLOC(ptr, newSize) (prealloc(ptr, newSize, L(__FILE__), __LINE__))
#else
/**
 * Portable realloc()
 */
#define REALLOC(ptr, newSize) (prealloc(ptr, newSize))
#endif

/**
 * Portable new()
 */
#define NEW(type, tag) ((type*) MALLOC(sizeof(type), tag))

/**
 * Allocates a new array
 */
#define NEW_ARRAY(type, nbElem, tag) ((type *) CALLOC(nbElem, sizeof(type), tag))

#ifdef PMEM_MAP_TRACE
/**
 * Portable free()
 */
#define FREE(ptr) pfree(ptr, L(__FILE__), __LINE__)
#else
/**
 * Portable free()
 */
#define FREE(ptr) pfree(ptr)
#endif

/**
 * @}
 */

/**
 * Allocates specified number of bytes, similar to malloc but initializes the
 * memory to 0.
 *
 * @param nbBytes The number of bytes to allocate.
 *
 * @param tag     The tag associated with the memory for reporting.
 *
 * @param file The file name in which the function is invoked.  Should be the
 * __FILE__ macro.
 *
 * @param line The line at which the function is invoked.  Should be the
 * __LINE__ macro.
 **/
#ifdef PMEM_MAP_TRACE
PORTABLE_API void *pmalloc(size_t nbBytes, const LCHAR* tag, const LCHAR* file, int line);
#else
PORTABLE_API void *pmalloc(size_t nbBytes);
#endif

/**
 * Allocate an array of items, similar to calloc.
 *
 * @param nbItems  The number items to allocate.
 *
 * @param itemSize The size of each item.
 *
 * @param tag      The tag associated with the memory for reporting.
 *
 * @param file The file name in which the function is invoked.  Should be the
 * __FILE__ macro.
 *
 * @param line The line at which the function is invoked.  Should be the
 * __LINE__ macro.
 *
 **/
#ifdef PMEM_MAP_TRACE
PORTABLE_API void *pcalloc(size_t nbItems, size_t itemSize, const LCHAR* tag, const LCHAR* file, int line);
#else
PORTABLE_API void *pcalloc(size_t nbItems, size_t itemSize);
#endif

/**
 * Reallocates data.  Similar to realloc.
 *
 * @param ptr A pointer previously allocated by pmalloc, pcalloc or prealloc.
 *
 * @param newSize The new size required.
 *
 * @param file The file name in which the function is invoked.  Should be the
 * __FILE__ macro.
 *
 * @param line The line at which the function is invoked.  Should be the
 * __LINE__ macro.
 *
 **/
#ifdef PMEM_MAP_TRACE
PORTABLE_API void *prealloc(void* ptr, size_t newSize, const LCHAR* file, int line);
#else
PORTABLE_API void *prealloc(void* ptr, size_t newSize);
#endif

/**
 * Frees data allocated through pmalloc, pcalloc or realloc.
 *
 * @param ptr A pointer previously allocated by pmalloc, pcalloc or prealloc.
 *
 * @param file The file name in which the function is invoked.  Should be the
 * __FILE__ macro.
 *
 * @param line The line at which the function is invoked.  Should be the
 * __LINE__ macro.
 *
 **/
#ifdef PMEM_MAP_TRACE
PORTABLE_API void pfree(void* ptr, const LCHAR* file, int line);
#else
PORTABLE_API void pfree(void* ptr);
#endif

/**
 * @addtogroup PmemoryModule PMemory API functions
 * Library for basic memory management.
 * Call PMemInit() to initialize and PmemShutdown() to shutdown module.
 *
 * @{
 */

/**
 * Initializes the memory management API.
 *
 * @return ESR_INVALID_STATE if the PMem module is already initialized or an internal error occurs
 */
PORTABLE_API ESR_ReturnCode PMemInit(void);

/**
 * Shutdowns the memory management API.  pmemReport is invoked with the same
 * file that was provided to pmemInit.
 *
 * @return ESR_INVALID_STATE if the PMem module is not initialized
 */
PORTABLE_API ESR_ReturnCode PMemShutdown(void);

/**
 * Enables low-level logging to file. This logs individual memory allocations and
 * deallocations. On shutdown, pmemDumpLogFile() will be invoked.
 *
 * @param file A file in which logging of memory related operations should be
 *    performed. If NULL, no logging is performed.
 * @return ESR_INVALID_STATE if the PMem module is not initialized
 */
PORTABLE_API ESR_ReturnCode PMemSetLogFile(PFile* file);

/**
 * Dumps memory report to the log file, closes it and disables logging.
 *
 * @return ESR_INVALID_STATE if the PMem module is not initialized or an internal error occurs
 */
PORTABLE_API ESR_ReturnCode PMemDumpLogFile(void);

/**
 * Enables/disables memory logging. This is useful for hiding allocations/deallocation
 * from pmemReport() and other reporting mechanisms, simply disable logging prior
 * to hidden operations and reenable it thereafter.
 *
 * @param value True if logging should be enabled
 * @return ESR_SUCCESS
 */
PORTABLE_API ESR_ReturnCode PMemSetLogEnabled(ESR_BOOL value);

/**
 * Hide memory allocation from pmemReport() by pretending the memory was deallocating. This is used to hide
 * memory leaks from pmemReport(), which is useful for internal variables which are deallocated after the
 * final call to pmemReport() occurs.
 *
 * @param ptr Address of memory allocation that should be hidden
 * @return ESR_SUCCESS
 */
PORTABLE_API ESR_ReturnCode PMemLogFree(void* ptr);

/**
 * Generates a report of the memory allocation.
 *
 * @param file A file in which the report is generated.  If set to NULL, the
 * report will be generated in the same file as that was provided to pmemInit.
 * Therefore, it is possible that no report is generated if the function is
 * invoked with NULL and pmemInit was also invoked with NULL.
 * @return ESR_WRITE_ERROR if an error occurs while writing to the file
 */
PORTABLE_API ESR_ReturnCode PMemReport(PFile* file);

/**
 * Allow user to set the memory pool size when S2G uses its own memory management.
 * It should be called before PMemInit()
 * The predefined (default) size is 3M for S2G
 *
 * @param size the memory pool size in byte
 * @return ESR_NOT_SUPPORTED if S2G uses native memory management; ESR_INVALID_STATE if it is called after PMemInit()
 */
PORTABLE_API ESR_ReturnCode PMemorySetPoolSize(size_t size);

/**
 * Get the memory pool size when S2G uses its own memory management
 *
 * @param size the memory pool size in byte
 * @return ESR_NOT_SUPPORTED if S2G uses native memory management
 */
PORTABLE_API ESR_ReturnCode PMemoryGetPoolSize(size_t *size);

/**
 * @}
 */

#endif

#endif
