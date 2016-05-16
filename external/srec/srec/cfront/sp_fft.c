/*---------------------------------------------------------------------------*
 *  sp_fft.c  *
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

/*
****************************************************************************
**
**  FILE:         sp_fft.cpp
**
**  CREATED:   11-September-99
**
**  DESCRIPTION:  Split-Radix FFT
**
**
**
**
**  MODIFICATIONS:
** Revision history log
** VSS revision history.  Do not edit by hand.
**
** $NoKeywords: $
**
*/

#ifndef _RTT
#include <stdio.h>
#endif
#include <math.h>
#include <assert.h>
#include "front.h"
#include "portable.h"

#include "sp_fft.h"
#include "himul32.h"
/*extern "C" asr_int32_t himul32(asr_int32_t factor1, asr_int32_t factor2);*/

/* >>>> Fixed Point, Floting Point, and Machine Specific Methods <<<<

  We will localize all fixed point, floating point, and machine specific
  operations into the following methods so that in the main body of the code
  we would not need to worry these issues.
*/

/* convert trigonomy function data to its required representation*/
static trigonomydata
to_trigonomydata(double a)
{
  unsigned scale = (unsigned int)(1 << (8 * sizeof(trigonomydata) - 1));
  return (trigonomydata)(a * scale);
}

/* Do a sign-extending right shift of x by i bits, and
 * round the result based on the leftmost bit shifted out.
 * Must have 1 <= i < 32.
 * Note that C doesn't define whether right shift of signed
 * ints sign-extends or zero-fills.
 * On platforms that do sign-extend, use the native right shift.
 * Else use a short branch-free sequence that forces in copies
 * of the sign bit.
 */
static PINLINE asr_int32_t rshift(asr_int32_t x, int i)
{
  asr_int32_t xshift = x >> i;
  ASSERT(i >= 1 && i < 32);

#if -1 >> 31 != -1
  asr_int32_t signbit = (asr_int32_t)(((asr_uint32_t)x & 0x80000000U) >> i);
  xshift |= -signbit;
#endif

  xshift += (x >> (i - 1)) & 1;
  return xshift;
}


/*  compute (a + jb)*(c + jd) = a*c - b*d + j(ad + bc) */
static PINLINE void complex_multiplier(trigonomydata a, trigonomydata b,
                                       fftdata c,   fftdata d,
                                       fftdata* real,  fftdata* imag)
{
  /*
      himul32(factor1, factor2) = floor( (factor1 * factor2) / 2**32 )
      we need floor( (factor1 * factor2) / 2**31 )
      retain one more bit of accuracy by left shifting first.
  */
  c <<= 1;
  d <<= 1;
  *real = himul32(a, c) - himul32(b, d);
  *imag = himul32(a, d) + himul32(b, c);
}

/* determine the maximum number of bits required to represent the data */
static PINLINE int data_bits(const int length, fftdata data[])
{
  asr_uint32_t  bits = 0;
  int     d    = 0;
  int     i;

  ASSERT(sizeof(data[0]) == 4);

  for (i = 0; i < length; i++)
  {
    d = data[i];
    bits |= (d > 0) ? d : -d;
  }

  d = 0;
  while (bits > 0)
  {
    bits >>= 1;
    d++;
  }

  return d;
}


/* >>>> Fixed Point, Floting Point, and Machine Independent Methods <<<< */

/* constructor */
srfft* new_srfft(unsigned logLength)
{
  srfft* pthis;

  /* cannot do smaller than 4 point FFT */
  ASSERT(logLength >= 2);

  pthis              = (srfft*) CALLOC(1, sizeof(srfft), "srfft");
  pthis->m_logLength = logLength;
  pthis->m_length    = 1 << logLength;

  allocate_bitreverse_tbl(pthis);
  allocate_butterfly_tbl(pthis);
  allocate_trigonomy_tbl(pthis);

  return pthis;
}

/* destructor */
void delete_srfft(srfft* pSrfft)
{
  FREE((char*)pSrfft->m_sin3Tbl);
  FREE((char*)pSrfft->m_cos3Tbl);
  FREE((char*)pSrfft->m_sin2Tbl);
  FREE((char*)pSrfft->m_cos2Tbl);
  FREE((char*)pSrfft->m_sin1Tbl);
  FREE((char*)pSrfft->m_cos1Tbl);
  FREE((char*)pSrfft->m_butterflyIndexTbl);
  FREE((char*)pSrfft->m_bitreverseTbl);
  FREE((char*)pSrfft);
}

/*
    allocate L shaped butterfly index lookup table
*/
void allocate_butterfly_tbl(srfft* pthis)
{
  unsigned butterflyLength, butterflies, *butterflyIndex;
  unsigned m, n, n2, i, j, i0, is, id, ii, ib;


  /* compute total number of L shaped butterflies */
  m = pthis->m_logLength;
  butterflyLength = 0;
  butterflies  = 0;
  for (i = 0; i < m; i++)
  {
    butterflies = (i % 2) ? butterflyLength : butterflyLength + 1;
    butterflyLength += butterflies;
  }

  /*  Allocate m more spaces to store size information */
  butterflyLength += m;
  butterflyIndex = (unsigned*) CALLOC(butterflyLength, sizeof(unsigned), "srfft.butterflyIndex");

  /* Compute and store L shaped butterfly indexes at each stage */
  n = pthis->m_length;
  n2  = n << 1;
  butterflyLength = 0;
  ib = 0;
  for (i = 0; i < m; i++)
  {
    butterflies = (i % 2) ? butterflyLength : butterflyLength + 1;
    butterflyLength += butterflies;

    /* Store number of L butterflies at stage m-i*/
    butterflyIndex[ib++] = butterflies;

    /* Compute Sorensen, Heideman, and Burrus indexes for L shaped butterfiles */
    id = n2;
    is = 0;
    n2 = n2 >> 1;
    while (is < n)
    {
      for (i0 = is; i0 < n; i0 += id)
      {
        butterflyIndex[ib] = i0;
        if (i0 != 0)
        {
          /* sort bufferfly index in increasing order to simplify look up */
          ii = ib;
          while (butterflyIndex[ii] < butterflyIndex[ii-1])
          {
            j = butterflyIndex[ii];
            butterflyIndex[ii] = butterflyIndex[ii-1];
            butterflyIndex[--ii] = j;
          }
        }
        ib++;
      }
      is = 2 * id - n2;
      id = id << 2;
    }
  }
  pthis->m_butterflyIndexTbl = butterflyIndex;

  /* move to stage 2 buffer index table */
  for (i = 0; i < m - 2; i++)
  {
    butterflies = *butterflyIndex;
    butterflyIndex += (butterflies + 1);
  }

  /*
      Since we want to compute four point butterflies directly,
      when we compute two point butterflieswe at the last stage
      we must bypass those two point butterflies that are decomposed
      from previous stage's four point butterflies .
  */
  butterflies = *butterflyIndex++; /* number of four point butterflies */
  ii = butterflies + 1;   /* index to the two point butterflies*/
  for (i = 0; i < butterflies; i++)
  {
    j = butterflyIndex[i];

    /*
        find those two point butterflies that are
        decomposed from the four point butterflies
    */
    while (butterflyIndex[ii] != j) /* look up is sure so do not need worry over bound*/
    {
      ii++;
    }
    butterflyIndex[ii++] = 0;

    ASSERT(ii <= butterflyLength + m);
  }
}


/*
    Allocate trigonoy function lookup tables
*/
void allocate_trigonomy_tbl(srfft* pthis)
{
  trigonomydata *pcos1, *psin1, *pcos2, *psin2, *pcos3, *psin3;
  unsigned  m, n, n2, n4, i, j, ii, nt;
  double   e, radias, radias3;

  m  = pthis->m_logLength;
  n  = pthis->m_length;
  nt = (n >> 1) - 1;
  pcos1 = (trigonomydata*) CALLOC(nt, sizeof(trigonomydata), "srfft.trigonomydata");
  psin1 = (trigonomydata*) CALLOC(nt, sizeof(trigonomydata), "srfft.trigonomydata");
  pcos2 = (trigonomydata*) CALLOC(nt, sizeof(trigonomydata), "srfft.trigonomydata");
  psin2 = (trigonomydata*) CALLOC(nt, sizeof(trigonomydata), "srfft.trigonomydata");
  pcos3 = (trigonomydata*) CALLOC(nt, sizeof(trigonomydata), "srfft.trigonomydata");
  psin3 = (trigonomydata*) CALLOC(nt, sizeof(trigonomydata), "srfft.trigonomydata");

  ii = 0;
  n2 = n << 1;
  for (i = 0; i < m - 1; i++)
  {
    n2 = n2 >> 1;
    n4 = n2 >> 2;
    e = 6.283185307179586 / n2;

    for (j = 0; j < n4; j++)
    {
      if (j != 0) /* there is no need for radias zero trigonomy tables */
      {
        radias  = j * e;
        radias3 = 3.0 * radias;

        pcos1[ii]   = to_trigonomydata(cos(radias));
        psin1[ii]   = to_trigonomydata(sin(radias));
        pcos3[ii]   = to_trigonomydata(cos(radias3));
        psin3[ii]   = to_trigonomydata(sin(radias3));
      }
      ii++;
    }
  }

  for (i = 0; i < nt; i++)
  {
    radias = 3.141592653589793 * (i + 1) / n;

    pcos2[i]  = to_trigonomydata(cos(radias));
    psin2[i]  = to_trigonomydata(sin(radias));
  }

  pthis->m_cos1Tbl = pcos1;
  pthis->m_sin1Tbl = psin1;
  pthis->m_cos2Tbl = pcos2;
  pthis->m_sin2Tbl = psin2;
  pthis->m_cos3Tbl = pcos3;
  pthis->m_sin3Tbl = psin3;

}

/*
    Allocate bit reverse tables
*/
void allocate_bitreverse_tbl(srfft* pthis)
{
  unsigned forward, reverse, *tbl;
  unsigned m, n, i, j;

  n  = pthis->m_length;
  tbl = (unsigned*) CALLOC(n, sizeof(unsigned), "srfft.bitreverseTbl");
  for (j = 0; j < n; j++) tbl[j] = 0;

  m  = pthis->m_logLength;
  forward = 1;
  reverse = n >> 1;
  for (i = 0; i < m; i++)
  {
    for (j = 0; j < n; j++)
    {
      if (forward & j) tbl[j] |= reverse;
    }
    reverse >>= 1;
    forward <<= 1;
  }

  pthis->m_bitreverseTbl = tbl;
}


/*
    Compute a four point FFT that requires no multiplications
*/
static PINLINE void four_point_fft1(fftdata* data)
{
  fftdata r0, r1, r2;

  r0  = data[0];
  r1  = data[4];
  data[0] = r0 + r1;
  data[4] = r0 - r1;

  r0  = data[2];
  r1  = data[6];
  data[2] = r0 + r1;
  data[6] = r0 - r1;

  r0  = data[1];
  r1  = data[5];
  data[1] = r0 + r1;
  data[5] = r0 - r1;

  r0  = data[3];
  r1  = data[7];
  data[3] = r0 + r1;
  data[7] = r0 - r1;

  r0 = data[0];
  r1 = data[2];
  data[0] = r0 + r1;
  data[2] = r0 - r1;

  r0 = data[1];
  r1 = data[3];
  data[1] = r0 + r1;
  data[3] = r0 - r1;

  r0 = data[4];
  r1 = data[7];
  r2 = data[6];
  data[4] = r0 + r1;
  data[6] = r0 - r1;

  r0 = data[5];
  data[5] = r0 - r2;
  data[7] = r0 + r2;
}

/*
    Compute a two point FFT that requires no multiplications
*/
static PINLINE void two_point_fft1(fftdata* data)
{
  fftdata r0, r1;

  r0 = data[0];
  r1 = data[2];
  data[0] = r0 + r1;
  data[2] = r0 - r1;

  r0 = data[1];
  r1 = data[3];
  data[1] = r0 + r1;
  data[3] = r0 - r1;
}


static PINLINE void comp_L_butterfly1(unsigned butteflyIndex, unsigned quarterLength,
                                      trigonomydata  cc1,  trigonomydata ss1,
                                      trigonomydata    cc3,  trigonomydata ss3,
                                      fftdata* data)
{
  unsigned k1, k2, k3;
  fftdata  r0, r1, r2, r3, i0, i1, i2, i3;

  quarterLength <<= 1;

  k1 = quarterLength;
  k2 = k1 + quarterLength;
  k3 = k2 + quarterLength;

  r0 = data[0];
  r1 = data[k1];
  r2 = data[k2];
  r3 = data[k3];
  i0 = data[1];
  i1 = data[k1+1];
  i2 = data[k2+1];
  i3 = data[k3+1];

  /* compute the radix-2 butterfly */
  data[0]    = r0 + r2;
  data[k1]   = r1 + r3;
  data[1]    = i0 + i2;
  data[k1+1] = i1 + i3;

  /* compute two radix-4 butterflies with twiddle factors */
  r0 -= r2;
  r1 -= r3;
  i0 -= i2;
  i1 -= i3;

  r2 = r0 + i1;
  i2 = r1 - i0;
  r3 = r0 - i1;
  i3 = r1 + i0;

  /*
      optimize the butterfly computation for zero's power twiddle factor
      that does not need multimplications
  */
  if (butteflyIndex == 0)
  {
    data[k2] = r2;
    data[k2+1] = -i2;
    data[k3] = r3;
    data[k3+1] = i3;
  }
  else
  {
    complex_multiplier(cc1, -ss1, r2, -i2, data + k2, data + k2 + 1);
    complex_multiplier(cc3, -ss3, r3, i3,  data + k3, data + k3 + 1);
  }
}

/**********************************************************************/
void do_fft1(srfft* pthis, unsigned length2, fftdata* data)
{
  unsigned  *indexTbl, indexLength;
  trigonomydata *cos1, *sin1, *cos3, *sin3;
  trigonomydata cc1, ss1, cc3, ss3;
  unsigned  n, m, n4, i, j, k, ii, k0;
  fftdata   temp;

  /* Load butterfly index table */
  indexTbl = pthis->m_butterflyIndexTbl;
  indexLength = 0;

  /* Load cosine and sine tables */
  cos1 = pthis->m_cos1Tbl;
  sin1 = pthis->m_sin1Tbl;
  cos3 = pthis->m_cos3Tbl;
  sin3 = pthis->m_sin3Tbl;

  /* stages of butterfly computation*/
  n = pthis->m_length;
  m = pthis->m_logLength;


  /*
      compute L shaped butterfies util only 4 and 2 point
      butterfiles are left
  */
  n4 = n >> 1;
  ii = 0;
  for (i = 0; i < m - 2; i++)
  {
    n4 >>= 1;

    /* read the number of L shaped butterfly nodes at the stage */
    indexLength = *indexTbl++;

    /*
        compute one L shaped butterflies at each stage
        j (time) and k (frequency) loops are reversed to minimize
        trigonomy table lookups
    */
    for (j = 0; j < n4; j++)
    {
      cc1 = cos1[ii];
      ss1 = sin1[ii];
      cc3 = cos3[ii];
      ss3 = sin3[ii++];
      for (k = 0; k < indexLength; k++)
      {
        k0 = indexTbl[k] + j;
        k0 <<= 1;
        comp_L_butterfly1(j, n4, cc1, ss1, cc3, ss3, data + k0);
      }
    }

    /* Move to the butterfly index table of the next stage*/
    indexTbl += indexLength;
  }

  /* Compute 4 point butterflies at stage 2 */
  indexLength = *indexTbl++;
  for (k = 0; k < indexLength; k++)
  {
    k0 = indexTbl[k];
    k0 <<= 1;
    four_point_fft1(data + k0);
  }
  indexTbl += indexLength;

  /* Compute 2 point butterflies of the last stage */
  indexLength = *indexTbl++;
  for (k = 0; k < indexLength; k++)
  {
    k0 = indexTbl[k];
    k0 <<= 1;

    /* k0 = 0 implies these nodes have been computed */
    if (k0 != 0)
    {
      two_point_fft1(data + k0);
    }
  }

  /* Bit reverse the data array */
  indexTbl = pthis->m_bitreverseTbl;
  for (i = 0; i < n; i++)
  {
    ii = indexTbl[i];
    if (i < ii)
    {
      j = i << 1;
      k = ii << 1;
      temp = data[j];
      data[j] = data[k];
      data[k] = temp;

      j++;
      k++;
      temp = data[j];
      data[j] = data[k];
      data[k] = temp;
    }
  }
}

void do_real_fft(srfft* pthis, unsigned n, fftdata* data)
{
  unsigned  n2;
  unsigned  i, i1, i2, i3, i4;
  fftdata   h1r, h1i, h2r, h2i, tr, ti;
  trigonomydata *cos2, *sin2;

  cos2  = pthis->m_cos2Tbl;
  sin2  = pthis->m_sin2Tbl;

  /* do a complex FFT of half size using the even indexed data
  ** as real component and odd indexed data as imaginary data components
  */

  do_fft1(pthis, n, data);

  /*
  **  queeze the real valued first and last component of
  **  the complex transform as elements data[0] and data[1]
  */
  tr = data[0];
  ti = data[1];
  data[0] = (tr + ti);
  data[1] = (tr - ti);

  /* do the rest of elements*/
  n2  = n >> 2;
  for (i = 1; i < n2; i++)
  {
    i1 = i << 1;
    i2 = i1 + 1;
    i3 = n - i1;
    i4 = i3 + 1;

    h1r = (data[i1] + data[i3]) / 2;
    h1i = (data[i2] - data[i4]) / 2;
    h2r = (data[i2] + data[i4]) / 2;
    h2i = -(data[i1] - data[i3]) / 2;

    complex_multiplier(cos2[i-1], -sin2[i-1], h2r, h2i, &tr, &ti);

    data[i1] = h1r + tr;
    data[i2] = h1i + ti;
    data[i3] = h1r - tr;
    data[i4] = -h1i + ti;
  }
  /* center one needs no multiplication, but has to reverse sign */
  i = (n >> 1);
  data[i+1] = -data[i+1];

}

/*****************************************************************************/

int do_real_fft_magsq(srfft* pthis, unsigned n, fftdata* data)
{
  fftdata tr, ti, last;
  unsigned i, ii, n1;
  int  scale    = 0;
  int  s        = 0;
  unsigned maxval   = 0;


  /*
  **   Windowed fftdata has an unknown data length - determine this using
  **   data_bits(), a maximum of:
  **
  **   fixed data = windowedData * 2**HAMMING_DATA_BITS
  **
  **   FFT will grow data log2Length. In order to avoid data overflow,
  **   we can scale data by a factor
  **
  **   scale = 8*sizeof(fftdata) - data_bits() - log2Length
  **
  **   In other words, we now have
  **
  **   fixed data = windowedData * 2**HAMMING_DATA_BITS * 2**scale
  **
  */


  scale = 8 * sizeof(fftdata) - 2 - pthis->m_logLength;
  scale -= data_bits(n, data);

  for (i = 0; i < n; i++)
  {
    if (scale < 0)
    {
      data[i] = rshift(data[i], -scale);
    }
    else
    {
      data[i] <<= scale;
    }
  }

  /* compute the real input fft,  the real valued first and last component of
  ** the complex transform is stored as elements data[0] and data[1]
  */

  do_real_fft(pthis, n, data);

  /*  After fft, we now have the data,
  **
  **  fixed data = fftdata * 2**HAMMING_DATA_BITS * 2**scale
  **
  **  to get fft data, we then need to reverse-shift the fixed data by the
  **  scale constant;
  **
  **  However, since our purpose is to compute magnitude, we can combine
  **  this step into the magnitude computation. Notice that
  **
  **  fixed data = fftdata * 2**(8*sizeof(fftdata) - DATA_BITS - log2Length)
  **
  **  if we use himul32 to compute the magnitude, which gives us,
  **
  **  fixed magnitude = fftdata magnitude * 2**(2*(32 - 16 - log2Length)) - 2**32)
  **                  = fftdata magnitude * 2**(-2*log2Length)
  **
  **  to get the fixed magnitude = fftdata magnitude * 2**(-log2Length-1)
  **                             = fftdata magnitude/FFT length
  **  we need to upscale fftdata to cancel out the log2Lenght-1 factor in
  **  the fixed magnitude
  **
  **  Notice that upshift scale log2Lenght-1 is not a constant, but a
  **  function of FFT length.
  **  Funthermore, even and odd log2Length-1 must be handled differently.
  **
  */

  /*
  **  This bit is a lot simpler now, we just aim to get the pre-magsqu
  **  values in a 30-bit range + sign.
  **  This is the max val if we want r*r+i*i guarenteed < signed int64 range.
  **  So shift the data up until it is ==30 bits (FFTDATA_SIZE-2)
  */

  s = (FFTDATA_SIZE - 2) - data_bits(n, data);
  /* n is twice the size, so this */
  

  for (i = 0; i < n; i++)
  {
    if (s < 0)
    {
      data[i] = rshift(data[i], -s);
    }
    else
    {
      data[i] <<= s;
    }
  }

  scale += s;

  /*
  **  OK, now we are in the 30bit range, we can do a magsq.
  **  This magsq output must be less that 60bit plus sign.
  */

  /*
  **  Special at start as DC and Nyquist freqs are in data[0] and data[1]
  **  respectively.
  */

  tr = data[0];
  data[0] = himul32(tr, tr);
  maxval |= data[0];

  tr = data[1];
  last = himul32(tr, tr);
  maxval |= last;

  n1 = n >> 1;
  for (i = 1; i < n1; i++)
  {
    ii = i << 1;
    tr = data[ii];
    data[i] = himul32(tr, tr);

    ti = data[++ii];
    data[i] += himul32(ti, ti);

    maxval |= data[i];
  }

  data[n1] = last; /* now the Nyquist freq can be put in place */

  /*
  **  computing magnitude _squared_ means the scale is effectively
  **  applied twice
  */

  scale *= 2;

  /*
  **  account for inherent scale of fft - we have do to this here as each
  **  stage scales by sqrt(2), and we couldn't add this to scale until
  **  after it had been multiplied by two (??)
  **  TODO: The truth is we got here by trial and error
  **   This should be checked.
  */

  scale += pthis->m_logLength + 1;

  /*
  **  doing the himul32() shift results in shifting down by 32(FFTDATA_SIZE) bits.
  */

  scale -= 32;

  ASSERT(maxval >= 0);
  ASSERT(!(maxval & 0xC0000000));
  /* we've done something wrong if */
  /* either of the top two bits  */
  /* get used!    */

  return(-scale);  /* return the amount we have shifted the */
  /* data _down_ by    */

}


/*****************************************************************************/

void configure_fft(fft_info *fft, int size)
{
  unsigned int log2Length, length;

  log2Length = 0;
  length = size / 2;
  while (length > 1)
  {
    length = length >> 1;
    log2Length++;
  }

  ASSERT(size == 1 << (log2Length + 1));
  fft->size2 = size;
  fft->size = size / 2;

  fft->m_srfft = new_srfft(log2Length);
  fft->real = (fftdata*) CALLOC(size + 2, sizeof(fftdata), "srfft.fft_data");
  fft->imag = fft->real + size / 2 + 1;
}

int fft_perform_and_magsq(fft_info *fft)
{
  unsigned n = fft->size2;
  fftdata     *real = fft->real;
  srfft       *pSrfft = fft->m_srfft;
  ;

  return do_real_fft_magsq(pSrfft, n, real);
}

void unconfigure_fft(fft_info *fft)
{
  ASSERT(fft);
  delete_srfft(fft->m_srfft);
  FREE((char*)fft->real);
}


int place_sample_data(fft_info *fft, fftdata *seq, fftdata *smooth, int num)
{
  int ii, size2;
  srfft * pSrfft;

  pSrfft = fft->m_srfft;

  ASSERT(fft);
  ASSERT(seq);
  ASSERT(smooth);
  ASSERT(num <= (int)fft->size2);
  size2 = fft->size2;

  for (ii = 0; ii < num; ii++)
  {
    fft->real[ii] = (fftdata)(seq[ii] * smooth[ii]);
  }

  for (; ii < size2; ii++)
  {
    fft->real[ii] = 0;
  }

  return(-(HALF_FFTDATA_SIZE - 1));
}

