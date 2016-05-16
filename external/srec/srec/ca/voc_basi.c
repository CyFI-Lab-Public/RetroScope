/*---------------------------------------------------------------------------*
 *  voc_basi.c  *
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

#include <stdlib.h>
#include <string.h>
#ifndef _RTT
#include <stdio.h>
#endif

#ifdef unix
#include <unistd.h>
#endif
#include <assert.h>


#include "simapi.h"
#include "portable.h"

static const char voc_basi[] = "$Id: voc_basi.c,v 1.11.6.14 2008/01/21 20:30:05 dahan Exp $";

#define ADD_SUNDRY_LABELS 0
#define ALLOW_UNDERSCORES       1
#define MAX_WORD_LEN 128

CA_Vocab *CA_AllocateVocabulary(void)
{
  CA_Vocab *hVocab = NULL;
  
  TRY_CA_EXCEPT
  
  hVocab = (CA_Vocab *) CALLOC_CLR(1, sizeof(CA_Vocab), "ca.hVocab");
  hVocab->is_loaded = False;
  hVocab->ca_rtti = CA_VOCABULARY_SIGNATURE;
  return (hVocab);
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hVocab)
}


void CA_FreeVocabulary(CA_Vocab *hVocab)
{
  TRY_CA_EXCEPT
  ASSERT(hVocab);
  FREE((char *) hVocab);
  return;
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hVocab)
}

void CA_LoadDictionary(CA_Vocab *hVocab, const LCHAR *vocname, char *phtname, ESR_Locale* locale)
{
  TRY_CA_EXCEPT
#ifndef _RTT
  
  ASSERT(hVocab);
//  if (phtname != NULL && strlen(phtname) > 0)
//  hVocab->voc.pht_table= read_phoneme_table (phtname);

  if (0 <= read_word_transcription(vocname, &hVocab->voc, locale)) {
    hVocab->is_loaded = True;
  } else {
    hVocab->is_loaded = False;
  }
  return;
#else
  log_report("RTT not in module\n");
  SERVICE_ERROR(FEATURE_NOT_SUPPORTED);
  return;
#endif
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hVocab)
}

void CA_UnloadDictionary(CA_Vocab *hVocab)
{
  TRY_CA_EXCEPT
  ASSERT(hVocab);
  if (hVocab->is_loaded == False)
    SERVICE_ERROR(VOCAB_NOT_LOADED);
    
  delete_word_transcription(&hVocab->voc);
  
  hVocab->is_loaded = False;
  return;
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hVocab)
}


int CA_CheckEntryInDictionary(CA_Vocab *hVocab, const char *label)
{
  int pronCount;
  char prons[256];
  TRY_CA_EXCEPT
  ASSERT(hVocab);
  
  pronCount = get_prons(&hVocab->voc, label, prons, sizeof(prons));

  if (pronCount <= 0) {
    /* try lower case, the general convention for dictionaries */
    unsigned i;
    char lower[128];
    for (i = 0; label[i]; i++) {
      if (i >= sizeof(lower) - 1) return -1;
      lower[i] = tolower(label[i]);
    }
    lower[i] = 0;
    pronCount = get_prons(&hVocab->voc, lower, prons, sizeof(prons));
    if (pronCount <= 0) return False;
  }
   
  return (True);
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hVocab)
}

int CA_GetFullEntryInDictionary(CA_Vocab *hVocab, const char *label, char *pron, int *pronSize, int pronMaxSize);

int CA_GetEntryInDictionary(CA_Vocab *hVocab, const char *label, char *pron, int *pronSize, int pronMaxSize)
{
  int rc;
  TRY_CA_EXCEPT
  ASSERT(hVocab);
  rc = CA_GetFullEntryInDictionary(hVocab, label, pron, pronSize, pronMaxSize);
  return rc;
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hVocab)
}

/* this looks up the entry entire, underscores and all, eg "good_bye" */

int CA_GetFullEntryInDictionary(CA_Vocab *hVocab, const char *label, char *pron, int *pronSize, int pronMaxSize)
{
  TRY_CA_EXCEPT
  int pronCount;
  
  ASSERT(hVocab);
  
  pronCount = get_prons(&hVocab->voc, label, pron, pronMaxSize);
  if (pronCount <= 0)
  {
    /* try lower case, the general convention for dictionaries */
    unsigned i;
    char lower[128];
    for (i = 0; label[i]; i++) {
      if (i >= sizeof(lower) - 1) return -1;
      lower[i] = tolower((unsigned char)label[i]);
    }
    lower[i] = 0;
    pronCount = get_prons(&hVocab->voc, lower, pron, pronMaxSize);
    if (pronCount <= 0) return False;
  }
  //*pronSize = pronCount;
  
  return (True);
  
  BEG_CATCH_CA_EXCEPT
    END_CATCH_CA_EXCEPT(hVocab)
}
