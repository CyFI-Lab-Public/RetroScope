/*---------------------------------------------------------------------------*
 *  pre_desc.h  *
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



#ifndef _h_pre_desc_
#define _h_pre_desc_

#ifdef SET_RCSID
static const char pre_desc_h[] = "$Id: pre_desc.h,v 1.3.6.10 2008/03/07 19:41:39 dahan Exp $";
#endif


#include "all_defs.h"
#include "hmm_type.h"
#include "specnorm.h"
#ifndef _RTT
#include "duk_io.h"
#endif

#define DO_SUBTRACTED_SEGMENTATION  0

#ifndef NONE
#define NONE   0
#endif
#define SCALE   1 /* Scaling the channels */
#define LIN_TRAN  2 /* Linear Transformation */
#define VFR   4 /* Variable frame rate */
#define USE_MULTAB      8 /* Set up multable distance calculations */

/**
 * @todo document
 */
typedef struct
{  /* mul-table data types */
  unsigned short sigma;
  int   num;
  short *pdf;
}
mul_table;

/**
 * @todo document
 */
typedef struct
{
  unsigned short num_dev8_index;
  unsigned char  *dev8_index;
  unsigned short *wt_index;
  short    *gauss_dist_table;
  short    **dist_ptr;
  prdata    multable_factor; /* euclidean to multable */
  prdata    multable_factor_gaussian; /* euclidean to multable */
  prdata    grand_mod_cov; /* grand covariance modulus */
  prdata    grand_mod_cov_gaussian; /* grand covariance modulus */
}
mul_table_info;

/**
 * @todo document
 */
typedef struct
{
  const prdata *table;
  prdata add_log_limit;
  prdata scale;   /* X - scale to log function */
  prdata inv_scale;
  float logscale;  /* Y - scale to log function */
}
logadd_table_info;

/**
 * @todo document
 */
typedef struct
{
  unsigned long num;
  accdata **between;
  accdata *bmean;
  accdata **within;
  accdata *wmean;
}
transform_info;

/**
 * @todo document
 */
typedef struct
{   /* Segmentation parameters */
  int  rel_low;
  int  rel_high;
  int  gap_period;
  int  click_period;
  int  breath_period;
  int  extend_annotation;
  int  param;
  int         min_initial_quiet_frames;    /* num silence frames needed before input */
  int         min_annotation_frames;          /* minimum overall length */
  int         max_annotation_frames;          /* maximum overall length */
  int         delete_leading_segments;        /* num segments to delete. 0=no action */
  int         leading_segment_accept_if_not_found; /* Do not reject segmentation if not found */
  int         leading_segment_min_frames;   /* remove unless shorter */
  int         leading_segment_max_frames;   /* remove unless exceeded */
  int         leading_segment_min_silence_gap_frames;/* remove if good silence gap to next segment */
  int  beep_size;  /*X201 beep filter */
  int  beep_threshold;  /*X201 beep filter */
  int  min_segment_rel_c0; /* Any segment gets deleted whose peak c0 is < max - min_segment_rel_c0 */

#if DO_SUBTRACTED_SEGMENTATION
  int         snr_holdoff;    /* Ignore first n frames when estimating speech level for SNR measure */
  int         min_acceptable_snr; /* for an acceptable segmentation */
#endif
}
endpoint_info;


/**
 * @todo document
 */
typedef struct
{  /* processed speech data/front end output */
  int  ref_count; /* reference counts */
  /* Pattern vector section */
  int  dim;  /* dimension of frame vector */
  int  use_dim; /* dimension used for recognition */
  int  whole_dim; /* reduced feature use. Set unused to 127 (0) on model construction */
  int  use_from; /* first channel used for recognition */
  featdata *last_frame; /* last frame processed in frame buffer */
  imeldata *seq;  /* current valid frame */
  imeldata *seq_unnorm; /* current valid frame, for whole-word models */
  prdata seq_sq_sum; /* sum of the squared of frames */
  prdata seq_sq_sum_whole; /* sum of the squared of frames, for wholeword */
  prdata seq_unnorm_sq_sum_whole; /* sum of the squared of frames, for wholeword */
  int  voicing_status; /* voicing code */
  int  post_proc; /* post processing functions */
  imeldata *offset; /* offset vector with transformation */
  imeldata **matrix; /* linear transformation matrix */
  int  imel_shift; /* Imelda scale factor (in shifts) */
  covdata **imelda; /* linear transformation matrix, PMC or RN */
  imeldata **invmat; /* inverse transformation matrix */
  int  inv_shift; /* inverse Imelda scale factor (in shifts) */
  covdata **inverse; /* inverse linear transformation matrix, PMC or RN */
#if PARTIAL_DISTANCE_APPROX /* Gaussian tail approximation? */
  int  partial_distance_calc_dim;  /* number of params to calc distance over, before approximating if beyond threshold */
  scodata partial_distance_threshold;
  prdata partial_distance_calc_threshold;
  prdata partial_distance_offset;
  prdata global_distance_over_n_params;
  int  global_model_means[MAX_DIMEN];
  prdata partial_mean_sq_sum;
  prdata partial_seq_sq_sum;
  prdata partial_seq_unnorm_sq_sum;
#endif
  imeldata *chan_offset;
  /* Channel Normalization etc */

  /* Tables */
  prdata exp_wt[MAX_WTS]; /* weights exp lookup table */
  mul_table_info mul;  /* Mul-table */
  logadd_table_info add; /* logadd-table */
  /* ENC */
  booldata is_setup_for_noise;
  booldata do_whole_enc; /* to enable ENC */
  booldata do_sub_enc; /* to enable ENC */
  booldata enc_count;
  booldata ambient_valid; /* ambient estimates valid */
  imeldata **pmc_fixmat; /* ENC matrix */
  imeldata **pmc_fixinv; /* inverse ENC matrix */
  covdata **pmc_matrix; /* ENC matrix in float */
  covdata **pmc_inverse; /* inverse ENC matrix in float */
  int  pmc_matshift; /* scaling */
  int  pmc_invshift; /* scaling */
  imeldata    *ambient_mean; /* ambient mean vector */
  imeldata    *ambient_prof; /* ambient estimates, pseudo space */
  imeldata    *ambient_prof_unnorm; /* ambient estimates, unnormalised */
  logadd_table_info fbadd; /* logadd-table for ENC */
#if DO_SUBTRACTED_SEGMENTATION
  int  mel_dim;
  covdata **spec_inverse;
  imeldata **spec_fixinv;
  int  spec_invshift;
  int  *cep_offset;
#endif
  /* Parameters */
  prdata mix_score_scale; /* Mixture score scaling constant */
  prdata uni_score_scale; /* Unimodal score scaling constant */
  prdata uni_score_offset; /* Unimodal score offset constant */
  prdata imelda_scale;  /* Imelda grand variance */
  /* Endpoint data */
  endpoint_info end;

}
preprocessed;

/**
 * @todo document
 */
typedef struct
{
  preprocessed    *prep; /* The preprocessed data structure */
  /* The following stuff cannot be cloned */
  booldata do_imelda; /* Alignment based accumulation */
  transform_info  imelda_acc;
}
pattern_info;

#endif /* _h_pre_desc_ */
