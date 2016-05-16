/*---------------------------------------------------------------------------*
 *  Int8ArrayList.c  *
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
#include "plog.h"
#include "pmemory.h"


ESR_ReturnCode Int8ArrayListAdd(Int8ArrayList* self, asr_int8_t element)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->add(self, element);
}

ESR_ReturnCode Int8ArrayListRemove(Int8ArrayList* self, asr_int8_t element)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->remove(self, element);
}

ESR_ReturnCode Int8ArrayListRemoveAll(Int8ArrayList* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->removeAll(self);
}

ESR_ReturnCode Int8ArrayListContains(Int8ArrayList* self, asr_int8_t element, ESR_BOOL* exists)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->contains(self, element, exists);
}

ESR_ReturnCode Int8ArrayListGetSize(Int8ArrayList* self, size_t* size)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getSize(self, size);
}

ESR_ReturnCode Int8ArrayListGet(Int8ArrayList* self, size_t index, asr_int8_t* element)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->get(self, index, element);
}

ESR_ReturnCode Int8ArrayListSet(Int8ArrayList* self, size_t index, asr_int8_t element)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->set(self, index, element);
}

ESR_ReturnCode Int8ArrayListToStaticArray(Int8ArrayList* self, asr_int8_t** newArray)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->toStaticArray(self, newArray);
}

ESR_ReturnCode Int8ArrayListClone(Int8ArrayList* self, Int8ArrayList* clone)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->clone(self, clone);
}

ESR_ReturnCode Int8ArrayListDestroy(Int8ArrayList* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->destroy(self);
}
