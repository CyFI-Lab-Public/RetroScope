/*---------------------------------------------------------------------------*
 *  acc_basi.c  *
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

#ifdef SET_RCSID
static const char *rcsid = 0 ? (const char *) &rcsid :
                           "$Id: acc_basi.c,v 1.4.6.11 2008/03/07 19:46:45 dahan Exp $";
#endif
                           
CA_Acoustic *CA_AllocateAcoustic(void)
{
  CA_Acoustic *hAcoust = NULL;
  
  TRY_CA_EXCEPT;
  
  hAcoust = (CA_Acoustic *) CALLOC_CLR(1,
            sizeof(CA_Acoustic), "ca.hAcoust");
  hAcoust->is_loaded = False;
  hAcoust->pattern_setup_count = 0;
  hAcoust->ca_rtti = CA_ACOUSTIC_SIGNATURE;
  BEG_CATCH_CA_EXCEPT;
  END_CATCH_CA_EXCEPT(hAcoust);
  return (hAcoust);
}


void CA_FreeAcoustic(CA_Acoustic *hAcoust)
{
  TRY_CA_EXCEPT
  
  ASSERT(hAcoust);
  FREE((char *) hAcoust);
  return;
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hAcoust)
}

int CA_LoadAcousticSub(CA_Acoustic *hAcoust, char *subname, CA_AcoustInputParams *hAcoustInp)
{
//#ifndef _RTT
//  int     load_genome = 0;
//#endif
  
  if (hAcoustInp == 0)
  {
    /* SpeechWorks image format! */
    hAcoust->swimodel = load_swimodel(subname);
    if (hAcoust->swimodel == NULL)
    {
        // failed to load, load_swimodel will have printed an error to the log
        return 0;
    }
    hAcoust->is_loaded = ESR_TRUE;
    return 1; 
  }
  else
  {
    SERVICE_ERROR(FEATURE_NOT_SUPPORTED);
    return 0; 
  }
}

void CA_UnloadAcoustic(CA_Acoustic *hAcoust)
{
  TRY_CA_EXCEPT
  ASSERT(hAcoust);
  
  if (hAcoust->is_loaded == False)
    SERVICE_ERROR(ACOUSTIC_NOT_LOADED);
  if (hAcoust->swimodel)
  {
    free_swimodel(hAcoust->swimodel);
    hAcoust->swimodel = 0;
    hAcoust->is_loaded = False;
    return;
  }
	else
		SERVICE_ERROR(ACOUSTIC_NOT_LOADED);
  return;
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hAcoust)
}
