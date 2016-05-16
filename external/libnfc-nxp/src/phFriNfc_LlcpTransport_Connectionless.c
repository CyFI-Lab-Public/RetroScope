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
 * \file  phFriNfc_LlcpTransport_Connectionless.c
 * \brief 
 *
 * Project: NFC-FRI
 *
 */
/*include files*/
#include <phOsalNfc.h>
#include <phLibNfcStatus.h>
#include <phLibNfc.h>
#include <phNfcLlcpTypes.h>
#include <phFriNfc_LlcpTransport.h>
#include <phFriNfc_Llcp.h>

static void phFriNfc_LlcpTransport_Connectionless_SendTo_CB(void*        pContext,
                                                            uint8_t      socketIndex,
                                                            NFCSTATUS    status);

NFCSTATUS phFriNfc_LlcpTransport_Connectionless_HandlePendingOperations(phFriNfc_LlcpTransport_Socket_t *pSocket)
{
   NFCSTATUS status = NFCSTATUS_FAILED;

   /* Check if something is pending and if transport layer is ready to send */
   if ((pSocket->pfSocketSend_Cb != NULL) &&
       (pSocket->psTransport->bSendPending == FALSE))
   {
      /* Fill the psLlcpHeader stuture with the DSAP,PTYPE and the SSAP */
      pSocket->sLlcpHeader.dsap  = pSocket->socket_dSap;
      pSocket->sLlcpHeader.ptype = PHFRINFC_LLCP_PTYPE_UI;
      pSocket->sLlcpHeader.ssap  = pSocket->socket_sSap;

      /* Send to data to the approiate socket */
      status =  phFriNfc_LlcpTransport_LinkSend(pSocket->psTransport,
                                   &pSocket->sLlcpHeader,
                                   NULL,
                                   &pSocket->sSocketSendBuffer,
                                   phFriNfc_LlcpTransport_Connectionless_SendTo_CB,
                                   pSocket->index,
                                   pSocket);
   }
   else
   {
      /* Cannot send now, retry later */
   }

   return status;
}


/* TODO: comment function Handle_Connectionless_IncommingFrame */
void Handle_Connectionless_IncommingFrame(phFriNfc_LlcpTransport_t      *pLlcpTransport,
                                          phNfc_sData_t                 *psData,
                                          uint8_t                       dsap,
                                          uint8_t                       ssap)
{
   phFriNfc_LlcpTransport_Socket_t * pSocket = NULL;
   uint8_t                           i       = 0;
   uint8_t                           writeIndex;

   /* Look through the socket table for a match */
   for(i=0;i<PHFRINFC_LLCP_NB_SOCKET_MAX;i++)
   {
      if(pLlcpTransport->pSocketTable[i].socket_sSap == dsap)
      {
         /* Socket found ! */
         pSocket = &pLlcpTransport->pSocketTable[i];

         /* Forward directly to application if a read is pending */
         if (pSocket->bSocketRecvPending == TRUE)
         {
            /* Reset the RecvPending variable */
            pSocket->bSocketRecvPending = FALSE;

            /* Copy the received buffer into the receive buffer */
            memcpy(pSocket->sSocketRecvBuffer->buffer, psData->buffer, psData->length);

            /* Update the received length */
            *pSocket->receivedLength = psData->length;

            /* call the recv callback */
            pSocket->pfSocketRecvFrom_Cb(pSocket->pRecvContext, ssap, NFCSTATUS_SUCCESS);
            pSocket->pfSocketRecvFrom_Cb = NULL;
         }
         /* If no read is pending, try to bufferize for later reading */
         else
         {
            if((pSocket->indexRwWrite - pSocket->indexRwRead) < pSocket->localRW)
            {
               writeIndex = pSocket->indexRwWrite % pSocket->localRW;
               /* Save SSAP */
               pSocket->sSocketRwBufferTable[writeIndex].buffer[0] = ssap;
               /* Save UI frame payload */
               memcpy(pSocket->sSocketRwBufferTable[writeIndex].buffer + 1,
                      psData->buffer,
                      psData->length);
               pSocket->sSocketRwBufferTable[writeIndex].length = psData->length;

               /* Update the RW write index */
               pSocket->indexRwWrite++;
            }
            else
            {
               /* Unable to bufferize the packet, drop it */
            }
         }
         break;
      }
   }
}

/* TODO: comment function phFriNfc_LlcpTransport_Connectionless_SendTo_CB */
static void phFriNfc_LlcpTransport_Connectionless_SendTo_CB(void*        pContext,
                                                            uint8_t      socketIndex,
                                                            NFCSTATUS    status)
{
   phFriNfc_LlcpTransport_Socket_t *         pLlcpSocket = (phFriNfc_LlcpTransport_Socket_t*)pContext;
   pphFriNfc_LlcpTransportSocketSendCb_t     pfSavedCallback;
   void *                                    pSavedContext;

   /* Call the send callback */
   pfSavedCallback = pLlcpSocket->pfSocketSend_Cb;
   if (pfSavedCallback != NULL)
   {
      pLlcpSocket->pfSocketSend_Cb = NULL;
      pfSavedCallback(pLlcpSocket->pSendContext, status);
   }
}

static void phFriNfc_LlcpTransport_Connectionless_Abort(phFriNfc_LlcpTransport_Socket_t* pLlcpSocket)
{
   if (pLlcpSocket->pfSocketSend_Cb != NULL)
   {
      pLlcpSocket->pfSocketSend_Cb(pLlcpSocket->pSendContext, NFCSTATUS_ABORTED);
      pLlcpSocket->pSendContext = NULL;
      pLlcpSocket->pfSocketSend_Cb = NULL;
   }
   if (pLlcpSocket->pfSocketRecvFrom_Cb != NULL)
   {
      pLlcpSocket->pfSocketRecvFrom_Cb(pLlcpSocket->pRecvContext, 0, NFCSTATUS_ABORTED);
      pLlcpSocket->pRecvContext = NULL;
      pLlcpSocket->pfSocketRecvFrom_Cb = NULL;
      pLlcpSocket->pfSocketRecv_Cb = NULL;
   }
   pLlcpSocket->pAcceptContext = NULL;
   pLlcpSocket->pfSocketAccept_Cb = NULL;
   pLlcpSocket->pListenContext = NULL;
   pLlcpSocket->pfSocketListen_Cb = NULL;
   pLlcpSocket->pConnectContext = NULL;
   pLlcpSocket->pfSocketConnect_Cb = NULL;
   pLlcpSocket->pDisconnectContext = NULL;
   pLlcpSocket->pfSocketDisconnect_Cb = NULL;
}

/**
* \ingroup grp_fri_nfc
* \brief <b>Close a socket on a LLCP-connectionless device</b>.
*
* This function closes a LLCP socket previously created using phFriNfc_LlcpTransport_Socket.
*
* \param[in]  pLlcpSocket                    A pointer to a phFriNfc_LlcpTransport_Socket_t.

* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_Connectionless_Close(phFriNfc_LlcpTransport_Socket_t*   pLlcpSocket)
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
   pLlcpSocket->RemoteBusyConditionInfo            = FALSE;
   pLlcpSocket->ReceiverBusyCondition              = FALSE;
   pLlcpSocket->socket_VS                          = 0;
   pLlcpSocket->socket_VSA                         = 0;
   pLlcpSocket->socket_VR                          = 0;
   pLlcpSocket->socket_VRA                         = 0;

   phFriNfc_LlcpTransport_Connectionless_Abort(pLlcpSocket);

   memset(&pLlcpSocket->sSocketOption, 0x00, sizeof(phFriNfc_LlcpTransport_sSocketOptions_t));

   if (pLlcpSocket->sServiceName.buffer != NULL) {
       phOsalNfc_FreeMemory(pLlcpSocket->sServiceName.buffer);
   }
   pLlcpSocket->sServiceName.buffer = NULL;
   pLlcpSocket->sServiceName.length = 0;

   return NFCSTATUS_SUCCESS;
}

/**
* \ingroup grp_fri_nfc
* \brief <b>Send data on a socket to a given destination SAP</b>.
*
* This function is used to write data on a socket to a given destination SAP.
* This function can only be called on a connectionless socket.
* 
*
* \param[in]  pLlcpSocket        A pointer to a LlcpSocket created.
* \param[in]  nSap               The destination SAP.
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
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_Connectionless_SendTo(phFriNfc_LlcpTransport_Socket_t             *pLlcpSocket,
                                                       uint8_t                                     nSap,
                                                       phNfc_sData_t*                              psBuffer,
                                                       pphFriNfc_LlcpTransportSocketSendCb_t       pSend_RspCb,
                                                       void*                                       pContext)
{
   NFCSTATUS status = NFCSTATUS_FAILED;

   /* Store send callback  and context*/
   pLlcpSocket->pfSocketSend_Cb = pSend_RspCb;
   pLlcpSocket->pSendContext    = pContext;

   /* Test if a send is already pending at transport level */
   if(pLlcpSocket->psTransport->bSendPending == TRUE)
   {
      /* Save the request so it can be handled in phFriNfc_LlcpTransport_Connectionless_HandlePendingOperations() */
      pLlcpSocket->sSocketSendBuffer = *psBuffer;
      pLlcpSocket->socket_dSap      = nSap;
      status = NFCSTATUS_PENDING;
   }
   else
   {
      /* Fill the psLlcpHeader stuture with the DSAP,PTYPE and the SSAP */
      pLlcpSocket->sLlcpHeader.dsap  = nSap;
      pLlcpSocket->sLlcpHeader.ptype = PHFRINFC_LLCP_PTYPE_UI;
      pLlcpSocket->sLlcpHeader.ssap  = pLlcpSocket->socket_sSap;

      /* Send to data to the approiate socket */
      status =  phFriNfc_LlcpTransport_LinkSend(pLlcpSocket->psTransport,
                                   &pLlcpSocket->sLlcpHeader,
                                   NULL,
                                   psBuffer,
                                   phFriNfc_LlcpTransport_Connectionless_SendTo_CB,
                                   pLlcpSocket->index,
                                   pLlcpSocket);
   }

   return status;
}


 /**
* \ingroup grp_lib_nfc
* \brief <b>Read data on a socket and get the source SAP</b>.
*
* This function is the same as phLibNfc_Llcp_Recv, except that the callback includes
* the source SAP. This functions can only be called on a connectionless socket.
* 
*
* \param[in]  pLlcpSocket        A pointer to a LlcpSocket created.
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
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phLibNfc_LlcpTransport_Connectionless_RecvFrom(phFriNfc_LlcpTransport_Socket_t                   *pLlcpSocket,
                                                         phNfc_sData_t*                                    psBuffer,
                                                         pphFriNfc_LlcpTransportSocketRecvFromCb_t         pRecv_Cb,
                                                         void                                              *pContext)
{
   NFCSTATUS   status = NFCSTATUS_PENDING;
   uint8_t     readIndex;
   uint8_t     ssap;

   if(pLlcpSocket->bSocketRecvPending)
   {
      status = PHNFCSTVAL(CID_FRI_NFC_LLCP_TRANSPORT, NFCSTATUS_REJECTED);
   }
   else
   {
      /* Check if pending packets in RW */
      if(pLlcpSocket->indexRwRead != pLlcpSocket->indexRwWrite)
      {
         readIndex = pLlcpSocket->indexRwRead % pLlcpSocket->localRW;

         /* Extract ssap and buffer from RW buffer */
         ssap = pLlcpSocket->sSocketRwBufferTable[readIndex].buffer[0];
         memcpy(psBuffer->buffer,
                pLlcpSocket->sSocketRwBufferTable[readIndex].buffer + 1,
                pLlcpSocket->sSocketRwBufferTable[readIndex].length);
         psBuffer->length = pLlcpSocket->sSocketRwBufferTable[readIndex].length;

         /* Reset RW buffer length */
         pLlcpSocket->sSocketRwBufferTable[readIndex].length = 0;

         /* Update Value Rw Read Index */
         pLlcpSocket->indexRwRead++;

         /* call the recv callback */
         pRecv_Cb(pContext, ssap, NFCSTATUS_SUCCESS);

         status = NFCSTATUS_SUCCESS;
      }
      /* Otherwise, wait for a packet to come */
      else
      {
         /* Store the callback and context*/
         pLlcpSocket->pfSocketRecvFrom_Cb  = pRecv_Cb;
         pLlcpSocket->pRecvContext         = pContext;

         /* Store the pointer to the receive buffer */
         pLlcpSocket->sSocketRecvBuffer   =  psBuffer;
         pLlcpSocket->receivedLength      =  &psBuffer->length;

         /* Set RecvPending to TRUE */
         pLlcpSocket->bSocketRecvPending = TRUE;
      }
   }
   return status;
}
