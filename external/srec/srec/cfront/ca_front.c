/*---------------------------------------------------------------------------*
 *  ca_front.c  *
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

/************************************************************************
 * CA_Frontend Methods
 ************************************************************************/


#ifndef _RTT
#include <stdio.h>
#endif
#include <stdlib.h>
#include <string.h>
#include <limits.h>

#ifdef unix
#include <unistd.h>
#endif
#include <assert.h>

#include "frontapi.h"

#include "portable.h"

#ifdef USE_COMP_STATS
void stop_front_end_clock(void);
void start_front_end_clock(void);
#endif

/*  These are front-end functions
*/

CA_Frontend *CA_AllocateFrontend(float srcscale, int offset,
                                 float sinkscale)
{
  CA_Frontend *hFrontend = NULL;
  TRY_CA_EXCEPT
  hFrontend = (CA_Frontend *) CALLOC_CLR(1, sizeof(CA_Frontend), "ca.hFrontend");
  hFrontend->src_scale = srcscale;
  hFrontend->sink_scale = sinkscale;
  hFrontend->offset = offset;
  
  hFrontend->is_configured = False;
  hFrontend->is_filter_loaded = False;
  
  hFrontend->ca_rtti = CA_FRONTEND_SIGNATURE;
  return (hFrontend);
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hFrontend)
}

void CA_FreeFrontend(CA_Frontend *hFrontend)
{
  TRY_CA_EXCEPT
  ASSERT(hFrontend);
  if (hFrontend->is_configured == True)
    SERVICE_ERROR(CONFIGURED_FRONTEND);
  if (hFrontend->is_filter_loaded == True)
    SERVICE_ERROR(SPEC_FILTER_CONFIGURED);
    
  FREE((char *) hFrontend);
  return;
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hFrontend);
}


void CA_ConfigureFrontend(CA_Frontend *hFrontend,
                          CA_FrontendInputParams *hFrontArgs)
{
  TRY_CA_EXCEPT
  ASSERT(hFrontend);
  ASSERT(hFrontArgs);
  
  if (hFrontArgs->is_loaded == False)
    SERVICE_ERROR(FRONTEND_INPUT_NOT_LOADED);
  if (hFrontend->is_configured == True)
    SERVICE_ERROR(CONFIGURED_FRONTEND);
    
  hFrontend->config = create_config_object() ;
  setup_config_object(hFrontend->config, hFrontArgs);
  hFrontend->is_configured = True;
  
  return;
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hFrontend);
}

void CA_UnconfigureFrontend(CA_Frontend *hFrontend)
{
  TRY_CA_EXCEPT
  ASSERT(hFrontend);
  if (hFrontend->is_configured == False)
    SERVICE_ERROR(UNCONFIGURED_FRONTEND);
    
  clear_config_object(hFrontend->config);
  delete_config_object(hFrontend->config);
  hFrontend->config = NULL;
  
  hFrontend->is_configured = False;
  
  return;
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hFrontend);
}


int CA_MakeFrame(CA_Frontend *hFrontend, CA_Utterance *hUtt, CA_Wave *hWave)
{
  /* Note: samdata has already been loaded
  */
  int valid;
  TRY_CA_EXCEPT
  featdata framdata[MAX_CHAN_DIM], voicedata = 0;
  
  ASSERT(hFrontend);
  ASSERT(hUtt);
  ASSERT(hWave);
  
  ASSERT(hUtt->data.gen_utt.frame->uttDim <= MAX_CHAN_DIM);
  
  if (hFrontend->is_configured == False)
    SERVICE_ERROR(UNCONFIGURED_FRONTEND);
  if (hUtt->data.utt_type != LIVE_INPUT)
    SERVICE_ERROR(UTTERANCE_INVALID);
    
#ifdef USE_COMP_STATS
  start_front_end_clock();
#endif
  
  
  if (hUtt->data.gen_utt.frame->haveVoiced)
    valid = make_frame(hWave->data.channel, hFrontend->config->waveobj,
                       hFrontend->config->freqobj, hFrontend->config->cepobj,
                       &hWave->voice, hWave->data.income, hWave->data.outgo,
                       hWave->data.num_samples,
                       framdata, &voicedata);
  else
    valid = make_frame(hWave->data.channel, hFrontend->config->waveobj,
                       hFrontend->config->freqobj,
                       hFrontend->config->cepobj,
                       NULL, hWave->data.income, hWave->data.outgo,
                       hWave->data.num_samples,
                       framdata, &voicedata);
                       
  /*  Ignore first 3 frames. This prevents a spike due to dc offset.
  **  This effect would normally be removed by loading the window
  **  overhang but we are not doing that (tricky).
  */
  if (valid > 0 && hWave->data.channel->frame_count > (DELTA + 3))
  {
    if (pushSingleFEPframe(hUtt->data.gen_utt.frame,
                           framdata, voicedata) != False)
      valid = False;
  }
  else valid = False;
  
#ifdef USE_COMP_STATS
  stop_front_end_clock();
#endif
  
  return (valid);
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hFrontend);
}

int CA_GetFrontendUtteranceDimension(CA_Frontend *hFrontend)
{
  int dim;
  TRY_CA_EXCEPT
  ASSERT(hFrontend);
  ASSERT(hFrontend->config);
  
  if (hFrontend->is_configured == False)
    SERVICE_ERROR(UNCONFIGURED_FRONTEND);
    
  /* TODO: Determine the dim exactly */
  dim = 2 * hFrontend->config->cepobj->mel_dim;
  if (hFrontend->config->cepobj->do_dd_mel)
    dim += hFrontend->config->cepobj->mel_dim;
  return (dim);
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hFrontend);
}
