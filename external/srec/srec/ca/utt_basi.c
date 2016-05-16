/*---------------------------------------------------------------------------*
 *  utt_basi.c  *
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

static const char utt_basi[] = "$Id: utt_basi.c,v 1.6.6.8 2007/10/15 18:06:24 dahan Exp $";


CA_Utterance *CA_AllocateUtterance(void)
{
  CA_Utterance *hUtt = NULL;
  TRY_CA_EXCEPT

  hUtt = (CA_Utterance *) CALLOC_CLR(1, sizeof(CA_Utterance), "ca.hUtt");
  hUtt->ca_rtti = CA_UTTERANCE_SIGNATURE;
  return (hUtt);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hUtt)
}


void CA_FreeUtterance(CA_Utterance *hUtt)
{
  TRY_CA_EXCEPT
  ASSERT(hUtt);

  FREE((char *) hUtt);
  return;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hUtt)
}


int CA_InitUtteranceForFrontend(CA_Utterance *hUtt,
                                CA_FrontendInputParams *hFrontPar)
{
  TRY_CA_EXCEPT
  ASSERT(hUtt);
  ASSERT(hFrontPar > 0);
  if (hUtt->data.utt_type != 0)
    SERVICE_ERROR(UTTERANCE_ALREADY_INITIALISED);

  init_utterance(&hUtt->data, LIVE_INPUT, 3*hFrontPar->mel_dim,  /* TODO: change to proper function */
                 DEFAULT_BUFFER_SIZE, KEEP_FRAMES,
                 hFrontPar->mel_dim, 1);
  if (hFrontPar->do_skip_even_frames)
    set_voicing_durations(&hUtt->data, hFrontPar->voice_duration / 2,
                          hFrontPar->quiet_duration / 2,
                          hFrontPar->unsure_duration / 2,
                          hFrontPar->start_windback / 2);
  else
    set_voicing_durations(&hUtt->data, hFrontPar->voice_duration,
                          hFrontPar->quiet_duration,
                          hFrontPar->unsure_duration,
                          hFrontPar->start_windback);
  return (0);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hUtt)

}


void CA_ClearUtterance(CA_Utterance *hUtt)
{

  TRY_CA_EXCEPT
  ASSERT(hUtt);
  if (hUtt->data.utt_type == 0)
  {
    SERVICE_ERROR(UTTERANCE_NOT_INITIALISED);
  }

  if (hUtt->data.utt_type == LIVE_INPUT)
  {
    free_utterance(&hUtt->data);
    hUtt->data.utt_type = 0;
  }
  else
  {
    SERVICE_ERROR(UTTERANCE_UNKNOWN);
  }
  return;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hUtt)
}


int CA_AdvanceUtteranceFrame(CA_Utterance *hUtt)
{
  TRY_CA_EXCEPT
  int status_code;

  ASSERT(hUtt);
  if (hUtt->data.utt_type != LIVE_INPUT)
    SERVICE_ERROR(UTTERANCE_NOT_INITIALISED);

  status_code = advance_utterance_frame(&hUtt->data);
  return (status_code);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hUtt)
}

int CA_UtteranceHasVoicing(CA_Utterance *hUtt)
{
  TRY_CA_EXCEPT
  ASSERT(hUtt);
  if (hUtt->data.utt_type != LIVE_INPUT)
    SERVICE_ERROR(UTTERANCE_NOT_INITIALISED);

  return (hUtt->data.gen_utt.frame->voicingDetected);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hUtt)
}


void CA_UnlockUtteranceForInput(CA_Utterance *hUtt)
{
  TRY_CA_EXCEPT
  ASSERT(hUtt);
  if (hUtt->data.utt_type != LIVE_INPUT)
    SERVICE_ERROR(UTTERANCE_NOT_INITIALISED);
  ASSERT(!isFrameBufferActive(hUtt->data.gen_utt.frame));
  startFrameCollection(hUtt->data.gen_utt.frame);
  return;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hUtt)
}


void CA_LockUtteranceFromInput(CA_Utterance *hUtt)
{
  TRY_CA_EXCEPT
  ASSERT(hUtt);
  if (hUtt->data.utt_type != LIVE_INPUT)
    SERVICE_ERROR(UTTERANCE_NOT_INITIALISED);
  ASSERT(isFrameBufferActive(hUtt->data.gen_utt.frame));


  stopFrameCollection(hUtt->data.gen_utt.frame);
  return;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hUtt)
}


void CA_ResetVoicing(CA_Utterance *hUtt)
{
  TRY_CA_EXCEPT
  ASSERT(hUtt);
  if (hUtt->data.utt_type != LIVE_INPUT)
    SERVICE_ERROR(UTTERANCE_NOT_INITIALISED);

  clearEndOfUtterance(hUtt->data.gen_utt.frame);
  clearC0Entries(hUtt->data.gen_utt.frame);
  startFrameCollection(hUtt->data.gen_utt.frame);
  return;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hUtt)
}


void CA_FlushUtteranceFrames(CA_Utterance *hUtt)
{
  TRY_CA_EXCEPT
  ASSERT(hUtt);
  if (hUtt->data.utt_type != LIVE_INPUT)
    SERVICE_ERROR(UTTERANCE_NOT_INITIALISED);

  setRECframePtr(hUtt->data.gen_utt.frame, 0, 0);
  releaseBlockedFramesInBuffer(hUtt->data.gen_utt.frame);
  hUtt->data.gen_utt.last_push = NULL;
  return;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hUtt)
}


void CA_SetEndOfUtteranceByLevelTimeout(CA_Utterance *hUtt, long timeout, long holdOff)
{
  TRY_CA_EXCEPT
  ASSERT(hUtt);
  if (hUtt->data.utt_type != LIVE_INPUT)
    SERVICE_ERROR(UTTERANCE_NOT_INITIALISED);

  setupEndOfUtterance(hUtt->data.gen_utt.frame, timeout, holdOff);
  return;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hUtt)
}


int CA_UtteranceHasEnded(CA_Utterance *hUtt)
{
  TRY_CA_EXCEPT
  ASSERT(hUtt);
  if (hUtt->data.utt_type != LIVE_INPUT)
    SERVICE_ERROR(UTTERANCE_NOT_INITIALISED);

  return (utterance_ended(&hUtt->data));

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hUtt)
}
