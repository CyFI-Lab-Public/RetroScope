/*---------------------------------------------------------------------------*
 *  SR_AcousticStateImpl.h                                                   *
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

#ifndef __SR_ACOUSTICSTATEIMPL_H
#define __SR_ACOUSTICSTATEIMPL_H



#ifndef __vxworks
#include <memory.h>
#endif
#include "SR_AcousticModels.h"
#include "SR_AcousticState.h"
#include "SR_RecognizerImpl.h"
#include "ESR_ReturnCode.h"

/**
 * SR_AcousticState implementation.
 */
typedef struct SR_AcousticStateImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  SR_AcousticState Interface;
}
SR_AcousticStateImpl;


/**
 * Default implementation.
 */
SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateCreateImpl(SR_Recognizer* recognizer);
/**
 * Default implementation.
 */
SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateDestroyImpl(SR_Recognizer* recognizer);
/**
 * Default implementation.
 */
SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateResetImpl(SR_Recognizer* recognizer);
/**
 * Default implementation.
 */
SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateLoadImpl(SR_Recognizer* recognizer, const LCHAR* filename);
/**
 * Default implementation.
 */
SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateSaveImpl(SR_Recognizer* recognizer, const LCHAR* filename);
/**
 * Default implementation.
 */
SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateSetImpl(SR_Recognizer* recognizer, const LCHAR *param_string );
/**
 * Default implementation.
 */
SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateGetImpl(SR_Recognizer* recognizer, LCHAR *param_string, size_t* len );
/**
 * Default implementation.
 */
SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateActivateModelImpl(SR_Recognizer* recognizer, SR_AcousticModelID id);
/**
 * Default implementation.
 */
SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateDeactivateModelImpl(SR_Recognizer* recognizer, SR_AcousticModelID id);


#endif /* __SR_ACOUSTICSTATEIMPL_H */
