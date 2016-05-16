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
* \file  phHciNfc_IDMgmt.h                                                    *
* \brief HCI Header for the Identity Management Gate.                         *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Fri Aug 14 17:01:26 2009 $                                           *
* $Author: ing04880 $                                                        *
* $Revision: 1.5 $                                                            *
* $Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                                                *
*                                                                             *
* =========================================================================== *
*/


#ifndef PHHCINFC_IDMGMT_H
#define PHHCINFC_IDMGMT_H

/*@}*/


/**
 *  \name HCI
 *
 * File: \ref phHciNfc_IDMgmt.h
 *
 */
/*@{*/
#define PHHCINFC_IDMGMT_FILEREVISION "$Revision: 1.5 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_IDMGMT_FILEALIASES  "$Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"  /**< \ingroup grp_file_attributes */
/*@}*/

/*
***************************** Header File Inclusion ****************************
*/

#include <phHciNfc_Generic.h>

/*
****************************** Macro Definitions *******************************
*/

/*
******************** Enumeration and Structure Definition **********************
*/



/*
*********************** Function Prototype Declaration *************************
*/

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_IDMgmt_Initialise function creates and the opens Identity 
 *  Management Gate 
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           Identity Mgmt Gate Initialisation is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */
extern
NFCSTATUS
phHciNfc_IDMgmt_Initialise(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                         );
/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_IDMgmt_Info_Sequence function obtains the information
 *  from the Identity Management Gate 
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           Identity Mgmt Gate Information is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */
extern
NFCSTATUS
phHciNfc_IDMgmt_Info_Sequence(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                         );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_IDMgmt_Release function closes the opened pipes between 
 *  the Host Controller Device and the NFC Device.
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           Release of the Identity Management gate 
 *                                      resources are pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */
extern
NFCSTATUS
phHciNfc_IDMgmt_Release(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                     );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_IDMgmt_Update_PipeInfo function updates the pipe_id of the Idetity
 *  Gate Managment Struction.
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pipeID                  pipeID of the Identity management Gate
 *  \param[in]  pPipeInfo               Update the pipe Information of the Identity
 *                                      Management Gate.
 *
 *  \retval NFCSTATUS_SUCCESS           AdminGate Response received Successfully.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *
 */

extern
NFCSTATUS
phHciNfc_IDMgmt_Update_PipeInfo(
                                phHciNfc_sContext_t     *psHciContext,
                                uint8_t                 pipeID,
                                phHciNfc_Pipe_Info_t    *pPipeInfo
                                );


/*!
 * \brief Updates the Sequence of Identity Managment Gate.
 *
 * This function Updates the Sequence of the Identity Management
 * gate Information Structure.
 * 
 */
extern
NFCSTATUS
phHciNfc_IDMgmt_Update_Sequence(
                                phHciNfc_sContext_t     *psHciContext,
                                phHciNfc_eSeqType_t     reader_seq
                             );

/*!
 * \brief Allocates the resources of Identity Managment Gate.
 *
 * This function Allocates the resources of the Identity Management
 * gate Information Structure.
 * 
 */
extern
NFCSTATUS
phHciNfc_IDMgmt_Init_Resources(
                                phHciNfc_sContext_t     *psHciContext
                             );


/*!
 * \brief Get the pipe_id of Identity Managment Gate.
 *
 * This function Get the pipe_id of Identity Managment Gate.
 * 
 */

extern
NFCSTATUS
phHciNfc_IDMgmt_Get_PipeID(
                                phHciNfc_sContext_t     *psHciContext,
                                uint8_t                 *ppipe_id
                           );



#endif

