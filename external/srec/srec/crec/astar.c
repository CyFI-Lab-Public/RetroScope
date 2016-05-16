/*---------------------------------------------------------------------------*
 *  astar.c  *
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

#include "pstdio.h"
#include "passert.h"

#include"srec_sizes.h"
#include"search_network.h"
#include"srec.h"
#include"srec_context.h"
#include"word_lattice.h"
#include "portable.h"
#include "srec_stats.h"
#include "astar.h"
#include "astar_pphash.h"

#ifdef SET_RCSID
static const char *rcsid = 0 ? (const char *) &rcsid :
                           "$Id: astar.c,v 1.19.4.9 2008/04/30 15:12:15 dahan Exp $";
#endif
                           
#define PRINT_ASTAR_SOMEWHAT  0
#define PRINT_ASTAR_DETAILS   0
#define PRINT_ASTAR_QBT_DETAILS   0 /* Quick Back Trace */
#define MAX_NBEST_LEN        32
#define NBEST_LEN_MARGIN     10
#define MAX_NUM_PARPS       400 /* 3*MAX_NBEST_LEN*MAX_WORDS_PER_COMPLETE_PATH need better dup 
													         check on complete paths */
#define ASTAR_PRUNE_DELTA 20000
#define DEBUG_PARP_MANAGEMENT 0

#if PRINT_ASTAR_DETAILS
static int do_draw_as_dotty = 0;
static int do_draw_file_idx = 0;

int astar_draw_tree_as_dotty(const char* file, srec* rec, AstarStack* stack);
#endif

/*
  The word graph is represented as an arc_token_list,
  arc_token's are chained together 2 linked lists,
  arc_token->first_next_arc ... like a "TO" node
  arc_token->next_arc_index ... a linked list of arcs leaving the same node

  get_arc_for_word() finds the arc_token for a particular extension
  backward though the word graph (ie. forward through the reverse word graph)

*/

#define ARC_TOKEN_ONE (arc_token*)1
arc_token* get_arc_for_word(arc_token* atoken, wordID word,
                            void* context_void,
                            wordID terminal_word)
{
  srec_context* context = (srec_context*)context_void;
  arc_token* arc_token_list = context->arc_token_list;
  arc_token* tmp;
  wordmap* wmap = context->olabels;
  
  if (atoken == ARC_TOKEN_ONE)
  {
    /* log_report("Warning:  bad thing is happening word=%d\n", word); */
    return 0;
  }
  else if (atoken == 0)
  {
    arc_token root_arc;
    root_arc.first_next_arc = ARC_TOKEN_LNK(arc_token_list, 0);
    root_arc.next_token_index = ARC_TOKEN_NULL;
    root_arc.ilabel = root_arc.olabel = 0;
    return get_arc_for_word(&root_arc, word, context_void, terminal_word);
    
    /* the arc token is NULL for partial paths just starting; but
       the word graph has nasty epsilons at the beginning, we'll remove 
       them later, but must handle them in the mean time. */
    atoken = &arc_token_list[0];
    for (; atoken; atoken = ARC_TOKEN_PTR(arc_token_list, atoken->next_token_index))
    {
      if (atoken->ilabel == word)
        return atoken;
      else if (atoken->ilabel == WORD_EPSILON_LABEL)
      {
        for (tmp = ARC_TOKEN_PTR(arc_token_list, atoken->first_next_arc); tmp;
             tmp = ARC_TOKEN_PTR(arc_token_list, tmp->next_token_index))
          if (tmp->ilabel == word)
            return tmp;
      }
      else if (atoken->ilabel < wmap->num_slots)
      {
        if (wordmap_whether_in_rule(wmap, word, atoken->ilabel))
          return atoken;
      }
    }
    return 0;
  }
  else if (word == terminal_word)
  {
    /* -pau- LABEL, the word graph does not seem to have them! */
    tmp = ARC_TOKEN_PTR(arc_token_list, atoken->first_next_arc);
    if (!tmp)
      return ARC_TOKEN_ONE;
    else if (tmp->first_next_arc == ARC_TOKEN_NULL && (tmp->ilabel == MAXwordID || tmp->ilabel == terminal_word))
      return ARC_TOKEN_ONE;
    else
    {
      /* again more weirdness in the output graph format1
      might be due to multiple endnodes? */
      for (tmp = ARC_TOKEN_PTR(arc_token_list, atoken->first_next_arc); tmp;
           tmp = ARC_TOKEN_PTR(arc_token_list, tmp->next_token_index))
        if (tmp->ilabel == MAXwordID && tmp->first_next_arc == ARC_TOKEN_NULL)
          return ARC_TOKEN_ONE;
      return 0;
    }
  }
  else
  {
#if PRINT_ASTAR_DETAILS
    printf("word %d allowed? ", word);
#endif
    if (atoken->first_next_arc == ARC_TOKEN_NULL)
      return 0;
    else
      tmp = ARC_TOKEN_PTR(arc_token_list, atoken->first_next_arc);
    /* handle single epsilons */
    if (tmp->ilabel == WORD_EPSILON_LABEL && tmp->next_token_index == ARC_TOKEN_NULL)
      tmp = ARC_TOKEN_PTR(arc_token_list, tmp->first_next_arc);
    for (; tmp; tmp = ARC_TOKEN_PTR(arc_token_list, tmp->next_token_index))
    {
#if PRINT_ASTAR_DETAILS
      printf(" W%d(%s)", tmp->ilabel, tmp->ilabel != MAXwordID ? wmap->words[tmp->ilabel] : "");
#endif
      if (tmp->ilabel == word)
      {
#if PRINT_ASTAR_DETAILS
        printf("\n");
#endif
        return tmp;
      }
      else if (tmp->ilabel < wmap->num_slots)
      {
        if (wordmap_whether_in_rule(wmap, word, tmp->ilabel))
          return tmp;
      }
    }
#if PRINT_ASTAR_DETAILS
    printf("\n");
#endif
    return 0;
  }
}

arc_token* get_arc_for_word_without_slot_annotation(arc_token* atoken, const char* word,
    void* context_void,
    wordID terminal_word)
{
  srec_context* context = (srec_context*)context_void;
  arc_token* arc_token_list = context->arc_token_list;
  arc_token* tmp;
  wordmap* wmap = context->olabels;
  wordID wdid = wordmap_find_index(wmap, word);
  
  if (atoken == ARC_TOKEN_ONE)
  {
    /* log_report("Warning:  bad thing is happening word=%d\n", word); */
    return 0;
  }
  else if (atoken == NULL)
  {
    arc_token root_arc;
    root_arc.first_next_arc = ARC_TOKEN_LNK(arc_token_list, 0);
    root_arc.next_token_index = ARC_TOKEN_NULL;
    root_arc.ilabel = root_arc.olabel = 0;
    return get_arc_for_word_without_slot_annotation(&root_arc, word, context_void, terminal_word);
  }
  else if (word == NULL)
  {
    /* -pau- LABEL, the word graph does not seem to have them! */
    tmp = ARC_TOKEN_PTR(arc_token_list, atoken->first_next_arc);
    if (!tmp)
      return ARC_TOKEN_ONE;
    else if (!tmp->first_next_arc && (tmp->ilabel == MAXwordID || tmp->ilabel == terminal_word))
      return ARC_TOKEN_ONE;
    else
    {
      /* again more weirdness in the output graph format1
      might be due to multiple endnodes? */
      for (tmp = ARC_TOKEN_PTR(arc_token_list, atoken->first_next_arc); tmp;
           tmp = ARC_TOKEN_PTR(arc_token_list, tmp->next_token_index))
        if (tmp->ilabel == MAXwordID && tmp->first_next_arc == ARC_TOKEN_NULL)
          return ARC_TOKEN_ONE;
      return 0;
    }
  }
  else
  {
    for (tmp = ARC_TOKEN_PTR(arc_token_list, atoken->first_next_arc); tmp;
         tmp = ARC_TOKEN_PTR(arc_token_list, tmp->next_token_index))
    {
      if (tmp->ilabel == wdid)
      {
        return tmp;
      }
      else if (tmp->ilabel < wmap->num_slots)
      {
        wdid = wordmap_find_index_in_rule(wmap, word, tmp->ilabel);
        if (wdid != MAXwordID)
          return tmp;
      }
      else if (tmp->ilabel == WORD_EPSILON_LABEL)
      {
        tmp = ARC_TOKEN_PTR(arc_token_list, tmp->first_next_arc);
        tmp = get_arc_for_word_without_slot_annotation(tmp, word, context_void, terminal_word);
        if (tmp) return tmp;
      }
    }
    return 0;
  }
}

/* protos */
#if DEBUG_PARP_MANAGEMENT
void list_free_parps(AstarStack* stack, char* msg);
#else
#define list_free_parps(stack,msg)
#endif

void print_partial_paths(partial_path** parps, int num_parps, srec* rec, const char* msg);
void print_path(partial_path* path, srec* rec, char* msg);
void sort_partial_paths(partial_path** parps, int num_parps);
void insert_partial_path(partial_path** parps, int *pnum_parps,
                         partial_path* insert_parp);
partial_path* make_new_partial_path(AstarStack* stack);
/*void free_partial_path(AstarStack* stack, partial_path* parp); put the proto in astar.h */

/* functions */

void append_arc_arriving(partial_path* path, partial_path* prev_path)
{
  partial_path** pprev;
  for (pprev = &path->first_prev_arc; (*pprev); pprev = &(*pprev)->linkl_prev_arc)
    ASSERT(*pprev != prev_path);
  *pprev = prev_path;
#if DEBUG_PARP_MANAGEMENT
  if (1)
  {
    int i, j;
    partial_path* path_list[256], *k;
    memset(path_list, 0, sizeof(partial_path*)*32);
    for (i = 0, k = path->first_prev_arc; k; k = k->linkl_prev_arc)
    {
      for (j = 0; j < i; j++)
        if (k == path_list[j])
          ASSERT(0);
      path_list[i++] = k;
    }
  }
#endif
}

static void remove_path_arriving(partial_path* path, partial_path* prev_path)
{
  partial_path** pprev;
  if (!path) return;
  for (pprev = &path->first_prev_arc; (*pprev); pprev = &(*pprev)->linkl_prev_arc)
    if (*pprev == prev_path)
    {
      *pprev = (*pprev)->linkl_prev_arc;
      return;
    }
  ASSERT(0);
}

partial_path* extend_path(AstarStack* stack,
                          partial_path* parp,
                          wtokenID extend_token_index,
                          arc_token* arc_for_extend_token_index,
                          bigcostdata max_cost,
                          word_token* word_token_array,
                          int* pwhether_complete)
{
  asr_int32_t netcost;
  partial_path* extended_parp;
  word_token* wtoken;
  costdata best_cost_for_node;
  wtokenID best_extend_token;
  partial_path* alt_extension;
  int sanity_count;
  
  wtoken = &word_token_array[ extend_token_index];
  
  if (wtoken->end_time > word_token_array[parp->token_index].end_time)
  {
    /* 20030921: this should never happen but we keep it for stop gap */
    /* why does it happen: when in srec_process_word_boundary() we
       occasionally kill_fsm_nodes_for_word_backtrace() but we did not kill the
       stokens for this backtrace, neither did we kill the altword_tokens.  it 
       would just take too long to go through all of them, and so occasionally 
       there may leak some bad backtraces */
    return 0;
  }
  
  /* finding the netcost of this path extension */
  best_extend_token = word_token_array[ parp->token_index].backtrace;
  best_cost_for_node = word_token_array[ best_extend_token].cost;
  wtoken = &word_token_array[ extend_token_index];
  ASSERT(word_token_array[best_extend_token].end_time ==
         word_token_array[extend_token_index].end_time);
  netcost = wtoken->cost - best_cost_for_node;
  /* ASSERT( netcost > 0); bug: sometimes this fails! fix: just use int32 */
  if (netcost + parp->costsofar > max_cost)
  {
#if PRINT_ASTAR_DETAILS
    printf("netcost %d (%d+%d) + parp->costsofar > max_cost %d\n",
           netcost, wtoken->cost, best_cost_for_node, parp->costsofar, max_cost);
#endif
    return 0;
  }
  
  /* check if we have a similar thing already extended */
  sanity_count = 0;
  for (alt_extension = parp->first_prev_arc; alt_extension;
       alt_extension = alt_extension->linkl_prev_arc)
  {
    wtokenID alt_token_index = alt_extension->token_index;
    wtokenID alt_bt_token_index;
    wtokenID bt_token_index;
    word_token* alt_wtoken;
    int join_frame_diff; /* need a signed frameID */
    
    ASSERT(sanity_count++ < 30);
    if (alt_token_index == MAXwtokenID)
      continue;
    alt_wtoken = &word_token_array[alt_token_index];
    if (alt_wtoken->word != wtoken->word)
      continue;
      
    alt_bt_token_index = alt_wtoken->backtrace;
    bt_token_index = wtoken->backtrace;
    if (alt_bt_token_index == MAXwtokenID && bt_token_index != MAXwtokenID)
      continue;
    else if (alt_bt_token_index != MAXwtokenID && bt_token_index == MAXwtokenID)
      continue;
    else if (alt_bt_token_index != MAXwtokenID && bt_token_index != MAXwtokenID)
    {
      word_token* alt_wtoken_bt;
      word_token* wtoken_bt;
      alt_wtoken_bt = &word_token_array[ alt_wtoken->backtrace];
      wtoken_bt = &word_token_array[ wtoken->backtrace];
      if (alt_wtoken_bt->word != wtoken_bt->word)
        continue;
    }
    
    join_frame_diff = alt_wtoken->end_time - wtoken->end_time;
    if (join_frame_diff < 0) join_frame_diff = -join_frame_diff;
    if (join_frame_diff > 5)
      continue;
      
    /* the proposed extension is similar in "all" ways to an existing
       extension, so let's not make this new extension */
#if PRINT_ASTAR_DETAILS
    printf("proposed extension already done elsewhere\n");
#endif
    return 0;
  }
  
  /* this is a TRUE new extension, so let's extend for sure */
  extended_parp = make_new_partial_path(stack);
  if (!extended_parp)
  {
#if PRINT_ASTAR_DETAILS
    printf("make_new_partial_path returned 0\n");
#endif
    return 0;
  }
  extended_parp->costsofar = parp->costsofar + netcost;
  extended_parp->token_index = extend_token_index;
  if (extend_token_index != MAXwtokenID)
    extended_parp->word = word_token_array[ extend_token_index].word;
  else
    extended_parp->word = MAXwordID;
  if (wtoken->backtrace == MAXwtokenID)
  {
    *pwhether_complete = 1;
    extended_parp->first_prev_arc = PARP_TERMINAL;
  }
  else
  {
    *pwhether_complete = 0;
  }
  extended_parp->arc_for_wtoken = arc_for_extend_token_index;
  
  extended_parp->refcount = 1;
  parp->refcount++;
  
  /* maintain the parp tree */
  extended_parp->next = parp;
  append_arc_arriving(parp, extended_parp);
#if PRINT_ASTAR_DETAILS
  printf("extend path returned %x\n", extended_parp);
#endif
  
  return extended_parp;
}

void check_stack_root_sanity(AstarStack* stack)
{
  partial_path* parp1 = stack->root_path;
  /* append_arc_arriving(stack->root_path, parp); */
  for (; parp1->linkl_prev_arc != NULL; parp1 = parp1->linkl_prev_arc)
    ASSERT(parp1 != parp1->linkl_prev_arc);
}

/*
 * make a blank partial path, free one if necessary
 */

partial_path* make_new_partial_path(AstarStack* stack)
{
  partial_path* return_parp = stack->free_parp_list;
  if (return_parp)
  {
    stack->free_parp_list = return_parp->next;
    memset((void*)return_parp, 0, sizeof(*return_parp)); /* needed */
  }
  else
  {
    log_report("Warning: ran out of partial_paths, reprune\n");
#if PRINT_ASTAR_DETAILS
    printf("Warning: ran out of partial_paths, reprune\n");
#endif
    /* kill the last one, and return it, this may free more than one parp! */
    if (stack->num_active_paths == 0)
      return 0;
    stack->num_active_paths--;
    return_parp = stack->active_paths[stack->num_active_paths];
    hash_del((FixedSizeHash*)stack->pphash, return_parp);
    free_partial_path(stack, return_parp);
    return_parp = stack->free_parp_list;
    stack->free_parp_list = return_parp->next;
    memset((void*)return_parp, 0, sizeof(*return_parp)); /* needed */
  }
  return return_parp;
}

/* free_partial_path
   frees the path that was passed in, and also any backpointers.
   refcount counts the number of parps that depend on this parp */
void free_partial_path(AstarStack* stack, partial_path* parp)
{
  partial_path* next_parp;
  for (; parp; parp = next_parp)
  {
    next_parp = parp->next;
    parp->refcount--;
    if (parp->refcount == 0)
    {    /* first time around this always passes */
      remove_path_arriving(parp->next, parp);
      parp->next = stack->free_parp_list;
      stack->free_parp_list = parp;
    }
    else break;
  }
}

/*
 * make a partial path from a single word at the very end of the graph
 */

partial_path* make_partial_path(AstarStack* stack,
                                wtokenID token_index, srec* rec,
                                int* pwhether_complete)
{
  partial_path* parp;
  word_token* wtoken;
  
  /* todo: replace this with partial_path_tokens! */
  parp = make_new_partial_path(stack);
  if (!parp) return parp;
  
  wtoken = &rec->word_token_array[token_index];
  parp->token_index = token_index;
  if (token_index != MAXwtokenID)
    parp->word = rec->word_token_array[ token_index].word;
  else
    parp->word = MAXwordID;
  /* wtoken->end_time should be equal to rec->current_search_frame */
  ASSERT(rec->accumulated_cost_offset[ wtoken->end_time] != 0);
  parp->costsofar = rec->accumulated_cost_offset[ wtoken->end_time];
  parp->costsofar += wtoken->cost;
  /* BKWD: rec->word_token_array[ wtoken->backtrace].cost + acc[] */
  /* FRWD: wtoken->cost + acc[] - rec->word_token_array[ wtoken->backtrace].cost + acc[] */
  parp->next = 0;
  parp->first_prev_arc = parp->linkl_prev_arc = 0;
  if (wtoken->backtrace == MAXwtokenID)
    *pwhether_complete = 1;
  else
    *pwhether_complete = 0;
  parp->arc_for_wtoken = 0;
  parp->refcount = 1;
  return parp;
}

/* initialize astar search */

AstarStack* astar_stack_make(srec* rec, int max_nbest_len)
{
  int i;
  AstarStack *stack;
  
  /* allocations */
  stack = (AstarStack*)CALLOC_CLR(1, sizeof(AstarStack), "search.astar.base");
  stack->max_active_paths = max_nbest_len + NBEST_LEN_MARGIN * 3;
  stack->max_complete_paths = max_nbest_len + NBEST_LEN_MARGIN;
  stack->complete_paths = (partial_path**)CALLOC_CLR(stack->max_complete_paths, sizeof(partial_path*), "search.astar.cplist");
  stack->complete_path_confidences = (int*)CALLOC_CLR(stack->max_complete_paths, sizeof(int), "search.astar.confvalues");
  stack->active_paths = (partial_path**)CALLOC_CLR(stack->max_active_paths, sizeof(partial_path*), "search.astar.aplist");
  stack->prune_delta = ASTAR_PRUNE_DELTA;
  
  stack->num_complete_paths = 0;
  stack->num_active_paths = 0;
  
  stack->partial_path_array = (partial_path*)CALLOC_CLR(MAX_NUM_PARPS, sizeof(stack->partial_path_array[0]), "search.astar.pparray");
  stack->partial_path_array_size = MAX_NUM_PARPS;
  
  stack->free_parp_list = &stack->partial_path_array[0];
  for (i = 1; i < MAX_NUM_PARPS; i++)
  {
    stack->partial_path_array[i-1].next = &stack->partial_path_array[i];
  }
  stack->partial_path_array[i-1].next = 0;
  stack->root_path = 0;
  
  stack->pphash = (void*)CALLOC_CLR(1, sizeof(FixedSizeHash), "search.astar.pphash");
  astar_stack_clear(stack);
  return stack;
}

int astar_stack_destroy(srec* rec)
{
  AstarStack *stack = rec->astar_stack;
  FREE(stack->active_paths);
  FREE(stack->complete_paths);
  FREE(stack->complete_path_confidences);
  FREE(stack->partial_path_array);
  FREE(stack->pphash);
  FREE(stack);
  rec->astar_stack = 0;
  return 0;
}

/* prepares for astar after forward search on an utterance */

int astar_stack_prepare(AstarStack* stack, int request_nbest_len, srec* rec)
{
  wtokenID token_index;
  word_token* wtoken;
  partial_path* parp;
  int whether_complete;
  frameID end_frame = rec->current_search_frame;
  int num_wordends;
  
  list_free_parps(stack, "astar_stack_prepare ");
  
  stack->num_active_paths = 0;
  stack->num_complete_paths = 0;
  
  stack->root_path = make_new_partial_path(stack);
  ASSERT(stack->root_path);
  stack->root_path->refcount = 9999;
  stack->root_path->token_index = MAXwtokenID;
  stack->root_path->word = MAXwordID;
  
  num_wordends = 0;
  for (token_index = rec->word_lattice->words_for_frame[end_frame];
       token_index != MAXwtokenID;
       token_index = wtoken->next_token_index)
  {
    num_wordends++;
    wtoken = &rec->word_token_array[ token_index];
    parp = make_partial_path(stack, token_index, rec, &whether_complete);
    if (!parp)
    {
      log_report("Error: out-of-memory in astar_stack_prepare(), "
                 "num_wordends %d\n", num_wordends);
      stack->num_complete_paths = 0;
      return 1;
    }
    append_arc_arriving(stack->root_path, parp);
    
    if (parp && whether_complete)
    {
      /* here .. check for dups ?? */
      stack->complete_paths[ stack->num_complete_paths++] = parp;
      if (stack->num_complete_paths == request_nbest_len)
        return 0;
    }
    else if (parp)
    {
      stack->active_paths[ stack->num_active_paths++] = parp;
    }
  }
  
  list_free_parps(stack, "astar_stack_prepare ");
  
  return 0;
}

/* cleans up astar after an utterance */

void astar_stack_clear(AstarStack* stack)
{
  int i;
  
  /* free the partial_path's that were allocated */
  for (i = 0; i < stack->num_active_paths; i++)
    free_partial_path(stack, stack->active_paths[i]);
  for (i = 0; i < stack->num_complete_paths; i++)
    free_partial_path(stack, stack->complete_paths[i]);
  if (stack->root_path)
    free_partial_path(stack, stack->root_path);
    
  /* this shouldn't be necessary, but there are a couple of bugs
     in parp management, so let's leave it for now */
  stack->free_parp_list = &stack->partial_path_array[0];
  for (i = 1; i < MAX_NUM_PARPS; i++)
    stack->partial_path_array[i-1].next = &stack->partial_path_array[i];
  stack->partial_path_array[i-1].next = 0;
  stack->num_active_paths = 0;
  stack->num_complete_paths = 0;
  stack->root_path = 0;
  
  list_free_parps(stack, "astar_stack_clear ");
  
}

/* do the astar search */

int astar_stack_do_backwards_search(srec* rec, int request_nbest_len)
{
  int i;
  AstarStack *stack = rec->astar_stack;
  word_token *wtoken, *btoken;
  partial_path *parp, *extended_parp, *tparp;
  wtokenID token_index, btoken_index;
  int whether_complete = 0;
  bigcostdata max_cost = 0;
  arc_token* arc_for_token_index = NULL;
  
  arc_token* arc_token_list;   /* to skip graph constraints, just set this to NULL */
  arcID arc_token_list_len;
  srec_word_lattice* lattice;
  
  int max_complete_paths;
  
  if (!rec || !rec->context)
  {
    log_report("Error: bad arguments in astar_stack_do_backwards_search()\n");
    return 1;
  }
  max_complete_paths = request_nbest_len < stack->max_complete_paths ?
                       request_nbest_len : stack->max_complete_paths;
                       
  arc_token_list = rec->context->arc_token_list;
  arc_token_list_len = rec->context->arc_token_list_len;
  lattice = rec->word_lattice;
  
  /* initialization, now from calling function */
  /* astar_stack_prepare(stack, request_nbest_len, rec); */
  hash_init((FixedSizeHash*)stack->pphash, rec);
  
  /* search */
  while (stack->num_active_paths > 0)
  {
  
    list_free_parps(stack, "do_astar_back BEG");
    
    /* extend top path */
    parp = stack->active_paths[0];
    wtoken = &rec->word_token_array[parp->token_index];
    token_index = wtoken->backtrace;
    wtoken = &rec->word_token_array[token_index];
    ASSERT(token_index != MAXwtokenID); /* should have been "complete" */
    
    
#if PRINT_ASTAR_DETAILS
    print_partial_paths(stack->complete_paths, stack->num_complete_paths,
                        rec, "=== Complete Paths ===\n");
    print_partial_paths(stack->active_paths, stack->num_active_paths,
                        rec, "=== Active Paths ===\n");
#endif
                        
    /* pop this one */
    for (i = 0; i < stack->num_active_paths - 1; i++)
      stack->active_paths[i] = stack->active_paths[i+1];
    stack->num_active_paths--;
    
    if (wtoken->end_time != MAXframeID)
    {
      /* sort the word token array by score, so that we pick the best
      scoring paths first */
      /* later add a 'whether_sorted' flag to the lattice_at_frame information */
      sort_word_lattice_at_frame(rec, (frameID)(wtoken->end_time + 1));
      
      /* extend this path, with every word ending where this word began */
      /* #warning there appear to be duplicates */
      
      btoken_index = lattice->words_for_frame[ wtoken->end_time+1];
    }
    else
    {
      btoken_index = MAXwtokenID;
    }
    
#if PRINT_ASTAR_DETAILS
    print_path(parp, rec, "Now Processing Top of Stack(2): ");
    printf("Frame %d\n", wtoken->end_time + 1);
    print_word_token_list(rec, btoken_index, "List of Word at Frame\n");
#endif
    
    for (; btoken_index != MAXwtokenID; btoken_index = btoken->next_token_index)
    {
      btoken = &rec->word_token_array[btoken_index];
      
      /* alternate choice must end at same frame */
      //      ASSERT(btoken->end_time == wtoken->end_time);
      
#if PRINT_ASTAR_DETAILS
      print_path(parp, rec, "Now Processing Top of Stack(3): ");
      print_word_token(rec, btoken_index, "Extending word ");
#endif
      
      /* check if this potential extension is allowed by the
      word graph, if not just drop it! */
      
      if (arc_token_list)
      {
        arc_for_token_index = get_arc_for_word(parp->arc_for_wtoken,
                                               btoken->word,
                                               rec->context,
                                               rec->context->beg_silence_word);
        if (arc_for_token_index == NULL)
        {
#if PRINT_ASTAR_DETAILS
          printf("Not allowed by graph!\n");
#endif
          continue;
        }
      }
      
      /* figure out the cost to beat ! */
      if (stack->num_complete_paths)
      {
        max_cost = stack->complete_paths[0]->costsofar + stack->prune_delta;
      }
      else if (stack->num_active_paths == stack->max_active_paths)
      {
        max_cost = stack->active_paths[ stack->num_active_paths-1]->costsofar;
      }
      else if (stack->num_active_paths > 0)
      {
        max_cost = stack->active_paths[0]->costsofar + stack->prune_delta;
      }
      else
      {
        max_cost = MAXbcostdata;
      }
      
      extended_parp = extend_path(stack, parp, btoken_index, arc_for_token_index, max_cost, rec->word_token_array, &whether_complete);
      
      if (extended_parp)
      {
        int fsh_rc = hash_set((FixedSizeHash*)stack->pphash, extended_parp);
        if (fsh_rc == FSH_KEY_OCCUPIED)
        {
          /* seen this path before, let's not bother with it */
#if PRINT_ASTAR_DETAILS
          print_path(extended_parp, rec, "dup!! ");
#endif
          free_partial_path(stack, extended_parp);
          extended_parp = 0;
        }
      }
      
      if (extended_parp && whether_complete)
      {
        ASSERT(stack->num_complete_paths < stack->max_complete_paths);
        stack->complete_paths[ stack->num_complete_paths++] = extended_parp;
        /*if(stack->num_complete_paths >= request_nbest_len)
          return 0;*/
        
        
#if PRINT_ASTAR_DETAILS
        print_path(extended_parp, rec, "&&Extended, complete : ");
#endif
      }
      else if (extended_parp)
      {
        /* todo: check if this extended_parp is already completed on the
           stack->complete_paths, if so just rejoin with that guy somehow */
        
#if PRINT_ASTAR_DETAILS
        print_path(extended_parp, rec, "&&Extended, incomplete : ");
#endif
        if (stack->num_active_paths == stack->max_active_paths)
        {
          /* kill the last one */
          stack->num_active_paths--;
          tparp = stack->active_paths[stack->num_active_paths];
          hash_del((FixedSizeHash*)stack->pphash, tparp);
          free_partial_path(stack, tparp);
        }
        insert_partial_path(stack->active_paths, &stack->num_active_paths,
                            extended_parp);
      }
#if PRINT_ASTAR_DETAILS
      else
      {
        printf("&&Extended, cost too high (>%d):\n", max_cost);
      }
#endif
      if (stack->num_complete_paths == max_complete_paths)
      {
#if PRINT_ASTAR_DETAILS
        printf("Complete paths are full %d, stopping\n", stack->num_complete_paths);
#endif
        break;
      }
    }
#if PRINT_ASTAR_DETAILS
    if (do_draw_as_dotty > 0)
    {
      char tmp[32];
      sprintf(tmp, "astar.%.3d.dot", do_draw_file_idx++);
      astar_draw_tree_as_dotty(tmp, rec, stack);
      if (do_draw_as_dotty > 1)
        system("C:/tools/graphviz/bin/dotty.exe astar.dot");
    }
#endif
    
    SREC_STATS_UPDATE_ASTAR(stack);
    hash_del((FixedSizeHash*)stack->pphash, parp);
    free_partial_path(stack, parp); /* done all extensions, now free */
    if (stack->num_complete_paths == max_complete_paths)
    {
#if PRINT_ASTAR_DETAILS
      printf("Complete paths are full %d, stopping\n", stack->num_complete_paths);
#endif
      break;
    }
    
    list_free_parps(stack, "do_astar_back END");
  }
  sort_partial_paths(stack->complete_paths, stack->num_complete_paths);
  /* if we're doing a search within a grammar, then print the complete choices
     else we're likely just doing reprune_word_tokens() */
#if PRINT_ASTAR_SOMEWHAT
  if (rec->context->arc_token_list)
    print_partial_paths(stack->complete_paths, stack->num_complete_paths,
                        rec, "=== Complete paths ===\n");
#endif
  /* now the caller must call clear */
  /* astar_stack_clear(stack); */
  
  return 0;
}

void sort_partial_paths(partial_path** parps, int num_parps)
{
  int i, j;
  for (i = 0; i < num_parps; i++)
  {
    for (j = 0; j < num_parps - 1; j++)
    {
      if (parps[j]->costsofar > parps[j+1]->costsofar)
      {
        partial_path* parp = parps[j];
        parps[j] = parps[j+1];
        parps[j+1] = parp;
      }
    }
  }
}

void insert_partial_path(partial_path** parps, int *pnum_parps, partial_path* insert_parp)
{
  int i, j, insert_index;
  int num_parps = *pnum_parps;
  
  /* maintain the list sorted, search the list linearly for now,
     do priority_q type heap later */
  insert_index = num_parps;
  for (i = 0; i < num_parps; i++)
  {
    if (insert_parp->costsofar < parps[i]->costsofar)
    {
      insert_index = i;
      break;
    }
  }
  for (j = num_parps; j > insert_index; --j)
    parps[j] = parps[j-1];
  parps[j] = insert_parp;
  num_parps++;
  *pnum_parps = num_parps;
}

void print_path(partial_path* ipath, srec* rec, char* msg)
{
  partial_path* path;
  word_token* wtoken;
  word_token* last_wtoken;
  char* p;
  char trans[256];
  int max_trans_len = 255;
  int rc;
#ifndef NDEBUG
  int sanity_count = 0;
#endif
  frameID end_time;
  
  PLogMessage("%spath score=%d ", msg, ipath->costsofar);
  
  rc = sprint_word_token_backtrace(trans, max_trans_len, rec, ipath->token_index);
  ASSERT(rc == 0);
  
  last_wtoken = 0;
  end_time = (ipath && ipath->token_index != MAXwtokenID) ? rec->word_token_array[ipath->token_index].end_time : MAXframeID;
#if SHOW_END_TIMES
  printf("%s@%d || ", trans, end_time);
#else
  printf("%s || ", trans);
#endif
  
  path = ipath->next;  /* we've already printed this thing */
  for (; path; path = path->next)
  {
    ASSERT(sanity_count++ < 256);
    if (path->token_index == MAXwtokenID) break;
    wtoken = &rec->word_token_array[ path->token_index];
    p = "NULL";
    if (rec->context->olabels->words[wtoken->word])
      p = rec->context->olabels->words[wtoken->word];
#if SHOW_END_TIMES
    printf("%s@%d ", p, wtoken->end_time);
#else
    printf("%s ", p);
#endif
    if (last_wtoken != NULL)
    {
      if (wtoken->end_time < last_wtoken->end_time)
      {
        printf(" Error: wt%d < lwt%d\n", wtoken->end_time, last_wtoken->end_time);
        pfflush(PSTDOUT);
        ASSERT(0);
      }
    }
    last_wtoken = wtoken;
  }
  printf("\n");
}

void print_partial_paths(partial_path** parps, int num_parps,
                         srec* rec, const char* msg)
{
  int i;
  char buf[32];
  printf("%s", msg);
  for (i = 0; i < num_parps; i++)
  {
    sprintf(buf, "%.3d ", i);
    print_path(parps[i], rec, buf);
  }
}


/*--------------------------------------------------------------------------*
 *                                                                          *
 * visualization .. sometimes helps debugging                               *
 *                                                                          *
 *--------------------------------------------------------------------------*/

#if PRINT_ASTAR_DETAILS


int astar_draw_arc_as_dotty(FILE* fp, partial_path* parp, int src_node, int *nodes_used, srec* rec)
{
  word_token* word_token_array = rec->word_token_array;
  partial_path* parp1;
  int sanity_count = 0;
  int to_nodes[32], *pto_nodes, inode;
  pto_nodes = to_nodes;
  
  fprintf(fp, "%d [label = \"%d\", shape = circle, style = solid]\n",
          src_node, src_node);
  for (parp1 = parp->first_prev_arc; parp1; parp1 = parp1->linkl_prev_arc)
  {
    arc_token* arc = parp1->arc_for_wtoken;
    for (inode = 0; nodes_used[inode]; inode++) ;
    nodes_used[inode] = 1;
    *pto_nodes++ = inode;
    fprintf(fp, "  %d -> %d [label = \"(%d).%d.%s@%d/%d\"];\n",
            src_node, inode, parp1->refcount,
            parp1->token_index,
            (arc && arc != (arc_token*)1) ? rec->context->olabels->words[arc->ilabel] : "NULL",
            word_token_array[ parp1->token_index].end_time,
            parp1->costsofar);
    if (sanity_count++  > 30) break;
  }
  
  pto_nodes = to_nodes;
  sanity_count = 0;
  for (parp1 = parp->first_prev_arc; parp1; parp1 = parp1->linkl_prev_arc)
  {
    astar_draw_arc_as_dotty(fp, parp1, *pto_nodes, nodes_used, rec);
    pto_nodes++;
    if (sanity_count++  > 30) break;
  }
  return 0;
}

int astar_draw_tree_as_dotty(const char* file, srec* rec, AstarStack* stack)
{
  word_token* word_token_array = rec->word_token_array;
  FILE* fp = fopen(file, "w");
  partial_path* parp;
  int nodes_used[1024];
  memset(nodes_used, 0, 1024*sizeof(int));
  
  fprintf(fp,
          "digraph FSM {\n"
          "rankdir = LR;\n"
          "size = \"8.5,11\";\n"
          "fontsize = 14;\n"
          "label = \"\\n\\naaron.PCLG\"\n"
          "center = 1;\n"
          "orientation = Landscape\n"
         );
  nodes_used[0] = 1;
  for (parp = stack->root_path; parp; parp = parp->linkl_prev_arc)
    astar_draw_arc_as_dotty(fp, parp, 0, nodes_used, rec);
    
  fprintf(fp, "}\n");
  fclose(fp);
  printf("....... dotty %s ......\n", file);
  return 0;
}

#endif

/*--------------------------------------------------------------------------*
 *                                                                          *
 * functions relating to in-recognition backtrace                           *
 *                                                                          *
 *--------------------------------------------------------------------------*/

int maybe_add_to_active_paths(AstarStack* stack, word_token* word_token_array, bigcostdata cost, wtokenID wtoken_index);
int astar_stack_prepare_from_active_search(AstarStack* stack, int nbestlen, srec* rec)
{
  wtokenID wtoken_index;
  ftokenID ftoken_index;
  fsmnode_token* ftoken;
  stokenID stoken_index;
  fsmarc_token* stoken;
  /* word_token* wtoken; */
  frameID prune_frame = rec->current_search_frame;
  int i, rc = 0, rc1 = 0;
  bigcostdata parp_costsofar;
  
  stack->num_active_paths = 0;
  stack->num_complete_paths = 0;
  stack->root_path = 0;
  
  /* put it on the stack */
  stack->root_path = make_new_partial_path(stack);
  ASSERT(stack->root_path);
  stack->root_path->refcount = 9999;
  stack->root_path->token_index = MAXwtokenID;
  stack->root_path->word = MAXwordID;
  
  ftoken_index = rec->active_fsmnode_tokens;
  for (; ftoken_index != MAXftokenID; ftoken_index = ftoken->next_token_index)
  {
    ftoken = &rec->fsmnode_token_array[ ftoken_index];
    wtoken_index = ftoken->word_backtrace;
    if (wtoken_index == MAXwtokenID)
      continue;
      
    /* fix the score */
    parp_costsofar = ftoken->cost;
    parp_costsofar += rec->accumulated_cost_offset[ prune_frame];
    
    rc += (rc1 = maybe_add_to_active_paths(stack, rec->word_token_array, parp_costsofar, wtoken_index));
    /* we can handle that a path was not added for this ftoken because
       we made sure to flag the wtokens along it's top backtrace */
  }
  
  stoken_index = rec->active_fsmarc_tokens;
  for (; stoken_index != MAXstokenID; stoken_index = stoken->next_token_index)
  {
    stoken = &rec->fsmarc_token_array[ stoken_index];
    for (i = 0; i < stoken->num_hmm_states; i++)
    {
      wtoken_index = stoken->word_backtrace[i];
      if (wtoken_index == MAXwtokenID)
        continue;
      parp_costsofar = stoken->cost[i];
      parp_costsofar += rec->accumulated_cost_offset[ prune_frame];
      
      rc += (rc1 = maybe_add_to_active_paths(stack, rec->word_token_array, parp_costsofar, wtoken_index));
      /* we can handle that a path was not added for this stoken because
      we made sure to flag the wtokens along it's top backtrace */
    }
  }
  
#if PRINT_ASTAR_DETAILS
  print_partial_paths(stack->active_paths, stack->num_active_paths,
                      rec, "== active paths before sorting ==\n");
  sort_partial_paths(stack->active_paths, stack->num_active_paths);
  print_partial_paths(stack->active_paths, stack->num_active_paths,
                      rec, "== active paths after sorting ==\n");
#endif
  list_free_parps(stack, "astar_prepare_from_active_search");
  return 0;
}

int maybe_add_to_active_paths(AstarStack* stack, word_token* word_token_array, bigcostdata parp_costsofar, wtokenID wtoken_index)
{
  int i;
  int replace_index;
  int inserts_index;
  partial_path* parp;
  word_token* wtoken;
  wtoken = &word_token_array[ wtoken_index];
  
  if (wtoken->backtrace == MAXwtokenID)
  {
    return 0;
  }
  
  /* see if we already have this word token backtrace */
  replace_index = -1;
  inserts_index = -1;
  for (i = 0; i < stack->num_active_paths; i++)
  {
    if (stack->active_paths[i]->token_index == wtoken_index)
    {
      if (parp_costsofar < stack->active_paths[i]->costsofar)
      {
        /* this one is better than another we already have! */
        replace_index = i;
        if (inserts_index < 0)
          inserts_index = i;
      }
      break;
    }
    else if (parp_costsofar < stack->active_paths[i]->costsofar)
    {
      if (inserts_index < 0)
        inserts_index = i;
    }
  }
#if PRINT_ASTAR_QBT_DETAILS
  printf("maybe_add replace %d insert %d\n", replace_index, inserts_index);
#endif
  
  if (replace_index >= 0)
  {
    free_partial_path(stack, stack->active_paths[replace_index]);
    /* stack->active_paths[replace_index] = 0; */
    for (i = replace_index; i > inserts_index; --i)
      stack->active_paths[i] = stack->active_paths[i-1];
    stack->active_paths[inserts_index] = 0;
  }
  else if (inserts_index >= 0)
  {
    if (stack->num_active_paths == stack->max_active_paths)
    {
      free_partial_path(stack, stack->active_paths[ stack->num_active_paths-1]);
      stack->num_active_paths--;
    }
    for (i = stack->num_active_paths; i > inserts_index; --i)
      stack->active_paths[i] = stack->active_paths[i-1];
    stack->active_paths[inserts_index] = 0;
    stack->num_active_paths++;
  }
  else if (stack->num_active_paths < stack->max_active_paths)
  {
    /* append if there's space */
    inserts_index = stack->num_active_paths;
    stack->num_active_paths++;
    stack->active_paths[inserts_index] = 0;
  }
  else
  {
    /* no space */
    return 1;
  }
  
  /* create a parp */
#if PRINT_ASTAR_QBT_DETAILS
  printf("maybe_add .. creating new parp %d\n", parp_costsofar);
#endif
  /* this should always succeed because of above frees */
  ASSERT(stack->free_parp_list);
  parp = make_new_partial_path(stack);
  parp->token_index = wtoken_index;
  if (wtoken_index != MAXwtokenID)
    parp->word = word_token_array[ wtoken_index].word;
  else
    parp->word = MAXwordID;
  parp->next = stack->root_path;
  parp->first_prev_arc = parp->linkl_prev_arc = 0;
  parp->arc_for_wtoken = 0;
  parp->refcount = 1;
  parp->costsofar = parp_costsofar;
  
  stack->active_paths[ inserts_index] = parp;
  
#if PRINT_ASTAR_QBT_DETAILS
  printf("maybe_add .. appending to root\n");
#endif
  append_arc_arriving(stack->root_path, parp);
  return 0;
}


int astar_stack_flag_word_tokens_used(AstarStack* stack, srec* rec)
{
  int i;
  wtokenID wtoken_index;
  partial_path* parp;
  int num_flagged_by_path;
  
#if PRINT_ASTAR_QBT_DETAILS
  print_partial_paths(stack->complete_paths, stack->num_complete_paths,
                      rec, "=== Complete QBT paths ===\n");
#endif
                      
  for (i = 0; i < stack->num_complete_paths; i++)
  {
    num_flagged_by_path = 0;
    for (parp = stack->complete_paths[i]; parp; parp = parp->next)
    {
      wtoken_index = parp->token_index;
      if (wtoken_index == MAXwtokenID) break;
      rec->word_token_array_flags[ wtoken_index]++;
      if (rec->word_token_array_flags[wtoken_index] > 0)
      {
        num_flagged_by_path++;
#if PRINT_ASTAR_QBT_DETAILS
        printf("%d ", wtoken_index);
#endif
      }
    }
    
    /* also flag the main backtrace of every word token */
    /* we do we need this?  I'm not sure, but it appears
       that some backtrace tokens are not flagged for whatever
       reason.  It's worth revisiting this when other bugs are
       are resolved.  This is in a separate loop from the 
       above because it allows us to detect that this is
       happening in the first place */
    for (parp = stack->complete_paths[i]; parp; parp = parp->next)
    {
      word_token *btoken, *last_btoken;
      wtokenID btoken_index;
      wtoken_index = parp->token_index;
      if (wtoken_index == MAXwtokenID) break;
      last_btoken = NULL;
      btoken = &rec->word_token_array[ wtoken_index];
      btoken_index = btoken->backtrace;
      for (; btoken_index != MAXwtokenID; btoken_index = btoken->backtrace)
      {
        btoken = &rec->word_token_array[ btoken_index];
        rec->word_token_array_flags[ btoken_index]++;
        if (rec->word_token_array_flags[ btoken_index] == 1)
        {
          num_flagged_by_path++;
#if PRINT_ASTAR_QBT_DETAILS
          printf("%db ", btoken_index);
#endif
        }
        if (last_btoken && last_btoken->end_time <= btoken->end_time)
        {
          PLogError("bad looping path encountered, breaking");
          break;
        }
        last_btoken = btoken;
      }
    }
    
#if PRINT_ASTAR_QBT_DETAILS
    printf("complete path %.3d flagged %d\n", i, num_flagged_by_path);
#endif
  }
  return 0;
}


#if DEBUG_PARP_MANAGEMENT
#define PARP_FREE 1
#define PARP_USED 2
void list_free_parps(AstarStack* stack, char* msg)
{
  partial_path* parp;
  int i, num = 0;
  char x[MAX_NUM_PARPS];
  for (i = 0; i < MAX_NUM_PARPS; i++) x[i] = 0;
  for (parp = stack->free_parp_list; parp; parp = parp->next)
  {
    num++;
    x[(parp-stack->partial_path_array)] = PARP_FREE;
  }
  PLogMessage("%sstack->free_parp_list size %d ", msg, num);
  PLogMessage("active %d complete %d\n", stack->num_active_paths, stack->num_complete_paths);
  
  for (i = 0; i < stack->num_active_paths; i++)
  {
    parp = stack->active_paths[i];
    for (; parp; parp = parp->next) x[(parp-stack->partial_path_array)] = PARP_USED;
  }
  for (i = 0; i < stack->num_complete_paths; i++)
  {
    parp = stack->complete_paths[i];
    for (; parp; parp = parp->next) x[(parp-stack->partial_path_array)] = PARP_USED;
  }
  if (stack->root_path)
    x[(stack->root_path-stack->partial_path_array)] = PARP_USED;
  printf("free: ");
  for (i = 0; i < MAX_NUM_PARPS; i++) if (x[i] == PARP_FREE) printf(" %d", i);
  printf("\n");
  printf("used: ");
  for (i = 0; i < MAX_NUM_PARPS; i++) if (x[i] == PARP_USED) printf(" %d", i);
  printf("\n");
  for (i = 0, num = 0; i < MAX_NUM_PARPS; i++) if (!x[i]) num++;
  printf("unaccounted for %d\n", num);
  ASSERT(num == 0);
  
}
#endif
