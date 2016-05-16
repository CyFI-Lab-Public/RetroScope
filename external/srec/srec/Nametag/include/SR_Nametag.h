/*---------------------------------------------------------------------------*
 *  SR_Nametag.h  *
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

#ifndef __SR_NAMETAG_H
#define __SR_NAMETAG_H



#include "SR_NametagDefs.h"
#include "SR_Recognizer.h"
#include "SR_RecognizerResult.h"

/**
 * Create a new Nametag from the last recognition result.
 *
 * @param result RecognizerResult handle
 * @param id Nametag id
 * @param self Nametag handle
 * @return ESR_OUT_OF_MEMORY if the system is out of memory; ESR_INVALID_ARGUMENT if result, self or the
 * value pointed to by value are null; ESR_INVALID_STATE if the nametag value is invalid
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagCreate(const SR_RecognizerResult* result,
    const LCHAR* id,
    SR_Nametag** self);

/**
 * Create a new Nametag from the persistent storage
 *
 * @param id Nametag id
 * @param value Transcription content (opaque)
 * @param len length of the value (number of opaque bytes)
 * @param self Nametag handle
 * @return ESR_OUT_OF_MEMORY if the system is out of memory; ESR_INVALID_ARGUMENT if result, self or the
 * value pointed to by value are null; ESR_INVALID_STATE if the nametag value is invalid
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagCreateFromValue(const LCHAR* id, const char* value, size_t len, SR_Nametag** self);
  
/**
 * Gets the nametag ID.
 *
 * @param self Nametag handle
 * @param id Nametag id
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagGetID(const SR_Nametag* self, LCHAR** id);

/**
 * Gets the nametag transcription.
 *
 * @param self Nametag handle
 * @param id Nametag id
 * @return ESR_INVALID_ARGUMENT if self is null
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagGetValue(const SR_Nametag* self, const char** pvalue, size_t *plen);

/**
 * Sets the nametag ID.
 *
 * @param self Nametag handle
 * @param id Nametag id
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagSetID(SR_Nametag* self, const LCHAR* id);

/**
 * Clones a nametag.
 *
 * @param self Nametag handle
 * @param result the resulting Nametag
 * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagClone(const SR_Nametag* self, SR_Nametag** result);

/**
 * Destroys a Nametag.
 *
 * @param self Nametag handle
 * @return ESR_SUCCESS
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagDestroy(SR_Nametag* self);

/**
 * @}
 */


#endif /* __SR_NAMETAG_H */
