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
* \file  phHciNfc_PollingLoop.h                                               *
* \brief HCI Header for the Polling loop Management.                              *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Mon Mar 29 17:34:49 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.6 $                                                            *
* $Aliases: NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                                                *
*                                                                             *
* =========================================================================== *
*/


#ifndef PHHCINFC_POLLINGLOOP_H
#define PHHCINFC_POLLINGLOOP_H

/*@}*/


/**
 *  \name HCI
 *
 * File: \ref phHciNfc_PollingLoop.h
 *
 */
/*@{*/
#define PHHCINFC_POLLINGLOOP_FILEREVISION "$Revision: 1.6 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_POLLINGLOOP_FILEALIASES  "$Aliases: NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"     /**< \ingroup grp_file_attributes */
/*@}*/

/*
***************************** Header File Inclusion ****************************
*/

#include <phHciNfc_Generic.h>

/*
****************************** Macro Definitions *******************************
*/
#define PL_DURATION             0x00U
#define PL_RD_PHASES            0x01U
#define PL_DISABLE_TARGET       0x02U



#define PL_RD_PHASES_DISABLE    0x80U

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
 *  The phHciNfc_PollLoop_Initialise function Initialises the polling loop and opens the
 *  polling loop pipe
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           Polling loop gate Initialisation is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
phHciNfc_PollLoop_Initialise(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                         );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_PollLoop_Release function closes the polling loop gate pipe  
 *  between the Host Controller Device and the NFC Device.
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           Release of the Polling loop gate resources are 
 *                                      pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
phHciNfc_PollLoop_Release(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                     );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_PollLoop_Update_PipeInfo function updates the pipe_id of the polling
 *  loop gate Managment Struction.
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link
 *  \param[in]  cfg_type                Poll configuration type
 *
 *  \param[in]  pcfg_info               Poll configuration info.
 *
 *  \retval NFCSTATUS_SUCCESS           Polling loop gate Response received Successfully.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *
 */
extern
NFCSTATUS
phHciNfc_PollLoop_Cfg (
                       void             *psHciHandle,
                       void             *pHwRef, 
                       uint8_t          cfg_type, 
                       void             *pcfg_info
                       );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_PollLoop_Update_PipeInfo function updates the pipe_id of the polling
 *  loop gate management structure. This function is used by the pipe management to 
 *  update the pipe id
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pipeID                  pipeID of the polling loop gate
 *  \param[in]  pPipeInfo               Update the pipe Information of the polling loop
 *                                      gate.
 *
 *  \retval NFCSTATUS_SUCCESS           Polling loop gate Response received Successfully.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *
 */

extern
NFCSTATUS
phHciNfc_PollLoop_Update_PipeInfo(
                                phHciNfc_sContext_t     *psHciContext,
                                uint8_t                 pipeID,
                                phHciNfc_Pipe_Info_t    *pPipeInfo
                                );


/*!
 * \brief Allocates the resources of Polling loop Managment Gate.
 *
 * This function Allocates the resources of the Polling loop management
 * gate Information Structure.
 * 
 */
extern
NFCSTATUS
phHciNfc_PollLoop_Init_Resources(
                                phHciNfc_sContext_t     *psHciContext
                             );


/*!
 * \brief Get the pipe_id of Polling loop managment Gate.
 *
 * This function Get the pipe_id of Polling loop managment Gate.
 * 
 */

extern
NFCSTATUS
phHciNfc_PollLoop_Get_PipeID(
                                phHciNfc_sContext_t     *psHciContext,
                                uint8_t                 *ppipe_id
                           );

#endif /* PHHCINFC_POLLINGLOOP_H */


