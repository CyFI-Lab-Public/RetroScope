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
* \file  phDnldNfc.h                                                          *
* \brief Download Mgmt Header for the Generic Download Management.            *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Thu Aug 26 15:39:56 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.7 $                                                           *
* $Aliases:  $                    
*                                                                             *
* =========================================================================== *
*/


/*@{*/

#ifndef PHDNLDNFC_H
#define PHDNLDNFC_H

/*@}*/
/**
 *  \name Download Mgmt
 *
 * File: \ref phDnldNfc.h
 *
 */
/*@{*/
#define PH_DNLDNFC_FILEREVISION "$Revision: 1.7 $" /**< \ingroup grp_file_attributes */
#define PH_DNLDNFC_FILEALIASES  "$Aliases:  $"     /**< \ingroup grp_file_attributes */
/*@}*/

/*
################################################################################
***************************** Header File Inclusion ****************************
################################################################################
*/

#include <phNfcStatus.h>
#include <phNfcInterface.h>

/*
################################################################################
****************************** Macro Definitions *******************************
################################################################################
*/


/*
################################################################################
******************** Enumeration and Structure Definition **********************
################################################################################
*/

#ifndef NXP_FW_PARAM
extern const uint8_t *nxp_nfc_fw;
#endif /* NXP_FW_PARAM */




/*
################################################################################
*********************** Function Prototype Declaration *************************
################################################################################
*/

/**
 * \ingroup grp_hci_nfc
 *
 *  The phDnldNfc_Upgrade function Upgrades the firmware of
 *  connected NFC Device with the data provided.
 *
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  pHalNotify              Upper layer Notification function
 *                                      pointer.
 *  \param[in]  psContext               psContext is the context of
 *                                      the Upper Layer.
 *
 *  \retval NFCSTATUS_PENDING           Upgrade of Download Layer is in Progress.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

 extern
 NFCSTATUS
 phDnldNfc_Upgrade (
                        phHal_sHwReference_t            *pHwRef,
#ifdef NXP_FW_PARAM
                        uint8_t                         *nxp_nfc_fw,
                        uint32_t                         fw_length,
#endif
                        pphNfcIF_Notification_CB_t      upgrade_complete,
                        void                            *context
                 );


#if  !defined (NXP_FW_INTEGRITY_VERIFY)

extern
NFCSTATUS
phDnldNfc_Run_Check(
                        phHal_sHwReference_t    *pHwRef
#ifdef NXP_FW_PARAM
                        ,uint8_t                 *nxp_nfc_fw
                        uint32_t                  fw_length
#endif
                   );
#endif /* #if  !defined (NXP_FW_INTEGRITY_VERIFY) */


#endif /* PHDNLDNFC_H */


