/*---------------------------------------------------------------------------*
 *  ArrayList.h  *
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

#ifndef __ARRAYLIST_H
#define __ARRAYLIST_H



#include "ESR_ReturnCode.h"
#include "PortPrefix.h"
#include "ptypes.h"
#include <stdlib.h>

/**
 * @addtogroup ArrayListModule ArrayList API functions
 * Collection of elements.
 *
 * @{
 */

/**
 * Collection of elements.
 */
typedef struct ArrayList_t
{
  /**
   * Adds element to list.
   *
   * @param self ArrayList handle
   * @param element Element to be added
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY is system is out of memory
   */
  ESR_ReturnCode(*add)(struct ArrayList_t* self, void* element);
  
  /**
   * Inserts an element in the the list at the specified location.  This
   * causes all elements above or at the specified location to be shifted by
   * one.
   *
   * @param self ArrayList handle
   * @param index  The index where to insert the element.
   * @param element The element to insert.
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_SUCCESS if success or anaother value indicating
  * the nature of the error. In particular, it returns ESR_ARGUMENT_OUT_OF_BOUNDS if index
   * is less than 0 or greater than the array's size.
   */
  ESR_ReturnCode(*insertAt)(struct ArrayList_t* self, size_t index,
                            void *element);
                            
  /**
  * Removes element from list.
  *
  * @param self ArrayList handle
  * @param element Element to be removed
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY is system is out of memory
  */
  ESR_ReturnCode(*remove)(struct ArrayList_t* self, const void* element);
  
  /**
  * Removes element from list at specified index.
  *
  * @param self ArrayList handle
  * @param index Index of element to be removed
  * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY is system is out of memory;
  * ESR_ARGUMENT_OUT_OF_BOUNDS if index is out of bounds
  */
  ESR_ReturnCode(*removeAtIndex)(struct ArrayList_t* self, size_t index);
  
  /**
  * Removes all elements from list.
  *
  * @param self ArrayList handle
  * @return ESR_INVALID_ARGUMENT if self is null
  */
  ESR_ReturnCode(*removeAll)(struct ArrayList_t* self);
  
  /**
  * Indicates if element is contained within the list.
  *
  * @param self ArrayList handle
  * @param element Element to check for
  * @param exists True if element was found
  * @return ESR_INVALID_ARGUMENT if self is null
  */
  ESR_ReturnCode(*contains)(struct ArrayList_t* self, const void* element, ESR_BOOL* exists);
  
  /**
  * Returns array size.
  *
  * @param self ArrayList handle
  * @param size Returned size
  * @return ESR_INVALID_ARGUMENT if self is null
  */
  ESR_ReturnCode(*getSize)(struct ArrayList_t* self, size_t* size);
  
  /**
  * Returns the element at the specified index.
  *
  * @param self ArrayList handle
  * @param index Element index
  * @param element Element being returned
  * @return ESR_INVALID_ARGUMENT if self is null; ESR_ARGUMENT_OUT_OF_BOUNDS if index is out of bounds
  */
  ESR_ReturnCode(*get)(struct ArrayList_t* self, size_t index, void** element);
  
  /**
  * Sets the element at the specified index.
  *
  * NOTE: Does *not* deallocate the element being overwritten.
  * @param self ArrayList handle
  * @param index Element index
  * @param element Element's new value
  * @return ESR_INVALID_ARGUMENT if self is null; ESR_ARGUMENT_OUT_OF_BOUNDS if index is out of bounds
  */
  ESR_ReturnCode(*set)(struct ArrayList_t* self, size_t index, void* element);
  
  /**
   * Converts the ArrayList to a static array.
   * The use of the ArrayList handle is undefined past this point.
   *
   * @param self ArrayList handle
   * @param newArray Pointer to resulting array
  * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*toStaticArray)(struct ArrayList_t* self, void** newArray);
  
  /**
  * Returns a clone of the ArrayList.
  *
  * @param self ArrayList handle
   * @param clone [out] Clone of the ArrayList (created externally, populated internally)
  * @return ESR_INVALID_ARGUMENT if self is null; ESR_ARGUMENT_OUT_OF_BOUNDS if index (used internally) is out of bounds
  * ESR_OUT_OF_MEMORY is system is out of memory
  */
  ESR_ReturnCode(*clone)(struct ArrayList_t* self, struct ArrayList_t* clone);
  
  /**
  * Destroys the ArrayList.
  *
  * @param self ArrayList handle
  * @return ESR_INVALID_ARGUMENT if self is null
  */
  ESR_ReturnCode(*destroy)(struct ArrayList_t* self);
}
ArrayList;

/**
 * Creates a new ArrayList.
 *
 * @param self ArrayList handle
 * @return ESR_INVALID_ARGUMENT if self or the value it points to are null; ESR_OUT_OF_MEMORY is system is out of memory
 */
PORTABLE_API ESR_ReturnCode ArrayListCreate(ArrayList** self);

/**
 * Creates a new ArrayList with minimum capacity.
 *
 * @param self ArrayList handle
 * @param minCapacity Minimum capacity of the array.
 * @return ESR_INVALID_ARGUMENT if self or the value it points to are null; ESR_OUT_OF_MEMORY is system is out of memory
 */
PORTABLE_API ESR_ReturnCode ArrayListCreateWithCapacity(ArrayList** self, size_t minCapacity);

/**
 * Adds element to list.
 *
 * @param self ArrayList handle
 * @param element Element to be added
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY is system is out of memory
 */
PORTABLE_API ESR_ReturnCode ArrayListAdd(ArrayList* self, void* element);


/**
 * Inserts an element in the the list at the specified location.  This
 * causes all elements above or at the specified location to be shifted by
 * one.
 *
 * @param self ArrayList handle
 * @param index  The index where to insert the element.
 * @param element The element to insert.
 *
 * @return ESR_SUCCESS if success or anaother value indicating the nature of
 * the error.  In particular, it returns ESR_ARGUMENT_OUT_OF_BOUNDS if index
 * is less than 0 or greater than the array's size.
 */
PORTABLE_API ESR_ReturnCode ArrayListInsertAt(ArrayList* self,
    size_t index,
    void *element);
    
/**
 * Removes element from list.
 *
 * @param self ArrayList handle
 * @param element Element to be removed
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY is system is out of memory
 */
PORTABLE_API ESR_ReturnCode ArrayListRemove(ArrayList* self, void* element);
/**
 * Removes element from list at specified index.
 *
 * @param self ArrayList handle
 * @param index Index of element to be removed
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY is system is out of memory;
 * ESR_ARGUMENT_OUT_OF_BOUNDS if index is out of bounds
 */
PORTABLE_API ESR_ReturnCode ArrayListRemoveAtIndex(ArrayList* self, size_t index);

/**
 * Removes all elements from list.
 *
 * @param self ArrayList handle
 * @return ESR_INVALID_ARGUMENT if self is null
 */
PORTABLE_API ESR_ReturnCode ArrayListRemoveAll(ArrayList* self);

/**
 * Indicates if element is contained within the list.
 *
 * @param self ArrayList handle
 * @param element Element to check for
 * @param exists True if element was found
 * @return ESR_INVALID_ARGUMENT if self is null
 */
PORTABLE_API ESR_ReturnCode ArrayListContains(ArrayList* self, void* element, ESR_BOOL* exists);

/**
 * Returns array size.
 *
 * @param self ArrayList handle
 * @param size Returned size
 * @return ESR_INVALID_ARGUMENT if self is null
 */
PORTABLE_API ESR_ReturnCode ArrayListGetSize(ArrayList* self, size_t* size);

/**
 * Returns the element at the specified index.
 *
 * @param self ArrayList handle
 * @param index Element index
 * @param element Element being returned
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_ARGUMENT_OUT_OF_BOUNDS if index is out of bounds
 */
PORTABLE_API ESR_ReturnCode ArrayListGet(ArrayList* self, size_t index, void** element);

/**
 * Sets the element at the specified index.
 *
 * NOTE: Does *not* deallocate the element being overwritten.
 * @param self ArrayList handle
 * @param index Element index
 * @param element Element's new value
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_ARGUMENT_OUT_OF_BOUNDS if index is out of bounds
 */
PORTABLE_API ESR_ReturnCode ArrayListSet(ArrayList* self, size_t index, void* element);

/**
 * Returns a clone of the ArrayList.
 *
 * @param self ArrayList handle
 * @param clone [out] Clone of the ArrayList (created externally, populated internally)
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_ARGUMENT_OUT_OF_BOUNDS if index (used internally) is out of bounds
 * ESR_OUT_OF_MEMORY is system is out of memory
 */
PORTABLE_API ESR_ReturnCode ArrayListClone(ArrayList* self, ArrayList* clone);

/**
 * Destroys an ArrayList.
 *
 * @param self ArrayList handle
 * @return ESR_INVALID_ARGUMENT if self is null
 */
PORTABLE_API ESR_ReturnCode ArrayListDestroy(ArrayList* self);

/**
 * @}
 */

#endif /* __ARRAYLIST_H */
