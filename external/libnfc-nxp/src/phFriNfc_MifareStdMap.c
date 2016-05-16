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
 * \file  phFriNfc_MifareStdMap.c
 * \brief NFC Ndef Mapping For Remote Devices.
 *
 * Project: NFC-FRI
 *
 * $Date: Thu May  6 10:01:55 2010 $
 * $Author: ing02260 $
 * $Revision: 1.22 $
 * $Aliases: NFC_FRI1.1_WK1017_R34_4,NFC_FRI1.1_WK1023_R35_1 $
 *
 */

#ifndef PH_FRINFC_MAP_MIFARESTD_DISABLED

#include <phFriNfc_MifareStdMap.h>
#include <phFriNfc_OvrHal.h>

#ifndef PH_HAL4_ENABLE
#include <phFriNfc_OvrHalCmd.h>
#endif

#include <phFriNfc_MapTools.h>

#include <phFriNfc_MifStdFormat.h>

/*! \ingroup grp_file_attributes
 *  \name NDEF Mapping
 *
 * File: \ref phFriNfcNdefMap.c
 *
 */
/*@{*/
#define PHFRINFCMIFARESTDMAP_FILEREVISION "$Revision: 1.22 $"
#define PHFRINFCMIFARESTDMAP_FILEALIASES  "$Aliases: NFC_FRI1.1_WK1017_R34_4,NFC_FRI1.1_WK1023_R35_1 $"
/*@}*/
/*@}*/

/*!
 * \name Mifare Standard Mapping - Helper Functions
 *
 */
/*@{*/

/*!
 * \brief \copydoc page_ovr Helper function for Mifare Std. This function Reads  
 *  a block from the card.
 */
static NFCSTATUS phFriNfc_MifStd_H_RdABlock(phFriNfc_NdefMap_t *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare Std. This function writes  
 *  into a block of the card.
 */
static NFCSTATUS phFriNfc_MifStd_H_WrABlock(phFriNfc_NdefMap_t *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare Std. This function authenticates  
 *  one sector at a time.
 */
static NFCSTATUS phFriNfc_MifStd_H_AuthSector(phFriNfc_NdefMap_t *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare 4k Check Ndef to 
 *  get the next AID blocks
 */
static NFCSTATUS phFriNfc_MifStd4k_H_CheckNdef(phFriNfc_NdefMap_t *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Check Ndef to store the AIDs
 */
static void phFriNfc_MifStd_H_fillAIDarray(phFriNfc_NdefMap_t *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to get the Sector from the current block
 */
static uint8_t phFriNfc_MifStd_H_GetSect(uint8_t  BlockNumber);

/*!
 * \brief \copydoc page_ovr Helper function to check the Ndef compliance of the  
 *  current block, if the block is not Ndef Compliant, increment the block till 
 *  the next Ndef compliant block using the Get Sector Helper function
 */
static NFCSTATUS phFriNfc_MifStd_H_BlkChk(phFriNfc_NdefMap_t *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to read the access bits of each sector
 */
static NFCSTATUS phFriNfc_MifStd_H_RdAcsBit(phFriNfc_NdefMap_t *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to check the access bits of each sector
 */
static NFCSTATUS phFriNfc_MifStd_H_ChkAcsBit(phFriNfc_NdefMap_t *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for read access bits, depending 
 * on the read/write/check ndef function called
 */
static NFCSTATUS phFriNfc_MifStd_H_ChkRdWr(phFriNfc_NdefMap_t *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for check ndef to check the 
 * ndef compliant sectors
 */
static void phFriNfc_MifStd_H_ChkNdefCmpltSects(phFriNfc_NdefMap_t *NdefMap);

#if 0
/*!
 * \brief \copydoc page_ovr Helper function for read ndef to check the 
 * the length L of the ndef TLV
 */
static uint8_t phFriNfc_MifStd_H_ChkNdefLen(phFriNfc_NdefMap_t *NdefMap,
                                        uint8_t             NDEFlength,
                                        uint8_t             *CRFlag);

/*!
 * \brief \copydoc page_ovr Helper function for read ndef to copy the 
 * card data to the user buffer
 */
static NFCSTATUS phFriNfc_MifStd_H_RdNdefTLV(phFriNfc_NdefMap_t *NdefMap,
                                         uint8_t            *Temp16Bytes);

#endif

/*!
 * \brief \copydoc page_ovr Helper function for read ndef to process the  
 * remaining bytes of length (L) in the TLV
 */
static NFCSTATUS phFriNfc_MifStd_H_RemainTLV(phFriNfc_NdefMap_t *NdefMap,
                                         uint8_t            *Flag,
                                         uint8_t            *Temp16Bytes);

/*!
 * \brief \copydoc page_ovr Helper function for read ndef to process the  
 * internal bytes
 */
static NFCSTATUS phFriNfc_MifStd_H_ChkIntLen(phFriNfc_NdefMap_t *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for read ndef to check the  
 * internal bytes without ndef tlv flag 
 */
static NFCSTATUS phFriNfc_MifStd_H_IntLenWioutNdef(phFriNfc_NdefMap_t  *NdefMap,
                                            uint8_t             *Flag,
                                            uint8_t             *TempintBytes);
#if 0
/*!
 * \brief \copydoc page_ovr Helper function for read ndef to check the  
 * internal bytes without ndef tlv flag 
 */
static NFCSTATUS phFriNfc_MifStd_H_IntLenWithNdef(phFriNfc_NdefMap_t *NdefMap,
                                            uint8_t             *TempintBytes);
#endif
/*!
 * \brief \copydoc page_ovr Helper function for write ndef to add the TLV  
 * structure 
 */
static uint8_t phFriNfc_MifStd_H_UpdateTLV(phFriNfc_NdefMap_t *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for write ndef to write the Length TLV 
 *
 */
static NFCSTATUS phFriNfc_MifStd_H_WriteNdefLen(phFriNfc_NdefMap_t *NdefMap);
#if 0
/*!
 * \brief \copydoc page_ovr Helper function for check ndef to check the ndef 
 * compliant blocks is 0
 */
static NFCSTATUS phFriNfc_MifStd_H_ChkNdefCmpltBlocks(phFriNfc_NdefMap_t   *NdefMap);
#endif
/*!
 * \brief \copydoc page_ovr Helper function to set the authentication flag 
 * for the ndef TLV block
 */
static void phFriNfc_MifStd_H_SetNdefBlkAuth(phFriNfc_NdefMap_t   *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to reset ndef TLV values. This is
 * used when the offset is BEGIN
 */
static void phFriNfc_MifStd_H_RdWrReset(phFriNfc_NdefMap_t   *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to read the first ndef compliant block to 
 * change the length
 */
static NFCSTATUS phFriNfc_MifStd_H_RdtoWrNdefLen(phFriNfc_NdefMap_t *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to get the actual length of card
 *
 */
static NFCSTATUS phFriNfc_MifStd_H_GetActCardLen(phFriNfc_NdefMap_t *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to check all the TLVs 
 *
 */
static NFCSTATUS phFriNfc_MifStd_H_ChkTLVs(phFriNfc_NdefMap_t *NdefMap,
                                           uint8_t            *CRFlag);

/*!
 * \brief \copydoc page_ovr Helper function to get the next TLV
 *
 */
static NFCSTATUS phFriNfc_MifStd_H_GetNxtTLV(phFriNfc_NdefMap_t *NdefMap,
                                       uint16_t             *TempLength,
                                       uint8_t              *TL4bytesFlag);

/*!
 * \brief \copydoc page_ovr Helper function to know whether the read 
 *  16 bytes are parsed completely
 */
static NFCSTATUS phFriNfc_MifStd_H_Chk16Bytes(phFriNfc_NdefMap_t   *NdefMap,
                                       uint16_t             TempLength);

/*!
 * \brief \copydoc page_ovr Helper function to know whether the read 
 *  16 bytes are parsed completely
 */
static NFCSTATUS phFriNfc_MifStd_H_ChkRemainTLVs(phFriNfc_NdefMap_t *NdefMap, 
                                          uint8_t            *CRFlag,
                                          uint8_t            *NDEFFlag);

/*!
 * \brief \copydoc page_ovr Helper function to call the Completion Routine
 *
 */
static void phFriNfc_MifStd_H_Complete(phFriNfc_NdefMap_t  *NdefMap,
                                 NFCSTATUS            Result);

/*!
 * \brief \copydoc page_ovr Helper function to get the Mifare 1k Sector Trailer
 *
 */
static void phFriNfc_MifStd_H_Get1kStTrail(phFriNfc_NdefMap_t  *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to get the Mifare 4k Sector Trailer
 *
 */
static void phFriNfc_MifStd_H_Get4kStTrail(phFriNfc_NdefMap_t  *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to process the check ndef call
 *
 */
static NFCSTATUS phFriNfc_MifStd_H_ProChkNdef(phFriNfc_NdefMap_t        *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to process the authentication of a sector
 *
 */
static NFCSTATUS phFriNfc_MifStd_H_ProAuth(phFriNfc_NdefMap_t       *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to read 16 bytes from a specifed block no.
 *
 */
static NFCSTATUS phFriNfc_MifStd_H_Rd16Bytes(phFriNfc_NdefMap_t     *NdefMap,
                                              uint8_t               BlockNo);

/*!
 * \brief \copydoc page_ovr Helper function to process access bits of the 
 * sector trailer
 */
static NFCSTATUS phFriNfc_MifStd_H_ProAcsBits(phFriNfc_NdefMap_t    *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to check the GPB bytes
 */
static NFCSTATUS phFriNfc_MifStd_H_GPBChk(phFriNfc_NdefMap_t        *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to check for the different 
 * status value in the process because of proprietary forum sector
 */
static NFCSTATUS phFriNfc_MifStd_H_ProStatNotValid(phFriNfc_NdefMap_t    *NdefMap,
                                                   NFCSTATUS             status);

/*!
 * \brief \copydoc page_ovr Helper function to read the NDEF TLV block
 */
static NFCSTATUS phFriNfc_MifStd_H_RdBeforeWr(phFriNfc_NdefMap_t        *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to process the NDEF TLV block 
 *  read bytes to start write from the NDEF TLV
 */
static NFCSTATUS phFriNfc_MifStd_H_ProBytesToWr(phFriNfc_NdefMap_t        *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to fill the send buffer to write
 */
static NFCSTATUS phFriNfc_MifStd_H_fillSendBuf(phFriNfc_NdefMap_t        *NdefMap,
                                               uint8_t                   Length);

/*!
 * \brief \copydoc page_ovr Helper function to write 16 bytes in a block
 */
static NFCSTATUS phFriNfc_MifStd_H_WrTLV(phFriNfc_NdefMap_t        *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to process the write TLV bytes in a block
 */
static NFCSTATUS phFriNfc_MifStd_H_ProWrTLV(phFriNfc_NdefMap_t        *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to update the remaining TLV
 */
static uint8_t phFriNfc_MifStd_H_UpdRemTLV(phFriNfc_NdefMap_t        *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to update the length field if more than one
 * NULL TLVs exists before of the NDEF TLV
 */
static void phFriNfc_MifStd_H_fillTLV1(phFriNfc_NdefMap_t        *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to update the length field if more than one 
 * NULL TLVs does not exists before the TLV
 */
static void phFriNfc_MifStd_H_fillTLV2(phFriNfc_NdefMap_t        *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to increment/decrement the ndef tlv block    
 * and read the block
 */
static NFCSTATUS phFriNfc_MifStd_H_CallWrNdefLen(phFriNfc_NdefMap_t        *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to check the current block is valid or not 
 * if not valid decrement the current block till the valid block
 */
static NFCSTATUS phFriNfc_MifStd_H_BlkChk_1(phFriNfc_NdefMap_t        *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function update the length of the TLV if NULL TLVs
 * greater than or equal to 2
 */
static void phFriNfc_MifStd_H_fillTLV1_1(phFriNfc_NdefMap_t        *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function update the length of the TLV if NULL TLVs
 * less than 2
 */
static void phFriNfc_MifStd_H_fillTLV2_1(phFriNfc_NdefMap_t        *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to read the TLV block
 */
static NFCSTATUS phFriNfc_MifStd_H_RdTLV(phFriNfc_NdefMap_t        *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to process the read TLV block
 */
static NFCSTATUS phFriNfc_MifStd_H_ProRdTLV(phFriNfc_NdefMap_t        *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to write the terminator TLV
 */
static NFCSTATUS phFriNfc_MifStd_H_WrTermTLV(phFriNfc_NdefMap_t   *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to process the write a block function
 */
static NFCSTATUS phFriNfc_MifStd_H_ProWrABlock(phFriNfc_NdefMap_t   *NdefMap);

#ifndef PH_HAL4_ENABLE
/*!
 * \brief \copydoc page_ovr Helper function to call poll function after the 
 * authentication has failed
 */
static NFCSTATUS phFriNfc_MifStd_H_CallPoll(phFriNfc_NdefMap_t   *NdefMap);
#endif

/*!
 * \brief \copydoc page_ovr Helper function to call connect function after the 
 * authentication has failed
 */
static NFCSTATUS phFriNfc_MifStd_H_CallConnect(phFriNfc_NdefMap_t   *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to call disconnect function after the 
 * authentication has failed
 */
static NFCSTATUS phFriNfc_MifStd_H_CallDisCon(phFriNfc_NdefMap_t   *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function to call know the ndef compliant block using 
 */
static void phFriNfc_MifStd1k_H_BlkChk(phFriNfc_NdefMap_t   *NdefMap,
                                    uint8_t              SectorID,
                                    uint8_t              *callbreak);

static uint8_t phFriNfc_MifStd_H_GetSectorTrailerBlkNo(uint8_t SectorID );
static NFCSTATUS phFriNfc_MifStd_H_ProSectorTrailorAcsBits(phFriNfc_NdefMap_t *NdefMap);
static NFCSTATUS phFriNfc_MifStd_H_WrSectorTrailorBlock(phFriNfc_NdefMap_t *NdefMap);
static NFCSTATUS phFriNfc_MifStd_H_ProWrSectorTrailor(phFriNfc_NdefMap_t *NdefMap);

/*@}*/
/**
 * \name Mifare Standard Mapping - Constants.        
 *
 */
/*@{*/
/* As per the spec, the value of this macro 
   PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT1 shall be 0xA0 */
#define PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT1        0xA0 /**< \internal Authenticate Command for MAD Sector */

/* As per the spec, the value of this macro 
   PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT2 shall be 0xA1 */
#define PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT2        0xA1 /**< \internal Authenticate Command for MAD Sector */

/* As per the spec, the value of this macro 
   PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT3 shall be 0xA2 */
#define PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT3        0xA2 /**< \internal Authenticate Command for MAD Sector */

/* As per the spec, the value of this macro 
   PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT4 shall be 0xA3 */
#define PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT4        0xA3 /**< \internal Authenticate Command for MAD Sector */

/* As per the spec, the value of this macro 
   PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT5 shall be 0xA4 */
#define PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT5        0xA4 /**< \internal Authenticate Command for MAD Sector */

/* As per the spec, the value of this macro 
   PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT6 shall be 0xA5 */
#define PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT6        0xA5 /**< \internal Authenticate Command for MAD Sector */

/* As per the spec, the value of this macro 
   PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_NDEFSECT1 shall be 0xD3 */
#define PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_NDEFSECT1       0xD3 /**< \internal Authenticate Command for NDEF Sectors 1 */

/* As per the spec, the value of this macro 
   PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_NDEFSECT2 shall be 0xF7 */
#define PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_NDEFSECT2       0xF7 /**< \internal Authenticate Command for NDEF Sectors 2 */
#define PH_FRINFC_NDEFMAP_MIFARESTD_NDEF_COMPVAL2         0x03 /**< \internal Ndef Compliant command 1 */
#define PH_FRINFC_NDEFMAP_MIFARESTD_NDEF_COMPVAL1         0xE1 /**< \internal Ndef Compliant command 2 */

/* Enable access bits check for the MAD sector 
#define ENABLE_ACS_BIT_CHK_FOR_MAD */
/*@}*/

NFCSTATUS phFriNfc_MifareStdMap_H_Reset(phFriNfc_NdefMap_t        *NdefMap)
{
    NFCSTATUS   status = NFCSTATUS_SUCCESS;
    uint8_t     index = PH_FRINFC_MIFARESTD_VAL0;
    if ( NdefMap == NULL)
    {
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {   
        /* Current Block stores the present block accessed in the card */
        NdefMap->StdMifareContainer.currentBlock = PH_FRINFC_MIFARESTD_VAL0;

        for(index = PH_FRINFC_MIFARESTD_VAL0; index < 
            PH_FRINFC_NDEFMAP_MIFARESTD_ST15_BYTES; index++)
        {
            /* internal buffer to store the odd bytes of length < 15 */
            NdefMap->StdMifareContainer.internalBuf[index] = PH_FRINFC_MIFARESTD_VAL0;
        }
        /* odd bytes length stored in the internal buffer */
        NdefMap->StdMifareContainer.internalLength = PH_FRINFC_MIFARESTD_VAL0;

        NdefMap->CardState = PH_NDEFMAP_CARD_STATE_INITIALIZED;

        /* Flag to get that last few bytes are taken from the user buffer */
        NdefMap->StdMifareContainer.RemainingBufFlag = PH_FRINFC_MIFARESTD_FLAG0;

        /* Flag to find that the read/write operation has reached the end of the card. 
            Further reading/writing is not possible */
        NdefMap->StdMifareContainer.ReadWriteCompleteFlag = PH_FRINFC_MIFARESTD_FLAG0;

        /* Flag to get that last few bytes are taken from the internal buffer */
        NdefMap->StdMifareContainer.internalBufFlag = PH_FRINFC_MIFARESTD_FLAG0;

        /* Authentication Flag for every sector */
        NdefMap->StdMifareContainer.AuthDone = PH_FRINFC_MIFARESTD_FLAG0;

        /* Used in Check Ndef for storing the sector ID */
        NdefMap->StdMifareContainer.SectorIndex = PH_FRINFC_MIFARESTD_VAL0;

        NdefMap->StdMifareContainer.NdefBlocks = PH_FRINFC_MIFARESTD_VAL0;

        NdefMap->StdMifareContainer.NoOfNdefCompBlocks = PH_FRINFC_MIFARESTD_VAL0;

        NdefMap->StdMifareContainer.ReadAcsBitFlag = PH_FRINFC_MIFARESTD_FLAG0;

        NdefMap->StdMifareContainer.remSizeUpdFlag = PH_FRINFC_MIFARESTD_FLAG0;

        NdefMap->TLVStruct.NoLbytesinTLV = PH_FRINFC_MIFARESTD_VAL0;

        NdefMap->TLVStruct.prevLenByteValue = PH_FRINFC_MIFARESTD_VAL0;

        NdefMap->TLVStruct.BytesRemainLinTLV = PH_FRINFC_MIFARESTD_VAL0;

        NdefMap->TLVStruct.NdefTLVBlock = PH_FRINFC_MIFARESTD_VAL0;

        NdefMap->TLVStruct.NdefTLVByte = PH_FRINFC_MIFARESTD_VAL0;

        NdefMap->TLVStruct.NULLTLVCount = PH_FRINFC_MIFARESTD_VAL0;

        NdefMap->StdMifareContainer.remainingSize = PH_FRINFC_MIFARESTD_VAL0;
        
        NdefMap->StdMifareContainer.ReadNdefFlag = PH_FRINFC_MIFARESTD_FLAG0;

        NdefMap->StdMifareContainer.WrNdefFlag = PH_FRINFC_MIFARESTD_FLAG0;

        NdefMap->StdMifareContainer.ChkNdefFlag = PH_FRINFC_MIFARESTD_FLAG0;

        NdefMap->StdMifareContainer.aidCompleteFlag = PH_FRINFC_MIFARESTD_FLAG0;

        NdefMap->StdMifareContainer.NFCforumSectFlag = PH_FRINFC_MIFARESTD_FLAG0;

        NdefMap->StdMifareContainer.ProprforumSectFlag = PH_FRINFC_MIFARESTD_PROP_1ST_CONFIG;

        NdefMap->StdMifareContainer.ReadCompleteFlag = PH_FRINFC_MIFARESTD_FLAG0;

        NdefMap->StdMifareContainer.FirstReadFlag = PH_FRINFC_MIFARESTD_FLAG0;

        NdefMap->StdMifareContainer.WrLength = PH_FRINFC_MIFARESTD_VAL1;

        NdefMap->StdMifareContainer.ChkNdefCompleteFlag = PH_FRINFC_MIFARESTD_FLAG0;

        NdefMap->StdMifareContainer.ReadOnlySectorIndex = PH_FRINFC_MIFARESTD_FLAG0;

        NdefMap->StdMifareContainer.TotalNoSectors = PH_FRINFC_MIFARESTD_FLAG0;

        NdefMap->StdMifareContainer.SectorTrailerBlockNo = PH_FRINFC_MIFARESTD_FLAG0;
    }

    return status;
}

/*!
 * \brief Check whether a particular Remote Device is NDEF compliant.
 *
 * The function checks whether the peer device is NDEF compliant.
 *
 * \param[in] NdefMap Pointer to a valid instance of the \ref phFriNfc_NdefMap_t 
 *                    structure describing the component context.
 *
 * \retval  NFCSTATUS_PENDING   The action has been successfully triggered.
 * \retval  Others              An error has occurred.
 *
 */

NFCSTATUS phFriNfc_MifareStdMap_ChkNdef( phFriNfc_NdefMap_t     *NdefMap)
{
    NFCSTATUS                   status =    NFCSTATUS_PENDING;
    uint8_t                     atq,
                                sak;
                                    
    if ( NdefMap == NULL)
    {
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        /* set the data for additional data exchange*/
        NdefMap->psDepAdditionalInfo.DepFlags.MetaChaining = 0;
        NdefMap->psDepAdditionalInfo.DepFlags.NADPresent = 0;
        NdefMap->psDepAdditionalInfo.NAD = 0;
        NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_CHECK_OPE;
        NdefMap->StdMifareContainer.CRIndex = PH_FRINFC_NDEFMAP_CR_CHK_NDEF;

        /* Get the Select Response and Sense Response to get 
            the exact Card Type either Mifare 1k or 4k */
#ifndef PH_HAL4_ENABLE
        sak = NdefMap->psRemoteDevInfo->RemoteDevInfo.CardInfo106.
                        Startup106.SelRes;
        atq = NdefMap->psRemoteDevInfo->RemoteDevInfo.CardInfo106.
                        Startup106.SensRes[0];        
#else
        sak = NdefMap->psRemoteDevInfo->RemoteDevInfo.Iso14443A_Info.Sak;
        atq = NdefMap->psRemoteDevInfo->RemoteDevInfo.Iso14443A_Info.AtqA[0];
        PHNFC_UNUSED_VARIABLE(atq);
#endif      

        if (0x08 == (sak & 0x18))
        {
            /* Total Number of Blocks in Mifare 1k Card */
            NdefMap->StdMifareContainer.NoOfNdefCompBlocks = 
                                        PH_FRINFC_NDEFMAP_MIFARESTD_1KNDEF_COMPBLOCK;
            NdefMap->StdMifareContainer.remainingSize = 
                                                ((NdefMap->CardType == PH_FRINFC_MIFARESTD_VAL0)?
                                                (PH_FRINFC_NDEFMAP_MIFARESTD_1KNDEF_COMPBLOCK * 
                                                PH_FRINFC_MIFARESTD_BLOCK_BYTES):
                                                NdefMap->StdMifareContainer.remainingSize);
            NdefMap->CardType = PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD;
        }
        else
        {
             /* Total Number of Blocks in Mifare 4k Card */
            NdefMap->StdMifareContainer.NoOfNdefCompBlocks = 
                                        PH_FRINFC_NDEFMAP_MIFARESTD_4KNDEF_COMPBLOCK;
            NdefMap->StdMifareContainer.remainingSize = 
                                            ((NdefMap->CardType == PH_FRINFC_MIFARESTD_VAL0)?
                                            (PH_FRINFC_NDEFMAP_MIFARESTD_4KNDEF_COMPBLOCK * 
                                            PH_FRINFC_MIFARESTD_BLOCK_BYTES): 
                                            NdefMap->StdMifareContainer.remainingSize);
            NdefMap->CardType = PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD;           
        }


        /*  phFriNfc_MifareStdMap_ChkNdef should be called only
            when currentBlock is 0 OR 64,65 and 66 (for Mifare 4k).
            Otherwise return error */
        /* and also Check the Authentication Flag */
        if((NdefMap->StdMifareContainer.currentBlock != 0) && 
            (NdefMap->StdMifareContainer.currentBlock != 1) &&
            (NdefMap->StdMifareContainer.currentBlock != 2) &&
            (NdefMap->StdMifareContainer.currentBlock != 64) && 
            (NdefMap->StdMifareContainer.currentBlock != 65) && 
            (NdefMap->StdMifareContainer.currentBlock != 66) )
        {
            status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_PARAMETER);
        }
        else if( NdefMap->StdMifareContainer.AuthDone == 0)
        {
            /*  Block 0 contains Manufacturer information and 
                also other informaton. So go for block 1 which
                contains AIDs. Authenticating any of the block 
                in a sector, Authenticates the whole sector */
            if(NdefMap->StdMifareContainer.currentBlock == 0)
            {
                NdefMap->StdMifareContainer.currentBlock = 1;
            }
            
            status = phFriNfc_MifStd_H_AuthSector(NdefMap);
        }
        else
        {            
            /*  Mifare 1k, sak = 0x08 atq = 0x04
                Mifare 4k, sak = 0x38 atq = 0x02 */
            if ((NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD) ||
                (NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD))
            {                           
                /* Change the state to Check Ndef Compliant */
                NdefMap->State = PH_FRINFC_NDEFMAP_STATE_CHK_NDEF_COMP;
                NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_CHECK_OPE;
                NdefMap->StdMifareContainer.ChkNdefFlag = PH_FRINFC_MIFARESTD_FLAG1;

                NdefMap->MapCompletionInfo.CompletionRoutine = phFriNfc_MifareStdMap_Process;
                NdefMap->MapCompletionInfo.Context = NdefMap;    

#ifndef PH_HAL4_ENABLE
                NdefMap->Cmd.MfCmd = phHal_eMifareCmdListMifareRead;
#else
                NdefMap->Cmd.MfCmd = phHal_eMifareRead;
#endif
                *NdefMap->SendRecvLength = NdefMap->TempReceiveLength;
                NdefMap->SendRecvBuf[0] = NdefMap->StdMifareContainer.currentBlock;
                NdefMap->SendLength = MIFARE_MAX_SEND_BUF_TO_READ;    
            
                /* Call the Overlapped HAL Transceive function */ 
                status = phFriNfc_OvrHal_Transceive(    NdefMap->LowerDevice,
                                                        &NdefMap->MapCompletionInfo,
                                                        NdefMap->psRemoteDevInfo,
                                                        NdefMap->Cmd,
                                                        &NdefMap->psDepAdditionalInfo,
                                                        NdefMap->SendRecvBuf,
                                                        NdefMap->SendLength,
                                                        NdefMap->SendRecvBuf,
                                                        NdefMap->SendRecvLength);    
            }
            else
            {
                /* Since we have decided temporarily not to go
                    for any new error codes we are using 
                    NFCSTATUS_INVALID_PARAMETER even though it is not 
                    the relevant error code here TBD */
                status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_PARAMETER);
            }
        }
    }
    return status;    
}


/*!
 * \brief Initiates Reading of NDEF information from the Remote Device.
 *
 * The function initiates the reading of NDEF information from a Remote Device.
 * It performs a reset of the state and starts the action (state machine).
 * A periodic call of the \ref phFriNfcNdefMap_Process has to be done once the action
 * has been triggered.
 */
NFCSTATUS phFriNfc_MifareStdMap_RdNdef( phFriNfc_NdefMap_t                  *NdefMap,
                                        uint8_t                             *PacketData,
                                        uint32_t                            *PacketDataLength,
                                        uint8_t                             Offset)
{
    NFCSTATUS               status =    NFCSTATUS_PENDING;

    NdefMap->ApduBufferSize = *PacketDataLength;
    NdefMap->NumOfBytesRead = PacketDataLength;
    *NdefMap->NumOfBytesRead = 0;
    NdefMap->ApduBuffIndex = 0;
    NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_READ_OPE;
    NdefMap->StdMifareContainer.CRIndex = PH_FRINFC_NDEFMAP_CR_RD_NDEF;
            
    if((NdefMap->CardState == PH_NDEFMAP_CARD_STATE_INVALID) 
        || (NdefMap->CardState == PH_NDEFMAP_CARD_STATE_INITIALIZED))
    {
        /* Card state  is not correct */
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_INVALID_PARAMETER); 
    }
    else
    {
        if( (Offset == PH_FRINFC_NDEFMAP_SEEK_BEGIN) || ( NdefMap->PrevOperation == 
            PH_FRINFC_NDEFMAP_WRITE_OPE))
        {
            phFriNfc_MifStd_H_RdWrReset(NdefMap);
            NdefMap->StdMifareContainer.ReadNdefFlag = PH_FRINFC_MIFARESTD_FLAG1;
            NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG0;
        }
        /* Offset = Current, but the read has reached the End of Card */
        if( (Offset == PH_FRINFC_NDEFMAP_SEEK_CUR) &&
            (NdefMap->StdMifareContainer.ReadWriteCompleteFlag == 
            PH_FRINFC_MIFARESTD_FLAG1))
        {
            status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_EOF_NDEF_CONTAINER_REACHED); 
        }
        else
        {
            NdefMap->Offset = (((Offset != PH_FRINFC_NDEFMAP_SEEK_BEGIN) && 
                                ( NdefMap->PrevOperation == 
                                PH_FRINFC_NDEFMAP_WRITE_OPE))?
                                PH_FRINFC_NDEFMAP_SEEK_BEGIN:
                                Offset);
            status = phFriNfc_MifStd_H_BlkChk(NdefMap);
            if(status == NFCSTATUS_SUCCESS)
            {
                NdefMap->ApduBuffer = PacketData;
                
                /* Read Operation in Progress */
                NdefMap->StdMifareContainer.ReadWriteCompleteFlag = PH_FRINFC_MIFARESTD_FLAG0;
                
                /* Check Authentication Flag */
                status = 
                ((NdefMap->StdMifareContainer.AuthDone == PH_FRINFC_MIFARESTD_FLAG1)?
                    phFriNfc_MifStd_H_RdABlock(NdefMap):
                    phFriNfc_MifStd_H_AuthSector(NdefMap));
            }
        }
    }
    return status;
}

/*!
 * \brief Initiates Writing of NDEF information to the Remote Device.
 *
 * The function initiates the writing of NDEF information to a Remote Device.
 * It performs a reset of the state and starts the action (state machine).
 * A periodic call of the \ref phFriNfcNdefMap_Process has to be done once the action
 * has been triggered.
 */
NFCSTATUS phFriNfc_MifareStdMap_WrNdef( phFriNfc_NdefMap_t     *NdefMap,
                                        uint8_t                 *PacketData,
                                        uint32_t                *PacketDataLength,
                                        uint8_t                 Offset)
{
    NFCSTATUS                   status =    NFCSTATUS_PENDING;

    NdefMap->ApduBuffer = PacketData;
    NdefMap->ApduBufferSize = *PacketDataLength;
    NdefMap->ApduBuffIndex = PH_FRINFC_MIFARESTD_VAL0;
    NdefMap->WrNdefPacketLength = PacketDataLength;
    *NdefMap->WrNdefPacketLength = PH_FRINFC_MIFARESTD_VAL0;
    NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_WRITE_OPE;
    NdefMap->StdMifareContainer.CRIndex = PH_FRINFC_NDEFMAP_CR_WR_NDEF;
    
    if((NdefMap->CardState == PH_NDEFMAP_CARD_STATE_INVALID) 
        || (NdefMap->CardState == PH_NDEFMAP_CARD_STATE_READ_ONLY))
    {
        /* Card state  is not correct */
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_INVALID_PARAMETER); 
    }
    else
    {
        if( (Offset == PH_FRINFC_NDEFMAP_SEEK_BEGIN) || 
        (NdefMap->PrevOperation == PH_FRINFC_NDEFMAP_READ_OPE))
        {
            NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->StdMifareContainer.RdBeforeWrFlag = PH_FRINFC_MIFARESTD_FLAG1;
            NdefMap->StdMifareContainer.WrNdefFlag = PH_FRINFC_MIFARESTD_FLAG1;
            NdefMap->StdMifareContainer.internalLength = PH_FRINFC_MIFARESTD_VAL0;
            NdefMap->StdMifareContainer.RdAfterWrFlag = PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->StdMifareContainer.AuthDone = PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->TLVStruct.NULLTLVCount = PH_FRINFC_MIFARESTD_VAL0;
            NdefMap->TLVStruct.NdefTLVByte = PH_FRINFC_MIFARESTD_VAL0;
            NdefMap->TLVStruct.NdefTLVAuthFlag = PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->StdMifareContainer.FirstReadFlag = PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->StdMifareContainer.remainingSize = 
                        (NdefMap->StdMifareContainer.NoOfNdefCompBlocks * 
                        PH_FRINFC_MIFARESTD_BLOCK_BYTES);
            NdefMap->StdMifareContainer.currentBlock = 
                                            PH_FRINFC_MIFARESTD_BLK4;
            NdefMap->StdMifareContainer.NdefBlocks = PH_FRINFC_MIFARESTD_VAL1;
            NdefMap->StdMifareContainer.NFCforumSectFlag = PH_FRINFC_MIFARESTD_FLAG0;
            /* This macro is added, to be compliant with the previous HAL 2.0 
                For HAL 2.0, polling is done before writing data to the mifare 
                std (if the offset is BEGIN), because if an error is reported 
                during read or write and again write is called, the PN531 state is 
                unchanged (so write will fail), to bring the PN531 to the correct 
                state, polling is done.  
                Changed on 13th Jan 2009
            */
#ifdef PH_HAL4_ENABLE
            NdefMap->StdMifareContainer.PollFlag = PH_FRINFC_MIFARESTD_FLAG0;
#else
            NdefMap->StdMifareContainer.PollFlag = PH_FRINFC_MIFARESTD_FLAG1;
#endif /* #ifdef PH_HAL4_ENABLE */
            NdefMap->StdMifareContainer.WrLength = PH_FRINFC_MIFARESTD_VAL0;
            NdefMap->StdMifareContainer.FirstWriteFlag = PH_FRINFC_MIFARESTD_FLAG1;
        }

        if(((Offset == PH_FRINFC_NDEFMAP_SEEK_CUR) &&
            (NdefMap->StdMifareContainer.ReadWriteCompleteFlag == 
            PH_FRINFC_MIFARESTD_FLAG1)) || ((NdefMap->StdMifareContainer.PollFlag == 
            PH_FRINFC_MIFARESTD_FLAG1) && (Offset == PH_FRINFC_NDEFMAP_SEEK_CUR)))
        {
            /* Offset = Current, but the read has reached the End of Card */
            status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_EOF_NDEF_CONTAINER_REACHED); 
        }
        else
        {
            NdefMap->Offset = (((Offset != PH_FRINFC_NDEFMAP_SEEK_BEGIN) && 
                            ( NdefMap->PrevOperation == 
                            PH_FRINFC_NDEFMAP_READ_OPE))?
                            PH_FRINFC_NDEFMAP_SEEK_BEGIN:
                            Offset);
            NdefMap->StdMifareContainer.AuthDone = PH_FRINFC_MIFARESTD_FLAG0;
            status = phFriNfc_MifStd_H_BlkChk(NdefMap);
            NdefMap->StdMifareContainer.ReadWriteCompleteFlag = PH_FRINFC_MIFARESTD_FLAG0;
            if(status == NFCSTATUS_SUCCESS)
            {
                if(NdefMap->StdMifareContainer.PollFlag == 
                    PH_FRINFC_MIFARESTD_FLAG1)
                {
                    /* if poll flag is set then call disconnect because the authentication 
                        has failed so reactivation of card is required */
                    status = phFriNfc_MifStd_H_CallDisCon(NdefMap);
                }
                /* Check Authentication Flag */ 
                else if(NdefMap->StdMifareContainer.AuthDone == PH_FRINFC_MIFARESTD_FLAG1)
                {
                    status =  ((NdefMap->Offset == 
                                PH_FRINFC_NDEFMAP_SEEK_BEGIN)?
                                phFriNfc_MifStd_H_RdBeforeWr(NdefMap):
                                phFriNfc_MifStd_H_WrABlock(NdefMap));
                }
                else
                {
                    status = phFriNfc_MifStd_H_AuthSector(NdefMap);
                }
            }
        }
    }
    return status;
}


/*!
 * \brief Completion Routine, Processing function, needed to avoid long blocking.
 * \note The lower (Overlapped HAL) layer must register a pointer to this function as a Completion
 *       Routine in order to be able to notify the component that an I/O has finished and data are
 *       ready to be processed.
 *
 */

void phFriNfc_MifareStdMap_Process( void       *Context,
                                    NFCSTATUS   Status)
{

    phFriNfc_NdefMap_t      *NdefMap; 
    
    uint8_t                 NDEFFlag = 0,
                            CRFlag = 0,
                            Temp16Bytes = 0,
                            i = 0;


    NdefMap = (phFriNfc_NdefMap_t *)Context;

    if((Status & PHNFCSTBLOWER) == (NFCSTATUS_SUCCESS & PHNFCSTBLOWER))
    {
        switch(NdefMap->State)
        {
            case PH_FRINFC_NDEFMAP_STATE_CHK_NDEF_COMP:
                Status = phFriNfc_MifStd_H_ProChkNdef(NdefMap);
                CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                            PH_FRINFC_MIFARESTD_FLAG1:
                            PH_FRINFC_MIFARESTD_FLAG0);
                break;

            case PH_FRINFC_NDEFMAP_STATE_READ:
                
                /* Receive Length for read shall always be equal to 16 */
                if((*NdefMap->SendRecvLength == PH_FRINFC_MIFARESTD_BYTES_READ) && 
                    (NdefMap->ApduBuffIndex < (uint16_t)NdefMap->ApduBufferSize))
                {
                    Temp16Bytes = PH_FRINFC_MIFARESTD_VAL0;
                    NDEFFlag = (uint8_t)PH_FRINFC_MIFARESTD_FLAG1;
                    if(NdefMap->TLVStruct.BytesRemainLinTLV != 0)
                    {
                        NDEFFlag = PH_FRINFC_MIFARESTD_FLAG0;
                        CRFlag = PH_FRINFC_MIFARESTD_FLAG0;
                        /* To read the remaining length (L) in TLV */
                        Status = phFriNfc_MifStd_H_RemainTLV(NdefMap, &NDEFFlag, &Temp16Bytes);
                        CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                            PH_FRINFC_MIFARESTD_FLAG1:
                                            PH_FRINFC_MIFARESTD_FLAG0); 
                    }

                    /* check the NDEFFlag is set. if this is not set, then 
                        in the above RemainTLV function all the 16 bytes has been 
                        read */
#if 0
                    if((NDEFFlag == PH_FRINFC_MIFARESTD_FLAG1) && 
                        (NdefMap->TLVStruct.NdefTLVFoundFlag == 
                        PH_FRINFC_MIFARESTD_FLAG1))
                    {
                        /* if the block is NDEF TLV then get data from here */
                        Status = phFriNfc_MifStd_H_RdNdefTLV(NdefMap, &Temp16Bytes);
                    }
#endif
                }
                else
                {
                    Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_RECEIVE_LENGTH);
                    CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
                }
                break;

            case PH_FRINFC_NDEFMAP_STATE_WRITE:
                Status = phFriNfc_MifStd_H_ProWrABlock(NdefMap);
                CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                    PH_FRINFC_MIFARESTD_FLAG1:
                                    PH_FRINFC_MIFARESTD_FLAG0);
                
                /* Call Completion Routine if CR Flag is Set to 1 */
                if(CRFlag == PH_FRINFC_MIFARESTD_FLAG1)
                {
                    *NdefMap->WrNdefPacketLength = NdefMap->ApduBuffIndex;
                }
                break;

            case PH_FRINFC_NDEFMAP_STATE_AUTH:
                NdefMap->StdMifareContainer.FirstReadFlag = PH_FRINFC_MIFARESTD_FLAG0;
                Status = phFriNfc_MifStd_H_ProAuth(NdefMap);
                CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                    PH_FRINFC_MIFARESTD_FLAG1:
                                    PH_FRINFC_MIFARESTD_FLAG0);
                break;

            case PH_FRINFC_NDEFMAP_STATE_RD_ACS_BIT:
                Status = phFriNfc_MifStd_H_ProAcsBits(NdefMap);
                CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                    PH_FRINFC_MIFARESTD_FLAG1:
                                    PH_FRINFC_MIFARESTD_FLAG0);                
                break;

            case PH_FRINFC_NDEFMAP_STATE_WR_NDEF_LEN:
                if(NdefMap->StdMifareContainer.RdAfterWrFlag == 
                    PH_FRINFC_MIFARESTD_FLAG1)
                {
                    Status = phFriNfc_MifStd_H_CallWrNdefLen(NdefMap);
                    CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                PH_FRINFC_MIFARESTD_FLAG1:
                                PH_FRINFC_MIFARESTD_FLAG0);
                }
                else
                {
                    /* Check this */
                    if(NdefMap->StdMifareContainer.TempBlockNo == 
                        NdefMap->StdMifareContainer.currentBlock)
                    {
                       (void)memcpy( NdefMap->StdMifareContainer.internalBuf,
                                NdefMap->StdMifareContainer.Buffer,
                                NdefMap->StdMifareContainer.internalLength);
                    }
                    *NdefMap->WrNdefPacketLength = NdefMap->ApduBuffIndex;
                    NdefMap->StdMifareContainer.currentBlock = 
                                    NdefMap->StdMifareContainer.TempBlockNo;
                    NdefMap->CardState = (uint8_t)((NdefMap->CardState == 
                                        PH_NDEFMAP_CARD_STATE_INITIALIZED)?
                                        PH_NDEFMAP_CARD_STATE_READ_WRITE:
                                        NdefMap->CardState);
                    CRFlag = (uint8_t)PH_FRINFC_MIFARESTD_FLAG1;
                }
                /*NdefMap->StdMifareContainer.remainingSize -= 
                (((NdefMap->ApduBufferSize) > (PH_FRINFC_MIFARESTD_NDEFTLV_L - 
                                            PH_FRINFC_MIFARESTD_VAL1))?
                                            ((uint16_t)(*NdefMap->WrNdefPacketLength + 
                                            PH_FRINFC_MIFARESTD_VAL4)):
                                            ((uint16_t)(*NdefMap->WrNdefPacketLength + 
                                            PH_FRINFC_MIFARESTD_VAL2)));*/
                break;

            case PH_FRINFC_NDEFMAP_STATE_RD_TO_WR_NDEF_LEN:
                CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
                Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_DEVICE_REQUEST);
                if(*NdefMap->SendRecvLength == PH_FRINFC_MIFARESTD_BYTES_READ)
                {
                    /* Size of NdefMap->SendRecvBuf is set by phLibNfc_Gen_NdefMapReset to PH_LIBNFC_GEN_MAX_BUFFER */
                    /* We don't have to check memory here */
                    for (i = PH_FRINFC_MIFARESTD_BYTES_READ; i > 0; i--)
                    {
                        NdefMap->SendRecvBuf[i] = NdefMap->SendRecvBuf[i-1];
                    }                     
                    NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                                NdefMap->StdMifareContainer.currentBlock;
                    Status = phFriNfc_MifStd_H_WriteNdefLen(NdefMap);
                    CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                PH_FRINFC_MIFARESTD_FLAG1:
                                PH_FRINFC_MIFARESTD_FLAG0);
                }
                break;

            case PH_FRINFC_NDEFMAP_STATE_GET_ACT_CARDSIZE:
                NDEFFlag = PH_FRINFC_MIFARESTD_FLAG1;
                if(NdefMap->TLVStruct.NoLbytesinTLV > PH_FRINFC_MIFARESTD_VAL0)
                {
                    NDEFFlag = PH_FRINFC_MIFARESTD_FLAG0;
                    Status = phFriNfc_MifStd_H_ChkRemainTLVs(NdefMap, &CRFlag, &NDEFFlag);
                    NdefMap->TLVStruct.NoLbytesinTLV = 
                                    PH_FRINFC_MIFARESTD_VAL0;
                }
                if((NDEFFlag == PH_FRINFC_MIFARESTD_FLAG1) && 
                    (CRFlag != PH_FRINFC_MIFARESTD_FLAG1))
                {
                    Status = phFriNfc_MifStd_H_ChkTLVs(NdefMap, &CRFlag);
                }
                if(((NdefMap->StdMifareContainer.ReadNdefFlag == 
                    PH_FRINFC_MIFARESTD_FLAG1) || 
                    (NdefMap->StdMifareContainer.WrNdefFlag == 
                    PH_FRINFC_MIFARESTD_FLAG1))&& 
                    (Status != NFCSTATUS_PENDING))
                {
                    NdefMap->StdMifareContainer.NFCforumSectFlag = 
                                            PH_FRINFC_MIFARESTD_FLAG1;
                    CRFlag = PH_FRINFC_MIFARESTD_FLAG0;
                    /* if the card state has changed to initialised and 
                     read ndef is called then error is returned */
                    if(((NdefMap->StdMifareContainer.WrNdefFlag == 
                        PH_FRINFC_MIFARESTD_FLAG1) && 
                        (NdefMap->CardState == PH_NDEFMAP_CARD_STATE_READ_ONLY)) || 
                        ((NdefMap->StdMifareContainer.ReadNdefFlag == 
                            PH_FRINFC_MIFARESTD_FLAG1) && 
                        (NdefMap->CardState == PH_NDEFMAP_CARD_STATE_INITIALIZED)))
                    {
                        Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                            NFCSTATUS_NO_NDEF_SUPPORT);
                    }
                    if(NdefMap->StdMifareContainer.AuthDone == 
                        PH_FRINFC_MIFARESTD_FLAG0)
                    {                       
                        Status = phFriNfc_MifStd_H_AuthSector(NdefMap);
                    }
                    else
                    {
                        Status = ((NdefMap->StdMifareContainer.ReadNdefFlag == 
                                    PH_FRINFC_MIFARESTD_FLAG1)?
                                    phFriNfc_MifStd_H_RdTLV(NdefMap):
                                    phFriNfc_MifStd_H_RdBeforeWr(NdefMap));
                    }
                    NdefMap->StdMifareContainer.ReadNdefFlag = 
                                            PH_FRINFC_MIFARESTD_FLAG0;
                    NdefMap->StdMifareContainer.WrNdefFlag = 
                                            PH_FRINFC_MIFARESTD_FLAG0;
                        
                }
                if(NdefMap->StdMifareContainer.ChkNdefFlag == 
                    PH_FRINFC_MIFARESTD_FLAG1)
                {
                    CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                            PH_FRINFC_MIFARESTD_FLAG1:
                            PH_FRINFC_MIFARESTD_FLAG0);
                }
                break;

            case PH_FRINFC_NDEFMAP_STATE_RD_BEF_WR:
                /* Read flag says that already part of TLV has been written */
                Status = phFriNfc_MifStd_H_ProBytesToWr(NdefMap);
                 CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                    PH_FRINFC_MIFARESTD_FLAG1:
                                    PH_FRINFC_MIFARESTD_FLAG0);
                break;

            case PH_FRINFC_NDEFMAP_STATE_WR_TLV:
                Status = phFriNfc_MifStd_H_ProWrTLV(NdefMap);
                CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                    PH_FRINFC_MIFARESTD_FLAG1:
                                    PH_FRINFC_MIFARESTD_FLAG0);
                break;

            case PH_FRINFC_NDEFMAP_STATE_RD_TLV:
                Status = phFriNfc_MifStd_H_ProRdTLV(NdefMap);
                CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                    PH_FRINFC_MIFARESTD_FLAG1:
                                    PH_FRINFC_MIFARESTD_FLAG0);
                break;

            case PH_FRINFC_NDEFMAP_STATE_TERM_TLV:
                phFriNfc_MifStd_H_SetNdefBlkAuth(NdefMap);
                NdefMap->StdMifareContainer.currentBlock = 
                                NdefMap->TLVStruct.NdefTLVBlock;
                Status = phFriNfc_MifStd_H_RdtoWrNdefLen(NdefMap);
                CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                    PH_FRINFC_MIFARESTD_FLAG1:
                                    PH_FRINFC_MIFARESTD_FLAG0);
                break;

            case PH_FRINFC_NDEFMAP_STATE_DISCONNECT:
                NdefMap->StdMifareContainer.PollFlag = PH_FRINFC_MIFARESTD_FLAG0;

#ifndef PH_HAL4_ENABLE
                Status = phFriNfc_MifStd_H_CallPoll(NdefMap);
                CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                    PH_FRINFC_MIFARESTD_FLAG1:
                                    PH_FRINFC_MIFARESTD_FLAG0);
                break;

            case PH_FRINFC_NDEFMAP_STATE_POLL:
#endif              
                Status = phFriNfc_MifStd_H_CallConnect(NdefMap);
                CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                    PH_FRINFC_MIFARESTD_FLAG1:
                                    PH_FRINFC_MIFARESTD_FLAG0);
                break;

            case PH_FRINFC_NDEFMAP_STATE_CONNECT:
                if(NdefMap->StdMifareContainer.FirstReadFlag == PH_FRINFC_MIFARESTD_FLAG1)
                {
                    NdefMap->StdMifareContainer.FirstReadFlag = PH_FRINFC_MIFARESTD_FLAG0;
                    Status = phFriNfc_MifStd_H_AuthSector(NdefMap);
                }
                else if((NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD ||
                         NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD) &&
                        (NdefMap->StdMifareContainer.ReadOnlySectorIndex &&
                         NdefMap->StdMifareContainer.SectorTrailerBlockNo ==  NdefMap->StdMifareContainer.currentBlock))
                {
                    NdefMap->StdMifareContainer.ReadOnlySectorIndex =
                        PH_FRINFC_MIFARESTD_FLAG0;
                    NdefMap->StdMifareContainer.SectorTrailerBlockNo =
                        PH_FRINFC_MIFARESTD_FLAG0;
                    NdefMap->StdMifareContainer.currentBlock = PH_FRINFC_MIFARESTD_FLAG0;
                        Status = NFCSTATUS_FAILED;
                }
                else
                {
                    Status = ((((NdefMap->Offset == PH_FRINFC_NDEFMAP_SEEK_CUR) && 
                        (NdefMap->PrevOperation == PH_FRINFC_NDEFMAP_WRITE_OPE)) || 
                        (NdefMap->StdMifareContainer.WrLength > 
                        PH_FRINFC_MIFARESTD_VAL0))?
                        phFriNfc_MifStd_H_ProStatNotValid(NdefMap, Status):
                        phFriNfc_MifStd_H_AuthSector(NdefMap));
                }
                CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                    PH_FRINFC_MIFARESTD_FLAG1:
                                    PH_FRINFC_MIFARESTD_FLAG0);
                break;

            case PH_FRINFC_NDEFMAP_STATE_RD_SEC_ACS_BIT:
                Status = phFriNfc_MifStd_H_ProSectorTrailorAcsBits(NdefMap);
                CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                    PH_FRINFC_MIFARESTD_FLAG1:
                                    PH_FRINFC_MIFARESTD_FLAG0);
                if ((CRFlag == PH_FRINFC_MIFARESTD_FLAG1) &&
                    (NdefMap->StdMifareContainer.WriteAcsBitFlag == PH_FRINFC_MIFARESTD_FLAG0))
                {
                    Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_DEVICE_REQUEST);
                }
            break;

            case PH_FRINFC_NDEFMAP_STATE_WRITE_SEC:
                /* Set flag for writing of Acs bit */
                NdefMap->StdMifareContainer.WriteAcsBitFlag = PH_FRINFC_MIFARESTD_FLAG1;

                /* The first NDEF sector is already made read only,
                   set card state to read only and proceed*/
                if(NdefMap->CardState != PH_NDEFMAP_CARD_STATE_READ_ONLY)
                {
                    Status = phFriNfc_MapTool_SetCardState(NdefMap, NdefMap->TLVStruct.BytesRemainLinTLV);
                    if(Status != NFCSTATUS_SUCCESS)
                    {
                        CRFlag = (uint8_t) PH_FRINFC_MIFARESTD_FLAG1;
                    }
                }

                if (CRFlag != PH_FRINFC_MIFARESTD_FLAG1)
                {
                    Status = phFriNfc_MifStd_H_ProWrSectorTrailor(NdefMap);
                    CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                       PH_FRINFC_MIFARESTD_FLAG1:
                                       PH_FRINFC_MIFARESTD_FLAG0);
                }
                break;

            default:
                Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_DEVICE_REQUEST);
                CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
                break;
        }
    }
    else if(NdefMap->State == PH_FRINFC_NDEFMAP_STATE_AUTH)
    {
        NdefMap->StdMifareContainer.PollFlag = PH_FRINFC_MIFARESTD_FLAG1;
        if(NdefMap->StdMifareContainer.FirstWriteFlag == 
            PH_FRINFC_MIFARESTD_FLAG1)
        {
            NdefMap->StdMifareContainer.FirstWriteFlag = 
                                            PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->StdMifareContainer.WrLength = 
                ((NdefMap->StdMifareContainer.NFCforumSectFlag == 
                PH_FRINFC_MIFARESTD_FLAG0)?
                PH_FRINFC_MIFARESTD_VAL1:
                NdefMap->StdMifareContainer.WrLength);
        }
        /*if(NdefMap->StdMifareContainer.WrLength != PH_FRINFC_MIFARESTD_VAL0)
        {
            Status = NFCSTATUS_SUCCESS;
            NdefMap->StdMifareContainer.ReadCompleteFlag = 
                                    PH_FRINFC_MIFARESTD_FLAG1;
            *NdefMap->WrNdefPacketLength = NdefMap->ApduBuffIndex;
            CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
        }
        else*/ if(NdefMap->StdMifareContainer.WrLength == PH_FRINFC_MIFARESTD_VAL0)
        {
            Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_EOF_NDEF_CONTAINER_REACHED);
            CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
        }
        else
        {
            /* Authentication has failed */
            Status = phFriNfc_MifStd_H_CallDisCon(NdefMap);
            CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                        PH_FRINFC_MIFARESTD_FLAG1:
                        PH_FRINFC_MIFARESTD_FLAG0);
        }
    }
    else
    {
        Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                            NFCSTATUS_INVALID_DEVICE_REQUEST);
        CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
    }
     /* Call Completion Routine if CR Flag is Set to 1 */
    if(CRFlag == PH_FRINFC_MIFARESTD_FLAG1)
    {
        phFriNfc_MifStd_H_Complete(NdefMap, Status);
    }
}

static NFCSTATUS phFriNfc_MifStd_H_RdABlock(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS               status =    NFCSTATUS_PENDING;

    /* set the data for additional data exchange*/
    NdefMap->psDepAdditionalInfo.DepFlags.MetaChaining = 0;
    NdefMap->psDepAdditionalInfo.DepFlags.NADPresent = 0;
    NdefMap->psDepAdditionalInfo.NAD = 0;

    NdefMap->State = PH_FRINFC_NDEFMAP_STATE_READ;
    NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_READ_OPE;
    NdefMap->MapCompletionInfo.CompletionRoutine = phFriNfc_MifareStdMap_Process;
    NdefMap->MapCompletionInfo.Context = NdefMap;
    
    if( NdefMap->ApduBuffIndex < 
        (uint16_t)NdefMap->ApduBufferSize)
    {
                
        if(NdefMap->StdMifareContainer.internalLength > PH_FRINFC_MIFARESTD_VAL0)
        {
            status = phFriNfc_MifStd_H_ChkIntLen(NdefMap);
        }/* internal Length Check */
        else
        {
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        NdefMap->StdMifareContainer.currentBlock;
            NdefMap->SendLength = MIFARE_MAX_SEND_BUF_TO_READ;
            *NdefMap->SendRecvLength = NdefMap->TempReceiveLength;

#ifndef PH_HAL4_ENABLE
            NdefMap->Cmd.MfCmd = phHal_eMifareCmdListMifareRead;
#else
            NdefMap->Cmd.MfCmd = phHal_eMifareRead;
#endif
            /* Call the Overlapped HAL Transceive function */ 
            status = phFriNfc_OvrHal_Transceive(    NdefMap->LowerDevice,
                                                    &NdefMap->MapCompletionInfo,
                                                    NdefMap->psRemoteDevInfo,
                                                    NdefMap->Cmd,
                                                    &NdefMap->psDepAdditionalInfo,
                                                    NdefMap->SendRecvBuf,
                                                    NdefMap->SendLength,
                                                    NdefMap->SendRecvBuf,
                                                    NdefMap->SendRecvLength);
        }        
    }
    else
    {
        /* Check for the Card Size */
        if((((NdefMap->StdMifareContainer.NoOfNdefCompBlocks - 
            NdefMap->StdMifareContainer.NdefBlocks) * 
            PH_FRINFC_MIFARESTD_BYTES_READ) == 0) ||  
            (NdefMap->ApduBufferSize == NdefMap->ApduBuffIndex))
        {
            NdefMap->StdMifareContainer.ReadWriteCompleteFlag = 
                (uint8_t)((((NdefMap->StdMifareContainer.NoOfNdefCompBlocks - 
                NdefMap->StdMifareContainer.NdefBlocks) * 
                PH_FRINFC_MIFARESTD_BYTES_READ) == 0)?
                PH_FRINFC_MIFARESTD_FLAG1:
                PH_FRINFC_MIFARESTD_FLAG0);
            *NdefMap->NumOfBytesRead = NdefMap->ApduBuffIndex;
            status = PHNFCSTVAL(CID_NFC_NONE, 
                                NFCSTATUS_SUCCESS);
        }
        else
        {
            /*Error: The control should not ideally come here.
                Return Error.*/
#ifndef PH_HAL4_ENABLE
            status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_CMD_ABORTED);
#else
            status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_FAILED);    
#endif
        }
    }
    return status;
}


static NFCSTATUS phFriNfc_MifStd_H_WrABlock(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS               status =    NFCSTATUS_PENDING;
    
    uint16_t                    RemainingBytes = 0,
                                BytesRemained = 0,
                                index = 0;

    uint8_t                     Temp16Bytes = 0;

    /* set the data for additional data exchange*/
    NdefMap->psDepAdditionalInfo.DepFlags.MetaChaining = 0;
    NdefMap->psDepAdditionalInfo.DepFlags.NADPresent = 0;
    NdefMap->psDepAdditionalInfo.NAD = 0;
    NdefMap->MapCompletionInfo.CompletionRoutine = phFriNfc_MifareStdMap_Process;
    NdefMap->MapCompletionInfo.Context = NdefMap;
    NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_WRITE_OPE;
    
    NdefMap->State = PH_FRINFC_NDEFMAP_STATE_WRITE;
   
    /* User Buffer Check */
    if( NdefMap->ApduBuffIndex < 
        (uint16_t)NdefMap->ApduBufferSize)
    {
        RemainingBytes = (((uint16_t)(NdefMap->ApduBufferSize -
                         NdefMap->ApduBuffIndex) < 
                        NdefMap->StdMifareContainer.remainingSize )?
                        (uint16_t)(NdefMap->ApduBufferSize -
                        NdefMap->ApduBuffIndex):
                        NdefMap->StdMifareContainer.remainingSize);

        NdefMap->SendRecvBuf[0] = NdefMap->StdMifareContainer.currentBlock;
        Temp16Bytes += PH_FRINFC_MIFARESTD_INC_1;
        /* Check for internal bytes */
        
        if(NdefMap->StdMifareContainer.internalLength > 0)
        {
            /* copy the bytes previously written in the internal Buffer */
            (void)memcpy(&( NdefMap->SendRecvBuf[Temp16Bytes]),
                    NdefMap->StdMifareContainer.internalBuf,
                    NdefMap->StdMifareContainer.internalLength);

            Temp16Bytes += (uint8_t)(NdefMap->StdMifareContainer.internalLength);
            if(RemainingBytes >= (  MIFARE_MAX_SEND_BUF_TO_WRITE - 
                                    Temp16Bytes))
            {
                /* Copy the Remaining bytes from the user buffer to make the send
                    data and length = 16 */
                (void)memcpy(&(NdefMap->SendRecvBuf[Temp16Bytes]),
                        NdefMap->ApduBuffer,
                        (MIFARE_MAX_SEND_BUF_TO_WRITE - Temp16Bytes));

                NdefMap->NumOfBytesWritten = 
                    (MIFARE_MAX_SEND_BUF_TO_WRITE - Temp16Bytes);
                Temp16Bytes += (MIFARE_MAX_SEND_BUF_TO_WRITE - Temp16Bytes);
                *NdefMap->DataCount = (Temp16Bytes - PH_FRINFC_MIFARESTD_VAL1);
            }
            else
            {
                (void)memcpy(&(NdefMap->SendRecvBuf[Temp16Bytes]),
                        NdefMap->ApduBuffer,
                        RemainingBytes);

                NdefMap->StdMifareContainer.internalBufFlag = PH_FRINFC_MIFARESTD_FLAG1;
                NdefMap->NumOfBytesWritten = RemainingBytes;
                Temp16Bytes += (uint8_t)(RemainingBytes);
                *NdefMap->DataCount = (Temp16Bytes - PH_FRINFC_MIFARESTD_VAL1);

                BytesRemained = (MIFARE_MAX_SEND_BUF_TO_WRITE - Temp16Bytes);
                /* Pad empty bytes with Zeroes to complete 16 bytes*/
                for(index = 0; index < BytesRemained; index++)
                {
                    NdefMap->SendRecvBuf[(Temp16Bytes + index)] = 
                                (uint8_t)((index == PH_FRINFC_MIFARESTD_VAL0)?
                                    PH_FRINFC_MIFARESTD_TERMTLV_T:
                                    PH_FRINFC_MIFARESTD_NULLTLV_T);
                    NdefMap->TLVStruct.SetTermTLVFlag = PH_FRINFC_MIFARESTD_FLAG1;
                }
                Temp16Bytes += (uint8_t)(BytesRemained);
            }
        }
        else
        {
            if(RemainingBytes >= (MIFARE_MAX_SEND_BUF_TO_WRITE - Temp16Bytes))
            {
                /* Bytes left to write < 16 
                    copy remaining bytes */
                (void)memcpy( &(NdefMap->SendRecvBuf[ 
                        Temp16Bytes]),
                        &(NdefMap->ApduBuffer[ 
                        NdefMap->ApduBuffIndex]),
                        (MIFARE_MAX_SEND_BUF_TO_WRITE - Temp16Bytes));

                NdefMap->NumOfBytesWritten = (MIFARE_MAX_SEND_BUF_TO_WRITE - Temp16Bytes);
                Temp16Bytes += (MIFARE_MAX_SEND_BUF_TO_WRITE - Temp16Bytes);
                *NdefMap->DataCount = (Temp16Bytes - PH_FRINFC_MIFARESTD_VAL1);
            }
            else
            {
                /* Bytes left to write < 16 
                    copy remaining bytes */
                (void)memcpy(&(NdefMap->SendRecvBuf[
                        Temp16Bytes]),
                        &(NdefMap->ApduBuffer[ 
                        NdefMap->ApduBuffIndex]),
                        RemainingBytes);

                NdefMap->StdMifareContainer.RemainingBufFlag = PH_FRINFC_MIFARESTD_FLAG1;
                NdefMap->NumOfBytesWritten = RemainingBytes;
                Temp16Bytes += (uint8_t)(RemainingBytes);
                *NdefMap->DataCount = (Temp16Bytes - PH_FRINFC_MIFARESTD_VAL1);
                

                /* Pad empty bytes with Zeroes to complete 16 bytes*/
                for(index = Temp16Bytes; index < MIFARE_MAX_SEND_BUF_TO_WRITE; index++)
                {
                    NdefMap->SendRecvBuf[index] = (uint8_t)((index == 
                                    Temp16Bytes)?
                                    PH_FRINFC_MIFARESTD_TERMTLV_T:
                                    PH_FRINFC_MIFARESTD_NULLTLV_T);

                    NdefMap->TLVStruct.SetTermTLVFlag = PH_FRINFC_MIFARESTD_FLAG1;
                }
            }
        }
        /* Buffer to store 16 bytes which is writing to the present block */
        (void)memcpy( NdefMap->StdMifareContainer.Buffer,
                &(NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_INC_1]),
                PH_FRINFC_MIFARESTD_BLOCK_BYTES);

        /* Write from here */
        NdefMap->SendLength = MIFARE_MAX_SEND_BUF_TO_WRITE;
#ifndef PH_HAL4_ENABLE
        NdefMap->Cmd.MfCmd = phHal_eMifareCmdListMifareWrite16;
#else
        NdefMap->Cmd.MfCmd = phHal_eMifareWrite16;
#endif
        *NdefMap->SendRecvLength = NdefMap->TempReceiveLength;
        /* Call the Overlapped HAL Transceive function */ 
        status = phFriNfc_OvrHal_Transceive(    NdefMap->LowerDevice,
                                                &NdefMap->MapCompletionInfo,
                                                NdefMap->psRemoteDevInfo,
                                                NdefMap->Cmd,
                                                &NdefMap->psDepAdditionalInfo,
                                                NdefMap->SendRecvBuf,
                                                NdefMap->SendLength,
                                                NdefMap->SendRecvBuf,
                                                NdefMap->SendRecvLength);       
    }
    else/* Check User Buffer */
    {
        if(NdefMap->StdMifareContainer.NdefBlocks >
             NdefMap->StdMifareContainer.NoOfNdefCompBlocks)
        {
            NdefMap->StdMifareContainer.ReadWriteCompleteFlag = 
                PH_FRINFC_MIFARESTD_FLAG1;
            status = PHNFCSTVAL(CID_NFC_NONE, 
                                NFCSTATUS_SUCCESS);
        }
        else if( NdefMap->ApduBuffIndex ==
            (uint16_t)NdefMap->ApduBufferSize)          
        {
            status = PHNFCSTVAL(CID_NFC_NONE, 
                                NFCSTATUS_SUCCESS);
        }
        else
        {
            /*Error: The control should not ideally come here.
              Return Error.*/
#ifndef PH_HAL4_ENABLE
            status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_CMD_ABORTED);
#else
            status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_FAILED);    
#endif
        }
    }
    return status;
}

static NFCSTATUS phFriNfc_MifStd_H_AuthSector(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS                   status = NFCSTATUS_PENDING;
    
    /* set the data for additional data exchange*/
    NdefMap->psDepAdditionalInfo.DepFlags.MetaChaining = 0;
    NdefMap->psDepAdditionalInfo.DepFlags.NADPresent = 0;
    NdefMap->psDepAdditionalInfo.NAD = 0;
    NdefMap->MapCompletionInfo.CompletionRoutine = phFriNfc_MifareStdMap_Process;
    NdefMap->MapCompletionInfo.Context = NdefMap;
    
    *NdefMap->SendRecvLength = NdefMap->TempReceiveLength;
    NdefMap->State = PH_FRINFC_NDEFMAP_STATE_AUTH;

    /* Authenticate */ 
#ifndef PH_HAL4_ENABLE
    NdefMap->Cmd.MfCmd = phHal_eMifareCmdListMifareAuthentA;
#else
    NdefMap->Cmd.MfCmd = phHal_eMifareAuthentA;
#endif
    NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                            ((NdefMap->TLVStruct.NdefTLVAuthFlag == 
                                PH_FRINFC_MIFARESTD_FLAG1)?
                                NdefMap->TLVStruct.NdefTLVBlock:
                                NdefMap->StdMifareContainer.currentBlock);

    /* if MAD blocks then authentication key is 
        0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5 else
        0xD3, 0xF7, 0xD3, 0xF7, 0xD3, 0xF7 */
    if(( (NdefMap->StdMifareContainer.currentBlock != PH_FRINFC_MIFARESTD_MAD_BLK0) && 
        (NdefMap->StdMifareContainer.currentBlock != PH_FRINFC_MIFARESTD_MAD_BLK1) &&
        (NdefMap->StdMifareContainer.currentBlock != PH_FRINFC_MIFARESTD_MAD_BLK2) &&
        (NdefMap->StdMifareContainer.currentBlock != PH_FRINFC_MIFARESTD_MAD_BLK64) && 
        (NdefMap->StdMifareContainer.currentBlock != PH_FRINFC_MIFARESTD_MAD_BLK65) && 
        (NdefMap->StdMifareContainer.currentBlock != PH_FRINFC_MIFARESTD_MAD_BLK66)) || 
        (NdefMap->TLVStruct.NdefTLVAuthFlag == 
                                (uint8_t)PH_FRINFC_MIFARESTD_FLAG1))
    {   
        NdefMap->SendRecvBuf[1] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_NDEFSECT1; /* 0xD3 */
        NdefMap->SendRecvBuf[2] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_NDEFSECT2; /* 0xF7 */
        NdefMap->SendRecvBuf[3] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_NDEFSECT1; /* 0xD3 */
        NdefMap->SendRecvBuf[4] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_NDEFSECT2; /* 0xF7 */
        NdefMap->SendRecvBuf[5] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_NDEFSECT1; /* 0xD3 */
        NdefMap->SendRecvBuf[6] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_NDEFSECT2; /* 0xF7 */
    }
    else
    {       
        NdefMap->SendRecvBuf[1] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT1; /* 0xA0 */
        NdefMap->SendRecvBuf[2] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT2; /* 0xA1 */
        NdefMap->SendRecvBuf[3] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT3; /* 0xA2 */
        NdefMap->SendRecvBuf[4] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT4; /* 0xA3 */
        NdefMap->SendRecvBuf[5] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT5; /* 0xA4 */
        NdefMap->SendRecvBuf[6] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_MADSECT6; /* 0xA5 */
    }

    if (NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD ||
        NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD)
    {
        if (NdefMap->StdMifareContainer.ReadOnlySectorIndex &&
            NdefMap->StdMifareContainer.SectorTrailerBlockNo ==  NdefMap->StdMifareContainer.currentBlock)
        {
            memcpy (&NdefMap->SendRecvBuf[1], &NdefMap->StdMifareContainer.UserScrtKeyB[0], PH_FRINFC_MIFARESTD_KEY_LEN);

            /* Authenticate with KeyB*/
#ifndef PH_HAL4_ENABLE
            NdefMap->Cmd.MfCmd = phHal_eMifareCmdListMifareAuthentB;
#else
            NdefMap->Cmd.MfCmd = phHal_eMifareAuthentB;
#endif
        }
    }

    NdefMap->SendLength = MIFARE_AUTHENTICATE_CMD_LENGTH;
    *NdefMap->SendRecvLength = NdefMap->TempReceiveLength;
    /* Call the Overlapped HAL Transceive function */ 
    status = phFriNfc_OvrHal_Transceive(    NdefMap->LowerDevice,
                                            &NdefMap->MapCompletionInfo,
                                            NdefMap->psRemoteDevInfo,
                                            NdefMap->Cmd,
                                            &NdefMap->psDepAdditionalInfo,
                                            NdefMap->SendRecvBuf,
                                            NdefMap->SendLength,
                                            NdefMap->SendRecvBuf,
                                            NdefMap->SendRecvLength);
    
    return status;
}

static void phFriNfc_MifStd_H_Complete(phFriNfc_NdefMap_t  *NdefMap,
                                        NFCSTATUS            Result)
{
    /* set the state back to the Reset_Init state*/
    NdefMap->State =  PH_FRINFC_NDEFMAP_STATE_RESET_INIT;

    /* set the completion routine*/
    NdefMap->CompletionRoutine[NdefMap->StdMifareContainer.CRIndex].
        CompletionRoutine(NdefMap->CompletionRoutine->Context, Result);
}

static NFCSTATUS phFriNfc_MifStd4k_H_CheckNdef(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS Result = NFCSTATUS_SUCCESS;

    /* Get the AID Block */
    if(NdefMap->StdMifareContainer.currentBlock == PH_FRINFC_MIFARESTD_MAD_BLK2)
    {
        NdefMap->StdMifareContainer.currentBlock = PH_FRINFC_MIFARESTD_MAD_BLK64;
        NdefMap->StdMifareContainer.AuthDone = PH_FRINFC_MIFARESTD_FLAG0;
    }
    else if(NdefMap->StdMifareContainer.currentBlock == PH_FRINFC_MIFARESTD_MAD_BLK64)
    {
        NdefMap->StdMifareContainer.currentBlock = PH_FRINFC_MIFARESTD_MAD_BLK65;
    }
    else
    {
        NdefMap->StdMifareContainer.currentBlock = PH_FRINFC_MIFARESTD_MAD_BLK66;
    }
                    
    Result = phFriNfc_MifareStdMap_ChkNdef(NdefMap);

    return Result;
    
}

static void phFriNfc_MifStd_H_fillAIDarray(phFriNfc_NdefMap_t *NdefMap)
{  
    uint8_t     byteindex = 0;
    
    if( (NdefMap->StdMifareContainer.currentBlock == PH_FRINFC_MIFARESTD_MAD_BLK1) || 
        (NdefMap->StdMifareContainer.currentBlock == PH_FRINFC_MIFARESTD_MAD_BLK64))
    {
        /* The First Two Bytes in Receive Buffer
            are CRC bytes so it is not copied
            instead, 0 is copied in AID[0] & AID[1] */
        NdefMap->StdMifareContainer.aid[NdefMap->StdMifareContainer.SectorIndex] = 
                                    PH_FRINFC_MIFARESTD_NDEF_COMP; 
        NdefMap->StdMifareContainer.SectorIndex++;
        byteindex = 2;
    }
    
    while(byteindex < PH_FRINFC_MIFARESTD_BYTES_READ)
    {
        if((NdefMap->SendRecvBuf[byteindex] == PH_FRINFC_NDEFMAP_MIFARESTD_NDEF_COMPVAL2) &&
            (NdefMap->SendRecvBuf[(byteindex + 1)] == PH_FRINFC_NDEFMAP_MIFARESTD_NDEF_COMPVAL1))
        {
            /* This flag is set when a NFC forum sector is found in a 
                MAD block for the first time*/
            NdefMap->StdMifareContainer.NFCforumSectFlag = PH_FRINFC_MIFARESTD_FLAG1;
            NdefMap->StdMifareContainer.aid[NdefMap->StdMifareContainer.SectorIndex] = 
                                        PH_FRINFC_MIFARESTD_NDEF_COMP;
            NdefMap->StdMifareContainer.SectorIndex++;
        }
        else
        {
            NdefMap->StdMifareContainer.aid[NdefMap->StdMifareContainer.SectorIndex] = 
                                    PH_FRINFC_MIFARESTD_NON_NDEF_COMP;
            NdefMap->StdMifareContainer.SectorIndex++;
            /* AID complete flag is set when a non NFC forum sector is found in a 
                MAD block after the NFC forum sector. After setting this, all other 
                values are ignored and are NOT NDEF compliant */
            NdefMap->StdMifareContainer.aidCompleteFlag = 
                ((NdefMap->StdMifareContainer.NFCforumSectFlag == 
                    PH_FRINFC_MIFARESTD_FLAG1)?
                    PH_FRINFC_MIFARESTD_FLAG1:
                    PH_FRINFC_MIFARESTD_FLAG0);

            NdefMap->StdMifareContainer.NFCforumSectFlag = PH_FRINFC_MIFARESTD_FLAG0;
            if(NdefMap->StdMifareContainer.aidCompleteFlag == PH_FRINFC_MIFARESTD_FLAG1)
            {
                break;
            }
        }
        byteindex += 2;
    }

    /* If "aidCompleteFlag" is set then the remaining sectors are made NOT 
        NDEF compliant */
    if((NdefMap->StdMifareContainer.aidCompleteFlag == PH_FRINFC_MIFARESTD_FLAG1) && 
        (NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD))
    {
        /* for Mifare 1k there are 16 sectors, till this number all sectors 
            are made NOT NDEF compliant */
        for(byteindex = NdefMap->StdMifareContainer.SectorIndex; 
            byteindex < PH_FRINFC_MIFARESTD1K_TOTAL_SECTOR;
            byteindex++)
        {
             NdefMap->StdMifareContainer.aid[byteindex] = 
                                    PH_FRINFC_MIFARESTD_NON_NDEF_COMP;
        }
    }
    /* for Mifare 4k there are 40 sectors, till this number all sectors
       are made NOT NDEF compliant */
    else if((NdefMap->StdMifareContainer.aidCompleteFlag == PH_FRINFC_MIFARESTD_FLAG1) &&
            (NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD))
    {
        for(byteindex = NdefMap->StdMifareContainer.SectorIndex;
            byteindex < PH_FRINFC_MIFARESTD4K_TOTAL_SECTOR;
            byteindex++)
        {
            NdefMap->StdMifareContainer.aid[byteindex] =
                PH_FRINFC_MIFARESTD_NON_NDEF_COMP;
        }
    }
}

static NFCSTATUS phFriNfc_MifStd_H_BlkChk(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint8_t     SectorID = 0, callbreak = 0;

    for(;;)
    {
        /* Get a Sector ID for the Current Block */
        SectorID = phFriNfc_MifStd_H_GetSect(NdefMap->StdMifareContainer.currentBlock);
        /* Check the card Type 1k or 4k */
        /* enter if Mifare 1k card. For Mifare 4k go to else */
        if(NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD)
        {
            /* if Sector Id > 15 No Sectors to write */
            if(SectorID > 15)
            {
                SectorID = phFriNfc_MifStd_H_GetSect(NdefMap->StdMifareContainer.currentBlock);
                /*Error: No Ndef Compliant Sectors present.*/
                Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_INVALID_PARAMETER);
                callbreak = 1;
            }
            else
            {
                phFriNfc_MifStd1k_H_BlkChk(NdefMap, SectorID, &callbreak);
            }
        } /* End of if */ /* End of Mifare 1k check */
        else /* Mifare 4k check starts here */
        {
            /* Sector > 39 no ndef compliant sectors found*/
            if(SectorID > PH_FRINFC_MIFARESTD_SECTOR_NO39)
            {
                /*Error: No Ndef Compliant Sectors present.*/
                Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_INVALID_PARAMETER);
                callbreak = 1;
            }
            else if(NdefMap->StdMifareContainer.currentBlock == PH_FRINFC_MIFARESTD_MAD_BLK64)
            {
                NdefMap->StdMifareContainer.currentBlock += PH_FRINFC_MIFARESTD_BLK4;
            }  
            else if(SectorID < PH_FRINFC_MIFARESTD_SECTOR_NO32) /* sector < 32 contains 4 blocks in each sector */
            {
                /* If the block checked is 63, the 3 blocks after this 
                    are AID(MAD) blocks so its need to be skipped */
                if(NdefMap->StdMifareContainer.currentBlock == PH_FRINFC_MIFARESTD_MAD_BLK63)
                {
                    NdefMap->StdMifareContainer.currentBlock += PH_FRINFC_MIFARESTD_BLK4;
                }               
                else
                {
                    phFriNfc_MifStd1k_H_BlkChk(NdefMap, SectorID, &callbreak);
                }
            }
            else
            {
                /* every last block of a sector needs to be skipped */
                if(((NdefMap->StdMifareContainer.currentBlock + 1) % 
                    PH_FRINFC_MIFARESTD_BLOCK_BYTES) == 0)
                {
                    NdefMap->StdMifareContainer.currentBlock++;                 
                }
                else
                {
                    if(NdefMap->StdMifareContainer.aid[SectorID] == 
                            PH_FRINFC_MIFARESTD_NDEF_COMP)
                    {
                        /* Check whether the block is first block of a (next)new sector and 
                            also check if it is first block then internal length is zero 
                            or not. Because once Authentication is done for the sector again 
                            we should not authenticate it again */
                        /* In this case 32 sectors contains 4 blocks and next remaining 8 sectors
                            contains 16 blocks that is why (32 * 4) + (sectorID - 32) *16*/
                        if((NdefMap->StdMifareContainer.currentBlock == 
                            ((PH_FRINFC_MIFARESTD_SECTOR_NO32 * PH_FRINFC_MIFARESTD_BLK4) + 
                            ((SectorID - PH_FRINFC_MIFARESTD_SECTOR_NO32) * PH_FRINFC_MIFARESTD_BLOCK_BYTES))) &&
                            (NdefMap->StdMifareContainer.internalLength == 0))
                        {
                            NdefMap->StdMifareContainer.AuthDone = 0;
                        }   
                        callbreak = 1;
                    }
                    else
                    {
                        NdefMap->StdMifareContainer.currentBlock += 16;
                    }
                }
            }
        }
        if(callbreak == 1)
        {
            break;
        }
    }

    return Result;
}

static uint8_t phFriNfc_MifStd_H_GetSect(uint8_t  BlockNumber)
{
    uint8_t SectorID = 0;

    if(BlockNumber >= PH_FRINFC_MIFARESTD4K_BLK128)
    {
        SectorID = (uint8_t)(PH_FRINFC_MIFARESTD_SECTOR_NO32 + 
                    ((BlockNumber - PH_FRINFC_MIFARESTD4K_BLK128)/
                    PH_FRINFC_MIFARESTD_BLOCK_BYTES));
    }
    else
    {
        SectorID = (BlockNumber/PH_FRINFC_MIFARESTD_BLK4);
    }
    return SectorID;
}

static NFCSTATUS phFriNfc_MifStd_H_RdAcsBit(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS Result = NFCSTATUS_SUCCESS;
    NdefMap->State = PH_FRINFC_NDEFMAP_STATE_RD_ACS_BIT;

    if( NdefMap->StdMifareContainer.ReadOnlySectorIndex &&
       NdefMap->StdMifareContainer.currentBlock == NdefMap->StdMifareContainer.SectorTrailerBlockNo)
    {
        NdefMap->State = PH_FRINFC_NDEFMAP_STATE_RD_SEC_ACS_BIT;
    }

    if(NdefMap->StdMifareContainer.ReadAcsBitFlag == PH_FRINFC_MIFARESTD_FLAG1)
    {
        /* Get the sector trailer */
        ((NdefMap->StdMifareContainer.currentBlock > 127)?
            phFriNfc_MifStd_H_Get4kStTrail(NdefMap):
            phFriNfc_MifStd_H_Get1kStTrail(NdefMap));
    }
    else
    {
        /* Give the current block to read */
        NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        NdefMap->StdMifareContainer.currentBlock;
    }
    
    Result = phFriNfc_MifStd_H_Rd16Bytes(NdefMap, 
                        NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0]);
    
    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_ChkAcsBit(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS Result = NFCSTATUS_SUCCESS;

    /* Blocks from 0 to 3 and from 64 to 67(MAD blocks) */
    if((NdefMap->StdMifareContainer.currentBlock == PH_FRINFC_MIFARESTD_MAD_BLK0) || 
        (NdefMap->StdMifareContainer.currentBlock == PH_FRINFC_MIFARESTD_MAD_BLK1) || 
        (NdefMap->StdMifareContainer.currentBlock == PH_FRINFC_MIFARESTD_MAD_BLK2) || 
        (NdefMap->StdMifareContainer.currentBlock == PH_FRINFC_MIFARESTD_MAD_BLK3) || 
        (NdefMap->StdMifareContainer.currentBlock == PH_FRINFC_MIFARESTD_MAD_BLK64) ||  
        (NdefMap->StdMifareContainer.currentBlock == PH_FRINFC_MIFARESTD_MAD_BLK65) || 
        (NdefMap->StdMifareContainer.currentBlock == PH_FRINFC_MIFARESTD_MAD_BLK66) )
    {
        /* Access bits check removed for the MAD blocks */
#ifdef ENABLE_ACS_BIT_CHK_FOR_MAD

        if(((NdefMap->SendRecvBuf[ 
            PH_FRINFC_MIFARESTD_VAL6] & 
            PH_FRINFC_MIFARESTD_MASK_FF) == 
            PH_FRINFC_MIFARESTD_MADSECT_ACS_BYTE6) && 
            ((NdefMap->SendRecvBuf[ 
            PH_FRINFC_MIFARESTD_VAL7] & 
            PH_FRINFC_MIFARESTD_MASK_FF) == 
            PH_FRINFC_MIFARESTD_MADSECT_ACS_BYTE7) && 
            ((NdefMap->SendRecvBuf[ 
            PH_FRINFC_MIFARESTD_VAL8] & 
            PH_FRINFC_MIFARESTD_MASK_FF) == 
            PH_FRINFC_MIFARESTD_ACS_BYTE8))
        {               
            NdefMap->StdMifareContainer.WriteFlag = 
                PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->StdMifareContainer.ReadFlag = 
                PH_FRINFC_MIFARESTD_FLAG1;
        }
        else
        {
            NdefMap->StdMifareContainer.WriteFlag = 
                PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->StdMifareContainer.ReadFlag = 
                PH_FRINFC_MIFARESTD_FLAG0;
        }

#else /* #ifdef ENABLE_ACS_BIT_CHK_FOR_MAD */
    
        NdefMap->CardState = PH_NDEFMAP_CARD_STATE_INITIALIZED;

#endif /* #ifdef ENABLE_ACS_BIT_CHK_FOR_MAD */
    }
    else
    {
        /* Check for Access bytes 6, 7 and 8 value = 
            0x7F, 0x07, 0x88 NFC forum sectors*/
        if(((NdefMap->SendRecvBuf[ 
            PH_FRINFC_MIFARESTD_VAL6] & 
            PH_FRINFC_MIFARESTD_MASK_FF) == 
            PH_FRINFC_MIFARESTD_NFCSECT_ACS_BYTE6) && 
            ((NdefMap->SendRecvBuf[ 
            PH_FRINFC_MIFARESTD_VAL7] & 
            PH_FRINFC_MIFARESTD_MASK_FF) == 
            PH_FRINFC_MIFARESTD_NFCSECT_ACS_BYTE7) && 
            ((NdefMap->SendRecvBuf[ 
            PH_FRINFC_MIFARESTD_VAL8] & 
            PH_FRINFC_MIFARESTD_MASK_FF) == 
            PH_FRINFC_MIFARESTD_ACS_BYTE8))
        {               
            NdefMap->StdMifareContainer.WriteFlag = 
                PH_FRINFC_MIFARESTD_FLAG1;
            NdefMap->StdMifareContainer.ReadFlag = 
                PH_FRINFC_MIFARESTD_FLAG1;
        }
        else if(((NdefMap->SendRecvBuf[ 
            PH_FRINFC_MIFARESTD_VAL6] & 
            PH_FRINFC_MIFARESTD_MASK_FF) == 
            PH_FRINFC_MIFARESTD_NFCSECT_RDACS_BYTE6) && 
            ((NdefMap->SendRecvBuf[
            PH_FRINFC_MIFARESTD_VAL7] & 
            PH_FRINFC_MIFARESTD_MASK_FF) == 
            PH_FRINFC_MIFARESTD_NFCSECT_RDACS_BYTE7) && 
            ((NdefMap->SendRecvBuf[ 
            PH_FRINFC_MIFARESTD_VAL8] & 
            PH_FRINFC_MIFARESTD_MASK_FF) == 
            PH_FRINFC_MIFARESTD_NFCSECT_RDACS_BYTE8))
        {
            /* Read Only state */
            /* Check for Access bytes 6, 7 and 8 value = 
                0x55, 0xAD, 0x2A NFC forum Sectors*/
            NdefMap->StdMifareContainer.WriteFlag = 
                PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->StdMifareContainer.ReadFlag = 
                PH_FRINFC_MIFARESTD_FLAG1;
        }
        else
        {
            NdefMap->StdMifareContainer.WriteFlag = 
                PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->StdMifareContainer.ReadFlag = 
                PH_FRINFC_MIFARESTD_FLAG0;
        }

#ifdef ENABLE_ACS_BIT_CHK_FOR_MAD
    
        /* Do nothing */

#else /* #ifdef ENABLE_ACS_BIT_CHK_FOR_MAD */

        Result = phFriNfc_MifStd_H_GPBChk(NdefMap);

#endif /* #ifdef ENABLE_ACS_BIT_CHK_FOR_MAD */

    }

#ifdef ENABLE_ACS_BIT_CHK_FOR_MAD

    Result = phFriNfc_MifStd_H_GPBChk(NdefMap);

#endif /* #ifdef ENABLE_ACS_BIT_CHK_FOR_MAD */

    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_ChkRdWr(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS Result = NFCSTATUS_SUCCESS;
    switch(NdefMap->PrevOperation)
    {
        case PH_FRINFC_NDEFMAP_CHECK_OPE:
            if(NdefMap->CardState == 
                PH_NDEFMAP_CARD_STATE_INVALID)
            {
                    /* No permission to read */
                Result = PHNFCSTVAL( CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_READ_FAILED);
            }
            else if((NdefMap->StdMifareContainer.currentBlock > 3) &&
                (NdefMap->StdMifareContainer.ChkNdefCompleteFlag ==
                                    PH_FRINFC_MIFARESTD_FLAG1) &&
                (NdefMap->StdMifareContainer.currentBlock != 
                            PH_FRINFC_MIFARESTD_MAD_BLK65) &&
                (NdefMap->StdMifareContainer.currentBlock != 
                            PH_FRINFC_MIFARESTD_MAD_BLK66))
            {
               /* NdefMap->StdMifareContainer.currentBlock =
                    ((NdefMap->StdMifareContainer.ReadCompleteFlag == 
                        PH_FRINFC_MIFARESTD_FLAG1)?
                        NdefMap->StdMifareContainer.currentBlock:
                        (NdefMap->StdMifareContainer.currentBlock + 
                        PH_FRINFC_MIFARESTD_VAL4));
                NdefMap->StdMifareContainer.currentBlock = 
                    ((NdefMap->StdMifareContainer.currentBlock == 
                        PH_FRINFC_MIFARESTD_MAD_BLK64)?
                    (NdefMap->StdMifareContainer.currentBlock + 
                    PH_FRINFC_MIFARESTD_VAL4):
                    NdefMap->StdMifareContainer.currentBlock);*/

                    Result = ((NdefMap->StdMifareContainer.ReadAcsBitFlag == 
                                PH_FRINFC_MIFARESTD_FLAG0)?
                                phFriNfc_MifStd_H_RdAcsBit(NdefMap):
                                phFriNfc_MifStd_H_AuthSector(NdefMap));             
            }
            else
            {
                Result = phFriNfc_MifareStdMap_ChkNdef(NdefMap);
            }
            break;

        case PH_FRINFC_NDEFMAP_READ_OPE:
            if(NdefMap->CardState == 
                PH_NDEFMAP_CARD_STATE_INVALID)
            {
                /* No permission to Read */
                Result = PHNFCSTVAL( CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_READ_FAILED);
            }
            else if(NdefMap->StdMifareContainer.ReadNdefFlag == 
                    PH_FRINFC_MIFARESTD_FLAG1)
            {
                Result = phFriNfc_MifStd_H_GetActCardLen(NdefMap);
            }
            else
            {
                Result = phFriNfc_MifStd_H_RdABlock(NdefMap);
            }
            break;

        case PH_FRINFC_NDEFMAP_WRITE_OPE:
            if((NdefMap->CardState == 
                PH_NDEFMAP_CARD_STATE_INVALID) || 
                (NdefMap->CardState == 
                PH_NDEFMAP_CARD_STATE_READ_ONLY))
            {
                /* No permission to Read */
                Result = PHNFCSTVAL( CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_WRITE_FAILED);
            }
            else if(NdefMap->StdMifareContainer.WrNdefFlag == 
                    PH_FRINFC_MIFARESTD_FLAG1)
            {
                Result = phFriNfc_MifStd_H_GetActCardLen(NdefMap);
            }
            else if(NdefMap->StdMifareContainer.RdBeforeWrFlag == 
                    PH_FRINFC_MIFARESTD_FLAG1)
            {
                /*NdefMap->StdMifareContainer.ReadFlag = 
                                PH_FRINFC_MIFARESTD_FLAG0;*/
                Result = phFriNfc_MifStd_H_RdBeforeWr(NdefMap);
            }
            else if(NdefMap->StdMifareContainer.RdAfterWrFlag == 
                    PH_FRINFC_MIFARESTD_FLAG1)
            {
                Result = phFriNfc_MifStd_H_RdtoWrNdefLen(NdefMap);
            }
            else
            {
                Result = (((NdefMap->TLVStruct.NdefTLVBlock == 
                            NdefMap->StdMifareContainer.currentBlock) && 
                            (NdefMap->Offset == 
                            PH_FRINFC_NDEFMAP_SEEK_BEGIN))?
                            phFriNfc_MifStd_H_RdBeforeWr(NdefMap):
                            phFriNfc_MifStd_H_WrABlock(NdefMap));
            }
            break;

        case PH_FRINFC_NDEFMAP_GET_ACTSIZE_OPE:
            Result = ((NdefMap->StdMifareContainer.ReadFlag == 
                        PH_FRINFC_MIFARESTD_FLAG0)?
                        (PHNFCSTVAL( CID_FRI_NFC_NDEF_MAP, 
                        NFCSTATUS_READ_FAILED)):
                        phFriNfc_MifStd_H_GetActCardLen(NdefMap));
            break;

        default:
            /* Operation is not correct */
            Result = PHNFCSTVAL( CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_PARAMETER);
            
            break;
    }
    return Result;
}

static void phFriNfc_MifStd_H_ChkNdefCmpltSects(phFriNfc_NdefMap_t *NdefMap)
{
    uint8_t index = 0;
    if(NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD)
    {
        for(index = 0; index < PH_FRINFC_MIFARESTD4K_TOTAL_SECTOR; index++)
        {
            /* For Mifare 4k, Block 0 to 31 contains 4 blocks */
            /* sector 0 and 15 are aid blocks */
            if(index != PH_FRINFC_MIFARESTD_SECTOR_NO16)
            {
                if(( (index < 32) && (index != PH_FRINFC_MIFARESTD_SECTOR_NO0)) 
                && (NdefMap->StdMifareContainer.aid[index] == 
                            PH_FRINFC_MIFARESTD_NON_NDEF_COMP))
                {
                    /* Only 3 blocks can be read/written till sector 31 */
                    NdefMap->StdMifareContainer.NoOfNdefCompBlocks -= 
                                                PH_FRINFC_MIFARESTD_MAD_BLK3;
                    
                }
                else
                {
                    /* For Mifare 4k, Block 32 to 39 contains 16 blocks */
                    if(NdefMap->StdMifareContainer.aid[index] == 
                        PH_FRINFC_MIFARESTD_NON_NDEF_COMP) 
                    {
                        /* Only 15 blocks can be read/written from sector 31 */
                        NdefMap->StdMifareContainer.NoOfNdefCompBlocks -= 
                                                PH_FRINFC_MIFARESTD_BLK15;
                    }
                }
            }
        } /* For index > 40 */
    }
    else
    {
        for(index = PH_FRINFC_MIFARESTD_SECTOR_NO1; 
            index < PH_FRINFC_MIFARESTD_SECTOR_NO16; index++)
        {
            if(NdefMap->StdMifareContainer.aid[index] == 
                PH_FRINFC_MIFARESTD_NON_NDEF_COMP) 
            {
                /*  Only three blocks can be read/written in 
                    a sector. So if a sector is non-ndef 
                    compliant, decrement 3 */
                NdefMap->StdMifareContainer.NoOfNdefCompBlocks -= 
                                        PH_FRINFC_MIFARESTD_MAD_BLK3;
            }
        }
    }
}

#if 0
static uint8_t phFriNfc_MifStd_H_ChkNdefLen(phFriNfc_NdefMap_t *NdefMap,
                                        uint8_t             NDEFlength,
                                        uint8_t             *CRFlag)
{
    uint8_t     NoLengthBytesinTLV = 0,
                TempBuffer[16] = {0};
    uint16_t    TempLength = 0;

    TempLength = ((NdefMap->StdMifareContainer.internalLength > 0)?
                NdefMap->StdMifareContainer.internalLength : 
                PH_FRINFC_MIFARESTD_BYTES_READ);

    if(NdefMap->StdMifareContainer.internalLength > 0)
    {
        (void)memcpy( TempBuffer,
                NdefMap->StdMifareContainer.internalBuf,
                TempLength);
    }
    else
    {
        (void)memcpy( TempBuffer,
                NdefMap->SendRecvBuf,
                TempLength);
    }
    
    /* if number of bytes for length (L) in TLV is greater than 0*/
    if(NdefMap->TLVStruct.NoLbytesinTLV != PH_FRINFC_MIFARESTD_VAL0)
    {
        switch(NdefMap->TLVStruct.NoLbytesinTLV)
        {
            /* there are two bytes remaining in the length (L) of TLV */
            case PH_FRINFC_MIFARESTD_VAL2:
                NoLengthBytesinTLV = PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES2;
                /* Get the value of the length (L) of the TLV */
                NdefMap->TLVStruct.BytesRemainLinTLV = *TempBuffer;
                NdefMap->TLVStruct.BytesRemainLinTLV = 
                        ((NdefMap->TLVStruct.BytesRemainLinTLV 
                        << PH_FRINFC_MIFARESTD_LEFTSHIFT8) + (TempBuffer[ 
                        PH_FRINFC_MIFARESTD_INC_1]));

                *CRFlag = (uint8_t)((NdefMap->TLVStruct.BytesRemainLinTLV < 
                            PH_FRINFC_MIFARESTD_NDEFTLV_L)?
                            PH_FRINFC_MIFARESTD_FLAG1 : 
                            PH_FRINFC_MIFARESTD_FLAG0);
                NdefMap->TLVStruct.BytesRemainLinTLV = ((*CRFlag == 
                                    PH_FRINFC_MIFARESTD_FLAG1)?
                                    PH_FRINFC_MIFARESTD_VAL0:
                                    NdefMap->TLVStruct.BytesRemainLinTLV);
                break;

            /* there is only one byte remaining in the length (L) of TLV */
            case PH_FRINFC_MIFARESTD_VAL1:
                NoLengthBytesinTLV = PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES1;
                /* Get the value of the length (L) of the TLV */
                NdefMap->TLVStruct.BytesRemainLinTLV = 
                            NdefMap->TLVStruct.prevLenByteValue;

                NdefMap->TLVStruct.BytesRemainLinTLV = 
                        ((NdefMap->TLVStruct.BytesRemainLinTLV 
                        << PH_FRINFC_MIFARESTD_LEFTSHIFT8) + (*TempBuffer));
                *CRFlag = (uint8_t)((NdefMap->TLVStruct.BytesRemainLinTLV < 
                            PH_FRINFC_MIFARESTD_NDEFTLV_L)?
                            PH_FRINFC_MIFARESTD_FLAG1 : 
                            PH_FRINFC_MIFARESTD_FLAG0); 

                NdefMap->TLVStruct.BytesRemainLinTLV = ((*CRFlag == 
                                    PH_FRINFC_MIFARESTD_FLAG1)?
                                    PH_FRINFC_MIFARESTD_VAL0:
                                    NdefMap->TLVStruct.BytesRemainLinTLV);
                break;

            default:
                break;
        }
        NdefMap->TLVStruct.NoLbytesinTLV = PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES0;
        NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
        NdefMap->TLVStruct.LcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
    }
    else if(TempBuffer[NDEFlength] < 
                PH_FRINFC_MIFARESTD_NDEFTLV_L)
    {
        NoLengthBytesinTLV = PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES1;
        /* Get the value of the length (L) of the TLV */
        NdefMap->TLVStruct.BytesRemainLinTLV = 
                                TempBuffer[NDEFlength];
                
        /* if NDEFLength has reached 16 bytes, then the next coming byte is not of type in TLV  */
        NdefMap->TLVStruct.TcheckedinTLVFlag = (((NDEFlength + PH_FRINFC_MIFARESTD_INC_1) 
                                                    == PH_FRINFC_MIFARESTD_BYTES_READ)?
                                                    PH_FRINFC_MIFARESTD_FLAG1 : 
                                                    PH_FRINFC_MIFARESTD_FLAG0);
        /* if NDEFLength has reached 16 bytes, then the next coming byte is not of length in TLV */
        NdefMap->TLVStruct.LcheckedinTLVFlag = (((NDEFlength + PH_FRINFC_MIFARESTD_INC_1) 
                                                        == PH_FRINFC_MIFARESTD_BYTES_READ)?
                                                        PH_FRINFC_MIFARESTD_FLAG1 : 
                                                        PH_FRINFC_MIFARESTD_FLAG0);
    }
    else
    {
        if((NDEFlength + PH_FRINFC_MIFARESTD_INC_1) 
            == PH_FRINFC_MIFARESTD_BYTES_READ)
        {
            NoLengthBytesinTLV = PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES1;
            /* Number of bytes left for length (L) field in TLV */ 
            NdefMap->TLVStruct.NoLbytesinTLV = PH_FRINFC_MIFARESTD_VAL2;
            /* if NDEFLength has reached 16 bytes, then the next coming byte is not of type in TLV  */
            NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG1;
            NdefMap->TLVStruct.LcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
        }
        else if((NDEFlength + PH_FRINFC_MIFARESTD_INC_2) 
                == PH_FRINFC_MIFARESTD_BYTES_READ)
        {
            NoLengthBytesinTLV = PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES2;
            /* Number of bytes left for length (L) field in TLV */ 
            NdefMap->TLVStruct.NoLbytesinTLV = PH_FRINFC_MIFARESTD_VAL1;
            /* if NDEFLength has reached 16 bytes, then the next byte is not of type in TLV  */
            NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG1;
            NdefMap->TLVStruct.LcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
            /* copy the value of the length field */
            NdefMap->TLVStruct.prevLenByteValue = 
                NdefMap->SendRecvBuf[(NDEFlength + PH_FRINFC_MIFARESTD_INC_1)];
        }
        else
        {
            /* if NDEFLength has reached 16 bytes, then the next byte is not of type in TLV  */
            NdefMap->TLVStruct.TcheckedinTLVFlag = (((NDEFlength + PH_FRINFC_MIFARESTD_INC_3) 
                                                        == PH_FRINFC_MIFARESTD_BYTES_READ)?
                                                        PH_FRINFC_MIFARESTD_FLAG1 : 
                                                        PH_FRINFC_MIFARESTD_FLAG0);

            /* if NDEFLength has reached 16 bytes, then the next byte is not of length in TLV */
            NdefMap->TLVStruct.LcheckedinTLVFlag = (((NDEFlength + PH_FRINFC_MIFARESTD_INC_1) 
                                                            == PH_FRINFC_MIFARESTD_BYTES_READ)?
                                                            PH_FRINFC_MIFARESTD_FLAG1 : 
                                                            PH_FRINFC_MIFARESTD_FLAG0);

            /* Number of bytes left for length (L) field in TLV */ 
            NdefMap->TLVStruct.NoLbytesinTLV = PH_FRINFC_MIFARESTD_VAL0;

            NoLengthBytesinTLV = PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES3;
            /* Get the value of the length (L) of the TLV */
            NdefMap->TLVStruct.BytesRemainLinTLV = 
                                            TempBuffer[(NDEFlength +
                                            PH_FRINFC_MIFARESTD_INC_1)];

            NdefMap->TLVStruct.BytesRemainLinTLV = 
                    ((NdefMap->TLVStruct.BytesRemainLinTLV 
                    << PH_FRINFC_MIFARESTD_LEFTSHIFT8) + TempBuffer[ 
                    (NDEFlength +PH_FRINFC_MIFARESTD_INC_2)]);
            
            *CRFlag = (uint8_t)((NdefMap->TLVStruct.BytesRemainLinTLV < 
                            PH_FRINFC_MIFARESTD_NDEFTLV_L)?
                            PH_FRINFC_MIFARESTD_FLAG1 : 
                            PH_FRINFC_MIFARESTD_FLAG0);
        }
    }
    return NoLengthBytesinTLV;
}
#endif
#if 0
static NFCSTATUS phFriNfc_MifStd_H_RdNdefTLV(phFriNfc_NdefMap_t *NdefMap,
                                         uint8_t            *Temp16Bytes)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint8_t     NDEFlength = 0,
                RdFlag = 0,
                CRFlag = 0;
    uint16_t    RemainingBytes = 0;

    while(PH_FRINFC_MIFARESTD_FLAG1 == NdefMap->TLVStruct.NdefTLVFoundFlag)
    {
        RemainingBytes = ((uint16_t)NdefMap->ApduBufferSize - 
                            NdefMap->ApduBuffIndex);
        if((((  NdefMap->StdMifareContainer.NoOfNdefCompBlocks -
            NdefMap->StdMifareContainer.NdefBlocks) * 
            PH_FRINFC_MIFARESTD_BLOCK_BYTES) + 
            PH_FRINFC_MIFARESTD_BLOCK_BYTES) < 
            RemainingBytes)
        {
            /* If the user Buffer is greater than the Card Size
                set LastBlockFlag = 1. This Flag is used to read bytes
                till the end of the card only */
            RemainingBytes = ((( NdefMap->StdMifareContainer.NoOfNdefCompBlocks -
                                NdefMap->StdMifareContainer.NdefBlocks) * 16) + 
                                NdefMap->StdMifareContainer.internalLength);
           
        }
        /* Ndef TLV flag to 0 */
        NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG0;
        
        if((NdefMap->TLVStruct.TcheckedinTLVFlag != PH_FRINFC_MIFARESTD_FLAG1) && 
            (NdefMap->TLVStruct.LcheckedinTLVFlag != PH_FRINFC_MIFARESTD_FLAG1) && 
            (NdefMap->TLVStruct.NoLbytesinTLV != PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES0))
        {
            CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
            break;
        }

        /* if type (T) of TLV flag is not set then only the below check is valid 
            because, for eg. assume, type (T) of TLV flag is set then this means 
            that it is already checked from the previous 16 bytes data (T is the 
            last byte of 16 bytes */
        if(NdefMap->TLVStruct.TcheckedinTLVFlag != PH_FRINFC_MIFARESTD_FLAG1)
        {
            CRFlag = ((NdefMap->SendRecvBuf[(*Temp16Bytes)]!= 
                        PH_FRINFC_MIFARESTD_NDEFTLV_T)?
                        PH_FRINFC_MIFARESTD_FLAG1:
                        PH_FRINFC_MIFARESTD_FLAG0);

            if(CRFlag == PH_FRINFC_MIFARESTD_FLAG1)
            {
                NdefMap->StdMifareContainer.ReadWriteCompleteFlag = 
                                    PH_FRINFC_MIFARESTD_FLAG1;
                RdFlag = PH_FRINFC_MIFARESTD_FLAG1;
                break;
            }
            /* skip Type T of the TLV */
            *Temp16Bytes += PH_FRINFC_MIFARESTD_INC_1;
        }
        else
        {
            NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
        }
        
        if((*Temp16Bytes + PH_FRINFC_MIFARESTD_VAL1) == PH_FRINFC_MIFARESTD_BYTES_READ)
        {
            NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG1;
            NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG1;
            /* 16 bytes completed */
            NdefMap->StdMifareContainer.currentBlock++;
            NdefMap->StdMifareContainer.NdefBlocks++;
            Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
            if(Result == NFCSTATUS_SUCCESS)
            {
                if(NdefMap->StdMifareContainer.AuthDone == PH_FRINFC_MIFARESTD_FLAG1)
                {
                    Result = phFriNfc_MifStd_H_RdABlock(NdefMap);
                }
                else
                {
                    Result = phFriNfc_MifStd_H_AuthSector(NdefMap);
                }
            }               
            break;
        }
        /* skip number of bytes taken by L in TLV, if L is already read */
        if(NdefMap->TLVStruct.LcheckedinTLVFlag != PH_FRINFC_MIFARESTD_FLAG1)
        {
            /*CRFlag = (((*(NdefMap->SendRecvBuf + *Temp16Bytes) == 
                        PH_FRINFC_MIFARESTD_NDEFTLV_L0) && 
                        (NdefMap->TLVStruct.NoLbytesinTLV == 
                        PH_FRINFC_MIFARESTD_VAL0))?
                        PH_FRINFC_MIFARESTD_FLAG1:
                        PH_FRINFC_MIFARESTD_FLAG0);*/

            NDEFlength = *Temp16Bytes;
            *Temp16Bytes += phFriNfc_MifStd_H_ChkNdefLen(NdefMap,
                                                            NDEFlength,
                                                            &CRFlag);
            if(CRFlag == PH_FRINFC_MIFARESTD_FLAG1)
            {
                NdefMap->StdMifareContainer.ReadWriteCompleteFlag = 
                                    PH_FRINFC_MIFARESTD_FLAG1;
                break;
            }

        }
        else
        {
            NdefMap->TLVStruct.LcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
        }

        if(*Temp16Bytes == PH_FRINFC_MIFARESTD_BYTES_READ)
        {
            NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG1;
            NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG1;
            /* 16 bytes completed */
            NdefMap->StdMifareContainer.currentBlock++;
            NdefMap->StdMifareContainer.NdefBlocks++;
            Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
            if(Result == NFCSTATUS_SUCCESS)
            {
                if(NdefMap->StdMifareContainer.AuthDone == PH_FRINFC_MIFARESTD_FLAG1)
                {
                    Result = phFriNfc_MifStd_H_RdABlock(NdefMap);
                }
                else
                {
                    Result = phFriNfc_MifStd_H_AuthSector(NdefMap);
                }
            }               
            break;
        }

        if((NdefMap->TLVStruct.BytesRemainLinTLV <=  
            (PH_FRINFC_MIFARESTD_BYTES_READ - *Temp16Bytes)) && 
            (RemainingBytes <= 
                NdefMap->TLVStruct.BytesRemainLinTLV))
        {
            /* Copy data to user buffer */
            (void)memcpy(&(NdefMap->ApduBuffer[
                    NdefMap->ApduBuffIndex]),
                    &(NdefMap->SendRecvBuf[ 
                    (*Temp16Bytes)]),
                    RemainingBytes);
            NdefMap->ApduBuffIndex += RemainingBytes;
            *Temp16Bytes += ((uint8_t)RemainingBytes);
            NdefMap->TLVStruct.BytesRemainLinTLV -= RemainingBytes;
            
            if(NdefMap->TLVStruct.BytesRemainLinTLV == 0)
            {
                NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG1;
                NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
                NdefMap->TLVStruct.LcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
            }
            else
            {
                /* copy the bytes to internal buffer that are read, 
                    but not used for the user buffer */
                (void)memcpy(NdefMap->StdMifareContainer.internalBuf,
                    &(NdefMap->SendRecvBuf[(*Temp16Bytes)]),
                    (PH_FRINFC_MIFARESTD_BYTES_READ - *Temp16Bytes));
                
                /* TempLength of the internal buffer */
                NdefMap->StdMifareContainer.internalLength = 
                            (PH_FRINFC_MIFARESTD_BYTES_READ - *Temp16Bytes);
            }
            CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
            break;
        }
        else if((NdefMap->TLVStruct.BytesRemainLinTLV <=  
            (PH_FRINFC_MIFARESTD_BYTES_READ - *Temp16Bytes)) && 
            (RemainingBytes > 
                NdefMap->TLVStruct.BytesRemainLinTLV))
        {
            /* Copy data to user buffer */
            (void)memcpy(&(NdefMap->ApduBuffer[ 
                    NdefMap->ApduBuffIndex]),
                    &(NdefMap->SendRecvBuf[ 
                    (*Temp16Bytes)]),
                    NdefMap->TLVStruct.BytesRemainLinTLV);
            /* increment the user buffer index */
            NdefMap->ApduBuffIndex += NdefMap->TLVStruct.BytesRemainLinTLV;
            *Temp16Bytes += ((uint8_t)NdefMap->TLVStruct.BytesRemainLinTLV);
            NdefMap->TLVStruct.BytesRemainLinTLV = 0;
            NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG1;
            NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->TLVStruct.LcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;

            /* check for 16 bytes of data been used */
            if(*Temp16Bytes == PH_FRINFC_MIFARESTD_BYTES_READ)
            {
                /* 16 bytes completed */
                NdefMap->StdMifareContainer.currentBlock++;
                NdefMap->StdMifareContainer.NdefBlocks++;
                Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
                if(Result == NFCSTATUS_SUCCESS)
                {
                    if(NdefMap->StdMifareContainer.AuthDone == PH_FRINFC_MIFARESTD_FLAG1)
                    {
                        Result = phFriNfc_MifStd_H_RdABlock(NdefMap);
                    }
                    else
                    {
                        Result = phFriNfc_MifStd_H_AuthSector(NdefMap);
                    }
                }
                break;
            }
        }
        else if((NdefMap->TLVStruct.BytesRemainLinTLV >  
            (PH_FRINFC_MIFARESTD_BYTES_READ - *Temp16Bytes)) && 
            (RemainingBytes <= 
                (PH_FRINFC_MIFARESTD_BYTES_READ - *Temp16Bytes)))
        {
            /* Copy data to user buffer */
            (void)memcpy(&(NdefMap->ApduBuffer[
                    NdefMap->ApduBuffIndex]),
                    &(NdefMap->SendRecvBuf[ 
                    (*Temp16Bytes)]),
                    RemainingBytes);
            NdefMap->ApduBuffIndex += RemainingBytes;
            NdefMap->TLVStruct.BytesRemainLinTLV -= RemainingBytes;
            if(RemainingBytes == (PH_FRINFC_MIFARESTD_BYTES_READ - *Temp16Bytes))
            {
                NdefMap->StdMifareContainer.ReadWriteCompleteFlag = (uint8_t)
                    (((NdefMap->StdMifareContainer.remainingSize == PH_FRINFC_MIFARESTD_VAL0) || 
                    (NdefMap->TLVStruct.BytesRemainLinTLV == PH_FRINFC_MIFARESTD_VAL0))?
                    PH_FRINFC_MIFARESTD_FLAG1:
                    PH_FRINFC_MIFARESTD_FLAG0);
                
                /* 16 bytes completed */
                NdefMap->StdMifareContainer.currentBlock++;
                NdefMap->StdMifareContainer.NdefBlocks++;
            }
            else
            {
                /* copy the bytes to internal buffer, that are read, 
                    but not used for the user buffer */
                (void)memcpy(NdefMap->StdMifareContainer.internalBuf,
                    &(NdefMap->SendRecvBuf[((*Temp16Bytes) + RemainingBytes)]),
                    (PH_FRINFC_MIFARESTD_BYTES_READ - 
                    (*Temp16Bytes + RemainingBytes)));
                
                /* Length of the internal buffer */
                NdefMap->StdMifareContainer.internalLength = 
                            (PH_FRINFC_MIFARESTD_BYTES_READ - 
                            (*Temp16Bytes + RemainingBytes));
            }
            *Temp16Bytes += ((uint8_t)RemainingBytes);
            CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
            break;
        }
        else 
        {
            if((NdefMap->TLVStruct.BytesRemainLinTLV >  
            (PH_FRINFC_MIFARESTD_BYTES_READ - *Temp16Bytes)) && 
            (RemainingBytes > 
                (PH_FRINFC_MIFARESTD_BYTES_READ - *Temp16Bytes)))
            {
                /* Copy data to user buffer */
                (void)memcpy(&(NdefMap->ApduBuffer[ 
                        NdefMap->ApduBuffIndex]),
                        &(NdefMap->SendRecvBuf[ 
                        (*Temp16Bytes)]),
                        (PH_FRINFC_MIFARESTD_BYTES_READ - *Temp16Bytes));
                NdefMap->ApduBuffIndex += (PH_FRINFC_MIFARESTD_BYTES_READ - *Temp16Bytes);
                NdefMap->TLVStruct.BytesRemainLinTLV -= 
                            (PH_FRINFC_MIFARESTD_BYTES_READ - *Temp16Bytes);
                *Temp16Bytes = PH_FRINFC_MIFARESTD_BYTES_READ;
                
                
                /* 16 bytes completed */
                NdefMap->StdMifareContainer.currentBlock++;
                NdefMap->StdMifareContainer.NdefBlocks++;
                Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
                if(Result == NFCSTATUS_SUCCESS)
                {
                    Result = ((NdefMap->StdMifareContainer.AuthDone == 
                                PH_FRINFC_MIFARESTD_FLAG1)?
                                phFriNfc_MifStd_H_RdABlock(NdefMap):
                                phFriNfc_MifStd_H_AuthSector(NdefMap));
                }
                break;
            }
        }
    }

    if(CRFlag == PH_FRINFC_MIFARESTD_FLAG1)
    {
        Result = (((RdFlag == PH_FRINFC_MIFARESTD_FLAG1) && 
                    (NdefMap->ApduBuffIndex == PH_FRINFC_MIFARESTD_VAL0))?
                    (PHNFCSTVAL( CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_EOF_CARD_REACHED)):
                    Result);
        *NdefMap->NumOfBytesRead = NdefMap->ApduBuffIndex;
        phFriNfc_MifStd_H_Complete(NdefMap, Result);
    }
    return Result;
}
#endif

static NFCSTATUS phFriNfc_MifStd_H_RemainTLV(phFriNfc_NdefMap_t *NdefMap,
                                         uint8_t            *Flag,
                                         uint8_t            *Temp16Bytes)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint8_t     CRFlag = 0;
    uint16_t    RemainingBytes = 0;

    RemainingBytes = ((uint16_t)NdefMap->ApduBufferSize - 
                            NdefMap->ApduBuffIndex);

    if(NdefMap->StdMifareContainer.remainingSize < 
        RemainingBytes)
    {
        /* If the user Buffer is greater than the Card Size
            set LastBlockFlag = 1. This Flag is used to read bytes
            till the end of the card only */
        RemainingBytes = NdefMap->StdMifareContainer.remainingSize;
    }

    /* Remaining Bytes of length (L) in TLV <=  16 */
    if((NdefMap->TLVStruct.BytesRemainLinTLV <= 
        (PH_FRINFC_MIFARESTD_BYTES_READ - (*Temp16Bytes))) && 
        (RemainingBytes <= NdefMap->TLVStruct.BytesRemainLinTLV))
    {
        /* Copy data to user buffer */
        (void)memcpy(&(NdefMap->ApduBuffer[ 
                NdefMap->ApduBuffIndex]),
                &(NdefMap->SendRecvBuf[
                (*Temp16Bytes)]),
                RemainingBytes);
        NdefMap->ApduBuffIndex += RemainingBytes;
        NdefMap->StdMifareContainer.remainingSize -= RemainingBytes;
        
        /* copy the bytes to internal buffer, that are read, 
                        but not used for the user buffer */
        if(RemainingBytes != NdefMap->TLVStruct.BytesRemainLinTLV)
        {
            (void)memcpy( NdefMap->StdMifareContainer.internalBuf,
#ifdef PH_HAL4_ENABLE
                    &(NdefMap->SendRecvBuf[((*Temp16Bytes) + RemainingBytes)]),
#else
                    &(NdefMap->SendRecvBuf[RemainingBytes]),
#endif /* #ifdef PH_HAL4_ENABLE */                    
                    ((PH_FRINFC_MIFARESTD_BYTES_READ - 
                    (*Temp16Bytes)) - RemainingBytes));     

            /* internal buffer length */
            NdefMap->StdMifareContainer.internalLength = 
                    ((PH_FRINFC_MIFARESTD_BYTES_READ - 
                    (*Temp16Bytes)) - RemainingBytes);
        }
        *Temp16Bytes += ((uint8_t)RemainingBytes);
        /* Remaining Bytes of length value in TLV */
        NdefMap->TLVStruct.BytesRemainLinTLV -= RemainingBytes;

        if(NdefMap->StdMifareContainer.internalLength == PH_FRINFC_MIFARESTD_VAL0)
        {
            NdefMap->StdMifareContainer.ReadWriteCompleteFlag = (uint8_t)
            (((NdefMap->StdMifareContainer.remainingSize == PH_FRINFC_MIFARESTD_VAL0) || 
            (NdefMap->TLVStruct.BytesRemainLinTLV == PH_FRINFC_MIFARESTD_VAL0))?
            PH_FRINFC_MIFARESTD_FLAG1:
            PH_FRINFC_MIFARESTD_FLAG0);
        
            /* internal length bytes completed */
            NdefMap->StdMifareContainer.currentBlock++;
            NdefMap->StdMifareContainer.NdefBlocks++;
        }
        
        if(NdefMap->TLVStruct.BytesRemainLinTLV == PH_FRINFC_MIFARESTD_VAL0)
        {
            /* Remaining Bytes of length (L) in TLV is Zero means that the next 
             coming bytes are containing type (T), length (L) in TLV */
            NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG1;
            NdefMap->TLVStruct.LcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
        }
        /* call completion routine */
        CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
        *Flag = PH_FRINFC_MIFARESTD_FLAG0;
    }
    else if((NdefMap->TLVStruct.BytesRemainLinTLV <= 
            (PH_FRINFC_MIFARESTD_BYTES_READ - (*Temp16Bytes))) && 
            (RemainingBytes > NdefMap->TLVStruct.BytesRemainLinTLV))
    {
        /* Copy data to user buffer */
        (void)memcpy(&(NdefMap->ApduBuffer[
                NdefMap->ApduBuffIndex]),
                &(NdefMap->SendRecvBuf[ 
                (*Temp16Bytes)]),
                NdefMap->TLVStruct.BytesRemainLinTLV);
        NdefMap->ApduBuffIndex += NdefMap->TLVStruct.BytesRemainLinTLV;
        NdefMap->StdMifareContainer.remainingSize -= 
                                NdefMap->TLVStruct.BytesRemainLinTLV;
        NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG1;
        *Temp16Bytes += ((uint8_t)NdefMap->TLVStruct.BytesRemainLinTLV);
        NdefMap->TLVStruct.BytesRemainLinTLV = PH_FRINFC_MIFARESTD_VAL0;

        *Flag = PH_FRINFC_MIFARESTD_FLAG1;
        
        NdefMap->TLVStruct.LcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
        NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
        /* 16 bytes completed */
        if(NdefMap->TLVStruct.BytesRemainLinTLV == 
            PH_FRINFC_MIFARESTD_BYTES_READ)
        {
            *Flag = PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->TLVStruct.BytesRemainLinTLV = 
                            PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES0;
            NdefMap->StdMifareContainer.currentBlock++;
            NdefMap->StdMifareContainer.NdefBlocks++;
            Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
            if(Result == NFCSTATUS_SUCCESS)
            {
                if(NdefMap->StdMifareContainer.AuthDone == PH_FRINFC_MIFARESTD_FLAG1)
                {
                    Result = phFriNfc_MifStd_H_RdABlock(NdefMap);
                }
                else
                {
                    Result = phFriNfc_MifStd_H_AuthSector(NdefMap);
                }
            }
        }
        else
        {
            NdefMap->TLVStruct.BytesRemainLinTLV = 
                            PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES0;
            /* The read operation has finished. so, completion routine
            can be called. set the Completion routine(CR) flag */
            CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
        }
    }
    else if((NdefMap->TLVStruct.BytesRemainLinTLV > 
            (PH_FRINFC_MIFARESTD_BYTES_READ - (*Temp16Bytes))) && 
            (RemainingBytes <= (PH_FRINFC_MIFARESTD_BYTES_READ - (*Temp16Bytes))))
    {
        /* Copy data to user buffer */
        (void)memcpy(&(NdefMap->ApduBuffer[ 
                NdefMap->ApduBuffIndex]),
                &(NdefMap->SendRecvBuf[
                (*Temp16Bytes)]),
                RemainingBytes);
        NdefMap->ApduBuffIndex += RemainingBytes;
        NdefMap->StdMifareContainer.remainingSize -= RemainingBytes;
        
        /* Remaining Bytes of length (L) in TLV */
        NdefMap->TLVStruct.BytesRemainLinTLV -= RemainingBytes;
        /* 
            The below macro PH_HAL4_ENABLE is added because, 
            there is an error with the following sequence
            Read begin (for ex : with 10 bytes)
            Read current (for ex : with 10 bytes). Here error buffer 
            is returned
            Reason : During first read, internal buffer is updated 
                    with the wrong buffer values
            Actually, if we read the first block (means TLV block), it 
            contains even the TLV structure. so to copy the actual buffer, 
            the TLV shall be excluded
        */
#ifdef PH_HAL4_ENABLE
        /* copy the bytes to internal buffer, that are read, 
                        but not used for the user buffer */
        (void)memcpy( NdefMap->StdMifareContainer.internalBuf,
                &(NdefMap->SendRecvBuf[(RemainingBytes + (*Temp16Bytes))]),
                ((PH_FRINFC_MIFARESTD_BYTES_READ - (*Temp16Bytes))
                - RemainingBytes));

        /* internal buffer length */
        NdefMap->StdMifareContainer.internalLength = 
                ((PH_FRINFC_MIFARESTD_BYTES_READ - (*Temp16Bytes)) - RemainingBytes);        
#else
        /* copy the bytes to internal buffer, that are read, 
                        but not used for the user buffer */
        (void)memcpy( NdefMap->StdMifareContainer.internalBuf,
                &(NdefMap->SendRecvBuf[RemainingBytes]),
                ((PH_FRINFC_MIFARESTD_BYTES_READ - (*Temp16Bytes))
                - RemainingBytes));

        /* internal buffer length */
        NdefMap->StdMifareContainer.internalLength = 
                ((PH_FRINFC_MIFARESTD_BYTES_READ - (*Temp16Bytes)) - RemainingBytes);
#endif /* #ifdef PH_HAL4_ENABLE */
        
        if(RemainingBytes == (PH_FRINFC_MIFARESTD_BYTES_READ - (*Temp16Bytes)))
        {
            NdefMap->StdMifareContainer.ReadWriteCompleteFlag = (uint8_t)
                (((NdefMap->StdMifareContainer.remainingSize == PH_FRINFC_MIFARESTD_VAL0) || 
                (NdefMap->TLVStruct.BytesRemainLinTLV == PH_FRINFC_MIFARESTD_VAL0))?
                PH_FRINFC_MIFARESTD_FLAG1:
                PH_FRINFC_MIFARESTD_FLAG0);
        
            /* internal length bytes completed */
            NdefMap->StdMifareContainer.currentBlock++;
            NdefMap->StdMifareContainer.NdefBlocks++;
        }
        *Temp16Bytes += ((uint8_t)RemainingBytes);
        NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG0;
        CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
        *Flag = PH_FRINFC_MIFARESTD_FLAG0;
    }
    else
    {
        if((NdefMap->TLVStruct.BytesRemainLinTLV > 
            (PH_FRINFC_MIFARESTD_BYTES_READ - (*Temp16Bytes))) && 
            (RemainingBytes > (PH_FRINFC_MIFARESTD_BYTES_READ - (*Temp16Bytes))))
        {
            *Flag = PH_FRINFC_MIFARESTD_FLAG0;
            /* Copy data to user buffer */
            (void)memcpy(&(NdefMap->ApduBuffer 
                    [NdefMap->ApduBuffIndex]),
                    &(NdefMap->SendRecvBuf[ 
                    (*Temp16Bytes)]),
                    (PH_FRINFC_MIFARESTD_BYTES_READ - (*Temp16Bytes)));
            NdefMap->ApduBuffIndex += (PH_FRINFC_MIFARESTD_BYTES_READ - (*Temp16Bytes));
            NdefMap->StdMifareContainer.remainingSize -= 
                                    (PH_FRINFC_MIFARESTD_BYTES_READ - (*Temp16Bytes));
            NdefMap->TLVStruct.BytesRemainLinTLV -= (PH_FRINFC_MIFARESTD_BYTES_READ - (*Temp16Bytes));
            *Temp16Bytes += (PH_FRINFC_MIFARESTD_BYTES_READ - (*Temp16Bytes));
            if(NdefMap->TLVStruct.BytesRemainLinTLV != PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES0)
            {
                NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG0;
            }
            /* 16 bytes completed */
            NdefMap->StdMifareContainer.currentBlock++;
            NdefMap->StdMifareContainer.NdefBlocks++;
            Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
            if(Result == NFCSTATUS_SUCCESS)
            {
                Result = ((NdefMap->StdMifareContainer.AuthDone == 
                        PH_FRINFC_MIFARESTD_FLAG1)?
                        phFriNfc_MifStd_H_RdABlock(NdefMap):
                        phFriNfc_MifStd_H_AuthSector(NdefMap));
            }
        }
    }

    if(CRFlag == PH_FRINFC_MIFARESTD_FLAG1)
    {
        *NdefMap->NumOfBytesRead = NdefMap->ApduBuffIndex;
        NdefMap->StdMifareContainer.ReadWriteCompleteFlag = (uint8_t)
            (((NdefMap->StdMifareContainer.remainingSize == PH_FRINFC_MIFARESTD_VAL0) || 
            (NdefMap->TLVStruct.BytesRemainLinTLV == PH_FRINFC_MIFARESTD_VAL0))?
            PH_FRINFC_MIFARESTD_FLAG1:
            PH_FRINFC_MIFARESTD_FLAG0);
    }

    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_ChkIntLen(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint8_t     NDEFFlag = PH_FRINFC_MIFARESTD_FLAG1;
    uint8_t     TempintBytes = 0;

    if(NdefMap->TLVStruct.BytesRemainLinTLV != 0)
    {
        NDEFFlag = PH_FRINFC_MIFARESTD_FLAG0;
        /* To read the remaining length (L) in TLV */
        Result = phFriNfc_MifStd_H_IntLenWioutNdef(NdefMap, &NDEFFlag, &TempintBytes);
    }
    NDEFFlag = PH_FRINFC_MIFARESTD_FLAG0;
    /* check the NDEFFlag is set. if this is not set, then 
        in the above RemainTLV function all the 16 bytes has been 
        read */
#if 0
    if((NDEFFlag == PH_FRINFC_MIFARESTD_FLAG1) && 
        (NdefMap->TLVStruct.NdefTLVFoundFlag == 
        PH_FRINFC_MIFARESTD_FLAG1))
    {
        /* if the block is NDEF TLV then get data from here */
        Result = phFriNfc_MifStd_H_IntLenWithNdef(NdefMap, &TempintBytes);
    }
#endif
    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_IntLenWioutNdef(phFriNfc_NdefMap_t *NdefMap,
                                            uint8_t            *Flag,
                                            uint8_t            *TempintBytes)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint8_t     CRFlag = 0;
    uint16_t    RemainingBytes = 0;

    RemainingBytes = ((uint16_t)NdefMap->ApduBufferSize - 
                            NdefMap->ApduBuffIndex);

    if(NdefMap->StdMifareContainer.remainingSize < 
        RemainingBytes)
    {
        /* If the user Buffer is greater than the Card Size
            set LastBlockFlag = 1. This Flag is used to read bytes
            till the end of the card only */
        RemainingBytes = NdefMap->StdMifareContainer.remainingSize;        
    }

    /* Remaining Bytes of length (L) in TLV <=  internal length */
    if((NdefMap->TLVStruct.BytesRemainLinTLV <= 
        NdefMap->StdMifareContainer.internalLength) && 
        (RemainingBytes <= NdefMap->TLVStruct.BytesRemainLinTLV))
    {
        (void)memcpy(&(NdefMap->ApduBuffer[
                NdefMap->ApduBuffIndex]),
                &(NdefMap->StdMifareContainer.internalBuf[ 
                (*TempintBytes)]),
                RemainingBytes);
        NdefMap->ApduBuffIndex += RemainingBytes;
        NdefMap->StdMifareContainer.remainingSize -= RemainingBytes;
        *TempintBytes += ((uint8_t)RemainingBytes);

        /* copy the bytes to internal buffer, that are read, 
                        but not used for the user buffer */
        (void)memcpy( NdefMap->StdMifareContainer.internalBuf,
                &(NdefMap->StdMifareContainer.internalBuf[RemainingBytes]),
                (NdefMap->StdMifareContainer.internalLength - RemainingBytes));

        /* internal buffer length */
        NdefMap->StdMifareContainer.internalLength -= RemainingBytes;

        NdefMap->TLVStruct.BytesRemainLinTLV -= RemainingBytes;
        if(NdefMap->StdMifareContainer.internalLength == PH_FRINFC_MIFARESTD_VAL0)
        {
           NdefMap->StdMifareContainer.ReadWriteCompleteFlag = (uint8_t)
            (((NdefMap->StdMifareContainer.remainingSize == PH_FRINFC_MIFARESTD_VAL0) || 
            (NdefMap->TLVStruct.BytesRemainLinTLV == PH_FRINFC_MIFARESTD_VAL0))?
            PH_FRINFC_MIFARESTD_FLAG1:
            PH_FRINFC_MIFARESTD_FLAG0);
        
            /* internal length bytes completed */
            NdefMap->StdMifareContainer.currentBlock++;
            NdefMap->StdMifareContainer.NdefBlocks++;
        }
        
        /* Remaining Bytes of length value in TLV */
        if(NdefMap->TLVStruct.BytesRemainLinTLV == 0)
        {
            /* Remaining Bytes of length (L) in TLV is Zero means that the next 
             coming bytes are containing type (T), length (L) in TLV */
            NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG1;
            NdefMap->TLVStruct.LcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
        }
        /* call completion routine */
        CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
        *Flag = PH_FRINFC_MIFARESTD_FLAG0;
    }
    else if((NdefMap->TLVStruct.BytesRemainLinTLV <= 
            NdefMap->StdMifareContainer.internalLength) && 
            (RemainingBytes > NdefMap->TLVStruct.BytesRemainLinTLV))
    {
        (void)memcpy(&(NdefMap->ApduBuffer[NdefMap->ApduBuffIndex]),
                &(NdefMap->StdMifareContainer.internalBuf[(*TempintBytes)]),
                NdefMap->TLVStruct.BytesRemainLinTLV);

        NdefMap->ApduBuffIndex += NdefMap->TLVStruct.BytesRemainLinTLV;
        NdefMap->StdMifareContainer.remainingSize -= 
                    NdefMap->TLVStruct.BytesRemainLinTLV;
        NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG1;

        *TempintBytes += ((uint8_t)NdefMap->TLVStruct.BytesRemainLinTLV);
        *Flag = PH_FRINFC_MIFARESTD_FLAG1;

        NdefMap->TLVStruct.LcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
        NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;

        NdefMap->TLVStruct.BytesRemainLinTLV = PH_FRINFC_MIFARESTD_VAL0;
        NdefMap->StdMifareContainer.ReadWriteCompleteFlag = (uint8_t)
        (((NdefMap->StdMifareContainer.remainingSize == PH_FRINFC_MIFARESTD_VAL0) || 
            (NdefMap->TLVStruct.BytesRemainLinTLV == PH_FRINFC_MIFARESTD_VAL0))?
            PH_FRINFC_MIFARESTD_FLAG1:
            PH_FRINFC_MIFARESTD_FLAG0);
#ifdef PH_HAL4_ENABLE

        if (PH_FRINFC_MIFARESTD_FLAG1 == 
            NdefMap->StdMifareContainer.ReadWriteCompleteFlag)
        {
            CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
        }

#endif /* #ifdef PH_HAL4_ENABLE */

        if( NdefMap->TLVStruct.BytesRemainLinTLV == 
            NdefMap->StdMifareContainer.internalLength)
        {
            /* Remaining Bytes in Length (L) field of TLV is 0 */
            NdefMap->TLVStruct.BytesRemainLinTLV = 
                            PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES0;
            NdefMap->StdMifareContainer.internalLength = PH_FRINFC_MIFARESTD_VAL0;
            *Flag = PH_FRINFC_MIFARESTD_FLAG0;
            /* internal length bytes completed */
            NdefMap->StdMifareContainer.currentBlock++;
            NdefMap->StdMifareContainer.NdefBlocks++;
            Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
            if(Result == NFCSTATUS_SUCCESS)
            {
                Result = 
                ((NdefMap->StdMifareContainer.AuthDone == PH_FRINFC_MIFARESTD_FLAG1)?
                phFriNfc_MifStd_H_RdABlock(NdefMap):
                phFriNfc_MifStd_H_AuthSector(NdefMap));
            }
        }
        else
        {
            /* Remaining Bytes in Length (L) field of TLV is 0 */
            NdefMap->TLVStruct.BytesRemainLinTLV = 
                            PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES0;
            *Flag = PH_FRINFC_MIFARESTD_FLAG1;
        }
    }
    else if((NdefMap->TLVStruct.BytesRemainLinTLV > 
            NdefMap->StdMifareContainer.internalLength) && 
            (RemainingBytes <= NdefMap->StdMifareContainer.internalLength))
    {
        (void)memcpy(&(NdefMap->ApduBuffer[NdefMap->ApduBuffIndex]),
                &(NdefMap->StdMifareContainer.internalBuf[(*TempintBytes)]),
                RemainingBytes);

        NdefMap->ApduBuffIndex += RemainingBytes;
        NdefMap->StdMifareContainer.remainingSize -= 
                            RemainingBytes;
        *TempintBytes += ((uint8_t)RemainingBytes);
        /* Remaining Bytes of length (L) in TLV */
        NdefMap->TLVStruct.BytesRemainLinTLV -= RemainingBytes;

        /* copy the bytes to internal buffer, that are read, 
                        but not used for the user buffer */
        (void)memcpy( NdefMap->StdMifareContainer.internalBuf,
                &(NdefMap->StdMifareContainer.internalBuf[RemainingBytes]),
                (NdefMap->StdMifareContainer.internalLength - RemainingBytes));

        /* internal buffer length */
        NdefMap->StdMifareContainer.internalLength -= RemainingBytes;
        if(NdefMap->StdMifareContainer.internalLength == PH_FRINFC_MIFARESTD_VAL0)
        {
            NdefMap->StdMifareContainer.ReadWriteCompleteFlag = (uint8_t)
            (((NdefMap->StdMifareContainer.remainingSize == PH_FRINFC_MIFARESTD_VAL0) || 
            (NdefMap->TLVStruct.BytesRemainLinTLV == PH_FRINFC_MIFARESTD_VAL0))?
            PH_FRINFC_MIFARESTD_FLAG1:
            PH_FRINFC_MIFARESTD_FLAG0);
        
            /* internal length bytes completed */
            NdefMap->StdMifareContainer.currentBlock++;
            NdefMap->StdMifareContainer.NdefBlocks++;
        }
        
        NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG0;
        CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
        *Flag = PH_FRINFC_MIFARESTD_FLAG0;
    }
    else
    {
        if((NdefMap->TLVStruct.BytesRemainLinTLV > 
            NdefMap->StdMifareContainer.internalLength) && 
            (RemainingBytes > NdefMap->StdMifareContainer.internalLength))
        {
            (void)memcpy(&(NdefMap->ApduBuffer[NdefMap->ApduBuffIndex]),
                    &(NdefMap->StdMifareContainer.internalBuf[(*TempintBytes)]),
                    NdefMap->StdMifareContainer.internalLength);
            *Flag = PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->ApduBuffIndex += NdefMap->StdMifareContainer.internalLength;
            NdefMap->StdMifareContainer.remainingSize -= 
                                NdefMap->StdMifareContainer.internalLength;
            NdefMap->TLVStruct.BytesRemainLinTLV -= NdefMap->StdMifareContainer.internalLength;
            
            if(NdefMap->TLVStruct.BytesRemainLinTLV != PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES0)
            {
                NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG0;
            }

            NdefMap->StdMifareContainer.internalLength = PH_FRINFC_MIFARESTD_VAL0;
            /* internal length bytes completed */
            NdefMap->StdMifareContainer.currentBlock++;
            NdefMap->StdMifareContainer.NdefBlocks++;
            Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
            if(Result == NFCSTATUS_SUCCESS)
            {
                Result = ((NdefMap->StdMifareContainer.AuthDone == 
                            PH_FRINFC_MIFARESTD_FLAG1)?
                            phFriNfc_MifStd_H_RdABlock(NdefMap):
                            phFriNfc_MifStd_H_AuthSector(NdefMap));
            }
        }
    }

    if(CRFlag == PH_FRINFC_MIFARESTD_FLAG1)
    {
        NdefMap->StdMifareContainer.ReadWriteCompleteFlag = (uint8_t)
        (((NdefMap->StdMifareContainer.remainingSize == PH_FRINFC_MIFARESTD_VAL0) || 
            (NdefMap->TLVStruct.BytesRemainLinTLV == PH_FRINFC_MIFARESTD_VAL0))?
            PH_FRINFC_MIFARESTD_FLAG1:
            PH_FRINFC_MIFARESTD_FLAG0);
        *NdefMap->NumOfBytesRead = NdefMap->ApduBuffIndex;
    }
    return Result;
}
#if 0
static NFCSTATUS phFriNfc_MifStd_H_IntLenWithNdef(phFriNfc_NdefMap_t *NdefMap,
                                            uint8_t             *TempintBytes)
{
    NFCSTATUS       Result = NFCSTATUS_SUCCESS;
    uint16_t        RemainingBytes = 0;
    uint8_t         CRFlag =  PH_FRINFC_MIFARESTD_FLAG0,
                    RdFlag = PH_FRINFC_MIFARESTD_FLAG0,
                    NDEFlength = 0;

    RemainingBytes = (uint16_t)(NdefMap->ApduBufferSize - NdefMap->ApduBuffIndex);
    while(PH_FRINFC_MIFARESTD_FLAG1 == NdefMap->TLVStruct.NdefTLVFoundFlag)
    {
        NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG0;
        RemainingBytes = ((uint16_t)NdefMap->ApduBufferSize - 
                        NdefMap->ApduBuffIndex);
        if((((  NdefMap->StdMifareContainer.NoOfNdefCompBlocks -
            NdefMap->StdMifareContainer.NdefBlocks) * 
            PH_FRINFC_MIFARESTD_BLOCK_BYTES) + 
            (NdefMap->StdMifareContainer.internalLength - 
            *TempintBytes)) < 
            RemainingBytes)
        {
            /* If the user Buffer is greater than the Card Size
                set LastBlockFlag = 1. This Flag is used to read bytes
                till the end of the card only */
            RemainingBytes = ((( NdefMap->StdMifareContainer.NoOfNdefCompBlocks -
                                NdefMap->StdMifareContainer.NdefBlocks) * 16) + 
                                (NdefMap->StdMifareContainer.internalLength - 
                                *TempintBytes));
            
        }

        if((NdefMap->TLVStruct.TcheckedinTLVFlag != PH_FRINFC_MIFARESTD_FLAG1) && 
            (NdefMap->TLVStruct.LcheckedinTLVFlag != PH_FRINFC_MIFARESTD_FLAG1) && 
            (NdefMap->TLVStruct.NoLbytesinTLV != PH_FRINFC_MIFARESTD_NDEFTLV_LBYTES0))
        {
            CRFlag = 1;
            break;
        }

        /* if type (T) of TLV flag is not set then only the below check is valid 
            because, for eg. assume, type (T) of TLV flag is set then this means 
            that it is already checked from the previous internal bytes data (T is the 
            last byte of internal bytes */
        if(NdefMap->TLVStruct.TcheckedinTLVFlag != PH_FRINFC_MIFARESTD_FLAG1)
        {
            CRFlag =(uint8_t) ((NdefMap->StdMifareContainer.internalBuf[(*TempintBytes)]!= 
                    PH_FRINFC_MIFARESTD_NDEFTLV_T)?
                    PH_FRINFC_MIFARESTD_FLAG1:
                    PH_FRINFC_MIFARESTD_FLAG0);

            if(CRFlag == PH_FRINFC_MIFARESTD_FLAG1)
            {
                NdefMap->StdMifareContainer.ReadWriteCompleteFlag = 
                                    PH_FRINFC_MIFARESTD_FLAG1;
                RdFlag = PH_FRINFC_MIFARESTD_FLAG1;
                break;
            }
            /* skip Type T of the TLV */
            *TempintBytes += PH_FRINFC_MIFARESTD_INC_1;
        }
        else
        {
            NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
        }
        
        if(*TempintBytes == NdefMap->StdMifareContainer.internalLength)
        {
            NdefMap->StdMifareContainer.internalLength = PH_FRINFC_MIFARESTD_VAL0;
            NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG1;
            NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG1;
            /* 16 bytes completed */
            NdefMap->StdMifareContainer.currentBlock++;
            NdefMap->StdMifareContainer.NdefBlocks++;
            Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
            if(Result == NFCSTATUS_SUCCESS)
            {
                Result = 
                    ((NdefMap->StdMifareContainer.AuthDone == PH_FRINFC_MIFARESTD_FLAG1)?
                    phFriNfc_MifStd_H_RdABlock(NdefMap):
                    phFriNfc_MifStd_H_AuthSector(NdefMap));
            }
            break;
        }
        
        /* skip number of bytes taken by L in TLV, if L is already read */
        if(NdefMap->TLVStruct.LcheckedinTLVFlag != PH_FRINFC_MIFARESTD_FLAG1)
        {
            CRFlag = (uint8_t)((NdefMap->StdMifareContainer.internalBuf[(*TempintBytes)]== 
                    PH_FRINFC_MIFARESTD_NDEFTLV_L0)?
                    PH_FRINFC_MIFARESTD_FLAG1:
                    PH_FRINFC_MIFARESTD_FLAG0);

            if(CRFlag == PH_FRINFC_MIFARESTD_FLAG1)
            {
                NdefMap->StdMifareContainer.ReadWriteCompleteFlag = 
                                    PH_FRINFC_MIFARESTD_FLAG1;
                RdFlag = PH_FRINFC_MIFARESTD_FLAG1;
                break;
            }
            NDEFlength = *TempintBytes;
            *TempintBytes += phFriNfc_MifStd_H_ChkNdefLen(NdefMap,
                                                            NDEFlength,
                                                            &CRFlag);
            if(CRFlag == PH_FRINFC_MIFARESTD_FLAG1)
            {
                NdefMap->StdMifareContainer.ReadWriteCompleteFlag = 
                                    PH_FRINFC_MIFARESTD_FLAG1;
                RdFlag = PH_FRINFC_MIFARESTD_FLAG1;
                break;
            }
        }
        else
        {
            NdefMap->TLVStruct.LcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
        }

        if(*TempintBytes == NdefMap->StdMifareContainer.internalLength)
        {
            NdefMap->StdMifareContainer.internalLength = PH_FRINFC_MIFARESTD_VAL0;
            NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG1;
            NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG1;
            /* 16 bytes completed */
            NdefMap->StdMifareContainer.currentBlock++;
            NdefMap->StdMifareContainer.NdefBlocks++;
            Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
            if(Result == NFCSTATUS_SUCCESS)
            {
                Result = ((NdefMap->StdMifareContainer.AuthDone == 
                        PH_FRINFC_MIFARESTD_FLAG1)?
                        phFriNfc_MifStd_H_RdABlock(NdefMap):
                        phFriNfc_MifStd_H_AuthSector(NdefMap));
            }               
            break;
        }
        
        if( (NdefMap->TLVStruct.BytesRemainLinTLV <= 
            (NdefMap->StdMifareContainer.internalLength - *TempintBytes)) && 
            (RemainingBytes <= NdefMap->TLVStruct.BytesRemainLinTLV))
        {
            /* Copy the internal bytes to the user buffer */
            (void)memcpy(&(NdefMap->ApduBuffer[
                    NdefMap->ApduBuffIndex]),
                    &(NdefMap->StdMifareContainer.internalBuf[ 
                    (*TempintBytes)]),
                    RemainingBytes);
            
            NdefMap->ApduBuffIndex += RemainingBytes;
            *TempintBytes += ((uint8_t)RemainingBytes);
            NdefMap->TLVStruct.BytesRemainLinTLV -= RemainingBytes;
            if(NdefMap->TLVStruct.BytesRemainLinTLV == 0)
            {
                NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG1;                    
            }
            /* Update the internal length */
            (void)memcpy( NdefMap->StdMifareContainer.internalBuf,
                    &(NdefMap->StdMifareContainer.internalBuf[ 
                    (*TempintBytes)]),
                    RemainingBytes);
            NdefMap->StdMifareContainer.internalLength -= (*TempintBytes +
                                                        RemainingBytes);
        
            CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
            break;
        }
        else if((NdefMap->TLVStruct.BytesRemainLinTLV <= 
                (NdefMap->StdMifareContainer.internalLength - *TempintBytes)) && 
                (RemainingBytes > NdefMap->TLVStruct.BytesRemainLinTLV))
        {
            /* Copy data to user buffer */
            (void)memcpy(&(NdefMap->ApduBuffer[ 
                    NdefMap->ApduBuffIndex]),
                    &(NdefMap->StdMifareContainer.internalBuf[ 
                    (*TempintBytes)]),
                    NdefMap->TLVStruct.BytesRemainLinTLV);

            /* increment the user buffer index */
            NdefMap->ApduBuffIndex += NdefMap->TLVStruct.BytesRemainLinTLV;
            *TempintBytes += ((uint8_t)NdefMap->TLVStruct.BytesRemainLinTLV);
            NdefMap->TLVStruct.BytesRemainLinTLV = PH_FRINFC_MIFARESTD_VAL0;
            NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG1;

            /* check for internal length bytes of data been used */
            if(*TempintBytes == NdefMap->StdMifareContainer.internalLength)
            {
                /* 16 bytes completed */
                NdefMap->StdMifareContainer.currentBlock++;
                NdefMap->StdMifareContainer.NdefBlocks++;
                Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
                if(Result == NFCSTATUS_SUCCESS)
                {
                    Result = ((NdefMap->StdMifareContainer.AuthDone == 
                            PH_FRINFC_MIFARESTD_FLAG1)?
                            phFriNfc_MifStd_H_RdABlock(NdefMap):
                            phFriNfc_MifStd_H_AuthSector(NdefMap));
                }
                break;
            }
        }
        else if((NdefMap->TLVStruct.BytesRemainLinTLV > 
                (NdefMap->StdMifareContainer.internalLength - *TempintBytes)) && 
                (RemainingBytes <= (NdefMap->StdMifareContainer.internalLength - 
                *TempintBytes)))
        {
            /* Copy data to user buffer */
            (void)memcpy(&(NdefMap->ApduBuffer[ 
                    NdefMap->ApduBuffIndex]),
                    &(NdefMap->StdMifareContainer.internalBuf[ 
                    (*TempintBytes)]),
                    RemainingBytes);
            NdefMap->ApduBuffIndex += RemainingBytes;
            *TempintBytes += ((uint8_t)RemainingBytes);
            NdefMap->TLVStruct.BytesRemainLinTLV -= RemainingBytes;

            /* copy the bytes to internal buffer, that are read, 
                but not used for the user buffer */
            (void)memcpy(NdefMap->StdMifareContainer.internalBuf,
                &(NdefMap->StdMifareContainer.internalBuf[(*TempintBytes)]),
                (NdefMap->StdMifareContainer.internalLength - *TempintBytes));
            
            /* Length of the internal buffer */
            NdefMap->StdMifareContainer.internalLength -= *TempintBytes;
            CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
            break;
        }
        else
        {
            if((NdefMap->TLVStruct.BytesRemainLinTLV > 
                (NdefMap->StdMifareContainer.internalLength - *TempintBytes)) && 
                (RemainingBytes > (NdefMap->StdMifareContainer.internalLength - 
                *TempintBytes)))
            {
                /* Copy data to user buffer */
                (void)memcpy(&(NdefMap->ApduBuffer[
                        NdefMap->ApduBuffIndex]),
                        &(NdefMap->StdMifareContainer.internalBuf[
                        (*TempintBytes)]),
                        (NdefMap->StdMifareContainer.internalLength - 
                        *TempintBytes));
                NdefMap->ApduBuffIndex += (NdefMap->StdMifareContainer.internalLength - 
                                            *TempintBytes);
                NdefMap->TLVStruct.BytesRemainLinTLV -= 
                            (NdefMap->StdMifareContainer.internalLength - *TempintBytes);
                *TempintBytes += (uint8_t)(NdefMap->StdMifareContainer.internalLength - 
                                            *TempintBytes);
                
                NdefMap->StdMifareContainer.internalLength -= *TempintBytes;
                
                /* 16 bytes completed */
                NdefMap->StdMifareContainer.currentBlock++;
                NdefMap->StdMifareContainer.NdefBlocks++;
                Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
                if(Result == NFCSTATUS_SUCCESS)
                {
                    Result = ((NdefMap->StdMifareContainer.AuthDone == 
                                PH_FRINFC_MIFARESTD_FLAG1)?
                                phFriNfc_MifStd_H_RdABlock(NdefMap):
                                phFriNfc_MifStd_H_AuthSector(NdefMap));
                }
                break;
            }
        }
    }

    if((((  NdefMap->StdMifareContainer.NoOfNdefCompBlocks -
        NdefMap->StdMifareContainer.NdefBlocks) * 
        PH_FRINFC_MIFARESTD_BLOCK_BYTES) + 
        NdefMap->StdMifareContainer.internalLength) ==
        PH_FRINFC_MIFARESTD_VAL0)
    {
        NdefMap->StdMifareContainer.ReadWriteCompleteFlag = 
                        PH_FRINFC_MIFARESTD_FLAG1;
    }

    if(CRFlag == PH_FRINFC_MIFARESTD_FLAG1)
    {
        Result = (((RdFlag == PH_FRINFC_MIFARESTD_FLAG1) && 
                    (NdefMap->ApduBuffIndex == PH_FRINFC_MIFARESTD_VAL0))?
                    (PHNFCSTVAL( CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_EOF_NDEF_CONTAINER_REACHED)):
                    Result);
                    
        *NdefMap->NumOfBytesRead = NdefMap->ApduBuffIndex;
    }
    
    return Result;
}
#endif

static NFCSTATUS phFriNfc_MifStd_H_WriteNdefLen(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    NdefMap->State = PH_FRINFC_NDEFMAP_STATE_WR_NDEF_LEN;

    /* If Current block = Ndef TLV block then the starting point 
        is writing from type of TLV 
        Else */

    if(NdefMap->StdMifareContainer.currentBlock == 
                                NdefMap->TLVStruct.NdefTLVBlock)
    {

        if(NdefMap->TLVStruct.NULLTLVCount >= 
            PH_FRINFC_MIFARESTD_VAL2)
        {
            phFriNfc_MifStd_H_fillTLV1(NdefMap);
        }
        else
        {
            phFriNfc_MifStd_H_fillTLV2(NdefMap);
        }
    }
    else
    {
        if(NdefMap->TLVStruct.NULLTLVCount >= 
            PH_FRINFC_MIFARESTD_VAL2)
        {        
            phFriNfc_MifStd_H_fillTLV1_1(NdefMap);
        }
        else
        {
            phFriNfc_MifStd_H_fillTLV2_1(NdefMap);
        }
    }

    (void)memcpy( NdefMap->StdMifareContainer.Buffer,
            &(NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL1]),
            PH_FRINFC_MIFARESTD_BYTES_READ);


    /* Write from here */
    NdefMap->SendLength = MIFARE_MAX_SEND_BUF_TO_WRITE;

#ifndef PH_HAL4_ENABLE
    NdefMap->Cmd.MfCmd = phHal_eMifareCmdListMifareWrite16;
#else
    NdefMap->Cmd.MfCmd = phHal_eMifareWrite16;
#endif

    *NdefMap->SendRecvLength = NdefMap->TempReceiveLength;
    /* Call the Overlapped HAL Transceive function */ 
    Result = phFriNfc_OvrHal_Transceive(    NdefMap->LowerDevice,
                                            &NdefMap->MapCompletionInfo,
                                            NdefMap->psRemoteDevInfo,
                                            NdefMap->Cmd,
                                            &NdefMap->psDepAdditionalInfo,
                                            NdefMap->SendRecvBuf,
                                            NdefMap->SendLength,
                                            NdefMap->SendRecvBuf,
                                            NdefMap->SendRecvLength);
    
    return Result;
}
#if 0
static NFCSTATUS phFriNfc_MifStd_H_ChkNdefCmpltBlocks(phFriNfc_NdefMap_t   *NdefMap)
{
    NFCSTATUS   Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_INVALID_PARAMETER);
    /*  If NoOfNdefCompBlocks is found to be zero, 
        should we warn the user */
    if((NdefMap->StdMifareContainer.NoOfNdefCompBlocks != 
        PH_FRINFC_MIFARESTD_VAL0) && 
        (NdefMap->StdMifareContainer.NoOfNdefCompBlocks <= 
        PH_FRINFC_MIFARESTD4K_MAX_BLOCKS))
    {
        /* Reading/writing to the card can start only 
        from the block 4 */                            
        NdefMap->StdMifareContainer.currentBlock = 4;
        NdefMap->StdMifareContainer.AuthDone = PH_FRINFC_MIFARESTD_FLAG0;
        Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
        if((Result == NFCSTATUS_SUCCESS) && 
            (NdefMap->StdMifareContainer.AuthDone == 
            PH_FRINFC_MIFARESTD_FLAG1))
        {
            NdefMap->StdMifareContainer.ReadAcsBitFlag = 0;
            Result = phFriNfc_MifStd_H_RdAcsBit(NdefMap);
        }   
        else
        {
            Result = phFriNfc_MifStd_H_AuthSector(NdefMap);
        }       
    }
    return Result;
}
#endif

static void phFriNfc_MifStd_H_RdWrReset(phFriNfc_NdefMap_t   *NdefMap)
{
    NdefMap->StdMifareContainer.currentBlock = PH_FRINFC_MIFARESTD_BLK4;
    NdefMap->StdMifareContainer.NdefBlocks = PH_FRINFC_MIFARESTD_VAL1;
    NdefMap->TLVStruct.BytesRemainLinTLV = PH_FRINFC_MIFARESTD_VAL0;
    NdefMap->TLVStruct.LcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
    NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;
    NdefMap->TLVStruct.NdefTLVAuthFlag = PH_FRINFC_MIFARESTD_FLAG0;
    NdefMap->TLVStruct.NdefTLVBlock = PH_FRINFC_MIFARESTD_MAD_BLK0;
    NdefMap->TLVStruct.NdefTLVByte = PH_FRINFC_MIFARESTD_VAL0;
    NdefMap->TLVStruct.NoLbytesinTLV = PH_FRINFC_MIFARESTD_VAL0;
    NdefMap->TLVStruct.NULLTLVCount = PH_FRINFC_MIFARESTD_VAL0;
    NdefMap->StdMifareContainer.internalLength = PH_FRINFC_MIFARESTD_VAL0;
    NdefMap->StdMifareContainer.AuthDone = PH_FRINFC_MIFARESTD_FLAG0;
    NdefMap->StdMifareContainer.NFCforumSectFlag = PH_FRINFC_MIFARESTD_FLAG0;
    NdefMap->StdMifareContainer.FirstReadFlag = PH_FRINFC_MIFARESTD_FLAG1;
    NdefMap->StdMifareContainer.ReadWriteCompleteFlag = PH_FRINFC_MIFARESTD_FLAG0;
    NdefMap->StdMifareContainer.remainingSize = (uint16_t)
                        (NdefMap->StdMifareContainer.NoOfNdefCompBlocks * 
                            PH_FRINFC_MIFARESTD_BLOCK_BYTES);
    NdefMap->StdMifareContainer.WrLength = PH_FRINFC_MIFARESTD_VAL1;

}

static NFCSTATUS phFriNfc_MifStd_H_RdtoWrNdefLen(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS Result = NFCSTATUS_SUCCESS;
    NdefMap->State = PH_FRINFC_NDEFMAP_STATE_RD_TO_WR_NDEF_LEN;
    
    if(NdefMap->TLVStruct.NdefTLVAuthFlag == PH_FRINFC_MIFARESTD_FLAG1)
    {
        NdefMap->StdMifareContainer.AuthDone = PH_FRINFC_MIFARESTD_FLAG0;
        Result = phFriNfc_MifStd_H_AuthSector(NdefMap);
    }
    else
    {
        NdefMap->SendRecvBuf[0] = NdefMap->StdMifareContainer.currentBlock;
        NdefMap->SendLength = MIFARE_MAX_SEND_BUF_TO_READ;
        *NdefMap->SendRecvLength = NdefMap->TempReceiveLength;

#ifndef PH_HAL4_ENABLE
        NdefMap->Cmd.MfCmd = phHal_eMifareCmdListMifareRead;
#else
        NdefMap->Cmd.MfCmd = phHal_eMifareRead;
#endif
        /* Call the Overlapped HAL Transceive function */ 
        Result = phFriNfc_OvrHal_Transceive(    NdefMap->LowerDevice,
                                                &NdefMap->MapCompletionInfo,
                                                NdefMap->psRemoteDevInfo,
                                                NdefMap->Cmd,
                                                &NdefMap->psDepAdditionalInfo,
                                                NdefMap->SendRecvBuf,
                                                NdefMap->SendLength,
                                                NdefMap->SendRecvBuf,
                                                NdefMap->SendRecvLength);
    }

    return Result;
}

static void phFriNfc_MifStd_H_SetNdefBlkAuth(phFriNfc_NdefMap_t   *NdefMap)
{
    NdefMap->TLVStruct.NdefTLVAuthFlag = 
            ((phFriNfc_MifStd_H_GetSect(NdefMap->TLVStruct.NdefTLVBlock)
            == phFriNfc_MifStd_H_GetSect(NdefMap->StdMifareContainer.currentBlock))?
            PH_FRINFC_MIFARESTD_FLAG0:
            PH_FRINFC_MIFARESTD_FLAG1);
}

static NFCSTATUS phFriNfc_MifStd_H_GetActCardLen(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    NdefMap->State = PH_FRINFC_NDEFMAP_STATE_GET_ACT_CARDSIZE;
    NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_GET_ACTSIZE_OPE;

    Result = ((NdefMap->StdMifareContainer.AuthDone == 
            PH_FRINFC_MIFARESTD_FLAG0)?
            phFriNfc_MifStd_H_AuthSector(NdefMap):
            phFriNfc_MifStd_H_Rd16Bytes(NdefMap, 
                NdefMap->StdMifareContainer.currentBlock));

    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_ChkTLVs(phFriNfc_NdefMap_t *NdefMap,
                                           uint8_t            *CRFlag)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint16_t        TempLength = PH_FRINFC_MIFARESTD_VAL0,
                    ShiftLength = PH_FRINFC_MIFARESTD_VAL0;
    uint8_t         TL4bytesFlag = PH_FRINFC_MIFARESTD_FLAG0;
    NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_GET_ACTSIZE_OPE;
    TempLength = NdefMap->TLVStruct.NdefTLVByte;
    for(;;)
    {
#ifdef PH_HAL4_ENABLE
        if((NdefMap->SendRecvBuf[TempLength] != PH_FRINFC_MIFARESTD_TERMTLV_T) && 
            (NdefMap->SendRecvBuf[TempLength] != PH_FRINFC_MIFARESTD_NULLTLV_T) && 
            (NdefMap->SendRecvBuf[TempLength] != PH_FRINFC_MIFARESTD_NDEFTLV_T) && 
            (FALSE == NdefMap->TLVStruct.NdefTLVFoundFlag))
        {
            Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_NO_NDEF_SUPPORT);
            NdefMap->TLVStruct.BytesRemainLinTLV = 0;
            NdefMap->CardState = PH_NDEFMAP_CARD_STATE_INVALID;
            *CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
            break;

        }
        else 
#endif /* #ifdef PH_HAL4_ENABLE */
        if((NdefMap->SendRecvBuf[TempLength] != PH_FRINFC_MIFARESTD_TERMTLV_T) && 
            (NdefMap->SendRecvBuf[TempLength] != PH_FRINFC_MIFARESTD_NULLTLV_T))
        {
            if(NdefMap->SendRecvBuf[TempLength] == PH_FRINFC_MIFARESTD_NDEFTLV_T)
            {
                NdefMap->TLVStruct.NdefTLVBlock = 
                            NdefMap->StdMifareContainer.currentBlock;
                NdefMap->TLVStruct.NdefTLVByte = (uint8_t)TempLength;
                NdefMap->TLVStruct.NdefTLVFoundFlag = 
                            ((NdefMap->SendRecvBuf[TempLength] == 
                                PH_FRINFC_MIFARESTD_NDEFTLV_T)?
                                PH_FRINFC_MIFARESTD_FLAG1:
                                PH_FRINFC_MIFARESTD_FLAG0);

                NdefMap->TLVStruct.NULLTLVCount = ((NdefMap->TLVStruct.NULLTLVCount 
                                            == PH_FRINFC_MIFARESTD_VAL1)?
                                            PH_FRINFC_MIFARESTD_VAL0:
                                            NdefMap->TLVStruct.NULLTLVCount);
            }
            else
            {
                NdefMap->TLVStruct.NULLTLVCount = PH_FRINFC_MIFARESTD_VAL0;
            }

            TempLength++;
            if(TempLength == PH_FRINFC_MIFARESTD_BYTES_READ)
            {
                NdefMap->TLVStruct.TcheckedinTLVFlag = 
                                        PH_FRINFC_MIFARESTD_FLAG1;
                NdefMap->TLVStruct.NoLbytesinTLV = 
                                    PH_FRINFC_MIFARESTD_VAL3;
            }
            Result = phFriNfc_MifStd_H_Chk16Bytes(  NdefMap,
                                                    TempLength);
            if(Result != NFCSTATUS_SUCCESS)
            {
                *CRFlag = (uint8_t)((Result == NFCSTATUS_PENDING)?
                            PH_FRINFC_MIFARESTD_FLAG0:
                            PH_FRINFC_MIFARESTD_FLAG1);
                break;
            }
            
            if(((((  NdefMap->StdMifareContainer.NoOfNdefCompBlocks -
                        NdefMap->StdMifareContainer.NdefBlocks) * 
                        PH_FRINFC_MIFARESTD_BLOCK_BYTES) + 
                        (PH_FRINFC_MIFARESTD_BLOCK_BYTES - 
                        TempLength)) <
                        NdefMap->SendRecvBuf[TempLength]) && 
                        ((NdefMap->SendRecvBuf[TempLength] < 
                        PH_FRINFC_MIFARESTD_NDEFTLV_L) && 
                        (NdefMap->TLVStruct.NdefTLVFoundFlag != 
                        PH_FRINFC_MIFARESTD_VAL1)))
            {
                /* Result = Error */
                Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_PARAMETER);
                *CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
                break;
            }

            if(((((  NdefMap->StdMifareContainer.NoOfNdefCompBlocks -
                        NdefMap->StdMifareContainer.NdefBlocks) * 
                        PH_FRINFC_MIFARESTD_BLOCK_BYTES) + 
                        (PH_FRINFC_MIFARESTD_BLOCK_BYTES - 
                        TempLength)) <
                        NdefMap->SendRecvBuf[TempLength]) && 
                        ((NdefMap->SendRecvBuf[TempLength] == 
                        PH_FRINFC_MIFARESTD_VAL0) && 
                        (NdefMap->TLVStruct.NdefTLVFoundFlag == 
                        PH_FRINFC_MIFARESTD_VAL1)))
            {
                /* Result = Error */
                Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_PARAMETER);
                *CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
                break;
            }

            if((NdefMap->TLVStruct.NdefTLVFoundFlag == 
                PH_FRINFC_MIFARESTD_FLAG1) && 
                (NdefMap->SendRecvBuf[TempLength] < 
                 PH_FRINFC_MIFARESTD_NDEFTLV_L))
            {
                Result = phFriNfc_MapTool_SetCardState(NdefMap, 
                                    NdefMap->SendRecvBuf[TempLength]);
                NdefMap->TLVStruct.BytesRemainLinTLV = 
                                    NdefMap->SendRecvBuf[TempLength];
                NdefMap->StdMifareContainer.remainingSize -= 
                                            PH_FRINFC_MIFARESTD_VAL2;
                /* This flag is set */
                NdefMap->StdMifareContainer.remSizeUpdFlag = 
                    (uint8_t)((NdefMap->TLVStruct.NULLTLVCount >= 
                    PH_FRINFC_MIFARESTD_VAL2)?
                    PH_FRINFC_MIFARESTD_FLAG0:
                    PH_FRINFC_MIFARESTD_FLAG1);

                *CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
                break;
            }
            
            NdefMap->StdMifareContainer.remainingSize -= 
                    ((  NdefMap->SendRecvBuf[TempLength] < 
                            PH_FRINFC_MIFARESTD_NDEFTLV_L)?
                            (NdefMap->SendRecvBuf[TempLength] 
                            + PH_FRINFC_MIFARESTD_VAL2):
                            PH_FRINFC_MIFARESTD_VAL0);

            if(( NdefMap->SendRecvBuf[TempLength] == 
                        PH_FRINFC_MIFARESTD_VAL0))
            {
                Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_INVALID_PARAMETER);
                *CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
                break;
            }

            TL4bytesFlag = PH_FRINFC_MIFARESTD_FLAG0;
            /* get the next TLV after the proprietary TLV */
            Result = 
                ((NdefMap->SendRecvBuf[TempLength] < 
                    PH_FRINFC_MIFARESTD_NDEFTLV_L)?
                    phFriNfc_MifStd_H_GetNxtTLV(NdefMap, &TempLength, &TL4bytesFlag):
                    NFCSTATUS_PENDING);

            if((TempLength >= PH_FRINFC_MIFARESTD_BYTES_READ) && 
                (Result == NFCSTATUS_SUCCESS))
            {
                NdefMap->TLVStruct.TcheckedinTLVFlag = 
                                        PH_FRINFC_MIFARESTD_FLAG0;
                NdefMap->TLVStruct.NoLbytesinTLV = 
                                    PH_FRINFC_MIFARESTD_VAL0;
                
                Result = phFriNfc_MifStd_H_GetActCardLen(NdefMap);
                *CRFlag = (uint8_t)((Result != NFCSTATUS_PENDING)?
                            PH_FRINFC_MIFARESTD_FLAG1:
                            PH_FRINFC_MIFARESTD_FLAG0);
                break;
            }
            else
            {
                if(Result == NFCSTATUS_PENDING)
                {
                    TL4bytesFlag = PH_FRINFC_MIFARESTD_FLAG1;
                    Result = ((NdefMap->SendRecvBuf[TempLength] == 
                            PH_FRINFC_MIFARESTD_NDEFTLV_L) ?
                            NFCSTATUS_SUCCESS:
                            (PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_PARAMETER)));
                    
                    if(Result != NFCSTATUS_SUCCESS)
                    {
                        *CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
                        break;
                    }
                    NdefMap->TLVStruct.NULLTLVCount = PH_FRINFC_MIFARESTD_VAL0;
                    TempLength++;
                    /* Check 0xFF */
                    if(TempLength == PH_FRINFC_MIFARESTD_BYTES_READ)
                    {
                        NdefMap->TLVStruct.TcheckedinTLVFlag = 
                                                PH_FRINFC_MIFARESTD_FLAG1;
                        NdefMap->TLVStruct.NoLbytesinTLV = 
                                            PH_FRINFC_MIFARESTD_VAL2;
                    }
                    Result = phFriNfc_MifStd_H_Chk16Bytes(  NdefMap,
                                                            TempLength);
                    if(Result != NFCSTATUS_SUCCESS)
                    {
                        break;
                    }

                    ShiftLength = NdefMap->SendRecvBuf[TempLength];
                    TempLength++;
                    if(TempLength == PH_FRINFC_MIFARESTD_BYTES_READ)
                    {
                        NdefMap->TLVStruct.TcheckedinTLVFlag = 
                                                PH_FRINFC_MIFARESTD_FLAG1;
                        NdefMap->TLVStruct.NoLbytesinTLV = 
                                            PH_FRINFC_MIFARESTD_VAL1;
                        NdefMap->TLVStruct.prevLenByteValue = 
                                    NdefMap->SendRecvBuf[(TempLength - 
                                                PH_FRINFC_MIFARESTD_VAL1)];
                    }
                    Result = phFriNfc_MifStd_H_Chk16Bytes(  NdefMap,
                                                            TempLength);
                    if(Result != NFCSTATUS_SUCCESS)
                    {
                        break;
                    }
                    
                    
                    if((((  NdefMap->StdMifareContainer.NoOfNdefCompBlocks -
                        NdefMap->StdMifareContainer.NdefBlocks) * 
                        PH_FRINFC_MIFARESTD_BLOCK_BYTES) + 
                        (PH_FRINFC_MIFARESTD_BLOCK_BYTES - 
                        TempLength)) <
                        (( ShiftLength
                        << 8) + NdefMap->SendRecvBuf[TempLength]))
                    {
                        Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_REMOTE_DEVICE);
                        
                        break;
                    }
                    
                    if(NdefMap->TLVStruct.NdefTLVFoundFlag == 
                        PH_FRINFC_MIFARESTD_FLAG1)
                    {
                        ShiftLength = (( ShiftLength<< 8) + 
                                    NdefMap->SendRecvBuf[TempLength]);
                        NdefMap->TLVStruct.BytesRemainLinTLV = ShiftLength;
                        Result = phFriNfc_MapTool_SetCardState(NdefMap, 
                                                                ShiftLength);
                        NdefMap->StdMifareContainer.remainingSize -= 
                                            PH_FRINFC_MIFARESTD_VAL4;
                        *CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
                        break;
                    }
                    
                    NdefMap->StdMifareContainer.remainingSize -= 
                                ((ShiftLength<< 8) + 
                                NdefMap->SendRecvBuf[(TempLength + PH_FRINFC_MIFARESTD_VAL1)]);
                    TempLength++;

                    /* get the next TLV after the proprietary TLV */
                    Result = phFriNfc_MifStd_H_GetNxtTLV(NdefMap, &TempLength, &TL4bytesFlag);

                    if((TempLength >= PH_FRINFC_MIFARESTD_BYTES_READ) && 
                        (Result == NFCSTATUS_SUCCESS))
                    {
                        NdefMap->TLVStruct.TcheckedinTLVFlag = 
                                                PH_FRINFC_MIFARESTD_FLAG0;
                        NdefMap->TLVStruct.NoLbytesinTLV = 
                                            PH_FRINFC_MIFARESTD_VAL0;
                        Result = phFriNfc_MifStd_H_GetActCardLen(NdefMap);

                        break;
                    }
                    break;
                }
            }
        }
        else if((NdefMap->SendRecvBuf[TempLength] == PH_FRINFC_MIFARESTD_TERMTLV_T) && 
                (NdefMap->TLVStruct.NdefTLVFoundFlag == PH_FRINFC_MIFARESTD_FLAG0))
        {
            Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_PARAMETER);
            NdefMap->StdMifareContainer.ReadWriteCompleteFlag =
                                        PH_FRINFC_MIFARESTD_FLAG1;
            break;

        }
        else if(NdefMap->SendRecvBuf[TempLength] == PH_FRINFC_MIFARESTD_NULLTLV_T)
        {
            TempLength++;
            NdefMap->TLVStruct.NULLTLVCount += PH_FRINFC_MIFARESTD_VAL1;
            ShiftLength = NdefMap->SendRecvBuf[(TempLength - PH_FRINFC_MIFARESTD_VAL1)];
            NdefMap->StdMifareContainer.remainingSize -= PH_FRINFC_MIFARESTD_VAL1;
            if(NdefMap->StdMifareContainer.remainingSize <
                    (( ShiftLength << 8) + NdefMap->SendRecvBuf[TempLength]))
            {
                Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_REMOTE_DEVICE);
                break;
            }
            Result = phFriNfc_MifStd_H_Chk16Bytes(  NdefMap,
                                                    TempLength);
            if(Result != NFCSTATUS_SUCCESS)
            {
                NdefMap->TLVStruct.NdefTLVByte = PH_FRINFC_MIFARESTD_VAL0;
                break;
            }
        }
        else
        {
            if((NdefMap->SendRecvBuf[TempLength] == PH_FRINFC_MIFARESTD_TERMTLV_T) && 
                (NdefMap->TLVStruct.NdefTLVFoundFlag == PH_FRINFC_MIFARESTD_FLAG1))
            {
                TempLength++;
                Result = NFCSTATUS_SUCCESS;
                NdefMap->StdMifareContainer.remainingSize -= 
                                                PH_FRINFC_MIFARESTD_VAL1;
            }
        }
    }

    if(NdefMap->TLVStruct.BytesRemainLinTLV > 
        NdefMap->StdMifareContainer.remainingSize)
    {
        Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_FORMAT);
    }
    else
    {
        if(NdefMap->StdMifareContainer.remainingSize ==
                PH_FRINFC_MIFARESTD_VAL0)
        {
            Result = ((NdefMap->TLVStruct.NdefTLVFoundFlag == 
                        PH_FRINFC_MIFARESTD_FLAG1)?
                        NFCSTATUS_SUCCESS:
                        (PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_INVALID_PARAMETER)));
        }
    }
    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_GetNxtTLV(phFriNfc_NdefMap_t    *NdefMap,
                                      uint16_t              *TempLength,
                                       uint8_t              *TL4bytesFlag)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint16_t    LengthRemaining = PH_FRINFC_MIFARESTD_VAL0,
                TempLen = PH_FRINFC_MIFARESTD_VAL0,
                ShiftLength = PH_FRINFC_MIFARESTD_VAL0;
    
    TempLen = (*TempLength);
    LengthRemaining = (PH_FRINFC_MIFARESTD_BYTES_READ - 
                        (TempLen + PH_FRINFC_MIFARESTD_VAL1));
    
    if(*TL4bytesFlag == PH_FRINFC_MIFARESTD_FLAG0)
    {
        (*TempLength) += (NdefMap->SendRecvBuf[TempLen] + 
                            PH_FRINFC_MIFARESTD_VAL1);

        if(NdefMap->TLVStruct.NdefTLVFoundFlag == PH_FRINFC_MIFARESTD_FLAG0)
        {
            LengthRemaining = 
                (((*TempLength) < PH_FRINFC_MIFARESTD_BYTES_READ)?
                PH_FRINFC_MIFARESTD_VAL0:
                (NdefMap->SendRecvBuf[TempLen] - 
                LengthRemaining));
        }
        else
        {
            LengthRemaining = 
                (((*TempLength) < PH_FRINFC_MIFARESTD_BYTES_READ)?
                PH_FRINFC_MIFARESTD_VAL0:
                (NdefMap->SendRecvBuf[TempLen] - 
                LengthRemaining));
        }
    }
    else
    {
        *TL4bytesFlag = PH_FRINFC_MIFARESTD_FLAG0;
        if(NdefMap->TLVStruct.NoLbytesinTLV == 
            PH_FRINFC_MIFARESTD_VAL1)
        {
            ShiftLength = NdefMap->TLVStruct.prevLenByteValue;
            (*TempLength) += ((ShiftLength 
                                << 8) + NdefMap->SendRecvBuf[TempLen] + 
                            PH_FRINFC_MIFARESTD_VAL1);

            LengthRemaining = 
                (((ShiftLength 
                << 8) + NdefMap->SendRecvBuf[TempLen]) - 
                LengthRemaining);
        }
        else
        {
            ShiftLength = NdefMap->SendRecvBuf[(TempLen - PH_FRINFC_MIFARESTD_VAL1)];
            (*TempLength) += ((ShiftLength 
                                << 8) + NdefMap->SendRecvBuf[TempLen] + 
                            PH_FRINFC_MIFARESTD_VAL1);

            LengthRemaining = 
                (((ShiftLength 
                    << 8) + NdefMap->SendRecvBuf[TempLen]) - 
                    LengthRemaining);
        }
    }
    
    NdefMap->TLVStruct.NdefTLVByte = 
        (uint8_t)(((*TempLength) < PH_FRINFC_MIFARESTD_BYTES_READ)?
        (*TempLength):
        (LengthRemaining % PH_FRINFC_MIFARESTD_BYTES_READ));

    while(LengthRemaining != PH_FRINFC_MIFARESTD_VAL0)
    {
        NdefMap->StdMifareContainer.currentBlock++;
        NdefMap->StdMifareContainer.NdefBlocks++;
        Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
        LengthRemaining -= 
            ((LengthRemaining <= PH_FRINFC_MIFARESTD_BYTES_READ)?
            LengthRemaining:
            PH_FRINFC_MIFARESTD_BYTES_READ);
    }
    
    if(NdefMap->TLVStruct.NdefTLVByte == PH_FRINFC_MIFARESTD_VAL0)
    {
        NdefMap->StdMifareContainer.currentBlock++;
        NdefMap->StdMifareContainer.NdefBlocks++;
        Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
    }
    
    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_Chk16Bytes(phFriNfc_NdefMap_t   *NdefMap,
                                       uint16_t             TempLength)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    if(TempLength == PH_FRINFC_MIFARESTD_BYTES_READ)
    {
        NdefMap->StdMifareContainer.currentBlock++;
        NdefMap->StdMifareContainer.NdefBlocks++;
        Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
                    
        Result = 
            ((NdefMap->StdMifareContainer.AuthDone == PH_FRINFC_MIFARESTD_FLAG1)?
            phFriNfc_MifStd_H_GetActCardLen(NdefMap):
            phFriNfc_MifStd_H_AuthSector(NdefMap));      
    }
    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_ChkRemainTLVs(phFriNfc_NdefMap_t *NdefMap, 
                                          uint8_t            *CRFlag,
                                          uint8_t            *NDEFFlag)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint16_t    TempLength = PH_FRINFC_MIFARESTD_VAL0,
                ShiftLength = PH_FRINFC_MIFARESTD_VAL0;
    uint8_t     TL4bytesFlag = PH_FRINFC_MIFARESTD_FLAG0;

    switch(NdefMap->TLVStruct.NoLbytesinTLV)
    {
        case PH_FRINFC_MIFARESTD_VAL3:
            /* if TLV is found then set card state */
            Result = ((NdefMap->TLVStruct.NdefTLVFoundFlag == 
                        PH_FRINFC_MIFARESTD_FLAG1)?
                        phFriNfc_MapTool_SetCardState(NdefMap, 
                        NdefMap->SendRecvBuf[TempLength]):
                        Result);

            /* Check the length field is less than or  
                equal to 0xFF if yes enter below statement 
                else enter else if*/
            if((NdefMap->SendRecvBuf[TempLength] < 
                PH_FRINFC_MIFARESTD_NDEFTLV_L) && 
                (Result == NFCSTATUS_SUCCESS))
            {
                NdefMap->StdMifareContainer.remainingSize -= 
                                PH_FRINFC_MIFARESTD_VAL2;
                
                Result = ((NdefMap->SendRecvBuf[TempLength] > 
                            NdefMap->StdMifareContainer.remainingSize)?
                            (PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_INVALID_FORMAT)):
                            Result);
                TL4bytesFlag = PH_FRINFC_MIFARESTD_FLAG0;
                if((NdefMap->TLVStruct.NdefTLVFoundFlag == 
                    PH_FRINFC_MIFARESTD_FLAG1) && 
                    (Result == NFCSTATUS_SUCCESS))
                {
                    NdefMap->TLVStruct.BytesRemainLinTLV = 
                    NdefMap->SendRecvBuf[TempLength];
                    *CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
                    
                }
                else if(Result == NFCSTATUS_SUCCESS)
                {
                    TempLength++;
                    Result = phFriNfc_MifStd_H_GetNxtTLV(NdefMap, 
                                    &TempLength, &TL4bytesFlag);

                    NdefMap->StdMifareContainer.remainingSize -= 
                                    NdefMap->SendRecvBuf[TempLength];
                    if((TempLength >= PH_FRINFC_MIFARESTD_BYTES_READ) && 
                        (*CRFlag == PH_FRINFC_MIFARESTD_FLAG0))
                    {
                        *NDEFFlag = PH_FRINFC_MIFARESTD_FLAG0;
                        Result = phFriNfc_MifStd_H_GetActCardLen(NdefMap);
                    }
                }
                
                else
                {
                    /* do nothing */
                }
            }
            else if((NdefMap->SendRecvBuf[TempLength] == 
                    PH_FRINFC_MIFARESTD_NDEFTLV_L) && 
                    (Result == NFCSTATUS_SUCCESS))
            {
                TempLength++;
                NdefMap->StdMifareContainer.remainingSize -= 
                                PH_FRINFC_MIFARESTD_VAL4;
                TL4bytesFlag = PH_FRINFC_MIFARESTD_FLAG0;
                Result = (((((uint16_t)NdefMap->SendRecvBuf[TempLength] << 8) + 
                                NdefMap->SendRecvBuf[(TempLength + 
                                PH_FRINFC_MIFARESTD_VAL1)]) > 
                                NdefMap->StdMifareContainer.remainingSize)?
                                (PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_FORMAT)):
                                Result);
                if((NdefMap->TLVStruct.NdefTLVFoundFlag == 
                    PH_FRINFC_MIFARESTD_FLAG1) && 
                    (Result == NFCSTATUS_SUCCESS))
                {
                    NdefMap->TLVStruct.NULLTLVCount = PH_FRINFC_MIFARESTD_VAL0;
                    NdefMap->TLVStruct.BytesRemainLinTLV = 
                                (((uint16_t)NdefMap->SendRecvBuf[TempLength] << 8) + 
                                NdefMap->SendRecvBuf[(TempLength + 
                                PH_FRINFC_MIFARESTD_VAL1)]);
                    *CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
                }
                else if(Result == NFCSTATUS_SUCCESS)
                {
                    TempLength++;
                    
                    Result = phFriNfc_MifStd_H_GetNxtTLV(NdefMap, 
                                &TempLength, &TL4bytesFlag);
                    NdefMap->StdMifareContainer.remainingSize -= 
                                (((uint16_t)NdefMap->SendRecvBuf[TempLength] << 8) + 
                                NdefMap->SendRecvBuf[(TempLength + 
                                PH_FRINFC_MIFARESTD_VAL1)]);

                    *NDEFFlag = PH_FRINFC_MIFARESTD_FLAG0;
                    Result = phFriNfc_MifStd_H_GetActCardLen(NdefMap);
                }
                else
                {
                    /* do nothing */
                    *CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
                }
            }
            else
            {
                /* Result = Error */
                Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_FORMAT);
                *CRFlag = PH_FRINFC_MIFARESTD_FLAG1;
            }
            break;

        case PH_FRINFC_MIFARESTD_VAL2:
        case PH_FRINFC_MIFARESTD_VAL1:
            ShiftLength = ((NdefMap->TLVStruct.NoLbytesinTLV == 
                            PH_FRINFC_MIFARESTD_VAL1)?
                            ((NdefMap->TLVStruct.prevLenByteValue << 8) + 
                            NdefMap->SendRecvBuf[TempLength]):
                            (((uint16_t)NdefMap->SendRecvBuf[TempLength] << 8) + 
                            NdefMap->SendRecvBuf[(TempLength + 
                            PH_FRINFC_MIFARESTD_VAL1)]));
            if((((  NdefMap->StdMifareContainer.NoOfNdefCompBlocks -
                    NdefMap->StdMifareContainer.NdefBlocks) * 
                    PH_FRINFC_MIFARESTD_BLOCK_BYTES) + 
                    (PH_FRINFC_MIFARESTD_BLOCK_BYTES - 
                    TempLength)) <
                    ShiftLength)
            {
                /* Result = Error */
                Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_PARAMETER);
                *CRFlag = PH_FRINFC_MIFARESTD_FLAG1;                    
            }
            else
            {
                NdefMap->StdMifareContainer.remainingSize -= 
                                    PH_FRINFC_MIFARESTD_VAL2;
                if(NdefMap->TLVStruct.NdefTLVFoundFlag == 
                        PH_FRINFC_MIFARESTD_FLAG1)
                {
                    NdefMap->TLVStruct.BytesRemainLinTLV = ShiftLength;
                    if(NdefMap->TLVStruct.BytesRemainLinTLV > 
                        NdefMap->StdMifareContainer.remainingSize)
                    {
                        Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                                NFCSTATUS_INVALID_FORMAT);
                    }
                    *CRFlag = PH_FRINFC_MIFARESTD_FLAG1; 
                    *NDEFFlag = PH_FRINFC_MIFARESTD_FLAG0;
                }
                else
                {
                    NdefMap->StdMifareContainer.remainingSize -= 
                                        ShiftLength;
                    *CRFlag = PH_FRINFC_MIFARESTD_FLAG0; 
                    TempLength += PH_FRINFC_MIFARESTD_VAL2;
                    TL4bytesFlag = PH_FRINFC_MIFARESTD_FLAG1;
                    Result = ((NdefMap->TLVStruct.NdefTLVFoundFlag == PH_FRINFC_MIFARESTD_FLAG1)?
                                NFCSTATUS_SUCCESS:
                                phFriNfc_MifStd_H_GetNxtTLV(NdefMap, &TempLength, &TL4bytesFlag));

                    *NDEFFlag = PH_FRINFC_MIFARESTD_FLAG0;
                    Result = phFriNfc_MifStd_H_GetActCardLen(NdefMap);
                }
            }
            break;

        default:
            break;
    }
    return Result;
}

static void phFriNfc_MifStd_H_Get1kStTrail(phFriNfc_NdefMap_t  *NdefMap)
{
    switch((NdefMap->StdMifareContainer.currentBlock % 4))
    {
        case PH_FRINFC_MIFARESTD_VAL0:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                            (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_MAD_BLK3);
            break;

        case PH_FRINFC_MIFARESTD_VAL1:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_MAD_BLK2);
            break;

        case PH_FRINFC_MIFARESTD_VAL2:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_MAD_BLK1);
            break;

        default:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        NdefMap->StdMifareContainer.currentBlock;
            break;
    }
}

static void phFriNfc_MifStd_H_Get4kStTrail(phFriNfc_NdefMap_t  *NdefMap)
{
    switch((NdefMap->StdMifareContainer.currentBlock % 16))
    {
        case PH_FRINFC_MIFARESTD_MAD_BLK0:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                            (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_BLK15);
            break;

        case PH_FRINFC_MIFARESTD_MAD_BLK1:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_BLK14);
            break;

        case PH_FRINFC_MIFARESTD_MAD_BLK2:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_BLK13);
            break;

        case PH_FRINFC_MIFARESTD_MAD_BLK3:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_BLK12);
            break;

        case PH_FRINFC_MIFARESTD_BLK4:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_BLK11);
            break;
        
        case PH_FRINFC_MIFARESTD_BLK5:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_BLK10);
            break;
        
        case PH_FRINFC_MIFARESTD_BLK6:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_BLK9);
            break;

        case PH_FRINFC_MIFARESTD_BLK7:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_BLK8);
            break;

        case PH_FRINFC_MIFARESTD_BLK8:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_BLK7);
            break;

        case PH_FRINFC_MIFARESTD_BLK9:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_BLK6);
            break;

        case PH_FRINFC_MIFARESTD_BLK10:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_BLK5);
            break;

        case PH_FRINFC_MIFARESTD_BLK11:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_BLK4);
            break;

        case PH_FRINFC_MIFARESTD_BLK12:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_MAD_BLK3);
            break;

        case PH_FRINFC_MIFARESTD_BLK13:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_MAD_BLK2);
            break;

        case PH_FRINFC_MIFARESTD_BLK14:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        (NdefMap->StdMifareContainer.currentBlock + 
                                        PH_FRINFC_MIFARESTD_MAD_BLK1);
            break;

        default:
            NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                        NdefMap->StdMifareContainer.currentBlock;
            break;
    }
}

static NFCSTATUS phFriNfc_MifStd_H_ProChkNdef(phFriNfc_NdefMap_t        *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    /* Copy remaining bytes into the AID array
        from Receive Buffer till array number 7 in aid */
    if(NdefMap->StdMifareContainer.currentBlock == 
        PH_FRINFC_MIFARESTD_VAL1)
    {
        /* Helper Function to Store AID Information */
        phFriNfc_MifStd_H_fillAIDarray(NdefMap);
        
        NdefMap->StdMifareContainer.currentBlock = PH_FRINFC_MIFARESTD_VAL2;
        /* read remaining AIDs from block number 2 */
        Result = ((NdefMap->StdMifareContainer.aidCompleteFlag == 
                PH_FRINFC_MIFARESTD_FLAG1)?
                Result:
                phFriNfc_MifareStdMap_ChkNdef( NdefMap));
    }
    else if(((NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD) &&
            (NdefMap->StdMifareContainer.currentBlock == 
            PH_FRINFC_MIFARESTD_MAD_BLK2)) || (
            (NdefMap->StdMifareContainer.currentBlock == 
            PH_FRINFC_MIFARESTD_MAD_BLK66) && 
            (NdefMap->CardType == 
            PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD)))
    {
        /* Helper Function to Store AID Information */
        phFriNfc_MifStd_H_fillAIDarray(NdefMap);
                                    
        NdefMap->StdMifareContainer.aidCompleteFlag = 
                        PH_FRINFC_MIFARESTD_FLAG1;
    }/* Mifare 1k and Mifare 4k end Check */
    else if((NdefMap->StdMifareContainer.currentBlock > 
            PH_FRINFC_MIFARESTD_VAL1) && 
            (NdefMap->CardType == 
            PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD))
    {   
        phFriNfc_MifStd_H_fillAIDarray(NdefMap);
        /* read remaining AIDs from block number 2 */
        /* Mifare 4k Helper Function */  
        Result = ((NdefMap->StdMifareContainer.aidCompleteFlag == 
                    PH_FRINFC_MIFARESTD_FLAG1)?
                    Result:
                    phFriNfc_MifStd4k_H_CheckNdef(NdefMap));
    } /* Card Type 4k Check */
    else
    {
        /* Since we have decided temporarily not to go
            for any new error codes we are using 
            NFCSTATUS_INVALID_PARAMETER even though it is not 
            the relevant error code here TBD */
        Result = PHNFCSTVAL(    CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_PARAMETER); 
    }

    if(NdefMap->StdMifareContainer.aidCompleteFlag == 
                        PH_FRINFC_MIFARESTD_FLAG1)
    {
        NdefMap->StdMifareContainer.ChkNdefCompleteFlag = 
                        PH_FRINFC_MIFARESTD_FLAG1;
        /*  The check for NDEF compliant information is now over for
            the Mifare 1K card.
            Update(decrement) the NoOfNdefCompBlocks as much required,
            depending on the NDEF compliant information found */
        /* Check the Sectors are Ndef Compliant */
        phFriNfc_MifStd_H_ChkNdefCmpltSects(NdefMap);
        if((NdefMap->StdMifareContainer.NoOfNdefCompBlocks == 0) || 
            (NdefMap->StdMifareContainer.NoOfNdefCompBlocks > 255))
        {
            Result = PHNFCSTVAL( CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_NO_NDEF_SUPPORT);
        }
        else
        {
            NdefMap->StdMifareContainer.aidCompleteFlag = 
                            PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->StdMifareContainer.NFCforumSectFlag = 
                                    PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->StdMifareContainer.currentBlock = PH_FRINFC_MIFARESTD_BLK4;
            Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
            Result = ((Result != NFCSTATUS_SUCCESS)?
                        Result:phFriNfc_MifStd_H_AuthSector(NdefMap));
        }
        
    }
    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_ProAuth(phFriNfc_NdefMap_t       *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    if(NdefMap->TLVStruct.NdefTLVAuthFlag == 
       PH_FRINFC_MIFARESTD_FLAG1)
    {
        NdefMap->TLVStruct.NdefTLVAuthFlag = 
                                    PH_FRINFC_MIFARESTD_FLAG0;
        NdefMap->StdMifareContainer.AuthDone = PH_FRINFC_MIFARESTD_FLAG1;
        Result = phFriNfc_MifStd_H_RdtoWrNdefLen(NdefMap);                   
    }
    else
    {
        NdefMap->StdMifareContainer.AuthDone = 1;
        NdefMap->StdMifareContainer.ReadAcsBitFlag = 1;
        Result = phFriNfc_MifStd_H_RdAcsBit(NdefMap);
    }
    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_Rd16Bytes(phFriNfc_NdefMap_t     *NdefMap,
                                              uint8_t               BlockNo)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = BlockNo;
    NdefMap->SendLength = MIFARE_MAX_SEND_BUF_TO_READ;
    *NdefMap->SendRecvLength = NdefMap->TempReceiveLength;
#ifndef PH_HAL4_ENABLE
    NdefMap->Cmd.MfCmd = phHal_eMifareCmdListMifareRead;
#else
    NdefMap->Cmd.MfCmd = phHal_eMifareRead;
#endif
    NdefMap->MapCompletionInfo.CompletionRoutine = phFriNfc_MifareStdMap_Process;
    NdefMap->MapCompletionInfo.Context = NdefMap; 
    /* Call the Overlapped HAL Transceive function */ 
    Result = phFriNfc_OvrHal_Transceive(    NdefMap->LowerDevice,
                                            &NdefMap->MapCompletionInfo,
                                            NdefMap->psRemoteDevInfo,
                                            NdefMap->Cmd,
                                            &NdefMap->psDepAdditionalInfo,
                                            NdefMap->SendRecvBuf,
                                            NdefMap->SendLength,
                                            NdefMap->SendRecvBuf,
                                            NdefMap->SendRecvLength);
    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_ProAcsBits(phFriNfc_NdefMap_t        *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint8_t     CRFlag = PH_FRINFC_MIFARESTD_FLAG0;
    if(*NdefMap->SendRecvLength == PH_FRINFC_MIFARESTD_BYTES_READ)
    {
        if(NdefMap->StdMifareContainer.ReadAcsBitFlag == 
            PH_FRINFC_MIFARESTD_FLAG1)
        {
            /* check for the correct access bits */
            Result = phFriNfc_MifStd_H_ChkAcsBit(NdefMap);
         
            if((NdefMap->StdMifareContainer.ChkNdefFlag == 
                PH_FRINFC_MIFARESTD_FLAG1) && 
                (Result == NFCSTATUS_SUCCESS))
            {
                if(NdefMap->CardState == 
                    PH_NDEFMAP_CARD_STATE_INVALID)
                {
                    NdefMap->StdMifareContainer.NoOfNdefCompBlocks = 
                    ((NdefMap->StdMifareContainer.currentBlock >= 
                    PH_FRINFC_MIFARESTD4K_BLK128)? 
                    (NdefMap->StdMifareContainer.NoOfNdefCompBlocks - 
                                    PH_FRINFC_MIFARESTD_BLK15):
                    (NdefMap->StdMifareContainer.NoOfNdefCompBlocks - 
                                    PH_FRINFC_MIFARESTD_MAD_BLK3));

                    NdefMap->StdMifareContainer.ProprforumSectFlag = 
                        ((NdefMap->StdMifareContainer.NFCforumSectFlag == 
                        PH_FRINFC_MIFARESTD_FLAG1)?
                        PH_FRINFC_MIFARESTD_PROP_2ND_CONFIG:
                        PH_FRINFC_MIFARESTD_PROP_3RD_CONFIG);

                    Result = phFriNfc_MifStd_H_ProStatNotValid(NdefMap, Result);
                }
                else
                {
                    NdefMap->StdMifareContainer.NFCforumSectFlag = 
                        (((NdefMap->StdMifareContainer.currentBlock == 64) && 
                        (NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD))?
                        NdefMap->StdMifareContainer.NFCforumSectFlag:
                                            PH_FRINFC_MIFARESTD_FLAG1);
                }

                if(NdefMap->StdMifareContainer.ProprforumSectFlag != 
                    PH_FRINFC_MIFARESTD_PROP_2ND_CONFIG)
                {
                    NdefMap->StdMifareContainer.ReadAcsBitFlag = PH_FRINFC_MIFARESTD_FLAG0;
                                /* ((NdefMap->StdMifareContainer.ReadCompleteFlag == 
                                        PH_FRINFC_MIFARESTD_FLAG1)?
                                        PH_FRINFC_MIFARESTD_FLAG0:
                                        PH_FRINFC_MIFARESTD_FLAG1);*/

                    NdefMap->StdMifareContainer.ReadCompleteFlag = 
                    (uint8_t)((((((NdefMap->StdMifareContainer.currentBlock + 
                        PH_FRINFC_MIFARESTD_VAL4) >= 
                        PH_FRINFC_MIFARESTD1K_MAX_BLK) && 
                        (NdefMap->CardType == 
                        PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD)) && 
                        (NdefMap->StdMifareContainer.ReadCompleteFlag == 
                        PH_FRINFC_MIFARESTD_FLAG0)) || 
                        (NdefMap->StdMifareContainer.ReadCompleteFlag == 
                        PH_FRINFC_MIFARESTD_FLAG1))?
                        PH_FRINFC_MIFARESTD_FLAG1:
                        PH_FRINFC_MIFARESTD_FLAG0);

                    NdefMap->StdMifareContainer.ReadCompleteFlag = 
                    (uint8_t)((((((uint16_t)(NdefMap->StdMifareContainer.currentBlock + 
                        PH_FRINFC_MIFARESTD_VAL4) >= 
                        PH_FRINFC_MIFARESTD4K_MAX_BLK) && 
                        (NdefMap->CardType == 
                        PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD)) && 
                        (NdefMap->StdMifareContainer.ReadCompleteFlag == 
                        PH_FRINFC_MIFARESTD_FLAG0)) || 
                        (NdefMap->StdMifareContainer.ReadCompleteFlag == 
                        PH_FRINFC_MIFARESTD_FLAG1))?
                        PH_FRINFC_MIFARESTD_FLAG1:
                        PH_FRINFC_MIFARESTD_FLAG0);

                    NdefMap->StdMifareContainer.currentBlock = 
                        ((NdefMap->StdMifareContainer.ReadCompleteFlag == 
                            PH_FRINFC_MIFARESTD_FLAG1)?
                            PH_FRINFC_MIFARESTD_BLK4:
                            NdefMap->StdMifareContainer.currentBlock);

                    Result = 
                    ((NdefMap->StdMifareContainer.ReadCompleteFlag == 
                            PH_FRINFC_MIFARESTD_FLAG1)?
                            phFriNfc_MifStd_H_BlkChk(NdefMap):
                            Result);
                }
            }
            
            Result = ((Result != NFCSTATUS_SUCCESS)?
                        Result:
                        phFriNfc_MifStd_H_ChkRdWr(NdefMap));
        }
        else
        {
            NdefMap->StdMifareContainer.ChkNdefFlag = 
                PH_FRINFC_MIFARESTD_FLAG0;
            /* Here its required to read the entire card to know the */
            /* Get exact ndef size of the card */
            Result = phFriNfc_MifStd_H_ChkTLVs(NdefMap, &CRFlag);
        }
    }
    else
    {
        /* Since we have decided temporarily not to go
            for any new error codes we are using 
            NFCSTATUS_INVALID_PARAMETER even though it is not 
            the relevant error code here TBD */
        Result = PHNFCSTVAL(    CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_PARAMETER);
    }
    
    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_GPBChk(phFriNfc_NdefMap_t        *NdefMap)
{
    NFCSTATUS Result = NFCSTATUS_SUCCESS;

    /* Spec version needs to be checked every time */
    Result = phFriNfc_MapTool_ChkSpcVer(NdefMap, PH_FRINFC_MIFARESTD_VAL9);
    
    /* Check rhe read and write access field 
        in GPB is 00b 
        bit 0 and 1 for write access check 
        bit 2 and 3 for read access check */
    if(Result == NFCSTATUS_SUCCESS)
    {
        if(((NdefMap->SendRecvBuf[ 
            PH_FRINFC_MIFARESTD_VAL9] & 
            PH_FRINFC_MIFARESTD_MASK_GPB_WR) == 
            PH_FRINFC_MIFARESTD_GPB_RD_WR_VAL) &&
            ((NdefMap->SendRecvBuf[ 
            PH_FRINFC_MIFARESTD_VAL9] & 
            PH_FRINFC_MIFARESTD_MASK_GPB_RD) == 
            PH_FRINFC_MIFARESTD_GPB_RD_WR_VAL))
        {
            NdefMap->CardState = (((NdefMap->StdMifareContainer.ChkNdefFlag == 
                                    PH_FRINFC_MIFARESTD_FLAG1) || 
                                    (NdefMap->StdMifareContainer.ReadNdefFlag == 
                                    PH_FRINFC_MIFARESTD_FLAG1) || 
                                    (NdefMap->StdMifareContainer.WrNdefFlag == 
                                    PH_FRINFC_MIFARESTD_FLAG1))?
                                    PH_NDEFMAP_CARD_STATE_INITIALIZED:
                                    PH_NDEFMAP_CARD_STATE_READ_WRITE);
        }
        else if(((NdefMap->SendRecvBuf[ 
            PH_FRINFC_MIFARESTD_VAL9] & 
            PH_FRINFC_MIFARESTD_MASK_GPB_WR) != 
            PH_FRINFC_MIFARESTD_GPB_RD_WR_VAL) &&
            ((NdefMap->SendRecvBuf[ 
            PH_FRINFC_MIFARESTD_VAL9] & 
            PH_FRINFC_MIFARESTD_MASK_GPB_RD) == 
            PH_FRINFC_MIFARESTD_GPB_RD_WR_VAL))
        {
            /* write access not given
            only read access check */
            NdefMap->CardState = PH_NDEFMAP_CARD_STATE_READ_ONLY;
        }
        else
        {
            NdefMap->CardState = PH_NDEFMAP_CARD_STATE_INVALID;
        }
    }
    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_ProStatNotValid(phFriNfc_NdefMap_t        *NdefMap,
                                                   NFCSTATUS                 status)
{
    NFCSTATUS Result = status;
    /* if NFC forum sector is not found before the proprietary one then 
        authenticate the next sector 
        Else it is a error*/
    if(NdefMap->StdMifareContainer.NFCforumSectFlag == 
        PH_FRINFC_MIFARESTD_FLAG0)
    {
        NdefMap->StdMifareContainer.ProprforumSectFlag = 
                        PH_FRINFC_MIFARESTD_PROP_3RD_CONFIG;
        if(NdefMap->StdMifareContainer.currentBlock < 
            PH_FRINFC_MIFARESTD4K_BLK128)
        {
#ifdef PH_HAL4_ENABLE
            /* Fix for the disovery problem, 
                if 1st sector is invalid then ignore the remaining sectors and 
                send an error if the card is mifare 1k, 
                if the card is mifare 4k, then update the block number to 67 and 
                continue. 
                Even if the authentication of that block fails then send error */
            if(((NdefMap->StdMifareContainer.currentBlock < 
                PH_FRINFC_MIFARESTD_BLK4) && 
                (NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD)) || 
                ((NdefMap->StdMifareContainer.currentBlock <= 
                PH_FRINFC_MIFARESTD_MAD_BLK67) && 
                (NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD)))
            {
                Result = PHNFCSTVAL( CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_NO_NDEF_SUPPORT);
            }
            else if((NdefMap->StdMifareContainer.currentBlock < 
                PH_FRINFC_MIFARESTD_BLK4) && 
                (NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD))
            {
                Result = NFCSTATUS_SUCCESS;
                NdefMap->StdMifareContainer.currentBlock = 
                            PH_FRINFC_MIFARESTD_MAD_BLK67;
            }
            else
#endif /* #ifdef PH_HAL4_ENABLE */
            if(((NdefMap->StdMifareContainer.currentBlock + 
                PH_FRINFC_MIFARESTD_BLK4) > 
                PH_FRINFC_MIFARESTD1K_MAX_BLK) && 
                (NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD))
            {
                Result = PHNFCSTVAL( CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_NO_NDEF_SUPPORT);
            }
            else
            {
                NdefMap->StdMifareContainer.remainingSize -= 
                        (PH_FRINFC_MIFARESTD_MAD_BLK3 * PH_FRINFC_MIFARESTD_BLOCK_BYTES);
                NdefMap->StdMifareContainer.currentBlock += 
                                PH_FRINFC_MIFARESTD_BLK4;
                Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
            }
        }
        else if((NdefMap->StdMifareContainer.currentBlock + 
                PH_FRINFC_MIFARESTD_BLK15) >  
                PH_FRINFC_MIFARESTD4K_MAX_BLK)
        {
            Result = PHNFCSTVAL( CID_FRI_NFC_NDEF_MAP, 
                        NFCSTATUS_NO_NDEF_SUPPORT);
        }
        else
        {
            NdefMap->StdMifareContainer.remainingSize -= 
                        (PH_FRINFC_MIFARESTD_BLK15 * PH_FRINFC_MIFARESTD_BLOCK_BYTES);
            NdefMap->StdMifareContainer.currentBlock += 
                                    PH_FRINFC_MIFARESTD_BLOCK_BYTES;
            Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
        }
        Result = ((Result != NFCSTATUS_SUCCESS)?
                (PHNFCSTVAL( CID_FRI_NFC_NDEF_MAP, 
                        NFCSTATUS_NO_NDEF_SUPPORT)):
                phFriNfc_MifStd_H_AuthSector(NdefMap));
    }
    else if((NdefMap->StdMifareContainer.ProprforumSectFlag ==  
            PH_FRINFC_MIFARESTD_PROP_3RD_CONFIG) && 
            (NdefMap->StdMifareContainer.NFCforumSectFlag == 
            PH_FRINFC_MIFARESTD_FLAG1))
    {
        /*  if the proprietary forum sector are found before 
            NFC forum sector then again a proprietary 
            forum sector are found after the NFC forum 
            sector */
        Result = PHNFCSTVAL( CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_NO_NDEF_SUPPORT);
    }
    else
    {
        NdefMap->StdMifareContainer.ProprforumSectFlag = 
                        PH_FRINFC_MIFARESTD_PROP_2ND_CONFIG;
        switch(NdefMap->PrevOperation)
        {
        case PH_FRINFC_NDEFMAP_CHECK_OPE:
        case PH_FRINFC_NDEFMAP_GET_ACTSIZE_OPE:
            Result = PHNFCSTVAL( CID_FRI_NFC_NDEF_MAP, 
                        NFCSTATUS_NO_NDEF_SUPPORT);
            break;

        case PH_FRINFC_NDEFMAP_READ_OPE:
            if((NdefMap->TLVStruct.NdefTLVFoundFlag == 
                PH_FRINFC_MIFARESTD_FLAG1) && 
                (NdefMap->TLVStruct.NoLbytesinTLV ==  
                PH_FRINFC_MIFARESTD_VAL0))
            {
                *NdefMap->NumOfBytesRead = NdefMap->ApduBuffIndex;
                Result = NFCSTATUS_SUCCESS;
            }
            else
            {
                Result = PHNFCSTVAL( CID_FRI_NFC_NDEF_MAP, 
                        NFCSTATUS_NO_NDEF_SUPPORT);
            }
            break;

        case PH_FRINFC_NDEFMAP_WRITE_OPE:
        default:
            /* This means the further write is not possible, 
                EOF_NDEF_CONTAINER_REACHED */
            NdefMap->StdMifareContainer.ReadWriteCompleteFlag = 
                                PH_FRINFC_MIFARESTD_FLAG1;
            /* Write the length to the L field in the TLV */
            NdefMap->StdMifareContainer.TempBlockNo = 
                        NdefMap->StdMifareContainer.currentBlock;
            phFriNfc_MifStd_H_SetNdefBlkAuth(NdefMap);
            NdefMap->StdMifareContainer.currentBlock = 
                            NdefMap->TLVStruct.NdefTLVBlock;
            Result = phFriNfc_MifStd_H_RdtoWrNdefLen(NdefMap);
            break;
        }
    }
    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_RdBeforeWr(phFriNfc_NdefMap_t        *NdefMap)
{
    NFCSTATUS Result = NFCSTATUS_SUCCESS;
    NdefMap->State = PH_FRINFC_NDEFMAP_STATE_RD_BEF_WR;
    NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_WRITE_OPE;

    Result = phFriNfc_MifStd_H_Rd16Bytes(NdefMap, 
                    NdefMap->StdMifareContainer.currentBlock);
    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_ProBytesToWr(phFriNfc_NdefMap_t        *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint8_t     TempLength = PH_FRINFC_MIFARESTD_VAL0;
    
    if(*NdefMap->SendRecvLength == PH_FRINFC_MIFARESTD_BYTES_READ)
    {
        (void)memcpy(&NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL1],
                    NdefMap->SendRecvBuf,
                    PH_FRINFC_MIFARESTD_BLOCK_BYTES);

        /* Write to Ndef TLV Block */
        NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                            NdefMap->StdMifareContainer.currentBlock;

        TempLength = ((NdefMap->StdMifareContainer.currentBlock == 
                        NdefMap->TLVStruct.NdefTLVBlock)?
                        phFriNfc_MifStd_H_UpdateTLV(NdefMap):
                        phFriNfc_MifStd_H_UpdRemTLV(NdefMap));

        NdefMap->StdMifareContainer.remainingSize -= 
            ((NdefMap->StdMifareContainer.remSizeUpdFlag == 
            PH_FRINFC_MIFARESTD_FLAG1)?
            PH_FRINFC_MIFARESTD_VAL2:
            PH_FRINFC_MIFARESTD_VAL0);

        NdefMap->StdMifareContainer.remSizeUpdFlag = PH_FRINFC_MIFARESTD_FLAG0;
        NdefMap->State = PH_FRINFC_NDEFMAP_STATE_WR_TLV;
        Result = ((TempLength == PH_FRINFC_MIFARESTD_BLOCK_BYTES)?
                    phFriNfc_MifStd_H_WrTLV(NdefMap):
                    phFriNfc_MifStd_H_fillSendBuf(NdefMap, TempLength));
    }
    else
    {
        Result = PHNFCSTVAL( CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_READ_FAILED);
    }

    return Result;
}

static uint8_t phFriNfc_MifStd_H_UpdateTLV(phFriNfc_NdefMap_t *NdefMap)
{
    uint8_t     TempLength = PH_FRINFC_MIFARESTD_VAL0;
    TempLength = (uint8_t)(NdefMap->TLVStruct.NdefTLVByte + PH_FRINFC_MIFARESTD_VAL1);
    /* Creating TLV */
    if(NdefMap->TLVStruct.NULLTLVCount >= 2)
    {
        if((PH_FRINFC_MIFARESTD_BYTES_READ - TempLength) == 
            PH_FRINFC_MIFARESTD_VAL0)
        {       
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_T;
        }
        else
        {
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_T;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_L0;
        }
    }
    else
    {
        switch((PH_FRINFC_MIFARESTD_BYTES_READ - 
                TempLength))
        {
        case PH_FRINFC_MIFARESTD_VAL0:
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NULLTLV_T;
        break;

        case PH_FRINFC_MIFARESTD_VAL1:
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NULLTLV_T;
            TempLength++;
            NdefMap->TLVStruct.prevLenByteValue = 
                                    (uint16_t)((NdefMap->SendRecvBuf[TempLength] >= 
                                    PH_FRINFC_MIFARESTD_NDEFTLV_L)?
                                    PH_FRINFC_MIFARESTD_VAL0:
                                    NdefMap->SendRecvBuf[TempLength]);
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NULLTLV_T;
        break;

        case PH_FRINFC_MIFARESTD_VAL2:
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NULLTLV_T;
            TempLength++;
            NdefMap->TLVStruct.prevLenByteValue = 
                                    (uint16_t)((NdefMap->SendRecvBuf[TempLength] >= 
                                    PH_FRINFC_MIFARESTD_NDEFTLV_L)?
                                    NdefMap->SendRecvBuf[(TempLength + 
                                                PH_FRINFC_MIFARESTD_VAL1)]:
                                    NdefMap->SendRecvBuf[TempLength]);
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NULLTLV_T;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_T;
        break;

        default:
            NdefMap->TLVStruct.prevLenByteValue = 
                            NdefMap->SendRecvBuf[TempLength];
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NULLTLV_T;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NULLTLV_T;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_T;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_L0;
        break;
        }
    }

    return TempLength;
}

static NFCSTATUS phFriNfc_MifStd_H_fillSendBuf(phFriNfc_NdefMap_t        *NdefMap,
                                               uint8_t                   Length)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint16_t    RemainingBytes = PH_FRINFC_MIFARESTD_VAL0,
                BytesToWrite = PH_FRINFC_MIFARESTD_VAL0;
    uint8_t     index = PH_FRINFC_MIFARESTD_VAL0;

    Length = (Length + PH_FRINFC_MIFARESTD_VAL1);

    RemainingBytes = (uint16_t)((NdefMap->StdMifareContainer.remainingSize 
                        < (uint16_t)(NdefMap->ApduBufferSize - 
                        NdefMap->ApduBuffIndex))?
                        NdefMap->StdMifareContainer.remainingSize:
                        (NdefMap->ApduBufferSize - 
                        NdefMap->ApduBuffIndex));

    NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = 
                            NdefMap->StdMifareContainer.currentBlock;
        /* Get the number of bytes that can be written after copying 
        the internal buffer */
    BytesToWrite = ((RemainingBytes < 
                    ((PH_FRINFC_MIFARESTD_WR_A_BLK - Length) - 
                    NdefMap->StdMifareContainer.internalLength))? 
                    RemainingBytes:
                    ((PH_FRINFC_MIFARESTD_WR_A_BLK - Length) - 
                    NdefMap->StdMifareContainer.internalLength));

    if(NdefMap->StdMifareContainer.internalLength > 
        PH_FRINFC_MIFARESTD_VAL0)
    {
        /* copy the internal buffer to the send buffer */
        (void)memcpy(&(NdefMap->SendRecvBuf[ 
                    Length]),
                    NdefMap->StdMifareContainer.internalBuf,
                    NdefMap->StdMifareContainer.internalLength);
    }

    /* Copy Bytes to write in the send buffer */
    (void)memcpy(&(NdefMap->SendRecvBuf[ 
                (Length + 
                NdefMap->StdMifareContainer.internalLength)]),
                &(NdefMap->ApduBuffer[NdefMap->ApduBuffIndex]),
                BytesToWrite);

    /* update number of bytes written from the user buffer */
    NdefMap->NumOfBytesWritten = BytesToWrite;
    
    /* check the exact number of bytes written to a block including the 
        internal length */
    *NdefMap->DataCount = 
            ((BytesToWrite + NdefMap->StdMifareContainer.internalLength
            + Length) - PH_FRINFC_MIFARESTD_VAL1);

    /* if total bytes to write in the card is less than 4 bytes then 
    pad zeroes till 4 bytes */
    if((BytesToWrite + NdefMap->StdMifareContainer.internalLength + 
        Length) < PH_FRINFC_MIFARESTD_WR_A_BLK)
    {
        for(index = (uint8_t)(BytesToWrite + 
                    NdefMap->StdMifareContainer.internalLength + 
                    Length); 
            index < PH_FRINFC_MIFARESTD_WR_A_BLK; 
            index++)
            {
                NdefMap->SendRecvBuf[index] = (uint8_t)((index == 
                                    (BytesToWrite + Length + 
                                    NdefMap->StdMifareContainer.internalLength))?
                                    PH_FRINFC_MIFARESTD_TERMTLV_T:
                                    PH_FRINFC_MIFARESTD_NULLTLV_T);

                NdefMap->TLVStruct.SetTermTLVFlag = PH_FRINFC_MIFARESTD_FLAG1;
            }
    }
#ifdef PH_HAL4_ENABLE

    NdefMap->TLVStruct.SetTermTLVFlag = PH_FRINFC_MIFARESTD_FLAG1;

#endif /* #ifdef PH_HAL4_ENABLE */

    /* A temporary buffer to hold four bytes of data that is 
        written to the card */
    (void)memcpy(NdefMap->StdMifareContainer.Buffer,
                &(NdefMap->SendRecvBuf[ 
                PH_FRINFC_MIFARESTD_VAL1]),
                PH_FRINFC_MIFARESTD_BLOCK_BYTES);

    NdefMap->State = PH_FRINFC_NDEFMAP_STATE_WR_TLV;
    Result = phFriNfc_MifStd_H_WrTLV(NdefMap);
    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_WrTLV(phFriNfc_NdefMap_t        *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;

    /* set the data for additional data exchange*/
    NdefMap->psDepAdditionalInfo.DepFlags.MetaChaining = 0;
    NdefMap->psDepAdditionalInfo.DepFlags.NADPresent = 0;
    NdefMap->psDepAdditionalInfo.NAD = 0;
    NdefMap->MapCompletionInfo.CompletionRoutine = phFriNfc_MifareStdMap_Process;
    NdefMap->MapCompletionInfo.Context = NdefMap;
    /* Write from here */
    NdefMap->SendLength = MIFARE_MAX_SEND_BUF_TO_WRITE;

#ifndef PH_HAL4_ENABLE
    NdefMap->Cmd.MfCmd = phHal_eMifareCmdListMifareWrite16;
#else
    NdefMap->Cmd.MfCmd = phHal_eMifareWrite16;
#endif

    *NdefMap->SendRecvLength = NdefMap->TempReceiveLength;
    /* Call the Overlapped HAL Transceive function */ 
    Result = phFriNfc_OvrHal_Transceive(    NdefMap->LowerDevice,
                                            &NdefMap->MapCompletionInfo,
                                            NdefMap->psRemoteDevInfo,
                                            NdefMap->Cmd,
                                            &NdefMap->psDepAdditionalInfo,
                                            NdefMap->SendRecvBuf,
                                            NdefMap->SendLength,
                                            NdefMap->SendRecvBuf,
                                            NdefMap->SendRecvLength);


    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_ProWrTLV(phFriNfc_NdefMap_t        *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    /* Check that if complete TLV has been written in the 
        card if yes enter the below check or go to else*/
    if(((((PH_FRINFC_MIFARESTD_BLOCK_BYTES - 
            NdefMap->TLVStruct.NdefTLVByte) ==  
            PH_FRINFC_MIFARESTD_VAL1) && 
            (NdefMap->TLVStruct.NULLTLVCount >= 
            PH_FRINFC_MIFARESTD_VAL2)) || 
            (((PH_FRINFC_MIFARESTD_BLOCK_BYTES - 
            NdefMap->TLVStruct.NdefTLVByte) <= 
            PH_FRINFC_MIFARESTD_VAL3) && 
            (NdefMap->TLVStruct.NULLTLVCount == 
            PH_FRINFC_MIFARESTD_VAL0))) && 
            (NdefMap->StdMifareContainer.currentBlock == 
            NdefMap->TLVStruct.NdefTLVBlock))
    {
        /* increment the block and chekc the block is in the same sector
            using the block check function */
        NdefMap->StdMifareContainer.RdBeforeWrFlag = PH_FRINFC_MIFARESTD_FLAG1;
        NdefMap->StdMifareContainer.currentBlock++;
        NdefMap->StdMifareContainer.NdefBlocks++;
        Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
        if(Result == NFCSTATUS_SUCCESS)
        {
            Result = ((NdefMap->StdMifareContainer.AuthDone == 
                        PH_FRINFC_MIFARESTD_FLAG0)?
                        phFriNfc_MifStd_H_AuthSector(NdefMap):
                        phFriNfc_MifStd_H_RdBeforeWr(NdefMap));
        }
    }
    else
    {
        NdefMap->StdMifareContainer.RdBeforeWrFlag = PH_FRINFC_MIFARESTD_FLAG0;
        if(NdefMap->ApduBuffIndex < 
                    (uint16_t)NdefMap->ApduBufferSize)
        {
            if(*NdefMap->DataCount < PH_FRINFC_MIFARESTD_BLOCK_BYTES)
            {
                /* Write complete, so next byte shall be */
                NdefMap->StdMifareContainer.internalLength =
                    *NdefMap->DataCount;
            
                /* Copy bytes less than 16 to internal buffer
                    for the next write this can be used */
                (void)memcpy( NdefMap->StdMifareContainer.internalBuf,
                        NdefMap->StdMifareContainer.Buffer,
                        NdefMap->StdMifareContainer.internalLength);
            }

            /* Increment the Send Buffer index */
             NdefMap->ApduBuffIndex += NdefMap->NumOfBytesWritten;

             NdefMap->StdMifareContainer.remainingSize -= 
                                        NdefMap->NumOfBytesWritten;

             /* Check for the End of Card */
            if((NdefMap->StdMifareContainer.remainingSize  == 
                PH_FRINFC_MIFARESTD_VAL0) || 
                (NdefMap->ApduBuffIndex == NdefMap->ApduBufferSize))
            {
                NdefMap->StdMifareContainer.ReadWriteCompleteFlag = 
                (uint8_t)((NdefMap->StdMifareContainer.remainingSize  == 0)?
                    PH_FRINFC_MIFARESTD_FLAG1:PH_FRINFC_MIFARESTD_FLAG0);

                if(NdefMap->StdMifareContainer.internalLength == 
                    PH_FRINFC_MIFARESTD_VAL0)
                {
                    NdefMap->StdMifareContainer.currentBlock++;
                    /* Mifare 4k Card, After 128th Block
                    each sector = 16 blocks in Mifare 4k */
                    Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
                    NdefMap->StdMifareContainer.NdefBlocks++;
                }

                NdefMap->TLVStruct.SetTermTLVFlag = 
                    (uint8_t)(((NdefMap->StdMifareContainer.remainingSize == 
                            PH_FRINFC_MIFARESTD_VAL0) || 
                            (NdefMap->TLVStruct.SetTermTLVFlag == 
                            PH_FRINFC_MIFARESTD_FLAG1))?
                            PH_FRINFC_MIFARESTD_FLAG1:
                            PH_FRINFC_MIFARESTD_FLAG0);
                
            }
            else
            {
                NdefMap->StdMifareContainer.currentBlock++;
                /* Mifare 4k Card, After 128th Block
                each sector = 16 blocks in Mifare 4k */
                Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
                if(Result == NFCSTATUS_SUCCESS)
                {
                    NdefMap->StdMifareContainer.NdefBlocks++;
                    Result = ((NdefMap->StdMifareContainer.AuthDone == 
                                PH_FRINFC_MIFARESTD_FLAG1)?
                                phFriNfc_MifStd_H_WrABlock(NdefMap):
                                phFriNfc_MifStd_H_AuthSector(NdefMap));
                }
            }
        }
    }

    if((Result == NFCSTATUS_SUCCESS) && 
        (NdefMap->TLVStruct.SetTermTLVFlag != 
        PH_FRINFC_MIFARESTD_FLAG1) && 
        (NdefMap->StdMifareContainer.remainingSize > 
        PH_FRINFC_MIFARESTD_VAL0))
    {
        Result = phFriNfc_MifStd_H_WrTermTLV(NdefMap);
    }
    else 
    {
        if((Result == NFCSTATUS_SUCCESS) && 
            (NdefMap->TLVStruct.SetTermTLVFlag == 
            PH_FRINFC_MIFARESTD_FLAG1))
        {
            /* Write the length to the L field in the TLV */
            NdefMap->StdMifareContainer.TempBlockNo = 
                        NdefMap->StdMifareContainer.currentBlock;
            phFriNfc_MifStd_H_SetNdefBlkAuth(NdefMap);
            NdefMap->StdMifareContainer.currentBlock = 
                            NdefMap->TLVStruct.NdefTLVBlock;
            Result = phFriNfc_MifStd_H_RdtoWrNdefLen(NdefMap);
        }
    }
    return Result;
}

static uint8_t phFriNfc_MifStd_H_UpdRemTLV(phFriNfc_NdefMap_t        *NdefMap)
{
    uint8_t     TempLength = PH_FRINFC_MIFARESTD_VAL1;
    if(NdefMap->TLVStruct.NULLTLVCount >= 
        PH_FRINFC_MIFARESTD_VAL2)
    {
        NdefMap->TLVStruct.prevLenByteValue = NdefMap->SendRecvBuf[TempLength];
        NdefMap->SendRecvBuf[TempLength] = 
                        PH_FRINFC_MIFARESTD_NDEFTLV_L0;
    }
    else
    {
        switch((PH_FRINFC_MIFARESTD_BLOCK_BYTES - 
            NdefMap->TLVStruct.NdefTLVByte))
        {
        case PH_FRINFC_MIFARESTD_VAL1:
            NdefMap->TLVStruct.prevLenByteValue = 
                    (((NdefMap->SendRecvBuf[TempLength] == 
                    PH_FRINFC_MIFARESTD_NDEFTLV_L))?
                    (((uint16_t)NdefMap->SendRecvBuf[(TempLength + PH_FRINFC_MIFARESTD_VAL1)]
                    << PH_FRINFC_MIFARESTD_LEFTSHIFT8) + 
                    NdefMap->SendRecvBuf[(TempLength + PH_FRINFC_MIFARESTD_VAL2)]):
                    NdefMap->SendRecvBuf[TempLength]);
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NULLTLV_T;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_T;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_L0;
            break;

        case PH_FRINFC_MIFARESTD_VAL2:
            NdefMap->TLVStruct.prevLenByteValue = 
                    (((NdefMap->SendRecvBuf[TempLength] == 
                        PH_FRINFC_MIFARESTD_NDEFTLV_L))?
                    (((uint16_t)NdefMap->SendRecvBuf[TempLength] << 
                    PH_FRINFC_MIFARESTD_LEFTSHIFT8) + 
                    NdefMap->SendRecvBuf[(TempLength + PH_FRINFC_MIFARESTD_VAL1)]):
                    NdefMap->SendRecvBuf[TempLength]);
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_T;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_L0;
            break;

        case PH_FRINFC_MIFARESTD_VAL3:
        default:
            NdefMap->TLVStruct.prevLenByteValue = 
                                            ((NdefMap->TLVStruct.prevLenByteValue << 
                                            PH_FRINFC_MIFARESTD_LEFTSHIFT8) 
                                            + NdefMap->SendRecvBuf[TempLength]);
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_L0;
            break;
        }
    }
    return TempLength;
}

static void phFriNfc_MifStd_H_fillTLV1(phFriNfc_NdefMap_t        *NdefMap)
{
    uint8_t     TempLength = (uint8_t)(NdefMap->TLVStruct.NdefTLVByte + 
                            PH_FRINFC_MIFARESTD_VAL1);
    
    NdefMap->TLVStruct.prevLenByteValue = 
                    ((NdefMap->Offset == PH_FRINFC_NDEFMAP_SEEK_CUR)?
                    (NdefMap->TLVStruct.prevLenByteValue + 
                    NdefMap->ApduBuffIndex):
                    NdefMap->ApduBuffIndex);

    NdefMap->StdMifareContainer.RdAfterWrFlag = PH_FRINFC_MIFARESTD_FLAG1;
    switch(NdefMap->TLVStruct.NdefTLVByte)
    {
    case PH_FRINFC_MIFARESTD_VAL0:
        if(NdefMap->TLVStruct.prevLenByteValue >=  
            PH_FRINFC_MIFARESTD_NDEFTLV_L)
        {
            NdefMap->SendRecvBuf[TempLength] = 
                            (uint8_t)(NdefMap->TLVStruct.prevLenByteValue >> 
                            PH_FRINFC_MIFARESTD_RIGHTSHIFT8);
            NdefMap->SendRecvBuf[(TempLength + 
                                PH_FRINFC_MIFARESTD_VAL1)] = 
                            (uint8_t)NdefMap->TLVStruct.prevLenByteValue;
        }
        else
        {
            NdefMap->SendRecvBuf[TempLength] = 
                            PH_FRINFC_MIFARESTD_NDEFTLV_T;
            NdefMap->SendRecvBuf[(TempLength + 
                                PH_FRINFC_MIFARESTD_VAL1)] = 
                            (uint8_t)NdefMap->TLVStruct.prevLenByteValue;

            NdefMap->StdMifareContainer.RdAfterWrFlag = 
                                PH_FRINFC_MIFARESTD_FLAG0;
        }
        break;

    case PH_FRINFC_MIFARESTD_VAL1:
        if(NdefMap->TLVStruct.prevLenByteValue >=  
            PH_FRINFC_MIFARESTD_NDEFTLV_L)
        {
            NdefMap->SendRecvBuf[TempLength - PH_FRINFC_MIFARESTD_VAL1] = 
                            PH_FRINFC_MIFARESTD_NDEFTLV_L;
            NdefMap->SendRecvBuf[TempLength] = 
                            (uint8_t)(NdefMap->TLVStruct.prevLenByteValue >> 
                            PH_FRINFC_MIFARESTD_RIGHTSHIFT8);
            NdefMap->SendRecvBuf[(TempLength + 
                                PH_FRINFC_MIFARESTD_VAL1)] = 
                            (uint8_t)NdefMap->TLVStruct.prevLenByteValue;
        }
        else
        {
            NdefMap->SendRecvBuf[TempLength] = 
                            PH_FRINFC_MIFARESTD_NDEFTLV_T;
            NdefMap->SendRecvBuf[(TempLength + 
                                PH_FRINFC_MIFARESTD_VAL1)] = 
                            (uint8_t)NdefMap->TLVStruct.prevLenByteValue;
            NdefMap->StdMifareContainer.RdAfterWrFlag = 
                                PH_FRINFC_MIFARESTD_FLAG0;
        }           
        break;

    case PH_FRINFC_MIFARESTD_VAL15:
        /* if "Type" of TLV present at byte 15 */
        if(NdefMap->TLVStruct.prevLenByteValue >=  
            PH_FRINFC_MIFARESTD_NDEFTLV_L)
        {
            /* Update the null TLV, ndef TLV block and ndef TLV byte */
            NdefMap->TLVStruct.NULLTLVCount = PH_FRINFC_MIFARESTD_VAL0;
            NdefMap->TLVStruct.NdefTLVBlock = 
                                NdefMap->StdMifareContainer.currentBlock;
            NdefMap->TLVStruct.NdefTLVByte = 
                                (TempLength - PH_FRINFC_MIFARESTD_VAL3);
            
            NdefMap->SendRecvBuf[(TempLength - PH_FRINFC_MIFARESTD_VAL2)] = 
                            PH_FRINFC_MIFARESTD_NDEFTLV_T;
            NdefMap->SendRecvBuf[(TempLength - PH_FRINFC_MIFARESTD_VAL1)] = 
                            PH_FRINFC_MIFARESTD_NDEFTLV_L;
            NdefMap->SendRecvBuf[TempLength] = 
                            (uint8_t)(NdefMap->TLVStruct.prevLenByteValue >> 
                            PH_FRINFC_MIFARESTD_RIGHTSHIFT8);
        }
        else
        {
            NdefMap->SendRecvBuf[TempLength] = 
                            PH_FRINFC_MIFARESTD_NDEFTLV_T;
        }           
        break;

    default:
        /* Already the TLV is present so just append the length field */
        if(NdefMap->TLVStruct.prevLenByteValue >= 
            PH_FRINFC_MIFARESTD_NDEFTLV_L)
        {
            /* Update the null TLV, ndef TLV block and ndef TLV byte */
            NdefMap->TLVStruct.NULLTLVCount = PH_FRINFC_MIFARESTD_VAL0;
            NdefMap->TLVStruct.NdefTLVBlock = 
                                NdefMap->StdMifareContainer.currentBlock;
            NdefMap->TLVStruct.NdefTLVByte = 
                                (TempLength - PH_FRINFC_MIFARESTD_VAL3);

            NdefMap->SendRecvBuf[(TempLength - PH_FRINFC_MIFARESTD_VAL2)] = 
                                (uint8_t)PH_FRINFC_MIFARESTD_NDEFTLV_T;
            NdefMap->SendRecvBuf[(TempLength - PH_FRINFC_MIFARESTD_VAL1)] = 
                                (uint8_t)PH_FRINFC_MIFARESTD_NDEFTLV_L;
            NdefMap->SendRecvBuf[TempLength] = 
                                (uint8_t)(NdefMap->TLVStruct.prevLenByteValue >> 
                                PH_FRINFC_MIFARESTD_RIGHTSHIFT8);
            NdefMap->SendRecvBuf[(TempLength + PH_FRINFC_MIFARESTD_VAL1)] = 
                                (uint8_t)NdefMap->TLVStruct.prevLenByteValue;
        }
        else
        {
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_T;
            NdefMap->SendRecvBuf[(TempLength + PH_FRINFC_MIFARESTD_VAL1)] = 
                                (uint8_t)NdefMap->TLVStruct.prevLenByteValue;
        }
        NdefMap->StdMifareContainer.RdAfterWrFlag = 
                                PH_FRINFC_MIFARESTD_FLAG0;
        break;
    }
}

static void phFriNfc_MifStd_H_fillTLV2(phFriNfc_NdefMap_t        *NdefMap)
{
    uint8_t     TempLength = (uint8_t)(NdefMap->TLVStruct.NdefTLVByte + 
                                PH_FRINFC_MIFARESTD_VAL1);
    
    NdefMap->TLVStruct.prevLenByteValue = ((NdefMap->Offset == 
                                    PH_FRINFC_NDEFMAP_SEEK_CUR)?
                                    (NdefMap->TLVStruct.prevLenByteValue + 
                                    NdefMap->ApduBuffIndex):
                                    NdefMap->ApduBuffIndex);
    NdefMap->StdMifareContainer.RdAfterWrFlag = PH_FRINFC_MIFARESTD_FLAG1;
    switch(NdefMap->TLVStruct.NdefTLVByte)
    {
    case PH_FRINFC_MIFARESTD_VAL13:
        if(NdefMap->TLVStruct.prevLenByteValue >= 
            PH_FRINFC_MIFARESTD_NDEFTLV_L)
        {
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_T;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_L;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = (uint8_t)(NdefMap->TLVStruct.prevLenByteValue >> 
                                                PH_FRINFC_MIFARESTD_RIGHTSHIFT8);
        }
        else
        {
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NULLTLV_T;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NULLTLV_T;
            TempLength++;

            /* Update the null TLV, ndef TLV block and ndef TLV byte */
            NdefMap->TLVStruct.NULLTLVCount = PH_FRINFC_MIFARESTD_VAL2;
            NdefMap->TLVStruct.NdefTLVBlock = 
                                NdefMap->StdMifareContainer.currentBlock;
            NdefMap->TLVStruct.NdefTLVByte = 
                                (TempLength - PH_FRINFC_MIFARESTD_VAL1);

            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_T;
        }
        break;

    case PH_FRINFC_MIFARESTD_VAL14:
        if(NdefMap->TLVStruct.prevLenByteValue >= 
            PH_FRINFC_MIFARESTD_NDEFTLV_L)
        {
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_T;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_L;
        }
        else
        {
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NULLTLV_T;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NULLTLV_T;
        }
        break;

    case PH_FRINFC_MIFARESTD_VAL15:
        if(NdefMap->TLVStruct.prevLenByteValue >= 
            PH_FRINFC_MIFARESTD_NDEFTLV_L)
        {
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_T;
        }
        else
        {
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NULLTLV_T;
        }
        break;

    default:
        if(NdefMap->TLVStruct.prevLenByteValue >= 
            PH_FRINFC_MIFARESTD_NDEFTLV_L)
        {
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_T;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_L;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = (uint8_t)(NdefMap->TLVStruct.prevLenByteValue >> 
                                                PH_FRINFC_MIFARESTD_RIGHTSHIFT8);
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = 
                                (uint8_t)NdefMap->TLVStruct.prevLenByteValue;
        }
        else
        {
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NULLTLV_T;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NULLTLV_T;
            TempLength++;

            /* Update the null TLV, ndef TLV block and ndef TLV byte */
            NdefMap->TLVStruct.NULLTLVCount = PH_FRINFC_MIFARESTD_VAL2;
            NdefMap->TLVStruct.NdefTLVBlock = 
                                NdefMap->StdMifareContainer.currentBlock;
            NdefMap->TLVStruct.NdefTLVByte = 
                                (TempLength - PH_FRINFC_MIFARESTD_VAL1);
            
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_T;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = 
                                (uint8_t)NdefMap->TLVStruct.prevLenByteValue;
        }
        NdefMap->StdMifareContainer.RdAfterWrFlag = PH_FRINFC_MIFARESTD_FLAG0;
        break;
    }
}

static NFCSTATUS phFriNfc_MifStd_H_CallWrNdefLen(phFriNfc_NdefMap_t        *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    if(NdefMap->TLVStruct.NULLTLVCount >= PH_FRINFC_MIFARESTD_VAL2)
    {
        if((NdefMap->TLVStruct.NdefTLVByte == PH_FRINFC_MIFARESTD_VAL0) || 
            (NdefMap->TLVStruct.NdefTLVByte == PH_FRINFC_MIFARESTD_VAL1) ) 
        {       
            /* In this case, current block is decremented because the 
                NULL TLVs are in the previous block */
            NdefMap->StdMifareContainer.currentBlock--;
            Result = phFriNfc_MifStd_H_BlkChk_1(NdefMap);
        }
        else
        {         
            /* case NdefMap->TLVStruct.NdefTLVByte = PH_FRINFC_MIFARESTD_VAL15: 
              Current block is incremented to update the remaining TLV 
                structure */
            NdefMap->StdMifareContainer.currentBlock++;
            Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
        }
    }
    else
    {
        if((NdefMap->TLVStruct.NdefTLVByte == PH_FRINFC_MIFARESTD_VAL13) || 
            (NdefMap->TLVStruct.NdefTLVByte == PH_FRINFC_MIFARESTD_VAL14) || 
            (NdefMap->TLVStruct.NdefTLVByte == PH_FRINFC_MIFARESTD_VAL15))
        {
            /* Current block is incremented to update the remaining TLV 
                structure */
            NdefMap->StdMifareContainer.currentBlock++;
            Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
        }
    }

    Result = ((Result == NFCSTATUS_SUCCESS)?
            phFriNfc_MifStd_H_RdtoWrNdefLen(NdefMap):
            Result);
    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_BlkChk_1(phFriNfc_NdefMap_t        *NdefMap)
{
    /* This function is to check the current block is in the authenticated sector and 
        also check the block does not match with the sector trailer block. If there is a 
        match then decrement the block and say the caller that the authentication 
        is required */
    NFCSTATUS   Result  = NFCSTATUS_SUCCESS;
    uint8_t     SectorID = PH_FRINFC_MIFARESTD_VAL0;
    
    /* Get a Sector ID for the Current Block */
    SectorID = phFriNfc_MifStd_H_GetSect(NdefMap->StdMifareContainer.currentBlock);

    /* Check the sector id is valid or not and if valid then check the 
        current block is greater than 128 */
    if((NdefMap->StdMifareContainer.aid[SectorID] == 
        PH_FRINFC_MIFARESTD_NDEF_COMP) && 
        (((SectorID <= PH_FRINFC_MIFARESTD_VAL15) && 
        (NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD)) || 
        ((SectorID <= PH_FRINFC_MIFARESTD_SECTOR_NO39) && 
        (NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD))))
    {
        if(NdefMap->StdMifareContainer.currentBlock > 128)
        {
            NdefMap->TLVStruct.NdefTLVAuthFlag = 
            ((((NdefMap->StdMifareContainer.currentBlock + 
                PH_FRINFC_MIFARESTD_VAL1) % 
                PH_FRINFC_MIFARESTD_MAD_BLK16) == 
                PH_FRINFC_MIFARESTD_VAL0)?
                PH_FRINFC_MIFARESTD_FLAG1:
                PH_FRINFC_MIFARESTD_FLAG0);

                NdefMap->StdMifareContainer.currentBlock -= 
            ((((NdefMap->StdMifareContainer.currentBlock + 
                PH_FRINFC_MIFARESTD_VAL1) % 
                PH_FRINFC_MIFARESTD_MAD_BLK16) == 
                PH_FRINFC_MIFARESTD_VAL0)?
                PH_FRINFC_MIFARESTD_VAL1:
                PH_FRINFC_MIFARESTD_VAL0);

        }
        else
        {
            NdefMap->TLVStruct.NdefTLVAuthFlag = 
            ((((NdefMap->StdMifareContainer.currentBlock + 
                PH_FRINFC_MIFARESTD_VAL1) % 
                PH_FRINFC_MIFARESTD_BLK4) == 
                PH_FRINFC_MIFARESTD_VAL0)?
                PH_FRINFC_MIFARESTD_FLAG1:
                PH_FRINFC_MIFARESTD_FLAG0);

            NdefMap->StdMifareContainer.currentBlock -= 
            ((((NdefMap->StdMifareContainer.currentBlock + 
                PH_FRINFC_MIFARESTD_VAL1) % 
                PH_FRINFC_MIFARESTD_BLK4) == 
                PH_FRINFC_MIFARESTD_VAL1)?
                PH_FRINFC_MIFARESTD_VAL1:
                PH_FRINFC_MIFARESTD_VAL0);
                
        }
    }
    else
    {
        /*Error: No Ndef Compliant Sectors present.*/
        Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_INVALID_PARAMETER);
    }
    
    return Result;
}

static void phFriNfc_MifStd_H_fillTLV1_1(phFriNfc_NdefMap_t        *NdefMap)
{
    switch(NdefMap->TLVStruct.NdefTLVByte)
    {
    case PH_FRINFC_MIFARESTD_VAL0:
        /* In the first write ndef length procedure, the 
            length is updated, in this case T and L = 0xFF of TLV are 
            updated */
        NdefMap->TLVStruct.NULLTLVCount = PH_FRINFC_MIFARESTD_VAL0;
        NdefMap->TLVStruct.NdefTLVBlock = 
                            NdefMap->StdMifareContainer.currentBlock;
        NdefMap->TLVStruct.NdefTLVByte = PH_FRINFC_MIFARESTD_VAL14;

        NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL15] = 
                                PH_FRINFC_MIFARESTD_NDEFTLV_T;
        NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL16] = 
                                PH_FRINFC_MIFARESTD_NDEFTLV_L;
        break;

    case PH_FRINFC_MIFARESTD_VAL1:
        /* In the first write ndef length procedure, the 
            length is updated, in this case T of TLV is 
            updated */
        NdefMap->TLVStruct.NULLTLVCount = PH_FRINFC_MIFARESTD_VAL0;
        NdefMap->TLVStruct.NdefTLVBlock = 
                            NdefMap->StdMifareContainer.currentBlock;
        NdefMap->TLVStruct.NdefTLVByte = 
                            PH_FRINFC_MIFARESTD_VAL15;
        NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL16] = PH_FRINFC_MIFARESTD_NDEFTLV_T;
        break;

    case PH_FRINFC_MIFARESTD_VAL15:
    default:
        /* In the first ndef write length, part of the L field or only T 
            (if update length is less than 255) is updated */
        NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL1] = 
                                    (uint8_t)NdefMap->TLVStruct.prevLenByteValue;
        break;
    }
    NdefMap->StdMifareContainer.RdAfterWrFlag = PH_FRINFC_MIFARESTD_FLAG0;
}

static void phFriNfc_MifStd_H_fillTLV2_1(phFriNfc_NdefMap_t        *NdefMap)
{
    uint8_t     TempLength = PH_FRINFC_MIFARESTD_VAL1;
    switch(NdefMap->TLVStruct.NdefTLVByte)
    {       
    case PH_FRINFC_MIFARESTD_VAL13:
        /* In last write ndef length, part of length (L) field of TLV 
            is updated now */
        NdefMap->SendRecvBuf[TempLength] = 
                    (uint8_t)NdefMap->TLVStruct.prevLenByteValue;
        break;

    case PH_FRINFC_MIFARESTD_VAL14:
        /* In last write ndef length, part of length (L) field of TLV 
                is updated now */
        if(NdefMap->TLVStruct.prevLenByteValue >= 
                    PH_FRINFC_MIFARESTD_NDEFTLV_L)
        {
            NdefMap->SendRecvBuf[TempLength] = 
                            (uint8_t)(NdefMap->TLVStruct.prevLenByteValue >> 
                            PH_FRINFC_MIFARESTD_RIGHTSHIFT8);
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = 
                            (uint8_t)NdefMap->TLVStruct.prevLenByteValue;
        }
        else
        {
            NdefMap->TLVStruct.NULLTLVCount = PH_FRINFC_MIFARESTD_VAL2;
            NdefMap->TLVStruct.NdefTLVBlock = 
                                NdefMap->StdMifareContainer.currentBlock;
            NdefMap->TLVStruct.NdefTLVByte = 
                                (TempLength - PH_FRINFC_MIFARESTD_VAL1);
            NdefMap->SendRecvBuf[TempLength] = PH_FRINFC_MIFARESTD_NDEFTLV_T;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = 
                                        (uint8_t)NdefMap->TLVStruct.prevLenByteValue;
        }
        break;

    case PH_FRINFC_MIFARESTD_VAL15:
    default:
        if(NdefMap->TLVStruct.prevLenByteValue >= 
            PH_FRINFC_MIFARESTD_NDEFTLV_L)
        {
            /* In last write ndef length, only T of TLV is updated and 
                length (L) field of TLV is updated now */
            NdefMap->SendRecvBuf[TempLength] = 
                            PH_FRINFC_MIFARESTD_NDEFTLV_L;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = 
                            (uint8_t)(NdefMap->TLVStruct.prevLenByteValue >> 
                            PH_FRINFC_MIFARESTD_RIGHTSHIFT8);
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = 
                            (uint8_t)NdefMap->TLVStruct.prevLenByteValue;
        }
        else
        {
            NdefMap->SendRecvBuf[TempLength] = 
                            PH_FRINFC_MIFARESTD_NULLTLV_T;
            TempLength++;
            NdefMap->TLVStruct.NULLTLVCount = PH_FRINFC_MIFARESTD_VAL2;
            NdefMap->TLVStruct.NdefTLVBlock = 
                                NdefMap->StdMifareContainer.currentBlock;
            NdefMap->TLVStruct.NdefTLVByte = 
                                (TempLength - PH_FRINFC_MIFARESTD_VAL1);
            NdefMap->SendRecvBuf[TempLength] = 
                            PH_FRINFC_MIFARESTD_NDEFTLV_T;
            TempLength++;
            NdefMap->SendRecvBuf[TempLength] = 
                        (uint8_t)NdefMap->TLVStruct.prevLenByteValue;
        }
        break;
    }
    NdefMap->StdMifareContainer.RdAfterWrFlag = PH_FRINFC_MIFARESTD_FLAG0;
}

static NFCSTATUS phFriNfc_MifStd_H_RdTLV(phFriNfc_NdefMap_t        *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    NdefMap->State = PH_FRINFC_NDEFMAP_STATE_RD_TLV;
    NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_READ_OPE;

    Result = phFriNfc_MifStd_H_Rd16Bytes(NdefMap, 
                NdefMap->StdMifareContainer.currentBlock);
    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_ProRdTLV(phFriNfc_NdefMap_t        *NdefMap)
{
    NFCSTATUS   Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_DEVICE_REQUEST);
    uint8_t     TempLength = PH_FRINFC_MIFARESTD_VAL0,
                NDEFFlag = PH_FRINFC_MIFARESTD_FLAG1;

    /*TempLength = (uint8_t)(((NdefMap->TLVStruct.NULLTLVCount >= 
                PH_FRINFC_MIFARESTD_VAL2) && 
                (NdefMap->TLVStruct.BytesRemainLinTLV > 0xFE))?
                ((NdefMap->TLVStruct.NdefTLVByte + 
                PH_FRINFC_MIFARESTD_VAL2)%
                PH_FRINFC_MIFARESTD_VAL16):
                ((NdefMap->TLVStruct.NdefTLVByte + 
                PH_FRINFC_MIFARESTD_VAL4)%
                PH_FRINFC_MIFARESTD_VAL16));*/

    TempLength = (uint8_t)((NdefMap->TLVStruct.BytesRemainLinTLV <= 0xFE)?
                        ((NdefMap->TLVStruct.NdefTLVByte + 
                        PH_FRINFC_MIFARESTD_VAL2)%
                        PH_FRINFC_MIFARESTD_VAL16):
                        ((NdefMap->TLVStruct.NdefTLVByte + 
                        PH_FRINFC_MIFARESTD_VAL4)%
                        PH_FRINFC_MIFARESTD_VAL16));

    if((*NdefMap->SendRecvLength == PH_FRINFC_MIFARESTD_BYTES_READ) && 
        (NdefMap->ApduBuffIndex < NdefMap->ApduBufferSize))
    {
        if(NdefMap->TLVStruct.BytesRemainLinTLV != 0)
        {
            NDEFFlag = PH_FRINFC_MIFARESTD_FLAG0;
            /* To read the remaining length (L) in TLV */
            Result = phFriNfc_MifStd_H_RemainTLV(NdefMap, &NDEFFlag, &TempLength);
        }
    }
    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_WrTermTLV(phFriNfc_NdefMap_t   *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint8_t     index = PH_FRINFC_MIFARESTD_VAL0;

    /* Change the state to check ndef compliancy */
    NdefMap->State = PH_FRINFC_NDEFMAP_STATE_TERM_TLV;
    
    NdefMap->SendRecvBuf[index] = 
                        NdefMap->StdMifareContainer.currentBlock;
    index++;
    NdefMap->SendRecvBuf[index] = PH_FRINFC_MIFARESTD_TERMTLV_T;
    index++;

    while(index < PH_FRINFC_MIFARESTD_WR_A_BLK)
    {
        NdefMap->SendRecvBuf[index] = PH_FRINFC_MIFARESTD_NULLTLV_T;
        index++;
    }

    NdefMap->TLVStruct.SetTermTLVFlag = PH_FRINFC_MIFARESTD_FLAG0;

    Result = phFriNfc_MifStd_H_WrTLV(NdefMap);
    
    return Result;
}


static NFCSTATUS phFriNfc_MifStd_H_ProWrABlock(phFriNfc_NdefMap_t   *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;

    NdefMap->StdMifareContainer.WrLength = PH_FRINFC_MIFARESTD_VAL0;
    if(NdefMap->ApduBuffIndex < 
        (uint16_t)NdefMap->ApduBufferSize)
    {
        /* Remaining bytes to write < 16 */
        if(NdefMap->StdMifareContainer.RemainingBufFlag == 
            PH_FRINFC_MIFARESTD_FLAG1)
        {
            /* Write complete, so next byte shall be */
            NdefMap->StdMifareContainer.internalLength =
                *NdefMap->DataCount;
        
            /* Copy bytes less than 16 to internal buffer
                for the next write this can be used */
            (void)memcpy( NdefMap->StdMifareContainer.internalBuf,
                    NdefMap->StdMifareContainer.Buffer,
                    NdefMap->StdMifareContainer.internalLength);

            /* Increment the Send Buffer index */
            NdefMap->ApduBuffIndex += NdefMap->NumOfBytesWritten;

            NdefMap->StdMifareContainer.remainingSize -= 
                    NdefMap->NumOfBytesWritten;

            NdefMap->StdMifareContainer.RemainingBufFlag = PH_FRINFC_MIFARESTD_VAL0;
            /* Check for the End of Card */
                NdefMap->StdMifareContainer.ReadWriteCompleteFlag = 
                    (uint8_t)((NdefMap->StdMifareContainer.remainingSize == 
                    PH_FRINFC_MIFARESTD_VAL0)?
                    PH_FRINFC_MIFARESTD_FLAG1:
                    PH_FRINFC_MIFARESTD_FLAG0);

            NdefMap->TLVStruct.SetTermTLVFlag = 
                    (uint8_t)(((NdefMap->StdMifareContainer.remainingSize == 
                            PH_FRINFC_MIFARESTD_VAL0) || 
                            (NdefMap->TLVStruct.SetTermTLVFlag == 
                            PH_FRINFC_MIFARESTD_FLAG1))?
                            PH_FRINFC_MIFARESTD_FLAG1:
                            PH_FRINFC_MIFARESTD_FLAG0);

        } /* internal Buffer > Send Buffer */
        else if(NdefMap->StdMifareContainer.internalBufFlag == 
                PH_FRINFC_MIFARESTD_FLAG1)
        {
            (void)memcpy(NdefMap->StdMifareContainer.internalBuf,
                    NdefMap->StdMifareContainer.Buffer,
                    *NdefMap->DataCount);

            NdefMap->StdMifareContainer.internalLength =
                                    *NdefMap->DataCount;

            /* Increment the Send Buffer index */
            NdefMap->ApduBuffIndex += 
                    NdefMap->NumOfBytesWritten;

            NdefMap->StdMifareContainer.remainingSize -= 
                    NdefMap->NumOfBytesWritten;

            NdefMap->StdMifareContainer.internalBufFlag = 
                                PH_FRINFC_MIFARESTD_FLAG0;
            /* Check for the End of Card */
            NdefMap->StdMifareContainer.ReadWriteCompleteFlag = 
                (uint8_t)(((NdefMap->StdMifareContainer.remainingSize == 
                    PH_FRINFC_MIFARESTD_VAL0) &&
                    (NdefMap->StdMifareContainer.internalLength == 
                    PH_FRINFC_MIFARESTD_VAL0))?
                    PH_FRINFC_MIFARESTD_FLAG1:
                    PH_FRINFC_MIFARESTD_FLAG0);

            NdefMap->TLVStruct.SetTermTLVFlag = 
                    (uint8_t)(((NdefMap->StdMifareContainer.remainingSize == 
                            PH_FRINFC_MIFARESTD_VAL0) || 
                            (NdefMap->TLVStruct.SetTermTLVFlag == 
                            PH_FRINFC_MIFARESTD_FLAG1))?
                            PH_FRINFC_MIFARESTD_FLAG1:
                            PH_FRINFC_MIFARESTD_FLAG0);
        }
        else
        {
            NdefMap->StdMifareContainer.internalLength = 0;
            /* Increment the Send Buffer index */
            NdefMap->ApduBuffIndex += 
                    NdefMap->NumOfBytesWritten;
            NdefMap->StdMifareContainer.remainingSize -= 
                    NdefMap->NumOfBytesWritten;
            
            /* Check for the End of Card */
            if((NdefMap->StdMifareContainer.remainingSize == 
                    PH_FRINFC_MIFARESTD_VAL0) || 
                    (NdefMap->ApduBuffIndex == NdefMap->ApduBufferSize))
            {
                NdefMap->StdMifareContainer.ReadWriteCompleteFlag = 
                (uint8_t)((NdefMap->StdMifareContainer.remainingSize == 0)?
                    PH_FRINFC_MIFARESTD_FLAG1:PH_FRINFC_MIFARESTD_FLAG0);

                if(NdefMap->StdMifareContainer.internalLength == 
                    PH_FRINFC_MIFARESTD_VAL0)
                {
                    NdefMap->StdMifareContainer.currentBlock++;
                    /* Mifare 4k Card, After 128th Block
                    each sector = 16 blocks in Mifare 4k */
                    Result = ((NdefMap->StdMifareContainer.remainingSize == 0)?
                                Result:
                                phFriNfc_MifStd_H_BlkChk(NdefMap));
                    NdefMap->StdMifareContainer.NdefBlocks++;
                }
                NdefMap->TLVStruct.SetTermTLVFlag = 
                    (uint8_t)(((NdefMap->StdMifareContainer.remainingSize == 
                            PH_FRINFC_MIFARESTD_VAL0) || 
                            (NdefMap->TLVStruct.SetTermTLVFlag == 
                            PH_FRINFC_MIFARESTD_FLAG1))?
                            PH_FRINFC_MIFARESTD_FLAG1:
                            PH_FRINFC_MIFARESTD_FLAG0);
            }
            else
            {
                NdefMap->StdMifareContainer.currentBlock++;
                NdefMap->StdMifareContainer.WrLength = 
                    (uint16_t)(NdefMap->ApduBufferSize - NdefMap->ApduBuffIndex);
                /* Mifare 4k Card, After 128th Block
                each sector = 16 blocks in Mifare 4k */
                Result = phFriNfc_MifStd_H_BlkChk(NdefMap);
                if(Result == NFCSTATUS_SUCCESS)
                {
                    NdefMap->StdMifareContainer.NdefBlocks++;
                    Result = ((NdefMap->StdMifareContainer.AuthDone == 
                                PH_FRINFC_MIFARESTD_FLAG1)?
                                phFriNfc_MifStd_H_WrABlock(NdefMap):
                                phFriNfc_MifStd_H_AuthSector(NdefMap));
                }
            }
        }
    }
    else
    {
        Result = PHNFCSTVAL(    CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_DEVICE_REQUEST); 
    }

    if((Result == NFCSTATUS_SUCCESS) && 
        (NdefMap->TLVStruct.SetTermTLVFlag != 
        PH_FRINFC_MIFARESTD_FLAG1) && 
        (NdefMap->StdMifareContainer.remainingSize > 
        PH_FRINFC_MIFARESTD_VAL0))
    {
        Result = phFriNfc_MifStd_H_WrTermTLV(NdefMap);
    }
    else 
    {
        if((Result == NFCSTATUS_SUCCESS) && 
            (NdefMap->TLVStruct.SetTermTLVFlag == 
            PH_FRINFC_MIFARESTD_FLAG1))
        {
            /* Write the length to the L field in the TLV */
            NdefMap->StdMifareContainer.TempBlockNo = 
                        NdefMap->StdMifareContainer.currentBlock;
            phFriNfc_MifStd_H_SetNdefBlkAuth(NdefMap);
            NdefMap->StdMifareContainer.currentBlock = 
                            NdefMap->TLVStruct.NdefTLVBlock;
            Result = phFriNfc_MifStd_H_RdtoWrNdefLen(NdefMap);
        }
    }
    return Result;
}

static NFCSTATUS phFriNfc_MifStd_H_CallDisCon(phFriNfc_NdefMap_t   *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    /*Set Ndef State*/
    NdefMap->State = PH_FRINFC_NDEFMAP_STATE_DISCONNECT;
    NdefMap->MapCompletionInfo.CompletionRoutine = phFriNfc_MifareStdMap_Process;
    NdefMap->MapCompletionInfo.Context = NdefMap; 

#ifndef PH_HAL4_ENABLE
    /*Call the Overlapped HAL POLL function */
    Result =  phFriNfc_OvrHal_Disconnect( NdefMap->LowerDevice,
                                          &NdefMap->MapCompletionInfo,
                                          NdefMap->psRemoteDevInfo);
#else
    /*Call the Overlapped HAL Reconnect function */
    Result =  phFriNfc_OvrHal_Reconnect( NdefMap->LowerDevice,
                                          &NdefMap->MapCompletionInfo,
                                          NdefMap->psRemoteDevInfo);
#endif

    return Result;
}

#ifndef PH_HAL4_ENABLE
static NFCSTATUS phFriNfc_MifStd_H_CallPoll(phFriNfc_NdefMap_t   *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    /*Set Ndef State*/
    NdefMap->State = PH_FRINFC_NDEFMAP_STATE_POLL;
    /* Opmodes */
    NdefMap->OpModeType[PH_FRINFC_MIFARESTD_VAL0] = phHal_eOpModesMifare;
    NdefMap->OpModeType[PH_FRINFC_MIFARESTD_VAL1] = phHal_eOpModesArrayTerminator;

    /* Number of devices to poll */
    NdefMap->NoOfDevices = PH_FRINFC_MIFARESTD_VAL1;

    /*Call the Overlapped HAL POLL function */
    Result =  phFriNfc_OvrHal_Poll( NdefMap->LowerDevice,
                                    &NdefMap->MapCompletionInfo,
                                    NdefMap->OpModeType,
                                    NdefMap->psRemoteDevInfo,
                                    &NdefMap->NoOfDevices,
                                    NdefMap->StdMifareContainer.DevInputParam);
    return Result;
}
#endif

static NFCSTATUS phFriNfc_MifStd_H_CallConnect(phFriNfc_NdefMap_t   *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    /*Set Ndef State*/
    NdefMap->State = PH_FRINFC_NDEFMAP_STATE_CONNECT;

    /*Call the Overlapped HAL POLL function */
    Result =  phFriNfc_OvrHal_Connect(  NdefMap->LowerDevice,
                                        &NdefMap->MapCompletionInfo,
                                        NdefMap->psRemoteDevInfo,
                                        NdefMap->StdMifareContainer.DevInputParam);
    return Result;
}

static void phFriNfc_MifStd1k_H_BlkChk(phFriNfc_NdefMap_t   *NdefMap,
                                    uint8_t              SectorID,
                                    uint8_t              *callbreak)
{
    /* every last block of a sector needs to be skipped */
    if(((NdefMap->StdMifareContainer.currentBlock + PH_FRINFC_MIFARESTD_INC_1) % 
        PH_FRINFC_MIFARESTD_BLK4) == 0)
    {
        NdefMap->StdMifareContainer.currentBlock++;                     
    }
    else 
    {
        if(NdefMap->StdMifareContainer.aid[SectorID] == 
            PH_FRINFC_MIFARESTD_NDEF_COMP)
        {
            /* Check whether the block is first block of a (next)new sector and 
            also check if it is first block then internal length is zero 
            or not. Because once Authentication is done for the sector again 
            we should not authenticate it again */
            if((NdefMap->StdMifareContainer.currentBlock == 
                (SectorID * PH_FRINFC_MIFARESTD_BLK4)) &&
                (NdefMap->StdMifareContainer.internalLength == 0))
            {
                NdefMap->StdMifareContainer.AuthDone = 0;
            }
            *callbreak = 1;
        }
        else
        {
            NdefMap->StdMifareContainer.currentBlock += PH_FRINFC_MIFARESTD_BLK4;
        }
    }
}

#ifdef UNIT_TEST
#include <phUnitTestNfc_MifareStd_static.c>
#endif

/*	Convert the Mifare card to ReadOnly.
        check preconditions before converting to read only
	1.shud b rd/write state
	2.check NDEF  for the card shud b completed
	3.if alrady read only return */
NFCSTATUS
phFriNfc_MifareStdMap_ConvertToReadOnly (
               phFriNfc_NdefMap_t *NdefMap,
               const uint8_t *ScrtKeyB)
{
    NFCSTATUS result = NFCSTATUS_SUCCESS;
    uint8_t totalNoSectors = 0 , sectorTrailerBlockNo = 0;

    if ( NdefMap == NULL)
    {
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_PARAMETER);
    }
    else if ( PH_NDEFMAP_CARD_STATE_INVALID == NdefMap->CardState )
    {
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_STATE);
    }
    else
    {
        /* card state is PH_NDEFMAP_CARD_STATE_READ_WRITE now */
        /* get AID  array and parse */
        if( PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD == NdefMap->CardType )
        {
            totalNoSectors  = PH_FRINFC_MIFARESTD1K_TOTAL_SECTOR;
        }
        else if ( PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD == NdefMap->CardType )
        {
             totalNoSectors  = PH_FRINFC_MIFARESTD4K_TOTAL_SECTOR;
        }

        /* Store Key B in the context */
        if(ScrtKeyB == NULL)
        {
            memset (NdefMap->StdMifareContainer.UserScrtKeyB, PH_FRINFC_MFSTD_FMT_DEFAULT_KEY,
                    PH_FRINFC_MIFARESTD_KEY_LEN);
        }
        else
        {
            memcpy (NdefMap->StdMifareContainer.UserScrtKeyB, ScrtKeyB, PH_FRINFC_MIFARESTD_KEY_LEN);
        }

        NdefMap->StdMifareContainer.TotalNoSectors = totalNoSectors;
        if(totalNoSectors == 0)
        {
            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_PARAMETER);
        }
        else
        {
            NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->StdMifareContainer.RdBeforeWrFlag = PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->StdMifareContainer.WrNdefFlag = PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->StdMifareContainer.internalLength = PH_FRINFC_MIFARESTD_VAL0;
            NdefMap->StdMifareContainer.RdAfterWrFlag = PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->StdMifareContainer.AuthDone = PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->StdMifareContainer.NFCforumSectFlag = PH_FRINFC_MIFARESTD_FLAG0;
            NdefMap->StdMifareContainer.WriteAcsBitFlag = PH_FRINFC_MIFARESTD_FLAG0;

            /* Sector 0 is MAD sector .Start from Sector 1 */
            for(NdefMap->StdMifareContainer.ReadOnlySectorIndex = PH_FRINFC_MIFARESTD_FLAG1;
                NdefMap->StdMifareContainer.ReadOnlySectorIndex < totalNoSectors;
                NdefMap->StdMifareContainer.ReadOnlySectorIndex++)
            {
                /* skip MAD sectors */
                if( PH_FRINFC_MIFARESTD_SECTOR_NO16 == NdefMap->StdMifareContainer.ReadOnlySectorIndex  )
                {
                    continue;
                }

                /* if not NDEF compliant skip  */
                if( PH_FRINFC_MIFARESTD_NON_NDEF_COMP ==
                    NdefMap->StdMifareContainer.aid[NdefMap->StdMifareContainer.ReadOnlySectorIndex])
                {
                    continue;
                }

                if (PH_FRINFC_MIFARESTD_NDEF_COMP ==
                     NdefMap->StdMifareContainer.aid[NdefMap->StdMifareContainer.ReadOnlySectorIndex])
                {
                    /*get the sector trailer block number */
                    sectorTrailerBlockNo =
                        phFriNfc_MifStd_H_GetSectorTrailerBlkNo(NdefMap->StdMifareContainer.ReadOnlySectorIndex);
                    NdefMap->StdMifareContainer.currentBlock = sectorTrailerBlockNo;
                    NdefMap->StdMifareContainer.SectorTrailerBlockNo = sectorTrailerBlockNo;

                    /* Proceed to authenticate the sector with Key B
                       and  modify the sector trailor bits to make it read only*/
                    result = phFriNfc_MifStd_H_AuthSector(NdefMap);

                    if (result == NFCSTATUS_PENDING )
                    {
                        break;
                    }
                }
            } /* end for */

            /* There are no NDEF sectors in this card , return */
            if(NdefMap->StdMifareContainer.ReadOnlySectorIndex == totalNoSectors &&
               NFCSTATUS_PENDING!= result )
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_NO_NDEF_SUPPORT);
            }
        } /* end else */
    }

    return result;
}

/* Get the block number of the sector trailor for the given sector trailer Id */
static uint8_t phFriNfc_MifStd_H_GetSectorTrailerBlkNo(uint8_t SectorID)
{
    uint8_t sectorTrailerblockNumber = 0;

    /* every last block of a sector needs to be skipped */
    if( SectorID < PH_FRINFC_MIFARESTD_SECTOR_NO32 )
    {
        sectorTrailerblockNumber = ( SectorID * PH_FRINFC_MIFARESTD_BLK4 ) + 3;
    }
    else
    {
        sectorTrailerblockNumber = ((PH_FRINFC_MIFARESTD_SECTOR_NO32 * PH_FRINFC_MIFARESTD_BLK4) +
            ((SectorID - PH_FRINFC_MIFARESTD_SECTOR_NO32) * PH_FRINFC_MIFARESTD_SECTOR_BLOCKS)) + 15;
    }

    return sectorTrailerblockNumber;
}

/* Called during ConvertToReadonly process to Authenticate NDEF compliant Sector */
static NFCSTATUS phFriNfc_MifStd_H_ProSectorTrailorAcsBits(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;

    if(*NdefMap->SendRecvLength == PH_FRINFC_MIFARESTD_BYTES_READ)
    {
        if(NdefMap->StdMifareContainer.ReadAcsBitFlag ==
            PH_FRINFC_MIFARESTD_FLAG1)
        {
            /* check for the correct access bits */
            Result = phFriNfc_MifStd_H_ChkAcsBit(NdefMap);
            if(Result  == NFCSTATUS_SUCCESS)
            {

                if(NdefMap->CardState == PH_NDEFMAP_CARD_STATE_READ_ONLY)
                {
                    /* Go to next sector */
                    Result = phFriNfc_MifStd_H_ProWrSectorTrailor(NdefMap);
                }
                else
                {
                    /* tranceive to write the data into SendRecvBuff */
                    Result = phFriNfc_MifStd_H_WrSectorTrailorBlock(NdefMap);
                }
            }
        }
    }
    else
    {
        Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                     NFCSTATUS_INVALID_PARAMETER);
    }

    return Result;
}

/* Make current NDEF compliant Sector ReadOnly
   modify the sector trailor bits and write it to the card*/
static NFCSTATUS phFriNfc_MifStd_H_WrSectorTrailorBlock(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS status = NFCSTATUS_PENDING;

    /* set the data for additional data exchange*/
    NdefMap->psDepAdditionalInfo.DepFlags.MetaChaining = 0;
    NdefMap->psDepAdditionalInfo.DepFlags.NADPresent = 0;
    NdefMap->psDepAdditionalInfo.NAD = 0;
    NdefMap->MapCompletionInfo.CompletionRoutine = phFriNfc_MifareStdMap_Process;
    NdefMap->MapCompletionInfo.Context = NdefMap;
    NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_WRITE_OPE;

    /* next state (update sector index) */
    NdefMap->State = PH_FRINFC_NDEFMAP_STATE_WRITE_SEC;

    /* Buffer Check */
    if(NdefMap->SendRecvBuf != NULL)
    {
        NdefMap->SendRecvBuf[10] = 0x00;
        NdefMap->SendRecvBuf[10] = NdefMap->SendRecvBuf[9] | PH_FRINFC_MIFARESTD_MASK_GPB_WR; /* WR bits 11*/

        /*The NdefMap->SendRecvBuf already has the sector trailor.
        modify the bits to make Read Only */
        NdefMap->SendRecvBuf[1] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_NDEFSECT1; /* 0xD3 */
        NdefMap->SendRecvBuf[2] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_NDEFSECT2; /* 0xF7 */
        NdefMap->SendRecvBuf[3] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_NDEFSECT1; /* 0xD3 */
        NdefMap->SendRecvBuf[4] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_NDEFSECT2; /* 0xF7 */
        NdefMap->SendRecvBuf[5] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_NDEFSECT1; /* 0xD3 */
        NdefMap->SendRecvBuf[6] = PH_FRINFC_NDEFMAP_MIFARESTD_AUTH_NDEFSECT2; /* 0xF7 */

        NdefMap->SendRecvBuf[7] = PH_FRINFC_MIFARESTD_NFCSECT_RDACS_BYTE6;/* 0x07 */
        NdefMap->SendRecvBuf[8] = PH_FRINFC_MIFARESTD_NFCSECT_RDACS_BYTE7;/* 0x8F */
        NdefMap->SendRecvBuf[9] = PH_FRINFC_MIFARESTD_NFCSECT_RDACS_BYTE8;/* 0x0F */

        NdefMap->SendRecvBuf[11] = NdefMap->StdMifareContainer.UserScrtKeyB[0];
        NdefMap->SendRecvBuf[12] = NdefMap->StdMifareContainer.UserScrtKeyB[1];
        NdefMap->SendRecvBuf[13] = NdefMap->StdMifareContainer.UserScrtKeyB[2];
        NdefMap->SendRecvBuf[14] = NdefMap->StdMifareContainer.UserScrtKeyB[3];
        NdefMap->SendRecvBuf[15] = NdefMap->StdMifareContainer.UserScrtKeyB[4];
        NdefMap->SendRecvBuf[16] = NdefMap->StdMifareContainer.UserScrtKeyB[5];

        /* Write to Ndef Sector Block */
        NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL0] = NdefMap->StdMifareContainer.currentBlock;

        /* Copy Ndef Sector Block into buffer */
        (void)memcpy(NdefMap->StdMifareContainer.Buffer,
                    &(NdefMap->SendRecvBuf[PH_FRINFC_MIFARESTD_VAL1]),
                    PH_FRINFC_MIFARESTD_BLOCK_BYTES);

        /* Write from here */
        NdefMap->SendLength = MIFARE_MAX_SEND_BUF_TO_WRITE;
#ifndef PH_HAL4_ENABLE
        NdefMap->Cmd.MfCmd = phHal_eMifareCmdListMifareWrite16;
#else
        NdefMap->Cmd.MfCmd = phHal_eMifareWrite16;
#endif
        *NdefMap->SendRecvLength = NdefMap->TempReceiveLength;

        /* Call the Overlapped HAL Transceive function */
        status = phFriNfc_OvrHal_Transceive(NdefMap->LowerDevice,
                                           &NdefMap->MapCompletionInfo,
                                           NdefMap->psRemoteDevInfo,
                                           NdefMap->Cmd,
                                           &NdefMap->psDepAdditionalInfo,
                                           NdefMap->SendRecvBuf,
                                           NdefMap->SendLength,
                                           NdefMap->SendRecvBuf,
                                           NdefMap->SendRecvLength);
    }
    else
    {
        /* Error: The control should not ideally come here.
           Return Error.*/
#ifndef PH_HAL4_ENABLE
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_CMD_ABORTED);
#else
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_FAILED);
#endif
    }

    return status;
}

/* Make next NDEF compliant Sector ReadOnly */
static NFCSTATUS phFriNfc_MifStd_H_ProWrSectorTrailor(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS status =  NFCSTATUS_FAILED;
    uint8_t sectorTrailerBlockNo = 0;

    /*Increment Sector Index */
    NdefMap->StdMifareContainer.ReadOnlySectorIndex++;

    /* skip if MAD2 */
    if(PH_FRINFC_MIFARESTD_SECTOR_NO16 == NdefMap->StdMifareContainer.ReadOnlySectorIndex )
    {
        NdefMap->StdMifareContainer.ReadOnlySectorIndex++;
    }

    /* if current sector index exceeds total sector index then
       all ndef sectors are made readonly then return success
       If a NON def sector is encountered return success*/
    if (NdefMap->StdMifareContainer.ReadOnlySectorIndex >= NdefMap->StdMifareContainer.TotalNoSectors ||
        PH_FRINFC_MIFARESTD_NON_NDEF_COMP  ==
        NdefMap->StdMifareContainer.aid[NdefMap->StdMifareContainer.ReadOnlySectorIndex])
    {
        status = NFCSTATUS_SUCCESS;
    }
    else if(PH_FRINFC_MIFARESTD_NDEF_COMP  == NdefMap->StdMifareContainer.aid[NdefMap->StdMifareContainer.ReadOnlySectorIndex])
    {
        /* Convert next NDEF sector to read only */
        sectorTrailerBlockNo = phFriNfc_MifStd_H_GetSectorTrailerBlkNo(NdefMap->StdMifareContainer.ReadOnlySectorIndex);
        NdefMap->StdMifareContainer.currentBlock = sectorTrailerBlockNo;
        NdefMap->StdMifareContainer.SectorTrailerBlockNo = sectorTrailerBlockNo;

        status = phFriNfc_MifStd_H_AuthSector(NdefMap);
    }

    return status;
}

#endif  /* PH_FRINFC_MAP_MIFARESTD_DISABLED */
