/*---------------------------------------------------------------------------*
 *  astar_pphash.c  *
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

#include "pstdio.h"
#include "passert.h"
#include "portable.h"

#include"duk_err.h"
#include"srec.h"
#include"astar.h"
#include"astar_pphash.h"

#define DEBUG_PPHASH 0


/* initialize the hash with no elements defined */

void hash_init(FixedSizeHash* hash, srec* rec_debug)
{
  int i;
  hash->hashsize = FSH_HASHSIZE;
  for (i = 0; i < hash->hashsize; i++)
    hash->items[i] = FSH_NULL;
  hash->rec = rec_debug;
}

/* compare a couple of paths,
   ie see whether the word history is the same */

int compare_parp(partial_path* parp1, partial_path* parp2, srec* rec)
{
  asr_int32_t diff = 0;
  if (parp1->first_prev_arc != PARP_TERMINAL ||
      parp2->first_prev_arc != PARP_TERMINAL)
  {
    diff = parp1->token_index - parp2->token_index;
  }
  else
  {
    while (parp1->next && parp2->next)
    {
      diff = parp1->word - parp2->word;
      if (diff)
      {
        goto CPE;
      }
      parp1 = parp1->next;
      parp2 = parp2->next;
    }
    diff = (int)parp1->next - (int)parp2->next;
  }
CPE:
  if (diff)
    diff = (diff < 0 ? -1 : 1);
  return diff;
}

/* find the bin */

unsigned int hashfunc(partial_path* parp)
{
  unsigned int hashval;
  if (parp->first_prev_arc != PARP_TERMINAL)
    hashval = parp->token_index;
  else
    hashval = 0;
  hashval = (hashval << 10) + parp->word;
  while ((parp = parp->next) != NULL)
  {
    if (parp->word != MAXwordID)
      hashval = hashval * 64 + parp->word + hashval % 65536;
  }
  return hashval;
}

/* get a history same as this one */

int hash_get(FixedSizeHash* hash, partial_path* parp, void** hval)
{
  unsigned int hkey_index = hashfunc(parp);
  partial_path* p_return;
  
  hkey_index = hkey_index % hash->hashsize;
  p_return = hash->items[hkey_index];
  if (!p_return)
    return FSH_NO_SUCH_KEY;
  for (; p_return; p_return = p_return->hashlink)
  {
    if (compare_parp(p_return, parp, hash->rec) == 0)
    {
      *hval = p_return;
      return FSH_SUCCESS;
    }
  }
  return FSH_NO_SUCH_KEY;
}

/* set this, return error is same path already there */

int hash_set(FixedSizeHash* hash, partial_path* parp)
{
  unsigned int hkey_index = hashfunc(parp);
  partial_path** p_insert;
  
  hkey_index = hkey_index % hash->hashsize;
  p_insert = &hash->items[hkey_index];
  for (; *p_insert; p_insert = &((*p_insert)->hashlink))
  {
    if (*p_insert == parp)
    {
#if 1||DEBUG_PPHASH
      print_path(parp, hash->rec, "problem in astar_pphash hash_set ");
#endif
      return FSH_SUCCESS;
    }
    else if (compare_parp(*p_insert, parp, hash->rec) == 0)
    {
#if DEBUG_PPHASH
      print_path(*p_insert, hash->rec, "key taken in astar_pphash hash_set ");
#endif
      return FSH_KEY_OCCUPIED;
    }
  }
  *p_insert = parp;
#if DEBUG_PPHASH
  printf("setting at %d ", hkey_index);
  print_path(parp, hash->rec, "");
#endif
  parp->hashlink = FSH_NULL;
  return FSH_SUCCESS;
}

/* delete an element */

int hash_del(FixedSizeHash* hash, partial_path* parp)
{
  unsigned int hkey_index = hashfunc(parp);
  partial_path** p_insert;
  
  hkey_index = hkey_index % hash->hashsize;
  p_insert = &hash->items[hkey_index];
  for (; *p_insert; p_insert = &((*p_insert)->hashlink))
  {
    if (compare_parp(*p_insert, parp, hash->rec) == 0)
    {
      *p_insert = parp->hashlink;
#if DEBUG_PPHASH
      printf("delhash at %d\n", hkey_index);
      print_path(parp, hash->rec, "deleted ");
#endif
      return FSH_SUCCESS;
    }
  }
  return FSH_NO_SUCH_KEY;
}

