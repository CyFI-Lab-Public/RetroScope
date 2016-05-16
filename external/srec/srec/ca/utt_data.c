/*---------------------------------------------------------------------------*
 *  utt_data.c  *
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
#include "../clib/fpi_tgt.inl"

static const char utt_data[] = "$Id: utt_data.c,v 1.8.6.6 2007/10/15 18:06:24 dahan Exp $";


int CA_SeekStartOfUtterance(CA_Utterance *hUtt)
{
  TRY_CA_EXCEPT
  int gap = 0;

  ASSERT(hUtt);

  if (utterance_started(&hUtt->data))
  {
    if ((gap = getBlockGap(hUtt->data.gen_utt.frame)) > 0)
      (void) setRECframePtr(hUtt->data.gen_utt.frame, gap, 1);
    if (hUtt->data.gen_utt.frame->holdOffPeriod > 0)
      setRECframePtr(hUtt->data.gen_utt.frame, -MIN(hUtt->data.gen_utt.frame->holdOffPeriod, getFrameGap(hUtt->data.gen_utt.frame)), 1);
    while (!(rec_frame_voicing_status(hUtt->data.gen_utt.frame) & VOICE_BIT))
    {
      incRECframePtr(hUtt->data.gen_utt.frame);
      if (getFrameGap(hUtt->data.gen_utt.frame) == 0)
        break;
    }
    setRECframePtr(hUtt->data.gen_utt.frame, MIN(hUtt->data.gen_utt.start_windback, getBlockGap(hUtt->data.gen_utt.frame)), 1);
#ifdef SREC_ENGINE_VERBOSE_LOGGING
    PLogMessage("L:  Frame start rewound to %d (%d, %d)", hUtt->data.gen_utt.frame->pullTime,
                hUtt->data.gen_utt.start_windback,
                getBlockGap(hUtt->data.gen_utt.frame));
#endif
    hUtt->data.gen_utt.last_push = NULL;
    return hUtt->data.gen_utt.start_windback;
  }
  else
    return 0;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hUtt)
}

int CA_GetUnprocessedFramesInUtterance(CA_Utterance *hUtt)
{
  TRY_CA_EXCEPT
  ASSERT(hUtt);
  return (getFrameGap(hUtt->data.gen_utt.frame));

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hUtt)
}


