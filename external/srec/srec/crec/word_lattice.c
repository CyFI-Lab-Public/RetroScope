/*---------------------------------------------------------------------------*
 *  word_lattice.c  *
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

#ifndef _RTT
#include "pstdio.h"
#endif
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "passert.h"
#include "portable.h"


#include "portable.h"

#include "hmm_desc.h"
#include "utteranc.h"
#include "hmmlib.h"

#include "srec_sizes.h"
#include "search_network.h"
#include "srec.h"
#include "word_lattice.h"
#include "astar.h"
#include "srec_stats.h"
#include "srec_results.h"

#define PRINT_WORD_LATTICE        0
#define PRINT_SEARCH_DETAILS      0

#define TRUE_KILL_WTOKEN(WT) { WT.cost = MAXcostdata; \
    WT.word = MAXwordID;  \
    WT.end_time = MAXframeID; \
    WT.end_node = MAXnodeID; \
    WT.backtrace = MAXwtokenID; \
  }

srec_word_lattice *allocate_word_lattice(frameID max_frames)
{
  srec_word_lattice *wl;

  wl = (srec_word_lattice*) CALLOC_CLR(1, sizeof(srec_word_lattice), "search.word_lattice.base");
  wl->max_frames = max_frames;
  wl->words_for_frame = (wtokenID*) CALLOC_CLR(max_frames, sizeof(wtokenID), "search.word_lattice.words");

  wl->whether_sorted = (asr_int16_t*)CALLOC_CLR(max_frames,  sizeof(asr_int16_t), "search.word_lattice.sflag");

  return wl;
}

void destroy_word_lattice(srec_word_lattice* wl)
{
  FREE(wl->words_for_frame);
  FREE(wl->whether_sorted);
  FREE(wl);
}

void initialize_word_lattice(srec_word_lattice* wl)
{
  frameID ifr;
  for (ifr = 0; ifr < wl->max_frames; ifr++)
  {
    wl->words_for_frame[ifr] = MAXwtokenID;
    wl->whether_sorted[ifr] = 0;
  }
}

costdata lattice_best_cost_to_frame(srec_word_lattice *wl, word_token* word_token_array, frameID ifr)
{
  int sanity_counter = 0;
  costdata best_cost = MAXcostdata;
  wtokenID wtoken_index = wl->words_for_frame[ ifr];
  while (wtoken_index != MAXwtokenID)
  {
    word_token* wtoken = &word_token_array[wtoken_index];
    if (sanity_counter++ > 200) return MAXcostdata;
    wtoken_index = wtoken->next_token_index;
    if (best_cost > wtoken->cost)
      best_cost = wtoken->cost;
  }
  return best_cost;
}

void lattice_add_word_tokens(srec_word_lattice *wl, frameID frame,
                             wtokenID word_token_list_head)
{
  if (frame >= wl->max_frames)
  {
    log_report("lattice_add_word_tokens: max_frame not big enough\n");
    ASSERT(0);
  }
  wl->words_for_frame[frame] = word_token_list_head;
}

void print_word_token_backtrace(srec* rec, wtokenID wtoken_index, char* tail)
{
  char* null = "NULL", *p;
  char iwttime[8] = { 0, 0, 0, 0, 0, 0, 0, 0};
  bigcostdata cost;
  bigcostdata cost_for_word;
  word_token *wtoken, *last_wtoken;

  last_wtoken = NULL;
  while (wtoken_index != MAXwtokenID)
  {
    wtoken = &rec->word_token_array[wtoken_index];
    if (wtoken->word < rec->context->olabels->num_words)
      p = rec->context->olabels->words[wtoken->word];
    else
      p = null;
    ASSERT(!last_wtoken || last_wtoken->end_time > wtoken->end_time);
    ASSERT(rec->accumulated_cost_offset[ wtoken->end_time] != 0);
    cost = wtoken->cost + rec->accumulated_cost_offset[ wtoken->end_time];

    if (wtoken->backtrace != MAXwtokenID)
    {
      word_token* next_wtoken = &rec->word_token_array[wtoken->backtrace];
      cost_for_word = cost - next_wtoken->cost - rec->accumulated_cost_offset[ next_wtoken->end_time];
    }
    else
    {
      cost_for_word = cost;
    }
    sprintf(iwttime, "/%d", WORD_TOKEN_GET_WD_ETIME(wtoken) );
    PLogMessage (" (%d W%d %s cost=%d/%d/%d time=%d%s node=%d)", wtoken_index, wtoken->word, p, wtoken->cost, cost, cost_for_word, wtoken->end_time, iwttime, wtoken->end_node);
    fflush(stdout);
    ASSERT(wtoken->backtrace != wtoken_index);
    wtoken_index = wtoken->backtrace;
    last_wtoken = wtoken;
  }
  PLogMessage (tail);
}

int sprint_bword_token_backtrace(char* buf, int buflen, srec* rec, wtokenID wtoken_index)
{
  char* null = "NULL", *p;
  char *pbuf = buf;
  *pbuf = 0;

  while (wtoken_index != MAXwtokenID)
  {
    word_token* wtoken = &rec->word_token_array[wtoken_index];
    p = null;
    if (wtoken->word < rec->context->olabels->num_words)
      p = rec->context->olabels->words[wtoken->word];
    ASSERT(pbuf + strlen(p) + 1 < buf + buflen);
    pbuf += sprintf(pbuf, "%s ", p);
    ASSERT(wtoken->backtrace != wtoken_index);

    wtoken_index = wtoken->backtrace;
  }
  if (pbuf > buf && *(pbuf - 1) == ' ') *(pbuf - 1) = 0;
  return 0;
}

#define ERROR_TRANSCRIPTION_TOO_LONG -1

ESR_ReturnCode sprint_word_token_backtraceByWordID(wordID* wordIDs, size_t* len, srec* rec, wtokenID wtoken_index)
{
  size_t i, currentLen = 0;
  ESR_ReturnCode rc;
  word_token* wtoken;

#if PRINT_SEARCH_DETAILS
  printf("in get backtrace wtoken %d\n", wtoken_index);
#endif

  while (wtoken_index != MAXwtokenID)
  {
    if (*len <= currentLen)
    {
      rc = ESR_BUFFER_OVERFLOW;
      PLogError(ESR_rc2str(rc));
      *len = currentLen + 1;
      goto CLEANUP;
    }
    wtoken = &rec->word_token_array[wtoken_index];
    wordIDs[currentLen] = wtoken->word;
    ++currentLen;

    if (wtoken_index == wtoken->backtrace)
    {
      *len = 0;
      PLogError("Result is loopy, rejecting");
      return ESR_INVALID_STATE;
    }
    wtoken_index = wtoken->backtrace;
  }

  /* reverse the order */
  for (i = 0; i < currentLen / 2; i++)
  {
    wordID tmp = wordIDs[i];
    wordIDs[i] = wordIDs[(currentLen-1-i)];
    wordIDs[(currentLen-1-i)] = tmp;
  }
  /* strip the pau/pau2 markers */
  if (currentLen >= 1 && wordIDs[0] == rec->context->beg_silence_word)
  {
    for (i = 0; i < currentLen - 1; i++)
      wordIDs[i] = wordIDs[i+1];
    currentLen--;
  }
  if (currentLen >= 1 && wordIDs[currentLen-1] == rec->context->end_silence_word)
    currentLen--;
  wordIDs[currentLen] = MAXwordID;
  *len = currentLen;

  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

int sprint_word_token_backtrace(char *transcription, int len, srec* rec, wtokenID wtoken_index)
{
  char *w;
  char *from_p;
  char *to_p;
  char *end;
  char *tr_end = transcription;
  int wlen;

#define SHOW_END_TIMES 1
#if SHOW_END_TIMES
  char buf[256/*64*/];
#endif

  *transcription = 0;

#if PRINT_SEARCH_DETAILS
  printf("in get backtrace wtoken %d\n", wtoken_index);
#endif

  while (wtoken_index != MAXwtokenID)
  {
    word_token* wtoken = &rec->word_token_array[wtoken_index];
#if PRINT_SEARCH_DETAILS
    printf("got token %d word %d\n", wtoken_index, wtoken->word);
#endif

    w = "NULL";
    if (wtoken->word < rec->context->olabels->num_words)
      w = rec->context->olabels->words[wtoken->word];
#if SHOW_END_TIMES
    /* should be defined outside because it is used outside by w */
    /* sprintf(buf,"%s@%d.%d",w, WORD_TOKEN_GET_WD_ETIME(wtoken), wtoken->end_time); */
    if (strlen(w) + 12 > sizeof(buf))
    {
      *transcription = 0;
      return ERROR_TRANSCRIPTION_TOO_LONG;
    }
    else
    {
      sprintf(buf, "%s@%d", w, wtoken->end_time);
      w = &buf[0];
    }
#endif
    wlen = strlen(w);
    if (wlen + tr_end - transcription + 1 >= len)
    {
      *transcription = 0;
      return ERROR_TRANSCRIPTION_TOO_LONG;
    }
    /*need to tack onto beginning, so move string over*/
    from_p = tr_end;
    to_p = tr_end + wlen + 1;
    tr_end = to_p;
    while (from_p >= transcription) *(to_p--) = *(from_p--);

    /* add a space*/
    *to_p = ' ';

    /*add the new word*/
    to_p = transcription;
    end = to_p + wlen;

    while (to_p < end) *(to_p++) = *(w++);

    if (wtoken_index == wtoken->backtrace)
    {
      *transcription = 0;
#if BUILD&BUILD_DEBUG
      printf("Error: result is loopy, rejecting\n");
#endif
      return ERROR_RESULT_IS_LOOPY;
    }
    wtoken_index = wtoken->backtrace;
  }
  return 0;
}

void print_word_token(srec* rec, wtokenID wtoken_index, char* msg)
{
  bigcostdata cost, cost_for_word;
  char *p = "NULL";
  word_token* wtoken = &rec->word_token_array[wtoken_index];

  PLogMessage ( msg );
  if (wtoken->word < rec->context->olabels->num_words)
    p = rec->context->olabels->words[wtoken->word];
  ASSERT(rec->accumulated_cost_offset[ wtoken->end_time] != 0);
  cost = wtoken->cost + rec->accumulated_cost_offset[wtoken->end_time];
  if (wtoken->backtrace != MAXwtokenID)
  {
    word_token* next_wtoken = &rec->word_token_array[wtoken->backtrace];
    cost_for_word = cost - next_wtoken->cost - rec->accumulated_cost_offset[next_wtoken->end_time];
  }
  else
  {
    cost_for_word = cost;
  }
  printf("wtoken %d W%i %s cost=%d/%d/%d time=%d/%d node=%d", wtoken_index,
         wtoken->word, p, wtoken->cost, cost, cost_for_word, wtoken->end_time, WORD_TOKEN_GET_WD_ETIME(wtoken), wtoken->end_node);
  pfflush(PSTDOUT);
  print_word_token_backtrace(rec, wtoken->backtrace, "\n");
}


void print_word_token_list(srec* rec, wtokenID wtoken_index, char* msg)
{
#ifndef NDEBUG
  int sanity_counter = 0;
#endif
  PLogMessage ( msg );
  while (wtoken_index != MAXwtokenID)
  {
    word_token* wtoken = &rec->word_token_array[wtoken_index];
    print_word_token(rec, wtoken_index, "");
    ASSERT(sanity_counter++ < 200);
    ASSERT(wtoken_index != wtoken->next_token_index);
    wtoken_index = wtoken->next_token_index;
  }
}

#define MAX_LEN 256
void srec_get_result(srec *rec)
{
  srec_word_lattice *wl;
  frameID i;
  wtokenID token_index;
  word_token *wtoken;

#if PRINT_SEARCH_DETAILS
  printf("in srec_get_result\n");
#endif

  wl = rec->word_lattice;
#if PRINT_WORD_LATTICE
  for (i = 0; i <= rec->current_search_frame; i++)
  {
#else
  for (i = rec->current_search_frame; i <= rec->current_search_frame; i++)
  {
#endif

    /* put the best choice at the top */
    sort_word_lattice_at_frame(rec, i);
    token_index = wl->words_for_frame[i];

#if PRINT_WORD_LATTICE
    printf("----- List of words for frame %d\n", i);
    print_word_token_list(rec, token_index, "");
#endif

    if (i == rec->current_search_frame && token_index != MAXwtokenID)
    {
      wtoken =  &(rec->word_token_array[token_index]);
      print_word_token(rec, token_index, "Final Top Choice: ");
    }
  }
}

static srec* WHICH_RECOG(multi_srec* rec)
{
  srec* return_rec = NULL;
  costdata current_best_cost = MAXcostdata;
  int i = 0;
#if DO_ALLOW_MULTIPLE_MODELS
  for (i = 0; i < rec->num_activated_recs; i++)
  {
#endif
    if (current_best_cost > rec->rec[i].current_best_cost)
    {
      current_best_cost = rec->rec[i].current_best_cost;
      return_rec = &rec->rec[i];
    }
#if DO_ALLOW_MULTIPLE_MODELS
  }
#endif
  return return_rec;
}

ESR_ReturnCode srec_get_top_choice_wordIDs(multi_srec* recm, wordID* wordIDs, size_t* len)
{
  srec* rec = WHICH_RECOG(recm);
  frameID end_frame;
  srec_word_lattice* wl;
  wtokenID token_index;
  ESR_ReturnCode rc;

  if (!rec)
  {
    rc = ESR_INVALID_STATE;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }

  end_frame = rec->current_search_frame;
  wl = rec->word_lattice;
  token_index = wl->words_for_frame[end_frame];

  if (token_index == MAXwtokenID)
  {
    PLogError(L("ESR_INVALID_STATE"));
    return ESR_INVALID_STATE;
  }
#if PRINT_WORD_LATTICE
  print_word_token_list(rec, token_index, "WORD TOKENS AT END\n");
#endif
  /* the head of the list on the last frame is always best */
  CHKLOG(rc, sprint_word_token_backtraceByWordID(wordIDs, len, rec, token_index));

  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

int srec_get_top_choice_transcription(multi_srec* recm, char *transcription, int len, int whether_strip_slot_markers)
{
  int rc;
  srec* rec = WHICH_RECOG(recm);
  frameID end_frame;
  srec_word_lattice* wl;
  wtokenID token_index;

  if (!rec)
  {
    *transcription = 0;
    return 1;
  }
  if( recm->eos_status == VALID_SPEECH_NOT_YET_DETECTED)
  {
      *transcription = 0;
      return 1;
  }

  end_frame = rec->current_search_frame;
  wl = rec->word_lattice;
  sort_word_lattice_at_frame(rec, end_frame);
  token_index = wl->words_for_frame[end_frame];

  if (token_index != MAXwtokenID)
  {
#if PRINT_WORD_LATTICE
    print_word_token_list(rec, token_index, "WORD TOKENS AT END\n");
#endif
    /* the head of the list on the last frame is always best */
    rc = sprint_word_token_backtrace(transcription, len, rec, token_index);
  }
  else
  {
    strcpy(transcription, "");
    rc = 1;
  }
  if (whether_strip_slot_markers)
    srec_result_strip_slot_markers(transcription);
  return rc;
}

int srec_get_top_choice_score(multi_srec* recm, bigcostdata *cost, int do_incsil)
{
  srec* rec = WHICH_RECOG(recm);
  frameID end_frame;
  srec_word_lattice* wl;
  wtokenID token_index;
  word_token* wtoken;

  if (!rec)
  {
    *cost = MAXcostdata;
    return 1;
  }

  end_frame = rec->current_search_frame;
  wl = rec->word_lattice;
  token_index = wl->words_for_frame[end_frame];

  if (end_frame < MAXframeID && token_index != MAXwtokenID)
  {
    wtoken = &rec->word_token_array[token_index];
    *cost = wtoken->cost;
    *cost += rec->accumulated_cost_offset[ wtoken->end_time];
    return 0;
  }
  else
  {
    *cost = MAXcostdata;
    return 1;
  }
}

int srec_print_results(multi_srec *recm, int max_choices)
{
  char transcription[MAX_LEN];
  bigcostdata cost;

  srec_get_top_choice_transcription(recm, transcription, MAX_LEN, 1);
  srec_get_top_choice_score(recm, &cost, SCOREMODE_INCLUDE_SILENCE);

  log_report("R: %8ld %8ld %s\t%.1f\n", 0, 0, transcription, cost);

  return 0;
}

/* sort the word lattice at this frame, todo: remove rec argument */

#define MAX_WTOKENS_AT_FRAME 64 /* +1 for the MAXwtokenID! */
void sort_word_lattice_at_frame(srec* rec, frameID frame)
{
  srec_word_lattice* wl = rec->word_lattice;
  word_token *wtoken, *wtoken2;
  wtokenID pwi[MAX_WTOKENS_AT_FRAME], token_index;
  word_token* word_token_array = rec->word_token_array;
  int i, j, npwi = 0;
  ASSERT(rec->word_priority_q->max_in_q < MAX_WTOKENS_AT_FRAME);

  ASSERT(frame < wl->max_frames);
  if (wl->whether_sorted[frame])
    return;

  wl->whether_sorted[frame] = 1;

  /* make an array of word token index addresses */
  for (pwi[npwi] = wl->words_for_frame[frame]; pwi[npwi] != MAXwtokenID;)
  {
    ASSERT(npwi < MAX_WTOKENS_AT_FRAME);
    token_index = pwi[npwi];
    wtoken = &word_token_array[ token_index];
    npwi++;
    pwi[npwi] = wtoken->next_token_index;
  }

  /* sort the word token indices, bubble sort is fine */
  for (i = 0; i < npwi; i++)
  {
    for (j = 0; j < (npwi - 1); j++)
    {
      wtoken = &word_token_array[ pwi[j]];
      wtoken2 = &word_token_array[ pwi[j+1]];
      if (wtoken->cost > wtoken2->cost)
      {
        token_index = pwi[j];
        pwi[j] = pwi[j+1];
        pwi[j+1] = token_index;
      }
    }
  }

  /*print_word_token_list(rec,wl->words_for_frame[frame],"## BEFORE SORT\n");*/
  wl->words_for_frame[ frame] = pwi[0];
  for (i = 0; i < npwi; i++)
  {
    wtoken = &word_token_array[ pwi[i]];
    wtoken->next_token_index = pwi[i+1]; /* last points nowhwere */
  }
  /*print_word_token_list(rec,wl->words_for_frame[frame],"## AFTER  SORT\n");*/
}


/* this frees a word token, it may still have references in the lattice though */

void free_word_token(srec *rec, wtokenID old_token_index)
{
  word_token* wtoken;
  wtoken = &rec->word_token_array[old_token_index];
  wtoken->next_token_index = rec->word_token_freelist;
  rec->word_token_freelist = old_token_index;
  TRUE_KILL_WTOKEN(rec->word_token_array[rec->word_token_freelist]);
}

/* this frees some earlier allocated word_tokens from previous frames,
   this makes sure we can always have some to spare for future frames */

void free_word_token_from_lattice(srec *rec, wtokenID old_token_index)
{
  word_token* wtoken;
  wtokenID *rtoken_index;
  word_token* rtoken;

#define CHECK_FREE_WORD_TOKEN 1
#if CHECK_FREE_WORD_TOKEN
  stokenID stoken_index, i;
  ftokenID ftoken_index;
  fsmarc_token* stoken;
  fsmnode_token* ftoken;
  int nerrs = 0;

  stoken_index = rec->active_fsmarc_tokens;
  for (; stoken_index != MAXstokenID; stoken_index = stoken->next_token_index)
  {
    stoken = &rec->fsmarc_token_array[stoken_index];
    for (i = 0; i < stoken->num_hmm_states; i++)
    {
      if (stoken->word_backtrace[i] == old_token_index)
      {
        printf("Error: can't delete wtoken %d cuz stoken%d.%d cost %d\n",
               old_token_index, stoken_index, i, stoken->cost[i]);
        nerrs++;
      }
    }
  }

  ftoken_index = rec->active_fsmnode_tokens;
  for (; ftoken_index != MAXftokenID; ftoken_index = ftoken->next_token_index)
  {
    ftoken = &rec->fsmnode_token_array[ftoken_index];
    if (ftoken->word_backtrace == old_token_index)
    {
      printf("Error: can't delete wtoken %d cuz ftoken %d cost %d\n",
             old_token_index, ftoken_index, ftoken->cost);
      nerrs++;
    }
  }

  /*  wtoken = &rec->word_token_array[old_token_index];
      for(ifr=wtoken->end_time+1; ifr>=0; ifr--) {
      wtoken_index = rec->word_lattice->words_for_frame[ifr];
      for( ; wtoken_index!= MAXwtokenID; wtoken_index=wtoken->next_token_index) {
      wtoken = &rec->word_token_array[wtoken_index];
      if(wtoken->backtrace == old_token_index) {
      printf("Error: can't delete wtoken %d cuz wtoken %d at frame %d backtraces cost %d\n",
      old_token_index, wtoken_index, ifr, wtoken->cost);
      nerrs++;
      }
      }
      }
  */
  ASSERT(nerrs == 0);
  if (nerrs > 0)
  {
    print_word_token(rec, old_token_index, "Error: while deleting ");
    return;
  }
#endif

  wtoken = &rec->word_token_array[old_token_index];
  /* remove from word lattice */
  rtoken_index = &rec->word_lattice->words_for_frame[ wtoken->end_time+1];
  for (; (*rtoken_index) != MAXwtokenID; rtoken_index = &rtoken->next_token_index)
  {
    rtoken = &rec->word_token_array[(*rtoken_index)];
    if (*rtoken_index == old_token_index)
    {
      *rtoken_index = wtoken->next_token_index;
      break;
    }
  }
  wtoken->next_token_index = rec->word_token_freelist;
  rec->word_token_freelist = old_token_index;
  TRUE_KILL_WTOKEN(rec->word_token_array[rec->word_token_freelist]);
}

int reprune_word_tokens(srec* rec, costdata current_best_cost)
{
  int i, keep_astar_prune;
  arc_token* keep_arc_token_list;

  stokenID stoken_index;
  fsmarc_token* stoken;
  wtokenID btindex;
  word_token* bttoken;
  wtokenID wtoken_index;
  word_token* wtoken;
  altword_token* awtoken;

  /* remember things about the astar before changing it for local purposes */
  keep_astar_prune = rec->astar_stack->prune_delta;
  /* rec->astar_stack->prune_delta = 400; */
  /* ignore the grammar constraints for this quick astar backward pass */
  keep_arc_token_list = rec->context->arc_token_list;
  rec->context->arc_token_list = 0;

  /* we will flag all wtokens to be kept */

  /* initialize the flags to keep all */
  memset(rec->word_token_array_flags, 0, sizeof(rec->word_token_array_flags[0])*rec->word_token_array_size);

  /* flag all those tokens not active, ie already free */
  wtoken_index = rec->word_token_freelist;
  for (; wtoken_index != MAXwtokenID; wtoken_index = wtoken->next_token_index)
  {
    wtoken = &rec->word_token_array[wtoken_index];
    rec->word_token_array_flags[wtoken_index]--;  /* already deleted */
  }

  /* flag along the best active state paths */
  stoken_index = rec->active_fsmarc_tokens;
  for (; stoken_index != MAXstokenID; stoken_index = stoken->next_token_index)
  {
    stoken = &rec->fsmarc_token_array[ stoken_index];
    for (i = 0; i < stoken->num_hmm_states; i++)
    {
      btindex = stoken->word_backtrace[i];
      for (; btindex != MAXwtokenID; btindex = bttoken->backtrace)
      {
        bttoken = &rec->word_token_array[ btindex];
        ASSERT(rec->word_token_array_flags[ btindex] >= 0);
        rec->word_token_array_flags[ btindex] = 1;
      }
      for (awtoken = stoken->aword_backtrace[i]; awtoken;
           awtoken = awtoken->next_token)
      {
        btindex = awtoken->word_backtrace;
        for (; btindex != MAXwtokenID; btindex = bttoken->backtrace)
        {
          bttoken = &rec->word_token_array[ btindex];
          rec->word_token_array_flags[ btindex] = 1;
        }
      }
    }
  }

  /* run the astar and flag a little more */
  astar_stack_prepare_from_active_search(rec->astar_stack, 100, rec);
  astar_stack_do_backwards_search(rec, 100);
  astar_stack_flag_word_tokens_used(rec->astar_stack, rec);
  astar_stack_clear(rec->astar_stack);

  /* kill_word_tokens */
  for (i = 0; i < rec->word_token_array_size; i++)
  {
    if (rec->word_token_array_flags[i] == 0) /* < 0 are already free! */
      free_word_token_from_lattice(rec, (frameID)i);
  }

  /* set this back to a regular astar from remembered values */
  rec->context->arc_token_list = keep_arc_token_list;
  rec->astar_stack->prune_delta = (costdata) keep_astar_prune;

  SREC_STATS_INC_WTOKEN_REPRUNES(1);
  return 0;
}

