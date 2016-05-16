/*---------------------------------------------------------------------------*
 *  ESR_Locale.h  *
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

#ifndef __ESR_LOCALE_H
#define __ESR_LOCALE_H



#include "ESR_SharedPrefix.h"
#include "ptypes.h"

/**
 * @addtogroup ESR_LocaleModule ESR_Locale API functions
 * Locale support functions.
 *
 * @{
 */

/**
 * List of locales.
 */
typedef enum
{
  /**
   * US English
   */
  ESR_LOCALE_EN_US,
  
  /**
   * France French
   */
  ESR_LOCALE_FR_FR,
  
  /**
   * Germany German
   */
  ESR_LOCALE_DE_DE,
  
  /**
   * UK English
   */
  ESR_LOCALE_EN_GB,
  
  /* others */
  ESR_LOCALE_IT_IT,
  ESR_LOCALE_NL_NL,
  ESR_LOCALE_PT_PT,
  ESR_LOCALE_ES_ES,
  ESR_LOCALE_JA_JP
  
} ESR_Locale;

/**
 * Given a locale, returns its string representation.
 *
 * @param locale The locale to translate
 * @return Locale string
 */
ESR_SHARED_API LCHAR* ESR_locale2str(const ESR_Locale locale);

/**
 * Given a locale's string representation, returns the associated ESR_Locale handle.
 *
 * @param str String representation of locale
 * @param locale [out] Resulting locale
 * @return ESR_INVALID_ARGUMENT if specified local was not recognized
 */
ESR_SHARED_API ESR_ReturnCode ESR_str2locale(const LCHAR* str, ESR_Locale* locale);

/**
 * @}
 */

#endif /* __ESR_LOCALE_H */
