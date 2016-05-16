/*---------------------------------------------------------------------------*
 *  frontobj.c  *
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
#if defined(__cplusplus) && defined(_MSC_VER)
extern "C"
{
#include <string.h>
}
#else
#include <string.h>
#endif

#ifndef _RTT
#include <stdio.h>
#endif
#ifdef unix
#include <unistd.h>
#endif
#ifndef POSIX
#include <memory.h>
#endif
#include <assert.h>

#include "front.h"

#include "portable.h"

#include "sh_down.h"

#define DEBUG       0


static void hamming_window(fftdata *ham, int win_len);

static front_wave *create_wave_object(void);
static void delete_wave_object(front_wave *waveobj);
static void setup_wave_object(front_wave *waveobj, front_parameters *parameters);
static void clear_wave_object(front_wave *waveobj);

static front_freq *create_freq_object(void);
static void delete_freq_object(front_freq *freqobj);
static void setup_freq_object(front_freq *freqobj, front_parameters *parameters, int mel_dim);
static void reset_freq_object(front_freq *freqobj);
static void clear_freq_object(front_freq *freqobj);

static front_cep *create_cep_object(void);
static void delete_cep_object(front_cep *cepobj);
static void setup_cep_object(front_cep *cepobj, front_parameters *parameters,
                             size_t num_fb, size_t mel_dim);
static void reset_cep_object(front_cep *cepobj);
static void clear_cep_object(front_cep *cepobj);



front_config *create_config_object(void)
{
  front_config  *config;
  config = (front_config *) CALLOC_CLR(1,
           sizeof(front_config), "cfront.front_config");
  return config;
}


/*******************************************************************************
**  FUNCTION: setup_config_object
**
**  DESCRIPTION: Set up the front end using the paramteters. This function
**       configures the member Wave, Freq and Cep objects, by calling their
**       create and setup
**       functions.
**
**  ARGUMENTS:
**
**  RETURNS: pointer to config object
**
*******************************************************************************/

void setup_config_object(front_config *config, front_parameters *parameters)
{

  ASSERT(config);
  ASSERT(parameters);

  /* Create and configure sub-components */
  config->waveobj = create_wave_object();
  config->freqobj = create_freq_object();
  config->cepobj = create_cep_object();

  setup_wave_object(config->waveobj, parameters);
  setup_freq_object(config->freqobj, parameters, parameters->mel_dim);
  setup_cep_object(config->cepobj, parameters, config->freqobj->nf,
                   parameters->mel_dim);
  return;
}

void clear_config_object(front_config *config)
{

  ASSERT(config);

  clear_wave_object(config->waveobj);
  clear_freq_object(config->freqobj);
  clear_cep_object(config->cepobj);

  delete_wave_object(config->waveobj);
  config->waveobj = NULL;
  delete_freq_object(config->freqobj);
  config->freqobj = NULL;
  delete_cep_object(config->cepobj);
  config->cepobj = NULL;
  return;
}


void delete_config_object(front_config *config)
{
  ASSERT(config);
  FREE((char *) config);
  return;
}


front_channel *create_channel_object(void)
{
  front_channel *channel;

  channel = (front_channel *) CALLOC_CLR(1,
            sizeof(front_channel), "cfront.channel");
  return channel;
}


void delete_channel_object(front_channel *channel)
{
  ASSERT(channel);
  FREE((char *) channel);
}


void setup_channel_object(
  front_channel *channel, front_wave *waveobj,
  front_freq *freqobj, front_cep *cepobj)
{
  ASSERT(channel);
  ASSERT(waveobj);
  ASSERT(freqobj);
  ASSERT(cepobj);

  channel->prebuff = (fftdata *) CALLOC(freqobj->window_length + 1,
                     sizeof(fftdata), "cfront.prebuff");
  channel->prerefbuff = (fftdata *) CALLOC(freqobj->window_length + 1,
                        sizeof(fftdata), "cfront.prerefbuff");
  channel->buff_size = freqobj->window_length + 1;

  /* Create gain normalization object, and space for filter bank storage. BP */
  channel->num_freq = freqobj->nf;
  channel->filterbank = (cepdata *) CALLOC(channel->num_freq,
                        sizeof(cepdata), "cfront.filterbank");
  channel->filterbankref = (cepdata *) CALLOC(channel->num_freq,
                           sizeof(cepdata), "cfront.filterbankref");

  channel->mel_dim = cepobj->mel_dim;
  channel->cep = (cepdata *) CALLOC((Q2 + 1) * (channel->mel_dim + 1),
                                          sizeof(cepdata), "cfront.cep");
  channel->rasta = (cepdata *) CALLOC((channel->mel_dim + 1),
                   sizeof(cepdata), "cfront.rasta");
  channel->framdata = (featdata *) CALLOC(3 * (channel->mel_dim + 1),
                      sizeof(featdata), "cfront.chan_framdata");

  if (freqobj->do_spectral_sub)
  {
    /*  Spectral subtraction requires estimate of BG levels. This is currently
        estimated on the first config->spectral_sub_frame_dur frames, which are
        assumed to be in silence. The channel means are estimated when the
        frame count is reached, and is_valid is set, in
        estimate_spectral_sub_means. Spectral subtraction is turned on with
        config->do_spectral_sub.
    */

    channel->spectral_sub = (spectral_sub_info *) CALLOC_CLR(1,
                            sizeof(spectral_sub_info), "cfront.spectral_sub_info");
    channel->spectral_sub->sub_vector = (cepdata *) CALLOC(NUM_MEL_FREQS,
                                        sizeof(cepdata), "cfront.spectral_sub_vector");
    channel->spectral_sub->frame_dur = cepobj->spectral_sub_frame_dur;
    channel->spectral_sub->scale = cepobj->spec_sub_scale;
  }

  ASSERT(freqobj->frame_period > 0);
  channel->frame_delay = DELTA + (freqobj->window_length / freqobj->frame_period) - 1;
  channel->forget_factor = cepobj->forget_factor;
  reset_channel_object(channel);
  return;
}


void clear_channel_object(front_channel *channel)
{
  ASSERT(channel);
  FREE((char *) channel->prebuff);
  channel->prebuff = NULL;
  FREE((char *) channel->prerefbuff);
  channel->prerefbuff = NULL;
  FREE((char *) channel->filterbank);
  channel->filterbank = NULL;
  FREE((char *) channel->filterbankref);
  channel->filterbankref = NULL;
  FREE((char *) channel->cep);
  channel->cep = NULL;
  FREE((char *) channel->rasta);
  channel->rasta = NULL;
  FREE((char *) channel->framdata);
  channel->framdata = NULL;
  if (channel->spectral_sub)
  {
    FREE((char *) channel->spectral_sub->sub_vector);
    FREE((char *) channel->spectral_sub);
    channel->spectral_sub = NULL;
  }
  channel->buff_size = 0;
  return;
}


/* Replacement fns for reset_std_channel */
void reset_channel_object(front_channel *channel)
{
  size_t ii;

  ASSERT(channel);

#if DEBUG
  log_report("Channel reset\n");
#endif
  memset(channel->cep, 0x00, Q2 *(channel->mel_dim + 1) * sizeof(float));
  memset(channel->prebuff, 0x00, channel->buff_size * sizeof(fftdata));
  memset(channel->prerefbuff, 0x00, channel->buff_size * sizeof(fftdata));
  channel->lastx = 0;

  for (ii = 0; ii <= channel->mel_dim; ii++)
    channel->rasta[ii] = 0;

  if (channel->spectral_sub)
  {
    channel->spectral_sub->is_valid = False;
    for (ii = 0; ii < NUM_MEL_FREQS; ii++)
      channel->spectral_sub->sub_vector[ii] = (cepdata) 0.0;
  }
  channel->frame_count = 0;

  return;
}


/******************************************************************************
**  WAVE OBJECT
*******************************************************************************/

static front_wave *create_wave_object(void)
{
  front_wave *waveobj;

  waveobj = (front_wave *) CALLOC_CLR(1, sizeof(front_wave), "cfront.waveobj");
  return waveobj;
}



static void delete_wave_object(front_wave *waveobj)
{
  ASSERT(waveobj);
  FREE((char *) waveobj);
  return;
}


static void setup_wave_object(front_wave *waveobj, front_parameters *parameters)
{
  ASSERT(waveobj);
  ASSERT(parameters);

  waveobj->samplerate = parameters->samplerate;
  /*  Be careful about the value of COEFDATA_SHIFT - it should be <16.
      During preemphasis the data is shifted up by COEFDATA_SHIFT too.
  */
  waveobj->pre_mel = (coefdata) fixed_point_convert(parameters->pre_mel,
                     COEFDATA_SHIFT);
  waveobj->high_clip   = parameters->high_clip;
  waveobj->low_clip    = parameters->low_clip;
  waveobj->max_per10000_clip  = parameters->max_per10000_clip;
  waveobj->max_dc_offset  = parameters->max_dc_offset;
  waveobj->high_noise_level_bit = parameters->high_noise_level_bit;
  waveobj->low_speech_level_bit = parameters->low_speech_level_bit;
  waveobj->min_samples  = parameters->min_samples;

  return;
}


static void clear_wave_object(front_wave *waveobj)
{
  ASSERT(waveobj);
  return;
}

/******************************************************************************
**  FREQUENCY OBJECT
*******************************************************************************/
static front_freq *create_freq_object(void)
{
  front_freq *freqobj;
  freqobj = (front_freq *) CALLOC_CLR(1,
            sizeof(front_freq), "cfront.freqobj");
  freqobj->fc = &freqobj->fcb[1];
  freqobj->lognp = 4;
  reset_freq_object(freqobj);
  return freqobj;
}


static void delete_freq_object(front_freq *freqobj)
{
  ASSERT(freqobj);
  FREE((char *) freqobj);
  return;
}


static void setup_freq_object(front_freq *freqobj,
                              front_parameters *parameters, int mel_dim)
{
  int     fmax, i, j, high_cut, bandwidth;
  float   t, finc, f;

  ASSERT(freqobj);
  ASSERT(parameters);
  ASSERT(FRAMERATE > 0);
  ASSERT(parameters->samplerate);

  freqobj->framerate = FRAMERATE;
  freqobj->frame_period = parameters->samplerate / freqobj->framerate;
  freqobj->samplerate = parameters->samplerate;
  freqobj->window_length = (int)(parameters->window_factor * freqobj->frame_period);
  freqobj->low_cut = parameters->low_cut;
  freqobj->high_cut = parameters->high_cut;
  freqobj->do_spectral_sub = parameters->do_spectral_sub;
  freqobj->do_filterbank_input = parameters->do_filterbank_input;
  freqobj->do_filterbank_dump = parameters->do_filterbank_dump;
  freqobj->num_fb_to_use = parameters->num_fb_to_use;
  freqobj->do_nonlinear_filter = True;    /* on by default */
  freqobj->spectrum_filter_num = 0;
  freqobj->warp_scale = parameters->warp_scale; /*## */
  freqobj->piecewise_start = parameters->piecewise_start; /*## */

  if (freqobj->high_cut > 0)
    high_cut = freqobj->high_cut;
  else
    high_cut = parameters->samplerate / 2;

  bandwidth = parameters->samplerate / 2;
  ASSERT(bandwidth != 0);

  freqobj->np = 1 << freqobj->lognp;                            /* fft sizing */
  while (freqobj->np < freqobj->window_length)
    freqobj->np *= 2, freqobj->lognp++;
  while (freqobj->np < 128)
    freqobj->np *= 2, freqobj->lognp++;
  if (freqobj->np > NP)
    SERVICE_ERROR(FFT_TOO_SMALL);

  /*  Change the values of the peakpick forward and backward coefficients
  to compensate for sample rate. */
  t = (float) exp(log((double)parameters->peakpickup)
                  * ((double)parameters->samplerate / (double)11025)
                  / ((double)freqobj->np / (double)256));
  freqobj->peakpickup = (fftdata) fixed_point_convert(t, COEFDATA_SHIFT);
  t = (float) exp(log((double)parameters->peakpickdown)
                  * ((double)parameters->samplerate / (double)11025)
                  / ((double)freqobj->np / (double)256));
  freqobj->peakpickdown = (fftdata) fixed_point_convert(t, COEFDATA_SHIFT);

#if BIGGER_WINDOW
  freqobj->window_length = freqobj->np;
#endif
  configure_fft(&freqobj->fft, freqobj->np);
  freqobj->ns = freqobj->np / 2 + 1;
  fmax = bandwidth;
  finc = (float)parameters->samplerate / (float)freqobj->np;
  /*    finc= fmax/freqobj->ns; */
  freqobj->cut_off_below = (int)(((long)freqobj->low_cut * freqobj->np) / (2.0 * bandwidth));
  freqobj->cut_off_above = (int)(((long)high_cut * freqobj->np) / (2.0 * bandwidth));

  freqobj->fc[-1] = (fftdata) freqobj->low_cut;                        /* 1st channel at cutoff */
  i = ((int)freqobj->low_cut + 50) / 100 + 1;              /* other channels at x00Hz */
  for (freqobj->nf = 0; i <= 10; i++, freqobj->nf++)
    freqobj->fc[freqobj->nf] = (fftdata)(100 * i);          /* 100 Hz */
  for (f = 1000.; f <= high_cut; freqobj->nf++)
  {
    f *= (float)1.1; /* 10% */
    freqobj->fc[freqobj->nf] = (fftdata) fixed_round(f);        /* 10 % */
  }
  freqobj->nf--;

  if ((freqobj->fc[freqobj->nf] + freqobj->fc[freqobj->nf-1]) / 2. > high_cut)
    freqobj->nf--;
  freqobj->fc[freqobj->nf] = (fftdata) high_cut;
  freqobj->fc[freqobj->nf+1] = (fftdata) high_cut;

#if DEBUG
  write_frames(freqobj->nf + 1, 1, freqobj->fc, D_FLOAT);
#endif

  for (i = 0; i <= freqobj->cut_off_below; i++)
    freqobj->framp[i] = 0;
  freqobj->fcscl[0] = 0;
  freqobj->fcmid[0] = freqobj->cut_off_below;
  f = (freqobj->cut_off_below + 1) * finc;
  for (i = 0, j = freqobj->cut_off_below + 1; i <= freqobj->nf; i++)
  {
    freqobj->fcscl[i+1] = (fftdata) 0.0;
    for (; f < freqobj->fc[i] && f < (float) high_cut; f += finc, j++)
    {
      t = (f - freqobj->fc[i-1]) / (freqobj->fc[i] - freqobj->fc[i-1]);
      freqobj->framp[j] = (fftdata) fixed_point_convert(t, RAMP_SHIFT); /* scale it up by 12 bits, (16 - 4) */
      freqobj->fcscl[i] += (fftdata) SHIFT_UP(1, RAMP_SHIFT) - freqobj->framp[j];    /* scale it up by 12 bits as well () */
      freqobj->fcscl[i+1] += freqobj->framp[j];       /* scale it up by 12 bits as well () */
    }
    if (j > freqobj->cut_off_above)
      freqobj->fcmid[i+1] = freqobj->cut_off_above;
    else
      freqobj->fcmid[i+1] = j;
  }
  /*  Put in a check to validate the range of fcscl[] for fixed point f/e
  */
#if DEBUG
  write_frames(freqobj->nf, 1, freqobj->fcmid, D_LONG);
  write_frames(freqobj->nf, 1, freqobj->fcscl, D_LONG);
  write_frames(freqobj->ns, 1, freqobj->framp, D_LONG);
#endif

  if (mel_dim > freqobj->nf)
    SERVICE_ERROR(BAD_COSINE_TRANSFORM);

  if (freqobj->nf > NF)
    SERVICE_ERROR(BAD_COSINE_TRANSFORM);

  /*  Weighting function construction */
  freqobj->ham = (fftdata *) CALLOC(freqobj->window_length + 1,
                                          sizeof(fftdata), "cfront.ham");
  hamming_window(freqobj->ham, freqobj->window_length);

  /*  Sine tables for FFT */

#if DEBUG
#define log_report printf
  log_report("fc\n");
  write_scaled_frames(freqobj->nf + 1, 1, freqobj->fc, D_FIXED, 1);

  log_report("framp\n");
  write_scaled_frames(freqobj->ns, 1, freqobj->framp, D_FIXED, 1);
#endif

  create_spectrum_filter(freqobj, parameters->spectrum_filter_freq,
                         parameters->spectrum_filter_spread);
  if (freqobj->spectrum_filter_num == 0)
    clear_spectrum_filter(freqobj);
  return;
}

static void hamming_window(fftdata *ham, int win_len)
/*
**  add Hamming window on speech data */
{
  int     i;
  double  f;
  double  coef;

  f = (2 * M_PI) / win_len;
  for (i = 0; i <= win_len; i++)
  {
    coef = 0.54 - 0.46 * cos(f * (double)i);
    ham[i] = (fftdata) fixed_point_convert((float)coef,
                                           HALF_FFTDATA_SIZE - 1);
  }
  return;
}

static void clear_freq_object(front_freq *freqobj)
{
  ASSERT(freqobj);
  if (freqobj->ham)
  {
    FREE((char *)freqobj->ham);
    freqobj->ham = NULL;
  }
  unconfigure_fft(&freqobj->fft);

  if (freqobj->spectrum_filter_num > 0)
    clear_spectrum_filter(freqobj);
  return;
}

static void reset_freq_object(front_freq *freqobj)
{
  ASSERT(freqobj);
  return;
}


/******************************************************************************
**  CEPSTRAL OBJECT
*******************************************************************************/
static front_cep *create_cep_object(void)
{
  front_cep *cepobj;
  cepobj = (front_cep *) CALLOC_CLR(1, sizeof(front_cep), "cfront.cepobj");
  return cepobj;
}


static void delete_cep_object(front_cep *cepobj)
{
  ASSERT(cepobj);
  FREE((char *) cepobj);
  return;
}

static void setup_cep_object(front_cep *cepobj, front_parameters *parameters,
                             size_t num_fb, size_t mel_dim)
{
  float f;
  size_t i, j;

  ASSERT(cepobj);
  ASSERT(parameters);
  cepobj->mel_dim = mel_dim;
  cepobj->do_dd_mel = parameters->do_dd_mel;
  cepobj->do_skip_even_frames = parameters->do_skip_even_frames;
  cepobj->do_smooth_c0 = parameters->do_smooth_c0;
  cepobj->sv6_margin = parameters->sv6_margin;
  cepobj->forget_factor = parameters->forget_factor;
  cepobj->spectral_sub_frame_dur = parameters->spectral_sub_frame_dur;
  cepobj->spec_sub_scale = (coefdata) fixed_point_convert(
                             parameters->spec_sub_scale, COEFDATA_SHIFT);
  cepobj->lpc_order  = parameters->lpc_order;


  cepobj->mel_offset = (cepdata *) CALLOC(MEL_FREQ_ARRAY_SIZE,
                       sizeof(cepdata), "cfront.mel_offset");
  cepobj->mel_loop = (cepdata *) CALLOC(MEL_FREQ_ARRAY_SIZE,
                     sizeof(cepdata), "cfront.mel_loop");
  cepobj->melA_scale = (cepdata *) CALLOC(cepobj->mel_dim + 1,
                       sizeof(cepdata), "cfront.melA_scale");
  cepobj->dmelA_scale = (cepdata *) CALLOC(cepobj->mel_dim + 1,
                        sizeof(cepdata), "cfront.dmelA_scale");
  cepobj->ddmelA_scale = (cepdata *) CALLOC(cepobj->mel_dim + 1,
                         sizeof(cepdata), "cfront.ddmelA_scale");
  cepobj->melB_scale = (cepdata *) CALLOC(cepobj->mel_dim + 1,
                       sizeof(cepdata), "cfront.melB_scale");
  cepobj->dmelB_scale = (cepdata *) CALLOC(cepobj->mel_dim + 1,
                        sizeof(cepdata), "cfront.dmelB_scale");
  cepobj->ddmelB_scale = (cepdata *) CALLOC(cepobj->mel_dim + 1,
                         sizeof(cepdata), "cfront.ddmelB_scale");
  cepobj->rastaA_scale = (cepdata *) CALLOC(cepobj->mel_dim + 1,
                         sizeof(cepdata), "cfront.rastaA_scale");
  cepobj->rastaB_scale = (cepdata *) CALLOC(cepobj->mel_dim + 1,
                         sizeof(cepdata), "cfront.rastaB_scale");

  cepobj->do_scales = True; /* Hack so default scalings are loaded */

  for (i = 0; i < MEL_FREQ_ARRAY_SIZE; ++i)
  {
    cepobj->mel_offset[i] = (cepdata) parameters->mel_offset[i];
    cepobj->mel_loop[i] = (cepdata) parameters->mel_loop[i];
  }

  for (i = 0; i <= cepobj->mel_dim; ++i)
  {
    cepobj->melA_scale[i] = (cepdata) parameters->melA_scale[i];
    cepobj->dmelA_scale[i] = (cepdata) parameters->dmelA_scale[i];
    cepobj->ddmelA_scale[i] = (cepdata) parameters->ddmelA_scale[i];
    cepobj->melB_scale[i] = (cepdata) parameters->melB_scale[i];
    cepobj->dmelB_scale[i] = (cepdata) parameters->dmelB_scale[i];
    cepobj->ddmelB_scale[i] = (cepdata) parameters->ddmelB_scale[i];
    cepobj->rastaA_scale[i] = (cepdata) parameters->rastaA_scale[i];
    cepobj->rastaB_scale[i] = (cepdata) parameters->rastaB_scale[i];
    cepobj->melA_scale[i] = (cepdata) fixed_point_convert((float) parameters->melA_scale[i],
                            BYTERANGE_SHIFT);
    cepobj->dmelA_scale[i] = (cepdata) fixed_point_convert((float) parameters->dmelA_scale[i],
                             BYTERANGE_SHIFT);
    cepobj->ddmelA_scale[i] = (cepdata) fixed_point_convert((float) parameters->ddmelA_scale[i],
                              BYTERANGE_SHIFT);
    cepobj->melB_scale[i] = (cepdata) fixed_point_convert((float) parameters->melB_scale[i],
                            BYTERANGE_SHIFT + LOG_SCALE_SHIFT);
    cepobj->dmelB_scale[i] = (cepdata) fixed_point_convert((float) parameters->dmelB_scale[i],
                             BYTERANGE_SHIFT + LOG_SCALE_SHIFT);
    cepobj->ddmelB_scale[i] = (cepdata) fixed_point_convert((float) parameters->ddmelB_scale[i],
                              BYTERANGE_SHIFT + LOG_SCALE_SHIFT);
    cepobj->rastaA_scale[i] = (cepdata) fixed_point_convert((float) parameters->rastaA_scale[i],
                              BYTERANGE_SHIFT);
    cepobj->rastaB_scale[i] = (cepdata) fixed_point_convert((float) parameters->rastaB_scale[i],
                              BYTERANGE_SHIFT + LOG_SCALE_SHIFT);
  }

  /* Now allocate space for the cosine matrix. Previously fixed. */
  f = (float)(M_PI / num_fb);
  cepobj->cs = (cepdata *) CALLOC(num_fb * num_fb, sizeof(cepdata), "cfront.cosine_matrix");
  for (i = 0; i < num_fb; ++i)
  {
    for (j = 0; j < num_fb; ++j) /* TODO: fixedpt cosine matrix */
      cepobj->cs[i*(num_fb)+j] = (cepdata) fixed_point_convert(
                                   (float)(cos((double)(i * (j + .5) * f)) / num_fb),
                                   COSINE_TABLE_SHIFT); /* balanced after icostrans */
  }
  create_lookup_log(&cepobj->logtab, 12);   /* TODO: rename 12 as macro */
  reset_cep_object(cepobj);
  return;
}


static void reset_cep_object(front_cep *cepobj)
{
  ASSERT(cepobj);
  return;
}


static void clear_cep_object(front_cep *cepobj)
{
  ASSERT(cepobj);
  if (cepobj->melA_scale)
    FREE((char*)cepobj->melA_scale);
  cepobj->melA_scale = NULL; /* Set to null in either case, for simplicity */
  if (cepobj->dmelA_scale)
    FREE((char*)cepobj->dmelA_scale);
  cepobj->dmelA_scale = NULL;
  if (cepobj->ddmelA_scale)
    FREE((char*)cepobj->ddmelA_scale);
  cepobj->ddmelA_scale = NULL;
  if (cepobj->melB_scale)
    FREE((char*)cepobj->melB_scale);
  cepobj->melB_scale = NULL;
  if (cepobj->dmelB_scale)
    FREE((char*)cepobj->dmelB_scale);
  cepobj->dmelB_scale = NULL;
  if (cepobj->ddmelB_scale)
    FREE((char*)cepobj->ddmelB_scale);
  cepobj->ddmelB_scale = NULL;
  if (cepobj->rastaA_scale)
    FREE((char*)cepobj->rastaA_scale);
  cepobj->rastaA_scale = NULL;
  if (cepobj->rastaB_scale)
    FREE((char*)cepobj->rastaB_scale);
  cepobj->rastaB_scale = NULL;
  if (cepobj->cs)
    FREE((char *) cepobj->cs);
  cepobj->cs = NULL;
  if (cepobj->mel_offset)
    FREE((char*)cepobj->mel_offset);
  cepobj->mel_offset = NULL;
  if (cepobj->mel_loop)
    FREE((char*)cepobj->mel_loop);
  cepobj->mel_loop = NULL;
  destroy_lookup_log(&cepobj->logtab);
  return;
}
