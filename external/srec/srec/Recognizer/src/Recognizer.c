/*---------------------------------------------------------------------------*
 *  Recognizer.c  *
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

#include "SR_Recognizer.h"
#include "SR_RecognizerImpl.h"
#include "ESR_Session.h"
#include "plog.h"
#include "pmemory.h"

#define COUNT_INTERVAL      20

ESR_ReturnCode SR_RecognizerStart(SR_Recognizer* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
#ifdef SREC_ENGINE_TRACK_RECOGNITION
  PLogMessage ( "Entering Recognizer Start\n" );
#endif
  return self->start(self);
}

ESR_ReturnCode SR_RecognizerStop(SR_Recognizer* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
#ifdef SREC_ENGINE_TRACK_RECOGNITION
  PLogMessage ( "Entering Recognizer Stop\n" );
#endif
  return self->stop(self);
}

ESR_ReturnCode SR_RecognizerDestroy(SR_Recognizer* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->destroy(self);
}

ESR_ReturnCode SR_RecognizerSetup(SR_Recognizer* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->setup(self);
}

ESR_ReturnCode SR_RecognizerUnsetup(SR_Recognizer* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->unsetup(self);
}

ESR_ReturnCode SR_RecognizerIsSetup(SR_Recognizer* self, ESR_BOOL* isSetup)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->isSetup(self, isSetup);
}

ESR_ReturnCode SR_RecognizerGetParameter(SR_Recognizer* self, const LCHAR* name, LCHAR* value, size_t* len)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getParameter(self, name, value, len);
}

ESR_ReturnCode SR_RecognizerGetSize_tParameter(SR_Recognizer* self, const LCHAR* name, size_t* value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getSize_tParameter(self, name, value);
}

ESR_ReturnCode SR_RecognizerGetBoolParameter(SR_Recognizer* self, const LCHAR* name, ESR_BOOL* value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getBoolParameter(self, name, value);
}

ESR_ReturnCode SR_RecognizerSetParameter(SR_Recognizer* self, const LCHAR* name, LCHAR* value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->setParameter(self, name, value);
}

ESR_ReturnCode SR_RecognizerSetSize_tParameter(SR_Recognizer* self, const LCHAR* name, size_t value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  if ( LSTRCMP( L("CREC.Frontend.samplerate"), name ) == 0 )
    return SR_Recognizer_Change_Sample_Rate ( self, value );
  else
    return self->setSize_tParameter(self, name, value);
}

ESR_ReturnCode SR_RecognizerSetBoolParameter(SR_Recognizer* self, const LCHAR* name, ESR_BOOL value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->setBoolParameter(self, name, value);
}

ESR_ReturnCode SR_RecognizerSetupRule(SR_Recognizer* self, SR_Grammar* grammar,
    const LCHAR* ruleName)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->setupRule(self, grammar, ruleName);
}

ESR_ReturnCode SR_RecognizerHasSetupRules(SR_Recognizer* self,
    ESR_BOOL* hasSetupRules)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->hasSetupRules(self, hasSetupRules);
}

ESR_ReturnCode SR_RecognizerActivateRule(SR_Recognizer* self, SR_Grammar* grammar,
    const LCHAR* ruleName, unsigned int weight)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->activateRule(self, grammar, ruleName, weight);
}

ESR_ReturnCode SR_RecognizerDeactivateRule(SR_Recognizer* self, SR_Grammar* grammar,
    const LCHAR* ruleName)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->deactivateRule(self, grammar, ruleName);
}

ESR_ReturnCode SR_RecognizerDeactivateAllRules(SR_Recognizer* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->deactivateAllRules(self);
}

ESR_ReturnCode SR_RecognizerIsActiveRule(SR_Recognizer* self, SR_Grammar* grammar,
    const LCHAR* ruleName, ESR_BOOL* isActiveRule)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->isActiveRule(self, grammar, ruleName, isActiveRule);
}

ESR_ReturnCode SR_RecognizerCheckGrammarConsistency(SR_Recognizer* self, SR_Grammar* grammar,
    ESR_BOOL* isConsistent)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->checkGrammarConsistency(self, grammar, isConsistent);
}

ESR_ReturnCode SR_RecognizerGetModels(SR_Recognizer* self, SR_AcousticModels** pmodels)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getModels(self, pmodels);
}

ESR_ReturnCode SR_RecognizerPutAudio(SR_Recognizer* self, asr_int16_t* buffer, size_t* bufferSize,
                                     ESR_BOOL isLast)
{
#ifdef SREC_ENGINE_TRACK_RECOGNITION
  static int counter = 0;
#endif

  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
#ifdef SREC_ENGINE_TRACK_RECOGNITION
  if ( ( counter % COUNT_INTERVAL ) == 0 )
    PLogMessage ( "Entering Recognizer Put Audio %d Times\n", counter );
  counter++;
#endif
  return self->putAudio(self, buffer, bufferSize, isLast);
}

ESR_ReturnCode SR_RecognizerAdvance(SR_Recognizer* self, SR_RecognizerStatus* status,
                                    SR_RecognizerResultType* type,
                                    SR_RecognizerResult** result)
{
#ifdef SREC_ENGINE_TRACK_RECOGNITION
  static int counter = 0;
#endif

  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
#ifdef SREC_ENGINE_TRACK_RECOGNITION
  if ( ( counter % COUNT_INTERVAL ) == 0 )
    PLogMessage ( "Entering Recognizer Advance %d Times\n", counter );
  counter++;
#endif
  return self->advance(self, status, type, result);
}

ESR_ReturnCode SR_RecognizerLoadUtterance(SR_Recognizer* self, const LCHAR* filename)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->loadUtterance(self, filename);
}

ESR_ReturnCode SR_RecognizerLoadWaveFile(SR_Recognizer* self, const LCHAR* filename)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->loadWaveFile(self, filename);
}

ESR_ReturnCode SR_RecognizerLogEvent(SR_Recognizer* self, const LCHAR* event)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->logEvent(self, event);
}

ESR_ReturnCode SR_RecognizerLogToken(SR_Recognizer* self, const LCHAR* token, const LCHAR* value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->logToken(self, token, value);
}

ESR_ReturnCode SR_RecognizerLogTokenInt(SR_Recognizer* self, const LCHAR* token, int value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->logTokenInt(self, token, value);
}


ESR_ReturnCode SR_RecognizerLogSessionStart(SR_Recognizer* self, const LCHAR* sessionName)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->logSessionStart(self, sessionName);
}


ESR_ReturnCode SR_RecognizerLogSessionEnd(SR_Recognizer* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->logSessionEnd(self);
}


ESR_ReturnCode SR_RecognizerLogWaveformData(SR_Recognizer* self,
    const LCHAR* waveformFilename,
    const LCHAR* transcription,
    const double bos,
    const double eos,
    ESR_BOOL isInvocab)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->logWaveformData(self, waveformFilename, transcription, bos, eos, isInvocab);
}


ESR_ReturnCode SR_RecognizerSetLockFunction(SR_Recognizer* self, SR_RecognizerLockFunction function, void* data)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->setLockFunction(self, function, data);
}

ESR_ReturnCode SR_RecognizerIsSignalClipping(SR_Recognizer* self, ESR_BOOL* isClipping)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->isSignalClipping(self, isClipping);
}

ESR_ReturnCode SR_RecognizerIsSignalDCOffset(SR_Recognizer* self, ESR_BOOL* isDCOffset)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->isSignalDCOffset(self, isDCOffset);
}

ESR_ReturnCode SR_RecognizerIsSignalNoisy(SR_Recognizer* self, ESR_BOOL* isNoisy)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->isSignalNoisy(self, isNoisy);
}

ESR_ReturnCode SR_RecognizerIsSignalTooQuiet(SR_Recognizer* self, ESR_BOOL* isTooQuiet)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->isSignalTooQuiet(self, isTooQuiet);
}

ESR_ReturnCode SR_RecognizerIsSignalTooFewSamples(SR_Recognizer* self, ESR_BOOL* isTooFewSamples)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->isSignalTooFewSamples(self, isTooFewSamples);
}

ESR_ReturnCode SR_RecognizerIsSignalTooManySamples(SR_Recognizer* self, ESR_BOOL* isTooManySamples)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->isSignalTooManySamples(self, isTooManySamples);
}



ESR_ReturnCode SR_Recognizer_Change_Sample_Rate ( SR_Recognizer *recognizer, size_t new_sample_rate )
    {
    ESR_ReturnCode  change_status;

    if ( recognizer != NULL )
        {
        change_status = SR_Recognizer_Change_Sample_RateImpl ( recognizer, new_sample_rate );
        }
    else
        {
        change_status = ESR_INVALID_ARGUMENT;
        PLogError ( L("ESR_INVALID_ARGUMENT") );
        }
    return ( change_status );
    }


