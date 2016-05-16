/*---------------------------------------------------------------------------*
 *  RecognizerResult.c  *
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

#include "plog.h"
#include "SR_RecognizerResult.h"
#include "SR_RecognizerResultImpl.h"


ESR_ReturnCode SR_RecognizerResultGetWaveform(const SR_RecognizerResult* self, 
																							const asr_int16_t** waveform, size_t* size)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getWaveform(self, waveform, size);
}

ESR_ReturnCode SR_RecognizerResultGetSize(const SR_RecognizerResult* self, size_t* count)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getSize(self, count);
}

ESR_ReturnCode SR_RecognizerResultGetKeyCount(const SR_RecognizerResult* self, const size_t nbest, size_t* count)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getKeyCount(self, nbest, count);
}

ESR_ReturnCode SR_RecognizerResultGetKeyList(const SR_RecognizerResult* self, const size_t nbest, LCHAR** list, size_t* size)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getKeyList(self, nbest, list, size);
}

ESR_ReturnCode SR_RecognizerResultGetValue(const SR_RecognizerResult* self, const size_t nbest, const LCHAR* key, LCHAR* value, size_t* len)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getValue(self, nbest, key, value, len);
}

ESR_ReturnCode SR_RecognizerResultGetLocale(const SR_RecognizerResult* self, ESR_Locale* locale)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getLocale(self, locale);
}
