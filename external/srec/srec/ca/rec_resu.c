/*---------------------------------------------------------------------------*
 *  rec_resu.c  *
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
#include "word_lattice.h"

#define USE_PREDICT   0
#define FRAME_KNEE    100
#define FRAME_SCALE   13  /* 23 for c32 */
#define JUNK_SCALE    0.87  /* 0.8 for c32 */

static const char rec_resu[] = "$Id: rec_resu.c,v 1.7.6.7 2007/10/15 18:06:24 dahan Exp $";

int CA_FullResultScore(CA_Recog *hRecog, int *score, int do_incsil)
{
  bigcostdata cost;
  
  srec_get_top_choice_score(hRecog->recm, &cost, do_incsil);
  
  *score = cost;
  return 0;
}


int CA_FullResultLabel(CA_Recog *hRecog, char *label, int len)
{
  int rc;
  TRY_CA_EXCEPT
  
  rc = srec_get_top_choice_transcription(hRecog->recm, label, len, 1);
  if (rc != 0)
    return REJECT_RESULT;
    
  return FULL_RESULT;
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hRecog)
}

ESR_ReturnCode CA_ResultStripSlotMarkers(char *text)
{
  srec_result_strip_slot_markers(text);
  return ESR_SUCCESS;
}

ESR_ReturnCode CA_GetRecogID(CA_Recog *hRecog, int *id)
{
  srec_get_bestcost_recog_id(hRecog->recm, id);
  return ESR_SUCCESS;
}

ESR_ReturnCode CA_FullResultWordIDs(CA_Recog *hRecog, wordID *wordIDs, size_t* len)
{
  return srec_get_top_choice_wordIDs(hRecog->recm, wordIDs, len);
}

void CA_ClearResults(CA_Recog *hRecog)
{
  TRY_CA_EXCEPT
  ASSERT(hRecog);
  
  srec_clear_results(hRecog->recm);
  return;
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hRecog)
}


int CA_RecognitionHasResults(CA_Recog *hRecog)
{
  TRY_CA_EXCEPT
  ASSERT(hRecog);
  
  if (!srec_has_results(hRecog->recm))
    return False;
  else
    return True;
}

int CA_IsEndOfUtteranceByResults(CA_Recog *hRecog)
{
  TRY_CA_EXCEPT
  ASSERT(hRecog);
  return multi_srec_get_eos_status(hRecog->recm);
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hRecog)
}
