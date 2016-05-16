/*---------------------------------------------------------------------------*
 *  SR_LexicalAnalyzer.h  *
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

#ifndef __SR_LEXICAL_ANALYZER_H
#define __SR_LEXICAL_ANALYZER_H



#include "SR_SemprocPrefix.h"
#include "SR_SemprocDefinitions.h"

#include "ptypes.h"
#include "pstdio.h"

#include "ESR_ReturnCode.h"

/**
 * The Lexical Analyzer
 */
typedef struct LexicalAnalyzer_t
{
  /**
   * Pointer to the script to analyze .
   */
  LCHAR* script;
  
  /**
   * Pointer to the next token in the script. 
   */
  LCHAR* nextToken;
  
}
LexicalAnalyzer;

/**
 * Create and Initialize
 * @param self pointer to the newly created object
 */
SREC_SEMPROC_API ESR_ReturnCode LA_Init(LexicalAnalyzer **self);

/**
 * Startup the analysis
 * @param self pointer to Lexical Analyzer
 * @param script pointer to the script to analyze
 */
SREC_SEMPROC_API ESR_ReturnCode LA_Analyze(LexicalAnalyzer *self, LCHAR *script);

/**
 * Free
 * @param self pointer to Lexical Analyzer
 */
SREC_SEMPROC_API ESR_ReturnCode LA_Free(LexicalAnalyzer *self);

/**
 * Gets the next token.
 * @param self pointer to Lexical Analyzer
 * @param token buffer to hold the next token
 * @param tokenLen length of token
 */
SREC_SEMPROC_API ESR_ReturnCode LA_nextToken(LexicalAnalyzer *self, LCHAR* token, size_t* tokenLen);


#endif /* __LEXICAL_ANALYZER_H */
