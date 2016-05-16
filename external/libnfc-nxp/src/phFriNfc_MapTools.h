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
 * \file  phFriNfc_MapTools.h
 * \brief NFC Internal Ndef Mapping File.
 *
 * Project: NFC-FRI
 *
 * $Date: Fri Oct 15 13:50:54 2010 $
 * $Author: ing02260 $
 * $Revision: 1.6 $
 * $Aliases:  $
 *
 */

#ifndef PHFRINFC_MAPTOOLS_H
#define PHFRINFC_MAPTOOLS_H

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
 * \name phFriNfc_MapTools.h
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
#ifdef DESFIRE_EV1
#define PH_NFCFRI_NDEFMAP_NFCDEV_MAJOR_VER_NUM_2           0x02
#endif /* */
#define PH_NFCFRI_NDEFMAP_NFCDEV_MINOR_VER_NUM             0x00

/* Macros to find major and minor TAG : Ex:Type1/Type2/Type3/Type4 version numbers*/
#define PH_NFCFRI_NDEFMAP_GET_MAJOR_TAG_VERNO(a)           (((a) & (0xf0))>>(4))
#define PH_NFCFRI_NDEFMAP_GET_MINOR_TAG_VERNO(a)           ((a) & (0x0f))

/* NFC Device Major and Minor Version numbers*/
/* !!CAUTION!! these needs to be updated periodically.Major and Minor version numbers
   should be compatible to the version number of currently implemented mapping document.
    Example : NFC Device version Number : 1.0 , specifies
              Major VNo is 1,
              Minor VNo is 0 */
#define PH_NFCFRI_MFSTDMAP_NFCDEV_MAJOR_VER_NUM             0x40
#define PH_NFCFRI_MFSTDMAP_NFCDEV_MINOR_VER_NUM             0x00

/* Macros to find major and minor TAG : Ex:Type1/Type2/Type3/Type4 version numbers*/
#define PH_NFCFRI_MFSTDMAP_GET_MAJOR_TAG_VERNO(a)           ((a) & (0x40)) // must be 0xC0
#define PH_NFCFRI_MFSTDMAP_GET_MINOR_TAG_VERNO(a)           ((a) & (0x30))

/*!
 * \name NDEF Mapping - states of the Finite State machine
 *
 */
/*@{*/


NFCSTATUS   phFriNfc_MapTool_ChkSpcVer( const phFriNfc_NdefMap_t  *NdefMap,
                                        uint8_t             VersionIndex);

NFCSTATUS phFriNfc_MapTool_SetCardState(phFriNfc_NdefMap_t  *NdefMap,
                                        uint32_t            Length);

#endif //PHFRINFC_MAPTOOLS_H
