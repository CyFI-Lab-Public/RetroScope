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
* \file  phHciNfc_RFReader.h                                                  *
* \brief HCI Header for the RF Reader Management Gate.                        *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Fri Aug 14 17:01:28 2009 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.17 $                                                            *
* $Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                                                *
*                                                                             *
* =========================================================================== *
*/


#ifndef PHHCINFC_RFREADER_H
#define PHHCINFC_RFREADER_H

/*@}*/


/**
 *  \name HCI
 *
 * File: \ref phHciNfc_RFReader.h
 *
 */
/*@{*/
#define PHHCINFC_RF_READER_FILEREVISION "$Revision: 1.17 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_RF_READER_FILEALIASES  "$Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"   /**< \ingroup grp_file_attributes */
/*@}*/

/*
***************************** Header File Inclusion ****************************
*/

#include <phHciNfc_Generic.h>

/*
****************************** Macro Definitions *******************************
*/
/* Events Requested by the Reader Application Gates */
#define EVT_READER_REQUESTED            0x10U
#define EVT_END_OPERATION               0x11U

/* Events Triggered by the Reader RF Gates */
#define EVT_TARGET_DISCOVERED           0x10U

/* Commands from ETSI HCI Specification */
#define WR_XCHGDATA                     0x10U

/* NXP Additional Commands apart from ETSI HCI Specification */
/* Command to Check the presence of the card */
#define NXP_WR_PRESCHECK                0x30U

/* Command to Activate the next card present in the field */
#define NXP_WR_ACTIVATE_NEXT            0x31U

/* Command to Activate a card with its UID */
#define NXP_WR_ACTIVATE_ID              0x32U

/* Command to Dispatch the card to UICC */
#define NXP_WR_DISPATCH_TO_UICC         0x33U

/* NXP Additional Events apart from ETSI HCI Specification */
/* Event to Release the Target and Restart The Wheel */
#define NXP_EVT_RELEASE_TARGET          0x35U


/* Type Macro to Update the  RF Reader Information */

#define HCI_RDR_ENABLE_TYPE             0x01U

#define UICC_CARD_ACTIVATION_SUCCESS    0x00U
#define UICC_CARD_ACTIVATION_ERROR      0x01U
#define UICC_RDR_NOT_INTERESTED         0x02U


/*
******************** Enumeration and Structure Definition **********************
*/



/*
*********************** Function Prototype Declaration *************************
*/

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_ReaderMgmt_Initialise function creates and the opens RF Reader 
 *  Management Gate 
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           Reader RF Mgmt Gate Initialisation is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */
extern
NFCSTATUS
phHciNfc_ReaderMgmt_Initialise(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                         );
/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_ReaderMgmt_Release function closes the opened RF Reader pipes 
 *  between the Host Controller Device and the NFC Device.
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           Release of the Reader RF Management gate 
 *                                      resources are pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */
extern
NFCSTATUS
phHciNfc_ReaderMgmt_Release(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                     );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_ReaderMgmt_Update_Sequence function Resets/Updates the sequence 
 *  to the Specified RF Reader Sequence .
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  reader_seq              reader_seq is the Type of sequence update
 *                                      required to reset .
 *
 *  \retval NFCSTATUS_SUCCESS           Updates/Resets the Sequence of the Reader
 *                                       RF Management gate Successsfully.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval 
 *   NFCSTATUS_INVALID_HCI_INFORMATION  The RF Reader Management information is
 *                                      invalid.
 *
 */

extern
NFCSTATUS
phHciNfc_ReaderMgmt_Update_Sequence(
                                phHciNfc_sContext_t     *psHciContext,
                                phHciNfc_eSeqType_t     reader_seq
                             );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_ReaderMgmt_Enable_Discovery function Enables the RF Reader 
 *  Gates to discover the corresponding PICC Tags .
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           Enable of the Reader RF Management gate 
 *                                      Discovery is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
phHciNfc_ReaderMgmt_Enable_Discovery(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                             );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_ReaderMgmt_Discovery function Enables/Disables/Restart/Continue
 *  the RF Reader Gates to discover the corresponding PICC Tags .
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           Enable of the Reader RF Management gate 
 *                                      Discovery is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_ReaderMgmt_Disable_Discovery function Disables the RF Reader 
 *  Gates discovery .
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           Disable of the Reader RF Management gate 
 *                                      Discovery is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
phHciNfc_ReaderMgmt_Disable_Discovery(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                             );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_ReaderMgmt_Info_Sequence function Gets the information 
 *  of the Tag discovered .
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           Reception the information of the discoverd 
 *                                      tag is ongoing.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
phHciNfc_ReaderMgmt_Info_Sequence(
                                   phHciNfc_sContext_t      *psHciContext,
                                   void                     *pHwRef
                               );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_ReaderMgmt_Select function connects the 
 *  the selected tag by performing certain operation.
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  target_type             target_type is the type of the
 *                                      Target Device to be connected .
 *
 *  \retval NFCSTATUS_PENDING           The selected tag initialisation for
 *                                      transaction ongoing.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
phHciNfc_ReaderMgmt_Select(
                                    phHciNfc_sContext_t     *psHciContext,
                                    void                    *pHwRef,
                                    phHal_eRemDevType_t     target_type
                );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_ReaderMgmt_Reactivate function reactivates the 
 *  the tag by performing reactivate operation.
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  target_type             target_type is the type of the
 *                                      Target Device to be reactivated .
 *
 *  \retval NFCSTATUS_PENDING           The tag reactivation ongoing.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */


extern
NFCSTATUS
phHciNfc_ReaderMgmt_Reactivate(
                                    phHciNfc_sContext_t     *psHciContext,
                                    void                    *pHwRef,
                                    phHal_eRemDevType_t     target_type
                );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_ReaderMgmt_Presence_Check function performs presence on ISO 
*   cards.
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link .
*
*   \retval NFCSTATUS_PENDING           The presence check for tag is ongoing.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*  \retval Other errors                Errors related to the other layers
*
*/

extern
NFCSTATUS
phHciNfc_ReaderMgmt_Presence_Check(
                                  phHciNfc_sContext_t       *psHciContext,
                                  void                      *pHwRef
                                  );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_ReaderMgmt_Activate_Next function activates and selects next 
*   tag or target present in the RF Field .
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link .
*
*   \retval NFCSTATUS_PENDING           The activation of the next tag is ongoing.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*  \retval Other errors                Errors related to the other layers
*
*/

extern
NFCSTATUS
phHciNfc_ReaderMgmt_Activate_Next(
                                  phHciNfc_sContext_t       *psHciContext,
                                  void                  *pHwRef
                                  );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_ReaderMgmt_UICC_Dispatch function de-activates the 
 *  the selected tag by de-selecting the tag and dispatch the Card to UICC.
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  target_type             target_type is the type of the
 *                                      Target Device to be de-selected .
 *  \param[in]  re_poll                 If True: Start re-polling of the target
 *                                      after the Target Device is de-activated
 *                                      or else - continue discovery with next
 *                                      technology.
 *
 *
 *  \retval NFCSTATUS_PENDING           Dispatching the selected tag to UICC
 *                                      is ongoing.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
phHciNfc_ReaderMgmt_UICC_Dispatch(
                                    phHciNfc_sContext_t     *psHciContext,
                                    void                    *pHwRef,
                                    phHal_eRemDevType_t     target_type
                );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_ReaderMgmt_Deselect function de-activates the 
 *  the selected tag by de-selecting the tag and restarting the discovery.
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  target_type             target_type is the type of the
 *                                      Target Device to be de-selected .
 *
 *  \retval NFCSTATUS_PENDING           Terminating the operations between selected
 *                                      tag is ongoing.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
phHciNfc_ReaderMgmt_Deselect(
                                    phHciNfc_sContext_t     *psHciContext,
                                    void                    *pHwRef,
                                    phHal_eRemDevType_t     target_type,
                                    uint8_t                 re_poll
                );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_ReaderMgmt_Exchange_Data function exchanges the 
 *  data to/from the selected tag .
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  p_xchg_info             The tag exchange info contains the command type,
 *                                      addr and data to be sent to the connected
 *                                      remote target device.
 *
 *  \retval NFCSTATUS_PENDING           Exchange of the data between the selected 
 *                                      tag is ongoing.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
phHciNfc_ReaderMgmt_Exchange_Data(
                                    phHciNfc_sContext_t     *psHciContext,
                                    void                    *pHwRef,
                                    phHciNfc_XchgInfo_t     *p_xchg_info
                );



/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Send_RFReader_Command function sends the HCI Reader Gate
 *  Specific Commands to the HCI Controller device.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  pipe_id                 The Reader pipe to which the 
 *                                      command is being sent.
 *  \param[in]  cmd                     The HCI Reader Gate specific command
 *                                      sent to a Reader pipe .
 *                                      
 *
 *  \retval NFCSTATUS_PENDING           ETSI HCI RF Reader gate Command 
 *                                      to be sent is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
 phHciNfc_Send_RFReader_Command (
                                phHciNfc_sContext_t *psHciContext,
                                void                *pHwRef,
                                uint8_t             pipe_id,
                                uint8_t             cmd
                    );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Send_RFReader_Event function sends the HCI Reader Gate
 *  Specific Events to the HCI Controller device.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  pipe_id                 The Reader pipe to which the 
 *                                      command is being sent.
 *  \param[in]  event                   The HCI Reader Gate specific event
 *                                      sent to a Reader pipe .
 *                                      
 *
 *  \retval NFCSTATUS_PENDING           ETSI HCI RF Reader gate Event
 *                                      to be sent is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
 phHciNfc_Send_RFReader_Event (
                                phHciNfc_sContext_t *psHciContext,
                                void                *pHwRef,
                                uint8_t             pipe_id,
                                uint8_t             event
                    );

#endif /* PHHCINFC_RFREADER_H */

