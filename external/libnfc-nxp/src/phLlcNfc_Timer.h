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

/*!
* \file  phLlcNfc_Timer.h
* \brief To create, start, stop and destroy timer.
*
* Project: NFC-FRI-1.1
*
* $Date: Thu Jun 10 17:26:41 2010 $
* $Author: ing02260 $
* $Revision: 1.14 $
* $Aliases: NFC_FRI1.1_WK1023_R35_1 $
*
*/

#ifndef PHLLCNFC_TIMER_H
#define PHLLCNFC_TIMER_H

/**
*  \name LLC NFC state machine handling
*
* File: \ref phLlcNfc_StateMachine.h
*
*/
/*@{*/
#define PH_LLCNFC_TIMER_FILEREVISION "$Revision: 1.14 $" /**< \ingroup grp_hal_nfc_llc_helper */
#define PH_LLCNFC_TIMER_FILEALIASES  "$Aliases: NFC_FRI1.1_WK1023_R35_1 $"     /**< \ingroup grp_hal_nfc_llc_helper */
/*@}*/
/*************************** Includes *******************************/

/*********************** End of includes ****************************/

/***************************** Macros *******************************/
#define PH_LLCNFC_CONNECTIONTIMER           (0x00)      /**< Timer for connection time out */
#define PH_LLCNFC_GUARDTIMER                (0x01)      /**< Timer for guard time out */
#define PH_LLCNFC_ACKTIMER                  (0x02)      /**< Timer for ack time out  */
#define PH_LLCNFC_MAX_RETRY_COUNT           (0x03)      /**< Retries */
/** Resolution value for the timer */
#define PH_LLCNFC_RESOLUTION                TIMER_RESOLUTION
/**< 0x05 Timer for connection time out value */
#define PH_LLCNFC_CONNECTION_TO_VALUE       LINK_CONNECTION_TIMEOUT
/**< 0x05 Timer for guard time out value */
#define PH_LLCNFC_GUARD_TO_VALUE            LINK_GUARD_TIMEOUT

#ifdef PIGGY_BACK

#define PH_LLCNFC_ACK_TO_VALUE              LINK_ACK_TIMEOUT

#endif /* #ifdef PIGGY_BACK */

#ifdef LLC_RESET_DELAY
    #define LLC_URSET_DELAY_TIME_OUT        LLC_RESET_DELAY
#else
    #define LLC_URSET_DELAY_TIME_OUT        50
#endif /* */



/************************ End of macros *****************************/

/********************** Callback functions **************************/

/******************* End of Callback functions **********************/

/********************* Structures and enums *************************/

/****************** End of structures and enums *********************/

/******************** Function declarations *************************/
/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC timer functions \b Timer Init function
*
* \copydoc page_reg This is to store LLCs main context structure
*
* \param[in, out] psLlcCtxt     Llc main structure information
*
* \retval NFCSTATUS_SUCCESS                 Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER       At least one parameter of the function is invalid.
*
*/
NFCSTATUS 
phLlcNfc_TimerInit (
    phLlcNfc_Context_t  *psLlcCtxt
);

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC timer functions \b Timer UnInit function
*
* \copydoc page_reg This is to uninitialise all timer related information
*
* \param[in, out] psLlcCtxt     Llc main structure information
*
* \retval NFCSTATUS_SUCCESS                 Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER       At least one parameter of the function is invalid.
*
*/
void 
phLlcNfc_TimerUnInit (
    phLlcNfc_Context_t  *psLlcCtxt
);

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC timer functions \b Create timer function
*
* \copydoc page_reg creates all the timers in the LLC context
*
*
*/
void 
phLlcNfc_CreateTimers (void);

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC timer functions \b Start timer function
*
* \copydoc page_reg starts the timer type given by the user
*
* \param[in] TimerType          Timer type to start
* \param[in] ns_value           Value of N(S) for which the timer is started
*
* \retval NFCSTATUS_SUCCESS                 Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER       At least one parameter of the function is invalid.
* \retval Others                            Errors related to OsalNfc.
*
*/
NFCSTATUS 
phLlcNfc_StartTimers (
    uint8_t             TimerType, 
    uint8_t             ns_value
);

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC timer functions \b Stop timer function
*
* \copydoc page_reg stop the timer type given by the user
*
* \param[in] TimerType              Timer type to start
* \param[in] no_of_gaurd_to_del     Guard time-out count shall be decreased as and when  
*                                   frame is removed
*
*
*/
void 
phLlcNfc_StopTimers (
    uint8_t             TimerType, 
    uint8_t             no_of_guard_to_del
);

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC timer functions \b Stop timer function
*
* \copydoc page_reg stop the timer type given by the user
*
*
*
*
*/
void 
phLlcNfc_StopAllTimers (void);


/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC timer functions \b Delete timer function
*
* \copydoc page_reg deletes all the timers in the LLC context
*
* \retval NFCSTATUS_SUCCESS                 Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER       At least one parameter of the function is invalid.
* \retval Others                            Errors related to OsalNfc.
*
*/
void 
phLlcNfc_DeleteTimer (void);

#ifdef LLC_URSET_NO_DELAY

    /* NO definition required */

#else /* #ifdef LLC_URSET_NO_DELAY */

void 
phLlcNfc_URSET_Delay_Notify (

    uint32_t            delay_id);



#endif /* #ifdef LLC_URSET_NO_DELAY */

/****************** End of Function declarations ********************/
#endif /* PHLLCNFC_TIMER_H */
