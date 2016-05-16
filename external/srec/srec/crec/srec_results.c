/*---------------------------------------------------------------------------*
 *  srec_results.c  *
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

#include"passert.h"

#include"portable.h"
#include"srec.h"
#include"search_network.h"
#include"srec_stats.h"
#if USE_COMP_STATS
#include"comp_stats.h"
#endif
#include"srec_results.h"

static srec* WHICH_RECOG(multi_srec* recm)
{
  srec* return_rec = NULL;
  costdata current_best_cost = MAXcostdata;
  int i = 0;
#if DO_ALLOW_MULTIPLE_MODELS
  for (i = 0; i < recm->num_activated_recs; i++)
  {
#endif
    if (current_best_cost > recm->rec[i].current_best_cost)
    {
      current_best_cost = recm->rec[i].current_best_cost;
      return_rec = &recm->rec[i];
    }
#if DO_ALLOW_MULTIPLE_MODELS
  }
#endif
  return return_rec;
}

int srec_get_bestcost_recog_id(multi_srec* recm, int* id)
{
  srec* rec = WHICH_RECOG(recm);
  if (!rec) *id = -1;
  else *id = rec->id;
  return 0;
}

void srec_result_strip_slot_markers(char* result)
{
  if (!result) return;
  else
  {
    char *p = result, *q = p;
    for (; (*q = *p); q++, p++)
    {
      if (p[0] == IMPORTED_RULES_DELIM && (p[2] == ' ' || p[2] == '\0'))
      {
        p += 2;
        *q = *p;
      }
    }
  }
}

int srec_has_results(multi_srec* recm)
{
  srec* rec = WHICH_RECOG(recm);
  frameID end_frame;
  if (!rec)
    return 0;
  end_frame = rec->current_search_frame;
  if (!rec->srec_ended)
    return 0;
  if (rec->word_lattice->words_for_frame[end_frame] != MAXwtokenID)
    return 1;
  if (rec->astar_stack->num_complete_paths)
    return 1;
  return 0;
}

int srec_clear_results(multi_srec* recm)
{
  srec* rec = WHICH_RECOG(recm);
  frameID ifr;
  SREC_STATS_SHOW();
  SREC_STATS_CLEAR();

  if (!rec)
    return 1;
  astar_stack_clear(rec->astar_stack);
  for (ifr = 0; ifr <= rec->current_search_frame; ifr++)
    rec->word_lattice->words_for_frame[ifr] = MAXwtokenID;

  return 0;
}

void* srec_nbest_prepare_list(multi_srec* recm, int n, asr_int32_t* bestcost)
{
  int rc;
  srec* rec = WHICH_RECOG(recm);
  AstarStack* stack = rec ? rec->astar_stack : 0;

  if (!stack)
    return NULL;
#if USE_COMP_STATS
  start_cs_clock1(&comp_stats->astar);
#endif
  rc = astar_stack_prepare(stack, n, rec);
  if (rc)
  {
    *bestcost = MAXbcostdata;
    return (void*)rec;
  }
  astar_stack_do_backwards_search(rec, n);
#if USE_COMP_STATS
  end_cs_clock1(&comp_stats->astar, 1);
#endif
  if (stack->num_complete_paths)
  {
    *bestcost = stack->complete_paths[0]->costsofar;
  }
  else
  {
    *bestcost = MAXbcostdata;
  }

  return (void*)(rec);
}

void srec_nbest_destroy_list(void* rec_void)
{
  srec* rec = (srec*)rec_void;
  AstarStack* stack = rec ? rec->astar_stack : 0;
  astar_stack_clear(stack);
}

int srec_nbest_get_num_choices(void* rec_void)
{
  srec* rec = (srec*)rec_void;
  AstarStack* stack = rec ? rec->astar_stack : 0;
  return stack ? stack->num_complete_paths : 0;
}

int srec_nbest_put_confidence_value(void* rec_void, int choice, int confidence_value)
{
  srec* rec = (srec*)rec_void;
  AstarStack* stack = rec ? rec->astar_stack : 0;
  if (!stack)
  {
	  return 1;
  }
  else
  {
  stack->complete_path_confidences[choice] = confidence_value;
  return 0;
  }
}

int srec_nbest_get_confidence_value(void* rec_void, int choice)
{
  srec* rec = (srec*)rec_void;
  AstarStack* stack = rec ? rec->astar_stack : 0;
  return stack->complete_path_confidences[choice];
}

int srec_nbest_fix_homonym_confidence_values(void* rec_void)
{
  int i, num_fixed = 0;
  srec* rec = (srec*)rec_void;
  AstarStack* stack = rec ? rec->astar_stack : 0;
  if (!stack)
    return num_fixed;
  for(i=1; i<stack->num_complete_paths; i++) {
    partial_path* parp = stack->complete_paths[i];
    for (; parp; parp = parp->next) {
      word_token* wtoken = &rec->word_token_array[ parp->token_index];
      if(WORD_TOKEN_GET_HOMONYM( wtoken)) {
        stack->complete_path_confidences[i] = stack->complete_path_confidences[i-1];
        num_fixed++;
        break;
      }
    }
  }
  return num_fixed;
}

LCHAR* srec_nbest_get_word(void* nbest, size_t choice)
{
  srec* rec = (srec*)nbest;
  return rec->context->olabels->words[choice];
}

int srec_nbest_remove_result(void* rec_void, int n)
{
  int i;
  srec* rec = (srec*)rec_void;
  AstarStack* stack = rec ? rec->astar_stack : 0;


  if (!stack || n < 0 || n >= stack->num_complete_paths)
  {
    return 0; /* out of range error */
  }

  /* free the partial_path which represents the entry */
  free_partial_path(stack, stack->complete_paths[n]);

  /* now I need to move everybody up one so I do not have a hole
     in the middle of my nbest list */
  for (i = n + 1 ; i < stack->num_complete_paths; i++)
    stack->complete_paths[i-1] = stack->complete_paths[i];
  stack->complete_paths[i-1] = 0; /* empty the last one */

  /* finally change the size of my nbest list */
  stack->num_complete_paths--;

  return 1;
}

ESR_ReturnCode srec_nbest_get_resultWordIDs(void* rec_void, size_t index, wordID* wordIDs, size_t* len, asr_int32_t* cost)
{
  const srec* rec = (srec*) rec_void;
  AstarStack* stack = rec ? rec->astar_stack : 0;
  partial_path* parp;
  wordID id;
  size_t currentLen = 0;

  if (!stack || index >= (size_t) stack->num_complete_paths)
  {
    if (wordIDs) *wordIDs = MAXwordID;
    if (len) *len = 0;
    *cost = MAXbcostdata;
    return ESR_ARGUMENT_OUT_OF_BOUNDS; /* out of range error */
  }

  parp = stack->complete_paths[index];
  *cost = stack->complete_paths[index]->costsofar;
  if (len == NULL || wordIDs == NULL)
    return ESR_SUCCESS;
  if (parp && parp->word == rec->context->beg_silence_word)
    parp = parp->next;
  while (parp)
  {
    id = parp->word;
    if (id == rec->context->end_silence_word)
      break;

    if (currentLen >= *len)
    {
      *wordIDs = MAXwordID;
      *len = currentLen + 1;
      return ESR_BUFFER_OVERFLOW; /* too little space error */
    }
    *wordIDs = id;
    ++wordIDs;
    ++currentLen;
    parp = parp->next;
  }
  --currentLen;

  if (currentLen >= *len)
  {
    *wordIDs = MAXwordID;
    *len = currentLen + 1;
    return ESR_BUFFER_OVERFLOW; /* too little space error */
  }
  *wordIDs = MAXwordID;
  *len = currentLen + 1;
  return ESR_SUCCESS;
}

int srec_nbest_get_result(void* rec_void, int n, char* label, int label_len, asr_int32_t* cost, int whether_strip_slot_markers)
{
  const srec* rec = (srec*)rec_void;
  AstarStack* stack = rec ? rec->astar_stack : 0;
  partial_path* parp;
  word_token* wtoken;
  int return_len;

  if (!stack || n < 0 || n >= stack->num_complete_paths)
  {
    *label = 0;
    *cost = MAXbcostdata;
    return 1; /* out of range error */
  }

  return_len = 0;
  parp = stack->complete_paths[n];
  *cost = stack->complete_paths[n]->costsofar;
  for (; parp; parp = parp->next)
  {
    const char *p;
    int lenp;
    wtoken = &rec->word_token_array[ parp->token_index];
    p = "NULL";
    if (rec->context->olabels->words[wtoken->word])
      p = rec->context->olabels->words[wtoken->word];
    if (!strcmp(p, "-pau2-"))
      break;

    lenp = (char)strlen(p);
    if (return_len + lenp >= label_len)
    {
      *label = 0;
      return 1; /* too little space error */
    }
    strcpy(label + return_len, p);
    return_len += lenp;
    if (whether_strip_slot_markers)
    {
      if (label[return_len-2] == IMPORTED_RULES_DELIM)
      {
        label[return_len-2] = 0;
        return_len -= 2;
      }
    }

#define SHOW_END_TIMES 1
#if SHOW_END_TIMES
    {
      char et[16];
      lenp = sprintf(et, "@%d", wtoken->end_time);
      if (return_len + lenp >= label_len)
        return 0;
      strcpy(label + return_len, et);
      return_len += lenp;
    }
#endif
    lenp = 1;
    if (return_len + lenp >= label_len)
      return 0; /* too little space error */
    strcpy(label + return_len, " ");
    return_len += lenp;
  }
  *(label + return_len) = 0;
  return 0;
}

int srec_nbest_get_choice_info(void* rec_void, int ibest, asr_int32_t* infoval, char* infoname)
{
  srec* rec = (srec*)rec_void;
  AstarStack* stack = rec ? rec->astar_stack : 0;

  if (!stack)
    return 1;

  if (ibest < 0 || ibest >= stack->num_complete_paths)
    return 1;

  /*!strcmp(infoname,"num_speech_frames")||
    !strcmp(infoname,"speech_frames_cost"))*/
  if (1)
  {
    partial_path* parp = stack->complete_paths[ibest];
    frameID start_frame = MAXframeID;
    frameID i, end_frame = MAXframeID;
    frameID num_speech_frames;
    bigcostdata speech_frames_cost, start_cost = 0, end_cost = 0;
    word_token* wtoken;
    frameID num_words;

    for (num_words = 0 ; parp; parp = parp->next)
    {
      if (parp->token_index == MAXwtokenID) break;
      wtoken = &rec->word_token_array[ parp->token_index];
      if (wtoken->word == rec->context->beg_silence_word)
      {
        start_frame = wtoken->end_time;
        start_cost = wtoken->cost + rec->accumulated_cost_offset[ start_frame];
        num_words--;
      }
      else if (parp->next &&
               parp->next->token_index != MAXwtokenID &&
               rec->word_token_array[ parp->next->token_index].word == rec->context->end_silence_word)
      {
        end_frame = wtoken->end_time;
        end_cost = wtoken->cost + rec->accumulated_cost_offset[ end_frame];
        num_words--;
      }
      num_words++;
    }

    if (start_frame != MAXframeID && end_frame != MAXframeID)
    {
      num_speech_frames = (frameID)(end_frame - start_frame);
      speech_frames_cost = end_cost - start_cost;
#define WTW_AT_NNREJ_TRAINING 40
      speech_frames_cost = speech_frames_cost - (num_words + 1) * (rec->context->wtw_average - WTW_AT_NNREJ_TRAINING);
      if (!strcmp(infoname,  "num_speech_frames"))
        *infoval = num_speech_frames;
      else if (!strcmp(infoname, "speech_frames_cost"))
        *infoval = speech_frames_cost;
      else if (!strcmp(infoname, "gsm_states_score_diff"))
      {
        /* this is the best cost, unconstrained by state sequence */
        bigcostdata gsm_states_cost = 0;
        for (i = start_frame + 1; i <= end_frame; i++)
        {
          gsm_states_cost += rec->cost_offset_for_frame[i];
          *infoval = (asr_int32_t)speech_frames_cost - (asr_int32_t)gsm_states_cost;
        }
      }
      else if (!strcmp(infoname, "gsm_words_score_diff"))
      {
        /* this is the best cost, unconstrained by word sequence */
        /* we can do this with astar.c ... with some work */
        *infoval = 0;
      }
      else if (!strcmp(infoname, "num_words"))
      {
        *infoval = num_words;
      }
      else if (!strcmp(infoname, "gsm_cost"))
      {
        bigcostdata gsm_states_cost = 0;
        for (i = start_frame + 1; i <= end_frame; i++)
          gsm_states_cost += rec->best_model_cost_for_frame[i];
        *infoval = gsm_states_cost;
      }
      else if (!strcmp(infoname, "num_total_frames"))
      {
        *infoval = rec->current_search_frame;
      }
      else if (!strcmp(infoname, "gsm_cost_all_frames"))
      {
        bigcostdata gsm_states_cost = 0;
        for (i = 0; i < rec->current_search_frame; i++)
          gsm_states_cost += rec->best_model_cost_for_frame[i];
        *infoval = gsm_states_cost;
      }
      else if (!strcmp(infoname, "acoustic_model_index"))
      {
        *infoval = rec->id;
      }
      else
      {
        log_report("Error: srec_nbest_get_choice_info does not know re %s\n", infoname);
        return 1;
      }
    }
  }
  return 0;
}


int srec_nbest_sort(void* rec_void)
{
  srec* rec = (srec*)rec_void;
  AstarStack* stack = rec ? rec->astar_stack : 0;
  size_t i, j, n;
  partial_path* parp;

  if (!stack || stack->num_complete_paths < 1)
    return 0; /* out of range error */

  n = stack->num_complete_paths;

  /* bubble sort is fine */
  /* PLogError("** srec_nbest_sort **\n"); */
  for (i = 0;i < n;i++)
    for (j = i + 1;j < n;j++)
      if (stack->complete_paths[j]->costsofar < stack->complete_paths[i]->costsofar)
      {
        /* PLogMessage(" %d %d", stack->complete_paths[j]->costsofar,      stack->complete_paths[j]->costsofar); */
        parp = stack->complete_paths[i];
        stack->complete_paths[i] = stack->complete_paths[j];
        stack->complete_paths[j] = parp;
      }
  return 1;

}
