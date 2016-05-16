/*---------------------------------------------------------------------------*
 *  pcputimer.h  *
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

#ifndef PCPUTIMER_H
#define PCPUTIMER_H



#include "PortPrefix.h"
#include "ptypes.h"

/**
 * @addtogroup PCPUTimerModule PCPUTimer API functions
 *
 * @{
 */

/** Typedef */
typedef struct PCPUTimer_t PCPUTimer;

/**
 * Creates a new timer object.
 *
 * @param timer PCPUTimer handle
 * @return ESR_INVALID_ARGUMENT if timer is value it points to is null
 */
PORTABLE_API ESR_ReturnCode PCPUTimerCreate(PCPUTimer **timer);


/**
 * Destroys timer object.
 *
 * @param timer PCPUTimer handle
 * @return ESR_INVALID_ARGUMENT if timer is null
 */
PORTABLE_API ESR_ReturnCode PCPUTimerDestroy(PCPUTimer *timer);

/**
 * Starts the timer. This sets the reference time from which all new elapsed
 * time are computed.  This does not reset the elapsed time to 0.  This is
 * useful to pause the timer.
 *
 * @return ESR_INVALID_ARGUMENT if timer is null; ESR_FATAL_ERROR if OS timer is available
 */
PORTABLE_API ESR_ReturnCode PCPUTimerStart(PCPUTimer *timer);

/**
 * Stops the timer.
 *
 * @return ESR_INVALID_ARGUMENT if timer is null; ESR_FATAL_ERROR if OS timer is available
 */
PORTABLE_API ESR_ReturnCode PCPUTimerStop(PCPUTimer *timer);

/**
 * Returns the timer elapsed time.  If the Timer is in the stopped state,
 * successive calls to getElapsed() will always return the same value.  If the
 * Timer is in the started state, successive calls will return the elapsed
 * time since the last time PCPUTimerStart() was called.
 *
 * @return ESR_INVALID_ARGUMENT if timer or elapsed to is null; ESR_FATAL_ERROR if OS timer is available
 */
PORTABLE_API ESR_ReturnCode PCPUTimerGetElapsed(PCPUTimer *timer,
    asr_uint32_t* elapsed);
    
/**
 * Resets the elapsed time to 0 and resets the reference time of the Timer.
 * This effectively reset the timer in the same state it was right after
 * creation.
 *
 * @return ESR_INVALID_ARGUMENT if timer is null
 */
PORTABLE_API ESR_ReturnCode PCPUTimerReset(PCPUTimer *timer);

/**
 * @}
 */


#endif
