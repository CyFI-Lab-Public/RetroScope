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
* \file  phHciNfc_Sequence.h                                                  *
* \brief State Machine Management for the HCI and the Function Sequence       *
* for a particular State.                                                     *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Fri Aug 14 17:01:28 2009 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.12 $                                                            * 
* $Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $  
*                                                                             *
* =========================================================================== *
*/

/*@{*/
#ifndef PHHCINFC_SEQUENCE_H
#define PHHCINFC_SEQUENCE_H

/*@}*/


/**
 *  \name HCI
 *
 * File: \ref phHciNfc_Sequence.h
 *
 */

/*@{*/
#define PHHCINFC_SEQUENCE_FILEREVISION "$Revision: 1.12 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_SEQUENCE_FILEALIASES  "$Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"    /**< \ingroup grp_file_attributes */
/*@}*/


/*
################################################################################
***************************** Header File Inclusion ****************************
################################################################################
*/

#include <phHciNfc_Generic.h>

/*
################################################################################
****************************** Macro Definitions *******************************
################################################################################
*/

/*
################################################################################
************************* Function Prototype Declaration ***********************
################################################################################
*/


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_FSM_Update function Validates the HCI State to
 *  the next operation ongoing.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  state                   state is the state to which the
 *                                      current HCI Layer state is validated.
 *  \param[in]  validate_type           validate the state by the type of the
 *                                      validation required.
 *
 *  \retval NFCSTATUS_SUCCESS           FSM Validated successfully .
 *  \retval NFCSTATUS_INVALID_STATE     The supplied state parameter is invalid.
 *
 */

extern 
NFCSTATUS 
phHciNfc_FSM_Validate(  
                        phHciNfc_sContext_t *psHciContext,  
                        phHciNfc_eState_t state,  
                        uint8_t validate_type
                    );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_FSM_Update function Checks and Updates the HCI State to
 *  the next valid State.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  next_state              next_state is the state to which
 *                                      we the HCI Layer.
 *  \param[in]  transition              transiton of the state whether 
 *                                      ongoing or complete .
 *
 *  \retval NFCSTATUS_SUCCESS           FSM Updated successfully .
 *  \retval NFCSTATUS_INVALID_STATE     The supplied state parameter is invalid.
 *
 */

extern
NFCSTATUS
phHciNfc_FSM_Update(
                        phHciNfc_sContext_t *psHciContext,
                        phHciNfc_eState_t   next_state
                   );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_FSM_Complete function completes the  ongoing state transition 
 *  from the current state to the next state.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *
 *  \retval NFCSTATUS_SUCCESS           FSM Updated successfully .
 *  \retval NFCSTATUS_INVALID_STATE     The supplied state parameter is invalid.
 *
 */

extern
NFCSTATUS
phHciNfc_FSM_Complete(
                        phHciNfc_sContext_t *psHciContext
                    );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_FSM_Rollback function rolls back to previous valid state 
 *  and abort the ongoing state transition. 
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *
 *  \retval NONE.
 *
 */

extern
void
phHciNfc_FSM_Rollback(
                        phHciNfc_sContext_t *psHciContext
                    );



/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Initialise_Sequence function sequence initialises the 
 *  HCI layer and the remote device by performing the operations required
 *  setup the reader and discovery functionality.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_SUCCESS           HCI current initialise sequence successful.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Other related errors
 *
 */


extern
NFCSTATUS
phHciNfc_Initialise_Sequence(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                             );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_PollLoop_Sequence function sequence starts the  
 *  discovery sequence of device.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_SUCCESS           HCI Discovery Configuration sequence successful.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Other related errors
 *
 */

extern
NFCSTATUS
phHciNfc_PollLoop_Sequence(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef
                         );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_EmulationCfg_Sequence function sequence configures the  
 *  device for different types of emulation supported.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_SUCCESS           HCI Emulation Configuration 
 *                                      sequence successful.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                    Other related errors
 *
 */

extern
NFCSTATUS
phHciNfc_EmulationCfg_Sequence(
                           phHciNfc_sContext_t      *psHciContext,
                           void                 *pHwRef
                           );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_SmartMx_Mode_Sequence function sequence configures the  
 *  SmartMx device for different modes by enabling and disabling polling.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_SUCCESS           HCI SmartMX Mode Configuration 
 *                                      sequence successful.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                 Other related errors
 *
 */

extern
NFCSTATUS
phHciNfc_SmartMx_Mode_Sequence(
                           phHciNfc_sContext_t      *psHciContext,
                           void                     *pHwRef
                          );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Connect_Sequence function sequence selects the  
 *  discovered target for performing the transaction.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_SUCCESS           HCI target selection sequence successful.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Other related errors
 *
 */

extern
NFCSTATUS
phHciNfc_Connect_Sequence(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef
                         );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Disconnect_Sequence function sequence de-selects the  
 *  selected target .
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_SUCCESS           HCI target de-selection sequence successful.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Other related errors
 *
 */

extern
NFCSTATUS
phHciNfc_Disconnect_Sequence(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef
                         );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Error_Sequence function sequence notifies the  
 *  error in the HCI sequence to the upper layer .
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  status                  Notify status information from the 
 *                                      HCI layer to the Upper Layer.
 *                                      
 *
 *  \retval NFCSTATUS_SUCCESS           HCI Error sequence Notification successful.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Other related errors
 *
 */
extern
void
phHciNfc_Error_Sequence(
                                void            *psContext,
                                void            *pHwRef,
                                NFCSTATUS       error_status,
                                void            *pdata,
                                uint8_t         length
                        );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Resume_Sequence function sequence resumes the  
 *  previous pending sequence of HCI .
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_SUCCESS           HCI sequence resume successful.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Other related errors
 *
 */

extern
NFCSTATUS
phHciNfc_Resume_Sequence(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                          );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Release_Sequence function sequence releases the 
 *  HCI layer and the remote device by performing the operations required
 *  release the reader and discovery functionality.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_SUCCESS           HCI current release sequence successful.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Other related errors
 *
 */


extern
NFCSTATUS
phHciNfc_Release_Sequence(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef
                         );


/*
################################################################################
***************************** Function Definitions *****************************
################################################################################
*/

#endif

