/*---------------------------------------------------------------------------*
 *  ptimer.h  *
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

#ifndef PTIMER_H
#define PTIMER_H



#include "PortPrefix.h"
#include "ptypes.h"

/**
 * @addtogroup PTimerModule PTimer API functions
 * API to facilitate computing elapsed time of operations.  The units of time
 * are milliseconds.
 *
 * @{
 */

/** Typedef */
typedef struct PTimer_t PTimer;

/**
 * Creates a new timer object.
 *
 * @param timer PTimer handle.
 */
PORTABLE_API ESR_ReturnCode PTimerCreate(PTimer **timer);

/**
 * Destroys the timer object.
 *
 * @param timer PTimer handle.
 */
PORTABLE_API ESR_ReturnCode PTimerDestroy(PTimer *timer);

/**
 * Starts the timer. This sets the reference time from which all new elapsed
 * time are computed.  This does not reset the elapsed time to 0.  This is
 * useful to pause the timer.
 **/
PORTABLE_API ESR_ReturnCode PTimerStart(PTimer *timer);

/**
 * Stops the timer.
 **/
PORTABLE_API ESR_ReturnCode PTimerStop(PTimer *timer);

/**
 * Returns the timer elapsed time.  If the Timer is in the stopped state,
 * successive calls to getElapsed() will always return the same value.  If the
 * Timer is in the started state, successive calls will return the elapsed
 * time since the last time PTimerStart() was called.
 */
PORTABLE_API ESR_ReturnCode PTimerGetElapsed(PTimer *timer,
    asr_uint32_t *elapsed);
    
/**
 * Resets the elapsed time to 0 and resets the reference time of the Timer.
 * This effectively reset the timer in the same state it was right after
 * creation.
 **/
PORTABLE_API ESR_ReturnCode PTimerReset(PTimer *timer);

/**
 * @}
 */


#endif
