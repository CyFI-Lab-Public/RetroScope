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
* \file  phHciNfc_LinkMgmt.h                                                  *
* \brief HCI Header for the Link Management Gate.                             *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Tue Mar 30 09:32:13 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.5 $                                                            *
* $Aliases: NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $  
*                                                                             *
* =========================================================================== *
*/


#ifndef PHHCINFC_LINKMGMT_H
#define PHHCINFC_LINKMGMT_H

/*@}*/


/**
 *  \name HCI
 *
 * File: \ref phHciNfc_LinkMgmt.h
 *
 */
/*@{*/
#define PHHCINFC_LINK_MGMT_FILEREVISION "$Revision: 1.5 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_LINK_MGMT_FILEALIASES  "$Aliases: NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"   /**< \ingroup grp_file_attributes */
/*@}*/

/*
***************************** Header File Inclusion ****************************
*/

#include <phHciNfc_Generic.h>

/*
****************************** Macro Definitions *******************************
*/

/*
******************** Enumeration and Structure Definition **********************
*/



/*
*********************** Function Prototype Declaration *************************
*/

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_LinkMgmt_Initialise function creates and the opens Link 
 *  Management Gate 
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           Link Mgmt Gate Initialisation is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */
extern
NFCSTATUS
phHciNfc_LinkMgmt_Initialise(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                         );
/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_LinkMgmt_Release function closes the opened pipes between 
 *  the Host Controller Device and the NFC Device.
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           Release of the Link Management gate 
 *                                      resources are pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */
extern
NFCSTATUS
phHciNfc_LinkMgmt_Release(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                     );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_LinkMgmt_Open function opens Link 
 *  Management Gate 
 *
 *  \param[in]  psHciContext            psHciContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           Link Mgmt Gate open is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
phHciNfc_LinkMgmt_Open(
                                phHciNfc_sContext_t     *psHciContext,
                                void                    *pHwRef
                         );


#endif

