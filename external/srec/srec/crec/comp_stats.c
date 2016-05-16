/*---------------------------------------------------------------------------*
 *  comp_stats.c  *
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



#include "buildopt.h"
#include "pstdio.h"
#include "passert.h"
#include <time.h>
#include "comp_stats.h"
#include "portable.h"
#include "PFile.h"

#ifdef SET_RCSID
static const char *rcsid = 0 ? (const char *) &rcsid : "$Id: compstats now";
#endif

#if defined(__cplusplus)
extern "C"
{
#endif
  COMP_STATS *comp_stats = NULL;
#if defined(__cplusplus)
}
#endif

/* create COMP_STATS object */

COMP_STATS *init_comp_stats(void)
{
  static COMP_STATS c;

  /*c = (COMP_STATS *) calloc( 1, sizeof( COMP_STATS ));*/
  /*c = (COMP_STATS *) NEW( COMP_STATS, L("crec.comp_stats")); */

  init_cs_clock(&c.overall_search);
  init_cs_clock(&c.models);
  init_cs_clock(&c.fsm_to_hmm);
  init_cs_clock(&c.internal_hmm);
  init_cs_clock(&c.hmm_to_fsm);
  init_cs_clock(&c.epsilon);
  init_cs_clock(&c.astar);
  init_cs_clock(&c.prune);
  init_cs_clock(&c.front_end);
  init_cs_clock(&c.word_lookup);
  init_cs_clock(&c.word_addition);
  c.total_time = 0;
  return &c;
}


void dump_comp_stats(COMP_STATS *cs, PFile* fp)
{
  if (getenv("HIDE_COMP_STATS"))
    return;
#if !defined(_WIN32) && !defined(__vxworks)
  if (clock() == (clock_t) - 1)
  {
    pfprintf(fp, "***WARNING: clock overrun!\n");
  }
#endif
  if (!cs) cs = comp_stats;

#ifdef SREC_ENGINE_VERBOSE_LOGGING
  pfprintf(fp, "Total Time %5.2f Seconds\n", cs->total_time);
#endif
  print_cs_clock(&cs->front_end, cs->total_time, fp, "Front end", "Frames");
  print_cs_clock(&cs->overall_search, cs->total_time, fp, "Total Search", "Frames");
  print_cs_clock(&cs->models, cs->total_time, fp, "   Models", "Models");
  print_cs_clock(&cs->internal_hmm, cs->total_time, fp, "   Internal HMM", "HMMs");
  print_cs_clock(&cs->fsm_to_hmm, cs->total_time, fp, "   FSM to HMM", "FSM_Nodes");
  print_cs_clock(&cs->prune, cs->total_time, fp, "   Prune", "HMM States");
  print_cs_clock(&cs->hmm_to_fsm, cs->total_time, fp, "   HMM to FSM", "HMMS");
  print_cs_clock(&cs->epsilon, cs->total_time, fp, "   Epsilon", "FSM_Nodes");
  print_cs_clock(&cs->astar, cs->total_time, fp, "   Astar", "Utterances");
  print_cs_clock(&cs->word_lookup, cs->total_time, fp, "   WordLookup", "Words");
  print_cs_clock(&cs->word_addition, cs->total_time, fp, "   WordAdd'tn", "Pronunciations");
  pfflush(fp);
}


void print_cs_clock(CS_CLOCK *c, float num_seconds, PFile* fp, char *prompt, char *item_name)
{
  if (c == NULL) return;
  /*  FPRINTF( fp, "%15.15s %8.2f.  Per Second of speech: %6.2f ms, %6.2f calls, %6.2f (%d) %s\n",
             prompt ? prompt : "",
             c->total_time/c->clocks_per_msec,
             (c->total_time/c->clocks_per_msec) / num_seconds,
      c->ncalls / num_seconds,
      c->item_count / num_seconds,
      c->item_count,
      item_name);*/
}


void start_cs_clock(CS_CLOCK *c)
{
  if (c == NULL) return;
#ifdef _WIN32
  {
    FILETIME dummy, kernelCPU, userCPU;
    GetThreadTimes(GetCurrentThread(), &dummy, &dummy, &kernelCPU,
                   &userCPU);
    c->last = kernelCPU.dwLowDateTime + ((__int64)kernelCPU.dwHighDateTime << 32) +
              userCPU.dwLowDateTime + ((__int64)userCPU.dwHighDateTime << 32);
  }
#elif defined(__vxworks)
  /* Should use a portable clock() */
  /* WxWorks: clock() always returns -1. VxWorks does not track per-task time or system idle time.
  There is no method of determining how long a task or the entire system has been doing work.
  tickGet( ) can be used to query the number of system ticks since system start.
  clock_gettime( ) can be used to get the current clock time.
  */
  c->last = tickGet();
#else
  c->last = clock();
#endif
}



void end_cs_clock(CS_CLOCK *c, int count)
{
  CS_TIME curr;
  if (c == NULL) return;
#ifdef _WIN32
  {
    FILETIME dummy, kernelCPU, userCPU;
    GetThreadTimes(GetCurrentThread(), &dummy, &dummy, &kernelCPU,
                   &userCPU);
    curr = kernelCPU.dwLowDateTime + ((__int64)kernelCPU.dwHighDateTime << 32) +
           userCPU.dwLowDateTime + ((__int64)userCPU.dwHighDateTime << 32);
  }
#elif defined(__vxworks)
  curr = tickGet();
#else
  curr = clock();
  if (curr == -1) return;     /* clock overrun */
#endif
  c->total_time += curr - c->last;
  c->last = curr;
  c->ncalls ++;
  c->item_count += count;
}


void reset_cs_clock(CS_CLOCK *c)
{
  if (c == NULL) return;
  c->ncalls = 0;
  c->total_time = 0;
  c->last = 0;
  c->item_count = 0;
}

void init_cs_clock(CS_CLOCK *c)
{
  if (c == NULL) return;
#if _WIN32
  c->clocks_per_msec = 10000.0;
#else
  c->clocks_per_msec = (double) CLOCKS_PER_SEC / 1000.0;
#endif
  reset_cs_clock(c);
}


CS_CLOCK *make_cs_clock(void)
{
  CS_CLOCK *c = (CS_CLOCK *) NEW(CS_CLOCK, L("crec.cs_clock"));
  init_cs_clock(c);
  return c;
}
