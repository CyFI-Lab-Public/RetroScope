/*---------------------------------------------------------------------------*
 *  prelib.h  *
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


#ifndef _h_prelib_
#define _h_prelib_

#ifdef SET_RCSID
static const char prelib_h[] = "$Id: prelib.h,v 1.2.6.7 2008/04/01 18:23:20 dahan Exp $";
#endif


#include "pre_desc.h"

void    init_preprocessed(preprocessed *datapak, int dimen,
                          float imelda_scale);
void init_partial_distance_approx(preprocessed *prep);
void clear_partial_distance_approx(preprocessed *prep);
void    clear_preprocessed(preprocessed *datapak);

void    linear_transform_frame(preprocessed *datapak, imeldata *fram, int do_shift);
void    inverse_transform_frame (preprocessed *prep, imeldata *fram, int do_shift);

#ifdef __cplusplus
extern "C"
{
#endif

  void create_lookup_logadd(logadd_table_info *table, float mul_scale);

#ifdef __cplusplus
}
#endif

void destroy_lookup_logadd(logadd_table_info *table);

void create_linear_transform(preprocessed *datapak, int matdim,
                             int with_offset);
void free_linear_transform(preprocessed *datapak);
#ifndef _RTT
int  init_newton_transform(preprocessed *datapak, float reqscale,
                           char *filename, int dimen);
void copy_linear_transform(preprocessed *datapak, float **new_matrix,
                           int dimen);
void load_imelda_transform(char *filename, int dim);
#endif

void set_cepstrum_offset(preprocessed *prep, int index, int value);

int voicing_bit(preprocessed *predat);

#if DO_SUBTRACTED_SEGMENTATION
void setup_spectrum_transform(preprocessed *prep, int numceps,
                              float *stat_sc, float *stat_off);
void clear_spectrum_transform(preprocessed *prep);
#endif

#endif
