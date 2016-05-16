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
* \file  phHciNfc_RFReaderB.c                                                 *
* \brief HCI Reader B Management Routines.                                    *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Mon Aug 17 15:17:07 2009 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.7 $                                                           *
* $Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
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

#if defined (TYPE_B)
#include <phHciNfc_RFReaderB.h>
/*
****************************** Macro Definitions *******************************
*/

#define RDR_B_SINGLE_TAG_FOUND              0x00U
#define RDR_B_MULTIPLE_TAGS_FOUND           0x03U
/* Commands exposed to the upper layer */
#define NXP_WRA_CONTINUE_ACTIVATION         0x12U

#define RDR_B_PUPI_INDEX                    0x03U
#define RDR_B_APP_DATA_INDEX                0x04U
#define RDR_B_AFI_INDEX                     0x02U
#define RDR_B_HIGHER_LAYER_RESP_INDEX       0x01U
#define RDR_B_HIGHER_LAYER_DATA_INDEX       0x05U


/*
*************************** Structure and Enumeration ***************************
*/


/*
*************************** Static Function Declaration **************************
*/

static 
NFCSTATUS
phHciNfc_Recv_ReaderB_Response(
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
phHciNfc_Recv_ReaderB_Event(
                            void               *psContext,
                            void               *pHwRef,
                            uint8_t            *pEvent,
#ifdef ONE_BYTE_LEN
                            uint8_t             length
#else
                            uint16_t            length
#endif
                            );

static
NFCSTATUS
phHciNfc_ReaderB_InfoUpdate(
                            phHciNfc_sContext_t     *psHciContext,
                            uint8_t                 index,
                            uint8_t                 *reg_value,
                            uint8_t                 reg_length
                            );

static
NFCSTATUS
phHciNfc_Recv_Iso_B_Packet(
                           phHciNfc_sContext_t  *psHciContext,
                           uint8_t              *pResponse,
#ifdef ONE_BYTE_LEN
                           uint8_t             length
#else
                           uint16_t            length
#endif
                           );
/*
*************************** Function Definitions ***************************
*/

NFCSTATUS
phHciNfc_ReaderB_Get_PipeID(
                            phHciNfc_sContext_t        *psHciContext,
                            uint8_t                    *ppipe_id
                            )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL != psHciContext)
        && ( NULL != ppipe_id )
        && ( NULL != psHciContext->p_reader_b_info ) 
        )
    {
        phHciNfc_ReaderB_Info_t     *p_rdr_b_info=NULL;
        p_rdr_b_info = (phHciNfc_ReaderB_Info_t *)
                            psHciContext->p_reader_b_info ;
        *ppipe_id =  p_rdr_b_info->pipe_id  ;
    }
    else 
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    return status;
}

NFCSTATUS
phHciNfc_ReaderB_Init_Resources(
                                phHciNfc_sContext_t     *psHciContext
                                )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_ReaderB_Info_t     *p_rdr_b_info=NULL;
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if(
            ( NULL == psHciContext->p_reader_b_info ) &&
                (phHciNfc_Allocate_Resource((void **)(&p_rdr_b_info),
                sizeof(phHciNfc_ReaderB_Info_t))== NFCSTATUS_SUCCESS)
            )
        {
            psHciContext->p_reader_b_info = p_rdr_b_info;
            p_rdr_b_info->current_seq = RDR_B_INVALID_SEQ;
            p_rdr_b_info->next_seq = RDR_B_INVALID_SEQ;
            p_rdr_b_info->pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INSUFFICIENT_RESOURCES);
        }

    }
    return status;
}

NFCSTATUS
phHciNfc_ReaderB_Update_Info(
                             phHciNfc_sContext_t        *psHciContext,
                             uint8_t                    infotype,
                             void                       *rdr_b_info
                             )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    
    if (NULL == psHciContext)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_reader_b_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_ReaderB_Info_t     *ps_rdr_b_info=NULL;
        ps_rdr_b_info = (phHciNfc_ReaderB_Info_t *)
                        psHciContext->p_reader_b_info ;

        switch(infotype)
        {
            case HCI_READER_B_ENABLE:
            {
                if(NULL != rdr_b_info)
                {
                    ps_rdr_b_info->enable_rdr_b_gate = 
                                *((uint8_t *)rdr_b_info);
                }
                break;
            }
            case HCI_READER_B_INFO_SEQ:
            {
                ps_rdr_b_info->current_seq = RDR_B_PUPI;
                ps_rdr_b_info->next_seq = RDR_B_PUPI;
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
phHciNfc_ReaderB_Update_PipeInfo(
                                 phHciNfc_sContext_t     *psHciContext,
                                 uint8_t                 pipeID,
                                 phHciNfc_Pipe_Info_t    *pPipeInfo
                                 )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_reader_b_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_ReaderB_Info_t *p_rdr_b_info=NULL;
        p_rdr_b_info = (phHciNfc_ReaderB_Info_t *)
                            psHciContext->p_reader_b_info ;
        /* Update the pipe_id of the reader B Gate obtained from the HCI Response */
        p_rdr_b_info->pipe_id = pipeID;
        p_rdr_b_info->p_pipe_info = pPipeInfo;
        /* Update the Response Receive routine of the reader B Gate */
        pPipeInfo->recv_resp = &phHciNfc_Recv_ReaderB_Response;
        /* Update the event Receive routine of the reader B Gate */
        pPipeInfo->recv_event = &phHciNfc_Recv_ReaderB_Event;
    }

    return status;
}

NFCSTATUS
phHciNfc_ReaderB_Info_Sequence (
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
    else if((NULL == psHciContext->p_reader_b_info) || 
        (HCI_READER_B_ENABLE != 
        ((phHciNfc_ReaderB_Info_t *)(psHciContext->p_reader_b_info))->
        enable_rdr_b_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }   
    else
    {
        phHciNfc_ReaderB_Info_t     *p_rdr_b_info=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        p_rdr_b_info = (phHciNfc_ReaderB_Info_t *)
                            psHciContext->p_reader_b_info ;
        p_pipe_info = p_rdr_b_info->p_pipe_info;
        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            switch(p_rdr_b_info->current_seq)
            {
                case RDR_B_PUPI:
                {
                    p_pipe_info->reg_index = RDR_B_PUPI_INDEX;
                    pipeid = p_rdr_b_info->pipe_id ;
                    /* Fill the data buffer and send the command to the 
                    device */
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                        pipeid, (uint8_t)ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_rdr_b_info->next_seq = RDR_B_APP_DATA;
                    }
                    break;
                }
                case RDR_B_APP_DATA:
                {
                    p_pipe_info->reg_index = RDR_B_APP_DATA_INDEX;
                    pipeid = p_rdr_b_info->pipe_id ;
                    /* Fill the data buffer and send the command to the 
                    device */
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                        pipeid, (uint8_t)ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_rdr_b_info->next_seq = RDR_B_AFI;
                    }
                    break;
                }
                case RDR_B_AFI:
                {
                    /* RW to the registry */
                    p_pipe_info->reg_index = RDR_B_AFI_INDEX;
                    pipeid = p_rdr_b_info->pipe_id ;
                    /* Fill the data buffer and send the command to the 
                    device */
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                        pipeid, (uint8_t)ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_rdr_b_info->next_seq = RDR_B_HIGHER_LAYER_RESP;
                    }
                    break;
                }

                case RDR_B_HIGHER_LAYER_RESP:
                {
                    p_pipe_info->reg_index = RDR_B_HIGHER_LAYER_RESP_INDEX;
                    pipeid = p_rdr_b_info->pipe_id ;
                    /* Fill the data buffer and send the command to the 
                        device */
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                    pipeid, (uint8_t)ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_rdr_b_info->next_seq = RDR_B_HIGHER_LAYER_DATA;
                    }
                    break;
                }

                case RDR_B_HIGHER_LAYER_DATA:
                {
                    /* RW to the registry */
                    p_pipe_info->reg_index = RDR_B_HIGHER_LAYER_DATA_INDEX;
                    pipeid = p_rdr_b_info->pipe_id ;
                    /* Fill the data buffer and send the command to the 
                    device */
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                        pipeid, (uint8_t)ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_rdr_b_info->next_seq = RDR_B_END_SEQUENCE;
                    }
                    break;
                }
                case RDR_B_END_SEQUENCE:
                {
                    phNfc_sCompletionInfo_t     CompInfo;
                    if (RDR_B_MULTIPLE_TAGS_FOUND == 
                        p_rdr_b_info->multiple_tgts_found)
                    {
                        CompInfo.status = NFCSTATUS_MULTIPLE_TAGS;
                    }
                    else
                    {
                        CompInfo.status = NFCSTATUS_SUCCESS;
                    }

                    CompInfo.info = &(p_rdr_b_info->reader_b_info);

                    p_rdr_b_info->reader_b_info.RemDevType = phHal_eISO14443_B_PICC;
                    p_rdr_b_info->current_seq = RDR_B_PUPI;
                    p_rdr_b_info->next_seq = RDR_B_PUPI;
                    status = NFCSTATUS_SUCCESS;
                    /* Notify to the upper layer */
                    phHciNfc_Tag_Notify(psHciContext, 
                                        pHwRef, 
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
phHciNfc_Recv_ReaderB_Response(
                               void                *psContext,
                               void                *pHwRef,
                               uint8_t             *pResponse,
#ifdef ONE_BYTE_LEN
                               uint8_t              length
#else
                               uint16_t             length
#endif
                               )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t         *psHciContext = 
                                (phHciNfc_sContext_t *)psContext ;

    if( (NULL == psHciContext) || (NULL == pHwRef) || (NULL == pResponse)
        || (length == 0))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_reader_b_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_ReaderB_Info_t     *p_rdr_b_info=NULL;
        uint8_t                     prev_cmd = ANY_GET_PARAMETER;

        p_rdr_b_info = (phHciNfc_ReaderB_Info_t *)
                                psHciContext->p_reader_b_info ;
        if( NULL == p_rdr_b_info->p_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            prev_cmd = p_rdr_b_info->p_pipe_info->prev_msg ;
            switch(prev_cmd)
            {
            case ANY_GET_PARAMETER:
                {
                    status = phHciNfc_ReaderB_InfoUpdate(psHciContext,
                                        p_rdr_b_info->p_pipe_info->reg_index, 
                                        &pResponse[HCP_HEADER_LEN],
                                        (uint8_t)(length - HCP_HEADER_LEN));
                    break;
                }
                case ANY_SET_PARAMETER:
                {
                    HCI_PRINT("Reader B Parameter Set \n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                                UPDATE_SEQ);
                    p_rdr_b_info->next_seq = RDR_B_PUPI;
                    break;
                }
                case ANY_OPEN_PIPE:
                {
                    HCI_PRINT("Reader B open pipe complete\n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                                UPDATE_SEQ);
                    p_rdr_b_info->next_seq = RDR_B_PUPI;
                    break;
                }
                case ANY_CLOSE_PIPE:
                {
                    HCI_PRINT("Reader B close pipe complete\n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                                UPDATE_SEQ);
                    break;
                }
                case NXP_WRA_CONTINUE_ACTIVATION:
                case NXP_WR_ACTIVATE_ID:
                {
                    HCI_PRINT("Reader B continue activation or ");
                    HCI_PRINT("reactivation completed \n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                    UPDATE_SEQ);
                    break;
                }
                case WR_XCHGDATA:
                {
                    if (length >= HCP_HEADER_LEN)
                    {
                        uint8_t         i = 1;
                        HCI_PRINT("ISO 14443-4B received \n");
                        /* Copy buffer to the receive buffer */
                        phHciNfc_Append_HCPFrame(psHciContext->recv_buffer, 
                                                0, pResponse, length);
                        psHciContext->rx_total = (length - i);
                        status = phHciNfc_Recv_Iso_B_Packet(psHciContext, 
                                                    &pResponse[HCP_HEADER_LEN],
                                                    (length - HCP_HEADER_LEN));
                    } 
                    else
                    {
                        status = PHNFCSTVAL(CID_NFC_HCI, 
                                            NFCSTATUS_INVALID_HCI_RESPONSE);
                    }
                    break;
                }
                case NXP_WR_PRESCHECK:
                {
                    HCI_PRINT("Presence check completed \n");
                    break;
                }
                case NXP_WR_ACTIVATE_NEXT:
                {
                    if (length > HCP_HEADER_LEN)
                    {
                        if (RDR_B_MULTIPLE_TAGS_FOUND == pResponse[HCP_HEADER_LEN])
                        {
                            p_rdr_b_info->multiple_tgts_found = 
                                RDR_B_MULTIPLE_TAGS_FOUND;
                        }
                        else
                        {
                            p_rdr_b_info->multiple_tgts_found = FALSE;
                        }
                        HCI_PRINT("Activate next completed \n");
                    }
                    else
                    {
                        status = PHNFCSTVAL(CID_NFC_HCI, 
                                            NFCSTATUS_INVALID_HCI_RESPONSE);
                    }
                    break;
                }
                case NXP_WR_DISPATCH_TO_UICC:
                {
                    switch(length)
                    {
                        case HCP_HEADER_LEN:
                        {
                            /* Error code field is optional, if no error 
                                code field in the response, then the command 
                                is successfully completed */
                            p_rdr_b_info->uicc_activation = 
                                        UICC_CARD_ACTIVATION_SUCCESS;
                            break;
                        }
                        case (HCP_HEADER_LEN + 1):
                        {
                            p_rdr_b_info->uicc_activation = 
                                        pResponse[HCP_HEADER_LEN];
                            break;
                        } /* End of case (HCP_HEADER_LEN + index) */
                        default:
                        {                            
                            status = PHNFCSTVAL(CID_NFC_HCI, 
                                                NFCSTATUS_INVALID_HCI_RESPONSE);
                            break;
                        }
                    } /* End of switch(length) */
                    if (NFCSTATUS_SUCCESS == status)
                    {
                        status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                                    UPDATE_SEQ);
                    }
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
                p_rdr_b_info->p_pipe_info->prev_status = NFCSTATUS_SUCCESS;
                p_rdr_b_info->current_seq = p_rdr_b_info->next_seq;
            }
        }
    }
    return status;
}


static
NFCSTATUS
phHciNfc_Recv_ReaderB_Event(
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
        || (length == 0))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_reader_b_info) || 
        (HCI_READER_B_ENABLE != 
        ((phHciNfc_ReaderB_Info_t *)(psHciContext->p_reader_b_info))->
        enable_rdr_b_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_HCP_Packet_t       *p_packet = NULL;
        phHciNfc_ReaderB_Info_t     *p_rdr_b_info=NULL;
        phHciNfc_HCP_Message_t      *message = NULL;
        uint8_t                     instruction=0, 
                                    i = 0;

        p_rdr_b_info = (phHciNfc_ReaderB_Info_t *)
                                    psHciContext->p_reader_b_info ;
        p_packet = (phHciNfc_HCP_Packet_t *)pEvent;
        message = &p_packet->msg.message;
        /* Get the instruction bits from the Message Header */
        instruction = (uint8_t) GET_BITS8( message->msg_header,
                        HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);

        if ((EVT_TARGET_DISCOVERED == instruction) 
            && ((RDR_B_MULTIPLE_TAGS_FOUND == message->payload[i] ) 
            || (RDR_B_SINGLE_TAG_FOUND == message->payload[i])) 
            )
        {
            phNfc_sCompletionInfo_t pCompInfo;

            if (RDR_B_MULTIPLE_TAGS_FOUND == message->payload[i])
            {
                p_rdr_b_info->multiple_tgts_found = RDR_B_MULTIPLE_TAGS_FOUND;
                pCompInfo.status = NFCSTATUS_MULTIPLE_TAGS;
            }
            else
            {
                p_rdr_b_info->multiple_tgts_found = FALSE;
                pCompInfo.status = NFCSTATUS_SUCCESS;
            }

            psHciContext->host_rf_type = phHal_eISO14443_B_PCD;
            p_rdr_b_info->reader_b_info.RemDevType = phHal_eISO14443_B_PICC;
            p_rdr_b_info->current_seq = RDR_B_PUPI;

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

static
NFCSTATUS
phHciNfc_ReaderB_InfoUpdate(
                            phHciNfc_sContext_t     *psHciContext,
                            uint8_t                 index,
                            uint8_t                 *reg_value,
                            uint8_t                 reg_length
                            )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_ReaderB_Info_t     *p_rdr_b_info=NULL;
    phHal_sIso14443BInfo_t      *p_tag_b_info = NULL;

    p_rdr_b_info = (phHciNfc_ReaderB_Info_t *)
                    (psHciContext->p_reader_b_info );
    p_tag_b_info = &(p_rdr_b_info->reader_b_info.RemoteDevInfo.Iso14443B_Info);

    switch(index)
    {
        case RDR_B_PUPI_INDEX:
        {
            HCI_PRINT_BUFFER("\tReader B PUPI", reg_value, reg_length);
            /* Update PUPI buffer and length in the remote device info, 
                PUPI length is 4 bytes */
            if(PHHAL_PUPI_LENGTH == reg_length)
            {
                (void)memcpy((void *)p_tag_b_info->AtqB.AtqResInfo.Pupi, 
                            (void *)reg_value, reg_length);
            }
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case RDR_B_APP_DATA_INDEX:
        {
            HCI_PRINT_BUFFER("\tReader B Application data", reg_value, reg_length);
            /* Update application data buffer and length, 3 bytes, 
                this includes CRC_B and number of application 
             */
            if(PHHAL_APP_DATA_B_LENGTH == reg_length)
            {
                (void)memcpy((void *)p_tag_b_info->AtqB.AtqResInfo.AppData, 
                            (void *)reg_value, reg_length);
            }
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case RDR_B_AFI_INDEX:
        {
            HCI_PRINT_BUFFER("\tReader B AFI", reg_value, reg_length);
            /* Update AFI byte, Only one byte */
            if(sizeof(*reg_value) == reg_length)
            {
                p_tag_b_info->Afi = *reg_value;
            }
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }

        case RDR_B_HIGHER_LAYER_RESP_INDEX:
        {
            HCI_PRINT_BUFFER("\tReader B higher layer response", reg_value, reg_length);
            /* Update higher layer response buffer and length */
            if (reg_length <= PHHAL_MAX_ATR_LENGTH)
            {
                (void)memcpy((void *)p_tag_b_info->HiLayerResp, 
                                (void *)reg_value, reg_length);
            }
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }

        case RDR_B_HIGHER_LAYER_DATA_INDEX:
        {
            HCI_PRINT_BUFFER("\tReader B higher layer data", reg_value, reg_length);
            /* Update higher layer data buffer and length */
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
phHciNfc_Recv_Iso_B_Packet(
                           phHciNfc_sContext_t  *psHciContext,
                           uint8_t              *pResponse,
#ifdef ONE_BYTE_LEN
                           uint8_t             length
#else
                           uint16_t            length
#endif
                           )
{
    NFCSTATUS       status = NFCSTATUS_SUCCESS;
    uint8_t         i = 1;        
    
    psHciContext->rx_index = HCP_HEADER_LEN;
    /* command WR_XCHG_DATA: so give ISO 14443-4B data to the upper layer */
    HCI_PRINT_BUFFER("ISO 14443-4B Bytes received", pResponse, length); 
    if(FALSE != pResponse[(length - i)])
    {
        status = PHNFCSTVAL(CID_NFC_HCI, 
                            NFCSTATUS_RF_ERROR);
    }
    return status;
}


NFCSTATUS
phHciNfc_ReaderB_Set_AFI(
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
    else if((NULL == psHciContext->p_reader_b_info) || 
        (HCI_READER_B_ENABLE != 
        ((phHciNfc_ReaderB_Info_t *)(psHciContext->p_reader_b_info))->
        enable_rdr_b_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_ReaderB_Info_t     *ps_rdr_b_info=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        ps_rdr_b_info = (phHciNfc_ReaderB_Info_t *)
                        psHciContext->p_reader_b_info ;
        p_pipe_info = ps_rdr_b_info->p_pipe_info;
        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            pipeid = ps_rdr_b_info->pipe_id ;
            p_pipe_info->reg_index = RDR_B_AFI_INDEX;
            
            p_pipe_info->param_info = &afi_value;
            p_pipe_info->param_length = sizeof(uint8_t); 
            /* Fill the data buffer and send the command to the 
            device */
            status = 
                phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                pipeid, (uint8_t)ANY_SET_PARAMETER);
        }
    }
    return status;
}

NFCSTATUS
phHciNfc_ReaderB_Set_LayerData(
                        void            *psContext,
                        void            *pHwRef,
                        phNfc_sData_t   *layer_data_info
                        )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t         *psHciContext = 
                                (phHciNfc_sContext_t *)psContext ;

    if( (NULL == psHciContext) || (NULL == pHwRef) || 
        (NULL == layer_data_info) || (NULL == layer_data_info->buffer)
        || (0 == layer_data_info->length))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_reader_b_info) || 
        (HCI_READER_B_ENABLE != 
        ((phHciNfc_ReaderB_Info_t *)(psHciContext->p_reader_b_info))->
        enable_rdr_b_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_ReaderB_Info_t     *ps_rdr_b_info=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        ps_rdr_b_info = (phHciNfc_ReaderB_Info_t *)
                        psHciContext->p_reader_b_info ;
        p_pipe_info = ps_rdr_b_info->p_pipe_info;
        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            p_pipe_info->reg_index = RDR_B_HIGHER_LAYER_DATA_INDEX;
            pipeid = ps_rdr_b_info->pipe_id ;
            p_pipe_info->param_info = (void *)layer_data_info->buffer;
            p_pipe_info->param_length = (uint8_t)
                                        layer_data_info->length;
            /* Fill the data buffer and send the command to the 
            device */
            status = phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                pipeid, (uint8_t)ANY_SET_PARAMETER);
        }
    }
    return status;
}
#endif /* #if defined (TYPE_B) */


