/*---------------------------------------------------------------------------*
 *  test_g2g.c  *
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
#include "pmemory.h"
#include "plog.h"
#include "HashMap.h"
#include "SR_Grammar.h"
#include "SR_Vocabulary.h"
#include "SR_SemanticResult.h"
#include "ESR_Session.h"
#include "ESR_Locale.h"
#include "ESR_CommandLine.h"
#include "LCHAR.h"

#include "PFileSystem.h"
#include "PANSIFileSystem.h"

#include "SR_GrammarImpl.h"

#include "simapi.h"
#include "srec_context.h"
#include "srec_arb.h"

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


int usage(LCHAR* exename)
{
  pfprintf(PSTDOUT,"usage: %s -base <basefilename> \n",exename);
  pfprintf(PSTDOUT,"<basefilename> can be a file.g2g or @g2gfilelist\n");
  pfprintf(PSTDOUT,"[-checkword id] .. also checks word id in the file\n");
  pfprintf(PSTDOUT,"[-swiarb esr/config/lang/models/generic.swiarb] ... enables word check\n");
  return 1;
}

/* protos */
ESR_ReturnCode find_phonemes_for_ihmms( CA_Arbdata* ca_arbdata, modelID* ihmms, int num_hmms);
ESR_ReturnCode Parse(SR_Grammar* grammar, LCHAR* trans, PFile* fout);
int CheckG2G(CA_Arbdata* arbdata, int* p4pTable, const char* base, int wordid, char* outbase);
void load_filelist(char* filelist, char*** pfiles, int *pnum_files);
int *phonemecode_for_pel_table(CA_Arbdata* arbdata);

int debug = 0;
#define MAX_LINE_LENGTH 256
#define MAX_STR_LENGTH   80
#define MAX_SEM_RESULTS   3
#define MAX_KEYS         30

/* main */

int main (int argc, char **argv)
{
  ESR_ReturnCode rc;
  LCHAR base[P_PATH_MAX] = L("");
  int i;
  CA_Arbdata* ca_arbdata;
  char*  arbfile = NULL;
  char** g2glist;
  int g2glist_len;
  char* outbase = NULL;
  int *p4pTable;
  int wordid = 0;
  int log_level = 0;

  /*
   * Initialize portable library.
   */
  CHKLOG(rc, PMemInit());
/*  CHKLOG(rc, PFileSystemCreate());
  CHKLOG(rc, PANSIFileSystemCreate());
  CHKLOG(rc, PANSIFileSystemAddPath(L("/dev/ansi"), L("/")));*/
  
  /* Set ANSI file-system as default file-system */
/*  CHKLOG(rc, PANSIFileSystemSetDefault(ESR_TRUE));*/
  /* Set virtual current working directory to native current working directory */
/*  len = P_PATH_MAX;
  CHKLOG(rc, PANSIFileSystemGetcwd(cwd, &len));
  CHKLOG(rc, PFileSystemChdir(cwd));*/
  
  if( argc <= 1)
	{
    usage(argv[0]);
    exit(EXIT_FAILURE);
  }

	for (i = 1; i < argc; ++i)
	{
		if(!LSTRCMP(argv[i], L("-base")))
		{
			++i;
			LSTRCPY(base, argv[i]);
		}
		else if(!LSTRCMP(argv[i],L("-out")))
		{
			outbase = argv[++i];
		}
		else if(!LSTRCMP(argv[i],L("-swiarb")))
		{
			arbfile = argv[++i];
		}
		else if(!LSTRCMP(argv[i],L("-checkword")))
		{
			wordid = atoi(argv[++i]);
		}
		else if(!LSTRCMP(argv[i],L("-log")))
		{
			log_level = 10;
		}
		else
		{
			printf("unrecog'd argument %s\n", argv[i]);
			exit(1);
		}
	}

	CHK(rc, PLogInit(NULL, log_level));

	if(arbfile) {
		ca_arbdata = CA_LoadArbdata(arbfile); 
		if(!ca_arbdata) {
      pfprintf(PSTDOUT, "Error: loading arbfile %s\n", arbfile);
      goto CLEANUP;
    }
    pfprintf(PSTDOUT, "arbdata done\n");
    p4pTable  = phonemecode_for_pel_table(ca_arbdata);
    pfprintf(PSTDOUT, "p4pTable done\n");
  } else {
    ca_arbdata = 0;
    p4pTable = 0;
  }
    
  if(base[0] == '@') {
    load_filelist(base+1, &g2glist, &g2glist_len);
    pfprintf(PSTDOUT, "g2glist %s .. %d entries\n", g2glist_len);
    for(i=0; i<g2glist_len; i++) 
      CheckG2G( ca_arbdata, p4pTable, g2glist[i], wordid, outbase);
  }
  else {
    CheckG2G( ca_arbdata, p4pTable, base, wordid, outbase);
  }

CLEANUP:
  PLogShutdown();
/*  PANSIFileSystemDestroy();
  PFileSystemDestroy();*/
  PMemSetLogFile(PSTDOUT);
  PMemDumpLogFile();
  PMemShutdown();
  return rc;
}

int CheckG2G(CA_Arbdata* ca_arbdata, int* p4pTable, const char* base, int wordid, char* outbase)
{
  ESR_ReturnCode rc;
  SR_GrammarImpl *grammarImpl;
  SR_Grammar* grammar = NULL;
  srec_context* fst;
  CA_Syntax* syntax;
  modelID ilabels_preceding[64], num_ilabels_preceding;
  modelID ilabels_following[64], num_ilabels_following;
  modelID ilabels[128], num_ilabels;
  int i,j;
  unsigned long g2gsize;

  if(1) {
    FILE* fp;
    fp = fopen(base, "rb");
    if(!fp) g2gsize = 0;
    else {
      fseek(fp, 0, SEEK_END);
      g2gsize = ftell(fp);
      fclose(fp);
    }
  }

  rc = SR_GrammarLoad(base, &grammar);
  if(rc != ESR_SUCCESS) {
    pfprintf(PSTDOUT, "%s failed at load\n", base);
    goto CLEANUP;
  }
  
  grammarImpl = (SR_GrammarImpl*)grammar;
  syntax = grammarImpl->syntax;
  if(outbase) {
    CA_DumpSyntax( syntax, outbase);
  }

  fst = syntax->synx;
  pfprintf(PSTDOUT, "%s %d arcs %d/%d/%d nodes %d/%d/%d words %d/%d chars %d/%d modelver %d\n",
	   base, g2gsize, 
		 fst->num_arcs, fst->num_base_arcs, fst->FSMarc_list_len,
		 fst->num_nodes, fst->num_base_nodes, fst->FSMnode_list_len,
	   fst->olabels->num_words, fst->olabels->max_words,
	   fst->olabels->next_chars-fst->olabels->chars, 
	   fst->olabels->max_chars,
#ifdef IMAGE_FORMAT_V2   
	   fst->modelid
#else
	   -1
#endif
	   );

  if(wordid == 0 || ca_arbdata == 0) 
    goto CLEANUP;

  if(wordid >= fst->olabels->num_words) {
    pfprintf(PSTDOUT, "%s failed 'cuz numwords(%d) < %d\n", base, 
	     fst->olabels->num_words, wordid);
    goto CLEANUP;
  }

  for(i=0; i<fst->num_arcs; i++) {
    if(fst->FSMarc_list[i].olabel == wordid) {
      FSMnode* node;
      FSMarc* arc = &fst->FSMarc_list[i];
      nodeID fr_node = arc->fr_node;
      arcID iarc;
      ilabels_following[0] = arc->ilabel;
      num_ilabels_following = 1;
      num_ilabels_preceding = 0;
      for( ; fr_node!=fst->start_node; fr_node=arc->fr_node) {
	node = &fst->FSMnode_list[fr_node];
	iarc = node->first_prev_arc;
	for( ; iarc!=MAXarcID; iarc=arc->linkl_prev_arc) {
	  arc = &fst->FSMarc_list[iarc];
	  if(arc->fr_node != fr_node) break;
	}
	if(iarc == MAXarcID) {
	  pfprintf(PSTDOUT, "%s failed at 11\n", base);
	  goto CLEANUP;
	}
	if(arc->ilabel == WORD_BOUNDARY) break;
	ilabels_preceding[num_ilabels_preceding++] = arc->ilabel;
      }
      arc = &fst->FSMarc_list[i];
      fr_node = arc->to_node;
      for( ; fr_node!=fst->end_node; fr_node=arc->to_node) {
	node = &fst->FSMnode_list[fr_node];
	iarc = node->un_ptr.first_next_arc;
	for( ; iarc!=MAXarcID; iarc=arc->linkl_next_arc) {
	  arc = &fst->FSMarc_list[iarc];
	  if(arc->to_node != fr_node) break;
	}
	if(iarc == MAXarcID) {
	  pfprintf(PSTDOUT, "%s failed at 12\n", base);
	  goto CLEANUP;
	}
	ilabels_following[num_ilabels_following++] = arc->ilabel;
	if(arc->ilabel == WORD_BOUNDARY) break;
      }
      num_ilabels = 0;
      for(j=0; j<num_ilabels_preceding; j++) 
	ilabels[num_ilabels++] = ilabels_preceding[num_ilabels_preceding-1-j];
      for(j=0; j<num_ilabels_following; j++) 
	ilabels[num_ilabels++] = ilabels_following[j];
      if(ilabels[num_ilabels-1] == WORD_BOUNDARY) 
	num_ilabels--;
      for(j=0; j<num_ilabels; j++) {
	if(ilabels[j]<fst->hmm_ilabel_offset) {
	  pfprintf(PSTDOUT, "%s failed at 15\n", base);
	  goto CLEANUP;
	} else 
	  ilabels[j] = ilabels[j] - (labelID)fst->hmm_ilabel_offset;
      }
      pfprintf(PSTDOUT, "%s (W%d) ihmms ", fst->olabels->words[wordid], wordid);
      for(j=0;j<num_ilabels;j++) 
	pfprintf(PSTDOUT, " %d", ilabels[j]);
      pfprintf(PSTDOUT, "\n");
      if(num_ilabels < 2) {
	pfprintf(PSTDOUT, "%s failed at 1\n", base);
	goto CLEANUP;
      }
      if(p4pTable) 
	rc = find_phonemes_for_ihmms( ca_arbdata, ilabels, num_ilabels);
      else {
	rc = ESR_SUCCESS;
	for(j=0; j<num_ilabels; j++) {
	  if(p4pTable[ ilabels[j]]<0) {
	    rc = ESR_NO_MATCH_ERROR;
	    ilabels[j] = MAXmodelID;
	  } else {
	    ilabels[j] = (modelID)p4pTable[ ilabels[j]];
	  }
	}
      }
	
      if(rc) {
	pfprintf(PSTDOUT, "%s failed at 2\n", base);
	goto CLEANUP;
      }
      pfprintf(PSTDOUT, "%s ", fst->olabels->words[wordid]);
      for(j=0;j<num_ilabels;j++) pfprintf(PSTDOUT, "%c", ilabels[j]);
      pfprintf(PSTDOUT, "\n");
      rc = Parse( grammar, fst->olabels->words[wordid], PSTDOUT);
      if(rc) {
	pfprintf(PSTDOUT, "%s failed at 3\n", base);
	goto CLEANUP;
      }
      pfprintf(PSTDOUT, "%s PASSED (on %s)\n", base, fst->olabels->words[wordid]);
      break;
    }
  }

  return 0;
 CLEANUP:
  if(grammar) SR_GrammarDestroy(grammar);
  return 1;

}


int traverse_tree(tree_node* node, tree_head *tree_topo, int *num_terminal_nodes)
{
  if(node) 
    tree_topo->nnodes++;
  
  if(node->node.quest_index < 0) {
    if(num_terminal_nodes) 
      (*num_terminal_nodes)++;
    if( node->term.pelid < tree_topo->low_pel_no) 
      tree_topo->low_pel_no = tree_topo->low_genone_no = node->term.pelid;
    if( node->term.pelid > tree_topo->high_pel_no) 
      tree_topo->high_pel_no = tree_topo->high_genone_no = node->term.pelid;
  } else {
    traverse_tree( (tree_node*)node->node.fail, tree_topo, num_terminal_nodes);
    traverse_tree( (tree_node*)node->node.pass, tree_topo, num_terminal_nodes);
  }
  return 0;
  
}

int num_nodes_in_tree(tree_node* node, int *num_terminal_nodes)
{
  tree_head topo;
  *num_terminal_nodes = 0;
  topo.nnodes = 0;
  traverse_tree(node, &topo, num_terminal_nodes);
  return topo.nnodes;
}

ESR_ReturnCode find_phonemes_for_ihmms( CA_Arbdata* ca_arbdata, modelID* ihmms, int num_ihmms)
{
  int ii, i;
  int num_hmms_in_phoneme;
  tree_head topo;
  srec_arbdata* a = (srec_arbdata*)ca_arbdata;
  int num_phonemes_for_ihmms = 0;

  for(ii=0; ii<num_ihmms; ii++) {
    for(i=0; i<a->num_phonemes; i++) {
      num_hmms_in_phoneme = 0;
      topo.low_pel_no  = 32567;
      topo.high_pel_no = 0;
      traverse_tree(a->pdata[i].model_nodes, &topo, &num_hmms_in_phoneme);
      if(debug)printf("phoneme %d num_hmms %d (%d-%d)\n", i, num_hmms_in_phoneme,
		      topo.low_pel_no, topo.high_pel_no);
      if(ihmms[ii] >= topo.low_pel_no && ihmms[ii]<= topo.high_pel_no) {
	ihmms[ii] = (modelID)i;
	num_phonemes_for_ihmms++;
	break;
      }
    }
    if( i==a->num_phonemes) {
      if(ihmms[ii]<=5) {
	ihmms[ii] = 0;
	num_phonemes_for_ihmms++;
      } else {
	PLogError("error: could not find hmm%d under any phoneme! ",ihmms[ii]);
      }
    }

  }
  if(num_phonemes_for_ihmms != num_ihmms) 
    return ESR_INVALID_ARGUMENT;
  else {
    for(ii=0; ii<num_ihmms; ii++) ihmms[ii] =  a->pdata[ ihmms[ii]].code;
    return ESR_SUCCESS;
  }
}

void display_results(SR_SemanticResult *result, PFile* fout)
{
  size_t i, size, len;
  LCHAR* keys[MAX_KEYS]; /* array of pointers to strings */
  LCHAR  value[MAX_STR_LENGTH];
  ESR_ReturnCode rc;

  size = MAX_KEYS;
  rc = result->getKeyList(result, (LCHAR**) &keys, &size); /* get the key list */
  if(rc == ESR_SUCCESS)
  {
    for(i=0; i<size; i++)
    {
      len = MAX_STR_LENGTH;
      if ((rc = result->getValue(result,keys[i],value,&len)) == ESR_SUCCESS)
        pfprintf(fout,"{%s : %s}\n",keys[i],value);
      else
        pfprintf(fout,"Error: %s\n",ESR_rc2str(rc));
    }
  }
  else
    pfprintf(fout,"Error: %s\n",ESR_rc2str(rc));
}

ESR_ReturnCode Parse(SR_Grammar* grammar, LCHAR* trans, PFile* fout)
{
  ESR_ReturnCode rc;
  int i, result_count;
  SR_SemanticResult* semanticResults[MAX_SEM_RESULTS];

  result_count = MAX_SEM_RESULTS; /* initially not greater than MAX */
  for(i =0; i<result_count; i++)
    SR_SemanticResultCreate(&semanticResults[i]); /* create the result holders */

  lstrtrim(trans);

  rc = grammar->checkParse(grammar, trans, semanticResults, (size_t*) &result_count);
  if(rc != ESR_SUCCESS)
    return rc;

  if(result_count < 1)
  {
    pfprintf(fout,"no parse\n\n");
    return ESR_NO_MATCH_ERROR;
  }
  else
  {
    pfprintf(fout,"parse ok (%d results)\n", result_count);
    for(i=0; i < result_count; i++)
      display_results(semanticResults[i],fout);

    for(i=0; i < MAX_SEM_RESULTS; i++)
    {
      rc = semanticResults[i]->destroy(semanticResults[i]);
      if(rc != ESR_SUCCESS)
        return rc;
    }
    return ESR_SUCCESS;
  }
}

void load_filelist(char* filelist, char*** pfiles, int *pnum_files)
{
  int i = 0;
  FILE* fp;
  char line[512];
  char **files = 0, *file;
  int num_files = 0;

  fp = fopen(filelist, "r");
  if(!fp) {
    pfprintf(PSTDOUT, "failed to open %s\n", filelist);
    goto DONE;
  }

  while( fgets(line, sizeof(line), fp)) {
    if(line[0] == '#') continue;
    i++;
  }
  fclose(fp);

  num_files = i;
  *files = CALLOC( num_files, sizeof(char*), __FILE__);
  fp = fopen(filelist, "r");
  for(i=0; fgets(line,sizeof(line),fp) && i<num_files; i++) {
    if(line[0] == '#') continue;
    strtok(line,"\n\r\t");
    file = files[i++] = CALLOC(strlen(line)+1,sizeof(char),__FILE__);
    strcpy( file, line);
  }
  fclose(fp);
  num_files = i;
  
 DONE:
  *pfiles = files;
  *pnum_files = num_files;
}

int* phonemecode_for_pel_table(CA_Arbdata* ca_arbdata)
{
  static int table[2048];
  int i,j;
  tree_head topo;
  srec_arbdata* a = (srec_arbdata*)ca_arbdata;
  int num_hmms_in_phoneme;
  
  for(j=0; j< (int)(sizeof(table)/sizeof(int)); j++)
    table[j] = 0;

  for(i=0; i<a->num_phonemes; i++) {
    num_hmms_in_phoneme = 0;
    topo.low_pel_no  = 32567;
    topo.high_pel_no = 0;
    traverse_tree(a->pdata[i].model_nodes, &topo, &num_hmms_in_phoneme);
    if(debug)printf("phoneme %d num_hmms %d (%d-%d)\n", i, num_hmms_in_phoneme,
		    topo.low_pel_no, topo.high_pel_no);
    
    for(j=topo.low_pel_no; j<=topo.high_pel_no; j++) 
      table[j] = a->pdata[i].code;
  }
  return &table[0];
}
