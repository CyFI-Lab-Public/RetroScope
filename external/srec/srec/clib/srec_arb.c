/*---------------------------------------------------------------------------*
 *  srec_arb.c                                                               *
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

#include "pstdio.h"
#include "passert.h"
#include "portable.h"

#include<string.h>

#include"portable.h"

#include"sizes.h"
#include"hmm_desc.h"
#include"search_network.h"     /* for EPSILON_OFFSET */
#include"srec_arb.h"

#define DEBUG_PRONS       0
#define IF_DEBUG_PRONS(X)

static const char *rcsid = 0 ? (const char *) &rcsid :
"$Id: srec_arb.c,v 1.27.4.15 2007/12/14 22:03:51 dahan Exp $";

int question_check(srec_question* quest, phonemeID lphon, phonemeID cphon, phonemeID rphon)
{
  asr_int16_t a = 0, b = 0;
  /* phon = a*16+b */
  if (quest->qtype == QUESTION_LEFT)
  {
    BIT_ADDRESS(lphon, a, b);
  }
#if USE_WWTRIPHONE
  else if(quest->qtype == QUESTION_WBLEFT) {
    if( lphon == WBPHONEME_CODE) return ANSWER_PASS;
    else return ANSWER_FAIL;
  }
  else if(quest->qtype == QUESTION_WBRIGHT) {
    if( rphon == WBPHONEME_CODE) return ANSWER_PASS;
    else return ANSWER_FAIL;
  }
#endif
  else
  {
    ASSERT(quest->qtype == QUESTION_RIGHT);
    BIT_ADDRESS(rphon, a, b);
  }
  return (quest->membership_bits[a] & b ? ANSWER_PASS : ANSWER_FAIL);
}

/* get model id for phoneme in context */
int get_modelid_for_pic(srec_arbdata* allotree, phonemeID lphon, phonemeID cphon, phonemeID rphon)
{
  int ans;
  tree_node* tnode = allotree->pdata[cphon].model_nodes;
  while (tnode->node.quest_index >= 0)
  {
    ans = question_check(&allotree->questions[tnode->node.quest_index],
                         lphon, cphon, rphon);
    tnode = (ans == ANSWER_FAIL ? (tree_node*)tnode->node.fail : (tree_node*)tnode->node.pass);
  }
  return tnode->term.pelid;
}

void read_questions(srec_question** pquestions, asr_int16_t num_questions, char **buffer, PFile *fp)
{
  srec_question *q;

  q = *pquestions = (srec_question*)(*buffer);

  *buffer += num_questions * sizeof(srec_question);
  while (num_questions-- > 0)
  {
    pfread(&(q->qtype), sizeof(asr_uint16_t), 1, fp);
    pfread(&(q->membership_bits), sizeof(asr_uint16_t), PSET_BIT_ARRAY_SIZE, fp);
    q++;
  }
}

/* we need to handle the interword silence here somehow,
   proposal:  we create one supermodel which combines the
   the model preceding silence and that follows silence, so
   "boston&mass" .. we'll have "n&m" as a single "supermodel",
   we'll put that supermodel in the graph but then overlay the
   actual models there on.   the overlay only needs to be done
   once.  The number of possible supermodels is 113655 which is
   larger than what an ilabel can hold, the solution to that is
   to encode also on the "cost" of the supermodel arc.

   /------SUPER(a&b)---\
   o----a1---o----b1----o
   \--a2--o--#--o--b2--/

   cost is 16bits, ilabel is 16bits
   on ilabel we encode the a1,(a2-a1)
   on cost we encode b1,(b2-b1)
   ... a1,b1 use 9 bits (400 models)
   ... deltas use 6 bits (+/-31 range)
   That leaves 1 bit left over, which is the top bit to signal this encoding,
   and make sure the cost is very high.
*/


int get_modelids_for_pron(srec_arbdata* allotree,
                          const char* phonemes, int num_phonemes,
                          modelID* acoustic_model_ids)
{
  int i;
  modelID modelid;
  phonemeID lphon, cphon, rphon;

  if( allotree == NULL)
	  return 1;

  if (num_phonemes == 0)
    return 0;

  IF_DEBUG_PRONS(printf("%s get_modelids_for_pron pronunciation %s\n", __FILE__, (char*)phonemes));

#if !USE_WWTRIPHONE
  lphon = (phonemeID)allotree->phoneme_index[ SILENCE_CODE];
  cphon = (phonemeID)allotree->phoneme_index[ (unsigned)phonemes[0]];
#else
  lphon = WBPHONEME_CODE; //(phonemeID)allotree->phoneme_index[ WBPHONEME_CODE];
  cphon = (phonemeID)allotree->phoneme_index[ (unsigned)phonemes[0]];
#endif
  if(cphon == MAXphonemeID)
    return 1; /* bad phoneme */
  for(i=0; i<num_phonemes; i++) {
#if !USE_WWTRIPHONE
    rphon = (i==num_phonemes-1 ?
	     (phonemeID)allotree->phoneme_index[ SILENCE_CODE] :
	     (phonemeID)allotree->phoneme_index[ (unsigned)phonemes[i+1] ] ) ;
#else
    rphon = (i==num_phonemes-1 ?
	     WBPHONEME_CODE /*(phonemeID)allotree->phoneme_index[ WBPHONEME_CODE] */ :
	     (phonemeID)allotree->phoneme_index[ (unsigned)phonemes[i+1] ] ) ;
#endif
    if (rphon == MAXphonemeID)
      return 1; /* bad phoneme */

    modelid = (modelID) get_modelid_for_pic(allotree, lphon, cphon, rphon);
    acoustic_model_ids[i] = modelid;
#if DEBUG_PRONS
    printf("%c%c%c hmm%d states", allotree->pdata[lphon].code,
           allotree->pdata[cphon].code, allotree->pdata[rphon].code,
           acoustic_model_ids[i]);
    for (j = 0; j < allotree->hmm_infos[modelid].num_states; j++)
      printf(" %d", allotree->hmm_infos[modelid].state_indices[j]);
    printf("\n");
#endif
    lphon = cphon;
    cphon = rphon;
  }
  return 0;
}

/*-----------------------------------------------------------------------*
 *                                                                       *
 * phoneme data stream functions                                         *
 *                                                                       *
 *-----------------------------------------------------------------------*/

tree_node* read_tree_node_f(char **buffer, PFile *fp)
{
  tree_node* tnode = (tree_node*) * buffer;
  pfread(&(tnode->node.quest_index), sizeof(asr_int16_t), 1, fp);
  pfread(&(tnode->term.pelid), sizeof(asr_int16_t), 1, fp);
  pfread(&(tnode->node.fail), sizeof(tree_branch_info*), 1, fp);
  pfread(&(tnode->node.pass), sizeof(tree_branch_info*), 1, fp);

  /* because tree_node is a union, the actual size maybe large than we have read */
  ASSERT(sizeof(asr_int16_t)*2 + sizeof(tree_branch_info *)*2 == sizeof(tree_node));

  *buffer += sizeof(tree_node);
  if (tnode->node.quest_index >= 0)
  {
    tnode->node.fail = (struct tree_branch_info*)read_tree_node_f(buffer, fp);
    tnode->node.pass = (struct tree_branch_info*)read_tree_node_f(buffer, fp);
  }
  return tnode;
}

void read_phoneme_data(phoneme_data** pdata, asr_int16_t num_phonemes, char **buffer,  PFile *fp)
{
  int i, ptr;
  phoneme_data *pd;

  pd = *pdata = (phoneme_data*)(*buffer);

  for (i = 0; i < num_phonemes; i++)
  {
    pfread(&(pd->name), sizeof(char), MAX_PHONEME_NAME_LEN, fp);
    pfread(&(pd->code), sizeof(asr_uint16_t), 1, fp);
    pfread(&ptr, sizeof(asr_int16_t), 1, fp);
    pfread(&(pd->model_nodes), sizeof(tree_node *), 1, fp);
    pfread(&(pd->num_states), sizeof(asr_uint16_t), 1, fp);
    pfread(&ptr, sizeof(asr_int16_t), 1, fp);
    pfread(&(pd->state_nodes), sizeof(tree_node *), MAX_PHONE_STATES, fp);
    pd++;
  }
  ASSERT(sizeof(phoneme_data) == MAX_PHONEME_NAME_LEN + sizeof(asr_int16_t)*4 + sizeof(tree_node *)*(1 + MAX_PHONE_STATES));
  (*buffer) += num_phonemes * sizeof(phoneme_data) / BYTES_PER_ATOM;
  ASSERT((char *)pd == *buffer);

  for (i = 0; i < num_phonemes; i++)
  {
#if STATE_NODES_NEEDED_AT_RUNTIME
    for (j = 0; j < (*pdata)[i].num_states; j++)
      (*pdata)[i].state_nodes[j] = read_tree_node_f(buffer);
#endif
    (*pdata)[i].model_nodes = read_tree_node_f(buffer, fp);
  }
}

/*-----------------------------------------------------------------------*
 *                                                                       *
 * hmm info stream functions                                             *
 *                                                                       *
 *-----------------------------------------------------------------------*/

void read_hmminfos(srec_arbdata* allotree, char** buffer, PFile *fp)
{
  int i, offset, num_atoms, num_hmms = allotree->num_hmms, ptr;
  HMMInfo* hmm_infos;
  hmm_infos = (HMMInfo*) * buffer;
  num_atoms = sizeof(HMMInfo) * num_hmms / BYTES_PER_ATOM;
  (*buffer) += num_atoms;
  for (i = 0; i < num_hmms; i++)
  {
    pfread(&hmm_infos[i].name[0], sizeof(char), MAX_PHONEME_NAME_LEN, fp);
    pfread(&(hmm_infos[i].num_states), sizeof(asr_int16_t), 1, fp);
    pfread(&ptr, sizeof(asr_int16_t), 1, fp);
    pfread(&(hmm_infos[i].state_indices), sizeof(asr_int16_t*), 1, fp);
  }

  /* through this and comments below, I was trying to keep the state_indices
     self-contained, to calculate offsets from saved pointers, but it doesn't
     appear to work;  so we resort to recovering state offsets from num_states
     state_indices = hmm_infos[0].state_indices; */
  pfread(*buffer, sizeof(asr_int16_t), allotree->num_states, fp);

  hmm_infos[0].state_indices = (asr_int16_t*) * buffer;
  num_atoms = sizeof(hmm_infos[0].state_indices[0]) * allotree->num_states / BYTES_PER_ATOM;
  (*buffer) += num_atoms;

  for (i = 0, offset = 0; i < num_hmms; i++)
  {
    /* int j,offset2 = hmm_infos[i].state_indices - state_indices; */
    hmm_infos[i].state_indices = hmm_infos[0].state_indices + offset;
    if (i >= HMM_COUNTER_OFFSET + NUM_SILENCE_HMMS - 1)
      offset += hmm_infos[i].num_states;
    /* printf("offset %d %d offset2 %d\n", i, offset, offset2);
       printf("hmm %d %x states", i, hmm_infos[i].state_indices);
       for(j=0; j<hmm_infos[i].num_states; j++)
       printf(" %d", hmm_infos[i].state_indices[j]);
       printf("\n"); */

  }
  allotree->hmm_infos = hmm_infos;
}

/*-----------------------------------------------------------------------*
 *                                                                       *
 * top level stream functions                                            *
 *                                                                       *
 *-----------------------------------------------------------------------*/

int read_arbdata_from_stream(srec_arbdata** pallotree, char* filename, int buffer_size)
{
  char* pbuf;
  srec_arbdata* allotree;
  int ptr;

  PFile* fp;
  long fpos;
  char* buffer;

  fp = file_must_open(NULL, (char*)filename, L("rb"), ESR_TRUE);
  if(!fp) {
    *pallotree = NULL;
    return 0;
  }
  pfseek(fp, 0, SEEK_END);
  fpos = pftell(fp);
  buffer = (char*)CALLOC_CLR(fpos, sizeof(char), "srec.arbdata");
  pfseek(fp, 0, SEEK_SET);

  buffer_size = fpos;
  pbuf = buffer;

  allotree = (srec_arbdata*)buffer;
  /* ASSERT(allotree->image_size == buffer_size); hack for now */

  /* read structure arbdata from file */
  pfread(&allotree->image, sizeof(char *), 1, fp);             /* image */
  pfread(&allotree->image_size, sizeof(asr_uint16_t), 1, fp);       /* image_szie */
  pfread(&allotree->num_phonemes, sizeof(asr_int16_t), 1, fp);      /* num_phonemes */
  pfread(&allotree->pdata, sizeof(phoneme_data *), 1, fp);     /* pdate */
  pfread(&allotree->num_questions, sizeof(asr_int16_t), 1, fp);     /* num_questions */

  pfread(&ptr, sizeof(asr_int16_t), 1, fp);     /* alignment problem */

  pfread(&allotree->questions, sizeof(srec_question *), 1, fp);/* questions */
  pfread(&allotree->num_states, sizeof(asr_int16_t), 1, fp);        /* num_states */
  pfread(&allotree->num_hmms, sizeof(asr_int16_t), 1, fp);          /* num_hmms */
  pfread(&allotree->hmm_infos, sizeof(HMMInfo *), 1, fp);      /* hmm_infos */
  pfread(allotree->phoneme_index, sizeof(asr_uint16_t), NUM_PHONEME_INDICES, fp); /* phoneme_index */

  allotree->image = buffer;

  pbuf += sizeof(*allotree) / BYTES_PER_ATOM;
  pbuf -= sizeof(void*); // PCPinfo

  ASSERT(pftell(fp) == pbuf - buffer);

#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("read allotree done %d\n", (int)(pbuf - buffer));
#endif

  allotree->questions = (srec_question *)pbuf;
  read_questions(&allotree->questions, allotree->num_questions, &pbuf, fp);
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("read_questions done %d\n", (int)(pbuf - buffer));
#endif
  ASSERT(pftell(fp) == pbuf - buffer);

  /* readme phoneme_data */
  read_phoneme_data(&allotree->pdata, allotree->num_phonemes, &pbuf, fp);
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("read_phoneme_data done %d\n", (int)(pbuf - buffer));
#endif
  ASSERT(pftell(fp) == pbuf - buffer);

  read_hmminfos(allotree, &pbuf, fp);
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("read_hmminfos done %d\n", (int)(pbuf - buffer));
#endif
  ASSERT(pftell(fp) == pbuf - buffer);

  *pallotree = allotree;
#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("read arbdata image size %d\n", allotree->image_size);
#endif
  ASSERT(pbuf - buffer == buffer_size);

  pfclose(fp);

  return 0;
}

/**
 * @todo document
 */
typedef struct
{
  unsigned short nnodes;
  unsigned long  size;
  long    phoneme;
  unsigned short node_pos;
  unsigned long  node_off;
  short    low_genone_no;
  short    high_genone_no;
  short    low_pel_no;
  short    high_pel_no;
}
tree_head;

static int traverse_tree(tree_node* node, tree_head *tree_topo, int *num_terminal_nodes)
{
  if (!node) return 0; /* should not happen */
  if (!tree_topo) return 0; /* should not happen */
  if (tree_topo->nnodes > 255)
    return 0; /* should not happen, might indicate infinite looping */

  tree_topo->nnodes++;

  if (node->node.quest_index < 0)
  {
    if (num_terminal_nodes)
    {
      (*num_terminal_nodes)++;
    }
    if (node->term.pelid < tree_topo->low_pel_no)
    {
      tree_topo->low_pel_no = node->term.pelid;
      tree_topo->low_genone_no = node->term.pelid;
    }
    if (node->term.pelid > tree_topo->high_pel_no)
    {
      tree_topo->high_pel_no = node->term.pelid;
      tree_topo->high_genone_no = node->term.pelid;
    }
  }
  else
  {
    traverse_tree((tree_node*)node->node.fail, tree_topo, num_terminal_nodes);
    traverse_tree((tree_node*)node->node.pass, tree_topo, num_terminal_nodes);
  }
  return 0;

}

#if 0
static int num_nodes_in_tree(tree_node* node, int *num_terminal_nodes)
{
  tree_head topo;
  *num_terminal_nodes = 0;
  topo.nnodes = 0;
  traverse_tree(node, &topo, num_terminal_nodes);
  return topo.nnodes;
}
#endif

static unsigned int version_arbdata_add(unsigned int ics, int data)
{
  unsigned int ocs = ((ics << 3) | (ics >> 29)) + data;
  /* if(debug)printf("ocs %d ics %d data %d\n", ocs, ics, data);*/
  return ocs;
}


unsigned int version_arbdata_models(srec_arbdata* a)
{
  int i, num_hmms_in_phoneme;

  tree_head topo;
  unsigned int checksum = 0;
  /* if(debug)printf("num_hmms %d\n", a->num_hmms); */
  /* if(debug)printf("num_phonemes %d\n", a->num_phonemes); */
  for (i = 0; i < a->num_phonemes; i++)
  {
    num_hmms_in_phoneme = 0;
    topo.low_pel_no = 32567;
    topo.high_pel_no = 0;
    topo.nnodes = 0;
    traverse_tree(a->pdata[i].model_nodes, &topo, &num_hmms_in_phoneme);
    /* if(debug)printf("phoneme %d num_hmms %d (%d-%d)\n", i, num_hmms_in_phoneme,
    topo.low_pel_no, topo.high_pel_no); */
    if (topo.nnodes == 256) return 0;
    checksum = version_arbdata_add(checksum, topo.low_pel_no);
  }
  return checksum;
}




