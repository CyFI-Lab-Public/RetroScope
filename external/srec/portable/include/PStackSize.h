/*---------------------------------------------------------------------------*
 *  PStackSize.h  *
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

#ifndef PSTACKSIZE_H
#define PSTACKSIZE_H

#include "PortPrefix.h"



/**
 * Initialize the stack base.  This should be done in the main() function.
 * Note that the local variables of main() are not taken into account in the
 * stack size computation.  To overcome this problem, rewrite main() as a
 * simple function that first invokes PSTACK_SIZE_INIT and then invokes the
 * original main().
 */
PORTABLE_API void PSTACK_SIZE_INIT();

/**
 * Computes the current stack size.  The returned value is the number of bytes
 * in the stack.
 */
PORTABLE_API size_t PSTACK_SIZE_GET();

#endif
