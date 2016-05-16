/*---------------------------------------------------------------------------*
 *  srec_sizes.h  *
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




#ifndef _h_srec_sizes_
#define _h_srec_sizes_

typedef asr_uint16_t costdata;  /*done as cost, so always >= 0*/
typedef asr_int32_t bigcostdata;          /*done as cost, so always >= 0*/
typedef asr_uint16_t miscdata;  /*for random small things*/
typedef asr_uint16_t labelID; /*16 bits is a bit overkill for this, but 8's not enough*/
typedef asr_uint16_t wordID;  /*for word index*/
typedef asr_uint16_t nodeID;  /*for FSM node index*/
typedef asr_uint16_t arcID;  /*for FSM arc index*/
typedef asr_uint16_t frameID;  /*for time frame*/
typedef asr_uint16_t stokenID;  /*for state token storage*/
typedef asr_uint16_t ftokenID;  /*for FSMnode token storage*/
typedef asr_uint16_t wtokenID;  /*for word token storage*/
typedef asr_uint16_t HMMID;  /*for HMMs*/
typedef asr_uint16_t modelID;  /*for models (HMM state distributions)*/

/*limits on each of the above sizes*/

#define MAXcostdata ((costdata)65535)
#define MAXbcostdata ((bigcostdata)2147483647)
#define FREEcostdata 0
#define MAXlabelID 65535
#define MAXwordID 65535
#define MAXnodeID 65535
#define MAXarcID 65535
#define MAXframeID ((frameID)65535)
#define MAXstokenID 65535
#define MAXftokenID 65535
#define MAXwtokenID 65535
#define MAXmodelID 65535
#define MAXHMMID 65535

#endif
