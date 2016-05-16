/*---------------------------------------------------------------------------*
 *  IntArrayListImpl.c  *
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

#include "IntArrayList.h"
#include "IntArrayListImpl.h"
#include "pmemory.h"
#include "passert.h"

#define MTAG NULL
#define INITIAL_SIZE 32


ESR_ReturnCode IntArrayListCreate(IntArrayList** self)
{
  IntArrayListImpl* impl;
  
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  impl = NEW(IntArrayListImpl, MTAG);
  if (impl == NULL)
    return ESR_OUT_OF_MEMORY;
  impl->Interface.add = &IntArrayList_Add;
  impl->Interface.contains = &IntArrayList_Contains;
  impl->Interface.destroy = &IntArrayList_Destroy;
  impl->Interface.get = &IntArrayList_Get;
  impl->Interface.getSize = &IntArrayList_GetSize;
  impl->Interface.remove = &IntArrayList_Remove;
  impl->Interface.removeAll = &IntArrayList_RemoveAll;
  impl->Interface.set = &IntArrayList_Set;
  impl->Interface.toStaticArray = &IntArrayList_ToStaticArray;
  impl->contents = MALLOC((INITIAL_SIZE + 1) * sizeof(int), MTAG);
  if (impl->contents == NULL)
  {
    FREE(impl);
    return ESR_OUT_OF_MEMORY;
  }
  impl->actualSize = INITIAL_SIZE;
  impl->virtualSize = 0;
  *self = (IntArrayList*) impl;
  return ESR_SUCCESS;
}

ESR_ReturnCode IntArrayListImport(int* value, IntArrayList** self)
{
  ESR_ReturnCode rc;
  IntArrayListImpl* impl;
  
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  CHK(rc, IntArrayListCreate(self));
  impl = (IntArrayListImpl*) self;
  impl->contents = value;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode IntArrayList_Add(IntArrayList* self, const int element)
{
  IntArrayListImpl* impl = (IntArrayListImpl*) self;
  
  if (impl->virtualSize >= impl->actualSize)
  {
    /* enlarge buffer */
    int* temp = REALLOC(impl->contents, (impl->actualSize * 2 + 1) * sizeof(int));
    if (temp == NULL)
      return ESR_OUT_OF_MEMORY;
    impl->contents = temp;
    impl->actualSize *= 2;
  }
  impl->contents[impl->virtualSize] = element;
  ++impl->virtualSize;
  return ESR_SUCCESS;
}

ESR_ReturnCode IntArrayList_Remove(IntArrayList* self, const int element)
{
  IntArrayListImpl* impl = (IntArrayListImpl*) self;
  int* contents = impl->contents; /* cache pointer */
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
    impl->contents = REALLOC(contents, (impl->actualSize / 2 + 1) * sizeof(int));
    passert(impl->contents != NULL); /* should never fail */
    impl->actualSize /= 2;
  }
  return ESR_SUCCESS;
}

ESR_ReturnCode IntArrayList_RemoveAll(IntArrayList* self)
{
  IntArrayListImpl* impl = (IntArrayListImpl*) self;
  
  impl->virtualSize = 0;
  return ESR_SUCCESS;
}

ESR_ReturnCode IntArrayList_Contains(IntArrayList* self, const int element, ESR_BOOL* exists)
{
  IntArrayListImpl* impl = (IntArrayListImpl*) self;
  size_t i;
  size_t virtualSize = impl->virtualSize; /* cache value */
  int* contents = impl->contents; /* cache value */
  
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

ESR_ReturnCode IntArrayList_Get(IntArrayList* self, size_t index, int* element)
{
  IntArrayListImpl* impl = (IntArrayListImpl*) self;
  
  passert(index >= 0 && index <= impl->virtualSize);
  *element = impl->contents[index];
  return ESR_SUCCESS;
}

ESR_ReturnCode IntArrayList_Set(IntArrayList* self, size_t index, const int element)
{
  IntArrayListImpl* impl = (IntArrayListImpl*) self;
  
  passert(index >= 0 && index <= impl->virtualSize);
  impl->contents[index] = element;
  return ESR_SUCCESS;
}

ESR_ReturnCode IntArrayList_GetSize(IntArrayList* self, size_t* size)
{
  IntArrayListImpl* impl = (IntArrayListImpl*) self;
  
  *size = impl->virtualSize;
  return ESR_SUCCESS;
}

ESR_ReturnCode IntArrayList_ToStaticArray(IntArrayList* self, int** newArray)
{
  IntArrayListImpl* impl = (IntArrayListImpl*) self;
  
  *newArray = impl->contents;
  impl->contents = NULL; /* prevent free() from deallocating buffer */
  return IntArrayList_Destroy(self);
}

ESR_ReturnCode IntArrayList_Destroy(IntArrayList* self)
{
  IntArrayListImpl* impl = (IntArrayListImpl*) self;
  
  FREE(impl->contents);
  FREE(impl);
  return ESR_SUCCESS;
}
