/******************************************************************************
 *
 *  Copyright (C) 2012-2013 Broadcom Corporation
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
 *  Vendor-specific handler for HCI events
 *
 ******************************************************************************/
#include "gki.h"
#include "nfc_hal_api.h"
#include "nfc_hal_int.h"
#include "nfc_hal_nv_ci.h"
#include "nfc_hal_nv_co.h"

#include <string.h>
#include "nfc_hal_nv_co.h"

#ifndef NFC_HAL_HCI_NV_READ_TIMEOUT
#define NFC_HAL_HCI_NV_READ_TIMEOUT    1000
#endif

#ifndef NFC_HAL_HCI_NFCC_RSP_TIMEOUT
#define NFC_HAL_HCI_NFCC_RSP_TIMEOUT   3000
#endif

#define NFC_HAL_HCI_NETWK_CMD_TYPE_A_CE_PIPE_INFO_OFFSET    0x0C
#define NFC_HAL_HCI_NETWK_CMD_TYPE_B_CE_PIPE_INFO_OFFSET    0x32
#define NFC_HAL_HCI_NETWK_CMD_TYPE_BP_CE_PIPE_INFO_OFFSET   0x7F
#define NFC_HAL_HCI_NETWK_CMD_TYPE_F_CE_PIPE_INFO_OFFSET    0xB4

#define NFC_HAL_HCI_PIPE_VALID_MASK                         0x80

extern tNFC_HAL_CFG *p_nfc_hal_cfg;
/****************************************************************************
** Internal function prototypes
****************************************************************************/
static void nfc_hal_hci_set_next_hci_netwk_config (UINT8 block);
static void nfc_hal_hci_remove_dyn_pipe_to_uicc1 (void);
static void nfc_hal_hci_handle_nv_read (UINT8 block, tHAL_NFC_STATUS status, UINT16 size);
static void nfc_hal_hci_init_complete (tHAL_NFC_STATUS status);
static void nfc_hal_hci_vsc_cback (tNFC_HAL_NCI_EVT event, UINT16 data_len, UINT8 *p_data);

/*******************************************************************************
**
** Function         nfc_hal_hci_evt_hdlr
**
** Description      Processing event for NFA HCI
**
** Returns          None
**
*******************************************************************************/
void nfc_hal_hci_evt_hdlr (tNFC_HAL_HCI_EVENT_DATA *p_evt_data)
{
    HAL_TRACE_DEBUG0 ("nfc_hal_hci_evt_hdlr ()");

    switch (p_evt_data->hdr.event)
    {
    case NFC_HAL_HCI_RSP_NV_READ_EVT:
        if (  (nfc_hal_cb.hci_cb.p_hci_netwk_info_buf && (p_evt_data->nv_read.block == HC_F3_NV_BLOCK || p_evt_data->nv_read.block == HC_F4_NV_BLOCK))
            ||(nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf && p_evt_data->nv_read.block == HC_F2_NV_BLOCK)  )
        {
        nfc_hal_hci_handle_nv_read (p_evt_data->nv_read.block, p_evt_data->nv_read.status, p_evt_data->nv_read.size);
        }
        else
        {
            /* Invalid block or no buffer, Ignore */
            HAL_TRACE_ERROR1 ("nfc_hal_hci_evt_hdlr: No buffer for handling read NV block: 0x%02x", p_evt_data->nv_read.block);
        }
        break;

    case NFC_HAL_HCI_RSP_NV_WRITE_EVT:
        /* NV Ram write completed - nothing to do... */
        break;

    default:
        break;
    }
}

/*******************************************************************************
**
** Function         nfc_hal_hci_enable
**
** Description      Program nv data on to controller
**
** Returns          void
**
*******************************************************************************/
void nfc_hal_hci_enable (void)
{

    UINT8 *p_hci_netwk_cmd;

    HAL_TRACE_DEBUG0 ("nfc_hal_hci_enable ()");

    if (nfc_hal_cb.nvm_cb.nvm_type == NCI_SPD_NVM_TYPE_NONE)
    {
        HAL_TRACE_DEBUG1 ("nfc_hal_hci_enable (): No HCI NETWK CMD to send for NVM Type: 0x%02x", nfc_hal_cb.nvm_cb.nvm_type);
        nfc_hal_hci_init_complete (HAL_NFC_STATUS_OK);
        return;
    }

    if (nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf)
    {
        p_hci_netwk_cmd = (UINT8 *) (nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf - NCI_MSG_HDR_SIZE);
        GKI_freebuf (p_hci_netwk_cmd);
        nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf = NULL;
    }

    if (nfc_hal_cb.hci_cb.p_hci_netwk_info_buf)
    {
        p_hci_netwk_cmd = (UINT8 *) (nfc_hal_cb.hci_cb.p_hci_netwk_info_buf - NCI_MSG_HDR_SIZE);
        GKI_freebuf (p_hci_netwk_cmd);
        nfc_hal_cb.hci_cb.p_hci_netwk_info_buf = NULL;
    }

    if (  (p_nfc_hal_cfg->nfc_hal_hci_uicc_support & HAL_NFC_HCI_UICC0_HOST)
        ||((p_nfc_hal_cfg->nfc_hal_hci_uicc_support & HAL_NFC_HCI_UICC1_HOST) && ((!nfc_hal_cb.hci_cb.hci_fw_workaround) || (nfc_hal_cb.nvm_cb.nvm_type == NCI_SPD_NVM_TYPE_EEPROM)))  )
    {
        if ((p_hci_netwk_cmd = (UINT8 *) GKI_getbuf (NCI_MSG_HDR_SIZE + NFC_HAL_HCI_NETWK_INFO_SIZE)) == NULL)
        {
            HAL_TRACE_ERROR0 ("nfc_hal_hci_enable: unable to allocate buffer for reading hci network info from nvram");
            nfc_hal_hci_init_complete (HAL_NFC_STATUS_FAILED);
        }
        else
        {
            nfc_hal_cb.hci_cb.p_hci_netwk_info_buf   = (UINT8 *) (p_hci_netwk_cmd + NCI_MSG_HDR_SIZE);
            nfc_hal_cb.hci_cb.hci_netwk_config_block = 0;
            if (p_nfc_hal_cfg->nfc_hal_hci_uicc_support & HAL_NFC_HCI_UICC0_HOST)
            {
                memset (nfc_hal_cb.hci_cb.p_hci_netwk_info_buf, 0, NFC_HAL_HCI_NETWK_INFO_SIZE);
                nfc_hal_nv_co_read ((UINT8 *) nfc_hal_cb.hci_cb.p_hci_netwk_info_buf, NFC_HAL_HCI_NETWK_INFO_SIZE, HC_F3_NV_BLOCK);
                nfc_hal_main_start_quick_timer (&nfc_hal_cb.hci_cb.hci_timer, NFC_HAL_HCI_VSC_TIMEOUT_EVT, NFC_HAL_HCI_NV_READ_TIMEOUT);
            }
            else
            {
                HAL_TRACE_DEBUG1 ("nfc_hal_hci_enable (): Skip send F3 HCI NETWK CMD for UICC Mask: 0x%02x", p_nfc_hal_cfg->nfc_hal_hci_uicc_support);
                nfc_hal_hci_set_next_hci_netwk_config (HC_F3_NV_BLOCK);
            }

        }
    }
    else
    {
        HAL_TRACE_DEBUG2 ("nfc_hal_hci_enable (): No HCI NETWK CMD to send for UICC Mask: 0x%02x & NVM Type: 0x%02x", p_nfc_hal_cfg->nfc_hal_hci_uicc_support, nfc_hal_cb.nvm_cb.nvm_type);
        nfc_hal_hci_set_next_hci_netwk_config (HC_F2_NV_BLOCK);
    }
}

/*******************************************************************************
**
** Function         nfc_hal_hci_handle_hci_netwk_info
**
** Description      Handler function for HCI Network Notification
**
** Returns          None
**
*******************************************************************************/
void nfc_hal_hci_handle_hci_netwk_info (UINT8 *p_data)
{
    UINT8  *p = p_data;
    UINT16 data_len;
    UINT8  target_handle;
    UINT8   hci_netwk_cmd[1 + NFC_HAL_HCI_SESSION_ID_LEN];

    HAL_TRACE_DEBUG0 ("nfc_hal_hci_handle_hci_netwk_info ()");

    /* skip NCI header byte0 (MT,GID), byte1 (OID) */
    p += 2;

    STREAM_TO_UINT8 (data_len, p);
    target_handle = *(UINT8 *) p;

    if (target_handle == NFC_HAL_HCI_DH_TARGET_HANDLE)
        nfc_hal_nv_co_write (p, data_len, HC_F2_NV_BLOCK);

    else if (target_handle == NFC_HAL_HCI_UICC0_TARGET_HANDLE)
    {
        if (  (!nfc_hal_cb.hci_cb.hci_fw_validate_netwk_cmd)
            ||(p[NFC_HAL_HCI_NETWK_CMD_TYPE_A_CE_PIPE_INFO_OFFSET] & NFC_HAL_HCI_PIPE_VALID_MASK)
            ||(p[NFC_HAL_HCI_NETWK_CMD_TYPE_B_CE_PIPE_INFO_OFFSET] & NFC_HAL_HCI_PIPE_VALID_MASK)
            ||(p[NFC_HAL_HCI_NETWK_CMD_TYPE_BP_CE_PIPE_INFO_OFFSET] & NFC_HAL_HCI_PIPE_VALID_MASK)
            ||(p[NFC_HAL_HCI_NETWK_CMD_TYPE_F_CE_PIPE_INFO_OFFSET] & NFC_HAL_HCI_PIPE_VALID_MASK)  )
        {
            /* HCI Network notification received for UICC 0, Update nv data */
            nfc_hal_nv_co_write (p, data_len,HC_F3_NV_BLOCK);
        }
        else
        {
            HAL_TRACE_DEBUG1 ("nfc_hal_hci_handle_hci_netwk_info(): Type A Card Emulation invalid, Reset nv file: 0x%02x", p[NFC_HAL_HCI_NETWK_CMD_TYPE_A_CE_PIPE_INFO_OFFSET]);
            hci_netwk_cmd[0] = NFC_HAL_HCI_UICC0_TARGET_HANDLE;
            memset (&hci_netwk_cmd[1], 0xFF, NFC_HAL_HCI_SESSION_ID_LEN);
            nfc_hal_nv_co_write (hci_netwk_cmd, 1, HC_F3_NV_BLOCK);
        }
    }
    else if (target_handle == NFC_HAL_HCI_UICC1_TARGET_HANDLE)
    {
        if (  (!nfc_hal_cb.hci_cb.hci_fw_validate_netwk_cmd)
            ||(p[NFC_HAL_HCI_NETWK_CMD_TYPE_A_CE_PIPE_INFO_OFFSET] & NFC_HAL_HCI_PIPE_VALID_MASK)
            ||(p[NFC_HAL_HCI_NETWK_CMD_TYPE_B_CE_PIPE_INFO_OFFSET] & NFC_HAL_HCI_PIPE_VALID_MASK)
            ||(p[NFC_HAL_HCI_NETWK_CMD_TYPE_BP_CE_PIPE_INFO_OFFSET] & NFC_HAL_HCI_PIPE_VALID_MASK)
            ||(p[NFC_HAL_HCI_NETWK_CMD_TYPE_F_CE_PIPE_INFO_OFFSET] & NFC_HAL_HCI_PIPE_VALID_MASK)  )
        {
            /* HCI Network notification received for UICC 1, Update nv data */
            nfc_hal_nv_co_write (p, data_len,HC_F4_NV_BLOCK);
        }
        else
        {
            HAL_TRACE_DEBUG1 ("nfc_hal_hci_handle_hci_netwk_info(): Type A Card Emulation invalid, Reset nv file: 0x%02x", p[NFC_HAL_HCI_NETWK_CMD_TYPE_A_CE_PIPE_INFO_OFFSET]);
            hci_netwk_cmd[0] = NFC_HAL_HCI_UICC1_TARGET_HANDLE;
            /* Reset Session ID */
            memset (&hci_netwk_cmd[1], 0xFF, NFC_HAL_HCI_SESSION_ID_LEN);
            nfc_hal_nv_co_write (hci_netwk_cmd, 1, HC_F4_NV_BLOCK);
        }
    }
}

/*******************************************************************************
**
** Function         nfc_hal_hci_fake_adm_notify_all_pipe_cleared_to_dh
**
** Description      Fake ADM_NOTIFY_ALL_PIPE_CLEARED cmd to nfc task
**
** Returns          None
**
*******************************************************************************/
void nfc_hal_hci_fake_adm_notify_all_pipe_cleared_to_dh (void)
{
    NFC_HDR  *p_msg;
    UINT8 *p, *ps;

    HAL_TRACE_DEBUG1 ("nfc_hal_hci_fake_adm_notify_all_pipe_cleared_to_dh (): Fake ADM_NOTIFY_ALL_PIPE_CLEARED (0x%02x) from HAL", NFC_HAL_HCI_HOST_ID_UICC1);

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
        NCI_DATA_BLD_HDR (p, nfc_hal_cb.hci_cb.hcp_conn_id, 0x03);
        /* HCP header with ADMIN pipe id and chaining bit set */
        *p++ = ((1 << 0x07) | (NFC_HAL_HCI_ADMIN_PIPE & 0x7F));
        /* HCP Message header with Command type instruction and ADM_NOTIFY_ALL_PIPE_CLEARED command */
        *p++ = ((NFC_HAL_HCI_COMMAND_TYPE << 6) | (NFC_HAL_HCI_ADM_NOTIFY_ALL_PIPE_CLEARED & 0x3F));
        /* HCP Data with UICC1 host id */
        *p = NFC_HAL_HCI_HOST_ID_UICC1;

#ifdef DISP_NCI
        DISP_NCI (ps, (UINT16) p_msg->len, TRUE);
#endif
        nfc_hal_send_nci_msg_to_nfc_task (p_msg);

    }
    else
    {
        HAL_TRACE_ERROR0 ("Unable to allocate buffer for faking ADM_NOTIFY_ALL_PIPE_CLEARED cmd from HAL to stack");
    }
}

/*******************************************************************************
**
** Function         nfc_hal_hci_handle_hcp_pkt_to_hc
**
** Description      Handle HCP Packet from NFC task to Host Controller
**
** Returns          FALSE to send the packet to host controller
**                  TRUE to drop the packet and fake credit ntf for hcp connection
**
*******************************************************************************/
BOOLEAN nfc_hal_hci_handle_hcp_pkt_to_hc (UINT8 *p_data)
{
    UINT8   chaining_bit;
    UINT8   pipe;
    UINT8   type;
    UINT8   inst;
    UINT8   index;

    HAL_TRACE_DEBUG0 ("nfc_hal_hci_handle_hcp_pkt_to_hc ()");

    if (  (!nfc_hal_cb.hci_cb.hci_fw_workaround)
        ||(nfc_hal_cb.nvm_cb.nvm_type != NCI_SPD_NVM_TYPE_UICC)  )
    {
        /* Do nothing, just forward the stack hcp packet to host controller */
        return FALSE;
    }

    chaining_bit = ((*p_data) >> 0x07) & 0x01;
    pipe = (*p_data++) & 0x7F;

    if (  (chaining_bit)
        &&(pipe == NFC_HAL_HCI_ADMIN_PIPE)  )
    {
        type  = ((*p_data) >> 0x06) & 0x03;

        if (type == NFC_HAL_HCI_COMMAND_TYPE)
        {
            inst  = (*p_data++ & 0x3F);
            if (inst == NFC_HAL_HCI_ANY_SET_PARAMETER)
            {
                index = *(p_data++);
                if (index == NFC_HAL_HCI_WHITELIST_INDEX)
                {
                    /* Set flag to fake ADM_NOTIFY_ALL_PIPE_CLEARED cmd to nfc task after
                     * response from host controller to set whitelist cmd
                     */
                    nfc_hal_cb.hci_cb.clear_all_pipes_to_uicc1 = TRUE;
                }
            }
        }
        else if (type == NFC_HAL_HCI_RESPONSE_TYPE)
        {
            if (nfc_hal_cb.hci_cb.clear_all_pipes_to_uicc1)
            {
                /* Got response to the fake ADM_NOTIFY_ALL_PIPE_CLEARED cmd sent by HAL to nfc task */
                nfc_hal_cb.hci_cb.clear_all_pipes_to_uicc1 =  FALSE;
                /* return TRUE to drop this hcp without forwarding to host controller */
                return TRUE;
            }
        }
    }

    return FALSE;
}

/*******************************************************************************
**
** Function         nfc_hal_hci_handle_hcp_pkt_from_hc
**
** Description      Handle HCP Packet from Host controller to Terminal Host
**
** Returns          None
**
*******************************************************************************/
void nfc_hal_hci_handle_hcp_pkt_from_hc (UINT8 *p_data)
{
    UINT8   chaining_bit;
    UINT8   pipe;
    UINT8   type;
    UINT8   inst;
    UINT8   hci_netwk_cmd[1 + NFC_HAL_HCI_SESSION_ID_LEN];
    UINT8   source_host;

    HAL_TRACE_DEBUG0 ("nfc_hal_hci_handle_hcp_pkt_from_hc ()");

    if (!nfc_hal_cb.hci_cb.hci_fw_workaround)
        return;

    chaining_bit = ((*p_data) >> 0x07) & 0x01;
    pipe = (*p_data++) & 0x7F;

    if (  (chaining_bit)
        &&(pipe == NFC_HAL_HCI_ADMIN_PIPE)  )
    {
        type  = ((*p_data) >> 0x06) & 0x03;

        if (type == NFC_HAL_HCI_COMMAND_TYPE)
        {
            inst  = (*p_data++ & 0x3F);

            if (inst == NFC_HAL_HCI_ADM_NOTIFY_ALL_PIPE_CLEARED)
            {

                STREAM_TO_UINT8 (source_host, p_data);

                HAL_TRACE_DEBUG1 ("nfc_hal_hci_handle_hcp_pkt_from_hc (): Received ADM_NOTIFY_ALL_PIPE_CLEARED command for UICC: 0x%02x", source_host);
                if (source_host == NFC_HAL_HCI_HOST_ID_UICC0)
                {
                    hci_netwk_cmd[0] = NFC_HAL_HCI_UICC0_TARGET_HANDLE;
                    /* Reset Session ID */
                    memset (&hci_netwk_cmd[1], 0xFF, NFC_HAL_HCI_SESSION_ID_LEN);
                    nfc_hal_nv_co_write (hci_netwk_cmd, 1, HC_F3_NV_BLOCK);
                    HAL_TRACE_DEBUG1 ("nfc_hal_hci_handle_hcp_pkt_from_hc (): Sent command to reset nv file for block: 0x%02x", HC_F3_NV_BLOCK);
                }
                else if (source_host == NFC_HAL_HCI_HOST_ID_UICC1)
                {
                    hci_netwk_cmd[0] = NFC_HAL_HCI_UICC1_TARGET_HANDLE;
                    /* Reset Session ID */
                    memset (&hci_netwk_cmd[1], 0xFF, NFC_HAL_HCI_SESSION_ID_LEN);
                    nfc_hal_nv_co_write (hci_netwk_cmd, 1, HC_F4_NV_BLOCK);
                    HAL_TRACE_DEBUG1 ("nfc_hal_hci_handle_hcp_pkt_from_hc (): Sent command to reset nv file for block: 0x%02x", HC_F4_NV_BLOCK);
                }
            }
        }
        else if (type == NFC_HAL_HCI_RESPONSE_TYPE)
        {
            if (nfc_hal_cb.hci_cb.clear_all_pipes_to_uicc1)
            {
                /* NVM Type is UICC and got response from host controller
                 * to Set whitelist command. Now fake ADM_NOTIFY_ALL_PIPE_CLEARED cmd to
                 * NFC Task and then forward the whitelist cmd response
                 */
                nfc_hal_hci_fake_adm_notify_all_pipe_cleared_to_dh ();
            }
        }
    }
}

/*******************************************************************************
**
** Function         nfc_hal_hci_handle_nv_read
**
** Description      handler function for nv read complete event
**
** Returns          None
**
*******************************************************************************/
void nfc_hal_hci_handle_nv_read (UINT8 block, tHAL_NFC_STATUS status, UINT16 size)
{
    UINT8   *p;
    UINT8   *p_hci_netwk_info = NULL;

    HAL_TRACE_DEBUG3 ("nfc_hal_hci_handle_nv_read (): Block: [0x%02x], Status: [0x%02x], Size: [0x%04x]", block, status, size);

    /* Stop timer as NVDATA Read Completed */
    nfc_hal_main_stop_quick_timer (&nfc_hal_cb.hci_cb.hci_timer);

    switch (block)
    {
    case HC_F3_NV_BLOCK:
    case HC_F4_NV_BLOCK:
        if (  (status != HAL_NFC_STATUS_OK)
            ||(size > NFC_HAL_HCI_NETWK_INFO_SIZE)
            ||(size < NFC_HAL_HCI_MIN_NETWK_INFO_SIZE)
            ||((nfc_hal_cb.hci_cb.hci_fw_workaround) && (block == HC_F4_NV_BLOCK) && (nfc_hal_cb.nvm_cb.nvm_type == NCI_SPD_NVM_TYPE_UICC))  )
        {
            HAL_TRACE_DEBUG1 ("nfc_hal_hci_handle_nv_read: Invalid data from nv memory, Set DEFAULT Configuration for block:0x%02x", block);
            memset (nfc_hal_cb.hci_cb.p_hci_netwk_info_buf, 0, NFC_HAL_HCI_NETWK_INFO_SIZE);
            nfc_hal_cb.hci_cb.p_hci_netwk_info_buf[0] = (block == HC_F3_NV_BLOCK) ? NFC_HAL_HCI_UICC0_TARGET_HANDLE : NFC_HAL_HCI_UICC1_TARGET_HANDLE;
            memset (&nfc_hal_cb.hci_cb.p_hci_netwk_info_buf[1], 0xFF, NFC_HAL_HCI_SESSION_ID_LEN);
            size = NFC_HAL_HCI_NETWK_INFO_SIZE;
        }

        p_hci_netwk_info = (UINT8 *) nfc_hal_cb.hci_cb.p_hci_netwk_info_buf - NCI_MSG_HDR_SIZE;
        break;

    case HC_F2_NV_BLOCK:

        if (  (status != HAL_NFC_STATUS_OK)
            ||(size > NFC_HAL_HCI_DH_NETWK_INFO_SIZE)
            ||(size < NFC_HAL_HCI_MIN_DH_NETWK_INFO_SIZE)  )
        {
            HAL_TRACE_DEBUG1 ("nfc_hal_hci_handle_nv_read: Invalid data from nv memory, Set DEFAULT Configuration for block:0x%02x", block);
            memset (nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf, 0, NFC_HAL_HCI_DH_NETWK_INFO_SIZE);
            nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf[0] = NFC_HAL_HCI_DH_TARGET_HANDLE;
            memset (&nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf[1], 0xFF, NFC_HAL_HCI_SESSION_ID_LEN);
            size = NFC_HAL_HCI_DH_NETWK_INFO_SIZE;
            p_hci_netwk_info = (UINT8 *) nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf - NCI_MSG_HDR_SIZE;
        }
        else
        {
            if ((nfc_hal_cb.hci_cb.hci_fw_workaround) && (nfc_hal_cb.nvm_cb.nvm_type == NCI_SPD_NVM_TYPE_UICC))
            {
                /* if NVM Type is UICC, then UICC1 will find session id mismatch when activated for patch download,
                 * and will remove pipes connected to DH even before DH is enabled, So DH will update NFCC
                 * control block by removing all dynamic pipes connected to UICC1 */

                nfc_hal_hci_remove_dyn_pipe_to_uicc1 ();
                size = NFC_HAL_HCI_DH_NETWK_INFO_SIZE;
            }
            p_hci_netwk_info = (UINT8 *) (nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf - NCI_MSG_HDR_SIZE);
        }
        break;

    default:
        return;
    }

        p = p_hci_netwk_info;
        /* Send HCI Network ntf command using nv data */
        NCI_MSG_BLD_HDR0 (p, NCI_MT_CMD, NCI_GID_PROP);
        NCI_MSG_BLD_HDR1 (p, NCI_MSG_HCI_NETWK);
        UINT8_TO_STREAM (p, (UINT8) size);

        nfc_hal_dm_send_nci_cmd (p_hci_netwk_info, (UINT16) (NCI_MSG_HDR_SIZE + size), nfc_hal_hci_vsc_cback);

        nfc_hal_cb.hci_cb.hci_netwk_config_block = block;
}

/*******************************************************************************
**
** Function         nfc_hal_hci_remove_dyn_pipe_to_uicc1
**
** Description      Prepare hci network command read from nv file removing
**                  all pipes connected to UICC1
**
** Returns          None
**
*******************************************************************************/
void nfc_hal_hci_remove_dyn_pipe_to_uicc1 (void)
{
    UINT8 *p, *np;
    UINT8 num_dyn_pipes = 0, new_num_dyn_pipes = 0;
    UINT8 xx;
    UINT8 source_host, dest_host, pipe_id;

    HAL_TRACE_DEBUG0 ("nfc_hal_hci_remove_dyn_pipe_to_uicc1 ()");

    p  = (UINT8 *) (nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf + NFC_HAL_HCI_MIN_DH_NETWK_INFO_SIZE);
    np = p;
    num_dyn_pipes = *(p - 1);

    for (xx = 0; xx < num_dyn_pipes; xx++,p += NFC_HAL_HCI_PIPE_INFO_SIZE)
    {
        source_host = *(UINT8 *) (p);
        dest_host   = *(UINT8 *) (p + 1);
        pipe_id     = *(UINT8 *) (p + 4);

        if ((source_host != NFC_HAL_HCI_HOST_ID_UICC1) && (dest_host != NFC_HAL_HCI_HOST_ID_UICC1))
        {
            memcpy (np, p, NFC_HAL_HCI_PIPE_INFO_SIZE);
            np += NFC_HAL_HCI_PIPE_INFO_SIZE;
            new_num_dyn_pipes++;
        }
    }

    memset ((UINT8 *) (np), 0, NFC_HAL_HCI_PIPE_INFO_SIZE * (20 - new_num_dyn_pipes));

    /* Update number of pipes after removing pipes connected to UICC1 */
    p = (UINT8 *) (nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf + NFC_HAL_HCI_MIN_DH_NETWK_INFO_SIZE);
    *(p - 1) = new_num_dyn_pipes;
}

/*******************************************************************************
**
** Function         nfc_hal_hci_init_complete
**
** Description      Notify VSC initialization is complete
**
** Returns          None
**
*******************************************************************************/
void nfc_hal_hci_init_complete (tHAL_NFC_STATUS status)
{
    UINT8 *p_hci_netwk_cmd;

    HAL_TRACE_DEBUG1 ("nfc_hal_hci_init_complete (): Status: [0x%02x]", status);

    if (nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf)
    {
        p_hci_netwk_cmd = (UINT8 *) (nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf - NCI_MSG_HDR_SIZE);
        GKI_freebuf (p_hci_netwk_cmd);
        nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf = NULL;
    }

    if (nfc_hal_cb.hci_cb.p_hci_netwk_info_buf)
    {
        p_hci_netwk_cmd = (UINT8 *) (nfc_hal_cb.hci_cb.p_hci_netwk_info_buf - NCI_MSG_HDR_SIZE);
        GKI_freebuf (p_hci_netwk_cmd);
        nfc_hal_cb.hci_cb.p_hci_netwk_info_buf = NULL;
    }

    NFC_HAL_SET_INIT_STATE (NFC_HAL_INIT_STATE_IDLE);

    nfc_hal_cb.p_stack_cback (HAL_NFC_POST_INIT_CPLT_EVT, status);
}

/*******************************************************************************
**
** Function         nfc_hal_hci_set_next_hci_netwk_config
**
** Description      set next hci network configuration
**
** Returns          None
**
*******************************************************************************/
void nfc_hal_hci_set_next_hci_netwk_config (UINT8 block)
{
    UINT8 *p_hci_netwk_cmd;

    HAL_TRACE_DEBUG1 ("nfc_hal_hci_set_next_hci_netwk_config (): Block: [0x%02x]", block);

    switch (block)
    {
    case HC_F3_NV_BLOCK:
        if (  (p_nfc_hal_cfg->nfc_hal_hci_uicc_support & HAL_NFC_HCI_UICC1_HOST)
            &&(nfc_hal_cb.hci_cb.p_hci_netwk_info_buf)
            &&((!nfc_hal_cb.hci_cb.hci_fw_workaround) || (nfc_hal_cb.nvm_cb.nvm_type == NCI_SPD_NVM_TYPE_EEPROM))  )
        {
            /* Send command to read nvram data for 0xF4 */
            memset (nfc_hal_cb.hci_cb.p_hci_netwk_info_buf, 0, NFC_HAL_HCI_NETWK_INFO_SIZE);
            nfc_hal_nv_co_read ((UINT8 *) nfc_hal_cb.hci_cb.p_hci_netwk_info_buf, NFC_HAL_HCI_NETWK_INFO_SIZE, HC_F4_NV_BLOCK);
            nfc_hal_main_start_quick_timer (&nfc_hal_cb.hci_cb.hci_timer, NFC_HAL_HCI_VSC_TIMEOUT_EVT, NFC_HAL_HCI_NV_READ_TIMEOUT);
            break;
        }
        HAL_TRACE_DEBUG2 ("nfc_hal_hci_set_next_hci_netwk_config (): Skip send F4 HCI NETWK CMD for UICC Mask: 0x%02x & NVM Type: 0x%02x", p_nfc_hal_cfg->nfc_hal_hci_uicc_support, nfc_hal_cb.nvm_cb.nvm_type);

    case HC_F4_NV_BLOCK:
        if ((p_hci_netwk_cmd = (UINT8 *) GKI_getbuf (NCI_MSG_HDR_SIZE + NFC_HAL_HCI_DH_NETWK_INFO_SIZE)) == NULL)
        {
            HAL_TRACE_ERROR0 ("nfc_hal_hci_set_next_hci_netwk_config: unable to allocate buffer for reading hci network info from nvram");
            nfc_hal_hci_init_complete (HAL_NFC_STATUS_FAILED);
        }
        else
        {
            nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf   = (UINT8 *) (p_hci_netwk_cmd + NCI_MSG_HDR_SIZE);
            /* Send command to read nvram data for 0xF2 */
            memset (nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf, 0, NFC_HAL_HCI_DH_NETWK_INFO_SIZE);
            nfc_hal_nv_co_read ((UINT8 *) nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf, NFC_HAL_HCI_DH_NETWK_INFO_SIZE, HC_F2_NV_BLOCK);
            nfc_hal_main_start_quick_timer (&nfc_hal_cb.hci_cb.hci_timer, NFC_HAL_HCI_VSC_TIMEOUT_EVT, NFC_HAL_HCI_NV_READ_TIMEOUT);
        }
        break;

    case HC_F2_NV_BLOCK:
        nfc_hal_hci_init_complete (HAL_NFC_STATUS_OK);
        break;

    default:
        HAL_TRACE_ERROR1 ("nfc_hal_hci_set_next_hci_netwk_config: unable to allocate buffer to send VSC 0x%02x", block);
        /* Brcm initialization failed */
        nfc_hal_hci_init_complete (HAL_NFC_STATUS_FAILED);
        break;
    }
}

/*******************************************************************************
**
** Function         nfc_hal_hci_vsc_cback
**
** Description      process VS callback event from stack
**
** Returns          none
**
*******************************************************************************/
static void nfc_hal_hci_vsc_cback (tNFC_HAL_NCI_EVT event, UINT16 data_len, UINT8 *p_data)
{
    UINT8 *p_ret = NULL;
    UINT8 status;

    p_ret  = p_data + NCI_MSG_HDR_SIZE;
    status = *p_ret;

    HAL_TRACE_DEBUG3 ("nfc_hal_hci_vsc_cback (): Event: [0x%02x], Data length: [0x%04x], Status: [0x%02x]", event, data_len, status);

    if (event  != NFC_VS_HCI_NETWK_RSP)
        return;

    if (status != HAL_NFC_STATUS_OK)
    {
        nfc_hal_hci_init_complete (HAL_NFC_STATUS_FAILED);
        return;
    }

    switch (nfc_hal_cb.hci_cb.hci_netwk_config_block)
    {
    case HC_F3_NV_BLOCK:
    case HC_F4_NV_BLOCK:
    case HC_F2_NV_BLOCK:
        nfc_hal_hci_set_next_hci_netwk_config (nfc_hal_cb.hci_cb.hci_netwk_config_block);
        break;

    default:
        /* Ignore the event */
        break;
    }
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
void nfc_hal_hci_timeout_cback (void *p_tle)
{
    TIMER_LIST_ENT  *p_tlent = (TIMER_LIST_ENT *)p_tle;

    HAL_TRACE_DEBUG0 ("nfc_hal_hci_timeout_cback ()");

    if (p_tlent->event == NFC_HAL_HCI_VSC_TIMEOUT_EVT)
    {
        HAL_TRACE_ERROR0 ("nfc_hal_hci_timeout_cback: Timeout - NFC HAL HCI BRCM Initialization Failed!");
        nfc_hal_hci_init_complete (HAL_NFC_STATUS_FAILED);
    }
}

