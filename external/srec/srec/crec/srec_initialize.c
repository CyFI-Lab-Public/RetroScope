/*---------------------------------------------------------------------------*
 *  srec_initialize.c  *
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

#ifndef _RTT
#include "pstdio.h"
#endif
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "passert.h"

#include "portable.h"

#include "hmm_desc.h"
#include "utteranc.h"
#include "hmmlib.h"

#include "srec_sizes.h"
#include "srec.h"
#include "word_lattice.h"
#include "swimodel.h"

#include "c42mul.h"

/*this file contains code which handles the initialization of the srec data structures*/

/*allocates an srec -

input args come from config and are:

    int         viterbi_prune_thresh;  score-based pruning threshold - only keep paths within this delta of best cost

    int         max_hmm_tokens;       controls the maximum number of HMM's alive in any frame.  If number
     exceeded, pruning gets tightened.  So, this threshold can be used
     to tradeoff accuracy for computation an memory
    int         max_fsmnode_tokens;   controls the maximum number of FSMs alive in any frame.  If number,
     exceeded, pruning gets tightened.  So, this threshold can be used
     to tradeoff accuracy for computation an memory
    int         max_word_tokens;      controls the maximum number of word tokens kept in the word lattice.
     if number exceeded, the word lattice is pruned more tightly (less word
     ends per frame

    int         max_altword_tokens;     controls the maximum number of alternative paths to propagate for proper nbest

    int         num_wordends_per_frame; controls the size of the word lattice - the number of word ends to
       keep at each time frame
    int         max_fsm_nodes;        allocation size of a few arrays in the search - needs to be big enough
     to handle any grammar that the search needs to run.  Initialization fails
     if num exceeded
    int         max_fsm_arcs;         allocation size of a few arrays in the search - needs to be big enough
     to handle any grammar that the search needs to run.  Initialization fails
     if num exceeded

*/

static void allocate_recognition1(srec *rec,
                                  int viterbi_prune_thresh,  /*score-based pruning threshold - only keep paths within this delta of best cost*/
                                  int max_hmm_tokens,
                                  int max_fsmnode_tokens,
                                  int max_word_tokens,
                                  int max_altword_tokens,
                                  int num_wordends_per_frame,
                                  int max_frames,
                                  int max_model_states)
{
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("allocating recognition arrays2 prune %d max_hmm_tokens %d max_fsmnode_tokens %d max_word_tokens %d max_altword_tokens %d max_wordends_per_frame %d\n",
              viterbi_prune_thresh,
              max_hmm_tokens,
              max_fsmnode_tokens,
              max_word_tokens,
              max_altword_tokens,
              num_wordends_per_frame);
#endif
  rec->current_model_scores = (costdata*) CALLOC_CLR(max_model_states, sizeof(costdata), "search.srec.current_model_scores"); /*FIX - either get NUM_MODELS from acoustic models, or check this someplace to make sure we have enough room*/
  rec->num_model_slots_allocated = (modelID)max_model_states;

  rec->fsmarc_token_array_size = (stokenID)max_hmm_tokens;

  rec->fsmarc_token_array = (fsmarc_token*) CALLOC_CLR(rec->fsmarc_token_array_size , sizeof(fsmarc_token), "search.srec.fsmarc_token_array");
  rec->max_new_states = (stokenID)max_hmm_tokens;

  rec->word_token_array = (word_token*) CALLOC_CLR(max_word_tokens, sizeof(word_token), "search.srec.word_token_array");
  rec->word_token_array_size = (wtokenID)max_word_tokens;
  /* todo: change this to a bit array later */
  rec->word_token_array_flags = (asr_int16_t*) CALLOC_CLR(max_word_tokens, sizeof(asr_int16_t), "search.srec.word_token_array_flags");

  rec->fsmnode_token_array = (fsmnode_token*) CALLOC_CLR(max_fsmnode_tokens, sizeof(fsmnode_token), "search.srec.fsmnode_token_array");
  rec->fsmnode_token_array_size = (ftokenID)max_fsmnode_tokens;

  rec->altword_token_array = (altword_token*) CALLOC_CLR(max_altword_tokens, sizeof(altword_token), "search.srec.altword_token_array");
  rec->altword_token_array_size = (wtokenID)max_altword_tokens;

  rec->prune_delta = (costdata)viterbi_prune_thresh;

  rec->max_frames   = (frameID)max_frames;
  rec->best_model_cost_for_frame = (costdata*)CALLOC_CLR(max_frames, sizeof(costdata), "search.srec.best_model_cost_for_frame");
  rec->word_lattice = allocate_word_lattice((frameID)max_frames);

  rec->word_priority_q = allocate_priority_q(num_wordends_per_frame);
  rec->best_fsmarc_token = MAXstokenID;

#define ASTAR_NBEST_LEN 10
  rec->astar_stack = astar_stack_make(rec, ASTAR_NBEST_LEN);
  rec->context = NULL;
}

static int check_parameter_range(int parval, int parmin, int parmax, const char* parname)
{
  if (parval > parmax)
  {
    log_report("Error: %s value %d is out-of-range [%d,%d]\n", parname,
               parval, parmin, parmax);
    return 1;
  }
  else
  {
    return 0;
  }
}

int allocate_recognition(multi_srec *rec,
                         int viterbi_prune_thresh,  /*score-based pruning threshold - only keep paths within this delta of best cost*/
                         int max_hmm_tokens,
                         int max_fsmnode_tokens,
                         int max_word_tokens,
                         int max_altword_tokens,
                         int num_wordends_per_frame,
                         int max_fsm_nodes,
                         int max_fsm_arcs,
                         int max_frames,
                         int max_model_states,
                         int max_searches)
{
  int i;

  if (check_parameter_range(max_fsm_nodes, 1, MAXnodeID, "max_fsm_nodes"))
    return 1;
  if (check_parameter_range(max_fsm_arcs, 1, MAXarcID, "max_fsm_arcs"))
    return 1;
  if (check_parameter_range(max_frames, 1, MAXframeID, "max_frames"))
    return 1;
  if (check_parameter_range(max_model_states, 1, MAXmodelID, "max_model_states"))
    return 1;
  if (check_parameter_range(max_hmm_tokens, 1, MAXstokenID, "max_hmm_tokens"))
    return 1;
  if (check_parameter_range(max_fsmnode_tokens, 1, MAXftokenID, "max_fsmnode_tokens"))
    return 1;
  if (check_parameter_range(viterbi_prune_thresh, 1, MAXcostdata, "viterbi_prune_thresh"))
    return 1;
  if (check_parameter_range(max_altword_tokens, 0, MAXftokenID, "max_altword_tokens"))
    return 1;
  if (check_parameter_range(max_searches, 1, 2, "max_searches"))
    return 1;

  rec->rec = (srec*)CALLOC_CLR(max_searches, sizeof(srec), "search.srec.base");
  rec->num_allocated_recs = max_searches;
  rec->num_swimodels      = 0;

  /* best_token_for_arc and best_token_for_node are shared across
     multiple searches */
  rec->best_token_for_arc = (stokenID*)CALLOC_CLR(max_fsm_arcs, sizeof(stokenID), "search.srec.best_token_for_arc");
  rec->max_fsm_arcs = (arcID)max_fsm_arcs;

  rec->best_token_for_node = (ftokenID*)CALLOC_CLR(max_fsm_nodes, sizeof(ftokenID), "search.srec.best_token_for_node");
  rec->max_fsm_nodes = (nodeID)max_fsm_nodes;

  /* cost offsets and accumulated cost offsets are pooled for all
     different searches, this saves memory and enables each search
     to know it's total scores */
  rec->cost_offset_for_frame = (costdata*)CALLOC_CLR(max_frames, sizeof(costdata), "search.srec.current_best_costs");
  rec->accumulated_cost_offset = (bigcostdata*)CALLOC_CLR(max_frames, sizeof(bigcostdata), "search.srec.accumulated_cost_offset");
  rec->max_frames = (frameID)max_frames;
  for (i = 0; i < max_frames; i++)
    rec->accumulated_cost_offset[i] = 0;

  /* now copy the shared data down to individual recogs */
  for (i = 0; i < rec->num_allocated_recs; i++)
  {
    allocate_recognition1(&rec->rec[i], viterbi_prune_thresh, max_hmm_tokens, max_fsmnode_tokens, max_word_tokens, max_altword_tokens, num_wordends_per_frame, max_frames, max_model_states);
    rec->rec[i].best_token_for_node     = rec->best_token_for_node;
    rec->rec[i].max_fsm_nodes           = rec->max_fsm_nodes;
    rec->rec[i].best_token_for_arc      = rec->best_token_for_arc;
    rec->rec[i].max_fsm_arcs            = rec->max_fsm_arcs;
    rec->rec[i].max_frames              = rec->max_frames;
    rec->rec[i].cost_offset_for_frame   = rec->cost_offset_for_frame;
    rec->rec[i].accumulated_cost_offset = rec->accumulated_cost_offset;
    rec->rec[i].id = (asr_int16_t)i;
  }
  rec->eos_status = VALID_SPEECH_NOT_YET_DETECTED;
  return 0;
}


static void free_recognition1(srec *rec)
{
  FREE(rec->current_model_scores);
  FREE(rec->fsmarc_token_array);
  FREE(rec->word_token_array);
  FREE(rec->word_token_array_flags);
  FREE(rec->fsmnode_token_array);
  FREE(rec->altword_token_array);
  FREE(rec->best_model_cost_for_frame);
  destroy_word_lattice(rec->word_lattice);
  free_priority_q(rec->word_priority_q);
  astar_stack_destroy(rec);
}

void free_recognition(multi_srec *rec)
{
  int i;
  for (i = 0; i < rec->num_allocated_recs; i++)
    free_recognition1(&rec->rec[i]);
  FREE(rec->accumulated_cost_offset);
  FREE(rec->cost_offset_for_frame);
  FREE(rec->best_token_for_node);
  FREE(rec->best_token_for_arc);
  FREE(rec->rec);
}

