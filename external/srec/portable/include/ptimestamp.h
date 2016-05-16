/*---------------------------------------------------------------------------*
 *  ptimestamp.h  *
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

#ifndef PTIMESTAMP_H
#define PTIMESTAMP_H



#include <time.h>
#include "PortPrefix.h"
#include "ptypes.h"

/**
 * @addtogroup PTimeStampModule PTimeStamp API functions
 *
 * @{
 */

/**
 * Time stamp structure with two fields: seconds and milliseconds.  The secs
 * field represent the number of seconds since January 1st 1970, 00:00:00 UTC.
 * msecs represent the number of milliseconds within that second.
 **/
typedef struct PTimeStamp_t
{
  /**
   * Seconds component of timestamp.
   */
  time_t secs;
  
  /**
   * Milliseconds component of timestamp.
   */
  asr_uint16_t msecs;
}
PTimeStamp;

/**
 * Sets the time stamp to represent current time.  Sets both secs field and
 * msecs field to 0 if platform does not support it.
 **/
PORTABLE_API void PTimeStampSet(PTimeStamp *timestamp);

/**
 * Returns the difference between two timestamps, in terms of milliseconds.
 *
 * @param a First timestamp
 * @param b Second timestamp
 * @return a - b
 */
PORTABLE_API int PTimeStampDiff(const PTimeStamp *a, const PTimeStamp *b);

/**
 * @}
 */


#endif
