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
 * \file  phFriNfc_Desfire.h
 * \brief NFC Ndef Mapping For Desfire Smart Card.
 *
 * Project: NFC-FRI
 *
 * $Date: Tue Jul 27 08:58:21 2010 $
 * $Author: ing02260 $
 * $Revision: 1.5 $
 * $Aliases:  $
 *
 */

#ifndef PHFRINFC_DESFIREMAP_H
#define PHFRINFC_DESFIREMAP_H

#include <phFriNfc.h>
#ifdef PH_HAL4_ENABLE
#include <phHal4Nfc.h>
#else
#include <phHalNfc.h>
#endif
#include <phNfcTypes.h>
#include <phNfcStatus.h>
#include <phFriNfc_NdefMap.h>



/*!
 * \name Desfire - Standard constants
 *
 */
/*@{*/
#define PH_FRINFC_NDEFMAP_DESF_READ_OP                              2  /*!< Desfire Operation Flag is Read */
#define PH_FRINFC_NDEFMAP_DESF_WRITE_OP                             3  /*!< Desfire Operation Flag is Write */
#define PH_FRINFC_NDEFMAP_DESF_NDEF_CHK_OP                          4  /*!< Desfire Operation Flag is Check Ndef */
#define PH_FRINFC_NDEFMAP_DESF_GET_LEN_OP                           5
#define PH_FRINFC_NDEFMAP_DESF_SET_LEN_OP                           6
#define PH_FRINFC_NDEFMAP_DESF_RESP_OFFSET                          2  /*!< Two Status Flag at the end of the
                                                                            Receive buffer*/
#define PH_FRINFC_NDEFMAP_DESF_CAPDU_SMARTTAG_PKT_SIZE              12 /*!< Send Length for Smart Tag function*/
#define PH_FRINFC_NDEFMAP_DESF_CAPDU_SELECT_FILE_PKT_SIZE           7  /*!< Send Length for Select File function */
#define PH_FRINFC_NDEFMAP_DESF_CAPDU_READ_BIN_PKT_SIZE              5  /*!< Send Length for Reading a Packet */

/*!
 * \name NDEF Mapping - states of the Finite State machine
 *
 */
/*@{*/
#ifdef DESFIRE_EV1
    #define PH_FRINFC_NDEFMAP_DESF_STATE_SELECT_SMART_TAG_EV1       4     /*!< Selection of Smart Tag is going on for Desfire EV1 */
#endif /* #ifdef DESFIRE_EV1 */
#define PH_FRINFC_NDEFMAP_DESF_STATE_SELECT_SMART_TAG               5     /*!< Selection of Smart Tag is going on */
#define PH_FRINFC_NDEFMAP_DESF_STATE_SELECT_FILE                    6     /*!< Selecting a file to read/write */
#define PH_FRINFC_NDEFMAP_DESF_STATE_READ_CAP_CONT                  7     /*!< Reading a capability container */
#define PH_FRINFC_NDEFMAP_DESF_STATE_READ_BIN                       8     /*!< Reading from the card */
#define PH_FRINFC_NDEFMAP_DESF_STATE_UPDATE_BIN_BEGIN               60    /*!< Writing to the card */
#define PH_FRINFC_NDEFMAP_DESF_STATE_UPDATE_BIN_END                 61    /*!< Writing to the card */

#define PH_FRINFC_NDEFMAP_DESF_STATE_CHK_NDEF                       10    /*!< Check Ndef is in progress */
#define PH_FRINFC_NDEFMAP_DESF_TLV_INDEX                            7     /*!< Specifies the index of TLV Structure */
#define PH_FRINFC_NDEFMAP_DESF_NDEF_CNTRL_TLV                       0x04  /*!< Specifies the NDEF File Cntrl TLV */
#define PH_FRINFC_NDEFMAP_DESF_PROP_CNTRL_TLV                       0x05  /*!< Specifies the Propreitary File Cntrl TLV */

/* Following Constants are used to navigate the Capability Container(CC)*/

/*!< Following two indexes represents the CCLEN in CC*/
#define PH_FRINFC_NDEFMAP_DESF_CCLEN_BYTE_FIRST_INDEX               0
#define PH_FRINFC_NDEFMAP_DESF_CCLEN_BYTE_SECOND_INDEX              1

/*!< Specifies the index of the Mapping Version in CC */
#define PH_FRINFC_NDEFMAP_DESF_VER_INDEX                            2

/*!< Following two indexes represents the MLe bytes in CC*/
#define PH_FRINFC_NDEFMAP_DESF_MLE_BYTE_FIRST_INDEX                 3
#define PH_FRINFC_NDEFMAP_DESF_MLE_BYTE_SECOND_INDEX                4

/*!< Following two indexes represents the MLc bytes in CC*/
#define PH_FRINFC_NDEFMAP_DESF_MLC_BYTE_FIRST_INDEX                 5
#define PH_FRINFC_NDEFMAP_DESF_MLC_BYTE_SECOND_INDEX                6

/*!< Specifies the index of the TLV in CC */
#define PH_FRINFC_NDEFMAP_DESF_TLV_INDEX                            7

/*!< Specifies the index of the TLV  length in CC */
#define PH_FRINFC_NDEFMAP_DESF_TLV_LEN_INDEX                        8

/*!< Following two indexes represents the NDEF file identifier in CC*/
#define PH_FRINFC_NDEFMAP_DESF_NDEF_FILEID_BYTE_FIRST_INDEX         9
#define PH_FRINFC_NDEFMAP_DESF_NDEF_FILEID_BYTE_SECOND_INDEX        10

/*!< Following two indexes represents the NDEF file size in CC */
#define PH_FRINFC_NDEFMAP_DESF_NDEF_FILESZ_BYTE_FIRST_INDEX         11
#define PH_FRINFC_NDEFMAP_DESF_NDEF_FILESZ_BYTE_SECOND_INDEX        12

/*!< Specifies the index of the NDEF file READ access byte in CC */
#define PH_FRINFC_NDEFMAP_DESF_NDEF_FILERD_ACCESS_INDEX             13

/*!< Specifies the index of the NDEF file WRITE access byte in CC */
#define PH_FRINFC_NDEFMAP_DESF_NDEF_FILEWR_ACCESS_INDEX             14


/* Macros to find Maximum NDEF File Size*/
#define PH_NFCFRI_NDEFMAP_DESF_NDEF_FILE_SIZE                       (NdefMap->DesfireCapContainer.NdefFileSize - 2)
/* Specifies the size of the NLEN Bytes*/
#define PH_FRINFC_NDEFMAP_DESF_NLEN_SIZE_IN_BYTES                    2


/* Following constants are used with buffer index's*/
#define PH_FRINFC_NDEFMAP_DESF_SW1_INDEX            0
#define PH_FRINFC_NDEFMAP_DESF_SW2_INDEX            1


/* Following constants are used for SW1 SW2 status codes*/
#define PH_FRINFC_NDEFMAP_DESF_RAPDU_SW1_BYTE                    0x90
#define PH_FRINFC_NDEFMAP_DESF_RAPDU_SW2_BYTE                    0x00


/* Following constatnts for shift bytes*/
#define PH_FRINFC_NDEFMAP_DESF_SHL8                             8


#define PH_FRINFC_DESF_GET_VER_CMD                          0x60
#define PH_FRINFC_DESF_NATIVE_CLASS_BYTE                    0x90
#define PH_FRINFC_DESF_NATIVE_OFFSET_P1                     0x00
#define PH_FRINFC_DESF_NATIVE_OFFSET_P2                     0x00
#define PH_FRINFC_DESF_NATIVE_GETVER_RESP                   0xAF
/*!
* \name NDEF Mapping - states of the Finite State machine
*
*/
/*@{*/

typedef enum
{
    PH_FRINFC_DESF_STATE_GET_UID,
    PH_FRINFC_DESF_STATE_GET_SW_VERSION,
    PH_FRINFC_DESF_STATE_GET_HW_VERSION

}phFriNfc_eMapDesfireState;

typedef enum
{
    PH_FRINFC_DESF_IDX_0,
    PH_FRINFC_DESF_IDX_1,
    PH_FRINFC_DESF_IDX_2,
    PH_FRINFC_DESF_IDX_3,
    PH_FRINFC_DESF_IDX_4,
    PH_FRINFC_DESF_IDX_5

}phFriNfc_eMapDesfireId;

#define PH_FRINFC_DESF_ISO_NATIVE_WRAPPER() \
    do \
{\
    NdefMap->SendRecvBuf[PH_FRINFC_DESF_IDX_0] = PH_FRINFC_DESF_NATIVE_CLASS_BYTE;\
    NdefMap->SendRecvBuf[PH_FRINFC_DESF_IDX_2] = PH_FRINFC_DESF_NATIVE_OFFSET_P1;\
    NdefMap->SendRecvBuf[PH_FRINFC_DESF_IDX_3] = PH_FRINFC_DESF_NATIVE_OFFSET_P2;\
    switch(NdefMap->State)\
{\
    case PH_FRINFC_DESF_STATE_GET_HW_VERSION :\
    case PH_FRINFC_DESF_STATE_GET_SW_VERSION :\
    case PH_FRINFC_DESF_STATE_GET_UID :\
    if ( NdefMap->State == PH_FRINFC_DESF_STATE_GET_HW_VERSION  )\
{\
    NdefMap->SendRecvBuf[PH_FRINFC_DESF_IDX_1] = PH_FRINFC_DESF_GET_VER_CMD;\
}\
        else\
{\
    NdefMap->SendRecvBuf[PH_FRINFC_DESF_IDX_1] = 0xAF;\
}\
    NdefMap->SendRecvBuf[PH_FRINFC_DESF_IDX_4] = 0x00;\
    NdefMap->SendLength = PH_FRINFC_DESF_IDX_5;\
    break;\
    default :\
    break;\
}\
} while(0)\





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

NFCSTATUS phFriNfc_Desfire_RdNdef(  phFriNfc_NdefMap_t  *NdefMap,
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

NFCSTATUS phFriNfc_Desfire_WrNdef(  phFriNfc_NdefMap_t  *NdefMap,
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

NFCSTATUS phFriNfc_Desfire_ChkNdef( phFriNfc_NdefMap_t  *NdefMap);

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

void phFriNfc_Desfire_Process(  void        *Context,
                                NFCSTATUS   Status);


#endif /* PHFRINFC_DESFIREMAP_H */

