/*---------------------------------------------------------------------------*
 *  LStringImpl.c  *
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

#include "lstring.h"
#include "LStringImpl.h"
#include "plog.h"
#include "pmemory.h"

#define MTAG NULL
#define INITIAL_SIZE 32

ESR_ReturnCode LStringCreate(LString** self)
{
  LStringImpl* impl;
  
  impl = NEW(LStringImpl, MTAG);
  if (impl == NULL)
    return ESR_OUT_OF_MEMORY;
  impl->Interface.append = &LString_Append;
  impl->Interface.toLCHAR = &LString_ToLCHAR;
  impl->Interface.reset = &LString_Reset;
  impl->Interface.destroy = &LString_Destroy;
  impl->size = INITIAL_SIZE;
  impl->value = MALLOC(sizeof(LCHAR) * INITIAL_SIZE, MTAG);
  if (impl->value == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }
  LSTRCPY(impl->value, L(""));
  if (impl->value == NULL)
    return ESR_OUT_OF_MEMORY;
  *self = (LString*) impl;
  return ESR_SUCCESS;
}

ESR_ReturnCode LString_Append(LString* self, const LCHAR* value)
{
  LStringImpl* impl = (LStringImpl*) self;
  size_t needed;
  
  needed = LSTRLEN(impl->value) + LSTRLEN(value) + 1;
  
  if (needed > impl->size)
  {
    LCHAR* temp = REALLOC(impl->value, sizeof(LCHAR) * (needed + (impl->size / 2)));
    if (temp == NULL)
      return ESR_OUT_OF_MEMORY;
    impl->size = sizeof(LCHAR) * (needed + (impl->size / 2));
    impl->value = temp;
  }
  LSTRCAT(impl->value, value);
  return ESR_SUCCESS;
}

ESR_ReturnCode LString_Reset(LString* self)
{
  LStringImpl* impl = (LStringImpl*) self;
  
  LSTRCPY(impl->value, L(""));
  return ESR_SUCCESS;
}

ESR_ReturnCode LString_ToLCHAR(LString* self, LCHAR** result)
{
  LStringImpl* impl = (LStringImpl*) self;
  
  if (result == NULL)
    return ESR_INVALID_ARGUMENT;
  *result = impl->value;
  impl->value = NULL;
  return self->destroy(self);
}

ESR_ReturnCode LString_Destroy(LString* self)
{
  LStringImpl* impl = (LStringImpl*) self;
  
  FREE(impl->value);
  FREE(impl);
  return ESR_SUCCESS;
}
