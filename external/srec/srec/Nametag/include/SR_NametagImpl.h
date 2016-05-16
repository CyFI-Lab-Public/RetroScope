/*---------------------------------------------------------------------------*
 *  SR_NametagImpl.h  *
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

#ifndef __SR_NAMETAGIMPL_H
#define __SR_NAMETAGIMPL_H



#include "ESR_ReturnCode.h"
#include "SR_Nametag.h"

/**
 * Nametag implementation.
 */
typedef struct SR_NametagImpl_t
{
  /**
   * Interface functions that must be implemented.
   */
  SR_Nametag Interface;
  /**
   * Nametag ID.
   */
  LCHAR* id;
  /**
   * Nametag phoneme value.
   */
  LCHAR* value;
  size_t value_len;
}
SR_NametagImpl;


/**
 * Default implementation.
 */
SREC_NAMETAG_API ESR_ReturnCode SR_Nametag_GetID(const SR_Nametag* self, LCHAR** id);
/**
 * Default implementation.
 */
SREC_NAMETAG_API ESR_ReturnCode SR_Nametag_GetValue(const SR_Nametag* self, const char** pvalue, size_t *plen);
/**
 * Default implementation.
 */
SREC_NAMETAG_API ESR_ReturnCode SR_Nametag_SetID(SR_Nametag* self, const LCHAR* id);
/**
 * Default implementation.
 */
SREC_NAMETAG_API ESR_ReturnCode SR_Nametag_Clone(const SR_Nametag* self, SR_Nametag** clone);
/**
 * Default implementation.
 */
SREC_NAMETAG_API ESR_ReturnCode SR_Nametag_Destroy(SR_Nametag* self);

#endif /* __SR_NAMETAGIMPL_H */
