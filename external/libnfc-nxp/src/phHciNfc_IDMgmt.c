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
* \file  phHciNfc_IDMgmt.c                                                    *
* \brief HCI Identity Management Gate Routines.                               *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Fri Jun 11 11:19:25 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.23 $                                                            *
* $Aliases: NFC_FRI1.1_WK1023_R35_1 $      
*                                                                             *
* =========================================================================== *
*/

/*
***************************** Header File Inclusion ****************************
*/
#include <phNfcCompId.h>
#include <phHciNfc_Pipe.h>
#include <phHciNfc_IDMgmt.h>
#include <phOsalNfc.h>

/*
****************************** Macro Definitions *******************************
*/

#define FW_VERSION_INDEX        0x01U
#define HCI_VERSION_INDEX       0x02U
#define HW_VERSION_INDEX        0x03U
#define VENDOR_NAME_INDEX       0x04U
#define MODEL_ID_INDEX          0x05U
#define GATES_LIST_INDEX        0x06U
#define FULL_VERSION_INDEX      0x10U

#define VERSION_LEN             0x03U
#define GATES_LIST_LEN          0x20U

/*
*************************** Structure and Enumeration ***************************
*/


/** \defgroup grp_hci_nfc HCI Identity Management Component
 *
 *
 */

typedef enum phHciNfc_IDMgmt_Seq{
    IDMGMT_PIPE_OPEN        = 0x00U,
    IDMGMT_GET_FULL_VERSION,
    IDMGMT_GET_FW_VERSION,
    IDMGMT_GET_HW_VERSION,
    IDMGMT_GET_HCI_VERSION,
    IDMGMT_GET_VENDOR_NAME,
    IDMGMT_GET_MODEL_ID,
    IDMGMT_GET_GATES_LIST,
    IDMGMT_PIPE_CLOSE
} phHciNfc_IDMgmt_Seq_t;

typedef struct phHciNfc_IDMgmt_Info{
    phHciNfc_IDMgmt_Seq_t   id_cur_seq;
    phHciNfc_IDMgmt_Seq_t   id_next_seq;
    phHciNfc_Pipe_Info_t    *p_pipe_info;
    uint8_t                 pipe_id;
    uint32_t                fw_version;
    uint32_t                hw_version;
    utf8_t                  vendor_name[VENDOR_NAME_LEN];
    uint8_t                 model_id;
    uint8_t                 hci_version;
    uint8_t                 gates_list[GATES_LIST_LEN];
    uint8_t                 full_version[NXP_FULL_VERSION_LEN];
} phHciNfc_IDMgmt_Info_t;

/*
*************************** Static Function Declaration **************************
*/

static
NFCSTATUS
phHciNfc_IDMgmt_InfoUpdate(
                                phHciNfc_sContext_t     *psHciContext,
                                phHal_sHwReference_t    *pHwRef,
                                uint8_t                 index,
                                uint8_t                 *reg_value,
                                uint8_t                 reg_length
                         );

static
NFCSTATUS
phHciNfc_Recv_IDMgmt_Response(
                        void                *psHciContext,
                        void                *pHwRef,
                        uint8_t             *pResponse,
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
 * \brief Allocates the resources of Identity Managment Gate.
 *
 * This function Allocates the resources of the Identity Management
 * gate Information Structure.
 * 
 */

NFCSTATUS
phHciNfc_IDMgmt_Init_Resources(
                                phHciNfc_sContext_t     *psHciContext
                             )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_IDMgmt_Info_t      *p_identity_info=NULL;
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if( ( NULL == psHciContext->p_identity_info )
            && (phHciNfc_Allocate_Resource((void **)(&p_identity_info),
                    sizeof(phHciNfc_IDMgmt_Info_t))== NFCSTATUS_SUCCESS)
        )
        {
            psHciContext->p_identity_info = p_identity_info;
            p_identity_info->id_cur_seq = IDMGMT_PIPE_OPEN;
            p_identity_info->id_next_seq = IDMGMT_PIPE_OPEN;
            p_identity_info->pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INSUFFICIENT_RESOURCES);
        }
    }
    return status;
}

/*!
 * \brief Get the pipe_id of Identity Managment Gate.
 *
 * This function Get the pipe_id of Identity Managment Gate.
 * 
 */


NFCSTATUS
phHciNfc_IDMgmt_Get_PipeID(
                                phHciNfc_sContext_t     *psHciContext,
                                uint8_t                 *ppipe_id
                             )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_IDMgmt_Info_t      *p_identity_info=NULL;
    if( (NULL != psHciContext)
        && ( NULL != ppipe_id )
        && ( NULL != psHciContext->p_identity_info ) 
      )
    {
        p_identity_info = (phHciNfc_IDMgmt_Info_t *)
                            psHciContext->p_identity_info ;
        *ppipe_id =  p_identity_info->pipe_id  ;
    }
    else 
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    return status;
}

NFCSTATUS
phHciNfc_IDMgmt_Update_Sequence(
                                phHciNfc_sContext_t     *psHciContext,
                                phHciNfc_eSeqType_t     reader_seq
                             )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_IDMgmt_Info_t      *p_identity_info=NULL;
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if( NULL == psHciContext->p_identity_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                        NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            p_identity_info = (phHciNfc_IDMgmt_Info_t *)
                                psHciContext->p_identity_info ;
            switch(reader_seq)
            {
                case RESET_SEQ:
                case INIT_SEQ:
                {
                    p_identity_info->id_cur_seq = IDMGMT_PIPE_OPEN;
                    p_identity_info->id_next_seq = IDMGMT_PIPE_OPEN;
                    break;
                }
                case UPDATE_SEQ:
                {
                    p_identity_info->id_cur_seq = 
                                            p_identity_info->id_next_seq;
                    break;
                }
                case INFO_SEQ:
                {
                    p_identity_info->id_cur_seq = IDMGMT_GET_FW_VERSION;
                    p_identity_info->id_next_seq = IDMGMT_GET_FW_VERSION;
                    break;
                }
                case REL_SEQ:
                {
                    p_identity_info->id_cur_seq = IDMGMT_PIPE_CLOSE;
                    p_identity_info->id_next_seq = IDMGMT_PIPE_CLOSE;
                    break;
                }
                default:
                {
                    break;
                }
            }
        }
    }

    return status;

}



/*!
 * \brief Initialisation of Identity Managment Gate.
 *
 * This function initialses the Identity Management gate and 
 * populates the Identity Management Information Structure
 * 
 */

NFCSTATUS
phHciNfc_IDMgmt_Initialise(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                         )
{
    NFCSTATUS                           status = NFCSTATUS_SUCCESS;
    phHciNfc_Pipe_Info_t                *p_pipe_info = NULL;
    phHciNfc_IDMgmt_Info_t              *p_identity_info=NULL;
#ifndef ESTABLISH_SESSION
    uint8_t                             id_pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;
#endif

    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {

        if( NULL == psHciContext->p_identity_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI,
                        NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            p_identity_info = (phHciNfc_IDMgmt_Info_t *)
                                psHciContext->p_identity_info ;
            p_pipe_info = p_identity_info->p_pipe_info;
            if(NULL == p_pipe_info )
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_NOT_ALLOWED);
            }
            else
            {
                switch(p_identity_info->id_cur_seq )
                {
                    /* Identity Mgmt pipe open sequence */
                    case IDMGMT_PIPE_OPEN:
                    {
                        status = phHciNfc_Open_Pipe( psHciContext,
                                                        pHwRef, p_pipe_info );
                        if(status == NFCSTATUS_SUCCESS)
                        {
                            p_identity_info->id_next_seq = IDMGMT_GET_FW_VERSION;
#ifndef ESTABLISH_SESSION
                            status = NFCSTATUS_PENDING;
#endif
                        }
                        break;
                    }
#ifndef ESTABLISH_SESSION
                    case IDMGMT_GET_FW_VERSION:
                    {
                        p_pipe_info->reg_index = FW_VERSION_INDEX;
                        id_pipe_id = p_identity_info->pipe_id ;
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                id_pipe_id, (uint8_t)ANY_GET_PARAMETER );
                        if(NFCSTATUS_PENDING == status )
                        {
                            p_identity_info->id_next_seq = IDMGMT_GET_HW_VERSION;
                            /* status = NFCSTATUS_SUCCESS; */
                        }
                        break;
                    }
                    case IDMGMT_GET_HW_VERSION:
                    {
                        p_pipe_info->reg_index = HW_VERSION_INDEX;
                        id_pipe_id = p_identity_info->pipe_id ;
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                id_pipe_id, (uint8_t)ANY_GET_PARAMETER );
                        if(NFCSTATUS_PENDING == status )
                        {
                            p_identity_info->id_next_seq = IDMGMT_GET_HCI_VERSION;
                            /* status = NFCSTATUS_SUCCESS; */
                        }
                        break;
                    }
                    case IDMGMT_GET_HCI_VERSION:
                    {
                        p_pipe_info->reg_index = HCI_VERSION_INDEX;
                        id_pipe_id = p_identity_info->pipe_id ;
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                id_pipe_id, (uint8_t)ANY_GET_PARAMETER );
                        if(NFCSTATUS_PENDING == status )
                        {
                            p_identity_info->id_next_seq = IDMGMT_GET_VENDOR_NAME;
                            /* status = NFCSTATUS_SUCCESS; */
                        }
                        break;
                    }
                    case IDMGMT_GET_VENDOR_NAME:
                    {
                        p_pipe_info->reg_index = VENDOR_NAME_INDEX;
                        id_pipe_id = p_identity_info->pipe_id ;
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                id_pipe_id, (uint8_t)ANY_GET_PARAMETER );
                        if(NFCSTATUS_PENDING == status )
                        {
                            p_identity_info->id_next_seq = IDMGMT_GET_MODEL_ID;
                            /* status = NFCSTATUS_SUCCESS; */
                        }
                        break;
                    }
                    case IDMGMT_GET_MODEL_ID:
                    {
                        p_pipe_info->reg_index = MODEL_ID_INDEX;
                        id_pipe_id = p_identity_info->pipe_id ;
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                id_pipe_id, (uint8_t)ANY_GET_PARAMETER );
                        if(NFCSTATUS_PENDING == status )
                        {
                            p_identity_info->id_next_seq = IDMGMT_GET_GATES_LIST;
                            /* status = NFCSTATUS_SUCCESS; */
                        }
                        break;
                    }
                    case IDMGMT_GET_GATES_LIST:
                    {
                        p_pipe_info->reg_index = GATES_LIST_INDEX;
                        id_pipe_id = p_identity_info->pipe_id ;
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                id_pipe_id, (uint8_t)ANY_GET_PARAMETER );
                        if(NFCSTATUS_PENDING == status )
                        {
                            p_identity_info->id_next_seq = IDMGMT_GET_FULL_VERSION;
                            /* status = NFCSTATUS_SUCCESS; */
                        }
                        break;
                    }
                    case IDMGMT_GET_FULL_VERSION:
                    {
                        p_pipe_info->reg_index = FULL_VERSION_INDEX;
                        id_pipe_id = p_identity_info->pipe_id ;
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                id_pipe_id, (uint8_t)ANY_GET_PARAMETER );
                        if(NFCSTATUS_PENDING == status )
                        {
                            p_identity_info->id_next_seq = IDMGMT_PIPE_CLOSE;
                            status = NFCSTATUS_SUCCESS;
                        }
                        break;
                    }
#endif
                    default:
                    {
                        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_SEQUENCE);
                        break;
                    }

                }/* End of the Sequence Switch */
            }/* End of Pipe Info Memory Check */

        }/* End of the Identity Info Memory Check */

    } /* End of Null Context Check */

    return status;
}



/*!
 * \brief Initialisation of Identity Managment Gate.
 *
 * This function initialses the Identity Management gate and 
 * populates the Identity Management Information Structure
 * 
 */

NFCSTATUS
phHciNfc_IDMgmt_Info_Sequence(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                         )
{
    NFCSTATUS                           status = NFCSTATUS_SUCCESS;
    phHciNfc_Pipe_Info_t                *p_pipe_info = NULL;
    phHciNfc_IDMgmt_Info_t              *p_identity_info=NULL;
    uint8_t                             id_pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;

    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {

        if( NULL == psHciContext->p_identity_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI,
                        NFCSTATUS_INVALID_HCI_INFORMATION);
        }
        else
        {
            p_identity_info = (phHciNfc_IDMgmt_Info_t *)
                                psHciContext->p_identity_info ;
            p_pipe_info = p_identity_info->p_pipe_info;
            if(NULL == p_pipe_info )
            {
                status = PHNFCSTVAL(CID_NFC_HCI, 
                                NFCSTATUS_NOT_ALLOWED);
            }
            else
            {
                switch(p_identity_info->id_cur_seq )
                {
                    case IDMGMT_GET_FW_VERSION:
                    {
                        p_pipe_info->reg_index = FW_VERSION_INDEX;
                        id_pipe_id = p_identity_info->pipe_id ;
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                id_pipe_id, (uint8_t)ANY_GET_PARAMETER );
                        if(NFCSTATUS_PENDING == status )
                        {
                            p_identity_info->id_next_seq = IDMGMT_GET_HW_VERSION;
                            /* status = NFCSTATUS_SUCCESS; */
                        }
                        break;
                    }
                    case IDMGMT_GET_HW_VERSION:
                    {
                        p_pipe_info->reg_index = HW_VERSION_INDEX;
                        id_pipe_id = p_identity_info->pipe_id ;
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                id_pipe_id, (uint8_t)ANY_GET_PARAMETER );
                        if(NFCSTATUS_PENDING == status )
                        {
                            p_identity_info->id_next_seq = IDMGMT_GET_HCI_VERSION;
                            /* status = NFCSTATUS_SUCCESS; */
                        }
                        break;
                    }
                    case IDMGMT_GET_HCI_VERSION:
                    {
                        p_pipe_info->reg_index = HCI_VERSION_INDEX;
                        id_pipe_id = p_identity_info->pipe_id ;
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                id_pipe_id, (uint8_t)ANY_GET_PARAMETER );
                        if(NFCSTATUS_PENDING == status )
                        {
                            p_identity_info->id_next_seq = IDMGMT_GET_VENDOR_NAME;
                            /* status = NFCSTATUS_SUCCESS; */
                        }
                        break;
                    }
                    case IDMGMT_GET_VENDOR_NAME:
                    {
                        p_pipe_info->reg_index = VENDOR_NAME_INDEX;
                        id_pipe_id = p_identity_info->pipe_id ;
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                id_pipe_id, (uint8_t)ANY_GET_PARAMETER );
                        if(NFCSTATUS_PENDING == status )
                        {
                            p_identity_info->id_next_seq = IDMGMT_GET_MODEL_ID;
                            /* status = NFCSTATUS_SUCCESS; */
                        }
                        break;
                    }
                    case IDMGMT_GET_MODEL_ID:
                    {
                        p_pipe_info->reg_index = MODEL_ID_INDEX;
                        id_pipe_id = p_identity_info->pipe_id ;
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                id_pipe_id, (uint8_t)ANY_GET_PARAMETER );
                        if(NFCSTATUS_PENDING == status )
                        {
                            p_identity_info->id_next_seq = IDMGMT_GET_GATES_LIST;
                            /* status = NFCSTATUS_SUCCESS; */
                        }
                        break;
                    }
                    case IDMGMT_GET_GATES_LIST:
                    {
                        p_pipe_info->reg_index = GATES_LIST_INDEX;
                        id_pipe_id = p_identity_info->pipe_id ;
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                id_pipe_id, (uint8_t)ANY_GET_PARAMETER );
                        if(NFCSTATUS_PENDING == status )
                        {
                            p_identity_info->id_next_seq = IDMGMT_GET_FULL_VERSION;
                            /* status = NFCSTATUS_SUCCESS; */
                        }
                        break;
                    }
                    case IDMGMT_GET_FULL_VERSION:
                    {
                        p_pipe_info->reg_index = FULL_VERSION_INDEX;
                        id_pipe_id = p_identity_info->pipe_id ;
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                id_pipe_id, (uint8_t)ANY_GET_PARAMETER );
                        if(NFCSTATUS_PENDING == status )
                        {
                            p_identity_info->id_next_seq = IDMGMT_PIPE_CLOSE;
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
            }/* End of Pipe Info Memory Check */

        }/* End of the Identity Info Memory Check */

    } /* End of Null Context Check */

    return status;
}

/*!
 * \brief Releases the resources allocated the Identity Management.
 *
 * This function Releases the resources allocated the Identity Management.
 */

NFCSTATUS
phHciNfc_IDMgmt_Release(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                             )
{
    NFCSTATUS                           status = NFCSTATUS_SUCCESS;
    phHciNfc_Pipe_Info_t                *p_pipe_info = NULL;
    phHciNfc_IDMgmt_Info_t              *p_identity_info=NULL;
    /* static phHciNfc_IDMgmt_Seq_t     identity_init_seq = IDMGMT_PIPE_CREATE; */

    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if( NULL != psHciContext->p_identity_info )
        {
            p_identity_info = (phHciNfc_IDMgmt_Info_t *)
                                psHciContext->p_identity_info ;
            p_pipe_info = p_identity_info->p_pipe_info;

            status = phHciNfc_Close_Pipe( psHciContext,
                                                pHwRef, p_pipe_info );
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_NOT_ALLOWED);

        }/* End of the Identity Info Memory Check */

    } /* End of Null Context Check */

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
phHciNfc_Recv_IDMgmt_Response(
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
    phHciNfc_IDMgmt_Info_t      *p_identity_info=NULL;
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    uint8_t                     prev_cmd = ANY_GET_PARAMETER;

    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(  NULL == psHciContext->p_identity_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        p_identity_info = (phHciNfc_IDMgmt_Info_t *)
                            psHciContext->p_identity_info ;
        if( NULL != p_identity_info->p_pipe_info)
        {
            prev_cmd = p_identity_info->p_pipe_info->prev_msg ;
            switch(prev_cmd)
            {
                case ANY_GET_PARAMETER:
                {
                    status = phHciNfc_IDMgmt_InfoUpdate(psHciContext,
                                (phHal_sHwReference_t *)pHwRef,
                                p_identity_info->p_pipe_info->reg_index, 
                                &pResponse[HCP_HEADER_LEN],
                                    (uint8_t)(length - HCP_HEADER_LEN));
                    break;
                }
                case ANY_SET_PARAMETER:
                {
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
                    break;
                }
                case ANY_OPEN_PIPE:
                {
                    break;
                }
                case ANY_CLOSE_PIPE:
                {
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
                p_identity_info->p_pipe_info->prev_status = NFCSTATUS_SUCCESS;
                p_identity_info->id_cur_seq = p_identity_info->id_next_seq;
            }
        }
    }
    return status;
}

/* Function to Update the  Pipe Information */
NFCSTATUS
phHciNfc_IDMgmt_Update_PipeInfo(
                                phHciNfc_sContext_t     *psHciContext,
                                uint8_t                 pipe_id,
                                phHciNfc_Pipe_Info_t    *pPipeInfo
                        )
{
    phHciNfc_IDMgmt_Info_t      *p_identity_info=NULL;
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if ( NULL == psHciContext->p_identity_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        p_identity_info = (phHciNfc_IDMgmt_Info_t *)
                                psHciContext->p_identity_info ;
        /* Update the pipe_id of the ID Mgmt Gate obtained from the HCI Response */
        p_identity_info->pipe_id = pipe_id;
        p_identity_info->p_pipe_info = pPipeInfo;
        if ( NULL != pPipeInfo)
        {
            /* Update the Response Receive routine of the IDMgmt Gate */
            pPipeInfo->recv_resp = &phHciNfc_Recv_IDMgmt_Response;
        }
    }

    return status;
}

static
NFCSTATUS
phHciNfc_IDMgmt_InfoUpdate(
                                phHciNfc_sContext_t     *psHciContext,
                                phHal_sHwReference_t    *pHwRef,
                                uint8_t                 index,
                                uint8_t                 *reg_value,
                                uint8_t                 reg_length
                          )
{
    phHciNfc_IDMgmt_Info_t      *p_identity_info=NULL;
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    uint8_t                     i=0;
    if( (NULL == psHciContext)
        || (NULL == reg_value)
        || (reg_length == 0)
      )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if ( NULL == psHciContext->p_identity_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        p_identity_info = (phHciNfc_IDMgmt_Info_t *)
                                psHciContext->p_identity_info ;

        switch(index)
        {
            case FW_VERSION_INDEX :
            {
                HCI_PRINT_BUFFER("\tFW Version:",reg_value,reg_length);
                for(i=0 ;(reg_length == VERSION_LEN)&&(i < reg_length); i++)
                {
                    p_identity_info->fw_version |= 
                                (uint32_t)(reg_value[VERSION_LEN - i - 1] << (BYTE_SIZE * i));
                }
                pHwRef->device_info.fw_version = p_identity_info->fw_version ;
                break;
            }
            case HW_VERSION_INDEX :
            {
                HCI_PRINT_BUFFER("\tHW Version:",reg_value,reg_length);
                for(i=0 ;(reg_length == VERSION_LEN)&&(i < reg_length); i++)
                {
                    p_identity_info->hw_version |= 
                                (uint32_t)(reg_value[VERSION_LEN - i - 1] << (BYTE_SIZE * i));
                }
                pHwRef->device_info.hw_version = p_identity_info->hw_version ;
                break;
            }
            case VENDOR_NAME_INDEX :
            {
                for(i=0 ;(reg_length <= VENDOR_NAME_LEN)&&(i < reg_length); i++)
                {
                    p_identity_info->vendor_name[i] = reg_value[i];
                    pHwRef->device_info.vendor_name[i]= reg_value[i];
                }
                HCI_DEBUG("\tVendor Name:%s",p_identity_info->vendor_name);
                break;
            }
            case MODEL_ID_INDEX :
            {
                HCI_PRINT_BUFFER("\tModel ID:",reg_value,reg_length);
                p_identity_info->model_id = reg_value[i] ;
                pHwRef->device_info.model_id = p_identity_info->model_id  ;
#ifndef NXP_HAL_ENABLE_SMX
                if( NFC_HW_PN65N == pHwRef->device_info.model_id)
#endif
                {
                  pHwRef->smx_connected = TRUE;
                }
                break;
            }
            case HCI_VERSION_INDEX :
            {
                HCI_PRINT_BUFFER("\tHCI Version:",reg_value,reg_length);
                p_identity_info->hci_version = reg_value[i] ;
                pHwRef->device_info.hci_version = p_identity_info->hci_version  ;
                break;
            }
            case GATES_LIST_INDEX :
            {
                HCI_PRINT_BUFFER("\tGates List:",reg_value,reg_length);
                for(i=0 ;(reg_length <= GATES_LIST_LEN)&&(i < reg_length); i++)
                {
                    p_identity_info->gates_list[i] = reg_value[i];
                }
                break;
            }
            case FULL_VERSION_INDEX :
            {
                HCI_PRINT_BUFFER("\tVERSION INFO:",reg_value,reg_length);
                for(i=0 ;(reg_length <= NXP_FULL_VERSION_LEN)&&(i < reg_length); i++)
                {
                    p_identity_info->full_version[i] = reg_value[i];
                    pHwRef->device_info.full_version[i]= reg_value[i];
                }
                break;
            }
            default:
            {
                status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
                break;
            } /*End of the default Switch Case */

        } /*End of the Index Switch */

    } /* End of Context and the Identity information validity check */

    return status;
}
