/*---------------------------------------------------------------------------*
 *  srec.h  *
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

/* this file contains defines needed by the srec search component*/

#ifndef _h_srec_
#define _h_srec_

#include "swimodel.h"
#include "hmm_desc.h"
#include "utteranc.h"
#include "hmmlib.h"
#include "srec_sizes.h"
#include "search_network.h"
#include "srec_context.h"
#include "srec_eosd.h"
#include "astar.h"

#define MAX_HMM 3            /*maximum HMM states in an allophone*/
#define DO_ALLOW_MULTIPLE_MODELS 1

/*in order to keep data sizes as small as possible, most of the the structure
  below use indices into one fsmarc_token array and one word_token array.  This
  makes the code a bit confusing (compared to just keeping pointers to these
  structure around), uses a bit more CPU, but saves memory and gives us more
  flexibility in the sizes of these data types*/

/**
 * @todo document
 */
typedef struct altword_token_t
{
  costdata costdelta;        /* cost relative to path being propagated */
  wordID word;               /* alternative word, diff from path b.p. */
  wtokenID word_backtrace;   /* alternative backtrace, diff from path b.p.*/
  struct altword_token_t* next_token; /* todo: change this to indices */
  asr_int16_t refcount;
  costdata costbasis;        /* cost of best fsmarc_token host */
}
altword_token;
#define AWTNULL 0
/* fsmarc_tokens and fsmnode_tokens point to a batch of altword_tokens
   to save memory, many fsmarc_tokens can point to the same altword_token
   and these are propagated by reference */

/**
 * @todo document
 */
typedef struct fsmarc_token_t
{
  frameID num_hmm_states;           /* number of hmm states */
  costdata cost[MAX_HMM];           /* cost so far*/
  wtokenID word_backtrace[MAX_HMM]; /* index into word tokens*/
  wordID word[MAX_HMM];             /* when the path encounters an output
             symbol, store it here*/
  frameID duration[MAX_HMM];        /* frames observed for this hmm state, todo: pack into char! */
  arcID FSMarc_index;               /* index into the FSM arc array */

  stokenID next_token_index;        /* for maintaining linked lists of these
             tokens, both in search and in freelist */
  altword_token* aword_backtrace[MAX_HMM];
}
fsmarc_token;
/* 30 bytes */


/**
 * These are used while maximizing into FSM nodes.
 */
typedef struct fsmnode_token_t
{
  costdata cost;
  wtokenID word_backtrace;  /* index into word tokens*/
  wordID word;              /* when the path encounters an output*/
  nodeID FSMnode_index;
  ftokenID next_token_index;
  altword_token* aword_backtrace;
  frameID silence_duration;
}
fsmnode_token;
/* 10 bytes */

/**
 * @todo document
 */
typedef struct word_token_t
{
  wordID word;                /* the word just observed */
  frameID end_time;           /* end time of the word just observed, includes trailing silence */
  nodeID end_node;            /* for backtrace with word graph */
  wtokenID backtrace;         /* for backtrace */
  costdata cost;              /* cost for path up to this point*/
  wtokenID next_token_index;  /* for maintaining linked lists of these tokens
       (both in the search and in the freelist) */
  frameID _word_end_time;     /* end time of the word just observed, excl trailing silence */
  /* since frameID is 16 bit, and 15bits is plenty
     (ie 32767 frames * 20ms/frame = 655 sec), we use the high-bit to store
	 whether this word_token represents a homonym, this is used in confidence
	 score fixing! */
#define WORD_TOKEN_GET_HOMONYM(wT)     (wT->_word_end_time & 0x8000)  // 10000000
#define WORD_TOKEN_SET_HOMONYM(wT,hM)  (wT->_word_end_time = (wT->_word_end_time&0x7fff)|(hM?0x8000:0))
#define WORD_TOKEN_GET_WD_ETIME(wT)    (wT->_word_end_time & 0x7fff) // 01111111
#define WORD_TOKEN_SET_WD_ETIME(wT,eT) (wT->_word_end_time = (wT->_word_end_time&0x8000)|(eT))
}
word_token;
/* 12 bytes */

/**
 * Contains what we need for later backtrace, nbest, etc.
 */
typedef struct
{
  /* there are various arrays below which frame number long - this is the number allocated */
  frameID max_frames;

  /* for each frame, head of a linked list of word tokens for that frame */
  wtokenID *words_for_frame;
  asr_int16_t *whether_sorted;

}
srec_word_lattice;

/*This is just implemented as a list so far - use Johan's fancy implementation later*/

/**
 * @todo document
 */
typedef struct priority_q_t
{
  wtokenID word_token_list;  /* index of head token in queue - keep worst at end
      (so we can pop one off) */
  costdata max_cost_in_q;
  miscdata num_in_q;
  miscdata max_in_q;
}
priority_q;

/*------------------------------------------------------------------*
 *                                                                  *
 *------------------------------------------------------------------*/

/* notes ... what needs to be acoustic model specific

   (p)ool it
   (1) single  .r but reset
   (x) specific

   1 context
   1 word_priority_q
   x word_lattice
   1 prune_delta
   1 current_search_frame

   1.r best_token_for_arc[]  max_fsm_arcs
   1.r best_token_for_node[]   max_fsm_nodes
   1 cost_offset_for_frame MAX_FRAMES
   1 accumulated_cost_offset_for_frame MAX_FRAMES

   x active_fsmarc_tokens
   num_new_states   ... num in active_fsmarc_tokens
   max_new_states   ... same as fsmarc_token_array_size

   x active_fsm_node_tokens

   ? current_model_scores num_model_slots_allocated

   p fsmarc_token_array _size _freelist
   p fsmnode_token_array  _size _freelist
   x word_token_array _size _freelist
   x word_token_array_flags

   ... not used! best_fsmarc_token
   srec_ended
   astar_stack
*/

struct srec_t
{  /*contains everything needed to run the search*/
  asr_int16_t id;                   /*contains an id for this recognizer*/
  srec_context *context;      /*contains the recognition context (fst, info about models, etc)*/
  priority_q *word_priority_q; /*used to keep track of new word in frame*/
  srec_word_lattice *word_lattice;  /*used to keep track of word lattice in utterance*/

  costdata prune_delta;        /* controls the amount of score-based pruning - should this go in the context instead?*/
  costdata current_prune_delta; /* when the above changes in mid-frame */
  costdata current_best_cost;   /* 0 if single recog */

  frameID current_search_frame;
  stokenID *best_token_for_arc;  /* non-owning ptr, see multi_srec below */

  stokenID active_fsmarc_tokens; /*head of list of state tokens for the next frame.  Used during
        the search to keep track of new states for new frame.  This
        is to allow us to efficently do things like prune, free state arrays, etc*/


  nodeID num_new_states;
  nodeID max_new_states;  /*the num allocated in the new_states array - if the search is exceeding this,
         we need to tighten the pruning*/

  ftokenID *best_token_for_node;   /* non-owning ptr, see multi_srec below */

  ftokenID active_fsmnode_tokens;  /* linked list of all fsmnode token (same as ones in
           best_state_for_node, just kept as a list)*/

  costdata *current_model_scores;  /* temporary array used by the search to contain model scores -
           size is max number of models*/
  modelID num_model_slots_allocated;  /*num allocated in above array - search will only
       work with models with less than this number of models*/

  /*the following arrays handle all the state and word tokens.  All of them
    are allocated to a fixed size at startup time, and the search uses elements
    from the first array in the search.  The pruning of the search is used to
    make sure that the allocated number is not exceeded*/


  fsmarc_token *fsmarc_token_array;  /*used for storage of all state tokens
           - allocated once at startup time and kept
           around.  It's fixed size and the search
           pruning must ensure that it is never
           exceeded*/
  stokenID fsmarc_token_array_size; /*total number of tokens allocated in this array*/
  stokenID fsmarc_token_freelist;   /*index to head of state token freelist*/

  fsmnode_token *fsmnode_token_array;  /*used for storage of all fsmnode tokens
           - allocated once at startup time and kept
           around.  It's fixed size and the search
           pruning must ensure that it is never
           exceeded*/
  ftokenID fsmnode_token_array_size; /*total number of tokens allocated in this array*/
  ftokenID fsmnode_token_freelist;   /*index to head of fsmnode token freelist*/

  word_token *word_token_array;    /* used for storage of all word tokens -
            allocated once at startup time and kept
            around.  It's fixed size and the search
            pruning must ensure that it is never
            exceeded*/
  asr_int16_t* word_token_array_flags;   /* bitarray used for flagging */
  wtokenID word_token_array_size;  /* total number of tokens allocated in
            this array*/
  wtokenID word_token_freelist;    /* index to head of word token freelist*/

  altword_token* altword_token_array; /* used to store alternative words before a wb */
  wtokenID altword_token_array_size;
  altword_token* altword_token_freelist;
  wtokenID altword_token_freelist_len;

  frameID max_frames;
  costdata* best_model_cost_for_frame;
  costdata* cost_offset_for_frame;        /* see multi_srec, below */
  bigcostdata* accumulated_cost_offset;   /* see multi_srec, below */

  stokenID best_fsmarc_token;      /* ?? index of best scoring state token
           this is used to lookup wtokens on the
           top choice path, to make sure they're not
           pruned via reprune_word_tokens() */
  costdata current_best_ftoken_cost[NODE_INFO_NUMS];
  ftokenID current_best_ftoken_index[NODE_INFO_NUMS];

  /*the following elements are to keep track of how big various arrays are*/
  nodeID max_fsm_nodes;           /* see multi_srec below */
  arcID max_fsm_arcs;             /* see multi_srec below */
  asr_int16_t srec_ended;
  AstarStack *astar_stack;        /* for backwards word search */
  const featdata* avg_state_durations;  /* average state durations (from AMs) */

  srec_eos_detector_state eosd_state;
};

#define MAX_RECOGNIZERS 2          /* generally, 1x for each acoustic model */
#define MAX_ACOUSTIC_MODELS 2

/**
 * @todo document
 */
typedef struct
{
  asr_int32_t num_allocated_recs;
  asr_int32_t num_activated_recs;
  srec* rec;                       /* size num_allocated_recs, one for
            each gender */

  frameID max_frames;
  costdata* cost_offset_for_frame; /* size max_frames, keeps track of
            current_best_costs bookkeeping from
            reset_current_best_costs_to_zero() */
  bigcostdata *accumulated_cost_offset; /* same as above but cumulative */


  ftokenID *best_token_for_node;  /* array (size max_fsm_nodes) best path into
           fsmnode - kept as an fsmnode_token */
  nodeID max_fsm_nodes;
  stokenID *best_token_for_arc;   /* array (size max_fsm_arcs) best path into
           fsmarc - kept as a fsmarc_token */
  arcID max_fsm_arcs;

  /* non owning pointer to compact acoustic models */
  asr_int32_t num_swimodels;
  const SWIModel    *swimodel[MAX_ACOUSTIC_MODELS];
  EOSrc eos_status;
}
multi_srec;

#ifdef __cplusplus
extern "C"
{
#endif
  priority_q* allocate_priority_q(int max_n);
  void free_priority_q(priority_q* pq);
  void clear_priority_q(priority_q *pq);
  wtokenID get_word_token_list(priority_q *pq, word_token *word_token_array);
  wtokenID add_word_token_to_priority_q(priority_q *pq, wtokenID token_index_to_add, word_token *word_token_array);
  void remove_non_end_word_from_q(srec *rec, priority_q *pq, word_token *word_token_array, nodeID end_node);
  costdata get_priority_q_threshold(priority_q *pq, word_token *word_token_array);

  void free_word_token(srec *rec, wtokenID old_token_index);
  int srec_begin(srec* rec, int begin_syn_node);
  void srec_no_more_frames(srec* rec);
  bigcostdata accumulated_cost_offset(costdata *cost_offsets, frameID frame);
  void multi_srec_get_speech_bounds(multi_srec* rec, frameID* start_frame, frameID* end_frame);
  int multi_srec_get_eos_status(multi_srec* rec);
#ifdef __cplusplus
}
#endif

/**
 * For visualization in the debugger
 */
typedef struct
{
  asr_uint16_t data[50];
}
us50;

/**
 * @todo document
 */
typedef struct
{
  asr_uint16_t data[250];
}
us250;

/**
 * @todo document
 */
typedef struct
{
  asr_uint16_t data[1000];
}
us1000;

#endif
