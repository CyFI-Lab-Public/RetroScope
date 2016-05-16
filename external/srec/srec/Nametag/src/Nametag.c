/*---------------------------------------------------------------------------*
 *  Nametag.c  *
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

#include "SR_Nametag.h"
#include "SR_NametagImpl.h"
#include "plog.h"
#include "pmemory.h"


ESR_ReturnCode SR_NametagGetID(const SR_Nametag* self, LCHAR** id)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getID(self, id);
}

ESR_ReturnCode SR_NametagGetValue(const SR_Nametag* self, const char** pvalue, size_t* plen)
{
    if (self == NULL || pvalue == NULL || plen == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getValue(self, pvalue, plen);
}

ESR_ReturnCode SR_NametagSetID(SR_Nametag* self, const LCHAR* id)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->setID(self, id);
}

ESR_ReturnCode SR_NametagClone(const SR_Nametag* self, SR_Nametag** result)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->clone(self, result);
}

ESR_ReturnCode SR_NametagDestroy(SR_Nametag* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->destroy(self);
}
