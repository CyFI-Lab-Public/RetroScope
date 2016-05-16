/*---------------------------------------------------------------------------*
 *  HashMapImpl.c  *
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


#include "HashMap.h"
#include "HashMapImpl.h"
#include "plog.h"
#include "pmemory.h"
#include "string.h"

#define MTAG NULL

static ESR_ReturnCode HashMapCreate_Internal(PHashTableArgs *hashArgs,
    HashMap **self)
{
  HashMapImpl* impl;
  ESR_ReturnCode rc = ESR_SUCCESS;
  
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  impl = NEW(HashMapImpl, MTAG);
  if (impl == NULL)
    return ESR_OUT_OF_MEMORY;
    
  if ((rc = PHashTableCreate(hashArgs, MTAG, &impl->table)) != ESR_SUCCESS)
  {
    FREE(impl);
    return rc;
  }
  
  impl->Interface.put = &HashMap_Put;
  impl->Interface.remove = &HashMap_Remove;
  impl->Interface.removeAndFree = &HashMap_RemoveAndFree;
  impl->Interface.removeAll = &HashMap_RemoveAll;
  impl->Interface.removeAndFreeAll = &HashMap_RemoveAndFreeAll;
  impl->Interface.removeAtIndex = &HashMap_RemoveAtIndex;
  impl->Interface.containsKey = &HashMap_ContainsKey;
  impl->Interface.getKeyAtIndex = &HashMap_GetKeyAtIndex;
  impl->Interface.get = &HashMap_Get;
  impl->Interface.getValueAtIndex = &HashMap_GetValueAtIndex;
  impl->Interface.getSize = &HashMap_GetSize;
  impl->Interface.destroy = &HashMap_Destroy;
  
  *self = (HashMap*) impl;
  return ESR_SUCCESS;
}

ESR_ReturnCode HashMapCreate(HashMap** self)
{
  return HashMapCreate_Internal(NULL, self);
}

ESR_ReturnCode HashMapCreateBins(size_t nbBins, HashMap** self)
{
  PHashTableArgs hashArgs;
  hashArgs.capacity = nbBins;
  hashArgs.maxLoadFactor = PHASH_TABLE_DEFAULT_MAX_LOAD_FACTOR;
  hashArgs.hashFunction = PHASH_TABLE_DEFAULT_HASH_FUNCTION;
  hashArgs.compFunction = PHASH_TABLE_DEFAULT_COMP_FUNCTION;
  return HashMapCreate_Internal(&hashArgs, self);
}

ESR_ReturnCode HashMap_Put(HashMap* self, const LCHAR* key, void* value)
{
  HashMapImpl* impl = (HashMapImpl*) self;
  PHashTableEntry *entry = NULL;
  ESR_ReturnCode rc;
  ESR_BOOL exists;
  
  CHKLOG(rc, PHashTableContainsKey(impl->table, key, &exists));
  if (!exists)
  {
    /* Not found, clone the key and insert it. */
    LCHAR *clone = (LCHAR *) MALLOC(sizeof(LCHAR) * (LSTRLEN(key) + 1), MTAG);
    if (clone == NULL) return ESR_OUT_OF_MEMORY;
    LSTRCPY(clone, key);
    if ((rc = PHashTablePutValue(impl->table, clone, value, NULL)) != ESR_SUCCESS)
    {
      FREE(clone);
    }
  }
  else
  {
    /* Key already present in table, just change the value. */
    CHKLOG(rc, PHashTableGetEntry(impl->table, key, &entry));
    rc = PHashTableEntrySetValue(entry, value, NULL);
  }
  return rc;
CLEANUP:
  return rc;
}

static ESR_ReturnCode HashMap_Remove_Internal(HashMapImpl* impl, const LCHAR* key, ESR_BOOL freeValue)
{
  PHashTableEntry *entry = NULL;
  ESR_ReturnCode rc = ESR_SUCCESS;
  LCHAR *clonedKey = NULL;
  void *value = NULL;
  
  CHK(rc, PHashTableGetEntry(impl->table, key, &entry));
  CHK(rc, PHashTableEntryGetKeyValue(entry, (void **)&clonedKey, (void **)&value));
  
  if (clonedKey)
    FREE(clonedKey);
  if (freeValue && value)
    FREE(value);
    
  return PHashTableEntryRemove(entry);
CLEANUP:
  return rc;
}

ESR_ReturnCode HashMap_Remove(HashMap* self, const LCHAR* key)
{
  return HashMap_Remove_Internal((HashMapImpl*) self, key, ESR_FALSE);
}

ESR_ReturnCode HashMap_RemoveAndFree(HashMap* self, const LCHAR* key)
{
  return HashMap_Remove_Internal((HashMapImpl*) self, key, ESR_TRUE);
}

static ESR_ReturnCode HashMap_RemoveAll_Internal(HashMapImpl *impl, ESR_BOOL freeValues)
{
  PHashTableEntry *entry1 = NULL;
  PHashTableEntry *entry2 = NULL;
  
  ESR_ReturnCode rc = ESR_SUCCESS;
  LCHAR *key = NULL;
  void *value = NULL;
  
  if ((rc = PHashTableEntryGetFirst(impl->table, &entry1)) != ESR_SUCCESS)
    goto end;
    
  while (entry1 != NULL)
  {
    if ((rc = PHashTableEntryGetKeyValue(entry1, (void **)&key, (void **)&value)) != ESR_SUCCESS)
      goto end;
    if (key) FREE(key);
    if (freeValues && value) FREE(value);
    entry2 = entry1;
    if ((rc = PHashTableEntryAdvance(&entry1)) != ESR_SUCCESS)
      goto end;
    if ((rc =  PHashTableEntryRemove(entry2)) != ESR_SUCCESS)
      goto end;
  }
end:
  return rc;
}

ESR_ReturnCode HashMap_RemoveAll(HashMap* self)
{
  return HashMap_RemoveAll_Internal((HashMapImpl *) self, ESR_FALSE);
}

ESR_ReturnCode HashMap_RemoveAndFreeAll(HashMap* self)
{
  return HashMap_RemoveAll_Internal((HashMapImpl *) self, ESR_TRUE);
}

ESR_ReturnCode HashMap_ContainsKey(HashMap* self, const LCHAR* key, ESR_BOOL* exists)
{
  HashMapImpl* impl = (HashMapImpl*) self;
  ESR_ReturnCode rc = ESR_SUCCESS;
  
  CHKLOG(rc, PHashTableContainsKey(impl->table, key, exists));
  return rc;
CLEANUP:
  return rc;
}

ESR_ReturnCode HashMap_Get(HashMap* self, const LCHAR* key, void** value)
{
  HashMapImpl* impl = (HashMapImpl*) self;
  PHashTableEntry *entry = NULL;
  ESR_ReturnCode rc = ESR_SUCCESS;
  
  CHK(rc, PHashTableGetEntry(impl->table, key, &entry));
  CHK(rc, PHashTableEntryGetKeyValue(entry, (void **)NULL, (void **)value));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

static ESR_ReturnCode HashMap_GetEntryAtIndex(HashMapImpl *impl, const size_t index,
    PHashTableEntry **entry)
{
  ESR_ReturnCode rc = ESR_SUCCESS;
  size_t i = 0;
  
  if ((rc = PHashTableEntryGetFirst(impl->table, entry)) != ESR_SUCCESS)
    goto end;
    
  while (*entry != NULL && i < index)
  {
    ++i;
    if ((rc = PHashTableEntryAdvance(entry)) != ESR_SUCCESS)
      goto end;
  }
  if (*entry == NULL)
    rc = ESR_ARGUMENT_OUT_OF_BOUNDS;
end:
  return rc;
}


ESR_ReturnCode HashMap_GetKeyAtIndex(HashMap* self, const size_t index, LCHAR** key)
{
  HashMapImpl* impl = (HashMapImpl*) self;
  PHashTableEntry *entry = NULL;
  ESR_ReturnCode rc;
  
  if ((rc = HashMap_GetEntryAtIndex(impl, index, &entry)) != ESR_SUCCESS)
    goto end;
    
  rc = PHashTableEntryGetKeyValue(entry, (void **) key, (void **) NULL);
  
end:
  return rc;
}

ESR_ReturnCode HashMap_GetValueAtIndex(HashMap* self, const size_t index, void** value)
{
  HashMapImpl* impl = (HashMapImpl*) self;
  PHashTableEntry *entry = NULL;
  ESR_ReturnCode rc;
  
  if ((rc = HashMap_GetEntryAtIndex(impl, index, &entry)) != ESR_SUCCESS)
    goto end;
    
  rc = PHashTableEntryGetKeyValue(entry, (void **)NULL, (void **)value);
  
end:
  return rc;
}

ESR_ReturnCode HashMap_RemoveAtIndex(HashMap* self, const size_t index)
{
  HashMapImpl* impl = (HashMapImpl*) self;
  PHashTableEntry *entry = NULL;
  ESR_ReturnCode rc;
  void *key;
  
  if ((rc = HashMap_GetEntryAtIndex(impl, index, &entry)) != ESR_SUCCESS)
    goto end;
    
  if ((rc = PHashTableEntryGetKeyValue(entry, (void **)&key, (void **)NULL)) != ESR_SUCCESS)
    goto end;
    
  if (key != NULL) FREE(key);
  
  rc = PHashTableEntryRemove(entry);
  
end:
  return rc;
}

ESR_ReturnCode HashMap_GetSize(HashMap* self, size_t* size)
{
  HashMapImpl* impl = (HashMapImpl*) self;
  return PHashTableGetSize(impl->table, size);
}

ESR_ReturnCode HashMap_Destroy(HashMap* self)
{
  HashMapImpl* impl = (HashMapImpl*) self;
  ESR_ReturnCode rc = ESR_SUCCESS;
  
  if ((rc = self->removeAll(self)) != ESR_SUCCESS)
    goto end;
    
  if (impl->table != NULL)
    rc = PHashTableDestroy(impl->table);
  FREE(impl);
end:
  return rc;
}
