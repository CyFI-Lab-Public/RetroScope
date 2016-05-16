/*---------------------------------------------------------------------------*
 *  srec_arb.h  *
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

#ifndef __SREC_ARB_H__
#define __SREC_ARB_H__

#include"sizes.h"
#include"hmm_desc.h"           /* 'cuz we're still using the old tree nodes */
#include"search_network.h"     /* for EPSILON_OFFSET */

#define PSET_BIT_ARRAY_SIZE 7  /* max phonemes is 7*16 (128) */
typedef asr_uint16_t phonemeID;
#define MAXphonemeID 255
#define SILENCE_CODE '#'
#define OPTSILENCE_CODE '&'
#define SILENCE_CODE_STR "#"
#define OPTSILENCE_CODE_STR "&"
#define HMM_COUNTER_OFFSET EPSILON_OFFSET
#define NUM_SILENCE_HMMS 3
#define WBPHONEME_CODE '_'
#define USE_WWTRIPHONE 0

#define QUESTION_LEFT  1
#define QUESTION_RIGHT 2
#define QUESTION_WBLEFT  3
#define QUESTION_WBRIGHT 4
#define ANSWER_FAIL    0
#define ANSWER_PASS    1

/**
 * @todo document
 */
typedef struct
{
  asr_uint16_t qtype;
  asr_uint16_t membership_bits[PSET_BIT_ARRAY_SIZE];
}
srec_question;
#define BIT_ADDRESS(K,A,B) { A=(K)/16; B=1<<((K)%16); }

#define MAX_PHONEME_NAME_LEN 8/BYTES_PER_ATOM

/**
 * @todo document
 */
typedef struct
{
  char   name[MAX_PHONEME_NAME_LEN];
  asr_uint16_t code;
  tree_node* model_nodes; /* pelid at the bottom, is really a HMM model ID */
  asr_uint16_t num_states;
  tree_node* state_nodes[MAX_PHONE_STATES];
}
phoneme_data;

/**
 * @todo document
 */
typedef struct
{
  char name[MAX_PHONEME_NAME_LEN]; /* 6400 bytes to free up here */
  asr_int16_t num_states;
  asr_int16_t* state_indices; /* only the first HMMInfo owns the pointer data */
}
HMMInfo;

#define NUM_PHONEME_INDICES 256
/**
 * @todo document
 */
typedef struct
{
  char* image;
  asr_uint16_t image_size;
  asr_int16_t num_phonemes;
  phoneme_data* pdata;
  asr_int16_t num_questions;
  srec_question* questions;
  asr_int16_t num_states; /* total number of states, all allophones, phonemes */
  asr_int16_t num_hmms;
  HMMInfo* hmm_infos;
  phonemeID phoneme_index[NUM_PHONEME_INDICES]; /* from short code to phoneme index */
  /* later add a data member called 'hmm_ilabel_offset', such that
     for graphs prepared with OSR/SGC, the ilabels on that graph
     must be offset by this number to get hmms */
  // struct PCPinfo* pcpinfo;
  void* pcpinfo;
}
srec_arbdata;

/*---------------------------------------------------------------------------*
 *                                                                           *
 *                                                                           *
 *                                                                           *
 *---------------------------------------------------------------------------*/

#if defined(__cplusplus) /*&& !defined(_ASCPP)*/
extern "C"
{
#endif

  int get_modelid_for_pic(srec_arbdata* allotree,
                          phonemeID lphon, phonemeID cphon, phonemeID rphon);
                          
  int get_modelids_for_pron(srec_arbdata* allotree, const char* phonemes,
                            int num_phonemes, modelID* acoustic_model_ids);
                            
  int read_arbdata_from_stream(srec_arbdata** pallotree, char* buffer, int buffer_size);
  unsigned int version_arbdata_models(srec_arbdata* a);
  
#if defined(__cplusplus) /*&& !defined(_ASCPP)*/
}
#endif

#endif
