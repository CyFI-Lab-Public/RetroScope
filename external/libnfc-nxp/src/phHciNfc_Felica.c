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
* \file  phHciNfc_Felica.c                                                 *
* \brief HCI Felica Management Routines.                                    *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Wed Feb 17 16:19:04 2010 $                                           *
* $Author: ing02260 $                                                         *
* $Revision: 1.11 $                                                           *
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
#include <phOsalNfc.h>

#if defined(TYPE_FELICA)
#include <phHciNfc_Felica.h>
/*
****************************** Macro Definitions *******************************
*/

#define FEL_SINGLE_TAG_FOUND                0x00U
#define FEL_MULTIPLE_TAGS_FOUND             0x03U
#define NXP_WRA_CONTINUE_ACTIVATION         0x12U

#define NXP_FEL_SYS_CODE                    0x01U
#define NXP_FEL_POLREQ_SYS_CODE             0x02U
#define NXP_FEL_CURRENTIDM                  0x04U
#define NXP_FEL_CURRENTPMM                  0x05U

#define NXP_FEL_SYS_CODE_LEN                0x02U
#define NXP_FEL_CUR_IDM_PMM_LEN             0x08U

#define FELICA_STATUS                       0x00U

uint8_t nxp_nfc_felica_timeout = NXP_FELICA_XCHG_TIMEOUT;

/* Presence check command for felica tag */
#define FELICA_REQ_MODE                     0x04U

/*
*************************** Structure and Enumeration ***************************
*/


/*
*************************** Static Function Declaration **************************
*/
static 
NFCSTATUS
phHciNfc_Recv_Felica_Response(
                               void                *psContext,
                               void                *pHwRef,
                               uint8_t             *pResponse,
#ifdef ONE_BYTE_LEN
                               uint8_t              length
#else
                               uint16_t             length
#endif
                               );

static
NFCSTATUS
phHciNfc_Recv_Felica_Event(
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
phHciNfc_Felica_InfoUpdate(
                            phHciNfc_sContext_t     *psHciContext,
                            uint8_t                 index,
                            uint8_t                 *reg_value,
                            uint8_t                 reg_length
                            );

static
NFCSTATUS
phHciNfc_Recv_Felica_Packet(
                           phHciNfc_sContext_t  *psHciContext,
                           uint8_t              cmd,
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
phHciNfc_Felica_Get_PipeID(
                           phHciNfc_sContext_t     *psHciContext,
                           uint8_t                 *ppipe_id
                           )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL != psHciContext)
        && ( NULL != ppipe_id )
        && ( NULL != psHciContext->p_felica_info ) 
        )
    {
        phHciNfc_Felica_Info_t     *p_fel_info = NULL;
        p_fel_info = (phHciNfc_Felica_Info_t *)psHciContext->p_felica_info ;
        *ppipe_id =  p_fel_info->pipe_id  ;
    }
    else 
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    return status;
}


NFCSTATUS
phHciNfc_Felica_Init_Resources(
                               phHciNfc_sContext_t     *psHciContext
                               )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_Felica_Info_t      *p_fel_info = NULL;
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if(
            ( NULL == psHciContext->p_felica_info ) &&
            (phHciNfc_Allocate_Resource((void **)(&p_fel_info),
            sizeof(phHciNfc_Felica_Info_t))== NFCSTATUS_SUCCESS)
            )
        {
            psHciContext->p_felica_info = p_fel_info;
            p_fel_info->current_seq = FELICA_INVALID_SEQ;
            p_fel_info->next_seq = FELICA_INVALID_SEQ;
            p_fel_info->pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INSUFFICIENT_RESOURCES);
        }

    }
    return status;
}

NFCSTATUS
phHciNfc_Felica_Update_PipeInfo(
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
    else if(NULL == psHciContext->p_felica_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_Felica_Info_t *p_fel_info=NULL;
        p_fel_info = (phHciNfc_Felica_Info_t *)psHciContext->p_felica_info ;

        /* Update the pipe_id of the Felica Gate obtained from the HCI 
        Response */
        p_fel_info->pipe_id = pipeID;
        p_fel_info->p_pipe_info = pPipeInfo;
        /* Update the Response Receive routine of the Felica Gate */
        pPipeInfo->recv_resp = phHciNfc_Recv_Felica_Response;
        /* Update the event Receive routine of the Felica Gate */
        pPipeInfo->recv_event = phHciNfc_Recv_Felica_Event;
    }

    return status;
}

NFCSTATUS
phHciNfc_Felica_Update_Info(
                             phHciNfc_sContext_t        *psHciContext,
                             uint8_t                    infotype,
                             void                       *fel_info
                             )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    
    if (NULL == psHciContext)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_felica_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_Felica_Info_t     *p_fel_info=NULL;
        p_fel_info = (phHciNfc_Felica_Info_t *)
                        psHciContext->p_felica_info ;

        switch(infotype)
        {
            case HCI_FELICA_ENABLE:
            {
                if (NULL != fel_info)
                {
                    p_fel_info->enable_felica_gate = 
                                        *((uint8_t *)fel_info);
                }
                break;
            }
            case HCI_FELICA_INFO_SEQ:
            {
                p_fel_info->current_seq = FELICA_SYSTEMCODE;
                p_fel_info->next_seq = FELICA_SYSTEMCODE;
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
phHciNfc_Felica_Info_Sequence (
                               void             *psHciHandle,
                               void             *pHwRef
                               )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t     *psHciContext = 
                            ((phHciNfc_sContext_t *)psHciHandle);

    HCI_PRINT ("HCI : phHciNfc_Felica_Info_Sequence called... \n");
    if( (NULL == psHciContext) 
        || (NULL == pHwRef)
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_felica_info) || 
        (HCI_FELICA_ENABLE != 
        ((phHciNfc_Felica_Info_t *)(psHciContext->p_felica_info))->
        enable_felica_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }   
    else
    {
        phHciNfc_Felica_Info_t      *p_fel_info=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        uint8_t                     pipeid = 0;

        p_fel_info = (phHciNfc_Felica_Info_t *)
                        psHciContext->p_felica_info ;
        p_pipe_info = p_fel_info->p_pipe_info;
        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            HCI_DEBUG ("HCI : p_fel_info->current_seq : %02X\n", p_fel_info->current_seq);
            switch(p_fel_info->current_seq)
            {
                case FELICA_SYSTEMCODE:
                {
                    p_pipe_info->reg_index = NXP_FEL_SYS_CODE;
                    pipeid = p_fel_info->pipe_id ;
                    /* Fill the data buffer and send the command to the 
                        device */
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                        pipeid, (uint8_t)ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_fel_info->next_seq = FELICA_CURRENTIDM;
                    }
                    break;
                }
                case FELICA_CURRENTIDM:
                {
                    p_pipe_info->reg_index = NXP_FEL_CURRENTIDM;
                    pipeid = p_fel_info->pipe_id ;
                    /* Fill the data buffer and send the command to the 
                        device */
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                        pipeid, (uint8_t)ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_fel_info->next_seq = FELICA_CURRENTPMM;
                    }
                    break;
                }
                case FELICA_CURRENTPMM:
                {
                    p_pipe_info->reg_index = NXP_FEL_CURRENTPMM;
                    pipeid = p_fel_info->pipe_id ;
                    /* Fill the data buffer and send the command to the 
                        device */
                    status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                        pipeid, (uint8_t)ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_fel_info->next_seq = FELICA_END_SEQUENCE;
                    }
                    break;
                }
                case FELICA_END_SEQUENCE:
                {
                    phNfc_sCompletionInfo_t     CompInfo;
                    if (FEL_MULTIPLE_TAGS_FOUND == 
                        p_fel_info->multiple_tgts_found)
                    {
                        CompInfo.status = NFCSTATUS_MULTIPLE_TAGS;
                    }
                    else
                    {
                        CompInfo.status = NFCSTATUS_SUCCESS;
                    }

                    CompInfo.info = &(p_fel_info->felica_info);

                    p_fel_info->felica_info.RemDevType = phHal_eFelica_PICC;
                    p_fel_info->current_seq = FELICA_SYSTEMCODE;
                    p_fel_info->next_seq = FELICA_SYSTEMCODE;
                    status = NFCSTATUS_SUCCESS;
                    HCI_DEBUG ("HCI : p_fel_info->felica_info.RemDevType : %02X\n", p_fel_info->felica_info.RemDevType);
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
            HCI_DEBUG ("HCI : p_fel_info->current_seq after : %02X\n", p_fel_info->current_seq);
            HCI_DEBUG ("HCI : p_fel_info->next_seq : %02X\n", p_fel_info->next_seq);
        }
    }
    HCI_PRINT ("HCI : phHciNfc_Felica_Info_Sequence end\n");
    return status;
}

static
NFCSTATUS
phHciNfc_Felica_InfoUpdate(
                            phHciNfc_sContext_t     *psHciContext, 
                            uint8_t                 index,
                            uint8_t                 *reg_value,
                            uint8_t                 reg_length
                            )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_Felica_Info_t     *p_fel_info=NULL;
    phHal_sFelicaInfo_t        *p_fel_tag_info = NULL;

    p_fel_info = (phHciNfc_Felica_Info_t *)(psHciContext->p_felica_info );
    p_fel_tag_info = &(p_fel_info->felica_info.RemoteDevInfo.Felica_Info);

    switch(index)
    {
        case NXP_FEL_SYS_CODE:
        {
            if (NXP_FEL_SYS_CODE_LEN == reg_length)
            {
                /* System code from registry is invalid in this case */
		p_fel_tag_info->SystemCode[0] = 0;
                p_fel_tag_info->SystemCode[1] = 0;
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case NXP_FEL_CURRENTIDM:
        {
            if (NXP_FEL_CUR_IDM_PMM_LEN == reg_length)
            {
                HCI_PRINT_BUFFER("\tFelica ID data", reg_value, reg_length);
                /* Update current PM values */
                (void)memcpy(p_fel_tag_info->IDm, reg_value, 
                            reg_length);
                p_fel_tag_info->IDmLength = reg_length;
            } 
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_HCI_RESPONSE);
            }
            break;
        }
        case NXP_FEL_CURRENTPMM:
        {
            if (NXP_FEL_CUR_IDM_PMM_LEN == reg_length)
            {
                HCI_PRINT_BUFFER("\tFelica PM data", reg_value, reg_length);
                /* Update current PM values */
                (void)memcpy(p_fel_tag_info->PMm, reg_value, 
                            reg_length);
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
phHciNfc_Recv_Felica_Packet(
                            phHciNfc_sContext_t  *psHciContext,
                            uint8_t              cmd,
                            uint8_t              *pResponse,
#ifdef ONE_BYTE_LEN
                            uint8_t             length
#else
                            uint16_t            length
#endif
                            )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    uint8_t                     index = 0;

    /* To remove "warning (VS C4100) : unreferenced formal parameter" */

    PHNFC_UNUSED_VARIABLE(length);

    if (NXP_FELICA_RAW == cmd)
    {
        if (FELICA_STATUS == pResponse[index])  /* Status byte */
        {
            index = (index + 1);
            psHciContext->rx_index = (HCP_HEADER_LEN + 1);
            HCI_PRINT_BUFFER("Felica Bytes received", &pResponse[index], (length - index));
            /* If Poll response received then update IDm and PMm parameters, when presence check going on */
            if (pResponse[index + 1] == 0x01)
            {
                if (length >= 19)
                {
                    /* IDm */
                    (void) memcpy(psHciContext->p_target_info->RemoteDevInfo.Felica_Info.IDm,
                                  &pResponse[index + 2], 8);
                    /* PMm */
                    (void) memcpy(psHciContext->p_target_info->RemoteDevInfo.Felica_Info.PMm,
                                  &pResponse[index + 2 + 8], 8);
                    index = index + 2 + 8 + 8;

                    /* SC */
                    if (length >= 21)
                    {
                        /* Copy SC if available */
                        psHciContext->p_target_info->RemoteDevInfo.Felica_Info.SystemCode[0] = pResponse[index];
                        psHciContext->p_target_info->RemoteDevInfo.Felica_Info.SystemCode[1] = pResponse[index + 1];
                    }
                    else
                    {
                        /* If SC is not available in packet then set to zero */
                        psHciContext->p_target_info->RemoteDevInfo.Felica_Info.SystemCode[0] = 0;
                        psHciContext->p_target_info->RemoteDevInfo.Felica_Info.SystemCode[1] = 0;
                    }
                }
                else
                {
                    status = PHNFCSTVAL(CID_NFC_HCI,
                                        NFCSTATUS_INVALID_HCI_RESPONSE);
                }
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
        psHciContext->rx_index = HCP_HEADER_LEN;

        /* command NXP_FELICA_CMD: so give Felica data to the upper layer */
        HCI_PRINT_BUFFER("Felica Bytes received", pResponse, length);
    }

    return status;
}


static 
NFCSTATUS
phHciNfc_Recv_Felica_Response(
                               void                *psContext,
                               void                *pHwRef,
                               uint8_t             *pResponse,
#ifdef ONE_BYTE_LEN
                               uint8_t          length
#else
                               uint16_t             length
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
    else if(NULL == psHciContext->p_felica_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_Felica_Info_t     *p_fel_info=NULL;
        uint8_t                     prev_cmd = ANY_GET_PARAMETER;
        p_fel_info = (phHciNfc_Felica_Info_t *)
                        psHciContext->p_felica_info ;
        if( NULL == p_fel_info->p_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            prev_cmd = p_fel_info->p_pipe_info->prev_msg ;
            switch(prev_cmd)
            {
                case ANY_GET_PARAMETER:
                {
                    status = phHciNfc_Felica_InfoUpdate(psHciContext, 
                                        p_fel_info->p_pipe_info->reg_index, 
                                        &pResponse[HCP_HEADER_LEN],
                                        (uint8_t)(length - HCP_HEADER_LEN));
                    break;
                }
                case ANY_SET_PARAMETER:
                {
                    HCI_PRINT("Felica Parameter Set \n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                                UPDATE_SEQ);
                    p_fel_info->next_seq = FELICA_SYSTEMCODE;
                    break;
                }
                case ANY_OPEN_PIPE:
                {
                    HCI_PRINT("Felica open pipe complete\n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                                UPDATE_SEQ);
                    p_fel_info->next_seq = FELICA_SYSTEMCODE;
                    break;
                }
                case ANY_CLOSE_PIPE:
                {
                    HCI_PRINT("Felica close pipe complete\n");
                    status = phHciNfc_ReaderMgmt_Update_Sequence(psHciContext, 
                                                                UPDATE_SEQ);
                    break;
                }
            
                case NXP_FELICA_RAW:
                case NXP_FELICA_CMD:
                case WR_XCHGDATA:
                {
                    HCI_PRINT("Felica packet received \n");
                    if (length >= HCP_HEADER_LEN)
                    {
                        phHciNfc_Append_HCPFrame(psHciContext->recv_buffer, 
                                                    0, pResponse, length);
                        psHciContext->rx_total = length;
                        status = phHciNfc_Recv_Felica_Packet(psHciContext,
                                                    prev_cmd, 
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
                    HCI_PRINT("Felica continue activation or ");
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
                        if (FEL_MULTIPLE_TAGS_FOUND == pResponse[HCP_HEADER_LEN])
                        {
                            p_fel_info->multiple_tgts_found = 
                                            FEL_MULTIPLE_TAGS_FOUND;
                        }
                        else
                        {
                            p_fel_info->multiple_tgts_found = FALSE;
                        }
                    }
                    else
                    {
                        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_RESPONSE);
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
                            p_fel_info->uicc_activation = 
                                        UICC_CARD_ACTIVATION_SUCCESS;
                            break;
                        }
                        case (HCP_HEADER_LEN + 1):
                        {
                            p_fel_info->uicc_activation = 
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
                p_fel_info->p_pipe_info->prev_status = NFCSTATUS_SUCCESS;
                p_fel_info->current_seq = p_fel_info->next_seq;
            }
        }
    }
    return status;
}


static
NFCSTATUS
phHciNfc_Recv_Felica_Event(
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

    HCI_PRINT ("HCI : phHciNfc_Recv_Felica_Event called...\n");
    if( (NULL == psHciContext) || (NULL == pHwRef) || (NULL == pEvent)
        || (0 == length))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHciContext->p_felica_info) || 
        (HCI_FELICA_ENABLE != 
        ((phHciNfc_Felica_Info_t *)(psHciContext->p_felica_info))->
        enable_felica_gate))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_HCP_Packet_t       *p_packet = NULL;
        phHciNfc_Felica_Info_t      *p_fel_info = NULL;
        phHciNfc_HCP_Message_t      *message = NULL;
        uint8_t                     instruction=0, 
                                    i = 0;

        p_fel_info = (phHciNfc_Felica_Info_t *)
                                psHciContext->p_felica_info ;
        p_packet = (phHciNfc_HCP_Packet_t *)pEvent;
        message = &p_packet->msg.message;
        /* Get the instruction bits from the Message Header */
        instruction = (uint8_t) GET_BITS8( message->msg_header,
                    HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);

        HCI_DEBUG ("HCI : instruction : %02X\n", instruction);
        HCI_DEBUG ("HCI : Multiple tag found : %02X\n", message->payload[i]);
        if ((EVT_TARGET_DISCOVERED == instruction) 
            && ((FEL_MULTIPLE_TAGS_FOUND == message->payload[i] ) 
            || (FEL_SINGLE_TAG_FOUND == message->payload[i])) 
            )
        {
            static phNfc_sCompletionInfo_t      pCompInfo;

            if (FEL_MULTIPLE_TAGS_FOUND == message->payload[i])
            {
                p_fel_info->multiple_tgts_found = FEL_MULTIPLE_TAGS_FOUND;
                pCompInfo.status = NFCSTATUS_MULTIPLE_TAGS;
            }
            else
            {
                p_fel_info->multiple_tgts_found = FALSE;
                pCompInfo.status = NFCSTATUS_SUCCESS;
            }

            HCI_DEBUG ("HCI : psHciContext->host_rf_type : %02X\n", psHciContext->host_rf_type);
            HCI_DEBUG ("HCI : p_fel_info->felica_info.RemDevType : %02X\n", p_fel_info->felica_info.RemDevType);
            HCI_DEBUG ("HCI : p_fel_info->current_seq : %02X\n", p_fel_info->current_seq);

            psHciContext->host_rf_type = phHal_eFelica_PCD;
            p_fel_info->felica_info.RemDevType = phHal_eFelica_PICC;
            p_fel_info->current_seq = FELICA_SYSTEMCODE;

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
    HCI_PRINT ("HCI : phHciNfc_Recv_Felica_Event end\n");
    return status;
}


NFCSTATUS
phHciNfc_Felica_Request_Mode(
                              phHciNfc_sContext_t   *psHciContext,
                              void                  *pHwRef)
{
    NFCSTATUS           status = NFCSTATUS_SUCCESS;
    static uint8_t      pres_chk_data[6] = {0};

    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        phHciNfc_Felica_Info_t          *ps_fel_info = NULL;
        phHciNfc_Pipe_Info_t            *ps_pipe_info = NULL;
        phHal_sFelicaInfo_t             *ps_rem_fel_info = NULL;

        ps_fel_info = (phHciNfc_Felica_Info_t *)
                            psHciContext->p_felica_info ;
        ps_pipe_info = ps_fel_info->p_pipe_info;
        
        if(NULL == ps_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            ps_rem_fel_info = 
                &(ps_fel_info->felica_info.RemoteDevInfo.Felica_Info);

            pres_chk_data[0] = sizeof(pres_chk_data);
            pres_chk_data[1] = 0x00; // Felica poll
            pres_chk_data[2] = 0xFF;
            pres_chk_data[3] = 0xFF;
            pres_chk_data[4] = 0x01;
            pres_chk_data[5] = 0x00;

            ps_pipe_info->param_info = pres_chk_data;
            ps_pipe_info->param_length = sizeof(pres_chk_data);
            status = phHciNfc_Send_Felica_Command(
                                        psHciContext, pHwRef, 
                                        ps_pipe_info->pipe.pipe_id, 
                                        NXP_FELICA_RAW);
        }
    }

    return status;
}

NFCSTATUS
phHciNfc_Send_Felica_Command(
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
    else if((NULL == psHciContext->p_felica_info) || 
        (HCI_FELICA_ENABLE != 
        ((phHciNfc_Felica_Info_t *)(psHciContext->p_felica_info))->
        enable_felica_gate) || 
        (HCI_UNKNOWN_PIPE_ID == 
        ((phHciNfc_Felica_Info_t *)(psHciContext->p_felica_info))->
        pipe_id) || 
        (pipe_id != 
        ((phHciNfc_Felica_Info_t *)(psHciContext->p_felica_info))->
        pipe_id))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_Felica_Info_t     *p_fel_info=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        phHciNfc_HCP_Packet_t       *hcp_packet = NULL;
        phHciNfc_HCP_Message_t      *hcp_message = NULL;
        uint8_t                     i = 0, 
                                    length = HCP_HEADER_LEN;

        p_fel_info = (phHciNfc_Felica_Info_t *)
                            psHciContext->p_felica_info ;
        p_pipe_info = p_fel_info->p_pipe_info;
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
            phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                            (uint8_t) pipe_id, HCP_MSG_TYPE_COMMAND, cmd);
            switch(cmd)
            {
                case NXP_FELICA_RAW:
                {
                    /*  
                    Buffer shall be updated with 
                    TO -              Time out (1 byte)
                    Status -          b0 to b2 indicate valid bits (1 byte)
                    Data  -           params received from this function 
                    */
                    hcp_message = &(hcp_packet->msg.message);

                    /* Time out */
                    hcp_message->payload[i++] = nxp_nfc_felica_timeout ;
                    /* Status */
                    hcp_message->payload[i++] = FELICA_STATUS;

                    phHciNfc_Append_HCPFrame((uint8_t *)hcp_message->payload,
                                        i, (uint8_t *)p_pipe_info->param_info,
                                        p_pipe_info->param_length);
                    length =(uint8_t)(length + i + p_pipe_info->param_length);
                    break;
                }
                case NXP_FELICA_CMD:
                {
                    /* 
                    Buffer shall be updated with 
                    Cmd -               Authentication A/B, read/write 
                    (1 byte)
                    Data  -             params received from this function 
                    */
                    hcp_message = &(hcp_packet->msg.message);

                    /* Command */
                    hcp_message->payload[i++] = 
                                 psHciContext->p_xchg_info->params.tag_info.cmd_type ;
                    phHciNfc_Append_HCPFrame((uint8_t *)hcp_message->payload,
                                        i, (uint8_t *)p_pipe_info->param_info,
                                        p_pipe_info->param_length);
                    length =(uint8_t)(length + i + p_pipe_info->param_length);
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

#endif /* #if defined(TYPE_FELICA) */
