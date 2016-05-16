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
* \file  phLlcNfc.c
* \brief Common LLC for the upper layer.
*
* Project: NFC-FRI-1.1
*
* $Date: Wed Apr 28 17:07:03 2010 $
* $Author: ing02260 $
* $Revision: 1.59 $
* $Aliases: NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
*
*/

/*************************** Includes *******************************/
#include <phNfcTypes.h>
#include <phNfcStatus.h>
#include <phOsalNfc.h>
#include <phNfcInterface.h>
#include <phLlcNfc_DataTypes.h>
#include <phLlcNfc.h>
#include <phLlcNfc_Frame.h>
#include <phLlcNfc_Interface.h>
#include <phLlcNfc_Timer.h>

/*********************** End of includes ****************************/

/***************************** Macros *******************************/

/************************ End of macros *****************************/

/***************************** Global variables *******************************/

#ifdef LLC_RELEASE_FLAG
    uint8_t             g_release_flag;
#endif /* #ifdef LLC_RELEASE_FLAG */

/************************ End of global variables *****************************/



/*********************** Local functions ****************************/
/**
* \ingroup grp_hal_nfc_llc
*
* \brief \b Init function
*
* \copydoc page_reg Initialise all variables of the LLC component (Asynchronous function).
*
* \param[in] pContext          LLC context provided by the upper layer. The LLC 
*                              context will be given to the upper layer through the
*                              \ref phLlcNfc_Register function
* \param[in] pLinkInfo         Link information of the hardware 
*
* \retval NFCSTATUS_PENDING            If the command is yet to be processed.
* \retval NFCSTATUS_INVALID_PARAMETER  At least one parameter of the function is invalid.
* \retval Other errors                 Errors related to the lower layers
*
*/
static 
NFCSTATUS 
phLlcNfc_Init (
               void    *pContext, 
               void    *pLinkInfo
               );

/**
* \ingroup grp_hal_nfc_llc
*
* \brief \b Send function
*
* \copydoc page_reg This asynchronous function gets the information from the upper layer and creates the 
*              proper LLC packet to send the information it to the hardware. The number of 
*              bytes written is obtained from the send response callback which has been 
*              registered in \ref phLlcNfc_Register function
*
* \param[in] pContext          LLC context is provided by the upper layer. The LLC 
*                              context earlier was given to the upper layer through the
*                              \ref phLlcNfc_Register function
* \param[in] pLinkInfo         Link information of the hardware.
* \param[in] pLlc_Buf          The information given by the upper layer to send it to 
*                              the lower layer
* \param[in] llcBufLength      the length of pLlc_Buf, that needs to be sent to the 
*                              lower layer is given by the upper layer
*
* \retval NFCSTATUS_PENDING                If the command is yet to be process.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
* \retval Other errors                     Errors related to the lower layers
*
*/
static 
NFCSTATUS 
phLlcNfc_Send ( 
               void            *pContext, 
               void            *pLinkInfo, 
               uint8_t         *pLlcBuf, 
               uint16_t        llcBufLength
               );

/**
* \ingroup grp_hal_nfc_llc
*
* \brief \b Receive function
*
* \copydoc page_reg This asynchronous function gets the length and the required buffer from
*          the upper layer to receive the information from the the hardware. The 
*          received data will be given through the receive response callback 
*          which has been registered in the \b phLlcNfc_Register function
*
* \param[in] pContext          LLC context is provided by the upper layer. The LLC 
*                              context earlier was given to the upper layer through the
*                              \b phLlcNfc_Register function
* \param[in] pLinkInfo         Link information of the hardware
* \param[in] pLlc_Buf          The information given by the upper layer to receive data from
*                              the lower layer
* \param[in] llcBufLength      The length of pLlc_Buf given by the upper layer
*
* \retval NFCSTATUS_PENDING                If the command is yet to be process.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
* \retval Other errors                     Errors related to the lower layers
*
*/
static 
NFCSTATUS 
phLlcNfc_Receive (  
                  void              *pContext, 
                  void              *pLinkInfo, 
                  uint8_t           *pLlcBuf, 
                  uint16_t          llcBufLength
                  );
/******************** End of Local functions ************************/

/********************** Global variables ****************************/

/******************** End of Global Variables ***********************/

NFCSTATUS 
phLlcNfc_Register (
    phNfcIF_sReference_t        *psReference,
    phNfcIF_sCallBack_t         if_callback,
    void                        *psIFConfig
)
{
    NFCSTATUS               result = NFCSTATUS_SUCCESS;
    phLlcNfc_Context_t      *ps_llc_ctxt = NULL;
    phNfcLayer_sCfg_t       *psconfig = (phNfcLayer_sCfg_t *)psIFConfig;
    
    PH_LLCNFC_PRINT("Llc Register called\n");
    if ((NULL == psReference) || (NULL == psIFConfig) || 
        (NULL == psReference->plower_if) ||
#if 0
        (NULL == if_callback.pif_ctxt) || 
#endif
        (NULL == if_callback.notify) ||
        (NULL == if_callback.receive_complete) ||  
        (NULL == if_callback.send_complete))
    {
        result = PHNFCSTVAL(CID_NFC_LLC, 
                            NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {        
        /* Now LLC is in RESET state */
        ps_llc_ctxt = (phLlcNfc_Context_t*)phOsalNfc_GetMemory(
                                        sizeof(phLlcNfc_Context_t));
        if (NULL == ps_llc_ctxt)
        {
            /* Memory allocation failed */
            result = PHNFCSTVAL(CID_NFC_LLC, 
                                NFCSTATUS_INSUFFICIENT_RESOURCES);
        }
        else 
        {
            result = NFCSTATUS_SUCCESS;

            (void)memset(ps_llc_ctxt, 0, sizeof(phLlcNfc_Context_t));

            /* Register the LLC functions to the upper layer */
            psReference->plower_if->init = (pphNfcIF_Interface_t)&phLlcNfc_Init;
            psReference->plower_if->release = (pphNfcIF_Interface_t)&phLlcNfc_Release;
            psReference->plower_if->send = (pphNfcIF_Transact_t)&phLlcNfc_Send;
            psReference->plower_if->receive = (pphNfcIF_Transact_t)&phLlcNfc_Receive;
            /* Copy the LLC context to the upper layer */
            psReference->plower_if->pcontext = ps_llc_ctxt;

            /* Register the callback function from the upper layer */
            ps_llc_ctxt->cb_for_if.receive_complete = if_callback.receive_complete;
            ps_llc_ctxt->cb_for_if.send_complete = if_callback.send_complete;
            ps_llc_ctxt->cb_for_if.notify = if_callback.notify;
            /* Get the upper layer context */
            ps_llc_ctxt->cb_for_if.pif_ctxt = if_callback.pif_ctxt;

            result = phLlcNfc_Interface_Register(ps_llc_ctxt, psconfig);

            if (NFCSTATUS_SUCCESS == result)
            {
#ifdef LLC_RELEASE_FLAG
                g_release_flag = FALSE;
#endif /* #ifdef LLC_RELEASE_FLAG */
            }
        }
    }
    PH_LLCNFC_DEBUG("Llc Register result : 0x%x\n", result);
    return result;
}

static
NFCSTATUS 
phLlcNfc_Init (
    void    *pContext, 
    void    *pLinkInfo
)
{
    NFCSTATUS               result = NFCSTATUS_SUCCESS;
    phLlcNfc_Context_t      *ps_llc_ctxt = (phLlcNfc_Context_t*)pContext;
    phLlcNfc_LlcPacket_t    s_packet_info;
    
    PH_LLCNFC_PRINT("Llc Init called\n");
    if ((NULL == ps_llc_ctxt) || (NULL == pLinkInfo))
    {
        result = PHNFCSTVAL(CID_NFC_LLC, 
                            NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        /* Initialisation */
        ps_llc_ctxt->phwinfo = pLinkInfo;
        /* Call the internal frame initialise */
        phLlcNfc_H_Frame_Init(ps_llc_ctxt);
        /* Call the internal LLC TL interface initialise */
        result = phLlcNfc_Interface_Init(ps_llc_ctxt);
        if (NFCSTATUS_SUCCESS == result)
        {
            /* Call the internal LLC timer initialise */
            result = phLlcNfc_TimerInit(ps_llc_ctxt);
        }
        
        if (NFCSTATUS_SUCCESS == result)
        {
            /* Create the static timer */
            phLlcNfc_CreateTimers();               

            /* Create a U frame */
            result = phLlcNfc_H_CreateUFramePayload(ps_llc_ctxt,
                                    &s_packet_info,
                                    &(s_packet_info.llcbuf_len),
                                    phLlcNfc_e_rset);
        }
        if (NFCSTATUS_SUCCESS == result)
        {
            /* Call DAL write */
            result = phLlcNfc_Interface_Write(ps_llc_ctxt, 
                                (uint8_t*)&(s_packet_info.s_llcbuf),
                                (uint32_t)s_packet_info.llcbuf_len);
        }
        if (NFCSTATUS_PENDING == result)
        {
            /* Start the timer */
            result = phLlcNfc_StartTimers(PH_LLCNFC_CONNECTIONTIMER, 0);
            if (NFCSTATUS_SUCCESS == result)
            {
                ps_llc_ctxt->s_frameinfo.sent_frame_type = 
                                                        init_u_rset_frame;
                result = NFCSTATUS_PENDING;
            }
        }
    
        if (NFCSTATUS_PENDING != result)
        {
            (void)phLlcNfc_Release(ps_llc_ctxt, pLinkInfo);
        }
    }
    PH_LLCNFC_DEBUG("Llc Init result : 0x%x\n", result);
    return result;
}

NFCSTATUS 
phLlcNfc_Release(
    void    *pContext, 
    void    *pLinkInfo
)
{
    NFCSTATUS               result = PHNFCSTVAL(CID_NFC_LLC, 
                                            NFCSTATUS_INVALID_PARAMETER);
    phLlcNfc_Context_t     *ps_llc_ctxt = (phLlcNfc_Context_t*)pContext;
    /* 
        1. Free all the memory allocated in initialise
    */
    PH_LLCNFC_PRINT("Llc release called\n");

    if ((NULL != ps_llc_ctxt) && (NULL != pLinkInfo))
    {
        result = NFCSTATUS_SUCCESS;
        ps_llc_ctxt->phwinfo = pLinkInfo;
#ifdef INCLUDE_DALINIT_DEINIT   
        if (NULL != ps_llc_ctxt->lower_if.release)
        {
            result = ps_llc_ctxt->lower_if.release(
                            ps_llc_ctxt->lower_if.pcontext, 
                            pLinkInfo);
        }
#endif
        if (NULL != ps_llc_ctxt->lower_if.transact_abort)
        {
            result = ps_llc_ctxt->lower_if.transact_abort(
                            ps_llc_ctxt->lower_if.pcontext, 
                            pLinkInfo);
        }
        if (NULL != ps_llc_ctxt->lower_if.unregister)
        {
            result = ps_llc_ctxt->lower_if.unregister(
                            ps_llc_ctxt->lower_if.pcontext, 
                            pLinkInfo);
        }
        
        /* Call the internal LLC timer un-initialise */
        phLlcNfc_TimerUnInit(ps_llc_ctxt);
        phLlcNfc_H_Frame_DeInit(&ps_llc_ctxt->s_frameinfo);
        (void)memset(ps_llc_ctxt, 0, sizeof(phLlcNfc_Context_t));
        phOsalNfc_FreeMemory(ps_llc_ctxt);
        ps_llc_ctxt = NULL;

#ifdef LLC_RELEASE_FLAG
        g_release_flag = TRUE;
#endif /* #ifdef LLC_RELEASE_FLAG */

    }
    PH_LLCNFC_DEBUG("Llc release result : 0x%04X\n", result);
    return result;
}

static
NFCSTATUS 
phLlcNfc_Send ( 
    void            *pContext, 
    void            *pLinkInfo, 
    uint8_t         *pLlcBuf, 
    uint16_t        llcBufLength
)
{
    /*
        1. Check the function parameters for valid values
        2. Create the I frame llc payload using the upper layer buffer
        3. Send the updated buffer to the below layer
        4. Store the I frame in a list, till acknowledge is received
    */
    NFCSTATUS               result = NFCSTATUS_SUCCESS;
    phLlcNfc_Context_t      *ps_llc_ctxt = (phLlcNfc_Context_t*)pContext;
    phLlcNfc_Frame_t        *ps_frame_info = NULL;
    phLlcNfc_LlcPacket_t    s_packet_info;
    phLlcNfc_StoreIFrame_t  *ps_store_frame = NULL;
#if 0
    uint8_t                 count = 1;
#endif /* #if 0 */

    PH_LLCNFC_PRINT ("Llc Send called\n");
    if ((NULL == ps_llc_ctxt) || (NULL == pLinkInfo) ||  
        (NULL == pLlcBuf) || (0 == llcBufLength) ||  
        (llcBufLength > PH_LLCNFC_MAX_IFRAME_BUFLEN))
    {
        /* Parameter check failed */
        result = PHNFCSTVAL(CID_NFC_LLC, 
                            NFCSTATUS_INVALID_PARAMETER);
    }
    else if (ps_llc_ctxt->s_frameinfo.s_send_store.winsize_cnt >= 
            ps_llc_ctxt->s_frameinfo.window_size)
    {
        /* Window size check failed */
        result = PHNFCSTVAL(CID_NFC_LLC, 
                            NFCSTATUS_NOT_ALLOWED);
    }
    else
    {
        ps_frame_info = &(ps_llc_ctxt->s_frameinfo);
        ps_store_frame = &(ps_frame_info->s_send_store);

        PH_LLCNFC_DEBUG ("Buffer length : 0x%04X\n", llcBufLength);
        PH_LLCNFC_PRINT_BUFFER (pLlcBuf, llcBufLength);
        
        /* Copy the hardware information */
        ps_llc_ctxt->phwinfo = pLinkInfo;

        /* Create I frame with the user buffer  */
        (void)phLlcNfc_H_CreateIFramePayload (
                                &(ps_llc_ctxt->s_frameinfo), 
                                &s_packet_info, 
                                pLlcBuf, (uint8_t)llcBufLength);


        /* Store the I frame in the send list */
        (void)phLlcNfc_H_StoreIFrame (ps_store_frame, s_packet_info);
        result = NFCSTATUS_PENDING;

#ifdef CTRL_WIN_SIZE_COUNT

        /* No check required */
        if ((TRUE != ps_frame_info->write_pending) && 
            (PH_LLCNFC_READPEND_REMAIN_BYTE != 
            ps_frame_info->read_pending))

#else /* #ifdef CTRL_WIN_SIZE_COUNT */
        
        if (1 == ps_frame_info->s_send_store.winsize_cnt)

#endif /* #ifdef CTRL_WIN_SIZE_COUNT */
        {
            /* Call write to the below layer, only if previous write 
                is completed */
            result = phLlcNfc_Interface_Write (ps_llc_ctxt, 
                                (uint8_t *)&(s_packet_info.s_llcbuf),
                                (uint32_t)s_packet_info.llcbuf_len);

            if ((NFCSTATUS_PENDING == result) || 
                (NFCSTATUS_BUSY == PHNFCSTATUS (result)))
            {
                ps_frame_info->write_status = result;
                if (NFCSTATUS_BUSY == PHNFCSTATUS(result))
                {
                    result = NFCSTATUS_PENDING;
                    ps_frame_info->write_wait_call = (phLlcNfc_eSentFrameType_t)
                            ((resend_i_frame == ps_frame_info->write_wait_call) ? 
                            ps_frame_info->write_wait_call : user_i_frame);
                }
                else
                {
                    /* Start the timer */
                    (void)phLlcNfc_StartTimers (PH_LLCNFC_GUARDTIMER, 
                                                ps_frame_info->n_s);
                    
                    /* "sent_frame_type is updated" only if the data is 
                        written to the lower layer */
                    ps_frame_info->sent_frame_type = user_i_frame;
                }
            }
#if 0
            /* Get the added frame array count */
            count = (uint8_t)((((ps_store_frame->start_pos + 
                            ps_store_frame->winsize_cnt) - count)) % 
                            PH_LLCNFC_MOD_NS_NR);
#endif /* #if 0 */
        }
        else
        {
            ps_frame_info->write_status = PHNFCSTVAL(CID_NFC_LLC, NFCSTATUS_BUSY);
            ps_frame_info->write_wait_call = (phLlcNfc_eSentFrameType_t)
                            ((resend_i_frame == ps_frame_info->write_wait_call) ? 
                            ps_frame_info->write_wait_call : user_i_frame);
        }
    }

    
    PH_LLCNFC_DEBUG ("Llc Send result : 0x%04X\n", result);
    return result;
}

static
NFCSTATUS 
phLlcNfc_Receive (  
    void            *pContext, 
    void            *pLinkInfo, 
    uint8_t         *pLlcBuf, 
    uint16_t        llcBufLength
)
{
    NFCSTATUS               result = NFCSTATUS_SUCCESS;
    phLlcNfc_Context_t      *ps_llc_ctxt = (phLlcNfc_Context_t*)pContext;
    phLlcNfc_LlcPacket_t    *ps_recv_pkt = NULL;

    PH_LLCNFC_PRINT("Llc Receive called\n");
    if ((NULL == ps_llc_ctxt) || (NULL == pLinkInfo) || 
        (NULL == pLlcBuf) || (0 == llcBufLength) || 
        (llcBufLength > PH_LLCNFC_MAX_IFRAME_BUFLEN))
    {
        result = PHNFCSTVAL(CID_NFC_LLC, 
                            NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        ps_llc_ctxt->phwinfo = pLinkInfo;
        
        ps_recv_pkt = &(ps_llc_ctxt->s_frameinfo.s_recvpacket);
        /* Always read the first byte to read the length, then 
        read the entire data later using the 1 byte buffer */
        llcBufLength = PH_LLCNFC_BYTES_INIT_READ;
        /* Call write to the below layer */
        result = phLlcNfc_Interface_Read(ps_llc_ctxt, 
                                PH_LLCNFC_READWAIT_OFF, 
                                &(ps_recv_pkt->s_llcbuf.llc_length_byte), 
                                llcBufLength);

        ps_llc_ctxt->s_frameinfo.upper_recv_call = TRUE;
        if (NFCSTATUS_PENDING != result)
        {
            ps_llc_ctxt->state = phLlcNfc_Initialised_State;
        }
    }
    PH_LLCNFC_DEBUG("Llc Receive result : 0x%04X\n", result);
    return result;
}
