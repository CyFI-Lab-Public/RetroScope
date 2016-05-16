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
* \file  phHciNfc_Generic.c                                                   *
* \brief Generic HCI Source for the HCI Management.                           *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Tue Jun  8 09:31:49 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.108 $                                                           *
* $Aliases: NFC_FRI1.1_WK1023_R35_1 $  
*                                                                             *
* =========================================================================== *
*/

/*
################################################################################
***************************** Header File Inclusion ****************************
################################################################################
*/

#include <phNfcCompId.h>
#include <phHciNfc_Sequence.h>
#include <phHciNfc_Pipe.h>
#include <phHciNfc_AdminMgmt.h>
#include <phHciNfc_IDMgmt.h>
#include <phHciNfc_LinkMgmt.h>
#include <phHciNfc_PollingLoop.h>
#include <phHciNfc_RFReader.h>
#include <phHciNfc_RFReaderA.h>
#include <phOsalNfc.h>

/*
################################################################################
****************************** Macro Definitions *******************************
################################################################################
*/

/* HCI timeout value */
uint32_t nxp_nfc_hci_response_timeout = NXP_NFC_HCI_TIMEOUT;

/*
################################################################################
************************ Static Variable Definitions ***************************
################################################################################
*/


#if  (NXP_NFC_HCI_TIMER == 1) 

#define NXP_HCI_RESPONSE_TIMEOUT  (NXP_NFC_HCI_TIMEOUT)

#include <phOsalNfc_Timer.h>
/** \internal HCI Response Timer to detect the 
 * Stalled HCI Response */
static uint32_t                    hci_resp_timer_id = NXP_INVALID_TIMER_ID;
static phHciNfc_sContext_t        *gpsHciContext= NULL;

#endif /* (NXP_NFC_HCI_TIMER == 1) */


/*
################################################################################
************************* Function Prototype Declaration ***********************
################################################################################
*/

#if  (NXP_NFC_HCI_TIMER == 1)

static
void
phHciNfc_Response_Timeout (
                uint32_t resp_timer_id, void *pContext
                );

#endif /* (NXP_NFC_HCI_TIMER == 1) */

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Send function sends the HCI Commands to the
 *  corresponding peripheral device, described by the HCI Context Structure.
 *
 *  \param[in]  psContext               psContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  pdata                   Pointer to the buffer containing
 *                                      the command to be sent.
 *  \param[in] length                   Variable that receives
 *                                      the number of bytes actually sent.
 *
 *  \retval NFCSTATUS_PENDING           Command successfully sent.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the lower layers
 *
 */

static
 NFCSTATUS
 phHciNfc_Send(
                    void                    *psContext,
                    void                    *pHwRef,
                    uint8_t                 *pdata,
#ifdef ONE_BYTE_LEN
                    uint8_t                 length
#else
                    uint16_t                length
#endif
              );

 static
 NFCSTATUS
 phHciNfc_Process_HCP (
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef,
                                void                    *pdata,
#ifdef ONE_BYTE_LEN
                                uint8_t                 length
#else
                                uint16_t                length
#endif
                      );


static
NFCSTATUS
phHciNfc_Process_Response (
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef,
                                void                    *pdata,
#ifdef ONE_BYTE_LEN
                                uint8_t                 length
#else
                                uint16_t                length
#endif
                         );

static
NFCSTATUS
phHciNfc_Error_Response (
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef,
                                void                    *pdata,
#ifdef ONE_BYTE_LEN
                                uint8_t                 length
#else
                                uint16_t                length
#endif
                         );

static
NFCSTATUS
phHciNfc_Process_Event (
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef,
                                void                    *pdata,
#ifdef ONE_BYTE_LEN
                                uint8_t                 length
#else
                                uint16_t                length
#endif
                         );


static
NFCSTATUS
phHciNfc_Process_Command (
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef,
                                void                    *pdata,
#ifdef ONE_BYTE_LEN
                                uint8_t                 length
#else
                                uint16_t                length
#endif
                         );


static
void
phHciNfc_Reset_Pipe_MsgInfo(    
                            phHciNfc_Pipe_Info_t    *p_pipe_info
                        );

static
void
phHciNfc_Build_HCPMessage(
                                phHciNfc_HCP_Packet_t *hcp_packet,
                                uint8_t             msg_type,
                                uint8_t             instruction
                          );

static
void
phHciNfc_Build_HCPHeader(
                                phHciNfc_HCP_Packet_t *hcp_packet,
                                uint8_t             chainbit,
                                uint8_t             pipe_id
                          );
/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Receive_HCP function receive the HCI Host Control Packet 
 *  Frames from the device.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in] pdata                    Pointer to the response buffer that
 *                                      receives the response read.
 *  \param[in] length                   Variable that receives
 *                                      the number of bytes read.
 *
 *  \retval NFCSTATUS_PENDING           HCP Frame receive pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Other related errors
 *
 *
 */


static
NFCSTATUS
phHciNfc_Receive_HCP (
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef,
                            uint8_t                 *pdata,
#ifdef ONE_BYTE_LEN
                            uint8_t                 length
#else
                            uint16_t                length
#endif
                     );


/*
################################################################################
***************************** Function Definitions *****************************
################################################################################
*/


#if  (NXP_NFC_HCI_TIMER == 1)

static
void
phHciNfc_Response_Timeout (
                    uint32_t resp_timer_id, void *pContext
                )
{
    phNfc_sCompletionInfo_t  comp_info = {0,0,0};

    if ( ( NULL != gpsHciContext)
            && (resp_timer_id == hci_resp_timer_id ))
    {
        pphNfcIF_Notification_CB_t  p_upper_notify =
            gpsHciContext->p_upper_notify;
        void                        *p_upper_context =
                                gpsHciContext->p_upper_context;
        phHal_sHwReference_t        *pHwRef = gpsHciContext->p_hw_ref;
		uint32_t				i = 0;


        HCI_DEBUG(" HCI TIMEOUT: HCI Response Timeout Occurred in %X Timer\n"
                                                                 ,resp_timer_id);
        /* Stop the Response Timer */
        phOsalNfc_Timer_Stop( hci_resp_timer_id );

		comp_info.status = PHNFCSTVAL(CID_NFC_HCI,
                        NFCSTATUS_BOARD_COMMUNICATION_ERROR); 
        /* Roll Back to the Select State */
        phHciNfc_FSM_Rollback(gpsHciContext);

		for(i=0;i < PHHCINFC_MAX_PIPE; i++)
		{
			phHciNfc_Reset_Pipe_MsgInfo(gpsHciContext->p_pipe_list[i]);
		}

        /* Notify the Error/Success Scenario to the upper layer */
        phHciNfc_Notify( p_upper_notify, p_upper_context,
                    pHwRef, (uint8_t) NFC_NOTIFY_DEVICE_ERROR, &comp_info );
    }

    return ;

}

#endif /* (NXP_NFC_HCI_TIMER == 1) */



/*!
 * \brief Allocation of the HCI Interface resources.
 *
 * This function releases and frees all the resources used by HCI Command and
 * Response Mechanism
 */

 NFCSTATUS
 phHciNfc_Allocate_Resource (
                                void                **ppBuffer,
                                uint16_t            size
                            )
{
    NFCSTATUS           status = NFCSTATUS_SUCCESS;

    *ppBuffer = (void *) phOsalNfc_GetMemory(size);
    if( *ppBuffer != NULL )
    {
        (void )memset(((void *)*ppBuffer), 0,
                                    size);
    }
    else
    {
        *ppBuffer = NULL;
        status = PHNFCSTVAL(CID_NFC_HCI,
                        NFCSTATUS_INSUFFICIENT_RESOURCES);
    }
    return status;
}



/*!
 * \brief Release of the HCI Interface resources.
 *
 * This function releases and frees all the resources used by HCI Command and
 * Response Mechanism
 */
 void
 phHciNfc_Release_Resources (
                                phHciNfc_sContext_t **ppsHciContext
                            )
{
    uint8_t i = 0;


#if  (NXP_NFC_HCI_TIMER == 1)

    if ( NXP_INVALID_TIMER_ID != hci_resp_timer_id )
    {
        /* Stop and Un-Intialise the Response Timer */
        phOsalNfc_Timer_Stop( hci_resp_timer_id );
        phOsalNfc_Timer_Delete( hci_resp_timer_id );
        HCI_DEBUG(" HCI : Timer %X Stopped and Released\n",
                                            hci_resp_timer_id);
        hci_resp_timer_id = NXP_INVALID_TIMER_ID;
    }
    gpsHciContext = NULL;

#endif /* (NXP_NFC_HCI_TIMER == 1) */


    if(NULL != (*ppsHciContext)->p_admin_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_admin_info);
        (*ppsHciContext)->p_admin_info = NULL;
    }
    if(NULL !=(*ppsHciContext)->p_link_mgmt_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_link_mgmt_info);
        (*ppsHciContext)->p_link_mgmt_info = NULL;
    }
    if(NULL !=(*ppsHciContext)->p_identity_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_identity_info);
        (*ppsHciContext)->p_identity_info = NULL;
    }
    if(NULL !=(*ppsHciContext)->p_device_mgmt_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_device_mgmt_info);
        (*ppsHciContext)->p_device_mgmt_info = NULL;
    }
    if(NULL !=(*ppsHciContext)->p_reader_mgmt_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_reader_mgmt_info);
        (*ppsHciContext)->p_reader_mgmt_info = NULL;
    }
    if(NULL !=(*ppsHciContext)->p_poll_loop_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_poll_loop_info);
        (*ppsHciContext)->p_poll_loop_info = NULL;
    }
    if(NULL !=(*ppsHciContext)->p_reader_a_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_reader_a_info);
        (*ppsHciContext)->p_reader_a_info = NULL;
    }
#ifdef TYPE_B
    if(NULL !=(*ppsHciContext)->p_reader_b_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_reader_b_info);
        (*ppsHciContext)->p_reader_b_info = NULL;
    }
#endif
#ifdef TYPE_FELICA
    if(NULL !=(*ppsHciContext)->p_felica_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_felica_info);
        (*ppsHciContext)->p_felica_info = NULL;
    }
#endif
#ifdef TYPE_JEWEL
    if(NULL !=(*ppsHciContext)->p_jewel_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_jewel_info);
        (*ppsHciContext)->p_jewel_info = NULL;
    }
#endif
#ifdef  TYPE_ISO15693
    if(NULL !=(*ppsHciContext)->p_iso_15693_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_iso_15693_info);
        (*ppsHciContext)->p_iso_15693_info = NULL;
    }
#endif /* #ifdef    TYPE_ISO15693 */
#ifdef ENABLE_P2P
    if(NULL !=(*ppsHciContext)->p_nfcip_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_nfcip_info);
        (*ppsHciContext)->p_nfcip_info = NULL;
    }
#endif
    if(NULL !=(*ppsHciContext)->p_emulation_mgmt_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_emulation_mgmt_info);
        (*ppsHciContext)->p_emulation_mgmt_info = NULL;
    }
    if(NULL !=(*ppsHciContext)->p_wi_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_wi_info);
        (*ppsHciContext)->p_wi_info = NULL;
    }
    if(NULL !=(*ppsHciContext)->p_swp_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_swp_info);
        (*ppsHciContext)->p_swp_info = NULL;
    }
    if(NULL !=(*ppsHciContext)->p_uicc_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_uicc_info);
        (*ppsHciContext)->p_uicc_info = NULL;
    }
#ifdef HOST_EMULATION
    if(NULL !=(*ppsHciContext)->p_ce_a_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_ce_a_info);
        (*ppsHciContext)->p_ce_a_info = NULL;
    }
    if(NULL !=(*ppsHciContext)->p_ce_b_info)
    {
        phOsalNfc_FreeMemory((*ppsHciContext)->p_ce_b_info);
        (*ppsHciContext)->p_ce_b_info = NULL;
    }
#endif

    for(i=0;i < PHHCINFC_MAX_PIPE; i++)
    {
        if(NULL != (*ppsHciContext)->p_pipe_list[i])
        {
            phOsalNfc_FreeMemory((*ppsHciContext)->p_pipe_list[i]);
        }
    }


    phOsalNfc_FreeMemory((*ppsHciContext));
    (*ppsHciContext) = NULL;

    return ;
}

static
void
phHciNfc_Reset_Pipe_MsgInfo(    
                            phHciNfc_Pipe_Info_t    *p_pipe_info
                        )
{
    if (p_pipe_info != NULL)
    {
        p_pipe_info->sent_msg_type = HCP_MSG_TYPE_RESERVED;
        p_pipe_info->prev_msg = MSG_INSTRUCTION_UNKNWON;
        p_pipe_info->prev_status = NFCSTATUS_INVALID_HCI_INSTRUCTION;
        p_pipe_info->param_info = NULL;
        p_pipe_info->param_length = FALSE ;
    }
    return;
}


void
phHciNfc_Release_Lower(
                    phHciNfc_sContext_t         *psHciContext,
                    void                        *pHwRef
               )
{
    phNfc_sLowerIF_t            *plower_if = 
                                    &(psHciContext->lower_interface);
    NFCSTATUS            status = NFCSTATUS_SUCCESS;

    PHNFC_UNUSED_VARIABLE(status);
    if(NULL != plower_if->release)
    {
        status = plower_if->release((void *)plower_if->pcontext,
                                        (void *)pHwRef);
        (void) memset((void *)plower_if, 0, sizeof(phNfc_sLowerIF_t));
        HCI_DEBUG(" HCI Releasing the Lower Layer Resources: Status = %02X\n"
                                                                    ,status);
    }

    return;
}



/*!
 * \brief Sends the HCI Commands to the corresponding peripheral device.
 *
 * This function sends the HCI Commands to the connected NFC Pheripheral device
 */
 static
 NFCSTATUS
 phHciNfc_Send (
                      void                  *psContext,
                      void                  *pHwRef,
                      uint8_t               *pdata,
#ifdef ONE_BYTE_LEN
                      uint8_t               length
#else
                      uint16_t              length
#endif
                     )
{
    phHciNfc_sContext_t     *psHciContext= (phHciNfc_sContext_t  *)psContext;
    NFCSTATUS               status = NFCSTATUS_SUCCESS;

    phNfc_sLowerIF_t        *plower_if = &(psHciContext->lower_interface);

    if( (NULL != plower_if) 
        && (NULL != plower_if->send)
      )
    {
        HCI_DEBUG("HCI: In Function: %s \n", __FUNCTION__);
        HCI_DEBUG("HCI: Response Pending status --> %s \n",
            (psHciContext->response_pending)?"TRUE":"FALSE");
        HCI_PRINT_BUFFER("Send Buffer",pdata,length);
        /* psHciContext->hci_transact_state = NFC_TRANSACT_SEND_IN_PROGRESS; */

#if  (NXP_NFC_HCI_TIMER == 1)

    if ( 
        (TRUE != psHciContext->tx_hcp_chaining)
        &&  (TRUE == psHciContext->response_pending)
        && ( NXP_INVALID_TIMER_ID != hci_resp_timer_id )
       )
    {
        /* Start the HCI Response Timer */
        phOsalNfc_Timer_Start( hci_resp_timer_id,
                nxp_nfc_hci_response_timeout, phHciNfc_Response_Timeout, NULL );
        HCI_DEBUG(" HCI : Timer %X Started \n", hci_resp_timer_id);
    }

#endif /* (NXP_NFC_HCI_TIMER == 1) */

        status = plower_if->send((void *)plower_if->pcontext,
                                (void *)pHwRef, pdata, length);
    }

    return status;
}


/*!
 * \brief Receives the HCI Response from the corresponding peripheral device.
 *
 * This function receives the HCI Command Response to the connected NFC
 * Pheripheral device.
 */

NFCSTATUS
phHciNfc_Receive(
                        void                *psContext,
                        void                *pHwRef,
                        uint8_t             *pdata,
#ifdef ONE_BYTE_LEN
                        uint8_t             length
#else
                        uint16_t            length
#endif
                    )
{
    phHciNfc_sContext_t     *psHciContext= (phHciNfc_sContext_t  *)psContext;
    phNfc_sLowerIF_t *plower_if = NULL ;
    NFCSTATUS         status = NFCSTATUS_SUCCESS;

    if(NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        plower_if = &(psHciContext->lower_interface);

        if( (NULL != plower_if) 
            && (NULL != plower_if->receive)
          )
        {
            /* psHciContext->hci_transact_state = NFC_TRANSACT_RECV_IN_PROGRESS; */
            status = plower_if->receive((void *)plower_if->pcontext,
                                    (void *)pHwRef, pdata, length);
        }
    }
    return status;
}


/*!
 * \brief Sends the HCP Packet to the lower link layer .
 *
 * This function Sends the HCI Data in the HCP packet format to the below
 * Link layer.
 */

 NFCSTATUS
 phHciNfc_Send_HCP (
                                phHciNfc_sContext_t *psHciContext,
                                void                *pHwRef
                   )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phHciNfc_HCP_Packet_t   *tx_data = (phHciNfc_HCP_Packet_t *)
                                    psHciContext->send_buffer;
    /* Skip the HCP Header Byte initially */
    uint16_t                tx_length = psHciContext->tx_total - 1 ;
    uint16_t                hcp_index = HCP_ZERO_LEN;
    uint8_t                 pipe_id = (uint8_t) HCI_UNKNOWN_PIPE_ID;
    static  uint8_t         chain_bit = HCP_CHAINBIT_DEFAULT;

    pipe_id =  (uint8_t) GET_BITS8( tx_data->hcp_header,
        HCP_PIPEID_OFFSET, HCP_PIPEID_LEN);

    /* Fragmentation of the HCP Frames */
    if ( tx_length > PHHCINFC_MAX_PACKET_DATA )
    {
        tx_data = &psHciContext->tx_packet;
        (void)memset((void *)tx_data, FALSE,
                        sizeof(phHciNfc_HCP_Packet_t));
        if (HCP_CHAINBIT_DEFAULT == chain_bit)
        {
            /* HCI Chaining Needs to be Done */
            psHciContext->tx_remain = tx_length;
            psHciContext->tx_hcp_frgmnt_index = HCP_ZERO_LEN ;
            chain_bit = HCP_CHAINBIT_BEGIN;
            /* Increment the Fragment index to skip the HCP Header */
            psHciContext->tx_hcp_frgmnt_index++; 
            psHciContext->tx_hcp_chaining = TRUE ;
            tx_length = PHHCINFC_MAX_PACKET_DATA ;
        }
        else if ( psHciContext->tx_remain > PHHCINFC_MAX_PACKET_DATA )
        {
			/* Intermediate Chained HCI Frames */
            tx_length = PHHCINFC_MAX_PACKET_DATA ;
        }
        else
        {
            /* End of Chaining Reached */
            chain_bit = HCP_CHAINBIT_END;
            tx_length = psHciContext->tx_remain ;
            psHciContext->tx_hcp_chaining = FALSE ;
        }
        
        /* Build the HCP Header to have Chaining Enabled */
        phHciNfc_Build_HCPHeader(tx_data, chain_bit , pipe_id );

        phHciNfc_Append_HCPFrame((uint8_t *)tx_data->msg.payload, hcp_index, 
            (&psHciContext->send_buffer[psHciContext->tx_hcp_frgmnt_index])
            , tx_length );
    }
    else
    {
        /* No Chaining Required */
        chain_bit = HCP_CHAINBIT_DEFAULT;

        psHciContext->tx_hcp_chaining = FALSE ;

        psHciContext->tx_remain = tx_length ;
    }
    
    /* Include the Skipped HCP Header Byte */
    tx_length++;

    status = phHciNfc_Send ( (void *) psHciContext, pHwRef,
                        (uint8_t *)tx_data, tx_length );
    
    return status;
}


/*!
 * \brief Receives the HCP Packet from the lower link layer .
 *
 * This function receives the HCI Data in the HCP packet format from the below
 * Link layer.
 */
 static
 NFCSTATUS
 phHciNfc_Receive_HCP (
                                phHciNfc_sContext_t *psHciContext,
                                void                *pHwRef,
                                uint8_t             *pdata,
#ifdef ONE_BYTE_LEN
                                uint8_t             length
#else
                                uint16_t            length
#endif
                   )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phHciNfc_HCP_Packet_t   *packet = NULL;
    uint8_t                 chainbit = HCP_CHAINBIT_DEFAULT;
    uint16_t                hcp_index = 0;

    packet = (phHciNfc_HCP_Packet_t *)pdata;
    chainbit = (uint8_t) GET_BITS8( packet->hcp_header,
        HCP_CHAINBIT_OFFSET, HCP_CHAINBIT_LEN);
    hcp_index = psHciContext->rx_hcp_frgmnt_index;
    HCI_PRINT_BUFFER("Receive Buffer",((uint8_t *)pdata),length);
    if (HCP_CHAINBIT_BEGIN == chainbit)
    {
        /* pdata = (uint8_t *)&psHciContext->rx_packet; */
        /* De Fragmentation of the Received HCP Frames */
        /* Subsequent Chaining Frames */
        if( hcp_index  > 0 )
        {
            /* Copy the obtained fragment and receive the next fragment */
            phHciNfc_Append_HCPFrame(
                psHciContext->recv_buffer, hcp_index, 
                    (uint8_t *)&pdata[HCP_MESSAGE_LEN],
                            (length - HCP_MESSAGE_LEN) );
            psHciContext->rx_hcp_frgmnt_index =(uint16_t)
                        (hcp_index + length - HCP_MESSAGE_LEN);
        }
        /* First Chaining Frame*/
        else
        {
            psHciContext->rx_hcp_chaining = TRUE ;
            /* Copy the obtained fragment and receive the next fragment */
            phHciNfc_Append_HCPFrame(psHciContext->recv_buffer, 
                hcp_index, pdata, length);
            psHciContext->rx_hcp_frgmnt_index = ( hcp_index + length ) ;

        }
        status = phHciNfc_Receive ( (void *) psHciContext, pHwRef,
                                                pdata, length);
    } 
    else
    {
        if(TRUE == psHciContext->rx_hcp_chaining)
        {
            /* If the chaining was done earlier */
            psHciContext->rx_hcp_chaining = FALSE ;
            /* Copy the Remaining buffer to the RX_BUFFER */
            phHciNfc_Append_HCPFrame(
                psHciContext->recv_buffer, hcp_index, 
                    (uint8_t *)&pdata[HCP_MESSAGE_LEN],
                            (length - HCP_MESSAGE_LEN) );
            /* If there is chaining done the return the same data */
            psHciContext->rx_total = 
                        (hcp_index + length - HCP_MESSAGE_LEN);
            psHciContext->rx_hcp_frgmnt_index = FALSE ;
        }
        else
        {
            (void) memcpy( psHciContext->recv_buffer, pdata, length);
            /* If there is no chaining done then return the same data */
            psHciContext->rx_total = (hcp_index + length);

        }
    }

    return status;
}


/*!
 * \brief Receives the HCP Packet from the lower link layer .
 *
 * This function receives the HCI Data in the HCP packet format from the below
 * Link layer.
 */

 static
 NFCSTATUS
 phHciNfc_Process_HCP (
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef,
                                void                    *pdata,
#ifdef ONE_BYTE_LEN
                                uint8_t             length
#else
                                uint16_t            length
#endif
                      )
{
    phHciNfc_HCP_Packet_t   *packet = NULL;
    phHciNfc_HCP_Message_t  *message = NULL;
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    uint8_t                 msg_type = 0;

    if( (NULL == pdata)
        || ( length < HCP_HEADER_LEN )
      )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        status = phHciNfc_Receive_HCP( psHciContext, pHwRef, pdata, length ); 
    }/* End of the Valid Data Handling */

    if( NFCSTATUS_SUCCESS  == status )
    {
        packet = (phHciNfc_HCP_Packet_t *)psHciContext->recv_buffer;
        length = 
#ifdef ONE_BYTE_LEN
            (uint8_t)
#endif
            psHciContext->rx_total ;
        message = &packet->msg.message;
        /* HCI_PRINT_BUFFER("Total Receive Buffer",((uint8_t *)pdata),length); */
        msg_type = (uint8_t) GET_BITS8( message->msg_header,
            HCP_MSG_TYPE_OFFSET, HCP_MSG_TYPE_LEN);
        switch ( msg_type )
        {
            case HCP_MSG_TYPE_RESPONSE:
            {
                status = phHciNfc_Process_Response( psHciContext,
                                                pHwRef, (void *)packet, length );
                break;
            }
            case HCP_MSG_TYPE_EVENT:
            {
                status = phHciNfc_Process_Event( psHciContext,
                                                pHwRef,(void *)packet, length );
                break;
            }
            case HCP_MSG_TYPE_COMMAND:
            {

                status = phHciNfc_Process_Command( psHciContext,
                                                pHwRef, (void *)packet, length );
                break;
            }
            /* case HCP_MSG_TYPE_RESERVED: */
            default:
            {
                status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_RESPONSE);
                break;
            }
        }
    }/* End of Receive HCP Status */
    return status;
}


 static
 NFCSTATUS
 phHciNfc_Process_Response (
                                 phHciNfc_sContext_t    *psHciContext,
                                 void                   *pHwRef,
                                 void                   *pdata,
#ifdef ONE_BYTE_LEN
                                 uint8_t             length
#else
                                 uint16_t            length
#endif
                             )
{
    phHciNfc_HCP_Packet_t   *packet = NULL;
    phHciNfc_HCP_Message_t  *message = NULL;
    uint8_t                 instruction=0;
    uint8_t                 pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;
    phHciNfc_Pipe_Info_t    *p_pipe_info = NULL;

    NFCSTATUS               status = NFCSTATUS_SUCCESS;

    packet = (phHciNfc_HCP_Packet_t *)pdata;
    message = &packet->msg.message;
    /* Get the instruction bits from the Message Header */
    instruction = (uint8_t) GET_BITS8( message->msg_header,
                                HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);
    /* Get the Pipe ID from the HCP Header */
    pipe_id =  (uint8_t) GET_BITS8( packet->hcp_header,
                                HCP_PIPEID_OFFSET, HCP_PIPEID_LEN);

#if  (NXP_NFC_HCI_TIMER == 1)

    if ( NXP_INVALID_TIMER_ID != hci_resp_timer_id )
    {
        /* Stop the HCI Response Timer */
        HCI_DEBUG(" HCI : Timer %X Stopped \n", hci_resp_timer_id);
        phOsalNfc_Timer_Stop( hci_resp_timer_id );
    }

#endif /* (NXP_NFC_HCI_TIMER == 1) */
    
    if (pipe_id >=  PHHCINFC_MAX_PIPE )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else if( ((uint8_t) ANY_OK != instruction)
        && ( (pipe_id !=    PIPETYPE_STATIC_ADMIN )
        && ( ADM_CLEAR_ALL_PIPE != (psHciContext->p_pipe_list[pipe_id])->prev_msg ))
        )
    {
        status = phHciNfc_Error_Response( psHciContext, pHwRef, pdata, length );
    }
    else
    {
        p_pipe_info = psHciContext->p_pipe_list[pipe_id];
        if( ( NULL != p_pipe_info )
            &&   ( HCP_MSG_TYPE_COMMAND == p_pipe_info->sent_msg_type  )
            &&   ( NULL != p_pipe_info->recv_resp )
        )
        {
            status = psHciContext->p_pipe_list[pipe_id]->recv_resp( psHciContext,
                                                            pHwRef, pdata, length );
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
        }
        /* There is no Pending Response */
        psHciContext->response_pending = FALSE ;
        HCI_DEBUG("HCI: Response Pending status --> FALSE, %s \n",
            __FUNCTION__);
        if( NFCSTATUS_SUCCESS == status )
        {
            phHciNfc_Reset_Pipe_MsgInfo(psHciContext->p_pipe_list[pipe_id]);
            status = phHciNfc_Resume_Sequence(psHciContext, pHwRef);

        }/* End of Success Status validation */
        else
        {
            HCI_DEBUG("HCI: Status --> %X \n", status );
        }

    } /* End of the Valid Response handling */
    return status;
}


static
 NFCSTATUS
 phHciNfc_Error_Response (
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef,
                                void                    *pdata,
#ifdef ONE_BYTE_LEN
                                uint8_t                 length
#else
                                uint16_t                length
#endif
                         )
{

    phHciNfc_HCP_Packet_t   *packet = (phHciNfc_HCP_Packet_t *)pdata;
    phHciNfc_HCP_Message_t  *message = &packet->msg.message;
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    uint8_t                 pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;
#if defined(HCI_TRACE) || defined (ERROR_INSTRUCTION)
    uint8_t                 instruction = 0;
    instruction = (uint8_t) GET_BITS8(message->msg_header,
                            HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);
#endif

    /* Get the Pipe ID from the HCP Header */
    pipe_id =  (uint8_t) GET_BITS8( packet->hcp_header,
                                HCP_PIPEID_OFFSET, HCP_PIPEID_LEN);
    /* Process the Error Response based on the obtained instruction */
#ifdef ERROR_INSTRUCTION
    switch(instruction)
    {
        case ANY_E_NOT_CONNECTED:
        case ANY_E_CMD_PAR_UNKNOWN:
        case ANY_E_NOK:
        case ANY_E_PIPES_FULL:
        case ANY_E_REG_PAR_UNKNOWN:
        case ANY_E_PIPE_NOT_OPENED:
        case ANY_E_CMD_NOT_SUPPORTED:
        case ANY_E_TIMEOUT:
        case ANY_E_REG_ACCESS_DENIED:
        case ANY_E_PIPE_ACCESS_DENIED:
        {
            /* Receive Error Notification to the Upper Layer */
            status = PHNFCSTVAL( CID_NFC_HCI, \
                            message->msg_header);
            phHciNfc_Error_Sequence(psHciContext, pHwRef, status , pdata, length );
            /* Return Success as the Error Sequence is already handled */
            psHciContext->response_pending = FALSE ;
            HCI_DEBUG("HCI: Response Pending status --> FALSE, %s \n",
            __FUNCTION__);
            status = NFCSTATUS_SUCCESS;
            break;
        }
            /* The Statement should not reach this case */
        /* case ANY_OK: */
        default:
        {
            /* status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_RESPONSE); */
            break;
        }
    }
#else
    status = PHNFCSTVAL( CID_NFC_HCI, message->msg_header);
    HCI_DEBUG("HCI Error Response(%u) from the Device \n", instruction);
    psHciContext->response_pending = FALSE ;
    HCI_DEBUG("HCI: Response Pending status --> FALSE, %s \n",
        __FUNCTION__);
    phHciNfc_Reset_Pipe_MsgInfo(psHciContext->p_pipe_list[pipe_id]);
    phHciNfc_Error_Sequence(psHciContext, pHwRef, status , pdata, (uint8_t) length );
    /* Return Success as the Error Sequence is already handled */
    status = NFCSTATUS_SUCCESS;
#endif

    return status;
}


static
 NFCSTATUS
 phHciNfc_Process_Event (
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef,
                                void                    *pdata,
#ifdef ONE_BYTE_LEN
                                uint8_t                 length
#else
                                uint16_t                length
#endif
                        )
{
    phHciNfc_HCP_Packet_t   *packet = NULL;
    phHciNfc_HCP_Message_t  *message = NULL;
    phHciNfc_Pipe_Info_t    *p_pipe_info = NULL;
    uint8_t                 instruction=0;
    uint8_t                 pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;

    NFCSTATUS               status = NFCSTATUS_SUCCESS;

    packet = (phHciNfc_HCP_Packet_t *)pdata;
    message = &packet->msg.message;
    /* Get the instruction bits from the Message Header */
    PHNFC_UNUSED_VARIABLE(instruction);
    instruction = (uint8_t) GET_BITS8( message->msg_header,
                                HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);
    /* Get the Pipe ID from the HCP Header */
    pipe_id =  (uint8_t) GET_BITS8( packet->hcp_header,
                                HCP_PIPEID_OFFSET, HCP_PIPEID_LEN);

    if (pipe_id >=  PHHCINFC_MAX_PIPE )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        p_pipe_info = psHciContext->p_pipe_list[pipe_id];
    }

    if( (p_pipe_info != NULL ) )
    {
        if( NULL != p_pipe_info->recv_event)
        {
            status = p_pipe_info->recv_event( psHciContext, pHwRef,
                                                        pdata, length );
        }
        else
        {
            HCI_DEBUG(" Event Handling Not Supported by the #%u Pipe \n",
                                                        pipe_id);
            status = PHNFCSTVAL(CID_NFC_HCI,
                                        NFCSTATUS_FEATURE_NOT_SUPPORTED);
        }
    }
    else
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }

    HCI_DEBUG("HCI: In Function: %s \n",
        __FUNCTION__);
    HCI_DEBUG("HCI: Response Pending status --> %s \n",
        (psHciContext->response_pending)?"TRUE":"FALSE");
    HCI_DEBUG("HCI: Event Pending status --> %s \n",
        (psHciContext->event_pending)?"TRUE":"FALSE");

        if ((TRUE == psHciContext->response_pending)
        || (TRUE == psHciContext->event_pending))
    {
        (void)memset(psHciContext->recv_buffer,
            FALSE, PHHCINFC_MAX_BUFFERSIZE);
        (void)memset((void *)&psHciContext->rx_packet,
            FALSE, sizeof(phHciNfc_HCP_Packet_t));

        /* Reset the Received Data Index */
        psHciContext->rx_index = ZERO;
        /* Reset the size of the total response data received */
        psHciContext->rx_total = ZERO;

        /* psHciContext->hci_transact_state = NFC_TRANSACT_SEND_COMPLETE;*/
        /* Receive the Response Packet */

        status = phHciNfc_Receive( psHciContext, pHwRef, 
                    (uint8_t *)(&psHciContext->rx_packet), 
                    sizeof(phHciNfc_HCP_Packet_t) );

        /* HCI_DEBUG("HCI Lower Layer Send Completion After Receive,\
        Status = %02X\n",status); */
    }
    else
    {
        if( 
/* #define EVENT_NOTIFY */
#ifndef EVENT_NOTIFY
            ( NFCSTATUS_SUCCESS == status  )
            || ( NFCSTATUS_RF_TIMEOUT == status  )
            || (( NFCSTATUS_MORE_INFORMATION == status  )
#else
            ((FALSE == psHciContext->event_pending )
#endif
            && ( pipe_id <= PHHCINFC_MAX_PIPE ))
            )
        {
            /* phHciNfc_Reset_Pipe_MsgInfo(psHciContext->p_pipe_list[pipe_id]); */
            status = phHciNfc_Resume_Sequence(psHciContext, pHwRef);

        }/* End of Success Status validation */
        else
        {
            HCI_DEBUG(" HCI: Pipe-ID --> %02X  \n", pipe_id);
            HCI_DEBUG(" HCI: PROCESS EVENT - Pending/Invalid Status : %X\n", status);
        }
    }

    return status;
}

static
 NFCSTATUS
 phHciNfc_Process_Command (
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef,
                                void                    *pdata,
#ifdef ONE_BYTE_LEN
                                uint8_t                 length
#else
                                uint16_t                length
#endif
                             )
{
    phHciNfc_HCP_Packet_t   *packet = NULL;
    phHciNfc_HCP_Message_t  *message = NULL;
    phHciNfc_Pipe_Info_t    *p_pipe_info = NULL;
    uint8_t                 instruction=0;
    uint8_t                 pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;

    NFCSTATUS               status = NFCSTATUS_SUCCESS;

    packet = (phHciNfc_HCP_Packet_t *)pdata;
    message = &packet->msg.message;
    /* Get the instruction bits from the Message Header */
    PHNFC_UNUSED_VARIABLE(instruction);

    instruction = (uint8_t) GET_BITS8( message->msg_header,
                                HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);
    /* Get the Pipe ID from the HCP Header */
    pipe_id =  (uint8_t) GET_BITS8( packet->hcp_header,
                                HCP_PIPEID_OFFSET, HCP_PIPEID_LEN);
    if (pipe_id >=  PHHCINFC_MAX_PIPE )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        p_pipe_info = psHciContext->p_pipe_list[pipe_id];
    }

    if( (p_pipe_info != NULL )
        )
    {
        if( NULL != p_pipe_info->recv_cmd)
        {
            status = p_pipe_info->recv_cmd( psHciContext,   pHwRef,
                                                        pdata, length );
        }
        else
        {
            HCI_DEBUG(" Command Handling Not Supported by the #%u Pipe \n",
                                                        pipe_id);
            status = PHNFCSTVAL(CID_NFC_HCI,
                                        NFCSTATUS_FEATURE_NOT_SUPPORTED);
        }
    }
    else
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }

    HCI_DEBUG("HCI: In Function: %s \n", __FUNCTION__);
    HCI_DEBUG("HCI: Response Pending status --> %s \n",
        (psHciContext->response_pending)?"TRUE":"FALSE");

    if(( NFCSTATUS_SUCCESS == status )
        && (TRUE != psHciContext->response_pending)
        )
    {
        /* Reset the Pipe Information Stored in the particular Pipe */
        /* phHciNfc_Reset_Pipe_MsgInfo(psHciContext->p_pipe_list[pipe_id]); */
        /* Resume the Execution Sequence */
        status = phHciNfc_Resume_Sequence(psHciContext, pHwRef);

    }/* End of Success Status validation */

    return status;
}


static
void
phHciNfc_Build_HCPMessage(
                                phHciNfc_HCP_Packet_t *hcp_packet,
                                uint8_t             msg_type,
                                uint8_t             instruction
                          )
{
    phHciNfc_HCP_Message_t  *hcp_message = NULL;

    hcp_message = &(hcp_packet->msg.message);
    /* Set the type to the provided message type in the HCP Message Header */ 
    hcp_message->msg_header = (uint8_t) SET_BITS8(hcp_message->msg_header,HCP_MSG_TYPE_OFFSET,
                HCP_MSG_TYPE_LEN, msg_type);
    /* Set the instruction to the kind of instruction in the HCP Message Header */ 
    hcp_message->msg_header  = (uint8_t) SET_BITS8(hcp_message->msg_header,HCP_MSG_INSTRUCTION_OFFSET,
                HCP_MSG_INSTRUCTION_LEN, instruction);
    /* hcp_message->msg_header = hcp_message->msg_header | temp ; */

}


static
void
phHciNfc_Build_HCPHeader(
                                phHciNfc_HCP_Packet_t *hcp_packet,
                                uint8_t             chainbit,
                                uint8_t             pipe_id
                          )
{
    /* Set the Chaining bit to the default type */ 
    hcp_packet->hcp_header = (uint8_t) SET_BITS8(hcp_packet->hcp_header,
                HCP_CHAINBIT_OFFSET, HCP_CHAINBIT_LEN, chainbit);
    /* Populate the Pipe ID to the HCP Header */ 
    hcp_packet->hcp_header  = (uint8_t) SET_BITS8(hcp_packet->hcp_header,HCP_PIPEID_OFFSET,
                HCP_PIPEID_LEN, pipe_id);

}

/*!
 * \brief Builds the HCP Frame Packet.
 *
 * This function builds the HCP Frame in the HCP packet format to send to the 
 * connected reader device.
 */

void
 phHciNfc_Build_HCPFrame (
                                phHciNfc_HCP_Packet_t *hcp_packet,
                                uint8_t             chainbit,
                                uint8_t             pipe_id,
                                uint8_t             msg_type,
                                uint8_t             instruction
                          )
{
    /* Fills the HCP Header in the packet */
    phHciNfc_Build_HCPHeader( hcp_packet,chainbit,pipe_id );
    /* Fills the HCP Message in the packet */
    phHciNfc_Build_HCPMessage( hcp_packet,msg_type,instruction );
}

/*!
 * \brief Appends the HCP Frame Packet.
 *
 * This function Appends the HCP Frame of the HCP packet to complete the
 * entire HCP Packet.
 */

void
 phHciNfc_Append_HCPFrame (
/*                              phHciNfc_sContext_t     *psHciContext, */
                                uint8_t                 *hcp_data,
                                uint16_t                hcp_index,
                                uint8_t                 *src_data,
                                uint16_t                src_len
                          )
{
    uint16_t src_index = 0;
    if( (NULL != src_data) 
        /* && (hcp_index >= 0) */
        && (src_len > 0) 
        )
    {
        for(src_index=0; src_index < src_len ; src_index++)
        {
            hcp_data[hcp_index + src_index] = src_data[src_index];
        }
    }
    return;
}


/*!
 * \brief Sends the Generic HCI Commands to the connected reader device.
 *
 * This function Sends the Generic HCI Command frames in the HCP packet format to the 
 * connected reader device.
 */

 NFCSTATUS
 phHciNfc_Send_Generic_Cmd (
                                phHciNfc_sContext_t *psHciContext,
                                void                *pHwRef,
                                uint8_t             pipe_id,
                                uint8_t             cmd
                    )
 {
    phHciNfc_HCP_Packet_t   *hcp_packet = NULL;
    phHciNfc_HCP_Message_t  *hcp_message = NULL;
    phHciNfc_Pipe_Info_t    *p_pipe_info = NULL;
    uint16_t                 length = 0;
    uint16_t                 i=0;
    NFCSTATUS               status = NFCSTATUS_SUCCESS;

    if((NULL == psHciContext)
        || ( pipe_id > PHHCINFC_MAX_PIPE)
        ||(NULL == psHciContext->p_pipe_list[pipe_id])
      )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
        HCI_DEBUG("%s: Invalid Arguments passed \n",
                                                "phHciNfc_Send_Generic_Cmd");
    }
    else
    {
        p_pipe_info = (phHciNfc_Pipe_Info_t *) 
                                psHciContext->p_pipe_list[pipe_id];
        psHciContext->tx_total = 0 ;
        length +=  HCP_HEADER_LEN ;
        switch( cmd )
        {
            case ANY_SET_PARAMETER:
            {
                
                hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;
                /* Construct the HCP Frame */
                phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                                        (uint8_t) pipe_id, HCP_MSG_TYPE_COMMAND, cmd);
                hcp_message = &(hcp_packet->msg.message);
                hcp_message->payload[i++] = p_pipe_info->reg_index ;
                phHciNfc_Append_HCPFrame((uint8_t *)hcp_message->payload,
                                            i, (uint8_t *)p_pipe_info->param_info,
                                            p_pipe_info->param_length);
                length =(uint16_t)(length + i + p_pipe_info->param_length);
                break;
            }
            case ANY_GET_PARAMETER:
            {

                hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;
                /* Construct the HCP Frame */
                phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                                        (uint8_t) pipe_id, HCP_MSG_TYPE_COMMAND, cmd);
                hcp_message = &(hcp_packet->msg.message);
                hcp_message->payload[i++] = p_pipe_info->reg_index ;
                length =(uint16_t)(length + i);
                break;
            }
            case ANY_OPEN_PIPE:
            case ANY_CLOSE_PIPE:
            {

                hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;
                /* Construct the HCP Frame */
                phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                                        (uint8_t) pipe_id, HCP_MSG_TYPE_COMMAND, cmd);
                break;
            }
            default:
            {
                status = PHNFCSTVAL( CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED );
                HCI_DEBUG("%s: Statement Should Not Occur \n","phHciNfc_Send_Generic_Cmd");
                break;
            }
        }
        if( NFCSTATUS_SUCCESS == status )
        {
            p_pipe_info->sent_msg_type = HCP_MSG_TYPE_COMMAND;
            p_pipe_info->prev_msg = cmd;
            psHciContext->tx_total = length;
            psHciContext->response_pending = TRUE ;
            /* Send the Constructed HCP packet to the lower layer */
            status = phHciNfc_Send_HCP( psHciContext, pHwRef );
            p_pipe_info->prev_status = NFCSTATUS_PENDING;
        }
    }

    return status;
}


/*!
 * \brief Sets the parameter of the registers in a particular Pipe.
 *
 * This function configures the registers in a particular Pipe.
 */

NFCSTATUS
phHciNfc_Set_Param (
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef,
                                phHciNfc_Pipe_Info_t    *p_pipe_info,
                                uint8_t                 reg_index,
                                void                    *p_param,
                                uint16_t                 param_length
                    )
 {
    NFCSTATUS               status = NFCSTATUS_SUCCESS ;

    if( (NULL == p_pipe_info)
        || (NULL == p_param)
        || (0 == param_length)
        )
    {
        status = PHNFCSTVAL( CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION );
    }
    else
    {
        p_pipe_info->param_info = (uint8_t *)p_param;
        p_pipe_info->param_length =  param_length;
        p_pipe_info->reg_index = reg_index;
        status = phHciNfc_Send_Generic_Cmd( psHciContext, pHwRef, 
            (uint8_t)p_pipe_info->pipe.pipe_id,
                (uint8_t)ANY_SET_PARAMETER);
        p_pipe_info->prev_status = status;
    }

    return status;
 }


#if 0
 /*!
 * \brief Gets the parameter of the registers in a particular Pipe.
 *
 * This function configures the registers in a particular Pipe.
 */

 NFCSTATUS
 phHciNfc_Get_Param (
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef,
                                phHciNfc_Pipe_Info_t    *p_pipe_info,
                                uint8_t                 reg_index,
                    )
 {
    NFCSTATUS               status = NFCSTATUS_SUCCESS ;

    return status;
 }
#endif


void
phHciNfc_Send_Complete (
                                void                    *psContext,
                                void                    *pHwRef,
                                phNfc_sTransactionInfo_t *pInfo
                       )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS ;
    uint16_t                length = 0;

    HCI_PRINT("HCI Send Completion....\n");
    if ( (NULL != psContext)
        && (NULL != pInfo) 
        && (NULL != pHwRef) 
        )
    {
        phHciNfc_sContext_t *psHciContext = (phHciNfc_sContext_t *)psContext;
        status = pInfo->status ;
        length = pInfo->length ;
        /* HCI_DEBUG("HCI Lower Layer Send Completion Before Receive,\
                                                Status = %02X\n",status); */
        if(status != NFCSTATUS_SUCCESS)
        {
            /* Handle the Error Scenario */
            (void)memset(psHciContext->send_buffer,
                                            FALSE, PHHCINFC_MAX_BUFFERSIZE);
            /* psHciContext->hci_transact_state = NFC_TRANSACT_COMPLETE;*/
            phHciNfc_Error_Sequence( psHciContext, pHwRef, status, NULL, 0 );
        }
        else
        {
	        HCI_DEBUG("HCI Send Completion... Length = %02X\n", length); 
            /* To complete the send complete with the send 
             * or receive with chaining.
             */
            if( (TRUE == psHciContext->tx_hcp_chaining)
                &&( psHciContext->tx_remain > HCP_ZERO_LEN ))
            {
                /* Skip the HCP Header Byte Sent */
                psHciContext->tx_remain -= length - 1;

                /* Skip the HCP Header Byte Sent */
                psHciContext->tx_hcp_frgmnt_index += length - 1;

                /* Send the Remaining HCP Data Frames */
                status = phHciNfc_Send_HCP( psHciContext, pHwRef );

                HCI_DEBUG("HCI (Chaining) Send Resume: Status = %02X\n", status);

                if( ( NFCSTATUS_SUCCESS != status )
                    && (NFCSTATUS_PENDING != status )
                    )
                {
                    phHciNfc_Error_Sequence( psHciContext, pHwRef, status, NULL, 0 );
                }/* End of the Status check */
            }
            else
            {
                psHciContext->tx_total = HCP_ZERO_LEN ;
                psHciContext->tx_remain = HCP_ZERO_LEN ;
                psHciContext->tx_hcp_frgmnt_index = HCP_ZERO_LEN ;
                HCI_DEBUG("HCI: %s: response_pending=%s, event_pending=%s",
                        __FUNCTION__,
                        (psHciContext->response_pending)?"TRUE":"FALSE",
                        (psHciContext->event_pending)?"TRUE":"FALSE"
                         );
                if ((TRUE == psHciContext->response_pending)
                    || (TRUE == psHciContext->event_pending))
                {
                    (void) memset(psHciContext->recv_buffer,
                        FALSE, PHHCINFC_MAX_BUFFERSIZE);
                    (void) memset((void *)&psHciContext->rx_packet,
                        FALSE, sizeof(phHciNfc_HCP_Packet_t));

                    /* Reset the Received Data Index */
                    psHciContext->rx_index = ZERO;
                    /* Reset the size of the total response data received */
                    psHciContext->rx_total = ZERO;

                    /* psHciContext->hci_transact_state = NFC_TRANSACT_SEND_COMPLETE;*/
                    /* Receive the Response Packet */
                    status = phHciNfc_Receive( psHciContext, pHwRef, 
                                (uint8_t *)(&psHciContext->rx_packet), 
                                sizeof(phHciNfc_HCP_Packet_t) );

                    /* HCI_DEBUG("HCI Lower Layer Send Completion After Receive,\
                    Status = %02X\n",status); */

                    if( ( NFCSTATUS_SUCCESS != status )
                         && (NFCSTATUS_PENDING != status )
                        )
                    {
                        phHciNfc_Error_Sequence( psHciContext, pHwRef, status, NULL, 0 );
                    }/* End of the Status check */
                }
                else
                {
                    status = phHciNfc_Resume_Sequence(psHciContext, pHwRef );
                } 
            } 

        } /* End of status != Success */

    } /* End of Context != NULL  */
}


void
phHciNfc_Receive_Complete (
                                void                    *psContext,
                                void                    *pHwRef,
                                phNfc_sTransactionInfo_t *pInfo
                                )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS ;
    void                    *pdata = NULL ;
    uint16_t                length = 0 ;

    HCI_PRINT("HCI Receive Completion....\n");
    if ( (NULL != psContext)
        && (NULL != pInfo) 
        && (NULL != pHwRef) 
        )
    {
        phHciNfc_sContext_t *psHciContext = (phHciNfc_sContext_t *)psContext;

        status = pInfo->status ;
        pdata = pInfo->buffer ;
        length = pInfo->length ;
        HCI_DEBUG("HCI Lower Layer Receive Completion, Status = %02X\n",status);
        if( NFCSTATUS_SUCCESS != status )
        {
            /* Handle the Error Scenario */
            /* psHciContext->hci_transact_state = NFC_TRANSACT_COMPLETE; */
            phHciNfc_Error_Sequence(psHciContext, pHwRef, status , pdata, (uint8_t)length );
        }
        else
        {
             /* Receive the remaining Response Packet */
            /* psHciContext->hci_transact_state = NFC_TRANSACT_RECV_COMPLETE; */
            status = phHciNfc_Process_HCP( psHciContext, pHwRef, pdata,(uint8_t) length );
            if( ( NFCSTATUS_SUCCESS != status )
                && (NFCSTATUS_PENDING != status )
              )
            {
                phHciNfc_Error_Sequence(psHciContext, pHwRef, status , pdata, (uint8_t) length );
            }
        }
    }
}

void
phHciNfc_Notify(
                    pphNfcIF_Notification_CB_t  p_upper_notify,
                    void                        *p_upper_context,
                    void                        *pHwRef,
                    uint8_t                     type,
                    void                        *pInfo
               )
{
    if( ( NULL != p_upper_notify) )
    {
        /* Notify the to the Upper Layer */
        (p_upper_notify)(p_upper_context, pHwRef, type, pInfo);
    }

}


void
phHciNfc_Tag_Notify(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef,
                            uint8_t                 type,
                            void                    *pInfo
               )
{
    phNfc_sCompletionInfo_t *psCompInfo = 
                                (phNfc_sCompletionInfo_t *)pInfo;
    pphNfcIF_Notification_CB_t  p_upper_notify = psHciContext->p_upper_notify;
    void                        *pcontext = psHciContext->p_upper_context;
    NFCSTATUS               status = NFCSTATUS_SUCCESS;

    switch( psHciContext->hci_state.next_state )
    {
        case hciState_Activate:
        {
            /* Roll Back to the Select State */
            phHciNfc_FSM_Rollback(psHciContext);
            break;
        }
        case hciState_Select:
        {
            status = phHciNfc_FSM_Complete(psHciContext);
            break;
        }
        default:
        {
            /* Roll Back to the Select State */
            phHciNfc_FSM_Rollback(psHciContext);
            break;
        }

    }

    if(NFCSTATUS_SUCCESS == status )
    {
            /* Notify the Tag Events to the Upper layer */
        phHciNfc_Notify( p_upper_notify, pcontext , pHwRef,
                                type, psCompInfo);
    }
    else
    {
        phHciNfc_Error_Sequence( psHciContext, pHwRef, status, NULL, 0 );
    }
}


void
phHciNfc_Target_Select_Notify(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef,
                            uint8_t                 type,
                            void                    *pInfo
               )
{
    phNfc_sCompletionInfo_t *psCompInfo = 
                                (phNfc_sCompletionInfo_t *)pInfo;
    pphNfcIF_Notification_CB_t  p_upper_notify = psHciContext->p_upper_notify;
    void                        *pcontext = psHciContext->p_upper_context;
    NFCSTATUS               status = NFCSTATUS_SUCCESS;

    switch( psHciContext->hci_state.next_state )
    {
        case hciState_Listen:
        {
            /* Roll Back to the Select State */
            status = phHciNfc_FSM_Complete(psHciContext);
            break;
        }
        case hciState_Select:
        {
            status = phHciNfc_FSM_Complete(psHciContext);
            break;
        }
        default:
        {
            /* Roll Back to the Select State */
            phHciNfc_FSM_Rollback(psHciContext);
            break;
        }

    }

    if(NFCSTATUS_SUCCESS == status )
    {
            /* Notify the Tag Events to the Upper layer */
        phHciNfc_Notify( p_upper_notify, pcontext , pHwRef,
                                type, psCompInfo);
    }
    else
    {
        phHciNfc_Error_Sequence( psHciContext, pHwRef, status, NULL, 0 );
    }

}




void
phHciNfc_Release_Notify(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef,
                            uint8_t                 type,
                            void                    *pInfo
               )
{
    phNfc_sCompletionInfo_t *psCompInfo = 
                                (phNfc_sCompletionInfo_t *)pInfo;
    pphNfcIF_Notification_CB_t  p_upper_notify = psHciContext->p_upper_notify;
    void                        *pcontext = psHciContext->p_upper_context;
    phHciNfc_Release_Resources( &psHciContext );
        /* Notify the Failure to the Upper Layer */
    phHciNfc_Notify( p_upper_notify, pcontext , pHwRef,
                            type, psCompInfo);
}


void
phHciNfc_Notify_Event(
                            void                    *psContext,
                            void                    *pHwRef,
                            uint8_t                 type,
                            void                    *pInfo
                    )
{
    NFCSTATUS            status = NFCSTATUS_SUCCESS;

    if ( (NULL != psContext)
        && (NULL != pInfo) 
        && (NULL != pHwRef) 
        )
    {
        phHciNfc_sContext_t *psHciContext = (phHciNfc_sContext_t *)psContext;

        /* Process based on the Notification type */
        switch(type)
        {
            case NFC_NOTIFY_INIT_COMPLETED:
            {
                phNfc_sCompletionInfo_t *psCompInfo = 
                                            (phNfc_sCompletionInfo_t *)pInfo;
                if(NFCSTATUS_SUCCESS == psCompInfo->status)
                {

#if  (NXP_NFC_HCI_TIMER == 1)
                    if ( NXP_INVALID_TIMER_ID == hci_resp_timer_id )
                    {
                        /* Create and Intialise the Response Timer */
                        hci_resp_timer_id = phOsalNfc_Timer_Create( );
                        HCI_DEBUG(" HCI : Timer %X Created \n",
                                                            hci_resp_timer_id);
                    }
                    else
                    {
                        HCI_DEBUG(" HCI : Timer Already Created, Timer ID : %X\n",
                                                                hci_resp_timer_id);
                    }
                    gpsHciContext = psHciContext;

#endif /* (NXP_NFC_HCI_TIMER == 1) */

                     /* Complete the Initialisation Sequence */
                    status = phHciNfc_Resume_Sequence(psContext ,pHwRef);
                }
                else
                {
                    /* Notify the Error Scenario to the Upper Layer */
                    phHciNfc_Notify(psHciContext->p_upper_notify,
                                    psHciContext->p_upper_context, pHwRef,
                                            NFC_NOTIFY_ERROR, psCompInfo);
                }
                break;
            }
            case NFC_NOTIFY_INIT_FAILED:
            {
                 /* Notify the Failure to the Upper Layer */
                phHciNfc_Release_Notify( psContext,pHwRef,
                                        type, pInfo );
                break;
            }
            case NFC_NOTIFY_RECV_COMPLETED:
            {
                /* Receive Completed from the Lower Layer */
                phHciNfc_Receive_Complete(psContext,pHwRef,pInfo);

                break;
            }
            case NFC_NOTIFY_SEND_COMPLETED:
            {
                /* Receive Completed from the Lower Layer */
                phHciNfc_Send_Complete(psContext,pHwRef,pInfo);

                break;
            }
            case NFC_NOTIFY_TRANSCEIVE_COMPLETED:
            {
                /* TODO: TO handle Both Send and Receive Complete */
                break;
            }
            case NFC_NOTIFY_TARGET_DISCOVERED:
            {
                HCI_PRINT(" PICC Discovery ! Obtain PICC Info .... \n");
                /* psHciContext->hci_seq = PL_DURATION_SEQ; */
                if ( hciState_Unknown == psHciContext->hci_state.next_state )
                {

                    status = phHciNfc_FSM_Update ( psHciContext, hciState_Select );


                    if (NFCSTATUS_SUCCESS != status)
                    {
                       status = phHciNfc_ReaderMgmt_Deselect( 
                            psHciContext, pHwRef, phHal_eISO14443_A_PICC, FALSE); 
                    }
                }
                else
                {
#ifdef SW_RELEASE_TARGET
                    /*status = phHciNfc_ReaderMgmt_Deselect( 
                        psHciContext, pHwRef, phHal_eISO14443_A_PICC, FALSE); */
                    psHciContext->target_release = TRUE;
#else
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_STATE);
#endif
                }
                break;
            }
            /* To Notify the Target Released Notification 
             * to the Above Layer */
            case NFC_NOTIFY_TARGET_RELEASED:
            /* To Notify the NFC Secure Element Transaction 
             * Information to the Above Layer */
            /* case NFC_NOTIFY_TRANSACTION: */
            /* To Notify the Generic Events To the Upper 
             * Layer */
            case NFC_NOTIFY_EVENT:
            /* To Notify the Data Receive  Notification 
             * to the Above Layer */
            case NFC_NOTIFY_RECV_EVENT:
            {
                phNfc_sCompletionInfo_t *psCompInfo = 
		                (phNfc_sCompletionInfo_t *)pInfo;

                if (((TRUE == psHciContext->event_pending) || 
                    (NFCSTATUS_RF_TIMEOUT == psCompInfo->status))
                    && ( hciState_Transact == psHciContext->hci_state.next_state))
                {
                    /* Rollback due to Transmission Error */
                    phHciNfc_FSM_Rollback(psHciContext);
                }
                psHciContext->event_pending = FALSE;
                phHciNfc_Notify(psHciContext->p_upper_notify,
                            psHciContext->p_upper_context, pHwRef,
                                type, pInfo);
                break;
            }
            case NFC_NOTIFY_DEVICE_ACTIVATED:
            {
                HCI_PRINT("  Device Activated! Obtaining Remote Reader Info .... \n");
                if ( hciState_Unknown == psHciContext->hci_state.next_state )
                {
                    switch (psHciContext->host_rf_type)
                    {
                        case phHal_eISO14443_A_PCD:
                        case phHal_eISO14443_B_PCD:
                        case phHal_eISO14443_BPrime_PCD:
                        case phHal_eFelica_PCD:
                        {
                            break;
                        }
                        case phHal_eNfcIP1_Initiator:
                        case phHal_eNfcIP1_Target:
                        {
                            break;
                        }
                        case phHal_eUnknown_DevType:
                        default:
                        {
                            status = PHNFCSTVAL(CID_NFC_HCI, 
                                        NFCSTATUS_INVALID_PARAMETER);
                            break;
                        }

                    }
                    status = phHciNfc_FSM_Update ( psHciContext, hciState_Listen );
                }
                else
                {
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_STATE);
                }
                break;
            }
            case NFC_NOTIFY_DEVICE_DEACTIVATED:
            {
                HCI_PRINT(" Device De-Activated! \n");
                if ( hciState_Unknown == psHciContext->hci_state.next_state )
                {
                    status = phHciNfc_FSM_Update ( psHciContext, hciState_Initialise );
                    if(NFCSTATUS_SUCCESS == status)
                    {
                        /* Complete to the Select State */
                        status = phHciNfc_FSM_Complete(psHciContext);
                    }
                    else
                    {
                        HCI_PRINT(" Device Deactivated.. But Invalid State \n");
                        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_STATE);
                    }
                }
                else
                {
                    status = phHciNfc_ReaderMgmt_Update_Sequence(
                                                psHciContext, INFO_SEQ );

                    if(( hciState_Listen == psHciContext->hci_state.next_state)
                        || (hciState_Transact == psHciContext->hci_state.next_state))
                    {
                        psHciContext->hci_state.next_state = hciState_Initialise;
                        /* Roll Back to the Default State */
                        status = phHciNfc_FSM_Complete(psHciContext);
                    }
                }
                psHciContext->event_pending = FALSE;
                phHciNfc_Notify(psHciContext->p_upper_notify,
                            psHciContext->p_upper_context, pHwRef,
                            NFC_NOTIFY_EVENT, pInfo);
                break;
            }
            case NFC_NOTIFY_DEVICE_ERROR:
            {
                phNfc_sCompletionInfo_t *psCompInfo = 
                                            (phNfc_sCompletionInfo_t *)pInfo;

                psCompInfo->status = ( NFCSTATUS_BOARD_COMMUNICATION_ERROR 
                                        != PHNFCSTATUS(psCompInfo->status))?
                                            NFCSTATUS_BOARD_COMMUNICATION_ERROR:
                                                psCompInfo->status ;

#if  (NXP_NFC_HCI_TIMER == 1)

                if ( NXP_INVALID_TIMER_ID != hci_resp_timer_id )
                {
                    HCI_DEBUG(" HCI : Response Timer Stop, Status:%02X",
                                                          psCompInfo->status);
                    /* Stop and Un-Intialise the Response Timer */
                    phOsalNfc_Timer_Stop( hci_resp_timer_id );
                }

#endif /* (NXP_NFC_HCI_TIMER == 1) */

                phHciNfc_Notify(psHciContext->p_upper_notify,
                            psHciContext->p_upper_context, pHwRef,
                            (uint8_t) NFC_NOTIFY_DEVICE_ERROR, pInfo);

                break;
            }

            case NFC_NOTIFY_ERROR:
            default:
            {
                phNfc_sCompletionInfo_t *psCompInfo = 
                                            (phNfc_sCompletionInfo_t *)pInfo;

#if  (NXP_NFC_HCI_TIMER == 1)

                if (( NFCSTATUS_BOARD_COMMUNICATION_ERROR == PHNFCSTATUS(psCompInfo->status))
                        && ( NXP_INVALID_TIMER_ID != hci_resp_timer_id ))                
                {
                    HCI_DEBUG(" HCI : Response Timer Stop, Status:%02X",
                                                          psCompInfo->status);
                    /* Stop the HCI Response Timer */
                    phOsalNfc_Timer_Stop( hci_resp_timer_id );
                }

#endif /* (NXP_NFC_HCI_TIMER == 1) */

                phHciNfc_Error_Sequence( psHciContext, pHwRef, 
                                                        psCompInfo->status, NULL, 0);
                break;
            }
        } /* End of Switch */
    } /* End of Context != NULL  */
}

