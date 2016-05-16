/*---------------------------------------------------------------------------*
 *  SemanticGraphImpl.c  *
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

#include "SR_SemprocPrefix.h"
#include "SR_SemprocDefinitions.h"
#include "SR_SemanticGraph.h"
#include "SR_SemanticGraphImpl.h"
#include "SR_SemanticProcessorImpl.h"
#include "ESR_ReturnCode.h"
#include "passert.h"
#include "pendian.h"
#include "plog.h"

static const char* MTAG = __FILE__;
#define AVG_SCRIPTS_PER_WORD 2.5
#define SLOTNAME_INDICATOR "__"
#define SLOTNAME_INDICATOR_LEN 2

#define PTR_TO_IDX(ptr, base) ((asr_uint32_t) (ptr == NULL ? 0xFFFFFFFFu : \
                               (asr_uint32_t)(ptr - base)))
#define IDX_TO_PTR(idx, base) (idx == 0xFFFFFFFFu ? NULL : base + idx)

ESR_ReturnCode SR_SemanticGraphCreate(SR_SemanticGraph** self)
{
  SR_SemanticGraphImpl* impl;

  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  impl = NEW(SR_SemanticGraphImpl, MTAG);
  if (impl == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }
  /* do not assume NEW initialize impl as zero, do it here */
  memset(impl, 0, sizeof(SR_SemanticGraphImpl));

  impl->Interface.destroy = &SR_SemanticGraph_Destroy;
  impl->Interface.unload = &SR_SemanticGraph_Unload;
  impl->Interface.load = &SR_SemanticGraph_Load;
  impl->Interface.save = &SR_SemanticGraph_Save;
  impl->Interface.addWordToSlot = &SR_SemanticGraph_AddWordToSlot;
  impl->Interface.reset = &SR_SemanticGraph_Reset;
  impl->script_olabel_offset = SEMGRAPH_SCRIPT_OFFSET;
  impl->scopes_olabel_offset = SEMGRAPH_SCOPE_OFFSET;

  *self = (SR_SemanticGraph*) impl;
  return ESR_SUCCESS;
}


/**
 * Default implementation.
 */
ESR_ReturnCode SR_SemanticGraph_Destroy(SR_SemanticGraph* self)
{
  SR_SemanticGraphImpl* impl = (SR_SemanticGraphImpl*) self;

  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }

  FREE(impl);
  return ESR_SUCCESS;
}

ESR_ReturnCode sr_semanticgraph_loadV2(SR_SemanticGraphImpl* impl, wordmap* ilabels, PFile* fp);


/* private function */
ESR_ReturnCode SR_SemanticGraph_LoadFromImage(SR_SemanticGraph* self, wordmap* ilabels, const LCHAR* g2g)
{
  SR_SemanticGraphImpl* impl = (SR_SemanticGraphImpl*) self;
  PFile* fp = NULL;
  struct
  {
    asr_uint32_t rec_context_image_size;
    /*  image data size of the recognition graph */
    asr_uint32_t format;
  }
  header;
  ESR_ReturnCode rc = ESR_SUCCESS;
  ESR_BOOL isLittleEndian;
  /*
    #if __BYTE_ORDER==__LITTLE_ENDIAN
    isLittleEndian = ESR_TRUE;
    #else
    isLittleEndian = ESR_FALSE;
    #endif
  */
  isLittleEndian = ESR_TRUE;

  fp = pfopen ( g2g, L("rb"));
/*  CHKLOG(rc, PFileSystemCreatePFile(g2g, isLittleEndian, &fp));
  CHKLOG(rc, PFileOpen(fp, L("rb")));*/

  if ( fp == NULL )
    goto CLEANUP;

  /* header */
  if (pfread(&header, 4, 2, fp) != 2)
  {
    rc = ESR_READ_ERROR;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }

  if (pfseek(fp, header.rec_context_image_size, SEEK_SET))
  {
    rc = ESR_READ_ERROR;
    PLogError(L("ESR_READ_ERROR: could not seek to semgraph data"));
    goto CLEANUP;
  }

  if (header.format == IMAGE_FORMAT_V2)
  {
    rc = sr_semanticgraph_loadV2(impl, ilabels, fp);
  }
  else
  {
    rc = ESR_INVALID_STATE;
    PLogError("PCLG.txt P.txt inconsistency");
    goto CLEANUP;
  }

CLEANUP:
  if (fp)
    pfclose (fp);
  if (rc != ESR_SUCCESS)
  {
    if (impl->arc_token_list != NULL)
    {
      FREE(impl->arc_token_list);
      impl->arc_token_list = NULL;
    }
  }
  return rc;
}

static ESR_ReturnCode deserializeArcTokenInfoV2(SR_SemanticGraphImpl *impl,
    PFile* fp);

static ESR_ReturnCode serializeArcTokenInfoV2(SR_SemanticGraphImpl *impl,
    PFile* fp);

ESR_ReturnCode sr_semanticgraph_loadV2(SR_SemanticGraphImpl* impl, wordmap* ilabels, PFile* fp)
{
  unsigned int i, nfields;
  ESR_ReturnCode rc = ESR_SUCCESS;
  struct
  {
    asr_uint32_t format;
    asr_uint32_t sgtype;
  }
  header;
  asr_uint32_t tmp[32];

  if (pfread(&header, 4/*sz*/, 2/*ni*/, fp) != 2)
  {
    rc = ESR_READ_ERROR;
    PLogError(L("ESR_READ_ERROR: could not read V2"));
    goto CLEANUP;
  }

  if (header.sgtype == GrammarTypeItemList)
  {
    /*
      tmp = new unsigned short[num_words];
      if( pfread( tmp, sizeof(tmp[0]), num_words, fp) != num_words) {
      rc = ESR_READ_ERROR;
      PLogMessage("can't read %d word script assocs\n", num_words);
      goto CLEANUP;
      }
    */
    /* convert these to an arc_token_list or whatever */
    PLogError("not supported v2 itemlist type");
    rc = ESR_INVALID_STATE;
    goto CLEANUP;

  }
  else
  {

    nfields = 2;
    if (pfread(tmp, sizeof(tmp[0]), nfields, fp) != nfields)
    {
      rc = ESR_WRITE_ERROR;
      PLogError(L("ESR_WRITE_ERROR: could not write script_olabel_offset"));
      goto CLEANUP;
    }
    i = 0;
    impl->script_olabel_offset = (wordID)tmp[i++];
    impl->scopes_olabel_offset = (wordID)tmp[i++];
    ASSERT(i == nfields);

    /* word arcs */
    if ((rc = deserializeArcTokenInfoV2(impl, fp)) != ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }

    /* use the ilabels provided externally (from recog graph ilabels) */
    impl->ilabels = ilabels;

    /* scopes */
    if ((rc = deserializeWordMapV2(&impl->scopes_olabels, fp)) != ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }

    /* scripts */
    if ((rc = deserializeWordMapV2(&impl->scripts, fp)) != ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
  }
CLEANUP:
  return rc;
}


static arc_token_lnk get_first_arc_leaving_node1(arc_token* arc_token_list,
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

static int strlen_with_null(const char* word) 
{ /* from srec_context.c */
  int len = strlen(word) + 1;
  if (len % 2 == 1) len++;
  return len;
}
/* private function */
ESR_ReturnCode SR_SemanticGraph_LoadFromTextFiles(SR_SemanticGraph* self, wordmap* ilabels, const LCHAR* basename, int num_words_to_add)
{
  ESR_ReturnCode rc = ESR_FATAL_ERROR;
  arcID num_scripts;
  int isConstString = 0;
  LCHAR filename[MAX_STRING_LEN];
  LCHAR line[MAX_SCRIPT_LEN];
  LCHAR iword[MAX_STRING_LEN];
  LCHAR oword[MAX_SCRIPT_LEN];
  LCHAR *p;
  unsigned int max_num_arc_tokens;
  nodeID from_node, into_node;
  wordID ilabel = 0;
  labelID olabel = 0;
  arc_token *atoken;
  arc_token *last_atoken;
  costdata cost = 0;
  arcID num_arcs;
  arc_token* arc_token_list;
  long fpos;
  PFile* p_text_file = NULL;
  PFile* scripts_file;
  SR_SemanticGraphImpl* semgraph = (SR_SemanticGraphImpl*) self;
  size_t lineNo;
  unsigned int i;
  wordID num_scope_words;
  asr_int32_t num_scope_chars;
  LCHAR* _tMp;    /* used by IS_SCOPE_MARKER() below */

  /* use the ilables that are provided externally (from recog graph ilabels) */
  semgraph->ilabels = ilabels;



  /* try to open the .script file */
  LSTRCPY(filename, basename);
  LSTRCAT(filename, ".script");
  scripts_file = pfopen ( filename, L("r") );
/*  CHKLOG(rc, PFileSystemCreatePFile(filename, TRUE, &scripts_file));
  CHKLOG(rc, PFileOpen(scripts_file, L("r")));*/

  if ( scripts_file == NULL )
  {
    rc = ESR_OPEN_ERROR;
    goto CLEANUP;
  }

  /* Load the scripts file
    assumptions:

  - the scripts file has each line ordered starting from 0 as such
  <integer><space><script>

  - the integer MUST become the index of the script in the wordmap

  - output labels referenced in the semgraph are the integers (wordmap index) prepending with '_'

  - output labels stored in the semgraph are actually integers which are equal to
    script_olabel_offset + <integer>
  */

  /* determine number of words/chars to allocate */
  fpos = pftell(scripts_file);
  for (i = num_scripts = 0; pfgets(line, MAX_SCRIPT_LEN, scripts_file); num_scripts++)
  {
    size_t len = LSTRLEN(line) + 1;
    if (len % 2) len++;
    i = i + len; /* count the chars */
  }
  pfseek(scripts_file, fpos, SEEK_SET);

  /* on each line I will have 1 big word */
  /* figure that each script for dynamically added words will be a simple assignment
     like myVar='someVal' ... which looks like almost 2.5 words, hence *2.5 */
  wordmap_create(&semgraph->scripts, i, num_scripts, (int)AVG_SCRIPTS_PER_WORD*num_words_to_add);

  /* load up all the information */
  lineNo = 0;
  while (pfgets(line, MAX_SCRIPT_LEN, scripts_file))
  {
    ASSERT( sizeof( iword[0]) == sizeof(char)); // else more code to write!
    if (sscanf(line, "%s ", iword) == 1)
    {
      LSTRCPY(oword, line + LSTRLEN(iword) + 1);
      /* may actually have spaces in it and this is messing me up ... here is the fix */
      /* copy the line starting after the iword */
      for (i = 0, p = line + LSTRLEN(iword) + 1; *p; p++)
      {
        if (*p == '\\')
        {
          if (isConstString)
            oword[i++] = *p;
          ++p;
        }
        else if (*p == '\'')
          isConstString = (isConstString ? 0 : 1) ; /* toggle */
        if (isConstString || !isspace(*p))
          oword[i++] = *p;
      }
      oword[i] = '\0';

      /* make sure that the index in the wordmap matches the line number */
      if (wordmap_add_word(semgraph->scripts, oword) != lineNo)
      {
        PLogError(L("ESR_READ_ERROR: internal error adding script (%d)"), num_words_to_add);
        return ESR_NO_MATCH_ERROR;
      }
      lineNo++;
    }
    else
    {
      PLogMessage(L("can't parse line %s"), line);
      passert(0);
    }
  }
  pfclose (scripts_file);

  /* try to open the P.txt file */
  LSTRCPY(filename, basename);
  LSTRCAT(filename, ".P.txt");
  p_text_file = pfopen ( filename, L("r"));
/*  CHKLOG(rc, PFileSystemCreatePFile(filename, TRUE, &p_text_file));
  CHKLOG(rc, PFileOpen(p_text_file, L("r")));*/

  if ( p_text_file == NULL )
    goto CLEANUP;

  /* determine number of word arcs to allocate */
  fpos = pftell(p_text_file);
  num_scope_words = 0;
  num_scope_chars = 0;
  for (num_arcs = 0; pfgets(line, MAX_STRING_LEN, p_text_file); ++num_arcs)
  {
    if (num_arcs == MAXarcID)
      break; /* error */
	if (sscanf(line, "%hu\t%hu\t%[^\t]\t%[^\t\n\r]", &from_node, &into_node, iword, oword) == 4)
    {
		if (IS_SCOPE_MARKER(oword)) {
			num_scope_words++;
			num_scope_chars += strlen_with_null( oword);
			if(num_scope_chars) num_scope_chars++ ;
  }
	}
  }
  max_num_arc_tokens = num_arcs + (arcID)num_words_to_add;
  MEMCHK(rc, max_num_arc_tokens, MAXarcID);
  pfseek(p_text_file, fpos, SEEK_SET);

  semgraph->arc_token_list = NEW_ARRAY(arc_token,max_num_arc_tokens, L("semgraph.wordgraph"));
  arc_token_list = semgraph->arc_token_list;
  /* need to initialize my wordmap */
  wordmap_create(&semgraph->scopes_olabels, num_scope_chars, num_scope_words,0); // max_num_arc_tokens);

  /* 1. first load up all the information */
  i = 0;
  while (pfgets(line, MAX_STRING_LEN, p_text_file))
  {
    if (sscanf(line, "%hu\t%hu\t%[^\t]\t%[^\t\n\r]", &from_node, &into_node, iword, oword) == 4)
    {
      /* the cost is 0 by default */
      cost = 0;
      /* since I am reading strings, and I want to store integers, I need to get
      the index of the string by looking up in the ilabels wordmap */
      ilabel = wordmap_find_index(ilabels, iword);

      /* now for the olabels, depending on the type of the label, I either use the index directly
      or save the index in a wordmap which will eventually give me the right index.
      Remember that the index must be offset by a certain value depending on which wordmap I'm using */

      if (IS_SCRIPT_MARKER(oword)) /* olabel type: script */
      {
        olabel = (labelID) atoi(&oword[1]);
        olabel = (wordID)(olabel + semgraph->script_olabel_offset); /* the offset */
      }
      else if (IS_SCOPE_MARKER(oword)) /* olabel type: scope marker */
      {
        /* check if the label is already in the wordmap, and reuse index */
        olabel = wordmap_find_index(semgraph->scopes_olabels, oword);

        if (olabel == MAXwordID) /* not found so add to wordmap and get new index */
          olabel = wordmap_add_word(semgraph->scopes_olabels, oword);
        olabel = (wordID)(olabel + semgraph->scopes_olabel_offset); /* the offset */
      }
      else /* olabel type: input symbols hopefully !!! */
      {
	/* if oword does not have a \t in the end, add a \t*/

        /* check if the label is already in the wordmap, and reuse index */
        olabel = wordmap_find_index(ilabels, oword);

        if (olabel == MAXwordID) /* not found so add to wordmap and get new index */
          PLogMessage(L("output label not found: %s"), oword);
      }

    }
    else if (sscanf(line, "%hu", &from_node) == 1)
    {
      into_node = MAXnodeID;
      ilabel = MAXwordID;
      olabel = MAXwordID;
      cost = 0;
    }
    else
    {
      PLogMessage(L("can't parse line %s"), line);
      passert(0);
    }

    /* okay, now that I have the data for the current arc, save it to the arc_token data structure*/
    atoken = &arc_token_list[i];
    ++i;

    atoken->ilabel = ilabel;
    atoken->olabel = olabel;
    /* atoken->cost = cost; not used for now */

    /* initially this stores INTEGERS !!! , I need to cross-reference the integers with the
    appropriate arc_token pointers (in the next steps for the algorithm) */
    atoken->first_next_arc = (arc_token_lnk)into_node;
    atoken->next_token_index = (arc_token_lnk)from_node;
  }
  num_arcs = (arcID) i;

  pfclose(p_text_file);
  p_text_file = NULL;

  wordmap_setbase(semgraph->scopes_olabels);
  wordmap_ceiling(semgraph->scopes_olabels); /* we won't be adding scopes! */
  wordmap_setbase(semgraph->scripts);

  /* 2. now do the internal cross references */
  /* in this pass we build the 1-to-1 links, and n-to-1 links in a graph */
  /* in other words... first_next_arc points to the first arc leaving the node */
  for (i = 0; i < num_arcs; ++i)
  {
    atoken = &arc_token_list[i];
    into_node = (nodeID)(int)atoken->first_next_arc; /* get the integer */
    atoken->first_next_arc = /* converts the integer id to a arc_token pointer */
      get_first_arc_leaving_node1(arc_token_list, num_arcs, (nodeID)(int)atoken->first_next_arc);
  }

  /* 3. now do more internal cross refs */
  /* in this pass we build the 1-to-n links */
  /* in other words ... setup the linked list of all arc leaving from the same node */
  last_atoken = &arc_token_list[0];
  for (i = 1; i < num_arcs; ++i)
  {
    atoken = &arc_token_list[i];
    /* if this arc and the last one do NOT leave the same node (i.e. from_node, see above),
    then the next_token_index is not used */
    if (atoken->next_token_index != last_atoken->next_token_index)
      last_atoken->next_token_index = ARC_TOKEN_NULL;
    else
      last_atoken->next_token_index = ARC_TOKEN_LNK(arc_token_list, i);
    last_atoken = atoken;
  }
  last_atoken->next_token_index = ARC_TOKEN_NULL;

#if DEBUG_ASTAR
  /* under debug, it's nice to be able to see the words leaving the
     destination node, they are stored sequentially in the debug ary */
  for (i = 0; i < num_arcs; i++)
  {
    LCHAR * p;
    arc_token* tmp;
    atoken = &arc_token_list[i];
    atoken->debug[0] = 0;
    tmp = ARC_TOKEN_PTR(arc_token_list, atoken->first_next_arc);
    for (; tmp; tmp = ARC_TOKEN_PTR(arc_token_list, tmp->next_token_index))
    {
      if (tmp->first_next_arc == ARC_TOKEN_NULL)
        p = "END";
      else if (!tmp->label)
        p = "NULL";
      else
        p = tmp->label;
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
  semgraph->arc_token_list_len = (arcID)max_num_arc_tokens;
  /* initialize the freelist */
  if (num_arcs < max_num_arc_tokens)
  {
    semgraph->arc_token_freelist = &semgraph->arc_token_list[num_arcs];
    for (i = num_arcs; i < max_num_arc_tokens - 1; i++)
    {
      semgraph->arc_token_list[i].first_next_arc = ARC_TOKEN_NULL;
      semgraph->arc_token_list[i].next_token_index = ARC_TOKEN_LNK(semgraph->arc_token_list, (i + 1));
    }
    semgraph->arc_token_list[i].first_next_arc = ARC_TOKEN_NULL;
    semgraph->arc_token_list[i].next_token_index = ARC_TOKEN_NULL;
  }
  else
    semgraph->arc_token_freelist = NULL;

  /* for dynamic addition */
  for (i = 0; i < MAX_NUM_SLOTS; i++)
    semgraph->arcs_for_slot[i] = NULL;

	semgraph->arc_token_insert_start = semgraph->arc_token_list + num_arcs;
    semgraph->arc_token_insert_end = NULL;
  return ESR_SUCCESS;
CLEANUP:
  if (p_text_file)
    pfclose (p_text_file);
  return rc;
}

ESR_ReturnCode SR_SemanticGraph_Load(SR_SemanticGraph* self, wordmap* ilabels, const LCHAR* basename, int num_words_to_add)
{
  ESR_ReturnCode rc;

  if (LSTRSTR(basename, L(".g2g")))
  {
    rc = SR_SemanticGraph_LoadFromImage(self, ilabels, basename);
  }
  else
  {
    rc = SR_SemanticGraph_LoadFromTextFiles(self, ilabels, basename, num_words_to_add);
  }
  return rc;
}

/**
 * Unload Sem graph
 */
ESR_ReturnCode SR_SemanticGraph_Unload(SR_SemanticGraph* self)
{
  SR_SemanticGraphImpl* semgraph = (SR_SemanticGraphImpl*) self;

  /* see the wordmap_create in the Load function */
  wordmap_destroy(&semgraph->scopes_olabels);
  wordmap_destroy(&semgraph->scripts);

  FREE(semgraph->arc_token_list);
  semgraph->arc_token_list = 0;
  return ESR_SUCCESS;
}

ESR_ReturnCode sr_semanticgraph_saveV1(SR_SemanticGraphImpl* impl, const LCHAR* g2g);
ESR_ReturnCode sr_semanticgraph_saveV2(SR_SemanticGraphImpl* impl, const LCHAR* g2g);

ESR_ReturnCode SR_SemanticGraph_Save(SR_SemanticGraph* self, const LCHAR* g2g, int version_number)
{
  SR_SemanticGraphImpl* impl = (SR_SemanticGraphImpl*) self;
  ESR_ReturnCode rc = ESR_SUCCESS;

  if (version_number == 2)
  {
    rc = sr_semanticgraph_saveV2(impl,  g2g);
  }
  else
  {
    PLogError("invalid version_number %d\n", version_number);
    rc = ESR_INVALID_ARGUMENT;
  }
  return rc;
}


int sr_semanticgraph_get_type(SR_SemanticGraphImpl* impl)
{
  arc_token *atoken, *arc_token_list = impl->arc_token_list;
  arc_token_lnk mergept;
  int expected_ilabel;
  atoken = impl->arc_token_list;

  /* 0 1 eps {
     1 2 13e_avenue myRoot}
     ...
     1 2 13e_avenue myRoot}
     2 */
  if (atoken->ilabel != WORD_EPSILON_LABEL)
    return GrammarTypeBNF;
  atoken = ARC_TOKEN_PTR(arc_token_list, atoken->first_next_arc);
  if (!atoken)
    return GrammarTypeBNF;
  mergept = atoken->first_next_arc;
  expected_ilabel = NUM_ITEMLIST_HDRWDS;
  for (; atoken; atoken = ARC_TOKEN_PTR(arc_token_list, atoken->next_token_index))
  {
    if (atoken->first_next_arc != mergept)
      return GrammarTypeBNF;
    if (atoken->ilabel != expected_ilabel)
      return GrammarTypeBNF;
    expected_ilabel++;
  }
  if (expected_ilabel != impl->ilabels->num_words)
    return GrammarTypeBNF;
  atoken = ARC_TOKEN_PTR(arc_token_list, mergept);
  for (; atoken; atoken = ARC_TOKEN_PTR(arc_token_list, atoken->first_next_arc))
  {
    if (atoken->next_token_index != ARC_TOKEN_NULL)
      return GrammarTypeBNF;
    if (atoken->ilabel != WORD_EPSILON_LABEL &&
        !(atoken->ilabel == MAXwordID && atoken->olabel == MAXwordID))
      return GrammarTypeBNF;
  }
  return GrammarTypeItemList;
}

#define SEMGR_OUTPUT_FORMAT_V2 478932784

ESR_ReturnCode sr_semanticgraph_saveV2(SR_SemanticGraphImpl* impl, const LCHAR* g2g)
{
  ESR_ReturnCode rc;
  PFile* fp;
  asr_uint32_t tmp[32];
  struct
  {
    asr_uint32_t format;
    asr_uint32_t sgtype;
  }
  header;
  unsigned int i, nfields;

  fp = pfopen ( g2g, L("r+b"));
/*  CHKLOG(rc, PFileSystemCreatePFile(g2g, isLittleEndian, &fp));
  CHKLOG(rc, PFileOpen(fp, L("r+b")));*/

  if ( fp == NULL )
  {
  	rc = ESR_OPEN_ERROR;
    goto CLEANUP;
  }

  pfseek(fp, 0, SEEK_END);

  header.format = IMAGE_FORMAT_V2;
  header.sgtype = sr_semanticgraph_get_type(impl);
  header.sgtype = GrammarTypeBNF;

#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage("sr_semanticgraph_saveV2() semgraphtype %d", header.sgtype);
#endif
  if (pfwrite(&header, 4 /*sz*/, 2/*ni*/, fp) != 2)
  {
    rc = ESR_WRITE_ERROR;
    PLogError(L("ESR_WRITE_ERROR: could not write V2"));
    goto CLEANUP;
  }

  if (header.sgtype == GrammarTypeItemList)
  {
    arc_token *parser, *atok;

    /* write num_words size array of short script ids
       this might be just a y=x array, but it could be there
       are synonyms, eg. NEW_YORK NEW_YORK_CITY -> same script
    */
    parser = impl->arc_token_list;
    parser = ARC_TOKEN_PTR(impl->arc_token_list, parser->first_next_arc);
    for (i = NUM_ITEMLIST_HDRWDS; i < impl->ilabels->num_words; i++)
    {
      for (atok = parser; atok; atok = ARC_TOKEN_PTR(impl->arc_token_list, atok->next_token_index))
      {
        if (atok->ilabel == i) break;
      }
      if (!atok)
      {
        rc = ESR_INVALID_STATE;
        PLogError("Can't find word %d in semgraph\n", i);
        goto CLEANUP;
      }
      tmp[0] = atok->olabel;
      if (pfwrite(tmp, sizeof(tmp[0]), 1, fp) != 1)
      {
        rc = ESR_WRITE_ERROR;
        PLogError(L("ESR_WRITE_ERROR: could not write V2"));
        goto CLEANUP;
      }
    }
    if ((rc = serializeWordMapV2(impl->scripts, fp)) != ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
  }
  else
  {

    i = 0;
    tmp[i++] = impl->script_olabel_offset;
    tmp[i++] = impl->scopes_olabel_offset;
    nfields = i;

    if (pfwrite(tmp, sizeof(tmp[0]), nfields, fp) != nfields)
    {
      rc = ESR_WRITE_ERROR;
      PLogError(L("ESR_WRITE_ERROR: could not write script_olabel_offset"));
      goto CLEANUP;
    }

    /* word arcs */
    if ((rc = serializeArcTokenInfoV2(impl, fp)) != ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }

    /* do not WRITE ilabels... this is a ref to the olabels from rec context */

    /* scopes */
    if ((rc = serializeWordMapV2(impl->scopes_olabels, fp)) != ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }

    if ((rc = serializeWordMapV2(impl->scripts, fp)) != ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
#ifdef SREC_ENGINE_VERBOSE_LOGGING
    PLogMessage("G2G done WR semg %d", pftell(fp));
#endif
  }
  rc = ESR_SUCCESS;
CLEANUP:
  if (fp)
    pfclose (fp);
  return rc;
}

arc_token* arc_tokens_find_ilabel(arc_token* base, arc_token* arc_token_list, wordID ilabel)
{
  arc_token* p;
  for (p = arc_token_list; p != NULL; p = ARC_TOKEN_PTR(base, p->next_token_index))
    if (p->ilabel == ilabel) return p;
  return NULL;
}

arc_token* arc_tokens_get_free(arc_token* base, arc_token** arc_token_freelist)
{
  arc_token* tmp = (*arc_token_freelist);
  if (tmp == NULL)
    return NULL;
  (*arc_token_freelist) = ARC_TOKEN_PTR(base, tmp->next_token_index);
  tmp->ilabel = tmp->olabel = 0;
  tmp->next_token_index = ARC_TOKEN_NULL;
  tmp->first_next_arc = ARC_TOKEN_NULL;
  return tmp;
}

int arc_tokens_list_size(arc_token* base, arc_token* head)
{
  arc_token* tmp = head;
  int count = 0;
  for (; tmp; tmp = ARC_TOKEN_PTR(base, tmp->next_token_index))
  {
    count++;
  }
  return count;
}

void arc_tokens_free_list(arc_token* base, arc_token** arc_token_freelist, arc_token* head)
{
  arc_token *tail, *next = (arc_token*)1;
  if (head == NULL)
    return;
  for (tail = head; ; tail = next)
  {
    next = ARC_TOKEN_PTR(base, tail->next_token_index);
    if (next == NULL) break;
  }
  tail->next_token_index = ARC_TOKEN_PTR2LNK(base, (*arc_token_freelist));
  *arc_token_freelist = head;
}

ESR_ReturnCode find_in_union_of_scripts(const LCHAR* union_script, const LCHAR* script, ESR_BOOL* isFound)
{
  const LCHAR* start;
  const LCHAR* end;
  const LCHAR* p;
  const LCHAR* q;

  if (union_script == NULL || script == NULL)
    return ESR_INVALID_ARGUMENT;

  start = LSTRCHR(union_script, L('\''));
  if (start == NULL)
    return ESR_INVALID_ARGUMENT;

  start++; /* point to first char after \' */

  end = LSTRCHR(start, L('\'')); /* point to last \' */
  if (end == NULL)
    return ESR_INVALID_ARGUMENT;

  p = start;

  start = LSTRCHR(script, L('\''));
  if (start == NULL)
    return ESR_INVALID_ARGUMENT;
  start++; /* point to first char after \' */

  q = start;

  while (p < end)
  {
    if (*p == MULTIPLE_MEANING_JOIN_CHAR) /* if at the end of a meaning (not end of union)
                                                  and p matched q all the way up to join char then found! */
    {
      *isFound = ESR_TRUE;
      return ESR_SUCCESS;
    }
    else if (*p == *q) /* while same keep going */
    {
      if (*p == *(end - 1)) /* if at the end and p matched q all the way then found! */
      {
        *isFound = ESR_TRUE;
        return ESR_SUCCESS;
      }
      q++;
    }
    else /* skip to next meaning after join char */
    {
      while (*p != MULTIPLE_MEANING_JOIN_CHAR && p < end)
        p++;
      /* reset q */
      q = start;
    }
    p++;
  }

  *isFound = ESR_FALSE;
  return ESR_SUCCESS;
}

#define QUOTE_CHAR L('\'')
int count_num_literals(const LCHAR* a, const LCHAR** start_points, int max_num_start_points)
{
  int num = 0;
  const LCHAR *p, *q = a;
  const LCHAR *end = a + LSTRLEN(a);
  while (1)
  {
    /* look for starting QUOTE_CHAR */
    for (p = q; p < end; p++)
    {
      if (*p == ESC_CHAR) p++;
      else if (*p == QUOTE_CHAR) break;
    }
    if (p == end) break;
    if (num > max_num_start_points) break; /* just abort the counting! */
    start_points[num] = p;
    /* look for ending QUOTE_CHAR */
    for (q = p + 1; q < end; q++)
    {
      if (*q == ESC_CHAR) q++;
      else if (*q == QUOTE_CHAR) break;
    }
    if (q == end) /* does not close! */
      return -1;
    p = ++q;
    num++;
  }
  return num;
}
int union_literal_pair(LCHAR* o, LCHAR* a, LCHAR* b, LCHAR** pptra)
{
  LCHAR *enda, *ptra, *endb, *ptrb;
  LCHAR *p, *ptro;
  enda = a + LSTRLEN(a);
  endb = b + LSTRLEN(b);
  /* capture the data from a to ptra */
  for (ptra = a + 1; ptra < enda; ptra++)
  {
    if (*ptra == ESC_CHAR) ptra++;
    else if (*ptra == QUOTE_CHAR) break;
  }
  /* capture the data from b to ptrb */
  for (ptrb = b + 1; ptrb < endb; ptrb++)
  {
    if (*ptrb == ESC_CHAR) ptrb++;
    else if (*ptrb == QUOTE_CHAR) break;
  }
  /* now make the output */
  ptro = o;
  *ptro++ = QUOTE_CHAR;
  for (p = a + 1; p < ptra; p++) *ptro++ = *p;
  *ptro++ = MULTIPLE_MEANING_JOIN_CHAR;
  for (p = b + 1; p < ptrb; p++) *ptro++ = *p;
  *ptro++ = QUOTE_CHAR;
  *ptro++ = 0;
  *pptra = ptra + 1;
  return 0;
}

/* now handles n1='52';n2='62'; UNION n1='53';nx='63'; */

ESR_ReturnCode make_union_of_scripts(LCHAR* union_script, const size_t max_len, const LCHAR* a, const LCHAR* b)
{
  int i, num_literals_in_a, num_literals_in_b;
  LCHAR *spa[8], *spb[8], *spo[8], *ptra;

  if (a == NULL || b == NULL)
    return ESR_INVALID_ARGUMENT;

  num_literals_in_a = count_num_literals(a, (const LCHAR **)spa, 8);
  num_literals_in_b = count_num_literals(b, (const LCHAR **)spb, 8);

  if (num_literals_in_a == 0 && num_literals_in_b == 0)
  {
    if (LSTRLEN(a) > max_len) return ESR_BUFFER_OVERFLOW;
    else
    {
      LSTRCPY(union_script, a);
      return ESR_SUCCESS;
    }
  }
  else if (num_literals_in_a != num_literals_in_b)
  {
    return ESR_INVALID_ARGUMENT;
  }

  /* V='Springfield_IL' union V='Springfield_MA' is V='Springfield_IL#Springfield_MA' */
  /* 18               +       18          -2     =  33 + 1 for NULL             */
  if ((LSTRLEN(a) + LSTRLEN(b) - 2) > max_len)
  {
    PLogError("Temp buffer (size %d) to hold union of multiple meanings (size %d) is too small", max_len, (LSTRLEN(a) + LSTRLEN(b) - 2));
    return ESR_BUFFER_OVERFLOW;
  }

  LSTRCPY(union_script, a);
  for (i = 0; i < num_literals_in_a; i++)
  {
    count_num_literals(union_script, (const LCHAR **)spo, 8);
    /* here union_script is n0='52';n1='62'; */
    union_literal_pair(spo[i], spa[i], spb[i], &ptra);
#ifdef _WIN32
    if (LSTRLEN(spo[i]) > MAX_SEMPROC_VALUE)
      pfprintf(PSTDOUT, "Warning: won't be able to parse this script! len %d>%d %s\n", LSTRLEN(spo[i]), MAX_SEMPROC_VALUE, spo[i]);
#endif
    /* here union_script is n0='52#53' */
    LSTRCAT(union_script, ptra);
    /* here union_script is n0='52#53';n1='62'; */
  }
  return ESR_SUCCESS;
}

/**
 * Default implementation.
 */
ESR_ReturnCode SR_SemanticGraph_AddWordToSlot(SR_SemanticGraph* self, const LCHAR* _slot, const LCHAR* word, const LCHAR* script, const ESR_BOOL newWordAddedToFST)
{
  struct SR_SemanticGraphImpl_t *impl = (struct SR_SemanticGraphImpl_t*) self;
  arc_token *token, *tmp;
  arc_token *tmp_arc_token_list;
  wordID wdID, scriptID, old_scriptID;
  wordID slotID;
  LCHAR union_script[MAX_STRING_LEN]; /* sizeof used elsewhere */
  ESR_ReturnCode rc; int i;
  int tmp_arc_token_list_len;
  int offset;
#define MAX_WORD_LEN 128
  char veslot[MAX_WORD_LEN];

  if (script == NULL || *script == L('\0') || !LSTRCMP(script, L("NULL")))
    return ESR_SUCCESS; /* no script to add so keep going */
  
  /* find out if the word I am adding already exists. If it already exists, then that means that I
     potentially am adding an alternate meaning for the word */
  /* the slotname in .PCLG.txt and .map files use __ as the indicator. Xufang */
  if(_slot[0] == '@') {
    strcpy(veslot,SLOTNAME_INDICATOR);
    strcat(veslot,_slot+1);
    strcat(veslot,SLOTNAME_INDICATOR);
  } else 
    strcpy(veslot, _slot);

  slotID = wordmap_find_rule_index(impl->ilabels, veslot);
  if (slotID == MAXwordID)
  {
    PLogError(L("ESR_NO_MATCH_ERROR: Could not find slotID in wordmap %s"), _slot);
    return ESR_NO_MATCH_ERROR;
  }
  wdID = wordmap_find_index_in_rule(impl->ilabels, word, slotID);
  if (wdID == MAXwordID)
  {
    PLogError(L("ESR_NO_MATCH_ERROR: Could not find wordID/slotID in wordmap %s/%d"), word, slotID);
    return ESR_NO_MATCH_ERROR;
  }

  /* **this is an optimization step** */
  /* Is word already added in this slot? if so, get the token pointer, else, token is NULL
   *
   * the assumption is that FST_AddWordToGrammar will tell us if this word was newly added in the FST, or
   * if the word was added at least 1 iteration ago, meaning that I have already added it to my
   * semgraph slot at some earlier point
   */
  if (newWordAddedToFST)
    token = NULL;
  else
    token = arc_tokens_find_ilabel(impl->arc_token_list, impl->arcs_for_slot[slotID], wdID);

#define FST_GROW_FACTOR   12/10
#define FST_GROWARCS_MIN    100  
  if (token == NULL) /* new word to add to slot */
  {
    /* add the script if new  */
    scriptID = wordmap_find_index(impl->scripts, script);
    if (scriptID == MAXwordID)
      scriptID = wordmap_add_word(impl->scripts, script);
    if (scriptID == MAXwordID)
    {
      PLogError(L("ESR_OUT_OF_MEMORY: Could not add script to wordmap"));
      return ESR_OUT_OF_MEMORY;
    }

    token = impl->arcs_for_slot[slotID];
    tmp = arc_tokens_get_free(impl->arc_token_list, &(impl->arc_token_freelist));
    if (tmp == NULL)
      {
#if defined (FST_GROW_FACTOR)
	tmp_arc_token_list_len = impl->arc_token_list_len * FST_GROW_FACTOR;
	if(tmp_arc_token_list_len - impl->arc_token_list_len <=FST_GROWARCS_MIN)
	  tmp_arc_token_list_len+=FST_GROWARCS_MIN;
	
	tmp_arc_token_list= NEW_ARRAY(arc_token,tmp_arc_token_list_len, L("semgraph.wordgraph"));
	if(!tmp_arc_token_list) {
	  PLogError(L("ESR_OUT_OF_MEMORY: Could not extend allocation of semgraph.wordgraph"));
	  return ESR_OUT_OF_MEMORY;
	}
	memcpy(tmp_arc_token_list,impl->arc_token_list, impl->arc_token_list_len*sizeof(arc_token));
	
	for(i=0; i<MAX_NUM_SLOTS;i++)
	  {
	    if(impl->arcs_for_slot[i] != NULL) { 
	      offset = impl->arcs_for_slot[i] - impl->arc_token_list;
	      impl->arcs_for_slot[i] = tmp_arc_token_list + offset;
	    }
	  }
	token = impl->arcs_for_slot[slotID];
	
	ASSERT( impl->arc_token_freelist == NULL);
	
	impl->arc_token_freelist = tmp_arc_token_list + impl->arc_token_list_len;
	
	FREE(impl->arc_token_list);
	impl->arc_token_insert_start = tmp_arc_token_list + (impl->arc_token_insert_start - impl->arc_token_list); //Rabih fix
	impl->arc_token_list = tmp_arc_token_list;

	for (i = impl->arc_token_list_len; i < tmp_arc_token_list_len - 1; i++)
	  {
	    impl->arc_token_list[i].first_next_arc = ARC_TOKEN_NULL;
	    impl->arc_token_list[i].next_token_index = ARC_TOKEN_LNK(impl->arc_token_list, (i + 1));
	  }
	impl->arc_token_list[i].first_next_arc = ARC_TOKEN_NULL;
	impl->arc_token_list[i].next_token_index = ARC_TOKEN_NULL;
	
	impl->arc_token_list_len = tmp_arc_token_list_len;
	tmp = arc_tokens_get_free(impl->arc_token_list, &(impl->arc_token_freelist));
      }
#endif
    if(tmp == NULL) {
      PLogError(L("ESR_OUT_OF_MEMORY: Error adding more arcs to graph\n"));
      return ESR_OUT_OF_MEMORY;
    }
    impl->arcs_for_slot[slotID] = tmp;
    tmp->next_token_index = ARC_TOKEN_PTR2LNK(impl->arc_token_list, token);
    tmp->ilabel = wdID;
    tmp->olabel = (wordID)(impl->script_olabel_offset + scriptID);
  }
  else
  {
    old_scriptID = token->olabel - impl->script_olabel_offset;

    if (!LSTRCMP(impl->scripts->words[old_scriptID], script))
    {
      /* nothing to do, we have the word, same meaning again so do nothing */
    }
    else
    {

      CHKLOG(rc, make_union_of_scripts(union_script, sizeof(union_script), impl->scripts->words[old_scriptID], script));

#ifdef SREC_ENGINE_VERBOSE_LOGGING
      PLogMessage(L("Adding alternate meaning %s for word %s (%s) in slot %s\n"), script, word,
                  impl->scripts->words[old_scriptID], impl->ilabels->words[slotID]);
#endif
      /* add the union as if new (if not already there) */
      scriptID = wordmap_find_index(impl->scripts, union_script);
      if (scriptID == MAXwordID)
        scriptID = wordmap_add_word(impl->scripts, union_script);
      if (scriptID == MAXwordID)
      {
        PLogError(L("ESR_OUT_OF_MEMORY: Could not add script to wordmap"));
        return ESR_OUT_OF_MEMORY;
      }

      /* make the olabel point to the union */
      token->olabel = (wordID)(impl->script_olabel_offset + scriptID);
    }
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}


/**
 * Default implementation.
 */
ESR_ReturnCode SR_SemanticGraph_Reset(SR_SemanticGraph* self)
{
  struct SR_SemanticGraphImpl_t *impl = (struct SR_SemanticGraphImpl_t*) self;
  wordID slotid;
  arc_token* tmp;
  arc_token *tmp_arc_token_list;

  wordmap_reset(impl->scopes_olabels);
  wordmap_reset(impl->scripts);
  wordmap_reset(impl->ilabels);   //Rabih: I added this
  for (slotid = 1; slotid < impl->ilabels->num_slots; slotid++)
  {
    tmp = impl->arcs_for_slot[slotid];
    arc_tokens_free_list(impl->arc_token_list, &(impl->arc_token_freelist), tmp);
    impl->arcs_for_slot[slotid] = NULL;
#if defined(SANITY_CHECK)
    int count;
    for (count = 0, tmp = impl->arc_token_freelist; tmp != NULL;
         tmp = ARC_TOKEN_PTR(impl->arc_token_list, tmp->next_token_index))
    {
      ASSERT(tmp->ilabel != 79324);
      tmp->ilabel = 79324;
      count++;
    }
    PLogError("after reset freelist size is %d", count);
#endif
  }
  
  // Rabih : Reset the arc_token_list
  if(impl->ilabels->num_words == impl->ilabels->num_base_words)
  {}
  else{
  impl->arc_token_list_len = (size_t)(impl->arc_token_insert_start - impl->arc_token_list);
  tmp_arc_token_list= NEW_ARRAY(arc_token,impl->arc_token_list_len, L("semgraph.wordgraph"));
  memcpy(tmp_arc_token_list,impl->arc_token_list, impl->arc_token_list_len*sizeof(arc_token)); 
  
  impl->arc_token_freelist = NULL;
  
  FREE(impl->arc_token_list);
  impl->arc_token_list = tmp_arc_token_list; 
  }
  return ESR_SUCCESS;
}

static ESR_ReturnCode serializeArcTokenInfoV2(SR_SemanticGraphImpl *impl,
    PFile* fp)
{
  int i;
  asr_uint32_t idx;
  arcID tmp[32];

  if (pfwrite(&impl->arc_token_list_len, 2, 1, fp) != 1)
    return ESR_WRITE_ERROR;

  idx = PTR_TO_IDX(impl->arc_token_freelist, impl->arc_token_list);

  if (pfwrite(&idx, 4, 1, fp) != 1)
    return ESR_WRITE_ERROR;

  idx = PTR_TO_IDX(impl->arc_token_insert_start, impl->arc_token_list);
  
  if (pfwrite(&idx, 4, 1, fp) != 1)
    return ESR_WRITE_ERROR;

  idx = 0;
  if (pfwrite(&idx, 4, 1, fp) != 1)
    return ESR_WRITE_ERROR;

  for (i = 0; i < impl->arc_token_list_len; ++i)
  {
    arc_token* token = &impl->arc_token_list[i];
    tmp[0] = token->ilabel;
    tmp[1] = token->olabel;
    tmp[2] = ARC_TOKEN_IDX(impl->arc_token_list, token->first_next_arc);
    tmp[3] = ARC_TOKEN_IDX(impl->arc_token_list, token->next_token_index);
    if (pfwrite(tmp, sizeof(tmp[0]), 4, fp) != 4)
      return ESR_WRITE_ERROR;
  }

  /* new, fixes load/save bug 2007 July 31 
	todo: change 4 to sizeof(asr_uint32) */
  if(1) {
	asr_uint32_t idx[MAX_NUM_SLOTS];
	for(i=0; i<MAX_NUM_SLOTS; i++) 
		idx[i] = PTR_TO_IDX(impl->arcs_for_slot[i], impl->arc_token_list);
	if (pfwrite(&idx, 4, MAX_NUM_SLOTS, fp) != MAX_NUM_SLOTS)
			return ESR_WRITE_ERROR;
  }

  return ESR_SUCCESS;
}

static ESR_ReturnCode deserializeArcTokenInfoV2(SR_SemanticGraphImpl *impl,
    PFile* fp)
{
  int i;
  asr_uint32_t idx;
  ESR_ReturnCode rc = ESR_SUCCESS;
  arcID tmp[32];

  if (pfread(&impl->arc_token_list_len, 2, 1, fp) != 1)
  {
    rc = ESR_READ_ERROR;
    PLogError(L("ESR_READ_ERROR: could not read arc_token_list_len"));
    return rc;
  }

  impl->arc_token_list = NEW_ARRAY(arc_token,
                                   impl->arc_token_list_len,
                                   L("semgraph.wordgraph"));

  if (impl->arc_token_list == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    PLogError(ESR_rc2str(rc));
    return ESR_OUT_OF_MEMORY;
  }

  if (pfread(&idx, 4, 1, fp) != 1)
  {
    rc = ESR_READ_ERROR;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }

  impl->arc_token_freelist = IDX_TO_PTR(idx, impl->arc_token_list);

  if (pfread(&idx, 4, 1, fp) != 1)
  {
    rc = ESR_READ_ERROR;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }

  impl->arc_token_insert_start = IDX_TO_PTR(idx, impl->arc_token_list);
  // impl->arc_token_insert_start = impl->arc_token_list + impl->arc_token_list_len; // Rabih's fix

  if (pfread(&idx, 4, 1, fp) != 1)
  {
    rc = ESR_READ_ERROR;
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  impl->arc_token_insert_end = 0;

  for (i = 0; i < impl->arc_token_list_len; ++i)
  {
    arc_token* token = &impl->arc_token_list[i];
    if (pfread(tmp, sizeof(tmp[0]), 4, fp) != 4)
    {
      rc = ESR_READ_ERROR;
      goto CLEANUP;
    }
    token->ilabel = tmp[0];
    token->olabel = tmp[1];
    if (tmp[2] == MAXarcID)
      token->first_next_arc = ARC_TOKEN_NULL;
    else
      token->first_next_arc = ARC_TOKEN_LNK(impl->arc_token_list, tmp[2]);
    if (tmp[3] == MAXarcID)
      token->next_token_index = ARC_TOKEN_NULL;
    else
      token->next_token_index = ARC_TOKEN_LNK(impl->arc_token_list, tmp[3]);
  }

  /* new, fixes load/save bug 2007 July 31 
	todo: change 4 to sizeof(asr_uint32) */
  if(1) {
		asr_uint32_t idx[MAX_NUM_SLOTS];
		if (pfread(&idx[0], 4, MAX_NUM_SLOTS, fp) != MAX_NUM_SLOTS) {
			rc = ESR_READ_ERROR;
			PLogError(ESR_rc2str(rc));
			goto CLEANUP;
		}
		for(i=0; i<MAX_NUM_SLOTS; i++) 
			impl->arcs_for_slot[i] = IDX_TO_PTR(idx[i], impl->arc_token_list);
   }

  return ESR_SUCCESS;

CLEANUP:
  FREE(impl->arc_token_list);
  impl->arc_token_list =
    impl->arc_token_freelist =
      impl->arc_token_insert_start =
        impl->arc_token_insert_end = NULL;
  return rc;
}
