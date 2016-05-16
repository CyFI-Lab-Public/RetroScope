/*---------------------------------------------------------------------------*
 *  chelfep.c  *
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
#include <string.h>


#ifdef unix
#include <unistd.h>
#endif
#include <assert.h>

#ifndef _RTT
#include "duk_io.h"
#endif
#include "voicing.h"
#include "front.h"
#include "portable.h"


#include "../clib/memmove.h"
#include "sh_down.h"

#define SMOOTH_C0_FOR_VOICING 1

#if SMOOTH_C0_FOR_VOICING
static featdata smoothed_c0(front_cep *cepobj, front_channel *channel);
#endif

int make_frame(front_channel *channel, front_wave *waveobj,
               front_freq *freqobj, front_cep *cepobj,
               voicing_info *voice,
               samdata *inFramesWorth, samdata *refFramesWorth,
               int num_samples,
               featdata *framdata, featdata *voicedata)
{
#if SMOOTH_C0_FOR_VOICING
  featdata smooth_c0;
#endif
  if (freqobj->do_filterbank_input)
    /* This memmove is in filterbank_emulation  */
    MEMMOVE(channel->cep + (channel->mel_dim + 1), channel->cep,
            (Q2 - 1) *(channel->mel_dim + 1), sizeof(float));
  else
  {
		/* not fb input */

    /*  JFR data processing is no longer supported. BP 21-Jul98. */

    /* 2. CEP data processing */
    filterbank_emulation(channel, waveobj, freqobj, cepobj,
                         inFramesWorth, refFramesWorth, num_samples);
    /* if doing fb dump then skip frame making. A top level will
        read channel->fbo and dump it. */
    if (freqobj->do_filterbank_dump)
      return True;
  }
  cepstrum_params(channel, waveobj, freqobj, cepobj);

  /* 4. Delta CEP data processing */
  (void) make_std_frame(channel,  cepobj, framdata);
  if (!channel->frame_valid)
    return (channel->frame_valid);


  /* 5. Voicing analysis */
  if (channel->frame_valid)
  {
    if (voice != NULL)
    {
#if SMOOTH_C0_FOR_VOICING
      if (cepobj->do_smooth_c0)
      {
        smooth_c0 = smoothed_c0(cepobj, channel);
        *voicedata = (featdata) voicing_analysis(voice, smooth_c0, NULL);
      }
      else
        *voicedata = (featdata) voicing_analysis(voice, framdata[0], NULL);

#else
      *voicedata = (featdata) voicing_analysis(voice, framdata[0], NULL);
#endif
    }
    if (cepobj->do_skip_even_frames)
      channel->frame_valid = (channel->frame_count) % 2;
  }
  return (channel->frame_valid);
}

static featdata smoothed_c0(front_cep *cepobj, front_channel *channel)
{
  cepdata  val;
  featdata fval;
  bigdata  scval;

  if (channel->frame_count <= 1)
    val = channel->cep[DELTA * (channel->mel_dim+1)];
  else
  {
    val = (channel->cep[(DELTA-1) * (channel->mel_dim+1)] >> 2)
          + (channel->cep[DELTA * (channel->mel_dim+1)] >> 1)
          + (channel->cep[(DELTA+1) * (channel->mel_dim+1)] >> 2);
  }

  /*  Now scaling and byteranging
  */
  /* Now take the costable scaling off the ceps. */
  ASSERT((cepobj->melA_scale[0] *(float)SHIFT_DOWN(val, COSINE_TABLE_SHIFT))
         < LONG_MAX);
  ASSERT((cepobj->melA_scale[0] *(float)SHIFT_DOWN(val, COSINE_TABLE_SHIFT))
         > -LONG_MAX);

  scval = (bigdata)(SHIFT_DOWN((bigdata)cepobj->melA_scale[0]
                               * (bigdata) SHIFT_DOWN(val, COSINE_TABLE_SHIFT)
                               + (bigdata)cepobj->melB_scale[0], BYTERANGE_SHIFT + LOG_SCALE_SHIFT));
  fval = (featdata) MAKEBYTE(scval);
  return (fval);
}

