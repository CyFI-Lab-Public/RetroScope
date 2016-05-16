/*---------------------------------------------------------------------------*
 *  get_fram.c  *
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
#ifndef _RTT
#include "pstdio.h"
#endif
#include <limits.h>
#include <math.h>
#include <string.h>
#include "passert.h"

#include "c42mul.h"
#include "portable.h"

#include "../clib/fpi_tgt.inl"

#define DEBUG   0
#define FUDGE_FACTOR 1.2f

const float root_pi_over_2 = (float) 1.2533141;

static const char get_fram[] = "$Id: get_fram.c,v 1.7.6.13 2007/10/15 18:06:24 dahan Exp $";

static void create_cepstrum_offsets(preprocessed *prep);
static void destroy_cepstrum_offsets(preprocessed *prep);
static void apply_channel_offset(preprocessed *prep);
static int compare_cached_frame(preprocessed *prep, utterance_info *utt);

void init_utterance(utterance_info *utt, int utt_type, int dimen,
                    int buffer_size, int keep_frames, int num_chan, int do_voicing)
/*
**  To setup the utterance structure
*/
{
  /*  Construct frame buffer  and voice buffer here
  */
  ASSERT(utt);
  ASSERT(dimen > 0);
  if (buffer_size < keep_frames)
    SERVICE_ERROR(BAD_ARGUMENT);
  utt->utt_type = utt_type;
  utt->gen_utt.dim = dimen;
  utt->gen_utt.frame = createFrameBuffer(buffer_size,
                                         dimen, keep_frames, do_voicing);
  utt->gen_utt.num_chan = num_chan;

  setup_ambient_estimation(utt->gen_utt.backchan,
                           utt->gen_utt.num_chan, 100);
  return;
}

void set_voicing_durations(utterance_info *utt, int voice_duration,
                           int quiet_duration, int unsure_duration,
                           int start_windback)
{
  utt->gen_utt.voice_duration = voice_duration;
  utt->gen_utt.quiet_duration = quiet_duration;
  utt->gen_utt.unsure_duration = unsure_duration;
  utt->gen_utt.start_windback = start_windback;
  return;
}

void free_utterance(utterance_info *utt)
/*
**  To close data file pointers etc.
*/
{
  /*  Destroy frame buffer
  */
  ASSERT(utt);

  clear_ambient_estimation(utt->gen_utt.backchan, utt->gen_utt.dim);
  if (utt->gen_utt.frame)
  {
    destroyFrameBuffer(utt->gen_utt.frame);
    utt->gen_utt.frame = NULL;
  }
  return;
}

void init_preprocessed(preprocessed *prep, int dimen, float imelda_scale)
/*
**  To setup the preprocessed structure
*/
{

  ASSERT(prep);
  ASSERT(dimen > 0);
  prep->dim = dimen;
  prep->seq = (imeldata *) CALLOC(prep->dim, sizeof(imeldata),
                                        "srec.prep->seq");
  prep->seq_unnorm = (imeldata *) CALLOC(prep->dim, sizeof(imeldata),
                     "srec.prep->seq_unnorm");
  prep->last_frame = (featdata *) CALLOC(prep->dim, sizeof(featdata),
                     "srec.prep->last_frame");

  /*  Setup constants for distance calculation
  */
  /* TODO: check numbers for non-zero */
  prep->add.scale = (prdata)((2 * imelda_scale * imelda_scale) / MUL_SCALE
                             + 0.5) - (prdata)0.5;
  prep->add.inv_scale = (prdata)(((float)(0x01 << 12) * MUL_SCALE) /
                                 (2 * imelda_scale * imelda_scale) + 0.5) -
                        (prdata)0.5;
  prep->mul.multable_factor_gaussian = 1;
  prep->mul.multable_factor = (prdata)(((MUL_SCALE * (0x01 << EUCLID_SHIFT)
                                         * prep->uni_score_scale)
                                        / (2 * (imelda_scale * imelda_scale
                                                * FUDGE_FACTOR * FUDGE_FACTOR))) / 128 + 0.5)
                              - (prdata)0.5;
  prep->mul.grand_mod_cov = (prdata)((MUL_SCALE * prep->uni_score_scale *
                                      prep->whole_dim *
                                      log((imelda_scale * FUDGE_FACTOR) /
                                          (SIGMA_BIAS * root_pi_over_2))) / 128 + 0.5)
                            - (prdata)0.5 - prep->uni_score_offset;
  prep->mul.grand_mod_cov_gaussian = (prdata)(2 * imelda_scale * imelda_scale *
                                     prep->use_dim *
                                     log(imelda_scale /
                                         (SIGMA_BIAS * root_pi_over_2)) + 0.5)
                                     - (prdata)0.5;
#if DEBUG
  log_report("grand_mod_cov %.1f, grand_mod_cov_gaussian %.1f\n",
             (float)prep->mul.grand_mod_cov,
             (float)prep->mul.grand_mod_cov_gaussian);
  log_report("multable_factor %f, multable_factor_gaussian %f\n",
             (float)prep->mul.multable_factor,
             (float)prep->mul.multable_factor_gaussian);
#endif


  create_cepstrum_offsets(prep);
  return;
}

void clear_preprocessed(preprocessed *prep)
/*
**  To setup the preprocessed structure
*/
{
  ASSERT(prep);
  destroy_cepstrum_offsets(prep);
  prep->dim = 0;
  FREE((char *)prep->last_frame);
  FREE((char *)prep->seq);
  FREE((char *)prep->seq_unnorm);
  return;
}

int get_data_frame(preprocessed *prep, utterance_info *utt)
/*
**  To get a frame amount of data and perform preprocessing functions
*/
{
  int status_code;

  ASSERT(prep);
  ASSERT(utt);
  if (utt->gen_utt.channorm && !utt->gen_utt.channorm->adj_valid)
    convert_adjustment_to_imelda(utt->gen_utt.channorm, prep);
  if (utt->gen_utt.dim != prep->dim)
    SERVICE_ERROR(UTTERANCE_DIMEN_MISMATCH);

  if (prep->post_proc & VFR)
  {
    if ((status_code = get_utterance_frame(prep, utt)) <= 0)
      return (status_code);

    log_report("get_data_frame vfr not supported\n");
    SERVICE_ERROR(FEATURE_NOT_SUPPORTED);
  }
  else
  {
    status_code = get_utterance_frame(prep, utt);
    if (status_code == 0) return(status_code);
    else if (status_code == -1) return(1);
  }

  if (prep->chan_offset)
    apply_channel_offset(prep);

  /*  Apply linear transformation if necessary
  */
  if (prep->post_proc & LIN_TRAN)
    linear_transform_frame(prep, prep->seq, True);

  memcpy(prep->seq_unnorm, prep->seq, prep->dim * sizeof(imeldata));
  if (utt->gen_utt.channorm)
    apply_channel_normalization_in_imelda(utt->gen_utt.channorm,
                                          prep->seq, prep->seq_unnorm,
                                          utt->gen_utt.channorm->dim);
  return (1);
}

int get_utterance_frame(preprocessed *prep, utterance_info *utt)
/*
**  To get a frame amount of data
**  Maintains a single data buffer and passes the pointers to frame of data.
**  Post-increments after copying
*/
{
  featdata  *frame_ptr;
  int ii;

  ASSERT(prep);
  ASSERT(utt);

  /*  Get the next data frame in
  */
  if (getFrameGap(utt->gen_utt.frame) > 0)
  {
    /*  is it a cloned object */
    if (prep->ref_count > 1 && compare_cached_frame(prep, utt))
      return (-1);

    frame_ptr = currentRECframePtr(utt->gen_utt.frame);
    if (frame_ptr == NULL)
      return (0);
    if (prep->ref_count > 1)
    {
      ASSERT(prep->last_frame);
      memcpy(prep->last_frame, frame_ptr,
             prep->dim* sizeof(featdata));
    }
    for (ii = 0; ii < utt->gen_utt.dim; ii++)
      prep->seq[ii] = (imeldata)frame_ptr[ii];
    /*  Apply fast-voice corrections if necessary */
    if (utt->gen_utt.frame->haveVoiced)
    {
      utterance_detection_fixup(utt->gen_utt.frame,
                                &utt->gen_utt.last_push, utt->gen_utt.voice_duration,
                                utt->gen_utt.quiet_duration, utt->gen_utt.unsure_duration);
      /*     if (isFrameBufferActive (utt->gen_utt.frame)
        && getFrameGap (utt->gen_utt.frame) <= utt->gen_utt.quiet_duration)
            SERVICE_ERROR (INTERNAL_ERROR); */
      prep->voicing_status =
        rec_frame_voicing_status(utt->gen_utt.frame);
    }
    return (1);
  }
  return (0);
}


int advance_utterance_frame(utterance_info *utt)
/*
**  To get a frame amount of data
*/
{
  ASSERT(utt);
  /*  if more samples are needed then read from file if the type matched
  */
  /*  Get the next data frame in
  */
  if (getFrameGap(utt->gen_utt.frame) > 0)
  {
    if (incRECframePtr(utt->gen_utt.frame) != False)
      return (0);
    return (1);
  }
  return (0);
}

int retreat_utterance_frame(utterance_info *utt)
/*
**  To get a frame amount of data
*/
{
  ASSERT(utt);

  if (getBlockGap(utt->gen_utt.frame) > 0)
  {
    if (decRECframePtr(utt->gen_utt.frame) != False)
      return (0);
    return (1);
  }
  return (0);
}

void prepare_data_frame(preprocessed *prep)
{
  int ii;
  prdata sum_sq;

  sum_sq = 0;

  for (ii = 0; ii < prep->whole_dim; ii++)
    sum_sq += (prdata) SQR((prdata)prep->seq[ii]);
  prep->seq_sq_sum_whole = -sum_sq;

  ASSERT(prep->whole_dim <= prep->use_dim);
  for (ii = 0; ii < prep->use_dim; ii++)
    sum_sq += (prdata) SQR((prdata)prep->seq[ii]);
  prep->seq_sq_sum = -sum_sq;

  sum_sq = 0;

  for (ii = 0; ii < prep->whole_dim; ii++)
    sum_sq += (prdata) SQR((prdata)prep->seq_unnorm[ii]);
  prep->seq_unnorm_sq_sum_whole = -sum_sq;

  return;
}

int utterance_started(utterance_info *utt)
{
  ASSERT(utt);
  if (utt->gen_utt.frame->haveVoiced
      && utt->gen_utt.frame->voicingDetected)
    return (True);
  else
    return (False);
}

int utterance_ended(utterance_info *utt)
{
  ASSERT(utt);
  return (utt->gen_utt.frame->utt_ended);
}

int load_utterance_frame(utterance_info *utt, unsigned char* pUttFrame, int voicing)
{
  featdata framdata[MAX_DIMEN];
  int      ii;

  ASSERT(utt);
  ASSERT(pUttFrame);

  for (ii = 0; ii < utt->gen_utt.frame->uttDim; ii++)
    framdata[ii] = (featdata) pUttFrame[ii];

  if (pushSingleFEPframe(utt->gen_utt.frame, framdata, voicing) != False)
    return (0);

  return (1);
}

int copy_utterance_frame(utterance_info *oututt, utterance_info *inutt)
{
  int      voicedata;
  featdata *framdata;

  ASSERT(oututt);
  ASSERT(inutt);

  if ((framdata = currentRECframePtr(inutt->gen_utt.frame)) == NULL)
    return (0);

  voicedata = getVoicingCode(inutt->gen_utt.frame, framdata);

  if (pushSingleFEPframe(oututt->gen_utt.frame, framdata, voicedata) != False)
    return (0);

  return (1);
}

int copy_pattern_frame(utterance_info *oututt, preprocessed *prep)
{
  int      ii;
  featdata frame_ptr[MAX_DIMEN];

  ASSERT(oututt);
  ASSERT(prep);
  ASSERT(oututt->gen_utt.dim < MAX_DIMEN);
  for (ii = 0; ii < oututt->gen_utt.dim; ii++)
    frame_ptr[ii] = (featdata) RANGE(prep->seq[ii], 0, 255);
  if (pushSingleFEPframe(oututt->gen_utt.frame, frame_ptr,
                         prep->voicing_status)
      != False) return(0);
  return (1);
}

static void create_cepstrum_offsets(preprocessed *prep)
{
  ASSERT(prep);
  prep->chan_offset = (imeldata *) CALLOC_CLR(prep->dim,
                      sizeof(imeldata), "srec.chan_offset");
  return;
}

void set_cepstrum_offset(preprocessed *prep, int index, int value)
{
  ASSERT(prep);
  ASSERT(prep->chan_offset);
  ASSERT(index >= 0 && index < prep->dim);
  prep->chan_offset[index] = (imeldata) value;
  return;
}

static void destroy_cepstrum_offsets(preprocessed *prep)
{
  ASSERT(prep);
  FREE((char *)prep->chan_offset);
  prep->chan_offset = 0;
  return;
}

static void apply_channel_offset(preprocessed *prep)
{
  int ii;

  for (ii = 0; ii < prep->dim; ii++)
    prep->seq[ii] += prep->chan_offset[ii];
  return;
}

static int compare_cached_frame(preprocessed *prep, utterance_info *utt)
{
  int      ii;
  featdata *frame_ptr;

  frame_ptr = currentRECframePtr(utt->gen_utt.frame);
  if (frame_ptr == NULL)
    return (False);
  for (ii = 0; ii < utt->gen_utt.dim; ii++)
    if (prep->last_frame[ii] != frame_ptr[ii])
      return (False);
  return (True);
}

void convert_adjustment_to_imelda(norm_info *norm, preprocessed *prep)
{
  int      ii;
  imeldata fram[MAX_DIMEN];

  ASSERT(prep);
  ASSERT(norm);
  for (ii = 0; ii < 12; ii++)      /* TODO: fix dimension properly, and sort out rouding/type */
    fram[ii] = (imeldata) norm->adjust[ii]; /* TODO: review types */
  for (; ii < prep->dim; ii++)
    fram[ii] = 0;

  linear_transform_frame(prep, fram, False);

  for (ii = 0; ii < prep->dim; ii++)
    norm->imelda_adjust[ii] = fram[ii];
#if DEBUG
  log_report("NORM AUX: ");
  for (ii = 0; ii < norm->dim; ii++)
    log_report("%d ", (int)norm->imelda_adjust[ii]);
  log_report("\n");
#endif
  norm->adj_valid = True;
  return;
}
