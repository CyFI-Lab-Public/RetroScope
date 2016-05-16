/*---------------------------------------------------------------------------*
 *  HashMapImpl.h  *
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

#ifndef __HASHMAPIMPL_H
#define __HASHMAPIMPL_H



#include <assert.h>
#include <stdlib.h>
#include "ESR_ReturnCode.h"
#include "phashtable.h"

/**
 * HashMap implementation.
 */
typedef struct HashMapImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  HashMap Interface;
  
  /**
   * Actual hash table implementation.
   **/
  PHashTable *table;
}
HashMapImpl;

/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode HashMap_Put(HashMap* self, const LCHAR* key, void* value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode HashMap_Remove(HashMap* self, const LCHAR* key);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode HashMap_RemoveAndFree(HashMap* self, const LCHAR* key);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode HashMap_RemoveAtIndex(HashMap* self, const size_t index);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode HashMap_RemoveAll(HashMap* self);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode HashMap_RemoveAndFreeAll(HashMap* self);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode HashMap_ContainsKey(HashMap* self, const LCHAR* key, ESR_BOOL* exists);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode HashMap_Get(HashMap* self, const LCHAR* key, void** value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode HashMap_GetKeyAtIndex(HashMap* self, const size_t index, LCHAR** key);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode HashMap_GetValueAtIndex(HashMap* self, const size_t index, void** value);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode HashMap_GetSize(HashMap* self, size_t* size);
/**
 * Default implementation.
 */
ESR_SHARED_API ESR_ReturnCode HashMap_Destroy(HashMap* self);

#endif /* __HASHMAPIMPL_H */
