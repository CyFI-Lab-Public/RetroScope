/*---------------------------------------------------------------------------*
 *  ArrayList.c  *
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
#include "plog.h"
#include "pmemory.h"


ESR_ReturnCode ArrayListAdd(ArrayList* self, void* element)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->add(self, element);
}

ESR_ReturnCode ArrayListInsertAt(ArrayList* self, size_t index, void* element)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->insertAt(self, index, element);
}

ESR_ReturnCode ArrayListRemove(ArrayList* self, void* element)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->remove(self, element);
}

ESR_ReturnCode ArrayListRemoveAtIndex(ArrayList* self, size_t index)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->removeAtIndex(self, index);
}

ESR_ReturnCode ArrayListRemoveAll(ArrayList* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->removeAll(self);
}

ESR_ReturnCode ArrayListContains(ArrayList* self, void* element, ESR_BOOL* exists)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->contains(self, element, exists);
}

ESR_ReturnCode ArrayListGetSize(ArrayList* self, size_t* size)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getSize(self, size);
}

ESR_ReturnCode ArrayListGet(ArrayList* self, size_t index, void** element)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->get(self, index, element);
}

ESR_ReturnCode ArrayListSet(ArrayList* self, size_t index, void* element)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->set(self, index, element);
}

ESR_ReturnCode ArrayListClone(ArrayList* self, ArrayList* clone)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->clone(self, clone);
}

ESR_ReturnCode ArrayListDestroy(ArrayList* self)
{
  if (self == NULL)
    return ESR_INVALID_ARGUMENT;
  return self->destroy(self);
}
