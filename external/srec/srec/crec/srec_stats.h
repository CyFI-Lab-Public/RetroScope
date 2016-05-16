/*---------------------------------------------------------------------------*
 *  srec_stats.h  *
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

#ifndef __SREC_STATS_H__
#define __SREC_STATS_H__

#include "srec.h"
#include "astar.h"
#include "word_lattice.h"

/*--------------------------------------------------------------------------*
 *                                                                          *
 * stats                                                                    *
 *                                                                          *
 *--------------------------------------------------------------------------*/

#define SREC_STATS_ACTIVE

#ifndef SREC_STATS_ACTIVE
#define SREC_STATS_CLEAR()
#define SREC_STATS_SHOW()
#define SREC_STATS_UPDATE(REC)
#define SREC_STATS_UPDATE_ASTAR(AsTaR)
#define SREC_STATS_INC_STOKEN_REPRUNES(K)
#define SREC_STATS_INC_FTOKEN_REPRUNES(K)
#define SREC_STATS_INC_WTOKEN_REPRUNES(K)

#else

#define SREC_STATS_CLEAR()  srec_stats_clear()
#if defined(__vxworks) && defined(NDEBUG)
#define SREC_STATS_SHOW()
#else
#define SREC_STATS_SHOW()   srec_stats_show()
#endif
#define SREC_STATS_UPDATE(ReC) srec_stats_update(ReC,0)
#define SREC_STATS_UPDATE_ASTAR(AsTaR) srec_stats_update_astar(AsTaR)
#define SREC_STATS_INC_STOKEN_REPRUNES(K) srec_stats_inc_stoken_reprunes(K)
#define SREC_STATS_INC_FTOKEN_REPRUNES(K) srec_stats_inc_ftoken_reprunes(K)
#define SREC_STATS_INC_WTOKEN_REPRUNES(K) srec_stats_inc_wtoken_reprunes(K)
#define SREC_STATS_INC_AWTOKEN_REPRUNES(K) srec_stats_inc_awtoken_reprunes(K)
#define SREC_STATS_INC_BAD_BACKTRACES() srec_stats_inc_bad_backtraces()
#define SREC_STATS_INC_FORCED_UPDATES() srec_stats_inc_forced_updates()

void srec_stats_clear(void);
void srec_stats_show(void);
void srec_stats_update(srec* rec, char* msg);
void srec_stats_inc_stoken_reprunes(int n);
void srec_stats_inc_ftoken_reprunes(int n);
void srec_stats_inc_wtoken_reprunes(int n);
void srec_stats_inc_awtoken_reprunes(int n);
void srec_stats_update_astar(AstarStack* stack);
void srec_stats_inc_bad_backtraces();
void srec_stats_inc_forced_updates();


#endif

#endif
