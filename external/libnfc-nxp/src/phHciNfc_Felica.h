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
* \file  phHciNfc_Felica.h                                                 *
* \brief HCI Felica Management Routines.                                    *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Fri Jun  5 12:10:53 2009 $                                           *
* $Author: ing02260 $                                                         *
* $Revision: 1.3 $                                                            *
* $Aliases: NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK926_R28_2,NFC_FRI1.1_WK926_R28_3,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                                                *
*                                                                             *
* =========================================================================== *
*/

#ifndef PHHCINFC_FELICA_H
#define PHHCINFC_FELICA_H

/*@}*/


/**
*  \name HCI
*
* File: \ref phHciNfc_Felica.h
*
*/
/*@{*/
#define PHHCINFC_FELICA_FILEREVISION "$Revision: 1.3 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_FELICA_FILEALIASES  "$Aliases: NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK926_R28_2,NFC_FRI1.1_WK926_R28_3,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"   /**< \ingroup grp_file_attributes */
/*@}*/

/*
***************************** Header File Inclusion ****************************
*/

#include <phHciNfc_Generic.h>

/*
****************************** Macro Definitions *******************************
*/

/* Commands exposed to the upper layer */

/* Enable the Felica */
#define HCI_FELICA_ENABLE                   0x01U
#define HCI_FELICA_INFO_SEQ                 0x02U

/* Felica read write commands */
#define NXP_FELICA_RAW                      0x20U
#define NXP_FELICA_CMD                      0x21U

/*
******************** Enumeration and Structure Definition **********************
*/
typedef enum phHciNfc_Felica_Seq{
    FELICA_SYSTEMCODE,
    FELICA_CURRENTIDM,
    FELICA_CURRENTPMM,
    FELICA_END_SEQUENCE,
    FELICA_INVALID_SEQ
} phHciNfc_Felica_Seq_t;

/* Information structure for the Felica Gate */
typedef struct phHciNfc_Felica_Info{
    /* Current running Sequence of the Felica Management */
    phHciNfc_Felica_Seq_t           current_seq;
    /* Next running Sequence of the Felica Management */
    phHciNfc_Felica_Seq_t           next_seq;
    /* Pointer to the Felica pipe information */
    phHciNfc_Pipe_Info_t            *p_pipe_info;
    uint8_t                         pipe_id;
    /* Flag to say about the multiple targets */
    uint8_t                         multiple_tgts_found;
    /* Felica information */
    phHal_sRemoteDevInformation_t   felica_info;
    /* Enable or disable reader gate */
    uint8_t                         enable_felica_gate;
    /* UICC re-activation status */
    uint8_t                         uicc_activation;
} phHciNfc_Felica_Info_t;

/*
*********************** Function Prototype Declaration *************************
*/

/*!
* \brief Allocates the resources of Felica management gate.
*
* This function Allocates the resources of the Felica management
* gate Information Structure.
* 
*/
extern
NFCSTATUS
phHciNfc_Felica_Init_Resources(
                                phHciNfc_sContext_t     *psHciContext
                                );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_Felica_Get_PipeID function gives the pipe id of the Felica 
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
phHciNfc_Felica_Get_PipeID(
                            phHciNfc_sContext_t     *psHciContext,
                            uint8_t                 *ppipe_id
                            );


/**
* \ingroup grp_hci_nfc
*
*  The phHciNfc_Felica_Update_PipeInfo function updates the pipe_id of the Felica
*  gate management Structure.
*
*  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                      context Structure.
*  \param[in]  pipeID                  pipeID of the Felica gate
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
phHciNfc_Felica_Update_PipeInfo(
                                 phHciNfc_sContext_t     *psHciContext,
                                 uint8_t                 pipeID,
                                 phHciNfc_Pipe_Info_t    *pPipeInfo
                                 );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_ReaderA_Update_Info function updated the felica gate info.
*
*   \param[in]  psHciContext     psHciContext is the pointer to HCI Layer
*                                context Structure.
*   \param[in]  infotype         To enable the felica gate
*   \param[in]  fel_info         felica gate info
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*/
extern 
NFCSTATUS
phHciNfc_Felica_Update_Info(
                             phHciNfc_sContext_t        *psHciContext,
                             uint8_t                    infotype,
                             void                       *fel_info
                             );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_Felica_Info_Sequence function executes the sequence of operations, to
*   get the SYSTEM CODE, IDM, PPM.
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
phHciNfc_Felica_Info_Sequence (
                                void             *psHciHandle,
                                void             *pHwRef
                                );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_Felica_Request_Mode function is to know about the felica tag is 
*   in the field or not
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
phHciNfc_Felica_Request_Mode(
                              phHciNfc_sContext_t   *psHciContext,
                              void                  *pHwRef);

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_Send_Felica_Command function executes the command sent by the 
*   upper layer, depending on the commands defined.
*
*   \param[in]  psContext        psContext is the pointer to HCI Layer
*                                context Structure.
*   \param[in]  pHwRef           pHwRef is the Information of
*                                the Device Interface Link
*   \param[in]  pipe_id          pipeID of the Felica gate 
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
phHciNfc_Send_Felica_Command(
                             phHciNfc_sContext_t   *psContext,
                             void                  *pHwRef,
                             uint8_t               pipe_id,
                             uint8_t               cmd
                             );

#endif /* #ifndef PHHCINFC_FELICA_H */


