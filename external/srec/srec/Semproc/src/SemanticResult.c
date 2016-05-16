/*---------------------------------------------------------------------------*
 *  SemanticResult.c  *
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

#include "SR_SemanticResult.h"
#include "SR_SemanticResultImpl.h"
#include "plog.h"


//static const char* MTAG = __FILE__;


ESR_ReturnCode SR_SemanticResultGetKeyCount(SR_SemanticResult* self, size_t* count)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getKeyCount(self, count);
}

ESR_ReturnCode SR_SemanticResultGetKeyList(SR_SemanticResult* self, LCHAR** list, size_t* size)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getKeyList(self, list, size);
}

ESR_ReturnCode SR_SemanticResultGetValue(SR_SemanticResult* self, const LCHAR* key, LCHAR* value, size_t* len)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getValue(self, key, value, len);
}

ESR_ReturnCode SR_SemanticResultDestroy(SR_SemanticResult* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->destroy(self);
}
