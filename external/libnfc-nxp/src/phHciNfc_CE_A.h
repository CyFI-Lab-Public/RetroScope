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
* \file  phHciNfc_CE_A.h                                             *
* \brief HCI card emulation management routines.                              *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Fri Aug 14 17:01:27 2009 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.5 $                                                            *
* $Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                                                *
*                                                                             *
* =========================================================================== *
*/


#ifndef PHHCINFC_CE_A_H
#define PHHCINFC_CE_A_H

/*@}*/


/**
*  \name HCI
*
* File: \ref phHciNfc_CE_A.h
*
*/
/*@{*/
#define PHHCINFC_CE_A_FILEREVISION "$Revision: 1.5 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_CE_A_FILEALIASES  "$Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"     /**< \ingroup grp_file_attributes */
/*@}*/

/*
***************************** Header File Inclusion ****************************
*/

#include <phHciNfc_Generic.h>

/*
****************************** Macro Definitions *******************************
*/

#define HOST_CE_A_MODE_INDEX                (0x01U)
#define HOST_CE_A_UID_REG_INDEX             (0x02U)
#define HOST_CE_A_SAK_INDEX                 (0x03U)
#define HOST_CE_A_ATQA_INDEX                (0x04U)
#define HOST_CE_A_APP_DATA_INDEX            (0x05U)
#define HOST_CE_A_FWI_SFGT_INDEX            (0x06U)
#define HOST_CE_A_CID_INDEX                 (0x07U)
#define HOST_CE_A_CLT_INDEX                 (0x08U)
#define HOST_CE_A_DATA_RATE_INDEX           (0x09U)


/*
******************** Enumeration and Structure Definition **********************
*/

/* Sequence list */
typedef enum phHciNfc_CE_A_Seq{
    HOST_CE_A_INVALID_SEQ,
    HOST_CE_A_PIPE_OPEN,
    HOST_CE_A_SAK_SEQ,
    HOST_CE_A_ATQA_SEQ,
    HOST_CE_A_ENABLE_SEQ,
    HOST_CE_A_DISABLE_SEQ,
    HOST_CE_A_PIPE_CLOSE, 
    HOST_CE_A_PIPE_DELETE
}phHciNfc_CE_A_Seq_t;

/* Information structure for the card emulation A Gate */
typedef struct phHciNfc_CE_A_Info{

    phHciNfc_CE_A_Seq_t         current_seq;
    phHciNfc_CE_A_Seq_t         next_seq;
    /* Pointer to the card emulation A pipe information */
    phHciNfc_Pipe_Info_t        *p_pipe_info;
    uint8_t                     pipe_id;
    
} phHciNfc_CE_A_Info_t;

/*
*********************** Function Prototype Declaration *************************
*/

/*!
 * \brief Allocates the resources of card emulation A management gate.
 *
 * This function Allocates the resources of the card emulation A management
 * gate Information Structure.
 * 
 */
extern
NFCSTATUS
phHciNfc_CE_A_Init_Resources(
                                phHciNfc_sContext_t     *psHciContext
                         );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_CE_A_Get_PipeID function gives the pipe id of the card 
*   emulation A gate
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
phHciNfc_CE_A_Get_PipeID(
                            phHciNfc_sContext_t     *psHciContext,
                            uint8_t                 *ppipe_id
                            );

/**
* \ingroup grp_hci_nfc
*
*  The phHciNfc_CE_A_Update_PipeInfo function updates the pipe_id of the card 
*  emulation A gate management Structure.
*
*  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                      context Structure.
*  \param[in]  pipeID                  pipeID of the card emulation A gate
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
phHciNfc_CE_A_Update_PipeInfo(
                                  phHciNfc_sContext_t     *psHciContext,
                                  uint8_t                 pipeID,
                                  phHciNfc_Pipe_Info_t    *pPipeInfo
                                  );

/**
* \ingroup grp_hci_nfc
*
*  The phHciNfc_CE_A_SendData_Event function sends data to the lo
*
*  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                      context Structure.
*  \param[in]  pHwRef                  pHwRef is the Information of
*                                      the Device Interface Link
*  \param[in]  pipeID                  pipeID of the card emulation A gate
*  \param[in]  pPipeInfo               Update the pipe Information of the reader 
*                                      A gate
*
*  \retval NFCSTATUS_SUCCESS           Function execution is successful.
*  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                      could not be interpreted properly.
*
*/
#ifdef CE_A_SEND_EVENT
extern
NFCSTATUS
phHciNfc_CE_A_SendData_Event(
                             void               *psContext,
                             void               *pHwRef,
                             uint8_t            *pEvent,
                             uint8_t            length
                       );
#endif /* #ifdef CE_A_SEND_EVENT */

/**
* \ingroup grp_hci_nfc
*
*  The phHciNfc_CE_A_Mode function sends data to the set the card emulation mode
*
*  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                      context Structure.
*  \param[in]  pHwRef                  pHwRef is the Information of
*                                      the Device Interface Link
*  \param[in]  enable_type             type to enable
*
*  \retval NFCSTATUS_SUCCESS           Function execution is successful.
*  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                      could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_CE_A_Mode(
                            void        *psHciHandle,
                            void        *pHwRef,
                            uint8_t     enable_type
                  );

/**
* \ingroup grp_hci_nfc
*
*  The phHciNfc_CE_A_Initialise function opens the CE A and set all the 
*   required parameters for CE A
*
*  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                      context Structure.
*  \param[in]  pHwRef                  pHwRef is the Information of
*                                      the Device Interface Link
*
*  \retval NFCSTATUS_SUCCESS           Function execution is successful.
*  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                      could not be interpreted properly.
*
*/
extern 
NFCSTATUS
phHciNfc_CE_A_Initialise(
                        phHciNfc_sContext_t     *psHciContext,
                        void                    *pHwRef
                        );

/**
* \ingroup grp_hci_nfc
*
*  The phHciNfc_CE_A_Initialise function close the CE A and reset all the 
*   required parameters to default value of CE A
*
*  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                      context Structure.
*  \param[in]  pHwRef                  pHwRef is the Information of
*                                      the Device Interface Link
*
*  \retval NFCSTATUS_SUCCESS           Function execution is successful.
*  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                      could not be interpreted properly.
*
*/
extern 
NFCSTATUS
phHciNfc_CE_A_Release(
                        phHciNfc_sContext_t     *psHciContext,
                        void                    *pHwRef
                        );


/**
* \ingroup grp_hci_nfc
*
*  The phHciNfc_CE_A_Update_Seq function to update CE A sequence depending on the 
*  specified \ref seq_type
*
*  \param[in]  psHciContext             psHciContext is the pointer to HCI Layer
*                                       context Structure.
*  \param[in]  seq_type                 sequence type specified in 
*                                       \ref phHciNfc_eSeqType_t
*
*  \retval NFCSTATUS_SUCCESS           Function execution is successful.
*  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                      could not be interpreted properly.
*
*/
extern 
NFCSTATUS
phHciNfc_CE_A_Update_Seq(
                        phHciNfc_sContext_t     *psHciContext,
                        phHciNfc_eSeqType_t     seq_type
                    );
#endif /* PHHCINFC_CE_A_H */


