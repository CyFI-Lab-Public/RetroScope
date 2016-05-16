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
* \file  phHciNfc_RFReaderA.h                                                 *
* \brief HCI Reader A Management Routines.                                    *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Fri Aug 14 17:01:27 2009 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.17 $                                                           *
* $Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                          *
*                                                                             *
* =========================================================================== *
*/


#ifndef PHHCINFC_RFREADERA_H
#define PHHCINFC_RFREADERA_H

/*@}*/


/**
 *  \name HCI
 *
 * File: \ref phHciNfc_ReaderA.h
 *
 */
/*@{*/
#define PHHCINFC_RFREADERA_FILEREVISION "$Revision: 1.17 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_RFREADERA_FILEALIASES  "$Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"   /**< \ingroup grp_file_attributes */
/*@}*/

/*
***************************** Header File Inclusion ****************************
*/

#include <phHciNfc_Generic.h>

/*
****************************** Macro Definitions *******************************
*/

/* Commands exposed to the upper layer */
#define NXP_WRA_CONTINUE_ACTIVATION         0x12U
#define NXP_MIFARE_RAW                      0x20U
#define NXP_MIFARE_CMD                      0x21U
#define DATA_RATE_MAX_DEFAULT_VALUE         0x00U

/* Enable the reader A */
#define HCI_READER_A_ENABLE                 0x01U
#define HCI_READER_A_INFO_SEQ               0x02U

#define RDR_A_TIMEOUT_MIN                   0x00U
#define RDR_A_TIMEOUT_MAX                   0x15U
/*
******************** Enumeration and Structure Definition **********************
*/
typedef enum phHciNfc_ReaderA_Seq{
    RDR_A_DATA_RATE_MAX,
    RDR_A_UID,
    RDR_A_SAK,
    RDR_A_ATQA,
    RDR_A_APP_DATA,
    RDR_A_FWI_SFGT,
    RDR_A_END_SEQUENCE, 
    RDR_A_INVALID_SEQ
} phHciNfc_ReaderA_Seq_t;

/* Information structure for the polling loop Gate */
typedef struct phHciNfc_ReaderA_Info{
    /* Current running Sequence of the reader A Management */
    phHciNfc_ReaderA_Seq_t          current_seq;
    /* Next running Sequence of the reader A Management */
    phHciNfc_ReaderA_Seq_t          next_seq;
    /* Pointer to the reader A pipe information */
    phHciNfc_Pipe_Info_t            *p_pipe_info;
    uint8_t                         pipe_id;
    /* Flag to say about the multiple targets */
    uint8_t                         multiple_tgts_found;
    /* Reader A information */
    phHal_sRemoteDevInformation_t   reader_a_info;
    /* Enable or disable reader gate */
    uint8_t                         enable_rdr_a_gate;
    /* UICC re-activation status */
    uint8_t                         uicc_activation;
} phHciNfc_ReaderA_Info_t;

/*
*********************** Function Prototype Declaration *************************
*/

/*!
 * \brief Allocates the resources of reader A management gate.
 *
 * This function Allocates the resources of the reader A management
 * gate Information Structure.
 * 
 */
extern
NFCSTATUS
phHciNfc_ReaderA_Init_Resources(
                                phHciNfc_sContext_t     *psHciContext
                         );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_ReaderA_Get_PipeID function gives the pipe id of the reader A 
*   gate
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_ReaderA_Get_PipeID(
                            phHciNfc_sContext_t     *psHciContext,
                            uint8_t                 *ppipe_id
                            );
                 
/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_ReaderA_Sequence function executes the sequence of operations, to
*   get the UID, SAK, ATQA  etc.
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_ReaderA_Info_Sequence (
                       void             *psHciHandle,
                       void             *pHwRef
                       );


/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_ReaderA_App_Data function is to get the application data information.
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_ReaderA_App_Data (
                           void             *psHciHandle,
                           void             *pHwRef
                           );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_ReaderA_Fwi_Sfgt function is to get the frame waiting time
*   information.
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_ReaderA_Fwi_Sfgt (
                           void             *psHciHandle,
                           void             *pHwRef
                           );

/**
* \ingroup grp_hci_nfc
*
*  The phHciNfc_ReaderA_Update_PipeInfo function updates the pipe_id of the reader A
*  gate management Structure.
*
*  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                      context Structure.
*  \param[in]  pipeID                  pipeID of the reader A gate
*  \param[in]  pPipeInfo               Update the pipe Information of the reader 
*                                      A gate
*
*  \retval NFCSTATUS_SUCCESS           Function execution is successful.
*  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                      could not be interpreted properly.
*
*/

extern
NFCSTATUS
phHciNfc_ReaderA_Update_PipeInfo(
                                  phHciNfc_sContext_t     *psHciContext,
                                  uint8_t                 pipeID,
                                  phHciNfc_Pipe_Info_t    *pPipeInfo
                                  );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_Send_ReaderA_Command function executes the command sent by the 
*   upper layer, depending on the commands defined.
*
*   \param[in]  psContext        psContext is the pointer to HCI Layer
*                                context Structure.
*   \param[in]  pHwRef           pHwRef is the Information of
*                                the Device Interface Link
*   \param[in]  pipe_id          pipeID of the reader A gate 
*   \param[in]  cmd              command that needs to be sent to the device
*   \param[in]  length           information length sent by the caller
*   \param[in]  params           information related to the command
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_Send_ReaderA_Command(
                              phHciNfc_sContext_t   *psContext,
                              void                  *pHwRef,
                              uint8_t               pipe_id,
                              uint8_t               cmd
                              );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_ReaderA_Auto_Activate function updates auto activate register
*
*   \param[in]  psContext        psContext is the pointer to HCI Layer
*                                context Structure.
*   \param[in]  pHwRef           pHwRef is the Information of
*                                the Device Interface Link
*   \param[in] activate_enable   to enable or disable auto activation
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_ReaderA_Auto_Activate(
                               void         *psContext,
                               void         *pHwRef,
                               uint8_t      activate_enable
                               );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_Send_ReaderA_Command function executes the command sent by the 
*   upper layer, depending on the commands defined.
*
*   \param[in]  psHciContext     psHciContext is the pointer to HCI Layer
*                                context Structure.
*   \param[in]  infotype         To enable the reader A gate
*   \param[in]  rdr_a_info       reader A gate info
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_ReaderA_Update_Info(
                             phHciNfc_sContext_t        *psHciContext,
                             uint8_t                    infotype,
                             void                       *rdr_a_info
                             );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_ReaderA_Cont_Active function executes NXP_WRA_CONTINUE_ACTIVATION
*   command to inform the CLF Controller after having received the event
*   EVT_TARGET_DISCOVERED to continue activation in case activation has 
*   been stopped after successful SAK response. The response to this command, sent 
*   as soon as the activation is finished, indicates the result of the 
*   activation procedure
*
*   \param[in]  psHciContext     psHciContext is the pointer to HCI Layer
*                                context Structure.
*   \param[in]  pHwRef           pHwRef is the Information of
*                                the Device Interface Link
*   \param[in]  pipeID           pipeID of the reader A gate
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_ReaderA_Cont_Activate (
                              phHciNfc_sContext_t       *psHciContext,
                              void                      *pHwRef
                              );

/**
* \ingroup grp_hci_nfc
*
*  The phHciNfc_ReaderA_Set_DataRateMax function updates the data rate max value 
*
*  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                      context Structure.
*  \param[in]  pipeID                  pipeID of the reader A gate
*  \param[in]  pPipeInfo               Update the pipe Information of the reader 
*                                      A gate
*
*  \retval NFCSTATUS_SUCCESS           Function execution is successful.
*  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                      could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_ReaderA_Set_DataRateMax(
                                 void         *psContext,
                                 void         *pHwRef,
                                 uint8_t      data_rate_value
                                 );

#endif /* #ifndef PHHCINFC_RFREADERA_H */

