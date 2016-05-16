/*---------------------------------------------------------------------------*
 *  linklist.h  *
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



/* each node stores pointer to data, and pointer to next node */
typedef struct LNode {
  void *data;   
  struct LNode *next;
  struct LNode *prev;
}LNode;

typedef struct LList {
  struct LNode *head;
  struct LNode *curr;
  struct LNode *tail;
}LList;

typedef enum{
  LListSuccess = 1,
  LListResourceAllocError,
  LListEmpty,
  LListInternalError
}LListResult;

/* Inserts after current element 
   At return, current element will be point to newly created node

   handle static allocation later - possibly using a pool of nodes?
   For now, dynamically allocate a new list node with the data
*/
LListResult Insert(LList *list, void *data);


/* Deletes at current element 
   At return, current element will point to previous node

   handle static deallocation later - possibly using a pool of nodes?
   For now, dynamically free a new list node
*/

LListResult Delete(LList *list);

/* If dynamic allocation is not allowed, provide small pool of nodes */
#ifdef USE_STATIC_SLTS
void ClearLNodeArray();
#endif

