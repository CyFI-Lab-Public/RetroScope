/*---------------------------------------------------------------------------*
 *  specnorm.h  *
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



#ifndef __specnorm_h
#define __specnorm_h

#ifdef SET_RCSID
static const char specnorm_h[] = "$Id: specnorm.h,v 1.1.10.4 2007/08/31 17:44:53 dahan Exp $";
#endif



#include "hmm_type.h"


#define USE_MEDIAN 0
#define USE_MEAN 1

#define C0_MARGIN 19
#define MEDIAN_SPREAD 20
#define MIN_COUNT 1 /* was 20 */

#define SV6_TO_MEAN 30 /* units of C0 */

#define UNIT_SIZE 1

/**
 * @todo document
 */
typedef struct
{
  int  gain_used;
  int  offset;
  int  forget_factor;
  int  estimate_period;
  long count;
  long *hist;
  int  low_entry;
  int  high_entry;
  long high_counts;
  long low_counts;
  int  perc_high;
  int  estimate_percentile;
  int  sv6_margin;
  int  sv6;
  int  median;
  int  mean;
  int  devn;
  long mean_count;
  long running_total;
  long running_total_devn;
}
spect_dist_info;

spect_dist_info *create_spectrum_distribution(int offset, int initial_median,
    int low_entry, int high_entry,
    int forget_factor,
    int estimate_period, int estimate_percentile,
    int sv6_margin);                                
                                
void destroy_spectrum_distribution(spect_dist_info *spec);
void clear_distribution_counts(spect_dist_info *spec);
void clear_mean_counts(spect_dist_info *spec);
void forget_distribution_counts(spect_dist_info *spec, int forget_factor);
void shift_distribution_counts(spect_dist_info *spec, int shift);
int  add_distribution_data(spect_dist_info *spec, int spec_val);
void evaluate_parameters(spect_dist_info *spec);
int  estimate_percentile(spect_dist_info *spec, int percentile);
void estimate_mean(spect_dist_info *spec, int forget_factor);
void estimate_sv6(spect_dist_info *spec);
int  median_normalize_data(spect_dist_info *spec, int spec_val);
int  mean_normalize_data(spect_dist_info *spec, int spec_val);
int  sv6_normalize_data(spect_dist_info *spec, int spec_val);
void shift_parameters(spect_dist_info *spec, int shift);


#endif
