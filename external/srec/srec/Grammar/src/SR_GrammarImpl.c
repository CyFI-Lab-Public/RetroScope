/*---------------------------------------------------------------------------*
 *  SR_GrammarImpl.c  *
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

#include "ESR_Session.h"
#include "SR_AcousticModels.h"
#include "SR_AcousticModelsImpl.h"
#include "SR_Grammar.h"
#include "SR_GrammarImpl.h"
#include "SR_SemanticGraphImpl.h"
#include "SR_SemanticProcessorImpl.h"
#include "SR_VocabularyImpl.h"
#include "SR_NametagImpl.h"
#include "passert.h"
#include "plog.h"
#include "pmemory.h"

#define MTAG NULL

ESR_ReturnCode SR_Grammar_Create(SR_Grammar** self)
{
  SR_GrammarImpl* impl;
  ESR_ReturnCode rc;
  ESR_BOOL exists;
  
  impl = NEW(SR_GrammarImpl, MTAG);
  if (impl == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }
  
  impl->Interface.addNametagToSlot = &SR_Grammar_AddNametagToSlot;
  impl->Interface.addWordToSlot = &SR_Grammar_AddWordToSlot;
  impl->Interface.checkParse = &SR_Grammar_CheckParse;
  impl->Interface.compile = &SR_Grammar_Compile;
  impl->Interface.destroy = &SR_Grammar_Destroy;
  impl->Interface.getParameter = &SR_Grammar_GetParameter;
  impl->Interface.getSize_tParameter = &SR_Grammar_GetSize_tParameter;
  impl->Interface.resetAllSlots = &SR_Grammar_ResetAllSlots;
  impl->Interface.save = &SR_Grammar_Save;
  impl->Interface.setDispatchFunction = &SR_Grammar_SetDispatchFunction;
  impl->Interface.setParameter = &SR_Grammar_SetParameter;
  impl->Interface.setSize_tParameter = &SR_Grammar_SetSize_tParameter;
  impl->Interface.setupRecognizer = &SR_Grammar_SetupRecognizer;
  impl->Interface.unsetupRecognizer = &SR_Grammar_UnsetupRecognizer;
  impl->Interface.setupVocabulary = &SR_Grammar_SetupVocabulary;
  impl->syntax = NULL;
  impl->recognizer = NULL;
  impl->vocabulary = NULL;
  impl->eventLog = NULL;
  impl->logLevel = 0;
  impl->isActivated = ESR_FALSE;
  
  CHKLOG(rc, ESR_SessionTypeCreate(&impl->parameters));
  
  /**
   * Create the Semantic Graph and Processor to support CheckParse function
   * (Since this function gets called by 'New', a semgraph and semproc are always
   * created when the grammar is created)
   */
  rc = SR_SemanticGraphCreate(&impl->semgraph);
  if (rc != ESR_SUCCESS)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  
  rc = SR_SemanticProcessorCreate(&impl->semproc);
  if (rc != ESR_SUCCESS)
  {
    PLogError(ESR_rc2str(rc));
    goto CLEANUP;
  }
  
  CHKLOG(rc, ESR_SessionExists(&exists));
  if (exists)
  {
    rc = ESR_SessionGetProperty(L("eventlog"), (void **)&impl->eventLog, TYPES_SR_EVENTLOG);
    if (rc != ESR_NO_MATCH_ERROR && rc != ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
    rc = ESR_SessionGetSize_t(L("SREC.Recognizer.osi_log_level"), &impl->logLevel);
    if (rc != ESR_NO_MATCH_ERROR && rc != ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }
  }
  
  *self = (SR_Grammar*) impl;
  return ESR_SUCCESS;
CLEANUP:
  FREE(impl);
  return rc;
}

ESR_ReturnCode SR_GrammarCreate(SR_Grammar** self)
{
  ESR_ReturnCode rc;
  
  if (self == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }
  CHKLOG(rc, SR_Grammar_Create(self));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_Grammar_Compile(SR_Grammar* self)
{
  SR_GrammarImpl* impl = (SR_GrammarImpl*) self;
  
  if (!CA_CompileSyntax(impl->syntax))
    return ESR_SUCCESS;
  PLogError(L("ESR_FATAL_ERROR"));
  return ESR_FATAL_ERROR;
}

/*
 * The buffer for the pron is set very large because the real size is lost later on
 * and all that is checked is whether a single phoneme will fit in the buffer. There
 * is no concept of decrementing the bytes left. Because that code is one big monolithic
 * piece of crap, it is very difficult to fix correctly. This kludge is appropriate
 * because we don't have time to fix this correctly and there are probably dozens of
 * similar problems in other parts of the code.
 */

ESR_ReturnCode SR_Grammar_AddWordToSlot(SR_Grammar* self, const LCHAR* slot, const LCHAR* word, 
																				const LCHAR* pronunciation, int weight, const LCHAR* tag)
{
  SR_GrammarImpl* impl = (SR_GrammarImpl*) self;
  SR_Vocabulary* vocab;
  LCHAR buffer[4096];
  const LCHAR* input_pronunciation = pronunciation;
  size_t len = 4096;
  ESR_ReturnCode rc = ESR_SUCCESS, logrc;
  int ca_rc = -99;

  if ( slot != NULL )
  {
    if ( strlen ( slot ) >= MAX_STRING_LEN )
    {
    PLogError ( "SR_Grammar_AddWordToSlot slot : %s too long : Max %d", slot, MAX_STRING_LEN - 1 );
    return ( ESR_INVALID_ARGUMENT );
    }
  }
  if ( word != NULL )
  {
    if ( strlen ( word ) >= MAX_STRING_LEN )
    {
    PLogError ( "SR_Grammar_AddWordToSlot word : %s too long : Max %d", word, MAX_STRING_LEN - 1 );
    return ( ESR_INVALID_ARGUMENT );
    }
  }
  if ( pronunciation != NULL )
  {
    if ( strlen ( pronunciation ) >= MAX_STRING_LEN )
    {
    PLogError ( "SR_Grammar_AddWordToSlot pronunciation : %s too long : Max %d", pronunciation, MAX_STRING_LEN  - 1 );
    return ( ESR_INVALID_ARGUMENT );
    }
  }
  if ( tag != NULL )
  {
    if ( strlen ( tag ) >= MAX_STRING_LEN )
    {
    PLogError ( "SR_Grammar_AddWordToSlot tag : %s too long : Max %d", tag, MAX_STRING_LEN - 1 );
    return ( ESR_INVALID_ARGUMENT );
    }
  }
#if 0
  /* make sure to have the latest arbdata to add words, however since
     the arbdata is known to be constant for all acoustic models we 
	 have (ie for the different sample rates), then there is no need
	 to do this, it slows down addition anyways */
  CA_Arbdata* ca_arbdata; 
  SR_AcousticModels* models;
  impl->recognizer->getModels( impl->recognizer, &models);
  ca_arbdata = models->GetArbdata(models);
  CA_AttachArbdataToSyntax( impl->syntax , ca_arbdata);
#endif

  /* yw HACK: Xanavi's application has bug. remove this check to let it work */
  /* TODO: add this word to the semantic graph with associated script tag */
  if (impl->vocabulary == NULL)
  {
    PLogError(L("ESR_INVALID_STATE"));
    return ESR_INVALID_STATE;
  }
  
  /* tag may be NULL which means no script (no-op denoted by a simple semi-colon) */
  if (!tag || !*tag)
    tag = L(";");
    
  if (!pronunciation || !(*pronunciation) || !LSTRCMP(pronunciation, L("NULL")))
  {
    vocab = (SR_Vocabulary*) impl->vocabulary;
    CHKLOG(rc, vocab->getPronunciation(vocab, word, buffer, &len));
    pronunciation = buffer;
  }
  
  /*
   * 'buffer' contains a list of null-terminated pronunciations.
   * Two consecutive null characters denote the end of the list.
   *
   * (In theory yes, but right now, only one pron is supported)
   */
  if (impl->eventLog != NULL)
  {
    CHKLOG(logrc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->logLevel, L("igrm"), (int)impl));
    CHKLOG(logrc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("SLOT"), slot));
    CHKLOG(logrc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("WORD"), word));
    if (input_pronunciation)
      CHKLOG(logrc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("PRON"), pronunciation));
    else
      CHKLOG(logrc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("GPRON"), pronunciation));
    CHKLOG(logrc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->logLevel, L("WEIGHT"), weight));
    CHKLOG(logrc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("TAG"), tag));
  }
  
  /* add word to syntax first */
  /*
   *
   * if word already exists and pron is same (i.e. as if no action)               returns FST_SUCCESS
   * if word already exists and pron is different (e.g. read-rEd and read-red)    returns FST_SUCCESS
   * if word does not exist and no duplicate pron exists (homonyms not supported) returns FST_SUCCESS
   *                                                                                 else FST_FAILED
   */
  ca_rc = CA_AddWordToSyntax(impl->syntax, slot, word, pronunciation, weight);
  switch (ca_rc)
  {
    case FST_SUCCESS:
      /* successful, now add word & tag to semgraph */
      CHKLOG(rc, impl->semgraph->addWordToSlot(impl->semgraph, slot, word, tag, 1));
      break;
    case FST_SUCCESS_ON_OLD_WORD:
    case FST_FAILED_ON_HOMOGRAPH:
      /* successful, now add word & tag to semgraph */
      CHKLOG(rc, impl->semgraph->addWordToSlot(impl->semgraph, slot, word, tag, 0));
      break;
    case FST_FAILED_ON_MEMORY:
      rc = ESR_OUT_OF_MEMORY;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    case FST_FAILED_ON_INVALID_ARGS:
      rc = ESR_INVALID_ARGUMENT;
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    case FST_FAILED_ON_HOMONYM:
      rc = ESR_NOT_SUPPORTED;
      /* remove this message from product */
#if !defined(NDEBUG) || defined(_WIN32)
      PLogError(L("%s: Homonym '%s' could not be added"), ESR_rc2str(rc), word);
#endif
      goto CLEANUP;
    default:
      rc = ESR_INVALID_STATE;
      PLogError(L("%s|%s|%s|ca_rc=%d"), word, pronunciation, ESR_rc2str(rc), ca_rc);
      goto CLEANUP;
  }
  
  if (impl->eventLog != NULL && (impl->logLevel & OSI_LOG_LEVEL_ADDWD))
  {
    CHKLOG(logrc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->logLevel, L("caRC"), (int) ca_rc));
    CHKLOG(logrc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("RSLT"), L("ok")));
    CHKLOG(logrc, SR_EventLogEvent_BASIC(impl->eventLog, impl->logLevel, L("ESRaddWd")));
  }
  return rc;
CLEANUP:
  PLogError(L("failed on |%s|%s|%s|\n"), slot, word, pronunciation);
  if (impl->eventLog != NULL && (impl->logLevel & OSI_LOG_LEVEL_ADDWD))
  {
    CHKLOG(logrc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->logLevel, L("caRC"), (int) ca_rc));
    CHKLOG(logrc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("RSLT"), L("err1")));
    CHKLOG(logrc, SR_EventLogEvent_BASIC(impl->eventLog, impl->logLevel, L("ESRaddWd")));
  }
  return rc;
}

ESR_ReturnCode SR_Grammar_ResetAllSlots(SR_Grammar* self)
{
  ESR_ReturnCode rc, logrc;
  int irc;
  SR_GrammarImpl* impl = (SR_GrammarImpl*) self;
  
  rc = impl->semgraph->reset(impl->semgraph);
  if (rc == ESR_SUCCESS)
  {
    irc = CA_ResetSyntax(impl->syntax);
    rc = irc ? ESR_INVALID_STATE : ESR_SUCCESS;
  }
  
  if (impl->eventLog != NULL)
  {
    CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->logLevel, L("igrm"), (int)impl));
    if (rc == ESR_SUCCESS)
      CHKLOG(logrc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("RSLT"), L("ok")));
    else
      CHKLOG(logrc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("RSLT"), L("fail")));
    CHKLOG(logrc, SR_EventLogEvent_BASIC(impl->eventLog, impl->logLevel, L("ESRrstSlot")));
  }
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_Grammar_AddNametagToSlot(SR_Grammar* self, const LCHAR* slot, 
                                           const SR_Nametag* nametag, int weight, const LCHAR* tag)
{
  SR_NametagImpl* nametagImpl = (SR_NametagImpl*) nametag;
  ESR_ReturnCode rc;
  
  CHKLOG(rc, self->addWordToSlot(self, slot, nametagImpl->id, nametagImpl->value, weight, tag));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_Grammar_SetDispatchFunction(SR_Grammar* self, const LCHAR* functionName, void* userData, SR_GrammarDispatchFunction function)
{
  SR_GrammarImpl* impl = (SR_GrammarImpl*) self;
  ESR_ReturnCode rc;
  SR_SemanticProcessorImpl* semprocImpl = (SR_SemanticProcessorImpl*) impl->semproc;
  
  CHKLOG(rc, EP_RegisterFunction(semprocImpl->parser, functionName, userData, function));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_GrammarLoad(const LCHAR* grammar, SR_Grammar** self)
{
  SR_Grammar* Interface = NULL;
  SR_GrammarImpl* impl;
  LCHAR* tok;
  ESR_ReturnCode rc;
  LCHAR filename[P_PATH_MAX];
  int addWords;
  
  
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  CHKLOG(rc, SR_Grammar_Create(&Interface));
  impl = (SR_GrammarImpl*) Interface;
  
  /**
   * Our filename (referring to the grammar to load, may have associated grammar properties
   * appended to the end of it. We need to split up the properties from the filename
   * example:
   *  recog_nm/namesnnumsSC_dyn,addWords=2000 becomes
   *    filename: recog_nm/namesnnumsSC_dyn
   *    property: addWords=2000
   */
  
  /* init */
  LSTRCPY(filename, grammar);
  addWords = 0;
  
  for (tok = strtok(filename, ","); tok; tok = strtok(NULL, ","))
  {
    if (LSTRSTR(tok, "addWords"))
    {
      addWords = atoi(LSTRCHR(tok, L('=')) + sizeof(LCHAR));
    }
    else if (tok != filename)
    {
      PLogError(L("UNKNOWN grammar load property %s"), tok);
      rc = ESR_INVALID_STATE;
      goto CLEANUP;
    }
  }
  
  /**
   * Based on the filename, determine if you are loading from image or loading from text files.
   * If from image, then the filename will have extension .g2g. If from file, then only a basename
   * will be provided (i.e. without extension)
   */
  
  impl->syntax = CA_AllocateSyntax();
  if (impl->syntax == NULL)
  {
    rc = ESR_OUT_OF_MEMORY;
    goto CLEANUP;
  }
  
  if (LSTRSTR(filename, L(".g2g")))
  {
    /* if the filename ends with .g2g, then we have a binary image */
    if (CA_LoadSyntaxFromImage(impl->syntax, (LCHAR*) filename))
    {
      rc = ESR_READ_ERROR;
      PLogError(L("ESR_READ_ERROR: Problem loading syntax from image"));
      goto CLEANUP;
    }
  }
  else
  {
    if (CA_LoadSyntaxAsExtensible(impl->syntax, (LCHAR*) filename, addWords))
    {
      rc = ESR_READ_ERROR;
      PLogError(L("ESR_READ_ERROR: Problem loading syntax from text"));
      goto CLEANUP;
    }
  }
  
  /*
   * Semantic Graph Loading
   *
   * - it was already created in Grammar_Create()
   * - reuse the same input labels from the recognition context
   * - load knows how to load from base filename or .g2g image
   */
  rc = impl->semgraph->load(impl->semgraph, impl->syntax->synx->olabels, filename, addWords);
  if (rc != ESR_SUCCESS)
  {
    PLogError(L("%s: loading semgraph from image %s"), ESR_rc2str(rc), filename);
    impl->semgraph = NULL;
    goto CLEANUP;
  }
  
  *self = Interface;
  if (impl->eventLog)
  {
    CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->logLevel, L("igrm"), (int)impl));
    CHKLOG(rc, SR_EventLogToken_BASIC(impl->eventLog, impl->logLevel, L("name"), filename));
    CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->logLevel, L("ESRldgrm")));
  }
  
  return ESR_SUCCESS;
CLEANUP:
  if (Interface != NULL)
    Interface->destroy(Interface);
  *self = NULL;
  return rc;
}

ESR_ReturnCode SR_Grammar_Save(SR_Grammar* self, const LCHAR* filename)
{
  SR_GrammarImpl* impl = (SR_GrammarImpl*) self;
  int version_number = 2;  

  if (filename == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  if (CA_DumpSyntaxAsImage(impl->syntax, filename, version_number)) /* returns 1 on failure */
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_STATE;
  }
  if (SR_SemanticGraph_Save(impl->semgraph, filename, version_number) != ESR_SUCCESS)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_STATE;
  }
  
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_Grammar_SetParameter(SR_Grammar* self, const LCHAR* key, void* value)
{
  /*TODO: complete with logging*/
  return ESR_NOT_IMPLEMENTED;
}

ESR_ReturnCode SR_Grammar_SetSize_tParameter(SR_Grammar* self, const LCHAR* key, size_t value)
{
  SR_GrammarImpl* impl = (SR_GrammarImpl*) self;
  size_t temp;
  ESR_ReturnCode rc;
  
  rc = impl->parameters->getSize_t(impl->parameters, key, &temp);
  if (rc == ESR_SUCCESS)
  {
    if (temp == value)
      return ESR_SUCCESS;
    CHKLOG(rc, impl->parameters->removeAndFreeProperty(impl->parameters, key));
  }
  else if (rc != ESR_NO_MATCH_ERROR)
    return rc;
    
  CHKLOG(rc, impl->parameters->setSize_t(impl->parameters, key, value));
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_Grammar_GetParameter(SR_Grammar* self, const LCHAR* key, void** value)
{

  /*TODO: complete with logging*/
  return ESR_NOT_IMPLEMENTED;
}

ESR_ReturnCode SR_Grammar_GetSize_tParameter(SR_Grammar* self, const LCHAR* key, size_t* value)
{
  SR_GrammarImpl* impl = (SR_GrammarImpl*) self;
  ESR_ReturnCode rc;
  
  if (!LSTRCMP(key, "locale"))
  {
    ESR_Locale locale;
    rc = SR_VocabularyGetLanguage(impl->vocabulary, &locale);
    if (rc != ESR_SUCCESS)
      return rc;
      
    *value = locale;
    return ESR_SUCCESS;
  }
  else
  {
    rc = impl->parameters->getSize_t(impl->parameters, key, value);
    if (rc == ESR_NO_MATCH_ERROR)
    {
      CHKLOG(rc, ESR_SessionGetSize_t(key, value));
      return ESR_SUCCESS;
    }
    if (rc != ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      return rc;
    }
    return ESR_SUCCESS;
  }
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_Grammar_Destroy(SR_Grammar* self)
{
  SR_GrammarImpl* impl = (SR_GrammarImpl*) self;
  ESR_ReturnCode rc;
  
  if (impl->parameters != NULL)
  {
    CHKLOG(rc, impl->parameters->destroy(impl->parameters));
    impl->parameters = NULL;
  }
  
  if (impl->syntax != NULL)
  {
    CA_FreeSyntax(impl->syntax);
    impl->syntax = NULL;
  }
  
  if (impl->semgraph != NULL)
  {
    CHKLOG(rc, impl->semgraph->unload(impl->semgraph));
    CHKLOG(rc, impl->semgraph->destroy(impl->semgraph));
    impl->semgraph = NULL;
  }
  
  if (impl->semproc != NULL)
  {
    CHKLOG(rc, impl->semproc->destroy(impl->semproc));
    impl->semproc = NULL;
  }
  
  if (impl->eventLog)
  {
    CHKLOG(rc, SR_EventLogTokenInt_BASIC(impl->eventLog, impl->logLevel, L("igrm"), (int)impl));
    CHKLOG(rc, SR_EventLogEvent_BASIC(impl->eventLog, impl->logLevel, L("ESRklgrm")));
  }
  
  FREE(self);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}

ESR_ReturnCode SR_Grammar_SetupRecognizer(SR_Grammar* self, SR_Recognizer* recognizer)
{
  ESR_ReturnCode rc;
  SR_GrammarImpl* impl = (SR_GrammarImpl*) self;
  CA_Arbdata* ca_arbdata;
  SR_AcousticModels* models = NULL;
  
  if (impl == NULL || recognizer == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  impl->recognizer  = recognizer;
  recognizer->setWordAdditionCeiling( recognizer, self);

  rc = recognizer->getModels( recognizer, &models);
  if(rc != ESR_SUCCESS || models == NULL) {
	  impl->recognizer = NULL;
	  CA_AttachArbdataToSyntax( impl->syntax, NULL);
	  return ESR_INVALID_STATE;
  } 
  ca_arbdata = (CA_Arbdata*)(models->getArbdata( models));
  rc = CA_AttachArbdataToSyntax( impl->syntax, ca_arbdata);
  if(rc != 0) 
	  return ESR_INVALID_STATE;
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_Grammar_UnsetupRecognizer(SR_Grammar* self)
{
  SR_GrammarImpl* impl = (SR_GrammarImpl*) self;
  if(impl == NULL) return ESR_INVALID_ARGUMENT;
  impl->recognizer  = NULL;
  CA_AttachArbdataToSyntax( impl->syntax, NULL);
  return ESR_SUCCESS;
}

SREC_GRAMMAR_API ESR_ReturnCode SR_Grammar_SetupVocabulary(SR_Grammar *self, SR_Vocabulary *vocabulary)
{
  SR_GrammarImpl* impl = (SR_GrammarImpl*) self;
  
  if (vocabulary == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  impl->vocabulary = vocabulary;
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_Grammar_CheckParse(SR_Grammar* self, const LCHAR* transcription, SR_SemanticResult** result, size_t* resultCount)
{
  ESR_ReturnCode rc;
  SR_GrammarImpl* impl = (SR_GrammarImpl*) self;
  size_t resultCountIn = *resultCount;
  
  if (transcription == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  
  /* NULL for special implementation when CheckParse is called by application that does not know
     about the hidden data structure SR_SemanticResult */
  if (result == NULL)
  {
    if (CA_CheckTranscription(impl->syntax, transcription, 0) == 0)
      *resultCount = 1;
    else
      *resultCount = 0;
    return ESR_SUCCESS;
  } 
  rc = impl->semproc->checkParse(impl->semproc, impl->semgraph, transcription, result, resultCount);
  if (*resultCount == 0)
  {
    /* get the literal that did parse from the text_parser.c code */
    char copy_of_trans[512];
    strcpy(copy_of_trans, transcription);
    *resultCount = resultCountIn;
    if (CA_CheckTranscription(impl->syntax, (LCHAR*)copy_of_trans, 0) == 0)
      rc = impl->semproc->checkParse(impl->semproc, impl->semgraph, copy_of_trans, result, resultCount);
  }
  return rc;
}

#define DISABLEcostdata 8192

ESR_ReturnCode SR_GrammarAllowOnly(SR_Grammar* self, const char* transcription)
{
  char copy_of[512], *word;
  int i, j;
  wordID wdids[32], nw = 0;
  SR_GrammarImpl* impl = (SR_GrammarImpl*)self;
  CA_Syntax* ca_syntax = impl->syntax;
  srec_context* fst = ca_syntax->synx;
  ESR_ReturnCode rc = ESR_SUCCESS;
  
  strcpy(copy_of, transcription);
  
  for (word = strtok(copy_of, " "); word; nw++, word = strtok(NULL, " "))
  {
    wdids[nw] =   wordmap_find_index(fst->olabels, word);
    if (wdids[nw] == MAXwordID)
      rc = ESR_NO_MATCH_ERROR;
  }
  
  for (i = 0; i < fst->num_arcs; i++)
  {
    wordID wdid = fst->FSMarc_list[i].olabel;
    if (wdid < EPSILON_OFFSET) ;
    else if (wdid == fst->beg_silence_word) ;
    else if (wdid == fst->end_silence_word) ;
    else
    {
      for (j = nw; --j >= 0;)
        if (wdid == wdids[j]) break;
      if (j < 0)
      {
        fst->FSMarc_list[i].cost |= DISABLEcostdata; /* disable this arc */
      }
      else
      {
        /* pfprintf(PSTDOUT, "enabling arc %d for %d %s\n",
           i, wdid, transcription); */
        fst->FSMarc_list[i].cost &= ~(DISABLEcostdata); /* enable this arc */
      }
    }
  }
  /* added, this way we prevent more failures due to dead ends */
  for (; ;)
  {
    FSMarc* arc;
    arcID j, counter = 0;
    nodeID node;
    costdata mincost;
    
    for (i = 0; i < fst->num_arcs; i++)
    {
      if (fst->FSMarc_list[i].cost < DISABLEcostdata)
      {
        node = fst->FSMarc_list[i].to_node;
        if (node == fst->end_node) continue;
        mincost = DISABLEcostdata;
        for (j = fst->FSMnode_list[node].un_ptr.first_next_arc; j != MAXarcID; j = arc->linkl_next_arc)
        {
          arc = &fst->FSMarc_list[j];
          if (arc->cost < mincost) mincost = arc->cost;
        }
        if (mincost >= DISABLEcostdata)
        {
          fst->FSMarc_list[i].cost |= DISABLEcostdata;
          counter++;
        }
      }
    }
    if (counter == 0) break;
  }
  
  return rc;
}

ESR_ReturnCode SR_GrammarAllowAll(SR_Grammar* self)
{
  int i;
  SR_GrammarImpl* impl = (SR_GrammarImpl*)self;
  CA_Syntax* ca_syntax = impl->syntax;
  srec_context* fst = ca_syntax->synx;
  ESR_ReturnCode rc = ESR_SUCCESS;
  
  for (i = 0; i < fst->num_arcs; i++)
  {
    fst->FSMarc_list[i].cost &= ~(DISABLEcostdata); /* enable this arc */
  }
  return rc;
}

