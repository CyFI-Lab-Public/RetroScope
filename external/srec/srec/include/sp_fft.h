/*---------------------------------------------------------------------------*
 *  sp_fft.h  *
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

#ifndef _SPLIT_RADIX_FFT_H_
#define _SPLIT_RADIX_FFT_H_

/*
////////////////////////////////////////////////////////////////////////////
//
//  FILE:         fft.h
//
//  CREATED:   11-September-99
//
//  DESCRIPTION:  Split-Radix FFT
//
//
//
//
//  MODIFICATIONS:
// Revision history log
    VSS revision history.  Do not edit by hand.

    $NoKeywords: $

*/

/*
>>>>> Floating and Fixed Point plit-Radix FFT -- The Fastest FFT  <<<<<<<<

The split-radix FFT improves the efficiency of the FFT implementation
by mixing a radix-2 and a radix-4 FFT algorithms. Specifically,
the radix-2 decomposes one Fourier transform into two half-sized Fourier
transform at each stage. It is the FFT presented in almost every textbook
and implemented. Unfortuantely, it is also the least efficient among the
power-of-two FFT, The radix-4 decomposes one Fourier transform into
four quarter-sized Fourier transform. It is much more
efficient than the radix-2 FFT, but it requires a power-of-four as the data length.
By combining the radix-2 and the radix-4 algorithms, one not only
removes the power-of-four limitation of the radix-4 alogorithm, but also
obtains the most efficient power-of-two FFT algorithm.

The split-radix FFT decomposes a Fourier transform

  X(k) = sum(n = 0 to N-1, x[n] * W**(n*k))

successively into one length N/2 radix-2 and two length N/4 radix-4 FFT

 X(2k) = sum(n = 0 to N/2 - 1, [x[n] + x(n + N/2)] * W**(2*n*k))
 X(4k + 1) = sum(n = 0 to N/4 - 1, [x[n] - x(n + N/2) - j(x[n] - x(n + 3*N/4))] * W(n)*W**(4*n*k))
 X(4k + 3) = sum(n = 0 to N/4 - 1, [x[n] - x(n + N/2) + j(x[n] - x(n + 3*N/4))] * W(n)*W**(4*n*k))

where W(n) = exp(-j2*PI*n/N) is called a twiddle factor

The detailed description of the algorithm (with bugs) can be found in

  H.V. Sorensen, M.T. Heideman, and C. S. Burrus, "On Computing the Split-Radix
  FFT,"IEEE Trans. Acoust., Speech, Signal Processing, pp. 152-200, Feb. 1986.

The implementation of the split-radix is similar to that of the radix-2
algorithm, except a smart index scheme is needed at each stage to determine
which nodes need to be decomposed further and which ones do not, since
a radix-2 decomposition is done at every stage while a radix-4 decomposition
is done at every other stage. This implementation uses an index scheme designed
by H.V. Sorensen, M.T. Heideman, and C. S. Burrus.

You can use this implementation for both floating and fixed point FFT and
inverse FFT computation by changing a compliler constant FIXED_POINT
in file fft.h. Some addtional information can be found in following sections..

 Usage
 Efficiency
 Examples:

>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
Usage:
>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

For floating point, you have to undefine the constant FIXED_POINT and
use typedef to define the trigonomy function and input data types.

For fixed point, the constant FIXED_POINT must be defined and a 32 bit
representation of both input data and trigonomy function data are required.
Furthermore, you have three choices to conduct fixed-point FFT.
If you use the algorithm in both Microsoft C and I86 hardware environment, you
should use an assemblyline implementation. To do this, you go to the file
himul32.cpp and define constants MICROSOFTC_HIMUL32 and I86_HIMUL3.
If you use the algorithm only in the Microsoft C environment,
a Microsoft specific implementation should be used. You do this by
going to the file himul32.cpp to undefine I86_HIMUL3, and
define MICROSOFTC_HIMUL32.
In any other situation, a stric C implementation should be used by
undefining both MICROSOFTC_HIMUL32 and I86_HIMUL3.

To use the algorithm, you need to constrcut an fft_info object

 fft_info* my_fft_info = new_fft_info(log2Size);

If you have a real data input data[] of size 2**(log2Size + 1), you
use

 do_real_fft(my_fft_info, size, data);

The positive half frequency Fourier transform will be returned, in addition
to the first and last elements of the transform that are stored in the
first and second elements of the data array. If the data[] array is the
Fourier transform of a real data input, you can also conduct the inverse
Fourier transform to get the real data input by

 do_real_ifft(my_fft_info, size, data);

Finally, you should remember to remove my_fft_info object using

 delete_fft_info(my_fft_info)

when you are done with FFT.

>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
Efficiency:
>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

TIME: Let M = log2Size be the power-of-two length and N = 2**M be the complex
  FFT input size. The following formulas give the comutation requirements
  of the split-radix FFT algorithm.

  Multiplications = (4/3)*M*N - (35/9)N + 4
  Adds   = (8/3)*M*N - (16/9)N + 2

  On a 266 MHz 64 MB RAM Pentium, using a release build,
  500 runs of paired 256 point complex (512  point real) input FFT
  (forward + inverse) have the following time counts:

Floating Point:
  Real:  0.160 sec.
  Complex: 0.120 sec.


Fixed Point:
  Assembly:  Real 0.170 sec, Complex 0.140 sec.
  Microsoft C: Real 0.250 sec, Complex 0.240 sec.
  Stric C:  Real 0.540 sec, Complex 0.441 sec.


SPACE: The computation is done in place so that there is no dynamic memory allocation
  in do_fft (do_real_fft) and do_ifft (do_real_ifft) call, except some
  temporary local variables.

  The memory space is needed in fft_info struct to store the cosine (sine) tales
  butterfly index table, and bit reverse table. Specifically,

  cosine(sine) tables: 3*N (half can be saved if we only save cosine or sine tables)
  butterfly index table: < N
  bitrever index table: N

>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
Example:
>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

The examples of running this program is given in a compiled out main()
in fft.cpp. You can run this program by compiling the fft.h, fft.cpp, and
himul32.cpp with main() compiled.

*/ /* end of the comment block*/

#ifdef __cplusplus
extern "C"
{
#endif


#define DATA_BITS 16   /*number of significant bits in fftdata*/
#define HAMMING_DATA_BITS 15

  typedef fftdata     trigonomydata;

  typedef struct
  {
    /* fft log length */
    unsigned  m_logLength;

    /* fft data length */
    unsigned  m_length;

    unsigned  *m_bitreverseTbl;

    /* L shaped butterfly index table */
    unsigned  *m_butterflyIndexTbl;

    /* sine and cosine tables */
    trigonomydata *m_sin1Tbl;
    trigonomydata *m_cos1Tbl;
    trigonomydata *m_cos2Tbl;
    trigonomydata *m_sin2Tbl;
    trigonomydata *m_sin3Tbl;
    trigonomydata *m_cos3Tbl;


  }
  srfft;

  typedef struct
  {
    srfft   *m_srfft;

    fftdata *real;
    fftdata *imag;

    asr_uint32_t  size;
    asr_uint32_t  size2;

  }
  fft_info;


  /****************************************************************
   new_srfft: create srfft object

   Parameters:
    log2Length -- the power-of-two length of the complex FFT

   Returns:
    the created srfft object
  ****************************************************************/
  srfft* new_srfft(unsigned log2Length);


  /****************************************************************
   delete_srfft: release all the memory allocated to srfft object

   Parameters:
    pthis -- a pointer to a srfft object created by new_srfft

   Returns:
    none
  ****************************************************************/
  void delete_srfft(srfft* pthis);


  /******************************************************************
   do_real_fft conducts a forward FFT of a real data array using
   the split-radix algorithm

   Parameters:
    pthis  -- a pointer to the srfft object
    length -- length of data array, must be a power-of-2 length
       and must be equal to 2**(log2Length + 1)
    data   -- A real data array, overwritten by the positive frequency
       half of its Fourier transform.
       Data[0] and data[1] store the first and last
       component of the the Fourier transform. After that, the
       even elements store the real component, while the odd
       elements store the imaginary component of the transform.
   Return:
    none
  *********************************************************************/
  void do_real_fft(srfft* pthis, unsigned length, fftdata* data);


  /******************************************************************
   do_real_ifft conducts an inverse FFT of a real data array's FFT using
   the split-radix algorithm

   Parameters:
    pthis  -- a pointer to the srfft object
    length -- length of data array, must be a power-of-2 length
       and must be equal to 2**(log2Length + 1)
    data   -- FFT of an real data array, overwritten by the real data array.
       For input, data[0] and data[1] store the first and last
       component of the the Fourier transform. After that,
       the even elements store the real component of the transform,
       while the odd elements store the imaginary component of
       the transform.
   Return:
    none
  *********************************************************************/
  void do_real_ifft(srfft* pthis, unsigned length, fftdata* data);


  /* >>>>>>>>>>>>>> Private Methods <<<<<<<<<<<<<<<<<<<<<<<<< */

  /******************************************************************
   allocate_butterfly_index uses an index scheme developed by
   Sorensen, Heideman, and Burrus to determine which L shaped
   butterfiles needs to be computed at each stage and
   store these indexes into a table

   Parameters:
    pthis -- a pointer to the srfft object

   Returns:
    none
  *********************************************************************/
  void allocate_butterfly_tbl(srfft* pthis);

  /******************************************************************
   allocate_trigonomy_tbl allocates trigonomy function tables
   for FFT computation

   Parameters:
    pthis -- a pointer to the srfft object

   Returns:
    none
  *********************************************************************/
  void allocate_trigonomy_tbl(srfft* pthis);

  /******************************************************************
   allocate_bitreverse_tbl() allocates bit reverse index table

   Parameters:
    pthis -- a pointer to the srfft object

   Returns:
    none
  *********************************************************************/
  void allocate_bitreverse_tbl(srfft* pthis);

#ifdef __cplusplus
}
#endif

#endif
