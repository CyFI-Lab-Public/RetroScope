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
 * \file  phFriNfc_LlcpMac.h
 * \brief NFC LLCP MAC Mappings For Different RF Technologies.
 *
 * Project: NFC-FRI
 *
 */

#ifndef PHFRINFC_LLCPMAC_H
#define PHFRINFC_LLCPMAC_H


/*include files*/
#include <phNfcTypes.h>
#include <phNfcLlcpTypes.h>
#include <phNfcStatus.h>
#include <phFriNfc.h>

#include <phFriNfc_OvrHal.h>

/** 
 * \name LLCP MAC Mapping
 *
 * File: \ref phFriNfc_LlcpMac.h
 *
 */


/** \defgroup grp_fri_nfc_llcp_mac LLCP MAC Mapping Component
 *
 *  This component implements the different MAC mapping for a Logical Link Control Protocol communication,
 *  as defined by the NFC Forum LLCP specifications.\n
 *  The MAC component handles the mapping for the different technologies supported by LLCP
 *..This component provides an API to the upper layer with the following features:\n\n
 *  - Reset the MAC mapping component
 *      - \ref phFriNfc_LlcpMac_ChkLlcp
 *      .
 *  - Check the LLCP Compliancy
 *      - \ref phFriNfc_LlcpMac_ChkLlcp
 *      .
 *  - Activate the LLCP link
 *      - \ref phFriNfc_LlcpMac_Activate
 *      .
 *  - Deactivate the LLCP link
 *      - \ref phFriNfc_LlcpMac_Deactivate
 *      .
 *  - Register the MAC component Interface with a specific technologie (NFCIP/ISO14443)
 *      - \ref phFriNfc_LlcpMac_Register
 *      .
 *  - Send packets through the LLCP link
 *      - \ref phFriNfc_LlcpMac_Send
 *      .
  *  - Receive packets through the LLCP link
 *      - \ref phFriNfc_LlcpMac_Receive
 *  
 */

/**
 * \ingroup grp_fri_nfc_llcp_mac
 * \brief Declaration of a MAC type
 */
struct phFriNfc_LlcpMac;
typedef struct phFriNfc_LlcpMac phFriNfc_LlcpMac_t;

/**
 *  \ingroup grp_fri_nfc_llcp_mac
 *
 */
/*========== ENUMERATES ===========*/

/* Enum reperesents the different MAC mapping*/
typedef enum phFriNfc_LlcpMac_eType
{
   phFriNfc_LlcpMac_eTypeNfcip,
   phFriNfc_LlcpMac_eTypeIso14443   
}phFriNfc_LlcpMac_eType_t;

/* Enum reperesents the different Peer type for a LLCP communication*/
typedef enum phFriNfc_LlcpMac_ePeerType
{
   phFriNfc_LlcpMac_ePeerTypeInitiator,
   phFriNfc_LlcpMac_ePeerTypeTarget
}phFriNfc_LlcpMac_ePeerType_t;






/*========== CALLBACKS ===========*/

typedef void (*phFriNfc_LlcpMac_Chk_CB_t) (void        *pContext,
                                           NFCSTATUS   status);

typedef void (*phFriNfc_LlcpMac_LinkStatus_CB_t) (void                             *pContext,
                                                  phFriNfc_LlcpMac_eLinkStatus_t   eLinkStatus,
                                                  phNfc_sData_t                    *psData,
                                                  phFriNfc_LlcpMac_ePeerType_t     PeerRemoteDevType);

typedef void (*phFriNfc_LlcpMac_Send_CB_t) (void            *pContext,
                                            NFCSTATUS       status);


typedef void (*phFriNfc_LlcpMac_Reveive_CB_t) (void               *pContext,
                                               NFCSTATUS          status,
                                               phNfc_sData_t      *psData);


/*========== FUNCTIONS TYPES ===========*/

typedef NFCSTATUS (*pphFriNfcLlpcMac_Chk_t) ( phFriNfc_LlcpMac_t               *LlcpMac,
                                              phFriNfc_LlcpMac_Chk_CB_t        ChkLlcpMac_Cb,
                                              void                             *pContext);

typedef NFCSTATUS (*pphFriNfcLlpcMac_Activate_t) (phFriNfc_LlcpMac_t                   *LlcpMac);

typedef NFCSTATUS (*pphFriNfcLlpcMac_Deactivate_t) (phFriNfc_LlcpMac_t                 *LlcpMac);

typedef NFCSTATUS (*pphFriNfcLlpcMac_Send_t) (phFriNfc_LlcpMac_t               *LlcpMac,
                                              phNfc_sData_t                    *psData,
                                              phFriNfc_LlcpMac_Send_CB_t       LlcpMacSend_Cb,
                                              void                             *pContext);

typedef NFCSTATUS (*pphFriNfcLlpcMac_Receive_t) (phFriNfc_LlcpMac_t               *LlcpMac,
                                                 phNfc_sData_t                    *psData,
                                                 phFriNfc_LlcpMac_Reveive_CB_t    LlcpMacReceive_Cb,
                                                 void                             *pContext);

/*========== STRUCTURES ===========*/

/**
 * \ingroup grp_fri_nfc_llcp_mac
 * \brief Generic Interface structure with the Lower Layer
 */
typedef struct phFriNfc_LlcpMac_Interface
{
   pphFriNfcLlpcMac_Chk_t              chk;
   pphFriNfcLlpcMac_Activate_t         activate;
   pphFriNfcLlpcMac_Deactivate_t       deactivate;
   pphFriNfcLlpcMac_Send_t             send;
   pphFriNfcLlpcMac_Receive_t          receive;
} phFriNfc_LlcpMac_Interface_t;

/**
 * \ingroup grp_fri_nfc_llcp_mac
 * \brief Definition of the MAC type
 */
struct phFriNfc_LlcpMac 
{
   phFriNfc_LlcpMac_eLinkStatus_t      LinkState;
   phHal_sRemoteDevInformation_t       *psRemoteDevInfo;
   phFriNfc_LlcpMac_LinkStatus_CB_t    LinkStatus_Cb;
   void                                *LinkStatus_Context;
   phFriNfc_LlcpMac_Interface_t        LlcpMacInterface; 
   phFriNfc_LlcpMac_ePeerType_t        PeerRemoteDevType;
   phFriNfc_LlcpMac_eType_t            MacType;

   /**<\internal Holds the completion routine informations of the Map Layer*/
   phFriNfc_CplRt_t                   MacCompletionInfo;
   void                               *LowerDevice;
   phFriNfc_LlcpMac_Send_CB_t         MacSend_Cb;
   void                               *MacSend_Context;
   phFriNfc_LlcpMac_Reveive_CB_t      MacReceive_Cb;
   void                               *MacReceive_Context;
   phNfc_sData_t                      *psReceiveBuffer;
   phNfc_sData_t                      *psSendBuffer;
   phNfc_sData_t                      sConfigParam;
   uint8_t                            RecvPending;
   uint8_t                            SendPending;
   uint8_t                            RecvStatus;
   phHal_uCmdList_t                   Cmd;
   phHal_sDepAdditionalInfo_t         psDepAdditionalInfo;
} ;


/*
################################################################################
********************** MAC Interface Function Prototype  ***********************
################################################################################
*/

/**
 * \ingroup grp_fri_nfc_llcp_mac
 * \brief
 */
NFCSTATUS phFriNfc_LlcpMac_Reset (phFriNfc_LlcpMac_t                 *LlcpMac,
                                  void                               *LowerDevice,
                                  phFriNfc_LlcpMac_LinkStatus_CB_t   LinkStatus_Cb,
                                  void                               *pContext);
/**
 * \ingroup grp_fri_nfc_llcp_mac
 * \brief
 */
NFCSTATUS phFriNfc_LlcpMac_ChkLlcp (phFriNfc_LlcpMac_t                  *LlcpMac, 
                                    phHal_sRemoteDevInformation_t       *psRemoteDevInfo,
                                    phFriNfc_LlcpMac_Chk_CB_t           ChkLlcpMac_Cb,
                                    void                                *pContext);

/**
 * \ingroup grp_fri_nfc_llcp_mac
 * \brief
 */
NFCSTATUS phFriNfc_LlcpMac_Activate (phFriNfc_LlcpMac_t                   *LlcpMac);

/**
 * \ingroup grp_fri_nfc_llcp_mac
 * \brief
 */
NFCSTATUS phFriNfc_LlcpMac_Deactivate (phFriNfc_LlcpMac_t                 *LlcpMac);

/**
 * \ingroup grp_fri_nfc_llcp_mac
 * \brief
 */
NFCSTATUS phFriNfc_LlcpMac_Send (phFriNfc_LlcpMac_t               *LlcpMac, 
                                 phNfc_sData_t                    *psData,
                                 phFriNfc_LlcpMac_Send_CB_t       LlcpMacSend_Cb,
                                 void                             *pContext);

/**
 * \ingroup grp_fri_nfc_llcp_mac
 * \brief
 */
NFCSTATUS phFriNfc_LlcpMac_Receive (phFriNfc_LlcpMac_t               *LlcpMac,
                                    phNfc_sData_t                    *psData,
                                    phFriNfc_LlcpMac_Reveive_CB_t    ReceiveLlcpMac_Cb,
                                    void                             *pContext);

#endif /* PHFRINFC_LLCPMAC_H */
