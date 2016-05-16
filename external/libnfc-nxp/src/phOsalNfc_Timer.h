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
 * \file  phOsalNfc_Timer.h
 * \brief Timer Implementation.
 *
 * Project: NFC-FRI 1.1
 *
 * $Date: Mon Mar 16 20:30:44 2009 $
 * $Author: ing01697 $
 * $Revision: 1.19 $
 * $Aliases: NFC_FRI1.1_WK912_R21_1,NFC_FRI1.1_WK914_PREP1,NFC_FRI1.1_WK914_R22_1,NFC_FRI1.1_WK914_R22_2,NFC_FRI1.1_WK916_R23_1,NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
 *
 */
#ifndef PHOSALNFC_TIMER_H
#define PHOSALNFC_TIMER_H

/* -----------------Include files ---------------------------------------*/ 
#include <phNfcTypes.h>
#ifdef PH_NFC_CUSTOMINTEGRATION
#include <phNfcCustomInt.h>
#else
#ifdef NXP_MESSAGING
#include <nfc_osal_deferred_call.h>
#endif

#ifdef NXP_MESSAGING
/**
 * \ingroup grp_osal_nfc
 *\brief Deferred message specific info declaration.
 * This type information packed as WPARAM  when \ref PH_OSALNFC_MESSAGE_BASE type windows message 
 * is posted to message handler thread.
 */ 
//typedef struct phOsalNfc_DeferedCalldInfo
//{
//		nfc_osal_def_call_t  pDeferedCall;/**< pointer to Deferred callback */
//		void				*pParam;/**< contains timer message specific details*/
//}phOsalNfc_DeferedCalldInfo_t;
typedef phLibNfc_DeferredCall_t phOsalNfc_DeferedCalldInfo_t;
#endif

/* ---------------- Macros ----------------------------------------------*/
/**
*\ingroup grp_osal_nfc
* OSAL timer message .This message type will be posted to calling application thread.
*/
//#define PH_OSALNFC_TIMER_MSG			  (0x315)
#define PH_OSALNFC_TIMER_MSG			  PH_LIBNFC_DEFERREDCALL_MSG

/**
 * \ingroup grp_osal_nfc
 * Invalid timer ID type.This ID used indicate timer creation is failed.
 */
#define PH_OSALNFC_INVALID_TIMER_ID		  (0xFFFF)

/*!
 * \ingroup grp_osal_nfc
 * \brief Timer callback interface which will be called once registered timer
 * timeout expires.
 * \param[in] TimerId  Timer Id for which callback is called.
 * \retval  None
 */
typedef void (*ppCallBck_t)(uint32_t TimerId, void *pContext);

/* -----------------Structures and Enumerations -------------------------*/
/**
 * \ingroup grp_osal_nfc
 **\brief Timer message structure definition.
 * Timer Message Structure contains timer specific informations like timer identifier
 * and timer callback.
 *
 */
typedef struct phOsalNfc_TimerMsg
{
   uint32_t			TimerId;/**< Timer ID*/
   ppCallBck_t		pCallBck;/**< pointer to Timer Callback*/
   void*			pContext;   /**< Timer Callback context*/
}phOsalNfc_Timer_Msg_t,*pphOsalNfc_TimerMsg_t;

/* -----------------Exported Functions----------------------------------*/
/**
 * \ingroup grp_osal_nfc
 * \brief Allows to create new timer.
 *
 * This API creates a cyclic timer. In case a valid timer is created returned
 * timer ID will be other than \ref PH_OSALNFC_INVALID_TIMER_ID. In case returned
 * timer id is \ref PH_OSALNFC_INVALID_TIMER_ID, this indicates timer creation
 * has failed.
 *
 * When a timer is created, it is not started by default. The application has to
 * explicitly start it using \ref phOsalNfc_Timer_Start().
 *
 * \param[in] void
 * \retval Created timer ID.
 * \note If timer ID value is PH_OSALNFC_INVALID_TIMER_ID, it indicates
 * an error occured during timer creation.
 *
 * \msc
 *  Application,phOsalNfc;
 *  Application=>phOsalNfc [label="phOsalNfc_Timer_Create()",URL="\ref phOsalNfc_Timer_Create"];
 *  Application<<phOsalNfc [label="Returns Valid timer ID"];
 * \endmsc
 */
uint32_t phOsalNfc_Timer_Create (void);

/**
 * \ingroup grp_osal_nfc
 * \brief Allows to start an already created timer.
 *
 * This function starts the requested timer. If the timer is already running,
 * timer stops and restarts with the new timeout value and new callback function
 * in case any.
 *
 * \note The old timeout and callback reference are not valid any more if timer
 * is restarted. Notifications are periodic and stop only when timer is stopped.
 * 
 * \param[in]  TimerId		valid timer ID obtained during timer creation.
 * \param[in]  RegTimeCnt	Requested time out in Milliseconds.
 * \note  In windows environment timer resolution should be more than
 *        50 mSec. In case time out value is below 50 mSec accuracy of timer
 *        behaviouer is not gauranteed.
 * \param[in]  Application_callback  Application Callback interface to be called
 * when timer expires.
 *
 * \msc
 *  Application,phOsalNfc;
 *  Application=>phOsalNfc [label="phOsalNfc_Timer_Create()", URL="\ref phOsalNfc_Timer_Create"];
 *  Application<<phOsalNfc [label="TIMERID"];
 *  Application=>phOsalNfc [label="phOsalNfc_Timer_Start(TIMERID, TIMEOUT, CB)", URL="\ref phOsalNfc_Timer_Start"];
 *  --- [label=" : On timer time out expired "];
 *  phOsalNfc=>phOsalNfc [label="CB()"];
 *  Application<-phOsalNfc[label="PH_OSALNFC_TIMER_MSG"];
 * \endmsc
 */
void phOsalNfc_Timer_Start(uint32_t     TimerId,
                           uint32_t     RegTimeCnt,
                           ppCallBck_t  Application_callback,
                           void         *pContext);

/**
 * \ingroup grp_osal_nfc
 * \brief Stop an already started timer.
 *
 * This API allows to stop running timers. In case the timer is stopped, its callback will not be
 * notified any more.
 *
 * \param[in] TimerId	            valid timer ID obtained suring timer creation.
 * \param[in] Application_callback   Application Callback interface to be called when timer expires.
 *
 * \msc
 *  Application,phOsalNfc;
 *  Application=>phOsalNfc [label="phOsalNfc_Timer_Create()",URL="\ref phOsalNfc_Timer_Create"];
 *  Application<<phOsalNfc [label="TIMERID"];
 *  Application=>phOsalNfc [label="phOsalNfc_Timer_Start(TIMERID, TIMEOUT, CB)",URL="\ref phOsalNfc_Timer_Start"];
 *  --- [label=" : On timer time out expired "];
 *  phOsalNfc=>phOsalNfc [label="CB()"];
 *  Application=>phOsalNfc [label="phOsalNfc_Timer_Stop(TIMERID)",URL="\ref phOsalNfc_Timer_Stop"]; 
 \endmsc
 */
void phOsalNfc_Timer_Stop(uint32_t TimerId);

/**
 * \ingroup grp_osal_nfc
 * \brief Allows to delete the timer which is already created.
 *
 * This API allows to delete a timer. Incase timer is running
 * it is stopped first and then deleted. if the given timer ID is invalid, this
 * function doesn't return any error. Application has to explicitly ensure
 * timer ID sent is valid.
 *
 * \param[in]  TimerId	timer identieir to delete the timer.
 */
void phOsalNfc_Timer_Delete(uint32_t TimerId);

#endif
#endif /* PHOSALNFC_TIMER_H */
