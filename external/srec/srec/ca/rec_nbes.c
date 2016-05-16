/*---------------------------------------------------------------------------*
 *  rec_nbes.c  *
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
#include "srec.h"
#include "portable.h"

#ifdef SET_RCSID
static const char *rcsid = 0 ? (const char *) &rcsid :
                           "$Id: rec_nbes.c,v 1.6.6.7 2007/11/13 22:18:02 rabih_majzoub Exp $";
#endif
                           
                           
CA_NBestList *CA_PrepareNBestList(CA_Recog *hRecog, int num, asr_int32_t *bestScore)
{
  CA_NBestList    *newList;
  
  TRY_CA_EXCEPT
  ASSERT(hRecog);
  
  newList = (CA_NBestList*)srec_nbest_prepare_list(hRecog->recm, num, bestScore);
  return newList;
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hRecog)
}


void CA_DeleteNBestList(CA_NBestList *nbest)
{
  if (nbest)
    srec_nbest_destroy_list(nbest);
  return;

	BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(nbest)
}


int CA_NBestListCount(CA_NBestList *nbest)
{
  TRY_CA_EXCEPT
  if (nbest)
    return srec_nbest_get_num_choices(nbest);
  else
    return 0;
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(nbest)
}

int CA_NBestListGetResultConfidenceValue(CA_NBestList *nbest, size_t choice, int *value)
{
  if (nbest)
  {
	  *value =srec_nbest_get_confidence_value(nbest, choice);
      return 1; 
  }
  else
      return 0;
}

int CA_NBestListRemoveResult(CA_NBestList *nbest, int index)
{
  return srec_nbest_remove_result(nbest,index);	
}

LCHAR* CA_NBestListGetResultWord(CA_NBestList* nbest, size_t iChoice)
{
  return srec_nbest_get_word(nbest,iChoice);
}

ESR_ReturnCode CA_NBestListGetResultWordIDs(CA_NBestList* nbest, size_t index, wordID* wordIDs, size_t* len, asr_int32_t* cost)
{
  if (!nbest)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return srec_nbest_get_resultWordIDs(nbest, index, wordIDs, len, cost);
}
