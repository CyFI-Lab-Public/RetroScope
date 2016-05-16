/*---------------------------------------------------------------------------*
 *  SR_SemanticGraph.h  *
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

#ifndef __SR_SEMANTICGRAPH_H
#define __SR_SEMANTICGRAPH_H



#include "SR_SemprocPrefix.h"
#include "pstdio.h"
#include "ptypes.h"
#include "srec_context.h" /* for wordmap */
#include "ESR_ReturnCode.h"


/**
 * A Semantic Graph is a data structure representing acceptable phrases and their associated
 * meaning. A graph is made up of nodes and arcs. Arcs are associated with input symbols and
 * output symbols. Input symbols are words that may be spoken, and output symbols are symbols
 * which allow for semantic interpretation. For example, certain output symbols are actually
 * labels which map to script expressions (eScript expressions, similar to JavaScript).
 * These expressions are interpreted by a Semantic Processor in order to determine meaning, such as
 * spoken input symbol: "one", expression: "DIGIT.V='1'", semantic interpretation: "1".
 *
 * Refer to the SR_SemanticProcessor.h documentation to find out more about parsing, and about
 * semantic interpretation (eScript).
 */
typedef struct SR_SemanticGraph_t
{
  /**
   * Destroys a semantic graph.
   *
   * @param self SR_SemanticGraph handle
   */
  ESR_ReturnCode(*destroy)(struct SR_SemanticGraph_t* self);
  /**
   * Loads a semantic graph from disk.
   *
   * @param self SR_SemanticGraph handle
   * @param ilabels Input word labels to be used when building the graph (The should be the same as
   * the output word labels from the recognition graph/context.)
   * @param basename File to read graph from (.g2g image or basename for text files)
   * @param num_words_to_add Number of words to add dynamically (only applies when loading from text files)
   * @todo complete documentation
   */
  ESR_ReturnCode(*load)(struct SR_SemanticGraph_t* self, wordmap* ilabels, const LCHAR* basename, int num_words_to_add);
  /**
   * Unloads a semantic graph.
   *
   * @param self SR_SemanticGraph handle
  * @return ESR_SUCCESS
   */
  ESR_ReturnCode(*unload)(struct SR_SemanticGraph_t* self);
  
  /**
   * Saves the semantic graph as a binary image. 
   *
   * @param self SR_SemanticGraph handle
   * @param filename Name of the binary image file.
   * @param version_number Target file format version.
   */
  ESR_ReturnCode(*save)(struct SR_SemanticGraph_t* self, const LCHAR* filename, int version_number);
  
  /**
   * Adds a word to the semantic graph at the specified slot. Tag may be defined or NULL.
   *
   * @param self SR_SemanticGraph handle
   * @param slot Where to insert in graph (only ROOT supported right now)
   * @param word Word to add.
   * @param word Semantic Tag for the word.
   * @param maybeMultiMeaning Indicates that we MAY be adding alternate multiple meanings a previously added word
   */
  ESR_ReturnCode(*addWordToSlot)(struct SR_SemanticGraph_t* self, const LCHAR* slot, const LCHAR* word, const LCHAR* tag, const ESR_BOOL maybeMultiMeaning);
	/**
	 * Removes all words from the semantic graph.
	 *
	 * @param self SR_SemanticGraph handle
	 */
  ESR_ReturnCode(*reset)(struct SR_SemanticGraph_t* self);
}
SR_SemanticGraph;


/**
 * Create a new Semantic Graph
 *
 * @param self SR_SemanticGraph handle
 */
SREC_SEMPROC_API ESR_ReturnCode SR_SemanticGraphCreate(SR_SemanticGraph** self);

#endif /* __SR_SEMANTICGRAPH_H */
