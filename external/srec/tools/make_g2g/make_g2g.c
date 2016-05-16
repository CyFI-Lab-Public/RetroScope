/*---------------------------------------------------------------------------*
 *  make_g2g.c  *
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

#include "pstdio.h"
#include "pmemory.h"
#include "plog.h"
#include "PFile.h"
#include "PFileSystem.h"
#include "PANSIFileSystem.h"

#include "SR_Grammar.h"
#include "ESR_CommandLine.h"
#include "ESR_Session.h"
#include "LCHAR.h"
#include "srec_context.h"

#define MAX_LINE_LENGTH 256
#define MAX_STR_LENGTH   80
#define MAX_SEM_RESULTS   3
#define MAX_KEYS         30

static void usage(LCHAR* exename)
{
  LFPRINTF(stdout,"usage: %s -base <base grammar filename> [-out <output file>] \n",exename);
}

int main (int argc, char *argv[])
{
  SR_Grammar* grammar = NULL;
  ESR_ReturnCode rc;
  LCHAR base[P_PATH_MAX];
  LCHAR* p;
  LCHAR outFilename[P_PATH_MAX];
  size_t len;

	/*
	 * Initialize portable library.
	 * Can't use CHKLOG() before plogInit, so use non-portable methods instead.
	 */
  CHKLOG(rc, PMemInit());
/*  CHKLOG(rc, PFileSystemCreate());
  CHKLOG(rc, PANSIFileSystemCreate());
  CHKLOG(rc, PANSIFileSystemAddPath(L("/dev/ansi/"), L("/")));*/
  CHKLOG(rc, PLogInit(NULL, 3));
  
  /* Set ANSI file-system as default file-system */
/*  CHKLOG(rc, PANSIFileSystemSetDefault(ESR_TRUE));*/
  /* Set virtual current working directory to native current working directory */
/*  len = P_PATH_MAX;
  CHKLOG(rc, PANSIFileSystemGetcwd(cwd, &len));
  CHKLOG(rc, PFileSystemChdir(cwd));*/
  
  len = P_PATH_MAX;
  rc = ESR_CommandLineGetValue(argc, (const char **)argv, L("base"), base, &len);
  if (rc==ESR_NO_MATCH_ERROR)
    {
    LFPRINTF(stderr, "ERROR: Mandatory option -base is unspecified\n");
    return ESR_INVALID_ARGUMENT;
  }
  else if (rc!=ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }

  len = P_PATH_MAX;
  rc = ESR_CommandLineGetValue(argc, (const char **)argv, L("out"), outFilename, &len);
  if (rc==ESR_NO_MATCH_ERROR)
    {
      LFPRINTF(stderr, "ERROR: Mandatory option -out is unspecified\n");
      return ESR_INVALID_ARGUMENT;
    }
  else if (rc!=ESR_SUCCESS)
    {
      PLogError(ESR_rc2str(rc));
      goto CLEANUP;
    }

  if (base==NULL || (LSTRCMP(outFilename, L(""))==0 ))
    {
      usage(argv[0]);
      exit(EXIT_FAILURE);
    }
  
  /* setup the default outfilename if not already set */
  if (!outFilename[0])
    {
      LSTRCPY(outFilename,base);
      /* get rid of the ',addWords=2000' grammar load param */
      p = LSTRCHR(outFilename,L(','));
      if(p) *p = 0;
      LSTRCAT(outFilename,L(".g2g"));
    }
  
  LFPRINTF(stdout,"Loading grammar %s from text files...\n",base);
  CHKLOG(rc, SR_GrammarLoad(base, &grammar));

  LFPRINTF(stdout,"Saving grammar as binary image %s...\n",outFilename);
  CHKLOG(rc, SR_GrammarSave(grammar, outFilename));

  LFPRINTF(stdout,"SUCCESS!\n");

CLEANUP:
 
  if (grammar)
		grammar->destroy(grammar);
  PLogShutdown();
/*	PANSIFileSystemDestroy();
	PFileSystemDestroy();*/
  PMemShutdown();
  return rc;
}
