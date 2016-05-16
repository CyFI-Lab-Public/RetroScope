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
* \file  phHciNfc_Jewel.c                                                 *
* \brief HCI Jewel/Topaz Management Routines.                                    *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Mon Mar 29 17:34:47 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.8 $                                                           *
* $Aliases: NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
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

#if defined(TYPE_JEWEL)
#include <phHciNfc_Jewel.h>

/*
****************************** Macro Definitions *******************************
*/
#define JEWEL_SINGLE_TAG_FOUND              0x00U
#define JEWEL_MULTIPLE_TAGS_FOUND           0x03U
#define NXP_WRA_CONTINUE_ACTIVATION         0x12U

#define NXP_JEWEL_READID                    0x78U
#define NXP_JEWEL_READID_LENGTH             0x06U

/*
*************************** Structure and Enumeration ***************************
*/

/*
*************************** Static Function Declaration **************************
*/

static 
NFCSTATUS
phHciNfc_Recv_Jewel_Response(
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
phHciNfc_Recv_Jewel_Event(
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
phHciNfc_Jewel_InfoUpdate(
                            phHciNfc_sContext_t     *psHciContext,
                            uint8_t                 index,
                            uint8_t                 *reg_value,
                            uint8_t                 reg_length
                            );

static
NFCSTATUS
phHciNfc_Recv_Jewel_Packet(
                            phHciNfc_sContext_t  *psHciContext,
                            uint8_t              *pResponse,
#ifdef ONE_BYTE_LEN
                            uint8_t             length
#else
                            uint16_t            length
#endif
                            );


NFCSTATUS
phHciNfc_Jewel_Get_PipeID(
                           phHciNfc_sContext_t     *psHciContext,
                           uint8_t                 *ppipe_id
                           )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL != psHciContext)
        && ( NULL != ppipe_id )
        && ( NULL != psHciContext->p_jewel_info ) 
        )
    {
        phHciNfc_Jewel_Info_t     *ps_jewel_info = NULL;
        ps_jewel_info = (phHciNfc_Jewel_Info_t *)psHciContext->p_jewel_info;
        *ppipe_id =  ps_jewel_info->pipe_id;
    }
    else 
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    return status;
}

NFCSTATUS
phHciNfc_Jewel_Init_Resources(
                               phHciNfc_sContext_t     *psHciContext
                               )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_Jewel_Info_t      *ps_jewel_info = NULL;
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if(
            ( NULL == psHciContext->p_jewel_info ) &&
            (phHciNfc_Allocate_Resource((void **)(&ps_jewel_info),
            sizeof(phHciNfc_Jewel_Info_t))== NFCSTATUS_SUCCESS)
            )
        {
            psHciContext->p_jewel_info = ps_jewel_info;
            ps_jewel_info->current_seq = JEWEL_INVALID_SEQ;
            ps_jewel_info->next_seq = JEWEL_INVALID_SEQ;
            ps_jewel_info->pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INSUFFICIENT_RESOURCES);
        }

    }
    return status;
}

NFCSTATUS
phHciNfc_Jewel_Update_PipeInfo(
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
    else if(NULL == psHciContext->p_jewel_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_Jewel_Info_t      *ps_jewel_info=NULL;
        ps_jewel_info = (phHciNfc_Jewel_Info_t *)psHciContext->p_jewel_info ;

        /* Update the pipe_id of the Jewel Gate obtained from the HCI 
        Response */
        ps_jewel_info->pipe_id = pipeID;
        ps_jewel_info->p_pipe_info = pPipeInfo;
        /* Update the Response Receive routine of the Jewel Gate */
        pPipeInfo->recv_resp = phHciNfc_Recv_Jewel_Response;
        /* Update the event Receive routine of the Jewel Gate */
        pPipeInfo->recv_event = phHciNfc_Recv_Jewel_Event;
    }

    return status;
}


NFCSTATUS
phHciNfc_Jewel_Update_Info(
                             phHciNfc_sContext_t        *psHciContext,
                             uint8_t                    infotype,
                             void                       *jewel_info
                             )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    
    if (NULL == psHciContext)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_jewel_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_Jewel_Info_t     *ps_jewel_info=NULL;
        ps_jewel_info = (phHciNfc_Jewel_Info_t *)
                        psHciContext->p_jewel_info ;

        switch(infotype)
        {
            case HCI_JEWEL_ENABLE:
            {
                if (NULL != jewel_info)
                {
                    ps_jewel_info->enable_jewel_gate =
                                        *((uint8_t *)jewel_info);
                }
                break;
            }
            case HCI_JEWEL_INFO_SEQ:
            {
                ps_jewel_info->current_seq = JEWEL_READID_SEQUENCE;
                ps_jewel_info->next_seq = JEWEL_READID_SEQUENCE;
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
phHciNfc_Jewel_Info_Sequence (
                               void             *psHciHandle,
                               void             *pHwRef
                               )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t     *psHciContext = 
                            ((phHciNfc_sContext_t *)psHciHandle);
    static uint8_t          paraminfo[NXP_JEWEL_READID_LENGTH + 1] = {0};

    if( (NULL == psHciContext) 
        || (NULL == pHwRef)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_jewel_info) || 
        (HCI_JEWEL_ENABLE != 
        ((phHciNfc_Jewel_Info_t *)(psHciContext->p_jewel_info))->
        enable_jewel_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }   
    else
    {
        phHciNfc_Jewel_Info_t      *ps_jewel_info=NULL;
        phHciNfc_Pipe_Info_t        *ps_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        ps_jewel_info = (phHciNfc_Jewel_Info_t *)
                        psHciContext->p_jewel_info ;
        ps_pipe_info = ps_jewel_info->p_pipe_info;
        if(NULL == ps_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            switch(ps_jewel_info->current_seq)
            {
                case JEWEL_READID_SEQUENCE:
                {
                    pipeid = ps_pipe_info->pipe.pipe_id;
                    ps_pipe_info->reg_index = NXP_JEWEL_READID;
                    paraminfo[0] = NXP_JEWEL_READID;

                    ps_pipe_info->param_info = (void *)&paraminfo;
                    ps_pipe_info->param_length = NXP_JEWEL_READID_LENGTH + 1;

                    status = phHciNfc_Send_Jewel_Command(psHciContext, 
                                            pHwRef, pipeid, 
                                            NXP_JEWEL_RAW);

                    if(NFCSTATUS_PENDING == status )
                    {
                        ps_jewel_info->next_seq = JEWEL_END_SEQUENCE;
                    }
                    break;
                }
                case JEWEL_END_SEQUENCE:
                {
                    phNfc_sCompletionInfo_t     CompInfo;
                    
                    ps_pipe_info->reg_index = JEWEL_END_SEQUENCE;
                    if (JEWEL_MULTIPLE_TAGS_FOUND == 
                        ps_jewel_info->multiple_tgts_found)
                    {
                        CompInfo.status = NFCSTATUS_MULTIPLE_TAGS;
                    }
                    else
                    {
                        CompInfo.status = NFCSTATUS_SUCCESS;
                    }

                    CompInfo.info = &(ps_jewel_info->s_jewel_info);

                    ps_jewel_info->s_jewel_info.RemDevType = phHal_eJewel_PICC;
                    ps_jewel_info->current_seq = JEWEL_READID_SEQUENCE;
                    ps_jewel_info->next_seq = JEWEL_READID_SEQUENCE;
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
phHciNfc_Jewel_InfoUpdate(
                            phHciNfc_sContext_t     *psHciContext, 
                            uint8_t                 index,
                            uint8_t                 *reg_value,
                            uint8_t                 reg_length
                            )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_Jewel_Info_t       *ps_jewel_info = NULL;
    phHal_sJewelInfo_t          *ps_jewel_tag_info = NULL;

    ps_jewel_info = (phHciNfc_Jewel_Info_t *)(psHciContext->p_jewel_info);
    ps_jewel_tag_info = &(ps_jewel_info->s_jewel_info.RemoteDevInfo.Jewel_Info);

    switch(index)
    {
        case NXP_JEWEL_READID:
        {
            HCI_PRINT_BUFFER("\tJewel ID", reg_value, reg_length);
            if(NXP_JEWEL_READID_LENGTH == reg_length)
            {
                uint8_t     i = 0;
                ps_jewel_tag_info->HeaderRom0 = reg_value[i++];
                ps_jewel_tag_info->HeaderRom1 = reg_value[i++];
                (void)memcpy(ps_jewel_tag_info->Uid, 
                            &(reg_value[i]), 
                            (reg_length - i));

                ps_jewel_tag_info->UidLength = (reg_length - i);
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
phHciNfc_Recv_Jewel_Packet(
                            phHciNfc_sContext_t  *psHciContext,
                            uint8_t              *pResponse,
#ifdef ONE_BYTE_LEN
                            uint8_t            length
#else
                            uint16_t           length
#endif
                            )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_Jewel_Info_t       *ps_jewel_info = (phHciNfc_Jewel_Info_t *)
                                (psHciContext->p_jewel_info);

    if (NXP_JEWEL_READID == ps_jewel_info->p_pipe_info->reg_index)
    {
        status = phHciNfc_Jewel_InfoUpdate(psHciContext, 
                            ps_jewel_info->p_pipe_info->reg_index, 
                            pResponse, (uint8_t)length);
    }
    else
    {
        /* Send Jewel data to the upper layer */
        HCI_PRINT_BUFFER("Jewel Bytes received", pResponse, length);
        psHciContext->rx_index = HCP_HEADER_LEN;
    }
    return status;
}


static 
NFCSTATUS
phHciNfc_Recv_Jewel_Response(
                               void                *psContext,
                               void                *pHwRef,
                               uint8_t             *pResponse,
#ifdef ONE_BYTE_LEN
                               uint8_t            length
#else
                               uint16_t           length
#endif
                               )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t         *psHciContext = 
                                (phHciNfc_sContext_t *)psContext;


    if( (NULL == psHciContext) || (NULL == pHwRef) || (NULL == pResponse)
        || (0 == length))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_jewel_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_Jewel_Info_t       *ps_jewel_info=NULL;
        uint8_t                     prev_cmd = ANY_GET_PARAMETER;
        ps_jewel_info = (phHciNfc_Jewel_Info_t *)
                        psHciContext->p_jewel_info ;
        if( NULL == ps_jewel_info->p_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            prev_cmd = ps_jewel_info->p_pipe_info->prev_msg ;
            switch(prev_cmd)
            {
                case ANY_GET_PARAMETER:
                {
                    if (length >= HCP_HEADER_LEN)
                    {
                        status = phHciNfc_Jewel_InfoUpdate(psHciContext, 
                                            ps_jewel_info->p_pipe_info->reg_index, 
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
                    HCI_PRINT("Jewel Parameter Set \n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                                UPDATE_SEQ);
                    ps_jewel_info->next_seq = JEWEL_READID_SEQUENCE;
                    break;
                }
                case ANY_OPEN_PIPE:
                {
                    HCI_PRINT("Jewel open pipe complete\n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                                UPDATE_SEQ);
                    ps_jewel_info->next_seq = JEWEL_READID_SEQUENCE;
                    break;
                }
                case ANY_CLOSE_PIPE:
                {
                    HCI_PRINT("Jewel close pipe complete\n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                                UPDATE_SEQ);
                    break;
                }
                case NXP_JEWEL_RAW:
                {
                    HCI_PRINT("Jewel packet received \n");
                    if (length >= HCP_HEADER_LEN)
                    {
                        phHciNfc_Append_HCPFrame(psHciContext->recv_buffer, 
                                                    0, pResponse, length);
                        psHciContext->rx_total = length;
                        status = phHciNfc_Recv_Jewel_Packet(psHciContext,
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
                case NXP_WRA_CONTINUE_ACTIVATION:
                case NXP_WR_ACTIVATE_ID:
                {
                    HCI_PRINT("Jewel continue activation or ");
                    HCI_PRINT("reactivation completed \n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                                UPDATE_SEQ);
                    break;
                }
                case NXP_WR_PRESCHECK:
                {
                    HCI_PRINT("Presence check completed \n");
                    break;
                }
                case NXP_WR_ACTIVATE_NEXT:
                {
                    HCI_PRINT("Activate next completed \n");
                    if (length > HCP_HEADER_LEN)
                    {
                        if (JEWEL_MULTIPLE_TAGS_FOUND == 
                            pResponse[HCP_HEADER_LEN])
                        {
                            ps_jewel_info->multiple_tgts_found = 
                                            JEWEL_MULTIPLE_TAGS_FOUND;
                        }
                        else
                        {
                            ps_jewel_info->multiple_tgts_found = FALSE;
                        }
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
                            /* Optional error code, if no error code field 
                                in the response, then this command is 
                                successfully completed */
                            ps_jewel_info->uicc_activation = 
                                        UICC_CARD_ACTIVATION_SUCCESS;
                            break;
                        }
                        case (HCP_HEADER_LEN + 1):
                        {
                            ps_jewel_info->uicc_activation = 
                                        pResponse[HCP_HEADER_LEN];
                            break;
                        } /* End of case (HCP_HEADER_LEN + index) */
                        default:
                        {
                            status = PHNFCSTVAL(CID_NFC_HCI, 
                                                NFCSTATUS_INVALID_HCI_RESPONSE);
                            break;
                        }
                    }
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
                ps_jewel_info->p_pipe_info->prev_status = NFCSTATUS_SUCCESS;
                ps_jewel_info->current_seq = ps_jewel_info->next_seq;
            }
        }
    }
    return status;
}

static
NFCSTATUS
phHciNfc_Recv_Jewel_Event(
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
        || (0 == length))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_jewel_info) || 
        (HCI_JEWEL_ENABLE != 
        ((phHciNfc_Jewel_Info_t *)(psHciContext->p_jewel_info))->
        enable_jewel_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_HCP_Packet_t       *p_packet = NULL;
        phHciNfc_Jewel_Info_t       *ps_jewel_info = NULL;
        phHciNfc_HCP_Message_t      *message = NULL;
        uint8_t                     instruction=0, 
                                    i = 0;

        ps_jewel_info = (phHciNfc_Jewel_Info_t *)
                                psHciContext->p_jewel_info ;
        p_packet = (phHciNfc_HCP_Packet_t *)pEvent;
        message = &p_packet->msg.message;
        /* Get the instruction bits from the Message Header */
        instruction = (uint8_t) GET_BITS8( message->msg_header,
                    HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);

        if ((EVT_TARGET_DISCOVERED == instruction) 
            && ((JEWEL_MULTIPLE_TAGS_FOUND == message->payload[i] ) 
            || (JEWEL_SINGLE_TAG_FOUND == message->payload[i])) 
            )
        {
            static phNfc_sCompletionInfo_t      pCompInfo;

            if (JEWEL_MULTIPLE_TAGS_FOUND == message->payload[i])
            {
                ps_jewel_info->multiple_tgts_found = 
                                        JEWEL_MULTIPLE_TAGS_FOUND;
                pCompInfo.status = NFCSTATUS_MULTIPLE_TAGS;
            }
            else
            {
                ps_jewel_info->multiple_tgts_found = FALSE;
                pCompInfo.status = NFCSTATUS_SUCCESS;
            }

            psHciContext->host_rf_type = phHal_eJewel_PCD;
            ps_jewel_info->s_jewel_info.RemDevType = phHal_eJewel_PICC;
            ps_jewel_info->current_seq = JEWEL_READID_SEQUENCE;

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
phHciNfc_Send_Jewel_Command(
                              phHciNfc_sContext_t   *psContext,
                              void                  *pHwRef,
                              uint8_t               pipe_id,
                              uint8_t               cmd
                              )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t         *psHciContext = 
                                (phHciNfc_sContext_t *)psContext ;
    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_jewel_info) || 
        (HCI_JEWEL_ENABLE != 
        ((phHciNfc_Jewel_Info_t *)(psHciContext->p_jewel_info))->
        enable_jewel_gate) || 
        (HCI_UNKNOWN_PIPE_ID == 
        ((phHciNfc_Jewel_Info_t *)(psHciContext->p_jewel_info))->
        pipe_id) || 
        (pipe_id != 
        ((phHciNfc_Jewel_Info_t *)(psHciContext->p_jewel_info))->
        pipe_id))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_Jewel_Info_t       *ps_jewel_info=NULL;
        phHciNfc_Pipe_Info_t        *ps_pipe_info=NULL;
        phHciNfc_HCP_Packet_t       *hcp_packet = NULL;
        phHciNfc_HCP_Message_t      *hcp_message = NULL;
        uint8_t                     i = 0, 
                                    length = HCP_HEADER_LEN;

        ps_jewel_info = (phHciNfc_Jewel_Info_t *)
                            psHciContext->p_jewel_info ;
        ps_pipe_info = ps_jewel_info->p_pipe_info;
        if(NULL == ps_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            psHciContext->tx_total = 0 ;
            hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;

            if (NXP_JEWEL_RAW == cmd)
            {
                /* Construct the HCP Frame */
                phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                                (uint8_t) pipe_id, HCP_MSG_TYPE_COMMAND, cmd);
                hcp_message = &(hcp_packet->msg.message);
                phHciNfc_Append_HCPFrame((uint8_t *)hcp_message->payload,
                                    i, (uint8_t *)ps_pipe_info->param_info,
                                    ps_pipe_info->param_length);
                length =(uint8_t)(length + i + ps_pipe_info->param_length);
            }
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_COMMAND);
            }

            if (NFCSTATUS_SUCCESS == status)
            {
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
phHciNfc_Jewel_GetRID(
                phHciNfc_sContext_t   *psHciContext,
                void                  *pHwRef)
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    static uint8_t              reader_id_info[NXP_JEWEL_READID_LENGTH] = {0};

    if( (NULL == psHciContext) || (NULL == pHwRef))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_jewel_info) || 
        (HCI_JEWEL_ENABLE != 
        ((phHciNfc_Jewel_Info_t *)(psHciContext->p_jewel_info))->
        enable_jewel_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_Jewel_Info_t      *ps_jewel_info=NULL;
        phHciNfc_Pipe_Info_t        *ps_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        ps_jewel_info = (phHciNfc_Jewel_Info_t *)
                        psHciContext->p_jewel_info ;

        ps_pipe_info = ps_jewel_info->p_pipe_info;
        if(NULL == ps_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            pipeid = ps_jewel_info->pipe_id ;
            reader_id_info[0] = NXP_JEWEL_READID;

            ps_pipe_info->param_info = (void *)&reader_id_info;
            ps_pipe_info->param_length = NXP_JEWEL_READID_LENGTH + 1 ;

            status = phHciNfc_Send_Jewel_Command(psHciContext, 
                                    pHwRef, pipeid, 
                                    NXP_JEWEL_RAW);
        }
    }
    return status;
}

#endif /* #if defined(TYPE_JEWEL) */


