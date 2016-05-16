/*---------------------------------------------------------------------------*
 *  cnorm_tr.c  *
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


#ifndef _RTT
#include <stdio.h>
#endif
#include <stdlib.h>
#include <math.h>
#include <string.h>
#include <assert.h>

#include "channorm.h"
#include "prelib.h"
#ifndef _RTT
#include "duk_io.h"
#endif
#include "portable.h"

#define DEBUG   0

#define ESTIMATE_PERIOD  -1
#define BACK_ESTIMATE_PERIOD    1000
#define ESTIMATE_PERCENTILE 50


static const char cnorm_tr[] = "$Id: cnorm_tr.c,v 1.4.10.6 2007/10/15 18:06:24 dahan Exp $";

norm_info *create_channel_normalization()
{
  norm_info *channorm;
  
  channorm = (norm_info *) CALLOC_CLR(1, sizeof(norm_info), "clib.channorm");
  return (channorm);
}

void destroy_channel_normalization(norm_info *channorm)
{
  ASSERT(channorm);
  FREE((char *)channorm);
  return;
}

void apply_channel_normalization_in_imelda(norm_info *channorm,
    imeldata *outframe, imeldata *frame,
    int dimen)
{
  int ii;
  
  ASSERT(channorm);
  ASSERT(frame);
  ASSERT(outframe);
  ASSERT(dimen <= channorm->dim);
  for (ii = 0; ii < dimen; ii++)
    outframe[ii] = MAKEBYTE(frame[ii] + channorm->imelda_adjust[ii]);
  return;
}

void estimate_normalization_parameters(norm_info *channorm,
                                       spect_dist_info **chandata, int dimen)
{
  int ii, adjust;
  
  ASSERT(channorm);
  ASSERT(chandata);
  ASSERT(dimen <= channorm->dim);
  for (ii = 0; ii < dimen; ii++)
    if (chandata[ii])
    {
      evaluate_parameters(chandata[ii]);
      /*  The additive expression is due to
      **  the normalization taking place before the
      **  utterance object is created
      */
      adjust = mean_normalize_data(chandata[ii], 0);
      /*     channorm->adjust[ii]= adjust; */
#if USE_MEDIAN
      shift_distribution_counts(chandata[ii], adjust);
#endif
      shift_parameters(chandata[ii], adjust);
#if NORM_IN_IMELDA
      channorm->imelda_adjust[ii] += adjust;
#else
      channorm->adjust[ii] += adjust;
#endif
    }
#if NORM_IN_IMELDA
  channorm->adj_valid = True;
#if DEBUG
  log_report("NORM IML: ");
  for (ii = 0; ii < channorm->dim; ii++)
    log_report("%d ", channorm->imelda_adjust[ii]);
  log_report("\n");
#endif
#else
  channorm->adj_valid = False;
#if DEBUG
  log_report("NORM ADJ: ");
  for (ii = 0; ii < channorm->dim; ii++)
    log_report("%d ", channorm->adjust[ii]);
  log_report("\n");
#endif
#endif
  return;
}

void setup_channel_normalization(norm_info *channorm,
                                 spect_dist_info **chandata, int dimen,
                                 int forget_factor)
{
  int ii;
  
  ASSERT(channorm);
  ASSERT(chandata);
  for (ii = 0; ii < dimen; ii++)
  {
#if MODEL_BASED || 1
    chandata[ii] = create_spectrum_distribution(
                     128, 128,
                     0, 255, forget_factor, ESTIMATE_PERIOD,
                     ESTIMATE_PERCENTILE, 10);
#else
    chandata[ii] = create_spectrum_distribution(
                     channorm->chan_tgt[ii], channorm->chan_init[ii],
                     0, 511, forget_factor, ESTIMATE_PERIOD,
                     ESTIMATE_PERCENTILE, 10);
#endif
    channorm->adjust[ii] = channorm->target[ii]
                           - channorm->init[ii];
  }
  channorm->adj_valid = False;
  return;
}

void clear_channel_normalization(spect_dist_info **chandata, int dimen)
{
  int ii;
  
  ASSERT(chandata);
  for (ii = 0; ii < dimen; ii++)
    if (chandata[ii])
    {
      destroy_spectrum_distribution(chandata[ii]);
      chandata[ii] = NULL;
    }
  return;
}

void setup_ambient_estimation(spect_dist_info **backchan, int dimen,
                              int forget_factor)
{
  int ii;
  
  ASSERT(backchan);
  for (ii = 0; ii < dimen; ii++)
    backchan[ii] = create_spectrum_distribution(
                     0, 0, 0, 255, forget_factor, BACK_ESTIMATE_PERIOD,
                     ESTIMATE_PERCENTILE, 10);
  return;
}

void clear_ambient_estimation(spect_dist_info **backchan, int dimen)
{
  int ii;
  
  ASSERT(backchan);
  
  for (ii = 0; ii < dimen; ii++)
    if (backchan[ii])
    {
      destroy_spectrum_distribution(backchan[ii]);
      backchan[ii] = NULL;
    }
  return;
}

