/*---------------------------------------------------------------------------*
 *  EventLog.c  *
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

#include "SR_EventLog.h"
#include "SR_EventLogImpl.h"
#include "pmemory.h"
#include "plog.h"


ESR_ReturnCode SR_EventLogDestroy(SR_EventLog* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->destroy(self);
}

ESR_ReturnCode SR_EventLogToken(SR_EventLog* self, const LCHAR* token, const LCHAR *value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->token(self, token, value);
}

ESR_ReturnCode SR_EventLogTokenInt(SR_EventLog* self, const LCHAR* token, int value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->tokenInt(self, token, value);
}

ESR_ReturnCode SR_EventLogTokenUint16_t(SR_EventLog* self, const LCHAR* token, asr_uint16_t value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->tokenUint16_t(self, token, value);
}

ESR_ReturnCode SR_EventLogTokenSize_t(SR_EventLog* self, const LCHAR* token, size_t value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->tokenSize_t(self, token, value);
}

ESR_ReturnCode SR_EventLogTokenBool(SR_EventLog* self, const LCHAR* token, ESR_BOOL value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->tokenBool(self, token, value);
}

ESR_ReturnCode SR_EventLogTokenFloat(SR_EventLog* self, const LCHAR* token, float value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->tokenFloat(self, token, value);
}

ESR_ReturnCode SR_EventLogEventSession(SR_EventLog* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->eventSession(self);
}

ESR_ReturnCode SR_EventLogEvent(SR_EventLog* self, const LCHAR *eventName)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->event(self, eventName);
}

ESR_ReturnCode SR_EventLogAudioOpen(SR_EventLog* self, const LCHAR* audio_type, size_t sample_rate, size_t sample_size)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->audioOpen(self, audio_type, sample_rate, sample_size);
}

ESR_ReturnCode SR_EventLogAudioClose(SR_EventLog* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->audioClose(self);
}

ESR_ReturnCode SR_EventLogAudioWrite(SR_EventLog* self, void* buffer, size_t num_bytes)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->audioWrite(self, buffer, num_bytes);
}

ESR_ReturnCode SR_EventLogAudioGetFilename(SR_EventLog* self, LCHAR* waveformFilename, size_t* len)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->audioGetFilename(self, waveformFilename, len);
}
