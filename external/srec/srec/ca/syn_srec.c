/*---------------------------------------------------------------------------*
 *  syn_srec.c  *
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

#include <stdlib.h>
#include <string.h>
#ifndef _RTT
#include <stdio.h>
#endif

#ifdef unix
#include <unistd.h>
#endif
#include <assert.h>

#ifndef _RTT
#include "duk_io.h"
#endif

#include "simapi.h"
#include "pendian.h"
#include "portable.h"
#include "srec_context.h"
#include "ESR_Session.h"

static void free_buffer(char* buffer, const char* allocname)
{
  FREE(buffer);
}

int CA_AttachArbdataToSyntax(CA_Syntax* syntax, CA_Arbdata* allotree)
{
  int rc;
  rc = FST_AttachArbdata(syntax->synx, (srec_arbdata*)allotree);
  return rc;
}

int CA_AddWordToSyntax(CA_Syntax* syntax, const char* slot,
                       const char *phrase, const char* pronunciation,
                       const int weight)
{
  int rc;
  rc = FST_AddWordToGrammar(syntax->synx, slot, phrase, pronunciation, weight);
  return rc;
}

int CA_ResetSyntax(CA_Syntax* syntax)
{
  int rc;
  rc = FST_ResetGrammar(syntax->synx);
  return rc;
}

int  CA_CompileSyntax(CA_Syntax *hSyntax)
{
  return FST_PrepareContext(hSyntax->synx);
}

int CA_LoadSyntaxAsExtensible(CA_Syntax *hSyntax, /*CA_Arbdata* arbdata,*/
                              char *synbase, int num_words_to_add)
{
  int rc;
  TRY_CA_EXCEPT
  ASSERT(hSyntax);
  if (hSyntax->setup_count > 0)
    SERVICE_ERROR(SYNTAX_ALREADY_SETUP);

  rc = FST_LoadContext(synbase, &hSyntax->synx, num_words_to_add);
  return rc ? 1 : 0;
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hSyntax)
}

/* this belongs part of the srec_context! */
/**
 * @todo document
 */
typedef struct
{
  asr_int32_t image_format;
  asr_int32_t image_size;
  asr_int32_t sizes_signature;
}
context_image_header;

int CA_LoadSyntaxFromImage(CA_Syntax *hSyntax, const LCHAR* filename)
{
  int result;
  PFile* fp = NULL;
  ESR_BOOL isLittleEndian;

  /*
#if __BYTE_ORDER==__LITTLE_ENDIAN
   isLittleEndian = ESR_TRUE;
#else
   isLittleEndian = ESR_FALSE;
#endif
  */
  isLittleEndian = ESR_TRUE;

  fp = pfopen ( filename, L("rb") );
/*  CHKLOG(rc, PFileSystemCreatePFile(filename, isLittleEndian, &fp));
  CHKLOG(rc, PFileOpen(fp, L("rb")));*/

  if ( fp == NULL )
    goto CLEANUP;

  result = FST_LoadContextFromImage(&hSyntax->synx, fp);
  pfclose(fp);
  return result ? 1 : 0;
CLEANUP:
  if (fp)
    pfclose (fp);
  return 1;
}

int CA_DumpSyntax(CA_Syntax* hSyntax, const char* basename)
{
  int result, totrc = 0;
  PFile* fp;
  char buf[256];
  /*ESR_ReturnCode rc;*/

  sprintf(buf, "%s.PCLG.txt", basename);
  fp = pfopen ( buf, L("wb") );
/*  CHKLOG(rc, PFileSystemCreatePFile(buf, ESR_TRUE, &fp));
  CHKLOG(rc, PFileOpen(fp, L("wb")));*/

  if ( fp == NULL )
    goto CLEANUP;

  result = FST_DumpGraph(hSyntax->synx, fp);
  pfclose(fp);
  totrc += result;

  sprintf(buf, "%s.map", basename);
  fp = pfopen ( buf, L("wb") );
/*  CHKLOG(rc, PFileSystemCreatePFile(buf, ESR_TRUE, &fp));
  CHKLOG(rc, PFileOpen(fp, L("wb")));*/

  if ( fp == NULL )
    goto CLEANUP;

  result = FST_DumpWordMap(fp, hSyntax->synx->olabels);
  pfclose(fp);
  totrc += result;

  sprintf(buf, "%s.Grev2.det.txt", basename);
  fp = pfopen ( buf, L("wb") );
/*  CHKLOG(rc, PFileSystemCreatePFile(buf, ESR_TRUE, &fp));
  CHKLOG(rc, PFileOpen(fp, L("wb")));*/

  if ( fp == NULL )
    goto CLEANUP;

  if (0)
    result = FST_DumpReverseWordGraph(hSyntax->synx, fp);
  pfclose(fp);
  totrc += result;

  return totrc ? 1 : 0;
CLEANUP:
  return 0;
}

int CA_DumpSyntaxAsImage(CA_Syntax* hSyntax, const char* imagename, int version_number)
{
  int result;
  PFile* fp;
  /*ESR_ReturnCode rc;*/
  ESR_BOOL isLittleEndian;

  isLittleEndian = ESR_TRUE;

  fp = pfopen ( imagename, L("wb") );
/*  CHKLOG(rc, PFileSystemCreatePFile(imagename, isLittleEndian, &fp));
  CHKLOG(rc, PFileOpen(fp, L("wb")));*/

  if ( fp == NULL )
    goto CLEANUP;

  if (version_number == 2)
  {
    result = FST_DumpContextAsImageV2(hSyntax->synx, fp);
  }
  else
  {
    PLogError("invalid version number %d\n", version_number);
    result = FST_FAILED_ON_INVALID_ARGS;
  }
  pfclose(fp);
  return result ? 1 : 0;
CLEANUP:
  return 0;
}

/* from syn_file.c */

CA_Syntax *CA_AllocateSyntax(void)
{
  CA_Syntax *hSyntax = NULL;
  TRY_CA_EXCEPT
  hSyntax = (CA_Syntax *) CALLOC_CLR(1, sizeof(CA_Syntax), "ca.hSyntax");
  hSyntax->has_groups = False;
  hSyntax->has_rules = False;
  hSyntax->setup_count = 0;
  hSyntax->ca_rtti = CA_SYNTAX_SIGNATURE;
  hSyntax->synx = 0;
  return (hSyntax);

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hSyntax)
}

void CA_FreeSyntax(CA_Syntax *hSyntax)
{
  TRY_CA_EXCEPT
  ASSERT(hSyntax);
  if (hSyntax->setup_count > 0)
    SERVICE_ERROR(SYNTAX_ALREADY_SETUP);
  /* todo: free hSyntax internals */
  FST_UnloadContext((srec_context*)(hSyntax->synx));
  hSyntax->synx = 0;
  FREE((char *) hSyntax);
  return;

  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hSyntax)
}

CA_Arbdata* CA_LoadArbdata(const char* filename)
{
  CA_Arbdata* ca_arbdata = NULL;
  int rc;

#ifdef SREC_ENGINE_VERBOSE_LOGGING
  PLogMessage(L("CA_LoadArbdata ... from file %s"), filename);
#endif
  rc = read_arbdata_from_stream((srec_arbdata**) & ca_arbdata, (char *)filename, 0);
  return ca_arbdata;
}

unsigned int CA_ArbdataGetModelVersionID(CA_Arbdata* ca_arbdata)
{
  return version_arbdata_models((srec_arbdata*)ca_arbdata);
}

int CA_ArbdataGetModelIdsForPron(CA_Arbdata* ca_arbdata, 
								 const char* pronunciation,  /* WB assumed at the edges */
								 int pronunciation_len,
								 modelID* pmodelIds)
{
	srec_arbdata *allotree = (srec_arbdata*)ca_arbdata;
	return get_modelids_for_pron( allotree,
                         pronunciation, pronunciation_len,
                          pmodelIds);
}

int CA_ArbdataGetModelIdsForPIC(CA_Arbdata* ca_arbdata, const char lphon, 
								const char cphon, 
								const char rphon)
{
  phonemeID lphon_ID, cphon_ID, rphon_ID;
  srec_arbdata *allotree = (srec_arbdata*)ca_arbdata;
  if(lphon==WBPHONEME_CODE){
#if !USE_WWTRIPHONE
    lphon_ID = (phonemeID)allotree->phoneme_index[ SILENCE_CODE];
#else
    lphon_ID = WBPHONEME_CODE; //(phonemeID)allotree->phoneme_index[ WBPHONEME_CODE];
#endif
  }
  else
    lphon_ID = (phonemeID)allotree->phoneme_index[(const unsigned char)lphon];
  cphon_ID = (phonemeID)allotree->phoneme_index[(const unsigned char)cphon];
  if(rphon==WBPHONEME_CODE){
#if !USE_WWTRIPHONE
    rphon_ID = (phonemeID)allotree->phoneme_index[ SILENCE_CODE];
#else
    rphon_ID = WBPHONEME_CODE; //(phonemeID)allotree->phoneme_index[ WBPHONEME_CODE];
#endif
  }
  else
    rphon_ID = (phonemeID)allotree->phoneme_index[(const unsigned char)rphon];
  return (modelID)get_modelid_for_pic(allotree, lphon_ID, cphon_ID, rphon_ID);
}

void CA_FreeArbdata(CA_Arbdata* ca_arbdata)
{
  free_buffer((char*)ca_arbdata, "srec.arbdata");
}
/* from syn_basi.c */

int CA_SetupSyntaxForRecognizer(CA_Syntax *hSyntax, CA_Recog *hRecog)
{
  int rc;
  const char* rule = "ROOT";
  rc = activate_grammar_for_recognition(hRecog->recm, hSyntax->synx, rule);
  return rc;
}

int CA_IsEnrollmentSyntax(CA_Syntax *hSyntax)
{
  return FST_IsVoiceEnrollment( hSyntax->synx);
}

int CA_CeilingSyntaxForRecognizer(CA_Syntax *hSyntax, CA_Recog *hRecog)
{
  if(!hSyntax || !hRecog) 
	  return 1;
  if(!hSyntax->synx || !hRecog->recm) 
	  return 1;
  hSyntax->synx->max_searchable_nodes = hRecog->recm->max_fsm_nodes;
  hSyntax->synx->max_searchable_arcs  = hRecog->recm->max_fsm_arcs;
  return 0;
}

/* checks if the phrase is in the grammar node, 0=in 1=not.in */
int CA_CheckTranscription(CA_Syntax *hSyntax, const char *transcription, int no)
{
  int rc;
  char literal[512];
  size_t max_literal_len = 512;
  srec_context* context = hSyntax->synx;
  rc = FST_CheckPath(context, transcription, literal, max_literal_len);
  if (rc == 0) strcpy((char*)transcription, literal);
  return rc;
}

/*---------------------------------------------------------------------------*
 *                                                                           *
 * do nothing functions                                                      *
 *                                                                           *
 *---------------------------------------------------------------------------*/

/* from syn_basi.c */


void CA_ClearSyntaxForRecognizer(CA_Syntax *hSyntax, CA_Recog *hRecog)
{
  int rc;
  hSyntax = NULL; /* not used */
  rc = clear_grammars_for_recognition(hRecog->recm);
  return;
}
