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
 * \file  phFriNfc_IntNdefMap.h
 * \brief NFC Internal Ndef Mapping File.
 *
 * Project: NFC-FRI
 *
 * $Date: Mon Sep 15 15:10:49 2008 $
 * $Author: ing08205 $
 * $Revision: 1.5 $
 * $Aliases: NFC_FRI1.1_WK838_R9_PREP2,NFC_FRI1.1_WK838_R9_1,NFC_FRI1.1_WK840_R10_PREP1,NFC_FRI1.1_WK840_R10_1,NFC_FRI1.1_WK842_R11_PREP1,NFC_FRI1.1_WK842_R11_PREP2,NFC_FRI1.1_WK842_R11_1,NFC_FRI1.1_WK844_PREP1,NFC_FRI1.1_WK844_R12_1,NFC_FRI1.1_WK846_PREP1,NFC_FRI1.1_WK846_R13_1,NFC_FRI1.1_WK848_PREP1,NFC_FRI1.1_WK848_R14_1,NFC_FRI1.1_WK850_PACK1,NFC_FRI1.1_WK851_PREP1,NFC_FRI1.1_WK850_R15_1,NFC_FRI1.1_WK902_PREP1,NFC_FRI1.1_WK902_R16_1,NFC_FRI1.1_WK904_PREP1,NFC_FRI1.1_WK904_R17_1,NFC_FRI1.1_WK906_R18_1,NFC_FRI1.1_WK908_PREP1,NFC_FRI1.1_WK908_R19_1,NFC_FRI1.1_WK910_PREP1,NFC_FRI1.1_WK910_R20_1,NFC_FRI1.1_WK912_PREP1,NFC_FRI1.1_WK912_R21_1,NFC_FRI1.1_WK914_PREP1,NFC_FRI1.1_WK914_R22_1,NFC_FRI1.1_WK914_R22_2,NFC_FRI1.1_WK916_R23_1,NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
 *
 */

#ifndef PHFRINFC_INTNDEFMAP_H
#define PHFRINFC_INTNDEFMAP_H

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
 * \name phFriNfc_IntNdefMap.h
 *       This file has functions which are used common across all the
         typ1/type2/type3/type4 tags.
 *
 */
/*@{*/

#define PH_FRINFC_NDEFMAP_TLVLEN_ZERO           0

/* NFC Device Major and Minor Version numbers*/
/* !!CAUTION!! these needs to be updated periodically.Major and Minor version numbers
   should be compatible to the version number of currently implemented mapping document.
    Example : NFC Device version Number : 1.0 , specifies
              Major VNo is 1,
              Minor VNo is 0 */ 
#define PH_NFCFRI_NDEFMAP_NFCDEV_MAJOR_VER_NUM             0x01 
#define PH_NFCFRI_NDEFMAP_NFCDEV_MINOR_VER_NUM             0x00 

/* Macros to find major and minor TAG : Ex:Type1/Type2/Type3/Type4 version numbers*/
#define PH_NFCFRI_NDEFMAP_GET_MAJOR_TAG_VERNO(a)           (((a) & (0xf0))>>(4))
#define PH_NFCFRI_NDEFMAP_GET_MINOR_TAG_VERNO(a)           ((a) & (0x0f))


/*!
 * \name NDEF Mapping - states of the Finite State machine
 *
 */
/*@{*/

NFCSTATUS phFriNfc_NdefMap_CheckSpecVersion(phFriNfc_NdefMap_t *NdefMap,
                                            uint8_t             VersionIndex);

NFCSTATUS phFriNfc_NdefMap_SetCardState(phFriNfc_NdefMap_t      *NdefMap,
                                        uint16_t                Length);

#endif /* PHFRINFC_INTNDEFMAP_H */
