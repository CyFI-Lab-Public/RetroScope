/*---------------------------------------------------------------------------*
 *  hmm_type.h  *
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



#ifndef _h_hmm_types
#define _h_hmm_types

#ifdef SET_RCSID
static const char hmm_type_h[] = "$Id: hmm_type.h,v 1.1.10.5 2007/08/31 17:44:53 dahan Exp $";
#endif


#include <math.h>
#include "all_defs.h"

typedef ESR_BOOL  booldata; /* general boolean data */
typedef int  scodata;        /* for path scores */
typedef int  prdata;         /* for observation probabilities */
typedef signed char     wtdata;
typedef unsigned char   unidata;        /* for unimodal model storage */
typedef unidata  moddata;        /* for segmental model storage */
typedef double          covdata;        /* covariance data */
typedef unsigned char   featdata;       /* for preprocessor data */
typedef int             imeldata;       /* for preprocessor data */
#define IMELDATA_TO_FEATDATA(X)   ((featdata)(X))
typedef long  accdata;        /* for accumulate storage */
typedef unsigned char   phonedata;      /* for model contexts */
typedef phonedata conxdata;
typedef int             latdata;        /* for lattice identifiers */

#endif
