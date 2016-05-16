/*---------------------------------------------------------------------------*
 *  ArrayListImpl.c  *
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



#include "ArrayList.h"
#include "ArrayListImpl.h"
#include "pmemory.h"

#define MTAG NULL
#define INITIAL_CAPACITY 16

ESR_ReturnCode ArrayListCreateWithCapacity(ArrayList **self, size_t minCapacity)
{
  ArrayListImpl* impl;
  
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
    
  impl = NEW(ArrayListImpl, MTAG);
  
  if (impl == NULL)
    return ESR_OUT_OF_MEMORY;
    
  impl->Interface.add = &ArrayList_Add;
  impl->Interface.insertAt = &ArrayList_InsertAt;
  impl->Interface.contains = &ArrayList_Contains;
  impl->Interface.destroy = &ArrayList_Destroy;
  impl->Interface.get = &ArrayList_Get;
  impl->Interface.getSize = &ArrayList_GetSize;
  impl->Interface.remove = &ArrayList_Remove;
  impl->Interface.removeAtIndex = &ArrayList_RemoveAtIndex;
  impl->Interface.removeAll = &ArrayList_RemoveAll;
  impl->Interface.set = &ArrayList_Set;
  impl->Interface.toStaticArray = NULL; /* Not implemented */
  impl->Interface.clone = &ArrayList_Clone;
  
  impl->contents = MALLOC(minCapacity * sizeof(void*), MTAG);
  if (impl->contents == NULL)
  {
    FREE(impl);
    return ESR_OUT_OF_MEMORY;
  }
  impl->capacity = minCapacity;
  impl->minCapacity = minCapacity;
  impl->size = 0;
  
  *self = (ArrayList*) impl;
  return ESR_SUCCESS;
}


ESR_ReturnCode ArrayListCreate(ArrayList** self)
{
  return ArrayListCreateWithCapacity(self, INITIAL_CAPACITY);
}

static ESR_ReturnCode ArrayList_Insert_Internal(ArrayListImpl *impl, size_t index, void *element)
{
  size_t i;
  
  if (impl->size >= impl->capacity)
  {
    /* enlarge buffer */
    size_t newCapacity = impl->capacity * 2;
    void** temp = REALLOC(impl->contents, newCapacity * sizeof(void*));
    if (temp == NULL)
      return ESR_OUT_OF_MEMORY;
    impl->contents = temp;
    impl->capacity = newCapacity;
  }
  
  for (i = impl->size; i > index; --i)
    impl->contents[i] = impl->contents[i - 1];
  ++impl->size;
  impl->contents[index] = element;
  return ESR_SUCCESS;
}

ESR_ReturnCode ArrayList_Add(ArrayList* self, void* element)
{
  ArrayListImpl *impl = (ArrayListImpl *) self;
  
  return ArrayList_Insert_Internal(impl, impl->size, element);
}

ESR_ReturnCode ArrayList_InsertAt(ArrayList *self, size_t index, void *element)
{
  ArrayListImpl *impl = (ArrayListImpl *) self;
  
  if (index > impl->size)
    return ESR_ARGUMENT_OUT_OF_BOUNDS;
    
  return ArrayList_Insert_Internal(impl, index, element);
}

static ESR_ReturnCode ArrayList_Remove_Internal(ArrayListImpl *impl, size_t i)
{
  --impl->size;
  while (i < impl->size)
  {
    impl->contents[i] = impl->contents[i+1];
    ++i;
  }
  
  if (impl->capacity > impl->minCapacity &&
      impl->size <= impl->capacity / 4)
  {
    void** temp;
    size_t newCapacity = impl->capacity / 2;
    
    /* shrink buffer */
    if ((temp = REALLOC(impl->contents, newCapacity * sizeof(void*))) == NULL)
      return ESR_OUT_OF_MEMORY;
    impl->contents = temp;
    impl->capacity = newCapacity;
  }
  return ESR_SUCCESS;
}

ESR_ReturnCode ArrayList_Remove(ArrayList* self, const void* element)
{
  ArrayListImpl* impl = (ArrayListImpl*) self;
  size_t i;
  
  /* Remove element */
  for (i = 0; i < impl->size; ++i)
  {
    if (impl->contents[i] == element)
      return ArrayList_Remove_Internal(impl, i);
  }
  
  return ESR_NO_MATCH_ERROR;
}

ESR_ReturnCode ArrayList_RemoveAtIndex(ArrayList* self, size_t index)
{
  ArrayListImpl* impl = (ArrayListImpl*) self;
  
  if (index >= impl->size)
    return ESR_ARGUMENT_OUT_OF_BOUNDS;
    
  return ArrayList_Remove_Internal(impl, index);
}

ESR_ReturnCode ArrayList_RemoveAll(ArrayList* self)
{
  ArrayListImpl* impl = (ArrayListImpl*) self;
  
  impl->size = 0;
  return ESR_SUCCESS;
}

ESR_ReturnCode ArrayList_Contains(ArrayList* self, const void* element,
                                  ESR_BOOL* exists)
{
  ArrayListImpl* impl = (ArrayListImpl*) self;
  size_t i;
  
  for (i = 0; i < impl->size; ++i)
  {
    if (impl->contents[i] == element)
    {
      *exists = ESR_TRUE;
      return ESR_SUCCESS;
    }
  }
  *exists = ESR_FALSE;
  return ESR_SUCCESS;
}

ESR_ReturnCode ArrayList_Get(ArrayList* self, size_t index, void** element)
{
  ArrayListImpl* impl = (ArrayListImpl*) self;
  
  if (index >= impl->size)
    return ESR_ARGUMENT_OUT_OF_BOUNDS;
  *element = impl->contents[index];
  return ESR_SUCCESS;
}

ESR_ReturnCode ArrayList_Set(ArrayList* self, size_t index, void* element)
{
  ArrayListImpl* impl = (ArrayListImpl*) self;
  
  if (index >= impl->size)
    return ESR_ARGUMENT_OUT_OF_BOUNDS;
  impl->contents[index] = element;
  return ESR_SUCCESS;
}

ESR_ReturnCode ArrayList_GetSize(ArrayList* self, size_t* size)
{
  ArrayListImpl* impl = (ArrayListImpl*) self;
  
  *size = impl->size;
  return ESR_SUCCESS;
}

ESR_ReturnCode ArrayList_Clone(ArrayList* self, ArrayList* clone)
{
  size_t size, i;
  void* element;
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

ESR_ReturnCode ArrayList_Destroy(ArrayList* self)
{
  ArrayListImpl* impl = (ArrayListImpl*) self;
  
  FREE(impl->contents);
  FREE(self);
  return ESR_SUCCESS;
}
