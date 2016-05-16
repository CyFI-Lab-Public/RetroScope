/*---------------------------------------------------------------------------*
 *  rec_load.c  *
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
#include "portable.h"

#define CROSSWORD 1

static const char rec_load[] = "$Id: rec_load.c,v 1.8.6.7 2007/10/15 18:06:24 dahan Exp $";


void CA_LoadModelsInAcoustic(CA_Recog *hRecog, CA_Acoustic *hAcoust,
                             CA_AcoustInputParams *hAcoustInp)
{
  int rc;
  TRY_CA_EXCEPT
  ASSERT(hRecog);
  ASSERT(hAcoust);
  ASSERT(hAcoustInp);
  
  if (hAcoust->is_loaded == False)
    SERVICE_ERROR(ACOUSTIC_NOT_LOADED);
  if (hAcoustInp->is_loaded == False)
    SERVICE_ERROR(ACOUSTIC_INPUT_NOT_LOADED);
    // returns 1 for OK, 0 if not OK
  rc = add_acoustic_model_for_recognition(hRecog->recm, hAcoust->swimodel); 
  return;
  
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hRecog)
}


void CA_UnloadRecognitionModels(CA_Recog *hRecog)
{
  TRY_CA_EXCEPT
  int rc;
  rc = clear_acoustic_models_for_recognition(hRecog->recm);
  BEG_CATCH_CA_EXCEPT
  END_CATCH_CA_EXCEPT(hRecog)
}
