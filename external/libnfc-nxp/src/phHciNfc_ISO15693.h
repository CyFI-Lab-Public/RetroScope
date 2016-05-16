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
* \file  phHciNfc_NfcIPMgmt.h                                                 *
* \brief HCI NFCIP-1 Management Routines.                                    *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Tue Jun 30 17:09:29 2009 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.1 $                                                            *
* $Aliases: NFC_FRI1.1_WK926_R28_2,NFC_FRI1.1_WK926_R28_3,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                                                *
*                                                                             *
* =========================================================================== *
*/

#ifndef PHHCINFC_ISO15693_H
#define PHHCINFC_ISO15693_H

/*@}*/


/**
*  \name HCI
*
* File: \ref phHciNfc_ISO15693.h
*
*/
/*@{*/
#define PHHCINFC_ISO15693_FILEREVISION "$Revision: 1.1 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_ISO15693_FILEALIASES  "$Aliases: NFC_FRI1.1_WK926_R28_2,NFC_FRI1.1_WK926_R28_3,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"   /**< \ingroup grp_file_attributes */
/*@}*/

/*
***************************** Header File Inclusion ****************************
*/

#include <phHciNfc_Generic.h>

/*
****************************** Macro Definitions *******************************
*/
/* Enable the ISO 15693 */
#define HCI_ISO_15693_ENABLE                    0x01U
#define HCI_ISO_15693_INFO_SEQ                  0x02U

#define NXP_ISO15693_CMD                        0x20U


/*
******************** Enumeration and Structure Definition **********************
*/

typedef enum phHciNfc_ISO15693_Seq{
    ISO15693_INVENTORY,
    ISO15693_AFI, 
    ISO15693_END_SEQUENCE,
    ISO15693_INVALID_SEQ
} phHciNfc_ISO15693_Seq_t;

typedef struct phHciNfc_ISO15693_Info{
    phHciNfc_ISO15693_Seq_t         current_seq;
    phHciNfc_ISO15693_Seq_t         next_seq;
    phHciNfc_Pipe_Info_t            *ps_15693_pipe_info;
    uint8_t                         pipe_id;    
    uint8_t                         multiple_tgts_found;
    phHal_sRemoteDevInformation_t   iso15693_info;
    uint8_t                         enable_iso_15693_gate;
}phHciNfc_ISO15693_Info_t;


/*
*********************** Function Prototype Declaration *************************
*/

/*!
* \brief Allocates the resources of ISO15693 management gate.
*
* This function Allocates the resources of the ISO15693 management
* gate Information Structure.
* 
*/
extern
NFCSTATUS
phHciNfc_ISO15693_Init_Resources(
                                  phHciNfc_sContext_t     *psHciContext
                                  );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_ISO15693_Get_PipeID function gives the pipe id of the ISO15693 
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
phHciNfc_ISO15693_Get_PipeID(
                              phHciNfc_sContext_t     *psHciContext,
                              uint8_t                 *ppipe_id
                              );

/**
* \ingroup grp_hci_nfc
*
*  The phHciNfc_ISO15693_Update_PipeInfo function updates the pipe_id of the ISO15693
*  gate management Structure.
*
*  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                      context Structure.
*  \param[in]  pipeID                  pipeID of the ISO15693 gate
*  \param[in]  pPipeInfo               Update the pipe Information of the ISO15693 
*                                      gate
*
*  \retval NFCSTATUS_SUCCESS           Function execution is successful.
*  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                      could not be interpreted properly.
*
*/

extern
NFCSTATUS
phHciNfc_ISO15693_Update_PipeInfo(
                                   phHciNfc_sContext_t     *psHciContext,
                                   uint8_t                 pipeID,
                                   phHciNfc_Pipe_Info_t    *pPipeInfo
                                   );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_ISO15693_Update_Info function stores the data sent by the 
*   upper layer.
*
*   \param[in]  psHciContext     psHciContext is the pointer to HCI Layer
*                                context Structure.
*   \param[in]  infotype         To enable the ISO 15693 gate
*   \param[in]  iso_15693_info   ISO 15693 gate info
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_ISO15693_Update_Info(
                             phHciNfc_sContext_t        *psHciContext,
                             uint8_t                    infotype,
                             void                       *iso_15693_info
                             );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_ISO15693_Sequence function executes the sequence of operations, to
*   get the NXP_ISO15693_INVENTORY, NXP_ISO15693_AFI.
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
phHciNfc_ISO15693_Info_Sequence (
                       void             *psHciHandle,
                       void             *pHwRef
                       );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_Send_ISO15693_Command function executes the command sent by the 
*   upper layer, depending on the commands defined.
*
*   \param[in]  psContext        psContext is the pointer to HCI Layer
*                                context Structure.
*   \param[in]  pHwRef           pHwRef is the Information of
*                                the Device Interface Link
*   \param[in]  pipe_id          pipeID of the ISO 15693 gate 
*   \param[in]  cmd              command that needs to be sent to the device
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern 
NFCSTATUS
phHciNfc_Send_ISO15693_Command(
                              phHciNfc_sContext_t   *psHciContext,
                              void                  *pHwRef,
                              uint8_t               pipe_id,
                              uint8_t               cmd
                              );

/**
* \ingroup grp_hci_nfc
*
*  The phHciNfc_ISO15693_Set_AFI function updates the AFI value 
*
*  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                      context Structure.
*  \param[in]  pipeID                  pipeID of the ISO 15693 gate
*  \param[in]  pPipeInfo               Update the pipe Information of the ISO  
*                                      15693 gate
*
*  \retval NFCSTATUS_SUCCESS           Function execution is successful.
*  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                      could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_ISO15693_Set_AFI(
                               void         *psContext,
                               void         *pHwRef,
                               uint8_t      afi_value
                               );

#endif /* #ifndef PHHCINFC_ISO15693_H */


