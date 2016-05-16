/*---------------------------------------------------------------------------*
 *  par_basi.c  *
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


#include "simapi.h"
#include "portable.h"

static const char par_basi[] = "$Id: par_basi.c,v 1.3.10.4 2007/10/15 18:06:24 dahan Exp $";


CA_AcoustInputParams *CA_AllocateAcousticParameters(void)
{
  CA_AcoustInputParams *hAcoustInp = NULL;
  TRY_CA_EXCEPT

  hAcoustInp = (CA_AcoustInputParams *) CALLOC_CLR(1,
               sizeof(CA_AcoustInputParams), "ca.hAcoustInp");

  hAcoustInp->is_loaded = False;
  hAcoustInp->ca_rtti = CA_ACOUSTIC_PARAMETERS_SIGNATURE;

  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hAcoustInp);

  return (hAcoustInp);

}


void CA_FreeAcousticParameters(CA_AcoustInputParams *hAcoustInp)
{
  TRY_CA_EXCEPT
  ASSERT(hAcoustInp);
  FREE((char *)hAcoustInp);
  return;
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hAcoustInp);

}


CA_PatInputParams *CA_AllocatePatternParameters(void)
{
  CA_PatInputParams *hPatInp = NULL;

  TRY_CA_EXCEPT

  hPatInp = (CA_PatInputParams *) CALLOC_CLR(1,
            sizeof(CA_PatInputParams), "ca.hPatInp");

  hPatInp->is_loaded = False;
  hPatInp->ca_rtti = CA_PATTERN_PARAMETERS_SIGNATURE;

  return (hPatInp);
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hPatInp);

}


void CA_FreePatternParameters(CA_PatInputParams *hPatInp)
{
  TRY_CA_EXCEPT
  ASSERT(hPatInp);
  FREE((char *)hPatInp);
  return;
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hPatInp);

}


CA_RecInputParams *CA_AllocateRecognitionParameters(void)
{
  CA_RecInputParams *hRecInp = NULL;
  TRY_CA_EXCEPT
  hRecInp = (CA_RecInputParams *) CALLOC_CLR(1,
            sizeof(CA_RecInputParams), "ca.hRecInp");

  if ( hRecInp != NULL )
    {
    hRecInp->is_loaded = False;
    hRecInp->ca_rtti = CA_RECOGNIZER_PARAMETERS_SIGNATURE;
    }
  return (hRecInp);
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hRecInp);

}


void CA_FreeRecognitionParameters(CA_RecInputParams *hRecInp)
{
  TRY_CA_EXCEPT
  ASSERT(hRecInp);
  FREE((char *)hRecInp);
  return;
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hRecInp);

}


