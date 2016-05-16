/*---------------------------------------------------------------------------*
 *  imeld_tr.c  *
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
#include "portable.h"

#include "../cfront/sh_down.h"


static const char imeld_tr[] = "$Id: imeld_tr.c,v 1.2.10.10 2008/04/01 18:23:20 dahan Exp $";

void linear_transform_frame(preprocessed *prep, imeldata *fram, int do_shift)
/*
**  Note the matrix is the transpose of the transformation
**  To transform a single frame in place */
{
  int      ii, jj;
  imeldata vec[MAX_DIMEN];
  int dim = prep->dim;

  ASSERT(prep);
  ASSERT(prep->dim < MAX_DIMEN);
  ASSERT(fram);
  for (ii = 0; ii < dim; ii++)
  {
    vec[ii] = 0;
    for (jj = 0; jj < prep->dim; jj++)
      vec[ii] += prep->matrix[ii][jj] * fram[jj];
    ASSERT(prep->imel_shift > 0);
    vec[ii] = (imeldata) SHIFT_DOWN((int)vec[ii],
                                    (unsigned int)prep->imel_shift);
  }

  if (do_shift)
  {
    if (prep->offset)
      for (ii = 0; ii < dim; ii++)
        fram[ii] = RANGE(vec[ii] + prep->offset[ii], 0, 255);
    else
      for (ii = 0; ii < dim; ii++)
        fram[ii] = RANGE(vec[ii], 0, 255);
  }
  else
  {
    for (ii = 0; ii < dim; ii++)
      fram[ii] = vec[ii];
  }
  return;
}

void inverse_transform_frame (preprocessed *prep, imeldata *fram, int do_shift)
/*
**  Note the matrix is the transpose of the transformation
**  To transform a single frame in place */
{
    int	     ii, jj;
    imeldata vec[MAX_DIMEN];

    ASSERT (prep);
    ASSERT (prep->dim < MAX_DIMEN);
    ASSERT (fram);

    if (prep->offset && do_shift)
	for (ii= 0; ii < prep->dim; ii++)
	    fram[ii] -= prep->offset[ii];

    for (ii= 0; ii < prep->dim; ii++) {
	vec[ii]= 0;
	for (jj= 0; jj < prep->dim; jj++)
	    vec[ii] += prep->invmat[ii][jj] * fram[jj];
	vec[ii]= SHIFT_DOWN (vec[ii], prep->inv_shift);
    //floating pt // for (jj= 0; jj < prep->dim; jj++)
	//floating pt // vec[ii] += (imeldata)(prep->inverse[ii][jj] * fram[jj]);
    }
    if (do_shift)
	for (ii= 0; ii < prep->dim; ii++)
	    fram[ii]= RANGE (vec[ii], 0, 255);
    else
	for (ii= 0; ii < prep->dim; ii++)
	    fram[ii]= vec[ii];
    return;
}


