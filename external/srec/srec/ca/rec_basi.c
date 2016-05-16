/*---------------------------------------------------------------------------*
 *  rec_basi.c  *
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

static const char rec_basi[] = "$Id: rec_basi.c,v 1.13.6.7 2007/10/15 18:06:24 dahan Exp $";

/*chopped - chopped a lot of stuff out of this file - all the references to the
stuff within the rec structure.  It seems like this should not be at this level.  If
we leave it below here, we can change the search (with a new rec type) without
having to change this level*/

CA_Recog *CA_AllocateRecognition()
{
  CA_Recog *hRecog = NULL;

#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("in CA_AllocateRecognition\n");
#endif

  TRY_CA_EXCEPT


  /* CREC_SET_SIGNATURE must be 'tied' to the
   * actual creation of the recog_info structure.
   * Any methods which take 'recog_info' as an argument,
   * even 'destroy_recognition()' will test the signature!
   */
  hRecog = (CA_Recog *) CALLOC_CLR(1, sizeof(CA_Recog), "ca.hRecog");

  hRecog->setup_count = 0;
  hRecog->is_running = False;
  hRecog->is_configured = False;
  hRecog->is_resultBlocked = False;
  hRecog->ca_rtti = CA_RECOGNIZER_SIGNATURE;

  hRecog->recm = (multi_srec*)CALLOC_CLR(1, sizeof(multi_srec), "ca.hRecog.srec");
  return (hRecog);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hRecog)
}

int CA_ConfigureRecognition(CA_Recog *hRecog, CA_RecInputParams *hRecInput)
{
  int rc = 0;
  TRY_CA_EXCEPT

  if (hRecog->is_configured == True)
    SERVICE_ERROR(RECOGNIZER_ALREADY_CONFIGURED);

  rc = allocate_recognition(hRecog->recm,
                            hRecInput->viterbi_prune_thresh,
                            hRecInput->max_hmm_tokens,
                            hRecInput->max_fsmnode_tokens,
                            hRecInput->max_word_tokens,
                            hRecInput->max_altword_tokens,
                            hRecInput->num_wordends_per_frame,
                            hRecInput->max_fsm_nodes,
                            hRecInput->max_fsm_arcs,
                            hRecInput->max_frames,
                            hRecInput->max_model_states,
                            hRecInput->max_searches);
  if (rc) return rc;

  /*rc =*/
  srec_eosd_allocate(&hRecog->eosd_parms,
                     hRecInput->eou_threshold,
                     hRecInput->eou_threshold,
                     hRecInput->terminal_timeout,
                     hRecInput->optional_terminal_timeout,
                     hRecInput->non_terminal_timeout,
                     hRecInput->max_frames);
  if (rc) return rc;

  hRecog->is_configured = True;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hRecog)
  return 0;
}

void CA_UnconfigureRecognition(CA_Recog *hRecog)
{
  TRY_CA_EXCEPT

  if (hRecog->is_configured == False)
    SERVICE_ERROR(RECOGNIZER_NOT_CONFIGURED);

  if (hRecog->is_running == True)
    SERVICE_ERROR(RECOGNIZER_ALREADY_STARTED);

  srec_eosd_destroy(hRecog->eosd_parms);
  free_recognition(hRecog->recm);
  hRecog->is_configured = False;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hRecog)
}



void CA_FreeRecognition(CA_Recog *hRecog)
{
  TRY_CA_EXCEPT
  ASSERT(hRecog);

  if (hRecog->is_configured == True)
    SERVICE_ERROR(RECOGNIZER_ALREADY_CONFIGURED);

  /* CREC_CLEAR_SIGNATURE must be 'tied' to the
   * actual destruction of the recog_info structure.
   * Any methods which take 'recog_info' as an argument,
   * even 'destroy_recognition()' will test the signature!
   */
  FREE(hRecog->recm);
  FREE(hRecog);

  return;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hRecog)
}


void CA_BeginRecognition(CA_Recog *hRecog, CA_Syntax *hSyntax,
                         int first_syntax_node, CA_RecInputParams *hRecInput)
{
  TRY_CA_EXCEPT
  /*ASSERT (hSyntax);*/
  ASSERT(hRecog);
  ASSERT(hRecInput);
  if (hRecog->is_running == True)
    SERVICE_ERROR(RECOGNIZER_ALREADY_STARTED);

  if (hRecog->is_configured == False)
    SERVICE_ERROR(RECOGNIZER_NOT_CONFIGURED);

  begin_recognition(hRecog->recm, first_syntax_node);
  hRecog->is_running = True;

  return;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hRecog)
}


void CA_AdvanceRecognitionByFrame(CA_Recog *hRecog, CA_Pattern *hPattern,
                                  CA_Utterance *hUtterance)
{
  int rc;
  TRY_CA_EXCEPT
  ASSERT(hRecog);
  ASSERT(hPattern);
  ASSERT(hUtterance);
  if (hRecog->is_running == False)
    SERVICE_ERROR(RECOGNIZER_NOT_STARTED);
  if (hPattern->is_loaded == False)
    SERVICE_ERROR(PATTERN_NOT_LOADED);
  //if (0 && hPattern->setup_sub == NULL && hPattern->setup_whole == NULL)
    //SERVICE_ERROR(PATTERN_NOT_SETUP);

  rc = multi_srec_viterbi(hRecog->recm,
                          hRecog->eosd_parms,
                          &hPattern->data,
                          &hUtterance->data);
  return;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hRecog)
}


int CA_EndRecognition(CA_Recog *hRecog, CA_Pattern *hPattern,
                      CA_Utterance *hUtterance)
{
  TRY_CA_EXCEPT
  int terminated;

  ASSERT(hRecog);
  ASSERT(hPattern);
  ASSERT(hUtterance);
  if (hRecog->is_running == False)
    SERVICE_ERROR(RECOGNIZER_NOT_STARTED);
  if (hPattern->is_loaded == False)
    SERVICE_ERROR(PATTERN_NOT_LOADED);
  //if (0 && hPattern->setup_sub == NULL && hPattern->setup_whole == NULL)
    //SERVICE_ERROR(PATTERN_NOT_SETUP);

  terminated = 1;
  end_recognition(hRecog->recm);

  if (terminated && hUtterance->data.gen_utt.do_channorm)
  {
    if (!inherit_recognition_statistics(&hUtterance->data, hRecog->recm,
                                        hUtterance->data.gen_utt.channorm->dim))
      SERVICE_ERROR(UNEXPECTED_DATA_ERROR); /* TODO: find a suitable error code */
  }

  if (terminated) hPattern->recog_terminated = True;
  hRecog->is_running = False;
  return (terminated);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hRecog)
}
