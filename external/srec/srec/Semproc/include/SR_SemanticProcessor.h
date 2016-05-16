/*---------------------------------------------------------------------------*
 *  SR_SemanticProcessor.h  *
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

#ifndef __SR_SEMANTICPROCESSOR_H
#define __SR_SEMANTICPROCESSOR_H



#include "SR_SemprocPrefix.h"
#include "SR_SemanticGraph.h"
#include "SR_SemanticResult.h"
#include "pstdio.h"
#include "ptypes.h"
#include "ESR_ReturnCode.h"




/**
 * Wrapper for the eScript Semantic Processor Implementation.
 */
typedef struct SR_SemanticProcessor_t
{
  /**
   * Parse a graph with the processor provided as argument. Store semantic results in the objects pointed to by each 
   * element in the array provided. In other words, each element of the array is a pointer to a SemanticResult object 
   * created (and destroyed) by the caller of the function.
   * The size of the array must be SWIrecResultData **result_dataindicated in resultCount. If the array is not big enough, ESR_BUFFER_OVERFLOW 
   * is returned with resultCount set to the size required. 
   */
  ESR_ReturnCode(*checkParse)(struct SR_SemanticProcessor_t* self, SR_SemanticGraph* semgraph, const LCHAR* transcription, SR_SemanticResult** result, size_t* resultCount);
  /**
   * Parse a graph with the processor provided as argument. Store semantic results in the objects pointed to by each 
   * element in the array provided. In other words, each element of the array is a pointer to a SemanticResult object 
   * created (and destroyed) by the caller of the function.
   * The size of the array must be SWIrecResultData **result_dataindicated in resultCount. If the array is not big enough, ESR_BUFFER_OVERFLOW 
   * is returned with resultCount set to the size required. 
   */
  ESR_ReturnCode(*checkParseByWordID)(struct SR_SemanticProcessor_t* self, SR_SemanticGraph* semgraph, wordID* wordIDs, SR_SemanticResult** result, size_t* resultCount);
  /**
   * Frees the memory used by the Semantic Processor.
   *
   * @param self SR_SemanticProcessor handle
   */
  ESR_ReturnCode(*destroy)(struct SR_SemanticProcessor_t* self);
  
  /**
   * Set a param to be read by Semantic Processor during processing.
   *
   * @param self SR_SemanticProcessor handle
   * @param key The name of the param
   * @param value The value of the param
   */
  ESR_ReturnCode(*setParam)(struct SR_SemanticProcessor_t* self, const LCHAR* key, const LCHAR* value);
  
  /**
   * Flush the internals of the semantic processor 
   *
   * @param self SR_SemanticProcessor handle
   */
  ESR_ReturnCode(*flush)(struct SR_SemanticProcessor_t* self);
  
}
SR_SemanticProcessor;


/**
 * Create a new Semantic Processor.
 *
 * @param self SR_SemanticProcessor handle
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticProcessorCreate(SR_SemanticProcessor** self);
/**
 * Create a new Semantic Processor.
 *
 * @param self SR_SemanticProcessor handle
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticProcessorDestroy(SR_SemanticProcessor* self);
/**
* Set a param to be read by Semantic Processor during processing.
*
* @param self SR_SemanticProcessor handle
* @param key The name of the param
* @param value The value of the param
*/
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticProcessorSetParam(SR_SemanticProcessor* self, const LCHAR* key, const LCHAR* value);
/**
* Flush the internals of the Semantic Processor
*
* @param self SR_SemanticProcessor handle
*/
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticProcessorFlush(SR_SemanticProcessor* self);



#endif /* __SR_SEMANTICPROCESSOR_H */
