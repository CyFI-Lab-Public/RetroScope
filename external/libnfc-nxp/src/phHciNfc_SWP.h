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
* \file  phHciNfc_SWP .h                                                          *
* \brief HCI wired interface gate Management Routines.                        *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Fri Aug 14 17:01:28 2009 $                                                                   *
* $Author: ing04880 $                                                                 *
* $Revision: 1.15 $                                                               *
* $Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                                                *
*                                                                             *
* =========================================================================== *
*/
#ifndef PHHCINFC_SWP_H
#define PHHCINFC_SWP_H
/*@}*/
/**
 *  \name HCI
 *
 * File: \ref phHciNfc_SWP.h
 *
 */
/*@{*/
#define PHHCINFC_SWPRED_FILEREVISION "$Revision: 1.15 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_SWPREDINTERFACE_FILEALIASES  "$Aliases: NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"   /**< \ingroup grp_file_attributes */
/*@}*/

/****************************** Header File Inclusion *****************************/
#include <phHciNfc_Generic.h>

/******************************* Macro Definitions ********************************/

/* Kb/sec */
#define UICC_REF_BITRATE            (106U) 
#define UICC_MAX_CONNECT_RETRY      (0x02U)

/* SWP switch mode event parameters */
#define UICC_SWITCH_MODE_OFF        (0x00U)
#define UICC_SWITCH_MODE_DEFAULT    (0x01U)
#define UICC_SWITCH_MODE_ON         (0x02U)

/******************** Enumeration and Structure Definition ***********************/

typedef enum phHciNfc_SWP_Seq{
    SWP_INVALID_SEQUENCE = 0x00U, 
    SWP_MODE_SEQ, 
    SWP_STATUS_SEQ, 
    SWP_END_SEQ
}phHciNfc_SWP_Seq_t;

typedef enum phHciNfc_SWP_Status{
    UICC_NOT_CONNECTED          =   0x00U,
    UICC_CONNECTION_ONGOING,
    UICC_CONNECTED,
    UICC_CONNECTION_LOST,
    UICC_DISCONNECTION_ONGOING,
    UICC_CONNECTION_FAILED
}phHciNfc_SWP_Status_t;


/* Information structure for  SWP  Gate */
typedef struct phHciNfc_SWP_Info{

    /* Pointer to SWP gate pipe information */
    phHciNfc_Pipe_Info_t            *p_pipe_info;
    /* SWP gate pipe Identified             */
    uint8_t                         pipe_id;
    /*Current internal Sequence type        */
    phHciNfc_SWP_Seq_t              current_seq;
    /*Current next Sequence ID          */
    phHciNfc_SWP_Seq_t              next_seq;

    phHciNfc_SWP_Status_t           uicc_status;

    uint8_t                         uicc_bitrate;

} phHciNfc_SWP_Info_t;

/************************ Function Prototype Declaration *************************/
/*!
 * \brief Allocates the resources required for  SWP gate management.
 *
 * This function Allocates necessary resources as requiered by SWP gate management
 *
 * \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *
 * \retval NFCSTATUS_SUCCESS           Function execution is successful
 *
 * \retval NFCSTATUS_INVALID_PARAMETER One or more of the given inputs are not valid
 */
extern
NFCSTATUS
phHciNfc_SWP_Init_Resources(phHciNfc_sContext_t  *psHciContext);

/**
* \ingroup grp_hci_nfc
*
* \brief Allocates the resources required for  SWP gate management.
*
* This function Allocates necessary resources as requiered by SWP gate management
*
* \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*
* \retval NFCSTATUS_SUCCESS           Function execution is successful
*
* \retval NFCSTATUS_INVALID_PARAMETER One or more of the given inputs are not valid
*/

extern
NFCSTATUS   
phHciNfc_SWPMgmt_Initialise(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                         );

/**
* \ingroup grp_hci_nfc
*
* \brief updates SWP gate specific pipe information .
*
* This function  intialises gate specific informations like pipe id,
* event handler and response handler etc.
*
* \param[in]  psHciContext          psHciContext is the pointer to HCI Layer
* \param[in]  pipeID                pipeID of the SWP management Gate
* \param[in]  pPipeInfo             Update the pipe Information of the SWP
*                                   Management Gate.
*
* \retval NFCSTATUS_SUCCESS           Function execution is successful
*
* \retval NFCSTATUS_INVALID_PARAMETER One or more of the given inputs are not valid
*/
extern
NFCSTATUS
phHciNfc_SWP_Update_PipeInfo(
                                  phHciNfc_sContext_t     *psHciContext,
                                  uint8_t                 pipeID,
                                  phHciNfc_Pipe_Info_t    *pPipeInfo
                                  );
/**
* \ingroup grp_hci_nfc
*
* \brief updates SWP gate specific pipe information .
*
* This function  intialises gate specific informations like pipe id,
* event handler and response handler etc.
*
* \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*
* \retval NFCSTATUS_SUCCESS           Function execution is successful
*
* \retval NFCSTATUS_INVALID_PARAMETER One or more of the given inputs are not valid
*/

extern
NFCSTATUS
phHciNfc_SWP_Get_PipeID(
                       phHciNfc_sContext_t        *psHciContext,
                       uint8_t                    *ppipe_id
                       );

/**
* \ingroup grp_hci_nfc
*
* \brief Enables /disables SWP mode .
*
* This function  enables/disables SWP link associated with UICC.
* 
*
* \param[in]  psHciContext              psHciContext is pointer to HCI Layer
*
* \param[in]  pHwRef                    pHwRef is underlying Hardware context.
*
* \param[in]  enable_type               0 means disable ,1 means enable SWP link.
* \retval NFCSTATUS_SUCCESS             Function execution is successful
*
* \retval NFCSTATUS_INVALID_PARAMETER   One or more of the given inputs are not valid
*/
extern
NFCSTATUS
phHciNfc_SWP_Configure_Default(
                            void        *psHciHandle,
                            void        *pHwRef,
                            uint8_t     enable_type
                        );

/**
* \ingroup grp_hci_nfc
*
* \brief Enables /disables SWP mode .
*
* This function  enables/disables SWP link associated with UICC.
* 
*
* \param[in]  psHciContext              psHciContext is pointer to HCI Layer
*
* \param[in]  pHwRef                    pHwRef is underlying Hardware context.
*
* \param[in]  mode                      TRUE Enable Protection.
* \retval NFCSTATUS_SUCCESS             Function execution is successful
*
* \retval NFCSTATUS_INVALID_PARAMETER   One or more of the given inputs are not valid
*/
extern
NFCSTATUS
phHciNfc_SWP_Protection(
                            void        *psHciHandle,
                            void        *pHwRef,
                            uint8_t     mode
                        );


/**
* \ingroup grp_hci_nfc
*
* \brief To send the switch mode event
*
* This function send an event to change the switch mode.
* 
*
* \param[in]  psHciContext              psHciContext is pointer to HCI Layer
*
* \param[in]  pHwRef                    pHwRef is underlying Hardware context.
*
* \param[in]  uicc_mode                 UICC_SWITCH_MODE_OFF
*                                       UICC_SWITCH_MODE_DEFAULT
*                                       UICC_SWITCH_MODE_ON
* \retval NFCSTATUS_SUCCESS             Function execution is successful
*
* \retval NFCSTATUS_INVALID_PARAMETER   One or more of the given inputs are not valid
*/
extern 
NFCSTATUS 
phHciNfc_SWP_Configure_Mode(
                              void              *psHciHandle,
                              void              *pHwRef,
                              uint8_t           uicc_mode
                          );

/**
* \ingroup grp_hci_nfc
*
* \brief To get the status of the UICC
*
* This function reads the status of the UICC. The status value can be any 
* of the values present in the \ref phHciNfc_SWP_Status_t
* 
*
* \param[in]  psHciContext              psHciContext is pointer to HCI Layer
*
* \param[in]  pHwRef                    pHwRef is underlying Hardware context.
* \retval NFCSTATUS_SUCCESS             Function execution is successful
*
* \retval NFCSTATUS_INVALID_PARAMETER   One or more of the given inputs are not valid
*/
extern
NFCSTATUS
phHciNfc_SWP_Get_Status(
                            void        *psHciHandle,
                            void        *pHwRef
                        );

/**
* \ingroup grp_hci_nfc
*
* \brief To get the bitrate
*
* This function reads the bitrate
* 
*
* \param[in]  psHciContext              psHciContext is pointer to HCI Layer
*
* \param[in]  pHwRef                    pHwRef is underlying Hardware context.
* \retval NFCSTATUS_SUCCESS             Function execution is successful
*
* \retval NFCSTATUS_INVALID_PARAMETER   One or more of the given inputs are not valid
*/
extern
NFCSTATUS
phHciNfc_SWP_Get_Bitrate(
                            void        *psHciHandle,
                            void        *pHwRef
                        );

/**
* \ingroup grp_hci_nfc
*
* \brief To update the sequence
*
* This function reads the bitrate
* 
*
* \param[in]  psHciContext              psHciContext is pointer to HCI Layer
*
* \param[in]  SWP_seq                   SWP sequence.
*
* \retval NFCSTATUS_SUCCESS             Function execution is successful
* \retval NFCSTATUS_INVALID_PARAMETER   One or more of the given inputs are not valid
*/
extern 
NFCSTATUS
phHciNfc_SWP_Update_Sequence(
                                phHciNfc_sContext_t     *psHciContext,
                                phHciNfc_eSeqType_t     SWP_seq
                             );

/**
* \ingroup grp_hci_nfc
*
* \brief To configure default mode and the default status.
*
* This function configures default status and default mode.
* 
*
* \param[in]  psHciContext              psHciContext is pointer to HCI Layer
* \param[in]  pHwRef                    pHwRef is underlying Hardware context.
* \param[in]  ps_emulation_cfg          emulation configuration info.
*
*
* \retval NFCSTATUS_SUCCESS             Function execution is successful
*
* \retval NFCSTATUS_INVALID_PARAMETER   One or more of the given inputs are not valid
*/
extern 
NFCSTATUS
phHciNfc_SWP_Config_Sequence(
                            phHciNfc_sContext_t     *psHciContext, 
                            void                    *pHwRef, 
                            phHal_sEmulationCfg_t   *ps_emulation_cfg
                        );
                             

#endif /* #ifndef PHHCINFC_SWP_H */


