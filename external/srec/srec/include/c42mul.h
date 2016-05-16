/*---------------------------------------------------------------------------*
 *  c42mul.h  *
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



#ifndef _h_c42mul_
#define _h_c42mul_

#ifdef SET_RCSID
static const char c42mul_h[] = "$Id: c42mul.h,v 1.8.6.8 2008/03/07 19:46:58 dahan Exp $";
#endif


#include "prelib.h"
#include "utteranc.h"
#include "duk_args.h"
#include "setting.h"
#include "srec_sizes.h"
#include "search_network.h"
#include "srec.h"
#include "swimodel.h"

#ifdef __cplusplus
extern "C"
{
#endif

  /*      Exception based error reporting */
#define SETUP_RETURN(REC)   ((REC)->err_code= setjmp ((REC)->except_buf))
#define RETURN_ERROR(CODE)  (rec->except_enabled ? longjmp(rec->except_buf, (CODE)) : SERVICE_ERROR(CODE))
#define SET_ERROR(CODE)     (rec->err_code= CODE)

  /*  Main calls to multi-line recognition
  */
  int multi_srec_viterbi(multi_srec *rec,
                         srec_eos_detector_parms* eosd,
                         pattern_info *pattern,
                         utterance_info* utt);

  void multi_srec_get_result(multi_srec *rec);
  int activate_grammar_for_recognition(multi_srec* rec1, srec_context* context, const char* rule);
  int clear_grammars_for_recognition(multi_srec* rec1);

  void partial_traceback(multi_srec *rec, pattern_info *pattern,
                         utterance_info *utt);
  void begin_recognition(multi_srec *rec, int begin_syn_node);
  void end_recognition(multi_srec *rec);
  int  add_acoustic_model_for_recognition(multi_srec* rec, const SWIModel* swimodel);
  int  clear_acoustic_models_for_recognition(multi_srec* rec);

  void free_recognition(multi_srec *rec);
  int allocate_recognition(multi_srec *rec,
                           int viterbi_prune_thresh,
                           /* score-based pruning threshold - only keep paths within this delta of best cost*/
                           int max_hmm_tokens,
                           int max_fsmnode_tokens,
                           int max_word_tokens,
                           int max_altword_tokens,
                           int num_wordends_per_frame,
                           int max_fsm_nodes,
                           int max_fsm_arcs,
                           int max_frames,
                           int max_model_states,
                           int max_searches);

  int compare_model_indices(multi_srec *rec1, srec *rec2);

  void reset_utt_ended_in_result(multi_srec *rec, int sil_dur);
  int  has_utt_ended_in_result(multi_srec *rec);


#if DO_STRESS_CALC
  void get_stress_in_segment(stress_info *stress, fepFramePkt *frmPkt,
                             int start, int end, int relative_to_pullp);
#endif


  /*  Utterance stuff moved in here
  */
  int get_data_frame(preprocessed *predat, utterance_info *utt);
  int get_utterance_frame(preprocessed *predat, utterance_info *utt);
  int advance_utterance_frame(utterance_info *utt);
  int retreat_utterance_frame(utterance_info *utt);
  int copy_pattern_frame(utterance_info *oututt, preprocessed *prep);
  void prepare_data_frame(preprocessed *predat);
  void convert_adjustment_to_imelda(norm_info *norm, preprocessed *prep);

  int inherit_recognition_statistics(utterance_info *utt, multi_srec *recog,
                                     int norm_dim);

#ifdef __cplusplus
}
#endif

#endif
