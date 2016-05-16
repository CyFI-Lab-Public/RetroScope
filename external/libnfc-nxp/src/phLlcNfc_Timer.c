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
* \file  phLlcNfc_Timer.c
* \brief To create, start, stop and destroy timer.
*
* Project: NFC-FRI-1.1
*
* $Date: Mon Jun 14 11:47:54 2010 $
* $Author: ing02260 $
* $Revision: 1.55 $
* $Aliases: NFC_FRI1.1_WK1023_R35_2,NFC_FRI1.1_WK1023_R35_1 $
*
*/

/*************************** Includes *******************************/
#include <phNfcTypes.h>
#include <phNfcStatus.h>
#include <phOsalNfc.h>
#include <phOsalNfc_Timer.h>
#include <phNfcInterface.h>
#include <phLlcNfc.h>
#include <phLlcNfc_DataTypes.h>
#include <phLlcNfc_Interface.h>
#include <phLlcNfc_Frame.h>
#include <phLlcNfc_Timer.h>

/*********************** End of includes ****************************/

/***************************** Macros *******************************/
/**< Timer for connection timer index */
#define PH_LLCNFC_CONNECTION_TO_INDEX       (0x00)
/**< Maximum guard timer can be present */
#define PH_LLCNFC_MAX_GUARD_TIMER           (0x04)
/** Connection time out bit to set */
#define PH_LLCNFC_CON_TO_BIT                (0)
/** Guard time out bit to set */
#define PH_LLCNFC_GUARD_TO_BIT              (1)
/** Ack time out bit to set */
#define PH_LLCNFC_ACK_TO_BIT                (2)
/** No of bits to set */
#define PH_LLCNFC_TO_NOOFBITS               (1)
/** Connection time out bit value */
#define PH_LLCNFC_CON_TO_BIT_VAL            (0x01)
/** Guard time out bit to set */
#define PH_LLCNFC_GUARD_TO_BIT_VAL          (0x02)
/** ACK time out bit to set */
#define PH_LLCNFC_ACK_TO_BIT_VAL            (0x04)

#define GUARD_TO_URSET


/************************ End of macros *****************************/

/*********************** Local functions ****************************/
/* This callback is for guard time out */
#ifdef LLC_TIMER_ENABLE
static 
void 
phLlcNfc_GuardTimeoutCb (
    uint32_t TimerId,
    void *pContext
);


#ifdef PIGGY_BACK
/* This callback is for acknowledge time out */
static 
void 
phLlcNfc_AckTimeoutCb (
    uint32_t TimerId                    
);
#endif /* #ifdef PIGGY_BACK */

/* This callback is for connection time out */
static 
void 
phLlcNfc_ConnectionTimeoutCb (
    uint32_t TimerId,
    void *pContext
);
#endif /* #ifdef LLC_TIMER_ENABLE */

/******************** End of Local functions ************************/

/********************** Global variables ****************************/
static phLlcNfc_Context_t   *gpphLlcNfc_Ctxt = NULL;

/******************** End of Global Variables ***********************/

NFCSTATUS 
phLlcNfc_TimerInit(
    phLlcNfc_Context_t  *psLlcCtxt
)
{
    NFCSTATUS   result = PHNFCSTVAL(CID_NFC_LLC, 
                                    NFCSTATUS_INVALID_PARAMETER);
    uint8_t     index = 0;
    if (NULL != psLlcCtxt)
    {
        result = NFCSTATUS_SUCCESS;
        gpphLlcNfc_Ctxt = psLlcCtxt;
        while (index < PH_LLCNFC_MAX_TIMER_USED)
        {
#ifdef LLC_TIMER_ENABLE
            gpphLlcNfc_Ctxt->s_timerinfo.timer_id[index] = 
                                    PH_OSALNFC_INVALID_TIMER_ID;
#endif /* #ifdef LLC_TIMER_ENABLE */
            index++;
        }
    }
    return result;
}

void   
phLlcNfc_TimerUnInit(
    phLlcNfc_Context_t  *psLlcCtxt
)
{
    uint8_t     index = 0;
    if ((NULL != gpphLlcNfc_Ctxt) && 
        (gpphLlcNfc_Ctxt == psLlcCtxt))
    {
        while (index <= PH_LLCNFC_ACKTIMER)
        {
            if (PH_LLCNFC_GUARDTIMER == index)
            {
                phLlcNfc_StopTimers (index, 
                        gpphLlcNfc_Ctxt->s_timerinfo.guard_to_count);
            }
            else
            {
                phLlcNfc_StopTimers (index, 0);
            }
            index++;
        }
        phLlcNfc_DeleteTimer();
    }
}

void 
phLlcNfc_CreateTimers(void)
{
    uint8_t     index = 0;
    
    while (index < PH_LLCNFC_MAX_TIMER_USED)
    {
#ifdef LLC_TIMER_ENABLE
        gpphLlcNfc_Ctxt->s_timerinfo.timer_id[index] = 
                            phOsalNfc_Timer_Create();
#endif /* #ifdef LLC_TIMER_ENABLE */
        index++;
    }
    return;
}

NFCSTATUS 
phLlcNfc_StartTimers (
    uint8_t             TimerType, 
    uint8_t             ns_value
)
{
    NFCSTATUS               result = NFCSTATUS_SUCCESS;
#ifdef LLC_TIMER_ENABLE

    uint32_t                timerid = 0;
    uint8_t                 timerstarted = 0;
    uint8_t                 timer_count = 0;
    uint16_t                timer_resolution = 0;
    ppCallBck_t             Callback = NULL;
    phLlcNfc_Timerinfo_t    *ps_timer_info = NULL;

    ps_timer_info = &(gpphLlcNfc_Ctxt->s_timerinfo);
    PHNFC_UNUSED_VARIABLE(result);

    PH_LLCNFC_PRINT("\n\nLLC : START TIMER CALLED\n\n");
    /* Depending on the timer type, use the Osal callback */
    switch(TimerType)
    {
        case PH_LLCNFC_CONNECTIONTIMER:
        {
            /* Get the connection timer flag */
            timerstarted = (uint8_t)
                GET_BITS8(ps_timer_info->timer_flag, 
                        PH_LLCNFC_CON_TO_BIT, 
                        PH_LLCNFC_TO_NOOFBITS);
            if (0 == timerstarted)
            {
                /* Timer not started, so start the timer */
                gpphLlcNfc_Ctxt->s_timerinfo.timer_flag = (uint8_t)
                                SET_BITS8 (ps_timer_info->timer_flag,
                                        PH_LLCNFC_CON_TO_BIT, 
                                        PH_LLCNFC_TO_NOOFBITS, 
                                        (PH_LLCNFC_CON_TO_BIT + 1));
            }
            
            timerid = ps_timer_info->timer_id[PH_LLCNFC_CONNECTION_TO_INDEX];
            Callback = (ppCallBck_t)&phLlcNfc_ConnectionTimeoutCb;
            timer_resolution = ps_timer_info->con_to_value = (uint16_t)
                                            PH_LLCNFC_CONNECTION_TO_VALUE;
            break;
        }

        case PH_LLCNFC_GUARDTIMER:
        {
            if (ps_timer_info->guard_to_count < PH_LLCNFC_MAX_GUARD_TIMER)
            {
                timer_count = ps_timer_info->guard_to_count;
                timer_resolution = (uint16_t)PH_LLCNFC_RESOLUTION;

                PH_LLCNFC_DEBUG("RESOLUTION VALUE : 0x%02X\n", PH_LLCNFC_RESOLUTION);
                PH_LLCNFC_DEBUG("TIME-OUT VALUE : 0x%02X\n", PH_LLCNFC_GUARD_TO_VALUE);
                
                /* Get the guard timer flag */
                timerstarted = (uint8_t)
                    GET_BITS8 (ps_timer_info->timer_flag, 
                            PH_LLCNFC_GUARD_TO_BIT, 
                            PH_LLCNFC_TO_NOOFBITS);

                PH_LLCNFC_DEBUG("GUARD TIMER NS INDEX : 0x%02X\n", ns_value);
                PH_LLCNFC_DEBUG("GUARD TIMER COUNT : 0x%02X\n", timer_count);
                PH_LLCNFC_DEBUG("GUARD TIMER STARTED : 0x%02X\n", timerstarted);

                if (0 == timerstarted)
                {
                    /* Timer not started, so start the timer */
                    ps_timer_info->timer_flag = (uint8_t)
                        SET_BITS8 (ps_timer_info->timer_flag,
                                    PH_LLCNFC_GUARD_TO_BIT, 
                                    PH_LLCNFC_TO_NOOFBITS, 
                                    PH_LLCNFC_GUARD_TO_BIT);
                }
                
                timerid = ps_timer_info->timer_id[PH_LLCNFC_GUARDTIMER];
                Callback = (ppCallBck_t)&phLlcNfc_GuardTimeoutCb;

                /* Guard time out value */
                ps_timer_info->guard_to_value[timer_count] = (uint16_t)
                                        PH_LLCNFC_GUARD_TO_VALUE;

                ps_timer_info->timer_ns_value[timer_count] = ns_value;
                ps_timer_info->frame_type[timer_count] = (uint8_t)invalid_frame;
                ps_timer_info->iframe_send_count[timer_count] = 0;

                if ((timer_count > 0) && 
                    (ps_timer_info->guard_to_value[(timer_count - 1)] >= 
                    PH_LLCNFC_GUARD_TO_VALUE))
                {
                    /* If the timer has been started already and the 
                        value is same as the previous means that timer has still 
                        not expired, so the time out value is increased by 
                        a resolution */
                    ps_timer_info->guard_to_value[timer_count] = (uint16_t)
                            (ps_timer_info->guard_to_value[(timer_count - 1)] + 
                            PH_LLCNFC_RESOLUTION);
                }

                PH_LLCNFC_DEBUG("GUARD TIMER VALUE : 0x%04X\n", ps_timer_info->guard_to_value[timer_count]);

                
                ps_timer_info->guard_to_count = (uint8_t)(
                                        ps_timer_info->guard_to_count + 1);
            }
            else
            {
                /* TIMER should not start, because the time out count has readched the limit */
                timerstarted = TRUE;
            }
            break;
        }
        
#ifdef PIGGY_BACK

        case PH_LLCNFC_ACKTIMER:
        {
            /* Get the ack timer flag */
            timerstarted = (uint8_t)GET_BITS8 (
                                    ps_timer_info->timer_flag, 
                                    PH_LLCNFC_ACK_TO_BIT,
                                    PH_LLCNFC_TO_NOOFBITS);

            if (FALSE == timerstarted)
            {
                /* Timer not started, so start the timer */
                ps_timer_info->timer_flag = (uint8_t)
                                SET_BITS8 (ps_timer_info->timer_flag, 
                                        PH_LLCNFC_ACK_TO_BIT,
                                        PH_LLCNFC_TO_NOOFBITS,
                                        (PH_LLCNFC_ACK_TO_BIT - 1));
            }


            timer_resolution = ps_timer_info->ack_to_value = (uint16_t)
                                            PH_LLCNFC_ACK_TO_VALUE;
            timerid = ps_timer_info->timer_id[PH_LLCNFC_ACKTIMER];
            Callback = (ppCallBck_t)&phLlcNfc_AckTimeoutCb;
            break;
        }

#endif /* #ifdef PIGGY_BACK */

        default:
        {
            result = PHNFCSTVAL(CID_NFC_LLC, 
                                NFCSTATUS_INVALID_PARAMETER);
            break;
        }
    }
    if ((NFCSTATUS_SUCCESS == result) && 
        (FALSE == timerstarted))
    {
        PH_LLCNFC_DEBUG("OSAL START TIMER CALLED TIMER ID : 0x%02X\n", timerid);
        phOsalNfc_Timer_Start (timerid, timer_resolution, Callback, NULL);
    }

    PH_LLCNFC_PRINT("\n\nLLC : START TIMER END\n\n");

#else /* #ifdef LLC_TIMER_ENABLE */

    PHNFC_UNUSED_VARIABLE(result);
    PHNFC_UNUSED_VARIABLE(TimerType);
    PHNFC_UNUSED_VARIABLE(ns_value);    

#endif /* #ifdef LLC_TIMER_ENABLE */
    return result;
}

void 
phLlcNfc_StopTimers (
    uint8_t             TimerType, 
    uint8_t             no_of_guard_to_del
)
{
    NFCSTATUS               result = NFCSTATUS_SUCCESS;
#ifdef LLC_TIMER_ENABLE

    uint32_t                timerid = 0,
                            timerflag = FALSE;
    uint8_t                 timer_count = 0;
    phLlcNfc_Timerinfo_t    *ps_timer_info = NULL;
    

    ps_timer_info = &(gpphLlcNfc_Ctxt->s_timerinfo);
    timerflag = ps_timer_info->timer_flag;

    PHNFC_UNUSED_VARIABLE (result);
    PH_LLCNFC_PRINT("\n\nLLC : STOP TIMER CALLED\n\n");
    switch(TimerType)
    {
        case PH_LLCNFC_CONNECTIONTIMER:
        {           
            ps_timer_info->timer_flag = (uint8_t)
                        SET_BITS8(ps_timer_info->timer_flag, 
                                PH_LLCNFC_CON_TO_BIT, 
                                PH_LLCNFC_TO_NOOFBITS, 0);
            timerid = ps_timer_info->timer_id
                                [PH_LLCNFC_CONNECTION_TO_INDEX];
            break;
        }

        case PH_LLCNFC_GUARDTIMER:
        {
            uint8_t             start_index = 0;

            timer_count = ps_timer_info->guard_to_count;

            PH_LLCNFC_DEBUG("GUARD TIMER COUNT BEFORE DELETE: 0x%02X\n", timer_count);
            PH_LLCNFC_DEBUG("GUARD TIMER TO DELETE: 0x%02X\n", no_of_guard_to_del);

            if (timer_count > no_of_guard_to_del)
            {
                /* The number of guard timer count is more than the 
                    guard timer to delete  */
                while (start_index < (timer_count - no_of_guard_to_del))
                {
                    /* Copy the previous stored timer values to the present */
                    ps_timer_info->guard_to_value[start_index] = (uint16_t)
                                (ps_timer_info->guard_to_value[
                                (no_of_guard_to_del + start_index)]);

                    ps_timer_info->iframe_send_count[start_index] = (uint8_t)
                                (ps_timer_info->iframe_send_count[
                                (no_of_guard_to_del + start_index)]);

                    PH_LLCNFC_DEBUG("GUARD TIMER NS INDEX DELETED : 0x%02X\n", ps_timer_info->timer_ns_value[start_index]);

                    ps_timer_info->timer_ns_value[start_index] = (uint8_t)
                                (ps_timer_info->timer_ns_value[
                                (no_of_guard_to_del + start_index)]);

                    ps_timer_info->frame_type[start_index] = (uint8_t)
                                (ps_timer_info->frame_type[
                                (no_of_guard_to_del + start_index)]);

                    start_index = (uint8_t)(start_index + 1);
                }
            }
            else
            {
                while (start_index < no_of_guard_to_del)
                {
                    ps_timer_info->guard_to_value[start_index] = 0;

                    ps_timer_info->iframe_send_count[start_index] = 0;

                    PH_LLCNFC_DEBUG("GUARD TIMER NS INDEX DELETED ELSE : 0x%02X\n", ps_timer_info->timer_ns_value[start_index]);

                    ps_timer_info->timer_ns_value[start_index] = 0;

                    ps_timer_info->frame_type[start_index] = 0;

                    start_index = (uint8_t)(start_index + 1);
                }
            }

            if (timer_count >= no_of_guard_to_del)
            {
                timer_count = (uint8_t)(timer_count - no_of_guard_to_del);
            }
            else
            {
                if (0 != no_of_guard_to_del)
                {
                    timer_count = 0;
                }
            }

            timerid = ps_timer_info->timer_id[PH_LLCNFC_GUARDTIMER];
            ps_timer_info->guard_to_count = timer_count;
            PH_LLCNFC_DEBUG("GUARD TIMER COUNT AFTER DELETE: 0x%02X\n", timer_count);

            if (0 == ps_timer_info->guard_to_count)
            {
                /* This means that there are no frames to run guard 
                    timer, so set the timer flag to 0 */
                ps_timer_info->timer_flag = (uint8_t)
                        SET_BITS8 (ps_timer_info->timer_flag, 
                                    PH_LLCNFC_GUARD_TO_BIT, 
                                    PH_LLCNFC_TO_NOOFBITS, 0);
            }
            else
            {
                timerflag = 0;
            }
            break;
        }  

#ifdef PIGGY_BACK
        case PH_LLCNFC_ACKTIMER:
        {
            timerflag = (timerflag & PH_LLCNFC_ACK_TO_BIT_VAL);

            ps_timer_info->timer_flag = (uint8_t)
                                SET_BITS8 (ps_timer_info->timer_flag, 
                                        PH_LLCNFC_ACK_TO_BIT,
                                        PH_LLCNFC_TO_NOOFBITS, 0);
            timerid = ps_timer_info->timer_id[PH_LLCNFC_ACKTIMER];
            ps_timer_info->ack_to_value = 0;
            break;
        }
#endif /* #ifdef PIGGY_BACK */

        default:
        {
            result = PHNFCSTVAL(CID_NFC_LLC, 
                                NFCSTATUS_INVALID_PARAMETER);
            break;
        }
    }

    if ((NFCSTATUS_SUCCESS == result) && (timerflag > 0))
    {
        PH_LLCNFC_DEBUG("OSAL STOP TIMER CALLED TIMER ID : 0x%02X\n", timerid);
        phOsalNfc_Timer_Stop (timerid);
    }

    PH_LLCNFC_PRINT("\n\nLLC : STOP TIMER END\n\n");

#else /* #ifdef LLC_TIMER_ENABLE */

    PHNFC_UNUSED_VARIABLE (result);
    PHNFC_UNUSED_VARIABLE (TimerType);
    PHNFC_UNUSED_VARIABLE (no_of_guard_to_del);

#endif /* #ifdef LLC_TIMER_ENABLE */
}

void 
phLlcNfc_StopAllTimers (void)
{

#ifdef LLC_TIMER_ENABLE

    phLlcNfc_Timerinfo_t    *ps_timer_info = NULL;
    uint8_t                 timer_started = 0;
    uint32_t                timerid = 0;
    uint8_t                 timer_index = 0;
    

    ps_timer_info = &(gpphLlcNfc_Ctxt->s_timerinfo);

    PH_LLCNFC_PRINT("\n\nLLC : STOP ALL TIMERS CALLED \n\n");

    timerid = ps_timer_info->timer_id[timer_index];
    timer_started = (uint8_t)
                GET_BITS8(ps_timer_info->timer_flag, 
                        PH_LLCNFC_CON_TO_BIT, 
                        PH_LLCNFC_TO_NOOFBITS);

    PH_LLCNFC_DEBUG("CONNECTION TIMER ID: 0x%02X\n", timerid);

    if (0 != timer_started)
    {
        /* Connection timer is started, so now stop it */
        ps_timer_info->timer_flag = (uint8_t)
                        SET_BITS8 (ps_timer_info->timer_flag, 
                                    PH_LLCNFC_CON_TO_BIT, 
                                    PH_LLCNFC_TO_NOOFBITS, 0);
#if 0

        ps_timer_info->con_to_value = 0;

#endif /* #if 0 */

        phOsalNfc_Timer_Stop (timerid);
    }

    timer_index = (uint8_t)(timer_index + 1);
    timerid = ps_timer_info->timer_id[timer_index];
    timer_started = (uint8_t)GET_BITS8 (ps_timer_info->timer_flag, 
                                        PH_LLCNFC_GUARD_TO_BIT, 
                                        PH_LLCNFC_TO_NOOFBITS);

    if (0 != timer_started)
    {
        /* Guard timer is already started */
        ps_timer_info->timer_flag = (uint8_t)
                        SET_BITS8 (ps_timer_info->timer_flag, 
                                    PH_LLCNFC_GUARD_TO_BIT, 
                                    PH_LLCNFC_TO_NOOFBITS, 0);

        timer_index = 0;
        ps_timer_info->guard_to_count = 0;

#if 0

        /* Reset all the guard timer related variables */
        while (timer_index < ps_timer_info->guard_to_count)
        {            
            ps_timer_info->guard_to_value[timer_index] = 0;
            ps_timer_info->iframe_send_count[timer_index] = 0;

            timer_index = (uint8_t)(timer_index + 1);
        }        

#endif /* #if 0 */

        PH_LLCNFC_DEBUG("GUARD TIMER ID: 0x%02X\n", timerid);

        /* Stop the timer */
        phOsalNfc_Timer_Stop (timerid);

        PH_LLCNFC_PRINT("\n\nLLC : STOP ALL TIMERS END \n\n");
    }

#endif /* #ifdef LLC_TIMER_ENABLE */
}

void 
phLlcNfc_DeleteTimer (void)
{
    uint8_t     index = 0;
    while (index < PH_LLCNFC_MAX_TIMER_USED)
    {
#ifdef LLC_TIMER_ENABLE
        phOsalNfc_Timer_Delete(
            gpphLlcNfc_Ctxt->s_timerinfo.timer_id[index]);
        gpphLlcNfc_Ctxt->s_timerinfo.timer_id[index] = 
                            PH_OSALNFC_INVALID_TIMER_ID;                            
#endif /* #ifdef LLC_TIMER_ENABLE */
        index++;
    }
}

#ifdef LLC_TIMER_ENABLE

#define LLC_GUARD_TIMER_RETRIES                         (0x03U)

static 
void 
phLlcNfc_GuardTimeoutCb (
    uint32_t TimerId,
    void *pContext
)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    phLlcNfc_Timerinfo_t        *ps_timer_info = NULL;
    phLlcNfc_Frame_t            *ps_frame_info = NULL;
    phLlcNfc_LlcPacket_t        s_packet_info;
    uint8_t                     index = 0;
    /* zero_to_index = Time out index has become 0 */
    uint8_t                     zero_to_index = 0;

#if defined (GUARD_TO_ERROR)
    phNfc_sCompletionInfo_t     notifyinfo = {0,0,0};
#endif /* #if defined (GUARD_TO_ERROR) */

    PH_LLCNFC_PRINT("\n\nLLC : GUARD TIMEOUT CB CALLED \n\n");

    if ((NULL != gpphLlcNfc_Ctxt) && (TimerId ==
        gpphLlcNfc_Ctxt->s_timerinfo.timer_id[PH_LLCNFC_GUARDTIMER]) &&
        (PH_LLCNFC_GUARD_TO_BIT_VAL ==
        (gpphLlcNfc_Ctxt->s_timerinfo.timer_flag &
        PH_LLCNFC_GUARD_TO_BIT_VAL)))
    {
        uint8_t                 timer_expired = FALSE;

        ps_frame_info = &(gpphLlcNfc_Ctxt->s_frameinfo);
        ps_timer_info = &(gpphLlcNfc_Ctxt->s_timerinfo);

#if !defined (CYCLIC_TIMER)
        phOsalNfc_Timer_Stop(
                    ps_timer_info->timer_id[PH_LLCNFC_GUARDTIMER]);
#endif

        PH_LLCNFC_DEBUG("NO OF TIMEOUT COUNT : 0x%02X\n", ps_timer_info->guard_to_count);
        /* Loop is completely depending on the number of different LLC  
           send called */
        while (index < ps_timer_info->guard_to_count) 
        {
            /* This loop runs for all the timer present in the data structure.
                This means if there are 2 I frame has been sent and
                response is not received for the I frames sent then the
                each time this timer expires, the time out value is decremented
                by the PH_LLCNFC_RESOLUTION value */
            if (0 != ps_timer_info->guard_to_value[index])
            {
                /* If timer value is not zero then enter,
                    this means that the value is not zero */
                if (ps_timer_info->guard_to_value[index] > 0)
                {
                    if (ps_timer_info->guard_to_value[index] >= 
                        PH_LLCNFC_RESOLUTION)
                    {
                        ps_timer_info->guard_to_value[index] = (uint16_t)
                            (ps_timer_info->guard_to_value[index] - 
                            PH_LLCNFC_RESOLUTION);
                    }
                    else
                    {
                        ps_timer_info->guard_to_value[index] = 0;
                    }
                }

                if (0 == ps_timer_info->guard_to_value[index])
                {
                    /* Timer value has expired, so resend has to be done
                        Timer value is 0 */
                    ps_timer_info->frame_type[index] = (uint8_t)resend_i_frame;
                    if (FALSE == timer_expired)
                    {
                        /* As the statement is in the loop, so there are possibilities
                            of more than 1 timer value can be 0, so if previous timer
                            value has already been 0, then again dont change the
                            index */
                    zero_to_index = index;
                    timer_expired = TRUE;
                }
            }
            }
            index = (uint8_t)(index + 1);
        }

#if !defined (CYCLIC_TIMER)        
        /* Start the timer again */
        phOsalNfc_Timer_Start(
                    ps_timer_info->timer_id[PH_LLCNFC_GUARDTIMER], 
                    PH_LLCNFC_RESOLUTION, phLlcNfc_GuardTimeoutCb, NULL);
#endif
        PH_LLCNFC_DEBUG("TIMER EXPIRED : 0x%02X\n", timer_expired);        

        if (TRUE == timer_expired)
        {
            PH_LLCNFC_DEBUG("TIMER EXPIRED INDEX: 0x%02X\n", zero_to_index);
            PH_LLCNFC_DEBUG("TIMER EXPIRED NS INDEX: 0x%02X\n", ps_timer_info->timer_ns_value[zero_to_index]);
            PH_LLCNFC_DEBUG("TIMER EXPIRED RETRIES : 0x%02X\n", ps_timer_info->iframe_send_count[zero_to_index]);

            PH_LLCNFC_DEBUG("TIMER EXPIRED GUARD TIME-OUT COUNT: 0x%02X\n", ps_timer_info->guard_to_value[zero_to_index]);

            if ((0 == ps_timer_info->guard_to_value[zero_to_index]) && 
                (ps_timer_info->iframe_send_count[zero_to_index] < 
                LLC_GUARD_TIMER_RETRIES))
            {
                if (ps_frame_info->s_send_store.winsize_cnt > 0)
                {
                    uint8_t             start_index = 0;
                    uint8_t             timer_count = 0;
                    uint8_t             while_exit = FALSE;

                    timer_count = ps_timer_info->guard_to_count;

                    /* Check before changing the index to resend, if index 
                        already exist then dont set the index */
                    while ((FALSE == while_exit) && (start_index < timer_count))
                    {
                        if (resend_i_frame == 
                            ps_timer_info->frame_type[start_index])
                        {
                            while_exit = TRUE;
                        }
                        else
                        {                     
                            start_index = (uint8_t)(start_index + 1);
                        }
                    }

                    if (FALSE == while_exit)
                    {
                        /* This " ps_timer_info->index_to_send " member is
                           useful, when 2 time out values are 0, then
                           only first timed out value has to be resent and
                           other has to wait until the the first timed out
                           I frame is resent
                           This statement is executed only if, none of the timer
                           has expires previously, this is the first timer in the
                           list that has time out value has 0
                           */
                        ps_timer_info->index_to_send = zero_to_index;
                    }
                    else
                    {
                        /* This statement is executed only if, any one of the time
                           out value was 0 previously, so first resend has to be done
                           for the previous I frame, so the index is set to the previous
                           I frame
                           */
                        ps_timer_info->index_to_send = start_index;
                    }

                    /* Now resend the frame stored */
                    result = phLlcNfc_H_SendTimedOutIFrame (gpphLlcNfc_Ctxt, 
                                            &(ps_frame_info->s_send_store), 
                                            0);
                }
            }
            else
            {
                if ((LLC_GUARD_TIMER_RETRIES == 
                    ps_timer_info->iframe_send_count[zero_to_index]) && 
                    (NULL != gpphLlcNfc_Ctxt->cb_for_if.notify))
                {
                    phLlcNfc_StopAllTimers ();
#if defined (GUARD_TO_ERROR)
                    
                    notifyinfo.status = PHNFCSTVAL(CID_NFC_LLC, 
                                        NFCSTATUS_BOARD_COMMUNICATION_ERROR);
#if 0
                    phOsalNfc_RaiseException(phOsalNfc_e_UnrecovFirmwareErr,1); 
#endif /* #if 0 */
                    /* Resend done, no answer from the device */
                    gpphLlcNfc_Ctxt->cb_for_if.notify (
                                    gpphLlcNfc_Ctxt->cb_for_if.pif_ctxt,
                                    gpphLlcNfc_Ctxt->phwinfo, 
                                    NFC_NOTIFY_DEVICE_ERROR, 
                                    &notifyinfo);

#endif /* #if defined (GUARD_TO_ERROR) */

#if (!defined (GUARD_TO_ERROR) && defined (GUARD_TO_URSET))

                    PH_LLCNFC_PRINT("U-RSET IS SENT \n");

                    result = phLlcNfc_H_CreateUFramePayload(gpphLlcNfc_Ctxt,
                                        &(s_packet_info),
                                        &(s_packet_info.llcbuf_len),
                                        phLlcNfc_e_rset);

                    result = phLlcNfc_Interface_Write(gpphLlcNfc_Ctxt,
                                    (uint8_t*)&(s_packet_info.s_llcbuf),
                                    (uint32_t)s_packet_info.llcbuf_len);

                    ps_frame_info->write_status = result;
                    if (NFCSTATUS_PENDING == result)
                    {
                        /* Start the timer */
                        result = phLlcNfc_StartTimers (PH_LLCNFC_CONNECTIONTIMER, 0);
                        if (NFCSTATUS_SUCCESS == result)
                        {
                            ps_frame_info->retry_cnt = 0;
                            gpphLlcNfc_Ctxt->s_frameinfo.sent_frame_type = 
                                                                    u_rset_frame;
                            result = NFCSTATUS_PENDING;
                        }
                    }
                    else
                    {
                        if (NFCSTATUS_BUSY == PHNFCSTATUS (result))
                        {                        
                            ps_frame_info->write_wait_call = u_rset_frame;
                        }
                    }

#endif /* #if defined (GUARD_TO_ERROR) */
                }
            }
        }
    }
    PH_LLCNFC_PRINT("\n\nLLC : GUARD TIMEOUT CB END\n\n");
}

#ifdef PIGGY_BACK

static 
void 
phLlcNfc_AckTimeoutCb (
    uint32_t TimerId
)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    phLlcNfc_Frame_t            *ps_frame_info = NULL;
    phLlcNfc_Timerinfo_t        *ps_timer_info = NULL;
    phLlcNfc_LlcPacket_t        s_packet_info;

    PH_LLCNFC_PRINT("\n\nLLC : ACK TIMEOUT CB CALLED\n\n");

    if ((NULL != gpphLlcNfc_Ctxt) && (TimerId ==
        gpphLlcNfc_Ctxt->s_timerinfo.timer_id[PH_LLCNFC_ACKTIMER])
        && (PH_LLCNFC_ACK_TO_BIT_VAL ==
        (gpphLlcNfc_Ctxt->s_timerinfo.timer_flag &
        PH_LLCNFC_ACK_TO_BIT_VAL)))
    {
        ps_frame_info = &(gpphLlcNfc_Ctxt->s_frameinfo);
        ps_timer_info = &(gpphLlcNfc_Ctxt->s_timerinfo);

        phLlcNfc_StopTimers (PH_LLCNFC_ACKTIMER, 0);

        if (NFCSTATUS_BUSY == PHNFCSTATUS (ps_frame_info->write_status))
        {
            /* Any how write cannot be done and some frame is ready to be sent
            so this frame will act as the ACK */
            result = phLlcNfc_H_WriteWaitCall (gpphLlcNfc_Ctxt);
        }
        else
        {
            /* Create S frame */
            (void)phLlcNfc_H_CreateSFramePayload (ps_frame_info, &(s_packet_info), phLlcNfc_e_rr);

            result = phLlcNfc_Interface_Write(gpphLlcNfc_Ctxt,
                        (uint8_t *)&(s_packet_info.s_llcbuf),
                        (uint32_t)(s_packet_info.llcbuf_len));

            if (NFCSTATUS_PENDING == result)
            {
                if (0 == ps_frame_info->send_error_count)
                {
                    ps_frame_info->write_wait_call = invalid_frame;
                }
                ps_frame_info->sent_frame_type = s_frame;
            }
            else
            {
                if (invalid_frame == ps_frame_info->write_wait_call)
                {
                    ps_frame_info->write_wait_call = s_frame;
                }
            }
        }
    }

    /* ACK is sent, so reset the response received count */
    gpphLlcNfc_Ctxt->s_frameinfo.resp_recvd_count = 0;

    PH_LLCNFC_PRINT("\n\nLLC : ACK TIMEOUT CB END\n\n");
}

#endif /* #ifdef PIGGY_BACK */

static 
void 
phLlcNfc_ConnectionTimeoutCb (
    uint32_t TimerId,
    void *pContext
)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    phNfc_sCompletionInfo_t     notifyinfo = {0,0,0};
    pphNfcIF_Notification_CB_t  notifyul = NULL;
    void                        *p_upperctxt = NULL;
    phLlcNfc_Frame_t            *ps_frame_info = NULL;
    phLlcNfc_Timerinfo_t        *ps_timer_info = NULL;
    phLlcNfc_LlcPacket_t        s_packet_info;
    
    PH_LLCNFC_PRINT("\n\nLLC : CONNECTION TIMEOUT CB CALLED\n\n");
    if ((NULL != gpphLlcNfc_Ctxt) && (TimerId == 
        gpphLlcNfc_Ctxt->s_timerinfo.timer_id[PH_LLCNFC_CONNECTIONTIMER]) 
        && (PH_LLCNFC_CON_TO_BIT_VAL == 
        (gpphLlcNfc_Ctxt->s_timerinfo.timer_flag & 
        PH_LLCNFC_CON_TO_BIT_VAL)))
    {
        ps_frame_info = &(gpphLlcNfc_Ctxt->s_frameinfo);
        ps_timer_info = &(gpphLlcNfc_Ctxt->s_timerinfo);
        if (ps_timer_info->con_to_value > 0)
        {
#if !defined (CYCLIC_TIMER)
            phOsalNfc_Timer_Stop(
                    ps_timer_info->timer_id[PH_LLCNFC_CONNECTIONTIMER]);
            /* phLlcNfc_StopTimers(PH_LLCNFC_CONNECTIONTIMER, 0); */
#endif
            ps_timer_info->con_to_value = 0;
        
            if (0 == ps_timer_info->con_to_value)
            {
                PH_LLCNFC_DEBUG("TIMER EXPIRED RETRY COUNT : %02X\n", ps_frame_info->retry_cnt);
                phLlcNfc_StopTimers (PH_LLCNFC_CONNECTIONTIMER, 0);
                
                if (ps_frame_info->retry_cnt < PH_LLCNFC_MAX_RETRY_COUNT)
                {
                    /* Create a U frame */
                    result = phLlcNfc_H_CreateUFramePayload(gpphLlcNfc_Ctxt, 
                                        &(s_packet_info),
                                        &(s_packet_info.llcbuf_len),
                                        phLlcNfc_e_rset);

                    if (NFCSTATUS_SUCCESS == result)
                    {
                        /* Call DAL write */
                        result = phLlcNfc_Interface_Write (gpphLlcNfc_Ctxt, 
                                (uint8_t*)&(s_packet_info.s_llcbuf),
                                (uint32_t)(s_packet_info.llcbuf_len));
                    }
                    if (NFCSTATUS_PENDING == result)
                    {
                        /* Start the timer */
                        result = phLlcNfc_StartTimers(PH_LLCNFC_CONNECTIONTIMER, 0);
                        if (NFCSTATUS_SUCCESS == result)
                        {
                            ps_frame_info->retry_cnt++;
                            result = NFCSTATUS_PENDING;
                        }
                    }
                    else
                    {
                        if (NFCSTATUS_BUSY == PHNFCSTATUS(result))
                        {
                            result = NFCSTATUS_PENDING;
                        }
                    }
                }
                else
                {
                    PH_LLCNFC_PRINT("RETRY COUNT LIMIT REACHED \n");
                    if ((ps_frame_info->retry_cnt == PH_LLCNFC_MAX_RETRY_COUNT) 
                        && (NULL != gpphLlcNfc_Ctxt->cb_for_if.notify))
                    {
                        void            *p_hw_info = NULL;
                        uint8_t         type = 0;
                        
                        p_hw_info = gpphLlcNfc_Ctxt->phwinfo;
                        notifyinfo.status = PHNFCSTVAL(CID_NFC_LLC, 
                                            NFCSTATUS_BOARD_COMMUNICATION_ERROR);
                        
                        notifyul = gpphLlcNfc_Ctxt->cb_for_if.notify;
                        p_upperctxt = gpphLlcNfc_Ctxt->cb_for_if.pif_ctxt;
                        type = NFC_NOTIFY_ERROR;
                        if (init_u_rset_frame == ps_frame_info->sent_frame_type)
                        {
                            type = NFC_NOTIFY_INIT_FAILED;
                            /* Release if, the initialisation is not complete */
                            result = phLlcNfc_Release(gpphLlcNfc_Ctxt, p_hw_info);
                            gpphLlcNfc_Ctxt = NULL;
                        }
                        else
                        {
                            type = NFC_NOTIFY_DEVICE_ERROR;
                            notifyinfo.status = PHNFCSTVAL(CID_NFC_LLC, 
                                            NFCSTATUS_BOARD_COMMUNICATION_ERROR);
#if 0
                            phOsalNfc_RaiseException(phOsalNfc_e_UnrecovFirmwareErr,1); 
#endif /* #if 0 */
                        }
                        /* Notify the upper layer */
                        notifyul(p_upperctxt, p_hw_info, type, &notifyinfo);
                    }
                }
            }
#if !defined (CYCLIC_TIMER)
            else
            {
                /* Start the timer again */
                phOsalNfc_Timer_Start(
                            ps_timer_info->timer_id[PH_LLCNFC_CONNECTIONTIMER], 
                            ps_timer_info->con_to_value, phLlcNfc_ConnectionTimeoutCb, NULL);
            }
#endif
        }
    }
    PH_LLCNFC_PRINT("\n\nLLC : CONNECTION TIMEOUT CB END\n\n");
}
#endif /* #ifdef LLC_TIMER_ENABLE */

#ifdef LLC_URSET_NO_DELAY

    /* NO definition required */

#else /* #ifdef LLC_URSET_NO_DELAY */

void 
phLlcNfc_URSET_Delay_Notify (
    uint32_t            delay_id,
    void                *pContext)
{
    phLlcNfc_Frame_t            *ps_frame_info = NULL;
    phNfc_sCompletionInfo_t     notifyinfo = {0,0,0};
    
    if (NULL != gpphLlcNfc_Ctxt)
    {
        ps_frame_info = &(gpphLlcNfc_Ctxt->s_frameinfo);

        phOsalNfc_Timer_Stop (delay_id);
        phOsalNfc_Timer_Delete (delay_id);
        if (ps_frame_info->s_send_store.winsize_cnt > 0)
        {
#if 0

            /* Resend I frame */
            (void)phLlcNfc_H_SendTimedOutIFrame (gpphLlcNfc_Ctxt, 
                                        &(ps_frame_info->s_send_store), 0);

#else

            (void)phLlcNfc_H_SendUserIFrame (gpphLlcNfc_Ctxt, 
                                        &(ps_frame_info->s_send_store));

#endif /* #if 0 */
            gpphLlcNfc_Ctxt->state = phLlcNfc_Resend_State;
        }
        else 
        {
            if ((init_u_rset_frame == ps_frame_info->sent_frame_type) && 
                (NULL != gpphLlcNfc_Ctxt->cb_for_if.notify))
            {
                ps_frame_info->sent_frame_type = write_resp_received;
                notifyinfo.status = NFCSTATUS_SUCCESS;
                /* Send the notification to the upper layer */
                gpphLlcNfc_Ctxt->cb_for_if.notify (
                            gpphLlcNfc_Ctxt->cb_for_if.pif_ctxt, 
                            gpphLlcNfc_Ctxt->phwinfo, 
                            NFC_NOTIFY_INIT_COMPLETED, 
                            &notifyinfo);
            }
        }
    }
}

#endif /* #ifdef LLC_URSET_NO_DELAY */

