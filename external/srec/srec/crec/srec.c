/*---------------------------------------------------------------------------*
 *  srec.c  *
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
#include <string.h>
#include <math.h>

#include "pstdio.h"
#include "passert.h"
#include "portable.h"

#include "hmm_desc.h"
#include "utteranc.h"
#include "hmmlib.h"
#include "srec_sizes.h"
#include "search_network.h"
#include "srec_context.h"
#include "srec.h"
#include "srec_stats.h"
#include "srec_debug.h"
#include "srec_tokens.h"
#include "word_lattice.h"
#include "swimodel.h"
#if USE_COMP_STATS
#include "comp_stats.h"
#endif
#include "c42mul.h"

#ifdef SET_RCSID
static const char *rcsid = 0 ? (const char *) &rcsid :
"$Id: srec.c,v 1.39.4.31 2008/06/23 17:20:39 dahan Exp $";
#endif

#define SILENCE_MODEL_INDEX 0
#define PRUNE_TIGHTEN 0.9     /*if we run out of room in the state arrays,
                                keep multiplying pruning thresh by this amount
                                until there is room */

/*--------------------------------------------------------------------------*
 *                                                                          *
 * protos                                                                   *
 *                                                                          *
 *--------------------------------------------------------------------------*/
static void reset_cost_offsets(multi_srec* rec, frameID current_search_frame,
                               costdata current_best_cost);
static void update_internal_hmm_states(srec *rec, costdata *pcurrent_prune_delta,
                                       costdata *pcurrent_best_cost,
                                       costdata *precomputed_model_scores);

/*--------------------------------------------------------------------------*
 *                                                                          *
 * utils                                                                    *
 *                                                                          *
 *--------------------------------------------------------------------------*/

static void dump_core(char *msg)
{
  PLogError ( msg );
  ASSERT(0);
}

static altword_token* reprune_altword_token_batch(srec* rec,
    altword_token* batch,
    bigcostdata costlimit)
{
  altword_token *awtoken, *next_awtoken;
  altword_token **awtokenp;

  /* look for previous invalidate, see below */
  if (batch->costbasis == MAXcostdata / 2)
  { /* was costdelta // costlimit */
    free_altword_token(rec, batch);
    return AWTNULL;
  }
  /* a flag to check whether we already pruned this batch would be nice */

  /* first prune the CDR of the list, ie everything but the head */
  awtokenp = &batch->next_token;
  for (awtoken = batch->next_token; awtoken != AWTNULL; awtoken = next_awtoken)
  {
    next_awtoken = awtoken->next_token;
    if ((bigcostdata)batch->costbasis + awtoken->costdelta > costlimit)
    {
      (*awtokenp) = awtoken->next_token;
      awtoken->refcount = 1;             /* to make sure it frees */
      free_altword_token(rec, awtoken);
    }
    else
      awtokenp = &awtoken->next_token;
  }

  /* now see about the CAR of the list, ie the head */
  if ((bigcostdata)(batch->costbasis) + batch->costdelta < costlimit)
  {
    /* head of list survives pruning => no problem */
  }
  else if (batch->next_token != AWTNULL)
  {
    /* head of list does not survive => since we can't change the pointer
       we copy a survivor into it and free that original */
    awtoken = batch->next_token;
    batch->costdelta      = awtoken->costdelta;
    batch->word           = awtoken->word;
    batch->word_backtrace = awtoken->word_backtrace;
    /*ASSERT( batch->refcount == awtoken->refcount); */
    /* batch->refcount       = awtoken->refcount; */
    batch->next_token     = awtoken->next_token;
    awtoken->refcount = 1; /* to make sure it frees */
    free_altword_token(rec, awtoken);
  }
  else
  {
    /* head of list does not survive, nothing survives, so invalidate it */
    batch->costbasis = MAXcostdata / 2; /* was costdelta */
    free_altword_token(rec, batch);
    batch = AWTNULL;
  }
  return batch;
}

static void reprune_altword_tokens(srec* rec)
{
  stokenID i, j;
  fsmarc_token* stoken;
  fsmnode_token* ftoken;
  bigcostdata current_prune_delta = rec->prune_delta;
  altword_token* awtoken;

  /* first clear the costbases */
  for (i = rec->active_fsmarc_tokens; i != MAXstokenID; i = stoken->next_token_index)
  {
    stoken = &rec->fsmarc_token_array[i];
    for (j = 0; j < stoken->num_hmm_states; j++)
      if ((awtoken = stoken->aword_backtrace[j]) != AWTNULL)
        awtoken->costbasis = MAXcostdata;
  }
  for (i = rec->active_fsmnode_tokens;i != MAXftokenID;i = ftoken->next_token_index)
  {
    ftoken = &rec->fsmnode_token_array[i];
    if ((awtoken = ftoken->aword_backtrace) != AWTNULL)
      awtoken->costbasis = MAXcostdata;
  }
  /* costbases for altword attached to stoken */
  for (i = rec->active_fsmarc_tokens; i != MAXstokenID; i = stoken->next_token_index)
  {
    stoken = &rec->fsmarc_token_array[i];
    for (j = 0; j < stoken->num_hmm_states; j++)
      if ((awtoken = stoken->aword_backtrace[j]) != AWTNULL)
        if (awtoken->costbasis > stoken->cost[j])
          awtoken->costbasis = stoken->cost[j];
  }
  /* costbases for altword attached to ftokens */
  for (i = rec->active_fsmnode_tokens;i != MAXftokenID;i = ftoken->next_token_index)
  {
    ftoken = &rec->fsmnode_token_array[i];
    if ((awtoken = ftoken->aword_backtrace) != AWTNULL)
      if (awtoken->costbasis > ftoken->cost)
        awtoken->costbasis = ftoken->cost;
  }

  /* now reprune */
  while (rec->altword_token_freelist_len < rec->altword_token_array_size / 4
         || rec->altword_token_freelist_len < 2*rec->word_priority_q->max_in_q)
  {
    SREC_STATS_INC_AWTOKEN_REPRUNES(1);
    current_prune_delta = (costdata)(PRUNE_TIGHTEN * PRUNE_TIGHTEN * current_prune_delta);
    for (i = rec->active_fsmarc_tokens; i != MAXstokenID; i = stoken->next_token_index)
    {
      stoken = &rec->fsmarc_token_array[i];
      for (j = 0; j < stoken->num_hmm_states; j++)
      {
        if (stoken->aword_backtrace[j] != AWTNULL)
        {
          stoken->aword_backtrace[j] =
            reprune_altword_token_batch(rec, stoken->aword_backtrace[j],
                                        current_prune_delta);
        }
      }
    }
    for (i = rec->active_fsmnode_tokens;i != MAXftokenID;i = ftoken->next_token_index)
    {
      ftoken = &rec->fsmnode_token_array[i];
      if (ftoken->aword_backtrace != AWTNULL)
      {
        ftoken->aword_backtrace =
          reprune_altword_token_batch(rec, ftoken->aword_backtrace,
                                      current_prune_delta);
      }
    }
    ASSERT(current_prune_delta > 0);
  }
}

#define refcopy_altwords(rEc, aWtOkEn) (aWtOkEn?(aWtOkEn->refcount++,aWtOkEn):aWtOkEn)

static altword_token* copy_altwords(srec* rec, altword_token* list1, costdata delta)
{
  altword_token *q2, dummy, *last_q2 = &dummy;
  costdata q2_costdelta;

  /* first check for space */
#if BUILD & BUILD_DEBUG
  int num = 0;

  for (num = 0, q2 = list1; q2 != AWTNULL; q2 = q2->next_token)
    num++;
  if (num > rec->altword_token_freelist_len)
  {
    printf("warning: mid-copy reprune_altword_tokens()\n");
    ASSERT(0);
    reprune_altword_tokens(rec);
  }
#endif

  /* now do the copy */
  for (; list1 != AWTNULL; list1 = list1->next_token)
  {
    ASSERT(list1->refcount >= 1);

    q2_costdelta = list1->costdelta + delta;
    ASSERT(list1->costdelta != MAXcostdata);
    if (q2_costdelta > rec->prune_delta)
      continue;
    q2 = get_free_altword_token(rec, NULL_IF_NO_TOKENS);
    if (!q2) /* this should never happen */
      break;
    last_q2->next_token = q2;
    q2->costdelta      = q2_costdelta;
    q2->word           = list1->word;
    q2->word_backtrace = list1->word_backtrace;
    last_q2 = q2;
  }
  last_q2->next_token = AWTNULL;
  return dummy.next_token;
}

#if 1
/* sizewise_altwords just makes sure the list of altwords is no longer than
   the number of possible word ends.  Any tokens beyond that length will get
   ignored later anyways, so we may as well kill them here.
   This also is related to the anticipated repruning.  This code currently
   makes use of calloc/free and qsort, but can easily be rewritten to just
   to a linear in-place sort, linear looking for the 10 best score should not
   take too long. This is on the todo list!
*/
int altword_token_ptr_cmp(const void* a, const void* b)
{
  const altword_token** A = (const altword_token**)a, **B = (const altword_token**)b;
  if ((*A)->costdelta > (*B)->costdelta) return 1;
  else if ((*A)->costdelta < (*B)->costdelta) return -1;
  else return 0;
}
static altword_token* sizewise_altwords(srec* rec, altword_token* awtoken_head)
{
#define SIZEWISE_THRESH (rec->word_priority_q->max_in_q)
#define SIZEWISE_TARGET (rec->word_priority_q->max_in_q*4/5)
  int i, num;
  altword_token *awtoken, **list;

  if( SIZEWISE_TARGET == 0) {
	  free_altword_token(rec, awtoken_head);
	  return NULL;
  }
  num = count_altword_token(rec, awtoken_head);
  /* if the linked list is shorter than max_in_q we're fine */
  if (num <= SIZEWISE_THRESH)
    return awtoken_head;
  else
  {
    list = (altword_token**)CALLOC(num, sizeof(altword_token*), L("search.srec.altword_tokens"));
    ASSERT(awtoken_head->refcount == 1);
    for (i = 0, awtoken = awtoken_head; i < num; i++, awtoken = awtoken->next_token)
      list[i] = awtoken;
    qsort(list, num, sizeof(altword_token*), altword_token_ptr_cmp);
    for (i = 0; i < SIZEWISE_TARGET; i++)
      list[i]->next_token = list[i+1];
    if(i>0) list[i-1]->next_token = AWTNULL;
    for (; i < num; i++)
    {
      list[i]->refcount = 1; /* make sure it frees */
      free_altword_token(rec, list[i]);
    }
    awtoken_head = list[0];
    awtoken_head->refcount = 1;
    FREE(list);
    return awtoken_head;
  }
}
#else
#define sizewise_altwords(ReC,hEad) hEad
#endif

/*--------------------------------------------------------------------------*
 *                                                                          *
 * acoustic scoring utils                                                   *
 *                                                                          *
 *--------------------------------------------------------------------------*/

#define DO_COMPUTE_MODEL     0
#define DO_NOT_COMPUTE_MODEL MAXcostdata

static asr_uint16_t best_uint16(asr_uint16_t* p, int n)
{
  asr_uint16_t rv = p[0];
  for (;--n > 0;p++) if (rv > *p) rv = *p;
  return rv;
}
static int compute_model_scores(costdata *current_model_scores, const SWIModel *acoustic_models,
                                pattern_info *pattern, frameID current_search_frame)
{
  int i;
  int num_models_computed = 0;

  for (i = 0; i < acoustic_models->num_hmmstates; i++)
  {
    if (current_model_scores[i] == DO_COMPUTE_MODEL)
    {
      scodata score = mixture_diagonal_gaussian_swimodel(pattern->prep,
              &acoustic_models->hmmstates[i], acoustic_models->num_dims);
      ASSERT(score <= 0 && "model score out of range");

      current_model_scores[i] = (costdata) - score;
      num_models_computed++;
    }
  }
  return num_models_computed;
}



/*precompute all needed models to be used by next frame of search*/

static int find_which_models_to_compute(srec *rec, const SWIModel *acoustic_models)
{
  int i;
  modelID model_index;
  stokenID current_token_index;
  ftokenID current_ftoken_index;
  fsmarc_token *current_token;
  fsmnode_token *current_ftoken;
  costdata *current_model_scores;
  /* arcID arc_index; */
  arcID fsm_arc_index;
  HMMInfo* hmm_info;
  FSMnode* fsm_node;
  FSMarc* fsm_arc;
  /*use the current_model_scores array both to tell the model computing stuff
    what models to compute and to get the scores back.  This is a bit ugly, but
    saves having another array to allocate*/

  /* this belongs elsewhere at initialization,
     eg. where we'll associate search to acoustic models
  */
  rec->avg_state_durations = acoustic_models->avg_state_durations;

  current_model_scores = rec->current_model_scores;

  for (model_index = 0; model_index < acoustic_models->num_hmmstates; model_index++)
  {
    current_model_scores[model_index] = DO_NOT_COMPUTE_MODEL;
  }

  current_token_index = rec->active_fsmarc_tokens;

  while (current_token_index != MAXstokenID)
  {
    current_token = &(rec->fsmarc_token_array[current_token_index]);
    /*need to compute all models needed within this HMM*/
    fsm_arc = &rec->context->FSMarc_list[ current_token->FSMarc_index];
    hmm_info = &rec->context->hmm_info_for_ilabel[fsm_arc->ilabel];

    /*handle all states that are alive in this hmm*/
    for (i = 0;i < hmm_info->num_states;i++)
    {
      if ((current_token->cost[i] != MAXcostdata) ||
          ((i > 0) && current_token->cost[i-1] != MAXcostdata))
      {
        model_index = hmm_info->state_indices[i];
        current_model_scores[model_index] = DO_COMPUTE_MODEL;
      }
    }
    current_token_index = current_token->next_token_index;
  }

  /*for each active FSM node, find models which can come from node*/

  current_ftoken_index = rec->active_fsmnode_tokens;

  while (current_ftoken_index != MAXftokenID)
  {
    current_ftoken = &(rec->fsmnode_token_array[current_ftoken_index]);
    fsm_node = &rec->context->FSMnode_list[current_ftoken->FSMnode_index];
    fsm_arc = NULL;
    for (fsm_arc_index = fsm_node->un_ptr.first_next_arc; fsm_arc_index != MAXarcID;
        fsm_arc_index = fsm_arc->linkl_next_arc) {
      fsm_arc = rec->context->FSMarc_list+fsm_arc_index;

      if (fsm_arc->ilabel != MAXlabelID)
      {
        hmm_info = &rec->context->hmm_info_for_ilabel[fsm_arc->ilabel];
        if (hmm_info->num_states > 0)
        {

          /* we should build in here a check that this arc has reasonable weight */
          /* if(fsm_arc->cost < rec->prune_delta)  */
          current_model_scores[hmm_info->state_indices[0]] = DO_COMPUTE_MODEL;
        }
      }
    }
    current_ftoken_index = current_ftoken->next_token_index;
  }

  /*compute the scores in a batch - this allows the model computing code to
    chunk it up however it wants*/
  return 0;
}

/*--------------------------------------------------------------------------*
 *                                                                          *
 * pruning utils                                                            *
 *                                                                          *
 *--------------------------------------------------------------------------*/

/*this is called at the end of the search*/
static int prune_new_tokens(srec *rec, costdata current_prune_thresh)
{
  int i;
  nodeID num_deleted;
  stokenID token_index;
  fsmarc_token *token;
  stokenID next_token_index;
  stokenID *list_head_pointer;
  char any_alive;

  num_deleted = 0;
  list_head_pointer = &(rec->active_fsmarc_tokens);

  for (token_index = rec->active_fsmarc_tokens; token_index != MAXstokenID;
       token_index = next_token_index)
  {

    token = &(rec->fsmarc_token_array[token_index]);
    next_token_index = token->next_token_index;

    any_alive = 0;

    for (i = 0;i < token->num_hmm_states;i++)
    {
      if (token->cost[i] < current_prune_thresh)
      {
        any_alive = 1;
      }
    }

    if (!any_alive)
    { /*everything pruned so recylce the token*/
      *list_head_pointer = next_token_index;

      rec->best_token_for_arc[rec->fsmarc_token_array[token_index].FSMarc_index] = MAXstokenID;

      free_fsmarc_token(rec, token_index);

      num_deleted++;
      rec->num_new_states--;
    }
    else
    {
      list_head_pointer = &token->next_token_index;
    }
  }
  return num_deleted;
}

static void reprune_word_tokens_if_necessary(srec *rec)
{
  word_token* wtoken;
  wtokenID wtoken_index = rec->word_token_freelist;
  wtokenID num_free_wtokens = 0;

  for (; wtoken_index != MAXwtokenID; wtoken_index = wtoken->next_token_index)
  {
    wtoken = &rec->word_token_array[ wtoken_index];
    num_free_wtokens++;
  }
  if (num_free_wtokens < 2*rec->word_priority_q->max_in_q)
    reprune_word_tokens(rec, 0);
}

/*this is called when we run out of room in the state token arrays and need to make more room -
 it only prunes in theh new time slice - we could also look at pruning the previous slice if needed*/


static costdata reprune_new_states(srec *rec, costdata current_best_cost, costdata current_prune_delta)
{
  int num_deleted;
  /*first check to see if current pruning thresholds make enough room
    (because of better max)*/

  prune_new_tokens(rec, current_best_cost + current_prune_delta);

  ASSERT(((float)current_best_cost) + current_prune_delta < (float)SHRT_MAX);

  while ((rec->num_new_states >= rec->max_new_states - 1)
         || rec->fsmarc_token_freelist == MAXstokenID)
  {

    SREC_STATS_INC_STOKEN_REPRUNES(1);

    current_prune_delta = (costdata)(PRUNE_TIGHTEN * current_prune_delta);

    if (current_prune_delta <= 1)
    {
      /*this seems like an unlikely case, but if we can't prune enough to make room, give up
      on the utterance (better than being stuck here forever!)*/

      /*FIX replace with crec abort mechanism*/
      PLogError ("reprune_new_states: can't seem to prune enough - best cost %d num_new_states %d\n",
             current_best_cost, rec->num_new_states);

      print_fsmarc_token_list(rec, rec->active_fsmarc_tokens, "CANNOT PRUNE");

      dump_core("reprune died\n");
    }

    num_deleted = prune_new_tokens(rec, current_best_cost + current_prune_delta);

    ASSERT(((float)current_best_cost + current_prune_delta) < (float)USHRT_MAX);
  }
  return current_prune_delta;
}



static void prune_fsmnode_tokens(srec *rec, costdata current_prune_thresh, ftokenID not_this_one)
{
  nodeID num_deleted;
  ftokenID token_index;
  fsmnode_token *token;
  ftokenID next_token_index;
  ftokenID *list_head_pointer;

  num_deleted = 0;
  token_index = rec->active_fsmnode_tokens;

  token = &(rec->fsmnode_token_array[token_index]);
  list_head_pointer = &(rec->active_fsmnode_tokens);

  while (token_index != MAXftokenID)
  {
    next_token_index = token->next_token_index;
    if( token_index!=not_this_one && token->cost >= current_prune_thresh)
    {
      /*pruned so recycle the token*/
      *list_head_pointer = next_token_index;
      rec->best_token_for_node[token->FSMnode_index] = MAXftokenID;
      free_fsmnode_token(rec, token_index);
      num_deleted++;
    }
    else
    {
      list_head_pointer = &token->next_token_index;
    }
    token_index = next_token_index;
    token = &(rec->fsmnode_token_array[token_index]);
  }
}


/*this is called when we run out of room in the state token arrays and need to make more room -
 it only prunes in theh new time slice - we could also look at pruning the previous slice if needed*/


static costdata reprune_fsmnode_tokens(srec *rec, costdata current_best_cost, costdata current_prune_delta,
                                       ftokenID not_this_one)
{

  /*first check to see if current pruning thresholds make enough
    room (because of better max)*/

  prune_fsmnode_tokens(rec, current_best_cost+current_prune_delta, not_this_one);

  ASSERT((float)current_best_cost + (float)current_prune_delta < (float)SHRT_MAX);

  while (rec->fsmnode_token_freelist == MAXftokenID)
  {
    SREC_STATS_INC_FTOKEN_REPRUNES(1);

    current_prune_delta = (costdata)(PRUNE_TIGHTEN * current_prune_delta);

    if (current_prune_delta <= 1)
    {
      /*this seems like an unlikely case, but if we can't prune enough to make room, give up
      on the utterance (better than being stuck here forever!)*/

      /*FIX replace with crec abort mechanism*/
      PLogError ("reprune_fsmnode_tokens: can't seem to prune enough - best cost %d\n",
             current_best_cost);

      print_fsmnode_token_list(rec, rec->active_fsmnode_tokens, "CANNOT PRUNE: ");

      dump_core("reprune_fsmnode_tokens() died\n");
    }

    prune_fsmnode_tokens(rec, current_best_cost+current_prune_delta, not_this_one);
    ASSERT((float)current_best_cost + (float)current_prune_delta < (float)USHRT_MAX);
  }

  return current_prune_delta;
}


void reset_best_cost_to_zero(srec* rec, costdata current_best_cost)
{
  fsmarc_token* stoken;
  stokenID token_index;
  short i, num_states;


  /*do the state tokens*/
  for (token_index = rec->active_fsmarc_tokens;
       token_index != MAXstokenID;
       token_index = stoken->next_token_index)
  {

    stoken = &rec->fsmarc_token_array[ token_index];
    num_states = stoken->num_hmm_states;
    for (i = 0; i < num_states; i++)
    {
      if (stoken->cost[i] < MAXcostdata)
      {
        ASSERT(stoken->cost[i]  >= current_best_cost);
        stoken->cost[i] = (costdata)(stoken->cost[i] - (costdata) current_best_cost);
      }
    }
  }
}

static void reset_cost_offsets(multi_srec* rec, frameID current_frame,
                               costdata current_best_cost)
{
  rec->cost_offset_for_frame[current_frame] = current_best_cost;
  if (current_frame == 0)
    rec->accumulated_cost_offset[current_frame] = current_best_cost;
  else
    rec->accumulated_cost_offset[current_frame] = rec->accumulated_cost_offset[current_frame-1] + current_best_cost;
}


/*--------------------------------------------------------------------------*
 *                                                                          *
 * word_token functions                                                     *
 *                                                                          *
 *--------------------------------------------------------------------------*/

/*this function allocates a new word token and remembers it in a list in the srec structure (to be used for backtrace later on*/

static wtokenID create_word_token(srec *rec)
{
  wtokenID word_token_index;
  word_token *word_token;

  word_token_index = get_free_word_token(rec, NULL_IF_NO_TOKENS);

  if (0 && word_token_index == MAXwtokenID)
  {
    /*FIX - use crec error handling*/
    dump_core("create_word_token: cannot allocate word token - we need"
              " to figure out a word pruning strategy when this happens!\n");
  }

  word_token = &(rec->word_token_array[word_token_index]);

  return word_token_index;
}

/* gets rid of fsmnode which trace back to this word since
   the word is not goingto make it ono the word lattice */

static int block_fsmnodes_per_backtrace(srec *rec, wtokenID wtoken_id)
{
  int num_ftokens_blocked = 0;
  ftokenID current_token_index = rec->active_fsmnode_tokens;
  while (current_token_index != MAXftokenID) {
    fsmnode_token *token = &(rec->fsmnode_token_array[current_token_index]);
	if (token->word_backtrace == wtoken_id) {
      num_ftokens_blocked++;
      token->cost = MAXcostdata;
	}
	current_token_index = token->next_token_index;
  }
  return num_ftokens_blocked;
}

/* processing a word boundary,
   current_token is the fsmnode_token to the left of the boundary
   cost is the cost through this frame
   pcurrent_word_threshold is the worst score for words on the prio.q
   returns the word_token index just created
*/
static wtokenID srec_process_word_boundary_nbest(srec* rec,
    nodeID end_node,
    wordID word,
    wtokenID word_backtrace,
    costdata cost,
    costdata* pcurrent_word_threshold,
    int *any_nodes_blocked)
{
  wtokenID wtoken_index;
  wtokenID return_wtoken_index;
  wtokenID token_id_to_remove;

  word_token* wtoken;

  if (word_backtrace != MAXwtokenID)
  {
    word_token* btoken = &rec->word_token_array[word_backtrace];
    if (btoken->end_time >= rec->current_search_frame)
    {
      SREC_STATS_INC_BAD_BACKTRACES();
      return MAXwtokenID;
    }
  }
  /*make new word token*/
  wtoken_index = create_word_token(rec);
  //ASSERT(wtoken_index != MAXwtokenID);
  if (wtoken_index == MAXwtokenID)
  {
    /* we could have called reprune_word_tokens() here, but we instead called it
       at the beginning of do_epsilon_updates() to avoid complications of
       re-pruning word tokens too deep in the search update */
    return_wtoken_index = MAXwtokenID;
    return return_wtoken_index;
  }

  wtoken = &(rec->word_token_array[wtoken_index]);
  wtoken->word = word;
  wtoken->_word_end_time = 0; // new
  wtoken->end_time = rec->current_search_frame;
  wtoken->end_node = end_node;
  wtoken->backtrace = word_backtrace;
  wtoken->cost = cost;

  token_id_to_remove = add_word_token_to_priority_q(rec->word_priority_q, wtoken_index, rec->word_token_array);

  if (token_id_to_remove == wtoken_index)
    return_wtoken_index = MAXwtokenID;
  else
  {
    /* the word was truly added to the priority q, so we must
       get the new worst word on that list */
    *pcurrent_word_threshold = get_priority_q_threshold(rec->word_priority_q, rec->word_token_array);
    return_wtoken_index = wtoken_index;
  }

  if (token_id_to_remove != MAXwtokenID)
  {
    /* Jean: must allow for this word_token to be recycled */

    /* ok, the word won't we maintained, so there's no point to
       continue to search this path (as headed by this fsmarc_token) */

      *any_nodes_blocked += block_fsmnodes_per_backtrace( rec, token_id_to_remove);

    /* we killed the fsmnode token associated with the word being removed.
       But, we didn't kill it's word backtrace, so there may be word tokens
       in the backtrace which cannot connect.  But we can't really kill
       the whole backtrace since it might be shared with other
       active states, Mike's idea is to add a counter on the
       word_tokens, which counts how many active paths are using
       this word_token ... TODO */

    /* print_word_token(rec, token_id_to_remove, "srec_process_word_boundary killed wtoken "); */
    free_word_token(rec, token_id_to_remove);

  }
  return return_wtoken_index;
}

ftokenID* srec_get_parent_for_active_fsmnode(srec* rec, ftokenID ftoken_index)
{
  ftokenID* parent_ftoken_index = &rec->active_fsmnode_tokens;
  while( (*parent_ftoken_index) != MAXftokenID) {
    fsmnode_token* parent_ftoken = &rec->fsmnode_token_array[ *parent_ftoken_index];
    if( *parent_ftoken_index == ftoken_index)
		return parent_ftoken_index;
    parent_ftoken_index = &parent_ftoken->next_token_index;
  }
  return NULL;
}

/*---------------------------------------------------------------------------*
 *                                                                           *
 * updates                                                                   *
 *                                                                           *
 *---------------------------------------------------------------------------*/


/*handles epsilon transitions (used for word boundaries).  Epsilons come from active
  fsmnode tokens and maximize into new FSMnode tokens (finds the best path to an FSM
  node).

  When we hit an
  epsilon, create a word token, put it in the path, and remember it in a
  list of all word tokens*/

static int do_epsilon_updates(srec *rec, costdata prune_delta,
                              costdata best_cost)
{
  fsmnode_token *new_ftoken;
  fsmnode_token *current_ftoken;
  wtokenID wtoken_index;
  FSMnode* fsm_node;
  FSMarc* fsm_arc;
  costdata cost, cost_with_wtw;
  ftokenID new_ftoken_index;
  ftokenID current_ftoken_index;
  costdata current_word_threshold;
  arcID fsm_arc_index;
  wordID word_with_wtw;
  int num_fsm_nodes_updated=0, num_fsm_nodes_blocked, num_fsm_nodes_blocked2;
  int num_wtokens_maybe_homonyms;
  costdata current_prune_delta;
  costdata current_prune_thresh;
  altword_token* awtoken;


  // printf("FRAME %d\n", rec->current_search_frame);
  // print_fsmnode_token_list(rec, rec->active_fsmnode_tokens, "BEFORE UPDATE_EPSILONS ACTIVE_FSMNODE_TOKENS: \n");

  current_word_threshold = MAXcostdata;
  current_prune_delta = prune_delta;
  current_prune_thresh = best_cost + current_prune_delta;

  current_ftoken_index = rec->active_fsmnode_tokens;
  while (current_ftoken_index != MAXftokenID)
  {
	//  print_fsmnode_token(rec, current_ftoken_index, "processing ... ");
    current_ftoken = &(rec->fsmnode_token_array[current_ftoken_index]);

    cost = current_ftoken->cost; /*get last state of token*/
    fsm_node = &rec->context->FSMnode_list[current_ftoken->FSMnode_index];
    fsm_arc = NULL;

    /* Jean: see below too, let's remember the wtoken_index we create in
       case we need to re-use it.  All N epsilon updates, and all M
       M outgoing arcs can share, cuz this is the same arriving arc@frame */

    wtoken_index = MAXwtokenID;

    if( cost >= current_prune_thresh) {
      ftokenID* parent_ftoken_index;
      // the srec_get_parent_for_active_fsmnode() functions can be
      // gotten rid of if we use a doubly-linked list (fwd/bwd ptrs)
      parent_ftoken_index = srec_get_parent_for_active_fsmnode( rec, current_ftoken_index);
      if(!parent_ftoken_index) {
	PLogError( ("Error: internal search error near %s %d, finding %d\n"), __FILE__, __LINE__, current_ftoken_index);
	print_fsmnode_token_list(rec, rec->active_fsmnode_tokens, "in DO_UPDATE_EPSILONS ACTIVE_FSMNODE_TOKENS: \n");
	exit(1);
      }
      *parent_ftoken_index = current_ftoken->next_token_index;
      // effectively release this fsmnode_token and go to next one
      rec->best_token_for_node[ current_ftoken->FSMnode_index] = MAXftokenID;
      free_fsmnode_token( rec, current_ftoken_index);
      current_ftoken_index = *parent_ftoken_index;
      continue;
    }

    num_fsm_nodes_updated++;
    num_fsm_nodes_blocked = 0;
    num_wtokens_maybe_homonyms = 0;
    for (fsm_arc_index = fsm_node->un_ptr.first_next_arc;
	 fsm_arc_index != MAXarcID;
	 fsm_arc_index = fsm_arc->linkl_next_arc)
      {
        fsm_arc = &rec->context->FSMarc_list[ fsm_arc_index];

        /* consider only epsilon transitions */
        if (fsm_arc->ilabel >= EPSILON_OFFSET)
          continue;

        /* can't loop to yourself on epsilon! */
        ASSERT(fsm_arc->to_node != current_ftoken->FSMnode_index);

        cost_with_wtw = current_ftoken->cost + fsm_arc->cost; /* WTW */
        word_with_wtw = current_ftoken->word;
        if(fsm_arc->olabel != WORD_EPSILON_LABEL)
          word_with_wtw = fsm_arc->olabel ;

        // we should compare cost_with_wtw but let's let the priority_q
	// do the pruning
	if (cost>=current_prune_thresh || fsm_arc->cost>=current_prune_thresh)
	  continue;

        /*if word boundary, see if it crosses the word end threshold*/
        /* no room on the word priority_q, so not worth pursuing */
        if (fsm_arc->ilabel == WORD_BOUNDARY && cost_with_wtw >= current_word_threshold) {
          continue; // goto NEXT_FTOKEN;
        }

		new_ftoken = NULL;
        new_ftoken_index = rec->best_token_for_node[ fsm_arc->to_node];
        if(new_ftoken_index != MAXftokenID)
          new_ftoken = &rec->fsmnode_token_array[ new_ftoken_index];
        if( new_ftoken && (current_ftoken->cost+fsm_arc->cost)<new_ftoken->cost) {
          /* clobber it */
        } else if(new_ftoken) {
          /* merge it */
        } else if(rec->fsmnode_token_freelist == MAXftokenID) {
	        /* create it? maybe  */
			current_prune_delta = reprune_fsmnode_tokens(rec, best_cost, current_prune_delta, current_ftoken_index);
		    current_prune_thresh = best_cost + current_prune_delta;
        }

        if (fsm_arc->ilabel == WORD_BOUNDARY) {
          /* 20030920, for sure the backtrace will change! */
          // token->word_backtrace = MAXwtokenID;

          wtoken_index = srec_process_word_boundary_nbest(rec,
						 current_ftoken->FSMnode_index,
                                                 word_with_wtw,
                                                 current_ftoken->word_backtrace,
                                                 cost_with_wtw,
                                                 &current_word_threshold,
                                                 &num_fsm_nodes_blocked);
		  if (wtoken_index != MAXwtokenID) {
            WORD_TOKEN_SET_WD_ETIME( (rec->word_token_array+wtoken_index),
              rec->word_token_array[wtoken_index].end_time - current_ftoken->silence_duration);
		  }
		  if( fsm_arc->olabel!=WORD_EPSILON_LABEL && wtoken_index != MAXwtokenID) {
			  num_wtokens_maybe_homonyms++;
			  if( num_wtokens_maybe_homonyms>1)
				WORD_TOKEN_SET_HOMONYM( (rec->word_token_array+wtoken_index), 1);
		  }
          /* now drop alternative words, note the use of current_token
             because token is on the other side already */
          if (current_ftoken->aword_backtrace != AWTNULL) {
            awtoken = current_ftoken->aword_backtrace;
            for (; awtoken != AWTNULL; awtoken = awtoken->next_token) {
              wtokenID wti;
              wti = srec_process_word_boundary_nbest(rec,
						     current_ftoken->FSMnode_index,
                                                     awtoken->word,
                                                     awtoken->word_backtrace,
                                                     cost_with_wtw + awtoken->costdelta,
                                                     &current_word_threshold,
                                                     &num_fsm_nodes_blocked2);
            }
            /* if we don't free the altwords here, i had thought that
               updates from stateN would make the altwords grow and grow,
               but by that time all the fsmnodes are brand new */
            /* leaving them alive allows them to propagate to state0 thru
               other epsilons (eg non .wb) to new nodes but we don't
               use such arcs.
               the .wb would not get dropped again 'cuz we check
               for that in wtoken_index above.
               this is quite complex and the case for dropping word_tokens
               from the node AFTER the .wb can be made
               ie. would need a re-write of do_epsilon_updates() */
			if( current_ftoken->aword_backtrace != AWTNULL)
              free_altword_token_batch(rec, current_ftoken->aword_backtrace);
            current_ftoken->aword_backtrace = AWTNULL;
            /*print_fsmnode_token(rec, token-rec->fsmnode_token_array, "123a");*/
          }

          if( wtoken_index != MAXwtokenID) {

            if( new_ftoken == NULL) {
              /* create token for the other side */
              new_ftoken_index = get_free_fsmnode_token(rec, NULL_IF_NO_TOKENS);
				  // this should not happen because of the above check near
				  // fsmnode_token_freelist == MAXftokenID
              ASSERT(new_ftoken_index != MAXftokenID);
              new_ftoken = &(rec->fsmnode_token_array[new_ftoken_index]);
              new_ftoken->word_backtrace = wtoken_index;
              new_ftoken->cost = cost_with_wtw;
              new_ftoken->word = WORD_EPSILON_LABEL;
              new_ftoken->FSMnode_index = fsm_arc->to_node;
              new_ftoken->aword_backtrace = AWTNULL;
              new_ftoken->next_token_index = current_ftoken->next_token_index;
              current_ftoken->next_token_index = new_ftoken_index;
              rec->best_token_for_node[fsm_arc->to_node] = new_ftoken_index;
            } else if(new_ftoken && cost_with_wtw<new_ftoken->cost) {
              /* update token on the other side */
              ftokenID *parent_ftoken_index;
              new_ftoken = &(rec->fsmnode_token_array[new_ftoken_index]);
              new_ftoken->cost = cost_with_wtw;
              new_ftoken->word_backtrace = wtoken_index;
              new_ftoken->word = WORD_EPSILON_LABEL;
              // unchanged token->FSMnode_index = fsm_arc->to_node;
              // because this token was updated, we need to reprocess it, right after
              parent_ftoken_index = srec_get_parent_for_active_fsmnode( rec, new_ftoken_index);
              if(!parent_ftoken_index) {
		PLogError( ("Error: internal search error near %s %d, finding %d\n"), __FILE__, __LINE__, new_ftoken_index);
		print_fsmnode_token_list(rec, rec->active_fsmnode_tokens, "in DO_UPDATE_EPSILONS ACTIVE_FSMNODE_TOKENS: \n");
		exit(1);
              }
              *parent_ftoken_index = new_ftoken->next_token_index;
              new_ftoken->next_token_index = current_ftoken->next_token_index;
              current_ftoken->next_token_index = new_ftoken_index;
              rec->best_token_for_node[ fsm_arc->to_node] = new_ftoken_index;
	      /* new_ftoken->aword_backtrace must be null, alts here were
		 processed and dropped in srec_process_word_boundary_nbest() */
              if(new_ftoken->aword_backtrace != AWTNULL) {
		PLogError( ("Error: internal search error near %s %d\n"), __FILE__, __LINE__);
		continue;
              }
            } else {
              /* token on other side is same or better, just leave it */
            }
          }
        }
        else if(fsm_arc->ilabel == EPSILON_LABEL) {
          if( new_ftoken == NULL) {
            /* create token for the other side */
            new_ftoken_index = get_free_fsmnode_token(rec, NULL_IF_NO_TOKENS);
			// this should not happen because of the above check near
			// fsmnode_token_freelist == MAXftokenID
            ASSERT(new_ftoken_index != MAXftokenID);
            new_ftoken = &(rec->fsmnode_token_array[new_ftoken_index]);
            new_ftoken->word_backtrace = current_ftoken->word_backtrace;
            new_ftoken->cost = cost_with_wtw;
            new_ftoken->word = word_with_wtw;
            new_ftoken->FSMnode_index = fsm_arc->to_node;
            new_ftoken->aword_backtrace = refcopy_altwords(rec, current_ftoken->aword_backtrace);
            new_ftoken->next_token_index = current_ftoken->next_token_index;
            current_ftoken->next_token_index = new_ftoken_index;
            rec->best_token_for_node[fsm_arc->to_node] = new_ftoken_index;
          } else if(new_ftoken && cost_with_wtw<new_ftoken->cost) {
            /* update token on the other side */
            ftokenID *parent_ftoken_index;
            new_ftoken = &(rec->fsmnode_token_array[new_ftoken_index]);
            new_ftoken->cost = cost_with_wtw;
            new_ftoken->word_backtrace = current_ftoken->word_backtrace;
            new_ftoken->word = word_with_wtw;
	    /* here we are giving up the path and alternatives that existed at
	       this node, which is not great! The new (better) top choice
	       coming in and it's alternatives are propagated instead.
	       TODO: merge the alternative lists and the previous top choice
	    */
	    if(new_ftoken->aword_backtrace!=AWTNULL)
              free_altword_token_batch( rec, new_ftoken->aword_backtrace);
	    new_ftoken->aword_backtrace = AWTNULL;
            new_ftoken->aword_backtrace = refcopy_altwords(rec, current_ftoken->aword_backtrace);
            // unchanged token->FSMnode_index = fsm_arc->to_node;
            // because this token was updated, we need to re-process it, right after
            parent_ftoken_index = srec_get_parent_for_active_fsmnode( rec, new_ftoken_index);
            if(!parent_ftoken_index) {
	      PLogError( ("Error: internal search error near %s %d, finding %d\n"), __FILE__, __LINE__, new_ftoken_index);
	      print_fsmnode_token_list(rec, rec->active_fsmnode_tokens, "in DO_UPDATE_EPSILONS ACTIVE_FSMNODE_TOKENS: \n");
	      exit(1);
            }
            *parent_ftoken_index = new_ftoken->next_token_index;
            new_ftoken->next_token_index = current_ftoken->next_token_index;
            current_ftoken->next_token_index = new_ftoken_index;
            rec->best_token_for_node[ fsm_arc->to_node] = new_ftoken_index;
          } else {
            /* token on other side is same or better, just leave it */
	    /* todo: maybe merge alternative lists? */
          }
        }
      } /* loop over arcs */

      ASSERT( current_ftoken->cost != MAXcostdata);
      if( num_fsm_nodes_blocked) {
        /* we do not want to propagate fsm node tokens for paths that have
           just been killed on the basis of no space for word propagation */
        prune_fsmnode_tokens(rec, MAXcostdata/2, current_ftoken_index);
      }

    // NEXT_FTOKEN:
      current_ftoken_index = current_ftoken->next_token_index;
    }

  // print_fsmnode_token_list(rec, rec->active_fsmnode_tokens, "AFTER UPDATE_EPSILONS ACTIVE_FSMNODE_TOKENS: \n");

  sanity_check_altwords(rec, rec->altword_token_freelist);
  return num_fsm_nodes_updated;
}

static void update_internal_hmm_states(srec *rec, costdata *pcurrent_prune_delta,
                                       costdata *pcurrent_best_cost,
                                       costdata *precomputed_model_scores)
{
  stokenID current_token_index;
  fsmarc_token *current_token;
  costdata current_best_cost;
  costdata current_prune_thresh;
  costdata current_prune_delta;
  costdata model_cost;
  asr_int16_t any_alive;
  HMMInfo *hmm_info;
  modelID model_index;
  asr_int16_t internal_state, end_state;
  arcID fsm_arc_index;
  FSMarc* fsm_arc;

  costdata prev_cost;
  costdata self_loop_cost;

  current_best_cost = *pcurrent_best_cost;
  current_prune_delta = *pcurrent_prune_delta;
  current_prune_thresh = current_best_cost + current_prune_delta;

  ASSERT(((float)current_best_cost + current_prune_delta) < (float)USHRT_MAX);

  /* best_token_for_arc must be reset here, cuz the same array might have
     been used by another gender.  Alternatively we could have let each
     recog use it's own array thereby save cpu at expense of memory */
  for (fsm_arc_index = 0; fsm_arc_index < rec->context->num_arcs; fsm_arc_index++)
    rec->best_token_for_arc[fsm_arc_index] = MAXstokenID;

  current_token_index = rec->active_fsmarc_tokens;
  while (current_token_index != MAXstokenID)
  {
    current_token = &(rec->fsmarc_token_array[current_token_index]);

    fsm_arc_index = current_token->FSMarc_index;
    fsm_arc = &rec->context->FSMarc_list[fsm_arc_index];

    /* best_token_for_arc must be set here, cuz it was reset above */
    rec->best_token_for_arc[fsm_arc_index] = current_token_index;

    hmm_info = &rec->context->hmm_info_for_ilabel[ fsm_arc->ilabel];
    any_alive = 0;
    end_state = current_token->num_hmm_states - 1;

    for (internal_state = end_state; internal_state >= 0; internal_state--)
    {

      model_index = hmm_info->state_indices[internal_state];
      model_cost = precomputed_model_scores[model_index];

      /*better to come from previous or self?*/

      if (internal_state > 0)
      {
        prev_cost = current_token->cost[internal_state-1];
                /* a duration model can be applied here,
                   by changing the prev_cost according to some function of
                     the current_token->duration[internal_state-1] rec->avg_state_durations[ prev_model_index] */
        if (prev_cost < current_prune_thresh)
        {
	  modelID prev_model_index;
          prev_cost = (costdata)(prev_cost + (costdata) model_cost);
          /* apply duration model for "next" transition, note that it's nice
             to access the duration model (avg_state_durations) somehwere
             other than the acoustic models which could be far away in memory
             arrive penalty would be applied here too if it was reqd */

          prev_model_index = hmm_info->state_indices[internal_state-1];
          prev_cost = (costdata)(prev_cost + (costdata) duration_penalty_depart(rec->avg_state_durations[ prev_model_index],
                                 current_token->duration[internal_state-1]));

        }
      }
      else
      {
        prev_cost = MAXcostdata;
      }

      self_loop_cost = current_token->cost[internal_state];
                /* a duration model can be applied here,
                   by changing the self_loop_cost according to some function of
                     the current_token->duration[internal_state] rec->avg_state_durations[ prev_model_index] */
      if (self_loop_cost < current_prune_thresh)
      {
        self_loop_cost = (costdata)(self_loop_cost + (costdata) model_cost);
        /* apply duration model for "loop" transition */

        self_loop_cost = (costdata)(self_loop_cost + (costdata) duration_penalty_loop(rec->avg_state_durations[ model_index],
                                    current_token->duration[internal_state]));

      }

      if (prev_cost < self_loop_cost)
      {
        current_token->cost[internal_state] = prev_cost;
        current_token->word_backtrace[internal_state] = current_token->word_backtrace[internal_state-1];
        current_token->word[internal_state] = current_token->word[internal_state-1];
                current_token->duration[internal_state] = 1;
        if (current_token->word[internal_state-1] != MAXwordID)
        {
          if (current_token->aword_backtrace[internal_state] != AWTNULL)
            free_altword_token_batch(rec,
                                     current_token->aword_backtrace[internal_state]);
          current_token->aword_backtrace[internal_state] = refcopy_altwords(rec, current_token->aword_backtrace[internal_state-1]);
          /*print_fsmarc_token(rec, current_token_index, "123c");*/
        }
        else
        {
          /* if there's no top choice, there shouldn't be alternatives! */
          ASSERT(current_token->aword_backtrace[internal_state] == AWTNULL);
          ASSERT(current_token->aword_backtrace[internal_state-1] == AWTNULL);
        }
      }
      else
      {
        current_token->cost[internal_state] = self_loop_cost;
                current_token->duration[internal_state]++;
      }

      if (current_token->cost[internal_state] < current_prune_thresh)
      {
        any_alive = 1;
        if (current_token->cost[internal_state] < current_best_cost)
        {
          current_best_cost = current_token->cost[internal_state];
          current_prune_thresh = current_best_cost + current_prune_delta;
        }
      }
    }
    current_token_index = current_token->next_token_index;
  }
  *pcurrent_best_cost = current_best_cost;
  *pcurrent_prune_delta = current_prune_delta;
}




static int GetNumArcsArrivingClip2(srec_context* context, FSMnode* fsm_node)
{
  arcID fpa = fsm_node->first_prev_arc;
  FSMarc* arc;

  if (fpa == MAXarcID)
    return 0;
  arc = &context->FSMarc_list[fpa];
  if (arc->linkl_prev_arc == MAXarcID)
    return 1;
  else
    return 2;
}

static int update_from_hmms_to_fsmnodes(srec *rec, costdata prune_delta, costdata best_cost)
{
  stokenID current_token_index;
  fsmarc_token *current_token;
  int end_state;
  costdata end_cost;
  costdata current_prune_thresh;
  costdata current_prune_delta;  /*may get tighter to keep num fsmnodes under control*/
  // vFSMarc vfsm_arc;
  FSMarc* fsm_arc;
  FSMnode* fsm_node;
  // vFSMnode vfsm_node;
  arcID fsm_arc_index;
  nodeID to_node_index;
  ftokenID new_ftoken_index;
  fsmnode_token *ftoken;
  modelID end_model_index;
  labelID ilabel;
  short end_cost_equality_hack;
  HMMInfo* hmm_info;
  altword_token *awtoken, *q;
  int num_fsmnode_updates = 0;

  current_prune_delta = prune_delta;
  current_prune_thresh = best_cost + current_prune_delta;
  current_token_index = rec->active_fsmarc_tokens;

  for (ilabel = 0; ilabel < NODE_INFO_NUMS; ilabel++)
  {
    rec->current_best_ftoken_cost[ilabel] = MAXcostdata / 2;
    rec->current_best_ftoken_index[ilabel] = MAXftokenID;
  }
  sanity_check_altwords(rec, rec->altword_token_freelist);

  while (current_token_index != MAXstokenID)
  {
    current_token = &(rec->fsmarc_token_array[current_token_index]);

    /*propagate from end of state token to new FSM node*/
    end_state = (char) current_token->num_hmm_states - 1;

    ASSERT((current_token->aword_backtrace[end_state] == AWTNULL)
           || (current_token->word[end_state] != MAXwordID));
    end_cost = current_token->cost[end_state];

    /* anticipated repruning: make sure there is enough space before
       beginning complex computation */
    if (rec->word_priority_q->max_in_q>1 && rec->altword_token_freelist_len < 3*rec->word_priority_q->max_in_q)
      reprune_altword_tokens(rec);

    if (end_cost < current_prune_thresh)
    {
      num_fsmnode_updates++;
      fsm_arc_index = current_token->FSMarc_index;
      fsm_arc = &rec->context->FSMarc_list[ fsm_arc_index];

      hmm_info = &rec->context->hmm_info_for_ilabel[ fsm_arc->ilabel];

      end_model_index = hmm_info->state_indices[end_state];

      end_cost = (costdata)(end_cost + (costdata) duration_penalty_depart(rec->avg_state_durations[end_model_index],
                            current_token->duration[end_state]));
      to_node_index = fsm_arc->to_node;
      new_ftoken_index = rec->best_token_for_node[to_node_index];
      if (new_ftoken_index == MAXftokenID)
      {
        /*we need to make sure there is room in the new_states array
          and there are free state tokens*/
        if (rec->fsmnode_token_freelist == MAXftokenID)
        {
          /*make sure there is room for another FSMnode token
            - if not, prune until there is room*/
          current_prune_delta = reprune_fsmnode_tokens(rec, best_cost, current_prune_delta, MAXftokenID);
          current_prune_thresh = best_cost + current_prune_delta;
        }

        /*because of the above check, this should always succeed*/
        new_ftoken_index = get_free_fsmnode_token(rec, EXIT_IF_NO_TOKENS);

        ftoken = &(rec->fsmnode_token_array[new_ftoken_index]);
        ftoken->FSMnode_index = to_node_index;
        ftoken->next_token_index = rec->active_fsmnode_tokens;
        ftoken->cost = end_cost;
        ftoken->word_backtrace = current_token->word_backtrace[end_state];
        ftoken->word = current_token->word[end_state];
        if (end_model_index == SILENCE_MODEL_INDEX && ftoken->word != rec->context->beg_silence_word)
        {
          ftoken->silence_duration = current_token->duration[end_state];
        }
        else
        {
          ftoken->silence_duration = 0;
        }
        if (ftoken->word != MAXwordID)
        {
          arcID narr;
          fsm_node = &rec->context->FSMnode_list[ fsm_arc->to_node];
          /* when there is only one arc arriving, a refcopy is good enough
             and saves memory */
          narr = (arcID) GetNumArcsArrivingClip2(rec->context, fsm_node);
          if (narr > 1)
            ftoken->aword_backtrace = copy_altwords(rec, current_token->aword_backtrace[end_state], 0);
          else
            ftoken->aword_backtrace = refcopy_altwords(rec, current_token->aword_backtrace[end_state]);
        }
        else
        {
          /* if there's no top choice, there shouldn't be alternatives! */
          ASSERT(current_token->aword_backtrace[end_state] == AWTNULL);
          ftoken->aword_backtrace = AWTNULL;
        }
        rec->active_fsmnode_tokens = new_ftoken_index;
        rec->best_token_for_node[to_node_index] = new_ftoken_index;
      }
      else /* a token already exists, use it! */
      {

        ftoken = &(rec->fsmnode_token_array[new_ftoken_index]);
        ASSERT( ((current_token->word[end_state] == MAXwordID) && (ftoken->word == MAXwordID))
             || ((current_token->word[end_state] != MAXwordID) && (ftoken->word != MAXwordID)) );

        /* this is a hack for preferring the shorter of the backtrace words
           when scores are equal, used to prefer longer pau2 word */
        end_cost_equality_hack = 0;
        if (end_cost == ftoken->cost)
        {
          if (current_token->word_backtrace[end_state] != ftoken->word_backtrace
              && current_token->word_backtrace[end_state] != MAXwtokenID)
          {
            frameID ct_end_time = MAXframeID, et_end_time = 0;
            if (current_token->word_backtrace[end_state] != MAXwtokenID)
              ct_end_time = rec->word_token_array[current_token->word_backtrace[end_state]].end_time;
            if (ftoken->word_backtrace != MAXwtokenID)
              et_end_time = rec->word_token_array[ftoken->word_backtrace].end_time;
            if (ct_end_time < et_end_time)
              end_cost_equality_hack = 1;
          }
        }

        if (end_cost < ftoken->cost || end_cost_equality_hack)
        {
          /* new one coming in is better, so push the current state down */
          /* ftoken info goes into awtoken */
          if (ftoken->word != MAXwordID)
          {
            /* copy_altwords() */
            awtoken = get_free_altword_token(rec, NULL_IF_NO_TOKENS);
            if (awtoken != AWTNULL)
            {
              awtoken->costdelta = ftoken->cost - end_cost;
              awtoken->word_backtrace = ftoken->word_backtrace;
              awtoken->word = ftoken->word;

              /* ensure full ownership! */
              q = ftoken->aword_backtrace;
              if (q != AWTNULL && q->refcount > 1)
              {
                awtoken->next_token = copy_altwords(rec, ftoken->aword_backtrace, ftoken->cost - end_cost);
                free_altword_token_batch(rec, ftoken->aword_backtrace);
                /* reversed order above here !! */
              }
              else
              {
                awtoken->next_token = ftoken->aword_backtrace;
				count_altword_token( rec, awtoken);
                for (q = awtoken->next_token; q; q = q->next_token)
                  q->costdelta += ftoken->cost - end_cost;
              }
              ftoken->aword_backtrace = awtoken;
              ftoken->aword_backtrace = sizewise_altwords(rec, ftoken->aword_backtrace);
			  if( (q=ftoken->aword_backtrace)!=AWTNULL) {
                for (q = ftoken->aword_backtrace; q->next_token; q = q->next_token) ;
                q->next_token = copy_altwords(rec, current_token->aword_backtrace[end_state], 0);
                ftoken->aword_backtrace = sizewise_altwords(rec, ftoken->aword_backtrace);
                /* awtoken->costbasis = &ftoken->cost; */
                ftoken->aword_backtrace->refcount = 1;
			  }
            }
          }
          else
          {
            /* if there's no top choice, there shouldn't be alternatives! */
            ASSERT(ftoken->aword_backtrace == AWTNULL);
          }
          /* and stoken info goes into ftoken */
          ftoken->cost = end_cost;
          ftoken->word_backtrace = current_token->word_backtrace[end_state];
          ftoken->word = current_token->word[end_state];
          if (end_model_index == SILENCE_MODEL_INDEX && ftoken->word != rec->context->beg_silence_word)
          {
            ftoken->silence_duration = current_token->duration[end_state];
          }
          else
          {
            ftoken->silence_duration = 0;
          }
        }
        else
        {
                    /* new arc arriving is worse */
          /* print_fsmarc_token(rec, current_token_index, "new_arc_arriving worse");
             print_fsmnode_token(rec, new_ftoken_index, "new_arc_arriving tonode");*/
          /* append it to the alt list */
          /* stoken info goes into the awtoken, ftoken unchanged */
          if (ftoken->word != MAXwordID)
          {
            /* copy_altwords() */
            awtoken = get_free_altword_token(rec, NULL_IF_NO_TOKENS);
            if (awtoken != AWTNULL)
            {
              awtoken->costdelta = end_cost - ftoken->cost;
              awtoken->word = current_token->word[end_state];
              awtoken->word_backtrace = current_token->word_backtrace[end_state];

              if (current_token->aword_backtrace[end_state] != AWTNULL)
                awtoken->next_token = copy_altwords(rec,
                                                    current_token->aword_backtrace[end_state],
                                                    awtoken->costdelta);
              else
                awtoken->next_token = AWTNULL;

              /* ensure full ownership!, this is new here! */
              q = ftoken->aword_backtrace;
              if (q != AWTNULL && q->refcount > 1)
              {
                q = copy_altwords(rec, ftoken->aword_backtrace, 0);
                free_altword_token_batch(rec, ftoken->aword_backtrace);
                ftoken->aword_backtrace = q;
              }
            }
            if (ftoken->aword_backtrace)
            {
              for (q = ftoken->aword_backtrace; q->next_token; q = q->next_token) ;
              q->next_token = awtoken;
            }
            else
            {
              ftoken->aword_backtrace = awtoken;
            }
			if (ftoken->aword_backtrace!=AWTNULL) {
				ftoken->aword_backtrace->refcount = 1;
				ftoken->aword_backtrace = sizewise_altwords(rec, ftoken->aword_backtrace);
			}
          }
        }
        /*print_fsmnode_token(rec, new_ftoken_index, "123e reused-token ");*/
      }
      ilabel = rec->context->FSMnode_info_list[ ftoken->FSMnode_index];
      ASSERT(ilabel < NODE_INFO_NUMS);
      if (ftoken->cost < rec->current_best_ftoken_cost[ilabel])
      {
        rec->current_best_ftoken_cost[ilabel]  = ftoken->cost;
        rec->current_best_ftoken_index[ilabel] = new_ftoken_index;
      }
      if (ftoken->cost < rec->current_best_ftoken_cost[NODE_INFO_UNKNOWN])
      {
        rec->current_best_ftoken_cost[NODE_INFO_UNKNOWN]  = ftoken->cost;
        rec->current_best_ftoken_index[NODE_INFO_UNKNOWN] = new_ftoken_index;
      }
      ASSERT(ftoken->word != MAXwordID || ftoken->aword_backtrace == AWTNULL);
    }
    current_token_index = current_token->next_token_index;
  }
  sanity_check_altwords(rec, rec->altword_token_freelist);
  return num_fsmnode_updates;
}

static int update_from_current_fsm_nodes_into_new_HMMs(srec* rec,
    costdata *pcurrent_prune_delta,
    costdata *pcurrent_best_cost,
    costdata *precomputed_model_scores)
{
  costdata prev_cost;
  FSMnode* fsm_node;
  FSMarc*  fsm_arc;
  arcID fsm_arc_index;
  HMMInfo *hmm_info;
  modelID model_index;
  fsmarc_token *token;
  stokenID new_token_index = MAXstokenID;
  costdata cost;
  costdata current_prune_thresh;
  costdata current_prune_delta = *pcurrent_prune_delta;
  costdata current_best_cost = *pcurrent_best_cost;
  ftokenID ftoken_index;
  ftokenID old_ftoken_index;
  fsmnode_token *fsmnode_token;
  int num_fsm_nodes_updated = 0;
  costdata orig_prune_delta;

  ftoken_index = rec->active_fsmnode_tokens;

  current_prune_thresh = *pcurrent_best_cost + *pcurrent_prune_delta;
  orig_prune_delta = *pcurrent_prune_delta;

  sanity_check_altwords(rec, rec->altword_token_freelist);

  while (ftoken_index != MAXftokenID)
  {
    fsmnode_token = &rec->fsmnode_token_array[ftoken_index];

    prev_cost = fsmnode_token->cost; /*get last state of token*/
    if (fsmnode_token->FSMnode_index == rec->context->end_node)
    {
      prev_cost = MAXcostdata;
    }

    if (prev_cost < current_prune_thresh)
    {
      num_fsm_nodes_updated++;

      fsm_node = &rec->context->FSMnode_list[fsmnode_token->FSMnode_index];

      /* loop over arcs leaving this fsm_node */
      for (fsm_arc_index = fsm_node->un_ptr.first_next_arc;
           fsm_arc_index != MAXarcID;
           fsm_arc_index = fsm_arc->linkl_next_arc)
      {
        labelID ilabel;
        wordID olabel;
        nodeID nextnode;

        fsm_arc = &rec->context->FSMarc_list[  fsm_arc_index];

        ilabel = fsm_arc->ilabel;
        olabel = fsm_arc->olabel;
        nextnode = fsm_arc->to_node;

        if (ilabel >= EPSILON_OFFSET)
        {
                    /*so, not an epsilon arc*/
          hmm_info = &rec->context->hmm_info_for_ilabel[ilabel];

          model_index = hmm_info->state_indices[0];

          cost = prev_cost + precomputed_model_scores[model_index];
          cost = (costdata)(cost + (costdata) fsm_arc->cost);

          if (cost < current_prune_thresh)
          {
            /*new node to keep*/

            /* look for the fsmarc_token* token, into which to maximize, else create new one */
            if (rec->best_token_for_arc[fsm_arc_index] == MAXstokenID)
            {

              /*make sure there is room for another state token - if not, prune
              until there is room*/
              /*we need to make sure there is room in the new_states array and
              there are free state tokens*/
              if (rec->fsmarc_token_freelist == MAXstokenID)
              {
                current_prune_delta = reprune_new_states(rec, current_best_cost, current_prune_delta);
              }

              /* because of the above check, this should always succeed */
              new_token_index = setup_free_fsmarc_token(rec, fsm_arc, fsm_arc_index, EXIT_IF_NO_TOKENS);

              token = &(rec->fsmarc_token_array[new_token_index]);

              token->next_token_index = rec->active_fsmarc_tokens;
              rec->active_fsmarc_tokens = new_token_index;
              rec->num_new_states++;

              rec->best_token_for_arc[fsm_arc_index] = new_token_index;
              token->cost[0] = MAXcostdata;
            }
            else
            {
              new_token_index = rec->best_token_for_arc[fsm_arc_index];
              token = &(rec->fsmarc_token_array[ new_token_index]);
            }

            if (cost < token->cost[0])
            {
              token->cost[0] = cost;
                            token->duration[0] = 1;
              token->word_backtrace[0] = fsmnode_token->word_backtrace;
              if (token->aword_backtrace[0] != AWTNULL)
                free_altword_token_batch(rec, token->aword_backtrace[0]);
              token->aword_backtrace[0] = AWTNULL;
              token->aword_backtrace[0] = refcopy_altwords(rec, fsmnode_token->aword_backtrace);

              if (olabel != WORD_EPSILON_LABEL)
              {
                token->word[0] = olabel;
                //ASSERT(token->aword_backtrace[0] == AWTNULL);
              }
              else
              {
                token->word[0] = fsmnode_token->word;
              }
              ASSERT(token->word[0] != MAXwordID
                     || token->aword_backtrace[0] == AWTNULL);
              if (cost < current_best_cost)
              {
                current_best_cost = cost;
                current_prune_delta = orig_prune_delta;  /*if we have a new best cost, the prune delta could go back up*/
                current_prune_thresh = cost + current_prune_delta;
                ASSERT((float)cost + (float)current_prune_delta < (float)USHRT_MAX);
              }
            }
          }
        }
      }
    }
    rec->best_token_for_node[fsmnode_token->FSMnode_index] = MAXftokenID; /*done with this node - remove it from the array*/
    old_ftoken_index = ftoken_index;

    ftoken_index = fsmnode_token->next_token_index;
    free_fsmnode_token(rec, old_ftoken_index); /*done with this node - free the token*/
    rec->active_fsmnode_tokens = ftoken_index; /*needed for sanity_check_altwords*/
  }
  /*done with all the tokens, set active tokens to NULL*/
  rec->active_fsmnode_tokens = MAXftokenID;
  sanity_check_altwords(rec, rec->altword_token_freelist);

  *pcurrent_best_cost = current_best_cost;
  *pcurrent_prune_delta = current_prune_delta;

  return num_fsm_nodes_updated;
}

#if USE_COMP_STATS
void start_front_end_clock(void)
{
  if (!comp_stats)
    comp_stats = init_comp_stats();
  start_cs_clock(&comp_stats->front_end);
}
void stop_front_end_clock(void)
{
  end_cs_clock(&comp_stats->front_end, 1);
}
#endif


/*---------------------------------------------------------------------------*
 *                                                                           *
 * begin and end                                                             *
 *                                                                           *
 *---------------------------------------------------------------------------*/

/*gets things started for the viterbi search - sets up things for frame 0*/

int srec_begin(srec *rec, int begin_syn_node)
{
  FSMnode* fsm_node;
  fsmnode_token *token;
  stokenID new_token_index;
  nodeID node_index;
  arcID arc_index;

  if (!rec || !rec->context)
  {
    log_report("Error: bad inputs to srec_begin()\n");
    return 1;
  }
  if (!rec->context->whether_prepared)
  {
    log_report("srec_begin: Grammar not prepared. Compiling!\n");
    FST_PrepareContext(rec->context);

    if (!rec->context->whether_prepared)
    {
      PLogError("ESR_INVALID_STATE: Grammar can not be compiled (FST_PrepareContext failed)");
      return ESR_INVALID_STATE ;
    }
  }

#if USE_COMP_STATS
  if (comp_stats == NULL)
    comp_stats = init_comp_stats();
#endif
  /*initialize token storage - not clear we really need this - as long as they
  are managed correctly, we should be able to do this on startup - not each utt*/
  initialize_free_fsmarc_tokens(rec);
  initialize_free_word_tokens(rec);
  initialize_free_fsmnode_tokens(rec);
  initialize_word_lattice(rec->word_lattice);
  initialize_free_altword_tokens(rec);

  if (rec->context->num_nodes > rec->max_fsm_nodes)
  {
    log_report("Error: srec_begin failing due to too many grammar nodes\n");
    return 1;
  }
  for (node_index = 0;node_index < rec->context->num_nodes;node_index++)
  {
    rec->best_token_for_node[node_index] = MAXftokenID;
  }
  if (rec->context->num_arcs > rec->max_fsm_arcs)
  {
    log_report("Error: srec_begin failing due to too many grammar arcs\n");
    return 1;
  }
  for (arc_index = 0;arc_index < rec->context->num_arcs;arc_index++)
  {
    rec->best_token_for_arc[arc_index] = MAXstokenID;
  }
  rec->srec_ended = 0;
  rec->num_new_states = 0;
  rec->current_best_cost = 0;
  rec->current_prune_delta = rec->prune_delta;

  /*need help from johan - does ths FSM only have one start node?
  Which one is it?   assume just one and it is node 0*/

  fsm_node =  &rec->context->FSMnode_list[ rec->context->start_node];
  node_index = (nodeID) rec->context->start_node;
  /* node_index is still 0 at this point */

  /*now we just need to setup an initial fsmnode token (for begin FSM node) and then do epsilon updates*/

  rec->active_fsmarc_tokens = MAXstokenID;

  new_token_index = get_free_fsmnode_token(rec, EXIT_IF_NO_TOKENS);

  token = &(rec->fsmnode_token_array[new_token_index]);
  token->word_backtrace = MAXwtokenID; /* real value set below*/
  token->cost = 0;
  token->word = MAXwordID;
  token->FSMnode_index = node_index;
  token->next_token_index = MAXftokenID;
  token->aword_backtrace = AWTNULL;

  rec->best_token_for_node[node_index] = new_token_index;
  rec->active_fsmnode_tokens = new_token_index;
  rec->current_search_frame = 0;

  do_epsilon_updates(rec, rec->prune_delta, 0);
  return 0;
}

void srec_force_the_end(srec* rec, frameID end_frame, wordID end_word)
{
  srec_word_lattice* wl = rec->word_lattice;
  wtokenID wtoken_index, tmp;
  frameID frame;
  wtoken_index = wl->words_for_frame[end_frame];
  if (wtoken_index == MAXwtokenID)
  {
    for (frame = end_frame - 1; frame > 20; frame--)
    {
      if (wl->words_for_frame[frame] != MAXwtokenID)
      {
        word_token* wtoken;
        wl->words_for_frame[end_frame] = wl->words_for_frame[frame];
        wl->words_for_frame[frame] = MAXwtokenID;
        for (tmp = wl->words_for_frame[end_frame]; tmp != MAXwtokenID;
             tmp = wtoken->next_token_index)
        {
          wtoken = &rec->word_token_array[tmp];
          wtoken->end_time = frame;
          wtoken->word = end_word;
          wtoken->end_node = rec->context->end_node;
        }
#ifdef _WIN32
        PLogError(L("Forced an end path at end frame %d/%d)\n"), frame, end_frame);
#endif
        break;
      }
    }
  }
}

/* when there are no more frames of input, this functions
   kills all paths not ending at the end node and
   creates a word linked list even though there is no WORD_BOUNDARY ilabel */

void srec_no_more_frames(srec* rec)
{
#if USE_COMP_STATS
  frameID end_frame = rec->current_search_frame;
#endif
  nodeID  end_node;
  fsmnode_token* ftoken;
  ftokenID current_token_index;
  costdata current_word_threshold = MAXcostdata;
  wtokenID word_token_index;
  int any_nodes_blocked = 0;
  altword_token* awtoken;

  /* this is just for sanity checking, to find out what the state was
     at the end of input */
  srec_check_end_of_speech_end(rec);

  if (rec->srec_ended) return;
  rec->srec_ended = 1;

#if USE_COMP_STATS
  comp_stats->total_time += (float)(end_frame / 50.0f);
  dump_comp_stats(comp_stats, PSTDOUT);
#endif

  end_node = rec->context->end_node;
  /*remove all word paths from the priority_q which do not end at end_node
    to make space for those being added below */
  remove_non_end_word_from_q(rec, rec->word_priority_q, rec->word_token_array,
                             end_node);

  if (rec->current_search_frame == 0)
    return;

  rec->accumulated_cost_offset[ rec->current_search_frame] =
    rec->accumulated_cost_offset[ rec->current_search_frame-1];
  rec->cost_offset_for_frame[ rec->current_search_frame] = 0;

  /* watch out if using the best_token_for_node[] array here
     is it valid? not if multiple recognizers, maybe we
     should remember best_token_for_end_node separately */

  current_token_index = rec->active_fsmnode_tokens;
  while (current_token_index != MAXftokenID)
  {
    ftoken = &rec->fsmnode_token_array[current_token_index];
    if (ftoken->FSMnode_index == end_node)
    {
      /* print_fsmnode_token(rec, current_token_index, "fsmnode_token at end_node "); */
      word_token_index = srec_process_word_boundary_nbest(rec,
                         ftoken->FSMnode_index,
                         ftoken->word,
                         ftoken->word_backtrace,
                          ftoken->cost, &current_word_threshold, &any_nodes_blocked);
      if (word_token_index != MAXwtokenID)
      {
        WORD_TOKEN_SET_WD_ETIME( (rec->word_token_array+word_token_index),
          rec->word_token_array[word_token_index].end_time - ftoken->silence_duration);
      }
      /* now also dump alternatives at this last frame, sep19'03 fixed */
      awtoken = ftoken->aword_backtrace;
      for (; awtoken != AWTNULL; awtoken = awtoken->next_token)
      {
        srec_process_word_boundary_nbest(rec,
                                         ftoken->FSMnode_index,
                                         awtoken->word,
                                         awtoken->word_backtrace,
                                         ftoken->cost + awtoken->costdelta,
                                         &current_word_threshold,
                                         &any_nodes_blocked);
      }
    }
    current_token_index = ftoken->next_token_index;
  }

  /* we clobber the word_lattice at the last frame that was created
     in do_epsilon_updates() */
  word_token_index = get_word_token_list(rec->word_priority_q, rec->word_token_array);
  lattice_add_word_tokens(rec->word_lattice, rec->current_search_frame, word_token_index);

  if (FST_IsVoiceEnrollment(rec->context) && word_token_index == MAXwtokenID)
  {
    srec_force_the_end(rec, rec->current_search_frame, rec->context->end_silence_word);
  }

  /* find the current_best_cost for this recognizer ... at the end node,
     it will be used to decide which recognizer wins! */
  rec->current_best_cost = lattice_best_cost_to_frame(rec->word_lattice,
                           rec->word_token_array,
                           rec->current_search_frame);

}

void srec_terminate(srec* rec)
{
  frameID ifr;
  stokenID stoken_index, next_stoken_index;
  fsmarc_token* stoken;
  ftokenID ftoken_index, next_ftoken_index;
  fsmnode_token* ftoken;
  wtokenID wtoken_index, next_wtoken_index;
  word_token* wtoken;

  /* release all state tokens */
  for (stoken_index = rec->active_fsmarc_tokens; stoken_index != MAXstokenID;
       stoken_index = next_stoken_index)
  {
    stoken = &rec->fsmarc_token_array[ stoken_index];
    next_stoken_index = stoken->next_token_index;
    free_fsmarc_token(rec, stoken_index);
  }
  rec->active_fsmarc_tokens = MAXstokenID;

  /* release all fsmnode tokens */
  for (ftoken_index = rec->active_fsmnode_tokens; ftoken_index != MAXftokenID;
       ftoken_index = next_ftoken_index)
  {
    ftoken = &rec->fsmnode_token_array[ ftoken_index];
    next_ftoken_index = ftoken->next_token_index;
    free_fsmnode_token(rec, ftoken_index);
  }
  rec->active_fsmnode_tokens = MAXftokenID;

  /* release all word tokens */
  for (ifr = 0; ifr < rec->current_search_frame; ifr++)
  {
    for (wtoken_index = rec->word_lattice->words_for_frame[ifr];
         wtoken_index != MAXwtokenID; wtoken_index = next_wtoken_index)
    {
      wtoken = &rec->word_token_array[wtoken_index];
      next_wtoken_index = wtoken->next_token_index;
      free_word_token(rec, wtoken_index);
    }
    rec->word_lattice->words_for_frame[ifr] = MAXwtokenID;
  }
  rec->current_model_scores[SILENCE_MODEL_INDEX] = DO_NOT_COMPUTE_MODEL;
  rec->current_best_cost = MAXcostdata;
  rec->srec_ended = 1;
}
/*------------------------------------------------------------------------*
 *                                                                        *
 * main work of the viterbi search                                        *
 *                                                                        *
 *------------------------------------------------------------------------*/

/*with new update to FSM node scheme, the sequence of operation is:

  for each frame:

  1. Handle all internal HMM updates based on new frame observations.  This is
  done in place with the current list of HMM tokens.

  2. For each current active FSM node (from previous frame), activate update
  into state 0 (either for existing HMM tokens or for new HMM tokens) by going
  through an observation frame (so, only go from an FSM node to a new HMM
  token if the first observation frame gets a score above the current pruning
  threshold).  FSM nodes are freed as this is done.  So, no FSMnode tokens are left
  at the end of this.

  3. Prune.  Note that the best score will have already been established for
  this frame (so therefore the pruning threshold will not change).

  4. reset best cost to 0 (to keep scores in range).  We can do this here since we already  know the best score.

  5. For end hmm states which are above the pruning threshold, create new
  FSMnode_tokens.

  6. update epsilons, including word boundary arcs (which put words onto the word lattice).
  epsilon updates go from FSM node to FSM node.

  repeat for next frame based on new FSM nodes and current HMMs

*/

void srec_viterbi_part1(srec *rec,
                        const SWIModel *acoustic_models,
                        pattern_info *pattern,
                        costdata silence_model_cost);

void srec_viterbi_part2(srec *rec);

int multi_srec_viterbi(multi_srec *recm,
                       srec_eos_detector_parms* eosd,
                       pattern_info *pattern,
                       utterance_info* utt_not_used)
{
  EOSrc eosrc1 = SPEECH_ENDED, eosrc2 = SPEECH_ENDED;
#if DO_ALLOW_MULTIPLE_MODELS
  ASSERT(recm->num_activated_recs == recm->num_swimodels);
    if (recm->num_activated_recs == 1)
  {
#endif
    srec* rec1 = &recm->rec[0];
#if USE_COMP_STATS
    start_cs_clock1(&comp_stats->overall_search);
#endif
    if (rec1->current_search_frame >= (rec1->word_lattice->max_frames - 1))
      return 1;
    srec_viterbi_part1(&recm->rec[0], recm->swimodel[0], pattern, DO_NOT_COMPUTE_MODEL);
    reset_best_cost_to_zero(rec1, rec1->current_best_cost);
    reset_cost_offsets(recm, rec1->current_search_frame, rec1->current_best_cost);
    rec1->current_prune_delta = rec1->prune_delta;
    rec1->current_best_cost   = 0;
    srec_viterbi_part2(&recm->rec[0]);
    eosrc1 = srec_check_end_of_speech(eosd, &recm->rec[0]);
#if USE_COMP_STATS
    end_cs_clock1(&comp_stats->overall_search, 1);
#endif

    SREC_STATS_UPDATE(&recm->rec[0]);
    recm->eos_status = eosrc1;
#if DO_ALLOW_MULTIPLE_MODELS
    }
  else if (recm->num_activated_recs == 2)
  {
    srec* rec1 = &recm->rec[0];
    srec* rec2 = &recm->rec[1];
    const SWIModel* acoustic_models1 = recm->swimodel[0];
    const SWIModel* acoustic_models2 = recm->swimodel[1];
    costdata diff;
    costdata current_best_cost;

    ASSERT(rec1->prune_delta == rec2->prune_delta);
    /* in part 1 we need to operate by adjusting the prune delta, 'cuz we want
       to operate on scores after consumption of a frame */
    if ((rec1->current_best_cost > MAXcostdata / 2 && !rec1->srec_ended) ||
        (rec2->current_best_cost > MAXcostdata / 2 && !rec2->srec_ended))
    {
      printf("hey %d %d\n", rec1->current_best_cost, rec2->current_best_cost);
    }

    /* figure out the prune_delta for the different genders, we
       want that pruning should be joint (i.e. prune male and
       female relative to overall best).  Before part1 we don't
       yet know the overall best, so we use the gender score gap
       from the last frame, and make the prune the worse gender
       accordingly more aggressive */

    if (!rec2->srec_ended && rec1->current_best_cost < rec2->current_best_cost)
    {
      diff = rec2->current_best_cost - rec1->current_best_cost;
      if (rec2->current_search_frame >= (rec2->word_lattice->max_frames - 1))
      {
        return 1;
      }
      if (diff > rec2->prune_delta)
      {
        srec_terminate(rec2);
#ifdef SREC_ENGINE_VERBOSE_LOGGING
        PLogMessage("T: terminate_viterbi(rec2) @%d", rec2->current_search_frame);
#endif
      }
      else
        rec2->current_prune_delta = rec2->prune_delta - diff;
      rec1->current_prune_delta = rec1->prune_delta;
    }
    else if (!rec1->srec_ended)
    {
      if (rec1->current_search_frame >= (rec1->word_lattice->max_frames - 1))
      {
        return 1;
      }
      diff = rec1->current_best_cost - rec2->current_best_cost;
      if (diff > rec1->prune_delta)
      {
        srec_terminate(rec1);
#ifdef SREC_ENGINE_VERBOSE_LOGGING
        PLogMessage("T: terminate_viterbi(rec1) @%d", rec1->current_search_frame);
#endif
      }
      else
        rec1->current_prune_delta = rec1->prune_delta - diff;
      rec2->current_prune_delta = rec2->prune_delta;
    }

    /* now run part1 for each gender */
    if (!rec1->srec_ended)
    {
      srec_viterbi_part1(rec1, acoustic_models1, pattern, DO_NOT_COMPUTE_MODEL);
      SREC_STATS_UPDATE(rec1);
    }

    if (!rec2->srec_ended)
    {
      srec_viterbi_part1(rec2, acoustic_models2, pattern, rec1->current_model_scores[SILENCE_MODEL_INDEX]);
      SREC_STATS_UPDATE(rec2);
    }

    /* now adjust score offsets, score offsets are shared across genders */

    if (rec1->current_best_cost <= rec2->current_best_cost)
    {
      /* am1 is winning, prune 2 harder */
      current_best_cost = rec1->current_best_cost;
      reset_cost_offsets(recm, rec1->current_search_frame, current_best_cost);
    }
    else
    {
      /* am2 is winning, prune 1 harder */
      current_best_cost = rec2->current_best_cost;
      reset_cost_offsets(recm, rec2->current_search_frame, current_best_cost);
    }

    /* jean: some cleanup needed here */
    /** best_token_for_arc = rec1->best_token_for_arc;
      rec1->best_token_for_arc = 0; **/
    if (!rec1->srec_ended)
    {
      reset_best_cost_to_zero(rec1, current_best_cost);
      rec1->current_best_cost = (costdata)(rec1->current_best_cost - (costdata) current_best_cost);
      srec_viterbi_part2(rec1);
      if (rec1->active_fsmnode_tokens == MAXftokenID)
        srec_terminate(rec1);
      if (!rec1->srec_ended)
        eosrc1 = srec_check_end_of_speech(eosd, rec1);
    }
    /** rec1->best_token_for_arc = best_token_for_arc;
      best_token_for_arc = rec2->best_token_for_arc;
      rec2->best_token_for_arc = 0; **/
    if (!rec2->srec_ended)
    {
      reset_best_cost_to_zero(rec2, current_best_cost);
      rec2->current_best_cost = (costdata)(rec2->current_best_cost - (costdata) current_best_cost);
      srec_viterbi_part2(rec2);
      if (rec2->active_fsmnode_tokens == MAXftokenID)
        srec_terminate(rec2);
      if (!rec2->srec_ended)
        eosrc2 = srec_check_end_of_speech(eosd, rec2);
    }
    /** rec2->best_token_for_arc = best_token_for_arc; **/
    SREC_STATS_UPDATE(rec1);
    SREC_STATS_UPDATE(rec2);
    recm->eos_status = eosrc1;
    if (rec1->current_best_cost > rec2->current_best_cost)
      recm->eos_status = eosrc2;
  }
#endif
    return 0;
}


void srec_viterbi_part1(srec *rec,
                        const SWIModel *acoustic_models,
                        pattern_info *pattern,
                        costdata silence_model_cost)
{
  costdata current_best_cost;
  /*  costdata current_prune_thresh; */
  costdata current_prune_delta;
  /* the score difference for pruning - can get adjusted below if
     pruning gets tighted to keep array sizes in check*/
  costdata *current_model_scores;
  int num_models_computed;
  nodeID num_fsm_nodes_updated;

#if USE_COMP_STATS
  start_cs_clock(&comp_stats->models);
#endif

  /*first go ahead and compute scores for all models which are needed by the search at this point*/


  find_which_models_to_compute(rec, acoustic_models);
  /* communication happens via rec->current_model_scores */
#define SCORE_FIRST_SILENCE_ONLY
#ifdef SCORE_FIRST_SILENCE_ONLY
  if (silence_model_cost != DO_NOT_COMPUTE_MODEL)
    rec->current_model_scores[SILENCE_MODEL_INDEX] = silence_model_cost;
#endif
  num_models_computed = compute_model_scores(rec->current_model_scores, acoustic_models, pattern, rec->current_search_frame);
  rec->best_model_cost_for_frame[rec->current_search_frame] = best_uint16(rec->current_model_scores, acoustic_models->num_hmmstates);

#if USE_COMP_STATS
  end_cs_clock(&comp_stats->models, num_models_computed);
  start_cs_clock(&comp_stats->internal_hmm);
#endif

  /*get some things out of the rec structure to make things a bit faster*/
  current_model_scores = rec->current_model_scores;

  /*update search to next frame*/
  current_best_cost = MAXcostdata - ((costdata)2) * rec->prune_delta; /*to avoid overflows, must clean up later */
  /* current_prune_thresh = MAXcostdata; */
  current_prune_delta = rec->current_prune_delta;

  /* srec_stats_update(rec, "(...0) "); */
  /*------------------------------------------------------------------------*
    1. Handle all internal HMM updates based on new frame observations.  This is
    done in place with the current list of HMM tokens.
   *------------------------------------------------------------------------*/

  update_internal_hmm_states(rec, &current_prune_delta, &current_best_cost, current_model_scores);

  /*  check_if_any_token_better_than_best_cost(rec, rec->active_fsmarc_tokens, current_best_cost, "after update into new");*/

#if USE_COMP_STATS
  end_cs_clock(&comp_stats->internal_hmm, rec->num_new_states);
  start_cs_clock(&comp_stats->fsm_to_hmm);
#endif

  /* srec_stats_update(rec, "(...1) "); */
  /*------------------------------------------------------------------------*
    2. For each current active FSM node (from previous frame), activate update
    into state 0 (either for existing HMM tokens or for new HMM tokens) by going
    through an observation frame (so, only go from an FSM node to a new HMM
    token if the first observation frame gets a score above the current pruning
    threshold).  FSM nodes are freed as this is done.  So, no FSMnode tokens are left
    at the end of this.
   *------------------------------------------------------------------------*/

  num_fsm_nodes_updated = (nodeID) update_from_current_fsm_nodes_into_new_HMMs(rec, &current_prune_delta, &current_best_cost, current_model_scores);
  /* srec_stats_update(rec, "(...2) "); */
  /*------------------------------------------------------------------------*
    3. Prune.  Note that the best score will have already been established for
    this frame (so therefore the pruning threshold will not change).
   *------------------------------------------------------------------------*/

#if USE_COMP_STATS
  end_cs_clock(&comp_stats->fsm_to_hmm, num_fsm_nodes_updated);
  start_cs_clock(&comp_stats->prune);
#endif

  prune_new_tokens(rec, (costdata)(current_best_cost + current_prune_delta));

  /* it's nice to do word token pruning here 'cuz we only need to traceback
     the active_fsmarc_tokens, active_fsmnode_tokens are propogated thereto */

  reprune_word_tokens_if_necessary(rec);

  rec->current_prune_delta = current_prune_delta;
  rec->current_best_cost = current_best_cost;
  /* srec_stats_update(rec, "(...3) "); */
#if USE_COMP_STATS
  end_cs_clock(&comp_stats->prune, rec->num_new_states);
#endif
}

void srec_viterbi_part2(srec *rec)
{
  wtokenID word_token_index;
  nodeID inode, num_fsm_nodes_updated;
  costdata current_prune_delta = rec->current_prune_delta;
  costdata current_best_cost = rec->current_best_cost;
  ftokenID* ftmp;
  int num_updates;

  /* first we clear the best_token_for_node array, there are no live
     fsmnode_tokens at this point, and we don't want leftovers from
     the last frame */
  ftmp = rec->best_token_for_node;
  for (inode = 0; inode < rec->context->num_nodes; inode++)
    *ftmp++ = MAXftokenID;

  /*------------------------------------------------------------------------*
    4. reset best cost to 0 (to keep scores in range).  We can do this here
    since we already know the best score.  This is done here so that
    no fsmnode tokens (there are none active now) need updating.  This is also
    done here before epsilons - that way we don't need to update the word
    tokens .

    We assume this was done just before part2.
   *------------------------------------------------------------------------*/

#if USE_COMP_STATS
  start_cs_clock(&comp_stats->hmm_to_fsm);
#endif

  /*------------------------------------------------------------------------*
    5. For end hmm states which are above the pruning threshold, create new
    FSMnode_tokens.
   *------------------------------------------------------------------------*/

  num_updates = update_from_hmms_to_fsmnodes(rec, current_prune_delta, current_best_cost);
  if (num_updates == 0)
  {
    num_updates = update_from_hmms_to_fsmnodes(rec, 2 * current_prune_delta, current_best_cost);
    SREC_STATS_INC_FORCED_UPDATES();
  }
  SREC_STATS_UPDATE(rec);

#if USE_COMP_STATS
  end_cs_clock(&comp_stats->hmm_to_fsm, rec->num_new_states);
  start_cs_clock(&comp_stats->epsilon);
#endif

  /* srec_stats_update(rec, "(...5) "); */

  /*------------------------------------------------------------------------*
    6. update epsilons, including word boundary arcs (which put words onto the word lattice).
    epsilon updates go from FSM node to FSM node.
   *------------------------------------------------------------------------*/

  /*clear priority_q for this frame*/
  clear_priority_q(rec->word_priority_q);

  num_fsm_nodes_updated = (nodeID) do_epsilon_updates(rec, current_prune_delta, current_best_cost);

#if USE_COMP_STATS
  end_cs_clock(&comp_stats->epsilon, num_fsm_nodes_updated);
#endif

  /* srec_stats_update(rec, "(...6) "); */
  rec->current_search_frame++;

  /* no need to prune again after epsilons since they add no new cost - if we
     add costs to epsilon arcs (at word boundaries for example), add another
     pruning stage */

  word_token_index = get_word_token_list(rec->word_priority_q, rec->word_token_array);
  lattice_add_word_tokens(rec->word_lattice, rec->current_search_frame, word_token_index);
}

/* get the top choice, trace it back, and find out where speech starts
   and ends.  this is used for channel normalization */

static srec* WHICH_RECOG(multi_srec* rec)
{
#if DO_ALLOW_MULTIPLE_MODELS
  srec* return_rec = NULL;
  costdata current_best_cost = MAXcostdata;
  int i = 0;
  for (i = 0; i < rec->num_activated_recs; i++)
  {
    if (current_best_cost > rec->rec[i].current_best_cost)
    {
      current_best_cost = rec->rec[i].current_best_cost;
      return_rec = &rec->rec[i];
    }
  }
  return return_rec;
#else
    return &rec->rec[0];
#endif
}

void multi_srec_get_speech_bounds(multi_srec* recm, frameID* start_frame, frameID* end_frame)
{
  frameID csf;
  wtokenID token_index;
  wordID last_word;
  srec* rec = WHICH_RECOG(recm);

  *start_frame = *end_frame = 0;

  if (!rec)
    return;
  csf = rec->current_search_frame;
  token_index = rec->word_lattice->words_for_frame[csf];
  last_word = MAXwordID;
  while (token_index != MAXwtokenID)
  {
    word_token* wtoken = &rec->word_token_array[token_index];
    word_token* next_wtoken;

    if (wtoken->word == rec->context->beg_silence_word)
    {
      if (*start_frame == 0) *start_frame = wtoken->end_time;
    }
    if (wtoken->word == rec->context->hack_silence_word)
    {
      if (wtoken->backtrace != MAXwtokenID)
      {
        next_wtoken = &rec->word_token_array[wtoken->backtrace];
        if (next_wtoken->word == rec->context->beg_silence_word)
          *start_frame = wtoken->end_time;
      }
    }

    if (last_word == rec->context->end_silence_word)
    {
      *end_frame = wtoken->end_time;
      if (wtoken->word ==  rec->context->hack_silence_word
          && wtoken->backtrace != MAXwtokenID)
      {
        next_wtoken = &rec->word_token_array[wtoken->backtrace];
        *end_frame = WORD_TOKEN_GET_WD_ETIME( next_wtoken);
      }
    }
    if (token_index ==  wtoken->backtrace)
    {
      /* infinite loop! */
      PLogError ("warning: breaking infinite loop\n");
      *end_frame = 0;
      break;
    }
    token_index = wtoken->backtrace;
    last_word = wtoken->word;
  }
}

int multi_srec_get_eos_status(multi_srec* rec)
{
  int rc;
  ASSERT(rec);
  rc = (int)rec->eos_status;
  if (rc < 0) rc = 0;
  return rc;
}

  /*
     ToDo List:

     end-pointing
     duration
     channel normalization
     re-use and appropriate killing of word_tokens
     pruning fsmnode_tokens
     astar backward for alternative choices

     minimized graphs and word merging
     Johans idea:
     When propagating a fsmarc_token, we need to remember the word.id when it
     is observed.  Let's continue to use fsmarc_token->word[] to remember those.
     When merging 2+ fsmarc_tokens into a fsmnode_token, we need remember
     both histories, not just the best. All histories and maintained on a linked
     list, with word_token->next_token_index serving as links, somehow we also
     remember the cost offset from one link to the next and keep track of that.
     Try to create the word_token as late a possible, so as to keep usage down.
     The list should be sorted so that we can drop things off the end, Ie. don't
     need to keep all word, a max of 10 is fine cuz that's the most we'll need
     to drop off at a .wb anyways!

     altwords .. working .. now cpu optimize ... ideas
     use only the head refcount, #define the refcopy, not a function
     free_altword_token_batch() should not double check for AWTNULL
     BUILD & BUILD_DEBUG in selected areas
     reprune_altword_token_batch ... change costbasis to a tag ... to say (already repruned)



     endpointing
     at grammar prepare ...
     get the list of endnodes ... get the list of opendnodes
     ... start from the graph's endnode, walk backwards on all null or silence arcs, find the nodes which have a silence or null path to the end: those are sinknodes
     ... sinknodes are endnodes or opendnodes ... the sinknodesO are the sinknodes that do go to speech arcs .. the sinknodes1 are the sinknodes that do not go to any speech arcs
     ... walkforward all sinknodes0 through iwt arcs, those are openendnodes
     ... walkforward all sinknodes1 through iwt arcs, those are endnodes
     get the top score fsmnode_token ...
     ... is it on an endnode ... has this been the top choice for the last 30 frames
     ... is it on an optional endnode ... has this neen the top choice for the last 50 frames?

  */
