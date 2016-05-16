/*---------------------------------------------------------------------------*
 *  SR_Nametags.h  *
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

#ifndef __SR_NAMETAGS_H
#define __SR_NAMETAGS_H



#include "SR_NametagsPrefix.h"
#include "ptypes.h"
#include "pstdio.h"
//#include "SR_Recognizer.h"
#include "SR_NametagDefs.h"
#include "ESR_ReturnCode.h"


/**
 * @addtogroup SR_NametagsModule SR_Nametags API functions
 * Represents a Nametag collection.
 *
 * @{
 */

/**
 * Represents a Nametag collection.
 */
typedef struct SR_Nametags_t
{
  /**
   * Loads a nametag collection.
   *
   * @param self Nametags handle
   * @param filename File to read from
   */
  ESR_ReturnCode(*load)(struct SR_Nametags_t* self, const LCHAR* filename);
  
  /**
   * Saves a nametag collection.
   *
   * @param self Nametags handle
   * @param filename File to write to
   */
  ESR_ReturnCode(*save)(struct SR_Nametags_t* self, const LCHAR* filename);
  
  /**
   * Adds nametag to collection.
   *
   * @param self Nametags handle
   * @param nametag Nametag to be added
   */
  ESR_ReturnCode(*add)(struct SR_Nametags_t* self, SR_Nametag* nametag);
  
  /**
   * Removes nametag from collection.
   *
   * @param self Nametags handle
   * @param id ID of nametag to be removed
   */
  ESR_ReturnCode(*remove)(struct SR_Nametags_t* self, const LCHAR* id);
  
  /**
   * Returns the number of nametags within the collection.
   *
   * @param self Nametags handle
   * @param result Resulting value
   */
  ESR_ReturnCode(*getSize)(struct SR_Nametags_t* self, size_t* result);
  
  /**
   * Returns Nametag with the specified ID. It is illegal to destroy the returned Nametag
   * until it is removed from the Nametags collection.
   *
   * @param self Nametags handle
   * @param ud Nametag id
   * @param nametag Nametag at index
   */
  ESR_ReturnCode(*get)(struct SR_Nametags_t* self, const LCHAR* id, SR_Nametag** nametag);
  
  /**
   * Returns Nametag at the specified index. It is illegal to destroy the returned Nametag
   * until it is removed from the Nametags collection.
   *
   * @param self Nametags handle
   * @param index Nametag index
   * @param nametag Nametag at index
   */
  ESR_ReturnCode(*getAtIndex)(struct SR_Nametags_t* self, size_t index, SR_Nametag** nametag);
  
  /**
   * Indicates if collection contains specified nametag.
   *
   * @param self Nametags handle
   * @param id Nametag ID to search for
    * @param result True if nametag was found
   */
  ESR_ReturnCode(*contains)(struct SR_Nametags_t* self, const LCHAR* id, ESR_BOOL* result);
  
  /**
  * Destroys a nametag collection.
  *
  * @param self Nametags handle
  */
  ESR_ReturnCode(*destroy)(struct SR_Nametags_t* self);
}
SR_Nametags;

/**
 * @name Nametags operations
 *
 * @{
 */

/**
 * Create a new Nametag collection.
 *
 * @param self Nametags handle
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsCreate(SR_Nametags** self);

/**
 * Loads a nametag collection.
 *
 * @param self Nametags handle
 * @param filename File to read from
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsLoad(SR_Nametags* self, const LCHAR* filename);

/**
 * Saves a nametag collection.
 *
 * @param self Nametags handle
 * @param filename File to write to
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsSave(SR_Nametags* self, const LCHAR* filename);

/**
 * Adds nametag to collection.
 *
 * @param self Nametags handle
 * @param nametag Nametag to be added
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsAdd(SR_Nametags* self, SR_Nametag* nametag);

/**
 * Removes nametag from collection.
 *
 * @param self Nametags handle
 * @param id ID of nametag to be removed
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsRemove(SR_Nametags* self, const LCHAR* id);

/**
 * Returns the number of nametags within the collection.
 *
 * @param self Nametags handle
 * @param result Resulting value
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsGetSize(SR_Nametags* self, size_t* result);

/**
 * Returns Nametag with the specified ID. It is illegal to destroy the returned Nametag
 * until it is removed from the Nametags collection.
 *
 * @param self Nametags handle
 * @param id Nametag ID
 * @param nametag Nametag at index
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsGet(SR_Nametags* self, const LCHAR* id, SR_Nametag** nametag);

/**
 * Returns Nametag at the specified index. It is illegal to destroy the returned Nametag
 * until it is removed from the Nametags collection.
 *
 * @param self Nametags handle
 * @param index Nametag index
 * @param nametag Nametag at index
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsGetAtIndex(SR_Nametags* self, size_t index, SR_Nametag** nametag);

/**
 * Indicates if collection contains specified nametag.
 *
 * @param self Nametags handle
 * @param id Nametag ID to search for
 * @param result True if nametag was found
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsContains(SR_Nametags* self, const LCHAR* id, ESR_BOOL* result);

/**
 * Destroys a Nametag collection.
 *
 * @param self Nametag handle
 */
SREC_NAMETAG_API ESR_ReturnCode SR_NametagsDestroy(SR_Nametags* self);

/**
 * @}
 */

/**
 * @}
 */

#endif /* __SR_NAMETAGS_H */
