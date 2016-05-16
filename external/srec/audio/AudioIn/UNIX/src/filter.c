/*---------------------------------------------------------------------------*
 *  filter.c  *
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

#define _ROUNDOFF

/****************************************************************************
 * FILENAME
 *     pcm44pcm11.c
 *
 * DESCRIPTION
 *
 *     Apply FIR filter to 44 kHz raw 16-bit PCM linear audio then
 *     downsample to 11 kHz.
 ****************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <time.h>

#include "filter.h"

/****************************************************************************
 *                                 FIR FILTER                               *
 ****************************************************************************/

/* b = firls(120, [0 5000 6000 44000/2]/44000*2, [1 1 0 0]);   */
/* bRounded = round(b*2^15);                                   */

const typeCoeff ps16FilterCoeff_up1_down4[] = {

        1,      9,     13,     10,     -1,    -16,    -24,    -18,      2,     25,
       38,     28,     -2,    -38,    -57,    -42,      3,     55,     81,     60,
       -3,    -76,   -113,    -84,      4,    104,    153,    114,     -5,   -138,
     -205,   -152,      5,    183,    271,    202,     -6,   -241,   -360,   -269,
        6,    321,    482,    364,     -6,   -438,   -667,   -511,      7,    632,
      986,    778,     -7,  -1030,  -1704,  -1450,      7,   2451,   5204,   7366,
     8185,   7366,   5204,   2451,      7,  -1450,  -1704,  -1030,     -7,    778,
      986,    632,      7,   -511,   -667,   -438,     -6,    364,    482,    321,
        6,   -269,   -360,   -241,     -6,    202,    271,    183,      5,   -152,
     -205,   -138,     -5,    114,    153,    104,      4,    -84,   -113,    -76,
       -3,     60,     81,     55,      3,    -42,    -57,    -38,     -2,     28,
       38,     25,      2,    -18,    -24,    -16,     -1,     10,     13,      9,
        1,
};
unsigned int filter_length = sizeof(ps16FilterCoeff_up1_down4)/sizeof(typeCoeff);

/****************************************************************************
 * FIR_struct *FIR_construct(unsigned int nTaps, FIR_type *pCoeffs)
 *
 * DESCRIPTION
 *     allocate and initialize FIR structure
 *
 * INPUT
 *     nTaps    - length of FIR filter
 *     pCoeffs  - pointer to FIR filter coefficients
 *     scale    - fixed point scale factor
 *
 * OUTPUT
 *     returns pointer to FIR structure; NULL if error
 ****************************************************************************/
FIR_struct* FIR_construct(unsigned int nTaps, const typeCoeff *pCoeffs, int scale, int factor_up, int factor_down)
{
FIR_struct *pFIR;

    if (nTaps == 0)
        return NULL;

    pFIR = malloc(sizeof(FIR_struct));
    if (pFIR == NULL)
        return NULL;

    // alloocate space for delay line use calloc to avoid uninitialized memory
    // that causes an audible "pop" at the beginning of audio.  SteveR
    pFIR->z = calloc(nTaps * sizeof(typeSample), 1);
    if (pFIR->z == NULL)
    {
        free(pFIR);
        return NULL;
    }

    pFIR->factor_up   = factor_up;
    pFIR->factor_down = factor_down;

    pFIR->state       = 0;
    pFIR->h           = pCoeffs;
    pFIR->nTaps       = nTaps;
    pFIR->scale       = scale;
    pFIR->round       = (1 << (scale-1));

    return pFIR;
}

/****************************************************************************
 * int FIR_deconstruct(FIR_struct *pFIR)
 *
 * DESCRIPTION
 *     deallocate FIR structure
 *
 * INPUT
 *     pFIR     - pointer to FIR structure
 *
 * OUTPUT
 *     returns 0 for success; 1 for failure
 ****************************************************************************/

int FIR_deconstruct(FIR_struct *pFIR)
{
    if (pFIR == NULL)
        return 1;

    if (pFIR->z == NULL)
        return 1;

    free(pFIR->z);
    free(pFIR);

    return 0;
}

/****************************************************************************
 * void FIR_reset(FIR_struct *pFIR)
 *
 * DESCRIPTION
 *
 *     reset FIR state
 *
 * INPUT
 *     pFIR     - pointer to FIR structure
 *
 * OUTPUT
 *     pFIR->state initialized
 ****************************************************************************/

void FIR_reset(FIR_struct *pFIR)
{
   pFIR->state = 0;

   memset(pFIR->z, pFIR->nTaps, sizeof(typeSample));
}

/*****************************************************************************
 * FIR_type FIR_downsample(unsigned int nInput, typeSample *pInput,
 *                         typeSample *pOutput, FIR_struct *pFIR)
 *
 * DESCRIPTION
 *
 *     Apply FIR filter to input data.  If nInput > 1, this will also
 *     decimate by a factor of nInput.  That is, the filter will only be
 *     evaluated every nInput samples, not at each of the nInput samples.
 *
 *     Breakup filter computation into 2 parts to avoid doing a wraparound
 *     check inside the loop.
 *
 *     Example:
 *
 *         pFIR->nTaps   = 8
 *         pFIR->state   = 2
 *         nInput        = 1
 *         *pInput       = s20
 *
 *      (a) Store new sample(s) in delay buffer z[]
 *
 *          Since pFIR->state == 2, store new sample s20 at location z[2]
 *
 *                           *** latest input stored at z[pFIR->state]
 *                           *
 *            -------------------------------------------------
 *          z | s14 | s13 | s20 | s19 | s18 | s17 | s16 | s15 |
 *            -------------------------------------------------
 *          h | h0  | h1  | h2  | h3  | h4  | h5  | h6  | h7  |
 *            -------------------------------------------------
 *               0     1     2     3     4     5     6     7
 *
 *      (b) Update state to point to newest sample, wrap if < 0
 *
 *          Since nInput == 1, state for newest sample is still 2
 *          (otherwise, update state -= nInput-1; wrap by adding nTaps if < 0)
 *
 *      (c) Accumulate "end part" first
 *
 *           z: start with latest sample at z[pFIR->state], then advance to right
 *           h: start with 1st filter coefficient, then advance to right
 *
 *          acc = h0*s20 + h1*s19 + h2*s18 + h3*s17 + h4*s16 + h5*s15
 *
 *      (d) Accumulate "beginning part"

 *           z: start with sample at beginning of delay buffer, then advance
 *              to sample before latest one at z[pFIR->state]
 *           h: continue with next filter coefficient from step (a)
 *
 *          acc += (h6*s14 + h7*s13)      FIR filter output
 *          *pOutput = acc
 *
 *      (e) Update FIR state
 *
 *             state--, wrapping if < 0 to simulate circular buffer
 *
 * INPUT
 *
 *     nInput   - number of new input samples; evaluate FIR at this point
 *     pInput   - pointer to input sample buffer
 *     pOutput  - pointer to output sample buffer
 *     pFIR     - pointer to FIR structure
 *
 * OUTPUT
 *
 *****************************************************************************/

void FIR_downsample(unsigned int nInput, typeSample *pInput,
                    typeSample *pOutput, FIR_struct *pFIR)
{
typeAccum        accum;
typeCoeff const *ph;      // pointer to coefficients
typeSample      *pz;      // pointer to delay line
typeSample      *pz_beg;  // pointer to beginning of delay line
typeSample      *pz_end;  // pointer to last slot in delay line
unsigned int     nTaps_end;
unsigned int     nTaps_beg;
unsigned int     i;

    // initialize
    accum  = 0;
    ph     = pFIR->h;                    // point to coefficients
    pz_beg = pFIR->z;                    // start of delay line
    pz_end = pFIR->z + pFIR->nTaps - 1;  // end of delay line

    // (a) Store new input samples in delay line (circular addressing would help a lot)
    pz = pFIR->z + pFIR->state;      // point to next empty slot in delay line
    for (i = 0; i < nInput; i++)
    {
       *pz-- = *pInput++;
       if (pz < pz_beg)
          pz = pz_end;    // wrap around (circular buffer)
    }

    // (b) adjust state to reflect addition of samples
    pFIR->state -= nInput-1;
    if (pFIR->state < 0)
       pFIR->state += pFIR->nTaps;  // wrap

    // (c) Accumulate "end part"
    pz = pFIR->z + pFIR->state;
    nTaps_end = pFIR->nTaps - pFIR->state;
    for (i = 0; i < nTaps_end; i++)
    {
        accum += *ph++ * *pz++;
    }

    // (d) Accumulate "beginning part"
    pz = pFIR->z;
    nTaps_beg = pFIR->state;
    for (i = 0; i < nTaps_beg; i++)
    {
        accum += *ph++ * *pz++;
    }

    // (e) Update FIR state for next batch of incoming samples
    pFIR->state--;
    if (pFIR->state < 0)
       pFIR->state += pFIR->nTaps;  // wrap

#ifdef _ROUNDOFF
    if (accum >= 0)
       accum += pFIR->round;
    else
       accum -= pFIR->round;
#endif

    *pOutput = (typeSample) (accum >> pFIR->scale);
}

#if 0
/*****************************************************************************
 * main
 *****************************************************************************/

int main(int argc, char* argv[])
{
FILE         *fpInputSamples;    // input raw PCM file
FILE         *fpOutputSamples;   // output raw PCM file

typeSample    s_in[FACTOR_DOWN]; // input samples
typeSample    s_out;             // filtered sample
FIR_struct   *pFIR;              // pointer to FIR structure
int           nSampleGet;        // number of samples to read from input speech file
int           nSampleRead;       // number of samples read from input speech file
unsigned long nSampleTot;        // total number of samples read so far
time_t        t0;                // time upon entry

   t0 = time(NULL);     // get time upon entry

    // Check Command-line Parameters
    if (argc != 3)
    {
        fprintf(stderr, "pcm44pcm11 v1.0\n");
        fprintf(stderr, "  - downsamples 44 kHz to 11 kHz (16-bit PCM, Intel byte order)\n");
        fprintf(stderr, "Usage: pcm44pcm11 <input PCM file> <output PCM file>\n\n");
        return 0;
    }

    // Open input sample file
    if ((fpInputSamples = fopen(argv[1], "rb")) == NULL)
    {
        fprintf(stderr, "Error reading input sample file: %s\n\n", argv[1]);
        exit(1);                                // abnormal exit
    }

    // Create output sample file
    if ((fpOutputSamples = fopen(argv[2], "wb")) == NULL)
    {
        fprintf(stderr, "Error creating output file: %s\n\n", argv[2]);
        fclose(fpInputSamples);
        exit(1);                                // abnormal exit
    }

   // **************************************************************************************
   // Begin filtering...
   // **************************************************************************************

   pFIR = FIR_construct(filter_length,
                        ps16FilterCoeff_up1_down4,
                        u16ScaleFilterCoeff_up1_down4,
                        FACTOR_UP,
                        FACTOR_DOWN);

   fprintf(stdout, "Filtering...\n");

   FIR_reset(pFIR);

   nSampleTot = 0;
   while (!feof(fpInputSamples))
   {
      nSampleGet = pFIR->factor_down;   // if downsampling, only filter every factor_down samples
      nSampleRead = fread(s_in, sizeof(typeSample), nSampleGet, fpInputSamples);
      if (feof(fpInputSamples) || (nSampleRead != nSampleGet))
         break;   // done with input file
      nSampleTot += nSampleRead;

      FIR_downsample(nSampleRead, s_in, &s_out, pFIR);

      if (nSampleTot < pFIR->nTaps)
         continue;   // wait until delay buffer has been filled to skip transients

      fwrite(&s_out, sizeof(typeSample), 1, fpOutputSamples);
   }

   fprintf(stdout, "\n\nTime elapsed: %d sec\n", time(NULL)-t0);

   FIR_deconstruct(pFIR);

   fclose(fpInputSamples);
   fclose(fpOutputSamples);

   return 0;
}

#endif
