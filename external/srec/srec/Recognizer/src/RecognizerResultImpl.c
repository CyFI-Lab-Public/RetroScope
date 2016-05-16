/*---------------------------------------------------------------------------*
 *  RecognizerResultImpl.c  *
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

#include "SR_RecognizerResult.h"
#include "SR_RecognizerResultImpl.h"
#include "SR_SemanticResult.h"
#include "SR_SemanticResultImpl.h"
#include "SR_SemprocDefinitions.h"
#include "plog.h"
#include "pmemory.h"
#include "ESR_Locale.h"

#define MTAG NULL

ESR_ReturnCode SR_RecognizerResult_Create(SR_RecognizerResult** self, SR_RecognizerImpl* recogImpl)
{
  SR_RecognizerResultImpl* impl;
  
  if (self == NULL || recogImpl == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  impl = NEW(SR_RecognizerResultImpl, MTAG);
  if (impl == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }
  impl->Interface.getWaveform = &SR_RecognizerResult_GetWaveform;
  impl->Interface.getSize = &SR_RecognizerResult_GetSize;
  impl->Interface.getKeyCount = &SR_RecognizerResult_GetKeyCount;
  impl->Interface.getKeyList = &SR_RecognizerResult_GetKeyList;
  impl->Interface.getValue = &SR_RecognizerResult_GetValue;
  impl->Interface.getLocale = &SR_RecognizerResult_GetLocale;
  
  impl->nbestList = NULL;
  impl->nbestListSize = 0;
  impl->results = NULL;
  impl->recogImpl = recogImpl;
  *self = (SR_RecognizerResult*) impl;
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_RecognizerResult_GetWaveform(const SR_RecognizerResult* self, 
																							 const asr_int16_t** waveform, size_t* size)
{
  SR_RecognizerResultImpl* impl = (SR_RecognizerResultImpl*) self;
  
  if (waveform == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  
  // just point to the circular buffer read start point
  if (impl->recogImpl->waveformBuffer->overflow_count == 0)
  {
    *waveform = (asr_int16_t*)(((unsigned char *) impl->recogImpl->waveformBuffer->cbuffer) + 
      sizeof(CircularBuffer) + impl->recogImpl->waveformBuffer->cbuffer->readIdx);
    
    *size = impl->recogImpl->waveformBuffer->read_size;
    return ESR_SUCCESS;
  }
  else
  {
    PLogMessage(L("Warning: Voice Enrollment audio buffer overflow (spoke too much, over by %d bytes)\n"),
                impl->recogImpl->waveformBuffer->overflow_count);
                
    *waveform = (asr_int16_t*)(((unsigned char *) impl->recogImpl->waveformBuffer->cbuffer) + sizeof(CircularBuffer) + impl->recogImpl->waveformBuffer->cbuffer->readIdx);
    *size = impl->recogImpl->waveformBuffer->read_size;
    return ESR_SUCCESS;
  }
}

ESR_ReturnCode SR_RecognizerResult_GetSize(const SR_RecognizerResult* self, size_t* count)
{
  SR_RecognizerResultImpl* impl = (SR_RecognizerResultImpl*) self;
  ESR_ReturnCode rc;
  
  CHKLOG(rc, ArrayListGetSize(impl->results, count));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerResult_GetKeyCount(const SR_RecognizerResult* self, 
																							 const size_t nbest, size_t* count)
{
  SR_RecognizerResultImpl* impl = (SR_RecognizerResultImpl*) self;
  ESR_ReturnCode rc;
  ArrayList* results;
	SR_SemanticResult* result;

	/* Choose nbest-list entry */
	CHKLOG(rc, impl->results->get(impl->results, nbest, (void **)&results));
  /*
   * Currently we only support one semantic result per nbestlist entry,
   * so we grab the first available one.
   */
  CHKLOG(rc, results->get(results, 0, (void **)&result));
  CHKLOG(rc, result->getKeyCount(result , count));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerResult_GetKeyList(const SR_RecognizerResult* self, 
																							const size_t nbest, LCHAR** list, size_t* listSize)
{
  SR_RecognizerResultImpl* impl = (SR_RecognizerResultImpl*) self;
  ArrayList* results;
  SR_SemanticResult* result;
  ESR_ReturnCode rc;
    
  /* Choose nbest-list entry */
  CHKLOG(rc, impl->results->get(impl->results, nbest, (void **)&results));
  
  /*
   * Currently we only support one semantic result per nbestlist entry,
   * so we grab the first available one.
   */
  CHKLOG(rc, results->get(results, 0, (void **)&result));
  CHKLOG(rc, result->getKeyList(result, list, listSize));
  
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerResult_GetValue(const SR_RecognizerResult* self, const size_t nbest, 
																						const LCHAR* key, LCHAR* value, size_t* len)
{
  SR_RecognizerResultImpl* impl = (SR_RecognizerResultImpl*) self;
  ArrayList* results;
  SR_SemanticResult* result;
  SR_SemanticResultImpl* resultImpl;
  LCHAR* lValue;
  size_t actualLen = 0, i, resultCount;
  ESR_ReturnCode rc;
  ESR_BOOL noMatch = ESR_TRUE;
  
  /* Choose nbest-list entry */
  CHKLOG(rc, impl->results->get(impl->results, nbest, (void **)&results));
  /* Get the number of semantic results for the entry */
  CHKLOG(rc, results->getSize(results, &resultCount));
  
  for (i = 0; i < resultCount; ++i)
  {
    /* Choose semantic result */
    CHKLOG(rc, results->get(results, i, (void **)&result));
    resultImpl = (SR_SemanticResultImpl*) result;
    rc = resultImpl->results->get(resultImpl->results, key, (void**) & lValue);
    if (rc == ESR_SUCCESS)
    {
      noMatch = ESR_FALSE;
      actualLen += LSTRLEN(lValue);
    }
    else if (rc != ESR_NO_MATCH_ERROR)
      return rc;
  }
  if (noMatch)
    return ESR_NO_MATCH_ERROR;
  ++actualLen;
  
  /* Check for overflow */
  if (actualLen + 1 > *len)
  {
/* Unfortunately some people are using get value functions to get the size of the value by
 * passing a zero length buffer which causes errors to be logged. I am adding code so
 * that the error is not logged when the length is zero, thus preventing lots of logs from
 * flooding the system.  SteveR
 */
    if ( ( *len ) != 0 )
      PLogError(L("Buffer Overflow while fetching value for %s of choice %d Len %d"),
		key, nbest, *len );
    *len = actualLen + 1;
    return ESR_BUFFER_OVERFLOW;
  }
  *len = actualLen;
  
  LSTRCPY(value, L(""));
  for (i = 0; i < resultCount; ++i)
  {
    /* Choose semantic result */
    CHKLOG(rc, results->get(results, i, (void **)&result));
    resultImpl = (SR_SemanticResultImpl*) result;
    rc = resultImpl->results->get(resultImpl->results, key, (void **) & lValue);
    if (rc == ESR_SUCCESS)
      LSTRCAT(value, lValue);
    else if (rc != ESR_NO_MATCH_ERROR)
      return rc;
      
    /* Separate semantic results with '#' token */
	if (i < resultCount - 1) {
		int len = LSTRLEN(value);
		value[len] = MULTIPLE_MEANING_JOIN_CHAR;
        value[len+1] = 0;
	}
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerResult_Destroy(SR_RecognizerResult* self)
{
  SR_RecognizerResultImpl* impl = (SR_RecognizerResultImpl*) self;
  ArrayList* semanticList;
  SR_SemanticResult* semanticResult;
  size_t nbest, i, j, num_semanticResults;
  ESR_ReturnCode rc;
  
  /* each nbest list entry has an ArrayList of Semantic Results... need to destroy them too */
  if (impl->results != NULL)
  {
    CHKLOG(rc, impl->results->getSize(impl->results, &nbest));
    for (i = 0; i < nbest; ++i)
    {
      CHKLOG(rc, impl->results->get(impl->results, 0, (void **)&semanticList));
      if (semanticList == NULL)
        continue;
        
      CHKLOG(rc, semanticList->getSize(semanticList, &num_semanticResults));
      for (j = 0; j < num_semanticResults; ++j)
      {
        LCHAR literal[256];
        size_t len;
        
        CHKLOG(rc, semanticList->get(semanticList, 0, (void **)&semanticResult));
        CHKLOG(rc, semanticList->remove(semanticList, semanticResult));
        len = sizeof(literal) / sizeof(LCHAR);
        CHKLOG(rc, semanticResult->getValue(semanticResult, "literal", (LCHAR*) &literal, &len));
        CHKLOG(rc, semanticResult->destroy(semanticResult));
      }
      CHKLOG(rc, impl->results->remove(impl->results, semanticList));
      CHKLOG(rc, semanticList->destroy(semanticList));
    }
    CHKLOG(rc, impl->results->destroy(impl->results));
    impl->results = NULL;
  }
  
  if (impl->nbestList != NULL)
  {
    CA_DeleteNBestList(impl->nbestList);
    impl->nbestList = NULL;
  }
  FREE(impl);
  return ESR_SUCCESS;
CLEANUP:
  passert(rc != ESR_BUFFER_OVERFLOW);
  return rc;
}

ESR_ReturnCode SR_RecognizerResult_GetLocale(const SR_RecognizerResult* self, ESR_Locale* locale)
{
  SR_RecognizerResultImpl* impl = (SR_RecognizerResultImpl*) self;
  *locale = impl->locale;
  return ESR_SUCCESS;
}
