/*---------------------------------------------------------------------------*
 *  srec_results.h  *
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

#ifndef __SREC_RESULTS_H__
#define __SREC_RESULTS_H__

#include"srec.h"

#ifdef __cplusplus
extern "C"
{
#endif

  /* results */
  int srec_has_results(multi_srec* rec);
  int srec_clear_results(multi_srec* rec);
  int srec_get_bestcost_recog_id(multi_srec* rec, int* id);

  /* nbest */
  void* srec_nbest_prepare_list(multi_srec* rec, int n, asr_int32_t* bestcost);
  void srec_nbest_destroy_list(void* nbest);
  int srec_nbest_get_num_choices(void* nbest);
  int srec_nbest_put_confidence_value(void* rec_void, int choice, int confidence_value);
  int srec_nbest_get_confidence_value(void* rec_void, int choice);
  int srec_nbest_fix_homonym_confidence_values(void* rec_void);
  int srec_nbest_get_result(void* nbest, int n, char* label, int label_len, asr_int32_t* cost, int whether_strip_slot_markers);
  LCHAR* srec_nbest_get_word(void* nbest, size_t choice);
  ESR_ReturnCode srec_nbest_get_resultWordIDs(void* nbest, size_t inde, wordID* wordIDs, size_t* len, asr_int32_t* cost);
  void srec_result_strip_slot_markers(char* result);
  int srec_nbest_get_choice_info(void* rec_void, int ibest, asr_int32_t* infoval, char* infoname);
  int srec_nbest_remove_result(void* rec_void, int n);
  int srec_nbest_sort(void* rec_void);


#ifdef __cplusplus
}
#endif

#endif
