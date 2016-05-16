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
* \file  phFriNfc_DesfireFormat.h
* \brief Type4 Smart card formatting.
*
* Project: NFC-FRI
*
* $Date: Tue Jul 27 08:59:52 2010 $
* $Author: ing02260 $
* $Revision: 1.3 $
* $Aliases:  $
*
*/

#ifndef PHFRINFC_DESFIREFORMAT_H
#define PHFRINFC_DESFIREFORMAT_H


/*! \ingroup grp_file_attributes
*  \name NDEF Smart Card Foramting
*
* File: \ref phFriNfc_DesfireFormat.h
*
*/
/*@{*/

/*@}*/


/* Enum to represent the state variables*/
enum{

    PH_FRINFC_DESF_STATE_CREATE_AID = 0, 
    PH_FRINFC_DESF_STATE_SELECT_APP = 1,
    PH_FRINFC_DESF_STATE_CREATE_CCFILE = 2,
    PH_FRINFC_DESF_STATE_CREATE_NDEFFILE = 3,
    PH_FRINFC_DESF_STATE_WRITE_CC_FILE = 4,
    PH_FRINFC_DESF_STATE_WRITE_NDEF_FILE = 5,
    PH_FRINFC_DESF_STATE_DISCON = 6,
    PH_FRINFC_DESF_STATE_CON = 7,
    PH_FRINFC_DESF_STATE_POLL = 8,
    PH_FRINFC_DESF_STATE_GET_UID = 9,
    PH_FRINFC_DESF_STATE_GET_SW_VERSION = 10,
    PH_FRINFC_DESF_STATE_GET_HW_VERSION = 11,
#ifdef FRINFC_READONLY_NDEF

#ifdef DESFIRE_FMT_EV1
    PH_FRINFC_DESF_STATE_RO_SELECT_APP_EV1 = 100,
#endif /* #ifdef DESFIRE_FMT_EV1 */

    PH_FRINFC_DESF_STATE_RO_SELECT_APP = 101,
    PH_FRINFC_DESF_STATE_RO_SELECT_CC_FILE = 102,
    PH_FRINFC_DESF_STATE_RO_READ_CC_FILE = 103,
    PH_FRINFC_DESF_STATE_RO_UPDATE_CC_FILE = 104,

#endif /* #ifdef FRINFC_READONLY_NDEF */

    /* following are used in the ISO wrapper commands*/
    PH_FRINFC_DESF_CREATEAPP_CMD = 0,
    PH_FRINFC_DESF_SELECTAPP_CMD = 1,
    PH_FRINFC_DESF_CREATECC_CMD = 2,
    PH_FRINFC_DESF_CREATENDEF_CMD = 3,
    PH_FRINFC_DESF_WRITECC_CMD = 4,
#ifdef FRINFC_READONLY_NDEF
    PH_FRINFC_DESF_WRITECC_CMD_READ_ONLY = 20, 
#endif /* #ifdef FRINFC_READONLY_NDEF */
    PH_FRINFC_DESF_WRITENDEF_CMD = 5,
    PH_FRINFC_DESF_GET_HW_VERSION_CMD = 6,
    PH_FRINFC_DESF_GET_SW_VERSION_CMD = 7,
    PH_FRINFC_DESF_GET_UID_CMD = 8,
    PH_FRINFC_DESF_WRITENDEF_CMD_SNLEN = 15,
    PH_FRINFC_DESF_WRITECC_CMD_SNLEN = 28,
    PH_FRINFC_DESF_CREATECCNDEF_CMD_SNLEN = 13,
    PH_FRINFC_DESF_SELECTAPP_CMD_SNLEN = 9,
    PH_FRINFC_DESF_CREATEAPP_CMD_SNLEN = 11,
    PH_FRINFC_DESF_NATIVE_OFFSET_P1 = 0x00,
    PH_FRINFC_DESF_NATIVE_OFFSET_P2 = 0x00,
    PH_FRINFC_DESF_NATIVE_LE_BYTE = 0x00,
    PH_FRINFC_DESF_NATIVE_CRAPP_WRDT_LEN = 5,
    PH_FRINFC_DESF_NATIVE_SLAPP_WRDT_LEN = 3,
    PH_FRINFC_DESF_NATIVE_CRCCNDEF_WRDT_LEN = 7,
    PH_FRINFC_DESF_NATIVE_WRCC_WRDT_LEN = 22,
    PH_FRINFC_DESF_NATIVE_WRNDEF_WRDT_LEN = 9

};


/* CC File contents*/

#define  PH_FRINFC_DESF_CCFILE_BYTES                    {0x00,0x0f,0x10,0x00,0x3B,0x00,0x34,0x04,0x06,0xE1,0x04,0x04,0x00,0x00,0x00 }
#define  PH_FRINFC_DESF_NDEFFILE_BYTES                  {0x00,0x00}
#define  PH_FRINFC_DESF_PICC_MASTER_KEY                 {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00 }
#define  PH_FRINFC_DESF_NFCFORUM_APP_KEY                {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00 }
#define  PH_FRINFC_DESF_COMM_SETTINGS                   0x00
#define  PH_FRINFC_DESF_CREATE_DATA_FILE_CMD            0xCD
#define  PH_FRINFC_DESF_NATIVE_CLASS_BYTE               0x90

/* Constant defined to specify the NFC Forum Application ID : 0xEEEE10*/
/* This is defined in order to support to N/W Byte order style : LSB : : MSB*/
#define PH_FRINFC_DESF_FIRST_AID_BYTE                   0x10
#define PH_FRINFC_DESF_SEC_AID_BYTE                     0xEE
#define PH_FRINFC_DESF_THIRD_AID_BYTE                   0xEE


/* Create File command constants*/
#define  PH_FRINFC_DESF_CREATE_AID_CMD                  0xCA
   
/* Specifies the NFC Forum App Number of Keys*/
#define  PH_FRINFC_DESF_NFCFORUM_APP_NO_OF_KEYS         0x01

#define  PH_FRINFC_DESF_SLECT_APP_CMD                   0x5A

#define  PH_FRINFC_DESF_GET_VER_CMD                     0x60


#define  PH_FRINFC_DESF_NATIVE_RESP_BYTE1               0x91 
#define  PH_FRINFC_DESF_NATIVE_RESP_BYTE2               0x00

/* Create CC File Commands*/
#define  PH_FRINFC_DESF_CC_FILE_ID                      0x03 
#define  PH_FRINFC_DESF_CC_FILE_SIZE                    0x0F
#define  PH_FRINFC_DESF_FIRST_BYTE_CC_ACCESS_RIGHTS     0x00
#define  PH_FRINFC_DESF_SEC_BYTE_CC_ACCESS_RIGHTS       0xE0


/* Create NDEF File Commands*/
#define  PH_FRINFC_DESF_NDEF_FILE_ID                    0x04
#define  PH_FRINFC_DESF_NDEF_FILE_SIZE                  0x04
#define  PH_FRINFC_DESF_FIRST_BYTE_NDEF_ACCESS_RIGHTS   0xE0
#define  PH_FRINFC_DESF_SEC_BYTE_NDEF_ACCESS_RIGHTS     0xEE


/* Write/Read Data commands/constants*/
#define  PH_FRINFC_DESF_WRITE_CMD                       0x3D

/* PICC additional frame response*/
#define  PH_FRINFC_DESF_PICC_ADDI_FRAME_RESP            0xAF

/* Response for PICC native DESFire wrapper cmd*/
#define  PH_FRINFC_DESF_NAT_WRAP_FIRST_RESP_BYTE        0x91
#define  PH_FRINFC_DESF_NAT_WRAP_SEC_RESP_BYTE          0x00

/* DESFire4 Major/Minor versions*/
#define  PH_FRINFC_DESF4_MAJOR_VERSION                  0x00
#define  PH_FRINFC_DESF4_MINOR_VERSION                  0x06

/* DESFire4 memory size*/
#define  PH_FRINFC_DESF4_MEMORY_SIZE                    0xEDE

enum{
    PH_SMTCRDFMT_DESF_VAL0 = 0,
    PH_SMTCRDFMT_DESF_VAL1 = 1,
    PH_SMTCRDFMT_DESF_VAL2 = 2,
    PH_SMTCRDFMT_DESF_VAL3 = 3,
    PH_SMTCRDFMT_DESF_VAL4 = 4,
    PH_SMTCRDFMT_DESF_VAL14 = 14,
    PH_SMTCRDFMT_DESF_VAL15 = 15
};



/*!
* \brief \copydoc page_reg Resets the component instance to the initial state and lets the component forget about
*        the list of registered items. Moreover, the lower device is set.
*
* \param[in] NdefSmtCrdFmt Pointer to a valid or uninitialized instance of \ref phFriNfc_sNdefSmtCrdFmt_t.
*
* \note  This function has to be called at the beginning, after creating an instance of
*        \ref phFriNfc_sNdefSmtCrdFmt_t. Use this function to reset the instance of smart card
formatting context variables.
*/
void phFriNfc_Desfire_Reset(phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt);

/*!
* \ingroup grp_fri_smart_card_formatting
*
* \brief Initiates the card formatting procedure for Remote Smart Card Type.
*
* \copydoc page_ovr The function initiates and formats the DESFire Card.After this 
*                   operation,remote card would be properly initialized and 
*                   Ndef Compliant.Depending upon the different card type, this 
*                   function handles formatting procedure.This function also handles
*                   the different recovery procedures for different types of the cards. 
*                   For both Format and Recovery Management same API is used.
* 
* \param[in] phFriNfc_sNdefSmartCardFmt_t Pointer to a valid instance of the \ref phFriNfc_sNdefSmartCardFmt_t
*                             structure describing the component context.
*
* \retval NFCSTATUS_SUCCESS                  Card formatting has been successfully completed.
* \retval NFCSTATUS_PENDING                  The action has been successfully triggered.
* \retval NFCSTATUS_FORMAT_ERROR             Error occured during the formatting procedure.
* \retval NFCSTATUS_INVALID_REMOTE_DEVICE    Card Type is unsupported.
* \retval NFCSTATUS_INVALID_DEVICE_REQUEST   Command or Operation types are mismatching.
*
*/
NFCSTATUS phFriNfc_Desfire_Format(phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt);

/*!
* \brief \copydoc page_reg Resets the component instance to the initial state and lets the component forget about
*        the list of registered items. Moreover, the lower device is set.
*
* \param[in] NdefSmtCrdFmt Pointer to a valid or uninitialized instance of \ref phFriNfc_sNdefSmtCrdFmt_t.
*
* \note  This function has to be called at the beginning, after creating an instance of
*        \ref phFriNfc_sNdefSmtCrdFmt_t. Use this function to reset the instance of smart card
formatting context variables.
*/
void phFriNfc_Desfire_Reset(phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt);

#ifdef FRINFC_READONLY_NDEF
/*!
 * \ingroup grp_fri_smart_card_formatting
 *
 * \brief Initiates the conversion of the already NDEF formatted tag to READ ONLY.
 *
 * \copydoc page_ovr  The function initiates the conversion of the already NDEF formatted
 * tag to READ ONLY. After this formation, remote card would be properly Ndef Compliant and READ ONLY.
 * Depending upon the different card type, this function handles formatting procedure.
 * 
 * \param[in] phFriNfc_sNdefSmartCardFmt_t Pointer to a valid instance of the \ref phFriNfc_sNdefSmartCardFmt_t
 *                             structure describing the component context.
 *
 * \retval NFCSTATUS_SUCCESS                  Card formatting has been successfully completed.
 * \retval NFCSTATUS_PENDING                  The action has been successfully triggered.
 * \retval NFCSTATUS_FORMAT_ERROR             Error occured during the formatting procedure.
 * \retval NFCSTATUS_INVALID_REMOTE_DEVICE    Card Type is unsupported.
 * \retval NFCSTATUS_INVALID_DEVICE_REQUEST   Command or Operation types are mismatching.
 *
 */
NFCSTATUS 
phFriNfc_Desfire_ConvertToReadOnly (
    phFriNfc_sNdefSmtCrdFmt_t   *NdefSmtCrdFmt);
#endif /* #ifdef FRINFC_READONLY_NDEF */

/**
*\ingroup grp_fri_smart_card_formatting
*
* \brief Smart card Formatting \b Completion \b Routine or \b Process function
*
* \copydoc page_ovr Completion Routine: This function is called by the lower layer (OVR HAL)
*                  when an I/O operation has finished. The internal state machine decides
*                  whether to call into the lower device again or to complete the process
*                  by calling into the upper layer's completion routine, stored within this
*                  component's context (\ref phFriNfc_sNdefSmtCrdFmt_t).
*
* The function call scheme is according to \ref grp_interact. No State reset is performed during
* operation.
*
* \param[in] Context The context of the current (not the lower/upper) instance, as set by the lower,
*            calling layer, upon its completion.
* \param[in] Status  The completion status of the lower layer (to be handled by the implementation of
*                    the state machine of this function like a regular return value of an internally
*                    called function).
*
* \note For general information about the completion routine interface please see \ref pphFriNfc_Cr_t . * The Different Status Values are as follows
*
*/
void phFriNfc_Desf_Process(void        *Context,
                           NFCSTATUS   Status);


#endif

