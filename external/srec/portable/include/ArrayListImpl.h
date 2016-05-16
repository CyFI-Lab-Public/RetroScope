/*---------------------------------------------------------------------------*
 *  ArrayListImpl.h  *
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

#ifndef __ARRAYLISTIMPL_H
#define __ARRAYLISTIMPL_H



#include "ESR_ReturnCode.h"
#include "PortPrefix.h"

/**
 * ArrayList implementation.
 */
typedef struct ArrayListImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  ArrayList Interface;
  
  /**
   * ArrayList contents.
   *
   * Represents an array of void* elements. An element having a value of NULL denotes an
   * empty slot.
   */
  void** contents;
  
  /**
   * number element in the array.
   */
  size_t size;
  
  /**
   * Actual capacity of the array.
   */
  size_t capacity;
  
  /**
   * Min capacity of the array.
   **/
  size_t minCapacity;
  
}
ArrayListImpl;


/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode ArrayList_Add(ArrayList* self, void* element);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode ArrayList_InsertAt(ArrayList* self, size_t index, void* element);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode ArrayList_Remove(ArrayList* self, const void* element);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode ArrayList_RemoveAtIndex(ArrayList* self, size_t index);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode ArrayList_RemoveAll(ArrayList* self);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode ArrayList_Contains(ArrayList* self, const void* element, ESR_BOOL* exists);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode ArrayList_Get(ArrayList* self, size_t index, void** element);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode ArrayList_Set(ArrayList* self, size_t index, void* element);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode ArrayList_GetSize(ArrayList* self, size_t* size);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode ArrayList_Clone(ArrayList* self, ArrayList* clone);

/**
 * Default implementation.
 */
PORTABLE_API ESR_ReturnCode ArrayList_Destroy(ArrayList* self);

#endif /* __ARRAYLIST_H */
