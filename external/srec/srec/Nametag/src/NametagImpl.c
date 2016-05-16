
/*---------------------------------------------------------------------------*
 *  NametagImpl.c  *
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

#include "SR_Nametag.h"
#include "SR_NametagImpl.h"
#include "SR_RecognizerImpl.h"
#include "SR_VocabularyImpl.h"
#include "plog.h"
#include "pmemory.h"

#define MTAG NULL
#define MAX_STRING_LEN P_PATH_MAX

ESR_ReturnCode SR_NametagCreate(const SR_RecognizerResult* result, const LCHAR* id, SR_Nametag** self)
{
  ESR_Locale locale;
  ESR_ReturnCode rc;
  size_t len;
  LCHAR transcription[MAX_STRING_LEN];

  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  
  rc = result->getSize(result, &len);
  if (rc != ESR_SUCCESS)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  if (len < 1)
  {
    PLogError(L("ESR_INVALID_ARGUMENT (recognition result nbest-list size=0)"));
    rc = ESR_INVALID_ARGUMENT;
    goto CLEANUP;
  }
  rc = result->getLocale(result, &locale);
  
  len = MAX_STRING_LEN;

  rc = result->getValue(result, 0, L("meaning"), transcription, &len);

  if (rc != ESR_SUCCESS && rc != ESR_BUFFER_OVERFLOW)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }

#if USE_HMM_BASED_ENROLLMENT /* srec_context.h */
  len = LSTRLEN(transcription)+1;
  rc = SR_NametagCreateFromValue(id, transcription, (int)len, self);
  if(rc ) goto CLEANUP;
#else
  
  if(1) {
    LCHAR short_pron[MAX_STRING_LEN], *short_pron_ptr;
    LCHAR* long_pron = transcription;
    LCHAR* multichar;
    LCHAR* p;
    LCHAR singlechar[2];
    
    *short_pron = 0;
    short_pron_ptr = short_pron;
    len = LSTRLEN(L("ph_"));
    for (multichar = strtok(long_pron, L(" \t\n\r")); multichar; multichar = strtok(NULL, L(" \t\n\r")))
      {
	p = multichar;
	if (LSTRNCMP(p, L("ph_"), len) != 0)
	  {
	    PLogError(L("Expecting 'ph_' prefix, got=%s"), p);
	    rc = ESR_INVALID_STATE;
	    goto CLEANUP;
	  }
	p += len;
	multichar = p;
	while (*p)
	  {
	    if (isdigit(*p))
	      {
		*p = L('\0');
		break;
	      }
	    ++p;
	  }
	if ((rc = SR_Vocabulary_etiinf_conv_from_multichar(locale, multichar, singlechar)) != ESR_SUCCESS)
	  {
	    PLogError(L("Could not convert long to short pron (input=%s, locale=%s)"), multichar, ESR_locale2str(locale));
	    goto CLEANUP;
	  }
	singlechar[1] = 0;
	if((short_pron_ptr - short_pron + 3) >= MAX_STRING_LEN) {
	  PLogError(L("Chopping too long pron in SR_NametagCreate()\n"));
	  break; // just cut if off
	}
	*short_pron_ptr++ = *singlechar;
      }
    *short_pron_ptr++ = 0; // null-term
    *short_pron_ptr++ = 0; // double-null-term!

    /* +2 = +1 for null, +1 for double-null */
    rc = SR_NametagCreateFromValue(id, short_pron, (short_pron_ptr-short_pron), self);
    if(rc ) goto CLEANUP;
  }
#endif

  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_NametagCreateFromValue(const LCHAR* id, const char* value, size_t len, SR_Nametag** self)
{
  SR_NametagImpl* impl;
  ESR_ReturnCode rc;
  
  passert(self != NULL);
  impl = NEW(SR_NametagImpl, MTAG);
  if (impl == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }
  
  impl->Interface.setID = &SR_Nametag_SetID;
  impl->Interface.getID = &SR_Nametag_GetID;
  impl->Interface.getValue = &SR_Nametag_GetValue;
  impl->Interface.clone = &SR_Nametag_Clone;
  impl->Interface.destroy = &SR_Nametag_Destroy;
  impl->id = NULL;
  impl->value = NULL;
  impl->value = (LCHAR*) MALLOC(sizeof(LCHAR) * (len), MTAG);

  if (impl->value == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  impl->value_len = len;
  // make sure we have a double-null term
  memcpy( (void*)impl->value, value, len); 
  LSTRNCPY(impl->value, value, len); 
  
  rc = SR_NametagSetID(&impl->Interface, id);
  if (rc != ESR_SUCCESS)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  
  *self = (SR_Nametag*) impl;
  return ESR_SUCCESS;
CLEANUP:
  impl->Interface.destroy(&impl->Interface);
  return rc;
}

ESR_ReturnCode SR_Nametag_Destroy(SR_Nametag* self)
{
  SR_NametagImpl* impl = (SR_NametagImpl*) self;
  if (impl->value != NULL)
  {
    FREE(impl->value);
    impl->value = NULL;
  }
  if (impl->id != NULL)
  {
    FREE(impl->id);
    impl->id = NULL;
  }
  FREE(impl);
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_Nametag_GetID(const SR_Nametag* self, LCHAR** id)
{
  SR_NametagImpl* impl = (SR_NametagImpl*) self;
  
  *id = impl->id;
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_Nametag_GetValue(const SR_Nametag* self, const char** pvalue, size_t* plen)
{
  SR_NametagImpl* impl = (SR_NametagImpl*) self;
  
  *pvalue = (const char*)impl->value;
  if(!impl->value) 
      return ESR_NO_MATCH_ERROR;
  *plen = impl->value_len;
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_Nametag_SetID(SR_Nametag* self, const LCHAR* id)
{
  SR_NametagImpl* impl = (SR_NametagImpl*) self;
  ESR_ReturnCode rc;
  
  FREE(impl->id);
  impl->id = (LCHAR*) MALLOC(sizeof(LCHAR) * (LSTRLEN(id) + 1), MTAG);
  if (impl->id == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  LSTRCPY(impl->id, id);
  
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_Nametag_Clone(const SR_Nametag* self, SR_Nametag** result)
{
  SR_NametagImpl* impl = (SR_NametagImpl*) self;
  ESR_ReturnCode rc;
  
  CHKLOG(rc, SR_NametagCreateFromValue(impl->id, impl->value, LSTRLEN(impl->value)+1, result));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}
