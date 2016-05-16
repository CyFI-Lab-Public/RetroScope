/*---------------------------------------------------------------------------*
 *  srec_context.c                                                           *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                         *
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


#include "stdlib.h"
#include "string.h"
#include "buildopt.h"
#include "setting.h"
#include "passert.h"
#include "pendian.h"
#include "portable.h"
#include "pstdio.h"
#include "ptypes.h"
#include "srec_context.h"
#include "srec_sizes.h"
#include "search_network.h"
#include "srec_arb.h"
#include "hmm_desc.h"
#if USE_COMP_STATS
#include "comp_stats.h"
#endif

#ifdef SET_RCSID
static const char *rcsid = 0 ? (const char *) &rcsid :
"$Id: srec_context.c,v 1.84.4.54 2008/05/15 20:06:39 dahan Exp $";
#endif


/* these are constants for now, we need to figure out how to make
   this more flexible, possible ideas are:
   (1) to specify these via API functions such as CR_LoadSyntax(),
   (2) use defaults that would have been set the main parfile,
   (3) put values for these fields in an actual grammar
*/

#define AVG_ARCS_PER_WORD    10
#define AVG_NODES_PER_WORD    7
#define AVG_CHARS_PER_WORD   18
#define MAX_PRON_LEN         1024
#define MAX_WORD_LEN 128
#define DO_WEIGHTED_ADDWORD   1
#define SLOTLOOP_OFFSET 64
#define SLOTNAME_INDICATOR "__"
#define RULE_INDICATOR '@'
#define MAX_LINE_LENGTH 512
#define DEFAULT_WB_COST 40
#define DISABLE_ARC_COST 999
#define DO_ARCS_IO_IN_ARC_ORDER 1

#define IS_SILENCE_ILABEL(ilabel,context) (ilabel >= context->hmm_ilabel_offset+EPSILON_OFFSET && ilabel<context->hmm_ilabel_offset+EPSILON_OFFSET+NUM_SILENCE_HMMS)
// looking to match filename.grxml.Names@__Names__ 
// see also grxmlcompile.cpp where these names are constructed
#define IS_SLOT_OLABEL(wd) (strchr(wd,IMPORTED_RULES_DELIM)<strstr(wd,SLOTNAME_INDICATOR) && strlen(wd)>4 && !strcmp(wd+strlen(wd)-2,SLOTNAME_INDICATOR) )

/*
SYNTAX : DATA_ALIGN(pointer, data type, bytes_filled)
EXAMPLE: We need to cast a memory address to a (wordmap*)
         so we call DATA_ALIGN(memptr, wordmap, filled),
         where FILLED contains the number of bytes that were used to align the pointer
*/
#if (CPU & CPU_ARM)||(CPU & CPU_STRONGARM)
/* under IPAQ it appears we need to align the structures
   to certain boundaries.  More attention needed here for other
   platforms ! */
#define DATA_ALIGN(x, y, z) if ((int) (x) % sizeof(y)) { \
    (z)=sizeof(y) - ((int) (x) % sizeof(y)); \
    (x) += (z); /* force correct data alignment */ \
  } else z=0;
#else
#define DATA_ALIGN(x, y, z)
#endif

static const int do_minimize = 1;
#define PTR_TO_IDX(ptr, base) ((asr_uint32_t) (ptr == NULL ? 0xFFFFFFFFu : (asr_uint32_t)(ptr - base)))
#define IDX_TO_PTR(idx, base) (idx == 0xFFFFFFFFu ? NULL : base + idx)

/* prototypes */

/*----------------------------------------------------------------------*
 *                                                                      *
 * internal prototypes                                                  *
 *                                                                      *
 *----------------------------------------------------------------------*/

/* general use functions */
char split_line(char* line, char** av);
asr_int32_t atoi_with_check(const char* buf, asr_int32_t max);
int sprintf_arc(char* buf, srec_context* fst, FSMarc* arc);
int printf_arc1(srec_context* fst, char* msg, FSMarc* arc);
int printf_node1(srec_context* fst, FSMnode* node);

/* fst_* functions are internal fst functions */
int fst_add_arcs(srec_context* fst, nodeID start_node, nodeID end_node,
                 wordID olabel, costdata cost,
                 modelID* model_sequence, int num_models);
int fst_push_arc_olabel(srec_context* fst, FSMarc* arc);
int fst_push_arc_cost(srec_context* fst, FSMarc* arc);
int fst_pull_arc_olabel(srec_context* fst, FSMarc* arc);
int fst_free_arc(srec_context* fst, FSMarc* arc);
int fst_free_node(srec_context* fst, FSMnode* node);
int fst_free_arc_net(srec_context* fst, FSMarc* arc);
arcID fst_get_free_arc(srec_context* fst);
nodeID fst_get_free_node(srec_context* fst);
int fst_pack_arc_usage(srec_context* fst);

void append_arc_arriving_node(srec_context* fst, FSMnode* to_node, FSMarc_ptr atok);
void append_arc_leaving_node(srec_context* fst, FSMnode* fr_node, FSMarc_ptr atok);
int num_arcs_leaving(srec_context* fst, FSMnode* node);

int num_arcs_arriving(srec_context* fst, FSMnode* node);
int num_arcs_arriving_gt_1(srec_context* fst, FSMnode* node);
int fst_fill_node_info(srec_context* context);
int fst_alloc_transit_points(srec_context* context);

/* higher-level functions */
int FST_LoadReverseWordGraph(srec_context* context, int num_words_to_add,
                             PFile* fp);
int FST_LoadParams(srec_context* context, PFile* fp);
int FST_AssumeParams(srec_context* context);
int FST_UnloadReverseWordGraph(srec_context* context);
int FST_AttachArbdata(srec_context* fst, srec_arbdata* allophone_tree);

static ESR_ReturnCode wordmap_clean ( wordmap *word_map );
static ESR_ReturnCode wordmap_populate ( wordmap *word_map, wordID num_words );

/*--------------------------------------------------------------------------*
 *                                                                          *
 *                                                                          *
 *                                                                          *
 *--------------------------------------------------------------------------*/

int FST_IsVoiceEnrollment(srec_context* context)
{
  if (context->olabels == NULL) return 0;
  if (context->olabels->num_words < 2) return 0;
  if (strstr(context->olabels->words[1], "enroll")) return 1;
  return 0;
}

int FST_LoadContext(const char* synbase, srec_context** pcontext,
                    int num_words_to_add)
{
  int rc;
  PFile* fp;
  char buffer[MAX_LINE_LENGTH];
  srec_context* context;

  context = (srec_context*)CALLOC_CLR(1, sizeof(srec_context), "srec.graph.base");
  if(!context)
	  return FST_FAILED_ON_MEMORY;
  memset(context, 0, sizeof(srec_context));

  context->addWordCaching_lastslot_name = 0;
  context->addWordCaching_lastslot_num = MAXwordID;
  context->addWordCaching_lastslot_needs_post_silence = ESR_FALSE;

  sprintf(buffer, "%s.map", synbase);
  fp = file_must_open(NULL, buffer, L("r"), ESR_TRUE);
  if (!fp) return FST_FAILED_ON_INVALID_ARGS;
  rc = FST_LoadWordMap(&context->olabels, num_words_to_add, fp);
  pfclose(fp);
  if (rc) return rc;

  sprintf(buffer, "%s.PCLG.txt", synbase);
  fp = file_must_open(NULL, buffer, L("r"), ESR_TRUE);
  if (!fp) return FST_FAILED_ON_INVALID_ARGS;
  rc = FST_LoadGraph(context, context->ilabels, context->olabels, num_words_to_add, fp);
  pfclose(fp);
  if (rc != FST_SUCCESS) return rc;

  sprintf(buffer, "%s.Grev2.det.txt", synbase);
  fp = file_must_open(NULL, buffer, L("r"), ESR_TRUE);
  if (!fp) return FST_FAILED_ON_INVALID_ARGS;
  rc = FST_LoadReverseWordGraph(context, num_words_to_add, fp);
  pfclose(fp);
  if (rc != FST_SUCCESS) return rc;

  sprintf(buffer, "%s.params", synbase);
  fp = file_must_open(NULL, buffer, L("r"), ESR_TRUE);
  if (fp) {
    rc = FST_LoadParams(context, fp);
    pfclose(fp);
    if (rc != FST_SUCCESS) return rc;
  } else {
    rc = FST_AssumeParams(context);
  }

  context->grmtyp = (arcID)FST_GetGrammarType(context);

  *pcontext = context;
  rc = fst_fill_node_info(context);
  return rc ? FST_FAILED_ON_INVALID_ARGS : FST_SUCCESS;
}

void FST_UnloadContext(srec_context* context)
{
  if (!context) return;

  FST_UnloadWordMap(&context->ilabels);
  FST_UnloadWordMap(&context->olabels);
  FST_UnloadGraph(context);
  FST_UnloadReverseWordGraph(context);
  FREE(context);
}

int FST_PrepareContext(srec_context* context)
{
  nodeID i;
  int rc = FST_SUCCESS;
  /* after all word additions, we need to "prepare" the context */

  /* first check for any changes to optional endnodes etc .. */
  for (i = 0; i < context->num_nodes; i++)
    if (context->FSMnode_info_list[i] == NODE_INFO_UNKNOWN)
      break;
  if (i != context->num_nodes)
    rc = fst_fill_node_info(context);

  context->whether_prepared = 1;
  return rc ? FST_FAILED_ON_INVALID_ARGS : FST_SUCCESS;
}

void fst_set_wb_costs( srec_context* context, costdata wbcost)
{
  unsigned int i;
  for(i=0; i<(unsigned int)context->FSMarc_list_len; i++) {
    if(context->FSMarc_list[i].ilabel == WORD_BOUNDARY)
      context->FSMarc_list[i].cost = wbcost;
  }
}

int FST_LoadParams(srec_context* context, PFile* fp)
{
  char line[MAX_LINE_LENGTH];
  costdata wbcost = MAXcostdata;
  while (pfgets(line, MAX_LINE_LENGTH, fp))
  {
    char* key = strtok(line," \n\r\t");
    char* val = key ? strtok(NULL," \n\r\t") : NULL;
    if(val && !strcmp(val,"="))
      val = key ? strtok(NULL," \n\r\t") : NULL;
    if(!key || !key[0])
      continue;
    else if(val && val[0]) {
      if(!strcmp(key,"word_penalty")) {
        wbcost = (costdata)atoi_with_check(val, MAXcostdata);
	if(wbcost == MAXcostdata) {
	  return FST_FAILED_ON_INVALID_ARGS;
	}
      } else {
	PLogError(L("error: unknown parameter %s in .params file"), key);
	return FST_FAILED_ON_INVALID_ARGS;
      }
    }
  }
  if(wbcost != MAXcostdata)
    fst_set_wb_costs( context, wbcost);
  return FST_SUCCESS;
}

int FST_AssumeParams( srec_context* context)
{
  fst_set_wb_costs( context, (costdata)DEFAULT_WB_COST);
  return FST_SUCCESS;
}


/*--------------------------------------------------------------------------*
 *                                                                          *
 *                                                                          *
 *                                                                          *
 *--------------------------------------------------------------------------*/

modelID hmm_number(const char* hmm_Name, modelID hmm_ilabel_offset)
{
  if (!strcmp(hmm_Name, "eps")) return EPSILON_LABEL;
  if (!strcmp(hmm_Name, ".wb")) return WORD_BOUNDARY;
  if (!strcmp(hmm_Name, ".ph")) return PHONE_BOUNDARY;
  ASSERT(hmm_Name[0] == 'h' && hmm_Name[1] == 'm' && hmm_Name[2] == 'm' && hmm_Name[3]);
  return (modelID)(hmm_ilabel_offset + (modelID)atoi_with_check(hmm_Name + 3, MAXmodelID));
}

char* hmm_name(modelID ilabel, modelID hmm_ilabel_offset, char* buf)
{
  if (ilabel == EPSILON_LABEL)
    sprintf(buf, "eps");
  else if (ilabel == WORD_BOUNDARY)
    sprintf(buf, ".wb");
  else if (ilabel == PHONE_BOUNDARY)
    sprintf(buf, ".ph");
  else
    sprintf(buf, "hmm%03d", ilabel - hmm_ilabel_offset);
  return buf;
}

/*------------------------------------------------------------------*
 *                                                                  *
 * words                                                            *
 *                                                                  *
 *------------------------------------------------------------------*/
int HashCmpWord(const LCHAR *key1, const LCHAR *key2)
{
	return LSTRCMP(key1,key2);
}
unsigned int HashGetCode(const void *key)
{
	const LCHAR* k = (const LCHAR*)key;
	unsigned int i, len, h = 0;
	len = LSTRLEN( k);
	for ( i = 0; i < len; i++)
		h = 31*h + (unsigned int)k[i];
	return h;
}

int wordmap_create(wordmap** pwmap, int num_chars, int num_words, int num_words_to_add)
{
  wordmap* Interface;
  PHashTableArgs hashArgs;
  ESR_ReturnCode rc;

  Interface = (wordmap*)CALLOC_CLR(1, sizeof(wordmap), "srec.graph.wordmap.base");
  Interface->max_words = (wordID)(num_words + num_words_to_add);
  Interface->num_words = (wordID)0;
  /* *pwmap->words = (ptr32*) CALLOC_CLR(wmap->max_words, sizeof(ptr32), "graph.wordmap.words"); */
  Interface->words = (char**) CALLOC_CLR(Interface->max_words, sizeof(char*), "srec.graph.wordmap.words");
  Interface->max_chars = num_chars + num_words_to_add * AVG_CHARS_PER_WORD;
  Interface->chars = (char*) CALLOC_CLR(Interface->max_chars, sizeof(char), "srec.graph.wordmap.chars");
  Interface->next_chars = Interface->chars;
  Interface->wordIDForWord = NULL;
  *pwmap = Interface;

  /* use a hashtable to save mapping between wdID and array index */
  if (num_words_to_add >= 0)
  {
    hashArgs.capacity = num_words + num_words_to_add + 10;
	if(hashArgs.capacity%2==0) hashArgs.capacity += 1;
    hashArgs.compFunction = HashCmpWord; // PHASH_TABLE_DEFAULT_COMP_FUNCTION;
    hashArgs.hashFunction = HashGetCode; // PHASH_TABLE_DEFAULT_HASH_FUNCTION;
    hashArgs.maxLoadFactor = PHASH_TABLE_DEFAULT_MAX_LOAD_FACTOR;
    CHKLOG(rc, PHashTableCreate(&hashArgs, L("srec.graph.wordmap.wordIDForWord.wordmap_create()"), &Interface->wordIDForWord));
  }
  else
  {
    Interface->wordIDForWord = NULL;
  }

  return FST_SUCCESS;

CLEANUP:
  wordmap_destroy(pwmap);
  return rc;
}
wordID wordmap_add_word(wordmap* wmap, const char* word);

int FST_LoadWordMap(wordmap** pwmap, int num_words_to_add, PFile* fp)
{
  wordID my_wID, wm_wID;
  char *word, *wID_string;
  int num_words;
  asr_int32_t num_chars;
  char buf[MAX_LINE_LENGTH];
  long fpos;
  wordmap* wmap;
  /* figure out the number of words */
  fpos = pftell(fp);
  for (num_words = 0, num_chars = 0; pfgets(buf, MAX_LINE_LENGTH, fp); num_words++)
  {
    word = strtok(buf, " \n\r\t");
    num_chars += strlen(word);
  }
  num_chars += num_words * 2; /* for null terminators */
  pfseek(fp, fpos, SEEK_SET);

  /* Alex added this to reuse this functionality elsewhere
    destroy function for this create function is actually taken care of by the
    FST_UnloadWordMap function
  */
  wordmap_create(&wmap, num_chars, num_words, num_words_to_add);

  /* later replace this with wordblocks, linked list of 8 character blocks! */
  while (pfgets(buf, MAX_LINE_LENGTH, fp))
  {
    word = strtok(buf, " \n\r\t");
    wID_string = strtok(NULL, " \n\r\t");
    my_wID = (wordID)atoi_with_check(wID_string, MAXwordID);

    // if(strstr(word,SLOTNAME_INDICATOR))
    //    word = strstr(word,SLOTNAME_INDICATOR);
    wm_wID = wordmap_add_word(wmap, word);
    ASSERT(my_wID == wm_wID);
  }
  ASSERT(wmap->num_words == num_words);

  for (my_wID = 1; my_wID < num_words; my_wID++)
  {
    if (!IS_SLOT_OLABEL(wmap->words[my_wID]))
      break;
  }
  wmap->num_slots = my_wID;
  wordmap_setbase(wmap);
  *pwmap = wmap;
  if(wmap->num_slots > MAX_NUM_SLOTS) 
    return FST_FAILED_ON_INVALID_ARGS;
  else 
    return FST_SUCCESS;
}

int wordmap_destroy(wordmap** wmap)
{
  if (wmap && *wmap)
  {
    wordmap_clean ( *wmap );
    if (((*wmap)->wordIDForWord)) PHashTableDestroy((*wmap)->wordIDForWord);
    if (((*wmap)->chars)) FREE((*wmap)->chars);
    if (((*wmap)->words)) FREE((*wmap)->words);
    if ((*wmap)) FREE((*wmap));
    *wmap = 0;
  }
  return FST_SUCCESS;
}

int FST_UnloadWordMap(wordmap** wmap)
{
  return wordmap_destroy(wmap);
}


int FST_DumpWordMap(PFile* fp, wordmap* wmap)
{
  wordID my_wID;
  for (my_wID = 0; my_wID < wmap->num_words; my_wID++)
  {
    pfprintf(fp, "%s %hu\n", wmap->words[my_wID], my_wID);
  }
  return FST_SUCCESS;
}

wordID wordmap_find_index(wordmap* wmap, const char* word)
{
  void *wdID_void;
  ESR_ReturnCode rc;
  size_t i;

  if (!word)
    return MAXwordID;

  if (wmap->num_words == 0)
    return MAXwordID;

  if (wmap->wordIDForWord)
  {
    rc = PHashTableGetValue(wmap->wordIDForWord, word, (void**)&wdID_void);

    if (rc == ESR_SUCCESS)
      return (wordID)(int)wdID_void;
  }
  else
  {
    for (i = 0; i < wmap->num_words; i++)
    {
      if (!strcmp(wmap->words[i], word))
      {
        return (wordID)i;
      }
    }
  }
  return MAXwordID;
}

wordID wordmap_find_rule_index(wordmap* wmap, const char* rule)
{
  int i;
  int strlen_rule = strlen(rule);
  for (i = wmap->num_slots; --i > 0;)
  { /* > (not >=) because eps is always 0 */
    char* p = strstr(wmap->words[i], SLOTNAME_INDICATOR);
    if(p != NULL){
      if (!strcmp(p, rule)) break;
      else if(!strncmp(p,rule,strlen_rule) && !strcmp(p+strlen_rule,SLOTNAME_INDICATOR)) break;
    }
  }
  if (i == 0) return MAXwordID;
  else return(wordID)i;
}

wordID wordmap_find_index_in_rule(wordmap* wmap, const char* word, wordID rule)
{
  int len = strlen(word);
  int rule0 = rule + '0';
  void *wdID_void;
  LCHAR word_dot_rule[256];
  ESR_ReturnCode rc;

  if (!word)
    return MAXwordID;
  LSTRCPY(word_dot_rule, word);
  word_dot_rule[len++] = IMPORTED_RULES_DELIM;
  word_dot_rule[len++] = (char)rule0;
  word_dot_rule[len++] = 0;
  rc = PHashTableGetValue(wmap->wordIDForWord, word_dot_rule, (void**)&wdID_void);
  if (rc == ESR_SUCCESS)
    return (wordID)(int)(wdID_void);
  return MAXwordID;
}

int strlen_with_null(const char* word)
{
#if 1 /* NT  */
  int len = strlen(word) + 1;
  if (len % 2 == 1) len++;
  return len;
#else /* C54 */
#endif
}

int wordmap_whether_in_rule(wordmap* wmap, wordID word, wordID rule)
{
  char* word_chars;
  int len;
  if (word > wmap->num_words) return 0;
  word_chars = wmap->words[word];
  len = strlen(word_chars);
  if (word_chars[len-1] == rule + '0' && word_chars[len-2] == IMPORTED_RULES_DELIM)
    return 1;
  else
    return 0;
}

void wordmap_setbase(wordmap* wmap)
{
  wmap->num_base_words = wmap->num_words;
  wmap->next_base_chars = wmap->next_chars;
}

void wordmap_ceiling(wordmap* wmap)
{
  /* this is irreversible */
  wmap->max_words = wmap->num_words;
  wmap->max_chars = (wmap->next_chars - wmap->chars);
}


static ESR_ReturnCode wordmap_clean ( wordmap *word_map )
    {
  ESR_ReturnCode  clean_status;
  PHashTableEntry *entry;
  PHashTableEntry *oldEntry;
  wordID          *value;

  clean_status = ESR_SUCCESS;

  if ( word_map->wordIDForWord == NULL )
    return clean_status;

  clean_status = PHashTableEntryGetFirst ( word_map->wordIDForWord, &entry);

  while ( ( entry != NULL ) && ( clean_status == ESR_SUCCESS ) )
    {
      clean_status = PHashTableEntryGetKeyValue ( entry, NULL, (void **)&value );

      if ( clean_status == ESR_SUCCESS )
	{
	  oldEntry = entry;
	  clean_status = PHashTableEntryAdvance ( &entry );

	  if ( clean_status == ESR_SUCCESS )
	    clean_status = PHashTableEntryRemove ( oldEntry );
	}
    }
    return ( clean_status );
    }


static ESR_ReturnCode wordmap_populate ( wordmap *word_map, wordID num_words )
{
  ESR_ReturnCode  populate_status;
  wordID          wdID;
  
  populate_status = ESR_SUCCESS;
  wdID = 0;
  
  if ( word_map->wordIDForWord != NULL )
    {
      while ( ( wdID < num_words ) && ( populate_status == ESR_SUCCESS ) )
	{
	  populate_status = PHashTablePutValue ( word_map->wordIDForWord, word_map->words[wdID],
						 (const void *)(int)wdID, NULL );
	  if ( populate_status == ESR_SUCCESS )
	    wdID++;
	  else {
	    return (populate_status);
	  }
	}
    }
  return ( populate_status );
}

void wordmap_reset(wordmap* wmap)
{
  char** tmp_words;
  int i;
  ESR_ReturnCode    reset_status;

  if (wmap->num_base_words < wmap->num_words)
  {
    /*wordID i = (wordID)(wmap->num_base_words);*/
    char* old_wmap_chars = wmap->chars;                       
    int offset = wmap->next_base_chars - wmap->chars;
    char* tmp_chars = NEW_ARRAY(char, offset, L("srec.g2g.graph.wordmap.chars"));
    if(!tmp_chars) {
      passert ( 0 && L("failed to reset the memory for wordmap.chars ") );
    }
    memcpy(tmp_chars,wmap->chars, offset * sizeof(char));
    FREE(wmap->chars);
    
    wmap->chars = tmp_chars;
    wmap->next_base_chars = wmap->chars + (wmap->next_base_chars - old_wmap_chars);
    wmap->max_chars = (wordID) offset;
    wmap->next_chars = wmap->next_base_chars;
    
    tmp_words = (char**) CALLOC_CLR(wmap->num_base_words, sizeof(char*), "srec.graph.wordmap.words");
    if(!tmp_words) {
      passert ( 0 && L("failed to reset the memory for wordmap.words ") );
    }
    memcpy( tmp_words, wmap->words, wmap->num_base_words * sizeof(char*));
    FREE( wmap->words);
    
    wmap->words = tmp_words;
    wmap->max_words = wmap->num_base_words;
    wmap->num_words  = wmap->num_base_words;
    
    for(i=0; i<wmap->num_words; i++)
      wmap->words[i] = wmap->chars + (wmap->words[i] - old_wmap_chars);
  }
  
  reset_status = wordmap_clean ( wmap );
  
  if ( reset_status == ESR_SUCCESS )
    {
      reset_status = wordmap_populate ( wmap, wmap->num_base_words );
      
      if ( reset_status != ESR_SUCCESS )
	{
	  wordmap_clean ( wmap );
	  passert ( 0 && L("wordmap_reset failed") );
	}
    }
  else
    {
      passert ( 0 && L("wordmap_reset failed") );
    }
}

#define FST_GROW_FACTOR 12/10
#define FST_GROW_MINCHARS 256
#define FST_GROW_MINWORDS  32
wordID wordmap_add_word(wordmap* wmap, const char* word)
{
  int len = strlen_with_null(word);
  wordID wdID =0;

  if (wmap->next_chars + len >= wmap->chars + wmap->max_chars)
  {
#if defined(FST_GROW_FACTOR)
     int i,tmp_max_chars= wmap->max_chars * FST_GROW_FACTOR;
	 char* old_wmap__chars = wmap->chars;
      if(tmp_max_chars - wmap->max_chars < FST_GROW_MINCHARS)
	tmp_max_chars +=FST_GROW_MINCHARS;

      char* tmp_chars = NEW_ARRAY(char, tmp_max_chars, L("srec.g2g.graph.wordmap.chars"));
      if(!tmp_chars) {
          PLogError(L("ESR_OUT_OF_MEMORY: Could not extend allocation of wordmap.chars"));
          return MAXwordID;
      }
      memcpy(tmp_chars,wmap->chars,wmap->max_chars * sizeof(char));
      FREE(wmap->chars);
      
      wmap->chars = tmp_chars;
      wmap->next_chars = wmap->chars + (wmap->next_chars - old_wmap__chars);
      wmap->next_base_chars = wmap->chars + (wmap->next_base_chars - old_wmap__chars); 
      wmap->max_chars = (wordID)tmp_max_chars;
      //Remove old keys --- Add WORD
      wordmap_clean (wmap );
      
      // adjust word pointers
      for(i=0; i<wmap->num_words; i++)
	{
	  wmap->words[i] = wmap->chars + (wmap->words[i] - old_wmap__chars);
	  // adjust hashtable ---
	  if (wmap->wordIDForWord)
	    {
	      ESR_ReturnCode rc = PHashTablePutValue ( wmap->wordIDForWord, wmap->words[i],
						       (const void *)i, NULL );
	      if ( rc != ESR_SUCCESS ) {
		goto CLEANUP;
	      }
	    }
	}
#else // so not defined(FST_GROW_FACTOR)
      PLogError("error: char overflow in wmap %d max %d\n", (int)(wmap->next_chars - wmap->chars), wmap->max_chars);
      return MAXwordID;
#endif // defined(FST_GROW_FACTOR)
  }
  //Add word
  if (wmap->num_words == wmap->max_words)
    {
#if defined(FST_GROW_FACTOR)
      char** tmp_words;
      wordID tmp_max_words;
      int itmp_max_words =  wmap->max_words * FST_GROW_FACTOR;
      
      if(itmp_max_words - wmap->max_words < FST_GROW_MINWORDS)
	    itmp_max_words += FST_GROW_MINWORDS;

      if( itmp_max_words >= MAXwordID) {
	    PLogError("error: word ptr overflow in wmap %d max %d\n", (int)wmap->num_words, wmap->max_words);
	    return MAXwordID;
      }
      tmp_max_words = (wordID)itmp_max_words;
	  tmp_words = (char**) CALLOC_CLR(tmp_max_words, sizeof(char*), "srec.graph.wordmap.words");
      if(!tmp_words) {
	PLogError(L("ESR_OUT_OF_MEMORY: Could not extend allocation of wordmap.words"));
        return MAXwordID;
      }
      memcpy( tmp_words, wmap->words, wmap->num_words * sizeof(char*));
      FREE( wmap->words);
      wmap->words = tmp_words;
      wmap->max_words = tmp_max_words;
#else // so not defined(FST_GROW_FACTOR)
      PLogError("error: word ptr overflow in wmap %d max %d\n", (int)wmap->num_words, wmap->max_words);
      return MAXwordID;
#endif // defined(FST_GROW_FACTOR)
    }
  if(1)
    {
    strcpy(wmap->next_chars, word);
    wmap->words[ wmap->num_words++] = wmap->next_chars;
    wmap->next_chars += len;
    wdID = (wordID)(wmap->num_words - (wordID)1);
    if (wmap->wordIDForWord)
    {
       ESR_ReturnCode rc = PHashTablePutValue ( wmap->wordIDForWord, wmap->words[wdID],
                      (const void *)(int)wdID, NULL );
    if ( rc != ESR_SUCCESS )
      goto CLEANUP;
    }
    return wdID;
  }
CLEANUP:
  PLogError("error: could not add word and wordID in wmap hash (%s -> %d)\n", word, wdID);
  return MAXwordID;
}

wordID wordmap_add_word_in_rule(wordmap* wmap, const char* word, wordID rule)
{
  int len = strlen_with_null(word) + 2;
  wordID wdID = 0;
  wordID i;
//wordmap_add_word_in_rule
  if (wmap->next_chars + len >= wmap->chars + wmap->max_chars)
  {
#if defined(FST_GROW_FACTOR)
      int tmp_max_chars = wmap->max_chars * FST_GROW_FACTOR;
      char* tmp_chars;
	  char* old_next_chars = wmap->next_chars;
	  char* old_chars = wmap->chars;

      if(tmp_max_chars-wmap->max_chars < FST_GROW_MINCHARS )
	    tmp_max_chars += FST_GROW_MINCHARS;
      if( wmap->chars + tmp_max_chars <= wmap->next_chars + len)
	    tmp_max_chars += len;

      tmp_chars = NEW_ARRAY(char, tmp_max_chars, L("srec.g2g.graph.wordmap.chars"));
    if(!tmp_chars) {
          PLogError(L("ESR_OUT_OF_MEMORY: Could not extend allocation of wordmap_add_in_rule.chars"));
          return MAXwordID;
      }
      memcpy(tmp_chars, wmap->chars, wmap->max_chars * sizeof(char));
      FREE(wmap->chars);

      wmap->chars = tmp_chars;
      wmap->next_chars = wmap->chars + (old_next_chars - old_chars) ;
      wmap->next_base_chars = wmap->chars + (wmap->next_base_chars - old_chars); 
      wmap->max_chars = (wordID)tmp_max_chars;
      
      //Remove old keys
      wordmap_clean ( wmap );
      
      // adjust word pointers wordmap_add_word_in_rule
      for(i=0; i<wmap->num_words; i++)
	{
 	  wmap->words[i] = wmap->chars +(wmap->words[i] - old_chars) ;
	  // adjust hashtable ----- add in rulewordmap_add_word_in_rule ----
	  if (wmap->wordIDForWord) {
	    ESR_ReturnCode rc = PHashTablePutValue ( wmap->wordIDForWord, wmap->words[i],
						     (void*)(int)(i), NULL );
	    if ( rc != ESR_SUCCESS )
	      goto CLEANUP;
	  }
	}
#else
      PLogError("error: char overflow in wmap %d max %d\n", (int)(wmap->next_chars - wmap->chars), wmap->max_chars);
      return MAXwordID;
#endif
  }//wordmap_add_word_in_rule
  if (wmap->num_words == wmap->max_words)
    {
#if defined(FST_GROW_FACTOR)
      char** tmp_words;
      wordID tmp_max_words;
      int itmp_max_words =  wmap->max_words * FST_GROW_FACTOR;
      if(itmp_max_words - wmap->max_words < FST_GROW_MINWORDS)
	itmp_max_words += FST_GROW_MINWORDS;
      
      if( itmp_max_words >= MAXwordID) {
	PLogError("error: word ptr overflow in wmap %d max %d\n", (int)wmap->num_words, wmap->max_words);
	return MAXwordID;
      }
      
      tmp_max_words = (wordID)itmp_max_words;
      tmp_words = (char**) CALLOC_CLR(tmp_max_words, sizeof(char*), "srec.graph.wordmap.words");
      if(!tmp_words) {
        PLogError(L("ESR_OUT_OF_MEMORY: Could not extend allocation of wordmap_add_rule.words"));
        return MAXwordID;
      }
      memcpy( tmp_words, wmap->words, wmap->num_words * sizeof(char*));
      FREE( wmap->words);
      wmap->words = tmp_words;
      wmap->max_words = tmp_max_words;
#else
      PLogError("error: word ptr overflow in wmap %d max %d\n", (int)wmap->num_words, wmap->max_words);
      return MAXwordID;
#endif
    }
  if(1)
  {
    char *p;
    const char *q;
    /* word.1 */
    for (p = wmap->next_chars, q = word; (*p = *q) != '\0'; p++, q++) ; /* basic strcpy() */
    *p++ = IMPORTED_RULES_DELIM;
    *p++ = (char)(rule + ((wordID)'0'));
    *p   = '\0';
    wmap->words[ wmap->num_words++] = wmap->next_chars;
    wmap->next_chars += len;
    wdID = (wordID)(wmap->num_words - (wordID)1);
    if (wmap->wordIDForWord)
      {
        ESR_ReturnCode rc = PHashTablePutValue ( wmap->wordIDForWord, wmap->words[wdID],
						 (void*)(int)(wdID), NULL );
	if ( rc != ESR_SUCCESS )
	  goto CLEANUP;
      }
    return wdID;
  }
CLEANUP:
  PLogError("error: could not add word and wordID in wmap hash (%s -> %d)\n", word, wdID);
  return MAXwordID;
}

/*----------------------------------------------------------------------*
 *                                                                      *
 * API                                                                  *
 *                                                                      *
 *----------------------------------------------------------------------*/

int FST_AttachArbdata(srec_context* fst, srec_arbdata* allophone_tree)
{
  int rc = 0;
  unsigned int allotree__modelid;

  fst->allotree = allophone_tree;
  if( !allophone_tree)
      return 1;
  fst->hmm_info_for_ilabel = allophone_tree->hmm_infos - fst->hmm_ilabel_offset;
  allotree__modelid = version_arbdata_models(allophone_tree);
  if (allotree__modelid != 0 && fst->modelid != 0)
  {
    if (allotree__modelid != fst->modelid)
    {
      PLogError("Error: modelids disagree, sgcbaseline(%u) arbdata(%u)", fst->modelid, allotree__modelid);
      rc = FST_FAILED_ON_INVALID_ARGS;
    }
  }

  return rc;
}

int FST_LoadGraph(srec_context* pfst, wordmap* imap, wordmap* omap,
                  int num_words_to_add, PFile* fp)
{
  int i, rc = 0;
  char line[MAX_LINE_LENGTH], *args[32], nargs;
  arcID max_num_FSMarcs;
  arcID max_num_FSMnodes;
  nodeID from_node, last_from_node, into_node, num_nodes, max_node_number = 0;
  FSMarc_ptr atok =  (FSMarc_ptr)0;
  FSMarc *atoken = NULL;
  FSMnode *fr_node, *to_node;
  costdata cost = FREEcostdata;
  arcID num_arcs;

  srec_context* fst = pfst;
  char *ilabel_str = 0, *olabel_str = 0;
  long fpos;
  arcID new_arc_id;
  asr_int32_t temp;


  /* determine number of arcs and nodes to allocate, add 50% for dynamic */
  fpos = pftell(fp);
  max_num_FSMnodes = 0;
  pfst->modelid = 0;
  for (max_num_FSMarcs = 0; pfgets(line, sizeof(line) / sizeof(char), fp); max_num_FSMarcs++)
  {
    if (strstr(line, "modelid:") == line)
    {
      char *p;
      pfst->modelid = strtoul(line + strlen("modelid:"), &p, 10);
    }
    from_node = (nodeID)atoi_with_check(line, MAXnodeID);
    if (from_node > max_num_FSMnodes)
      max_num_FSMnodes = from_node;
  }
  pfseek(fp, fpos, SEEK_SET);
  temp = max_num_FSMnodes + 1 /*why+1?*/ + num_words_to_add * AVG_NODES_PER_WORD;
  if (temp >= MAXnodeID)
  {
    max_num_FSMnodes = MAXnodeID - 1;
    PLogMessage("Warning: using max nodes instead\n");
  }
  else
  {
    max_num_FSMnodes = (nodeID)temp;
  }
  temp = max_num_FSMarcs + num_words_to_add * AVG_ARCS_PER_WORD;
  if (temp >= MAXarcID)
  {
    max_num_FSMarcs = MAXarcID - 1;
    PLogMessage("Warning: using max arcs instead\n");
  }
  else
  {
    max_num_FSMarcs = (arcID)temp;
  }
  fst->olabels = omap;
  if (imap)
  {
    /* generally no imap is specified */
    fst->ilabels = imap;
    fst->hmm_ilabel_offset = wordmap_find_index(fst->ilabels, "hmm0");
    ASSERT(fst->hmm_ilabel_offset >= 0);
  }
  else
  {
    fst->ilabels = (wordmap*)CALLOC_CLR(1, sizeof(wordmap), "srec.graph.imap");
    fst->ilabels->num_words = fst->ilabels->max_words = 0;
    fst->ilabels->words = 0;
    /* this is bad hard code, we can get this from the swiarb file, it is
       equal to the number of phonemes (53 for enu, 39 for jpn) plus the special
       models (eps,.wb,.ph,h#,#h,iwt,not) minus 1 ('cuz base number is 0) */
    fst->hmm_ilabel_offset = 128; /* should match MAX_PHONEMES */
  }
  fst->FSMarc_list = (FSMarc*)CALLOC_CLR(max_num_FSMarcs, sizeof(FSMarc), "srec.graph.arcs");
  fst->FSMnode_list = (FSMnode*)CALLOC_CLR(max_num_FSMnodes, sizeof(FSMnode), "srec.graph.nodes");
  fst->FSMnode_info_list = (FSMnode_info*)CALLOC_CLR(max_num_FSMnodes, sizeof(char), "srec.graph.nodeinfos");

  /* setup the arc freelist */
  fst->FSMarc_freelist = 0;
  fst->num_arcs = 0;
  for (i = 0; i < max_num_FSMarcs - 1; i++)
  {
    fst->FSMarc_list[i].linkl_next_arc = ARC_ItoX(i + 1);
    fst->FSMarc_list[i].linkl_prev_arc = FSMARC_FREE;
  }
  fst->FSMarc_list[i].linkl_next_arc = FSMARC_NULL;
  fst->FSMarc_list[i].linkl_prev_arc = FSMARC_FREE;

  /* initialize the nodes, 'cuz reading is random order */
  fst->num_nodes = 0;
  for (i = 0; i < max_num_FSMnodes; i++)
  {
    fr_node = &fst->FSMnode_list[i];
    fr_node->un_ptr.first_next_arc = fr_node->first_prev_arc = FSMARC_NULL;
  }

  /* 1. first load up all the information */
  IF_DEBUG_WDADD(printf("load graph ... 1\n"));
  num_arcs = 0;
  last_from_node = MAXnodeID;
  while (pfgets(line, sizeof(line) / sizeof(char), fp))
  {
    if (strstr(line, "modelid:") == line) continue;
    IF_DEBUG_WDADD(printf("read arc %s", line));
    nargs = split_line(line, args);
    if (nargs >= 4)
    {
      from_node = (nodeID)atoi_with_check(args[0], MAXnodeID);
      into_node = (nodeID)atoi_with_check(args[1], MAXnodeID);
      ilabel_str = args[2];
      olabel_str = args[3];
      cost = FREEcostdata;
      if (nargs == 5)
        PLogError(L("Warning: too many arguments on line %s"), line);
    }
    else if (nargs == 1)
    {
      from_node = (nodeID)atoi_with_check(args[0], MAXnodeID);
      into_node = MAXnodeID;
      ilabel_str = 0;
      olabel_str = 0;
      cost = FREEcostdata;
    }
    else
    {
      from_node = into_node = 0;
      PLogError("can't parse line %s\n", line);
      ASSERT(0);
    }

    if (into_node == MAXnodeID)
    {
      fst->end_node = from_node;
    }
    else
    {
      new_arc_id = fst_get_free_arc(fst);
      if (new_arc_id == MAXarcID)
        return FST_FAILED_ON_MEMORY;
      atok = ARC_ItoX(new_arc_id);
      atoken = ARC_XtoP(atok);
      num_arcs++;
      fr_node = &fst->FSMnode_list[from_node];
      to_node = &fst->FSMnode_list[into_node];
      if (fst->ilabels->num_words == 0)
      {
        atoken->ilabel = hmm_number(ilabel_str, fst->hmm_ilabel_offset);
	/* Xufang: if olabel_str is a slotname, change the input label to a no-silence and no-eps HMM. It is true that the weight of slot arcs is 999 and hmm will not take effect in the runtime, but it was found that when the slot is a loop, eps and silence will cause the recursive function fst_node_has_speech_to_come into a no-step-out status.*/
	if(strstr(olabel_str, SLOTNAME_INDICATOR)!=NULL)
	  atoken->ilabel = fst->hmm_ilabel_offset + SLOTLOOP_OFFSET;
      }
      else
      {
        atoken->ilabel = wordmap_find_index(fst->ilabels, ilabel_str);
      }
      atoken->olabel = wordmap_find_index(fst->olabels, olabel_str);
      /* if(g_fst_options.wtw_cost_override>0) {
      if(atoken->ilabel == WORD_BOUNDARY)
      atoken->cost = g_fst_options.wtw_cost_override;
      else
      atoken->cost = g_fst_options.arc_cost_override;
      } else  */
      atoken->cost = cost;
#if DEBUG_WDADD
      atoken->ilabel_str = (atoken->ilabel < fst->ilabels->num_words ? fst->ilabels->words[atoken->ilabel] : 0);
      atoken->olabel_str = (atoken->olabel < fst->olabels->num_words ? fst->olabels->words[atoken->olabel] : 0);
#endif
      append_arc_leaving_node(fst, fr_node, atok);
      if (into_node > max_node_number)
        max_node_number = into_node;
      append_arc_arriving_node(fst, to_node, atok);
      atoken->fr_node = NODE_ItoX(from_node);
      atoken->to_node = NODE_ItoX(into_node);
    }
    last_from_node = from_node;
  }
  ASSERT(fst->num_arcs == num_arcs);

  /* setup the node freelist */
  IF_DEBUG_WDADD(printf("load graph ... 6\n"));
  num_nodes = (nodeID)(max_node_number + 1);
  if( max_num_FSMnodes > num_nodes) {
  fst->FSMnode_freelist = num_nodes;
  for (i = num_nodes; i < (max_num_FSMnodes - 1); i++)
  {
    fst->FSMnode_list[i].un_ptr.next_node = NODE_ItoX(i + 1);
    fst->FSMnode_list[i].first_prev_arc = FSMARC_FREE;
  }
	if (i == (max_num_FSMnodes - 1)) {
     fst->FSMnode_list[i].un_ptr.next_node = FSMNODE_NULL;
     fst->FSMnode_list[i].first_prev_arc = FSMARC_FREE;
    }
  } else
	fst->FSMnode_freelist  = FSMNODE_NULL;

  /* some book-keeping stuff */
  IF_DEBUG_WDADD(printf("load graph ... 7\n"));
  fst->num_base_arcs = fst->num_arcs = num_arcs;
  fst->FSMarc_list_len = max_num_FSMarcs;
  fst->FSMnode_list_len = max_num_FSMnodes;
  fst->num_base_nodes = fst->num_nodes = num_nodes;
  fst->start_node = 0;
  /* fst->end_node = 0; this is set up above */

  /* beg and end word labels */
  fst->beg_silence_word = wordmap_find_index(fst->olabels, "-pau-");
  fst->end_silence_word = wordmap_find_index(fst->olabels, "-pau2-");
  fst->hack_silence_word = wordmap_find_index(fst->olabels, "silence");

  /* set the search limitations, real ones will be registered later */
  fst->max_searchable_nodes = 0;
  fst->max_searchable_arcs = 0;

  rc = fst_alloc_transit_points(fst);
  return (rc ? FST_FAILED_ON_INVALID_ARGS : FST_SUCCESS);
}

int FST_UnloadGraph(srec_context* pfst)
{
  if (pfst->ilabels)
    FREE(pfst->ilabels);
  FREE(pfst->FSMarc_list);
  FREE(pfst->FSMnode_list);
  FREE(pfst->FSMnode_info_list);
  pfst->FSMarc_list = 0;
  pfst->FSMnode_list = 0;
  pfst->FSMnode_info_list = 0;
  return FST_SUCCESS;
}

int FST_DumpGraph(srec_context* fst, PFile* fp)
{
  int rc = 0;
  nodeID i;
  FSMarc_ptr atok;
  FSMarc* atoken;
  nodeID from_node, into_node;
  FSMnode* ntoken;
  char *ilabel, *olabel;

  for (i = 0; i < fst->num_nodes; i++)
  {
    from_node = i;
    ntoken = &fst->FSMnode_list[i];
    if (ntoken->first_prev_arc == FSMARC_FREE)
      continue;
    if (ntoken->un_ptr.first_next_arc != FSMARC_NULL)
    {
      for (atok = ntoken->un_ptr.first_next_arc; atok != FSMARC_NULL; atok = atoken->linkl_next_arc)
      {
        char buf[32];
        atoken = ARC_XtoP(atok);
        into_node = NODE_XtoI(atoken->to_node);
        ilabel = fst->ilabels->num_words == 0 ?
                 hmm_name(atoken->ilabel, fst->hmm_ilabel_offset, buf) :
                 fst->ilabels->words[atoken->ilabel] ;
        olabel = fst->olabels->words[atoken->olabel];

        if (atoken->cost != FREEcostdata)
        {
          /* regular arc */
          pfprintf(fp, "%hu\t%hu\t%s\t%s\t%hu\n",
                  from_node, into_node, ilabel, olabel, atoken->cost);
        }
        else
        {
          /* regular zero cost arc */
          pfprintf(fp, "%hu\t%hu\t%s\t%s\n",
                  from_node, into_node, ilabel, olabel);
        }
      }
    }
    else
    {
      pfprintf(fp, "%hu\n", from_node);
    }
  }
  return rc;
}

int FST_AddWordToGrammar(srec_context* fst, const char* _slot,
                         const char* word, const char* pron,
                         const int cost)
{
  int num_pron_ampersands = 0, num_prons_added = 0, num_words_added = 0;
  int i, pron_len, model_sequence_len;
  modelID model_sequence[MAX_PRON_LEN];
  char phoneme_sequence[MAX_PRON_LEN];
  wordID olabel = MAXwordID;
  int irc, rc = FST_SUCCESS;
  nodeID start_node = MAXnodeID, end_node = MAXnodeID;
  char veslot[MAX_WORD_LEN];
#if USE_HMM_BASED_ENROLLMENT
  const char* Tpron;
#endif

  /* The addword from voice enroll still use @, and to test voice enroll, @ sign was removed and __ was added at the begining and ending of a word. Xufang */
  if(_slot[0] == '@') {
    strcpy(veslot,SLOTNAME_INDICATOR);
    strcat(veslot,_slot+1);
    strcat(veslot,SLOTNAME_INDICATOR);
  } else
    strcpy(veslot, _slot);

#if USE_COMP_STATS
  if (!comp_stats)
    comp_stats = init_comp_stats1();
  start_cs_clock1(&comp_stats->word_addition);
#endif

  /* we expect developers to call AddWord with the same slot many times,
  so we cache the slot start and end nodes and re-use on subseq calls */
  if( fst->addWordCaching_lastslot_num == MAXwordID )
    fst->addWordCaching_lastslot_name = NULL;
  else {
    fst->addWordCaching_lastslot_name = fst->olabels->words[fst->addWordCaching_lastslot_num];
    fst->addWordCaching_lastslot_name = strstr(fst->addWordCaching_lastslot_name, SLOTNAME_INDICATOR);
    ASSERT( fst->addWordCaching_lastslot_name);
  }
  if( fst->addWordCaching_lastslot_name==NULL || strcmp( fst->addWordCaching_lastslot_name, veslot)) {

    fst->addWordCaching_lastslot_num = wordmap_find_rule_index(fst->olabels, veslot);
    /* olabel not found is a user error */
    if (fst->addWordCaching_lastslot_num == MAXwordID)
    {
      size_t i;

      pfprintf(PSTDOUT, L("error: slot '%s' not found among ["), veslot);
      for (i = 1; i < (size_t) fst->olabels->num_slots; ++i)
        pfprintf(PSTDOUT, "%s, ", fst->olabels->words[i]);
      pfprintf(PSTDOUT, L("] possible\n"));
      rc = FST_FAILED_ON_INVALID_ARGS;
      goto RETRC;
    }

    fst->addWordCaching_lastslot_name = fst->olabels->words[fst->addWordCaching_lastslot_num];
    fst->addWordCaching_lastslot_name = strstr(fst->addWordCaching_lastslot_name, SLOTNAME_INDICATOR);
    ASSERT(fst->addWordCaching_lastslot_name);

    /* now find where in the graph this slot is referenced, note that
       there might be more than one place, but here we ignore multiples */
    for (i = fst->num_fsm_exit_points; --i >= 0;)
      {
	arcID arcid = fst->fsm_exit_points[i].arc_index;
	if (fst->FSMarc_list[arcid].olabel == fst->addWordCaching_lastslot_num)
	  {
	    FSMarc* arc;
	    FSMnode* node;
	    start_node = fst->fsm_exit_points[i].from_node_index;
	    end_node = fst->fsm_exit_points[i].wbto_node_index;
	    fst->addWordCaching_lastslot_needs_post_silence = ESR_TRUE;
	    fst->addWordCaching_lastslot_ifsm_exit_point = i;
	    node = &fst->FSMnode_list[ end_node];
	    arc = &fst->FSMarc_list[node->un_ptr.first_next_arc];
	    if (arc->olabel == fst->end_silence_word && arc->linkl_next_arc == MAXarcID)
	      fst->addWordCaching_lastslot_needs_post_silence = ESR_FALSE;
	    break;
	  }
      }

    /* not found in the graph is an internal error, coding error */
    if (i < 0 || start_node>=fst->num_nodes || end_node>=fst->num_nodes)
      {
	PLogError("error: (internal) finding olabel %d %d %d\n", fst->addWordCaching_lastslot_num,
		  start_node, end_node);
	goto RETRC;
      }
  } /* cached or not */

  i = fst->addWordCaching_lastslot_ifsm_exit_point;
  start_node = fst->fsm_exit_points[i].from_node_index;
  end_node = fst->fsm_exit_points[i].wbto_node_index;

  /* now start_node and end_node are known */
  if (!word || !*word || !pron || !*pron)
    {
      rc = FST_FAILED_ON_INVALID_ARGS;
      PLogError("error: null word/pron on input to FST_AddWordToGrammar()\n");
      goto RETRC;
    }

  /* from here */
  IF_DEBUG_WDADD(printf("Adding %s %s\n", word, (const char*)pron));

  /* loop over all prons, we break when we hit the double-null (hence the +1) */
  for( ; (*pron)!='\0'; pron+=(pron_len+1)) {
    pron_len = strlen((char*)pron);
    if (pron_len >= MAX_PRON_LEN)
      {
	PLogError("error: wordadd failed on word %s due to pron [%s] too long\n", word, (char*)pron);
	rc = FST_FAILED_ON_INVALID_ARGS;
	goto RETRC;
      }

    /* check for searchability after adding, estimating at most pron_len
       arcs and nodes will be added */
    if (fst->num_arcs + pron_len > fst->max_searchable_arcs)
      {
	PLogError("error: wordadd failed on word %s due to %d arc search limit\n",
		  word, fst->max_searchable_arcs);
	rc = FST_FAILED_ON_MEMORY;
	goto RETRC;
      }

    if (fst->num_nodes + pron_len > fst->max_searchable_nodes)
      {
	PLogError("error: wordadd failed on word %s due to %d node search limit\n",
		  word, fst->max_searchable_nodes);
	rc = FST_FAILED_ON_MEMORY;
	goto RETRC;
      }

    /* add the word if necessary, !FST_SUCCESS is allowed if we're just adding
       an alternate pronunciation */
    olabel = wordmap_find_index_in_rule(fst->olabels, word, fst->addWordCaching_lastslot_num);
    if (olabel == MAXwordID)
      {
	olabel = wordmap_add_word_in_rule(fst->olabels, word, fst->addWordCaching_lastslot_num);
	if (olabel != MAXwordID)
	  num_words_added++;
      }
    if (olabel == MAXwordID)
      {
	PLogError("error: wordmap_add_word failed\n");
	rc = FST_FAILED_ON_MEMORY;
	goto RETRC;
      }

#if USE_HMM_BASED_ENROLLMENT
    // pron should be converted to model_sequence, model_sequence_len
#define VETRANS_PREFIX "wd_hmm"
#define VETRANS_PREFIX_LEN 6
    if( LSTRSTR(pron, VETRANS_PREFIX)!=NULL) {
      // printf("Debug-pron: %d, %s\n",pron_len, pron);
      model_sequence_len=0;
      for(Tpron=pron; (Tpron=strstr(Tpron, VETRANS_PREFIX))!=NULL; ){
	// Tpron = strstr(Tpron, "wd_hmm");
	Tpron += VETRANS_PREFIX_LEN; // skip over "wd_hmm"
	// Copy hmm number (56,132,...)
	model_sequence[ model_sequence_len] = atoi_with_check(Tpron, MAXmodelID);
	model_sequence_len++;
      }


      /* we need to append silence at the end since we might be dealing with a slot
	 that has ensuing speech, formerly we only dealt with ROOT which defacto
	 was followed by the pau2 silence, so ROOT did not need its own */
      if (fst->addWordCaching_lastslot_needs_post_silence)
	model_sequence[model_sequence_len++] = 3; // <<< hmm3_sil !!! ugly hard-code here

      /* append the word boundary */
      model_sequence[model_sequence_len++] = WORD_BOUNDARY;
      /* translate to input label ids */
      for (i = 0; i < model_sequence_len; i++)
	{
	  if (model_sequence[i] >= EPSILON_OFFSET)
	    model_sequence[i] = (modelID)(model_sequence[i] + fst->hmm_ilabel_offset);
	}
    }
    else
#endif
      {
	pron_len = strlen((char*)pron);
	if (pron_len >= MAX_PRON_LEN)
	  {
	    PLogError("error: wordadd failed on word %s due to pron [%s] too long\n", word, (char*)pron);
	    rc = FST_FAILED_ON_INVALID_ARGS;
	    goto RETRC;
	  }

	for (i = 0; i < pron_len; i++)
	  {
	    if (pron[i] == OPTSILENCE_CODE)
	      {
		phoneme_sequence[i] = SILENCE_CODE;
		num_pron_ampersands++;
	      }
	    else
	      phoneme_sequence[i] = pron[i];
	  }
	/* we need to append silence at the end since we might be dealing with a slot
	   that has ensuing speech, formerly we only dealt with ROOT which defacto
	   was followed by the pau2 silence, so ROOT did not need its own */
	if (fst->addWordCaching_lastslot_needs_post_silence)
	  phoneme_sequence[i++] = SILENCE_CODE;

	model_sequence_len = i;
	irc = get_modelids_for_pron(fst->allotree, phoneme_sequence, model_sequence_len, model_sequence);
	/* check for bad sequence of phonemes */
	if (irc)
	  {
	    PLogError("error: get_modelids_for_pron(%s) returned %d\n", pron, irc);
	    rc = FST_FAILED_ON_INVALID_ARGS;
	    goto RETRC;
	  }
	IF_DEBUG_WDADD(printf("model_sequence ...\n"));

	/* append the word boundary */
	model_sequence[model_sequence_len++] = WORD_BOUNDARY;
	/* translate to input label ids */
	for (i = 0; i < model_sequence_len; i++)
	  {
	    if (model_sequence[i] >= EPSILON_OFFSET)
	      model_sequence[i] = (modelID)(model_sequence[i] + fst->hmm_ilabel_offset);
	  }
      } /*  end of ph_t ph_r .. type decoding of the model  sequence */

    /* now do the actual addition */
    rc = fst_add_arcs(fst, start_node, end_node, olabel, (costdata)cost, model_sequence, model_sequence_len);
    if (rc == FST_SUCCESS)
      {
	num_prons_added++;
      }
    else if (rc == FST_FAILED_ON_HOMONYM)
      {
	/* maybe the another pron will work? */
      }
    else
      {
	PLogMessage("error: fst_add_arcs() failed adding word %s pron %s ('&' as 'iwt')\n", word, (char*)pron);
	goto RETRC;
      }

    /* second add the pron with no silences,
       this only applies to true prons, not wd_hmm333 wd_hmm222 type prons */

    if (num_pron_ampersands > 0)
      {
	for (i = 0, model_sequence_len = 0; i < pron_len; i++)
	  {
	    if (pron[i] != OPTSILENCE_CODE)
	      phoneme_sequence[model_sequence_len++] = pron[i];
	  }

	irc = get_modelids_for_pron(fst->allotree, phoneme_sequence, model_sequence_len, model_sequence);
	/* check for bad sequence of phonemes */
	if (irc)
	  {
	    PLogError("error: get_modelids_for_pron(%s) returned %d\n", pron, rc);
	    rc = FST_FAILED_ON_INVALID_ARGS;
	    goto RETRC;
	  }
	else
	  {
	    IF_DEBUG_WDADD(printf("model_sequence ...\n"));

	    /* append the word boundary */
	    model_sequence[model_sequence_len++] = WORD_BOUNDARY;
	    /* translate to input label ids */
	    for (i = 0; i < model_sequence_len; i++)
	      {
		if (model_sequence[i] >= EPSILON_OFFSET)
		  model_sequence[i] = (modelID)(model_sequence[i] + fst->hmm_ilabel_offset);
	      }
	    /* now do the actual addition */
	    rc = fst_add_arcs(fst, start_node, end_node,
			      olabel, (costdata)cost, model_sequence, model_sequence_len);

	    if (rc == FST_SUCCESS)
	      {
		num_prons_added++;
	      }
	    else if (rc == FST_FAILED_ON_HOMONYM)
	      {
		/* maybe another pron worked? */
	      }
	    else
	      {
		PLogMessage("Warning: fst_add_arcs() failed while adding "
			    "word %s pron %s (skip '&')\n", word, (char*)pron);
		goto RETRC;
	      }
	  }
      }
  }


 RETRC:
  /* set this to make sure that FST_Prepare gets called after add word */
  fst->whether_prepared = 0;
#if USE_COMP_STATS
  end_cs_clock1(&comp_stats->word_addition, 1);
#endif

  if (rc < 0 && rc != FST_FAILED_ON_HOMONYM)
    return rc;

  if (num_prons_added == 0 && num_words_added > 0)
    {
      return FST_FAILED_ON_HOMONYM;
    }
  else if (num_prons_added == 0 && num_words_added == 0)
    {
      return FST_FAILED_ON_HOMOGRAPH;
    }
  else if (num_prons_added > 0 && num_words_added == 0)
    {
      return FST_SUCCESS_ON_OLD_WORD;
    }
  else
    {
      /* if(num_prons_added>0 && num_words_added>0) */
      return FST_SUCCESS;
    }
}

/* remove arcs leaving a node, arcs added via word add, are just discarded
to be cleaned up for re-use by other functions */
void remove_added_arcs_leaving(srec_context* fst, nodeID ni)
{
  FSMnode* node = &fst->FSMnode_list[ ni];
  FSMarc *arc = NULL, *arc2;
  arcID *pai, ai, ai2;
  for (pai = &node->un_ptr.first_next_arc, ai = (*pai); ai != MAXarcID; pai = &arc->linkl_next_arc, ai = (*pai))
  {
    if (ai < fst->num_base_arcs)
    {
      arc = &fst->FSMarc_list[ai];
    }
    else
    {
      arc2 = &fst->FSMarc_list[ai];
      for (ai2 = arc2->linkl_next_arc; ai2 >= fst->num_base_arcs && ai2 != MAXarcID;
           ai2 = arc2->linkl_next_arc)
      {
        arc2 = &fst->FSMarc_list[ai2];
      }
      *pai = ai2;
    }
  }
}

/* remove arcs arriving at a node, arcs added via word add, are just discarded
   to be cleaned up for re-use by other functions */
void remove_added_arcs_arriving(srec_context* fst, nodeID ni)
{
  FSMnode* node = &fst->FSMnode_list[ni];
  FSMarc *arc = NULL, *arc2;
  arcID *pai, ai, ai2;
  for (pai = &node->first_prev_arc, ai = (*pai); ai != MAXarcID;
       pai = &arc->linkl_prev_arc, ai = (*pai))
  {
    if (ai < fst->num_base_arcs)
    {
      arc = &fst->FSMarc_list[ai];
    }
    else
    {
      arc2 = &fst->FSMarc_list[ai];
      for (ai2 = arc2->linkl_prev_arc; ai2 >= fst->num_base_arcs && ai2 != MAXarcID;
           ai2 = arc2->linkl_prev_arc)
      {
        arc2 = &fst->FSMarc_list[ai2];
      }
      *pai = ai2;
    }
  }
}

/* reset greammar, but resetting all the arcs, nodes and words added
   via the addword functions */

int FST_ResetGrammar(srec_context* fst)
{
  int i, rc = 0;
  nodeID fst_slot_start_node, fst_slot_end_node;
  wordID fst_slot_slotnum;
  arcID ai;
  nodeID ni2, ni3, ni4, ni3a;
  FSMnode_t *node, *node2, *node3;
  FSMarc_t *arc, *arc2, *arc3;

  /*fst_slot_slotnum = wordmap_find_rule_index(fst->olabels, slot);*/
  for (fst_slot_slotnum = 1; fst_slot_slotnum < fst->olabels->num_slots;
       fst_slot_slotnum++)
  {

    if (fst_slot_slotnum == MAXwordID)
    {
      char *slot = "";
      PLogError("error: slot '%s' not found among [%d,%d] possible\n", slot, 1, fst->olabels->num_slots - 1);
      return (rc = FST_FAILED_ON_INVALID_ARGS);
    }

    /* now find where in the graph this slot is referenced, note that
       there might be more than one place, but here we ignore multiples */
    fst_slot_start_node = MAXnodeID;
    fst_slot_end_node = MAXnodeID;
    for (i = fst->num_fsm_exit_points; --i >= 0;)
    {
      ai = fst->fsm_exit_points[i].arc_index;
      if (fst->FSMarc_list[ai].olabel == fst_slot_slotnum)
      {
        fst_slot_start_node = fst->fsm_exit_points[i].from_node_index;
        fst_slot_end_node = fst->fsm_exit_points[i].wbto_node_index;
      }
    }

    /* this 'slot' could be the root rule, which can't be removed */
    if (fst_slot_start_node == MAXnodeID || fst_slot_end_node == MAXnodeID)
      continue;

    /*
      start                     end
      node   node2    node3   node4
       o--@N-->o--sil-->o--wb-->o
         arc     arc2   | arc3  ^
                 |       |wb
                 \__sil->o
    arc4   ni3a
    */

    remove_added_arcs_leaving(fst, fst_slot_start_node);
    node = &fst->FSMnode_list[ fst_slot_start_node];
    for (ai = node->un_ptr.first_next_arc; ai != MAXarcID; ai = arc->linkl_next_arc)
    {
      arc = &fst->FSMarc_list[ai];
      if (arc->olabel != fst_slot_slotnum)
        continue;

      ni2 = arc->to_node;
      remove_added_arcs_arriving(fst, ni2);
      if (ni2 == fst_slot_end_node)
        continue;
      node2 = &fst->FSMnode_list[ni2];
      arc2 = &fst->FSMarc_list[ node2->un_ptr.first_next_arc];


      ni3 = arc2->to_node;
      remove_added_arcs_arriving(fst, ni3);
      if (ni3 == fst_slot_end_node)
        continue;
      node3 = &fst->FSMnode_list[ni3];
      arc3 = &fst->FSMarc_list[ node3->un_ptr.first_next_arc];
      while (arc3->linkl_next_arc != MAXarcID)
      {
        arc3 = &fst->FSMarc_list[arc3->linkl_next_arc];
        ni3a = arc3->to_node;
        remove_added_arcs_arriving(fst, ni3a);
      }
      arc3 = &fst->FSMarc_list[ node3->un_ptr.first_next_arc];

      ni4 = arc3->to_node;
      remove_added_arcs_arriving(fst, ni4);
      ASSERT(ni4 == fst_slot_end_node);
      if (ni4 == fst_slot_end_node)
        continue;
    }
  }

  /* reset the freelist for nodes */   
  if( fst->num_nodes == fst->num_base_nodes )
    {}
  else{
    FSMnode *ntoken;
    FSMnode* tmp_FSMnode_list;
    FSMnode_info* tmp_FSMnode_info_list;

	fst->FSMnode_freelist = MAXnodeID;
    fst->num_nodes = fst->FSMnode_list_len = fst->num_base_nodes;

	tmp_FSMnode_list = (FSMnode*)CALLOC_CLR(fst->FSMnode_list_len, sizeof(FSMnode), "srec.graph.nodes");
    if(!tmp_FSMnode_list){
     PLogError("ERROR: Could NOT reset the memory for srec.graph.nodes");
     return FST_FAILED_ON_MEMORY;
     }
    memcpy( tmp_FSMnode_list, fst->FSMnode_list, fst->FSMnode_list_len*sizeof(FSMnode));

    /* scan to the end of the free list (to be re-used) */
    nodeID* last_free_node = (&fst->FSMnode_freelist);
	ntoken = (*last_free_node==MAXnodeID) ? NULL : &fst->FSMnode_list[*last_free_node] ;

	for( ; *last_free_node!=MAXnodeID; last_free_node = &ntoken->un_ptr.next_node)
		 ntoken = &tmp_FSMnode_list[ *last_free_node];

	FREE( fst->FSMnode_list);
    tmp_FSMnode_info_list = (FSMnode_info*)CALLOC_CLR(fst->FSMnode_list_len, sizeof(FSMnode_info), "srec.graph.nodeinfos");
    if(!tmp_FSMnode_info_list){
     PLogError("ERROR: Could NOT reset the memory for srec.graph.nodeinfos");
     return FST_FAILED_ON_MEMORY;
     }
	// copy in node info
	memcpy( tmp_FSMnode_info_list, fst->FSMnode_info_list, fst->FSMnode_list_len*sizeof(FSMnode_info));


	FREE( fst->FSMnode_info_list);
	fst->FSMnode_info_list = tmp_FSMnode_info_list;
    fst->FSMnode_list = tmp_FSMnode_list;
  }

  /*ni = fst->FSMnode_freelist = fst->num_base_nodes;
    node = &fst->FSMnode_list[ni];
    for (; ni < fst->FSMnode_list_len - 1; ni++, node++)
       node->un_ptr.next_node = (nodeID)(ni + 1);
    node->un_ptr.next_node = MAXnodeID;
    fst->num_nodes = fst->num_base_nodes;*/

  /* reset the freelist for arcs */ 
  if( fst->num_arcs == fst->num_base_arcs )
    {}
  else {
    FSMarc* atoken = NULL;
    FSMarc* tmp_FSMarc_list;
    
    fst->num_arcs = fst->num_base_arcs;
    fst->FSMarc_list_len = fst->num_base_arcs;
    fst->FSMarc_freelist = MAXarcID;
    tmp_FSMarc_list = (FSMarc*)CALLOC_CLR(fst->FSMarc_list_len, sizeof(FSMarc), "srec.graph.arcs");
    if(!tmp_FSMarc_list){
      PLogError("ERROR: Could NOT reset the memory for srec.graph.arcs");
     return FST_FAILED_ON_MEMORY;
    }
    memcpy( tmp_FSMarc_list, fst->FSMarc_list, fst->FSMarc_list_len*sizeof(FSMarc));
    
    /* scan to the end of the free list (to be re-used) */
    arcID* last_free_arc = &fst->FSMarc_freelist;
    atoken = (*last_free_arc==MAXarcID) ? NULL : &fst->FSMarc_list[*last_free_arc] ;
    
    for( ; *last_free_arc!=MAXarcID; last_free_arc=&atoken->linkl_next_arc)
      atoken = &tmp_FSMarc_list[ *last_free_arc];

    FREE( fst->FSMarc_list);
    fst->FSMarc_list = tmp_FSMarc_list;
  }
  
  /*ai = fst->FSMarc_freelist = fst->num_base_arcs;
    arc = &fst->FSMarc_list[ai];
    for (; ai < fst->FSMarc_list_len - 1; ai++, arc++)
    arc->linkl_next_arc = (arcID)(ai + 1);
    arc->linkl_next_arc = MAXarcID;
    fst->num_arcs = fst->num_base_arcs;
    fst->FSMarc_list_len = fst->num_base_arcs;*/
  
  /* now remove all the added words */
  wordmap_reset(fst->olabels);
  return FST_SUCCESS;
}

/* See the description of arc_tokens in the header file. These
   arcs are nodeless, which make loading from a file that
   references nodes a little harder.  We need to do it in stages.
   Later, when we load from binary image it should be much simpler.
*/

arc_token_lnk get_first_arc_leaving_node(arc_token* arc_token_list,
    arcID num_arcs,
    nodeID node)
{
  arcID i;
  for (i = 0; i < num_arcs; i++)
  {
    if ((nodeID)(int)arc_token_list[i].next_token_index == node)
      return ARC_TOKEN_LNK(arc_token_list, i);
  }
  return ARC_TOKEN_NULL;
}

int FST_LoadReverseWordGraph(srec_context* context, int num_words_to_add, PFile* fp)
{
  arcID i;
  char line[MAX_LINE_LENGTH];
  char word_label_as_str[128];
  arcID num_alloc_arc_tokens;
  nodeID from_node, into_node;
  labelID word_label = MAXwordID;
  arc_token *atoken, *last_atoken;
  costdata cost;
  arcID num_arcs;
  arc_token *arc_token_list, *tmp;
  long fpos;

  /* determine number of word arcs to allocate */
  fpos = pftell(fp);
  for (num_arcs = 0; pfgets(line, MAX_LINE_LENGTH, fp);  num_arcs++) ;
  num_alloc_arc_tokens = num_arcs;
  num_alloc_arc_tokens = (arcID)(num_arcs + num_words_to_add);
  pfseek(fp, fpos, SEEK_SET);

  context->arc_token_list = (arc_token*)CALLOC_CLR(num_alloc_arc_tokens, sizeof(arc_token), "srec.graph.wordgraph");
  arc_token_list = context->arc_token_list;

  /* 1. first load up all the information */
  i = 0;
  while (pfgets(line, MAX_LINE_LENGTH, fp))
  {
    if (sscanf(line, "%hu\t%hu\t%s", &from_node, &into_node, word_label_as_str) == 3)
    {
      word_label = wordmap_find_index(context->olabels, word_label_as_str);
      // ASSERT(word_label >= 0);
      cost = FREEcostdata;
    }
    else if (sscanf(line, "%hu", &from_node) == 1)
    {
      into_node = MAXnodeID;
      word_label = MAXwordID;
      cost = FREEcostdata;
    }
    else
    {
      PLogError("FST_LoadReverseWordGraph() .. can't parse line %s\n", line);
      ASSERT(0);
    }
    atoken = &arc_token_list[i];
    i++;
    atoken->ilabel = word_label;
	/* atoken->olabel = WORD_EPSILON_LABEL; */
    /*atoken->cost = cost; cost is not used for now */
#if DEBUG_ASTAR
    atoken->label = (word_label == MAXwordID ? 0 : wmap->words[word_label]);
#endif
    atoken->first_next_arc = (arc_token_lnk)into_node;
    atoken->next_token_index = (arc_token_lnk)from_node;
  }
  num_arcs = i;

  /* 2. now do the internal cross references */
  for (i = 0; i < num_arcs; i++)
  {
    atoken = &arc_token_list[i];
    into_node = (nodeID)atoken->first_next_arc;
    if (into_node == MAXnodeID)
      atoken->first_next_arc = ARC_TOKEN_NULL;
    else
      atoken->first_next_arc = get_first_arc_leaving_node(arc_token_list, num_arcs, (nodeID)atoken->first_next_arc);
  }

  /* 3. now do more internal cross refs */
  last_atoken = &arc_token_list[0];
  for (i = 1; i < num_arcs; i++)
  {
    atoken = &arc_token_list[i];
    if (atoken->next_token_index != last_atoken->next_token_index)
    {
      last_atoken->next_token_index = ARC_TOKEN_NULL;
    }
    else
    {
      last_atoken->next_token_index = ARC_TOKEN_LNK(arc_token_list, i);
    }
    last_atoken = atoken;
  }
  last_atoken->next_token_index = ARC_TOKEN_NULL;

#if DEBUG_ASTAR
  /* under debug, it's nice to be able to see the words leaving the
     destination node, they are stored sequentially in the debug ary */
  for (i = 0; i < num_arcs; i++)
  {
    char * p;
    atoken = &arc_token_list[i];
    atoken->debug[0] = 0;
    for (tmp = ARC_TOKEN_LNK(arc_token_list, atoken->first_next_arc); tmp != NULL;
         tmp = ARC_TOKEN_PTR(arc_token_list, tmp->next_token_index))
    {
      if (tmp->first_next_arc == ARC_TOKEN_NULL)
      {
        p = "END";
      }
      else if (!tmp->label)
      {
        p = "NULL";
      }
      else
      {
        p = tmp->label;
      }
      if (strlen(atoken->debug) + strlen(p) + 6 < 64)
      {
        strcat(atoken->debug, p);
        strcat(atoken->debug, " ");
      }
      else
      {
        strcat(atoken->debug, "...");
        break;
      }
    }
  }
#endif
  context->arc_token_list_len = num_alloc_arc_tokens;
  if (num_alloc_arc_tokens > num_arcs)
  {
    atoken = &context->arc_token_list[num_arcs];
    for (i = num_arcs; i < num_alloc_arc_tokens; i++, atoken++)
    {
      atoken->first_next_arc = ARC_TOKEN_NULL;
      atoken->ilabel         = MAXwordID;
      atoken->next_token_index = ARC_TOKEN_LNK(arc_token_list, i + 1);/*atoken+1;*/
    }
    atoken--;
    atoken->next_token_index = ARC_TOKEN_NULL;
    context->arc_token_freelist = &context->arc_token_list[num_arcs];
  }
  else
  {
    context->arc_token_freelist = NULL;
  }
  /* the insert start is the arc that goes to the end */
  atoken = context->arc_token_list;
  atoken = ARC_TOKEN_PTR(arc_token_list, atoken->first_next_arc);
  for (; atoken; atoken = ARC_TOKEN_PTR(arc_token_list, atoken->next_token_index))
  {
    if (atoken->first_next_arc == ARC_TOKEN_NULL)
      break;
    tmp = ARC_TOKEN_PTR(context->arc_token_list, atoken->first_next_arc);
    if (tmp->ilabel == MAXwordID && tmp->first_next_arc == ARC_TOKEN_NULL)
      break;
  }
  context->arc_token_insert_start = atoken;

  return FST_SUCCESS;
}

int FST_UnloadReverseWordGraph(srec_context* context)
{
  FREE(context->arc_token_list);
  context->arc_token_list = 0;
  return FST_SUCCESS;
}

/*----------------------------------------------------------------------*
 *                                                                      *
 * general                                                              *
 *                                                                      *
 *----------------------------------------------------------------------*/

char split_line(char* line, char** av)
{
  int i = 0;
  av[i] = strtok(line, "\n\r\t ");
  for (; av[i];)
  {
    av[++i] = strtok(NULL, "\n\r\t ");
  }
  return i;
}

asr_int32_t atoi_with_check(const char* buf, asr_int32_t mymax)
{
  asr_int32_t ans = atol(buf);
  ASSERT(ans < mymax);
  return ans;
}

/*----------------------------------------------------------------------*
 *                                                                      *
 * fst                                                                  *
 *                                                                      *
 *----------------------------------------------------------------------*/

arcID fst_get_free_arc(srec_context* fst)
{
  FSMarc* atoken;
  arcID atokid = fst->FSMarc_freelist;
  if (atokid == MAXarcID)
  {
    PLogError("error: ran out of arcs\n");
    return atokid;
  }
  atoken = &fst->FSMarc_list[atokid];
  fst->FSMarc_freelist = ARC_XtoI(atoken->linkl_next_arc);
  memset(atoken, 0, sizeof(FSMarc));
  atoken->to_node = FSMNODE_NULL;
  atoken->fr_node = FSMNODE_NULL;
  atoken->linkl_next_arc = FSMARC_NULL;
  atoken->linkl_prev_arc = FSMARC_NULL;
  atoken->ilabel = 0;
  atoken->olabel = 0;
  atoken->cost   = 0;
  fst->num_arcs++;
  return atokid;
}
nodeID fst_get_free_node(srec_context* fst)
{
  FSMnode* ntoken;
  nodeID ntokid = fst->FSMnode_freelist;
  if (ntokid == MAXnodeID)
  {
    PLogError("error: ran out of nodes\n");
    return ntokid;
  }
  ntoken = &fst->FSMnode_list[ntokid];
  fst->FSMnode_freelist = NODE_XtoI(ntoken->un_ptr.next_node);
  ntoken->un_ptr.first_next_arc = FSMARC_NULL;
  ntoken->first_prev_arc = FSMARC_NULL;
  /* ASSERT( (int)(ntok - fst->FSMnode_list) == fst->num_nodes); */
  fst->num_nodes++;
  return ntokid;
}

int fst_free_node(srec_context* fst, FSMnode* node)
{
  IF_DEBUG_WDADD(printf_node(fst, "freeing: ", node));
  node->first_prev_arc = FSMARC_NULL;
  node->un_ptr.next_node = fst->FSMnode_freelist;
  node->first_prev_arc = FSMARC_FREE;
  fst->FSMnode_freelist = (nodeID)(node - fst->FSMnode_list);
  /* fst->num_nodes--; not allowed unless we compress! */
  return 0;
}

int fst_free_arc(srec_context* fst, FSMarc* arc)
{
  IF_DEBUG_WDADD(printf_arc(fst, "freeing: ", arc));
  arc->linkl_prev_arc = FSMARC_FREE;
  arc->linkl_next_arc = ARC_ItoX(fst->FSMarc_freelist);
  arc->cost = 2;
  arc->ilabel = 0;
  arc->olabel = 0;
  IF_DEBUG_WDADD(arc->ilabel_str = 0);
  IF_DEBUG_WDADD(arc->olabel_str = 0);
  fst->FSMarc_freelist = (arcID)(arc - fst->FSMarc_list);
  fst->num_arcs--;
  return 0;
}

int fst_free_arc_net(srec_context* fst, FSMarc* arc)
{
  if (!arc)
    return 0;
  /* need to traverse the arc, get the 'from' nodes,
     then traverse the from nodes and kill arcs */
  /* we'll need this after an error condition */
  return 0;
}

int fst_push_arc_olabel(srec_context* fst, FSMarc* arc)
{
  FSMarc_ptr atok;
  FSMarc* atoken;
  FSMnode_ptr ntok = arc->to_node;

  /* look for all arcs leaving this arc, and mark them
     normally there is only one, except for multipron words */
  for (atok = FIRST_NEXT(ntok); atok != FSMARC_NULL; atok = atoken->linkl_next_arc)
  {
    atoken = ARC_XtoP(atok);
    /* cannot have multiple olabels before a boundary */
    if (atoken->olabel != WORD_EPSILON_LABEL)
      return FST_FAILED_INTERNAL;
    atoken->olabel = arc->olabel;
#if DO_WEIGHTED_ADDWORD
    atoken->cost = (costdata)(atoken->cost + arc->cost);
#endif
    IF_DEBUG_WDADD(atoken->olabel_str = arc->olabel_str);
  }
  /* unlabel this arc */
  arc->olabel = WORD_EPSILON_LABEL;
#if DO_WEIGHTED_ADDWORD
  arc->cost = FREEcostdata;
#endif
  IF_DEBUG_WDADD(arc->olabel_str = fst->olabels->words[ arc->olabel]);
  return FST_SUCCESS;
}

#if DO_WEIGHTED_ADDWORD
int fst_push_arc_cost(srec_context* fst, FSMarc* arc)
{
  FSMarc_ptr atok;
  FSMarc* atoken;
  FSMnode_ptr ntok = arc->to_node;
  /* look for all arcs leaving this arc, and push the cost thereto */
  for (atok = FIRST_NEXT(ntok); atok != FSMARC_NULL; atok = atoken->linkl_next_arc)
  {
    atoken = ARC_XtoP(atok);
    atoken->cost = (costdata)(atoken->cost + arc->cost);
  }
  arc->cost = FREEcostdata;
  return FST_SUCCESS;
}
#endif


int fst_pull_arc_olabel(srec_context* fst, FSMarc* arc)
{
  FSMarc_ptr atok;
  FSMarc* atoken;
  FSMnode_ptr fr_node = arc->fr_node;
  FSMarc_ptr tmp_atok;
  FSMarc *tmp_atoken;
  FSMnode *anode;

  if (arc->olabel == WORD_EPSILON_LABEL)
    return FST_SUCCESS;

  /* can the olabel be pulled earlier? */
  for (atok = FIRST_PREV(fr_node); atok != FSMARC_NULL; atok = atoken->linkl_prev_arc)
  {
    atoken = ARC_XtoP(atok);
    anode = NODE_XtoP(atoken->to_node);

    tmp_atok = anode->un_ptr.first_next_arc;
    if (tmp_atok != FSMARC_NULL)
    {
      tmp_atoken = ARC_XtoP(tmp_atok);
      if (tmp_atoken->linkl_next_arc != FSMARC_NULL)
      {
        return FST_CONTINUE;
      }
    }
  }

  /* look for all arcs arriving at this arc, and mark them */
  for (atok = FIRST_PREV(fr_node);atok != FSMARC_NULL;atok = atoken->linkl_prev_arc)
  {
    atoken = ARC_XtoP(atok);
    /* cannot have multiple olabels! serious internal error!? */
    if (atoken->olabel != WORD_EPSILON_LABEL)
    {
      PLogError("error: internal error, in fst_pull_arc_olabel()\n");
      return FST_FAILED_INTERNAL;
    }
    atoken->olabel = arc->olabel;
#if DO_WEIGHTED_ADDWORD
    atoken->cost = arc->cost;
#endif
    IF_DEBUG_WDADD(atoken->olabel_str = arc->olabel_str);
  }
  /* unlabel this arc */
  arc->olabel = WORD_EPSILON_LABEL;
#if DO_WEIGHTED_ADDWORD
  arc->cost   = FREEcostdata;
#endif
  IF_DEBUG_WDADD(arc->olabel_str = fst->olabels->words[ arc->olabel]);
  return FST_SUCCESS;
}

static FSMarc* find_next_arc_with_ilabel(srec_context* fst, FSMnode* node, labelID ilabel, FSMarc** last)
{
  FSMarc_ptr atok;
  FSMarc* atoken = NULL;
  for (atok = node->un_ptr.first_next_arc; atok != FSMARC_NULL; atok = atoken->linkl_next_arc)
  {
    atoken = ARC_XtoP(atok);
    if (atoken->ilabel == ilabel)
      return atoken;
  }
  *last = atoken; /* to this we may want to append a new arc */
  return NULL;
}

FSMarc* find_prev_arc_with_iolabels(srec_context* fst, FSMnode* node, labelID ilabel, labelID olabel, FSMarc** last)
{
  FSMarc_ptr atok;
  FSMarc* atoken = NULL;
  FSMarc_ptr tmp_atok;
  FSMarc* tmp_atoken;

  for (atok = node->first_prev_arc; atok != FSMARC_NULL; atok = atoken->linkl_prev_arc)
  {
    atoken = ARC_XtoP(atok);
    if (atoken->olabel == olabel && atoken->ilabel == ilabel)
    {
      tmp_atok = NODE_XtoP(atoken->fr_node)->un_ptr.first_next_arc;
      if (tmp_atok != FSMARC_NULL)
      {
        tmp_atoken = ARC_XtoP(tmp_atok);

        if (tmp_atoken->linkl_next_arc == FSMARC_NULL)
          return atoken;
      }
      else
        return atoken;
    }

  }
  if (last) *last = atoken;
  return NULL;
}

int num_arcs_leaving(srec_context* fst, FSMnode* node)
{
  int num_arcs = 0;
  FSMarc_ptr atok;
  FSMarc* atoken;
  for (atok = node->un_ptr.first_next_arc; atok != FSMARC_NULL; atok = atoken->linkl_next_arc)
  {
    atoken = ARC_XtoP(atok);
    num_arcs++;
  }
  return num_arcs;
}

int num_arcs_arriving(srec_context* fst, FSMnode* node)
{
  int num_arcs = 0;
  FSMarc_ptr atok;
  FSMarc* atoken;
  for (atok = node->first_prev_arc; atok != FSMARC_NULL; atok = atoken->linkl_prev_arc)
  {
    atoken = ARC_XtoP(atok);
    num_arcs++;
  }
  return num_arcs;
}

PINLINE int num_arcs_arriving_gt_1(srec_context* fst, FSMnode* node)
{
  FSMarc_ptr atok = node->first_prev_arc;
  FSMarc* atoken;
  if (atok == FSMARC_NULL)
    return 0;
  atoken = ARC_XtoP(atok);
  return atoken->linkl_prev_arc == FSMARC_NULL ? 0 : 1;
}

static void remove_arc_arriving(srec_context* fst, FSMnode* to_node, FSMarc* arc)
{
  FSMarc* atoken;
  FSMarc_ptr* atok;
  for (atok = &to_node->first_prev_arc;(*atok) != FSMARC_NULL; atok = &atoken->linkl_prev_arc)
  {
    atoken = ARC_XtoP(*atok);
    if (atoken == arc)
    {
      (*atok) = arc->linkl_prev_arc;
      return;
    }
  }
  ASSERT(0);
}

int split_node_for_arc(srec_context* fst, FSMarc* arc)
{
  arcID new_arc_id;
  nodeID new_node_id;
  FSMnode* new_node;
  FSMnode* old_node;
  FSMarc_ptr atok, new_next, last_new_next;
  FSMarc *atoken, *new_next_p;

  IF_DEBUG_WDADD(printf_arc(fst, "spliting ... ", arc));

  new_node_id = fst_get_free_node(fst);
  if (new_node_id == MAXnodeID)
    return FST_FAILED_ON_MEMORY;
  new_node = &fst->FSMnode_list[ new_node_id];
  old_node = NODE_XtoP(arc->to_node);
  arc->to_node = NODE_ItoX(new_node_id);
  new_node->first_prev_arc = ARC_PtoX(arc);
  remove_arc_arriving(fst, old_node, arc);
  arc->linkl_prev_arc = FSMARC_NULL;

  last_new_next = FSMARC_NULL;
  for (atok = old_node->un_ptr.first_next_arc; atok != FSMARC_NULL; atok = atoken->linkl_next_arc)
  {
    atoken = ARC_XtoP(atok);
    new_arc_id = fst_get_free_arc(fst);
    if (new_arc_id == MAXarcID)
      return FST_FAILED_ON_MEMORY;
    new_next = ARC_ItoX(new_arc_id);
    if (last_new_next != FSMARC_NULL)
      LINKL_NEXT(last_new_next) = new_next;
    else
      new_node->un_ptr.first_next_arc = new_next;
    new_next_p = ARC_XtoP(new_next);
    new_next_p->ilabel = atoken->ilabel;
    new_next_p->to_node = atoken->to_node;
    new_next_p->fr_node = arc->to_node;
    new_next_p->olabel = atoken->olabel;
    IF_DEBUG_WDADD(new_next_p->ilabel_str = atoken->ilabel_str);
    IF_DEBUG_WDADD(new_next_p->olabel_str = atoken->olabel_str);
    new_next_p->linkl_next_arc = FSMARC_NULL; /* set at next loop */
    append_arc_arriving_node(fst, NODE_XtoP(atoken->to_node), new_next);
    last_new_next = new_next;
  }
  return FST_SUCCESS;
}

int fst_add_arcs(srec_context* fst, nodeID start_node, nodeID end_node,
                 wordID add_tree_olabel, costdata add_tree_cost,
                 modelID* add_tree_start, int add_tree_len)
{
  int rc = FST_SUCCESS;
  FSMarc_ptr atok = (FSMarc_ptr)0, last_atok = (FSMarc_ptr)0;
  FSMarc* atoken = NULL;
  FSMarc *next_atoken, *prev_atoken;
  FSMarc *append_to;
  FSMnode *late_start_node, *early_end_node;
  FSMnode *ntoken, *last_ntoken;
  FSMnode_ptr ntok, last_ntok;
  modelID *add_tree_end = add_tree_start + add_tree_len - 1;
  modelID *add_tree_late_start, *add_tree_early_end;
  modelID *add_tree;
  arcID new_arc_id, atokid;
  nodeID new_node_id, atokid_node;
  wordID add_tree_olabel_use = add_tree_olabel;
  wordID add_tree_cost_use   = add_tree_cost;

  append_to = NULL;
  add_tree = add_tree_start;
#define FST_GROW_MINARCS  100
#define FST_GROW_MINNODES 100
#if defined(FST_GROW_FACTOR)

  /* make sure we have enough arcs available */
  if(fst->num_arcs + add_tree_len >= fst->FSMarc_list_len)
   {
	FSMarc* tmp_FSMarc_list;
	arcID tmp_FSMarc_list_len;
	int itmp_FSMarc_list_len = fst->FSMarc_list_len*FST_GROW_FACTOR ;

	if( itmp_FSMarc_list_len - fst->FSMarc_list_len < FST_GROW_MINARCS)
		itmp_FSMarc_list_len += FST_GROW_MINARCS;

	if( itmp_FSMarc_list_len - fst->FSMarc_list_len < add_tree_len)
		itmp_FSMarc_list_len += add_tree_len;

    if(itmp_FSMarc_list_len >= (int)MAXarcID)
	     return FST_FAILED_ON_MEMORY;

	tmp_FSMarc_list_len = (arcID)itmp_FSMarc_list_len;

    tmp_FSMarc_list = (FSMarc*)CALLOC_CLR(tmp_FSMarc_list_len, sizeof(FSMarc), "srec.graph.arcs");
      if(!tmp_FSMarc_list) {
	PLogError("error: Failed to extend the memory for arcs\n");
        return FST_FAILED_INTERNAL;
      }
    memcpy( tmp_FSMarc_list, fst->FSMarc_list, fst->FSMarc_list_len*sizeof(FSMarc));

	/* initialize the new free list.
    head of this new free list is tmp_FSMarc_list[ fst->FSMarc_list_len] */
	for(atokid=fst->FSMarc_list_len; atokid<tmp_FSMarc_list_len-1; atokid++) {
	   tmp_FSMarc_list[atokid].linkl_next_arc = ARC_ItoX(atokid+1);
       tmp_FSMarc_list[atokid].linkl_prev_arc = FSMARC_FREE;
    }
    tmp_FSMarc_list[atokid].linkl_next_arc = FSMARC_NULL;
    tmp_FSMarc_list[atokid].linkl_prev_arc = FSMARC_FREE;

    /* scan to the end of the old free list (to be re-used) */
    arcID* last_free_arc = &fst->FSMarc_freelist;
    atoken = (*last_free_arc==MAXarcID) ? NULL : &fst->FSMarc_list[*last_free_arc] ;

    for( ; *last_free_arc!=MAXarcID; last_free_arc=&atoken->linkl_next_arc)
	    atoken = &tmp_FSMarc_list[ *last_free_arc];

	/* append the new free list to the current free list */
	*last_free_arc = fst->FSMarc_list_len;

	FREE( fst->FSMarc_list);
    fst->FSMarc_list = tmp_FSMarc_list;
    fst->FSMarc_list_len = tmp_FSMarc_list_len;
   }

   /* make sure we have enough nodes available */
   if(fst->num_nodes + add_tree_len >= fst->FSMnode_list_len)
   {
     FSMnode* tmp_FSMnode_list;
	 nodeID tmp_FSMnode_list_len;
	 FSMnode_info* tmp_FSMnode_info_list;
	 int itmp_FSMnode_list_len = fst->FSMnode_list_len * FST_GROW_FACTOR ;

	 if( itmp_FSMnode_list_len - fst->FSMnode_list_len < FST_GROW_MINNODES)
		itmp_FSMnode_list_len += FST_GROW_MINNODES;

	 if( itmp_FSMnode_list_len - fst->FSMnode_list_len < add_tree_len)
		itmp_FSMnode_list_len += add_tree_len;

	 if(itmp_FSMnode_list_len >= (int)MAXnodeID)
	     return FST_FAILED_ON_MEMORY;

	 tmp_FSMnode_list_len = (nodeID)itmp_FSMnode_list_len;

     tmp_FSMnode_list = (FSMnode*)CALLOC_CLR(tmp_FSMnode_list_len, sizeof(FSMnode), "srec.graph.nodes");
     if(!tmp_FSMnode_list) {
       PLogError("ERROR: Failed to extend the memory for nodes\n");
       return FST_FAILED_INTERNAL;
     }
     memcpy( tmp_FSMnode_list, fst->FSMnode_list, fst->FSMnode_list_len*sizeof(FSMnode));

	/* initialize the new free node list.
    head of this new free list is tmp_FSMnode_list[ fst->FSMnode_list_len] */
    for(atokid_node=fst->FSMnode_list_len; atokid_node<tmp_FSMnode_list_len-1; atokid_node++)
	{
	   tmp_FSMnode_list[atokid_node].un_ptr.next_node = NODE_ItoX(atokid_node + 1);
       tmp_FSMnode_list[atokid_node].first_prev_arc = FSMARC_FREE;
    }
       tmp_FSMnode_list[atokid_node].un_ptr.next_node = FSMNODE_NULL;
       tmp_FSMnode_list[atokid_node].first_prev_arc = FSMARC_FREE;

    /* scan to the end of the old free list (to be re-used) */
    nodeID* last_free_node = (&fst->FSMnode_freelist);
	ntoken = (*last_free_node==MAXnodeID) ? NULL : &fst->FSMnode_list[*last_free_node] ;

	for( ; *last_free_node!=MAXnodeID; last_free_node = &ntoken->un_ptr.next_node)
		 ntoken = &tmp_FSMnode_list[ *last_free_node];

	/* append the new free list to the current free list */
	*last_free_node = fst->FSMnode_list_len;

	FREE( fst->FSMnode_list);

	tmp_FSMnode_info_list = (FSMnode_info*)CALLOC_CLR( tmp_FSMnode_list_len, sizeof(FSMnode_info), "srec.graph.nodeinfos");
     if(!tmp_FSMnode_info_list) {
       PLogError("ERROR: Failed to extend the memory for node infos\n");
       return FST_FAILED_INTERNAL;
     }
	// copy in old node info
	memcpy( tmp_FSMnode_info_list, fst->FSMnode_info_list, fst->FSMnode_list_len*sizeof(FSMnode_info));

	// initialize the new node info
	for (atokid_node=fst->FSMnode_list_len; atokid_node < tmp_FSMnode_list_len; atokid_node++)
		tmp_FSMnode_info_list[atokid_node] = NODE_INFO_UNKNOWN;

	FREE( fst->FSMnode_info_list);
	fst->FSMnode_info_list = tmp_FSMnode_info_list;
    fst->FSMnode_list = tmp_FSMnode_list;
    fst->FSMnode_list_len = tmp_FSMnode_list_len;
}
#endif
   late_start_node = &fst->FSMnode_list[start_node];

  while (1)
  {

    next_atoken = find_next_arc_with_ilabel(fst, late_start_node,
                                            *add_tree /*->ilabel*/,
                                            &append_to);
    if (next_atoken != NULL)
    {
      /* so next_atok->ilabel == add_tree->ilabel */

      if (next_atoken->ilabel == WORD_BOUNDARY && *add_tree/*->ilabel*/ == WORD_BOUNDARY)
      {
	 next_atoken = NULL;
	 break; /* break as if nothing more can be shared! */
	// return FST_FAILED_ON_HOMONYM;
      }

      if (num_arcs_arriving_gt_1(fst, NODE_XtoP(next_atoken->to_node)))
      {
        split_node_for_arc(fst, next_atoken);
        /* unfortunate side effect here, that if we later find out this
           was a homonym addition, then this expansion was useless and
           for now we don't undo it! */
      }

      /* we shouldn't really push the olabel if it's the same for both! */
      if (next_atoken->olabel != add_tree_olabel_use)
      {
        if (next_atoken->olabel != WORD_EPSILON_LABEL)
        {
          if (fst_push_arc_olabel(fst, next_atoken) != FST_SUCCESS)
          {
            PLogError("error: internal error fst_push_arc_olabel()\n");
            return FST_FAILED_INTERNAL;
          }
        }
#if DO_WEIGHTED_ADDWORD
        else
          fst_push_arc_cost(fst, next_atoken);
#endif
      }
      else if (add_tree_olabel_use != WORD_EPSILON_LABEL)
      {
        /* the graph already has this word, so we just re-use the olabel
           and disable the setting of olabel down below */
        add_tree_olabel_use = WORD_EPSILON_LABEL;
#if DO_WEIGHTED_ADDWORD
        add_tree_cost_use   = FREEcostdata;
#endif
      }
      add_tree++;
      late_start_node = NODE_XtoP(next_atoken->to_node);
    }
    else
    {
      break;
    }
  }

  add_tree_late_start = add_tree;
  early_end_node = &fst->FSMnode_list[end_node];

  if (!do_minimize)
  {
    last_ntoken = NULL;
    last_ntok = NODE_PtoX(late_start_node);
    for (; add_tree <= add_tree_end; add_tree++)
    {
      new_arc_id = fst_get_free_arc(fst);
      if (new_arc_id == MAXarcID)
      {
        PLogError("fst_get_free_arc() failed\n");
        return FST_FAILED_ON_MEMORY;
      }
      atok = ARC_ItoX(new_arc_id);
      atoken = ARC_XtoP(atok);
      if (add_tree != add_tree_end)
      {
        new_node_id = fst_get_free_node(fst);
        if (new_node_id == MAXnodeID)
        {
          PLogError("fst_get_free_node() failed\n");
          return FST_FAILED_ON_MEMORY;
        }
        ntok = NODE_ItoX(new_node_id);
        ntoken = NODE_XtoP(ntok);
        ntoken->first_prev_arc = atok;
      }
      else
      {
        ntok = FSMNODE_NULL;
        ntoken = NULL;
      }
      atoken->fr_node = last_ntok; /* was NODE_PtoX(late_start_node) */
      atoken->to_node = ntok;
      atoken->ilabel = *add_tree;
      atoken->linkl_next_arc = FSMARC_NULL;
      atoken->linkl_prev_arc = FSMARC_NULL;
      IF_DEBUG_WDADD(atoken->ilabel_str = fst->ilabels->words[ atoken->ilabel]);
      if (!last_ntoken)
      {
        append_to->linkl_next_arc = atok;
        atoken->olabel = add_tree_olabel_use;
#if DO_WEIGHTED_ADDWORD
        atoken->cost  =  add_tree_cost_use;
#endif
        IF_DEBUG_WDADD(atok->olabel_str = fst->olabels->words[ atoken->olabel]);
      }
      else
      {
        last_ntoken->un_ptr.first_next_arc = atok;
      }
      last_ntoken = ntoken;
      last_ntok   = ntok;
    }
    /*  add_tree_end->to_node = early_end_node;
    add_tree_end->linkl_next_arc = FSMARC_NULL;
    atok = last_arc_arriving( early_end_node);
    atok->linkl_prev_arc = add_tree_end;
    add_tree_end->linkl_prev_arc = FSMARC_NULL; */
    atoken->to_node = NODE_ItoX(end_node);
    append_arc_arriving_node(fst, &fst->FSMnode_list[end_node], atok);
  }

  else /* ie. do_minimize from the rear */
  {

    add_tree = add_tree_end;
    new_arc_id = fst_get_free_arc(fst);
    if (new_arc_id == MAXarcID)
      return FST_FAILED_ON_MEMORY;
    atok = ARC_ItoX(new_arc_id);
    atoken = ARC_XtoP(atok);
    atoken->olabel = add_tree_olabel_use;
#if DO_WEIGHTED_ADDWORD
    atoken->cost   = add_tree_cost_use;
#endif
    atoken->ilabel = *add_tree_late_start;
    atoken->fr_node = NODE_PtoX(late_start_node);
    if (atoken->ilabel == WORD_BOUNDARY)
    {
      atoken->cost = (costdata)(atoken->cost + fst->wtw_average);
    }
    else
    {
      atoken->cost   = add_tree_cost_use + fst->wtw_average;
      add_tree_cost_use = FREEcostdata;
    }
    IF_DEBUG_WDADD(atoken->olabel_str = fst->olabels->words[ atoken->olabel]);
    IF_DEBUG_WDADD(atoken->ilabel_str = fst->ilabels->words[ atoken->ilabel]);
    append_arc_leaving_node(fst, late_start_node, atok);

    last_atok = atok;
    while (1)
    {

      if (add_tree == add_tree_late_start)
        break;
      /* if there are other arcs leaving this node then joining
      earlier than here will result in over-generation, so don't
      if( atok!=end_arc && atok->linkl_next_arc != FSMARC_NULL)
      break;
      */
      /*
      also need boundary conditions checker here
      */

      /* */
      IF_DEBUG_WDADD(printf_node(fst, early_end_node));

      /* if( num_arcs_leaving( add_tree_end->fr_node->first_prev_arc->to_node) >1) {
      printf("merge stopped due to num_arcs leaving ..\n");
      printf_arc(fst, add_tree_end->fr_node->first_prev_arc->to_node->first_next_arc);
      break;
      } */

      for (atok = early_end_node->first_prev_arc; atok != FSMARC_NULL; atok = atoken->linkl_prev_arc)
      {
        atoken = ARC_XtoP(atok);
	/* never pull before the slot marker */
	if(atoken->cost == DISABLE_ARC_COST) break; 
	if(atoken->olabel>0 && atoken->olabel<fst->olabels->num_slots) break;  
        fst_pull_arc_olabel(fst, atoken); /* fails are ok */
      }

      prev_atoken = find_prev_arc_with_iolabels(fst, early_end_node,
                    *add_tree /*->ilabel*/,
                    WORD_EPSILON_LABEL,
                    NULL /*&append_to*/);
      if (!prev_atoken)
        break;
      else
      {
        /* this check is now inside find_prev_arc_with_iolabels */
        /* if( num_arcs_leaving(prev_atok->fr_node->first_prev_arc->to_node->first_next_arc)>1)*/

        /* now look to merge earlier still */
        early_end_node = NODE_XtoP(prev_atoken->fr_node);
        add_tree--;
        if (add_tree == add_tree_late_start)
          break;
      }
    }
    add_tree_early_end = add_tree;
    for (add_tree = add_tree_late_start + 1; add_tree <= add_tree_early_end; add_tree++)
    {
      new_node_id = fst_get_free_node(fst);
      if (new_node_id == MAXnodeID)
        return FST_FAILED_ON_MEMORY;
      ntok = NODE_ItoX(new_node_id);
      ntoken = NODE_XtoP(ntok);
      new_arc_id = fst_get_free_arc(fst);
      if (new_arc_id == MAXarcID)
        return FST_FAILED_ON_MEMORY;
      atok = ARC_ItoX(new_arc_id);
      atoken = ARC_XtoP(atok);
      atoken->ilabel = *add_tree;
#if DO_WEIGHTED_ADDWORD
      atoken->cost   = FREEcostdata; /* idea: distribute add_tree_cost here;*/
#endif
      atoken->olabel = WORD_EPSILON_LABEL;
      atoken->fr_node = ntok;
      atoken->to_node = FSMNODE_NULL; /* filled in next loop */
      IF_DEBUG_WDADD(atoken->olabel_str = fst->olabels->words[atoken->olabel]);
      IF_DEBUG_WDADD(atoken->ilabel_str = fst->ilabels->words[atoken->ilabel]);

      ntoken->un_ptr.first_next_arc = atok;
      ntoken->first_prev_arc = last_atok;
      TO_NODE(last_atok) = ntok;
      last_atok = atok;
    }
    TO_NODE(last_atok) = NODE_PtoX(early_end_node);
    append_arc_arriving_node(fst, early_end_node, last_atok);
  }
  return rc;
}


void append_arc_leaving_node(srec_context* fst, FSMnode* fr_node, FSMarc_ptr arc)
{
  FSMarc_ptr* atok = &fr_node->un_ptr.first_next_arc;
  FSMarc* atoken = ARC_XtoP(*atok);
  for (; (*atok) != FSMARC_NULL; atok = &atoken->linkl_next_arc)
  {
    atoken = ARC_XtoP(*atok);
  }
  *atok = arc;
  LINKL_NEXT(arc) = FSMARC_NULL;
}
void append_arc_arriving_node(srec_context* fst, FSMnode* to_node, FSMarc_ptr arc)
{
  FSMarc_ptr* atok = &to_node->first_prev_arc;
  FSMarc* atoken = ARC_XtoP(*atok);
  for (; (*atok) != FSMARC_NULL; atok = &atoken->linkl_prev_arc)
  {
    atoken = ARC_XtoP(*atok);
  }
  *atok = arc;
  LINKL_PREV(arc) = FSMARC_NULL;
}


int printf_node1(srec_context* fst, FSMnode* node)
{
  return 0;
}
int printf_arc1(srec_context* fst, char* msg, FSMarc* arc)
{
  char buffer[MAX_LINE_LENGTH];
  sprintf_arc(buffer, fst, arc);
  printf("%s%s\n", msg, buffer);
  return 0;
}
int sprintf_arc(char* buf, srec_context* fst, FSMarc* arc)
{
  int rc;
  FSMnode* to_node = NODE_XtoP(arc->to_node);
  arcID arc_index = (arcID)(arc - fst->FSMarc_list);
  if (to_node->un_ptr.first_next_arc == FSMARC_NULL)
    rc = sprintf(buf, "arc%hu\n", arc_index);
  else
  {
    rc = sprintf(buf, "arc%hu\t%hu,%hu\t%s\t%s\t%hu\n",
                 arc_index,
                 ARC_XtoI(to_node->un_ptr.first_next_arc),
                 arc->linkl_next_arc != FSMARC_NULL ? ARC_XtoI(arc->linkl_next_arc) : -1,
                 fst->ilabels->words[arc->ilabel],
                 fst->olabels->words[arc->olabel],
                 arc->cost);
  }
  return rc;
}

/* dumps the recognition context as a binary file,
   it will also store any empty space for growing */

#define ENCODE(SIGN,TYP) { SIGN+=sizeof(TYP); SIGN=SIGN<<3; ASSERT((shifted+=3)<32); }

asr_int32_t FST_sizes_signature()
{
#ifndef NDEBUG
  int shifted = 0;
#endif
  asr_int32_t signature = 0;
  ENCODE(signature, arcID);
  ENCODE(signature, nodeID);
  ENCODE(signature, wordID);
  ENCODE(signature, labelID);
  ENCODE(signature, costdata);
  return signature;
}

int FST_ContextImageSize(srec_context* context)
{
  int size = 0;
  size += sizeof(srec_context);
  size += sizeof(wordmap);
  size += sizeof(char*) * context->olabels->max_words;
  size += sizeof(char) * context->olabels->max_chars;
  size += sizeof(wordmap);
  if (context->ilabels->words != NULL)
    size += sizeof(char*) * context->olabels->max_words;
  if (context->ilabels->chars != NULL)
    size += sizeof(char) * context->olabels->max_chars;
  size += sizeof(FSMarc) * context->FSMarc_list_len;
  size += sizeof(FSMnode) * context->FSMnode_list_len;
  size += sizeof(FSMnode_info) * context->FSMnode_list_len;
  size += sizeof(arc_token) * context->arc_token_list_len;
  return size;
}

ESR_ReturnCode serializeWordMapV2(wordmap *wordmap, PFile* fp)
{
  unsigned int i = 0;
  unsigned int nfields;
  unsigned int tmp2[32];

  i = 0;
  tmp2[i++] = wordmap->num_words;
  tmp2[i++] = wordmap->num_slots;
  tmp2[i++] = wordmap->max_words;
  tmp2[i++] = wordmap->num_base_words;
  tmp2[i++] = wordmap->max_chars;
  tmp2[i++] = (wordmap->next_chars - wordmap->chars);
  tmp2[i++] = (wordmap->next_base_chars - wordmap->chars);
  nfields = i;
  if (pfwrite(tmp2, sizeof(tmp2[0]), nfields, fp) != nfields)
    return ESR_WRITE_ERROR;

  if (pfwrite(wordmap->chars, sizeof(char), wordmap->max_chars, fp) != (size_t)wordmap->max_chars)
    return ESR_WRITE_ERROR;

  return ESR_SUCCESS;
}

ESR_ReturnCode deserializeWordMapV2(wordmap **pwordmap, PFile* fp)
{
  unsigned int i = 0;
  unsigned int nfields;
  unsigned int tmp2[32];
  wordmap *awordmap;
  char *p;
  ESR_ReturnCode rc = ESR_SUCCESS;
  unsigned int next_chars_idx, next_base_chars_idx;

  awordmap = NEW(wordmap, L("srec.g2g.graph.wordmap.base"));
  if (awordmap == NULL)
  {
    PLogError("NEW failed on srec.g2g.graph.wordmap.base\n");
    rc = ESR_OUT_OF_MEMORY;
    goto CLEANUP;
  }
  awordmap->wordIDForWord = NULL; // early break to cleanup needs this

  nfields = 7;
  if (pfread(tmp2, sizeof(tmp2[0]), nfields, fp) != nfields)
  {
    PLogError("pfread failed when reading nfields\n");
    rc = ESR_READ_ERROR;
    goto CLEANUP;
  }

  i = 0;
  awordmap->num_words      = (wordID)tmp2[i++];
  awordmap->num_slots      = (wordID)tmp2[i++];
  awordmap->max_words      = (wordID)tmp2[i++];
  awordmap->num_base_words = (wordID)tmp2[i++];
  awordmap->max_chars      = tmp2[i++];
  next_chars_idx           = tmp2[i++];
  next_base_chars_idx      = tmp2[i++];
  ASSERT(nfields == i);

  awordmap->words = NEW_ARRAY(char*, awordmap->max_words, L("srec.g2g.graph.wordmap.words"));
  if (awordmap->words == NULL)
  {
    PLogError("NEW_ARRAY failed for srec.g2g.graph.wordmap.words %d\n", awordmap->max_words);
    rc = ESR_OUT_OF_MEMORY;
    goto CLEANUP;
  }
  awordmap->chars = NEW_ARRAY(char, awordmap->max_chars, L("srec.g2g.graph.wordmap.chars"));
  if (awordmap->chars == NULL)
  {
    PLogError("NEW_ARRAY failed for srec.g2g.graph.wordmap.chars %d\n", awordmap->max_chars);
    rc = ESR_OUT_OF_MEMORY;
    goto CLEANUP;
  }
  awordmap->next_chars = awordmap->chars + next_chars_idx;
  awordmap->next_base_chars = awordmap->chars + next_base_chars_idx;

  if ((i=pfread(awordmap->chars, sizeof(char), awordmap->max_chars, fp)) != (size_t)awordmap->max_chars)
  {
    PLogError("pfread failed while reading %d chars\n", awordmap->max_chars);
    rc = ESR_READ_ERROR;
    goto CLEANUP;
  }


  p = awordmap->chars;
  ASSERT((int)p % 2 == 0);
  nfields = 0;
  if (nfields < awordmap->num_words)
    awordmap->words[nfields++] = p;
  for (; p < awordmap->next_chars; p++) // was next_base_chars
  {
    if ( ( *p ) == '\0' )
    {
      if (nfields == awordmap->num_words) // was num_base_words
        break;
      if (((int)p) % 2 == 0) p++; /* so that words begin on even byte bound */
      awordmap->words[nfields++] = p + 1;
    }
  }
  ASSERT(nfields == awordmap->num_words); // was num_base_words

  if (awordmap->max_words >= awordmap->num_base_words) 
    {
      PHashTableArgs hashArgs;
      hashArgs.capacity = awordmap->max_words;
      if(hashArgs.capacity%2==0) hashArgs.capacity += 1; 
      hashArgs.compFunction = HashCmpWord; //PHASH_TABLE_DEFAULT_COMP_FUNCTION;
      hashArgs.hashFunction = HashGetCode; //PHASH_TABLE_DEFAULT_HASH_FUNCTION;
      hashArgs.maxLoadFactor = PHASH_TABLE_DEFAULT_MAX_LOAD_FACTOR;
      CHKLOG(rc, PHashTableCreate(&hashArgs, L("srec.graph.wordmap.wordIDForWord.deserializeWordMap()"), &awordmap->wordIDForWord));
      
      rc = wordmap_populate ( awordmap, awordmap->num_words );
      
      if (rc != ESR_SUCCESS)
	{
        wordmap_clean ( awordmap );
        goto CLEANUP;
      }
    }
  else
    {
      awordmap->wordIDForWord = NULL;
    }
  
  /* success */
  *pwordmap = awordmap;
  return ESR_SUCCESS;

 CLEANUP:
  if (awordmap != NULL)
  {
    if (awordmap->wordIDForWord != NULL)
      PHashTableDestroy(awordmap->wordIDForWord);
    if (awordmap->words != NULL) FREE(awordmap->words);
    if (awordmap->chars != NULL) FREE(awordmap->chars);
    FREE(awordmap);
  }
  return rc;
}

static ESR_ReturnCode serializeArcToken(arc_token *token_base,
                                        int i,
                                        PFile* fp)
{
  arc_token *token = token_base + i;
  asr_uint32_t idx;

  if (pfwrite(&token->ilabel, 2, 1, fp) != 1)
    return ESR_WRITE_ERROR;

  if (pfwrite(&token->olabel, 2, 1, fp) != 1)
    return ESR_WRITE_ERROR;

  /* if (pfwrite(&token->cost, 2, 1, fp) != 1)
     return ESR_WRITE_ERROR; */
  idx = PTR_TO_IDX(ARC_TOKEN_PTR(token_base, token->first_next_arc), token_base);

  if (pfwrite(&idx, 4, 1, fp) != 1)
    return ESR_WRITE_ERROR;

  idx = PTR_TO_IDX(ARC_TOKEN_PTR(token_base, token->next_token_index), token_base);

  if (pfwrite(&idx, 4, 1, fp) != 1)
    return ESR_WRITE_ERROR;

  return ESR_SUCCESS;
}

static ESR_ReturnCode serializeArcTokenInfo(srec_context *context,
    PFile* fp)
{
  int i;
  asr_uint32_t idx;
  ESR_ReturnCode rc;

  if (pfwrite(&context->arc_token_list_len, 2, 1, fp) != 1)
    return ESR_WRITE_ERROR;

  idx = PTR_TO_IDX(context->arc_token_freelist, context->arc_token_list);

  if (pfwrite(&idx, 4, 1, fp) != 1)
    return ESR_WRITE_ERROR;

  idx = PTR_TO_IDX(context->arc_token_insert_start, context->arc_token_list);

  if (pfwrite(&idx, 4, 1, fp) != 1)
    return ESR_WRITE_ERROR;

  for (i = 0; i < context->arc_token_list_len; ++i)
  {
    rc = serializeArcToken(context->arc_token_list, i, fp);
    if (rc != ESR_SUCCESS) return rc;
  }
  return ESR_SUCCESS;
}

static ESR_ReturnCode deserializeArcToken(arc_token *token_base,
    int i,
    PFile* fp)
{
  arc_token *token = token_base + i;
  asr_uint32_t idx[2];
  asr_uint16_t labels[2];

  if (pfread(labels, 2, 2, fp) != 2)
    return ESR_READ_ERROR;

  token->ilabel = labels[0];
  token->olabel = labels[1];

  /* if (pfread(&token->cost, 2, 1, fp) != 1)
     return ESR_READ_ERROR; */

  if (pfread(idx, 4, 2, fp) != 2)
    return ESR_READ_ERROR;

  token->first_next_arc = ARC_TOKEN_PTR2LNK(token_base, IDX_TO_PTR(idx[0], token_base));
  token->next_token_index = ARC_TOKEN_PTR2LNK(token_base, IDX_TO_PTR(idx[1], token_base));

  return ESR_SUCCESS;
}

static ESR_ReturnCode deserializeArcTokenInfo(srec_context *context,
    PFile* fp)
{
  ESR_ReturnCode rc;
  int i;
  asr_uint32_t idx;

  if (pfread(&context->arc_token_list_len, 2, 1, fp) != 1) {
    PLogError("pfread failed in deserializeArcTokenInfo()\n");
    return ESR_READ_ERROR;
  }

  context->arc_token_list = NEW_ARRAY(arc_token, context->arc_token_list_len,
                                      L("srec.graph.wordgraph"));

  if (context->arc_token_list == NULL)
    return ESR_OUT_OF_MEMORY;

  if (pfread(&idx, 4, 1, fp) != 1)
  {
    rc = ESR_READ_ERROR;
    goto CLEANUP;
  }

  context->arc_token_freelist =
    IDX_TO_PTR(idx, context->arc_token_list);

  if (pfread(&idx, 4, 1, fp) != 1)
  {
    rc = ESR_READ_ERROR;
    goto CLEANUP;
  }

  context->arc_token_insert_start =
    IDX_TO_PTR(idx, context->arc_token_list);

  for (i = 0; i < context->arc_token_list_len; ++i)
  {
    rc = deserializeArcToken(context->arc_token_list, i, fp);
    if (rc != ESR_SUCCESS) goto CLEANUP;
  }
  return ESR_SUCCESS;

CLEANUP:
  FREE(context->arc_token_list);
  context->arc_token_list =
    context->arc_token_freelist =
      context->arc_token_insert_start = NULL;
  return rc;
}

int FST_GetGrammarType(srec_context* context)
{
  arc_token *t, *u;
  wordID expected_wdid;
  /* 0 1 0
     1 2 4
     ...
     1 2 1316
     2
  */
  t = context->arc_token_list;
  if (t->ilabel != WORD_EPSILON_LABEL)
    return GrammarTypeBNF;
  if (t->next_token_index)
    return GrammarTypeBNF;
  t = ARC_TOKEN_PTR(context->arc_token_list, t->first_next_arc);
  expected_wdid = NUM_ITEMLIST_HDRWDS;
  for (; t; t = ARC_TOKEN_PTR(context->arc_token_list, t->next_token_index))
  {
    if (t->ilabel != expected_wdid)
      return GrammarTypeBNF;
    u = ARC_TOKEN_PTR(context->arc_token_list, t->first_next_arc);
    if (u != NULL && u->ilabel != MAXwordID)
      return GrammarTypeBNF;
    expected_wdid++;
  }
  if (expected_wdid != context->olabels->num_words)
    return GrammarTypeBNF;
  return GrammarTypeItemList;
}

int FST_DumpContextAsImageV2(srec_context* context, PFile* fp)
{
  asr_uint32_t header[4];
  arcID tmp[32], i, j, nfields;
  FSMarc* arc;
  ESR_ReturnCode rc;
#if !defined(DO_ARCS_IO_IN_ARC_ORDER)
  FSMnode* node;
  arcID arcid, num_arcs, num_arcs_written;
#endif

  /* Write header information. */
  header[0] = 0;
  header[1] = IMAGE_FORMAT_V2;
  header[2] = context->modelid;
  header[3] = context->grmtyp;
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("FST_DumpContextAsImageV2() fstgrmtyp %d", context->grmtyp);
#endif

  if (pfwrite(header, sizeof(header[0]), 4, fp) != 4)
  {
    PLogError("FST_DumpContextAsImage: Could not write header.\n");
    return FST_FAILED_INTERNAL;
  }

  /* arcs and nodes */
  i = 0;
  tmp[i++] = context->num_arcs;
  tmp[i++] = context->FSMarc_list_len;
  tmp[i++] = context->num_base_arcs;
  tmp[i++] = context->FSMarc_freelist;
  tmp[i++] = context->max_searchable_arcs;

  tmp[i++] = context->num_nodes;
  tmp[i++] = context->FSMnode_list_len;
  tmp[i++] = context->num_base_nodes;
  tmp[i++] = context->FSMnode_freelist;
  tmp[i++] = context->start_node;
  tmp[i++] = context->end_node;
  tmp[i++] = context->max_searchable_nodes;

  tmp[i++] = context->beg_silence_word;
  tmp[i++] = context->end_silence_word;
  tmp[i++] = context->hack_silence_word;
  tmp[i++] = context->hmm_ilabel_offset;

  nfields = i;

  if (pfwrite(tmp, sizeof(tmp[0]), nfields, fp) != nfields)
    return ESR_WRITE_ERROR;
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("G2G done WR hdrs %d", pftell(fp));
#endif

#if defined(DO_ARCS_IO_IN_ARC_ORDER)
  if( 1) {
	 i = 0;
     tmp[i++] = context->end_node;
     tmp[i++] = MAXnodeID;
     tmp[i++] = MAXmodelID;
     tmp[i++] = MAXwordID;
     tmp[i++] = FREEcostdata;
     nfields = i;
     if (pfwrite(tmp, sizeof(tmp[0]), nfields, fp) != nfields)
        return ESR_WRITE_ERROR;
  }
  for(j=0; j<context->num_arcs; j++) {
        arc = &context->FSMarc_list[j];
        i = 0;
        tmp[i++] = arc->fr_node;
        tmp[i++] = arc->to_node;
	ASSERT(arc->to_node == context->end_node || arc->to_node != MAXnodeID);
        tmp[i++] = arc->ilabel;
        tmp[i++] = arc->olabel;
        tmp[i++] = arc->cost;
        nfields = i;
        if (pfwrite(tmp, sizeof(tmp[0]), nfields, fp) != nfields)
          return ESR_WRITE_ERROR;
  }
#else
  num_arcs_written = 0;
  for (j = 0; j < context->num_nodes; j++) {
    node = &context->FSMnode_list[j];
    num_arcs = (arcID)num_arcs_leaving(context, node);
    arcid = node->un_ptr.first_next_arc;
    if (arcid == MAXarcID) {
      i = 0;
      tmp[i++] = (arcID)j;
      tmp[i++] = MAXarcID;
      tmp[i++] = MAXarcID;
      tmp[i++] = MAXarcID;
      tmp[i++] = MAXarcID;
      nfields = i;
      if (pfwrite(tmp, sizeof(tmp[0]), nfields, fp) != nfields)
        return ESR_WRITE_ERROR;
      /* num_arcs_written++; end node don't have a to-arc */
    }
    else {
      for (; arcid != MAXarcID; arcid = arc->linkl_next_arc) {
        arc = &context->FSMarc_list[arcid];
        i = 0;
        tmp[i++] = (arcID)j;
        tmp[i++] = arc->to_node;
	ASSERT(arc->to_node == context->end_node || arc->to_node != MAXnodeID);
        tmp[i++] = arc->ilabel;
        tmp[i++] = arc->olabel;
        tmp[i++] = arc->cost;
        nfields = i;
        if (pfwrite(tmp, sizeof(tmp[0]), nfields, fp) != nfields)
          return ESR_WRITE_ERROR;
        num_arcs_written++;
      }
    }
  }
  ASSERT(num_arcs_written == context->num_arcs);
#endif

#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("G2G done WR arcs %d", pftell(fp));
#endif

  /* node info   .. will now be calculated on line */
  /* wrapup_cost .. will now be calculated on line */

  /* exit points for slots */
  if (1)
  {
    srec_fsm_exit_point *p, *q;
    if (pfwrite(&context->num_fsm_exit_points, 2, 1, fp) != 1)
      return ESR_WRITE_ERROR;
    p = context->fsm_exit_points;
    q = p + MAX_NUM_SLOTS;
    while (p < q)
    {
      if (pfwrite(&p->from_node_index, 2, 1, fp) != 1)
        return ESR_WRITE_ERROR;
      if (pfwrite(&p->arc_index, 2, 1, fp) != 1)
        return ESR_WRITE_ERROR;
      if (pfwrite(&p->wbto_node_index, 2, 1, fp) != 1)
        return ESR_WRITE_ERROR;
      ++p;
    }
  }
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("G2G done WR exits %d", pftell(fp));
#endif
  /* no entry points stuff */

  /* no saving ilabels */

  /* now load the ilabels */
  rc = serializeWordMapV2(context->olabels, fp);
  if (rc != ESR_SUCCESS)
    return rc;
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("G2G done WR wmap %d", pftell(fp));
#endif

  if (context->grmtyp != GrammarTypeItemList)
  {
    if (serializeArcTokenInfo(context, fp) != ESR_SUCCESS)
    {
      PLogError("FST_DumpContextAsImage: could not write arc token data.\n");
      return FST_FAILED_INTERNAL;
    }
  }
  else
  {
    /* nothing, at decoding time, we'll just construct the graph
       from the word list, ie just this ..
       0 1 0
       1 2 4
       ...
       1 2 1316
       2
       ie
       0 1 0
       for(i=4;i<nwords;i++) { "1 2 i" }
       2
       because words 0-3 are eps,-pau-,-pau2-,slot@grammarname.grxml
    */
  }

  /* Write header information. */
  header[0] = pftell(fp);
  header[1] = IMAGE_FORMAT_V2;

  /* goto beginning of file. */
  if (pfseek(fp, 0, SEEK_SET))
  {
    PLogError("FST_DumpContextAsImage: could not reposition for header.\n");
    return FST_FAILED_INTERNAL;
  }

  if (pfwrite(header, 4, 2, fp) != 2)
  {
    PLogError("FST_DumpContextAsImage: Could not write header.\n");
    return FST_FAILED_INTERNAL;
  }

  /* reset pointer at end of file. */
  if (pfseek(fp, 0, SEEK_END))
  {
    PLogError("FST_DumpContextAsImage: could not reposition file pointer at end.\n");
    return FST_FAILED_INTERNAL;
  }

  return FST_SUCCESS;
}


int FST_LoadContextFromImageV2(srec_context* fst, PFile* fp)
{
  asr_int32_t header[6];
  arcID tmp[32], num_arcs, new_arc_id;
  unsigned int i, nfields;
  srec_fsm_exit_point *p, *q;
  arcID max_num_FSMarcs;
  nodeID max_num_FSMnodes;
  FSMarc_ptr atok =  (FSMarc_ptr)0;
  FSMarc *atoken = NULL;
  FSMnode *fr_node, *to_node;
  int rc = FST_SUCCESS;
  ESR_ReturnCode esr_rc;
  ESR_BOOL seenFinalNode = ESR_FALSE;

  /* read header information. */
  if (pfread(&header[2], sizeof(header[0]), 2, fp) != 2)
  {
    PLogError("FST_DumpContextAsImage: Could not read header.\n");
    return FST_FAILED_INTERNAL;
  }
  fst->modelid = header[2];
  fst->grmtyp  = header[3];

  nfields = 16;
  if (pfread(tmp, sizeof(tmp[0]), nfields, fp) != nfields)
    return ESR_READ_ERROR;

  /* arcs and nodes */
  i = 0;
  fst->num_arcs             = max_num_FSMarcs = tmp[i++];
  fst->FSMarc_list_len      = tmp[i++];
  fst->num_base_arcs        = tmp[i++];
  fst->FSMarc_freelist      = tmp[i++];
  fst->max_searchable_arcs  = tmp[i++] * 0; /*5*/
  fst->num_nodes            = max_num_FSMnodes = tmp[i++];
  fst->FSMnode_list_len     = tmp[i++];
  fst->num_base_nodes       = tmp[i++];
  fst->FSMnode_freelist     = tmp[i++];
  fst->start_node           = tmp[i++];/*10*/
  fst->end_node             = tmp[i++];
  fst->max_searchable_nodes = tmp[i++] * 0;
  fst->beg_silence_word     = tmp[i++];
  fst->end_silence_word     = tmp[i++];
  fst->hack_silence_word    = tmp[i++]; /*15*/
  fst->hmm_ilabel_offset    = tmp[i++]; /* 16 */

  fst->addWordCaching_lastslot_name = 0;
  fst->addWordCaching_lastslot_num = MAXwordID;
  fst->addWordCaching_lastslot_needs_post_silence = ESR_FALSE;

  ASSERT(i == nfields);
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("G2G done RD hdrs %d", pftell(fp));
#endif

  ASSERT(fst->num_arcs >= fst->num_base_arcs); // was ==
  ASSERT(fst->num_nodes >= fst->num_base_nodes); // was ==

  fst->FSMarc_list = (FSMarc*)CALLOC_CLR(fst->FSMarc_list_len, sizeof(FSMarc), "srec.graph.arcs");
  if (!fst->FSMarc_list)
  {
    rc = FST_FAILED_ON_MEMORY;
    PLogError("CALLOC_CLR fst->FSMarc_list \n");
    goto CLEANUP;
  }
  fst->FSMnode_list = (FSMnode*)CALLOC_CLR(fst->FSMnode_list_len, sizeof(FSMnode), "srec.graph.nodes");
  if (!fst->FSMnode_list)
  {
    rc = FST_FAILED_ON_MEMORY;
    PLogError("CALLOC_CLR fst->FSMnode_list  failed\n");
    goto CLEANUP;
  }
  fst->FSMnode_info_list = (FSMnode_info*)CALLOC_CLR(fst->FSMnode_list_len, sizeof(FSMnode_info), "srec.graph.nodeinfos");

  if (!fst->FSMnode_info_list)
  {
    rc = FST_FAILED_ON_MEMORY;
    PLogError("CALLOC_CLR fst->FSMnode_info_list failed\n");
    goto CLEANUP;
  }

  /* setup the arc freelist */
  fst->FSMarc_freelist = 0;
  fst->num_arcs = 0;
  for (i = 0; i < (unsigned)fst->FSMarc_list_len - 1; i++)
  {
    fst->FSMarc_list[i].linkl_next_arc = ARC_ItoX(i + 1);
    fst->FSMarc_list[i].linkl_prev_arc = FSMARC_FREE;
  }
  fst->FSMarc_list[i].linkl_next_arc = FSMARC_NULL;
  fst->FSMarc_list[i].linkl_prev_arc = FSMARC_FREE;

  /* initialize the nodes, 'cuz reading is random order */
  for (i = 0; i < max_num_FSMnodes; i++)
  {
    fr_node = &fst->FSMnode_list[i];
    fr_node->un_ptr.first_next_arc = fr_node->first_prev_arc = FSMARC_NULL;
  }

  /* 1. first load up all the information */
  num_arcs = 0;
  for (i = 0; i < max_num_FSMarcs || !seenFinalNode; i++)
  {
    if (i > max_num_FSMarcs && !seenFinalNode)
    {
      PLogError("Final node never encountered");
      rc = ESR_INVALID_STATE;
      goto CLEANUP;
    }
	nfields = 5;
    if (pfread(tmp,sizeof(tmp[0]),nfields,fp) != nfields)
    {
      PLogError("reading arc");
      rc = ESR_INVALID_STATE;
      goto CLEANUP;
    }

    if (tmp[1] == MAXnodeID)
    {
      seenFinalNode = ESR_TRUE;
      if(fst->end_node != tmp[0]) {
	PLogError("error with arc %d->%d ilabel %d olabel %d cost %d\n", tmp[0], tmp[1], tmp[2], tmp[3], tmp[4]);
      ASSERT(fst->end_node == tmp[0]);
      }
      i--;
    }
    else
    {
      new_arc_id = fst_get_free_arc(fst);
      if (new_arc_id == MAXarcID)
        return FST_FAILED_ON_MEMORY;
      atok = ARC_ItoX(new_arc_id);
      atoken = ARC_XtoP(atok);
      num_arcs++;

      ASSERT(tmp[0] < fst->num_nodes);
      ASSERT(tmp[1] < fst->num_nodes);
      fr_node = &fst->FSMnode_list[ tmp[0]];
      to_node = &fst->FSMnode_list[ tmp[1]];
      atoken->ilabel = tmp[2];
      atoken->olabel = tmp[3];
      atoken->cost   = tmp[4];
      append_arc_leaving_node(fst, fr_node, atok);
      append_arc_arriving_node(fst, to_node, atok);
      atoken->fr_node = NODE_ItoX(tmp[0]);
      atoken->to_node = NODE_ItoX(tmp[1]);
    }
  }
  ASSERT(fst->num_arcs == num_arcs);
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("G2G done RD arcs %d", pftell(fp));
#endif

  /* setup the node freelist */
  if (fst->num_nodes < fst->FSMnode_list_len)
  {
    fst->FSMnode_freelist = fst->num_nodes;
    for (i = fst->num_nodes; i < (unsigned)fst->FSMnode_list_len - 1; i++)
    {
      fst->FSMnode_list[i].un_ptr.next_node = NODE_ItoX(i + 1);
      fst->FSMnode_list[i].first_prev_arc = FSMARC_FREE;
    }
    fst->FSMnode_list[i].un_ptr.next_node = FSMNODE_NULL;
    fst->FSMnode_list[i].first_prev_arc = FSMARC_FREE;
  }
  else
  {
    fst->FSMnode_freelist = MAXnodeID;
  }

  /* node info   .. will now be calculated on line */
  /* wrapup_cost .. will now be calculated on line */
  fst->whether_prepared = 0;
  fst_fill_node_info(fst);
  for (i = 0; i < fst->num_nodes; i++)
    fst->FSMnode_info_list[i] = NODE_INFO_UNKNOWN;

  /* exit points for slots */
  if (pfread(&fst->num_fsm_exit_points, 2, 1, fp) != 1)
    return ESR_READ_ERROR;
  p = fst->fsm_exit_points;
  q = p + MAX_NUM_SLOTS;
  while (p < q)
  {
    if (pfread(&p->from_node_index, 2, 1, fp) != 1)
      return ESR_WRITE_ERROR;
    if (pfread(&p->arc_index, 2, 1, fp) != 1)
      return ESR_WRITE_ERROR;
    if (pfread(&p->wbto_node_index, 2, 1, fp) != 1)
      return ESR_WRITE_ERROR;
    ++p;
  }
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("G2G done RD exits %d", pftell(fp));
#endif

  /* no entry points stuff */

  /* ilabels were not saved, create them as empty */
  fst->ilabels = (wordmap*)CALLOC_CLR(1, sizeof(wordmap), "srec.graph.imap");
  fst->ilabels->num_words = fst->ilabels->max_words = 0;
  fst->ilabels->words = 0;

  /* now save the olabels */
  esr_rc = deserializeWordMapV2(&fst->olabels, fp);
  if (esr_rc != ESR_SUCCESS)
  {
    PLogError("deserializeWordMapV2() failed\n");
    rc = FST_FAILED_INTERNAL;
    goto CLEANUP;
  }
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("G2G done RD wmap %d", pftell(fp));
#endif

  /* arctokeninfo */
  if (fst->grmtyp != GrammarTypeItemList)
  {
    if (deserializeArcTokenInfo(fst, fp) != ESR_SUCCESS)
    {
      PLogError("FST_DumpContextAsImage: could not write arc token data.\n");
      rc = FST_FAILED_INTERNAL;
      goto CLEANUP;
    }
  }
  else
  {
    /* here we just construct an isolated list, but we should scrap it later */
    wordID wdid;
    arc_token *atl, *final, *atli;
    fst->arc_token_list_len = fst->olabels->num_words + 2 - NUM_ITEMLIST_HDRWDS;
    atl = NEW_ARRAY(arc_token, fst->arc_token_list_len, L("srec.graph.wordgraph"));
    if(!atl) {
      PLogError("fst->arc_token_list_len failed\n");
      goto CLEANUP;
    }
    atl->first_next_arc = ARC_TOKEN_LNK(atl, 1);
    atl->next_token_index = ARC_TOKEN_NULL;
    final = atl + fst->arc_token_list_len - 1;
    for (i = 1, wdid = NUM_ITEMLIST_HDRWDS; wdid < fst->olabels->num_words; i++, wdid++)
    {
      atli = &atl[i];
      atli->ilabel = wdid;
      atli->olabel = wdid;/*not used*/
      atli->next_token_index = ARC_TOKEN_LNK(atl, (i + 1));
      atli->first_next_arc = ARC_TOKEN_LNK(atl, (fst->arc_token_list_len - 1)); /*final*/
    }
    ASSERT(atl + i == final);
    final->next_token_index = ARC_TOKEN_NULL;
    final->first_next_arc = ARC_TOKEN_NULL;
    final->ilabel = final->olabel = 0;
    fst->arc_token_list = atl;
  }
  return FST_SUCCESS;
CLEANUP:
  return FST_FAILED_INTERNAL;
}


int FST_LoadContextFromImageV2(srec_context* context, PFile* fp);

int FST_LoadContextFromImage(srec_context** pcontext, PFile* fp)
{
  srec_context* context = NULL;
  /* used to force correct data alignment */
  asr_int32_t header[2];
  int rc = FST_SUCCESS;

  /*printf("FST_LoadContexFromImage\n");*/
  if (!fp)
  {
    PLogError("FST_LoadContextImage() failed; bad file pointer\n");
    return FST_FAILED_ON_INVALID_ARGS;
  }

  context = NEW(srec_context, L("srec.graph.binary"));
  if (context == NULL)
  {
    PLogError("FST_LoadContextFromImage: out of memory while allocating context.\n");
    return FST_FAILED_ON_MEMORY;
  }
  memset(context, 0, sizeof(srec_context));

  /* header */
  if (pfread(header, 4, 2, fp) != 2)
  {
    PLogError("FST_LoadContextFromImage: failed reading header.\n");
    rc = FST_FAILED_ON_INVALID_ARGS;
    goto CLEANUP;
  }

  if (header[1] == IMAGE_FORMAT_V2)
  {
    rc = FST_LoadContextFromImageV2(context, fp);
    if (rc != FST_SUCCESS)
      goto CLEANUP;
  }
  else
  {
    PLogError("FST_LoadContextFromImage() failed on image_format\n");
    goto CLEANUP;
  }
  *pcontext = context;
  return FST_SUCCESS;
CLEANUP:
  if (context) FREE(context);
  *pcontext = 0;
  return rc;
}


int FST_DumpReverseWordGraph(srec_context* context, PFile* fp)
{
  /* not implemented, use FST_DumpSyntaxAsImage() for now */
  return 1;
}

/* this belongs part of the srec_context */
typedef struct
{
  asr_int32_t image_format;
  asr_int32_t image_size;
  asr_int32_t sizes_signature;
}
context_image_header;


int FST_LoadContextFromStreamImage(srec_context** pcontext, char* buffer, asr_int32_t buffer_len)
{
  return FST_FAILED_INTERNAL;
}

typedef struct nodeID_list_t
{
  nodeID *nodes, num_nodes, num_alloc;
}
nodeID_list;

int fst_node_has_speech_to_come(srec_context* context, nodeID node_index)
{
  /* recursive func ... try to keep stack use low! */
  /* later we should cache this information:
     need 2bits:  endn opte regl dunno */
  FSMarc* arc;
  arcID arc_index = context->FSMnode_list[ node_index].un_ptr.first_next_arc;
  for (; arc_index != MAXarcID; arc_index = arc->linkl_next_arc)
  {
    arc = &context->FSMarc_list[arc_index];
    if (arc->ilabel >= context->hmm_ilabel_offset + EPSILON_OFFSET + NUM_SILENCE_HMMS)
    {
      return 1;
    }
    else if (arc->ilabel < EPSILON_OFFSET)
    {
      if (fst_node_has_speech_to_come(context, arc->to_node))
      {
        return 1;
      }
    }
    else if (IS_SILENCE_ILABEL(arc->ilabel, context))
    {
      /* another silence! */
      if (fst_node_has_speech_to_come(context, arc->to_node))
        return 1;
    }
  }
  return 0;
}

int fst_fill_node_info(srec_context* context)
{
  char* infos = context->FSMnode_info_list;
  nodeID_list optends_obj;
  nodeID_list *optends = &optends_obj;

  FSMnode* node;
  FSMarc* arc;
  nodeID i, j, node_index;
  arcID arc_index;
  costdata wrapup_cost;
  int wrapup_cost_inconsistencies;
  /* int num_near_end_nodes; nodes within EPS or silence to the end, these
     are called NODE_INFO_ENDNODES, so that they can be excluded from the
     comparison with end_node score in the eos detection */

  optends->num_nodes    = 1;
  optends->num_alloc    = 8192; /* scaled up from 512 to support dynamic grammar add word of 5000 */
  optends->nodes        = (nodeID*)CALLOC(optends->num_alloc, sizeof(nodeID), "srec.tmp.optendnodes");
  optends->nodes[0]     = context->end_node;
  /* num_near_end_nodes = 0; */

  for (i = 0; i < optends->num_nodes; i++)
  {
    node_index = optends->nodes[i];
    node = &context->FSMnode_list[ node_index];
    for (arc_index = node->first_prev_arc; arc_index != MAXarcID;
         arc_index = arc->linkl_prev_arc)
    {
      arc = &context->FSMarc_list[arc_index];
      if (arc->fr_node != node_index)
      {

        if (IS_SILENCE_ILABEL(arc->ilabel, context) || arc->ilabel < EPSILON_OFFSET)
        {
          /* ok, fr_node goes to the end via silence, check for dups */
          for (j = 0; j < optends->num_nodes; j++)
            if (optends->nodes[j] == arc->fr_node) break;
          /* append it to the list */
          if (j == optends->num_nodes)
          {
            if (optends->num_nodes < optends->num_alloc)
              optends->nodes[ optends->num_nodes++] = arc->fr_node;
            else
            {
              ASSERT(0 && "allocated too few optend nodes");
              return 0;
            }
          }
        }
      }
    }
  }

  /* now set the info array, default is regular */
  for (i = 0; i < context->num_nodes; i++)
    infos[i] = NODE_INFO_REGULAR;
  /* also set the other nodes, we'll check whether nodes were added
     via these flags */
  for (; i < context->FSMnode_list_len; i++)
    infos[i] = NODE_INFO_UNKNOWN;
  infos[ context->end_node] = NODE_INFO_ENDNODE;

  /* get rid of direct to end nodes, etc */
  for (i = 0, j = 0; i < optends->num_nodes; i++)
  {
    optends->nodes[j] = optends->nodes[i];
    if (fst_node_has_speech_to_come(context, optends->nodes[i]))
    {
      j++;
      infos[ optends->nodes[i]] = NODE_INFO_OPTENDN;
    }
    else
    {
      infos[ optends->nodes[i]] = NODE_INFO_ENDNODE;
      /* num_near_end_nodes++; */
    }
  }
  optends->num_nodes = j;

  /* printf("get_oppend_nodes (%d)", optends->num_nodes);
     for(i=0; i<optends->num_nodes; i++) printf(" %d", optends->nodes[i]);
     printf("\n");
  */

  FREE(optends->nodes);

  /* find the wrapup cost */
  node = &context->FSMnode_list[ context->end_node];
  wrapup_cost = MAXcostdata;
  wrapup_cost_inconsistencies = 0;
  for (arc_index = node->first_prev_arc; arc_index != MAXarcID;
       arc_index = arc->linkl_prev_arc)
  {
    arc = &context->FSMarc_list[ arc_index];
    if (IS_SILENCE_ILABEL(arc->ilabel, context) &&
        arc->olabel == context->end_silence_word)
    {
      if (wrapup_cost == MAXcostdata)
        wrapup_cost = arc->cost;
      else if (context->wrapup_cost != arc->cost)
      {
        wrapup_cost = arc->cost;
        wrapup_cost_inconsistencies++;
      }
    }
  }
#define MIN_WRAPUP_COST 100
  context->wtw_average = wrapup_cost;
  if (context->wtw_average > 200)
    context->wtw_average = 200;
  if (context->wrapup_cost < MIN_WRAPUP_COST)
    context->wrapup_cost = MIN_WRAPUP_COST;
  /*log_report("context->wrapup_cost %d (%d)\n", context->wrapup_cost,
    wrapup_cost_inconsistencies);*/
  return 0;
}

int fst_alloc_transit_points(srec_context* context)
{
  arcID i;
  wordID num_slots = context->olabels->num_slots;
  wordID olabel;
  asr_int16_t nfxps = 0;
  FSMarc* arc;
  FSMnode* node;

  context->num_fsm_exit_points = 0;
  /* slot 0 is invalid, it is the "eps", so num_slots==1 means no slots! */
  if (num_slots == 1) 
    return 0;

  /* scan through to finds arc that carry rule references */
  for (i = 0; i < context->num_arcs; i++)
  {
    olabel =  context->FSMarc_list[i].olabel;
    /* (wordID)(olabel-1) < (num_slots-1) ?? might work */
    if (olabel > 0 && olabel < num_slots)
    {
      context->FSMarc_list[i].cost = DISABLE_ARC_COST;
      if (nfxps >= MAX_NUM_SLOTS)
      {
        PLogError("error: too many fsm exit points in fsm, too many public rules referenced from here\n");
        return 0;
      }
      context->fsm_exit_points[nfxps].arc_index = i;
      context->fsm_exit_points[nfxps].from_node_index = context->FSMarc_list[i].fr_node;
      /* skip over the trailing silence and .wb labels */
      for (arc = &context->FSMarc_list[i]; arc->ilabel != WORD_BOUNDARY;)
      {
        node = &context->FSMnode_list[arc->to_node];
        arc =  &context->FSMarc_list[node->un_ptr.first_next_arc];
      }
      context->fsm_exit_points[nfxps].wbto_node_index = arc->to_node;
      nfxps++;

    } /* olabel<num_slots */
  } /* i<num_arcs */
  context->num_fsm_exit_points  = nfxps;
  return 0;
}


/*
  cross-word modeling:
  for now we'll assume silence context on either side, later we'll create
  a graph with the right fan out on the edges of each slot.  At that point we
  may have routines like ... if(is_dead_end(arc)) continue;

  word removal:
  we'll use refcounters on arcs, we can search the whole graph for the
  olabel, then remove forward and remove backwards

  word addition:
  glenville ^ hmm183 hmm222 hmm162 hmm246 hmm346 hmm191 hmm219

  set imap=C:/users/dahan/speech2go/fst/enu_d2f_fray_g/model.map
  set omap=C:/users/dahan/speech2go/fst/enu_d2f_fray_g/namesnnumsSC.map
  wintel/debug/addword.exe -min
  $SWISDK/bin/fsmcompileD -t -i $imap -o $omap -F out.PCLG out.PCLG.txt
  $SWISDK/bin/fsmdrawD -i $imap -o $omap out.PCLG > out.PCLG.dot
  dotty out.PCLG.dot

  need
  - a sanity check
  - remove word
  - multiple pronunciations handling
  - homonyms handling, other boundary conditions checking
  - measure the speed

  alan_adams al~#ad}z
  alan_adams al~ad}z
  paul_adams p{l#ad}z



*/
