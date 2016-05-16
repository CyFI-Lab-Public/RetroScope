/*---------------------------------------------------------------------------*
 *  phashtable.c  *
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






#include <string.h>

#include "phashtable.h"
#include "plog.h"
#include "pmemory.h"
#include "pstdio.h"

//extern int strcmp(const char * s1,  const char * s2);

#define ALLOC_SIZE 16

struct PHashTableEntry_t
{
  const void *key;
  const void *value;
  PHashTable *table;
  unsigned int idx;
  PHashTableEntry *next;
  PHashTableEntry *prev;
  unsigned int hashCode;
};

typedef struct PHashTableEntryBlock_t
{
  PHashTableEntry entries[ALLOC_SIZE];
  struct PHashTableEntryBlock_t *next;
}
PHashTableEntryBlock;


struct PHashTable_t
{
  PHashTableArgs args;
  const LCHAR *memoryTag;
  unsigned int size;
  float maxLoadFactor;
  PHashTableEntry **entries;
  unsigned int threshold;
  PHashTableEntry *freeList;
  PHashTableEntryBlock *entryBlock;
};

#include "pcrc.h"

static unsigned int hashString(const void *key)
{
  return ~pcrcComputeString(key);
}

ESR_ReturnCode PHashTableCreate(PHashTableArgs *args,
                                const LCHAR *memTag,
                                PHashTable **table)
{
  PHashTable *tmp;
  unsigned int i;
  
  if (table == NULL ||
      (args != NULL && args->maxLoadFactor <= 0.0))
    return ESR_INVALID_ARGUMENT;
    
    
  if ((tmp = NEW(PHashTable, memTag)) == NULL)
    return ESR_OUT_OF_MEMORY;
    
  if (args == NULL)
  {
    tmp->args.capacity = PHASH_TABLE_DEFAULT_CAPACITY;
    tmp->args.maxLoadFactor = PHASH_TABLE_DEFAULT_MAX_LOAD_FACTOR;
    tmp->args.hashFunction = PHASH_TABLE_DEFAULT_HASH_FUNCTION;
    tmp->args.compFunction = PHASH_TABLE_DEFAULT_COMP_FUNCTION;
  }
  else
  {
    memcpy(&tmp->args, args, sizeof(PHashTableArgs));
  }
  if (tmp->args.hashFunction == PHASH_TABLE_DEFAULT_HASH_FUNCTION)
    tmp->args.hashFunction = hashString;
    
  if (tmp->args.compFunction == PHASH_TABLE_DEFAULT_COMP_FUNCTION)
    tmp->args.compFunction = LSTRCMP;
    
  tmp->entries = NEW_ARRAY(PHashTableEntry *, tmp->args.capacity, memTag);
  
  if (tmp->entries == NULL)
  {
    FREE(tmp);
    return ESR_OUT_OF_MEMORY;
  }
  
  for (i = tmp->args.capacity; i > 0;)
  {
    tmp->entries[--i] = NULL;
  }
  
  tmp->memoryTag = memTag;
  tmp->size = 0;
  tmp->threshold = (unsigned int)(tmp->args.capacity * tmp->args.maxLoadFactor);
  tmp->freeList = NULL;
  tmp->entryBlock = NULL;
  
  *table = tmp;
  return ESR_SUCCESS;
}

ESR_ReturnCode PHashTableDestroy(PHashTable *table)
{
  PHashTableEntryBlock *tmp, *block;
  
  if (table == NULL)
    return ESR_INVALID_ARGUMENT;
    
  block = table->entryBlock;
  while (block != NULL)
  {
    tmp = block->next;
    FREE(block);
    block = tmp;
  }
  
  FREE(table->entries);
  FREE(table);
  return ESR_SUCCESS;
}

ESR_ReturnCode PHashTableGetSize(PHashTable *table,
                                 size_t *size)
{
  if (table == NULL || size == NULL)
    return ESR_INVALID_ARGUMENT;
    
  *size = table->size;
  return ESR_SUCCESS;
}

static PHashTableEntry *getEntry(PHashTable *table,
                                 const void *key,
                                 unsigned int hashCode,
                                 unsigned int idx)
{
  PHashTableEntry *entry = table->entries[idx];
  
  if (key == NULL)
  {
    while (entry != NULL)
    {
      if (entry->key == NULL)
        return entry;
        
      entry = entry->next;
    }
  }
  else
  {
    while (entry != NULL)
    {
      if (entry->hashCode == hashCode && table->args.compFunction(key, entry->key) == 0)
        return entry;
        
      entry = entry->next;
    }
  }
  
  return NULL;
}

static void removeEntry(PHashTableEntry *entry)
{
  if (entry->prev == NULL)
    entry->table->entries[entry->idx] = entry->next;
  else
    entry->prev->next = entry->next;
    
  if (entry->next != NULL)
    entry->next->prev = entry->prev;
    
  entry->table->size--;
  
  entry->next = entry->table->freeList;
  entry->table->freeList = entry;
  /* clean up entry for re-use. */
  entry->key = entry->value = NULL;
}

ESR_ReturnCode PHashTableGetValue(PHashTable *table, const void *key, void **value)
{
  PHashTableEntry *entry;
  unsigned int hashCode;
  unsigned int idx;
  
  if (table == NULL || value == NULL)
    return ESR_INVALID_ARGUMENT;
    
  hashCode = table->args.hashFunction(key);
  idx = hashCode % table->args.capacity;
  if ((entry = getEntry(table, key, hashCode, idx)) != NULL)
  {
    *value = (void *) entry->value;
    return ESR_SUCCESS;
  }
  else
  {
    *value = NULL;
    return ESR_NO_MATCH_ERROR;
  }
}

ESR_ReturnCode PHashTableContainsKey(PHashTable *table, const void *key, ESR_BOOL* exists)
{
  ESR_ReturnCode rc;
  PHashTableEntry* entry;
  
  if (table == NULL || exists == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  rc = PHashTableGetEntry(table, key, &entry);
  if (rc == ESR_SUCCESS)
    *exists = ESR_TRUE;
  else if (rc == ESR_NO_MATCH_ERROR)
    *exists = ESR_FALSE;
  else
    goto CLEANUP;
    
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode PHashTableGetEntry(PHashTable *table, const void *key, PHashTableEntry **entry)
{
  unsigned int hashCode;
  unsigned int idx;
  PHashTableEntry* result;
  
  if (table == NULL || entry == NULL)
    return ESR_INVALID_ARGUMENT;
    
  hashCode = table->args.hashFunction(key);
  idx = hashCode % table->args.capacity;
  
  result = getEntry(table, key, hashCode, idx);
  if (result == NULL)
    return ESR_NO_MATCH_ERROR;
  *entry = result;
  return ESR_SUCCESS;
}

static ESR_ReturnCode PHashTableRehash(PHashTable *table)
{
  unsigned int i, idx;
  unsigned int oldCapacity = table->args.capacity;
  unsigned int newCapacity = ((oldCapacity << 1) | 0x01);
  PHashTableEntry *entry, *tmp, *next;
  
  PHashTableEntry **newEntries =
    (PHashTableEntry **)
    REALLOC(table->entries,
            sizeof(PHashTableEntry *) * newCapacity);
            
  if (newEntries == NULL)
    return ESR_OUT_OF_MEMORY;
    
  table->entries = newEntries;
  table->args.capacity = newCapacity;
  table->threshold = (unsigned int)(newCapacity * table->args.maxLoadFactor);
  
  for (i = oldCapacity; i < newCapacity; ++i)
  {
    table->entries[i] = NULL;
  }
  
  for (i = 0; i < oldCapacity; i++)
  {
    for (entry = table->entries[i]; entry != NULL;)
    {
      idx = entry->hashCode % newCapacity;
      if (idx != i)
      {
        /* Need to change location. */
        entry->idx = idx;
        
        next = entry->next;
        
        if (entry->prev != NULL)
          entry->prev->next = next;
        else
          table->entries[i] = next;
          
        if (next != NULL)
          next->prev = entry->prev;
          
        tmp = table->entries[idx];
        entry->next = tmp;
        entry->prev = NULL;
        if (tmp != NULL)
          tmp->prev = entry;
        table->entries[idx] = entry;
        
        entry = next;
      }
      else
      {
        /* Already in the right slot. */
        entry = entry->next;
      }
    }
  }
  return ESR_SUCCESS;
}


ESR_ReturnCode PHashTablePutValue(PHashTable *table,
                                  const void *key,
                                  const void *value,
                                  void **oldValue)
{
  ESR_ReturnCode rc = ESR_SUCCESS;
  unsigned int hashCode, idx;
  PHashTableEntry *entry;
  
  if (table == NULL) return ESR_INVALID_ARGUMENT;
  hashCode = table->args.hashFunction(key);
  idx = hashCode % table->args.capacity;
  
  entry = getEntry(table, key, hashCode, idx);
  if (entry != NULL)
  {
    if (oldValue != NULL) *oldValue = (void *) entry->value;
    entry->value = value;
    return ESR_SUCCESS;
  }
  
  /* If we get here, we need to add a new entry.  But first, verify if we need
     to rehash. */
  if (table->size >= table->threshold)
  {
    if ((rc = PHashTableRehash(table)) != ESR_SUCCESS)
      return rc;
    idx = hashCode % table->args.capacity;
  }
  
  if (table->freeList == NULL)
  {
    /* Allocate a new block and put all entries on the free list. */
    PHashTableEntryBlock *block;
    int i;
    
    block = NEW(PHashTableEntryBlock, table->memoryTag);
    if (block == NULL)
      return ESR_OUT_OF_MEMORY;
      
    block->next = table->entryBlock;
    table->entryBlock = block;
    
    for (i = 0; i < ALLOC_SIZE - 1; ++i)
    {
      block->entries[i].next = &block->entries[i+1];
    }
    block->entries[ALLOC_SIZE-1].next = NULL;
    
    /* do not see any bug in following code. But on the VxWorks with optimization option -O3
      it produces wrong result: block->entries[0].next is correct but block->entries[1].next = NULL
      it causes lot of memory wastes.
    for (i = 0, entry = block->entries; i < ALLOC_SIZE - 1; ++i, ++entry)
    {
      entry->next = entry+1;
    }
    entry->next = table->freeList;
    */
    
    table->freeList = block->entries;
  }
  
  /* Get an entry from the freeList. */
  entry = table->freeList;
  table->freeList = entry->next;
  
  /* Initialize entry data structure. */
  entry->table = table;
  entry->idx = idx;
  entry->key = key;
  entry->value = value;
  entry->hashCode = hashCode;
  entry->next = table->entries[idx];
  entry->prev = NULL;
  if (entry->next != NULL)
    entry->next->prev = entry;
  table->entries[idx] = entry;
  table->size++;
  
  if (oldValue != NULL) *oldValue = NULL;
  return ESR_SUCCESS;
}


ESR_ReturnCode PHashTableRemoveValue(PHashTable *table,
                                     const void *key,
                                     void **oldValue)
{
  unsigned int hashCode, idx;
  PHashTableEntry *entry;
  ESR_ReturnCode rc;
  
  if (table == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  
  hashCode = table->args.hashFunction(key);
  idx = hashCode % table->args.capacity;
  
  entry = getEntry(table, key, hashCode, idx);
  if (entry != NULL)
  {
    if (oldValue != NULL)
      *oldValue = (void*) entry->value;
    removeEntry(entry);
  }
  else
  {
    if (oldValue != NULL)
      *oldValue = NULL;
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode PHashTableEntryGetKeyValue(PHashTableEntry *entry,
    void **key,
    void **value)
{
  if (entry == NULL) return ESR_INVALID_ARGUMENT;
  
  if (key != NULL) *key = (void *) entry->key;
  if (value != NULL) *value = (void *) entry->value;
  return ESR_SUCCESS;
}


/**
 * Sets the value associated with this entry.
 * @param entry The hashtable entry.
 * @param value The value to associate with the entry.
 * @param oldvalue If this pointer is non-NULL, it will be set to the previous value associated with this entry.
 **/
ESR_ReturnCode PHashTableEntrySetValue(PHashTableEntry *entry,
                                       const void *value,
                                       void **oldValue)
{
  if (entry == NULL) return ESR_INVALID_ARGUMENT;
  
  if (oldValue != NULL) *oldValue = (void *) entry->value;
  entry->value = value;
  return ESR_SUCCESS;
}


/**
 * Removes the entry from its hash table.
 *
 * @param entry The hashtable entry.
 **/
ESR_ReturnCode PHashTableEntryRemove(PHashTableEntry *entry)
{
  if (entry == NULL)
    return ESR_INVALID_ARGUMENT;
    
  removeEntry(entry);
  
  return ESR_SUCCESS;
}

static PHashTableEntry* iteratorAdvance(PHashTable *table, PHashTableEntry *entry)
{
  unsigned int idx;
  
  if (entry != NULL)
  {
    idx = entry->idx;
    entry = entry->next;
    if (entry == NULL)
    {
      while (++idx < table->args.capacity)
      {
        if (table->entries[idx] != NULL)
        {
          entry = table->entries[idx];
          break;
        }
      }
    }
  }
  else
  {
    for (idx = 0; idx < table->args.capacity; ++idx)
    {
      if (table->entries[idx] != NULL)
      {
        entry = table->entries[idx];
        break;
      }
    }
  }
  return entry;
}


ESR_ReturnCode PHashTableEntryGetFirst(PHashTable *table, PHashTableEntry **entry)
{
  if (table == NULL || entry == NULL)
    return ESR_INVALID_ARGUMENT;
    
  *entry = iteratorAdvance(table, NULL);
  return ESR_SUCCESS;
}

/**
 * Iterates on the next key and value.  Returns a NULL key when at the end of the hash table.
 *
 * @param iter The iterator on which the iteration is performed.
 * @param key Returns the key associated with the entry, cannot be NULL.
 * @param value If non-NULL, returns the value associated with the entry.
 **/
ESR_ReturnCode PHashTableEntryAdvance(PHashTableEntry **entry)
{
  if (entry == NULL || *entry == NULL)
    return ESR_INVALID_ARGUMENT;
    
  *entry = iteratorAdvance((*entry)->table, *entry);
  return ESR_SUCCESS;
}
