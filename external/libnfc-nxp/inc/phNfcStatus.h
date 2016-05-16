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

/**
 * \file  phNfcStatus.h
 * \brief NFC Status Values - Function Return Codes
 *
 * Project: NFC MW / HAL
 *
 * $Date: Wed Apr 29 16:28:21 2009 $
 * $Author: ing04880 $
 * $Revision: 1.31 $
 * $Aliases: NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
 *
 */

#ifndef PHNFCSTATUS_H /* */
#define PHNFCSTATUS_H/* */

#ifndef PH_FRINFC_EXCLUDE_FROM_TESTFW /* */

/**
 *  \name NFC Status
 *
 * File: \ref phNfcStatus.h
 *
 *\defgroup grp_retval ERROR Status Codes
 */
/*@{*/
#define PHNFCSTATUS_FILEREVISION "$Revision: 1.31 $" /**< \ingroup grp_file_attributes */
#define PHNFCSTATUS_FILEALIASES  "$Aliases: NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"      /**< \ingroup grp_file_attributes */
/*@}*/

#endif /* PH_FRINFC_EXCLUDE_FROM_TESTFW */

#include <phNfcTypes.h>

/* Internally required by \ref PHNFCSTVAL. */
#define PHNFCSTSHL8                          (8U)
/* Required by \ref PHNFCSTVAL. */
#define PHNFCSTBLOWER                        ((NFCSTATUS)(0x00FFU))

/**
 *  \name NFC Status Composition Macro
 *
 *  This is the macro which must be used to compose status values.
 *
 *  \param[in] phNfcCompID Component ID, as defined in \ref phNfcCompId.h .
 *  \param[in] phNfcStatus Status values, as defined in \ref phNfcStatus.h .
 *
 *  \note The macro is not required for the \ref NFCSTATUS_SUCCESS value. This is the only
 *        return value to be used directly.
 *        For all other values it shall be used in assigment and conditional statements e.g.:
 *        - NFCSTATUS status = PHNFCSTVAL(phNfcCompID, phNfcStatus); ...
 *        - if (status == PHNFCSTVAL(phNfcCompID, phNfcStatus)) ...
 *
 */
#define PHNFCSTVAL(phNfcCompID, phNfcStatus)                                \
            ( ((phNfcStatus) == (NFCSTATUS_SUCCESS)) ? (NFCSTATUS_SUCCESS) :  \
                ( (((NFCSTATUS)(phNfcStatus)) & (PHNFCSTBLOWER)) |          \
                    (((uint16_t)(phNfcCompID)) << (PHNFCSTSHL8)) ) )     /**< \ingroup grp_retval
                                                                              Use this macro for return value composition. */
#define PHNFCSTATUS(phNfcStatus)  ((phNfcStatus) & 0x00FFU)
#define PHNFCCID(phNfcStatus)  (((phNfcStatus) & 0xFF00U)>>8)

/**
 *  \name Status Codes
 *
 *  Generic Status codes for the NFC components. Combined with the Component ID
 *  they build the value (status) returned by each
 *  function. Some Specific E.g.:\n
 *  <UL>
 *      <LI> \ref grp_comp_id "Component ID" -  e.g. 0x10, plus                 </LI>
 *      <LI> status code as listed in this file - e.g. 0x03                     </LI>
 *  </UL>
 *  result in a status value of 0x0003.
 *
 *  \note The \ref grp_comp_id "Component ID" used for return codes
     must not be \ref CID_NFC_NONE
 *        except for the value \ref NFCSTATUS_SUCCESS.
 */
/*@{*/

/** \ingroup grp_retval
    The function indicates successful completion. */
#define NFCSTATUS_SUCCESS                                       (0x0000)

/** \ingroup grp_retval
    At least one paramter could not be properly interpreted. */
#define NFCSTATUS_INVALID_PARAMETER                             (0x0001)

/** \ingroup grp_retval
    The buffer provided by the caller is too small. */
#define NFCSTATUS_BUFFER_TOO_SMALL                              (0x0003)

/** \ingroup grp_retval
    Device specifier/handle value is invalid for the operation. */
#define NFCSTATUS_INVALID_DEVICE                                (0x0006)

/** \ingroup grp_retval
    The function executed successfully but could have returned
    more information than space provided by the caller. */
#define NFCSTATUS_MORE_INFORMATION                              (0x0008)

/** \ingroup grp_retval
    No response from the remote device received: Time-out.*/
#define NFCSTATUS_RF_TIMEOUT                                    (0x0009)

/** \ingroup grp_retval
    RF Error during data transaction with the remote device.*/
#define NFCSTATUS_RF_ERROR                                      (0x000A)

/** \ingroup grp_retval
    Not enough resources (e.g. allocation failed.). */
#define NFCSTATUS_INSUFFICIENT_RESOURCES                        (0x000C)

/** \ingroup grp_retval
    A non-blocking function returns this immediately to indicate
    that an internal operation is in progress. */
#define NFCSTATUS_PENDING                                       (0x000D)

/** \ingroup grp_retval
    A board communication error occurred
    (e.g. configuration went wrong). */
#define NFCSTATUS_BOARD_COMMUNICATION_ERROR                     (0x000F)

/** \ingroup grp_retval
    Invalid State of the particular state machine
 */
#define NFCSTATUS_INVALID_STATE									(0x0011)


/** \ingroup grp_retval
    This Layer is Not initialised, hence initialisation required.
 */
#define NFCSTATUS_NOT_INITIALISED								(0x0031)


/** \ingroup grp_retval
    The Layer is already initialised, hence initialisation repeated.
 */
#define NFCSTATUS_ALREADY_INITIALISED							(0x0032)


/** \ingroup grp_retval
    Feature not supported . */
#define NFCSTATUS_FEATURE_NOT_SUPPORTED                         (0x0033)

/** \ingroup grp_retval
    The Unregistration command has failed because the user wants to unregister on
    an element for which he was not registered*/
#define NFCSTATUS_NOT_REGISTERED								(0x0034)


/** \ingroup grp_retval
    The Registration command has failed because the user wants to register on
    an element for which he is already registered*/
#define NFCSTATUS_ALREADY_REGISTERED							(0x0035)

/** \ingroup grp_retval
    Single Tag with Multiple
	Protocol support detected. */
#define NFCSTATUS_MULTIPLE_PROTOCOLS	 	                    (0x0036)

/** \ingroup grp_retval
    Feature not supported . */
#define NFCSTATUS_MULTIPLE_TAGS	 	                            (0x0037)

/** \ingroup grp_retval
    A DESELECT event has occurred. */
#define NFCSTATUS_DESELECTED                                    (0x0038)

/** \ingroup grp_retval
    A RELEASE event has occurred. */
#define NFCSTATUS_RELEASED                                      (0x0039)

/** \ingroup grp_retval
    The operation is currently not possible or not allowed */
#define NFCSTATUS_NOT_ALLOWED                                   (0x003A)

/** \ingroup grp_retval
     The sytem is busy with the previous operation.    
*/
#define NFCSTATUS_BUSY											(0x006F)


/* NDEF Mapping error codes */

/** \ingroup grp_retval
    The remote device (type) is not valid for this request. */
#define NFCSTATUS_INVALID_REMOTE_DEVICE                         (0x001D)

/** \ingroup grp_retval
    Smart tag functionality not supported */
#define NFCSTATUS_SMART_TAG_FUNC_NOT_SUPPORTED                  (0x0013)

/** \ingroup grp_retval
    Read operation failed */
#define NFCSTATUS_READ_FAILED                                   (0x0014)

/** \ingroup grp_retval
    Write operation failed */
#define NFCSTATUS_WRITE_FAILED                                  (0x0015)

/** \ingroup grp_retval
    Non Ndef Compliant */
#define NFCSTATUS_NO_NDEF_SUPPORT                               (0x0016)

/** \ingroup grp_retval
    Could not proceed further with the write operation: reached card EOF*/
#define NFCSTATUS_EOF_NDEF_CONTAINER_REACHED                    (0x001A)

/** \ingroup grp_retval
    Incorrect number of bytes received from the card*/
#define NFCSTATUS_INVALID_RECEIVE_LENGTH                        (0x001B)

/** \ingroup grp_retval
    The data format/composition is not understood/correct. */
#define NFCSTATUS_INVALID_FORMAT                                (0x001C)


/** \ingroup grp_retval
    There is not sufficient storage available. */
#define NFCSTATUS_INSUFFICIENT_STORAGE                          (0x001F)

/** \ingroup grp_retval
    The Ndef Format procedure has failed. */
#define NFCSTATUS_FORMAT_ERROR                                  (0x0023)


/*
 * Macros Required for FRI Stack
NFCSTATUS_INVALID_PARAMETER		
NFCSTATUS_CMD_ABORTED		
NFCSTATUS_FORMAT_STATUS		
NFSTATUS_SUCCESS		
NFCSTATUS_INVALID_REMOTE_DEVICE		
NFCSTATUS_PENDING		
NFCSTATUS_EOF_NDEF_CONTAINED_REACHED		
NFCSTATUS_NO_NDEF_SUPPORT		
NFCSTATUS_SMART_TAG_FUNC_NOT_SUPPORTED		
NFCSTATUS_READ_FAILED		
NFCSTATUS_WRITE_FAILED		
NFCSTATUS_INVALID_RECEIVE_LENGTH		
NFCSTATUS_BUFFER_TOO_SMALL		
NFCSTATUS_NODE_NOT_FOUND		
*/


/* 0x70 to 0xCF Values are Component Specific Error Values */


/** \ingroup grp_retval
    Max number of Status Codes*/
#define NFCSTATUS_FAILED										(0x00FF)

/*@}*/

#ifdef RFU_STATUS_CODES  /* To Be Removed later */

/** \ingroup grp_retval
    The function/command has been aborted. */
#define NFCSTATUS_CMD_ABORTED                                   (0x0002)

/** \ingroup grp_retval
 *  Repeated call of the connecting function is not allowed.
 *   \if hal
 *     \sa \ref phHalNfc_Connect
 *   \endif */
#define NFCSTATUS_ALREADY_CONNECTED                             (0x0004)

/** \ingroup grp_retval
 *  Calling the polling function is not allowed when remote device
 *  are allready connected.
 *   \if hal
 *     \sa \ref phHalNfc_Poll
 *   \endif */
#define NFCSTATUS_MULTI_POLL_NOT_SUPPORTED                      (0x0005)


/** \ingroup grp_retval
    No target found after poll.*/
#define NFCSTATUS_NO_DEVICE_FOUND                               (0x000A)

/** \ingroup grp_retval
    No target found after poll.*/
#define NFCSTATUS_NO_TARGET_FOUND                               (0x000A)

/** \ingroup grp_retval
    Attempt to disconnect a not connected remote device. */
#define NFCSTATUS_NO_DEVICE_CONNECTED                           (0x000B)

/** \ingroup grp_retval
    External RF field detected. */
#define NFCSTATUS_EXTERNAL_RF_DETECTED                          (0x000E)

/** \ingroup grp_retval
    Message is not allowed by the state machine
    (e.g. configuration went wrong). */
#define NFCSTATUS_MSG_NOT_ALLOWED_BY_FSM                        (0x0010)

/** \ingroup grp_retval
    No access has been granted. */
#define NFCSTATUS_ACCESS_DENIED                                 (0x001E)

/** \ingroup grp_retval
    No registry node matches the specified input data. */
#define NFCSTATUS_NODE_NOT_FOUND                                (0x0017)

/** \ingroup grp_retval
    The current module is busy ; one might retry later */
#define NFCSTATUS_SMX_BAD_STATE									(0x00F0)


/** \ingroup grp_retval
    The Abort mechanism has failed for unexpected reason: user can try again*/
#define NFCSTATUS_ABORT_FAILED									(0x00F2)


/** \ingroup grp_retval
    The Registration command has failed because the user wants to register as target
    on a operating mode not supported*/
#define NFCSTATUS_REG_OPMODE_NOT_SUPPORTED						(0x00F5)


#endif


#endif /* PHNFCSTATUS_H */

