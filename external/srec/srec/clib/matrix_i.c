/*---------------------------------------------------------------------------*
 *  matrix_i.c  *
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
#include "portable.h"

#define PIVOT 1
#define DEBUG 0

#define TINY  1.0e-20
#define SIGNIFICANT 0 /* 1.0e-20 */

static const char matrix_i[] = "$Id: matrix_i.c,v 1.2.10.3 2007/10/15 18:06:24 dahan Exp $";

void lubksb(double **mat, int dim, int *index, double *b);
int  ludcmp(double **mat, int dim, int *index);

int invert_matrix(covdata **mat, covdata **inv, int dim)
{
  double *col, **input;
  int ii, jj, *index, err_code;
#if DEBUG
  double  sum;
  int     kk;
#endif
  
  ASSERT(mat);
  ASSERT(inv);
  index = (int *) CALLOC(dim, sizeof(int), "clib.index_imatrix");
  col = (double *) CALLOC(dim, sizeof(double), "clib.col");
  input = (double **) CALLOC(dim, sizeof(double *), "clib.input_imatrix");
  for (ii = 0; ii < dim; ii++)
  {
    input[ii] = (double *) CALLOC(dim, sizeof(double), "clib.input_imatrix[]");
    for (jj = 0; jj < dim; jj++)
      input[ii][jj] = mat[ii][jj];
  }
  
  if ((err_code = ludcmp(input, dim, index)) > 0) return(err_code);
  for (jj = 0; jj < dim; jj++)
  {
    for (ii = 0; ii < dim; ii++)
      col[ii] = 0;
    col[jj] = 1;
    lubksb(input, dim, index, col);
    for (ii = 0; ii < dim; ii++)
      inv[ii][jj] = col[ii];
  }
  for (ii = 0; ii < dim; ii++)
    FREE((char *)input[ii]);
  FREE((char *)input);
  FREE((char *)col);
  FREE((char *)index);
  
#if DEBUG
  printf("Testing the inverse:\n");
  for (ii = 0; ii < dim; ii++)
  {
    for (jj = 0; jj < dim; jj++)
    {
      sum = 0;
      for (kk = 0; kk < dim; kk++)
        sum += mat[ii][kk] * inv[kk][jj];
      printf("%.2f ", sum);
    }
    printf("\n");
  }
#endif
  
  return (0);
}

int ludcmp(double **mat, int dim, int *index)
/*
**  This routine is straight out of the numerical recipes in C
*/
{
  int ii, imax = 0, jj, kk;
  double big, dumm, sum, temp;
  double *vv;
  
  vv = (double *) CALLOC(dim + 5, sizeof(double), "clib.ludcmp.vv");
#if PIVOT
  for (ii = 0; ii < dim; ii++)
  {
    big = 0;
    for (jj = 0; jj < dim; jj++)
      if ((temp = (double) fabs(mat[ii][jj])) > big) big = temp;
    if (big <= SIGNIFICANT)
    {
      log_report("Singular matrix in routine ludcmp\n");
      return (SINGULAR_MATRIX);
    }
    vv[ii] = 1 / big;
  }
#endif
  
  for (jj = 0; jj < dim; jj++)
  {
    for (ii = 0; ii < jj; ii++)
    {
      sum = mat[ii][jj];
      for (kk = 0; kk < ii; kk++)
        sum -= mat[ii][kk] * mat[kk][jj];
      mat[ii][jj] = sum;
    }
    big = 0;
    for (ii = jj; ii < dim; ii++)
    {
      sum = mat[ii][jj];
      for (kk = 0; kk < jj; kk++)
        sum -= mat[ii][kk] * mat[kk][jj];
      mat[ii][jj] = sum;
      if ((dumm = (double)(vv[ii] * fabs(sum))) >= big)
      {
        big = dumm;
        imax = ii;
      }
    }
    
#if PIVOT
    if (jj != imax)
    {
      for (kk = 0; kk < dim; kk++)
      {
        dumm = mat[imax][kk];
        mat[imax][kk] = mat[jj][kk];
        mat[jj][kk] = dumm;
      }
      vv[imax] = vv[jj];
    }
    index[jj] = imax;
#else
    index[jj] = jj;
#endif
    
    if (fabs(mat[jj][jj]) <= SIGNIFICANT) mat[jj][jj] = (double)TINY;
    if (jj < (dim - 1))
    {
      dumm = 1 / mat[jj][jj];
      for (ii = jj + 1; ii < dim; ii++)
        mat[ii][jj] *= dumm;
    }
  }
  
  FREE((char *)vv);
  return (0);
}

void lubksb(double **mat, int dim, int *index, double *b)
{
  int ii, ix = -1, ip, jj;
  double sum;
  
  for (ii = 0; ii < dim; ii++)
  {
#if PIVOT
    ip = index[ii];
    sum = b[ip];
    b[ip] = b[ii];
#else
    sum = b[ii];
#endif
    if (ix >= 0)
      for (jj = ix; jj < ii; jj++)
        sum -= mat[ii][jj] * b[jj];
    else if (sum) ix = ii;
    b[ii] = sum;
  }
  
  for (ii = dim - 1; ii >= 0; ii--)
  {
    sum = b[ii];
    for (jj = ii + 1; jj < dim; jj++)
      sum -= mat[ii][jj] * b[jj];
    b[ii] = sum / mat[ii][ii];
  }
}
