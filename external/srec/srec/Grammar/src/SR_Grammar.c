/*---------------------------------------------------------------------------*
 *  SR_Grammar.c  *
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



#include "SR_Grammar.h"
#include "SR_GrammarImpl.h"
#include "plog.h"
#include "pmemory.h"


ESR_ReturnCode SR_GrammarCompile(SR_Grammar* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->compile(self);
}

ESR_ReturnCode SR_GrammarAddWordToSlot(SR_Grammar* self, const LCHAR* slot, const LCHAR* word, 
				        const LCHAR* pronunciation, int weight, const LCHAR* tag)
    {

    if ( self == NULL )
        {
        PLogError(L("ESR_INVALID_ARGUMENT : Grammar Is Null"));
        return ESR_INVALID_ARGUMENT;
        }
    return self->addWordToSlot(self, slot, word, pronunciation, weight, tag);
    }


ESR_ReturnCode SR_GrammarResetAllSlots(SR_Grammar* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->resetAllSlots(self);
}

ESR_ReturnCode SR_GrammarAddNametagToSlot(SR_Grammar* self, const LCHAR* slot, 
                                          const SR_Nametag* nametag,  int weight, const LCHAR* tag)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->addNametagToSlot(self, slot, nametag, weight, tag);
}

ESR_ReturnCode SR_GrammarSetDispatchFunction(SR_Grammar* self, const LCHAR* name, void* userData, SR_GrammarDispatchFunction function)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->setDispatchFunction(self, name, userData, function);
}

ESR_ReturnCode SR_GrammarSave(SR_Grammar* self, const LCHAR* filename)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->save(self, filename);
}

ESR_ReturnCode SR_GrammarSetParameter(SR_Grammar* self, const LCHAR* key, void* value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->setParameter(self, key, value);
}

ESR_ReturnCode SR_GrammarSetSize_tParameter(SR_Grammar* self, const LCHAR* key, size_t value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->setSize_tParameter(self, key, value);
}

ESR_ReturnCode SR_GrammarGetParameter(SR_Grammar* self, const LCHAR* key, void** value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getParameter(self, key, value);
}

ESR_ReturnCode SR_GrammarGetSize_tParameter(SR_Grammar* self, const LCHAR* key, size_t* value)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getSize_tParameter(self, key, value);
}

// ESR_ReturnCode SR_GrammarSetupModels(SR_Grammar* self, SR_AcousticModels* models)
ESR_ReturnCode SR_GrammarSetupRecognizer(SR_Grammar* self, struct SR_Recognizer_t* recognizer)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  // retgurn self->setupModels( self, models);
  return self->setupRecognizer(self, recognizer);
}
ESR_ReturnCode SR_GrammarUnsetupRecognizer(SR_Grammar* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->unsetupRecognizer(self);
}

ESR_ReturnCode SR_GrammarSetupVocabulary(SR_Grammar *self, SR_Vocabulary *vocabulary)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->setupVocabulary(self, vocabulary);
}

/* ESR_ReturnCode SR_GrammarGetModels(SR_Grammar* self, SR_AcousticModels** models)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->getModels(self, models);
}
*/
ESR_ReturnCode SR_GrammarCheckParse(SR_Grammar* self, const LCHAR* transcription, SR_SemanticResult** result_void, size_t* resultCount)
{
  SR_SemanticResult** result = (SR_SemanticResult**)result_void;
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->checkParse(self, transcription, result, resultCount);
}

ESR_ReturnCode SR_GrammarDestroy(SR_Grammar* self)
{
  if (self == NULL)
  {
    PLogError(L("ESR_INVALID_ARGUMENT"));
    return ESR_INVALID_ARGUMENT;
  }
  return self->destroy(self);
}
