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
* \file  phHciNfc_Jewel.h                                                 *
* \brief HCI Jewel Management Routines.                                    *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Mon Mar 29 17:34:50 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.3 $                                                            *
* $Aliases: NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                                                *
*                                                                             *
* =========================================================================== *
*/

#ifndef PHHCINFC_JEWEL_H
#define PHHCINFC_JEWEL_H

/*@}*/


/**
*  \name HCI
*
* File: \ref phHciNfc_Jewel.h
*
*/
/*@{*/
#define PHHCINFC_JEWEL_FILEREVISION "$Revision: 1.3 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_JEWEL_FILEALIASES  "$Aliases: NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"   /**< \ingroup grp_file_attributes */
/*@}*/

/*
***************************** Header File Inclusion ****************************
*/

#include <phHciNfc_Generic.h>

/*
****************************** Macro Definitions *******************************
*/

/* Commands exposed to the upper layer */

/* Enable the Jewel */
#define HCI_JEWEL_ENABLE                    0x01U
#define HCI_JEWEL_INFO_SEQ                  0x02U

/* Jewel read write commands */
#define NXP_JEWEL_RAW                       0x23U

/*
******************** Enumeration and Structure Definition **********************
*/
typedef enum phHciNfc_Jewel_Seq{
    JEWEL_READID_SEQUENCE,
    JEWEL_END_SEQUENCE,
    JEWEL_INVALID_SEQ
} phHciNfc_Jewel_Seq_t;

/* Information structure for the Jewel Gate */
typedef struct phHciNfc_Jewel_Info{
    /* Current running Sequence of the Jewel Management */
    phHciNfc_Jewel_Seq_t            current_seq;
    /* Next running Sequence of the Jewel Management */
    phHciNfc_Jewel_Seq_t            next_seq;
    /* Pointer to the Jewel pipe information */
    phHciNfc_Pipe_Info_t            *p_pipe_info;
    uint8_t                         pipe_id;
    /* Flag to say about the multiple targets */
    uint8_t                         multiple_tgts_found;
    /* Jewel information */
    phHal_sRemoteDevInformation_t   s_jewel_info;
    /* Enable or disable reader gate */
    uint8_t                         enable_jewel_gate;
    /* UICC re-activation status */
    uint8_t                         uicc_activation;
} phHciNfc_Jewel_Info_t;

/*
*********************** Function Prototype Declaration *************************
*/

/*!
* \brief Allocates the resources of Jewel management gate.
*
* This function Allocates the resources of the Jewel management
* gate Information Structure.
* 
*/
extern
NFCSTATUS
phHciNfc_Jewel_Init_Resources(
                                phHciNfc_sContext_t     *psHciContext
                                );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_Jewel_Get_PipeID function gives the pipe id of the Jewel 
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
phHciNfc_Jewel_Get_PipeID(
                            phHciNfc_sContext_t     *psHciContext,
                            uint8_t                 *ppipe_id
                            );


/**
* \ingroup grp_hci_nfc
*
*  The phHciNfc_Jewel_Update_PipeInfo function updates the pipe_id of the Jewel
*  gate management Structure.
*
*  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                      context Structure.
*  \param[in]  pipeID                  pipeID of the Jewel gate
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
phHciNfc_Jewel_Update_PipeInfo(
                                 phHciNfc_sContext_t     *psHciContext,
                                 uint8_t                 pipeID,
                                 phHciNfc_Pipe_Info_t    *pPipeInfo
                                 );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_Jewel_Update_Info function updated the jewel gate info.
*
*   \param[in]  psHciContext        psHciContext is the pointer to HCI Layer
*                                   context Structure.
*   \param[in]  infotype            To enable the jewel gate
*   \param[in]  jewel_info          Jewel gate info
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*/
extern 
NFCSTATUS
phHciNfc_Jewel_Update_Info(
                             phHciNfc_sContext_t        *psHciContext,
                             uint8_t                    infotype,
                             void                       *jewel_info
                             );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_Jewel_Info_Sequence function executes the sequence of operations, to
*   get the ID.
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
phHciNfc_Jewel_Info_Sequence (
                                void             *psHciHandle,
                                void             *pHwRef
                                );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_Send_Jewel_Command function executes the command sent by the 
*   upper layer, depending on the commands defined.
*
*   \param[in]  psContext        psContext is the pointer to HCI Layer
*                                context Structure.
*   \param[in]  pHwRef           pHwRef is the Information of
*                                the Device Interface Link
*   \param[in]  pipe_id          pipeID of the jewel gate 
*   \param[in]  cmd              command that needs to be sent to the device
*   \param[in]  length           information length sent by the caller
*   \param[in]  params           information related to the command
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*/
extern
NFCSTATUS
phHciNfc_Send_Jewel_Command(
                             phHciNfc_sContext_t   *psContext,
                             void                  *pHwRef,
                             uint8_t               pipe_id,
                             uint8_t               cmd
                             );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_Jewel_GetRID function executes the command to read the ID
*
*   \param[in]  psHciContext     psHciContext is the pointer to HCI Layer
*                                context Structure.
*   \param[in]  pHwRef           pHwRef is the Information of
*                                the Device Interface Link
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*/
extern 
NFCSTATUS 
phHciNfc_Jewel_GetRID(
                phHciNfc_sContext_t   *psHciContext,
                void                  *pHwRef);

#endif /* #ifndef PHHCINFC_JEWEL_H */


