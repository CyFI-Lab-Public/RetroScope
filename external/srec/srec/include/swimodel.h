/*---------------------------------------------------------------------------*
 *  swimodel.h  *
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

#ifndef __SWIMODEL_H__
#define __SWIMODEL_H__

#include "hmm_type.h"
#include "pre_desc.h"
#include "srec_sizes.h"
#include "PortExport.h"


/**
 * @todo document
 */
typedef struct
{
  short num_pdfs;           /* number of pdfs for this state */
  /* featdata avg_durn;           average state duration, belongs here but stored
     elsewhere to avoid paging back to memory of acoustic models, c54!! */
  const featdata *means;            /* pointer to block of means for the set
       of pdfs (points into the allmeans array)*/
  const wtdata *weights;            /*pointer to weights*/
}
SWIhmmState;

/**
 * Model loading storage structures.
 */
typedef struct
{
  void* mmap_zip_data;          /* mmap file in one chunk */
  size_t mmap_zip_size;         /* size of above */
  modelID num_hmmstates;        /* number of hmm states ~ 800 */
  short num_dims;               /* feature vector dimensions ~ 36 or 28 */
  modelID num_pdfs;             /* total number of pdfs ~ 4800 */
  const SWIhmmState *hmmstates;       /* size num_hmmstates ~ 800*/
  const featdata *allmeans;        /* size num_dims*num_pdfs ~ 36*4800 */
  const wtdata *allweights;        /* size num_pdfs ~ 4800 */
  const featdata *avg_state_durations; /* average duration of this acoustic model state */
}
SWIModel;

#ifdef __cplusplus
extern "C"
{
#endif

/* SpeechWorks compact acoustic models */
const SWIModel *load_swimodel(const char *filename);
void free_swimodel(const SWIModel* swimodel);
scodata mixture_diagonal_gaussian_swimodel(const preprocessed *prep, const SWIhmmState *spd, short num_dims);

extern const char loop_cost_table [128][6];
extern const char trans_cost_table [128][6];

#ifdef __cplusplus
}
#endif


/* the looping cost for the new duration model. In this new duration model,
   the looping probability is multiplied by a sigmoid function having the
   following form: sigm(-scale(duration_so_far-offset))  so that the looping
   cost increases as the duration_so_far increases and encouraging to
   stay within a given state for a duration approx. equal to the average state
   duration. The looping cost values are implemented as a lookup table.*/

static PINLINE costdata duration_penalty_loop(frameID average_duration, frameID duration_so_far)
{
 if (average_duration > 127)  average_duration =  127;
 if(duration_so_far> 6) duration_so_far = 6;
 return (costdata)loop_cost_table[average_duration][duration_so_far-1];
}

/* the transition cost for the new duration model. In this new duration model,
   the transition probability is multiplied by a sigmoid function having the
   following form: sigm(scale(duration_so_far-offset)) so that the  transition
   cost decreases as the duration_so_far increases thus encouraging to leave
   a given state when the duration exceeds the average state duration. The transition
   cost values are implemented as a lookup table*/

static PINLINE costdata duration_penalty_depart(frameID average_duration, frameID duration_so_far)
{
  if (average_duration > 127)     average_duration = 127;
  if(duration_so_far> 6)     duration_so_far = 6;
  return (costdata) trans_cost_table[average_duration][duration_so_far-1];
}

#endif /* __SWIMODEL_H__ */

