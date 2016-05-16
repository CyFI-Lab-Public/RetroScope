/*---------------------------------------------------------------------------*
 *  srec_context.h  *
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

#ifndef _h_srec_context_
#define _h_srec_context_

#include "srec_arb.h"
#include "astar.h"

#include "portable.h"

#include "phashtable.h"

#define CONTEXT_FILE_FORMAT_VERSION1_ID 10001
#define IMAGE_FORMAT_V1   32432
#define IMAGE_FORMAT_V2   32439
#define USE_HMM_BASED_ENROLLMENT 0

/*********************************************************************
 *                                                                   *
 * WordMap                                                           *
 *                                                                   *
 *********************************************************************/

/* todo: for dynamic vocabs, a word should eventually be replaced
   not so much "char* word" but "wordblock* head", Jean to merge
   in that code later. */

typedef struct
{
  wordID num_words;
  wordID num_slots;        
  wordID max_words;
  wordID num_base_words;      /* before any additions */
  /* ptr32* words; c55 ?? */
  char** words;               /* size max_words */
  
  char* chars;                /* FOUR_BYTE_PTR(char*, chars, dummy1); */
  asr_int32_t max_chars;
  char* next_chars;  /* FOUR_BYTE_PTR(char*, next_chars, dummy2); */
  char* next_base_chars;      /* before any additions */
  PHashTable *wordIDForWord;
}
wordmap;

/*********************************************************************
 *                                                                   *
 * FST                                                               *
 *                                                                   *
 *********************************************************************/

typedef struct srec_fsm_entry_point_t
{
  nodeID node_index;
  nodeID* node_for_lpcp;        /* size num_lpcps */
}
srec_fsm_entry_point;

/**
 * srec_fsm_exit_point_t holds information about a particular slot within
 * the fst, so that we don't need to recalculate it each time.
 */
typedef struct srec_fsm_exit_point_t
{
  nodeID from_node_index;  /* from node, there can be multiple arcs leaving here */
  arcID arc_index;         /* arc on which the "class" rests */
  nodeID wbto_node_index;  /* node index after the .wb ilabel */
}
srec_fsm_exit_point;
#define MAX_NUM_SLOTS 12    /* SLOTS */
#define IMPORTED_RULES_DELIM '.' /* SLOT MARKER */

typedef char FSMnode_info; 
#define NODE_INFO_UNKNOWN 0
#define NODE_INFO_ENDNODE 1
#define NODE_INFO_OPTENDN 2
#define NODE_INFO_REGULAR 3
#define NODE_INFO_NUMS    4

typedef struct srec_context
{
  asr_uint32_t modelid;  /* modelid at compilation time, or 0 for unknown */
  int grmtyp;            /* GrammarType */
  
  FSMarc* FSMarc_list;   /* allocation base */
  arcID num_arcs;        /* number of arcs actually used */
  arcID FSMarc_list_len; /* number of arcs allocated     */
  arcID num_base_arcs;   /* number of arcs before additions */
  arcID FSMarc_freelist; /* head of the free list */
  
  FSMnode* FSMnode_list;
  nodeID num_nodes;
  nodeID FSMnode_list_len;
  nodeID num_base_nodes;
  nodeID FSMnode_freelist;
  FSMnode_info* FSMnode_info_list; /* todo: change this to an ary of 2bit els*/
  
  costdata wrapup_cost;        /* cost of going from optend nodes to endnode */
  costdata wtw_average;        /* cost of going from optend nodes to endnode */
  
  nodeID start_node;
  nodeID end_node;
  
  asr_int16_t num_fsm_exit_points;  /* one per rule import */
  srec_fsm_exit_point fsm_exit_points[MAX_NUM_SLOTS];
  /* caching for add word, because FST_AddWordToSlot() is often sequentially 
	 on the same slot */
  wordID addWordCaching_lastslot_num;
  LCHAR* addWordCaching_lastslot_name;
  ESR_BOOL addWordCaching_lastslot_needs_post_silence;
  wordID addWordCaching_lastslot_ifsm_exit_point;
  
  wordID beg_silence_word;
  wordID end_silence_word;
  wordID hack_silence_word;
  
  /* aux */
  wordmap *ilabels;           /* input arc labels */
  wordmap *olabels;           /* word labels */
  srec_arbdata *allotree;          /* for addword, knows hmm to state conversion */
  
  /* word graph, for a-star */
  arc_token* arc_token_list;
  arcID arc_token_list_len;
  arc_token* arc_token_freelist;
  arc_token* arc_token_insert_start;
  
  /* search capabilities, return error if adding words beyond this! */
  nodeID max_searchable_nodes;
  arcID max_searchable_arcs;
  
  /* these are pointers to data owned by others, made part of this
     structure for completeness of information needed by an active
     search, for banked memory models, we may want these to be a "copy" */
  asr_int16_t hmm_ilabel_offset;        /* offset for ilabels to hmm */
  HMMInfo* hmm_info_for_ilabel;   /* ilabel to state conversion */
  featdata* _unused_avg_state_durations;  /* average durations */
  
  /* says whether a grammar has been prepared FST_Prepare()
     a Grammar must be prepared before it is used in a recognition */
  asr_int16_t whether_prepared;
}
srec_context;


/*********************************************************************
 *                                                                   *
 * Functions                                                         *
 *                                                                   *
 *********************************************************************/

#define FST_SUCCESS_ON_OLD_WORD 2
#define FST_CONTINUE   1
#define FST_SUCCESS 0
#define FST_FAILED_ON_INVALID_ARGS -2
#define FST_FAILED_ON_MEMORY -3
#define FST_FAILED_ON_HOMONYM -4
#define FST_FAILED_ON_HOMOGRAPH -5
#define FST_FAILED_INTERNAL -6
/* #define FST_NEWWORD    2 // implies success */

#define NUM_ITEMLIST_HDRWDS    4
enum GrammarType { GrammarTypeUnknown = 0, GrammarTypeBNF = 1, GrammarTypeItemList = 2 };

#ifdef __cplusplus
extern "C"
{
#endif

  /* FST type functions */
  int FST_AttachArbdata(srec_context* fst, srec_arbdata* allophone_tree);
  int FST_DumpGraph(srec_context* fst, PFile* fp);
  int FST_DumpWordMap(PFile* fp, wordmap* wmap);
  int FST_DumpReverseWordGraph(srec_context* context, PFile* fp);
  
  int FST_AddWordToGrammar(srec_context* fst,
                           const char* slot,
                           const char* word,
                           const char* pron, const int cost);
  int FST_ResetGrammar(srec_context* fst);
  
  int FST_PrepareContext(srec_context* fst);
  int FST_IsVoiceEnrollment(srec_context* context);
  int FST_LoadContext(const char* synbase, srec_context** pcontext, int num_words_to_add);
  void FST_UnloadContext(srec_context* context);
  
  int FST_LoadWordMap(wordmap** pwmap, int num_words_to_add, PFile* fp);
  int FST_UnloadWordMap(wordmap** pwmap);
  int FST_LoadGraph(srec_context* pfst, wordmap* imap, wordmap* omap,
                    int num_words_to_add, PFile* fp);
  int FST_UnloadGraph(srec_context* pfst);
  
#if defined(DO_ALLOW_V1_G2G_FILES)
  int FST_DumpContextAsImageV1(srec_context* context, PFile* fp);
#endif
  int FST_DumpContextAsImageV2(srec_context* context, PFile* fp);
  int FST_LoadContextFromImage(srec_context** pcontext, PFile* fp);
  
  int FST_CheckPath(srec_context* context, const char* transcription,
                    char* literal, size_t max_literal_len);
#define FST_GetNodeInfo(cn,nd) (cn->FSMnode_info_list[nd])
                    
  /* wordmap functions */
  int wordmap_whether_in_rule(wordmap* wmap, wordID word, wordID rule);
  wordID wordmap_find_index(wordmap* wmap, const char* word);
  wordID wordmap_find_index_in_rule(wordmap* wmap, const char* word, wordID rule);
  wordID wordmap_find_rule_index(wordmap* wmap, const char* rule);
  int wordmap_create(wordmap** pwmap, int num_chars, int num_words, int num_words_to_add);
  int wordmap_destroy(wordmap** pwmap);
  wordID wordmap_add_word(wordmap* wmap, const char* word);
  void wordmap_reset(wordmap* wmap);
  void wordmap_setbase(wordmap* wmap);
  void wordmap_ceiling(wordmap* wmap);
  wordID wordmap_add_word_in_rule(wordmap* wmap, const char* word, wordID rule);
  
  /* utils */
  asr_int32_t atoi_with_check(const char* buf, asr_int32_t mymax);
  arc_token_lnk get_first_arc_leaving_node(arc_token* arc_token_list, arcID num_arcs, nodeID node);
  ESR_ReturnCode deserializeWordMapV2(wordmap **pwordmap, PFile* fp);
  ESR_ReturnCode serializeWordMapV2(wordmap *wordmap, PFile* fp);
  int FST_GetGrammarType(srec_context* context);
  
#ifdef __cplusplus
}
#endif


#endif
