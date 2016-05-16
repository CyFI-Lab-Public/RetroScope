/*---------------------------------------------------------------------------*
 *  c47mulsp.c  *
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
#include "passert.h"

#include "c42mul.h"
#include "portable.h"
#include "srec_context.h"
#include "srec.h"

int add_acoustic_model_for_recognition(multi_srec* recm, const SWIModel* model)
{
  if (recm->num_swimodels >= MAX_ACOUSTIC_MODELS)
  {
    log_report("Error: recognizer can't hold any more acoustic models\n");
    return 0;
  }
  if (recm->num_activated_recs >= recm->num_allocated_recs)
  {
    log_report("Error: too few recognizers allocated\n");
    return 0;
  }

  if (recm->rec[0].num_model_slots_allocated < model->num_hmmstates)
  {
    PLogError("recognizer max_model_states %d, acoustic model num states %d, set CREC.Recognizer.max_model_states higher\n",
              recm->rec[0].num_model_slots_allocated,
              model->num_hmmstates);
    return 0;
  }

  recm->swimodel[ recm->num_swimodels] = model;
  recm->num_swimodels++;

  recm->num_activated_recs++;
  return 1;
}

int clear_acoustic_models_for_recognition(multi_srec* recm)
{
  recm->num_swimodels = 0;
  recm->num_activated_recs = 0;
  return 0;
}

void begin_recognition(multi_srec *recm, int begin_syn_node)
{
  int i = 0;
#if DO_ALLOW_MULTIPLE_MODELS
  ASSERT(recm->num_activated_recs == recm->num_swimodels);
  for (i = 0; i < recm->num_activated_recs; i++)
#endif
    srec_begin(&recm->rec[i], begin_syn_node);
  for (i = 0;i < recm->max_fsm_nodes;i++)
    recm->best_token_for_node[i] = MAXftokenID;
  recm->eos_status = VALID_SPEECH_CONTINUING;
}

void end_recognition(multi_srec *recm)
/*
**  To free space allocated for recognizer variables
*/
{
  int i = 0;
#if DO_ALLOW_MULTIPLE_MODELS
  for (i = 0; i < recm->num_activated_recs; i++)
#endif
		srec_no_more_frames(&recm->rec[i]);
  /* srec_get_result(rec);  */
}

int activate_grammar_for_recognition(multi_srec* recm, srec_context* grammar, const char* rule)
{
  srec_context* context = grammar;

  context->max_searchable_nodes = recm->max_fsm_nodes;
  context->max_searchable_arcs  = recm->max_fsm_arcs;

  if (context->max_searchable_nodes < context->num_nodes || context->max_searchable_arcs < context->num_arcs)
  {
    PLogError(L("Error: context switch failed due to search limitations [arcs max=%d, actual=%d], [nodes max=%d, actual=%d]\n"),
              context->max_searchable_arcs, context->num_arcs,
              context->max_searchable_nodes, context->num_nodes);
    return 1;
  }
  else
  {
    int i, rc = 0;
    for (i = 0; i < recm->num_allocated_recs; i++)
      recm->rec[i].context = context;
    rc = FST_PrepareContext(context);
    if (rc)
      return rc;
    else
      return 0;
  }
}

int clear_grammars_for_recognition(multi_srec* recm)
{
  int i;
  for (i = 0; i < recm->num_allocated_recs; i++)
  {
    recm->rec[i].context = NULL;
  }
  return 0;
}








