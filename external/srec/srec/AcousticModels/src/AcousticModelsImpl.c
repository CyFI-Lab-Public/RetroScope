/*---------------------------------------------------------------------------*
 *  AcousticModelsImpl.c  *
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


#ifndef lint
#endif /* lint */

#include "ESR_Session.h"
#include "IntArrayList.h"
#include "LCHAR.h"
#include "passert.h"
#include "plog.h"
#include "pmemory.h"
#include "SR_AcousticModels.h"
#include "SR_AcousticModelsImpl.h"
#include "pstdio.h"
#include "SR_EventLog.h"

#define MTAG NULL

#define CHKINTARRAY(rc, list, operation) rc = operation; \
  if (rc!=ESR_SUCCESS) \
  { \
    IntArrayListDestroy(list); \
    list = NULL; \
    PLogError(ESR_rc2str(rc)); \
    return rc; \
  }




/**
 * Initializes acoustic-models properties to default values.
 *
 * Replaces setup_acoustic_parameters()
 */
ESR_ReturnCode SR_AcousticModels_ToSession()
{
  ESR_ReturnCode rc;

  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Acoustic.dimen", 16));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Acoustic.skip", 5));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Acoustic.stay", 5));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Acoustic.whole_skip", 10));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Acoustic.whole_stay", 10));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Acoustic.durscale", 5));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Acoustic.frame_period", 10));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Acoustic.minvar", 1));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Acoustic.maxvar", 64000));
  CHKLOG(rc, ESR_SessionSetBoolIfEmpty("CREC.Acoustic.load_all_at_once", ESR_FALSE));
  CHKLOG(rc, ESR_SessionSetLCHARIfEmpty("CREC.Acoustic.load_models", L("all")));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

/**
 * Initializes pattern properties to default values.
 *
 * Replaces setup_pattern_parameters()
 */
ESR_ReturnCode SR_AcousticModels_PatternToSession()
{
  ESR_ReturnCode rc;

  /* Old comment: Remember to keep "ca_pip.h" up to date with these parameters... */

  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.dimen", 16));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.whole_dimen", 0));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.start", 0));
  CHKLOG(rc, ESR_SessionSetBoolIfEmpty("CREC.Pattern.chelt_imelda", ESR_FALSE));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.vfrlimit", 100));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.vfrthresh", 0));
  CHKLOG(rc, ESR_SessionSetFloatIfEmpty("CREC.Pattern.mix_score_scale", 0.46f));
  CHKLOG(rc, ESR_SessionSetFloatIfEmpty("CREC.Pattern.imelda_scale", 16));
  CHKLOG(rc, ESR_SessionSetFloatIfEmpty("CREC.Pattern.uni_score_scale", 0.46f));
  CHKLOG(rc, ESR_SessionSetFloatIfEmpty("CREC.Pattern.uni_score_offset", 0));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.forget_speech", 40));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.forget_background", 100));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.rel_low", 15));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.rel_high", 30));

  /* Gap: longest stop gap */
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.gap_period", 16));

  /* Click: longest isolated high-amplitude insert in silence */
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.click_period", 6));

  /* Breath: longest isolated medium amplitude */
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.breath_period", 50));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.extend_annotation", 0));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.min_initial_quiet_frames", 0));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.min_annotation_frames", 0));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.max_annotation_frames", 800));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.min_segment_rel_c0", 800));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.delete_leading_segments", 0));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.leading_segment_min_frames", 0));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.leading_segment_max_frames", 20));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.leading_segment_min_silence_gap_frames", 20));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.leading_segment_accept_if_not_found", 0));

#if DO_SUBTRACTED_SEGMENTATION
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.snr_holdoff", 0));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.min_acceptable_snr", 0));
#endif

  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.param", 0));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.beep_size", 0));
  CHKLOG(rc, ESR_SessionSetIntIfEmpty("CREC.Pattern.beep_threshold", 0));

  return ESR_SUCCESS;

CLEANUP:
  return rc;
}

/**
 * Populates legacy pattern parameters from the session.
 *
 * Replaces setup_pattern_parameters()
 */
ESR_ReturnCode SR_AcousticModels_LoadLegacyPatternParameters(CA_PatInputParams* params)
{
  ESR_ReturnCode rc;

  passert(params != NULL);
  params->is_loaded = ESR_FALSE;
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.dimen", &params->dimen));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.whole_dimen", &params->whole_dimen));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.start", &params->feat_start));
  CHKLOG(rc, ESR_SessionGetFloat("CREC.Pattern.mix_score_scale", &params->mix_score_scale));
  CHKLOG(rc, ESR_SessionGetFloat("CREC.Pattern.imelda_scale", &params->imelda_scale));
  CHKLOG(rc, ESR_SessionGetFloat("CREC.Pattern.uni_score_scale", &params->uni_score_scale));
  CHKLOG(rc, ESR_SessionGetFloat("CREC.Pattern.uni_score_offset", &params->uni_score_offset));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.forget_speech", &params->forget_speech));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.forget_background", &params->forget_background));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.rel_low", &params->rel_low));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.rel_high", &params->rel_high));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.gap_period", &params->gap_period));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.click_period", &params->click_period));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.breath_period", &params->breath_period));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.extend_annotation", &params->extend_annotation));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.min_initial_quiet_frames", &params->min_initial_quiet_frames));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.min_annotation_frames", &params->min_annotation_frames));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.max_annotation_frames", &params->max_annotation_frames));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.min_segment_rel_c0", &params->min_segment_rel_c0));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.delete_leading_segments", &params->delete_leading_segments));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.leading_segment_min_frames", &params->leading_segment_min_frames));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.leading_segment_max_frames", &params->leading_segment_max_frames));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.leading_segment_min_silence_gap_frames", &params->leading_segment_min_silence_gap_frames));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.leading_segment_accept_if_not_found", &params->leading_segment_accept_if_not_found));

#if DO_SUBTRACTED_SEGMENTATION
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.snr_holdoff", &params->snr_holdoff));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.min_acceptable_snr", &params->min_acceptable_snr));
#endif

  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.param", &params->param));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.beep_size", &params->beep_size));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Pattern.beep_threshold", &params->beep_threshold));

  params->is_loaded = ESR_TRUE;
  return ESR_SUCCESS;

CLEANUP:
  return rc;
}

/**
 * Generate legacy AcousticModels parameter structure from ESR_Session.
 *
 * @param params Resulting structure
 */
ESR_ReturnCode SR_AcousticModels_GetLegacyParameters(CA_AcoustInputParams* params)
{
  ESR_ReturnCode rc;
  size_t maxLabel = MAX_LABEL;

  passert(params != NULL);
  params->is_loaded = ESR_FALSE;
  CHKLOG(rc, ESR_SessionGetInt("CREC.Acoustic.dimen", &params->dimen));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Acoustic.skip", &params->skip_penalty));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Acoustic.stay", &params->stay_penalty));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Acoustic.whole_skip", &params->whole_skip_penalty));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Acoustic.whole_stay", &params->whole_stay_penalty));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Acoustic.durscale", &params->dur_scale));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Acoustic.frame_period", &params->frame_period));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Acoustic.minvar", &params->min_var));
  CHKLOG(rc, ESR_SessionGetInt("CREC.Acoustic.maxvar", &params->max_var));
  CHKLOG(rc, ESR_SessionGetBool("CREC.Acoustic.load_all_at_once", &params->load_all));
  CHKLOG(rc, ESR_SessionGetLCHAR("CREC.Acoustic.load_models", params->load_models, &maxLabel));
  params->is_loaded = ESR_TRUE;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

int LogArbdataVersion(unsigned int ver)
{
  ESR_ReturnCode rc;
  SR_EventLog* eventLog;
  size_t osi_log_level = 0;
  ESR_BOOL exists = ESR_FALSE;

  CHKLOG(rc, ESR_SessionExists(&exists));
  if (exists)
  {
    rc = ESR_SessionGetProperty(L("eventlog"), (void **)&eventLog, TYPES_SR_EVENTLOG);
    if ((rc != ESR_NO_MATCH_ERROR) && (rc != ESR_SUCCESS))
    {
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
    if (eventLog)
    {
      rc = ESR_SessionGetSize_t(L("SREC.Recognizer.osi_log_level"), &osi_log_level);
      if ((rc != ESR_NO_MATCH_ERROR) && (rc != ESR_SUCCESS))
      {
        PLogError(ESR_rc2str(rc));
        goto CLEANUP;
      }
      if (osi_log_level > 0)
      {
        rc = SR_EventLogTokenSize_t(eventLog, L("VER"), ver);
        rc = SR_EventLogEvent(eventLog, L("ESRarbd"));
      }
    }
  }
CLEANUP:
  return 0;
}



ESR_ReturnCode SR_AcousticModelsLoad(const LCHAR* filename, SR_AcousticModels** self)
{
  int use_image;
  LCHAR arbfile[P_PATH_MAX];
  CA_AcoustInputParams* acousticParams;
  CA_Acoustic* acoustic;
  LCHAR modelFilename[P_PATH_MAX];
  SR_AcousticModelsImpl* impl;
  size_t len;
  ESR_ReturnCode rc;

  impl = NEW(SR_AcousticModelsImpl, MTAG);
  if (impl == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }
  impl->Interface.destroy = &SR_AcousticModels_Destroy;
  impl->Interface.save = &SR_AcousticModels_Save;
  impl->Interface.setParameter = &SR_AcousticModels_SetParameter;
  impl->Interface.getParameter = &SR_AcousticModels_GetParameter;
  impl->Interface.getCount = &SR_AcousticModels_GetCount;
  impl->Interface.getID = &SR_AcousticModels_GetID;
  impl->Interface.setID = &SR_AcousticModels_SetID;
  impl->Interface.getArbdata = &SR_AcousticModels_GetArbdata;
  impl->setupPattern = &SR_AcousticModels_SetupPattern;
  impl->unsetupPattern = &SR_AcousticModels_UnsetupPattern;
  impl->getLegacyParameters = &SR_AcousticModels_GetLegacyParameters;
  impl->parameters = NULL;
  impl->pattern = NULL;
  impl->acoustic = NULL;
  impl->arbdata = NULL;
  impl->contents = NULL;
  impl->size = 0;
  acousticParams = NULL;
  acoustic = NULL;

  rc = SR_AcousticModels_PatternToSession();
  if (rc != ESR_SUCCESS)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  rc = SR_AcousticModels_ToSession();
  if (rc != ESR_SUCCESS)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  rc = ArrayListCreate(&impl->acoustic);
  if (rc != ESR_SUCCESS)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }

  acousticParams = CA_AllocateAcousticParameters();
  if (acousticParams == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  rc = impl->getLegacyParameters(acousticParams);
  if (rc != ESR_SUCCESS)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }

  rc = ESR_SessionGetInt(L("cmdline.use_image"), &use_image);
  if (rc != ESR_SUCCESS)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }

  while (ESR_TRUE)
  {
    int i;
    // skip space to next relative pathname
    while (LISSPACE(*filename)) filename++;
    if (*filename == L('\0')) break;
    // copy the relative pathname
    for (i = 0; *filename != L('\0') && !LISSPACE(*filename); i++)
    {
      modelFilename[i] = *filename++;
    }
    modelFilename[i] = L('\0');
    
    if (LSTRLEN(modelFilename) == 0 || modelFilename[0] == '#')
      continue;
    rc = lstrtrim(modelFilename);
    if (rc != ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }

    len = P_PATH_MAX;
    CHKLOG(rc, ESR_SessionPrefixWithBaseDirectory(modelFilename, &len));
    acoustic = CA_AllocateAcoustic();
    if (acoustic == NULL)
    {
      rc = ESR_OUT_OF_MEMORY;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
    if (use_image == 1)
    {
      rc = ESR_INVALID_STATE;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
    else if (use_image == 2)
    {
      if (!CA_LoadAcousticSub(acoustic, modelFilename, 0))
      {
        rc = ESR_INVALID_STATE;
        PLogError(ESR_rc2str(rc));
        goto CLEANUP;
      }
    }
    else
    {
            /* TODO: Is this being used? */
      if (!CA_LoadAcousticSub(acoustic, modelFilename, acousticParams))
      {
        rc = ESR_INVALID_STATE;
        PLogError(ESR_rc2str(rc));
        goto CLEANUP;
      }
    }
    rc = ArrayListAdd(impl->acoustic, acoustic);
    if (rc != ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }

  }
  len = P_PATH_MAX;
  rc = ESR_SessionGetLCHAR(L("cmdline.arbfile"), (LCHAR*) & arbfile, &len);
  if (rc != ESR_SUCCESS)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  len = P_PATH_MAX;
  CHKLOG(rc, ESR_SessionPrefixWithBaseDirectory(arbfile, &len));
  impl->arbdata = CA_LoadArbdata(arbfile);
  if (impl->arbdata == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    goto CLEANUP;
  }
  len = CA_ArbdataGetModelVersionID(impl->arbdata);
  LogArbdataVersion(len);

  CA_FreeAcousticParameters(acousticParams);
  *self = (SR_AcousticModels*) impl;

  return ESR_SUCCESS;
CLEANUP:
  if (acousticParams != NULL)
    CA_FreeAcousticParameters(acousticParams);
  impl->Interface.destroy(&impl->Interface);
  return rc;
}

ESR_ReturnCode SR_AcousticModels_Destroy(SR_AcousticModels* self)
{
  SR_AcousticModelsImpl* impl = (SR_AcousticModelsImpl*) self;
  CA_Acoustic* acoustic;
  ESR_ReturnCode rc;
  size_t size, i;

  if (impl->pattern != NULL)
  {
    CHKLOG(rc, impl->acoustic->getSize(impl->acoustic, &size));
    for (i = 0; i < size; ++i)
    {
      CHKLOG(rc, impl->acoustic->get(impl->acoustic, i, (void **)&acoustic));
      CA_ClearPatternForAcoustic(impl->pattern, acoustic);
    }
    CA_UnloadPattern(impl->pattern);
    CA_FreePattern(impl->pattern);
    impl->pattern = NULL;
  }

  if (impl->acoustic != NULL)
  {
    CHKLOG(rc, impl->acoustic->getSize(impl->acoustic, &size));
    for (i = 0; i < size; ++i)
    {
      CHKLOG(rc, impl->acoustic->get(impl->acoustic, 0, (void **)&acoustic));
      CHKLOG(rc, impl->acoustic->removeAtIndex(impl->acoustic, 0));

      /* Free acoustic models */
      CA_UnloadAcoustic(acoustic);
      CA_FreeAcoustic(acoustic);
    }

    CHKLOG(rc, impl->acoustic->destroy(impl->acoustic));
    impl->acoustic = NULL;
  }

  if (impl->arbdata != NULL)
  {
    CA_FreeArbdata(impl->arbdata);
    impl->arbdata = NULL;
  }

  FREE(impl);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_AcousticModels_Save(SR_AcousticModels* self, const LCHAR* filename)
{
  /*SR_AcousticModelsImpl* impl = (SR_AcousticModelsImpl*) self;*/

  /* CA_WriteAcousticImage(impl->acoustic, filename, 0); */
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_AcousticModels_SetParameter(SR_AcousticModels* self, const LCHAR* key, LCHAR* value)
{
  SR_AcousticModelsImpl* impl = (SR_AcousticModelsImpl*) self;
  LCHAR* temp;
  ESR_ReturnCode rc;

  rc = HashMapGet(impl->parameters, key, (void **)&temp);
  if (rc == ESR_SUCCESS)
  {
    /* Key already exists, remove old value if necessary */
    if (LSTRCMP(temp, value) == 0)
      return ESR_SUCCESS;
    CHKLOG(rc, HashMapRemove(impl->parameters, key));
    FREE(temp);
  }
  else if (rc != ESR_NO_MATCH_ERROR)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }

  /* Allocate and put new key */
  temp = MALLOC(sizeof(LCHAR) * (LSTRLEN(value) + 1), MTAG);
  if (temp == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  CHKLOG(rc, impl->parameters->put(impl->parameters, key, temp));
  return ESR_SUCCESS;
CLEANUP:
  FREE(temp);
  return rc;
}

ESR_ReturnCode SR_AcousticModels_GetParameter(SR_AcousticModels* self, const LCHAR* key, LCHAR* value, size_t* len)
{
  SR_AcousticModelsImpl* impl = (SR_AcousticModelsImpl*) self;
  LCHAR* temp;
  ESR_ReturnCode rc;

  rc = HashMapGet(impl->parameters, key, (void **)&temp);
  if (rc == ESR_NO_MATCH_ERROR)
    CHKLOG(rc, ESR_SessionGetLCHAR(key, value, len));
  if (rc != ESR_SUCCESS)
  {
    PLogError(ESR_rc2str(rc));
    return rc;
  }
  if (LSTRLEN(temp) + 1 > *len)
  {
    *len = LSTRLEN(temp) + 1;
    PLogError(L("ESR_BUFFER_OVERFLOW"));
    return ESR_BUFFER_OVERFLOW;
  }
  *len = LSTRLEN(temp) + 1;
  LSTRCPY(value, temp);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_AcousticModels_GetCount(SR_AcousticModels* self, size_t* size)
{
  SR_AcousticModelsImpl* impl = (SR_AcousticModelsImpl*) self;
  ESR_ReturnCode rc;

  CHKLOG(rc, ArrayListGetSize(impl->acoustic, size));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_AcousticModels_GetID(SR_AcousticModels* self, size_t index,
                                       SR_AcousticModelID* id, size_t* size)
{
  /* TODO: complete */
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_AcousticModels_SetID(SR_AcousticModels* self, size_t index,
                                       SR_AcousticModelID* id)
{
  /* TODO: complete */
  return ESR_SUCCESS;
}

void* SR_AcousticModels_GetArbdata(SR_AcousticModels* self)
{
	SR_AcousticModelsImpl* impl = (SR_AcousticModelsImpl*)self;
	return impl? (void*)impl->arbdata : NULL;
}

/**
 * When AcousticModels are associated with a Recognizer, they initialize their
 * Pattern objects using that Recognizer.
 *
 * @param self SR_AcousticModels handle
 * @param recognizer The recognizer
 */
ESR_ReturnCode SR_AcousticModels_SetupPattern(SR_AcousticModels* self,
    SR_Recognizer* recognizer)
{
  CA_PatInputParams* patternParams = NULL;
  LCHAR mulname[P_PATH_MAX];
  LCHAR ldaname[P_PATH_MAX];
  SR_RecognizerImpl* recog;
  SR_AcousticModelsImpl* impl = (SR_AcousticModelsImpl*) self;
  CA_Acoustic* acoustic;
  size_t i, size, len;
  int dimen;
  ESR_ReturnCode rc;
  ESR_BOOL isPatternLoaded = ESR_FALSE;

  if (recognizer == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  recog = (SR_RecognizerImpl*) recognizer;

  impl->pattern = CA_AllocatePattern();
  if (impl->pattern == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  patternParams = CA_AllocatePatternParameters();
  if (patternParams == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }

  CHKLOG(rc, SR_AcousticModels_LoadLegacyPatternParameters(patternParams));
  dimen = CA_GetFrontendUtteranceDimension(recog->frontend);

  LSTRCPY(mulname, L(""));

  len = P_PATH_MAX;
  CHKLOG(rc, ESR_SessionGetLCHAR(L("cmdline.lda"), (LCHAR*) &ldaname, &len));
  len = P_PATH_MAX;
  CHKLOG(rc, ESR_SessionPrefixWithBaseDirectory(ldaname, &len));

  CA_LoadPattern(impl->pattern, patternParams, dimen, mulname, ldaname);
  isPatternLoaded = ESR_TRUE;
  CHKLOG(rc, impl->acoustic->getSize(impl->acoustic, &size));
  for (i = 0; i < size; ++i)
  {
    CHKLOG(rc, impl->acoustic->get(impl->acoustic, i, (void **)&acoustic));
    CA_SetupPatternForAcoustic(impl->pattern, acoustic);
  }
  CA_FreePatternParameters(patternParams);
  return ESR_SUCCESS;
CLEANUP:
  if (impl->pattern != NULL)
  {
    if (isPatternLoaded == ESR_TRUE)
      CA_UnloadPattern(impl->pattern);
    CA_FreePattern(impl->pattern);
  }
  if (patternParams != NULL)
    CA_FreePatternParameters(patternParams);
  return rc;
}

/**
 * When AcousticModels are deassociated with a Recognizer, they deinitialize their
 * Pattern objects.
 *
 * @param self SR_AcousticModels handle
 */
ESR_ReturnCode SR_AcousticModels_UnsetupPattern(SR_AcousticModels* self)
{
  SR_AcousticModelsImpl* impl = (SR_AcousticModelsImpl*) self;
  CA_Acoustic* acoustic;
  size_t i, size;
  ESR_ReturnCode rc;

  CHKLOG(rc, impl->acoustic->getSize(impl->acoustic, &size));
  for (i = 0; i < size; ++i)
  {
    CHKLOG(rc, impl->acoustic->get(impl->acoustic, i, (void **)&acoustic));
    CA_ClearPatternForAcoustic(impl->pattern, acoustic);
  }
  CA_UnloadPattern(impl->pattern);
  CA_FreePattern(impl->pattern);
  impl->pattern = NULL;
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}
