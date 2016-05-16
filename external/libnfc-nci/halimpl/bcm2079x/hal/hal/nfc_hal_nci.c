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
 *  This file contains function of the NFC unit to receive/process NCI/VS
 *  commands/responses.
 *
 ******************************************************************************/
#include <string.h>
#include "nfc_hal_int.h"
#include "nfc_hal_post_reset.h"
#include "userial.h"
#include "nci_defs.h"


/*****************************************************************************
** Constants and types
*****************************************************************************/

/*****************************************************************************
** Local function prototypes
*****************************************************************************/

/*******************************************************************************
**
** Function         nfc_hal_nci_assemble_nci_msg
**
** Description      This function is called to reassemble the received NCI
**                  response/notification packet, if required.
**                  (The data packets are posted to NFC task for reassembly)
**
** Returns          void.
**
*******************************************************************************/
void nfc_hal_nci_assemble_nci_msg (void)
{
    NFC_HDR *p_msg = nfc_hal_cb.ncit_cb.p_rcv_msg;
    UINT8 u8;
    UINT8 *p, *pp;
    UINT8 hdr[2];
    UINT8   *ps, *pd;
    UINT16  size, needed;
    BOOLEAN disp_again = FALSE;

    if ((p_msg == NULL) || (p_msg->len < NCI_MSG_HDR_SIZE))
        return;

#ifdef DISP_NCI
    DISP_NCI ((UINT8 *) (p_msg + 1) + p_msg->offset, (UINT16) (p_msg->len), TRUE);
#endif

    p       = (UINT8 *) (p_msg + 1) + p_msg->offset;
    u8      = *p++;
    /* remove the PBF bit for potential reassembly later */
    hdr[0]  = u8 & ~NCI_PBF_MASK;
    if ((u8 & NCI_MT_MASK) == NCI_MT_DATA)
    {
        /* clear the RFU in octet1 */
        *(p) = 0;
        /* data packet reassembly is performed in NFC task */
        return;
    }
    else
    {
        *(p) &= NCI_OID_MASK;
    }

    hdr[1]  = *p;
    pp = hdr;
    /* save octet0 and octet1 of an NCI header in layer_specific for the received packet */
    STREAM_TO_UINT16 (p_msg->layer_specific, pp);

    if (nfc_hal_cb.ncit_cb.p_frag_msg)
    {
        if (nfc_hal_cb.ncit_cb.p_frag_msg->layer_specific != p_msg->layer_specific)
        {
            /* check if these fragments are of the same NCI message */
            HAL_TRACE_ERROR2 ("nfc_hal_nci_assemble_nci_msg() - different messages 0x%x, 0x%x!!", nfc_hal_cb.ncit_cb.p_frag_msg->layer_specific, p_msg->layer_specific);
            nfc_hal_cb.ncit_cb.nci_ras  |= NFC_HAL_NCI_RAS_ERROR;
        }
        else if (nfc_hal_cb.ncit_cb.nci_ras == 0)
        {
            disp_again = TRUE;
            /* if not previous reassembly error, append the new fragment */
            p_msg->offset   += NCI_MSG_HDR_SIZE;
            p_msg->len      -= NCI_MSG_HDR_SIZE;
            size    = GKI_get_buf_size (nfc_hal_cb.ncit_cb.p_frag_msg);
            needed  = (NFC_HDR_SIZE + nfc_hal_cb.ncit_cb.p_frag_msg->len + nfc_hal_cb.ncit_cb.p_frag_msg->offset + p_msg->len);
            if (size >= needed)
            {
                /* the buffer for reassembly is big enough to append the new fragment */
                ps   = (UINT8 *) (p_msg + 1) + p_msg->offset;
                pd   = (UINT8 *) (nfc_hal_cb.ncit_cb.p_frag_msg + 1) + nfc_hal_cb.ncit_cb.p_frag_msg->offset + nfc_hal_cb.ncit_cb.p_frag_msg->len;
                memcpy (pd, ps, p_msg->len);
                nfc_hal_cb.ncit_cb.p_frag_msg->len  += p_msg->len;
                /* adjust the NCI packet length */
                pd   = (UINT8 *) (nfc_hal_cb.ncit_cb.p_frag_msg + 1) + nfc_hal_cb.ncit_cb.p_frag_msg->offset + 2;
                *pd  = (UINT8) (nfc_hal_cb.ncit_cb.p_frag_msg->len - NCI_MSG_HDR_SIZE);
            }
            else
            {
                nfc_hal_cb.ncit_cb.nci_ras  |= NFC_HAL_NCI_RAS_TOO_BIG;
                HAL_TRACE_ERROR2 ("nfc_hal_nci_assemble_nci_msg() buffer overrun (%d + %d)!!", nfc_hal_cb.ncit_cb.p_frag_msg->len, p_msg->len);
            }
        }
        /* we are done with this new fragment, free it */
        GKI_freebuf (p_msg);
    }
    else
    {
        nfc_hal_cb.ncit_cb.p_frag_msg = p_msg;
    }


    if ((u8 & NCI_PBF_MASK) == NCI_PBF_NO_OR_LAST)
    {
        /* last fragment */
        p_msg               = nfc_hal_cb.ncit_cb.p_frag_msg;
        p                   = (UINT8 *) (p_msg + 1) + p_msg->offset;
        *p                  = u8; /* this should make the PBF flag as Last Fragment */
        nfc_hal_cb.ncit_cb.p_frag_msg  = NULL;

        p_msg->layer_specific = nfc_hal_cb.ncit_cb.nci_ras;
        /* still report the data packet, if the incoming packet is too big */
        if (nfc_hal_cb.ncit_cb.nci_ras & NFC_HAL_NCI_RAS_ERROR)
        {
            /* NFCC reported NCI fragments for different NCI messages and this is the last fragment - drop it */
            HAL_TRACE_ERROR0 ("nfc_hal_nci_assemble_nci_msg() clearing NCI_RAS_ERROR");
            GKI_freebuf (p_msg);
            p_msg = NULL;
        }
#ifdef DISP_NCI
        if ((nfc_hal_cb.ncit_cb.nci_ras == 0) && (disp_again))
        {
            DISP_NCI ((UINT8 *) (p_msg + 1) + p_msg->offset, (UINT16) (p_msg->len), TRUE);
        }
#endif
        /* clear the error flags, so the next NCI packet is clean */
        nfc_hal_cb.ncit_cb.nci_ras = 0;
    }
    else
    {
        /* still reassembling */
        p_msg = NULL;
    }

    nfc_hal_cb.ncit_cb.p_rcv_msg = p_msg;
}

/*****************************************************************************
**
** Function         nfc_hal_nci_receive_nci_msg
**
** Description
**      Handle incoming data (NCI events) from the serial port.
**
**      If there is data waiting from the serial port, this funciton reads the
**      data and parses it. Once an entire NCI message has been read, it sends
**      the message the the NFC_TASK for processing
**
*****************************************************************************/
static BOOLEAN nfc_hal_nci_receive_nci_msg (tNFC_HAL_NCIT_CB *p_cb, UINT8 byte)
{
    UINT16      len;
    BOOLEAN     msg_received = FALSE;

    switch (p_cb->rcv_state)
    {
    case NFC_HAL_RCV_NCI_MSG_ST:

        /* Initialize rx parameters */
        p_cb->rcv_state = NFC_HAL_RCV_NCI_HDR_ST;
        p_cb->rcv_len   = NCI_MSG_HDR_SIZE;

        /* Start of new message. Allocate a buffer for message */
        if ((p_cb->p_rcv_msg = (NFC_HDR *) GKI_getpoolbuf (NFC_HAL_NCI_POOL_ID)) != NULL)
        {
            /* Initialize NFC_HDR */
            p_cb->p_rcv_msg->len    = 0;
            p_cb->p_rcv_msg->event  = 0;
            p_cb->p_rcv_msg->offset = 0;

            *((UINT8 *) (p_cb->p_rcv_msg + 1) + p_cb->p_rcv_msg->offset + p_cb->p_rcv_msg->len++) = byte;
        }
        else
        {
            HAL_TRACE_ERROR0 ("Unable to allocate buffer for incoming NCI message.");
        }
        p_cb->rcv_len--;
        break;

    case NFC_HAL_RCV_NCI_HDR_ST:

        if (p_cb->p_rcv_msg)
        {
            *((UINT8 *) (p_cb->p_rcv_msg + 1) + p_cb->p_rcv_msg->offset + p_cb->p_rcv_msg->len++) = byte;
        }

        p_cb->rcv_len--;

        /* Check if we read in entire NFC message header yet */
        if (p_cb->rcv_len == 0)
        {
            p_cb->rcv_len       = byte;

            /* If non-zero payload, then go to receive-data state */
            if (byte > 0)
            {
                p_cb->rcv_state = NFC_HAL_RCV_NCI_PAYLOAD_ST;
            }
            else
            {
                msg_received    = TRUE;
                p_cb->rcv_state = NFC_HAL_RCV_IDLE_ST;
            }
        }
        break;

    case NFC_HAL_RCV_NCI_PAYLOAD_ST:

        p_cb->rcv_len--;
        if (p_cb->p_rcv_msg)
        {
            *((UINT8 *) (p_cb->p_rcv_msg + 1) + p_cb->p_rcv_msg->offset + p_cb->p_rcv_msg->len++) = byte;

            if (p_cb->rcv_len > 0)
            {
                /* Read in the rest of the message */
                len = USERIAL_Read (USERIAL_NFC_PORT, ((UINT8 *) (p_cb->p_rcv_msg + 1) + p_cb->p_rcv_msg->offset + p_cb->p_rcv_msg->len),  p_cb->rcv_len);
                p_cb->p_rcv_msg->len    += len;
                p_cb->rcv_len           -= len;
            }
        }

        /* Check if we read in entire message yet */
        if (p_cb->rcv_len == 0)
        {
            msg_received    = TRUE;
            p_cb->rcv_state = NFC_HAL_RCV_IDLE_ST;
        }
        break;
    }

    return (msg_received);
}

/*****************************************************************************
**
** Function         nfc_hal_nci_receive_bt_msg
**
** Description
**      Handle incoming BRCM specific data from the serial port.
**
**      If there is data waiting from the serial port, this funciton reads the
**      data and parses it. Once an entire message has been read, it returns
**      TRUE.
**
*****************************************************************************/
static BOOLEAN nfc_hal_nci_receive_bt_msg (tNFC_HAL_NCIT_CB *p_cb, UINT8 byte)
{
    UINT16  len;
    BOOLEAN msg_received = FALSE;

    switch (p_cb->rcv_state)
    {
    case NFC_HAL_RCV_BT_MSG_ST:

        /* Initialize rx parameters */
        p_cb->rcv_state = NFC_HAL_RCV_BT_HDR_ST;
        p_cb->rcv_len   = HCIE_PREAMBLE_SIZE;

        if ((p_cb->p_rcv_msg = (NFC_HDR *) GKI_getpoolbuf (NFC_HAL_NCI_POOL_ID)) != NULL)
        {
            /* Initialize NFC_HDR */
            p_cb->p_rcv_msg->len    = 0;
            p_cb->p_rcv_msg->event  = 0;
            p_cb->p_rcv_msg->offset = 0;

            *((UINT8 *) (p_cb->p_rcv_msg + 1) + p_cb->p_rcv_msg->offset + p_cb->p_rcv_msg->len++) = byte;
        }
        else
        {
            HAL_TRACE_ERROR0 ("[nfc] Unable to allocate buffer for incoming NCI message.");
        }
        p_cb->rcv_len--;
        break;

    case NFC_HAL_RCV_BT_HDR_ST:
        if (p_cb->p_rcv_msg)
        {
            *((UINT8 *) (p_cb->p_rcv_msg + 1) + p_cb->p_rcv_msg->offset + p_cb->p_rcv_msg->len++) = byte;
        }
        p_cb->rcv_len--;

        /* Check if we received entire preamble yet */
        if (p_cb->rcv_len == 0)
        {
            /* Received entire preamble. Length is in the last byte(s) of the preamble */
            p_cb->rcv_len = byte;

            /* Verify that buffer is big enough to fit message */
            if ((sizeof (NFC_HDR) + HCIE_PREAMBLE_SIZE + byte) > GKI_get_buf_size (p_cb->p_rcv_msg))
            {
                /* Message cannot fit into buffer */
                GKI_freebuf (p_cb->p_rcv_msg);
                p_cb->p_rcv_msg     = NULL;

                HAL_TRACE_ERROR0 ("Invalid length for incoming BT HCI message.");
            }

            /* Message length is valid */
            if (byte)
            {
                /* Read rest of message */
                p_cb->rcv_state = NFC_HAL_RCV_BT_PAYLOAD_ST;
            }
            else
            {
                /* Message has no additional parameters. (Entire message has been received) */
                msg_received    = TRUE;
                p_cb->rcv_state = NFC_HAL_RCV_IDLE_ST;  /* Next, wait for packet type of next message */
            }
        }
        break;

    case NFC_HAL_RCV_BT_PAYLOAD_ST:
        p_cb->rcv_len--;
        if (p_cb->p_rcv_msg)
        {
            *((UINT8 *) (p_cb->p_rcv_msg + 1) + p_cb->p_rcv_msg->offset + p_cb->p_rcv_msg->len++) = byte;

            if (p_cb->rcv_len > 0)
            {
                /* Read in the rest of the message */
                len = USERIAL_Read (USERIAL_NFC_PORT, ((UINT8 *) (p_cb->p_rcv_msg + 1) + p_cb->p_rcv_msg->offset + p_cb->p_rcv_msg->len),  p_cb->rcv_len);
                p_cb->p_rcv_msg->len    += len;
                p_cb->rcv_len           -= len;
            }
        }

        /* Check if we read in entire message yet */
        if (p_cb->rcv_len == 0)
        {
            msg_received        = TRUE;
            p_cb->rcv_state     = NFC_HAL_RCV_IDLE_ST;      /* Next, wait for packet type of next message */
        }
        break;
    }

    /* If we received entire message */
#if (NFC_HAL_TRACE_PROTOCOL == TRUE)
    if (msg_received && p_cb->p_rcv_msg)
    {
        /* Display protocol trace message */
        DispHciEvt (p_cb->p_rcv_msg);
    }
#endif

    return (msg_received);
}

/*******************************************************************************
**
** Function         nfc_hal_nci_proc_rx_bt_msg
**
** Description      Received BT message from NFCC
**
**                  Notify command complete if initializing NFCC
**                  Forward BT message to NFC task
**
** Returns          void
**
*******************************************************************************/
static void nfc_hal_nci_proc_rx_bt_msg (void)
{
    UINT8   *p;
    NFC_HDR *p_msg;
    UINT16  opcode, old_opcode;
    tNFC_HAL_BTVSC_CPLT       vcs_cplt_params;
    tNFC_HAL_BTVSC_CPLT_CBACK *p_cback = NULL;

    /* if complete BT message is received successfully */
    if (nfc_hal_cb.ncit_cb.p_rcv_msg)
    {
        p_msg   = nfc_hal_cb.ncit_cb.p_rcv_msg;
        HAL_TRACE_DEBUG1 ("nfc_hal_nci_proc_rx_bt_msg (): GOT an BT msgs init_sta:%d", nfc_hal_cb.dev_cb.initializing_state);
        HAL_TRACE_DEBUG2 ("event: 0x%x, wait_rsp:0x%x", p_msg->event, nfc_hal_cb.ncit_cb.nci_wait_rsp);
        /* increase the cmd window here */
        if (nfc_hal_cb.ncit_cb.nci_wait_rsp == NFC_HAL_WAIT_RSP_PROP)
        {
            p = (UINT8 *) (p_msg + 1) + p_msg->offset;
            if (*p == HCI_COMMAND_COMPLETE_EVT)
            {
                p  += 3; /* code, len, cmd window */
                STREAM_TO_UINT16 (opcode, p);
                p   = nfc_hal_cb.ncit_cb.last_hdr;
                STREAM_TO_UINT16 (old_opcode, p);
                if (opcode == old_opcode)
                {
                    nfc_hal_cb.ncit_cb.nci_wait_rsp = NFC_HAL_WAIT_RSP_NONE;
                    p_cback = (tNFC_HAL_BTVSC_CPLT_CBACK *)nfc_hal_cb.ncit_cb.p_vsc_cback;
                    nfc_hal_cb.ncit_cb.p_vsc_cback  = NULL;
                    nfc_hal_main_stop_quick_timer (&nfc_hal_cb.ncit_cb.nci_wait_rsp_timer);
                }
            }
        }

        /* if initializing BRCM NFCC */
        if ((nfc_hal_cb.dev_cb.initializing_state == NFC_HAL_INIT_STATE_W4_APP_COMPLETE) ||
            (nfc_hal_cb.dev_cb.initializing_state == NFC_HAL_INIT_STATE_W4_CONTROL_DONE))
        {
            /* this is command complete event for baud rate update or download patch */
            p = (UINT8 *) (p_msg + 1) + p_msg->offset;

            p += 1;    /* skip opcode */
            STREAM_TO_UINT8  (vcs_cplt_params.param_len, p);

            p += 1;    /* skip num command packets */
            STREAM_TO_UINT16 (vcs_cplt_params.opcode, p);

            vcs_cplt_params.param_len -= 3;
            vcs_cplt_params.p_param_buf = p;

            if (nfc_hal_cb.dev_cb.initializing_state == NFC_HAL_INIT_STATE_W4_CONTROL_DONE)
            {
                NFC_HAL_SET_INIT_STATE(NFC_HAL_INIT_STATE_IDLE);
                nfc_hal_cb.p_stack_cback (HAL_NFC_RELEASE_CONTROL_EVT, HAL_NFC_STATUS_OK);
            }
            if (p_cback)
            {
                nfc_hal_cb.ncit_cb.p_vsc_cback = NULL;
                (*p_cback) (&vcs_cplt_params);
            }

            /* do not BT send message to NFC task */
            GKI_freebuf (p_msg);
        }
        else
        {
            /* do not BT send message to NFC task */
            GKI_freebuf(nfc_hal_cb.ncit_cb.p_rcv_msg);
        }
        nfc_hal_cb.ncit_cb.p_rcv_msg = NULL;
    }
}

/*****************************************************************************
**
** Function         nfc_hal_nci_receive_msg
**
** Description
**      Handle incoming data (NCI events) from the serial port.
**
**      If there is data waiting from the serial port, this funciton reads the
**      data and parses it. Once an entire NCI message has been read, it sends
**      the message the the NFC_TASK for processing
**
*****************************************************************************/
BOOLEAN nfc_hal_nci_receive_msg (UINT8 byte)
{
    tNFC_HAL_NCIT_CB *p_cb = &(nfc_hal_cb.ncit_cb);
    BOOLEAN msg_received = FALSE;

    if (p_cb->rcv_state == NFC_HAL_RCV_IDLE_ST)
    {
        /* if this is NCI message */
        if (byte == HCIT_TYPE_NFC)
        {
            p_cb->rcv_state = NFC_HAL_RCV_NCI_MSG_ST;
        }
        /* if this is BT message */
        else if (byte == HCIT_TYPE_EVENT)
        {
            p_cb->rcv_state = NFC_HAL_RCV_BT_MSG_ST;
        }
        else
        {
            HAL_TRACE_ERROR1 ("Unknown packet type drop this byte 0x%x", byte);
        }
    }
    else if (p_cb->rcv_state <= NFC_HAL_RCV_NCI_PAYLOAD_ST)
    {
        msg_received = nfc_hal_nci_receive_nci_msg (p_cb, byte);
    }
    else
    {
        if (nfc_hal_nci_receive_bt_msg (p_cb, byte))
        {
            /* received BT message */
            nfc_hal_nci_proc_rx_bt_msg ();
        }
    }

    return (msg_received);
}

/*******************************************************************************
**
** Function         nfc_hal_nci_preproc_rx_nci_msg
**
** Description      NFCC sends NCI message to DH while initializing NFCC
**                  processing low power mode
**
** Returns          TRUE, if NFC task need to receive NCI message
**
*******************************************************************************/
BOOLEAN nfc_hal_nci_preproc_rx_nci_msg (NFC_HDR *p_msg)
{
    UINT8 *p, *pp, cid;
    UINT8 mt, pbf, gid, op_code;
    UINT8 payload_len;
    UINT16 data_len;

    HAL_TRACE_DEBUG0 ("nfc_hal_nci_preproc_rx_nci_msg()");

    /* if initializing BRCM NFCC */
    if (nfc_hal_cb.dev_cb.initializing_state != NFC_HAL_INIT_STATE_IDLE)
    {
        nfc_hal_dm_proc_msg_during_init (p_msg);
        /* do not send message to NFC task while initializing NFCC */
        return (FALSE);
    }
    else
    {
        p = (UINT8 *) (p_msg + 1) + p_msg->offset;
        pp = p;
        NCI_MSG_PRS_HDR0 (p, mt, pbf, gid);
        NCI_MSG_PRS_HDR1 (p, op_code);
        payload_len = *p++;

        if (mt == NCI_MT_DATA)
        {
            if (nfc_hal_cb.hci_cb.hcp_conn_id)
            {
                NCI_DATA_PRS_HDR(pp, pbf, cid, data_len);
                if (cid == nfc_hal_cb.hci_cb.hcp_conn_id)
                {
                    nfc_hal_hci_handle_hcp_pkt_from_hc (pp);
                }

            }
        }

        if (gid == NCI_GID_PROP) /* this is for hci netwk ntf */
        {
            if (mt == NCI_MT_NTF)
            {
                if (op_code == NCI_MSG_HCI_NETWK)
                {
                    nfc_hal_hci_handle_hci_netwk_info ((UINT8 *) (p_msg + 1) + p_msg->offset);
                }
            }
        }
        else if (gid == NCI_GID_RF_MANAGE)
        {
            if (mt == NCI_MT_NTF)
            {
                if (op_code == NCI_MSG_RF_INTF_ACTIVATED)
                {
                    if ((nfc_hal_cb.max_rf_credits) && (payload_len > 5))
                    {
                        /* API used wants to limit the RF data credits */
                        p += 5; /* skip RF disc id, interface, protocol, tech&mode, payload size */
                        if (*p > nfc_hal_cb.max_rf_credits)
                        {
                            HAL_TRACE_DEBUG2 ("RfDataCredits %d->%d", *p, nfc_hal_cb.max_rf_credits);
                            *p = nfc_hal_cb.max_rf_credits;
                        }
                    }
                }
            }
        }
        else if (gid == NCI_GID_CORE)
        {
            if (mt == NCI_MT_RSP)
            {
                if (op_code == NCI_MSG_CORE_CONN_CREATE)
                {
                    if (nfc_hal_cb.hci_cb.b_wait_hcp_conn_create_rsp)
                    {
                        p++; /* skip status byte */
                        nfc_hal_cb.hci_cb.b_wait_hcp_conn_create_rsp = FALSE;
                        p++; /* skip buff size */
                        p++; /* num of buffers */
                        nfc_hal_cb.hci_cb.hcp_conn_id = *p;
                    }
                }
            }
        }
    }

    if (nfc_hal_cb.dev_cb.power_mode == NFC_HAL_POWER_MODE_FULL)
    {
        if (nfc_hal_cb.dev_cb.snooze_mode != NFC_HAL_LP_SNOOZE_MODE_NONE)
        {
            /* extend idle timer */
            nfc_hal_dm_power_mode_execute (NFC_HAL_LP_RX_DATA_EVT);
        }
    }

    return (TRUE);
}

/*******************************************************************************
**
** Function         nfc_hal_nci_add_nfc_pkt_type
**
** Description      Add packet type (HCIT_TYPE_NFC)
**
** Returns          TRUE, if NFCC can receive NCI message
**
*******************************************************************************/
void nfc_hal_nci_add_nfc_pkt_type (NFC_HDR *p_msg)
{
    UINT8   *p;
    UINT8   hcit;

    /* add packet type in front of NCI header */
    if (p_msg->offset > 0)
    {
        p_msg->offset--;
        p_msg->len++;

        p  = (UINT8 *) (p_msg + 1) + p_msg->offset;
        *p = HCIT_TYPE_NFC;
    }
    else
    {
        HAL_TRACE_ERROR0 ("nfc_hal_nci_add_nfc_pkt_type () : No space for packet type");
        hcit = HCIT_TYPE_NFC;
        USERIAL_Write (USERIAL_NFC_PORT, &hcit, 1);
    }
}

/*******************************************************************************
**
** Function         nci_brcm_check_cmd_create_hcp_connection
**
** Description      Check if this is command to create HCP connection
**
** Returns          None
**
*******************************************************************************/
static void nci_brcm_check_cmd_create_hcp_connection (NFC_HDR *p_msg)
{
    UINT8 *p;
    UINT8 mt, pbf, gid, op_code;

    nfc_hal_cb.hci_cb.b_wait_hcp_conn_create_rsp = FALSE;

    p = (UINT8 *) (p_msg + 1) + p_msg->offset;

    if (nfc_hal_cb.dev_cb.initializing_state == NFC_HAL_INIT_STATE_IDLE)
    {
        NCI_MSG_PRS_HDR0 (p, mt, pbf, gid);
        NCI_MSG_PRS_HDR1 (p, op_code);

        if (gid == NCI_GID_CORE)
        {
            if (mt == NCI_MT_CMD)
            {
                if (op_code == NCI_MSG_CORE_CONN_CREATE)
                {
                    if (  ((NCI_CORE_PARAM_SIZE_CON_CREATE + 4) == *p++)
                        &&(NCI_DEST_TYPE_NFCEE == *p++)
                        &&(1 == *p++)
                        &&(NCI_CON_CREATE_TAG_NFCEE_VAL == *p++)
                        &&(2 == *p++)  )
                    {
                        p++;
                        if (NCI_NFCEE_INTERFACE_HCI_ACCESS == *p)
                        {
                            nfc_hal_cb.hci_cb.b_wait_hcp_conn_create_rsp = TRUE;
                            return;
                        }
                    }

                }
            }
        }
    }
}

/*******************************************************************************
**
** Function         nfc_hal_nci_send_cmd
**
** Description      Send NCI command to the transport
**
** Returns          void
**
*******************************************************************************/
void nfc_hal_nci_send_cmd (NFC_HDR *p_buf)
{
    BOOLEAN continue_to_process = TRUE;
    UINT8   *ps, *pd;
    UINT16  max_len;
    UINT16  buf_len, offset;
    UINT8   *p;
    UINT8   hdr[NCI_MSG_HDR_SIZE];
    UINT8   nci_ctrl_size = nfc_hal_cb.ncit_cb.nci_ctrl_size;
    UINT8   delta = 0;

    if (  (nfc_hal_cb.hci_cb.hcp_conn_id == 0)
        &&(nfc_hal_cb.nvm_cb.nvm_type != NCI_SPD_NVM_TYPE_NONE)  )
        nci_brcm_check_cmd_create_hcp_connection ((NFC_HDR*) p_buf);

    /* check low power mode state */
    continue_to_process = nfc_hal_dm_power_mode_execute (NFC_HAL_LP_TX_DATA_EVT);

    if (!continue_to_process)
    {
        /* save the command to be sent until NFCC is free. */
        nfc_hal_cb.ncit_cb.p_pend_cmd   = p_buf;
        return;
    }

    max_len = nci_ctrl_size + NCI_MSG_HDR_SIZE;
    buf_len = p_buf->len;
    offset  = p_buf->offset;
#ifdef DISP_NCI
    if (buf_len > max_len)
    {
        /* this command needs to be fragmented. display the complete packet first */
        DISP_NCI ((UINT8 *) (p_buf + 1) + p_buf->offset, p_buf->len, FALSE);
    }
#endif
    ps      = (UINT8 *) (p_buf + 1) + p_buf->offset;
    memcpy (hdr, ps, NCI_MSG_HDR_SIZE);
    while (buf_len > max_len)
    {
        HAL_TRACE_DEBUG2 ("buf_len (%d) > max_len (%d)", buf_len, max_len);
        /* the NCI command is bigger than the NFCC Max Control Packet Payload Length
         * fragment the command */

        p_buf->len  = max_len;
        ps   = (UINT8 *) (p_buf + 1) + p_buf->offset;
        /* mark the control packet as fragmented */
        *ps |= NCI_PBF_ST_CONT;
        /* adjust the length of this fragment */
        ps  += 2;
        *ps  = nci_ctrl_size;

        /* add NCI packet type in front of message */
        nfc_hal_nci_add_nfc_pkt_type (p_buf);

        /* send this fragment to transport */
        p = (UINT8 *) (p_buf + 1) + p_buf->offset;

#ifdef DISP_NCI
        delta = p_buf->len - max_len;
        DISP_NCI (p + delta, (UINT16) (p_buf->len - delta), FALSE);
#endif
        USERIAL_Write (USERIAL_NFC_PORT, p, p_buf->len);

        /* adjust the len and offset to reflect that part of the command is already sent */
        buf_len -= nci_ctrl_size;
        offset  += nci_ctrl_size;
        HAL_TRACE_DEBUG2 ("p_buf->len: %d buf_len (%d)", p_buf->len, buf_len);
        p_buf->len      = buf_len;
        p_buf->offset   = offset;
        pd   = (UINT8 *) (p_buf + 1) + p_buf->offset;
        /* restore the NCI header */
        memcpy (pd, hdr, NCI_MSG_HDR_SIZE);
        pd  += 2;
        *pd  = (UINT8) (p_buf->len - NCI_MSG_HDR_SIZE);
    }

    HAL_TRACE_DEBUG1 ("p_buf->len: %d", p_buf->len);

    /* add NCI packet type in front of message */
    nfc_hal_nci_add_nfc_pkt_type (p_buf);

    /* send this fragment to transport */
    p = (UINT8 *) (p_buf + 1) + p_buf->offset;

#ifdef DISP_NCI
    delta = p_buf->len - buf_len;
    DISP_NCI (p + delta, (UINT16) (p_buf->len - delta), FALSE);
#endif
    USERIAL_Write (USERIAL_NFC_PORT, p, p_buf->len);

    GKI_freebuf (p_buf);
}

/*******************************************************************************
**
** Function         nfc_hal_nci_cmd_timeout_cback
**
** Description      callback function for timeout
**
** Returns          void
**
*******************************************************************************/
void nfc_hal_nci_cmd_timeout_cback (void *p_tle)
{
    TIMER_LIST_ENT  *p_tlent = (TIMER_LIST_ENT *)p_tle;

    HAL_TRACE_DEBUG0 ("nfc_hal_nci_cmd_timeout_cback ()");

    nfc_hal_cb.ncit_cb.nci_wait_rsp = NFC_HAL_WAIT_RSP_NONE;

    if (p_tlent->event == NFC_HAL_TTYPE_NCI_WAIT_RSP)
    {
        if (nfc_hal_cb.dev_cb.initializing_state <= NFC_HAL_INIT_STATE_W4_PATCH_INFO)
        {
            NFC_HAL_SET_INIT_STATE (NFC_HAL_INIT_STATE_IDLE);
            nfc_hal_main_pre_init_done (HAL_NFC_STATUS_ERR_CMD_TIMEOUT);
        }
        else if (nfc_hal_cb.dev_cb.initializing_state == NFC_HAL_INIT_STATE_W4_APP_COMPLETE)
        {
            if (nfc_hal_cb.prm.state != NFC_HAL_PRM_ST_IDLE)
            {
                nfc_hal_prm_process_timeout (NULL);
            }
            else
            {
                NFC_HAL_SET_INIT_STATE (NFC_HAL_INIT_STATE_IDLE);
                nfc_hal_main_pre_init_done (HAL_NFC_STATUS_ERR_CMD_TIMEOUT);
            }
        }
        else if (nfc_hal_cb.dev_cb.initializing_state == NFC_HAL_INIT_STATE_W4_POST_INIT_DONE)
        {
            NFC_HAL_SET_INIT_STATE (NFC_HAL_INIT_STATE_IDLE);
            nfc_hal_cb.p_stack_cback (HAL_NFC_POST_INIT_CPLT_EVT, HAL_NFC_STATUS_ERR_CMD_TIMEOUT);
        }
        else if (nfc_hal_cb.dev_cb.initializing_state == NFC_HAL_INIT_STATE_W4_CONTROL_DONE)
        {
            NFC_HAL_SET_INIT_STATE(NFC_HAL_INIT_STATE_IDLE);
            nfc_hal_cb.p_stack_cback (HAL_NFC_RELEASE_CONTROL_EVT, HAL_NFC_STATUS_ERR_CMD_TIMEOUT);
        }
        else if (nfc_hal_cb.dev_cb.initializing_state == NFC_HAL_INIT_STATE_W4_PREDISCOVER_DONE)
        {
            NFC_HAL_SET_INIT_STATE(NFC_HAL_INIT_STATE_IDLE);
            nfc_hal_cb.p_stack_cback (HAL_NFC_PRE_DISCOVER_CPLT_EVT, HAL_NFC_STATUS_ERR_CMD_TIMEOUT);
        }
    }
}


/*******************************************************************************
**
** Function         HAL_NfcSetMaxRfDataCredits
**
** Description      This function sets the maximum RF data credit for HAL.
**                  If 0, use the value reported from NFCC.
**
** Returns          none
**
*******************************************************************************/
void HAL_NfcSetMaxRfDataCredits (UINT8 max_credits)
{
    HAL_TRACE_DEBUG2 ("HAL_NfcSetMaxRfDataCredits %d->%d", nfc_hal_cb.max_rf_credits, max_credits);
    nfc_hal_cb.max_rf_credits   = max_credits;
}
