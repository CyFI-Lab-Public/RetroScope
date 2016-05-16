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
 *  This file contains the action functions for NFA-EE
 *
 ******************************************************************************/
#include <string.h>
#include "nfa_sys.h"
#include "nfa_api.h"
#include "nfa_dm_int.h"
#include "nfa_sys_int.h"
#include "nfc_api.h"
#include "nfa_ee_int.h"


/* the de-bounce timer:
 * The NFA-EE API functions are called to set the routing and VS configuration.
 * When this timer expires, the configuration is sent to NFCC all at once.
 * This is the timeout value for the de-bounce timer. */
#ifndef NFA_EE_ROUT_TIMEOUT_VAL
#define NFA_EE_ROUT_TIMEOUT_VAL         1000
#endif

#define NFA_EE_ROUT_BUF_SIZE            540
#define NFA_EE_ROUT_ONE_TECH_CFG_LEN    4
#define NFA_EE_ROUT_ONE_PROTO_CFG_LEN   4
#define NFA_EE_ROUT_MAX_TLV_SIZE        0xFD


/* the following 2 tables convert the technology mask in API and control block to the command for NFCC */
#define NFA_EE_NUM_TECH     3
const UINT8 nfa_ee_tech_mask_list[NFA_EE_NUM_TECH] =
{
    NFA_TECHNOLOGY_MASK_A,
    NFA_TECHNOLOGY_MASK_B,
    NFA_TECHNOLOGY_MASK_F
};

const UINT8 nfa_ee_tech_list[NFA_EE_NUM_TECH] =
{
    NFC_RF_TECHNOLOGY_A,
    NFC_RF_TECHNOLOGY_B,
    NFC_RF_TECHNOLOGY_F
};

/* the following 2 tables convert the protocol mask in API and control block to the command for NFCC */
#define NFA_EE_NUM_PROTO     5
const UINT8 nfa_ee_proto_mask_list[NFA_EE_NUM_PROTO] =
{
    NFA_PROTOCOL_MASK_T1T,
    NFA_PROTOCOL_MASK_T2T,
    NFA_PROTOCOL_MASK_T3T,
    NFA_PROTOCOL_MASK_ISO_DEP,
    NFA_PROTOCOL_MASK_NFC_DEP
};

const UINT8 nfa_ee_proto_list[NFA_EE_NUM_PROTO] =
{
    NFC_PROTOCOL_T1T,
    NFC_PROTOCOL_T2T,
    NFC_PROTOCOL_T3T,
    NFC_PROTOCOL_ISO_DEP,
    NFC_PROTOCOL_NFC_DEP
};

static void nfa_ee_report_discover_req_evt(void);
static void nfa_ee_build_discover_req_evt (tNFA_EE_DISCOVER_REQ *p_evt_data);
/*******************************************************************************
**
** Function         nfa_ee_conn_cback
**
** Description      process connection callback event from stack
**
** Returns          void
**
*******************************************************************************/
static void nfa_ee_conn_cback (UINT8 conn_id, tNFC_CONN_EVT event, tNFC_CONN *p_data)
{
    BT_HDR             *p_msg;
    tNFA_EE_NCI_CONN    cbk;

    NFA_TRACE_DEBUG2("nfa_ee_conn_cback: conn_id: %d, event=0x%02x", conn_id, event);

    cbk.hdr.event   = NFA_EE_NCI_CONN_EVT;
    if (event == NFC_DATA_CEVT)
    {
        /* Treat data event specially to avoid potential memory leak */
        cbk.hdr.event   = NFA_EE_NCI_DATA_EVT;
    }
    cbk.conn_id     = conn_id;
    cbk.event       = event;
    cbk.p_data      = p_data;
    p_msg           = (BT_HDR *)&cbk;

    nfa_ee_evt_hdlr (p_msg);
}


/*******************************************************************************
**
** Function         nfa_ee_find_total_aid_len
**
** Description      Find the total len in aid_cfg from start_entry to the last
**
** Returns          void
**
*******************************************************************************/
int nfa_ee_find_total_aid_len(tNFA_EE_ECB *p_cb, int start_entry)
{
    int len = 0, xx;

    if (p_cb->aid_entries > start_entry)
    {
        for (xx = start_entry; xx < p_cb->aid_entries; xx++)
        {
            len += p_cb->aid_len[xx];
        }
    }
    return len;
}




/*******************************************************************************
**
** Function         nfa_ee_find_aid_offset
**
** Description      Given the AID, find the associated tNFA_EE_ECB and the
**                  offset in aid_cfg[]. *p_entry is the index.
**
** Returns          void
**
*******************************************************************************/
tNFA_EE_ECB * nfa_ee_find_aid_offset(UINT8 aid_len, UINT8 *p_aid, int *p_offset, int *p_entry)
{
    int  xx, yy, aid_len_offset, offset;
    tNFA_EE_ECB *p_ret = NULL, *p_ecb;

    p_ecb = &nfa_ee_cb.ecb[NFA_EE_CB_4_DH];
    aid_len_offset = 1; /* skip the tag */
    for (yy = 0; yy < nfa_ee_cb.cur_ee; yy++, p_ecb++)
    {
        if (p_ecb->aid_entries)
        {
            offset = 0;
            for (xx = 0; xx < p_ecb->aid_entries; xx++)
            {
                if (  (p_ecb->aid_cfg[offset + aid_len_offset] == aid_len)
                    &&(memcmp(&p_ecb->aid_cfg[offset + aid_len_offset + 1], p_aid, aid_len) == 0)  )
                {
                    p_ret = p_ecb;
                    if (p_offset)
                        *p_offset = offset;
                    if (p_entry)
                        *p_entry  = xx;
                    break;
                }
                offset += p_ecb->aid_len[xx];
            }

            if (p_ret)
            {
                /* found the entry already */
                break;
            }
        }
        p_ecb = &nfa_ee_cb.ecb[yy];
    }

    return p_ret;
}

/*******************************************************************************
**
** Function         nfa_ee_report_event
**
** Description      report the given event to the callback
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_report_event(tNFA_EE_CBACK *p_cback, tNFA_EE_EVT event, tNFA_EE_CBACK_DATA *p_data)
{
    int xx;

    /* use the given callback, if not NULL */
    if (p_cback)
    {
        (*p_cback)(event, p_data);
        return;
    }
    /* if the given is NULL, report to all registered ones */
    for (xx = 0; xx < NFA_EE_MAX_CBACKS; xx++)
    {
        if (nfa_ee_cb.p_ee_cback[xx] != NULL)
        {
            (*nfa_ee_cb.p_ee_cback[xx])(event, p_data);
        }
    }
}
/*******************************************************************************
**
** Function         nfa_ee_start_timer
**
** Description      start the de-bounce timer
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_start_timer(void)
{
    nfa_sys_start_timer(&nfa_ee_cb.timer, NFA_EE_ROUT_TIMEOUT_EVT, NFA_EE_ROUT_TIMEOUT_VAL);
}

/*******************************************************************************
**
** Function         nfa_ee_api_discover
**
** Description      process discover command from user
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_api_discover(tNFA_EE_MSG *p_data)
{
    tNFA_EE_CBACK *p_cback = p_data->ee_discover.p_cback;
    tNFA_EE_CBACK_DATA  evt_data = {0};

    NFA_TRACE_DEBUG1 ("nfa_ee_api_discover() in_use:%d", nfa_ee_cb.discv_timer.in_use);
    if (nfa_ee_cb.discv_timer.in_use)
    {
        nfa_sys_stop_timer(&nfa_ee_cb.discv_timer);
        NFC_NfceeDiscover(FALSE);
    }
    if (nfa_ee_cb.p_ee_disc_cback == NULL && NFC_NfceeDiscover(TRUE) == NFC_STATUS_OK)
    {
        nfa_ee_cb.p_ee_disc_cback   = p_cback;
    }
    else
    {
        evt_data.status = NFA_STATUS_FAILED;
        nfa_ee_report_event (p_cback, NFA_EE_DISCOVER_EVT, &evt_data);
    }
}

/*******************************************************************************
**
** Function         nfa_ee_api_register
**
** Description      process register command from user
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_api_register(tNFA_EE_MSG *p_data)
{
    int xx;
    tNFA_EE_CBACK *p_cback = p_data->ee_register.p_cback;
    tNFA_EE_CBACK_DATA  evt_data = {0};
    BOOLEAN found = FALSE;

    evt_data.ee_register = NFA_STATUS_FAILED;
    /* loop through all entries to see if there's a matching callback */
    for (xx = 0; xx < NFA_EE_MAX_CBACKS; xx++)
    {
        if (nfa_ee_cb.p_ee_cback[xx] == p_cback)
        {
            evt_data.ee_register        = NFA_STATUS_OK;
            found                       = TRUE;
            break;
        }
    }

    /* If no matching callback, allocated an entry */
    if (!found)
    {
        for (xx = 0; xx < NFA_EE_MAX_CBACKS; xx++)
        {
            if (nfa_ee_cb.p_ee_cback[xx] == NULL)
            {
                nfa_ee_cb.p_ee_cback[xx]    = p_cback;
                evt_data.ee_register        = NFA_STATUS_OK;
                break;
            }
        }
    }
    /* This callback is verified (not NULL) in NFA_EeRegister() */
    (*p_cback)(NFA_EE_REGISTER_EVT, &evt_data);

    /* report NFCEE Discovery Request collected during booting up */
    nfa_ee_build_discover_req_evt (&evt_data.discover_req);
    (*p_cback)(NFA_EE_DISCOVER_REQ_EVT, &evt_data);
}

/*******************************************************************************
**
** Function         nfa_ee_api_deregister
**
** Description      process de-register command from user
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_api_deregister(tNFA_EE_MSG *p_data)
{
    tNFA_EE_CBACK *p_cback = NULL;
    int                 index  = p_data->deregister.index;
    tNFA_EE_CBACK_DATA  evt_data = {0};

    NFA_TRACE_DEBUG0 ("nfa_ee_api_deregister");
    p_cback = nfa_ee_cb.p_ee_cback[index];
    nfa_ee_cb.p_ee_cback[index] = NULL;
    if (p_cback)
        (*p_cback)(NFA_EE_DEREGISTER_EVT, &evt_data);
}


/*******************************************************************************
**
** Function         nfa_ee_api_mode_set
**
** Description      process mode set command from user
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_api_mode_set(tNFA_EE_MSG *p_data)
{
    tNFA_EE_ECB *p_cb= p_data->cfg_hdr.p_cb;

    NFA_TRACE_DEBUG2 ("nfa_ee_api_mode_set() handle:0x%02x mode:%d", p_cb->nfcee_id, p_data->mode_set.mode);
    NFC_NfceeModeSet (p_cb->nfcee_id, p_data->mode_set.mode);
    /* set the NFA_EE_STATUS_PENDING bit to indicate the status is not exactly active */
    if (p_data->mode_set.mode == NFC_MODE_ACTIVATE)
        p_cb->ee_status = NFA_EE_STATUS_PENDING | NFA_EE_STATUS_ACTIVE;
    else
    {
        p_cb->ee_status = NFA_EE_STATUS_INACTIVE;
        /* DH should release the NCI connection before deactivate the NFCEE */
        if (p_cb->conn_st == NFA_EE_CONN_ST_CONN)
        {
            p_cb->conn_st = NFA_EE_CONN_ST_DISC;
            NFC_ConnClose(p_cb->conn_id);
        }
    }
    /* report the NFA_EE_MODE_SET_EVT status on the response from NFCC */
}



/*******************************************************************************
**
** Function         nfa_ee_api_set_tech_cfg
**
** Description      process set technology routing configuration from user
**                  start a 1 second timer. When the timer expires,
**                  the configuration collected in control block is sent to NFCC
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_api_set_tech_cfg(tNFA_EE_MSG *p_data)
{
    tNFA_EE_ECB *p_cb = p_data->cfg_hdr.p_cb;
    tNFA_EE_CBACK_DATA  evt_data = {0};

    p_cb->tech_switch_on   = p_data->set_tech.technologies_switch_on;
    p_cb->tech_switch_off  = p_data->set_tech.technologies_switch_off;
    p_cb->tech_battery_off = p_data->set_tech.technologies_battery_off;
    p_cb->ecb_flags       |= NFA_EE_ECB_FLAGS_TECH;
    if (p_cb->tech_switch_on | p_cb->tech_switch_off | p_cb->tech_battery_off)
    {
        /* if any technology in any power mode is configured, mark this entry as configured */
        nfa_ee_cb.ee_cfged    |= nfa_ee_ecb_to_mask(p_cb);
    }
    nfa_ee_start_timer();
    nfa_ee_report_event (p_cb->p_ee_cback, NFA_EE_SET_TECH_CFG_EVT, &evt_data);
}

/*******************************************************************************
**
** Function         nfa_ee_api_set_proto_cfg
**
** Description      process set protocol routing configuration from user
**                  start a 1 second timer. When the timer expires,
**                  the configuration collected in control block is sent to NFCC
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_api_set_proto_cfg(tNFA_EE_MSG *p_data)
{
    tNFA_EE_ECB *p_cb = p_data->cfg_hdr.p_cb;
    tNFA_EE_CBACK_DATA  evt_data = {0};

    p_cb->proto_switch_on       = p_data->set_proto.protocols_switch_on;
    p_cb->proto_switch_off      = p_data->set_proto.protocols_switch_off;
    p_cb->proto_battery_off     = p_data->set_proto.protocols_battery_off;
    p_cb->ecb_flags            |= NFA_EE_ECB_FLAGS_PROTO;
    if (p_cb->proto_switch_on | p_cb->proto_switch_off | p_cb->proto_battery_off)
    {
        /* if any protocol in any power mode is configured, mark this entry as configured */
        nfa_ee_cb.ee_cfged         |= nfa_ee_ecb_to_mask(p_cb);
    }
    nfa_ee_start_timer();
    nfa_ee_report_event (p_cb->p_ee_cback, NFA_EE_SET_PROTO_CFG_EVT, &evt_data);
}

/*******************************************************************************
**
** Function         nfa_ee_api_add_aid
**
** Description      process add an AID routing configuration from user
**                  start a 1 second timer. When the timer expires,
**                  the configuration collected in control block is sent to NFCC
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_api_add_aid(tNFA_EE_MSG *p_data)
{
    tNFA_EE_API_ADD_AID *p_add = &p_data->add_aid;
    tNFA_EE_ECB *p_cb = p_data->cfg_hdr.p_cb;
    tNFA_EE_ECB *p_chk_cb;
    UINT8   *p, *p_start;
    int     len, len_needed;
    tNFA_EE_CBACK_DATA  evt_data = {0};
    int offset = 0, entry = 0;

    NFA_TRACE_DEBUG3 ("nfa_ee_api_add_aid nfcee_id:0x%x %02x%02x",
        p_cb->nfcee_id, p_add->p_aid[0], p_add->p_aid[1]);
    p_chk_cb = nfa_ee_find_aid_offset(p_add->aid_len, p_add->p_aid, &offset, &entry);
    if (p_chk_cb)
    {
        NFA_TRACE_DEBUG0 ("nfa_ee_api_add_aid The AID entry is already in the database");
        if (p_chk_cb == p_cb)
        {
            p_cb->aid_rt_info[entry]    |= NFA_EE_AE_ROUTE;
            p_cb->aid_pwr_cfg[entry]     = p_add->power_state;
        }
        else
        {
            NFA_TRACE_ERROR1 ("The AID entry is already in the database for different NFCEE ID:0x%02x", p_chk_cb->nfcee_id);
            evt_data.status = NFA_STATUS_SEMANTIC_ERROR;
        }
    }
    else
    {
        /* Find the total length so far */
        len = nfa_ee_find_total_aid_len(p_cb, 0);

        /* make sure the control block has enough room to hold this entry */
        len_needed  = p_add->aid_len + 2; /* tag/len */

        if ((len_needed + len) > NFA_EE_MAX_AID_CFG_LEN)
        {
            evt_data.status = NFA_STATUS_BUFFER_FULL;
        }
        else
        {
            /* add AID */
            p_cb->aid_pwr_cfg[p_cb->aid_entries]    = p_add->power_state;
            p_cb->aid_rt_info[p_cb->aid_entries]    = NFA_EE_AE_ROUTE;
            p       = p_cb->aid_cfg + len;
            p_start = p;
            *p++    = NFA_EE_AID_CFG_TAG_NAME;
            *p++    = p_add->aid_len;
            memcpy(p, p_add->p_aid, p_add->aid_len);
            p      += p_add->aid_len;

            p_cb->aid_len[p_cb->aid_entries++]     = (UINT8)(p - p_start);
        }
    }

    if (evt_data.status == NFA_STATUS_OK)
    {
        /* mark AID changed */
        p_cb->ecb_flags                       |= NFA_EE_ECB_FLAGS_AID;
        nfa_ee_cb.ee_cfged                    |= nfa_ee_ecb_to_mask(p_cb);
    }
    NFA_TRACE_DEBUG2 ("status:%d ee_cfged:0x%02x ",evt_data.status, nfa_ee_cb.ee_cfged);
    /* report the status of this operation */
    nfa_ee_report_event (p_cb->p_ee_cback, NFA_EE_ADD_AID_EVT, &evt_data);
}

/*******************************************************************************
**
** Function         nfa_ee_api_remove_aid
**
** Description      process remove an AID routing configuration from user
**                  start a 1 second timer. When the timer expires,
**                  the configuration collected in control block is sent to NFCC
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_api_remove_aid(tNFA_EE_MSG *p_data)
{
    tNFA_EE_ECB  *p_cb;
    tNFA_EE_CBACK_DATA  evt_data = {0};
    int offset = 0, entry = 0, len;
    int rest_len;
    tNFA_EE_CBACK *p_cback = NULL;

    NFA_TRACE_DEBUG0 ("nfa_ee_api_remove_aid");
    p_cb = nfa_ee_find_aid_offset(p_data->rm_aid.aid_len, p_data->rm_aid.p_aid, &offset, &entry);
    if (p_cb && p_cb->aid_entries)
    {
        NFA_TRACE_DEBUG2 ("aid_rt_info[%d]: 0x%02x", entry, p_cb->aid_rt_info[entry]);
        /* mark routing and VS changed */
        if (p_cb->aid_rt_info[entry] & NFA_EE_AE_ROUTE)
            p_cb->ecb_flags         |= NFA_EE_ECB_FLAGS_AID;

        if (p_cb->aid_rt_info[entry] & NFA_EE_AE_VS)
            p_cb->ecb_flags         |= NFA_EE_ECB_FLAGS_VS;

        /* remove the aid */
        if ((entry+1) < p_cb->aid_entries)
        {
            /* not the last entry, move the aid entries in control block */
            /* Find the total len from the next entry to the last one */
            rest_len = nfa_ee_find_total_aid_len(p_cb, entry + 1);

            len = p_cb->aid_len[entry];
            NFA_TRACE_DEBUG2 ("nfa_ee_api_remove_aid len:%d, rest_len:%d", len, rest_len);
            GKI_shiftup (&p_cb->aid_cfg[offset], &p_cb->aid_cfg[offset+ len], rest_len);
            rest_len = p_cb->aid_entries - entry;
            GKI_shiftup (&p_cb->aid_len[entry], &p_cb->aid_len[entry + 1], rest_len);
            GKI_shiftup (&p_cb->aid_pwr_cfg[entry], &p_cb->aid_pwr_cfg[entry + 1], rest_len);
            GKI_shiftup (&p_cb->aid_rt_info[entry], &p_cb->aid_rt_info[entry + 1], rest_len);
        }
        /* else the last entry, just reduce the aid_entries by 1 */
        p_cb->aid_entries--;
        nfa_ee_cb.ee_cfged      |= nfa_ee_ecb_to_mask(p_cb);
        /* report NFA_EE_REMOVE_AID_EVT to the callback associated the NFCEE */
        p_cback = p_cb->p_ee_cback;
    }
    else
    {
        NFA_TRACE_ERROR0 ("nfa_ee_api_remove_aid The AID entry is not in the database");
        evt_data.status = NFA_STATUS_INVALID_PARAM;
    }
    nfa_ee_report_event (p_cback, NFA_EE_REMOVE_AID_EVT, &evt_data);
}

/*******************************************************************************
**
** Function         nfa_ee_api_update_now
**
** Description      Initiates connection creation process to the given NFCEE
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_api_update_now(tNFA_EE_MSG *p_data)
{
    nfa_sys_stop_timer(&nfa_ee_cb.timer);
    nfa_ee_cb.ee_cfged  |= NFA_EE_CFGED_UPDATE_NOW;
    nfa_ee_rout_timeout(p_data);
}

/*******************************************************************************
**
** Function         nfa_ee_api_connect
**
** Description      Initiates connection creation process to the given NFCEE
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_api_connect(tNFA_EE_MSG *p_data)
{
    tNFA_EE_ECB  *p_cb = p_data->connect.p_cb;
    int xx;
    tNFA_EE_CBACK_DATA  evt_data = {0};

    evt_data.connect.status       = NFA_STATUS_FAILED;
    if (p_cb->conn_st == NFA_EE_CONN_ST_NONE)
    {
        for (xx = 0; xx < p_cb->num_interface; xx++)
        {
            if (p_data->connect.ee_interface == p_cb->ee_interface[xx])
            {
                p_cb->p_ee_cback        = p_data->connect.p_cback;
                p_cb->conn_st           = NFA_EE_CONN_ST_WAIT;
                p_cb->use_interface     = p_data->connect.ee_interface;
                evt_data.connect.status = NFC_ConnCreate(NCI_DEST_TYPE_NFCEE, p_data->connect.nfcee_id,
                    p_data->connect.ee_interface, nfa_ee_conn_cback);
                /* report the NFA_EE_CONNECT_EVT status on the response from NFCC */
                break;
            }
        }
    }

    if (evt_data.connect.status != NCI_STATUS_OK)
    {
        evt_data.connect.ee_handle    = (tNFA_HANDLE)p_data->connect.nfcee_id | NFA_HANDLE_GROUP_EE;
        evt_data.connect.status       = NFA_STATUS_INVALID_PARAM;
        evt_data.connect.ee_interface = p_data->connect.ee_interface;
        nfa_ee_report_event (p_data->connect.p_cback, NFA_EE_CONNECT_EVT, &evt_data);
    }
}

/*******************************************************************************
**
** Function         nfa_ee_api_send_data
**
** Description      Send the given data packet to the given NFCEE
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_api_send_data(tNFA_EE_MSG *p_data)
{
    tNFA_EE_ECB  *p_cb = p_data->send_data.p_cb;
    BT_HDR *p_pkt;
    UINT16 size = NCI_MSG_OFFSET_SIZE + NCI_DATA_HDR_SIZE + p_data->send_data.data_len + BT_HDR_SIZE;
    UINT8  *p;
    tNFA_STATUS status = NFA_STATUS_FAILED;

    if (p_cb->conn_st == NFA_EE_CONN_ST_CONN)
    {
        p_pkt = (BT_HDR *)GKI_getbuf(size);
        if (p_pkt)
        {
            p_pkt->offset   = NCI_MSG_OFFSET_SIZE + NCI_DATA_HDR_SIZE;
            p_pkt->len      = p_data->send_data.data_len;
            p               = (UINT8 *)(p_pkt+1) + p_pkt->offset;
            memcpy(p, p_data->send_data.p_data, p_pkt->len);
            NFC_SendData (p_cb->conn_id, p_pkt);
        }
        else
        {
            nfa_ee_report_event( p_cb->p_ee_cback, NFA_EE_NO_MEM_ERR_EVT, (tNFA_EE_CBACK_DATA *)&status);
        }
    }
    else
    {
        nfa_ee_report_event( p_cb->p_ee_cback, NFA_EE_NO_CB_ERR_EVT, (tNFA_EE_CBACK_DATA *)&status);
    }
}

/*******************************************************************************
**
** Function         nfa_ee_api_disconnect
**
** Description      Initiates closing of the connection to the given NFCEE
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_api_disconnect(tNFA_EE_MSG *p_data)
{
    tNFA_EE_ECB  *p_cb = p_data->disconnect.p_cb;
    tNFA_EE_CBACK_DATA  evt_data = {0};

    if (p_cb->conn_st == NFA_EE_CONN_ST_CONN)
    {
        p_cb->conn_st = NFA_EE_CONN_ST_DISC;
        NFC_ConnClose(p_cb->conn_id);
    }
    evt_data.handle = (tNFA_HANDLE)p_cb->nfcee_id | NFA_HANDLE_GROUP_EE;
    nfa_ee_report_event(p_cb->p_ee_cback, NFA_EE_DISCONNECT_EVT, &evt_data);
}

/*******************************************************************************
**
** Function         nfa_ee_report_disc_done
**
** Description      Process the callback for NFCEE discovery response
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_report_disc_done(BOOLEAN notify_enable_done)
{
    tNFA_EE_CBACK           *p_cback;
    tNFA_EE_CBACK_DATA      evt_data = {0};

    NFA_TRACE_DEBUG3("nfa_ee_report_disc_done() em_state:%d num_ee_expecting:%d notify_enable_done:%d", nfa_ee_cb.em_state, nfa_ee_cb.num_ee_expecting, notify_enable_done);
    if (nfa_ee_cb.num_ee_expecting == 0)
    {
        if (notify_enable_done)
        {
            if (nfa_ee_cb.em_state == NFA_EE_EM_STATE_INIT_DONE)
            {
                nfa_sys_cback_notify_enable_complete (NFA_ID_EE);
                if (nfa_ee_cb.p_enable_cback)
                    (*nfa_ee_cb.p_enable_cback)(NFA_EE_DISC_STS_ON);
            }
            else if ((nfa_ee_cb.em_state == NFA_EE_EM_STATE_RESTORING) && (nfa_ee_cb.ee_flags & NFA_EE_FLAG_NOTIFY_HCI) )
            {
                nfa_ee_cb.ee_flags   &= ~NFA_EE_FLAG_NOTIFY_HCI;
                if (nfa_ee_cb.p_enable_cback)
                    (*nfa_ee_cb.p_enable_cback)(NFA_EE_DISC_STS_ON);
            }
        }


        if (nfa_ee_cb.p_ee_disc_cback)
        {
            /* notify API callback */
            p_cback                         = nfa_ee_cb.p_ee_disc_cback;
            nfa_ee_cb.p_ee_disc_cback       = NULL;
            evt_data.status                         = NFA_STATUS_OK;
            evt_data.ee_discover.num_ee             = NFA_EE_MAX_EE_SUPPORTED;
            NFA_EeGetInfo(&evt_data.ee_discover.num_ee, evt_data.ee_discover.ee_info);
            nfa_ee_report_event (p_cback, NFA_EE_DISCOVER_EVT, &evt_data);
        }
    }
}

/*******************************************************************************
**
** Function         nfa_ee_restore_ntf_done
**
** Description      check if any ee_status still has NFA_EE_STATUS_PENDING bit
**
** Returns          TRUE, if all NFA_EE_STATUS_PENDING bits are removed
**
*******************************************************************************/
BOOLEAN nfa_ee_restore_ntf_done(void)
{
    tNFA_EE_ECB     *p_cb;
    BOOLEAN         is_done = TRUE;
    int             xx;

    p_cb = nfa_ee_cb.ecb;
    for (xx = 0; xx < nfa_ee_cb.cur_ee; xx++, p_cb++)
    {
        if ((p_cb->nfcee_id != NFA_EE_INVALID) && (p_cb->ee_old_status & NFA_EE_STATUS_RESTORING))
        {
            is_done = FALSE;
            break;
        }
    }
    return is_done;
}

/*******************************************************************************
**
** Function         nfa_ee_remove_pending
**
** Description      check if any ee_status still has NFA_EE_STATUS_RESTORING bit
**
** Returns          TRUE, if all NFA_EE_STATUS_RESTORING bits are removed
**
*******************************************************************************/
static void nfa_ee_remove_pending(void)
{
    tNFA_EE_ECB     *p_cb;
    tNFA_EE_ECB     *p_cb_n, *p_cb_end;
    int             xx, num_removed = 0;
    int             first_removed = NFA_EE_MAX_EE_SUPPORTED;

    p_cb = nfa_ee_cb.ecb;
    for (xx = 0; xx < nfa_ee_cb.cur_ee; xx++, p_cb++)
    {
        if ((p_cb->nfcee_id != NFA_EE_INVALID) && (p_cb->ee_status & NFA_EE_STATUS_RESTORING))
        {
            p_cb->nfcee_id  = NFA_EE_INVALID;
            num_removed ++;
            if (first_removed == NFA_EE_MAX_EE_SUPPORTED)
                first_removed   = xx;
        }
    }

    NFA_TRACE_DEBUG3("nfa_ee_remove_pending() cur_ee:%d, num_removed:%d first_removed:%d", nfa_ee_cb.cur_ee, num_removed, first_removed);
    if (num_removed && (first_removed != (nfa_ee_cb.cur_ee - num_removed)))
    {
        /* if the removes ECB entried are not at the end, move the entries up */
        p_cb_end = &nfa_ee_cb.ecb[nfa_ee_cb.cur_ee - 1];
        p_cb = &nfa_ee_cb.ecb[first_removed];
        for (p_cb_n = p_cb + 1; p_cb_n <= p_cb_end;)
        {
            while ((p_cb_n->nfcee_id == NFA_EE_INVALID) && (p_cb_n <= p_cb_end))
            {
                p_cb_n++;
            }

            if (p_cb_n <= p_cb_end)
            {
                memcpy(p_cb, p_cb_n, sizeof(tNFA_EE_ECB));
                p_cb_n->nfcee_id = NFA_EE_INVALID;
            }
            p_cb++;
            p_cb_n++;
        }
    }
    nfa_ee_cb.cur_ee -= (UINT8)num_removed;
}


/*******************************************************************************
**
** Function         nfa_ee_nci_disc_rsp
**
** Description      Process the callback for NFCEE discovery response
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_nci_disc_rsp(tNFA_EE_MSG *p_data)
{
    tNFC_NFCEE_DISCOVER_REVT    *p_evt = p_data->disc_rsp.p_data;
    tNFA_EE_ECB              *p_cb;
    UINT8   xx;
    UINT8   num_nfcee = p_evt->num_nfcee;
    BOOLEAN notify_enable_done = FALSE;

    NFA_TRACE_DEBUG3("nfa_ee_nci_disc_rsp() em_state:%d cur_ee:%d, num_nfcee:%d", nfa_ee_cb.em_state, nfa_ee_cb.cur_ee, num_nfcee);
    switch (nfa_ee_cb.em_state)
    {
    case NFA_EE_EM_STATE_INIT:
        nfa_ee_cb.cur_ee            = 0;
        nfa_ee_cb.num_ee_expecting  = 0;
        if (num_nfcee == 0)
        {
            nfa_ee_cb.em_state = NFA_EE_EM_STATE_INIT_DONE;
            notify_enable_done = TRUE;
            if (p_evt->status != NFC_STATUS_OK)
            {
                nfa_sys_stop_timer(&nfa_ee_cb.discv_timer);
            }
        }
        break;

    case NFA_EE_EM_STATE_INIT_DONE:
        if (num_nfcee)
        {
            /* if this is initiated by api function,
             * check if the number of NFCEE expected is more than what's currently in CB */
            if (num_nfcee > NFA_EE_MAX_EE_SUPPORTED)
                num_nfcee = NFA_EE_MAX_EE_SUPPORTED;
            if (nfa_ee_cb.cur_ee < num_nfcee)
            {
                p_cb = &nfa_ee_cb.ecb[nfa_ee_cb.cur_ee];
                for (xx = nfa_ee_cb.cur_ee; xx < num_nfcee; xx++, p_cb++)
                {
                    /* mark the new entries as a new one */
                    p_cb->nfcee_id = NFA_EE_INVALID;
                }
            }
            nfa_ee_cb.cur_ee = num_nfcee;
        }
        break;

    case NFA_EE_EM_STATE_RESTORING:
        if (num_nfcee == 0)
        {
            nfa_ee_cb.em_state = NFA_EE_EM_STATE_INIT_DONE;
            nfa_ee_remove_pending();
            nfa_ee_check_restore_complete();
            if (p_evt->status != NFC_STATUS_OK)
            {
                nfa_sys_stop_timer(&nfa_ee_cb.discv_timer);
            }
        }
        break;
    }

    if (p_evt->status == NFC_STATUS_OK)
    {
        nfa_ee_cb.num_ee_expecting = p_evt->num_nfcee;
        if (nfa_ee_cb.num_ee_expecting > NFA_EE_MAX_EE_SUPPORTED)
        {
            NFA_TRACE_ERROR2 ("NFA-EE num_ee_expecting:%d > max:%d", nfa_ee_cb.num_ee_expecting, NFA_EE_MAX_EE_SUPPORTED);
        }
    }
    nfa_ee_report_disc_done(notify_enable_done);
    NFA_TRACE_DEBUG3("nfa_ee_nci_disc_rsp() em_state:%d cur_ee:%d num_ee_expecting:%d", nfa_ee_cb.em_state, nfa_ee_cb.cur_ee, nfa_ee_cb.num_ee_expecting);
}

/*******************************************************************************
**
** Function         nfa_ee_nci_disc_ntf
**
** Description      Process the callback for NFCEE discovery notification
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_nci_disc_ntf(tNFA_EE_MSG *p_data)
{
    tNFC_NFCEE_INFO_REVT    *p_ee = p_data->disc_ntf.p_data;
    tNFA_EE_ECB             *p_cb = NULL;
    BOOLEAN                 notify_enable_done = FALSE;
    BOOLEAN                 notify_new_ee = FALSE;
    tNFA_EE_CBACK_DATA      evt_data = {0};
    tNFA_EE_INFO            *p_info;
    tNFA_EE_EM_STATE        new_em_state = NFA_EE_EM_STATE_MAX;

    NFA_TRACE_DEBUG4("nfa_ee_nci_disc_ntf() em_state:%d ee_flags:0x%x cur_ee:%d num_ee_expecting:%d", nfa_ee_cb.em_state, nfa_ee_cb.ee_flags, nfa_ee_cb.cur_ee, nfa_ee_cb.num_ee_expecting);
    if (nfa_ee_cb.num_ee_expecting)
    {
        nfa_ee_cb.num_ee_expecting--;
        if ((nfa_ee_cb.num_ee_expecting == 0) && (nfa_ee_cb.p_ee_disc_cback != NULL))
        {
            /* Discovery triggered by API function */
            NFC_NfceeDiscover(FALSE);
        }
    }
    switch (nfa_ee_cb.em_state)
    {
    case NFA_EE_EM_STATE_INIT:
        if (nfa_ee_cb.cur_ee < NFA_EE_MAX_EE_SUPPORTED)
        {
            /* the cb can collect up to NFA_EE_MAX_EE_SUPPORTED ee_info */
            p_cb = &nfa_ee_cb.ecb[nfa_ee_cb.cur_ee++];
        }

        if (nfa_ee_cb.num_ee_expecting == 0)
        {
            /* notify init_done callback */
            nfa_ee_cb.em_state = NFA_EE_EM_STATE_INIT_DONE;
            notify_enable_done = TRUE;
        }
        break;

    case NFA_EE_EM_STATE_INIT_DONE:
        p_cb = nfa_ee_find_ecb (p_ee->nfcee_id);
        if (p_cb == NULL)
        {
            /* the NFCEE ID is not in the last NFCEE discovery
             * maybe it's a new one */
            p_cb = nfa_ee_find_ecb (NFA_EE_INVALID);
            if (p_cb)
            {
                nfa_ee_cb.cur_ee++;
                notify_new_ee = TRUE;
            }
        }
        else if (p_cb->ecb_flags & NFA_EE_ECB_FLAGS_ORDER)
        {
            nfa_ee_cb.cur_ee++;
            notify_new_ee = TRUE;
        }
        else
        {
            NFA_TRACE_DEBUG3 ("cur_ee:%d ecb_flags=0x%02x  ee_status=0x%x", nfa_ee_cb.cur_ee, p_cb->ecb_flags, p_cb->ee_status);
        }
        break;

    case NFA_EE_EM_STATE_RESTORING:
        p_cb = nfa_ee_find_ecb (p_ee->nfcee_id);
        if (p_cb == NULL)
        {
            /* the NFCEE ID is not in the last NFCEE discovery
             * maybe it's a new one */
            p_cb = nfa_ee_find_ecb (NFA_EE_INVALID);
            if (p_cb)
            {
                nfa_ee_cb.cur_ee++;
                notify_new_ee = TRUE;
            }
        }
        if (nfa_ee_cb.num_ee_expecting == 0)
        {
            /* notify init_done callback */
            notify_enable_done = TRUE;
            if (nfa_ee_restore_ntf_done())
            {
                new_em_state       = NFA_EE_EM_STATE_INIT_DONE;
            }
        }
        break;
    }
    NFA_TRACE_DEBUG1 ("nfa_ee_nci_disc_ntf cur_ee:%d", nfa_ee_cb.cur_ee);

    if (p_cb)
    {
        p_cb->nfcee_id      = p_ee->nfcee_id;
        p_cb->ee_status     = p_ee->ee_status;
        p_cb->num_interface = p_ee->num_interface;
        memcpy(p_cb->ee_interface, p_ee->ee_interface, p_ee->num_interface);
        p_cb->num_tlvs      = p_ee->num_tlvs;
        memcpy(p_cb->ee_tlv, p_ee->ee_tlv, p_ee->num_tlvs * sizeof(tNFA_EE_TLV));

        if (nfa_ee_cb.em_state == NFA_EE_EM_STATE_RESTORING)
        {
            /* NCI spec says: An NFCEE_DISCOVER_NTF that contains a Protocol type of "HCI Access"
             * SHALL NOT contain any other additional Protocol
             * i.e. check only first supported NFCEE interface is HCI access */
            /* NFA_HCI module handles restoring configurations for HCI access */
            if (p_cb->ee_interface[0] != NFC_NFCEE_INTERFACE_HCI_ACCESS)
            {
                if ((nfa_ee_cb.ee_flags & NFA_EE_FLAG_WAIT_HCI) == 0)
                {
                    nfa_ee_restore_one_ecb (p_cb);
                }
                /* else wait for NFA-HCI module to restore the HCI network information before enabling the NFCEE */
            }
        }

        if ((nfa_ee_cb.p_ee_disc_cback == NULL) && (notify_new_ee == TRUE))
        {
            if (nfa_dm_is_active() && (p_cb->ee_status != NFA_EE_STATUS_REMOVED))
            {
                /* report this NFA_EE_NEW_EE_EVT only after NFA_DM_ENABLE_EVT is reported */
                p_info                  = &evt_data.new_ee;
                p_info->ee_handle       = NFA_HANDLE_GROUP_EE | (tNFA_HANDLE)p_cb->nfcee_id;
                p_info->ee_status       = p_cb->ee_status;
                p_info->num_interface   = p_cb->num_interface;
                p_info->num_tlvs        = p_cb->num_tlvs;
                memcpy(p_info->ee_interface, p_cb->ee_interface, p_cb->num_interface);
                memcpy(p_info->ee_tlv, p_cb->ee_tlv, p_cb->num_tlvs * sizeof(tNFA_EE_TLV));
                nfa_ee_report_event (NULL, NFA_EE_NEW_EE_EVT, &evt_data);
            }
        }
        else
            nfa_ee_report_disc_done(notify_enable_done);

        if (p_cb->ecb_flags & NFA_EE_ECB_FLAGS_ORDER)
        {
            NFA_TRACE_DEBUG0 ("NFA_EE_ECB_FLAGS_ORDER");
            p_cb->ecb_flags &= ~NFA_EE_ECB_FLAGS_ORDER;
            nfa_ee_report_discover_req_evt();
        }

    }

    if (new_em_state != NFA_EE_EM_STATE_MAX)
    {
        nfa_ee_cb.em_state = new_em_state;
        nfa_ee_check_restore_complete();
    }

    if ((nfa_ee_cb.cur_ee == nfa_ee_max_ee_cfg) && (nfa_ee_cb.em_state == NFA_EE_EM_STATE_INIT_DONE) )
    {
        if (nfa_ee_cb.discv_timer.in_use)
        {
            nfa_sys_stop_timer (&nfa_ee_cb.discv_timer);
            p_data->hdr.event = NFA_EE_DISCV_TIMEOUT_EVT;
            nfa_ee_evt_hdlr((BT_HDR *)p_data);
        }
    }
}

/*******************************************************************************
**
** Function         nfa_ee_check_restore_complete
**
** Description      Check if restore the NFA-EE related configuration to the
**                  state prior to low power mode is complete.
**                  If complete, notify sys.
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_check_restore_complete(void)
{
    UINT32  xx;
    tNFA_EE_ECB     *p_cb;
    BOOLEAN         proc_complete = TRUE;

    p_cb = nfa_ee_cb.ecb;
    for (xx = 0; xx < nfa_ee_cb.cur_ee; xx++, p_cb++)
    {
        if (p_cb->ecb_flags & NFA_EE_ECB_FLAGS_RESTORE)
        {
            /* NFA_HCI module handles restoring configurations for HCI access.
             * ignore the restoring status for HCI Access */
            if (p_cb->ee_interface[0] != NFC_NFCEE_INTERFACE_HCI_ACCESS)
            {
                proc_complete = FALSE;
                break;
            }
        }
    }

    NFA_TRACE_DEBUG2 ("nfa_ee_check_restore_complete nfa_ee_cb.ee_cfg_sts:0x%02x proc_complete:%d", nfa_ee_cb.ee_cfg_sts, proc_complete);
    if (proc_complete)
    {
        /* update routing table when NFA_EE_ROUT_TIMEOUT_EVT is received */
        if (nfa_ee_cb.ee_cfg_sts & NFA_EE_STS_PREV_ROUTING)
            nfa_ee_api_update_now(NULL);

        nfa_ee_cb.em_state = NFA_EE_EM_STATE_INIT_DONE;
        nfa_sys_cback_notify_nfcc_power_mode_proc_complete (NFA_ID_EE);
    }
}

/*******************************************************************************
**
** Function         nfa_ee_build_discover_req_evt
**
** Description      Build NFA_EE_DISCOVER_REQ_EVT for all active NFCEE
**
** Returns          void
**
*******************************************************************************/
static void nfa_ee_build_discover_req_evt (tNFA_EE_DISCOVER_REQ *p_evt_data)
{
    tNFA_EE_ECB           *p_cb;
    tNFA_EE_DISCOVER_INFO *p_info;
    UINT8                 xx;

    if (!p_evt_data)
        return;

    p_evt_data->num_ee = 0;
    p_cb               = nfa_ee_cb.ecb;
    p_info             = p_evt_data->ee_disc_info;

    for (xx = 0; xx < nfa_ee_cb.cur_ee; xx++, p_cb++)
    {
        if (  (p_cb->ee_status & NFA_EE_STATUS_INT_MASK)
            ||(p_cb->ee_status != NFA_EE_STATUS_ACTIVE)
            ||((p_cb->ecb_flags & NFA_EE_ECB_FLAGS_DISC_REQ) == 0)  )
        {
            continue;
        }
        p_info->ee_handle       = (tNFA_HANDLE)p_cb->nfcee_id | NFA_HANDLE_GROUP_EE;
        p_info->la_protocol     = p_cb->la_protocol;
        p_info->lb_protocol     = p_cb->lb_protocol;
        p_info->lf_protocol     = p_cb->lf_protocol;
        p_info->lbp_protocol    = p_cb->lbp_protocol;
        p_evt_data->num_ee++;
        p_info++;

        NFA_TRACE_DEBUG6 ("[%d] ee_handle:0x%x, listen protocol A:%d, B:%d, F:%d, BP:%d",
                          p_evt_data->num_ee, p_cb->nfcee_id,
                          p_cb->la_protocol, p_cb->lb_protocol, p_cb->lf_protocol, p_cb->lbp_protocol);
    }

    p_evt_data->status     = NFA_STATUS_OK;
}

/*******************************************************************************
**
** Function         nfa_ee_report_discover_req_evt
**
** Description      Report NFA_EE_DISCOVER_REQ_EVT for all active NFCEE
**
** Returns          void
**
*******************************************************************************/
static void nfa_ee_report_discover_req_evt(void)
{
    tNFA_EE_DISCOVER_REQ    evt_data;

    if (nfa_ee_cb.p_enable_cback)
        (*nfa_ee_cb.p_enable_cback) (NFA_EE_DISC_STS_REQ);


    /* if this is restoring NFCC */
    if (!nfa_dm_is_active ())
    {
        NFA_TRACE_DEBUG0 ("nfa_ee_report_discover_req_evt DM is not active");
        return;
    }

    nfa_ee_build_discover_req_evt (&evt_data);
    nfa_ee_report_event(NULL, NFA_EE_DISCOVER_REQ_EVT, (tNFA_EE_CBACK_DATA *)&evt_data);
}

/*******************************************************************************
**
** Function         nfa_ee_nci_mode_set_rsp
**
** Description      Process the result for NFCEE ModeSet response
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_nci_mode_set_rsp(tNFA_EE_MSG *p_data)
{
    tNFA_EE_ECB *p_cb;
    tNFA_EE_MODE_SET    mode_set;
    tNFC_NFCEE_MODE_SET_REVT    *p_rsp = p_data->mode_set_rsp.p_data;

    NFA_TRACE_DEBUG2 ("nfa_ee_nci_mode_set_rsp() handle:0x%02x mode:%d", p_rsp->nfcee_id, p_rsp->mode);
    p_cb = nfa_ee_find_ecb (p_rsp->nfcee_id);
    if (p_cb == NULL)
    {
        NFA_TRACE_ERROR1 ("nfa_ee_nci_mode_set_rsp() Can not find cb for handle:0x%02x", p_rsp->nfcee_id);
        return;
    }

    /* update routing table and vs on mode change */
    nfa_ee_start_timer();

    if (p_rsp->status == NFA_STATUS_OK)
    {

        if (p_rsp->mode == NFA_EE_MD_ACTIVATE)
        {
            p_cb->ee_status = NFC_NFCEE_STATUS_ACTIVE;
        }
        else
        {
            if (p_cb->tech_switch_on | p_cb->tech_switch_off | p_cb->tech_battery_off |
                p_cb->proto_switch_on| p_cb->proto_switch_off| p_cb->proto_battery_off |
                p_cb->aid_entries)
            {
                /* this NFCEE still has configuration when deactivated. clear the configuration */
                nfa_ee_cb.ee_cfged  &= ~nfa_ee_ecb_to_mask(p_cb);
                nfa_ee_cb.ee_cfg_sts|= NFA_EE_STS_CHANGED_ROUTING;
                NFA_TRACE_DEBUG0("deactivating/still configured. Force update");
            }
            p_cb->tech_switch_on    = p_cb->tech_switch_off = p_cb->tech_battery_off    = 0;
            p_cb->proto_switch_on   = p_cb->proto_switch_off= p_cb->proto_battery_off   = 0;
            p_cb->aid_entries       = 0;
            p_cb->ee_status = NFC_NFCEE_STATUS_INACTIVE;
        }
    }
    NFA_TRACE_DEBUG4 ("status:%d ecb_flags  :0x%02x ee_cfged:0x%02x ee_status:%d",
        p_rsp->status, p_cb->ecb_flags  , nfa_ee_cb.ee_cfged, p_cb->ee_status);
    if (p_cb->ecb_flags   & NFA_EE_ECB_FLAGS_RESTORE)
    {
        if (p_cb->conn_st == NFA_EE_CONN_ST_CONN)
        {
            /* NFA_HCI module handles restoring configurations for HCI access */
            if (p_cb->ee_interface[0] != NFC_NFCEE_INTERFACE_HCI_ACCESS)
            {
                NFC_ConnCreate(NCI_DEST_TYPE_NFCEE, p_cb->nfcee_id,  p_cb->use_interface, nfa_ee_conn_cback);
            }
        }
        else
        {
            p_cb->ecb_flags   &= ~NFA_EE_ECB_FLAGS_RESTORE;
            nfa_ee_check_restore_complete();
        }
    }
    else
    {
        mode_set.status     = p_rsp->status;
        mode_set.ee_handle  = (tNFA_HANDLE)p_rsp->nfcee_id | NFA_HANDLE_GROUP_EE;
        mode_set.ee_status  = p_cb->ee_status;

        nfa_ee_report_event(p_cb->p_ee_cback, NFA_EE_MODE_SET_EVT, (tNFA_EE_CBACK_DATA *)&mode_set);
        if (p_cb->ee_status == NFC_NFCEE_STATUS_INACTIVE)
        {
            /* Report NFA_EE_DISCOVER_REQ_EVT for all active NFCEE */
            nfa_ee_report_discover_req_evt();
        }
    }
}

/*******************************************************************************
**
** Function         nfa_ee_nci_conn
**
** Description      process the connection callback events
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_nci_conn(tNFA_EE_MSG *p_data)
{
    tNFA_EE_ECB      *p_cb;
    tNFA_EE_NCI_CONN    *p_cbk   = &p_data->conn;
    tNFC_CONN           *p_conn  = p_data->conn.p_data;
    BT_HDR              *p_pkt   = NULL;
    tNFA_EE_CBACK_DATA  evt_data = {0};
    tNFA_EE_EVT         event    = NFA_EE_INVALID;
    tNFA_EE_CBACK       *p_cback = NULL;

    if (p_cbk->event == NFC_CONN_CREATE_CEVT)
    {
        p_cb = nfa_ee_find_ecb (p_cbk->p_data->conn_create.id);
    }
    else
    {
        p_cb = nfa_ee_find_ecb_by_conn_id (p_cbk->conn_id);
        if (p_cbk->event == NFC_DATA_CEVT)
            p_pkt = p_conn->data.p_data;
    }

    if (p_cb)
    {
        p_cback         = p_cb->p_ee_cback;
        evt_data.handle = (tNFA_HANDLE)p_cb->nfcee_id | NFA_HANDLE_GROUP_EE;
        switch (p_cbk->event)
        {
        case NFC_CONN_CREATE_CEVT:
            if (p_conn->conn_create.status == NFC_STATUS_OK)
            {
                p_cb->conn_id = p_cbk->conn_id;
                p_cb->conn_st = NFA_EE_CONN_ST_CONN;
            }
            else
            {
                p_cb->conn_st = NFA_EE_CONN_ST_NONE;
            }
            if (p_cb->ecb_flags & NFA_EE_ECB_FLAGS_RESTORE)
            {
                p_cb->ecb_flags &= ~NFA_EE_ECB_FLAGS_RESTORE;
                nfa_ee_check_restore_complete();
            }
            else
            {
                evt_data.connect.status       = p_conn->conn_create.status;
                evt_data.connect.ee_interface = p_cb->use_interface;
                event = NFA_EE_CONNECT_EVT;
            }
            break;

        case NFC_CONN_CLOSE_CEVT:
            if (p_cb->conn_st != NFA_EE_CONN_ST_DISC)
                event = NFA_EE_DISCONNECT_EVT;
            p_cb->conn_st    = NFA_EE_CONN_ST_NONE;
            p_cb->p_ee_cback = NULL;
            p_cb->conn_id    = 0;
            if (nfa_ee_cb.em_state == NFA_EE_EM_STATE_DISABLING)
            {
                if (nfa_ee_cb.ee_flags & NFA_EE_FLAG_WAIT_DISCONN)
                {
                    if (nfa_ee_cb.num_ee_expecting)
                    {
                        nfa_ee_cb.num_ee_expecting--;
                    }
                }
                if (nfa_ee_cb.num_ee_expecting == 0)
                {
                    nfa_ee_cb.ee_flags &= ~NFA_EE_FLAG_WAIT_DISCONN;
                    nfa_ee_check_disable();
                }
            }
            break;

        case NFC_DATA_CEVT:
            if (p_cb->conn_st == NFA_EE_CONN_ST_CONN)
            {
                /* report data event only in connected state */
                if (p_cb->p_ee_cback && p_pkt)
                {
                    evt_data.data.len   = p_pkt->len;
                    evt_data.data.p_buf = (UINT8 *)(p_pkt+1) + p_pkt->offset;
                    event               = NFA_EE_DATA_EVT;
                    p_pkt               = NULL; /* so this function does not free this GKI buffer */
                }
            }
            break;
        }

        if ((event != NFA_EE_INVALID) && (p_cback))
            (*p_cback)(event, &evt_data);
    }
    if (p_pkt)
        GKI_freebuf (p_pkt);
}


/*******************************************************************************
**
** Function         nfa_ee_nci_action_ntf
**
** Description      process the NFCEE action callback event
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_nci_action_ntf(tNFA_EE_MSG *p_data)
{
    tNFC_EE_ACTION_REVT *p_cbk = p_data->act.p_data;
    tNFA_EE_ACTION      evt_data;

    evt_data.ee_handle  = (tNFA_HANDLE)p_cbk->nfcee_id | NFA_HANDLE_GROUP_EE;
    evt_data.trigger    = p_cbk->act_data.trigger;
    memcpy (&(evt_data.param), &(p_cbk->act_data.param), sizeof (tNFA_EE_ACTION_PARAM));
    nfa_ee_report_event(NULL, NFA_EE_ACTION_EVT, (tNFA_EE_CBACK_DATA *)&evt_data);
}

/*******************************************************************************
**
** Function         nfa_ee_nci_disc_req_ntf
**
** Description      process the NFCEE discover request callback event
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_nci_disc_req_ntf(tNFA_EE_MSG *p_data)
{
    tNFC_EE_DISCOVER_REQ_REVT   *p_cbk = p_data->disc_req.p_data;
    tNFA_HANDLE         ee_handle;
    tNFA_EE_ECB         *p_cb = NULL;
    UINT8 xx;

    NFA_TRACE_DEBUG2 ("nfa_ee_nci_disc_req_ntf () num_info: %d cur_ee:%d", p_cbk->num_info, nfa_ee_cb.cur_ee );

    for (xx = 0; xx < p_cbk->num_info; xx++)
    {
        ee_handle = NFA_HANDLE_GROUP_EE|p_cbk->info[xx].nfcee_id;

        p_cb = nfa_ee_find_ecb (p_cbk->info[xx].nfcee_id);
        if (!p_cb)
        {
            NFA_TRACE_DEBUG1 ("Cannot find cb for NFCEE: 0x%x", p_cbk->info[xx].nfcee_id);
            p_cb = nfa_ee_find_ecb (NFA_EE_INVALID);
            if (p_cb)
            {
                p_cb->nfcee_id   = p_cbk->info[xx].nfcee_id;
                p_cb->ecb_flags |= NFA_EE_ECB_FLAGS_ORDER;
            }
            else
            {
                NFA_TRACE_ERROR1 ("Cannot allocate cb for NFCEE: 0x%x", p_cbk->info[xx].nfcee_id);
                continue;
            }
        }

        p_cb->ecb_flags |= NFA_EE_ECB_FLAGS_DISC_REQ;
        if (p_cbk->info[xx].op == NFC_EE_DISC_OP_ADD)
        {
            if (p_cbk->info[xx].tech_n_mode == NFC_DISCOVERY_TYPE_LISTEN_A)
            {
                p_cb->la_protocol = p_cbk->info[xx].protocol;
            }
            else if (p_cbk->info[xx].tech_n_mode == NFC_DISCOVERY_TYPE_LISTEN_B)
            {
                p_cb->lb_protocol = p_cbk->info[xx].protocol;
            }
            else if (p_cbk->info[xx].tech_n_mode == NFC_DISCOVERY_TYPE_LISTEN_F)
            {
                p_cb->lf_protocol = p_cbk->info[xx].protocol;
            }
            else if (p_cbk->info[xx].tech_n_mode == NFC_DISCOVERY_TYPE_LISTEN_B_PRIME)
            {
                p_cb->lbp_protocol = p_cbk->info[xx].protocol;
            }
            NFA_TRACE_DEBUG6 ("nfcee_id=0x%x ee_status=0x%x ecb_flags=0x%x la_protocol=0x%x la_protocol=0x%x la_protocol=0x%x",
                p_cb->nfcee_id, p_cb->ee_status, p_cb->ecb_flags,
                p_cb->la_protocol, p_cb->lb_protocol, p_cb->lf_protocol);
        }
        else
        {
            if (p_cbk->info[xx].tech_n_mode == NFC_DISCOVERY_TYPE_LISTEN_A)
            {
                p_cb->la_protocol = 0;
            }
            else if (p_cbk->info[xx].tech_n_mode == NFC_DISCOVERY_TYPE_LISTEN_B)
            {
                p_cb->lb_protocol = 0;
            }
            else if (p_cbk->info[xx].tech_n_mode == NFC_DISCOVERY_TYPE_LISTEN_F)
            {
                p_cb->lf_protocol = 0;
            }
            else if (p_cbk->info[xx].tech_n_mode == NFC_DISCOVERY_TYPE_LISTEN_B_PRIME)
            {
                p_cb->lbp_protocol = 0;
        }
        }
    }


    /* Report NFA_EE_DISCOVER_REQ_EVT for all active NFCEE */
    if ((p_cb->ecb_flags & NFA_EE_ECB_FLAGS_ORDER) == 0)
        nfa_ee_report_discover_req_evt();

}

/*******************************************************************************
**
** Function         nfa_ee_is_active
**
** Description      Check if the given NFCEE is active
**
** Returns          TRUE if the given NFCEE is active
**
*******************************************************************************/
BOOLEAN nfa_ee_is_active (tNFA_HANDLE nfcee_id)
{
    BOOLEAN is_active = FALSE;
    int     xx;
    tNFA_EE_ECB  *p_cb = nfa_ee_cb.ecb;

    if ((NFA_HANDLE_GROUP_MASK & nfcee_id) == NFA_HANDLE_GROUP_EE)
        nfcee_id    &= NFA_HANDLE_MASK;

    /* compose output */
    for (xx = 0; xx < nfa_ee_cb.cur_ee; xx++, p_cb++)
    {
        if ((tNFA_HANDLE)p_cb->nfcee_id == nfcee_id)
        {
            if (p_cb->ee_status == NFA_EE_STATUS_ACTIVE)
            {
                is_active = TRUE;
            }
            break;
        }
    }
    return is_active;
}

/*******************************************************************************
**
** Function         nfa_ee_get_tech_route
**
** Description      Given a power state, find the technology routing destination.
**                  The result is filled in the given p_handles
**                  in the order of A, B, F, Bprime
**
** Returns          None
**
*******************************************************************************/
void nfa_ee_get_tech_route (UINT8 power_state, UINT8 *p_handles)
{
    int     xx, yy;
    tNFA_EE_ECB *p_cb;
    UINT8   tech_mask_list[NFA_EE_MAX_TECH_ROUTE] =
    {
        NFA_TECHNOLOGY_MASK_A,
        NFA_TECHNOLOGY_MASK_B,
        NFA_TECHNOLOGY_MASK_F,
        NFA_TECHNOLOGY_MASK_B_PRIME
    };

    NFA_TRACE_DEBUG1("nfa_ee_get_tech_route(): %d", power_state);

    for (xx = 0; xx < NFA_EE_MAX_TECH_ROUTE; xx++)
    {
        p_handles[xx] = NFC_DH_ID;
        p_cb = &nfa_ee_cb.ecb[nfa_ee_cb.cur_ee - 1];
        for (yy = 0; yy < nfa_ee_cb.cur_ee; yy++, p_cb--)
        {
            if (p_cb->ee_status == NFC_NFCEE_STATUS_ACTIVE)
            {
                switch (power_state)
                {
                case NFA_EE_PWR_STATE_ON:
                    if (p_cb->tech_switch_on & tech_mask_list[xx])
                        p_handles[xx] = p_cb->nfcee_id;
                    break;
                case NFA_EE_PWR_STATE_SWITCH_OFF:
                    if (p_cb->tech_switch_off & tech_mask_list[xx])
                        p_handles[xx] = p_cb->nfcee_id;
                    break;
                case NFA_EE_PWR_STATE_BATT_OFF:
                    if (p_cb->tech_battery_off & tech_mask_list[xx])
                        p_handles[xx] = p_cb->nfcee_id;
                    break;
                }
            }
        }
    }
    NFA_TRACE_DEBUG4("0x%x, 0x%x, 0x%x, 0x%x", p_handles[0], p_handles[1], p_handles[2], p_handles[3]);
}

/*******************************************************************************
**
** Function         nfa_ee_route_add_one_ecb
**
** Description      Add the routing entries for one NFCEE/DH
**
** Returns          NFA_STATUS_OK, if ok to continue
**
*******************************************************************************/
tNFA_STATUS nfa_ee_route_add_one_ecb(tNFA_EE_ECB *p_cb, int max_len, BOOLEAN more, UINT8 *ps, int *p_cur_offset)
{
    UINT8   *p, tlv_size, *pa;
    UINT8   num_tlv, len;
    int     xx;
    int     start_offset;
    UINT8   power_cfg = 0;
    UINT8   *pp = ps + *p_cur_offset;
    UINT8   entry_size;
    UINT8   max_tlv = (UINT8)((max_len > NFA_EE_ROUT_MAX_TLV_SIZE)?NFA_EE_ROUT_MAX_TLV_SIZE:max_len);
    tNFA_STATUS status = NFA_STATUS_OK;

    /* use the first byte of the buffer (ps) to keep the num_tlv */
    num_tlv  = *ps;
    NFA_TRACE_DEBUG5 ("nfa_ee_route_add_one_ecb max_len:%d, max_tlv:%d, cur_offset:%d, more:%d, num_tlv:%d",
        max_len, max_tlv, *p_cur_offset, more, num_tlv);
    pp       = ps + 1 + *p_cur_offset;
    p        = pp;
    tlv_size = (UINT8)*p_cur_offset;
    /* add the Technology based routing */
    for (xx = 0; xx < NFA_EE_NUM_TECH; xx++)
    {
        power_cfg = 0;
        if (p_cb->tech_switch_on & nfa_ee_tech_mask_list[xx])
            power_cfg |= NCI_ROUTE_PWR_STATE_ON;
        if (p_cb->tech_switch_off & nfa_ee_tech_mask_list[xx])
            power_cfg |= NCI_ROUTE_PWR_STATE_SWITCH_OFF;
        if (p_cb->tech_battery_off & nfa_ee_tech_mask_list[xx])
            power_cfg |= NCI_ROUTE_PWR_STATE_BATT_OFF;
        if (power_cfg)
        {
            *pp++   = NFC_ROUTE_TAG_TECH;
            *pp++   = 3;
            *pp++   = p_cb->nfcee_id;
            *pp++   = power_cfg;
            *pp++   = nfa_ee_tech_list[xx];
            num_tlv++;
            if (power_cfg != NCI_ROUTE_PWR_STATE_ON)
                nfa_ee_cb.ee_cfged  |= NFA_EE_CFGED_OFF_ROUTING;
        }
    }

    /* add the Protocol based routing */
    for (xx = 0; xx < NFA_EE_NUM_PROTO; xx++)
    {
        power_cfg = 0;
        if (p_cb->proto_switch_on & nfa_ee_proto_mask_list[xx])
            power_cfg |= NCI_ROUTE_PWR_STATE_ON;
        if (p_cb->proto_switch_off & nfa_ee_proto_mask_list[xx])
            power_cfg |= NCI_ROUTE_PWR_STATE_SWITCH_OFF;
        if (p_cb->proto_battery_off & nfa_ee_proto_mask_list[xx])
            power_cfg |= NCI_ROUTE_PWR_STATE_BATT_OFF;
        if (power_cfg)
        {
            *pp++   = NFC_ROUTE_TAG_PROTO;
            *pp++   = 3;
            *pp++   = p_cb->nfcee_id;
            *pp++   = power_cfg;
            *pp++   = nfa_ee_proto_list[xx];
            num_tlv++;
            if (power_cfg != NCI_ROUTE_PWR_STATE_ON)
                nfa_ee_cb.ee_cfged  |= NFA_EE_CFGED_OFF_ROUTING;
        }
    }

    /* add the AID routing */
    if (p_cb->aid_entries)
    {
        start_offset = 0;
        for (xx = 0; xx < p_cb->aid_entries; xx++)
        {
            /* add one AID entry */
            if (p_cb->aid_rt_info[xx] & NFA_EE_AE_ROUTE)
            {
                num_tlv++;
                pa      = &p_cb->aid_cfg[start_offset];
                pa ++; /* EMV tag */
                len     = *pa++; /* aid_len */
                *pp++   = NFC_ROUTE_TAG_AID;
                *pp++   = len + 2;
                *pp++   = p_cb->nfcee_id;
                *pp++   = p_cb->aid_pwr_cfg[xx];
                /* copy the AID */
                memcpy(pp, pa, len);
                pp     += len;
            }
            start_offset += p_cb->aid_len[xx];
        }
    }
    entry_size = (UINT8)(pp - p);
    tlv_size  += entry_size;
    if (entry_size)
    {
        nfa_ee_cb.ee_cfged |= nfa_ee_ecb_to_mask(p_cb);
    }
    if (p_cb->ecb_flags   & NFA_EE_ECB_FLAGS_ROUTING)
    {
        nfa_ee_cb.ee_cfg_sts   |= NFA_EE_STS_CHANGED_ROUTING;
    }
    NFA_TRACE_DEBUG3 ("ee_cfg_sts:0x%02x entry_size:%d, tlv_size:%d", nfa_ee_cb.ee_cfg_sts, entry_size, tlv_size);

    if (tlv_size > max_tlv)
    {
        /* exceeds routing table size - report ERROR */
        status  = NFA_STATUS_BUFFER_FULL;
    }

    else if (more == FALSE)
    {
        /* last entry. update routing table now */
        if (nfa_ee_cb.ee_cfg_sts & NFA_EE_STS_CHANGED_ROUTING)
        {
            if (tlv_size)
            {
                nfa_ee_cb.ee_cfg_sts       |= NFA_EE_STS_PREV_ROUTING;
            }
            else
            {
                nfa_ee_cb.ee_cfg_sts       &= ~NFA_EE_STS_PREV_ROUTING;
            }
            NFC_SetRouting(more, p_cb->nfcee_id, num_tlv, tlv_size, ps + 1);
        }
        else if (nfa_ee_cb.ee_cfg_sts & NFA_EE_STS_PREV_ROUTING)
        {
            if (tlv_size == 0)
            {
                nfa_ee_cb.ee_cfg_sts       &= ~NFA_EE_STS_PREV_ROUTING;
                /* indicated routing is configured to NFCC */
                nfa_ee_cb.ee_cfg_sts       |= NFA_EE_STS_CHANGED_ROUTING;
                NFC_SetRouting(more, p_cb->nfcee_id, 0, 0, ps + 1);
            }
        }
    }
    else
    {
        /* update the total num_tlv current offset */
        *ps              = num_tlv;
        *p_cur_offset   += entry_size;
    }

    return status;
}


/*******************************************************************************
**
** Function         nfa_ee_need_recfg
**
** Description      Check if any API function to configure the routing table or
**                  VS is called since last update
**
**                  The algorithm for the NFCEE configuration handling is as follows:
**
**                  Each NFCEE_ID/DH has its own control block - tNFA_EE_ECB
**                  Each control block uses ecb_flags to keep track if an API
**                  that changes routing/VS is invoked.
**                  This ecb_flags is cleared at the end of nfa_ee_update_rout().
**
**                  nfa_ee_cb.ee_cfged is the bitmask of the control blocks with
**                  routing/VS configuration and NFA_EE_CFGED_UPDATE_NOW.
**                  nfa_ee_cb.ee_cfged is cleared and re-calculated at the end of
**                  nfa_ee_update_rout().
**
**                  nfa_ee_cb.ee_cfg_sts is used to check is any status is changed
**                  and the associated command is issued to NFCC.
**                  nfa_ee_cb.ee_cfg_sts is AND with NFA_EE_STS_PREV at the end of
**                  nfa_ee_update_rout() to clear the NFA_EE_STS_CHANGED bits
**                  (except NFA_EE_STS_CHANGED_CANNED_VS is cleared in nfa_ee_vs_cback)
**
** Returns          TRUE if any configuration is changed
**
*******************************************************************************/
static BOOLEAN nfa_ee_need_recfg(void)
{
    BOOLEAN needed = FALSE;
    UINT32  xx;
    tNFA_EE_ECB  *p_cb;
    UINT8   mask;

    NFA_TRACE_DEBUG2("nfa_ee_need_recfg() ee_cfged: 0x%02x ee_cfg_sts: 0x%02x", nfa_ee_cb.ee_cfged, nfa_ee_cb.ee_cfg_sts);
    /* if no routing/vs is configured, do not need to send the info to NFCC */
    if (nfa_ee_cb.ee_cfged || nfa_ee_cb.ee_cfg_sts)
    {
        if (nfa_ee_cb.ee_cfged & NFA_EE_CFGED_UPDATE_NOW)
        {
            needed = TRUE;
        }
        else if (nfa_ee_cb.ee_cfg_sts & NFA_EE_STS_CHANGED)
        {
            needed = TRUE;
        }
        else
        {
            p_cb = &nfa_ee_cb.ecb[NFA_EE_CB_4_DH];
            mask = 1 << NFA_EE_CB_4_DH;
            for (xx = 0; xx <= nfa_ee_cb.cur_ee; xx++)
            {
                NFA_TRACE_DEBUG3("%d: ecb_flags  : 0x%02x, mask: 0x%02x", xx, p_cb->ecb_flags  , mask);
                if ((p_cb->ecb_flags  ) && (nfa_ee_cb.ee_cfged & mask))
                {
                    needed = TRUE;
                    break;
                }
                p_cb = &nfa_ee_cb.ecb[xx];
                mask = 1 << xx;
            }
        }
    }

    return needed;
}

/*******************************************************************************
**
** Function         nfa_ee_rout_timeout
**
** Description      Anytime VS or routing entries are changed,
**                  a 1 second timer is started. This function is called when
**                  the timer expires or NFA_EeUpdateNow() is called.
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_rout_timeout(tNFA_EE_MSG *p_data)
{
    NFA_TRACE_DEBUG0("nfa_ee_rout_timeout()");
    if (nfa_ee_need_recfg())
    {
        /* discovery is not started */
        nfa_ee_update_rout();
    }
}

/*******************************************************************************
**
** Function         nfa_ee_discv_timeout
**
** Description
**
**
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_discv_timeout(tNFA_EE_MSG *p_data)
{
    NFC_NfceeDiscover(FALSE);
    if (nfa_ee_cb.p_enable_cback)
        (*nfa_ee_cb.p_enable_cback)(NFA_EE_DISC_STS_OFF);
}

/*******************************************************************************
**
** Function         nfa_ee_lmrt_to_nfcc
**
** Description      This function would set the listen mode routing table
**                  to NFCC.
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_lmrt_to_nfcc(tNFA_EE_MSG *p_data)
{
    int xx;
    tNFA_EE_ECB          *p_cb;
    UINT8   *p = NULL;
    BOOLEAN more = TRUE;
    UINT8   last_active = NFA_EE_INVALID;
    int     max_len, len;
    tNFA_STATUS status = NFA_STATUS_FAILED;
    int     cur_offset;

    /* update routing table: DH and the activated NFCEEs */
    p = (UINT8 *)GKI_getbuf(NFA_EE_ROUT_BUF_SIZE);
    if (p == NULL)
    {
        NFA_TRACE_ERROR0 ("nfa_ee_lmrt_to_nfcc() no buffer to send routing info.");
        nfa_ee_report_event( NULL, NFA_EE_NO_MEM_ERR_EVT, (tNFA_EE_CBACK_DATA *)&status);
        return;
    }

    /* find the last active NFCEE. */
    p_cb = &nfa_ee_cb.ecb[nfa_ee_cb.cur_ee - 1];
    for (xx = 0; xx < nfa_ee_cb.cur_ee; xx++, p_cb--)
    {
        if (p_cb->ee_status == NFC_NFCEE_STATUS_ACTIVE)
        {
            if (last_active == NFA_EE_INVALID)
            {
                last_active = p_cb->nfcee_id;
                NFA_TRACE_DEBUG1 ("last_active: 0x%x", last_active);
            }
        }
    }
    if (last_active == NFA_EE_INVALID)
    {
        more = FALSE;
    }

    /* add the routing for DH first */
    status  = NFA_STATUS_OK;
    max_len = NFC_GetLmrtSize();
    cur_offset  = 0;
    /* use the first byte of the buffer (p) to keep the num_tlv */
    *p          = 0;
    status = nfa_ee_route_add_one_ecb(&nfa_ee_cb.ecb[NFA_EE_CB_4_DH], max_len, more, p, &cur_offset);

    /* add only what is supported by NFCC. report overflow */
    if (status == NFA_STATUS_OK)
    {
        /* add the routing for NFCEEs */
        p_cb = &nfa_ee_cb.ecb[0];
        for (xx = 0; (xx < nfa_ee_cb.cur_ee) && more; xx++, p_cb++)
        {
            len = 0;
            if (p_cb->ee_status == NFC_NFCEE_STATUS_ACTIVE)
            {
                NFA_TRACE_DEBUG2 ("nfcee_id:0x%x, last_active: 0x%x", p_cb->nfcee_id, last_active);
                if (last_active == p_cb->nfcee_id)
                    more = FALSE;
                status = nfa_ee_route_add_one_ecb(p_cb, max_len, more, p, &cur_offset);
                if (status != NFA_STATUS_OK)
                {
                    more    = FALSE;
                }
            }
        }
    }
    if (status != NFA_STATUS_OK)
    {
        nfa_ee_report_event( NULL, NFA_EE_ROUT_ERR_EVT, (tNFA_EE_CBACK_DATA *)&status);
    }
    GKI_freebuf(p);
}

/*******************************************************************************
**
** Function         nfa_ee_update_rout
**
** Description      This function would set the VS and listen mode routing table
**                  to NFCC.
**
** Returns          void
**
*******************************************************************************/
void nfa_ee_update_rout(void)
{
    int xx;
    tNFA_EE_ECB          *p_cb;
    UINT8   mask;
    BT_HDR  msg;

    NFA_TRACE_DEBUG1 ("nfa_ee_update_rout ee_cfg_sts:0x%02x", nfa_ee_cb.ee_cfg_sts);

    /* use action function to send routing and VS configuration to NFCC */
    msg.event = NFA_EE_CFG_TO_NFCC_EVT;
    nfa_ee_evt_hdlr (&msg);

    /* all configuration is updated to NFCC, clear the status mask */
    nfa_ee_cb.ee_cfg_sts   &= NFA_EE_STS_PREV;
    nfa_ee_cb.ee_cfged  = 0;
    p_cb                = &nfa_ee_cb.ecb[0];
    for (xx = 0; xx < NFA_EE_NUM_ECBS; xx++, p_cb++)
    {
        p_cb->ecb_flags     = 0;
        mask                = (1 << xx);
        if (p_cb->tech_switch_on | p_cb->tech_switch_off | p_cb->tech_battery_off |
            p_cb->proto_switch_on| p_cb->proto_switch_off| p_cb->proto_battery_off |
            p_cb->aid_entries)
        {
            /* this entry has routing configuration. mark it configured */
            nfa_ee_cb.ee_cfged  |= mask;
        }
    }
    NFA_TRACE_DEBUG2 ("ee_cfg_sts:0x%02x ee_cfged:0x%02x", nfa_ee_cb.ee_cfg_sts, nfa_ee_cb.ee_cfged);
}


