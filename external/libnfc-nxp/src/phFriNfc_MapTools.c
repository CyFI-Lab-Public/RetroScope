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
 * \file  phFriNfc_MapTools.c
 * \brief NFC Ndef Internal Mapping File.
 *
 * Project: NFC-FRI
 *
 * $Date: Fri Oct 15 13:50:54 2010 $
 * $Author: ing02260 $
 * $Revision: 1.6 $
 * $Aliases:  $
 *
 */

#include <phFriNfc_NdefMap.h>
#include <phFriNfc_MapTools.h>

#ifndef PH_FRINFC_MAP_MIFAREUL_DISABLED
#include <phFriNfc_MifareULMap.h>
#endif  /* PH_FRINFC_MAP_MIFAREUL_DISABLED*/

#ifndef PH_FRINFC_MAP_MIFARESTD_DISABLED
#include <phFriNfc_MifareStdMap.h>
#endif  /* PH_FRINFC_MAP_MIFARESTD_DISABLED */

#ifndef PH_FRINFC_MAP_DESFIRE_DISABLED
#include <phFriNfc_DesfireMap.h>
#endif  /* PH_FRINFC_MAP_DESFIRE_DISABLED */

#ifndef PH_FRINFC_MAP_FELICA_DISABLED
#include <phFriNfc_FelicaMap.h>
#endif  /* PH_FRINFC_MAP_FELICA_DISABLED */

#include <phFriNfc_OvrHal.h>

/*! \ingroup grp_file_attributes
 *  \name NDEF Mapping
 *
 * File: \ref phFriNfc_MapTools.c
 *       This file has functions which are used common across all the
 *       typ1/type2/type3/type4 tags.
 *
 */
/*@{*/
#define PHFRINFCNDEFMAP_FILEREVISION "$Revision: 1.6 $"
#define PHFRINFCNDEFMAP_FILEALIASES  "$Aliases:  $"
/*@}*/

NFCSTATUS phFriNfc_MapTool_SetCardState(phFriNfc_NdefMap_t  *NdefMap,
                                        uint32_t            Length)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    if(Length == PH_FRINFC_NDEFMAP_MFUL_VAL0)
    {
        /* As the NDEF LEN / TLV Len is Zero, irrespective of any state the card
           shall be set to INITIALIZED STATE*/
        NdefMap->CardState =(uint8_t) (((NdefMap->CardState ==
                                PH_NDEFMAP_CARD_STATE_READ_ONLY) ||
                                (NdefMap->CardState ==
                                PH_NDEFMAP_CARD_STATE_INVALID))?
                                PH_NDEFMAP_CARD_STATE_INVALID:
                                PH_NDEFMAP_CARD_STATE_INITIALIZED);
    }
    else
    {
        switch(NdefMap->CardState)
        {
            case PH_NDEFMAP_CARD_STATE_INITIALIZED:
                NdefMap->CardState =(uint8_t) ((NdefMap->CardState ==
                    PH_NDEFMAP_CARD_STATE_INVALID)?
                    NdefMap->CardState:
                    PH_NDEFMAP_CARD_STATE_READ_WRITE);
            break;

            case PH_NDEFMAP_CARD_STATE_READ_ONLY:
                NdefMap->CardState = (uint8_t)((NdefMap->CardState ==
                    PH_NDEFMAP_CARD_STATE_INVALID)?
                    NdefMap->CardState:
                    PH_NDEFMAP_CARD_STATE_READ_ONLY);
            break;

            case PH_NDEFMAP_CARD_STATE_READ_WRITE:
                NdefMap->CardState = (uint8_t)((NdefMap->CardState ==
                    PH_NDEFMAP_CARD_STATE_INVALID)?
                    NdefMap->CardState:
                    PH_NDEFMAP_CARD_STATE_READ_WRITE);
                if (NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD ||
                    NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD)
                {
                    if(NdefMap->StdMifareContainer.ReadOnlySectorIndex &&
                       NdefMap->StdMifareContainer.SectorTrailerBlockNo == NdefMap->StdMifareContainer.currentBlock )
                    {
                        NdefMap->CardState = (uint8_t)((NdefMap->CardState ==
                                                        PH_NDEFMAP_CARD_STATE_INVALID)?
                                                        NdefMap->CardState:
                                                        PH_NDEFMAP_CARD_STATE_READ_ONLY);
                    }
                }
            break;

            default:
                NdefMap->CardState = PH_NDEFMAP_CARD_STATE_INVALID;
                Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                            NFCSTATUS_NO_NDEF_SUPPORT);
            break;
        }
    }
    Result = ((NdefMap->CardState ==
                PH_NDEFMAP_CARD_STATE_INVALID)?
                PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                NFCSTATUS_NO_NDEF_SUPPORT):
                Result);
    return Result;
}

/*  To check mapping spec version */

NFCSTATUS   phFriNfc_MapTool_ChkSpcVer( const phFriNfc_NdefMap_t  *NdefMap,
                                        uint8_t             VersionIndex)
{
    NFCSTATUS status = NFCSTATUS_SUCCESS;

    uint8_t TagVerNo = NdefMap->SendRecvBuf[VersionIndex];

    if ( TagVerNo == 0 )
    {
        /*Return Status Error “ Invalid Format”*/
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,NFCSTATUS_INVALID_FORMAT);
    }
    else
    {
        switch (NdefMap->CardType)
        {
            case PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD:
            case PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD:
            {
                /* calculate the major and minor version number of Mifare std version number */
                status = (( (( PH_NFCFRI_MFSTDMAP_NFCDEV_MAJOR_VER_NUM ==
                                PH_NFCFRI_MFSTDMAP_GET_MAJOR_TAG_VERNO(TagVerNo ) )&&
                            ( PH_NFCFRI_MFSTDMAP_NFCDEV_MINOR_VER_NUM ==
                                PH_NFCFRI_MFSTDMAP_GET_MINOR_TAG_VERNO(TagVerNo))) ||
                            (( PH_NFCFRI_MFSTDMAP_NFCDEV_MAJOR_VER_NUM ==
                                PH_NFCFRI_MFSTDMAP_GET_MAJOR_TAG_VERNO(TagVerNo ) )&&
                            ( PH_NFCFRI_MFSTDMAP_NFCDEV_MINOR_VER_NUM <
                                PH_NFCFRI_MFSTDMAP_GET_MINOR_TAG_VERNO(TagVerNo) )))?
                        NFCSTATUS_SUCCESS:
                        PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_FORMAT));
                break;
            }

#ifdef DESFIRE_EV1
            case PH_FRINFC_NDEFMAP_ISO14443_4A_CARD_EV1:
            {
                /* calculate the major and minor version number of T3VerNo */
                if( (( PH_NFCFRI_NDEFMAP_NFCDEV_MAJOR_VER_NUM_2 ==
                        PH_NFCFRI_NDEFMAP_GET_MAJOR_TAG_VERNO(TagVerNo ) )&&
                    ( PH_NFCFRI_NDEFMAP_NFCDEV_MINOR_VER_NUM ==
                        PH_NFCFRI_NDEFMAP_GET_MINOR_TAG_VERNO(TagVerNo))) ||
                    (( PH_NFCFRI_NDEFMAP_NFCDEV_MAJOR_VER_NUM_2 ==
                        PH_NFCFRI_NDEFMAP_GET_MAJOR_TAG_VERNO(TagVerNo ) )&&
                    ( PH_NFCFRI_NDEFMAP_NFCDEV_MINOR_VER_NUM <
                        PH_NFCFRI_NDEFMAP_GET_MINOR_TAG_VERNO(TagVerNo) )))
                {
                    status = PHNFCSTVAL(CID_NFC_NONE,NFCSTATUS_SUCCESS);
                }
                else
                {
                    if (( PH_NFCFRI_NDEFMAP_NFCDEV_MAJOR_VER_NUM_2 <
                            PH_NFCFRI_NDEFMAP_GET_MAJOR_TAG_VERNO(TagVerNo) ) ||
                    ( PH_NFCFRI_NDEFMAP_NFCDEV_MAJOR_VER_NUM_2 >
                            PH_NFCFRI_NDEFMAP_GET_MAJOR_TAG_VERNO(TagVerNo)))
                    {
                        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,NFCSTATUS_INVALID_FORMAT);
                    }
                }
                break;
            }
#endif /* #ifdef DESFIRE_EV1 */

            default:
            {
                /* calculate the major and minor version number of T3VerNo */
                if( (( PH_NFCFRI_NDEFMAP_NFCDEV_MAJOR_VER_NUM ==
                        PH_NFCFRI_NDEFMAP_GET_MAJOR_TAG_VERNO(TagVerNo ) )&&
                    ( PH_NFCFRI_NDEFMAP_NFCDEV_MINOR_VER_NUM ==
                        PH_NFCFRI_NDEFMAP_GET_MINOR_TAG_VERNO(TagVerNo))) ||
                    (( PH_NFCFRI_NDEFMAP_NFCDEV_MAJOR_VER_NUM ==
                        PH_NFCFRI_NDEFMAP_GET_MAJOR_TAG_VERNO(TagVerNo ) )&&
                    ( PH_NFCFRI_NDEFMAP_NFCDEV_MINOR_VER_NUM <
                        PH_NFCFRI_NDEFMAP_GET_MINOR_TAG_VERNO(TagVerNo) )))
                {
                    status = PHNFCSTVAL(CID_NFC_NONE,NFCSTATUS_SUCCESS);
                }
                else
                {
                    if (( PH_NFCFRI_NDEFMAP_NFCDEV_MAJOR_VER_NUM <
                            PH_NFCFRI_NDEFMAP_GET_MAJOR_TAG_VERNO(TagVerNo) ) ||
                    ( PH_NFCFRI_NDEFMAP_NFCDEV_MAJOR_VER_NUM >
                            PH_NFCFRI_NDEFMAP_GET_MAJOR_TAG_VERNO(TagVerNo)))
                    {
                        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,NFCSTATUS_INVALID_FORMAT);
                    }
                }
                break;
            }


        }
    }

    return (status);
}
