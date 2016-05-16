/*---------------------------------------------------------------------------*
 *  word_lattice.h  *
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

#ifndef WORD_LATTICE_H
#define WORD_LATTICE_H

#include"srec_sizes.h"
#include"srec.h"
#include"search_network.h"

#if defined(__cplusplus)
extern "C"
{
#endif

  void print_word_token(srec* rec, wtokenID wtoken_index, char* msg);
  void print_word_token_backtrace(srec* rec, wtokenID wtoken_index, char* tail);
  void print_word_token_list(srec* rec, wtokenID wtoken_index, char* msg);
  int sprint_bword_token_backtrace(char *buf, int len, srec* rec, wtokenID wtoken_index);
  
#define SCOREMODE_EXCLUDE_SILENCE 0
#define SCOREMODE_INCLUDE_SILENCE 1
#define ERROR_TRANSCRIPTION_TOO_LONG -1
#define ERROR_RESULT_IS_LOOPY        -2
  
  int srec_print_results(multi_srec *rec, int max_choices);
  int srec_get_top_choice_score(multi_srec* rec, bigcostdata *cost, int do_incsil);
  int srec_get_top_choice_transcription(multi_srec* rec, char *transcription, int len, int whether_strip_slot_markers) ;
  ESR_ReturnCode srec_get_top_choice_wordIDs(multi_srec* recm, wordID* wordIDs, size_t* len);
  int sprint_word_token_backtrace(char *transcription, int len, srec* rec, wtokenID wtoken_index);
  void sort_word_lattice_at_frame(srec* rec, frameID frame);
  int reprune_word_tokens(srec* rec, costdata current_best_cost);
  srec_word_lattice *allocate_word_lattice(frameID max_frames);
  void destroy_word_lattice(srec_word_lattice* wl);
  void initialize_word_lattice(srec_word_lattice* wl);
  void lattice_add_word_tokens(srec_word_lattice *wl, frameID frame,
                               wtokenID word_token_list_head);
  costdata lattice_best_cost_to_frame(srec_word_lattice *wl, word_token* word_token_array, frameID ifr);
  
#if defined(__cplusplus)
}
#endif

#endif
