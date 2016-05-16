/*---------------------------------------------------------------------------*
 *  ExpressionEvaluator.c  *
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

#include "SR_ExpressionEvaluator.h"
#include "LCHAR.h"
#include "plog.h"



//static const char* MTAG = __FILE__;


ESR_ReturnCode EE_Init(ExpressionEvaluator** self)
{
  return ESR_SUCCESS;
}

ESR_ReturnCode EE_Free(ExpressionEvaluator* self)
{
  return ESR_SUCCESS;
}

ESR_ReturnCode EE_concat(LCHAR* name, LCHAR** operands, size_t opCount, void* data, LCHAR* resultBuf, size_t* resultLen)
{
  size_t i, opLen;
  ESR_ReturnCode rc;
  
  if (operands == NULL || resultBuf == NULL || resultLen == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  *resultLen = 0;
  for (i = 0; i < opCount; ++i)
  {
    opLen = LSTRLEN(operands[i]);
    MEMCHK(rc, (*resultLen + opLen), MAX_STRING_LEN);
    LSTRCAT(resultBuf, operands[i]);
    *resultLen += opLen;
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode EE_conditional(LCHAR* name, LCHAR** operands, size_t opCount, void* data, LCHAR* resultBuf, size_t* resultLen)
{
  if (operands == NULL || resultBuf == NULL || resultLen == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  
  if (!LSTRCMP(operands[0], UNDEFINED_SYMBOL) || !operands[0] ||
      !LSTRCMP(operands[0], FALSE_SYMBOL))
  {
    if (strlen(operands[2]) >= *resultLen)
    {
      PLogError("EE_conditional overflow error %d<%d\n", *resultLen, strlen(operands[2]));
      *resultLen = strlen(operands[2]);
      return ESR_BUFFER_OVERFLOW;
    }
    LSTRCPY(resultBuf, operands[2]);
  }
  else
  {
    if (strlen(operands[1]) >= *resultLen)
    {
      PLogError("EE_conditional overflow error %d<%d\n", *resultLen, strlen(operands[1]));
      *resultLen = strlen(operands[1]);
      return ESR_BUFFER_OVERFLOW;
    }
    LSTRCPY(resultBuf, operands[1]);
  }
  *resultLen = LSTRLEN(resultBuf);
  return ESR_SUCCESS;
}


ESR_ReturnCode EE_add(LCHAR* name, LCHAR** operands, size_t opCount, void* data, LCHAR* resultBuf, size_t* resultLen)
{
  size_t i, sum;
  
  if (operands == NULL || resultBuf == NULL || resultLen == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  sum = 0;
  for (i = 0; i < opCount; ++i)
    sum += atoi(operands[i]);
    
  return litostr(sum, resultBuf, resultLen, BASE_10);
}

ESR_ReturnCode EE_subtract(LCHAR* name, LCHAR** operands, size_t opCount, void* data, LCHAR* resultBuf, size_t* resultLen)
{
  size_t i;
  int diff;
  
  if (operands == NULL || resultBuf == NULL || resultLen == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  diff = atoi(operands[0]);
  for (i = 1; i < opCount; ++i)
    diff -= atoi(operands[i]);
    
  return litostr(diff, resultBuf, resultLen, BASE_10);
}
