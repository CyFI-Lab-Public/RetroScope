/*---------------------------------------------------------------------------*
 *  grxmlcompile.cpp  *
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

#include <ext/hash_map>

#include "fst/lib/fst.h"
#include "fst/lib/fstlib.h"
#include "fst/lib/arc.h"
#include "fst/lib/fst-decl.h"
#include "fst/lib/vector-fst.h"
#include "fst/lib/arcsort.h"
#include "fst/lib/invert.h"

#include "fst-io.h"

#include "ESR_Locale.h"
#include "LCHAR.h"
#include "pstdio.h"
#include "PFileSystem.h"
#include "PANSIFileSystem.h"
#include "plog.h"
#include "pmemory.h"
#include "ESR_Session.h"
#include "SR_Session.h"
#include "SR_Vocabulary.h"
#include "srec_arb.h"       // for EPSILON_LABEL etc
#include <fstream>
#include <iostream>
#include "tinyxml.h"
#include "grxmldoc.h"

#ifdef MEMTRACE
#include <mcheck.h>
#endif

#define OPENFST_ACKNOWLEDGEMENT	\
	"This tool uses the OpenFst library. \n" \
 "Licensed under the Apache License, Version 2.0 (the \"License\");\n" \
" you may not use this file except in compliance with the License.\n" \
" You may obtain a copy of the License at" \
"\n" \
"      http://www.apache.org/licenses/LICENSE-2.0\n" \
"\n" \
" Unless required by applicable law or agreed to in writing, software\n" \
" distributed under the License is distributed on an \"AS IS\" BASIS,\n" \
" WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" \
" See the License for the specific language governing permissions and\n" \
" limitations under the License.\n" \
"\n" \
" This library was developed at Google Research (M. Riley, J. Schalkwyk, W. Skut) and NYU's Courant Institute (C. Allauzen, M. Mohri). It is intended to be comprehensive, flexible, efficient and scale well to large problems. It is an open source project distributed under the Apache license. \n" 


#define TINYXML_ACKNOWLEDGEMENT	\
	"This tool uses the tinyxml library. \n" \
"Copyright (c) 2007 Project Admins: leethomason \n" \
"The TinyXML software is provided 'as-is', without any express or implied\n" \
"warranty. In no event will the authors be held liable for any damages\n" \
"arising from the use of this software.\n" \
"\n" \
"Permission is granted to anyone to use this software for any purpose,\n" \
"including commercial applications, and to alter it and redistribute it\n" \
"freely, subject to the following restrictions:\n" 

#define NUANCE_COPYRIGHT \
"// grxmlcompile\n" \
"//\n" \
"// Licensed under the Apache License, Version 2.0 (the \"License\");\n" \
"// you may not use this file except in compliance with the License.\n" \
"// You may obtain a copy of the License at\n" \
"//\n" \
"//      http://www.apache.org/licenses/LICENSE-2.0\n" \
"//\n" \
"// Unless required by applicable law or agreed to in writing, software\n" \
"// distributed under the License is distributed on an \"AS IS\" BASIS,\n" \
"// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" \
"// See the License for the specific language governing permissions and\n" \
"// limitations under the License.\n" \
"//\n" \
"// This program compiles a .grxml grammar into the graphs needed for \n" \
"// decoding with SREC\n" \
"// \n" 

#define MAX_LINE_LENGTH   256
#define MAX_PATH_NAME 512
#define MAX_PRONS_LENGTH 1024
#define SILENCE_PREFIX_WORD "-pau-"
#define SILENCE_SUFFIX_WORD "-pau2-"
#define SLOT_SUFFIX "__"
#define SLOT_PREFIX "__"
#define MAX_NUM_SLOTS 12 /* must agree with srec_context.h */
#define EXTRA_EPSILON_LABEL 39999 // must be higher than the number of models
#define DEFAULT_WB_COST 40
#define DEFAULT_WB_COST_STR "40"
#define SLOT_COUNTER_OFFSET 30000 // must be higher than the number of models
#define NOISE_PHONEME_CODE 'J'

static int debug = 0;
static int verbose = 0;

using namespace std;

ESR_ReturnCode make_openfst_graphs(GRXMLDoc* pDoc, /* for metas */
				   const std::string& grxmlBasename,
				   const char* vocabFilename,
				   const char* cfstFilename, 
				   const char* modelmapFilename);

const char* showline(const char* fn, int line_num)
{
  static char line[8096] = { 0 };
  int line_count = 0;
  ifstream strm(fn);
  while (strm && strm.getline(line, sizeof(line))) 
	  if(line_count++ == line_num) break;
  return &line[0];
}

std::string ExtractFileName(const std::string& full)
{
  std::string::size_type idx = full.find_last_of("/");
    
  if (idx != std::string::npos)
    return full.substr(idx+1);
  else 
    return full;
}

/*-----------------------------------------------------------------------*
 *                                                                       *
 *                                                                       *
 *-----------------------------------------------------------------------*/

int usage_error(const char* prgname)
{
  printf("USAGE: -par <par file> -grxml <grxml grammar file> -vocab <dictionary file (.ok)> [-outdir <output directory>]\n");
  return (int)ESR_INVALID_ARGUMENT;
}

int main(int argc, char* argv[])
{
  ESR_ReturnCode status = ESR_SUCCESS;
  char *parfile = NULL;
  char *grxmlfile = NULL;
  char *cmdline_vocfile = NULL;
  std::string outdir("."); // default output dir is current directory
  /* for now, assume char and LCHAR are the same, else fail to compile! */
  { char zzz[ 1 - (sizeof(LCHAR)!=sizeof(char))]; zzz[0] = 0; } 

#ifdef MEMTRACE
    mtrace();
#endif

#if defined(GRXMLCOMPILE_PRINT_ACKNOWLEDGEMENT)
    cout << OPENFST_ACKNOWLEDGEMENT <<std::endl;
    cout << TINYXML_ACKNOWLEDGEMENT <<std::endl;
    cout << NUANCE_COPYRIGHT <<std::endl;
#endif

    // Process all XML files given on command line

    if(argc<5){
      return usage_error(argv[0]);
    }

    for(int i=1;i<argc;i++)
    {
      if(!strcmp(argv[i],"-grxml"))
        grxmlfile = argv[++i];
      else if(!strcmp(argv[i],"-debug"))
        debug++;
      else if(!strcmp(argv[i],"-verbose"))
        verbose++;
      else if(!strcmp(argv[i],"-par") || !strcmp(argv[i],"-parfile"))
        parfile = argv[++i];
      else if(!strcmp(argv[i],"-vocab")) 
        cmdline_vocfile = argv[++i];
      else if(!strcmp(argv[i],"-outdir")) 
        outdir = std::string(argv[++i]);
      else {
        printf("error_usage: argument [%s]\n", argv[i]);
	return usage_error(argv[0]);
	return (int)ESR_INVALID_ARGUMENT;
      }
    }

    //process_xml( std::string(grxmlfile), parfile );
    std::string filename = std::string(grxmlfile);

    /***************************
            process xml
    ***************************/

    cout << "processing [" << filename << "] ..." << endl;
    
    TiXmlDocument node;
    bool bLoadedOK = node.LoadFile( filename.c_str() );
    if(!bLoadedOK || node.Error()) {
      std::cout << "Error: while creating TiXmlDocument from " << filename << std::endl;
      std::cout << "Error: " << node.Error() << " id " << node.ErrorId() << " row " << node.ErrorRow() << " col " << node.ErrorCol() << std::endl;
      std::cout << "Error: " << node.ErrorDesc() <<  std::endl;
      std::cout << "Error: near " << showline( filename.c_str(), node.ErrorRow()) << std::endl;
      return (int)ESR_INVALID_ARGUMENT;
    }
    
    
    // *************************************************
    //	Parse the file into a DOM object and create word graph
    //
    GRXMLDoc *doc = new (GRXMLDoc);
    std::string filenameNoPath = ExtractFileName(filename);
    doc->parseGrammar( node, filenameNoPath );   // THE PARSING AND NETWORK BUILD HAPPENS IN HERE
    /************************
      end of xml processing
    ************************/
    
    // Create grammar network files. Use prefix of input file for output.
    std::string s = filename;
    std::string grxmlbase = outdir + "/" + ExtractFileName(grxmlfile);
    unsigned int p1 = grxmlbase.find_last_of(".");
    if ( p1 != string::npos ) 
      grxmlbase.assign( grxmlbase, 0, p1);

    std::string newName;
    newName = grxmlbase + ".map";
    doc->writeMapFile( newName );
    newName = grxmlbase + ".script";
    doc->writeScriptFile( newName );

    doc->writeGraphFiles( grxmlbase, false );
    
    // 
    // SR initialization
    //
    char vocfile[MAX_PATH_NAME];
    char cfstfile[MAX_PATH_NAME];
    char modelmapfile[MAX_PATH_NAME];
    size_t len;
    
    PMemInit();
    printf("info: Using parfile %s\n",parfile);
    status = SR_SessionCreate((const LCHAR*) parfile);
    // status = SR_SessionCreate ( parfile );
    if (  status != ESR_SUCCESS ) {
      LPRINTF("Error: SR_SessionCreate(%s) %s\n", parfile, ESR_rc2str(status));
      return (int)status;
    }
    
    // vocfile
    if(cmdline_vocfile) {
      strcpy( vocfile, cmdline_vocfile);
    } else {
      len = MAX_PATH_NAME;
      ESR_SessionGetLCHAR ( L("cmdline.vocabulary"), (LCHAR*)vocfile, &len );
      // skip PrefixWithBaseDirectory(), 'tis done inside SR_VocabularyLoad()
    }
    printf("info: Using dictionary %s\n",vocfile);
    
    // modelmapfile
    len = MAX_PATH_NAME;
    ESR_SessionGetLCHAR ( L("cmdline.arbfile"), (LCHAR*)modelmapfile, &len);
    len = MAX_PATH_NAME;
    status = ESR_SessionPrefixWithBaseDirectory ( (LCHAR*)modelmapfile, &len);
    char* p = strrchr(modelmapfile,'/');
    if(!p) p = strrchr(modelmapfile,'\\');
    if(p) strcpy(p, "/models128x.map");
    
    // cfstfile
    len = MAX_PATH_NAME;
    ESR_SessionGetLCHAR ( L("cmdline.arbfile"), (LCHAR*)cfstfile, &len);
    len = MAX_PATH_NAME;
    status = ESR_SessionPrefixWithBaseDirectory ( (LCHAR*)cfstfile, &len);
    p = strrchr(cfstfile,'/');
    if(!p) p = strrchr(cfstfile,'\\');
    if(p) strcpy(p, "/generic.C");
    
    status = make_openfst_graphs( doc, grxmlbase, (const char*)vocfile, (const char*)cfstfile, (const char*)modelmapfile);
    if(status != ESR_SUCCESS) {
      LPRINTF("Error: make_openfst_graphs() returned %s\n",  ESR_rc2str(status));      
    } else {
      /* make_openfst_graphs() can sometimes call doc->setMeta() to put
	 Session parameters into the .params file, so writeParamsFile()
	 should be called after make_openfst_graphs() */
      newName = grxmlbase + ".params";
      doc->writeParamsFile( newName );
    }

    //
    // SR de-initialization
    //
    SR_SessionDestroy();
    PMemShutdown();
    
    delete doc;
    return (int)status;
}

/*-----------------------------------------------------------------*
 * utils                                                           *
 *-----------------------------------------------------------------*/

bool is_slot_symbol( const char* sym)
{
  const char* p = strstr(sym,SLOT_PREFIX);
  int len = strlen(sym);
  if(len>4 && !strcmp(sym+len-2,SLOT_SUFFIX) && (p-sym)<len-2) {
    return true;
  } else 
    return false;
}

int64 StrToId(const char *s, fst::SymbolTable *syms,
	      const char *name) 
{
  int64 n;
  if (syms) {
    n = syms->Find(s);
    if (n < 0) {
      cerr << "FstReader: Symbol \"" << s
	   << "\" is not mapped to any integer " << name
	   << ", symbol table = " << syms->Name();
    }
  } else {
    char *p;
    n = strtoll(s, &p, 10);
    if (p < s + strlen(s) || n < 0) {
      cerr << "FstReader: Bad " << name << " integer = \"" << s;
    }
  }
  return n;
}

/* FstMergeOLabelsToILabels, FstSplitOLabelsFromILabels
   are used to make sure the minimization does not go overboard in pushing
   output labels toward the beginning of the graph.  When that happens 
   then the speech recognition decoder fails! */

ESR_ReturnCode FstMergeOLabelsToILabels( fst::StdVectorFst& fst_, int max_ilabels )
{
  fst::StdArc::StateId s = fst_.Start();
  if (s == fst::kNoStateId)
    return ESR_INVALID_ARGUMENT;
  for (fst::StateIterator< fst::StdVectorFst> siter(fst_); 
       !siter.Done(); siter.Next()) {
    s = siter.Value();
    
    for(fst::MutableArcIterator<fst::StdVectorFst> aiter(&fst_, s);
	!aiter.Done(); aiter.Next()) {
      fst::StdArc arc = aiter.Value();
      if( arc.ilabel >= max_ilabels || 
	  (float)arc.ilabel + ((float)max_ilabels)*arc.olabel > INT_MAX) {
	std::cout << "Error: internal error in FstMergeOLabelsToILabels() " << std::endl;
	return ESR_NOT_IMPLEMENTED;
      }
      arc.ilabel = arc.ilabel + max_ilabels * arc.olabel;
      arc.olabel = 0;
      aiter.SetValue( arc);
    }
  }
  return ESR_SUCCESS;
}

ESR_ReturnCode FstMergeOLabelsToILabels_GetMax( fst::StdVectorFst& fst_, int& max_ilabel )
{
  if (fst_.Start() == fst::kNoStateId) return ESR_INVALID_ARGUMENT;
  for (fst::StateIterator< fst::StdVectorFst> siter(fst_); 
       !siter.Done(); siter.Next()) {
    for(fst::MutableArcIterator<fst::StdVectorFst> aiter(&fst_, siter.Value());
	!aiter.Done(); aiter.Next()) {
      if( aiter.Value().ilabel > max_ilabel) 
	max_ilabel = aiter.Value().ilabel;
    }
  }
  max_ilabel++;
  return ESR_SUCCESS;
}

ESR_ReturnCode FstSplitOLabelsFromILabels( fst::StdVectorFst& fst_, int max_ilabels )
{
  fst::StdArc::StateId s = fst_.Start();
  if (s == fst::kNoStateId)
    return ESR_INVALID_ARGUMENT;
  for (fst::StateIterator< fst::StdVectorFst> siter(fst_); 
       !siter.Done(); siter.Next()) {
    s = siter.Value();

    for(fst::MutableArcIterator<fst::StdVectorFst> aiter(&fst_, s);
	!aiter.Done(); aiter.Next()) {
      fst::StdArc arc = aiter.Value();
      arc.olabel = arc.ilabel / max_ilabels;
      arc.ilabel = arc.ilabel - arc.olabel*max_ilabels;
      aiter.SetValue( arc);
    }
  }
  return ESR_SUCCESS;
}

/* this is to replace the "fake" extra epsilon input labels, which were
   put there to disambiguate homonyms */

ESR_ReturnCode FstReplaceILabel( fst::StdVectorFst& fst_, int from_ilabel, int into_ilabel)
{
  fst::StdArc::StateId s = fst_.Start();
  if (s == fst::kNoStateId)
    return ESR_INVALID_ARGUMENT;
  for (fst::StateIterator< fst::StdVectorFst> siter(fst_); 
       !siter.Done(); siter.Next()) {
    s = siter.Value();

    for(fst::MutableArcIterator<fst::StdVectorFst> aiter(&fst_, s);
	!aiter.Done(); aiter.Next()) {
      fst::StdArc arc = aiter.Value();
      if(arc.ilabel == from_ilabel) {
	arc.ilabel = into_ilabel;
	aiter.SetValue( arc);
      }
    }
  }
  return ESR_SUCCESS;
}

/* this pushes the slot labels forward which gives an opportunity for 
   multiple instances of the slot to be merged, eg. lookup NAME
   vs lookup contact NAME .. if in separate rules, then they will
   merge thanks to using 3 arcs for the NAME */

ESR_ReturnCode FstPushSlotLikeOLabels( fst::StdVectorFst& fst_, int myMin, int myMax)
{
  int i;
  ESR_ReturnCode rc = ESR_SUCCESS;
  char done_for_state[2*65536]; // hope this is enough!
  memset( &done_for_state[0], 0, sizeof(done_for_state));

  fst::StdArc::StateId s = fst_.Start();
  for (fst::StateIterator< fst::StdVectorFst> siter(fst_); 
       !siter.Done(); siter.Next()) {
    s = siter.Value();

    if(done_for_state[ s]) continue;
    done_for_state[ s]++;

    for(fst::MutableArcIterator<fst::StdVectorFst> aiter(&fst_, s);
	!aiter.Done(); aiter.Next()) {
      fst::StdArc arc = aiter.Value();
      if(arc.olabel >= myMin && arc.olabel < myMax) {
	fst::StdArc::StateId s2 = arc.nextstate;
	int slotId = arc.olabel;

	if(verbose) 
	  std::cout << "info: FstPushSlotLikeOLabels() at state " << s << " arc ilabel " << arc.ilabel << " olabel " << arc.olabel << std::endl;

	arc.ilabel = EPSILON_LABEL;
	arc.olabel = EPSILON_LABEL;
	arc.weight = 0; // zero weight
	aiter.SetValue( arc);
	done_for_state[ s2]++;
	for(fst::MutableArcIterator<fst::StdVectorFst> aiter2(&fst_, s2);
	    !aiter2.Done(); aiter2.Next()) {
	  fst::StdArc arc2 = aiter2.Value();
	  if(arc2.ilabel == WORD_BOUNDARY) {
	    std::cout << "Error: FstPushSlotLikeOLabels() failing, there could be confusion between the slot (hack-pron) and a real-pron, the slot olabel may have been pushed by earlier fst operations!" << std::endl;
	    rc = ESR_INVALID_STATE;
	  } else 
	    arc2.ilabel = EPSILON_LABEL;
	  arc2.olabel = slotId;
	  aiter2.SetValue( arc2);
	}
      }
    }
  }

  /* check */
  int *num_pclg_arcs_using_slot = new int[myMax];
  for(i=0;i<myMax;i++) num_pclg_arcs_using_slot[i] = 0;
  for (fst::StateIterator< fst::StdVectorFst> siter(fst_); 
       !siter.Done(); siter.Next()) {
    s = siter.Value();

    for(fst::MutableArcIterator<fst::StdVectorFst> aiter(&fst_, s);
	!aiter.Done(); aiter.Next()) {
      fst::StdArc arc = aiter.Value();
      if(arc.olabel >= myMin && arc.olabel < myMax) 
	num_pclg_arcs_using_slot[arc.olabel]++;
    }
  }
  for(i=0; i<myMax; i++) {
    if(num_pclg_arcs_using_slot[i] > 1) {
      std::cout << "Error: SREC will not support multiply referred slots." << std::endl;
      std::cout << "Error: Consider re-working your grammar to merge the references into one rule" << std::endl;
      std::cout << "Error: or use two different slots" << std::endl;
      rc = ESR_NOT_SUPPORTED;
    }
  }
  delete [] num_pclg_arcs_using_slot;

  return rc;
}

/* gets the range of slot numbers, myMin inclusive, myMax is exclusive */

void get_slot_olabel_range( const fst::SymbolTable* syms, int* myMin, int* myMax)
{
  // assumes slots are at the top of the symbol table
  fst::SymbolTableIterator iter( *syms);
  *myMin = *myMax = 0;
  for(iter.Reset(); !iter.Done(); iter.Next() ) {
    const char* sym = iter.Symbol();
    if ( is_slot_symbol( sym)) {
      if(! (*myMin)) *myMin = iter.Value();
      *myMax = iter.Value()+1;
    }
  }
}

/* SLOT_COUNTER_OFFSET
   The cfst is used to turn phonemes into acoustic models, but we're using
   special phonemes for the slots, and must here add those as pass through
   in the Cfst, meaning that the slot marker must be unchanged after 
   composition.  To do that we find the places in the Cfst where silence is 
   used, and put the slot marker arcs in parallel.  This also causes the 
   models before the slot to assume silence to the right, and the models after
   the slot to assume silence to the left, both of which are reasonable */

ESR_ReturnCode FstAddSlotMarkersToCFst( fst::StdVectorFst& cfst_, int myMin, int myMax)
{
  int num_silence_arcs_in_cfst = 0;
  int mimicPhonemeCode = SILENCE_CODE;

  fst::StdArc::StateId s = cfst_.Start();
  if (s == fst::kNoStateId)
    return ESR_INVALID_ARGUMENT;
  for (fst::StateIterator< fst::StdVectorFst> siter(cfst_); 
       !siter.Done(); siter.Next()) {
    s = siter.Value();

    for(fst::MutableArcIterator<fst::StdVectorFst> aiter(&cfst_, s);
	!aiter.Done(); aiter.Next()) {
      fst::StdArc arc = aiter.Value();
      if( arc.olabel == mimicPhonemeCode) {
	num_silence_arcs_in_cfst++;
	for(int i=myMin; i<myMax; i++) 
	  cfst_.AddArc( s, fst::StdArc(SLOT_COUNTER_OFFSET+i /*model*/,
				       SLOT_COUNTER_OFFSET+i /*phoneme*/, 0.0, arc.nextstate));
      }
    }
  }
  fst::ArcSort(&cfst_, fst::StdOLabelCompare());
  if(!num_silence_arcs_in_cfst) 
    return ESR_INVALID_ARGUMENT;
  else 
    return ESR_SUCCESS;
}

/*
 * make the graphs used by the recognition engine during the search.
 */

ESR_ReturnCode make_openfst_graphs(  GRXMLDoc* pDoc,
				     const std::string& grxmlBasename,
				     const char* vocabFilename,
				     const char* cfstFilename, 
				     const char* modelmapFilename)
{
  SR_Vocabulary *vocab = 0;
  ESR_ReturnCode rc;
  
  fst::StdVectorFst l_fst;      // .L file, created from the .map and .ok
  
  int stateSt, stateEn;
  size_t len;
  bool do_skip_interword_silence = false;
  hash_map<string,int> homonym_count;
  int word_penalty = 0;

  rc = SR_VocabularyLoad(vocabFilename, &vocab);
  if (rc != ESR_SUCCESS) {
    cerr << "Error: " <<  ESR_rc2str(rc) << endl;
    return ESR_INVALID_ARGUMENT; // goto CLEANUP;
  }
  
  std::string word_penalty_str;
  if( pDoc->findMeta(std::string("word_penalty"),word_penalty_str)) 
    word_penalty = atoi((const char *)word_penalty_str.c_str());
  else {
    rc = ESR_SessionGetInt( L("CREC.Recognizer.wordpen"), &word_penalty);
    if(rc != ESR_SUCCESS) 
      word_penalty = DEFAULT_WB_COST;
    word_penalty_str = DEFAULT_WB_COST_STR;
    pDoc->setMeta( std::string("word_penalty"), word_penalty_str) ;
    cout << "using word_penalty " << word_penalty << endl;
  }

  std::string do_skip_interword_silence_str;
  if( pDoc->findMeta(std::string("do_skip_interword_silence"), do_skip_interword_silence_str)) 
    do_skip_interword_silence = ((do_skip_interword_silence_str != "true") ? false : true);
    
  /*-----------------------------------------------------------------*
   *   read the .map and .omap created from grxmlcompiler classes    *
   *-----------------------------------------------------------------*/
    
  std::string omapFilename = grxmlBasename + std::string(".omap");
  std::string imapFilename = grxmlBasename + std::string(".map");
  
  cout << "info: reading word symbols " << imapFilename << endl;
  fst::SymbolTable *word_syms = fst::SymbolTable::ReadText(imapFilename);
  if(!word_syms) {
    cerr << "error: reading word_syms" << endl;
    return ESR_INVALID_ARGUMENT;
  }
  cout << "info: reading parser symbols " << omapFilename << endl;
  fst::SymbolTable *prsr_syms = fst::SymbolTable::ReadText(omapFilename);
  if(!prsr_syms) {
    cerr << "error: reading prsr_syms" << endl;
    return ESR_INVALID_ARGUMENT;
  }
  cout << "info: reading model symbols " << modelmapFilename << endl;
  fst::SymbolTable *model_syms = fst::SymbolTable::ReadText(modelmapFilename);
  if(!prsr_syms) {
    cerr << "error: reading prsr_syms" << endl;
    return ESR_INVALID_ARGUMENT;
  }
  int max_model_sym = 0;
  /* if(1) {
     fst::SymbolTableIterator iter( *model_syms);
     for(iter.Reset(); !iter.Done(); iter.Next() ) max_model_sym++; */
  
  /*-----------------------------------------------------------------*
   * create the .L pronunciations transducer                         *
   *-----------------------------------------------------------------*/

  // Adds state 0 to the initially empty FST and make it the start state.
  stateSt = l_fst.AddState();   
  stateEn = l_fst.AddState();
  l_fst.SetStart(stateSt);  // arg is state ID
  l_fst.SetFinal(stateEn, 0.0);  // 1st arg is state ID, 2nd arg weight
  l_fst.AddArc(stateEn, fst::StdArc(EPSILON_LABEL,EPSILON_LABEL,0.0,stateSt));

  int num_slots = 0;
  fst::SymbolTableIterator iter( *word_syms);
  for(iter.Reset(); !iter.Done(); iter.Next() ) {
    ESR_ReturnCode rc;
    LCHAR prons[MAX_PRONS_LENGTH];
    const char* phrase = iter.Symbol();
    int wordId = iter.Value();
    bool wordId_is_silence = false;
    bool wordId_is_slot    = false;
    /* script or scope marker, skip it */
    /* if( is_scope_marker( phrase) || is_script_marker(phrase)) 
       continue; */
    /* epsilon */
    if(!strcmp( phrase, SILENCE_PREFIX_WORD) 
       || !strcmp(phrase,SILENCE_SUFFIX_WORD))
      wordId_is_silence = true;
    else if( !strcmp( phrase, "eps") && wordId == 0) 
      continue;
    /* rule markers */
    else if( strstr( phrase, ".grxml@"))
      continue;
    /* script markers */
    else if( phrase[0]=='_' && strspn(phrase+1,"0123456789")==strlen(phrase+1))
      continue;
    else if(is_slot_symbol(phrase)) {
      cout << "SLOT>> " << phrase << endl;
      wordId_is_slot = true;
      num_slots++;
    } 

    if(num_slots > MAX_NUM_SLOTS) {
      std::cout << "Error: SREC may have trouble with this many slots! (" << num_slots << ")" << std::endl;
      // return ESR_NOT_SUPPORTED;
    }

    if(wordId_is_slot) {
      int stateP = stateSt, statePp1;
      /* with 2 arcs, we have a better chance to merge the slot if used from
	 different parts of the grammar, see FstPushSlotLikeOLabels elsewhere */
      statePp1 = l_fst.AddState();
      l_fst.AddArc(stateP, fst::StdArc( wordId+SLOT_COUNTER_OFFSET, wordId, 0.0, statePp1));
      stateP = statePp1;  
      statePp1 = l_fst.AddState();
      l_fst.AddArc(stateP, fst::StdArc( wordId+SLOT_COUNTER_OFFSET, EPSILON_LABEL, 0.0, statePp1));
      stateP = statePp1;  
      l_fst.AddArc(stateP, fst::StdArc( WORD_BOUNDARY, EPSILON_LABEL, 0.0, stateEn));
    } else {
      size_t len_used;
      LCHAR *pron = 0, *p;
      /* word is ok, get the pron */
      len = MAX_PRONS_LENGTH;
      rc = SR_VocabularyGetPronunciation(vocab, phrase, prons, &len);
      if (rc != ESR_SUCCESS) {
	LPRINTF( "ERROR: SR_VocabularyGetPronunciation(*,%s,*,*) returned %s\n", phrase, ESR_rc2str(rc));
	SR_VocabularyDestroy(vocab);
	return rc;
      }
      for(len_used=0; len_used<len; ) {
	pron = &prons[0]+len_used;
	len_used += LSTRLEN(pron)+1;
	if( *pron == 0) break;
	int stateP = stateSt, statePp1;
	int olabel = wordId;
	LPRINTF("%s : %s\n", phrase, pron);
	/* main pronunciation */
	for(p=pron; *p; p++) {
	  statePp1 = l_fst.AddState();
	  if(*p == OPTSILENCE_CODE) {
	    l_fst.AddArc(stateP, fst::StdArc( SILENCE_CODE, olabel, 0.0, statePp1));
	    l_fst.AddArc(stateP, fst::StdArc( EPSILON_LABEL, olabel, 0.0, statePp1));
	  } else {
	    l_fst.AddArc(stateP, fst::StdArc( *p, olabel, 0.0, statePp1));
	  }
	  stateP = statePp1;
	  olabel = EPSILON_LABEL;
	}
	/* add epsilons if this is a homonym */
	string pron_string = pron;
	hash_map<string,int>::const_iterator it = homonym_count.find( pron_string);
	if(it == homonym_count.end()) {
	  homonym_count[ pron_string] = 0;
	} else {
	  homonym_count[ pron_string] = homonym_count[ pron_string]+1;
	}
	int extra_epsilons_needed = homonym_count[ pron_string] ;
	if(wordId_is_silence) extra_epsilons_needed = 0;
	for(int i=0;i<extra_epsilons_needed;i++) {
	  statePp1 = l_fst.AddState();
	  l_fst.AddArc(stateP, fst::StdArc( EXTRA_EPSILON_LABEL, olabel, 0.0, statePp1));
	  stateP = statePp1;
	}
	/* add optional silence after each word */
	if(!do_skip_interword_silence && !wordId_is_silence && !wordId_is_slot) {
	  statePp1 = l_fst.AddState();
	  l_fst.AddArc(stateP, fst::StdArc( SILENCE_CODE, EPSILON_LABEL, 0.0, statePp1));
	  l_fst.AddArc(statePp1, fst::StdArc( WORD_BOUNDARY, EPSILON_LABEL, 0.0, stateEn));
	  l_fst.AddArc(stateP, fst::StdArc( WORD_BOUNDARY, EPSILON_LABEL, 0.0, stateEn));
	} else if(wordId_is_silence && !strcmp(phrase, SILENCE_SUFFIX_WORD)) {
	  /* SILENCE_SUFFIX_WORD does not need a terminal .wb */
	  l_fst.AddArc(stateP, fst::StdArc( EPSILON_LABEL, EPSILON_LABEL, 0.0, stateEn));
	} else {
	  l_fst.AddArc(stateP, fst::StdArc( WORD_BOUNDARY, EPSILON_LABEL, 0.0, stateEn));
	}
      } // loop over multiple prons
    } // slot vs non-slot
  } /* .map (word_syms) iterator */
  
  std::string lfstFilename = grxmlBasename + ".L";
  // We can save this FST to a file with: 
  if(debug) l_fst.Write(lfstFilename.c_str());
  
  /*-----------------------------------------------------------------*
   *   read the .P.txt created from grxmlcompiler classes            *
   *-----------------------------------------------------------------*/
  
  std::string ptxtFilename = grxmlBasename + std::string(".P.txt");
  std::ifstream istrm(ptxtFilename.c_str());
  if(!istrm) {
    cerr << "error: reading ptxtFilename" << endl;
    return ESR_INVALID_ARGUMENT;
  }
  
  cout << "info: reading parser from text " << ptxtFilename << endl;
  fst::FstReader<fst::StdArc> reader( istrm, ptxtFilename, word_syms, prsr_syms, 
				      /*state_syms*/ NULL, 
				      /*acceptor*/ false,
				      /*ikeep*/ false,
				      /*okeep*/ false,
				      /*nkeep*/ false);
  // .P file, created from the .P.txt and .omap
  const fst::StdVectorFst& p_fst = reader.Fst();
  
  /*-----------------------------------------------------------------*
   *   make the helper FSTs                                          *
   *-----------------------------------------------------------------*/
  
  cout << "info: creating helper fsts" << endl;
  fst::StdVectorFst prefix_fst;
  fst::StdVectorFst suffix_fst;
  fst::StdVectorFst eps_fst;
  // int eps_word = StrToId("eps", word_syms, "arc ilabel");
  int pau_word = StrToId(SILENCE_PREFIX_WORD, word_syms, "arc ilabel");
  int pau2_word = StrToId(SILENCE_SUFFIX_WORD, word_syms, "arc ilabel");
  if(pau_word < 0 || pau2_word < 0) 
    return ESR_INVALID_ARGUMENT;
  
  stateSt = prefix_fst.AddState();   
  stateEn = prefix_fst.AddState();
  prefix_fst.SetStart(stateSt);  // arg is state ID
  prefix_fst.SetFinal(stateEn, 0.0);  // 1st arg is state ID, 2nd arg weight
  prefix_fst.AddArc(stateSt, fst::StdArc(pau_word, pau_word, 0.0, stateEn));
  
  stateSt = suffix_fst.AddState();   
  stateEn = suffix_fst.AddState();
  suffix_fst.SetStart(stateSt);  // arg is state ID
  suffix_fst.SetFinal(stateEn, 0.0);  // 1st arg is state ID, 2nd arg weight
  suffix_fst.AddArc(stateSt, fst::StdArc(pau2_word, pau2_word, 0.0, stateEn));
  
  stateSt = eps_fst.AddState();   
  stateEn = stateSt; // stateEn = eps_fst.AddState();
  eps_fst.SetStart(stateSt);  // arg is state ID
  eps_fst.SetFinal(stateEn, 0.0);  // 1st arg is state ID, 2nd arg weight
  // eps_fst.AddArc(stateSt, fst::StdArc(eps_word, eps_word, 0.0, stateEn));
  
  /*-----------------------------------------------------------------*
   *    make Grev2.det.txt                                           *
   *-----------------------------------------------------------------*/
  cout << "info: creating reverse g fst" << endl;
  fst::StdVectorFst g_fst = p_fst;   // this is a copy!!
  fst::StdVectorFst grev_fst;        // reversed
  fst::StdVectorFst grev_min_fst;    // eps removed and minimized
  fst::StdVectorFst grev_det_fst;

  fst::Project(&g_fst, fst::PROJECT_INPUT);
  if(debug) g_fst.Write( grxmlBasename + ".G");
  fst::Reverse( g_fst, &grev_fst);
  if(debug) grev_fst.Write( grxmlBasename + ".Grev");
  fst::RmEpsilon( &grev_fst, /*connect?*/ true );
  if(debug) grev_fst.Write( grxmlBasename + ".Grevrme");
  fst::Determinize(grev_fst, &grev_det_fst);
  if(debug) grev_det_fst.Write( grxmlBasename + ".Grevrmedet");
  if(1) fst::Minimize(&grev_det_fst);
  if(debug) grev_det_fst.Write( grxmlBasename + ".Grevrmedetmin");
  fst::Concat( &eps_fst, grev_det_fst);
  grev_det_fst = eps_fst;
  if(debug) grev_det_fst.Write( grxmlBasename + ".Grevrmedetmin2");
  std::string grevFilename = grxmlBasename + std::string(".Grev2.det.txt");
  
  cout << "info: writing reverse G fst as text " << grevFilename << endl;
  ostream* ostrm1 = new ofstream( grevFilename.c_str(), ios_base::out);
  fst::FstPrinter<fst::StdArc> printer1( grev_det_fst,
					word_syms, word_syms, 
					 NULL, /*acceptor?*/ true);
  printer1.Print( ostrm1, grevFilename);
  delete ostrm1;
  
  /*-----------------------------------------------------------------*
   *    make PCLG.txt                                                *
   *-----------------------------------------------------------------*/
  
  fst::StdVectorFst* c_fst;
  fst::StdVectorFst lg_fst;
  fst::StdVectorFst clg_fst;
  fst::StdVectorFst clg_det_fst;
  
  cout << "info: reading model fst " << cfstFilename << endl;
  c_fst = fst::StdVectorFst::Read( cfstFilename);
  
  int slot_olabel_min=0, slot_olabel_max=0; // [min,max) .. ie excludes max
  get_slot_olabel_range( word_syms, &slot_olabel_min, &slot_olabel_max);
  if(slot_olabel_max > MAX_NUM_SLOTS) 
    std::cout << "Error: SREC may have trouble with this many slots! (" << slot_olabel_max << ")" << std::endl;

  /* add slot markers as if they were silence phonemes, this makes the context
     for them as if the slot were silence, which is reasonable, although another
     reasonable thing would be to allow all contexts.  Adding the true context
     only would add complexity and slow down word addition too much. */

  rc = FstAddSlotMarkersToCFst( *c_fst, slot_olabel_min, slot_olabel_max);
  if(rc) return rc;

  fst::Concat( &g_fst, suffix_fst);
  fst::Concat( &prefix_fst, g_fst);
  if(debug) prefix_fst.Write( grxmlBasename + ".G2");    
  fst::ComposeOptions copts( /*connect?*/ true);
  
  fst::ArcSort(&l_fst, fst::StdOLabelCompare());
  fst::ArcSort(&prefix_fst, fst::StdILabelCompare());

  fst::Compose(l_fst, prefix_fst, &lg_fst, copts);
  if(debug) lg_fst.Write( grxmlBasename + ".LG");    
  fst::ArcSort(&lg_fst, fst::StdILabelCompare());
  if(debug) lg_fst.Write( grxmlBasename + ".LG2");    

  fst::RmEpsilon( &lg_fst, /*connect?*/ true );
  if(debug) lg_fst.Write( grxmlBasename + ".LGrme");    
  fst::Determinize( lg_fst, &clg_fst); // clg_fst is really lg_det_fst!
  if(debug) clg_fst.Write( grxmlBasename + ".LGrmedet");    
  rc = FstReplaceILabel( clg_fst, EXTRA_EPSILON_LABEL, EPSILON_LABEL);
  fst::Compose( *c_fst, clg_fst, &clg_det_fst, copts);
  if(debug) clg_det_fst.Write( grxmlBasename + ".CLGrmedet");    

  rc = FstMergeOLabelsToILabels_GetMax( clg_det_fst, /*int&*/max_model_sym);
  if(verbose)
    cout << "info: merging into ilabels I=i+" << max_model_sym << "*o" << endl;
  rc = FstMergeOLabelsToILabels( clg_det_fst, max_model_sym);
  if(debug) clg_det_fst.Write( grxmlBasename + ".CLGrmedet2");    
  fst::Minimize( &clg_det_fst);
  if(debug) clg_det_fst.Write( grxmlBasename + ".CLGrmedet3");    
  if(verbose) 
    cout << "info: splitting from ilabels" << endl;
  rc = FstSplitOLabelsFromILabels( clg_det_fst, max_model_sym);
  if(debug) clg_det_fst.Write( grxmlBasename + ".CLGrmedet4");    

  rc = FstPushSlotLikeOLabels( clg_det_fst, slot_olabel_min, slot_olabel_max);
  if(rc != ESR_SUCCESS) 
        std::cout << "Error: FstPushSlotLikeOLabels() failed" << std::endl;
  if(debug) clg_det_fst.Write( grxmlBasename + ".CLG");    

  std::string pclgFilename = grxmlBasename + ".PCLG.txt";
  ostream* ostrm = new ofstream( pclgFilename.c_str(), ios_base::out);
  fst::FstPrinter<fst::StdArc> printer( clg_det_fst, 
					model_syms, word_syms, 
					NULL, /*acceptor?*/ false);
  printer.Print( ostrm, pclgFilename);
  delete ostrm;
  
  delete c_fst;
  delete word_syms;  word_syms = NULL;
  delete prsr_syms;  prsr_syms = NULL;
  delete model_syms; model_syms = NULL;
  
  /*-----------------------------------------------------------------*
   *    cleanup                                                      *
   *-----------------------------------------------------------------*/

  if(vocab) {
    SR_VocabularyDestroy(vocab);
    vocab = NULL;
  }
  
  return rc;

}


