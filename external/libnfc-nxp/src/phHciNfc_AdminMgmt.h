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
* \file  phHciNfc_AdminMgmt.h                                                 *
* \brief HCI Header for the Admin Gate Management.                            *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Mon Mar 29 17:34:48 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.7 $                                                            *
* $Aliases: NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                                                *
*                                                                             *
* =========================================================================== *
*/


#ifndef PHHCINFC_ADMINMGMT_H
#define PHHCINFC_ADMINMGMT_H

/*@}*/


/**
 *  \name HCI
 *
 * File: \ref phHciNfc_AdminMgmt.h
 *
 */
/*@{*/
#define PHHCINFC_ADMINMGMT_FILEREVISION "$Revision: 1.7 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_ADMINMGMT_FILEALIASES  "$Aliases: NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"   /**< \ingroup grp_file_attributes */
/*@}*/

/*
***************************** Header File Inclusion ****************************
*/

#include <phHciNfc_Generic.h>

/*
****************************** Macro Definitions *******************************
*/

#define EVT_HOT_PLUG    0x03

/*
******************** Enumeration and Structure Definition **********************
*/

/** \defgroup grp_hci_nfc HCI Component
 *
 *
 */


/*
*********************** Function Prototype Declaration *************************
*/

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Admin_Initialise function Initialises the AdminGate and opens the
 *  Admin Gate pipe
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           AdminGate Initialisation is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
phHciNfc_Admin_Initialise(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                         );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Admin_Release function closes the opened pipes between 
 *  the Host Controller Device and the NFC Device.
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  host_type               host_type is the type of the host
 *                                      to be released.
 *
 *  \retval NFCSTATUS_PENDING           Release of the Admingate resources are 
 *                                      pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
phHciNfc_Admin_Release(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef,
                                phHciNfc_HostID_t        host_type
                     );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Send_Admin_Cmd function Sends the Particular AdminGate 
 *  command to the Host Controller Device.
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  cmd                     cmd to be sent to the Admin gate of the
 *                                      Host controller.
 *  \param[in]  length                  Size of the data sent in the parameter.
 *  \param[in,out]  params              params contains the parameters that are
 *                                      required by the particular HCI command.
 *
 *  \retval None
 *
 */

extern
NFCSTATUS
phHciNfc_Send_Admin_Cmd (
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef,
                            uint8_t                 cmd,
                            uint8_t                 length,
                            void                    *params
                        );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Send_Admin_Cmd function Sends the Particular AdminGate 
 *  command to the Host Controller Device.
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  Event                   Event to be sent to the Admin gate of the
 *                                      Host controller.
 *  \param[in]  length                  Size of the data sent in the parameter.
 *  \param[in,out]  params              params contains the parameters that are
 *                                      required by the particular HCI command.
 *
 *  \retval None
 *
 */

extern
NFCSTATUS
phHciNfc_Send_Admin_Event (
                    phHciNfc_sContext_t   *psHciContext,
                    void                  *pHwRef,
                    uint8_t               event,
                    uint8_t               length,
                    void                  *params
                    );

extern
NFCSTATUS
phHciNfc_Admin_CE_Init(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef,
                            phHciNfc_GateID_t       ce_gate
                        );


#endif

