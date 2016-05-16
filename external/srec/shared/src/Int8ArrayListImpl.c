/*---------------------------------------------------------------------------*
 *  Int8ArrayListImpl.c  *
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

#include "Int8ArrayList.h"
#include "Int8ArrayListImpl.h"
#include "pmemory.h"
#include "passert.h"

#define MTAG NULL
#define INITIAL_SIZE 32


ESR_ReturnCode Int8ArrayListCreate(Int8ArrayList** self)
{
  Int8ArrayListImpl* impl;
  
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  impl = NEW(Int8ArrayListImpl, MTAG);
  if (impl == NULL)
    return ESR_OUT_OF_MEMORY;
  impl->Interface.add = &Int8ArrayList_Add;
  impl->Interface.contains = &Int8ArrayList_Contains;
  impl->Interface.destroy = &Int8ArrayList_Destroy;
  impl->Interface.get = &Int8ArrayList_Get;
  impl->Interface.getSize = &Int8ArrayList_GetSize;
  impl->Interface.remove = &Int8ArrayList_Remove;
  impl->Interface.removeAll = &Int8ArrayList_RemoveAll;
  impl->Interface.set = &Int8ArrayList_Set;
  impl->Interface.toStaticArray = &Int8ArrayList_ToStaticArray;
  impl->Interface.clone = &Int8ArrayList_Clone;
  impl->contents = MALLOC((INITIAL_SIZE + 1) * sizeof(asr_int8_t), MTAG);
  if (impl->contents == NULL)
  {
    FREE(impl);
    return ESR_OUT_OF_MEMORY;
  }
  impl->actualSize = INITIAL_SIZE;
  impl->virtualSize = 0;
  *self = (Int8ArrayList*) impl;
  return ESR_SUCCESS;
}

ESR_ReturnCode Int8ArrayListImport(asr_int8_t* value, Int8ArrayList** self)
{
  ESR_ReturnCode rc;
  Int8ArrayListImpl* impl;
  
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  CHK(rc, Int8ArrayListCreate(self));
  impl = (Int8ArrayListImpl*) self;
  impl->contents = value;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode Int8ArrayList_Add(Int8ArrayList* self, const asr_int8_t element)
{
  Int8ArrayListImpl* impl = (Int8ArrayListImpl*) self;
  
  if (impl->virtualSize >= impl->actualSize)
  {
    /* enlarge buffer */
    asr_int8_t* temp = REALLOC(impl->contents, (impl->actualSize * 2 + 1) * sizeof(asr_int8_t));
    if (temp == NULL)
      return ESR_OUT_OF_MEMORY;
    impl->contents = temp;
    impl->actualSize *= 2;
  }
  impl->contents[impl->virtualSize] = element;
  ++impl->virtualSize;
  return ESR_SUCCESS;
}

ESR_ReturnCode Int8ArrayList_Remove(Int8ArrayList* self, const asr_int8_t element)
{
  Int8ArrayListImpl* impl = (Int8ArrayListImpl*) self;
  asr_int8_t* contents = impl->contents; /* cache pointer */
  size_t virtualSize = impl->virtualSize; /* cache value */
  size_t i;
  
  for (i = 0; i < virtualSize; ++i)
  {
    if (contents[i] == element)
    {
      --virtualSize;
      break;
    }
  }
  /* shift remaining elements back */
  for (; i < virtualSize; ++i)
    contents[i] = contents[i+1];
    
  impl->virtualSize = virtualSize; /* flush cache */
  if (virtualSize <= impl->actualSize / 4)
  {
    /* shrink buffer */
    impl->contents = REALLOC(contents, (impl->actualSize / 2 + 1) * sizeof(asr_int8_t));
    passert(impl->contents != NULL); /* should never fail */
    impl->actualSize /= 2;
  }
  return ESR_SUCCESS;
}

ESR_ReturnCode Int8ArrayList_RemoveAll(Int8ArrayList* self)
{
  Int8ArrayListImpl* impl = (Int8ArrayListImpl*) self;
  
  impl->virtualSize = 0;
  return ESR_SUCCESS;
}

ESR_ReturnCode Int8ArrayList_Contains(Int8ArrayList* self, const asr_int8_t element, ESR_BOOL* exists)
{
  Int8ArrayListImpl* impl = (Int8ArrayListImpl*) self;
  size_t i;
  size_t virtualSize = impl->virtualSize; /* cache value */
  asr_int8_t* contents = impl->contents; /* cache value */
  
  for (i = 0; i < virtualSize; ++i)
  {
    if (contents[i] == element)
    {
      *exists = ESR_TRUE;
      return ESR_SUCCESS;
    }
  }
  *exists = ESR_FALSE;
  return ESR_SUCCESS;
}

ESR_ReturnCode Int8ArrayList_Get(Int8ArrayList* self, size_t index, asr_int8_t* element)
{
  Int8ArrayListImpl* impl = (Int8ArrayListImpl*) self;
  
  passert(index >= 0 && index <= impl->virtualSize);
  *element = impl->contents[index];
  return ESR_SUCCESS;
}

ESR_ReturnCode Int8ArrayList_Set(Int8ArrayList* self, size_t index, const asr_int8_t element)
{
  Int8ArrayListImpl* impl = (Int8ArrayListImpl*) self;
  
  passert(index >= 0 && index <= impl->virtualSize);
  impl->contents[index] = element;
  return ESR_SUCCESS;
}

ESR_ReturnCode Int8ArrayList_GetSize(Int8ArrayList* self, size_t* size)
{
  Int8ArrayListImpl* impl = (Int8ArrayListImpl*) self;
  
  *size = impl->virtualSize;
  return ESR_SUCCESS;
}

ESR_ReturnCode Int8ArrayList_ToStaticArray(Int8ArrayList* self, asr_int8_t** newArray)
{
  Int8ArrayListImpl* impl = (Int8ArrayListImpl*) self;
  
  *newArray = impl->contents;
  impl->contents = NULL; /* prevent free() from deallocating buffer */
  return Int8ArrayList_Destroy(self);
}

ESR_ReturnCode Int8ArrayList_Clone(Int8ArrayList* self, Int8ArrayList* clone)
{
  size_t size, i;
  asr_int8_t element;
  ESR_ReturnCode rc;
  
  CHK(rc, clone->removeAll(clone));
  CHK(rc, self->getSize(self, &size));
  for (i = 0; i < size; ++i)
  {
    CHK(rc, self->get(self, i, &element));
    CHK(rc, clone->add(clone, element));
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode Int8ArrayList_Destroy(Int8ArrayList* self)
{
  Int8ArrayListImpl* impl = (Int8ArrayListImpl*) self;
  
  FREE(impl->contents);
  FREE(impl);
  return ESR_SUCCESS;
}
