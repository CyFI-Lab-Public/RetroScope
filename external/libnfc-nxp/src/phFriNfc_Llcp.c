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
 * \file  phFriNfc_Llcp.c
 * \brief NFC LLCP core
 *
 * Project: NFC-FRI
 *
 */

/*include files*/
#include <phNfcLlcpTypes.h>
#include <phOsalNfc_Timer.h>

#include <phFriNfc_Llcp.h>
#include <phFriNfc_LlcpUtils.h>

/**
 * \internal 
 * \name States of the LLC state machine.
 *
 */
/*@{*/
#define PHFRINFC_LLCP_STATE_RESET_INIT               0   /**< \internal Initial state.*/
#define PHFRINFC_LLCP_STATE_CHECKED                  1   /**< \internal The tag has been checked for LLCP compliance.*/
#define PHFRINFC_LLCP_STATE_ACTIVATION               2   /**< \internal The deactivation phase.*/
#define PHFRINFC_LLCP_STATE_PAX                      3   /**< \internal Parameter exchange phase.*/
#define PHFRINFC_LLCP_STATE_OPERATION_RECV           4   /**< \internal Normal operation phase (ready to receive).*/
#define PHFRINFC_LLCP_STATE_OPERATION_SEND           5   /**< \internal Normal operation phase (ready to send).*/
#define PHFRINFC_LLCP_STATE_DEACTIVATION             6   /**< \internal The deactivation phase.*/
/*@}*/

/**
 * \internal 
 * \name Masks used for VERSION parsing.
 *
 */
/*@{*/
#define PHFRINFC_LLCP_VERSION_MAJOR_MASK            0xF0    /**< \internal Mask to apply to get major version number.*/
#define PHFRINFC_LLCP_VERSION_MINOR_MASK            0x0F    /**< \internal Mask to apply to get major version number.*/
/*@}*/

/**
 * \internal 
 * \name Invalid values for parameters.
 *
 */
/*@{*/
#define PHFRINFC_LLCP_INVALID_VERSION              0x00   /**< \internal Invalid VERSION value.*/
/*@}*/

/**
 * \internal 
 * \name Internal constants.
 *
 */
/*@{*/
#define PHFRINFC_LLCP_MAX_PARAM_TLV_LENGTH \
   (( PHFRINFC_LLCP_TLV_LENGTH_HEADER + PHFRINFC_LLCP_TLV_LENGTH_VERSION ) + \
    ( PHFRINFC_LLCP_TLV_LENGTH_HEADER + PHFRINFC_LLCP_TLV_LENGTH_MIUX ) + \
    ( PHFRINFC_LLCP_TLV_LENGTH_HEADER + PHFRINFC_LLCP_TLV_LENGTH_WKS ) + \
    ( PHFRINFC_LLCP_TLV_LENGTH_HEADER + PHFRINFC_LLCP_TLV_LENGTH_LTO ) + \
    ( PHFRINFC_LLCP_TLV_LENGTH_HEADER + PHFRINFC_LLCP_TLV_LENGTH_OPT ))   /**< \internal Maximum size of link params TLV.*/
/*@}*/



/* --------------------------- Internal functions ------------------------------ */

static void phFriNfc_Llcp_Receive_CB( void               *pContext,
                                      NFCSTATUS          status,
                                      phNfc_sData_t      *psData);
static NFCSTATUS phFriNfc_Llcp_HandleIncomingPacket( phFriNfc_Llcp_t    *Llcp,
                                                     phNfc_sData_t      *psPacket );
static void phFriNfc_Llcp_ResetLTO( phFriNfc_Llcp_t *Llcp );
static NFCSTATUS phFriNfc_Llcp_InternalSend( phFriNfc_Llcp_t                    *Llcp,
                                             phFriNfc_Llcp_sPacketHeader_t      *psHeader,
                                             phFriNfc_Llcp_sPacketSequence_t    *psSequence,
                                             phNfc_sData_t                      *psInfo );
static bool_t phFriNfc_Llcp_HandlePendingSend ( phFriNfc_Llcp_t *Llcp );

static phNfc_sData_t * phFriNfc_Llcp_AllocateAndCopy(phNfc_sData_t * pOrig)
{
   phNfc_sData_t * pDest = NULL;

   if (pOrig == NULL)
   {
       return NULL;
   }

   pDest = phOsalNfc_GetMemory(sizeof(phNfc_sData_t));
   if (pDest == NULL)
   {
      goto error;
   }

   pDest->buffer = phOsalNfc_GetMemory(pOrig->length);
   if (pDest->buffer == NULL)
   {
      goto error;
   }

   memcpy(pDest->buffer, pOrig->buffer, pOrig->length);
   pDest->length = pOrig->length;

   return pDest;

error:
   if (pDest != NULL)
   {
      if (pDest->buffer != NULL)
      {
         phOsalNfc_FreeMemory(pDest->buffer);
      }
      phOsalNfc_FreeMemory(pDest);
   }
   return NULL;
}

static void phFriNfc_Llcp_Deallocate(phNfc_sData_t * pData)
{
   if (pData != NULL)
   {
      if (pData->buffer != NULL)
      {
         phOsalNfc_FreeMemory(pData->buffer);
      }
      else
      {
         LLCP_PRINT("Warning, deallocating empty buffer");
      }
      phOsalNfc_FreeMemory(pData);
   }
}

static NFCSTATUS phFriNfc_Llcp_InternalDeactivate( phFriNfc_Llcp_t *Llcp )
{
   phFriNfc_Llcp_Send_CB_t pfSendCB;
   void * pSendContext;
   if ((Llcp->state == PHFRINFC_LLCP_STATE_OPERATION_RECV) ||
       (Llcp->state == PHFRINFC_LLCP_STATE_OPERATION_SEND) ||
       (Llcp->state == PHFRINFC_LLCP_STATE_PAX)            ||
       (Llcp->state == PHFRINFC_LLCP_STATE_ACTIVATION))
   {
      /* Update state */
      Llcp->state = PHFRINFC_LLCP_STATE_DEACTIVATION;

      /* Stop timer */
      phOsalNfc_Timer_Stop(Llcp->hSymmTimer);

      Llcp->psSendHeader = NULL;
      Llcp->psSendSequence = NULL;
      /* Return delayed send operation in error, in any */
      if (Llcp->psSendInfo != NULL)
      {
         phFriNfc_Llcp_Deallocate(Llcp->psSendInfo);
         Llcp->psSendInfo = NULL;
      }
      if (Llcp->pfSendCB != NULL)
      {
         /* Get Callback params */
         pfSendCB = Llcp->pfSendCB;
         pSendContext = Llcp->pSendContext;
         /* Reset callback params */
         Llcp->pfSendCB = NULL;
         Llcp->pSendContext = NULL;
         /* Call the callback */
         (pfSendCB)(pSendContext, NFCSTATUS_FAILED);
      }

      /* Notify service layer */
      Llcp->pfLink_CB(Llcp->pLinkContext, phFriNfc_LlcpMac_eLinkDeactivated);

      /* Forward check request to MAC layer */
      return phFriNfc_LlcpMac_Deactivate(&Llcp->MAC);
   }

   return NFCSTATUS_SUCCESS;
}


static NFCSTATUS phFriNfc_Llcp_SendSymm( phFriNfc_Llcp_t *Llcp )
{
   phFriNfc_Llcp_sPacketHeader_t sHeader;

   sHeader.dsap  = PHFRINFC_LLCP_SAP_LINK;
   sHeader.ssap  = PHFRINFC_LLCP_SAP_LINK;
   sHeader.ptype = PHFRINFC_LLCP_PTYPE_SYMM;
   return phFriNfc_Llcp_InternalSend(Llcp, &sHeader, NULL, NULL);
}


static NFCSTATUS phFriNfc_Llcp_SendPax( phFriNfc_Llcp_t *Llcp, phFriNfc_Llcp_sLinkParameters_t *psLinkParams)
{
   uint8_t                       pTLVBuffer[PHFRINFC_LLCP_MAX_PARAM_TLV_LENGTH];
   phNfc_sData_t                 sParamsTLV;
   phFriNfc_Llcp_sPacketHeader_t sHeader;
   NFCSTATUS                     result;

   /* Prepare link parameters TLV */
   sParamsTLV.buffer = pTLVBuffer;
   sParamsTLV.length = PHFRINFC_LLCP_MAX_PARAM_TLV_LENGTH;
   result = phFriNfc_Llcp_EncodeLinkParams(&sParamsTLV, psLinkParams, PHFRINFC_LLCP_VERSION);
   if (result != NFCSTATUS_SUCCESS)
   {
      /* Error while encoding */
      return NFCSTATUS_FAILED;
   }

   /* Check if ready to send */
   if (Llcp->state != PHFRINFC_LLCP_STATE_OPERATION_SEND)
   {
      /* No send pending, send the PAX packet */
      sHeader.dsap  = PHFRINFC_LLCP_SAP_LINK;
      sHeader.ssap  = PHFRINFC_LLCP_SAP_LINK;
      sHeader.ptype = PHFRINFC_LLCP_PTYPE_PAX;
      return phFriNfc_Llcp_InternalSend(Llcp, &sHeader, NULL, &sParamsTLV);
   }
   else
   {
      /* Error: A send is already pending, cannot send PAX */
      /* NOTE: this should not happen since PAX are sent before any other packet ! */
      return NFCSTATUS_FAILED;
   }
}


static NFCSTATUS phFriNfc_Llcp_SendDisconnect( phFriNfc_Llcp_t *Llcp )
{
   phFriNfc_Llcp_sPacketHeader_t sHeader;

   /* Check if ready to send */
   if (Llcp->state != PHFRINFC_LLCP_STATE_OPERATION_SEND)
   {
      /* No send pending, send the DISC packet */
      sHeader.dsap  = PHFRINFC_LLCP_SAP_LINK;
      sHeader.ssap  = PHFRINFC_LLCP_SAP_LINK;
      sHeader.ptype = PHFRINFC_LLCP_PTYPE_DISC;
      return phFriNfc_Llcp_InternalSend(Llcp, &sHeader, NULL, NULL);
   }
   else
   {
      /* A send is already pending, raise a flag to send DISC as soon as possible */
      Llcp->bDiscPendingFlag = TRUE;
      return NFCSTATUS_PENDING;
   }
}


static void phFriNfc_Llcp_Timer_CB(uint32_t TimerId, void *pContext)
{
   phFriNfc_Llcp_t               *Llcp = (phFriNfc_Llcp_t*)pContext;

   PHNFC_UNUSED_VARIABLE(TimerId);

   /* Check current state */
   if (Llcp->state == PHFRINFC_LLCP_STATE_OPERATION_RECV)
   {
      /* No data is coming before LTO, disconnecting */
      phFriNfc_Llcp_InternalDeactivate(Llcp);
   }
   else if (Llcp->state == PHFRINFC_LLCP_STATE_OPERATION_SEND)
   {
      /* Send SYMM */
      phFriNfc_Llcp_SendSymm(Llcp);
   }
   else
   {
      /* Nothing to do if not in Normal Operation state */
   }
}


static NFCSTATUS phFriNfc_Llcp_HandleAggregatedPacket( phFriNfc_Llcp_t *Llcp,
                                                       phNfc_sData_t *psRawPacket )
{
   phNfc_sData_t  sInfo;
   phNfc_sData_t  sCurrentInfo;
   uint16_t       length;
   NFCSTATUS      status;

   /* Get info field */
   sInfo.buffer = psRawPacket->buffer + PHFRINFC_LLCP_PACKET_HEADER_SIZE;
   sInfo.length = psRawPacket->length - PHFRINFC_LLCP_PACKET_HEADER_SIZE;

   /* Check for empty info field */
   if (sInfo.length == 0)
   {
      return NFCSTATUS_FAILED;
   }

   /* Check consistency */
   while (sInfo.length != 0)
   {
      /* Check if enough room to read length */
      if (sInfo.length < sizeof(sInfo.length))
      {
         return NFCSTATUS_FAILED;
      }
      /* Read length */
      length = (sInfo.buffer[0] << 8) | sInfo.buffer[1];
      /* Update info buffer */
      sInfo.buffer += 2; /*Size of length field is 2*/
      sInfo.length -= 2; /*Size of length field is 2*/
      /* Check if declared length fits in remaining space */
      if (length > sInfo.length)
      {
         return NFCSTATUS_FAILED;
      }
      /* Update info buffer */
      sInfo.buffer += length;
      sInfo.length -= length;
   }

   /* Get info field */
   sInfo.buffer = psRawPacket->buffer + PHFRINFC_LLCP_PACKET_HEADER_SIZE;
   sInfo.length = psRawPacket->length - PHFRINFC_LLCP_PACKET_HEADER_SIZE;

   /* Handle aggregated packets */
   while (sInfo.length != 0)
   {
      /* Read length */
      length = (sInfo.buffer[0] << 8) | sInfo.buffer[1];
      /* Update info buffer */
      sInfo.buffer += 2;        /* Size of length field is 2 */
      sInfo.length -= 2;    /*Size of length field is 2*/
      /* Handle aggregated packet */
      sCurrentInfo.buffer=sInfo.buffer;
      sCurrentInfo.length=length;
      status = phFriNfc_Llcp_HandleIncomingPacket(Llcp, &sCurrentInfo);
      if ( (status != NFCSTATUS_SUCCESS) &&
           (status != NFCSTATUS_PENDING) )
      {
         /* TODO: Error: invalid frame */
      }
      /* Update info buffer */
      sInfo.buffer += length;
      sInfo.length -= length;
   }
   return NFCSTATUS_SUCCESS;
}


static NFCSTATUS phFriNfc_Llcp_ParseLinkParams( phNfc_sData_t                    *psParamsTLV,
                                                phFriNfc_Llcp_sLinkParameters_t  *psParsedParams,
                                                uint8_t                          *pnParsedVersion )
{
   NFCSTATUS                        status;
   uint8_t                          type;
   phFriNfc_Llcp_sLinkParameters_t  sParams;
   phNfc_sData_t                    sValueBuffer;
   uint32_t                         offset = 0;
   uint8_t                          version = PHFRINFC_LLCP_INVALID_VERSION;

   /* Check for NULL pointers */
   if ((psParamsTLV == NULL) || (psParsedParams == NULL) || (pnParsedVersion == NULL))
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Prepare default param structure */
   sParams.miu    = PHFRINFC_LLCP_MIU_DEFAULT;
   sParams.wks    = PHFRINFC_LLCP_WKS_DEFAULT;
   sParams.lto    = PHFRINFC_LLCP_LTO_DEFAULT;
   sParams.option = PHFRINFC_LLCP_OPTION_DEFAULT;

   /* Decode TLV */
   while (offset < psParamsTLV->length)
   {
      status = phFriNfc_Llcp_DecodeTLV(psParamsTLV, &offset, &type, &sValueBuffer);
      if (status != NFCSTATUS_SUCCESS)
      {
         /* Error: Ill-formed TLV */
         return status;
      }
      switch(type)
      {
         case PHFRINFC_LLCP_TLV_TYPE_VERSION:
         {
            /* Check length */
            if (sValueBuffer.length != PHFRINFC_LLCP_TLV_LENGTH_VERSION)
            {
               /* Error : Ill-formed VERSION parameter TLV */
               break;
            }
            /* Get VERSION */
            version = sValueBuffer.buffer[0];
            break;
         }
         case PHFRINFC_LLCP_TLV_TYPE_MIUX:
         {
            /* Check length */
            if (sValueBuffer.length != PHFRINFC_LLCP_TLV_LENGTH_MIUX)
            {
               /* Error : Ill-formed MIUX parameter TLV */
               break;
            }
            /* Get MIU */
            sParams.miu = (PHFRINFC_LLCP_MIU_DEFAULT + ((sValueBuffer.buffer[0] << 8) | sValueBuffer.buffer[1])) & PHFRINFC_LLCP_TLV_MIUX_MASK;
            break;
         }
         case PHFRINFC_LLCP_TLV_TYPE_WKS:
         {
            /* Check length */
            if (sValueBuffer.length != PHFRINFC_LLCP_TLV_LENGTH_WKS)
            {
               /* Error : Ill-formed MIUX parameter TLV */
               break;
            }
            /* Get WKS */
            sParams.wks = (sValueBuffer.buffer[0] << 8) | sValueBuffer.buffer[1];
            /* Ignored bits must always be set */
            sParams.wks |= PHFRINFC_LLCP_TLV_WKS_MASK;
            break;
         }
         case PHFRINFC_LLCP_TLV_TYPE_LTO:
         {
            /* Check length */
            if (sValueBuffer.length != PHFRINFC_LLCP_TLV_LENGTH_LTO)
            {
               /* Error : Ill-formed LTO parameter TLV */
               break;
            }
            /* Get LTO */
            sParams.lto = sValueBuffer.buffer[0];
            break;
         }
         case PHFRINFC_LLCP_TLV_TYPE_OPT:
         {
            /* Check length */
            if (sValueBuffer.length != PHFRINFC_LLCP_TLV_LENGTH_OPT)
            {
               /* Error : Ill-formed OPT parameter TLV */
               break;;
            }
            /* Get OPT */
            sParams.option = sValueBuffer.buffer[0] & PHFRINFC_LLCP_TLV_OPT_MASK;
            break;
         }
         default:
         {
            /* Error : Unknown Type */
            break;
         }
      }
   }

   /* Check if a VERSION parameter has been provided */
   if (version == PHFRINFC_LLCP_INVALID_VERSION)
   {
      /* Error : Mandatory VERSION parameter not provided */
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Save response */
   *pnParsedVersion = version;
   memcpy(psParsedParams, &sParams, sizeof(phFriNfc_Llcp_sLinkParameters_t));

   return NFCSTATUS_SUCCESS;
}


static NFCSTATUS phFriNfc_Llcp_VersionAgreement( uint8_t localVersion,
                                                 uint8_t remoteVersion,
                                                 uint8_t *pNegociatedVersion )
{
   uint8_t     localMajor  = localVersion  & PHFRINFC_LLCP_VERSION_MAJOR_MASK;
   uint8_t     localMinor  = localVersion  & PHFRINFC_LLCP_VERSION_MINOR_MASK;
   uint8_t     remoteMajor = remoteVersion & PHFRINFC_LLCP_VERSION_MAJOR_MASK;
   uint8_t     remoteMinor = remoteVersion & PHFRINFC_LLCP_VERSION_MINOR_MASK;
   uint8_t     negociatedVersion;

   /* Check for NULL pointers */
   if (pNegociatedVersion == NULL)
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Compare Major numbers */
   if (localMajor == remoteMajor)
   {
      /* Version agreement succeed : use lowest version */
      negociatedVersion = localMajor | ((remoteMinor<localMinor)?remoteMinor:localMinor);
   }
   else if (localMajor > remoteMajor)
   {
      /* Decide if versions are compatible */
      /* Currently, there is no backward compatibility to handle */
      return NFCSTATUS_FAILED;
   }
   else /* if (localMajor < remoteMajor) */
   {
      /* It is up to the remote host to decide if versions are compatible */
      /* Set negociated version to our local version, the remote will
         deacivate the link if its own version agreement fails */
      negociatedVersion = localVersion;
   }

   /* Save response */
   *pNegociatedVersion = negociatedVersion;

   return NFCSTATUS_SUCCESS;
}


static NFCSTATUS phFriNfc_Llcp_InternalActivate( phFriNfc_Llcp_t *Llcp,
                                                 phNfc_sData_t   *psParamsTLV)
{
   NFCSTATUS                        status;
   phFriNfc_Llcp_sLinkParameters_t  sRemoteParams;
   uint8_t                          remoteVersion;
   uint8_t                          negociatedVersion;
   const uint16_t nMaxHeaderSize =  PHFRINFC_LLCP_PACKET_HEADER_SIZE +
                                    PHFRINFC_LLCP_PACKET_SEQUENCE_SIZE;

   /* Parse parameters  */
   status = phFriNfc_Llcp_ParseLinkParams(psParamsTLV, &sRemoteParams, &remoteVersion);
   if (status != NFCSTATUS_SUCCESS)
   {
      /* Error: invalid parameters TLV */
      status = NFCSTATUS_FAILED;
   }
   else
   {
      /* Version agreement procedure */
      status = phFriNfc_Llcp_VersionAgreement(PHFRINFC_LLCP_VERSION , remoteVersion, &negociatedVersion);
      if (status != NFCSTATUS_SUCCESS)
      {
         /* Error: version agreement failed */
         status = NFCSTATUS_FAILED;
      }
      else
      {
         /* Save parameters */
         Llcp->version = negociatedVersion;
         memcpy(&Llcp->sRemoteParams, &sRemoteParams, sizeof(phFriNfc_Llcp_sLinkParameters_t));

         /* Update remote MIU to match local Tx buffer size */
         if (Llcp->nTxBufferLength < (Llcp->sRemoteParams.miu + nMaxHeaderSize))
         {
            Llcp->sRemoteParams.miu = Llcp->nTxBufferLength - nMaxHeaderSize;
         }

         /* Initiate Symmetry procedure by resetting LTO timer */
         /* NOTE: this also updates current state */
         phFriNfc_Llcp_ResetLTO(Llcp);
      }
   }
   /* Notify upper layer, if Activation failed CB called by Deactivate */
   if (status == NFCSTATUS_SUCCESS)
   {
      /* Link activated ! */
      Llcp->pfLink_CB(Llcp->pLinkContext, phFriNfc_LlcpMac_eLinkActivated);
   }

   return status;
}


static NFCSTATUS phFriNfc_Llcp_HandleMACLinkActivated( phFriNfc_Llcp_t  *Llcp,
                                                       phNfc_sData_t    *psParamsTLV)
{
   NFCSTATUS                     status = NFCSTATUS_SUCCESS;

   /* Create the timer */
   Llcp->hSymmTimer = phOsalNfc_Timer_Create();
   if (Llcp->hSymmTimer == PH_OSALNFC_INVALID_TIMER_ID)
   {
      /* Error: unable to create timer */
      return NFCSTATUS_INSUFFICIENT_RESOURCES;
   }

   /* Check if params received from MAC activation procedure */
   if (psParamsTLV == NULL)
   {
      /* No params with selected MAC mapping, enter PAX mode for parameter exchange */
      Llcp->state = PHFRINFC_LLCP_STATE_PAX;
      /* Set default MIU for PAX exchange */
      Llcp->sRemoteParams.miu = PHFRINFC_LLCP_MIU_DEFAULT;
      /* If the local device is the initiator, it must initiate PAX exchange */
      if (Llcp->eRole == phFriNfc_LlcpMac_ePeerTypeInitiator)
      {
         /* Send PAX */
         status = phFriNfc_Llcp_SendPax(Llcp, &Llcp->sLocalParams);
      }
   }
   else
   {
      /* Params exchanged during MAX activation, try LLC activation */
      status = phFriNfc_Llcp_InternalActivate(Llcp, psParamsTLV);
   }

   if (status == NFCSTATUS_SUCCESS)
   {
      /* Start listening for incoming packets */
      Llcp->sRxBuffer.length = Llcp->nRxBufferLength;
      phFriNfc_LlcpMac_Receive(&Llcp->MAC, &Llcp->sRxBuffer, phFriNfc_Llcp_Receive_CB, Llcp);
   }

   return status;
}


static void phFriNfc_Llcp_HandleMACLinkDeactivated( phFriNfc_Llcp_t  *Llcp )
{
   uint8_t state = Llcp->state;

   /* Delete the timer */
   if (Llcp->hSymmTimer != PH_OSALNFC_INVALID_TIMER_ID)
   {
      phOsalNfc_Timer_Delete(Llcp->hSymmTimer);
   }

   /* Reset state */
   Llcp->state = PHFRINFC_LLCP_STATE_RESET_INIT;

   switch (state)
   {
      case PHFRINFC_LLCP_STATE_DEACTIVATION:
      {
         /* The service layer has already been notified, nothing more to do */
         break;
      }
      default:
      {
         /* Notify service layer of link failure */
         Llcp->pfLink_CB(Llcp->pLinkContext, phFriNfc_LlcpMac_eLinkDeactivated);
         break;
      }
   }
}


static void phFriNfc_Llcp_ChkLlcp_CB( void       *pContext,
                                      NFCSTATUS  status )
{
   /* Get monitor from context */
   phFriNfc_Llcp_t *Llcp = (phFriNfc_Llcp_t*)pContext;

   /* Update state */
   Llcp->state = PHFRINFC_LLCP_STATE_CHECKED;

   /* Invoke callback */
   Llcp->pfChk_CB(Llcp->pChkContext, status);
}

static void phFriNfc_Llcp_LinkStatus_CB( void                              *pContext,
                                         phFriNfc_LlcpMac_eLinkStatus_t    eLinkStatus,
                                         phNfc_sData_t                     *psParamsTLV,
                                         phFriNfc_LlcpMac_ePeerType_t      PeerRemoteDevType)
{
   NFCSTATUS status;

   /* Get monitor from context */
   phFriNfc_Llcp_t *Llcp = (phFriNfc_Llcp_t*)pContext;

   /* Save the local peer role (initiator/target) */
   Llcp->eRole = PeerRemoteDevType;

   /* Check new link status */
   switch(eLinkStatus)
   {
      case phFriNfc_LlcpMac_eLinkActivated:
      {
         /* Handle MAC link activation */
         status = phFriNfc_Llcp_HandleMACLinkActivated(Llcp, psParamsTLV);
         if (status != NFCSTATUS_SUCCESS)
         {
            /* Error: LLC link activation procedure failed, deactivate MAC link */
            status = phFriNfc_Llcp_InternalDeactivate(Llcp);
         }
         break;
      }
      case phFriNfc_LlcpMac_eLinkDeactivated:
      {
         /* Handle MAC link deactivation (cannot fail) */
         phFriNfc_Llcp_HandleMACLinkDeactivated(Llcp);
         break;
      }
      default:
      {
         /* Warning: Unknown link status, should not happen */
      }
   }
}


static void phFriNfc_Llcp_ResetLTO( phFriNfc_Llcp_t *Llcp )
{
   uint32_t nDuration = 0;
   uint8_t bIsReset = 0;

   /* Stop timer */
   phOsalNfc_Timer_Stop(Llcp->hSymmTimer);


   /* Update state */
   if (Llcp->state == PHFRINFC_LLCP_STATE_OPERATION_RECV)
   {
      Llcp->state = PHFRINFC_LLCP_STATE_OPERATION_SEND;
   }
   else if (Llcp->state == PHFRINFC_LLCP_STATE_OPERATION_SEND)
   {
      Llcp->state = PHFRINFC_LLCP_STATE_OPERATION_RECV;
   }
   else if (Llcp->state != PHFRINFC_LLCP_STATE_DEACTIVATION &&
            Llcp->state != PHFRINFC_LLCP_STATE_RESET_INIT)
   {
      bIsReset = 1;
      /* Not yet in OPERATION state, perform first reset */
      if (Llcp->eRole == phFriNfc_LlcpMac_ePeerTypeInitiator)
      {
         Llcp->state = PHFRINFC_LLCP_STATE_OPERATION_SEND;
      }
      else
      {
         Llcp->state = PHFRINFC_LLCP_STATE_OPERATION_RECV;
      }
   }

   /* Calculate timer duration */
   /* NOTE: nDuration is in 1/100s, and timer system takes values in 1/1000s */
   if (Llcp->state == PHFRINFC_LLCP_STATE_OPERATION_RECV)
   {
      /* Response must be received before LTO announced by remote peer */
      nDuration = Llcp->sRemoteParams.lto * 10;
   }
   else
   {
      if (bIsReset)
      {
          /* Immediately bounce SYMM back - it'll take
           * a while for the host to come up with something,
           * and maybe the remote is faster.
           */
          nDuration = 1;
      }
      else
      {
          /* Must answer before the local announced LTO */
          /* NOTE: to ensure the answer is completely sent before LTO, the
                  timer is triggered _before_ LTO expiration */
          /* TODO: make sure time scope is enough, and avoid use of magic number */
          nDuration = (Llcp->sLocalParams.lto * 10) / 2;
      }
   }

   LLCP_DEBUG("Starting LLCP timer with duration %d", nDuration);

   /* Restart timer */
   phOsalNfc_Timer_Start(
      Llcp->hSymmTimer,
      nDuration,
      phFriNfc_Llcp_Timer_CB,
      Llcp);
}


static NFCSTATUS phFriNfc_Llcp_HandleLinkPacket( phFriNfc_Llcp_t    *Llcp,
                                                 phNfc_sData_t      *psPacket )
{
   NFCSTATUS                        result;
   phFriNfc_Llcp_sPacketHeader_t    sHeader;

   /* Parse header */
   phFriNfc_Llcp_Buffer2Header(psPacket->buffer, 0, &sHeader);

   /* Check packet type */
   switch (sHeader.ptype)
   {
      case PHFRINFC_LLCP_PTYPE_SYMM:
      {
         /* Nothing to do, the LTO is handled upon all packet reception */
         result = NFCSTATUS_SUCCESS;
         break;
      }
      
      case PHFRINFC_LLCP_PTYPE_AGF:
      {
         /* Handle the aggregated packet */
         result = phFriNfc_Llcp_HandleAggregatedPacket(Llcp, psPacket);
         if (result != NFCSTATUS_SUCCESS)
         {
            /* Error: invalid info field, dropping frame */
         }
         break;
      }
      
      case PHFRINFC_LLCP_PTYPE_DISC:
      {
         /* Handle link disconnection request */
         result = phFriNfc_Llcp_InternalDeactivate(Llcp);
         break;
      }
      
     
      case PHFRINFC_LLCP_PTYPE_FRMR:
      {
         /* TODO: what to do upon reception of FRMR on Link SAP ? */
         result = NFCSTATUS_SUCCESS;
         break;
      }

      case PHFRINFC_LLCP_PTYPE_PAX:
      {
         /* Ignore PAX when in Normal Operation */
         result = NFCSTATUS_SUCCESS;
         break;
      }

      default:
      {
         /* Error: invalid ptype field, dropping packet */
         break;
      }
   }

   return result;
}


static NFCSTATUS phFriNfc_Llcp_HandleTransportPacket( phFriNfc_Llcp_t    *Llcp,
                                                      phNfc_sData_t      *psPacket )
{
   phFriNfc_Llcp_Recv_CB_t          pfRecvCB;
   void                             *pContext;
   NFCSTATUS                        result = NFCSTATUS_SUCCESS;
   phFriNfc_Llcp_sPacketHeader_t    sHeader;

   /* Forward to upper layer */
   if (Llcp->pfRecvCB != NULL)
   {
      /* Get callback details */
      pfRecvCB = Llcp->pfRecvCB;
      pContext = Llcp->pRecvContext;
      /* Reset callback details */
      Llcp->pfRecvCB = NULL;
      Llcp->pRecvContext = NULL;
      /* Call the callback */
      (pfRecvCB)(pContext, psPacket, NFCSTATUS_SUCCESS);
   }

   return result;
}


static bool_t phFriNfc_Llcp_HandlePendingSend ( phFriNfc_Llcp_t *Llcp )
{
   phFriNfc_Llcp_sPacketHeader_t    sHeader;
   phNfc_sData_t                    sInfoBuffer;
   phFriNfc_Llcp_sPacketHeader_t    *psSendHeader = NULL;
   phFriNfc_Llcp_sPacketSequence_t  *psSendSequence = NULL;
   phNfc_sData_t                    *psSendInfo = NULL;
   NFCSTATUS                        result;
   uint8_t                          bDeallocate = FALSE;
   uint8_t                          return_value = FALSE;
   /* Handle pending disconnection request */
   if (Llcp->bDiscPendingFlag == TRUE)
   {
      /* Last send si acheived, send the pending DISC packet */
      sHeader.dsap  = PHFRINFC_LLCP_SAP_LINK;
      sHeader.ssap  = PHFRINFC_LLCP_SAP_LINK;
      sHeader.ptype = PHFRINFC_LLCP_PTYPE_DISC;
      /* Set send params */
      psSendHeader = &sHeader;
      /* Reset flag */
      Llcp->bDiscPendingFlag = FALSE;
   }
   /* Handle pending frame reject request */
   else if (Llcp->bFrmrPendingFlag == TRUE)
   {
      /* Last send si acheived, send the pending FRMR packet */
      sInfoBuffer.buffer = Llcp->pFrmrInfo;
      sInfoBuffer.length = sizeof(Llcp->pFrmrInfo);
      /* Set send params */
      psSendHeader = &Llcp->sFrmrHeader;
      psSendInfo   = &sInfoBuffer;
      /* Reset flag */
      Llcp->bFrmrPendingFlag = FALSE;
   }
   /* Handle pending service frame */
   else if (Llcp->pfSendCB != NULL)
   {
      /* Set send params */
      psSendHeader = Llcp->psSendHeader;
      psSendSequence = Llcp->psSendSequence;
      psSendInfo = Llcp->psSendInfo;
      /* Reset pending send infos */
      Llcp->psSendHeader = NULL;
      Llcp->psSendSequence = NULL;
      Llcp->psSendInfo = NULL;
      bDeallocate = TRUE;
   }

   /* Perform send, if needed */
   if (psSendHeader != NULL)
   {
      result = phFriNfc_Llcp_InternalSend(Llcp, psSendHeader, psSendSequence, psSendInfo);
      if ((result != NFCSTATUS_SUCCESS) && (result != NFCSTATUS_PENDING))
      {
         /* Error: send failed, impossible to recover */
         phFriNfc_Llcp_InternalDeactivate(Llcp);
      }
      return_value = TRUE;
   } else if (Llcp->pfSendCB == NULL) {
      // Nothing to send, send SYMM instead to allow peer to send something
      // if it wants.
      phFriNfc_Llcp_SendSymm(Llcp);
      return_value = TRUE;
   }

clean_and_return:
   if (bDeallocate)
   {
       phFriNfc_Llcp_Deallocate(psSendInfo);
   }

   return return_value;
}

static NFCSTATUS phFriNfc_Llcp_HandleIncomingPacket( phFriNfc_Llcp_t    *Llcp,
                                                     phNfc_sData_t      *psPacket )
{
   NFCSTATUS                        status = NFCSTATUS_SUCCESS;
   phFriNfc_Llcp_sPacketHeader_t    sHeader;

   /* Parse header */
   phFriNfc_Llcp_Buffer2Header(psPacket->buffer, 0, &sHeader);

   /* Check destination */
   if (sHeader.dsap == PHFRINFC_LLCP_SAP_LINK)
   {
      /* Handle packet as destinated to the Link SAP */
      status = phFriNfc_Llcp_HandleLinkPacket(Llcp, psPacket);
   }
   else if (sHeader.dsap >= PHFRINFC_LLCP_SAP_NUMBER)
   {
     /* NOTE: this cannot happen since "psHeader->dsap" is only 6-bit wide */
     status = NFCSTATUS_FAILED;
   }
   else
   {
      /* Handle packet as destinated to the SDP and transport SAPs */
      status = phFriNfc_Llcp_HandleTransportPacket(Llcp, psPacket);
   }
   return status;
}


static void phFriNfc_Llcp_Receive_CB( void               *pContext,
                                      NFCSTATUS          status,
                                      phNfc_sData_t      *psData)
{
   /* Get monitor from context */
   phFriNfc_Llcp_t               *Llcp = (phFriNfc_Llcp_t*)pContext;
   NFCSTATUS                     result = NFCSTATUS_SUCCESS;
   phFriNfc_Llcp_sPacketHeader_t sPacketHeader;

   /* Check reception status and for pending disconnection */
   if ((status != NFCSTATUS_SUCCESS) || (Llcp->bDiscPendingFlag == TRUE))
   {
	  LLCP_DEBUG("\nReceived LLCP packet error - status = 0x%04x", status);
      /* Reset disconnection operation */
      Llcp->bDiscPendingFlag = FALSE;
      /* Deactivate the link */
      phFriNfc_Llcp_InternalDeactivate(Llcp);
      return;
   }

   /* Parse header */
   phFriNfc_Llcp_Buffer2Header(psData->buffer, 0, &sPacketHeader);

   if (sPacketHeader.ptype != PHFRINFC_LLCP_PTYPE_SYMM)
   {
      LLCP_PRINT_BUFFER("\nReceived LLCP packet :", psData->buffer, psData->length);
   }
   else
   {
      LLCP_PRINT("?");
   }


   /* Check new link status */
   switch(Llcp->state)
   {
      /* Handle packets in PAX-waiting state */
      case PHFRINFC_LLCP_STATE_PAX:
      {
         /* Check packet type */
         if (sPacketHeader.ptype == PHFRINFC_LLCP_PTYPE_PAX)
         {
            /* Params exchanged during MAC activation, try LLC activation */
            result = phFriNfc_Llcp_InternalActivate(Llcp, psData+PHFRINFC_LLCP_PACKET_HEADER_SIZE);
            /* If the local LLC is the target, it must answer the PAX */
            if (Llcp->eRole == phFriNfc_LlcpMac_ePeerTypeTarget)
            {
               /* Send PAX */
               result = phFriNfc_Llcp_SendPax(Llcp, &Llcp->sLocalParams);
            }
         }
         else
         {
            /* Warning: Received packet with unhandled type in PAX-waiting state, drop it */
         }
         break;
      }

      /* Handle normal operation packets */
      case PHFRINFC_LLCP_STATE_OPERATION_RECV:
      case PHFRINFC_LLCP_STATE_OPERATION_SEND:
      {
         /* Handle Symmetry procedure by resetting LTO timer */
         phFriNfc_Llcp_ResetLTO(Llcp);
         /* Handle packet */
         result = phFriNfc_Llcp_HandleIncomingPacket(Llcp, psData);
         if ( (result != NFCSTATUS_SUCCESS) &&
              (result != NFCSTATUS_PENDING) )
         {
            /* TODO: Error: invalid frame */
         }
         /* Perform pending send request, if any */
         phFriNfc_Llcp_HandlePendingSend(Llcp);
         break;
      }

      default:
      {
         /* Warning: Should not receive packets in other states, drop them */
      }
   }

   /* Restart reception */
   Llcp->sRxBuffer.length = Llcp->nRxBufferLength;
   phFriNfc_LlcpMac_Receive(&Llcp->MAC, &Llcp->sRxBuffer, phFriNfc_Llcp_Receive_CB, Llcp);
}


static void phFriNfc_Llcp_Send_CB( void               *pContext,
                                   NFCSTATUS          status )
{
   /* Get monitor from context */
   phFriNfc_Llcp_t                  *Llcp = (phFriNfc_Llcp_t*)pContext;
   phFriNfc_Llcp_Send_CB_t          pfSendCB;
   void                             *pSendContext;

   /* Call the upper layer callback if last packet sent was  */
   /* NOTE: if Llcp->psSendHeader is not NULL, this means that the send operation is still not initiated */
   if (Llcp->psSendHeader == NULL)
   {
      if (Llcp->pfSendCB != NULL)
      {
         /* Get Callback params */
         pfSendCB = Llcp->pfSendCB;
         pSendContext = Llcp->pSendContext;
         /* Reset callback params */
         Llcp->pfSendCB = NULL;
         Llcp->pSendContext = NULL;
         /* Call the callback */
         (pfSendCB)(pSendContext, status);
      }
   }

   /* Check reception status */
   if (status != NFCSTATUS_SUCCESS)
   {
       /* Error: Reception failed, link must be down */
       phFriNfc_Llcp_InternalDeactivate(Llcp);
   }
}


static NFCSTATUS phFriNfc_Llcp_InternalSend( phFriNfc_Llcp_t                    *Llcp,
                                             phFriNfc_Llcp_sPacketHeader_t      *psHeader,
                                             phFriNfc_Llcp_sPacketSequence_t    *psSequence,
                                             phNfc_sData_t                      *psInfo )
{
   NFCSTATUS status;
   phNfc_sData_t  *psRawPacket = &Llcp->sTxBuffer; /* Use internal Tx buffer */

   /* Handle Symmetry procedure */
   phFriNfc_Llcp_ResetLTO(Llcp);

   /* Generate raw packet to send (aggregate header + sequence + info fields) */
   psRawPacket->length = 0;
   psRawPacket->length += phFriNfc_Llcp_Header2Buffer(psHeader, psRawPacket->buffer, psRawPacket->length);
   if (psSequence != NULL)
   {
      psRawPacket->length += phFriNfc_Llcp_Sequence2Buffer(psSequence, psRawPacket->buffer, psRawPacket->length);
   }
   if (psInfo != NULL)
   {
      memcpy(psRawPacket->buffer + psRawPacket->length, psInfo->buffer, psInfo->length);
      psRawPacket->length += psInfo->length;
   }

   if (psHeader->ptype != PHFRINFC_LLCP_PTYPE_SYMM)
   {
      LLCP_PRINT_BUFFER("\nSending LLCP packet :", psRawPacket->buffer, psRawPacket->length);
   }
   else
   {
      LLCP_PRINT("!");
   }

   /* Send raw packet */
   status = phFriNfc_LlcpMac_Send (
               &Llcp->MAC,
               psRawPacket,
               phFriNfc_Llcp_Send_CB,
               Llcp );

   return status;
}

/* ---------------------------- Public functions ------------------------------- */

NFCSTATUS phFriNfc_Llcp_EncodeLinkParams( phNfc_sData_t                   *psRawBuffer,
                                          phFriNfc_Llcp_sLinkParameters_t *psLinkParams,
                                          uint8_t                         nVersion )
{
   uint32_t    nOffset = 0;
   uint16_t    miux;
   uint16_t    wks;
   uint8_t     pValue[2];
   NFCSTATUS   result = NFCSTATUS_SUCCESS;

   /* Check parameters */
   if ((psRawBuffer == NULL) || (psLinkParams == NULL))
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Encode mandatory VERSION field */
   if (result == NFCSTATUS_SUCCESS)
   {
      result = phFriNfc_Llcp_EncodeTLV(
                  psRawBuffer,
                  &nOffset,
                  PHFRINFC_LLCP_TLV_TYPE_VERSION,
                  PHFRINFC_LLCP_TLV_LENGTH_VERSION,
                  &nVersion);
   }

   /* Encode mandatory VERSION field */
   if (result == NFCSTATUS_SUCCESS)
   {
      /* Encode MIUX field, if needed */
      if (psLinkParams->miu != PHFRINFC_LLCP_MIU_DEFAULT)
      {
         miux = (psLinkParams->miu - PHFRINFC_LLCP_MIU_DEFAULT) & PHFRINFC_LLCP_TLV_MIUX_MASK;
         pValue[0] = (miux >> 8) & 0xFF;
         pValue[1] =  miux       & 0xFF;
         result = phFriNfc_Llcp_EncodeTLV(
                     psRawBuffer,
                     &nOffset,
                     PHFRINFC_LLCP_TLV_TYPE_MIUX,
                     PHFRINFC_LLCP_TLV_LENGTH_MIUX,
                     pValue);
      }
   }

   /* Encode WKS field */
   if (result == NFCSTATUS_SUCCESS)
   {
      wks = psLinkParams->wks | PHFRINFC_LLCP_TLV_WKS_MASK;
      pValue[0] = (wks >> 8) & 0xFF;
      pValue[1] =  wks       & 0xFF;
      result = phFriNfc_Llcp_EncodeTLV(
                  psRawBuffer,
                  &nOffset,
                  PHFRINFC_LLCP_TLV_TYPE_WKS,
                  PHFRINFC_LLCP_TLV_LENGTH_WKS,
                  pValue);
   }

   /* Encode LTO field, if needed */
   if (result == NFCSTATUS_SUCCESS)
   {
      if (psLinkParams->lto != PHFRINFC_LLCP_LTO_DEFAULT)
      {
         result = phFriNfc_Llcp_EncodeTLV(
                     psRawBuffer,
                     &nOffset,
                     PHFRINFC_LLCP_TLV_TYPE_LTO,
                     PHFRINFC_LLCP_TLV_LENGTH_LTO,
                     &psLinkParams->lto);
      }
   }

   /* Encode OPT field, if needed */
   if (result == NFCSTATUS_SUCCESS)
   {
      if (psLinkParams->option != PHFRINFC_LLCP_OPTION_DEFAULT)
      {
         result = phFriNfc_Llcp_EncodeTLV(
                     psRawBuffer,
                     &nOffset,
                     PHFRINFC_LLCP_TLV_TYPE_OPT,
                     PHFRINFC_LLCP_TLV_LENGTH_OPT,
                     &psLinkParams->option);
      }
   }

   if (result != NFCSTATUS_SUCCESS)
   {
      /* Error: failed to encode TLV */
      return NFCSTATUS_FAILED;
   }

   /* Save new buffer size */
   psRawBuffer->length = nOffset;

   return result;
}


NFCSTATUS phFriNfc_Llcp_Reset( phFriNfc_Llcp_t                 *Llcp,
                               void                            *LowerDevice,
                               phFriNfc_Llcp_sLinkParameters_t *psLinkParams,
                               void                            *pRxBuffer,
                               uint16_t                        nRxBufferLength,
                               void                            *pTxBuffer,
                               uint16_t                        nTxBufferLength,
                               phFriNfc_Llcp_LinkStatus_CB_t   pfLink_CB,
                               void                            *pContext )
{
   const uint16_t nMaxHeaderSize =  PHFRINFC_LLCP_PACKET_HEADER_SIZE +
                                    PHFRINFC_LLCP_PACKET_SEQUENCE_SIZE;
   NFCSTATUS result;

   /* Check parameters presence */
   if ((Llcp == NULL) || (LowerDevice == NULL) || (pfLink_CB == NULL) ||
       (pRxBuffer == NULL) || (pTxBuffer == NULL) )
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Check parameters value */
   if (psLinkParams->miu < PHFRINFC_LLCP_MIU_DEFAULT)
   {
      return NFCSTATUS_INVALID_PARAMETER;
   }

   /* Check if buffers are large enough to support minimal MIU */
   if ((nRxBufferLength < (nMaxHeaderSize + PHFRINFC_LLCP_MIU_DEFAULT)) ||
       (nTxBufferLength < (nMaxHeaderSize + PHFRINFC_LLCP_MIU_DEFAULT)) )
   {
      return NFCSTATUS_BUFFER_TOO_SMALL;
   }

   /* Check compatibility between reception buffer size and announced MIU */
   if (nRxBufferLength < (nMaxHeaderSize + psLinkParams->miu))
   {
      return NFCSTATUS_BUFFER_TOO_SMALL;
   }

   /* Start with a zero-filled monitor */
   memset(Llcp, 0x00, sizeof(phFriNfc_Llcp_t));

   /* Reset the MAC Mapping layer */
   result = phFriNfc_LlcpMac_Reset(&Llcp->MAC, LowerDevice, phFriNfc_Llcp_LinkStatus_CB, Llcp);
   if (result != NFCSTATUS_SUCCESS) {
      return result;
   }

   /* Save the working buffers */
   Llcp->sRxBuffer.buffer = pRxBuffer;
   Llcp->sRxBuffer.length = nRxBufferLength;
   Llcp->nRxBufferLength = nRxBufferLength;
   Llcp->sTxBuffer.buffer = pTxBuffer;
   Llcp->sTxBuffer.length = nTxBufferLength;
   Llcp->nTxBufferLength = nTxBufferLength;

   /* Save the link status callback references */
   Llcp->pfLink_CB = pfLink_CB;
   Llcp->pLinkContext = pContext;

   /* Save the local link parameters */
   memcpy(&Llcp->sLocalParams, psLinkParams, sizeof(phFriNfc_Llcp_sLinkParameters_t));

   return NFCSTATUS_SUCCESS;
}


NFCSTATUS phFriNfc_Llcp_ChkLlcp( phFriNfc_Llcp_t               *Llcp,
                                 phHal_sRemoteDevInformation_t *psRemoteDevInfo,
                                 phFriNfc_Llcp_Check_CB_t      pfCheck_CB,
                                 void                          *pContext )
{
   /* Check parameters */
   if ( (Llcp == NULL) || (psRemoteDevInfo == NULL) || (pfCheck_CB == NULL) )
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Check current state */
   if( Llcp->state != PHFRINFC_LLCP_STATE_RESET_INIT ) {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_STATE);
   }

   /* Save the compliance check callback */
   Llcp->pfChk_CB = pfCheck_CB;
   Llcp->pChkContext = pContext;

   /* Forward check request to MAC layer */
   return phFriNfc_LlcpMac_ChkLlcp(&Llcp->MAC, psRemoteDevInfo, phFriNfc_Llcp_ChkLlcp_CB, (void*)Llcp);
}


NFCSTATUS phFriNfc_Llcp_Activate( phFriNfc_Llcp_t *Llcp )
{
   /* Check parameters */
   if (Llcp == NULL)
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Check current state */
   if( Llcp->state != PHFRINFC_LLCP_STATE_CHECKED ) {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_STATE);
   }

   /* Update state */
   Llcp->state = PHFRINFC_LLCP_STATE_ACTIVATION;

   /* Reset any headers to send */
   Llcp->psSendHeader = NULL;
   Llcp->psSendSequence = NULL;

   /* Forward check request to MAC layer */
   return phFriNfc_LlcpMac_Activate(&Llcp->MAC);
}


NFCSTATUS phFriNfc_Llcp_Deactivate( phFriNfc_Llcp_t *Llcp )
{
   NFCSTATUS status;

   /* Check parameters */
   if (Llcp == NULL)
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Check current state */
   if( (Llcp->state != PHFRINFC_LLCP_STATE_OPERATION_RECV) &&
       (Llcp->state != PHFRINFC_LLCP_STATE_OPERATION_SEND) ) {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_STATE);
   }

   /* Send DISC packet */
   status = phFriNfc_Llcp_SendDisconnect(Llcp);
   if (status == NFCSTATUS_PENDING)
   {
      /* Wait for packet to be sent before deactivate link */
      return status;
   }

   /* Perform actual deactivation */
   return phFriNfc_Llcp_InternalDeactivate(Llcp);
}


NFCSTATUS phFriNfc_Llcp_GetLocalInfo( phFriNfc_Llcp_t                   *Llcp,
                                      phFriNfc_Llcp_sLinkParameters_t   *pParams )
{
   /* Check parameters */
   if ((Llcp == NULL) || (pParams == NULL))
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Copy response */
   memcpy(pParams, &Llcp->sLocalParams, sizeof(phFriNfc_Llcp_sLinkParameters_t));

   return NFCSTATUS_SUCCESS;
}


NFCSTATUS phFriNfc_Llcp_GetRemoteInfo( phFriNfc_Llcp_t                  *Llcp,
                                       phFriNfc_Llcp_sLinkParameters_t  *pParams )
{
   /* Check parameters */
   if ((Llcp == NULL) || (pParams == NULL))
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Copy response */
   memcpy(pParams, &Llcp->sRemoteParams, sizeof(phFriNfc_Llcp_sLinkParameters_t));

   return NFCSTATUS_SUCCESS;
}


NFCSTATUS phFriNfc_Llcp_Send( phFriNfc_Llcp_t                  *Llcp,
                              phFriNfc_Llcp_sPacketHeader_t    *psHeader,
                              phFriNfc_Llcp_sPacketSequence_t  *psSequence,
                              phNfc_sData_t                    *psInfo,
                              phFriNfc_Llcp_Send_CB_t          pfSend_CB,
                              void                             *pContext )
{
   NFCSTATUS result;
   /* Check parameters */
   if ((Llcp == NULL) || (psHeader == NULL) || (pfSend_CB == NULL))
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Check if previous phFriNfc_Llcp_Send() has finished */
   if (Llcp->pfSendCB != NULL)
   {
      /* Error: a send operation is already running */
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_REJECTED);
   }

   /* Save the callback parameters */
   Llcp->pfSendCB = pfSend_CB;
   Llcp->pSendContext = pContext;

   if (Llcp->state == PHFRINFC_LLCP_STATE_OPERATION_SEND)
   {
      /* Ready to send */
      result = phFriNfc_Llcp_InternalSend(Llcp, psHeader, psSequence, psInfo);
   }
   else if (Llcp->state == PHFRINFC_LLCP_STATE_OPERATION_RECV)
   {
      /* Not ready to send, save send params for later use */
      Llcp->psSendHeader = psHeader;
      Llcp->psSendSequence = psSequence;
      Llcp->psSendInfo = phFriNfc_Llcp_AllocateAndCopy(psInfo);
      result = NFCSTATUS_PENDING;
   }
   else
   {
      /* Incorrect state for sending ! */
      result = PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_STATE);;
   }

   if (result != NFCSTATUS_PENDING) {
       Llcp->pfSendCB = NULL;
   }
   return result;
}


NFCSTATUS phFriNfc_Llcp_Recv( phFriNfc_Llcp_t            *Llcp,
                              phFriNfc_Llcp_Recv_CB_t    pfRecv_CB,
                              void                       *pContext )
{
   NFCSTATUS result = NFCSTATUS_SUCCESS;

   /* Check parameters */
   if ((Llcp == NULL) || (pfRecv_CB == NULL))
   {
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_INVALID_PARAMETER);
   }

   /* Check if previous phFriNfc_Llcp_Recv() has finished */
   if (Llcp->pfRecvCB != NULL)
   {
      /* Error: a send operation is already running */
      return PHNFCSTVAL(CID_FRI_NFC_LLCP, NFCSTATUS_REJECTED);
   }

   /* Save the callback parameters */
   Llcp->pfRecvCB = pfRecv_CB;
   Llcp->pRecvContext = pContext;

   /* NOTE: nothing more to do, the receive function is called in background */

   return result;
}
