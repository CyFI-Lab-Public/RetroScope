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
 * \file  phFriNfc_MifareStdMap.h
 * \brief NFC Ndef Mapping For Remote Devices.
 *
 * Project: NFC-FRI
 *
 * $Date: Tue May 19 10:30:01 2009 $
 * $Author: ing07336 $
 * $Revision: 1.7 $
 * $Aliases: NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
 *
 */

#ifndef PHFRINFC_MIFARESTDMAP_H
#define PHFRINFC_MIFARESTDMAP_H

#include <phFriNfc.h>
#ifdef PH_HAL4_ENABLE
#include <phHal4Nfc.h>
#else
#include <phHalNfc.h>
#endif
/**  Fix: [NFC-FRI  * * * CCB * * * 0000429]: 
    phFriNfc_MifareMap : Error during compilation  **/
#include <phNfcStatus.h>
#include <phFriNfc_OvrHal.h>

#ifndef PH_HAL4_ENABLE
#include <phFriNfc_OvrHalCmd.h>
#endif

#include <phNfcTypes.h>
#include <phFriNfc_NdefMap.h>

/*! \ingroup grp_file_attributes
 *  \name NDEF Mapping
 *
 * File: \ref phFriNfc_NdefMap.h
 *
 */
/*@{*/
#define PH_FRINFC_MIFARESTDMAP_FILEREVISION "$Revision: 1.7 $"
#define PH_FRINFC_MIFARESTDMAP_FILEALIASES  "$Aliases: NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"
/*@}*/


/*! \defgroup grp_fri_nfc_ndef_map NDEF Mapping for Remote Devices
 *
 *  This component encapsulates Ndef Registration and Listening data and functionality.
 */
/*@{*/

/*!
 * \name NDEF Mapping - states of the Finite State machine
 *
 */
/*@{*/
#define PH_FRINFC_NDEFMAP_STATE_INIT                        0   /*!< Init state. The start-up state */
#define PH_FRINFC_NDEFMAP_STATE_READ                        1   /*!< Read State */
#define PH_FRINFC_NDEFMAP_STATE_WRITE                       2   /*!< Write is going on*/
#define PH_FRINFC_NDEFMAP_STATE_AUTH                        3   /*!< Authenticate is going on*/
#define PH_FRINFC_NDEFMAP_STATE_CHK_NDEF_COMP               4   /*!< Check Ndef is going on */
#define PH_FRINFC_NDEFMAP_STATE_RD_ACS_BIT                  5   /*!< Read access bit is in progress */
#define PH_FRINFC_NDEFMAP_STATE_WR_NDEF_LEN                 6   /*!< Write NDEF TLV LEngth*/
#define PH_FRINFC_NDEFMAP_STATE_RD_TO_WR_NDEF_LEN           7   /*!< read to write the Ndef TLV*/
#define PH_FRINFC_NDEFMAP_STATE_GET_ACT_CARDSIZE            8   /*!< Get the card size */
#define PH_FRINFC_NDEFMAP_STATE_RD_BEF_WR                   9   /*!< Read the NDEF TLV block before starting write */
#define PH_FRINFC_NDEFMAP_STATE_WR_TLV                      10   /*!< Read the NDEF TLV block before starting write */
#define PH_FRINFC_NDEFMAP_STATE_RD_TLV                      11   /*!< Read the NDEF TLV block */
#define PH_FRINFC_NDEFMAP_STATE_TERM_TLV                    12   /*!< Write terminator TLV block */
#define PH_FRINFC_NDEFMAP_STATE_POLL                        13   /*!< Poll in progress */
#define PH_FRINFC_NDEFMAP_STATE_DISCONNECT                  14   /*!< Disconnect in progress */
#define PH_FRINFC_NDEFMAP_STATE_CONNECT                     15   /*!< Connect in progress */

#define PH_FRINFC_NDEFMAP_STATE_RD_SEC_ACS_BIT				16    /*!< Convert to ReadOnly in progress */
#define PH_FRINFC_NDEFMAP_STATE_WRITE_SEC					17    /*!< Convert to ReadOnly in progress */


/*@}*/

/*!
 * \name Mifare Standard - NDEF Compliant Flags
 *
 */
/*@{*/
#define PH_FRINFC_MIFARESTD_NDEF_COMP                       0   /*!< Sector is NDEF Compliant */
#define PH_FRINFC_MIFARESTD_NON_NDEF_COMP                   1   /*!< Sector is not NDEF Compliant */
/*@}*/

/*!
 * \name Mifare Standard - NDEF Compliant Flags
 *
 */
/*@{*/
#define PH_FRINFC_MIFARESTD_PROP_1ST_CONFIG                      0   /*!< No proprietary forum sector found */
#define PH_FRINFC_MIFARESTD_PROP_2ND_CONFIG                      1   /*!< Here the proprietary 
                                                                        forum sector exists after NFC forum
                                                                        sector */
#define PH_FRINFC_MIFARESTD_PROP_3RD_CONFIG                      2   /*!< Here the proprietary 
                                                                        forum sector exists before NFC forum
                                                                        sector */
/*@}*/

/*!
 * \name Mifare Standard - NDEF Compliant Flags
 *
 */
/*@{*/
#define PH_FRINFC_MIFARESTD_MADSECT_ACS_BYTE6                   0x78   /*!< Access Bit for Byte 6 in 
                                                                        MAD sector trailer */
#define PH_FRINFC_MIFARESTD_MADSECT_ACS_BYTE7                   0x77   /*!< Access Bit for Byte 7 in 
                                                                        MAD sector trailer */
#define PH_FRINFC_MIFARESTD_NFCSECT_ACS_BYTE6                   0x7F   /*!< Access Bit for Byte 6 in 
                                                                        NFC forum sector trailer */
#define PH_FRINFC_MIFARESTD_NFCSECT_ACS_BYTE7                   0x07   /*!< Access Bit for Byte 7 in 
                                                                        NFC forum sector trailer */
#define PH_FRINFC_MIFARESTD_ACS_BYTE8                           0x88   /*!< Access Bit for Byte 8 in 
                                                                        all sector trailer */
#define PH_FRINFC_MIFARESTD_NFCSECT_RDACS_BYTE6                 0x07   /*!< Access Bit for Byte 6 in 
                                                                        NFC forum sector trailer for 
                                                                        Read Only State */
#define PH_FRINFC_MIFARESTD_NFCSECT_RDACS_BYTE7                 0x8F   /*!< Access Bit for Byte 7 in 
                                                                        NFC forum sector trailer 
                                                                        Read Only State */
#define PH_FRINFC_MIFARESTD_NFCSECT_RDACS_BYTE8                 0x0F   /*!< Access Bit for Byte 8 in 
                                                                        NFC forum sector trailer 
                                                                        Read Only State */
/*@}*/

/*!
 * \name Mifare Standard - Mifare Standard constants
 *
 */
/*@{*/
#define MIFARE_MAX_SEND_BUF_TO_READ                         1   /*!< Send Length for Reading a Block */
#define MIFARE_MAX_SEND_BUF_TO_WRITE                        17  /*!< Send Length for writing a Block */
#define MIFARE_AUTHENTICATE_CMD_LENGTH                      7   /*!< Send Length for authenticating a Block */
/*@}*/

/*!
 * \name Mifare standard - Constants
 *
 */
/*@{*/
#define PH_FRINFC_MIFARESTD_MAD_BLK0                      0  /*!< Block number 0 */
#define PH_FRINFC_MIFARESTD_MAD_BLK1                      1  /*!< Block number 1 */
#define PH_FRINFC_MIFARESTD_MAD_BLK2                      2  /*!< Block number 2 */
#define PH_FRINFC_MIFARESTD_MAD_BLK3                      3  /*!< Block number 3 */
#define PH_FRINFC_MIFARESTD_BLK4                          4  /*!< Block number 4 */
#define PH_FRINFC_MIFARESTD_BLK5                          5  /*!< Block number 5 */
#define PH_FRINFC_MIFARESTD_BLK6                          6  /*!< Block number 6 */
#define PH_FRINFC_MIFARESTD_BLK7                          7  /*!< Block number 7 */
#define PH_FRINFC_MIFARESTD_BLK8                          8  /*!< Block number 8 */
#define PH_FRINFC_MIFARESTD_BLK9                          9  /*!< Block number 9 */
#define PH_FRINFC_MIFARESTD_BLK10                         10 /*!< Block number 10 */
#define PH_FRINFC_MIFARESTD_BLK11                         11 /*!< Block number 11 */
#define PH_FRINFC_MIFARESTD_BLK12                         12 /*!< Block number 12 */
#define PH_FRINFC_MIFARESTD_BLK13                         13 /*!< Block number 13 */
#define PH_FRINFC_MIFARESTD_BLK14                         14 /*!< Block number 14 */
#define PH_FRINFC_MIFARESTD_BLK15                         15 /*!< Block number 15 */
#define PH_FRINFC_MIFARESTD_MAD_BLK16                     16 /*!< Block number 16 */
#define PH_FRINFC_MIFARESTD_MAD_BLK63                     63 /*!< Block number 63 */
#define PH_FRINFC_MIFARESTD_MAD_BLK64                     64 /*!< Block number 64 */
#define PH_FRINFC_MIFARESTD_MAD_BLK65                     65 /*!< Block number 65 */
#define PH_FRINFC_MIFARESTD_MAD_BLK66                     66 /*!< Block number 66 */
#define PH_FRINFC_MIFARESTD_MAD_BLK67                     67 /*!< Block number 67 */
#define PH_FRINFC_MIFARESTD4K_BLK128                      128 /*!< Block number 128 for Mifare 4k */
#define PH_FRINFC_MIFARESTD_SECTOR_NO0                    0  /*!< Sector 0 */
#define PH_FRINFC_MIFARESTD_SECTOR_NO1                    1  /*!< Sector 1 */
#define PH_FRINFC_MIFARESTD_SECTOR_NO16                   16 /*!< Sector 16 */
#define PH_FRINFC_MIFARESTD_SECTOR_NO39                   39 /*!< Sector 39 */
#define PH_FRINFC_MIFARESTD_SECTOR_NO32                   32 /*!< Sector 32 */
#define PH_FRINFC_MIFARESTD4K_TOTAL_SECTOR                40 /*!< Sector 40 */
#define PH_FRINFC_MIFARESTD1K_TOTAL_SECTOR                16 /*!< Sector 40 */
#define PH_FRINFC_MIFARESTD_BYTES_READ                    16 /*!< Bytes read */
#define PH_FRINFC_MIFARESTD_BLOCK_BYTES                   16 /*!< Bytes per block */
#define PH_FRINFC_MIFARESTD_SECTOR_BLOCKS                 16 /*!< Blocks per sector */
#define PH_FRINFC_MIFARESTD_WR_A_BLK                      17 /*!< 17 bytes (including current block)
                                                                  are given to transfer */
#define PH_FRINFC_MIFARESTD4K_MAX_BLOCKS                  210 /*!< Maximum number of Mifare 4k Blocks 
                                                                excluding sector trailer */
#define PH_FRINFC_MIFARESTD1K_MAX_BLK                     63 /*!< Maximum number of Mifare 1k blocks  
                                                                including the sector trailer*/
#define PH_FRINFC_MIFARESTD4K_MAX_BLK                     254 /*!< Maximum number of Mifare 4k blocks  
                                                                including the sector trailer*/
#define PH_FRINFC_MIFARESTD_FLAG1                         1 /*!< Flag to set 1 */
#define PH_FRINFC_MIFARESTD_FLAG0                         0 /*!< Flag to set 0 */
#define PH_FRINFC_MIFARESTD_INC_1                         1 /*!< increment by 1 */
#define PH_FRINFC_MIFARESTD_INC_2                         2 /*!< increment by 2 */
#define PH_FRINFC_MIFARESTD_INC_3                         3 /*!< increment by 3 */
#define PH_FRINFC_MIFARESTD_INC_4                         4 /*!< increment by 4 */
#define PH_FRINFC_MIFARESTD_VAL0                          0 /*!< Value initialised to 0 */
#define PH_FRINFC_MIFARESTD_VAL1                          1 /*!< Value initialised to 1 */
#define PH_FRINFC_MIFARESTD_VAL2                          2 /*!< Value initialised to 2 */
#define PH_FRINFC_MIFARESTD_VAL3                          3 /*!< Value initialised to 3 */
#define PH_FRINFC_MIFARESTD_VAL4                          4 /*!< Value initialised to 4 */
#define PH_FRINFC_MIFARESTD_VAL5                          5 /*!< Value initialised to 5 */
#define PH_FRINFC_MIFARESTD_VAL6                          6 /*!< Value initialised to 6 */
#define PH_FRINFC_MIFARESTD_VAL7                          7 /*!< Value initialised to 7 */
#define PH_FRINFC_MIFARESTD_VAL8                          8 /*!< Value initialised to 8 */
#define PH_FRINFC_MIFARESTD_VAL9                          9 /*!< Value initialised to 9 */
#define PH_FRINFC_MIFARESTD_VAL10                         10 /*!< Value initialised to 10 */
#define PH_FRINFC_MIFARESTD_VAL11                         11 /*!< Value initialised to 11 */
#define PH_FRINFC_MIFARESTD_VAL12                         12 /*!< Value initialised to 12 */
#define PH_FRINFC_MIFARESTD_VAL13                         13 /*!< Value initialised to 13 */
#define PH_FRINFC_MIFARESTD_VAL14                         14 /*!< Value initialised to 14 */
#define PH_FRINFC_MIFARESTD_VAL15                         15 /*!< Value initialised to 15 */
#define PH_FRINFC_MIFARESTD_VAL16                         16 /*!< Value initialised to 16 */
#define PH_FRINFC_MIFARESTD_NDEFTLV_L                     0xFF /*!< Length of the TLV */
#define PH_FRINFC_MIFARESTD_NDEFTLV_T                     0x03 /*!< Length of the TLV */
#define PH_FRINFC_MIFARESTD_NDEFTLV_L0                    0x00 /*!< Length of the TLV */
#define PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES0               0 /*!< Number of bytes taken by length (L) of the TLV */
#define PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES1               1 /*!< Number of bytes taken by length (L) of the TLV */
#define PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES2               2 /*!< Number of bytes taken by length (L) of the TLV */
#define PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES3               3 /*!< Number of bytes taken by length (L) of the TLV */
#define PH_FRINFC_MIFARESTD_PROPTLV_T                     0xFD /*!< Type of Proprietary TLV */
#define PH_FRINFC_MIFARESTD_TERMTLV_T                     0xFE /*!< Type of Terminator TLV */
#define PH_FRINFC_MIFARESTD_NULLTLV_T                     0x00 /*!< Type of NULL TLV */
#define PH_FRINFC_MIFARESTD_LEFTSHIFT8                    8 /*!< Left shift by 8 */
#define PH_FRINFC_MIFARESTD_RIGHTSHIFT8                   8 /*!< Right shift by 8 */
#define PH_FRINFC_MIFARESTD_MASK_FF                       0xFF /*!< Mask 0xFF */
#define PH_FRINFC_MIFARESTD_MASK_GPB_WR                   0x03 /*!< Mask 0x03 for GPB byte */
#define PH_FRINFC_MIFARESTD_MASK_GPB_RD                   0x0C /*!< Mask 0xOC for GPB byte */
#define PH_FRINFC_MIFARESTD_GPB_RD_WR_VAL                 0x00 /*!< GPB Read Write value */
#define PH_FRINFC_MIFARESTD_KEY_LEN                       0x06 /*!< MIFARE Std key length */

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
NFCSTATUS phFriNfc_MifareStdMap_H_Reset(  phFriNfc_NdefMap_t        *NdefMap);



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
 * \retval NFCSTATUS_EOF_NDEF_CONTAINER_REACHED              No Space in the File to read.
 * \retval NFCSTATUS_MORE_INFORMATION              There are more bytes to read in the card.
 * \retval NFCSTATUS_SUCCESS                       Last Byte of the card read.
 * \retval NFCSTATUS_INVALID_DEVICE                The device has not been opened or has been disconnected
 *                                                 meanwhile.
 * \retval NFCSTATUS_CMD_ABORTED                   The caller/driver has aborted the request.
 * \retval NFCSTATUS_BUFFER_TOO_SMALL              The buffer provided by the caller is too small.
 * \retval NFCSTATUS_RF_TIMEOUT                    No data has been received within the TIMEOUT period.
 *
 */
NFCSTATUS phFriNfc_MifareStdMap_RdNdef( phFriNfc_NdefMap_t  *NdefMap,
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

NFCSTATUS phFriNfc_MifareStdMap_WrNdef( phFriNfc_NdefMap_t  *NdefMap,
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
NFCSTATUS phFriNfc_MifareStdMap_ChkNdef(phFriNfc_NdefMap_t      *NdefMap);

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
void phFriNfc_MifareStdMap_Process( void       *Context,
                                    NFCSTATUS   Status);

/*!
 *
 * The function Convert the Mifare card to ReadOnly.
 *
 * \param[in] NdefMap Pointer to a valid instance of the \ref phFriNfc_NdefMap_t structure describing
 *                    the component context.
 *
 * \retval NFCSTATUS_PENDING               The action has been successfully triggered.
 * \retval NFCSTATUS_INVALID_PARAMETER     At least one parameter of the function is invalid.
 * \retval NFCSTATUS_INVALID_STATE         The mifare card should be in read_write state to convert card to
 *					   readonly.If any other state the function return NFCSTATUS_INVALID_STATE
 *\retval NFCSTATUS_NOT_ALLOWED			   If card is already in read_only state or initialized state
 *
 */
NFCSTATUS phFriNfc_MifareStdMap_ConvertToReadOnly(phFriNfc_NdefMap_t      *NdefMap, const uint8_t *ScrtKeyB);

#endif /* PHFRINFC_MIFARESTDMAP_H */
