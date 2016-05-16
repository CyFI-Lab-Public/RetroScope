/*---------------------------------------------------------------------------*
 *  SR_SemanticResultImpl.h  *
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

#ifndef __SR_SEMANTICRESULTIMPL_H
#define __SR_SEMANTICRESULTIMPL_H



#include "ESR_ReturnCode.h"
#include "HashMap.h"


/**
 * SemanticResult implementation.
 */
typedef struct SR_SemanticResultImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  SR_SemanticResult Interface;
  
  /**
   * Semantic [key, value] pairs.
   */
  HashMap* results;
}
SR_SemanticResultImpl;

/**
 * Default implementation.
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticResult_GetKeyCount(SR_SemanticResult* self, size_t* count);
/**
 * Default implementation.
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticResult_GetKeyList(SR_SemanticResult* self, LCHAR** list, size_t* size);
/**
 * Default implementation.
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticResult_GetValue(SR_SemanticResult* self, const LCHAR* key, LCHAR* value, size_t* len);
/**
 * Default implementation.
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticResult_Destroy(SR_SemanticResult* self);

#endif /* __SR_SEMANTICRESULTIMPL_H */
