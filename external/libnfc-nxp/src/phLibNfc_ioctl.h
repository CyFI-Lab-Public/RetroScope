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
*\file  phLibNfc_ioctl.h
*\brief Contains LibNfc IOCTL details.
*Project:   NFC-FRI 1.1
* $Workfile:: phLibNfc_ioctl.h $
* $Modtime::          $
* $Author: ing07299 $
* $Revision: 1.9 $
* $Aliases: NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK949_SDK_INT,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK1003_SDK,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1008_SDK,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1007_SDK,NFC_FRI1.1_WK1014_SDK,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1019_SDK,NFC_FRI1.1_WK1024_SDK $
*\defgroup grp_lib_ioctl IOCTL code details
*/


#ifndef PHLIBNFCIOCTL_H /* */
#define PHLIBNFCIOCTL_H /* */

#include <phNfcIoctlCode.h>
#include<phLibNfc.h>

/**
* \ingroup  grp_lib_ioctl
* \brief   Allows to initiate firmware download to connected PN544
*
*/
#define PHLIBNFC_FW_DOWNLOAD    NFC_FW_DOWNLOAD
/**
* \ingroup  grp_lib_ioctl
* \brief   Allows to read memory from connected PN544 .  
*
*/
#define PHLIBNFC_MEM_READ       NFC_MEM_READ

/**
* \ingroup  grp_lib_ioctl
* \brief   Allows to write PN544 memory.  
*
*/
#define PHLIBNFC_MEM_WRITE      NFC_MEM_WRITE

/**
* \ingroup  grp_lib_ioctl
* \brief   Allows to do Antenna test.  
*
*/
#define	PHLIBNFC_ANTENNA_TEST    DEVMGMT_ANTENNA_TEST			
/**
* \ingroup  grp_lib_ioctl
* \brief   Allows to do SWP test.  
*
*/
#define	PHLIBNFC_SWP_TEST		 DEVMGMT_SWP_TEST								
/**
* \ingroup  grp_lib_ioctl
* \brief   Allows to do PRBS test.  
*
*/
#define	PHLIBNFC_PRBS_TEST       DEVMGMT_PRBS_TEST					

/**
* \ingroup  grp_lib_ioctl
* \brief   Allows to switch UICC mode.  
*
*/
#define	PHLIBNFC_SWITCH_SWP_MODE   NFC_SWITCH_SWP_MODE

typedef struct
{
  void                          *pCliCntx;
  pphLibNfc_IoctlCallback_t     CliRspCb;
  phHal_sHwReference_t          *psHwReference;
  phNfc_sData_t*                pOutParam;
  uint16_t                      IoctlCode;
}phLibNfc_Ioctl_Cntx_t;

#endif /* PHLIBNFCIOCTL_H */




