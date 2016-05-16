/*---------------------------------------------------------------------------*
 *  run_seq_lts.c  *
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
#include <ctype.h>

#ifndef NO_STDERR   
#include <stdio.h>
#else
extern void PrintError(char *msg, unsigned long p1, unsigned long p2, unsigned long p3);
#endif

#include "passert.h"
#include "pmemory.h"
#include "plog.h"
#include "phashtable.h"
#include "lts_error.h"
#include "lts.h"
#include "lts_seq_internal.h"
#include "port_fileio.h"
#include "platform_utils.h" /* strdup, safe_strtok, etc */

#define ASSERT(x) passert(x)

#ifdef TI_DSP
#include "tidsp_defines.h"
#endif

#ifdef _DEBUG
#define PRINT_LOAD_TREE_SUMMARY 0
#define PRINT_LOAD_TREE 0
#define PRINT_CONS_COMB 0
#define PRINT_DP_LETTER 0
#define PRINT_LTS_WORD 0
#define PRINT_DICT_LOOKUP 0   
#endif

#define LTS_MARKER_WORD_START "WS"
#define LTS_MARKER_PRON_START "PS"
#define LTS_MARKER_SYLL_START "SS"
#define LTS_MARKER_SYLL_START_DD "SS%d"
#define LTS_MARKER_PIPESEP "|"
#define LTS_MARKER_PIPESEP_CHAR '|'

static int load_int(PORT_FILE *fp);
static SWIsltsResult load_lquestions(LQUESTION ***pquestions, int *pnum_questions, PORT_FILE *fp);
static SWIsltsResult free_lquestions(LQUESTION ** questions, int num_questions);
static SWIsltsResult load_letter_mapping(PORT_FILE *fp, LM **ppLetterMap);
static SWIsltsResult free_letter_mapping(LM *lm);
static SWIsltsResult load_phone_mapping(PORT_FILE *fp, PM **ppPhoneMap);
static SWIsltsResult free_phone_mapping(PM *pm);
static SWIsltsResult load_outputs(char ***poutputs, char ***pinputs, int *pnum, PORT_FILE *fp);
static SWIsltsResult free_outputs(char **outputs, char **inputs, int num);
static SWIsltsResult load_trees(RT_LTREE ***ptrees, int *num_letters,
                              LQUESTION ***pquestions, int *num_questions, LM **plm, PORT_FILE *fp);
static SWIsltsResult free_trees(RT_LTREE **trees, int num_letters, LQUESTION **questions, int num_questions, LM *lm);
static SWIsltsResult load_allowable_cons_comb(LTS *lts, PORT_FILE *fp);
static SWIsltsResult free_allowable_cons_comb(LTS *lts);
static SWIsltsResult load_question_strings(LTS* lts, PORT_FILE* fp); 
static SWIsltsResult free_question_strings(LTS* lts);  
#define find_letter_index( myLet, myLM) (myLM->letter_index_for_letter[ toupper(myLet)])
int find_phone(const char *ph, PM *pm);
int find_best_string(const char *str, LTS* lts);  
int find_best_prefix_string(const char *str, LTS* lts);  
int fill_up_dp_for_letter(LTS *lts, const char *input_word, int word_len, int index, int root_start, int root_end, int left_phone);
#define in_list(myV, myQ)   (bitarray_read_bit( myQ->membership, myV))
#define qmatches(myQ, myU)  (in_list( myU->properties[ myQ->type], myQ))
int matches(LQUESTION *q1, LQUESTION *q2, int type, LDP *dp) ;
int find_output_for_dp(LTS *lts, int *pbackoff_output);
int add_output(char *output, char **output_phone_string, int out_len, int max_phone_length);
int is_allowable_cons_comb(LTS *lts, const char *cons_string);
void adjust_syllable_boundaries(LTS *lts, char **output_phone_string, int num_out, int max_phone_length);
SWIsltsResult lts_for_word(LTS *lts, char *word, int word_len, char **output_phone_string, int max_phone_length, int *num_out);

/*------------
 *
 * bitarray
 *
 *-----------*/

#define bitarray_read_bit( biTs, iBiT) ( biTs[iBiT/16] & (1<<((iBiT)%16)) )  
/* int bitarray_read_bit( unsigned short* bits, int iBit)
   {  // ASSERT( iBit<256);
   return bits[iBit/16] & (1<<((iBit)%16));  
   } */

void bitarray_write_bit( unsigned short* bits, int iBit, int iVal)
{ 
  unsigned short sect;
  ASSERT( iBit<256);
  sect = bits[iBit/16];
  if(iVal) { sect |= (1<<(iBit%16)); }
  else { sect &= ~(1<<(iBit%16)); }
  bits[ iBit/16] = sect;
}
void bitarray_populate_from_list(unsigned short* bits, char* list, int listlen)
{
  unsigned int i;
  for(i=0; i<UCHAR_MAX/sizeof(unsigned short)/8; i++) 
    bits[i] = 0;
  for(i=0; i<(unsigned int)listlen; i++) 
    bitarray_write_bit( bits, list[i], 1);
}

/*-----------
 * 
 * PHashTable
 *
 *-----------*/

static int HashCmpWord(const LCHAR *key1, const LCHAR *key2)
{ return strcmp((const char*)key1,(const char*)key2); }
static unsigned int HashGetCode(const void *key) 
{
  const char* k = (const char*)key;
  unsigned int i, len, h = 0;
  len = strlen(k);
  for (i=0; i<len; i++) h = 31*h + (unsigned int)k[i];
  return h;
}
void* my_PHashTableCreate_FromStrings( const char* strings[], int num_strings, 
				       const LCHAR* hashName)
{
  PHashTable* table = NULL;
  ESR_ReturnCode       rc = ESR_SUCCESS;
  PHashTableArgs       hashArgs;
  int i;
  hashArgs.capacity = 63;
  hashArgs.compFunction = HashCmpWord; // PHASH_TABLE_DEFAULT_COMP_FUNCTION;
  hashArgs.hashFunction = HashGetCode; // PHASH_TABLE_DEFAULT_HASH_FUNCTION;
  hashArgs.maxLoadFactor = PHASH_TABLE_DEFAULT_MAX_LOAD_FACTOR;
  rc = PHashTableCreate( &hashArgs, hashName, &table);
  for(i=0; i<num_strings; i++) {
    void* old;
    /* formerly the code used linear lookup, so let's avoid dups to match up */
    rc = PHashTableGetValue( table, strings[i], (void**)&old);
    if(rc != ESR_SUCCESS) {
      rc = PHashTablePutValue( table, strings[i], (const void *)i, NULL );
    }
  }
  return table;
}

/*---------
 *
 * i/o
 *
 *---------*/

static int load_int(PORT_FILE *fp)
{
  int v;

  PORT_FREAD_INT16((uint16 *)&v, sizeof(int), 1, fp);

  return v;
}

static SWIsltsResult load_lquestions(LQUESTION ***pquestions, int *pnum_questions, PORT_FILE *fp)
{
  int                  i, num_questions;
  LQUESTION         ** questions;
  SWIsltsResult          nRes = SWIsltsSuccess;

  num_questions = load_int(fp);

#if PRINT_LOAD_TREE_SUMMARY
  pfprintf(PSTDOUT,"loading %d questions\n", num_questions);
#endif

  *pquestions = questions = (LQUESTION**) lts_alloc(num_questions, sizeof(LQUESTION*));
  if (questions == NULL) {
    nRes = SWIsltsErrAllocResource;
    goto CLEAN_UP;
  }

  for (i=0;i<num_questions;i++) {
    questions[i] = (LQUESTION*) lts_alloc(1, sizeof(LQUESTION));
    if (questions[i] == NULL) {
      nRes = SWIsltsErrAllocResource;
      goto CLEAN_UP;
    }

#if PRINT_LOAD_TREE
    pfprintf(PSTDOUT,"LOAD_TREE: loading question %d\n", i);
#endif

    PORT_FREAD_CHAR(&(questions[i]->type), sizeof(char), 1, fp);
    PORT_FREAD_CHAR(&(questions[i]->num_list), sizeof(char), 1, fp);

    questions[i]->list = (unsigned char*) lts_alloc(questions[i]->num_list, sizeof(unsigned char));
    if (questions[i]->list == NULL) {
      nRes = SWIsltsErrAllocResource;
      goto CLEAN_UP;
    }

    PORT_FREAD_CHAR(questions[i]->list, sizeof(char), (questions[i]->num_list), fp);

    bitarray_populate_from_list( questions[i]->membership, (char*) questions[i]->list, questions[i]->num_list);
  }

  *pnum_questions = num_questions;
  return SWIsltsSuccess;

 CLEAN_UP:
  
  free_lquestions(questions, num_questions);
  *pnum_questions = 0;
  *pquestions = NULL;
  return nRes;
}

/* deallocate questions */
static SWIsltsResult free_lquestions(LQUESTION ** questions, int num_questions)
{ 
  SWIsltsResult          nRes = SWIsltsSuccess;
  int                  i;

  if (questions) {
    for (i=0; i<num_questions; i++) {
      if (questions[i]->list) {
        FREE(questions[i]->list);
        questions[i]->list = NULL;
      }
      FREE(questions[i]);
      questions[i] = NULL;
    }
    FREE(questions);
  }
  return nRes;
}

static SWIsltsResult load_letter_mapping(PORT_FILE *fp, LM **ppLetterMap)
{
  SWIsltsResult          nRes = SWIsltsSuccess;
  unsigned char        len;
  LM                 * lm;
  int                  i;

  /*  pfprintf(PSTDOUT,"got len %d\n", len);*/
  lm = (LM*) lts_alloc(1, sizeof(LM));
  if (lm == NULL) {
    nRes = SWIsltsErrAllocResource;
    goto CLEAN_UP;
  }

  PORT_FREAD_CHAR(&len, sizeof(char), 1, fp);
  lm->num_letters = len;

  lm->letters = (char*) lts_alloc(len, sizeof(char));
  if (lm->letters == NULL) {
    nRes = SWIsltsErrAllocResource;
    goto CLEAN_UP;
  }

  lm->type = (char*) lts_alloc(len, sizeof(char));
  if (lm->type == NULL) {
    nRes = SWIsltsErrAllocResource;
    goto CLEAN_UP;
  }

  PORT_FREAD_CHAR(lm->letters, sizeof(char), len, fp);
  PORT_FREAD_CHAR(lm->type, sizeof(char), len, fp);

  { 
    unsigned int letter;
    for (letter=0; letter <= UCHAR_MAX; letter++) 
      lm->letter_index_for_letter[letter] = LTS_MAXCHAR;
  }

  for (i=0;i<len;i++) {
    char letter = toupper(lm->letters[i]);
    lm->letters[i] = letter;
    lm->letter_index_for_letter[(unsigned char)letter] = i;
  }
  *ppLetterMap = lm;
  return SWIsltsSuccess;

 CLEAN_UP:
  free_letter_mapping(lm);
  *ppLetterMap = NULL;
  return nRes;
}

/* deallocate letter mapping */
static SWIsltsResult free_letter_mapping(LM *lm)
{
  SWIsltsResult          nRes = SWIsltsSuccess;

  if (lm) {
    if (lm->letters) {
      FREE(lm->letters);
      lm->letters = NULL;
    }
    if (lm->type) {
      FREE(lm->type);
      lm->type = NULL;
    }
    lm->num_letters = 0;
    FREE(lm);
  }
  return nRes;
}

static SWIsltsResult load_phone_mapping(PORT_FILE *fp, PM **ppPhoneMap)
{
  SWIsltsResult          nRes = SWIsltsSuccess;
  PM                 * pm;
  int                  i;
  unsigned char        len;
  char               * ph;

  pm = (PM*) lts_alloc(1, sizeof(PM));
  if (pm == NULL) {
    nRes = SWIsltsErrAllocResource;
    goto CLEAN_UP;
  }

  pm->num_phones = load_int(fp);

  pm->phones = (char**) lts_alloc(pm->num_phones, sizeof(char*));
  if (pm->phones == NULL) {
    nRes = SWIsltsErrAllocResource;
    goto CLEAN_UP;
  }

  for (i=0;i<pm->num_phones;i++) {
    PORT_FREAD_CHAR(&len, sizeof(unsigned char), 1, fp);

    pm->phoneH = NULL;
    pm->phones[i] = ph = (char*) lts_alloc(len+1, sizeof(char));
    if (ph == NULL) {
      nRes = SWIsltsErrAllocResource;
      goto CLEAN_UP;
    }

    PORT_FREAD_CHAR(ph, sizeof(char), len, fp);
    ph[len] = '\0';
  }
  pm->phoneH = my_PHashTableCreate_FromStrings( (const char**)pm->phones, 
						pm->num_phones, 
						L("lts.phoneH"));
  if(pm->phoneH == NULL) {
    nRes = SWIsltsErrAllocResource;
    goto CLEAN_UP;
  }
  *ppPhoneMap = pm;
  return SWIsltsSuccess;
  
 CLEAN_UP:
  free_phone_mapping(pm);
  *ppPhoneMap = NULL;

  return nRes;
}

/* deallocate phone mapping */
static SWIsltsResult free_phone_mapping(PM *pm)
{
  SWIsltsResult          nRes = SWIsltsSuccess;
  int                  i;

  if (pm) {
    if (pm->phones) {
      for (i=0; i<pm->num_phones; i++) {
        if (pm->phones[i]) {
          FREE(pm->phones[i]);
          pm->phones[i] = NULL;
        }
      }
      FREE(pm->phones);
      pm->phones = NULL;
    }
    if(pm->phoneH) 
      PHashTableDestroy( (PHashTable*)pm->phoneH);
    pm->phoneH = NULL;
    FREE(pm);
  }
  return nRes;
}


static SWIsltsResult load_outputs(char ***poutputs, char ***pinputs, int *pnum, PORT_FILE *fp)
{
  SWIsltsResult        nRes = SWIsltsSuccess;
  int                  i;
  char              ** outputs = NULL;
  char              ** inputs = NULL;
  int                  num;
  unsigned char        olen;
  char               * out;
  unsigned char        ilen;
  char               * in;

  num = load_int(fp);

  *poutputs = outputs = (char **) lts_alloc(num, sizeof(char*));
  if (outputs == NULL) {
    nRes = SWIsltsErrAllocResource;
    goto CLEAN_UP;
  }

  *pinputs = inputs = (char **) lts_alloc(num, sizeof(char*));
  if (inputs == NULL) {
    nRes = SWIsltsErrAllocResource;
    goto CLEAN_UP;
  }

  for (i=0;i<num;i++) {
    PORT_FREAD_CHAR(&olen, sizeof(char), 1, fp);
    out = outputs[i] = lts_alloc(olen + 1, sizeof(char));
    if (out == NULL) {
      nRes = SWIsltsErrAllocResource;
      goto CLEAN_UP;
    }

    if (olen > 0) {
      PORT_FREAD_CHAR(out, sizeof(char), olen, fp);
    }
    out[olen] = '\0';
    PORT_FREAD_CHAR(&ilen, sizeof(char), 1, fp);
    in = inputs[i] = lts_alloc(ilen + 1, sizeof(char));
    if (in == NULL) {
      nRes = SWIsltsErrAllocResource;
      goto CLEAN_UP;
    }

    if (ilen > 0) {
      PORT_FREAD_CHAR(in, sizeof(char), ilen, fp);
    }
    in[ilen] = '\0';
#if PRINT_LOAD_TREE
    if (ilen > 0) pfprintf(PSTDOUT,"LOAD_TREE: got input %s out %s\n", in, outputs[i]);
    pfprintf(PSTDOUT,"LOAD_TREE: outputs[%d] len %d out %x out %s\n", i, olen, outputs[i], outputs[i]);
#endif
  }

  *pnum = num;
  return SWIsltsSuccess;

 CLEAN_UP:
  
  free_outputs(outputs, inputs, num);
  *poutputs = NULL;
  *pinputs = NULL;
  *pnum = 0;

  return nRes;
}

static SWIsltsResult free_outputs(char **outputs, char **inputs, int num)
{
  SWIsltsResult          nRes = SWIsltsSuccess;
  int                  i;       

  if (outputs) {
    for (i=0; i<num; i++) {
      if (outputs[i]) {
        FREE(outputs[i]);
        outputs[i] = NULL;
      }
    }
    FREE(outputs);
  }

  if (inputs) {
    for (i=0; i<num; i++) {
      if (inputs[i]) {
        FREE(inputs[i]);
        inputs[i] = NULL;
      }
    }
    FREE(inputs);
  }
  return nRes;
}

static SWIsltsResult load_trees(RT_LTREE ***ptrees, int *num_letters,
                      LQUESTION ***pquestions, int *num_questions, LM **plm, PORT_FILE *fp)
{
  SWIsltsResult          nRes = SWIsltsSuccess;
  int                  let, i;
  RT_LTREE           * tree = NULL;
  RT_LTREE          ** trees = NULL;

#if PRINT_LOAD_TREE_SUMMARY
  pfprintf(PSTDOUT,"loading letter mapping\n");
#endif
  *ptrees = NULL;
  *pquestions = NULL;
  *plm = NULL;

  nRes = load_letter_mapping(fp, plm);
  if (nRes != SWIsltsSuccess) {
    goto CLEAN_UP;
  }

#if PRINT_LOAD_TREE_SUMMARY
  pfprintf(PSTDOUT,"loading questions\n");
#endif

  nRes = load_lquestions(pquestions, num_questions, fp);
  if (nRes != SWIsltsSuccess) {
    goto CLEAN_UP;
  }

  *num_letters = load_int(fp);

  if (*num_letters != (*plm)->num_letters) {
#ifndef NO_STDERR      
    PLogError(L("Error loading data, num_letters %d doesn't match num from mapping %d\n"), 
            *num_letters, (*plm)->num_letters);
#endif
    nRes = SWIsltsInternalErr;
    goto CLEAN_UP;
  }

  *ptrees = trees = (RT_LTREE**) lts_alloc(*num_letters, sizeof(RT_LTREE*));
  if (trees == NULL) {
    nRes = SWIsltsErrAllocResource;
    goto CLEAN_UP;
  }

  for (let=0;let<*num_letters;let++) {
    /*    pfprintf(PSTDOUT,"loading for t %d\n", t);*/

    trees[let] = tree = (RT_LTREE*) lts_alloc(1, sizeof(RT_LTREE));
    if (tree == NULL) {
      nRes = SWIsltsErrAllocResource;
      goto CLEAN_UP;
    }

    tree->num_nodes = load_int(fp);

    tree->values_or_question1 = (short*) lts_alloc(tree->num_nodes, sizeof(short));
    if (tree->values_or_question1 == NULL) {
      nRes = SWIsltsErrAllocResource;
      goto CLEAN_UP;
    }

    tree->question2 = (short*) lts_alloc(tree->num_nodes, sizeof(short));
    if (tree->question2 == NULL) {
      nRes = SWIsltsErrAllocResource;
      goto CLEAN_UP;
    }

    tree->left_nodes = (short *) lts_alloc(tree->num_nodes, sizeof(short));
    if (tree->left_nodes == NULL) {
      nRes = SWIsltsErrAllocResource;
      goto CLEAN_UP;
    }

#if PRINT_LOAD_TREE
    pfprintf(PSTDOUT,"LOAD_TREE: Tree for let %d num_nodes %d\n", let, tree->num_nodes);
#endif

    for (i=0;i<tree->num_nodes;i++) {
      PORT_FREAD_INT16(&(tree->left_nodes[i]), sizeof(short), 1, fp);
      PORT_FREAD_INT16(&(tree->values_or_question1[i]), sizeof(short), 1, fp);

#if PRINT_LOAD_TREE
      pfprintf(PSTDOUT,"LOAD_TREE:  node[%d] %d %d", i, tree->left_nodes[i], tree->values_or_question1[i]);
#endif

      PORT_FREAD_INT16(&(tree->question2[i]), sizeof(short), 1, fp);
      if (tree->left_nodes[i] != NO_NODE) {
        if (tree->question2[i] == -1) tree->question2[i] = 0;
#if PRINT_LOAD_TREE
        pfprintf(PSTDOUT," %x", (unsigned short) tree->question2[i]);
#endif
      }

#if PRINT_LOAD_TREE
      pfprintf(PSTDOUT,"\n");
#endif
    }
  }

  return SWIsltsSuccess;

 CLEAN_UP:

  free_trees(trees, *num_letters, *pquestions, *num_questions, *plm);
  *ptrees = NULL;
  *pquestions = NULL;
  *plm = NULL;
  *num_letters = 0;
  *num_questions = 0;

  return nRes;
}

/* deallocate trees */
static SWIsltsResult free_trees(RT_LTREE **trees, int num_letters, 
                       LQUESTION **questions, int num_questions, LM *lm)
{
  SWIsltsResult          nRes = SWIsltsSuccess;
  int                  i;
  RT_LTREE           * tree;

  if (lm) {
    free_letter_mapping(lm);
  }
  if (questions) {
    free_lquestions(questions, num_questions);
  }

  if (trees) {
    for (i=0; i<num_letters; i++) {
      if (trees[i]) {
        tree = trees[i];  
        if (tree->values_or_question1) {
          FREE(tree->values_or_question1);
          tree->values_or_question1 = NULL;
        }
        if (tree->question2) {
          FREE(tree->question2);
          tree->question2 = NULL;
        }
        if (tree->left_nodes) {
          FREE(tree->left_nodes);
          tree->left_nodes = NULL;
        }
        FREE(trees[i]);
        trees[i] = NULL;
      }
    }    
    FREE(trees);
  }
  return nRes;
}

static SWIsltsResult load_allowable_cons_comb(LTS *lts, PORT_FILE *fp)
{
  SWIsltsResult          nRes = SWIsltsSuccess;
  char                line[50];
  char                tempstr[50];
  char              * tok;
  int                 i, toklen;
  int                 count;
  char          seps[] = " 	\n";

  lts->num_cons_comb = 0;
  lts->allowable_cons_combH = NULL;

  while (PORT_FGETS(line, 50, fp)) {

#ifndef TI_DSP

    /*need to get rid of sme crud at the end of the line because it is being read in binary mode*/
    for (i=strlen(line)-1;i>=0;i--) {
      if (!isalpha(line[i])) line[i] = ' ';
    }
#endif
    count = 0;
    tok = safe_strtok(line, seps, &toklen);    
    tempstr[0] = '\0';

    /* get all available sequence of tokens */
    while(tok && toklen > 0){
      count += toklen;  
      strncat(tempstr, tok, toklen);
      tempstr[count+1] = '\0';
      strcat(tempstr, " ");
      count++;

      tok = safe_strtok(tok+toklen, seps, &toklen);
    }
    if (count > 0) {

        /* delete the final space */
        tempstr[count-1] = '\0';
    
        lts->allowable_cons_comb[lts->num_cons_comb] = (char*) lts_alloc(strlen(tempstr)+1, sizeof(char));
        if (lts->allowable_cons_comb[lts->num_cons_comb] == NULL) {
          nRes = SWIsltsErrAllocResource;
          goto CLEAN_UP;
        }
    
        strcpy(lts->allowable_cons_comb[lts->num_cons_comb], tempstr);
    
#if PRINT_CONS_COMB
        pfprintf(PSTDOUT,"LOAD_TREE: allowable_cons_comb[%d]: %s\n", lts->num_cons_comb, tempstr);
#endif
    
        lts->num_cons_comb++;
        if (lts->num_cons_comb >= MAX_CONS_COMB) {
#ifndef NO_STDERR      
            PLogError(L("MAX_CONS_COMB %d exceeded\n"), MAX_CONS_COMB);
#endif
          nRes = SWIsltsInternalErr;
          goto CLEAN_UP;
        }
    }
  }
  if (lts->num_cons_comb == 0) {
#ifndef NO_STDERR
    PLogError(L("Warning: the data file is missing consonant combinations - syllable boundaries will be incorrect\n"));
#endif  
  }
  lts->allowable_cons_combH = my_PHashTableCreate_FromStrings( (const char**)lts->allowable_cons_comb, lts->num_cons_comb, L("lts.allowable_cons_combH"));
  if(lts->allowable_cons_combH == NULL) {
    nRes = SWIsltsErrAllocResource;
    goto CLEAN_UP;
  }

#if PRINT_LOAD_TREE_SUMMARY
  pfprintf(PSTDOUT,"loaded %d cons combinations\n", lts->num_cons_comb);
#endif

  return SWIsltsSuccess;

 CLEAN_UP:

  free_allowable_cons_comb(lts);

  return nRes;
}

static SWIsltsResult free_allowable_cons_comb(LTS *lts)
{
  SWIsltsResult          nRes = SWIsltsSuccess;
  int                  i;

  for (i=0; i<lts->num_cons_comb; i++) {
    if (lts->allowable_cons_comb[i]) {
      FREE(lts->allowable_cons_comb[i]);
      lts->allowable_cons_comb[i] = NULL;
    }
  }
  if(lts->allowable_cons_combH) 
    PHashTableDestroy( (PHashTable*)lts->allowable_cons_combH);
  lts->allowable_cons_combH = NULL;
  return nRes;
}

static SWIsltsResult load_question_strings(LTS* lts, PORT_FILE* fp) 
{
  SWIsltsResult          nRes = SWIsltsSuccess;
  int                  i;
  int                  num;
  unsigned char        len;
  char              ** strings;
  char               * str;

  num = load_int(fp);

  lts->strings = strings = (char **) lts_alloc(num, sizeof(char*));
  lts->string_lens = (char*)lts_alloc(num, sizeof(char));

  if (strings == NULL || lts->string_lens == NULL ) {
    nRes = SWIsltsErrAllocResource;
    goto CLEAN_UP;
  }

  for (i=0;i<num;i++) {
    PORT_FREAD_CHAR(&len, sizeof(char), 1, fp);

    str = strings[i] = lts_alloc(len + 1, sizeof(char));
    if (str == NULL) {
      nRes = SWIsltsErrAllocResource;
      goto CLEAN_UP;
    }

    if (len > 0) {
      PORT_FREAD_CHAR(str, sizeof(char), len, fp);
    }
    str[len] = '\0';

    bitarray_populate_from_list( lts->membership, lts->strings[i], len);
    lts->string_lens[i] = strlen(lts->strings[i]);
  }

  // *pnum = num;
  lts->num_strings = num; 

  return SWIsltsSuccess;

 CLEAN_UP:

  free_question_strings(lts); 

  return nRes;
}

/* deallocate question strings */
static SWIsltsResult free_question_strings(LTS* lts) 
{
  SWIsltsResult          nRes = SWIsltsSuccess;
  int                  i;

  if (lts->strings) {
    for (i=0;i<lts->num_strings;i++) {
      if (lts->strings[i]) {
        FREE(lts->strings[i]);
        lts->strings[i] = NULL;
      }
    }
    FREE(lts->strings);
    if(lts->string_lens) FREE(lts->string_lens);
    lts->strings = NULL;
    lts->string_lens = NULL;
  }
  return nRes;
}


SWIsltsResult create_lts(char *data_filename, LTS_HANDLE *phLts)
{
  SWIsltsResult          nRes = SWIsltsSuccess;
  LTS                * lts;

#ifdef USE_STATIC_SLTS  
  /* TODO: language-specific ID here? */
  lts = &g_lts;

#else /* !USE_STATIC_SLTS */
  
  PORT_FILE *fp;

  lts = (LTS*) lts_alloc(1, sizeof(LTS));
  if (lts == NULL) {
    nRes = SWIsltsErrAllocResource;
    goto CLEAN_UP;
  }

  fp = PORT_FOPEN(data_filename, "rb");
  if (fp == NULL) {
#ifndef NO_STDERR
    PLogError(L("Cannot open %s\n"), data_filename);
#endif    
    nRes = SWIsltsFileOpenErr;
    goto CLEAN_UP;
  }
   nRes = load_phone_mapping(fp, &lts->phone_mapping);
   if (nRes != SWIsltsSuccess) {
     PLogError(L("SWIsltsErr: load_phone_mapping() failed: Err_code = %d\n"), nRes);
     goto CLEAN_UP;
   }

   nRes = load_question_strings(lts, fp); 
   if (nRes != SWIsltsSuccess) {
     PLogError(L("SWIsltsErr: load_question_strings() failed: Err_code = %d\n"), nRes);
     goto CLEAN_UP;
   }

   nRes  = load_outputs(&(lts->outputs), &(lts->input_for_output), &lts->num_outputs, fp);
   if (nRes != SWIsltsSuccess) {
     PLogError(L("SWIsltsErr: load_outputs() failed: Err_code = %d\n"), nRes);
     goto CLEAN_UP;
   }

#if PRINT_LOAD_TREE
  pfprintf(PSTDOUT,"LOAD_TREE: got %d outputs, loading trees\n", lts->num_outputs);
#endif

  nRes = load_trees(&(lts->trees), &(lts->num_letters),
                 &(lts->questions), &(lts->num_questions),
                 &(lts->letter_mapping),
                 fp);
  if (nRes != SWIsltsSuccess) {
    PLogError(L("SWIsltsErr: load_trees() failed: Err_code = %d\n"), nRes);
    goto CLEAN_UP;
  }

  nRes = load_allowable_cons_comb(lts, fp);
  if (nRes != SWIsltsSuccess) {
    PLogError(L("SWIsltsErr: load_allowable_cons_comb() failed: Err_code = %d\n"), nRes);
    goto CLEAN_UP;
  }

  PORT_FCLOSE(fp);

#endif /* !USE_STATIC_SLTS */

  *phLts = lts;
  return SWIsltsSuccess;

 CLEAN_UP:

  free_lts(lts);
  *phLts = NULL;
  return nRes;
}

/* deallocates LTS */
SWIsltsResult free_lts(LTS_HANDLE hlts)
{
  SWIsltsResult          nRes = SWIsltsSuccess;
  LTS                * lts = (LTS *)hlts;

  if (lts) {

#ifndef USE_STATIC_SLTS 
    free_phone_mapping(lts->phone_mapping);
    free_question_strings(lts); 
    lts->strings = NULL;
    lts->phone_mapping = NULL;

    free_outputs(lts->outputs, lts->input_for_output, lts->num_outputs);
    lts->input_for_output = lts->outputs = NULL;
    
    free_trees(lts->trees, lts->num_letters, 
               lts->questions, lts->num_questions, 
               lts->letter_mapping);
    lts->trees = NULL;
    lts->questions = NULL;
    lts->letter_mapping = NULL;
    
    free_allowable_cons_comb(lts);
    FREE(lts);
#endif /* !USE_STATIC_LTS */
  }

  return nRes;
}


int find_phone(const char *ph, PM *pm)
{
  ESR_ReturnCode rc;
  int iRet = -1;
  rc = PHashTableGetValue((PHashTable*)pm->phoneH, ph, (void**)(void*)&iRet);
  if (rc != ESR_SUCCESS) 
    PLogError("error while in find_phone(%s,%x)\n", ph, pm);
  return iRet;
}

int find_best_string(const char *str, LTS* lts) 
{
  int i, maxlen, maxi, len;
  int len_str;

  if(str[0] == '\0')   return -1;
  len_str = strlen(str);

  maxi = -1;
  maxlen = 0;
  
  for (i=0;i<lts->num_strings;i++) {
    len = lts->string_lens[i];
    if( len > len_str) 
      continue; /* no point in comparison */
    if (strncmp(str, lts->strings[i], len) == 0) {
      if (len > maxlen) {
	maxlen = len;
        maxi = i;
      }
    }
  }
  return maxi;
}

int find_best_prefix_string(const char *str, LTS* lts) 
{
  int i;
  int maxlen;
  int maxi;
  int len;
  int prelen;

  maxi = -1;
  maxlen = 0;

  prelen = strlen(str);

  for (i=0;i<lts->num_strings;i++) {
    len = lts->string_lens[i];
    if (len <= prelen) {
      if (strncmp(str + (prelen - len), lts->strings[i], len) == 0) {
        if (len > maxlen) {
          maxlen = len;
          maxi = i;
        }
      }
    }
  }
  return maxi;
}

int fill_up_dp_for_letter(LTS *lts, const char *input_word, int word_len, int index, int root_start, int root_end, int left_phone)
{
  int i,j;
  LDP *dp;
  unsigned char letter;
  int hit_wb;
  LM *lm;
  unsigned char word[MAX_WORD_LEN];
  char tempstr[MAX_WORD_LEN];
  int first_syl_end;
  int last_syl_start;

  dp = &(lts->dp);
  lm = lts->letter_mapping;

  /* the LTS decision tree does not seem to be well trained at all for 
     the letter ' when followed by "s"  ... It seems to result in the 
	 phoneme 'm', which is wrong.   "'t" seems to be OK though. 
	 BAD: Kevin's : k6v6nmz ...  pal's : palmz ... paul's : p{lz
	 BAD: janice's : jan6s6mz ... tom's house : t)mmz&h?s ... tonya's : t)ny6mz
	 BAD: jake's house : jAk6mz&h?s
	 Ignoring ' as below we get ...  
     BETTER: Kevin's : kev6nz  ... pal's : palz ... paul's : p{lz  
	 BETTER: janice's : jan6s6s ... tom's house : t)mz&h?s ... tonya's : t)ny6s
	 BETTER: jake's house : jAk6s&h?s
	 The proper solution requires a legitimate text normalizer with special 
	 handling of cases like 's which would always put a "z" there, 
	 except if preceded by an unvoiced stop (ptk) which requires a "s" there.
	 For now let's just skip the ' letter, which testing shows to be generally
	 safe (janice's, jake's etc are better but still not quite right). */

  if(input_word[index] == '\'') 
    return 1; // same as unknown character

  letter = find_letter_index(input_word[index], lm); 

  if (letter == LTS_MAXCHAR) {
  /* lisa - we need to decide how to handle this case.  Do we just silently skip unknown 
    characters or warn the app or user somehow*/
#ifdef NO_STDERR
    PrintError("unknown character on input %c - skipping\n", input_word[index], NULL, NULL);
#else    
    PLogError(L("unknown character on input %c - skipping\n"), input_word[index]);
#endif    
    return 1;
  }

  hit_wb = 0;

  /*pfprintf(PSTDOUT,"left context\n");*/

  for (j=0;j<5;j++) {
    if (hit_wb) {
      dp->properties[ Left1+j] = find_letter_index(LTS_MARKER_PIPESEP_CHAR, lm); 
    } else {
      i = index - (j+1);
      if (i < 0) dp->properties[ Left1+j] = find_letter_index(LTS_MARKER_PIPESEP_CHAR, lm); 
      else {
        dp->properties[ Left1+j] = find_letter_index(input_word[i], lm); 
        if (dp->properties[ Left1+j] == LTS_MAXCHAR) { /*assume an unknown character is a word boundary*/
          dp->properties[ Left1+j] = find_letter_index(LTS_MARKER_PIPESEP_CHAR, lm); 
          hit_wb = 1;
        }
      }
    }
  }

  /*pfprintf(PSTDOUT,"right context\n");*/

  hit_wb = 0;
  for (j=0;j<5;j++) {
    if (hit_wb) {
      dp->properties[ Right1+j] = find_letter_index(LTS_MARKER_PIPESEP_CHAR, lm); 
    } else {
      i = index + (j+1);
      if (i >= word_len) dp->properties[Right1+j] = find_letter_index(LTS_MARKER_PIPESEP_CHAR, lm); 
      else {
        dp->properties[ Right1+j] = find_letter_index(input_word[i], lm); 
        if (dp->properties[ Right1+j] == LTS_MAXCHAR) { /*assume an unknown character is a word boundary*/
          dp->properties[ Right1+j] = find_letter_index(LTS_MARKER_PIPESEP_CHAR, lm); 
          hit_wb = 1;
        }
      }
    }
  }

  dp->letter = letter; // properties[ Letter] = letter;

  dp->properties[ LeftPhone1] = left_phone;

  /*pfprintf(PSTDOUT,"word stuff\n"); */

  /*find word start and end - use unknown character as word boundaries*/

  dp->properties[ WordLen] = word_len;

  if (index == 0) dp->properties[ LetInWord] = 0;
  else if (index == word_len-1) dp->properties[ LetInWord] = 2;
  else dp->properties[ LetInWord] = 1;

  for (i=0;i<word_len;i++) {
    word[i] = find_letter_index(input_word[i], lm);  
  }

  /*figure out syllable in word - not really syllables - just looks to see if is or at first or last vowel*/
  /*  pfprintf(PSTDOUT,"syl stuff\n");*/

  first_syl_end = word_len;
  for (i=0;i<word_len;i++) {
    if (lm->type[word[i]] == 1) {
      for (j=i+1;j<word_len;j++) {
        if (lm->type[word[j]] != 1) break;
      }
      first_syl_end = j;
      break;
    }
  }
  last_syl_start = 0;
  for (i=word_len-1;i>=0;i--) {
    if (lm->type[word[i]] == 1) {
      for (j=i-1;j>=0;j--) {
        if (lm->type[word[j]] != 1) break;
      }
      last_syl_start = j;
      break;
    }
  }

#if PRINT_DP_LETTER  
  pfprintf(PSTDOUT,"first_syl_end %d last_syl_start %d\n", first_syl_end, last_syl_start);
#endif

  if (index > last_syl_start) dp->properties[ SylInWord] = 2;
  else if (index < first_syl_end) dp->properties[ SylInWord] = 0;
  else dp->properties[ SylInWord] = 1;

  first_syl_end = word_len;
  for (i=0;i<word_len;i++) {
    if (lm->type[word[i]] == 1) {
      for (j=i+1;j<word_len;j++) {
        if (lm->type[word[j]] != 1) break;
      }
      for (;j<word_len;j++) {
        if (lm->type[word[j]] == 1) break;
      }
      first_syl_end = j;
      break;
    }
  }
  last_syl_start = 0;
  for (i=word_len-1;i>=0;i--) {
    if (lm->type[word[i]] == 1) {
      for (j=i-1;j>=0;j--) {
        if (lm->type[word[j]] != 1) break;
      }
      for (;j>=0;j--) {
        if (lm->type[word[j]] == 1) break;
      }
      last_syl_start = j;
      break;
    }
  }

#if PRINT_DP_LETTER
  pfprintf(PSTDOUT,"first_syl_end %d last_syl_start %d\n", first_syl_end, last_syl_start);
#endif

  if (index > last_syl_start) dp->properties[ Syl2InWord] = 2;
  else if (index  < first_syl_end) dp->properties[ Syl2InWord] = 0;
  else dp->properties[Syl2InWord] = 1;


  first_syl_end = word_len;
  for (i=root_start;i<root_end;i++) {
    if (lm->type[word[i]] == 1) {
      for (j=i+1;j<word_len;j++) {
        if (lm->type[word[j]] != 1) break;
      }
      first_syl_end = j;
      break;
    }
  }
  last_syl_start = 0;
  for (i=root_end-1;i>=root_start;i--) {
    if (lm->type[word[i]] == 1) {
      for (j=i-1;j>=0;j--) {
        if (lm->type[word[j]] != 1) break;
      }
      last_syl_start = j;
      break;
    }
  }

#if PRINT_DP_LETTER
  pfprintf(PSTDOUT,"first_syl_end %d last_syl_start %d\n", first_syl_end, last_syl_start);
#endif

  if (index > last_syl_start) dp->properties[SylInRoot] = 2;
  else if (index < first_syl_end) dp->properties[ SylInRoot] = 0;
  else dp->properties[ SylInRoot] = 1;

  first_syl_end = word_len;
  for (i=root_start;i<root_end;i++) {
    if (lm->type[word[i]] == 1) {
      for (j=i+1;j<word_len;j++) {
        if (lm->type[word[j]] != 1) break;
      }
      for (;j<word_len;j++) {
        if (lm->type[word[j]] == 1) break;
      }
      first_syl_end = j;
      break;
    }
  }
  last_syl_start = 0;
  for (i=root_end-1;i>=root_start;i--) {
    if (lm->type[word[i]] == 1) {
      for (j=i-1;j>=0;j--) {
        if (lm->type[word[j]] != 1) break;
      }
      for (;j>=0;j--) {
        if (lm->type[word[j]] == 1) break;
      }
      last_syl_start = j;
      break;
    }
  }

#if PRINT_DP_LETTER  
  pfprintf(PSTDOUT,"first_syl_end %d last_syl_start %d\n", first_syl_end, last_syl_start);
#endif

  if (index > last_syl_start) dp->properties[Syl2InRoot] = 2;
  else if (index  < first_syl_end) dp->properties[Syl2InRoot] = 0;
  else dp->properties[Syl2InRoot] = 1;


  dp->properties[Left_DFRE] = index - root_start;
  dp->properties[Right_DFRE] = (root_end - index) - 1;


  /*  pfprintf(PSTDOUT,"strings\n");*/
#if PRINT_DP_LETTER
  pfprintf(PSTDOUT,"input word %s num_strings %d\n", input_word, lts->num_strings);
#endif

  dp->properties[RightString] = find_best_string(input_word+index+1, lts); 
  strcpy(tempstr, input_word);
  tempstr[index] = '\0';

  dp->properties[LeftString] = find_best_prefix_string(tempstr, lts);

#if PRINT_DP_LETTER
  pfprintf(PSTDOUT,"dp %c ", lm->letters[dp->letter]);

  for (i=0;i<word_len;i++) {
    pfprintf(PSTDOUT,"%c", lm->letters[word[i]]);
  }
  pfprintf(PSTDOUT," %c%c%c {%c} %c%c%c liw %d siw %d s2iw %d nw %d sir %d s2ir %d left_DFRE %d right_DFRE %d\n",
         lm->letters[dp->left_context[2]],
         lm->letters[dp->left_context[1]],
         lm->letters[dp->left_context[0]],
         lm->letters[dp->letter],
         lm->letters[dp->right_context[0]],
         lm->letters[dp->right_context[1]],
         lm->letters[dp->right_context[2]],
         dp->let_in_word,
         dp->syl_in_word,
         dp->syl2_in_word,
         dp->word_len,
         dp->syl_in_root,
         dp->syl2_in_root,
         dp->left_DFRE, dp->right_DFRE);
#endif

  return 0;
}

int matches(LQUESTION *q1, LQUESTION *q2, int type, LDP *dp) 
{
  int m1, m2;
  switch(type) {
  case 0:
    return qmatches(q1, dp);
  case 1:
    m1 = qmatches(q1, dp);
    m2 = qmatches(q2, dp);
    return(m1 && m2);
  case 2:
    m1 = qmatches(q1, dp);
    m2 = qmatches(q2, dp);
    return(m1 && !m2);
  case 3:
    m1 = qmatches(q1, dp);
    m2 = qmatches(q2, dp);
    return(!m1 && m2);
  case 4:
    m1 = qmatches(q1, dp);
    m2 = qmatches(q2, dp);
    return(!m1 && !m2);
  default:
    return -1;
  }
  /* should not come here */
  return -1;
}

int find_output_for_dp(LTS *lts, int *pbackoff_output)
{
  LDP *dp;
  int index;
  RT_LTREE *tree;
  LQUESTION *q1;
  LQUESTION *q2;
  int comb_type;
  int q2_index;
  int left_index;

  dp = &(lts->dp);
  tree = lts->trees[dp->letter]; // properties[Letter]];

  index = 0;

  while (1) {
    left_index = tree->left_nodes[index];

    if (left_index == NO_NODE) { /*means its a leaf node*/
      *pbackoff_output = tree->question2[index];
      return tree->values_or_question1[index];
    }
    q1 = lts->questions[tree->values_or_question1[index]];
    q2_index = tree->question2[index] & 0x1FFF;
    comb_type = (tree->question2[index] & 0xE000) >> 13;

    q2 = lts->questions[q2_index];

    if (matches(q1, q2, comb_type, dp)) {
      index = left_index;
    } else {
      index = left_index+1;
    }
  }
}
int add_output(char *output, char **output_phone_string, int out_len, int max_phone_length)
{
  char *tok;
  int toklen;
  char seps[] = " ";

  if (strlen(output) == 0) return out_len;

  tok = safe_strtok(output, seps, &toklen);
  while (tok && toklen) {
    if ((toklen > 0) && (strncmp(tok, "null", 4) != 0)) {

      if (isdigit(tok[toklen-1])) {
        /*means it's a vowel.  So, add a syllable boundary.  It's position 
          gets adjusted later by adjust_syllable_boundaries()*/
        strcpy(output_phone_string[out_len++], LTS_MARKER_SYLL_START);
        if (out_len >= max_phone_length) return max_phone_length;
      }      
      strncpy(output_phone_string[out_len], tok, toklen);
      output_phone_string[out_len++][toklen] = '\0';
      if (out_len >= max_phone_length) return max_phone_length;
    }
    tok = safe_strtok(tok+toklen, seps, &toklen);
  }
  return out_len;
}

int is_allowable_cons_comb(LTS *lts, const char *cons_string)
{
  /* int i;
     for (i=0;i<lts->num_cons_comb;i++) {
     #if PRINT_CONS_COMB    
     pfprintf(PSTDOUT,"checking {%s} vs c[%d] {%s}\n", cons_string, i, lts->allowable_cons_comb[i]);
     #endif    
     if (strcmp(cons_string, lts->allowable_cons_comb[i]) == 0) return 1;
     }
     return 0;
  */
  ESR_ReturnCode rc; 
  void* iVal = NULL;
  rc = PHashTableGetValue( (PHashTable*)lts->allowable_cons_combH, cons_string, &iVal);
  if(rc == ESR_SUCCESS) 
    return 1;
  else 
    return 0;
}





void adjust_syllable_boundaries(LTS *lts, char **output_phone_string, int num_out, int max_phone_length)
{
  char *out;
  int i,j;
  int syl_start;
  int stress = 0;
  int first_syl_bound;

  char tempstr[20];

  /*there should already be a syllable boundary before each vowel (add_output put one there)*/
  /*so just find these, then shift back by allowable consonant combinations and move the syllable mark*/

  for (i=0;i<num_out;i++) {
    out = output_phone_string[i];
    if (strcmp(out, LTS_MARKER_SYLL_START) == 0) { /*means there is a syllable boundary
      														 find start of allowable sequence*/

      syl_start = 0;

      for (j=i-1;j>0;j--) {
        out = output_phone_string[j];
        if (isdigit(out[strlen(out)-1])) {
          syl_start = j+1;
          break; /*means it's a vowel*/
        }
        if (strcmp(out, LTS_MARKER_WORD_START) == 0) {
          syl_start = j+1;
          break; /*don't push syl boundaries before word boundaries*/
        }
        if (strcmp(out, LTS_MARKER_PRON_START) == 0) {
          syl_start = j+1;
          break; /*don't push syl boundaries before phrase boundaries*/
        }

        /* for sequences longer than 2,
           check 3-syllable onset first, then check 2-syllable onset */
        if(j > 1){
          sprintf(tempstr, "%s %s %s", output_phone_string[j-2], output_phone_string[j-1], 
            output_phone_string[j]);
          if (!is_allowable_cons_comb(lts, tempstr)) {          
            sprintf(tempstr, "%s %s", output_phone_string[j-1], output_phone_string[j]);
            if (!is_allowable_cons_comb(lts, tempstr)) {
#if PRINT_CONS_COMB	  
              pfprintf(PSTDOUT,"cons comb %s %s not allowed\n", output_phone_string[j-1], 
                output_phone_string[j]);
#endif
              syl_start = j;
              break;        
            }
          }
        }        
        /* for sequences shorter than 2 */
        else
        {
          sprintf(tempstr, "%s %s", output_phone_string[j-1], output_phone_string[j]);
          if (!is_allowable_cons_comb(lts, tempstr)) {
#if PRINT_CONS_COMB	  
            pfprintf(PSTDOUT,"cons comb %s %s not allowed\n", output_phone_string[j-1], 
              output_phone_string[j]);
#endif
            syl_start = j;
            break;        
          }
        } 
      } /* end for j=i-1 */

      /*shift over stuff between syl_start a gap*/
      for (j=i;j>syl_start;j--) {
        strcpy(output_phone_string[j], output_phone_string[j-1]);
      }
      /*now find stress level from phone (and remove it) and add it to syl bound*/

      if (i<num_out-1) {
        out = output_phone_string[i+1];

        if (isdigit(out[strlen(out)-1])) {
          stress = atoi(out + strlen(out)-1);
        } else {
          stress = 0; /*should not happen*/
        }
      } else {
        stress = 0; /*should not happen*/
      }

      sprintf(output_phone_string[syl_start], LTS_MARKER_SYLL_START_DD, stress);
    } /* end if (strcmp(out, LTS_MARKER_SYLL_START) == 0) */
  } /* end for i=0 */

  /*remove all the stress marking from the vowels*/
  for (i=0;i<num_out;i++) {
    out = output_phone_string[i];
    if ((strncmp(out, LTS_MARKER_SYLL_START, 2) != 0) && isdigit(out[strlen(out)-1])) {
      out[strlen(out)-1] = '\0'; /*remove the stress from the vowel*/
    }
  }

  /* word boundary must be followed by syllable boundary
    if no syllable boundary exists after a word boundary, move the first
    syllable boundary to after the word boundary */
  first_syl_bound = -1;
  syl_start = -1;
  for (i=1;i<num_out;i++) {
    if ((strcmp(output_phone_string[i-1], LTS_MARKER_WORD_START) == 0) && 
      (strncmp(output_phone_string[i], LTS_MARKER_SYLL_START, 2) != 0)) {

      syl_start = i;
      /* search for first occurance of syllable boundary */
      for(j=syl_start+1;j<num_out; j++){
        out = output_phone_string[j];
        if(strncmp(out, LTS_MARKER_SYLL_START, 2) == 0 && isdigit(out[strlen(out)-1])){
            stress = atoi(out + strlen(out)-1);
            first_syl_bound = j;
            break;
        }
      }

      /* swap entries until syl bound reaches word bound */
      if(first_syl_bound >= 0){
        for(; j>syl_start; j--){
          strcpy(output_phone_string[j], output_phone_string[j-1]);
        }  
        /* put syllable boundary after word boundary */
        sprintf(output_phone_string[syl_start], LTS_MARKER_SYLL_START_DD, stress);
        
        /* advance i, reset variables */        
        i = first_syl_bound;
        first_syl_bound = syl_start = -1;

      }  
    }
  }

}


SWIsltsResult lts_for_word(LTS *lts, char *word, int word_len, char **output_phone_string, int max_phone_length, int *pnum_out)
{
  SWIsltsResult          nRes = SWIsltsSuccess;
  int                  i,j;
  int                  root_start;
  int                  root_end;
  int                  output_index;
  int                  left_phone;
  char               * input_seq;
  int                  found_match;
  int                  start_num_out;
  int                  backoff_output;
  int                  num_out;

  start_num_out = num_out = *pnum_out;

  root_start = 0;
  root_end = word_len;

  for (i=0;i<word_len;i++) {

    if ((i == 0) || (num_out == 0)) {
      /*      pfprintf(PSTDOUT,"about to call find_phone1\n");*/
      left_phone = find_phone(LTS_MARKER_PIPESEP, lts->phone_mapping);

#if PRINT_LTS_WORD
      pfprintf(PSTDOUT,"got phone %d for initial | (LTS_MARKER_PIPESEP)\n", left_phone);
#endif	
      if (left_phone < 0) {

#ifdef NO_STDERR
        PrintError("Error, cannot find | in phone mappings\n", NULL, NULL, NULL);
#else 
        PLogError(L("Error, cannot find | in phone mappings\n"));
#endif          
        nRes = SWIsltsInternalErr;
        goto CLEAN_UP;
      }
    } else {

#if PRINT_LTS_WORD        
      pfprintf(PSTDOUT,"about to call find_phone2 num_out %d\n", num_out);
      pfprintf(PSTDOUT,"out[%d] %s\n", num_out-1, output_phone_string[num_out-1]);
#endif

      if (strcmp(output_phone_string[num_out-1], LTS_MARKER_PRON_START) == 0) left_phone = find_phone(LTS_MARKER_PIPESEP, lts->phone_mapping);
      else if (strcmp(output_phone_string[num_out-1], LTS_MARKER_WORD_START) == 0) left_phone = find_phone(LTS_MARKER_PIPESEP, lts->phone_mapping);
      else left_phone = find_phone(output_phone_string[num_out-1], lts->phone_mapping);

#if PRINT_LTS_WORD    
      pfprintf(PSTDOUT,"got phone %d for %s\n", left_phone, output_phone_string[num_out-1]);
#endif

      if (left_phone < 0) {
          
#ifdef NO_STDERR
        PrintError("Error, cannot find %s in phone mappings\n", (unsigned long)output_phone_string[num_out-1], NULL, NULL);
#else 
        PLogError(L("Error, cannot find %s in phone mappings\n"), output_phone_string[num_out-1]);
#endif          
        nRes = SWIsltsInternalErr;
        goto CLEAN_UP;
      }
    }

    /*    pfprintf(PSTDOUT,"calling fill up dp\n");*/
    if (fill_up_dp_for_letter(lts, word, word_len, i, root_start, root_end, left_phone)) continue;

    /*    pfprintf(PSTDOUT,"calling find output\n");*/
    output_index = find_output_for_dp(lts, &backoff_output);

#if PRINT_LTS_WORD
    pfprintf(PSTDOUT,"got output %d\n", output_index);
#endif

    found_match = 1;

    if (strlen(lts->input_for_output[output_index]) > 0) {
        /*some extra input string to use up*/
#if PRINT_LTS_WORD	
      pfprintf(PSTDOUT,"GOT INPUT %s for %s letter %c\n", lts->input_for_output[output_index], word, word[i]);
#endif

      input_seq = lts->input_for_output[output_index];
      if (input_seq[0] == '=') {
        root_end = i;
        input_seq = input_seq+1; /*skip suffix indicator*/
      }
      for (j=i+1;;j++) {
        if (input_seq[j-(i+1)] == '\0') break;
        if (input_seq[j-(i+1)] == '-') {
          root_start = j;
          break;
        }
        if (j >= word_len) {
          found_match = 0;
          break;
        }

        if (input_seq[j-(i+1)] != word[j]) {
          found_match = 0;
          break;
        }
      }
      if (found_match) {
        i = j-1;
      }
    }

    if (!found_match) {
#if PRINT_LTS_WORD	
      pfprintf(PSTDOUT,"using backoff output %s instead of regular %s\n", 
               lts->outputs[backoff_output], 
               ts->outputs[output_index]);
#endif

      num_out = add_output(lts->outputs[backoff_output], output_phone_string, num_out, max_phone_length);
    } 
    else {
      num_out = add_output(lts->outputs[output_index], output_phone_string, num_out, max_phone_length);
    }
    if (num_out >= max_phone_length) {
      nRes = SWIsltsMaxInputExceeded;
      goto CLEAN_UP;
    }
  }

  *pnum_out = num_out;
  return SWIsltsSuccess;

 CLEAN_UP:

  *pnum_out = 0;
  return nRes;
}



SWIsltsResult run_lts(LTS_HANDLE h, FSM_DICT_HANDLE hdict, char *input_sentence, char **output_phone_string, int *phone_length)
{
  SWIsltsResult            nRes = SWIsltsSuccess;
  int                    i;
  int                    len;
  int                    num_out = 0;
  LTS                  * lts;  
  int                    was_in_phrase;
  char                   word[MAX_WORD_LEN];
  int                    num_in_word;
  int                    max_phone_length;
  int                    pron_len;

  max_phone_length = *phone_length;

  len = strlen(input_sentence);

  lts = (LTS*) h;

  was_in_phrase = 0;

  /*add a phrase start then word start at beginning*/

  strcpy(output_phone_string[num_out++], LTS_MARKER_PRON_START);
  if (num_out >= max_phone_length) {
    nRes = SWIsltsMaxInputExceeded;
    goto CLEAN_UP;
  }

  num_in_word = 0;
  pron_len = 1;    // for the first time through

  for (i=0;i<=len;i++) {

#if PRINT_LTS_WORD    
    pfprintf(PSTDOUT,"WORKING on letter %d %c\n", i, input_sentence[i]);
#endif
	
    /* Treat hyphen as word delimiter.  Not quite right for German 
       hyphenated compounds, but still an improvement. */
    if ((input_sentence[i] == ' ') || (input_sentence[i] == '-') || (input_sentence[i] == '\t') || (i == len)) {
      if (num_in_word>0 ) {
        strcpy(output_phone_string[num_out++], LTS_MARKER_WORD_START);
        if (num_out >= max_phone_length) {
          nRes = SWIsltsMaxInputExceeded;
          goto CLEAN_UP;
        }

        word[num_in_word] = '\0';
        
        if (1) {

#if PRINT_DICT_LOOKUP    
          pfprintf(PSTDOUT,"Did not find %s in dictionary\n", word);
#endif
		  pron_len = -num_out;
          nRes = lts_for_word(lts, word, num_in_word, output_phone_string, max_phone_length, &num_out);
		  pron_len += num_out; // now pron_len is the number of phonemes/markers added
		  if(pron_len == 0) 
			  num_out--; // to backspace on the LTS_MARKER_WORD_START !!
          if (nRes != SWIsltsSuccess) {
            goto CLEAN_UP;
          }
        }        
        num_in_word = 0;
      }
    } 
    else if ( (input_sentence[i] == '.') 
                || (input_sentence[i] == ',')
                || (input_sentence[i] == '!')
                || (input_sentence[i] == '?')
                || (input_sentence[i] == '\n')) {
      if (was_in_phrase) { 
        /*add a phrase boundary after lts is called*/
        if (num_in_word > 0) {
          strcpy(output_phone_string[num_out++], LTS_MARKER_WORD_START);
          if (num_out >= max_phone_length) {
            nRes = SWIsltsMaxInputExceeded;
            goto CLEAN_UP;
          }

          word[num_in_word] = '\0';
          
          if (1) {
            nRes = lts_for_word(lts, word, num_in_word, output_phone_string, max_phone_length, &num_out);
            if (nRes != SWIsltsSuccess) {
              goto CLEAN_UP;
            }
          }
          num_in_word = 0;
        }
        strcpy(output_phone_string[num_out++], LTS_MARKER_PRON_START);
        if (num_out >= max_phone_length) {
          nRes = SWIsltsMaxInputExceeded;
          goto CLEAN_UP;
        }
        was_in_phrase = 0;
      }
    }
    else {
      if (num_in_word < MAX_WORD_LEN-1) {
        word[num_in_word++] = toupper(input_sentence[i]);
        was_in_phrase = 1;
      }
    }
  }
  /*adjust syllable boundaries*/
  adjust_syllable_boundaries(lts, output_phone_string, num_out, max_phone_length);

  *phone_length = num_out;
  return SWIsltsSuccess;

 CLEAN_UP:

  *phone_length = 0;
  return nRes;
}

#ifdef USE_STATIC_SLTS
void *lts_alloc(int num, int size)
{
#ifdef NO_STDERR      
    PrintError("USE_STATIC_SLTS: lts_alloc should not be called", NULL, NULL, NULL);
#else    
    PLogError(L("USE_STATIC_SLTS: lts_alloc should not be called"));
#endif  
  return NULL;
}
#else

void *lts_alloc(int num, int size)
{
  void *p;
  p = CALLOC(num, size, MTAG);
  return p;
}
#endif /* USE_STATIC_SLTS */
