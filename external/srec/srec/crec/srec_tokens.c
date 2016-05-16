/*---------------------------------------------------------------------------*
 *  srec_tokens.c  *
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

#include"srec.h"
#include"srec_tokens.h"
#include "passert.h"
#include "portable.h"


/* we do no expect dump_core() to ever be called */
static void dump_core(char *msg)
{
  PLogError ( msg );
  ASSERT(0);
}

/*
 * fsmarc_token management
 */

int count_fsmarc_token_list(srec* rec, stokenID token_index)
{
  int count = 0;
  
  while (token_index != MAXstokenID)
  {
    fsmarc_token* stoken = &rec->fsmarc_token_array[token_index];
    token_index = stoken->next_token_index;
    count++;
    ASSERT(count < 5000);
  }
  return count;
}

/* static int num_fsmarc_tokens_allocated = 0; */

void initialize_free_fsmarc_tokens(srec *rec)
{
  stokenID i;
  
  /* num_fsmarc_tokens_allocated = 0; */
  for (i = 0;i < rec->fsmarc_token_array_size - 1;i++)
  {
    rec->fsmarc_token_array[i].next_token_index = i + 1;
  }
  rec->fsmarc_token_array[rec->fsmarc_token_array_size-1].next_token_index = MAXstokenID;
  rec->fsmarc_token_freelist = 0;
}

/*allocates the token and sets it up for a given arc*/
stokenID setup_free_fsmarc_token(srec *rec, FSMarc* arc, arcID fsm_arc_index, miscdata what_to_do_if_fails)
{
  int i;
  stokenID token_to_return;
  fsmarc_token *token;
  
  if (rec->fsmarc_token_freelist ==  MAXstokenID)
  {
    if (what_to_do_if_fails == EXIT_IF_NO_TOKENS)
    {
      /*FIX - replace with crec error handling*/
      dump_core("setup_free_fsmarc_token: ran out of tokens\n");
    }
    if (what_to_do_if_fails == NULL_IF_NO_TOKENS)
    {
      return MAXstokenID;
    }
    else
    { /*no other conditions for now, so just exit*/
      /*FIX - replace with crec error handling*/
      dump_core("setup_free_fsmarc_token: ran out of tokens\n");
    }
  }
  
  ASSERT(rec->fsmarc_token_freelist < rec->fsmarc_token_array_size);
  token_to_return = rec->fsmarc_token_freelist;
  token = &(rec->fsmarc_token_array[token_to_return]);
  
  token->FSMarc_index = fsm_arc_index;
  arc = &(rec->context->FSMarc_list[fsm_arc_index]);
  token->num_hmm_states = rec->context->hmm_info_for_ilabel[arc->ilabel].num_states;
  
  for (i = 0;i < token->num_hmm_states;i++)
  {
    token->cost[i] = MAXcostdata;
    token->word[i] = MAXwordID;
    token->word_backtrace[i] = MAXwtokenID;
    token->duration[i] = MAXframeID;
    token->aword_backtrace[i] = AWTNULL;
  }
  
  rec->fsmarc_token_freelist = token->next_token_index;
  
  /* num_fsmarc_tokens_allocated++; */
  return token_to_return;
}


void free_fsmarc_token(srec *rec, stokenID old_token_index)
{
  fsmarc_token* stoken;
  ASSERT(old_token_index < rec->fsmarc_token_array_size);
  stoken = &rec->fsmarc_token_array[old_token_index];
  stoken->next_token_index = rec->fsmarc_token_freelist;
  rec->fsmarc_token_freelist = old_token_index;
  { int i;
    for (i = 0; i < stoken->num_hmm_states; i++)
      if (stoken->aword_backtrace[i] != AWTNULL)
        free_altword_token_batch(rec, stoken->aword_backtrace[i]);
  }
}

void sort_fsmarc_token_list(srec* rec, stokenID* ptoken_index)
{};

/*
 * word_token management
 */

void initialize_free_word_tokens(srec *rec)
{
  wtokenID i;
  word_token* wtoken = NULL;
  
  for (i = 0;i < rec->word_token_array_size;i++)
  {
    wtoken = &rec->word_token_array[i];
    wtoken->next_token_index = i + 1;
  }
  /* last one must point nowhere */
  wtoken->next_token_index = MAXwtokenID;
  rec->word_token_freelist = 0;
}

wtokenID get_free_word_token(srec *rec, miscdata what_to_do_if_fails)
{
  wtokenID token_to_return;
  word_token* wtoken;
  
  if (rec->word_token_freelist ==  MAXwtokenID)
  {
    if (what_to_do_if_fails == EXIT_IF_NO_TOKENS)
    {
      /*FIX - replace with crec error handling*/
      dump_core("get_free_word_token: ran out of tokens\n");
    }
    if (what_to_do_if_fails == NULL_IF_NO_TOKENS)
    {
      return MAXwtokenID;
    }
    else
    { /*no other conditions for now, so just exit*/
      /*FIX - replace with crec error handling*/
      dump_core("get_free_word_token: ran out of tokens\n");
    }
  }
  
  token_to_return = rec->word_token_freelist;
  wtoken =  &rec->word_token_array[token_to_return];
  rec->word_token_freelist = wtoken->next_token_index;
  
  /*note that we are returning without setting any contents of the token (including next_token_index)
   leave it for the calling program to take care of that*/
  
  return token_to_return;
}




/*
 * fsmnode_token management
 */

int count_fsmnode_token_list(srec* rec, ftokenID token_index)
{
  int count = 0;
  
  while (token_index != MAXftokenID)
  {
    fsmnode_token* ftoken = &rec->fsmnode_token_array[token_index];
    token_index = ftoken->next_token_index;
    count++;
  }
  return count;
}

void initialize_free_fsmnode_tokens(srec *rec)
{
  ftokenID i;
  fsmnode_token* ftoken = NULL;
  
  for (i = 0;i < rec->fsmnode_token_array_size;i++)
  {
    ftoken = &rec->fsmnode_token_array[i];
    ftoken->next_token_index = i + 1;
  }
  /* last one must point nowhere */
  ftoken->next_token_index = MAXftokenID;
  rec->fsmnode_token_freelist = 0;
}

ftokenID get_free_fsmnode_token(srec *rec, miscdata what_to_do_if_fails)
{
  ftokenID token_to_return;
  fsmnode_token* ftoken;
  
  if (rec->fsmnode_token_freelist ==  MAXftokenID)
  {
    if (what_to_do_if_fails == EXIT_IF_NO_TOKENS)
    {
      /*FIX - replace with crec error handling*/
      dump_core("get_free_fsmnode_token: ran out of tokens\n");
    }
    if (what_to_do_if_fails == NULL_IF_NO_TOKENS)
    {
      return MAXftokenID;
    }
    else
    { /*no other conditions for now, so just exit*/
      /*FIX - replace with crec error handling*/
      dump_core("get_free_fsmnode_token: ran out of tokens\n");
    }
  }
  
  token_to_return = rec->fsmnode_token_freelist;
  ftoken =  &rec->fsmnode_token_array[token_to_return];
  rec->fsmnode_token_freelist = ftoken->next_token_index;
  
  /*note that we are returning without setting any contents of the token
    (including next_token_index)
    leave it for the calling program to take care of that */
  
  return token_to_return;
}

void free_fsmnode_token(srec *rec, ftokenID old_token_index)
{
  fsmnode_token* ftoken;
  ASSERT(old_token_index < rec->fsmnode_token_array_size);
  ftoken = &rec->fsmnode_token_array[old_token_index];
  ftoken->next_token_index = rec->fsmnode_token_freelist;
  ftoken->cost = MAXcostdata;
  rec->fsmnode_token_freelist = old_token_index;
  if (ftoken->aword_backtrace != AWTNULL)
    free_altword_token_batch(rec, ftoken->aword_backtrace);
}

/*
 *  altword token management
 */

void initialize_free_altword_tokens(srec *rec)
{
  wtokenID i;
  altword_token* awtoken = NULL;
  for (i = 0;i < rec->altword_token_array_size;i++)
  {
    awtoken = rec->altword_token_array + i;
    awtoken->next_token = awtoken + 1;
    awtoken->costdelta  = MAXcostdata;
    awtoken->refcount   = 0;
    awtoken->costbasis  = 0;
  }
  /* last one must point nowhere */
  awtoken->next_token = NULL;
  rec->altword_token_freelist = &rec->altword_token_array[0];
  rec->altword_token_freelist_len = rec->altword_token_array_size;
}


int count_altword_token(srec* rec, altword_token* b)
{
  int num = 0;
  for (; b; b = b->next_token) { 
	  num++; 
      // if(num>9999) ASSERT(0); 
  }
  return num;
}


/* get a free altword token, handle out of memory later!! */
altword_token* get_free_altword_token(srec* rec, miscdata what_to_do_if_fails)
{
  altword_token* awtoken = rec->altword_token_freelist;
  /* what_to_do_if_fails ... we do not ever expect failure because
     all get_free's are preceded by repruning, but if there should
     ever be a failure, we will return NULL and handle it in the 
     caller */
  if (!awtoken /*&& what_to_do_if_fails==NULL_IF_NO_TOKENS*/)
    return awtoken;
  awtoken->refcount = 1;
  rec->altword_token_freelist = awtoken->next_token;
  rec->altword_token_freelist_len--;
  return awtoken;
}

/* release an altword token */
int free_altword_token(srec* rec, altword_token* old_token)
{
  ASSERT(old_token->refcount >= 1);
  if (--old_token->refcount <= 0)
  {
    old_token->next_token = rec->altword_token_freelist;
    old_token->costdelta  = MAXcostdata;
    rec->altword_token_freelist = old_token;
    rec->altword_token_freelist_len++;
  }
  return old_token->refcount; /* return zero if truly freed */
}

altword_token* free_altword_token_batch(srec* rec, altword_token* old_token)
{
  /* char dd[128], *ddp = &dd[0]; */
  ASSERT(old_token->refcount >= 1);
  if (--old_token->refcount <= 0)
  {
    altword_token *awtoken, *next_awtoken;
    for (awtoken = old_token; awtoken != AWTNULL; awtoken = next_awtoken)
    {
      next_awtoken = awtoken->next_token;
      /* *(ddp++) = '0' + awtoken->refcount; */
      awtoken->costdelta  = MAXcostdata;
      awtoken->next_token = rec->altword_token_freelist;
      rec->altword_token_freelist = awtoken;
      rec->altword_token_freelist_len++;
    }
  }  
  return AWTNULL;
}

