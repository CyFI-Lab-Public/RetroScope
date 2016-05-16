/*---------------------------------------------------------------------------*
 *  dictTest.c  *
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

#define MAX_LINE_LENGTH     512
#define MAX_PRONS_LENGTH 1024

static ESR_ReturnCode InitSession ( LCHAR *parfilename );
static ESR_ReturnCode ShutdownSession ( void );

void usage()
{
  LPRINTF("usage: dictTest [-words words.txt] [-vocab dictionary.ok] [-itest input.tst] [-out output.txt] [-locale en-us|en-gb|fr-fr|de-de] [-parfile baseline.par]\n");
}

void doGetProns(SR_Vocabulary *vocab, LCHAR* phrase, size_t len, FILE* fout)
{
  ESR_ReturnCode rc;
  LCHAR prons[MAX_PRONS_LENGTH];

  rc = SR_VocabularyGetPronunciation(vocab, phrase, prons, &len);
  // rc = vocab->getPronunciation(vocab, phrase, prons, &len);

  if (rc != ESR_SUCCESS)
    LFPRINTF(fout,"ERROR: %s\n", ESR_rc2str(rc));
  else {
    size_t len_used;
    LCHAR *pron = 0;
    for(len_used=0; len_used<len; ) {
      pron = &prons[0]+len_used;
      len_used += LSTRLEN(pron)+1;
      LFPRINTF(fout,"%s : %s\n", phrase, pron);
    }
  }
}

//parses the input file, runs phoneme tests and produces output to be parsed by perl script
void doInputTestPhonemes(SR_Vocabulary *vocab, PFile* fin, FILE* fout)
{
#if 0
  //waste of space with all of these arrays, they are too large, but leave for now
  ESR_ReturnCode rc;
  LCHAR line[2 * MAX_PRONS_LENGTH];
  LCHAR phoneme[MAX_PRONS_LENGTH];
  LCHAR* phrase;
  LCHAR* expectedPhoneme;
    LCHAR** tokenArray;
  size_t len;

  //read through the test file parsing it into the variables
  while(!pfeof(fin))
  {
    pfgets(line, MAX_LINE_LENGTH, fin);

        rc = ESR_ProcessLinearToCommandLineTokens(line, &tokenArray, &len);
        if (rc!=ESR_SUCCESS || len!=2)
        {
          LFPRINTF(fout, "ERROR: INVALID FORMAT for input line\n");
            continue;
        }
        phrase = tokenArray[0];
        expectedPhoneme = tokenArray[1];

      LPRINTF( "expected %s\n", expectedPhoneme);

        len = MAX_PRONS_LENGTH;
        rc = vocab->getPronunciation(vocab, phrase, phoneme, &len);

        if(rc != ESR_SUCCESS)
            LFPRINTF(fout,"ERROR: %s\n", ESR_rc2str(rc));
        else
        {
            LFPRINTF(fout,"%s|%s|%s|", phrase, expectedPhoneme, phoneme);

            if(LSTRCMP(expectedPhoneme, phoneme) == 0)
                LFPRINTF(fout,"PASSED\n");
            else
                LFPRINTF(fout,"FAILED\n");
        }
  }
#endif
}

int main(int argc, char **argv)
{
  LCHAR phrase[MAX_LINE_LENGTH];
  SR_Vocabulary *vocab = 0;
  LCHAR vocabfile[MAX_LINE_LENGTH];
  LCHAR outfilename[MAX_LINE_LENGTH];
  LCHAR testfilename[MAX_LINE_LENGTH];
  LCHAR parfilename[MAX_LINE_LENGTH];
  LCHAR wordfile[MAX_LINE_LENGTH];
  LCHAR locale[MAX_LINE_LENGTH];
  LCHAR ptemp[MAX_LINE_LENGTH];
  LCHAR* p;
  ESR_ReturnCode rc;
  int i;
  PFile* fin = 0;
  FILE* fout = stdout;
  size_t len;
  ESR_BOOL bSession = ESR_FALSE;

  LCHAR *env_sdk_path;
  LCHAR *env_lang;

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

    fout = stdout;
  *vocabfile = 0;
  *wordfile = 0;
  *locale = 0;
  *outfilename = 0;
  *testfilename = 0;
  *parfilename = 0;

  /* get some phrases from the user */
  LPRINTF("\nDictation Test Program for esr (Nuance Communications, 2007)\n");

  if(argc != 1 && argc != 3 && argc != 5 && argc != 7 && argc != 9)
  {
    usage();
        rc = 1;
    goto CLEANUP;
  }

  for(i=1; i<argc; i++)
  {
    if(!LSTRCMP(argv[i], L("-words")))
      LSTRCPY(wordfile, argv[++i]);
    else if(!LSTRCMP(argv[i], L("-vocab")))
      LSTRCPY(vocabfile, argv[++i]);
    else if(!LSTRCMP(argv[i], L("-locale")))
      LSTRCPY(locale, argv[++i]);
    else if(!LSTRCMP(argv[i], L("-out")))
      LSTRCPY(outfilename, argv[++i]);
    else if(!LSTRCMP(argv[i], L("-itest")))
      LSTRCPY(testfilename, argv[++i]);
    else if(!LSTRCMP(argv[i], L("-parfile")) || !LSTRCMP(argv[i], L("-par")) )
      LSTRCPY(parfilename, argv[++i]);
    else {
      usage();
      rc = 1;
      goto CLEANUP;
    }
  }

  if ( *parfilename == L('\0') )
  {
    LPRINTF ( "Warning: No parfile defined in the command line.\n" );
    LPRINTF ( "Looking for the default parfile, $ESRSDK/config/$ESRLANG/baseline.par...\n" );

    env_sdk_path =  LGETENV(L("ESRSDK"));
    if ( env_sdk_path != NULL )
    {
      LSPRINTF ( parfilename, L("%s/config/"), env_sdk_path );
      env_lang = LGETENV(L("ESRLANG"));
      if ( env_lang != NULL )
      {
         LSTRCAT ( parfilename, env_lang );
         LSTRCAT ( parfilename, L("/baseline.par") );
      }
      else
      {
        LPRINTF("Error: An environment variable ESRLANG should be defined.\n");
        goto CLEANUP;
      }
    }
    else
    {
      LPRINTF("Error: An environment variable ESRSDK should be defined.\n");
      goto CLEANUP;
    }
  }

  rc = InitSession( parfilename );
  if ( rc != ESR_SUCCESS )
  {
    LPRINTF("Error: %s\n", ESR_rc2str(rc));
    goto CLEANUP;
  }
  bSession = ESR_TRUE;

  if (*vocabfile == 0)
  {
    len = sizeof(vocabfile);
    rc = ESR_SessionGetLCHAR ( L("cmdline.vocabulary"), vocabfile, &len );
    env_sdk_path =  LGETENV(L("ESRSDK"));
    if ( env_sdk_path != NULL )
      {
	LSPRINTF ( parfilename, L("%s/config/"), env_sdk_path );
	env_lang = LGETENV(L("ESRLANG"));
	if ( env_lang != NULL )
	  {
	    LSTRCAT ( parfilename, env_lang );
	    LSTRCAT ( parfilename, L("/baseline.par") );
	  }
	else
	  {
	    LPRINTF("Error: An environment variable ESRLANG should be defined.\n");
	    goto CLEANUP;
	  }
      }
    else
      {
	LPRINTF("Error: An environment variable ESRSDK should be defined.\n");
	goto CLEANUP;
      }

    strcpy(ptemp, env_sdk_path);
    strcat(ptemp,"/config/");
    strcat(ptemp,env_lang);
    strcat(ptemp,"/");
    strcat(ptemp,vocabfile);
    strcpy(vocabfile,ptemp);
    if ( rc == ESR_SUCCESS )
    {
      len = sizeof(vocabfile);
       rc = ESR_SessionPrefixWithBaseDirectory(vocabfile, &len);
    }
    else
    {
       *vocabfile = 0;
    }
  }

  if (*vocabfile)
    rc = SR_VocabularyLoad(vocabfile, &vocab);
  else if (*locale)
  {
    ESR_Locale localeTag;

    rc = ESR_str2locale(locale, &localeTag);
    if (rc != ESR_SUCCESS)
    {
      LPRINTF("Error: %s\n",ESR_rc2str(rc));
      goto CLEANUP;
    }
    rc = SR_VocabularyCreate(localeTag, &vocab);
  }
  else
    rc = SR_VocabularyCreate(ESR_LOCALE_EN_US, &vocab);

  if (rc != ESR_SUCCESS)
  {
    LPRINTF("Error: %s\n",ESR_rc2str(rc));
    goto CLEANUP;
  }

  if (*outfilename) /* output file */
  {
    if  ((fout = fopen(outfilename,"w")) == NULL)
    {
      LPRINTF("Could not open file: %s\n",outfilename);
      rc = 1;
      goto CLEANUP;
    }
  }

  if (*wordfile) /* file mode */
  {
    if ((fin = pfopen(wordfile,"r")) == NULL)
    {
      LPRINTF("Could not open file: %s\n", wordfile);
      goto CLEANUP;
    }
    while (pfgets(phrase, MAX_LINE_LENGTH, fin)!=NULL)
    {
      lstrtrim(phrase);
      doGetProns(vocab, phrase, MAX_PRONS_LENGTH, fout);
    }

  }
  else if (*testfilename) /* test file mode */
  {
    if ((fin = pfopen(testfilename,"r")) == NULL)
    {
      LPRINTF("Could not open file: %s\n", testfilename);
      rc = 1;
      goto CLEANUP;
    }
    doInputTestPhonemes(vocab, fin, fout);
  }
  else /* interactive mode */
  {
    LPRINTF("'qqq' to quit\n");
    while (ESR_TRUE)
    {
      LPRINTF("> ");
      if(! pfgets(phrase, MAX_LINE_LENGTH, PSTDIN ))
        break;
      // remove trailing whitespace
      for(p=&phrase[0]; *p!=0 && *p!='\n' && *p!='\r'; p++) {}
      *p=0;
      lstrtrim(phrase);
      if(!LSTRCMP("qqq",phrase))
        break;
      else
        doGetProns(vocab, phrase, MAX_PRONS_LENGTH, fout);
     }
  }

CLEANUP:
  if(vocab)
    vocab->destroy(vocab);

  if(bSession)
    ShutdownSession();

  if(fin)
    pfclose(fin);

  if(fout && fout != stdout)
    fclose(fout);

/*  PANSIFileSystemDestroy();
  PFileSystemDestroy();*/
  PMemShutdown();
  return rc;
}

static ESR_ReturnCode InitSession ( LCHAR *parfilename )
{
  ESR_ReturnCode    init_status;

  init_status = SR_SessionCreate ( parfilename );

  return ( init_status );
}

static ESR_ReturnCode ShutdownSession ( void )
{
  ESR_ReturnCode    shutdown_status;

  shutdown_status = SR_SessionDestroy ( );

  return ( shutdown_status );
}

