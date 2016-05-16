/*---------------------------------------------------------------------------*
 *  test_swiarb.c                                                            *
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


#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "plog.h"
#include "passert.h"
#include "duk_args.h"
#include "duk_err.h"
#include "ptrd.h"

#include "srec_arb.h"
#include "simapi.h"

#include "PFileSystem.h"
#include "PANSIFileSystem.h"

#define MAX_PATH_LENGTH 256
#define MAX_LINE_LENGTH 256
#define MAX_ENTRY_LENGTH 128
#define MAX_NUM_REC_CONTEXTS 4
#define ANY_SYNTAX syntax_list[0] /* just get around internal checks */
#define NUM_WORDS_TO_ADD 500
#define MAX_INTERACTIVE_NUM 128

#define printf_vector(HEAD, FMT, PTR, NN) { unsigned int iI; printf(HEAD); for(iI=0;iI<(NN);iI++) printf(FMT, PTR[iI]); printf("\n"); }

/* #include"scg_arbdata.c" */
//static int debug = 0;


int main (int argc, char **argv)
{
	int i, j;
	int interactive_test = 0;
    CA_Arbdata             *ca_arbdata = NULL;     /* new, link btw acc/syn */
	//char *modelmap = NULL;
	char *arbfile = NULL;
	char* q;
	modelID model_sequence[128];
	char pronunciation[256];
	int pronunciation_len;
	int rc;
	srec_arbdata *allotree = NULL;

/* initial memory */
	CHKLOG(rc, PMemInit());

	if(argc<=1){
	  printf("USAGE: -swiarb <swiarb file> -interactive\n");
	  exit(1);
	}


	for(i=1; i<argc; i++) {
      if(!strcmp(argv[i],"-swiarb")) {
	if(argc==2){
	  printf("Please specify the swiarb file.\n");
	  exit(1);
	}
	arbfile = argv[++i];
	printf("using swiarb from file %s\n", arbfile);
      } else if(!strcmp(argv[i],"-interactive")) {
	interactive_test++;
      } else {
	printf("error_usage: argument [%s]\n", argv[i]);
	exit(1);
      }
    }
	
/* get modelID for a triphone */
    ca_arbdata = CA_LoadArbdata(arbfile);
    
    for(i=0; i<MAX_INTERACTIVE_NUM; i++){

      if(interactive_test){
	printf("Type \"quit\" to exit the test.\n");
	printf("pronunciation: ");
	q = fgets(pronunciation, sizeof(pronunciation), stdin);
	if(!strcmp(q,"quit\n")) break;
      }
      else{
	printf("USAGE: -swiarb <swiarb file> -interactive\n");
	exit(1);
      }

      pronunciation_len = strlen(pronunciation)-1;
      CA_ArbdataGetModelIdsForPron(ca_arbdata,
                                 pronunciation, pronunciation_len,
                                 &model_sequence[0]);


      printf("short pronunciation length is %d.\n", pronunciation_len);
      printf("Acoustic model IDs (\"#\" is silence,\"_\" is word boundary):\n");
      for (j=0;j<pronunciation_len;j++){
      
	if(j==0){
	  if(pronunciation_len==1) 
	    printf("triphone:_%c_ -> ModelID:%d\n", pronunciation[j], model_sequence[j]);
	    else
	  printf("triphone:_%c%c -> ModelID:%d\n", pronunciation[j], pronunciation[j+1],
	       model_sequence[j]);
	}
	else if(j==(pronunciation_len-1)){
	  printf("triphone:%c%c_ -> ModelID:%d\n", pronunciation[j-1], pronunciation[j], model_sequence[j]);
	}
	else{
	  printf("triphone:%c%c%c -> ModelID:%d\n", pronunciation[j-1], pronunciation[j], pronunciation[j+1],
	       model_sequence[j]);
	}
      
	allotree = (srec_arbdata*)ca_arbdata;
	printf_vector("pel_ids: ", " %d", allotree->hmm_infos[model_sequence[j]].state_indices, 
		    (unsigned int) allotree->hmm_infos[model_sequence[j]].num_states);
	printf("\n");
      
      }
    }

  CA_FreeArbdata( ca_arbdata);
	
  PMemShutdown();
  return 0;
CLEANUP:
  return 1;
}

