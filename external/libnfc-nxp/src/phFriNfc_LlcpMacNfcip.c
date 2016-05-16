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


/**
 * \file  phFriNfc_LlcpMacNfcip.c
 * \brief NFC LLCP MAC Mappings For Different RF Technologies.
 *
 * Project: NFC-FRI
 *
 */


/*include files*/
#include <phFriNfc_LlcpMac.h>
#include <phLibNfcStatus.h>
#include <phLibNfc.h>
#include <phLibNfc_Internal.h>
#include <stdio.h>
#include <string.h>

static NFCSTATUS phFriNfc_LlcpMac_Nfcip_Send(phFriNfc_LlcpMac_t               *LlcpMac,
                                             phNfc_sData_t                    *psData,
                                             phFriNfc_LlcpMac_Send_CB_t       LlcpMacSend_Cb,
                                             void                             *pContext);


static void phFriNfc_LlcpMac_Nfcip_TriggerRecvCb(phFriNfc_LlcpMac_t  *LlcpMac,
                                                 NFCSTATUS           status)
{
   phFriNfc_LlcpMac_Reveive_CB_t pfReceiveCB;
   void                          *pReceiveContext;

   if (LlcpMac->MacReceive_Cb != NULL)
   {
      /* Save callback params */
      pfReceiveCB = LlcpMac->MacReceive_Cb;
      pReceiveContext = LlcpMac->MacReceive_Context;

      /* Reset the pointer to the Receive Callback and Context*/
      LlcpMac->MacReceive_Cb = NULL;
      LlcpMac->MacReceive_Context = NULL;

      /* Call the receive callback */
      pfReceiveCB(pReceiveContext, status, LlcpMac->psReceiveBuffer);
   }
}

static void phFriNfc_LlcpMac_Nfcip_TriggerSendCb(phFriNfc_LlcpMac_t  *LlcpMac,
                                                 NFCSTATUS           status)
{
   phFriNfc_LlcpMac_Send_CB_t pfSendCB;
   void                       *pSendContext;

   if (LlcpMac->MacSend_Cb != NULL)
   {
      /* Save context in local variables */
      pfSendCB     = LlcpMac->MacSend_Cb;
      pSendContext = LlcpMac->MacSend_Context;

      /* Reset the pointer to the Send Callback */
      LlcpMac->MacSend_Cb = NULL;
      LlcpMac->MacSend_Context = NULL;

      /* Call Send callback */
      pfSendCB(pSendContext, status);
   }
}

static NFCSTATUS phFriNfc_LlcpMac_Nfcip_Chk(phFriNfc_LlcpMac_t                   *LlcpMac,
                                            phFriNfc_LlcpMac_Chk_CB_t            ChkLlcpMac_Cb,
                                            void                                 *pContext)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;
   uint8_t Llcp_Magic_Number[] = {0x46,0x66,0x6D};

   if(NULL == LlcpMac || NULL == ChkLlcpMac_Cb || NULL == pContext)
   {
      status = PHNFCSTVAL(CID_FRI_NFC_LLCP_MAC, NFCSTATUS_INVALID_PARAMETER);
   }
   else
   {
      status = (NFCSTATUS)memcmp(Llcp_Magic_Number,LlcpMac->psRemoteDevInfo->RemoteDevInfo.NfcIP_Info.ATRInfo,3);
      if(!status)
      {
         LlcpMac->sConfigParam.buffer = &LlcpMac->psRemoteDevInfo->RemoteDevInfo.NfcIP_Info.ATRInfo[3] ;
         LlcpMac->sConfigParam.length = (LlcpMac->psRemoteDevInfo->RemoteDevInfo.NfcIP_Info.ATRInfo_Length - 3);
         status = NFCSTATUS_SUCCESS;
      }
      else
      {
         status = PHNFCSTVAL(CID_FRI_NFC_LLCP_MAC, NFCSTATUS_FAILED);
      }
      ChkLlcpMac_Cb(pContext,status);
   }

   return status;
}

static NFCSTATUS phFriNfc_LlcpMac_Nfcip_Activate (phFriNfc_LlcpMac_t   *LlcpMac)
{
   NFCSTATUS status  = NFCSTATUS_SUCCESS;

   if(LlcpMac == NULL)
   {
      status = PHNFCSTVAL(CID_FRI_NFC_LLCP_MAC, NFCSTATUS_INVALID_PARAMETER);
   }
   else
   {
      LlcpMac->LinkState = phFriNfc_LlcpMac_eLinkActivated;
      LlcpMac->LinkStatus_Cb(LlcpMac->LinkStatus_Context,
                             LlcpMac->LinkState,
                             &LlcpMac->sConfigParam,
                             LlcpMac->PeerRemoteDevType);
   }

   return status;
}

static NFCSTATUS phFriNfc_LlcpMac_Nfcip_Deactivate (phFriNfc_LlcpMac_t   *LlcpMac)
{
   NFCSTATUS status  = NFCSTATUS_SUCCESS;

   if(NULL == LlcpMac)
   {
      status = PHNFCSTVAL(CID_FRI_NFC_LLCP_MAC, NFCSTATUS_INVALID_PARAMETER);
   }
   else
   {
      /* Set the flag of LinkStatus to deactivate */
      LlcpMac->LinkState = phFriNfc_LlcpMac_eLinkDeactivated;

      if (LlcpMac->SendPending)
      {
         /* Reset Flag */
         LlcpMac->SendPending = FALSE;
         phFriNfc_LlcpMac_Nfcip_TriggerSendCb(LlcpMac, NFCSTATUS_FAILED);
      }

      if (LlcpMac->RecvPending)
      {
         /* Reset Flag */
         LlcpMac->RecvPending = FALSE;
         phFriNfc_LlcpMac_Nfcip_TriggerRecvCb(LlcpMac, NFCSTATUS_FAILED);
      }

      LlcpMac->LinkStatus_Cb(LlcpMac->LinkStatus_Context,
                             LlcpMac->LinkState,
                             NULL,
                             LlcpMac->PeerRemoteDevType);
   }

   return status;
}

static void phFriNfc_LlcpMac_Nfcip_Send_Cb(void       *pContext,
                                           NFCSTATUS   Status)
{
   phFriNfc_LlcpMac_t            *LlcpMac = (phFriNfc_LlcpMac_t *)pContext;

#ifdef LLCP_CHANGES
   if(gpphLibContext->LibNfcState.next_state
                               == eLibNfcHalStateShutdown)
   {
      phLibNfc_Pending_Shutdown();
      Status = NFCSTATUS_SHUTDOWN;
   }
#endif /* #ifdef LLCP_CHANGES */

   /* Reset Send and Receive Flag */
   LlcpMac->SendPending = FALSE;
   LlcpMac->RecvPending = FALSE;

   phFriNfc_LlcpMac_Nfcip_TriggerSendCb(LlcpMac, Status);

}

static void phFriNfc_LlcpMac_Nfcip_Receive_Cb(void       *pContext,
                                              NFCSTATUS   Status)
{
   phFriNfc_LlcpMac_t               *LlcpMac = (phFriNfc_LlcpMac_t *)pContext;
#ifdef LLCP_CHANGES

   phFriNfc_LlcpMac_Send_CB_t       pfSendCB;
   void                             *pSendContext;


   if(gpphLibContext->LibNfcState.next_state
                               == eLibNfcHalStateShutdown)
   {
      phLibNfc_Pending_Shutdown();
      Status = NFCSTATUS_SHUTDOWN;
   }

   if (NFCSTATUS_SHUTDOWN == Status)
   {
      /* Save context in local variables */
      pfSendCB = LlcpMac->MacSend_Cb;
      pSendContext = LlcpMac->MacSend_Context;

      /* Reset the pointer to the Send Callback */
      LlcpMac->MacSend_Cb = NULL;
      LlcpMac->MacSend_Context = NULL;

      /* Reset Send and Receive Flag */
      LlcpMac->SendPending = FALSE;
      LlcpMac->RecvPending = FALSE;
   }

#endif /* #ifdef LLCP_CHANGES */

   phFriNfc_LlcpMac_Nfcip_TriggerRecvCb(LlcpMac, Status);

#ifdef LLCP_CHANGES

   if (NFCSTATUS_SHUTDOWN == Status)
   {
       if ((LlcpMac->SendPending) && (NULL != pfSendCB))
       {
           pfSendCB(pSendContext, Status);
      }
   }
   else

#endif /* #ifdef LLCP_CHANGES */
   {
   /* Test if a send is pending */
   if(LlcpMac->SendPending)
   {
      Status = phFriNfc_LlcpMac_Nfcip_Send(LlcpMac,LlcpMac->psSendBuffer,LlcpMac->MacSend_Cb,LlcpMac->MacSend_Context);
   }
}
}

static void phFriNfc_LlcpMac_Nfcip_Transceive_Cb(void       *pContext,
                                                 NFCSTATUS   Status)
{
   phFriNfc_LlcpMac_t               *LlcpMac = (phFriNfc_LlcpMac_t *)pContext;

#ifdef LLCP_CHANGES
   if(gpphLibContext->LibNfcState.next_state
                               == eLibNfcHalStateShutdown)
   {
      phLibNfc_Pending_Shutdown();
      Status = NFCSTATUS_SHUTDOWN;
   }
#endif /* #ifdef LLCP_CHANGES */

   /* Reset Send and Receive Flag */
   LlcpMac->SendPending = FALSE;
   LlcpMac->RecvPending = FALSE;

   /* Call the callbacks */
   phFriNfc_LlcpMac_Nfcip_TriggerSendCb(LlcpMac, Status);
   phFriNfc_LlcpMac_Nfcip_TriggerRecvCb(LlcpMac, Status);
}

static NFCSTATUS phFriNfc_LlcpMac_Nfcip_Send(phFriNfc_LlcpMac_t               *LlcpMac,
                                             phNfc_sData_t                    *psData,
                                             phFriNfc_LlcpMac_Send_CB_t       LlcpMacSend_Cb,
                                             void                             *pContext)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;
   
   if(NULL == LlcpMac || NULL == psData || NULL == LlcpMacSend_Cb || NULL == pContext)
   {
      status = PHNFCSTVAL(CID_FRI_NFC_LLCP_MAC, NFCSTATUS_INVALID_PARAMETER);
   }
   else if(LlcpMac->MacSend_Cb != NULL && LlcpMac->PeerRemoteDevType == phFriNfc_LlcpMac_ePeerTypeInitiator)
   {
      /*Previous callback is pending */
      status = NFCSTATUS_REJECTED;
   }
   else
   {
      /* Save the LlcpMacSend_Cb */
      LlcpMac->MacSend_Cb = LlcpMacSend_Cb;
      LlcpMac->MacSend_Context = pContext;

      switch(LlcpMac->PeerRemoteDevType)
      {
      case phFriNfc_LlcpMac_ePeerTypeInitiator:
         {
            if(LlcpMac->RecvPending)
            {
               /*set the completion routines for the LLCP Transceive function*/
                LlcpMac->MacCompletionInfo.CompletionRoutine = phFriNfc_LlcpMac_Nfcip_Transceive_Cb;
                LlcpMac->MacCompletionInfo.Context = LlcpMac;

                /* set the command type*/
                LlcpMac->Cmd.NfcIP1Cmd = phHal_eNfcIP1_Raw;

                /* set the Additional Info*/
                LlcpMac->psDepAdditionalInfo.DepFlags.MetaChaining = 0;
                LlcpMac->psDepAdditionalInfo.DepFlags.NADPresent = 0;
                LlcpMac->SendPending = TRUE;

                status = phFriNfc_OvrHal_Transceive(LlcpMac->LowerDevice,
                                                    &LlcpMac->MacCompletionInfo,
                                                    LlcpMac->psRemoteDevInfo,
                                                    LlcpMac->Cmd,
                                                    &LlcpMac->psDepAdditionalInfo,
                                                    psData->buffer,
                                                    (uint16_t)psData->length,
                                                    LlcpMac->psReceiveBuffer->buffer,
                                                    (uint16_t*)&LlcpMac->psReceiveBuffer->length);
            }
            else
            {
               LlcpMac->SendPending = TRUE;
               LlcpMac->psSendBuffer = psData;
               return status = NFCSTATUS_PENDING;
            }
         }break;
      case phFriNfc_LlcpMac_ePeerTypeTarget:
         {
            if(!LlcpMac->RecvPending)
            {
               LlcpMac->SendPending = TRUE;
               LlcpMac->psSendBuffer = psData;
               return status = NFCSTATUS_PENDING;
            }
            else
            {
               /*set the completion routines for the LLCP Send function*/
               LlcpMac->MacCompletionInfo.CompletionRoutine = phFriNfc_LlcpMac_Nfcip_Send_Cb;
               LlcpMac->MacCompletionInfo.Context = LlcpMac;
               status = phFriNfc_OvrHal_Send(LlcpMac->LowerDevice,
                                             &LlcpMac->MacCompletionInfo,
                                             LlcpMac->psRemoteDevInfo,
                                             psData->buffer,
                                             (uint16_t)psData->length);
            }
         }break;
      default:
         {
            status = PHNFCSTVAL(CID_FRI_NFC_LLCP_MAC, NFCSTATUS_INVALID_DEVICE);
         }break;
      }
   }
   return status;
}

static NFCSTATUS phFriNfc_LlcpMac_Nfcip_Receive(phFriNfc_LlcpMac_t               *LlcpMac,
                                                phNfc_sData_t                    *psData,
                                                phFriNfc_LlcpMac_Reveive_CB_t    LlcpMacReceive_Cb,
                                                void                             *pContext)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;
   if(NULL == LlcpMac || NULL==psData || NULL == LlcpMacReceive_Cb || NULL == pContext)
   {
      status = PHNFCSTVAL(CID_FRI_NFC_LLCP_MAC, NFCSTATUS_INVALID_PARAMETER);
   }
   else if(LlcpMac->MacReceive_Cb != NULL)
   {
      /*Previous callback is pending */
      status = NFCSTATUS_REJECTED;
   }
   else
   {
      /* Save the LlcpMacReceive_Cb */
      LlcpMac->MacReceive_Cb = LlcpMacReceive_Cb;
      LlcpMac->MacReceive_Context = pContext;

      /* Save the pointer to the receive buffer */
      LlcpMac->psReceiveBuffer= psData;

      switch(LlcpMac->PeerRemoteDevType)
      {
      case phFriNfc_LlcpMac_ePeerTypeInitiator:
         {
            if(LlcpMac->SendPending)
            {
               /*set the completion routines for the LLCP Transceive function*/
               LlcpMac->MacCompletionInfo.CompletionRoutine = phFriNfc_LlcpMac_Nfcip_Transceive_Cb;
               LlcpMac->MacCompletionInfo.Context = LlcpMac;
               /* set the command type*/
               LlcpMac->Cmd.NfcIP1Cmd = phHal_eNfcIP1_Raw;
               /* set the Additional Info*/
               LlcpMac->psDepAdditionalInfo.DepFlags.MetaChaining = 0;
               LlcpMac->psDepAdditionalInfo.DepFlags.NADPresent = 0;
               LlcpMac->RecvPending = TRUE;

               status = phFriNfc_OvrHal_Transceive(LlcpMac->LowerDevice,
                                                   &LlcpMac->MacCompletionInfo,
                                                   LlcpMac->psRemoteDevInfo,
                                                   LlcpMac->Cmd,
                                                   &LlcpMac->psDepAdditionalInfo,
                                                   LlcpMac->psSendBuffer->buffer,
                                                   (uint16_t)LlcpMac->psSendBuffer->length,
                                                   psData->buffer,
                                                   (uint16_t*)&psData->length);
            }
            else
            {
               LlcpMac->RecvPending = TRUE;
               return status = NFCSTATUS_PENDING;
            }
         }break;
      case phFriNfc_LlcpMac_ePeerTypeTarget:
         {
             /*set the completion routines for the LLCP Receive function*/
            LlcpMac->MacCompletionInfo.CompletionRoutine = phFriNfc_LlcpMac_Nfcip_Receive_Cb;
            /* save the context of LlcpMacNfcip */
            LlcpMac->MacCompletionInfo.Context = LlcpMac;
            LlcpMac->RecvPending = TRUE;

            status = phFriNfc_OvrHal_Receive(LlcpMac->LowerDevice,
                                             &LlcpMac->MacCompletionInfo,
                                             LlcpMac->psRemoteDevInfo,
                                             LlcpMac->psReceiveBuffer->buffer,
                                             (uint16_t*)&LlcpMac->psReceiveBuffer->length);
         }break;
      default:
         {
            status = PHNFCSTVAL(CID_FRI_NFC_LLCP_MAC, NFCSTATUS_INVALID_DEVICE);
         }break;
      }
   }
   return status;
}


NFCSTATUS phFriNfc_LlcpMac_Nfcip_Register (phFriNfc_LlcpMac_t *LlcpMac)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;

   if(NULL != LlcpMac)
   {
      LlcpMac->LlcpMacInterface.chk = phFriNfc_LlcpMac_Nfcip_Chk;
      LlcpMac->LlcpMacInterface.activate   = phFriNfc_LlcpMac_Nfcip_Activate;
      LlcpMac->LlcpMacInterface.deactivate = phFriNfc_LlcpMac_Nfcip_Deactivate;
      LlcpMac->LlcpMacInterface.send = phFriNfc_LlcpMac_Nfcip_Send;
      LlcpMac->LlcpMacInterface.receive = phFriNfc_LlcpMac_Nfcip_Receive;

      return NFCSTATUS_SUCCESS;
   }
   else
   {
      return status = PHNFCSTVAL(CID_FRI_NFC_LLCP_MAC, NFCSTATUS_FAILED);
   }
}
