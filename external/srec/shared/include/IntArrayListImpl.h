/*---------------------------------------------------------------------------*
 *  IntArrayListImpl.h  *
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

#ifndef __INTARRAYLISTIMPL_H
#define __INTARRAYLISTIMPL_H



#include "ESR_ReturnCode.h"
#include "ESR_SharedPrefix.h"

/**
 * IntArrayList implementation.
 */
typedef struct IntArrayListImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  IntArrayList Interface;
  /**
   * IntArrayList contents.
   */
  int* contents;
  /**
   * Virtual number of allocated element slots.
   */
  size_t virtualSize;
  /**
   * Actual number of allocated element slots.
   */
  size_t actualSize;
}
IntArrayListImpl;


/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode IntArrayList_Add(IntArrayList* self, const int element);

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode IntArrayList_Remove(IntArrayList* self, const int element);

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode IntArrayList_RemoveAll(IntArrayList* self);

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode IntArrayList_Contains(IntArrayList* self, const int element, ESR_BOOL* exists);

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode IntArrayList_Get(IntArrayList* self, size_t index, int* element);

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode IntArrayList_Set(IntArrayList* self, size_t index, const int element);

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode IntArrayList_GetSize(IntArrayList* self, size_t* size);

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode IntArrayList_ToStaticArray(IntArrayList* self, int** newArray);

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode IntArrayList_Destroy(IntArrayList* self);

#endif /* __INTARRAYLIST_H */
