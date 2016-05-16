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

#include <phNfcTypes.h>
#include <phLibNfc.h>
#include <phLibNfc_Internal.h>
#include <phFriNfc_Llcp.h>
#include <phFriNfc_LlcpTransport.h>

/* ---------------------------- Internal macros -------------------------------- */

#ifndef STATIC_DISABLE
#define STATIC static
#else
#define STATIC
#endif

/* ----------------------- Internal functions headers -------------------------- */

STATIC
NFCSTATUS static_CheckState();

STATIC
NFCSTATUS static_CheckDevice(phLibNfc_Handle hRemoteDevice);

STATIC
void phLibNfc_Llcp_CheckLlcp_Cb(void *pContext,NFCSTATUS status);

STATIC
void phLibNfc_Llcp_Link_Cb(void *pContext,phLibNfc_Llcp_eLinkStatus_t status);

/* --------------------------- Internal functions ------------------------------ */

STATIC NFCSTATUS static_CheckState()
{
   /* Check if the global context is set */
   if(gpphLibContext == NULL)
   {
      return NFCSTATUS_NOT_INITIALISED;
   }

   /* Check if initialized */
   if(gpphLibContext->LibNfcState.cur_state == eLibNfcHalStateShutdown)
   {
      return NFCSTATUS_NOT_INITIALISED;
   }

   /* Check if shutting down */
   if(gpphLibContext->LibNfcState.next_state == eLibNfcHalStateShutdown)
   {
      return NFCSTATUS_SHUTDOWN;
   }

   return NFCSTATUS_SUCCESS;
}

STATIC NFCSTATUS static_CheckDevice(phLibNfc_Handle hRemoteDevice)
{
   phLibNfc_sRemoteDevInformation_t*   psRemoteDevInfo = (phLibNfc_sRemoteDevInformation_t*)hRemoteDevice;

   if (hRemoteDevice == NULL)
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* If local device is the Initiator (remote is Target),
    * check if connection is correct
    */
   if (psRemoteDevInfo->RemDevType == phHal_eNfcIP1_Target)
   {
      /* Check if any device connected */
      if(gpphLibContext->Connected_handle == 0)
      {
         return NFCSTATUS_TARGET_NOT_CONNECTED;
      }

      /* Check if handle corresponds to connected one */
      if(hRemoteDevice != gpphLibContext->Connected_handle)
      {
         return NFCSTATUS_INVALID_HANDLE;
      }
   }

   /* Check if previous callback is pending or if remote peer is not LLCP compliant */
   if ((gpphLibContext->status.GenCb_pending_status == TRUE) ||
            (gpphLibContext->llcp_cntx.bIsLlcp == FALSE))
   {
      return NFCSTATUS_REJECTED;
   }

   return NFCSTATUS_SUCCESS;
}

/* ---------------------------- Public functions ------------------------------- */

NFCSTATUS phLibNfc_Mgt_SetLlcp_ConfigParams( phLibNfc_Llcp_sLinkParameters_t* pConfigInfo,
                                            pphLibNfc_RspCb_t                pConfigRspCb,
                                            void*                            pContext
                                            )
{
   NFCSTATUS      result;
   phNfc_sData_t  sGeneralBytesBuffer;
   phLibNfc_sNfcIPCfg_t sNfcIPCfg;
   const uint8_t  pMagicBuffer[] = { 0x46, 0x66, 0x6D };

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if ((pConfigInfo == NULL) || (pConfigRspCb == NULL))
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Save the config for later use */
   memcpy( &gpphLibContext->llcp_cntx.sLocalParams,
           pConfigInfo,
           sizeof(phLibNfc_Llcp_sLinkParameters_t) );

   /* Copy magic number in NFCIP General Bytes */
   memcpy(sNfcIPCfg.generalBytes, pMagicBuffer, sizeof(pMagicBuffer));
   sNfcIPCfg.generalBytesLength = sizeof(pMagicBuffer);

   /* Encode link parameters in TLV to configure P2P General Bytes */
   sGeneralBytesBuffer.buffer = sNfcIPCfg.generalBytes + sizeof(pMagicBuffer);
   sGeneralBytesBuffer.length = sizeof(sNfcIPCfg.generalBytes) - sizeof(pMagicBuffer);
   result = phFriNfc_Llcp_EncodeLinkParams( &sGeneralBytesBuffer,
                                            pConfigInfo,
                                            PHFRINFC_LLCP_VERSION);
   if (result != NFCSTATUS_SUCCESS)
   {
      return PHNFCSTATUS(result);
   }
   sNfcIPCfg.generalBytesLength += (uint8_t)sGeneralBytesBuffer.length;

   /* Set the P2P general bytes */
   result = phLibNfc_Mgt_SetP2P_ConfigParams(&sNfcIPCfg, pConfigRspCb, pContext);
   if (result != NFCSTATUS_PENDING)
   {
      return PHNFCSTATUS(result);
   }

   /* Resets the LLCP LLC component */
   result = phFriNfc_Llcp_Reset( &gpphLibContext->llcp_cntx.sLlcpContext,
                                 gpphLibContext->psOverHalCtxt,
                                 pConfigInfo,
                                 gpphLibContext->llcp_cntx.pRxBuffer,
                                 sizeof(gpphLibContext->llcp_cntx.pRxBuffer),
                                 gpphLibContext->llcp_cntx.pTxBuffer,
                                 sizeof(gpphLibContext->llcp_cntx.pTxBuffer),
                                 phLibNfc_Llcp_Link_Cb,
                                 gpphLibContext);
   if (result != NFCSTATUS_SUCCESS)
   {
      return PHNFCSTATUS(result);
   }

   /* Resets the LLCP Transport component */
   result = phFriNfc_LlcpTransport_Reset( &gpphLibContext->llcp_cntx.sLlcpTransportContext,
                                          &gpphLibContext->llcp_cntx.sLlcpContext );
   if (result != NFCSTATUS_SUCCESS)
   {
      return PHNFCSTATUS(result);
   }

   return NFCSTATUS_PENDING;
}

NFCSTATUS phLibNfc_Llcp_CheckLlcp( phLibNfc_Handle              hRemoteDevice,
                                   pphLibNfc_ChkLlcpRspCb_t     pCheckLlcp_RspCb,
                                   pphLibNfc_LlcpLinkStatusCb_t pLink_Cb,
                                   void*                        pContext
                                   )
{
   NFCSTATUS                           result;
   phLibNfc_sRemoteDevInformation_t*   psRemoteDevInfo = (phLibNfc_sRemoteDevInformation_t*)hRemoteDevice;

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if ((hRemoteDevice == 0)       ||
       (pCheckLlcp_RspCb == NULL) ||
       (pLink_Cb == NULL))
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* If local device is the Initiator (remote is Target),
    * check if connection is correct
    */
   if (psRemoteDevInfo->RemDevType == phHal_eNfcIP1_Target)
   {
      /* Check if any device connected */
      if(gpphLibContext->Connected_handle == 0)
      {
         return NFCSTATUS_TARGET_NOT_CONNECTED;
      }

      /* Check if handle corresponds to connected one */
      if(hRemoteDevice != gpphLibContext->Connected_handle)
      {
         return NFCSTATUS_INVALID_HANDLE;
      }
   }

   /* Prepare callback */
   gpphLibContext->CBInfo.pClientLlcpLinkCb = pLink_Cb;
   gpphLibContext->CBInfo.pClientLlcpLinkCntx = pContext;

   // DEBUG: Reset at least the state
   gpphLibContext->llcp_cntx.sLlcpContext.state = 0;

   /* Prepare callback */
   gpphLibContext->CBInfo.pClientLlcpCheckRespCb = pCheckLlcp_RspCb;
   gpphLibContext->CBInfo.pClientLlcpCheckRespCntx = pContext;

   /* Update state */
   result = phLibNfc_UpdateNextState(gpphLibContext, eLibNfcHalStateTransaction);
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Call the component function */
   result = phFriNfc_Llcp_ChkLlcp( &gpphLibContext->llcp_cntx.sLlcpContext,
                                   psRemoteDevInfo,
                                   phLibNfc_Llcp_CheckLlcp_Cb,
                                   gpphLibContext
                                   );
   result = PHNFCSTATUS(result);
   if (result == NFCSTATUS_PENDING)
   {
      gpphLibContext->status.GenCb_pending_status = TRUE;
   }
   else if (result == NFCSTATUS_SUCCESS)
   {
      /* Nothing to do */
   }
   else if (result != NFCSTATUS_FAILED)
   {
      result = NFCSTATUS_TARGET_LOST;
   }

   return result;
}

/* LLCP link callback */
STATIC
void phLibNfc_Llcp_Link_Cb(void *pContext, phLibNfc_Llcp_eLinkStatus_t status)
{
   phLibNfc_LibContext_t         *pLibNfc_Ctxt = (phLibNfc_LibContext_t *)pContext;
   pphLibNfc_LlcpLinkStatusCb_t  pClientCb = NULL;
   void                          *pClientContext = NULL;

   if(pLibNfc_Ctxt != gpphLibContext)
   {
      /*wrong context returned from below layer*/
      phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
   }
   else
   {
      /* Close all sockets */
      phFriNfc_LlcpTransport_CloseAll(&gpphLibContext->llcp_cntx.sLlcpTransportContext);

      /* Copy callback details */
      pClientCb = gpphLibContext->CBInfo.pClientLlcpLinkCb;
      pClientContext = gpphLibContext->CBInfo.pClientLlcpLinkCntx;

      /* Trigger the callback */
      if(pClientCb != NULL)
      {
         pClientCb(pClientContext, status);
      }
   }
}

/* Response callback for phLibNfc_Ndef_CheckNdef */
STATIC
void phLibNfc_Llcp_CheckLlcp_Cb(void *pContext, NFCSTATUS status)
{
   phLibNfc_LibContext_t      *pLibNfc_Ctxt = (phLibNfc_LibContext_t *)pContext;
   NFCSTATUS                  RetStatus = NFCSTATUS_SUCCESS;
   pphLibNfc_ChkLlcpRspCb_t   pClientCb = NULL;
   void                       *pClientContext = NULL;

   if(pLibNfc_Ctxt != gpphLibContext)
   {
      /*wrong context returned from below layer*/
      phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
   }
   else
   {
      if(gpphLibContext->LibNfcState.next_state == eLibNfcHalStateShutdown)
      {
         /*shutdown called before completion of check Ndef, allow shutdown to happen */
         phLibNfc_Pending_Shutdown();
         RetStatus = NFCSTATUS_SHUTDOWN;
      }
      else if(gpphLibContext->LibNfcState.next_state == eLibNfcHalStateRelease)
      {
         RetStatus = NFCSTATUS_ABORTED;
      }
      else
      {
         if(status == NFCSTATUS_SUCCESS)
         {
            /* Remote peer is LLCP compliant */
            gpphLibContext->llcp_cntx.bIsLlcp = TRUE;
         }
         else if(PHNFCSTATUS(status)== NFCSTATUS_FAILED)
         {
            RetStatus = NFCSTATUS_FAILED; 
            gpphLibContext->llcp_cntx.bIsLlcp = FALSE;
         }
         else
         {
            RetStatus = NFCSTATUS_TARGET_LOST;
         }
      }

      /* Update the current state */
      gpphLibContext->status.GenCb_pending_status = FALSE;
      phLibNfc_UpdateCurState(RetStatus,gpphLibContext);

      /* Copy callback details */
      pClientCb = gpphLibContext->CBInfo.pClientLlcpCheckRespCb;
      pClientContext = gpphLibContext->CBInfo.pClientLlcpCheckRespCntx;

      /* Reset saved callback */
      gpphLibContext->CBInfo.pClientCkNdefCb = NULL;
      gpphLibContext->CBInfo.pClientCkNdefCntx = NULL;

      /* Trigger the callback */
      if(pClientCb != NULL)
      {
         pClientCb(pClientContext,RetStatus);
      }
   }
}

NFCSTATUS phLibNfc_Llcp_Activate( phLibNfc_Handle hRemoteDevice )
{
   NFCSTATUS result;

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if (hRemoteDevice == 0)
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Check device */
   result = static_CheckDevice(hRemoteDevice);
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Start activation */
   result = phFriNfc_Llcp_Activate(&gpphLibContext->llcp_cntx.sLlcpContext);

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_Deactivate( phLibNfc_Handle  hRemoteDevice )
{
   NFCSTATUS result;

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if (hRemoteDevice == 0)
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Check device */
   result = static_CheckDevice(hRemoteDevice);
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Start deactivation */
   result = phFriNfc_Llcp_Deactivate(&gpphLibContext->llcp_cntx.sLlcpContext);

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_GetLocalInfo( phLibNfc_Handle                  hRemoteDevice,
                                      phLibNfc_Llcp_sLinkParameters_t* pConfigInfo
                                      )
{
   NFCSTATUS result;

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if (pConfigInfo == NULL)
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Get local infos */
   result = phFriNfc_Llcp_GetLocalInfo(&gpphLibContext->llcp_cntx.sLlcpContext, pConfigInfo);

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_GetRemoteInfo( phLibNfc_Handle                    hRemoteDevice,
                                       phLibNfc_Llcp_sLinkParameters_t*   pConfigInfo
                                       )
{
   NFCSTATUS result;

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if ((hRemoteDevice == 0) ||
       (pConfigInfo == NULL))
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Check device */
   result = static_CheckDevice(hRemoteDevice);
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Get local infos */
   result = phFriNfc_Llcp_GetRemoteInfo(&gpphLibContext->llcp_cntx.sLlcpContext, pConfigInfo);

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_DiscoverServices( phLibNfc_Handle     hRemoteDevice,
                                          phNfc_sData_t       *psServiceNameList,
                                          uint8_t             *pnSapList,
                                          uint8_t             nListSize,
                                          pphLibNfc_RspCb_t   pDiscover_Cb,
                                          void                *pContext
                                          )
{
   NFCSTATUS                           result;
   PHNFC_UNUSED_VARIABLE(hRemoteDevice);

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if ((hRemoteDevice == 0)       ||
       (psServiceNameList == NULL) ||
       (pnSapList == NULL) ||
       (nListSize == 0) ||
       (pDiscover_Cb == NULL))
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Check device */
   result = static_CheckDevice(hRemoteDevice);
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Prepare callback */
   gpphLibContext->CBInfo.pClientLlcpDiscoveryCb = pDiscover_Cb;
   gpphLibContext->CBInfo.pClientLlcpDiscoveryCntx = pContext;

   /* Update state */
   result = phLibNfc_UpdateNextState(gpphLibContext, eLibNfcHalStateTransaction);
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Call the component function */
   result = phFriNfc_LlcpTransport_DiscoverServices( &gpphLibContext->llcp_cntx.sLlcpTransportContext,
                                                     psServiceNameList,
                                                     pnSapList,
                                                     nListSize,
                                                     pDiscover_Cb,
                                                     pContext
                                                     );
   result = PHNFCSTATUS(result);
   if ((result == NFCSTATUS_PENDING) || (result == NFCSTATUS_SUCCESS))
   {
      /* Nothing to do */
   }
   else if (result != NFCSTATUS_FAILED)
   {
      result = NFCSTATUS_TARGET_LOST;
   }

   return result;
}

NFCSTATUS phLibNfc_Llcp_Socket( phLibNfc_Llcp_eSocketType_t      eType,
                                phLibNfc_Llcp_sSocketOptions_t*  psOptions,
                                phNfc_sData_t*                   psWorkingBuffer,
                                phLibNfc_Handle*                 phSocket,
                                pphLibNfc_LlcpSocketErrCb_t      pErr_Cb,
                                void*                            pContext
                                )
{
   NFCSTATUS                        result;
   phFriNfc_LlcpTransport_Socket_t  *psSocket;

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   /* NOTE: Transport Layer test psOption and psWorkingBuffer value */
   if ((phSocket == NULL)        ||
       (pErr_Cb == NULL))
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Get local infos */
   result = phFriNfc_LlcpTransport_Socket(&gpphLibContext->llcp_cntx.sLlcpTransportContext,
                                          eType,
                                          psOptions,
                                          psWorkingBuffer,
                                          &psSocket,
                                          pErr_Cb,
                                          pContext);

   /* Send back the socket handle */
   *phSocket = (phLibNfc_Handle)psSocket;

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_Close( phLibNfc_Handle hSocket )
{
   NFCSTATUS                        result;
   phFriNfc_LlcpTransport_Socket_t  *psSocket = (phFriNfc_LlcpTransport_Socket_t*)hSocket;

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if (hSocket == 0)
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Get local infos */
   /* TODO: if connected abort and close else close only */
   result = phFriNfc_LlcpTransport_Close(psSocket);

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_SocketGetLocalOptions( phLibNfc_Handle                  hSocket,
                                               phLibNfc_Llcp_sSocketOptions_t*  psLocalOptions
                                               )
{
   NFCSTATUS                        result;
   phFriNfc_LlcpTransport_Socket_t  *psSocket = (phFriNfc_LlcpTransport_Socket_t*)hSocket;

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if ((hSocket == 0) ||
       (psLocalOptions == NULL))
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Get local options */
   result = phFriNfc_LlcpTransport_SocketGetLocalOptions(psSocket, psLocalOptions);

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_SocketGetRemoteOptions( phLibNfc_Handle                  hRemoteDevice,
                                                phLibNfc_Handle                  hSocket,
                                                phLibNfc_Llcp_sSocketOptions_t*  psRemoteOptions
                                                )
{
   NFCSTATUS                        result;
   phFriNfc_LlcpTransport_Socket_t  *psSocket = (phFriNfc_LlcpTransport_Socket_t*)hSocket;

   LLCP_PRINT("phLibNfc_Llcp_SocketGetRemoteOptions");

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if ((hRemoteDevice == 0) ||
       (hSocket == 0)       ||
       (psRemoteOptions == NULL))
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Check device */
   result = static_CheckDevice(hRemoteDevice);
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Get remote infos */
   result = phFriNfc_LlcpTransport_SocketGetRemoteOptions(psSocket, psRemoteOptions);

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_Bind( phLibNfc_Handle hSocket,
                              uint8_t         nSap,
                              phNfc_sData_t * psServiceName
                              )
{
   NFCSTATUS                        result;
   phFriNfc_LlcpTransport_Socket_t  *psSocket = (phFriNfc_LlcpTransport_Socket_t*)hSocket;

   LLCP_PRINT("phLibNfc_Llcp_Bind");

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if (hSocket == 0)
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Bind the socket to the designated port */
   result = phFriNfc_LlcpTransport_Bind(psSocket, nSap, psServiceName);

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_Listen( phLibNfc_Handle                  hSocket,
                                pphLibNfc_LlcpSocketListenCb_t   pListen_Cb,
                                void*                            pContext
                                )
{
   NFCSTATUS                        result;
   phFriNfc_LlcpTransport_Socket_t  *psSocket = (phFriNfc_LlcpTransport_Socket_t*)hSocket;

   LLCP_PRINT("phLibNfc_Llcp_Listen");

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   /* NOTE : psServiceName may be NULL, do not test it ! */
   if ((hSocket == 0) ||
       (pListen_Cb == NULL))
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Start listening for incoming connections */
   result = phFriNfc_LlcpTransport_Listen( psSocket,
                                           (pphFriNfc_LlcpTransportSocketListenCb_t)pListen_Cb,
                                           pContext );

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_Accept( phLibNfc_Handle                  hSocket,
                                phLibNfc_Llcp_sSocketOptions_t*  psOptions,
                                phNfc_sData_t*                   psWorkingBuffer,
                                pphLibNfc_LlcpSocketErrCb_t      pErr_Cb,
                                pphLibNfc_LlcpSocketAcceptCb_t   pAccept_RspCb,
                                void*                            pContext
                                )
{
   NFCSTATUS                        result;
   phFriNfc_LlcpTransport_Socket_t  *psSocket = (phFriNfc_LlcpTransport_Socket_t*)hSocket;

   LLCP_PRINT("phLibNfc_Llcp_Accept");

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if ((hSocket == 0)            ||
       (psOptions == NULL)       ||
       (psWorkingBuffer == NULL) ||
       (pErr_Cb == NULL)         ||
       (pAccept_RspCb == NULL))
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Accept incoming connection */
   result = phFriNfc_LlcpTransport_Accept( psSocket,
                                           psOptions,
                                           psWorkingBuffer,
                                           pErr_Cb,
                                           pAccept_RspCb,
                                           pContext );

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_Reject( phLibNfc_Handle                  hRemoteDevice,
                                phLibNfc_Handle                  hSocket,
                                pphLibNfc_LlcpSocketRejectCb_t   pReject_RspCb,
                                void*                            pContext
                                )
{
   NFCSTATUS                        result;
   phFriNfc_LlcpTransport_Socket_t  *psSocket = (phFriNfc_LlcpTransport_Socket_t*)hSocket;

   LLCP_PRINT("phLibNfc_Llcp_Reject");

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if ((hRemoteDevice == 0)      ||
       (hSocket == 0)            ||
       (pReject_RspCb == NULL))
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Check device */
   result = static_CheckDevice(hRemoteDevice);
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Reject incoming connection */
   result = phFriNfc_LlcpTransport_Reject( psSocket,
                                           pReject_RspCb,
                                           pContext );

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_Connect( phLibNfc_Handle                 hRemoteDevice,
                                 phLibNfc_Handle                 hSocket,
                                 uint8_t                         nSap,
                                 pphLibNfc_LlcpSocketConnectCb_t pConnect_RspCb,
                                 void*                           pContext
                                 )
{
   NFCSTATUS                        result;
   phFriNfc_LlcpTransport_Socket_t  *psSocket = (phFriNfc_LlcpTransport_Socket_t*)hSocket;

   LLCP_PRINT("phLibNfc_Llcp_Connect");

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if ((hRemoteDevice == 0)      ||
	   (hSocket == 0)            ||
       (pConnect_RspCb == NULL))
   {
      LLCP_PRINT("phLibNfc_Llcp_Connect NFCSTATUS_INVALID_PARAMETER");
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Check device */
   result = static_CheckDevice(hRemoteDevice);
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Try to connect on a remote service, given its SAP */
   result = phFriNfc_LlcpTransport_Connect( psSocket,
                                            nSap,
                                            pConnect_RspCb,
                                            pContext );

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_ConnectByUri( phLibNfc_Handle                 hRemoteDevice,
                                      phLibNfc_Handle                 hSocket,
                                      phNfc_sData_t*                  psUri,
                                      pphLibNfc_LlcpSocketConnectCb_t pConnect_RspCb,
                                      void*                           pContext
                                      )
{
   NFCSTATUS                        result;
   phFriNfc_LlcpTransport_Socket_t  *psSocket = (phFriNfc_LlcpTransport_Socket_t*)hSocket;

   LLCP_PRINT("phLibNfc_Llcp_ConnectByUri");

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if ((hRemoteDevice == 0)      ||
       (hSocket == 0)            ||
       (psUri   == NULL)         ||
       (pConnect_RspCb == NULL))
   {
      LLCP_PRINT("phLibNfc_Llcp_ConnectByUri NFCSTATUS_INVALID_PARAMETER");
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Check device */
   result = static_CheckDevice(hRemoteDevice);
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Try to connect on a remote service, using SDP */
   result = phFriNfc_LlcpTransport_ConnectByUri( psSocket,
                                                 psUri,
                                                 pConnect_RspCb,
                                                 pContext );

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_Disconnect( phLibNfc_Handle                    hRemoteDevice,
                                    phLibNfc_Handle                    hSocket,
                                    pphLibNfc_LlcpSocketDisconnectCb_t pDisconnect_RspCb,
                                    void*                              pContext
                                    )
{
   NFCSTATUS                        result;
   phFriNfc_LlcpTransport_Socket_t  *psSocket = (phFriNfc_LlcpTransport_Socket_t*)hSocket;

   LLCP_PRINT("phLibNfc_Llcp_Disconnect");

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if ((hRemoteDevice == 0) ||
       (hSocket == 0)       ||
       (pDisconnect_RspCb == NULL))
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Check device */
   result = static_CheckDevice(hRemoteDevice);
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Disconnect a logical link */
   result = phFriNfc_LlcpTransport_Disconnect( psSocket,
                                               pDisconnect_RspCb,
                                               pContext );

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_Recv( phLibNfc_Handle              hRemoteDevice,
                              phLibNfc_Handle              hSocket,
                              phNfc_sData_t*               psBuffer,
                              pphLibNfc_LlcpSocketRecvCb_t pRecv_RspCb,
                              void*                        pContext
                              )
{
   NFCSTATUS                        result;
   phFriNfc_LlcpTransport_Socket_t  *psSocket = (phFriNfc_LlcpTransport_Socket_t*)hSocket;

   LLCP_PRINT("phLibNfc_Llcp_Recv");

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if ((hRemoteDevice == 0)   ||
       (hSocket == 0)         ||
       (psBuffer == NULL)     ||
       (pRecv_RspCb == NULL))
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Check device */
   result = static_CheckDevice(hRemoteDevice);
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Receive data from the logical link */
   result = phFriNfc_LlcpTransport_Recv( psSocket,
                                         psBuffer,
                                         pRecv_RspCb,
                                         pContext );

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_RecvFrom( phLibNfc_Handle                   hRemoteDevice,
                                  phLibNfc_Handle                   hSocket,
                                  phNfc_sData_t*                    psBuffer,
                                  pphLibNfc_LlcpSocketRecvFromCb_t  pRecv_Cb,
                                  void*                             pContext
                                  )
{
   NFCSTATUS                        result;
   phFriNfc_LlcpTransport_Socket_t  *psSocket = (phFriNfc_LlcpTransport_Socket_t*)hSocket;

   LLCP_PRINT("phLibNfc_Llcp_RecvFrom");

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if ((hRemoteDevice == 0)   ||
       (hSocket == 0)         ||
       (psBuffer == NULL)     ||
       (pRecv_Cb == NULL))
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Check device */
   result = static_CheckDevice(hRemoteDevice);
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Receive data from the logical link */
   result = phFriNfc_LlcpTransport_RecvFrom( psSocket,
                                             psBuffer,
                                             pRecv_Cb,
                                             pContext );

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_Send( phLibNfc_Handle              hRemoteDevice,
                              phLibNfc_Handle              hSocket,
                              phNfc_sData_t*               psBuffer,
                              pphLibNfc_LlcpSocketSendCb_t pSend_RspCb,
                              void*                        pContext
                              )
{
   NFCSTATUS                        result;
   phFriNfc_LlcpTransport_Socket_t  *psSocket = (phFriNfc_LlcpTransport_Socket_t*)hSocket;

   LLCP_PRINT("phLibNfc_Llcp_Send");

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if ((hRemoteDevice == 0)   ||
       (hSocket == 0)         ||
       (psBuffer == NULL)     ||
       (pSend_RspCb == NULL))
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Check device */
   result = static_CheckDevice(hRemoteDevice);
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Send data to the logical link */
   result = phFriNfc_LlcpTransport_Send( psSocket,
                                         psBuffer,
                                         pSend_RspCb,
                                         pContext );

   return PHNFCSTATUS(result);
}

NFCSTATUS phLibNfc_Llcp_SendTo( phLibNfc_Handle               hRemoteDevice,
                                phLibNfc_Handle               hSocket,
                                uint8_t                       nSap,
                                phNfc_sData_t*                psBuffer,
                                pphLibNfc_LlcpSocketSendCb_t  pSend_RspCb,
                                void*                         pContext
                                )
{
   NFCSTATUS                        result;
   phFriNfc_LlcpTransport_Socket_t  *psSocket = (phFriNfc_LlcpTransport_Socket_t*)hSocket;

   LLCP_PRINT("phLibNfc_Llcp_SendTo");

   /* State checking */
   result = static_CheckState();
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Parameters checking */
   if ((hRemoteDevice == 0)   ||
       (hSocket == 0)         ||
       (psBuffer == NULL)     ||
       (pSend_RspCb == NULL))
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Check device */
   result = static_CheckDevice(hRemoteDevice);
   if (result != NFCSTATUS_SUCCESS)
   {
      return result;
   }

   /* Send data to the logical link */
   result = phFriNfc_LlcpTransport_SendTo( psSocket,
                                           nSap,
                                           psBuffer,
                                           pSend_RspCb,
                                           pContext );

   return PHNFCSTATUS(result);
}
