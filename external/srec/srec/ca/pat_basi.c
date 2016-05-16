/*---------------------------------------------------------------------------*
 *  pat_basi.c  *
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
#include "swicms.h"

#ifdef SET_RCSID
static const char *rcsid = 0 ? (const char *) &rcsid :
                           "$Id: pat_basi.c,v 1.9.6.11 2008/03/07 18:49:28 dahan Exp $";
#endif

extern const float   root_pi_over_2;

CA_Pattern *CA_AllocatePattern(void)
{
  TRY_CA_EXCEPT
  CA_Pattern *hPattern = NULL;

  hPattern = (CA_Pattern *) CALLOC_CLR(1,
             sizeof(CA_Pattern), "ca.hPattern");
  hPattern->is_loaded = False;
  //hPattern->setup_whole = NULL;
  //hPattern->setup_sub = NULL;
  hPattern->ca_rtti = CA_PATTERN_SIGNATURE;
  return (hPattern);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hPattern)
}


void CA_FreePattern(CA_Pattern *hPattern)
{
  TRY_CA_EXCEPT

  ASSERT(hPattern);
  FREE((char *) hPattern);
  return;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hPattern)
}


int CA_LoadPattern(CA_Pattern *hPattern, CA_PatInputParams *hPatInput,
                   int dimen , char *multable , char *imelda)
{

  TRY_CA_EXCEPT
#ifndef _RTT
  int ii, ret_code;

  ASSERT(hPattern);
  ASSERT(hPatInput);
  if (hPattern->is_loaded == True)
    SERVICE_ERROR(PATTERN_ALREADY_LOADED);
  if (hPatInput->is_loaded == False)
    SERVICE_ERROR(PATTERN_INPUT_NOT_LOADED);

  hPattern->data.prep = (preprocessed *) CALLOC_CLR(1,
                        sizeof(preprocessed), "ca.hPattern->data.prep");

  /*  Load the Imelda transform if specified */
  if (imelda && strlen(imelda) > 0)
  {
    ret_code = init_newton_transform(hPattern->data.prep, 0, imelda, hPatInput->dimen);
    if (ret_code > 0)
      SERVICE_ERROR(PATTERN_NOT_LOADED);
  }
  else
  {
    hPattern->data.prep->use_dim = hPatInput->dimen;
    hPattern->data.prep->use_from = hPatInput->feat_start;
  }

  if (hPatInput->whole_dimen == 0)
    hPattern->data.prep->whole_dim = hPatInput->dimen;
  else
    hPattern->data.prep->whole_dim = hPatInput->whole_dimen;
  if (hPattern->data.prep->whole_dim > hPattern->data.prep->use_dim)
    SERVICE_ERROR(BAD_PARAMETER);

  hPattern->data.prep->mix_score_scale = (prdata)(128 * hPatInput->mix_score_scale + 0.5)
                                         - (prdata)0.5;
  hPattern->data.prep->uni_score_scale = (prdata)(128 * hPatInput->uni_score_scale + 0.5)
                                         - (prdata)0.5;      
  hPattern->data.prep->uni_score_offset = (prdata) hPatInput->uni_score_offset;
  hPattern->data.prep->imelda_scale = (prdata)hPatInput->imelda_scale;
  init_preprocessed(hPattern->data.prep, dimen, hPatInput->imelda_scale); /* TODO: move this to Setup */


  /* Annotation parameters */
  hPattern->data.prep->end.rel_low  = hPatInput->rel_low;
  hPattern->data.prep->end.rel_high  = hPatInput->rel_high;
  hPattern->data.prep->end.gap_period  = hPatInput->gap_period;
  hPattern->data.prep->end.click_period = hPatInput->click_period;
  hPattern->data.prep->end.breath_period = hPatInput->breath_period;
  hPattern->data.prep->end.extend_annotation = hPatInput->extend_annotation;
  hPattern->data.prep->end.min_annotation_frames = hPatInput->min_annotation_frames;
  hPattern->data.prep->end.max_annotation_frames = hPatInput->max_annotation_frames;
  hPattern->data.prep->end.min_segment_rel_c0 = hPatInput->min_segment_rel_c0;
  hPattern->data.prep->end.min_initial_quiet_frames = hPatInput->min_initial_quiet_frames;
  hPattern->data.prep->end.delete_leading_segments = hPatInput->delete_leading_segments;
  hPattern->data.prep->end.leading_segment_min_frames = hPatInput->leading_segment_min_frames;
  hPattern->data.prep->end.leading_segment_max_frames = hPatInput->leading_segment_max_frames;
  hPattern->data.prep->end.leading_segment_min_silence_gap_frames
  = hPatInput->leading_segment_min_silence_gap_frames;
  hPattern->data.prep->end.leading_segment_accept_if_not_found
  = hPatInput->leading_segment_accept_if_not_found;
#if DO_SUBTRACTED_SEGMENTATION
  hPattern->data.prep->end.snr_holdoff = hPatInput->snr_holdoff;
  hPattern->data.prep->end.min_acceptable_snr = hPatInput->min_acceptable_snr;
#endif
  hPattern->data.prep->end.param  = hPatInput->param;
  hPattern->data.prep->end.beep_size = hPatInput->beep_size;
  hPattern->data.prep->end.beep_threshold = hPatInput->beep_threshold;


  /* log-lookup table */
  create_lookup_logadd(&hPattern->data.prep->add, (float)MUL_SCALE);

  /* Build the weights conversion table */
  for (ii = 0; ii < MAX_WTS; ii++)
    hPattern->data.prep->exp_wt[ii] =
      (prdata)(WEIGHT_SCALE * exp((double)(0x01 << WT_ADJUST) *
                                  (double) - ii / (MUL_SCALE * hPattern->data.prep->add.scale)) +
               0.5) - (prdata)0.5;

  hPattern->data.prep->ref_count = 1;
  hPattern->is_loaded = True;

  return (True);
#else
  log_report("RTT not in module\n");
  SERVICE_ERROR(FEATURE_NOT_SUPPORTED);
  return (False);
#endif


  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hPattern)

}

void CA_UnloadPattern(CA_Pattern *hPattern)
{
  TRY_CA_EXCEPT
  ASSERT(hPattern);
  if (hPattern->is_loaded == False)
    SERVICE_ERROR(PATTERN_NOT_LOADED);

  if (--hPattern->data.prep->ref_count == 0)
  {
    if (hPattern->data.prep->matrix)
      free_linear_transform(hPattern->data.prep);

    if (hPattern->data.prep->add.table)
      destroy_lookup_logadd(&hPattern->data.prep->add);

    clear_preprocessed(hPattern->data.prep);

    FREE((char *) hPattern->data.prep);
    hPattern->data.prep = NULL;
  }

  hPattern->is_loaded = False;
  return;
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hPattern)
}


void CA_SetupPatternForAcoustic(CA_Pattern *hPattern, CA_Acoustic *hAcoust)
{
  TRY_CA_EXCEPT
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("in SetupPatternForAcoustic\n");
#endif
  /*  Setup for mul-table if necessary
  */
  ASSERT(hPattern);
  ASSERT(hAcoust);
  if (hPattern->is_loaded == False)
    SERVICE_ERROR(PATTERN_NOT_LOADED);
  if (hAcoust->is_loaded == False)
    SERVICE_ERROR(ACOUSTIC_NOT_LOADED);
  /* Check that if the Acoustic object is already set up then
  the pattern objects must have certain similarities. */
  if (hAcoust->pattern_setup_count > 0)
  {
    if (hPattern->data.prep->imelda_scale != hAcoust->imelda_scale)
      SERVICE_ERROR(ACOUSTIC_PATTERN_MISMATCH);
    if (hPattern->data.prep->use_dim != hAcoust->use_dim)
      SERVICE_ERROR(ACOUSTIC_PATTERN_MISMATCH);
  }
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  //PLogMessage("mod_style %d\n", hAcoust->acc.mod_style);
#endif

  hAcoust->pattern_setup_count++;
  return;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hPattern)
}

void CA_ClearPatternForAcoustic(CA_Pattern *hPattern, CA_Acoustic *hAcoust)
{
  TRY_CA_EXCEPT
  ASSERT(hPattern);
  ASSERT(hAcoust);
  if (hPattern->is_loaded == False)
    SERVICE_ERROR(PATTERN_NOT_LOADED);
  if (hAcoust->pattern_setup_count == 0)
    SERVICE_ERROR(ACOUSTIC_HAS_NO_PATTERN);
  hAcoust->pattern_setup_count--;

  return;
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hPattern)
}


int CA_MakePatternFrame(CA_Pattern *hPattern, CA_Utterance *hUtt)
{

  TRY_CA_EXCEPT
  int status_code;
  swicms_norm_info* swicms;

  ASSERT(hPattern);
  ASSERT(hUtt);

  if (hPattern->is_loaded == False)
    SERVICE_ERROR(PATTERN_NOT_LOADED);

  status_code = get_data_frame(hPattern->data.prep, &hUtt->data);

  /* swicms_cache_frame() must be here because get_data_frame() is called from
     backtracing.  Is the caching at the front-end even necessary any more?
  */
  swicms = hUtt->data.gen_utt.swicms;
  if (!swicms->is_valid)
    swicms_lda_process(swicms, hPattern->data.prep);

  swicms_cache_frame(swicms, hPattern->data.prep->seq_unnorm,
                     hUtt->data.gen_utt.channorm->dim);
  apply_channel_normalization_in_swicms(swicms, hPattern->data.prep->seq,
                                        hPattern->data.prep->seq_unnorm,
                                        hUtt->data.gen_utt.channorm->dim);

  /* prepare is fairly useless and should be removed */
  prepare_data_frame(hPattern->data.prep);
  return (status_code);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hPattern)
}

