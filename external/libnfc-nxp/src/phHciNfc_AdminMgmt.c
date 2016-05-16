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
* \file  hHciNfc_AdminMgmt.c                                                  *
* \brief HCI Admin Gate Management Routines.                                  *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Mon Apr  5 19:23:34 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.47 $                                                           *
* $Aliases: NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
*                                                                             *
* =========================================================================== *
*/

/*
***************************** Header File Inclusion ****************************
*/
#include <phNfcCompId.h>
#include <phHciNfc_Pipe.h>
#include <phHciNfc_AdminMgmt.h>
#include <phHciNfc_DevMgmt.h>
#include <phOsalNfc.h>
/*
****************************** Macro Definitions *******************************
*/

#define SESSION_INDEX       0x01U
#define MAX_PIPE_INDEX      0x02U
#define WHITELIST_INDEX     0x03U
#define HOST_LIST_INDEX     0x04U

/* Max Whitelist Supported by the Device*/
#define SESSIONID_LEN       0x08U
#define WHITELIST_MAX_LEN   0x03U
#define HOST_LIST_MAX_LEN   0x05U

/* Address Definitions for HW Configuration */
#define NFC_ADDRESS_UICC_SESSION        0x9EA2U



/*
*************************** Structure and Enumeration ***************************
*/

typedef enum phHciNfc_Admin_Seq{
    ADMIN_PIPE_OPEN     = 0x00U,
    ADMIN_GET_HOST_LIST,
    ADMIN_GET_WHITE_LIST,
    ADMIN_GET_SESSION,
    ADMIN_VERIFY_SESSION,
    ADMIN_CLEAR_UICC_PIPES,
    ADMIN_CLEAR_PIPES,
    ADMIN_PIPE_REOPEN,
    ADMIN_CREATE_PIPES,
    ADMIN_SET_SESSION,
    ADMIN_SET_WHITE_LIST,
    ADMIN_UPDATE_PIPES,
    ADMIN_PIPE_CLOSE,
    ADMIN_DELETE_PIPES,
    ADMIN_END_SEQUENCE
} phHciNfc_Admin_Seq_t;


/* Information structure for the Admin Gate */
typedef struct phHciNfc_AdminGate_Info{
    /* Current running Sequence of the Admin Management */
    phHciNfc_Admin_Seq_t            current_seq;
    /* Next running Sequence of the Admin Management */
    phHciNfc_Admin_Seq_t            next_seq;
    /* Pointer to the Admin Pipe Information */
    phHciNfc_Pipe_Info_t            *admin_pipe_info;
    /* Sequence for the Pipe Initialisation */
    phHciNfc_PipeMgmt_Seq_t         pipe_seq;
    /* Session ID of the Device */
    uint8_t                         session_id[SESSIONID_LEN];
    /* Max number of pipes that can be created on the Device */
    uint8_t                         max_pipe;
    /* List of Hosts that can be access the device Admin Gate. */
    uint8_t                         whitelist[WHITELIST_MAX_LEN];
    /* Host List from the Host Controller */
    uint8_t                         host_list[HOST_LIST_MAX_LEN];
} phHciNfc_AdminGate_Info_t;

/*
*************************** Static Function Declaration **************************
*/

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Recv_Admin_Response function interprets the received AdminGate
 *  response from the Host Controller Gate.
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in,out]  pResponse           Response received from the Host Cotroller
 *                                      Admin gate.
 *  \param[in]  length                  length contains the length of the
 *                                      response received from the Host Controller.
 *
 *  \retval NFCSTATUS_PENDING           AdminGate Response to be received is pending.
 *  \retval NFCSTATUS_SUCCESS           AdminGate Response received Successfully.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

static
NFCSTATUS
phHciNfc_Recv_Admin_Response(
                        void                *psHciContext,
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
phHciNfc_Admin_InfoUpdate(
                                phHciNfc_sContext_t     *psHciContext,
                                phHal_sHwReference_t    *pHwRef,
                                uint8_t                 index,
                                uint8_t                 *reg_value,
                                uint8_t                 reg_length
                         );

static
 NFCSTATUS
 phHciNfc_Recv_Admin_Cmd (
                        void                *psContext,
                        void                *pHwRef,
                        uint8_t             *pCmd,
#ifdef ONE_BYTE_LEN
                        uint8_t             length
#else
                        uint16_t            length
#endif
                     );


static
 NFCSTATUS
 phHciNfc_Recv_Admin_Event (
                        void                *psContext,
                        void                *pHwRef,
                        uint8_t             *pEvent,
#ifdef ONE_BYTE_LEN
                        uint8_t             length
#else
                        uint16_t            length
#endif
                     );


/*
*************************** Function Definitions ***************************
*/



/*!
 * \brief Initialisation of Admin Gate and Establish the Session .
 *
 * This function initialses the Admin Gates and Establishes the Session by creating
 * all the required pipes and sets the Session ID
 * 
 */

NFCSTATUS
phHciNfc_Admin_Initialise(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                         )
{
    NFCSTATUS                           status = NFCSTATUS_SUCCESS;
    phHciNfc_Pipe_Info_t                *p_pipe_info = NULL;
    phHciNfc_AdminGate_Info_t           *p_admin_info=NULL;
    uint8_t                             length = 0;

    if( (NULL == psHciContext)
        || (NULL == pHwRef )
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if( ( NULL == psHciContext->p_admin_info )
            && (phHciNfc_Allocate_Resource((void **)(&p_admin_info),
                    sizeof(phHciNfc_AdminGate_Info_t))== NFCSTATUS_SUCCESS)
          )
        {
            psHciContext->p_admin_info = (void *) p_admin_info;
            p_admin_info->current_seq = ADMIN_PIPE_OPEN;
            p_admin_info->next_seq = ADMIN_END_SEQUENCE;
            p_admin_info->admin_pipe_info = NULL;
        }
        else
        {
            p_admin_info = (phHciNfc_AdminGate_Info_t * )
                                psHciContext->p_admin_info ;
        }

        if( NULL == p_admin_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI,
                        NFCSTATUS_INSUFFICIENT_RESOURCES);
        }
        else
        {
            switch(p_admin_info->current_seq)
            {
                /* Admin pipe open sequence , Initially open the Admin Pipe */
                case ADMIN_PIPE_OPEN:
                {
                    if(phHciNfc_Allocate_Resource((void **)(&p_pipe_info),
                        sizeof(phHciNfc_Pipe_Info_t))!= NFCSTATUS_SUCCESS)
                    {
                        status = PHNFCSTVAL(CID_NFC_HCI,
                                NFCSTATUS_INSUFFICIENT_RESOURCES);
                    }
                    else
                    {
                        /* Populate the pipe information in the pipe handle */
                        ((phHciNfc_Pipe_Info_t *)p_pipe_info)->pipe.pipe_id = 
                                        PIPETYPE_STATIC_ADMIN;
                        ((phHciNfc_Pipe_Info_t *)p_pipe_info)->recv_resp = 
                                        &phHciNfc_Recv_Admin_Response;
                        ((phHciNfc_Pipe_Info_t *)p_pipe_info)->recv_cmd = 
                                        &phHciNfc_Recv_Admin_Cmd;
                        ((phHciNfc_Pipe_Info_t *)p_pipe_info)->recv_event = 
                                        &phHciNfc_Recv_Admin_Event;
                        psHciContext->p_pipe_list[PIPETYPE_STATIC_ADMIN] =
                                                                    p_pipe_info ;
                        status = phHciNfc_Open_Pipe( psHciContext,
                                                            pHwRef,p_pipe_info );
                        if(status == NFCSTATUS_SUCCESS)
                        {
                            p_admin_info->admin_pipe_info = p_pipe_info ;
                            p_admin_info->next_seq = ADMIN_GET_SESSION;
                            status = NFCSTATUS_PENDING;
                        }
                    }
                    break;
                }
                case ADMIN_GET_SESSION:
                {
                    p_pipe_info = p_admin_info->admin_pipe_info;
                    p_pipe_info->reg_index = SESSION_INDEX;
                    p_pipe_info->prev_status = 
                        phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                            (uint8_t)HCI_ADMIN_PIPE_ID,
                                (uint8_t)ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == p_pipe_info->prev_status )
                    {
#ifdef UICC_SESSION_RESET
                        p_admin_info->next_seq = ADMIN_CLEAR_UICC_PIPES;
#elif defined (ESTABLISH_SESSION)
                        p_admin_info->next_seq = ADMIN_VERIFY_SESSION;
#else
                        p_admin_info->next_seq = ADMIN_CLEAR_PIPES;
#endif
                        status = NFCSTATUS_PENDING;
                    }
                    break;
                }
#ifdef UICC_SESSION_RESET
                case ADMIN_CLEAR_UICC_PIPES:
                {
                    uint8_t config = 0x00;
                    p_pipe_info = p_admin_info->admin_pipe_info;
                     /* TODO: Implement the Clear UICC PIPES Using
                      * Memory configuration.
                      */
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_UICC_SESSION , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_admin_info->next_seq = ADMIN_CLEAR_PIPES;
                        status = NFCSTATUS_PENDING;
                    }
                    break;
                }
#endif
                case ADMIN_VERIFY_SESSION:
                {
                    phHal_sHwConfig_t *p_hw_config = 
                             (phHal_sHwConfig_t *) psHciContext->p_config_params;
                    phHal_sHwReference_t *p_hw_ref = 
                             (phHal_sHwReference_t *) pHwRef;
                    int             cmp_val = 0;
                    p_pipe_info = p_admin_info->admin_pipe_info;
                    cmp_val = phOsalNfc_MemCompare(p_hw_config->session_id , 
                                 p_hw_ref->session_id , 
                                         sizeof(p_hw_ref->session_id));
                    if((cmp_val == 0) 
                        && ( HCI_SESSION == psHciContext->init_mode)
                        )
                    {
                        psHciContext->hci_mode = hciMode_Session;
                        status = phHciNfc_Update_Pipe( psHciContext, pHwRef,
                                                &p_admin_info->pipe_seq );
                        if((status == NFCSTATUS_SUCCESS) 
                            && (NULL != p_pipe_info))
                        {
                            
                            p_pipe_info->reg_index = MAX_PIPE_INDEX;
                            status = phHciNfc_Send_Generic_Cmd( psHciContext,  
                                    pHwRef, (uint8_t)HCI_ADMIN_PIPE_ID,
                                                    (uint8_t)ANY_GET_PARAMETER );
                            p_pipe_info->prev_status = status;
                            if(NFCSTATUS_PENDING == status )
                            {
                                p_admin_info->next_seq = ADMIN_PIPE_CLOSE;
                                status = NFCSTATUS_SUCCESS;
                            }
                        }
                        else
                        {
                            status = PHNFCSTVAL(CID_NFC_HCI, 
                                            NFCSTATUS_INVALID_HCI_SEQUENCE);
                        }
                        break;
                    }
                    else
                    {
                        /* To clear the pipe information*/
                        psHciContext->hci_mode = hciMode_Override;
                        p_admin_info->current_seq = ADMIN_CLEAR_PIPES;
                    }
                }
                /* fall through */
                case ADMIN_CLEAR_PIPES:
                {
                    p_pipe_info = p_admin_info->admin_pipe_info;
                    p_pipe_info->prev_status = 
                                    phHciNfc_Send_Admin_Cmd( psHciContext,
                                        pHwRef, ADM_CLEAR_ALL_PIPE,
                                            length, p_pipe_info);
                    status = ((p_pipe_info->prev_status == NFCSTATUS_PENDING)?
                                            NFCSTATUS_SUCCESS : 
                                                p_pipe_info->prev_status);
                    if(status == NFCSTATUS_SUCCESS) 
                    {
                        p_admin_info->next_seq = ADMIN_PIPE_REOPEN;
                        status = NFCSTATUS_PENDING;
                    }
                    break;
                }
                /* Admin pipe Re-Open sequence , Re-Open the Admin Pipe */
                case ADMIN_PIPE_REOPEN:
                {
                    p_pipe_info = p_admin_info->admin_pipe_info;
                    status = phHciNfc_Open_Pipe( psHciContext,
                                                        pHwRef,p_pipe_info );
                    if(status == NFCSTATUS_SUCCESS)
                    {
                        p_admin_info->next_seq = ADMIN_CREATE_PIPES;
                        status = NFCSTATUS_PENDING;
                    }
                    break;
                }
                case ADMIN_CREATE_PIPES:
                {
                    status = phHciNfc_Create_All_Pipes( psHciContext, pHwRef,
                                                        &p_admin_info->pipe_seq );
                    if(status == NFCSTATUS_SUCCESS) 
                    {
                        p_admin_info->next_seq = ADMIN_GET_WHITE_LIST;
                        status = NFCSTATUS_PENDING;
                    }
                    break;
                }
                case ADMIN_GET_WHITE_LIST:
                {
                    p_pipe_info = p_admin_info->admin_pipe_info;
                    if(NULL == p_pipe_info )
                    {
                        status = PHNFCSTVAL(CID_NFC_HCI, 
                                        NFCSTATUS_INVALID_HCI_SEQUENCE);
                    }
                    else
                    {
                        p_pipe_info->reg_index = WHITELIST_INDEX;
                        status = phHciNfc_Send_Generic_Cmd( psHciContext,  
                                pHwRef, (uint8_t)HCI_ADMIN_PIPE_ID,
                                                (uint8_t)ANY_GET_PARAMETER );
                        p_pipe_info->prev_status = status;
                        if(HCI_SELF_TEST == psHciContext->init_mode)
                        {
                            status = ((NFCSTATUS_PENDING == status )?
                                            NFCSTATUS_SUCCESS : status);
                        }
                        else 
                        {
                            if(NFCSTATUS_PENDING == status )
                            {
                                p_admin_info->next_seq = ADMIN_GET_HOST_LIST;
                                /* status = NFCSTATUS_SUCCESS; */
                            }
                        }
                    }
                    break;
                }
                case ADMIN_GET_HOST_LIST:
                {
                    p_pipe_info = p_admin_info->admin_pipe_info;
                    if(NULL == p_pipe_info )
                    {
                        status = PHNFCSTVAL(CID_NFC_HCI, 
                                        NFCSTATUS_INVALID_HCI_SEQUENCE);
                    }
                    else
                    {
                        p_pipe_info->reg_index = HOST_LIST_INDEX;
                        status = phHciNfc_Send_Generic_Cmd( psHciContext,  
                                pHwRef, (uint8_t)HCI_ADMIN_PIPE_ID,
                                                (uint8_t)ANY_GET_PARAMETER );
                        p_pipe_info->prev_status = status;
                        if(NFCSTATUS_PENDING == status )
                        {

#if defined(HOST_WHITELIST)
                            p_admin_info->next_seq = ADMIN_SET_WHITE_LIST;
#else
                            p_admin_info->next_seq = ADMIN_SET_SESSION;
                            status = NFCSTATUS_SUCCESS;
#endif
                        }
                    }
                    break;
                }
                case ADMIN_SET_WHITE_LIST:
                {
                    p_pipe_info = p_admin_info->admin_pipe_info;
                    if(NULL == p_pipe_info )
                    {
                        status = PHNFCSTVAL(CID_NFC_HCI, 
                                        NFCSTATUS_INVALID_HCI_SEQUENCE);
                    }
                    else
                    {
                        uint8_t             i = 0;

                        for (i = 0; i < WHITELIST_MAX_LEN - 2; i++ )
                        {
                            p_admin_info->whitelist[i] = i + 2;
                        }
                        status = phHciNfc_Set_Param(psHciContext, pHwRef,
                                      p_pipe_info, WHITELIST_INDEX, 
                                        (uint8_t *)p_admin_info->whitelist, i );
                        if(NFCSTATUS_PENDING == status )
                        {
                            p_admin_info->next_seq = ADMIN_SET_SESSION;
                            status = NFCSTATUS_SUCCESS;
                        }
                    }
                    break;
                }
                case ADMIN_SET_SESSION:
                {
                    phHal_sHwConfig_t *p_hw_config = 
                             (phHal_sHwConfig_t *) psHciContext->p_config_params;
                    p_pipe_info = p_admin_info->admin_pipe_info;
                    status = phHciNfc_Set_Param(psHciContext, pHwRef, p_pipe_info,
                        SESSION_INDEX, (uint8_t *)(p_hw_config->session_id),
                            sizeof(p_hw_config->session_id));
                    if(NFCSTATUS_PENDING == p_pipe_info->prev_status )
                    {
                        p_admin_info->next_seq = ADMIN_PIPE_CLOSE;
                        status = NFCSTATUS_SUCCESS;
                    }
                    break;
                }
                default:
                {
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_SEQUENCE);
                    break;
                }

            }/* End of the Sequence Switch */

        }/* End of the Admin Info Memory Check */

    }/* End of Null context Check */

    return status;
}

#ifdef HOST_EMULATION

/*!
 * \brief Creates the Card Emulation Gate Pipes .
 *
 * This function Creates the Card Emulation Gate.
 */

NFCSTATUS
phHciNfc_Admin_CE_Init(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef,
                                phHciNfc_GateID_t       ce_gate

                             )
{
    NFCSTATUS                           status = NFCSTATUS_SUCCESS;
    /* phHciNfc_Pipe_Info_t             *pipe_info = NULL; */
    phHciNfc_AdminGate_Info_t           *p_admin_info=NULL;

    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if( NULL != psHciContext->p_admin_info )
        {
            p_admin_info = psHciContext->p_admin_info;

            switch(ce_gate)
            {
                /* Card Emulation A Gate Pipe Creation */
                case phHciNfc_CETypeAGate:
                {
                    p_admin_info->pipe_seq = PIPE_CARD_A_CREATE;
                    break;
                }
                /* Card Emulation B Gate Pipe Creation */
                case phHciNfc_CETypeBGate:
                {
                    p_admin_info->pipe_seq = PIPE_CARD_B_CREATE;
                    break;
                }
                default:
                {
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_HCI_GATE_NOT_SUPPORTED);
                    break;
                }
            } /* End of CE Gate Switch */

            if (NFCSTATUS_SUCCESS == status)
            {
                status = phHciNfc_CE_Pipes_OP( psHciContext,
                                        pHwRef, &p_admin_info->pipe_seq );
                if(status == NFCSTATUS_SUCCESS)
                {
                    p_admin_info->next_seq = ADMIN_END_SEQUENCE;
                    /* status = NFCSTATUS_PENDING; */
                }
            }

        }/* End of NULL Check for the Admin_Info */
    } /* End of Null Check for the Context */
    return status;
}

#endif

/*!
 * \brief Releases the resources allocated the Admin Management.
 *
 * This function Releases the resources allocated the Admin Management
 * and resets the hardware to the reset state.
 */

NFCSTATUS
phHciNfc_Admin_Release(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef,
                                phHciNfc_HostID_t        host_type
                             )
{
    NFCSTATUS                           status = NFCSTATUS_SUCCESS;
    phHciNfc_Pipe_Info_t                *p_pipe_info = NULL;

    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if( NULL != psHciContext->p_admin_info )
        {
            if(phHciNfc_UICCHostID != host_type)
            {
                p_pipe_info = psHciContext->p_pipe_list[PIPETYPE_STATIC_ADMIN];

                status = phHciNfc_Close_Pipe( psHciContext,
                                                    pHwRef, p_pipe_info );
            }

        }/* End of NULL Check for the Admin_Info */
    } /* End of Null Check for the Context */
    return status;
}


/*!
 * \brief Sends the HCI Admin Event to the corresponding peripheral device.
 *
 * This function sends the HCI Admin Events to the connected NFC Pheripheral
 * device
 */

 NFCSTATUS
 phHciNfc_Send_Admin_Event (
                      phHciNfc_sContext_t   *psHciContext,
                      void                  *pHwRef,
                      uint8_t               event,
                      uint8_t               length,
                      void                  *params
                     )
{
    phHciNfc_HCP_Packet_t       *hcp_packet = NULL;
    phHciNfc_AdminGate_Info_t   *p_admin_info=NULL;
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) 
        || (NULL == pHwRef) 
      )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        psHciContext->tx_total = 0 ;
        length +=  HCP_HEADER_LEN ;
        p_admin_info = psHciContext->p_admin_info;

        if( EVT_HOT_PLUG ==   event )
        {

            /* Use the HCP Packet Structure to Construct the send HCP
                * Packet data.
                */
            hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;
            phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                                    (uint8_t) HCI_ADMIN_PIPE_ID,
                                    HCP_MSG_TYPE_EVENT, event);
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INSTRUCTION);
        }

        if( NFCSTATUS_SUCCESS == status )
        {
            p_admin_info->admin_pipe_info->sent_msg_type = HCP_MSG_TYPE_EVENT;
            p_admin_info->admin_pipe_info->prev_msg = event ;
            p_admin_info->admin_pipe_info->param_info = params ;
            psHciContext->tx_total = length;
            psHciContext->response_pending = FALSE ;
            status = phHciNfc_Send_HCP( (void *)psHciContext, (void *)pHwRef );
            p_admin_info->admin_pipe_info->prev_status = NFCSTATUS_PENDING;
        }
    }

    return status;
}


/*!
 * \brief Sends the HCI Admin Commands to the corresponding peripheral device.
 *
 * This function sends the HCI Admin Commands to the connected NFC Pheripheral
 * device
 */

 NFCSTATUS
 phHciNfc_Send_Admin_Cmd (
                      phHciNfc_sContext_t   *psHciContext,
                      void                  *pHwRef,
                      uint8_t               cmd,
                      uint8_t               length,
                      void                  *params
                     )
{
    phHciNfc_HCP_Packet_t       *hcp_packet = NULL;
    phHciNfc_HCP_Message_t      *hcp_message = NULL;
    phHciNfc_AdminGate_Info_t   *p_admin_info=NULL;
    phHciNfc_Pipe_Info_t        *p_pipe_info = NULL;
    uint8_t                     i=0;
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) 
        || (NULL == pHwRef) 
        || (NULL == params)
      )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        p_pipe_info = (phHciNfc_Pipe_Info_t *)  params;
        psHciContext->tx_total = 0 ;
        length +=  HCP_HEADER_LEN ;
        p_admin_info = psHciContext->p_admin_info;
        switch(  cmd )
        {
            case ADM_CREATE_PIPE:
            {
                hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;
                /* Use the HCP Packet Structure to Construct the send HCP
                * Packet data.
                */
                phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                                        (uint8_t) HCI_ADMIN_PIPE_ID,
                                        HCP_MSG_TYPE_COMMAND, cmd);
                hcp_message = &(hcp_packet->msg.message);

                /* Source HOST ID Parameter is not passed as a 
                 * parameter in the HCI SPEC */

                /* hcp_message->payload[i++] = p_pipe_info->pipe.source.host_id; */
                hcp_message->payload[i++] = p_pipe_info->pipe.source.gate_id;
                hcp_message->payload[i++] = p_pipe_info->pipe.dest.host_id;
                hcp_message->payload[i++] = p_pipe_info->pipe.dest.gate_id;
                break;
            }
            case ADM_DELETE_PIPE:
            {
                uint8_t     pipe_id = (uint8_t) HCI_UNKNOWN_PIPE_ID;

                pipe_id = p_pipe_info->pipe.pipe_id;
                if( pipe_id < PIPETYPE_DYNAMIC )
                {
                    /* The Static Pipes cannot be Deleted */
                    status = PHNFCSTVAL(CID_NFC_HCI,
                                    NFCSTATUS_INVALID_PARAMETER );
                    HCI_DEBUG("phHciNfc_Send_Admin_Cmd: Static Pipe %u "
                                                "Cannot be Deleted \n",pipe_id);
                }
                else
                {

                    /* Use the HCP Packet Structure to Construct the send HCP
                     * Packet data.
                     */
                    hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;
                    phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                                            (uint8_t) HCI_ADMIN_PIPE_ID,
                                            HCP_MSG_TYPE_COMMAND, cmd);
                    hcp_message = &(hcp_packet->msg.message);
                    hcp_message->payload[i++] = pipe_id ;
                }
                break;
            }
            case ADM_CLEAR_ALL_PIPE:
            {

                /* Use the HCP Packet Structure to Construct the send HCP
                 * Packet data.
                 */
                hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;
                phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                                        (uint8_t) HCI_ADMIN_PIPE_ID,
                                        HCP_MSG_TYPE_COMMAND, cmd);
                hcp_message = &(hcp_packet->msg.message);
                break;
            }
            /* These are notifications and can not be sent by the Host */
            /* case ADM_NOTIFY_PIPE_CREATED: */
            /* case ADM_NOTIFY_PIPE_DELETED: */
            /* case ADM_NOTIFY_ALL_PIPE_CLEARED: */
            default:
                status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_COMMAND);
                break;
        }
        if( NFCSTATUS_SUCCESS == status )
        {
            p_admin_info->admin_pipe_info->sent_msg_type = HCP_MSG_TYPE_COMMAND;
            p_admin_info->admin_pipe_info->prev_msg = cmd;
            p_admin_info->admin_pipe_info->param_info = p_pipe_info;
            psHciContext->tx_total = length;
            psHciContext->response_pending = TRUE;
            status = phHciNfc_Send_HCP( (void *)psHciContext, (void *)pHwRef );
            p_admin_info->admin_pipe_info->prev_status = NFCSTATUS_PENDING;
        }
    }

    return status;
}


/*!
 * \brief Receives the HCI Response from the corresponding peripheral device.
 *
 * This function receives the HCI Command Response from the connected NFC
 * Pheripheral device.
 */

static
NFCSTATUS
phHciNfc_Recv_Admin_Response(
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
    phHciNfc_sContext_t         *psHciContext = 
                                    (phHciNfc_sContext_t *)psContext ;
    phHciNfc_HCP_Packet_t       *hcp_packet = NULL;
    phHciNfc_HCP_Message_t      *hcp_message = NULL;
    phHciNfc_Pipe_Info_t        *p_pipe_info = NULL;
    phHciNfc_AdminGate_Info_t   *p_admin_info = NULL;
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    uint8_t                     pipe_id = (uint8_t) HCI_UNKNOWN_PIPE_ID;
    uint8_t                     prev_cmd = 0;
    NFCSTATUS                   prev_status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if ( NULL == psHciContext->p_admin_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        hcp_packet = (phHciNfc_HCP_Packet_t *)pResponse;
        hcp_message = &hcp_packet->msg.message;
        p_admin_info = psHciContext->p_admin_info;
        prev_cmd = p_admin_info->admin_pipe_info->prev_msg ;
        prev_status = p_admin_info->admin_pipe_info->prev_status ;
        if(prev_status == NFCSTATUS_PENDING)
        {
            switch(prev_cmd)
            {
                case ANY_SET_PARAMETER:
                {
                    break;
                }
                case ANY_GET_PARAMETER:
                {
                    status = phHciNfc_Admin_InfoUpdate(psHciContext,
                                (phHal_sHwReference_t *)pHwRef,
                                p_admin_info->admin_pipe_info->reg_index, 
                                    &pResponse[HCP_HEADER_LEN],
                                        (uint8_t)(length - HCP_HEADER_LEN));
                    break;
                }
                case ANY_OPEN_PIPE:
                {
                    break;
                }
                case ANY_CLOSE_PIPE:
                {
                    phOsalNfc_FreeMemory(p_admin_info->admin_pipe_info);
                    p_admin_info->admin_pipe_info = NULL;
                    psHciContext->p_pipe_list[PIPETYPE_STATIC_ADMIN] = NULL;
                    break;
                }
                case ADM_CREATE_PIPE:
                {
                    p_pipe_info = (phHciNfc_Pipe_Info_t *)
                                        p_admin_info->admin_pipe_info->param_info;
                    pipe_id = hcp_message->payload[RESPONSE_PIPEID_OFFSET];
                    status = phHciNfc_Update_PipeInfo(psHciContext,
                        &(p_admin_info->pipe_seq), pipe_id, p_pipe_info);
                    if(NFCSTATUS_SUCCESS == status )
                    {
                        psHciContext->p_pipe_list[pipe_id] = p_pipe_info;
                        p_pipe_info->pipe.pipe_id = pipe_id;
                    }
                    break;
                }
                case ADM_DELETE_PIPE:
                {
                    p_pipe_info = (phHciNfc_Pipe_Info_t *)
                                    p_admin_info->admin_pipe_info->param_info;
                    if ( NULL != p_pipe_info )
                    {
                        pipe_id = p_pipe_info->pipe.pipe_id;
                        status = phHciNfc_Update_PipeInfo(
                            psHciContext, &(p_admin_info->pipe_seq),
                             (uint8_t) HCI_UNKNOWN_PIPE_ID, p_pipe_info);
                        if(NFCSTATUS_SUCCESS == status )
                        {
                            phOsalNfc_FreeMemory(p_pipe_info);
                            psHciContext->p_pipe_list[pipe_id] = NULL;
                        }
                    }
                    break;
                }
                case ADM_CLEAR_ALL_PIPE:
                {
                    break;
                }
                default:
                {
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_RESPONSE);
                    HCI_DEBUG("%s: Default Statement Should Not Occur \n",
                                                    "phHciNfc_Recv_Admin_Response");
                    break;
                }
            }
        }
        if( NFCSTATUS_SUCCESS == status )
        {
            if( NULL != p_admin_info->admin_pipe_info)
            {
                p_admin_info->admin_pipe_info->prev_status = NFCSTATUS_SUCCESS;
            }
            p_admin_info->current_seq = p_admin_info->next_seq;
        }
    }
    return status;
}

/*!
 * \brief Receives the HCI Admin Commands from the corresponding peripheral device.
 *
 * This function receives  the HCI Admin Commands from the connected NFC Pheripheral
 * device
 */
static
 NFCSTATUS
 phHciNfc_Recv_Admin_Cmd (
                        void                *psContext,
                        void                *pHwRef,
                        uint8_t             *pCmd,
#ifdef ONE_BYTE_LEN
                        uint8_t             length
#else
                        uint16_t            length
#endif
                     )
{
    phHciNfc_sContext_t         *psHciContext = 
                                    (phHciNfc_sContext_t *)psContext ;
    phHciNfc_HCP_Packet_t       *hcp_packet = NULL;
    phHciNfc_HCP_Message_t      *hcp_message = NULL;
    phHciNfc_AdminGate_Info_t   *p_admin_info=NULL;
    phHciNfc_Pipe_Info_t        *p_pipe_info = NULL;
    uint8_t                     index=0;
    uint8_t                     pipe_id = (uint8_t) HCI_UNKNOWN_PIPE_ID;
    uint8_t                     cmd = (uint8_t) HCP_MSG_INSTRUCTION_INVALID;
    uint8_t                     response = (uint8_t) ANY_OK;
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) 
        || (NULL == pHwRef) 
        || (HCP_HEADER_LEN > length ) 
      )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        hcp_packet = (phHciNfc_HCP_Packet_t *)pCmd;
        hcp_message = &hcp_packet->msg.message;
        p_admin_info = psHciContext->p_admin_info;
        /* Get the Command instruction bits from the Message Header */
        cmd = (uint8_t) GET_BITS8( hcp_message->msg_header,
            HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);

        switch( cmd )
        {
            /* These are notifications sent by the Host Controller */
            case ADM_NOTIFY_PIPE_CREATED:
            {
                pipe_id = hcp_message->payload[RESPONSE_PIPEID_OFFSET];
                p_pipe_info = (phHciNfc_Pipe_Info_t *)
                        phOsalNfc_GetMemory(sizeof(phHciNfc_Pipe_Info_t));
                memset(p_pipe_info, 0, sizeof(phHciNfc_Pipe_Info_t));
                if(NULL != p_pipe_info)
                {
                    /* The Source Host is the UICC Host */
                    p_pipe_info->pipe.source.host_id = 
                                    hcp_message->payload[index++];
                    /* The Source Gate is same as the Destination Gate */
                    p_pipe_info->pipe.source.gate_id    = 
                                    hcp_message->payload[index++];
                    /* The Source Host is the Terminal Host */
                    p_pipe_info->pipe.dest.host_id = 
                                    hcp_message->payload[index++];
                    p_pipe_info->pipe.dest.gate_id  = 
                                    hcp_message->payload[index++];
                    p_pipe_info->pipe.pipe_id   = 
                                    hcp_message->payload[index++];
                }
                status = phHciNfc_Update_PipeInfo(psHciContext,
                    &(p_admin_info->pipe_seq), pipe_id, p_pipe_info);

                if( NFCSTATUS_SUCCESS == status )
                {
                    psHciContext->p_pipe_list[pipe_id] = p_pipe_info;
                    if (NULL != p_pipe_info)
                    {
                        p_pipe_info->pipe.pipe_id = pipe_id;
                    }
                }
                break;
            }
            case ADM_NOTIFY_PIPE_DELETED:
            {
                pipe_id = hcp_message->payload[index++];
                p_pipe_info = psHciContext->p_pipe_list[pipe_id];
                if ( NULL != p_pipe_info )
                {
                        status = phHciNfc_Update_PipeInfo(
                            psHciContext, &(p_admin_info->pipe_seq),
                             (uint8_t) HCI_UNKNOWN_PIPE_ID, p_pipe_info);
                    if(NFCSTATUS_SUCCESS == status )
                    {
                        phOsalNfc_FreeMemory(p_pipe_info);
                        psHciContext->p_pipe_list[pipe_id] = NULL;
                    }
                }
                break;
            }
            /* TODO: Since we receive the Host ID, we need to clear
             * all the pipes created with the host
             */
            case ADM_NOTIFY_ALL_PIPE_CLEARED:
            {
                break;
            }
            /* case ADM_CREATE_PIPE: */
            /* case ADM_DELETE_PIPE: */
            /* case ADM_CLEAR_ALL_PIPE: */
            default:
            {
                response = ANY_E_CMD_NOT_SUPPORTED;
                status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_COMMAND_NOT_SUPPORTED);
                break;
            }
        }
        hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;
        phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                                (uint8_t) HCI_ADMIN_PIPE_ID,
                                HCP_MSG_TYPE_RESPONSE, response );
        psHciContext->tx_total = HCP_HEADER_LEN;
        status = phHciNfc_Send_HCP( (void *)psHciContext, (void *)pHwRef );

        p_admin_info->admin_pipe_info->recv_msg_type = HCP_MSG_TYPE_COMMAND;
        p_admin_info->admin_pipe_info->sent_msg_type = HCP_MSG_TYPE_RESPONSE;
        p_admin_info->admin_pipe_info->prev_msg = response;
        p_admin_info->admin_pipe_info->prev_status = NFCSTATUS_PENDING;
    }
    return status;
}

/*!
 * \brief Receives the HCI Admin Event from the corresponding peripheral device.
 *
 * This function receives  the HCI Admin Events from the connected NFC Pheripheral
 * device
 */
static
 NFCSTATUS
 phHciNfc_Recv_Admin_Event (
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
    phHciNfc_sContext_t         *psHciContext = 
                                    (phHciNfc_sContext_t *)psContext ;
    phHciNfc_HCP_Packet_t       *hcp_packet = NULL;
    phHciNfc_HCP_Message_t      *hcp_message = NULL;
    uint8_t                     event = (uint8_t) HCP_MSG_INSTRUCTION_INVALID;
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) 
        || (NULL == pHwRef) 
        || (HCP_HEADER_LEN > length ) 
      )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        hcp_packet = (phHciNfc_HCP_Packet_t *)pEvent;
        hcp_message = &hcp_packet->msg.message;
        /* Get the Command instruction bits from the Message Header */
        event = (uint8_t) GET_BITS8( hcp_message->msg_header,
            HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);

        if( EVT_HOT_PLUG ==   event )
        {
            status = phHciNfc_Send_Admin_Event ( psHciContext, pHwRef, 
                                EVT_HOT_PLUG, 0 ,NULL);

        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INSTRUCTION);
        }


    }
    return status;
}


static
NFCSTATUS
phHciNfc_Admin_InfoUpdate(
                                phHciNfc_sContext_t     *psHciContext,
                                phHal_sHwReference_t    *pHwRef,
                                uint8_t                 index,
                                uint8_t                 *reg_value,
                                uint8_t             reg_length
                          )
{
    phHciNfc_AdminGate_Info_t   *p_admin_info=NULL;
    uint8_t                     i=0;
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    if(NULL == reg_value)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        p_admin_info = psHciContext->p_admin_info ;
        HCI_PRINT_BUFFER("Admin Mgmt Info Buffer",reg_value,reg_length);
        switch(index)
        {
            case SESSION_INDEX :
            {
                for(i=0 ;(reg_length == SESSIONID_LEN)&&(i < reg_length); i++)
                {
                    p_admin_info->session_id[i] = reg_value[i];
                    pHwRef->session_id[i] = reg_value[i];
                }
                break;
            }
            case MAX_PIPE_INDEX :
            {
                p_admin_info->max_pipe = reg_value[i];
                break;
            }
            case WHITELIST_INDEX :
            {
                for(i=0 ;(reg_length <= WHITELIST_MAX_LEN)&&(i < reg_length); i++)
                {
                    p_admin_info->whitelist[i] = reg_value[i];
                }
                break;
            }
            case HOST_LIST_INDEX :
            {
                for(i=0 ;(reg_length <= HOST_LIST_MAX_LEN)&&(i < reg_length); i++)
                {
                    p_admin_info->host_list[i] = reg_value[i];
                }
                break;
            }
            default:
            {
                status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_RESPONSE);
                break;
            } /*End of the default Switch Case */

        } /*End of the Index Switch */

    } /* End of Context and the Identity information validity check */

    return status;
}

