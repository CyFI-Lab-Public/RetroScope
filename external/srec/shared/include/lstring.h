/*---------------------------------------------------------------------------*
 *  lstring.h  *
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

#ifndef __LSTRING_H
#define __LSTRING_H



#include "ESR_ReturnCode.h"
#include "ESR_SharedPrefix.h"
#include "ptypes.h"

/**
 * @addtogroup LStringModule LString API functions
 *
 * @{
 */

/**
 * LString interface.
 *
 * @see list of functions used to operate on @ref LStringModule "LString" objects
 */
typedef struct LString_t
{
  /**
   * Appends text to LString.
   *
   * @param self LString handle
   * @param value Value to append.
   */
  ESR_ReturnCode(*append)(struct LString_t* self, const LCHAR* value);
  /**
   * Clears the string contents.
   *
   * @param self LString handle
   */
  ESR_ReturnCode(*reset)(struct LString_t* self);
  /**
   * Destroys LString in favour of static LCHAR* string.
   * The LString object should not be used past this point.
   *
   * @param self LString handle
   * @param result Resulting LCHAR*
   */
  ESR_ReturnCode(*toLCHAR)(struct LString_t* self, LCHAR** result);
  /**
   * Destroys LString object.
   *
   * @param self LString handle
   */
  ESR_ReturnCode(*destroy)(struct LString_t* self);
}
LString;



/**
 * Creates new LString.
 *
 * @param self LString handle
 */
ESR_SHARED_API ESR_ReturnCode LStringCreate(LString** self);
/**
 * Appends text to LString.
 *
 * @param self LString handle
 * @param value Value to append.
 */
ESR_SHARED_API ESR_ReturnCode LStringAppend(LString* self, const LCHAR* value);
/**
 * Clears the string contents.
 *
 * @param self LString handle
 */
ESR_SHARED_API ESR_ReturnCode LStringReset(LString* self);
/**
 * Destroys LString in favour of static LCHAR* string.
 * The LString object should not be used past this point.
 *
 * @param self LString handle
 * @param result Resulting LCHAR*
 */
ESR_SHARED_API ESR_ReturnCode LStringToLCHAR(LString* self, LCHAR** result);
/**
 * Destroys LString object.
 *
 * @param self LString handle
 */
ESR_SHARED_API ESR_ReturnCode LStringDestroy(LString* self);

/**
 * @}
 */


#endif /* __LSTRING_H */
