/******************************************************************************
 *
 *  Copyright (C) 2010-2013 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/


/******************************************************************************
 *
 *  Functions for handling NFC HAL NCI Transport events
 *
 ******************************************************************************/
#include <string.h>
#include "nfc_hal_int.h"
#include "nfc_hal_post_reset.h"
#include "userial.h"
#include "upio.h"

/****************************************************************************
** Definitions
****************************************************************************/

/* Default NFC HAL NCI port configuration  */
NFC_HAL_TRANS_CFG_QUALIFIER tNFC_HAL_TRANS_CFG nfc_hal_trans_cfg =
{
    NFC_HAL_SHARED_TRANSPORT_ENABLED,   /* bSharedTransport */
    USERIAL_BAUD_115200,                /* Baud rate */
    USERIAL_FC_HW                       /* Flow control */
};

/* Control block for NFC HAL NCI transport */
#if NFC_DYNAMIC_MEMORY == FALSE
tNFC_HAL_CB nfc_hal_cb;
#endif

extern tNFC_HAL_CFG *p_nfc_hal_cfg;
/****************************************************************************
** Internal function prototypes
****************************************************************************/
static void nfc_hal_main_userial_cback (tUSERIAL_PORT port, tUSERIAL_EVT evt, tUSERIAL_EVT_DATA *p_data);
static void nfc_hal_main_handle_terminate (void);
static void nfc_hal_main_timeout_cback (void *p_tle);

#if (NFC_HAL_DEBUG == TRUE)
const char * const nfc_hal_init_state_str[] =
{
    "IDLE",             /* Initialization is done                */
    "W4_XTAL_SET",      /* Waiting for crystal setting rsp       */
    "POST_XTAL_SET",    /* Waiting for reset ntf after xtal set  */
    "W4_NFCC_ENABLE",   /* Waiting for reset ntf atter REG_PU up */
    "W4_BUILD_INFO",    /* Waiting for build info rsp            */
    "W4_PATCH_INFO",    /* Waiting for patch info rsp            */
    "W4_APP_COMPL",     /* Waiting for complete from application */
    "W4_POST_INIT",     /* Waiting for complete of post init     */
    "W4_CONTROL",       /* Waiting for control release           */
    "W4_PREDISC",       /* Waiting for complete of prediscover   */
    "CLOSING"           /* Shutting down                         */
};
#endif

/*******************************************************************************
**
** Function         nfc_hal_main_init
**
** Description      This function initializes control block for NFC HAL
**
** Returns          nothing
**
*******************************************************************************/
void nfc_hal_main_init (void)
{
    /* Clear control block */
    memset (&nfc_hal_cb, 0, sizeof (tNFC_HAL_CB));

    nfc_hal_cb.ncit_cb.nci_ctrl_size   = NFC_HAL_NCI_INIT_CTRL_PAYLOAD_SIZE;
    nfc_hal_cb.trace_level             = NFC_HAL_INITIAL_TRACE_LEVEL;
    nfc_hal_cb.timer.p_cback           = nfc_hal_main_timeout_cback;
}

/*******************************************************************************
**
** Function         nfc_hal_main_open_transport
**
** Description      Open transport and prepare for new incoming message;
**
** Returns          nothing
**
*******************************************************************************/
static void nfc_hal_main_open_transport (void)
{
    tUSERIAL_OPEN_CFG open_cfg;

    /* Initialize control block */
    nfc_hal_cb.ncit_cb.rcv_state = NFC_HAL_RCV_IDLE_ST; /* to process packet type */

    if (nfc_hal_cb.ncit_cb.p_rcv_msg)
    {
        GKI_freebuf (nfc_hal_cb.ncit_cb.p_rcv_msg);
        nfc_hal_cb.ncit_cb.p_rcv_msg = NULL;
    }

    /* open transport */
    open_cfg.fmt    = (USERIAL_DATABITS_8 | USERIAL_PARITY_NONE | USERIAL_STOPBITS_1);
    open_cfg.baud   = nfc_hal_trans_cfg.userial_baud;
    open_cfg.fc     = nfc_hal_trans_cfg.userial_fc;
    open_cfg.buf    = USERIAL_BUF_BYTE;

    USERIAL_Open (USERIAL_NFC_PORT, &open_cfg, nfc_hal_main_userial_cback);

    {
        /* Wait for NFCC to enable - Core reset notification */
        NFC_HAL_SET_INIT_STATE (NFC_HAL_INIT_STATE_W4_NFCC_ENABLE);

        /* NFCC Enable timeout */
        nfc_hal_main_start_quick_timer (&nfc_hal_cb.timer, NFC_HAL_TTYPE_NFCC_ENABLE,
                                        ((p_nfc_hal_cfg->nfc_hal_nfcc_enable_timeout)*QUICK_TIMER_TICKS_PER_SEC)/1000);
    }
}

/*******************************************************************************
**
** Function         nfa_hal_pre_discover_done_cback
**
** Description      Pre-discovery CFG is sent.
**
** Returns          nothing
**
*******************************************************************************/
void nfa_hal_pre_discover_done_cback (tNFC_HAL_NCI_EVT event, UINT16 data_len, UINT8 *p_data)
{
    NFC_HAL_SET_INIT_STATE(NFC_HAL_INIT_STATE_IDLE);
    nfc_hal_main_stop_quick_timer (&nfc_hal_cb.ncit_cb.nci_wait_rsp_timer);
    nfc_hal_cb.p_stack_cback (HAL_NFC_PRE_DISCOVER_CPLT_EVT, HAL_NFC_STATUS_OK);
}

/*******************************************************************************
**
** Function         nfa_hal_send_pre_discover_cfg
**
** Description      sending Pre-discovery CFG
**
** Returns          nothing
**
*******************************************************************************/
void nfa_hal_send_pre_discover_cfg (void)
{
    if (nfc_hal_dm_set_config (p_nfc_hal_pre_discover_cfg [0],
                               &p_nfc_hal_pre_discover_cfg[1],
                                nfa_hal_pre_discover_done_cback) != HAL_NFC_STATUS_OK)
    {
        nfa_hal_pre_discover_done_cback(0, 0, NULL);
    }
}

/*******************************************************************************
**
** Function         nfc_hal_main_send_error
**
** Description      send an Error event to NFC stack
**
** Returns          nothing
**
*******************************************************************************/
void nfc_hal_main_send_error (tHAL_NFC_STATUS status)
{
    /* Notify stack */
    nfc_hal_cb.p_stack_cback(HAL_NFC_ERROR_EVT, status);
}

/*******************************************************************************
**
** Function         nfc_hal_main_userial_cback
**
** Description      USERIAL callback for NCI transport
**
** Returns          nothing
**
*******************************************************************************/
static void nfc_hal_main_userial_cback (tUSERIAL_PORT port, tUSERIAL_EVT evt, tUSERIAL_EVT_DATA *p_data)
{
    if (evt == USERIAL_RX_READY_EVT)
    {
        /* Notify transport task of serial port event */
        GKI_send_event (NFC_HAL_TASK, NFC_HAL_TASK_EVT_DATA_RDY);
    }
    else if (evt == USERIAL_TX_DONE_EVT)
    {
        /* Serial driver has finshed sending data from USERIAL_Write */
        /* Currently, no action is needed for this event */
    }
    else if (evt == USERIAL_ERR_EVT)
    {
        HAL_TRACE_ERROR0 ("nfc_hal_main_userial_cback: USERIAL_ERR_EVT. Notifying NFC_TASK of transport error");
        if (nfc_hal_cb.ncit_cb.nci_wait_rsp != NFC_HAL_WAIT_RSP_NONE)
        {
            nfc_hal_main_stop_quick_timer (&nfc_hal_cb.ncit_cb.nci_wait_rsp_timer);
            nfc_hal_nci_cmd_timeout_cback ((void *)&nfc_hal_cb.ncit_cb.nci_wait_rsp_timer);
        }
        else
        {
            nfc_hal_main_send_error (HAL_NFC_STATUS_ERR_TRANSPORT);
        }
    }
    else if (evt == USERIAL_WAKEUP_EVT)
    {
        HAL_TRACE_DEBUG1 ("nfc_hal_main_userial_cback: USERIAL_WAKEUP_EVT: %d", p_data->sigs);
    }
    else
    {
        HAL_TRACE_DEBUG1 ("nfc_hal_main_userial_cback: unhandled userial evt: %i", evt);
    }
}

/*******************************************************************************
**
** Function         nfc_hal_main_pre_init_done
**
** Description      notify complete of pre-initialization
**
** Returns          nothing
**
*******************************************************************************/
void nfc_hal_main_pre_init_done (tHAL_NFC_STATUS status)
{
    HAL_TRACE_DEBUG1 ("nfc_hal_main_pre_init_done () status = %d", status);

    if (status != HAL_NFC_STATUS_OK)
    {
        nfc_hal_main_handle_terminate ();

        /* Close uart */
        USERIAL_Close (USERIAL_NFC_PORT);
    }

    /* Notify NFC Task the status of initialization */
    nfc_hal_cb.p_stack_cback (HAL_NFC_OPEN_CPLT_EVT, status);
}

/*******************************************************************************
**
** Function         nfc_hal_main_timeout_cback
**
** Description      callback function for timeout
**
** Returns          void
**
*******************************************************************************/
static void nfc_hal_main_timeout_cback (void *p_tle)
{
    TIMER_LIST_ENT  *p_tlent = (TIMER_LIST_ENT *) p_tle;

    HAL_TRACE_DEBUG0 ("nfc_hal_main_timeout_cback ()");

    switch (p_tlent->event)
    {
    case NFC_HAL_TTYPE_POWER_CYCLE:
        nfc_hal_main_open_transport ();
        break;

    case NFC_HAL_TTYPE_NFCC_ENABLE:
        /* NFCC should have enabled now, notify transport openned */
        nfc_hal_dm_pre_init_nfcc ();
        break;

    default:
        HAL_TRACE_DEBUG1 ("nfc_hal_main_timeout_cback: unhandled timer event (0x%04x)", p_tlent->event);
        break;
    }
}

/*******************************************************************************
**
** Function         nfc_hal_main_handle_terminate
**
** Description      Handle NFI transport shutdown
**
** Returns          nothing
**
*******************************************************************************/
static void nfc_hal_main_handle_terminate (void)
{
    NFC_HDR *p_msg;

    /* dequeue and free buffer */
    if (nfc_hal_cb.ncit_cb.p_pend_cmd != NULL)
    {
        GKI_freebuf (nfc_hal_cb.ncit_cb.p_pend_cmd);
        nfc_hal_cb.ncit_cb.p_pend_cmd = NULL;
    }

    /* Free unsent nfc rx buffer */
    if (nfc_hal_cb.ncit_cb.p_rcv_msg)
    {
        GKI_freebuf (nfc_hal_cb.ncit_cb.p_rcv_msg);
        nfc_hal_cb.ncit_cb.p_rcv_msg  = NULL;
    }

    /* Free buffer for pending fragmented response/notification */
    if (nfc_hal_cb.ncit_cb.p_frag_msg)
    {
        GKI_freebuf (nfc_hal_cb.ncit_cb.p_frag_msg);
        nfc_hal_cb.ncit_cb.p_frag_msg = NULL;
    }

    /* Free buffers in the tx mbox */
    while ((p_msg = (NFC_HDR *) GKI_read_mbox (NFC_HAL_TASK_MBOX)) != NULL)
    {
        GKI_freebuf (p_msg);
    }

    /* notify closing transport */
    nfc_hal_dm_shutting_down_nfcc ();
}

/*******************************************************************************
**
** Function         nfc_hal_main_start_quick_timer
**
** Description      Start a timer for the specified amount of time.
**                  NOTE: The timeout resolution depends on including modules.
**                  QUICK_TIMER_TICKS_PER_SEC should be used to convert from
**                  time to ticks.
**
**
** Returns          void
**
*******************************************************************************/
void nfc_hal_main_start_quick_timer (TIMER_LIST_ENT *p_tle, UINT16 type, UINT32 timeout)
{
    NFC_HDR *p_msg;

    /* if timer list is currently empty, start periodic GKI timer */
    if (nfc_hal_cb.quick_timer_queue.p_first == NULL)
    {
        /* if timer starts on other than NCIT task (script wrapper) */
        if(GKI_get_taskid () != NFC_HAL_TASK)
        {
            /* post event to start timer in NCIT task */
            if ((p_msg = (NFC_HDR *) GKI_getbuf (NFC_HDR_SIZE)) != NULL)
            {
                p_msg->event = NFC_HAL_EVT_TO_START_QUICK_TIMER;
                GKI_send_msg (NFC_HAL_TASK, NFC_HAL_TASK_MBOX, p_msg);
            }
        }
        else
        {
            GKI_start_timer (NFC_HAL_QUICK_TIMER_ID, ((GKI_SECS_TO_TICKS (1) / QUICK_TIMER_TICKS_PER_SEC)), TRUE);
        }
    }

    GKI_remove_from_timer_list (&nfc_hal_cb.quick_timer_queue, p_tle);

    p_tle->event = type;
    p_tle->ticks = timeout; /* Save the number of ticks for the timer */

    GKI_add_to_timer_list (&nfc_hal_cb.quick_timer_queue, p_tle);
}

/*******************************************************************************
**
** Function         nfc_hal_main_stop_quick_timer
**
** Description      Stop a timer.
**
** Returns          void
**
*******************************************************************************/
void nfc_hal_main_stop_quick_timer (TIMER_LIST_ENT *p_tle)
{
    GKI_remove_from_timer_list (&nfc_hal_cb.quick_timer_queue, p_tle);

    /* if timer list is empty stop periodic GKI timer */
    if (nfc_hal_cb.quick_timer_queue.p_first == NULL)
    {
        GKI_stop_timer (NFC_HAL_QUICK_TIMER_ID);
    }
}

/*******************************************************************************
**
** Function         nfc_hal_main_process_quick_timer_evt
**
** Description      Process quick timer event
**
** Returns          void
**
*******************************************************************************/
static void nfc_hal_main_process_quick_timer_evt (void)
{
    TIMER_LIST_ENT  *p_tle;

    GKI_update_timer_list (&nfc_hal_cb.quick_timer_queue, 1);

    while ((nfc_hal_cb.quick_timer_queue.p_first) && (!nfc_hal_cb.quick_timer_queue.p_first->ticks))
    {
        p_tle = nfc_hal_cb.quick_timer_queue.p_first;
        GKI_remove_from_timer_list (&nfc_hal_cb.quick_timer_queue, p_tle);

        if (p_tle->p_cback)
        {
            (*p_tle->p_cback) (p_tle);
        }
    }

    /* if timer list is empty stop periodic GKI timer */
    if (nfc_hal_cb.quick_timer_queue.p_first == NULL)
    {
        GKI_stop_timer (NFC_HAL_QUICK_TIMER_ID);
    }
}

/*******************************************************************************
**
** Function         nfc_hal_send_nci_msg_to_nfc_task
**
** Description      This function is called to send nci message to nfc task
**
** Returns          void
**
*******************************************************************************/
void nfc_hal_send_nci_msg_to_nfc_task (NFC_HDR * p_msg)
{
#ifdef NFC_HAL_SHARED_GKI
    /* Using shared NFC/HAL GKI resources - send message buffer directly to NFC_TASK for processing */
    p_msg->event = BT_EVT_TO_NFC_NCI;
    GKI_send_msg (NFC_TASK, NFC_MBOX_ID, p_msg);
#else
    /* Send NCI message to the stack */
    nfc_hal_cb.p_data_cback (p_msg->len, (UINT8 *) ((p_msg + 1)
                                 + p_msg->offset));
    GKI_freebuf(p_msg);
#endif
}

/*******************************************************************************
**
** Function         nfc_hal_send_credit_ntf_for_cid
**
** Description      This function is called to send credit ntf
**                  for the specified connection id to nfc task
**
** Returns          void
**
*******************************************************************************/
static void nfc_hal_send_credit_ntf_for_cid (UINT8 cid)
{
    NFC_HDR  *p_msg;
    UINT8    *p, *ps;

    /* Start of new message. Allocate a buffer for message */
    if ((p_msg = (NFC_HDR *) GKI_getpoolbuf (NFC_HAL_NCI_POOL_ID)) != NULL)
    {
        /* Initialize NFC_HDR */
        p_msg->len    = NCI_DATA_HDR_SIZE + 0x03;
        p_msg->event  = 0;
        p_msg->offset = 0;
        p_msg->layer_specific = 0;

        p = (UINT8 *) (p_msg + 1) + p_msg->offset;
        ps = p;
        NCI_MSG_BLD_HDR0(p, NCI_MT_NTF, NCI_GID_CORE);
        NCI_MSG_BLD_HDR1(p, NCI_MSG_CORE_CONN_CREDITS);
        UINT8_TO_STREAM (p, 0x03);

        /* Number of credit entries */
        *p++ = 0x01;
        /* Connection id of the credit ntf */
        *p++ = cid;
        /* Number of credits */
        *p = 0x01;
#ifdef DISP_NCI
        DISP_NCI (ps, (UINT16) p_msg->len, TRUE);
#endif
        nfc_hal_send_nci_msg_to_nfc_task (p_msg);
    }
    else
    {
        HAL_TRACE_ERROR0 ("Unable to allocate buffer for Sending credit ntf to stack");
    }
}

/*******************************************************************************
**
** Function         nfc_hal_main_send_message
**
** Description      This function is calledto send an NCI message.
**
** Returns          void
**
*******************************************************************************/
static void nfc_hal_main_send_message (NFC_HDR *p_msg)
{
    UINT8   *ps, *pp, cid, pbf;
    UINT16  len = p_msg->len;
    UINT16  data_len;
#ifdef DISP_NCI
    UINT8   delta;
#endif

    HAL_TRACE_DEBUG1 ("nfc_hal_main_send_message() ls:0x%x", p_msg->layer_specific);
    if (  (p_msg->layer_specific == NFC_HAL_WAIT_RSP_CMD)
        ||(p_msg->layer_specific == NFC_HAL_WAIT_RSP_VSC)  )
    {
        nfc_hal_nci_send_cmd (p_msg);
    }
    else
    {
        /* NFC task has fragmented the data packet to the appropriate size
         * and data credit is available; just send it */

        /* add NCI packet type in front of message */
        nfc_hal_nci_add_nfc_pkt_type (p_msg);

        /* send this packet to transport */
        ps = (UINT8 *) (p_msg + 1) + p_msg->offset;
        pp = ps + 1;
#ifdef DISP_NCI
        delta = p_msg->len - len;
        DISP_NCI (ps + delta, (UINT16) (p_msg->len - delta), FALSE);
#endif
        if (nfc_hal_cb.hci_cb.hcp_conn_id)
        {
            NCI_DATA_PRS_HDR(pp, pbf, cid, data_len);
            if (cid == nfc_hal_cb.hci_cb.hcp_conn_id)
            {
                if (nfc_hal_hci_handle_hcp_pkt_to_hc (pp))
                {
                    HAL_TRACE_DEBUG0 ("nfc_hal_main_send_message() - Drop rsp to Fake cmd, Fake credit ntf");
                    GKI_freebuf (p_msg);
                    nfc_hal_send_credit_ntf_for_cid (cid);
                    return;
                }
            }

        }
        /* check low power mode state */
        if (nfc_hal_dm_power_mode_execute (NFC_HAL_LP_TX_DATA_EVT))
        {
            USERIAL_Write (USERIAL_NFC_PORT, ps, p_msg->len);
        }
        else
        {
            HAL_TRACE_ERROR0 ("nfc_hal_main_send_message(): drop data in low power mode");
        }
        GKI_freebuf (p_msg);
    }
}

/*******************************************************************************
**
** Function         nfc_hal_main_task
**
** Description      NFC HAL NCI transport event processing task
**
** Returns          0
**
*******************************************************************************/
UINT32 nfc_hal_main_task (UINT32 param)
{
    UINT16   event;
    UINT8    byte;
    UINT8    num_interfaces;
    UINT8    *p;
    NFC_HDR  *p_msg;
    BOOLEAN  free_msg;

    HAL_TRACE_DEBUG0 ("NFC_HAL_TASK started");

    /* Main loop */
    while (TRUE)
    {
        event = GKI_wait (0xFFFF, 0);

        /* Handle NFC_HAL_TASK_EVT_INITIALIZE (for initializing NCI transport) */
        if (event & NFC_HAL_TASK_EVT_INITIALIZE)
        {
            HAL_TRACE_DEBUG0 ("NFC_HAL_TASK got NFC_HAL_TASK_EVT_INITIALIZE signal. Opening NFC transport...");

            nfc_hal_main_open_transport ();
        }

        /* Check for terminate event */
        if (event & NFC_HAL_TASK_EVT_TERMINATE)
        {
            HAL_TRACE_DEBUG0 ("NFC_HAL_TASK got NFC_HAL_TASK_EVT_TERMINATE");
            nfc_hal_main_handle_terminate ();

            /* Close uart */
            USERIAL_Close (USERIAL_NFC_PORT);

            if (nfc_hal_cb.p_stack_cback)
            {
                nfc_hal_cb.p_stack_cback (HAL_NFC_CLOSE_CPLT_EVT, HAL_NFC_STATUS_OK);
                nfc_hal_cb.p_stack_cback = NULL;
            }
            continue;
        }

        /* Check for power cycle event */
        if (event & NFC_HAL_TASK_EVT_POWER_CYCLE)
        {
            HAL_TRACE_DEBUG0 ("NFC_HAL_TASK got NFC_HAL_TASK_EVT_POWER_CYCLE");
            nfc_hal_main_handle_terminate ();

            /* Close uart */
            USERIAL_Close (USERIAL_NFC_PORT);

            /* power cycle timeout */
            nfc_hal_main_start_quick_timer (&nfc_hal_cb.timer, NFC_HAL_TTYPE_POWER_CYCLE,
                                            (NFC_HAL_POWER_CYCLE_DELAY*QUICK_TIMER_TICKS_PER_SEC)/1000);
            continue;
        }

        /* NCI message ready to be sent to NFCC */
        if (event & NFC_HAL_TASK_EVT_MBOX)
        {
            while ((p_msg = (NFC_HDR *) GKI_read_mbox (NFC_HAL_TASK_MBOX)) != NULL)
            {
                free_msg = TRUE;
                switch (p_msg->event & NFC_EVT_MASK)
                {
                case NFC_HAL_EVT_TO_NFC_NCI:
                    nfc_hal_main_send_message (p_msg);
                    /* do not free buffer. NCI VS code may keep it for processing later */
                    free_msg = FALSE;
                    break;

                case NFC_HAL_EVT_POST_CORE_RESET:
                    NFC_HAL_SET_INIT_STATE (NFC_HAL_INIT_STATE_W4_POST_INIT_DONE);

                    /* set NCI Control packet size from CORE_INIT_RSP */
                    p = (UINT8 *) (p_msg + 1) + p_msg->offset + NCI_MSG_HDR_SIZE;
                    p += 5;
                    STREAM_TO_UINT8 (num_interfaces, p);
                    p += (num_interfaces + 3);
                    nfc_hal_cb.ncit_cb.nci_ctrl_size = *p;

                    /* start post initialization */
                    nfc_hal_cb.dev_cb.next_dm_config = NFC_HAL_DM_CONFIG_LPTD;
                    nfc_hal_cb.dev_cb.next_startup_vsc = 1;

                    nfc_hal_dm_config_nfcc ();
                    break;

                case NFC_HAL_EVT_TO_START_QUICK_TIMER:
                    GKI_start_timer (NFC_HAL_QUICK_TIMER_ID, ((GKI_SECS_TO_TICKS (1) / QUICK_TIMER_TICKS_PER_SEC)), TRUE);
                    break;

                case NFC_HAL_EVT_HCI:
                    nfc_hal_hci_evt_hdlr ((tNFC_HAL_HCI_EVENT_DATA *) p_msg);
                    break;

                case NFC_HAL_EVT_PRE_DISCOVER:
                    NFC_HAL_SET_INIT_STATE(NFC_HAL_INIT_STATE_W4_PREDISCOVER_DONE);
                    nfa_hal_send_pre_discover_cfg ();
                    break;

                case NFC_HAL_EVT_CONTROL_GRANTED:
                    nfc_hal_dm_send_pend_cmd ();
                    break;

                default:
                    break;
                }

                if (free_msg)
                    GKI_freebuf (p_msg);
            }
        }

        /* Data waiting to be read from serial port */
        if (event & NFC_HAL_TASK_EVT_DATA_RDY)
        {
            while (TRUE)
            {
                /* Read one byte to see if there is anything waiting to be read */
                if (USERIAL_Read (USERIAL_NFC_PORT, &byte, 1) == 0)
                {
                    break;
                }

                if (nfc_hal_nci_receive_msg (byte))
                {
                    /* complete of receiving NCI message */
                    nfc_hal_nci_assemble_nci_msg ();
                    if (nfc_hal_cb.ncit_cb.p_rcv_msg)
                    {
                        if (nfc_hal_nci_preproc_rx_nci_msg (nfc_hal_cb.ncit_cb.p_rcv_msg))
                        {
                            /* Send NCI message to the stack */
                            nfc_hal_send_nci_msg_to_nfc_task (nfc_hal_cb.ncit_cb.p_rcv_msg);
                        }
                        else
                        {
                            if (nfc_hal_cb.ncit_cb.p_rcv_msg)
                                GKI_freebuf(nfc_hal_cb.ncit_cb.p_rcv_msg);
                        }
                        nfc_hal_cb.ncit_cb.p_rcv_msg = NULL;
                    }
                }
            } /* while (TRUE) */
        }

        /* Process quick timer tick */
        if (event & NFC_HAL_QUICK_TIMER_EVT_MASK)
        {
            nfc_hal_main_process_quick_timer_evt ();
        }
    }

    HAL_TRACE_DEBUG0 ("nfc_hal_main_task terminated");

    GKI_exit_task (GKI_get_taskid ());
    return 0;
}

/*******************************************************************************
**
** Function         HAL_NfcSetTraceLevel
**
** Description      This function sets the trace level for HAL.  If called with
**                  a value of 0xFF, it simply returns the current trace level.
**
** Returns          The new or current trace level
**
*******************************************************************************/
UINT8 HAL_NfcSetTraceLevel (UINT8 new_level)
{
    if (new_level != 0xFF)
        nfc_hal_cb.trace_level = new_level;

    return (nfc_hal_cb.trace_level);
}
