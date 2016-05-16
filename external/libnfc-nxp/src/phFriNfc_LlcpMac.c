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
 * \file  phFriNfc_LlcpMac.c
 * \brief NFC LLCP MAC Mappings For Different RF Technologies.
 *
 * Project: NFC-FRI
 *
 */


/*include files*/
#include <phFriNfc_LlcpMac.h>
#include <phFriNfc_LlcpMacNfcip.h>
#include <phLibNfcStatus.h>
#include <phLibNfc.h>
#include <phLibNfc_Internal.h>

NFCSTATUS phFriNfc_LlcpMac_Reset (phFriNfc_LlcpMac_t                 *LlcpMac,
                                  void                               *LowerDevice,
                                  phFriNfc_LlcpMac_LinkStatus_CB_t   LinkStatus_Cb,
                                  void                               *pContext)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;
   /* Store the Linkstatus callback function of the upper layer */
   LlcpMac->LinkStatus_Cb = LinkStatus_Cb;

   /* Store a pointer to the upper layer context */
   LlcpMac->LinkStatus_Context = pContext;

   /* Set the LinkStatus variable to the default state */
   LlcpMac->LinkState = phFriNfc_LlcpMac_eLinkDefault;

   /* Store a pointer to the lower layer */
   LlcpMac->LowerDevice =  LowerDevice; 

   LlcpMac->psRemoteDevInfo         = NULL;
   LlcpMac->PeerRemoteDevType       = 0;
   LlcpMac->MacType                 = 0;
   LlcpMac->MacReceive_Cb           = NULL;
   LlcpMac->MacSend_Cb              = NULL;
   LlcpMac->psSendBuffer            = NULL;
   LlcpMac->RecvPending             = 0;
   LlcpMac->SendPending             = 0;

   return status;
}

NFCSTATUS phFriNfc_LlcpMac_ChkLlcp (phFriNfc_LlcpMac_t                  *LlcpMac, 
                                    phHal_sRemoteDevInformation_t       *psRemoteDevInfo,
                                    phFriNfc_LlcpMac_Chk_CB_t           ChkLlcpMac_Cb,
                                    void                                *pContext)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;
   if (NULL == LlcpMac || NULL == psRemoteDevInfo)
   {
      status = PHNFCSTVAL(CID_FRI_NFC_LLCP_MAC, NFCSTATUS_INVALID_PARAMETER);
   }
   else
   {
      /* Store the Remote Device info received from Device Discovery  */
      LlcpMac->psRemoteDevInfo = psRemoteDevInfo;

      if(LlcpMac->psRemoteDevInfo->RemDevType == phHal_eNfcIP1_Initiator)
      {
         /* Set the PeerRemoteDevType variable to the Target type */
         LlcpMac->PeerRemoteDevType = phFriNfc_LlcpMac_ePeerTypeTarget;
      }
      else if(LlcpMac->psRemoteDevInfo->RemDevType == phHal_eNfcIP1_Target)
      {
         /* Set the PeerRemoteDevType variable to the Initiator type */
         LlcpMac->PeerRemoteDevType = phFriNfc_LlcpMac_ePeerTypeInitiator;
      }

      switch(LlcpMac->psRemoteDevInfo->RemDevType)
      {
      case phHal_eNfcIP1_Initiator:
      case phHal_eNfcIP1_Target:
         {
            /* Set the MAC mapping type detected */
            LlcpMac->MacType = phFriNfc_LlcpMac_eTypeNfcip;

            /* Register the lower layer to the MAC mapping component */
            status = phFriNfc_LlcpMac_Nfcip_Register (LlcpMac); 
            if(status == NFCSTATUS_SUCCESS)
            {
               status  = LlcpMac->LlcpMacInterface.chk(LlcpMac,ChkLlcpMac_Cb,pContext);
            }
            else
            {
               status = PHNFCSTVAL(CID_FRI_NFC_LLCP_MAC, NFCSTATUS_FAILED);
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

NFCSTATUS phFriNfc_LlcpMac_Activate (phFriNfc_LlcpMac_t   *LlcpMac)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;

   if(LlcpMac->LlcpMacInterface.activate == NULL)
   {  
      status = PHNFCSTVAL(CID_FRI_NFC_LLCP_MAC, NFCSTATUS_INVALID_PARAMETER);
   }
   else
   {
      status = LlcpMac->LlcpMacInterface.activate(LlcpMac);
   }
   return status;
}

NFCSTATUS phFriNfc_LlcpMac_Deactivate (phFriNfc_LlcpMac_t   *LlcpMac)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;
   if(LlcpMac->LlcpMacInterface.deactivate == NULL)
   {  
      status = PHNFCSTVAL(CID_FRI_NFC_LLCP_MAC, NFCSTATUS_INVALID_PARAMETER);
   }
   else
   {
      status = LlcpMac->LlcpMacInterface.deactivate(LlcpMac);
   }
   return status;
}

NFCSTATUS phFriNfc_LlcpMac_Send (phFriNfc_LlcpMac_t               *LlcpMac, 
                                 phNfc_sData_t                    *psData,
                                 phFriNfc_LlcpMac_Send_CB_t       LlcpMacSend_Cb,
                                 void                             *pContext)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;

   if(NULL== LlcpMac->LlcpMacInterface.send || NULL==psData || NULL==LlcpMacSend_Cb || NULL==pContext)
   {
      status = PHNFCSTVAL(CID_FRI_NFC_LLCP_MAC, NFCSTATUS_INVALID_PARAMETER);
   }
   else
   {
      status = LlcpMac->LlcpMacInterface.send(LlcpMac,psData,LlcpMacSend_Cb,pContext);
   }
   return status;
}

NFCSTATUS phFriNfc_LlcpMac_Receive (phFriNfc_LlcpMac_t               *LlcpMac,
                                    phNfc_sData_t                    *psData,
                                    phFriNfc_LlcpMac_Reveive_CB_t    ReceiveLlcpMac_Cb,
                                    void                             *pContext)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;

   if(LlcpMac->LlcpMacInterface.receive == NULL || NULL==psData || NULL==ReceiveLlcpMac_Cb || NULL==pContext)
   {
      status = PHNFCSTVAL(CID_FRI_NFC_LLCP_MAC, NFCSTATUS_INVALID_PARAMETER);
   }
   else
   {
      status = LlcpMac->LlcpMacInterface.receive(LlcpMac,psData,ReceiveLlcpMac_Cb,pContext);
   }
   return status;

}


