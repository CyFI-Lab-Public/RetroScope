/*---------------------------------------------------------------------------*
 *  VocabularyImpl.c                                                         *
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

#include "ESR_Session.h"
#include "SR_Vocabulary.h"
#include "SR_VocabularyImpl.h"
#include "passert.h"
#include "plog.h"
#include "ptypes.h"
#include "pmemory.h"

//#define DEBUG 1
#define MAX_PRON_LEN 256
#define MAX_WORD_LEN    40
#define MTAG NULL
#define MAX_PHONE_LEN 4
#define DO_DEFER_LOADING_UNTIL_LOOKUPS 1

static PINLINE LCHAR* get_first_word(LCHAR* curr, LCHAR* end);
static PINLINE LCHAR* get_next_word(LCHAR* curr, LCHAR* end);
static ESR_ReturnCode run_ttt(const LCHAR *input_sentence, LCHAR *output_sentence, int *text_length);

#define MAX_NUM_PRONS 4
#define LSTRDUP(src) LSTRCPY(CALLOC(LSTRLEN(src)+1, sizeof(LCHAR), "srec.Vocabulary.LSTRDUP"), (src))
#define LSTRFREE(src) FREE(src)

/**
 * Creates a new vocabulary but does not set the locale.
 *
 * @param self SR_Vocabulary handle
 */
#ifdef USE_TTP
ESR_ReturnCode SR_CreateG2P(SR_Vocabulary* self)
{
  ESR_ReturnCode      rc = ESR_SUCCESS;
  SWIsltsResult       res = SWIsltsSuccess;
  SR_VocabularyImpl * impl = (SR_VocabularyImpl*) self;
  LCHAR               szG2PDataFile[P_PATH_MAX];
  size_t              len = P_PATH_MAX;
  ESR_BOOL                bG2P = ESR_TRUE;

     rc = ESR_SessionGetBool ( L("G2P.Available"), &bG2P );
     if ( rc != ESR_SUCCESS )
       {
	 PLogError(L("ESR_FATAL_ERROR: ESR_SessionGetBool() - G2P.Available fails with return code %d\n"), rc);
	 return rc;
       }
     if ( bG2P == ESR_FALSE )
       {
	 impl->hSlts = NULL;
	 return ESR_SUCCESS;
       }

     rc = ESR_SessionGetLCHAR ( L("G2P.Data"), szG2PDataFile, &len );
     if ( rc != ESR_SUCCESS )
       {
	 PLogError(L("ESR_FATAL_ERROR: ESR_SessionGetLCHAR() - G2P.Data fails with return code %d\n"), rc);
	 return rc;
     }
     rc = ESR_SessionPrefixWithBaseDirectory(szG2PDataFile, &len);
     if ( rc != ESR_SUCCESS )
       {
	 PLogError(L("ESR_FATAL_ERROR: ESR_SessionPrefixWithBaseDirectory() - G2P.Data fails with return code %d\n"), rc);
	 return rc;
       }

     res = SWIsltsInit();
     if (res == SWIsltsSuccess)
       {
	 /* data_file: en-US-ttp.data */
	 res = SWIsltsOpen(&(impl->hSlts), szG2PDataFile);
	 if (res != SWIsltsSuccess)
	   {
	     PLogError(L("ESR_FATAL_ERROR: SWIsltsOpen( ) fails with return code %d\n"), res);
	     FREE(impl);
	     return ESR_FATAL_ERROR;
	   }
       }
     else
     {
       PLogError(L("ESR_FATAL_ERROR: SWIsltsInit( ) fails with return code %d\n"), res);
       FREE(impl);
       return ESR_FATAL_ERROR;
     }
     return rc;
}

ESR_ReturnCode SR_DestroyG2P(SR_Vocabulary* self)
{
  ESR_ReturnCode      rc = ESR_SUCCESS;
  SWIsltsResult       res = SWIsltsSuccess;
  SR_VocabularyImpl * impl = (SR_VocabularyImpl*) self;
  ESR_BOOL                bG2P = ESR_TRUE;

  rc = ESR_SessionGetBool ( L("G2P.Available"), &bG2P );
  if ( rc != ESR_SUCCESS )
     {
       PLogError(L("ESR_FATAL_ERROR: ESR_SessionGetBool() - G2P.Available fails with return code %d\n"), rc);
       return rc;
     }
  if ( bG2P == ESR_FALSE || impl->hSlts == NULL)
    {
      return ESR_SUCCESS;
    }

  res = SWIsltsClose(impl->hSlts);
  if (res == SWIsltsSuccess)
    {
      res = SWIsltsTerm();
      if (res != SWIsltsSuccess)
	{
	  PLogError(L("ESR_FATAL_ERROR: SWIsltsTerm( ) fails with return code %d\n"), res);
	  rc = ESR_FATAL_ERROR;
          }
    }
  else
    {
      PLogError(L("ESR_FATAL_ERROR: SWIsltsClose( ) fails with return code %d\n"), res);
      rc = ESR_FATAL_ERROR;
    }
  return rc;
}
#endif /* USE_TTP */

/**
 * Creates a new vocabulary but does not set the locale.
 *
 * @param self SR_Vocabulary handle
 */
ESR_ReturnCode SR_VocabularyCreateImpl(SR_Vocabulary** self)
{
  SR_VocabularyImpl* impl;

  if (self==NULL)
    {
      PLogError(L("ESR_INVALID_ARGUMENT"));
      return ESR_INVALID_ARGUMENT;
    }
  impl = NEW(SR_VocabularyImpl, MTAG);
  if (impl==NULL)
    {
      PLogError(L("ESR_OUT_OF_MEMORY"));
      return ESR_OUT_OF_MEMORY;
    }

  impl->Interface.save = &SR_VocabularySaveImpl;
  impl->Interface.getPronunciation = &SR_VocabularyGetPronunciationImpl;
     impl->Interface.getLanguage = &SR_VocabularyGetLanguageImpl;
     impl->Interface.destroy = &SR_VocabularyDestroyImpl;
     impl->vocabulary = NULL;

     *self = (SR_Vocabulary*) impl;
     impl->hSlts = NULL;
     return ESR_SUCCESS;
}

ESR_ReturnCode SR_VocabularyDestroyImpl(SR_Vocabulary* self)
{
  SR_VocabularyImpl* impl = (SR_VocabularyImpl*) self;

#ifdef USE_TTP
  SR_DestroyG2P(self);
#endif

     if (impl->vocabulary!=NULL)
       {
	 CA_UnloadDictionary(impl->vocabulary);
	 CA_FreeVocabulary(impl->vocabulary);
	 impl->vocabulary = NULL;
       }
	   LSTRFREE(impl->filename);
     FREE(impl);
     return ESR_SUCCESS;
}

ESR_ReturnCode sr_vocabularyloadimpl_for_real(SR_VocabularyImpl* impl)
{
	ESR_ReturnCode rc = ESR_SUCCESS;
	ESR_BOOL sessionExists = ESR_FALSE;
  LCHAR vocabulary[P_PATH_MAX];
  size_t len;

     impl->vocabulary = CA_AllocateVocabulary();
     if (impl->vocabulary==NULL)
       {
	 rc = ESR_OUT_OF_MEMORY;
	 PLogError(ESR_rc2str(rc));
	 goto CLEANUP;
       }

     CHKLOG(rc, ESR_SessionExists(&sessionExists));

     if (sessionExists)
       {
          LSTRCPY(vocabulary, impl->filename);
          len = P_PATH_MAX;
          CHKLOG(rc, ESR_SessionPrefixWithBaseDirectory(vocabulary, &len));
       }
     else
       LSTRCPY(vocabulary, impl->filename);

     CA_LoadDictionary(impl->vocabulary, vocabulary, L(""), &impl->locale);
     if(impl->vocabulary->is_loaded == False /*(booldata)*/ ) {
       CHKLOG(rc, ESR_INVALID_ARGUMENT);
     }
     impl->ttp_lang = TTP_LANG(impl->locale);

#ifdef USE_TTP
     rc = SR_CreateG2P((SR_Vocabulary*)impl);
	 if (rc != ESR_SUCCESS) {
          goto CLEANUP;
     }
#endif

CLEANUP:
	 return rc;
}

ESR_ReturnCode SR_VocabularyLoadImpl(const LCHAR* filename, SR_Vocabulary** self)
{
  SR_Vocabulary* Interface;
  SR_VocabularyImpl* impl;
  ESR_ReturnCode rc;

     CHK(rc, SR_VocabularyCreateImpl(&Interface));
     impl = (SR_VocabularyImpl*) Interface;
#if DO_DEFER_LOADING_UNTIL_LOOKUPS
	 impl->vocabulary = NULL;
	 impl->ttp_lang = NULL;
	 impl->filename = LSTRDUP( filename);
	 impl->locale = ESR_LOCALE_EN_US; // default really
	 impl->hSlts = NULL;
#else
	 impl->filename = LSTRDUP( filename);
	 CHKLOG( rc, sr_vocabularyloadimpl_for_real( impl));
#endif

     *self = Interface;
     return ESR_SUCCESS;
 CLEANUP:
     Interface->destroy(Interface);
     return rc;
}

ESR_ReturnCode SR_VocabularySaveImpl(SR_Vocabulary* self, const LCHAR* filename)
{
  /* TODO: complete */
  return ESR_SUCCESS;
}

/* internal util function prototype */
/* we split the string on all non-alphanum and "'" which
is handled below */
#define LSINGLEQUOTE L('\'')
int split_on_nonalphanum(LCHAR* toSplit, LCHAR** end, const ESR_Locale locale)
{
  int nsplits = 0;
  LCHAR* _next = toSplit;
    while(*_next)
    {
		do {
			if(*_next == LSINGLEQUOTE && locale == ESR_LOCALE_EN_US) {
				if(_next[1] != 't' && _next[1] != 's') break;
				else if( LISALNUM(_next[2])) break; // LISDIGIT
				else { *_next++; continue; }
			}
			if(!*_next || !LISALNUM(*_next)) break;
			*_next++;
		} while(1);
      // FORMERLY:  while(*_next && LISALNUM(*_next))     _next++;

      /* check if I am at the last word or not */
      if(*_next)
      {
        *_next = 0; /* replace split_char with '\0' the word */
		nsplits++;
        _next++;    /* point to first char of next word */
		*end = _next; /* we'll be push forward later, if there's content here!*/
      }
      else
        *end = _next;
    }
	return nsplits;
}

void join(LCHAR* toJoin, LCHAR* end, LCHAR join_char)
{
  LCHAR* _next;
    for(_next = toJoin; _next<end; _next++)
		if(*_next == 0) *_next = join_char;
}

size_t get_num_prons( const LCHAR* word_prons, const LCHAR** word_pron_ptr, int max_num_prons)
{
  int num_prons = 0;
  while(word_prons && *word_prons) {
    word_pron_ptr[ num_prons++] = word_prons;
    if(num_prons >= max_num_prons) break;
    while( *word_prons) word_prons++;
    word_prons++;
  }
  return num_prons;
}

/* This function is used from multi-word phrases, such as "mike smith".  We
   build up the pronunication of the phrase, by appending the pronunciation
   of each word.  We need to handle the cases of multiple prons for "mike"
   and multiple prons for "smith".  For simple cases we try to run faster
   code. */

int append_to_each_with_joiner( LCHAR* phrase_prons, const LCHAR* word_prons, const LCHAR joiner, size_t max_len, size_t* len)
{
  LCHAR* word_pron_ptr[MAX_NUM_PRONS];
  LCHAR* phrase_pron_ptr[MAX_NUM_PRONS];
  LCHAR *dst, *max_dst;
  const LCHAR *src;
  size_t nphrase_prons = get_num_prons( phrase_prons, (const LCHAR**)phrase_pron_ptr, MAX_NUM_PRONS);
  size_t nword_prons = get_num_prons( word_prons, (const LCHAR**)word_pron_ptr, MAX_NUM_PRONS);
  max_dst = phrase_prons+max_len-3;

  if( nword_prons == 0)
    return 0;
  else if(nphrase_prons == 0) {
	for(src=word_prons,dst=phrase_prons; src && *src; ) {
		for( ; *src && dst<max_dst; ) {
			*dst++ = *src++;
		}
      *dst++ = *src++; // copy the null
    }
    *dst = 0; // add a double-null
	*len = dst-phrase_prons;
    return 0;
  }
  else if(nphrase_prons == 1 && nword_prons == 1) {
    for(dst=phrase_prons; *dst; ) dst++;
    if(joiner!=L('\0')) *dst++ = joiner;
    for(src=word_prons; *src && dst<max_dst; ) *dst++ = *src++;
    *dst++ = 0;
    *dst = 0; // add a double-null
	*len = dst-phrase_prons;
    return 0;
  }
  else  {
    size_t i,j;
    LCHAR *phrase_pron_dups[MAX_NUM_PRONS];
    LCHAR *dst_good_end = phrase_prons+1;
    for(i=0;i<nphrase_prons; i++)
      phrase_pron_dups[i] = LSTRDUP( phrase_pron_ptr[i]);
    dst = phrase_prons;
    for(i=0;i<nphrase_prons; i++) {
      for(j=0; j<nword_prons; j++) {
	for(src=phrase_pron_dups[i]; *src && dst<max_dst; ) *dst++=*src++;
	if(dst>max_dst) break;
	if(joiner!=L('\0')) *dst++ = joiner;
	for(src=word_pron_ptr[j]; *src && dst<max_dst; ) *dst++=*src++;
	if(dst>max_dst) break;
	*dst++ = 0;
	dst_good_end = dst;
      }
    }
    *dst_good_end++ = 0; // double-null terminator
    for(i=0; i<nphrase_prons; i++) LSTRFREE( phrase_pron_dups[i]);
    return 0;
  }
}

PINLINE LCHAR* get_first_word(LCHAR* curr, LCHAR* end)
{
  while(*curr==L('\0') && curr<end) curr++;
  return curr;
}

PINLINE LCHAR* get_next_word(LCHAR* curr, LCHAR* end)
{
  while(*curr) curr++;
  if(curr<end)  curr++;
  while( !*curr && curr<end) curr++;
  return curr;
}

/*
  For each word in a phrase (words separated by spaces)

  if the complete word is in the dictionary
  return pron
  else
  if the word contains '_', split the word into parts
  and check if parts are in the dictionary.
  if none of the parts are in the dictionary,
  reassemble the parts and pass the whole thing to TTP
  else
  build the pron by concat of TTP pron and dictionary pron for individual parts
*/
ESR_ReturnCode SR_VocabularyGetPronunciationImpl(SR_Vocabulary* self, const LCHAR* phrase, LCHAR* pronunciation, size_t* pronunciation_len)
{
  SR_VocabularyImpl* impl = (SR_VocabularyImpl*) self;
  /* copy of phrase */
  LCHAR copy_of_phrase[MAX_PRON_LEN];

  /* pointer to curr phoneme output */
  LCHAR* curr_phoneme = pronunciation;
  // size_t pronunciation_len = *len;

  ESR_ReturnCode nEsrRes = ESR_SUCCESS;
  int text_length;
  size_t len;
  int nsplits;

#ifdef USE_TTP
  SWIsltsResult      res = SWIsltsSuccess;
  SWIsltsTranscription  *pTranscriptions = NULL;
  int nNbrOfTranscriptions = 0;
#endif /* USE_TTP */
  /* full inf pron after conversion */
  LCHAR infpron[MAX_PRON_LEN];
  LCHAR* p_infpron;
  LCHAR* curr;     /* pointer to current word */
  LCHAR* end = 0;   /* pointer to end of phrase */

  if(self == NULL || phrase == NULL)
    {
      PLogError(L("ESR_INVALID_ARGUMENT"));
      return ESR_INVALID_ARGUMENT;
    }

  if( LSTRLEN(phrase) >= MAX_PRON_LEN)
	return ESR_ARGUMENT_OUT_OF_BOUNDS;

#if DO_DEFER_LOADING_UNTIL_LOOKUPS
  if( impl->vocabulary == NULL) {
    CHKLOG( nEsrRes, sr_vocabularyloadimpl_for_real( impl));
  }
#endif

  /* by default, check the whole word entry first (regardless of underscores) */
  if( CA_GetEntryInDictionary(impl->vocabulary, phrase, pronunciation, (int*)&len, MAX_PRON_LEN)) {
    // len includes the final null, but not the double-null
    *pronunciation_len = LSTRLEN(pronunciation)+1;
    // look for double-null terminator
    while( pronunciation[ (*pronunciation_len)] != L('\0'))
      *pronunciation_len += LSTRLEN( pronunciation + (*pronunciation_len)) + 1;

    return ESR_SUCCESS;
  }

  /*************************/
  /* split digit strings */
  text_length = MAX_PRON_LEN;
  nEsrRes = run_ttt(phrase, copy_of_phrase, &text_length);
  if (nEsrRes != ESR_SUCCESS)
    {
      PLogError(L("ESR_FATAL_ERROR: run_ttt( ) fails with return code %d\n"), nEsrRes);
      return nEsrRes;
    }

  len = 0;
  *curr_phoneme = L('\0');
  if( *pronunciation_len>=12) curr_phoneme[1] = L('\0');
  else return ESR_INVALID_ARGUMENT;

  /*************************/
  /* split into word parts */
  nsplits = split_on_nonalphanum(copy_of_phrase, &end, impl->locale);

  /******************************************************/
  /* if none of the words are found in the dictionary, then
     reassemble and get the TTP pron for the whole thing */
  curr=get_first_word(copy_of_phrase,end);
  /* check if there are any valid characters at all */
  if(!curr || !*curr)
    return ESR_INVALID_ARGUMENT;
  /* now loop over all words in the phrase */
  for(   ; *curr; curr = get_next_word(curr,end))
    {
      LCHAR* squote = NULL;
      p_infpron = infpron;

      /* by default, check the whole word entry first (regardless of LSINGLEQUOTE) */
      if(CA_GetEntryInDictionary(impl->vocabulary, curr, p_infpron, (int*)&len, MAX_PRON_LEN))
        {
          /* concatenate, and insert join_char between words */
          append_to_each_with_joiner( pronunciation, p_infpron, OPTSILENCE_CODE, MAX_PRON_LEN, &len);
        }
      else {
        p_infpron[0] = 0;
        /* if this is English AND we're dealing with a quote (possessive or a
           contraction), then we use the dictionary for the stuff before the
           quote, and use the TTP to find out what single phoneme should
           correspond the the thing after the quote ('s' or 't').  This keeps
           the code clean (no phoneme codes here), and maps 's' to 's' or 'z'
           with the intelligence of the G2P engine */
        if( impl->locale == ESR_LOCALE_EN_US) {
          if( (squote=LSTRCHR(curr,LSINGLEQUOTE))==NULL) {}
          else {
            *squote = L('\0');   // temporary
            if( CA_GetEntryInDictionary(impl->vocabulary, curr, p_infpron, (int*)&len, MAX_PRON_LEN)) {
            } else
              p_infpron[0] = 0;
            *squote = LSINGLEQUOTE; // undo temporary
          }
        }
#ifdef USE_TTP
        pTranscriptions = NULL;
        if (impl->hSlts)
          {
            res = SWIsltsG2PGetWordTranscriptions(impl->hSlts, curr, &pTranscriptions, &nNbrOfTranscriptions);
            if (res != SWIsltsSuccess) {
              PLogError(L("ESR_FATAL_ERROR: SWIsltsG2PGetWordTranscriptions( ) fails with return code %d\n"), res);
              return ESR_FATAL_ERROR;
            }
            if( impl->locale == ESR_LOCALE_EN_US && p_infpron[0] && squote!=L('\0')) {
              const LCHAR* lastPhoneme = pTranscriptions[0].pBuffer;
              while(lastPhoneme && *lastPhoneme && lastPhoneme[1]!=L('\0'))
                lastPhoneme++;
              append_to_each_with_joiner( pronunciation, p_infpron, OPTSILENCE_CODE, MAX_PRON_LEN, &len);
              append_to_each_with_joiner( pronunciation, lastPhoneme, L('\0'), MAX_PRON_LEN, &len);
            } else {
              /* only one transcription available from seti */
              p_infpron = pTranscriptions[0].pBuffer;
              append_to_each_with_joiner( pronunciation, p_infpron, OPTSILENCE_CODE, MAX_PRON_LEN, &len);
#if defined(SREC_ENGINE_VERBOSE_LOGGING)
              PLogError("L: used G2P for %s", curr);
#endif

            }
            if (pTranscriptions) {
              res = SWIsltsG2PFreeWordTranscriptions(impl->hSlts, pTranscriptions);
              pTranscriptions = NULL;
              if (res != SWIsltsSuccess) {
                PLogError(L("ESR_FATAL_ERROR: SWIsltsG2PFreeWordTranscriptions( ) fails with return code %d\n"), res);
                return ESR_FATAL_ERROR;
              }
            }
          } else {
            nEsrRes = ESR_INVALID_ARGUMENT;
            PLogError(L("ESR_INVALID_ARGUMENT: impl->hSlts was not configured!"));
            return nEsrRes;
          }
#else /* USE_TTP */
        nEsrRes = ESR_INVALID_ARGUMENT;
        PLogError(L("ESR_INVALID_ARGUMENT: need USE_TTP build to guess pronunciations!"));
        return nEsrRes;
#endif
      } /* multi-word phrase */
    } /* loop over words in phrase */
  len = LSTRLEN(pronunciation)+1;
  // look for double-null terminator
  while( pronunciation[ len] != L('\0'))
    len += LSTRLEN( pronunciation + len) + 1;
  *pronunciation_len = len;
  nEsrRes = ESR_SUCCESS;
 CLEANUP:
  return nEsrRes;
}

ESR_ReturnCode SR_VocabularyGetLanguageImpl(SR_Vocabulary* self, ESR_Locale* locale)
{
  SR_VocabularyImpl* impl = (SR_VocabularyImpl*) self;

  *locale = impl->locale;
  return ESR_SUCCESS;
}

/* simple text normalization rountine for splitting up any digit string */
static ESR_ReturnCode run_ttt(const LCHAR *input_sentence, LCHAR *output_sentence, int *text_length)
{
  ESR_ReturnCode         nRes = ESR_SUCCESS;
  int                    num_out = 0;
  int                    max_text_length = *text_length / sizeof(LCHAR) - 1;
  ESR_BOOL                   bDigit = False;

  while (*input_sentence != L('\0')) {
    if (num_out + 2 >= max_text_length) {
      nRes = ESR_FATAL_ERROR;
      goto CLEAN_UP;
    }

    if (L('0') <= *input_sentence && *input_sentence <= L('9')) {
      if (num_out > 0 && !LISSPACE(output_sentence[num_out-1]) ) {
		  // put 1 space before digits
        output_sentence[num_out] = L(' ');
        num_out++;
		while( LISSPACE(*input_sentence) ) input_sentence++;
      }
      output_sentence[num_out] = *input_sentence;
      num_out++;
      bDigit = True;
    }
    else {
      if (bDigit == True && !LISSPACE(output_sentence[num_out-1])) {
		// put 1 space after digits
        output_sentence[num_out] = L(' ');
        num_out++;
		while( LISSPACE(*input_sentence)) input_sentence++;
      }
		output_sentence[num_out] = *input_sentence;
		num_out++;
      bDigit = False;
    }
    input_sentence++;
	if( LISSPACE(output_sentence[num_out-1]))
		while(LISSPACE(*input_sentence )) input_sentence++; // remove repeated spaces
  }

  output_sentence[num_out] = L('\0');
  *text_length = num_out * sizeof(LCHAR);
  return ESR_SUCCESS;

 CLEAN_UP:

  *output_sentence = L('\0');
  *text_length = 0;
  return nRes;
}
