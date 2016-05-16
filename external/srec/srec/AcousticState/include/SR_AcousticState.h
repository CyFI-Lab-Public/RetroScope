/*---------------------------------------------------------------------------*
 *  SR_AcousticState.h                                                       *
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

#ifndef __SR_ACOUSTICSTATE_H
#define __SR_ACOUSTICSTATE_H



#include <stddef.h>
#include <stdlib.h>
#include "SR_AcousticModels.h"
#include "SR_AcousticStatePrefix.h"
#include "SR_Recognizer.h"
#include "pstdio.h"
#include "ESR_ReturnCode.h"


/**
 * @addtogroup SR_AcousticStateModule SR_AcousticState API functions
 * Contains Acoustic state information.
 *
 * @{
 */

/**
 * Contains Acoustic state information.
 */
typedef struct SR_AcousticState_t
{
  /**
   * Resets the acoustic state object.
   * The recognizer adapts to the acoustic state of the caller and calling environment
   * during a call in order to improve recognition accuracy. The platform must reset
   * the AcousticState either at the beginning of a new call or at the end of a call in
   * order to reset acoustic state information for a new caller.
   *
   * @param recognizer SR_Recognizer handle
  * @return ESR_INVALID_ARGUMENT if recognizer is null
   */
  ESR_ReturnCode(*reset)(SR_Recognizer* recognizer);
  /**
   * Loads an AcousticState from file.
   *
   * @param recognizer SR_Recognizer handle
   * @param file File to read from
   * @return ESR_INVALID_ARGUMENT if recognizer is null
   */
  ESR_ReturnCode(*load)(SR_Recognizer* recognizer, const LCHAR* filename);
  /**
   * Saves an AcousticState to a file.
   *
   * @param recognizer SR_Recognizer handle
   * @param file File to write into
   * @return ESR_INVALID_ARGUMENT if recognizer is null
   */
  ESR_ReturnCode(*save)(SR_Recognizer* recognizer, const LCHAR* filename);
  /**
   * Gets an AcousticState into a string.
   *
   * @param recognizer SR_Recognizer handle
   * @param param_string contains data from the recognizer
   * @return ESR_INVALID_ARGUMENT if recognizer is null
   */
  ESR_ReturnCode(*get)(SR_Recognizer* recognizer, LCHAR *param_string, size_t* len );
  /**
   * Sets an AcousticState from a string.
   *
   * @param recognizer SR_Recognizer handle
   * @param param_string contains data to set
   * @return ESR_INVALID_ARGUMENT if recognizer is null
   */
  ESR_ReturnCode(*set)(SR_Recognizer* recognizer, const LCHAR *param_string );
  /**
   * Destrroys the acoustic state of a recognizer
   *
   * @param recognizer SR_Recognizer handle
  * @return ESR_INVALID_ARGUMENT if recognizer is null
   */
  ESR_ReturnCode(*destroy)(SR_Recognizer* recognizer);
}
SR_AcousticState;

/**
 * @name AcousticState
 *
 * An AcousticState is a container for the configurations of several items used in recognition:
 *
 * - What models are in use (where there is more than one available). E.g. M/F or adapted models.
 * - Properties of the channels (microphone, environment, speaker). These properties are updated
 * during recognition. E.g. There may be more than one microphone in use, offering zone-based
 * input. Each zone may require the maintenance of its own channel settings.
 *
 * AcousticState must offer persistence to reflect optimal settings and preferred use on start-up.
 *
 * @{
 */

/**
 * Loads an AcousticState from file.
 *
 * @param recognizer SR_Recognizer handle
 * @param filename File to read from
 * @return ESR_INVALID_ARGUMENT if recognizer is null
 */
SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateLoad(SR_Recognizer* recognizer, const LCHAR* filename);
/**
 * Saves an AcousticState to a file.
 *
 * @param recognizer SR_Recognizer handle
 * @param filename File to write into
 * @return ESR_NOT_IMPLEMENTED
 */
SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateSave(SR_Recognizer* recognizer, const LCHAR* filename);
/**
 * @}
 *
 * @name Possible AcousticState <-> Model operations
 *
 * @{
 */

/**
 * Resets the acoustic state object.
 * The recognizer adapts to the acoustic state of the caller and calling environment
 * during a call in order to improve recognition accuracy. The platform must reset
 * the AcousticState either at the beginning of a new call or at the end of a call in
 * order to reset acoustic state information for a new caller.
 *
 * @param recognizer SR_Recognizer handle
 * @return ESR_INVALID_ARGUMENT if recognizer is null
 */
SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateReset(SR_Recognizer* recognizer);
SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateSet ( SR_Recognizer* recognizer, const LCHAR *param_string );
SREC_ACOUSTICSTATE_API ESR_ReturnCode SR_AcousticStateGet ( SR_Recognizer* recognizer, LCHAR *param_string, size_t* len );
/**
 * @}
 */

/**
 * @}
 */

#endif /* __SR_ACOUSTICSTATE_H */
