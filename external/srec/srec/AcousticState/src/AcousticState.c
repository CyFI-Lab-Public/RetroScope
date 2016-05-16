/*---------------------------------------------------------------------------*
 *  AcousticState.c                                                          *
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

#include "SR_Recognizer.h"
#include "SR_RecognizerImpl.h"
#include "plog.h"
#include "pmemory.h"

SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateReset(SR_Recognizer* recognizer)
{
  SR_RecognizerImpl* impl;

  if (recognizer == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  impl = (SR_RecognizerImpl*) recognizer;

  return impl->acousticState->reset(recognizer);
}

SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateLoad(SR_Recognizer* recognizer, const LCHAR* filename)
{
  SR_RecognizerImpl* impl;

  if (recognizer == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  impl = (SR_RecognizerImpl*) recognizer;

  return impl->acousticState->load(recognizer, filename);
}

SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateSave(SR_Recognizer* recognizer, const LCHAR* filename)
{
  SR_RecognizerImpl* impl;

  if (recognizer == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  impl = (SR_RecognizerImpl*) recognizer;

  return impl->acousticState->save(recognizer, filename);
}


SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateSet ( SR_Recognizer* recognizer, const LCHAR *param_string )
{
  SR_RecognizerImpl* impl;

  if (recognizer == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  impl = (SR_RecognizerImpl*) recognizer;

  return impl->acousticState->set ( recognizer, param_string );
}


SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateGet ( SR_Recognizer* recognizer, LCHAR *param_string, size_t* len )
{
  SR_RecognizerImpl* impl;

  if (recognizer == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  impl = (SR_RecognizerImpl*) recognizer;

  return impl->acousticState->get ( recognizer, param_string, len );
}


