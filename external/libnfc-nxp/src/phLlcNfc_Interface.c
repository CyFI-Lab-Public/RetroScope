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
* \file  phLlcNfc_Interface.c
* \brief Interface for both LLC and transport layer
*
* Project: NFC-FRI-1.1
*
* $Date: Tue Jun  1 14:41:26 2010 $
* $Author: ing02260 $
* $Revision: 1.75 $
* $Aliases: NFC_FRI1.1_WK1023_R35_1 $
*
*/

/*************************** Includes *******************************/
#include <phNfcTypes.h>
#include <phNfcStatus.h>
#include <phOsalNfc.h>
#include <phNfcInterface.h>
#include <phLlcNfc_DataTypes.h>
#include <phLlcNfc_Timer.h>
#include <phLlcNfc_Frame.h>
#include <phLlcNfc.h>
#include <phLlcNfc_Interface.h>
#ifdef PH_LLCNFC_STUB
#include <phDalNfc_Stub.h>
#endif
#ifdef PH_LLCNFC_DALINT
#include <phDal4Nfc.h>
#endif
#define LOG_TAG "NFC-LLC"

#include <utils/Log.h>
/*********************** End of includes ****************************/

/***************************** Macros *******************************/
#define PH_LLCNFC_APPEND_LEN                        (4)
#define LLC_NS_FRAME_HEADER_MASK                    (0x38U)
/************************ End of macros *****************************/

/*********************** Local functions ****************************/
static
void 
phLlcNfc_WrResp_Cb(
                   void                        *pContext, 
                   void                        *pHwInfo, 
                   phNfc_sTransactionInfo_t    *pCompInfo
                   );

static
void 
phLlcNfc_RdResp_Cb(
                   void                        *pContext,
                   void                        *pHwInfo, 
                   phNfc_sTransactionInfo_t    *pCompInfo
                   );

/******************** End of Local functions ************************/

/********************** Global variables ****************************/
int libnfc_llc_error_count = 0;

/******************** End of Global Variables ***********************/

NFCSTATUS 
phLlcNfc_Interface_Register(
    phLlcNfc_Context_t          *psLlcCtxt, 
    phNfcLayer_sCfg_t           *psIFConfig
)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    phNfcIF_sCallBack_t         if_cb = {0,0,0,0};
    phNfcIF_sReference_t        sreference = {0,0,0};
    
    if ((NULL == psLlcCtxt) || (NULL == psIFConfig))
    {
        result = PHNFCSTVAL(CID_NFC_LLC, 
                            NFCSTATUS_INVALID_PARAMETER);
    }
    else 
    {
        result = NFCSTATUS_SUCCESS;
        if_cb.notify = NULL;
        if_cb.receive_complete = (pphNfcIF_Transact_Completion_CB_t)&phLlcNfc_RdResp_Cb;
        if_cb.send_complete = (pphNfcIF_Transact_Completion_CB_t)&phLlcNfc_WrResp_Cb;
        if_cb.pif_ctxt = psLlcCtxt; 
        sreference.plower_if = &(psLlcCtxt->lower_if);
        result = PHNFCSTVAL(CID_NFC_LLC, NFCSTATUS_INVALID_PARAMETER);
#ifdef PH_LLCNFC_STUB
        result = phDalNfc_StubRegister(&sreference, if_cb, psIFConfig->layer_next);
#endif /* #ifdef PH_LLCNFC_STUB */
#ifdef PH_LLCNFC_DALINT
        result = phDal4Nfc_Register(&sreference, if_cb, psIFConfig->layer_next);
#else
        if ((NULL != psIFConfig->layer_next) && 
            (NULL != psIFConfig->layer_next->layer_registry))
        {
            result = psIFConfig->layer_next->layer_registry(
                        &sreference, 
                        if_cb, 
                        (void *)&psIFConfig[(psIFConfig->layer_index - 1)]);
        }
#endif /* #ifdef PH_LLCNFC_DALINT */
    }
    PH_LLCNFC_DEBUG("Llc Dal Interface Register result : 0x%x\n", result);
    return result;
}
 
NFCSTATUS 
phLlcNfc_Interface_Init(
    phLlcNfc_Context_t      *psLlcCtxt
)
{
    /*
        1. Get the pointer of the main llc context
    */
    NFCSTATUS   result = NFCSTATUS_SUCCESS;
    if ((NULL == psLlcCtxt) ||  
        (NULL == psLlcCtxt->lower_if.init))
    {
        result = PHNFCSTVAL(CID_NFC_LLC, 
                            NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {        
        /* Initialise the main context */
        result = psLlcCtxt->lower_if.init(  psLlcCtxt->lower_if.pcontext, 
                                            psLlcCtxt->phwinfo);        
    }
    PH_LLCNFC_DEBUG("Llc Dal Interface Init result : 0x%x\n", result);
    return result;
}

NFCSTATUS 
phLlcNfc_Interface_Read(
    phLlcNfc_Context_t      *psLlcCtxt, 
    uint8_t                 readWaitOn, 
    uint8_t                 *pLlcBuffer, 
    uint32_t                llcBufferLength
)
{
    NFCSTATUS   result = NFCSTATUS_PENDING;
    /*
        1. Call DAL or TL read with "phLlcNfc_LlcTl_RdResp_Cb" as 
            callback function
    */
    PH_LLCNFC_PRINT("Llc Dal Interface Read called\n");
    if ((NULL == psLlcCtxt) || (NULL == pLlcBuffer) || 
        (0 == llcBufferLength) || (NULL == psLlcCtxt->lower_if.receive) || 
        (readWaitOn > PH_LLCNFC_READWAIT_ON))
    {
        result = PHNFCSTVAL(CID_NFC_LLC, NFCSTATUS_INVALID_PARAMETER);
    }
    else if (PH_LLCNFC_READPEND_FLAG_OFF != 
            psLlcCtxt->s_frameinfo.read_pending)
    {
        /* do nothing */
    } 
    else 
    {
        if (PH_LLCNFC_READWAIT_OFF == readWaitOn)
        {   
            result = psLlcCtxt->lower_if.receive(
                            psLlcCtxt->lower_if.pcontext, 
                            psLlcCtxt->phwinfo, 
                            pLlcBuffer, 
                            (uint8_t)llcBufferLength);
        } 
        else
        {
            result = psLlcCtxt->lower_if.receive_wait(
                            psLlcCtxt->lower_if.pcontext, 
                            psLlcCtxt->phwinfo, 
                            pLlcBuffer, 
                            (uint16_t)llcBufferLength);
        }

        if(NFCSTATUS_PENDING == result)
        {
            if (PH_LLCNFC_READPEND_ONE_BYTE == llcBufferLength)
            {
                psLlcCtxt->s_frameinfo.read_pending = 
                                    PH_LLCNFC_READPEND_ONE_BYTE;
            }
            else
            {
                psLlcCtxt->s_frameinfo.read_pending = 
                                    PH_LLCNFC_READPEND_REMAIN_BYTE;
            }
        }
    }
    PH_LLCNFC_DEBUG("Llc Dal Interface Read result : 0x%x\n", result);
    return result;
}

NFCSTATUS 
phLlcNfc_Interface_Write(
    phLlcNfc_Context_t      *psLlcCtxt,
    uint8_t             *pLlcBuffer, 
    uint32_t            llcBufferLength
)
{
    NFCSTATUS   result = NFCSTATUS_PENDING;

    PH_LLCNFC_PRINT("Llc Dal Interface Write called\n");
    /*
        1. Call DAL or TL write with "phLlcNfc_LlcTl_WrResp_Cb" as 
            callback function
    */
    if ((NULL == psLlcCtxt) || (NULL == pLlcBuffer) || 
        (0 == llcBufferLength) || 
        (NULL == psLlcCtxt->lower_if.send))
    {
        PH_LLCNFC_DEBUG ("psLlcCtxt : 0x%p\n", psLlcCtxt);
        PH_LLCNFC_DEBUG ("pLlcBuffer : 0x%p\n", pLlcBuffer);
        PH_LLCNFC_DEBUG ("llcBufferLength : 0x%08X\n", llcBufferLength);
        result = PHNFCSTVAL(CID_NFC_LLC, NFCSTATUS_INVALID_PARAMETER);
    }
    else 
    {        
        PH_LLCNFC_PRINT("Buffer to be send to Dal : \n");
        PH_LLCNFC_PRINT_BUFFER(pLlcBuffer, llcBufferLength);

        if ((TRUE == psLlcCtxt->s_frameinfo.write_pending) || 
            (PH_LLCNFC_READPEND_REMAIN_BYTE == 
            psLlcCtxt->s_frameinfo.read_pending))
        {
            result = PHNFCSTVAL(CID_NFC_LLC, NFCSTATUS_BUSY);
        }
        else
        {
#ifdef LLC_DATA_BYTES

            PH_LLCNFC_PRINT_DATA (pLlcBuffer, llcBufferLength);
            PH_LLCNFC_STRING (";\n");
    
#endif /* LLC_DATA_BYTES */

            psLlcCtxt->s_frameinfo.s_llcpacket.llcbuf_len = (uint8_t)llcBufferLength;
            (void)memcpy ((void *)&(psLlcCtxt->s_frameinfo.s_llcpacket.s_llcbuf),
                        (void *)pLlcBuffer, llcBufferLength);

            result = psLlcCtxt->lower_if.send(psLlcCtxt->lower_if.pcontext, 
                                                psLlcCtxt->phwinfo, 
                                                (uint8_t *)&(psLlcCtxt->s_frameinfo.s_llcpacket.s_llcbuf),
                                                (uint16_t)llcBufferLength);
            if(NFCSTATUS_PENDING == result)
            {
                psLlcCtxt->s_frameinfo.write_pending = TRUE;
#ifdef PIGGY_BACK
                /* Stop the ACK timer, as the ACK or I frame is sent */
                phLlcNfc_StopTimers (PH_LLCNFC_ACKTIMER, 0);
                /* ACK is sent, so reset the response received count */
                psLlcCtxt->s_frameinfo.resp_recvd_count = 0;
#endif /* #ifdef PIGGY_BACK */
            }
        }
    }
    PH_LLCNFC_DEBUG("Llc Dal Interface Write result : 0x%x\n", result);
    return result;
}

static
void 
phLlcNfc_WrResp_Cb(
    void                        *pContext, 
    void                        *pHwInfo, 
    phNfc_sTransactionInfo_t    *pCompInfo
)
{
    /* 
        1. Check the window size, if window size = windows
        1. Call the send callback, which has been registered by upper 
            layer
    */
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    phLlcNfc_Context_t          *ps_llc_ctxt = (phLlcNfc_Context_t*)pContext;
    phLlcNfc_Frame_t            *ps_frame_info = NULL;
    phLlcNfc_LlcPacket_t        *ps_recv_pkt = NULL;
    phLlcNfc_StoreIFrame_t      *ps_store_frame = NULL;
    phNfc_sCompletionInfo_t     notifyinfo = {0,0,0};
    uint8_t                     count = 0;

    PH_LLCNFC_PRINT("\n\nLLC : WRITE RESP CB CALLED\n\n");
    
    if ((NULL != ps_llc_ctxt) && (NULL != pCompInfo) && (NULL != pHwInfo))
    {
        ps_llc_ctxt->s_frameinfo.write_pending = FALSE;

        PHNFC_UNUSED_VARIABLE(result);
        
        if(NFCSTATUS_SUCCESS == pCompInfo->status)
        {
            ps_frame_info = &(ps_llc_ctxt->s_frameinfo);
            ps_recv_pkt = &(ps_frame_info->s_recvpacket);
            ps_store_frame = &(ps_frame_info->s_send_store);
            count = ps_frame_info->s_send_store.start_pos;

            PH_LLCNFC_DEBUG("RECEIVE length : 0x%02X\n", ps_recv_pkt->llcbuf_len);
            PH_LLCNFC_DEBUG("SENT frame type : 0x%02X\n", ps_frame_info->sent_frame_type);
            PH_LLCNFC_DEBUG("WRITE PENDING : 0x%02X\n", ps_frame_info->write_pending);
            PH_LLCNFC_DEBUG("WRITE PENDING status : 0x%04X\n", ps_frame_info->write_status);
            PH_LLCNFC_DEBUG("WRITE wait frame type : 0x%02X\n", ps_frame_info->write_wait_call);
            PH_LLCNFC_DEBUG("NS START POS : 0x%02X\n", ps_store_frame->start_pos);
            PH_LLCNFC_DEBUG("WIN SIZE : 0x%02X\n", ps_store_frame->winsize_cnt);

            switch(ps_frame_info->sent_frame_type)
            {
                case init_u_rset_frame:
                {
                    /* First U frame sent properly, update sent frame type 
                        in the callback */
                    result = phLlcNfc_Interface_Read (ps_llc_ctxt, 
                                    PH_LLCNFC_READWAIT_OFF, 
                                    &(ps_recv_pkt->s_llcbuf.llc_length_byte),
                                    (uint8_t)PH_LLCNFC_BYTES_INIT_READ);

                    if (NFCSTATUS_BUSY == 
                        PHNFCSTATUS (ps_frame_info->write_status))
                    {
                        ps_frame_info->write_status = NFCSTATUS_PENDING;
                        result = phLlcNfc_H_WriteWaitCall (ps_llc_ctxt);
                    }
                    break;
                }

                case init_u_a_frame:
                {
                    /* First UA frame sent properly, update sent frame type 
                        in the callback. Send the notification to the 
                        upper layer */
                    ps_frame_info->sent_frame_type = write_resp_received;
                    result = phLlcNfc_Interface_Read (ps_llc_ctxt, 
                                    PH_LLCNFC_READWAIT_OFF, 
                                    &(ps_recv_pkt->s_llcbuf.llc_length_byte),
                                    (uint8_t)PH_LLCNFC_BYTES_INIT_READ);

                    if(NULL != ps_llc_ctxt->cb_for_if.notify)
                    {
                        notifyinfo.status = NFCSTATUS_SUCCESS;
                        ps_llc_ctxt->cb_for_if.notify (
                                        ps_llc_ctxt->cb_for_if.pif_ctxt, 
                                        ps_llc_ctxt->phwinfo, 
                                        NFC_NOTIFY_INIT_COMPLETED, 
                                        &notifyinfo);
                    }                    
                    break;
                }

                case u_rset_frame:
                {
                    /* Retries has failed to work, so U frame is sent */
                    ps_frame_info->sent_frame_type = write_resp_received;

                    if (NFCSTATUS_BUSY == 
                        PHNFCSTATUS (ps_frame_info->write_status))
                    {
                        ps_frame_info->write_status = NFCSTATUS_PENDING;
                        result = phLlcNfc_H_WriteWaitCall (ps_llc_ctxt);
                    }
                    break;
                }

                case user_i_frame:
                {
                    /* Send complete */
                    count = ps_frame_info->n_s;

                    ps_store_frame->s_llcpacket[count].frame_to_send = 
                    ps_frame_info->sent_frame_type = write_resp_received;

                    /* N(S) shall be incremented now, because, this callback
                        ensures that packet is sent */
                    count = 
                    ps_frame_info->n_s = ((ps_frame_info->n_s + 1) % 
                                            PH_LLCNFC_MOD_NS_NR);

                    result = phLlcNfc_Interface_Read(ps_llc_ctxt, 
                                    PH_LLCNFC_READWAIT_OFF, 
                                    &(ps_recv_pkt->s_llcbuf.llc_length_byte),
                                    (uint8_t)PH_LLCNFC_BYTES_INIT_READ);

                    if (NFCSTATUS_BUSY == 
                        PHNFCSTATUS (ps_frame_info->write_status))
                    {
                        ps_frame_info->write_status = NFCSTATUS_PENDING;
                        result = phLlcNfc_H_WriteWaitCall (ps_llc_ctxt);
                    }
                   

                    if ((((ps_store_frame->start_pos + ps_store_frame->winsize_cnt) % 
                        PH_LLCNFC_MOD_NS_NR) == ps_frame_info->n_s) && 
                        (ps_frame_info->window_size == ps_store_frame->winsize_cnt))
                    {
                        /* Don't call the upper layer send completion callback, 
                            because last sent frame is the maximum that can be 
                            held by LLC due to windowing
                            store the callback info, call send completion shall 
                            be sent to upper layer only after the ACK is received for the 
                            I frames */
                        ps_llc_ctxt->send_cb_len = (pCompInfo->length - 
                                                    PH_LLCNFC_APPEND_LEN);
                    }
                    else 
                    {
                        /* Send completion is sent to upper layer 
                        Actually, this allows the upper layer to send data, if any
                        */
                        if (NULL != ps_llc_ctxt->cb_for_if.send_complete) 
                        {                            
                            pCompInfo->length = (pCompInfo->length - 
                                                PH_LLCNFC_APPEND_LEN);
                            ps_llc_ctxt->cb_for_if.send_complete (
                                        ps_llc_ctxt->cb_for_if.pif_ctxt, 
                                        pHwInfo, pCompInfo);
                        }
                    }
                    break;
                }

                case s_frame:
                {
#if 0
                    uint8_t         i_frame_ns_value = 0;
#endif /* #if 0 */
                    /* S frame is only sent when ever a I frame is received from  
                        the PN544 in the read response callback, so the received I 
                        frame is acknowledged with S frame. The write response 
                        callback for sent S frame is in progress. */
                    ps_frame_info->sent_frame_type = write_resp_received;

#if 0
                    i_frame_ns_value = 
                        ((ps_store_frame->s_llcpacket[count].s_llcbuf.sllcpayload.llcheader 
                        & LLC_NS_FRAME_HEADER_MASK) >> PH_LLCNFC_NS_START_BIT_POS);


                     PH_LLCNFC_DEBUG("Actual ns value : 0x%02X\n",
                                                  i_frame_ns_value);
#endif /* #if 0 */

                     PH_LLCNFC_DEBUG("Window size : 0x%02X\n",
                                       ps_frame_info->s_send_store.winsize_cnt);
                     PH_LLCNFC_DEBUG("frame to send : 0x%02X\n",
                              ps_store_frame->s_llcpacket[count].frame_to_send);

                    if (NFCSTATUS_BUSY == 
                        PHNFCSTATUS(ps_frame_info->write_status))
                    {
                        ps_frame_info->write_status = NFCSTATUS_PENDING;
                        result = phLlcNfc_H_WriteWaitCall (ps_llc_ctxt);
                    }
#ifdef LLC_UPP_LAYER_NTFY_WRITE_RSP_CB
                    phLlcNfc_H_SendInfo (ps_llc_ctxt);
#endif /* #ifdef LLC_UPP_LAYER_NTFY_WRITE_RSP_CB */
                    break;
                }

#ifdef LLC_RR_INSTEAD_OF_REJ
                case rej_rr_s_frame:
                {
                    if (NFCSTATUS_BUSY == 
                        PHNFCSTATUS(ps_frame_info->write_status))
                    {
                        ps_frame_info->write_status = NFCSTATUS_PENDING;
                        result = phLlcNfc_H_WriteWaitCall (ps_llc_ctxt);
                    }
                    break;
                }
#endif /* #ifdef LLC_RR_INSTEAD_OF_REJ */

                case resend_i_frame:
                {
                    /* The code reaches here, only if stored I frame is sent 
                        No changes here, but send next I frame from the stored list, 
                        in the read response callback, only if proper S or I frame 
                        is received from the PN544 */                    
                    result = phLlcNfc_Interface_Read(ps_llc_ctxt, 
                                    PH_LLCNFC_READWAIT_OFF, 
                                    &(ps_recv_pkt->s_llcbuf.llc_length_byte),
                                    (uint8_t)PH_LLCNFC_BYTES_INIT_READ);

                    if (NFCSTATUS_BUSY == PHNFCSTATUS(ps_frame_info->write_status))
                    {
                        ps_frame_info->write_status = NFCSTATUS_PENDING;
                        result = phLlcNfc_H_WriteWaitCall (ps_llc_ctxt);
                    }

                    if (ps_store_frame->winsize_cnt == 
                        ps_frame_info->window_size)
                    {
                        /* Don't call the upper layer send completion callback, 
                            store the callback info, call send completion after 
                            ack for written frame 
                        ps_llc_ctxt->send_cb_len = pCompInfo->length; */
                    }
                    else 
                    {
                        /* ***** This notification needs to be disabled ***** */
                        if(NULL != ps_llc_ctxt->cb_for_if.send_complete) 
                        {
                            pCompInfo->length = (pCompInfo->length - 
                                                PH_LLCNFC_APPEND_LEN);
                            ps_llc_ctxt->cb_for_if.send_complete(
                                        ps_llc_ctxt->cb_for_if.pif_ctxt, 
                                        pHwInfo, pCompInfo);
                        }
                    } 

                    if(user_i_frame == 
                        ps_store_frame->s_llcpacket[count].frame_to_send)
                    {
                        /* Send complete */
                        ps_store_frame->s_llcpacket[count].frame_to_send = 
                                                            resend_i_frame;
                    }
                    break;
                }

                case rejected_i_frame:
                {
                    /* Update the sent frame type, if window size count is 0 */
                    ps_frame_info->sent_frame_type = write_resp_received;
                    /* The code enters here, whenever a I frame is resent and for 
                        this resent I frame, an I frame received from PN544. 
                        So the S frame is sent as the acknowledgment */
                    if (NFCSTATUS_BUSY == 
                            PHNFCSTATUS(ps_frame_info->write_status))
                    {
                        ps_frame_info->write_status = NFCSTATUS_PENDING;
                        result = phLlcNfc_H_WriteWaitCall (ps_llc_ctxt);
                    }
                    break;
                }

                case resend_s_frame:
                {
                    /* Update the sent frame type, if window size count is 0 */
                    ps_frame_info->sent_frame_type = write_resp_received;
                    /* The code enters here, whenever a I frame is resent and for 
                        this resent I frame, an I frame received from PN544. 
                        So the S frame is sent as the acknowledgment */
                    if (NFCSTATUS_BUSY == 
                            PHNFCSTATUS(ps_frame_info->write_status))
                    {
                        ps_frame_info->write_status = NFCSTATUS_PENDING;
                        result = phLlcNfc_H_WriteWaitCall (ps_llc_ctxt);
                    }
                    
#ifdef LLC_UPP_LAYER_NTFY_WRITE_RSP_CB
                    phLlcNfc_H_SendInfo (ps_llc_ctxt);
#endif /* #ifdef LLC_UPP_LAYER_NTFY_WRITE_RSP_CB */
                    break;
                }

                case reject_s_frame:
                {
                    result = phLlcNfc_Interface_Read(ps_llc_ctxt, 
                                    PH_LLCNFC_READWAIT_OFF, 
                                    &(ps_recv_pkt->s_llcbuf.llc_length_byte),
                                    (uint8_t)PH_LLCNFC_BYTES_INIT_READ);

                    if (NFCSTATUS_BUSY == 
                        PHNFCSTATUS(ps_frame_info->write_status))
                    {
                        ps_frame_info->write_status = NFCSTATUS_PENDING;
                        result = phLlcNfc_H_WriteWaitCall (ps_llc_ctxt);
                    }
                    break;
                }

                case u_a_frame:
                {
                    result = phLlcNfc_Interface_Read(ps_llc_ctxt, 
                                    PH_LLCNFC_READWAIT_OFF, 
                                    &(ps_recv_pkt->s_llcbuf.llc_length_byte),
                                    (uint8_t)PH_LLCNFC_BYTES_INIT_READ);

                    PH_LLCNFC_DEBUG("WIN SIZE : 0x%02X\n", ps_frame_info->s_send_store.winsize_cnt);

                    if(ps_frame_info->s_send_store.winsize_cnt > 0)
                    {
                        result = phLlcNfc_H_SendUserIFrame (ps_llc_ctxt,
                                            &(ps_frame_info->s_send_store));
                    }
                    break;
                }

                case resend_rej_s_frame:
                {
                    result = phLlcNfc_Interface_Read(ps_llc_ctxt, 
                                    PH_LLCNFC_READWAIT_OFF, 
                                    &(ps_recv_pkt->s_llcbuf.llc_length_byte),
                                    (uint8_t)PH_LLCNFC_BYTES_INIT_READ);

                    PH_LLCNFC_DEBUG("WIN SIZE : 0x%02X\n", ps_frame_info->s_send_store.winsize_cnt);

                    if(ps_frame_info->s_send_store.winsize_cnt > 0)
                    {
                        result = phLlcNfc_H_SendTimedOutIFrame (ps_llc_ctxt,
                                            &(ps_frame_info->s_send_store), 0);
                    }
                    break;
                }

                default :
                {
                    break;
                }
            }
        }
        else
        {
            /* Write not successful */
            if(NULL != ps_llc_ctxt->cb_for_if.send_complete)
            {
                phLlcNfc_StopTimers(PH_LLCNFC_GUARDTIMER, 
                                    ps_llc_ctxt->s_timerinfo.guard_to_count);
                PH_LLCNFC_DEBUG("Error status received : 0x%x\n", pCompInfo->status);
                ps_llc_ctxt->cb_for_if.send_complete(
                                    ps_llc_ctxt->cb_for_if.pif_ctxt, 
                                    pHwInfo, pCompInfo);
            }
        }
    }
    PH_LLCNFC_PRINT("\n\nLLC : WRITE RESP CB END\n\n");
}

static
void 
phLlcNfc_RdResp_Cb(
    void                        *pContext,
    void                        *pHwInfo,
    phNfc_sTransactionInfo_t    *pCompInfo
)
{
    /*
        1. LLC Receive has been called by the upper layer, the response 
            for this function is called by the lower layer
        2. Get the frame information from the receive buffer
        3. Depending on the received frame type, process the received 
            buffer
    */
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    phLlcNfc_Context_t          *ps_llc_ctxt = (phLlcNfc_Context_t*)pContext;
    void                        *p_upperctxt = NULL;
    uint8_t                     crc1 = 0, 
                                crc2 = 0;
    phLlcNfc_Frame_t            *ps_frame_info = NULL;
    phLlcNfc_LlcPacket_t        *ps_recv_pkt = NULL;
    phLlcNfc_Payload_t          *ps_llc_payload = NULL;
    pphNfcIF_Notification_CB_t  notifyul = NULL;
    phNfc_sCompletionInfo_t     notifyinfo = {0,0,0};

    PH_LLCNFC_PRINT("\n\nLLC : READ RESP CB CALLED\n\n");
    
    if ((NULL != ps_llc_ctxt) && (NULL != pCompInfo) && (NULL != pHwInfo)
       && (NULL != pCompInfo->buffer))
    {
        ps_frame_info = &(ps_llc_ctxt->s_frameinfo);
        ps_recv_pkt = &(ps_frame_info->s_recvpacket);
        ps_llc_payload = &(ps_recv_pkt->s_llcbuf.sllcpayload);

        ps_llc_ctxt->s_frameinfo.read_pending = PH_LLCNFC_READPEND_FLAG_OFF;
        
        if (NFCSTATUS_SUCCESS == pCompInfo->status)
        {
            if ((PH_LLCNFC_MIN_BUFLEN_RECVD == pCompInfo->length) &&
                (((PH_LLCNFC_MIN_BUFLEN_RECVD + 1) < *(pCompInfo->buffer)) &&
                (PH_LLCNFC_MAX_BUFLEN_RECV_SEND > *(pCompInfo->buffer))))
            {
                PH_LLCNFC_PRINT("Buffer received : \n");
                PH_LLCNFC_PRINT_BUFFER(pCompInfo->buffer, pCompInfo->length);

#if 0
                /* Received length is 1 and receive buffer 
                contains the length field which is greater than 2, 
                so read the remaining bytes*/
                ps_recv_pkt->s_llcbuf.llc_length_byte = pCompInfo->buffer[0];
#endif
                result = phLlcNfc_Interface_Read(ps_llc_ctxt, 
                                PH_LLCNFC_READWAIT_OFF, 
                                (uint8_t *)ps_llc_payload, 
                                (uint32_t)(ps_recv_pkt->s_llcbuf.llc_length_byte));

                if ((init_u_rset_frame == ps_frame_info->sent_frame_type) && 
                    (NFCSTATUS_PENDING != result) && 
                    (NULL != ps_llc_ctxt->cb_for_if.notify))
                {
                    PH_LLCNFC_PRINT("Initialised error\n");
                    notifyinfo.status = result;
                    /* Copy the upper layer callback pointer and the upper 
                        layer context, after that call release */
                    notifyul = ps_llc_ctxt->cb_for_if.notify;
                    p_upperctxt = ps_llc_ctxt->cb_for_if.pif_ctxt;
                    result = phLlcNfc_Release(ps_llc_ctxt, pHwInfo);

                    /* Wrong result, so Init failed sent */
                    notifyul(p_upperctxt, pHwInfo, 
                            NFC_NOTIFY_INIT_FAILED, &notifyinfo);
                }
            }
            else if (TRUE == ps_llc_ctxt->s_frameinfo.write_pending)
            {
                /* Ignore the bytes as write is not complete and 
                pend a read for reading 1 byte */
                result = phLlcNfc_Interface_Read(ps_llc_ctxt, 
                                PH_LLCNFC_READWAIT_OFF, 
                                (uint8_t *)&(
                                ps_recv_pkt->s_llcbuf.llc_length_byte), 
                                PH_LLCNFC_MIN_BUFLEN_RECVD);
            }
            else if (((PH_LLCNFC_MIN_BUFLEN_RECVD + 1) < pCompInfo->length) &&
                (PH_LLCNFC_MAX_BUFLEN_RECV_SEND > pCompInfo->length) && 
                (pCompInfo->length == ps_recv_pkt->s_llcbuf.llc_length_byte))
            {
                PH_LLCNFC_PRINT("Buffer received : \n");
                PH_LLCNFC_PRINT_BUFFER(pCompInfo->buffer, pCompInfo->length);
                PH_LLCNFC_DEBUG("WIN SIZE : 0x%02X\n", ps_frame_info->s_send_store.winsize_cnt);

                /* Receive is complete, so move the state to INITIALISED */
                if (phLlcNfc_Resend_State != ps_llc_ctxt->state)
                {
                    result = phLlcNfc_H_ChangeState(ps_llc_ctxt, 
                                                    phLlcNfc_Initialised_State);
                }
                /* Copy the received buffer and length */
                ps_recv_pkt->llcbuf_len = (uint8_t)
                                (ps_recv_pkt->s_llcbuf.llc_length_byte + 1);
#if 0
                (void)memcpy(ps_llc_payload, pCompInfo->buffer, 
                            pCompInfo->length);
#endif

                /* 
                Check the CRC
                ps_llc_ctxt->s_frameinfo.s_recvpacket.s_llcbuf : 
                        consists llc length byte + llc header + data + CRC 
                        (which needs to be calculated by the below function)
                ps_llc_ctxt->s_frameinfo.s_recvpacket.llcbuf_len : 
                        Total length of the above buffer
                ps_llc_ctxt->s_frameinfo.s_recvpacket.llcbuf_len - 2 : 
                        -2 because the CRC has to be calculated, only for the 
                        bytes which has llc length byte + llc header + data. 
                        But total length (llcbuf_len) consists of above mentioned 
                        things with 2 byte CRC 
                ps_llc_ctxt->s_frameinfo.s_recvpacket.s_llcbuf.sllcpayload.llcpayload : 
                        consists only data (no length byte and no llc header)
                        (psllcctxt->s_frameinfo.s_recvpacket.llcbuf_len - 4) : 
                        is the array index of the first CRC byte to be calculated
                        (psllcctxt->s_frameinfo.s_recvpacket.llcbuf_len - 3) : 
                        is the array index of the second CRC byte to be calculated
                */
                phLlcNfc_H_ComputeCrc((uint8_t *)&(ps_recv_pkt->s_llcbuf), 
                                    (ps_recv_pkt->llcbuf_len - 2), 
                                    &crc1, &crc2);

                if ((crc1 == ps_llc_payload->llcpayload[
                            (ps_recv_pkt->llcbuf_len - 4)]) 
                    && (crc2 == ps_llc_payload->llcpayload[
                            (ps_recv_pkt->llcbuf_len - 3)]))
                {
                    result = phLlcNfc_H_ProRecvFrame(ps_llc_ctxt);
                }
#ifdef LLC_DISABLE_CRC
                else
                {   
                    result = phLlcNfc_H_ProRecvFrame(ps_llc_ctxt);
                }
#else
                else if (ps_frame_info->recv_error_count < 
                    PH_LLCNFC_MAX_REJ_RETRY_COUNT)
                {
                    ALOGW("LLC bad crc");
                    PH_LLCNFC_PRINT("CRC ERROR RECVD \n");
                    PH_LLCNFC_DEBUG("RECV ERROR COUNT : 0x%02X\n", ps_frame_info->recv_error_count);

                    ps_frame_info->recv_error_count = (uint8_t)
                                    (ps_frame_info->recv_error_count + 1);
                    libnfc_llc_error_count++;

                    result = phLlcNfc_Interface_Read(ps_llc_ctxt, 
                        PH_LLCNFC_READWAIT_OFF, 
                        (uint8_t *)&(ps_recv_pkt->s_llcbuf.llc_length_byte), 
                        PH_LLCNFC_BYTES_INIT_READ);
#ifdef CRC_ERROR_REJ
                    /* Send REJ (S frame), as the CRC received has error  */
                    result = phLlcNfc_H_SendRejectFrame (ps_llc_ctxt);

#endif /* #ifdef CRC_ERROR_REJ */

                }
                else
                {
                    ALOGE("max LLC retries exceeded, stack restart");
                    result = phLlcNfc_Interface_Read (ps_llc_ctxt, 
                                PH_LLCNFC_READWAIT_OFF, 
                                (uint8_t *)&(ps_recv_pkt->s_llcbuf.llc_length_byte), 
                                PH_LLCNFC_BYTES_INIT_READ);

                    /* Raise the exception for CRC error received from the  */
                    notifyinfo.status = PHNFCSTVAL(CID_NFC_LLC, 
                                            NFCSTATUS_BOARD_COMMUNICATION_ERROR);
#if 0
                    phOsalNfc_RaiseException(phOsalNfc_e_UnrecovFirmwareErr,1); 
#endif /* #if 0 */
                        /* Resend done, no answer from the device */
                    ps_llc_ctxt->cb_for_if.notify (
                                    ps_llc_ctxt->cb_for_if.pif_ctxt,
                                    ps_llc_ctxt->phwinfo, 
                                    NFC_NOTIFY_DEVICE_ERROR, 
                                    &notifyinfo);
                }

#endif /* #ifdef LLC_DISABLE_CRC */
            } /* read more than 1 byte */
            else if (ps_frame_info->recv_error_count >= 
                    PH_LLCNFC_MAX_REJ_RETRY_COUNT)
            {
                ALOGE("max LLC retries exceeded, stack restart");
                result = phLlcNfc_Interface_Read (ps_llc_ctxt, 
                        PH_LLCNFC_READWAIT_OFF, 
                        (uint8_t *)&(ps_recv_pkt->s_llcbuf.llc_length_byte), 
                        PH_LLCNFC_BYTES_INIT_READ);

                /* Raise the exception for CRC error received from the  */
                notifyinfo.status = PHNFCSTVAL(CID_NFC_LLC, 
                                        NFCSTATUS_BOARD_COMMUNICATION_ERROR);
#if 0
                phOsalNfc_RaiseException(phOsalNfc_e_UnrecovFirmwareErr,1); 
#endif /* #if 0 */
                    /* Resend done, no answer from the device */
                ps_llc_ctxt->cb_for_if.notify (
                                ps_llc_ctxt->cb_for_if.pif_ctxt,
                                ps_llc_ctxt->phwinfo, 
                                NFC_NOTIFY_DEVICE_ERROR, 
                                &notifyinfo);
            }
            else if (((PH_LLCNFC_MIN_BUFLEN_RECVD + 1) < pCompInfo->length) &&
                (PH_LLCNFC_MAX_BUFLEN_RECV_SEND > pCompInfo->length) && 
                (pCompInfo->length != ps_recv_pkt->s_llcbuf.llc_length_byte))
            {
                ALOGE("bad LLC length1 %d", pCompInfo->length);
                ps_frame_info->recv_error_count = (uint8_t)
                                    (ps_frame_info->recv_error_count + 1);
                libnfc_llc_error_count++;

                result = phLlcNfc_Interface_Read(ps_llc_ctxt, 
                        PH_LLCNFC_READWAIT_OFF, 
                        (uint8_t *)&(ps_recv_pkt->s_llcbuf.llc_length_byte), 
                        PH_LLCNFC_BYTES_INIT_READ);

#ifdef CRC_ERROR_REJ

                /* Send REJ (S frame), as the CRC received has error  */
                result = phLlcNfc_H_SendRejectFrame (ps_llc_ctxt);

#endif /* #ifdef CRC_ERROR_REJ */
            }
            else if ((PH_LLCNFC_MIN_BUFLEN_RECVD == pCompInfo->length) &&
                ((*(pCompInfo->buffer) > (PH_LLCNFC_MAX_BUFLEN_RECV_SEND - 1))
                ||(*(pCompInfo->buffer) <= (PH_LLCNFC_MIN_BUFLEN_RECVD + 1))))
            {
                /* Temporary fix for the 0xFF data received  
                    Solution for the read one byte, giving error in buffer
                    PH_LLCNFC_MAX_BUFLEN_RECV_SEND (0x21) is the maximum 
                    bytes expected by LLC, if the buffer 
                    value is greater than (0x21 - 1), then pend a read to 
                    get 1 byte again
                */
                ALOGW("bad LLC length byte %x\n", *(pCompInfo->buffer));
                ps_frame_info->recv_error_count = (uint8_t)
                                    (ps_frame_info->recv_error_count + 1);
                libnfc_llc_error_count++;

                result = phLlcNfc_Interface_Read(ps_llc_ctxt, 
                        PH_LLCNFC_READWAIT_OFF, 
                        (uint8_t *)&(ps_recv_pkt->s_llcbuf.llc_length_byte), 
                        PH_LLCNFC_BYTES_INIT_READ);
            }
            else
            {
                ALOGW("unknown LLC error1");
                ps_frame_info->recv_error_count = (uint8_t)
                                    (ps_frame_info->recv_error_count + 1);
                libnfc_llc_error_count++;

                phLlcNfc_StopTimers(PH_LLCNFC_GUARDTIMER, 
                                    ps_llc_ctxt->s_timerinfo.guard_to_count);
                pCompInfo->status = PHNFCSTVAL(CID_NFC_LLC, 
                                                NFCSTATUS_INVALID_FORMAT);
                pCompInfo->buffer = NULL;
                pCompInfo->length = 0;
                result = phLlcNfc_Interface_Read(ps_llc_ctxt, 
                        PH_LLCNFC_READWAIT_OFF, 
                        (uint8_t *)&(ps_recv_pkt->s_llcbuf.llc_length_byte), 
                        PH_LLCNFC_BYTES_INIT_READ);
                if (NULL != ps_llc_ctxt->cb_for_if.receive_complete)
                {
                    ps_llc_ctxt->cb_for_if.receive_complete(
                                        ps_llc_ctxt->cb_for_if.pif_ctxt, 
                                        pHwInfo, pCompInfo);
                }
            }
        } else if (NFCSTATUS_READ_FAILED == pCompInfo->status) {
            // partial read - try reading the length byte again
            ALOGW("LLC length mis-match\n");
            ps_frame_info->recv_error_count = (uint8_t)
                                (ps_frame_info->recv_error_count + 1);
            libnfc_llc_error_count++;

            result = phLlcNfc_Interface_Read(ps_llc_ctxt,
                    PH_LLCNFC_READWAIT_OFF,
                    (uint8_t *)&(ps_recv_pkt->s_llcbuf.llc_length_byte),
                    PH_LLCNFC_BYTES_INIT_READ);
        }
        else
        {
            ALOGW("unknown LLC error2");
            ps_frame_info->recv_error_count = (uint8_t)
                                    (ps_frame_info->recv_error_count + 1);
            libnfc_llc_error_count++;

            phLlcNfc_StopTimers(PH_LLCNFC_GUARDTIMER, 
                                ps_llc_ctxt->s_timerinfo.guard_to_count);
            PH_LLCNFC_DEBUG("Status Error : 0x%x\n", pCompInfo->status);
            if (NULL != ps_llc_ctxt->cb_for_if.receive_complete)
            {
                ps_llc_ctxt->cb_for_if.receive_complete(
                                    ps_llc_ctxt->cb_for_if.pif_ctxt, 
                                    pHwInfo, pCompInfo);
            }
        }
    }
    else
    {
        if ((NULL != ps_llc_ctxt) && (NULL != pCompInfo) 
            && (NULL != ps_llc_ctxt->cb_for_if.receive_complete))
        {
            ps_llc_ctxt->cb_for_if.receive_complete(
                                    ps_llc_ctxt->cb_for_if.pif_ctxt, 
                                    pHwInfo, pCompInfo);
        }
    }

    PH_LLCNFC_PRINT("\n\nLLC : READ RESP CB END\n\n");
}

void 
phLlcNfc_H_SendInfo (
                    phLlcNfc_Context_t          *psLlcCtxt
                    )
{
    phLlcNfc_LlcPacket_t        *ps_recv_pkt = NULL;
    phLlcNfc_Frame_t            *ps_frame_info = NULL;
    phNfc_sTransactionInfo_t    comp_info = {0,0,0,0,0};

    ps_frame_info = &(psLlcCtxt->s_frameinfo);
    ps_recv_pkt = &(ps_frame_info->s_recvpacket);

    if ((ps_recv_pkt->llcbuf_len > 0) && 
        (ps_recv_pkt->llcbuf_len <= PH_LLCNFC_MAX_LLC_PAYLOAD))
    {
        comp_info.status = NFCSTATUS_SUCCESS;
        /* Chop the extra Llc bytes received */
#if 0
        comp_info.length = (ps_recv_pkt->llcbuf_len - 
                            PH_LLCNFC_LEN_APPEND);
#else
        comp_info.length = (uint16_t)psLlcCtxt->recvbuf_length;
#endif /*  */

        if (0 != comp_info.length)
        {
#if 0
            (void)memcpy ((void *)psLlcCtxt->precv_buf, (void *)(
                        ps_recv_pkt->s_llcbuf.sllcpayload.llcpayload), 
                        comp_info.length);
#endif /* #if 0 */
            comp_info.buffer = psLlcCtxt->precv_buf;
        }
        else
        {
            comp_info.buffer = NULL;
        }
    }
    else
    {
        comp_info.status = PHNFCSTVAL(CID_NFC_LLC, 
                                    NFCSTATUS_INVALID_FORMAT);
        comp_info.length = 0;
        comp_info.buffer = NULL;
    }

    (void)phLlcNfc_Interface_Read(psLlcCtxt, 
                        PH_LLCNFC_READWAIT_OFF, 
                        &(ps_recv_pkt->s_llcbuf.llc_length_byte),
                        (uint8_t)PH_LLCNFC_BYTES_INIT_READ);

    if ((NFCSTATUS_SUCCESS == comp_info.status) && 
        (0 == comp_info.length))
    {
        /* May be a NULL I frame received from PN544, so dont do 
            any thing */
    }
    else
    {
        if ((NULL != psLlcCtxt->cb_for_if.receive_complete) && 
            (TRUE == ps_frame_info->upper_recv_call))
        {
            ps_frame_info->upper_recv_call = FALSE;
            psLlcCtxt->cb_for_if.receive_complete(
                                psLlcCtxt->cb_for_if.pif_ctxt, 
                                psLlcCtxt->phwinfo, 
                                &comp_info);
        }
        else
        {
            if (NULL != psLlcCtxt->cb_for_if.notify)
            {
                    psLlcCtxt->cb_for_if.notify(
                            psLlcCtxt->cb_for_if.pif_ctxt, 
                            psLlcCtxt->phwinfo, 
                            NFC_NOTIFY_RECV_COMPLETED, 
                            &comp_info);
            }
        }
    }
    return;
}

