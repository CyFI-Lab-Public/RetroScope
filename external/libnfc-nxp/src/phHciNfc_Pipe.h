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
* \file  phHciNfc_Pipe.h                                                      *
* \brief HCI Header for the Pipe Management.                                  *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Fri Aug 14 17:01:27 2009 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.17 $                                                            *
* $Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $      
*                                                                             *
* =========================================================================== *
*/

/*@{*/

#ifndef PHHCINFC_PIPE_H
#define PHHCINFC_PIPE_H

/*@}*/


/**
 *  \name HCI
 *
 * File: \ref phHciNfc_Pipe.h
 *
 */
/*@{*/
#define PHHCINFC_PIPE_FILEREVISION "$Revision: 1.17 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_PIPE_FILEALIASES  "$Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"    /**< \ingroup grp_file_attributes */
/*@}*/

/*
***************************** Header File Inclusion ****************************
*/

#include <phHciNfc.h>
#include <phHciNfc_Generic.h>

/*
****************************** Macro Definitions *******************************
*/

#define PIPEINFO_SIZE               0x04U
#define PIPEID_LEN                  0x01U

#define RESPONSE_GATEID_OFFSET      0x03U
#define RESPONSE_PIPEID_OFFSET      0x04U


#define PIPETYPE_STATIC_LINK        0x00U
#define PIPETYPE_STATIC_ADMIN       0x01U
#define PIPETYPE_DYNAMIC            0x02U

/*
******************** Enumeration and Structure Definition **********************
*/

typedef enum phHciNfc_PipeMgmt_Seq{
    /* Pipe for Identitiy Management   */
    PIPE_IDMGMT_CREATE      = 0x00U,
    /* Pipe for Configuring PN544 Nfc Device  */
    PIPE_PN544MGMT_CREATE,
    /* Pipe for Configuring Polling Wheel  */
    PIPE_POLLINGLOOP_CREATE,
    /* Pipes for Configuring the RF Readers  */
    PIPE_READER_A_CREATE,
    PIPE_READER_B_CREATE,
    PIPE_READER_F_CREATE,
    PIPE_READER_JWL_CREATE,
    PIPE_READER_ISO15693_CREATE,
    /* Pipes for configuring the Card Emulation  */
    PIPE_CARD_A_CREATE,
    PIPE_CARD_A_DELETE,
    PIPE_CARD_B_CREATE,
    PIPE_CARD_B_DELETE,
    PIPE_CARD_F_CREATE,
    PIPE_CARD_F_DELETE,
    /* Pipes for Peer to Peer Communication  */
    PIPE_NFC_INITIATOR_CREATE,
    PIPE_NFC_TARGET_CREATE,
    /* Secure Element Commands */
    PIPE_WI_CREATE,
    PIPE_SWP_CREATE,
    /* Connectiviy Gate Pipe */
    PIPE_CONNECTIVITY,

    /* Clearing all the created Pipes  */
    PIPE_DELETE_ALL,
    PIPE_MGMT_END
} phHciNfc_PipeMgmt_Seq_t;

/** \defgroup grp_hci_nfc HCI Component
 *
 *
 */

/*
*********************** Function Prototype Declaration *************************
*/

/*!
 * \brief Creates the Pipes of all the Supported Gates .
 *
 * This function Creates the pipes for all the supported gates 
 */

extern
NFCSTATUS
phHciNfc_Create_All_Pipes(
                                phHciNfc_sContext_t          *psHciContext,
                                void                         *pHwRef,
                                phHciNfc_PipeMgmt_Seq_t      *p_pipe_seq
                          );

/*!
 * \brief Deletes the Pipes of all the Supported Gates .
 *
 * This function Deletes the pipes for all the supported gates 
 */
extern
NFCSTATUS
phHciNfc_Delete_All_Pipes(
                                phHciNfc_sContext_t             *psHciContext,
                                void                            *pHwRef,
                                phHciNfc_PipeMgmt_Seq_t         pipeSeq
                          );

/*!
 * \brief Updates the Information of Pipes of all the Supported Gates .
 *
 * This function Updates the pipe information for all the supported gates 
 */

extern
NFCSTATUS
phHciNfc_Update_PipeInfo(
                                phHciNfc_sContext_t             *psHciContext,
                                phHciNfc_PipeMgmt_Seq_t         *pPipeSeq,
                                uint8_t                         pipe_id,
                                phHciNfc_Pipe_Info_t            *pPipeInfo
                      );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Open_Pipe function opens
 *  .
 *
 *  \param[in]  psContext               psContext is pointer to the context
 *                                      Structure of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  pPipeHandle             pPipeHandle is the handle used to open
 *                                      the Static or Dynamically Created Pipe.
 *
 *  \retval NFCSTATUS_PENDING           Pipe Open is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the lower layers
 *
 */

 extern
 NFCSTATUS
 phHciNfc_Open_Pipe (
                        phHciNfc_sContext_t     *psContext,
                        void                    *pHwRef,
                        phHciNfc_Pipe_Info_t    *pPipeHandle
                        );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Close_Pipe function closes
 *  .
 *
 *  \param[in]  psContext               psContext is pointer to the context
 *                                      Structure of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  pPipeHandle             pPipeHandle is the handle used to closes
 *                                      the Static or Dynamically Created Pipe.
 *
 *  \retval NFCSTATUS_PENDING           Pipe close is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the lower layers
 *
 */

 extern
 NFCSTATUS
 phHciNfc_Close_Pipe (
                        phHciNfc_sContext_t     *psContext,
                        void                    *pHwRef,
                        phHciNfc_Pipe_Info_t    *pPipeHandle
                        );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Delete_Pipe function deletes the dynamically created pipe
 *  using the supplied pipe handle.
 *
 *  \param[in]  psContext               psContext is pointer to the context
 *                                      Structure of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  pPipeHandle             pPipeHandle is the handle used to delete
 *                                      the Dynamically Created Pipe.
 *
 *  \retval NFCSTATUS_PENDING           Pipe Deletion is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *
 */

extern
NFCSTATUS
phHciNfc_Delete_Pipe(
                        phHciNfc_sContext_t     *psHciContext,
                        void                    *pHwRef,
                        phHciNfc_Pipe_Info_t    *pPipeHandle
                    );

/*!
 * \brief Creates and Update the Pipes during the Session
 *
 * This function Creates and Update the Pipes of all the Supported Gates 
 * for the already initialised session.
 */

extern
NFCSTATUS
phHciNfc_Update_Pipe(
                        phHciNfc_sContext_t     *psHciContext,
                        void                    *pHwRef,
                        phHciNfc_PipeMgmt_Seq_t *p_pipe_seq
                    );


extern
NFCSTATUS
phHciNfc_CE_Pipes_OP(
                            phHciNfc_sContext_t             *psHciContext,
                            void                            *pHwRef,
                            phHciNfc_PipeMgmt_Seq_t         *p_pipe_seq
                     );

#endif

