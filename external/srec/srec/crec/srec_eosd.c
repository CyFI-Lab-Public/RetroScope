/*---------------------------------------------------------------------------*
 *  srec_eosd.c  *
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

#include"portable.h"
#include"passert.h"
#include"srec.h"
#include"srec_eosd.h"
#include"srec_context.h"
#include"word_lattice.h"

void srec_eosd_allocate(srec_eos_detector_parms** peosd,
                        int eos_costdelta,
                        int opt_eos_costdelta,
                        int terminal_timeout,
                        int optional_terminal_timeout,
                        int non_terminal_timeout,
                        int max_speech_duration)
{
  srec_eos_detector_parms* eosd;
  eosd = (srec_eos_detector_parms*)CALLOC(1, sizeof(srec_eos_detector_parms), "search.endpointer");
  eosd->eos_costdelta        = (frameID)eos_costdelta;
  eosd->opt_eos_costdelta    = (frameID)opt_eos_costdelta;
  eosd->endnode_timeout      = (frameID)terminal_timeout;
  eosd->optendnode_timeout   = (frameID)optional_terminal_timeout;
  eosd->internalnode_timeout = (frameID)non_terminal_timeout;
  eosd->inspeech_timeout     = (frameID)max_speech_duration;
  *peosd = eosd;
}

void srec_eosd_destroy(srec_eos_detector_parms* eosd)
{
  FREE(eosd);
}

/* The current algorithm does not make use of most of the frmcnt counters,
   rather we look at the eos frame from the final end node search state
   and comparrer with the current frame.  The new method is less sensitive
   to background noise.

   The 1.9 method had a blatant bug in that we were reseting the optend_frmnt
   when there were no live alternative tokens, ie xftoken == NUL was causing
   reset!
*/

void srec_eosd_state_reset(srec_eos_detector_state* eosd_state)
{
  eosd_state->endnode_frmcnt = 0;
  eosd_state->optendnode_frmcnt = 0;
  eosd_state->internalnode_frmcnt = 0;
  eosd_state->inspeech_frmcnt = 0;
  eosd_state->internalnode_node_index = MAXnodeID;
}

EOSrc srec_check_end_of_speech_end(srec* rec)
{
  EOSrc rc = SPEECH_MAYBE_ENDED;
  return rc;
}

EOSrc srec_check_end_of_speech(srec_eos_detector_parms* eosd_parms, srec* rec)
{
  nodeID end_node;
  EOSrc rc = VALID_SPEECH_CONTINUING;
  bigcostdata eos_cost_margin;
  bigcostdata opteos_cost_margin;
  word_token* last_wtoken;
  int nframes_since_eos;
  
  fsmnode_token *ftoken, *eftoken, *oeftoken, *xftoken;
  ftokenID ftoken_index, eftoken_index, oeftoken_index, xftoken_index;
  costdata wrapup_cost = rec->context->wrapup_cost;
  srec_eos_detector_state* eosd_state = &rec->eosd_state;
  
  if (rec->current_search_frame == 1)
    srec_eosd_state_reset(eosd_state);
    
  end_node = rec->context->end_node;
  eftoken_index = rec->best_token_for_node[ end_node];
  if (eftoken_index != MAXftokenID)
    eftoken = &rec->fsmnode_token_array[ eftoken_index];
  else
    eftoken = NULL;
    
  xftoken_index  = rec->current_best_ftoken_index[NODE_INFO_REGULAR];
  if (xftoken_index != MAXftokenID)
    xftoken = &rec->fsmnode_token_array[ xftoken_index];
  else
    xftoken = NULL;
    
  oeftoken_index = rec->current_best_ftoken_index[NODE_INFO_OPTENDN];
  if (oeftoken_index != MAXftokenID)
    oeftoken = &rec->fsmnode_token_array[ oeftoken_index];
  else
    oeftoken = NULL;
    
    
  if (rec->srec_ended)
    rc = SPEECH_MAYBE_ENDED;
  else if (rec->current_search_frame >= rec->word_lattice->max_frames - 1
           || rec->current_search_frame >= eosd_parms->inspeech_timeout)
  {
    /* here we will need to differentiate max_frames from
       num_frames_allocated */
    if (eftoken_index != MAXftokenID)
      rc = SPEECH_ENDED;
    else
      rc = SPEECH_TOO_LONG;
  }
  else
  {
  
    /* reset the internal counter? */
    ftoken_index = rec->current_best_ftoken_index[NODE_INFO_REGULAR];
    if (ftoken_index != MAXftokenID)
    {
      ftoken = &rec->fsmnode_token_array[ ftoken_index];
      if (eosd_state->internalnode_node_index != ftoken->FSMnode_index)
      {
        eosd_state->internalnode_node_index = ftoken->FSMnode_index;
        eosd_state->internalnode_frmcnt = 1;
      }
      else
      {
        if (ftoken->word != rec->context->beg_silence_word)
          eosd_state->internalnode_frmcnt++;
      }
    }
    else
    {
      eosd_state->internalnode_frmcnt = 1;
      eosd_state->internalnode_node_index = MAXnodeID;
    }
    
    /* nframes since eos */
    if (eftoken)
    {
      last_wtoken = NULL;
      if (eftoken->word_backtrace != MAXwtokenID)
      {
        last_wtoken = &rec->word_token_array[eftoken->word_backtrace];
        nframes_since_eos = rec->current_search_frame - last_wtoken->end_time;
      }
      else
        nframes_since_eos = 0;
    }
    else
      nframes_since_eos = 0;
      
    /* eos cost margin */
    if (!eftoken)
    {
      eos_cost_margin = 0;
    }
    else if (!oeftoken && !xftoken)
    {
      eos_cost_margin = MAXcostdata;
    }
    else if (!oeftoken)
    {
      eos_cost_margin = xftoken->cost + wrapup_cost - eftoken->cost;
    }
    else if (!xftoken)
    {
      eos_cost_margin = oeftoken->cost + wrapup_cost - eftoken->cost;
    }
    else if (oeftoken->cost > eftoken->cost)
    {
      eos_cost_margin = xftoken->cost + wrapup_cost - eftoken->cost;
    }
    else
    { /* if(oeftoken->cost < eftoken->cost) */
      eos_cost_margin = oeftoken->cost + wrapup_cost - eftoken->cost;
    }
    
    /* opteos cost margin */
    if (!eftoken)
    {
      opteos_cost_margin = 0;
    }
    else if (!oeftoken)
    {
      opteos_cost_margin = 0;
    }
    else if (!xftoken)
    {
      opteos_cost_margin = MAXcostdata;
    }
    else
    {
      opteos_cost_margin = xftoken->cost + wrapup_cost - eftoken->cost;
    }
    
    if (eftoken)
    {
      if (oeftoken && nframes_since_eos > eosd_parms->optendnode_timeout
          && opteos_cost_margin > eosd_parms->eos_costdelta)
      {
        rc = SPEECH_ENDED;
        
      }
      else if (!oeftoken && nframes_since_eos > eosd_parms->endnode_timeout
               && eos_cost_margin > eosd_parms->eos_costdelta)
      {
        rc = SPEECH_ENDED;
        
      }
      else if (nframes_since_eos > eosd_parms->optendnode_timeout
               && eos_cost_margin > eosd_parms->eos_costdelta)
      {
        rc = SPEECH_ENDED;
        
      }
      else
      {
        rc = VALID_SPEECH_CONTINUING;
      }
    }
    
    /* reached internal timeout, ie at same node for so long? */
    if (eosd_state->internalnode_frmcnt >= eosd_parms->internalnode_timeout)
    {
      /* PLogMessage("eosd_state->internalnode_frmcnt %d eosd_parms->internalnode_timeout %d\n", eosd_state->internalnode_frmcnt, eosd_parms->internalnode_timeout); */
      ftoken_index = rec->current_best_ftoken_index[NODE_INFO_REGULAR];
      ftoken = &rec->fsmnode_token_array [ ftoken_index];
      /* sprintf(buf, "eos rec%d@%d,%d i%d> ", rec->id,
      rec->current_search_frame, ftoken->FSMnode_index,
      eosd_state->internalnode_frmcnt);
      PLogMessage(buf); 
      sprint_word_token_backtrace(buf,sizeof(buf),rec,ftoken->word_backtrace);
      PLogMessage(" %s\n", buf); */
      rc = SPEECH_ENDED;
    }
  }
  
  /* the endnode will never win against an optend node because
     the cost at endnode is the same or worse (even wrapup_cost adjustment) */
  
  
  
  /* so we need to check for optend nodes separately here
     but we really need to remember best_optendnode_index, best_endnode_index
     best_nonendnode_index */
  return rc;
}
