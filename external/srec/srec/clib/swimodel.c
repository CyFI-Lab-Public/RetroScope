/*---------------------------------------------------------------------------*
 *  swimodel.c  *
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
#include <stdio.h>
#endif
#include <math.h>
#include <stdlib.h>
#include <assert.h>

#include "prelib.h"
#include "hmmlib.h"
#include "portable.h"
#include "errhndl.h"

#include "log_add.h"
#include "swimodel.h"


#define MTAG NULL


/*--------------------------------------------------------------*
 *                                                              *
 *                                                              *
 *                                                              *
 *--------------------------------------------------------------*/

/* the looping cost lookup table. This table was generated empirically
   by looking at resulting residency distributions, trying to make them
   look roughly like normal distributions centered at the average state
   durations */

const char loop_cost_table [128][6] = {
{0,0,0,0,0,0},
{13,15,16,16,16,16},
{12,13,14,14,14,14},
{11,12,13,13,13,13},
{10,12,12,12,12,12},
{10,11,11,12,12,12},
{10,11,11,11,11,11},
{10,11,11,11,11,11},
{9,11,11,11,11,11},
{9,10,11,11,11,11},
{9,10,10,10,10,10},
{9,10,10,10,10,10},
{9,10,10,10,10,10},
{9,10,10,10,10,10},
{9,10,10,10,10,10},
{9,10,10,10,10,10},
{9,10,10,10,10,10},
{8,10,10,10,10,10},
{8,10,10,10,10,10},
{8,10,10,10,10,10},
{8,10,10,10,10,10},
{8,10,10,10,10,10},
{8,10,10,10,10,10},
{7,10,10,10,10,10},
{7,10,10,10,10,10},
{7,10,10,10,10,10},
{6,10,10,10,10,10},
{6,10,10,10,10,10},
{6,10,10,10,10,10},
{5,10,10,10,10,10},
{5,10,10,10,10,10},
{4,10,10,10,10,10},
{4,9,10,10,10,10},
{3,9,10,10,10,10},
{2,9,10,10,10,10},
{2,9,10,10,10,10},
{2,9,10,10,10,10},
{1,9,10,10,10,10},
{1,9,10,10,10,10},
{1,9,10,10,10,10},
{0,9,10,10,10,10},
{0,9,10,10,10,10},
{0,9,10,10,10,10},
{0,8,10,10,10,10},
{0,8,10,10,10,10},
{0,8,10,10,10,10},
{0,7,10,10,10,10},
{0,7,10,10,10,10},
{0,6,10,10,10,10},
{0,5,10,10,10,10},
{0,5,10,10,10,10},
{0,4,10,10,10,10},
{0,3,10,10,10,10},
{0,2,10,10,10,10},
{0,2,10,10,10,10},
{0,1,10,10,10,10},
{0,1,9,10,10,10},
{0,0,9,10,10,10},
{0,0,9,10,10,10},
{0,0,9,10,10,10},
{0,0,9,10,10,10},
{0,0,9,10,10,10},
{0,0,9,10,10,10},
{0,0,9,10,10,10},
{0,0,9,10,10,10},
{0,0,8,10,10,10},
{0,0,8,10,10,10},
{0,0,7,10,10,10},
{0,0,6,10,10,10},
{0,0,6,10,10,10},
{0,0,5,10,10,10},
{0,0,4,10,10,10},
{0,0,3,10,10,10},
{0,0,2,10,10,10},
{0,0,1,10,10,10},
{0,0,1,10,10,10},
{0,0,0,10,10,10},
{0,0,0,10,10,10},
{0,0,0,9,10,10},
{0,0,0,9,10,10},
{0,0,0,9,10,10},
{0,0,0,9,10,10},
{0,0,0,9,10,10},
{0,0,0,9,10,10},
{0,0,0,9,10,10},
{0,0,0,9,10,10},
{0,0,0,8,10,10},
{0,0,0,8,10,10},
{0,0,0,7,10,10},
{0,0,0,6,10,10},
{0,0,0,5,10,10},
{0,0,0,3,10,10},
{0,0,0,2,10,10},
{0,0,0,1,10,10},
{0,0,0,1,10,10},
{0,0,0,0,10,10},
{0,0,0,0,10,10},
{0,0,0,0,10,10},
{0,0,0,0,10,10},
{0,0,0,0,10,10},
{0,0,0,0,9,10},
{0,0,0,0,9,10},
{0,0,0,0,9,10},
{0,0,0,0,9,10},
{0,0,0,0,9,10},
{0,0,0,0,9,10},
{0,0,0,0,9,10},
{0,0,0,0,8,10},
{0,0,0,0,7,10},
{0,0,0,0,6,10},
{0,0,0,0,5,10},
{0,0,0,0,3,10},
{0,0,0,0,2,10},
{0,0,0,0,1,10},
{0,0,0,0,0,10},
{0,0,0,0,0,10},
{0,0,0,0,0,10},
{0,0,0,0,0,10},
{0,0,0,0,0,10},
{0,0,0,0,0,10},
{0,0,0,0,0,10},
{0,0,0,0,0,9},
{0,0,0,0,0,9},
{0,0,0,0,0,9},
{0,0,0,0,0,9},
{0,0,0,0,0,9},
{0,0,0,0,0,9},
{0,0,0,0,0,8}
};

/* the transition cost lookup table. This table was generated empirically
   by looking at resulting residency distributions, trying to make them
   look roughly like normal distributions centered at the average state
   durations */

const char trans_cost_table [128][6] = {
{0,0,0,0,0,0},
{0,0,0,0,0,0},
{0,0,0,0,0,0},
{0,0,0,0,0,0},
{0,0,0,0,0,0},
{0,0,0,0,0,0},
{0,0,0,0,0,0},
{1,0,0,0,0,0},
{1,0,0,0,0,0},
{1,0,0,0,0,0},
{1,0,0,0,0,0},
{1,0,0,0,0,0},
{1,0,0,0,0,0},
{1,0,0,0,0,0},
{1,0,0,0,0,0},
{1,0,0,0,0,0},
{1,0,0,0,0,0},
{1,0,0,0,0,0},
{1,0,0,0,0,0},
{1,0,0,0,0,0},
{2,0,0,0,0,0},
{2,0,0,0,0,0},
{2,0,0,0,0,0},
{2,0,0,0,0,0},
{2,0,0,0,0,0},
{2,0,0,0,0,0},
{2,0,0,0,0,0},
{3,0,0,0,0,0},
{3,0,0,0,0,0},
{3,0,0,0,0,0},
{3,0,0,0,0,0},
{4,0,0,0,0,0},
{4,0,0,0,0,0},
{4,0,0,0,0,0},
{5,0,0,0,0,0},
{5,0,0,0,0,0},
{6,1,0,0,0,0},
{6,1,0,0,0,0},
{7,1,0,0,0,0},
{7,1,0,0,0,0},
{8,1,0,0,0,0},
{8,1,0,0,0,0},
{9,1,0,0,0,0},
{10,1,0,0,0,0},
{10,1,0,0,0,0},
{11,2,0,0,0,0},
{12,2,0,0,0,0},
{13,2,0,0,0,0},
{13,2,0,0,0,0},
{14,3,0,0,0,0},
{15,3,0,0,0,0},
{15,3,0,0,0,0},
{16,4,0,0,0,0},
{17,4,0,0,0,0},
{17,5,0,0,0,0},
{18,6,0,0,0,0},
{18,6,0,0,0,0},
{19,7,0,0,0,0},
{19,8,0,0,0,0},
{19,9,0,0,0,0},
{20,10,0,0,0,0},
{20,11,0,0,0,0},
{20,12,0,0,0,0},
{20,13,0,0,0,0},
{21,14,1,0,0,0},
{21,15,1,0,0,0},
{21,16,1,0,0,0},
{21,17,1,0,0,0},
{22,18,2,0,0,0},
{22,19,2,0,0,0},
{22,19,2,0,0,0},
{22,20,3,0,0,0},
{22,20,3,0,0,0},
{23,21,4,0,0,0},
{23,21,5,0,0,0},
{23,22,6,0,0,0},
{23,22,7,0,0,0},
{23,23,8,0,0,0},
{23,23,9,0,0,0},
{23,23,10,0,0,0},
{24,23,12,0,0,0},
{24,24,13,0,0,0},
{24,24,14,0,0,0},
{24,24,16,0,0,0},
{24,24,17,0,0,0},
{24,24,18,0,0,0},
{25,24,20,0,0,0},
{25,25,21,1,0,0},
{25,25,22,1,0,0},
{25,25,22,1,0,0},
{25,25,23,2,0,0},
{25,25,24,2,0,0},
{25,25,24,3,0,0},
{25,25,25,3,0,0},
{26,26,25,4,0,0},
{26,26,25,5,0,0},
{26,26,25,6,0,0},
{26,26,26,8,0,0},
{26,26,26,9,0,0},
{26,26,26,11,0,0},
{26,26,26,13,0,0},
{27,27,26,15,0,0},
{27,27,27,17,0,0},
{27,27,27,18,0,0},
{27,27,27,20,0,0},
{27,27,27,22,0,0},
{27,27,27,23,0,0},
{27,27,27,24,0,0},
{27,27,27,25,0,0},
{27,27,27,26,1,0},
{28,28,28,26,1,0},
{28,28,28,27,1,0},
{28,28,28,27,2,0},
{28,28,28,27,3,0},
{28,28,28,28,3,0},
{28,28,28,28,5,0},
{28,28,28,28,6,0},
{28,28,28,28,8,0},
{28,28,28,28,10,0},
{29,29,29,29,12,0},
{29,29,29,29,14,0},
{29,29,29,29,16,0},
{29,29,29,29,19,0},
{29,29,29,29,21,0},
{29,29,29,29,23,0},
{29,29,29,29,24,0},
{29,29,29,29,26,0},
{29,29,29,29,27,0}
};

/*--------------------------------------------------------------*
 *                                                              *
 *                                                              *
 *                                                              *
 *--------------------------------------------------------------*/

static short load_short(PFile* fp)
{
  short v;
  pfread(&v, sizeof(short), 1, fp);
  return v;
}

const SWIModel* load_swimodel(const char *filename)
{
  int i;
  SWIModel *swimodel = NULL;
  const void* file = NULL;

#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("load_swimodel: loaded %s", filename);
#endif
  swimodel = (SWIModel*) CALLOC(1, sizeof(SWIModel), "clib.models.base");

  if (mmap_zip(filename, &swimodel->mmap_zip_data, &swimodel->mmap_zip_size)) {
      PLogError("load_swimodel: mmap_zip failed for %s\n", filename);
      goto CLEANUP;
  }
  file = swimodel->mmap_zip_data;

  swimodel->num_hmmstates = *(const short*)file;
  file += sizeof(short);
  swimodel->num_dims = *(const short*)file;
  file += sizeof(short);
  swimodel->num_pdfs = *(const short*)file;
  file += sizeof(short);

  SWIhmmState* hmmstates = (SWIhmmState*) CALLOC(swimodel->num_hmmstates, sizeof(SWIhmmState), "clib.models.states");
  swimodel->hmmstates = hmmstates;

  const short* num_pdfs_in_model = (const short*)file;
  file += sizeof(short) * swimodel->num_hmmstates;

  swimodel->allmeans = (const featdata*)file;
  file += sizeof(featdata) * swimodel->num_pdfs * swimodel->num_dims;

  swimodel->allweights = (const wtdata*)file;
  file += sizeof(wtdata) * swimodel->num_pdfs;

  swimodel->avg_state_durations = (const featdata*)file;
  file += sizeof(featdata) * swimodel->num_hmmstates;

  if (file > swimodel->mmap_zip_data + swimodel->mmap_zip_size) {
      PLogError("load_swimodel: not enough data in %s", filename);
      goto CLEANUP;
  }

#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("loaded models %s num_hmmstates %d num_dims %d num_pdfs %d weights[0] %d\n",
              filename, swimodel->num_hmmstates, swimodel->num_dims, swimodel->num_pdfs,
              *swimodel->allweights);
#endif

  const featdata* mean_ptr = swimodel->allmeans;
  const wtdata* weight_ptr = swimodel->allweights;

  for (i = 0;i < swimodel->num_hmmstates;i++)
  {
    hmmstates[i].num_pdfs = num_pdfs_in_model[i];
    hmmstates[i].means = mean_ptr;
    hmmstates[i].weights = weight_ptr;
    mean_ptr += swimodel->num_dims * num_pdfs_in_model[i];
    weight_ptr += num_pdfs_in_model[i];
  }

  return swimodel;

CLEANUP:
  free_swimodel(swimodel);
  return NULL;
}

void free_swimodel(const SWIModel* swimodel)
{
  if (!swimodel) return;
  if (swimodel->mmap_zip_data) munmap_zip(swimodel->mmap_zip_data, swimodel->mmap_zip_size);
  FREE((void*)swimodel->hmmstates);
  FREE((void*)swimodel);
}

static PINLINE prdata Gaussian_Grand_Density_Swimodel(const preprocessed *data, const featdata *means)
/*
**  Observation probability function of a Gaussian pdf
**  with diagonal covariance matrix.
*/
{
  prdata pval;
  prdata diff;
  const imeldata *dvec;
  const imeldata *dend;
  int count;

  dvec = data->seq + data->use_from;    /* Move to starting feature element */

  pval = 0;
  dend = dvec + data->use_dim;
  count = 0;
  while (dvec < dend)
  {
    diff = *(means++) - *(dvec++);
    pval -= diff * diff;
  }
  pval = data->mul.multable_factor_gaussian
         * (pval - data->mul.grand_mod_cov_gaussian);
  return (pval);
}

scodata mixture_diagonal_gaussian_swimodel(const preprocessed *prep,
    const SWIhmmState *spd, short num_dims)
/*
**  Observation probability function
*/
{
  int ii;
  prdata pval, gval;

  prdata dval;
  const featdata *meanptr;
  const wtdata *weightptr;

  ASSERT(prep);
  ASSERT(spd);

  pval = -(prdata) MAX_LOG;

  meanptr = spd->means;
  weightptr = spd->weights;

  for (ii = 0; ii < spd->num_pdfs; ii++)
  {
    gval = ((prdata) * (weightptr++) * prep->add.scale
            + Gaussian_Grand_Density_Swimodel(prep, meanptr));

    meanptr += num_dims;

    if (pval > gval)
    {
      dval = pval - gval;
      if (dval < prep->add.add_log_limit)
        pval += log_increment_inline(dval, &prep->add);
    }
    else
    {
      dval = gval - pval;
      if (dval < prep->add.add_log_limit)
        pval = gval + log_increment_inline(dval, &prep->add);
      else
        pval = gval;
    }
  }
  ASSERT(pval > ((0x01 << 31) / (prep->mix_score_scale * prep->add.inv_scale)));
  pval = ((pval * prep->mix_score_scale - 64 * prep->add.scale)
          * prep->add.inv_scale) >> 19;

  return ((scodata)pval);
}
