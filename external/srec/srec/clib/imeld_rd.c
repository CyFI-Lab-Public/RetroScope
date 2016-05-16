/*---------------------------------------------------------------------------*
 *  imeld_rd.c  *
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
#include <stdlib.h>
#include <math.h>
#include <string.h>
#ifdef unix
#include <limits.h>
#endif
#include <assert.h>

#include "prelib.h"
#ifndef _RTT
#include "duk_io.h"
#endif

#include "pendian.h"
#include "portable.h"

static const char imeld_rd[] = "$Id: imeld_rd.c,v 1.5.6.7 2007/10/15 18:06:24 dahan Exp $";

/* function prototypes */

imeldata **create_fixed_matrix(int dimen);
covdata **create_matrix(int dimen);
void delete_matrix(covdata **matrix, int dimen);
void delete_fixed_matrix(imeldata **matrix, int dimen);
int scale_matrix_for_fixedpoint(imeldata **fixmat, covdata **matrix,int dimen);
int     invert_matrix(covdata **mat, covdata **inv, int dim);


void create_linear_transform(preprocessed *prep, int matdim,
                             int with_offset)
{
  ASSERT(prep);
  ASSERT(matdim > 0);
  prep->dim = matdim;
  prep->matrix = create_fixed_matrix(matdim);
  if (with_offset)
    prep->offset = (imeldata *) CALLOC(matdim,
                   sizeof(imeldata), "clib.offset");
  prep->imelda = create_matrix(matdim);
  prep->invmat = create_fixed_matrix(matdim);
  prep->inverse = create_matrix(matdim);
  return;
}

void free_linear_transform(preprocessed *prep)
{
  ASSERT(prep);
  ASSERT(prep->matrix);
  delete_fixed_matrix(prep->matrix, prep->dim);
  if (prep->offset)
    FREE(prep->offset);
  prep->matrix = NULL;
  prep->offset = NULL;
  ASSERT(prep->imelda);
  delete_matrix(prep->imelda, prep->dim);
  prep->imelda = NULL;
  ASSERT(prep->invmat);
  ASSERT(prep->inverse);
  delete_fixed_matrix(prep->invmat, prep->dim);
  delete_matrix(prep->inverse, prep->dim);
  prep->invmat = NULL;
  prep->inverse = NULL;
  return;
}

#ifndef _RTT
int init_newton_transform(preprocessed *prep, float reqscale,
                          char *filename, int dimen)
/*
*/
{
  int  ii, jj;
  unsigned short matdim;
  double scale, onerow[MAX_DIMEN];
  PFile* dfpt;
  long foffset;
  double xfp;
  /* Open file
  */
  ASSERT(prep);
  ASSERT(filename);
  dfpt = file_must_open(NULL, filename, ("rb"), ESR_TRUE);
  prep->post_proc |= LIN_TRAN;
  prep->use_dim = dimen;
  pfread(&matdim, sizeof(short), 1, dfpt);
  if (matdim > MAX_DIMEN)
    SERVICE_ERROR(BAD_IMELDA);

  create_linear_transform(prep, matdim, 1);
  pfread(&scale, sizeof(double), 1, dfpt);

  if (reqscale != 0) scale = reqscale;
#if DEBUG
  PLogMessage("L: LDA Suggested scale is %.1f\n", scale);
#endif
  if (!prep->dim) prep->dim = matdim;
  else if (prep->dim != matdim)
  {
    log_report("Data (%d) and LDA (%d) dimensions don't match\n",
               prep->dim, matdim);
    SERVICE_ERROR(BAD_IMELDA);
  }

  /*  Eigenvalues, ignored
  */
  pfread(onerow, sizeof(double), matdim, dfpt);

  /*  Translation Vector
  */
  pfread(onerow, sizeof(double), matdim, dfpt);
  for (ii = 0; ii < matdim; ii++)
  {
    xfp = scale * (onerow[ii] - UTB_MEAN) + UTB_MEAN;
    if (xfp > 0.0)
      xfp += 0.5;
    else if (xfp < 0.0)
      xfp -= 0.5;

    prep->offset[ii] = (imeldata) xfp;
  }

  /*  The imelda matrix
  */
  for (ii = 0; ii < matdim; ii++)
  {
    pfread(onerow, sizeof(double), matdim, dfpt);
    for (jj = 0; jj < matdim; jj++)
      prep->imelda[ii][jj] = (covdata)(scale * onerow[jj]);
  }

  prep->imel_shift = scale_matrix_for_fixedpoint(prep->matrix,
                     prep->imelda, matdim);

  /* The inverse imelda matrix
   */
  foffset = pftell(dfpt);
  pfread(onerow, sizeof(double), matdim, dfpt);

  if (pfeof(dfpt) != 0)
  {
#ifdef SREC_ENGINE_VERBOSE_LOGGING
    PLogMessage("W: Inverting imelda matrix");
#endif
    invert_matrix(prep->imelda, prep->inverse, prep->dim);
  }
  else
  {
    pfseek(dfpt, foffset, SEEK_SET);

    for (ii = 0; ii < matdim; ii++)
    {
      pfread(onerow, sizeof(double), matdim, dfpt);
      for (jj = 0; jj < matdim; jj++)
        prep->inverse[ii][jj] = (covdata)(onerow[jj] / scale);
    }
  }

  prep->inv_shift = scale_matrix_for_fixedpoint(prep->invmat,
                    prep->inverse, matdim);

  pfclose(dfpt);
  return (0);
}
#endif
