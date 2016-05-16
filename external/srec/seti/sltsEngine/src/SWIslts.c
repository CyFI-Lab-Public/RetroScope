/*---------------------------------------------------------------------------*
 *  SWIslts.c  *
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

#define _IN_SWI_SLTS__

#ifdef SWISLTS_USE_STATIC_API
#define SWISLTS_FNEXPORT
#else
#ifdef WIN32
#include <windows.h>
#define SWISLTS_FNEXPORT __declspec(dllexport)
#else
#define SWISLTS_FNEXPORT
#endif
#endif

#include <stdlib.h>


#include <string.h>

#include "pmemory.h"
#include "PFile.h"
#include "plog.h"

#include "SWIslts.h"
#include "lts.h"

/**
 * Phone map table
 */
typedef struct SLTS_PhoneMap_t{
    LCHAR *src;
    LCHAR *des;
} SLTS_PhoneMap;

#define INF_SILENCE_OPTIONAL         (const char *)"&"

static const SLTS_PhoneMap g_aPhoneMap[] = {
     {"PS", "&"},
     {"SS0", ""},
     {"SS1", ""},
     {"SS2", ""},
     {"WS", "&"}
};
static const int g_numPhones = sizeof(g_aPhoneMap) / sizeof(g_aPhoneMap[0]);

#ifdef USE_STATIC_SLTS 
#define MAX_INPUT_LEN 255
static SLTS_Engine g_sltsEngine;
static SWIsltsWrapper g_sltsWrapper;
#endif

#define MAX_PRON_LEN      255
#define MAX_PHONE_LEN     4

static SWIsltsResult GetPhoneStr(SLTS_Engine *pEng, char *apszPhones[], int num_phones, char *pszPhoneStr, size_t *len);

SWISLTS_FNEXPORT SWIsltsResult SWIsltsGetWrapper(SWIsltsWrapper **ppLtsWrap)
{
  if (ppLtsWrap != NULL) {
#ifdef USE_STATIC_SLTS
    *ppLtsWrap = &g_sltsWrapper;
#else
    *ppLtsWrap = MALLOC(sizeof(SWIsltsWrapper), MTAG);
    if (*ppLtsWrap == NULL) {
      return SWIsltsErrAllocResource;
    }
#endif
    (*ppLtsWrap)->init = SWIsltsInit;
    (*ppLtsWrap)->term = SWIsltsTerm;
    (*ppLtsWrap)->open = SWIsltsOpen;
    (*ppLtsWrap)->close = SWIsltsClose;
    (*ppLtsWrap)->textToPhone = SWIsltsTextToPhone;
  }
  return SWIsltsSuccess;
}

SWISLTS_FNEXPORT SWIsltsResult SWIsltsReleaseWrapper(SWIsltsWrapper *pLtsWrap)
{
#ifndef USE_STATIC_SLTS
  if (pLtsWrap != NULL) {
    FREE(pLtsWrap);
    pLtsWrap = NULL;
  }
#endif
  return SWIsltsSuccess;
}

/* External Core SLTS API implementation */
SWISLTS_FNEXPORT SWIsltsResult SWIsltsInit()
{
  return SWIsltsSuccess;
}

SWISLTS_FNEXPORT SWIsltsResult SWIsltsTerm()
{
  return SWIsltsSuccess;
}

/* create a new instance of SLTS */
SWISLTS_FNEXPORT SWIsltsResult SWIsltsOpen(SWIsltsHand *phLts, 
                                           const char *data_filename)     
{
  SLTS_Engine      * pEng;
  SWIsltsResult      nRes = SWIsltsSuccess;

  if ((phLts == NULL)
#ifndef USE_STATIC_SLTS
    || (data_filename == NULL)
#endif 	
    ) 
  {
    return SWIsltsInvalidParam;
  }

#ifdef USE_STATIC_SLTS 
  pEng = &g_sltsEngine;
#else
  pEng = CALLOC(1, sizeof(SLTS_Engine), MTAG);
  if (pEng == NULL) {
    return SWIsltsErrAllocResource;
  }
#endif

  /* initialize */
  nRes = create_lts((char *)data_filename, &pEng->m_hLts);
  if (nRes != SWIsltsSuccess) {
    PLogError(L("create_lts with the model file (%s) fails with return code %d\n"), (char *)data_filename, nRes);
    goto CLEAN_UP;
  }

  *phLts = (SWIsltsHand)pEng;
  
  return SWIsltsSuccess;

 CLEAN_UP:
  if (*phLts != NULL) {
    SWIsltsClose(*phLts);
  }
  
  return nRes;
}

/* deletes given instance of SLTS */
SWISLTS_FNEXPORT SWIsltsResult SWIsltsClose(SWIsltsHand hLts)
{
  SLTS_Engine *pEng = (SLTS_Engine *)hLts;
  if (pEng == NULL) {
    return SWIsltsInvalidParam;
  }

  /* clean up internal buffers and slts structure */
  if (pEng->m_hLts) {
    free_lts(pEng->m_hLts);
  }
  pEng->m_hLts = NULL;

#ifndef USE_STATIC_SLTS 
  FREE(pEng);
#endif

  pEng = NULL;

  return SWIsltsSuccess;
}

/* send phones to internal buffer */
SWISLTS_FNEXPORT SWIsltsResult SWIsltsTextToPhone(SWIsltsHand hLts, 
                                               const char *text, 
                                               char *output_phone_string[],
                                               int *output_phone_len,
                                               int max_phone_len)
{
  int i;
  SWIsltsResult          nRes = SWIsltsSuccess;
#ifdef USE_STATIC_SLTS  
  char new_text[MAX_INPUT_LEN];
#else
  char *new_text;
#endif

  SLTS_Engine *pEng;
  if (hLts == NULL) {
    return SWIsltsInvalidParam;
  }

  if (text == NULL) {
    return SWIsltsInvalidParam;
  }
  
  /* check that the output phone string param is allocated */
  for(i=0; i<max_phone_len; i++){
    if(output_phone_string[i] == NULL)
      return SWIsltsInvalidParam;
  }

  pEng = (SLTS_Engine *)hLts;

  /* get rid newlines, tabs, and spaces, if any */
#ifdef USE_STATIC_SLTS 
  if((strlen(text)+1) > MAX_INPUT_LEN){  
    return SWIsltsMaxInputExceeded;
  }
#else  
  new_text = MALLOC((strlen(text)+1)*sizeof(char), MTAG);
  if (new_text == NULL) {
    PLogError(L("SWISLTS_OUT_OF_MEMORY"));
    return SWIsltsErrAllocResource;        
  }
#endif

  strcpy(new_text, text);
  i = strlen(new_text)-1;
  while(new_text[i] == '\n' || new_text[i] == ' ' || new_text[i] == '\t') i--;
  new_text[i+1] = '\0'; 

  /* now check if the input string is empty */
  if(strlen(new_text) == 0){
    *output_phone_len = 0;
    nRes = SWIsltsEmptyPhoneString;
    goto CLEAN_UP;
  }

  *output_phone_len = max_phone_len;
  nRes = run_lts(pEng->m_hLts, pEng->m_hDict, new_text, output_phone_string, output_phone_len);
  if (nRes != SWIsltsSuccess) {
    goto CLEAN_UP;
  }

#ifndef USE_STATIC_SLTS 
  if(new_text){
    FREE(new_text);
  }
  new_text = NULL;
#endif

  return SWIsltsSuccess;

 CLEAN_UP:

#ifndef USE_STATIC_SLTS 
  if(new_text){
    FREE(new_text);
  }
  new_text = NULL;
#endif

  return nRes;
}

SWISLTS_FNEXPORT SWIsltsResult SWIsltsG2PGetWordTranscriptions(SWIsltsHand hLts, 
                                                               const char *text, 
                                                               SWIsltsTranscription **ppTranscriptions,
                                                               int *pnNbrOfTranscriptions)
{
  SWIsltsResult          nRes = SWIsltsSuccess;
  char                  PHONE_STRING[MAX_PRON_LEN][MAX_PHONE_LEN];
  char                * phone_string[MAX_PRON_LEN];   
  SLTS_Engine          * pEng = (SLTS_Engine *)hLts;
  int                    i;
  int                    num_phones = 0;
  SWIsltsTranscription * pTranscription = NULL;
  int                    nNbrOfTranscriptions = 0;
  int                  * pnNbrOfTranscriptionsToSave = NULL;
  LCHAR                * pBlock = NULL;

  for( i = 0; i < MAX_PRON_LEN; i++ ) {
    phone_string[i] = PHONE_STRING[i]; 
  }

  nRes = SWIsltsTextToPhone(hLts, text, phone_string, &num_phones, MAX_PRON_LEN);
  if( nRes != SWIsltsSuccess ) {
    PLogError(L("SWIsltsTextToPhone( ) fails with return code %d\n"), nRes);
    goto CLEAN_UP;
  }
#if DEBUG
  pfprintf(PSTDOUT,"number of phones: %d\n ", num_phones);
  for( i = 0; i < num_phones; i++ ) {
    pfprintf(PSTDOUT,"%s ", phone_string[i]);
  }
  pfprintf(PSTDOUT,"\n ");
#endif

  /* only one transcription available from seti */
  nNbrOfTranscriptions = 1;
  pBlock = (LCHAR *)CALLOC(sizeof(int) + nNbrOfTranscriptions * sizeof(SWIsltsTranscription), sizeof(LCHAR), MTAG);
  if (pBlock == NULL) {
    PLogError(L("SWISLTS_OUT_OF_MEMORY"));
    nRes = SWIsltsErrAllocResource;
    goto CLEAN_UP;
  }
  pnNbrOfTranscriptionsToSave = (int *)pBlock;
  pTranscription = (SWIsltsTranscription *)(pBlock + sizeof(int));

  *ppTranscriptions = pTranscription;
  *pnNbrOfTranscriptions = *pnNbrOfTranscriptionsToSave = nNbrOfTranscriptions;

  /* extra +1 for double-null at the end */
  pTranscription->pBuffer = MALLOC(MAX_PHONE_LEN * (num_phones + 1+1), MTAG);
  if( pTranscription->pBuffer == NULL ) {
    PLogError(L("SWISLTS_OUT_OF_MEMORY"));
    nRes = SWIsltsErrAllocResource;
    goto CLEAN_UP;
  }

  nRes = GetPhoneStr(pEng, phone_string, num_phones, (char *)pTranscription->pBuffer, &(pTranscription->nSizeOfBuffer));
  if( nRes != SWIsltsSuccess ) {
    PLogError(L("SWIsltsInternalErr: GetPhoneStr( ) fails with return code %d\n"), nRes);
    goto CLEAN_UP;
  }

  return SWIsltsSuccess;

 CLEAN_UP:

  *ppTranscriptions = NULL;
  *pnNbrOfTranscriptions = 0;  

  for( i = 0; i < nNbrOfTranscriptions; i++ ) {
    if(pTranscription[i].pBuffer) {
      FREE(pTranscription[i].pBuffer);
    }
  }
  FREE(pTranscription);

  return nRes;
}


SWISLTS_FNEXPORT SWIsltsResult SWIsltsG2PFreeWordTranscriptions(SWIsltsHand hLts, 
                                                                SWIsltsTranscription *pTranscriptions)
{
  SWIsltsResult          nRes = SWIsltsSuccess;
  int                    nNbrOfTranscriptions;
  int                    i;
  LCHAR                * pBuffer = NULL;

  if( pTranscriptions == NULL ) {
    return SWIsltsInvalidParam;
  }

  pBuffer = ((LCHAR *)pTranscriptions - sizeof(int));
  nNbrOfTranscriptions = (int)*pBuffer;

  for( i = 0; i < nNbrOfTranscriptions; i++ ) {
    if( pTranscriptions[i].pBuffer ) {
      FREE(pTranscriptions[i].pBuffer);
    }
  }
  FREE(pBuffer);

  return nRes;
}

static SWIsltsResult GetPhoneStr(SLTS_Engine *pEng, char *apszPhones[], int num_phones, char *pszPhoneStr, size_t *len)
{
  int                    i, j;
  int                    nFound;
  SWIsltsResult          nRes = SWIsltsSuccess;
  const char           * pszLastPhone = NULL;
  
  *pszPhoneStr = '\0';

  for( i = 0; i < num_phones; i++ ) {
    nFound = 0;
    for ( j = 0; j <  g_numPhones && nFound == 0; j++ ) {
      if( strcmp(apszPhones[i], g_aPhoneMap[j].src) == 0 ) {
        nFound = 1;
        if( strcmp(g_aPhoneMap[j].des, INF_SILENCE_OPTIONAL) == 0 ) {
          if( *pszPhoneStr != '\0' && strcmp(pszLastPhone, INF_SILENCE_OPTIONAL) != 0 ) {
            strcat(pszPhoneStr, g_aPhoneMap[j].des);
          }
        }
        else if( g_aPhoneMap[j].des != '\0' ) {
          strcat(pszPhoneStr, g_aPhoneMap[j].des);
        }
        pszLastPhone = g_aPhoneMap[j].des;
      }
    }
    if( nFound == 0 ) {
      strcat(pszPhoneStr, apszPhones[i]);
      pszLastPhone = apszPhones[i];
    }
  }

  *len = strlen(pszPhoneStr) + 1;
  // add the double-null per SREC/Vocon convention
  pszPhoneStr[ *len] = 0;

  return nRes;
}
