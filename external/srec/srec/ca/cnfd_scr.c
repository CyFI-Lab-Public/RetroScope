/*---------------------------------------------------------------------------*
 *  cnfd_scr.c  *
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

/* Mark - Jan 2002 */

#include "math.h"
#include "stdlib.h"
#include "stdio.h"
#include "simapi.h"
#include "pendian.h"
#include "portable.h"
#include "srec_results.h"
#include "ESR_Session.h"

#if USE_CONFIDENCE_SCORER

#ifdef SREC_ENGINE_VERBOSE_LOGGING
static const char* const conf_feature_names[12] =
  { "gdiff", "sd", "sd13", "spf", "abs", "gdiffpf", "gv" };
#endif

#define CONF_FEATURE_GDIFF           0
#define CONF_FEATURE_SCORE_DIFF      1
#define CONF_FEATURE_SCORE_DIFF13    2
#define CONF_FEATURE_SCORE_PER_FRAME 3
#define CONF_FEATURE_ABSOLUTE_SCORE  4
#define CONF_FEATURE_GDIFF_PER_FRAME 5
#define NUM_CONF_FEATURES            6
#if NUM_CONF_FEATURES != NCONFPARS
#error allocate difference in simapi.h
#endif

CA_ConfidenceScorer* CA_AllocateConfidenceScorer(void)
{
  CA_ConfidenceScorer *hConfidenceScorer;
  hConfidenceScorer = NULL;

  TRY_CA_EXCEPT

  hConfidenceScorer = (CA_ConfidenceScorer *) CALLOC_CLR(1,
                      sizeof(CA_ConfidenceScorer), "ca.hConfidenceScorer");

  return (hConfidenceScorer);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hConfidenceScorer)

}


void CA_FreeConfidenceScorer(CA_ConfidenceScorer* hConfidenceScorer)
{
  TRY_CA_EXCEPT

  ASSERT(hConfidenceScorer);

  CA_UnloadConfidenceScorer(hConfidenceScorer);

  FREE((char *) hConfidenceScorer);
  return;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hConfidenceScorer);
}


int CA_LoadConfidenceScorer(CA_ConfidenceScorer* hConfidenceScorer)
{
  static char const * const names[NUM_CONF_FEATURES] = {
      "gdiff",
      "sd",
      "sd13",
      "spf",
      "abs",
      "gdiffpf",
  };
  int i, j;
  
  for (j = 0; j < 2; j++) {
    for (i = 0; i < NUM_CONF_FEATURES; i++) {
      char name[256];
      char value[256];
      Confidence_model_parameters* params;
      size_t len;

      if (j == 0) {
        params = &hConfidenceScorer->one_nbest;
        sprintf(name, "SREC.Confidence.sigmoid_param.%s.one_nbest", names[i]);
      }
      else {
        params = &hConfidenceScorer->many_nbest;
        sprintf(name, "SREC.Confidence.sigmoid_param.%s.many_nbest", names[i]);
      }
      len = P_PATH_MAX;
      if (ESR_SUCCESS != ESR_SessionGetLCHAR(name, (LCHAR*) value, &len)) {
        return ESR_FALSE;
      }
      if (3 != sscanf(value, "%lg %lg %lg",
          &params->scale[i], &params->offset[i], &params->weight[i])) {
        return ESR_FALSE;
      }
    }
  }
  return ESR_TRUE;
}


void CA_UnloadConfidenceScorer(CA_ConfidenceScorer* hConfidenceScorer)
{
  ASSERT(hConfidenceScorer);
}



static int CA_ConfScorerGetFeatures(CA_Recog* recog, CA_NBestList *nbestlist, float* features, int *num_features,
									int choice_number, int num_choices_left);

int CA_ComputeConfidenceValues(CA_ConfidenceScorer* hConfidenceScorer, CA_Recog* recog,
                                                            CA_NBestList *nbestlist)
{
  float features[12];
  double value=1.0, final_value, confidence_value;
  double confidence_feature, confidence_feature_weighted;
  int i, num_features,current_choice;
  int rc, error_check;
  int num_choices,num_choices_left;

  confidence_value = 1.0;
  num_choices_left = num_choices = srec_nbest_get_num_choices(nbestlist);

  for(current_choice=0;current_choice<num_choices;current_choice++)
    {
      confidence_value = 1.0;
      rc = CA_ConfScorerGetFeatures(recog, nbestlist, features, &num_features, current_choice, num_choices_left);
      if (rc)
        {
          PLogError("confscor failed\n");
          error_check = srec_nbest_put_confidence_value(nbestlist, current_choice, 0);
          if(error_check)
            return 1;
          num_choices_left--;
          continue;
        }

      if (num_choices_left == 1)
        {
          for (i=0;i<NUM_CONF_FEATURES;i++) {
            if(i==CONF_FEATURE_SCORE_DIFF || i==CONF_FEATURE_SCORE_DIFF13) {
              confidence_feature_weighted = 1.0;
            }
            else {
              confidence_feature = 1.0/(1.0 + exp((hConfidenceScorer->one_nbest.scale[i] *
                                                   features[i]) + hConfidenceScorer->one_nbest.offset[i]));
              confidence_feature_weighted = pow(confidence_feature,hConfidenceScorer->one_nbest.weight[i]);
            }
            confidence_value = confidence_value * confidence_feature_weighted;
          }
        }
      else
        {
          for (i=0;i<NUM_CONF_FEATURES;i++) {
            confidence_feature = 1.0/(1.0 + exp((hConfidenceScorer->many_nbest.scale[i] *
                                                 features[i]) + hConfidenceScorer->many_nbest.offset[i]));
            confidence_value = confidence_value * pow(confidence_feature, hConfidenceScorer->many_nbest.weight[i]);
          }
        }

      value *= confidence_value;
      final_value = 1000.0 * value;
      error_check = srec_nbest_put_confidence_value(nbestlist, current_choice, (int)final_value);
      if(error_check)
        return 1;
      num_choices_left--;
	}
	num_choices_left = srec_nbest_fix_homonym_confidence_values( nbestlist);
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("confidence %d features ", (int)final_value);
  for (i = 0; i < num_features; i++)
    PLogMessage(" %s %f", conf_feature_names[i], features[i]);
#endif

  return 0;
}

int CA_ConfScorerGetFeatures(CA_Recog* recog, CA_NBestList *nbestlist, float* features, int *num_features,
							 int choice_number, int num_choices_left)
{
  //static char* rejfeat_type = (char*) - 1;
  int rc;
  asr_int32_t num_speech_frames = 400;
#define MAX_ENTRY_LENGTH 512
  char label0[MAX_ENTRY_LENGTH];
  char label1[MAX_ENTRY_LENGTH];
  asr_int32_t cost0, cost1, cost2;
  asr_int32_t speech_cost0;
  asr_int32_t gsm_cost = 0, am_index = 0, num_words = 0;

  if (!nbestlist)
    return 1;

  ASSERT(features);
  ASSERT(num_features);
  ASSERT(nbestlist);
  ASSERT(recog);

  /* @F=(,"gdiff","sdiff12","sdiff13","spf1","speechcost0","gdiffpf");
            0       1          2        3         4           5      */
 if (num_choices_left > 0)
  {
    rc = srec_nbest_get_result(nbestlist, choice_number, label0, MAX_ENTRY_LENGTH, &cost0, choice_number);
    if (rc) return rc;
    rc = srec_nbest_get_choice_info(nbestlist, choice_number, &num_speech_frames, "num_speech_frames");
    if (rc) return rc;
    rc = srec_nbest_get_choice_info(nbestlist, choice_number, &speech_cost0, "speech_frames_cost");
    if (rc) return rc;
    ASSERT(!rc);
    features[CONF_FEATURE_ABSOLUTE_SCORE] = ((float)(speech_cost0));
    features[CONF_FEATURE_SCORE_PER_FRAME] = ((float)(speech_cost0)) / (float)num_speech_frames;
    if (num_choices_left> 1)
    {
      rc = srec_nbest_get_result(nbestlist, choice_number+1, label1, MAX_ENTRY_LENGTH, &cost1, choice_number);
      if (rc) return rc;
      features[CONF_FEATURE_SCORE_DIFF] = ((float)cost1 - (float)cost0);
      if (num_choices_left > 2)
      {
        rc = srec_nbest_get_result(nbestlist, choice_number+2, label1, MAX_ENTRY_LENGTH, &cost2, choice_number);
        if (rc) return rc;
        features[CONF_FEATURE_SCORE_DIFF13] = ((float)cost2 - (float)cost0);
      }
      else
      {
        cost2 = (-1);
        features[CONF_FEATURE_SCORE_DIFF13] = ((float)cost1 - (float)cost0);
      }
    }
    else
    {
      features[CONF_FEATURE_SCORE_DIFF] = 400;
      features[CONF_FEATURE_SCORE_DIFF13] = 400;
      cost1 = cost2 = (-1);
    }


    srec_nbest_get_choice_info(nbestlist, choice_number, &gsm_cost, "gsm_cost");
    srec_nbest_get_choice_info(nbestlist, choice_number, &am_index, "acoustic_model_index");
    srec_nbest_get_choice_info(nbestlist, choice_number, &num_words, "num_words");

    features[CONF_FEATURE_GDIFF] = (float)(speech_cost0 - gsm_cost);
    /* should never happen */
    if (num_speech_frames == 0) num_speech_frames = 1;
    features[CONF_FEATURE_GDIFF_PER_FRAME] = ((float)(speech_cost0 - gsm_cost)) / (float)num_speech_frames;
    // features[CONF_FEATURE_GENDER_VALUE] = (float)am_index;
    *num_features = NUM_CONF_FEATURES;
    rc = 0;
  }
  else
  {
    *num_features = 0;
    rc = 1;
  }

#define DUMP_FEATURES_FOR_RETRAIN 0
#if DUMP_FEATURES_FOR_RETRAIN
  srec_nbest_get_choice_info(nbestlist, 0, &gsm_cost, "gsm_cost");
  srec_nbest_get_choice_info(nbestlist, 0, &am_index, "acoustic_model_index");
  srec_nbest_get_choice_info(nbestlist, 0, &num_words, "num_words");

  printf("REJFEATS:");
  printf(" cost0(%d)", cost0);
  printf(" cost1(%d)", cost1);
  printf(" cost2(%d)", cost2);
  printf(" speechcost0(%d)", speech_cost0);
  printf(" nframes0(%d)", num_speech_frames);
  printf(" nwords0(%d)", num_words);
  printf(" gsmcost(%d)", gsm_cost);
  printf(" amindex(%d)", am_index);
  /* printf(" glmcost(%d)", glm_cost);*/
  printf("\n");
#endif
  return rc; /* no error reported */
}


#endif /* #if USE_CONFIDENCE_SCORER */

