/*---------------------------------------------------------------------------*
 *  frontpar.h  *
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



#ifndef _h_frontpar_
#define _h_frontpar_

#include "hmm_type.h"

/**
 * @todo document
 */
typedef struct
{
  int  ca_rtti;
  booldata    is_loaded;
  int   mel_dim;
  int   samplerate;
  float window_factor;
  float pre_mel;
  int   low_cut;
  int   high_cut;
  ESR_BOOL  do_skip_even_frames;
  ESR_BOOL  do_smooth_c0;
  float offset;
  ESR_BOOL  do_dd_mel;
  int   forget_factor;            /* preserve % of previous hist */
  int   sv6_margin;
  ESR_BOOL  do_rastac0;           /* rasta c0 is skipped if false. Don't really need this now.    */
  ESR_BOOL  do_spectral_sub;
  int   spectral_sub_frame_dur;
  float spec_sub_scale;
  ESR_BOOL  do_filterbank_dump;
  ESR_BOOL  do_filterbank_input;
  int   num_fb_to_use;
  int   lpc_order;
  float peakpickup;
  float peakpickdown;
  float warp_scale;                      /*## */
  float piecewise_start;                      /*## */
  int melA_scale[MAX_CEP_DIM];
  int melB_scale[MAX_CEP_DIM];
  int dmelA_scale[MAX_CEP_DIM];
  int dmelB_scale[MAX_CEP_DIM];
  int ddmelA_scale[MAX_CEP_DIM];
  int ddmelB_scale[MAX_CEP_DIM];
  int rastaA_scale[MAX_CEP_DIM];
  int rastaB_scale[MAX_CEP_DIM];
  int mel_offset[MAX_CHAN_DIM];
  int mel_loop[MAX_CHAN_DIM];
  int   spectrum_filter_freq[MAX_FILTER_NUM];
  int   spectrum_filter_spread[MAX_FILTER_NUM];
  int   voice_margin;
  int   fast_voice_margin;
  int   tracker_margin;
  int   voice_duration;
  int   quiet_duration;
  int   unsure_duration;
  int   start_windback;
  int   high_clip;
  int   low_clip;
  int   max_per10000_clip;
  int   max_dc_offset;
  int   high_noise_level_bit;
  int   low_speech_level_bit;
  int   min_samples;
}
front_parameters;

#endif /*_h_frontpar_ */


