/*---------------------------------------------------------------------------*
 *  SR_VocabularyImpl.h  *
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

#ifndef __SR_VOCABULARYIMPL_H
#define __SR_VOCABULARYIMPL_H



#include <stdlib.h>
#include "ESR_ReturnCode.h"
#include "HashMap.h"
#ifdef USE_TTP
#include "SWIslts.h"
#endif /* USE_TTP */


#include "simapi.h"

/**
 * Vocabulary implementation.
 */
typedef struct SR_VocabularyImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  SR_Vocabulary Interface;
  /**
   * Legacy CREC vocabulary.
   */
  LCHAR* filename;
  CA_Vocab* vocabulary;
  /**
   * Vocabulary locale.
   */
  ESR_Locale locale;
  /**
   * String to identify TTP language associated with locale.
   */
  LCHAR *ttp_lang;
  
#ifdef USE_TTP
  /**
   * Handle to a TTP engine.
   */
  SWIsltsHand hSlts;
#endif /* USE_TTP */
}
SR_VocabularyImpl;


/**
 * Default implementation.
 */
ESR_ReturnCode SR_VocabularyCreateImpl(SR_Vocabulary** self);
/**
 * Default implementation.
 */
ESR_ReturnCode SR_VocabularyLoadImpl(const LCHAR* filename, SR_Vocabulary** self);
/**
 * Default implementation.
 */
ESR_ReturnCode SR_VocabularySaveImpl(SR_Vocabulary* self, const LCHAR* filename);
/**
 * Default implementation.
 */
ESR_ReturnCode SR_VocabularyAddWordImpl(SR_Vocabulary* self, const LCHAR* word);
/**
 * Default implementation.
 */
ESR_ReturnCode SR_VocabularyDeleteWordImpl(SR_Vocabulary* self, const LCHAR* word);
/**
 * Default implementation.
 */
ESR_ReturnCode SR_VocabularyContainsWordImpl(SR_Vocabulary* self, const LCHAR* word, ESR_BOOL* result);
/**
 * Default implementation.
 */
ESR_ReturnCode SR_VocabularyGetPronunciationImpl(SR_Vocabulary* self, const LCHAR* word, LCHAR* phoneme, size_t* len);
/**
 * Default implementation.
 */
ESR_ReturnCode SR_VocabularyGetLanguageImpl(SR_Vocabulary* self, ESR_Locale* locale);
/**
 * Default implementation.
 */
ESR_ReturnCode SR_VocabularyDestroyImpl(SR_Vocabulary* self);
/**
 * Default implementation.
 */
ESR_ReturnCode SR_CreateG2P(SR_Vocabulary* self);
/**
 * Default implementation.
 */
ESR_ReturnCode SR_DestroyG2P(SR_Vocabulary* self);

/* TODO change this later if we get other languages to support*/
#define TTP_LANG(locale) locale == ESR_LOCALE_EN_US ? L("enu") : \
                         locale == ESR_LOCALE_FR_FR ? L("fra") : \
                         locale == ESR_LOCALE_DE_DE ? L("deu") : \
                         locale == ESR_LOCALE_EN_GB ? L("eng") : \
                         locale == ESR_LOCALE_JA_JP ? L("jpn") : \
                         locale == ESR_LOCALE_NL_NL ? L("nln") : \
                         locale == ESR_LOCALE_IT_IT ? L("ita") : \
                         locale == ESR_LOCALE_ES_ES ? L("esp") : \
                         locale == ESR_LOCALE_PT_PT ? L("ptp") : \
                         L("enu")  /* en-us is default */

#endif /* __SR_VOCABULARYIMPL_H */
