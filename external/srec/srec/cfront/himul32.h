/*---------------------------------------------------------------------------*
 *  himul32.h  *
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
////////////////////////////////////////////////////////////////////////////
//
//  FILE:         himul32.cpp
//
//  CREATED:   11-September-99
//
//  DESCRIPTION:  A multiplier returns most-significant 32 bits of the 64-bit
//      product of its two signed 32-bit integers
//
//
//
//
//  MODIFICATIONS:
// Revision history log
    VSS revision history.  Do not edit by hand.

    $NoKeywords: $

*/

/* do not use PPC. VxWorks defines the PPC in vxcpu.h */
#if defined(_PPC_)

/* Reads timebase register for a higher precision clock */

asm PINLINE int32 himul32(asr_int32_t factor1, asr_int32_T factor2)
{
  %   reg factor1;
  reg factor2;
  
  mulhw   r3, factor1, factor2  # place the high order 32 bits of the product in the return register r3
    }
    
#else
    
/******************************************************************
himul32 returns the most-significant 32 bits of the 64-bit
product of its two signed 32-bit integer arguments.
In other words, it's the exact value of the mathematical expression
floor( (factor1 * factor2) / 2**32 )
This is a platform-independent definition that needs to be
implemented in platform-specific ways.
    
Parameters:
factor1 -- first signed 32 bit integer
factor2 -- second signed 32 bit integer
    
Returns:
the most-significant 32 bits of the multiplication results
*********************************************************************/
    
#if COMPILER == C_MICROSOFT
    
#if TARGET_CPU == CPU_I86
    
PINLINE asr_int32_t himul32(asr_int32_t factor1, asr_int32_t factor2)
{
  asr_int32_t retval;
  /*
  // The x86 imul instruction, given a single 32-bit operand, computes
  // the signed 64-bit product of register EAX and that operand, into
  // the register pair EDX:EAX.  So we have to move the first factor into
  // EAX, then IMUL, then take the high 32 bits (in EDX) and move them
  // back to EAX (because that's where a function's return value is
  // taken from).
  */
  __asm {
    mov     eax, factor1
    imul    factor2
    mov     retval, edx
  }
  return retval;
}
    
#else /* TARGET_CPU != CPU_I86 */
    
    PINLINE asr_int32_t himul32(asr_int32_t factor1, asr_int32_t factor2)
    {
      union {
        __int64 full;
        struct
        {
          asr_int32_t lo;
          asr_int32_t hi;
        }
        pieces;
      } result;
    
      __int64 x = factor1;
      __int64 y = factor2;
      result.full = x * y;
      return result.pieces.hi;
    }
    
#endif /* TARGET_CPU == CPU_I86 */
    
#else  /* ~ COMPILER != C_MICROSOFT */
    
    /*** ANSI C ***/
    
    PINLINE asr_int32_t himul32(asr_int32_t factor1, asr_int32_t factor2)
    {
    
      asr_uint32_t x = (asr_uint32_t)factor1;
      asr_uint32_t y = (asr_uint32_t)factor2;
      asr_uint32_t xhi, xlo, yhi, ylo;
      asr_uint32_t hi, lo, mid;
      asr_uint32_t oldlo, carry;
      int sign = 0;
    
      if (factor1 < 0)
      {
        x = (asr_uint32_t) - factor1;
        sign = 1;
      }
      if (factor2 < 0)
      {
        y = (asr_uint32_t) - factor2;
        sign = 1 - sign;
      }
      xhi = x >> 16;       /* <= 2**15 */
      xlo = x & 0xffff;    /* <  2**16 */
      yhi = y >> 16;       /* <= 2**15 */
      ylo = y & 0xffff;    /* <  2**16 */
    
      lo = xlo * ylo;
      /*
      // xhi <= 2**15 and ylo <= 2**16-1, so
      // xhi * ylo <= 2**31 - 2**15.
      // Ditto for yhi * xlo, so their sum is
      // <= 2*32 - 2**16, and so the next line can't overflow.
      */
      mid = xhi * ylo + yhi * xlo;
      hi = xhi * yhi;
    
      /*
      // Now add the low part of mid to the high part of lo, and the
      // high part of mid to the low part of hi:
      //                    xxxxxxxx xxxxxxxx     lo
      //           xxxxxxxx xxxxxxxx              mid
      //  xxxxxxxx xxxxxxxx                       hi
      //  -----------------------------------
      //                    xxxxxxxx xxxxxxxx     lo
      //  xxxxxxxx xxxxxxxx                       hi
      // Note that folding mid into lo can cause a carry.  An old trick
      // for portable carry-detection applies:  if a and b are unsigned,
      // their sum overflows if and only if it's less than a (or b; can
      // check either one).
      */
    
      oldlo = lo;
      lo += mid << 16;
      carry = lo < oldlo;
    
      hi += carry + (mid >> 16);
    
      if (sign)
      {
        /*
        // Result must be negated, which is the same as taking the
        // complement and adding 1.  So there's a carry out of the low
        // half if and only if it's 0 now.
        */
        hi = ~hi;
        hi += lo == 0;
      }
    
      return (asr_int32_t)hi;
    }
    
#endif  /* ~ COMPILER == C_MICROSOFT */
    
    
#endif
