/*---------------------------------------------------------------------------*
 *  utt_proc.c  *
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

static const char utt_proc[] = "$Id: utt_proc.c,v 1.4.6.6 2007/10/15 18:06:24 dahan Exp $";

int CA_CalculateUtteranceStatistics(CA_Utterance *hUtt, int start, int end)
{
  TRY_CA_EXCEPT
  int ii, frames = 0;
  
  frames = get_background_statistics(hUtt->data.gen_utt.frame,
                                      -start, -end,
                                      hUtt->data.gen_utt.backchan,
                                      hUtt->data.gen_utt.num_chan, 1);
  
  /* log_report ("UTT (%d): ", frames); */
  for (ii = 0; ii < hUtt->data.gen_utt.num_chan; ii++)
  {
    evaluate_parameters(hUtt->data.gen_utt.backchan[ii]);
    /* log_report ("%d ", hUtt->data.gen_utt.backchan[ii]->mean); */
  }
  /* log_report ("\n");*/
  return (frames);
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hUtt)
}
