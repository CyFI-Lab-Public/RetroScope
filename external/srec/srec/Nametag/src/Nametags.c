/*---------------------------------------------------------------------------*
 *  Nametags.c  *
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

#include "plog.h"
#include "SR_Nametags.h"

ESR_ReturnCode SR_NametagsLoad(SR_Nametags* self, const LCHAR* filename)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->load(self, filename);
}

ESR_ReturnCode SR_NametagsSave(SR_Nametags* self, const LCHAR* filename)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->save(self, filename);
}

ESR_ReturnCode SR_NametagsAdd(SR_Nametags* self, SR_Nametag* nametag)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->add(self, nametag);
}

ESR_ReturnCode SR_NametagsRemove(SR_Nametags* self, const LCHAR* id)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->remove(self, id);
}

ESR_ReturnCode SR_NametagsGetSize(SR_Nametags* self, size_t* result)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getSize(self, result);
}

ESR_ReturnCode SR_NametagsGet(SR_Nametags* self, const LCHAR* id, SR_Nametag** nametag)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->get(self, id, nametag);
}

ESR_ReturnCode SR_NametagsGetAtIndex(SR_Nametags* self, size_t index, SR_Nametag** nametag)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getAtIndex(self, index, nametag);
}

ESR_ReturnCode SR_NametagsContains(SR_Nametags* self, const LCHAR* id, ESR_BOOL* result)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->contains(self, id, result);
}

ESR_ReturnCode SR_NametagsDestroy(SR_Nametags* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->destroy(self);
}
