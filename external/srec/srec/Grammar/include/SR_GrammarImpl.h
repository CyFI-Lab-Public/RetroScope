/*---------------------------------------------------------------------------*
 *  SR_GrammarImpl.h  *
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

#ifndef __SR_GRAMMARIMPL_H
#define __SR_GRAMMARIMPL_H



#include "ArrayList.h"
#include "HashMap.h"
#include "SR_Grammar.h"
#include "SR_Nametags.h"
#include "ESR_ReturnCode.h"
#include "ESR_SessionType.h"
#include "SR_EventLog.h"
#include "SR_Recognizer.h"

/* Semproc stuff */
#include "SR_SemanticGraph.h"
#include "SR_SemanticProcessor.h"



#include "simapi.h"

/**
 * SR_Grammar implementation.
 */
typedef struct SR_GrammarImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  SR_Grammar Interface;
  
  /**
   * Associates a grammar with a Recognizer.
   *
   * @param self SR_Grammar handle
   * @param recognizer The recognizer
   */
  // ESR_ReturnCode(*setupAcousticModels)(SR_Grammar* self, SR_AcousticModels* models);
  ESR_ReturnCode(*setupRecognizer)(SR_Grammar* self, SR_Recognizer* recognizer);
  /**
   * Dissociates a grammar from a Recognizer.
   *
   * @param self SR_Grammar handle
   */
  ESR_ReturnCode(*unsetupRecognizer)(SR_Grammar* self);
  
  /**
   * Legacy CREC syntax.
   */
  CA_Syntax* syntax;
  /**
   * Recognizer associated with SR_Grammar.
   */
  // SR_AcousticModels* models;
  SR_Recognizer* recognizer;
  
  /**
   * Vocabulary.
   */
  SR_Vocabulary* vocabulary;
  
  /**
   * Semantic Graph
   */
  SR_SemanticGraph* semgraph;
  
  /**
   * Semantic Processor
   */
  SR_SemanticProcessor* semproc;
  
  /**
   * Grammar-specific parameters.
   */
  ESR_SessionType* parameters;
  
  /**
   * Event Log reference set internally by regognizer during its association with this grammar
   */
  SR_EventLog* eventLog;
  
  /**
   * Event log, logging level.
   */
  size_t logLevel;
  
  /**
   * Indicates if the grammar has been activated.
   */
  ESR_BOOL isActivated;
}
SR_GrammarImpl;


/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_CompileRule(SR_Grammar* self, const LCHAR* name);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_AddRuleFromList(SR_Grammar* self, SR_Vocabulary* vocabulary, const LCHAR* name);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_Compile(SR_Grammar* self);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_AddRule(SR_Grammar* self, const LCHAR* name, const LCHAR* value);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_DeleteRule(SR_Grammar* self, const LCHAR* name);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_ContainsRule(SR_Grammar* self, const LCHAR* name, ESR_BOOL* result);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_AddWordToSlot(SR_Grammar* self, const LCHAR* slot, 
																												 const LCHAR* word, const LCHAR* pronunciation, 
																												 int weight, const LCHAR* tag);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_ResetAllSlots(SR_Grammar* self);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_AddNametagToSlot(SR_Grammar* self, const LCHAR* slot, 
																														const SR_Nametag* nametag, int weight, 
																														const LCHAR* tag);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_Create(SR_Grammar** self);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_Destroy(SR_Grammar* self);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_Save(SR_Grammar* self, const LCHAR* filename);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_CheckParse(SR_Grammar* self, const LCHAR* transcription, SR_SemanticResult** result, size_t* resultCount);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_SetDispatchFunction(SR_Grammar* self, const LCHAR* name, void* userData, SR_GrammarDispatchFunction function);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_SetParameter(SR_Grammar* self, const LCHAR* key, void* value);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_SetSize_tParameter(SR_Grammar* self, const LCHAR* key, size_t value);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_GetParameter(SR_Grammar* self, const LCHAR* key, void** value);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_GetSize_tParameter(SR_Grammar* self, const LCHAR* key, size_t* value);
/**
 * Default implementation.
 */
// SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_SetupModels(SR_Grammar* self, SR_AcousticModels* models);
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_SetupRecognizer(SR_Grammar* self, SR_Recognizer* recognizer);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_UnsetupRecognizer(SR_Grammar* self);
/**
 * Default implementation.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_SetupVocabulary(SR_Grammar *self, SR_Vocabulary *vocabulary);


#endif /* __SR_GRAMMARIMPL_H */
