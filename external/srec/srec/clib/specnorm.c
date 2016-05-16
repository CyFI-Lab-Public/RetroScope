/*---------------------------------------------------------------------------*
 *  specnorm.c  *
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

#include "duk_err.h"
#include "specnorm.h"
#include "portable.h"

#define DEBUG   0

static const char specnorm[] = "$Id: specnorm.c,v 1.2.10.7 2007/10/15 18:06:24 dahan Exp $";

int copy_distribution_counts(spect_dist_info *spec, spect_dist_info *base);


int add_distribution_data(spect_dist_info *spec, int spec_val)
{
  /*  Median calculations
  */
  ASSERT(spec);
#if USE_MEDIAN
  if (spec_val < spec->low_entry) spec->low_counts += UNIT_SIZE;
  else if (spec_val > spec->high_entry) spec->high_counts += UNIT_SIZE;
  else spec->hist[spec_val - spec->low_entry] += UNIT_SIZE;
#endif
  
  /*  Mean calculations
  */
#if 1
  spec->running_total += spec_val - spec->mean;
  spec->running_total_devn += (spec_val - spec->mean)
                              * (spec_val - spec->mean);
#else
  spec->running_total += spec_val;
  spec->running_total_devn += spec_val * spec_val;
#endif
                              
  spec->count++;
  if (spec->estimate_period > 0 && spec->count >= spec->estimate_period)
  {
    evaluate_parameters(spec);
    spec->gain_used = False;
    spec->count = 0;
    return (1);
  }
  return (0);
}

void evaluate_parameters(spect_dist_info *spec)
{
  ASSERT(spec);
#if USE_MEDIAN
  estimate_sv6(spec);
  spec->median = estimate_percentile(spec, spec->estimate_percentile);
  spec->perc_high = estimate_percentile(spec, 90);  /* check this value */
#endif
#if USE_MEAN
  estimate_mean(spec, spec->forget_factor);
#endif
#if USE_MEDIAN
  forget_distribution_counts(spec, spec->forget_factor);
#endif
  spec->count = 0;
  return;
}

#if USE_MEDIAN
int estimate_percentile(spect_dist_info *spec, int percentile)
{
  int ii, jj, count, cumval;
  long    accum = 0;
  
  /*  Calculate the median
  */
  ASSERT(spec);
  if (spec->count < MIN_COUNT) return(spec->median);
  if (percentile == 0)
    percentile = spec->estimate_percentile;
  count = spec->low_counts + spec->high_counts;
  for (ii = 0; ii <= (spec->high_entry - spec->low_entry); ii++)
    count += spec->hist[ii];
  count = (count * percentile) / 100;
  
  cumval = spec->low_counts;
  for (ii = 0; ii <= (spec->high_entry - spec->low_entry)
       && cumval < count; ii++)
    cumval += spec->hist[ii];
    
  count = 0;
  for (jj = ii; jj <= (spec->high_entry - spec->low_entry); jj++)
  {
    count += spec->hist[jj];
    accum += spec->hist[jj] * (jj - ii);
  }
#if DEBUG
  if (count > 0)
    log_report("Median margin %d\n", accum / count);
#endif
  return (spec->low_entry + ii);
}

void estimate_sv6(spect_dist_info *spec)
{
  int ii, jj, count, span, totcount;
  long    accum;
  
  /*  Calculate the median
  */
  ASSERT(spec);
  if (spec->count < MIN_COUNT) return;
  count = spec->high_counts;
  accum = 0;
  span = spec->high_entry - spec->low_entry;
  for (ii = 0, jj = spec->high_entry - spec->low_entry;
       ii <= span; ii++, jj--)
  {
    count += spec->hist[jj];
    accum += spec->hist[jj] * ii;
    if (count > 0 && (ii - accum / count) > spec->sv6_margin)
      break;
  }
  
  totcount = count;
  for (; ii <= span; ii++, jj--)
    totcount += spec->hist[jj];
  totcount += spec->high_counts;
  
#if DEBUG
  log_report("SV6 (%d) Percentage %d, %d, %d\n", spec->sv6_margin,
             (count * 100) / totcount,
             totcount, spec->count);
#endif
  if (count > 0)
    spec->sv6 = spec->high_entry - accum / count;
  return;
}
#endif

void estimate_mean(spect_dist_info *spec, int forget_factor)
{
  /*  Calculate the mean and standard deviation
  */
  ASSERT(spec);
  if (spec->count < MIN_COUNT) return;
#if DEBUG
  log_report("old mean= %d, ", spec->mean);
#endif
  spec->mean_count = (spec->mean_count * (100 - forget_factor)) / 100;
  spec->mean_count += spec->count;
  if (spec->mean_count > 0)
  {
    spec->devn = spec->running_total_devn / spec->mean_count
                 - (spec->running_total * spec->running_total)
                 / (spec->mean_count * spec->mean_count);
    spec->devn = (int) sqrt((double)  spec->devn);
    if (spec->running_total >= 0)
      spec->mean += (spec->running_total + spec->mean_count / 2)
                    / spec->mean_count;
    else
      spec->mean += (spec->running_total - spec->mean_count / 2)
                    / spec->mean_count;
  }
#if DEBUG
  log_report("accumulates= %d and %d (%d), ", spec->running_total,
             spec->mean_count, spec->count);
  log_report("new mean= %d\n", spec->mean);
#endif
  spec->running_total = 0;
  spec->running_total_devn = 0;
  
  return;
}

#if USE_MEDIAN
int median_normalize_data(spect_dist_info *spec, int spec_val)
{
  return (spec_val - spec->median + spec->offset);
}

int sv6_normalize_data(spect_dist_info *spec, int spec_val)
{
  return (spec_val - spec->sv6 + spec->offset);
}
#endif

int mean_normalize_data(spect_dist_info *spec, int spec_val)
{
  return (spec_val - spec->mean + spec->offset);
}

spect_dist_info *create_spectrum_distribution(int offset, int initial_median,
    int low_entry, int high_entry,
    int forget_factor, int estimate_period,
    int estimate_percentile,
    int sv6_margin)
{
  spect_dist_info *spec;
  
  if (high_entry < low_entry) return(NULL);
  
  spec = (spect_dist_info *) CALLOC_CLR(1,
         sizeof(spect_dist_info), "clib.spec");
  if (estimate_period == 0) /* basically disable 0 as an estimate period */
    spec->estimate_period = 1;
  else
    spec->estimate_period = estimate_period;
  spec->forget_factor = forget_factor;
  spec->offset = offset;
  
#if USE_MEDIAN
  spec->hist = (long *) CALLOC_CLR(high_entry - low_entry + 1,
                                         sizeof(int), "clib.spec.hist");
  spec->low_entry = low_entry;
  spec->high_entry = high_entry;
  spec->median = initial_median;
  spec->estimate_percentile = estimate_percentile;
  spec->sv6_margin = sv6_margin;
  clear_distribution_counts(spec);
#endif
#if USE_MEAN
  spec->mean = initial_median;
  spec->devn = 0;
  clear_mean_counts(spec);
#endif
  spec->sv6 = initial_median;
  
  return (spec);
}

void destroy_spectrum_distribution(spect_dist_info *spec)
{
  ASSERT(spec);
#if USE_MEDIAN
  FREE((char *)spec->hist);
#endif
  FREE((char *)spec);
  return;
}

#if USE_MEDIAN
void clear_distribution_counts(spect_dist_info *spec)
{
  int ii;
  
  ASSERT(spec);
  spec->high_counts = 0;
  spec->low_counts = 0;
  spec->count = 0;
  for (ii = 0; ii <= (spec->high_entry - spec->low_entry); ii++)
    spec->hist[ii] = 0;
  return;
}

int copy_distribution_counts(spect_dist_info *spec, spect_dist_info *base)
{
  int ii;
  
  ASSERT(spec);
  ASSERT(base);
  ASSERT(spec->hist);
  ASSERT(base->hist);
  if (base->low_entry != spec->low_entry ||
      base->high_entry != spec->high_entry)
    return (False);
  spec->high_counts = base->high_counts;
  spec->low_counts = base->low_counts;
  for (ii = 0; ii <= (spec->high_entry - spec->low_entry); ii++)
    spec->hist[ii] = base->hist[ii];
  return (True);
}

void forget_distribution_counts(spect_dist_info *spec, int forget_factor)
{
  int ii, remember;
  
  ASSERT(spec);
  /*    remember= 100 - (forget_factor * spec->count)/spec->estimate_period; */
  remember = 100 - forget_factor;
  spec->high_counts = (spec->high_counts * remember) / 100;
  spec->low_counts = (spec->low_counts * remember) / 100;
  for (ii = 0; ii <= (spec->high_entry - spec->low_entry); ii++)
    spec->hist[ii] = (spec->hist[ii] * remember) / 100;
  return;
}

void shift_distribution_counts(spect_dist_info *spec, int shift)
{
  int ii;
  
  ASSERT(spec);
  if (shift > 0)
  {
    if (shift > (spec->high_entry - spec->low_entry))
      SERVICE_ERROR(UNEXPECTED_DATA_ERROR); /* TODO: find a new error code */
    for (ii = 0; ii < shift; ii++)
      spec->high_counts += spec->hist[spec->high_entry
                                      - spec->low_entry - shift + ii];
                                      
    MEMMOVE(&spec->hist[shift], spec->hist,
            (spec->high_entry - spec->low_entry - shift + 1),
            sizeof(int));
    for (ii = 0; ii < shift; ii++)
      spec->hist[ii] = 0;
  }
  else if (shift < 0)
  {
    if (shift < (spec->low_entry - spec->high_entry))
      SERVICE_ERROR(UNEXPECTED_DATA_ERROR); /* TODO: find a new error code */
    for (ii = 0; ii < -shift; ii++)
      spec->low_counts += spec->hist[ii];
      
    MEMMOVE(spec->hist, spec->hist - shift,
            (spec->high_entry - spec->low_entry + shift + 1),
            sizeof(int));
    for (ii = shift; ii < 0; ii++)
      spec->hist[ii + spec->high_entry - spec->low_entry + 1] = 0;
  }
  return;
}
#endif

void clear_mean_counts(spect_dist_info *spec)
{
  ASSERT(spec);
  spec->mean_count = 0;
  spec->count = 0;
  spec->running_total = 0;
  spec->running_total_devn = 0;
  return;
}

void shift_parameters(spect_dist_info *spec, int shift)
{
  ASSERT(spec);
  spec->mean += shift;
#if USE_MEDIAN
  spec->median += shift;
  spec->sv6 += shift;
#endif
  return;
}
