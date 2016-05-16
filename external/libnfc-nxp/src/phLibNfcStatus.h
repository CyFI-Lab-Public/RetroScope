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
 * \file  phLibNfcStatus.h
 * \brief NFC Status Values - Function Return Codes
 *
 * Project: NFC MW / HAL
 *
 * $Date: Thu Feb 25 19:16:41 2010 $
 * $Author: ing07385 $
 * $Revision: 1.24 $
 * $Aliases: NFC_FRI1.1_WK1008_SDK,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1007_SDK,NFC_FRI1.1_WK1014_SDK,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1019_SDK,NFC_FRI1.1_WK1024_SDK $
 *
 */
#ifndef PHLIBNFCSTATUS_H /* */
#define PHLIBNFCSTATUS_H/* */

#include <phNfcStatus.h>

#define LLCP_CHANGES
#define LLCP_TRANSACT_CHANGES

#ifdef LLCP_TRANSACT_CHANGES
/* These two macros are defined due to non availibity of the below macros in header files
#define PHFRINFC_LLCP_STATE_RESET_INIT               0   // \internal Initial state
#define PHFRINFC_LLCP_STATE_CHECKED                  1   // \internal The tag has been checked for LLCP compliance
*/
#define LLCP_STATE_RESET_INIT                       0x00U
#define LLCP_STATE_CHECKED                          0x01U


#endif /* #ifdef LLCP_TRANSACT_CHANGES */
#define LIB_NFC_VERSION_SET(v,major,minor,patch,build) ((v) = \
                                    ( ((major) << 24) & 0xFF000000 ) | \
                                    ( ((minor) << 16) & 0x00FF0000 ) | \
                                    ( ((patch) << 8) & 0x0000FF00 ) | \
                                    ( (build) & 0x000000FF ) )

#define NFCSTATUS_SHUTDOWN                  (0x0091)
#define NFCSTATUS_TARGET_LOST               (0x0092)
#define NFCSTATUS_REJECTED                  (0x0093)
#define NFCSTATUS_TARGET_NOT_CONNECTED      (0x0094) 
#define NFCSTATUS_INVALID_HANDLE            (0x0095)
#define NFCSTATUS_ABORTED                   (0x0096)
#define NFCSTATUS_COMMAND_NOT_SUPPORTED     (0x0097)
#define NFCSTATUS_NON_NDEF_COMPLIANT        (0x0098)
#define NFCSTATUS_OK                        (0x0000)

#ifndef NFCSTATUS_NOT_ENOUGH_MEMORY
#define NFCSTATUS_NOT_ENOUGH_MEMORY         (0x001F)
#endif


#endif /* PHNFCSTATUS_H */



