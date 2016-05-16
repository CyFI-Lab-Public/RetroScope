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
 * \file  phHal4Nfc_P2P.c
 * \brief Hal4Nfc_P2P source.
 *
 * Project: NFC-FRI 1.1
 *
 * $Date: Mon May 31 11:43:43 2010 $
 * $Author: ing07385 $
 * $Revision: 1.56 $
 * $Aliases: NFC_FRI1.1_WK1023_R35_1 $
 *
 */

/* ---------------------------Include files ------------------------------------*/
#include <phHal4Nfc.h>
#include <phHal4Nfc_Internal.h>
#include <phOsalNfc.h>
#include <phOsalNfc_Timer.h>
#include <phHciNfc.h>
#include <phNfcConfig.h>

/* ------------------------------- Macros ------------------------------------*/

#ifdef _WIN32
/*Timeout value for recv data timer for P2P.This timer is used for creating 
  Asynchronous behavior in the scenario where the data is received even before 
  the upper layer calls the phHal4Nfc_receive().*/
#define     PH_HAL4NFC_RECV_CB_TIMEOUT       100U
#else
#define     PH_HAL4NFC_RECV_CB_TIMEOUT      0x00U
#endif/*#ifdef _WIN32*/


/* --------------------Structures and enumerations --------------------------*/

/*timer callback to send already buffered receive data to upper layer*/
static void phHal4Nfc_P2PRecvTimerCb(uint32_t P2PRecvTimerId, void *pContext);

/* ---------------------- Function definitions ------------------------------*/

/*  Transfer the user data to another NfcIP device from the host. 
 *  pTransferCallback is called, when all steps in the transfer sequence are 
 *  completed.*/
NFCSTATUS 
phHal4Nfc_Send(            
                phHal_sHwReference_t                    *psHwReference,
                phHal4Nfc_TransactInfo_t                *psTransferInfo,
                phNfc_sData_t                            sTransferData,
                pphHal4Nfc_SendCallback_t                pSendCallback,                
                void                                    *pContext               
                )
{
    NFCSTATUS RetStatus = NFCSTATUS_PENDING;
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL;

    /*NULL checks*/
    if((NULL == psHwReference) 
        ||( NULL == pSendCallback )
        || (NULL == psTransferInfo)
        )
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_INVALID_PARAMETER);
    }
    /*Check initialised state*/
    else if((NULL == psHwReference->hal_context)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4CurrentState 
                                               < eHal4StateOpenAndReady)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4NextState 
                                               == eHal4StateClosed))
    {
        RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_NOT_INITIALISED);     
    }  
    /*Only NfcIp1 Target can call this API*/
    else if(phHal_eNfcIP1_Initiator != psTransferInfo->remotePCDType)
    {
        RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_INVALID_DEVICE);
    }
    else
    { 
        Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)psHwReference->hal_context;
        if(NULL == Hal4Ctxt->psTrcvCtxtInfo)
        {
            RetStatus= PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_FAILED);
        }
        /*Check Activated*/
        else if(NFC_EVT_ACTIVATED == Hal4Ctxt->sTgtConnectInfo.EmulationState)
        {
            Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt = pContext;     
            /*Register upper layer callback*/
            Hal4Ctxt->psTrcvCtxtInfo->pP2PSendCb  = pSendCallback;      
            PHDBG_INFO("NfcIP1 Send");
            /*allocate buffer to store senddata received from upper layer*/
            if (NULL == Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData)
            {
                Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData = (phNfc_sData_t *)
                        phOsalNfc_GetMemory(sizeof(phNfc_sData_t));
                if(NULL != Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData)
                {
                    (void)memset(Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData, 0, 
                                                    sizeof(phNfc_sData_t));
                    Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                                            = PH_OSALNFC_INVALID_TIMER_ID;
                }
            }

            Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData->buffer
                = sTransferData.buffer;
            Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData->length 
                = sTransferData.length;

            /* If data size is less than Peer's Max frame length, then no chaining is required */
            if(Hal4Ctxt->rem_dev_list[0]->RemoteDevInfo.NfcIP_Info.MaxFrameLength >= sTransferData.length)
            {
                Hal4Ctxt->psTrcvCtxtInfo->
                    XchangeInfo.params.nfc_info.more_info = FALSE;
                Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.tx_length
                    = (uint8_t)sTransferData.length;
                Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.tx_buffer
                    = sTransferData.buffer;
            }
            else/*set more_info to true,to indicate more data pending to be sent*/
            {
                Hal4Ctxt->psTrcvCtxtInfo->
                    XchangeInfo.params.nfc_info.more_info = TRUE;
                Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.tx_length
                    = Hal4Ctxt->rem_dev_list[0]->RemoteDevInfo.NfcIP_Info.MaxFrameLength;
                Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.tx_buffer
                    = sTransferData.buffer;
                Hal4Ctxt->psTrcvCtxtInfo->NumberOfBytesSent
                    += Hal4Ctxt->rem_dev_list[0]->RemoteDevInfo.NfcIP_Info.MaxFrameLength;
            }
            PHDBG_INFO("HAL4:Calling Hci_Send_data()");
            RetStatus = phHciNfc_Send_Data (
                Hal4Ctxt->psHciHandle,
                psHwReference,
                NULL,
                &(Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo)
                );
            /*check return status*/
            if (NFCSTATUS_PENDING == RetStatus)
            {
                /*Set P2P_Send_In_Progress to defer any disconnect call until
                 Send complete occurs*/
                Hal4Ctxt->psTrcvCtxtInfo->P2P_Send_In_Progress = TRUE;
                Hal4Ctxt->Hal4NextState = eHal4StateTransaction;
                /*No of bytes remaining for next send*/
                Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData->length
                    -= Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.tx_length;
            }           
        }
        else/*Deactivated*/
        {
            RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_DESELECTED);
        }
    }
    return RetStatus;
}


/*  Transfer the user data to the another NfcIP device from the host. 
 *  pTransferCallback is called, when all steps in the transfer sequence are 
 *  completed.*/

NFCSTATUS 
phHal4Nfc_Receive(                
                  phHal_sHwReference_t                  *psHwReference,
                  phHal4Nfc_TransactInfo_t              *psRecvInfo,
                  pphHal4Nfc_ReceiveCallback_t          pReceiveCallback,
                  void                                  *pContext
                 )
{
    NFCSTATUS RetStatus = NFCSTATUS_PENDING;
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL;
     /*NULL checks*/
    if((NULL == psHwReference) 
        ||( NULL == pReceiveCallback)
        ||( NULL == psRecvInfo))
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_INVALID_PARAMETER);
    }
    /*Check initialised state*/
    else if((NULL == psHwReference->hal_context)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4CurrentState 
                                               < eHal4StateOpenAndReady)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4NextState 
                                               == eHal4StateClosed))
    {
        RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_NOT_INITIALISED);     
    }   
    else
    {
        Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)psHwReference->hal_context;
        if(NFC_EVT_ACTIVATED == Hal4Ctxt->sTgtConnectInfo.EmulationState)
        {
            /*Following condition gets satisfied only on target side,if receive
              is not already called*/
            if(NULL == Hal4Ctxt->psTrcvCtxtInfo)
            {
                Hal4Ctxt->psTrcvCtxtInfo= (pphHal4Nfc_TrcvCtxtInfo_t)
                    phOsalNfc_GetMemory((uint32_t)
                    (sizeof(phHal4Nfc_TrcvCtxtInfo_t)));
                if(NULL != Hal4Ctxt->psTrcvCtxtInfo)
                {
                    (void)memset(Hal4Ctxt->psTrcvCtxtInfo,0,
                        sizeof(phHal4Nfc_TrcvCtxtInfo_t));
                    Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                        = PH_OSALNFC_INVALID_TIMER_ID;
                    Hal4Ctxt->psTrcvCtxtInfo->RecvDataBufferStatus = NFCSTATUS_PENDING;
                }
            }
            if(NULL == Hal4Ctxt->psTrcvCtxtInfo)
            {
                phOsalNfc_RaiseException(phOsalNfc_e_NoMemory,0);
                RetStatus= PHNFCSTVAL(CID_NFC_HAL , 
                    NFCSTATUS_INSUFFICIENT_RESOURCES);
            }
            else /*Store callback & Return status pending*/
            {
                Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt = pContext;     
                /*Register upper layer callback*/
                Hal4Ctxt->psTrcvCtxtInfo->pUpperTranceiveCb = NULL;
                Hal4Ctxt->psTrcvCtxtInfo->pP2PRecvCb = pReceiveCallback;
                if(NFCSTATUS_PENDING != 
                    Hal4Ctxt->psTrcvCtxtInfo->RecvDataBufferStatus)
                {               
                    /**Create a timer to send received data in the callback*/
                    if(Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                        == PH_OSALNFC_INVALID_TIMER_ID)
                    {
                        PHDBG_INFO("HAL4: Transaction Timer Create for Receive");
                        Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId 
                            = phOsalNfc_Timer_Create();
                    }
                    if(Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                        == PH_OSALNFC_INVALID_TIMER_ID)
                    {
                        RetStatus = PHNFCSTVAL(CID_NFC_HAL ,
                            NFCSTATUS_INSUFFICIENT_RESOURCES);                      
                    }
                    else/*start the timer*/
                    {
                        phOsalNfc_Timer_Start(
                            Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId,
                            PH_HAL4NFC_RECV_CB_TIMEOUT,
							 phHal4Nfc_P2PRecvTimerCb,
							 NULL
                            );
                    }
                }
            }
        }
        else/*deactivated*/
        {
            RetStatus= PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_DESELECTED);
        }
    }   
    return RetStatus;
}

/*Timer callback for recv data timer for P2P.This timer is used for creating 
  Asynchronous behavior in the scenario where the data is received even before 
  the upper layer calls the phHal4Nfc_receive().*/
static void phHal4Nfc_P2PRecvTimerCb(uint32_t P2PRecvTimerId, void *pContext)
{
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)(
                                            gpphHal4Nfc_Hwref->hal_context);
    pphHal4Nfc_ReceiveCallback_t pUpperRecvCb = NULL;
    NFCSTATUS RecvDataBufferStatus = NFCSTATUS_PENDING;
	PHNFC_UNUSED_VARIABLE(pContext);

    phOsalNfc_Timer_Stop(P2PRecvTimerId);            
    phOsalNfc_Timer_Delete(P2PRecvTimerId);
    if(NULL != Hal4Ctxt->psTrcvCtxtInfo)
    {
        RecvDataBufferStatus = Hal4Ctxt->psTrcvCtxtInfo->RecvDataBufferStatus;
        Hal4Ctxt->psTrcvCtxtInfo->RecvDataBufferStatus = NFCSTATUS_PENDING;
    
        Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId 
            = PH_OSALNFC_INVALID_TIMER_ID; 
        /*Update state*/
        Hal4Ctxt->Hal4NextState = (eHal4StateTransaction
             == Hal4Ctxt->Hal4NextState?eHal4StateInvalid:Hal4Ctxt->Hal4NextState);
        /*Provide address of received data to upper layer data pointer*/
        Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData 
            = &(Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData);  
        /*Chk NULL and call recv callback*/
        if(Hal4Ctxt->psTrcvCtxtInfo->pP2PRecvCb != NULL)
        {           
            pUpperRecvCb = Hal4Ctxt->psTrcvCtxtInfo->pP2PRecvCb;
            Hal4Ctxt->psTrcvCtxtInfo->pP2PRecvCb = NULL;        
            (*pUpperRecvCb)(
                Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
                Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData,
                RecvDataBufferStatus
                );
        }
    }
    return;
}

/**Send complete handler*/
void phHal4Nfc_SendCompleteHandler(phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,void *pInfo)
{
    pphHal4Nfc_SendCallback_t pUpperSendCb = NULL;
    pphHal4Nfc_TransceiveCallback_t pUpperTrcvCb = NULL;
    NFCSTATUS SendStatus = ((phNfc_sCompletionInfo_t *)pInfo)->status;
    pphHal4Nfc_DiscntCallback_t pUpperDisconnectCb = NULL;
    Hal4Ctxt->psTrcvCtxtInfo->P2P_Send_In_Progress = FALSE; 
    /*Send status Success or Pending disconnect in HAl4*/
    if((SendStatus != NFCSTATUS_SUCCESS)
        ||(NFC_INVALID_RELEASE_TYPE != Hal4Ctxt->sTgtConnectInfo.ReleaseType))
    {   
        Hal4Ctxt->Hal4NextState = eHal4StateInvalid;
        /*Update Status*/
        SendStatus = (NFCSTATUS)(NFC_INVALID_RELEASE_TYPE != 
          Hal4Ctxt->sTgtConnectInfo.ReleaseType?NFCSTATUS_RELEASED:SendStatus);
        /*Callback For Target Send*/
        if(NULL != Hal4Ctxt->psTrcvCtxtInfo->pP2PSendCb)
        {
            pUpperSendCb = Hal4Ctxt->psTrcvCtxtInfo->pP2PSendCb;
            Hal4Ctxt->psTrcvCtxtInfo->pP2PSendCb = NULL;
            (*pUpperSendCb)(
                Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
                SendStatus
                );
        }
        else/*Callback For Initiator Send*/
        {
            if(NULL !=  Hal4Ctxt->psTrcvCtxtInfo->pUpperTranceiveCb)
            {
                Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData->length = 0;
                pUpperTrcvCb = Hal4Ctxt->psTrcvCtxtInfo->pUpperTranceiveCb;
                Hal4Ctxt->psTrcvCtxtInfo->pUpperTranceiveCb = NULL;
                (*pUpperTrcvCb)(
                    Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
                    Hal4Ctxt->sTgtConnectInfo.psConnectedDevice,
                    Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData,
                    SendStatus
                    );
            }
        }
        /*Issue Pending disconnect from HAl4*/
        if(NFC_INVALID_RELEASE_TYPE != Hal4Ctxt->sTgtConnectInfo.ReleaseType)
        {
            SendStatus = phHal4Nfc_Disconnect_Execute(gpphHal4Nfc_Hwref);
            if((NFCSTATUS_PENDING != SendStatus) &&
               (NULL != Hal4Ctxt->sTgtConnectInfo.pUpperDisconnectCb))
            {
                pUpperDisconnectCb = 
                    Hal4Ctxt->sTgtConnectInfo.pUpperDisconnectCb;
                Hal4Ctxt->sTgtConnectInfo.pUpperDisconnectCb = NULL;
                (*pUpperDisconnectCb)(
                    Hal4Ctxt->sUpperLayerInfo.psUpperLayerDisconnectCtxt,
                    Hal4Ctxt->sTgtConnectInfo.psConnectedDevice,
                    SendStatus                            
                    );/*Notify disconnect failed to upper layer*/       
            }
        }           
    }
    else 
    {
        /*More info remaining in send buffer.continue with sending remaining 
          bytes*/
        if(Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData->length
                                            > Hal4Ctxt->rem_dev_list[0]->RemoteDevInfo.NfcIP_Info.MaxFrameLength)
        {           
            /*Set more info*/
            Hal4Ctxt->psTrcvCtxtInfo->
                XchangeInfo.params.nfc_info.more_info = TRUE;
            /*copy to tx_buffer ,remaining bytes.NumberOfBytesSent is the 
              number of bytes already sent from current send buffer.*/
            Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.tx_buffer
                = (Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData->buffer
                   + Hal4Ctxt->psTrcvCtxtInfo->NumberOfBytesSent);
            Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.tx_length
                = Hal4Ctxt->rem_dev_list[0]->RemoteDevInfo.NfcIP_Info.MaxFrameLength;
            Hal4Ctxt->psTrcvCtxtInfo->NumberOfBytesSent
                += Hal4Ctxt->rem_dev_list[0]->RemoteDevInfo.NfcIP_Info.MaxFrameLength;
            Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData->length
                -= Hal4Ctxt->rem_dev_list[0]->RemoteDevInfo.NfcIP_Info.MaxFrameLength;
            PHDBG_INFO("Hal4:Calling Hci_senddata() from sendcompletehandler1");
            SendStatus = phHciNfc_Send_Data (
                Hal4Ctxt->psHciHandle,
                gpphHal4Nfc_Hwref,
                Hal4Ctxt->sTgtConnectInfo.psConnectedDevice,
                &(Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo)
                );
            if(NFCSTATUS_PENDING == SendStatus)
            {
                Hal4Ctxt->psTrcvCtxtInfo->P2P_Send_In_Progress = TRUE;
            }
        }
        /*Remaining bytes is less than PH_HAL4NFC_MAX_SEND_LEN*/
        else if(Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData->length > 0)
        {
            Hal4Ctxt->psTrcvCtxtInfo->
                XchangeInfo.params.nfc_info.more_info = FALSE;
            Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.tx_length
                = (uint8_t)Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData->length;
            Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.tx_buffer
                = (Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData->buffer
                + Hal4Ctxt->psTrcvCtxtInfo->NumberOfBytesSent);
            Hal4Ctxt->psTrcvCtxtInfo->NumberOfBytesSent = 0;
            /*No of bytes remaining for next send*/
            Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData->length = 0;
            PHDBG_INFO("Hal4:Calling Hci_senddata() from sendcompletehandler2");
            SendStatus = phHciNfc_Send_Data (
                Hal4Ctxt->psHciHandle,
                gpphHal4Nfc_Hwref,
                Hal4Ctxt->sTgtConnectInfo.psConnectedDevice,
                &(Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo)
                );
        }
        else/*No more Bytes left.Send complete*/
        {
            Hal4Ctxt->psTrcvCtxtInfo->NumberOfBytesSent = 0;
            /*Callback For Target Send*/
            if(NULL != Hal4Ctxt->psTrcvCtxtInfo->pP2PSendCb)
            {
                pUpperSendCb = Hal4Ctxt->psTrcvCtxtInfo->pP2PSendCb;
                Hal4Ctxt->psTrcvCtxtInfo->pP2PSendCb = NULL;
                (*pUpperSendCb)(
                    Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
                     SendStatus
                    );
            }
            else
            {
                /**Start timer to keep track of transceive timeout*/
#ifdef TRANSACTION_TIMER
                phOsalNfc_Timer_Start(
                    Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId,
                    PH_HAL4NFC_TRANSCEIVE_TIMEOUT,
                    phHal4Nfc_TrcvTimeoutHandler
                    );
#endif /*TRANSACTION_TIMER*/
            }
        }
    }
    return;
}

/**Receive complete handler*/
void phHal4Nfc_RecvCompleteHandler(phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,void *pInfo)
{
    pphHal4Nfc_ReceiveCallback_t pUpperRecvCb = NULL;
    pphHal4Nfc_TransceiveCallback_t pUpperTrcvCb = NULL;
    NFCSTATUS RecvStatus = ((phNfc_sTransactionInfo_t *)pInfo)->status;
    /*allocate TrcvContext if not already allocated.Required since 
     Receive complete can occur before any other send /receive calls.*/
    if(NULL == Hal4Ctxt->psTrcvCtxtInfo)
    {
        Hal4Ctxt->psTrcvCtxtInfo= (pphHal4Nfc_TrcvCtxtInfo_t)
            phOsalNfc_GetMemory((uint32_t)
            (sizeof(phHal4Nfc_TrcvCtxtInfo_t)));
        if(NULL != Hal4Ctxt->psTrcvCtxtInfo)
        {
            (void)memset(Hal4Ctxt->psTrcvCtxtInfo,0,
                sizeof(phHal4Nfc_TrcvCtxtInfo_t));  
            Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                = PH_OSALNFC_INVALID_TIMER_ID;
            Hal4Ctxt->psTrcvCtxtInfo->RecvDataBufferStatus 
                = NFCSTATUS_PENDING;
        }       
    }
    if(NULL == Hal4Ctxt->psTrcvCtxtInfo)
    {
        phOsalNfc_RaiseException(phOsalNfc_e_NoMemory,0);
        RecvStatus = PHNFCSTVAL(CID_NFC_HAL , 
            NFCSTATUS_INSUFFICIENT_RESOURCES);
    }
    else
    {
        /*Allocate 4K buffer to copy the received data into*/
        if(NULL == Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.buffer)
        {
            Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.buffer
                = (uint8_t *)phOsalNfc_GetMemory(
                        PH_HAL4NFC_MAX_RECEIVE_BUFFER
                        );
            if(NULL == Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.buffer)
            {
                phOsalNfc_RaiseException(phOsalNfc_e_NoMemory,
                    0);
                RecvStatus = NFCSTATUS_INSUFFICIENT_RESOURCES;
            }
            else/*memset*/
            {
                (void)memset(
                    Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.buffer,
                    0,
                    PH_HAL4NFC_MAX_RECEIVE_BUFFER
                    );
            }
        }

        if(RecvStatus != NFCSTATUS_INSUFFICIENT_RESOURCES)
        {
            /*Copy the data*/
            (void)memcpy(
                (Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.buffer
                + Hal4Ctxt->psTrcvCtxtInfo->P2PRecvLength),
                ((phNfc_sTransactionInfo_t *)pInfo)->buffer,
                ((phNfc_sTransactionInfo_t *)pInfo)->length
                );
            /*Update P2PRecvLength,this also acts as the offset to append more 
              received bytes*/
            Hal4Ctxt->psTrcvCtxtInfo->P2PRecvLength 
                += ((phNfc_sTransactionInfo_t *)pInfo)->length;
            Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.length 
                = Hal4Ctxt->psTrcvCtxtInfo->P2PRecvLength;
        }

        if(RecvStatus != NFCSTATUS_MORE_INFORMATION)
        {
            Hal4Ctxt->psTrcvCtxtInfo->P2PRecvLength = 0;          
            Hal4Ctxt->Hal4NextState = (eHal4StateTransaction
             == Hal4Ctxt->Hal4NextState?eHal4StateInvalid:Hal4Ctxt->Hal4NextState);
            if(NFCSTATUS_PENDING == Hal4Ctxt->psTrcvCtxtInfo->RecvDataBufferStatus)
            {
                /*Initiator case*/
                if(NULL != Hal4Ctxt->psTrcvCtxtInfo->pUpperTranceiveCb)
                {
                    RecvStatus =(NFCSTATUS_RF_TIMEOUT == RecvStatus?
                                NFCSTATUS_DESELECTED:RecvStatus);
                    pUpperTrcvCb = Hal4Ctxt->psTrcvCtxtInfo->pUpperTranceiveCb;
                    Hal4Ctxt->psTrcvCtxtInfo->pUpperTranceiveCb = NULL;
                    Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData 
                        = &(Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData);
                    (*pUpperTrcvCb)(
                        Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
                        Hal4Ctxt->sTgtConnectInfo.psConnectedDevice,                
                        Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData,
                        RecvStatus
                        );
                }
                /*P2P target*/
                else if(NULL != Hal4Ctxt->psTrcvCtxtInfo->pP2PRecvCb)
                {
                    pUpperRecvCb = Hal4Ctxt->psTrcvCtxtInfo->pP2PRecvCb;
                    Hal4Ctxt->psTrcvCtxtInfo->pP2PRecvCb = NULL;
                    Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData 
                        = &(Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData);
                    (*pUpperRecvCb)(
                        Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
                        Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData,
                        RecvStatus
                        );
                }
                else
                {
                    /*Receive data buffer is complete with data & P2P receive has
                      not yet been called*/
                    Hal4Ctxt->psTrcvCtxtInfo->RecvDataBufferStatus 
                        = NFCSTATUS_SUCCESS;
                }
            }
        }
    }
    return;
}

/*Activation complete handler*/
void phHal4Nfc_P2PActivateComplete(
                             phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                             void *pInfo
                            )
{
    phHal_sEventInfo_t *psEventInfo = (phHal_sEventInfo_t *)pInfo;
    NFCSTATUS Status = NFCSTATUS_SUCCESS;
    static phHal4Nfc_DiscoveryInfo_t sDiscoveryInfo;
    /*Copy notification info to provide to upper layer*/
    phHal4Nfc_NotificationInfo_t uNotificationInfo = {&sDiscoveryInfo};
    Hal4Ctxt->sTgtConnectInfo.EmulationState = NFC_EVT_ACTIVATED;
    /*if P2p notification is registered*/
    if( NULL != Hal4Ctxt->sUpperLayerInfo.pP2PNotification)
    {
        /*Allocate remote device Info for P2P target*/
        uNotificationInfo.psDiscoveryInfo->NumberOfDevices = 1;
        if(NULL == Hal4Ctxt->rem_dev_list[0])
        {
            Hal4Ctxt->rem_dev_list[0] 
                = (phHal_sRemoteDevInformation_t *)
                    phOsalNfc_GetMemory(
                    sizeof(phHal_sRemoteDevInformation_t)
                    );
        }
        if(NULL == Hal4Ctxt->rem_dev_list[0])
        {
            phOsalNfc_RaiseException(phOsalNfc_e_NoMemory,0);
            Status = PHNFCSTVAL(CID_NFC_HAL , 
                NFCSTATUS_INSUFFICIENT_RESOURCES);
        }
        else
        {
            (void)memset((void *)Hal4Ctxt->rem_dev_list[0],
                                0,sizeof(phHal_sRemoteDevInformation_t));
            /*Copy device info*/
            (void)memcpy(Hal4Ctxt->rem_dev_list[0],
                                psEventInfo->eventInfo.pRemoteDevInfo,
                                sizeof(phHal_sRemoteDevInformation_t)
                                );
            /*Allocate Trcv context info*/
            if(NULL == Hal4Ctxt->psTrcvCtxtInfo)
            {
                Hal4Ctxt->psTrcvCtxtInfo= (pphHal4Nfc_TrcvCtxtInfo_t)
                    phOsalNfc_GetMemory((uint32_t)
                    (sizeof(phHal4Nfc_TrcvCtxtInfo_t)));
                if(NULL != Hal4Ctxt->psTrcvCtxtInfo)
                {
                    (void)memset(Hal4Ctxt->psTrcvCtxtInfo,0,
                        sizeof(phHal4Nfc_TrcvCtxtInfo_t));
                    Hal4Ctxt->psTrcvCtxtInfo->RecvDataBufferStatus 
                        = NFCSTATUS_PENDING;
                    Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                                        = PH_OSALNFC_INVALID_TIMER_ID;
                }
            }
            if(NULL == Hal4Ctxt->psTrcvCtxtInfo)
            {
                phOsalNfc_RaiseException(phOsalNfc_e_NoMemory,0);
                Status= PHNFCSTVAL(CID_NFC_HAL , 
                    NFCSTATUS_INSUFFICIENT_RESOURCES);
            }
            else
            {
                /*Update state*/
                Hal4Ctxt->Hal4CurrentState = eHal4StateEmulation;
                Hal4Ctxt->Hal4NextState = eHal4StateInvalid;
                uNotificationInfo.psDiscoveryInfo->ppRemoteDevInfo
                    = Hal4Ctxt->rem_dev_list;
                /*set session Opened ,this will keep track of whether the session 
                 is alive.will be reset if a Event DEACTIVATED is received*/
                Hal4Ctxt->rem_dev_list[0]->SessionOpened = TRUE;
                (*Hal4Ctxt->sUpperLayerInfo.pP2PNotification)(
                    Hal4Ctxt->sUpperLayerInfo.P2PDiscoveryCtxt,
                    NFC_DISCOVERY_NOTIFICATION,
                    uNotificationInfo,
                    Status
                    );
            }
        }
    }
    return;
}

/*Deactivation complete handler*/
void phHal4Nfc_HandleP2PDeActivate(
                               phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                               void *pInfo
                               )
{
    pphHal4Nfc_ReceiveCallback_t pUpperRecvCb = NULL;
    pphHal4Nfc_SendCallback_t pUpperSendCb = NULL;
    phHal4Nfc_NotificationInfo_t uNotificationInfo;
    uNotificationInfo.psEventInfo = (phHal_sEventInfo_t *)pInfo;
    /*session is closed*/
    if(NULL != Hal4Ctxt->rem_dev_list[0])
    {
        Hal4Ctxt->rem_dev_list[0]->SessionOpened = FALSE;
        Hal4Ctxt->psADDCtxtInfo->nbr_of_devices = 0;
    }
    Hal4Ctxt->sTgtConnectInfo.psConnectedDevice = NULL;
    /*Update state*/
    Hal4Ctxt->Hal4CurrentState = eHal4StateOpenAndReady;
    Hal4Ctxt->Hal4NextState  = eHal4StateInvalid;
    Hal4Ctxt->sTgtConnectInfo.EmulationState = NFC_EVT_DEACTIVATED;
    /*If Trcv ctxt info is allocated ,free it here*/
    if(NULL != Hal4Ctxt->psTrcvCtxtInfo)
    {
        if(PH_OSALNFC_INVALID_TIMER_ID != 
            Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId)
        {
            phOsalNfc_Timer_Stop(Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId);
            phOsalNfc_Timer_Delete(Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId);
        }
        pUpperRecvCb = Hal4Ctxt->psTrcvCtxtInfo->pP2PRecvCb;
        pUpperSendCb = Hal4Ctxt->psTrcvCtxtInfo->pP2PSendCb;
        /*Free Hal4 resources used by Target*/
        if (NULL != Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.buffer)
        {
            phOsalNfc_FreeMemory(Hal4Ctxt->psTrcvCtxtInfo->
                sLowerRecvData.buffer);
        }
        if((NULL == Hal4Ctxt->sTgtConnectInfo.psConnectedDevice) 
            && (NULL != Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData))
        {
            phOsalNfc_FreeMemory(Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData);
        }  
        phOsalNfc_FreeMemory(Hal4Ctxt->psTrcvCtxtInfo);
        Hal4Ctxt->psTrcvCtxtInfo = NULL;
    }
    /*if recv callback is pending*/
    if(NULL != pUpperRecvCb)
    {
        (*pUpperRecvCb)(
                        Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
                        NULL,
                        NFCSTATUS_DESELECTED
                        );
    }  
    /*if send callback is pending*/
    else if(NULL != pUpperSendCb)
    {
        (*pUpperSendCb)(
                        Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
                        NFCSTATUS_DESELECTED
                        );
    }  
    /*if pP2PNotification is registered*/
    else if(NULL != Hal4Ctxt->sUpperLayerInfo.pP2PNotification)
    {
        (*Hal4Ctxt->sUpperLayerInfo.pP2PNotification)(
                                Hal4Ctxt->sUpperLayerInfo.P2PDiscoveryCtxt,
                                NFC_EVENT_NOTIFICATION,
                                uNotificationInfo,
                                NFCSTATUS_DESELECTED
                                );
    }
    else/*Call Default event handler*/
    {
        if(NULL != Hal4Ctxt->sUpperLayerInfo.pDefaultEventHandler)
        {
            Hal4Ctxt->sUpperLayerInfo.pDefaultEventHandler(
                Hal4Ctxt->sUpperLayerInfo.DefaultListenerCtxt,
                NFC_EVENT_NOTIFICATION,
                uNotificationInfo,
                NFCSTATUS_DESELECTED
                );
        }
    }
}
