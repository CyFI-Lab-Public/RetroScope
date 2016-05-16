/*---------------------------------------------------------------------------*
 *  make_ve_grammar.c                                                            *
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

#define MAX_FILE_NAME_LEN 64
#define DEFAULT_WWTRIPHONE_SILMODE 3


/* check if the central phoneme is a word-specific phoneme; if so, do not enroll it into the ve grammar.*/
int ws_verify(char * allo_phoneme){
  switch(allo_phoneme[0]){
  case '(': return 1;
  case '.': return 1;
  case '0': return 1;
  case '1': return 1;
  case '2': return 1;
  case '3': return 1;
  case '4': return 1;
  case '5': return 1;
  case '7': return 1;
  case '8': return 1;
  case '9': return 1;
  case '=': return 1;
  case '>': return 1;
  case 'B': return 1;
  case 'F': return 1;
  case 'G': return 1;
  case 'H': return 1;
  case 'K': return 1;
  case 'M': return 1;
  case 'Q': return 1;
  case 'R': return 1;
  case 'W': return 1;
  case 'X': return 1;
  case 'Y': return 1;
  case '[': return 1;
  case '\\': return 1;
  case '|': return 1;
  case '+': return 1;
  default: return 0;
  }
}

int main (int argc, char **argv)
{
	int i;
	char filen[MAX_FILE_NAME_LEN]="";
	CA_Arbdata *ca_arbdata = NULL;     /* new, link btw acc/syn */
	char *arbfile = NULL;
	char *base = NULL;

	FILE* pfile;
	FILE* pFile_PCLG;
	FILE* pFile_map;
	FILE* pFile_P;
        FILE* pFile_Grev;
	FILE* pFile_script;

	int num_hmms;
	int num_wd = 0;
	int script_line = 0;
	int cflag = 0, fnode = 0;
	int sil_model = DEFAULT_WWTRIPHONE_SILMODE;
	int rc;
	srec_arbdata *allotree = NULL;
	 
	nodeID startNode       = 0;
	nodeID pauEndNode      = 1;
	nodeID modelStartNode  = 2;
	nodeID modelEndNode    = 3;
	nodeID pau2StartNode   = 4;
	nodeID pau2EndNode     = 5;
	nodeID endNode         = 6;

	/* initial memory */
	CHKLOG(rc, PMemInit());

	if(argc<5){
	  printf("USAGE: -swiarb <swiarb file> -base <output base name>\n");
	  exit(1);
	}


	for(i=1; i<argc; i++) {
	  if(!strcmp(argv[i],"-swiarb")) {
	    arbfile = argv[++i];
	    printf("using swiarb from file %s\n", arbfile);
	  }
	  else if(!strcmp(argv[i],"-base")){
	    base = argv[++i];
	  }
	  else {
	    printf("error_usage: argument [%s]\n", argv[i]);
	    exit(1);
	  }
	}
    	
	/* check arb file exist*/
	if ( (pfile = fopen(arbfile, "r")) != NULL ){
	    fclose(pfile);
	}
	else{
	  printf("ERROR: the specified swiarb file does not exist.\n");
	  exit(1);
	}

	
	ca_arbdata = CA_LoadArbdata(arbfile);
  
	allotree = (srec_arbdata*)ca_arbdata;
	num_hmms = allotree->num_hmms;


	/* Dump out VE .PCLG.txt, .Grev2.det.txt, .P.txt, .script and .map files; .P.txt, .script and .map are not necessary for voice enroll, so just dump out to create .g2g file. Xufang */

	printf("Dumping out VE files\n");

	strcat(filen,base);
	strcat(filen,".PCLG.txt");
	pFile_PCLG = fopen(filen,"w");

	filen[0]='\0';
	strcat(filen,base);
	strcat(filen,".map");
        pFile_map = fopen(filen,"w");

        filen[0]='\0';
        strcat(filen,base);
        strcat(filen,".P.txt");
        pFile_P = fopen(filen,"w");

        filen[0]='\0';
        strcat(filen,base);
        strcat(filen,".Grev2.det.txt");
        pFile_Grev = fopen(filen,"w");

        filen[0]='\0';
        strcat(filen,base);
        strcat(filen,".script");
        pFile_script = fopen(filen,"w");

        fprintf(pFile_Grev,"0\t1\teps\t80\n");
        fprintf(pFile_Grev,"1\t2\t%s.grxml@VE_Words\n",base);

	fprintf(pFile_map,"eps %d\n",num_wd++);
        fprintf(pFile_map,"%s.grxml@ROOT %d\n",base,num_wd++);
        fprintf(pFile_map,"%s.grxml@VE_Words %d\n",base,num_wd++);
        fprintf(pFile_map,"-pau- %d\n",num_wd++);
        fprintf(pFile_map,"-pau2- %d\n",num_wd++);
        fprintf(pFile_map,"@VE_Words %d\n",num_wd++);

        fprintf(pFile_P,"0\t1\teps\t{\t\n");
        fprintf(pFile_P,"1\t2\teps\t{\t\n");
        fprintf(pFile_P,"2\t3\teps\t{\t\n");
        fprintf(pFile_P,"2\t4\teps\t{\t\n");
        fprintf(pFile_P,"3\t5\t%s.grxml@VE_Words\t%s.grxml@VE_Words\t\n",base,base);
        fprintf(pFile_P,"4\t8\teps\t{\t\n");
        fprintf(pFile_P,"5\t6\teps\t_3\t\n");
        fprintf(pFile_P,"6\t7\teps\tVE_Words}\t\n");
        fprintf(pFile_P,"7\t9\teps\t_2\t\n");

        fprintf(pFile_script,"%d type=SENT.type;meaning=SENT.V;\n",script_line++);
        fprintf(pFile_script,"%d type='NEW';V=UTT.V;\n",script_line++);
        fprintf(pFile_script,"%d type='OLD';V=VE_Words.V;\n",script_line++);
	fprintf(pFile_script,"%d V=UTT.V?UTT.V:'--';\n",script_line++);
        fprintf(pFile_script,"%d V=PHONEME.V\n",script_line++);

	for(i=0;i<num_hmms;i++){
	  if(ws_verify(allotree->hmm_infos[i].name))
	    continue;
	  if(!strcmp(allotree->hmm_infos[i].name,"#")){
	    sil_model = i;
	    fprintf(pFile_PCLG,"%d\t%d\thmm%d_#sil#\t-pau-\n", startNode, pauEndNode, i);
            fprintf(pFile_PCLG,"%d\t%d\t.wb\teps\n", pauEndNode, modelStartNode);
          }
          else{
            if(strlen(allotree->hmm_infos[i].name)>0){
	      if(cflag==0){
		fnode = i;
		cflag = 1;
	      }
              fprintf(pFile_PCLG,"%d\t%d\thmm%d_%s\twd_hmm%d_%s\t40\n", modelStartNode, modelEndNode, 
		      i,allotree->hmm_infos[i].name,i,allotree->hmm_infos[i].name);
	      fprintf(pFile_map,"wd_hmm%d_%s %d\n",i,allotree->hmm_infos[i].name,num_wd++);
	      fprintf(pFile_Grev,"1\t3\twd_hmm%d_%s\n",i,allotree->hmm_infos[i].name);
	      fprintf(pFile_P,"8\t10\twd_hmm%d_%s\t_%d\t\n",i,allotree->hmm_infos[i].name,script_line);
	      fprintf(pFile_script,"%d V=V?V:'';V=V+'wd_hmm%d_%s';\n",script_line++,i,allotree->hmm_infos[i].name);
	    }
          }
	}

        fprintf(pFile_PCLG,"%d\t%d\t.wb\teps\n", modelEndNode, modelStartNode);
        fprintf(pFile_PCLG,"%d\t%d\t.wb\teps\n", modelEndNode, pau2StartNode);
        fprintf(pFile_PCLG,"%d\t%d\thmm%d_#sil#\t-pau2-\n",pau2StartNode, pau2EndNode, sil_model);
        fprintf(pFile_PCLG,"%d\t%d\t.wb\teps\n", pau2EndNode, endNode);
        fprintf(pFile_PCLG,"%d\n", endNode);

        fprintf(pFile_Grev,"2\n");
	for(i=fnode;i<num_hmms;i++){
          if(ws_verify(allotree->hmm_infos[i].name))
            continue;	  
	  fprintf(pFile_Grev,"3\t3\twd_hmm%d_%s\t40\n",i,allotree->hmm_infos[i].name);
	}
        fprintf(pFile_Grev,"3\n");

        fprintf(pFile_P,"9\t11\teps\tSENT}\t\n");
        fprintf(pFile_P,"10\t12\teps\tPHONEME}\t\n");
        fprintf(pFile_P,"11\t13\teps\t_0\t\n");
        fprintf(pFile_P,"12\t14\teps\t_4\t\n");
        fprintf(pFile_P,"13\t15\teps\tROOT}\t\n");
        fprintf(pFile_P,"14\t16\teps\teps\t\n");
        fprintf(pFile_P,"15\t\n");
        fprintf(pFile_P,"16\t17\teps\tUTT}\t\n");
        fprintf(pFile_P,"16\t8\teps\t{\t\n");
        fprintf(pFile_P,"17\t9\teps\t_1\t\n");

	fclose(pFile_PCLG);
	printf("Creating %s.PCLG.txt...\n",base);
        fclose(pFile_Grev);
        printf("Creating %s.Grev2.det.txt...\n",base);
        fclose(pFile_map);
	printf("Creating %s.map...\n",base);
        fclose(pFile_P);
	printf("Creating %s.P.txt...\n",base);
	fclose(pFile_script);
	printf("Creating %s.script...\n",base);
	printf("SUCCESS!\n");


  CA_FreeArbdata( ca_arbdata);
	
  PMemShutdown();
  return 0;
CLEANUP:
  return 1;
}

