/*---------------------------------------------------------------------------*
 *  filter.h  *
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

#ifndef __FILTER_H__
#define __FILTER_H__

// Define new data types

typedef short typeSample;  // for input and output samples
typedef short typeCoeff;   // FIR filter coefficients
typedef long  typeAccum;   // FIR filter accumulator

#define FACTOR_UP                       1     // upsampling factor
#define FACTOR_DOWN                     4     // downsampling factor

#define u16ScaleFilterCoeff_up1_down4  15

typedef struct fir_struct
{
   int              state;   // state of FIR delay line (index of next slot to fill)
   typeSample      *z;       // pointer to delay line

   unsigned int     factor_up;
   unsigned int     factor_down;

   unsigned int     nTaps;   // length of FIR filter
   unsigned int     scale;   // fixed-point filter scale factor
   const typeCoeff *h;       // pointer to FIR filter coefficients
   typeAccum        round;   // used for roundoff
} FIR_struct;

extern const typeCoeff ps16FilterCoeff_up1_down4[];

extern unsigned int filter_length;

extern  void FIR_downsample(unsigned int nInput, typeSample *pInput,
			   typeSample *pOutput, FIR_struct *pFIR);

extern  FIR_struct* FIR_construct(unsigned int nTaps, const typeCoeff *pCoeffs, int scale, int factor_up, int factor_down);

extern  int FIR_deconstruct(FIR_struct *pFIR);

extern  void FIR_reset(FIR_struct *pFIR);

#endif
