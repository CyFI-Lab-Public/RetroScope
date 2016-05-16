/*---------------------------------------------------------------------------*
 *  SR_Session.h  *
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

#ifndef __SR_SESSION_H
#define __SR_SESSION_H



#include "ESR_ReturnCode.h"
#include "SR_SessionPrefix.h"
#include "ptypes.h"

/**
 * @addtogroup SR_SessionModule SR_Session API functions
 * - SR_Session inherits from @ref ESR_SessionTypeModule "ESR_SessionType." As a consequence,
 *   you may invoke any @ref ESR_SessionTypeModule "ESR_SessionType" functions on it.
 *   For example: @ref ESR_SessionGetSize "ESR_SessionGetSize(size)" is legal
 *
 * @{
 */

/**
 * Initializes the SREC session.
 *
 * @param filename File to read session information from
 * @return ESR_OPEN_ERROR if file cannot be opened; ESR_READ_ERROR if file cannot be read;
 * ESR_OUT_OF_MEMORY if system is out of memory
 */
SREC_SESSION_API ESR_ReturnCode SR_SessionCreate(const LCHAR* filename);

/**
 * Destroys the SREC session.
 *
 * @return ESR_SUCCESS
 */
SREC_SESSION_API ESR_ReturnCode SR_SessionDestroy(void);

/**
 * @}
 */


#endif /* __SR_SESSION_H */
