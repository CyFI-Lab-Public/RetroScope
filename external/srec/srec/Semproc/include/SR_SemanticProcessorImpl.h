/*---------------------------------------------------------------------------*
 *  SR_SemanticProcessorImpl.h  *
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

#ifndef __SR_SEMANTICPROCESSORIMPL_H
#define __SR_SEMANTICPROCESSORIMPL_H



#include "SR_SemanticGraph.h"
#include "SR_SemanticProcessor.h"
#include "SR_SemanticResult.h"
#include "ESR_ReturnCode.h"


/**
 * eScript Semantic Processor stuff
 */
#include "SR_ExpressionParser.h"
#include "SR_ExpressionEvaluator.h"
#include "SR_LexicalAnalyzer.h"
#include "SR_SymbolTable.h"

/**
 * SR_SemanticProcessor implementation.
 */
typedef struct SR_SemanticProcessorImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  SR_SemanticProcessor Interface;
  
  ExpressionParser* parser;
  ExpressionEvaluator* eval;
  SymbolTable* symtable;
  LexicalAnalyzer* analyzer;
  /* the accumulated scripts */
  LCHAR* acc_scripts;
}
SR_SemanticProcessorImpl;


/**
 * Default implementation.
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticProcessor_Destroy(SR_SemanticProcessor* self);
/**
 * Default implementation.
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticProcessor_CheckParse(SR_SemanticProcessor* self, SR_SemanticGraph* semgraph, const LCHAR* transcription, SR_SemanticResult** result, size_t* resultCount);
/**
 * Default implementation.
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticProcessor_CheckParseByWordID(SR_SemanticProcessor* self, SR_SemanticGraph* semgraph, wordID* wordIDs, SR_SemanticResult** result, size_t* resultCount);
/**
 * Default implementation.
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticProcessor_SetParam(SR_SemanticProcessor* self, const LCHAR* key, const LCHAR* value);
/**
 * Default implementation.
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticProcessor_Flush(SR_SemanticProcessor* self);
/**
 * Default implementation.
 */
SREC_SEMPROC_API PINLINE ESR_BOOL isnum(const LCHAR* str);




#endif /* __SR_SEMANTICPROCESSORIMPL_H */
