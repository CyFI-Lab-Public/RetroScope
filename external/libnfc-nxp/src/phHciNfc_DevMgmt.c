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
* \file  phHciNfc_DevMgmt.c                                                   *
* \brief HCI PN544 Device Management Gate Routines.                           *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Fri Mar 12 10:21:54 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.29 $                                                            *
* $Aliases: NFC_FRI1.1_WK1007_R33_3,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                                                *
*                                                                             *
* =========================================================================== *
*/

/*
***************************** Header File Inclusion ****************************
*/
#include <phNfcConfig.h>
#include <phNfcCompId.h>
#include <phHciNfc_Pipe.h>
#include <phHciNfc_DevMgmt.h>
#include <phHciNfc_Emulation.h>
#include <phOsalNfc.h>

/*
****************************** Macro Definitions *******************************
*/

/*  Commands for System Management module */
#define NXP_RF_CHECK_SETTINGS   0x10U
#define NXP_RF_UPDATE_SETTINGS  0x11U

/* Self Test Commands */
#define NXP_SELF_TEST_ANTENNA   0x20U
#define NXP_SELF_TEST_SWP       0x21U
#define NXP_SELF_TEST_NFCWI     0x22U
#define NXP_SELF_TEST_PRBS      0x25U

/* System Management propreitary control */
#define NXP_DBG_READ            0x3EU
#define NXP_DBG_WRITE           0x3FU

/* System Management Events */
#define NXP_EVT_SET_AUTONOMOUS   0x01U
#define NXP_EVT_CLF_WAKEUP       0x02U

/* System Management Information Events */
#define NXP_EVT_INFO_TXLDO_OVERCUR   0x10U
#define NXP_EVT_INFO_PMUVCC          0x11U
#define NXP_EVT_INFO_EXT_RF_FIELD    0x12U
#define NXP_EVT_INFO_MEM_VIOLATION   0x13U
#define NXP_EVT_INFO_TEMP_OVERHEAT   0x14U
#define NXP_EVT_INFO_LLC_ERROR       0x15U

#define NFC_DEV_TXLDO_MASK           0x03U


/*
*************************** Structure and Enumeration ***************************
*/


/** \defgroup grp_hci_nfc HCI PN544 Device Management Component
 *
 *
 */

typedef enum phHciNfc_DevMgmt_Seq{
    DEV_MGMT_PIPE_OPEN      = 0x00U,
    DEV_MGMT_SET_PWR_STATUS,
    DEV_MGMT_SET_INFO_EVT,
    DEV_MGMT_GET_EEPROM_INFO,
    DEV_MGMT_GPIO_PDIR,
    DEV_MGMT_GPIO_PEN,
    DEV_MGMT_TX_LDO,
    DEV_MGMT_IFC_TO_RX_H,
    DEV_MGMT_IFC_TO_RX_L,
    DEV_MGMT_IFC_TO_TX_H,
    DEV_MGMT_IFC_TO_TX_L,
    DEV_MGMT_ANAIRQ_CONF,
    DEV_MGMT_PMOS_MOD,
    DEV_MGMT_CLK_REQ,
    DEV_MGMT_INPUT_CLK,
    DEV_MGMT_UICC_PWR_REQUEST,
    DEV_MGMT_ACTIVE_GUARD_TO,
    DEV_MGMT_MAX_ACT_TO_LOW,
    DEV_MGMT_MAX_ACT_TO_HIGH,
    DEV_MGMT_UICC_CE_A_ACCESS,
    DEV_MGMT_UICC_CE_B_ACCESS,
    DEV_MGMT_UICC_CE_BP_ACCESS,
    DEV_MGMT_UICC_CE_F_ACCESS,
    DEV_MGMT_UICC_RD_A_ACCESS,
    DEV_MGMT_UICC_RD_B_ACCESS,
    DEV_MGMT_UICC_BIT_RATE,
    DEV_MGMT_UICC_RX_ERR_CNT,
    DEV_MGMT_UICC_TX_ERR_CNT,
    DEV_MGMT_LLC_GRD_TO_H,
    DEV_MGMT_LLC_GRD_TO_L,
    DEV_MGMT_LLC_ACK_TO_H,
    DEV_MGMT_LLC_ACK_TO_L,
    DEV_MGMT_FELICA_RC,
    DEV_MGMT_EVT_AUTONOMOUS,
    DEV_MGMT_PIPE_CLOSE
} phHciNfc_DevMgmt_Seq_t;


typedef struct phHciNfc_DevMgmt_Info{
    phHciNfc_DevMgmt_Seq_t  current_seq;
    phHciNfc_DevMgmt_Seq_t  next_seq;
    phHciNfc_Pipe_Info_t    *p_pipe_info;
    uint8_t                 test_status;
    uint8_t                 value;
    uint8_t                 rf_status;
    uint8_t                 pmuvcc_status;
    uint8_t                 overheat_status;
    uint8_t                 *p_val;
    uint8_t                 eeprom_crc;
    phNfc_sData_t           test_result;

} phHciNfc_DevMgmt_Info_t;


/*
*************************** Static Function Declaration **************************
*/

static
NFCSTATUS
phHciNfc_DevMgmt_InfoUpdate(
                                phHciNfc_sContext_t     *psHciContext,
                                phHal_sHwReference_t    *pHwRef,
                                uint8_t                 index,
                                uint8_t                 *reg_value,
                                uint8_t                 reg_length
                         );

static
NFCSTATUS
phHciNfc_Recv_DevMgmt_Response(
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
phHciNfc_Recv_DevMgmt_Event(
                        void                *psContext,
                        void                *pHwRef,
                        uint8_t             *pEvent,
#ifdef ONE_BYTE_LEN
                        uint8_t             length
#else
                        uint16_t            length
#endif
                    );


static
NFCSTATUS
phHciNfc_Send_DevMgmt_Command (
                            phHciNfc_sContext_t *psHciContext,
                            void                *pHwRef,
                            uint8_t             pipe_id,
                            uint8_t             cmd
                );

static
 NFCSTATUS
 phHciNfc_Send_DevMgmt_Event (
                                phHciNfc_sContext_t *psHciContext,
                                void                *pHwRef,
                                uint8_t             pipe_id,
                                uint8_t             event
                    );

/*
*************************** Function Definitions ***************************
*/


NFCSTATUS
phHciNfc_DevMgmt_Init_Resources(
                           phHciNfc_sContext_t  *psHciContext
                          )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_DevMgmt_Info_t          *p_device_mgmt_info=NULL;
   
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if(( NULL == psHciContext->p_device_mgmt_info ) &&
             (phHciNfc_Allocate_Resource((void **)(&p_device_mgmt_info),
            sizeof(phHciNfc_DevMgmt_Info_t))== NFCSTATUS_SUCCESS))
        {
            psHciContext->p_device_mgmt_info = p_device_mgmt_info;
            p_device_mgmt_info->current_seq = DEV_MGMT_PIPE_OPEN;
            p_device_mgmt_info->next_seq = DEV_MGMT_PIPE_OPEN;
            p_device_mgmt_info->p_pipe_info = NULL;
        }
        else
        {
            status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INSUFFICIENT_RESOURCES);
        }
    }
    return status;
}


NFCSTATUS
phHciNfc_DevMgmt_Get_PipeID(
                            phHciNfc_sContext_t        *psHciContext,
                            uint8_t                    *ppipe_id
                            )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL != psHciContext)
        && ( NULL != ppipe_id )
        && ( NULL != psHciContext->p_device_mgmt_info ) 
        )
    {
        phHciNfc_Pipe_Info_t     *p_pipe_info = NULL;
        p_pipe_info = ((phHciNfc_DevMgmt_Info_t *)
            psHciContext->p_device_mgmt_info)->p_pipe_info ;
        if (NULL != p_pipe_info)
        {
            *ppipe_id = p_pipe_info->pipe.pipe_id ;
        }
        else
        {
            *ppipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;
        }

    }
    else 
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    return status;
}


NFCSTATUS
phHciNfc_DevMgmt_Get_Test_Result(
                                phHciNfc_sContext_t        *psHciContext,
                                phNfc_sData_t              *p_test_result
                            )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL != psHciContext)
        && ( NULL != p_test_result )
        && ( NULL != psHciContext->p_device_mgmt_info ) 
        )
    {
        phHciNfc_DevMgmt_Info_t *p_device_mgmt_info=NULL;
        p_device_mgmt_info = (phHciNfc_DevMgmt_Info_t *)
                                psHciContext->p_device_mgmt_info ;
        p_test_result->buffer = p_device_mgmt_info->test_result.buffer;
        p_test_result->length = p_device_mgmt_info->test_result.length;

    }
    else 
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }

    return status;
}


NFCSTATUS
phHciNfc_DevMgmt_Set_Test_Result(
                                phHciNfc_sContext_t        *psHciContext,
                                uint8_t                    test_status
                            )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;

    if( (NULL != psHciContext)
        && ( NULL != psHciContext->p_device_mgmt_info ) 
        )
    {
        phHciNfc_DevMgmt_Info_t *p_device_mgmt_info=NULL;
        p_device_mgmt_info = (phHciNfc_DevMgmt_Info_t *)
                                psHciContext->p_device_mgmt_info ;
        p_device_mgmt_info->test_status = test_status;
        
    }
    else 
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }

    return status;
}



NFCSTATUS
phHciNfc_DevMgmt_Update_PipeInfo(
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
    else if(NULL == psHciContext->p_device_mgmt_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        phHciNfc_DevMgmt_Info_t *p_device_mgmt_info=NULL;
        p_device_mgmt_info = (phHciNfc_DevMgmt_Info_t *)
                                psHciContext->p_device_mgmt_info ;
        /* Update the pipe_info of the Device Management Gate obtained 
         * from HCI Response */
        p_device_mgmt_info->p_pipe_info = pPipeInfo;
        if (( NULL != pPipeInfo)
            && ((uint8_t)HCI_UNKNOWN_PIPE_ID != pipeID)
            )
        {
            /* Update the Response Receive routine of the Device 
             * Managment Gate */
            pPipeInfo->recv_resp = &phHciNfc_Recv_DevMgmt_Response;
            pPipeInfo->recv_event = &phHciNfc_Recv_DevMgmt_Event;
        }
    }

    return status;
}


 NFCSTATUS
 phHciNfc_DevMgmt_Configure (
                                phHciNfc_sContext_t *psHciContext,
                                void                *pHwRef,
                                uint16_t            address,
                                uint8_t             value
                    )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_Pipe_Info_t        *p_pipe_info = NULL;
    uint8_t                     pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;
    uint8_t                     i=0;
    uint8_t                     params[5];

    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(  NULL == psHciContext->p_device_mgmt_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        p_pipe_info = ((phHciNfc_DevMgmt_Info_t *)
                       psHciContext->p_device_mgmt_info)->p_pipe_info ;

        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                            NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            pipe_id = p_pipe_info->pipe.pipe_id ;
            params[i++] = 0x00;
            params[i++] = (uint8_t)(address >> BYTE_SIZE);
            params[i++] = (uint8_t)address;
            params[i++] = value;
            p_pipe_info->param_info = &params;
            p_pipe_info->param_length = i ;
            status = phHciNfc_Send_DevMgmt_Command( psHciContext, pHwRef, 
                                                pipe_id, (uint8_t)NXP_DBG_WRITE );
        }
    }
    return status;
}


 NFCSTATUS
 phHciNfc_DevMgmt_Get_Info (
                                phHciNfc_sContext_t *psHciContext,
                                void                *pHwRef,
                                uint16_t            address,
                                uint8_t             *p_val
                    )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_Pipe_Info_t        *p_pipe_info = NULL;
    uint8_t                     pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;
    uint8_t                     i=0;
    uint8_t                     params[5];

    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(  NULL == psHciContext->p_device_mgmt_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        p_pipe_info = ((phHciNfc_DevMgmt_Info_t *)
                       psHciContext->p_device_mgmt_info)->p_pipe_info ;

        if(NULL == p_pipe_info )
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                            NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            ((phHciNfc_DevMgmt_Info_t *)
                    psHciContext->p_device_mgmt_info)->p_val = p_val;
            pipe_id = p_pipe_info->pipe.pipe_id ;
            params[i++] = 0x00;
            params[i++] = (uint8_t)(address >> BYTE_SIZE);
            params[i++] = (uint8_t) address;
            p_pipe_info->param_info = &params;
            p_pipe_info->param_length = i ;
            status = phHciNfc_Send_DevMgmt_Command( psHciContext, pHwRef, 
                                            pipe_id, (uint8_t)NXP_DBG_READ );
        }
    }
    return status;

}

/*!
 * \brief Initialisation of PN544 Device Managment Gate.
 *
 * This function initialses the PN544 Device Management gate and 
 * populates the PN544 Device Management Information Structure
 * 
 */

NFCSTATUS
phHciNfc_DevMgmt_Initialise(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                         )
{
    NFCSTATUS                       status = NFCSTATUS_SUCCESS;
    phHciNfc_Pipe_Info_t            *p_pipe_info = NULL;
    phHciNfc_DevMgmt_Info_t         *p_device_mgmt_info=NULL;
    static uint8_t                   config = 0x10;

    if( ( NULL == psHciContext )
        || (NULL == pHwRef )
        )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if (NULL == psHciContext->p_device_mgmt_info)
    {
        status = PHNFCSTVAL(CID_NFC_HCI,
                    NFCSTATUS_INVALID_HCI_INFORMATION);
    }/* End of the PN544 Device Info Memory Check */
    else
    {
        p_device_mgmt_info = (phHciNfc_DevMgmt_Info_t *)
                                psHciContext->p_device_mgmt_info ;
        p_pipe_info = p_device_mgmt_info->p_pipe_info;

        if (NULL == p_pipe_info)
        {
            status = PHNFCSTVAL(CID_NFC_HCI, 
                            NFCSTATUS_INVALID_HCI_SEQUENCE);
        }
        else
        {
            switch(p_device_mgmt_info->current_seq )
            {
                /* PN544 Device Mgmt pipe open sequence */
                case DEV_MGMT_PIPE_OPEN:
                {
                    status = phHciNfc_Open_Pipe( psHciContext,
                                                    pHwRef, p_pipe_info );
                    if(status == NFCSTATUS_SUCCESS)
                    {
                        p_device_mgmt_info->next_seq =
                                    DEV_MGMT_FELICA_RC;
                        status = NFCSTATUS_PENDING;
                    }
                    break;
                }
                case DEV_MGMT_GET_EEPROM_INFO:
                {
                    p_pipe_info->reg_index = DEVICE_INFO_EEPROM_INDEX;
                    status = phHciNfc_Send_Generic_Cmd( psHciContext, 
                            pHwRef, (uint8_t)p_pipe_info->pipe.pipe_id,
                                (uint8_t)ANY_GET_PARAMETER);
                    if(NFCSTATUS_PENDING == status )
                    {
#if  ( NXP_NFC_IFC_TIMEOUT & 0x01 )
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_IFC_TO_TX_H;
#else
                        p_device_mgmt_info->next_seq =
                                        DEV_MGMT_TX_LDO;
#endif /* #if  ( NXP_NFC_IFC_TIMEOUT & 0x01 ) */
                    }
                    break;
                }
                case DEV_MGMT_GPIO_PDIR:
                {
                    config = 0x00;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_GPIO_PDIR , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_GPIO_PEN;
                    }
                    break;
                }
                case DEV_MGMT_GPIO_PEN:
                {
                    config = NXP_NFC_GPIO_MASK(NXP_DOWNLOAD_GPIO)| 0x03 ;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_GPIO_PEN , config );
                    if(NFCSTATUS_PENDING == status )
                    {
#if  ( NXP_NFC_IFC_TIMEOUT & 0x01 )
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_IFC_TO_TX_H;
#else
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_TX_LDO;
#endif /* #if  ( NXP_NFC_IFC_TIMEOUT & 0x01 ) */
                    }
                    break;
                }
                case DEV_MGMT_FELICA_RC:
                {
                    config = 0x00;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                                 NFC_FELICA_RC_ADDR , config );
                    if(NFCSTATUS_PENDING == status )
                    {

                        if ((HCI_SELF_TEST == psHciContext->init_mode )
                            || (HCI_NFC_DEVICE_TEST == psHciContext->init_mode ))
                        {
                            p_device_mgmt_info->next_seq =
                                   DEV_MGMT_GPIO_PDIR;
                        }
                        else
                        {
                            p_device_mgmt_info->next_seq =
                                   DEV_MGMT_GET_EEPROM_INFO;
                        }
                    }
                    break;
                }
                
#if  ( NXP_NFC_IFC_TIMEOUT & 0x01 )

                case DEV_MGMT_IFC_TO_TX_H:
                {
                    config =  (uint8_t)
                        ( NXP_NFC_IFC_CONFIG_DEFAULT >> BYTE_SIZE ) /* 0x03 */;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                        NFC_ADDRESS_IFC_TO_TX_H , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_IFC_TO_TX_L;
                    }
                    break;
                }
                case DEV_MGMT_IFC_TO_TX_L:
                {
                    config = (uint8_t)
                        ( NXP_NFC_IFC_CONFIG_DEFAULT & BYTE_MASK ) /* 0xE8 */;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_IFC_TO_TX_L , config );
                    if(NFCSTATUS_PENDING == status )
                    {
#if  ( NXP_NFC_IFC_TIMEOUT & 0x02 )
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_IFC_TO_RX_H;
#else
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_TX_LDO;
#endif /* #if  ( NXP_NFC_IFC_TIMEOUT & 0x02 ) */
                    }
                    break;
                }
                case DEV_MGMT_IFC_TO_RX_H:
                {
                    config = 0x10;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_IFC_TO_RX_H , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_IFC_TO_RX_L;
                    }
                    break;
                }
                case DEV_MGMT_IFC_TO_RX_L:
                {
                    config = 0x1E;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_IFC_TO_RX_L , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_TX_LDO;
                    }
                    break;
                }

#endif /* #if  ( NXP_NFC_IFC_TIMEOUT & 0x01 ) */
                case DEV_MGMT_TX_LDO:
                {
#if ( NXP_HAL_VERIFY_EEPROM_CRC & 0x01U )
                    if (0 != p_device_mgmt_info->eeprom_crc)
                    {
                        status = NFCSTATUS_FAILED;
                    }
                    else
#endif
                    {
                       config = (NFC_DEV_HWCONF_DEFAULT |
                                    (NXP_DEFAULT_TX_LDO & NFC_DEV_TXLDO_MASK));
                       status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_HW_CONF , config );
                       if(NFCSTATUS_PENDING == status )
                       {
#if ( SW_TYPE_RF_TUNING_BF & 0x01)
                           p_device_mgmt_info->next_seq = DEV_MGMT_ANAIRQ_CONF;
#else
                           p_device_mgmt_info->next_seq = DEV_MGMT_CLK_REQ;
#endif
                           /* status = NFCSTATUS_SUCCESS; */
                       }
                    }
                    break;
                }
#if ( SW_TYPE_RF_TUNING_BF & 0x01)
                /* The Analogue IRQ Configuartion */
                case DEV_MGMT_ANAIRQ_CONF:
                {
                    config = 0x04;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_ANAIRQ_CONF , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_PMOS_MOD;
                        /* status = NFCSTATUS_SUCCESS; */
                    }
                    break;
                }
                /* The PMOS Modulation Index */
                case DEV_MGMT_PMOS_MOD:
                {
                    config = NFC_DEV_PMOS_MOD_DEFAULT;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_PMOS_MOD , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_CLK_REQ;
                        /* status = NFCSTATUS_SUCCESS; */
                    }
                    break;
                }
#endif /* #if ( SW_TYPE_RF_TUNING_BF & 0x01) */
                case DEV_MGMT_CLK_REQ:
                {
                    config = ((phHal_sHwConfig_t *)
                                    psHciContext->p_config_params)->clk_req ;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_CLK_REQ , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_INPUT_CLK;
                        /* status = NFCSTATUS_SUCCESS; */
                    }
                    break;
                }
                case DEV_MGMT_INPUT_CLK:
                {
                    config = ((phHal_sHwConfig_t *)
                                    psHciContext->p_config_params)->input_clk;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_CLK_INPUT , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_UICC_PWR_REQUEST;
                    }
                    break;
                }
                case DEV_MGMT_UICC_PWR_REQUEST:
                {
                    config = NXP_UICC_PWR_REQUEST;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_SWP_PWR_REQ , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_UICC_RD_A_ACCESS;
                    }
                    break;
                }
                case DEV_MGMT_UICC_RD_A_ACCESS:
                {
#if ( NXP_UICC_RD_RIGHTS & 0x01 )
                    config = (uint8_t) phHciNfc_RFReaderAGate;
#else
                    config = 0xFFU;
#endif /* #if ( NXP_UICC_RD_RIGHTS & 0x01 ) */
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_UICC_RD_A_ACCESS , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_UICC_RD_B_ACCESS;
                    }
                    break;
                }
                case DEV_MGMT_UICC_RD_B_ACCESS:
                {
#if ( NXP_UICC_RD_RIGHTS & 0x02 )
                    config = (uint8_t) phHciNfc_RFReaderBGate;
#else
                    config = 0xFFU;
#endif /* #if ( NXP_UICC_RD_RIGHTS & 0x02 ) */
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_UICC_RD_B_ACCESS , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_UICC_CE_A_ACCESS;
                    }
                    break;
                }
                case DEV_MGMT_UICC_CE_A_ACCESS:
                {
#if defined(HOST_EMULATION) || ( NXP_UICC_CE_RIGHTS & 0x01 )
                    config = (uint8_t) phHciNfc_CETypeAGate;
#else
                    config = 0xFFU;
#endif /* #if ( NXP_UICC_CE_RIGHTS & 0x01 ) */
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_UICC_CE_A_ACCESS , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_UICC_CE_B_ACCESS;
                    }
                    break;
                }
                case DEV_MGMT_UICC_CE_B_ACCESS:
                {
#if defined(HOST_EMULATION) || ( NXP_UICC_CE_RIGHTS & 0x02 )
                    config = (uint8_t) phHciNfc_CETypeBGate;
#else
                    config = 0xFFU;
#endif /* #if ( NXP_UICC_CE_RIGHTS & 0x02 ) */
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_UICC_CE_B_ACCESS , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_UICC_CE_BP_ACCESS;
                    }
                    break;
                }
                case DEV_MGMT_UICC_CE_BP_ACCESS:
                {
#if defined(HOST_EMULATION) || ( NXP_UICC_CE_RIGHTS & 0x04 )
                    config = (uint8_t) phHciNfc_CETypeBPrimeGate;
#else
                    config = 0xFFU;
#endif /* #if ( NXP_UICC_CE_RIGHTS & 0x04 ) */
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_UICC_CE_BP_ACCESS , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_UICC_CE_F_ACCESS;
                    }
                    break;
                }
                case DEV_MGMT_UICC_CE_F_ACCESS:
                {
#if defined(HOST_EMULATION) || ( NXP_UICC_CE_RIGHTS & 0x08 )
                    config = (uint8_t) phHciNfc_CETypeFGate;
#else
                    config = 0xFFU;
#endif /* #if ( NXP_UICC_CE_RIGHTS & 0x08 ) */
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_UICC_CE_F_ACCESS , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_UICC_BIT_RATE;
                    }
                    break;
                }
                case DEV_MGMT_UICC_BIT_RATE:
                {
                    config = NXP_UICC_BIT_RATE;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_SWP_BITRATE , config );
                    if(NFCSTATUS_PENDING == status )
                    {
#if defined (CFG_PWR_STATUS) 
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_SET_PWR_STATUS;
#else
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_SET_INFO_EVT;
#endif
                    }
                    break;
                }
#ifdef CFG_PWR_STATUS
                case DEV_MGMT_SET_PWR_STATUS:
                {
                    config = NXP_SYSTEM_PWR_STATUS;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_PWR_STATUS , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_SET_INFO_EVT;
                    }
                    break;
                }
#endif
                case DEV_MGMT_SET_INFO_EVT:
                {
                    config = NXP_SYSTEM_EVT_INFO;
                    status = phHciNfc_Set_Param(psHciContext, pHwRef,
                                    p_pipe_info, DEVICE_INFO_EVT_INDEX, 
                                    (uint8_t *)&config, sizeof(config) );
                    if(NFCSTATUS_PENDING == status )
                    {
#if  ( HOST_LINK_TIMEOUT & 0x01 )
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_LLC_GRD_TO_H;
#else
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_EVT_AUTONOMOUS;
                        status = NFCSTATUS_SUCCESS;
#endif /* #if ( HOST_LINK_TIMEOUT & 0x01 ) */
                    }
                    break;
                }
#if  ( HOST_LINK_TIMEOUT & 0x01 )

                case DEV_MGMT_LLC_GRD_TO_H:
                {
                    config =(uint8_t)
                        ( NXP_NFC_LINK_GRD_CFG_DEFAULT >> BYTE_SIZE ) /* 0x00 */;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_LLC_GRD_TO_H , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_LLC_GRD_TO_L;
                    }
                    break;
                }
                case DEV_MGMT_LLC_GRD_TO_L:
                {
                    config = (uint8_t)
                        ( NXP_NFC_LINK_GRD_CFG_DEFAULT & BYTE_MASK ) /* 0x32 */;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_LLC_GRD_TO_L , config );
                    if(NFCSTATUS_PENDING == status )
                    {
#if  ( HOST_LINK_TIMEOUT & 0x02 )
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_LLC_ACK_TO_H;
#else
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_EVT_AUTONOMOUS;
                        status = NFCSTATUS_SUCCESS;
#endif /* #if ( HOST_LINK_TIMEOUT & 0x02 ) */
                    }
                    break;
                }
                case DEV_MGMT_LLC_ACK_TO_H:
                {
                    config = (uint8_t)
                        ( NXP_NFC_LINK_ACK_CFG_DEFAULT >> BYTE_SIZE )/* 0x00 */;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_LLC_ACK_TO_H , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_LLC_ACK_TO_L;
                    }
                    break;
                }
                case DEV_MGMT_LLC_ACK_TO_L:
                {
                    config = (uint8_t)
                        ( NXP_NFC_LINK_ACK_CFG_DEFAULT & BYTE_MASK ) /* 0x00 */;;
                    status = phHciNfc_DevMgmt_Configure( psHciContext, pHwRef,
                            NFC_ADDRESS_LLC_ACK_TO_L , config );
                    if(NFCSTATUS_PENDING == status )
                    {
                        p_device_mgmt_info->next_seq =
                                                DEV_MGMT_EVT_AUTONOMOUS;
                        status = NFCSTATUS_SUCCESS;
                    }
                    break;
                }

#endif /* #if ( HOST_LINK_TIMEOUT & 0x01 ) */
                default:
                {
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_SEQUENCE);
                    break;
                }

            }/* End of the Sequence Switch */
        }

    } /* End of Null Context Check */

    return status;
}

/*!
 * \brief Releases the resources allocated the PN544 Device Management.
 *
 * This function Releases the resources allocated the PN544 Device Management
 * and resets the hardware to the reset state.
 */

NFCSTATUS
phHciNfc_DevMgmt_Release(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                             )
{
    NFCSTATUS                           status = NFCSTATUS_SUCCESS;
    phHciNfc_Pipe_Info_t                *p_pipe_info = NULL;
    phHciNfc_DevMgmt_Info_t         *p_device_mgmt_info=NULL;

    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if( NULL != psHciContext->p_device_mgmt_info )
        {
            p_device_mgmt_info = (phHciNfc_DevMgmt_Info_t *)
                                psHciContext->p_device_mgmt_info ;
            switch(p_device_mgmt_info->current_seq)
            {
                
                /* PN544 Device pipe close sequence */
                case DEV_MGMT_EVT_AUTONOMOUS:
                {
                    p_pipe_info = p_device_mgmt_info->p_pipe_info;

                    p_pipe_info->param_info = NULL;
                    p_pipe_info->param_length = HCP_ZERO_LEN;

                    status = phHciNfc_Send_DevMgmt_Event(psHciContext, pHwRef, 
                                p_pipe_info->pipe.pipe_id, NXP_EVT_SET_AUTONOMOUS);
                    if(status == NFCSTATUS_PENDING)
                    {
                        p_device_mgmt_info->next_seq = DEV_MGMT_PIPE_OPEN;
                        status = NFCSTATUS_SUCCESS;
                    }
                    break;
                }
                /* PN544 Device pipe close sequence */
                case DEV_MGMT_PIPE_CLOSE:
                {
                    p_pipe_info = p_device_mgmt_info->p_pipe_info;

                    status = phHciNfc_Close_Pipe( psHciContext,
                                                        pHwRef, p_pipe_info );
                    if(status == NFCSTATUS_SUCCESS)
                    {
                        p_device_mgmt_info->next_seq = DEV_MGMT_PIPE_OPEN;
                        /* status = NFCSTATUS_PENDING; */
                    }
                    break;
                }
                default:
                {
                    status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_SEQUENCE);
                    break;
                }

            }/* End of the Sequence Switch */

        }/* End of the PN544 Device Info Memory Check */

    } /* End of Null Context Check */

    return status;
}

NFCSTATUS
phHciNfc_DevMgmt_Update_Sequence(
                                phHciNfc_sContext_t     *psHciContext,
                                phHciNfc_eSeqType_t     DevMgmt_seq
                             )
{
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_DevMgmt_Info_t          *p_device_mgmt_info=NULL;
    if( NULL == psHciContext )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if ( NULL == psHciContext->p_device_mgmt_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, 
                        NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        p_device_mgmt_info = (phHciNfc_DevMgmt_Info_t *)
                            psHciContext->p_device_mgmt_info ;
        switch(DevMgmt_seq)
        {
            case RESET_SEQ:
            case INIT_SEQ:
            {
                p_device_mgmt_info->current_seq = DEV_MGMT_PIPE_OPEN;
                p_device_mgmt_info->next_seq = DEV_MGMT_PIPE_OPEN ;
                break;
            }
            case UPDATE_SEQ:
            {
                p_device_mgmt_info->current_seq = p_device_mgmt_info->next_seq;
            
                break;
            }
            case REL_SEQ:
            {
                p_device_mgmt_info->current_seq = DEV_MGMT_EVT_AUTONOMOUS;
                p_device_mgmt_info->next_seq = DEV_MGMT_EVT_AUTONOMOUS ;
                break;
            }
            default:
            {
                break;
            }
        }/* End of Update Sequence Switch */
    }
    return status;

}



/*!
 * \brief Perform the System Management Tests  
 * provided by the corresponding peripheral device.
 *
 * This function performs the System Management Tests provided by the NFC
 * Peripheral device.
 */

NFCSTATUS
phHciNfc_DevMgmt_Test(
                    void                            *psContext,
                    void                            *pHwRef,
                    uint8_t                         test_type,
                    phNfc_sData_t                   *test_param
                 )
{
    phHciNfc_sContext_t         *psHciContext = 
                                    (phHciNfc_sContext_t *)psContext ;
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHciNfc_Pipe_Info_t        *p_pipe_info = NULL;
    uint8_t                     pipe_id = (uint8_t)HCI_UNKNOWN_PIPE_ID;

    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(  NULL == psHciContext->p_device_mgmt_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        phHciNfc_DevMgmt_Info_t *p_device_mgmt_info = (phHciNfc_DevMgmt_Info_t *)
                                        psHciContext->p_device_mgmt_info ;
        p_pipe_info = ((phHciNfc_DevMgmt_Info_t *)
                       psHciContext->p_device_mgmt_info)->p_pipe_info ;
        switch(test_type)
        {
            case NXP_SELF_TEST_ANTENNA:
            case NXP_SELF_TEST_SWP:
            case NXP_SELF_TEST_PRBS:
            /* case NXP_SELF_TEST_NFCWI: */
            {
                if (NULL != p_pipe_info)
                {
                    pipe_id = p_pipe_info->pipe.pipe_id ;
                    if ( NULL != test_param )
                    {
                        p_pipe_info->param_info = test_param->buffer;
                        p_pipe_info->param_length = (uint8_t)test_param->length;
                    }
                    p_device_mgmt_info->test_result.buffer = NULL;
                    p_device_mgmt_info->test_result.length = 0;
                    status = 
                        phHciNfc_Send_DevMgmt_Command( psHciContext, pHwRef, 
                            pipe_id, (uint8_t)test_type );
                }
                else
                {
                    status = PHNFCSTVAL(CID_NFC_HCI, 
                                        NFCSTATUS_INVALID_HCI_INFORMATION);
                }
                break;
            }
            default:
            {
                status = PHNFCSTVAL(CID_NFC_HCI,
                            NFCSTATUS_FEATURE_NOT_SUPPORTED);
                break;
            }
        }

    }
    return status;
}


/*!
 * \brief Receives the HCI Response from the corresponding peripheral device.
 *
 * This function receives the HCI Command Response from the connected NFC
 * Peripheral device.
 */
static
NFCSTATUS
phHciNfc_Recv_DevMgmt_Response(
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
    phHciNfc_DevMgmt_Info_t *p_device_mgmt_info=NULL;
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    uint8_t                     prev_cmd = ANY_GET_PARAMETER;

    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(  NULL == psHciContext->p_device_mgmt_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        p_device_mgmt_info = (phHciNfc_DevMgmt_Info_t *)
                            psHciContext->p_device_mgmt_info ;
        prev_cmd = p_device_mgmt_info->p_pipe_info->prev_msg ;
        switch(prev_cmd)
        {
            case ANY_GET_PARAMETER:
            {
                status = phHciNfc_DevMgmt_InfoUpdate(psHciContext,
                            (phHal_sHwReference_t *)pHwRef,
                            p_device_mgmt_info->p_pipe_info->reg_index, 
                            &pResponse[HCP_HEADER_LEN],
                                (uint8_t)(length - HCP_HEADER_LEN));
                break;
            }
            case ANY_SET_PARAMETER:
            {
                break;
            }
            case ANY_OPEN_PIPE:
            {
                break;
            }
            case ANY_CLOSE_PIPE:
            {
                phOsalNfc_FreeMemory(p_device_mgmt_info->p_pipe_info);
                p_device_mgmt_info->p_pipe_info = NULL;
                psHciContext->p_pipe_list[PIPETYPE_STATIC_LINK] = NULL;
                break;
            }
            case NXP_DBG_READ:
            /* fall through */
            case NXP_DBG_WRITE:
            {
                if( NULL != p_device_mgmt_info->p_val )
                {
                    *p_device_mgmt_info->p_val = (uint8_t)( length > HCP_HEADER_LEN ) ?
                                        pResponse[HCP_HEADER_LEN]: 0;
                    p_device_mgmt_info->p_val = NULL;
                }
                break;
            }
            /* Self Test Commands */
            case NXP_SELF_TEST_ANTENNA:
            case NXP_SELF_TEST_SWP:
            case NXP_SELF_TEST_NFCWI:
            case NXP_SELF_TEST_PRBS:
            {
                p_device_mgmt_info->test_status = (uint8_t) ( length > HCP_HEADER_LEN ) ?
                                    pResponse[HCP_HEADER_LEN]: 0;
                p_device_mgmt_info->test_result.buffer = (uint8_t)( length > HCP_HEADER_LEN ) ?
                                    &pResponse[HCP_HEADER_LEN]: NULL;
                p_device_mgmt_info->test_result.length = ( length - HCP_HEADER_LEN );
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
            if( NULL != p_device_mgmt_info->p_pipe_info)
            {
                p_device_mgmt_info->p_pipe_info->prev_status = NFCSTATUS_SUCCESS;
            }
            p_device_mgmt_info->current_seq = p_device_mgmt_info->next_seq;
        }

    }
    return status;
}

/*!
 * \brief Receives the HCI Event from the corresponding peripheral device.
 *
 * This function receives the HCI Event from the connected NFC
 * Peripheral device.
 */

static
NFCSTATUS
phHciNfc_Recv_DevMgmt_Event(
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
    phHciNfc_DevMgmt_Info_t     *p_device_mgmt_info=NULL;
    phHciNfc_HCP_Packet_t       *hcp_packet = NULL;
    phHciNfc_HCP_Message_t      *hcp_message = NULL;
    NFCSTATUS                   status = NFCSTATUS_SUCCESS;
    phHal_sEventInfo_t          event_info;
    uint8_t                     event = (uint8_t) HCP_MSG_INSTRUCTION_INVALID;

    if( (NULL == psHciContext) || (NULL == pHwRef) )
    {
      status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if(  NULL == psHciContext->p_device_mgmt_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED);
    }
    else
    {
        p_device_mgmt_info = (phHciNfc_DevMgmt_Info_t *)
                            psHciContext->p_device_mgmt_info ;
        hcp_packet = (phHciNfc_HCP_Packet_t *)pEvent;
        hcp_message = &hcp_packet->msg.message;

        /* Get the Event instruction bits from the Message Header */
        event = (uint8_t) GET_BITS8( hcp_message->msg_header,
            HCP_MSG_INSTRUCTION_OFFSET, HCP_MSG_INSTRUCTION_LEN);
        event_info.eventHost = phHal_eHostController ;
        event_info.eventSource = phHal_ePCD_DevType;

        switch(event)
        {
            /* Information Events */
            case NXP_EVT_INFO_TXLDO_OVERCUR:
            {
                event_info.eventType = NFC_INFO_TXLDO_OVERCUR;
                break;
            }
            case NXP_EVT_INFO_PMUVCC:
            {
                p_device_mgmt_info->pmuvcc_status = (uint8_t) ( length > HCP_HEADER_LEN ) ?
                                    pEvent[HCP_HEADER_LEN]: 0;
                break;
            }
            case NXP_EVT_INFO_EXT_RF_FIELD:
            {
                event_info.eventSource = phHal_ePICC_DevType ;
                p_device_mgmt_info->rf_status = (uint8_t) ( length > HCP_HEADER_LEN ) ?
                                    pEvent[HCP_HEADER_LEN]: 0;
#ifdef EVT_INFO_EXT_EVT_DIRECT
                event_info.eventType = ( CE_EVT_NFC_FIELD_ON == 
                                (p_device_mgmt_info->rf_status & 0x1FU))?
                                    NFC_EVT_FIELD_ON : NFC_EVT_FIELD_OFF;
#else
                event_info.eventType = (TRUE == p_device_mgmt_info->rf_status)?
                                    NFC_EVT_FIELD_ON : NFC_EVT_FIELD_OFF;
#endif
                break;
            }
            case NXP_EVT_INFO_MEM_VIOLATION:
            {
                event_info.eventType = NFC_INFO_MEM_VIOLATION;
                ALOGW("Your NFC controller is kinda hosed, take it to npelly@ to fix");
                break;
            }
            case NXP_EVT_INFO_TEMP_OVERHEAT:
            {
                p_device_mgmt_info->overheat_status = (uint8_t)( length > HCP_HEADER_LEN ) ?
                                    pEvent[HCP_HEADER_LEN]: 0;
                event_info.eventType = NFC_INFO_TEMP_OVERHEAT;
                event_info.eventInfo.overheat_status = 
                                    p_device_mgmt_info->overheat_status;
                break;
            }
            case NXP_EVT_INFO_LLC_ERROR:
            {
                event_info.eventType = NFC_INFO_LLC_ERROR;
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
            if( NULL != p_device_mgmt_info->p_pipe_info)
            {
                p_device_mgmt_info->p_pipe_info->prev_status = 
                                                    NFCSTATUS_SUCCESS;
            }
            p_device_mgmt_info->current_seq = 
                                        p_device_mgmt_info->next_seq;
            phHciNfc_Notify_Event(psHciContext, pHwRef, 
                                        NFC_NOTIFY_EVENT, &event_info);
        }
    }
    return status;
}


static
NFCSTATUS
phHciNfc_DevMgmt_InfoUpdate(
                                phHciNfc_sContext_t     *psHciContext,
                                phHal_sHwReference_t    *pHwRef,
                                uint8_t                 index,
                                uint8_t                 *reg_value,
                                uint8_t                 reg_length
                          )
{
    phHciNfc_DevMgmt_Info_t *p_device_mgmt_info=NULL;
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    uint8_t                 i=0;

    PHNFC_UNUSED_VARIABLE(pHwRef);
    if( (NULL == psHciContext)
        || (NULL == reg_value)
        || (reg_length == 0)
      )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
    }
    else if ( NULL == psHciContext->p_device_mgmt_info )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_HCI_INFORMATION);
    }
    else
    {
        p_device_mgmt_info = (phHciNfc_DevMgmt_Info_t *)
                                psHciContext->p_device_mgmt_info ;

        switch(index)
        {
            case DEVICE_INFO_EEPROM_INDEX:
            {
                p_device_mgmt_info->eeprom_crc = reg_value[i];
                break;
            }
            default:
            {
                break;
            }
        }

    } /* End of Context and the PN544 Device information validity check */

    return status;
}



/*!
 * \brief Sends the RF Settings HCI Additonal Commands to the connected 
 * reader device.
 *
 * This function Sends the RF Settings HCI Command frames in the HCP packet 
 * format to the connected reader device.
 */
static
 NFCSTATUS
 phHciNfc_Send_DevMgmt_Command (
                                phHciNfc_sContext_t *psHciContext,
                                void                *pHwRef,
                                uint8_t             pipe_id,
                                uint8_t             cmd
                    )
 {
    phHciNfc_HCP_Packet_t   *hcp_packet = NULL;
    phHciNfc_HCP_Message_t  *hcp_message = NULL;
    phHciNfc_Pipe_Info_t    *p_pipe_info = NULL;
    uint8_t                 length=0;
    uint8_t                 i = 0;
    NFCSTATUS               status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext)
        || ( pipe_id > PHHCINFC_MAX_PIPE)
        ||(NULL == psHciContext->p_pipe_list[pipe_id])
      )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
        HCI_DEBUG("%s: Invalid Arguments passed \n",
                                                "phHciNfc_Send_DevMgmt_Command");
    }
    else
    {
        p_pipe_info = (phHciNfc_Pipe_Info_t *) 
                                psHciContext->p_pipe_list[pipe_id];
        psHciContext->tx_total = 0 ;
        length +=  HCP_HEADER_LEN ;
        switch( cmd )
        {
            /* Self Test Commands */
            case NXP_SELF_TEST_ANTENNA:
            case NXP_SELF_TEST_SWP:
            case NXP_SELF_TEST_NFCWI:
            case NXP_SELF_TEST_PRBS:
            /* Internal Properietary Commands */
            case NXP_DBG_READ:
            case NXP_DBG_WRITE:
            {
                
                hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;
                /* Construct the HCP Frame */
                phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                                        (uint8_t) pipe_id, HCP_MSG_TYPE_COMMAND, cmd);
                hcp_message = &(hcp_packet->msg.message);
                /* Append the RF Setting Parameter also the optional Value */
                phHciNfc_Append_HCPFrame((uint8_t *)hcp_message->payload,
                                            i, p_pipe_info->param_info,
                                            p_pipe_info->param_length);
                length =(uint8_t)(length + i + p_pipe_info->param_length);
                break;
            }
            default:
            {
                status = PHNFCSTVAL( CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED );
                HCI_DEBUG("%s: Statement Should Not Occur \n",
                                                "phHciNfc_Send_DevMgmt_Command");
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
 * \brief Sends the RF Settings HCI Additonal Events to the connected 
 * reader device.
 *
 * This function Sends the RF Settings HCI Events frames in the HCP packet 
 * format to the connected reader device.
 */

static
 NFCSTATUS
 phHciNfc_Send_DevMgmt_Event (
                                phHciNfc_sContext_t *psHciContext,
                                void                *pHwRef,
                                uint8_t             pipe_id,
                                uint8_t             event
                    )
 {
    phHciNfc_HCP_Packet_t   *hcp_packet = NULL;
    phHciNfc_HCP_Message_t  *hcp_message = NULL;
    phHciNfc_Pipe_Info_t    *p_pipe_info = NULL;
    uint8_t                 length=0;
    uint8_t                 i = 0;
    NFCSTATUS               status = NFCSTATUS_SUCCESS;

    if( (NULL == psHciContext)
        || ( pipe_id > PHHCINFC_MAX_PIPE)
        ||(NULL == psHciContext->p_pipe_list[pipe_id])
      )
    {
        status = PHNFCSTVAL(CID_NFC_HCI, NFCSTATUS_INVALID_PARAMETER);
        HCI_DEBUG("%s: Invalid Arguments passed \n",
                                                "phHciNfc_Send_DevMgmt_Event");
    }
    else
    {
        p_pipe_info = (phHciNfc_Pipe_Info_t *) 
                                psHciContext->p_pipe_list[pipe_id];
        psHciContext->tx_total = 0 ;
        length +=  HCP_HEADER_LEN ;
        if( NXP_EVT_SET_AUTONOMOUS == event )
        {
            
            hcp_packet = (phHciNfc_HCP_Packet_t *) psHciContext->send_buffer;
            /* Construct the HCP Frame */
            phHciNfc_Build_HCPFrame(hcp_packet,HCP_CHAINBIT_DEFAULT,
                                    (uint8_t) pipe_id, HCP_MSG_TYPE_EVENT, event);
            hcp_message = &(hcp_packet->msg.message);
            /* Append the RF Setting Parameter also the optional Value */
            phHciNfc_Append_HCPFrame((uint8_t *)hcp_message->payload,
                                        i, p_pipe_info->param_info,
                                        p_pipe_info->param_length);
            length =(uint8_t)(length + i + p_pipe_info->param_length);
        }
        else
        {
            status = PHNFCSTVAL( CID_NFC_HCI, NFCSTATUS_FEATURE_NOT_SUPPORTED );
            HCI_DEBUG("%s: Statement Should Not Occur \n",
                                            "phHciNfc_Send_DevMgmt_Event");
        }
        if( NFCSTATUS_SUCCESS == status )
        {
            p_pipe_info->sent_msg_type = HCP_MSG_TYPE_EVENT;
            p_pipe_info->prev_msg = event;
            psHciContext->tx_total = length;

            /* Send the Constructed HCP packet to the lower layer */
            status = phHciNfc_Send_HCP( psHciContext, pHwRef );
            p_pipe_info->prev_status = NFCSTATUS_PENDING;
        }
    }

    return status;
}





