/*---------------------------------------------------------------------------*
 *  RecognizerImpl.c  *
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


#include "ESR_Session.h"
#include "ESR_SessionTypeImpl.h"
#include "IntArrayList.h"
#include "LCHAR.h"
#include "passert.h"
#include "plog.h"
#include "pstdio.h"
#include "pmemory.h"
#include "ptimestamp.h"
#include "SR_AcousticModelsImpl.h"
#include "SR_AcousticStateImpl.h"
#include "SR_GrammarImpl.h"
#include "SR_SemprocDefinitions.h"
#include "SR_SemanticResult.h"
#include "SR_SemanticResultImpl.h"
#include "SR_Recognizer.h"
#include "SR_RecognizerImpl.h"
#include "SR_RecognizerResultImpl.h"
#include "SR_SemanticResultImpl.h"
#include "SR_EventLog.h"
#include "srec.h"

#define MTAG NULL
#define FILTER_NBEST_BY_SEM_RESULT 1
#define AUDIO_CIRC_BUFFER_SIZE 20000
#define SEMPROC_ACTIVE 1
#define SAMPLE_SIZE (16 / CHAR_BIT) /* 16-bits / sample */

/* milliseconds per FRAME = 1/FRAMERATE * 1000 */
/* We multiple by 2 because we skip even frames */
#define MSEC_PER_FRAME (2000/FRAMERATE)
#define MAX_ENTRY_LENGTH 512
#define PREFIX_WORD     "-pau-"
#define PREFIX_WORD_LEN 5
#define SUFFIX_WORD     "-pau2-"
#define SUFFIX_WORD_LEN 6


static ESR_ReturnCode SR_Recognizer_Reset_Buffers ( SR_RecognizerImpl *impl );

/**
 * Initializes recognizer properties to default values.
 *
 * Replaces setup_recognition_parameters()
 */
ESR_ReturnCode SR_RecognizerToSessionImpl()
{
  ESR_ReturnCode rc;

  /* Old comment: remember to keep "ca_rip.h" up to date with these parameters... */

  /* CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.max_acoustic_models", 2)); */
  CHKLOG(rc, ESR_SessionSetBoolIfEmpty("CREC.Recognizer.partial_results", ESR_FALSE));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.NBest", 1));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.eou_threshold", 100));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.max_altword_tokens", 400));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.max_frames", 1000));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.max_fsm_arcs", 3000));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.max_fsm_nodes", 3000));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.max_fsmnode_tokens", 1000));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.max_hmm_tokens", 1000));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.max_model_states", 1000));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.max_searches", 2));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.max_word_tokens", 1000));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.non_terminal_timeout", 50));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.num_wordends_per_frame", 10));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.often", 10));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.optional_terminal_timeout", 30));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.reject", 500));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.terminal_timeout", 10));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.viterbi_prune_thresh", 5000));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Recognizer.wordpen", 0));

  CHKLOG(rc, ESR_SessionSetSize_tIfEmpty("SREC.Recognizer.utterance_timeout", 400));

  CHKLOG(rc, ESR_SessionSetBoolIfEmpty("enableGetWaveform", ESR_FALSE));

  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

/**
 * Initializes frontend properties to default values.
 *
 * Replaces load_up_parameter_list()
 */
ESR_ReturnCode SR_RecognizerFrontendToSessionImpl()
{
  IntArrayList* intList = NULL;
  ESR_ReturnCode rc;
  ESR_BOOL exists;
  size_t i;

  /* Old comment: Remember to keep "ca_pip.h" up to date with these parameters... */

  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.mel_dim", 12));
  CHKLOG(rc, ESR_SessionSetSize_tIfEmpty("CREC.Frontend.samplerate", 8000));
  CHKLOG(rc, ESR_SessionSetFloatIfEmpty("CREC.Frontend.premel", 0.98f));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.lowcut", 260));  /* Hz */
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.highcut", 4000)); /* Hz */
  CHKLOG(rc, ESR_SessionSetFloatIfEmpty("CREC.Frontend.window_factor", 2.0)); /* times the frame size */
  CHKLOG(rc, ESR_SessionSetBoolIfEmpty("CREC.Frontend.do_skip_even_frames", ESR_FALSE)); /* 10/20 ms rate */
  CHKLOG(rc, ESR_SessionSetFloatIfEmpty("CREC.Frontend.offset", 0)); /* additional */
  CHKLOG(rc, ESR_SessionSetBoolIfEmpty("CREC.Frontend.ddmel", ESR_FALSE)); /* delta-delta mel pars */
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.forgetfactor", 40));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.sv6_margin", 10));
  CHKLOG(rc, ESR_SessionSetBoolIfEmpty("CREC.Frontend.rasta", ESR_FALSE));
  CHKLOG(rc, ESR_SessionSetBoolIfEmpty("CREC.Frontend.rastac0", ESR_FALSE));
  CHKLOG(rc, ESR_SessionSetBoolIfEmpty("CREC.Frontend.spectral_subtraction", ESR_FALSE));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.spec_sub_dur", 0));
  CHKLOG(rc, ESR_SessionSetFloatIfEmpty("CREC.Frontend.spec_sub_scale", 1.0));
  CHKLOG(rc, ESR_SessionSetBoolIfEmpty("CREC.Frontend.do_filterbank_dump", ESR_FALSE)); /* Output is filterbank (30 floats) */
  CHKLOG(rc, ESR_SessionSetBoolIfEmpty("CREC.Frontend.do_filterbank_input", ESR_FALSE)); /* Input is filterbank (30 floats) in place of audio samples */
  CHKLOG(rc, ESR_SessionSetBoolIfEmpty("CREC.Frontend.do_smooth_c0", ESR_TRUE));
  CHKLOG(rc, ESR_SessionSetBoolIfEmpty("CREC.Frontend.plp", ESR_FALSE)); /* Do PLP instead of MEL */
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.lpcorder", 12)); /* order of lpc analysis in plp processing */
  CHKLOG(rc, ESR_SessionSetFloatIfEmpty("CREC.Frontend.warp_scale", 1.0));
  CHKLOG(rc, ESR_SessionSetFloatIfEmpty("CREC.Frontend.piecewise_start", 1.0));
  CHKLOG(rc, ESR_SessionSetFloatIfEmpty("CREC.Frontend.peakdecayup", -1.0)); /* If +ve, decay factor on peakpicker (low to high) */
  CHKLOG(rc, ESR_SessionSetFloatIfEmpty("CREC.Frontend.peakdecaydown", -1.0)); /* If +ve, decay factor on peakpicker (high to low) */
  CHKLOG(rc, ESR_SessionSetBoolIfEmpty("CREC.Frontend.cuberoot", ESR_FALSE)); /* Use cube root instead of log */

  CHKLOG(rc, ESR_SessionContains("CREC.Frontend.mel_offset", &exists));
  if (!exists)
  {
    CHKLOG(rc, IntArrayListCreate(&intList));
    for (i = 0; i < 32; ++i)
      CHKLOG(rc, IntArrayListAdd(intList, 0));
    CHKLOG(rc, ESR_SessionSetProperty("CREC.Frontend.mel_offset", intList, TYPES_INTARRAYLIST));
    intList = NULL;
  }

  CHKLOG(rc, ESR_SessionContains("CREC.Frontend.mel_loop", &exists));
  if (!exists)
  {
    CHKLOG(rc, IntArrayListCreate(&intList));
    for (i = 0; i < 32; ++i)
      CHKLOG(rc, IntArrayListAdd(intList, 1));
    CHKLOG(rc, ESR_SessionSetProperty("CREC.Frontend.mel_loop", intList, TYPES_INTARRAYLIST));
    intList = NULL;
  }

  CHKLOG(rc, ESR_SessionContains("CREC.Frontend.melA", &exists));
  if (!exists)
  {
    CHKLOG(rc, IntArrayListCreate(&intList));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 13.2911));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 47.2229));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 79.2485));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 92.1967));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 136.3855));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 152.2896));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 183.3601));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 197.4200));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 217.8278));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 225.6556));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 263.3073));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 277.193));
    CHKLOG(rc, ESR_SessionSetProperty("CREC.Frontend.melA", intList, TYPES_INTARRAYLIST));
    intList = NULL;
  }

  CHKLOG(rc, ESR_SessionContains("CREC.Frontend.melB", &exists));
  if (!exists)
  {
    CHKLOG(rc, IntArrayListCreate(&intList));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 37.0847));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 91.3289));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 113.9995));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 123.0336));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 131.2704));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 128.9942));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 120.5267));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 132.0079));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 129.8076));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 126.5029));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 121.8519));
    CHKLOG(rc, ESR_SessionSetProperty("CREC.Frontend.melB", intList, TYPES_INTARRAYLIST));
    intList = NULL;
  }

  CHKLOG(rc, ESR_SessionContains("CREC.Frontend.dmelA", &exists));
  if (!exists)
  {
    CHKLOG(rc, IntArrayListCreate(&intList));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 91.6305));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 358.3790));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 527.5946));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 536.3163));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 731.2385));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 757.8382));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 939.4460));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 1028.4136));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 1071.3193));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 1183.7922));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 1303.1014));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 1447.7766));
    CHKLOG(rc, ESR_SessionSetProperty("CREC.Frontend.dmelA", intList, TYPES_INTARRAYLIST));
    intList = NULL;
  }

  CHKLOG(rc, ESR_SessionContains("CREC.Frontend.dmelB", &exists));
  if (!exists)
  {
    CHKLOG(rc, IntArrayListCreate(&intList));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.4785));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.3878));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.4029));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.3182));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.3706));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.5394));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.5150));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.4270));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.4871));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.4088));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.4361));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.5449));
    CHKLOG(rc, ESR_SessionSetProperty("CREC.Frontend.dmelB", intList, TYPES_INTARRAYLIST));
    intList = NULL;
  }

  CHKLOG(rc, ESR_SessionContains("CREC.Frontend.ddmelA", &exists));
  if (!exists)
  {
    CHKLOG(rc, IntArrayListCreate(&intList));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 10.7381));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 32.6775));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 46.2301));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 51.5438));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 57.6636));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 57.0581));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 65.3696));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 70.1910));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 71.6751));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 78.2364));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 83.2440));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 89.6261));
    CHKLOG(rc, ESR_SessionSetProperty("CREC.Frontend.ddmelA", intList, TYPES_INTARRAYLIST));
    intList = NULL;
  }

  CHKLOG(rc, ESR_SessionContains("CREC.Frontend.ddmelB", &exists));
  if (!exists)
  {
    CHKLOG(rc, IntArrayListCreate(&intList));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.5274));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.5098));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.5333));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.5963));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.5132));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.5282));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.5530));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.5682));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.4662));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.4342));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.5235));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.4061));
    CHKLOG(rc, ESR_SessionSetProperty("CREC.Frontend.ddmelB", intList, TYPES_INTARRAYLIST));
    intList = NULL;
  }

  CHKLOG(rc, ESR_SessionContains("CREC.Frontend.rastaA", &exists));
  if (!exists)
  {
    CHKLOG(rc, IntArrayListCreate(&intList));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 7.80));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 37.0));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 54.0));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 57.0));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 84.0));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 86.5));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 98.1));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 127.0));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 153.0));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 160.0));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 188.0));
    CHKLOG(rc, IntArrayListAdd(intList, (int) 199.0));
    CHKLOG(rc, ESR_SessionSetProperty("CREC.Frontend.rastaA", intList, TYPES_INTARRAYLIST));
    intList = NULL;
  }

  CHKLOG(rc, ESR_SessionContains("CREC.Frontend.rastaB", &exists));
  if (!exists)
  {
    CHKLOG(rc, IntArrayListCreate(&intList));
    CHKLOG(rc, IntArrayListAdd(intList, 117));
    CHKLOG(rc, IntArrayListAdd(intList, 121));
    CHKLOG(rc, IntArrayListAdd(intList, 114));
    CHKLOG(rc, IntArrayListAdd(intList, 111));
    CHKLOG(rc, IntArrayListAdd(intList, 113));
    CHKLOG(rc, IntArrayListAdd(intList, 126));
    CHKLOG(rc, IntArrayListAdd(intList, 134));
    CHKLOG(rc, IntArrayListAdd(intList, 130));
    CHKLOG(rc, IntArrayListAdd(intList, 135));
    CHKLOG(rc, IntArrayListAdd(intList, 129));
    CHKLOG(rc, IntArrayListAdd(intList, 139));
    CHKLOG(rc, IntArrayListAdd(intList, 138));
    CHKLOG(rc, ESR_SessionSetProperty("CREC.Frontend.rastaB", intList, TYPES_INTARRAYLIST));
    intList = NULL;
  }

  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.speech_detect", 18));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.speech_above", 18));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.ambient_within", 12));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.start_windback", 50));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.utterance_allowance", 40));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.speech_duration", 6));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.quiet_duration", 20));

  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.high_clip", 32767));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.low_clip", -32768));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.max_per10000_clip", 10));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.max_dc_offset", 1000));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.high_noise_level_bit", 11));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.low_speech_level_bit", 11));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Frontend.min_samples", 10000));

  CHKLOG(rc, ESR_SessionContains("CREC.Frontend.spectrum_filter_freq", &exists));
  if (!exists)
  {
    CHKLOG(rc, IntArrayListCreate(&intList));
    CHKLOG(rc, ESR_SessionSetProperty("CREC.Frontend.spectrum_filter_freq", intList, TYPES_INTARRAYLIST));
    intList = NULL;
  }
  CHKLOG(rc, ESR_SessionContains("CREC.Frontend.spectrum_filter_spread", &exists));
  if (!exists)
  {
    CHKLOG(rc, IntArrayListCreate(&intList));
    CHKLOG(rc, ESR_SessionSetProperty("CREC.Frontend.spectrum_filter_spread", intList, TYPES_INTARRAYLIST));
    intList = NULL;
  }
  return ESR_SUCCESS;
CLEANUP:
  if (intList != NULL)
    intList->destroy(intList);
  return rc;
}

/**
 * Generate legacy frontend parameter structure from ESR_Session.
 *
 * @param impl SR_RecognizerImpl handle
 * @param params Resulting structure
 */
ESR_ReturnCode SR_RecognizerGetFrontendLegacyParametersImpl(CA_FrontendInputParams* params)
{
  ESR_ReturnCode rc;
  IntArrayList* intList;
  size_t size, i, size_tValue;
  int iValue;

  passert(params != NULL);
  params->is_loaded = ESR_FALSE;
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.mel_dim", &params->mel_dim));
  CHKLOG(rc, ESR_SessionGetSize_t("CREC.Frontend.samplerate", &size_tValue));
  params->samplerate = (int) size_tValue;
  CHKLOG(rc, ESR_SessionGetFloat("CREC.Frontend.premel", &params->pre_mel));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.lowcut", &params->low_cut));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.highcut", &params->high_cut));
  CHKLOG(rc, ESR_SessionGetFloat("CREC.Frontend.window_factor", &params->window_factor));
  CHKLOG(rc, ESR_SessionGetBool("CREC.Frontend.do_skip_even_frames", &params->do_skip_even_frames));
  CHKLOG(rc, ESR_SessionGetFloat("CREC.Frontend.offset", &params->offset));
  CHKLOG(rc, ESR_SessionGetBool("CREC.Frontend.ddmel", &params->do_dd_mel));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.forgetfactor", &params->forget_factor));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.sv6_margin", &params->sv6_margin));
  CHKLOG(rc, ESR_SessionGetBool("CREC.Frontend.rastac0", &params->do_rastac0));
  CHKLOG(rc, ESR_SessionGetBool("CREC.Frontend.spectral_subtraction", &params->do_spectral_sub));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.spec_sub_dur", &params->spectral_sub_frame_dur));
  CHKLOG(rc, ESR_SessionGetFloat("CREC.Frontend.spec_sub_scale", &params->spec_sub_scale));
  CHKLOG(rc, ESR_SessionGetBool("CREC.Frontend.do_filterbank_dump", &params->do_filterbank_input));
  CHKLOG(rc, ESR_SessionGetBool("CREC.Frontend.do_filterbank_input", &params->do_filterbank_input));
  CHKLOG(rc, ESR_SessionGetBool("CREC.Frontend.do_smooth_c0", &params->do_smooth_c0));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.lpcorder", &params->lpc_order));
  CHKLOG(rc, ESR_SessionGetFloat("CREC.Frontend.warp_scale", &params->warp_scale));
  CHKLOG(rc, ESR_SessionGetFloat("CREC.Frontend.piecewise_start", &params->piecewise_start));
  CHKLOG(rc, ESR_SessionGetFloat("CREC.Frontend.peakdecayup", &params->peakpickup));
  CHKLOG(rc, ESR_SessionGetFloat("CREC.Frontend.peakdecaydown", &params->peakpickdown));

  CHKLOG(rc, ESR_SessionGetProperty("CREC.Frontend.mel_offset", (void **)&intList, TYPES_INTARRAYLIST));
  if (intList == NULL)
  {
    PLogError(L("ESR_INVALID_STATE"));
    return ESR_INVALID_STATE;
  }
  CHKLOG(rc, IntArrayListGetSize(intList, &size));
  for (i = 0; i < size; ++i)
    CHKLOG(rc, IntArrayListGet(intList, i, &params->mel_offset[i]));

  CHKLOG(rc, ESR_SessionGetProperty("CREC.Frontend.mel_loop", (void **)&intList, TYPES_INTARRAYLIST));
  if (intList == NULL)
  {
    PLogError(L("ESR_INVALID_STATE"));
    return ESR_INVALID_STATE;
  }
  CHKLOG(rc, IntArrayListGetSize(intList, &size));
  for (i = 0; i < size; ++i)
    CHKLOG(rc, IntArrayListGet(intList, i, &params->mel_loop[i]));

  CHKLOG(rc, ESR_SessionGetProperty("CREC.Frontend.melA", (void **)&intList, TYPES_INTARRAYLIST));
  CHKLOG(rc, IntArrayListGetSize(intList, &size));
  for (i = 0; i < size; ++i)
    CHKLOG(rc, IntArrayListGet(intList, i, &params->melA_scale[i]));

  CHKLOG(rc, ESR_SessionGetProperty("CREC.Frontend.melB", (void **)&intList, TYPES_INTARRAYLIST));
  CHKLOG(rc, IntArrayListGetSize(intList, &size));
  for (i = 0; i < size; ++i)
    CHKLOG(rc, IntArrayListGet(intList, i, &params->melB_scale[i]));

  CHKLOG(rc, ESR_SessionGetProperty("CREC.Frontend.dmelA", (void **)&intList, TYPES_INTARRAYLIST));
  CHKLOG(rc, IntArrayListGetSize(intList, &size));
  for (i = 0; i < size; ++i)
    CHKLOG(rc, IntArrayListGet(intList, i, &params->dmelA_scale[i]));

  CHKLOG(rc, ESR_SessionGetProperty("CREC.Frontend.dmelB", (void **)&intList, TYPES_INTARRAYLIST));
  CHKLOG(rc, IntArrayListGetSize(intList, &size));
  for (i = 0; i < size; ++i)
    CHKLOG(rc, IntArrayListGet(intList, i, &params->dmelB_scale[i]));

  CHKLOG(rc, ESR_SessionGetProperty("CREC.Frontend.ddmelA", (void **)&intList, TYPES_INTARRAYLIST));
  CHKLOG(rc, IntArrayListGetSize(intList, &size));
  for (i = 0; i < size; ++i)
    CHKLOG(rc, IntArrayListGet(intList, i, &params->ddmelA_scale[i]));

  CHKLOG(rc, ESR_SessionGetProperty("CREC.Frontend.ddmelB", (void **)&intList, TYPES_INTARRAYLIST));
  CHKLOG(rc, IntArrayListGetSize(intList, &size));
  for (i = 0; i < size; ++i)
    CHKLOG(rc, IntArrayListGet(intList, i, &params->ddmelB_scale[i]));

  CHKLOG(rc, ESR_SessionGetProperty("CREC.Frontend.rastaA", (void **)&intList, TYPES_INTARRAYLIST));
  CHKLOG(rc, IntArrayListGetSize(intList, &size));
  for (i = 0; i < size; ++i)
    CHKLOG(rc, IntArrayListGet(intList, i, &params->rastaA_scale[i]));

  CHKLOG(rc, ESR_SessionGetProperty("CREC.Frontend.rastaB", (void **)&intList, TYPES_INTARRAYLIST));
  CHKLOG(rc, IntArrayListGetSize(intList, &size));
  for (i = 0; i < size; ++i)
    CHKLOG(rc, IntArrayListGet(intList, i, &params->rastaB_scale[i]));

  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.speech_detect", &params->voice_margin));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.speech_above", &params->fast_voice_margin));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.ambient_within", &params->tracker_margin));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.start_windback", &params->start_windback));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.utterance_allowance", &params->unsure_duration));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.speech_duration", &params->voice_duration));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.quiet_duration", &params->quiet_duration));

  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.high_clip", &params->high_clip));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.low_clip", &params->low_clip));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.max_per10000_clip", &params->max_per10000_clip));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.max_dc_offset", &params->max_dc_offset));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.high_noise_level_bit", &params->high_noise_level_bit));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.low_speech_level_bit", &params->low_speech_level_bit));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Frontend.min_samples", &params->min_samples));

  CHKLOG(rc, ESR_SessionGetProperty("CREC.Frontend.spectrum_filter_freq", (void **)&intList, TYPES_INTARRAYLIST));
  if (intList == NULL)
  {
    PLogError(L("ESR_INVALID_STATE"));
    return ESR_INVALID_STATE;
  }
  CHKLOG(rc, IntArrayListGetSize(intList, &size));
  for (i = 0; i < size; ++i)
  {
    CHKLOG(rc, IntArrayListGet(intList, i, &iValue));
    params->spectrum_filter_freq[i] = iValue;
  }

  CHKLOG(rc, ESR_SessionGetProperty("CREC.Frontend.spectrum_filter_spread", (void **)&intList, TYPES_INTARRAYLIST));
  if (intList == NULL)
  {
    PLogError(L("ESR_INVALID_STATE"));
    return ESR_INVALID_STATE;
  }
  CHKLOG(rc, IntArrayListGetSize(intList, &size));
  for (i = 0; i < size; ++i)
  {
    CHKLOG(rc, IntArrayListGet(intList, i, &iValue));
    params->spectrum_filter_spread[i] = iValue;
  }
  params->is_loaded = ESR_TRUE;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

/**
 * Creates frontend components of SR_Recognizer.
 *
 * @param impl SR_RecognizerImpl handle
 */
ESR_ReturnCode SR_RecognizerCreateFrontendImpl(SR_RecognizerImpl* impl)
{
  ESR_ReturnCode rc;
  CA_FrontendInputParams* frontendParams;

  /* Create a frontend object */
  impl->frontend = CA_AllocateFrontend(1, 0, 1);
  frontendParams = CA_AllocateFrontendParameters();
  CHKLOG(rc, SR_RecognizerGetFrontendLegacyParametersImpl(frontendParams));

  CA_ConfigureFrontend(impl->frontend, frontendParams);

  /* Create a wave object */
  impl->wavein = CA_AllocateWave('N');
  if (impl->wavein == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  CA_ConfigureWave(impl->wavein, impl->frontend);
  CA_ConfigureVoicingAnalysis(impl->wavein, frontendParams);

  CA_LoadCMSParameters(impl->wavein, NULL, frontendParams);

  /* Create an utterance object */
  impl->utterance = CA_AllocateUtterance();
  if (impl->utterance == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  CA_InitUtteranceForFrontend(impl->utterance, frontendParams);
  CA_AttachCMStoUtterance(impl->wavein, impl->utterance);
  CA_FreeFrontendParameters(frontendParams);
  return ESR_SUCCESS;

CLEANUP:
  if (impl->frontend != NULL)
  {
    CA_UnconfigureFrontend(impl->frontend);
    CA_FreeFrontend(impl->frontend);
    impl->frontend = NULL;
  }
  if (impl->wavein != NULL)
  {
    CA_UnconfigureWave(impl->wavein);
    CA_FreeWave(impl->wavein);
    impl->wavein = NULL;
  }
  if (impl->utterance != NULL)
  {
    CA_ClearUtterance(impl->utterance);
    CA_FreeUtterance(impl->utterance);
    impl->utterance = NULL;
  }
  if (frontendParams != NULL)
    CA_FreeFrontendParameters(frontendParams);
  return rc;
}

/**
 * Populates legacy recognizer parameters from the session.
 *
 * Replaces setup_pattern_parameters()
 */
ESR_ReturnCode SR_AcousticModels_LoadLegacyRecognizerParameters(CA_RecInputParams* params)
{
  ESR_ReturnCode rc;

  passert(params != NULL);
  params->is_loaded = ESR_FALSE;
  CHKLOG(rc, ESR_SessionGetBool("CREC.Recognizer.partial_results", &params->do_partial));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.NBest", &params->top_choices));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.eou_threshold", &params->eou_threshold));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.max_altword_tokens", &params->max_altword_tokens));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.max_frames", &params->max_frames));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.max_fsm_arcs", &params->max_fsm_arcs));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.max_fsm_nodes", &params->max_fsm_nodes));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.max_fsmnode_tokens", &params->max_fsmnode_tokens));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.max_hmm_tokens", &params->max_hmm_tokens));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.max_model_states", &params->max_model_states));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.max_searches", &params->max_searches));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.max_word_tokens", &params->max_word_tokens));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.non_terminal_timeout", &params->non_terminal_timeout));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.num_wordends_per_frame", &params->num_wordends_per_frame));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.often", &params->traceback_freq));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.optional_terminal_timeout", &params->optional_terminal_timeout));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.reject", &params->reject_score));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.terminal_timeout", &params->terminal_timeout));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.viterbi_prune_thresh", &params->viterbi_prune_thresh));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Recognizer.wordpen", &params->word_penalty));
  params->is_loaded = ESR_TRUE;

  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerCreate(SR_Recognizer** self)
{
  SR_RecognizerImpl* impl;
  CA_RecInputParams* recogParams = NULL;
  ESR_ReturnCode rc;
  LCHAR recHandle[12];

  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  impl = NEW(SR_RecognizerImpl, MTAG);
  if (impl == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }

  impl->Interface.start = &SR_RecognizerStartImpl;
  impl->Interface.stop = &SR_RecognizerStopImpl;
  impl->Interface.destroy = &SR_RecognizerDestroyImpl;
  impl->Interface.setup = &SR_RecognizerSetupImpl;
  impl->Interface.unsetup = &SR_RecognizerUnsetupImpl;
  impl->Interface.isSetup = &SR_RecognizerIsSetupImpl;
  impl->Interface.getParameter = &SR_RecognizerGetParameterImpl;
  impl->Interface.getSize_tParameter = &SR_RecognizerGetSize_tParameterImpl;
  impl->Interface.getBoolParameter = &SR_RecognizerGetBoolParameterImpl;
  impl->Interface.setParameter = &SR_RecognizerSetParameterImpl;
  impl->Interface.setSize_tParameter = &SR_RecognizerSetSize_tParameterImpl;
  impl->Interface.setBoolParameter = &SR_RecognizerSetBoolParameterImpl;
  impl->Interface.setLockFunction = &SR_RecognizerSetLockFunctionImpl;
  impl->Interface.hasSetupRules = &SR_RecognizerHasSetupRulesImpl;
  impl->Interface.activateRule = &SR_RecognizerActivateRuleImpl;
  impl->Interface.deactivateRule = &SR_RecognizerDeactivateRuleImpl;
  impl->Interface.deactivateAllRules = &SR_RecognizerDeactivateAllRulesImpl;
  impl->Interface.isActiveRule = &SR_RecognizerIsActiveRuleImpl;
  impl->Interface.setWordAdditionCeiling = &SR_RecognizerSetWordAdditionCeilingImpl;
  impl->Interface.checkGrammarConsistency = &SR_RecognizerCheckGrammarConsistencyImpl;
  impl->Interface.getModels = &SR_RecognizerGetModelsImpl;
  impl->Interface.putAudio = &SR_RecognizerPutAudioImpl;
  impl->Interface.advance = &SR_RecognizerAdvanceImpl;
  impl->Interface.loadUtterance = &SR_RecognizerLoadUtteranceImpl;
  impl->Interface.loadWaveFile = &SR_RecognizerLoadWaveFileImpl;
  impl->Interface.logEvent = &SR_RecognizerLogEventImpl;
  impl->Interface.logToken = &SR_RecognizerLogTokenImpl;
  impl->Interface.logTokenInt = &SR_RecognizerLogTokenIntImpl;
  impl->Interface.logSessionStart = &SR_RecognizerLogSessionStartImpl;
  impl->Interface.logSessionEnd = &SR_RecognizerLogSessionEndImpl;
  impl->Interface.logWaveformData = &SR_RecognizerLogWaveformDataImpl;
  impl->Interface.isSignalClipping = &SR_RecognizerIsSignalClippingImpl;
  impl->Interface.isSignalDCOffset = &SR_RecognizerIsSignalDCOffsetImpl;
  impl->Interface.isSignalNoisy = &SR_RecognizerIsSignalNoisyImpl;
  impl->Interface.isSignalTooFewSamples = &SR_RecognizerIsSignalTooFewSamplesImpl;
  impl->Interface.isSignalTooManySamples = &SR_RecognizerIsSignalTooManySamplesImpl;
  impl->Interface.isSignalTooQuiet = &SR_RecognizerIsSignalTooQuietImpl;

  impl->frontend = NULL;
  impl->wavein = NULL;
  impl->utterance = NULL;
  impl->confidenceScorer = NULL;
  impl->recognizer = NULL;
  impl->models = NULL;
  impl->grammars = NULL;
  impl->result = NULL;
  impl->parameters = NULL;
  impl->acousticState = NULL;
  impl->audioBuffer = NULL;
  impl->buffer = NULL;
  impl->frames = impl->processed;
  impl->internalState = SR_RECOGNIZER_INTERNAL_BEGIN;
  impl->isStarted = ESR_FALSE;
  impl->isRecognizing = ESR_FALSE;
  impl->gotLastFrame = ESR_FALSE;
  impl->sampleRate = 0;
  impl->lockFunction = NULL;
  impl->lockData = NULL;
  impl->eventLog = NULL;
  impl->osi_log_level = 0;
  impl->waveformBuffer = NULL;
  impl->isSignalQualityInitialized = ESR_FALSE;
  impl->beginningOfSpeechOffset = 0;
  impl->gatedMode = ESR_TRUE;
  impl->bgsniff = 0;
  impl->isSignalClipping       = ESR_FALSE;
  impl->isSignalDCOffset       = ESR_FALSE;
  impl->isSignalNoisy          = ESR_FALSE;
  impl->isSignalTooFewSamples  = ESR_FALSE;
  impl->isSignalTooManySamples = ESR_FALSE;
  impl->isSignalTooQuiet       = ESR_FALSE;

  CHKLOG(rc, ESR_SessionTypeCreate(&impl->parameters));
  CHKLOG(rc, SR_RecognizerToSessionImpl());
  CHKLOG(rc, ESR_SessionGetSize_t(L("SREC.Recognizer.osi_log_level"), &impl->osi_log_level));

  /* create the event log */
  if (impl->osi_log_level) /* do some logging if non-zero val */
    CHKLOG(rc, ESR_SessionGetProperty(L("eventlog"), (void **)&impl->eventLog, TYPES_SR_EVENTLOG));

  /* Record the OSI log event */
  psprintf(recHandle, L("%p"), impl);
  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("REC"), recHandle));
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SWIcrst")));

  CHKLOG(rc, SR_RecognizerFrontendToSessionImpl());
  CHKLOG(rc, SR_RecognizerCreateFrontendImpl(impl));
  rc = ESR_SessionGetProperty("recognizer.confidenceScorer", (void **)&impl->confidenceScorer, TYPES_CONFIDENCESCORER);
  if (rc == ESR_NO_MATCH_ERROR)
  {
    impl->confidenceScorer = CA_AllocateConfidenceScorer();

    if (!CA_LoadConfidenceScorer(impl->confidenceScorer)) {
      rc = ESR_INVALID_STATE;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
    CHKLOG(rc, ESR_SessionSetProperty("recognizer.confidenceScorer", impl->confidenceScorer, TYPES_CONFIDENCESCORER));
  }
  else if (rc != ESR_SUCCESS)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }

  recogParams = CA_AllocateRecognitionParameters();
  if (recogParams == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  CHKLOG(rc, SR_AcousticModels_LoadLegacyRecognizerParameters(recogParams));
  impl->recognizer = CA_AllocateRecognition();
  if (impl->recognizer == NULL)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  CA_ConfigureRecognition(impl->recognizer, recogParams);
  CA_FreeRecognitionParameters(recogParams);
  CHKLOG(rc, HashMapCreate(&impl->grammars));
  CHKLOG(rc, CircularBufferCreate(sizeof(asr_int16_t) * AUDIO_CIRC_BUFFER_SIZE, MTAG, &impl->buffer));
  CHKLOG(rc, ESR_SessionGetSize_t("CREC.Frontend.samplerate", &impl->sampleRate));

  impl->FRAME_SIZE = impl->sampleRate / FRAMERATE * SAMPLE_SIZE;

  if ((impl->audioBuffer = MALLOC(impl->FRAME_SIZE, MTAG)) == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    goto CLEANUP;
  }

  /* create the waveform buffer */
  CHKLOG(rc, WaveformBuffer_Create(&impl->waveformBuffer, impl->FRAME_SIZE));

  CHKLOG(rc, ESR_SessionGetSize_t("SREC.Recognizer.utterance_timeout", &impl->utterance_timeout));

  /* OSI logging (SUCCESS) */
  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("REC"), recHandle));
  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("SUCCESS"), L("ESR_SUCCESS")));
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SWIcrnd")));

  CHKLOG(rc, SR_AcousticStateCreateImpl(&impl->Interface));

  CHKLOG(rc, ESR_SessionGetSize_t(L("cmdline.bgsniff"), &impl->bgsniff));
  /* gated mode == beginning of speech detection */
  CHKLOG(rc, ESR_SessionGetBool(L("cmdline.gatedmode"), &impl->gatedMode));

  *self = (SR_Recognizer*) impl;
  return ESR_SUCCESS;
CLEANUP:
  /* OSI logging (FAILURE) */
  if (impl->eventLog != NULL)
  {
    SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("REC"), recHandle);
    SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("FAILURE"), ESR_rc2str(rc));
    SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SWIcrnd"));
  }

  if (recogParams != NULL)
    CA_FreeRecognitionParameters(recogParams);
  impl->Interface.destroy(&impl->Interface);
  return rc;
}

ESR_ReturnCode SR_RecognizerDestroyImpl(SR_Recognizer* self)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  ESR_BOOL exists; // isSetup;
  ESR_ReturnCode rc;
  LCHAR recHandle[12];

  if (impl->result != NULL)
  {
    SR_RecognizerResult_Destroy(impl->result);
    impl->result = NULL;
  }

  if (impl->eventLog != NULL)
  {
    /* Record the OSI log event */
    psprintf(recHandle, L("%p"), impl);
    CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("REC"), recHandle));
    CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SWIdesst")));
  }

  /* Clean session */
  CHKLOG(rc, ESR_SessionContains("recognizer.confidenceScorer", &exists));
  if (exists)
    CHKLOG(rc, ESR_SessionRemoveProperty("recognizer.confidenceScorer"));

  if (impl->confidenceScorer != NULL)
  {
    CA_FreeConfidenceScorer(impl->confidenceScorer);
    impl->confidenceScorer = NULL;
  }

  /* Clear CMS, CRS_RecognizerClose() */
  if (impl->wavein != NULL)
  {
    ESR_BOOL isAttached, isConfigured;

    CHKLOG(rc, CA_IsCMSAttachedtoUtterance(impl->wavein, &isAttached));
    if (isAttached)
      CA_DetachCMSfromUtterance(impl->wavein, impl->utterance);

    CHKLOG(rc, CA_IsConfiguredForAgc(impl->wavein, &isConfigured));
    if (isConfigured)
      CA_ClearCMSParameters(impl->wavein);
  }

  /* Free Utterance */
  if (impl->utterance != NULL)
  {
    CA_ClearUtterance(impl->utterance);
    CA_FreeUtterance(impl->utterance);
    impl->utterance = NULL;
  }

  /* Free WaveformBuffer */
  if (impl->waveformBuffer != NULL)
  {
    WaveformBuffer_Destroy(impl->waveformBuffer);
    impl->waveformBuffer = NULL;
  }

  /* Free recognizer */
/*  CHKLOG(rc, self->isSetup(self, &isSetup));
  if (isSetup)
    CHKLOG(rc, self->unsetup(self));*/
  if (impl->grammars != NULL)
    CHKLOG(rc, self->deactivateAllRules(self));
  if (impl->recognizer != NULL)
  {
    CA_UnloadRecognitionModels(impl->recognizer);
    CA_UnconfigureRecognition(impl->recognizer);
    CA_FreeRecognition(impl->recognizer);
    impl->recognizer = NULL;
  }

  if (impl->grammars != NULL)
  {
    CHKLOG(rc, HashMapDestroy(impl->grammars));
    impl->grammars = NULL;
  }

  if (impl->buffer != NULL)
  {
    FREE(impl->buffer);
    impl->buffer = NULL;
  }

  if (impl->audioBuffer != NULL)
  {
    FREE(impl->audioBuffer);
    impl->audioBuffer = NULL;
  }

  /* Free frontend */
  if (impl->frontend)
  {
    CA_UnconfigureFrontend(impl->frontend);
    CA_FreeFrontend(impl->frontend);
    impl->frontend = NULL;
  }

  /* Free wave */
  if (impl->wavein)
  {
    CA_UnconfigureWave(impl->wavein);
    CA_FreeWave(impl->wavein);
    impl->wavein = NULL;
  }

  if (impl->parameters != NULL)
    CHKLOG(rc, impl->parameters->destroy(impl->parameters));

  if (impl->eventLog != NULL)
  {
    /* OSI logging (SUCCESS) */
    CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("REC"), recHandle));
    CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("SUCCESS"), L("ESR_SUCCESS")));
    CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SWIdesnd")));
    impl->eventLog = NULL;
  }

  if (impl->acousticState != NULL)
  {
    impl->acousticState->destroy(self);
    impl->acousticState = NULL;
  }
  FREE(impl);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode beginRecognizing(SR_RecognizerImpl* impl)
{
  CA_RecInputParams* recogParams;
  LCHAR tok[80];
  LCHAR* val;
  PTimeStamp BORT;
  size_t i, grammarSize;
  ESR_ReturnCode rc;

  /* Setup recognizer for new utterance */
  recogParams = CA_AllocateRecognitionParameters();
  if (recogParams == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  SR_AcousticModels_LoadLegacyRecognizerParameters(recogParams);
  CA_BeginRecognition(impl->recognizer, NULL, 1, recogParams);
  CA_FreeRecognitionParameters(recogParams);
  impl->isRecognizing = ESR_TRUE;

  /* OSI log the  grammars */
  CHKLOG(rc, HashMapGetSize(impl->grammars, &grammarSize));
  for (i = 0; i < grammarSize; ++i)
  {
    psprintf(tok, L("GURI%d"), i);
    /* use the key as the grammar URI */
    CHKLOG(rc, HashMapGetKeyAtIndex(impl->grammars, i, &val));
    CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, tok, val));
  }
  /* OSI ACST acoustic state reset */
  CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->osi_log_level, L("ACST"), 0));
  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("LANG"), L("en-us")));

  /* OSI log the start of recognition */
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SWIrcst")));

  /* save the BORT timing (begin of recog) */
  PTimeStampSet(&BORT);
  impl->recogLogTimings.BORT = PTimeStampDiff(&BORT, &impl->timestamp);

  return ESR_SUCCESS;
CLEANUP:
  if (recogParams != NULL)
    CA_FreeRecognitionParameters(recogParams);
  return rc;
}

ESR_ReturnCode SR_RecognizerStartImpl(SR_Recognizer* self)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  size_t silence_duration_in_frames;
  size_t end_of_utterance_hold_off_in_frames;
  size_t grammarCount;
  ESR_ReturnCode rc;
  ESR_BOOL enableGetWaveform = ESR_FALSE;

  CHKLOG(rc, impl->grammars->getSize(impl->grammars, &grammarCount));
  if (impl->models == NULL)
  {
    PLogError("ESR_INVALID_STATE: No rule has been set up");
    return ESR_INVALID_STATE;
  }
  if (grammarCount < 1)
  {
    PLogError("ESR_INVALID_STATE: No rule has been activated");
    return ESR_INVALID_STATE;
  }

  if (!CA_OpenWaveFromDevice(impl->wavein, DEVICE_RAW_PCM, impl->frontend->samplerate, 0, WAVE_DEVICE_RAW))
  {
    rc = ESR_INVALID_STATE;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }

  /* Setup utterance */
  CA_UnlockUtteranceForInput(impl->utterance);

  /* Setup utterance */
  CHKLOG(rc, ESR_SessionGetSize_t(L("cmdline.silence_duration_in_frames"), &silence_duration_in_frames));
  CHKLOG(rc, ESR_SessionGetSize_t(L("cmdline.end_of_utterance_hold_off_in_frames"), &end_of_utterance_hold_off_in_frames));
  CA_SetEndOfUtteranceByLevelTimeout(impl->utterance, silence_duration_in_frames, end_of_utterance_hold_off_in_frames);

  CA_ResetVoicing(impl->utterance);

  /*
   * NOTE: We don't actually begin the recognizer here, the beginning of speech
   * detector will do that.
   */

  impl->gotLastFrame = ESR_FALSE;
  impl->isStarted = ESR_TRUE;
  impl->isRecognizing = ESR_FALSE;
  impl->isSignalQualityInitialized = ESR_FALSE;
  impl->internalState = SR_RECOGNIZER_INTERNAL_BEGIN;
  PTimeStampSet(&impl->timestamp);

  /* reset waveform buffer at start of every recognition */
  CHKLOG(rc, WaveformBuffer_Reset(impl->waveformBuffer));

  /* is waveform buffering active? */
  rc = ESR_SessionGetBool(L("enableGetWaveform"), &enableGetWaveform);
  // rc = impl->parameters->getBool(impl->parameters, L("enableGetWaveform"), &enableGetWaveform);
  if (rc != ESR_SUCCESS && rc != ESR_NO_MATCH_ERROR)
  {
    PLogError(L("%s: could determine whether VoiceEnrollment active or not"), ESR_rc2str(rc));
    goto CLEANUP;
  }
  if (enableGetWaveform)
    CHKLOG(rc, WaveformBuffer_SetBufferingState(impl->waveformBuffer, WAVEFORM_BUFFERING_ON_CIRCULAR));
  else
    CHKLOG(rc, WaveformBuffer_SetBufferingState(impl->waveformBuffer, WAVEFORM_BUFFERING_OFF));

  /* I am going to try to open the audio waveform file here */
  if (impl->osi_log_level & OSI_LOG_LEVEL_AUDIO)
  {
    /* open a new audio waveform file */
    rc = SR_EventLogAudioOpen(impl->eventLog, L("audio/L16"), impl->sampleRate, SAMPLE_SIZE);
    if (rc != ESR_SUCCESS)
    {
      PLogError(L("%s: could not open the RIFF audio file"), ESR_rc2str(rc));
      goto CLEANUP;
    }
  }
  impl->frames = impl->processed = 0;
  return ESR_SUCCESS;
CLEANUP:
/*  self->stop(self);*/
  return rc;
}

ESR_ReturnCode SR_RecognizerStopImpl(SR_Recognizer* self)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  SR_AcousticModelsImpl* modelsImpl;
  ESR_ReturnCode rc;

  PLOG_DBG_API_ENTER();
  if (!impl->isStarted)
  {
    /* In case the user calls stop() twice */
    return ESR_SUCCESS;
  }
  modelsImpl = (SR_AcousticModelsImpl*) impl->models;

  /* Clean-up recognizer and utterance */
  switch (impl->internalState)
  {
    case SR_RECOGNIZER_INTERNAL_BEGIN:
      /* Recognizer was never started */
      CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("MODE"), L("BEGIN")));
      CA_LockUtteranceFromInput(impl->utterance);
      impl->internalState = SR_RECOGNIZER_INTERNAL_END;
      if (impl->eventLog != NULL)
      {
        CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("SR_RECOGNIZER_INTERNAL_BEGIN -> SR_RECOGNIZER_INTERNAL_END")));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
        CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
      }
      break;

    case SR_RECOGNIZER_INTERNAL_BOS_TIMEOUT:
      /* Recognizer was never started */
      CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("MODE"), L("BOS_TIMEOUT")));
      CA_LockUtteranceFromInput(impl->utterance);
      impl->internalState = SR_RECOGNIZER_INTERNAL_END;
      if (impl->eventLog != NULL)
      {
        CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("SR_RECOGNIZER_INTERNAL_BOS_TIMEOUT -> SR_RECOGNIZER_INTERNAL_END")));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
        CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
      }
      break;

    case SR_RECOGNIZER_INTERNAL_BOS_NO_MATCH:
      /* Recognizer was never started */
      CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("MODE"), L("BOS_NO_MATCH")));
      CA_LockUtteranceFromInput(impl->utterance);
      impl->internalState = SR_RECOGNIZER_INTERNAL_END;
      if (impl->eventLog != NULL)
      {
        CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("SR_RECOGNIZER_INTERNAL_BOS_NO_MATCH -> SR_RECOGNIZER_INTERNAL_END")));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
        CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
      }
      break;

    case SR_RECOGNIZER_INTERNAL_BOS_DETECTION:
      /* Recognizer was never started */
      CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("MODE"), L("BOS_DETECTION")));
      CA_LockUtteranceFromInput(impl->utterance);
      impl->internalState = SR_RECOGNIZER_INTERNAL_END;
      if (impl->eventLog != NULL)
      {
        CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("SR_RECOGNIZER_INTERNAL_BOS_DETECTION -> SR_RECOGNIZER_INTERNAL_END")));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
        CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
      }
      break;

    case SR_RECOGNIZER_INTERNAL_EOS_DETECTION:
      CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("MODE"), L("EOS_DETECTION")));
      CA_LockUtteranceFromInput(impl->utterance);
      if (!CA_EndRecognition(impl->recognizer, modelsImpl->pattern, impl->utterance))
      {
        rc = ESR_INVALID_STATE;
        PLogError(ESR_rc2str(rc));
        goto CLEANUP;
      }
      impl->internalState = SR_RECOGNIZER_INTERNAL_END;
      if (impl->eventLog != NULL)
      {
        CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("SR_RECOGNIZER_INTERNAL_EOS_DETECTION -> SR_RECOGNIZER_INTERNAL_END")));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
        CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
      }
      break;

    case SR_RECOGNIZER_INTERNAL_EOI:
      CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("MODE"), L("EOI")));
      CA_LockUtteranceFromInput(impl->utterance);
      if (!CA_EndRecognition(impl->recognizer, modelsImpl->pattern, impl->utterance))
      {
        rc = ESR_INVALID_STATE;
        PLogError(ESR_rc2str(rc));
        goto CLEANUP;
      }
      impl->internalState = SR_RECOGNIZER_INTERNAL_END;
      if (impl->eventLog != NULL)
      {
        CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("SR_RECOGNIZER_INTERNAL_EOI -> SR_RECOGNIZER_INTERNAL_END")));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
        CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
      }
      break;

    case SR_RECOGNIZER_INTERNAL_EOS:
      CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("MODE"), L("EOS")));
      CA_LockUtteranceFromInput(impl->utterance);
      if (!CA_EndRecognition(impl->recognizer, modelsImpl->pattern, impl->utterance))
      {
        rc = ESR_INVALID_STATE;
        PLogError(ESR_rc2str(rc));
        goto CLEANUP;
      }
      impl->internalState = SR_RECOGNIZER_INTERNAL_END;
      if (impl->eventLog != NULL)
      {
        CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("SR_RECOGNIZER_INTERNAL_EOS -> SR_RECOGNIZER_INTERNAL_END")));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
        CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
      }
      break;

    case SR_RECOGNIZER_INTERNAL_END:
      /* Recognizer already shut down */
      CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("MODE"), L("END")));
      break;

    default:
      /* Shut down recognizer */
      CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->osi_log_level, L("MODE"), impl->internalState));
      if (impl->eventLog != NULL)
      {
        CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("unknown state -> SR_RECOGNIZER_INTERNAL_END")));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
        CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
      }
      CA_LockUtteranceFromInput(impl->utterance);
      if (impl->isRecognizing)
      {
        if (!CA_EndRecognition(impl->recognizer, modelsImpl->pattern, impl->utterance))
        {
          rc = ESR_INVALID_STATE;
          PLogError(ESR_rc2str(rc));
          goto CLEANUP;
        }
      }
      rc = ESR_INVALID_STATE;
      PLogError(L("%s: %d"), ESR_rc2str(rc), impl->internalState);
      impl->internalState = SR_RECOGNIZER_INTERNAL_END;
      goto CLEANUP;
  }
  if (impl->eventLog != NULL)
  {
    int n;
    LCHAR result[MAX_ENTRY_LENGTH];
    result[0] = L('\0');

    n = CA_GetUnprocessedFramesInUtterance(impl->utterance);
    CHKLOG(rc, SR_EventLogTokenInt(impl->eventLog, L("CA_GetUnprocessedFramesInUtterance() (x10ms)"), n));
    CA_FullResultLabel(impl->recognizer, result, MAX_ENTRY_LENGTH - 1);
    CHKLOG(rc, SR_EventLogToken(impl->eventLog, L("CA_FullResultLabel() (x20ms)"), result));
    n = CircularBufferGetSize(impl->buffer);
    CHKLOG(rc, SR_EventLogTokenInt(impl->eventLog, L("CircularBufferGetSize() (samples)"), n / SAMPLE_SIZE));
  }
  if (impl->lockFunction)
    impl->lockFunction(ESR_LOCK, impl->lockData);
  CircularBufferReset(impl->buffer);
  if (impl->lockFunction)
    impl->lockFunction(ESR_UNLOCK, impl->lockData);
  if (CA_RecognitionHasResults(impl->recognizer))
    CA_ClearResults(impl->recognizer);
  CA_FlushUtteranceFrames(impl->utterance);
  CA_CalculateCMSParameters(impl->wavein);
  CA_CloseDevice(impl->wavein);

  /* record the OSI event */
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SWIstop")));

  if (impl->result != NULL)
  {
    CHKLOG(rc, SR_RecognizerResult_Destroy(impl->result));
    impl->result = NULL;
  }

  if (impl->lockFunction)
    impl->lockFunction(ESR_LOCK, impl->lockData);
  impl->gotLastFrame = ESR_TRUE;
  PLOG_DBG_TRACE((L("SR_Recognizer shutdown occured")));
  impl->isStarted = ESR_FALSE;
  impl->isRecognizing = ESR_FALSE;
  if (impl->osi_log_level & OSI_LOG_LEVEL_AUDIO)
    SR_EventLogAudioClose(impl->eventLog);

  impl->recogLogTimings.BORT = 0;
  impl->recogLogTimings.DURS = 0;
  impl->recogLogTimings.EORT = 0;
  impl->recogLogTimings.EOSD = 0;
  impl->recogLogTimings.EOSS = 0;
  impl->recogLogTimings.BOSS = 0;
  impl->recogLogTimings.EOST = 0;
  impl->eos_reason = L("undefined");

  if (impl->lockFunction)
    impl->lockFunction(ESR_UNLOCK, impl->lockData);
  PLOG_DBG_API_EXIT(rc);
  return rc;
CLEANUP:
  PLOG_DBG_API_EXIT(rc);
  return rc;
}

ESR_ReturnCode SR_RecognizerSetupImpl(SR_Recognizer* self)
{
  ESR_ReturnCode rc;
  CA_AcoustInputParams* acousticParams = NULL;
  SR_AcousticModelsImpl* modelsImpl;
  SR_AcousticModels* models;
  SR_RecognizerImpl* recogImpl = NULL;
  CA_Acoustic* acoustic;
  size_t size, i;
  LCHAR           filenames[P_PATH_MAX];
  size_t          len;

  len = P_PATH_MAX;
  CHKLOG(rc, ESR_SessionGetLCHAR ( L("cmdline.modelfiles"), filenames, &len ));

  CHKLOG(rc, SR_AcousticModelsLoad ( filenames, &models ));

  if (models == NULL)
    {
      PLogError(L("ESR_INVALID_STATE while finding cmdline.modelfiles"));
      return ESR_INVALID_STATE;
    }
  modelsImpl = (SR_AcousticModelsImpl*) models;
  recogImpl = (SR_RecognizerImpl*) self;
  acousticParams = NULL;

  CHKLOG(rc, SR_AcousticModelsGetCount(models, &size));
  acousticParams = CA_AllocateAcousticParameters();
  if (acousticParams == NULL)
      {
      rc = ESR_OUT_OF_MEMORY;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
      }
    CHKLOG(rc, modelsImpl->getLegacyParameters(acousticParams));
    CHKLOG(rc, ArrayListGetSize(modelsImpl->acoustic, &size));
    for (i = 0; i < size; ++i)
      {
      CHKLOG(rc, ArrayListGet(modelsImpl->acoustic, i, (void **)&acoustic));
      CA_LoadModelsInAcoustic(recogImpl->recognizer, acoustic, acousticParams);
      }
  CA_FreeAcousticParameters(acousticParams);

  recogImpl->models = models;
  CHKLOG(rc, modelsImpl->setupPattern(recogImpl->models, self));
  return ESR_SUCCESS;
 CLEANUP:
  if (acousticParams != NULL)
    CA_FreeAcousticParameters(acousticParams);
  if (recogImpl != NULL)
    CA_UnloadRecognitionModels(recogImpl->recognizer);
  return rc;
}

ESR_ReturnCode SR_RecognizerUnsetupImpl(SR_Recognizer* self)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  SR_AcousticModelsImpl* modelsImpl = (SR_AcousticModelsImpl*) impl->models;
  ESR_ReturnCode rc;

  CHKLOG(rc, modelsImpl->unsetupPattern(impl->models));
  CA_UnloadRecognitionModels(impl->recognizer);
  CHKLOG(rc, SR_AcousticModelsDestroy ( impl->models ));
  impl->models = NULL;
  return ESR_SUCCESS;
 CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerIsSetupImpl(SR_Recognizer* self, ESR_BOOL* isSetup)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;

  if (isSetup == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  *isSetup = impl->models != NULL;
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_RecognizerGetParameterImpl(SR_Recognizer* self, const LCHAR* key,
    LCHAR* value, size_t* len)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  ESR_ReturnCode rc;

  rc = impl->parameters->getLCHAR(impl->parameters, key, value, len);
  if (rc == ESR_NO_MATCH_ERROR)
  {
    CHKLOG(rc, ESR_SessionGetLCHAR(key, value, len));
    return ESR_SUCCESS;
  }
  else if (rc != ESR_SUCCESS)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

/*
 * The get / set code is a mess. Since we only use size_t parameters, that's all
 * that I am going to make work. The impl->parameters don't work so you always
 * have to get them from the session. The impl always logs an error. SteveR
 */

ESR_ReturnCode SR_RecognizerGetSize_tParameterImpl(SR_Recognizer* self, const LCHAR* key,
    size_t* value)
{
  ESR_ReturnCode rc;

  CHKLOG(rc, ESR_SessionGetSize_t(key, value));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerGetBoolParameterImpl(SR_Recognizer* self, const LCHAR* key, ESR_BOOL* value)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  ESR_ReturnCode rc;

  rc = impl->parameters->getBool(impl->parameters, key, value);
  if (rc == ESR_NO_MATCH_ERROR)
  {
    CHKLOG(rc, ESR_SessionGetBool(key, value));
    return ESR_SUCCESS;
  }
  else if (rc != ESR_SUCCESS)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerSetParameterImpl(SR_Recognizer* self, const LCHAR* key,
    LCHAR* value)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  LCHAR temp[256];
  ESR_ReturnCode rc;
  size_t len = 256;

  rc = impl->parameters->getLCHAR(impl->parameters, key, temp, &len);
  if (rc == ESR_SUCCESS)
  {
    if (LSTRCMP(temp, value) == 0)
      return ESR_SUCCESS;
    CHKLOG(rc, impl->parameters->removeAndFreeProperty(impl->parameters, key));
  }
  else if (rc != ESR_NO_MATCH_ERROR && rc != ESR_INVALID_RESULT_TYPE)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }

  CHKLOG(rc, impl->parameters->setLCHAR(impl->parameters, key, value));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}
/*
 * The only set param function that is working is for the size_t parameters; and not
 * all of them are working, only the ones specified in the function itself. There are
 * two reasons for this: first most of the set functions just put the value in an unused
 * table that has no effect; second many of the changes need to be propogated to a specific
 * part of the code. This needs to be evaluated on a per parameter basis. SteveR
 */

/*
 * This function will be used to set parameters in the session. We need to go through
 * the recognizer so as to propogate the values into the recognizer. We will rely on
 * the session to do the right thing. SteveR
 */

ESR_ReturnCode SR_RecognizerSetSize_tParameterImpl(SR_Recognizer* self, const LCHAR* key,
    size_t value)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  ESR_ReturnCode rc;

  rc = ESR_SessionSetSize_t ( key, value );

  if (rc == ESR_SUCCESS)
  {
    if  ( LSTRCMP ( L("SREC.Recognizer.utterance_timeout"), key ) == 0 )
    {
      impl->utterance_timeout = value;
    }
    else if  ( LSTRCMP ( L("CREC.Recognizer.terminal_timeout"), key ) == 0 )
    {
      impl->recognizer->eosd_parms->endnode_timeout = value;
    }
    else if  ( LSTRCMP ( L("CREC.Recognizer.optional_terminal_timeout"), key ) == 0 )
    {
      impl->recognizer->eosd_parms->optendnode_timeout = value;
    }
    else if  ( LSTRCMP ( L("CREC.Recognizer.non_terminal_timeout"), key ) == 0 )
    {
      impl->recognizer->eosd_parms->internalnode_timeout = value;
    }
    else if  ( LSTRCMP ( L("CREC.Recognizer.eou_threshold"), key ) == 0 )
    {
      impl->recognizer->eosd_parms->eos_costdelta = (frameID)value;
      impl->recognizer->eosd_parms->opt_eos_costdelta = (frameID)value;
    }
    else
    {
      PLogError(L("ESR_INVALID_ARGUMENT"));
      rc = ESR_INVALID_ARGUMENT;
    }
  }
  return rc;
}


ESR_ReturnCode SR_RecognizerSetBoolParameterImpl(SR_Recognizer* self, const LCHAR* key, ESR_BOOL value)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  ESR_BOOL temp;
  ESR_ReturnCode rc;

  rc = impl->parameters->getBool(impl->parameters, key, &temp);
  if (rc == ESR_SUCCESS)
  {
    if (temp == value)
      return ESR_SUCCESS;
    CHKLOG(rc, impl->parameters->removeAndFreeProperty(impl->parameters, key));
  }
  else if (rc != ESR_NO_MATCH_ERROR && rc != ESR_INVALID_RESULT_TYPE)
    return rc;

  CHKLOG(rc, impl->parameters->setBool(impl->parameters, key, value));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerHasSetupRulesImpl(SR_Recognizer* self, ESR_BOOL* hasSetupRules)
{
  SR_RecognizerImpl* recogImpl = (SR_RecognizerImpl*) self;
  size_t size;
  ESR_ReturnCode rc;

  if (hasSetupRules == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  CHKLOG(rc, HashMapGetSize(recogImpl->grammars, &size));
  *hasSetupRules = size > 0;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerActivateRuleImpl(SR_Recognizer* self, SR_Grammar* grammar,
    const LCHAR* ruleName, unsigned int weight)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  SR_GrammarImpl* grammarImpl = (SR_GrammarImpl*) grammar;
  SR_AcousticModelsImpl* modelsImpl;
  LCHAR grammarID[80];
  ESR_ReturnCode rc;
  char *failure_reason = NULL;

  if (grammar == NULL)
  {
    if (impl->eventLog)
      failure_reason = "badinput";
    rc = ESR_INVALID_ARGUMENT;
    PLogError(L("ESR_INVALID_ARGUMENT"));
    goto CLEANUP;
  }

  if (impl->models == NULL)
  {
    failure_reason = "nomodels";
    rc = ESR_INVALID_STATE;
    PLogError(L("acoustic models must be configured"));
    goto CLEANUP;
  }

  modelsImpl = (SR_AcousticModelsImpl*) impl->models;

  if (ruleName == NULL)
    psprintf(grammarID, L("%p"), grammar);
  else
  {
    if (LSTRLEN(ruleName) > 80)
    {
      rc = ESR_BUFFER_OVERFLOW;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
    LSTRCPY(grammarID, ruleName);
  }

  CHKLOG(rc, HashMapPut(impl->grammars, grammarID, grammar));
  if (CA_SetupSyntaxForRecognizer(grammarImpl->syntax, impl->recognizer))
  {
    failure_reason = "cafailed";
    rc = ESR_INVALID_STATE;
    PLogError(L("ESR_INVALID_STATE"));
    goto CLEANUP;
  }

   CHKLOG(rc, SR_Grammar_SetupRecognizer(grammar, self));
  grammarImpl->isActivated = ESR_TRUE;

  /*
   * If we want to log dynamically added words, then we must give the grammar a reference
   * to our event log. The grammar logs word additions if and only if its reference to
   * eventLog is non-null.
   */
  if (impl->osi_log_level & OSI_LOG_LEVEL_ADDWD)
    grammarImpl->eventLog = impl->eventLog;
  else
    grammarImpl->eventLog = NULL;

  rc = ESR_SUCCESS;

CLEANUP:
  if (impl->eventLog)
  {
    if (failure_reason)
    {
      SR_EventLogTokenInt(impl->eventLog, L("igrm"), (int) grammar);
      SR_EventLogToken(impl->eventLog, L("rule"), ruleName);
      SR_EventLogToken(impl->eventLog, L("rslt"), "fail");
      SR_EventLogToken(impl->eventLog, L("reason"), failure_reason);
      SR_EventLogEvent(impl->eventLog, L("ESRacGrm"));
    }
    else
    {
      SR_EventLogTokenInt(impl->eventLog, L("igrm"), (int) grammar);
      SR_EventLogToken(impl->eventLog, L("rule"), ruleName);
      SR_EventLogToken(impl->eventLog, L("rslt"), "ok");
      SR_EventLogEvent(impl->eventLog, L("ESRacGrm"));
    }
  }
  return rc;
}

ESR_ReturnCode SR_RecognizerDeactivateRuleImpl(SR_Recognizer* self, SR_Grammar* grammar,
    const LCHAR* ruleName)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  SR_GrammarImpl* grammarImpl = (SR_GrammarImpl*) grammar;
  LCHAR grammarID[MAX_INT_DIGITS+1];
  ESR_ReturnCode rc;

  if (ruleName == NULL)
  {
    psprintf(grammarID, L("%p"), grammar);
    CHKLOG(rc, HashMapRemove(impl->grammars, grammarID));
  }
  else
    CHKLOG(rc, HashMapRemove(impl->grammars, ruleName));
  grammarImpl->isActivated = ESR_FALSE;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerDeactivateAllRulesImpl(SR_Recognizer* self)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  ESR_ReturnCode rc;

  CHKLOG(rc, HashMapRemoveAll(impl->grammars));
  CA_ClearSyntaxForRecognizer(0, impl->recognizer);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerIsActiveRuleImpl(SR_Recognizer* self, SR_Grammar* grammar,
    const LCHAR* ruleName, ESR_BOOL* isActiveRule)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  LCHAR grammarID[MAX_INT_DIGITS+1];
  ESR_ReturnCode rc;

  psprintf(grammarID, L("%p"), grammar);
  CHKLOG(rc, HashMapContainsKey(impl->grammars, (LCHAR*) &grammarID, isActiveRule));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerSetWordAdditionCeilingImpl(SR_Recognizer* self, SR_Grammar* grammar)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  SR_GrammarImpl* grammarImpl = (SR_GrammarImpl*)grammar;
  int iRc;

  if(!impl || !grammarImpl)
    return ESR_INVALID_ARGUMENT;
  iRc = CA_CeilingSyntaxForRecognizer( grammarImpl->syntax, impl->recognizer);
  if(iRc) return ESR_INVALID_STATE;

  return ESR_SUCCESS;
}

ESR_ReturnCode SR_RecognizerCheckGrammarConsistencyImpl(SR_Recognizer* self, SR_Grammar* grammar,
    ESR_BOOL* isConsistent)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  SR_GrammarImpl* grammarImpl;
  SR_RecognizerImpl* impl2;


  grammarImpl = (SR_GrammarImpl*) grammar;
  impl2 = (SR_RecognizerImpl*)grammarImpl->recognizer;
  // *isConsistent = grammarImpl->models == impl->models;
  *isConsistent = (impl2->models == impl->models);
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_RecognizerGetModelsImpl(SR_Recognizer* self, SR_AcousticModels** pmodels)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  *pmodels = impl->models;
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_RecognizerPutAudioImpl(SR_Recognizer* self, asr_int16_t* buffer, size_t* bufferSize,
    ESR_BOOL isLast)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  ESR_ReturnCode rc;
  int    rcBufWrite;
  size_t nbWritten;

  if (isLast == ESR_FALSE && (buffer == NULL || bufferSize == NULL))
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }

  if (impl->lockFunction)
    impl->lockFunction(ESR_LOCK, impl->lockData);
  if (!impl->isStarted)
  {
    if (impl->lockFunction)
      impl->lockFunction(ESR_UNLOCK, impl->lockData);
    PLogMessage(L("ESR_INVALID_STATE: Tried pushing audio while recognizer was offline"));
    return ESR_INVALID_STATE;
  }
  if (impl->gotLastFrame)
  {
    if (impl->lockFunction)
      impl->lockFunction(ESR_UNLOCK, impl->lockData);
    PLogMessage(L("ESR_INVALID_STATE: isLast=TRUE"));
    return ESR_INVALID_STATE;
  }
  if (buffer == NULL && isLast == ESR_FALSE)
  {
    if (impl->lockFunction)
      impl->lockFunction(ESR_UNLOCK, impl->lockData);
    PLogError(L("ESR_INVALID_ARGUMENT: got NULL  buffer on non-terminal frame"));
    return ESR_INVALID_ARGUMENT;
  }

  rcBufWrite = CircularBufferWrite(impl->buffer, buffer, *bufferSize * SAMPLE_SIZE);
  if (rcBufWrite < 0)
  {
    rc = ESR_INVALID_STATE;
    PLogError(L("%s: error writing to buffer (buffer=%d, available=%u)"), ESR_rc2str(rc), (int) impl->buffer, CircularBufferGetAvailable(impl->buffer));
    goto CLEANUP;
  }

  nbWritten = (size_t)rcBufWrite;
  if (nbWritten % SAMPLE_SIZE != 0)
  {
    size_t amountUnwritten;

    /* The buffer is byte-based while we're sample based. Make sure we write entire samples or not at all */
    amountUnwritten = CircularBufferUnwrite(impl->buffer, nbWritten % SAMPLE_SIZE);
    passert(amountUnwritten == nbWritten % SAMPLE_SIZE);
    nbWritten -= amountUnwritten;
  }
  passert(nbWritten % 2 == 0); /* make sure CircularBufferSize is divisible by 2 */

  if (nbWritten < *bufferSize * SAMPLE_SIZE)
  {
    rc = ESR_BUFFER_OVERFLOW;
#ifndef NDEBUG
    PLOG_DBG_TRACE((L("%s: writing to circular buffer"), ESR_rc2str(rc)));
#endif
    *bufferSize = nbWritten / SAMPLE_SIZE;
    if (impl->lockFunction)
      impl->lockFunction(ESR_UNLOCK, impl->lockData);
    goto CLEANUP;
  }
  if (impl->lockFunction)
    impl->lockFunction(ESR_UNLOCK, impl->lockData);

  if (isLast)
    impl->gotLastFrame = ESR_TRUE;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

/* utility function to sort the ArrayList of nbest list results by the score of the first
   semantic result */
ESR_ReturnCode SemanticResults_SortByScore(ArrayList *results, size_t nbestSize)
{
  ESR_ReturnCode rc;
  ArrayList* semanticResultList;
  ArrayList* semanticResultList_swap;
  SR_SemanticResult* semanticResult_i;
  SR_SemanticResult* semanticResult_j;
  size_t i, j;
  LCHAR scoreStr[MAX_ENTRY_LENGTH] ;
  size_t scoreStrLen = MAX_ENTRY_LENGTH ;
  int score_i, score_j;

  /* bubble sort */
  for (i = 0; i < (size_t)nbestSize; ++i)
  {
    for (j = i + 1; j < (size_t)nbestSize; ++j)
    {
      /* get for i */
      CHKLOG(rc, ArrayListGet(results, i, (void **)&semanticResultList)); /* nbest index */
      CHKLOG(rc, ArrayListGet(semanticResultList, 0, (void **)&semanticResult_i));      /* semresult 0 */

      /* get for j */
      CHKLOG(rc, ArrayListGet(results, j, (void **)&semanticResultList)); /* nbest index */
      CHKLOG(rc, ArrayListGet(semanticResultList, 0, (void **)&semanticResult_j));      /* semresult 0 */

      scoreStrLen = MAX_ENTRY_LENGTH ;
      CHKLOG(rc, semanticResult_i->getValue(semanticResult_i, "raws", scoreStr, &scoreStrLen));
      CHKLOG(rc, lstrtoi(scoreStr, &score_i, 10));
      scoreStrLen = MAX_ENTRY_LENGTH ;
      CHKLOG(rc, semanticResult_j->getValue(semanticResult_j, "raws", scoreStr, &scoreStrLen));
      CHKLOG(rc, lstrtoi(scoreStr, &score_j, 10));

      if (score_j < score_i)
      {
        /* need to swap */
        CHKLOG(rc, ArrayListGet(results, i, (void **)&semanticResultList_swap)); /* put i in swap */
        CHKLOG(rc, ArrayListSet(results, i, semanticResultList));       /* put j in i    */
        CHKLOG(rc, ArrayListSet(results, j, semanticResultList_swap));  /* put swap in j */
      }
    }
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode filter_CA_FullResultLabel(const LCHAR* label, LCHAR *filtered_label, size_t* boss, size_t* eoss)
{
  ESR_ReturnCode rc;
  enum
  {
    NO_COPY,
    FRAME,
    WORD,
  } filter_state = WORD;
  LCHAR *dst = filtered_label;
  LCHAR eosBuf[16]; /* max 9999 + '\0' */
  LCHAR bosBuf[16]; /* max 9999 + '\0' */
  LCHAR* pBuf = NULL;

  /**
   * example: you want to filter this:
   *
   * "-pau-@23 clock@97 twenty_four@125 hour@145  "
   *        ^boss = 23                       ^ eoss = 145
   * and get this:
   *
   * "clock twenty_four hour"
   */

  passert(LSTRLEN(label) > 0);
  while (*label)
  {
    switch (filter_state)
    {
      case NO_COPY:
        if (*label == L(' '))
          filter_state = WORD;
        else if (*label == L('@'))
        {
          filter_state = FRAME;
          if (pBuf == NULL)
            pBuf = bosBuf;
          else
          {
            *pBuf = 0;
            pBuf = eosBuf;
          }
        }
        break;
      case WORD:
        if (*label == L('@'))
        {
          *dst = L(' '); /* insert space */
          dst++;
          filter_state = FRAME;
          if (pBuf == NULL)
            pBuf = bosBuf;
          else
          {
            *pBuf = 0;
            pBuf = eosBuf;
          }
        }
        else
        {
          *dst = *label;
          dst++;
        }
        break;
      case FRAME:
        if (*label == L(' '))
          filter_state = WORD;
        else
        {
          *pBuf = *label;
          pBuf++;
        }
        break;
    }
    label++;
  }
  *dst = 0; /* term the string */
  *pBuf = 0; /* term the string */

  /* trim the end spaces */
  dst--;
  while (*dst == ' ')
    *dst-- = '\0';

  /* set the eos signal indicated by the end pointed data */
  if (eosBuf[0] != 0)
    CHKLOG(rc, lstrtoui(eosBuf, eoss, 10));
  else
    eoss = 0;

  if (bosBuf[0] != 0)
    CHKLOG(rc, lstrtoui(bosBuf, boss, 10));
  else
    boss = 0;

  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

/**
 * Populates the recognizer result if it can, otherwise it returns NO MATCH cuz no results exist
 *
 * INPUT STATE: SR_RECOGNIZER_INTERNAL_EOS
 *
 * @param self SR_Recognizer handle
 * @todo break up into smaller functions
 */
ESR_ReturnCode SR_RecognizerCreateResultImpl(SR_Recognizer* self, SR_RecognizerStatus* status,
    SR_RecognizerResultType* type)
{
  LCHAR label[MAX_ENTRY_LENGTH * 2];  /* run out of buffer */
#define WORDID_COUNT 48 /* can be quite high for voice enrollment! */
  wordID wordIDs[WORDID_COUNT];
  LCHAR tok[80];
  LCHAR waveformFilename[P_PATH_MAX];
  LCHAR* pkey;
  SR_GrammarImpl* pgrammar;
  asr_int32_t raws; /* raw score */
  size_t iBest, nbestSize, jBest, k, grammarSize, semanticResultsSize, grammarIndex_for_iBest;
  LCHAR* lValue;
  LCHAR* lValue2;
  int confValue;
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  SR_RecognizerResultImpl* resultImpl = (SR_RecognizerResultImpl*) impl->result;
  ESR_BOOL containsKey;
  int valid, score, recogID;
  LCHAR result[MAX_ENTRY_LENGTH];
  size_t len, size;
  size_t locale;
  int current_choice;

  /**
   * Semantic result stuff
   */
  /* a temp buffer to hold semantic results of a parse (there may be several results) */
  SR_SemanticResult* semanticResults[MAX_SEM_RESULTS];
  ArrayList* semanticList;
  ArrayList* semanticList2;
  SR_SemanticResultImpl* semanticImpl;
  SR_SemanticResultImpl* semanticImpl2;
  SR_SemanticResult* semanticResult;
  SR_SemanticResult* semanticResult2;
  waveform_buffering_state_t buffering_state;

  SR_AcousticModelsImpl* modelsImpl = (SR_AcousticModelsImpl*) impl->models;
  ESR_ReturnCode rc;
  PTimeStamp EORT;

  CA_LockUtteranceFromInput(impl->utterance);
  if (!CA_EndRecognition(impl->recognizer, modelsImpl->pattern, impl->utterance))
  {
    PLogError(L("ESR_INVALID_STATE"));
    return ESR_INVALID_STATE;
  }

  /* check if the forward search was successful */
  valid = CA_FullResultLabel(impl->recognizer, result, MAX_ENTRY_LENGTH - 1);
  CA_GetRecogID(impl->recognizer, &recogID);
  CA_FullResultScore(impl->recognizer, &score, 1);
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage(L("R: %s type %d score %d from recognizer%d"), result, type, score, valid, recogID);
  PLogMessage(L("R: %s score %d from recognizer%d"), result, score, valid, recogID);
#endif
#ifdef _WIN32
  //pfprintf(PSTDOUT, ("R: %s type %d score %d from recognizer%d\n"), result, type, score, valid, recogID);
#endif


  switch (valid)
  {
    case FULL_RESULT:
      CHKLOG(rc, filter_CA_FullResultLabel(result, label, &impl->recogLogTimings.BOSS, &impl->recogLogTimings.EOSS));
#ifdef SREC_ENGINE_VERBOSE_LOGGING
      PLogMessage("R: %s", result);
#endif
      CA_FullResultScore(impl->recognizer, (int*) &raws, 0);
#ifdef SREC_ENGINE_VERBOSE_LOGGING
      PLogMessage("S: %d", raws);
#endif

      /* now that we have an endpointed result, we can parse the result transcription
         to see where speech started and ended. Then we can trim off excess parts of the
         recorded audio waveform (if exists) so that nametags are just the right amount of
         audio
      */
      CHKLOG(rc, WaveformBuffer_GetBufferingState(impl->waveformBuffer, &buffering_state));
      if (buffering_state != WAVEFORM_BUFFERING_OFF)
      {
        CHKLOG(rc, WaveformBuffer_GetSize(impl->waveformBuffer, &size));
        if (size > 0)
        {
          rc = WaveformBuffer_ParseEndPointedResultAndTrim(impl->waveformBuffer, result, impl->FRAME_SIZE);
          if (rc == ESR_BUFFER_OVERFLOW)
          {
            /* Nametag EOS occured beyond end of buffer */
          }
          else if (rc != ESR_SUCCESS)
          {
            PLogError(ESR_rc2str(rc));
            goto CLEANUP;
          }
        }
      }
      break;

    case REJECT_RESULT:
#ifdef SREC_ENGINE_VERBOSE_LOGGING
      PLogMessage(L("R: <REJECTED>"));
#endif
      break;
    default:
#ifdef SREC_ENGINE_VERBOSE_LOGGING
      PLogMessage(L("E: No results available"));
      PLogMessage(L("R: <FAILED>"));
#endif
      break;
  }


  if (valid == FULL_RESULT)
  {
    /* Populate SR_RecognizerResult */
    resultImpl->nbestList = CA_PrepareNBestList(impl->recognizer, 10, &raws);
    if (resultImpl->nbestList == NULL)
    {
      /*
       * This is not a failure. It simply means that I have not advanced far
       * enough in recognition in order to obtain results (no paths in
       * graph). This occurs, for instance, when a eof is reached (no more data)
       * and I have not even created any paths in my graph.
       */

      *status = SR_RECOGNIZER_EVENT_NO_MATCH;
      *type = SR_RECOGNIZER_RESULT_TYPE_COMPLETE;
      impl->internalState = SR_RECOGNIZER_INTERNAL_END;
      if (impl->eventLog != NULL)
      {
        CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("SR_RecognizerCreateResultImpl() -> SR_RECOGNIZER_INTERNAL_END")));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
        CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
      }
      passert(0);
      return ESR_SUCCESS;
    }

    nbestSize = CA_NBestListCount(resultImpl->nbestList);
  }
  else
    nbestSize = 0;

  if (resultImpl->results != NULL)
    ArrayListRemoveAll(resultImpl->results);
  else
    CHKLOG(rc, ArrayListCreate(&resultImpl->results));
  if (nbestSize == 0)
  {
    /*
     * Got empty n-best list even though the recognition was successful.
     * We handle this in the same way that recog_startpt does... we consider it a no match.
     * We could adjust the CREC.Recognizer.viterbi_prune_thresh to a higher level, but that
     * may not fix the problem completely. We need to fix the bug in the astar search!!!
     */
    *status = SR_RECOGNIZER_EVENT_NO_MATCH;
    *type = SR_RECOGNIZER_RESULT_TYPE_COMPLETE;
    impl->internalState = SR_RECOGNIZER_INTERNAL_END;
    if (impl->eventLog != NULL)
    {
      CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("SR_RecognizerCreateResultImpl() -> SR_RECOGNIZER_INTERNAL_END")));
      CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
      CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
      CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
    }
#ifdef SREC_ENGINE_VERBOSE_LOGGING
    PLogMessage(L("ESR_INVALID_STATE: got empty n-best list even though the recognition was successful"));
#endif
    return ESR_SUCCESS; /* we do not want to halt the app in this case */
  }
  else
  {
    *status = SR_RECOGNIZER_EVENT_RECOGNITION_RESULT;
    *type = SR_RECOGNIZER_RESULT_TYPE_COMPLETE;
    impl->internalState = SR_RECOGNIZER_INTERNAL_END;
    if (impl->eventLog != NULL)
    {
      CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("SR_RecognizerCreateResultImpl() -> SR_RECOGNIZER_INTERNAL_END")));
      CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
      CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
      CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
    }
  }

  /**
   * All grammars associated with the recognizer are considered to be active
   * and therefore, I do a semantic parse on each. On the first grammar that
   * gives one or more semantic results, I stop parsing the other grammars.
   */
  CHKLOG(rc, impl->grammars->getSize(impl->grammars, &grammarSize));
  ASSERT( grammarSize == 1);

  for (iBest = 0; iBest < nbestSize; ++iBest)
  {
    len = WORDID_COUNT;
    if (CA_NBestListGetResultWordIDs(resultImpl->nbestList, iBest, wordIDs, &len, &raws) != ESR_SUCCESS)
    {
      *status = SR_RECOGNIZER_EVENT_NO_MATCH;
      *type = SR_RECOGNIZER_RESULT_TYPE_COMPLETE;
      impl->internalState = SR_RECOGNIZER_INTERNAL_END;
      if (impl->eventLog != NULL)
      {
        CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("SR_RecognizerCreateResultImpl() -> SR_RECOGNIZER_INTERNAL_END")));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
        CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
      }
      PLogError(L("ESR_INVALID_STATE: got bad n-best list entry %d"), iBest);
      return ESR_INVALID_STATE;
    }

    CHKLOG(rc, ArrayListCreate(&semanticList));
    CHKLOG(rc, resultImpl->results->add(resultImpl->results, semanticList));

    grammarIndex_for_iBest = 0;
    CHKLOG(rc, impl->grammars->getKeyAtIndex(impl->grammars, grammarIndex_for_iBest, &pkey));
    CHKLOG(rc, impl->grammars->get(impl->grammars, pkey, (void **)&pgrammar));

    CHKLOG(rc, SR_GrammarGetSize_tParameter((SR_Grammar*) pgrammar, L("locale"), &locale));
    resultImpl->locale = locale;

    /* I need to manage my semantic results external to the check parse function */
    for (k = 0; k < MAX_SEM_RESULTS; ++k)
      SR_SemanticResultCreate(&semanticResults[k]);

    /*
       The code here tries to make the voice-enrollment more effective.
       The VE grammar decodes a sequence of best phonemes, but the nbest
       processing may find a better score for an alternative choice than
       the score of the viterbi best choice.  The reason for this is that
       alternative choices don't honor cross-word context-dependency quite
       accurately.  If we choose an alternative choice then the sequence of
       phoneme decoded does not correspond to the sequence of models decoded.
       To counter this, we FORCIBLY make sure the top choice here is the
       VITERBI top choice.
    */

    if (iBest == 0)
      {
        if (CA_IsEnrollmentSyntax( pgrammar->syntax)) {
          /* this was voice enrollment, so let's try to replace */
          // 	char* word1 = CA_NBestListGetResultWord(resultImpl->nbestList,wordIDs[0]);
          // char* word2 = CA_NBestListGetResultWord(resultImpl->nbestList,wordIDs[1]);
          // if (!strncmp(word1,voice_enroll_word_prefix,VEWPLEN)&&!strncmp(word2,voice_enroll_word_prefix,VEWPLEN))
          len = WORDID_COUNT;
          rc = CA_FullResultWordIDs(impl->recognizer, wordIDs, &len);
          if (rc != ESR_SUCCESS)
            {
              /* in case of problem with viterbi path choice, we revert back */
              len = WORDID_COUNT;
              rc = CA_NBestListGetResultWordIDs(resultImpl->nbestList, iBest, wordIDs, &len, &raws) ;
            }
        }
      }

    LSTRCPY(label, L(""));
    for (k = 0; wordIDs[k] != MAXwordID; ++k)
      {
        LCHAR* wordk = NULL;
        wordk = CA_NBestListGetResultWord(resultImpl->nbestList,wordIDs[k]);
        LSTRCAT(label, wordk);
        LSTRCAT(label, L(" "));
      }
    CHKLOG(rc, CA_ResultStripSlotMarkers(label));
    passert(LSTRCMP(label, L("")) != 0);

    /* strip the trailing blank */
    k = LSTRLEN(label) - 1;
    if (k > 0 && label[k] == L(' '))
      label[k] = 0;

    semanticResultsSize = MAX_SEM_RESULTS;

#if SEMPROC_ACTIVE

    /* set the literal prior to processing so that semproc can read the value
       during processing */
    CHKLOG(rc, pgrammar->semproc->flush(pgrammar->semproc));
    CHKLOG(rc, pgrammar->semproc->setParam(pgrammar->semproc, L("literal"), label));

    rc = pgrammar->semproc->checkParseByWordID(pgrammar->semproc, pgrammar->semgraph,
                                               wordIDs, semanticResults, &semanticResultsSize);

    /* rc = pgrammar->semproc->checkParse(pgrammar->semproc, pgrammar->semgraph,
       label, semanticResults, &semanticResultsSize); */

    if (rc != ESR_SUCCESS)
      {
        for (k = 0; k < MAX_SEM_RESULTS; ++k)
          {
            semanticResults[k]->destroy(semanticResults[k]);
            semanticResults[k] = NULL;
          }
        goto CLEANUP;
      }
#else
    semanticResultsSize = 0;
#endif
    /* cleanup the empty ones */
    for (k = semanticResultsSize; k < MAX_SEM_RESULTS; ++k)
      {
        CHKLOG(rc, semanticResults[k]->destroy(semanticResults[k]));
        semanticResults[k] = NULL;
      }

    /* save the good ones */
    for (k = 0; k < semanticResultsSize; ++k)
      {
        /*
         * Save the pointer to the semantic result that was created.
         * Remember that the semantic result array only holds pointers
         * and for each time that the function is called, new semantic results
         * are created, and the pointers overwrite old values in the array
         */
        CHKLOG(rc, semanticList->add(semanticList, semanticResults[k]));
      }

#if SEMPROC_ACTIVE
    if (semanticResultsSize > 0)
      {
        /* OSI log the grammar(s) that was used in recognizing */
        psprintf(tok, L("GURI%d"), grammarIndex_for_iBest);
        CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("GRMR"), tok));
      }
#else
    /* OSI log the grammar(s) that was used in recognizing */
    psprintf(tok, L("GURI%d"), grammarIndex_for_iBest);
    CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("GRMR"), tok));
#endif

    /* Populate semantic results for each nbest list entry */
    CHKLOG(rc, semanticList->getSize(semanticList, &semanticResultsSize));
    if (semanticResultsSize == 0)
    {
      /*
       * If there was no semantic result... then I need to create one so that I can store
       * literal, conf, meaning which are default keys that must ALWAYS exist
       */
      CHKLOG(rc, SR_SemanticResultCreate(&semanticResult));
      CHKLOG(rc, semanticList->add(semanticList, semanticResult));
      semanticResultsSize = 1;
    }

    for (k = 0; k < semanticResultsSize;++k)
    {
      CHKLOG(rc, semanticList->get(semanticList, k, (void **)&semanticResult));
      if (semanticResult == NULL)
      {
        PLogError(L("nbest entry contained NULL semanticResult"), ESR_INVALID_STATE);
        return ESR_INVALID_STATE;
      }

      semanticImpl = (SR_SemanticResultImpl*) semanticResult;

      /* put in the literal */
      lValue = MALLOC(sizeof(LCHAR) * (LSTRLEN(label) + 1), MTAG);
      if (lValue == NULL)
      {
        PLogError(L("ESR_OUT_OF_MEMORY"));
        return ESR_OUT_OF_MEMORY;
      }
      LSTRCPY(lValue, label);
      CHKLOG(rc, semanticImpl->results->put(semanticImpl->results, L("literal"), lValue));

      /* if the meaning is not set, then put in the meaning which will be the literal */
      CHKLOG(rc, semanticImpl->results->containsKey(semanticImpl->results, L("meaning"), &containsKey));
      if (!containsKey)
      {
        lValue = MALLOC(sizeof(LCHAR) * (LSTRLEN(label) + 1), MTAG);
        if (lValue == NULL)
        {
          PLogError(L("ESR_OUT_OF_MEMORY"));
          return ESR_OUT_OF_MEMORY;
        }
        LSTRCPY(lValue, label);
        CHKLOG(rc, semanticImpl->results->put(semanticImpl->results, L("meaning"), lValue));
      }

      /* put in the raw score */
      psprintf(label, L("%d"), raws);
      lValue = MALLOC(sizeof(LCHAR) * (LSTRLEN(label) + 1), MTAG);
      if (lValue == NULL)
      {
        PLogError(L("ESR_OUT_OF_MEMORY"));
        return ESR_OUT_OF_MEMORY;
      }
      LSTRCPY(lValue, label);
      CHKLOG(rc, semanticImpl->results->put(semanticImpl->results, L("raws"), lValue));
    }
  }

  /* Now I have an nBest list where each entry has at least one semantic result */
  /* What I need to do is filter out the nBest list entries which have matching
     semantic results for 'meaning' */
  /* Once I have filtered out the nBest list based on this criteria, I can calculate the confidence
     score and populate the result of the first entry with the raw score */

#if FILTER_NBEST_BY_SEM_RESULT

  for (iBest = nbestSize-1; iBest>0; iBest--) /* do not filter out nBest entry 0 */
  {
    /**
     * This is the entry (indexed by i) targeted for removal
     *
     */

    /* get the nBest entry which you wish to remove (if duplicate found) */
    CHKLOG(rc, ArrayListGet(resultImpl->results, iBest, (void **)&semanticList));

    /* get the first sem_result for the entry */
    CHKLOG(rc, ArrayListGet(semanticList, 0, (void **)&semanticResult));
    semanticImpl = (SR_SemanticResultImpl*) semanticResult;

    /* get the meaning */
    CHKLOG(rc, semanticImpl->results->get(semanticImpl->results, L("meaning"), (void **)&lValue));

    /* get the other entries to check against (start with 0, end on the current i entry) */
    for (jBest = 0; jBest < iBest; ++jBest)
    {
      /*
       * This is the entry (indexed by jBest) that we will compare with
       */

      /* get the nBest entry which you wish to compare with */
      CHKLOG(rc, ArrayListGet(resultImpl->results, jBest, (void **)&semanticList2));

      CHKLOG(rc, ArrayListGet(semanticList2, 0, (void **)&semanticResult2));
      semanticImpl2 = (SR_SemanticResultImpl*) semanticResult2;

      CHKLOG(rc, semanticImpl2->results->get(semanticImpl2->results, L("meaning"), (void **)&lValue2));
      if (LSTRCMP(lValue, lValue2) == 0)
      {
        /* pfprintf(PSTDOUT,"duplicate sem result found %d == %d\n", iBest, jBest);
        pfprintf(PSTDOUT,"removing %d\n", iBest); */

        /* removing from the list indexed by iBest */
        CHKLOG(rc, semanticList->remove(semanticList, semanticResult));
        CHKLOG(rc, semanticResult->destroy(semanticResult));

        CHKLOG(rc, resultImpl->results->remove(resultImpl->results, semanticList));
        CHKLOG(rc, semanticList->destroy(semanticList));

        if (!CA_NBestListRemoveResult(resultImpl->nbestList, iBest))
          return ESR_ARGUMENT_OUT_OF_BOUNDS;
        break;
      }
    }
  }
  nbestSize = CA_NBestListCount(resultImpl->nbestList);
#endif

  CHKLOG(rc, ArrayListGetSize(resultImpl->results, &nbestSize));

  if (nbestSize)
  {
   if(CA_ComputeConfidenceValues(impl->confidenceScorer, impl->recognizer, resultImpl->nbestList))
        return ESR_INVALID_STATE;

   for(current_choice=nbestSize-1;current_choice>=0;current_choice--)
   {
    /* get the nBest entry you want to deal with */
    CHKLOG(rc, ArrayListGet(resultImpl->results, current_choice, (void **)&semanticList));
    /* get the first sem_result for that entry */
    CHKLOG(rc, ArrayListGet(semanticList, 0, (void **)&semanticResult));
    semanticImpl = (SR_SemanticResultImpl*) semanticResult;

    /* put in the conf value for that nBest entry */
    if(!CA_NBestListGetResultConfidenceValue( resultImpl->nbestList, current_choice, &confValue))
      return ESR_ARGUMENT_OUT_OF_BOUNDS;

    psprintf(label, L("%d"), confValue);
    lValue = MALLOC(sizeof(LCHAR) * (LSTRLEN(label) + 1), MTAG);
      if (lValue == NULL)
      {
        PLogError(L("ESR_OUT_OF_MEMORY"));
        return ESR_OUT_OF_MEMORY;
      }
      LSTRCPY(lValue, label);
      CHKLOG(rc, semanticImpl->results->put(semanticImpl->results, L("conf"),lValue));
    }
  CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->osi_log_level, L("CMPT"), 0));
  }

  /* OSI log the end of recognition and all bufferred tokens */

  /* OSI log end of recognition time */
  PTimeStampSet(&EORT);
  impl->recogLogTimings.EORT = PTimeStampDiff(&EORT, &impl->timestamp);
  impl->recogLogTimings.DURS = impl->processed * MSEC_PER_FRAME;

  /*****************************************/
  /* OSI Logging stuff */
  /*****************************************/
if( impl->osi_log_level != 0)
 {
  /* get the nBest size (this size may have changed since previous set cuz of nbest list filtering) */
  CHKLOG(rc, ArrayListGetSize(resultImpl->results, &nbestSize));
  /* OSI log the nBest list size */
  CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->osi_log_level, L("NBST"), nbestSize));


  for (iBest = 0; iBest < nbestSize; iBest++) /* loop */
  {
    /* get the nBest entry */
    CHKLOG(rc, ArrayListGet(resultImpl->results, iBest, (void**)&semanticList));

    /* get the first sem_result for the entry (ther emay be many, but ignore others) */
    CHKLOG(rc, ArrayListGet(semanticList, 0, (void **)&semanticResult));
    semanticImpl = (SR_SemanticResultImpl*) semanticResult;

    /* get the meaning and OSI log it */
    CHKLOG(rc, semanticImpl->results->get(semanticImpl->results, L("meaning"), (void **)&lValue));
    /* OSI log RSLT (meaning) for nbest item */
    CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("RSLT"), lValue));

    /* get the literal and OSI log it */
    CHKLOG(rc, semanticImpl->results->get(semanticImpl->results, L("literal"), (void **)&lValue));
    /* OSI log RAWT SPOK (literal) for nbest item */
    CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("RAWT"), lValue));
    CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("SPOK"), lValue));

    /* get the score and OSI log it */
    CHKLOG(rc, semanticImpl->results->get(semanticImpl->results, L("raws"), (void **)&lValue));
    /* OSI log RAWS (score) for nbest item */
    CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("RAWS"), lValue));
    /* get the confidence value and OSI log it */
    CHKLOG(rc, semanticImpl->results->get(semanticImpl->results, L("conf"), (void **)&lValue));
    /* OSI log CONF (values) for nbest item */
    CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("CONF"), lValue));
  }

  /* log the values */
  CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->osi_log_level, L("BORT"), impl->recogLogTimings.BORT));
  CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->osi_log_level, L("DURS"), impl->recogLogTimings.DURS));
  CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->osi_log_level, L("EORT"), impl->recogLogTimings.EORT));
  CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->osi_log_level, L("EOSD"), impl->recogLogTimings.EOSD));
  CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->osi_log_level, L("EOSS"), impl->recogLogTimings.EOSS));
  CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->osi_log_level, L("EOST"), impl->recogLogTimings.EOST));
  if (impl->osi_log_level & OSI_LOG_LEVEL_AUDIO)
  {
    len = P_PATH_MAX;
    CHKLOG(rc, SR_EventLogAudioGetFilename(impl->eventLog, waveformFilename, &len));
    CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("WVNM"), waveformFilename));
  }
  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("RSTT"), L("ok")));
  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("RENR"), L("ok")));
  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("ENDR"), impl->eos_reason));
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SWIrcnd")));

  CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->osi_log_level, L("BOSS"), impl->recogLogTimings.BOSS)); /* extra not in OSI spec */
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("ESRboss")));

  /*
   * Record which recognizer was the successful one (male or female)
   * this index refers to the order in the swimdllist file.
   */
  CHKLOG(rc, CA_GetRecogID(impl->recognizer, &recogID));
  CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->osi_log_level, L("RECOG"), recogID));
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("ESRrcid")));

  /* Record semantic results returned by top nbestlist entry */
  if (1)
  {
#define MAX_SEMANTIC_KEYS 50
    LCHAR* semanticKeys[MAX_SEMANTIC_KEYS];
#define SEMANTIC_VALUE_SIZE 512
    LCHAR semanticValue[SEMANTIC_VALUE_SIZE];
    size_t num_semanticKeys;

    rc = resultImpl->results->getSize(resultImpl->results, &nbestSize);
    if (rc != ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      goto DONE;
    }
    for (iBest = 0; iBest < nbestSize; ++iBest) /* loop2 */
    {
      rc = resultImpl->results->get(resultImpl->results, iBest, (void **)&semanticList);
      if (rc != ESR_SUCCESS)
      {
        PLogError(ESR_rc2str(rc));
        goto DONE;
      }

	  /* semanticResultsSize is the number of semantic meanings, although
		 ambiguous parses are not entirely supported
		 num_semanticKeys    is associated to a particular parse         */

      rc = semanticList->getSize(semanticList, &semanticResultsSize);
      if (rc != ESR_SUCCESS)
      {
        PLogError(ESR_rc2str(rc));
        goto DONE;
      }
      for (k = 0; k < semanticResultsSize; ++k)
      {
		size_t iKey;
        rc = semanticList->get(semanticList, k, (void **)&semanticResult);
        if (rc != ESR_SUCCESS)
        {
          PLogError(ESR_rc2str(rc));
          goto DONE;
        }
        num_semanticKeys = MAX_SEMANTIC_KEYS;
        rc = semanticResult->getKeyList(semanticResult, (LCHAR**) & semanticKeys, &num_semanticKeys);
        if (rc != ESR_SUCCESS)
        {
          PLogError(ESR_rc2str(rc));
          goto DONE;
        }

        for (iKey=0; iKey<num_semanticKeys; ++iKey)
        {
          len = SEMANTIC_VALUE_SIZE;
          rc = semanticResult->getValue(semanticResult, semanticKeys[iKey], (LCHAR*) &semanticValue, &len);
          if (rc != ESR_SUCCESS)
          {
            PLogError(ESR_rc2str(rc));
            goto DONE;
          }

          rc = SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, semanticKeys[iKey], semanticValue);
          if (rc != ESR_SUCCESS)
          {
            PLogError(ESR_rc2str(rc));
            goto DONE;
          }
        }
      }
    }
    rc = SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("ESR_SemanticResult[0]"));
    if (rc != ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      goto DONE;
    }
  }
}
DONE:
  return ESR_SUCCESS;
CLEANUP:
  impl->internalState = SR_RECOGNIZER_INTERNAL_END;
  return rc;
}

/**
 * Indicates if it is possible to push data from SREC into the internal recognizer.
 * If data can be pushed, ESR_CONTINUE_PROCESSING is returned.
 *
 * INPUT STATES: SR_RECOGNIZER_INTERNAL_BOS_DETECTION, SR_RECOGNIZER_INTERNAL_EOS_DETECTION
 * OUTPUT STATES: same or SR_RECOGNIZER_INTERNAL_EOI
 */
PINLINE ESR_ReturnCode canPushAudioIntoRecognizer(SR_RecognizerImpl* impl)
{
  ESR_ReturnCode rc;

  if (impl->lockFunction)
    impl->lockFunction(ESR_LOCK, impl->lockData);

  /* do I have enough to make a frame ? */
  if (CircularBufferGetSize(impl->buffer) < impl->FRAME_SIZE)
  {
    /* Not enough data */
    if (!impl->gotLastFrame)
    {
      /* not last frame, so ask for more audio */
      if (impl->lockFunction)
        impl->lockFunction(ESR_UNLOCK, impl->lockData);
      return ESR_SUCCESS;
    }
    else
    {
      /* last frame, make do with what you have */
      if (impl->lockFunction)
        impl->lockFunction(ESR_UNLOCK, impl->lockData);
#ifdef SREC_ENGINE_VERBOSE_LOGGING
      PLogMessage("L: Voicing END (EOI) at %d frames (%d processed)", impl->frames, impl->processed);
#endif
      impl->isRecognizing = ESR_FALSE;
      impl->recogLogTimings.EOSD = impl->frames;
      impl->eos_reason = L("EOI");
      impl->internalState = SR_RECOGNIZER_INTERNAL_EOI;
      if (impl->eventLog != NULL)
      {
        CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("canPushAudioIntoRecognizer() -> SR_RECOGNIZER_INTERNAL_EOI")));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
        CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
      }
      return ESR_CONTINUE_PROCESSING;
    }
  }
  if (impl->lockFunction)
    impl->lockFunction(ESR_UNLOCK, impl->lockData);
  return ESR_CONTINUE_PROCESSING;
CLEANUP:
  return rc;
}

/**
 * Pushes data from SREC into the internal recognizer.
 *
 * INPUT STATES: SR_RECOGNIZER_INTERNAL_BOS_DETECTION, SR_RECOGNIZER_INTERNAL_EOS_DETECTION
 * OUTPUT STATES: same
 */
PINLINE ESR_ReturnCode pushAudioIntoRecognizer(SR_RecognizerImpl* impl, SR_RecognizerStatus* status,
    SR_RecognizerResultType* type,
    SR_RecognizerResult* result)
{
  size_t count;
  ESR_ReturnCode rc;

  if (CA_GetUnprocessedFramesInUtterance(impl->utterance) > 0 && impl->frames >= impl->bgsniff)
  {
    /* Don't push frames unless they're needed */

    /* Check for leaked state */
    passert(*status == SR_RECOGNIZER_EVENT_INVALID && *type == SR_RECOGNIZER_RESULT_TYPE_INVALID);
    return ESR_CONTINUE_PROCESSING;
  }
  if (impl->lockFunction)
    impl->lockFunction(ESR_LOCK, impl->lockData);
  count = CircularBufferRead(impl->buffer, impl->audioBuffer, impl->FRAME_SIZE);
  if (impl->lockFunction)
    impl->lockFunction(ESR_UNLOCK, impl->lockData);

  WaveformBuffer_Write(impl->waveformBuffer, impl->audioBuffer, count);
  if (impl->osi_log_level & OSI_LOG_LEVEL_AUDIO)
  {
    rc = SR_EventLogAudioWrite(impl->eventLog, impl->audioBuffer, count);
    if (rc == ESR_BUFFER_OVERFLOW)
      rc = ESR_INVALID_STATE;
    if (rc != ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      if (impl->lockFunction)
        impl->lockFunction(ESR_UNLOCK, impl->lockData);
      goto CLEANUP;
    }
  }
  if (count < impl->FRAME_SIZE)
  {
    rc = ESR_INVALID_STATE;
    PLogError(L("%s: error reading buffer data (count=%d, frameSize=%d)"), ESR_rc2str(rc), count, impl->FRAME_SIZE);
    goto CLEANUP;
  }
  if (!CA_LoadSamples(impl->wavein, impl->audioBuffer, impl->sampleRate / FRAMERATE))
  {
    PLogError(L("ESR_INVALID_STATE"));
    rc = ESR_INVALID_STATE;
    goto CLEANUP;
  }

  CA_ConditionSamples(impl->wavein);
  /* Check for leaked state */
  passert(*status == SR_RECOGNIZER_EVENT_INVALID && *type == SR_RECOGNIZER_RESULT_TYPE_INVALID);
  return ESR_CONTINUE_PROCESSING;
CLEANUP:
  return rc;
}

/**
 * INPUT STATES: SR_RECOGNIZER_INTERNAL_BOS_DETECTION, SR_RECOGNIZER_INTERNAL_EOS_DETECTION
 * OUTPUT STATES: same
 */
PINLINE ESR_ReturnCode generateFrameFromAudio(SR_RecognizerImpl* impl, SR_RecognizerStatus* status,
    SR_RecognizerResultType* type,
    SR_RecognizerResult* result)
{
  if (CA_GetUnprocessedFramesInUtterance(impl->utterance) > 0 && impl->frames >= impl->bgsniff)
  {
    /* Don't create frames unless they're needed */

    /* Check for leaked state */
    passert(*status == SR_RECOGNIZER_EVENT_INVALID && *type == SR_RECOGNIZER_RESULT_TYPE_INVALID);
    return ESR_CONTINUE_PROCESSING;
  }

  /* Try processing one frame */
  if (!CA_MakeFrame(impl->frontend, impl->utterance, impl->wavein))
  {
    /*
    * One of three cases occured:
    *
    * - We don't have enough samples to process one frame. This should be impossible because
    * pushAudioIntoRecognizer() is always called before us and will not continue if we don't
    * have enough samples.
    *
    * - The internal recognizer needs a minimum amount of audio before it'll begin generating
    *   frames. This is normal and we return with a success value.
    *
    * - The recognizer skips every even frame number (for performance reasons). This is normal
    *   and we return with a success value.
    */
    *status = SR_RECOGNIZER_EVENT_INCOMPLETE;
    *type = SR_RECOGNIZER_RESULT_TYPE_NONE;
    return ESR_SUCCESS;
  }
  ++impl->frames;
  /* Check for leaked state */
  passert(*status == SR_RECOGNIZER_EVENT_INVALID && *type == SR_RECOGNIZER_RESULT_TYPE_INVALID);
  return ESR_CONTINUE_PROCESSING;
}

/**
 * INPUT STATES: SR_RECOGNIZER_INTERNAL_EOS_DETECTION
 * OUTPUT STATES: same
 */
PINLINE ESR_ReturnCode generateFrameStats(SR_RecognizerImpl* impl, SR_RecognizerStatus* status,
                           SR_RecognizerResultType* type,
                           SR_RecognizerResult* result)
{
  if (impl->frames < impl->bgsniff)
  {
    /* Wait until we have enough frames to estimate background stats */
    *status = SR_RECOGNIZER_EVENT_INCOMPLETE;
    *type = SR_RECOGNIZER_RESULT_TYPE_NONE;
    return ESR_SUCCESS;
  }
  else if (impl->frames == impl->bgsniff)
    CA_CalculateUtteranceStatistics(impl->utterance, 0, impl->bgsniff);

  /* Check for leaked state */
  passert(*status == SR_RECOGNIZER_EVENT_INVALID && *type == SR_RECOGNIZER_RESULT_TYPE_INVALID);
  return ESR_CONTINUE_PROCESSING;
}

/**
 * INPUT STATES: SR_RECOGNIZER_INTERNAL_EOS_DETECTION
 * OUTPUT STATES: same or SR_RECOGNIZER_INTERNAL_EOI, SR_RECOGNIZER_INTERNAL_EOS
 */
PINLINE ESR_ReturnCode generatePatternFromFrame(SR_RecognizerImpl* impl, SR_RecognizerStatus* status,
    SR_RecognizerResultType* type,
    SR_RecognizerResult* result)
{
  SR_AcousticModelsImpl* modelsImpl;
  ESR_ReturnCode rc;

  /* Run the search */
  modelsImpl = (SR_AcousticModelsImpl*) impl->models;
  if (!CA_MakePatternFrame(modelsImpl->pattern, impl->utterance))
  {
    *status = SR_RECOGNIZER_EVENT_NO_MATCH;
    *type = SR_RECOGNIZER_RESULT_TYPE_COMPLETE;
    impl->internalState = SR_RECOGNIZER_INTERNAL_END;
    if (impl->eventLog != NULL)
    {
      CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("generatePatternFromFrame() -> SR_RECOGNIZER_INTERNAL_END")));
      CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
      CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
      CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
    }
    PLogError(L("ESR_INVALID_STATE"));
    return ESR_INVALID_STATE;
  }
  if (!CA_AdvanceUtteranceFrame(impl->utterance))
  {
    *status = SR_RECOGNIZER_EVENT_NO_MATCH;
    *type = SR_RECOGNIZER_RESULT_TYPE_COMPLETE;
    impl->internalState = SR_RECOGNIZER_INTERNAL_END;
    if (impl->eventLog != NULL)
    {
      CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("canPushAudioIntoRecognizer() -> SR_RECOGNIZER_INTERNAL_END")));
      CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
      CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
      CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
    }
    PLogError(L("ESR_INVALID_STATE"));
    return ESR_INVALID_STATE;
  }
  CA_AdvanceRecognitionByFrame(impl->recognizer, modelsImpl->pattern, impl->utterance);
  ++impl->processed;

  if (impl->lockFunction)
    impl->lockFunction(ESR_LOCK, impl->lockData);
  if (impl->gotLastFrame && CircularBufferGetSize(impl->buffer) < impl->FRAME_SIZE)
  {
    /*
     * SREC have run out of data but the underlying recognizer might have some frames
     * queued for processing.
     */
    if (CA_GetUnprocessedFramesInUtterance(impl->utterance) > 0)
    {
      /* EOI means end of input */
#ifdef SREC_ENGINE_VERBOSE_LOGGING
      PLogMessage("L: Voicing END (EOI) at %d frames (%d processed)", impl->frames, impl->processed);
#endif
      impl->isRecognizing = ESR_FALSE;
      impl->recogLogTimings.EOSD = impl->frames;
      impl->eos_reason = L("EOI");
      impl->internalState = SR_RECOGNIZER_INTERNAL_EOI;
      if (impl->eventLog != NULL)
      {
        CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("generatePatternFromFrame() -> SR_RECOGNIZER_INTERNAL_EOI")));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
        CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
      }
    }
    else
    {
#ifdef SREC_ENGINE_VERBOSE_LOGGING
      PLogMessage("L: Voicing END (EOF) at %d frames (%d processed)", impl->frames, impl->processed);
#endif

      impl->isRecognizing = ESR_FALSE;
      impl->recogLogTimings.EOSD = impl->frames;
      impl->eos_reason = L("EOF");
      impl->internalState = SR_RECOGNIZER_INTERNAL_EOS;
      if (impl->eventLog != NULL)
      {
        CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("generatePatternFromFrame() -> SR_RECOGNIZER_INTERNAL_EOS")));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
        CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
        CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
      }
      *status = SR_RECOGNIZER_EVENT_END_OF_VOICING;
      *type = SR_RECOGNIZER_RESULT_TYPE_NONE;
      passert(impl->processed == impl->frames);
      if (impl->lockFunction)
        impl->lockFunction(ESR_UNLOCK, impl->lockData);
      return ESR_SUCCESS;
    }
  }
  if (impl->lockFunction)
    impl->lockFunction(ESR_UNLOCK, impl->lockData);

  /* Check for leaked state */
  passert(*status == SR_RECOGNIZER_EVENT_INVALID && *type == SR_RECOGNIZER_RESULT_TYPE_INVALID);
  return ESR_CONTINUE_PROCESSING;
CLEANUP:
  return rc;
}

/**
 * Same as generatePatternFromFrame() only the buffer is known to be empty.
 *
 * INPUT STATES: SR_RECOGNIZER_INTERNAL_EOI
 * OUTPUT STATES: same or SR_RECOGNIZER_INTERNAL_EOS
 */
PINLINE ESR_ReturnCode generatePatternFromFrameEOI(SR_RecognizerImpl* impl, SR_RecognizerStatus* status,
    SR_RecognizerResultType* type,
    SR_RecognizerResult* result)
{
  SR_AcousticModelsImpl* modelsImpl;
  ESR_ReturnCode rc;

  /* Run the search */
  modelsImpl = (SR_AcousticModelsImpl*) impl->models;

  if (CA_GetUnprocessedFramesInUtterance(impl->utterance) <= 0)
  {
    passert(impl->processed == impl->frames);
    *status = SR_RECOGNIZER_EVENT_END_OF_VOICING;
    *type = SR_RECOGNIZER_RESULT_TYPE_NONE;
    impl->internalState = SR_RECOGNIZER_INTERNAL_EOS;
    return ESR_SUCCESS;
  }

  if (!CA_MakePatternFrame(modelsImpl->pattern, impl->utterance))
  {
    *status = SR_RECOGNIZER_EVENT_NO_MATCH;
    *type = SR_RECOGNIZER_RESULT_TYPE_COMPLETE;
    impl->internalState = SR_RECOGNIZER_INTERNAL_END;
    if (impl->eventLog != NULL)
    {
      CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("generatePatternFromFrameEOI() -> SR_RECOGNIZER_INTERNAL_END")));
      CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
      CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
      CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
    }
    PLogError(L("ESR_INVALID_STATE"));
    return ESR_INVALID_STATE;
  }
  if (!CA_AdvanceUtteranceFrame(impl->utterance))
  {
    *status = SR_RECOGNIZER_EVENT_NO_MATCH;
    *type = SR_RECOGNIZER_RESULT_TYPE_COMPLETE;
    impl->internalState = SR_RECOGNIZER_INTERNAL_END;
    if (impl->eventLog != NULL)
    {
      CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("generatePatternFromFrameEOI() -> SR_RECOGNIZER_INTERNAL_END")));
      CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
      CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
      CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
    }
    PLogError(L("ESR_INVALID_STATE"));
    return ESR_INVALID_STATE;
  }
  CA_AdvanceRecognitionByFrame(impl->recognizer, modelsImpl->pattern, impl->utterance);
  ++impl->processed;

  if (impl->lockFunction)
    impl->lockFunction(ESR_LOCK, impl->lockData);

  if (CA_GetUnprocessedFramesInUtterance(impl->utterance) <= 0)
  {
    passert(impl->processed == impl->frames);
    *status = SR_RECOGNIZER_EVENT_END_OF_VOICING;
    *type = SR_RECOGNIZER_RESULT_TYPE_NONE;
    impl->internalState = SR_RECOGNIZER_INTERNAL_EOS;
    if (impl->eventLog != NULL)
    {
      CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("generatePatternFromFrameEOI() -> SR_RECOGNIZER_INTERNAL_EOS")));
      CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
      CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
      CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
    }
    if (impl->lockFunction)
      impl->lockFunction(ESR_UNLOCK, impl->lockData);
    return ESR_SUCCESS;
  }
  if (impl->lockFunction)
    impl->lockFunction(ESR_UNLOCK, impl->lockData);

  /* Check for leaked state */
  passert(*status == SR_RECOGNIZER_EVENT_INVALID && *type == SR_RECOGNIZER_RESULT_TYPE_INVALID);
  return ESR_CONTINUE_PROCESSING;
CLEANUP:
  if (impl->lockFunction)
    impl->lockFunction(ESR_UNLOCK, impl->lockData);
  return rc;
}


/**
 * INPUT STATES: SR_RECOGNIZER_INTERNAL_EOI, SR_RECOGNIZER_INTERNAL_EOS_DETECTION
 * OUTPUT STATES: same or SR_RECOGNIZER_INTERNAL_EOS
 */
ESR_ReturnCode detectEndOfSpeech(SR_RecognizerImpl* impl, SR_RecognizerStatus* status,
                                 SR_RecognizerResultType* type,
                                 SR_RecognizerResult* result)
{
  EOSrc eos; /* eos means end of speech */
  int eos_by_level; /* eos means end of speech */
  PTimeStamp timestamp;
  ESR_ReturnCode rc;
  ESR_BOOL enableGetWaveform = ESR_FALSE;

  eos_by_level = CA_UtteranceHasEnded(impl->utterance);
  if (eos_by_level)
  {
    eos = SPEECH_ENDED_BY_LEVEL_TIMEOUT;
  }
  else
  {
    eos = CA_IsEndOfUtteranceByResults(impl->recognizer);
  }

  ESR_SessionGetBool(L("enableGetWaveform"), &enableGetWaveform);
  //impl->parameters->getBool(impl->parameters, L("enableGetWaveform"), &enableGetWaveform);

  if (eos == VALID_SPEECH_CONTINUING && enableGetWaveform && impl->waveformBuffer->overflow_count > 0)
  {
    size_t bufferSize;
    CHKLOG(rc, WaveformBuffer_GetSize(impl->waveformBuffer, &bufferSize));
    PLogMessage("Forcing EOS due to wfbuf overflow (fr=%d,sz=%d,of=%d)", impl->frames, bufferSize, impl->waveformBuffer->overflow_count);
    eos = SPEECH_TOO_LONG;
  }

  if (eos != VALID_SPEECH_CONTINUING)
  {
    switch (eos)
    {
      case SPEECH_ENDED:
        /* normal */
        impl->eos_reason = L("itimeout");
        break;

      case SPEECH_ENDED_WITH_ERROR:
        /* error */
        impl->eos_reason = L("err");
        break;

      case SPEECH_TOO_LONG:
        /* timeout*/
        impl->eos_reason = L("ctimeout");
        break;

      case SPEECH_MAYBE_ENDED:
        /* normal */
        impl->eos_reason = L("itimeout");
        break;
      case SPEECH_ENDED_BY_LEVEL_TIMEOUT:
        /* normal */
        impl->eos_reason = L("levelTimeout");
        break;

      default:
        /* error */
        impl->eos_reason = L("err");
    }

#ifdef SREC_ENGINE_VERBOSE_LOGGING
    PLogMessage("L: Voicing END (EOS) at %d frames, %d processed (reason: %s)\n", impl->frames, impl->processed, impl->eos_reason);
#endif

    impl->recogLogTimings.EOSD = impl->frames; /* how many frames have been sent prior to detect EOS */
    PTimeStampSet(&timestamp); /* time it took to detect EOS (in millisec) */
    impl->recogLogTimings.EOST = PTimeStampDiff(&timestamp, &impl->timestamp);

    *status = SR_RECOGNIZER_EVENT_END_OF_VOICING;
    *type = SR_RECOGNIZER_RESULT_TYPE_NONE;
    impl->internalState = SR_RECOGNIZER_INTERNAL_EOS;
    if (impl->eventLog != NULL)
    {
      CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("internalState"), L("detectEndOfSpeech() -> SR_RECOGNIZER_INTERNAL_EOS")));
      CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("reason"), impl->eos_reason));
      CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("frames"), impl->frames));
      CHKLOG(rc, SR_EventLogTokenSize_t_BASIC(impl->eventLog, impl->osi_log_level, L("processed"), impl->processed));
      CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SR_Recognizer")));
    }
    impl->isRecognizing = ESR_FALSE;
    return ESR_SUCCESS;
  }

  /* Check for leaked state */
  passert(*status == SR_RECOGNIZER_EVENT_INVALID && *type == SR_RECOGNIZER_RESULT_TYPE_INVALID);
  return ESR_CONTINUE_PROCESSING;
CLEANUP:
  return rc;
}

/**
 * INPUT STATES: SR_RECOGNIZER_INTERNAL_BOS_DETECTION
 * OUTPUT STATES: same or SR_RECOGNIZER_INTERNAL_EOS_DETECTION, SR_RECOGNIZER_INTERNAL_EOI
 */
ESR_ReturnCode detectBeginningOfSpeech(SR_RecognizerImpl* impl,
                                       SR_RecognizerStatus* status,
                                       SR_RecognizerResultType* type,
                                       SR_RecognizerResult* result)
{
  ESR_ReturnCode rc;
  ESR_BOOL gatedMode;
  size_t num_windback_bytes, num_windback_frames;
  waveform_buffering_state_t buffering_state;

  CHKLOG(rc, ESR_SessionGetBool(L("cmdline.gatedmode"), &gatedMode));

  if (gatedMode || (!gatedMode && impl->frames < impl->bgsniff))
  {
    ESR_BOOL pushable = ESR_FALSE;

    rc = canPushAudioIntoRecognizer(impl);
    if (rc == ESR_SUCCESS)
    {
      /* Not enough samples to process one frame */
      if (CA_GetUnprocessedFramesInUtterance(impl->utterance) <= 0)
      {
        *status = SR_RECOGNIZER_EVENT_NEED_MORE_AUDIO;
        *type = SR_RECOGNIZER_RESULT_TYPE_NONE;
        return ESR_SUCCESS;
      }
    }
    else if (rc != ESR_CONTINUE_PROCESSING)
      return rc;
    else if (impl->internalState == SR_RECOGNIZER_INTERNAL_EOI)
    {
      /* Got end of input before beginning of speech */
      *status = SR_RECOGNIZER_EVENT_NO_MATCH;
      *type = SR_RECOGNIZER_RESULT_TYPE_COMPLETE;
      impl->internalState = SR_RECOGNIZER_INTERNAL_BOS_NO_MATCH;
      CHKLOG(rc, impl->Interface.stop(&impl->Interface));
      return ESR_SUCCESS;
    }
    else
      pushable = ESR_TRUE;
    if (pushable)
    {
      rc = pushAudioIntoRecognizer(impl, status, type, result);
      /* OUTPUT STATES: same or SR_RECOGNIZER_INTERNAL_EOI */
      if (rc != ESR_CONTINUE_PROCESSING)
      {
        /* Not enough samples to process one frame */
        return rc;
      }
      rc = generateFrameFromAudio(impl, status, type, result);
      /* OUTPUT STATES: same */
      if (rc != ESR_CONTINUE_PROCESSING)
      {
        /*
         * The internal recognizer needs a minimum amount of audio before
         * it begins generating frames.
         */
        return rc;
      }
    }
    if (!CA_AdvanceUtteranceFrame(impl->utterance))
    {
      PLogError(L("ESR_INVALID_STATE: Failed Advancing Utt Frame %d"), impl->frames);
      return ESR_INVALID_STATE;
    }
    if (CA_UtteranceHasVoicing(impl->utterance))
    {
      /* Utterance stats for Lombard if enough frames */
      if (impl->frames > impl->bgsniff)
      {
#ifdef SREC_ENGINE_VERBOSE_LOGGING
        PLogMessage("L:  Voicing START at %d frames", impl->frames);
#endif
        /* OSI log the endpointed data */

        CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->osi_log_level, L("BTIM"), impl->frames * MSEC_PER_FRAME));
        CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->osi_log_level, L("BRGN"), 0)); /* Barge-in not supported */
        CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SWIendp")));

        CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->osi_log_level, L("BOSD"), impl->frames));
        CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("ESRbosd")));

        if (gatedMode)
          CA_CalculateUtteranceStatistics(impl->utterance, (int)(impl->frames * -1), 0);
        else
          CA_CalculateUtteranceStatistics(impl->utterance, 0, impl->frames);
      }

      /* OK, we've got voicing or the end of input has occured
      ** (or both, I suppose).  If we had voicing then progress
      ** the recognizer, otherwise skip to the end.
      ** Of course, we could be running outside 'Gated Mode'
      ** so we won't have any frames processed at all yet -
      ** in this case start the recognizer anyway.
      */

      /*************************************
       ** Run recognition until endOfInput **
       *************************************/

      /*
       * Initialize both recognizers first
       * and disable reporting of results
       */
      if (gatedMode)
      {
        /*
         * We're in Gated Mode -
         * Because we'll have had voicing we wind-back
         * until the start of voicing (unsure region)
         */
        num_windback_frames = CA_SeekStartOfUtterance(impl->utterance);
        impl->beginningOfSpeechOffset = impl->frames - num_windback_frames;
        num_windback_bytes = num_windback_frames * impl->FRAME_SIZE * 2 /* due to skip even frames */;

        /* pfprintf(PSTDOUT,L("audio buffer windback %d frames == %d bytes\n"), num_windback_frames, num_windback_bytes); */
        CHKLOG(rc, WaveformBuffer_GetBufferingState(impl->waveformBuffer, &buffering_state));
        if (buffering_state != WAVEFORM_BUFFERING_OFF)
          CHKLOG(rc, WaveformBuffer_WindBack(impl->waveformBuffer, num_windback_bytes));

        /*
         * Only transition to linear if it was previously circular (in other words if
         * buffering was active in the first place)
         */
        if (buffering_state == WAVEFORM_BUFFERING_ON_CIRCULAR)
          CHKLOG(rc, WaveformBuffer_SetBufferingState(impl->waveformBuffer, WAVEFORM_BUFFERING_ON_LINEAR));
        impl->frames = CA_GetUnprocessedFramesInUtterance(impl->utterance);
      }
      else
        impl->frames = 0;
      /* reset the frames */
      impl->processed = 0;
      CHKLOG(rc, beginRecognizing(impl));
      impl->internalState = SR_RECOGNIZER_INTERNAL_EOS_DETECTION;
      *status = SR_RECOGNIZER_EVENT_START_OF_VOICING;
      *type = SR_RECOGNIZER_RESULT_TYPE_NONE;
      return ESR_SUCCESS;
    }
    else
    {
      if (impl->frames > impl->utterance_timeout)
      {
        /* beginning of speech timeout */
        impl->internalState = SR_RECOGNIZER_INTERNAL_BOS_TIMEOUT;
        *status = SR_RECOGNIZER_EVENT_START_OF_UTTERANCE_TIMEOUT;
        *type = SR_RECOGNIZER_RESULT_TYPE_COMPLETE;
        CHKLOG(rc, impl->Interface.stop(&impl->Interface));
        return ESR_SUCCESS;
      }
    }
  }
  else if (!gatedMode && impl->frames >= impl->bgsniff)
  {
    /*
    * If not gated mode and I have processed enough frames, then start the recognizer
    * right away.
    */
    impl->internalState = SR_RECOGNIZER_INTERNAL_EOS_DETECTION;
    *status = SR_RECOGNIZER_EVENT_INCOMPLETE;
    *type = SR_RECOGNIZER_RESULT_TYPE_NONE;

    /* reset the frames */
    impl->frames = impl->processed = 0;
    CHKLOG(rc, beginRecognizing(impl));
    return ESR_SUCCESS;
  }
  *status = SR_RECOGNIZER_EVENT_INCOMPLETE;
  *type = SR_RECOGNIZER_RESULT_TYPE_NONE;
  return ESR_SUCCESS;

CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerAdvanceImpl(SR_Recognizer* self, SR_RecognizerStatus* status,
                                        SR_RecognizerResultType* type,
                                        SR_RecognizerResult** result)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  ESR_BOOL pushable;
  ESR_ReturnCode rc;

  if (status == NULL || type == NULL || result == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }

  /* create the result holder and save the pointer */
  /* creation only happens once (due to the if condition) */
  if (impl->result == NULL)
    CHKLOG(rc, SR_RecognizerResult_Create(&impl->result, impl));
  *result = impl->result;

  /*
   * The following two lines are used to detect bugs whereby we forget to set
   * status or type before returning
   */
  *status = SR_RECOGNIZER_EVENT_INVALID;
  *type = SR_RECOGNIZER_RESULT_TYPE_INVALID;

MOVE_TO_NEXT_STATE:
  switch (impl->internalState)
  {
    case SR_RECOGNIZER_INTERNAL_BEGIN:
      impl->internalState = SR_RECOGNIZER_INTERNAL_BOS_DETECTION;
      *status = SR_RECOGNIZER_EVENT_STARTED;
      *type = SR_RECOGNIZER_RESULT_TYPE_NONE;
      return ESR_SUCCESS;

    case SR_RECOGNIZER_INTERNAL_BOS_DETECTION:
      rc = detectBeginningOfSpeech(impl, status, type, impl->result);
      if (rc != ESR_CONTINUE_PROCESSING)
      {
        /*
         * SR_RECOGNIZER_INTERNAL_BOS_DETECTION, SR_RECOGNIZER_INTERNAL_EOS_DETECTION, or
         * SR_RECOGNIZER_INTERNAL_EOI
         */
        return rc;
      }
      /* Leaked state */
      passert(0);
      break;

    case SR_RECOGNIZER_INTERNAL_EOS_DETECTION:
      pushable = ESR_FALSE;
      rc = canPushAudioIntoRecognizer(impl);
      if (rc == ESR_SUCCESS)
      {
        /* Not enough samples to process one frame */
        if (CA_GetUnprocessedFramesInUtterance(impl->utterance) <= 0)
        {
          *status = SR_RECOGNIZER_EVENT_NEED_MORE_AUDIO;
          *type = SR_RECOGNIZER_RESULT_TYPE_NONE;
          return ESR_SUCCESS;
        }
      }
      else if (rc != ESR_CONTINUE_PROCESSING)
        return rc;
      else if (impl->internalState == SR_RECOGNIZER_INTERNAL_EOI)
        goto MOVE_TO_NEXT_STATE;
      else
        pushable = ESR_TRUE;
      if (pushable)
      {
        rc = pushAudioIntoRecognizer(impl, status, type, impl->result);
        if (rc != ESR_CONTINUE_PROCESSING)
        {
          /* Not enough samples to process one frame */
          return rc;
        }
        if (impl->internalState == SR_RECOGNIZER_INTERNAL_EOI)
          goto MOVE_TO_NEXT_STATE;
        rc = generateFrameFromAudio(impl, status, type, impl->result);
        if (rc != ESR_CONTINUE_PROCESSING)
        {
          /*
           * The internal recognizer needs a minimum amount of audio before
           * it begins generating frames.
           */
          return rc;
        }
      }
      rc = generateFrameStats(impl, status, type, impl->result);
      if (rc != ESR_CONTINUE_PROCESSING)
      {
        /* Not enough frames to calculate stats */
        return rc;
      }
      rc = generatePatternFromFrame(impl, status, type, impl->result);
      if (rc != ESR_CONTINUE_PROCESSING)
      {
        /* End of speech detected */
        return rc;
      }
      if (impl->internalState == SR_RECOGNIZER_INTERNAL_END)
        goto MOVE_TO_NEXT_STATE;
      rc = detectEndOfSpeech(impl, status, type, impl->result);
      if (rc != ESR_CONTINUE_PROCESSING)
      {
        /* End of speech detected */
        return rc;
      }
      *status = SR_RECOGNIZER_EVENT_INCOMPLETE;
      *type = SR_RECOGNIZER_RESULT_TYPE_NONE;
      return ESR_SUCCESS;

    case SR_RECOGNIZER_INTERNAL_EOI:
      /*
       * On EOI (end of input), we need to process the remaining frames that had not
       * been processed when PutAudio set the gotLastFrame flag
       */
      rc = generatePatternFromFrameEOI(impl, status, type, impl->result);
      if (rc != ESR_CONTINUE_PROCESSING)
      {
        /* End of speech detected */
        return rc;
      }
      rc = detectEndOfSpeech(impl, status, type, impl->result);
      if (rc != ESR_CONTINUE_PROCESSING)
      {
        /* End of speech detected */
        return rc;
      }
      *status = SR_RECOGNIZER_EVENT_INCOMPLETE;
      *type = SR_RECOGNIZER_RESULT_TYPE_NONE;
      return ESR_SUCCESS;

    case SR_RECOGNIZER_INTERNAL_EOS:
      /* On EOS (end of speech detected - not due to end of input), create the result */
      if (impl->lockFunction)
        impl->lockFunction(ESR_LOCK, impl->lockData);
      CircularBufferReset(impl->buffer);
      if (impl->lockFunction)
        impl->lockFunction(ESR_UNLOCK, impl->lockData);
      CHKLOG(rc, SR_RecognizerCreateResultImpl((SR_Recognizer*) impl, status, type));
      impl->internalState = SR_RECOGNIZER_INTERNAL_END;
      return ESR_SUCCESS;

    case SR_RECOGNIZER_INTERNAL_END:
      return ESR_SUCCESS;
    default:
      PLogError(L("ESR_INVALID_STATE"));
      return ESR_INVALID_STATE;
  }
CLEANUP:
  return rc;
}



ESR_ReturnCode SR_RecognizerLoadUtteranceImpl(SR_Recognizer* self, const LCHAR* filename)
{
  /* TODO: complete */
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_RecognizerLoadWaveFileImpl(SR_Recognizer* self, const LCHAR* filename)
{
  /* TODO: complete */
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_RecognizerLogEventImpl(SR_Recognizer* self, const LCHAR* event)
{
  ESR_ReturnCode rc;
  SR_RecognizerImpl *impl = (SR_RecognizerImpl*) self;
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, event));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerLogTokenImpl(SR_Recognizer* self, const LCHAR* token, const LCHAR* value)
{
  ESR_ReturnCode rc;
  SR_RecognizerImpl *impl = (SR_RecognizerImpl*) self;
  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, token, value));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerLogTokenIntImpl(SR_Recognizer* self, const LCHAR* token, int value)
{
  ESR_ReturnCode rc;
  SR_RecognizerImpl *impl = (SR_RecognizerImpl*) self;
  CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->osi_log_level, token, value));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerLogSessionStartImpl(SR_Recognizer* self, const LCHAR* sessionName)
{
  ESR_ReturnCode rc;
  SR_RecognizerImpl *impl = (SR_RecognizerImpl*) self;
  /**
  * OSI Platform logging.
  * In OSR, these events are logged by the platform. We have no platform in ESR, so we
   * log them here.
  */

  /* call (session) start, tokens optional */
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SWIclst")));

  /* service start, in this case SRecTest service */
  CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->osi_log_level, L("SVNM"), sessionName));
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SWIsvst")));
  if (impl->osi_log_level & OSI_LOG_LEVEL_BASIC)
    CHKLOG(rc, SR_EventLogEventSession(impl->eventLog));

  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerLogSessionEndImpl(SR_Recognizer* self)
{
  ESR_ReturnCode rc;
  SR_RecognizerImpl *impl = (SR_RecognizerImpl*) self;

  /* OSI log end of call (session) */
  CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->osi_log_level, L("SWIclnd")));
  if (impl->osi_log_level & OSI_LOG_LEVEL_BASIC)
    CHKLOG(rc, SR_EventLogEventSession(impl->eventLog));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}


ESR_ReturnCode SR_RecognizerLogWaveformDataImpl(SR_Recognizer* self, const LCHAR* waveformFilename,
    const LCHAR* transcription, const double bos,
    const double eos, ESR_BOOL isInvocab)
{
  ESR_ReturnCode rc;
  SR_RecognizerImpl *impl = (SR_RecognizerImpl*) self;
  LCHAR num[P_PATH_MAX];
  int frame;

  CHKLOG(rc, SR_EventLogToken_AUDIO(impl->eventLog, impl->osi_log_level, L("FILE"), waveformFilename));
  CHKLOG(rc, SR_EventLogToken_AUDIO(impl->eventLog, impl->osi_log_level, L("TRANS"), transcription));
  sprintf(num, L("%.2f"), bos);
  CHKLOG(rc, SR_EventLogToken_AUDIO(impl->eventLog, impl->osi_log_level, L("BOS_SEC"), num));
  sprintf(num, L("%.2f"), eos);
  CHKLOG(rc, SR_EventLogToken_AUDIO(impl->eventLog, impl->osi_log_level, L("EOS_SEC"), num));
  CHKLOG(rc, SR_EventLogTokenInt_AUDIO(impl->eventLog, impl->osi_log_level, L("FRAMESIZE"), impl->FRAME_SIZE));
  CHKLOG(rc, SR_EventLogTokenInt_AUDIO(impl->eventLog, impl->osi_log_level, L("SAMPLERATE"), impl->sampleRate));
  frame = (int)(bos * impl->sampleRate * 2 /* 2 bytes per sample */) / impl->FRAME_SIZE;
  CHKLOG(rc, SR_EventLogTokenInt_AUDIO(impl->eventLog, impl->osi_log_level, L("BOS_FR"), frame));
  frame = (int)(eos * impl->sampleRate * 2 /* 2 bytes per sample */) / impl->FRAME_SIZE;
  CHKLOG(rc, SR_EventLogTokenInt_AUDIO(impl->eventLog, impl->osi_log_level, L("EOS_FR"), frame));
  CHKLOG(rc, SR_EventLogTokenInt_AUDIO(impl->eventLog, impl->osi_log_level, L("INVOCAB"), isInvocab));
  CHKLOG(rc, SR_EventLogEvent_AUDIO(impl->eventLog, impl->osi_log_level, L("ESRwfrd")));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerSetLockFunctionImpl(SR_Recognizer* self, SR_RecognizerLockFunction function, void* data)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;

  impl->lockFunction = function;
  impl->lockData = data;
  return ESR_SUCCESS;
}

static ESR_ReturnCode doSignalQualityInit(SR_RecognizerImpl* impl)
{
  CA_DoSignalCheck(impl->wavein, &impl->isSignalClipping, &impl->isSignalDCOffset,
                   &impl->isSignalNoisy, &impl->isSignalTooQuiet, &impl->isSignalTooFewSamples,
                   &impl->isSignalTooManySamples);
  impl->isSignalQualityInitialized = ESR_TRUE;
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_RecognizerIsSignalClippingImpl(SR_Recognizer* self, ESR_BOOL* isClipping)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  ESR_ReturnCode rc;

  if (isClipping == NULL)
  {
    PLogError("SR_RecognizerIsSignalClippingImpl", ESR_INVALID_ARGUMENT);
    return ESR_INVALID_ARGUMENT;
  }
  if (!impl->isSignalQualityInitialized)
    CHKLOG(rc, doSignalQualityInit(impl));
  *isClipping = impl->isSignalClipping;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerIsSignalDCOffsetImpl(SR_Recognizer* self, ESR_BOOL* isDCOffset)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  ESR_ReturnCode rc;

  if (isDCOffset == NULL)
  {
    PLogError("SR_RecognizerIsSignalDCOffsetImpl", ESR_INVALID_ARGUMENT);
    return ESR_INVALID_ARGUMENT;
  }
  if (!impl->isSignalQualityInitialized)
    CHKLOG(rc, doSignalQualityInit(impl));
  *isDCOffset = impl->isSignalDCOffset;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerIsSignalNoisyImpl(SR_Recognizer* self, ESR_BOOL* isNoisy)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  ESR_ReturnCode rc;

  if (isNoisy == NULL)
  {
    PLogError("SR_RecognizerIsSignalNoisyImpl", ESR_INVALID_ARGUMENT);
    return ESR_INVALID_ARGUMENT;
  }
  if (!impl->isSignalQualityInitialized)
    CHKLOG(rc, doSignalQualityInit(impl));
  *isNoisy = impl->isSignalNoisy;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerIsSignalTooQuietImpl(SR_Recognizer* self, ESR_BOOL* isTooQuiet)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  ESR_ReturnCode rc;

  if (isTooQuiet == NULL)
  {
    PLogError("SR_RecognizerIsSignalTooQuietImpl", ESR_INVALID_ARGUMENT);
    return ESR_INVALID_ARGUMENT;
  }
  if (!impl->isSignalQualityInitialized)
    CHKLOG(rc, doSignalQualityInit(impl));
  *isTooQuiet = impl->isSignalTooQuiet;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerIsSignalTooFewSamplesImpl(SR_Recognizer* self, ESR_BOOL* isTooFewSamples)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  ESR_ReturnCode rc;

  if (isTooFewSamples == NULL)
  {
    PLogError("SR_RecognizerIsSignalTooFewSamplesImpl", ESR_INVALID_ARGUMENT);
    return ESR_INVALID_ARGUMENT;
  }
  if (!impl->isSignalQualityInitialized)
    CHKLOG(rc, doSignalQualityInit(impl));
  *isTooFewSamples = impl->isSignalTooFewSamples;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_RecognizerIsSignalTooManySamplesImpl(SR_Recognizer* self, ESR_BOOL* isTooManySamples)
{
  SR_RecognizerImpl* impl = (SR_RecognizerImpl*) self;
  ESR_ReturnCode rc;

  if (isTooManySamples == NULL)
  {
    PLogError("SR_RecognizerIsSignalTooManySamplesImpl", ESR_INVALID_ARGUMENT);
    return ESR_INVALID_ARGUMENT;
  }
  if (!impl->isSignalQualityInitialized)
    CHKLOG(rc, doSignalQualityInit(impl));
  *isTooManySamples = impl->isSignalTooManySamples;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}



/**************************************/
/* Waveform Buffer stuff              */
/**************************************/
ESR_ReturnCode WaveformBuffer_Create(WaveformBuffer** waveformBuffer, size_t frame_size)
{
  ESR_ReturnCode rc;
  WaveformBuffer *buf;
  size_t val_size_t;
  int    val_int;
  ESR_BOOL   exists;

  buf = NEW(WaveformBuffer, L("SR_RecognizerImpl.wvfmbuf"));
  if (buf == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(L("%s: could not create WaveformBuffer"), ESR_rc2str(rc));
    goto CLEANUP;
  }

  ESR_SessionContains(L("SREC.voice_enroll.bufsz_kB"), &exists);
  if (exists)
    ESR_SessionGetSize_t(L("SREC.voice_enroll.bufsz_kB"), &val_size_t);
  else
    val_size_t = DEFAULT_WAVEFORM_BUFFER_MAX_SIZE;
  val_size_t *= 1024; /* convert to kB*/
  CHKLOG(rc, CircularBufferCreate(val_size_t, L("SR_RecognizerImpl.wvfmbuf.cbuffer"), &buf->cbuffer));

  ESR_SessionContains(L("CREC.Frontend.start_windback"), &exists);
  if (exists)
    ESR_SessionGetInt(L("CREC.Frontend.start_windback"), &val_int);
  else
    val_int = DEFAULT_WAVEFORM_WINDBACK_FRAMES;
  val_int *= frame_size; /* convert frames to bytes */
  buf->windback_buffer_sz = (size_t) val_int;
  buf->windback_buffer = MALLOC(buf->windback_buffer_sz, L("SR_RecognizerImpl.wvfmbuf.windback"));
  if (buf->windback_buffer == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(L("%s: could not create Waveform windback buffer"), ESR_rc2str(rc));
    goto CLEANUP;
  }


  ESR_SessionContains(L("SREC.voice_enroll.eos_comfort_frames"), &exists);
  if (exists)
    ESR_SessionGetSize_t(L("SREC.voice_enroll.eos_comfort_frames"), &val_size_t);
  else
    val_size_t = DEFAULT_EOS_COMFORT_FRAMES;
  buf->eos_comfort_frames = val_size_t;

  ESR_SessionContains(L("SREC.voice_enroll.bos_comfort_frames"), &exists);
  if (exists)
    ESR_SessionGetSize_t(L("SREC.voice_enroll.bos_comfort_frames"), &val_size_t);
  else
    val_size_t = DEFAULT_BOS_COMFORT_FRAMES;
  buf->bos_comfort_frames = val_size_t;

  /* initially off */
  buf->state = WAVEFORM_BUFFERING_OFF;

  *waveformBuffer = buf;
  return ESR_SUCCESS;
CLEANUP:
  WaveformBuffer_Destroy(buf);
  return rc;
}

ESR_ReturnCode WaveformBuffer_Write(WaveformBuffer* waveformBuffer, void *data, size_t num_bytes)
{
  size_t available_bytes;
  size_t done_bytes;

  /* do nothing if not active */
  switch (waveformBuffer->state)
  {
    case WAVEFORM_BUFFERING_OFF:
      return ESR_SUCCESS;

    case WAVEFORM_BUFFERING_ON_CIRCULAR:
      available_bytes = CircularBufferGetAvailable(waveformBuffer->cbuffer);
      if (available_bytes < num_bytes)
      {
        done_bytes = CircularBufferSkip(waveformBuffer->cbuffer, num_bytes - available_bytes);
        if (done_bytes != num_bytes - available_bytes)
        {
          PLogError("WaveformBuffer_Write: error when skipping bytes");
          return ESR_INVALID_STATE;
        }
      }
      done_bytes = CircularBufferWrite(waveformBuffer->cbuffer, data, num_bytes);
      if (done_bytes != num_bytes)
      {
        PLogError("WaveformBuffer_Write: error when writing bytes");
        return ESR_INVALID_STATE;
      }
      return ESR_SUCCESS;

    case WAVEFORM_BUFFERING_ON_LINEAR:
      available_bytes = CircularBufferGetAvailable(waveformBuffer->cbuffer);
      if (available_bytes < num_bytes)
      {
        waveformBuffer->overflow_count += num_bytes;
        return ESR_BUFFER_OVERFLOW;
      }
      done_bytes = CircularBufferWrite(waveformBuffer->cbuffer, data, num_bytes);
      if (done_bytes != num_bytes)
      {
        PLogError("WaveformBuffer_Write: error when writing bytes");
        return ESR_INVALID_STATE;
      }
      return ESR_SUCCESS;

    default:
      PLogError("WaveformBuffer_Write: bad control path");
      return ESR_INVALID_STATE;
  }
}

ESR_ReturnCode WaveformBuffer_Read(WaveformBuffer* waveformBuffer, void *data, size_t* num_bytes)
{
  size_t bytes_to_read;
  ESR_ReturnCode rc;

  if (num_bytes == NULL)
  {
    rc = ESR_INVALID_ARGUMENT;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  if (waveformBuffer->overflow_count > 0)
  {
    memset(data, 0, *num_bytes);
    *num_bytes = 0;
    PLogError(L("WaveformBuffer_Read: previous overflow causes read to return NULL"));
    return ESR_SUCCESS;
  }

  if (waveformBuffer->read_size != 0 && *num_bytes > waveformBuffer->read_size)
  {
    PLogError(L("ESR_OUT_OF_MEMORY: waveform buffer too small for read, increase from %d to %d"), *num_bytes, waveformBuffer->read_size);
    return ESR_OUT_OF_MEMORY;
  }

  if (waveformBuffer->read_size == 0)
    bytes_to_read = *num_bytes;
  else
    bytes_to_read = MIN(waveformBuffer->read_size, *num_bytes);
  waveformBuffer->read_size -= bytes_to_read;
  *num_bytes = CircularBufferRead(waveformBuffer->cbuffer, data, bytes_to_read);
  if (*num_bytes != bytes_to_read)
  {
    PLogError("WaveformBuffer_Read: error reading buffer");
    return ESR_INVALID_STATE;
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

/* WindBack will save the last num_bytes recorded, reset the buffer, and then load the
   saved bytes at the beginning of the buffer */
ESR_ReturnCode WaveformBuffer_WindBack(WaveformBuffer* waveformBuffer, const size_t num_bytes)
{
  ESR_ReturnCode rc;
  size_t bufferSize;

  if (num_bytes <= 0)
  {
    CHKLOG(rc, WaveformBuffer_Reset(waveformBuffer));
    return ESR_SUCCESS;
  }

  /* make sure windback buffer is big enough */
  if (num_bytes > waveformBuffer->windback_buffer_sz)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(L("%s: windback buffer is too small (needed=%d, had=%d)"), ESR_rc2str(rc), num_bytes, waveformBuffer->windback_buffer_sz);
    goto CLEANUP;
  }

  CHKLOG(rc, WaveformBuffer_GetSize(waveformBuffer, &bufferSize));
  /* skip the first few bytes written */
  if (bufferSize < num_bytes)
  {
    PLogError("bufferSize %d num_bytes %d (ESR_INVALID_STATE)\n", bufferSize, num_bytes);
    bufferSize = 0;
  }
  else
  {
    bufferSize -= num_bytes;
  }
  CHKLOG(rc, WaveformBuffer_Skip(waveformBuffer, bufferSize));
  /* read the last few bytes written */
  bufferSize = num_bytes;
  CHKLOG(rc, WaveformBuffer_Read(waveformBuffer, waveformBuffer->windback_buffer, &bufferSize));

  /* reset buffer */
  CHKLOG(rc, WaveformBuffer_Reset(waveformBuffer));

  /* rewrite the saved bytes at the beginning */
  CHKLOG(rc, WaveformBuffer_Write(waveformBuffer, waveformBuffer->windback_buffer, bufferSize));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode WaveformBuffer_Destroy(WaveformBuffer* waveformBuffer)
{
  if (waveformBuffer->cbuffer)
    FREE(waveformBuffer->cbuffer);
  if (waveformBuffer->windback_buffer)
    FREE(waveformBuffer->windback_buffer);
  if (waveformBuffer)
    FREE(waveformBuffer);
  return ESR_SUCCESS;
}

ESR_ReturnCode WaveformBuffer_SetBufferingState(WaveformBuffer* waveformBuffer, waveform_buffering_state_t state)
{
  waveformBuffer->state = state;
  return ESR_SUCCESS;
}

ESR_ReturnCode WaveformBuffer_GetBufferingState(WaveformBuffer* waveformBuffer, waveform_buffering_state_t* state)
{
  *state = waveformBuffer->state;
  return ESR_SUCCESS;
}

/**
 * @return ESR_BUFFER_OVERFLOW if nametag EOS occured beyond end of buffer
 */
ESR_ReturnCode WaveformBuffer_ParseEndPointedResultAndTrim(WaveformBuffer* waveformBuffer, const LCHAR* end_pointed_result, const size_t bytes_per_frame)
{
  const LCHAR *p;
  size_t bos_frame, eos_frame, bufferSize, read_start_offset;
  ESR_ReturnCode rc;

  /* potential end pointed results

     -pau-@19 tape@36 scan@64 down@88 -pau2-@104
     -pau-@19 tape@34 off@55 -pau2-@78
     -pau-@19 tape@47 help@66 -pau2-@80
     -pau-@16 tape@36 reverse@71 -pau2-@91
     -pau-@21 tape@42 scan@59 down@80 -pau2-@91

     what I need to extract is the integer between "-pau-@" and ' '
     and the integer between '@' and " -pau2-"
  */


  p = LSTRSTR( end_pointed_result, PREFIX_WORD);
  if(p) p+=PREFIX_WORD_LEN; while(p && *p == '@') p++;
  rc = p ? lstrtoui(p, &bos_frame, 10) : ESR_INVALID_ARGUMENT;
  if (rc == ESR_INVALID_ARGUMENT)
  {
    PLogError(L("%s: extracting bos from text=%s"), ESR_rc2str(rc), end_pointed_result);
    goto CLEANUP;
  }
  else if (rc != ESR_SUCCESS)
    goto CLEANUP;

  p = LSTRSTR( end_pointed_result, SUFFIX_WORD);
  while(p && p>end_pointed_result && p[-1]!='@') --p;
  rc = p ? lstrtoui(p, &eos_frame, 10) : ESR_INVALID_ARGUMENT;
  if (rc == ESR_INVALID_ARGUMENT)
  {
    PLogError(L("%s: extracting eos from text=%s"), ESR_rc2str(rc), end_pointed_result);
    goto CLEANUP;
  }
  else if (rc != ESR_SUCCESS)
    goto CLEANUP;

  bos_frame -= (bos_frame > waveformBuffer->bos_comfort_frames ? waveformBuffer->bos_comfort_frames : 0);
  eos_frame += waveformBuffer->eos_comfort_frames;

  /*
   * I know where speech started, so I want to skip frames 0 to bos_frame.
   * I also know where speech ended so I want to set the amount of frames(bytes) to read for
   * the nametag audio buffer (i.e. the read_size)
   */

  read_start_offset = bos_frame * bytes_per_frame * 2 /* times 2 because of skip even frames */;
  waveformBuffer->read_size = (eos_frame - bos_frame) * bytes_per_frame * 2 /* times 2 because of skip even frames */;

  CHKLOG(rc, WaveformBuffer_GetSize(waveformBuffer, &bufferSize));
  if (read_start_offset + waveformBuffer->read_size > bufferSize)
  {
    waveformBuffer->overflow_count += read_start_offset + waveformBuffer->read_size - bufferSize;
    passert(waveformBuffer->overflow_count > 0);
    PLogMessage(L("Warning: Voice Enrollment audio buffer overflow (spoke too much, over by %d bytes)"),
                waveformBuffer->overflow_count);
    return ESR_BUFFER_OVERFLOW;
  }
  CHKLOG(rc, WaveformBuffer_Skip(waveformBuffer, read_start_offset));
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage(L("Voice Enrollment: bos@%d, eos@%d, therefore sizeof(waveform) should be %d"), bos_frame, eos_frame, waveformBuffer->read_size);
#endif
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}


ESR_ReturnCode WaveformBuffer_Reset(WaveformBuffer* waveformBuffer)
{
  CircularBufferReset(waveformBuffer->cbuffer);
  waveformBuffer->overflow_count = 0;
  waveformBuffer->read_size = 0;
  return ESR_SUCCESS;
}

ESR_ReturnCode WaveformBuffer_GetSize(WaveformBuffer* waveformBuffer, size_t* size)
{
  *size = CircularBufferGetSize(waveformBuffer->cbuffer);
  return ESR_SUCCESS;
}

ESR_ReturnCode WaveformBuffer_Skip(WaveformBuffer* waveformBuffer, const size_t bytes)
{
  if (CircularBufferSkip(waveformBuffer->cbuffer, bytes) != (int) bytes)
    return ESR_INVALID_STATE;
  return ESR_SUCCESS;
}



static ESR_ReturnCode SR_Recognizer_Reset_Buffers ( SR_RecognizerImpl *impl )
    {
    ESR_ReturnCode  reset_status;

    FREE ( impl->audioBuffer );
    impl->audioBuffer = NULL;
    impl->audioBuffer = MALLOC ( impl->FRAME_SIZE, MTAG );

    if ( impl->audioBuffer != NULL )
        {
        WaveformBuffer_Destroy ( impl->waveformBuffer );
        impl->waveformBuffer = NULL;
        reset_status = WaveformBuffer_Create ( &impl->waveformBuffer, impl->FRAME_SIZE );
        }
    else
        {
        reset_status = ESR_OUT_OF_MEMORY;
        }
    return ( reset_status );
    }



static ESR_ReturnCode SR_Recognizer_Validate_Sample_Rate ( size_t sample_rate )
    {
    ESR_ReturnCode  validate_status;

    switch ( sample_rate )
        {
        case 8000:
        case 11025:
        case 16000:
        case 22050:
            validate_status = ESR_SUCCESS;
            break;

        default:
            validate_status = ESR_INVALID_ARGUMENT;
            break;
        }
    return ( validate_status );
    }



static ESR_ReturnCode SR_Recognizer_Sample_Rate_Needs_Change ( size_t new_sample_rate, ESR_BOOL *needs_changing )
    {
    ESR_ReturnCode  validate_status;
    size_t          current_sample_rate;

    validate_status = ESR_SessionGetSize_t ( "CREC.Frontend.samplerate", &current_sample_rate );

    if ( validate_status == ESR_SUCCESS )
        {
        if ( new_sample_rate != current_sample_rate )
            *needs_changing = ESR_TRUE;
        else
            *needs_changing = ESR_TRUE;
        }
    return ( validate_status );
    }



static ESR_ReturnCode SR_Recognizer_Change_Sample_Rate_Session_Params_8K ( void )
    {
    ESR_ReturnCode  change_status;
    LCHAR           model_filenames [P_PATH_MAX];
    LCHAR           lda_filename [P_PATH_MAX];
    size_t          filename_length;

    filename_length = P_PATH_MAX;
    change_status = ESR_SessionGetLCHAR ( L("cmdline.modelfiles8"), model_filenames, &filename_length );

    if ( change_status == ESR_SUCCESS )
        {
        filename_length = P_PATH_MAX;
        change_status = ESR_SessionGetLCHAR ( L("cmdline.lda8"), lda_filename, &filename_length );

/* From this point on, if an error occurs, we're screwed and recovery is probably impossible */
        if ( change_status == ESR_SUCCESS )
            {
            change_status = ESR_SessionSetSize_t ( "CREC.Frontend.samplerate", 8000 );
            if ( change_status == ESR_SUCCESS )
                {
                change_status = ESR_SessionSetInt ( "CREC.Frontend.highcut", 4000 );

                if ( change_status == ESR_SUCCESS )
                    {
                    change_status =  ESR_SessionSetLCHAR ( L("cmdline.modelfiles"), model_filenames );

                    if ( change_status == ESR_SUCCESS )
                        change_status = ESR_SessionSetLCHAR ( L("cmdline.lda"), lda_filename );
                    }
                }
            }
        else
            {
            PLogError (L("\nMissing Parameter lda8\n"));
            }
        }
    else
        {
        PLogError (L("\nMissing Parameter models8\n"));
        }
    return ( change_status );
    }



static ESR_ReturnCode SR_Recognizer_Change_Sample_Rate_Session_Params_11K_to_22K ( size_t sample_rate )
    {
    ESR_ReturnCode  change_status;
    LCHAR           model_filenames [P_PATH_MAX];
    LCHAR           lda_filename [P_PATH_MAX];
    size_t          filename_length;

    filename_length = P_PATH_MAX;
    change_status = ESR_SessionGetLCHAR ( L("cmdline.modelfiles11"), model_filenames, &filename_length );

    if ( change_status == ESR_SUCCESS )
        {
        filename_length = P_PATH_MAX;
        change_status = ESR_SessionGetLCHAR ( L("cmdline.lda11"), lda_filename, &filename_length );

/* From this point on, if an error occurs, we're screwed and recovery is probably impossible */
        if ( change_status == ESR_SUCCESS )
            {
            change_status = ESR_SessionSetSize_t ( "CREC.Frontend.samplerate", sample_rate );

            if ( change_status == ESR_SUCCESS )
                {
                change_status = ESR_SessionSetInt ( "CREC.Frontend.highcut", 5500 );

                if ( change_status == ESR_SUCCESS )
                    {
                    change_status =  ESR_SessionSetLCHAR ( L("cmdline.modelfiles"), model_filenames );

                    if ( change_status == ESR_SUCCESS )
                        change_status = ESR_SessionSetLCHAR ( L("cmdline.lda"), lda_filename );
                    }
                }
            }
        else
            {
            PLogError (L("\nMissing Parameter lda11\n"));
            }
        }
    else
        {
        PLogError (L("\nMissing Parameter models11\n"));
        }
    return ( change_status );
    }



static ESR_ReturnCode SR_Recognizer_Change_Sample_Rate_Session_Params ( size_t new_sample_rate )
    {
    ESR_ReturnCode  change_status;

    if ( new_sample_rate == 8000 )
        change_status = SR_Recognizer_Change_Sample_Rate_Session_Params_8K ( );
    else
        change_status = SR_Recognizer_Change_Sample_Rate_Session_Params_11K_to_22K ( new_sample_rate );

    return ( change_status );
    }



ESR_ReturnCode SR_Recognizer_Change_Sample_RateImpl ( SR_Recognizer *recognizer, size_t new_sample_rate )
    {
    ESR_ReturnCode          change_status;
    ESR_BOOL                rate_needs_changing;
    SR_RecognizerImpl       *impl;
    CA_FrontendInputParams  *frontendParams;

    change_status = SR_Recognizer_Validate_Sample_Rate ( new_sample_rate );

    if ( change_status == ESR_SUCCESS )
        {
        change_status = SR_Recognizer_Sample_Rate_Needs_Change ( new_sample_rate, &rate_needs_changing );

        if ( change_status == ESR_SUCCESS )
            {
            if ( rate_needs_changing == ESR_TRUE )
                {
                change_status = SR_Recognizer_Change_Sample_Rate_Session_Params ( new_sample_rate );

                if ( change_status == ESR_SUCCESS )
                    { // SR_RecognizerCreateFrontendImpl
                    impl = (SR_RecognizerImpl *)recognizer;
                    change_status = SR_RecognizerUnsetupImpl( recognizer );

                    if ( change_status == ESR_SUCCESS )
                        {
                        CA_UnconfigureFrontend ( impl->frontend );
                        frontendParams = CA_AllocateFrontendParameters ( );

                        if ( frontendParams != NULL )
                            {
                            change_status = SR_RecognizerGetFrontendLegacyParametersImpl ( frontendParams );

                            if ( change_status == ESR_SUCCESS )
                                {
                                CA_ConfigureFrontend ( impl->frontend, frontendParams );
                                CA_UnconfigureWave ( impl->wavein );
                                CA_ConfigureWave ( impl->wavein, impl->frontend );
                                impl->sampleRate = new_sample_rate;
                                impl->FRAME_SIZE = impl->sampleRate / FRAMERATE * SAMPLE_SIZE;
                                change_status = SR_Recognizer_Reset_Buffers ( impl );

                                if ( change_status == ESR_SUCCESS )
                                    {
                                    change_status = SR_RecognizerSetupImpl( recognizer );

                                    if ( change_status == ESR_SUCCESS )
                                        change_status = SR_AcousticStateReset ( recognizer );
                                    }
                                else
                                    {
                                    SR_RecognizerSetupImpl( recognizer );   /* Otherwise recognizer is in bad state */
                                    }
                                }
                            CA_FreeFrontendParameters ( frontendParams );
                            }
                        else
                            {
                            SR_RecognizerSetupImpl( recognizer );   /* Otherwise recognizer is in bad state */
                            change_status = ESR_OUT_OF_MEMORY;
                            }
                        }
                    }
                }
            }
        }
    return ( change_status );
    }


