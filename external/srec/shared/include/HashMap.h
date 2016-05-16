/*---------------------------------------------------------------------------*
 *  HashMap.h  *
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

#ifndef __HASHMAP_H
#define __HASHMAP_H



#include "ESR_ReturnCode.h"
#include "ESR_SharedPrefix.h"
#include "ptypes.h"
#include <stdlib.h>


/**
 * @addtogroup HashMapModule HashMap API functions
 * Hashed [key, value] mapping.
 *
 * @{
 */

/**
 * Hashed [key, value] mapping.
 */
typedef struct HashMap_t
{
  /**
   * Sets new mapping, storing a reference to the value.
   * The key can be safely deallocated after this operation.
   *
   * @param self HashMap handle
   * @param key Mapping key
   * @param value Mapping value
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode(*put)(struct HashMap_t* self, const LCHAR* key, void* value);
  
  /**
   * Removes the mapping for this key from this map if present.
   *
   * @param self HashMap handle
   * @param key Key whose mapping is to be removed from the map.
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*remove)(struct HashMap_t* self, const LCHAR* key);
  
  /**
   * Removes the mapping for this key from this map if present and frees the value.
   *
   * @param self HashMap handle
   * @param key Key whose mapping is to be removed from the map.
    * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*removeAndFree)(struct HashMap_t* self, const LCHAR* key);
  
  /**
   * Removes the mappings for the key at the specified index.
   *
   * @param self HashMap handle
   * @param index Index of element to be removed
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_ARGUMENT_OUT_OF_BOUNDS if index is out of bounds
   */
  ESR_ReturnCode(*removeAtIndex)(struct HashMap_t* self, const size_t index);
  
  /**
   * Removes all mappings from this map.
   *
   * @param self HashMap handle
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*removeAll)(struct HashMap_t* self);
  
  /**
   * Removes all mappings from this map and frees the values.
   *
   * @param self HashMap handle
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*removeAndFreeAll)(struct HashMap_t* self);
  
  /**
   * Indicates if element is contained within the list.
   *
   * @param self HashMap handle
   * @param key Key to check for
   * @param exists True if key was found
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*containsKey)(struct HashMap_t* self, const LCHAR* key, ESR_BOOL* exists);
  
  /**
   * Returns the number of mappings contained in this map.
   *
   * @param self HashMap handle
   * @param size Returned size
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*getSize)(struct HashMap_t* self, size_t* size);
  
  /**
   * Returns the value to which the specified key is mapped in this identity hash map,
   * or null if the map contains no mapping for this key.
   *
   * @param self HashMap handle
   * @param key the key whose associated value is to be returned.
   * @param value the value to which this map maps the specified key, or null if the
   *              map contains no mapping for this key.
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_NO_MATCH_ERROR if key cannot be found
   */
  ESR_ReturnCode(*get)(struct HashMap_t* self, const LCHAR* key, void** value);
  /**
   * Returns the key at the specified index.
   *
   * @param self HashMap handle
   * @param index the key index
   * @param key the key at the specified index
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_ARGUMENT_OUT_OF_BOUNDS if index is out of bounds
   */
  ESR_ReturnCode(*getKeyAtIndex)(struct HashMap_t* self, const size_t index, LCHAR** key);
  /**
   * Returns the value at the specified index.
   *
   * @param self HashMap handle
   * @param index the key index
   * @param value the value at the specified index
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_ARGUMENT_OUT_OF_BOUNDS if index is out of bounds
   */
  ESR_ReturnCode(*getValueAtIndex)(struct HashMap_t* self, const size_t index, void** value);
  /**
   * Destroys the HashMap.
   *
   * @param self HashMap handle
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*destroy)(struct HashMap_t* self);
}
HashMap;

/**
 * Creates a new HashMap.
 *
 * @param self HashMap handle
 * @return ESR_INVALID_ARGUMENT if self or the value it points to are null
 */
ESR_SHARED_API ESR_ReturnCode HashMapCreate(HashMap** self);
/**
 * Creates a new HashMap.
 *
 * @param bins The number of hashing bins to be used.
 * @param self HashMap handle
 * @return ESR_INVALID_ARGUMENT if self or the value it points to are null
 */
ESR_SHARED_API ESR_ReturnCode HashMapCreateBins(size_t nbBins, HashMap** self);
/**
 * Sets new mapping, storing a reference to the value.
 * The key can be safely deallocated after this operation.
 *
 * @param self HashMap handle
 * @param key Mapping key
 * @param value Mapping value
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
 */
ESR_SHARED_API ESR_ReturnCode HashMapPut(HashMap* self, const LCHAR* key, void* value);
/**
 * Removes the mapping for this key from this map if present.
 * The value can be safely deallocated after this operation.
 * If the map previously contained a mapping for this key, the old value is replaced,
 * but not deallocated.
 *
 * @param self HashMap handle
 * @param key Key whose mapping is to be removed from the map.
 * @return ESR_INVALID_ARGUMENT if self is null
 */
ESR_SHARED_API ESR_ReturnCode HashMapRemove(HashMap* self, const LCHAR* key);
/**
 * Removes the mapping for this key from this map if present and frees the value.
 * The value can be safely deallocated after this operation.
 *
 * @param self HashMap handle
 * @param key Key whose mapping is to be removed from the map.
 * @return ESR_INVALID_ARGUMENT if self is null
 */
ESR_SHARED_API ESR_ReturnCode HashMapRemoveAndFree(HashMap* self, const LCHAR* key);
/**
 * Removes the mappings for the key at the specified index.
 *
 * @param self HashMap handle
 * @param index Index of element to be removed
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_ARGUMENT_OUT_OF_BOUNDS if index is out of bounds
 */
ESR_SHARED_API ESR_ReturnCode HashMapRemoveAtIndex(HashMap* self, const size_t index);
/**
 * Removes all mappings from this map.
 *
 * @param self HashMap handle
 * @return ESR_INVALID_ARGUMENT if self is null
 */
ESR_SHARED_API ESR_ReturnCode HashMapRemoveAll(HashMap* self);
/**
 * Removes all mappings from this map and frees the values.
 *
 * @param self HashMap handle
 * @return ESR_INVALID_ARGUMENT if self is null
 */
ESR_SHARED_API ESR_ReturnCode HashMapRemoveAndFreeAll(HashMap* self);
/**
 * Indicates if element is contained within the list.
 *
 * @param self HashMap handle
 * @param key Key to check for
 * @param exists True if key was found
 * @return ESR_INVALID_ARGUMENT if self is null
 */
ESR_SHARED_API ESR_ReturnCode HashMapContainsKey(HashMap* self, const LCHAR* key, ESR_BOOL* exists);
/**
 * Returns the number of mappings contained in this map.
 *
 * @param self HashMap handle
 * @param size Returned size
 * @return ESR_INVALID_ARGUMENT if self is null
 */
ESR_SHARED_API ESR_ReturnCode HashMapGetSize(HashMap* self, size_t* size);
/**
 * Returns the value to which the specified key is mapped in this identity hash map,
 * or null if the map contains no mapping for this key.
 *
 * @param self HashMap handle
 * @param key the key whose associated value is to be returned.
 * @param value the value to which this map maps the specified key, or null if the
 *              map contains no mapping for this key.
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_NO_MATCH_ERROR if key cannot be found
 */
ESR_SHARED_API ESR_ReturnCode HashMapGet(HashMap* self, const LCHAR* key, void** value);
/**
 * Returns the key at the specified index.
 *
 * @param self HashMap handle
 * @param index the key index
 * @param key the key at the specified index
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_ARGUMENT_OUT_OF_BOUNDS if index is out of bounds
 */
ESR_SHARED_API ESR_ReturnCode HashMapGetKeyAtIndex(HashMap* self, const size_t index, LCHAR** key);
/**
 * Returns the value at the specified index.
 *
 * @param self HashMap handle
 * @param index the key index
 * @param value the key at the specified index
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_ARGUMENT_OUT_OF_BOUNDS if index is out of bounds
 */
ESR_SHARED_API ESR_ReturnCode HashMapGetValueAtIndex(HashMap* self, const size_t index, void** value);
/**
 * Destroys an HashMap.
 *
 * @param self HashMap handle
 * @return ESR_INVALID_ARGUMENT if self is null
 */
ESR_SHARED_API ESR_ReturnCode HashMapDestroy(HashMap* self);

/**
 * @}
 */


#endif /* __HASHMAP_H */
