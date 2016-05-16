/*---------------------------------------------------------------------------*
 *  SR_NametagsImpl.h  *
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

#ifndef __SR_NAMETAGSIMPL_H
#define __SR_NAMETAGSIMPL_H



#include "ArrayList.h"
#include "ESR_ReturnCode.h"
#include "HashMap.h"
#include "SR_EventLog.h"
#include "SR_Nametag.h"
#include "SR_Nametags.h"

/**
 * Nametags implementation.
 */
typedef struct SR_NametagsImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  SR_Nametags Interface;
  /**
   * Nametag collection.
   */
  HashMap* value;
  /**
   * OSI event-log.
   */
  SR_EventLog* eventLog;
  /**
   * Eventlog logging level.
   */
  size_t logLevel;
}
SR_NametagsImpl;


/**
 * Default implementation.
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsLoadImpl(SR_Nametags* self, const LCHAR* filename);
/**
 * Default implementation.
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsSaveImpl(SR_Nametags* self, const LCHAR* filename);
/**
 * Default implementation.
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsAddImpl(SR_Nametags* self, SR_Nametag* nametag);
/**
 * Default implementation.
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsRemoveImpl(SR_Nametags* self, const LCHAR* id);
/**
 * Default implementation.
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsGetSizeImpl(SR_Nametags* self, size_t* result);
/**
 * Default implementation.
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsGetImpl(SR_Nametags* self, const LCHAR* id, SR_Nametag** nametag);
/**
 * Default implementation.
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsGetAtIndexImpl(SR_Nametags* self, size_t index, SR_Nametag** nametag);
/**
 * Default implementation.
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsContainsImpl(SR_Nametags* self, const LCHAR* id, ESR_BOOL* result);
/**
 * Default implementation.
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsDestroyImpl(SR_Nametags* self);

#endif /* __SR_NAMETAGSIMPL_H */
