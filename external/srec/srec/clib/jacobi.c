/*---------------------------------------------------------------------------*
 *  jacobi.c  *
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
#include <assert.h>

#include "hmmlib.h"
#include "portable.h"

#undef EPSILON
#define EPSILON         0.00000001
#define MAX_ITER      100
#define DEFAULT_MAX_PC_DIFF     0.0001
#define DEFAULT_MAX_OFF_DIAG 0.0001

static void   Rotate(double **a, int dim, int i, int j, int k, int l, double s,
                     double tau);
                     
static double SumOffDiag(double **mat, int dim);


/* ------------------------------------------------------------------------ */

void Jacobi(double **matrix, int dim, double *egval, double **egvec)
/*
//    Compute all eigenvalues and eigenvectors of the real symmetric matrix
//    <mat>  of size  <dim>x<dim>.  Fills in <egval> with the eigenvalues
//    and  <egvec[i]>  with the i-th normalized eignevector of  <mat>.
*/
{
  int    i, j, k;
  int    nRotations,  /* number of Jacobi rotations that are performed */
  iter;
  double g, thresh, sum, c, s, t, tau, h;
  double *b, *d, *z;
  double **v, **a;
  
  ASSERT(matrix);
  ASSERT(egval);
  ASSERT(egvec);
  
  b = (double *) CALLOC(dim, sizeof(double), "clib.jacobi.b");
  d = (double *) CALLOC(dim, sizeof(double), "clib.jacobi.d");
  z = (double *) CALLOC(dim, sizeof(double), "clib.jacobi.z");
  a = (double **) CALLOC(dim, sizeof(double *), "clib.jacobi.input_jacobi");
  v = (double **) CALLOC(dim, sizeof(double *), "clib.jacobi.input_jacobi");
  for (i = 0; i < dim; i++)
  {
    a[i] = (double *) CALLOC(dim, sizeof(double), "clib.jacobi.input_jacobi[]");
    v[i] = (double *) CALLOC(dim, sizeof(double), "clib.jacobi.input_jacobi[]");
    for (j = 0; j < dim; j++)
      a[i][j] = (float) matrix[i][j];
  }
  
  /* initialize v to identity matrix, d and b to the diagonal of mat */
  for (i = 0; i < dim; i++)
  {
    v[i][i] = 1.0;
    b[i] = d[i] = a[i][i];
  }
  
  nRotations = 0;
  iter = 0;
  
  while (1)
  {
    sum = SumOffDiag(a, dim);
    
    if (sum < EPSILON)    /* normal convergence */
    {
      log_report("\nConverged after %u iterations", iter);
      break;
    }
    if (iter >= MAX_ITER)
    {
      log_report("\nMax number %u of iterations reached",
                 MAX_ITER);
      break;
    }
    
    if (iter < 3)
      thresh = 20.0 * sum / (dim * dim);    /* .. first 3 iterations only */
    else
      thresh = 0.0;                    /* .. thereafter */
      
    for (i = 0; i < dim - 1; i++)
    {
      for (j = i + 1; j < dim; j++)
      {
        g = 100.0 * fabs(a[i][i]);
        
        /* after 4 iter's, skip rotation if off-diag elmt is small */
        if ((iter >= 4) && (g < EPSILON*fabs(d[i]))
            && (g < EPSILON*fabs(d[j])))
        {
          a[i][i] = 0.0;
        }
        else if (g > thresh)
        {
          h = d[j] - d[i];
          
          if (g < EPSILON*fabs(h))
            t = a[i][j] / h;
          else
          {
            double theta = 0.5 * h / a[i][j];
            t = 1.0 / (fabs(theta) + sqrt(1.0 + theta * theta));
            if (theta < 0.0)  t = -t;
          }
          
          c = 1.0 / sqrt(1 + t * t);
          s = t * c;
          tau = s / (1.0 + c);
          h = t * a[i][j];
          
          z[i] -= h;
          z[j] += h;
          d[i] -= h;
          d[j] += h;
          a[i][j] = 0.0;
          
          for (k = 0  ; k < i;   k++)  Rotate(a, dim, k, i, k, j, s, tau);
          for (k = i + 1; k < j;   k++)  Rotate(a, dim, i, k, k, j, s, tau);
          for (k = j + 1; k < dim; k++)  Rotate(a, dim, i, k, j, k, s, tau);
          
          for (k = 0;   k < dim; k++)  Rotate(v, dim, k, i, k, j, s, tau);
          
          nRotations++;
        }
      }
    }
    
    for (i = 0; i < dim; i++)    /* update d[] and re-initialize z[] */
    {
      b[i] += z[i];
      d[i] = b[i];
      z[i] = 0.0;
    }
    
    iter++;
  }
  
  /* save eigenvectors and eigenvalues */
  for (i = 0; i < dim; i++)
  {
    /* the i-th column of v is the i-th eigenvector */
    for (j = 0; j < dim; j++)  egvec[i][j] = v[j][i]; /* TODO: should this be egvec[j][i] */
    
    /* the i-th entry of d is the i-th eigenvalue */
    egval[i] = d[i];
  }
  
  log_report("\nDiagonalization required %u Jacobi rotations",
             nRotations);
             
  FREE((char *)b);
  FREE((char *)d);
  FREE((char *)z);
  for (i = 0; i < dim; i++)
  {
    FREE((char *)a[i]);
    FREE((char *)v[i]);
  }
  FREE((char *)a);
  FREE((char *)v);
  return;
}

/* ------------------------------------------------------------------------ */

static void Rotate(double **a, int dim, int i, int j, int k, int l, double s,
                   double tau)
{
  double g = a[i][j],
             h = a[k][l];
             
  a[i][j] = g - s * (h + g * tau);
  a[k][l] = h + s * (g - h * tau);
  return;
}

static double SumOffDiag(double **mat, int dim)
/*
//    Compute the sum of the absolute values of the off-diagonal elements
**/
{
  int    i, j;
  double sum = 0.0;
  
  for (i = 0; i < dim - 1; i++)
    for (j = i + 1; j < dim; j++)
      sum += fabs(mat[i][j]);
      
  return (sum);
}

