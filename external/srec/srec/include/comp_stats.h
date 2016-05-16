/*---------------------------------------------------------------------------*
 *  comp_stats.h  *
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

#ifndef __COMP_STATS_H__
#define __COMP_STATS_H__




#include <stdlib.h>
#include <stdio.h>

#include "pstdio.h"

#ifdef _WIN32
#include <windows.h>
typedef __int64 CS_TIME;
typedef __int64 CS_ACC_TIME;
#else
typedef clock_t CS_TIME;
typedef unsigned long CS_ACC_TIME;
#endif

#ifdef __vxworks
/*
 * the reason to rename the functions is:
 * Xanavi project required to combine S2G and Solo together, Solo has the same API
 * duplicate function names are not allowed in VxWorks
 */
#define init_comp_stats init_comp_stats_esr
#define dump_comp_stats dump_comp_stats_esr

#define init_cs_clock   init_cs_clock_esr
#define print_cs_clock  print_cs_clock_esr

#define start_cs_clock  start_cs_clock_esr
#define end_cs_clock    end_cs_clock_esr

#define reset_cs_clock  reset_cs_clock_esr
#define reset_cs_clock  reset_cs_clock_esr
#define make_cs_clock   make_cs_clock_esr

#endif

/**
 * @todo document
 */
typedef struct CS_CLOCK_t
{
  CS_TIME last;
  CS_ACC_TIME total_time;
  double clocks_per_msec;
  int ncalls;
  int item_count;
}
CS_CLOCK;

/**
 * @todo document
 */
typedef struct COMP_STATS_t
{
  CS_CLOCK overall_search;
  CS_CLOCK models;
  CS_CLOCK fsm_to_hmm;
  CS_CLOCK hmm_to_fsm;
  CS_CLOCK internal_hmm;
  CS_CLOCK epsilon;
  CS_CLOCK prune;
  CS_CLOCK front_end;
  CS_CLOCK word_lookup;
  CS_CLOCK word_addition;
  float total_time;   /*in seconds*/
  CS_CLOCK astar;
}
COMP_STATS;


void reset_cs_clock(CS_CLOCK *clock);
void init_cs_clock(CS_CLOCK *c);
CS_CLOCK *make_cs_clock(void);

#if defined(__cplusplus) && !defined(_ASCPP)
extern "C"
{
#endif
  COMP_STATS *init_comp_stats(void);
  void start_cs_clock(CS_CLOCK *clock);
  void end_cs_clock(CS_CLOCK *c, int count);
#if defined(__cplusplus) && !defined(_ASCPP)
}
#endif

void print_cs_clock(CS_CLOCK *c, float num_seconds, PFile* fp, char *prompt, char *item_name);
void dump_comp_stats(COMP_STATS *c, PFile* fp);

#if USE_COMP_STATS
#if defined(__cplusplus) && !defined(_ASCPP)
extern "C"
{
#endif
  extern COMP_STATS *comp_stats;
#if defined(__cplusplus) && !defined(_ASCPP)
}
#endif
#define init_comp_stats1()        init_comp_stats()
#define reset_cs_clock1( CLK)     reset_cs_clock( CLK)
#define init_cs_clock1( CLK)      init_cs_clock( CLK)
#define make_cs_clock1()          make_cs_clock()
#define start_cs_clock1( CLK)     start_cs_clock( CLK)
#define end_cs_clock1( CLK,CNT)   end_cs_clock( CLK,CNT)
#define print_cs_clock1( CLK, NS,FP,PR,IN)  print_cs_clock( CLK, NS,FP,PR,IN)
#define dump_comp_stats1( CS,FP)  dump_comp_stats( CS,FP)

#else /* not USE_COMP_STATS */

#define init_comp_stats1()
#define reset_cs_clock1( CLK)
#define init_cs_clock1( CLK)
#define make_cs_clock1()
#define start_cs_clock1( CLK)
#define end_cs_clock1( CLK,CNT)
#define print_cs_clock1( CLK, NS,FP,PR,IN)
#define dump_comp_stats1( CS,FP)
#endif

#endif
