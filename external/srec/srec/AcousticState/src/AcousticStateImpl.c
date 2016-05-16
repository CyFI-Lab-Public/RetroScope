/*---------------------------------------------------------------------------*
 *  AcousticStateImpl.c                                                      *
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

#include "SR_AcousticState.h"
#include "SR_AcousticStateImpl.h"
#include "plog.h"
#include "pmemory.h"

#define MTAG __FILE__

ESR_ReturnCode SR_AcousticStateCreateImpl(SR_Recognizer* recognizer)
{
  SR_AcousticStateImpl* impl;
  SR_RecognizerImpl* recogImpl = (SR_RecognizerImpl*) recognizer;

  if (recogImpl == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  impl = NEW(SR_AcousticStateImpl, MTAG);
  if (impl == NULL)
  {
    PLogError(L("ESR_OUT_OF_MEMORY"));
    return ESR_OUT_OF_MEMORY;
  }

  impl->Interface.load = &SR_AcousticStateLoadImpl;
  impl->Interface.save = &SR_AcousticStateSaveImpl;
  impl->Interface.destroy = &SR_AcousticStateDestroyImpl;
  impl->Interface.reset = &SR_AcousticStateResetImpl;
  impl->Interface.set = &SR_AcousticStateSetImpl;
  impl->Interface.get = &SR_AcousticStateGetImpl;

  recogImpl->acousticState = &impl->Interface;
  return ESR_SUCCESS;
}


ESR_ReturnCode SR_AcousticStateGetImpl(SR_Recognizer* self, LCHAR *param_string, size_t* len )
{
  SR_RecognizerImpl* recogImpl = (SR_RecognizerImpl*) self;

  return CA_GetCMSParameters(recogImpl->wavein, param_string, len );
}


ESR_ReturnCode SR_AcousticStateSetImpl(SR_Recognizer* self, const LCHAR *param_string )
{
  SR_RecognizerImpl* recogImpl = (SR_RecognizerImpl*) self;

  return CA_SetCMSParameters(recogImpl->wavein, param_string );
}


ESR_ReturnCode SR_AcousticStateDestroyImpl(SR_Recognizer* recognizer)
{
  SR_RecognizerImpl* recogImpl = (SR_RecognizerImpl*) recognizer;
  SR_AcousticStateImpl* impl = (SR_AcousticStateImpl*) recogImpl->acousticState;

  FREE(impl);
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_AcousticStateResetImpl(SR_Recognizer* recognizer)
{
  SR_RecognizerImpl* recogImpl = (SR_RecognizerImpl*) recognizer;
  CA_ReLoadCMSParameters(recogImpl->wavein, NULL);
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_AcousticStateLoadImpl(SR_Recognizer* self, const LCHAR* filename)
{
  SR_RecognizerImpl* recogImpl = (SR_RecognizerImpl*) self;

  CA_ReLoadCMSParameters(recogImpl->wavein, filename);
  return ESR_SUCCESS;
}

ESR_ReturnCode SR_AcousticStateSaveImpl(SR_Recognizer* recognizer, const LCHAR* filename)
{
  return ESR_NOT_IMPLEMENTED;
}
