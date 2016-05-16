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
* \file  phHciNfc_RFReaderA.c                                                 *
* \brief HCI Reader A Management Routines.                                    *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Wed Feb 17 16:19:04 2010 $                                           *
* $Author: ing02260 $                                                         *
* $Revision: 1.57 $                                                           *
* $Aliases: NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
*                                                                             *
* =========================================================================== *
*/

/*
***************************** Header File Inclusion ****************************
*/
#include <phNfcCompId.h>
#include <phHciNfc_Pipe.h>
#include <phHciNfc_RFReader.h>
#include <phHciNfc_RFReaderA.h>
#include <phOsalNfc.h>
/*
****************************** Macro Definitions *******************************
*/

/* Registry used for getting the data */
#define RDR_A_DATA_RATE_MAX_INDEX           0x01U
#define RDR_A_UID_INDEX                     0x02U
#define RDR_A_SAK_INDEX                     0x03U
#define RDR_A_ATQA_INDEX                    0x04U
#define RDR_A_APP_DATA_INDEX                0x05U
#define RDR_A_FWI_SFGT_INDEX                0x06U

/* Registry index for auto activation */
#define NXP_AUTO_ACTIVATION_INDEX           0x10U

#define RDR_A_SAK_FWI_SFGT_LENGTH           0x01U

#define RDR_A_SINGLE_TAG_FOUND              0x00U
#define RDR_A_MULTIPLE_TAGS_FOUND           0x03U

#define RDR_A_MAX_APP_DATA_LEN              0x30U

/* Time out */
#define RDR_A_MIFARE_STATUS                 0x00U

#define RDR_A_MIFARE_RAW_LENGTH             0x03U

uint8_t nxp_nfc_mifareraw_timeout = NXP_MIFARE_XCHG_TIMEOUT;
/*
*************************** Structure and Enumeration ***************************
*/


/*
*************************** Static Function Declaration **************************
*/

static 
NFCSTATUS
phHciNfc_Recv_ReaderA_Response(
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
phHciNfc_Recv_ReaderA_Event(
                             void               *psContext,
                             void               *pHwRef,
                             uint8_t            *pEvent,
#ifdef ONE_BYTE_LEN
                             uint8_t            length
#else
                             uint16_t           length
#endif
                       );

static
NFCSTATUS
phHciNfc_ReaderA_InfoUpdate(
                            phHciNfc_sContext_t     *psHciContext,
                            uint8_t                 index,
                            uint8_t                 *reg_value,
                            uint8_t                 reg_length
                         );

static
NFCSTATUS
phHciNfc_Recv_Mifare_Packet(
                            phHciNfc_sContext_t *psHciContext,
                            uint8_t             cmd,
                            uint8_t             *pResponse,
#ifdef ONE_BYTE_LEN
                            uint8_t             length
#else
                            uint16_t            length
#endif
                            );

static
NFCSTATUS
phHciNfc_Recv_Iso_A_Packet(
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
phHciNfc_ReaderA_Get_PipeID(
                             phHciNfc_sContext_t        *psHciContext,
                             uint8_t                    *ppipe_id
                             )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL != psHciContext)
        && ( NULL != ppipe_id )
        && ( NULL != psHciContext->p_reader_a_info ) 
        )
    {
        phHciNfc_ReaderA_Info_t     *p_rdr_a_info=NULL;
        p_rdr_a_info = (phHciNfc_ReaderA_Info_t *)
            psHciContext->p_reader_a_info ;
        *ppipe_id =  p_rdr_a_info->pipe_id  ;
    }
    else 
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    return status;
}


NFCSTATUS
phHciNfc_ReaderA_Init_Resources(
                                phHciNfc_sContext_t     *psHciContext
                         )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_ReaderA_Info_t     *p_rdr_a_info=NULL;
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if(
            ( NULL == psHciContext->p_reader_a_info ) &&
             (phHciNfc_Allocate_Resource((void **)(&p_rdr_a_info),
            sizeof(phHciNfc_ReaderA_Info_t))== NFCSTATUS_SUCCESS)
          )
        {
            psHciContext->p_reader_a_info = p_rdr_a_info;
            p_rdr_a_info->current_seq = RDR_A_INVALID_SEQ;
            p_rdr_a_info->next_seq = RDR_A_INVALID_SEQ;
            p_rdr_a_info->pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INSUFFICIENT_RESOURCES);
        }
        
    }
    return status;
}

NFCSTATUS
phHciNfc_ReaderA_Update_PipeInfo(
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
    else if(NULL == psHciContext->p_reader_a_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_ReaderA_Info_t *p_rdr_a_info=NULL;
        p_rdr_a_info = (phHciNfc_ReaderA_Info_t *)
                                psHciContext->p_reader_a_info ;
        /* Update the pipe_id of the reader A Gate obtained from the HCI Response */
        p_rdr_a_info->pipe_id = pipeID;
        p_rdr_a_info->p_pipe_info = pPipeInfo;
        if (NULL != pPipeInfo)
        {
            /* Update the Response Receive routine of the reader A Gate */
            pPipeInfo->recv_resp = &phHciNfc_Recv_ReaderA_Response;
            /* Update the event Receive routine of the reader A Gate */
            pPipeInfo->recv_event = &phHciNfc_Recv_ReaderA_Event;
        }
    }

    return status;
}

NFCSTATUS
phHciNfc_ReaderA_Info_Sequence (
                       void             *psHciHandle,
                       void             *pHwRef
                       )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t     *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);

    HCI_PRINT ("HCI : phHciNfc_ReaderA_Info_Sequence called... \n");
    if( (NULL == psHciContext) 
        || (NULL == pHwRef)
      )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_reader_a_info) || 
        (HCI_READER_A_ENABLE != 
        ((phHciNfc_ReaderA_Info_t *)(psHciContext->p_reader_a_info))->
        enable_rdr_a_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }   
    else
    {
        phHciNfc_ReaderA_Info_t     *p_rdr_a_info=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;
        
        p_rdr_a_info = (phHciNfc_ReaderA_Info_t *)
                                psHciContext->p_reader_a_info ;
        p_pipe_info = p_rdr_a_info->p_pipe_info;
        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                            NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            HCI_DEBUG ("HCI : p_rdr_a_info->current_seq : %02X\n", p_rdr_a_info->current_seq);
            switch(p_rdr_a_info->current_seq)
            {
                case RDR_A_UID:
                {
                    p_pipe_info->reg_index = RDR_A_UID_INDEX;
                    pipeid = p_rdr_a_info->pipe_id ;
                    /* Fill the data buffer and send the command to the 
                            device */
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                        pipeid, (uint8_t)ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_rdr_a_info->next_seq = RDR_A_SAK;
                    }
                    break;
                }
                case RDR_A_SAK:
                {
                    p_pipe_info->reg_index = RDR_A_SAK_INDEX;
                    pipeid = p_rdr_a_info->pipe_id ;
                    /* Fill the data buffer and send the command to the 
                            device */
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                        pipeid, (uint8_t)ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_rdr_a_info->next_seq = RDR_A_ATQA;
                    }
                    break;
                }
                case RDR_A_ATQA:
                {
                    p_pipe_info->reg_index = RDR_A_ATQA_INDEX;
                    pipeid = p_rdr_a_info->pipe_id ;
                    /* Fill the data buffer and send the command to the 
                            device */
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                        pipeid, (uint8_t)ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_rdr_a_info->next_seq = RDR_A_END_SEQUENCE;
                    }
                    break;
                }
                case RDR_A_END_SEQUENCE:
                {
                    phNfc_sCompletionInfo_t     CompInfo;
                    if (RDR_A_MULTIPLE_TAGS_FOUND == 
                        p_rdr_a_info->multiple_tgts_found)
                    {
                        CompInfo.status = NFCSTATUS_MULTIPLE_TAGS;
                    }
                    else
                    {
                        CompInfo.status = NFCSTATUS_SUCCESS;
                    }
                    
                    CompInfo.info = &(p_rdr_a_info->reader_a_info);

                    p_rdr_a_info->reader_a_info.RemDevType = phHal_eISO14443_A_PICC;
                    p_rdr_a_info->current_seq = RDR_A_UID;
                    p_rdr_a_info->next_seq = RDR_A_UID;
                    status = NFCSTATUS_SUCCESS;
                    HCI_DEBUG ("HCI : p_rdr_a_info->reader_a_info.RemDevType : %02X\n", p_rdr_a_info->reader_a_info.RemDevType);
                    HCI_DEBUG ("HCI : status notified: %02X\n", CompInfo.status);
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
            HCI_DEBUG ("HCI : p_rdr_a_info->current_seq after : %02X\n", p_rdr_a_info->current_seq);
            HCI_DEBUG ("HCI : p_rdr_a_info->next_seq : %02X\n", p_rdr_a_info->next_seq);
        }
    }

    HCI_PRINT ("HCI : phHciNfc_ReaderA_Info_Sequence end \n");
    return status;
}


static
NFCSTATUS
phHciNfc_ReaderA_InfoUpdate(
                                phHciNfc_sContext_t     *psHciContext,
                                uint8_t                 index,
                                uint8_t                 *reg_value,
                                uint8_t                 reg_length
                         )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_ReaderA_Info_t     *p_rdr_a_info=NULL;
    phHal_sIso14443AInfo_t        *p_tag_a_info = NULL;
                                
    p_rdr_a_info = (phHciNfc_ReaderA_Info_t *)
                                (psHciContext->p_reader_a_info );
    p_tag_a_info = &(p_rdr_a_info->reader_a_info.RemoteDevInfo.Iso14443A_Info);
    
    switch(index)
    {
        case RDR_A_UID_INDEX:
        {
            /* Maximum UID length can go upto 10 bytes */
            if (reg_length <= PHHAL_MAX_UID_LENGTH)
            {
                HCI_PRINT_BUFFER("\tReader A UID", reg_value, reg_length);
                /* Update UID buffer and length */
                p_tag_a_info->UidLength = reg_length;
                (void)memcpy(
                        p_tag_a_info->Uid, 
                        reg_value, 
                        p_tag_a_info->UidLength);
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case RDR_A_SAK_INDEX:
        {
            /* SAK length is 1 byte */
            if (RDR_A_SAK_FWI_SFGT_LENGTH == reg_length)
            {
                HCI_PRINT_BUFFER("\tReader A SAK", reg_value, reg_length);
                /* Copy SAK byte */
                p_tag_a_info->Sak = *reg_value;
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case RDR_A_ATQA_INDEX:
        {
            /* ATQA length shall be 2 bytes */
            if (PHHAL_ATQA_LENGTH == reg_length)
            {
                HCI_PRINT_BUFFER("\tReader A ATQA", reg_value, reg_length);
                /* Copy ATQA */
                (void)memcpy(p_tag_a_info->AtqA,
                    reg_value, 
                    reg_length);
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case RDR_A_APP_DATA_INDEX:
        {
            /* Remote device info provided by the user */
            p_tag_a_info = 
                    &(psHciContext->p_target_info->RemoteDevInfo.Iso14443A_Info);
            /* Historical bytes length shall be 2 bytes */
            if (reg_length <= RDR_A_MAX_APP_DATA_LEN)
            {
                HCI_PRINT_BUFFER("\tReader A APP DATA", reg_value, reg_length);
                p_tag_a_info->AppDataLength = reg_length;
                /* Historical bytes */
                (void)memcpy(p_tag_a_info->AppData, 
                    reg_value, 
                    reg_length);
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case RDR_A_FWI_SFGT_INDEX:
        {
            if (RDR_A_SAK_FWI_SFGT_LENGTH == reg_length)
            {
                HCI_PRINT_BUFFER("\tReader A FWI SFGT", reg_value, reg_length);
                p_tag_a_info->Fwi_Sfgt = *reg_value;
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
phHciNfc_Recv_ReaderA_Response(
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
        || (length == 0))
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_reader_a_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_ReaderA_Info_t     *p_rdr_a_info=NULL;
        uint8_t                     prev_cmd = ANY_GET_PARAMETER;
        p_rdr_a_info = (phHciNfc_ReaderA_Info_t *)
                            psHciContext->p_reader_a_info ;
        if( NULL == p_rdr_a_info->p_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                            NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            prev_cmd = p_rdr_a_info->p_pipe_info->prev_msg ;
            switch(prev_cmd)
            {
                case ANY_GET_PARAMETER:
                {
                    status = phHciNfc_ReaderA_InfoUpdate(psHciContext,
                                    p_rdr_a_info->p_pipe_info->reg_index, 
                                    &pResponse[HCP_HEADER_LEN],
                                    (uint8_t)(length - HCP_HEADER_LEN));
#if 0
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                                UPDATE_SEQ);
#endif
                    break;
                }
                case ANY_SET_PARAMETER:
                {
                    HCI_PRINT("Reader A Parameter Set \n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                    UPDATE_SEQ);
                    p_rdr_a_info->next_seq = RDR_A_UID;
                    break;
                }
                case ANY_OPEN_PIPE:
                {
                    HCI_PRINT("Reader A open pipe complete\n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                    UPDATE_SEQ);
                    p_rdr_a_info->next_seq = RDR_A_UID;
                    break;
                }
                case ANY_CLOSE_PIPE:
                {
                    HCI_PRINT("Reader A close pipe complete\n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                    UPDATE_SEQ);
                    break;
                }
                case NXP_WRA_CONTINUE_ACTIVATION:
                case NXP_WR_ACTIVATE_ID:
                {
                    HCI_PRINT("Reader A continue activation or ");
                    HCI_PRINT("reactivation completed \n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                    UPDATE_SEQ);
                    break;
                }
                case NXP_MIFARE_RAW:
                case NXP_MIFARE_CMD:
                {
                    if (length > HCP_HEADER_LEN)
                    {
                        HCI_PRINT("Mifare packet received \n");
                        /* Copy buffer to the receive buffer */
                        phHciNfc_Append_HCPFrame(psHciContext->recv_buffer, 
                            0, pResponse, length);
                        psHciContext->rx_total = length;
                        status = phHciNfc_Recv_Mifare_Packet(psHciContext, 
                                                prev_cmd, 
                                                &pResponse[HCP_HEADER_LEN],
                                                (length - HCP_HEADER_LEN));

                    } 
                    else if (length == HCP_HEADER_LEN)
                    {
                        psHciContext->rx_total = length;
                        psHciContext->rx_index = HCP_HEADER_LEN;

                    }
                    else
                    {
                        status = PHNFCSTVAL(CID_NFC_HCI, 
                                        NFCSTATUS_INVALID_HCI_RESPONSE);
                    }
                    break;
                }
                case WR_XCHGDATA:
                {
                    if (length >= HCP_HEADER_LEN)
                    {
                        uint8_t         i = 1;
                        HCI_PRINT("ISO 14443-4A received \n");
                        /* Copy buffer to the receive buffer */
                        phHciNfc_Append_HCPFrame(psHciContext->recv_buffer, 
                            0, pResponse, (length - i));
                        psHciContext->rx_total = (length - i);
                        status = phHciNfc_Recv_Iso_A_Packet(psHciContext, 
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
                        if (RDR_A_MULTIPLE_TAGS_FOUND == pResponse[HCP_HEADER_LEN])
                        {
                            p_rdr_a_info->multiple_tgts_found = 
                                RDR_A_MULTIPLE_TAGS_FOUND;
                        }
                        else
                        {
                            p_rdr_a_info->multiple_tgts_found = FALSE;
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
                            /* Optional error code, if no error code field 
                                in the response, then this command is 
                                successfully completed */
                            p_rdr_a_info->uicc_activation = 
                                    UICC_CARD_ACTIVATION_SUCCESS;
                            break;
                        }
                        case (HCP_HEADER_LEN + 1):
                        {
                            p_rdr_a_info->uicc_activation = 
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
            } /* End of switch(prev_cmd) */
            if( NFCSTATUS_SUCCESS == status )
            {
                p_rdr_a_info->p_pipe_info->prev_status = NFCSTATUS_SUCCESS;
                p_rdr_a_info->current_seq = p_rdr_a_info->next_seq;
            }
        }
    }
    return status;
}

static
NFCSTATUS
phHciNfc_Recv_Iso_A_Packet(
                            phHciNfc_sContext_t *psHciContext,
                            uint8_t             *pResponse,
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
    /* command WRA_XCHG_DATA: so give ISO 14443-4A data to the upper layer */
    if(FALSE != pResponse[(length - i)])
    {
        status = PHNFCSTVAL(CID_NFC_HCI, 
                            NFCSTATUS_RF_ERROR);
    }
    HCI_PRINT_BUFFER("ISO 14443- 4A Bytes received", pResponse, length);
    
    return status;
}

static
NFCSTATUS
phHciNfc_Recv_Mifare_Packet(
                           phHciNfc_sContext_t  *psHciContext,
                           uint8_t              cmd,
                           uint8_t              *pResponse,
#ifdef ONE_BYTE_LEN
                           uint8_t              length
#else
                           uint16_t             length
#endif
                           )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    /* To remove "warning (VS C4100) : unreferenced formal parameter" */
    PHNFC_UNUSED_VARIABLE(pResponse);
    PHNFC_UNUSED_VARIABLE(length);
    
    if (NXP_MIFARE_RAW == cmd)
    {
#ifdef ENABLE_MIFARE_RAW
        uint8_t                 index = 0;
#ifndef HAL_SW_3A_STATUS
        if(phHal_eISO14443_3A_PICC == psHciContext->p_target_info->RemDevType)
        {
            index++;
            psHciContext->rx_index = (index + HCP_HEADER_LEN);
            HCI_PRINT_BUFFER("Mifare Bytes received", &pResponse[index], (length - index));
        }
        else
#endif
        if (RDR_A_MIFARE_STATUS == pResponse[index])  /* Status byte */
        {
            index++;
            psHciContext->rx_index = (index + HCP_HEADER_LEN);
            HCI_PRINT_BUFFER("Mifare Bytes received", &pResponse[index], (length - index));
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_RESPONSE);
        }
#else
        psHciContext->rx_index = HCP_HEADER_LEN;
        /* Give Mifare data to the upper layer */
        HCI_PRINT_BUFFER("Mifare Bytes received", pResponse, length);        
#endif /* #ifdef ENABLE_MIFARE_RAW */
    } 
    else
    {
        psHciContext->rx_index = HCP_HEADER_LEN;
        /* command NXP_MIFARE_CMD: so give Mifare data to the upper layer */
        HCI_PRINT_BUFFER("Mifare Bytes received", pResponse, length);
    }
    
    return status;
}

static
NFCSTATUS
phHciNfc_Recv_ReaderA_Event(
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

    HCI_PRINT ("HCI : phHciNfc_Recv_ReaderA_Event called...\n");
    if( (NULL == psHciContext) || (NULL == pHwRef) || (NULL == pEvent)
        || (length == 0))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_reader_a_info) || 
        (HCI_READER_A_ENABLE != 
        ((phHciNfc_ReaderA_Info_t *)(psHciContext->p_reader_a_info))->
        enable_rdr_a_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_HCP_Packet_t       *p_packet = NULL;
        phHciNfc_ReaderA_Info_t     *p_rdr_a_info=NULL;
        phHciNfc_HCP_Message_t      *message = NULL;
        uint8_t                     instruction=0, 
                                    i = 0;

        p_rdr_a_info = (phHciNfc_ReaderA_Info_t *)
                        psHciContext->p_reader_a_info ;
        p_packet = (phHciNfc_HCP_Packet_t *)pEvent;
        message = &p_packet->msg.message;
        /* Get the instruction bits from the Message Header */
        instruction = (uint8_t) GET_BITS8( message->msg_header,
            HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);

        HCI_DEBUG ("HCI : instruction : %02X\n", instruction);
        HCI_DEBUG ("HCI : Multiple tag found : %02X\n", message->payload[i]);

        if ((EVT_TARGET_DISCOVERED == instruction) 
            && ((RDR_A_MULTIPLE_TAGS_FOUND == message->payload[i] ) 
                || (RDR_A_SINGLE_TAG_FOUND == message->payload[i])) 
          )
        {
            phNfc_sCompletionInfo_t pCompInfo;

            if (RDR_A_MULTIPLE_TAGS_FOUND == message->payload[i])
            {
                p_rdr_a_info->multiple_tgts_found = RDR_A_MULTIPLE_TAGS_FOUND;
                pCompInfo.status = NFCSTATUS_MULTIPLE_TAGS;
            }
            else
            {
                p_rdr_a_info->multiple_tgts_found = FALSE;
                pCompInfo.status = NFCSTATUS_SUCCESS;
            }
            
            psHciContext->host_rf_type = phHal_eISO14443_A_PCD;
            p_rdr_a_info->reader_a_info.RemDevType = phHal_eISO14443_A_PICC;
            p_rdr_a_info->current_seq = RDR_A_UID;

            HCI_DEBUG ("HCI : psHciContext->host_rf_type : %02X\n", psHciContext->host_rf_type);
            HCI_DEBUG ("HCI : p_rdr_a_info->reader_a_info.RemDevType : %02X\n", p_rdr_a_info->reader_a_info.RemDevType);
            HCI_DEBUG ("HCI : p_rdr_a_info->current_seq : %02X\n", p_rdr_a_info->current_seq);

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
    HCI_PRINT ("HCI : phHciNfc_Recv_ReaderA_Event end\n");
    return status;
}

NFCSTATUS
phHciNfc_ReaderA_Auto_Activate(
                               void         *psContext,
                               void         *pHwRef,
                               uint8_t      activate_enable
                               )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t         *psHciContext = 
                                (phHciNfc_sContext_t *)psContext ;
    if( (NULL == psHciContext) || (NULL == pHwRef))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_reader_a_info) || 
        (HCI_READER_A_ENABLE != 
        ((phHciNfc_ReaderA_Info_t *)(psHciContext->p_reader_a_info))->
        enable_rdr_a_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_ReaderA_Info_t     *p_rdr_a_info=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        p_rdr_a_info = (phHciNfc_ReaderA_Info_t *)
                                psHciContext->p_reader_a_info ;
        p_pipe_info = p_rdr_a_info->p_pipe_info;
        p_pipe_info->reg_index = NXP_AUTO_ACTIVATION_INDEX;
        p_pipe_info->param_info = &activate_enable;
        p_pipe_info->param_length = sizeof(activate_enable);
        pipeid = p_rdr_a_info->pipe_id ;
        status = phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                pipeid, (uint8_t)ANY_SET_PARAMETER);
        if(NFCSTATUS_PENDING == status )
        {
            status = NFCSTATUS_SUCCESS;
        }
    }
    return status;
}

NFCSTATUS
phHciNfc_ReaderA_Set_DataRateMax(
                               void         *psContext,
                               void         *pHwRef,
                               uint8_t      data_rate_value
                               )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t         *psHciContext = 
        (phHciNfc_sContext_t *)psContext ;
    if( (NULL == psHciContext) || (NULL == pHwRef))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_reader_a_info) || 
        (HCI_READER_A_ENABLE != 
        ((phHciNfc_ReaderA_Info_t *)(psHciContext->p_reader_a_info))->
        enable_rdr_a_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_ReaderA_Info_t     *p_rdr_a_info=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        p_rdr_a_info = (phHciNfc_ReaderA_Info_t *)
                            psHciContext->p_reader_a_info ;
        p_pipe_info = p_rdr_a_info->p_pipe_info;
        p_pipe_info->reg_index = RDR_A_DATA_RATE_MAX_INDEX;
        p_pipe_info->param_info = &data_rate_value;
        p_pipe_info->param_length = sizeof(data_rate_value);
        pipeid = p_rdr_a_info->pipe_id ;
        status = phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                    pipeid, (uint8_t)ANY_SET_PARAMETER);
    }
    return status;
}


NFCSTATUS
phHciNfc_Send_ReaderA_Command(
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
    else if((NULL == psHciContext->p_reader_a_info) || 
        (HCI_READER_A_ENABLE != 
        ((phHciNfc_ReaderA_Info_t *)(psHciContext->p_reader_a_info))->
        enable_rdr_a_gate) || 
        (HCI_UNKNOWN_PIPE_ID == 
        ((phHciNfc_ReaderA_Info_t *)(psHciContext->p_reader_a_info))->
        pipe_id) || 
        (pipe_id != 
        ((phHciNfc_ReaderA_Info_t *)(psHciContext->p_reader_a_info))->
        pipe_id))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_ReaderA_Info_t     *p_rdr_a_info=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        phHciNfc_HCP_Packet_t       *hcp_packet = NULL;
        phHciNfc_HCP_Message_t      *hcp_message = NULL;
        uint8_t                     i = 0;
        uint16_t                    length = HCP_HEADER_LEN;
        
        p_rdr_a_info = (phHciNfc_ReaderA_Info_t *)
                            psHciContext->p_reader_a_info ;
        p_pipe_info = p_rdr_a_info->p_pipe_info;
        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                        NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            psHciContext->tx_total = 0 ;
            hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;
            /* Construct the HCP Frame */
            switch(cmd)
            {
                case NXP_WRA_CONTINUE_ACTIVATION:
                case NXP_WR_ACTIVATE_ID:
                {
                    phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                        (uint8_t) pipe_id, HCP_MSG_TYPE_COMMAND, cmd);
                    break;
                }

                case NXP_MIFARE_RAW:
                {
                    if (p_pipe_info->param_length < RDR_A_MIFARE_RAW_LENGTH) 
                    {
                        status = PHNFCSTVAL(CID_NFC_HCI, 
                                            NFCSTATUS_INVALID_PARAMETER);
                    }
                    else
                    {
                        /*  
                            Buffer shall be updated with 
                            TO -              Time out (1 byte)
                            Status -          b0 to b2 indicate valid bits (1 byte)
                            Data (with CRC) - params received from this function 
                        */
                        hcp_message = &(hcp_packet->msg.message);
#ifdef ENABLE_MIFARE_RAW
                        /* Time out */
                        hcp_message->payload[i++] = nxp_nfc_mifareraw_timeout;
                        /* Status */
                        hcp_message->payload[i++] = RDR_A_MIFARE_STATUS;
#else
                        cmd = NXP_MIFARE_CMD;
#endif /* #ifdef ENABLE_MIFARE_RAW */
                        phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                            (uint8_t) pipe_id, HCP_MSG_TYPE_COMMAND, cmd);

                        phHciNfc_Append_HCPFrame((uint8_t *)hcp_message->payload,
                            i, (uint8_t *)p_pipe_info->param_info,
#ifdef ENABLE_MIFARE_RAW
                            p_pipe_info->param_length);
#else
                            (p_pipe_info->param_length - 2));
#endif /* #ifdef ENABLE_MIFARE_RAW */

#ifdef ENABLE_MIFARE_RAW
                        length =(uint16_t)(length + i + p_pipe_info->param_length);
#else
                        length =(uint16_t)(length + i + p_pipe_info->param_length - 2);
#endif /* #ifdef ENABLE_MIFARE_RAW */
                    }
                    break;
                }
                case NXP_MIFARE_CMD:
                {
                    /* 
                        Buffer shall be updated with 
                        Cmd -               Authentication A/B, read/write 
                                            (1 byte)
                        Addr -              Address associated with Mifare cmd
                                            (1 byte)
                        Data  -             params received from this function 
                    */
                    phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                                (uint8_t) pipe_id, HCP_MSG_TYPE_COMMAND, cmd);
                    hcp_message = &(hcp_packet->msg.message);

                    /* Command */
                    hcp_message->payload[i++] = 
                                   psHciContext->p_xchg_info->params.tag_info.cmd_type ;
                     /* Address */
                    hcp_message->payload[i++] = 
                                    psHciContext->p_xchg_info->params.tag_info.addr ;
                    phHciNfc_Append_HCPFrame((uint8_t *)hcp_message->payload,
                        i, (uint8_t *)p_pipe_info->param_info,
                        p_pipe_info->param_length);
                    length =(uint16_t)(length + i + p_pipe_info->param_length);
                    break;
                }
                default:
                {
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_COMMAND);
                    break;
                }
            }
            if (NFCSTATUS_SUCCESS == status)
            {
                p_pipe_info->sent_msg_type = (uint8_t)HCP_MSG_TYPE_COMMAND;
                p_pipe_info->prev_msg = cmd;
                psHciContext->tx_total = length;
                psHciContext->response_pending = TRUE;

                /* Send the Constructed HCP packet to the lower layer */
                status = phHciNfc_Send_HCP( psHciContext, pHwRef);
                p_pipe_info->prev_status = status;
            }
        }
    }
    return status;
}

NFCSTATUS
phHciNfc_ReaderA_Cont_Activate (
                              phHciNfc_sContext_t       *psHciContext,
                              void                      *pHwRef
                              )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    
    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_reader_a_info) || 
        (HCI_READER_A_ENABLE != 
        ((phHciNfc_ReaderA_Info_t *)(psHciContext->p_reader_a_info))->
        enable_rdr_a_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_ReaderA_Info_t     *p_rdr_a_info = (phHciNfc_ReaderA_Info_t *)
                                    psHciContext->p_reader_a_info;
        /* 
            NXP_WRA_CONTINUE_ACTIVATION:
            for activation command */
        status = phHciNfc_Send_ReaderA_Command(psHciContext, 
                        pHwRef, (uint8_t)p_rdr_a_info->pipe_id, 
                        (uint8_t)NXP_WRA_CONTINUE_ACTIVATION);
    }
    return status;
}

NFCSTATUS
phHciNfc_ReaderA_Update_Info(
                             phHciNfc_sContext_t        *psHciContext,
                             uint8_t                    infotype,
                             void                       *rdr_a_info
                             )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    
    if (NULL == psHciContext)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_reader_a_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_ReaderA_Info_t     *p_rdr_a_info=NULL;
        p_rdr_a_info = (phHciNfc_ReaderA_Info_t *)
                        psHciContext->p_reader_a_info ;

        switch(infotype)
        {
            case HCI_READER_A_ENABLE:
            {
                if(NULL != rdr_a_info)
                {
                    p_rdr_a_info->enable_rdr_a_gate = 
                                    *((uint8_t *)rdr_a_info);
                }
                break;
            }
            case HCI_READER_A_INFO_SEQ:
            {
                p_rdr_a_info->current_seq = RDR_A_UID;
                p_rdr_a_info->next_seq = RDR_A_UID;
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
phHciNfc_ReaderA_App_Data (
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
    else if((NULL == psHciContext->p_reader_a_info) || 
        (HCI_READER_A_ENABLE != 
        ((phHciNfc_ReaderA_Info_t *)(psHciContext->p_reader_a_info))->
        enable_rdr_a_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }   
    else
    {
        phHciNfc_ReaderA_Info_t     *p_rdr_a_info=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        p_rdr_a_info = (phHciNfc_ReaderA_Info_t *)
            psHciContext->p_reader_a_info ;
        p_pipe_info = p_rdr_a_info->p_pipe_info;
        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            p_pipe_info->reg_index = RDR_A_APP_DATA_INDEX;
            pipeid = p_rdr_a_info->pipe_id ;
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
phHciNfc_ReaderA_Fwi_Sfgt (
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
    else if((NULL == psHciContext->p_reader_a_info) || 
        (HCI_READER_A_ENABLE != 
        ((phHciNfc_ReaderA_Info_t *)(psHciContext->p_reader_a_info))->
        enable_rdr_a_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }   
    else
    {
        phHciNfc_ReaderA_Info_t     *p_rdr_a_info=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        p_rdr_a_info = (phHciNfc_ReaderA_Info_t *)
            psHciContext->p_reader_a_info ;
        p_pipe_info = p_rdr_a_info->p_pipe_info;
        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            p_pipe_info->reg_index = RDR_A_FWI_SFGT_INDEX;
            pipeid = p_rdr_a_info->pipe_id ;
            /* Fill the data buffer and send the command to the 
            device */
            status = 
                phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                pipeid, (uint8_t)ANY_GET_PARAMETER);
        }
    }
    return status;
}

