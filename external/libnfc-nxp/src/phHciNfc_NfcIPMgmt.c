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
* =========================================================================== *
*                                                                             *
*                                                                             *
* \file  phHciNfc_NfcIPMgmt.c                                                 *
* \brief HCI NFCIP-1 management routines.                                     *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Tue Jun  8 09:32:31 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.33 $                                                           *
* $Aliases: NFC_FRI1.1_WK1023_R35_1 $
*                                                                             *
* =========================================================================== *
*/

/*
***************************** Header File Inclusion ****************************
*/
#include <phNfcCompId.h>
#include <phNfcHalTypes.h>
#include <phHciNfc_Pipe.h>
#include <phHciNfc_RFReader.h>
#include <phHciNfc_Emulation.h>
#include <phOsalNfc.h>

#if defined (ENABLE_P2P)
#include <phHciNfc_NfcIPMgmt.h>
/*
****************************** Macro Definitions *******************************
*/
/* RF Error */
#define NFCIP_RF_NO_ERROR                   0x00U
#define NFCIP_STATUS_MAX_VALUE              0x01U

/* Read and write to the below registry for initiator and target */
#define NXP_NFCIP_MODE                      0x01U
#define NXP_NFCIP_ATR_REQ                   0x02U
#define NXP_NFCIP_ATR_RES                   0x03U
#define NXP_NFCIP_PSL1                      0x04U
#define NXP_NFCIP_PSL2                      0x05U
#define NXP_NFCIP_DID                       0x06U
#define NXP_NFCIP_NAD                       0x07U
#define NXP_NFCIP_OPTIONS                   0x08U
#define NXP_NFCIP_STATUS                    0x09U
#define NXP_NFCIP_NFCID3I                   0x0AU
#define NXP_NFCIP_NFCID3T                   0x0BU
#define NXP_NFCIP_PARAM                     0x0CU
#define NXP_NFCIP_MERGE                     0x0DU

/* command */
#define NXP_NFCIP_ATTREQUEST                0x12U
#define NXP_NFCI_CONTINUE_ACTIVATION        0x13U

/* Event */
#define NXP_EVT_NFC_SND_DATA                0x01U
#define NXP_EVT_NFC_ACTIVATED               0x02U
#define NXP_EVT_NFC_DEACTIVATED             0x03U
#define NXP_EVT_NFC_RCV_DATA                0x04U
#define NXP_EVT_NFC_CONTINUE_MI             0x05U

#define NFCIP_DATE_RATE_FACTOR              0x40U
#define NFCIP_DATE_RATE_SHIFT               0x06U
#define NFCIP_DATA_RATE_CALC(val) \
        ((((uint8_t)(val) >> NFCIP_DATE_RATE_SHIFT) + \
        0x01U) * NFCIP_DATE_RATE_FACTOR)
#define NFCIP_COMM_INITIATOR_SHIFT          0x03
#define NFCIP_COMM_FACTOR                   0x03
/*
*************************** Structure and Enumeration ***************************
*/

/*
*************************** Static Function Declaration **************************
*/
static
NFCSTATUS
phHciNfc_NfcIP_InfoUpdate(
                              phHciNfc_sContext_t     *psHciContext,
                              uint8_t                 index,
                              uint8_t                 *reg_value,
                              uint8_t                 reg_length
                              );

static
NFCSTATUS
phHciNfc_NfcIP_RecvData(
                        phHciNfc_sContext_t  *psHciContext,
                        void                 *pHwRef,
                        uint8_t              *pResponse,
#ifdef ONE_BYTE_LEN
                        uint8_t              length
#else
                        uint16_t             length
#endif
                  );

static
NFCSTATUS
phHciNfc_Recv_NfcIP_Response(
                             phHciNfc_sContext_t    *psHciContext,
                             phHciNfc_Pipe_Info_t   *ppipe_info, 
                             uint8_t                *pResponse,
#ifdef ONE_BYTE_LEN
                             uint8_t                length
#else
                             uint16_t               length
#endif
                             );

static
NFCSTATUS
phHciNfc_Recv_NfcIP_Event(
                        phHciNfc_sContext_t     *psHciContext,
                        void                    *pHwRef,
                        uint8_t                 *pEvent,
#ifdef ONE_BYTE_LEN
                        uint8_t                 length
#else
                        uint16_t                length
#endif
                        );

static
NFCSTATUS
phHciNfc_Recv_Initiator_Event(
                          void                  *psContext,
                          void                  *pHwRef,
                          uint8_t               *pEvent,
#ifdef ONE_BYTE_LEN
                          uint8_t               length
#else
                          uint16_t              length
#endif
                          );

static
NFCSTATUS
phHciNfc_Recv_Target_Event(
                            void                    *psContext,
                            void                    *pHwRef,
                            uint8_t                 *pEvent,
#ifdef ONE_BYTE_LEN
                            uint8_t                 length
#else
                            uint16_t                length
#endif
                            );

static
NFCSTATUS
phHciNfc_Recv_Initiator_Response(
                              void                  *psContext,
                              void                  *pHwRef,
                              uint8_t               *pResponse,
#ifdef ONE_BYTE_LEN
                              uint8_t               length
#else
                              uint16_t              length
#endif
                              );

static
NFCSTATUS
phHciNfc_Recv_Target_Response(
                           void                 *psContext,
                           void                 *pHwRef,
                           uint8_t              *pResponse,
#ifdef ONE_BYTE_LEN
                           uint8_t              length
#else
                           uint16_t             length
#endif
                           );
/*
*************************** Function Definitions ***************************
*/

NFCSTATUS
phHciNfc_Initiator_Init_Resources(
                                  phHciNfc_sContext_t     *psHciContext
                                  )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_NfcIP_Info_t       *p_init_info=NULL;
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if (NULL != psHciContext->p_nfcip_info)
        {
            status = NFCSTATUS_SUCCESS;
        } 
        else if(( NULL == psHciContext->p_nfcip_info ) &&
            (phHciNfc_Allocate_Resource((void **)(&p_init_info),
            sizeof(phHciNfc_NfcIP_Info_t))== NFCSTATUS_SUCCESS)
            )
        {
            psHciContext->p_nfcip_info = p_init_info;
            p_init_info->nfcip_type = NFCIP_INVALID;
            p_init_info->current_seq = NFCIP_INVALID_SEQUENCE;
            p_init_info->next_seq = NFCIP_INVALID_SEQUENCE;
            p_init_info->p_init_pipe_info = NULL;
            p_init_info->p_tgt_pipe_info = NULL;
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INSUFFICIENT_RESOURCES);
        }

    }
    return status;
}

NFCSTATUS
phHciNfc_Initiator_Get_PipeID(
                              phHciNfc_sContext_t     *psHciContext,
                              uint8_t                 *ppipe_id
                              )
{
    NFCSTATUS       status = NFCSTATUS_SUCCESS;
    if( (NULL != psHciContext)
        && ( NULL != ppipe_id )
        && ( NULL != psHciContext->p_nfcip_info ) 
        )
    {
        phHciNfc_NfcIP_Info_t     *p_init_info=NULL;
        p_init_info = (phHciNfc_NfcIP_Info_t *)
                            psHciContext->p_nfcip_info ;
        *ppipe_id =  p_init_info->p_init_pipe_info->pipe.pipe_id  ;
    }
    else 
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    return status;
}

NFCSTATUS
phHciNfc_Initiator_Update_PipeInfo(
                                   phHciNfc_sContext_t     *psHciContext,
                                   uint8_t                 pipeID,
                                   phHciNfc_Pipe_Info_t    *pPipeInfo
                                   )
{
    NFCSTATUS       status = NFCSTATUS_SUCCESS;
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_nfcip_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_NfcIP_Info_t       *p_init_info=NULL;
        p_init_info = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        /* Update the pipe_id of the NFCIP-1 initiator Gate obtained from 
        the HCI Response */
        p_init_info->p_init_pipe_info = pPipeInfo;
        p_init_info->p_init_pipe_info->pipe.pipe_id = pipeID;        
        /* Update the Response Receive routine of the NFCIP-1 initiator Gate */
        pPipeInfo->recv_resp = &phHciNfc_Recv_Initiator_Response;
        /* Update the event Receive routine of the NFCIP-1 initiator Gate */
        pPipeInfo->recv_event = &phHciNfc_Recv_Initiator_Event;
    }
    return status;
}

NFCSTATUS
phHciNfc_NfcIP_Presence_Check(
                                phHciNfc_sContext_t   *psHciContext,
                                void                  *pHwRef
                                )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_nfcip_info) || 
        (NFCIP_INVALID == 
        ((phHciNfc_NfcIP_Info_t *)(psHciContext->p_nfcip_info))->nfcip_type))        
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_NfcIP_Info_t       *p_nfcipinfo=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;

        p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        p_pipe_info = ((NFCIP_INITIATOR == p_nfcipinfo->nfcip_type)? 
                        p_nfcipinfo->p_init_pipe_info : 
                        p_nfcipinfo->p_tgt_pipe_info);

        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }            
        else
        {
            phHciNfc_HCP_Packet_t       *hcp_packet = NULL;
            uint16_t                    length = HCP_HEADER_LEN;
            uint8_t                     pipeid = 0;

            pipeid = p_pipe_info->pipe.pipe_id;
            psHciContext->tx_total = 0 ;
            hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;
            /* Construct the HCP Frame */
            phHciNfc_Build_HCPFrame(hcp_packet,(uint8_t)HCP_CHAINBIT_DEFAULT,
                            (uint8_t) pipeid, (uint8_t)HCP_MSG_TYPE_COMMAND, 
                            (uint8_t)NXP_NFCIP_ATTREQUEST);

            p_pipe_info->sent_msg_type = (uint8_t)HCP_MSG_TYPE_COMMAND;
            p_pipe_info->prev_msg = (uint8_t)NXP_NFCIP_ATTREQUEST;
            psHciContext->tx_total = length;
            psHciContext->response_pending = (uint8_t)TRUE;

            /* Send the Constructed HCP packet to the lower layer */
            status = phHciNfc_Send_HCP( psHciContext, pHwRef);
            p_pipe_info->prev_status = status;
        }
    }
    return status;
}

static
NFCSTATUS
phHciNfc_Recv_Initiator_Response(
                                 void                *pContext,
                                 void                *pHwRef,
                                 uint8_t             *pResponse,
#ifdef ONE_BYTE_LEN
                                 uint8_t             length
#else
                                 uint16_t            length
#endif
                              )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t         *psHciContext = 
                                (phHciNfc_sContext_t *)pContext ;

    if( (NULL == psHciContext) || (NULL == pHwRef) || (NULL == pResponse)
        || (0 == length))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_nfcip_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_NfcIP_Info_t   *p_nfcip_info=NULL;
        phHciNfc_Pipe_Info_t    *p_pipe_info = NULL;        

        p_nfcip_info = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        p_pipe_info = p_nfcip_info->p_init_pipe_info;
        if( NULL == p_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            status = phHciNfc_Recv_NfcIP_Response(psHciContext,  
                                                p_pipe_info, pResponse, 
                                                length);
            if (NFCSTATUS_SUCCESS == status)
            {
                status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                            UPDATE_SEQ);
            }
        }
    }
    return status;
}

static
NFCSTATUS
phHciNfc_Recv_Initiator_Event(
                              void               *psContext,
                              void               *pHwRef,
                              uint8_t            *pEvent,
#ifdef ONE_BYTE_LEN
                              uint8_t             length
#else
                              uint16_t            length
#endif
                              )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t         *psHciContext = 
                                (phHciNfc_sContext_t *)psContext ;
    if( (NULL == psHciContext) || (NULL == pHwRef) || (NULL == pEvent)
        || (0 == length))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_nfcip_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_HCP_Packet_t       *p_packet = NULL;
        phHciNfc_NfcIP_Info_t       *p_nfcip_info=NULL;
        phHciNfc_HCP_Message_t      *message = NULL;
        uint8_t                     instruction=0;

        p_nfcip_info = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        p_packet = (phHciNfc_HCP_Packet_t *)pEvent;
        message = &p_packet->msg.message;
        /* Get the instruction bits from the Message Header */
        instruction = (uint8_t) GET_BITS8( message->msg_header,
                    HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);
        if (NXP_EVT_NFC_ACTIVATED == instruction)
        {
            p_nfcip_info->nfcip_type = NFCIP_INITIATOR;
            psHciContext->host_rf_type = phHal_eNfcIP1_Initiator;
            p_nfcip_info->rem_nfcip_tgt_info.RemDevType = phHal_eNfcIP1_Target;
        }

        status = phHciNfc_Recv_NfcIP_Event(psHciContext,
                                        pHwRef, pEvent, length);        
    }
    return status;
}

NFCSTATUS
phHciNfc_Target_Init_Resources(
                               phHciNfc_sContext_t     *psHciContext
                               )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_NfcIP_Info_t      *p_target_info=NULL;
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if (NULL != psHciContext->p_nfcip_info)
        {
            status = NFCSTATUS_SUCCESS;
        } 
        else if(
            ( NULL == psHciContext->p_nfcip_info ) &&
            (phHciNfc_Allocate_Resource((void **)(&p_target_info),
            sizeof(phHciNfc_NfcIP_Info_t))== NFCSTATUS_SUCCESS)
            )
        {
            psHciContext->p_nfcip_info = p_target_info;
            p_target_info->nfcip_type = NFCIP_INVALID;
            p_target_info->current_seq = NFCIP_INVALID_SEQUENCE;
            p_target_info->next_seq = NFCIP_INVALID_SEQUENCE;
            p_target_info->p_tgt_pipe_info = NULL;
            p_target_info->p_tgt_pipe_info = NULL;
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INSUFFICIENT_RESOURCES);
        }

    }
    return status;
}

NFCSTATUS
phHciNfc_Target_Get_PipeID(
                           phHciNfc_sContext_t     *psHciContext,
                           uint8_t                 *ppipe_id
                           )
{
    NFCSTATUS       status = NFCSTATUS_SUCCESS;
    if( (NULL != psHciContext)
        && ( NULL != ppipe_id )
        && ( NULL != psHciContext->p_nfcip_info ) 
        )
    {
        phHciNfc_NfcIP_Info_t     *p_target_info=NULL;
        p_target_info = (phHciNfc_NfcIP_Info_t *)
                            psHciContext->p_nfcip_info ;
        *ppipe_id =  p_target_info->p_tgt_pipe_info->pipe.pipe_id;
    }
    else 
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    return status;
}

NFCSTATUS
phHciNfc_Target_Update_PipeInfo(
                                phHciNfc_sContext_t     *psHciContext,
                                uint8_t                 pipeID,
                                phHciNfc_Pipe_Info_t    *pPipeInfo
                                )
{
    NFCSTATUS       status = NFCSTATUS_SUCCESS;
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_nfcip_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_NfcIP_Info_t       *p_target_info=NULL;
        p_target_info = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        /* Update the pipe_id of the NFCIP-1 target Gate obtained from 
        the HCI Response */
        p_target_info->p_tgt_pipe_info = pPipeInfo;
        p_target_info->p_tgt_pipe_info->pipe.pipe_id = pipeID;
        /* Update the Response Receive routine of the NFCIP-1 target Gate */
        pPipeInfo->recv_resp = &phHciNfc_Recv_Target_Response;
        /* Update the event Receive routine of the NFCIP-1 target Gate */
        pPipeInfo->recv_event = &phHciNfc_Recv_Target_Event;
    }
    return status;
}

static
NFCSTATUS
phHciNfc_Recv_Target_Response(
                             void                *pContext,
                             void                *pHwRef,
                             uint8_t             *pResponse,
#ifdef ONE_BYTE_LEN
                             uint8_t             length
#else
                             uint16_t            length
#endif
                             )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t         *psHciContext = 
                                (phHciNfc_sContext_t *)pContext ;

    if( (NULL == psHciContext) || (NULL == pHwRef) || (NULL == pResponse)
        || (0 == length))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_nfcip_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_NfcIP_Info_t   *p_nfcip_info=NULL;
        phHciNfc_Pipe_Info_t    *p_pipe_info = NULL;        

        p_nfcip_info = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        p_pipe_info = p_nfcip_info->p_tgt_pipe_info;
        if( NULL == p_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            status = phHciNfc_Recv_NfcIP_Response(psHciContext,  
                                                p_pipe_info, pResponse, 
                                                length);
            if (NFCSTATUS_SUCCESS == status)
            {
                status = phHciNfc_EmuMgmt_Update_Seq(psHciContext, 
                                                    UPDATE_SEQ);
            }
        }
    }
    return status;
}

static
NFCSTATUS
phHciNfc_Recv_Target_Event(
                           void                     *psContext,
                           void                     *pHwRef,
                           uint8_t                  *pEvent,
#ifdef ONE_BYTE_LEN
                           uint8_t                  length
#else
                           uint16_t                 length
#endif
                           )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t         *psHciContext = 
                                (phHciNfc_sContext_t *)psContext ;
    if( (NULL == psHciContext) || (NULL == pHwRef) || (NULL == pEvent)
        || (0 == length))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_nfcip_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_HCP_Packet_t       *p_packet = NULL;
        phHciNfc_NfcIP_Info_t       *p_nfcip_info=NULL;
        phHciNfc_HCP_Message_t      *message = NULL;
        uint8_t                     instruction=0;

        p_nfcip_info = (phHciNfc_NfcIP_Info_t *)psHciContext->p_nfcip_info ;
        p_packet = (phHciNfc_HCP_Packet_t *)pEvent;
        message = &p_packet->msg.message;
        /* Get the instruction bits from the Message Header */
        instruction = (uint8_t) GET_BITS8( message->msg_header,
                    HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);
        if (NXP_EVT_NFC_ACTIVATED == instruction)
        {
            p_nfcip_info->nfcip_type = NFCIP_TARGET;
            psHciContext->host_rf_type = phHal_eNfcIP1_Target;
            p_nfcip_info->rem_nfcip_tgt_info.RemDevType = 
                                            phHal_eNfcIP1_Initiator;
        }
        status = phHciNfc_Recv_NfcIP_Event(psHciContext,
                                        pHwRef, pEvent, length);        
    }
    return status;
}

static
NFCSTATUS
phHciNfc_Recv_NfcIP_Response(
                             phHciNfc_sContext_t    *psHciContext,
                             phHciNfc_Pipe_Info_t   *ppipe_info, 
                             uint8_t                *pResponse,
#ifdef ONE_BYTE_LEN
                             uint8_t                length
#else
                             uint16_t               length
#endif
                             )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_NfcIP_Info_t       *p_nfcipinfo=NULL;    
    uint8_t                     prev_cmd = ANY_GET_PARAMETER;

    p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                    psHciContext->p_nfcip_info ;    
    prev_cmd = ppipe_info->prev_msg ;
    switch(prev_cmd)
    {
        case ANY_OPEN_PIPE:
        {
            HCI_PRINT("NFCIP-1 NFCIP open pipe complete\n");
            p_nfcipinfo->next_seq = NFCIP_NFCID3I;
            break;
        }
        case ANY_CLOSE_PIPE:
        {
            HCI_PRINT("NFCIP-1 NFCIP close pipe complete\n");
            p_nfcipinfo->next_seq = NFCIP_NFCID3I;
            break;
        }
        case ANY_GET_PARAMETER:
        {
            HCI_PRINT("NFCIP-1 NFCIP get parameter complete\n");
            if (length >= HCP_HEADER_LEN)
            {
                status = phHciNfc_NfcIP_InfoUpdate(psHciContext,
                                    ppipe_info->reg_index, 
                                    &pResponse[HCP_HEADER_LEN],
                                    (uint8_t)(length - HCP_HEADER_LEN));
            }
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }            
            break;
        }
        case ANY_SET_PARAMETER:
        {
            HCI_PRINT("NFCIP-1 NFCIP Parameter Set \n");            
            p_nfcipinfo->next_seq = NFCIP_NFCID3I;
            break;
        }
        case NXP_NFCI_CONTINUE_ACTIVATION:
        case NXP_NFCIP_ATTREQUEST:
        {
            p_nfcipinfo->next_seq = NFCIP_NFCID3I;            
            break;
        }
        default:
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_RESPONSE);
            break;
        }
    }
    
    if( NFCSTATUS_SUCCESS == status )
    {
        ppipe_info->prev_status = NFCSTATUS_SUCCESS;
        p_nfcipinfo->current_seq = p_nfcipinfo->next_seq;
    }
    return status;
}

static
NFCSTATUS
phHciNfc_Recv_NfcIP_Event(
                          phHciNfc_sContext_t       *psHciContext,
                          void                      *pHwRef,
                          uint8_t                   *pEvent,
#ifdef ONE_BYTE_LEN
                          uint8_t                   length
#else
                          uint16_t                  length
#endif
                          )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_HCP_Packet_t       *p_packet = NULL;
    phHciNfc_NfcIP_Info_t       *p_nfcipinfo=NULL;
    phHciNfc_HCP_Message_t      *message = NULL;
    phNfc_sCompletionInfo_t     pCompInfo;
    uint8_t                     instruction=0;
    uint8_t                     type = 0;

    p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                    psHciContext->p_nfcip_info ;
    p_packet = (phHciNfc_HCP_Packet_t *)pEvent;
    message = &p_packet->msg.message;
    /* Get the instruction bits from the Message Header */
    instruction = (uint8_t) GET_BITS8( message->msg_header,
                    HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);

    switch(instruction)
    {
        case NXP_EVT_NFC_ACTIVATED:
        {
            HCI_PRINT("NFCIP-1 device discovered\n");
            
            if (NFCIP_INITIATOR == p_nfcipinfo->nfcip_type)
            {
                pCompInfo.info = &(p_nfcipinfo->rem_nfcip_tgt_info);
                type = NFC_NOTIFY_TARGET_DISCOVERED;
            }
            else
            {
                type = NFC_NOTIFY_DEVICE_ACTIVATED;
            }

            if(length > HCP_HEADER_LEN)
            {
                HCI_DEBUG("NfcIP-1 activation mode : %d\n", pEvent[HCP_HEADER_LEN]);
                /* Mode indicates in which mode the current activation 
                    as be done
                        - 0x00: Passive mode
                        - 0x01: Active */
                p_nfcipinfo->activation_mode = pEvent[HCP_HEADER_LEN];
            }
            pCompInfo.status = NFCSTATUS_SUCCESS;
            /* Notify to the HCI Generic layer To Update the FSM */
            phHciNfc_Notify_Event(psHciContext, pHwRef, 
                                type, &pCompInfo);            
            break;
        }
        case NXP_EVT_NFC_DEACTIVATED:
        {
            static phHal_sEventInfo_t   event_info;

            event_info.eventHost = phHal_eHostController;
            event_info.eventType = NFC_EVT_DEACTIVATED;
            p_nfcipinfo->activation_mode = FALSE;
            if (NFCIP_INITIATOR == p_nfcipinfo->nfcip_type)
            {
                p_nfcipinfo->rem_nfcip_tgt_info.RemDevType = 
                                        phHal_eNfcIP1_Target;
                event_info.eventSource = phHal_eNfcIP1_Initiator;
            }
            else
            {
                p_nfcipinfo->rem_nfcip_tgt_info.RemDevType = 
                                        phHal_eNfcIP1_Initiator;
                event_info.eventSource = phHal_eNfcIP1_Target;
            }
            /* Reset the sequence */
            p_nfcipinfo->current_seq = NFCIP_NFCID3I;
            p_nfcipinfo->next_seq = NFCIP_NFCID3I;

            HCI_PRINT("NFCIP-1 Target Deactivated\n");
            phHciNfc_Notify_Event(psHciContext, pHwRef, 
                                NFC_NOTIFY_DEVICE_DEACTIVATED, 
                                &event_info);
            break;
        }
        case NXP_EVT_NFC_RCV_DATA:
        {
            status = phHciNfc_NfcIP_RecvData(psHciContext, 
                                pHwRef,
                                &pEvent[HCP_HEADER_LEN],
                                (length - HCP_HEADER_LEN));
            break;
        }
        case NXP_EVT_NFC_CONTINUE_MI:
        {
            /* psHciContext->response_pending = FALSE; */
            psHciContext->event_pending = FALSE;
            break;
        }
        default:
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
            break;
        }
    }

    return status;
}

static 
NFCSTATUS
phHciNfc_NfcIP_RecvData(
                  phHciNfc_sContext_t  *psHciContext,
                  void                 *pHwRef,
                  uint8_t              *pResponse,
#ifdef ONE_BYTE_LEN
                  uint8_t              length
#else
                  uint16_t             length
#endif
                  )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    uint8_t                     index = 0;

    if( (NULL == psHciContext) 
        || (NULL == pHwRef)
        || (NULL == pResponse)
        || (0 == length))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else 
    {
        phNfc_sTransactionInfo_t    transInfo;
        phHciNfc_NfcIP_Info_t       *p_nfcipinfo = NULL;
        uint8_t                     type = 0;

        p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info;
        HCI_PRINT("NFCIP-1 received bytes :");        
        if (NFCIP_RF_NO_ERROR == pResponse[index])
        {
            HCI_PRINT_BUFFER("device ", &pResponse[index], (length - index));
            transInfo.status = NFCSTATUS_SUCCESS;
            index++;
            if (TRUE == pResponse[index])
            {                
                /* Update the more information bit to the upper layer */
                transInfo.status = NFCSTATUS_MORE_INFORMATION;
            }
            index++;

            
            transInfo.buffer = &pResponse[index];
            transInfo.length = (length - index);            
            type = (uint8_t)NFC_NOTIFY_RECV_EVENT;
        }
        else
        {
            HCI_PRINT("NFCIP-1 receive RF ERROR ");
            p_nfcipinfo->activation_mode = FALSE;
            type = (uint8_t)NFC_NOTIFY_RECV_EVENT;
            transInfo.status = NFCSTATUS_RF_TIMEOUT;
            transInfo.buffer = NULL;
            transInfo.length = 0;
        }
        status = NFCSTATUS_PENDING;
        /* Event NXP_EVT_NFC_RCV_DATA: so give received data to 
           the upper layer */
        phHciNfc_Notify_Event(psHciContext, pHwRef,
                                type, 
                                &transInfo );
    }
    return status;
}

NFCSTATUS
phHciNfc_NfcIP_Send_Data (
                         phHciNfc_sContext_t    *psHciContext,
                         void                   *pHwRef, 
                         phHciNfc_XchgInfo_t    *sData
                         )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) || (NULL == pHwRef) || (NULL == sData) || 
        (NULL == sData->tx_buffer) || (0 == sData->tx_length))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_nfcip_info) || 
        (NFCIP_INVALID == 
        ((phHciNfc_NfcIP_Info_t *)(psHciContext->p_nfcip_info))->nfcip_type))        
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_NfcIP_Info_t       *p_nfcipinfo=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;

        p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        p_pipe_info = ((NFCIP_INITIATOR == p_nfcipinfo->nfcip_type)? 
                        p_nfcipinfo->p_init_pipe_info : 
                        p_nfcipinfo->p_tgt_pipe_info);

        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }          
        else
        {
            phHciNfc_HCP_Packet_t       *hcp_packet = NULL;
            phHciNfc_HCP_Message_t      *hcp_message = NULL;
            uint16_t                    length = HCP_HEADER_LEN;
            uint8_t                     pipeid = 0, 
                                        i = 0;

            HCI_PRINT_BUFFER("HCI NFCIP-1 Send Data: ", sData->tx_buffer, sData->tx_length);

            psHciContext->tx_total = 0 ;
            pipeid = p_pipe_info->pipe.pipe_id;
            hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;
            hcp_message = &(hcp_packet->msg.message);
            hcp_message->payload[i] = sData->params.nfc_info.more_info;
            i++;
            
            /* Construct the HCP Frame */
            phHciNfc_Build_HCPFrame(hcp_packet,(uint8_t)HCP_CHAINBIT_DEFAULT,
                            (uint8_t) pipeid, (uint8_t)HCP_MSG_TYPE_EVENT, 
                            (uint8_t)NXP_EVT_NFC_SND_DATA);

            phHciNfc_Append_HCPFrame((uint8_t *)hcp_message->payload,
                            i, (uint8_t *)sData->tx_buffer,
                            (uint8_t)sData->tx_length);

            length =(uint16_t)(length + i + sData->tx_length);

            p_pipe_info->sent_msg_type = (uint8_t)HCP_MSG_TYPE_EVENT;
            p_pipe_info->prev_msg = NXP_EVT_NFC_SND_DATA;
            psHciContext->tx_total = length;
            /* Send the Constructed HCP packet to the lower layer */
            status = phHciNfc_Send_HCP( psHciContext, pHwRef);
#if !defined (ENABLE_CONTINUE_MI)
            if ((TRUE == sData->params.nfc_info.more_info) && 
                (NFCSTATUS_PENDING == status))
            {
                /* If more information bit is set, then wait for the event 
                    NXP_EVT_NFC_CONTINUE_MI */
                /* psHciContext->response_pending = TRUE; */
                psHciContext->event_pending = TRUE;
            }
#endif /* #if defined (ENABLE_CONTINUE_MI) */
            p_pipe_info->prev_status = status;
        }
    }
    return status;
}

NFCSTATUS
phHciNfc_NfcIP_Info_Sequence (
                                  phHciNfc_sContext_t   *psHciContext,
                                  void                  *pHwRef
#ifdef NOTIFY_REQD
                                  , 
                                  uint8_t               notify_reqd
#endif /* #ifdef NOTIFY_REQD */
                                  )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    
    if( (NULL == psHciContext) 
        || (NULL == pHwRef)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_nfcip_info) || 
        (NFCIP_INVALID == 
        ((phHciNfc_NfcIP_Info_t *)(psHciContext->p_nfcip_info))->
        nfcip_type))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }   
    else
    {
        phHciNfc_NfcIP_Info_t       *p_nfcipinfo=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;

        p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        p_pipe_info = ((NFCIP_INITIATOR == p_nfcipinfo->nfcip_type)? 
                        p_nfcipinfo->p_init_pipe_info: 
                        p_nfcipinfo->p_tgt_pipe_info);
        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            switch(p_nfcipinfo->current_seq)
            {
                case NFCIP_NFCID3I:
                {
                    p_pipe_info->reg_index = (uint8_t)NXP_NFCIP_NFCID3I;
                    /* Fill the data buffer and send the command to the 
                    device */
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                            p_pipe_info->pipe.pipe_id, 
                                            ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_nfcipinfo->next_seq = NFCIP_NFCID3T;
                    }
                    break;
                }
                case NFCIP_NFCID3T:
                {
                    p_pipe_info->reg_index = (uint8_t)NXP_NFCIP_NFCID3T;
                    /* Fill the data buffer and send the command to the 
                    device */
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                                p_pipe_info->pipe.pipe_id, 
                                                ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_nfcipinfo->next_seq = NFCIP_PARAM;
                    }
                    break;
                }
                case NFCIP_PARAM:
                {
                    p_pipe_info->reg_index = (uint8_t)NXP_NFCIP_PARAM;
                    /* Fill the data buffer and send the command to the 
                    device */
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                                p_pipe_info->pipe.pipe_id, 
                                                ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_nfcipinfo->next_seq = NFCIP_ATR_INFO;
                    }
                    break;
                }
                case NFCIP_ATR_INFO:
                {
                    p_pipe_info->reg_index = (uint8_t)((NFCIP_INITIATOR == 
                            p_nfcipinfo->nfcip_type)?
                            NXP_NFCIP_ATR_RES : 
                            NXP_NFCIP_ATR_REQ);
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                                p_pipe_info->pipe.pipe_id, 
                                                ANY_GET_PARAMETER);

                    if(NFCSTATUS_PENDING == status )
                    {
                        p_nfcipinfo->next_seq = NFCIP_STATUS;
                    }
                    break;
                }
                case NFCIP_STATUS:
                {
                    p_pipe_info->reg_index = (uint8_t)NXP_NFCIP_STATUS;
                    /* Fill the data buffer and send the command to the 
                    device */
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                                p_pipe_info->pipe.pipe_id, 
                                                ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
#ifdef NOTIFY_REQD
                        if(FALSE == notify_reqd)
#else /* #ifdef NOTIFY_REQD */
                        if (NULL != psHciContext->p_target_info)
#endif /* #ifdef NOTIFY_REQD */
                        {
                            p_nfcipinfo->next_seq = NFCIP_NFCID3I;
                            status = NFCSTATUS_SUCCESS;
                        }
                        else
                        {
                            p_nfcipinfo->next_seq = NFCIP_END_SEQUENCE;
                        }
                    }
                    break;
                }
                case NFCIP_END_SEQUENCE:
                {                    
                    phHal_uRemoteDevInfo_t          *rem_nfcipinfo = NULL;

                    if (NULL != psHciContext->p_target_info)
                    {
                        /* This is given to user */
                        rem_nfcipinfo = 
                                &(psHciContext->p_target_info->RemoteDevInfo);                        
                    }
                    else
                    {
                        rem_nfcipinfo = 
                                &(p_nfcipinfo->rem_nfcip_tgt_info.RemoteDevInfo);
                    }

                    /* Update maximum frame length */
                    rem_nfcipinfo->NfcIP_Info.MaxFrameLength = 
                                        p_nfcipinfo->max_frame_len;

                    p_nfcipinfo->current_seq = NFCIP_NFCID3I;
                    p_nfcipinfo->next_seq = NFCIP_NFCID3I;

                    rem_nfcipinfo->NfcIP_Info.Nfcip_Active = 
                                            p_nfcipinfo->activation_mode;
                    
                    if (NFCIP_INITIATOR == p_nfcipinfo->nfcip_type)
                    {
                        phNfc_sCompletionInfo_t         CompInfo;

                        p_nfcipinfo->rem_nfcip_tgt_info.RemDevType = 
                                                    phHal_eNfcIP1_Target;

                        /* Update initiator speed */
                        rem_nfcipinfo->NfcIP_Info.Nfcip_Datarate = 
                                            (phHalNfc_eDataRate_t)
                                            (p_nfcipinfo->initiator_speed);

                        
                        /* Update ATR info */
                        rem_nfcipinfo->NfcIP_Info.ATRInfo_Length = 
                                                    p_nfcipinfo->atr_res_length;
                        (void)memcpy(
                                (void *)rem_nfcipinfo->NfcIP_Info.ATRInfo, 
                                (void *)p_nfcipinfo->atr_res_info, 
                                rem_nfcipinfo->NfcIP_Info.ATRInfo_Length);

                        /* Update NFCID */
                        rem_nfcipinfo->NfcIP_Info.NFCID_Length = 
                                        p_nfcipinfo->nfcid3i_length;
                        (void)memcpy(
                                (void *)rem_nfcipinfo->NfcIP_Info.NFCID, 
                                (void *)p_nfcipinfo->nfcid3i, 
                                rem_nfcipinfo->NfcIP_Info.NFCID_Length);

                        CompInfo.status = status = NFCSTATUS_SUCCESS;
                        if (NULL != psHciContext->p_target_info)
                        {
                            CompInfo.info = &(psHciContext->p_target_info);
                        }
                        else
                        {
                            CompInfo.info = &(p_nfcipinfo->rem_nfcip_tgt_info);
                        }
                        /* Notify to the upper layer */
                        phHciNfc_Tag_Notify(psHciContext, pHwRef, 
                                            NFC_NOTIFY_TARGET_DISCOVERED, 
                                            &CompInfo);
                    } 
                    else
                    {
                        static phHal_sEventInfo_t   event_info;                        
                        
                        p_nfcipinfo->rem_nfcip_tgt_info.RemDevType = 
                                                    phHal_eNfcIP1_Initiator;

                        /* Update target speed  */
                        rem_nfcipinfo->NfcIP_Info.Nfcip_Datarate = 
                                            (phHalNfc_eDataRate_t)
                                            (p_nfcipinfo->target_speed);
                        /* Update ATR info */
                        rem_nfcipinfo->NfcIP_Info.ATRInfo_Length = 
                                                    p_nfcipinfo->atr_req_length;
                        (void)memcpy(
                                (void *)rem_nfcipinfo->NfcIP_Info.ATRInfo, 
                                (void *)p_nfcipinfo->atr_req_info, 
                                rem_nfcipinfo->NfcIP_Info.ATRInfo_Length);

                        /* Update NFCID */
                        rem_nfcipinfo->NfcIP_Info.NFCID_Length = 
                                        p_nfcipinfo->nfcid3t_length;
                        (void)memcpy(
                                (void *)rem_nfcipinfo->NfcIP_Info.NFCID, 
                                (void *)p_nfcipinfo->nfcid3t, 
                                rem_nfcipinfo->NfcIP_Info.NFCID_Length);

                        event_info.eventHost = phHal_eHostController;
                        event_info.eventType = NFC_EVT_ACTIVATED;
                        event_info.eventSource = phHal_eNfcIP1_Target;
                        event_info.eventInfo.pRemoteDevInfo = 
                                            &(p_nfcipinfo->rem_nfcip_tgt_info);

                        phHciNfc_Target_Select_Notify((void *)psHciContext, 
                                                    pHwRef, 
                                                    NFC_NOTIFY_EVENT, 
                                                    &(event_info));
                    }
                    break;
                }
                default:
                {
                    status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
                    break;
                }
            }           
        }
    }
    return status;
}

static
NFCSTATUS
phHciNfc_NfcIP_InfoUpdate(
                          phHciNfc_sContext_t     *psHciContext,
                          uint8_t                 index,
                          uint8_t                 *reg_value,
                          uint8_t                 reg_length
                          )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_NfcIP_Info_t       *p_nfcipinfo = NULL;                                
    phHal_sNfcIPInfo_t          *p_rem_nfcipinfo = NULL;   

    p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)(psHciContext->p_nfcip_info );
    p_rem_nfcipinfo = &(p_nfcipinfo->rem_nfcip_tgt_info.RemoteDevInfo.NfcIP_Info);  


    switch(index)
    {
        case NXP_NFCIP_ATR_RES:
        {
            if (reg_length <= NFCIP_ATR_MAX_LENGTH)
            {                
                /* Remote device info provided by the user */
                
                HCI_PRINT_BUFFER("\tNFCIP ATR_RES", reg_value, reg_length);

                p_rem_nfcipinfo->ATRInfo_Length = 
                p_nfcipinfo->atr_res_length = reg_length;

                (void)memcpy((void *)p_rem_nfcipinfo->ATRInfo, 
                                    (void *)reg_value, 
                                    p_rem_nfcipinfo->ATRInfo_Length);

                (void)memcpy((void *)p_nfcipinfo->atr_res_info, 
                                    (void *)reg_value, 
                                    p_nfcipinfo->atr_res_length);
                if (NULL != psHciContext->p_target_info)
                {
                    phHal_sNfcIPInfo_t       *p_remtgt_info = NULL;
                    /* This is given to user */
                    p_remtgt_info = 
                    &(psHciContext->p_target_info->RemoteDevInfo.NfcIP_Info);
                    p_remtgt_info->ATRInfo_Length = reg_length;
                    (void)memcpy((void *)p_remtgt_info->ATRInfo, 
                                        (void *)reg_value, 
                                        p_remtgt_info->ATRInfo_Length);
                }
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case NXP_NFCIP_STATUS:
        {
            if (sizeof(*reg_value) == reg_length) 
#ifdef STATUS_BUFFER_CHECK
                && (*reg_value <= NFCIP_STATUS_MAX_VALUE))
#endif /* #ifdef STATUS_ERROR */
            {
                HCI_PRINT_BUFFER("\tNFCIP STATUS", reg_value, reg_length);
                p_nfcipinfo->linkstatus = *reg_value;
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case NXP_NFCIP_NFCID3I:
        {
            if (reg_length <= NFCIP_NFCID_LENGTH)
            {
                HCI_PRINT_BUFFER("\tNFCIP NFCID3I", reg_value, reg_length);
                p_nfcipinfo->nfcid3i_length = 
                p_rem_nfcipinfo->NFCID_Length = reg_length;
                (void)memcpy((void *)p_rem_nfcipinfo->NFCID, 
                                    (void *)reg_value, 
                                    p_rem_nfcipinfo->NFCID_Length);
                (void)memcpy((void *)p_nfcipinfo->nfcid3i, 
                                    (void *)reg_value, 
                                    reg_length);
                if ((NULL != psHciContext->p_target_info) && 
                    (NFCIP_INITIATOR == p_nfcipinfo->nfcip_type))
                {
                    phHal_sNfcIPInfo_t       *p_remtgt_info = NULL;
                    /* This is given to user */
                    p_remtgt_info = 
                    &(psHciContext->p_target_info->RemoteDevInfo.NfcIP_Info);
                    p_remtgt_info->NFCID_Length = reg_length;
                    (void)memcpy((void *)p_remtgt_info->NFCID, 
                                        (void *)reg_value, 
                                        p_remtgt_info->NFCID_Length);
                }
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case NXP_NFCIP_NFCID3T:
        {
            if (reg_length <= NFCIP_NFCID_LENGTH)
            {
                HCI_PRINT_BUFFER("\tNFCIP NFCID3T", reg_value, reg_length);
                p_nfcipinfo->nfcid3t_length = 
                p_rem_nfcipinfo->NFCID_Length = reg_length;
                (void)memcpy((void *)p_rem_nfcipinfo->NFCID, 
                                    (void *)reg_value, 
                                    p_rem_nfcipinfo->NFCID_Length);
                (void)memcpy((void *)p_nfcipinfo->nfcid3t, 
                                    (void *)reg_value, 
                                    reg_length);
                if ((NULL != psHciContext->p_target_info) && 
                    (NFCIP_TARGET == p_nfcipinfo->nfcip_type))
                {
                    phHal_sNfcIPInfo_t       *p_remtgt_info = NULL;
                    /* This is given to user */
                    p_remtgt_info = 
                    &(psHciContext->p_target_info->RemoteDevInfo.NfcIP_Info);
                    p_remtgt_info->NFCID_Length = reg_length;
                    (void)memcpy((void *)p_remtgt_info->NFCID, 
                                        (void *)reg_value, 
                                        p_remtgt_info->NFCID_Length);
                }
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case NXP_NFCIP_PARAM:
        {
            if (sizeof(*reg_value) == reg_length)
            {
                HCI_PRINT_BUFFER("\tNFCIP PARAMS", reg_value, reg_length);
                p_nfcipinfo->initiator_speed = (phHciNfc_eP2PSpeed_t)
                                ((*reg_value >> NFCIP_COMM_INITIATOR_SHIFT)
                                & NFCIP_COMM_FACTOR);
                if (p_nfcipinfo->nfcip_type == NFCIP_INITIATOR) {
                    switch(p_nfcipinfo->initiator_speed) {
                        case phNfc_eDataRate_106:
                            ALOGI("I'm P2P %s Initiator @ 106 kb/s", p_nfcipinfo->activation_mode ? "Active" : "Passive");
                            break;
                        case phNfc_eDataRate_212:
                            ALOGI("I'm P2P %s Initiator @ 212 kb/s", p_nfcipinfo->activation_mode ? "Active" : "Passive");
                            break;
                        case phNfc_eDataRate_424:
                            ALOGI("I'm P2P %s Initiator @ 424 kb/s", p_nfcipinfo->activation_mode ? "Active" : "Passive");
                            break;
                    }
                }
                p_nfcipinfo->target_speed = (phHciNfc_eP2PSpeed_t)
                                (*reg_value & NFCIP_COMM_FACTOR);
                if (p_nfcipinfo->nfcip_type == NFCIP_TARGET) {
                    switch(p_nfcipinfo->target_speed) {
                        case phNfc_eDataRate_106:
                            ALOGI("I'm P2P %s Target @ 106 kb/s", p_nfcipinfo->activation_mode ? "Active" : "Passive");
                            break;
                        case phNfc_eDataRate_212:
                            ALOGI("I'm P2P %s Target @ 212 kb/s", p_nfcipinfo->activation_mode ? "Active" : "Passive");
                            break;
                        case phNfc_eDataRate_424:
                            ALOGI("I'm P2P %s Target @ 424 kb/s", p_nfcipinfo->activation_mode ? "Active" : "Passive");
                            break;
                    }
                }
                p_nfcipinfo->max_frame_len = NFCIP_DATA_RATE_CALC(*reg_value);

                if (p_nfcipinfo->max_frame_len > NFCIP_MAX_DEP_REQ_HDR_LEN)
                {
                    p_nfcipinfo->max_frame_len -= NFCIP_MAX_DEP_REQ_HDR_LEN;

                    if (NULL != psHciContext->p_target_info)
                    {
                        phHal_sNfcIPInfo_t       *p_remtgt_info = NULL;
                        /* This is given to user */
                        p_remtgt_info =
                        &(psHciContext->p_target_info->RemoteDevInfo.NfcIP_Info);
                        p_remtgt_info->MaxFrameLength = p_nfcipinfo->max_frame_len;
                        p_remtgt_info->Nfcip_Datarate = (phHalNfc_eDataRate_t)
                                                p_nfcipinfo->initiator_speed;
                    }
                }
                else
                {
                    status = PHNFCSTVAL(CID_NFC_HCI,
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
                }
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case NXP_NFCIP_MODE:
        {
            if (sizeof(*reg_value) == reg_length)
            {
                HCI_PRINT_BUFFER("\tNFCIP MODE", reg_value, reg_length);
                p_nfcipinfo->nfcip_mode = *reg_value;
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case NXP_NFCIP_ATR_REQ:
        {
            if (reg_length <= NFCIP_ATR_MAX_LENGTH)
            {
                HCI_PRINT_BUFFER("\tNFCIP ATR_REQ", reg_value, reg_length);
                p_rem_nfcipinfo->ATRInfo_Length = 
                        p_nfcipinfo->atr_req_length = reg_length;
                (void)memcpy((void *)p_rem_nfcipinfo->ATRInfo, 
                                    (void *)reg_value, 
                                    p_rem_nfcipinfo->ATRInfo_Length);
                (void)memcpy((void *)p_nfcipinfo->atr_req_info, 
                                    (void *)reg_value, 
                                    p_nfcipinfo->atr_req_length);
                if (NULL != psHciContext->p_target_info)
                {
                    phHal_sNfcIPInfo_t       *p_remtgt_info = NULL;
                    /* This is given to user */
                    p_remtgt_info = 
                    &(psHciContext->p_target_info->RemoteDevInfo.NfcIP_Info);
                    p_remtgt_info->NFCID_Length = reg_length;
                    (void)memcpy((void *)p_remtgt_info->ATRInfo, 
                                        (void *)reg_value, 
                                        p_remtgt_info->ATRInfo_Length);
                }
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case NXP_NFCIP_PSL1:
        {
            if (sizeof(*reg_value) == reg_length)
            {
                HCI_PRINT_BUFFER("\tNFCIP PSL1", reg_value, reg_length);
                p_nfcipinfo->psl1 = *reg_value;
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case NXP_NFCIP_PSL2:
        {
            if (sizeof(*reg_value) == reg_length)
            {
                HCI_PRINT_BUFFER("\tNFCIP PSL2", reg_value, reg_length);
                p_nfcipinfo->psl2 = *reg_value;
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case NXP_NFCIP_DID:
        {
            if (sizeof(*reg_value) == reg_length)
            {
                HCI_PRINT_BUFFER("\tNFCIP DID", reg_value, reg_length);
                p_nfcipinfo->did = *reg_value;
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case NXP_NFCIP_NAD:
        {
            if (sizeof(*reg_value) == reg_length)
            {
                HCI_PRINT_BUFFER("\tNFCIP NAD", reg_value, reg_length);
                p_nfcipinfo->nad = *reg_value;
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case NXP_NFCIP_OPTIONS:
        {
            if (sizeof(*reg_value) == reg_length)
            {
                HCI_PRINT_BUFFER("\tNFCIP OPTIONS", reg_value, reg_length);
                p_nfcipinfo->options = *reg_value;
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        default:
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_RESPONSE);
            break;
        }
    }
    return status;
}


NFCSTATUS
phHciNfc_NfcIP_SetMode(
                        phHciNfc_sContext_t     *psHciContext,
                        void                    *pHwRef,
                        phHciNfc_eNfcIPType_t   nfciptype,
                        uint8_t                 nfcip_mode                      
                        )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) || (NULL == pHwRef) || 
        (nfcip_mode > (uint8_t)NFCIP_MODE_ALL))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if (NFCIP_INVALID == nfciptype)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else if(NULL == psHciContext->p_nfcip_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_NfcIP_Info_t       *p_nfcipinfo=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        p_pipe_info = ((NFCIP_INITIATOR == nfciptype)? 
                        p_nfcipinfo->p_init_pipe_info: 
                        p_nfcipinfo->p_tgt_pipe_info);

        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            pipeid = p_pipe_info->pipe.pipe_id ;
            p_pipe_info->reg_index = (uint8_t)NXP_NFCIP_MODE;

            p_pipe_info->param_info = &nfcip_mode;
            p_pipe_info->param_length = sizeof(uint8_t);
            status = phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                    pipeid, (uint8_t)ANY_SET_PARAMETER);
        }
    }
    return status;
}

NFCSTATUS
phHciNfc_NfcIP_SetNAD(
                       phHciNfc_sContext_t      *psHciContext,
                       void                     *pHwRef,
                       phHciNfc_eNfcIPType_t    nfciptype,
                       uint8_t                  nad
                       )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) || (NULL == pHwRef))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if (NFCIP_INVALID == nfciptype)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else if(NULL == psHciContext->p_nfcip_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_NfcIP_Info_t       *p_nfcipinfo=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        p_pipe_info = ((NFCIP_INITIATOR == nfciptype)? 
                        p_nfcipinfo->p_init_pipe_info: 
                        p_nfcipinfo->p_tgt_pipe_info);
        
        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            pipeid = p_pipe_info->pipe.pipe_id ;
            p_pipe_info->reg_index = (uint8_t)NXP_NFCIP_NAD;

            p_pipe_info->param_info = &nad;
            p_pipe_info->param_length = sizeof(uint8_t);        
            status = phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                    pipeid, (uint8_t)ANY_SET_PARAMETER);
        }
    }
    return status;
}

NFCSTATUS
phHciNfc_NfcIP_SetDID(
                      phHciNfc_sContext_t   *psHciContext,
                      void                  *pHwRef,
                      uint8_t               did
                      )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    
    if( (NULL == psHciContext) || (NULL == pHwRef))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_nfcip_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_NfcIP_Info_t       *p_nfcipinfo=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        p_pipe_info = p_nfcipinfo->p_init_pipe_info;
        
        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            pipeid = p_pipe_info->pipe.pipe_id ;
            p_pipe_info->reg_index = (uint8_t)NXP_NFCIP_DID;

            p_pipe_info->param_info = &did;
            p_pipe_info->param_length = sizeof(uint8_t);        
            status = phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                        pipeid, (uint8_t)ANY_SET_PARAMETER);
        }
    }
    return status;
}

NFCSTATUS
phHciNfc_NfcIP_SetOptions(
                      phHciNfc_sContext_t       *psHciContext,
                      void                      *pHwRef,
                      phHciNfc_eNfcIPType_t     nfciptype,
                      uint8_t                   nfcip_options
                      )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) || (NULL == pHwRef))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if (NFCIP_INVALID == nfciptype)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else if(NULL == psHciContext->p_nfcip_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_NfcIP_Info_t       *p_nfcipinfo=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        p_pipe_info = ((NFCIP_INITIATOR == p_nfcipinfo->nfcip_type)? 
                        p_nfcipinfo->p_init_pipe_info: 
                        p_nfcipinfo->p_tgt_pipe_info);
        pipeid = p_pipe_info->pipe.pipe_id ;
        p_pipe_info->reg_index = (uint8_t)NXP_NFCIP_OPTIONS;

        p_pipe_info->param_info = &nfcip_options;
        p_pipe_info->param_length = sizeof(uint8_t);
        status = phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                pipeid, (uint8_t)ANY_SET_PARAMETER);
    }
    return status;
}

NFCSTATUS
phHciNfc_NfcIP_SetATRInfo(
                          phHciNfc_sContext_t       *psHciContext,
                          void                      *pHwRef,
                          phHciNfc_eNfcIPType_t     nfciptype,
                          phHal_sNfcIPCfg_t         *atr_info
                          )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) || (NULL == pHwRef) || (NULL == atr_info) || 
        (atr_info->generalBytesLength > NFCIP_ATR_MAX_LENGTH))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if (NFCIP_INVALID == nfciptype)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else if(NULL == psHciContext->p_nfcip_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_NfcIP_Info_t       *p_nfcipinfo=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                    psHciContext->p_nfcip_info ;
        p_pipe_info = ((NFCIP_INITIATOR == nfciptype)? 
                        p_nfcipinfo->p_init_pipe_info: 
                        p_nfcipinfo->p_tgt_pipe_info);

        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            pipeid = p_pipe_info->pipe.pipe_id ;
            p_pipe_info->reg_index = (uint8_t)((NFCIP_INITIATOR == nfciptype)? 
                                        NXP_NFCIP_ATR_REQ : 
                                        NXP_NFCIP_ATR_RES);

            p_pipe_info->param_info = atr_info->generalBytes;
            p_pipe_info->param_length = (uint8_t)
                                        atr_info->generalBytesLength;        
            status = phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                    pipeid, (uint8_t)ANY_SET_PARAMETER);
        }
    }
    return status;
}

NFCSTATUS
phHciNfc_NfcIP_SetPSL1(
                          phHciNfc_sContext_t   *psHciContext,
                          void                  *pHwRef,
                          uint8_t               psl1
                          )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) || (NULL == pHwRef))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_nfcip_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_NfcIP_Info_t       *p_nfcipinfo=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        p_pipe_info = p_nfcipinfo->p_init_pipe_info;

        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            pipeid = p_pipe_info->pipe.pipe_id ;
            p_pipe_info->reg_index = (uint8_t)NXP_NFCIP_PSL1;

            p_pipe_info->param_info = &psl1;
            p_pipe_info->param_length = sizeof(uint8_t);        
            status = phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                pipeid, (uint8_t)ANY_SET_PARAMETER);
        }
    }
    return status;
}

NFCSTATUS
phHciNfc_NfcIP_SetPSL2(
                       phHciNfc_sContext_t  *psHciContext,
                       void                 *pHwRef,
                       uint8_t              psl2
                          )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) || (NULL == pHwRef))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_nfcip_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_NfcIP_Info_t       *p_nfcipinfo=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        p_pipe_info = p_nfcipinfo->p_init_pipe_info;

        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            pipeid = p_pipe_info->pipe.pipe_id ;
            p_pipe_info->reg_index = (uint8_t)NXP_NFCIP_PSL2;

            p_pipe_info->param_info = &psl2;
            p_pipe_info->param_length = sizeof(uint8_t);        
            status = phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                    pipeid, (uint8_t)ANY_SET_PARAMETER);
        }
    }
    return status;
}

NFCSTATUS
phHciNfc_NfcIP_GetStatus(
                       phHciNfc_sContext_t      *psHciContext,
                       void                     *pHwRef,
                       phHciNfc_eNfcIPType_t    nfciptype
                          )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) || (NULL == pHwRef))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if (NFCIP_INVALID == nfciptype)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else if(NULL == psHciContext->p_nfcip_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_NfcIP_Info_t       *p_nfcipinfo=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        p_pipe_info =  ((NFCIP_INITIATOR == nfciptype)? 
                        p_nfcipinfo->p_init_pipe_info : 
                        p_nfcipinfo->p_tgt_pipe_info);
        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            pipeid = p_pipe_info->pipe.pipe_id ;
            p_pipe_info->reg_index = (uint8_t)NXP_NFCIP_STATUS;

            status = phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                        pipeid, (uint8_t)ANY_GET_PARAMETER);
        }
    }
    return status;
}

NFCSTATUS
phHciNfc_NfcIP_GetParam(
                         phHciNfc_sContext_t    *psHciContext,
                         void                   *pHwRef, 
                         phHciNfc_eNfcIPType_t  nfciptype
                         )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) || (NULL == pHwRef))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if (NFCIP_INVALID == nfciptype)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else if(NULL == psHciContext->p_nfcip_info) 
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_NfcIP_Info_t       *p_nfcipinfo=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        p_pipe_info =  ((NFCIP_INITIATOR == nfciptype)? 
                        p_nfcipinfo->p_init_pipe_info : 
                        p_nfcipinfo->p_tgt_pipe_info);
        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            pipeid = p_pipe_info->pipe.pipe_id ;
            p_pipe_info->reg_index = (uint8_t)NXP_NFCIP_PARAM;

            status = phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                    pipeid, (uint8_t)ANY_GET_PARAMETER);
        }
    }
    return status;
}

NFCSTATUS
phHciNfc_Initiator_Cont_Activate (
                                phHciNfc_sContext_t       *psHciContext,
                                void                      *pHwRef
                                )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_nfcip_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_NfcIP_Info_t     *p_nfcipinfo = NULL;
        phHciNfc_Pipe_Info_t      *p_pipe_info=NULL;

        p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        p_nfcipinfo->nfcip_type = NFCIP_INITIATOR;
        psHciContext->host_rf_type = phHal_eNfcIP1_Initiator;
        p_pipe_info =  p_nfcipinfo->p_init_pipe_info;
        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            phHciNfc_HCP_Packet_t       *hcp_packet = NULL;
            uint16_t                    length = HCP_HEADER_LEN; 
            uint8_t                     pipeid = 0;

            pipeid = p_pipe_info->pipe.pipe_id;
            psHciContext->tx_total = 0 ;
            hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;
            /* Construct the HCP Frame */
            phHciNfc_Build_HCPFrame(hcp_packet,(uint8_t)HCP_CHAINBIT_DEFAULT,
                            (uint8_t) pipeid, (uint8_t)HCP_MSG_TYPE_COMMAND, 
                            (uint8_t)NXP_NFCI_CONTINUE_ACTIVATION);

            p_pipe_info->sent_msg_type = (uint8_t)HCP_MSG_TYPE_COMMAND;
            p_pipe_info->prev_msg = (uint8_t)NXP_NFCI_CONTINUE_ACTIVATION;
            psHciContext->tx_total = length;
            psHciContext->response_pending = (uint8_t)TRUE;

            /* Send the Constructed HCP packet to the lower layer */
            status = phHciNfc_Send_HCP( psHciContext, pHwRef);
            p_pipe_info->prev_status = status;
        }       
    }
    return status;
}


NFCSTATUS
phHciNfc_NfcIP_GetATRInfo (
                           phHciNfc_sContext_t      *psHciContext,
                           void                     *pHwRef, 
                           phHciNfc_eNfcIPType_t    nfciptype
                           )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) 
        || (NULL == pHwRef))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if (NFCIP_INVALID == nfciptype)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else if(NULL == psHciContext->p_nfcip_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }   
    else
    {
        phHciNfc_NfcIP_Info_t       *p_nfcipinfo=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        p_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;

        p_pipe_info = ((NFCIP_INITIATOR == nfciptype)? 
                        p_nfcipinfo->p_init_pipe_info : 
                        p_nfcipinfo->p_tgt_pipe_info);
        
        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            p_pipe_info->reg_index = (uint8_t)((NFCIP_INITIATOR == nfciptype)?
                                    NXP_NFCIP_ATR_RES : 
                                    NXP_NFCIP_ATR_REQ);
            pipeid = p_pipe_info->pipe.pipe_id ;
            /* Fill the data buffer and send the command to the 
                device */
            status = 
                phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                pipeid, (uint8_t)ANY_GET_PARAMETER);
        }
    }
    return status;
}

NFCSTATUS 
phHciNfc_NfcIP_SetMergeSak( 
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef, 
                            uint8_t                 sak_value
                           )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) || (NULL == pHwRef) || 
        (sak_value > (uint8_t)TRUE))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_nfcip_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_NfcIP_Info_t       *ps_nfcipinfo=NULL;
        phHciNfc_Pipe_Info_t        *ps_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        ps_nfcipinfo = (phHciNfc_NfcIP_Info_t *)
                        psHciContext->p_nfcip_info ;
        ps_pipe_info = ps_nfcipinfo->p_tgt_pipe_info;

        if(NULL == ps_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            pipeid = ps_pipe_info->pipe.pipe_id ;
            ps_pipe_info->reg_index = (uint8_t)NXP_NFCIP_MERGE;

            ps_pipe_info->param_info = &sak_value;
            ps_pipe_info->param_length = sizeof(uint8_t);
            status = phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                    pipeid, (uint8_t)ANY_SET_PARAMETER);
        }
    }
    return status;
}

#endif /* #if defined (ENABLE_P2P) */

