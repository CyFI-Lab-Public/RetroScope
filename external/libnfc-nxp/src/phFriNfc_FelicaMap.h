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
 * \file  phFriNfc_FelicaMap.h
 * \brief NFC Ndef Mapping For Felica Smart Card.
 *
 * Project: NFC-FRI
 *
 * $Date: Wed Apr  8 14:37:05 2009 $
 * $Author: ing02260 $
 * $Revision: 1.4 $
 * $Aliases: NFC_FRI1.1_WK914_R22_1,NFC_FRI1.1_WK914_R22_2,NFC_FRI1.1_WK916_R23_1,NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
 *
 */

#ifndef PHFRINFC_FELICAMAP_H
#define PHFRINFC_FELICAMAP_H

#include <phFriNfc.h>
#if !defined PH_HAL4_ENABLE
#include <phHalNfc.h>
#endif
#include <phNfcStatus.h>
#include <phNfcTypes.h>
#include <phFriNfc_NdefMap.h>


#ifndef PH_FRINFC_EXCLUDE_FROM_TESTFW /* */

#define PH_FRINFC_NDEFMAP_FELICAMAP_FILEREVISION "$Revision: 1.4 $"
#define PH_FRINFC_NDEFMAP_FELLICAMAP_FILEALIASES  "$Aliases: NFC_FRI1.1_WK914_R22_1,NFC_FRI1.1_WK914_R22_2,NFC_FRI1.1_WK916_R23_1,NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"

/* NDEF Mapping - states of the Finite State machine */
#define PH_NFCFRI_NDEFMAP_FELI_STATE_SELECT_WILD_CARD           1 /* Select Wild Card State*/
#define PH_NFCFRI_NDEFMAP_FELI_STATE_SELECT_NDEF_APP            2 /* Select NFC Forum Application State*/
#define PH_FRINFC_NDEFMAP_FELI_STATE_CHK_NDEF                   3 /* Ndef Complient State*/
#define PH_NFCFRI_NDEFMAP_FELI_STATE_RD_ATTR                    4 /* Read Attribute Information State*/
#define PH_NFCFRI_NDEFMAP_FELI_STATE_RD_BLOCK                   5 /* Read Data state*/
#define PH_NFCFRI_NDEFMAP_FELI_STATE_WR_BLOCK                   6 /* Write Data State*/
#define PH_NFCFRI_NDEFMAP_FELI_STATE_ATTR_BLK_WR_BEGIN          7 /* Write Attrib Blk for write Begin*/
#define PH_NFCFRI_NDEFMAP_FELI_STATE_ATTR_BLK_WR_END            8 /* Write Attrib Blk for write End*/
#define PH_NFCFRI_NDEFMAP_FELI_STATE_WR_EMPTY_MSG               9 /* write Empty Ndef Msg*/


#define PH_NFCFRI_NDEFMAP_FELI_WR_RESP_BYTE                     0x09 /* Write Cmd Response Byte*/
#define PH_NFCFRI_NDEFMAP_FELI_RD_RESP_BYTE                     0x07 /* Read Cmd Response Byte*/

#define PH_NFCFRI_NDEFMAP_FELI_NMAXB                            13 /* Nmaxb Identifier*/
#define PH_NFCFRI_NDEFMAP_FELI_NBC                              14 /* Nbc Identifier*/    

#define PH_FRINFC_NDEFMAP_FELI_OP_NONE                          15 /* To Read the attribute information*/
#define PH_FRINFC_NDEFMAP_FELI_WR_ATTR_RD_OP                    16 /* To Read the attribute info. while a WR Operationg*/
#define PH_FRINFC_NDEFMAP_FELI_RD_ATTR_RD_OP                    17 /* To Read the attribute info. while a RD Operationg*/
#define PH_FRINFC_NDEFMAP_FELI_CHK_NDEF_OP                      18 /* To Process the read attribute info. while a ChkNdef Operation*/
#define PH_FRINFC_NDEFMAP_FELI_WR_EMPTY_MSG_OP                  19 /* To Process the Empty NDEF Msg while erasing the NDEF data*/

#define PH_FRINFC_NDEFMAP_FELI_NUM_DEVICE_TO_DETECT             1

#define PH_NFCFRI_NDEFMAP_FELI_RESP_HEADER_LEN                  13 /* To skip response code, IDm, status flgas and Nb*/
#define PH_NFCFRI_NDEFMAP_FELI_VERSION_INDEX                    13 /* Specifies Index of the version in Attribute Resp Buffer*/
#define PH_NFCFRI_NDEFMAP_FELI_PKT_LEN_INDEX                    0 /* Specifies Index of the Packet Length*/


/* To Handle the EOF staus*/
#ifndef TRUE
#define TRUE                                                    1
#endif /* #ifndef TRUE */

#ifndef FALSE
#define FALSE                                                   0
#endif /* #ifndef FALSE */


/* NFC Device Major and Minor Version numbers*/
/* !!CAUTION!! these needs to be updated periodically.Major and Minor version numbers
   should be compatible to the version number of currently implemented mapping document.
    Example : NFC Device version Number : 1.0 , specifies
              Major VNo is 1,
              Minor VNo is 0 */ 
#define PH_NFCFRI_NDEFMAP_FELI_NFCDEV_MAJOR_VER_NUM             0x01 
#define PH_NFCFRI_NDEFMAP_FELI_NFCDEV_MINOR_VER_NUM             0x00 

/* Macros to find major and minor T3T version numbers*/
#define PH_NFCFRI_NDEFMAP_FELI_GET_MAJOR_T3T_VERNO(a)\
do\
{\
    (((a) & (0xf0))>>(4))\
}while (0)

#define PH_NFCFRI_NDEFMAP_FELI_GET_MINOR_T3T_VERNO(a)\
do\
{\
    ((a) & (0x0f))\
}while (0)


/* Macro for LEN Byte Calculation*/
#define PH_NFCFRI_NDEFMAP_FELI_CAL_LEN_BYTES(Byte1,Byte2,Byte3,DataLen)\
do\
{ \
    (DataLen) = (Byte1); \
    (DataLen) = (DataLen) << (16);\
    (DataLen) += (Byte2);\
    (DataLen) = (DataLen) << (8);\
    (DataLen) += (Byte3);\
}while(0)
    

                            

/* Enum for the data write operations*/
typedef enum
{
    FELICA_WRITE_STARTED,
    FELICA_WRITE_ENDED,
    FELICA_EOF_REACHED_WR_WITH_BEGIN_OFFSET,
    FELICA_EOF_REACHED_WR_WITH_CURR_OFFSET,
    FELICA_RD_WR_EOF_CARD_REACHED,
    FELICA_WRITE_EMPTY_MSG
   
}phFriNfc_FelicaError_t;



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
 * \retval NFCSTATUS_SUCCESS                       Last Byte of the card read.
 * \retval NFCSTATUS_INVALID_DEVICE                The device has not been opened or has been disconnected
 *                                                 meanwhile.
 * \retval NFCSTATUS_CMD_ABORTED                   The caller/driver has aborted the request.
 * \retval NFCSTATUS_RF_TIMEOUT                    No data has been received within the TIMEOUT period.
 *
 */

NFCSTATUS phFriNfc_Felica_RdNdef(  phFriNfc_NdefMap_t  *NdefMap,
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
 * \retval NFCSTATUS_RF_TIMEOUT                     No data has been received within the TIMEOUT period.
 *
 */

NFCSTATUS phFriNfc_Felica_WrNdef(  phFriNfc_NdefMap_t  *NdefMap,
                                    uint8_t             *PacketData,
                                    uint32_t            *PacketDataLength,
                                    uint8_t             Offset);

/*!
 * \brief \copydoc page_ovr Initiates Writing of Empty NDEF information to the Remote Device.
 *
 * The function initiates the erasing of NDEF information to a Remote Device.
 * It performs a reset of the state and starts the action (state machine).
 * A periodic call of the \ref phFriNfc_NdefMap_Process has to be done once the action
 * has been triggered.
 *
 * \param[in] NdefMap Pointer to a valid instance of the \ref phFriNfc_NdefMap_t structure describing
 *                    the component context.
 *
 * \retval NFCSTATUS_PENDING                        The action has been successfully triggered.
 * \retval NFCSTATUS_SUCCESS                        Empty msessage is completely written
 *                                                  into the card.
 * \retval NFCSTATUS_INVALID_DEVICE                 The device has not been opened or has been disconnected
 *                                                  meanwhile.
 * \retval NFCSTATUS_CMD_ABORTED                    The caller/driver has aborted the request.
 * \retval NFCSTATUS_RF_TIMEOUT                     No data has been received within the TIMEOUT period.
 *
 */

NFCSTATUS phFriNfc_Felica_EraseNdef(  phFriNfc_NdefMap_t  *NdefMap);
                                

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
 * \retval NFCSTATUS_RF_TIMEOUT             No data has been received within the TIMEOUT period.
 *
 */

NFCSTATUS phFriNfc_Felica_ChkNdef( phFriNfc_NdefMap_t     *NdefMap);

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

void phFriNfc_Felica_Process(void       *Context,
                             NFCSTATUS   Status);


#endif /* PH_FRINFC_EXCLUDE_FROM_TESTFW */


#endif /* PHFRINFC_FELICAMAP_H */


