/*---------------------------------------------------------------------------*
 *  wav_acc.c  *
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



static const char wav_acc_c[] = "$Id: wav_acc.c,v 1.6.6.7 2007/10/15 18:06:24 dahan Exp $";

#ifndef _RTT
#include "pstdio.h"
#endif
#include <stdlib.h>
#include <limits.h>
#include <math.h>
#include <string.h>
#include "passert.h"
#include "pendian.h"
#include "portable.h"


#ifndef _RTT
#include "duk_io.h"
#endif

#include "sample.h"
#include "mulaw.h"

#include "portable.h"


void create_sample_buffer(wave_info *wave, int frame_size, int window_size)
{
  ASSERT(wave);
  ASSERT(frame_size > 0);
  ASSERT(window_size >= frame_size);
  wave->income = (samdata *) CALLOC(window_size, sizeof(samdata), "cfront.wave.income");
  wave->outgo = (samdata *) CALLOC(window_size, sizeof(samdata), "cfront.wave.outgo");
  wave->window_size = window_size;
  wave->frame_size = frame_size;
#if DEBUG
  log_report("window %d frame %d\n", window_size, frame_size);
#endif
  return;
}

void free_sample_buffer(wave_info *wave)
{
  ASSERT(wave);
  if (wave->income)
    FREE((char *)wave->income);
  if (wave->outgo)
    FREE((char *)wave->outgo);
  wave->income = NULL;
  wave->outgo = NULL;
  wave->window_size = 0;
  wave->frame_size = 0;
  return;
}

void reset_sig_check(wave_stats *ws)
/*
**  Resets the wave statistics
*/
{
  int ii;

  ASSERT(ws);

  ws->sum  = 0;
  ws->sum2 = 0;
  ws->sumsqu = 0;
  ws->sumsqu2 = 0;
  ws->nsam = 0;
  ws->highclip = 0;
  ws->lowclip = 0;

  for (ii = 0; ii < MAXHISTBITS; ii++)
    ws->bithist[ii] = 0;
}

#define OVERFLOW_MASK 0x40000000

void get_sig_check(wave_stats *ws, int *nsam, int *pclowclip, int *pchighclip,
                   int *dc_offset, int *amp, int *pc5, int *pc95,
                   int *overflow)
/*
**  Returns the wave statistics
*/
{
  float mean;
  int num;
  int ntot;
  int npc;
  int ii;
  float sqr_devn;

  ASSERT(ws);

  /* *nsam = ws->nsam / 100; */
  *nsam = ws->nsam;

  *overflow = 0;

  if (ws->nsam == 0)
  {
    *pclowclip  = 0;
    *pchighclip = 0;
    *dc_offset  = 0;
    *amp        = 0;
    *pc5 = 0;
    *pc95 = 0;
    return;
  }

  if (ws->nsam > OVERFLOW_MASK) *overflow = 1;

  *pclowclip  = (int)(((float)ws->lowclip  * 10000.0) / (float)ws->nsam);
  *pchighclip = (int)(((float)ws->highclip * 10000.0) / (float)ws->nsam);

  mean = ((float)ws->sum + (float)ws->sum2 * OVERFLOW_MASK) / ws->nsam;

  *dc_offset = (int) mean;
  sqr_devn = (((float)ws->sumsqu + (float)ws->sumsqu2 * OVERFLOW_MASK)
              / (float)ws->nsam) - (mean * mean);
  *amp = integer_square_root((int)sqr_devn);

  /* now analyse the histogram */

  num = 0;
  for (ii = 0; ii < MAXHISTBITS; ii++)
  {
    num += ws->bithist[ii];
  }

  ntot = num;
  npc = ntot / 20; /* 5% cutoff */

  for (ii = num = 0; (ii < MAXHISTBITS) && (num < npc); ii++)
  {
    num += ws->bithist[ii];
  }

  *pc5 = ii;

  npc = (int)(0.95 * ntot);  /* 95% cutoff */

  for (ii = num = 0; (ii < MAXHISTBITS) && (num < npc); ii++)
  {
    num += ws->bithist[ii];
  }

  *pc95 = ii;
  return;
}

void acc_wave_stats(wave_info* wave)
/*
**  Updates the wave statistics
*/
{
  int ii;
  int val;
  samdata hclip;
  samdata lclip;
  wave_stats *ws;
  int sumabs;
  int num;

  ASSERT(wave);

  ws = &wave->stats;
  hclip = ws->highclip_level;
  lclip = ws->lowclip_level;

  if (ws->nsam > OVERFLOW_MASK) return;
  /* as soon as we have at least 1073741824 */
  /* samples, stop accumulating.   */

  sumabs = 0;
  num = 0;

  for (ii = 0; ii < wave->num_samples; ii++)
  {
    val = (int) wave->income[ii];
    ws->sum += val;
    ws->sumsqu += val * val;
    if (ws->sumsqu > OVERFLOW_MASK)
    {
      ws->sumsqu -= OVERFLOW_MASK;
      ws->sumsqu2++;
    }
    /* nasty bit here as ANSI C does not do >32bit */
    /* Assumes that samples are no larger than */
    /* signed shorts    */

    ws->nsam++;

    if (val >= hclip) ws->highclip++;
    if (val <= lclip) ws->lowclip++;

    sumabs += abs(val);
    num++;
  }

  if (ws->sum >= OVERFLOW_MASK)
  {
    ws->sum -= OVERFLOW_MASK;
    ws->sum2++;
  }
  else if (ws->sum < -OVERFLOW_MASK)
  {
    ws->sum += OVERFLOW_MASK;
    ws->sum2--;
  }
  /* another >32bit workaround  */
  /* assumes wave->num_samples < 32878 */
  /* this is really overkill as we expect */
  /* the mean to be around zero anyway */

  if (num > 0) sumabs /= num;
  ii = 0;
  while (sumabs)
  {
    sumabs >>= 1;
    ii++;
  }

  ASSERT(ii <= 16); /* unusual case i=16 if all samples -32678 */
  ws->bithist[ii]++;
  return;
}
