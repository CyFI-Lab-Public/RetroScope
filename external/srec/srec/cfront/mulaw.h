/*---------------------------------------------------------------------------*
 *  mulaw.h  *
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
**  The mu-law decoding table
**  Published internally by Alan Christie, Dragon UK.
*/

#ifdef SET_RCSID
static const char mulaw_h[] = "$Id: mulaw.h,v 1.2.10.3 2007/08/31 17:44:52 dahan Exp $";
#endif

static const short u_law[256] =
  {
    -8031, -7775, -7519, -7263, -7007, -6751, -6495, -6239, /* u-Law Table */
    -5983, -5727, -5471, -5215, -4959, -4703, -4447, -4191,
    -3999, -3871, -3743, -3615, -3487, -3359, -3231, -3103, /* To Convert  */
    -2975, -2847, -2719, -2591, -2463, -2335, -2207, -2079, /* PEB Data to */
    -1983, -1919, -1855, -1791, -1727, -1663, -1599, -1535, /* Linear for  */
    -1471, -1407, -1343, -1279, -1215, -1151, -1087, -1023, /* Subsequent  */
    -975,  -943,  -911,  -879,  -847,  -815,  -783,  -751, /* Processing  */
    -719,  -687,  -655,  -623,  -591,  -559,  -527,  -495,
    -471,  -455,  -439,  -423,  -407,  -391,  -375,  -359,
    -343,  -327,  -311,  -295,  -279,  -263,  -247,  -231,
    -219,  -211,  -203,  -195,  -187,  -179,  -171,  -163,
    -155,  -147,  -139,  -131,  -123,  -115,  -107,   -99,
    -93,   -89,   -85,   -81,   -77,   -73,   -69,   -65,
    -61,   -57,   -53,   -49,   -45,   -41,   -37,   -33,
    -30,   -28,   -26,   -24,   -22,   -20,   -18,   -16,
    -14,   -12,   -10,    -8,    -6,    -4,    -2,     0,
    8031,  7775,  7519,  7263,  7007,  6751,  6495,  6239,
    5983,  5727,  5471,  5215,  4959,  4703,  4447,  4191,
    3999,  3871,  3743,  3615,  3487,  3359,  3231,  3103,
    2975,  2847,  2719,  2591,  2463,  2335,  2207,  2079,
    1983,  1919,  1855,  1791,  1727,  1663,  1599,  1535,
    1471,  1407,  1343,  1279,  1215,  1151,  1087,  1023,
    975,   943,   911,   879,   847,   815,   783,   751,
    719,   687,   655,   623,   591,   559,   527,   495,
    471,   455,   439,   423,   407,   391,   375,   359,
    343,   327,   311,   295,   279,   263,   247,   231,
    219,   211,   203,   195,   187,   179,   171,   163,
    155,   147,   139,   131,   123,   115,   107,    99,
    93,    89,    85,    81,    77,    73,    69,    65,
    61,    57,    53,    49,    45,    41,    37,    33,
    30,    28,    26,    24,    22,    20,    18,    16,
    14,    12,    10,     8,     6,     4,     2,     0
  };
  
#define MULAW_TO_LINEAR(X) u_law[(X)]
