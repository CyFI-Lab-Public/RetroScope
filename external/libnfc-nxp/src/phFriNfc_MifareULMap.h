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

/*
 * \file  phFriNfc_MifareULMap.h
 * \brief NFC Ndef Mapping For Mifare UL Card.
 *
 * Project: NFC-FRI
 *
 * $Date: Fri Aug  7 13:06:49 2009 $
 * $Author: ing07336 $
 * $Revision: 1.9 $
 * $Aliases: NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
 *
 */

#ifndef PHFRINFC_MIFAREULMAP_H
#define PHFRINFC_MIFAREULMAP_H

#include <phFriNfc.h>
#if !defined PH_HAL4_ENABLE
#include <phHal4Nfc.h>
#endif
#include <phNfcStatus.h>
#include <phNfcTypes.h>
#include <phFriNfc_NdefMap.h>

#define PH_FRINFC_NDEFMAP_MIFAREMAP_FILEREVISION "$Revision: 1.9 $"
#define PH_FRINFC_NDEFMAP_MIFAREMAP_FILEALIASES  "$Aliases: NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"


/*!
 * \name Mifare UL - states of the Finite State machine
 *
 */
/*@{*/
#define PH_FRINFC_NDEFMAP_MFUL_STATE_READ                        1   /*!< Read State */
#define PH_FRINFC_NDEFMAP_MFUL_STATE_WRITE                       2   /*!< Write is going on*/
#define PH_FRINFC_NDEFMAP_MFUL_STATE_CHK_NDEF_COMP               3   /*!< Check Ndef is going on */
#define PH_FRINFC_NDEFMAP_MFUL_STATE_FND_NDEF_COMP               4   /*!< to find the NDEF TLV */
#define PH_FRINFC_NDEFMAP_MFUL_STATE_TERM_TLV                    5   /*!< to write the terminator TLV */
#define PH_FRINFC_NDEFMAP_MFUL_STATE_WR_LEN_TLV                  6   /*!< Write L value of TLV */
#define PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_CHK_1         7   /*!< to send sector select command 1 */
#define PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_CHK_2		 8   /*!< to send sector select command 2 */
#define PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_RESET_1       9   /*!< to send sector select command 1 for resetting sector 0 */
#define PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_RESET_2		 10   /*!< to send sector select command 2 for resetting sector 0 */
#define PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_READ_1      	 11   /*!< to send sector select command 1 for resetting sector 0 */
#define PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_READ_2		 12   /*!< to send sector select command 2 for resetting sector 0 */
#define PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_WRITE_1       13   /*!< to send sector select command 1 for resetting sector 0 */
#define PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_WRITE_2		 14   /*!< to send sector select command 2 for resetting sector 0 */
#define PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_RW_1       15   /*!< to send sector select command 1 for resetting sector 0 */
#define PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_RW_2		 16   /*!< to send sector select command 2 for resetting sector 0 */
#define PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_WRITE_INIT_1       17   /*!< to send sector select command 1 for resetting sector 0 */
#define PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_WRITE_INIT_2		 18   /*!< to send sector select command 2 for resetting sector 0 */


/*@}*/

/*!
 * \name Mifare - constants for the capability container
 *
 */
/*@{*/
#define PH_FRINFC_NDEFMAP_MFUL_CC_BYTE0                 0xE1 /*!< Capability container byte 0 = 0xE1 */
#define PH_FRINFC_NDEFMAP_MFUL_CC_BYTE1                 0x10 /*!< Capability container byte 1 = 0x10 */
#define PH_FRINFC_NDEFMAP_MFUL_CC_BYTE2                 0x06 /*!< Capability container byte 2 = 0x06 */
#define PH_FRINFC_NDEFMAP_MFUL_CC_BYTE3_RW              0x00 /*!< Capability container byte 3 = 0x00 for 
                                                                  READ WRITE/INITIALISED card state*/
#define PH_FRINFC_NDEFMAP_MFUL_CC_BYTE3_RO              0x0F /*!< Capability container byte 3 = 0x0F for 
                                                                  READ only card state*/
/*@}*/

/*!
 * \name Mifare - constants for Flags
 *
 */
/*@{*/
#define PH_FRINFC_NDEFMAP_MFUL_FLAG0                    0 /*!< Flag value = 0 */
#define PH_FRINFC_NDEFMAP_MFUL_FLAG1                    1 /*!< Flag value = 1 */
/*@}*/

/*!
 * \name Mifare - constants for left shift
 *
 */
/*@{*/
#define PH_FRINFC_NDEFMAP_MFUL_SHIFT8                   8 /*!< Flag value = 0 */
/*@}*/

/*!
 * \name Mifare - TLV related constants
 *
 */
/*@{*/
#define PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_T                0x03 /*!< Type value of TLV = 0x03 */
#define PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_L                0x00 /*!< Length value of TLV = 0x00 */
#define PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_LFF              0xFF /*!< Length value of TLV = 0xFF */
#define PH_FRINFC_NDEFMAP_MFUL_TERMTLV                  0xFE /*!< Terminator TLV value = 0xFE */
#define PH_FRINFC_NDEFMAP_MFUL_NULLTLV                  0x00 /*!< Null TLV value = 0x00 */
#define PH_FRINFC_NDEFMAP_MFUL_LOCK_CTRL_TLV			0x01 /*!< Lock Control TLV value = 0x01 */
#define PH_FRINFC_NDEFMAP_MFUL_MEM_CTRL_TLV				0x02 /*!< Memory Control TVL value = 0x02 */
#define PH_FRINFC_NDEFMAP_MFUL_PROPRIETRY_TLV			0xFD /*!< Proprietry TVL value = 0xFD */


/*@}*/


/*!
 * \name Mifare - Standard constants
 *
 */
/*@{*/
#define PH_FRINFC_NDEFMAP_MFUL_WR_A_BLK                 0x05 /*!< Send Length for Write Ndef */
#define PH_FRINFC_NDEFMAP_MFUL_MAX_SEND_BUF_TO_READ     0x01 /*!< Send Length for Read Ndef */
#define PH_FRINFC_NDEFMAP_MFUL_CHECK_RESP               0x04 /*!< Value of the Sense Response for Mifare UL */
#define PH_FRINFC_NDEFMAP_MFUL_OTP_OFFSET               3    /*!< To initialise the Offset */
#define PH_FRINFC_NDEFMAP_MFUL_MUL8                     8    /*!< Multiply by 8 */
#define PH_FRINFC_NDEFMAP_MFUL_VAL0                     0    /*!< Value 0 */
#define PH_FRINFC_NDEFMAP_MFUL_VAL1                     1    /*!< Value 1 */
#define PH_FRINFC_NDEFMAP_MFUL_VAL2                     2    /*!< Value 2 */
#define PH_FRINFC_NDEFMAP_MFUL_VAL3                     3    /*!< Value 3 */
#define PH_FRINFC_NDEFMAP_MFUL_VAL4                     4    /*!< Value 4 */
#define PH_FRINFC_NDEFMAP_MFUL_VAL5                     5    /*!< Value 5 */
#define PH_FRINFC_NDEFMAP_MFUL_VAL64                    64    /*!< Value 64 */
#define PH_FRINFC_NDEFMAP_MFUL_BYTE0                    0x00 /*!< Byte number 0 */
#define PH_FRINFC_NDEFMAP_MFUL_BYTE1                    0x01 /*!< Byte number 1 */
#define PH_FRINFC_NDEFMAP_MFUL_BYTE2                    0x02 /*!< Byte number 2 */
#define PH_FRINFC_NDEFMAP_MFUL_BYTE3                    0x03 /*!< Byte number 3 */
#define PH_FRINFC_NDEFMAP_MFUL_BYTE4                    0x04 /*!< Byte number 4 */
#define PH_FRINFC_NDEFMAP_MFUL_BLOCK0                   0x00 /*!< Block number 0 */
#define PH_FRINFC_NDEFMAP_MFUL_BLOCK1                   0x01 /*!< Block number 1 */
#define PH_FRINFC_NDEFMAP_MFUL_BLOCK2                   0x02 /*!< Block number 2 */
#define PH_FRINFC_NDEFMAP_MFUL_BLOCK3                   0x03 /*!< Block number 3 */
#define PH_FRINFC_NDEFMAP_MFUL_BLOCK4                   0x04 /*!< Block number 4 */
#define PH_FRINFC_NDEFMAP_MFUL_BLOCK5                   0x05 /*!< Block number 5 */

#define PH_FRINFC_NDEFMAP_MFUL_RDBYTES_16               0x10 /*!< Read Bytes 16 */
#define PH_FRINFC_NDEFMAP_STMFUL_MAX_CARD_SZ            48   /*!< For static maximum memory size is 48 bytes */
#define PH_FRINFC_NDEFMAP_MFUL_WR_BUF_STR               0x04 /*!< To store the block of data written to the card */
/*@}*/

/*!
 * \brief \copydoc page_reg Resets the component instance to the initial state and lets the component forget about
 *        the list of registered items. Moreover, the lower device is set.
 *
 * \param[in] NdefMap Pointer to a valid or uninitialised instance of \ref phFriNfc_NdefMap_t .
 *
 * \note  This function has to be called at the beginning, after creating an instance of
 *        \ref phFriNfc_NdefMap_t . Use this function to reset the instance and/or switch
 *        to a different underlying device (different NFC device or device mode, or different
 *        Remote Device).
 */
NFCSTATUS phFriNfc_MifareUL_H_Reset(  phFriNfc_NdefMap_t        *NdefMap);

/*!
 * \brief \copydoc page_ovr Initiates Reading of NDEF information from the Remote Device.
 *
 * The function initiates the reading of NDEF information from a Remote Device.
 * It performs a reset of the state and starts the action (state machine).
 * A periodic call of the \ref phFriNfc_NdefMap_Process has to be done once the action
 * has been triggered.
 *
 * \param[in] NdefMap Pointer to a valid instance of the \ref phFriNfc_NdefMap_t structure describing
 *                    the component context.
 *
 * \param[in] PacketData  Pointer to a location that receives the NDEF Packet.
 *
 * \param[in,out] PacketDataLength Pointer to a variable receiving the length of the NDEF packet.
 *
 * \param[in] Offset Indicates whether the read operation shall start from the begining of the
 *            file/card storage \b or continue from the last offset. The last Offset set is stored
 *            within a context variable (must not be modified by the integration).
 *            If the caller sets the value to \ref PH_FRINFC_NDEFMAP_SEEK_CUR, the component shall
 *            start reading from the last offset set (continue where it has stopped before).
 *            If set to \ref PH_FRINFC_NDEFMAP_SEEK_BEGIN, the component shall start reading
 *            from the begining of the card (restarted) 
 *
 * \retval NFCSTATUS_PENDING                       The action has been successfully triggered.
 * \retval NFCSTATUS_INVALID_DEVICE_REQUEST        If Previous Operation is Write Ndef and Offset
 *                                                 is Current then this error is displayed.
 * \retval NFCSTATUS_EOF_NDEF_CONTAINER_REACHED         No Space in the File to read.
 * \retval NFCSTATUS_MORE_INFORMATION              There are more bytes to read in the card.
 * \retval NFCSTATUS_SUCCESS                       Last Byte of the card read.
 * \retval NFCSTATUS_INVALID_DEVICE                The device has not been opened or has been disconnected
 *                                                 meanwhile.
 * \retval NFCSTATUS_CMD_ABORTED                   The caller/driver has aborted the request.
 * \retval NFCSTATUS_BUFFER_TOO_SMALL              The buffer provided by the caller is too small.
 * \retval NFCSTATUS_RF_TIMEOUT                    No data has been received within the TIMEOUT period.
 *
 */

NFCSTATUS phFriNfc_MifareUL_RdNdef( phFriNfc_NdefMap_t  *NdefMap,
                                    uint8_t             *PacketData,
                                    uint32_t            *PacketDataLength,
                                    uint8_t             Offset);

/*!
 * \brief \copydoc page_ovr Initiates Writing of NDEF information to the Remote Device.
 *
 * The function initiates the writing of NDEF information to a Remote Device.
 * It performs a reset of the state and starts the action (state machine).
 * A periodic call of the \ref phFriNfc_NdefMap_Process has to be done once the action
 * has been triggered.
 *
 * \param[in] NdefMap Pointer to a valid instance of the \ref phFriNfc_NdefMap_t structure describing
 *                    the component context.
 *
 * \param[in] PacketData  Pointer to a location that holds the prepared NDEF Packet.
 *
 * \param[in,out] PacketDataLength Variable specifying the length of the prepared NDEF packet.
 *
 * \param[in] Offset Indicates whether the write operation shall start from the begining of the
 *            file/card storage \b or continue from the last offset. The last Offset set is stored
 *            within a context variable (must not be modified by the integration).
 *            If the caller sets the value to \ref PH_FRINFC_NDEFMAP_SEEK_CUR, the component shall
 *            start writing from the last offset set (continue where it has stopped before).
 *            If set to \ref PH_FRINFC_NDEFMAP_SEEK_BEGIN, the component shall start writing
 *            from the begining of the card (restarted) 
 *
 * \retval NFCSTATUS_PENDING                        The action has been successfully triggered.
 * \retval NFCSTATUS_INVALID_DEVICE_REQUEST         If Previous Operation is Write Ndef and Offset
 *                                                  is Current then this error is displayed.
 * \retval NFCSTATUS_EOF_NDEF_CONTAINER_REACHED               Last byte is written to the card after this
 *                                                  no further writing is possible.
 * \retval NFCSTATUS_SUCCESS                        Buffer provided by the user is completely written
 *                                                  into the card.
 * \retval NFCSTATUS_INVALID_DEVICE                 The device has not been opened or has been disconnected
 *                                                  meanwhile.
 * \retval NFCSTATUS_CMD_ABORTED                    The caller/driver has aborted the request.
 * \retval NFCSTATUS_BUFFER_TOO_SMALL               The buffer provided by the caller is too small.
 * \retval NFCSTATUS_RF_TIMEOUT                     No data has been received within the TIMEOUT period.
 *
 */

NFCSTATUS phFriNfc_MifareUL_WrNdef( phFriNfc_NdefMap_t  *NdefMap,
                                    uint8_t             *PacketData,
                                    uint32_t            *PacketDataLength,
                                    uint8_t             Offset);

/*!
 * \brief \copydoc page_ovr Check whether a particulat Remote Device is NDEF compliant.
 *
 * The function checks whether the peer device is NDEF compliant.
 *
 * \param[in] NdefMap Pointer to a valid instance of the \ref phFriNfc_NdefMap_t structure describing
 *                    the component context.
 *
 * \retval NFCSTATUS_PENDING               The action has been successfully triggered.
 * \retval NFCSTATUS_INVALID_PARAMETER     At least one parameter of the function is invalid.
 * \retval NFCSTATUS_INVALID_DEVICE         The device has not been opened or has been disconnected
 *                                          meanwhile.
 * \retval NFCSTATUS_CMD_ABORTED            The caller/driver has aborted the request.
 * \retval NFCSTATUS_BUFFER_TOO_SMALL       The buffer provided by the caller is too small.
 * \retval NFCSTATUS_RF_TIMEOUT             No data has been received within the TIMEOUT period.
 *
 */

NFCSTATUS phFriNfc_MifareUL_ChkNdef(    phFriNfc_NdefMap_t  *NdefMap);

/*!
 * \brief \copydoc page_cb Completion Routine, Processing function, needed to avoid long blocking.
 *
 * The function call scheme is according to \ref grp_interact. No State reset is performed during operation.
 *
 * \copydoc pphFriNfc_Cr_t
 *
 * \note The lower (Overlapped HAL) layer must register a pointer to this function as a Completion
 *       Routine in order to be able to notify the component that an I/O has finished and data are
 *       ready to be processed.
 *
 */

void phFriNfc_MifareUL_Process( void        *Context,
                                NFCSTATUS   Status);

                                 
#endif /* PHFRINFC_MIFAREULMAP_H */
