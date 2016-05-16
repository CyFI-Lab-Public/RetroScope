/*---------------------------------------------------------------------------*
 *  fronttyp.h  *
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

#ifndef _h_fronttyp_
#define _h_fronttyp_

typedef long     bigdata;
typedef int     coefdata;
typedef short     samdata;
typedef int     fftdata; /* TODO: Swap in fixed point datatype when working */
typedef int     cepdata;

/******************************************************************************
**  Fixed point processing macros. These are defined in non-fixed point build also.
*******************************************************************************/

#define COEFDATA_SIZE   32 /* coefficients */
#define SAMDATA_SIZE    16 /* samples */
#define FFTDATA_SIZE    32 /* fft internal data */
#define CEPDATA_SIZE    32 /* cepstrum internal data */

#define HALF_COEFDATA_SIZE      (COEFDATA_SIZE >> 1)
#define HALF_SAMDATA_SIZE       (SAMDATA_SIZE >> 1)
#define HALF_FFTDATA_SIZE       (FFTDATA_SIZE >> 1)
#define HALF_CEPDATA_SIZE       (CEPDATA_SIZE >> 1)
#define QUART_CEPDATA_SIZE      (CEPDATA_SIZE >> 2)
#define QUART_FFTDATA_SIZE      (CEPDATA_SIZE >> 2)

/*  scaling in terms of shifts */
#define COSINE_TABLE_SHIFT HALF_CEPDATA_SIZE   /* minimum 5 */
#define RAMP_SHIFT  6
#define HALF_RAMP_SHIFT  (RAMP_SHIFT >> 1)
#define COEFDATA_SHIFT  8
#define HALF_COEFDATA_SHIFT (COEFDATA_SHIFT >> 1)
#define WAVE_SHIFT  0
#define BYTERANGE_SHIFT  1

#endif
