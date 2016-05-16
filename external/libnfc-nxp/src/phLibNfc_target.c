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
 * \file phLibNfc_Target.c

 * Project: NFC FRI 1.1
 *
 * $Date: Thu Oct 15 15:24:43 2009 $
 * $Author: ing07299 $
 * $Revision: 1.12 $
 * $Aliases: NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK944_SDK,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK949_SDK_INT,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK1003_SDK,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1008_SDK,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1007_SDK,NFC_FRI1.1_WK1014_SDK,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1019_SDK,NFC_FRI1.1_WK1024_SDK $
 *
 */

/*
************************* Header Files ***************************************
*/

#include <phLibNfcStatus.h>
#include <phLibNfc.h>
#include <phHal4Nfc.h>
#include <phOsalNfc.h>
#include <phLibNfc_Internal.h>
#include <phLibNfc_ndef_raw.h>
#include <phLibNfc_initiator.h>
#include <phLibNfc_discovery.h>

/*
*************************** Macro's  ****************************************
*/

#ifndef STATIC_DISABLE
#define STATIC static
#else
//#undef STATIC
#define STATIC 
#endif
/*
*************************** Global Variables **********************************
*/

/*
*************************** Static Function Declaration ***********************
*/

/* Remote device receive callback */
STATIC void phLibNfc_RemoteDev_Receive_Cb(
                                void            *context,
                                phNfc_sData_t   *rec_rsp_data, 
                                NFCSTATUS       status
                                );

/* Remote device Send callback */
STATIC void phLibNfc_RemoteDev_Send_Cb(
                                void        *Context,
                                NFCSTATUS   status
                                );

/*
*************************** Function Definitions ******************************
*/

/**
* Interface used to receive data from initiator at target side during P2P
* communication.
*/
NFCSTATUS phLibNfc_RemoteDev_Receive(phLibNfc_Handle       hRemoteDevice,  
                                pphLibNfc_Receive_RspCb_t  pReceiveRspCb,  
                                void                       *pContext
                                ) 
{
    NFCSTATUS RetVal = NFCSTATUS_FAILED;
    /*Check Lib Nfc is initialized*/
    if((NULL == gpphLibContext)|| 
        (gpphLibContext->LibNfcState.cur_state == eLibNfcHalStateShutdown))
    {
        RetVal = NFCSTATUS_NOT_INITIALISED;
    }/*Check application has sent valid parameters*/
    else if (gpphLibContext->LibNfcState.cur_state == eLibNfcHalStateRelease)
    {
        RetVal = NFCSTATUS_DESELECTED;
    }
    else if((NULL == pReceiveRspCb)
        || (NULL == pContext)
        || (0 == hRemoteDevice))
    {
        RetVal= NFCSTATUS_INVALID_PARAMETER;
    }   
    else if(gpphLibContext->LibNfcState.next_state == eLibNfcHalStateShutdown)
    {
        RetVal = NFCSTATUS_SHUTDOWN;
    }
    else if((TRUE == gpphLibContext->status.GenCb_pending_status)       
        ||(NULL!=gpphLibContext->sNfcIp_Context.pClientNfcIpRxCb)
        ||(phHal_eNfcIP1_Target==
        ((phHal_sRemoteDevInformation_t*)hRemoteDevice)->RemDevType))
    {
        /*Previous callback is pending or if initiator uses this api */
        RetVal = NFCSTATUS_REJECTED;
    }/*check for Discovered initiator handle and handle sent by application */
    else if(gpphLibContext->sNfcIp_Context.Rem_Initiator_Handle != hRemoteDevice)
    {
        RetVal= NFCSTATUS_INVALID_DEVICE;
    }
#ifdef LLCP_TRANSACT_CHANGES
    else if ((LLCP_STATE_RESET_INIT != gpphLibContext->llcp_cntx.sLlcpContext.state)
            && (LLCP_STATE_CHECKED != gpphLibContext->llcp_cntx.sLlcpContext.state))
    {
        RetVal = NFCSTATUS_BUSY;
    }
#endif /* #ifdef LLCP_TRANSACT_CHANGES */
    else
    {
        if(eLibNfcHalStatePresenceChk ==
                gpphLibContext->LibNfcState.next_state)
        {
            gpphLibContext->sNfcIp_Context.pClientNfcIpRxCb = NULL;
            RetVal = NFCSTATUS_PENDING;
        }
        else
        {
            /*Call below layer receive and register the callback with it*/
            PHDBG_INFO("LibNfc:P2P Receive In Progress");
            RetVal =phHal4Nfc_Receive(                                          
                            gpphLibContext->psHwReference,
                            (phHal4Nfc_TransactInfo_t*)gpphLibContext->psTransInfo,
                            (pphLibNfc_Receive_RspCb_t)
                            phLibNfc_RemoteDev_Receive_Cb,
                            (void *)gpphLibContext
                            );  
        }   
        if(NFCSTATUS_PENDING == RetVal)
        {
            /*Update the Next state as Transaction*/
            gpphLibContext->sNfcIp_Context.pClientNfcIpRxCb= pReceiveRspCb;
            gpphLibContext->sNfcIp_Context.pClientNfcIpRxCntx = pContext;
            gpphLibContext->status.GenCb_pending_status=TRUE;
            gpphLibContext->LibNfcState.next_state = eLibNfcHalStateTransaction;
        }
        else
        {
            RetVal = NFCSTATUS_FAILED;
        }       
    }
    return RetVal;
}
/**
* Response callback for Remote Device Receive.
*/
STATIC void phLibNfc_RemoteDev_Receive_Cb(
                                    void            *context,
                                    phNfc_sData_t   *rec_rsp_data, 
                                    NFCSTATUS       status
                                    )
{
    pphLibNfc_Receive_RspCb_t       pClientCb=NULL;
    
    phLibNfc_LibContext_t   *pLibNfc_Ctxt = (phLibNfc_LibContext_t *)context;
    void                    *pUpperLayerContext=NULL;

    /* Check for the context returned by below layer */
    if(pLibNfc_Ctxt != gpphLibContext)
    {   /*wrong context returned*/
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
    }
    else
    {
        pClientCb = gpphLibContext->sNfcIp_Context.pClientNfcIpRxCb;
        pUpperLayerContext = gpphLibContext->sNfcIp_Context.pClientNfcIpRxCntx;

        gpphLibContext->sNfcIp_Context.pClientNfcIpRxCb = NULL;
        gpphLibContext->sNfcIp_Context.pClientNfcIpRxCntx = NULL;
        gpphLibContext->status.GenCb_pending_status = FALSE;
        if(eLibNfcHalStateShutdown == gpphLibContext->LibNfcState.next_state)
        {   /*shutdown called before completion of P2P receive allow
              shutdown to happen */
            phLibNfc_Pending_Shutdown();
            status = NFCSTATUS_SHUTDOWN;    
        }
        else if(eLibNfcHalStateRelease == gpphLibContext->LibNfcState.next_state)
        {
            status = NFCSTATUS_ABORTED;
        }
        else
        {
            if((NFCSTATUS_SUCCESS != status) && 
                (PHNFCSTATUS(status) != NFCSTATUS_MORE_INFORMATION ) )
            {
                /*During p2p receive operation initiator was removed
                from RF field of target*/
                status = NFCSTATUS_DESELECTED;
            }
            else
            {
                status = NFCSTATUS_SUCCESS;
            }
        }   
        /* Update current state */
        phLibNfc_UpdateCurState(status,gpphLibContext);
               
        if (NULL != pClientCb)
        {
            /*Notify to upper layer status and No. of bytes
             actually received */
            pClientCb(pUpperLayerContext, rec_rsp_data, status);          
        }
    }
    return;
}

/**
* Interface used to send data from target to initiator during P2P communication
*/
NFCSTATUS 
phLibNfc_RemoteDev_Send(
                        phLibNfc_Handle      hRemoteDevice,  
                        phNfc_sData_t *      pTransferData,  
                        pphLibNfc_RspCb_t    pSendRspCb,  
                        void                 *pContext
                        )
{
    NFCSTATUS RetVal = NFCSTATUS_FAILED;
    /*Check Lib Nfc stack is initilized*/
    if((NULL == gpphLibContext)|| 
        (gpphLibContext->LibNfcState.cur_state == eLibNfcHalStateShutdown))
    {
        RetVal = NFCSTATUS_NOT_INITIALISED;
    }
    else if (gpphLibContext->LibNfcState.cur_state == eLibNfcHalStateRelease)
    {
        RetVal = NFCSTATUS_DESELECTED;
    }
    /*Check application has sent the valid parameters*/
    else if((NULL == pTransferData)
        || (NULL == pSendRspCb)
        || (NULL == pTransferData->buffer)
        || (0 == pTransferData->length)
        || (NULL == pContext)
        || (0 == hRemoteDevice))
    {
        RetVal= NFCSTATUS_INVALID_PARAMETER;
    }   
    else if(gpphLibContext->LibNfcState.next_state == eLibNfcHalStateShutdown)
    {
        RetVal = NFCSTATUS_SHUTDOWN;
    }
	else if((TRUE == gpphLibContext->status.GenCb_pending_status)       
        ||(NULL!=gpphLibContext->sNfcIp_Context.pClientNfcIpRxCb)
        ||(phHal_eNfcIP1_Target==
        ((phHal_sRemoteDevInformation_t*)hRemoteDevice)->RemDevType))
    {
        /*Previous callback is pending or local device is Initiator
        then don't allow */
        RetVal = NFCSTATUS_REJECTED;
    }/*Check for Discovered initiator handle and handle sent by application */
    else if(gpphLibContext->sNfcIp_Context.Rem_Initiator_Handle != hRemoteDevice)
    {
        RetVal= NFCSTATUS_INVALID_DEVICE;
    }
    else if((NULL!=gpphLibContext->sNfcIp_Context.pClientNfcIpTxCb))
    {
        RetVal =NFCSTATUS_BUSY ;
    }
#ifdef LLCP_TRANSACT_CHANGES
    else if ((LLCP_STATE_RESET_INIT != gpphLibContext->llcp_cntx.sLlcpContext.state)
            && (LLCP_STATE_CHECKED != gpphLibContext->llcp_cntx.sLlcpContext.state))
    {
        RetVal= NFCSTATUS_BUSY;
    }
#endif /* #ifdef LLCP_TRANSACT_CHANGES */
    else
    {
        if(eLibNfcHalStatePresenceChk ==
                gpphLibContext->LibNfcState.next_state)
        {
            gpphLibContext->sNfcIp_Context.pClientNfcIpTxCb = NULL;
            RetVal = NFCSTATUS_PENDING;
        }
        else
        {
            if(gpphLibContext->psTransInfo!=NULL)
            {
                (void)memset(gpphLibContext->psTransInfo,
                                0,
                                sizeof(phLibNfc_sTransceiveInfo_t));                
            
                gpphLibContext->psTransInfo->addr =UNKNOWN_BLOCK_ADDRESS;
                /*pointer to send data */
                gpphLibContext->psTransInfo->sSendData.buffer = 
                                                    pTransferData->buffer;   
                /*size of send data*/
                gpphLibContext->psTransInfo->sSendData.length = 
                                                    pTransferData->length;   

                /* Copy remote device type */
                gpphLibContext->sNfcIp_Context.TransactInfoRole.remotePCDType =
                    ((phHal_sRemoteDevInformation_t*)hRemoteDevice)->RemDevType;
                /*Call Hal4 Send API and register callback with it*/
                PHDBG_INFO("LibNfc:P2P send In Progress");
                RetVal= phHal4Nfc_Send(                                          
                                gpphLibContext->psHwReference,
                                &(gpphLibContext->sNfcIp_Context.TransactInfoRole),
                                gpphLibContext->psTransInfo->sSendData,
                                (pphLibNfc_RspCb_t)
                                phLibNfc_RemoteDev_Send_Cb,
                                (void *)gpphLibContext
                                ); 
            }
        }
        if(NFCSTATUS_PENDING == RetVal)
        {
            /* Update next state to transaction */
            gpphLibContext->sNfcIp_Context.pClientNfcIpTxCb= pSendRspCb;
            gpphLibContext->sNfcIp_Context.pClientNfcIpTxCntx = pContext;
            gpphLibContext->status.GenCb_pending_status=TRUE;
            gpphLibContext->LibNfcState.next_state = eLibNfcHalStateTransaction;
        }
        else
        {
            RetVal = NFCSTATUS_FAILED;
        }
    }
    return RetVal;
}

/*
* Response callback for Remote Device Send.
*/
STATIC void phLibNfc_RemoteDev_Send_Cb(
                            void        *Context,
                            NFCSTATUS   status
                            )
{
    pphLibNfc_RspCb_t       pClientCb=NULL;
    phLibNfc_LibContext_t   *pLibNfc_Ctxt = (phLibNfc_LibContext_t *)Context;
    void                    *pUpperLayerContext=NULL;

    /* Check for the context returned by below layer */
    if(pLibNfc_Ctxt != gpphLibContext)
    {   /*wrong context returned*/
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
    }
    else
    {
        if(eLibNfcHalStateShutdown == gpphLibContext->LibNfcState.next_state)
        {   /*shutdown called before completion p2p send allow
              shutdown to happen */
            phLibNfc_Pending_Shutdown();
            status = NFCSTATUS_SHUTDOWN;    
        }
        else if(eLibNfcHalStateRelease == gpphLibContext->LibNfcState.next_state)
        {
            status = NFCSTATUS_ABORTED;
        }
        else
        {
            gpphLibContext->status.GenCb_pending_status = FALSE;
            if((NFCSTATUS_SUCCESS != status) && 
                (PHNFCSTATUS(status) != NFCSTATUS_MORE_INFORMATION ) )
            {
                /*During p2p send operation initator was not present in RF
                field of target*/
                status = NFCSTATUS_DESELECTED;
            }
            else
            {
                status = NFCSTATUS_SUCCESS;
            }
        }
        /* Update current state */
        phLibNfc_UpdateCurState(status,gpphLibContext);

        pClientCb = gpphLibContext->sNfcIp_Context.pClientNfcIpTxCb;
        pUpperLayerContext = gpphLibContext->sNfcIp_Context.pClientNfcIpTxCntx;

        gpphLibContext->sNfcIp_Context.pClientNfcIpTxCb = NULL;
        gpphLibContext->sNfcIp_Context.pClientNfcIpTxCntx = NULL;
        if (NULL != pClientCb)
        {
            /* Notify to upper layer status and No. of bytes
             actually written or send to initiator */
            pClientCb(pUpperLayerContext, status);          
        }
    }
    return;
}




