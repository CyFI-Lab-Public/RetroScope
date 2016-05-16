/*---------------------------------------------------------------------------*
 *  SR_AcousticModelsImpl.h  *
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

#ifndef __SR_ACOUSTICMODELSIMPL_H
#define __SR_ACOUSTICMODELSIMPL_H



#ifndef __vxworks
#include <memory.h>
#endif
#include "SR_AcousticModels.h"
#include "SR_RecognizerImpl.h"
#include "ESR_ReturnCode.h"

/* Legacy CREC headers */
#include "simapi.h"

/**
 * AcousticModels implementation.
 */
typedef struct SR_AcousticModelsImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  SR_AcousticModels Interface;
  
  /**
   * When AcousticModels are associated with a Recognizer, they initialize their 
   * Pattern objects using that Recognizer.
   *
   * @param self SR_AcousticModels handle
   * @param recognizer The recognizer
   */
  ESR_ReturnCode(*setupPattern)(SR_AcousticModels* self, SR_Recognizer* recognizer);
  /**
   * When AcousticModels are deassociated with a Recognizer, they deinitialize their 
   * Pattern objects.
   *
   * @param self SR_AcousticModels handle
   */
  ESR_ReturnCode(*unsetupPattern)(SR_AcousticModels* self);
  /**
   * Generate legacy AcousticModels parameter structure from ESR_Session.
   *
   * @param params Resulting structure
   */
  ESR_ReturnCode(*getLegacyParameters)(CA_AcoustInputParams* params);
  
  /**
   * AcousticModels parameters.
   */
  HashMap* parameters;
  /**
   * Legacy CREC pattern.
   */
  CA_Pattern* pattern;
  /**
   * ArrayList of legacy CREC acoustic models.
   */
  ArrayList* acoustic;
  /**
   * Legacy Arbdata structure.
   */
  CA_Arbdata* arbdata;
  /**
   * Contents of AcousticModels.
   */
  void* contents;
  /**
   * Size of contents.
   */
  int size;
}
SR_AcousticModelsImpl;


/**
 * Default implementation.
 */
SREC_ACOUSTICMODELS_API ESR_ReturnCode SR_AcousticModels_Destroy(SR_AcousticModels* self);
/**
 * Default implementation.
 */
SREC_ACOUSTICMODELS_API ESR_ReturnCode SR_AcousticModels_Save(SR_AcousticModels* self,
    const LCHAR* filename);
/**
 * Default implementation.
 */
SREC_ACOUSTICMODELS_API ESR_ReturnCode SR_AcousticModels_SetParameter(SR_AcousticModels* self,
    const LCHAR* key,
    LCHAR* value);
/**
 * Default implementation.
 */
SREC_ACOUSTICMODELS_API ESR_ReturnCode SR_AcousticModels_GetParameter(SR_AcousticModels* self,
    const LCHAR* key,
    LCHAR* value, size_t* len);
/**
 * Default implementation.
 */
SREC_ACOUSTICMODELS_API ESR_ReturnCode SR_AcousticModels_GetCount(SR_AcousticModels* self,
    size_t* size);
/**
 * Default implementation.
 */
SREC_ACOUSTICMODELS_API ESR_ReturnCode SR_AcousticModels_GetID(SR_AcousticModels* self,
    size_t index,
    SR_AcousticModelID* id,
    size_t* size);
/**
 * Default implementation.
 */
SREC_ACOUSTICMODELS_API ESR_ReturnCode SR_AcousticModels_SetID(SR_AcousticModels* self,
    size_t index,
    SR_AcousticModelID* id);
/**
 * Default implementation.
 */
SREC_ACOUSTICMODELS_API void* SR_AcousticModels_GetArbdata(SR_AcousticModels* self);

/**
 * When AcousticModels are associated with a Recognizer, they initialize their
 * Pattern objects using that Recognizer.
 *
 * @param self SR_AcousticModels handle
 * @param recognizer The recognizer
 */
ESR_ReturnCode SR_AcousticModels_SetupPattern(SR_AcousticModels* self, SR_Recognizer* recognizer);
/**
 * When AcousticModels are deassociated with a Recognizer, they deinitialize their
 * Pattern objects.
 *
 * @param self SR_AcousticModels handle
 */
ESR_ReturnCode SR_AcousticModels_UnsetupPattern(SR_AcousticModels* self);
/**
 * Generate legacy AcousticModels parameter structure from ESR_Session.
 *
 * @param params Resulting structure
 */
ESR_ReturnCode SR_AcousticModels_GetLegacyParameters(CA_AcoustInputParams* params);


#endif /* __SR_ACOUSTICMODELSIMPL_H */
