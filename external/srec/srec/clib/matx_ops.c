/*---------------------------------------------------------------------------*
 *  matx_ops.c  *
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


#include <stdlib.h>
#ifndef _RTT
#include <stdio.h>
#endif
#include <math.h>
#include <string.h>
#include <assert.h>

#include "hmmlib.h"
#include "prelib.h"
#include "duk_io.h"
#include "duk_err.h"
#include "pendian.h"
#include "portable.h"

static const char matx_ops[] = "$Id: matx_ops.c,v 1.5.6.6 2007/10/15 18:06:24 dahan Exp $";

covdata **create_matrix(int dimen)
{
  int     ii;
  covdata **matrix;
  
  matrix = (covdata **) CALLOC(dimen, sizeof(covdata *),
                                     "clib.cov_matrix");
  for (ii = 0; ii < dimen; ii++)
    matrix[ii] = (covdata *) CALLOC(dimen, sizeof(covdata),
                                          "clib.cov_matrix[]");
  return (matrix);
}

void delete_matrix(covdata **matrix, int dimen)
{
  int ii;
  
  ASSERT(matrix);
  for (ii = 0; ii < dimen; ii++)
    FREE((char *)matrix[ii]);
  FREE((char *)matrix);
  return;
}

void diagonal_elements(covdata *vector, covdata **matrix, int dim)
{
  int ii;
  
  ASSERT(vector);
  ASSERT(matrix);
  for (ii = 0; ii < dim; ii++)
    vector[ii] = (float) matrix[ii][ii];
  return;
}

int scale_matrix_for_fixedpoint(imeldata **fixmat, covdata **matrix,
                                int dimen)
{
  int ii, jj, shift;
  long scale_coef;
  double sum_coef, xfp;
  double max_sum_coef = 0.0;
  
  ASSERT(matrix);
  ASSERT(fixmat);
  ASSERT(dimen > 0);
  max_sum_coef = 0;
  for (ii = 0; ii < dimen; ii++)
  {
    sum_coef = 0;
    for (jj = 0; jj < dimen; jj++)
      sum_coef += fabs(matrix[ii][jj]);
    if (sum_coef > max_sum_coef)
      max_sum_coef = sum_coef;
  }
  
  scale_coef = (long)((double)FIXED_MAX / max_sum_coef);
  if (scale_coef < 1)
    SERVICE_ERROR(BAD_IMELDA); /* TODO: find a suitable code */
    
  shift = 0;
  while (scale_coef > 1)
  {
    shift++;
    scale_coef >>= 1;
  }
  
  scale_coef = (1 << shift);
  
  /* read in again and scale up using prep->imel_shift
  */
  for (ii = 0; ii < dimen; ii++)
    for (jj = 0; jj < dimen; jj++)
    {
      xfp = ((double)scale_coef) * matrix[ii][jj];
      if (xfp > 0.0)
        xfp += 0.5;
      else if (xfp < 0.0)
        xfp -= 0.5;
      ASSERT(xfp < FIXED_MAX);
      ASSERT(xfp > -FIXED_MAX);
      fixmat[ii][jj] = (imeldata) xfp;
    }
  return (shift);
}

imeldata **create_fixed_matrix(int dimen)
{
  int     ii;
  imeldata **matrix;
  
  matrix = (imeldata **) CALLOC(dimen, sizeof(imeldata *),
                                      "clib.fixed_matrix");
  for (ii = 0; ii < dimen; ii++)
    matrix[ii] = (imeldata *) CALLOC(dimen, sizeof(imeldata),
                                           "clib.fixed_matrix[]");
  return (matrix);
}

void delete_fixed_matrix(imeldata **matrix, int dimen)
{
  int ii;
  
  ASSERT(matrix);
  
  for (ii = 0; ii < dimen; ii++)
    FREE((char *)matrix[ii]);
  FREE((char *)matrix);
  return;
}
