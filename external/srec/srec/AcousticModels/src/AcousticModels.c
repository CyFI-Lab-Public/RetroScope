/*---------------------------------------------------------------------------*
 *  AcousticModels.c  *
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

#include "SR_AcousticModels.h"
#include "SR_AcousticModelsImpl.h"
#include "plog.h"
#include "pmemory.h"


ESR_ReturnCode SR_AcousticModelsDestroy(SR_AcousticModels* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->destroy(self);
}

ESR_ReturnCode SR_AcousticModelsSave(SR_AcousticModels* self, const LCHAR* filename)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->save(self, filename);
}

ESR_ReturnCode SR_AcousticModelsSetParameter(SR_AcousticModels* self, const LCHAR* key, LCHAR* value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->setParameter(self, key, value);
}

ESR_ReturnCode SR_AcousticModelsGetParameter(SR_AcousticModels* self, const LCHAR* key, LCHAR* value, size_t* len)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getParameter(self, key, value, len);
}

ESR_ReturnCode SR_AcousticModelsGetCount(SR_AcousticModels* self, size_t* size)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getCount(self, size);
}

ESR_ReturnCode SR_AcousticModelsGetID(SR_AcousticModels* self, size_t index, SR_AcousticModelID* id, size_t* size)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getID(self, index, id, size);
}

ESR_ReturnCode SR_AcousticModelsSetID(SR_AcousticModels* self, size_t index, SR_AcousticModelID* id)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->setID(self, index, id);
}
