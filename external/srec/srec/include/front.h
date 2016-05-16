/*---------------------------------------------------------------------------*
 *  front.h  *
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



#ifndef _h_front_
#define _h_front_

#include "all_defs.h"
#include "fronttyp.h"
#include "log_tabl.h"
#include "duk_err.h"
#include "voicing.h"
#include "specnorm.h"
#include "channorm.h"
#include "swicms.h"
#ifndef _RTT
#include "duk_io.h"
#endif
#include "fft.h"
#include "frontpar.h" /* Shared front end parameters structure. Native data types only. */


#define SPEC_SUB        0
#define SPEC_CORRECT    0
#define BIGGER_WINDOW   0
#define MIN_WARP_SCALE 0.5
#define MAX_WARP_SCALE 1.5

#define D_FIXED D_LONG



#define FRAMERATE       100
#define NUM_MEL_FREQS   30                      /* up to 3750 Hz. Now >5512 -BP */
#define DELTA           3
#define Q2              7
#define NP  1025
#define NF  40
#define NC  40
#define MEL_FREQ_ARRAY_SIZE 30


/* Spectral sub def moved from spec_sub.c BP */
#ifdef SET_RCSID
static const char spec_sub_h[] = "$Id: front.h,v 1.2.10.9 2007/08/31 17:44:53 dahan Exp $";
#endif

/**
 * @todo document
 */
typedef struct
{
  cepdata     *sub_vector;
  int      is_valid;
  unsigned int    frame_dur;
  cepdata     scale;
  unsigned int    count;
}
spectral_sub_info;


/**
 * Contains the data storage points associated with a channel.
 */
typedef struct
{
  size_t mel_dim;

  /* WAVE data */
  int buff_size;
  samdata *outbuff;          /* incoming samples buffer */
  samdata *refbuff;          /* outgoing samples buffer */
  fftdata *prebuff;          /* buffer for preemphasised data */
  fftdata *prerefbuff;       /* buffer for preemphasised data outgoing */
  int     forget_factor;
  norm_info *channorm;
  swicms_norm_info *swicms;
  spect_dist_info *spchchan[MAX_CHAN_DIM];

  /* FREQ data */
  int     shift;
  int     num_freq;
  cepdata *filterbank;
  cepdata *filterbankref;
  spectral_sub_info *spectral_sub;

  /* CEP data */
  int     frame_valid;          /* whether frame is valid */
  long    frame_count;          /* frame count */
  int     frame_delay;         /* ignore the first few frames */
  cepdata *cep;                 /* cepstrum coefs. of prev. frames */
  cepdata *rasta;
  featdata *framdata;
  bigdata lastx;
}
front_channel;



/*  This is where the front end objects are defined
     WAVE (front_wave)
     FREQ (front_freq)
     CEP  (front_cep)
*/

/**
 * @todo document
 */
typedef struct
{
  size_t samdim;
  int   samtyp;
  int   samplerate;
  coefdata    pre_mel;
  int   high_clip;
  int   low_clip;
  int   max_per10000_clip;
  int   max_dc_offset;
  int   high_noise_level_bit;
  int   low_speech_level_bit;
  int   min_samples;
}
front_wave;


/**
 * FREQ object.
 */
typedef struct
{
  int     window_length;
  int     samplerate;
  int     framerate;
  int     frame_period;                 /* the following 3 are private */
  ESR_BOOL    do_spectral_sub;
  int     do_nonlinear_filter;
  ESR_BOOL    do_filterbank_input;
  ESR_BOOL    do_filterbank_dump;
  float   warp_scale;                     /*## */
  float   piecewise_start;                     /*## */
  int     low_cut;
  int     high_cut;
  int     num_fb_to_use;
  int     *spectrum_filter;    /* List of FFT taps to filter */
  int     spectrum_filter_num;
  fftdata peakpickup;
  fftdata peakpickdown;
  int     cut_off_below, cut_off_above;
  int     np, ns, nf, lognp;
  fftdata fcb[NF];
  fftdata *fc;
  int     fcmid[NF+2];
  fftdata fcscl[NF+1], framp[NP+1];
  fftdata *ham;
  fft_info fft;
}
front_freq;


/**
 * CEP object.
 */
typedef struct
{
  ESR_BOOL    do_dd_mel;
  ESR_BOOL    do_rasta;
  int     do_scales;
  ESR_BOOL    do_plp;
  size_t  mel_dim;
  int     lpc_order;
  ESR_BOOL    do_skip_even_frames;
  ESR_BOOL    do_smooth_c0;
  int     spectral_sub_frame_dur;
  coefdata spec_sub_scale;
  int     forget_factor;           /* preserve % of previous hist */
  int     sv6_margin;
  cepdata *melA_scale;
  cepdata *melB_scale;
  cepdata *dmelA_scale;
  cepdata *dmelB_scale;
  cepdata *ddmelA_scale;
  cepdata *ddmelB_scale;
  cepdata *rastaA_scale;
  cepdata *rastaB_scale;
  cepdata *mel_offset;
  cepdata *mel_loop;
  cepdata *cs;
  log_table_info  logtab;
}
front_cep;


/**
 * @todo document
 */
typedef struct
{
  front_wave *waveobj;
  front_freq *freqobj;
  front_cep  *cepobj;
  /* Internal memberrs that may need to be configurable. Currently constants
  size_t mel_dim;
  */
}
front_config;


/*  Front end function declarations follow */


front_config *config_frontend(void);
int make_frame(front_channel *channel, front_wave *waveobj,
               front_freq *freqobj, front_cep *cepobj,
               voicing_info *voice,
               samdata *inFramesWorth, samdata *refFramesWorth,
               int num_samples,
               featdata *framdata, featdata *voicedata);
void standard_front_init(front_config *config, front_freq *freqobj);
void init_cepstrum_analysis(front_config *config, front_freq *freqobj);


void load_samples(front_channel *channel, int window_length,
                  samdata *incom, samdata *outgo, int nsam);
void filterbank_emulation(front_channel * channel, front_wave *waveobj,
                          front_freq *freqobj, front_cep *cepobj, samdata *income, samdata *outgo,
                          int num_samples);
void cepstrum_params(front_channel *channel, front_wave *waveobj,
                     front_freq *freqobj, front_cep *cepobj);
int make_std_frame(front_channel *channel, front_cep *cepobj,
                   featdata *hFrame);
int purge_std_frames(front_channel *channel, front_cep *cepobj,
                     featdata *hFrame, int frame);

void init_spectral_sub(front_config *config, front_freq *freqobj);
void close_spectral_sub(front_freq *freqobj);
void reset_spectral_sub(front_freq *freqobj);
void do_spectral_subtraction(cepdata *fbo, spectral_sub_info* spectral_sub,
                             int num_freqs);

int create_spectrum_filter(front_freq *freqobj, int *freq, int *spread);

void clear_spectrum_filter(front_freq *freqobj);

front_config *create_config_object(void) ;
void setup_config_object(front_config *config, front_parameters *parameters);
void clear_config_object(front_config *config);
void delete_config_object(front_config *config);


front_channel *create_channel_object(void) ;
void delete_channel_object(front_channel *channel);
void setup_channel_object(front_channel *channel, front_wave *waveobj,
                          front_freq *freqobj, front_cep *cepobj);
void clear_channel_object(front_channel *channel);
void reset_channel_object(front_channel *channel);

/*
** Fixed pont front end
**
** Function:      Scaling (in bit shifts)
**
** preemphasis   -1
** hamming-window    0
** fft     0
** magnitude   HFN
** filterbank    0
**
** log
** cosine    HFN-
** regression    0
** scaling
*/


#endif /* _h_front_ */
