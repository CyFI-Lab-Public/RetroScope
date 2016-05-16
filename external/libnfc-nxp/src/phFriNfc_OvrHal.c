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
 * \file  phFriNfc_OvrHal.c
 * \brief Overlapped HAL
 *
 * Project: NFC-FRI
 * Creator: Gerald Kersch
 *
 * $Date: Wed May  5 10:47:27 2010 $
 * Changed by: $Author: ing02260 $
 * $Revision: 1.37 $
 * $Aliases: NFC_FRI1.1_WK1017_R34_3,NFC_FRI1.1_WK1023_R35_1 $
 *
 */

#include <phFriNfc_OvrHal.h>
#include <phOsalNfc.h>
#include <phFriNfc_NdefMap.h>
#include <phFriNfc_SmtCrdFmt.h>


#ifdef PHFRINFC_OVRHAL_MOCKUP  /* */
//#include <phLibNfc_Gen.h>
#endif /* PHFRINFC_OVRHAL_MOCKUP */
/*
*   

*/
#define MAX_MIF_PACKET_LEN                      0x0FU
#define MIFARE_PLUS_UID_INDEX_TO_COPY           0x03U
#define MIFARE_PLUS_UID_LENGTH                  0x07U
#define MIFARE_CLASSIC_UID_LENGTH               0x04U
#define MIFARE_UID_LEN_TO_COPY                  0x04U

static void phFriNfc_OvrHal_CB_Send(void *context,
                                    NFCSTATUS status);
static void phFriNfc_OvrHal_CB_Receive(void *context,
                                       phNfc_sData_t *pDataInfo,
                                       NFCSTATUS status);
static void phFriNfc_OvrHal_CB_Transceive(void *context,
                               phHal_sRemoteDevInformation_t *RemoteDevHandle,
                               phNfc_sData_t  *pRecvdata,
                               NFCSTATUS status
                               );
static void phFriNfc_OvrHal_CB_ConnectDisconnect(void *context,
                               phHal_sRemoteDevInformation_t *RemoteDevHandle,
                               NFCSTATUS status
                               );

static void  phFriNfc_OvrHal_SetComplInfo(phFriNfc_OvrHal_t *OvrHal,
                                   phFriNfc_CplRt_t  *CompletionInfo,
                                   uint8_t            Operation);

NFCSTATUS phFriNfc_OvrHal_Transceive(phFriNfc_OvrHal_t              *OvrHal,
                                     phFriNfc_CplRt_t               *CompletionInfo,
                                     phHal_sRemoteDevInformation_t  *RemoteDevInfo,
                                     phHal_uCmdList_t                Cmd,
                                     phHal_sDepAdditionalInfo_t     *DepAdditionalInfo,
                                     uint8_t                        *SendBuf,
                                     uint16_t                        SendLength,
                                     uint8_t                        *RecvBuf,
                                     uint16_t                       *RecvLength)
{
    NFCSTATUS status = NFCSTATUS_PENDING;
    uint8_t i = 0;
    uint32_t length = SendLength;

    /* To remove "warning (VS C4100) : unreferenced formal parameter" */
    PHNFC_UNUSED_VARIABLE(DepAdditionalInfo);

    /* Check the Input Parameters */
    if ((NULL == OvrHal) || (NULL == CompletionInfo) || (NULL == RemoteDevInfo)
        || (NULL == (void*)SendBuf) || (NULL == RecvBuf) || (NULL == RecvLength) 
        || ((phHal_eJewel_PICC != RemoteDevInfo->RemDevType) && (0 == SendLength)))

    {
        status = PHNFCSTVAL(CID_FRI_NFC_OVR_HAL, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        /* 16 is the maximum data, that can be sent to the mifare standard */
        static uint8_t      mif_send_buf[MAX_MIF_PACKET_LEN] = {0};
        /* Populate the Transfer info structure */
        OvrHal->TranceiveInfo.cmd = Cmd;
     
        /* Populate the Send Buffer Info */
        if((phHal_eMifare_PICC == RemoteDevInfo->RemDevType) 
            || (phHal_eISO14443_3A_PICC == RemoteDevInfo->RemDevType))
        {
            OvrHal->TranceiveInfo.addr = SendBuf[i++];
            length = (SendLength - i);

            if ((phHal_eMifareAuthentA == Cmd.MfCmd) 
                || (phHal_eMifareAuthentB == Cmd.MfCmd))
            {
                uint8_t     uid_index = 0;
                /* Authentication requires UID in the send buffer */
                uint8_t     uid_len = 
                        RemoteDevInfo->RemoteDevInfo.Iso14443A_Info.UidLength;
                OvrHal->TranceiveInfo.sSendData.buffer = mif_send_buf;

                switch (uid_len)
                {
                    case MIFARE_PLUS_UID_LENGTH:
                    {
                        uid_index = MIFARE_PLUS_UID_INDEX_TO_COPY;
                        uid_len = MIFARE_UID_LEN_TO_COPY;
                        break;
                    }

                    case MIFARE_CLASSIC_UID_LENGTH:
                    {
                        uid_index = 0;
                        break;
                    }

                    default:
                    {
                        status = PHNFCSTVAL (CID_FRI_NFC_OVR_HAL, 
                                            NFCSTATUS_READ_FAILED);                                            
                        break;
                    }
                }

                if (NFCSTATUS_PENDING == status)
                {
                    /* copy uid to the send buffer for the authentication */
                    (void)memcpy ((void *)mif_send_buf, 
                        (void *)&RemoteDevInfo->RemoteDevInfo.Iso14443A_Info.Uid[uid_index], 
                        uid_len);

                    (void)memcpy((mif_send_buf + uid_len), &(SendBuf[i]), length);
                    length += uid_len;
                }
            }
            else
            {
                OvrHal->TranceiveInfo.sSendData.buffer = &SendBuf[i++];         
            }           
            OvrHal->TranceiveInfo.sSendData.length = length;
        }
        else
        {
            OvrHal->TranceiveInfo.sSendData.buffer = &SendBuf[i++];
            OvrHal->TranceiveInfo.sSendData.length = length;
        }
        
        if (NFCSTATUS_PENDING == status)
        {
            /* Populate the Receive buffer */
            OvrHal->TranceiveInfo.sRecvData.buffer = RecvBuf;
            OvrHal->TranceiveInfo.sRecvData.length = *RecvLength;
            OvrHal->pndef_recv_length = RecvLength;
            phFriNfc_OvrHal_SetComplInfo(OvrHal,CompletionInfo, PH_FRINFC_OVRHAL_TRX);
            
            /* Call the HAL 4.0 Transceive Function */
            status = phHal4Nfc_Transceive (OvrHal->psHwReference, 
                                            &OvrHal->TranceiveInfo, RemoteDevInfo, 
                                            phFriNfc_OvrHal_CB_Transceive, (void *)OvrHal);
        }

    }
    return status;  

}

NFCSTATUS phFriNfc_OvrHal_Receive(phFriNfc_OvrHal_t              *OvrHal,
                                  phFriNfc_CplRt_t               *CompletionInfo,
                                  phHal_sRemoteDevInformation_t  *RemoteDevInfo,
                                  uint8_t                        *RecvBuf,
                                  uint16_t                       *RecvLength)
{
   NFCSTATUS status = NFCSTATUS_PENDING;

   /* Check the Input Parameters */
   if(   (NULL==OvrHal)  || (NULL==CompletionInfo) || (NULL==RemoteDevInfo)
      || (NULL==RecvBuf) || (NULL==RecvLength) )
   {
      status = PHNFCSTVAL(CID_FRI_NFC_OVR_HAL ,NFCSTATUS_INVALID_PARAMETER);
   }
   else
   {
      /* Get the remote dev type */
      OvrHal->TransactInfo.remotePCDType = RemoteDevInfo->RemDevType;
      /* Save the receive buffer for use in callback */
      OvrHal->sReceiveData.buffer = RecvBuf;
      OvrHal->sReceiveData.length = *RecvLength;

      OvrHal->pndef_recv_length = RecvLength;
      
      /* Set the callback */
      phFriNfc_OvrHal_SetComplInfo(OvrHal, CompletionInfo, PH_FRINFC_OVRHAL_RCV);

      /* Call the HAL 4.0 Receive Function */
      status = phHal4Nfc_Receive( OvrHal->psHwReference,
                                  &OvrHal->TransactInfo,
                                  phFriNfc_OvrHal_CB_Receive,
                                  (void *)OvrHal);               
   }
   return status;  
}

NFCSTATUS phFriNfc_OvrHal_Send(phFriNfc_OvrHal_t              *OvrHal,
                               phFriNfc_CplRt_t               *CompletionInfo,
                               phHal_sRemoteDevInformation_t  *RemoteDevInfo,
                               uint8_t                        *SendBuf,
                               uint16_t                       SendLength)
{
   NFCSTATUS status = NFCSTATUS_PENDING;

   /* Check the Input Parameters */
   if(   (NULL==OvrHal) || (NULL==CompletionInfo) || (NULL==RemoteDevInfo) || (NULL==SendBuf)  )
   {
      status = PHNFCSTVAL(CID_FRI_NFC_OVR_HAL ,NFCSTATUS_INVALID_PARAMETER);
   }
   else
   {
      /* Get the remote dev type */
      OvrHal->TransactInfo.remotePCDType = RemoteDevInfo->RemDevType;
      /* Save the receive buffer for use in callback */
      OvrHal->sSendData.buffer = SendBuf;
      OvrHal->sSendData.length = SendLength;
      
      /* Set the callback */
      phFriNfc_OvrHal_SetComplInfo(OvrHal, CompletionInfo, PH_FRINFC_OVRHAL_SND);

      /* Call the HAL 4.0 Receive Function */
      status = phHal4Nfc_Send( OvrHal->psHwReference,
                               &OvrHal->TransactInfo,
                               OvrHal->sSendData,
                               phFriNfc_OvrHal_CB_Send,
                               (void *)OvrHal);
   }
   return status;
}

#ifndef PH_FRINFC_MAP_MIFARESTD_DISABLED


NFCSTATUS phFriNfc_OvrHal_Reconnect(phFriNfc_OvrHal_t              *OvrHal,
                                     phFriNfc_CplRt_t               *CompletionInfo,
                                     phHal_sRemoteDevInformation_t  *RemoteDevInfo)
{
    NFCSTATUS status = NFCSTATUS_PENDING;
     
    /* Check the Input Parameters */
    if((NULL == OvrHal) || (NULL == CompletionInfo) || (NULL == RemoteDevInfo))
    {
        status = PHNFCSTVAL(CID_FRI_NFC_OVR_HAL ,NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
         phFriNfc_OvrHal_SetComplInfo(OvrHal, CompletionInfo, PH_FRINFC_OVRHAL_DIS);

         status = phHal4Nfc_Connect(                                   
                             OvrHal->psHwReference,
                             RemoteDevInfo,                     
                             phFriNfc_OvrHal_CB_ConnectDisconnect,
                             (void *)OvrHal);       
    }

    return status;
}



NFCSTATUS phFriNfc_OvrHal_Connect(phFriNfc_OvrHal_t              *OvrHal,
                                        phFriNfc_CplRt_t               *CompletionInfo,
                                        phHal_sRemoteDevInformation_t  *RemoteDevInfo,
                                        phHal_sDevInputParam_t         *DevInputParam)
{
    NFCSTATUS status = NFCSTATUS_PENDING;
         
    /* Check the Input Parameters */
    if((NULL == OvrHal) || (NULL == CompletionInfo) || (NULL == RemoteDevInfo) ||
        (NULL == DevInputParam))
    {
        status = PHNFCSTVAL(CID_FRI_NFC_OVR_HAL ,NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        phFriNfc_OvrHal_SetComplInfo(OvrHal, CompletionInfo, PH_FRINFC_OVRHAL_CON);

        status = phHal4Nfc_Connect(
                                    OvrHal->psHwReference, 
                                    RemoteDevInfo,                                    
                                    phFriNfc_OvrHal_CB_ConnectDisconnect,
                                    (void *)OvrHal
                                    );
    }

    return status;
}

#endif

static void phFriNfc_OvrHal_CB_Transceive(void *context,
                                          phHal_sRemoteDevInformation_t *RemoteDevHandle,
                                          phNfc_sData_t  *pRecvdata,
                                          NFCSTATUS status
                                          )

{
    phFriNfc_OvrHal_t       *OvrHal = (phFriNfc_OvrHal_t *)context; 
    
    if (NULL != OvrHal)
    {        
        if(NULL != pRecvdata && OvrHal->TranceiveInfo.sRecvData.buffer != NULL && pRecvdata->buffer != NULL)
        {
           /* Work-around for the NFCIP Tranceive API */
            if (OvrHal->TranceiveInfo.sRecvData.buffer != pRecvdata->buffer)
            {
                memcpy(OvrHal->TranceiveInfo.sRecvData.buffer, pRecvdata->buffer, pRecvdata->length);
            }
            if (OvrHal->pndef_recv_length != NULL)
            {
               *OvrHal->pndef_recv_length = (uint16_t) pRecvdata->length;
            }
        }
        if(NULL != RemoteDevHandle)
        {   
            /* Fix for Warning 4100 */
            RemoteDevHandle=RemoteDevHandle;
        }

        if (NULL != OvrHal->TemporaryCompletionInfo.CompletionRoutine)
        {
            OvrHal->TemporaryCompletionInfo.CompletionRoutine(
                OvrHal->TemporaryCompletionInfo.Context,
                status);
        }
    }
}

static void phFriNfc_OvrHal_CB_Send(void *context,
                                    NFCSTATUS status)
{
    phFriNfc_OvrHal_t *OvrHal = (phFriNfc_OvrHal_t *)context;
    
    if (NULL != OvrHal)
    {
        if (NULL != OvrHal->TemporarySndCompletionInfo.CompletionRoutine)
        {
            OvrHal->TemporarySndCompletionInfo.CompletionRoutine(
                OvrHal->TemporarySndCompletionInfo.Context,
                status);
        }
    }
}

static void phFriNfc_OvrHal_CB_Receive(void *context,
                                       phNfc_sData_t *pDataInfo,
                                       NFCSTATUS status)
{
    phFriNfc_OvrHal_t *OvrHal = (phFriNfc_OvrHal_t *)context; 
    
    if (NULL != OvrHal)
    {
        /* Copy the received buffer */
        if(NULL != pDataInfo && OvrHal->sReceiveData.buffer != NULL && pDataInfo->buffer != NULL)
        {
            memcpy(OvrHal->sReceiveData.buffer, pDataInfo->buffer, pDataInfo->length);
            *OvrHal->pndef_recv_length = (uint16_t) pDataInfo->length;
        }

        if (NULL != OvrHal->TemporaryRcvCompletionInfo.CompletionRoutine)
        {
            OvrHal->TemporaryRcvCompletionInfo.CompletionRoutine(
                OvrHal->TemporaryRcvCompletionInfo.Context,
                status);
        }
    }
}

static void phFriNfc_OvrHal_CB_ConnectDisconnect(void *context,
                               phHal_sRemoteDevInformation_t *RemoteDevHandle,
                               NFCSTATUS status
                               )

{
    phFriNfc_OvrHal_t   *OvrHal = (phFriNfc_OvrHal_t *)context;
    
    if (NULL != OvrHal)
    {
        if (RemoteDevHandle != NULL)
        {
            /* Fix for Warning 4100 */
            RemoteDevHandle = RemoteDevHandle;
        }
        else
        {
            status = NFCSTATUS_FAILED;
        }
        
        OvrHal->TemporaryCompletionInfo.CompletionRoutine(
                OvrHal->TemporaryCompletionInfo.Context, status);
    }

}

static void  phFriNfc_OvrHal_SetComplInfo(phFriNfc_OvrHal_t *OvrHal,
                                   phFriNfc_CplRt_t  *CompletionInfo,
                                   uint8_t            Operation)

{
   OvrHal->Operation = Operation;
   switch(Operation)
   {
      case PH_FRINFC_OVRHAL_RCV:
      {
         OvrHal->TemporaryRcvCompletionInfo.CompletionRoutine = CompletionInfo->CompletionRoutine;
         OvrHal->TemporaryRcvCompletionInfo.Context = CompletionInfo->Context;
         break;
      }
      case PH_FRINFC_OVRHAL_SND:
      {
         OvrHal->TemporarySndCompletionInfo.CompletionRoutine = CompletionInfo->CompletionRoutine;
         OvrHal->TemporarySndCompletionInfo.Context = CompletionInfo->Context;
         break;
      }
      default:
      {
         OvrHal->TemporaryCompletionInfo.CompletionRoutine = CompletionInfo->CompletionRoutine;
         OvrHal->TemporaryCompletionInfo.Context = CompletionInfo->Context;
         break;
      }
   }
}
