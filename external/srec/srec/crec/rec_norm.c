/*---------------------------------------------------------------------------*
 *  rec_norm.c  *
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


#include <stdlib.h>
#ifndef _RTT
#include "pstdio.h"
#endif
#include <limits.h>
#include <math.h>
#include <string.h>
#include "passert.h"

#include "c42mul.h"
#include "portable.h"

#include "../clib/fpi_tgt.inl"

int inherit_recognition_statistics(utterance_info *utt, multi_srec *recm,
                                   int norm_dim)
{
  /* inherit_recognition_stats use a callback function mechanism, */
  frameID speech_start, speech_end;
  multi_srec_get_speech_bounds(recm, &speech_start, &speech_end);
  swicms_update(utt->gen_utt.swicms, speech_start, speech_end);
  return (True);
}
