/*---------------------------------------------------------------------------*
 *  HashMap.c  *
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
#include "pmemory.h"
#include <string.h>

ESR_ReturnCode HashMapPut(HashMap* self, const LCHAR* key, void* value)
{
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  return self->put(self, key, value);
}

ESR_ReturnCode HashMapRemove(HashMap* self, const LCHAR* key)
{
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  return self->remove(self, key);
}

ESR_ReturnCode HashMapRemoveAndFree(HashMap* self, const LCHAR* key)
{
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  return self->removeAndFree(self, key);
}

ESR_ReturnCode HashMapRemoveAtIndex(HashMap* self, const size_t index)
{
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  return self->removeAtIndex(self, index);
}

ESR_ReturnCode HashMapRemoveAll(HashMap* self)
{
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  return self->removeAll(self);
}

ESR_ReturnCode HashMapRemoveAndFreeAll(HashMap* self)
{
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  return self->removeAndFreeAll(self);
}

ESR_ReturnCode HashMapContainsKey(HashMap* self, const LCHAR* key, ESR_BOOL* exists)
{
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  return self->containsKey(self, key, exists);
}

ESR_ReturnCode HashMapGetSize(HashMap* self, size_t* size)
{
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  return self->getSize(self, size);
}

ESR_ReturnCode HashMapGet(HashMap* self, const LCHAR* key, void** value)
{
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  return self->get(self, key, value);
}

ESR_ReturnCode HashMapGetKeyAtIndex(HashMap* self, const size_t index, LCHAR** key)
{
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  return self->getKeyAtIndex(self, index, key);
}

ESR_ReturnCode HashMapGetValueAtIndex(HashMap* self, const size_t index, void** value)
{
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  return self->getValueAtIndex(self, index, value);
}

ESR_ReturnCode HashMapDestroy(HashMap* self)
{
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  return self->destroy(self);
}
