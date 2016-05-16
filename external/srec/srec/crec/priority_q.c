/*---------------------------------------------------------------------------*
 *  priority_q.c  *
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
#include <string.h>
#include <math.h>
#include "passert.h"

#include "portable.h"

#include "hmm_desc.h"
#include "utteranc.h"
#include "hmmlib.h"

#include "srec_sizes.h"
#include "search_network.h"
#include "srec.h"
#include "word_lattice.h"

#define PRINT_SEARCH_DETAILS 0

/*this is just implemented as a list so far - FIX this!!*/

/*allocates priority_q to han le max_n entries*/
priority_q* allocate_priority_q(int max_n)
{
  priority_q *pq;
  
  pq = (priority_q*) CALLOC(1, sizeof(priority_q), "search.srec.priority_q");
  pq->max_cost_in_q = MAXcostdata;
  pq->word_token_list = MAXwordID;
  pq->max_in_q = (miscdata)max_n;
  pq->num_in_q = 0;
  return pq;
}

void free_priority_q(priority_q* pq)
{
  FREE(pq);
}

/*empties out priority_q*/

void clear_priority_q(priority_q *pq)
{
  pq->max_cost_in_q = MAXcostdata;
  pq->word_token_list = MAXwordID;
  pq->num_in_q = 0;
  /* Jean: what about the list of free tokens? */
}
/* returns the head of a linked list of all words in the priority_q.
   Return MAXwtokenID if list is empty */

wtokenID get_word_token_list(priority_q *pq, word_token *word_token_array)
{
  return pq->word_token_list;
}

void remove_non_end_word_from_q(srec *rec, priority_q *pq, word_token *word_token_array, nodeID end_node)
{
  word_token *token;
  wtokenID *ptoken_index;
  wtokenID old_token_index;
  
  pq->max_cost_in_q = MAXcostdata;
  pq->num_in_q = 0;
  ptoken_index = &(pq->word_token_list);
  
  while (*ptoken_index != MAXwtokenID)
  {
    token = &(word_token_array[*ptoken_index]);
    if (token->end_node != end_node)
    {
      old_token_index = *ptoken_index;
      *ptoken_index = token->next_token_index;
      free_word_token(rec, old_token_index);
      pq->max_cost_in_q = MAXcostdata; /* fix: sep9 */
    }
    else
    {
      pq->num_in_q++;
      if ((pq->max_cost_in_q == MAXcostdata) || (token->cost > pq->max_cost_in_q))
      {
        pq->max_cost_in_q = token->cost;
      }
      ptoken_index = &(token->next_token_index);
    }
  }
}

int compare_histories(word_token* token1, word_token* token2,
                      word_token* word_token_array)
{
  int history_for_token1 = 0;
  int history_for_token2 = 0;
  
  /* compare_histories() was an attempt to be smart about the priority_q,
     in that we don't need to store two word_tokens when the two tokens 
     are the same word (obviously ending at the same frame), and with the 
     same word history.  This happens for a digit that has multiple end nodes
     due to context-dependency.  When "history_for_token" ignores the end_node,
     then we're all clear to save just 1 word_token, but continue propagating
     all paths from the end nodes.  That bit of "continue propagating" is not 
     done. THE OTHER PROBLEM is that the two nodes may NOT be
     simply different CD end models, they may be different from digit shifting!
     We're screwed if we drop the path, unless we compare all the way back to
     the start of utterance. */
  
  if (token1->word != token2->word)
    return 1;
  if (token1->end_node != token2->end_node)
    return 1;
    
  if (token1->backtrace != MAXwordID)
  {
    history_for_token1 += token1->end_node * 1000000;
    history_for_token1 += word_token_array[token1->backtrace].word * 10000;
    history_for_token1 += word_token_array[token1->backtrace].end_time;
  }
  
  if (token2->backtrace != MAXwordID)
  {
    history_for_token2 += token2->end_node * 1000000;
    history_for_token2 += word_token_array[token2->backtrace].word * 10000;
    history_for_token2 += word_token_array[token2->backtrace].end_time;
  }
  
#if PRINT_SEARCH_DETAILS
  printf("comparing history_for_token1 %d history_for_token2 %d\n",
         history_for_token1, history_for_token2);
#endif
         
  if (history_for_token1 == history_for_token2)
  {
    return 0;
  }
  else
  {
    return 1;
  }
}

#if PRINT_SEARCH_DETAILS
void sanity_check_priority_q(priority_q* pq, word_token *word_token_array)
{
  int n = 0;
  wtokenID token_index;
  word_token* token;
  n = 0;
  token_index = pq->word_token_list;
  while (token_index != MAXwordID)
  {
    token = &(word_token_array[token_index]);
    token_index = token->next_token_index;
    n++;
  }
  ASSERT(n == pq->num_in_q);
  if (pq->num_in_q == pq->max_in_q)
  {
    token = &(word_token_array[pq->word_token_list]);
    ASSERT(pq->max_cost_in_q == token->cost);
  }
}
#endif

/*adds a word token to the priority_q.  Returns the index of the word to
  remove.
  if nothing needs to be removed, returns MAXwtokenID.
  if no room on priority_q, returns the one being put on */

wtokenID add_word_token_to_priority_q(priority_q *pq, wtokenID token_index_to_add, word_token *word_token_array)
{
  word_token *token;
  word_token *token_to_add;
  wtokenID token_index, return_token_index;
  wordID word_to_add;
  costdata cost_to_add;
  wtokenID *ptoken_index;
  wtokenID *pplace_to_add;
  wtokenID *pdelete_index;
  word_token *token_to_delete;
  
  token_to_add = &(word_token_array[token_index_to_add]);
  cost_to_add = token_to_add->cost;
  
#if PRINT_SEARCH_DETAILS
  printf("WORDADD PQ tokenid %d cost %d\n", token_index_to_add, cost_to_add);
  token_index = pq->word_token_list;
  while (token_index != MAXwordID)
  {
    token = &(word_token_array[token_index]);
    printf("WORDADD PQ token %d word %d cost %d\n", token_index, token->word, token->cost);
    token_index = token->next_token_index;
  }
#endif
  
  if (cost_to_add >= pq->max_cost_in_q && pq->num_in_q >= pq->max_in_q)
  {
#if PRINT_SEARCH_DETAILS
    printf("WORDADD PQ - rejecting because cost too high cost_to_add(%d) max_cost_in_q(%d) num_in_q(%d)\n",
           cost_to_add, pq->max_cost_in_q, pq->num_in_q);
#endif
#if PRINT_SEARCH_DETAILS
    printf("WORDADD PQ (D) returning %d\n", token_index_to_add);
    sanity_check_priority_q(pq, word_token_array);
#endif
    return token_index_to_add;
  }
  
  word_to_add = token_to_add->word;
  /* search for duplicate words first */
  ptoken_index = &(pq->word_token_list);
  pplace_to_add = NULL;
  pdelete_index = NULL;
  while ((*ptoken_index) != MAXwordID)
  {
    token = &word_token_array[(*ptoken_index)];
    
    if (token->word == token_to_add->word
        && !compare_histories(token, token_to_add, word_token_array))
    {
      if (token->cost < cost_to_add)
      {
        /* don't bother adding, there's another like it on the list!
           with a better score! */
#if PRINT_SEARCH_DETAILS
        printf("WORDADD PQ - rejecting because another like it is on the list\n");
#endif
        /* TODO: when returning back on the basis that something else is better,
           we should let the caller know what to use instead, ie, make the
           distinction between no-space and something-else-better */
        token = &word_token_array[ token_index_to_add];
        token->next_token_index = (*ptoken_index);
        return token_index_to_add;
      }
      else
      {
        /* ok, replace the one on the list with this better scoring one! */
        pdelete_index = ptoken_index;
      }
    }
    if (token->cost < cost_to_add && pplace_to_add == NULL)
    {
      pplace_to_add = ptoken_index;
      /* do not break, 'cuz we're still searching for a possible duplicates */
    }
    ptoken_index = &(token->next_token_index);
  }
  if (!pplace_to_add)
    pplace_to_add = ptoken_index;
    
  /* add the token by inserting in the linked list */
  token_index = *pplace_to_add;
  *pplace_to_add = token_index_to_add;
  token_to_add->next_token_index = token_index;
  pq->num_in_q++;
  if (pplace_to_add == &pq->word_token_list && pq->num_in_q >= pq->max_in_q)
    pq->max_cost_in_q = cost_to_add;
    
  /* now delete any duplicate that was found */
  if (pdelete_index)
  {
    token_index = *pdelete_index;
    token_to_delete = &word_token_array[  token_index];
    *pdelete_index = token_to_delete->next_token_index;
    pq->num_in_q--;
#if PRINT_SEARCH_DETAILS
    printf("WORDADD PQ (B) returning %d\n", token_index);
#endif
    return_token_index = token_index;
  }
  
  /* now check for max length in the queue */
  if (pq->num_in_q > pq->max_in_q)
  { /* really expecting just 1 over */
    token_index = pq->word_token_list;
    token = &(word_token_array[ token_index]);
    pq->num_in_q--;
    pq->word_token_list = token->next_token_index;
#if PRINT_SEARCH_DETAILS
    printf("WORDADD PQ (C) returning %d\n", token_index);
#endif
    return_token_index = token_index;
  }
  else
  {
    return_token_index = MAXwtokenID;
  }
  if (pq->num_in_q >= pq->max_in_q)
  {
    token_index = pq->word_token_list;
    token = &(word_token_array[token_index]);
    pq->max_cost_in_q = token->cost;
  }
  else
  { /* pq->num_in_q < pq->max_in_q, fixed sep9 */
    pq->max_cost_in_q = MAXcostdata;
  }
#if PRINT_SEARCH_DETAILS
  printf("WORDADD PQ (A) returning %d\n", token_index);
  sanity_check_priority_q(pq, word_token_array);
#endif
  return return_token_index;
}


/*returns the cost threshold for the end of the priority queue.
  If words have greater cost than this, no need to try to put them on the
  queue*/

costdata get_priority_q_threshold(priority_q *pq, word_token *word_token_array)
{
  return pq->max_cost_in_q;
}





