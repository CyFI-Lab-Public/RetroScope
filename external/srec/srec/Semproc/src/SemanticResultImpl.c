/*---------------------------------------------------------------------------*
 *  SemanticResultImpl.c  *
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
#include <pmemory.h>
#include "plog.h"


static const char* MTAG = __FILE__;


ESR_ReturnCode SR_SemanticResultCreate(SR_SemanticResult** self)
{
  SR_SemanticResultImpl* impl;
  ESR_ReturnCode rc;
  
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  impl = NEW(SR_SemanticResultImpl, MTAG);
  if (impl == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }
  
  impl->Interface.destroy = &SR_SemanticResult_Destroy;
  impl->Interface.getKeyCount = &SR_SemanticResult_GetKeyCount;
  impl->Interface.getKeyList = &SR_SemanticResult_GetKeyList;
  impl->Interface.getValue = &SR_SemanticResult_GetValue;
  impl->results = NULL;
  
  rc = HashMapCreate(&impl->results);
  if (rc != ESR_SUCCESS)
    goto CLEANUP;
  *self = (SR_SemanticResult*) impl;
  return ESR_SUCCESS;
CLEANUP:
  impl->Interface.destroy(&impl->Interface);
  return rc;
}

ESR_ReturnCode SR_SemanticResult_GetKeyCount(SR_SemanticResult* self, size_t* count)
{
  SR_SemanticResultImpl* impl = (SR_SemanticResultImpl*) self;
  ESR_ReturnCode rc;
  
  CHKLOG(rc, impl->results->getSize(impl->results, count));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_SemanticResult_GetKeyList(SR_SemanticResult* self, LCHAR** list, size_t* count)
{
  SR_SemanticResultImpl* impl = (SR_SemanticResultImpl*) self;
  LCHAR* theKey;
  ESR_ReturnCode rc;
  size_t size, i;
    
  CHKLOG(rc, HashMapGetSize(impl->results, &size));
  
  if (size > *count)
  {
    PLogError(L("ESR_BUFFER_OVERFLOW"));
    *count = size;
    return ESR_BUFFER_OVERFLOW;
  }  
  else if (list == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  *count = size;
  for (i = 0; i < size; ++i)
  {
    CHKLOG(rc, HashMapGetKeyAtIndex(impl->results, i, &theKey));
    list[i] = theKey;
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_SemanticResult_GetValue(SR_SemanticResult* self, const LCHAR* key, LCHAR* value, size_t* len)
{
  SR_SemanticResultImpl* impl = (SR_SemanticResultImpl*) self;
  LCHAR* theValue;
  ESR_ReturnCode rc;
  
  CHKLOG(rc, impl->results->get(impl->results, key, (void **)&theValue));
  if (LSTRLEN(theValue) + 1 > *len)
  {
    *len = LSTRLEN(theValue) + 1;
    PLogError(L("ESR_BUFFER_OVERFLOW, requires len>=%d"), LSTRLEN(theValue) + 1);
    return ESR_BUFFER_OVERFLOW;
  }
  LSTRCPY(value, theValue);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_SemanticResult_Destroy(SR_SemanticResult* self)
{
  SR_SemanticResultImpl* impl = (SR_SemanticResultImpl*) self;
  ESR_ReturnCode rc = ESR_SUCCESS;
  
  CHKLOG(rc, HashMapRemoveAndFreeAll(impl->results));
  CHKLOG(rc, HashMapDestroy(impl->results));
  FREE(impl);
  return rc;
CLEANUP:
  return rc;
}
