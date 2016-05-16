/*---------------------------------------------------------------------------*
 *  voicing.h  *
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



#ifndef __voicing_h
#define __voicing_h

#ifdef SET_RCSID
static const char voicing_h[] = "$Id: voicing.h,v 1.1.10.3 2007/08/31 17:44:53 dahan Exp $";
#endif


#include "hmm_type.h"

#define B0_HANG1 100
#define B0_HANG2 300
#define B0_RATE1 15     /* 256 * 0.06 */
#define B0_RATE2 38     /* 256 * 0.15 */
#define B1_RATE  26     /* 256 * 0.1 */
#define DYNAMIC_RANGE (70 << 8)   /* typical dynamic range */

/*  The following are internal constants used by the voicing detector program
*/
#define VOICE_MASK           0xfffffff0
#define VOICE_BIT            0x01L
#define QUIET_BIT            0x02L
#define FAST_VOICE_BIT       0x04L
#define BELOW_THRESHOLD_BIT  0x08L
#define REC_VOICE_BIT      0x10L
#define REC_QUIET_BIT      0x20L
#define REC_UNSURE_BIT       0x40L

#define VOICING_DATA(X)      ((X) & (VOICE_BIT | QUIET_BIT))
#define FAST_MATCH_DATA(X)   ((X) & (FAST_VOICE_BIT | QUIET_BIT))

#define FAST_BIT_SET(X)      ((X) & FAST_VOICE_BIT)
#define QUIET_BIT_SET(X)     ((X) & QUIET_BIT)
#define RECOGNIZER_QUIET(X)  ((X) & REC_QUIET_BIT)
#define SET_VOICING_CODES(X,C) (((X) & ~(REC_VOICE_BIT | REC_QUIET_BIT | REC_UNSURE_BIT)) | (C))

typedef featdata voicedata;

typedef struct
{
  int   b0;   /* background estimate, level 0 */
  int   b1;   /* background estimate, level 1 */
  int   s0;
  int   margin;
  int   fast_margin;
  int   quiet_margin;
  int   voice_duration; /* threshold for consecutive speech frames */
  int   quiet_duration; /* threshold for consecutive silence frames */
  int   count;
  long  sil_count;  /* no. of consecutive silence frames */
  long  fast_count;  /* no. of consecutive speech frames */
  long  speech_count;  /* no. of consecutive speech frames for barge-in */
  int   voice_status;  /* voicing decision */
}
voicing_info;


void init_voicing_analysis(voicing_info *voice);
long voicing_analysis(voicing_info *voice, voicedata enval, int* log);

#endif
