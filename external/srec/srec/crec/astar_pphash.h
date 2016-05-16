/*---------------------------------------------------------------------------*
 *  astar_pphash.h  *
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

#ifndef __ASTAR_PPHASH__
#define __ASTAR_PPHASH__

#define FSH_SUCCESS       0
#define FSH_KEY_OCCUPIED  1
#define FSH_NO_SUCH_KEY   2
#define FSH_HASHSIZE     37
#define FSH_NULL 0

#include "astar.h"

/**
 * The FixedSizeHash is a hash that does not grow in size.
 * In each bin there are a number of elements, which are maintained
 * via a linked list.
 * This is used to find out whether a path with the same word history
 * as the one being expanded has already been search.  If yes, we can
 * abort this one.
 */
typedef struct
{
  int hashsize;
  partial_path* items[FSH_HASHSIZE];
  srec* rec;
}
FixedSizeHash;

void hash_init(FixedSizeHash* hash, srec* rec);
int hash_del(FixedSizeHash* hash, partial_path* parp);
unsigned int hashfunc(partial_path* parp);
int compare_parp(partial_path* parp1, partial_path* parp2, srec* rec);
int hash_get(FixedSizeHash* hash, partial_path* parp, void** hval);
int hash_set(FixedSizeHash* hash, partial_path* parp);

#endif
