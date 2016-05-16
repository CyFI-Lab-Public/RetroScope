/*---------------------------------------------------------------------------*
 *  channorm.h  *
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




#ifndef __channorm_h
#define __channorm_h

#ifdef SET_RCSID
static const char channorm_h[] = "$Id: channorm.h,v 1.1.10.4 2007/08/31 17:44:52 dahan Exp $";
#endif

#include "all_defs.h"
#include "hmm_type.h"
#include "specnorm.h"


typedef struct
{
  int dim;
  int init[MAX_CHAN_DIM];              /*  Values located in the .CMN file */
  int target[MAX_CHAN_DIM];            /*  Values located in the .TMN file */
  int adjust[MAX_CHAN_DIM];
  int adj_valid;
  imeldata imelda_adjust[MAX_CHAN_DIM];
}
norm_info;

norm_info *create_channel_normalization(void);
void destroy_channel_normalization(norm_info *channorm);
void apply_channel_normalization(norm_info *channorm, imeldata *fram,
                                 int dimen);
void apply_channel_normalization_in_imelda(norm_info *channorm,
    imeldata *outframe,
    imeldata *frame, int dimen);
void estimate_normalization_parameters(norm_info *channorm,
                                       spect_dist_info **chandata, int dimen);
void setup_channel_normalization(norm_info *channorm,
                                 spect_dist_info **chandata, int dimen,
                                 int forget_factor);
void clear_channel_normalization(spect_dist_info **chandata, int dimen);
int load_channel_parameters(char *basename, norm_info *channorm);

void setup_ambient_estimation(spect_dist_info **backchan, int dimen,
                              int forget_factor);
void clear_ambient_estimation(spect_dist_info **backchan, int dimen);


#endif
