/*---------------------------------------------------------------------------*
 *  linklist_impl.c  *
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

#include "pmemory.h"
#include "plog.h"

#include "linklist.h"

extern void *lts_alloc(int num, int size);


/* very simple static memory allocation:
   1. pool of linked list nodes - from static allocated array  
   2. each node is marked "used" when allocated; marked "unused" when deallocated
   3. since the stress linked lists deal with single words, an array will suffice.
*/

#ifdef USE_STATIC_SLTS
#define NUM_ALLOC_NODES 30  /* Max 30 syllables per word */

typedef struct LNodeAllocElement
{
  LNode node;
  short usedflag;
} LNodeAllocElement;

static LNodeAllocElement g_LNodeAllocArray[NUM_ALLOC_NODES];

void ClearLNodeArray()
{
  int i;
  LNode *n;

  for(i=0; i<NUM_ALLOC_NODES; i++){
    g_LNodeAllocArray[i].usedflag = 0;  
    n = &(g_LNodeAllocArray[i].node);  
    n->data = 0;   
    n->next = n->prev = 0;
  }
}

static LNode *AllocNode()
{
  int i;

  /* return first unused element */
  for(i=0; i<NUM_ALLOC_NODES; i++){
    if(g_LNodeAllocArray[i].usedflag == 0){
      g_LNodeAllocArray[i].usedflag = 1;

	  /* zero out the node first*/
	  (g_LNodeAllocArray[i].node).data = NULL;
	  (g_LNodeAllocArray[i].node).prev = NULL;
	  (g_LNodeAllocArray[i].node).next = NULL;
		  
      return &(g_LNodeAllocArray[i].node);
    }
  }
  /* ran out of nodes */
  return NULL;
}

static void FreeNode(LNode *n)
{
  int i;
  long addr;

  /* compare addresses of pointers */
  for(i=0; i<NUM_ALLOC_NODES; i++){
    addr = (long) (&(g_LNodeAllocArray[i].node));
    if(addr == (long)n){
      g_LNodeAllocArray[i].usedflag = 0;
      return;
    }
  }

  /* not found. don't do anything */
   return;
}


#else /* !USE_STATIC_SLTS */

static LNode *AllocNode()
{
  return (LNode *)lts_alloc(1, sizeof(LNode));
}
static void FreeNode(LNode *n)
{
  FREE(n);
}

#endif



/* Inserts after current element 
   At return, current element will be point to newly created node

   handle static allocation later - possibly using a pool of nodes?
   For now, dynamically allocate a new list node with the data
*/
LListResult Insert(LList *list, void *data)
{
  LNode *newnode = AllocNode();
  if(newnode == NULL){
     return LListResourceAllocError; 
  }
  newnode->data = data;

  if(list->head == NULL){
    /* if list is empty, assign to head */   
    list->head = newnode;     
    (list->head)->next = NULL;
    (list->head)->prev = NULL;
    
    /* update curr to newly inserted node */
    list->curr = list->head;
    list->tail = list->head;
    return LListSuccess;
  }
  
    /* curr not specified, insert from the end */  
  if(list->curr == NULL){
    list->curr = list->tail;
  }
  
  /* in cases with single node, default to insert at end */
  if(list->curr == list->tail){
    /* insert at the end */
    newnode->prev = list->curr;
    newnode->next = NULL;
    (list->curr)->next = newnode;

    /* update both curr and end */
    list->curr = newnode;
    list->tail = newnode;
    return LListSuccess;
    
  }else if(list->curr == list->head){
    /* insert at head */
    newnode->next = list->head;
    newnode->prev = NULL;
    (list->head)->prev = newnode;

    /* update curr to newly inserted node */
    list->curr = list->head;
    list->head = newnode;     
    
    return LListSuccess;
  
  }else{
    /* insert somewhere in middle */
    newnode->prev = list->curr;
    newnode->next = (list->curr)->next;
    (list->curr)->next = newnode;
    (newnode->next)->prev = newnode;

    /* update curr to newly inserted node */
    list->curr = newnode;
    return LListSuccess;
  }
}

/* Deletes at current element 
   At return, current element will point to previous node

   handle static deallocation later - possibly using a pool of nodes?
   For now, dynamically free a new list node
*/

LListResult Delete(LList *list)
{
  LNode *curr;

  if(list->head == NULL){
    return LListEmpty;
  } 
  
  /* start deleting from the end if curr not specified */
  if(list->curr == NULL){
    list->curr = list->tail;
  } 
    
  curr = list->curr;

  if(curr == list->head){
  /* delete from the head */
    list->head = curr->next;

    if(list->head != NULL){
      (list->head)->prev = NULL;
    }
    
    FreeNode(curr);
    list->curr = list->head;
    return LListSuccess;
    
  }else if(curr == list->tail){
    /* delete from the end */
    list->tail = curr->prev;

    if(list->tail != NULL){
      (list->tail)->next = NULL;
    }
        
    FreeNode(curr);
    list->curr = list->tail;
    return LListSuccess;
    
  }else{
    /* delete somewhere in the middle */
    list->curr = curr->next;
    
    /* still check, just in case*/
    if(curr->next != NULL){
      (curr->next)->prev = curr->prev;
    }
    if(curr->prev != NULL){
      (curr->prev)->next = curr->next;
    }

    FreeNode(curr);
    return LListSuccess;
  }    
}

