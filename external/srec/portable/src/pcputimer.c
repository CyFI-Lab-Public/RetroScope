/*---------------------------------------------------------------------------*
 *  pcputimer.c  *
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



#include "pcputimer.h"
#include "pmemory.h"

#if defined(_WIN32)

/*
  Note that this implementation assumes that GetThreadTimes is
  available (requires NT 3.5 and above) and that 64 bit arithmetic is
  available (requires VC)
*/

struct PCPUTimer_t
{
  HANDLE hThread;
  LARGE_INTEGER RefTime;
  asr_uint32_t elapsed;
};


/**
 * Creates a new timer object.
 **/
ESR_ReturnCode PCPUTimerCreate(PCPUTimer **timer)
{
  PCPUTimer *tmp = NULL;
  
  if (timer == NULL)
    return ESR_INVALID_ARGUMENT;
  tmp = NEW(PCPUTimer, "PCPUTimer");
  if (tmp == NULL) return ESR_OUT_OF_MEMORY;
  
  tmp->hThread = GetCurrentThread();
  tmp->RefTime.QuadPart = -1;
  tmp->elapsed = 0;
  *timer = tmp;
  
  return ESR_SUCCESS;
}

ESR_ReturnCode PCPUTimerDestroy(PCPUTimer *timer)
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
ESR_ReturnCode PCPUTimerStart(PCPUTimer *timer)
{
  FILETIME CreationTime;
  FILETIME ExitTime;
  FILETIME KernelTime;
  FILETIME UserTime;
  
  if (timer == NULL) return ESR_INVALID_ARGUMENT;
  if (!GetThreadTimes(timer->hThread,
                      &CreationTime, &ExitTime, &KernelTime, &UserTime))
  {
    return ESR_FATAL_ERROR;
  }
  
  timer->RefTime.QuadPart = (((LARGE_INTEGER*) & KernelTime)->QuadPart +
                             ((LARGE_INTEGER*) & UserTime)->QuadPart);
                             
  return ESR_SUCCESS;
}

/**
 * Stops the timer.
 **/
ESR_ReturnCode PCPUTimerStop(PCPUTimer *timer)
{
  if (timer == NULL) return ESR_INVALID_ARGUMENT;
  if (timer->RefTime.QuadPart != -1)
  {
    FILETIME CreationTime;
    FILETIME ExitTime;
    FILETIME KernelTime;
    FILETIME UserTime;
    
    if (!GetThreadTimes(timer->hThread,
                        &CreationTime, &ExitTime, &KernelTime, &UserTime))
      return ESR_FATAL_ERROR;
      
    timer->elapsed =
      (asr_uint32_t) (((LARGE_INTEGER*) &KernelTime)->QuadPart +
                  ((LARGE_INTEGER*) &UserTime)->QuadPart -
                  timer->RefTime.QuadPart) / 10;
  }
  return ESR_SUCCESS;
}

/**
 * Returns the timer elapsed time.  If the Timer is in the stopped state,
 * successive calls to getElapsed() will always return the same value.  If
 * the Timer is in the started state, successive calls will return the
 * elapsed time since the last time PCPUTimerStart() was called.
 */
ESR_ReturnCode PCPUTimerGetElapsed(PCPUTimer *timer, asr_uint32_t *elapsed)
{
  if (timer == NULL || elapsed == NULL) return ESR_INVALID_ARGUMENT;
  if (timer->RefTime.QuadPart != -1)
  {
    FILETIME CreationTime;
    FILETIME ExitTime;
    FILETIME KernelTime;
    FILETIME UserTime;
    
    if (!GetThreadTimes(timer->hThread,
                        &CreationTime, &ExitTime, &KernelTime, &UserTime))
      return ESR_FATAL_ERROR;
      
    *elapsed = timer->elapsed +
               (asr_uint32_t)(((LARGE_INTEGER*) & KernelTime)->QuadPart +
                              ((LARGE_INTEGER*) & UserTime)->QuadPart -
                              timer->RefTime.QuadPart) / 10;
  }
  else
    *elapsed = timer->elapsed;
  return ESR_SUCCESS;
}


/**
 * Resets the elapsed time to 0 and resets the reference time of the Timer.
 * This effectively reset the timer in the same state it was right after creation.
 **/
ESR_ReturnCode PCPUTimerReset(PCPUTimer *timer)
{
  if (timer == NULL) return ESR_INVALID_ARGUMENT;
  timer->RefTime.QuadPart = -1;
  timer->elapsed = 0;
  return ESR_SUCCESS;
}

#elif defined(POSIX)
/*
*/

struct PCPUTimer_t
{
  HANDLE   hThread;
  asr_uint32_t RefTime;
  asr_uint32_t elapsed;
};

/**
* Creates a new timer object.
**/
ESR_ReturnCode PCPUTimerCreate(PCPUTimer **timer)
{
  PCPUTimer *tmp = NULL;

  if (timer == NULL) return ESR_INVALID_ARGUMENT;
  tmp = NEW(PCPUTimer, "PCPUTimer");
  if (tmp == NULL) return ESR_OUT_OF_MEMORY;

  tmp->hThread = (HANDLE)pthread_self();
  tmp->elapsed = 0;
  *timer = tmp;

  return ESR_SUCCESS;
}

ESR_ReturnCode PCPUTimerDestroy(PCPUTimer *timer)
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
ESR_ReturnCode PCPUTimerStart(PCPUTimer *timer)
{
  return ESR_SUCCESS;
}

/**
* Stops the timer.
**/
ESR_ReturnCode PCPUTimerStop(PCPUTimer *timer)
{
  return ESR_SUCCESS;
}

/**
* Returns the timer elapsed time.  If the Timer is in the stopped state,
* successive calls to getElapsed() will always return the same value.  If
* the Timer is in the started state, successive calls will return the
* elapsed time since the last time PCPUTimerStart() was called.
*/
ESR_ReturnCode PCPUTimerGetElapsed(PCPUTimer *timer, asr_uint32_t *elapsed)
{
  return ESR_SUCCESS;
}


/**
* Resets the elapsed time to 0 and resets the reference time of the Timer.
* This effectively reset the timer in the same state it was right after creation.
**/
ESR_ReturnCode PCPUTimerReset(PCPUTimer *timer)
{
  return ESR_SUCCESS;
}

#else
/* #error "Ptimer not implemented for this platform." */
#endif
