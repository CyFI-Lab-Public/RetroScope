/*---------------------------------------------------------------------------*
 *  ca_wave.c  *
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

/************************************************************************
 * CA_Wave Methods
 ************************************************************************/

#ifndef _RTT
#include <stdio.h>
#endif
#include <stdlib.h>
#include <string.h>
#include <limits.h>

#ifdef unix
#include <unistd.h>
#endif
#include <assert.h>


#include "frontapi.h"
#include "portable.h"

/* CA_Wave functions. See simpars.c for frontend params */

CA_Wave *CA_AllocateWave(char type)
{
  CA_Wave *hWave = NULL;
  TRY_CA_EXCEPT
  hWave = (CA_Wave *) CALLOC_CLR(1, sizeof(CA_Wave), "cfront.hWave");
#ifndef _RTT
  hWave->data.device.file.typ = type;
#endif
  hWave->data.channel = create_channel_object();
  hWave->is_configured = False;
  hWave->is_configuredForVoicing = False;
  hWave->ca_rtti = CA_WAVE_SIGNATURE;
  return (hWave);

  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hWave);
}

void CA_ConfigureWave(CA_Wave *hWave, CA_Frontend *hFrontend)
{
  TRY_CA_EXCEPT
  ASSERT(hWave);
  ASSERT(hFrontend);
  ASSERT(FRAMERATE > 0);

  if (hFrontend->is_configured == False)
    SERVICE_ERROR(UNCONFIGURED_FRONTEND);
  if (hWave->is_configured == True)
    SERVICE_ERROR(CONFIGURED_WAVE);

  ASSERT(hFrontend->config->waveobj->samplerate);

  hWave->data.scale  = hFrontend->src_scale;
  hWave->data.offset = hFrontend->offset;
  setup_channel_object(hWave->data.channel, hFrontend->config->waveobj,
                       hFrontend->config->freqobj,
                       hFrontend->config->cepobj);

  hWave->data.samplerate = hFrontend->config->waveobj->samplerate;

  /* Buffer size is set to one frame's worth of data */
  create_sample_buffer(&hWave->data,
                       hFrontend->config->waveobj->samplerate / FRAMERATE,
                       hFrontend->config->freqobj->window_length);

  hWave->data.stats.highclip_level = hFrontend->config->waveobj->high_clip;
  hWave->data.stats.lowclip_level  = hFrontend->config->waveobj->low_clip;
  hWave->data.stats.max_per10000_clip =
    hFrontend->config->waveobj->max_per10000_clip;
  hWave->data.stats.max_dc_offset =
    hFrontend->config->waveobj->max_dc_offset;
  hWave->data.stats.high_noise_level_bit =
    hFrontend->config->waveobj->high_noise_level_bit;
  hWave->data.stats.low_speech_level_bit =
    hFrontend->config->waveobj->low_speech_level_bit;
  hWave->data.stats.min_samples =
    hFrontend->config->waveobj->min_samples;

  hWave->is_configured = True;
  return;

  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hWave);
}

void CA_ConfigureVoicingAnalysis(CA_Wave *hWave, CA_FrontendInputParams *hFrontPar)
{
  TRY_CA_EXCEPT

  hWave->voice.margin = hFrontPar->voice_margin;
  hWave->voice.fast_margin = hFrontPar->fast_voice_margin;
  hWave->voice.quiet_margin = hFrontPar->tracker_margin;
  hWave->voice.voice_duration = hFrontPar->voice_duration;
  hWave->voice.quiet_duration = hFrontPar->quiet_duration;
  hWave->is_configuredForVoicing = True;
  return;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hWave)
}

void CA_UnconfigureWave(CA_Wave *hWave)
{
  TRY_CA_EXCEPT
  ASSERT(hWave);
  if (hWave->is_configured == False)
    SERVICE_ERROR(UNCONFIGURED_WAVE);

  clear_channel_object(hWave->data.channel);
  free_sample_buffer(&hWave->data);
  hWave->data.samplerate = 0;

  hWave->is_configured = False;

/* The following is not correct in this place as it does not match where it is set to TRUE.
 * This is needed for the sample rate change function.
 * hWave->is_configuredForVoicing = False;
 */
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hWave);
}

void CA_FreeWave(CA_Wave *hWave)
{
  TRY_CA_EXCEPT
  ASSERT(hWave);

  if (hWave->is_configured == True)
    SERVICE_ERROR(CONFIGURED_WAVE);

  delete_channel_object(hWave->data.channel);
  FREE((char *) hWave);
  return;
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hWave);
}

int CA_OpenWaveFromDevice(CA_Wave *hWave,
                          int wave_type,
                          int samplerate,
                          int device_id,
                          int device_type)
{
  TRY_CA_EXCEPT
  ASSERT(hWave);
  ASSERT(device_id >= 0);
  ASSERT(device_type == WAVE_DEVICE_RAW);

  if (hWave->is_configured == False)
    SERVICE_ERROR(UNCONFIGURED_WAVE);
  if (hWave->is_configuredForVoicing == False)
    SERVICE_ERROR(UNCONFIGURED_WAVE);

  reset_channel_object(hWave->data.channel);
  hWave->data.wave_type = wave_type;
  hWave->data.device_type = device_type;
  hWave->data.device.ext.op = WAVE_DEVICE_INPUT;
  hWave->data.samplerate = samplerate;
  init_voicing_analysis(&hWave->voice);

  reset_sig_check(&hWave->data.stats);
  hWave->data.do_stats = ESR_TRUE;

  return (True);
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hWave);
}


ESR_BOOL CA_DoSignalCheck(CA_Wave *hWave,
                     ESR_BOOL *clipping,
                     ESR_BOOL *dcoffset,
                     ESR_BOOL *highnoise,
                     ESR_BOOL *quietspeech,
                     ESR_BOOL *too_few_samples,
                     ESR_BOOL *too_many_samples)
{
  TRY_CA_EXCEPT

  int nsam;
  int pclowclip;
  int pchighclip;
  int dc_offset;
  int amp;
  int pc5;
  int pc95;
  int overflow;
  ESR_BOOL error;
  wave_stats *ws;

  ASSERT(hWave);

  ws = &hWave->data.stats;
  get_sig_check(ws, &nsam, &pclowclip, &pchighclip, &dc_offset, &amp,
                &pc5, &pc95, &overflow);

  if ((pclowclip + pchighclip) > ws->max_per10000_clip)
    *clipping = ESR_TRUE;
  else     *clipping = ESR_FALSE;
  if (abs(dc_offset) > ws->max_dc_offset) *dcoffset = ESR_TRUE;
  else     *dcoffset = ESR_FALSE;
  if (pc5 >= ws->high_noise_level_bit) *highnoise = ESR_TRUE;
  else     *highnoise = ESR_FALSE;
  if (pc95 < ws->low_speech_level_bit) *quietspeech = ESR_TRUE;
  else     *quietspeech = ESR_FALSE;
  if (nsam < ws->min_samples)  *too_few_samples = ESR_TRUE;
  else     *too_few_samples = ESR_FALSE;
  if (overflow)    *too_many_samples = ESR_TRUE;
  else     *too_many_samples = ESR_FALSE;

  /* This is better than casting the logical expression to ESR_BOOL */
  if (*clipping || *dcoffset || *highnoise || *quietspeech || *too_few_samples || *too_many_samples)
  	error = ESR_TRUE;
  else
  	error = ESR_FALSE;

  return(error);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hWave)
}

void CA_CloseDevice(CA_Wave *hWave)
{
  TRY_CA_EXCEPT
  if (hWave->is_configured == False)
    SERVICE_ERROR(UNCONFIGURED_WAVE);

  ASSERT(hWave->data.device.ext.op == WAVE_DEVICE_INPUT);  /* because I don't have output yet! */

  return;
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hWave);
}


int CA_LoadSamples(CA_Wave *hWave,
                   samdata *pPCMData,
                   int sampleCount)
{
  ASSERT(hWave);
  ASSERT(pPCMData);

  TRY_CA_EXCEPT
  if (hWave->is_configured == False)
    SERVICE_ERROR(UNCONFIGURED_WAVE);
  if (hWave->data.device_type != WAVE_DEVICE_RAW)
    SERVICE_ERROR(BAD_WAV_DEVICE);
  if (hWave->data.window_size < sampleCount)
    SERVICE_ERROR(INCORRECT_SAMPLERATE);

  memcpy(hWave->data.income,
         pPCMData,
         sampleCount * sizeof(samdata));

  hWave->data.num_samples = sampleCount;

  if (hWave->data.do_stats)
    acc_wave_stats(&hWave->data);

  return (True);
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hWave);
}

void CA_ConditionSamples(CA_Wave *hWave)
{
  TRY_CA_EXCEPT
  int ii;

  if (hWave->is_configured == False)
    SERVICE_ERROR(UNCONFIGURED_WAVE);

  if (hWave->data.offset != 0)
    for (ii = 0; ii < hWave->data.num_samples; ii++)
      hWave->data.income[ii] = RANGE(hWave->data.income[ii] +
                                     hWave->data.offset, SHRT_MIN, SHRT_MAX);
  if (hWave->data.scale != 1.0)
    for (ii = 0; ii < hWave->data.num_samples; ii++)
      hWave->data.income[ii] = (short) RANGE(hWave->data.income[ii] *
                                             hWave->data.scale, SHRT_MIN, SHRT_MAX);
  return;
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hWave);
}
