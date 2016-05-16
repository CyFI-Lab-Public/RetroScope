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
* \file  phFriNfc_ISO15693Format.h
* \brief ISO-15693 Smart card formatting.
*
* Project: NFC-FRI
*
* $Date:  $
* $Author: ing02260 $
* $Revision: 1.0 $
* $Aliases:  $
*
*/

#ifndef PHFRINFC_ISO15693FORMAT_H
#define PHFRINFC_ISO15693FORMAT_H

/****************************** Macro definitions start ********************************/

/****************************** Macro definitions end ********************************/

/****************************** Data structures start ********************************/

/****************************** Data structures end ********************************/

/*********************** External function declarations start ***********************/
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
void 
phFriNfc_ISO15693_FmtReset (
    phFriNfc_sNdefSmtCrdFmt_t *psNdefSmtCrdFmt);

/*!
* \ingroup grp_fri_smart_card_formatting
*
* \brief Initiates the card formatting procedure for Remote Smart Card Type.
*
* \copydoc page_ovr The function initiates and formats the ISO-15693 Card.After this 
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
NFCSTATUS 
phFriNfc_ISO15693_Format (
    phFriNfc_sNdefSmtCrdFmt_t *psNdefSmtCrdFmt);

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
* \note For general information about the completion routine interface please see \ref pphFriNfc_Cr_t . 
* The Different Status Values are as follows
*
*/
void 
phFriNfc_ISO15693_FmtProcess (
    void        *pContext,
    NFCSTATUS   Status);

/*********************** External function declarations end ***********************/

#endif /* #define PHFRINFC_ISO15693FORMAT_H */



