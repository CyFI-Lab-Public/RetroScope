/*---------------------------------------------------------------------------*
 *  srec_debug.c  *
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

#include "srec_debug.h"
#include "passert.h"
#include "portable.h"
#include "srec_tokens.h"


static char* sprint_altwords(srec* rec, altword_token* awtoken, char* buf)
{
  char *bufp = &buf[0];

  if (awtoken == AWTNULL) buf[0] = 0;
  else
  {
    for (; awtoken; awtoken = awtoken->next_token)
    {
      bufp += sprintf(bufp, "%d,", awtoken->word);
      ASSERT(awtoken->costdelta != MAXcostdata);
    }
    if (bufp > &buf[0]) *(bufp - 1) = 0;
  }
  return &buf[0];
}

void print_fsmnode_token(srec* rec, ftokenID token_index, char* msg)
{
  fsmnode_token *ftoken;
  char word_backtrace_trans[512];
  char *p;
  char buf[64];
  if (token_index == MAXftokenID)
  {
    printf("%sftoken %d\n", msg, token_index);
    return;
  }
  ftoken = &rec->fsmnode_token_array[token_index];
  printf("%sftoken %d rec %d@%d fsmnode %d cost %d word %d(%s) word_backtrace %d next_token_index %d ", msg, token_index, rec->id, rec->current_search_frame, ftoken->FSMnode_index, ftoken->cost, ftoken->word,
         sprint_altwords(rec, ftoken->aword_backtrace, buf),
         ftoken->word_backtrace, ftoken->next_token_index);         
  
  p = "null";
  if (ftoken->word < rec->context->olabels->num_words)
    p = rec->context->olabels->words[ftoken->word];
  sprint_bword_token_backtrace(word_backtrace_trans, sizeof(word_backtrace_trans), rec, ftoken->word_backtrace);
  printf(" [%s] %s\n", p, word_backtrace_trans);
}

void print_fsmnode_token_list(srec* rec, stokenID token_index, char* msg)
{
  printf("%s", msg);
  
  while (token_index != MAXftokenID)
  {
    fsmnode_token* ftoken = &rec->fsmnode_token_array[token_index];
    print_fsmnode_token(rec, token_index, "");
    token_index = ftoken->next_token_index;
  }
}

void print_search_status(srec* rec)
{
  int count, count2;
  printf("SEARCH STATUS .. frame %d\n", rec->current_search_frame);
  printf("prune_delta %d active_fsmarc_tokens %d\n",
         rec->prune_delta, rec->active_fsmarc_tokens);
  printf("num_new_states %d/%d fsmarc_token_array_size %d freelist %d\n", rec->num_new_states, rec->max_new_states, rec->fsmarc_token_array_size, rec->fsmarc_token_freelist);
  printf("active_fsmnode_tokens %d num_models %d fsmnode_token_array_size %d freelist %d\n", rec->active_fsmnode_tokens, rec->num_model_slots_allocated, rec->fsmnode_token_array_size, rec->fsmnode_token_freelist);
  count = count_fsmnode_token_list(rec, rec->active_fsmnode_tokens);
  count2 = count_fsmarc_token_list(rec, rec->active_fsmarc_tokens);
  printf("number active: %d fsmnodes %d fsmarcs\n", count, count2);
}

void print_fsmarc_token(srec* rec, stokenID token_index, char* msg)
{
  int i;
  srec_context *context = rec->context;
  
  fsmarc_token* stoken = &rec->fsmarc_token_array[token_index];
  FSMarc* arc = &context->FSMarc_list[stoken->FSMarc_index];
  costdata* costs = &stoken->cost[0];
  wordID* wordids = &stoken->word[0];
	frameID* duration = &stoken->duration[0];
  wtokenID* word_backtrace = &stoken->word_backtrace[0];
  bigcostdata cost_offset = rec->accumulated_cost_offset[rec->current_search_frame-1];
  char word_backtrace_trans[256];
  
  printf("%sstoken %4d at arc %4d ilabel %4d nextnode %4d states", msg, token_index,
         stoken->FSMarc_index, arc->ilabel, arc->to_node);
  for (i = 0; i < stoken->num_hmm_states; i++)
  {
    char buf[64];
    char *p = "null";
    if (wordids[i] < context->olabels->num_words) p = context->olabels->words[wordids[i]];
    sprint_bword_token_backtrace(word_backtrace_trans, 256, rec, word_backtrace[i]);
    printf(" w%d(%s)/%s/c%d/C%d/B%d/%d(%s)", wordids[i],
           sprint_altwords(rec, stoken->aword_backtrace[i], buf),
           p, costs[i], costs[i] + cost_offset, word_backtrace[i], duration[i], word_backtrace_trans);
  }
  printf("\n");
}

void print_fsmarc_token_list(srec* rec, stokenID token_index, char* msg)
{
  printf("%s", msg);
  while (token_index != MAXstokenID)
  {
    fsmarc_token* stoken = &rec->fsmarc_token_array[token_index];
    print_fsmarc_token(rec, token_index, "");
    token_index = stoken->next_token_index;
  }
}
