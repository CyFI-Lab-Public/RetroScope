/*---------------------------------------------------------------------------*
 *  SR_Vocabulary.h                                                          *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                         *
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

#ifndef __SR_VOCABULARY_H
#define __SR_VOCABULARY_H



#include "ESR_Locale.h"
#include "ESR_ReturnCode.h"
#include "pstdio.h"
#include "SR_VocabularyPrefix.h"


/**
 * @addtogroup SR_VocabularyModule SR_Vocabulary API functions
 * A vocabulary maps words to their phonetic representation.
 *
 * @{
 */

/**
 * A vocabulary maps words to their phonetic representation.
 */
typedef struct SR_Vocabulary_t
{
  /**
  * Saves a vocabulary to file.
  *
  * @param self SR_Vocabulary handle
  * @param filename File to write to
  */
  ESR_ReturnCode(*save)(struct SR_Vocabulary_t* self, const LCHAR* filename);

  /**
   * Returns phonetic representation of word.
   *
   * @param self SR_Vocabulary handle
   * @param word Word to check for
	 * @param pronunciation [out] Phonetic representation of word
	 * @param len [in/out] Length of value argument. If the return code is
	 *            ESR_BUFFER_OVERFLOW, the required length is returned in this variable.
   */
  ESR_ReturnCode(*getPronunciation)(struct SR_Vocabulary_t* self, const LCHAR* word, LCHAR* pronunciation, size_t* len);

  /**
   * Returns vocabulary locale.
   *
   * @param self SR_Vocabulary handle
   * @param locale [out] Vocabulary locale
   */
  ESR_ReturnCode(*getLanguage)(struct SR_Vocabulary_t* self, ESR_Locale* locale);

  /**
  * Destroys a Vocabulary.
  *
  * @param self SR_Vocabulary handle
  */
  ESR_ReturnCode(*destroy)(struct SR_Vocabulary_t* self);
}
SR_Vocabulary;

/**
 * @name Vocabulary creation
 *
 * There are two ways to generate a vocabulary:
 *
 * 1. Load a vocabulary from disk. Phonemes are retrieved from a lookup table,
 *    and fall back on a TTP engine if necessary.
 * 2. Create an empty vocabulary. Phonemes are retrieved exclusively from a TTP engine.
 *
 * @{
 */

/**
 * Creates an empty Vocabulary using the specified language.
 *
 * @param locale
 * @param self SR_Vocabulary handle
 */
SREC_VOCABULARY_API ESR_ReturnCode SR_VocabularyCreate(ESR_Locale locale, SR_Vocabulary** self);

/**
 * Loads a vocabulary from file.
 *
 * @param self SR_Vocabulary handle
 * @param filename File to read from
 * @todo In the future, read language from the underlying vocabulary file
 */
SREC_VOCABULARY_API ESR_ReturnCode SR_VocabularyLoad(const LCHAR* filename, SR_Vocabulary** self);

/**
 * @}
 */

/**
 * Saves a vocabulary to file.
 *
 * @param self SR_Vocabulary handle
 * @param filename File to write to
 */
SREC_VOCABULARY_API ESR_ReturnCode SR_VocabularySave(SR_Vocabulary* self, const LCHAR* filename);

/**
 * Adds word to vocabulary.
 *
 * @param self SR_Vocabulary handle
 * @param word Word to be added
 * @todo Function purpose is unclear
 */
SREC_VOCABULARY_API ESR_ReturnCode SR_VocabularyAddWord(SR_Vocabulary* self, const LCHAR* word);


/**
 * Returns vocabulary locale.
 *
 * @param self SR_Vocabulary handle
 * @param locale [out] Vocabulary locale
 */
SREC_VOCABULARY_API ESR_ReturnCode SR_VocabularyGetLanguage(SR_Vocabulary* self, ESR_Locale* locale);

/**
 * Destroys a Vocabulary.
 *
 * @param self SR_Vocabulary handle
 */
SREC_VOCABULARY_API ESR_ReturnCode SR_VocabularyDestroy(SR_Vocabulary* self);

/**
 * Looks up a word to vocabulary.
 *
 * @param self SR_Vocabulary handle
 * @param word Word to be added
 * @param pronunciation resulting pronunication
 * @param len [in/out] Length of phoeme argument. If the return code is ESR_BUFFER_OVERFLOW,
 *            the required length is returned in this variable.
 */
SREC_VOCABULARY_API ESR_ReturnCode SR_VocabularyGetPronunciation(SR_Vocabulary* self, const LCHAR* word, LCHAR* pronunciation, size_t* len);

/**
 * @}
 */

/* To-Do: the following functions need to be removed.  The functions are still used in SR_NameTag.dll  */
SREC_VOCABULARY_API ESR_ReturnCode SR_Vocabulary_etiinf_conv_multichar(ESR_Locale locale, const LCHAR* input, LCHAR* output, size_t max_len);
SREC_VOCABULARY_API ESR_ReturnCode SR_Vocabulary_etiinf_conv_from_multichar(ESR_Locale locale, const LCHAR* input, LCHAR* output);

#endif /* __SR_VOCABULARY_H */
