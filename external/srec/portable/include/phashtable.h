/*---------------------------------------------------------------------------*
 *  phashtable.h  *
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

#ifndef PHASHTABLE_H
#define PHASHTABLE_H



#include "PortPrefix.h"
#include "ptypes.h"
#include "ESR_ReturnCode.h"

/**
 * The default initial capacity of a hash table.
 */
#define PHASH_TABLE_DEFAULT_CAPACITY 11

/**
 *
 * The default maximum load factor
 */
#define PHASH_TABLE_DEFAULT_MAX_LOAD_FACTOR (0.75f)

/**
 * Default hash function used for hashing keys.  The default function assumes
 * the key is a 0-terminated LSTRING.
 */
#define PHASH_TABLE_DEFAULT_HASH_FUNCTION NULL

/**
 * Default compare function used for hashing keys.  The default function
 * assumes the key are 0-terminated LSTRING and uses LSTRCMP.
 */
#define PHASH_TABLE_DEFAULT_COMP_FUNCTION NULL

/**
 * @addtogroup HashTableModule HashTable API functions
 * Abstract hash table operations.  The keys of the Map are strings and values
 * are plain void pointers.  The keys and values are only stored as pointers
 * and it is the responsibility of the user to ensure proper memory management
 * for the keys and values.
 *
 * The HashTable is implemented using an array of linked lists.  The capacity
 * of the HashTable is the number of entries in this array.  The load factor
 * of the HashTable is the ratio of the total number of entries in the table
 * vs the capacity of the table.  The lower the load factor, the faster the
 * look-up is.  However, a lower load factor calls for a bigger capacity,
 * hence it increases the memory requirement.
 *
 * When the load factor exceeds the maximum load factor, the capacity of the
 * hash table is increased and each entry is put in its new linked list based
 * on the new capacity.
 *
 * @{
 */

/**
 * Signature for hashing functions.
 */
typedef unsigned int(*PHashFunction)(const void *key);

/**
 * Signature for comparison functions.  Must return 0 if key1 is identical to
 * key2 and non-zero otherwise.  The hash function and the comparison function
 * are related in the sense that if the comparison function for two keys
 * return 0, then the values returned by the hash function when given these
 * keys as arguments must be equal.
 */
typedef int (*PHashCompFunction)(const LCHAR *key1, const LCHAR *key2);

/** Typedef */
typedef struct PHashTable_t PHashTable;
/** Typedef */
typedef struct PHashTableEntry_t PHashTableEntry;

/**
 * Structure specified to specify initialization parameters for the hash
 * table.
 */
typedef struct PHashTableArgs_t
{
  /**
   * Total capacity.
   */
  size_t capacity;
  
  /**
   * Maximum load-factor before hashtable is rehashed.
   */
  float maxLoadFactor;
  
  /**
   * Hashing function used to compute the hashcode of a key.
   */
  PHashFunction hashFunction;
  
  /**
   * Function used to compare two keys.
   */
  PHashCompFunction compFunction;
}
PHashTableArgs;

/**
 * Creates an hash table.  The hash table is created with specified capacity
 * and maximum load factor.
 *
 * @param hashArgs Specifies the arguments controlling the hashtable.  If
 * NULL, all arguments are assumed to be the default value.  This value is
 * copied. This is the responsibility of the caller to delete the
 * HashTableArgs if required.
 *
 * @param memTag Memory tag to be used for the internal memory allocation
 * calls.  Since this string is used by the memory allocation tag, it is not
 * copied internally and it must remain valid for the lifetime of the hash
 * table including the call to the HashTableDestroy function.  Most likely,
 * this string is a static string or is allocated from the stack.
 *
 * @param hashtable A pointer to the returned hash table. This parameter may
 * not be NULL.
 * @return ESR_INVALID_ARGUMENT if hashArgs, or hashTable is null or
 * hashArgs->maxLoadFactor <= 0; ESR_OUT_OF_MEMORY if system is out of memory
 */
PORTABLE_API ESR_ReturnCode PHashTableCreate(PHashTableArgs *hashArgs,
    const LCHAR *memTag,
    PHashTable **hashtable);
    
/**
 * Destructor.  The keys and values need to be deleted (if necessary) before
 * deleting the table to avoid memory leak.
 *
 * @param ESR_INVALID_ARGUMENT if hashtable is null
 */
PORTABLE_API ESR_ReturnCode PHashTableDestroy(PHashTable *hashtable);

/**
 * Retrieves the size (number of entries) of the hashtable.
 *
 * @return ESR_INVALID_ARGUMENT if hashtable or size is null
 */
PORTABLE_API ESR_ReturnCode PHashTableGetSize(PHashTable *hashtable,
    size_t *size);
    
    
/**
 * Retrieves the value associated with a key.
 *
 * @param hashtable The hashtable
 * @param key The key for which to retrieve the value.
 * @param value The value associated with the key.
 * @return If no match, ESR_NO_MATCH_ERROR is returned.
 */
PORTABLE_API ESR_ReturnCode PHashTableGetValue(PHashTable *hashtable,
    const void *key, void **value);
    
/**
 * Indicates if hashtable contains the specified key.
 *
 * @param hashtable The hashtable
 * @param key The key for which to retrieve the value.
 * @param exists [out] True if the key was found
 * @return ESR_INVALID_ARGUMENT if self is null
 */
PORTABLE_API ESR_ReturnCode PHashTableContainsKey(PHashTable *hashtable,
    const void *key, ESR_BOOL* exists);
/**
 * Associates a value with a key.
 *
 * @param hashtable The hashtable
 *
 * @param key The key to associate a value with.
 *
 * @param value The value to associate with a key.
 *
 * @param oldValue If this pointer is non-NULL, it will be set to the
 * value previously associated with the key.
 * @return ESR_INVALID_STATE if hashtable is null
 */
PORTABLE_API ESR_ReturnCode PHashTablePutValue(PHashTable *hashtable,
    const void *key,
    const void *value,
    void **oldValue);
    
/**
 * Removes the value with associated with a key.  Note that calling this
 * function might cause a leak in the event that the key needs to be deleted.
 * In those situations, use PHashTableGetEntry, then retrieve the key by
 * PHashTableEntryGetKeyValue, destroy the key and value and then use
 * PHashTableEntryRemove.
 *
 * @param hashtable The hashtable
 * @param key The key for which to remove the associated value.
 * @param oldValue If this pointer is non-NULL, it will be set to the value
 * previously associated with the key and that was removed.
 * @return ESR_INVALID_ARGUMENT if hashtable is null
 */
PORTABLE_API ESR_ReturnCode PHashTableRemoveValue(PHashTable *hashtable,
    const void *key,
    void **oldValue);
    
/**
 * Retrieves the hash entry corresponding to the key.
 *
 * @param hashtable The hashtable
 * @param key The key for which to retrieve the hash entry.
 * @param entry The entry associated with the key. Cannot be NULL.
 * @return If no match, ESR_NO_MATCH_ERROR is returned.
 */
PORTABLE_API ESR_ReturnCode PHashTableGetEntry(PHashTable *hashtable,
    const void *key,
    PHashTableEntry **entry);
    
/**
 * Returns the key and value associated with this entry.  Both key and values
 * can be deleted after removing the entry from the table.
 *
 * @param entry The hashtable entry
 * @param key If non-NULL, returns the key associated with the entry.
 * @param value If non-NULL, returns the value associated with the entry.
 * @return ESR_INVALID_ARGUMENT if entry is null
 */
PORTABLE_API ESR_ReturnCode PHashTableEntryGetKeyValue(PHashTableEntry *entry,
    void **key,
    void **value);
    
/**
 * Sets the value associated with this entry.
 *
 * @param entry The hashtable entry.
 * @param value The value to associate with the entry.
 * @param oldValue If this pointer is non-NULL, it will be set to the value
 * previously associated with this entry.
 */
PORTABLE_API ESR_ReturnCode PHashTableEntrySetValue(PHashTableEntry *entry,
    const void *value,
    void **oldValue);
    
/**
 * Removes the entry from its hash table.
 *
 * POST-CONDITION: 'entry' variable is invalid
 *
 * @param entry The hashtable entry.
 * @return ESR_INVALID_ARGUMENT if entry is null
 */
PORTABLE_API ESR_ReturnCode PHashTableEntryRemove(PHashTableEntry *entry);

/**
 * Resets the iterator at the beginning.
 */
PORTABLE_API
ESR_ReturnCode PHashTableEntryGetFirst(PHashTable *table,
                                       PHashTableEntry **entry);
                                       
/**
 * Advance to the next entry in the hash table.
 *
 * @param entry the current entry.
 * @return ESR_INVALID_ARGUMENT if entry or the value it points to is null.
 */
PORTABLE_API ESR_ReturnCode PHashTableEntryAdvance(PHashTableEntry** entry);

/**
 * @}
 */

#endif
