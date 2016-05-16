/*---------------------------------------------------------------------------*
 *  hmm_desc.h  *
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



#ifndef _h_hmm_desc_
#define _h_hmm_desc_

#ifdef SET_RCSID
static const char hmm_desc_h[] = "$Id: hmm_desc.h,v 1.2.10.6 2008/01/21 20:30:05 dahan Exp $";
#endif

#include "creccons.h"   /* CREC Public Constants    */

#include "all_defs.h"
#include "hmm_type.h"
#include "sizes.h"



#define PDTYP(X)        ((X) & 0x0f)

#define DIAG            (1<<4) /* Diagonal covariance model */
#define FULL            (2<<4) /* Full covariance model */
#define VARTYP(X)       ((X) & 0x30)

#define EXPDUR  (1<<6) /* Exponential duration model */
#define DURTYP(X) ((X) & 0xc0)

/**
 * @todo document
 */
typedef struct
{
  unsigned char phone; /*  Internal phoneme symbol */
  unsigned char pr_code[4]; /*  Printable phone code */
  unsigned char near_phone;
  int  num_states;
  unsigned char dict_code;    /*  single char printable code */
}
phoneme_info;

/**
 * @todo document
 */
typedef struct
{
  int  num_phones;
  phoneme_info *phoneme;
  int  index[256];
}
phoneme_set;

/**
 * The space_filler var here is to make sure that this structure is
 * always the same size on different platforms.  ARM unix appears to
 * want structures to by a multiple of 4 bytes, hence the filler.
 */
typedef struct
{
  unsigned char left_phone[MAX_PHONEMES];
  unsigned char right_phone[MAX_PHONEMES];
  short  apply;
  short  space_filler; /* TODO: revisit this issue */
}
question;

#define NON_GENERIC 0
#define RIGHT_GENERIC 1
#define LEFT_GENERIC 2 /*  Was the other way */
#define BOTH_GENERIC 3
#define DIPHONE  9

/* the terminal_tree_node structure is used in a union with tree_branch_info
   resulting in a "tree_node".  We must initialize "tree_node"s of both
   both types in large static array, which is hard.  So instead we initialize
   a static array of terminal_tree_node's but need to fill that structure up
   with dummies to be the same size as the full "tree_node".  For 2-byte
   pointer configurations this may not be memory efficient :( */

/**
 * @todo document
 */
typedef struct terminal_tree_node_info
{
  asr_int16_t    quest_index;
  asr_int16_t   pelid;
  asr_int16_t   avg_durn;
  asr_int16_t          dummy_filler1, dummy_filler2, dummy_filler3;
}
terminal_tree_node;

/**
 * @todo document
 */
typedef struct tree_branch_info
{
  asr_int16_t  quest_index;
  struct tree_branch_info  *fail;
  struct tree_branch_info  *pass;
}
tree_branch_info;

/**
 * @todo document
 */
typedef union {
  struct tree_branch_info    node;
  struct terminal_tree_node_info term;
} tree_node;

/**
 * @todo document
 */
typedef struct
{
  int       no_states;
  /*    int       phoneme; */
  tree_node *root[MAX_PHONE_STATES];
}
tree_info;

/**
 * holds the body of a sorted .ok dictionary file
 */
typedef struct
{
  char* ok_file_data; /* data in the .ok file */
  int ok_file_data_length; /* length of data ok_file_data */
  const char* first_entry; /* first entry in the dictionary */
  const char* last_entry; /* last entry in the dictionary */
  int hasUpper; /* nonzero if upper case present in dictionary (usually not) */
}
vocab_info;

#endif
