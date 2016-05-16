/*---------------------------------------------------------------------------*
 *  voicing.c  *
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
#include <string.h>
#include <math.h>
#include <assert.h>

#include <limits.h>
#ifndef _RTT
#include <stdio.h>
#endif

#include "all_defs.h"
#include "voicing.h"

#include "portable.h"

#include "../cfront/sh_down.h"

#define DEBUG        0


static const char voicing[] = "$Id: voicing.c,v 1.1.10.5 2007/10/15 18:06:24 dahan Exp $";


void init_voicing_analysis(voicing_info *chan)
{
  chan->count = -1;
  chan->sil_count = 0;
  chan->speech_count = 0;
  chan->fast_count = 0;
#if DEBUG
  log_report("U: 255 255 255 -1 -1 -1 -1\n");
#endif
  return;
}

long voicing_analysis(voicing_info *chan, voicedata enval , int* log)
{
  long  retval;
  int   threshold;

  if (chan->count < 0)
  {
    chan->b1 = SHIFT_UP(enval, 8);
    chan->b0 = SHIFT_UP(enval, 8);
    chan->count = -1;
  }

  /*  background level
  */
  if (chan->b0 > SHIFT_UP(enval, 8))
  {
    chan->b0 = SHIFT_UP(enval, 8);
    chan->count = 0;
  }
  if (chan->count > B0_HANG2)
    chan->b0 += B0_RATE2;
  else if (chan->count > B0_HANG1)
    chan->b0 += B0_RATE1;

  chan->count++;

  /*  the second background level
  */
  if ((enval - chan->quiet_margin) < (chan->b0 >> 8))
    chan->b1 += SHIFT_DOWN(B1_RATE * (SHIFT_UP(enval, 8) - chan->b1), 8);

  /*  speech level
  */
  if (chan->s0 < SHIFT_UP(enval, 8))
    chan->s0 = SHIFT_UP(enval, 8);
  else
    chan->s0 -= B0_RATE1;

  /*  increase the range by 25% */
  threshold = (chan->b1 + (SHIFT_DOWN(
                             MAX(chan->s0 - chan->b0 - DYNAMIC_RANGE, 0), 2))) >> 8;

  /*  Is it speech?
  */
  if (enval > (threshold + chan->margin))
    chan->speech_count++;
  else
    chan->speech_count = 0;

  /*  Is it Fast-match speech
  */
  if (enval > (threshold + chan->fast_margin))
    chan->fast_count++;
  else
    chan->fast_count = 0;

  if (enval <= (threshold + chan->quiet_margin))
    chan->sil_count++;
  else
    chan->sil_count = 0;

  /*******************
   * Returning flags *
   *******************/

  retval = 0L;

  if (chan->fast_count > chan->voice_duration)
    retval = FAST_VOICE_BIT;
  else if (chan->sil_count > chan->quiet_duration)
    retval = QUIET_BIT;

  if (chan->speech_count > chan->voice_duration)
    retval |= VOICE_BIT;

  if (chan->sil_count > 0)
    retval |= BELOW_THRESHOLD_BIT;

  chan->voice_status = retval;
#if DEBUG
  log_report("U: %d %.1f %.1f, %d %d %d %d\n", (int) enval,
             chan->b0 / 256.0, chan->b1 / 256.0,
             chan->speech_count, chan->fast_count,
             chan->sil_count, chan->count);
#endif
  return (retval);
}
