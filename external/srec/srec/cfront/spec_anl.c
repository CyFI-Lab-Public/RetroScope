/*---------------------------------------------------------------------------*
 *  spec_anl.c  *
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
#ifndef _RTT
#include <stdio.h>
#endif
#include <string.h>
#include <math.h>
#include <limits.h>
#include <assert.h>

#include "hmm_desc.h"
#include "front.h"
#include "pendian.h"
#include "portable.h"
#include "LCHAR.h"

#include "../clib/memmove.h"

#define DEBUG           0

#include "sh_down.h"

static int sort_ints_unique(int *list, int *num);
//static void mask_fft_taps(fftdata *data, int num, front_freq *freqobj);

void peakpick(front_freq *freqobj, fftdata *density, int num_freq);
void magsq(fftdata *x, fftdata *y, fftdata *z, int ns);

void preemph(fftdata *data, int window_len, samdata *wav_data,
             int num_samples, coefdata pre_mel,
             bigdata *last_sample);
void filtbank(front_freq *freqobj, fftdata *density, cepdata *fbo);



void filterbank_emulation(front_channel * channel, front_wave *waveobj,
                          front_freq *freqobj, front_cep *cepobj, samdata *income,
                          samdata *outgo, int num_samples)
{
  /*  Part II. Mel cepstrum coefficients
  **
  **  Maintain parameter queue */
  MEMMOVE(channel->cep + (channel->mel_dim + 1), channel->cep,
          (Q2 - 1) *(channel->mel_dim + 1), sizeof(cepdata));
  channel->shift = 0;

  /*  2.01 Pre-emphasize waveform
  Only the new samples are preemphasized.  To carry on from the previous call,
  the last sample value is stored in lastx.
  */
  preemph(channel->prebuff, freqobj->window_length, income, num_samples,
          waveobj->pre_mel, &channel->lastx);

#if DEBUG
  log_report("preemphasized data\n");
  write_scaled_frames(freqobj->window_length, 1, channel->prebuff, D_FIXED, (float) 1 / (0x01 << WAVE_SHIFT));
#endif
  /******************************************************************************
  **  The "NEW" fft performs shifting operations in fixed point, to maximise
  **  precision.
  **
  *******************************************************************************/
  channel->shift += place_sample_data(&freqobj->fft, channel->prebuff,
                                      freqobj->ham, freqobj->window_length);
#if DEBUG
  log_report("windowed data\n");
  if (channel->shift >= 0)
  {
    write_scaled_frames(freqobj->fft.size, 1, freqobj->fft.real, D_FIXED, (float)(0x01 << channel->shift));
    write_scaled_frames(freqobj->fft.size, 1, freqobj->fft.imag, D_FIXED, (float)(0x01 << channel->shift));
  }
  else
  {
    write_scaled_frames(freqobj->fft.size, 1, freqobj->fft.real, D_FIXED, (float)1 / (0x01 << -channel->shift));
    write_scaled_frames(freqobj->fft.size, 1, freqobj->fft.imag, D_FIXED, (float)1 / (0x01 << -channel->shift));
  }
#endif
  channel->shift *= 2;
  channel->shift += fft_perform_and_magsq(&freqobj->fft);

#if DEBUG
  log_report("After magnitude squared (%d)\n", channel->frame_count);
  if (channel->shift >= 0)
    write_scaled_frames(freqobj->fft.size, 1, freqobj->fft.real, D_FIXED, (float)(0x01 << (channel->shift)));
  else
    write_scaled_frames(freqobj->fft.size, 1, freqobj->fft.real, D_FIXED, (float)1 / (0x01 << (- channel->shift)));
#endif

#if DEBUG
  log_report("After magnitude squared: ");
  if (channel->shift >= 0)
    write_scaled_frames(freqobj->fft.size, 1, (void *)freqobj->fft.real, D_FIXED, (float)(0x01 <<  channel->shift));
  else
    write_scaled_frames(freqobj->fft.size, 1, (void *)freqobj->fft.real, D_FIXED, (float)1 / (0x01 <<  -channel->shift));
#endif

  if (freqobj->do_nonlinear_filter)
    peakpick(freqobj, freqobj->fft.real, freqobj->fft.size + 1);

#if DEBUG
  log_report("After peakpick: ");
  if (channel->shift >= 0)
    write_scaled_frames(freqobj->fft.size + 1, 1, (void *)freqobj->fft.real, D_FIXED, (float)(0x01 << channel->shift));
  else
    write_scaled_frames(freqobj->fft.size + 1, 1, (void *)freqobj->fft.real, D_FIXED, (float)1 / (0x01 << -channel->shift));
#endif

  /*  2.23 Apply filterbank emulation */
  channel->shift += RAMP_SHIFT;
  filtbank(freqobj, freqobj->fft.real, channel->filterbank);
#if DEBUG
  log_report("After filterbanked: ");
  if (channel->shift >= 0)
    write_scaled_frames(freqobj->nf, 1, channel->filterbank, D_FIXED, (float)(0x01 << channel->shift));
  else
    write_scaled_frames(freqobj->nf, 1, channel->filterbank, D_FIXED, (float)1 / (0x01 << -channel->shift));
#endif

  return;
}


void preemph(fftdata *data, int window_len, samdata *wav_data,
             int num_samples, coefdata pre_mel,
             bigdata *last_sample)
/*
**  pre-emphasize on speech data, check for end of data */
/*  SCALE: In this stage we're introducing a scale factor of 2 */
{
  int i;
  bigdata temp;

  ASSERT(data);
  ASSERT(last_sample);
  ASSERT(wav_data);
  ASSERT(num_samples >= 0);
  if (num_samples > window_len)
    num_samples = window_len;

  if (num_samples < window_len)
    MEMMOVE(data, data + num_samples, (window_len - num_samples),
            sizeof(fftdata));
  data += window_len - num_samples;

  /*  If no preemphasis to do
  */
  if (pre_mel == 0)
  { /* dont't shift */
    for (i = 0; i < num_samples; i++)
      data[i] = (fftdata) wav_data[i];
    return;
  }

  /*  Otherwise do the preemphasis
  */
  for (i = 0; i < num_samples; i++)
  {
    temp = SHIFT_UP((bigdata)wav_data[i], COEFDATA_SHIFT);
    data[i] = (fftdata)(SHIFT_DOWN(temp - (*last_sample), COEFDATA_SHIFT));
    *last_sample = (bigdata)pre_mel * wav_data[i];

  }
  return;
}

void magsq(fftdata *x, fftdata *y, fftdata *z, int ns)
/*
**  magnitude squared, tailored for TI FFT routines
**  The dynamic range should fit 32 - RAMP_SHIFT */
{
  int i;

  ASSERT((float)x[0] *(float)x[0] < LONG_MAX);
  ASSERT((float)x[0] *(float)x[0] > LONG_MIN);
  z[0] = (fftdata)((bigdata)x[0] * (bigdata)x[0]);
  for (i = 1; i < ns; i++)
  {
    ASSERT(((fftdata)x[i] *(fftdata)x[i]) >= 0);
    ASSERT(((fftdata)y[i] *(fftdata)y[i]) >= 0);
    ASSERT((float)x[i] *(float)x[i] < LONG_MAX);
    ASSERT((float)x[i] *(float)x[i] > LONG_MIN);
    ASSERT((float)y[i] *(float)y[i] < LONG_MAX);
    ASSERT((float)y[i] *(float)y[i] > LONG_MIN);
    /*    z[i]= (fftdata) SHIFT_DOWN ((bigdata)x[i] * (bigdata)x[i] + (bigdata)y[i] * (bigdata)y[i], RAMP_SHIFT);
    */
    z[i] = (fftdata)(((bigdata)x[i] * (bigdata)x[i])
                     + ((bigdata)y[i] * (bigdata)y[i]));
    if (z[i] <= 0)
      z[i] = (fftdata) 1;
  }
  return;
}

void peakpick(front_freq *freqobj, fftdata *density, int num_freq)
{
  int i;
  fftdata peak;
  fftdata bdecay;
  fftdata fdecay;
  int first;
  int last;

  ASSERT(freqobj);
  /* Fixed pt requires scale up of COEFDATA_SHIFT on these pars (coefdata) */
  bdecay = freqobj->peakpickdown;
  fdecay = freqobj->peakpickup;

  if ((bdecay <= (fftdata) 0.0) && (fdecay <= (fftdata) 0.0))
    return;

  first = freqobj->cut_off_below;
  last  = freqobj->cut_off_above;
  /* this filters from cut_off_below to       */
  /* cut_off_above inclusive          */

  if (last >= num_freq)
    last = num_freq - 1;
  /* as most routines seem to check both      */
  /* limits                           */

  if (bdecay > 0.0)
  {
    ASSERT(density[last] >= 0);
    peak = density[last];
    for (i = last - 1; i >= first; i--)
    {
      peak = (fftdata)(SHIFT_DOWN((bigdata)peak, COEFDATA_SHIFT) * (bigdata)bdecay);
      ASSERT(peak >= 0);
      if (density[i] > peak)
        peak = density[i];
      else
        density[i] = peak;
    }
  }
  if (fdecay > 0.0)
  {
    peak = density[first];
    for (i = first + 1; i <= last; i++)
    {
      peak = (fftdata)(SHIFT_DOWN((bigdata)peak, COEFDATA_SHIFT) * (bigdata)fdecay);
      if (density[i] > peak)
        peak = density[i];
      else
        density[i] = peak;
    }
  }
  return;
}

void filtbank(front_freq *freqobj, fftdata *density, cepdata *fbo)
/*
**  pwr spect -> filter bank output (linear) */
{
  int i, j, k;
  bigdata t, sum, mom, nxt;

  /*  Scale down before starting mel-filterbank operations
  */
  for (i = 0; i < freqobj->cut_off_above; i++)
    density[i] = SHIFT_DOWN(density[i], RAMP_SHIFT);

  j = MAX(freqobj->fcmid[0], freqobj->cut_off_below);
  nxt = 0;
  for (; j < freqobj->fcmid[1]; j++)
  {
    ASSERT(((float)nxt + (float)freqobj->framp[j] *(float)density[j]) < LONG_MAX);
    ASSERT(((float)nxt + (float)freqobj->framp[j] *(float)density[j]) > -LONG_MAX);
    nxt += (bigdata) SHIFT_DOWN((bigdata)freqobj->framp[j] * (bigdata)density[j], RAMP_SHIFT);
  }
  for (i = 0, k = 2; i < freqobj->nf; i++, k++)
  {
    sum = mom = 0;
    for (; j < freqobj->fcmid[k]; j++)
    {
      /* TODO: Tidy up this fixed pt shifting. BP */

      ASSERT((float) freqobj->framp[j] *(float) density[j] < LONG_MAX);
      ASSERT((float) freqobj->framp[j] *(float) density[j] > LONG_MIN);
      ASSERT((float) sum + (float)density[j] < LONG_MAX);
      ASSERT((float) sum + (float)density[j] > LONG_MIN);
      sum += (bigdata) density[j];
      ASSERT((float) mom + (float) freqobj->framp[j] *(float) density[j] < LONG_MAX);
      ASSERT((float) mom + (float) freqobj->framp[j] *(float) density[j] > LONG_MIN);

      mom += (bigdata)(long) SHIFT_DOWN((bigdata)freqobj->framp[j] * (bigdata)density[j], RAMP_SHIFT);
    }

    ASSERT(((float)nxt + (float)sum - (float)mom) < LONG_MAX);
    ASSERT(((float)nxt + (float)sum - (float)mom) > LONG_MIN);

    /* TODO: refine this expression. Shift down fcscl in advance.  */
    t = (bigdata)((SHIFT_UP(nxt + sum - mom, HALF_RAMP_SHIFT)
                   + SHIFT_DOWN(freqobj->fcscl[i+1], HALF_RAMP_SHIFT + 1))
                  / SHIFT_DOWN(freqobj->fcscl[i+1], HALF_RAMP_SHIFT));
    /* TODO: cleanup and also check for division by zero */
    nxt = mom;
    fbo[i] = (cepdata) t;
  }
  return;
}

int create_spectrum_filter(front_freq *freqobj, int *freq, int *spread)
{
  int ii, jj, freq_step;
  int lo, hi;
  ASSERT(freqobj);
  ASSERT(freqobj->spectrum_filter_num == 0);
  ASSERT(freqobj->samplerate > 0);
  /* Convert to FFT taps. Mark adjacent taps as well as taps within spread */
  freq_step = (freqobj->samplerate << 12) / (2 * freqobj->fft.size);
  freqobj->spectrum_filter = (int *) CALLOC_CLR(freqobj->fft.size + 1, sizeof(int), "cfront.spectrum_filter");
  freqobj->spectrum_filter_num = 0;
  for (ii = 0 ; ii < MAX_FILTER_NUM; ii++)
  {
    if (freq[ii] == 0)
      continue;
    lo = (((freq[ii] - spread[ii]) * 2 * freqobj->fft.size) + freqobj->samplerate / 2) / freqobj->samplerate;
    hi = (((freq[ii] + spread[ii]) * 2 * freqobj->fft.size) + freqobj->samplerate / 2) / freqobj->samplerate;


    for (jj = lo; jj <= hi;jj++)
    {
      if (freqobj->spectrum_filter_num >= (int) freqobj->fft.size)
        SERVICE_ERROR(MAX_FILTER_POINTS_EXCEEDED);
      freqobj->spectrum_filter[freqobj->spectrum_filter_num++] = jj;
    }
    /* jj=0;
     while (((jj+1)*freq_step)>>12 <= freq[ii]-spread[ii])
         jj++;
     while (((jj-1)*freq_step>>12) < freq[ii]+spread[ii]){
         if (freqobj->spectrum_filter_num >= (int) freqobj->fft.size)
      SERVICE_ERROR (MAX_FILTER_POINTS_EXCEEDED);
         freqobj->spectrum_filter[freqobj->spectrum_filter_num++]= jj;
         jj++;
     }
    */
  }
  sort_ints_unique(freqobj->spectrum_filter, &freqobj->spectrum_filter_num);
  return (freqobj->spectrum_filter_num);
}

void clear_spectrum_filter(front_freq *freqobj)
{
  ASSERT(freqobj->spectrum_filter);
  if (freqobj->spectrum_filter)
    FREE((char *) freqobj->spectrum_filter);
  freqobj->spectrum_filter = NULL;
  freqobj->spectrum_filter_num = 0;
  return;
}

static int sort_ints_unique(int *list, int *num)
{
  /*  Sort a list of ints and make unique */
  int ii, jj, temp;
  for (ii = 1; ii < *num; ii++)
  {
    for (jj = 0; jj < ii; jj++)
    {
      temp = list[ii];
      if (temp < list[jj])
      {
        MEMMOVE(&list[jj+1], &list[jj], (ii - jj), sizeof(int));
        list[jj] = temp;
        break;
      }
      if (temp == list[jj])
      {
        MEMMOVE(&list[ii], &list[ii+1], (*num - ii), sizeof(int));



        (*num)--;
      }
    }
  }
  return *num;
}

//static void mask_fft_taps(fftdata *data, int num, front_freq *freqobj)
//{
//  for (int i = 0; i < freqobj->spectrum_filter_num; ++i)
//  {
//    ASSERT(freqobj->spectrum_filter[i] < num);
//    data[freqobj->spectrum_filter[i]] = 0;
//  }
//}

/* --------------------------------------------------
 freq_warp will do pure linear warping if the warp
 scale > 1.0. Otherwise it will do piecewise warp
 which means warping the second part, from xstart
 to the bandwidth with another scale which is
 determined by b and c in the formulation.
 In general, 0.7 < wscale < 1.4, and xstart <= 1
 08/15/01, Puming Zhan
 --------------------------------------------------- */
void freq_warp(front_freq *freqobj, fftdata *inbuf, int ns)
{
  int i;
  int nsE;
  float x1, y1, b, c, wscale;
  fftdata *tmpbuf;

  ASSERT(freqobj && inbuf);

  ASSERT(freqobj->warp_scale != 0);

  wscale = freqobj->warp_scale;
  x1     = freqobj->piecewise_start;
  tmpbuf = (fftdata *) CALLOC(ns, sizeof(fftdata), "cfront.tmpbuf");

  if (wscale < MIN_WARP_SCALE || wscale > MAX_WARP_SCALE)
  {
    SERVICE_ERROR(WARP_SCALE);
  }
  if (x1 > 1.0 || x1 < 0.5)
  {
    SERVICE_ERROR(PIECEWISE_START);
  }

  y1 = x1 < wscale ? (float)x1 / wscale : (float)1.0;

  b = y1 < 1.0 ? (float)((1.0 - x1) / (1.0 - y1)) : (float)0.0;

  c = (float)((1.0 - b) * (ns - 1));

  nsE = (int)(y1 * (ns - 1));

  for (i = 0; i < ns; i++)
  {
    float x = i > nsE ? b * i + c : wscale * i;
    int   u = (int)ceil((double)x);
    int   l = (int)floor((double)x);
    float w1 = x - l;
    float w2 = 1 - w1;

    if (u < ns)
    {
      tmpbuf[i] = (int)(w1 * inbuf[u] + w2 * inbuf[l]);
    }
    else
    {
      tmpbuf[i] = inbuf[ns-1];
    }
  }

  /* need to copy the warped fft into inbuf    */
  /* because the following function filtbank() */
  /* will take inbuf as input                  */
  /* considering that this function will be    */
  /* for every frame, it may not be a good idea*/
  /* to do malloc here                         */

  for (i = 0; i < ns; i++)
    inbuf[i] = tmpbuf[i];

  FREE((char *) tmpbuf);
}
