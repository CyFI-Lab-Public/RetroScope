/*---------------------------------------------------------------------------*
 *  PStackTrace.h  *
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

#ifndef __PSTACKTRACE_H
#define __PSTACKTRACE_H



#include "ESR_ReturnCode.h"
#include "PortPrefix.h"

/**
 * @addtogroup PStackTraceModule PStackTrace API functions
 * Manipulates process stack-trace information.
 *
 * @{
 */

/**
 * Maximum length of stack-trace string.
 */
#define P_MAX_STACKTRACE 4096

/**
 * Maximum length of function name.
 */
#define P_MAX_FUNCTION_NAME 80

/**
 * Creates a new StackTrace object.
 *
 * @return ESR_INVALID_STATE if 1) module is already in the middle of being initialized
 * 2) module has already been initialized by another process 3) module fails to get the current directory
 * 4) module cannot retrieve the name of the current executing module (DLL/EXE)
 * 5) ESR_BUFFER_OVERFLOW if an internal buffer is too small 6) module cannot retrieve the current executing module
 * listing 7) module cannot retrieve the symbol table 8) module cannot create a new monitor
 */
PORTABLE_API ESR_ReturnCode PStackTraceCreate(void);

/**
 * Indicates if the PStackTrace module is initialized.
 *
 * @param value True if the module is initialized
 * @return ESR_INVALID_ARGUMENT is value is null; ESR_FATAL_ERROR if locking an internal mutex fails
 */
PORTABLE_API ESR_ReturnCode PStackTraceIsInitialized(ESR_BOOL* value);

/**
 * Returns the depth of the current stack trace (0-based).
 *
 * @param depth [out] The depth
 * @return ESR_NOT_SUPPORTED if debug symbols are missing
 */
PORTABLE_API ESR_ReturnCode PStackTraceGetDepth(size_t* depth);

/**
 * Returns the current process stack-track.
 *
 * @param text [out] The resulting stack-trace text
 * @param len [in/out] Size of text argument. If the return code is ESR_BUFFER_OVERFLOW,
 *            the required length is returned in this variable.
 * @return ESR_NOT_SUPPORTED if debug symbols are missing
 */
PORTABLE_API ESR_ReturnCode PStackTraceGetValue(LCHAR* text, size_t* len);

/**
 * Returns the current function name.
 *
 * @param text [out] The resulting function text
 * @param len [in/out] Size of text argument. If the return code is ESR_BUFFER_OVERFLOW,
 *            the required length is returned in this variable.
 * @return ESR_NOT_SUPPORTED if debug symbols are missing
 */
PORTABLE_API ESR_ReturnCode PStackTraceGetFunctionName(LCHAR* text, size_t* len);

/**
 * Removes the deepest level of the stack-trace.
 *
 * @param text [in/out] The stack-trace string.
 * @return ESR_SUCCESS
 */
PORTABLE_API ESR_ReturnCode PStackTracePopLevel(LCHAR* text);

/**
 * Destroys the stack trace object.
 *
 * @return ESR_SUCCESS
 */
PORTABLE_API ESR_ReturnCode PStackTraceDestroy(void);

/**
 * @}
 */

#endif /* __PSTACKTRACE_H */
