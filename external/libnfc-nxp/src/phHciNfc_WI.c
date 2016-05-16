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
* \file  phHciNfc_WI.c                                                        *
* \brief HCI WI gate Management Routines.                                     *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Tue Aug 18 10:22:34 2009 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.33 $                                                            *
* $Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                                                *                                                                             *
* =========================================================================== *
*/

/*
***************************** Header File Inclusion ****************************
*/
#include <phNfcCompId.h>
#include <phHciNfc_Pipe.h>
#include <phHciNfc_WI.h>
#include <phOsalNfc.h>
#include <phHciNfc_Emulation.h>
/*
****************************** Macro Definitions *******************************
*/
/*  WI gate specific Events definition */
#define NXP_EVT_SE_START_OF_TRANSACTION (0x01U)
#define NXP_EVT_SE_END_OF_TRANSACTION   (0x02U)
#define NXP_EVT_SE_SWITCH_MODE          (0x03U)
#define NXP_EVT_SE_TRANSACTION          (0x04U)

/* WI Gate registry Settings */
/* set default mode mode as virtual mode */
#define NXP_SE_DEFAULTMODE_INDEX        (0x01)
#define NXP_SE_EVENTS_INDEX             (0x05)                  

/* Set Bit 0 and Bit 1 to report Start of transaction and End of transaction*/
#define WI_ENABLE_EVENTS                (0x04)
#define WI_VIRTUALMODE                  (0x01)
#define WI_OFFMODE                      (0x00)
#define AID_SIZE                        (0x20)
/****************** Structure and Enumeration ****************************/


/****************** Static Function Declaration **************************/

static uint8_t paypass_removal[2]          = {0x50, 0x00};
static uint8_t mifare_access               = 0x60;

static 
NFCSTATUS 
phHciNfc_Recv_WI_Response(  
                              void  *psContext,
                              void  *pHwRef,
                              uint8_t *pResponse,
#ifdef ONE_BYTE_LEN
                              uint8_t            length
#else
                              uint16_t           length
#endif
                         );

static 
NFCSTATUS 
phHciNfc_Recv_WI_Event(
                        void    *psContext,
                        void    *pHwRef,
                        uint8_t *pEvent,
#ifdef ONE_BYTE_LEN
                        uint8_t            length
#else
                        uint16_t           length
#endif
                    );

static 
NFCSTATUS
phHciNfc_Send_WI_Event(
                        phHciNfc_sContext_t     *psHciContext,
                        void                    *pHwRef,
                        uint8_t                 pipe_id,
                        uint8_t                 event
                    );

static
NFCSTATUS
phHciNfc_WI_InfoUpdate(
                            phHciNfc_sContext_t     *psHciContext,
                            uint8_t                 index,
                            uint8_t                 *reg_value,
                            uint8_t                 reg_length
                            );


#if defined (WI_UPDATE_SEQ)
static
NFCSTATUS
phHciNfc_WI_Update_Sequence(
                                phHciNfc_sContext_t     *psHciContext,
                                phHciNfc_eSeqType_t     WI_seq
                         );
#endif /* #if defined (WI_UPDATE_SEQ) */

/*
*************************** Function Definitions ***************************
*/



NFCSTATUS
phHciNfc_WI_Init_Resources(
                           phHciNfc_sContext_t  *psHciContext
                          )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_WI_Info_t          *p_WI_info=NULL;
   
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if(( NULL == psHciContext->p_wi_info ) &&
             (phHciNfc_Allocate_Resource((void **)(&p_WI_info),
            sizeof(phHciNfc_WI_Info_t))== NFCSTATUS_SUCCESS))
        {
            psHciContext->p_wi_info = p_WI_info;
            p_WI_info->current_seq = eWI_PipeOpen;
            p_WI_info->next_seq = eWI_PipeOpen;
            p_WI_info->pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INSUFFICIENT_RESOURCES);
        }
        
    }
    return status;
}

NFCSTATUS
phHciNfc_WI_Get_PipeID(
                            phHciNfc_sContext_t        *psHciContext,
                            uint8_t                    *ppipe_id
                            )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL != psHciContext)
        && ( NULL != ppipe_id )
        && ( NULL != psHciContext->p_wi_info ) 
        )
    {
        phHciNfc_WI_Info_t     *p_wi_info=NULL;
        p_wi_info = (phHciNfc_WI_Info_t *)
            psHciContext->p_wi_info ;
        *ppipe_id =  p_wi_info->pipe_id  ;
    }
    else 
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    return status;
}


NFCSTATUS
phHciNfc_WI_Update_PipeInfo(
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
    else if(NULL == psHciContext->p_wi_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_WI_Info_t *p_WI_info=NULL;
        p_WI_info = (phHciNfc_WI_Info_t *)
                                psHciContext->p_wi_info ;
        /* Update the pipe_id of the WI Gate obtained from HCI Response */
        p_WI_info->pipe_id = pipeID;
        p_WI_info->p_pipe_info = pPipeInfo;
        if ( NULL != pPipeInfo)
        {
            /* Update the Response Receive routine of the WI Gate */
            pPipeInfo->recv_resp = &phHciNfc_Recv_WI_Response;
            /* Update the event Receive routine of the WI Gate */
            pPipeInfo->recv_event = &phHciNfc_Recv_WI_Event;
        }
    }

    return status;
}

#if defined (WI_UPDATE_SEQ)
static
NFCSTATUS
phHciNfc_WI_Update_Sequence(
                                phHciNfc_sContext_t     *psHciContext,
                                phHciNfc_eSeqType_t     WI_seq
                             )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_WI_Info_t          *p_WI_info=NULL;
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if ( NULL == psHciContext->p_wi_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, 
                        NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        p_WI_info = (phHciNfc_WI_Info_t *)
                            psHciContext->p_wi_info ;
        switch(WI_seq)
        {
            case RESET_SEQ:
            case INIT_SEQ:
            {
                p_WI_info->current_seq = eWI_PipeOpen;
                p_WI_info->next_seq = eWI_SetDefaultMode ;
            }break;
            case UPDATE_SEQ:
            {
                p_WI_info->current_seq = p_WI_info->next_seq;
            
            }break;
            case REL_SEQ:
            {
                p_WI_info->current_seq = eWI_PipeOpen;
                p_WI_info->next_seq = eWI_PipeClose ;
            }break;
            default:
            {
                break;
            }
        }/* End of Update Sequence Switch */
    }
    return status;

}
#endif /* #if defined (WI_UPDATE_SEQ) */

NFCSTATUS
phHciNfc_WI_Configure_Default(
                              void                  *psHciHandle,
                              void                  *pHwRef,
                              uint8_t               enable_type
                          )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    static uint8_t          param = 0;
    phHciNfc_sContext_t     *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    
    if( (NULL == psHciContext)||(NULL == pHwRef))
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if ( NULL == psHciContext->p_wi_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, 
                        NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        phHciNfc_WI_Info_t          *p_wi_info=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        
        p_wi_info = (phHciNfc_WI_Info_t*)psHciContext->p_wi_info;
       
        p_pipe_info = p_wi_info->p_pipe_info;
        if(NULL == p_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                            NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            p_pipe_info->reg_index = NXP_SE_DEFAULTMODE_INDEX;
            /* Enable/Disable Default Virtual Mode for SmartMx */
            param = (uint8_t)enable_type;
            p_pipe_info->param_info =(void*)&param ;
            p_pipe_info->param_length = sizeof(param) ;
            status = phHciNfc_Send_Generic_Cmd(psHciContext,pHwRef, 
                                    p_wi_info->pipe_id,(uint8_t)ANY_SET_PARAMETER);

        }/* End of else part*/
    } 
    return status;
}

NFCSTATUS
phHciNfc_WI_Get_Default(
                              void                  *psHciHandle,
                              void                  *pHwRef
                              )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS; 
    phHciNfc_sContext_t     *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);

    if( (NULL == psHciContext)||(NULL == pHwRef))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if ( NULL == psHciContext->p_wi_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, 
                            NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        phHciNfc_WI_Info_t          *p_wiinfo=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;

        p_wiinfo = (phHciNfc_WI_Info_t*)psHciContext->p_wi_info;

        p_pipe_info = p_wiinfo->p_pipe_info;
        if(NULL == p_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            p_pipe_info->reg_index = NXP_SE_DEFAULTMODE_INDEX;
            
            status = phHciNfc_Send_Generic_Cmd(psHciContext,pHwRef, 
                                            p_wiinfo->pipe_id,
                                            (uint8_t)ANY_GET_PARAMETER);

        }/* End of else part*/
    } 
    return status;
}


NFCSTATUS
phHciNfc_WI_Configure_Mode(
                              void                *psHciHandle,
                              void                *pHwRef,
                              phHal_eSmartMX_Mode_t   e_smx_mode
                          )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    static uint8_t          param = 0;
    phHciNfc_sContext_t     *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    
    if( (NULL == psHciContext)||(NULL == pHwRef))
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if ( NULL == psHciContext->p_wi_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, 
                        NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        phHciNfc_WI_Info_t          *p_wi_info=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        
        p_wi_info = (phHciNfc_WI_Info_t*)psHciContext->p_wi_info;
       
        p_pipe_info = p_wi_info->p_pipe_info;
        if(NULL == p_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                            NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            /* Switch the Mode of the SmartMx */
            param = (uint8_t)e_smx_mode;
            p_pipe_info->param_info =(void*)&param ;
            p_pipe_info->param_length = sizeof(param) ;
            status = phHciNfc_Send_WI_Event( psHciContext, pHwRef,
                                p_wi_info->pipe_id, NXP_EVT_SE_SWITCH_MODE );
            /* Send the Success Status as this is an event */
            status = ( (status == NFCSTATUS_PENDING)?
                                NFCSTATUS_SUCCESS : status);

        }/* End of else part*/
    } 
    return status;
}


NFCSTATUS
phHciNfc_WI_Configure_Notifications(
                                    void        *psHciHandle,
                                    void        *pHwRef,
                                    phHciNfc_WI_Events_t eNotification
                                )
{   
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    static uint8_t          param = 0;
    phHciNfc_sContext_t     *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    
    if( (NULL == psHciContext)||(NULL == pHwRef))
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if ( NULL == psHciContext->p_wi_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, 
                        NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        phHciNfc_WI_Info_t          *p_wi_info=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
      
        
        p_wi_info = (phHciNfc_WI_Info_t*)psHciContext->p_wi_info;
        p_pipe_info = p_wi_info->p_pipe_info;
        if(NULL == p_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI,
                                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {   
            if(eEnableEvents == eNotification)
            {
                /* Enable start and end of transaction events*/
                param = WI_ENABLE_EVENTS;   
            }
            else
            {
                /* Disable Events*/
                param = FALSE ; 
            }
            p_pipe_info->reg_index = NXP_SE_EVENTS_INDEX;
            p_pipe_info->param_info =(void*)&param ;
            p_pipe_info->param_length = sizeof(param) ;
                        
            status = phHciNfc_Send_Generic_Cmd(psHciContext,pHwRef, 
            p_wi_info->pipe_id,(uint8_t)ANY_SET_PARAMETER);
        }
    }
    return status;
}


/*!
* \brief Sends WI gate specfic HCI Events to the connected reader device.
* This function Sends the WI mode specific HCI Event frames in the HCP packet format to the 
* connected reader device.
*/

static 
NFCSTATUS
phHciNfc_Send_WI_Event(
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

    if( (NULL == psHciContext)
        || ( pipe_id > PHHCINFC_MAX_PIPE)
        ||(NULL == psHciContext->p_pipe_list[pipe_id])
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
        HCI_DEBUG("%s: Invalid Arguments passed \n",
            "phHciNfc_Send_WI_Event");
    }
    else
    {
        p_pipe_info = (phHciNfc_Pipe_Info_t *) 
            psHciContext->p_pipe_list[pipe_id];
        psHciContext->tx_total = 0 ;
        length =length+HCP_HEADER_LEN ;

        if( NXP_EVT_SE_SWITCH_MODE == event)
        {
            hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;
            /* Construct the HCP Frame */
            phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                                    (uint8_t) pipe_id, HCP_MSG_TYPE_EVENT, event);
            hcp_message = &(hcp_packet->msg.message);
            phHciNfc_Append_HCPFrame((uint8_t *)hcp_message->payload,
                                            i, (uint8_t *)p_pipe_info->param_info,
                                            p_pipe_info->param_length);
            length =(uint8_t)(length + i + p_pipe_info->param_length);
        }
        else
        {
            status = PHNFCSTVAL( CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INSTRUCTION );
            HCI_DEBUG("%s: Invalid Send Event Request \n","phHciNfc_Send_WI_Event");
        }

        if( NFCSTATUS_SUCCESS == status )
        {
            p_pipe_info->sent_msg_type = HCP_MSG_TYPE_EVENT ;
            p_pipe_info->prev_msg = event ;
            psHciContext->tx_total = length;

            /* Send the Constructed HCP packet to the lower layer */
            status = phHciNfc_Send_HCP( psHciContext, pHwRef );
            p_pipe_info->prev_status = NFCSTATUS_PENDING;
        }
    }
    return status;
}

static 
NFCSTATUS
phHciNfc_Recv_WI_Response(
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
                                (phHciNfc_sContext_t *)psContext;


    if( (NULL == psHciContext) || (NULL == pHwRef) || (NULL == pResponse)
        || (length == 0))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_wi_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_WI_Info_t     *p_wiinfo=NULL;
        uint8_t                 prev_cmd = ANY_GET_PARAMETER;
        p_wiinfo = (phHciNfc_WI_Info_t *)psHciContext->p_wi_info ;

        if( NULL == p_wiinfo->p_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            prev_cmd = p_wiinfo->p_pipe_info->prev_msg ;
            switch(prev_cmd)
            {
                case ANY_GET_PARAMETER:
                {
                    if (length > HCP_HEADER_LEN)
                    {
                        status = phHciNfc_WI_InfoUpdate (psHciContext, 
                                        p_wiinfo->p_pipe_info->reg_index, 
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
                    HCI_PRINT("WI Parameter Set \n");
                    status = phHciNfc_EmuMgmt_Update_Seq(psHciContext, 
                                                        UPDATE_SEQ);
                    break;
                }
                case ANY_OPEN_PIPE:
                {
                    HCI_PRINT("WI gate open pipe complete\n");
                    status = phHciNfc_EmuMgmt_Update_Seq(psHciContext, 
                                                        UPDATE_SEQ);
                    break;
                }
                case ANY_CLOSE_PIPE:
                {
                    HCI_PRINT("WI close pipe complete\n");
                    status = phHciNfc_EmuMgmt_Update_Seq(psHciContext, 
                                                        UPDATE_SEQ);
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
                p_wiinfo->p_pipe_info->prev_status = NFCSTATUS_SUCCESS;
                p_wiinfo->current_seq = p_wiinfo->next_seq;
            }
        }
    }
    return status;
}

static
NFCSTATUS
phHciNfc_Recv_WI_Event(
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
    phHal_sEventInfo_t          EventInfo;
    /* phNfc_sNotificationInfo_t   NotificationInfo; */
    phHciNfc_sContext_t         *psHciContext =(phHciNfc_sContext_t *)psContext;


    if( (NULL == psHciContext) || (NULL == pHwRef) || (NULL == pEvent)
        || (length == 0))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_wi_info) 
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {   
        phHciNfc_HCP_Packet_t       *p_packet = NULL;
        phHciNfc_HCP_Message_t      *message = NULL;
        phHciNfc_WI_Info_t          *p_wi_info = NULL;
        uint8_t                     EventType = 0;                      

        p_wi_info = (phHciNfc_WI_Info_t *)psHciContext->p_wi_info ;

        p_packet = (phHciNfc_HCP_Packet_t *)pEvent;
        message = &(p_packet->msg.message);
        /* Get the instruction bits from the Message Header */
        EventType = (uint8_t) GET_BITS8( message->msg_header,
            HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);

        EventInfo.eventHost = phHal_eHostController;
        EventInfo.eventSource = phHal_ePICC_DevType;
        /* Now check for possible Transaction events for reporting */
        switch(EventType)
        {   
            case NXP_EVT_SE_START_OF_TRANSACTION:
            {
                EventInfo.eventType = NFC_EVT_START_OF_TRANSACTION;
                break;
            }
            case NXP_EVT_SE_END_OF_TRANSACTION:
            {   
                EventInfo.eventType = NFC_EVT_END_OF_TRANSACTION;
                break;
            }
            case NXP_EVT_SE_TRANSACTION:
            {
                EventInfo.eventType = NFC_EVT_TRANSACTION;
                EventInfo.eventInfo.aid.buffer = (uint8_t *)p_wi_info->aid;
                /* check for AID data is at least 1 byte is their */
                if (length > HCP_HEADER_LEN)
                {
                    EventInfo.eventInfo.aid.length = length - HCP_HEADER_LEN;
                    memcpy((void *)p_wi_info->aid, message->payload,
                           EventInfo.eventInfo.aid.length );
                }

                /* Filter Transaction event */
                if (EventInfo.eventInfo.aid.length == 4)
                {
                    EventInfo.eventType = NFC_EVT_APDU_RECEIVED;
                }
                else if (EventInfo.eventInfo.aid.length == 2)
                {
                    if (!memcmp(paypass_removal, EventInfo.eventInfo.aid.buffer, EventInfo.eventInfo.aid.length))
                    {
                        EventInfo.eventType = NFC_EVT_EMV_CARD_REMOVAL;
                    }
                    else if(mifare_access == EventInfo.eventInfo.aid.buffer[0])
                    {
                        EventInfo.eventType = NFC_EVT_MIFARE_ACCESS;
                    }
                }

                EventInfo.eventInfo.aid.buffer = (uint8_t *)p_wi_info->aid;
                (void) memcpy((void *)p_wi_info->aid,message->payload,
                                EventInfo.eventInfo.aid.length );
                break;
            }
            default:
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_INSTRUCTION);
                break;
            }
        }
        if (NFCSTATUS_SUCCESS == status )
        {
            phHciNfc_Notify_Event(  psHciContext, pHwRef, 
            NFC_NOTIFY_EVENT, (void*)&EventInfo);
        }
    }
    return status;
}

static 
NFCSTATUS 
phHciNfc_WI_InfoUpdate(
                       phHciNfc_sContext_t     *psHciContext,
                       uint8_t                 index,
                       uint8_t                 *reg_value,
                       uint8_t                 reg_length
                       )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phHciNfc_WI_Info_t      *p_wiinfo = NULL;

    p_wiinfo = psHciContext->p_wi_info;

    if ((NXP_SE_DEFAULTMODE_INDEX == index) &&
        (sizeof(*reg_value) == reg_length))
    {       
        p_wiinfo->default_type = *reg_value;
    } 
    else
    {
        status = PHNFCSTVAL(CID_NFC_HCI, 
                            NFCSTATUS_INVALID_HCI_RESPONSE);
    }

    return status;
}

