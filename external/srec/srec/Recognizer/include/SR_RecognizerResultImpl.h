/*---------------------------------------------------------------------------*
 *  SR_RecognizerResultImpl.h  *
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

#ifndef __SR_RECOGNIZERRESULTIMPL_H
#define __SR_RECOGNIZERRESULTIMPL_H



#include <string.h>
#include "ESR_ReturnCode.h"
#include "ArrayList.h"
#include "HashMap.h"
#include "SR_RecognizerImpl.h"


#include "simapi.h"

/**
 * RecognitionResult implementation.
 */
typedef struct SR_RecognizerResultImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  SR_RecognizerResult Interface;
  
  /**
   * N-best list.
   */
  CA_NBestList* nbestList;
  /**
   * Size of n-best list.
   */
  size_t nbestListSize;
  
  /**
   * Locale of grammar that produced this result
   */
  ESR_Locale locale;
  
  /**
   * Pointer to regognizer which owns this object.
   */
  SR_RecognizerImpl* recogImpl;
  
  /**
   * N-best list. ArrayList of ArrayLists of SR_SemanticResult.
   * The first ArrayList denotes the nbest-list.
   * The second ArrayList denotes the collection of semantic results per nbest list entry.
   */
  ArrayList* results;
}
SR_RecognizerResultImpl;

/**
 * Create a new recognizer result.
 *
 * @param self RecognizerResult handle
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerResult_Create(SR_RecognizerResult** self, SR_RecognizerImpl* recogImpl);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerResult_GetWaveform(const SR_RecognizerResult* self,
    const asr_int16_t** waveform,
    size_t* size);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerResult_GetSize(const SR_RecognizerResult* self,
    size_t* resultSize);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerResult_GetKeyCount(const SR_RecognizerResult* self,
    const size_t nbest,
    size_t* count);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerResult_GetKeyList(const SR_RecognizerResult* self,
    const size_t nbest,
    LCHAR** list,
    size_t* listSize);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerResult_GetValue(const SR_RecognizerResult* self,
    const size_t nbest,
    const LCHAR* key,
    LCHAR* value,
    size_t* len);
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerResult_GetLocale(const SR_RecognizerResult* self,
    ESR_Locale* locale);
    
    
/**
 * Default implementation.
 */
SREC_RECOGNIZER_API ESR_ReturnCode SR_RecognizerResult_Destroy(SR_RecognizerResult* self);

#endif /* __SR_RECOGNIZERRESULTIMPL_H */
