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
* \file  phHciNfc_SWP.c                                                       *
* \brief HCI SWP gate Management Routines.                                     *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Tue Aug 18 10:16:36 2009 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.31 $                                                            *
* $Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                                                *                                                                             *
* =========================================================================== *
*/

/*
***************************** Header File Inclusion ****************************
*/
#include <phNfcCompId.h>
#include <phHciNfc_Pipe.h>
#include <phHciNfc_SWP.h>
#include <phOsalNfc.h>
#include <phHciNfc_Emulation.h>
#include <phHciNfc_DevMgmt.h>
/*
****************************** Macro Definitions *******************************
*/

/* SWP Gate regsitry Settings */
/* set default mode mode as virtual mode */
#define NXP_SWP_DEFAULT_MODE_INDEX      (0x01U)
/* Get the Status of the UICC Connection */
#define NXP_SWP_STATUS_INDEX            (0x02U)

/* Configure the Secure Element Protected Mode */
#define NXP_SWP_PROTECTED_INDEX         (0x03U)

/* Switch mode index */
#define NXP_EVT_SWP_SWITCH_MODE         (0x03U)

/* Protected Event from the Host Controller */
#define NXP_EVT_SWP_PROTECTED           (0x04U)

/****************** Structure and Enumeration ****************************/

/****************** Static Function Declaration **************************/

static 
NFCSTATUS 
phHciNfc_Recv_SWP_Response(
                            void        *psContext,
                            void        *pHwRef,
                            uint8_t     *pResponse,
#ifdef ONE_BYTE_LEN
                            uint8_t     length
#else
                            uint16_t    length
#endif
                        );

static 
NFCSTATUS
phHciNfc_Send_SWP_Event(
                       phHciNfc_sContext_t      *psHciContext,
                       void                     *pHwRef,
                       uint8_t                  pipe_id,
                       uint8_t                  event
                       );


static 
NFCSTATUS
phHciNfc_Recv_SWP_Event(
                        void                *psContext,
                        void                *pHwRef,
                        uint8_t             *pEvent,
#ifdef ONE_BYTE_LEN
                       uint8_t               length
#else
                       uint16_t              length
#endif
                       );

static
NFCSTATUS
phHciNfc_SWP_InfoUpdate(
                                phHciNfc_sContext_t     *psHciContext,
                                uint8_t                 index,
                                uint8_t                 *reg_value,
                                uint8_t                 reg_length
                          );

/*
*************************** Function Definitions ***************************
*/

NFCSTATUS
phHciNfc_SWP_Get_PipeID(
                       phHciNfc_sContext_t        *psHciContext,
                       uint8_t                    *ppipe_id
                       )
{
    NFCSTATUS         status = NFCSTATUS_SUCCESS;

    if( (NULL != psHciContext)
        && ( NULL != ppipe_id )
        && ( NULL != psHciContext->p_swp_info ) 
        )
    {
        phHciNfc_SWP_Info_t     *p_swp_info=NULL;
        p_swp_info = (phHciNfc_SWP_Info_t *)
                        psHciContext->p_swp_info ;
        *ppipe_id =  p_swp_info->pipe_id  ;
    }
    else 
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    return status;
}

NFCSTATUS
phHciNfc_SWP_Init_Resources(phHciNfc_sContext_t *psHciContext)
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_SWP_Info_t         *ps_swp_info=NULL;
   
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if(( NULL == psHciContext->p_swp_info ) &&
             (phHciNfc_Allocate_Resource((void **)(&ps_swp_info),
            sizeof(phHciNfc_SWP_Info_t))== NFCSTATUS_SUCCESS))
        {
            psHciContext->p_swp_info = ps_swp_info;
            ps_swp_info->current_seq = SWP_INVALID_SEQUENCE;
            ps_swp_info->next_seq = SWP_INVALID_SEQUENCE;
            ps_swp_info->pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INSUFFICIENT_RESOURCES);
        }
        
    }
    return status;
}


NFCSTATUS
phHciNfc_SWP_Update_PipeInfo(
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
    else if(NULL == psHciContext->p_swp_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_SWP_Info_t     *ps_swp_info=NULL;
        ps_swp_info = (phHciNfc_SWP_Info_t *)
                                psHciContext->p_swp_info ;
        /* Update the pipe_id of the SWP Gate obtained from HCI Response */
        ps_swp_info->pipe_id = pipeID;
        ps_swp_info->p_pipe_info = pPipeInfo;
        if (NULL != pPipeInfo)
        {
            /* Update the Response Receive routine of the SWP Gate */
            pPipeInfo->recv_resp = &phHciNfc_Recv_SWP_Response;
            pPipeInfo->recv_event =&phHciNfc_Recv_SWP_Event;
        }
    }
    return status;
}


static 
NFCSTATUS
phHciNfc_Recv_SWP_Response(
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
    else if(NULL == psHciContext->p_swp_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_SWP_Info_t     *ps_swp_info=NULL;
        uint8_t                 prev_cmd = ANY_GET_PARAMETER;

        ps_swp_info = (phHciNfc_SWP_Info_t *)
                        psHciContext->p_swp_info ;
        if( NULL == ps_swp_info->p_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                            NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            prev_cmd = ps_swp_info->p_pipe_info->prev_msg ;
            switch(prev_cmd)
            {
                case ANY_GET_PARAMETER:
                {
                    HCI_PRINT(" Getting the SWP Parameter \n");
                    status = phHciNfc_SWP_InfoUpdate(psHciContext,
                                    ps_swp_info->p_pipe_info->reg_index, 
                                    &pResponse[HCP_HEADER_LEN],
                                    (uint8_t)(length - HCP_HEADER_LEN));

                    break;
                }
                case ANY_SET_PARAMETER:
                {
                    HCI_PRINT("SWP Parameter Set \n");
                    break;
                }
                case ANY_OPEN_PIPE:
                {
                    HCI_PRINT("SWP gate open pipe complete\n");
                    break;
                }
                case ANY_CLOSE_PIPE:
                {
                    HCI_PRINT("SWP close pipe complete\n");
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
                status = phHciNfc_EmuMgmt_Update_Seq(psHciContext, 
                                                    UPDATE_SEQ);
                ps_swp_info->p_pipe_info->prev_status = NFCSTATUS_SUCCESS;
                ps_swp_info->current_seq = ps_swp_info->next_seq;
            }
        }
    }
    return status;
}


NFCSTATUS
phHciNfc_SWP_Configure_Default(
                            void        *psHciHandle,
                            void        *pHwRef,
                            uint8_t     enable_type
                        )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    static uint8_t          param = 0 ;
    phHciNfc_sContext_t     *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);

    if((NULL == psHciContext)||(NULL == pHwRef))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_swp_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_SWP_Info_t         *ps_swp_info=NULL;
        phHciNfc_Pipe_Info_t        *ps_pipe_info=NULL;
        
        ps_swp_info = (phHciNfc_SWP_Info_t*)psHciContext->p_swp_info;       
        ps_pipe_info = ps_swp_info->p_pipe_info;

        if(NULL == ps_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            ps_pipe_info->reg_index = NXP_SWP_DEFAULT_MODE_INDEX;
            /* Enable/Disable SWP link */
            param = (uint8_t)enable_type;
            ps_pipe_info->param_info =(void*)&param ;
            ps_pipe_info->param_length = sizeof(param) ;
            status = phHciNfc_Send_Generic_Cmd(psHciContext, pHwRef, 
                                        ps_swp_info->pipe_id, 
                                        (uint8_t)ANY_SET_PARAMETER);
        }

    }
    return status;
}


NFCSTATUS
phHciNfc_SWP_Get_Status(
                            void        *psHciHandle,
                            void        *pHwRef
                        )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    /* static uint8_t       param = 0 ; */
    phHciNfc_sContext_t     *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);

    if((NULL == psHciContext)||(NULL == pHwRef))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_swp_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_SWP_Info_t         *ps_swp_info=NULL;
        phHciNfc_Pipe_Info_t        *ps_pipe_info=NULL;

        ps_swp_info = (phHciNfc_SWP_Info_t*)psHciContext->p_swp_info;
        ps_pipe_info = ps_swp_info->p_pipe_info;

        if(NULL == ps_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            ps_pipe_info->reg_index = NXP_SWP_STATUS_INDEX;
            status = phHciNfc_Send_Generic_Cmd(psHciContext, pHwRef,  
                                    ps_swp_info->pipe_id, 
                                    (uint8_t)ANY_GET_PARAMETER);
        }
    }
    return status;
}


NFCSTATUS
phHciNfc_SWP_Get_Bitrate(
                            void        *psHciHandle,
                            void        *pHwRef
                        )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t     *psHciContext = ((phHciNfc_sContext_t *)
                                            psHciHandle);

    if((NULL == psHciContext) || (NULL == pHwRef))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_swp_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_SWP_Info_t         *ps_swp_info = NULL;
        
        ps_swp_info = (phHciNfc_SWP_Info_t*)psHciContext->p_swp_info;
       
        status = phHciNfc_DevMgmt_Get_Info(psHciContext, pHwRef, 
                    NFC_ADDRESS_SWP_BITRATE, &(ps_swp_info->uicc_bitrate));

    }
    return status;
}


NFCSTATUS
phHciNfc_SWP_Protection(
                            void        *psHciHandle,
                            void        *pHwRef,
                            uint8_t     mode
                      )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    static uint8_t          param = 0 ;
    phHciNfc_sContext_t     *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);

    if((NULL == psHciContext)||(NULL == pHwRef))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_swp_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_SWP_Info_t         *ps_swp_info=NULL;
        phHciNfc_Pipe_Info_t        *ps_pipe_info=NULL;
        
        ps_swp_info = (phHciNfc_SWP_Info_t*)psHciContext->p_swp_info;       
        ps_pipe_info = ps_swp_info->p_pipe_info;

        if(NULL == ps_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            ps_pipe_info->reg_index = NXP_SWP_PROTECTED_INDEX;
            /* Enable/Disable SWP Protection */
            param = (uint8_t)mode;
            ps_pipe_info->param_info =(void*)&param ;
            ps_pipe_info->param_length = sizeof(param) ;
            status = phHciNfc_Send_Generic_Cmd(psHciContext, pHwRef, 
                                        ps_swp_info->pipe_id, 
                                        (uint8_t)ANY_SET_PARAMETER);
        }

    }
    return status;
}




static
NFCSTATUS
phHciNfc_SWP_InfoUpdate(
                                phHciNfc_sContext_t     *psHciContext,
                                uint8_t                 index,
                                uint8_t                 *reg_value,
                                uint8_t                 reg_length
                          )
{
    phHciNfc_SWP_Info_t         *ps_swp_info=NULL;
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    ps_swp_info = (phHciNfc_SWP_Info_t *)
                    psHciContext->p_swp_info ;

    /* To remove "warning (VS C4100) : unreferenced formal parameter" */
    PHNFC_UNUSED_VARIABLE(reg_length);
    switch(index)
    {
        case NXP_SWP_DEFAULT_MODE_INDEX:
        {
            HCI_PRINT_BUFFER("\tUICC Enable Register:",reg_value,reg_length);
            break;
        } 
            /* Get the Status of the UICC Connection */
        case NXP_SWP_STATUS_INDEX:
        {
            HCI_PRINT_BUFFER("\tUICC Connection Status:", reg_value, reg_length);
            ps_swp_info->uicc_status = (phHciNfc_SWP_Status_t ) *reg_value ;
            break;
        }
        case NXP_SWP_PROTECTED_INDEX:
        {
            HCI_PRINT_BUFFER("\t UICC Card Emulation Rights :",reg_value,reg_length);

            break;
        }
        default:
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
            break;
        }
    } /* End of switch(index) */
    
    return status;
}

NFCSTATUS
phHciNfc_SWP_Configure_Mode(
                              void              *psHciHandle,
                              void              *pHwRef,
                              uint8_t           uicc_mode
                          )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    static uint8_t          param = 0;
    phHciNfc_sContext_t     *psHciContext = ((phHciNfc_sContext_t *)
                                            psHciHandle);
    
    if( (NULL == psHciContext)||(NULL == pHwRef))
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if ( NULL == psHciContext->p_swp_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, 
                        NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        phHciNfc_SWP_Info_t         *ps_swp_info=NULL;
        phHciNfc_Pipe_Info_t        *ps_pipe_info=NULL;
        
        ps_swp_info = (phHciNfc_SWP_Info_t*)psHciContext->p_swp_info;
       
        ps_pipe_info = ps_swp_info->p_pipe_info;
        if(NULL == ps_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            /* Switch the Mode of the SmartMx */
            param = uicc_mode;
            ps_pipe_info->param_info =(void*)&param ;
            ps_pipe_info->param_length = sizeof(param) ;
            status = phHciNfc_Send_SWP_Event( psHciContext, pHwRef,
                                            ps_swp_info->pipe_id, 
                                            NXP_EVT_SWP_SWITCH_MODE );

            /* Send the Success Status as this is an event */
            status = ((status == NFCSTATUS_PENDING)?
                    NFCSTATUS_SUCCESS : status);

        }/* End of else part*/
    } 
    return status;
}

static 
NFCSTATUS
phHciNfc_Recv_SWP_Event(
                        void                *psContext,
                        void                *pHwRef,
                        uint8_t             *pEvent,
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
    static phHal_sEventInfo_t   EventInfo;
    

    if( (NULL == psHciContext) || (NULL == pHwRef) || (NULL == pEvent)
        || (length == 0))
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_swp_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_SWP_Info_t     *ps_swp_info=NULL;

        ps_swp_info = (phHciNfc_SWP_Info_t *)
                        psHciContext->p_swp_info ;
        if( NULL == ps_swp_info->p_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                            NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            phHciNfc_HCP_Packet_t       *p_packet = NULL;
            phHciNfc_HCP_Message_t      *message = NULL;
            uint8_t                     EventType = 0;                      

            p_packet = (phHciNfc_HCP_Packet_t *)pEvent;
            message = &(p_packet->msg.message);
            /* Get the instruction bits from the Message Header */
            EventType = (uint8_t) GET_BITS8( message->msg_header,
                HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);

            EventInfo.eventHost = phHal_eHostController;
            EventInfo.eventSource = phHal_ePICC_DevType;
            /* Occurrence of the Protected events for reporting */
            if (NXP_EVT_SWP_PROTECTED == EventType) 
            {
                EventInfo.eventType = NFC_EVT_PROTECTED;
            }
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_RESPONSE);
            }

            if (NFCSTATUS_SUCCESS == status )
            {
                phHciNfc_Notify_Event(  psHciContext, pHwRef, 
                NFC_NOTIFY_EVENT, (void*)&EventInfo);
            }

        }
    }
    return status;
}



static 
NFCSTATUS
phHciNfc_Send_SWP_Event(
                       phHciNfc_sContext_t      *psHciContext,
                       void                     *pHwRef,
                       uint8_t                  pipe_id,
                       uint8_t                  event
                       )
{
    phHciNfc_HCP_Packet_t   *hcp_packet = NULL;
    phHciNfc_HCP_Message_t  *hcp_message = NULL;
    phHciNfc_Pipe_Info_t    *p_pipe_info = NULL;
    uint8_t                 length = 0;
    uint8_t                 i=0;
    NFCSTATUS               status = NFCSTATUS_SUCCESS;

    p_pipe_info = (phHciNfc_Pipe_Info_t *) 
                    psHciContext->p_pipe_list[pipe_id];
    psHciContext->tx_total = 0 ;
    length = (length + HCP_HEADER_LEN);

    hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;
    /* Construct the HCP Frame */
    phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                            (uint8_t) pipe_id, 
                            HCP_MSG_TYPE_EVENT, event);

    hcp_message = &(hcp_packet->msg.message);

    phHciNfc_Append_HCPFrame((uint8_t *)hcp_message->payload,
                            i, 
                            (uint8_t *)p_pipe_info->param_info,
                            p_pipe_info->param_length);
    length = (uint8_t)(length + i + p_pipe_info->param_length);

    p_pipe_info->sent_msg_type = HCP_MSG_TYPE_EVENT ;
    p_pipe_info->prev_msg = event ;
    psHciContext->tx_total = length;

    /* Send the Constructed HCP packet to the lower layer */
    status = phHciNfc_Send_HCP( psHciContext, pHwRef );
    if(NFCSTATUS_PENDING == status)
    {
        ((phHciNfc_SWP_Info_t *)psHciContext->p_swp_info)->current_seq = 
            ((phHciNfc_SWP_Info_t *)psHciContext->p_swp_info)->next_seq;
        p_pipe_info->prev_status = status;
    }

    return status;
}

NFCSTATUS
phHciNfc_SWP_Update_Sequence(
                                phHciNfc_sContext_t     *psHciContext,
                                phHciNfc_eSeqType_t     SWP_seq
                             )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_SWP_Info_t         *ps_swp_info=NULL;
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if( NULL == psHciContext->p_swp_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, 
                            NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        ps_swp_info = (phHciNfc_SWP_Info_t *)
                            psHciContext->p_swp_info ;
        switch(SWP_seq)
        {
            case RESET_SEQ:
            case INIT_SEQ:
            {
                ps_swp_info->current_seq = SWP_INVALID_SEQUENCE;
                ps_swp_info->next_seq = SWP_INVALID_SEQUENCE ;
                break;
            }
            case UPDATE_SEQ:
            {
                ps_swp_info->current_seq = ps_swp_info->next_seq;
                break;
            }
            case REL_SEQ:
            {
                ps_swp_info->current_seq = SWP_INVALID_SEQUENCE;
                ps_swp_info->next_seq =  SWP_INVALID_SEQUENCE;
                break;
            }
            case CONFIG_SEQ:
            {
                ps_swp_info->current_seq = SWP_STATUS_SEQ;
                ps_swp_info->next_seq =  SWP_STATUS_SEQ;
                break;
            }
            default:
            {
                break;
            }
        }
    }
    return status;
}

NFCSTATUS
phHciNfc_SWP_Config_Sequence(
                            phHciNfc_sContext_t     *psHciContext, 
                            void                    *pHwRef, 
                            phHal_sEmulationCfg_t   *ps_emulation_cfg
                        )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_SWP_Info_t         *ps_swp_info=NULL;

    if ((NULL == psHciContext) || (NULL == pHwRef) || 
        (NULL == ps_emulation_cfg))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if( NULL == psHciContext->p_swp_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, 
                            NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        phHciNfc_Pipe_Info_t        *ps_pipe_info = NULL;
        phHal_sUiccEmuCfg_t         *uicc_config = 
                                    &(ps_emulation_cfg->config.uiccEmuCfg);

        ps_swp_info = (phHciNfc_SWP_Info_t *)psHciContext->p_swp_info;
        ps_pipe_info = ps_swp_info->p_pipe_info;

        if (NULL == ps_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            switch(ps_swp_info->current_seq)
            {
                case SWP_STATUS_SEQ :
                {
                    status = phHciNfc_SWP_Configure_Default( psHciContext,
                                            pHwRef, uicc_config->enableUicc );

                    if(status == NFCSTATUS_PENDING)
                    {
                        ps_swp_info->next_seq = SWP_STATUS_SEQ;
                        status = NFCSTATUS_SUCCESS;
                    }
                    break;
                }
                case SWP_MODE_SEQ :
                {
                    status = phHciNfc_SWP_Configure_Mode( psHciContext, 
                                        pHwRef, UICC_SWITCH_MODE_DEFAULT );
                                        /* UICC_SWITCH_MODE_ON  */
                    if(status == NFCSTATUS_PENDING)
                    {
                        ps_swp_info->next_seq = SWP_STATUS_SEQ;
                        status = NFCSTATUS_SUCCESS;
                    }
                    break;
                }
                default :
                {
                    status = PHNFCSTVAL(CID_NFC_HCI, 
                                        NFCSTATUS_INVALID_HCI_INFORMATION);
                    break;
                }
            }
        }
    }
    return status;
}



