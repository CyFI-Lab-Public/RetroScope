/*---------------------------------------------------------------------------*
 *  Int8ArrayList.h  *
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

#ifndef __INT8ARRAYLIST_H
#define __INT8ARRAYLIST_H



#include "ESR_ReturnCode.h"
#include "ESR_SharedPrefix.h"
#include "ptypes.h"
#include <stdlib.h>

/**
 * @addtogroup Int8ArrayListModule Int8ArrayList API functions
 * List of Int8 elements.
 *
 * @{
 */

/**
 * List of elements.
 */
typedef struct Int8ArrayList_t
{
  /**
   * Adds element to list.
   *
   * @param self Int8ArrayList handle
   * @param element Element to be added
   */
  ESR_ReturnCode(*add)(struct Int8ArrayList_t* self, asr_int8_t element);
  
  /**
  * Removes element from list.
  *
  * @param self Int8ArrayList handle
  * @param element Element to be removed
  */
  ESR_ReturnCode(*remove)(struct Int8ArrayList_t* self, asr_int8_t element);
  
  /**
  * Removes all elements from list.
  *
  * @param self Int8ArrayList handle
  */
  ESR_ReturnCode(*removeAll)(struct Int8ArrayList_t* self);
  
  /**
  * Indicates if element is contained within the list.
  *
  * @param self Int8ArrayList handle
  * @param element Element to check for
  * @param exists True if element was found
  */
  ESR_ReturnCode(*contains)(struct Int8ArrayList_t* self, asr_int8_t element, ESR_BOOL* exists);
  
  /**
  * Returns array size.
  *
  * @param self Int8ArrayList handle
  * @param size Returned size
  */
  ESR_ReturnCode(*getSize)(struct Int8ArrayList_t* self, size_t* size);
  
  /**
  * Returns the element at the specified index.
  *
  * @param self Int8ArrayList handle
  * @param index Element index
  * @param element Element being returned
  */
  ESR_ReturnCode(*get)(struct Int8ArrayList_t* self, size_t index, asr_int8_t* element);
  
  /**
  * Sets the element at the specified index.
  *
  * NOTE: Does *not* deallocate the element being overwritten.
  * @param self Int8ArrayList handle
  * @param index Element index
  * @param element Element's new value
  */
  ESR_ReturnCode(*set)(struct Int8ArrayList_t* self, size_t index, asr_int8_t element);
  
  /**
  * Returns a clone of the Int8ArrayList.
  * @param self Int8ArrayList handle
   * @param clone [out] Clone of the Int8ArrayList (created externally, populated 
   *                    internally)
  */
  ESR_ReturnCode(*clone)(struct Int8ArrayList_t* self, struct Int8ArrayList_t* clone);
  
  /**
   * Converts the Int8ArrayList to a static array.
   * The use of the Int8ArrayList handle is undefined past this point.
   *
   * @param self Int8ArrayList handle
   * @param newArray Pointer to resulting array
   */
  ESR_ReturnCode(*toStaticArray)(struct Int8ArrayList_t* self, asr_int8_t** newArray);
  
  /**
  * Destroys the Int8ArrayList.
  * @param self Int8ArrayList handle
  */
  ESR_ReturnCode(*destroy)(struct Int8ArrayList_t* self);
}
Int8ArrayList;

/**
 * Creates a new Int8ArrayList.
 *
 * @param self ArrayList handle
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayListCreate(Int8ArrayList** self);

/**
 * Creates a new Int8ArrayList from the supplied static array.
 * The static array may not be used past this point.
 *
 * @param value Initial value
 * @param self Int8ArrayList handle
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayListImport(asr_int8_t* value, Int8ArrayList** self);

/**
 * Adds element to list.
 *
 * @param self Int8ArrayList handle
 * @param element Element to be added
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayListAdd(Int8ArrayList* self, asr_int8_t element);

/**
 * Removes element from list.
 *
 * @param self Int8ArrayList handle
 * @param element Element to be removed
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayListRemove(Int8ArrayList* self, asr_int8_t element);

/**
 * Removes all elements from list.
 *
 * @param self Int8ArrayList handle
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayListRemoveAll(Int8ArrayList* self);

/**
 * Indicates if element is contained within the list.
 *
 * @param self Int8ArrayList handle
 * @param element Element to check for
 * @param exists True if element was found
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayListContains(Int8ArrayList* self, asr_int8_t element, ESR_BOOL* exists);

/**
 * Returns array size.
 *
 * @param self Int8ArrayList handle
 * @param size Returned size
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayListGetSize(Int8ArrayList* self, size_t* size);

/**
 * Returns the element at the specified index.
 *
 * @param self Int8ArrayList handle
 * @param index Element index
 * @param element Element being returned
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayListGet(Int8ArrayList* self, size_t index, asr_int8_t* element);

/**
 * Sets the element at the specified index.
 *
 * NOTE: Does *not* deallocate the element being overwritten.
 * @param self Int8ArrayList handle
 * @param index Element index
 * @param element Element's new value
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayListSet(Int8ArrayList* self, size_t index, asr_int8_t element);

/**
 * Converts the Int8ArrayList to a static array.
 * The Int8ArrayList handle may not be used past this point.
 *
 * @param self Int8ArrayList handle
 * @param newArray Pointer to resulting array
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayListToStaticArray(Int8ArrayList* self, asr_int8_t** newArray);

/**
 * Returns a clone of the Int8ArrayList.
 * @param self Int8ArrayList handle
 * @param clone [out] Clone of the Int8ArrayList (created externally, populated
 *                    internally)
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayListClone(Int8ArrayList* self, Int8ArrayList* clone);

/**
 * Destroys an Int8ArrayList.
 *
 * @param self Int8ArrayList handle
 */
ESR_SHARED_API ESR_ReturnCode Int8ArrayListDestroy(Int8ArrayList* self);

/**
 * @}
 */


#endif /* __INT8ARRAYLIST_H */
