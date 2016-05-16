/*---------------------------------------------------------------------------*
 *  astar.h  *
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

#ifndef __ASTAR_H__
#define __ASTAR_H__

#include "search_network.h"
#include "srec_sizes.h"
#include "sizes.h"

/*********************************************************************
 *                                                                   *
 * Word Graph for Astar                                              *
 *                                                                   *
 *********************************************************************/

/* #define DEBUG_ASTAR 1 */

/* an arc_token is used for the word graph, this implementation
   removes the need for nodes, and allows arc_tokens to be
   freed and re-used easily for dynamic grammar creation.
   Why do away with nodes?  Nodes need a list of outgoing arcs,
   or arc pointers.  Rather than store this arc list as an array,
   we can store it as a linked list, for easy addition/removal.
   Nodes are now just a pointer to the first arc in a linked list.
   But further, why not just reference the "first_arc" instead of
   a node?  That's what we're doing here.  The "end_node" is
   really an arc, whose arc->first_next_arc is NULL.

   This experimental implementation is working for the moment!
*/

/* VxWorks 5.4 does not support unnamed struct/union yet */
/* here we use indices to link one arc token to the next */
#define ARC_TOKEN_LNK(bAsE,iDx) ((arcID)iDx)
#define ARC_TOKEN_PTR(bAsE,atp) (atp==MAXarcID?NULL:bAsE+atp)
#define ARC_TOKEN_PTR2LNK(bAsE,atp) (atp==NULL?MAXarcID:(arcID)(atp-bAsE))
#define ARC_TOKEN_IDX(bAsE,atp) (atp)
#define ARC_TOKEN_NULL MAXarcID
typedef arcID      arc_token_lnk;
typedef struct arc_token_t
{
#ifdef DEBUG_ASTAR
  char* label, debug[64];
#endif
  wordID  ilabel;                  /* input label */
  labelID olabel;                  /* output label */
  arc_token_lnk first_next_arc;
  arc_token_lnk next_token_index;
}
arc_token;

/**
 * @todo document
 */
typedef struct partial_path_t
{
  wtokenID token_index;
  wordID   word;           /* quick access to word (wta[token_index].word) */
  bigcostdata costsofar;   /* quick access to total score, frwd+bkwd */
  struct partial_path_t* next;
  struct partial_path_t* first_prev_arc;
  struct partial_path_t* linkl_prev_arc;
  arc_token* arc_for_wtoken;
  short refcount;
  struct partial_path_t* hashlink;
}
partial_path;
#define PARP_TERMINAL         ((partial_path*)-1)

typedef struct
{

  partial_path* free_parp_list;
  partial_path* partial_path_array;
  int partial_path_array_size;
  
  /* todo: replace these pointers with partial_path_token type things */
  int max_active_paths;
  int num_active_paths;
  partial_path** active_paths;    /* partial paths, sorted by score */
  
  int max_complete_paths;
  int num_complete_paths;
  partial_path** complete_paths;
  int* complete_path_confidences;
  partial_path* root_path;        /* root is the rightmost partial path
           to be used for as root of a tree
           for checking paths already visited */
  costdata prune_delta;
  void* pphash;
}
AstarStack;

typedef struct srec_t srec;
typedef srec* psrec;

int astar_stack_do_backwards_search(psrec rec, int request_nbest_len);
int astar_stack_prepare(AstarStack* stack, int request_nbest_len, psrec rec);
int astar_stack_prepare_from_active_search(AstarStack* stack, int request_nbest_len, psrec rec);
void astar_stack_clear(AstarStack* stack);
int astar_stack_flag_word_tokens_used(AstarStack* stack, psrec rec);
AstarStack* astar_stack_make(psrec rec, int max_nbest_len);
int astar_stack_destroy(psrec rec);

void free_partial_path(AstarStack* stack, partial_path* parp);
void print_path(partial_path* parp, psrec rec, char* msg);

arc_token* get_arc_for_word(arc_token* atoken, wordID word, void* context_void,
                            wordID terminal_word);
                            
arc_token* get_arc_for_word_without_slot_annotation(arc_token* atoken, const char* word,
    void* context_void, wordID terminal_word);
    
#endif
