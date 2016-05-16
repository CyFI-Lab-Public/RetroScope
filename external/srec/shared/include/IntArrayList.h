/*---------------------------------------------------------------------------*
 *  IntArrayList.h  *
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

#ifndef __INTARRAYLIST_H
#define __INTARRAYLIST_H



#include "ESR_ReturnCode.h"
#include "ESR_SharedPrefix.h"
#include "ptypes.h"
#include <stdlib.h>

/**
 * @addtogroup IntArrayListModule IntArrayList API functions
 * List of elements.
 *
 * @{
 */

/**
 * List of elements.
 */
typedef struct IntArrayList_t
{
  /**
   * Adds element to list.
   *
   * @param self IntArrayList handle
   * @param element Element to be added
   */
  ESR_ReturnCode(*add)(struct IntArrayList_t* self, int element);
  
  /**
  * Removes element from list.
  *
  * @param self IntArrayList handle
  * @param element Element to be removed
  */
  ESR_ReturnCode(*remove)(struct IntArrayList_t* self, int element);
  
  /**
  * Removes all elements from list.
  *
  * @param self IntArrayList handle
  */
  ESR_ReturnCode(*removeAll)(struct IntArrayList_t* self);
  
  /**
  * Indicates if element is contained within the list.
  *
  * @param self IntArrayList handle
  * @param element Element to check for
  * @param exists True if element was found
  */
  ESR_ReturnCode(*contains)(struct IntArrayList_t* self, int element, ESR_BOOL* exists);
  
  /**
  * Returns array size.
  *
  * @param self IntArrayList handle
  * @param size Returned size
  */
  ESR_ReturnCode(*getSize)(struct IntArrayList_t* self, size_t* size);
  
  /**
  * Returns the element at the specified index.
  *
  * @param self IntArrayList handle
  * @param index Element index
  * @param element Element being returned
  */
  ESR_ReturnCode(*get)(struct IntArrayList_t* self, size_t index, int* element);
  
  /**
  * Sets the element at the specified index.
  *
  * NOTE: Does *not* deallocate the element being overwritten.
  * @param self IntArrayList handle
  * @param index Element index
  * @param element Element's new value
  */
  ESR_ReturnCode(*set)(struct IntArrayList_t* self, size_t index, int element);
  
  /**
   * Converts the IntArrayList to a static array.
   * The use of the IntArrayList handle is undefined past this point.
   *
   * @param self IntArrayList handle
   * @param newArray Pointer to resulting array
   */
  ESR_ReturnCode(*toStaticArray)(struct IntArrayList_t* self, int** newArray);
  
  /**
  * Destroys the IntArrayList.
  * @param self IntArrayList handle
  */
  ESR_ReturnCode(*destroy)(struct IntArrayList_t* self);
}
IntArrayList;

/**
 * Creates a new IntArrayList.
 *
 * @param self ArrayList handle
 */
ESR_SHARED_API ESR_ReturnCode IntArrayListCreate(IntArrayList** self);

/**
 * Creates a new IntArrayList from the supplied static array.
 * The static array may not be used past this point.
 *
 * @param value Initial value
 * @param self IntArrayList handle
 */
ESR_SHARED_API ESR_ReturnCode IntArrayListImport(int* value, IntArrayList** self);

/**
 * Adds element to list.
 *
 * @param self IntArrayList handle
 * @param element Element to be added
 */
ESR_SHARED_API ESR_ReturnCode IntArrayListAdd(IntArrayList* self, int element);

/**
 * Removes element from list.
 *
 * @param self IntArrayList handle
 * @param element Element to be removed
 */
ESR_SHARED_API ESR_ReturnCode IntArrayListRemove(IntArrayList* self, int element);

/**
 * Removes all elements from list.
 *
 * @param self IntArrayList handle
 */
ESR_SHARED_API ESR_ReturnCode IntArrayListRemoveAll(IntArrayList* self);

/**
 * Indicates if element is contained within the list.
 *
 * @param self IntArrayList handle
 * @param element Element to check for
 * @param exists True if element was found
 */
ESR_SHARED_API ESR_ReturnCode IntArrayListContains(IntArrayList* self, int element, ESR_BOOL* exists);

/**
 * Returns array size.
 *
 * @param self IntArrayList handle
 * @param size Returned size
 */
ESR_SHARED_API ESR_ReturnCode IntArrayListGetSize(IntArrayList* self, size_t* size);

/**
 * Returns the element at the specified index.
 *
 * @param self IntArrayList handle
 * @param index Element index
 * @param element Element being returned
 */
ESR_SHARED_API ESR_ReturnCode IntArrayListGet(IntArrayList* self, size_t index, int* element);

/**
 * Sets the element at the specified index.
 *
 * NOTE: Does *not* deallocate the element being overwritten.
 * @param self IntArrayList handle
 * @param index Element index
 * @param element Element's new value
 */
ESR_SHARED_API ESR_ReturnCode IntArrayListSet(IntArrayList* self, size_t index, int element);

/**
 * Converts the IntArrayList to a static array.
 * The IntArrayList handle may not be used past this point.
 *
 * @param self IntArrayList handle
 * @param newArray Pointer to resulting array
 */
ESR_SHARED_API ESR_ReturnCode IntArrayListToStaticArray(IntArrayList* self, int** newArray);

/**
 * Destroys an IntArrayList.
 *
 * @param self IntArrayList handle
 */
ESR_SHARED_API ESR_ReturnCode IntArrayListDestroy(IntArrayList* self);

/**
 * @}
 */


#endif /* __INTARRAYLIST_H */
