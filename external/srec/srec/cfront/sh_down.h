/*---------------------------------------------------------------------------*
 *  sh_down.h  *
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

#ifndef _SH_DOWN_INL_
#define _SH_DOWN_INL_

#include <limits.h>

#include "fronttyp.h"
#include "setting.h"
#include "portable.h"

#define SHIFT_DOWN(X,S) ((S) > 0 ? shift_down_inline ((X), (S)) : (X))
#define SHIFT_UP(X,S) ((S) > 0 ? shift_up_inline ((X), (S)) : (X))

#define COND_SHIFT_DOWN(X,S) (SHIFT_DOWN(X,S))
#define COND_SHIFT_UP(X,S) (SHIFT_UP(X,S))

static PINLINE int fixed_point_convert(float xx, int shift);
static PINLINE int shift_up_inline(int value, unsigned int shift);
static PINLINE int shift_down_inline(int value, unsigned int shift);
static PINLINE int fixed_round(float value);

#define TRUNCATE_ON_SHIFT   1

static PINLINE int shift_up_inline(int value, unsigned int shift)
{
  /* Shift up using bit operations with max limit */
  int temp, retval;

  ASSERT(shift > 0);
  if (value > 0)
    temp = value;
  else
    temp = -value;

  retval = temp << shift;

  if ((retval > (int)LONG_MAX) || (retval < temp)) /* TODO: max_val if LONG_MAX, overflow won't be detected */
    retval = (int)LONG_MAX;
  if (value > 0)
    return retval;
  else
    return (-retval);
}


static PINLINE int shift_down_inline(int value, unsigned int shift)
/* Shift down using bit operations with rounding */
{
  if (shift-- == 0)
    return (value);
  if (value >= 0)
    return (((value >> shift) + 1) >> 1);
  else
    return (-((((-value) >> shift) + 1) >> 1));
}

static PINLINE int fixed_point_convert(float xx, int shift)
{
  float   scaled_val;

  ASSERT(shift >= 0);
  scaled_val = xx * (0x01 << shift);
  if (scaled_val >= 0)
    if (scaled_val > LONG_MAX)
      return (LONG_MAX);
    else
      return ((int)(scaled_val + 0.5));
  else
    if (scaled_val < -LONG_MAX)
      return (-LONG_MAX);
    else
      return ((int)(scaled_val - 0.5));
}

static PINLINE int fixed_round(float value)
{
  if (value > 0)
    return ((int)(value + 0.5));
  else
    return ((int)(value - 0.5));
}

#endif
