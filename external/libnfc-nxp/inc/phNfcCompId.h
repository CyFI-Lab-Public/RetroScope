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
 * \file phNfcCompId.h
 * \brief NFC Component ID Values - Used for Function Return Codes
 *
 * Project: NFC MW / HAL
 *
 * $Date: Thu Mar 12 12:00:30 2009 $
 * $Author: ing04880 $
 * $Revision: 1.6 $
 * $Aliases: NFC_FRI1.1_WK912_PREP1,NFC_FRI1.1_WK912_R21_1,NFC_FRI1.1_WK914_PREP1,NFC_FRI1.1_WK914_R22_1,NFC_FRI1.1_WK914_R22_2,NFC_FRI1.1_WK916_R23_1,NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
 *
 */

#ifndef PHNFCCOMPID_H  /* */
#define PHNFCCOMPID_H  /* */

#ifndef PH_FRINFC_EXCLUDE_FROM_TESTFW /* */

/**
 *  \name NFC Comp. ID
 *
 * File: \ref phNfcCompId.h
 *
 */
/*@{*/
#define PHNFCCOMPID_FILEREVISION "$Revision: 1.6 $" /**< \ingroup grp_file_attributes */
#define PHNFCCOMPID_FILEALIASES  "$Aliases: NFC_FRI1.1_WK912_PREP1,NFC_FRI1.1_WK912_R21_1,NFC_FRI1.1_WK914_PREP1,NFC_FRI1.1_WK914_R22_1,NFC_FRI1.1_WK914_R22_2,NFC_FRI1.1_WK916_R23_1,NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"     /**< \ingroup grp_file_attributes */
/*@}*/

#endif /* PH_FRINFC_EXCLUDE_FROM_TESTFW */

/**
 *  \ingroup grp_comp_id
 *
 *  \name Component IDs
 *
 *  IDs for all NFC components. Combined with the Status Code they build the value (status)
 *  returned by each function.
 *
 *  ID Number Spaces:
 *  - 01..1F: HAL
 *  - 20..3F: NFC-MW (Local Device)
 *  - 40..5F: NFC-MW (Remote Device)
 *  .
 *
 *  \note The value \ref CID_NFC_NONE does not exist for Component IDs. Do not use this value except
 *         for \ref NFCSTATUS_SUCCESS. The enumeration function uses \ref CID_NFC_NONE
 *         to mark unassigned \ref phHal_sHwReference_t "Hardware References".
 *
 *  \if hal
 *   \sa \ref phHalNfc_Enumerate
 *  \endif
 */
/*@{*/
#define CID_NFC_NONE                    0x00    /**< \ingroup grp_comp_id
                                                     Unassigned or doesn't apply (see \ref NFCSTATUS_SUCCESS). */
#define CID_NFC_DAL                     0x01    /**< \ingroup grp_comp_id
                                                     Driver Abstraction Layer
                                                     \if hal (\ref grp_subcomponents) \endif .   */
#define CID_NFC_LLC                     0x07    /**< \ingroup grp_comp_id
                                                     Logical Link Control Layer
                                                     \if hal (\ref grp_subcomponents) \endif . */
#define CID_NFC_HCI                     0x08    /**< \ingroup grp_comp_id
                                                     Host Control Interface Layer 
                                                     \if hal (\ref grp_subcomponents) \endif . */
#define CID_NFC_DNLD                    0x09    /**< \ingroup grp_comp_id
                                                     Firmware Download Management Layer
                                                     \if hal (\ref grp_subcomponents) \endif . */
#define CID_NFC_HAL                     0x10    /**< \ingroup grp_comp_id
                                                     Hardware Abstraction Layer \if hal (\ref grp_hal_common) \endif . */
#define CID_FRI_NFC_OVR_HAL             0x20    /**< \ingroup grp_comp_id
                                                     NFC-Device, HAL-based. */
#define CID_FRI_NFC_NDEF_RECORD         0x22    /**< \ingroup grp_comp_id
                                                     NDEF Record Tools Library. */
#define CID_FRI_NFC_NDEF_MAP            0x23    /**< \ingroup grp_comp_id
                                                     NDEF Mapping. */
#define CID_FRI_NFC_NDEF_REGISTRY       0x24    /**< \ingroup grp_comp_id
                                                     NDEF_REGISTRY. */
#define CID_FRI_NFC_AUTO_DEV_DIS        0x25    /**< \ingroup grp_comp_id
                                                     Automatic Device Discovery. */
#define CID_FRI_NFC_NDEF_SMTCRDFMT      0x26    /**< \ingroup grp_comp_id
                                                     Smart Card Formatting */
#define CID_FRI_NFC_LLCP                0x27    /**< \ingroup grp_comp_id
                                                     LLCP Core. */
#define CID_FRI_NFC_LLCP_MAC            0x28    /**< \ingroup grp_comp_id
                                                     LLCP Mac Mappings. */
#define CID_FRI_NFC_LLCP_TRANSPORT      0x29    /**< \ingroup grp_comp_id
                                                     LLCP Transport. */
#define CID_NFC_LIB                     0x30    /**< \ingroup grp_comp_id
                                                     NFC Library Layer \if hal (\ref grp_hal_common) \endif . */
#define CID_MAX_VALUE                   0xF0    /**< \ingroup grp_comp_id
                                                     The maximum CID value that is defined. */

/*@}*/

#endif /* PHNFCCOMPID_H */
