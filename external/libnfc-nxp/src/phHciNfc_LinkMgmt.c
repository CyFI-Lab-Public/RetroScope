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
* \file  phHciNfc_LinkMgmt.c                                                  *
* \brief HCI Link Management Gate Routines.                                   *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Thu Feb 11 18:52:19 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.11 $                                                            *
* $Aliases: NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                                                *
*                                                                             *
* =========================================================================== *
*/

/*
***************************** Header File Inclusion ****************************
*/
#include <phNfcCompId.h>
#include <phHciNfc_Pipe.h>
#include <phHciNfc_LinkMgmt.h>
#include <phOsalNfc.h>

/*
****************************** Macro Definitions *******************************
*/

#define REC_ERROR_INDEX         0x01U

#define REC_RETRY_LEN           0x02U

/*
*************************** Structure and Enumeration ***************************
*/


/** \defgroup grp_hci_nfc HCI Link Management Component
 *
 *
 */

typedef enum phHciNfc_LinkMgmt_Seq{
    LINK_MGMT_PIPE_OPEN     = 0x00U,
    LINK_MGMT_GET_REC_ERROR,
    LINK_MGMT_SET_REC_ERROR,
    LINK_MGMT_PIPE_CLOSE
} phHciNfc_LinkMgmt_Seq_t;

typedef struct phHciNfc_LinkMgmt_Info{
    phHciNfc_LinkMgmt_Seq_t link_cur_seq;
    phHciNfc_LinkMgmt_Seq_t link_next_seq;
    phHciNfc_Pipe_Info_t    *p_pipe_info;
    /* Rec Error Count Number from the Host Controller */
    uint16_t                hc_rec_error;
    /* Rec Error Count Number of the Terminal Host */
    uint16_t                rec_error;
} phHciNfc_LinkMgmt_Info_t;


/*
*************************** Static Function Declaration **************************
*/

static
NFCSTATUS
phHciNfc_LinkMgmt_InfoUpdate(
                                phHciNfc_sContext_t     *psHciContext,
                                phHal_sHwReference_t    *pHwRef,
                                uint8_t                 index,
                                uint8_t                 *reg_value,
                                uint8_t                 reg_length
                         );
static
NFCSTATUS
phHciNfc_Recv_LinkMgmt_Response(
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
 * \brief Initialisation of Link Managment Gate.
 *
 * This function initialses the Link Management gate and 
 * populates the Link Management Information Structure
 * 
 */

NFCSTATUS
phHciNfc_LinkMgmt_Initialise(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                         )
{
    NFCSTATUS                           status = NFCSTATUS_SUCCESS;
    phHciNfc_Pipe_Info_t                *p_pipe_info = NULL;
    phHciNfc_LinkMgmt_Info_t            *p_link_mgmt_info=NULL;
    uint8_t                             link_pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;

    if( ( NULL == psHciContext )
        || (NULL == pHwRef )
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if( ( NULL == psHciContext->p_link_mgmt_info )
            && (phHciNfc_Allocate_Resource((void **)(&p_link_mgmt_info),
                    sizeof(phHciNfc_LinkMgmt_Info_t))== NFCSTATUS_SUCCESS)
          )
        {
            psHciContext->p_link_mgmt_info = p_link_mgmt_info;
            p_link_mgmt_info->link_cur_seq = LINK_MGMT_PIPE_OPEN;
            p_link_mgmt_info->link_next_seq = LINK_MGMT_PIPE_OPEN;
            p_link_mgmt_info->p_pipe_info = NULL;
        }
        else
        {
            p_link_mgmt_info = (phHciNfc_LinkMgmt_Info_t *)
                                psHciContext->p_link_mgmt_info ;
        }

        if( NULL == p_link_mgmt_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI,
                        NFCSTATUS_INVALID_HCI_INFORMATION);
        }
#ifdef ESTABLISH_SESSION
        else if( hciMode_Session == psHciContext->hci_mode )
        {
            status = NFCSTATUS_SUCCESS;
        }
#endif
        else
        {
            switch(p_link_mgmt_info->link_cur_seq )
            {
                /* Link Mgmt pipe open sequence */
                case LINK_MGMT_PIPE_OPEN:
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
                                        PIPETYPE_STATIC_LINK;
                        ((phHciNfc_Pipe_Info_t *)p_pipe_info)->recv_resp = 
                                        &phHciNfc_Recv_LinkMgmt_Response;
                        psHciContext->p_pipe_list[PIPETYPE_STATIC_LINK] =
                                                                    p_pipe_info ;
                        status = phHciNfc_Open_Pipe( psHciContext,
                                                            pHwRef,p_pipe_info );
                        if(status == NFCSTATUS_SUCCESS)
                        {
                            p_link_mgmt_info->p_pipe_info = p_pipe_info ;
                            p_link_mgmt_info->link_next_seq = 
                                                    LINK_MGMT_GET_REC_ERROR;
                            status = NFCSTATUS_PENDING;
                        }
                    }
                    break;
                }
                case LINK_MGMT_GET_REC_ERROR:
                {
                    p_pipe_info = p_link_mgmt_info->p_pipe_info;
                    if(NULL == p_pipe_info )
                    {
                        status = PHNFCSTVAL(CID_NFC_HCI, 
                                        NFCSTATUS_INVALID_HCI_SEQUENCE);
                    }
                    else
                    {
                        p_pipe_info->reg_index = REC_ERROR_INDEX;
                        link_pipe_id = PIPETYPE_STATIC_LINK ;
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                link_pipe_id,   (uint8_t)ANY_GET_PARAMETER );
                        if(NFCSTATUS_PENDING == status )
                        {
                            p_link_mgmt_info->link_next_seq =
                                                        LINK_MGMT_PIPE_CLOSE;
                            status = NFCSTATUS_SUCCESS;
                        }
                    }
                    break;
                }
                case LINK_MGMT_SET_REC_ERROR:
                {
                    p_pipe_info = p_link_mgmt_info->p_pipe_info;
                    if(NULL == p_pipe_info )
                    {
                        status = PHNFCSTVAL(CID_NFC_HCI, 
                                        NFCSTATUS_INVALID_HCI_SEQUENCE);
                    }
                    else
                    {
                        p_pipe_info->reg_index = REC_ERROR_INDEX;
                        link_pipe_id = PIPETYPE_STATIC_LINK ;
                        status = 
                            phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
                                link_pipe_id,   (uint8_t)ANY_GET_PARAMETER );
                        if(NFCSTATUS_PENDING == status )
                        {
                            p_link_mgmt_info->link_next_seq =
                                                        LINK_MGMT_PIPE_CLOSE;
                            status = NFCSTATUS_SUCCESS;
                        }
                    }
                    break;
                }
                default:
                {
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_SEQUENCE);
                    break;
                }

            }/* End of the Sequence Switch */

        }/* End of the Link Info Memory Check */
    } /* End of Null Context Check */

    return status;
}

/*!
 * \brief Opens the Link Management Pipe of the Link Management Gate.
 *
 * This function Opens the Link Management Pipe of the Link Management
 * Gate and Confirms that the HCI Link is behaving as expected.
 */

NFCSTATUS
phHciNfc_LinkMgmt_Open(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                             )
{
    NFCSTATUS                           status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        phHciNfc_LinkMgmt_Info_t  *p_link_mgmt_info=
            (phHciNfc_LinkMgmt_Info_t *)psHciContext->p_link_mgmt_info ;
        if(( NULL != p_link_mgmt_info ) && 
                ( NULL != p_link_mgmt_info->p_pipe_info  ))
        {
            status = phHciNfc_Open_Pipe( psHciContext,
                            pHwRef, p_link_mgmt_info->p_pipe_info );
            if(status == NFCSTATUS_SUCCESS)
            {
                status = NFCSTATUS_PENDING;
            }
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_NOT_ALLOWED);

        }/* End of the Identity Info Memory Check */

    } /* End of Null Context Check */

    return status;
}


NFCSTATUS
phHciNfc_LinkMgmt_Release(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                             )
{
    NFCSTATUS                           status = NFCSTATUS_SUCCESS;
    phHciNfc_LinkMgmt_Info_t            *p_link_mgmt_info=NULL;

    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if( NULL != psHciContext->p_link_mgmt_info )
        {
            p_link_mgmt_info = (phHciNfc_LinkMgmt_Info_t *)
                                psHciContext->p_link_mgmt_info ;
            status = phHciNfc_Close_Pipe( psHciContext,
                            pHwRef, p_link_mgmt_info->p_pipe_info );
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
phHciNfc_Recv_LinkMgmt_Response(
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
    phHciNfc_LinkMgmt_Info_t    *p_link_mgmt_info=NULL;
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    uint8_t                     prev_cmd = ANY_GET_PARAMETER;

    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(  NULL == psHciContext->p_link_mgmt_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        p_link_mgmt_info = (phHciNfc_LinkMgmt_Info_t *)
                            psHciContext->p_link_mgmt_info ;
        prev_cmd = p_link_mgmt_info->p_pipe_info->prev_msg ;
        switch(prev_cmd)
        {
            case ANY_GET_PARAMETER:
            {
                status = phHciNfc_LinkMgmt_InfoUpdate(psHciContext,
                            (phHal_sHwReference_t *)pHwRef,
                            p_link_mgmt_info->p_pipe_info->reg_index, 
                            &pResponse[HCP_HEADER_LEN],
                                (uint8_t)(length - HCP_HEADER_LEN));
                break;
            }
            case ANY_SET_PARAMETER:
            {
                status = PHNFCSTVAL(CID_NFC_HCI,
                                    NFCSTATUS_FEATURE_NOT_SUPPORTED);
                break;
            }
            case ANY_OPEN_PIPE:
            {
                break;
            }
            case ANY_CLOSE_PIPE:
            {
                phOsalNfc_FreeMemory(p_link_mgmt_info->p_pipe_info);
                p_link_mgmt_info->p_pipe_info = NULL;
                psHciContext->p_pipe_list[PIPETYPE_STATIC_LINK] = NULL;
                break;
            }
            default:
            {
                status = PHNFCSTVAL(CID_NFC_HCI,
                                        NFCSTATUS_INVALID_HCI_RESPONSE);
                break;
            }
        }
        if( NFCSTATUS_SUCCESS == status )
        {
            if( NULL != p_link_mgmt_info->p_pipe_info)
            {
                p_link_mgmt_info->p_pipe_info->prev_status = NFCSTATUS_SUCCESS;
            }
            p_link_mgmt_info->link_cur_seq = p_link_mgmt_info->link_next_seq;
        }

    }
    return status;
}


static
NFCSTATUS
phHciNfc_LinkMgmt_InfoUpdate(
                                phHciNfc_sContext_t     *psHciContext,
                                phHal_sHwReference_t    *pHwRef,
                                uint8_t                 index,
                                uint8_t                 *reg_value,
                                uint8_t                 reg_length
                          )
{
    phHciNfc_LinkMgmt_Info_t    *p_link_mgmt_info=NULL;
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    uint8_t                     i=0;
    if( (NULL == psHciContext)
        || (NULL == pHwRef)
        || (NULL == reg_value)
        || (reg_length == 0)
      )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if ( NULL == psHciContext->p_link_mgmt_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        p_link_mgmt_info = (phHciNfc_LinkMgmt_Info_t *)
                                psHciContext->p_link_mgmt_info ;
        if (REC_ERROR_INDEX == index)
        {
            HCI_PRINT_BUFFER("\tHost Controller REC Error Count :",reg_value,reg_length);
            /* p_link_mgmt_info->hc_rec_error = reg_value[i] ; */
            for(i=0 ;(reg_length == REC_RETRY_LEN)&&(i < reg_length); i++)
            {
                p_link_mgmt_info->hc_rec_error |= 
                            (uint16_t)(reg_value[i] << (BYTE_SIZE * i));
            }
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
        } /* End of the Index Check */

    } /* End of Context and the Link information validity check */

    return status;
}
