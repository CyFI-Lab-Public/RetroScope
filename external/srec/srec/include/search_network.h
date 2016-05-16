/*---------------------------------------------------------------------------*
 *  search_network.h  *
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

/* this file containts data structures needed for defining FSM network*/

#ifndef _h_search_network_
#define _h_search_network_

#include"srec_sizes.h"

typedef struct FSMarc_t FSMarc;
typedef struct FSMnode_t FSMnode;

#define DEBUG_WDADD 0

/* DEBUG_WDADD:
   it's really hard to debug the incremental word addition feature
   without being able to navigate through the graph structure, this
   is enabled in msdev by a compilation that uses actual pointers
   for arc and node references, arc and node pointers can be expanded
   in visual studio, but the IDs (offsets into base pointers) cannot.
   During dev, care should be take to keep the !DEBUG_WDADD fast

   ItoX, XtoP, etc .. convert IDs to X to Ptrs, where
   I ... means ID
   P ... means pointer
   X ... means ID or pointer, depending on DEBUG_WDADD
*/

#if DEBUG_WDADD

#define IF_DEBUG_WDADD(EXPRESSION) EXPRESSION
#define printf_arc printf_arc1
#define printf_node printf_node1
#define TO_NODE(ARC) (ARC)->to_node
#define NEXT_NODE(NOD) (NOD)->next_node
#define LINKL_NEXT(ARC) (ARC)->linkl_next_arc
#define LINKL_PREV(ARC) (ARC)->linkl_prev_arc
#define FIRST_PREV(NOD) (NOD)->first_prev_arc
#define FIRST_NEXT(NOD) (NOD)->first_next_arc
#define ARC_XtoP(ARC) (ARC)
#define ARC_XtoI(ARC)  ((arcID)((ARC)-fst->FSMarc_list))
#define ARC_PtoX(ARC)  (ARC)
#define ARC_ItoX(ARC_ID)  (&fst->FSMarc_list[ARC_ID])
#define NODE_XtoP(NOD) (NOD)
#define NODE_XtoI(NOD) ((nodeID)((NOD)-fst->FSMnode_list))
#define NODE_PtoX(NOD) (NOD)
#define NODE_ItoX(NODE_ID) (&fst->FSMnode_list[NODE_ID])
#define FSMARC_NULL NULL
#define FSMNODE_NULL NULL
#define FSMARC_FREE (FSMarc*)0xffffffff
#define FSMNODE_FREE (FSMnode*)0xffffffff

#else

#define IF_DEBUG_WDADD(EXPRESSION)
#define printf_arc
#define printf_node
#define TO_NODE(ARC) fst->FSMarc_list[(ARC)].to_node
#define NEXT_NODE(NOD) fst->FSMnode_list[(NOD)].un_ptr.next_node
#define LINKL_NEXT(ARC) fst->FSMarc_list[(ARC)].linkl_next_arc
#define LINKL_PREV(ARC) fst->FSMarc_list[(ARC)].linkl_prev_arc
#define FIRST_PREV(NOD) fst->FSMnode_list[(NOD)].first_prev_arc
#define FIRST_NEXT(NOD) fst->FSMnode_list[(NOD)].un_ptr.first_next_arc
#define ARC_XtoP(ARC) (&fst->FSMarc_list[(ARC)])
#define ARC_XtoI(ARC)  ((arcID)(ARC))
#define ARC_PtoX(ARC)  ((arcID)((ARC)-fst->FSMarc_list))
#define ARC_ItoX(ARC_ID)  ((arcID)(ARC_ID))
#define NODE_XtoP(NOD) (&fst->FSMnode_list[(NOD)])
#define NODE_XtoI(NOD) ((nodeID)(NOD))
#define NODE_PtoX(NOD) ((nodeID)((NOD)-fst->FSMnode_list))
#define NODE_ItoX(NODE_ID) ((nodeID)(NODE_ID))
#define FSMARC_NULL MAXarcID
#define FSMNODE_NULL MAXnodeID
#define FSMARC_FREE MAXarcID-1

#endif

#if DEBUG_WDADD
typedef FSMnode* FSMnode_ptr;
typedef FSMarc*  FSMarc_ptr;
#else
typedef nodeID      FSMnode_ptr;
typedef arcID       FSMarc_ptr;
#endif

/**
 * @todo document
 */
typedef struct FSMnode_t
{
  union {
    FSMarc_ptr first_next_arc;
    FSMnode_ptr next_node;
  } un_ptr;
  FSMarc_ptr first_prev_arc; /* this can be removed if not doing addword */
}
FSMnode_t;

/**
 * @todo document
 */
typedef struct FSMarc_t
{
#if DEBUG_WDADD
  char* ilabel_str;
  char* olabel_str;
#endif
  FSMnode_ptr to_node;
  FSMarc_ptr linkl_next_arc;
  
  FSMnode_ptr fr_node;         /* this can be removed if not doing addword */
  FSMarc_ptr linkl_prev_arc;   /* this can be removed if not doing addword */
  
  labelID ilabel;              /* input label */
  labelID olabel;              /* output label */
  costdata cost;
}
FSMarc_t;

/*according to Johan:*/
#define EPSILON_OFFSET 3
#define EPSILON_LABEL 0
#define WORD_BOUNDARY 1
#define PHONE_BOUNDARY 2

/* */
#define WORD_EPSILON_LABEL 0




#endif
