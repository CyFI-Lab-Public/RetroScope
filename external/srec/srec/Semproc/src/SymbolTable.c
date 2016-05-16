/*---------------------------------------------------------------------------*
 *  SymbolTable.c  *
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

#include "SR_SymbolTable.h"
#include "plog.h"
#include "pmemory.h"


static const char* MTAG = __FILE__;

ESR_ReturnCode ST_Init(SymbolTable **ptr)
{
  ESR_ReturnCode rc;
  
  if (ptr == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  *ptr = NEW(SymbolTable, MTAG);
  
  if (*ptr == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }
  CHKLOG(rc, HashMapCreate(&(*ptr)->hashmap));
  
  (*ptr)->num_special_symbols = 0;
  
  /* init the memory for the hashtable */
  return ST_reset(*ptr);
CLEANUP:
  return rc;
}

ESR_ReturnCode ST_Free(SymbolTable *self)
{
  ESR_ReturnCode rc;
  
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  
  /* free all the slots that were used
     and remove all hashtable entries */
  ST_reset(self);
  
  /* delete the hash table */
  if (self->hashmap)
    CHKLOG(rc, HashMapDestroy(self->hashmap));
    
  /* delete the symbol table */
  if (self != NULL)
    FREE(self);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ST_putKeyValue(SymbolTable* self, const LCHAR* key, const LCHAR* value)
{
  Symbol* symbol;
  LCHAR* buf;
  ESR_ReturnCode rc;
  
  if (self == NULL || key == NULL || value == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  /* reuse the old entry if it exists
   but if no old entry exists for this key then I need to create a new one */
  rc = HashMapGet(self->hashmap, key, (void**) & buf);
  if (rc == ESR_NO_MATCH_ERROR)
  {
    CHKLOG(rc, ST_getSymbolSlot(self, &symbol));
    
    /* copy the key */
    MEMCHK(rc, LSTRLEN(key), MAX_SEMPROC_KEY);
    LSTRCPY(symbol->key, key);
    
    /* creates a new entry if it does not already exist */
    CHKLOG(rc, HashMapPut(self->hashmap, symbol->key, symbol->value));
    
    /* for later */
    buf = symbol->value;
  }
  else if (rc != ESR_SUCCESS)
    return rc;
    
  if (LSTRLEN(value) >= MAX_SEMPROC_VALUE)
    PLogError("Warning: chopping length of value len %d > %d (%s)\n", LSTRLEN(value), MAX_SEMPROC_VALUE, value);
  LSTRNCPY(buf, value, MAX_SEMPROC_VALUE);
  buf[MAX_SEMPROC_VALUE-1] = 0;
  /* MEMCHK(rc, LSTRLEN(value), MAX_SEMPROC_VALUE);
     LSTRCPY(buf, value); */
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ST_Copy(SymbolTable* self, HashMap* dst)
{
  static const LCHAR* _MTAG = L("semproc.st.copy");
  size_t i, size;
  LCHAR *pkey;
  LCHAR *pvalue;
  LCHAR *copyValue;
  
  if (!dst) return ESR_INVALID_ARGUMENT;
  
  HashMapGetSize(self->hashmap, &size);
  for (i = 0;i < size;i++)
  {
    HashMapGetKeyAtIndex(self->hashmap, i, &pkey);
    HashMapGet(self->hashmap, pkey, (void **)&pvalue);
    /* add one more space */
    copyValue = (LCHAR*) CALLOC(LSTRLEN(pvalue) + 1, sizeof(LCHAR), _MTAG);
    if (!copyValue)
    {
      PLogError(L("ESR_OUT_OF_MEMORY"));
      return ESR_OUT_OF_MEMORY;
    }
    LSTRCPY(copyValue, pvalue);
    HashMapPut(dst, pkey, copyValue);
  }
  return ESR_SUCCESS;
}

ESR_ReturnCode ST_getKeyValue(SymbolTable* self, const LCHAR* key, LCHAR** value)
{
  ESR_ReturnCode rc;
  LCHAR *dot;
  size_t i;
  
  if (self == NULL || key == NULL || value == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  
  rc = HashMapGet(self->hashmap, key, (void**)value);
  
  if (rc == ESR_SUCCESS || rc != ESR_NO_MATCH_ERROR)
    return rc;
    
  if (rc == ESR_NO_MATCH_ERROR)
  {
    /* handle SPECIAL CASEs */
    for (i = 0;i < self->num_special_symbols; i++)
    {
      /* try as is */
      if (!LSTRCMP(key, self->SpecialSymbols[i].key))
      {
        *value = self->SpecialSymbols[i].value;
        return ESR_SUCCESS;
      }
      
      /* try without dot */
      dot = LSTRCHR(key, L('.'));
      if (dot)
        key = ++dot;
        
      /* is it a match? */
      if (!LSTRCMP(key, self->SpecialSymbols[i].key))
      {
        *value = self->SpecialSymbols[i].value;
        return ESR_SUCCESS;
      }
    }
  }
  
  *value = UNDEFINED_SYMBOL;
  return ESR_SUCCESS;
}

ESR_ReturnCode ST_getSymbolSlot(SymbolTable* ptr, Symbol** slot)
{
  ESR_ReturnCode rc;
  
  if (ptr == NULL || slot == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  
  *slot = ptr->next++;
  MEMCHK(rc, ptr->next, &ptr->Symbols[MAX_SYMBOLS-1]);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ST_reset(SymbolTable *ptr)
{
  int i;
  ESR_ReturnCode rc;
  
  if (ptr == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  CHKLOG(rc, HashMapRemoveAll(ptr->hashmap));
  ptr->next = &ptr->Symbols[0];
  for (i = 0; i < MAX_SYMBOLS; i++)
  {
    ptr->Symbols[i].key[0] = 0;
    ptr->Symbols[i].value[0] = 0;
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ST_reset_all(SymbolTable *ptr)
{
  int i;
  ESR_ReturnCode rc;
  
  if (ptr == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  CHKLOG(rc, HashMapRemoveAll(ptr->hashmap));
  ptr->next = &ptr->Symbols[0
                           ];
  for (i = 0; i < MAX_SYMBOLS; i++)
  {
    ptr->Symbols[i].key[0] = 0;
    ptr->Symbols[i].value[0] = 0;
  }
  for (i = 0; i < MAX_SPECIAL_SYMBOLS; i++)
  {
    ptr->SpecialSymbols[i].key[0] = 0;
    ptr->SpecialSymbols[i].value[0] = 0;
  }
  ptr->num_special_symbols = 0;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode ST_putSpecialKeyValue(SymbolTable* self, const LCHAR* key, const LCHAR* value)
{
  size_t i;
  
  if (self == NULL || key == NULL || value == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  
  /* see if already there, and overwrite */
  for (i = 0;i < self->num_special_symbols;i++)
  {
    if (!LSTRCMP(self->SpecialSymbols[i].key, key))
    {
      LSTRCPY(self->SpecialSymbols[i].value, value);
      return ESR_SUCCESS;
    }
  }
  
  if (self->num_special_symbols < MAX_SPECIAL_SYMBOLS)
  {
    LSTRCPY(self->SpecialSymbols[self->num_special_symbols].key, key);
    LSTRCPY(self->SpecialSymbols[self->num_special_symbols].value, value);
    ++self->num_special_symbols;
    return ESR_SUCCESS;
  }
  PLogError(L("Semproc: Symbol table has too many special symbols"));
  return ESR_BUFFER_OVERFLOW;
}
