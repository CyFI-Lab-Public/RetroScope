/*---------------------------------------------------------------------------*
 *  ESR_Locale.c  *
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


#include "ESR_Locale.h"
#include "plog.h"
#include "LCHAR.h"


LCHAR* ESR_locale2str(const ESR_Locale locale)
{
    switch (locale) {
        case ESR_LOCALE_EN_US: return L("ESR_LOCALE_EN_US");
        case ESR_LOCALE_FR_FR: return L("ESR_LOCALE_FR_FR");
        case ESR_LOCALE_DE_DE: return L("ESR_LOCALE_DE_DE");
        case ESR_LOCALE_EN_GB: return L("ESR_LOCALE_EN_GB");
        case ESR_LOCALE_IT_IT: return L("ESR_LOCALE_IT_IT");
        case ESR_LOCALE_NL_NL: return L("ESR_LOCALE_NL_NL");
        case ESR_LOCALE_PT_PT: return L("ESR_LOCALE_PT_PT");
        case ESR_LOCALE_ES_ES: return L("ESR_LOCALE_ES_ES");
        case ESR_LOCALE_JA_JP: return L("ESR_LOCALE_JA_JP");
    }
    return L("invalid locale code");
}


ESR_ReturnCode ESR_str2locale(const LCHAR* str, ESR_Locale* locale)
{
    int rtn = 0;
    if      (!lstrcasecmp(str, L("EN-US"), &rtn) && !rtn) *locale = ESR_LOCALE_EN_US;
    else if (!lstrcasecmp(str, L("FR-FR"), &rtn) && !rtn) *locale = ESR_LOCALE_FR_FR;
    else if (!lstrcasecmp(str, L("DE-DE"), &rtn) && !rtn) *locale = ESR_LOCALE_DE_DE;
    else if (!lstrcasecmp(str, L("EN-GB"), &rtn) && !rtn) *locale = ESR_LOCALE_EN_GB;
    else if (!lstrcasecmp(str, L("IT-IT"), &rtn) && !rtn) *locale = ESR_LOCALE_IT_IT;
    else if (!lstrcasecmp(str, L("NL-NL"), &rtn) && !rtn) *locale = ESR_LOCALE_NL_NL;
    else if (!lstrcasecmp(str, L("PT-PT"), &rtn) && !rtn) *locale = ESR_LOCALE_PT_PT;
    else if (!lstrcasecmp(str, L("ES-ES"), &rtn) && !rtn) *locale = ESR_LOCALE_ES_ES;
    else if (!lstrcasecmp(str, L("JA-JP"), &rtn) && !rtn) *locale = ESR_LOCALE_JA_JP;
    else {
        PLogError(L("no locale defined for %s"), str);
        return ESR_INVALID_ARGUMENT;
    }
    return ESR_SUCCESS;
}
