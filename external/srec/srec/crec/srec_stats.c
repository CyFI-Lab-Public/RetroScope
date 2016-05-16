/*---------------------------------------------------------------------------*
 *  srec_stats.c  *
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

#include"srec_stats.h"
#include "passert.h"
#include "portable.h"


#ifdef SREC_STATS_ACTIVE

typedef struct
{
  int num_fsmarc_tokens;
  int num_fsmnode_tokens;
  int num_word_tokens;
  int num_altword_tokens, num_altword_token_batches;

  int num_astar_active_parps;
  int num_astar_complete_parps;
  int num_astar_parps_in_use;

  int num_fsmarc_token_reprunes;
  int num_fsmnode_token_reprunes;
  int num_word_token_reprunes;
  int num_altword_token_reprunes;
  int num_bad_backtraces;
  int num_forced_updates;

}
srec_stats;

srec_stats my_srec_stats;

#define MAX_IN_SAMPLE(MaX,SamPle) \
  if((MaX)<(SamPle)) MaX = (SamPle);

void srec_stats_clear()
{
  memset(&my_srec_stats, 0, sizeof(my_srec_stats));
}

void srec_stats_show()
{
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage(
    L("SREC STATS: FWD tokens s %d f %d w %d aw %d // ASTAR parps a %d c %d t %d // REPRUNES s %d f %d w %d aw %d bbt %d fcu %d\n"),
    my_srec_stats.num_fsmarc_tokens,
    my_srec_stats.num_fsmnode_tokens,
    my_srec_stats.num_word_tokens,
    my_srec_stats.num_altword_tokens,
    my_srec_stats.num_astar_active_parps,
    my_srec_stats.num_astar_complete_parps,
    my_srec_stats.num_astar_parps_in_use,
    my_srec_stats.num_fsmarc_token_reprunes,
    my_srec_stats.num_fsmnode_token_reprunes,
    my_srec_stats.num_word_token_reprunes,
    my_srec_stats.num_altword_token_reprunes,
    my_srec_stats.num_bad_backtraces,
    my_srec_stats.num_forced_updates
  );
#endif
}

void srec_stats_update(srec* rec, char* msg)
{
  int i;
  asr_int16_t num;
  fsmnode_token* ftoken;
  fsmarc_token* stoken;
  word_token* wtoken;
  altword_token* awtoken;
  asr_int16_t numb;
  stokenID st_index;
  ftokenID ft_index;
  wtokenID wt_index;

  if (msg) PLogMessage ( msg );
  /* state tokens */
  st_index = rec->active_fsmarc_tokens;
  for (num = 0; st_index != MAXstokenID; st_index = stoken->next_token_index)
  {
    stoken = &rec->fsmarc_token_array[st_index];
    num++;
  }
  if (msg) PLogMessage ( " stokens %d", num );
  MAX_IN_SAMPLE(my_srec_stats.num_fsmarc_tokens, num);

  /* fsmnode tokens */
  ft_index = rec->active_fsmnode_tokens;
  for (num = 0 ; ft_index != MAXftokenID; ft_index = ftoken->next_token_index)
  {
    ftoken = &rec->fsmnode_token_array[ft_index];
    num++;
  }
  if (msg) PLogMessage ( " ftokens %d", num );
  MAX_IN_SAMPLE(my_srec_stats.num_fsmnode_tokens, num);

  /* word tokens */
  for (i = 0, num = 0; i < rec->current_search_frame; i++)
  {
    wt_index = rec->word_lattice->words_for_frame[i];
    for (; wt_index != MAXwtokenID; wt_index = wtoken->next_token_index)
    {
      wtoken = &rec->word_token_array[wt_index];
      num++;
    }
  }
  if (msg) PLogMessage ( " wtokens %d", num );
  MAX_IN_SAMPLE(my_srec_stats.num_word_tokens, num);

  /* altword tokens */
  for (num = 0, awtoken = rec->altword_token_freelist; awtoken; awtoken = awtoken->next_token)
    num++;
  num = rec->altword_token_array_size - num;
  for (numb = 0, i = 0; i < rec->altword_token_array_size; i++)
    if (rec->altword_token_array[i].next_token == AWTNULL)
      numb++;
  numb--; /* foreach tail, there is a head, remove the freelist head pointer */
  if (msg) PLogMessage ( " awtokens %d/%d", num, numb );
  MAX_IN_SAMPLE(my_srec_stats.num_altword_tokens, num);
  MAX_IN_SAMPLE(my_srec_stats.num_altword_token_batches, numb);
  if (msg) PLogMessage ( "\n" );
}

void srec_stats_update_astar(AstarStack* stack)
{
  int num_parps_in_use;
  partial_path *parp;
  /* active parps are the leaves of the tree, still being extended */
  MAX_IN_SAMPLE(my_srec_stats.num_astar_active_parps,
                stack->num_active_paths);
  /* complete parps are the leaves, for completed paths */
  MAX_IN_SAMPLE(my_srec_stats.num_astar_complete_parps,
                stack->num_complete_paths);
  
  num_parps_in_use = stack->partial_path_array_size;
  for (parp = stack->free_parp_list; parp; parp = parp->next)
    num_parps_in_use--;

  MAX_IN_SAMPLE(my_srec_stats.num_astar_parps_in_use, num_parps_in_use);
}

void srec_stats_inc_stoken_reprunes(int n)
{
  my_srec_stats.num_fsmarc_token_reprunes   += n;
}
void srec_stats_inc_ftoken_reprunes(int n)
{
  my_srec_stats.num_fsmnode_token_reprunes += n;
}
void srec_stats_inc_wtoken_reprunes(int n)
{
  my_srec_stats.num_word_token_reprunes    += n;
}
void srec_stats_inc_awtoken_reprunes(int n)
{
  my_srec_stats.num_altword_token_reprunes += n;
}
void srec_stats_inc_bad_backtraces()
{
  my_srec_stats.num_bad_backtraces++;
}
void srec_stats_inc_forced_updates()
{
  my_srec_stats.num_forced_updates++;
}
#endif


