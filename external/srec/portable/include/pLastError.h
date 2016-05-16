/*---------------------------------------------------------------------------*
 *  pLastError.h  *
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

#ifndef __PLASTERROR_H
#define __PLASTERROR_H

#include "LCHAR.h"
#include "PortPrefix.h"

#define printGetLastError(text) \
  printGetLastErrorInternal(text, __FILE__, __LINE__)

/**
 * Retrieves a verbose error message associated with the result of GetLastError() and outputs it
 * using PLogError (if available) or alternatively to PSTDERR.
 *
 * @param prefix A message to prefix the error message with.
 */
PORTABLE_API void printGetLastErrorInternal(const LCHAR* text, char* file, int line);

#endif /* __PLASTERROR_H */
