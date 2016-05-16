/*---------------------------------------------------------------------------*
 *  SR_NametagDefs.h  *
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

#ifndef __SR_NAMETAGDEFS_H
#define __SR_NAMETAGDEFS_H



#include "SR_NametagPrefix.h"
#include "ESR_Locale.h"
#include "ptypes.h"
#include "ESR_ReturnCode.h"


/**
 * @addtogroup SR_NametagModule SR_Nametag API functions
 * Nametags are user-added words. These words are created using the audio that has been collected
 * during a recognition phase. The Recognizer stores the audio in the RecognizerResult object.
 * The result is passed to NametagCreate() to create the nametag. The Nametags object is a 
 * collection of nametags. The nametag will consist of the speech parts of the audio. The
 * silence from the ends of the utterance, and any long pause between speech chunks, is removed.
 *
 * @{
 */

/**
 * Represents a nametag.
 */
typedef struct SR_Nametag_t
{
	/**
	 * Returns the Nametag ID.
	 *
	 * @param self Nametag handle
	 * @param id Nametag ID
	 * @return ESR_INVALID_ARGUMENT if self is null
	 */
  ESR_ReturnCode (*getID)(const struct SR_Nametag_t* self, LCHAR** id);

  	/**
	 * Returns the Nametag ID.
	 *
	 * @param self Nametag handle
	 * @param pvalue pointer to the transcription, SR_Nametag retains ownership
	 * @param plen pointer to the length of the transcription
	 * @return ESR_NO_MATCH_ERROR if self is null or bad voice tag
	 */
  ESR_ReturnCode (*getValue)(const struct SR_Nametag_t* self, const char** pvalue, size_t* plen);

	/**
	 * Sets the Nametag ID.
	 *
	 * @param self Nametag handle
	 * @param id Nametag ID
	 * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
	 */
	ESR_ReturnCode (*setID)(struct SR_Nametag_t* self, const LCHAR* id);

  /**
   * Clones a nametag.
   *
   * @param self Nametag handle
   * @param result the resulting Nametag
   * @return ESR_INVALID_ARGUMENT if self is null; ESR_OUT_OF_MEMORY if system is out of memory
   */
  ESR_ReturnCode (*clone)(const struct SR_Nametag_t* self, struct SR_Nametag_t** result);

  /**
	 * Destroys a nametag.
	 *
	 * @param self Nametag handle
	 * @return ESR_SUCCESS
	 */
	ESR_ReturnCode (*destroy)(struct SR_Nametag_t* self);
} SR_Nametag;

/**
 * @}
 */

#endif /* __SR_NAMETAGDEFS_H */
