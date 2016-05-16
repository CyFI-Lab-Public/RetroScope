/*---------------------------------------------------------------------------*
 *  ptimer.c  *
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



#include "pmemory.h"
#include "ptimer.h"
#include "pmutex.h"

#ifdef _WIN32

/*
  Note that this implementation assumes that QueryPerformanceCounter is
  available (requires NT 3.1 and above) and that 64 bit arithmetic is
  available (requires VC)
*/

struct PTimer_t
{
  LARGE_INTEGER PerformanceFreq;
  LARGE_INTEGER RefTime;
  LARGE_INTEGER elapsed;
};



/**
 * Creates a new timer object.
 **/
ESR_ReturnCode PTimerCreate(PTimer **timer)
{
  PTimer *tmp = NULL;
  
  if (timer == NULL)
    return ESR_INVALID_ARGUMENT;
  tmp = NEW(PTimer, "PTimer");
  if (tmp == NULL)
    return ESR_OUT_OF_MEMORY;
    
  if (QueryPerformanceFrequency(&tmp->PerformanceFreq) == 0)
  {
    FREE(tmp);
    return ESR_NOT_SUPPORTED;
  }
  tmp->PerformanceFreq.QuadPart /= 1000;
  
  tmp->RefTime.QuadPart = 0;
  tmp->elapsed.QuadPart = 0;
  *timer = tmp;
  return ESR_SUCCESS;
}

ESR_ReturnCode PTimerDestroy(PTimer *timer)
{
  if (timer == NULL) return ESR_INVALID_ARGUMENT;
  FREE(timer);
  return ESR_SUCCESS;
}

/**
 * Starts the timer. This sets the reference time from which all new elapsed
 * time are computed.  This does not reset the elapsed time to 0.  This is
 * useful to pause the timer.
 **/
ESR_ReturnCode PTimerStart(PTimer *timer)
{
  if (timer == NULL) return ESR_INVALID_ARGUMENT;
  return (QueryPerformanceCounter(&timer->RefTime) ?
          ESR_SUCCESS :
          ESR_NOT_SUPPORTED);
}

/**
 * Stops the timer.
 **/
ESR_ReturnCode PTimerStop(PTimer *timer)
{
  if (timer == NULL) return ESR_INVALID_ARGUMENT;
  if (timer->RefTime.QuadPart != 0)
  {
    LARGE_INTEGER now;
    if (!QueryPerformanceCounter(&now)) return ESR_NOT_SUPPORTED;
    timer->elapsed.QuadPart += now.QuadPart - timer->RefTime.QuadPart;
    timer->RefTime.QuadPart = 0;
  }
  return ESR_SUCCESS;
}

/**
 * Returns the timer elapsed time.  If the Timer is in the stopped state,
 * successive calls to getElapsed() will always return the same value.  If
 * the Timer is in the started state, successive calls will return the
 * elapsed time since the last time PTimerStart() was called.
 */
ESR_ReturnCode PTimerGetElapsed(PTimer *timer, asr_uint32_t* elapsed)
{
  if (timer == NULL || elapsed == NULL)
    return ESR_INVALID_ARGUMENT;
    
  if (timer->RefTime.QuadPart != 0)
  {
    LARGE_INTEGER now;
    if (!QueryPerformanceCounter(&now)) return ESR_NOT_SUPPORTED;
    *elapsed = (asr_uint32_t) ((timer->elapsed.QuadPart + (now.QuadPart - timer->RefTime.QuadPart))
                           / timer->PerformanceFreq.QuadPart);
  }
  else
    *elapsed = (asr_uint32_t) (timer->elapsed.QuadPart / timer->PerformanceFreq.QuadPart);

  return ESR_SUCCESS;
}


/**
 * Resets the elapsed time to 0 and resets the reference time of the Timer.
 * This effectively reset the timer in the same state it was right after creation.
 **/
ESR_ReturnCode PTimerReset(PTimer *timer)
{
  if (timer == NULL) return ESR_INVALID_ARGUMENT;
  timer->RefTime.QuadPart = 0;
  timer->elapsed.QuadPart = 0;
  return ESR_SUCCESS;
}

#elif defined(POSIX)
#include "ptrd.h"
/*
POSIX has a timer
*/
/* Clocks and timers: clock_settime, clock_gettime, clock_getres, timer_xxx and nanosleep */
#ifndef _POSIX_TIMERS
#ifndef __vxworks /* __vxworks does not define it! */
#error "Timer is not defined!"
#endif /* __vxworks */
#endif /* _POSIX_TIMERS */

#define TIMER_MAX_VAL 10000

struct PTimer_t
{
  timer_t  timer;
  asr_uint32_t elapsed;
};

/**
* Creates a new timer object.
**/
ESR_ReturnCode PTimerCreate(PTimer **timer)
{
  PTimer *tmp = NULL;

  if (timer == NULL) return ESR_INVALID_ARGUMENT;
  tmp = NEW(PTimer, "PTimer");
  if (tmp == NULL) return ESR_OUT_OF_MEMORY;

  *timer = tmp;
  if (timer_create(CLOCK_REALTIME, NULL, &(tmp->timer)) < 0)
    return ESR_NOT_SUPPORTED;

  return ESR_SUCCESS;
}

ESR_ReturnCode PTimerDestroy(PTimer *timer)
{
  if (timer == NULL) return ESR_INVALID_ARGUMENT;
  timer_delete(timer->timer);
  FREE(timer);

  return ESR_SUCCESS;
}

/**
* Starts the timer. This sets the reference time from which all new elapsed
* time are computed.  This does not reset the elapsed time to 0.  This is
* useful to pause the timer.
**/
ESR_ReturnCode PTimerStart(PTimer *timer)
{
  struct itimerspec expire_time;

  if (timer == NULL) return ESR_INVALID_ARGUMENT;

  expire_time.it_value.tv_sec = TIMER_MAX_VAL; /* set a large time for the timer */
  expire_time.it_value.tv_nsec = 0;
  return (timer_settime(timer->timer, 0, &expire_time, NULL) == 0 ?
          ESR_SUCCESS :
          ESR_NOT_SUPPORTED);
}

/**
* Stops the timer.
**/
ESR_ReturnCode PTimerStop(PTimer *timer)
{
  struct itimerspec remaining;

  if (timer == NULL) return ESR_INVALID_ARGUMENT;

  if (timer_gettime(timer->timer, &remaining) != 0) return ESR_NOT_SUPPORTED;
#if defined(__vxworks)
  timer_cancel(timer->timer);
#endif
  timer->elapsed = (asr_uint32_t) ((TIMER_MAX_VAL - remaining.it_value.tv_sec) * SECOND2MSECOND
						- remaining.it_value.tv_nsec / MSECOND2NSECOND);

  return ESR_SUCCESS;
}

/**
* Returns the timer elapsed time.  If the Timer is in the stopped state,
* successive calls to getElapsed() will always return the same value.  If
* the Timer is in the started state, successive calls will return the
* elapsed time since the last time PTimerStart() was called.
*/
ESR_ReturnCode PTimerGetElapsed(PTimer *timer, asr_uint32_t* elapsed)
{
  if (timer == NULL || elapsed == NULL)
    return ESR_INVALID_ARGUMENT;

  if (timer->elapsed == 0) /* stop is not called */
  {
    struct itimerspec remaining;
    if (timer_gettime(timer->timer, &remaining) != 0) return ESR_NOT_SUPPORTED;
    *elapsed = (asr_uint32_t) ((TIMER_MAX_VAL - remaining.it_value.tv_sec) * SECOND2MSECOND
						- remaining.it_value.tv_nsec / MSECOND2NSECOND);
  }
  else
    *elapsed = timer->elapsed;

  return ESR_SUCCESS;
}


/**
* Resets the elapsed time to 0 and resets the reference time of the Timer.
* This effectively reset the timer in the same state it was right after creation.
**/
ESR_ReturnCode PTimerReset(PTimer *timer)
{
  if (timer == NULL) return ESR_INVALID_ARGUMENT;
  timer->elapsed = 0;
  return ESR_SUCCESS;
}

#else
#error "Ptimer not implemented for this platform."
#endif
