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
 * \file  phFriNfc_LlcpTransport_Connection.c
 * \brief 
 *
 * Project: NFC-FRI
 *
 */
/*include files*/
#define LOG_TAG "NFC"
#include <cutils/log.h>
#include <phOsalNfc.h>
#include <phLibNfcStatus.h>
#include <phLibNfc.h>
#include <phNfcLlcpTypes.h>
#include <phFriNfc_LlcpTransport.h>
#include <phFriNfc_LlcpTransport_Connection.h>
#include <phFriNfc_Llcp.h>
#include <phFriNfc_LlcpUtils.h>

/* Function definition */
static NFCSTATUS phFriNfc_Llcp_Send_ReceiveReady_Frame(phFriNfc_LlcpTransport_Socket_t*    pLlcpSocket);
static NFCSTATUS phFriNfc_Llcp_Send_ReceiveNotReady_Frame(phFriNfc_LlcpTransport_Socket_t*   pLlcpSocket);

static NFCSTATUS static_performSendInfo(phFriNfc_LlcpTransport_Socket_t * psLlcpSocket);
/**********   End Function definition   ***********/

/* TODO: comment functionphFriNfc_LlcpTransport_ConnectionOriented_SendLlcp_CB */
static void phFriNfc_LlcpTransport_ConnectionOriented_SendLlcp_CB(void*        pContext,
                                                                  uint8_t      socketIndex,
                                                                  NFCSTATUS    status)
{
   phFriNfc_LlcpTransport_t          *psTransport;
   phFriNfc_LlcpTransport_Socket_t    psTempLlcpSocket;
   phFriNfc_LlcpTransport_Socket_t   *psLocalLlcpSocket = NULL;
   phNfc_sData_t                     sFrmrBuffer;
   uint8_t                           index;
   uint8_t                           socketFound = FALSE;
   NFCSTATUS                         result;
   /* Get Send CB context */
   psTransport = (phFriNfc_LlcpTransport_t*)pContext;

   if(status == NFCSTATUS_SUCCESS)
   {
      /* Test the socket */
      switch(psTransport->pSocketTable[socketIndex].eSocket_State)
      {
      case phFriNfc_LlcpTransportSocket_eSocketAccepted:
         {
            /* Set socket state to Connected */
            psTransport->pSocketTable[socketIndex].eSocket_State  = phFriNfc_LlcpTransportSocket_eSocketConnected;
            /* Call the Accept Callback */
            psTransport->pSocketTable[socketIndex].pfSocketAccept_Cb(psTransport->pSocketTable[socketIndex].pAcceptContext,status);
            psTransport->pSocketTable[socketIndex].pfSocketAccept_Cb = NULL;
            psTransport->pSocketTable[socketIndex].pAcceptContext = NULL;
         }break;

      case phFriNfc_LlcpTransportSocket_eSocketRejected:
         {
            /* Store the Llcp socket in a local Llcp socket */
            psTempLlcpSocket = psTransport->pSocketTable[socketIndex];

            /* Reset the socket  and set the socket state to default */
            result = phFriNfc_LlcpTransport_Close(&psTransport->pSocketTable[socketIndex]);

            /* Call the Reject Callback */
            psTempLlcpSocket.pfSocketSend_Cb(psTempLlcpSocket.pRejectContext,status);
            psTempLlcpSocket.pfSocketSend_Cb = NULL;
         }break;

      case phFriNfc_LlcpTransportSocket_eSocketConnected:
         {
            if(!psTransport->pSocketTable[socketIndex].bSocketSendPending && psTransport->pSocketTable[socketIndex].pfSocketSend_Cb != NULL)
            {
               psTransport->pSocketTable[socketIndex].pfSocketSend_Cb(psTransport->pSocketTable[socketIndex].pSendContext,status);
               psTransport->pSocketTable[socketIndex].pfSocketSend_Cb = NULL;
            }
         }break;
      default:
         /* Nothing to do */
         break;
      }
   }
   else
   {
      /* Send CB error */
      if(!psTransport->pSocketTable[socketIndex].bSocketSendPending && psTransport->pSocketTable[socketIndex].pfSocketSend_Cb != NULL)
      {
         psTransport->pSocketTable[socketIndex].pfSocketSend_Cb(psTransport->pSocketTable[socketIndex].pSendContext,status);
         psTransport->pSocketTable[socketIndex].pfSocketSend_Cb = NULL;
      }
   }
}


NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_HandlePendingOperations(phFriNfc_LlcpTransport_Socket_t *pSocket)
{
   NFCSTATUS                  result = NFCSTATUS_FAILED;
   phFriNfc_LlcpTransport_t   *psTransport = pSocket->psTransport;
   /* I FRAME */
   if(pSocket->bSocketSendPending == TRUE)
   {
      /* Test the RW window */
      if(CHECK_SEND_RW(pSocket))
      {
         if (!testAndSetSendPending(psTransport)) {
            result = static_performSendInfo(pSocket);
            if (result != NFCSTATUS_SUCCESS && result != NFCSTATUS_PENDING) {
                clearSendPending(psTransport);
            }
         }
      }
   }
   /* RR FRAME */
   else if(pSocket->bSocketRRPending == TRUE)
   {
      /* Reset RR pending */
      pSocket->bSocketRRPending = FALSE;

      /* Send RR Frame */
      result = phFriNfc_Llcp_Send_ReceiveReady_Frame(pSocket);
   }
   /* RNR Frame */
   else if(pSocket->bSocketRNRPending == TRUE)
   {
      /* Reset RNR pending */
      pSocket->bSocketRNRPending = FALSE;

      /* Send RNR Frame */
      result = phFriNfc_Llcp_Send_ReceiveNotReady_Frame(pSocket);
   }
   /* CC Frame */
   else if(pSocket->bSocketAcceptPending == TRUE)
   {
      if (!testAndSetSendPending(psTransport))
      {
         /* Reset Accept pending */
         pSocket->bSocketAcceptPending = FALSE;

         /* Fill the psLlcpHeader stuture with the DSAP,CC PTYPE and the SSAP */
         pSocket->sLlcpHeader.dsap  = pSocket->socket_dSap;
         pSocket->sLlcpHeader.ptype = PHFRINFC_LLCP_PTYPE_CC;
         pSocket->sLlcpHeader.ssap  = pSocket->socket_sSap;

         /* Set the socket state to accepted */
         pSocket->eSocket_State = phFriNfc_LlcpTransportSocket_eSocketAccepted;

         /* Send a CC Frame */
         result =  phFriNfc_LlcpTransport_LinkSend(psTransport,
                                   &pSocket->sLlcpHeader,
                                   NULL,
                                   &pSocket->sSocketSendBuffer,
                                   phFriNfc_LlcpTransport_ConnectionOriented_SendLlcp_CB,
                                   pSocket->index,
                                   psTransport);

         if (result != NFCSTATUS_SUCCESS && result != NFCSTATUS_PENDING) {
            clearSendPending(psTransport);
         }
      }
   }
   /* CONNECT FRAME */
   else if(pSocket->bSocketConnectPending == TRUE)
   {
      if (!testAndSetSendPending(psTransport))
      {
         /* Reset Accept pending */
         pSocket->bSocketConnectPending = FALSE;

         /* Set the socket in connecting state */
         pSocket->eSocket_State = phFriNfc_LlcpTransportSocket_eSocketConnecting;

         /* send CONNECT */
         result =  phFriNfc_LlcpTransport_LinkSend(psTransport,
                                 &pSocket->sLlcpHeader,
                                 NULL,
                                 &pSocket->sSocketSendBuffer,
                                 phFriNfc_LlcpTransport_ConnectionOriented_SendLlcp_CB,
                                 pSocket->index,
                                 psTransport);

         if (result != NFCSTATUS_SUCCESS && result != NFCSTATUS_PENDING) {
            clearSendPending(psTransport);
         }
      }
   }
   /* DISC FRAME */
   else if(pSocket->bSocketDiscPending == TRUE)
   {
      if (!testAndSetSendPending(psTransport))
      {
         /* Reset Disc Pending */
         pSocket->bSocketDiscPending = FALSE;

         /* Set the socket in connecting state */
         pSocket->eSocket_State = phFriNfc_LlcpTransportSocket_eSocketDisconnecting;

         /* Send DISC */
         result =  phFriNfc_LlcpTransport_LinkSend(psTransport,
                                   &pSocket->sLlcpHeader,
                                   NULL,
                                   &pSocket->sSocketSendBuffer,
                                   phFriNfc_LlcpTransport_ConnectionOriented_SendLlcp_CB,
                                   pSocket->index,
                                   psTransport);

         if (result != NFCSTATUS_SUCCESS && result != NFCSTATUS_PENDING) {
            clearSendPending(psTransport);
         }
         /* Call ErrCB due to a DISC */
         pSocket->pSocketErrCb(pSocket->pContext, PHFRINFC_LLCP_ERR_DISCONNECTED);
      }
   }
   return result;
}

static NFCSTATUS static_performSendInfo(phFriNfc_LlcpTransport_Socket_t * psLlcpSocket)
{
   phFriNfc_LlcpTransport_t   *psTransport = psLlcpSocket->psTransport;
   NFCSTATUS                  status;

   /* Set the Header */
   psLlcpSocket->sLlcpHeader.dsap   = psLlcpSocket->socket_dSap;
   psLlcpSocket->sLlcpHeader.ptype  = PHFRINFC_LLCP_PTYPE_I;
   psLlcpSocket->sLlcpHeader.ssap   = psLlcpSocket->socket_sSap;

   /* Set Sequence Numbers */
   psLlcpSocket->sSequence.ns = psLlcpSocket->socket_VS;
   psLlcpSocket->sSequence.nr = psLlcpSocket->socket_VR;

   /* Update the VRA */
   psLlcpSocket->socket_VRA = psLlcpSocket->socket_VR;


   /* Send I_PDU */
   status =  phFriNfc_LlcpTransport_LinkSend(psTransport,
                                &psLlcpSocket->sLlcpHeader,
                                &psLlcpSocket->sSequence,
                                &psLlcpSocket->sSocketSendBuffer,
                                phFriNfc_LlcpTransport_ConnectionOriented_SendLlcp_CB,
                                psLlcpSocket->index,
                                psLlcpSocket->psTransport);
   if (status == NFCSTATUS_SUCCESS || status == NFCSTATUS_PENDING) {
      /* Update VS */
      psLlcpSocket->socket_VS = (psLlcpSocket->socket_VS+1)%16;

      /* Reset Send Pending */
      psLlcpSocket->bSocketSendPending = FALSE;
   }

   return status;
}

static void phFriNfc_LlcpTransport_ConnectionOriented_Abort(phFriNfc_LlcpTransport_Socket_t * pLlcpSocket)
{
   if (pLlcpSocket->pfSocketSend_Cb != NULL)
   {
      pLlcpSocket->pfSocketSend_Cb(pLlcpSocket->pSendContext, NFCSTATUS_ABORTED);
      pLlcpSocket->pfSocketSend_Cb = NULL;
   }
   pLlcpSocket->pSendContext = NULL;
   if (pLlcpSocket->pfSocketRecv_Cb != NULL)
   {
      pLlcpSocket->pfSocketRecv_Cb(pLlcpSocket->pRecvContext, NFCSTATUS_ABORTED);
      pLlcpSocket->pfSocketRecv_Cb = NULL;
   }
   pLlcpSocket->pRecvContext = NULL;
   if (pLlcpSocket->pfSocketAccept_Cb != NULL)
   {
      pLlcpSocket->pfSocketAccept_Cb(pLlcpSocket->pAcceptContext, NFCSTATUS_ABORTED);
      pLlcpSocket->pfSocketAccept_Cb = NULL;
   }
   pLlcpSocket->pAcceptContext = NULL;
   if (pLlcpSocket->pfSocketConnect_Cb != NULL)
   {
      pLlcpSocket->pfSocketConnect_Cb(pLlcpSocket->pConnectContext, 0, NFCSTATUS_ABORTED);
      pLlcpSocket->pfSocketConnect_Cb = NULL;
   }
   pLlcpSocket->pConnectContext = NULL;
   if (pLlcpSocket->pfSocketDisconnect_Cb != NULL)
   {
      pLlcpSocket->pfSocketDisconnect_Cb(pLlcpSocket->pDisconnectContext, NFCSTATUS_ABORTED);
      pLlcpSocket->pfSocketDisconnect_Cb = NULL;
   }
   pLlcpSocket->pDisconnectContext = NULL;

   pLlcpSocket->pfSocketRecvFrom_Cb = NULL;
   pLlcpSocket->pfSocketListen_Cb = NULL;
   pLlcpSocket->pListenContext = NULL;
}


static NFCSTATUS phFriNfc_Llcp_Send_ReceiveReady_Frame(phFriNfc_LlcpTransport_Socket_t*    pLlcpSocket)
{
   NFCSTATUS   status = NFCSTATUS_SUCCESS;

   /* Test if a send is pending */
   if(testAndSetSendPending(pLlcpSocket->psTransport))
   {
      pLlcpSocket->bSocketRRPending = TRUE;
      status = NFCSTATUS_PENDING;
   }
   else
   {
      /* Set the header of the RR frame */
      pLlcpSocket->sLlcpHeader.dsap   = pLlcpSocket->socket_dSap;
      pLlcpSocket->sLlcpHeader.ptype  = PHFRINFC_LLCP_PTYPE_RR;
      pLlcpSocket->sLlcpHeader.ssap   = pLlcpSocket->socket_sSap;

      /* Set sequence number for RR Frame */
      pLlcpSocket->sSequence.ns = 0;
      pLlcpSocket->sSequence.nr = pLlcpSocket->socket_VR;

      /* Update VRA */
      pLlcpSocket->socket_VRA = (uint8_t)pLlcpSocket->sSequence.nr;

      /* Send RR frame */
      status =  phFriNfc_LlcpTransport_LinkSend(pLlcpSocket->psTransport,
                                   &pLlcpSocket->sLlcpHeader,
                                   &pLlcpSocket->sSequence,
                                   NULL,
                                   phFriNfc_LlcpTransport_ConnectionOriented_SendLlcp_CB,
                                   pLlcpSocket->index,
                                   pLlcpSocket->psTransport);
      if (status != NFCSTATUS_SUCCESS && status != NFCSTATUS_PENDING) {
         clearSendPending(pLlcpSocket->psTransport);
      }
   }

   return status;
}

static NFCSTATUS phFriNfc_Llcp_Send_ReceiveNotReady_Frame(phFriNfc_LlcpTransport_Socket_t*   pLlcpSocket)
{
   NFCSTATUS   status = NFCSTATUS_SUCCESS;


   /* Test if a send is pending */
   if(testAndSetSendPending(pLlcpSocket->psTransport))
   {
      pLlcpSocket->bSocketRNRPending = TRUE;
      status = NFCSTATUS_PENDING;
   }
   else
   {
      /* Set the header of the RNR frame */
      pLlcpSocket->sLlcpHeader.dsap   = pLlcpSocket->socket_dSap;
      pLlcpSocket->sLlcpHeader.ptype  = PHFRINFC_LLCP_PTYPE_RNR;
      pLlcpSocket->sLlcpHeader.ssap   = pLlcpSocket->socket_sSap;

      /* Set sequence number for RNR Frame */
      pLlcpSocket->sSequence.ns = 0x00;
      pLlcpSocket->sSequence.nr = pLlcpSocket->socket_VR;

      /* Update VRA */
      pLlcpSocket->socket_VRA = (uint8_t)pLlcpSocket->sSequence.nr;

      /* Send RNR frame */
      status =  phFriNfc_LlcpTransport_LinkSend(pLlcpSocket->psTransport,
                                   &pLlcpSocket->sLlcpHeader,
                                   &pLlcpSocket->sSequence,
                                   NULL,
                                   phFriNfc_LlcpTransport_ConnectionOriented_SendLlcp_CB,
                                   pLlcpSocket->index,
                                   pLlcpSocket->psTransport);
      if (status != NFCSTATUS_SUCCESS && status != NFCSTATUS_PENDING) {
         clearSendPending(pLlcpSocket->psTransport);
      }
   }
   return status;
}

static NFCSTATUS phFriNfc_Llcp_GetSocket_Params(phNfc_sData_t                    *psParamsTLV,
                                                phNfc_sData_t                    *psServiceName,
                                                uint8_t                          *pRemoteRW_Size,
                                                uint16_t                         *pRemoteMIU)
{
   NFCSTATUS         status = NFCSTATUS_SUCCESS;
   phNfc_sData_t     sValueBuffer;
   uint32_t          offset = 0;
   uint8_t           type;
   
   /* Check for NULL pointers */
   if ((psParamsTLV == NULL) || (pRemoteRW_Size == NULL) || (pRemoteMIU == NULL))
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }
   else
   {
      /* Decode TLV */
      while (offset < psParamsTLV->length)
      {
         status = phFriNfc_Llcp_DecodeTLV(psParamsTLV, &offset, &type,&sValueBuffer);
         if (status != NFCSTATUS_SUCCESS)
         {
            /* Error: Ill-formed TLV */
            return status;
         }
         switch(type)
         {
            case PHFRINFC_LLCP_TLV_TYPE_SN:
            {
               /* Test if a SN is present in the TLV */
               if(sValueBuffer.length == 0)
               {
                  /* Error : Ill-formed SN parameter TLV */
                  break;
               }
               /* Get the Service Name */
               *psServiceName = sValueBuffer;
            }break;

            case PHFRINFC_LLCP_TLV_TYPE_RW:
            {
               /* Check length */
               if (sValueBuffer.length != PHFRINFC_LLCP_TLV_LENGTH_RW)
               {
                  /* Error : Ill-formed MIUX parameter TLV */
                  break;
               }
               *pRemoteRW_Size = sValueBuffer.buffer[0];
            }break;

            case PHFRINFC_LLCP_TLV_TYPE_MIUX:
            {
               /* Check length */
               if (sValueBuffer.length != PHFRINFC_LLCP_TLV_LENGTH_MIUX)
               {
                  /* Error : Ill-formed MIUX parameter TLV */
                  break;
               }
               *pRemoteMIU = PHFRINFC_LLCP_MIU_DEFAULT + (((sValueBuffer.buffer[0] << 8) | sValueBuffer.buffer[1]) & PHFRINFC_LLCP_TLV_MIUX_MASK);
            }break;

            default:
            {
               /* Error : Unknown type */
               break;
            }
         }
      }
   }
   return status;
}


/* TODO: comment function Handle_ConnectFrame */
static void Handle_ConnectionFrame(phFriNfc_LlcpTransport_t      *psTransport,
                                   phNfc_sData_t                 *psData,
                                   uint8_t                       dsap,
                                   uint8_t                       ssap)
{ 
   NFCSTATUS                         status = NFCSTATUS_SUCCESS;

   uint8_t                                   index;
   uint8_t                                   socketFound = FALSE;
   phFriNfc_LlcpTransport_Socket_t           *pLlcpSocket = NULL;
   phFriNfc_LlcpTransport_Socket_t           *psLocalLlcpSocket = NULL;
   pphFriNfc_LlcpTransportSocketListenCb_t   pListen_Cb = NULL;
   void                                      *pListenContext = NULL;

   phNfc_sData_t                             sServiceName;
   uint8_t                                   remoteRW  = PHFRINFC_LLCP_RW_DEFAULT;
   uint16_t                                  remoteMIU = PHFRINFC_LLCP_MIU_DEFAULT;

   status = phFriNfc_Llcp_GetSocket_Params(psData,
                                           &sServiceName,
                                           &remoteRW,
                                           &remoteMIU);

   if(status != NFCSTATUS_SUCCESS)
   {
      /* Incorrect TLV */
      /* send FRMR */ 
      status  = phFriNfc_LlcpTransport_SendFrameReject(psTransport,
                                                       dsap,
                                                       PHFRINFC_LLCP_PTYPE_CONNECT,
                                                       ssap,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00);
   }
   else
   {
      if(dsap == PHFRINFC_LLCP_SAP_SDP)
      {
         /* Search a socket with the SN */
         for(index=0;index<PHFRINFC_LLCP_NB_SOCKET_MAX;index++)
         {
            /* Test if the socket is in Listen state and if its SN is the good one */
            if(psTransport->pSocketTable[index].bSocketListenPending 
               &&  (sServiceName.length == psTransport->pSocketTable[index].sServiceName.length) 
               && !memcmp(sServiceName.buffer,psTransport->pSocketTable[index].sServiceName.buffer,sServiceName.length))
            {
               /* socket with the SN found */
               socketFound = TRUE;

               psLocalLlcpSocket = &psTransport->pSocketTable[index];

               /* Get the new ssap number, it is the ssap number of the socket found */
               dsap = psLocalLlcpSocket->socket_sSap;
               /* Get the ListenCB of the socket */
               pListen_Cb = psLocalLlcpSocket->pfSocketListen_Cb;
               pListenContext = psLocalLlcpSocket->pListenContext;
               break;
            }
         }
     } 
     else
     {
        /* Search a socket with the DSAP */
        for(index=0;index<PHFRINFC_LLCP_NB_SOCKET_MAX;index++)
        {
           /* Test if the socket is in Listen state and if its port number is the good one */
           if(psTransport->pSocketTable[index].bSocketListenPending && psTransport->pSocketTable[index].socket_sSap == dsap)
           {
              /* socket with the SN found */
              socketFound = TRUE;

              psLocalLlcpSocket = &psTransport->pSocketTable[index];

              /* Get the Listen CB and the Context of the socket */
               pListen_Cb = psLocalLlcpSocket->pfSocketListen_Cb;
               pListenContext = psLocalLlcpSocket->pListenContext;
              break;
           }
        }
     }
   }

   /* Test if a socket has beeen found */
   if(socketFound)
   {
      /* Reset the FLAG socketFound*/
      socketFound = FALSE;

      /* Search a socket free and no socket connect on this DSAP*/
      for(index=0;index<PHFRINFC_LLCP_NB_SOCKET_MAX;index++)
      {
         if(psTransport->pSocketTable[index].eSocket_State == phFriNfc_LlcpTransportSocket_eSocketDefault && socketFound != TRUE)
         {
            socketFound = TRUE;

            psTransport->pSocketTable[index].index = index;

            /* Create a communication socket */
            pLlcpSocket = &psTransport->pSocketTable[index];

            /* Set the communication option of the Remote Socket */
            pLlcpSocket->remoteMIU = remoteMIU;
            pLlcpSocket->remoteRW  = remoteRW;

            /* Set SSAP/DSAP of the new socket created for the communication with the remote */
            pLlcpSocket->socket_dSap = ssap;
            pLlcpSocket->socket_sSap = dsap;

            /* Set the state and the type of the new socket */
            pLlcpSocket->eSocket_State = phFriNfc_LlcpTransportSocket_eSocketBound;
            pLlcpSocket->eSocket_Type  = phFriNfc_LlcpTransport_eConnectionOriented;
           
         }
         else if(((psTransport->pSocketTable[index].eSocket_State == phFriNfc_LlcpTransportSocket_eSocketConnected)
                  || (psTransport->pSocketTable[index].eSocket_State == phFriNfc_LlcpTransportSocket_eSocketAccepted))
                  && ((psTransport->pSocketTable[index].socket_sSap == ssap)&&(psTransport->pSocketTable[index].socket_dSap == dsap)))
                  
         {
            socketFound = FALSE;
            
            if(pLlcpSocket != NULL)
            {
               /* Reset Socket Information */
               pLlcpSocket->remoteMIU = 0;
               pLlcpSocket->remoteRW  = 0;

               /* Set SSAP/DSAP of the new socket created for the communication with the remote */
               pLlcpSocket->socket_dSap = 0;
               pLlcpSocket->socket_sSap = 0;

               /* Set the state and the type of the new socket */
               pLlcpSocket->eSocket_State = phFriNfc_LlcpTransportSocket_eSocketDefault;
               pLlcpSocket->eSocket_Type  = phFriNfc_LlcpTransport_eDefaultType;
               break;
            }
         }
      }



      /* Test if a socket has been found */
      if(socketFound)
      {
         /* Call the Listen CB */
         pListen_Cb(pListenContext,pLlcpSocket);
      }
      else
      {
         /* No more socket are available */
         /* Send a DM (0x21) */
         status = phFriNfc_LlcpTransport_SendDisconnectMode (psTransport,
                                                             ssap,
                                                             dsap,
                                                             PHFRINFC_LLCP_DM_OPCODE_SOCKET_NOT_AVAILABLE);
      }
   }
   else
   {
      /* Service Name not found or Port number not found */
      /* Send a DM (0x02) */ 
      status = phFriNfc_LlcpTransport_SendDisconnectMode (psTransport,
                                                          ssap,
                                                          dsap,
                                                          PHFRINFC_LLCP_DM_OPCODE_SAP_NOT_FOUND);
   }
}

/* TODO: comment function Handle_ConnectFrame */
static void Handle_ConnectionCompleteFrame(phFriNfc_LlcpTransport_t      *psTransport,
                                           phNfc_sData_t                 *psData,
                                           uint8_t                       dsap,
                                           uint8_t                       ssap)
{
   NFCSTATUS                          status = NFCSTATUS_SUCCESS;
   uint8_t                            index;
   uint8_t                            remoteRW  = PHFRINFC_LLCP_RW_DEFAULT;
   uint16_t                           remoteMIU = PHFRINFC_LLCP_MIU_DEFAULT;
   uint8_t                            socketFound = FALSE;
   phFriNfc_LlcpTransport_Socket_t*   psLocalLlcpSocket = NULL;

   status = phFriNfc_Llcp_GetSocket_Params(psData,
                                           NULL,
                                           &remoteRW,
                                           &remoteMIU);

   if(status != NFCSTATUS_SUCCESS)
   {
      /* Incorrect TLV */
      /* send FRMR */ 
      status  = phFriNfc_LlcpTransport_SendFrameReject(psTransport,
                                                       dsap,
                                                       PHFRINFC_LLCP_PTYPE_CC,
                                                       ssap,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00);
   }
   else
   {
      /* Search a socket in connecting state and with the good SSAP */
      for(index=0;index<PHFRINFC_LLCP_NB_SOCKET_MAX;index++)
      {
         /* Test if the socket is in Connecting state and if its SSAP number is the good one */
         if(psTransport->pSocketTable[index].eSocket_State  == phFriNfc_LlcpTransportSocket_eSocketConnecting 
            && psTransport->pSocketTable[index].socket_sSap == dsap)
         {
            /* socket with the SN found */
            socketFound = TRUE;

            /* Update the DSAP value with the incomming Socket sSap */
            psTransport->pSocketTable[index].socket_dSap = ssap;

            /* Store a pointer to the socket found */
            psLocalLlcpSocket = &psTransport->pSocketTable[index];
            break;
         }
      }
      /* Test if a socket has been found */
      if(socketFound)
      {
         /* Set the socket state to connected */
         psLocalLlcpSocket->eSocket_State = phFriNfc_LlcpTransportSocket_eSocketConnected;

         /* Reset the socket_VS,socket_VR,socket_VSA and socket_VRA variables */
         psLocalLlcpSocket->socket_VR  = 0;
         psLocalLlcpSocket->socket_VRA = 0;
         psLocalLlcpSocket->socket_VS  = 0;
         psLocalLlcpSocket->socket_VSA = 0;

         /* Store the Remote parameters (MIU,RW) */
         psLocalLlcpSocket->remoteMIU  = remoteMIU;
         psLocalLlcpSocket->remoteRW   = remoteRW;

         /* Call the Connect CB and reset callback info */
         psLocalLlcpSocket->pfSocketConnect_Cb(psLocalLlcpSocket->pConnectContext,0x00,NFCSTATUS_SUCCESS);
         psLocalLlcpSocket->pfSocketConnect_Cb = NULL;
         psLocalLlcpSocket->pConnectContext = NULL;
      }
      else
      {
         /* No socket Active */
         /* CC Frame not handled */
      }
   }
}

/* TODO: comment function Handle_DisconnectFrame */
static void Handle_DisconnectFrame(phFriNfc_LlcpTransport_t      *psTransport,
                                   uint8_t                       dsap,
                                   uint8_t                       ssap)
{
   NFCSTATUS   status = NFCSTATUS_SUCCESS;
   uint8_t     index;
   uint8_t     socketFound = FALSE;
   phFriNfc_LlcpTransport_Socket_t*   psLocalLlcpSocket = NULL;

   /* Search a socket in connected state and the good SSAP */
   for(index=0;index<PHFRINFC_LLCP_NB_SOCKET_MAX;index++)
   {
      /* Test if the socket is in Connected state and if its SSAP number is the good one */
      if(psTransport->pSocketTable[index].eSocket_State == phFriNfc_LlcpTransportSocket_eSocketConnected 
         && psTransport->pSocketTable[index].socket_sSap == dsap)
      {
         /* socket found */
         socketFound = TRUE;

         /* Store a pointer to the socket found */
         psLocalLlcpSocket = &psTransport->pSocketTable[index];
         break;
      }
   }

   /* Test if a socket has been found */
   if(socketFound)
   {
      /* Test if a send IFRAME is pending with this socket */
      if((psLocalLlcpSocket->bSocketSendPending == TRUE) || (psLocalLlcpSocket->bSocketRecvPending == TRUE))
      {
         /* Call the send CB, a disconnect abort the send request */
         if (psLocalLlcpSocket->pfSocketSend_Cb != NULL && psLocalLlcpSocket->bSocketSendPending == TRUE)
         {
            /* Copy CB + context in local variables */
            pphFriNfc_LlcpTransportSocketSendCb_t  pfSendCb = psLocalLlcpSocket->pfSocketSend_Cb;
            void*                                  pSendContext = psLocalLlcpSocket->pSendContext;
            /* Reset CB + context */
            psLocalLlcpSocket->pfSocketSend_Cb = NULL;
            psLocalLlcpSocket->pSendContext = NULL;
            /* Perform callback */
            pfSendCb(pSendContext, NFCSTATUS_FAILED);
         }
         /* Call the send CB, a disconnect abort the receive request */
         if (psLocalLlcpSocket->pfSocketRecv_Cb != NULL && psLocalLlcpSocket->bSocketRecvPending == TRUE)
         {
            /* Copy CB + context in local variables */
            pphFriNfc_LlcpTransportSocketRecvCb_t  pfRecvCb = psLocalLlcpSocket->pfSocketRecv_Cb;
            void*                                  pRecvContext = psLocalLlcpSocket->pRecvContext;
            /* Reset CB + context */
            psLocalLlcpSocket->pfSocketRecv_Cb = NULL;
            psLocalLlcpSocket->pRecvContext = NULL;
            /* Perform callback */
            pfRecvCb(pRecvContext, NFCSTATUS_FAILED);
         }
         psLocalLlcpSocket->bSocketRecvPending = FALSE;
         psLocalLlcpSocket->bSocketSendPending = FALSE;
      }

      /* Update the socket state */
      psLocalLlcpSocket->eSocket_State = phFriNfc_LlcpTransportSocket_eSocketDisconnecting;

      /* Send a DM*/
      /* TODO: use a socket internal flag to save */
      status = phFriNfc_LlcpTransport_SendDisconnectMode(psTransport,
                                                         ssap,
                                                         dsap,
                                                         PHFRINFC_LLCP_DM_OPCODE_DISCONNECTED);

      /* Call ErrCB due to a DISC */
      psTransport->pSocketTable[index].pSocketErrCb(psTransport->pSocketTable[index].pContext, PHFRINFC_LLCP_ERR_DISCONNECTED);
   }
   else
   {
      /* No socket Active */
      /* DISC Frame not handled */
   }
}

/* TODO: comment function Handle_ConnectFrame */
static void Handle_DisconnetModeFrame(phFriNfc_LlcpTransport_t      *psTransport,
                                      phNfc_sData_t                 *psData,
                                      uint8_t                       dsap,
                                      uint8_t                       ssap)
{
   NFCSTATUS                           status = NFCSTATUS_SUCCESS;
   uint8_t                             index;
   uint8_t                             socketFound = FALSE;
   uint8_t                             dmOpCode;
   phFriNfc_LlcpTransport_Socket_t     *psLocalLlcpSocket = NULL;

   /* Test if the DM buffer is correct */
   if(psData->length != PHFRINFC_LLCP_DM_LENGTH)
   {
      /* send FRMR */
      status  = phFriNfc_LlcpTransport_SendFrameReject(psTransport,
                                                       dsap,
                                                       PHFRINFC_LLCP_PTYPE_DM,
                                                       ssap,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00,
                                                       0x00);
   }
   else
   {
      /* Search a socket waiting for a DM (Disconnecting State) */
      for(index=0;index<PHFRINFC_LLCP_NB_SOCKET_MAX;index++)
      {
         /* Test if the socket is in Disconnecting  or connecting state and if its SSAP number is the good one */
         if((psTransport->pSocketTable[index].eSocket_State   == phFriNfc_LlcpTransportSocket_eSocketDisconnecting 
            || psTransport->pSocketTable[index].eSocket_State == phFriNfc_LlcpTransportSocket_eSocketConnecting)
            && psTransport->pSocketTable[index].socket_sSap   == dsap)
         {
            /* socket found */
            socketFound = TRUE;

            /* Store a pointer to the socket found */
            psLocalLlcpSocket = &psTransport->pSocketTable[index];
            break;
         }
      }

      /* Test if a socket has been found */
      if(socketFound)
      {
         /* Set dmOpcode */
         dmOpCode = psData->buffer[0];

         switch(dmOpCode)
         {
         case PHFRINFC_LLCP_DM_OPCODE_DISCONNECTED:
            {
               /* Set the socket state to disconnected */
               psLocalLlcpSocket->eSocket_State = phFriNfc_LlcpTransportSocket_eSocketCreated;

               /* Call Disconnect CB */
               if (psLocalLlcpSocket->pfSocketDisconnect_Cb != NULL)
               {
                  psLocalLlcpSocket->pfSocketDisconnect_Cb(psLocalLlcpSocket->pDisconnectContext,NFCSTATUS_SUCCESS);
                  psLocalLlcpSocket->pfSocketDisconnect_Cb = NULL;
               }

            }break;

         case PHFRINFC_LLCP_DM_OPCODE_CONNECT_REJECTED:
         case PHFRINFC_LLCP_DM_OPCODE_CONNECT_NOT_ACCEPTED:
         case PHFRINFC_LLCP_DM_OPCODE_SAP_NOT_ACTIVE:
         case PHFRINFC_LLCP_DM_OPCODE_SAP_NOT_FOUND:
         case PHFRINFC_LLCP_DM_OPCODE_SOCKET_NOT_AVAILABLE:
            {
               /* Set the socket state to bound */
               psLocalLlcpSocket->eSocket_State = phFriNfc_LlcpTransportSocket_eSocketCreated;
               if(psLocalLlcpSocket->pfSocketConnect_Cb != NULL)
               {
                  /* Call Connect CB */
                  psLocalLlcpSocket->pfSocketConnect_Cb(psLocalLlcpSocket->pConnectContext,dmOpCode,NFCSTATUS_FAILED);
                  psLocalLlcpSocket->pfSocketConnect_Cb = NULL;
               }
            }break;
         }
      }
   }
}

/* TODO: comment function Handle_Receive_IFrame */
static void Handle_Receive_IFrame(phFriNfc_LlcpTransport_t      *psTransport,
                                  phNfc_sData_t                 *psData,
                                  uint8_t                       dsap,
                                  uint8_t                       ssap)
{
   NFCSTATUS   status = NFCSTATUS_SUCCESS;

   phFriNfc_LlcpTransport_Socket_t*    psLocalLlcpSocket = NULL;
   phFriNfc_Llcp_sPacketSequence_t    sLlcpLocalSequence;

   uint32_t    dataLengthAvailable = 0;
   uint32_t    dataLengthWrite = 0;
   uint8_t     index;
   uint8_t     socketFound = FALSE;
   uint8_t     WFlag = 0;
   uint8_t     IFlag = 0;
   uint8_t     RFlag = 0;
   uint8_t     SFlag = 0;
   uint8_t     nr_val;
   uint32_t    offset = 0;
   uint32_t    rw_offset;

   /* Get NS and NR Value of the I Frame*/
   phFriNfc_Llcp_Buffer2Sequence( psData->buffer, offset, &sLlcpLocalSequence);


   /* Update the buffer pointer */
   psData->buffer = psData->buffer + PHFRINFC_LLCP_PACKET_SEQUENCE_SIZE;

   /* Update the length value (without the header length) */
   psData->length = psData->length - PHFRINFC_LLCP_PACKET_SEQUENCE_SIZE;

   /* Search a socket waiting for an I FRAME (Connected State) */
   for(index=0;index<PHFRINFC_LLCP_NB_SOCKET_MAX;index++)
   {
      /* Test if the socket is in connected state and if its SSAP and DSAP are valid */
      if((  (psTransport->pSocketTable[index].eSocket_State == phFriNfc_LlcpTransportSocket_eSocketConnected)
         || (psTransport->pSocketTable[index].eSocket_State == phFriNfc_LlcpTransportSocket_eSocketAccepted))
         && psTransport->pSocketTable[index].socket_sSap == dsap
         && psTransport->pSocketTable[index].socket_dSap == ssap)
      {
         /* socket found */
         socketFound = TRUE;

         /* Store a pointer to the socket found */
         psLocalLlcpSocket = &psTransport->pSocketTable[index];
         break;
      }
   }

   /* Test if a socket has been found */
   if(socketFound)
   {
      /* Test NS */
      /*if(sLlcpLocalSequence.ns != psLocalLlcpSocket->socket_VR)
      {
         SFlag = TRUE;
      }*/

      /* Calculate offset of current frame in RW, and check validity */
      if(sLlcpLocalSequence.ns >= psLocalLlcpSocket->socket_VRA)
      {
         rw_offset = sLlcpLocalSequence.ns - psLocalLlcpSocket->socket_VRA;
      }
      else
      {
         rw_offset = 16 - (psLocalLlcpSocket->socket_VRA - sLlcpLocalSequence.ns);
      }
      if(rw_offset >= psLocalLlcpSocket->localRW)
      {
         /* FRMR 0x01 */
         SFlag = TRUE;
      }

      /* Check Info length */
      if(psData->length > (uint32_t)(psLocalLlcpSocket->localMIUX + PHFRINFC_LLCP_MIU_DEFAULT))
      {
         IFlag = TRUE;
      }


      /* Test NR */
      nr_val = (uint8_t)sLlcpLocalSequence.nr;
      do
      {
         if(nr_val == psLocalLlcpSocket->socket_VS)
         {
            break;
         }

         nr_val = (nr_val+1)%16;

         if(nr_val == psLocalLlcpSocket->socket_VSA)
         {
            /* FRMR 0x02 */
            RFlag = TRUE;
            break;
         }
      }while(nr_val != sLlcpLocalSequence.nr);


      if( WFlag != 0 || IFlag != 0 || RFlag != 0 || SFlag != 0)
      {
         /* Send FRMR */
         status = phFriNfc_LlcpTransport_SendFrameReject(psTransport,
                                                         dsap,
                                                         PHFRINFC_LLCP_PTYPE_I,
                                                         ssap,
                                                         &sLlcpLocalSequence,
                                                         WFlag,
                                                         IFlag,
                                                         RFlag,
                                                         SFlag,
                                                         psLocalLlcpSocket->socket_VS,
                                                         psLocalLlcpSocket->socket_VSA,
                                                         psLocalLlcpSocket->socket_VR,
                                                         psLocalLlcpSocket->socket_VRA);

      }
      else
      {
        /* Update VSA */
        psLocalLlcpSocket->socket_VSA = (uint8_t)sLlcpLocalSequence.nr;

        /* Test if the Linear Buffer length is null */
        if(psLocalLlcpSocket->bufferLinearLength == 0)
        {
            /* Test if a Receive is pending and RW empty */
            if(psLocalLlcpSocket->bSocketRecvPending == TRUE && (psLocalLlcpSocket->indexRwWrite == psLocalLlcpSocket->indexRwRead))
            {
               /* Reset Flag */
               psLocalLlcpSocket->bSocketRecvPending = FALSE;

               /* Save I_FRAME into the Receive Buffer */
               memcpy(psLocalLlcpSocket->sSocketRecvBuffer->buffer,psData->buffer,psData->length);
               psLocalLlcpSocket->sSocketRecvBuffer->length = psData->length;

               /* Update VR */
               psLocalLlcpSocket->socket_VR = (psLocalLlcpSocket->socket_VR+1)%16;

               /* Call the Receive CB */
               psLocalLlcpSocket->pfSocketRecv_Cb(psLocalLlcpSocket->pRecvContext, NFCSTATUS_SUCCESS);
               psLocalLlcpSocket->pfSocketRecv_Cb = NULL;

               /* Test if a send is pending with this socket */
               if(psLocalLlcpSocket->bSocketSendPending == TRUE && CHECK_SEND_RW(psLocalLlcpSocket))
               {
                  /* Test if a send is pending at LLC layer */
                  if(!testAndSetSendPending(psLocalLlcpSocket->psTransport))
                  {
                     status = static_performSendInfo(psLocalLlcpSocket);
                     if (status != NFCSTATUS_SUCCESS && status != NFCSTATUS_PENDING) {
                         clearSendPending(psTransport);
                     }
                  }
               }
               else
               {
                  /* RR */
                  status = phFriNfc_Llcp_Send_ReceiveReady_Frame(psLocalLlcpSocket);
               }
            }
            else
            {
               /* Test if RW is full */
               if((psLocalLlcpSocket->indexRwWrite - psLocalLlcpSocket->indexRwRead)<psLocalLlcpSocket->localRW)
               {
                  if(psLocalLlcpSocket->sSocketRwBufferTable[(psLocalLlcpSocket->indexRwWrite%psLocalLlcpSocket->localRW)].length == 0)
                  {
                     /* Save I_FRAME into the RW Buffers */
                     memcpy(psLocalLlcpSocket->sSocketRwBufferTable[(psLocalLlcpSocket->indexRwWrite%psLocalLlcpSocket->localRW)].buffer,psData->buffer,psData->length);
                     psLocalLlcpSocket->sSocketRwBufferTable[(psLocalLlcpSocket->indexRwWrite%psLocalLlcpSocket->localRW)].length = psData->length;

                     if(psLocalLlcpSocket->ReceiverBusyCondition != TRUE)
                     {
                        /* Receiver Busy condition */
                        psLocalLlcpSocket->ReceiverBusyCondition = TRUE;

                        /* Send RNR */
                        status = phFriNfc_Llcp_Send_ReceiveNotReady_Frame(psLocalLlcpSocket);
                     }
                     /* Update the RW write index */
                     psLocalLlcpSocket->indexRwWrite++;
                  }
               }
            }
        }
        else
        {
           /* Copy the buffer into the RW buffer */
           memcpy(psLocalLlcpSocket->sSocketRwBufferTable[(psLocalLlcpSocket->indexRwWrite%psLocalLlcpSocket->localRW)].buffer,psData->buffer,psData->length);

           /* Update the length */
           psLocalLlcpSocket->sSocketRwBufferTable[(psLocalLlcpSocket->indexRwWrite%psLocalLlcpSocket->localRW)].length = psData->length;

           /* Test the length of the available place in the linear buffer */
           dataLengthAvailable = phFriNfc_Llcp_CyclicFifoAvailable(&psLocalLlcpSocket->sCyclicFifoBuffer);

           if(dataLengthAvailable >= psLocalLlcpSocket->sSocketRwBufferTable[(psLocalLlcpSocket->indexRwWrite%psLocalLlcpSocket->localRW)].length)
           {
              /* Store Data into the linear buffer */
              dataLengthWrite = phFriNfc_Llcp_CyclicFifoWrite(&psLocalLlcpSocket->sCyclicFifoBuffer,
                                                              psLocalLlcpSocket->sSocketRwBufferTable[(psLocalLlcpSocket->indexRwWrite%psLocalLlcpSocket->localRW)].buffer,
                                                              psLocalLlcpSocket->sSocketRwBufferTable[(psLocalLlcpSocket->indexRwWrite%psLocalLlcpSocket->localRW)].length);

              /* Update VR */
              psLocalLlcpSocket->socket_VR = (psLocalLlcpSocket->socket_VR+1)%16;

              /* Update the length */
              psLocalLlcpSocket->sSocketRwBufferTable[(psLocalLlcpSocket->indexRwWrite%psLocalLlcpSocket->localRW)].length = 0x00;

              /* Test if a Receive Pending*/
              if(psLocalLlcpSocket->bSocketRecvPending == TRUE)
              {
                 /* Reset Flag */
                 psLocalLlcpSocket->bSocketRecvPending = FALSE;

                 phFriNfc_LlcpTransport_ConnectionOriented_Recv(psLocalLlcpSocket,
                                                                psLocalLlcpSocket->sSocketRecvBuffer,
                                                                psLocalLlcpSocket->pfSocketRecv_Cb,
                                                                psLocalLlcpSocket->pRecvContext);
              }

              /* Test if a send is pending with this socket */
              if((psLocalLlcpSocket->bSocketSendPending == TRUE) && CHECK_SEND_RW(psLocalLlcpSocket))
              {
                 /* Test if a send is pending at LLC layer */
                 if(!testAndSetSendPending(psLocalLlcpSocket->psTransport))
                 {
                    status = static_performSendInfo(psLocalLlcpSocket);
                    if (status != NFCSTATUS_SUCCESS && status != NFCSTATUS_PENDING) {
                        clearSendPending(psTransport);
                    }
                 }
              }
              else
              {
                 /* RR */
                 status = phFriNfc_Llcp_Send_ReceiveReady_Frame(psLocalLlcpSocket);
              }
           }
           else
           {
               if(psLocalLlcpSocket->ReceiverBusyCondition != TRUE)
               {
                  /* Receiver Busy condition */
                  psLocalLlcpSocket->ReceiverBusyCondition = TRUE;

                  /* Send RNR */
                  status = phFriNfc_Llcp_Send_ReceiveNotReady_Frame(psLocalLlcpSocket);
               }

              /* Update the RW write index */
              psLocalLlcpSocket->indexRwWrite++;
           }
         }
      }
   }
   else
   {
      /* No active  socket*/
      /* I FRAME not Handled */
   }
}

static void Handle_ReceiveReady_Frame(phFriNfc_LlcpTransport_t      *psTransport,
                                      phNfc_sData_t                 *psData,
                                      uint8_t                       dsap,
                                      uint8_t                       ssap)
{
   NFCSTATUS   status = NFCSTATUS_SUCCESS;
   uint8_t     index;
   uint8_t     socketFound = FALSE;
   uint8_t      WFlag = 0;
   uint8_t      IFlag = 0;
   uint8_t      RFlag = 0;
   uint8_t      SFlag = 0;
   uint32_t     offset = 0;
   uint8_t      nr_val;

   phFriNfc_LlcpTransport_Socket_t*   psLocalLlcpSocket = NULL;
   phFriNfc_Llcp_sPacketSequence_t    sLlcpLocalSequence;

   /* Get NS and NR Value of the I Frame*/
   phFriNfc_Llcp_Buffer2Sequence( psData->buffer, offset, &sLlcpLocalSequence);

   /* Search a socket waiting for an RR FRAME (Connected State) */
   for(index=0;index<PHFRINFC_LLCP_NB_SOCKET_MAX;index++)
   {
      /* Test if the socket is in connected state and if its SSAP and DSAP are valid */
      if(psTransport->pSocketTable[index].eSocket_State  == phFriNfc_LlcpTransportSocket_eSocketConnected 
         && psTransport->pSocketTable[index].socket_sSap == dsap
         && psTransport->pSocketTable[index].socket_dSap == ssap)
      {
         /* socket found */
         socketFound = TRUE;

         /* Store a pointer to the socket found */
         psLocalLlcpSocket = &psTransport->pSocketTable[index];
         psLocalLlcpSocket->index = psTransport->pSocketTable[index].index;
         break;
      }
   }

   /* Test if a socket has been found */
   if(socketFound)
   {
      /* Test NR */
      nr_val = (uint8_t)sLlcpLocalSequence.nr;
      do
      {
         if(nr_val == psLocalLlcpSocket->socket_VS)
         {
            break;
         }

         nr_val = (nr_val+1)%16;

         if(nr_val == psLocalLlcpSocket->socket_VSA)
         {
            RFlag = TRUE;
            break;
         }

      }while(nr_val != sLlcpLocalSequence.nr);


      /* Test if Info field present */
      if(psData->length > 1)
      {
         WFlag = TRUE;
         IFlag = TRUE;
      }

      if (WFlag || IFlag || RFlag || SFlag)
      {
         /* Send FRMR */
         status = phFriNfc_LlcpTransport_SendFrameReject(psTransport,
                                                         dsap, PHFRINFC_LLCP_PTYPE_RR, ssap,
                                                         &sLlcpLocalSequence,
                                                         WFlag, IFlag, RFlag, SFlag,
                                                         psLocalLlcpSocket->socket_VS,
                                                         psLocalLlcpSocket->socket_VSA,
                                                         psLocalLlcpSocket->socket_VR,
                                                         psLocalLlcpSocket->socket_VRA);
      }
      else
      {
         /* Test Receiver Busy condition */
         if(psLocalLlcpSocket->RemoteBusyConditionInfo == TRUE)
         {
            /* Notify the upper layer */
            psLocalLlcpSocket->pSocketErrCb(psLocalLlcpSocket->pContext,PHFRINFC_LLCP_ERR_NOT_BUSY_CONDITION);
            psLocalLlcpSocket->RemoteBusyConditionInfo = FALSE;
         }
         /* Update VSA */
         psLocalLlcpSocket->socket_VSA = (uint8_t)sLlcpLocalSequence.nr;

         /* Test if a send is pendind */
         if(psLocalLlcpSocket->bSocketSendPending == TRUE)
         {
            /* Test the RW window */
            if(CHECK_SEND_RW(psLocalLlcpSocket))
            {
               /* Test if a send is pending at LLC layer */
               if(!testAndSetSendPending(psLocalLlcpSocket->psTransport))
               {
                  status = static_performSendInfo(psLocalLlcpSocket);
                  if (status != NFCSTATUS_SUCCESS && status != NFCSTATUS_PENDING) {
                      clearSendPending(psTransport);
                  }
               }
            }
         }
      }
   }
   else
   {
      /* No active  socket*/
      /* RR Frame not handled*/
   }
}

static void Handle_ReceiveNotReady_Frame(phFriNfc_LlcpTransport_t      *psTransport,
                                      phNfc_sData_t                    *psData,
                                      uint8_t                          dsap,
                                      uint8_t                          ssap)
{
   NFCSTATUS   status = NFCSTATUS_SUCCESS;
   uint8_t     index;
   uint8_t     socketFound = FALSE;
   bool_t      bWFlag = 0;
   bool_t      bIFlag = 0;
   bool_t      bRFlag = 0;
   bool_t      bSFlag = 0;
   uint32_t    offset = 0;
   uint8_t     nr_val;

   phFriNfc_LlcpTransport_Socket_t*   psLocalLlcpSocket = NULL;
   phFriNfc_Llcp_sPacketSequence_t   sLlcpLocalSequence;

   /* Get NS and NR Value of the I Frame*/
   phFriNfc_Llcp_Buffer2Sequence( psData->buffer, offset, &sLlcpLocalSequence);

   /* Search a socket waiting for an RNR FRAME (Connected State) */
   for(index=0;index<PHFRINFC_LLCP_NB_SOCKET_MAX;index++)
   {
      /* Test if the socket is in connected state and if its SSAP and DSAP are valid */
      if(psTransport->pSocketTable[index].eSocket_State  == phFriNfc_LlcpTransportSocket_eSocketConnected 
         && psTransport->pSocketTable[index].socket_sSap == dsap
         && psTransport->pSocketTable[index].socket_dSap == ssap)
      {
         /* socket found */
         socketFound = TRUE;

         /* Store a pointer to the socket found */
         psLocalLlcpSocket = &psTransport->pSocketTable[index];
         break;
      }
   }

   /* Test if a socket has been found */
   if(socketFound)
   {
      /* Test NR */
      nr_val = (uint8_t)sLlcpLocalSequence.nr;
      do
      {

         if(nr_val == psLocalLlcpSocket->socket_VS)
         {
            break;
         }

         nr_val = (nr_val+1)%16;
         
         if(nr_val == psLocalLlcpSocket->socket_VSA)
         {
            /* FRMR 0x02 */
            bRFlag = TRUE;
            break;
         }
      }while(nr_val != sLlcpLocalSequence.nr);

      /* Test if Info field present */
      if(psData->length > 1)
      {
         /* Send FRMR */
         bWFlag = TRUE;
         bIFlag = TRUE;
      }

      if( bWFlag != 0 || bIFlag != 0 || bRFlag != 0 || bSFlag != 0)
      {
         /* Send FRMR */
         status = phFriNfc_LlcpTransport_SendFrameReject(psTransport,
                                                         dsap, PHFRINFC_LLCP_PTYPE_RNR, ssap,
                                                         &sLlcpLocalSequence,
                                                         bWFlag, bIFlag, bRFlag, bSFlag,
                                                         psLocalLlcpSocket->socket_VS,
                                                         psLocalLlcpSocket->socket_VSA,
                                                         psLocalLlcpSocket->socket_VR,
                                                         psLocalLlcpSocket->socket_VRA);
      }
      else
      {
         /* Notify the upper layer */
         psLocalLlcpSocket->pSocketErrCb(psTransport->pSocketTable[index].pContext,PHFRINFC_LLCP_ERR_BUSY_CONDITION);
         psLocalLlcpSocket->RemoteBusyConditionInfo = TRUE;

         /* Update VSA */
         psLocalLlcpSocket->socket_VSA = (uint8_t)sLlcpLocalSequence.nr;

         /* Test if a send is pendind */
         if(psLocalLlcpSocket->bSocketSendPending == TRUE && CHECK_SEND_RW(psLocalLlcpSocket))
         {
            /* Test if a send is pending at LLC layer */
            if(!testAndSetSendPending(psLocalLlcpSocket->psTransport))
            {
               status = static_performSendInfo(psLocalLlcpSocket);
               if (status != NFCSTATUS_SUCCESS && status != NFCSTATUS_PENDING) {
                   clearSendPending(psTransport);
               }
            }
         }
      }
   }
   else
   {
      /* No active  socket*/
      /* RNR Frame not handled*/
   }
}

static void Handle_FrameReject_Frame(phFriNfc_LlcpTransport_t      *psTransport,
                                     uint8_t                       dsap,
                                     uint8_t                       ssap)
{
   NFCSTATUS   status = NFCSTATUS_SUCCESS;
   uint8_t     index;
   uint8_t     socketFound = FALSE;

   /* Search a socket waiting for a FRAME */
   for(index=0;index<PHFRINFC_LLCP_NB_SOCKET_MAX;index++)
   {
      /* Test if the socket is in connected state and if its SSAP and DSAP are valid */
      if(psTransport->pSocketTable[index].socket_sSap == dsap
         && psTransport->pSocketTable[index].socket_dSap == ssap)
      {
         /* socket found */
         socketFound = TRUE;
         break;
      }
   }

   /* Test if a socket has been found */
   if(socketFound)
   {
      /* Set socket state to disconnected */
      psTransport->pSocketTable[index].eSocket_State =  phFriNfc_LlcpTransportSocket_eSocketDisconnected;

      /* Call ErrCB due to a FRMR*/
      psTransport->pSocketTable[index].pSocketErrCb( psTransport->pSocketTable[index].pContext,PHFRINFC_LLCP_ERR_FRAME_REJECTED);

      /* Close the socket */
      status = phFriNfc_LlcpTransport_ConnectionOriented_Close(&psTransport->pSocketTable[index]);
   }
   else
   {
      /* No active  socket*/
      /* FRMR Frame not handled*/
   }
}

/* TODO: comment function Handle_ConnectionOriented_IncommingFrame */
void Handle_ConnectionOriented_IncommingFrame(phFriNfc_LlcpTransport_t           *psTransport,
                                              phNfc_sData_t                      *psData,
                                              uint8_t                            dsap,
                                              uint8_t                            ptype,
                                              uint8_t                            ssap)
{
   phFriNfc_Llcp_sPacketSequence_t  sSequence = {0,0};

   switch(ptype)
   {
      case PHFRINFC_LLCP_PTYPE_CONNECT:
         {
            Handle_ConnectionFrame(psTransport,
                                   psData,
                                   dsap,
                                   ssap);
         }break;

      case PHFRINFC_LLCP_PTYPE_DISC:
         {
            Handle_DisconnectFrame(psTransport,
                                   dsap,
                                   ssap);
         }break;

      case PHFRINFC_LLCP_PTYPE_CC:
         {
            Handle_ConnectionCompleteFrame(psTransport,
                                           psData,
                                           dsap,
                                           ssap);
         }break;

      case PHFRINFC_LLCP_PTYPE_DM:
         {
            Handle_DisconnetModeFrame(psTransport,
                                      psData,
                                      dsap,
                                      ssap);
         }break;

      case PHFRINFC_LLCP_PTYPE_FRMR:
         {
            Handle_FrameReject_Frame(psTransport,
                                     dsap,
                                     ssap);
         }break;

      case PHFRINFC_LLCP_PTYPE_I:
         {
            Handle_Receive_IFrame(psTransport,
                                  psData,
                                  dsap,
                                  ssap);
         }break;

      case PHFRINFC_LLCP_PTYPE_RR:
         {
            Handle_ReceiveReady_Frame(psTransport,
                                      psData,
                                      dsap,
                                      ssap);
         }break;

      case PHFRINFC_LLCP_PTYPE_RNR:
         {
            Handle_ReceiveNotReady_Frame(psTransport,
                                         psData,
                                         dsap,
                                         ssap);
         }break;

      case PHFRINFC_LLCP_PTYPE_RESERVED1:
      case PHFRINFC_LLCP_PTYPE_RESERVED2:
      case PHFRINFC_LLCP_PTYPE_RESERVED3:
         {
            phFriNfc_LlcpTransport_SendFrameReject( psTransport,
                                                    dsap, ptype, ssap,
                                                    &sSequence,
                                                    TRUE, FALSE, FALSE, FALSE,
                                                    0, 0, 0, 0);
         }break;
   }
}

/**
* \ingroup grp_lib_nfc
* \brief <b>Get the local options of a socket</b>.
*
* This function returns the local options (maximum packet size and receive window size) used
* for a given connection-oriented socket. This function shall not be used with connectionless
* sockets.
*
* \param[out] pLlcpSocket           A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  psLocalOptions        A pointer to be filled with the local options of the socket.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_SocketGetLocalOptions(phFriNfc_LlcpTransport_Socket_t  *pLlcpSocket,
                                                                          phLibNfc_Llcp_sSocketOptions_t   *psLocalOptions)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;

   /* Get Local MIUX */
   psLocalOptions->miu = pLlcpSocket->sSocketOption.miu;

   /* Get Local Receive Window */
   psLocalOptions->rw = pLlcpSocket->sSocketOption.rw;

   return status;
}

/**
* \ingroup grp_lib_nfc
* \brief <b>Get the local options of a socket</b>.
*
* This function returns the remote options (maximum packet size and receive window size) used
* for a given connection-oriented socket. This function shall not be used with connectionless
* sockets.
*
* \param[out] pLlcpSocket           A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  psRemoteOptions       A pointer to be filled with the remote options of the socket.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_SocketGetRemoteOptions(phFriNfc_LlcpTransport_Socket_t*   pLlcpSocket,
                                                                           phLibNfc_Llcp_sSocketOptions_t*    psRemoteOptions)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;

   /* Get Remote MIUX */
   psRemoteOptions->miu = pLlcpSocket->remoteMIU;

   /* Get Remote  Receive Window */
   psRemoteOptions->rw = pLlcpSocket->remoteRW;

   return status;
}


/**
* \ingroup grp_fri_nfc
* \brief <b>Listen for incoming connection requests on a socket</b>.
*
* This function switches a socket into a listening state and registers a callback on
* incoming connection requests. In this state, the socket is not able to communicate
* directly. The listening state is only available for connection-oriented sockets
* which are still not connected. The socket keeps listening until it is closed, and
* thus can trigger several times the pListen_Cb callback.
*
*
* \param[in]  pLlcpSocket        A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  pListen_Cb         The callback to be called each time the
*                                socket receive a connection request.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state to switch
*                                            to listening state.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_Listen(phFriNfc_LlcpTransport_Socket_t*          pLlcpSocket,
                                                           pphFriNfc_LlcpTransportSocketListenCb_t   pListen_Cb,
                                                           void*                                     pContext)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;
   uint8_t   index;

   /* Store the listen callback */
   pLlcpSocket->pfSocketListen_Cb = pListen_Cb;

   /* store the context */
   pLlcpSocket->pListenContext = pContext;

   /* Set RecvPending to TRUE */
   pLlcpSocket->bSocketListenPending = TRUE;

   /* Set the socket state*/
   pLlcpSocket->eSocket_State = phFriNfc_LlcpTransportSocket_eSocketRegistered;

   return status;
}

/**
* \ingroup grp_fri_nfc
* \brief <b>Accept an incoming connection request for a socket</b>.
*
* This functions allows the client to accept an incoming connection request.
* It must be used with the socket provided within the listen callback. The socket
* is implicitly switched to the connected state when the function is called.
*
* \param[in]  pLlcpSocket           A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  psOptions             The options to be used with the socket.
* \param[in]  psWorkingBuffer       A working buffer to be used by the library.
* \param[in]  pErr_Cb               The callback to be called each time the accepted socket
*                                   is in error.
* \param[in]  pAccept_RspCb         The callback to be called when the Accept operation is completed
* \param[in]  pContext              Upper layer context to be returned in the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_BUFFER_TOO_SMALL         The working buffer is too small for the MIU and RW
*                                            declared in the options.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_Accept(phFriNfc_LlcpTransport_Socket_t*             pLlcpSocket,
                                                           phFriNfc_LlcpTransport_sSocketOptions_t*     psOptions,
                                                           phNfc_sData_t*                               psWorkingBuffer,
                                                           pphFriNfc_LlcpTransportSocketErrCb_t         pErr_Cb,
                                                           pphFriNfc_LlcpTransportSocketAcceptCb_t      pAccept_RspCb,
                                                           void*                                        pContext)

{
   NFCSTATUS status = NFCSTATUS_SUCCESS;

   uint32_t offset = 0;
   uint8_t miux[2];
   uint8_t  i;
   /* Store the options in the socket */
   memcpy(&pLlcpSocket->sSocketOption, psOptions, sizeof(phFriNfc_LlcpTransport_sSocketOptions_t));

   /* Set socket local params (MIUX & RW) */
   pLlcpSocket ->localMIUX = (pLlcpSocket->sSocketOption.miu - PHFRINFC_LLCP_MIU_DEFAULT) & PHFRINFC_LLCP_TLV_MIUX_MASK;
   pLlcpSocket ->localRW   = pLlcpSocket->sSocketOption.rw & PHFRINFC_LLCP_TLV_RW_MASK;

   /* Set the pointer and the length for the Receive Window Buffer */
   for(i=0;i<pLlcpSocket->localRW;i++)
   {
      pLlcpSocket->sSocketRwBufferTable[i].buffer = psWorkingBuffer->buffer + (i*pLlcpSocket->sSocketOption.miu);
      pLlcpSocket->sSocketRwBufferTable[i].length = 0;
   }

   /* Set the pointer and the length for the Send Buffer */
   pLlcpSocket->sSocketSendBuffer.buffer     = psWorkingBuffer->buffer + pLlcpSocket->bufferRwMaxLength;
   pLlcpSocket->sSocketSendBuffer.length     = pLlcpSocket->bufferSendMaxLength;
  
   /* Set the pointer and the length for the Linear Buffer */
   pLlcpSocket->sSocketLinearBuffer.buffer   = psWorkingBuffer->buffer + pLlcpSocket->bufferRwMaxLength + pLlcpSocket->bufferSendMaxLength;
   pLlcpSocket->sSocketLinearBuffer.length   = pLlcpSocket->bufferLinearLength;

   if(pLlcpSocket->sSocketLinearBuffer.length != 0)
   {
      /* Init Cyclic Fifo */
      phFriNfc_Llcp_CyclicFifoInit(&pLlcpSocket->sCyclicFifoBuffer,
                                   pLlcpSocket->sSocketLinearBuffer.buffer,
                                   pLlcpSocket->sSocketLinearBuffer.length);
   }

   pLlcpSocket->pSocketErrCb            = pErr_Cb;
   pLlcpSocket->pContext                = pContext;
   
   /* store the pointer to the Accept callback */
   pLlcpSocket->pfSocketAccept_Cb   = pAccept_RspCb;
   pLlcpSocket->pAcceptContext      = pContext;

   /* Reset the socket_VS,socket_VR,socket_VSA and socket_VRA variables */
   pLlcpSocket->socket_VR  = 0;
   pLlcpSocket->socket_VRA = 0;
   pLlcpSocket->socket_VS  = 0;
   pLlcpSocket->socket_VSA = 0;

   /* MIUX */
   if(pLlcpSocket->localMIUX != PHFRINFC_LLCP_MIUX_DEFAULT)
   {
      /* Encode MIUX value */
      phFriNfc_Llcp_EncodeMIUX(pLlcpSocket->localMIUX,
                               miux);

      /* Encode MIUX in TLV format */
      status =  phFriNfc_Llcp_EncodeTLV(&pLlcpSocket->sSocketSendBuffer,
                                        &offset,
                                        PHFRINFC_LLCP_TLV_TYPE_MIUX,
                                        PHFRINFC_LLCP_TLV_LENGTH_MIUX,
                                        miux);
      if(status != NFCSTATUS_SUCCESS)
      {
         /* Call the CB */
         status = PHNFCSTVAL(CID_FRI_NFC_LLCP_TRANSPORT, NFCSTATUS_FAILED);
         goto clean_and_return;
      }
   }

   /* Receive Window */
   if(pLlcpSocket->sSocketOption.rw != PHFRINFC_LLCP_RW_DEFAULT)
   {
      /* Encode RW value */
      phFriNfc_Llcp_EncodeRW(&pLlcpSocket->sSocketOption.rw);

      /* Encode RW in TLV format */
      status =  phFriNfc_Llcp_EncodeTLV(&pLlcpSocket->sSocketSendBuffer,
                                        &offset,
                                        PHFRINFC_LLCP_TLV_TYPE_RW,
                                        PHFRINFC_LLCP_TLV_LENGTH_RW,
                                        &pLlcpSocket->sSocketOption.rw);
      if(status != NFCSTATUS_SUCCESS)
      {
         /* Call the CB */
         status = PHNFCSTVAL(CID_FRI_NFC_LLCP_TRANSPORT, NFCSTATUS_FAILED);
         goto clean_and_return;
      }
   }


   /* Test if a send is pending */
   if(testAndSetSendPending(pLlcpSocket->psTransport))
   {
      pLlcpSocket->bSocketAcceptPending = TRUE;

      /* Update Send Buffer length value */
      pLlcpSocket->sSocketSendBuffer.length = offset;

      status = NFCSTATUS_PENDING;
   }
   else
   {
      /* Fill the psLlcpHeader stuture with the DSAP,CC PTYPE and the SSAP */
      pLlcpSocket->sLlcpHeader.dsap  = pLlcpSocket->socket_dSap;
      pLlcpSocket->sLlcpHeader.ptype = PHFRINFC_LLCP_PTYPE_CC;
      pLlcpSocket->sLlcpHeader.ssap  = pLlcpSocket->socket_sSap;

      /* Set the socket state to accepted */
      pLlcpSocket->eSocket_State           = phFriNfc_LlcpTransportSocket_eSocketAccepted;

      /* Update Send Buffer length value */
      pLlcpSocket->sSocketSendBuffer.length = offset;

      /* Send a CC Frame */
      status =  phFriNfc_LlcpTransport_LinkSend(pLlcpSocket->psTransport,
                                   &pLlcpSocket->sLlcpHeader,
                                   NULL,
                                   &pLlcpSocket->sSocketSendBuffer,
                                   phFriNfc_LlcpTransport_ConnectionOriented_SendLlcp_CB,
                                   pLlcpSocket->index,
                                   pLlcpSocket->psTransport);
      if (status != NFCSTATUS_SUCCESS && status != NFCSTATUS_PENDING) {
          clearSendPending(pLlcpSocket->psTransport);
      }
   }

clean_and_return:
   if(status != NFCSTATUS_PENDING)
   {
      LLCP_PRINT("Release Accept callback");
      pLlcpSocket->pfSocketAccept_Cb = NULL;
      pLlcpSocket->pAcceptContext = NULL;
   }

   return status;
}

 /**
* \ingroup grp_fri_nfc
* \brief <b>Reject an incoming connection request for a socket</b>.
*
* This functions allows the client to reject an incoming connection request.
* It must be used with the socket provided within the listen callback. The socket
* is implicitly closed when the function is called.
*
* \param[in]  pLlcpSocket           A pointer to a phFriNfc_LlcpTransport_Socket_t.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phLibNfc_LlcpTransport_ConnectionOriented_Reject( phFriNfc_LlcpTransport_Socket_t*           pLlcpSocket,
                                                            pphFriNfc_LlcpTransportSocketRejectCb_t   pReject_RspCb,
                                                            void                                      *pContext)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;

   /* Set the state of the socket */
   pLlcpSocket->eSocket_State   = phFriNfc_LlcpTransportSocket_eSocketRejected;

   /* Store the Reject callback */
   pLlcpSocket->pfSocketSend_Cb = pReject_RspCb;
   pLlcpSocket->pRejectContext  = pContext;

   /* Send a DM*/
   status = phFriNfc_LlcpTransport_SendDisconnectMode(pLlcpSocket->psTransport,
                                                      pLlcpSocket->socket_dSap,
                                                      pLlcpSocket->socket_sSap,
                                                      PHFRINFC_LLCP_DM_OPCODE_CONNECT_REJECTED);

   return status;
}

/**
* \ingroup grp_fri_nfc
* \brief <b>Try to establish connection with a socket on a remote SAP</b>.
*
* This function tries to connect to a given SAP on the remote peer. If the
* socket is not bound to a local SAP, it is implicitly bound to a free SAP.
*
* \param[in]  pLlcpSocket        A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  nSap               The destination SAP to connect to.
* \param[in]  psUri              The URI corresponding to the destination SAP to connect to.
* \param[in]  pConnect_RspCb     The callback to be called when the connection
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Connection operation is in progress,
*                                            pConnect_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_Connect( phFriNfc_LlcpTransport_Socket_t*           pLlcpSocket,
                                                             uint8_t                                    nSap,
                                                             phNfc_sData_t*                             psUri,
                                                             pphFriNfc_LlcpTransportSocketConnectCb_t   pConnect_RspCb,
                                                             void*                                      pContext)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;
   uint32_t offset = 0;
   uint8_t miux[2];

   /* Test if a nSap is present */
   if(nSap != PHFRINFC_LLCP_SAP_DEFAULT)
   {
      /* Set DSAP port number with the nSap value */
      pLlcpSocket->socket_dSap = nSap;
   }
   else
   {
      /* Set DSAP port number with the SDP port number */
      pLlcpSocket->socket_dSap = PHFRINFC_LLCP_SAP_SDP;
   }

   /* Store the Connect callback and context */
   pLlcpSocket->pfSocketConnect_Cb = pConnect_RspCb;
   pLlcpSocket->pConnectContext = pContext;

   /* Set the socket Header */
   pLlcpSocket->sLlcpHeader.dsap  = pLlcpSocket->socket_dSap;
   pLlcpSocket->sLlcpHeader.ptype = PHFRINFC_LLCP_PTYPE_CONNECT;
   pLlcpSocket->sLlcpHeader.ssap  = pLlcpSocket->socket_sSap;

   /* MIUX */
   if(pLlcpSocket->localMIUX != PHFRINFC_LLCP_MIUX_DEFAULT)
   {
      /* Encode MIUX value */
      phFriNfc_Llcp_EncodeMIUX(pLlcpSocket->localMIUX,
                               miux);

      /* Encode MIUX in TLV format */
      status =  phFriNfc_Llcp_EncodeTLV(&pLlcpSocket->sSocketSendBuffer,
                                        &offset,
                                        PHFRINFC_LLCP_TLV_TYPE_MIUX,
                                        PHFRINFC_LLCP_TLV_LENGTH_MIUX,
                                        miux);
      if(status != NFCSTATUS_SUCCESS)
      {
         status = PHNFCSTVAL(CID_FRI_NFC_LLCP_TRANSPORT, NFCSTATUS_FAILED);
         goto clean_and_return;
      }
   }

   /* Receive Window */
   if(pLlcpSocket->sSocketOption.rw != PHFRINFC_LLCP_RW_DEFAULT)
   {
      /* Encode RW value */
      phFriNfc_Llcp_EncodeRW(&pLlcpSocket->sSocketOption.rw);

      /* Encode RW in TLV format */
      status =  phFriNfc_Llcp_EncodeTLV(&pLlcpSocket->sSocketSendBuffer,
                                        &offset,
                                        PHFRINFC_LLCP_TLV_TYPE_RW,
                                        PHFRINFC_LLCP_TLV_LENGTH_RW,
                                        &pLlcpSocket->sSocketOption.rw);
      if(status != NFCSTATUS_SUCCESS)
      {
         status = PHNFCSTVAL(CID_FRI_NFC_LLCP_TRANSPORT, NFCSTATUS_FAILED);
         goto clean_and_return;
      }
   }

   /* Test if a Service Name is present */
   if(psUri != NULL)
   {
      /* Encode SN in TLV format */
      status =  phFriNfc_Llcp_EncodeTLV(&pLlcpSocket->sSocketSendBuffer,
                                        &offset,
                                        PHFRINFC_LLCP_TLV_TYPE_SN,
                                        (uint8_t)psUri->length,
                                        psUri->buffer);
      if(status != NFCSTATUS_SUCCESS)
      {
         status = PHNFCSTVAL(CID_FRI_NFC_LLCP_TRANSPORT, NFCSTATUS_FAILED);
         goto clean_and_return;
      }
   }

   /* Test if a send is pending */
   if(testAndSetSendPending(pLlcpSocket->psTransport))
   {
      pLlcpSocket->bSocketConnectPending =  TRUE;

      /* Update Send Buffer length value */
      pLlcpSocket->sSocketSendBuffer.length = offset;

      status = NFCSTATUS_PENDING;
   }
   else
   {
      /* Update Send Buffer length value */
      pLlcpSocket->sSocketSendBuffer.length = offset;

      /* Set the socket in connecting state */
      pLlcpSocket->eSocket_State = phFriNfc_LlcpTransportSocket_eSocketConnecting;

      status =  phFriNfc_LlcpTransport_LinkSend(pLlcpSocket->psTransport,
                                   &pLlcpSocket->sLlcpHeader,
                                   NULL,
                                   &pLlcpSocket->sSocketSendBuffer,
                                   phFriNfc_LlcpTransport_ConnectionOriented_SendLlcp_CB,
                                   pLlcpSocket->index,
                                   pLlcpSocket->psTransport);
      if (status != NFCSTATUS_SUCCESS && status != NFCSTATUS_PENDING) {
          clearSendPending(pLlcpSocket->psTransport);
      }
   }

clean_and_return:
   if(status != NFCSTATUS_PENDING)
   {
      LLCP_PRINT("Release Connect callback");
      pLlcpSocket->pfSocketConnect_Cb = NULL;
      pLlcpSocket->pConnectContext = NULL;
   }

   return status;
}


/**
* \ingroup grp_lib_nfc
* \brief <b>Disconnect a currently connected socket</b>.
*
* This function initiates the disconnection of a previously connected socket.
*
* \param[in]  pLlcpSocket        A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  pDisconnect_RspCb  The callback to be called when the 
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Disconnection operation is in progress,
*                                            pDisconnect_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phLibNfc_LlcpTransport_ConnectionOriented_Disconnect(phFriNfc_LlcpTransport_Socket_t*           pLlcpSocket,
                                                               pphLibNfc_LlcpSocketDisconnectCb_t         pDisconnect_RspCb,
                                                               void*                                      pContext)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;

   /* Store the Disconnect callback  and context*/
   pLlcpSocket->pfSocketDisconnect_Cb = pDisconnect_RspCb;
   pLlcpSocket->pDisconnectContext = pContext;

   /* Set the socket in connecting state */
   pLlcpSocket->eSocket_State = phFriNfc_LlcpTransportSocket_eSocketDisconnecting;

   /* Test if a send IFRAME is pending with this socket */
   if((pLlcpSocket->bSocketSendPending == TRUE) || (pLlcpSocket->bSocketRecvPending == TRUE))
   {
      pLlcpSocket->bSocketSendPending = FALSE;
      pLlcpSocket->bSocketRecvPending = FALSE;

      /* Call the send CB, a disconnect abort the send request */
      if (pLlcpSocket->pfSocketSend_Cb != NULL)
      {
         /* Copy CB + context in local variables */
         pphFriNfc_LlcpTransportSocketSendCb_t  pfSendCb     = pLlcpSocket->pfSocketSend_Cb;
         void*                                  pSendContext = pLlcpSocket->pSendContext;
         /* Reset CB + context */
         pLlcpSocket->pfSocketSend_Cb = NULL;
         pLlcpSocket->pSendContext = NULL;
         /* Perform callback */
         pfSendCb(pSendContext, NFCSTATUS_FAILED);
      }
      /* Call the send CB, a disconnect abort the receive request */
      if (pLlcpSocket->pfSocketRecv_Cb != NULL)
      {
         /* Copy CB + context in local variables */
         pphFriNfc_LlcpTransportSocketRecvCb_t  pfRecvCb     = pLlcpSocket->pfSocketRecv_Cb;
         void*                                  pRecvContext = pLlcpSocket->pRecvContext;
         /* Reset CB + context */
         pLlcpSocket->pfSocketRecv_Cb = NULL;
         pLlcpSocket->pRecvContext = NULL;
         /* Perform callback */
         pfRecvCb(pRecvContext, NFCSTATUS_FAILED);
       }
   }

   /* Test if a send is pending */
   if( testAndSetSendPending(pLlcpSocket->psTransport))
   {
      pLlcpSocket->bSocketDiscPending =  TRUE;
      status = NFCSTATUS_PENDING;
   }
   else
   {
      /* Set the socket Header */
      pLlcpSocket->sLlcpHeader.dsap  = pLlcpSocket->socket_dSap;
      pLlcpSocket->sLlcpHeader.ptype = PHFRINFC_LLCP_PTYPE_DISC;
      pLlcpSocket->sLlcpHeader.ssap  = pLlcpSocket->socket_sSap;

      status =  phFriNfc_LlcpTransport_LinkSend(pLlcpSocket->psTransport,
                                   &pLlcpSocket->sLlcpHeader,
                                   NULL,
                                   NULL,
                                   phFriNfc_LlcpTransport_ConnectionOriented_SendLlcp_CB,
                                   pLlcpSocket->index,
                                   pLlcpSocket->psTransport);
      if(status != NFCSTATUS_SUCCESS && status != NFCSTATUS_PENDING)
      {
         clearSendPending(pLlcpSocket->psTransport);
         LLCP_PRINT("Release Disconnect callback");
         pLlcpSocket->pfSocketDisconnect_Cb = NULL;
         pLlcpSocket->pDisconnectContext = NULL;
      }
   }

   return status;
}

/* TODO: comment function phFriNfc_LlcpTransport_Connectionless_SendTo_CB */
static void phFriNfc_LlcpTransport_ConnectionOriented_DisconnectClose_CB(void*        pContext,
                                                                         NFCSTATUS    status)
{
   phFriNfc_LlcpTransport_Socket_t   *pLlcpSocket = (phFriNfc_LlcpTransport_Socket_t*)pContext;

   if(status == NFCSTATUS_SUCCESS)
   {
      /* Reset the pointer to the socket closed */
      pLlcpSocket->eSocket_State                      = phFriNfc_LlcpTransportSocket_eSocketDefault;
      pLlcpSocket->eSocket_Type                       = phFriNfc_LlcpTransport_eDefaultType;
      pLlcpSocket->pContext                           = NULL;
      pLlcpSocket->pSocketErrCb                       = NULL;
      pLlcpSocket->socket_sSap                        = PHFRINFC_LLCP_SAP_DEFAULT;
      pLlcpSocket->socket_dSap                        = PHFRINFC_LLCP_SAP_DEFAULT;
      pLlcpSocket->bSocketRecvPending                 = FALSE;
      pLlcpSocket->bSocketSendPending                 = FALSE;
      pLlcpSocket->bSocketListenPending               = FALSE;
      pLlcpSocket->bSocketDiscPending                 = FALSE;
      pLlcpSocket->socket_VS                          = 0;
      pLlcpSocket->socket_VSA                         = 0;
      pLlcpSocket->socket_VR                          = 0;
      pLlcpSocket->socket_VRA                         = 0;

      pLlcpSocket->indexRwRead                        = 0;
      pLlcpSocket->indexRwWrite                       = 0;

      phFriNfc_LlcpTransport_ConnectionOriented_Abort(pLlcpSocket);

      memset(&pLlcpSocket->sSocketOption, 0x00, sizeof(phFriNfc_LlcpTransport_sSocketOptions_t));

      if (pLlcpSocket->sServiceName.buffer != NULL) {
          phOsalNfc_FreeMemory(pLlcpSocket->sServiceName.buffer);
      }
      pLlcpSocket->sServiceName.buffer = NULL;
      pLlcpSocket->sServiceName.length = 0;
   }
   else
   {
      /* Disconnect close Error */
   }
}

/**
* \ingroup grp_fri_nfc
* \brief <b>Close a socket on a LLCP-connected device</b>.
*
* This function closes a LLCP socket previously created using phFriNfc_LlcpTransport_Socket.
* If the socket was connected, it is first disconnected, and then closed.
*
* \param[in]  pLlcpSocket                    A pointer to a phFriNfc_LlcpTransport_Socket_t.

* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_Close(phFriNfc_LlcpTransport_Socket_t*   pLlcpSocket)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;

   if(pLlcpSocket->eSocket_State == phFriNfc_LlcpTransportSocket_eSocketConnected)
   {
      status = phLibNfc_LlcpTransport_ConnectionOriented_Disconnect(pLlcpSocket,
                                                                    phFriNfc_LlcpTransport_ConnectionOriented_DisconnectClose_CB,
                                                                    pLlcpSocket);
   }
   else
   {
      LLCP_PRINT("Socket not connected, no need to disconnect");
      /* Reset the pointer to the socket closed */
      pLlcpSocket->eSocket_State                      = phFriNfc_LlcpTransportSocket_eSocketDefault;
      pLlcpSocket->eSocket_Type                       = phFriNfc_LlcpTransport_eDefaultType;
      pLlcpSocket->pContext                           = NULL;
      pLlcpSocket->pSocketErrCb                       = NULL;
      pLlcpSocket->socket_sSap                        = PHFRINFC_LLCP_SAP_DEFAULT;
      pLlcpSocket->socket_dSap                        = PHFRINFC_LLCP_SAP_DEFAULT;
      pLlcpSocket->bSocketRecvPending                 = FALSE;
      pLlcpSocket->bSocketSendPending                 = FALSE;
      pLlcpSocket->bSocketListenPending               = FALSE;
      pLlcpSocket->bSocketDiscPending                 = FALSE;
      pLlcpSocket->RemoteBusyConditionInfo            = FALSE;
      pLlcpSocket->ReceiverBusyCondition              = FALSE;
      pLlcpSocket->socket_VS                          = 0;
      pLlcpSocket->socket_VSA                         = 0;
      pLlcpSocket->socket_VR                          = 0;
      pLlcpSocket->socket_VRA                         = 0;

      pLlcpSocket->indexRwRead                        = 0;
      pLlcpSocket->indexRwWrite                       = 0;

      phFriNfc_LlcpTransport_ConnectionOriented_Abort(pLlcpSocket);

      memset(&pLlcpSocket->sSocketOption, 0x00, sizeof(phFriNfc_LlcpTransport_sSocketOptions_t));

      if (pLlcpSocket->sServiceName.buffer != NULL) {
          phOsalNfc_FreeMemory(pLlcpSocket->sServiceName.buffer);
      }
      pLlcpSocket->sServiceName.buffer = NULL;
      pLlcpSocket->sServiceName.length = 0;
   }
   return NFCSTATUS_SUCCESS;
} 


/**
* \ingroup grp_fri_nfc
* \brief <b>Send data on a socket</b>.
*
* This function is used to write data on a socket. This function
* can only be called on a connection-oriented socket which is already
* in a connected state.
* 
*
* \param[in]  pLlcpSocket        A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  psBuffer           The buffer containing the data to send.
* \param[in]  pSend_RspCb        The callback to be called when the 
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Reception operation is in progress,
*                                            pSend_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_Send(phFriNfc_LlcpTransport_Socket_t*             pLlcpSocket,
                                                         phNfc_sData_t*                               psBuffer,
                                                         pphFriNfc_LlcpTransportSocketSendCb_t        pSend_RspCb,
                                                         void*                                        pContext)
{
   NFCSTATUS status = NFCSTATUS_SUCCESS;


   /* Test the RW window */
   if(!CHECK_SEND_RW(pLlcpSocket))
   {
      /* Store the Send CB and context */
      pLlcpSocket->pfSocketSend_Cb    = pSend_RspCb;
      pLlcpSocket->pSendContext       = pContext;

      /* Set Send pending */
      pLlcpSocket->bSocketSendPending = TRUE;

      /* Store send buffer pointer */
      pLlcpSocket->sSocketSendBuffer = *psBuffer;

      /* Set status */
      status = NFCSTATUS_PENDING;
   }
   else
   {
      /* Store send buffer pointer */
      pLlcpSocket->sSocketSendBuffer = *psBuffer;

      /* Store the Send CB and context */
      pLlcpSocket->pfSocketSend_Cb    = pSend_RspCb;
      pLlcpSocket->pSendContext       = pContext;

      /* Test if a send is pending */
      if(testAndSetSendPending(pLlcpSocket->psTransport))
      {
         /* Set Send pending */
         pLlcpSocket->bSocketSendPending = TRUE;

         /* Set status */
         status = NFCSTATUS_PENDING;
      }
      else
      {
         /* Store the Send CB and context */
         pLlcpSocket->pfSocketSend_Cb    = pSend_RspCb;
         pLlcpSocket->pSendContext       = pContext;

         status = static_performSendInfo(pLlcpSocket);

         if(status != NFCSTATUS_SUCCESS && status != NFCSTATUS_PENDING)
         {
            clearSendPending(pLlcpSocket->psTransport);
            LLCP_PRINT("Release Send callback");
            pLlcpSocket->pfSocketSend_Cb = NULL;
            pLlcpSocket->pSendContext = NULL;
         }
      }

   }
   return status;
}


 /**
* \ingroup grp_fri_nfc
* \brief <b>Read data on a socket</b>.
*
* This function is used to read data from a socket. It reads at most the
* size of the reception buffer, but can also return less bytes if less bytes
* are available. If no data is available, the function will be pending until
* more data comes, and the response will be sent by the callback. This function
* can only be called on a connection-oriented socket.
* 
*
* \param[in]  pLlcpSocket        A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  psBuffer           The buffer receiving the data.
* \param[in]  pRecv_RspCb        The callback to be called when the 
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Reception operation is in progress,
*                                            pRecv_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_Recv( phFriNfc_LlcpTransport_Socket_t*             pLlcpSocket,
                                                          phNfc_sData_t*                               psBuffer,
                                                          pphFriNfc_LlcpTransportSocketRecvCb_t        pRecv_RspCb,
                                                          void*                                        pContext)
{
   NFCSTATUS   status = NFCSTATUS_SUCCESS;
   uint32_t    dataLengthStored = 0;
   uint32_t    dataLengthAvailable = 0; 
   uint32_t    dataLengthRead = 0;
   uint32_t    dataLengthWrite = 0;
   bool_t      dataBufferized = FALSE;

   /* Test if the WorkingBuffer Length is null */
   if(pLlcpSocket->bufferLinearLength == 0)
   {
      if (pLlcpSocket->eSocket_State != phFriNfc_LlcpTransportSocket_eSocketConnected)
      {
          return NFCSTATUS_FAILED;
      }
      
      /* Test If data is present in the RW Buffer */
      if(pLlcpSocket->indexRwRead != pLlcpSocket->indexRwWrite)
      {
         if(pLlcpSocket->sSocketRwBufferTable[(pLlcpSocket->indexRwRead%pLlcpSocket->localRW)].length != 0)
         {
            /* Save I_FRAME into the Receive Buffer */
            memcpy(psBuffer->buffer,pLlcpSocket->sSocketRwBufferTable[(pLlcpSocket->indexRwRead%pLlcpSocket->localRW)].buffer,pLlcpSocket->sSocketRwBufferTable[(pLlcpSocket->indexRwRead%pLlcpSocket->localRW)].length);
            psBuffer->length = pLlcpSocket->sSocketRwBufferTable[(pLlcpSocket->indexRwRead%pLlcpSocket->localRW)].length;

            dataBufferized = TRUE;

            /* Update VR */
            pLlcpSocket->socket_VR = (pLlcpSocket->socket_VR+1)%16;

            /* Update RW Buffer length */
            pLlcpSocket->sSocketRwBufferTable[(pLlcpSocket->indexRwRead%pLlcpSocket->localRW)].length = 0;

            /* Update Value Rw Read Index*/
            pLlcpSocket->indexRwRead++;
         }
      }

      if(dataBufferized == TRUE)
      {
         /* Call the Receive CB */
         pRecv_RspCb(pContext,NFCSTATUS_SUCCESS);

         if(pLlcpSocket->ReceiverBusyCondition == TRUE)
         {
            /* Reset the ReceiverBusyCondition Flag */
            pLlcpSocket->ReceiverBusyCondition = FALSE;
            /* RR */
            /* TODO: report status? */
            phFriNfc_Llcp_Send_ReceiveReady_Frame(pLlcpSocket);
         }
      }
      else
      {
         /* Set Receive pending */
         pLlcpSocket->bSocketRecvPending = TRUE;

         /* Store the buffer pointer */
         pLlcpSocket->sSocketRecvBuffer = psBuffer;

         /* Store the Recv CB and context */
         pLlcpSocket->pfSocketRecv_Cb  = pRecv_RspCb;
         pLlcpSocket->pRecvContext     = pContext;

         /* Set status */
         status = NFCSTATUS_PENDING;
      }
   }
   else
   {
      /* Test if data is present in the linear buffer*/
      dataLengthStored = phFriNfc_Llcp_CyclicFifoUsage(&pLlcpSocket->sCyclicFifoBuffer);

      if(dataLengthStored != 0)
      {
         if(psBuffer->length > dataLengthStored)
         {
            psBuffer->length = dataLengthStored;
         }

         /* Read data from the linear buffer */
         dataLengthRead = phFriNfc_Llcp_CyclicFifoFifoRead(&pLlcpSocket->sCyclicFifoBuffer,
                                                           psBuffer->buffer,
                                                           psBuffer->length);

         if(dataLengthRead != 0)
         {
            /* Test If data is present in the RW Buffer */
            while(pLlcpSocket->indexRwRead != pLlcpSocket->indexRwWrite)
            {
               /* Get the data length available in the linear buffer  */
               dataLengthAvailable = phFriNfc_Llcp_CyclicFifoAvailable(&pLlcpSocket->sCyclicFifoBuffer);

               /* Exit if not enough memory available in linear buffer */
               if(dataLengthAvailable < pLlcpSocket->sSocketRwBufferTable[(pLlcpSocket->indexRwRead%pLlcpSocket->localRW)].length)
               {
                  break;
               }

               /* Write data into the linear buffer */
               dataLengthWrite = phFriNfc_Llcp_CyclicFifoWrite(&pLlcpSocket->sCyclicFifoBuffer,
                                                               pLlcpSocket->sSocketRwBufferTable[(pLlcpSocket->indexRwRead%pLlcpSocket->localRW)].buffer,
                                                               pLlcpSocket->sSocketRwBufferTable[(pLlcpSocket->indexRwRead%pLlcpSocket->localRW)].length);
               /* Update VR */
               pLlcpSocket->socket_VR = (pLlcpSocket->socket_VR+1)%16;

               /* Set flag bufferized to TRUE */
               dataBufferized = TRUE;

               /* Update RW Buffer length */
               pLlcpSocket->sSocketRwBufferTable[(pLlcpSocket->indexRwRead%pLlcpSocket->localRW)].length = 0;

               /* Update Value Rw Read Index*/
               pLlcpSocket->indexRwRead++;
            }

            /* Test if data has been bufferized after a read access */
            if(dataBufferized == TRUE)
            {
               /* Get the data length available in the linear buffer  */
               dataLengthAvailable = phFriNfc_Llcp_CyclicFifoAvailable(&pLlcpSocket->sCyclicFifoBuffer);
               if((dataLengthAvailable >= pLlcpSocket->sSocketOption.miu) && (pLlcpSocket->ReceiverBusyCondition == TRUE))
               {
                  /* Reset the ReceiverBusyCondition Flag */
                  pLlcpSocket->ReceiverBusyCondition = FALSE;
                  /* RR */
                  /* TODO: report status? */
                  phFriNfc_Llcp_Send_ReceiveReady_Frame(pLlcpSocket);
               }
            }
            
            /* Call the Receive CB */
            pRecv_RspCb(pContext,NFCSTATUS_SUCCESS);
         }
         else
         {
            /* Call the Receive CB   */
            status = NFCSTATUS_FAILED;
         }
      }
      else
      {
         if (pLlcpSocket->eSocket_State != phFriNfc_LlcpTransportSocket_eSocketConnected)
         {
             status = NFCSTATUS_FAILED;
         }
         else
         {
            /* Set Receive pending */
            pLlcpSocket->bSocketRecvPending = TRUE;

            /* Store the buffer pointer */
            pLlcpSocket->sSocketRecvBuffer = psBuffer;

            /* Store the Recv CB and context */
            pLlcpSocket->pfSocketRecv_Cb  = pRecv_RspCb;
            pLlcpSocket->pRecvContext     = pContext;

            /* Set status */
            status = NFCSTATUS_PENDING;
         }
      }
   }

   if(status != NFCSTATUS_PENDING)
   {
      /* Note: The receive callback must be released to avoid being called at abort */
      LLCP_PRINT("Release Receive callback");
      pLlcpSocket->pfSocketRecv_Cb = NULL;
      pLlcpSocket->pRecvContext = NULL;
   }

   return status;
}


