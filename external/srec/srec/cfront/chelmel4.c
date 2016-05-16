/*---------------------------------------------------------------------------*
 *  chelmel4.c  *
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
#include "portable.h"

#define DEBUG           0
#define LOG_AS_PRINT 0

#define LPCMAX 100


#if LOG_AS_PRINT /* BP temp hack */
#define log_report printf
#endif

#include "sh_down.h"


/* cepstrum_params has been broken into three functions:
 filterbank_emulation    - does preemp, window, fft and filtbank
 gain_adjustment                 - estimates gain
 cepstrum_params                 - does gain adj.(if on), spec corr and cos transform
   This enables us to bypass gain adjustment.
*/

//static void mel_cuberoot_offset(cepdata *fbo, cepdata *ch_off, int nf);
#if SPEC_CORRECT
static void mel_spectrum_correction(cepdata *fbo, cepdata *ch_gain, int nf);
#endif
static void mel_loglookup_with_offset(front_cep *cepobj,
                                      front_channel *channel);
//static void mel_exp(cepdata *fbo, int nf);
//static void durbin(cepdata *a, cepdata *r, int n);
//static void lpc_to_cepstral_recursion(cepdata *c, cepdata *a, int nc, int n);
static void icostrans(cepdata *cs, cepdata fb[], cepdata cep[], int nf, int nc);


void cepstrum_params(front_channel *channel, front_wave *waveobj,
                     front_freq *freqobj, front_cep *cepobj)
{
#if SPEC_CORRECT
  /*  2.30 Apply a spectrum correction */
  if (cepobj->mel_loop)
    mel_spectrum_correction(freqobj->filterbank, cepobj->mel_loop, channel->num_freq);
#endif
  /*  2.33 Calculate log dB energy values */
    mel_loglookup_with_offset(cepobj, channel);
#if DEBUG
  log_report("Filterbank output: ");
  write_scaled_frames(freqobj->nf, 1, channel->filterbank, D_FIXED, 1 / (float)LOG_SCALE);
#endif

  /*  2.34 Cosine transformation */
  icostrans(cepobj->cs, channel->filterbank, channel->cep,
            channel->num_freq, cepobj->mel_dim);

#if DEBUG
  log_report("Cepstrum coefficients: ");
  write_scaled_frames((cepobj->mel_dim + 1), 1, channel->cep, D_FIXED, (float)1 / (0x01 << (LOG_SCALE_SHIFT + COSINE_TABLE_SHIFT)));
#endif
  return;
}

static void icostrans(cepdata *cs, cepdata fb[], cepdata cep[], int nf, int nc)
/*
**  inv rotated-cosine transform
**  ref Davis and Mermelstein, ASSP 1980 */
{
  int   i, j;
  cepdata *cp;
  cepdata temp;

  for (i = 0; i <= nc; i++)
  {
    cp = &cs[i*nf];
    for (j = 0, temp = 0; j < nf; j++)
      temp += fb[j] * cp[j];
    cep[i] = temp;
  }
  return;
}

#if SPEC_CORRECT
static void mel_spectrum_correction(cepdata *fbo, cepdata *ch_gain, int nf)
/*
**  pwr spect -> filter bank output */
{
  int i;

  for (i = 0;i < nf; i++)
    fbo[i] = fbo[i] * ch_gain[i]; /* TODO: Fixedpt scale up and down */
  return;
}
#endif

static void mel_loglookup_with_offset(front_cep *cepobj,
                                      front_channel *channel)
/*
**  pwr spect -> filter bank output */
{
  int ii;

  if (channel->shift > 0)
    for (ii = 0; ii < channel->num_freq; ii++)
    {
      channel->filterbank[ii] = (cepdata) log_lookup(&cepobj->logtab,
                                (int)(channel->filterbank[ii] +
                                      SHIFT_DOWN(cepobj->mel_offset[ii], channel->shift)),
                                channel->shift);
    }
  else
    for (ii = 0; ii < channel->num_freq; ii++)
    {
      channel->filterbank[ii] = (cepdata) log_lookup(&cepobj->logtab,
                                (int)(channel->filterbank[ii] +
                                      SHIFT_UP(cepobj->mel_offset[ii], -channel->shift)),
                                channel->shift);
    }

  return;
}

//static void mel_exp(cepdata *fbo, int nf)
///*
//**  pwr spect -> filter bank output */
//{
//  int i;
//  for (i = 0; i < nf; i++)
//  {
//    fbo[i] = (cepdata) exp((double) fbo[i]);
//  }
//  return;
//}
//
//static void durbin(
//  cepdata *a,   /* lpc coefficients           */
//  cepdata *r,   /* autocorrelation coefficients       */
//  int n)     /* order of lpc analysis        */
//{
//  int i, j;
//  cepdata A[LPCMAX+1][LPCMAX+1], sum;
//  cepdata k[LPCMAX+1];
//  cepdata e[LPCMAX+1];
//
//  e[0] = r[0];
//  for (i = 1; i <= n; i++)
//  {
//    sum = 0;
//    for (j = 1; j <= (i - 1); j++)
//    {
//      sum += A[j][i-1] * r[i-j];
//    }
//    k[i] = -(r[i] + sum) / e[i-1];
//    A[i][i] = k[i] ;
//    for (j = 1; j <= (i - 1); j++)
//    {
//      A[j][i] = A[j][i-1] + k[i] * A[i-j][i-1];
//    }
//    e[i] = (1 - k[i] * k[i]) * e[i-1];
//  }
//  for (j = 1 ; j <= n; j++)
//  {
//    a[j] = A[j][n];
//  }
//
//  a[0] = (cepdata) 1.0 ;
//  return;
//}
//
//static void lpc_to_cepstral_recursion(
//  cepdata *c,     /* cepstral coefficients        */
//  cepdata *a,     /* lpc coeffiecients            */
//  int nc,         /* order of cepstra             */
//  int n)          /* order of lpc                 */
//{
//  int k, i;
//  cepdata sum;
//
//  ASSERT(nc < LPCMAX);
//
//  for (i = n + 1; i <= nc; i++)
//  {
//    a[i] = (cepdata) 0.0;
//  }
//  /* if lpc order less    */
//  /* than cepstral order  */
//  /* define higher lpccos */
//
//  for (i = 1; i <= nc; i++)
//  {
//    sum = (cepdata) 0.0;
//    for (k = 1; k <= (i - 1); k++)
//    {
//      sum = sum + k * c[k] * a[i-k]; /* TODO: fixedpt range for mult */
//    }
//    c[i] = -a[i] - (sum / i);               /* cepstral calculated  */
//    /* to <=nc in icostrans */
//    /* so I shall do the    */                                                      /* same here            */
//  }
//}

