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
 * \file  phFriNfc_MifULFormat.c
 * \brief NFC Ndef Formatting For Mifare ultralight card.
 *
 * Project: NFC-FRI
 *
 * $Date: Mon Dec 13 14:14:12 2010 $
 * $Author: ing02260 $
 * $Revision: 1.9 $
 * $Aliases:  $
 *
 */

#include <phFriNfc_MifULFormat.h>
#include <phFriNfc_OvrHal.h>

/*! \ingroup grp_file_attributes
 *  \name NDEF Mapping
 *
 * File: \ref phFriNfc_MifULFormat.c
 *
 */
/*@{*/
#define PHFRINFCMIFULFORMAT_FILEREVISION "$Revision: 1.9 $"
#define PHFRINFCMIFULFORMAT_FILEALIASES  "$Aliases:  $"
/*@}*/

#ifdef FRINFC_READONLY_NDEF
    /* Mifare UL OTP block number is 3 */
    #define RD_LOCK_OTP_BLOCK_NUMBER            0x02U
    #define OTP_BLOCK_NUMBER                    0x03U
    /* READ ONLY value that shall be written in the OTP to make the card read only */
    #define READ_ONLY_VALUE_IN_OTP              0x0FU
    /* Mifare UL OTP block number is 3 */
    #define MIFARE_UL_READ_MAX_SIZE             16U
    /* 1st Lock byte value */
    #define MIFARE_UL_LOCK_BYTE1_VALUE          0xF8U
    /* 2nd Lock byte value */
    #define MIFARE_UL_LOCK_BYTE2_VALUE          0xFFU
    /* Mifare ULC dynamic lock byte address */
    #define MIFARE_ULC_DYNAMIC_LOCK_BYTES_ADDR  0x28U
    /* Type 2 STATIC CARD memory value in the OTP */
    #define TYPE_2_STATIC_MEM_SIZE_VALUE        0x06U
    /* Type 2 DYNAMIC CARD memory value in the OTP */
    #define TYPE_2_DYNAMIC_MEM_SIZE_VALUE       0x12U
    /* Lock byte 3 value to be ORed with the existing value */
    #define MIFARE_UL_LOCK_BYTE3_VALUE          0xEEU
    /* Posiiton of the memory information in the stored OTP bytes */
    #define TYPE_2_MEM_SIZE_POSITION            0x02U
    /* 3rd Lock byte position after reading the block number 0x28 */
    #define TYPE_2_LOCK_BYTE3_POS_RD_BLK28      0x00U

#ifdef PH_NDEF_MIFARE_ULC

    /* Lock control TLVs, TYPE identifier */
    #define LOCK_CTRL_TYPE_IN_TLV               0x01U
    /* Lock control TLVs, Length expected */
    #define LOCK_CTRL_LEN_IN_TLV                0x03U

    /* NDEF message TLVs, TYPE identifier */
    #define NDEF_TYPE_IN_TLV                    0x03U

    #define MFUL_NULL_TLV                       0x00U
    #define THREE_BYTE_LENGTH_FIELD             0xFFU
    #define TERMINATOR_TLV                      0xFEU
    #define MIFARE_ULC_SIZE                     0xC0U
    #define MFUL_NIBBLE_SIZE                    0x04U
    #define MFUL_NIBBLE_MASK                    0x0FU
    #define MFUL_BYTE_SIZE_IN_BITS              0x08U
    #define MFUL_BLOCK_SIZE_IN_BYTES            0x04U
    /* Initial (0 to 3 blocks) 4 blocks are ignored, i.e., 16 bytes */
    #define MFUL_INITIAL_BYTES_IGNORED          0x10U

    #define MFUL_CONVERT_BITS_TO_BYTES(bits_to_bytes) \
            (((bits_to_bytes % MFUL_BYTE_SIZE_IN_BITS) > 0) ? \
            ((bits_to_bytes / MFUL_BYTE_SIZE_IN_BITS) + 1) : \
            (bits_to_bytes / MFUL_BYTE_SIZE_IN_BITS))

    typedef enum phFriNfc_MfUL_Parse
    {
        LOCK_TLV_T, 
        LOCK_TLV_L,
        LOCK_TLV_V, 
        NDEF_TLV_T, 
        NDEF_TLV_L,
        NDEF_TLV_V
    }phFriNfc_MfUL_Parse_t;

#endif /* #ifdef PH_NDEF_MIFARE_ULC */

#endif /* #ifdef FRINFC_READONLY_NDEF */
/*!
* \brief \copydoc page_ovr Helper function for Mifare UL. This function calls the   
* transceive function
*/
static NFCSTATUS phFriNfc_MfUL_H_Transceive(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt);

/*!
* \brief \copydoc page_ovr Helper function for Mifare UL. This function calls the   
* read or write operation
*/
static NFCSTATUS phFriNfc_MfUL_H_WrRd(phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt);

/*!
* \brief \copydoc page_ovr Helper function for Mifare UL. This function fills the  
* send buffer for transceive function
*/
static void phFriNfc_MfUL_H_fillSendBuf(phFriNfc_sNdefSmtCrdFmt_t   *NdefSmtCrdFmt,
                                        uint8_t                     BlockNo);

/*!
* \brief \copydoc page_ovr Helper function for Mifare UL. This function shall process
* the read bytes
*/
static NFCSTATUS phFriNfc_MfUL_H_ProRd16Bytes(phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt);

/*!
* \brief \copydoc page_ovr Helper function for Mifare UL. This function shall process the
* OTP bytes written
*/
static NFCSTATUS phFriNfc_MfUL_H_ProWrOTPBytes(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt);

#ifdef FRINFC_READONLY_NDEF

#ifdef PH_NDEF_MIFARE_ULC

static 
NFCSTATUS 
phFriNfc_MfUL_ParseTLVs (
    phFriNfc_sNdefSmtCrdFmt_t       *NdefSmtCrdFmt, 
    uint8_t                         *data_to_parse, 
    uint8_t                         size_to_parse);

static 
NFCSTATUS 
phFriNfc_MfUL_GetLockBytesInfo (
    phFriNfc_sNdefSmtCrdFmt_t       *NdefSmtCrdFmt);

static 
NFCSTATUS 
phFriNfc_MfUL_GetDefaultLockBytesInfo (
    phFriNfc_sNdefSmtCrdFmt_t          *NdefSmtCrdFmt);

static 
uint8_t 
phFriNfc_MfUL_GetSkipSize (
    phFriNfc_sNdefSmtCrdFmt_t       *NdefSmtCrdFmt, 
    uint8_t                         block_number, 
    uint8_t                         byte_number);

static 
NFCSTATUS 
phFriNfc_MfUL_ReadWriteLockBytes (
    phFriNfc_sNdefSmtCrdFmt_t          *NdefSmtCrdFmt);

static 
NFCSTATUS
phFriNfc_MfUL_UpdateAndWriteLockBits (
    phFriNfc_sNdefSmtCrdFmt_t          *NdefSmtCrdFmt);

static 
uint8_t
phFriNfc_MfUL_CalcRemainingLockBits (
    phFriNfc_sNdefSmtCrdFmt_t          *NdefSmtCrdFmt);

#endif /* #ifdef PH_NDEF_MIFARE_ULC */

#endif /* #ifdef FRINFC_READONLY_NDEF */

static int MemCompare1 ( void *s1, void *s2, unsigned int n );
/*The function does a comparision of two strings and returns a non zero value 
if two strings are unequal*/
static int MemCompare1 ( void *s1, void *s2, unsigned int n )
{
    int8_t   diff = 0;
    int8_t *char_1  =(int8_t *)s1;
    int8_t *char_2  =(int8_t *)s2;
    if(NULL == s1 || NULL == s2)
    {
        PHDBG_CRITICAL_ERROR("NULL pointer passed to memcompare");
    }
    else
    {
        for(;((n>0)&&(diff==0));n--,char_1++,char_2++)
        {
            diff = *char_1 - *char_2;
        }
    }
    return (int)diff;
}

void phFriNfc_MfUL_Reset(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt)
{
    uint8_t OTPByte[] = PH_FRINFC_MFUL_FMT_OTP_BYTES; 

    NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock = PH_FRINFC_MFUL_FMT_VAL_0;
    (void)memcpy(NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes,
                OTPByte,
                sizeof(NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes));
#ifdef FRINFC_READONLY_NDEF
    NdefSmtCrdFmt->AddInfo.Type2Info.LockBytes[0] = 0;
    NdefSmtCrdFmt->AddInfo.Type2Info.LockBytes[1] = 0;
    NdefSmtCrdFmt->AddInfo.Type2Info.LockBytes[2] = 0;
    NdefSmtCrdFmt->AddInfo.Type2Info.LockBytes[3] = 0;
#endif /* #ifdef FRINFC_READONLY_NDEF */
}

NFCSTATUS phFriNfc_MfUL_Format(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt)
{
    NFCSTATUS               Result = NFCSTATUS_SUCCESS;
    uint8_t                 OTPByte[] = PH_FRINFC_MFUL_FMT_OTP_BYTES;
    
    NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock = PH_FRINFC_MFUL_FMT_VAL_0;
    (void)memcpy(NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes,
                OTPByte,
                sizeof(NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes));

    /* Set the state */
    NdefSmtCrdFmt->State = PH_FRINFC_MFUL_FMT_RD_16BYTES;
    /* Initialise current block to the lock bits block */
    NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock = PH_FRINFC_MFUL_FMT_VAL_2;
    
    /* Start authentication */
    Result = phFriNfc_MfUL_H_WrRd(NdefSmtCrdFmt);
    return Result;
}

#ifdef FRINFC_READONLY_NDEF

NFCSTATUS
phFriNfc_MfUL_ConvertToReadOnly (
    phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt)
{
    NFCSTATUS               result = NFCSTATUS_SUCCESS;

    NdefSmtCrdFmt->AddInfo.Type2Info.DefaultLockBytesFlag = TRUE;
    NdefSmtCrdFmt->AddInfo.Type2Info.ReadDataIndex = 0;

    NdefSmtCrdFmt->State = PH_FRINFC_MFUL_FMT_RO_RD_16BYTES;

    result = phFriNfc_MfUL_H_WrRd (NdefSmtCrdFmt);

    return result;
}

#endif /* #ifdef FRINFC_READONLY_NDEF */

void phFriNfc_MfUL_Process(void             *Context,
                           NFCSTATUS        Status)
{
    phFriNfc_sNdefSmtCrdFmt_t  *NdefSmtCrdFmt = (phFriNfc_sNdefSmtCrdFmt_t *)Context;
    
    if(Status == NFCSTATUS_SUCCESS)
    {
        switch(NdefSmtCrdFmt->State)
        {
        case PH_FRINFC_MFUL_FMT_RD_16BYTES:
            Status = phFriNfc_MfUL_H_ProRd16Bytes(NdefSmtCrdFmt);
            break;

        case PH_FRINFC_MFUL_FMT_WR_OTPBYTES:
            Status = phFriNfc_MfUL_H_ProWrOTPBytes(NdefSmtCrdFmt);
            break;

        case PH_FRINFC_MFUL_FMT_WR_TLV:
#ifdef PH_NDEF_MIFARE_ULC               
            if (NdefSmtCrdFmt->CardType == PH_FRINFC_NDEFMAP_MIFARE_ULC_CARD)
            {
                /* Write NDEF TLV in block number 5 */
                NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock = 
                                                        PH_FRINFC_MFUL_FMT_VAL_5;
                /* Card already have the OTP bytes so write TLV */
                NdefSmtCrdFmt->State = PH_FRINFC_MFUL_FMT_WR_TLV1;
                
                Status = phFriNfc_MfUL_H_WrRd (NdefSmtCrdFmt);
            }
#endif /* #ifdef PH_NDEF_MIFARE_ULC */

            break;

#ifdef FRINFC_READONLY_NDEF

        case PH_FRINFC_MFUL_FMT_RO_RD_16BYTES:
        {
            if (MIFARE_UL_READ_MAX_SIZE == *NdefSmtCrdFmt->SendRecvLength)
            {
                uint8_t         otp_lock_page_size = 0;
                uint8_t         i = 0;

                otp_lock_page_size = sizeof (NdefSmtCrdFmt->AddInfo.Type2Info.LockBytes);
                (void)memcpy ((void *)NdefSmtCrdFmt->AddInfo.Type2Info.LockBytes,
                            (void *)NdefSmtCrdFmt->SendRecvBuf,
                            sizeof(NdefSmtCrdFmt->AddInfo.Type2Info.LockBytes));

                NdefSmtCrdFmt->AddInfo.Type2Info.LockBytes[2] = (uint8_t)
                                    (NdefSmtCrdFmt->AddInfo.Type2Info.LockBytes[2] 
                                    | MIFARE_UL_LOCK_BYTE1_VALUE);
                NdefSmtCrdFmt->AddInfo.Type2Info.LockBytes[3] = MIFARE_UL_LOCK_BYTE2_VALUE;
                i = (uint8_t)(i + otp_lock_page_size);

                otp_lock_page_size = sizeof (NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes);                                
                
                (void)memcpy ((void *)NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes,
                            (void *)(NdefSmtCrdFmt->SendRecvBuf + i),
                            sizeof(NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes));

                NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes[(otp_lock_page_size - 1)] =
                                                        READ_ONLY_VALUE_IN_OTP;

                switch (NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes[TYPE_2_MEM_SIZE_POSITION])
                {
                    case TYPE_2_STATIC_MEM_SIZE_VALUE:
                    {
                        NdefSmtCrdFmt->State = PH_FRINFC_MFUL_FMT_RO_WR_OTP_BYTES;
                        Status = phFriNfc_MfUL_H_WrRd (NdefSmtCrdFmt);
                        break;
                    }

#ifdef PH_NDEF_MIFARE_ULC
                    case TYPE_2_DYNAMIC_MEM_SIZE_VALUE:
                    {
                        NdefSmtCrdFmt->State = 
                                PH_FRINFC_MFUL_FMT_RO_NDEF_PARSE_RD_BYTES;

                        /* Start reading from block 4 */
                        NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock = 4;
                        Status = phFriNfc_MfUL_H_WrRd (NdefSmtCrdFmt);
                        break;
                    }
#endif /* #ifdef PH_NDEF_MIFARE_ULC */

                    default:
                    {
                        Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                            NFCSTATUS_INVALID_DEVICE_REQUEST);
                        break;
                    }
                }
            }
            else
            {
                Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            break;
        }

        case PH_FRINFC_MFUL_FMT_RO_WR_OTP_BYTES:
        {
            switch (NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes[TYPE_2_MEM_SIZE_POSITION])
            {
                case TYPE_2_STATIC_MEM_SIZE_VALUE:
#ifdef PH_NDEF_MIFARE_ULC
                case TYPE_2_DYNAMIC_MEM_SIZE_VALUE:
#endif /* #ifdef PH_NDEF_MIFARE_ULC */
                {
                    NdefSmtCrdFmt->State = PH_FRINFC_MFUL_FMT_RO_WR_LOCK_BYTES;
                    Status = phFriNfc_MfUL_H_WrRd (NdefSmtCrdFmt);
                    break;
                }

                default:
                {
                    Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                        NFCSTATUS_INVALID_DEVICE_REQUEST);
                    break;
                }
            }
            break;
        }

#ifdef PH_NDEF_MIFARE_ULC

        case PH_FRINFC_MFUL_FMT_RO_NDEF_PARSE_RD_BYTES:
        {
            if (MIFARE_UL_READ_MAX_SIZE == *NdefSmtCrdFmt->SendRecvLength)
            {
                Status = phFriNfc_MfUL_ParseTLVs (NdefSmtCrdFmt, 
                                        NdefSmtCrdFmt->SendRecvBuf, 
                                        (uint8_t)*NdefSmtCrdFmt->SendRecvLength);

                if (!Status)
                {
                    NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock = 
                        NdefSmtCrdFmt->AddInfo.Type2Info.LockBlockNumber;
                    Status = phFriNfc_MfUL_ReadWriteLockBytes (NdefSmtCrdFmt);
                }
            }
            else
            {
                Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            break;
        }

        case PH_FRINFC_MFUL_FMT_RO_RD_DYN_LOCK_BYTES:
        {
            if (MIFARE_UL_READ_MAX_SIZE == *NdefSmtCrdFmt->SendRecvLength)
            {
                (void)memcpy ((void *)NdefSmtCrdFmt->AddInfo.Type2Info.ReadData,
                            (void *)NdefSmtCrdFmt->SendRecvBuf,
                            sizeof(NdefSmtCrdFmt->AddInfo.Type2Info.ReadData));

                NdefSmtCrdFmt->AddInfo.Type2Info.ReadDataIndex = 0;

                Status = phFriNfc_MfUL_UpdateAndWriteLockBits (NdefSmtCrdFmt);
                
            }
            else
            {
                Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            } 
            break;
        }

        case PH_FRINFC_MFUL_FMT_RO_WR_DYN_LOCK_BYTES:
        {
            NdefSmtCrdFmt->AddInfo.Type2Info.ReadDataIndex = (uint8_t)
                                    (NdefSmtCrdFmt->AddInfo.Type2Info.ReadDataIndex + 
                                    MFUL_BLOCK_SIZE_IN_BYTES);

            if (!phFriNfc_MfUL_CalcRemainingLockBits (NdefSmtCrdFmt))
            {
                /* There is no lock bits to write, then write OTP bytes */
                NdefSmtCrdFmt->State = PH_FRINFC_MFUL_FMT_RO_WR_OTP_BYTES;
                Status = phFriNfc_MfUL_H_WrRd (NdefSmtCrdFmt);
            }
            else if ((NdefSmtCrdFmt->AddInfo.Type2Info.ReadDataIndex < 
                MIFARE_UL_READ_MAX_SIZE) 
                && (phFriNfc_MfUL_CalcRemainingLockBits (NdefSmtCrdFmt)))
            {
                /* If remaining lock bits has to be written and the data is already read */
                Status = phFriNfc_MfUL_UpdateAndWriteLockBits (NdefSmtCrdFmt);
            }
            else
            {
                /* Increment current block by 4 because if a data is read then 16 
                    bytes will be given which is 4 blocks */
                NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock = (uint8_t)
                            (NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock + 4);
                Status = phFriNfc_MfUL_ReadWriteLockBytes (NdefSmtCrdFmt);
            }
            break;
        }

#endif /* #ifdef PH_NDEF_MIFARE_ULC */

        case PH_FRINFC_MFUL_FMT_RO_WR_LOCK_BYTES:
        {
            /* Do nothing */
            break;
        }

#endif /* #ifdef FRINFC_READONLY_NDEF */

#ifdef PH_NDEF_MIFARE_ULC   
        case PH_FRINFC_MFUL_FMT_WR_TLV1:
        
        break;
#endif /* #ifdef PH_NDEF_MIFARE_ULC */
        
        default:
            Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                NFCSTATUS_INVALID_DEVICE_REQUEST);
            break;
        }
    }
    /* Status is not success then call completion routine */
    if(Status != NFCSTATUS_PENDING)
    {
        phFriNfc_SmtCrdFmt_HCrHandler(NdefSmtCrdFmt, Status);
    }
}

#ifdef FRINFC_READONLY_NDEF

#ifdef PH_NDEF_MIFARE_ULC

static 
uint8_t 
phFriNfc_MfUL_GetSkipSize (
    phFriNfc_sNdefSmtCrdFmt_t       *NdefSmtCrdFmt, 
    uint8_t                         block_number, 
    uint8_t                         byte_number)
{
    uint8_t                     skip_size = 0;
    phFriNfc_Type2_AddInfo_t    *ps_type2_info = 
                                &NdefSmtCrdFmt->AddInfo.Type2Info;

    /* This check is added, because the default lock bits is always 
        present after the DATA AREA. 
        So, default lock bytes doesnt have any skip size */
    if (!ps_type2_info->DefaultLockBytesFlag)
    {
        /* Only check for the lock control TLV */
        if ((block_number == ps_type2_info->LockBlockNumber) 
            && (byte_number == ps_type2_info->LockByteNumber))
        {
            skip_size = MFUL_CONVERT_BITS_TO_BYTES(ps_type2_info->NoOfLockBits);
        }
    }
    
    return skip_size;
}

static 
NFCSTATUS 
phFriNfc_MfUL_GetLockBytesInfo (
    phFriNfc_sNdefSmtCrdFmt_t          *NdefSmtCrdFmt)
{
    NFCSTATUS                       result = NFCSTATUS_SUCCESS;
    phFriNfc_Type2_AddInfo_t        *ps_type2_info = 
                                    &(NdefSmtCrdFmt->AddInfo.Type2Info);
    uint8_t                         page_address = 0;
    uint8_t                         bytes_offset = 0;
    uint8_t                         lock_index = 0;


    page_address = (uint8_t)(ps_type2_info->DynLockBytes[lock_index] >> MFUL_NIBBLE_SIZE);
    bytes_offset = (uint8_t)(ps_type2_info->DynLockBytes[lock_index] & MFUL_NIBBLE_MASK);

    lock_index = (lock_index + 1);
    ps_type2_info->NoOfLockBits = ps_type2_info->DynLockBytes[lock_index];

    lock_index = (lock_index + 1);
    ps_type2_info->LockBytesPerPage = 
                            (ps_type2_info->DynLockBytes[lock_index] & MFUL_NIBBLE_MASK);
    ps_type2_info->BytesLockedPerLockBit = 
                            (ps_type2_info->DynLockBytes[lock_index] >> MFUL_NIBBLE_SIZE);

    /* Apply the formula to calculate byte address 
        ByteAddr = ((PageAddr * (2 ^ BytesPerPage)) + ByteOffset)
    */
    ps_type2_info->LockByteNumber = (uint8_t)((page_address 
                                * (1 << ps_type2_info->LockBytesPerPage))
                                + bytes_offset);

    ps_type2_info->LockBlockNumber = (uint8_t)(ps_type2_info->LockByteNumber / 
                                                MFUL_BLOCK_SIZE_IN_BYTES);
    ps_type2_info->LockByteNumber = (uint8_t)(ps_type2_info->LockByteNumber % 
                                                MFUL_BLOCK_SIZE_IN_BYTES);

#if 0
    if (
        /* Out of bound memory check */    
        ((ps_locktlv_info->ByteAddr + ps_locktlv_info->Size) > 
        (uint16_t)(psNdefMap->TopazContainer.CCByteBuf[2] * 
        TOPAZ_BYTES_PER_BLOCK)) || 

        /* Check the static lock and reserved areas memory blocks */
        ((ps_locktlv_info->ByteAddr >= TOPAZ_STATIC_LOCK_RES_START) && 
        (ps_locktlv_info->ByteAddr < TOPAZ_STATIC_LOCK_RES_END)) || 
        (((ps_locktlv_info->ByteAddr + ps_locktlv_info->Size - 1) >= 
        TOPAZ_STATIC_LOCK_RES_START) && 
        ((ps_locktlv_info->ByteAddr + ps_locktlv_info->Size - 1) < 
        TOPAZ_STATIC_LOCK_RES_END))
        )
    {
        ps_locktlv_info->ByteAddr = 0;
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                            NFCSTATUS_NO_NDEF_SUPPORT);
    }
    else
    {
        ps_locktlv_info->BlkNum = (ps_locktlv_info->ByteAddr / 
                                    TOPAZ_BYTES_PER_BLOCK);
        ps_locktlv_info->ByteNum = (ps_locktlv_info->ByteAddr % 
                                    TOPAZ_BYTES_PER_BLOCK);
    }
#endif /* #if 0 */

    return result;
}

static 
uint8_t
phFriNfc_MfUL_CalcRemainingLockBits (
    phFriNfc_sNdefSmtCrdFmt_t          *NdefSmtCrdFmt)
{
    uint8_t                         lock_bits_remaining = 0;
    phFriNfc_Type2_AddInfo_t        *ps_type2_info = 
                                    &(NdefSmtCrdFmt->AddInfo.Type2Info);

    lock_bits_remaining = (uint8_t)(ps_type2_info->NoOfLockBits - 
                                    ps_type2_info->LockBitsWritten);

    return lock_bits_remaining;
}

static 
NFCSTATUS
phFriNfc_MfUL_UpdateAndWriteLockBits (
    phFriNfc_sNdefSmtCrdFmt_t          *NdefSmtCrdFmt)
{
    NFCSTATUS                       result = NFCSTATUS_SUCCESS;
    phFriNfc_Type2_AddInfo_t        *ps_type2_info = 
                                    &(NdefSmtCrdFmt->AddInfo.Type2Info);
    uint8_t                         byte_index = 0;
    uint8_t                         no_of_bits_left_in_block = 0;
    uint8_t                         remaining_lock_bits = 0;
    uint8_t                         remaining_lock_bytes = 0;
    /* Array of 3 is used because the lock bits with 4 bytes in a block
        is handled in the function phFriNfc_MfUL_ReadWriteLockBytes 
        So use this function only if lock bytes doesnt use the entire block */
    uint8_t                         lock_bytes_value[MFUL_BLOCK_SIZE_IN_BYTES] = {0};
    uint8_t                         lock_byte_index = 0;

    (void)memcpy ((void *)lock_bytes_value, 
                (void*)(ps_type2_info->ReadData + ps_type2_info->ReadDataIndex), 
                sizeof (ps_type2_info->DynLockBytes));
    remaining_lock_bits = phFriNfc_MfUL_CalcRemainingLockBits (NdefSmtCrdFmt);
    
    if (ps_type2_info->CurrentBlock == ps_type2_info->LockBlockNumber)
    {
        /* 1st write to lock bits, so byte_index is updated */
        byte_index = ps_type2_info->LockByteNumber;
    }
    
    no_of_bits_left_in_block = (uint8_t)((MFUL_BLOCK_SIZE_IN_BYTES - byte_index) * 
                                MFUL_BYTE_SIZE_IN_BITS);

    if (no_of_bits_left_in_block >= remaining_lock_bits)
    {
        /* Entire lock bits can be written 
            if block size is more than number of lock bits. 
            so allocate the lock bits with value 1b and 
            dont change the remaining bits */
        if (remaining_lock_bits % MFUL_BYTE_SIZE_IN_BITS)
        {
            /* mod operation has resulted in a value, means lock bits ends in between a byte */
            uint8_t         mod_value = 0;

            remaining_lock_bytes = ((remaining_lock_bits /
                                    MFUL_BYTE_SIZE_IN_BITS) + 1);

            /* mod_value is used to fill the only partial bits and 
                remaining bits shall be untouched */
            mod_value = (uint8_t)(remaining_lock_bits % MFUL_BYTE_SIZE_IN_BITS);
            if (remaining_lock_bits > MFUL_BYTE_SIZE_IN_BITS)
            {
                /* lock bits to write is greater than 8 bits */
                while (lock_byte_index < (remaining_lock_bytes - 1))
                {
                    /* Set 1b to all bits left in the block */
                    lock_bytes_value[byte_index] = 0xFF;
                    lock_byte_index = (uint8_t)(lock_byte_index + 1);
                    byte_index = (uint8_t)(byte_index + 1);
                }
                /* Last byte of the lock bits shall be filled partially,
                    Set only the remaining lock bits and dont change 
                    the other bit value */
                lock_bytes_value[byte_index] = 0;
                lock_bytes_value[byte_index] = (uint8_t)
                        SET_BITS8 (lock_bytes_value[byte_index], 0, 
                                    mod_value, 1);
            }
            else
            {
                /* lock bits to write is less than 8 bits, so 
                    there is only one byte to write.
                    Set only the remaining lock bits and dont change 
                    the other bit value */
                lock_bytes_value[0] = (uint8_t)SET_BITS8 (lock_bytes_value[0], 0, 
                                                        mod_value, 1);
            }
        } /* if (remaining_lock_bits % MFUL_BYTE_SIZE_IN_BITS) */
        else
        {
            /* MOD operation is 00, that means entire byte value shall be 0xFF, means
            every bit shall be to 1 */
            remaining_lock_bytes = (remaining_lock_bits /
                                    MFUL_BYTE_SIZE_IN_BITS);

            while (lock_byte_index < remaining_lock_bytes)
            {
                /* Set 1b to all bits left in the block */
                lock_bytes_value[byte_index] = 0xFF;
                lock_byte_index = (uint8_t)(lock_byte_index + 1);
                byte_index = (uint8_t)(byte_index + 1);
            }
        } /* else of if (remaining_lock_bits % MFUL_BYTE_SIZE_IN_BITS) */
        ps_type2_info->LockBitsWritten = (uint8_t)(ps_type2_info->LockBitsWritten + 
                                            remaining_lock_bits);
    } /* if (no_of_bits_left_in_block >= remaining_lock_bits) */
    else
    {
        /* Update till the left bits in the block and then carry 
            out next operation after this write */
        while (lock_byte_index < (no_of_bits_left_in_block / MFUL_BYTE_SIZE_IN_BITS))
        {
            /* Set 1b to all bits left in the block */
            lock_bytes_value[byte_index] = 0xFF;
            lock_byte_index = (uint8_t)(lock_byte_index + 1);
            byte_index = (uint8_t)(byte_index + 1);
        }
        ps_type2_info->LockBitsWritten = (uint8_t)(ps_type2_info->LockBitsWritten + 
                                            no_of_bits_left_in_block);
    } /* else of if (no_of_bits_left_in_block >= remaining_lock_bits) */

    
    /* Copy the values back to the DynLockBytes structure member */ 
    (void)memcpy ((void*)ps_type2_info->DynLockBytes,
                (void *)lock_bytes_value,  
                sizeof (ps_type2_info->DynLockBytes));


    NdefSmtCrdFmt->State = PH_FRINFC_MFUL_FMT_RO_WR_DYN_LOCK_BYTES;
    result = phFriNfc_MfUL_H_WrRd (NdefSmtCrdFmt);

    return result;
}

static 
NFCSTATUS 
phFriNfc_MfUL_ReadWriteLockBytes (
    phFriNfc_sNdefSmtCrdFmt_t          *NdefSmtCrdFmt)
{
    NFCSTATUS                       result = NFCSTATUS_SUCCESS;
    phFriNfc_Type2_AddInfo_t        *ps_type2_info = 
                                    &(NdefSmtCrdFmt->AddInfo.Type2Info);
    uint8_t                         write_flag = FALSE;

    if (/* Lock bytes starts from the beginning of the block */
        (0 == ps_type2_info->LockByteNumber)
        /* To make sure this is the first read */
        && (ps_type2_info->CurrentBlock == ps_type2_info->LockBlockNumber)
        /* Lock bytes are greater than or equal to the block size, i.e., 4 bytes */
        && (phFriNfc_MfUL_CalcRemainingLockBits (NdefSmtCrdFmt)
        >= (MFUL_BLOCK_SIZE_IN_BYTES * MFUL_BYTE_SIZE_IN_BITS)))
    {        
        /* Then directly write the lock bytes, dont waste time for read  */
        (void)memset ((void *)ps_type2_info->DynLockBytes, 0xFF, 
                        sizeof (ps_type2_info->DynLockBytes));
        write_flag = TRUE;
    }
    else if (ps_type2_info->CurrentBlock == ps_type2_info->LockBlockNumber)
    {
        /* Read is mandatory, First read and then update the block, 
            because chances are there that lock byte may start in between 
            the block */
    }
    else if (/* check if remaining bytes exceeds or same as the block size */
        (phFriNfc_MfUL_CalcRemainingLockBits (NdefSmtCrdFmt)
        >= (MFUL_BLOCK_SIZE_IN_BYTES * MFUL_BYTE_SIZE_IN_BITS)))
    {
        /* Then directly write the lock bytes, dont waste time for read */
        (void)memset ((void *)ps_type2_info->DynLockBytes, 0xFF, 
                        sizeof (ps_type2_info->DynLockBytes));
        write_flag = TRUE;
    }
    else
    {
        /* Read is mandatory, First read and then update the block */
    }

    if (write_flag)
    {
        NdefSmtCrdFmt->State = PH_FRINFC_MFUL_FMT_RO_WR_DYN_LOCK_BYTES;
        result = phFriNfc_MfUL_H_WrRd (NdefSmtCrdFmt);
    }
    else
    {
        NdefSmtCrdFmt->State = PH_FRINFC_MFUL_FMT_RO_RD_DYN_LOCK_BYTES;
        result = phFriNfc_MfUL_H_WrRd (NdefSmtCrdFmt);
    }

    return result;
}

static 
NFCSTATUS 
phFriNfc_MfUL_GetDefaultLockBytesInfo (
    phFriNfc_sNdefSmtCrdFmt_t          *NdefSmtCrdFmt)
{
    NFCSTATUS                       result = NFCSTATUS_SUCCESS;
    phFriNfc_Type2_AddInfo_t        *ps_type2_info = 
                                    &(NdefSmtCrdFmt->AddInfo.Type2Info);
    uint16_t                        lock_byte_start_addr = 0;

    /*  The position of the dynamic lock bits starts from 
        the first byte after the data area */
    lock_byte_start_addr = (uint16_t)(MFUL_INITIAL_BYTES_IGNORED + 
                        (ps_type2_info->OTPBytes[TYPE_2_MEM_SIZE_POSITION] * 8));

    ps_type2_info->LockBlockNumber = (uint8_t)(lock_byte_start_addr / 
                                                MFUL_BLOCK_SIZE_IN_BYTES);
    ps_type2_info->LockByteNumber = (uint8_t)(lock_byte_start_addr % 
                                                MFUL_BLOCK_SIZE_IN_BYTES);
    /* Default settings 
       NoOfLockBits = [(DataAreaSize - 48)/8] */
    ps_type2_info->NoOfLockBits = (uint8_t)
        (((ps_type2_info->OTPBytes[TYPE_2_MEM_SIZE_POSITION] * 8) - 48)/8);

    return result;
}

static 
NFCSTATUS 
phFriNfc_MfUL_ParseTLVs (
    phFriNfc_sNdefSmtCrdFmt_t       *NdefSmtCrdFmt, 
    uint8_t                         *data_to_parse, 
    uint8_t                         size_to_parse)
{
    NFCSTATUS                       result = NFCSTATUS_SUCCESS;
    static uint8_t                  lock_mem_ndef_index = 0;
    static uint8_t                  skip_lock_mem_size = 0;
    static uint16_t                 card_size_remaining = 0;
    static uint16_t                 ndef_data_size = 0;
    static phFriNfc_MfUL_Parse_t    parse_tlv = LOCK_TLV_T;
    uint8_t                         parse_index = 0;
    
    if ((0 == card_size_remaining) && (0 == parse_index))
    {
        /* card size is calculated only once */
        card_size_remaining = (uint16_t)
            (NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes[TYPE_2_MEM_SIZE_POSITION] * 8);
    }

    while ((parse_index < size_to_parse)
        && (NFCSTATUS_SUCCESS == result)  
        && (NDEF_TLV_V != parse_tlv)
        && (0 != card_size_remaining))
    {
        if (0 == skip_lock_mem_size)
        {
            /* Skip the lock TLVs, so get the lock bits */
            skip_lock_mem_size = phFriNfc_MfUL_GetSkipSize (NdefSmtCrdFmt, 
                                        NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock, 
                                        parse_index);
        }

        if (0 != skip_lock_mem_size)
        {
            if (skip_lock_mem_size >= (size_to_parse - parse_index))
            {
                /* if skip size is more than the size to parse, then  */
                card_size_remaining = (uint16_t)(card_size_remaining - 
                                    (size_to_parse - parse_index));
                skip_lock_mem_size = (uint8_t)(skip_lock_mem_size - 
                                            ((size_to_parse - parse_index)));
                parse_index = size_to_parse;
            }
            else
            {
                card_size_remaining = (uint16_t)(card_size_remaining - 
                                        skip_lock_mem_size);

                parse_index = (uint8_t)(parse_index + skip_lock_mem_size);
                skip_lock_mem_size = 0;
            }
        }
        else
        {
            switch (parse_tlv)
            {
                case LOCK_TLV_T:
                {
                    switch (*(data_to_parse + parse_index))
                    {
                        case MFUL_NULL_TLV:
                        {
                            /* Do nothing, parse further */
                            break;
                        }

                        case LOCK_CTRL_TYPE_IN_TLV:
                        {
                            parse_tlv = LOCK_TLV_L;
                            break;
                        }

                        case NDEF_TYPE_IN_TLV:
                        {
                            parse_tlv = NDEF_TLV_L;
                            /* Default lock bytes shall be taken */
                            NdefSmtCrdFmt->AddInfo.Type2Info.DefaultLockBytesFlag = 
                                                                            TRUE;
                            result = phFriNfc_MfUL_GetDefaultLockBytesInfo (NdefSmtCrdFmt);
                            break;
                        }

                        default:
                        {
                            parse_tlv = LOCK_TLV_T;
                            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                                NFCSTATUS_NO_NDEF_SUPPORT);
                            break;
                        }
                    }
                    break;
                }

                case LOCK_TLV_L:
                {
                    if (LOCK_CTRL_LEN_IN_TLV == *(data_to_parse + parse_index))
                    {
                        parse_tlv = LOCK_TLV_V;
                    }
                    else
                    {
                        skip_lock_mem_size = 0;
                        parse_tlv = LOCK_TLV_T;
                        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                                NFCSTATUS_NO_NDEF_SUPPORT);
                    }
                    break;
                }

                case LOCK_TLV_V:
                {
                    switch (lock_mem_ndef_index)
                    {
                        case 0:
                        case 1:
                        {
                            NdefSmtCrdFmt->AddInfo.Type2Info.DefaultLockBytesFlag = 
                                                                                FALSE;
                            NdefSmtCrdFmt->AddInfo.Type2Info.DynLockBytes[lock_mem_ndef_index] = 
                                            *(data_to_parse + parse_index);
                            lock_mem_ndef_index = (uint8_t)(lock_mem_ndef_index + 1);
                            break;
                        }

                        case 2:
                        {
                            NdefSmtCrdFmt->AddInfo.Type2Info.DynLockBytes[lock_mem_ndef_index] = 
                                            *(data_to_parse + parse_index);
                            parse_tlv = NDEF_TLV_T;
                            lock_mem_ndef_index = 0;
                            result = phFriNfc_MfUL_GetLockBytesInfo (NdefSmtCrdFmt);
                            break;
                        }

                        default:
                        {
                            skip_lock_mem_size = 0;
                            parse_tlv = LOCK_TLV_T;
                            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                                NFCSTATUS_NO_NDEF_SUPPORT);
                            break;
                        }
                    }
                    break;
                } /* switch (lock_mem_ndef_index) in case LOCK_TLV_V */

                case NDEF_TLV_T:
                {
                    switch (*(data_to_parse + parse_index))
                    {
                        case MFUL_NULL_TLV:
                        {
                            /* Do nothing, parse further */
                            break;
                        }

                        case NDEF_TYPE_IN_TLV:
                        {
                            parse_tlv = NDEF_TLV_L;
                            break;
                        }

                        default:
                        {
                            skip_lock_mem_size = 0;
                            parse_tlv = LOCK_TLV_T;
                            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                                NFCSTATUS_NO_NDEF_SUPPORT);
                            break;
                        }
                    }
                    break;
                } /* switch (*(data_to_parse + parse_index)) in case NDEF_TLV_T */

                case NDEF_TLV_L:
                {
                    switch (lock_mem_ndef_index)
                    {
                        case 0:
                        {
                            if (THREE_BYTE_LENGTH_FIELD == *(data_to_parse + parse_index))
                            {
                                lock_mem_ndef_index = (uint8_t)(lock_mem_ndef_index + 1);
                            }
                            else
                            {
                                ndef_data_size = *(data_to_parse + parse_index);
                                parse_tlv = NDEF_TLV_V;
                                lock_mem_ndef_index = 0;
                            }
                            break;
                        }

                        case 1:
                        {
                            ndef_data_size = (uint16_t)(*(data_to_parse + parse_index) << 8);
                            break;
                        }

                        case 2:
                        {
                            ndef_data_size = (uint16_t)(ndef_data_size | 
                                                        *(data_to_parse + parse_index));
                            parse_tlv = NDEF_TLV_V;
                            lock_mem_ndef_index = 0;
                            break;
                        }
                    } /* switch (lock_mem_ndef_index) in case NDEF_TLV_L */
                    break;
                }

                case NDEF_TLV_V:
                {
                    break;
                }
                
                default:
                {
                    skip_lock_mem_size = 0;
                    parse_tlv = LOCK_TLV_T;
                    result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                        NFCSTATUS_NO_NDEF_SUPPORT);
                    break;
                }
            } /* switch (parse_tlv) */
            
        } /* else part of if (0 != skip_lock_mem_size) */

        if (0 == card_size_remaining)
        {
            skip_lock_mem_size = 0;
            parse_tlv = LOCK_TLV_T;
            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                NFCSTATUS_NO_NDEF_SUPPORT);
        }
        else if (NDEF_TLV_V != parse_tlv)
        {
            /* Increment the index */
            parse_index = (uint8_t)(parse_index + 1);
            /* card size is decremented as the memory area is parsed  */
            card_size_remaining = (uint16_t)(card_size_remaining - 1);
        }
        else
        {
            /* L field of the NDEF TLV
                L field can have 1 byte or also 3 bytes 
               */
            uint8_t length_to_deduct = 1;

            if ((NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes[TYPE_2_MEM_SIZE_POSITION] 
                * 8) >= THREE_BYTE_LENGTH_FIELD)
            {
                length_to_deduct = 3;
            }
            /* parse_tlv has reached the VALUE field of the NDEF TLV */
            if ((card_size_remaining - length_to_deduct) < ndef_data_size)
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                    NFCSTATUS_NO_NDEF_SUPPORT);
            }

            lock_mem_ndef_index = 0;
            skip_lock_mem_size = 0;
            card_size_remaining = 0;
        }
    } /* while ((parse_index < size_to_parse)
        && (NFCSTATUS_SUCCESS != result)  
        && (NDEF_TLV_V != parse_tlv)
        && (0 != card_size_remaining)) */
    
    if ((NDEF_TLV_V == parse_tlv) || (NFCSTATUS_SUCCESS != result))
    {
        parse_tlv = LOCK_TLV_T;
    }
    else
    {
        NdefSmtCrdFmt->State = PH_FRINFC_MFUL_FMT_RO_NDEF_PARSE_RD_BYTES;
        NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock = 
                        (NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock + 4);

        result = phFriNfc_MfUL_H_WrRd (NdefSmtCrdFmt);
    }

    if (NFCSTATUS_PENDING != result)
    {
        lock_mem_ndef_index = 0;
        skip_lock_mem_size = 0;
        card_size_remaining = 0;
    }
    return result;
}

#endif /* #ifdef PH_NDEF_MIFARE_ULC */

#endif /* #ifdef FRINFC_READONLY_NDEF */

static NFCSTATUS phFriNfc_MfUL_H_WrRd( phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt )
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;

    /* Fill the send buffer */
    phFriNfc_MfUL_H_fillSendBuf(NdefSmtCrdFmt, 
                            NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock);

    /* Call transceive */
    Result = phFriNfc_MfUL_H_Transceive (NdefSmtCrdFmt);
    
    return Result;
}

static NFCSTATUS phFriNfc_MfUL_H_Transceive(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;

    /* set the data for additional data exchange*/
    NdefSmtCrdFmt->psDepAdditionalInfo.DepFlags.MetaChaining = 0;
    NdefSmtCrdFmt->psDepAdditionalInfo.DepFlags.NADPresent = 0;
    NdefSmtCrdFmt->psDepAdditionalInfo.NAD = 0;

    /*set the completion routines for the card operations*/
    NdefSmtCrdFmt->SmtCrdFmtCompletionInfo.CompletionRoutine = phFriNfc_NdefSmtCrd_Process;
    NdefSmtCrdFmt->SmtCrdFmtCompletionInfo.Context = NdefSmtCrdFmt;

    *NdefSmtCrdFmt->SendRecvLength = PH_FRINFC_SMTCRDFMT_MAX_SEND_RECV_BUF_SIZE;

    /* Call the Overlapped HAL Transceive function */ 
    Result = phFriNfc_OvrHal_Transceive(    NdefSmtCrdFmt->LowerDevice,
                                            &NdefSmtCrdFmt->SmtCrdFmtCompletionInfo,
                                            NdefSmtCrdFmt->psRemoteDevInfo,
                                            NdefSmtCrdFmt->Cmd,
                                            &NdefSmtCrdFmt->psDepAdditionalInfo,
                                            NdefSmtCrdFmt->SendRecvBuf,
                                            NdefSmtCrdFmt->SendLength,
                                            NdefSmtCrdFmt->SendRecvBuf,
                                            NdefSmtCrdFmt->SendRecvLength);
    return Result;
}

static void phFriNfc_MfUL_H_fillSendBuf( phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt,
                                 uint8_t                    BlockNo)
{
#ifdef PH_NDEF_MIFARE_ULC
    uint8_t     NDEFTLV1[4] = {0x01, 0x03, 0xA0, 0x10}; 
    uint8_t     NDEFTLV2[4] = {0x44, 0x03, 0x00, 0xFE}; 
#endif /* #ifdef PH_NDEF_MIFARE_ULC */
    uint8_t     NDEFTLV[4] = {0x03, 0x00, 0xFE, 0x00};


    
    
    /* First byte for send buffer is always the block number */
    NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFUL_FMT_VAL_0] = (uint8_t)BlockNo;
    switch(NdefSmtCrdFmt->State)
    {
#ifdef FRINFC_READONLY_NDEF

        case PH_FRINFC_MFUL_FMT_RO_RD_16BYTES:
        {
#ifdef PH_HAL4_ENABLE
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareRead;
#else
        /* Read command */
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareCmdListMifareRead;
#endif /* #ifdef PH_HAL4_ENABLE */
            *NdefSmtCrdFmt->SendRecvBuf = RD_LOCK_OTP_BLOCK_NUMBER;
            /* Send length for read command is always one */
            NdefSmtCrdFmt->SendLength = PH_FRINFC_MFUL_FMT_VAL_1;
            break;
        }

#ifdef PH_NDEF_MIFARE_ULC

        case PH_FRINFC_MFUL_FMT_RO_NDEF_PARSE_RD_BYTES:
        {
#ifdef PH_HAL4_ENABLE
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareRead;
#else
        /* Read command */
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareCmdListMifareRead;
#endif /* #ifdef PH_HAL4_ENABLE */
            *NdefSmtCrdFmt->SendRecvBuf = 
                    NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock;
            /* Send length for read command is always one */
            NdefSmtCrdFmt->SendLength = PH_FRINFC_MFUL_FMT_VAL_1;
            break;
        }

        case PH_FRINFC_MFUL_FMT_RO_RD_DYN_LOCK_BYTES:
        {
#ifdef PH_HAL4_ENABLE
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareRead;
#else
        /* Read command */
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareCmdListMifareRead;
#endif /* #ifdef PH_HAL4_ENABLE */
            *NdefSmtCrdFmt->SendRecvBuf = NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock;
            /* Send length for read command is always one */
            NdefSmtCrdFmt->SendLength = PH_FRINFC_MFUL_FMT_VAL_1;
            break;
        }

        case PH_FRINFC_MFUL_FMT_RO_WR_DYN_LOCK_BYTES:
        {
#ifdef PH_HAL4_ENABLE
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareWrite4;
#else
            /* Write command */
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareCmdListMifareWrite4;
#endif /* #ifdef PH_HAL4_ENABLE */

            /* Send length for read command is always one */
            NdefSmtCrdFmt->SendLength = PH_FRINFC_MFUL_FMT_VAL_5;
            *NdefSmtCrdFmt->SendRecvBuf = NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock;
            (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFUL_FMT_VAL_1],
                         NdefSmtCrdFmt->AddInfo.Type2Info.DynLockBytes,
                         PH_FRINFC_MFUL_FMT_VAL_4);
            break;
        }

#endif /* #ifdef PH_NDEF_MIFARE_ULC */

        case PH_FRINFC_MFUL_FMT_RO_WR_LOCK_BYTES:
        {
#ifdef PH_HAL4_ENABLE
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareWrite4;
#else
            /* Read command */
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareCmdListMifareWrite4;
#endif /* #ifdef PH_HAL4_ENABLE */

            /* Send length for read command is always one */
            NdefSmtCrdFmt->SendLength = PH_FRINFC_MFUL_FMT_VAL_5;
            *NdefSmtCrdFmt->SendRecvBuf = RD_LOCK_OTP_BLOCK_NUMBER;
            (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFUL_FMT_VAL_1],
                         NdefSmtCrdFmt->AddInfo.Type2Info.LockBytes,
                         PH_FRINFC_MFUL_FMT_VAL_4);
            break;
        }

        case PH_FRINFC_MFUL_FMT_RO_WR_OTP_BYTES:
        {
#ifdef PH_HAL4_ENABLE
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareWrite4;
#else
            /* Read command */
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareCmdListMifareWrite4;
#endif /* #ifdef PH_HAL4_ENABLE */

            /* Send length for read command is always one */
            NdefSmtCrdFmt->SendLength = PH_FRINFC_MFUL_FMT_VAL_5;
            *NdefSmtCrdFmt->SendRecvBuf = OTP_BLOCK_NUMBER;
            (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFUL_FMT_VAL_1],
                         NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes,
                         PH_FRINFC_MFUL_FMT_VAL_4);
            break;
        }

#endif /* #ifdef FRINFC_READONLY_NDEF */

    case PH_FRINFC_MFUL_FMT_RD_16BYTES:
#ifdef PH_HAL4_ENABLE
        NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareRead;
#else
        /* Read command */
        NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareCmdListMifareRead;
#endif /* #ifdef PH_HAL4_ENABLE */
        /* Send length for read command is always one */
        NdefSmtCrdFmt->SendLength = PH_FRINFC_MFUL_FMT_VAL_1;
        break;

    case PH_FRINFC_MFUL_FMT_WR_OTPBYTES:
        /* Send length for read command is always Five */
        NdefSmtCrdFmt->SendLength = PH_FRINFC_MFUL_FMT_VAL_5;
        /* Write command */
#ifdef PH_HAL4_ENABLE
        NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareWrite4;
#else
        NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareCmdListMifareWrite4;
#endif /* #ifdef PH_HAL4_ENABLE */
        /* Copy the OTP bytes */
        (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFUL_FMT_VAL_1], 
                    NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes,
                    PH_FRINFC_MFUL_FMT_VAL_4);
        break;

    case PH_FRINFC_MFUL_FMT_WR_TLV:
#ifndef PH_NDEF_MIFARE_ULC      
    default:
#endif /* #ifndef PH_NDEF_MIFARE_ULC */    
        /* Send length for read command is always Five */
        NdefSmtCrdFmt->SendLength = PH_FRINFC_MFUL_FMT_VAL_5;
        /* Write command */
#ifdef PH_HAL4_ENABLE
        NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareWrite4;
#else
        NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareCmdListMifareWrite4;
#endif /* #ifdef PH_HAL4_ENABLE */        
        /* Copy the NDEF TLV */
#ifdef PH_NDEF_MIFARE_ULC

        if (NdefSmtCrdFmt->CardType == PH_FRINFC_NDEFMAP_MIFARE_ULC_CARD)
        {
            (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFUL_FMT_VAL_1], 
                    NDEFTLV1,
                    PH_FRINFC_MFUL_FMT_VAL_4);
        }
        else if (NdefSmtCrdFmt->CardType == PH_FRINFC_NDEFMAP_MIFARE_UL_CARD)
        {
            (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFUL_FMT_VAL_1], 
                NDEFTLV,
                PH_FRINFC_MFUL_FMT_VAL_4);
        }
        else
        {
        }
#else
        (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFUL_FMT_VAL_1], 
                    NDEFTLV,
                    PH_FRINFC_MFUL_FMT_VAL_4);

#endif /* #ifdef PH_NDEF_MIFARE_ULC */

        break;

#ifdef PH_NDEF_MIFARE_ULC
    case PH_FRINFC_MFUL_FMT_WR_TLV1:
        if (NdefSmtCrdFmt->CardType == PH_FRINFC_NDEFMAP_MIFARE_ULC_CARD)
        {
            /* Send length for write command is always Five */
            NdefSmtCrdFmt->SendLength = PH_FRINFC_MFUL_FMT_VAL_5;
            /* Write command */
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareWrite4;
            (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFUL_FMT_VAL_1], 
                        NDEFTLV2,
                        PH_FRINFC_MFUL_FMT_VAL_4);
        }
        break;
    default:
        break;
#endif /* #ifdef PH_NDEF_MIFARE_ULC */

    
    }
}

static NFCSTATUS phFriNfc_MfUL_H_ProRd16Bytes( phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt )
{
    NFCSTATUS   Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                    NFCSTATUS_FORMAT_ERROR);
    uint32_t    memcompare = PH_FRINFC_MFUL_FMT_VAL_0;
    uint8_t     ZeroBuf[] = {0x00, 0x00, 0x00, 0x00};

#ifdef PH_NDEF_MIFARE_ULC
    uint8_t                 OTPByteUL[] = PH_FRINFC_MFUL_FMT_OTP_BYTES;
    uint8_t                 OTPByteULC[] = PH_FRINFC_MFULC_FMT_OTP_BYTES;
#endif /* #ifdef PH_NDEF_MIFARE_ULC */

    /* Check the lock bits (byte number 2 and 3 of block number 2) */
    if ((NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFUL_FMT_VAL_2] == 
        PH_FRINFC_MFUL_FMT_LOCK_BITS_VAL) && 
        (NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFUL_FMT_VAL_3] == 
        PH_FRINFC_MFUL_FMT_LOCK_BITS_VAL))
    {

#ifdef PH_NDEF_MIFARE_ULC

        if (NdefSmtCrdFmt->SendRecvBuf[8] == 0x02 && 
            NdefSmtCrdFmt->SendRecvBuf[9] == 0x00) 
        {
            NdefSmtCrdFmt->CardType = PH_FRINFC_NDEFMAP_MIFARE_ULC_CARD;
            
            (void)memcpy(NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes,
                        OTPByteULC,
                        sizeof(NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes));
        }
        else if (NdefSmtCrdFmt->SendRecvBuf[8] == 0xFF && 
                NdefSmtCrdFmt->SendRecvBuf[9] == 0xFF)
        {
            NdefSmtCrdFmt->CardType = PH_FRINFC_NDEFMAP_MIFARE_UL_CARD;
            
            (void)memcpy(NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes,
                        OTPByteUL,
                        sizeof(NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes));
        }
        else
        {
            NdefSmtCrdFmt->CardType = PH_FRINFC_NDEFMAP_MIFARE_UL_CARD;
        }

#endif /* #ifdef PH_NDEF_MIFARE_ULC */

        memcompare = (uint32_t) 
                    MemCompare1(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFUL_FMT_VAL_4],
                            NdefSmtCrdFmt->AddInfo.Type2Info.OTPBytes,
                            PH_FRINFC_MFUL_FMT_VAL_4);

        if (memcompare == PH_FRINFC_MFUL_FMT_VAL_0)
        {
            /* Write NDEF TLV in block number 4 */
            NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock = 
                                                PH_FRINFC_MFUL_FMT_VAL_4;
            /* Card already have the OTP bytes so write TLV */
            NdefSmtCrdFmt->State = PH_FRINFC_MFUL_FMT_WR_TLV;
        }
        else
        {
            /* IS the card new, OTP bytes = {0x00, 0x00, 0x00, 0x00} */
            memcompare = (uint32_t)MemCompare1(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFUL_FMT_VAL_4],
                                ZeroBuf,
                                PH_FRINFC_MFUL_FMT_VAL_4);
            /* If OTP bytes are Zero then the card is Zero */
            if (memcompare == PH_FRINFC_MFUL_FMT_VAL_0)
            {
                /* Write OTP bytes in block number 3 */
                NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock = 
                                                PH_FRINFC_MFUL_FMT_VAL_3;
                /* Card already have the OTP bytes so write TLV */
                NdefSmtCrdFmt->State = PH_FRINFC_MFUL_FMT_WR_OTPBYTES;
            }
        }
    }

    

#ifdef PH_NDEF_MIFARE_ULC
    if(
        ((NdefSmtCrdFmt->State == PH_FRINFC_MFUL_FMT_WR_TLV) || 
        (NdefSmtCrdFmt->State == PH_FRINFC_MFUL_FMT_WR_OTPBYTES)) &&
        ((NdefSmtCrdFmt->CardType == PH_FRINFC_NDEFMAP_MIFARE_ULC_CARD) ||
        (NdefSmtCrdFmt->CardType == PH_FRINFC_NDEFMAP_MIFARE_UL_CARD))
        )
#else
    if((NdefSmtCrdFmt->State == PH_FRINFC_MFUL_FMT_WR_TLV) || 
        (NdefSmtCrdFmt->State == PH_FRINFC_MFUL_FMT_WR_OTPBYTES))
#endif /* #ifdef PH_NDEF_MIFARE_ULC */        
    {
        Result = phFriNfc_MfUL_H_WrRd(NdefSmtCrdFmt);
    }
    return Result;
}

static NFCSTATUS phFriNfc_MfUL_H_ProWrOTPBytes( phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt )
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    /* Card already have the OTP bytes so write TLV */
    NdefSmtCrdFmt->State = PH_FRINFC_MFUL_FMT_WR_TLV;

    /* Write NDEF TLV in block number 4 */
    NdefSmtCrdFmt->AddInfo.Type2Info.CurrentBlock = 
                                PH_FRINFC_MFUL_FMT_VAL_4;

    Result = phFriNfc_MfUL_H_WrRd(NdefSmtCrdFmt);
    return Result;
}

