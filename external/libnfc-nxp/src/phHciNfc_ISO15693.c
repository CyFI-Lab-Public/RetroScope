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
* \file  phHciNfc_ISO15693.c                                                 *
* \brief HCI ISO-15693 management routines.                                     *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Thu Feb 11 18:54:47 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.7 $                                                           *
* $Aliases:  $
*                                                                             *
* =========================================================================== *
*/

/*
***************************** Header File Inclusion ****************************
*/
#include <phNfcCompId.h>
#include <phHciNfc_Pipe.h>
#include <phHciNfc_RFReader.h>
#include <phOsalNfc.h>

#if defined (TYPE_ISO15693)
#include <phHciNfc_ISO15693.h>

/*
****************************** Macro Definitions *******************************
*/
#define ISO_15693_INVENTORY_INDEX               0x01U
#define ISO_15693_AFI_INDEX                     0x02U

#define ISO_15693_INVENTORY_LENGTH              0x0AU
#define ISO_15693_AFI_LENGTH                    0x01U

#define ISO_15693_SINGLE_TAG_FOUND              0x00U
#define ISO_15693_MULTIPLE_TAGS_FOUND           0x03U

/*
*************************** Structure and Enumeration ***************************
*/

/*
*************************** Static Function Declaration **************************
*/

static
NFCSTATUS
phHciNfc_ISO15693_InfoUpdate(
                                phHciNfc_sContext_t     *psHciContext,
                                uint8_t                 index,
                                uint8_t                 *reg_value,
                                uint8_t                 reg_length
                         );

static 
NFCSTATUS
phHciNfc_Recv_ISO15693_Response(
                        void                *psContext,
                        void                *pHwRef,
                        uint8_t             *pResponse,
#ifdef ONE_BYTE_LEN
                        uint8_t             length
#else
                        uint16_t            length
#endif
                       );

static
NFCSTATUS
phHciNfc_Recv_ISO15693_Event(
                             void               *psContext,
                             void               *pHwRef,
                             uint8_t            *pEvent,
#ifdef ONE_BYTE_LEN
                             uint8_t            length
#else
                             uint16_t           length
#endif
                       );

/*
*************************** Function Definitions ***************************
*/

NFCSTATUS
phHciNfc_ISO15693_Init_Resources(
                                  phHciNfc_sContext_t     *psHciContext
                                  )
{    
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_ISO15693_Info_t    *ps_15693_info=NULL;
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if (NULL != psHciContext->p_iso_15693_info)
        {
            status = NFCSTATUS_SUCCESS;
        } 
        else if(( NULL == psHciContext->p_iso_15693_info ) &&
            (phHciNfc_Allocate_Resource((void **)(&ps_15693_info),
            sizeof(phHciNfc_ISO15693_Info_t))== NFCSTATUS_SUCCESS)
            )
        {
            psHciContext->p_iso_15693_info = ps_15693_info;
            ps_15693_info->current_seq = ISO15693_INVENTORY;
            ps_15693_info->next_seq = ISO15693_INVENTORY;
            ps_15693_info->ps_15693_pipe_info = NULL;
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INSUFFICIENT_RESOURCES);
        }

    }
    return status;
}


NFCSTATUS
phHciNfc_ISO15693_Get_PipeID(
                              phHciNfc_sContext_t     *psHciContext,
                              uint8_t                 *ppipe_id
                              )
{
    NFCSTATUS       status = NFCSTATUS_SUCCESS;
    if( (NULL != psHciContext)
        && ( NULL != ppipe_id )
        && ( NULL != psHciContext->p_iso_15693_info ) 
        )
    {
        phHciNfc_ISO15693_Info_t     *ps_15693_info = NULL;
        ps_15693_info = (phHciNfc_ISO15693_Info_t *)
                            psHciContext->p_iso_15693_info ;
        *ppipe_id =  ps_15693_info->ps_15693_pipe_info->pipe.pipe_id;
    }
    else 
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    return status;
}


NFCSTATUS
phHciNfc_ISO15693_Update_PipeInfo(
                                   phHciNfc_sContext_t     *psHciContext,
                                   uint8_t                 pipeID,
                                   phHciNfc_Pipe_Info_t    *pPipeInfo
                                   )
{
    NFCSTATUS       status = NFCSTATUS_SUCCESS;
    if((NULL == psHciContext) || (NULL == pPipeInfo))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_iso_15693_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_ISO15693_Info_t       *ps_15693_info = NULL;
        ps_15693_info = (phHciNfc_ISO15693_Info_t *)
                        psHciContext->p_iso_15693_info ;

        /* Update the pipe_id of the ISO15693 Gate obtained from 
        the HCI Response */
        ps_15693_info->ps_15693_pipe_info = pPipeInfo;
        ps_15693_info->pipe_id = pipeID;
        ps_15693_info->ps_15693_pipe_info->pipe.pipe_id = pipeID;        
        /* Update the Response Receive routine of the ISO15693 Gate */
        pPipeInfo->recv_resp = &phHciNfc_Recv_ISO15693_Response;
        /* Update the event Receive routine of the ISO15693 Gate */
        pPipeInfo->recv_event = &phHciNfc_Recv_ISO15693_Event;
    }
    return status;
}


NFCSTATUS
phHciNfc_ISO15693_Update_Info(
                             phHciNfc_sContext_t        *psHciContext,
                             uint8_t                    infotype,
                             void                       *iso_15693_info
                             )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    
    if (NULL == psHciContext)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_iso_15693_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_ISO15693_Info_t     *ps_15693_info = NULL;
        ps_15693_info = (phHciNfc_ISO15693_Info_t *)
                        psHciContext->p_iso_15693_info;

        switch(infotype)
        {
            case HCI_ISO_15693_ENABLE:
            {
                if (NULL != iso_15693_info)
                {
                    ps_15693_info->enable_iso_15693_gate = 
                                    *((uint8_t *)iso_15693_info);
                }
                break;
            }
            case HCI_ISO_15693_INFO_SEQ:
            {
                ps_15693_info->current_seq = ISO15693_INVENTORY;
                ps_15693_info->next_seq = ISO15693_INVENTORY;
                break;
            }           
            default:
            {
                status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
                break;
            }
        }
    }
    return status;
}


NFCSTATUS
phHciNfc_ISO15693_Info_Sequence (
                       void             *psHciHandle,
                       void             *pHwRef
                       )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t     *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    if( (NULL == psHciContext) 
        || (NULL == pHwRef)
      )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_iso_15693_info) || 
        (HCI_ISO_15693_ENABLE != 
        ((phHciNfc_ISO15693_Info_t *)(psHciContext->p_iso_15693_info))->
        enable_iso_15693_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }   
    else
    {
        phHciNfc_ISO15693_Info_t    *ps_15693_info = NULL;
        phHciNfc_Pipe_Info_t        *ps_pipe_info = NULL;
        uint8_t                     pipeid = 0;
        
        ps_15693_info = (phHciNfc_ISO15693_Info_t *)
                                psHciContext->p_iso_15693_info;
        ps_pipe_info = ps_15693_info->ps_15693_pipe_info;

        if(NULL == ps_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            switch(ps_15693_info->current_seq)
            {
                case ISO15693_INVENTORY:
                {
                    ps_pipe_info->reg_index = ISO_15693_INVENTORY_INDEX;
                    pipeid = ps_pipe_info->pipe.pipe_id ;
                    /* Fill the data buffer and send the command to the 
                            device */
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                pipeid, (uint8_t)ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
                        ps_15693_info->next_seq = ISO15693_AFI;
                    }
                    break;
                }
                case ISO15693_AFI:
                {
                    ps_pipe_info->reg_index = ISO_15693_AFI_INDEX;
                    pipeid = ps_pipe_info->pipe.pipe_id ;
                    /* Fill the data buffer and send the command to the 
                            device */
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                pipeid, (uint8_t)ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
                        ps_15693_info->next_seq = ISO15693_END_SEQUENCE;
                    }
                    break;
                }
                case ISO15693_END_SEQUENCE:
                {
                    phNfc_sCompletionInfo_t     CompInfo;
                    if (ISO_15693_MULTIPLE_TAGS_FOUND == 
                        ps_15693_info->multiple_tgts_found)
                    {
                        CompInfo.status = NFCSTATUS_MULTIPLE_TAGS;
                    }
                    else
                    {
                        CompInfo.status = NFCSTATUS_SUCCESS;
                    }

                    CompInfo.info = &(ps_15693_info->iso15693_info);
                    
                    ps_15693_info->iso15693_info.RemDevType = 
                                    phHal_eISO15693_PICC;
                    ps_15693_info->current_seq = ISO15693_INVENTORY;
                    ps_15693_info->next_seq = ISO15693_INVENTORY;
                    status = NFCSTATUS_SUCCESS;
                    /* Notify to the upper layer */
                    phHciNfc_Tag_Notify(psHciContext, pHwRef, 
                                    NFC_NOTIFY_TARGET_DISCOVERED, 
                                    &CompInfo);
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
phHciNfc_ISO15693_InfoUpdate(
                                phHciNfc_sContext_t     *psHciContext,
                                uint8_t                 index,
                                uint8_t                 *reg_value,
                                uint8_t                 reg_length
                         )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_ISO15693_Info_t    *ps_15693_info = NULL;
    uint8_t                     i = 0;
                                
    ps_15693_info = (phHciNfc_ISO15693_Info_t *)
                    (psHciContext->p_iso_15693_info);


    switch(index)
    {
        case ISO_15693_INVENTORY_INDEX:
        {
            if (ISO_15693_INVENTORY_LENGTH == reg_length)
            {
                ps_15693_info->iso15693_info.RemoteDevInfo
                    .Iso15693_Info.Flags = *(reg_value + i );
                i++;
                ps_15693_info->iso15693_info.RemoteDevInfo
                    .Iso15693_Info.Dsfid = *(reg_value + i );
                i++;
                (void)memcpy(ps_15693_info->iso15693_info.
                     RemoteDevInfo.Iso15693_Info.Uid,
                       (reg_value+i), (reg_length - i ));
                ps_15693_info->iso15693_info.RemoteDevInfo
                                    .Iso15693_Info.UidLength = ( reg_length - i ); 
                HCI_PRINT_BUFFER("\tISO 15693 inventory", reg_value, reg_length);
            }
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case ISO_15693_AFI_INDEX:
        {
            if (ISO_15693_AFI_LENGTH == reg_length)
            {
                ps_15693_info->iso15693_info.RemoteDevInfo
                                    .Iso15693_Info.Afi = *(reg_value + i );
                HCI_PRINT_BUFFER("\tISO 15693 AFI", reg_value, reg_length);
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


static 
NFCSTATUS
phHciNfc_Recv_ISO15693_Response(
                        void                *psContext,
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
                                (phHciNfc_sContext_t *)psContext ;
    

    if( (NULL == psHciContext) || (NULL == pHwRef) || (NULL == pResponse)
        || (0 == length))
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_iso_15693_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_ISO15693_Info_t    *ps_15693_info = NULL;
        phHciNfc_Pipe_Info_t        *ps_pipe_info = NULL;
        uint8_t                     prev_cmd = ANY_GET_PARAMETER;

        ps_15693_info = (phHciNfc_ISO15693_Info_t *)
                            psHciContext->p_iso_15693_info;

        ps_pipe_info = ps_15693_info->ps_15693_pipe_info;
        if( NULL == ps_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            prev_cmd = ps_pipe_info->prev_msg ;
            switch(prev_cmd)
            {
                case ANY_GET_PARAMETER:
                {
                    status = phHciNfc_ISO15693_InfoUpdate(psHciContext,
                                    ps_pipe_info->reg_index, 
                                    &pResponse[HCP_HEADER_LEN],
                                    (uint8_t)(length - HCP_HEADER_LEN));
#if 0
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                                UPDATE_SEQ);
#endif /* #if 0 */
                    break;
                }
                case ANY_SET_PARAMETER:
                {
                    HCI_PRINT("ISO 15693 Parameter Set \n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                    UPDATE_SEQ);
                    ps_15693_info->next_seq = ISO15693_INVENTORY;
                    break;
                }
                case ANY_OPEN_PIPE:
                {
                    HCI_PRINT("ISO 15693 open pipe complete\n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                    UPDATE_SEQ);
                    ps_15693_info->next_seq = ISO15693_INVENTORY;
                    break;
                }
                case ANY_CLOSE_PIPE:
                {
                    HCI_PRINT("ISO 15693 close pipe complete\n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                    UPDATE_SEQ);
                    break;
                }

                case NXP_ISO15693_CMD:
                {
                    if (length >= HCP_HEADER_LEN)
                    {
                        HCI_PRINT("ISO 15693 packet received \n");
                        /* Copy buffer to the receive buffer */
                        phHciNfc_Append_HCPFrame(psHciContext->recv_buffer, 
                            0, pResponse, length);
                        psHciContext->rx_total = length;
                        psHciContext->rx_index = HCP_HEADER_LEN;
                        HCI_PRINT_BUFFER("ISO 15693 Bytes received", 
                                                        pResponse, length);
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
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_RESPONSE);
                    break;
                }
            }/* End of switch(prev_cmd) */

            if( NFCSTATUS_SUCCESS == status )
            {
                ps_pipe_info->prev_status = NFCSTATUS_SUCCESS;
                ps_15693_info->current_seq = ps_15693_info->next_seq;
            }
        }
    }
    return status;
}

static
NFCSTATUS
phHciNfc_Recv_ISO15693_Event(
                             void               *psContext,
                             void               *pHwRef,
                             uint8_t            *pEvent,
#ifdef ONE_BYTE_LEN
                             uint8_t            length
#else
                             uint16_t           length
#endif
                       )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t         *psHciContext = 
                                (phHciNfc_sContext_t *)psContext ;
    if( (NULL == psHciContext) || (NULL == pHwRef) || (NULL == pEvent)
        || (length == 0))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_iso_15693_info) || 
        (HCI_ISO_15693_ENABLE != 
        ((phHciNfc_ISO15693_Info_t *)(psHciContext->p_iso_15693_info))->
        enable_iso_15693_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_HCP_Packet_t       *p_packet = NULL;
        phHciNfc_ISO15693_Info_t    *ps_15693_info=NULL;
        phHciNfc_HCP_Message_t      *message = NULL;
        uint8_t                     instruction=0, 
                                    i = 0;

        ps_15693_info = (phHciNfc_ISO15693_Info_t *)
                        psHciContext->p_iso_15693_info;
        p_packet = (phHciNfc_HCP_Packet_t *)pEvent;
        message = &p_packet->msg.message;
        /* Get the instruction bits from the Message Header */
        instruction = (uint8_t) GET_BITS8( message->msg_header,
                HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);

        if ((EVT_TARGET_DISCOVERED == instruction) 
            && ((ISO_15693_MULTIPLE_TAGS_FOUND == message->payload[i]) 
                || (ISO_15693_SINGLE_TAG_FOUND == message->payload[i])) 
          )
        {
            phNfc_sCompletionInfo_t pCompInfo;

/* #define NFC_ISO_15693_MULTIPLE_TAGS_SUPPORT 0x00 */
#if (NFC_ISO_15693_MULTIPLE_TAGS_SUPPORT >= 0x01)

            if (ISO_15693_MULTIPLE_TAGS_FOUND == message->payload[i])
            {
                ps_15693_info->multiple_tgts_found = ISO_15693_MULTIPLE_TAGS_FOUND;
                pCompInfo.status = NFCSTATUS_MULTIPLE_TAGS;
            }
            else
#endif /* #if (NFC_ISO_15693_MULTIPLE_TAGS_SUPPORT <= 0x01) */
            {
                ps_15693_info->multiple_tgts_found = FALSE;
                pCompInfo.status = NFCSTATUS_SUCCESS;
            }
            /* CompInfo.info = &(ps_15693_info->iso15693_info); */

            psHciContext->host_rf_type = phHal_eISO15693_PCD;
            ps_15693_info->iso15693_info.RemDevType = phHal_eISO15693_PICC;
            ps_15693_info->current_seq = ISO15693_INVENTORY;
            /* Notify to the HCI Generic layer To Update the FSM */
            phHciNfc_Notify_Event(psHciContext, pHwRef, 
                                    NFC_NOTIFY_TARGET_DISCOVERED, 
                                    &pCompInfo);

        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_RESPONSE);
        }
    }
    return status;
}

NFCSTATUS
phHciNfc_Send_ISO15693_Command(
                              phHciNfc_sContext_t   *psHciContext,
                              void                  *pHwRef,
                              uint8_t               pipe_id,
                              uint8_t               cmd
                              )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    
    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_iso_15693_info) || 
        (HCI_ISO_15693_ENABLE != 
        ((phHciNfc_ISO15693_Info_t *)(psHciContext->p_iso_15693_info))->
        enable_iso_15693_gate) || 
        (HCI_UNKNOWN_PIPE_ID == 
        ((phHciNfc_ISO15693_Info_t *)(psHciContext->p_iso_15693_info))->
        pipe_id) || 
        (pipe_id != 
        ((phHciNfc_ISO15693_Info_t *)(psHciContext->p_iso_15693_info))->
        pipe_id))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_ISO15693_Info_t    *ps_15693_info=NULL;
        phHciNfc_Pipe_Info_t        *ps_pipe_info=NULL;
        phHciNfc_HCP_Packet_t       *hcp_packet = NULL;
        phHciNfc_HCP_Message_t      *hcp_message = NULL;
        uint8_t                     i = 0;
        uint16_t                    length = HCP_HEADER_LEN;
        
        ps_15693_info = (phHciNfc_ISO15693_Info_t *)
                            psHciContext->p_iso_15693_info ;
        ps_pipe_info = ps_15693_info->ps_15693_pipe_info;
        if(NULL == ps_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                        NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            psHciContext->tx_total = 0 ;
            hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;
            /* Construct the HCP Frame */
            if (NXP_ISO15693_CMD == cmd)
            {
                phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                                (uint8_t) pipe_id, HCP_MSG_TYPE_COMMAND, cmd);
                hcp_message = &(hcp_packet->msg.message);

#if 0
                /* Command */
                hcp_message->payload[i++] = 
                                psHciContext->p_xchg_info->params.tag_info.cmd_type ;
                    /* Address */
                hcp_message->payload[i++] = 
                                psHciContext->p_xchg_info->params.tag_info.addr ;
#endif /* #if 0 */
                phHciNfc_Append_HCPFrame((uint8_t *)hcp_message->payload,
                    i, (uint8_t *)ps_pipe_info->param_info,
                    ps_pipe_info->param_length);
                length =(uint16_t)(length + i + ps_pipe_info->param_length);
            
                ps_pipe_info->sent_msg_type = (uint8_t)HCP_MSG_TYPE_COMMAND;
                ps_pipe_info->prev_msg = cmd;
                psHciContext->tx_total = length;
                psHciContext->response_pending = TRUE;

                /* Send the Constructed HCP packet to the lower layer */
                status = phHciNfc_Send_HCP( psHciContext, pHwRef);
                ps_pipe_info->prev_status = status;
            }
        }
    }
    return status;
}

NFCSTATUS
phHciNfc_ISO15693_Set_AFI(
                               void         *psContext,
                               void         *pHwRef,
                               uint8_t      afi_value
                               )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t         *psHciContext = 
                                (phHciNfc_sContext_t *)psContext ;

    if( (NULL == psHciContext) || (NULL == pHwRef))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_iso_15693_info) || 
        (HCI_ISO_15693_ENABLE != 
        ((phHciNfc_ISO15693_Info_t *)(psHciContext->p_iso_15693_info))->
        enable_iso_15693_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_ISO15693_Info_t    *ps_15693_info = NULL;
        phHciNfc_Pipe_Info_t        *ps_pipe_info = NULL;
        uint8_t                     pipeid = 0;

        ps_15693_info = (phHciNfc_ISO15693_Info_t *)
                            psHciContext->p_iso_15693_info ;
        ps_pipe_info = ps_15693_info->ps_15693_pipe_info;

        if( NULL == ps_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            ps_pipe_info->reg_index = ISO_15693_AFI_INDEX;
            ps_pipe_info->param_info = &afi_value;
            ps_pipe_info->param_length = sizeof(afi_value);
            pipeid = ps_pipe_info->pipe.pipe_id ;
            status = phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                        pipeid, (uint8_t)ANY_SET_PARAMETER);
        }
    }
    return status;
}

#endif /* #if defined (TYPE_ISO15693) */


