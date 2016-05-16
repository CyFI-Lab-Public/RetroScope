/*---------------------------------------------------------------------------*
 *  parseStringTest.c  *
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
#include "SR_SemanticResult.h"
#include "ESR_Session.h"
#include "ESR_Locale.h"
#include "LCHAR.h"

#include "PFileSystem.h"
#include "PANSIFileSystem.h"

/* for testing RecognizerImpl.c, see below */
#include"buildopt.h"
#include"setting.h"
#include"srec_sizes.h"
#include"SR_GrammarImpl.h"

/* defines */
#define MAX_LINE_LENGTH 256
#define MAX_STR_LENGTH  512
#define MAX_SEM_RESULTS   3
#define MAX_KEYS         30

/* protos */
ESR_ReturnCode process_single_key_line(SR_Grammar* grammar, PFile* fin, PFile* fout);
ESR_ReturnCode process_multi_key_line(SR_Grammar* grammar, const LCHAR* rootrule, PFile* fin, PFile* fout);

/* struct */
typedef struct Opts
{
  int use_parse_by_string_ids;
  int do_check_all_ids;
}
Opts;

int usage(LCHAR* exename)
{
  pfprintf(PSTDOUT, "usage: %s -base <basefilename> [-in <input file>] [-out <output file>] [-itest <testfilename>]\n", exename);
  return 1;
}

void lstr_strip_multiple_spaces(LCHAR* trans)
{
  char *src=trans, *dst=trans;
  for( ;(*dst = *src)!=L('\0'); src++) {
    if(*dst != ' ') dst++;
    else if(src[1] != ' ') dst++;
  }
}

/**
 * Display the Semantic Result
 */
void display_results(SR_SemanticResult *result, PFile* fout)
{
  size_t i, size, len;
  LCHAR* keys[MAX_KEYS]; /* array of pointers to strings */
  LCHAR  value[MAX_STR_LENGTH];
  ESR_ReturnCode rc;
  
  size = MAX_KEYS;
  rc = result->getKeyList(result, (LCHAR**) & keys, &size); /* get the key list */
  if (rc == ESR_SUCCESS)
  {
    for (i = 0; i < size; i++)
    {
      len = MAX_STR_LENGTH;
      if ((rc = result->getValue(result, keys[i], value, &len)) == ESR_SUCCESS)
        pfprintf(fout, "{%s : %s}\n", keys[i], value);
      else
        pfprintf(fout, "Error: %s\n", ESR_rc2str(rc));
    }
    pfprintf(fout, "--Done--\n");
  }
  else
    pfprintf(fout, "Error: %s\n", ESR_rc2str(rc));
}

ESR_ReturnCode Parse(SR_Grammar* grammar, LCHAR* trans, PFile* fout, Opts* opts)
{
  ESR_ReturnCode rc = ESR_SUCCESS;
  size_t i, result_count, key_count;
  SR_SemanticResult* semanticResults[MAX_SEM_RESULTS];
  wordID wordIDs[32], *wordIDptr;
  SR_GrammarImpl* pgrammar = (SR_GrammarImpl*)grammar;
  wordmap* wmap;
  
  if (opts->do_check_all_ids)
  {
    wordID id;
    Opts myopts;
    memcpy(&myopts, opts, sizeof(myopts));
    myopts.do_check_all_ids = 0;
    wmap = pgrammar->syntax->synx->olabels;
    /* start at word 4 because "eps, -pau- -pau2- @root */
    for (id = 4; id < wmap->num_words; id++)
    {
      trans = wmap->words[id];
      Parse(grammar, trans, fout, &myopts);
    }
    return 0;
  }
  
  result_count = MAX_SEM_RESULTS; /* initially not greater than MAX */
  for (i = 0; i < result_count; i++)
    SR_SemanticResultCreate(&semanticResults[i]); /* create the result holders */
  lstrtrim(trans);
  /* check for multiple space separators! */
  lstr_strip_multiple_spaces(trans);
  
  if (!opts->use_parse_by_string_ids)
  {
    rc = grammar->checkParse(grammar, trans, semanticResults, (size_t*) & result_count);
  }
  else
  {
    char copy_of_trans[256], *p;
    strcpy(copy_of_trans, trans);
    wmap = pgrammar->syntax->synx->olabels;
    wordIDs[0] = wordIDs[1] = MAXwordID;
    wordIDptr = &wordIDs[0];
    for (p = strtok(copy_of_trans, " "); p; p = strtok(NULL, " "))
    {
      for (i = 0; i < wmap->num_words; i++)
        if (!strcmp(wmap->words[i], p))
        {
          *wordIDptr++ = (wordID)i;
          break;
        }
      if (i == wmap->num_words)
      {
        wordIDs[0] = MAXwordID;
        break;
      }
    }
    *wordIDptr++ = MAXwordID;
    
    /* printf("wordids:");
       for(wordIDptr=&wordIDs[0]; *wordIDptr!=MAXwordID; wordIDptr++) 
       printf(" %d/%s", *wordIDptr, wmap->words[*wordIDptr]);
       printf("\n"); */
    
    if (wordIDs[0] == MAXwordID)
    {
      result_count = 0;
      rc = ESR_SUCCESS;
    }
    else
    {
      rc = pgrammar->semproc->flush(pgrammar->semproc);
      rc = pgrammar->semproc->setParam(pgrammar->semproc, L("literal"), trans);
      rc = pgrammar->semproc->checkParseByWordID(pgrammar->semproc, pgrammar->semgraph,
           wordIDs, semanticResults, &result_count);
    }
  }
  if (rc != ESR_SUCCESS)
  {
    pfprintf(fout, "error (%s)\n\n", trans);
    return rc;
  }
  
  if (result_count < 1)
  {
    pfprintf(fout, "no parse (%s)\n\n", trans);
  }
  else
  {
    key_count = 0xffff;
    rc = SR_SemanticResultGetKeyCount(semanticResults[0], &key_count);
    pfprintf(fout, "parse ok (%d results) (%s) (%d)\n", result_count, trans, key_count);
    for (i = 0; i < result_count; i++)
      display_results(semanticResults[i], fout);
      
    for (i = 0; i < MAX_SEM_RESULTS; i++)
    {
      rc = semanticResults[i]->destroy(semanticResults[i]);
      if (rc != ESR_SUCCESS)
        return rc;
    }
  }
  return ESR_SUCCESS;
}

/* tests the transcription against the grammar and then decided based on what was expected of the test
whether or not is it considered a pass or fail */
ESR_ReturnCode ParseTestSet(SR_Grammar* grammar, LCHAR* trans, LCHAR* key, LCHAR* ref, LCHAR* result, PFile* fout)
{
  size_t len;
  ESR_ReturnCode rc;
  int i, result_count;
  SR_SemanticResult* semanticResults[MAX_SEM_RESULTS];
  LCHAR  value[MAX_STR_LENGTH];
  
  result_count = MAX_SEM_RESULTS;
  for (i = 0; i < result_count; i++)
    SR_SemanticResultCreate(&semanticResults[i]);
    
  lstrtrim(trans);
  /* check for multiple space separators! */
  lstr_strip_multiple_spaces(trans);
  
  pfprintf(fout, "checking (%s) ref(%s) res(%s)\n", trans, ref, result);
  rc = grammar->checkParse(grammar, trans, semanticResults, (size_t*) & result_count);
  if (rc != ESR_SUCCESS)
    return rc;
    
  /*result file will contain
  transcription | key | reference | result | PASSESD/FAILED */
  
  if (result_count < 1) /*failed to parse, but this could still be a pass if you expected a failure*/
  {
    pfprintf(fout, "NO PARSE FOR: %s|%s|%s|  |", trans, key, ref);
    if (strcmp("FAIL", result) == 0)
      pfprintf(fout, "PASSED (%s)\n", trans);
    else
      pfprintf(fout, "FAILED (%s)\n", trans);
  }
  else /*parsed, look at what was expected, what was returned and which of PASS/FAIL is expected */
  {
    for (i = 0; i < result_count; i++)
    {
      len = MAX_STR_LENGTH;
      if ((rc = semanticResults[i]->getValue(semanticResults[i], key, value, &len)) == ESR_SUCCESS)
      {
        pfprintf(fout, "%s|%s|%s|%s|", trans, key, ref, value);
        
        if (strcmp(value, ref) == 0 && strcmp("PASS", result) == 0)
          pfprintf(fout, "PASSED\n");
        else
          pfprintf(fout, "FAILED\n");
      }
      else
      {
        pfprintf(fout, "ERROR: %s, while checking key='%s'\n", ESR_rc2str(rc), key);
      }
    }
    
    /*deallocate semantic results*/
    for (i = 0; i < MAX_SEM_RESULTS; i++)
    {
      rc = semanticResults[i]->destroy(semanticResults[i]);
      if (rc != ESR_SUCCESS)
        return rc;
    }
  }
  return ESR_SUCCESS;
}

int main(int argc, char **argv)
{
  LCHAR trans[MAX_LINE_LENGTH];
  SR_Grammar* grammar = NULL;
  ESR_ReturnCode rc;
  LCHAR base[P_PATH_MAX] = L("");
  LCHAR infilename[P_PATH_MAX] = L("");
  LCHAR inRTfilename[P_PATH_MAX] = L("");
  LCHAR outfilename[P_PATH_MAX] = L("");
  PFile *fin = NULL, *fout = NULL;
  int i;
  LCHAR *rootrule = L("myRoot"), *p;
  Opts opts = { 0, 0 };
  
  /*
   * Initialize portable library.
   */
  CHKLOG(rc, PMemInit());

  fin = PSTDIN;
  fout = PSTDOUT;
  
  if (argc < 3)
  {
    usage(argv[0]);
    exit(EXIT_FAILURE);
  }
  for (i = 1; i < argc; ++i)
  {
    if (!LSTRCMP(argv[i], L("-base")))
    {
      ++i;
      LSTRCPY(base, argv[i]);
    }
    else if (!LSTRCMP(argv[i], L("-in")))
    {
      ++i;
      LSTRCPY(infilename, argv[i]);
    }
    else if (!LSTRCMP(argv[i], L("-out")))
    {
      ++i;
      LSTRCPY(outfilename, argv[i]);
    }
    else if (!LSTRCMP(argv[i], L("-itest")))
    {
      ++i;
      LSTRCPY(inRTfilename, argv[i]);
    }
    else if (!LSTRCMP(argv[i], L("-ids")))
    {
      opts.use_parse_by_string_ids = 1;
    }
    else if (!LSTRCMP(argv[i], L("-allids")))
    {
      opts.do_check_all_ids = 1;
      opts.use_parse_by_string_ids = 1;
    }
    else
      return usage(argv[0]);
  }
  
  CHK(rc, PLogInit(NULL, 0));
    
  rc = SR_GrammarLoad(base, &grammar);
  if (rc != ESR_SUCCESS)
    goto CLEANUP;
    
  if (*outfilename)
  {
    if ((fout = pfopen(outfilename, "w")) == NULL)
    {
      pfprintf(PSTDOUT, "Could not open file: %s\n", outfilename);
      rc = 1;
      goto CLEANUP;
    }
  }
  
  if (opts.do_check_all_ids)
  {
    rc = Parse(grammar, NULL, fout, &opts);
  }
  else if (*infilename)
  {
    if (LSTRCMP(infilename, "-") == 0)
    {
      fin = PSTDIN;
    }
    else if ((fin = pfopen(infilename, "r")) == NULL)
    {
      pfprintf(PSTDOUT, "Could not open file: %s\n", infilename);
      rc = 1;
      goto CLEANUP;
    }
    for (;;)
    {
      if (pfgets(trans, MAX_LINE_LENGTH, fin) == NULL)
      {
        if (!pfeof(fin))
        {
          rc = ESR_READ_ERROR;
          PLogError(ESR_rc2str(rc));
        }
        break;
      }
      if (trans[0] == '#') continue;
      lstrtrim(trans);
      /* check for multiple space separators! */
      lstr_strip_multiple_spaces(trans);
      pfprintf(fout, "Transcription: %s\n", trans);
      if ((rc = Parse(grammar, trans, fout, &opts)) != ESR_SUCCESS)
        goto CLEANUP;
      pfprintf(fout, "\n");
    }
  }
  else if (*inRTfilename) /*using a test file*/
  {
    if ((fin = pfopen(inRTfilename, "r")) == NULL)
    {
      pfprintf(PSTDOUT, "Could not open test file: %s\n", inRTfilename);
      rc = 1;
      goto CLEANUP;
    }
    
    /*read through the test file parsing it into the variables
     FORMAT: "the transciption" key "value" 
    */
    while (ESR_TRUE)
    {
      if (0) rc = process_single_key_line(grammar, fin, fout);
      else  rc = process_multi_key_line(grammar, rootrule, fin, fout);
      if (rc == ESR_READ_ERROR)
      {
        rc = ESR_SUCCESS;
        break;
      }
    }
  }
  else
  {
    /* get some transcriptions from the user */
    pfprintf(PSTDOUT, "\nSemantic Parser Test Program for esr (Nuance Communicaitions, 2007)\n");
    pfprintf(PSTDOUT, "'qqq' to quit\n");
    
    while (ESR_TRUE)
    {
      pfprintf(PSTDOUT, "> ");

      if (!fgets(trans, MAX_LINE_LENGTH, PSTDIN))
        break;
      // remove trailing whitespace
      for(p=&trans[0]; *p!=0 && *p!='\n' && *p!='\r'; p++) {}
      *p=0;

      if (!LSTRCMP("qqq", trans))
        break;
      else
        if ((rc = Parse(grammar, trans, fout, &opts)) != ESR_SUCCESS)
          goto CLEANUP;
    }
  }
CLEANUP:
  if (fin && fin != PSTDIN)
    pfclose(fin);
  if (fout && fout != PSTDOUT)
    pfclose(fout);
  if (grammar) grammar->destroy(grammar);
  PLogShutdown();
/*  PANSIFileSystemDestroy();
  PFileSystemDestroy();*/
  PMemShutdown();
  return rc;
}

ESR_ReturnCode process_single_key_line(SR_Grammar* grammar, PFile* fin, PFile* fout)
{
  LCHAR* position;
  LCHAR line[MAX_LINE_LENGTH];
  LCHAR trans[MAX_LINE_LENGTH];
  LCHAR key[MAX_LINE_LENGTH];
  LCHAR refValue[MAX_LINE_LENGTH];
  LCHAR result[MAX_LINE_LENGTH];
  ESR_ReturnCode rc;
  
  position = pfgets(line, MAX_LINE_LENGTH, fin);
  if (line[0] == '#')
    return ESR_SUCCESS;
  if (!strncmp(line, "__END__", 7))
    return ESR_READ_ERROR;
  if (position == NULL)
  {
    if (pfeof(fin))
      return ESR_READ_ERROR;
    else
    {
      PLogError(L("ESR_READ_ERROR"));
      return ESR_READ_ERROR;
    }
  }
  
  //get the transcription to test
  if ((position = strtok(line, "\"")) != NULL)
  {
    LSTRCPY(trans, position);
  }
  else
  {
    pfprintf(fout, "INVALID FORMAT for input line 1 \n");
    rc = ESR_INVALID_ARGUMENT;
    goto CLEANUP;
  }
  
  //get the key (meaning)
  if ((position = strtok(NULL, " \t")) != NULL)
  {
    LSTRCPY(key, position);
  }
  else
  {
    pfprintf(fout, "INVALID FORMAT for input line 2\n");
    rc = ESR_INVALID_ARGUMENT;
    goto CLEANUP;
  }
  
  //get the expected return string
  if ((position = strtok(NULL, "\"")) != NULL)
  {
    LSTRCPY(refValue, position);
  }
  else
  {
    pfprintf(fout, "INVALID FORMAT for input line 3\n");
    rc = ESR_INVALID_ARGUMENT;
    goto CLEANUP;
  }
  
  //get the expected result PASS/FAIL
  //there is no need to write PASS, if nothing is written PASS is assumed
  if ((position = strtok(NULL, " \t\r\n\"")) != NULL)
  {
    LSTRCPY(result, position);
    
    if (strcmp(result, "PASS") != 0 && strcmp(result, "FAIL") != 0)
    {
      pfprintf(fout, "INVALID FORMAT for input line, use either PASS or FAIL\n");
      rc = ESR_INVALID_ARGUMENT;
      goto CLEANUP;
    }
    
    if ((rc = ParseTestSet(grammar, trans, key, refValue, result, fout)) != ESR_SUCCESS)
      goto CLEANUP;
  }
  else
  {
    if ((rc = ParseTestSet(grammar, trans, key, refValue, "PASS", fout)) != ESR_SUCCESS)
      goto CLEANUP;
  }
  rc = ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode process_multi_key_line(SR_Grammar* grammar, const LCHAR* rootrule, PFile* fin, PFile* fout)
{
  LCHAR *position, *p;
  LCHAR line[MAX_LINE_LENGTH];
  LCHAR trans[MAX_LINE_LENGTH];
  LCHAR keyvals[MAX_LINE_LENGTH];
  ESR_ReturnCode rc;
  SR_SemanticResult* semanticResults[MAX_SEM_RESULTS];
  LCHAR refkey[MAX_LINE_LENGTH];
  LCHAR refval[MAX_LINE_LENGTH], value[MAX_STR_LENGTH];
  size_t i, j, len;
  size_t result_count;
  
  position = pfgets(line, MAX_LINE_LENGTH, fin);
  if (line[0] == '#')
    return ESR_SUCCESS;
  if (!strncmp(line, "__END__", 7))
    return ESR_READ_ERROR;
  if (position == NULL)
  {
    if (pfeof(fin))
      return ESR_READ_ERROR;
    else
    {
      PLogError(L("ESR_READ_ERROR"));
      return ESR_READ_ERROR;
    }
  }
  
  /* we're trying to parse
    Hello there : BONJOUR
   */
  p = strtok(line, ":");
  LSTRCPY(trans, p);
  /* strip trailing spaces */
  for (len = strlen(trans); len > 0 && trans[len-1] == ' '; len--)
    trans[len-1] = 0;
    
  p = strtok(NULL, "\n\r");
  /* strip leading spaces */
  while (*p == ' ' || *p == '\t')  p++;
  LSTRCPY(keyvals, p);
  
  result_count = MAX_SEM_RESULTS;
  for (i = 0; i < result_count; i++)
    SR_SemanticResultCreate(&semanticResults[i]);
    
  /* pfprintf(fout,"checking (%s) ref(%s)\n", trans, keyvals); */
  rc = grammar->checkParse(grammar, trans, semanticResults, (size_t*) & result_count);
  if (rc != ESR_SUCCESS)
    return rc;
    
  /*result file will contain
  transcription | key | reference | result | PASSESD/FAILED */
  
  if (result_count < 1) /*failed to parse, but this could still be a pass if you expected a failure*/
  {
    pfprintf(fout, "%s|%s|  |", trans, keyvals);
    if (!strcmp("FAIL", keyvals) || !strcmp(keyvals, "-"))
      pfprintf(fout, "PASSED\n");
    else
      pfprintf(fout, "FAILED\n");
  }
  else /*parsed, look at what was expected, what was returned and which of PASS/FAIL is expected */
  {
    size_t size, len;
    LCHAR* keys_available[MAX_KEYS]; /* array of pointers to strings */
    size = MAX_KEYS;
    rc = semanticResults[0]->getKeyList(semanticResults[0], (LCHAR**) & keys_available, &size);
    
    for (p = strtok(keyvals, ";"); p; p = strtok(NULL, ";"))
    {
      sprintf(refkey, "%s.%s", rootrule, p);
      p = strchr(refkey, '=');
      assert(p);
      *p = 0;
      p++;
      if (*p == '\'') p++;
      LSTRCPY(refval, p);
      if (refval[ strlen(refval)-1] == '\'') refval[strlen(refval)-1] = 0;
      
      for (i = 0; i < result_count; i++)
      {
        len = MAX_STR_LENGTH;
        for (j = 0; j < size; j++)
          if (!strcmp(keys_available[j], refkey)) break;
        if (j < size)
          rc = semanticResults[i]->getValue(semanticResults[i], refkey, value, &len);
        else
        {
          LSTRCPY(value, "<NOSUCHKEY>");
          rc = ESR_NO_MATCH_ERROR;
        }
        pfprintf(fout, "%s|%s|%s|%s|", trans, refkey, refval, value);
        if (strcmp(value, refval) == 0)
          pfprintf(fout, "PASSED\n");
        else
          pfprintf(fout, "FAILED\n");
      }
    }
    
    /*deallocate semantic results*/
    for (i = 0; i < MAX_SEM_RESULTS; i++)
    {
      rc = semanticResults[i]->destroy(semanticResults[i]);
      if (rc != ESR_SUCCESS)
        PLogError("%s while destroying", ESR_rc2str(rc));
    }
  }
  return ESR_SUCCESS;
}

