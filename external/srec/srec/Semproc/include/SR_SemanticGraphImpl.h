/*---------------------------------------------------------------------------*
 *  SR_SemanticGraphImpl.h  *
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

#ifndef __SR_SEMANTICGRAPHIMPL_H
#define __SR_SEMANTICGRAPHIMPL_H



#include "SR_SemprocPrefix.h"
#include "SR_SemanticGraph.h"
#include "pstdio.h"
#include "ptypes.h"
#include "ESR_ReturnCode.h"

/**
 * SREC stuff
 */
#include "srec_context.h"


/**
 * SR_SemanticGraph implementation.
 */
typedef struct SR_SemanticGraphImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  SR_SemanticGraph Interface;
  
  /**
   * Input labels are  words in a spoken utterance.
   * These words are the SAME as those used by the recogizer graph, so I want to
   * reuse that data rather than duplicate it.
   * Withing this module, ilabls are constant... they are owned and may only be changed
   * externally by AddWordToSlot() for example
   */
  wordmap* ilabels;
  
  /**
   * The word map containing the actual scripts. The index of teh script in the wordmap
   * corresponds to the index found in the graph minus the script_olabel_offset
   */
  wordmap* scripts;
  
  /**
   * Integer offset for referencing script output labels when mapping between
   * integer ids, and their respective string values.
   */
  labelID script_olabel_offset;     /* starts at SEMGRAPH_SCRIPT_OFFSET */
  
  /**
   * Output labels for end of scope markers. These are of the form
   * "rule_name}"
   * This is pretty static doesen't change
   */
  wordmap* scopes_olabels;
  
  /**
   * Integer offset for referencing end of scope output labels when mapping between
   * integer ids, and their respective string values.
   */
  size_t scopes_olabel_offset;     /* starts at SEMGRAPH_SCOPE_OFFSET */
  
  /**
   * Double linked list of arcs forming graph
   * ilables are integers which map to words in the word maps
   * olabels are integers which map to words in the word maps
   */
  arc_token* arc_token_list;
  
  /**
   * The arc where additional words may be added on to (see addWordToSlot)
   * Only Root slot supported for now.
   */
  arc_token* arc_token_insert_start;
  
  /**
   * The end node for dynamically added words.
   */
  arc_token* arc_token_insert_end;
  
  /**
   * Free list of arcs for dynamic add word to slot.
   */
  arc_token* arc_token_freelist;
  
  /**
   * The number of arcs in the graph
   */
  arcID arc_token_list_len;
  
  /* slot addition */
  arc_token* arcs_for_slot[MAX_NUM_SLOTS];
  
}
SR_SemanticGraphImpl;

/* internal functions */
arc_token* arc_tokens_find_ilabel(arc_token* base, arc_token* arc_token_list, wordID wdid);
arc_token* arc_tokens_get_free(arc_token* base, arc_token** arc_token_freelist);

/**
 * Default implementation.
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticGraph_Destroy(SR_SemanticGraph* self);
/**
 * Default implementation.
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticGraph_Load(SR_SemanticGraph* self, wordmap* ilabels, const LCHAR* basename, int num_words_to_add);
/**
 * Default implementation.
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticGraph_Unload(SR_SemanticGraph* self);
/**
 * Default implementation.
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticGraph_Save(SR_SemanticGraph* self, const LCHAR* filename, int version_number);
/**
 * Default implementation.
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticGraph_AddWordToSlot(SR_SemanticGraph* self, const LCHAR* slot, const LCHAR* word, const LCHAR* tag, const ESR_BOOL maybeMultiMeaning);
/**
 * Default implementation.
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticGraph_Reset(SR_SemanticGraph* self);

#endif /* __SR_SEMANTICGRAPHIMPL_H */
