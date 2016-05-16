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
* $Date: Thu Jun 11 18:45:00 2009 $                                           *
* $Author: ing02260 $                                                         *
* $Revision: 1.14 $                                                            *
* $Aliases: NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK926_R28_2,NFC_FRI1.1_WK926_R28_3,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                                                                *
*                                                                             *
* =========================================================================== *
*/

#ifndef PHHCINFC_NFCIPMGMT_H
#define PHHCINFC_NFCIPMGMT_H

/*@}*/


/**
*  \name HCI
*
* File: \ref phHciNfc_NfcIPMgmt.h
*
*/
/*@{*/
#define PHHCINFC_NFCIP1MGMT_FILEREVISION "$Revision: 1.14 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_NFCIP1MGMT_FILEALIASES  "$Aliases: NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK926_R28_2,NFC_FRI1.1_WK926_R28_3,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"   /**< \ingroup grp_file_attributes */
/*@}*/

/*
***************************** Header File Inclusion ****************************
*/

#include <phHciNfc_Generic.h>

/*
****************************** Macro Definitions *******************************
*/

/* Commands exposed to the upper layer */


/*
******************** Enumeration and Structure Definition **********************
*/
/* LENGTH definition */
#define NFCIP_ATR_MAX_LENGTH                PHHAL_MAX_ATR_LENGTH
#define NFCIP_NFCID_LENGTH                  PHHAL_MAX_UID_LENGTH
#define NFCIP_MAX_DEP_REQ_HDR_LEN           0x05

typedef enum phHciNfc_eNfcIPType{
    NFCIP_INVALID                        = 0x00U,
    NFCIP_INITIATOR,
    NFCIP_TARGET 
}phHciNfc_eNfcIPType_t;

typedef enum phHciNfc_NfcIP_Seq{
    NFCIP_INVALID_SEQUENCE              = 0x00U,
    NFCIP_ATR_INFO,
    NFCIP_STATUS,
    NFCIP_NFCID3I,
    NFCIP_NFCID3T,
    NFCIP_PARAM,
    NFCIP_END_SEQUENCE
} phHciNfc_NfcIP_Seq_t;

typedef enum phHciNfc_eP2PSpeed{
    NFCIP_SPEED_106                         = 0x00U, 
    NFCIP_SPEED_212, 
    NFCIP_SPEED_424, 
    NFCIP_SPEED_848, 
    NFCIP_SPEED_1696, 
    NFCIP_SPEED_3392, 
    NFCIP_SPEED_6784, 
    NFCIP_SPEED_RFU 
}phHciNfc_eP2PSpeed_t;

typedef enum phHciNfc_eNfcIPMode{
    NFCIP_MODE_PAS_106                         = 0x01U, 
    NFCIP_MODE_PAS_212                         = 0x02U, 
    NFCIP_MODE_PAS_424                         = 0x04U, 
    NFCIP_MODE_ACT_106                         = 0x08U, 
    NFCIP_MODE_ACT_212                         = 0x10U, 
    NFCIP_MODE_ACT_424                         = 0x20U,
    NFCIP_MODE_ALL                             = 0x3FU
}phHciNfc_eNfcIPMode_t;

typedef struct phHciNfc_NfcIP_Info
{
    phHciNfc_NfcIP_Seq_t            current_seq;
    phHciNfc_NfcIP_Seq_t            next_seq;
    phHciNfc_eNfcIPType_t           nfcip_type;
    phHciNfc_Pipe_Info_t            *p_init_pipe_info;
    phHciNfc_Pipe_Info_t            *p_tgt_pipe_info;
    phHal_sRemoteDevInformation_t   rem_nfcip_tgt_info;
    /* ATR_RES = General bytes length, Max length = 48 bytes for 
        host = target */
    uint8_t                         atr_res_info[NFCIP_ATR_MAX_LENGTH];
    uint8_t                         atr_res_length;
    /* ATR_REQ = General bytes length, Max length = 48 bytes for 
        host = initiator */
    uint8_t                         atr_req_info[NFCIP_ATR_MAX_LENGTH];
    uint8_t                         atr_req_length;
    /* Contains the current status of the NFCIP-1 link 
        when communication has been set.
            0x00 -> data is expected from the host
            0x01 -> data is expected from the RF side */
    uint8_t                         linkstatus;
    /* Contains the random NFCID3I conveyed with the ATR_REQ. 
        always 10 bytes length */ 
    uint8_t                         nfcid3i_length;
    uint8_t                         nfcid3i[NFCIP_NFCID_LENGTH];
    /* Contains the random NFCID3T conveyed with the ATR_RES.
        always 10 bytes length */
    uint8_t                         nfcid3t_length;
    uint8_t                         nfcid3t[NFCIP_NFCID_LENGTH];
    /* Contains the current parameters of the NFCIP-1 link when 
    communication has been set.
        - bits 0 to 2: data rate target to initiator
        - bits 3 to 5: data rate initiator to target
            0 -> Divisor equal to 1
            1 -> Divisor equal to 2
            2 -> Divisor equal to 4
            3 -> Divisor equal to 8
            4 -> Divisor equal to 16
            5 -> Divisor equal to 32
            6 -> Divisor equal to 64
            7 -> RFU
            - bits 6 to 7: maximum frame length
            0 -> 64 bytes
            1 -> 128 bytes
            2 -> 192 bytes
            3 -> 256 bytes  */
    phHciNfc_eP2PSpeed_t            initiator_speed;
    phHciNfc_eP2PSpeed_t            target_speed;
    uint16_t                        max_frame_len;
    /* Supported modes */
    uint8_t                         nfcip_mode;
    uint8_t                         psl1;
    uint8_t                         psl2;
    uint8_t                         nad;
    uint8_t                         did;
    uint8_t                         options;
    uint8_t                         activation_mode;
}phHciNfc_NfcIP_Info_t;


/*
*********************** Function Prototype Declaration *************************
*/
/*!
* \brief Allocates the resources of NFCIP-1 initiator management gate.
*
* This function Allocates the resources of the NFCIP-1 initiator management
* gate Information Structure.
* 
*/
extern
NFCSTATUS
phHciNfc_Initiator_Init_Resources(
                                  phHciNfc_sContext_t     *psHciContext
                                  );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_Initiator_Get_PipeID function gives the pipe id of the NFCIP-1 
*   initiator gate
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
phHciNfc_Initiator_Get_PipeID(
                              phHciNfc_sContext_t     *psHciContext,
                              uint8_t                 *ppipe_id
                              );

/**
* \ingroup grp_hci_nfc
*
*  The phHciNfc_Initiator_Update_PipeInfo function updates the pipe_id of the NFCIP-1
*  initiator gate management Structure.
*
*  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                      context Structure.
*  \param[in]  pipeID                  pipeID of the NFCIP-1 initiator gate
*  \param[in]  pPipeInfo               Update the pipe Information of the NFCIP-1 
*                                      initiator gate
*
*  \retval NFCSTATUS_SUCCESS           Function execution is successful.
*  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                      could not be interpreted properly.
*
*/

extern
NFCSTATUS
phHciNfc_Initiator_Update_PipeInfo(
                                   phHciNfc_sContext_t     *psHciContext,
                                   uint8_t                 pipeID,
                                   phHciNfc_Pipe_Info_t    *pPipeInfo
                                   );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_Send_Initiator_Command function executes the command sent by the 
*   upper layer, depending on the commands defined.
*
*   \param[in]  psContext        psContext is the pointer to HCI Layer
*                                context Structure.
*   \param[in]  pHwRef           pHwRef is the Information of
*                                the Device Interface Link
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_NfcIP_Presence_Check(
                                phHciNfc_sContext_t   *psContext,
                                void                  *pHwRef
                                );

/*!
* \brief Allocates the resources of NFCIP-1 target management gate.
*
* This function Allocates the resources of the NFCIP-1 target management
* gate Information Structure.
* 
*/
extern
NFCSTATUS
phHciNfc_Target_Init_Resources(
                               phHciNfc_sContext_t     *psHciContext
                               );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_Target_Get_PipeID function gives the pipe id of the NFCIP-1 
*   target gate
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
phHciNfc_Target_Get_PipeID(
                           phHciNfc_sContext_t     *psHciContext,
                           uint8_t                 *ppipe_id
                           );

/**
* \ingroup grp_hci_nfc
*
*  The phHciNfc_Target_Update_PipeInfo function updates the pipe_id of the NFCIP-1
*  target gate management Structure.
*
*  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                      context Structure.
*  \param[in]  pipeID                  pipeID of the NFCIP-1 target gate
*  \param[in]  pPipeInfo               Update the pipe Information of the NFCIP-1 
*                                      target gate
*
*  \retval NFCSTATUS_SUCCESS           Function execution is successful.
*  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                      could not be interpreted properly.
*
*/

extern
NFCSTATUS
phHciNfc_Target_Update_PipeInfo(
                                phHciNfc_sContext_t     *psHciContext,
                                uint8_t                 pipeID,
                                phHciNfc_Pipe_Info_t    *pPipeInfo
                                );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_NfcIP_Info_Sequence function executes the sequence of operations, to
*   get ATR_RES, NFCID3I, NFCID3T, PARAMS  etc.
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*   \param[in]  notify_reqd             if TRUE continue till END_SEQUENCE, else 
*                                       stop the sequence
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_NfcIP_Info_Sequence (
                              phHciNfc_sContext_t   *psHciContext,
                              void                  *pHwRef
#ifdef NOTIFY_REQD
                              , 
                              uint8_t               notify_reqd
#endif /* #ifdef NOTIFY_REQD */
                              );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_NfcIP_SetMode function sets the value for NFCIP-1 modes
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*   \param[in]  nfciptype               Specifies initiator or target
*   \param[in]  nfcip_mode              nfcip_mode is the supported mode 
*                                       information
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern 
NFCSTATUS
phHciNfc_NfcIP_SetMode(
                       phHciNfc_sContext_t      *psHciContext,
                       void                     *pHwRef,
                       phHciNfc_eNfcIPType_t    nfciptype,
                       uint8_t                  nfcip_mode                      
                       );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_NfcIP_SetNAD function sets the NAD value
*
*   \param[in]  psHciContext            pContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*   \param[in]  nfciptype               Specifies initiator or target
*   \param[in]  nad                     Node address, this will be used as 
*                                       logical address of the initiator (b4 to b7)
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern 
NFCSTATUS
phHciNfc_NfcIP_SetNAD(
                      phHciNfc_sContext_t   *psHciContext,
                      void                  *pHwRef,
                      phHciNfc_eNfcIPType_t nfciptype,
                      uint8_t               nad
                      );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_NfcIP_SetDID function sets the DID value for the initiator
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*   \param[in]  did                     Device ID
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern 
NFCSTATUS
phHciNfc_NfcIP_SetDID(
                      phHciNfc_sContext_t   *psHciContext,
                      void                  *pHwRef,
                      uint8_t               did
                      );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_NfcIP_SetOptions function sets the different options depending on
*   the host type (initiator or target) like PSL, NAD and DID
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*   \param[in]  nfciptype               Specifies initiator or target
*   \param[in]  nfcip_options           specifies enabled options PSL, NAD and DID
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern 
NFCSTATUS
phHciNfc_NfcIP_SetOptions(
                          phHciNfc_sContext_t       *psHciContext,
                          void                      *pHwRef,
                          phHciNfc_eNfcIPType_t     nfciptype,
                          uint8_t                   nfcip_options
                          );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_NfcIP_SetATRInfo function sets the general byte information
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*   \param[in]  nfciptype               Specifies initiator or target
*   \param[in]  atr_info                contains the general bytes of the ATR_REQ
*                                       (initiator) or ATR_RES (target) (max size = 
*                                       48 bytes)
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_NfcIP_SetATRInfo(
                          phHciNfc_sContext_t       *psHciContext,
                          void                      *pHwRef,
                          phHciNfc_eNfcIPType_t     nfciptype,
                          phHal_sNfcIPCfg_t         *atr_info
                          );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_NfcIP_SetPSL1 function sets the BRS byte of PSL_REQ
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*   \param[in]  psl1                    specifies the BRS byte of PSL_REQ
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_NfcIP_SetPSL1(
                       phHciNfc_sContext_t   *psHciContext,
                       void                  *pHwRef,
                       uint8_t               psl1
                          );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_NfcIP_SetPSL2 function sets the BRS byte of PSL_REQ
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*   \param[in]  psl2                    specifies the FSL byte of PSL_REQ
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_NfcIP_SetPSL2(
                       phHciNfc_sContext_t      *psHciContext,
                       void                     *pHwRef,
                       uint8_t                  psl2
                          );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_NfcIP_GetStatus function receives the present status of the 
*   NFCIP-1 link, when communication has been set.
*   If 0x00 is the status, then it means data is expected from the host
*   If 0x01 is the status, then it means data is expected from the RF side
*   Other status values are error
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*   \param[in]  nfciptype               Specifies initiator or target
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern 
NFCSTATUS
phHciNfc_NfcIP_GetStatus(
                            phHciNfc_sContext_t      *psHciContext,
                            void                     *pHwRef,
                            phHciNfc_eNfcIPType_t    nfciptype
                          );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_NfcIP_GetStatus function receives the current parameters of the 
*   NFCIP-1 link, when communication has been set.
*       - bits 0 to 2: data rate target to initiator
*       - bits 3 to 5: data rate initiator to target
*           0 -> Divisor equal to 1
*           1 -> Divisor equal to 2
*           2 -> Divisor equal to 4
*           3 -> Divisor equal to 8
*           4 -> Divisor equal to 16
*           5 -> Divisor equal to 32
*           6 -> Divisor equal to 64
*           7 -> RFU
*       - bits 6 to 7: maximum frame length
*           0 -> 64 bytes
*           1 -> 128 bytes
*           2 -> 192 bytes
*           3 -> 256 bytes
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*   \param[in]  nfciptype               Specifies initiator or target
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern 
NFCSTATUS
phHciNfc_NfcIP_GetParam(
                         phHciNfc_sContext_t    *psHciContext,
                         void                   *pHwRef,
                         phHciNfc_eNfcIPType_t  nfciptype
                         );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_NfcIP_Send_Data function sends data using the SEND_DATA event
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*   \param[in]  sData                   Data to be sent to the lower layer
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern 
NFCSTATUS
phHciNfc_NfcIP_Send_Data (
                         phHciNfc_sContext_t    *psHciContext,
                         void                   *pHwRef, 
                         phHciNfc_XchgInfo_t    *sData
                         );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_Initiator_Cont_Activate function to activate the NFCIP initiator
*
*   \param[in]  pContext                pContext is the pointer to HCI Layer
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
phHciNfc_Initiator_Cont_Activate (
                                  phHciNfc_sContext_t       *psHciContext,
                                  void                      *pHwRef
                                  );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_NfcIP_GetATRInfo function is to get ATR information
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*   \param[in]  nfciptype               Specifies initiator or target
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern
NFCSTATUS
phHciNfc_NfcIP_GetATRInfo (
                           phHciNfc_sContext_t      *psHciContext,
                           void                     *pHwRef, 
                           phHciNfc_eNfcIPType_t    nfciptype
                           );

/**
* \ingroup grp_hci_nfc
*
*   The phHciNfc_NfcIP_SetMergeSak function is to indicate, if the NFCIP-1 
*   target feature must be merged with Type A RF card feature in order to 
*   present only one type A target (set of the related bit in SAK to 
*   reflect the ISO18092 compliancy).
*       0x00 -> disabled
*       0x01 -> enabled
*       Others values are RFU : error code returned as NFCSTATUS_INVALID_PARAMETER
*
*   \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
*                                       context Structure.
*   \param[in]  pHwRef                  pHwRef is the Information of
*                                       the Device Interface Link
*   \param[in]  sak_value               Specifies initiator or target
*
*   \retval NFCSTATUS_SUCCESS           Function execution is successful.
*   \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be interpreted properly.
*
*/
extern 
NFCSTATUS 
phHciNfc_NfcIP_SetMergeSak( 
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef, 
                            uint8_t                 sak_value
                           );
#endif /* #ifndef PHHCINFC_NFCIPMGMT_H */


