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
* \file  phHciNfc_DevMgmt.h                                                   *
* \brief HCI Header for the PN544 Device Management Gate.                     *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Tue Jun  8 09:30:49 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.15 $                                                            *
* $Aliases: NFC_FRI1.1_WK1023_R35_1 $  
*                                                                             *
* =========================================================================== *
*/


#ifndef PHHCINFC_DEVMGMT_H
#define PHHCINFC_DEVMGMT_H

/*@}*/


/**
 *  \name HCI
 *
 * File: \ref phHciNfc_DevMgmt.h
 *
 */
/*@{*/
#define PHHCINFC_DEVICE_MGMT_FILEREVISION "$Revision: 1.15 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_DEVICE_MGMT_FILEALIASES  "$Aliases: NFC_FRI1.1_WK1023_R35_1 $"     /**< \ingroup grp_file_attributes */
/*@}*/

/*
***************************** Header File Inclusion ****************************
*/

#include <phHciNfc_Generic.h>

/*
****************************** Macro Definitions *******************************
*/

#define DEVICE_PWR_STATUS_INDEX         0x01U
#define DEVICE_INFO_EVT_INDEX           0x02U
#define DEVICE_INFO_EEPROM_INDEX        0x03U

#define  NXP_DOWNLOAD_GPIO              0x04U

/* GPIO PIN Mask Macro */
#define NXP_NFC_GPIO_MASK(n)  ((uint8_t)(1U << (n)))

/* Address Definitions for GPIO Register Configuration */
#define NFC_ADDRESS_GPIO_PDIR           0xF821U
#define NFC_ADDRESS_GPIO_PEN            0xF829U


/* Address Definitions for SWP Configuration */
#define NFC_ADDRESS_SWP_BITRATE         0x9C01U
#define NFC_ADDRESS_SWP_PWR_REQ         0x9EB4U

/* Address Definitions for UICC Host Configuration */
#define NFC_ADDRESS_UICC_RD_A_ACCESS    0x9ED9U
#define NFC_ADDRESS_UICC_RD_B_ACCESS    0x9EDAU
#define NFC_ADDRESS_UICC_CE_A_ACCESS    0x9EDBU
#define NFC_ADDRESS_UICC_CE_B_ACCESS    0x9EDCU
#define NFC_ADDRESS_UICC_CE_BP_ACCESS   0x9EDDU
#define NFC_ADDRESS_UICC_CE_F_ACCESS    0x9EDEU

/* Address Definitions for SE Configuration */

/* Address Definitions for HW Configuration */
#define NFC_ADDRESS_CLK_REQ             0x9E71U
#define NFC_ADDRESS_CLK_INPUT           0x9809U
#define NFC_ADDRESS_HW_CONF             0x9810U
#define NFC_ADDRESS_PWR_STATUS          0x9EAAU

/* Address Definitions for RF Configuration */


/* Address Definitions for Interframe Character Timeout Configuration */
#define NFC_ADDRESS_IFC_TO_RX_H          0x9C0CU
#define NFC_ADDRESS_IFC_TO_RX_L          0x9C0DU
#define NFC_ADDRESS_IFC_TO_TX_H          0x9C12U
#define NFC_ADDRESS_IFC_TO_TX_L          0x9C13U


/* Address Definitions for LLC Configuration */
#define NFC_ADDRESS_LLC_ACK_TO_H          0x9C27U
#define NFC_ADDRESS_LLC_ACK_TO_L          0x9C28U
#define NFC_ADDRESS_LLC_GRD_TO_H          0x9C31U
#define NFC_ADDRESS_LLC_GRD_TO_L          0x9C32U

#define NFC_ADDRESS_ACT_GRD_TO          0x9916U

/* The Address Definition for the TYPE B Tuning */

#ifdef SW_TYPE_RF_TUNING_BF
#define NFC_ADDRESS_ANAIRQ_CONF         0x9801U
#define NFC_ADDRESS_PMOS_MOD            0x997AU
#endif

#define NFC_FELICA_RC_ADDR              0x9F9AU

/* The Address Definition for the Enabling the EVT_HOT_PLUG */
#define NFC_ADDRESS_HOTPLUG_EVT         0x9FF0U


/*
******************** Enumeration and Structure Definition **********************
*/



/*
*********************** Function Prototype Declaration *************************
*/

/************************ Function Prototype Declaration *************************/

/*!
 * \brief Allocates the resources required for  PN544 Device management gate.
 *
 * This function Allocates necessary resources as requiered by PN544 Device
 * gate management
 *
 * \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *
 * \retval NFCSTATUS_SUCCESS           Function execution is successful
 *
 * \retval NFCSTATUS_INVALID_PARAMETER One or more of the given inputs are not valid
 */

extern
NFCSTATUS
phHciNfc_DevMgmt_Init_Resources(phHciNfc_sContext_t   *psHciContext);


/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_DevMgmt_Get_PipeID function gives the pipe id of the PN544 Device
*   management gate
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*   \param[in]  ppipe_id                ppipe_id of the Device management Gate
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/

extern
NFCSTATUS
phHciNfc_DevMgmt_Get_PipeID(
                            phHciNfc_sContext_t     *psHciContext,
                            uint8_t                 *ppipe_id
                            );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_DevMgmt_Initialise function creates and the opens the pipe 
 *  PN544 Device Management Gate in the NFC Device
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           Device Mgmt Gate Initialisation is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
phHciNfc_DevMgmt_Initialise(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                         );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_DevMgmt_Test function performs the System Management Tests 
 * provided by the NFC Peripheral device.
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  test_type               test_type is the type of the Self Test
 *                                      that needs to be performed on the device.
 *  \param[in]  test_param              test_param is the parameter for the Self Test
 *                                      that needs to be performed on the device.
 *
 *
 *  \retval NFCSTATUS_PENDING           Self Test on the Device Management gate 
 *                                      is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
phHciNfc_DevMgmt_Test(
                    void                            *psContext,
                    void                            *pHwRef,
                    uint8_t                         test_type,
                    phNfc_sData_t                   *test_param
                 );

extern
NFCSTATUS
phHciNfc_DevMgmt_Get_Info (
                            phHciNfc_sContext_t *psHciContext,
                            void                *pHwRef,
                            uint16_t            address,
                            uint8_t             *p_val
                );

extern
NFCSTATUS
phHciNfc_DevMgmt_Configure (
                            phHciNfc_sContext_t *psHciContext,
                            void                *pHwRef,
                            uint16_t            address,
                            uint8_t             value
                );

extern
NFCSTATUS
phHciNfc_DevMgmt_Get_Test_Result(
                            phHciNfc_sContext_t        *psHciContext,
                            phNfc_sData_t              *test_result
                            );


/**
* \ingroup grp_hci_nfc
*
* \brief Allocates the resources required for  PN544 Device 
* management gate 
* This function Allocates necessary resources as requiered by PN544 
* Device management gate 
*
* \param[in]  psHciContext          psHciContext is the pointer to HCI Layer
* \param[in]  pipeID                pipeID of the Device management Gate
* \param[in]  pPipeInfo             Update the pipe Information of the Device
*                                   Management Gate.
*
* \retval NFCSTATUS_SUCCESS           Function execution is successful
*
* \retval NFCSTATUS_INVALID_PARAMETER One or more of the given inputs are not valid
*/
extern
NFCSTATUS
phHciNfc_DevMgmt_Update_PipeInfo(
                                  phHciNfc_sContext_t     *psHciContext,
                                  uint8_t                 pipeID,
                                  phHciNfc_Pipe_Info_t    *pPipeInfo
                                  );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_DevMgmt_Release function closes the opened pipes between 
 *  the Device Management Gate in the Host Controller Device 
 *  and the NFC Device.
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           Release of the Device Management gate 
 *                                      resources are pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */
extern
NFCSTATUS
phHciNfc_DevMgmt_Release(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                     );

extern
NFCSTATUS
phHciNfc_DevMgmt_Update_Sequence(
                                phHciNfc_sContext_t     *psHciContext,
                                phHciNfc_eSeqType_t     DevMgmt_seq
                             );

extern
NFCSTATUS
phHciNfc_DevMgmt_Set_Test_Result(
                                phHciNfc_sContext_t        *psHciContext,
                                uint8_t                    test_status
                            )
;

#endif

