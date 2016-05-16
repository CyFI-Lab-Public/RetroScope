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
* \file  phHciNfc_RFReaderB.h                                                 *
* \brief HCI Reader B Management Routines.                                    *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Fri Aug 14 17:01:26 2009 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.5 $                                                            *
* $Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                                                *
*                                                                             *
* =========================================================================== *
*/

#ifndef PHHCINFC_RFREADERB_H
#define PHHCINFC_RFREADERB_H

/*@}*/


/**
*  \name HCI
*
* File: \ref phHciNfc_ReaderB.h
*
*/
/*@{*/
#define PHHCINFC_RFREADERB_FILEREVISION "$Revision: 1.5 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_RFREADERB_FILEALIASES  "$Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"   /**< \ingroup grp_file_attributes */
/*@}*/

/*
***************************** Header File Inclusion ****************************
*/

#include <phHciNfc_Generic.h>

/*
****************************** Macro Definitions *******************************
*/

/* Enable the reader B */
#define HCI_READER_B_ENABLE                 0x01U
#define HCI_READER_B_INFO_SEQ               0x02U

/*
******************** Enumeration and Structure Definition **********************
*/
typedef enum phHciNfc_ReaderB_Seq{
    RDR_B_PUPI,
    RDR_B_APP_DATA,
    RDR_B_AFI,
    RDR_B_HIGHER_LAYER_RESP,
    RDR_B_HIGHER_LAYER_DATA,
    RDR_B_END_SEQUENCE,
    RDR_B_INVALID_SEQ
} phHciNfc_ReaderB_Seq_t;

/* Information structure for the reader B Gate */
typedef struct phHciNfc_ReaderB_Info{
    /* Current running Sequence of the reader B Management */
    phHciNfc_ReaderB_Seq_t          current_seq;
    /* Next running Sequence of the reader B Management */
    phHciNfc_ReaderB_Seq_t          next_seq;
    /* Pointer to the reader B pipe information */
    phHciNfc_Pipe_Info_t            *p_pipe_info;
    uint8_t                         pipe_id;
    /* Flag to say about the multiple targets */
    uint8_t                         multiple_tgts_found;
    /* Reader B information */
    phHal_sRemoteDevInformation_t   reader_b_info;
    /* Enable or disable reader gate */
    uint8_t                         enable_rdr_b_gate;
    /* UICC re-activation status */
    uint8_t                         uicc_activation;
} phHciNfc_ReaderB_Info_t;

/*
*********************** Function Prototype Declaration *************************
*/

/*!
* \brief Allocates the resources of reader B management gate.
*
* This function Allocates the resources of the reader B management
* gate Information Structure.
* 
*/
extern
NFCSTATUS
phHciNfc_ReaderB_Init_Resources(
                                phHciNfc_sContext_t     *psHciContext
                                );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_ReaderB_Get_PipeID function gives the pipe id of the reader B 
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
phHciNfc_ReaderB_Get_PipeID(
                            phHciNfc_sContext_t     *psHciContext,
                            uint8_t                 *ppipe_id
                            );


/**
* \ingroup grp_hci_nfc
*
*  The phHciNfc_ReaderB_Update_PipeInfo function updates the pipe_id of the reader B
*  gate management Structure.
*
*  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                      context Structure.
*  \param[in]  pipeID                  pipeID of the reader B gate
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
phHciNfc_ReaderB_Update_PipeInfo(
                                 phHciNfc_sContext_t     *psHciContext,
                                 uint8_t                 pipeID,
                                 phHciNfc_Pipe_Info_t    *pPipeInfo
                                 );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_ReaderB_Info_Sequence function executes the sequence of operations, to
*   get the PUPI, AFI, APPLICATION_DATA  etc.
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
phHciNfc_ReaderB_Info_Sequence (
                                void             *psHciHandle,
                                void             *pHwRef
                                );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_ReaderB_Update_Info function updates the reader B information.
*
*   \param[in]  psHciContext     psHciContext is the pointer to HCI Layer
*                                context Structure.
*   \param[in]  infotype         To enable the reader B gate
*   \param[in]  rdr_b_info       reader B gate info
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern 
NFCSTATUS
phHciNfc_ReaderB_Update_Info(
                             phHciNfc_sContext_t        *psHciContext,
                             uint8_t                    infotype,
                             void                       *rdr_b_info
                             );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_ReaderB_Set_LayerData function updates higher layer data
*   registry
*
*   \param[in]  psContext        psContext is the pointer to HCI Layer
*                                context Structure.
*   \param[in]  pHwRef           pHwRef is the Information of
*                                the Device Interface Link
*   \param[in]  layer_data_info  layer data information
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern 
NFCSTATUS
phHciNfc_ReaderB_Set_LayerData(
                        void            *psContext,
                        void            *pHwRef,
                        phNfc_sData_t   *layer_data_info
                        );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_ReaderB_Set_AFI function updates application family 
*   identifier registry
*
*   \param[in]  psContext        psContext is the pointer to HCI Layer
*                                context Structure.
*   \param[in]  pHwRef           pHwRef is the Information of
*                                the Device Interface Link
*   \param[in]  afi_value        to afi value update
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern 
NFCSTATUS
phHciNfc_ReaderB_Set_AFI(
                        void         *psContext,
                        void         *pHwRef,
                        uint8_t      afi_value
                        );
#endif /* #ifndef PHHCINFC_RFREADERB_H */


