/*---------------------------------------------------------------------------*
 *  lts_seq_internal.h  *
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



#ifndef _LTS_SEQ_INTERNAL_H__
#define _LTS_SEQ_INTERNAL_H__

#define NO_NODE 10000
#define MAX_WORD_LEN 50
#define LTS_MAXCHAR 255
#define MAX_CONS_COMB 100
#define MAX_NUM_CONTEXT 5
#define NUM_STRESS_LVL 3 /* SS1, SS2, SS0 */

/*
 * Question types:
 * this enum cannot be changed for a given DATA file 
 */

typedef enum {
  UnKnown = 0,
  Left1,
  Left2,
  Left3,
  Left4,
  Left5,     /*5*/
  Right1,
  Right2,
  Right3,
  Right4,
  Right5,    /*10*/
  LeftPhone1,
  LetInWord,
  SylInWord,
  WordLen,   
  Syl2InWord, /*15*/
  SylInRoot, 
  Syl2InRoot,
  LeftString,
  RightString,
  Left_DFRE,      /*20*/  /*DFRE = distance from root edge*/
  Right_DFRE,      /*DFRE = distance from root edge*/
  NumQuestionTypes
} QuestionType;

typedef struct LQUESTION {
  unsigned char num_list;
  unsigned char type;
  unsigned char *list;	/*list of items to match against for question*/
  unsigned short membership[16]; // UCHAR_MAX/sizeof(unsigned short)/8

} LQUESTION;

#ifdef SKIP_LDP_PROPERTIES
typedef struct LDP {
  unsigned char letter;
  unsigned char left_context[MAX_NUM_CONTEXT];
  unsigned char right_context[MAX_NUM_CONTEXT];
  char left_phone1;
  char let_in_word;
  char syl_in_word;
  char syl2_in_word;
  char syl_in_root;
  char syl2_in_root;
  char word_len;
  int left_string_index;
  int right_string_index;
  int left_DFRE;  /*DFRE = distance from root edge*/
  int right_DFRE;
} LDP;
#else
typedef struct LDP {
  unsigned char letter;
  int properties[ NumQuestionTypes];
} LDP;
#endif


/*RT tree is the compact representations of the trees
  Got rid of the NODE structures in order to save the overhead.

  Instead, the two arrays below are indexed by node_index
*/
typedef struct RT_LTREE {
  short *values_or_question1;  /*if leaf node, this is the value at the node.  If not, this
				 is the index into the questions*/
  short *question2;   /*also used to hold backoff_output for leaf nodes*/
  short *left_nodes;   /*right_node_index is always left_nodex+1, so just store left.
			 If = MAX_NODES, then this is a leaf node*/
  int num_nodes;
} RT_LTREE;


typedef struct LM { /*letter mappings*/
  char *letters;
  char *type;
  int num_letters;
  int letter_index_for_letter[UCHAR_MAX+1];
} LM;


typedef struct PM { /*phone mappings*/
  char **phones;
  int num_phones;
  void* phoneH; /* hash table if any */
} PM;

typedef struct LTS {
  char **outputs;
  char **input_for_output;
  int num_outputs;

  char **strings;
  int num_strings;
  char *string_lens;
  unsigned short membership[16]; // UCHAR_MAX/sizeof(unsigned short)/8

  RT_LTREE **trees;
  LQUESTION **questions;

  LM *letter_mapping;
  PM *phone_mapping;
  LDP dp;
  char *allowable_cons_comb[MAX_CONS_COMB];
  int num_cons_comb;
  void* allowable_cons_combH; /* hash table */
  int num_letters;
  int num_questions;

} LTS;


/* check for combinations of LTS phones to substitute for ETI phones */
/* LTS_ETI_PHONES are defined in a language specific header file slts_phone_def.h */
void replace_eti_phones(char *dest, char *src);

void *lts_alloc(int num, int size);


#endif /* _LTS_SEQ_INTERNAL_H__ */
