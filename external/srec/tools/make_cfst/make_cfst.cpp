/*---------------------------------------------------------------------------*
 *  make_cfst.cpp                                                            *
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

#include "ptypes.h"
#include "srec_arb.h"
#include "simapi.h"

#include "fst/lib/fstlib.h"
#include "fst/lib/fst-decl.h"
#include "fst/lib/vector-fst.h"
#include "fst/lib/arcsort.h"
#include "fst/lib/invert.h"
#include "fst/lib/rmepsilon.h"

#define MAX_LINE_LENGTH     256
#define MAX_PRONS_LENGTH 1024
#define EPSILON_LABEL 0
#define MAX_MODELS 1024
#define MAXPHID 8888
#define MAX_PHONEMES 128

using namespace fst;

int usage(const char* prog)
{
  printf("usage: %s  -phones models/phones.map -models models/models128x.map -cfst models/generic.C -swiarb models/generic.swiarb\n", prog);
  return 1;
}

typedef struct Minifst_t
{
  char lcontexts[MAX_PHONEMES];
  char rcontexts[MAX_PHONEMES];
  int modelId;
  int stateSt;
  int stateEn;
  phonemeID phonemeId;
  unsigned char phonemeCode;
  int lcontext_state[MAX_PHONEMES];
  int rcontext_state[MAX_PHONEMES];
} Minifst;

int main(int argc, char **argv)
{
  char* cfstFilename = 0;
  char* swiarbFilename = 0;
  char* phonesMap;
  char* modelsMap;
  int i;
  phonemeID lphonId, cphonId, rphonId;
  unsigned char cphon;
  modelID modelId, max_modelId = 0;
  int stateSt, stateEn;
  int stateN, stateNp1;
  int rc;
  Minifst minifst[MAX_MODELS];
  int do_show_text = 1;
  int do_until_step = 99;

  /* initial memory */
  rc = PMemInit();
  ASSERT( rc == ESR_SUCCESS);

  // A vector FST is a general mutable FST
  fst::StdVectorFst myCfst;
  // fst::Fst<fst::StdArc> myCfst;

  if(argc <= 1) 
    return usage(argv[0]);

  for(i=1; i<argc; i++)
  {
    if(0) ;
    else if(!strcmp(argv[i],"-phones"))
      phonesMap = argv[++i];
    else if(!strcmp(argv[i],"-models"))
      modelsMap = argv[++i];
    else if(!strcmp(argv[i],"-cfst"))
      cfstFilename = argv[++i];
    else if(!strcmp(argv[i],"-step"))
      do_until_step = atoi(argv[++i]);
    else if(!strcmp(argv[i],"-swiarb"))
      swiarbFilename = argv[++i];
    else {
      return usage(argv[0]);
    }
  }

  printf("loading %s ...\n", swiarbFilename);
  CA_Arbdata* ca_arbdata = CA_LoadArbdata(swiarbFilename);
  srec_arbdata *allotree = (srec_arbdata*)ca_arbdata;


  /*-------------------------------------------------------------------------
   *
   *       /---<---<---<---<---<---<---\
   *      /                             \
   *     /       -wb--         -wb-      \
   *    /        \   /         \  /       \
   *   0 ---#--->  n ----M---> n+1 ---#---> 1
   *
   *
   *
   *
   *-------------------------------------------------------------------------
   */

  // Adds state 0 to the initially empty FST and make it the start state.
  stateSt = myCfst.AddState();   // 1st state will be state 0 (returned by AddState)
  stateEn = myCfst.AddState();
  myCfst.SetStart(stateSt);  // arg is state ID
  myCfst.SetFinal(stateEn, 0.0);  // 1st arg is state ID, 2nd arg weight
  myCfst.AddArc(stateEn, fst::StdArc(EPSILON_LABEL, EPSILON_LABEL, 0.0, stateSt));

  phonemeID silencePhonId = 0;
  modelID silenceModelId = 0;
  silenceModelId = (modelID)get_modelid_for_pic(allotree, silencePhonId, silencePhonId, silencePhonId);
  // silenceModelId += MODEL_LABEL_OFFSET; #define MODEL_LABEL_OFFSET 128 

  for(modelId=0; modelId<MAX_MODELS; modelId++) {
    minifst[modelId].modelId = MAXmodelID;
    minifst[modelId].stateSt = minifst[modelId].stateEn = 0;
    minifst[modelId].phonemeId = MAXphonemeID;
    minifst[modelId].phonemeCode = 0;
    for(i=0;i<MAX_PHONEMES;i++) {
      minifst[modelId].lcontexts[i] = minifst[modelId].rcontexts[i] = 0;
      minifst[modelId].lcontext_state[i] = minifst[modelId].rcontext_state[i] = 0;
    }
  }

  for(cphonId=0; cphonId<allotree->num_phonemes && cphonId<MAXPHID; cphonId++) {
    cphon = allotree->pdata[cphonId].code;
    printf("processing phoneme %d of %d %d %c\n", cphonId, allotree->num_phonemes, cphon, cphon);
    
    for(lphonId=0; lphonId<allotree->num_phonemes && lphonId<MAXPHID; lphonId++) {
      unsigned char lphon = allotree->pdata[lphonId].code;
      for(rphonId=0; rphonId<allotree->num_phonemes && rphonId<MAXPHID; rphonId++) {
	unsigned char rphon = allotree->pdata[rphonId].code;
	if( 1|| cphon=='a') { //22222
	  modelId = (modelID)get_modelid_for_pic(allotree, lphonId, cphonId, rphonId);
	} else {
	  modelId = (modelID)get_modelid_for_pic(allotree, 0, cphonId, 0);
	}
	if(modelId == MAXmodelID) {
	  printf("error while get_modelid_for_pic( %p, %d, %d, %d)\n",
		 allotree, lphonId, cphonId, rphonId);
	  continue;
	} else 
	  if(do_show_text) printf("%c %c %c hmm%03d_%c %d %d %d\n", lphon, cphon, rphon, modelId, cphon, lphonId, cphonId, rphonId);
	ASSERT(modelId < MAX_MODELS);
	minifst[ modelId].phonemeId = cphonId;
	minifst[ modelId].phonemeCode = cphon;
	minifst[ modelId].modelId = modelId;
	minifst[ modelId].lcontexts[lphonId] = 1;
	minifst[ modelId].rcontexts[rphonId] = 1;
	if(modelId>max_modelId) max_modelId = modelId;
      }
    }
  }

  printf("adding model arcs .. max_modelId %d\n",max_modelId);
  for(modelId=0; modelId<=max_modelId; modelId++) {
    if( minifst[modelId].modelId == MAXmodelID) continue;
    cphon = minifst[modelId].phonemeCode;
    minifst[modelId].stateSt = (stateN = myCfst.AddState());
    minifst[modelId].stateEn = (stateNp1 = myCfst.AddState()); /* n plus 1 */
    myCfst.AddArc( stateN, fst::StdArc(cphon,modelId,0.0,stateNp1));
    myCfst.AddArc( stateNp1, fst::StdArc(WORD_BOUNDARY,WORD_BOUNDARY,0.0,stateNp1));

    if(do_show_text) printf("%d\t\%d\t%c\t\%d\n", stateN,stateNp1,cphon,modelId);
#if 1
    for( lphonId=0; lphonId<allotree->num_phonemes; lphonId++) {
      minifst[modelId].lcontext_state[lphonId] = myCfst.AddState();
      myCfst.AddArc( minifst[modelId].lcontext_state[lphonId],
		  fst::StdArc(EPSILON_LABEL,EPSILON_LABEL,0.0,
			      minifst[modelId].stateSt));
			      
    }
    for( rphonId=0; rphonId<allotree->num_phonemes; rphonId++) {
      minifst[modelId].rcontext_state[rphonId] = myCfst.AddState();
      myCfst.AddArc( minifst[modelId].stateEn, 
		  fst::StdArc(EPSILON_LABEL,EPSILON_LABEL,0.0,
			      minifst[modelId].rcontext_state[rphonId]));
    }
#endif
  }
#if 1
  printf("adding cross-connections\n");
  for( modelId=0; modelId<=max_modelId; modelId++) {
    printf("processing model %d\n", modelId);
    if( minifst[modelId].modelId == MAXmodelID) continue;
    cphonId = minifst[modelId].phonemeId;
    for( modelID mId=0; mId<=max_modelId; mId++) {
      if( minifst[mId].modelId != MAXmodelID &&
	  // minifst[mId].phonemeId == rphonId &&
	  minifst[modelId].rcontexts[ minifst[mId].phonemeId] == 1 &&
	  minifst[mId].lcontexts[ cphonId] == 1) {
	myCfst.AddArc( minifst[modelId].stateEn,
		    fst::StdArc(EPSILON_LABEL,EPSILON_LABEL,0.0,
				minifst[mId].stateSt));
      }
    }
  }
  /* start node connections */
  myCfst.AddArc( stateSt,
	      fst::StdArc(EPSILON_LABEL, EPSILON_LABEL, 0.0,
			  minifst[silenceModelId].stateSt));
  myCfst.AddArc(  minifst[silenceModelId].stateEn,
	      fst::StdArc(EPSILON_LABEL, EPSILON_LABEL, 0.0, stateEn));
#endif
  
  fst::StdVectorFst fst2;
  fst::StdVectorFst* ofst = &myCfst;
  if(do_until_step>0) {
    printf("invert\n");
    fst::Invert(&myCfst);
    bool FLAGS_connect = true;
    if(do_until_step>1) {
      printf("rmepsilon\n");
      fst::RmEpsilon( &myCfst, FLAGS_connect);
      if(do_until_step>2) {
	printf("determinize\n");
	fst::Determinize(myCfst, &fst2);
	ofst = &fst2;
	if(do_until_step>3) {
	  printf("arcsort olabels\n");
	  fst::ArcSort(&fst2, fst::StdOLabelCompare());
	}
      }
    }
  }

#if 0
  for(fst::SymbolTableIterator syms_iter( *syms); !syms_iter.Done(); syms_iter.Next() ) {
    int value = (int)syms_iter.Value();
    const char* key = syms_iter.Symbol();
  } 
#endif
  
  printf("writing output file %s\n", cfstFilename);
  
  // We can save this FST to a file with: 
  /* fail compilation if char and LCHAR aren't the same! */

  { char zzz[ 1 - (sizeof(LCHAR)!=sizeof(char))]; zzz[0] = 0; } 
  ofst->Write((const char*)cfstFilename);

  CA_FreeArbdata( ca_arbdata);

  PMemShutdown();

  //  CLEANUP:
  return (int)rc;
}


