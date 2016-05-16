/*
 * Copyright (C) 2010 NXP Semiconductors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * \file phOsalNfc_Timer.c
 * \brief OSAL Timer Implementation for linux
 *
 * Project: Trusted NFC Linux Light
 *
 * $Date: 03 aug 2009
 * $Author: Jérémie Corbier
 * $Revision: 1.0
 *
 */

#include <stdlib.h>
#include <signal.h>
#include <time.h>

#include <phOsalNfc.h>
#include <phOsalNfc_Timer.h>
#include <stdio.h>

#include <phDal4Nfc_messageQueueLib.h>

#define NSECS 1000000
#define MAX_NO_TIMERS 16

/*!
 * \struct phOsalNfc_Timer
 * Internal OSAL timer structure
 */
struct phOsalNfc_Timer
{
   timer_t handle;         /*!< System timer handle. */
   ppCallBck_t callback;   /*!< Callback to be called when timer expires. */
   void* pContext;         /*!< Callback context. */
#ifdef NXP_MESSAGING
   void *ptr;
#endif
   int nIsStopped;
};

static struct phOsalNfc_Timer timers[MAX_NO_TIMERS] =
{
   {0, NULL, NULL
#ifdef NXP_MESSAGING
     , NULL
#endif
     , 0
   },
};

#ifdef NXP_MESSAGING
extern int nDeferedCallMessageQueueId;

void phOsalNfc_Timer_DeferredCall(void *params)
{
   phOsalNfc_Timer_Msg_t *timer_msg;

   if(params == NULL)
      return;

   timer_msg = (phOsalNfc_Timer_Msg_t *)params;

   if((timer_msg != NULL) && (timer_msg->pCallBck != NULL))
      timer_msg->pCallBck(timer_msg->TimerId, timer_msg->pContext);

   if ((timer_msg->TimerId >= MAX_NO_TIMERS) || (timer_msg->TimerId < 0))
   {
      printf("Bad TimerId=%d, should be <= to %d\n", timer_msg->TimerId, MAX_NO_TIMERS);
   }
   else
   {
      if(timers[timer_msg->TimerId].ptr != NULL)
      {
         phOsalNfc_FreeMemory(timers[timer_msg->TimerId].ptr);
         timers[timer_msg->TimerId].ptr = NULL;
      }
   }
   phOsalNfc_FreeMemory(timer_msg);
}
#endif

/*!
 * \brief System timer callback.
 *        This callback is called by Linux whenever one the timers expires.  It
 *        calls the corresponding registered callback.
 *
 * \param sv structure storing the expired timer ID.
 */
static void phOsalNfc_Timer_Expired(union sigval sv)
{
   uint32_t timerid = (uint32_t)(sv.sival_int);

   if((timerid < MAX_NO_TIMERS)&&(timers[timerid].nIsStopped == 1))
   {
      //printf("phOsalNfc_Timer_Expired : Expired but already stopped TimerId=%d\n", timerid);
      return;
   }

   if(timerid < MAX_NO_TIMERS)
   {
#ifndef CYCLIC_TIMER
      phOsalNfc_Timer_Stop(timerid);
#else

#endif
#ifdef NXP_MESSAGING
      phOsalNfc_Timer_Msg_t *timer_msg;
      phOsalNfc_DeferedCalldInfo_t *osal_defer_msg;
      phDal4Nfc_Message_Wrapper_t wrapper;

      timer_msg = phOsalNfc_GetMemory(sizeof(phOsalNfc_Timer_Msg_t));
      if(timer_msg == NULL)
         phOsalNfc_RaiseException(phOsalNfc_e_NoMemory, 0);

      osal_defer_msg = phOsalNfc_GetMemory(sizeof(phOsalNfc_DeferedCalldInfo_t));
      if(osal_defer_msg == NULL)
      {
         phOsalNfc_FreeMemory(timer_msg);
         phOsalNfc_RaiseException(phOsalNfc_e_NoMemory, 0);
      }

      timer_msg->TimerId = timerid;
      timer_msg->pCallBck = timers[timerid].callback;
      timer_msg->pContext = timers[timerid].pContext;

      osal_defer_msg->pCallback = phOsalNfc_Timer_DeferredCall;
      osal_defer_msg->pParameter = timer_msg;

      wrapper.mtype = 1;
      wrapper.msg.eMsgType = PH_OSALNFC_TIMER_MSG;
      wrapper.msg.pMsgData = osal_defer_msg;
      wrapper.msg.Size = sizeof(phOsalNfc_DeferedCalldInfo_t);

      timers[timerid].ptr = osal_defer_msg;

      phDal4Nfc_msgsnd(nDeferedCallMessageQueueId, (void *)&wrapper,
         sizeof(phOsalNfc_Message_t), 0);
#else
      (timers[timerid].callback)(timerid, timers[timerid].pContext);
#endif
   }
}

static void phOsalNfc_Timer_Dummy_Cb(uint32_t timerid, void *pContext) {}

/*!
 * \brief Creates a new timer.
 *        This function checks whether there is an available timer slot.  If
 *        this is the case, then it reserves it for future usage and returns its
 *        ID.
 *
 * \return a valid timer ID or PH_OSALNFC_INVALID_TIMER_ID if an error occured.
 */
uint32_t phOsalNfc_Timer_Create(void)
{
   uint32_t timerid;
   struct sigevent se;

   se.sigev_notify = SIGEV_THREAD;
   se.sigev_notify_function = phOsalNfc_Timer_Expired;
   se.sigev_notify_attributes = NULL;

   /* Look for available timer slot */
   for(timerid = 0; timerid < MAX_NO_TIMERS; timerid++)
      if(timers[timerid].callback == NULL)
         break;
   if(timerid == MAX_NO_TIMERS)
      return PH_OSALNFC_INVALID_TIMER_ID;

   se.sigev_value.sival_int = (int)timerid;

   /* Create POSIX timer */
   if(timer_create(CLOCK_REALTIME, &se, &(timers[timerid].handle)) == -1)
      return PH_OSALNFC_INVALID_TIMER_ID;
   timers[timerid].callback = phOsalNfc_Timer_Dummy_Cb;
#ifdef NXP_MESSAGING
   timers[timerid].ptr = NULL;
#endif

   return timerid;
}

/*!
 * \brief Starts a timer.
 *        This function starts the timer \a TimerId with an expiration time of
 *        \a RegTimeCnt milliseconds.  Each time it expires, \a
 *        Application_callback is called.
 *
 * \param TimerId a valid timer ID.
 * \param RegTimeCnt expiration time in milliseconds.
 * \param Application_callback callback to be called when timer expires.
 */
void phOsalNfc_Timer_Start(uint32_t TimerId,
                           uint32_t RegTimeCnt,
                           ppCallBck_t  Application_callback,
                           void *pContext)
{
   struct itimerspec its;

   if(TimerId >= MAX_NO_TIMERS)
      return;
   if(Application_callback == NULL)
      return;
   if(timers[TimerId].callback == NULL)
      return;

   its.it_interval.tv_sec  = 0;
   its.it_interval.tv_nsec = 0;
   its.it_value.tv_sec     = RegTimeCnt / 1000;
   its.it_value.tv_nsec    = 1000000 * (RegTimeCnt % 1000);
   if(its.it_value.tv_sec == 0 && its.it_value.tv_nsec == 0)
   {
     // this would inadvertently stop the timer
     its.it_value.tv_nsec = 1;
   }

   timers[TimerId].callback = Application_callback;
   timers[TimerId].pContext = pContext;
   timers[TimerId].nIsStopped = 0;

   timer_settime(timers[TimerId].handle, 0, &its, NULL);
}

/*!
 * \brief Stops a timer.
 *        This function stops an already started timer.
 *
 * \param TimerId a valid timer ID.
 */
void phOsalNfc_Timer_Stop(uint32_t TimerId)
{
   struct itimerspec its = {{0, 0}, {0, 0}};

   if(TimerId >= MAX_NO_TIMERS)
      return;
   if(timers[TimerId].callback == NULL)
      return;
   if(timers[TimerId].nIsStopped == 1)
      return;

   timers[TimerId].nIsStopped = 1;
   timer_settime(timers[TimerId].handle, 0, &its, NULL);
}

/*!
 * \brief Deletes a timer.
 *        This function deletes a timer.
 *
 * \param TimerId a valid timer ID.
 */
void phOsalNfc_Timer_Delete(uint32_t TimerId)
{
   if(TimerId >= MAX_NO_TIMERS)
      return;
   if(timers[TimerId].callback == NULL)
      return;

   timer_delete(timers[TimerId].handle);

   timers[TimerId].callback = NULL;
   timers[TimerId].pContext = NULL;
}
