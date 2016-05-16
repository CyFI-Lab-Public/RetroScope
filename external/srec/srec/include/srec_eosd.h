/*---------------------------------------------------------------------------*
 *  srec_eosd.h  *
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

#ifndef __SREC_EOSD_H__
#define __SREC_EOSD_H__

#include"sizes.h"
/* all values <=0 will continue recognition. >0 stops it */
typedef enum { VALID_SPEECH_NOT_YET_DETECTED = -1, VALID_SPEECH_CONTINUING = 0, SPEECH_ENDED, SPEECH_ENDED_WITH_ERROR, SPEECH_TOO_LONG, SPEECH_MAYBE_ENDED, SPEECH_ENDED_BY_LEVEL_TIMEOUT } EOSrc;

typedef struct srec_eos_detector_parms_t
{
  costdata eos_costdelta;
  costdata opt_eos_costdelta;

  frameID endnode_timeout;
  frameID optendnode_timeout;
  frameID internalnode_timeout;
  frameID inspeech_timeout;
}
srec_eos_detector_parms;

typedef struct srec_eos_detector_state_t
{
  frameID endnode_frmcnt;
  frameID optendnode_frmcnt;
  frameID internalnode_frmcnt;
  frameID inspeech_frmcnt;

  nodeID  internalnode_node_index;
}
srec_eos_detector_state;

void srec_eosd_allocate(srec_eos_detector_parms** eosd_parms,
                        int eos_costdelta,
                        int opt_eos_costdelta,
                        int endnode_timeout,
                        int optendnode_timeout,
                        int internalnode_timeout,
                        int inspeech_timeout);

void srec_eosd_destroy(srec_eos_detector_parms* eosd_parms);

void srec_eosd_state_reset(srec_eos_detector_state* eosd_state);
EOSrc srec_check_end_of_speech(srec_eos_detector_parms* eosd_parms, srec* rec);
EOSrc srec_check_end_of_speech_end(srec* rec);

#endif
