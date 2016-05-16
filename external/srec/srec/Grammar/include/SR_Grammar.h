/*---------------------------------------------------------------------------*
 *  SR_Grammar.h  *
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

#ifndef __SR_GRAMMAR_H
#define __SR_GRAMMAR_H



#include "ESR_ReturnCode.h"
#include "pstdio.h"
#include "ptypes.h"
#include "SR_GrammarPrefix.h"
#include "SR_NametagDefs.h"
#include "SR_Vocabulary.h"
#include "SR_AcousticModels.h"
#include "SR_Recognizer.h"
#include "SR_SemanticResult.h"

/* forward decl needed because of SR_Recognizer.h <-> SR_Grammar.h include loop */
struct SR_Recognizer_t;

/**
 * @addtogroup SR_GrammarModule SR_Grammar API functions
 * Contains recognition grammar.
 *
 * A grammar consists of a list of rules.
 *
 * A rule consists of a list of words and slots. For example:
 * MY_RULE = "My name is $name" where "name" is a slot
 *
 * Words, Nametags may be added or removed from slots.
 * Upon adding and removing words, the grammar needs to be recompiled for the changes to
 * take place. However, the original CompiledGrammar remains valid even if compilation never
 * takes place.
 *
 * Two types of slots exist: word slots and nametag slots
 *
 * @{
 */

/**
 * Grammar dispatch function. Used for symantic processing.
 *
 * @param functionName Name of function that was invoked
 * @param argv Argument values passed to function
 * @param argc Number of arguments passed to function
 * @param value Dispatch value (specified using SR_GrammarSetDispatchValue)
 * @param result Result of function operation. Caller passes in this buffer, function fills it.
 * @param resultSize Size of result buffer. If the passed in buffer was not large enough to hold
 *                   the result, this value is updated with the required length.
 */
typedef ESR_ReturnCode(*SR_GrammarDispatchFunction)(LCHAR* functionName, LCHAR** argv, size_t argc, void* value, LCHAR* result, size_t* resultSize);

/**
 * Contains recognition grammar.
 *
 * A grammar consists of a list of rules.
 *
 * A rule consists of a list of words and slots. For example:
 * MY_RULE = "My name is $name" where "name" is a slot
 *
 * Words, Nametags may be added or removed from slots.
 * Upon adding and removing words, the grammar needs to be recompiled for the changes to
 * take place. However, the original CompiledGrammar remains valid even if compilation never
 * takes place.
 *
 * Two types of slots exist: word slots and nametag slots
 */
typedef struct SR_Grammar_t
{
  /**
   * Compiles the grammar.
   * In the case of a precompiled grammar, the function compiles those portions of the grammar 
   * that were dynamically added since the last compilation.
   *
   * @param self SR_Grammar handle
  * @return ESR_SUCCESS if compilation succeeded, ESR_FATAL_ERROR otherwise.
   */
  ESR_ReturnCode(*compile)(struct SR_Grammar_t* self);
  
  /**
  * Saves a compiled grammar to a file.
  *
  * @param self SR_Grammar handle
  * @param filename File to write grammar into
  * @return ESR_INVALID_ARGUMENT if self or filename are null; ESR_INVALID_STATE if could not save the grammar
  */
  ESR_ReturnCode(*save)(struct SR_Grammar_t* self, const LCHAR* filename);
  
  /**
   * Indicates if a transcription is a valid result of a Grammar rule.
   *
   * @param self SR_Grammar handle
   * @param transcription Transcription value
   * @param result [in/out] Array of semantic results to be populated
   * @param resultCount [in/out] Length of result array
  * @return ESR_INVALID_ARGUMENT if self or transcription are null; ESR_INVALID_STATE if an internal error has occured.
   */
  ESR_ReturnCode(*checkParse)(struct SR_Grammar_t* self, const LCHAR* transcription, SR_SemanticResult** result, size_t* resultCount);
  
  /**
   * Adds word to rule slot.
   *
   * @param self SR_Grammar handle
   * @param slot Slot name
   * @param word Word to be added to the slot
   * @param pronunciation Word pronunciation (optional). Pass NULL to omit.
   * @param weight value to associate with word when adding to grammar; use to determine cost when parsing
   * @param tag eScript semantic expression (tag) for the word  
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_STATE if the vocabulary is missing, 
   * if OSI logging fails; ESR_OUT_OF_MEMORY if word cannot be added to the grammar 
   * (addWords=X is too small); ESR_NOT_SUPPORTED if homonyms are added to the grammar
   */
  ESR_ReturnCode(*addWordToSlot)(struct SR_Grammar_t* self, const LCHAR* slot, const LCHAR* word, 
		const LCHAR* pronunciation, int weight, const LCHAR* tag);
  
  /**
   * Removes all elements from all slots.
   *
   * @param self SR_Grammar handle
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_STATE if resetting the slots or OSI logging fails
   */
  ESR_ReturnCode(*resetAllSlots)(struct SR_Grammar_t* self);
  
  /**
   * Adds nametag to rule slot.
   *
   * @param self SR_Grammar handle
   * @param slot Slot name
   * @param nametag Nametag to be added to the grammar
   * @param weight value to associate with nametag when adding to grammar; use to determine cost when parsing
   * @param tag eScript semantic expression (tag) for the nametag
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_STATE if the grammar is active, 
   * if the vocabulary is missing, if OSI logging fails; ESR_OUT_OF_MEMORY if word cannot be added to 
   * the grammar (addWords=X is too small); ESR_NOT_SUPPORTED if homonyms are added to the grammar
   */
  ESR_ReturnCode(*addNametagToSlot)(struct SR_Grammar_t* self, const LCHAR* slot, 
    const SR_Nametag* nametag, int weight, const LCHAR* tag);
  
  /**
   * Sets user dispatch function (used for parsed callback, etc)
   *
   * @param self SR_Grammar handle
   * @param name The name of the function which will trigger this callback when encountered during grammar parsing.
   * @param userData The user data to be referenced in the callback implementation later on.
   * @param function The dispatch function
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_SUCCESS
   */
  ESR_ReturnCode(*setDispatchFunction)(struct SR_Grammar_t* self, const LCHAR* name, void* userData, SR_GrammarDispatchFunction function);
  
  /**
   * Sets grammar parameter, overriding session defaults.
   *
   * @param self SR_Grammar handle
   * @param key Parameter name
   * @param value Parameter value
  * @return ESR_INVALID_ARGUMENT if self is null; ESR_NOT_IMPLEMENTED
   */
  ESR_ReturnCode(*setParameter)(struct SR_Grammar_t* self, const LCHAR* key, void* value);
  
  /**
   * Sets grammar parameters.
   * This is a convenience function.
   *
   * @param self SR_Grammar handle
   * @param key Parameter name
   * @param value Parameter value
  * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_RESULT_TYPE if the property is already set and its type is size_t
   */
  ESR_ReturnCode(*setSize_tParameter)(struct SR_Grammar_t* self, const LCHAR* key, size_t value);
  
  /**
   * Returns grammar parameter value.
   *
   * @param self SR_Grammar handle
   * @param key Parameter name
   * @param value Parameter value
  * @return ESR_INVALID_ARGUMENT if self is null; ESR_NOT_IMPLEMENTED
   */
  ESR_ReturnCode(*getParameter)(struct SR_Grammar_t* self, const LCHAR* key, void** value);
  
  /**
   * Return copy of unsigned int grammar parameter.
   * This is a convenience function.
   *
   * @param self SR_Grammar handle
   * @param key Parameter name
   * @param value [out] Used to hold the parameter value
   * @param len [in/out] Length of value argument. If the return code is ESR_BUFFER_OVERFLOW,
   *            the required length is returned in this variable.
  * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_RESULT_TYPE if the property type is not size_t; ESR_NO_MATCH_ERROR if property is not set
   */
  ESR_ReturnCode(*getSize_tParameter)(struct SR_Grammar_t* self, const LCHAR* key, size_t* value);
  
  /**
   * Configures a vocabulary with the grammar.
   *
   * @param self SR_Grammar handle
   * @param vocabulary The vocabulary to associate with
  * @return ESR_INVALID_ARGUMENT if self or vocabulary are null
   */
  ESR_ReturnCode(*setupVocabulary)(struct SR_Grammar_t *self, SR_Vocabulary *vocabulary);
  
  /**
   * Associates Recognizer with the grammar.
   *
   * @param self SR_Grammar handle
   * @param recognizer The recognizer to associate
   * @return ESR_INVALID_ARGUMENT if self or recognizer are null
   */
  // ESR_ReturnCode(*setupModels)(struct SR_Grammar_t* self, SR_AcousticModels* models);
  ESR_ReturnCode(*setupRecognizer)(struct SR_Grammar_t* self, struct SR_Recognizer_t* recognizer);
  /**
   * Dissociates Recognizer with the grammar.
   *
   * @param self SR_Grammar handle
   * @return ESR_INVALID_ARGUMENT if self is null
   */
  ESR_ReturnCode(*unsetupRecognizer)(struct SR_Grammar_t* self);
  
  /**
   * Returns AcousticModels associated with the grammar.
   *
   * @param self SR_Grammar handle
   * @param models Associated models
  * @return ESR_INVALID_ARGUMENT if self or models are null
   */
  // ESR_ReturnCode(*getModels)(struct SR_Grammar_t* self, SR_AcousticModels** models);
  
  /**
   * Destroys a grammar.
   *
   * @param self SR_Grammar handle
  * @return ESR_INVALID_ARGUMENT if self is null, ESR_INVALID_STATE if OSI logging fails
   */
  ESR_ReturnCode(*destroy)(struct SR_Grammar_t* self);
}
SR_Grammar;

/**
 * @name Grammar compilation
 *
 * Categories:
 * - Initialization
 * - Compile from expressions (not supported by SREC) or load pre-compiled grammars
 * - Dynamic modification; slots
 * - Support functions (isExtant, etc)
 * - Use in recognition; activation
 *
 * IMPORTANT NOTE: There are two main approaches to grammar activation in a recognizer.
 * - 1. Load a pre-compiler grammar setup. This is not just the set of expressions but is also
 *      an image of the actual model components network that fully describes the recognizer
 *      task. This type of Grammar can be extended with single arcs at pre-defined points
 *      called slots.
 *
 * - 2. Create a network dynamically from a set of regular expressions.
 *
 * SREC supports 1. but not 2. CREC supports 2 but not 1. Both approaches are covered by
 * this interface. Pre-compiled grammars inherently refer to models. It is therefore
 * important to ensure consistency of model usage between all activated grammars. This can
 * be done prior to grammar rule activation in the Recognizer
 * (see SR_RecognizerCheckGrammarConsistency()).
 *
 * A Grammar may consist of one or more rules. Rules are given as expressions (this interface is
 * independent of the format). A rule may contain other rules. Before a rule can be used in
 * recognition it must be compiled (or loaded), setup by a recognizer and activated.
 * -  The Grammar_CompileRule() step combines all sub-rule expressions into a single underlying
 *    member of Grammar.
 * -  The Recognizer_ActivateRule() step simply raises a flat to make a compiled rule available
 *    for recognition.
 *
 * Once a Grammar is setup by a recognizer it is not permissible to modify its rules  Thus, in
 * order to be able to support a combination of a static rule and one that requires changing, it
 * is most efficient to separate these rules into two Grammar objects.
 *
 * NOTE: The modification/setup constraint ensures consistency between the rule definitions
 * in the Grammar and the setup rules. It would be possible to remove this constraint and
 * allow rules that had no dependents to be modified while the grammar was setup. This makes
 * the API freer but also less consistent and more susceptible to error. There would be no
 * footprint cost with having two grammars in place of one grammar with two rules unless the
 * rules overlapped. If there was overlap then it might have been possible to minimize the
 * shared parts
 * @{
 */

/**
 * Compiles the grammar.
 * In the case of a precompiled grammar, the function compiles those portions of the grammar
 * that were dynamically added since the last compilation.
 *
 * @param self SR_Grammar handle
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_SUCCESS if compilation succeeded, ESR_FATAL_ERROR otherwise.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarCompile(SR_Grammar* self);
/**
 * @}
 *
 * @name Special "Slot"-based Grammar functions
 *
 * Slots are points in a pre-compiled grammar where a simple extension of the grammar may
 * be made. They support the insertion of words or nametags into a pr-defined position in
 * the Grammar. Slots are first declared in an expression which is compiled and saved for
 * re-loading. The names of these slots are used in a similar way as rule names.
 *
 * @{
 */
/**
 * Adds word to rule slot.
 *
 * @param self SR_Grammar handle
 * @param slot Slot name
 * @param word Word to be added to the slot
 * @param pronunciation Word pronunciation (optional). Pass NULL to omit.
 * @param weight value to associate with word when adding to grammar; use to determine cost when parsing
 * @param tag eScript semantic expression for the word. In other words, eScript will execute
 *            "MEANING=<tag>"
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_STATE if the grammar is active,
 * if the vocabulary is missing, if OSI logging fails; ESR_OUT_OF_MEMORY if word cannot be added to
 * the grammar (addWords=X is too small); ESR_NOT_SUPPORTED if homonyms are added to the grammar
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarAddWordToSlot(SR_Grammar* self, const LCHAR* slot, 
																												const LCHAR* word, const LCHAR* pronunciation, 
																												int weight, const LCHAR* tag);
/**
 * Removes all elements from all slots.
 *
 * @param self SR_Grammar handle
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_STATE if resetting the slots or 
 * OSI logging fails
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarResetAllSlots(SR_Grammar* self);
/**
 * Adds word to rule slot.
 *
 * @param self SR_Grammar handle
 * @param slot Slot name
 * @param nametag Nametag to be added to the slot
 * @param weight value to associate with nametag when adding to grammar; use to determine cost when parsing
 * @param tag eScript semantic expression (tag) for the nametag
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_STATE if the grammar is active, if the
 * vocabulary is missing, if OSI logging fails; ESR_OUT_OF_MEMORY if word cannot be added to the
 * grammar (addWords=X is too small); ESR_NOT_SUPPORTED if homonyms are added to the grammar
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarAddNametagToSlot(SR_Grammar* self, const LCHAR* slot, 
const struct SR_Nametag_t* nametag, int weight, const LCHAR* tag);
/**
 * @}
 *
 * @name Grammar Setup functions
 *
 * The Grammar object needs an association with several objects:
 * - A Grammar object must use one and only one Vocabulary object.
 * - A Grammar object may use one and only one Nametags object. (The Nametags object can
 *   however be used by more than one Grammar. A Nametags collection object must be used
 *   before nametags can be added to Grammar slots.)
 *
 * @see Nametags_Add() and associated functions for Nametags management.
 *
 * @{
 */

/**
 * Configures a vocabulary with the grammar.
 *
 * @param self SR_Grammar handle
 * @param vocabulary The vocabulary to associate with
 * @return ESR_INVALID_ARGUMENT if self or vocabulary are null
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarSetupVocabulary(SR_Grammar *self, SR_Vocabulary *vocabulary);
/**
 * Associates Grammar with a Recognizer (eg. such that word additions can take place).
 *
 * @param self SR_Grammar handle
 * @param models The recognizer to associate
 * @return ESR_INVALID_ARGUMENT if self or recognizer are null
 */
// SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarSetupModels(SR_Grammar* self, SR_AcousticModels* models);
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarSetupRecognizer(SR_Grammar* self, struct SR_Recognizer_t* recognizer);
/**
 * Dissociate Grammar from a Recognizer.
 *
 * @param self SR_Grammar handle
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarUnsetupRecognizer(SR_Grammar* self);
/**
 * Returns AcousticModels associated with a Grammar.
 *
 * @param self SR_Grammar handle
 * @param models Associated models
 * @return ESR_INVALID_ARGUMENT if self or models are null
 */
// SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarGetModels(SR_Grammar* self, SR_AcousticModels** models);

/**
 * @}
 *
 * @name  Basic Grammar functions
 *
 * @{
 */

/**
 * Create a new grammar.
 *
 * @param self SR_Grammar handle
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarCreate(SR_Grammar** self);
/**
 * Destroys a grammar.
 *
 * @param self SR_Grammar handle
 * @return ESR_INVALID_ARGUMENT if self is null, ESR_INVALID_STATE if OSI logging fails
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarDestroy(SR_Grammar* self);
/**
 * Loads a compiled grammar from a file or an image. If the filename has extention .g2g
 * then it will be loaded as an image. Otherwise, provide only a basename (i.e. without
 * file extensions), and a set of text-based grammar files will be loaded.
 *
 * @param filename File to read grammar from
 * @param self SR_Grammar handle
 * @return ESR_INVALID_ARGUMENT if self or the value it points to are null. If the filename load property
 * (i.e. addWords=X) is unknown; ESR_OUT_OF_MEMORY if system is out of memory; ESR_READ_ERROR if grammar file be read
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarLoad(const LCHAR* filename, SR_Grammar** self);
/**
 * Saves a compiled grammar to a file.
 *
 * @param self SR_Grammar handle
 * @param filename File to write grammar into
 * @return ESR_INVALID_ARGUMENT if self or filename are null; ESR_INVALID_STATE if could not save the grammar
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarSave(SR_Grammar* self, const LCHAR* filename);
/**
 * Sets user dispatch function (used for parsed callback, etc)
 *
 * @param self SR_Grammar handle
 * @param name The name of the function which will trigger this callback when encountered during grammar parsing.
 * @param userData The user data to be referenced in the callback implementation later on.
 * @param function The dispatch function
 * @return ESR_SUCCESS
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarSetDispatchFunction(SR_Grammar* self, const LCHAR* name, void* userData, SR_GrammarDispatchFunction function);
/**
 * Sets grammar parameter, overriding session defaults.
 *
 * @param self SR_Grammar handle
 * @param key Parameter name
 * @param value Parameter value
 * @return ESR_NOT_IMPLEMENTED
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarSetParameter(SR_Grammar* self, const LCHAR* key, void* value);
/**
 * Sets grammar parameters.
 * This is a convenience function.
 *
 * @param self SR_Grammar handle
 * @param key Parameter name
 * @param value Parameter value
 * @return ESR_INVALID_RESULT_TYPE if the property is already set and its type is size_t
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarSetSize_tParameter(SR_Grammar* self, const LCHAR* key, size_t value);
/**
 * Returns grammar parameter value.
 *
 * @param self SR_Grammar handle
 * @param key Parameter name
 * @param value Parameter value
 * @return ESR_NOT_IMPLEMENTED
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarGetParameter(SR_Grammar* self, const LCHAR* key, void** value);
/**
 * Return copy of unsigned int grammar parameter.
 * This is a convenience function.
 *
 * @param self SR_Grammar handle
 * @param key Parameter name
 * @param value [out] Used to hold the parameter value
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_INVALID_RESULT_TYPE if the property type is not size_t; ESR_NO_MATCH_ERROR if property is not set
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarGetSize_tParameter(SR_Grammar* self, const LCHAR* key, size_t* value);
/**
 * Checks if transcription is parsable by the grammar.
 *
 * @param self SR_Grammar handle
 * @param transcription transcription to be checked
 * @param result should be NULL
 * @param resultCount used to return the number of valid parses
 * @return ESR_INVALID_ARGUMENT if self, transcription are null; ESR_INVALID_STATE if an internal error has occured.
 */
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarCheckParse(SR_Grammar* self, const LCHAR* transcription, SR_SemanticResult** result, size_t* resultCount);
/**
 * @}
 */

/**
 * @}
 */

SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarAllowOnly(SR_Grammar* self, const char* transcription);
SREC_GRAMMAR_API ESR_ReturnCode SR_GrammarAllowAll(SR_Grammar* self);

#endif /* __SR_GRAMMAR_H */
