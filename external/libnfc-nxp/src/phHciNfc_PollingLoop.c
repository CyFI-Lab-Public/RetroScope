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
* \file  phHciNfc_PollingLoop.c                                               *
* \brief HCI polling loop Management Routines.                                *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Mon Mar 29 17:34:48 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.35 $                                                           *
* $Aliases: NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
*                                                                             *
* =========================================================================== *
*/

/*
***************************** Header File Inclusion ****************************
*/
#include <phNfcCompId.h>
#include <phNfcHalTypes.h>
#include <phHciNfc_Pipe.h>
#include <phHciNfc_PollingLoop.h>
#include <phOsalNfc.h>
/*
****************************** Macro Definitions *******************************
*/

/* Registry index to which command has to be sent */
#define PL_PAUSE_INDEX                  0x08U
#define PL_EMULATION_INDEX              0x07U
#define PL_RD_PHASES_INDEX              0x06U
#define PL_DISABLE_TARGET_INDEX         0x09U

/* Events */
#define NXP_EVT_CLK_ACK                 0x01U
#define NXP_EVT_CLK_REQUEST             0x02U
#define NXP_EVT_ACTIVATE_RDPHASES       0x03U
#define NXP_EVT_DEACTIVATE_RDPHASES     0x04U

/* Command length */
#define PL_DURATION_LENGTH              0x02U
#define PL_BYTE_LEN_1                   0x01U

#define PL_BIT_FIELD_ENABLED            0x01U


#define PL_EMULATION_FACTOR             0x0AU
/* Default duration  (100 ms * 1000) micro seconds, 
    always duration shall be less then 3145680 
    micro seconds */
#define PL_DEFAULT_DURATION             100000U
/* Maximum duration */
#define PL_MAX_DURATION                 3145000U
#define PL_DURATION_MIN_VALUE           48U
#define PL_DURATION_CALC(duration)      \
                    ((uint16_t)((duration)/PL_DURATION_MIN_VALUE))

/*
*************************** Structure and Enumeration ***************************
*/

typedef enum phHciNfc_Poll_Seq{
    PL_PIPE_OPEN                    =   0x00U,
    PL_PIPE_CLOSE,
    PL_SET_DURATION,
    PL_GET_DURATION,
    PL_GET_RD_PHASES,
    PL_SET_RD_PHASES,
    PL_GET_DISABLE_TARGET,
    PL_SET_DISABLE_TARGET,
    PL_END_SEQUENCE
} phHciNfc_Poll_Seq_t;

/* Information structure for the polling loop Gate */
typedef struct phHciNfc_PollLoop_Info{
    /* Current running Sequence of the polling loop Management */
    phHciNfc_Poll_Seq_t             current_seq;
    /* Next running Sequence of the polling loop Management */
    phHciNfc_Poll_Seq_t             next_seq;
    /* Pointer to the polling loop pipe information */
    phHciNfc_Pipe_Info_t            *p_pipe_info;
    uint8_t                         pipe_id;
} phHciNfc_PollLoop_Info_t;

/*
*************************** Static Function Declaration **************************
*/

static
NFCSTATUS
phHciNfc_PollLoop_InfoUpdate(
                                phHciNfc_sContext_t     *psHciContext,
                                uint8_t                 index,
                                uint8_t                 *reg_value,
                                uint8_t                 reg_length
                         );
/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Recv_Pl_Response function interprets the received polling loop
 *  response from the Host Controller Gate.
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in,out]  pResponse           Response received from the Host Cotroller
 *                                      polling loop gate.
 *  \param[in]  length                  length contains the length of the
 *                                      response received from the Host Controller.
 *
 *  \retval NFCSTATUS_PENDING           Polling loop gate Response to be received 
 *                                      is pending.
 *  \retval NFCSTATUS_SUCCESS           Polling loop gate Response received 
 *                                      Successfully.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

static
NFCSTATUS
phHciNfc_Recv_PollLoop_Response(
                        void                *psContext,
                        void                *pHwRef,
                        uint8_t             *pResponse,
#ifdef ONE_BYTE_LEN
                        uint8_t            length
#else
                        uint16_t           length
#endif
                       );

static
NFCSTATUS
phHciNfc_Recv_PollLoop_Event(
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
phHciNfc_PollLoop_Get_PipeID(
                                phHciNfc_sContext_t     *psHciContext,
                                uint8_t                 *ppipe_id
                           )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    
    if( (NULL != psHciContext)
        && ( NULL != ppipe_id )
        && ( NULL != psHciContext->p_poll_loop_info ) 
      )
    {
        phHciNfc_PollLoop_Info_t        *p_poll_info=NULL;
        p_poll_info = (phHciNfc_PollLoop_Info_t *)
                            psHciContext->p_poll_loop_info ;
        *ppipe_id =  p_poll_info->pipe_id  ;
    }
    else 
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    return status;
}

NFCSTATUS
phHciNfc_PollLoop_Init_Resources(
                                phHciNfc_sContext_t     *psHciContext
                         )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_PollLoop_Info_t    *p_poll_info=NULL;
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if( 
            ( NULL == psHciContext->p_poll_loop_info )
            && (phHciNfc_Allocate_Resource((void **)(&p_poll_info),
            sizeof(phHciNfc_PollLoop_Info_t))== NFCSTATUS_SUCCESS)
          )
        {
            psHciContext->p_poll_loop_info = p_poll_info;
            p_poll_info->current_seq = PL_PIPE_OPEN;
            p_poll_info->next_seq = PL_PIPE_CLOSE;
            p_poll_info->pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INSUFFICIENT_RESOURCES);
        }
        
    }
    return status;
}

/*!
 * \brief Initialisation of polling loop Gate and Establish the Session .
 *
 * This function initialses the polling loop Gates and 
 * all the required pipes and sets the Session ID
 * 
 */
NFCSTATUS
phHciNfc_PollLoop_Initialise(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                         )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if( NULL == psHciContext->p_poll_loop_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI,
                        NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            phHciNfc_PollLoop_Info_t    *p_poll_info=NULL;
            phHciNfc_Pipe_Info_t        *p_pipe_info = NULL;
            p_poll_info = (phHciNfc_PollLoop_Info_t *)
                                psHciContext->p_poll_loop_info ;
            p_pipe_info = p_poll_info->p_pipe_info;
            if(NULL == p_pipe_info )
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_INVALID_HCI_SEQUENCE);
            }
            else
            {
                HCI_PRINT("Polling loop open pipe in progress ...\n");
                status = phHciNfc_Open_Pipe( psHciContext,
                                            pHwRef, p_pipe_info );
                if(NFCSTATUS_SUCCESS == status)
                {
                    p_poll_info->next_seq = PL_PIPE_CLOSE;
                }
            }
        }
    }
    return status;
}

NFCSTATUS
phHciNfc_PollLoop_Release(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                     )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if( NULL != psHciContext->p_poll_loop_info )
        {
            phHciNfc_PollLoop_Info_t            *p_poll_info=NULL;
            p_poll_info = (phHciNfc_PollLoop_Info_t *)
                                psHciContext->p_poll_loop_info ;
            if (PL_PIPE_CLOSE == p_poll_info->current_seq)
            {
                phHciNfc_Pipe_Info_t            *p_pipe_info = NULL;
                p_pipe_info = p_poll_info->p_pipe_info;
                if(NULL == p_pipe_info )
                {
                    status = PHNFCSTVAL(CID_NFC_HCI, 
                        NFCSTATUS_INVALID_HCI_SEQUENCE);
                }
                else
                {                
                    HCI_PRINT("Polling loop close pipe in progress ...\n");
                    status = phHciNfc_Close_Pipe( psHciContext,
                                                pHwRef, p_pipe_info );
                    if(status == NFCSTATUS_SUCCESS)
                    {
                        p_poll_info->next_seq = PL_PIPE_OPEN;
                    }
                }
            }
            else
            {
                status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_SEQUENCE);
            } /* End of if (PL_PIPE_CLOSE == p_pl_info->cur_seq) */
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);          
        } /* End of if( NULL != psHciContext->p_poll_loop_info ) */
    } /* End of if( (NULL == psHciContext) || (NULL == pHwRef) ) */
    return status;
}

NFCSTATUS
phHciNfc_PollLoop_Cfg (
                        void                *psHciHandle,
                        void                *pHwRef, 
                        uint8_t             cfg_type, 
                        void                *pcfg_info
                     )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phHciNfc_sContext_t     *psHciContext = ((phHciNfc_sContext_t *)psHciHandle);
    uint8_t                 poll_cfg;
    static uint16_t         pl_duration = 0;

    /* To remove "warning (VS C4100) : unreferenced formal parameter" */
    PHNFC_UNUSED_VARIABLE(pcfg_info);

    if( (NULL == psHciContext) 
        || (NULL == pHwRef)
      )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL == psHciContext->p_poll_loop_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_PollLoop_Info_t    *p_poll_info=NULL;
        phHciNfc_Pipe_Info_t        *p_pipe_info=NULL;
        phHal_sADD_Cfg_t            *p_poll_cfg = NULL;
        uint8_t                     pipeid = 0;
        
        p_poll_cfg = (phHal_sADD_Cfg_t*)psHciContext->p_config_params;
        p_poll_info = (phHciNfc_PollLoop_Info_t *)
                                psHciContext->p_poll_loop_info ;
        p_pipe_info = p_poll_info->p_pipe_info;
        if((NULL == p_pipe_info) || (NULL == p_poll_cfg))
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                            NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            switch(cfg_type)
            {
                case PL_DURATION:
                {                    
                    /* 
                        Data memory has to be copied to 
                        param_info and also depending on the 
                        CARD_EMULATION or PAUSE, change the 
                        p_pipe_info->reg_index
                    */
                    if(p_poll_cfg->Duration > PL_MAX_DURATION)
                    {
                        p_poll_cfg->Duration = PL_MAX_DURATION;
                    }


                    if (FALSE == 
                        p_poll_cfg->PollDevInfo.PollCfgInfo.DisableCardEmulation)
                    {
                        p_poll_cfg->Duration = ((p_poll_cfg->Duration < 
                                                PL_DURATION_MIN_VALUE)? 
                                                (PL_DEFAULT_DURATION * 
                                                PL_EMULATION_FACTOR): 
                                                p_poll_cfg->Duration );
                        p_pipe_info->reg_index = PL_EMULATION_INDEX;
                    }
                    else
                    {
                        p_poll_cfg->Duration = ((p_poll_cfg->Duration <
                                                PL_DURATION_MIN_VALUE)? 
                                                PL_DEFAULT_DURATION : 
                                                p_poll_cfg->Duration);
                        p_pipe_info->reg_index = PL_PAUSE_INDEX;
                    }
                    p_pipe_info->param_length = PL_DURATION_LENGTH;

                    /* Calculate duration */
                    pl_duration = (uint16_t)
                                PL_DURATION_CALC(p_poll_cfg->Duration);
                    
                    /* Swap the 2 byte value */
                    pl_duration = (uint16_t)((pl_duration << BYTE_SIZE) | 
                                ((uint8_t)(pl_duration >> BYTE_SIZE)));
                    /* Copy the duration from poll config structure, 
                        provided by the upper layer */
                    p_pipe_info->param_info = (void *)&(pl_duration);
                    
                    pipeid = p_poll_info->pipe_id ;
                    if (PL_GET_DURATION == p_poll_info->current_seq)
                    {
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                            pipeid, (uint8_t)ANY_GET_PARAMETER);
                        if (NFCSTATUS_PENDING == status)
                        {
                            p_poll_info->next_seq = PL_PIPE_CLOSE;
                            status = NFCSTATUS_SUCCESS;
                        }
                    }
                    else
                    {
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                pipeid, (uint8_t)ANY_SET_PARAMETER);
                        if(NFCSTATUS_PENDING == status )
                        {
#ifdef ENABLE_VERIFY_PARAM
                            p_poll_info->next_seq = PL_GET_DURATION;
#else
                            status = NFCSTATUS_SUCCESS;
#endif /* #ifdef ENABLE_VERIFY_PARAM */
                        }
                    }               
                    break;
                }
                case PL_RD_PHASES:
                {
                    poll_cfg = (uint8_t) p_poll_cfg->PollDevInfo.PollEnabled;
                    p_pipe_info->param_length = PL_BYTE_LEN_1;
                    p_pipe_info->reg_index = PL_RD_PHASES_INDEX;
                    
                    /* Data memory has to be copied to 
                        param_info */
                    p_pipe_info->param_info = (void *)&(poll_cfg); 
                    pipeid = p_poll_info->pipe_id ;
                    if (PL_GET_RD_PHASES == p_poll_info->current_seq)
                    {
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                            pipeid, (uint8_t)ANY_GET_PARAMETER);
                        if (NFCSTATUS_PENDING == status)
                        {
                            status = NFCSTATUS_SUCCESS;
                        }
                    }
                    else
                    {
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                pipeid, (uint8_t)ANY_SET_PARAMETER);
                        if(NFCSTATUS_PENDING == status )
                        {
#ifdef ENABLE_VERIFY_PARAM
                            p_poll_info->next_seq = PL_GET_RD_PHASES;
#else                           
                            status = NFCSTATUS_SUCCESS;
#endif /* #ifdef ENABLE_VERIFY_PARAM */
                        }
                    }
                    break;
                }
                case PL_DISABLE_TARGET:
                {
                    if (NULL == pcfg_info)
                    {
                        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
                    } 
                    else
                    {
                        /* poll_cfg = (uint8_t) p_poll_cfg->NfcIP_Tgt_Disable; */
                        p_pipe_info->param_length = PL_BYTE_LEN_1;
                        p_pipe_info->reg_index = PL_DISABLE_TARGET_INDEX;

                        /* Data memory has to be copied to 
                        param_info */
                        p_pipe_info->param_info = pcfg_info; 
                        pipeid = p_poll_info->pipe_id ;
                        if (PL_GET_DISABLE_TARGET == p_poll_info->current_seq)
                        {
                            status = 
                                phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                pipeid, (uint8_t)ANY_GET_PARAMETER);
                            if (NFCSTATUS_PENDING == status)
                            {
                                status = NFCSTATUS_SUCCESS;
                            }
                        }
                        else
                        {
                            status = 
                                phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                pipeid, (uint8_t)ANY_SET_PARAMETER);
                            if( NFCSTATUS_PENDING == status )
                            {
#ifdef ENABLE_VERIFY_PARAM
                                /* p_poll_info->current_seq = PL_GET_DISABLE_TARGET; */
                                p_poll_info->next_seq = PL_GET_DISABLE_TARGET;
#else
                                status = NFCSTATUS_SUCCESS;
#endif /* #ifdef ENABLE_VERIFY_PARAM */
                            }
                        }
                    }
                    break;
                }
                default:
                {
                    status = PHNFCSTVAL(CID_NFC_HCI, 
                                    NFCSTATUS_INVALID_PARAMETER);
                    break;
                }
            }
        }       
    }
    return status;
}

/* Function to assign pipe ID */
NFCSTATUS
phHciNfc_PollLoop_Update_PipeInfo(
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
    else if ( NULL == psHciContext->p_poll_loop_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_PollLoop_Info_t    *p_poll_info=NULL;
        p_poll_info = (phHciNfc_PollLoop_Info_t *)
                                psHciContext->p_poll_loop_info ;
        /* Update the pipe_id of the ID Mgmt Gate obtained from the HCI Response */
        p_poll_info->pipe_id = pipeID;
        p_poll_info->p_pipe_info = pPipeInfo;
        if (NULL != pPipeInfo)
        {
            /* Update the Response Receive routine of the IDMgmt Gate */
            pPipeInfo->recv_resp = &phHciNfc_Recv_PollLoop_Response;
            /* Update the event Receive routine of the IDMgmt Gate */
            pPipeInfo->recv_event = &phHciNfc_Recv_PollLoop_Event;
        }
    }

    return status;
}

static 
NFCSTATUS
phHciNfc_Recv_PollLoop_Response(
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
                                (phHciNfc_sContext_t *)psContext ;
    

    if( (NULL == psHciContext) || (NULL == pHwRef) || (NULL == pResponse)
        || (length == 0))
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(  NULL == psHciContext->p_poll_loop_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_PollLoop_Info_t    *p_poll_info=NULL;
        uint8_t                     prev_cmd = ANY_GET_PARAMETER;
        p_poll_info = (phHciNfc_PollLoop_Info_t *)
                            psHciContext->p_poll_loop_info ;
        if( NULL == p_poll_info->p_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                        NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            prev_cmd = p_poll_info->p_pipe_info->prev_msg ;
            switch(prev_cmd)
            {
                case ANY_SET_PARAMETER:
                {
                    HCI_PRINT("Polling loop Set Param complete\n");
                    break;
                }
                case ANY_GET_PARAMETER:
                {
                    status = phHciNfc_PollLoop_InfoUpdate(psHciContext,
                                p_poll_info->p_pipe_info->reg_index, 
                                &pResponse[HCP_HEADER_LEN],
                                    (uint8_t)(length - HCP_HEADER_LEN));
                    break;
                }
                case ANY_OPEN_PIPE:
                {
                    HCI_PRINT("Polling loop open pipe complete\n");
                    break;
                }
                case ANY_CLOSE_PIPE:
                {
                    HCI_PRINT("Polling loop close pipe complete\n");
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
                p_poll_info->p_pipe_info->prev_status = NFCSTATUS_SUCCESS;
                p_poll_info->current_seq = p_poll_info->next_seq;
            }
        }
    }
    return status;
}

static
NFCSTATUS
phHciNfc_Recv_PollLoop_Event(
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
        || (length <= HCP_HEADER_LEN))
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(  NULL == psHciContext->p_poll_loop_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_HCP_Packet_t       *p_packet = NULL;
        phHciNfc_PollLoop_Info_t    *p_poll_info=NULL;
        phHciNfc_HCP_Message_t      *message = NULL;
        static phHal_sEventInfo_t   event_info;
        uint8_t                     instruction=0;

        p_poll_info = (phHciNfc_PollLoop_Info_t *)
                        psHciContext->p_poll_loop_info ;

        PHNFC_UNUSED_VARIABLE(p_poll_info);
        p_packet = (phHciNfc_HCP_Packet_t *)pEvent;
        message = &p_packet->msg.message;
        /* Get the instruction bits from the Message Header */
        instruction = (uint8_t) GET_BITS8( message->msg_header,
            HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);

        switch(instruction)
        {
            case NXP_EVT_CLK_ACK:
            {               
                break;
            }
            case NXP_EVT_CLK_REQUEST:
            {
                break;
            }
            case NXP_EVT_ACTIVATE_RDPHASES:
            {
                HCI_PRINT("Polling loop activate read phase complete\n");
                event_info.eventHost = phHal_eHostController;
                event_info.eventType = NFC_UICC_RDPHASES_ACTIVATE_REQ;
                event_info.eventInfo.rd_phases = pEvent[HCP_HEADER_LEN];
                ((phHal_sHwReference_t *)pHwRef)->uicc_rdr_active = TRUE;
                phHciNfc_Notify_Event((void *)psHciContext, 
                                            pHwRef, 
                                            NFC_NOTIFY_EVENT, 
                                            &(event_info));
                break;
            }
            case NXP_EVT_DEACTIVATE_RDPHASES:
            {
                HCI_PRINT("Polling loop deactivate read phase complete\n");
                event_info.eventHost = phHal_eHostController;
                event_info.eventType = NFC_UICC_RDPHASES_DEACTIVATE_REQ;
                event_info.eventInfo.rd_phases = pEvent[HCP_HEADER_LEN];
                ((phHal_sHwReference_t *)pHwRef)->uicc_rdr_active = FALSE;
                phHciNfc_Notify_Event((void *)psHciContext, 
                                            pHwRef, 
                                            NFC_NOTIFY_EVENT, 
                                            &(event_info));
                break;
            }
            default:
            {
                status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_RESPONSE);
                break;
            }
        }
    }
    return status;
}


static
NFCSTATUS
phHciNfc_PollLoop_InfoUpdate(
                                phHciNfc_sContext_t     *psHciContext,
                                uint8_t                 index,
                                uint8_t                 *reg_value,
                                uint8_t                 reg_length
                          )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_PollLoop_Info_t    *p_poll_info=NULL;
    p_poll_info = (phHciNfc_PollLoop_Info_t *)
                            (psHciContext->p_poll_loop_info );
    /* To remove "warning (VS 4100) : unreferenced formal parameter" */
    PHNFC_UNUSED_VARIABLE(reg_value);
    PHNFC_UNUSED_VARIABLE(reg_length);
    /* Variable was set but never used (ARM warning) */
    PHNFC_UNUSED_VARIABLE(p_poll_info);
    switch(index)
    {
        case PL_EMULATION_INDEX:
        case PL_PAUSE_INDEX:
        {
            HCI_PRINT_BUFFER("\tPoll duration", reg_value, reg_length);
            break;
        }
        case PL_RD_PHASES_INDEX:
        {
            HCI_PRINT_BUFFER("\tPoll read phase", reg_value, reg_length);
            break;
        }
#if defined (CLK_REQUEST)
        case PL_CLK_REQUEST_INDEX:
        {
            HCI_PRINT_BUFFER("\tPoll clock request", reg_value, reg_length);
            break;
        }
#endif /* #if defined (CLK_REQUEST) */
#if defined (INPUT_CLK)
        case PL_INPUT_CLK_INDEX:
        {
            HCI_PRINT_BUFFER("\tPoll input clock", reg_value, reg_length);
            break;
        }
#endif/* #if defined (INPUT_CLK) */
        default:
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_RESPONSE);
            break;
        }
    }
    return status;
}


