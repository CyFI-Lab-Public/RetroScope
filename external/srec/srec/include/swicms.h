/*---------------------------------------------------------------------------*
 *  swicms.h                                                                 *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                         *
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

#ifndef __SWICMS_H__
#define __SWICMS_H__

#include"all_defs.h"
#include"sizes.h"
#include"fronttyp.h"
#include"pre_desc.h"

#define DEBUG_SWICMS        0
#define MAX_CACHED_FRAMES 800
#define SWICMS_CACHE_RESOLUTION_DEFAULT   8
#define SWICMS_CACHE_SIZE_DEFAULT         100 /* equals #frames/resolution */

/**
 * This is used for casting in debugger, just type (imelvec*)tmn.
 */
typedef struct
{
  imeldata vec[MAX_CHAN_DIM];
}
imelvec;

/**
 * Does channel normalization without using fine recognition segmenation.  It remembers the
 * frames of speech and uses that as a channel mean for the next utterance.  A forget_factor
 * is used to weigh the new speech mean estimate with an older one.
 */
typedef struct
{
  imeldata tmn [MAX_CHAN_DIM];                 /* target mean */
  imeldata cmn [MAX_CHAN_DIM];                 /* channel mean */

  imeldata lda_tmn [MAX_CHAN_DIM];                 /* target mean */
  imeldata lda_cmn [MAX_CHAN_DIM];                 /* channel mean */

  imeldata adjust[MAX_CHAN_DIM]; /* target less channel */

  int is_valid;
  int forget_factor;           /* in frames, mass of cmn average */
  int sbindex;                 /* speech to background index
        100 -> use only speech to calculate CMN
        000 -> use only background to calculate CMN
        050 -> use half/half ..
        all numbers in between are acceptable */

  int num_frames_in_cmn; /* num frames used to estimate cmn (or lda_cmn) */

  /* for in-utterance channel normalization */
  struct {
    int forget_factor2;     /* cmn is given this weight to start off */
    int disable_after;      /* we disable in-utt cms after this many fr*/
    int enable_after;       /* we enable in-utt cms after this many fr*/
    int num_bou_frames_to_skip;   /* don't start accum 'til this many frames */
    int num_frames_since_bou;     /* counter for above, bou=begin-of-utt     */
    int num_frames_in_accum;      /* number of frames in accum */
    imeldata accum[MAX_CHAN_DIM]; /* accumulates frames of the current utt */
  } inutt;

  int cached_num_frames;       /* we cache frames, until recognition is done
        and can calculate speech mean from these */
  int cache_resolution;        /* we'll avg this many frames per section */
  imeldata cached_sections[SWICMS_CACHE_SIZE_DEFAULT][MAX_CHAN_DIM];
  /*const*/ preprocessed* _prep;
}
swicms_norm_info;

int swicms_init(swicms_norm_info* swicms);
int swicms_cache_frame(swicms_norm_info* swicms, imeldata* frame, int dimen);
int apply_channel_normalization_in_swicms(swicms_norm_info *swicms,
    imeldata* oframe, imeldata* iframe,
    int dimen);
int swicms_lda_process(swicms_norm_info* swicms, preprocessed* prep);

int swicms_update(swicms_norm_info* swicms, int speech_start_frame, int speech_end_frame);

ESR_ReturnCode swicms_set_cmn(swicms_norm_info *swicms, const LCHAR *new_cmn_params );
ESR_ReturnCode swicms_get_cmn(swicms_norm_info *swicms, LCHAR *cmn_params, size_t* len );

#if DEBUG_SWICMS
int swicms_compare(swicms_norm_info* swicms, imeldata* imelda_adjust);
int swicms_dump_stats(swicms_norm_info* swicms);
#else
#define swicms_compare(swicms,ia)
#define swicms_dump_stats(swicms)
#endif

#endif

