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
* \file  phFriNfc_TopazDynamicMap.c
* \brief NFC Ndef Mapping For Remote Devices.
*
* Project: NFC-FRI
*
* $Date: Wed Oct 27 10:21:29 2010 $
* $Author: ing02260 $
* $Revision: 1.41 $
* $Aliases:  $
*
*/



#include <phFriNfc_NdefMap.h>
#include <phFriNfc_TopazMap.h>
#include <phFriNfc_MapTools.h>
#include <phFriNfc_OvrHal.h>

#if !(defined(PH_FRINFC_MAP_TOPAZ_DISABLED ) || defined (PH_FRINFC_MAP_TOPAZ_DYNAMIC_DISABLED ))

/*! \ingroup grp_file_attributes
*  \name NDEF Mapping
*
* File: \ref phFriNfcNdefMap.c
*
*/
/*@{*/
#define PHFRINFCTOPAZMAP_FILEREVISION "$Revision: 1.41 $"
#define PHFRINFCTOPAZMAP_FILEALIASES  "$Aliases:  $"
/*@}*/
/*!
* \name Topaz Mapping - Helper data structures and macros
*
*/
/*@{*/

/********************************** Start of data structures *********************************/
#ifdef FRINFC_READONLY_NDEF

    #define DYN_CC_BLOCK_NUMBER                                     (0x01U)
    #define DYN_STATIC_LOCK_BLOCK_NUM                               (0x0EU)

    #define DYN_STATIC_LOCK0_BYTE_NUM                               (0x00U)
    #define DYN_STATIC_LOCK0_BYTE_VALUE                             (0xFFU)

    #define DYN_STATIC_LOCK1_BYTE_NUM                               (0x01U)
    #define DYN_STATIC_LOCK1_BYTE_VALUE                             (0x7FU)

    #define DYN_CC_RWA_BYTE_NUMBER                                  (0x03U)
    #define DYN_CC_READ_ONLY_VALUE                                  (0x0FU)

#endif /* #ifdef FRINFC_READONLY_NDEF */

/*!
* \brief \copydoc page_ovr enum for the topaz sequence of execution. 
*/
typedef enum phFriNfc_Tpz_ParseSeq
{
    LOCK_T_TLV, 
    LOCK_L_TLV, 
    LOCK_V_TLV,
    MEM_T_TLV, 
    MEM_L_TLV,
    MEM_V_TLV,
    NDEF_T_TLV, 
    NDEF_L_TLV, 
    NDEF_V_TLV
}phFriNfc_Tpz_ParseSeq_t;

typedef enum phFriNfc_Tpz_WrSeq
{
    WR_NDEF_T_TLV, 
    WR_NMN_0, 
    WR_LEN_1_0, 
    WR_LEN_2_0,
    WR_LEN_3_0,
    WR_DATA,
    WR_DATA_READ_REQD, 
    WR_LEN_1_VALUE, 
    WR_LEN_2_VALUE,
    WR_LEN_3_VALUE,     
    WR_NMN_E1
}phFriNfc_Tpz_WrSeq_t;

#ifdef FRINFC_READONLY_NDEF

typedef enum phFriNfc_Tpz_RO_Seq
{
    WR_READONLY_CC, 
    RD_LOCK_BYTES, 
    WR_LOCK_BYTES, 
    RD_STATIC_LOCK_BYTE0,
    WR_STATIC_LOCK_BYTE0,
    WR_STATIC_LOCK_BYTE1
}phFriNfc_Tpz_RO_Seq_t;

#endif /* #ifdef FRINFC_READONLY_NDEF  */

/********************************** End of data structures *********************************/

/********************************** Start of Macros *********************************/
/* New state for TOPAZ dynamic  card*/
#define PH_FRINFC_TOPAZ_STATE_RD_FOR_WR_NDEF            (0x10U)

#ifdef FRINFC_READONLY_NDEF
    #define PH_FRINFC_TOPAZ_STATE_READ_ONLY             (0x11U)
#endif /* #ifdef FRINFC_READONLY_NDEF */

#define NIBBLE_SIZE                                     (0x04U)
/* Byte shifting for the topaz */
#define TOPAZ_BYTE_SHIFT                                (0x08U)
/* Lock and memory control TLV length. Always 3 bytes */
#define TOPAZ_MEM_LOCK_TLV_LENGTH                       (0x03U)
/* UID byte length */
#define TOPAZ_UID_BYTES_LENGTH                          (0x08U)

/* Number os static lock and reserved bytes */
#define TOPAZ_STATIC_LOCK_RES_BYTES                     (0x18U)
/* Number of static lock and reserved memory. This value is 3 (because 
    block number D, E and F are lock and reserved blocks */
#define TOPAZ_STATIC_LOCK_BLOCK_AREAS                   (0x03U)
/* First lock or reserved block in the static area of the card */
#define TOPAZ_STATIC_LOCK_FIRST_BLOCK_NO                (0x0DU)
/* First lock or reserved byte number in the static area of the card */
#define TOPAZ_STATIC_LOCK_RES_START                     (0x68U)
/* End lock or reserved byte number in the static area of the card */
#define TOPAZ_STATIC_LOCK_RES_END                       (0x78U)

/* CC byte length */
#define TOPAZ_CC_BYTES_LENGTH                           (0x04U)

/* In TOPAZ card each block has 8 bytes */
#define TOPAZ_BYTES_PER_BLOCK                           (0x08U)
/* Each byte has 8 bites */
#define TOPAZ_BYTE_SIZE_IN_BITS                         (0x08U)

/* This mask is to get the least significant NIBBLE from a BYTE */
#define TOPAZ_NIBBLE_MASK                               (0x0FU)
/* This is used to mask the least significant BYTE from a TWO BYTE value */
#define TOPAZ_BYTE_LENGTH_MASK                          (0x00FFU)

/* Total segments in TOPAZ 512 bytes card. Each segment = 128 bytes, 
so there are 4 segements in the card */
#define TOPAZ_TOTAL_SEG_TO_READ                         (0x04U)
/* SPEC version value shall be 0x10 as per the TYPE 1 specification */
#define TOPAZ_SPEC_VERSION                              (0x10U)

/* Response length for READ SEGMENT command is 128 bytes */
#define TOPAZ_SEGMENT_READ_LENGTH                       (0x80U)
/* Response length for WRITE-1E command is 1 byte */
#define TOPAZ_WRITE_1_RESPONSE                          (0x01U)
/* Response length for WRITE-8E command is 8 bytes */
#define TOPAZ_WRITE_8_RESPONSE                          (0x08U)
/* Response length for READ-8 command is 8 bytes */
#define TOPAZ_READ_8_RESPONSE                           (0x08U)

/* Data bytes that can be written for the WRITE-8E command is 8 bytes */
#define TOPAZ_WRITE_8_DATA_LENGTH                       (0x08U)

/* Get the exact byte address of the card from the segment number 
    and the parse index of each segment */
#define TOPAZ_BYTE_ADR_FROM_SEG(seg, parse_index) \
    (((seg) * TOPAZ_SEGMENT_READ_LENGTH) + (parse_index))

/* Get the segment number of the card from the byte address */
#define TOPAZ_SEG_FROM_BYTE_ADR(byte_addr) \
    ((byte_addr) / TOPAZ_SEGMENT_READ_LENGTH)
/* Get the block number of the card from the byte address */ 
#define TOPAZ_BLK_FROM_BYTE_ADR(byte_addr) \
    ((byte_addr) / TOPAZ_BYTES_PER_BLOCK)
/* Get the block offset of a block number of the card from the byte address */ 
#define TOPAZ_BYTE_OFFSET_FROM_BYTE_ADR(byte_addr) \
    ((byte_addr) % TOPAZ_BYTES_PER_BLOCK)
/* Get the exact byte address of the card from the block number 
    and the byte offset of the block number */
#define TOPAZ_BYTE_ADR_FROM_BLK(block_no, byte_offset) \
    (((block_no) * TOPAZ_BYTES_PER_BLOCK) + (byte_offset))
/* To increment the block number and if block number overlaps with the 
    static lock and reserved blocks, then skip the blocks */
#define TOPAZ_INCREMENT_SKIP_STATIC_BLOCK(block_no) \
    ((((block_no) + 1) == TOPAZ_STATIC_LOCK_FIRST_BLOCK_NO) ? \
    (((block_no) + 1) + TOPAZ_STATIC_LOCK_BLOCK_AREAS) : \
    ((block_no) + 1))
/* Check topaz spec version number */
#define TOPAZ_COMPARE_VERSION(device_ver, tag_ver) \
    ((device_ver & 0xF0) >= (tag_ver & 0xF0))

#ifdef FRINFC_READONLY_NDEF

#define TOPAZ_CONVERT_BITS_TO_BYTES(bits_to_bytes) \
            (((bits_to_bytes % TOPAZ_BYTE_SIZE_IN_BITS) > 0) ? \
            ((bits_to_bytes / TOPAZ_BYTE_SIZE_IN_BITS) + 1) : \
            (bits_to_bytes / TOPAZ_BYTE_SIZE_IN_BITS))

#endif /* #ifdef FRINFC_READONLY_NDEF */
/********************************** End of Macros *********************************/

/*@}*/


/*!
* \name Topaz Mapping - Helper Functions
*
*/
/*@{*/

/*!
* \brief \copydoc page_ovr Helper function for Topaz. This function shall read defined 
* bytes from the card.
*/
static 
NFCSTATUS 
phFriNfc_Tpz_H_NxpRead (
    phFriNfc_NdefMap_t          *psNdefMap);

/*!
* \brief \copydoc page_ovr Helper function for Topaz. This function shall process 
* received read id command.
*/
static 
NFCSTATUS 
phFriNfc_Tpz_H_ChkReadID (
    phFriNfc_NdefMap_t          *psNdefMap);


/*!
* \brief \copydoc page_ovr Helper function for Topaz. This function shall process 
* read response. 
*/
static 
NFCSTATUS 
phFriNfc_Tpz_H_ProReadResp (
    phFriNfc_NdefMap_t  *psNdefMap);

/*!
* \brief \copydoc page_ovr Helper function for Topaz. This function calls the 
* completion routine
*/
static 
void 
phFriNfc_Tpz_H_Complete (
    phFriNfc_NdefMap_t  *NdefMap,
    NFCSTATUS           Status);


/*!
* \brief \copydoc page_ovr Helper function for Topaz check ndef. This function checks 
* the lock bits and set a card state
*/
static 
NFCSTATUS 
phFriNfc_Tpz_H_ChkLockBits (
    phFriNfc_NdefMap_t  *psNdefMap);

/*!
* \brief \copydoc page_ovr Helper function for Topaz. This function writes defined 
* bytes into the card
*/
static 
NFCSTATUS 
phFriNfc_Tpz_H_NxpWrite (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint8_t                     *p_write_data, 
    uint8_t                     wr_data_len);


/*!
* \brief \copydoc page_ovr Helper function for Topaz. This function parses the read bytes
* till the NDEF TLV is found. Also, it returns error if it founds wrong TLVs.
*/
static 
NFCSTATUS 
phFriNfc_Tpz_H_ParseTLVs (
    phFriNfc_NdefMap_t          *psNdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function parses the read bytes
 * till the TYPE of the LOCK control TLV is found. 
 * Also, it returns error if it founds wrong TYPE.
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_ParseLockTLVType (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint8_t                     *p_parse_data, 
    uint16_t                    *p_parse_index, 
    uint16_t                    total_len_to_parse, 
    phFriNfc_Tpz_ParseSeq_t     *seq_to_execute);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function parses the read bytes
 * till the TYPE of the MEMORY control TLV is found. 
 * Also, it returns error if it founds wrong TYPE.
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_ParseMemTLVType (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint8_t                     *p_parse_data, 
    uint16_t                    *p_parse_index, 
    uint16_t                    total_len_to_parse, 
    phFriNfc_Tpz_ParseSeq_t     *seq_to_execute);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function parses the read bytes
 * till the TYPE of the NDEF control TLV is found. 
 * Also, it returns error if it founds wrong TYPE.
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_ParseNdefTLVType (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint8_t                     *p_parse_data, 
    uint16_t                    *p_parse_index, 
    uint16_t                    total_len_to_parse, 
    phFriNfc_Tpz_ParseSeq_t     *seq_to_execute);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function gets the lock bytes 
 * information.
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_GetLockBytesInfo (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint8_t                     *p_lock_info);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function gets the reserved bytes 
 * information.
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_GetMemBytesInfo (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint8_t                     *p_mem_info);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function copies and checks the CC bytes.  
 * This function checks for the lock bytes value and card state also.
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_CheckCCBytes (
    phFriNfc_NdefMap_t          *psNdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function checks the CC bytes.  
 * If .
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_CheckCCBytesForWrite (
    phFriNfc_NdefMap_t          *psNdefMap);


/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function copies the read bytes.  
 * This function also checks for the lock and reserved bytes and skips the bytes before copying it 
 * in the buffer.
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_CopyReadData (
    phFriNfc_NdefMap_t          *psNdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function copies the stored read bytes.  
 * This function is used only for the offset " PH_FRINFC_NDEFMAP_SEEK_CUR ".
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_RemainingReadDataCopy (
    phFriNfc_NdefMap_t          *psNdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function gives the exact byte address
 * of the value field after the NDEF TYPE field
 */
static 
uint16_t 
phFriNfc_Tpz_H_GetNDEFValueFieldAddrForRead (
    phFriNfc_NdefMap_t          *psNdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function gives the exact byte address
 * of the value field after the NDEF TYPE field
 */
static 
uint16_t 
phFriNfc_Tpz_H_GetNDEFValueFieldAddrForWrite (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint16_t                     size_to_write);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function gives the number of bytes to skip.  
 * This function checks the input byte address and checks if any lock or reserved bytes matches with the 
 * given address. if yes, then it will return number od bytes to skip.
 */
static 
uint16_t
phFriNfc_Tpz_H_GetSkipSize (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint16_t                    byte_adr_card);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function gives the actual data that can 
 * be read and written in the card.  
 * This function checks for the lock and reserved bytes and subtracts the remaining size to give the 
 * actual size.
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_ActualCardSize (
    phFriNfc_NdefMap_t          *psNdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function processes the response for
 * the write data
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_ProWrResp (
    phFriNfc_NdefMap_t          *psNdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function processes the read 8 commands,  
 * that is required for writing the data
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_ProRdForWrResp (
    phFriNfc_NdefMap_t          *psNdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function copies the user data to the 
 * write buffer and writes the data to the card. If the lock or memory blocks are in between the 
 * write data, then read the current block
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_CopySendWrData (
    phFriNfc_NdefMap_t          *psNdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function compares the input block 
 * number with lock bytes block number and returns the p_skip_size which is the lock bytes 
 * size
 */
static 
uint16_t 
phFriNfc_Tpz_H_CompareLockBlocks (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint8_t                     block_no, 
    uint16_t                    *p_skip_size);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function compares the input block 
 * number with reserved bytes block number and returns the p_skip_size which is the reserved bytes 
 * size
 */
static 
uint16_t 
phFriNfc_Tpz_H_CompareMemBlocks (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint8_t                     block_no, 
    uint16_t                    *p_skip_size);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function copies the read data and update 
 * the user bytes by skipping lock or memory control areas. Also, used while updating the value field
 * skips the initial bytes and to start at the proper value field byte offset of the block
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_CopyReadDataAndWrite (
    phFriNfc_NdefMap_t          *psNdefMap);


/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function reads the required block for writing, 
 * as some of the bytes shall not be overwritten
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_RdForWrite (
    phFriNfc_NdefMap_t          *psNdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function reads the length block for writing, 
 * updates the length bytes with 0
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_UpdateLenFieldZeroAfterRead (
    phFriNfc_NdefMap_t          *psNdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function reads the length block for writing, 
 * updates the length bytes with exact bytes that was written in the card
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_UpdateLenFieldValuesAfterRead (
    phFriNfc_NdefMap_t          *psNdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Topaz. This function writes the NDEF TYPE of the 
 * NDEF TLV to the specific byte address. This function is called only if the previous write is  
 * failed or there is no NDEF TLV with correct CC bytes
 */
static 
NFCSTATUS 
phFriNfc_Tpz_H_UpdateNdefTypeField (
    phFriNfc_NdefMap_t          *psNdefMap);

#ifdef FRINFC_READONLY_NDEF

static
NFCSTATUS
phFriNfc_Tpz_H_ProcessReadOnly (
    phFriNfc_NdefMap_t          *psNdefMap);

static 
NFCSTATUS
phFriNfc_Tpz_H_UpdateAndWriteLockBits (
    phFriNfc_NdefMap_t          *psNdefMap);


#endif /* #ifdef FRINFC_READONLY_NDEF */


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
NFCSTATUS  phFriNfc_TopazDynamicMap_ChkNdef( phFriNfc_NdefMap_t     *NdefMap)
{
    NFCSTATUS   Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
        NFCSTATUS_INVALID_PARAMETER);
    if ( NdefMap != NULL)
    {
        /* Update the previous operation */
        NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_CHECK_OPE;
        /* Update the CR index to know from which operation completion 
        routine has to be called */
        NdefMap->TopazContainer.CRIndex = PH_FRINFC_NDEFMAP_CR_CHK_NDEF;
        NdefMap->TopazContainer.Cur_RW_Index = PH_FRINFC_TOPAZ_VAL0;
        NdefMap->TopazContainer.CurrentSeg = 0;
        NdefMap->TopazContainer.NdefTLVByteAddress = 0;
        NdefMap->CardState = PH_NDEFMAP_CARD_STATE_INVALID;

        NdefMap->TopazContainer.CurrentBlock = 0;
        NdefMap->TopazContainer.WriteSeq = 0;
        NdefMap->TopazContainer.ExpectedSeq = 0;
        
        (void)memset ((void *)&(NdefMap->LockTlv), 0, 
                        sizeof (phFriNfc_LockCntrlTLVCont_t));

        (void)memset ((void *)&(NdefMap->MemTlv), 0, 
                    sizeof (phFriNfc_ResMemCntrlTLVCont_t));
        
        /* Set card state */
        NdefMap->CardType = PH_FRINFC_NDEFMAP_TOPAZ_DYNAMIC_CARD;

        /* Change the state to Read */
        NdefMap->State = PH_FRINFC_TOPAZ_STATE_READ;
        
        NdefMap->TopazContainer.InternalState = PH_FRINFC_TOPAZ_DYNAMIC_INIT_CHK_NDEF;
#ifdef TOPAZ_RAW_SUPPORT

        *NdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_RSEG;

#else 

#ifdef PH_HAL4_ENABLE
        NdefMap->Cmd.JewelCmd = phHal_eJewel_ReadSeg;   
#else
        NdefMap->Cmd.JewelCmd = phHal_eJewelCmdListJewelRead;
#endif

#endif /* #ifdef TOPAZ_RAW_SUPPORT */

        Result = phFriNfc_Tpz_H_NxpRead(NdefMap);

      }
    return Result;    
}


/*!
* \brief Initiates Reading of NDEF information from the Remote Device.
*
* The function initiates the reading of NDEF information from a Remote Device.
* It performs a reset of the state and starts the action (state machine).
* A periodic call of the \ref phFriNfcNdefMap_Process has to be done once the action
* has been triggered.
*/

NFCSTATUS phFriNfc_TopazDynamicMap_RdNdef( phFriNfc_NdefMap_t           *NdefMap,
                                        uint8_t                         *PacketData,
                                        uint32_t                        *PacketDataLength,
                                        uint8_t                         Offset)
{
    NFCSTATUS               Result =    NFCSTATUS_SUCCESS;

    /* Copy user buffer to the context */
    NdefMap->ApduBuffer = PacketData;
    /* Copy user length to the context */
    NdefMap->ApduBufferSize = *PacketDataLength;
    /* Update the user memory size to a context variable */
    NdefMap->NumOfBytesRead = PacketDataLength;
    /* Number of bytes read from the card is zero. 
    This variable returns the number of bytes read 
    from the card. */
    *NdefMap->NumOfBytesRead = 0;
    /* Index to know the length read */
    NdefMap->ApduBuffIndex = PH_FRINFC_TOPAZ_VAL0;    
    /* Store the offset in the context */
    NdefMap->Offset = Offset;
    /* Update the CR index to know from which operation completion 
    routine has to be called */ 
    NdefMap->TopazContainer.CRIndex = PH_FRINFC_NDEFMAP_CR_RD_NDEF;
    NdefMap->TopazContainer.SkipLockBlkFlag = 0;

    NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_READ_OPE;
    if ((PH_FRINFC_NDEFMAP_SEEK_CUR == Offset) &&
        (TRUE == NdefMap->TopazContainer.ReadWriteCompleteFlag))
    {
        Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_EOF_NDEF_CONTAINER_REACHED); 
    }
    else if ((PH_NDEFMAP_CARD_STATE_INITIALIZED == 
            NdefMap->CardState) || 
            (0 == NdefMap->TopazContainer.ActualNDEFMsgSize))
    {
        /* Length field of NDEF TLV is 0, so read cannot proceed */
        Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_READ_FAILED);
    }
    else if ((PH_FRINFC_NDEFMAP_SEEK_BEGIN == Offset) || 
        (PH_FRINFC_NDEFMAP_READ_OPE != NdefMap->PrevOperation))
    {
        /* If previous operation is not read then the read shall 
        start from BEGIN */
        NdefMap->Offset = PH_FRINFC_NDEFMAP_SEEK_BEGIN;
        /* Initialise byte number */
        NdefMap->TopazContainer.Cur_RW_Index = PH_FRINFC_TOPAZ_VAL0;

        NdefMap->TopazContainer.RemainingReadSize = 0;
        NdefMap->TopazContainer.ReadBufferSize = 0;
        NdefMap->TopazContainer.ReadWriteCompleteFlag = FALSE;
        NdefMap->TopazContainer.CurrentBlock = 0;
        NdefMap->TopazContainer.WriteSeq = 0;

        NdefMap->TopazContainer.CurrentSeg = (uint8_t)TOPAZ_SEG_FROM_BYTE_ADR (
                        phFriNfc_Tpz_H_GetNDEFValueFieldAddrForRead (NdefMap));
                
         /* Change the state to Read ID */
        NdefMap->State = PH_FRINFC_TOPAZ_STATE_READID;
        /*Change the state to Read ID*/
        NdefMap->TopazContainer.ReadWriteCompleteFlag = 0;
#ifdef TOPAZ_RAW_SUPPORT

        NdefMap->SendRecvBuf[0] = PH_FRINFC_TOPAZ_CMD_READID;

#else 

#ifdef PH_HAL4_ENABLE
        NdefMap->Cmd.JewelCmd = phHal_eJewel_RID;   
#else
        NdefMap->Cmd.JewelCmd = phHal_eJewelCmdListJewelRid;
#endif

#endif /* #ifdef TOPAZ_RAW_SUPPORT */
        Result =  phFriNfc_Tpz_H_NxpRead(NdefMap);

    }
    else
    {
         /* Change the state to Read */
          NdefMap->State = PH_FRINFC_TOPAZ_STATE_READ;
          Result = phFriNfc_Tpz_H_RemainingReadDataCopy (NdefMap);
    }

    
    return Result;
}

#ifdef FRINFC_READONLY_NDEF

NFCSTATUS
phFriNfc_TopazDynamicMap_ConvertToReadOnly (
    phFriNfc_NdefMap_t     *psNdefMap)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    uint8_t                     cc_read_only_byte = 0x0FU;

    psNdefMap->State = PH_FRINFC_TOPAZ_STATE_READ_ONLY;
    
    psNdefMap->TopazContainer.read_only_seq = 0;



    psNdefMap->TopazContainer.CurrentBlock = 0x01U;
    psNdefMap->TopazContainer.ByteNumber = 0x03U;
                    
#ifdef TOPAZ_RAW_SUPPORT
    *psNdefMap->SendRecvBuf = (uint8_t)PH_FRINFC_TOPAZ_CMD_WRITE_1E;
#else 
    psNdefMap->Cmd.JewelCmd = phHal_eJewel_Write1E;
#endif /* #ifdef TOPAZ_RAW_SUPPORT */

    result = phFriNfc_Tpz_H_NxpWrite (psNdefMap, &cc_read_only_byte, 
                                    1);
        
    if (NFCSTATUS_PENDING == result)
    {
        psNdefMap->TopazContainer.read_only_seq = (uint8_t)WR_READONLY_CC;
    }


    return result;
}

#endif /* #ifdef FRINFC_READONLY_NDEF */

/*!
* \brief Initiates Writing of NDEF information to the Remote Device.
*
* The function initiates the writing of NDEF information to a Remote Device.
* It performs a reset of the state and starts the action (state machine).
* A periodic call of the \ref phFriNfcNdefMap_Process has to be done once the action
* has been triggered.
*/
NFCSTATUS phFriNfc_TopazDynamicMap_WrNdef( phFriNfc_NdefMap_t     *NdefMap,
                                   uint8_t                 *PacketData,
                                   uint32_t                *PacketDataLength,
                                   uint8_t                 Offset)
{
    NFCSTATUS                   Result = NFCSTATUS_SUCCESS;

    /* Copy user buffer to the context */
    NdefMap->ApduBuffer = PacketData;
    /* Copy user length to the context */
    NdefMap->ApduBufferSize = *PacketDataLength;
    /* Index to know the length written */
    NdefMap->ApduBuffIndex = 0;
    /* Update the user memory size to a context variable */
    NdefMap->WrNdefPacketLength = PacketDataLength;
    /* Number of bytes written to the card is zero. 
    This variable returns the number of bytes written 
    to the card. */
    *NdefMap->WrNdefPacketLength = 0;
    /* Update the CR index to know from which operation completion 
    routine has to be called */
    NdefMap->TopazContainer.CRIndex = PH_FRINFC_NDEFMAP_CR_WR_NDEF;
    /* Store the offset in the context */
    NdefMap->Offset = Offset;

    /* Update the previous operation to write operation */
    NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_WRITE_OPE;

    if (PH_NDEFMAP_CARD_STATE_READ_ONLY == NdefMap->CardState)
    {
        Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_WRITE_FAILED);
    }
    else if ((PH_FRINFC_NDEFMAP_SEEK_CUR == Offset) &&
        (TRUE == NdefMap->TopazContainer.ReadWriteCompleteFlag))
    {
        /* Offset = Current, but the read has reached the End of Card */
        Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_EOF_NDEF_CONTAINER_REACHED); 
    }
    else if (0 == NdefMap->TopazContainer.NdefTLVByteAddress)
    {
        /* No NDEF TLV found in the card, so write not possible */
        Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_NO_NDEF_SUPPORT);
    }
    else if ((PH_FRINFC_NDEFMAP_SEEK_BEGIN == Offset) || 
        (PH_FRINFC_NDEFMAP_WRITE_OPE != NdefMap->PrevOperation))
    {
        NdefMap->Offset = PH_FRINFC_NDEFMAP_SEEK_BEGIN;
        /* Initialise byte number */
        NdefMap->TopazContainer.Cur_RW_Index = PH_FRINFC_TOPAZ_VAL0;
        /* State has to be changed */
        NdefMap->State = PH_FRINFC_TOPAZ_STATE_READ;
        NdefMap->TopazContainer.ReadWriteCompleteFlag = FALSE;

        NdefMap->TopazContainer.CurrentSeg = 0;
        NdefMap->TopazContainer.CurrentBlock = 1;
        NdefMap->TopazContainer.WriteSeq = 0;

#ifdef TOPAZ_RAW_SUPPORT

        *NdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_READ8;

#else 

        /* Topaz command = Jewel Nxp Read */
#ifdef PH_HAL4_ENABLE
        NdefMap->Cmd.JewelCmd = phHal_eJewel_Read;
#else
        NdefMap->Cmd.JewelCmd = phHal_eJewelCmdListJewelRead;
#endif  
        
        NdefMap->Cmd.JewelCmd = phHal_eJewel_Read8;

#endif /* #ifdef TOPAZ_RAW_SUPPORT */
        /* Call read segment */
        Result = phFriNfc_Tpz_H_NxpRead (NdefMap);
    }
    else
    {
#if 0
        /* This part is to handle the Current offset, 
        Current offset is not yet validated */
        Result = phFriNfc_Tpz_H_NxpWrite(NdefMap);
#endif /* #if 0 */
    } 
    
    return Result;
}


/*!
* \brief Completion Routine, Processing function, needed to avoid long blocking.
* \note The lower (Overlapped HAL) layer must register a pointer to this function as a Completion
*       Routine in order to be able to notify the component that an I/O has finished and data are
*       ready to be processed.
*
*/

void phFriNfc_TopazDynamicMap_Process( void       *Context,
                               NFCSTATUS   Status)
{

    phFriNfc_NdefMap_t      *NdefMap; 

    NdefMap = (phFriNfc_NdefMap_t *)Context;


    if((NFCSTATUS_SUCCESS & PHNFCSTBLOWER) == (Status & PHNFCSTBLOWER))
    {
        switch(NdefMap->State)
        {
            case PH_FRINFC_TOPAZ_STATE_READ:
            {
                Status = phFriNfc_Tpz_H_ProReadResp (NdefMap);
                break;
            }

            case PH_FRINFC_TOPAZ_STATE_WRITE:
            {
                Status =  phFriNfc_Tpz_H_ProWrResp (NdefMap);
                break;
            }

            case PH_FRINFC_TOPAZ_STATE_RD_FOR_WR_NDEF:
            {
                Status =  phFriNfc_Tpz_H_ProRdForWrResp (NdefMap);
                break;
            }           

            case PH_FRINFC_TOPAZ_STATE_READID:
            {
                Status = phFriNfc_Tpz_H_ChkReadID(NdefMap);
                break;
            }

#ifdef FRINFC_READONLY_NDEF
            case PH_FRINFC_TOPAZ_STATE_READ_ONLY:
            {
                Status = phFriNfc_Tpz_H_ProcessReadOnly (NdefMap);
                break;
            }
#endif /* #ifdef FRINFC_READONLY_NDEF */
            
            default:
            {
                Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_DEVICE_REQUEST);
                break;
            }
        }
    }
    
    /* Call for the Completion Routine*/
    if(Status != NFCSTATUS_PENDING)
    {
        phFriNfc_Tpz_H_Complete(NdefMap, Status);
    }
}

#ifdef FRINFC_READONLY_NDEF

static 
NFCSTATUS
phFriNfc_Tpz_H_UpdateAndWriteLockBits (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    NFCSTATUS                       result = NFCSTATUS_SUCCESS;
    phFriNfc_TopazCont_t            *ps_tpz_info = NULL;
    phFriNfc_LockCntrlTLVCont_t     *ps_locktlv_info = NULL;
    uint8_t                         remaining_lock_bits = 0;
    uint8_t                         byte_index = 0;
    uint8_t                         lock_bytes_value[TOPAZ_BYTES_PER_BLOCK] = {0};
    uint8_t                         lock_byte_index = 0;
    uint8_t                         no_of_bits_left_in_block = 0;

    ps_tpz_info = &(psNdefMap->TopazContainer);
    ps_locktlv_info = &(psNdefMap->LockTlv);

    (void)memcpy ((void *)lock_bytes_value, (void *)psNdefMap->SendRecvBuf, 
                    TOPAZ_BYTES_PER_BLOCK);

    if (ps_tpz_info->CurrentBlock == ps_locktlv_info->BlkNum)
    {
        /* Get the lock bits that has to locked */
        remaining_lock_bits = ps_locktlv_info->LockTlvBuff[1];
        byte_index = (uint8_t)ps_locktlv_info->ByteNum;
    }
    else
    {
        /* This condition applies only for the lock bits not ending with 
        " ps_locktlv_info->BlkNum ".
        Calculate the remaining lock bits */
        remaining_lock_bits = (uint8_t)(ps_locktlv_info->LockTlvBuff[1] - 
                    ps_tpz_info->lock_bytes_written);
    }

    no_of_bits_left_in_block = (uint8_t)((TOPAZ_BYTES_PER_BLOCK - byte_index) * 
                                TOPAZ_BYTE_SIZE_IN_BITS);

    if (no_of_bits_left_in_block >= remaining_lock_bits)
    {
        /* Entire lock bits can be written */
        uint8_t                 mod_value = 0;

        mod_value = (uint8_t)(remaining_lock_bits % TOPAZ_BYTES_PER_BLOCK);

        if (mod_value)
        {
            /* The lock bits ends in between of a byte */
            /* lock bits to write is greater than 8 bits */
            if (mod_value > TOPAZ_BYTE_SIZE_IN_BITS)
            {
                while (lock_byte_index < 
                    (TOPAZ_CONVERT_BITS_TO_BYTES(remaining_lock_bits) - 1))
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
                lock_bytes_value[0] = (uint8_t)
                        SET_BITS8 (lock_bytes_value[0], 0, 
                                    mod_value, 1);
            }            
        } /* if (mod_value) */
        else
        {
            /* The lock bits exactly ends at a byte 
            MOD operation is 00, that means entire byte value shall be 0xFF, means
            every bit shall be to 1 */

            while (lock_byte_index < TOPAZ_CONVERT_BITS_TO_BYTES(remaining_lock_bits))
            {
                /* Set 1b to all bits left in the block */
                lock_bytes_value[byte_index] = 0xFF;
                lock_byte_index = (uint8_t)(lock_byte_index + 1);
                byte_index = (uint8_t)(byte_index + 1);
            }
        } /* else of if (mod_value) */
        ps_tpz_info->lock_bytes_written = remaining_lock_bits;
    }
    else /* if (no_of_bits_left_in_block >= remaining_lock_bits) */
    {
        /* Partial lock bits can be written. use next read to write 
            the remaining lock bits  */
        while (lock_byte_index <  (no_of_bits_left_in_block / 
                            TOPAZ_BYTES_PER_BLOCK))
        {
            /* Set 1b to all bits left in the block */
            lock_bytes_value[byte_index] = 0xFF;
            lock_byte_index = (uint8_t)(lock_byte_index + 1);
            byte_index = (uint8_t)(byte_index + 1);
        }
        ps_tpz_info->lock_bytes_written = (uint8_t)(no_of_bits_left_in_block / 
                            TOPAZ_BYTES_PER_BLOCK);
    } /* else of if (no_of_bits_left_in_block >= remaining_lock_bits) */

#ifdef TOPAZ_RAW_SUPPORT
    *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_WRITE_E8;
#else
    psNdefMap->Cmd.JewelCmd = phHal_eJewel_Write8E;
#endif /* #ifdef TOPAZ_RAW_SUPPORT */

    result = phFriNfc_Tpz_H_NxpWrite (psNdefMap, lock_bytes_value, 
                                    sizeof (lock_bytes_value));
    return result;
}

static
NFCSTATUS
phFriNfc_Tpz_H_ProcessReadOnly (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    NFCSTATUS                           result = NFCSTATUS_SUCCESS;
    phFriNfc_Tpz_RO_Seq_t               e_readonly_seq = RD_LOCK_BYTES;
    phFriNfc_TopazCont_t                *ps_tpz_info = NULL;
    phFriNfc_LockCntrlTLVCont_t         *ps_locktlv_info = NULL;
    static uint8_t                      static_lock_bytes[2] = {0};

    ps_tpz_info = &(psNdefMap->TopazContainer);
    ps_locktlv_info = &(psNdefMap->LockTlv);
    e_readonly_seq = (phFriNfc_Tpz_RO_Seq_t)psNdefMap->TopazContainer.read_only_seq;

    switch (e_readonly_seq)
    {
        case WR_READONLY_CC:
        {
            if (TOPAZ_WRITE_1_RESPONSE == *psNdefMap->SendRecvLength)
            {
                psNdefMap->TopazContainer.CurrentBlock = (uint8_t)
                                psNdefMap->LockTlv.BlkNum;

                e_readonly_seq = RD_LOCK_BYTES;
#ifdef TOPAZ_RAW_SUPPORT

                *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_READ8;

#else 

        /* Topaz command = Jewel Nxp Read */
#ifdef PH_HAL4_ENABLE
                psNdefMap->Cmd.JewelCmd = phHal_eJewel_Read;
#else
                psNdefMap->Cmd.JewelCmd = phHal_eJewelCmdListJewelRead;
#endif  
        
                psNdefMap->Cmd.JewelCmd = phHal_eJewel_Read8;

#endif /* #ifdef TOPAZ_RAW_SUPPORT */
                /* Call read segment */
                result = phFriNfc_Tpz_H_NxpRead (psNdefMap);
            }
            else
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            break;
        }

        case RD_LOCK_BYTES:
        {
            if (TOPAZ_READ_8_RESPONSE == *psNdefMap->SendRecvLength)
            { 
                result = phFriNfc_Tpz_H_UpdateAndWriteLockBits (psNdefMap);

                if (NFCSTATUS_PENDING == result)
                {
                    e_readonly_seq = WR_LOCK_BYTES;
                }
            }
            else
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            break;
        }

        case WR_LOCK_BYTES:
        {
            if (TOPAZ_WRITE_8_RESPONSE == *psNdefMap->SendRecvLength)
            {
                ps_tpz_info->CurrentBlock = (uint8_t)
                                        (ps_tpz_info->CurrentBlock + 1); 
                if (ps_locktlv_info->LockTlvBuff[1] - 
                    ps_tpz_info->lock_bytes_written)
                {                    
#ifdef TOPAZ_RAW_SUPPORT

                    *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_READ8;

#else 

                    /* Topaz command = Jewel Nxp Read */
#ifdef PH_HAL4_ENABLE
                    psNdefMap->Cmd.JewelCmd = phHal_eJewel_Read;
#else
                    psNdefMap->Cmd.JewelCmd = phHal_eJewelCmdListJewelRead;
#endif  
        
                    psNdefMap->Cmd.JewelCmd = phHal_eJewel_Read8;

#endif /* #ifdef TOPAZ_RAW_SUPPORT */
                    /* Call read segment */
                    result = phFriNfc_Tpz_H_NxpRead (psNdefMap);
                    e_readonly_seq = RD_LOCK_BYTES;
                }
                else
                {
                    ps_tpz_info->CurrentBlock = (uint8_t)
                                        DYN_STATIC_LOCK_BLOCK_NUM;
                    ps_tpz_info->ByteNumber = (uint8_t)
                                        DYN_STATIC_LOCK0_BYTE_NUM;
#ifdef TOPAZ_RAW_SUPPORT

                    *psNdefMap->SendRecvBuf = (uint8_t)PH_FRINFC_TOPAZ_CMD_READ8;

#else 

                    /* Topaz command = Jewel Nxp Read */
#ifdef PH_HAL4_ENABLE
                    psNdefMap->Cmd.JewelCmd = phHal_eJewel_Read;
#else
                    psNdefMap->Cmd.JewelCmd = phHal_eJewelCmdListJewelRead;
#endif  
        
                    psNdefMap->Cmd.JewelCmd = phHal_eJewel_Read8;

#endif /* #ifdef TOPAZ_RAW_SUPPORT */
                    /* Call read segment */
                    result = phFriNfc_Tpz_H_NxpRead (psNdefMap);
                    e_readonly_seq = RD_STATIC_LOCK_BYTE0;

                }                
            }
            else
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            break;
        }
        
        case RD_STATIC_LOCK_BYTE0:
        {
            if (TOPAZ_READ_8_RESPONSE == *psNdefMap->SendRecvLength)
            {
                uint8_t                 lock_byte_value = 0;

                (void)memcpy ((void *)static_lock_bytes, 
                            (void *)(psNdefMap->SendRecvBuf + 
                                ps_tpz_info->ByteNumber), 
                            sizeof (static_lock_bytes));


                lock_byte_value = (uint8_t)(static_lock_bytes[0] | 
                                    DYN_STATIC_LOCK0_BYTE_VALUE);
                    
#ifdef TOPAZ_RAW_SUPPORT
                    *psNdefMap->SendRecvBuf = (uint8_t)PH_FRINFC_TOPAZ_CMD_WRITE_1E;
#else
                    psNdefMap->Cmd.JewelCmd = phHal_eJewel_Write1E;
#endif /* #ifdef TOPAZ_RAW_SUPPORT */

                result = phFriNfc_Tpz_H_NxpWrite (psNdefMap, &lock_byte_value, 
                                                    1);

                    if (NFCSTATUS_PENDING == result)
                    {
                    e_readonly_seq = (uint8_t)WR_STATIC_LOCK_BYTE0;
                    }
                }                
            else
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            break;
        }        

        case WR_STATIC_LOCK_BYTE0:
        {
            if (TOPAZ_WRITE_1_RESPONSE == *psNdefMap->SendRecvLength)
            {
                uint8_t                 lock_byte_value = 
                                        (static_lock_bytes[1] | 
                                        DYN_STATIC_LOCK1_BYTE_VALUE);

                ps_tpz_info->CurrentBlock = (uint8_t)
                                    DYN_STATIC_LOCK_BLOCK_NUM;
                ps_tpz_info->ByteNumber = (uint8_t)
                                    DYN_STATIC_LOCK1_BYTE_NUM;
#ifdef TOPAZ_RAW_SUPPORT
                *psNdefMap->SendRecvBuf = (uint8_t)PH_FRINFC_TOPAZ_CMD_WRITE_1E;
#else
                psNdefMap->Cmd.JewelCmd = phHal_eJewel_Write1E;
#endif /* #ifdef TOPAZ_RAW_SUPPORT */

                result = phFriNfc_Tpz_H_NxpWrite (psNdefMap, &lock_byte_value, 
                                                    1);

                if (NFCSTATUS_PENDING == result)
                {
                    e_readonly_seq = (uint8_t)WR_STATIC_LOCK_BYTE1;
                }
            }
            else
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            break;
        }

        case WR_STATIC_LOCK_BYTE1:
        {
            if (TOPAZ_WRITE_1_RESPONSE == *psNdefMap->SendRecvLength)
            {
                /* READ ONLY successful */
            }
            else
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            break;
        }

        default:
        {
            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            break;
        }
    }

    psNdefMap->TopazContainer.read_only_seq = (uint8_t)e_readonly_seq;
    return result;
}

#endif /* #ifdef FRINFC_READONLY_NDEF */

static 
NFCSTATUS 
phFriNfc_Tpz_H_ProWrResp (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    NFCSTATUS                           result = NFCSTATUS_SUCCESS;
    phFriNfc_TopazCont_t                *ps_tpz_info = NULL;
    phFriNfc_Tpz_WrSeq_t                write_seq;
    uint8_t                             write_buf[] = {0x00};
    uint8_t                             write_index = 0;
    uint16_t                            write_len = 0;
    uint16_t                            len_byte_addr = 0;

    ps_tpz_info = &(psNdefMap->TopazContainer);
    write_seq = (phFriNfc_Tpz_WrSeq_t)(ps_tpz_info->WriteSeq);
    write_len = (uint16_t)((psNdefMap->ApduBufferSize < ps_tpz_info->NDEFRWSize) ? 
                psNdefMap->ApduBufferSize : ps_tpz_info->NDEFRWSize);

    switch (write_seq)
    {
        case WR_NDEF_T_TLV:
        {
            /* TYPE field of the NDEF TLV write is complete */
            if (TOPAZ_WRITE_8_RESPONSE == *psNdefMap->SendRecvLength)
            {
                psNdefMap->State = (uint8_t)
                                    PH_FRINFC_TOPAZ_STATE_WRITE;

                /* Now, Write 0 to the magic number byte */
                ps_tpz_info->WriteSeq = (uint8_t)WR_NMN_0;
                write_seq = WR_NMN_0;
                ps_tpz_info->CurrentBlock = 1;
                ps_tpz_info->ByteNumber = 0;
                        
#ifdef TOPAZ_RAW_SUPPORT
                *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_WRITE_1E;
#else
                psNdefMap->Cmd.JewelCmd = phHal_eJewel_Write1E;
#endif /* #ifdef TOPAZ_RAW_SUPPORT */
                result = phFriNfc_Tpz_H_NxpWrite (psNdefMap, write_buf, 
                                                sizeof (write_buf));
            }
            else
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            break;
        }

        case WR_NMN_0:
        {
            /* Magic number set to 0 write is complete */
            if (TOPAZ_WRITE_1_RESPONSE == *psNdefMap->SendRecvLength)
            {
                ps_tpz_info->WriteSeq = (uint8_t)(write_seq + 1);
                write_seq = (phFriNfc_Tpz_WrSeq_t)(write_seq + 1);
                /* Now the sequence = WR_LEN_1_0, so Length block is read, 
                    and only length bytes are made 0, before writing data to 0 
                */
                result = phFriNfc_Tpz_H_RdForWrite (psNdefMap);
            }
            else
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            break;
        }

        case WR_LEN_1_0:
        {
            /* Length field is updated with the value 0 */
            if (TOPAZ_WRITE_8_RESPONSE != *psNdefMap->SendRecvLength)
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            else if (write_len >= 0xFF)
            {
                ps_tpz_info->ByteNumber = 0;
                
                ps_tpz_info->CurrentBlock = (uint8_t)
                                    TOPAZ_INCREMENT_SKIP_STATIC_BLOCK (
                                    ps_tpz_info->CurrentBlock);                

                ps_tpz_info->WriteSeq = (uint8_t)(write_seq + 1);
                write_seq = (phFriNfc_Tpz_WrSeq_t)(write_seq + 1);
                /* Now the sequence = WR_LEN_1_1, so Length block is read, 
                    and only length bytes are made 0, before writing data to 0 
                */
                result = phFriNfc_Tpz_H_RdForWrite (psNdefMap);
            }
            else
            {
                /* NDEF data length < 0xFF */
                len_byte_addr = phFriNfc_Tpz_H_GetNDEFValueFieldAddrForWrite 
                                                (psNdefMap, write_len);
                ps_tpz_info->CurrentBlock = (uint8_t)
                        TOPAZ_BLK_FROM_BYTE_ADR (len_byte_addr);
                ps_tpz_info->ByteNumber = (uint8_t)
                        TOPAZ_BYTE_OFFSET_FROM_BYTE_ADR (len_byte_addr);

                
                ps_tpz_info->WriteSeq = (uint8_t)WR_DATA;
                write_seq = WR_DATA;

                if (0 != ps_tpz_info->ByteNumber)
                {
                    /* If data starts in between the block then read 
                        the data */
                    result = phFriNfc_Tpz_H_RdForWrite (psNdefMap);
                }
                else
                {
                    /* Data starts at the beginning of the block, so start 
                        writing the user data */
                    result = phFriNfc_Tpz_H_CopySendWrData (psNdefMap);
                }
            }
            break;
        }

        case WR_LEN_2_0:
        case WR_LEN_2_VALUE:
        {
            /* 2nd length field is updated with the value 0 or the correct 
                written value */
            if (TOPAZ_WRITE_8_RESPONSE != *psNdefMap->SendRecvLength)
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            else
            {
                ps_tpz_info->ByteNumber = 0;
                ps_tpz_info->CurrentBlock = (uint8_t)
                                    TOPAZ_INCREMENT_SKIP_STATIC_BLOCK (
                                    ps_tpz_info->CurrentBlock);
                ps_tpz_info->WriteSeq = (uint8_t)(write_seq + 1);
                write_seq = (phFriNfc_Tpz_WrSeq_t)(write_seq + 1);
                /* If length byte starts in between the block then read 
                    the length block */
                result = phFriNfc_Tpz_H_RdForWrite (psNdefMap);
            }
            break;
        }

        case WR_LEN_3_0:
        {
            /* 3rd length field is updated with the value 0 */
            if (TOPAZ_WRITE_8_RESPONSE != *psNdefMap->SendRecvLength)
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            else
            {
                len_byte_addr = phFriNfc_Tpz_H_GetNDEFValueFieldAddrForWrite 
                                                (psNdefMap, write_len);
                ps_tpz_info->CurrentBlock = (uint8_t)
                        TOPAZ_BLK_FROM_BYTE_ADR (len_byte_addr);
                ps_tpz_info->ByteNumber = (uint8_t)
                        TOPAZ_BYTE_OFFSET_FROM_BYTE_ADR (len_byte_addr);

                ps_tpz_info->WriteSeq = (uint8_t)(write_seq + 1);
                write_seq = (phFriNfc_Tpz_WrSeq_t)(write_seq + 1);

                if (0 != ps_tpz_info->ByteNumber)
                {
                    /* If data starts in between the block then read 
                        the data */
                    result = phFriNfc_Tpz_H_RdForWrite (psNdefMap);
                }
                else
                {
                    /* Data starts at the beginning of the block, so start 
                        writing the user data */
                    result = phFriNfc_Tpz_H_CopySendWrData (psNdefMap);
                }
            }
            break;
        }

        case WR_DATA:
        {
            /* Data is written from the input buffer */
            if (TOPAZ_WRITE_8_RESPONSE != *psNdefMap->SendRecvLength)
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            else if (write_len == psNdefMap->ApduBuffIndex)
            {
                /* Data to be written is completely written to the card */
                *psNdefMap->WrNdefPacketLength = psNdefMap->ApduBuffIndex;
                ps_tpz_info->WriteSeq = (uint8_t)WR_LEN_1_VALUE;
                write_seq = WR_LEN_1_VALUE;
                /* To write the first length byte, it has to be read and then 
                    the length has to be updated */
                result = phFriNfc_Tpz_H_RdForWrite (psNdefMap);
            }
            else
            {
                ps_tpz_info->ByteNumber = 0;
                /* Go to the next block */
                ps_tpz_info->CurrentBlock = (uint8_t)
                                    TOPAZ_INCREMENT_SKIP_STATIC_BLOCK (
                                    ps_tpz_info->CurrentBlock);
                /* Copy and write the user data */
                result = phFriNfc_Tpz_H_CopySendWrData (psNdefMap);
            }
            break;
        }

        case WR_DATA_READ_REQD: 
        {
            /* This sequence is executed, if the first read has some 
                lock or reserved blocks bytes and the lock or reserved 
                blocks are extended to the next block  */
            if (TOPAZ_WRITE_8_RESPONSE != *psNdefMap->SendRecvLength)
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            else
            {
                ps_tpz_info->ByteNumber = 0;
                /* Go to the next block */
                ps_tpz_info->CurrentBlock = (uint8_t)
                                    TOPAZ_INCREMENT_SKIP_STATIC_BLOCK (
                                    ps_tpz_info->CurrentBlock);
                /* Write is complete for one block, now because lock bytes are 
                    shifted to next blocks, the next block is read and update 
                    the written data by skipping the lock or reserved memory bytes */
                result = phFriNfc_Tpz_H_RdForWrite (psNdefMap);
            }
            break;
        }

        case WR_LEN_3_VALUE:
        {
            /* 3rd LENGTH field byte is updated with correct written value */
            if (TOPAZ_WRITE_8_RESPONSE != *psNdefMap->SendRecvLength)
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            else
            {
#ifdef TOPAZ_RAW_SUPPORT
                *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_WRITE_1E;
#else
                psNdefMap->Cmd.JewelCmd = phHal_eJewel_Write1E;
#endif /* #ifdef TOPAZ_RAW_SUPPORT */

                psNdefMap->State = (uint8_t)PH_FRINFC_TOPAZ_STATE_WRITE;

                write_buf[write_index] = PH_FRINFC_TOPAZ_CC_BYTE0;
                write_index = (uint8_t)(write_index + 1);

                ps_tpz_info->ByteNumber = 0;
                ps_tpz_info->CurrentBlock = 1;

                ps_tpz_info->WriteSeq = (uint8_t)WR_NMN_E1;
                write_seq = WR_NMN_E1;

                /* Length byte write is complete, so now update the magic 
                    number byte with value 0xE1 */
                result = phFriNfc_Tpz_H_NxpWrite(psNdefMap, write_buf, 
                                                write_index);
            }
            break;
        }

        case WR_LEN_1_VALUE:
        {
            /* 1st LENGTH field byte is updated */
            if (TOPAZ_WRITE_8_RESPONSE != *psNdefMap->SendRecvLength)
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            else if (write_len < 0xFF)
            {
                /* Total length to write is less than 0xFF, so LENGTH field has 
                    only one byte, then update the magic number byte with 
                    value 0xE1 */
#ifdef TOPAZ_RAW_SUPPORT
                *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_WRITE_1E;
#else
                psNdefMap->Cmd.JewelCmd = phHal_eJewel_Write1E;
#endif /* #ifdef TOPAZ_RAW_SUPPORT */
                psNdefMap->State = (uint8_t)PH_FRINFC_TOPAZ_STATE_WRITE;

                write_buf[write_index] = PH_FRINFC_TOPAZ_CC_BYTE0;
                write_index = (uint8_t)(write_index + 1);

                ps_tpz_info->ByteNumber = 0;
                ps_tpz_info->CurrentBlock = 1;

                ps_tpz_info->WriteSeq = (uint8_t)WR_NMN_E1;
                write_seq = WR_NMN_E1;
                result = phFriNfc_Tpz_H_NxpWrite(psNdefMap, write_buf, 
                                                write_index);
            }
            else
            {
                /* 2nd byte of the LENGTH field has to be updated so, 
                    read the block, before updating it */
                ps_tpz_info->ByteNumber = 0;
                ps_tpz_info->CurrentBlock = (uint8_t)
                                    TOPAZ_INCREMENT_SKIP_STATIC_BLOCK (
                                    ps_tpz_info->CurrentBlock);
                ps_tpz_info->WriteSeq = (uint8_t)WR_LEN_2_VALUE;
                write_seq = WR_LEN_2_VALUE;
                result = phFriNfc_Tpz_H_RdForWrite (psNdefMap);
            }
            break;
        }

        case WR_NMN_E1:
        {
            /* Magic number is written, so update the actual ndef length.  */
            if (TOPAZ_WRITE_1_RESPONSE == *psNdefMap->SendRecvLength)
            {
                *psNdefMap->WrNdefPacketLength = (uint32_t)
                                                psNdefMap->ApduBuffIndex;
                ps_tpz_info->ActualNDEFMsgSize = (uint16_t)
                                                psNdefMap->ApduBuffIndex;
            }
            else
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            break;
        }

        default:
        {
            break;
        }
    }

    return result;
}

static 
NFCSTATUS 
phFriNfc_Tpz_H_UpdateNdefTypeField (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    NFCSTATUS                           result = NFCSTATUS_SUCCESS;
    phFriNfc_TopazCont_t                *ps_tpz_info = NULL;
    uint8_t                             write_buf[TOPAZ_WRITE_8_DATA_LENGTH];

    ps_tpz_info = &(psNdefMap->TopazContainer);

    (void)memcpy ((void *)write_buf, (void *)
                psNdefMap->SendRecvBuf, TOPAZ_WRITE_8_DATA_LENGTH);

    /* Update the TYPE field of the NDEF TLV */
    write_buf[ps_tpz_info->ByteNumber] = PH_FRINFC_TOPAZ_NDEF_T;

    psNdefMap->State = (uint8_t)PH_FRINFC_TOPAZ_STATE_WRITE;

#ifdef TOPAZ_RAW_SUPPORT
    *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_WRITE_E8;
#else
    psNdefMap->Cmd.JewelCmd = phHal_eJewel_Write8E;
#endif /* #ifdef TOPAZ_RAW_SUPPORT */
    result = phFriNfc_Tpz_H_NxpWrite(psNdefMap, write_buf, 
                                    sizeof (write_buf));

    return result;
}

static 
NFCSTATUS 
phFriNfc_Tpz_H_ProRdForWrResp (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    /* This function is used during the write operation */
    NFCSTATUS                           result = NFCSTATUS_SUCCESS;
    phFriNfc_TopazCont_t                *ps_tpz_info = NULL;    

    ps_tpz_info = &(psNdefMap->TopazContainer);    

    psNdefMap->State = PH_FRINFC_TOPAZ_STATE_WRITE;

    if (TOPAZ_READ_8_RESPONSE == *psNdefMap->SendRecvLength)
    {        
        switch ((phFriNfc_Tpz_WrSeq_t)ps_tpz_info->WriteSeq)
        {
            case WR_NDEF_T_TLV:
            {
                /* Read bytes are for updating the TYPE field of the NDEF TLV */
                result = phFriNfc_Tpz_H_UpdateNdefTypeField (psNdefMap);
                break;
            }

            case WR_LEN_1_0:
            case WR_LEN_2_0:
            case WR_LEN_3_0:
            {
                /* Read bytes are for updating the LENGTH field to 0 of the NDEF TLV and 
                also to update the data from the user buffer */
                result = phFriNfc_Tpz_H_UpdateLenFieldZeroAfterRead (psNdefMap);
                break;
            }

            case WR_DATA:
            case WR_DATA_READ_REQD:
            {
                /* Read bytes are for skipping the lock and reserved bytes */
                result = phFriNfc_Tpz_H_CopyReadDataAndWrite (psNdefMap);
                break;
            }

            case WR_LEN_1_VALUE:
            case WR_LEN_2_VALUE:
            case WR_LEN_3_VALUE:
            {
                /* Read bytes are for updating the LENGTH field to the correct values 
                    of the NDEF TLV */
                result = phFriNfc_Tpz_H_UpdateLenFieldValuesAfterRead (psNdefMap);
                break;
            }

            default:
            {
                /* Code must not come come here */
                break;
            }
        }
    }
    else
    {
        /* Error in the length, wither the HW has sent wrong response length or 
            the response length byte is corrupted */
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                            NFCSTATUS_INVALID_RECEIVE_LENGTH);
    }

    
    return result;
}

static 
NFCSTATUS 
phFriNfc_Tpz_H_ChkReadID(
    phFriNfc_NdefMap_t      *psNdefMap)
{
    NFCSTATUS   result = NFCSTATUS_SUCCESS;
    int         compare_result = 0;
    uint8_t     recv_index = 0;
    
    
    if (PH_FRINFC_TOPAZ_VAL6 == *psNdefMap->SendRecvLength)
    {
        if (((psNdefMap->SendRecvBuf[recv_index] & 
            PH_FRINFC_TOPAZ_HEADROM0_CHK) == PH_FRINFC_TOPAZ_DYNAMIC_HEADROM0_VAL))
        {
            /* Copy UID to the context*/
            compare_result = phOsalNfc_MemCompare (
                                psNdefMap->psRemoteDevInfo->RemoteDevInfo.Jewel_Info.Uid, 
                                &psNdefMap->SendRecvBuf[PH_FRINFC_TOPAZ_VAL2],
                                TOPAZ_UID_LENGTH_FOR_READ_WRITE);
            if (0 == compare_result)
            {
                /* State has to be changed */
                psNdefMap->State = PH_FRINFC_TOPAZ_STATE_READ;

                /* Topaz command = READSEG */
#ifdef TOPAZ_RAW_SUPPORT

                *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_RSEG;

#else

#ifdef PH_HAL4_ENABLE
                psNdefMap->Cmd.JewelCmd = phHal_eJewel_Read;
#else
                psNdefMap->Cmd.JewelCmd = phHal_eJewelCmdListJewelRead;
#endif
                psNdefMap->Cmd.JewelCmd = phHal_eJewel_ReadSeg;

#endif /* #ifdef TOPAZ_RAW_SUPPORT */
                /* Read bytes from the card */
                result = phFriNfc_Tpz_H_NxpRead (psNdefMap);
            }
            else
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_NO_NDEF_SUPPORT);

            }
        }
    }
    else
    {
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                            NFCSTATUS_INVALID_RECEIVE_LENGTH);
    }

    return result;
}

#define TOPAZ_READ_ID_ZERO_LENGTH                   (0x06U)
static 
NFCSTATUS 
phFriNfc_Tpz_H_NxpRead (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    NFCSTATUS           result = NFCSTATUS_SUCCESS;
    uint8_t             send_index = 0;
#ifdef TOPAZ_RAW_SUPPORT
    uint8_t             read_append[] = { 0x00, 0x00, 0x00, 0x00, 
                                        0x00, 0x00, 0x00, 0x00};
#endif /* #ifdef TOPAZ_RAW_SUPPORT */

    /* set the data for additional data exchange*/
    psNdefMap->psDepAdditionalInfo.DepFlags.MetaChaining = PH_FRINFC_TOPAZ_VAL0;
    psNdefMap->psDepAdditionalInfo.DepFlags.NADPresent = PH_FRINFC_TOPAZ_VAL0;
    psNdefMap->psDepAdditionalInfo.NAD = PH_FRINFC_TOPAZ_VAL0;

    psNdefMap->MapCompletionInfo.CompletionRoutine = phFriNfc_TopazDynamicMap_Process;
    psNdefMap->MapCompletionInfo.Context = psNdefMap;

    *psNdefMap->SendRecvLength = psNdefMap->TempReceiveLength;

    /* Depending on the jewel command, the send length is decided */
#ifdef TOPAZ_RAW_SUPPORT

    psNdefMap->Cmd.JewelCmd = phHal_eJewel_Raw;
    /* " send_index " is incremented because already received buffer is filled with 
        TOPAZ command */
    send_index = (uint8_t)(send_index + 1);

    switch (*psNdefMap->SendRecvBuf)
#else
    switch(psNdefMap->Cmd.JewelCmd)
#endif /* #ifdef TOPAZ_RAW_SUPPORT */    
    {
#ifdef TOPAZ_RAW_SUPPORT

        case PH_FRINFC_TOPAZ_CMD_READID:
        {
            (void)memcpy ((void *)(psNdefMap->SendRecvBuf + send_index), 
                        (void *)read_append, TOPAZ_READ_ID_ZERO_LENGTH);
            send_index = (uint8_t)(send_index + TOPAZ_READ_ID_ZERO_LENGTH);
            break;
        }

        case PH_FRINFC_TOPAZ_CMD_READ8:
        {
            psNdefMap->SendRecvBuf[send_index] = 
                                    psNdefMap->TopazContainer.CurrentBlock;
            send_index = (uint8_t)(send_index + 1);
            break;
        }

        case PH_FRINFC_TOPAZ_CMD_RSEG:
        {
            psNdefMap->SendRecvBuf[send_index] = (uint8_t)
                                            (psNdefMap->TopazContainer.CurrentSeg
                                             << NIBBLE_SIZE);
            send_index = (uint8_t)(send_index + 1);
            break;
        }

#else /* #ifdef TOPAZ_RAW_SUPPORT */

#ifdef PH_HAL4_ENABLE
        case phHal_eJewel_RID:
        case phHal_eJewel_ReadAll:  
#else
        case phHal_eJewelCmdListJewelRid:
        case phHal_eJewelCmdListJewelReadAll:
#endif
        {
            /* For READ ID and READ ALL, send length is 0 */
            psNdefMap->SendLength = PH_FRINFC_TOPAZ_VAL0;
            break;
        }

#ifdef PH_HAL4_ENABLE
        case phHal_eJewel_Read:
#else
        case phHal_eJewelCmdListJewelRead:
#endif
        {
            /* Need to check the User data size request*/

            psNdefMap->SendLength = PH_FRINFC_TOPAZ_VAL3;
            break;
        }

        case phHal_eJewel_ReadSeg:
        {
            psNdefMap->SendRecvBuf[send_index] = (uint8_t)
                                            (psNdefMap->TopazContainer.CurrentSeg
                                             << NIBBLE_SIZE);
            send_index = (uint8_t)(send_index + 1);
            psNdefMap->SendLength = send_index;
            break;
        }

        case phHal_eJewel_Read8:
        {
            psNdefMap->Cmd.JewelCmd = phHal_eJewel_Read4;
            psNdefMap->SendRecvBuf[send_index] = psNdefMap->TopazContainer.CurrentBlock;
            send_index = (uint8_t)(send_index + 1);
            psNdefMap->SendLength = send_index;
            break;
        }

#endif /* #ifdef TOPAZ_RAW_SUPPORT */

        default:
        {
            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                NFCSTATUS_INVALID_DEVICE_REQUEST);
            break;
        }
    }
    if(result == NFCSTATUS_SUCCESS)
    {
#ifdef TOPAZ_RAW_SUPPORT

        if (PH_FRINFC_TOPAZ_CMD_READID != *psNdefMap->SendRecvBuf)
        {
            (void)memcpy ((void *)(psNdefMap->SendRecvBuf + send_index), 
                            (void *)read_append, sizeof (read_append));
            send_index = (uint8_t)(send_index + sizeof (read_append));

            (void)memcpy ((void *)(psNdefMap->SendRecvBuf + send_index), 
                        (void *)psNdefMap->psRemoteDevInfo->RemoteDevInfo.Jewel_Info.Uid, 
                        TOPAZ_UID_LENGTH_FOR_READ_WRITE);
            send_index = (uint8_t)(send_index + 
                        TOPAZ_UID_LENGTH_FOR_READ_WRITE);            
        }

        psNdefMap->SendLength = send_index;

#endif /* #ifdef TOPAZ_RAW_SUPPORT */
        /* Call the Overlapped HAL Transceive function */ 
        result = phFriNfc_OvrHal_Transceive(    psNdefMap->LowerDevice,
                                                &psNdefMap->MapCompletionInfo,
                                                psNdefMap->psRemoteDevInfo,
                                                psNdefMap->Cmd,
                                                &psNdefMap->psDepAdditionalInfo,
                                                psNdefMap->SendRecvBuf,
                                                psNdefMap->SendLength,
                                                psNdefMap->SendRecvBuf,
                                                psNdefMap->SendRecvLength);
    }
    return result;
}


static 
NFCSTATUS 
phFriNfc_Tpz_H_NxpWrite(
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint8_t                     *p_write_data, 
    uint8_t                     wr_data_len)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    phFriNfc_TopazCont_t        *ps_tpz_info = NULL;
    uint8_t                     send_index = 0;

    ps_tpz_info = &(psNdefMap->TopazContainer);        

    /* set the data for additional data exchange*/
    psNdefMap->psDepAdditionalInfo.DepFlags.MetaChaining = PH_FRINFC_TOPAZ_VAL0;
    psNdefMap->psDepAdditionalInfo.DepFlags.NADPresent = PH_FRINFC_TOPAZ_VAL0;
    psNdefMap->psDepAdditionalInfo.NAD = PH_FRINFC_TOPAZ_VAL0;

    psNdefMap->MapCompletionInfo.CompletionRoutine = phFriNfc_TopazDynamicMap_Process;
    psNdefMap->MapCompletionInfo.Context = psNdefMap;

    *psNdefMap->SendRecvLength = psNdefMap->TempReceiveLength;

#ifdef TOPAZ_RAW_SUPPORT
    /* " send_index " is incremented because already received buffer is filled with 
        TOPAZ command */
    send_index = (uint8_t)(send_index + 1);
    psNdefMap->Cmd.JewelCmd = phHal_eJewel_Raw;

    switch (*psNdefMap->SendRecvBuf)

#else /* #ifdef TOPAZ_RAW_SUPPORT */

    switch (psNdefMap->Cmd.JewelCmd)

#endif /* #ifdef TOPAZ_RAW_SUPPORT */
    {
#ifdef TOPAZ_RAW_SUPPORT

        case PH_FRINFC_TOPAZ_CMD_WRITE_1E:
        {
            psNdefMap->SendRecvBuf[send_index] = (uint8_t)((ps_tpz_info->CurrentBlock 
                                                << (NIBBLE_SIZE - 1)) | 
                                                ps_tpz_info->ByteNumber);
            send_index = (uint8_t)(send_index + 1);
            break;
        }

        case PH_FRINFC_TOPAZ_CMD_WRITE_E8:
        {
            psNdefMap->SendRecvBuf[send_index] = ps_tpz_info->CurrentBlock;
            send_index = (uint8_t)(send_index + 1);
            break;
        }

#else /* #ifdef TOPAZ_RAW_SUPPORT */

        case phHal_eJewel_Write1E:
        {
            psNdefMap->SendRecvBuf[send_index] = (uint8_t)((ps_tpz_info->CurrentBlock 
                                                << (NIBBLE_SIZE - 1)) | 
                                                ps_tpz_info->ByteNumber);
            send_index = (uint8_t)(send_index + 1);

            
            break;
        }

        case phHal_eJewel_Write8E:
        {
            psNdefMap->Cmd.JewelCmd = phHal_eJewel_Write4E;
            psNdefMap->SendRecvBuf[send_index] = ps_tpz_info->CurrentBlock;
            send_index = (uint8_t)(send_index + 1);
            break;
        }

#endif /* #ifdef TOPAZ_RAW_SUPPORT */

        default:
        {
            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                NFCSTATUS_INVALID_DEVICE_REQUEST); 
            break;
        }
    }
  

    if (NFCSTATUS_SUCCESS == result)
    {
        (void)memcpy ((void *)(psNdefMap->SendRecvBuf + send_index), 
                    (void *)p_write_data, wr_data_len);

        send_index = (uint8_t)(send_index + wr_data_len);

#ifdef TOPAZ_RAW_SUPPORT

        (void)memcpy ((void *)(psNdefMap->SendRecvBuf + send_index), 
                    (void *)psNdefMap->psRemoteDevInfo->RemoteDevInfo.Jewel_Info.Uid, 
                    TOPAZ_UID_LENGTH_FOR_READ_WRITE);
        send_index = (uint8_t)(send_index + TOPAZ_UID_LENGTH_FOR_READ_WRITE);

#endif /* #ifdef TOPAZ_RAW_SUPPORT */

        psNdefMap->SendLength = send_index;

        /* Call the Overlapped HAL Transceive function */ 
        result = phFriNfc_OvrHal_Transceive(    psNdefMap->LowerDevice,
                                                &psNdefMap->MapCompletionInfo,
                                                psNdefMap->psRemoteDevInfo,
                                                psNdefMap->Cmd,
                                                &psNdefMap->psDepAdditionalInfo,
                                                psNdefMap->SendRecvBuf,
                                                psNdefMap->SendLength,
                                                psNdefMap->SendRecvBuf,
                                                psNdefMap->SendRecvLength);
    }
    return result;
}

static 
NFCSTATUS 
phFriNfc_Tpz_H_ProReadResp(
    phFriNfc_NdefMap_t          *psNdefMap)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    phFriNfc_TopazCont_t        *ps_tpz_info = NULL;
    uint8_t                     write_buffer[] = {0x00};

    ps_tpz_info = &(psNdefMap->TopazContainer);

    switch (psNdefMap->PrevOperation)
    {
        case  PH_FRINFC_NDEFMAP_CHECK_OPE:
        {
            if (PH_FRINFC_TOPAZ_DYNAMIC_READSEG_RESP == 
                *psNdefMap->SendRecvLength)
            {
                if (0 == ps_tpz_info->CurrentSeg)
                {
                    result = phFriNfc_Tpz_H_CheckCCBytes (psNdefMap);
                }

                if (NFCSTATUS_SUCCESS == result)
                {
                    result = phFriNfc_Tpz_H_ParseTLVs (psNdefMap);
                }
            }
            else
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH); 
            }
            break;
        }

        case  PH_FRINFC_NDEFMAP_READ_OPE:
        {
            if (PH_FRINFC_TOPAZ_DYNAMIC_READSEG_RESP == 
                *psNdefMap->SendRecvLength)
            {
                /* call the data bytes to internal buffer*/
                result = phFriNfc_Tpz_H_CopyReadData (psNdefMap);
            }
            else
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH); 
            }
            break;
        }

        case  PH_FRINFC_NDEFMAP_WRITE_OPE:
        {
            /* read the bytes for cheking the CC bytes and lock bit status*/
            if(TOPAZ_READ_8_RESPONSE == *psNdefMap->SendRecvLength)
            {
                (void)memcpy ((void *)ps_tpz_info->CCByteBuf, 
                            (void *)(psNdefMap->SendRecvBuf), 
                            TOPAZ_CC_BYTES_LENGTH);

                result = phFriNfc_Tpz_H_CheckCCBytesForWrite (psNdefMap);
                if (NFCSTATUS_SUCCESS == result)
                {
                    if ((0x00 == *ps_tpz_info->CCByteBuf) || 
                        (NDEF_T_TLV == ps_tpz_info->ExpectedSeq))
                    {
                        /* This statement is for getting the new 
                            NDEF TLV byte address, because 1st CC byte is  
                            corrupted or no NDEF TLV in the card

                            If the 1st CC byte (NDEF magic number) in the  
                            card is 0, means that previous write has failed, 
                            so to write the exact file 
                            OR
                            The NDEF TLV is not present in the entire card, and  
                            the sequence is NDEF_T_TLV (this means, that lock and  
                            memory control TLV is found in the card)                
                        */
                        psNdefMap->State = (uint8_t)
                                        PH_FRINFC_TOPAZ_STATE_RD_FOR_WR_NDEF;
                        ps_tpz_info->WriteSeq = (uint8_t)WR_NDEF_T_TLV;

                        ps_tpz_info->CurrentBlock = (uint8_t)
                                    TOPAZ_BLK_FROM_BYTE_ADR (
                                        ps_tpz_info->NdefTLVByteAddress);

                        ps_tpz_info->ByteNumber = (uint8_t)
                                    TOPAZ_BYTE_OFFSET_FROM_BYTE_ADR (
                                        ps_tpz_info->NdefTLVByteAddress);

#ifdef TOPAZ_RAW_SUPPORT
                        *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_READ8;
#else
                        psNdefMap->Cmd.JewelCmd = phHal_eJewel_Read8;
#endif /* #ifdef TOPAZ_RAW_SUPPORT */

                        result = phFriNfc_Tpz_H_NxpRead (psNdefMap);
                    }
                    else
                    {
                        ps_tpz_info->WriteSeq = (uint8_t)WR_NMN_0;
                        ps_tpz_info->CurrentBlock = 1;
                        ps_tpz_info->ByteNumber = 0;
                        psNdefMap->State = (uint8_t)
                                            PH_FRINFC_TOPAZ_STATE_WRITE;
#ifdef TOPAZ_RAW_SUPPORT
                        *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_WRITE_1E;
#else
                        psNdefMap->Cmd.JewelCmd = phHal_eJewel_Write1E;
#endif /* #ifdef TOPAZ_RAW_SUPPORT */

                        /* Call read 8 */
                        result = phFriNfc_Tpz_H_NxpWrite (psNdefMap, write_buffer, 
                                                    sizeof (write_buffer));
                    }
                    
                }
            }
            else
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH); 
            }
            break;
        }

        default:
        {
            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                NFCSTATUS_INVALID_DEVICE_REQUEST);
            break;
        }
    }
    
    return result;
}



static void phFriNfc_Tpz_H_Complete(phFriNfc_NdefMap_t  *NdefMap,
                                    NFCSTATUS           Status)
{
    /* set the state back to the Reset_Init state*/
    NdefMap->State =  PH_FRINFC_NDEFMAP_STATE_RESET_INIT;

    /* set the completion routine*/
    NdefMap->CompletionRoutine[NdefMap->TopazContainer.CRIndex].
        CompletionRoutine(NdefMap->CompletionRoutine->Context, Status);
}

static 
NFCSTATUS  
phFriNfc_Tpz_H_ChkLockBits(
    phFriNfc_NdefMap_t  *psNdefMap)
{
    NFCSTATUS           result = NFCSTATUS_SUCCESS;
#ifdef ENABLE_LOCK_BITS_CHECK
    uint8_t             *p_recv_buf = psNdefMap->SendRecvBuf;
#endif /* #ifdef ENABLE_LOCK_BITS_CHECK */
    psNdefMap->CardState = PH_NDEFMAP_CARD_STATE_INITIALIZED;

#ifdef ENABLE_LOCK_BITS_CHECK

    /* Set the card state */
    psNdefMap->CardState =  (uint8_t)
        (((p_recv_buf[PH_FRINFC_TOPAZ_DYNAMIC_LOCKBIT_BYTENO_0] == 
            PH_FRINFC_TOPAZ_DYNAMIC_LOCKBYTE_0) && 
            ((p_recv_buf[PH_FRINFC_TOPAZ_DYNAMIC_LOCKBIT_BYTENO_1] == 
            PH_FRINFC_TOPAZ_DYNAMIC_LOCKBYTE_1)) &&
            ((p_recv_buf[PH_FRINFC_TOPAZ_DYNAMIC_LOCKBIT_BYTENO_2] == 
            PH_FRINFC_TOPAZ_DYNAMIC_LOCKBYTE_2TO7)) &&
            ((p_recv_buf[PH_FRINFC_TOPAZ_DYNAMIC_LOCKBIT_BYTENO_3] == 
            PH_FRINFC_TOPAZ_DYNAMIC_LOCKBYTE_2TO7)) &&
            ((p_recv_buf[PH_FRINFC_TOPAZ_DYNAMIC_LOCKBIT_BYTENO_4] == 
            PH_FRINFC_TOPAZ_DYNAMIC_LOCKBYTE_2TO7)) &&
            ((p_recv_buf[PH_FRINFC_TOPAZ_DYNAMIC_LOCKBIT_BYTENO_5] == 
            PH_FRINFC_TOPAZ_DYNAMIC_LOCKBYTE_2TO7)) &&
            ((p_recv_buf[PH_FRINFC_TOPAZ_DYNAMIC_LOCKBIT_BYTENO_6] == 
            PH_FRINFC_TOPAZ_DYNAMIC_LOCKBYTE_2TO7))) &&
            ((p_recv_buf[PH_FRINFC_TOPAZ_DYNAMIC_LOCKBIT_BYTENO_7] == 
            PH_FRINFC_TOPAZ_DYNAMIC_LOCKBYTE_2TO7)) ?
                PH_NDEFMAP_CARD_STATE_INITIALIZED :
                PH_NDEFMAP_CARD_STATE_READ_ONLY);

#endif /* #ifdef ENABLE_LOCK_BITS_CHECK */

    /* Set the card state from CC bytes */
    if (PH_NDEFMAP_CARD_STATE_INITIALIZED == psNdefMap->CardState)
    {
        switch ((psNdefMap->TopazContainer.CCByteBuf[3] & 0xFF))
        {
            case PH_FRINFC_TOPAZ_CC_READWRITE:
            {
                psNdefMap->CardState = PH_NDEFMAP_CARD_STATE_INITIALIZED;
                break;
            }

            case PH_FRINFC_TOPAZ_CC_READONLY:
            {
                psNdefMap->CardState = PH_NDEFMAP_CARD_STATE_READ_ONLY;
                break;
            }

            default: 
            {
                psNdefMap->CardState = PH_NDEFMAP_CARD_STATE_INVALID;
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_NO_NDEF_SUPPORT);
                break;
            }
        }
    }
    
    return result;
}

static 
NFCSTATUS 
phFriNfc_Tpz_H_CheckCCBytes (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    NFCSTATUS                       result = NFCSTATUS_SUCCESS;
    phFriNfc_TopazCont_t            *ps_tpz_info = &(psNdefMap->TopazContainer);
    uint8_t                         *p_recv_buf = psNdefMap->SendRecvBuf;
    uint16_t                        parse_index = 0;

    parse_index = (uint16_t)(parse_index + TOPAZ_UID_BYTES_LENGTH);

    (void)memcpy ((void *)ps_tpz_info->CCByteBuf, 
                (void *)(p_recv_buf + parse_index), 
                TOPAZ_CC_BYTES_LENGTH);

    p_recv_buf = ps_tpz_info->CCByteBuf;
    parse_index = 0;

#ifdef TOPAZ_MAGIC_NO_CHK_ENABLE
    /* 1st CC byte value = 0 or 0xE1 */
    if ((PH_FRINFC_TOPAZ_CC_BYTE0 == p_recv_buf[parse_index])
#ifdef TOPAZ_MAGIC_NO_0_CHK_ENABLE
        || (0 == p_recv_buf[parse_index])
#endif /* #if TOPAZ_MAGIC_NO_0_CHK_ENABLE */
        )
#endif /* #ifdef TOPAZ_MAGIC_NO_CHK_ENABLE */
    {
        parse_index = (uint16_t)(parse_index + 1);
        /* 2nd CC byte value = 0x10 */
        result = phFriNfc_Tpz_H_ChkSpcVer (psNdefMap, p_recv_buf[parse_index]);        
    }
#ifdef TOPAZ_MAGIC_NO_CHK_ENABLE
    else
    {
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                            NFCSTATUS_NO_NDEF_SUPPORT);
    }
#endif /* #ifdef TOPAZ_MAGIC_NO_CHK_ENABLE */

    if (NFCSTATUS_SUCCESS == result)
    {
        parse_index = (uint16_t)(parse_index + 1);
        /* 3rd CC byte value = 0x3F for 512 card */
        if (PH_FRINFC_TOPAZ_DYNAMIC_CC_BYTE2_MMSIZE == p_recv_buf[parse_index])
        {
            /* Card size calculated as ((3rd CC byte * 8) - 4 CC bytes) */
            psNdefMap->CardMemSize = (uint16_t)((p_recv_buf[parse_index] * 
                                    TOPAZ_BYTES_PER_BLOCK) - 
                                    TOPAZ_CC_BYTES_LENGTH);
            ps_tpz_info->RemainingSize = (uint16_t)(psNdefMap->CardMemSize + 
                                        TOPAZ_UID_BYTES_LENGTH + 
                                        TOPAZ_CC_BYTES_LENGTH);
            result = phFriNfc_Tpz_H_ChkLockBits (psNdefMap);
        }
        else
        {
            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                NFCSTATUS_NO_NDEF_SUPPORT);
        }
    }

    if (NFCSTATUS_SUCCESS != result)
    {
        psNdefMap->CardState = PH_NDEFMAP_CARD_STATE_INVALID;
    }

    return result;
}

static 
NFCSTATUS 
phFriNfc_Tpz_H_CheckCCBytesForWrite (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    NFCSTATUS                           result = NFCSTATUS_SUCCESS;
    phFriNfc_TopazCont_t                *ps_tpz_info = NULL;
    uint8_t                             check_cc_rw[] = {TOPAZ_SPEC_VERSION, 
                                        PH_FRINFC_TOPAZ_DYNAMIC_CC_BYTE2_MMSIZE, 
                                        PH_FRINFC_TOPAZ_CC_READWRITE};
    uint8_t                             check_index = 0;

    ps_tpz_info = &(psNdefMap->TopazContainer);

#ifdef TOPAZ_MAGIC_NO_CHK_ENABLE
    if (
        (PH_FRINFC_TOPAZ_CC_BYTE0 == ps_tpz_info->CCByteBuf[check_index]) 
#if TOPAZ_MAGIC_NO_0_CHK_ENABLE
        || (0 == ps_tpz_info->CCByteBuf[check_index])
#endif /* #if TOPAZ_MAGIC_NO_0_CHK_ENABLE */
        )
#endif /* #ifdef TOPAZ_MAGIC_NO_CHK_ENABLE */
    {
        check_index = (uint8_t)(check_index + 1);

        if ((!TOPAZ_COMPARE_VERSION(check_cc_rw[0], ps_tpz_info->CCByteBuf[1])) ||
            (check_cc_rw[1] != ps_tpz_info->CCByteBuf[2]) || 
            (check_cc_rw[2] != ps_tpz_info->CCByteBuf[3]))
        {
            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                NFCSTATUS_NO_NDEF_SUPPORT);
        }
    }
#ifdef TOPAZ_MAGIC_NO_CHK_ENABLE
    else
    {
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                            NFCSTATUS_NO_NDEF_SUPPORT);
    }    
#endif /* #ifdef TOPAZ_MAGIC_NO_CHK_ENABLE */
    return result;
}

static 
uint16_t 
phFriNfc_Tpz_H_GetNDEFValueFieldAddrForRead (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    phFriNfc_TopazCont_t            *ps_tpz_info = &(psNdefMap->TopazContainer);
    uint16_t                        skip_size = 0;
    uint16_t                        byte_addr = 0;
    uint8_t                         exit_index = 0;

    byte_addr = ps_tpz_info->NdefTLVByteAddress;

    while (exit_index < ((ps_tpz_info->ActualNDEFMsgSize >= 0xFF) ? 3 : 1))
    {
        byte_addr = (uint16_t)(byte_addr + 1);
        if (TOPAZ_STATIC_LOCK_RES_START == byte_addr)
        {
            byte_addr = (uint16_t)(byte_addr + TOPAZ_STATIC_LOCK_RES_BYTES);
        }
        skip_size = phFriNfc_Tpz_H_GetSkipSize (psNdefMap, byte_addr);
        
        byte_addr = (uint16_t)(byte_addr + skip_size);
        exit_index = (uint8_t)(exit_index + 1);
    }

    byte_addr = (uint16_t)(byte_addr + 1);
    if (TOPAZ_STATIC_LOCK_RES_START == byte_addr)
    {
        byte_addr = (uint16_t)(byte_addr + TOPAZ_STATIC_LOCK_RES_BYTES);
    }
    skip_size = phFriNfc_Tpz_H_GetSkipSize (psNdefMap, byte_addr);
    
    byte_addr = (uint16_t)(byte_addr + skip_size);

    return byte_addr;
}

static 
uint16_t 
phFriNfc_Tpz_H_GetNDEFValueFieldAddrForWrite (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint16_t                    size_to_write)
{
    phFriNfc_TopazCont_t            *ps_tpz_info = &(psNdefMap->TopazContainer);
    uint16_t                        skip_size = 0;
    uint16_t                        byte_addr = 0;
    uint8_t                         exit_index = 0;

    byte_addr = ps_tpz_info->NdefTLVByteAddress;

    while (exit_index < ((size_to_write >= 0xFF) ? 3 : 1))
    {
        byte_addr = (uint16_t)(byte_addr + 1);
        if (TOPAZ_STATIC_LOCK_RES_START == byte_addr)
        {
            byte_addr = (uint16_t)(byte_addr + TOPAZ_STATIC_LOCK_RES_BYTES);
        }
        skip_size = phFriNfc_Tpz_H_GetSkipSize (psNdefMap, byte_addr);
        
        byte_addr = (uint16_t)(byte_addr + skip_size);
        exit_index = (uint8_t)(exit_index + 1);
    }

    byte_addr = (uint16_t)(byte_addr + 1);
    if (TOPAZ_STATIC_LOCK_RES_START == byte_addr)
    {
        byte_addr = (uint16_t)(byte_addr + TOPAZ_STATIC_LOCK_RES_BYTES);
    }
    skip_size = phFriNfc_Tpz_H_GetSkipSize (psNdefMap, byte_addr);
    
    byte_addr = (uint16_t)(byte_addr + skip_size);

    return byte_addr;
}


static 
NFCSTATUS 
phFriNfc_Tpz_H_RemainingReadDataCopy (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    NFCSTATUS                       result = NFCSTATUS_SUCCESS;
    phFriNfc_TopazCont_t            *ps_tpz_info = &(psNdefMap->TopazContainer);
    uint8_t                         copy_temp_buf[PH_FRINFC_NDEFMAP_TOPAZ_MAX_SIZE];
    uint16_t                        copy_length = 0;
    uint16_t                        read_copy_length = 0;


    if (0 != ps_tpz_info->ReadBufferSize)
    {
        /* Data is already copied, so give it from the stored buffer */
        if ((psNdefMap->ApduBufferSize - psNdefMap->ApduBuffIndex) >= 
            ps_tpz_info->ReadBufferSize)
        {
            read_copy_length = ps_tpz_info->ReadBufferSize;
            (void)memcpy ((void *)(psNdefMap->ApduBuffer + psNdefMap->ApduBuffIndex), 
                    (void *)ps_tpz_info->ReadBuffer, ps_tpz_info->ReadBufferSize);
        }
        else
        {
            read_copy_length = (uint16_t)(psNdefMap->ApduBufferSize - 
                                psNdefMap->ApduBuffIndex);

            copy_length = (uint16_t)(ps_tpz_info->ReadBufferSize - 
                            read_copy_length);

            /* Copy data to user buffer */
            (void)memcpy ((void *)(psNdefMap->ApduBuffer + psNdefMap->ApduBuffIndex), 
                    (void *)ps_tpz_info->ReadBuffer, read_copy_length);

            /* Copy data from " ReadBuffer " to temporary buffer */
            (void)memcpy ((void *)copy_temp_buf, 
                    (void *)(ps_tpz_info->ReadBuffer + read_copy_length), 
                    copy_length);

            /* Copy data from temporary buffer to " ReadBuffer " */
            (void)memcpy ((void *)ps_tpz_info->ReadBuffer, 
                    (void *)copy_temp_buf, copy_length);

        }

        psNdefMap->ApduBuffIndex = (uint16_t)(psNdefMap->ApduBuffIndex + 
                                    read_copy_length);
        ps_tpz_info->ReadBufferSize = (uint8_t)
                            (ps_tpz_info->ReadBufferSize - 
                            read_copy_length);
        ps_tpz_info->RemainingReadSize = (uint16_t)(
                            ps_tpz_info->RemainingReadSize - read_copy_length);
    }

    if (0 == ps_tpz_info->RemainingReadSize)
    {
        /* No data to read, so return */
        *psNdefMap->NumOfBytesRead = psNdefMap->ApduBuffIndex;
        ps_tpz_info->ReadBufferSize = 0;
        ps_tpz_info->ReadWriteCompleteFlag = TRUE;
    }
    else if (psNdefMap->ApduBuffIndex == psNdefMap->ApduBufferSize)
    {
        /* User data length is read completely */
        *psNdefMap->NumOfBytesRead = psNdefMap->ApduBuffIndex;
    }
    else
    {
        /* Stored data is not enough, so continue reading the next segment */
        ps_tpz_info->CurrentSeg = (uint8_t)
                            (ps_tpz_info->CurrentSeg + 1);
#ifdef TOPAZ_RAW_SUPPORT

        *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_RSEG;

#else 

        psNdefMap->Cmd.JewelCmd = phHal_eJewel_ReadSeg;

#endif /* #ifdef TOPAZ_RAW_SUPPORT */
        result = phFriNfc_Tpz_H_NxpRead (psNdefMap);
    }

    return result;
}

static 
NFCSTATUS 
phFriNfc_Tpz_H_CopyReadData (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    NFCSTATUS                       result = NFCSTATUS_SUCCESS;    
    phFriNfc_TopazCont_t            *ps_tpz_info = &(psNdefMap->TopazContainer);
    phFriNfc_LockCntrlTLVCont_t     *ps_locktlv_info = NULL;
    phFriNfc_ResMemCntrlTLVCont_t   *ps_memtlv_info = NULL;
    uint16_t                        copy_index = 0;
    uint16_t                        copy_length = 0;
    uint16_t                        recv_length = 0;
    static uint16_t                 skip_size = 0;
    /* byte address read */
    uint16_t                        copy_till_address = 0;
    uint16_t                        exact_copy_length = 0;
    uint16_t                        actual_ndef_length = 0;
    

    recv_length = *(psNdefMap->SendRecvLength);
    
    actual_ndef_length = ps_tpz_info->ActualNDEFMsgSize;
    if (PH_FRINFC_NDEFMAP_SEEK_CUR == psNdefMap->Offset)
    {
        actual_ndef_length = (uint16_t)(
                            ps_tpz_info->RemainingReadSize + 
                            psNdefMap->ApduBuffIndex);
    }
    
    exact_copy_length = (uint16_t)((psNdefMap->ApduBufferSize > 
                            actual_ndef_length) ? actual_ndef_length : 
                            psNdefMap->ApduBufferSize);

    if (0 == ps_tpz_info->CurrentSeg)
    {
        /* Skip copying the UID bytes, CC bytes, and lock and reserved memory bytes 
             */
        recv_length = (*(psNdefMap->SendRecvLength) - TOPAZ_STATIC_LOCK_RES_BYTES);
    }

    if (TOPAZ_SEG_FROM_BYTE_ADR (
        phFriNfc_Tpz_H_GetNDEFValueFieldAddrForRead (psNdefMap)) == 
        ps_tpz_info->CurrentSeg)
    {
        copy_index = (uint16_t)(copy_index + (
                    phFriNfc_Tpz_H_GetNDEFValueFieldAddrForRead (
                        psNdefMap) % TOPAZ_SEGMENT_READ_LENGTH));
        skip_size = 0;
    }

    if (0 != skip_size)
    {
        copy_index = (copy_index + skip_size);
        skip_size = 0;
    }
    
    while (copy_index < recv_length)
    {
        copy_length = (uint16_t)(recv_length - copy_index);
        copy_till_address = 0;
        /* IF MORE THAN ONE TLV EXISTS THEN ADD A WHILE LOOP HERE, AND PLACE THE 
            IF STATEMENT INSIDE THE WHILE LOOP. ALSO,
            ps_locktlv_info = &(psNdefMap->LockTlv) change this to 
            ps_locktlv_info = &(psNdefMap->LockTlv[index])
            */
        ps_locktlv_info = &(psNdefMap->LockTlv);
        if (
            /* Check the lock bytes belong to this segment */
            (ps_tpz_info->CurrentSeg == 
            (ps_locktlv_info->ByteAddr / TOPAZ_SEGMENT_READ_LENGTH)) && 
            /* Now to check if the copy_index has surpassed the lock byte address */
            (TOPAZ_BYTE_ADR_FROM_SEG(ps_tpz_info->CurrentSeg, copy_index) 
            <= ps_locktlv_info->ByteAddr)
            )
        {
            if ((ps_locktlv_info->ByteAddr < TOPAZ_STATIC_LOCK_RES_START) || 
                (ps_locktlv_info->ByteAddr >= (TOPAZ_STATIC_LOCK_RES_END + 8)))
            {
                copy_till_address = ps_locktlv_info->ByteAddr;
            }
            skip_size = phFriNfc_Tpz_H_GetSkipSize (psNdefMap, 
                                                        ps_locktlv_info->ByteAddr);
        }

        /* IF MORE THAN ONE TLV EXISTS THEN ADD A WHILE LOOP HERE, AND PLACE THE 
            IF STATEMENT INSIDE THE WHILE LOOP. ALSO,
            ps_memtlv_info = &(psNdefMap->MemTlv) change this to 
            ps_memtlv_info = &(psNdefMap->MemTlv[index])
            */
        ps_memtlv_info = &(psNdefMap->MemTlv);
        if (
            /* Check the reserved bytes belong to this segment */
            (ps_tpz_info->CurrentSeg == 
            (ps_memtlv_info->ByteAddr / TOPAZ_SEGMENT_READ_LENGTH)) && 
            /* Now to check if the copy_index has surpassed the reserved byte address */
            (TOPAZ_BYTE_ADR_FROM_SEG(ps_tpz_info->CurrentSeg, copy_index) 
            <= ps_memtlv_info->ByteAddr)
            )
        {
            if ((ps_memtlv_info->ByteAddr < TOPAZ_STATIC_LOCK_RES_START) ||  
                (ps_memtlv_info->ByteAddr >= (TOPAZ_STATIC_LOCK_RES_END + 8)))
            {
                copy_till_address = (uint16_t)
                            (((ps_memtlv_info->ByteAddr < copy_till_address) || 
                                (0 == copy_till_address))?  
                            ps_memtlv_info->ByteAddr : copy_till_address);
            }

            if (copy_till_address == ps_memtlv_info->ByteAddr)
            {
                skip_size = phFriNfc_Tpz_H_GetSkipSize (psNdefMap, 
                                                            ps_memtlv_info->ByteAddr);
            }
        }


        copy_length = (uint16_t) ((copy_till_address == 0) ? copy_length : 
                    ((copy_till_address % TOPAZ_SEGMENT_READ_LENGTH) - 
                    copy_index));

        /* After lock bytes, there are immediate reserved bytes, so " copy_length " 
            can be 0 */
        if (0 != copy_length)
        {
            /* If complete user buffer is not filled and the 
                read data is greater than the user data buffer, then get the 
                remaining size that should be copied. 
                The below " if " statement is used for the above scenario */
            if ((copy_length > (uint16_t)
                (exact_copy_length - psNdefMap->ApduBuffIndex)) && 
                (exact_copy_length != psNdefMap->ApduBuffIndex))
            {            
                copy_length = (uint16_t)(exact_copy_length - 
                                        psNdefMap->ApduBuffIndex);
            }

            if (exact_copy_length != psNdefMap->ApduBuffIndex)
            {
                (void)memcpy ((void *)(psNdefMap->ApduBuffer + 
                        psNdefMap->ApduBuffIndex), 
                        (void *)(psNdefMap->SendRecvBuf + copy_index), 
                        copy_length);
#if 0
                if (((copy_till_address == 0) ? copy_length : 
                    ((copy_till_address % TOPAZ_SEGMENT_READ_LENGTH) - 
                    copy_index)) > (uint16_t)
                    (exact_copy_length - psNdefMap->ApduBuffIndex))
                {                
                    /* Copy remaining buffer in the static memory */
                    (void)memcpy ((void *)(ps_tpz_info->ReadBuffer + 
                            ps_tpz_info->ReadBufferSize), 
                            (void *)(psNdefMap->SendRecvBuf + copy_index), 
                            (((copy_till_address % TOPAZ_SEGMENT_READ_LENGTH) - 
                            copy_index) - copy_length));

                    ps_tpz_info->ReadBufferSize = (uint16_t)(((copy_till_address % 
                                                    TOPAZ_SEGMENT_READ_LENGTH) - 
                                                    copy_index) - copy_length);

                    /* Copy the data in the user buffer */
                    copy_index = (uint16_t)(copy_index + 
                                ((copy_till_address % TOPAZ_SEGMENT_READ_LENGTH) - 
                                copy_index));
                }
                else
#endif /* #if 0 */
                {
                    /* Copy the data in the user buffer */
                    copy_index = (uint16_t)(copy_index + copy_length);
                }

                psNdefMap->ApduBuffIndex = (uint16_t)(psNdefMap->ApduBuffIndex + 
                                            copy_length);


            }
            else
            {
                copy_length = (uint16_t) ((copy_till_address == 0) ? copy_length : 
                            ((copy_till_address % TOPAZ_SEGMENT_READ_LENGTH) - 
                            copy_index));

                /* Actual NDEF message size is greater than the last index copied in 
                    the user buffer */
                if (actual_ndef_length > (psNdefMap->ApduBuffIndex + 
                    ps_tpz_info->ReadBufferSize))
                {
                    /* The statement is correct, check the remaining length */
                    copy_length = ((copy_length > (actual_ndef_length - 
                                psNdefMap->ApduBuffIndex)) ? 
                                (actual_ndef_length - 
                                psNdefMap->ApduBuffIndex) : 
                                copy_length);

                    /* Copy remaining buffer in the static memory */
                    (void)memcpy ((void *)(ps_tpz_info->ReadBuffer + 
                                ps_tpz_info->ReadBufferSize), 
                                (void *)(psNdefMap->SendRecvBuf + copy_index), 
                                copy_length);

                    ps_tpz_info->ReadBufferSize = (uint8_t)(
                                                    ps_tpz_info->ReadBufferSize + 
                                                    copy_length);
                }

                /* Copy the data in the user buffer */
                copy_index = (uint16_t)(copy_index + copy_length); 
            }
        }

        if (copy_index != copy_till_address)
        {
            skip_size = 0;
        }

        if ((copy_index + skip_size) <= recv_length)
        {
            copy_index = (uint16_t)(copy_index + skip_size);
            skip_size = 0;
        }
        else
        {
            skip_size = (uint16_t)((skip_size > 0) ? 
                                    (recv_length - copy_index) : 0);
            copy_index = (uint16_t)recv_length;
        }                        
    }

    if (exact_copy_length != psNdefMap->ApduBuffIndex)
    {
        ps_tpz_info->CurrentSeg = (uint8_t)
                            (ps_tpz_info->CurrentSeg + 1);
#ifdef TOPAZ_RAW_SUPPORT

        *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_RSEG;

#else 

        psNdefMap->Cmd.JewelCmd = phHal_eJewel_ReadSeg;

#endif /* #ifdef TOPAZ_RAW_SUPPORT */
        result = phFriNfc_Tpz_H_NxpRead (psNdefMap);
    }
    else
    {
        *psNdefMap->NumOfBytesRead = psNdefMap->ApduBuffIndex;
        if (psNdefMap->ApduBuffIndex == actual_ndef_length)
        {
            ps_tpz_info->ReadBufferSize = 0;
            ps_tpz_info->ReadWriteCompleteFlag = TRUE;
        }
        else
        {
            ps_tpz_info->RemainingReadSize = (actual_ndef_length - 
                                        psNdefMap->ApduBuffIndex);
        }
    }
    return result;
}


static 
NFCSTATUS 
phFriNfc_Tpz_H_ParseTLVs (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    phFriNfc_TopazCont_t        *ps_tpz_info = &(psNdefMap->TopazContainer);
    uint8_t                     *p_recv_buf = NULL;
    uint16_t                    recv_length = 0;
    uint16_t                    parse_index = 0;
    phFriNfc_Tpz_ParseSeq_t     expected_seq = (phFriNfc_Tpz_ParseSeq_t)
                                ps_tpz_info->ExpectedSeq;
    uint16_t                    byte_addr = 0;
    /* This variable is kept static because if the size to skip LOCK or RESERVED 
    bytes extends to next read then it shall be stored and used to skip the next 
    read the bytes
    */
    static uint16_t             skip_size = 0;
    /* This variable is kept static because if the bytes extends from the read segment, 
        then the index shall be stored
    This is to store index copied from the 
    1. lock memory VALUE field bytes in the LOCK and MEMORY CONTROL TLV. 
    2. Also, LENGTH field of the NDEF TLV */
    static uint8_t              lock_mem_ndef_index = 0;
    /* This variable is kept static because if the bytes extends from the read segment, 
        then it has to stored
    This is to store the 
    1. lock memory VALUE field bytes in the LOCK and MEMORY CONTROL TLV. 
    2. Also, LENGTH field of the NDEF TLV */
    static uint8_t              lock_mem_buf[TOPAZ_MEM_LOCK_TLV_LENGTH] = {0};
    /* This is used in case if there is no MAGIC NUMBER found 
                        OR 
        TYPE field is not found after reading entire card */
    static uint16_t             ndef_tlv_byte_addr = 0;

    p_recv_buf = psNdefMap->SendRecvBuf;
    recv_length = *psNdefMap->SendRecvLength;

    if (0 == ps_tpz_info->CurrentSeg)
    {
        /* First read, so reset all the static variables */
        lock_mem_ndef_index = 0;
        skip_size = 0;
        ndef_tlv_byte_addr = 0;

        /* Skip copying the UID bytes and CC bytes, which is first 12 bytes */
        parse_index = (uint16_t)(TOPAZ_UID_BYTES_LENGTH + 
                                TOPAZ_CC_BYTES_LENGTH);
        /* Delete the lock and reserved memory bytes 
            (which are the last 24 bytes in the card) */
        recv_length = (uint16_t)(*(psNdefMap->SendRecvLength) - 
                                TOPAZ_STATIC_LOCK_RES_BYTES);        
    }

    while ((parse_index < recv_length) && (NFCSTATUS_SUCCESS == result) && 
        (NDEF_V_TLV != expected_seq))
    {
        if (0 == skip_size)
        {
            /* Macro used to get the exact byte address of the card. 
                This is done by using the current segment and the parse index */
            byte_addr = TOPAZ_BYTE_ADR_FROM_SEG (ps_tpz_info->CurrentSeg, parse_index);
            /* Skip size is to skip the lock or memory reserved bytes  */
            skip_size = phFriNfc_Tpz_H_GetSkipSize (psNdefMap, byte_addr);
        }

        if (0 != skip_size)
        {
            if ((recv_length - parse_index) >= skip_size)
            {
                parse_index = (uint16_t)(parse_index + skip_size);
                skip_size = 0;
            }
            else
            {
                parse_index = (uint16_t)(parse_index + (recv_length - 
                                parse_index));
                skip_size = (uint16_t)(skip_size - (recv_length - 
                                parse_index));
            }
        }
        else 
        {
            switch (expected_seq)
            {
                case LOCK_T_TLV:
                {
                    /* Parse the bytes till TYPE field of LOCK TLV is found, Once the 
                        TYPE field is found then change the sequence to LOCK_L_TLV */
                    result = phFriNfc_Tpz_H_ParseLockTLVType (psNdefMap, p_recv_buf, 
                                            &parse_index, recv_length, &expected_seq);
                    
                    break;
                }

                case LOCK_L_TLV:
                {
                    /* Parse the length field of LOCK TLV. Length field value of the 
                        LOCK TLV is always 3 */
                    if (TOPAZ_MEM_LOCK_TLV_LENGTH != p_recv_buf[parse_index])
                    {
                        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                            NFCSTATUS_NO_NDEF_SUPPORT);
                    }
                    else
                    {
                        parse_index = (uint16_t)(parse_index + 1);
                        expected_seq = LOCK_V_TLV;
                    }
                    break;
                }

                case LOCK_V_TLV:
                {
                    /* Parse the VALUE field of the LOCK TLV */
                    lock_mem_buf[lock_mem_ndef_index] = p_recv_buf[parse_index];
                    parse_index = (uint16_t)(parse_index + 1);
                    lock_mem_ndef_index = (uint8_t)(lock_mem_ndef_index + 1);
                    

                    /* All the 3 bytes are copied in the local buffer */
                    if (TOPAZ_MEM_LOCK_TLV_LENGTH == lock_mem_ndef_index)
                    {
#ifdef FRINFC_READONLY_NDEF
                        (void)memcpy ((void *)psNdefMap->LockTlv.LockTlvBuff, 
                                (void *)lock_mem_buf, sizeof (lock_mem_buf));
#endif /* #ifdef FRINFC_READONLY_NDEF */
                        /* Calculate the byte address and size of the lock bytes */
                        result = phFriNfc_Tpz_H_GetLockBytesInfo (psNdefMap, lock_mem_buf);
                        lock_mem_ndef_index = 0;
                        expected_seq = MEM_T_TLV;                    
                    }
                    break;
                }

                case MEM_T_TLV:
                {
                    /* Parse the bytes till TYPE field of MEMORY TLV is found, Once the 
                        TYPE field is found then change the sequence to MEM_L_TLV */
                    result = phFriNfc_Tpz_H_ParseMemTLVType (psNdefMap, p_recv_buf, 
                                            &parse_index, recv_length, &expected_seq);                    
                    break;
                }

                case MEM_L_TLV:
                {
                    /* Parse the length field of MEMORY TLV. Length field value of the 
                        MEMORY TLV is always 3 */
                    if (TOPAZ_MEM_LOCK_TLV_LENGTH != p_recv_buf[parse_index])
                    {
                        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                            NFCSTATUS_NO_NDEF_SUPPORT);
                    }
                    else
                    {
                        parse_index = (uint16_t)(parse_index + 1);
                        expected_seq = MEM_V_TLV;
                    }

                    break;
                }

                case MEM_V_TLV:
                {
                    /* Parse the VALUE field of the MEMORY TLV */
                    lock_mem_buf[lock_mem_ndef_index] = p_recv_buf[parse_index];
                    parse_index = (uint16_t)(parse_index + 1);
                    lock_mem_ndef_index = (uint8_t)(lock_mem_ndef_index + 1);

                    /* All the 3 bytes are copied in the local buffer */
                    if (TOPAZ_MEM_LOCK_TLV_LENGTH == lock_mem_ndef_index)
                    {
                        /* Calculate the byte address and size of the lock bytes */
                        ndef_tlv_byte_addr = TOPAZ_BYTE_ADR_FROM_SEG (
                                            ps_tpz_info->CurrentSeg , parse_index);
                        result = phFriNfc_Tpz_H_GetMemBytesInfo (psNdefMap, lock_mem_buf);
                        lock_mem_ndef_index = 0;
                        expected_seq = NDEF_T_TLV;
                    }

                    break;
                }

                case NDEF_T_TLV:
                {
                    /* Parse the bytes till TYPE field of NDEF TLV is found, Once the 
                        TYPE field is found then change the sequence to NDEF_L_TLV */
                    result = phFriNfc_Tpz_H_ParseNdefTLVType (psNdefMap, p_recv_buf, 
                                            &parse_index, recv_length, &expected_seq);
                    
                    break;
                }

                case NDEF_L_TLV:
                {
                    /* Length field of the NDEF TLV */
                    if (0 == lock_mem_ndef_index)
                    {
                        /* This is the 1st time, the loop has entered this case, 
                            means that the NDEF byte address has to be updated */
                        ps_tpz_info->NdefTLVByteAddress = (uint16_t)
                                TOPAZ_BYTE_ADR_FROM_SEG (ps_tpz_info->CurrentSeg, 
                                (parse_index - 1));
                    }

                    if (0 != lock_mem_ndef_index)
                    {
                        /* There is already index has been updated, update remaining 
                            buffer */
                        lock_mem_buf[lock_mem_ndef_index] = p_recv_buf[parse_index];
                        parse_index = (uint16_t)(parse_index + 1);
                        lock_mem_ndef_index = (uint8_t)(lock_mem_ndef_index + 1);

                        if (TOPAZ_MEM_LOCK_TLV_LENGTH == lock_mem_ndef_index)
                        {
                            lock_mem_ndef_index = 0;
                            ps_tpz_info->ActualNDEFMsgSize = (uint16_t)((lock_mem_buf[1] << 
                                        TOPAZ_BYTE_SHIFT) | lock_mem_buf[2]);
                            expected_seq = NDEF_V_TLV;
                        }
                    }
                    /* Check for remaining size in the card and the actual ndef length */
                    else if (p_recv_buf[parse_index] <= 
                            (ps_tpz_info->RemainingSize - (parse_index + 1)))
                    {
                        /* This check is added to see that length field in the TLV is 
                            greater than the 1 byte */
                        if (0xFF == p_recv_buf[parse_index])
                        {
                            lock_mem_buf[lock_mem_ndef_index] = 
                                                    p_recv_buf[parse_index];
                            lock_mem_ndef_index = (uint8_t)(lock_mem_ndef_index + 1);
                        }
                        else
                        {
                            /* Length field of the TLV is ONE byte, so update the 
                            actual ndef size */
                            lock_mem_ndef_index = 0;
                            ps_tpz_info->ActualNDEFMsgSize = (uint16_t)
                                                        p_recv_buf[parse_index];
                            
                            expected_seq = NDEF_V_TLV;
                        }
                        parse_index = (uint16_t)(parse_index + 1);
                    }
                    else
                    {
                        /* Wrong length, remaining size in the card is lesser than the actual 
                            ndef message length */
                        lock_mem_ndef_index = 0;
                        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                            NFCSTATUS_NO_NDEF_SUPPORT);
                    }
                    break;
                }

                default:
                {
                    break;
                }
            }/* end of switch (expected_seq) */
        } /* end of if (0 != skip_size) */
    } /* while ((parse_index < recv_length) && (NFCSTATUS_SUCCESS != result) && 
        (NDEF_V_TLV != expected_seq)) */

    ps_tpz_info->ExpectedSeq = (uint8_t)expected_seq;

    if (0 == ps_tpz_info->CurrentSeg)
    {
        /* First segment has the STATIC lock and reserved bytes, so delete it from 
            the remaining size */
        ps_tpz_info->RemainingSize = (uint16_t)(ps_tpz_info->RemainingSize - 
                                    (parse_index + TOPAZ_STATIC_LOCK_RES_BYTES));

    }
    else
    {
        ps_tpz_info->RemainingSize = (uint16_t)(ps_tpz_info->RemainingSize - 
                                    parse_index);
    }

    if ((NDEF_V_TLV == expected_seq) && (NFCSTATUS_SUCCESS == result))
    {
        /* NDEF TLV found */
        result = phFriNfc_Tpz_H_ActualCardSize (psNdefMap);

        if ((PH_NDEFMAP_CARD_STATE_READ_ONLY != psNdefMap->CardState) && 
            (0 != ps_tpz_info->ActualNDEFMsgSize))
        {
            /* Check if the card state is READ ONLY or the actual NDEF size is 0 
                if actual NDEF size is 0, then card state is INITIALISED
            */
            psNdefMap->CardState = PH_NDEFMAP_CARD_STATE_READ_WRITE;
        }
    }

    if ((NFCSTATUS_SUCCESS == result) && (NDEF_V_TLV != expected_seq))
    {
        ps_tpz_info->CurrentSeg = (uint8_t)(ps_tpz_info->CurrentSeg + 1);
        if (TOPAZ_TOTAL_SEG_TO_READ == ps_tpz_info->CurrentSeg)
        {
            /* Max segment to read reached, so no more read can be done */
            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                NFCSTATUS_NO_NDEF_SUPPORT);
        }
        else
        {
#ifdef TOPAZ_RAW_SUPPORT

            *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_RSEG;

#else 

            psNdefMap->Cmd.JewelCmd = phHal_eJewel_ReadSeg;

#endif /* #ifdef TOPAZ_RAW_SUPPORT */ 
            result = phFriNfc_Tpz_H_NxpRead(psNdefMap);
        }
    }

    if ((NFCSTATUS_SUCCESS != result) && (NFCSTATUS_PENDING != result))
    {
        /* Error scenario */
        ps_tpz_info->NdefTLVByteAddress = 0;
        ps_tpz_info->ActualNDEFMsgSize = 0;
    }
    
    if (NFCSTATUS_PENDING != result)
    {
        /* Exit scenario */
        if ((0x00 == *ps_tpz_info->CCByteBuf) || 
            ((NDEF_T_TLV == expected_seq) && 
            (TOPAZ_TOTAL_SEG_TO_READ == ps_tpz_info->CurrentSeg)))
        {
            /* This statement is for getting the new 
                NDEF TLV byte address, because 1st CC byte is corrupted or 
                no NDEF TLV in the card

                If the 1st CC byte (NDEF magic number) in the card is 0, means 
                that previous write has failed, so to write the exact TLV, 
                calculate the byte number
                                            OR
                The NDEF TLV is not present in the entire card, and the sequence is 
                NDEF_T_TLV (this means, that lock and memory control TLV is found 
                in the card)                
                */
            uint16_t             size_to_skip = 0;
            ps_tpz_info->ActualNDEFMsgSize = 0;
            
            if (0 != ndef_tlv_byte_addr)
            {
                /* ndef_tlv_byte_addr is updated, only after complete parsing the 
                    memory control TLV so the value shall not be 0 */
                do 
                {
                    /* This loop is added to make sure the lock and reserved bytes are not 
                    overwritten */
                    size_to_skip = 0;
                    size_to_skip = phFriNfc_Tpz_H_GetSkipSize (psNdefMap, 
                                                            ndef_tlv_byte_addr);

                    ndef_tlv_byte_addr = (uint16_t)(ndef_tlv_byte_addr + 
                                            size_to_skip);
                }while (0 != size_to_skip);

                /* Update the TLV byte address */
                ps_tpz_info->NdefTLVByteAddress = ndef_tlv_byte_addr;

                /* Update the remaining size */
                ps_tpz_info->RemainingSize = (uint16_t)(psNdefMap->CardMemSize + 
                                        TOPAZ_UID_BYTES_LENGTH + 
                                        TOPAZ_CC_BYTES_LENGTH);

                ps_tpz_info->RemainingSize = (uint16_t)
                                            (ps_tpz_info->RemainingSize - 
                                            (ndef_tlv_byte_addr + 
                                            TOPAZ_STATIC_LOCK_RES_BYTES));
                (void)phFriNfc_Tpz_H_ActualCardSize (psNdefMap);

                /* Length byte is subtracted here to get the actual NDEF 
                    read and write size */
                ps_tpz_info->NDEFRWSize = (uint16_t)
                                        (ps_tpz_info->NDEFRWSize - 2);
                ndef_tlv_byte_addr = 0;
                result = NFCSTATUS_SUCCESS;
            }
        }
    }

    return result;
}

static 
NFCSTATUS 
phFriNfc_Tpz_H_CopyReadDataAndWrite (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    NFCSTATUS                           result = NFCSTATUS_SUCCESS;
    phFriNfc_TopazCont_t                *ps_tpz_info = NULL;
    uint8_t                             write_buf[TOPAZ_WRITE_8_DATA_LENGTH];
    uint16_t                            write_index = 0;
    uint16_t                            write_len = 0;
    uint16_t                            byte_addr = 0;
    static uint16_t                     skip_size = 0;

    ps_tpz_info = &(psNdefMap->TopazContainer);

    write_len = (uint16_t)((psNdefMap->ApduBufferSize < ps_tpz_info->NDEFRWSize) ?
                        psNdefMap->ApduBufferSize : ps_tpz_info->NDEFRWSize);

    (void)memcpy ((void *)write_buf, (void *)psNdefMap->SendRecvBuf, 
                    TOPAZ_WRITE_8_DATA_LENGTH);

    if (ps_tpz_info->CurrentBlock == TOPAZ_BLK_FROM_BYTE_ADR (
        phFriNfc_Tpz_H_GetNDEFValueFieldAddrForWrite (psNdefMap, write_len)))
    {
        skip_size = 0;
    }

    /* Byte Number != 0 menas that the VALUE field of the TLV is in between the 
        block, so the first few bytes shall be copied and then user data has to 
        be copied
        */
    if (0 != ps_tpz_info->ByteNumber)
    {
        write_index = (uint16_t)(write_index + ps_tpz_info->ByteNumber);
    }
    

    if (0 != skip_size)
    {
        write_index = (uint16_t)(write_index + skip_size);        
    }

    while ((write_index < TOPAZ_WRITE_8_DATA_LENGTH) && 
        (write_len != psNdefMap->ApduBuffIndex))
    {
        skip_size = 0;
        byte_addr = TOPAZ_BYTE_ADR_FROM_BLK (ps_tpz_info->CurrentBlock, 
                                            ps_tpz_info->ByteNumber);
        skip_size = phFriNfc_Tpz_H_GetSkipSize (psNdefMap, byte_addr);

        if (0 == skip_size)
        {
            write_buf[write_index] = 
                        psNdefMap->ApduBuffer[psNdefMap->ApduBuffIndex];

            write_index = (uint16_t)(write_index + 1);
            psNdefMap->ApduBuffIndex = (uint16_t)
                                        (psNdefMap->ApduBuffIndex + 1);
            ps_tpz_info->ByteNumber = (uint8_t)
                                        (ps_tpz_info->ByteNumber + 1);
        }
        else
        {
            
            if (skip_size >= (TOPAZ_WRITE_8_DATA_LENGTH - write_index))
            {
                skip_size = (uint16_t)(skip_size - (TOPAZ_WRITE_8_DATA_LENGTH
                            - write_index));
                write_index = (uint16_t)TOPAZ_WRITE_8_DATA_LENGTH;
            }
            else
            {
                ps_tpz_info->ByteNumber = (uint8_t)
                            (ps_tpz_info->ByteNumber + skip_size);
                write_index = (uint16_t)(write_index + skip_size);
                skip_size = 0;
            }            
        }
    }

    if (psNdefMap->ApduBuffIndex == write_len)
    {
        ps_tpz_info->WriteSeq = (uint8_t)WR_DATA;
    }
    else
    {
        if (0 != skip_size)
        {
            ps_tpz_info->WriteSeq = (uint8_t)WR_DATA_READ_REQD;
            
        }
        else
        {
            ps_tpz_info->WriteSeq = (uint8_t)WR_DATA;
        }
    }

#ifdef TOPAZ_RAW_SUPPORT
    *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_WRITE_E8;
#else
    psNdefMap->Cmd.JewelCmd = phHal_eJewel_Write8E;
#endif /* #ifdef TOPAZ_RAW_SUPPORT */
    result = phFriNfc_Tpz_H_NxpWrite (psNdefMap, write_buf, 
                                            sizeof (write_buf));

    return result;

}

static 
NFCSTATUS 
phFriNfc_Tpz_H_UpdateLenFieldValuesAfterRead (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    /* This function is called, only when the LENGTH field has to be updated 
        with the correct value */
    NFCSTATUS                           result = NFCSTATUS_SUCCESS;
    phFriNfc_TopazCont_t                *ps_tpz_info = NULL;
    uint16_t                            write_len = 0;
    uint16_t                            write_index = 0;    
    uint16_t                            byte_addr = 0;
    phFriNfc_Tpz_WrSeq_t                write_seq;
    /* This variable is kept static because if the size to skip LOCK or RESERVED 
    bytes extends to next read then it shall be stored and used to skip the next 
    read the bytes
    */
    static uint16_t                     skip_size = 0;
    uint8_t                             write_buf[TOPAZ_WRITE_8_DATA_LENGTH];
    uint8_t                             exit_while = FALSE;

    ps_tpz_info = &(psNdefMap->TopazContainer);
    write_len = (uint16_t)((psNdefMap->ApduBufferSize < ps_tpz_info->NDEFRWSize) ? 
                psNdefMap->ApduBufferSize : ps_tpz_info->NDEFRWSize);
   
    psNdefMap->State = PH_FRINFC_TOPAZ_STATE_WRITE;

    (void)memcpy ((void *)write_buf, (void *)psNdefMap->SendRecvBuf, 
                    TOPAZ_WRITE_8_DATA_LENGTH);

    write_seq = (phFriNfc_Tpz_WrSeq_t)ps_tpz_info->WriteSeq;

    if (WR_LEN_1_VALUE == write_seq)
    {
        /* First LENGTH field is geting updated, so the skip size 
            reset is done */
        skip_size = 0;
    }

    if (0 != ps_tpz_info->ByteNumber)
    {
        /* Byte Number is not 0, means that some data shall not be overwriteen till 
            that position in the block */
        write_index = (uint16_t)(write_index + ps_tpz_info->ByteNumber); 
    }

    if (0 != skip_size)
    {
        /* This is possible after updating the FIRST length field 
            skip size is skipped because of the pending LOCK or 
            RESERVED bytes
        */
        write_index = (uint16_t)(write_index + skip_size);        
    }

    while ((write_index < TOPAZ_WRITE_8_DATA_LENGTH) && 
        (FALSE == exit_while))
    {
        skip_size = 0;
        /* Get the exact byte address from the block number and 
            byte number */
        byte_addr = TOPAZ_BYTE_ADR_FROM_BLK (ps_tpz_info->CurrentBlock, 
                                            ps_tpz_info->ByteNumber);
        /* Get the skip size */
        skip_size = phFriNfc_Tpz_H_GetSkipSize (psNdefMap, byte_addr);

        if (0 == skip_size)
        {
            switch (write_seq)
            {
                case WR_LEN_1_VALUE:
                {
                    /* First sequenc is always to update 1st LENGTH field of the TLV */
                    if (write_len < 0xFF)
                    {
                        /* This means the LENGTH field is only one BYTE */
                        write_buf[write_index] = (uint8_t)
                                        psNdefMap->ApduBuffIndex;
                        /* Exit the loop */
                        exit_while = TRUE;
                    }
                    else
                    {
                        /* Update the 1st LENGTH field */
                        write_buf[write_index] = (uint8_t)0xFF;                        
                    }
                    break;
                }

                case WR_LEN_2_VALUE:
                {
                    /* Update the 2nd LENGTH field */
                    write_buf[write_index] = (uint8_t)
                                (psNdefMap->ApduBuffIndex >> BYTE_SIZE);
                    break;
                }

                case WR_LEN_3_VALUE:
                {
                    /* Update the 3rd LENGTH field */
                    write_buf[write_index] = (uint8_t)
                                (psNdefMap->ApduBuffIndex & 
                                TOPAZ_BYTE_LENGTH_MASK);
                    /* Exit the loop */
                    exit_while = TRUE;
                    break;
                }                

                default:
                {
                    /* Invalid case */
                    break;
                }
            }
            write_index = (uint16_t)(write_index + 1);
            if (
                /* As the write is done for 8 bytes, the write index cant 
                    go for more than or equal to 8 bytes, if it reaches 8 bytes 
                    then sequence shall not be incrmented */
                (TOPAZ_WRITE_8_DATA_LENGTH != write_index) && 
                /* If the last length field byte is updated then the 
                    write sequence shall not be incremented */
                (WR_LEN_3_VALUE != write_seq) && 
                /* Check added if the write length is less than 0xFF. 
                    If length is less than 0xFF, then write sequence   
                    shall not be incremented */
                (write_len >= 0xFF)
                )
            {
                /* Sequence is incremented to the next level */
                write_seq = (phFriNfc_Tpz_WrSeq_t)(write_seq + 1);
            }
            /* Byte number is incremented */
            ps_tpz_info->ByteNumber = (uint8_t)
                            (ps_tpz_info->ByteNumber + 1);
        }
        else
        {
            
            if (skip_size >= (TOPAZ_WRITE_8_DATA_LENGTH - write_index))
            {
                skip_size = (uint16_t)(skip_size - (TOPAZ_WRITE_8_DATA_LENGTH
                            - write_index));
                write_index = (uint16_t)TOPAZ_WRITE_8_DATA_LENGTH;
            }
            else
            {
                ps_tpz_info->ByteNumber = (uint8_t)
                            (ps_tpz_info->ByteNumber + skip_size);
                write_index = (uint16_t)(write_index + skip_size);
                skip_size = 0;
            }
            
        }
    }

    ps_tpz_info->WriteSeq = (uint8_t)write_seq;

#ifdef TOPAZ_RAW_SUPPORT
    *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_WRITE_E8;
#else
    psNdefMap->Cmd.JewelCmd = phHal_eJewel_Write8E;
#endif /* #ifdef TOPAZ_RAW_SUPPORT */

    result = phFriNfc_Tpz_H_NxpWrite (psNdefMap, write_buf, 
                                            sizeof (write_buf));
    return result;
}



static 
NFCSTATUS 
phFriNfc_Tpz_H_UpdateLenFieldZeroAfterRead (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    /* This function is called, only when the LENGTH field has to be updated 
        with the 0 */
    NFCSTATUS                           result = NFCSTATUS_SUCCESS;
    phFriNfc_TopazCont_t                *ps_tpz_info = NULL;
    uint16_t                            write_len = 0;
    uint16_t                            write_index = 0;
    uint16_t                            prev_apdu_index = 0;
    uint16_t                            byte_addr = 0;
    phFriNfc_Tpz_WrSeq_t                write_seq;
    /* This variable is kept static because if the size to skip LOCK or RESERVED 
        bytes extends to next read then it shall be stored and used to skip the next 
        read bytes
    */
    static uint16_t                     skip_size = 0;
    uint8_t                             write_buf[TOPAZ_WRITE_8_DATA_LENGTH];

    ps_tpz_info = &(psNdefMap->TopazContainer);
    write_len = (uint16_t)((psNdefMap->ApduBufferSize < ps_tpz_info->NDEFRWSize) ? 
                psNdefMap->ApduBufferSize : ps_tpz_info->NDEFRWSize);

    psNdefMap->State = PH_FRINFC_TOPAZ_STATE_WRITE;

    (void)memcpy ((void *)write_buf, (void *)psNdefMap->SendRecvBuf, 
                    TOPAZ_WRITE_8_DATA_LENGTH);

    prev_apdu_index = psNdefMap->ApduBuffIndex;
    write_seq = (phFriNfc_Tpz_WrSeq_t)ps_tpz_info->WriteSeq;

    if (WR_LEN_1_0 == write_seq)
    {
         /* First LENGTH field is geting updated, so the skip size 
            reset is done */
        skip_size = 0;
    }

    if (0 != ps_tpz_info->ByteNumber)
    {
        /* Byte Number is not 0, means that some data shall not be overwriteen till 
            that position in the block */
        write_index = (uint16_t)(write_index + ps_tpz_info->ByteNumber);   
        ps_tpz_info->ByteNumber = 0;
    }

    if (0 != skip_size)
    {
        /* This is possible after updating the FIRST length field 
            skip size is skipped because of the pending LOCK or 
            RESERVED bytes
        */
        write_index = (uint16_t)(write_index + skip_size);        
    }

    while ((write_index < TOPAZ_WRITE_8_DATA_LENGTH) && 
        (write_len != psNdefMap->ApduBuffIndex))
    {
        skip_size = 0;
        byte_addr = TOPAZ_BYTE_ADR_FROM_BLK (ps_tpz_info->CurrentBlock, 
                                            ps_tpz_info->ByteNumber);
        skip_size = phFriNfc_Tpz_H_GetSkipSize (psNdefMap, byte_addr);

        if (0 == skip_size)
        {
            switch (write_seq)
            {
                case WR_LEN_1_0:
                {
                    /* First sequence is always to update 1st LENGTH field 
                        of the TLV */
                    write_buf[write_index] = 0x00;
                    write_index = (uint16_t)(write_index + 1);
                    if (write_len < 0xFF)
                    {
                        /* LENGTH field is only 1 byte, so update change the sequence to 
                            update user data */
                        write_seq = WR_DATA;
                    }
                    else
                    {
                        /* Go to the next LENGTH field to update */
                        write_seq = (phFriNfc_Tpz_WrSeq_t)((TOPAZ_WRITE_8_DATA_LENGTH != 
                                    write_index) ? 
                                    (write_seq + 1) : write_seq);
                    }
                    break;
                }

                case WR_LEN_2_0:
                case WR_LEN_3_0:
                {
                    /* Update 2nd and 3rd LEGNTH field */
                    write_buf[write_index] = 0x00;
                    write_index = (uint16_t)(write_index + 1);
                    write_seq = (phFriNfc_Tpz_WrSeq_t)((TOPAZ_WRITE_8_DATA_LENGTH != 
                                write_index) ? 
                                (write_seq + 1) : write_seq);
                    break;
                }                

                case WR_DATA:
                default:
                {
                    /* Update the buffer by the user data */
                    write_buf[write_index] = 
                            psNdefMap->ApduBuffer[psNdefMap->ApduBuffIndex];
                    
                    write_index = (uint16_t)(write_index + 1);
                    psNdefMap->ApduBuffIndex = (uint16_t)
                                        (psNdefMap->ApduBuffIndex + 1);
                    break;
                }
                
            }
            
            ps_tpz_info->ByteNumber = (uint8_t)
                            (ps_tpz_info->ByteNumber + 1);
        }
        else
        {
            /* LOCK and MEMORY bytes are found */
            if (skip_size >= (TOPAZ_WRITE_8_DATA_LENGTH - write_index))
            {
                /* skip size has exceeded the block number, so calculate the 
                remaining skip size  */
                skip_size = (uint16_t)(skip_size - (TOPAZ_WRITE_8_DATA_LENGTH
                            - write_index));
                write_index = (uint16_t)TOPAZ_WRITE_8_DATA_LENGTH;
            }
            else
            {
                /* skip the LOCK and MEMORY bytes size */
                ps_tpz_info->ByteNumber = (uint8_t)
                            (ps_tpz_info->ByteNumber + skip_size);
                write_index = (uint16_t)(write_index + skip_size);
                skip_size = 0;
            }
        }
    }

    if (psNdefMap->ApduBuffIndex == write_len)
    {
        /* User data has been completely copied and it is ready to write, so 
            change the sequence */
        ps_tpz_info->WriteSeq = (uint8_t)WR_DATA;
    }
    else if ((WR_DATA == write_seq) && (prev_apdu_index == 
        psNdefMap->ApduBuffIndex))
    {
        /* The user data has not been written, only the LENGTH field is 
            updated */
        ps_tpz_info->WriteSeq = (uint8_t)((write_len < 0xFF) ? 
                                WR_LEN_1_0 : WR_LEN_3_0);        
    }
    else
    {
        /*  Update the sequence in the context */
        ps_tpz_info->WriteSeq = (uint8_t)write_seq;
    }

    ps_tpz_info->ByteNumber = 0;

#ifdef TOPAZ_RAW_SUPPORT
    *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_WRITE_E8;
#else
    psNdefMap->Cmd.JewelCmd = phHal_eJewel_Write8E;
#endif /* #ifdef TOPAZ_RAW_SUPPORT */

    result = phFriNfc_Tpz_H_NxpWrite (psNdefMap, write_buf, 
                                            sizeof (write_buf));
    return result;
}

static 
NFCSTATUS 
phFriNfc_Tpz_H_RdForWrite (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    NFCSTATUS                           result = NFCSTATUS_SUCCESS;
    phFriNfc_TopazCont_t                *ps_tpz_info = NULL;
    phFriNfc_Tpz_WrSeq_t                write_seq;
    uint16_t                            byte_addr = 0;
    uint8_t                             exit_while = FALSE;
    uint16_t                            skip_size = 0;

    ps_tpz_info = &(psNdefMap->TopazContainer);
    write_seq = (phFriNfc_Tpz_WrSeq_t)(ps_tpz_info->WriteSeq);

#ifdef TOPAZ_RAW_SUPPORT

    *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_READ8;

#else 

    psNdefMap->Cmd.JewelCmd = phHal_eJewel_Read8;

#endif /* #ifdef TOPAZ_RAW_SUPPORT */
    
    psNdefMap->State = PH_FRINFC_TOPAZ_STATE_RD_FOR_WR_NDEF;

    switch (write_seq)
    {
        case WR_LEN_1_0:
        case WR_LEN_1_VALUE:
        {
            byte_addr = (ps_tpz_info->NdefTLVByteAddress + 1);
            
            /* This loop is to skip the lock amd reserved bytes */
            while (FALSE == exit_while)
            {
                if (TOPAZ_STATIC_LOCK_FIRST_BLOCK_NO == 
                    TOPAZ_BLK_FROM_BYTE_ADR (byte_addr))
                {
                    byte_addr = (uint16_t)(byte_addr + 
                                TOPAZ_STATIC_LOCK_RES_BYTES);
                }
                skip_size = phFriNfc_Tpz_H_GetSkipSize (psNdefMap, 
                                                            byte_addr);
                if (0 != skip_size)
                {
                    byte_addr = (uint16_t)(byte_addr + skip_size);

                    
                }
                else
                {
                    exit_while = TRUE;
                }
            }
            break;
        }

        case WR_LEN_2_0:
        case WR_LEN_3_0:
        case WR_LEN_2_VALUE:
        case WR_LEN_3_VALUE:
        {            
            byte_addr = (uint16_t)TOPAZ_BYTE_ADR_FROM_BLK (ps_tpz_info->CurrentBlock, 
                                                    ps_tpz_info->ByteNumber); 
            /* This loop is for to skip the lock amd reserved bytes */
            while (FALSE == exit_while)
            {
                skip_size = phFriNfc_Tpz_H_GetSkipSize (psNdefMap, 
                                                            byte_addr);
                if (0 != skip_size)
                {
                    byte_addr = (uint16_t)(byte_addr + skip_size);
                }
                else
                {
                    exit_while = TRUE;
                }
            }
            break;
        }

        case WR_DATA_READ_REQD:
        {
            /* Lock or reserved bytes found bytes */
            byte_addr = (uint16_t)TOPAZ_BYTE_ADR_FROM_BLK (ps_tpz_info->CurrentBlock, 
                                                    ps_tpz_info->ByteNumber); 
            break;
        }
        
        default:
        {
            break;
        }
    }

    ps_tpz_info->CurrentBlock = (uint8_t)
                        TOPAZ_BLK_FROM_BYTE_ADR (byte_addr);
    ps_tpz_info->ByteNumber = (uint8_t)
                        TOPAZ_BYTE_OFFSET_FROM_BYTE_ADR (byte_addr);

    result = phFriNfc_Tpz_H_NxpRead (psNdefMap);

    return result;
}

static 
uint16_t 
phFriNfc_Tpz_H_CompareLockBlocks (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint8_t                     block_no, 
    uint16_t                    *p_skip_size)
{
    uint16_t                            return_addr = 0;
    phFriNfc_LockCntrlTLVCont_t         *ps_locktlv_info = NULL;

    ps_locktlv_info = &(psNdefMap->LockTlv);

    if (block_no == ps_locktlv_info->BlkNum)
    {
        /* ps_tpz_info->ByteNumber = (uint8_t)ps_locktlv_info->ByteNum; */
        *p_skip_size = ps_locktlv_info->Size;
        return_addr = ps_locktlv_info->ByteAddr;
    }
   
    return return_addr;
}

static 
uint16_t 
phFriNfc_Tpz_H_CompareMemBlocks (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint8_t                     block_no, 
    uint16_t                    *p_skip_size)
{
    uint16_t                            return_addr = 0;
    phFriNfc_ResMemCntrlTLVCont_t       *ps_memtlv_info = NULL;

    ps_memtlv_info = &(psNdefMap->MemTlv);

    if (block_no == ps_memtlv_info->BlkNum)
    {
        /* ps_tpz_info->ByteNumber = (uint8_t)ps_memtlv_info->ByteNum; */
        *p_skip_size = ps_memtlv_info->Size;
        return_addr = ps_memtlv_info->ByteAddr;
    }

    return return_addr;
}


static 
NFCSTATUS 
phFriNfc_Tpz_H_CopySendWrData (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    NFCSTATUS                           result = NFCSTATUS_SUCCESS;
    phFriNfc_TopazCont_t                *ps_tpz_info = NULL;
    uint8_t                             write_buf[TOPAZ_WRITE_8_DATA_LENGTH];
    uint16_t                            write_len;
    uint8_t                             copy_length;
    uint16_t                            skip_size = 0;

    ps_tpz_info = &(psNdefMap->TopazContainer);
    write_len = (uint16_t)((psNdefMap->ApduBufferSize < ps_tpz_info->NDEFRWSize) ? 
                psNdefMap->ApduBufferSize : ps_tpz_info->NDEFRWSize); 

    if (0 != phFriNfc_Tpz_H_CompareLockBlocks (psNdefMap, 
                ps_tpz_info->CurrentBlock, &skip_size))
    {
        ps_tpz_info->WriteSeq = (uint8_t)WR_DATA_READ_REQD;
        ps_tpz_info->ByteNumber = 0;
        result = phFriNfc_Tpz_H_RdForWrite (psNdefMap);
    }
    else if (0 != phFriNfc_Tpz_H_CompareMemBlocks (psNdefMap, 
                ps_tpz_info->CurrentBlock, &skip_size))
    {
        ps_tpz_info->WriteSeq = (uint8_t)WR_DATA_READ_REQD;
        ps_tpz_info->ByteNumber = 0;
        result = phFriNfc_Tpz_H_RdForWrite (psNdefMap);
    }
    else
    {
#ifdef TOPAZ_RAW_SUPPORT
        *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_WRITE_E8;
#else
        psNdefMap->Cmd.JewelCmd = phHal_eJewel_Write8E;
#endif /* #ifdef TOPAZ_RAW_SUPPORT */
        psNdefMap->State = (uint8_t)PH_FRINFC_TOPAZ_STATE_WRITE;

        if ((write_len - psNdefMap->ApduBuffIndex) >= (uint16_t)TOPAZ_WRITE_8_DATA_LENGTH)
        {
            copy_length = (uint8_t)TOPAZ_WRITE_8_DATA_LENGTH;
            (void)memcpy ((void *)write_buf, 
                    (void *)(psNdefMap->ApduBuffer + psNdefMap->ApduBuffIndex), 
                    copy_length);

            psNdefMap->ApduBuffIndex = (uint16_t)(psNdefMap->ApduBuffIndex + 
                                        copy_length);
        }
        else
        {
            copy_length = (uint8_t)(write_len - psNdefMap->ApduBuffIndex);

            (void)memcpy ((void *)write_buf, 
                    (void *)(psNdefMap->ApduBuffer + psNdefMap->ApduBuffIndex), 
                    TOPAZ_WRITE_8_DATA_LENGTH);

            psNdefMap->ApduBuffIndex = (uint16_t)(psNdefMap->ApduBuffIndex + 
                                        copy_length);

            (void)memset ((void *)(write_buf + copy_length), 0x00, 
                        (TOPAZ_WRITE_8_DATA_LENGTH - copy_length));
        }

#ifdef TOPAZ_RAW_SUPPORT
        *psNdefMap->SendRecvBuf = PH_FRINFC_TOPAZ_CMD_WRITE_E8;
#else
        psNdefMap->Cmd.JewelCmd = phHal_eJewel_Write8E;
#endif /* #ifdef TOPAZ_RAW_SUPPORT */

        result = phFriNfc_Tpz_H_NxpWrite (psNdefMap, write_buf, 
                                            sizeof (write_buf));
    }


    return result;
}


static 
NFCSTATUS 
phFriNfc_Tpz_H_ActualCardSize (
    phFriNfc_NdefMap_t          *psNdefMap)
{
    NFCSTATUS                           result = NFCSTATUS_SUCCESS;
    phFriNfc_TopazCont_t                *ps_tpz_info = NULL;
    phFriNfc_LockCntrlTLVCont_t         *ps_locktlv_info = NULL;
    phFriNfc_ResMemCntrlTLVCont_t       *ps_memtlv_info = NULL;
    uint16_t                            ndef_value_byte_addr = 0;
    uint16_t                            ndef_read_write_size = 0;

    ps_tpz_info = &(psNdefMap->TopazContainer);
    if (ps_tpz_info->ActualNDEFMsgSize > ps_tpz_info->RemainingSize)
    {
        ps_tpz_info->ActualNDEFMsgSize = 0;
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                            NFCSTATUS_NO_NDEF_SUPPORT);
    }
    else
    {
        ndef_read_write_size = ps_tpz_info->RemainingSize;
        ndef_value_byte_addr = phFriNfc_Tpz_H_GetNDEFValueFieldAddrForRead 
                                (psNdefMap);
        
        ps_locktlv_info = &(psNdefMap->LockTlv);
        if (ps_locktlv_info->ByteAddr > ndef_value_byte_addr)
        {
            ndef_read_write_size = (ndef_read_write_size -  
                                    ps_locktlv_info->Size);
        }

        ps_memtlv_info = &(psNdefMap->MemTlv);
        if (ps_memtlv_info->ByteAddr > ndef_value_byte_addr)
        {
            ndef_read_write_size = (ndef_read_write_size -  
                                    ps_memtlv_info->Size);
        }

        if (ps_tpz_info->ActualNDEFMsgSize > ndef_read_write_size)
        {
            ps_tpz_info->ActualNDEFMsgSize = 0;
            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                NFCSTATUS_NO_NDEF_SUPPORT);
        }
        else
        {
            ps_tpz_info->NDEFRWSize = (uint16_t)
                            ((ps_tpz_info->ActualNDEFMsgSize < 0xFF) ? 
                            (ndef_read_write_size - 2) : 
                            ndef_read_write_size);
        }
    }

    return result;
}

static 
NFCSTATUS 
phFriNfc_Tpz_H_ParseLockTLVType (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint8_t                     *p_parse_data, 
    uint16_t                    *p_parse_index, 
    uint16_t                    total_len_to_parse, 
    phFriNfc_Tpz_ParseSeq_t     *seq_to_execute)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    uint16_t                    parse_index = *p_parse_index;
    phFriNfc_Tpz_ParseSeq_t     expected_seq = *seq_to_execute;

    PHNFC_UNUSED_VARIABLE(psNdefMap);
    PHNFC_UNUSED_VARIABLE(total_len_to_parse);

    switch (p_parse_data[parse_index])
    {
        case PH_FRINFC_TOPAZ_LOCK_CTRL_T:
        {
            expected_seq = LOCK_L_TLV;
            parse_index = (parse_index + 1);
            break;
        }

        case PH_FRINFC_TOPAZ_NULL_T:
        {
            expected_seq = LOCK_T_TLV;
            parse_index = (parse_index + 1);
            break;
        }

        default:
        {                        
            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                NFCSTATUS_NO_NDEF_SUPPORT);
            break;
        }
    }
    
    
    *seq_to_execute = expected_seq;
    *p_parse_index = parse_index;
    return result;
}

static 
NFCSTATUS 
phFriNfc_Tpz_H_ParseMemTLVType (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint8_t                     *p_parse_data, 
    uint16_t                    *p_parse_index, 
    uint16_t                    total_len_to_parse, 
    phFriNfc_Tpz_ParseSeq_t     *seq_to_execute)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    uint16_t                    parse_index = *p_parse_index;
    phFriNfc_Tpz_ParseSeq_t     expected_seq = *seq_to_execute;

    PHNFC_UNUSED_VARIABLE(psNdefMap);
    PHNFC_UNUSED_VARIABLE(total_len_to_parse);

    switch (p_parse_data[parse_index])
    {
        case PH_FRINFC_TOPAZ_LOCK_CTRL_T:
        {
            expected_seq = LOCK_L_TLV;
            parse_index = (parse_index + 1);
            break;
        }

        case PH_FRINFC_TOPAZ_NULL_T:
        {
            expected_seq = MEM_T_TLV;
            parse_index = (parse_index + 1);
            break;
        }

        case PH_FRINFC_TOPAZ_MEM_CTRL_T:
        {
            expected_seq = MEM_L_TLV;
            parse_index = (parse_index + 1);
            break;
        }

        default:
        {                        
            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                NFCSTATUS_NO_NDEF_SUPPORT);
            break;
        }
    }
    
    *seq_to_execute = expected_seq;
    *p_parse_index = parse_index;
    return result;
}

static 
NFCSTATUS 
phFriNfc_Tpz_H_ParseNdefTLVType (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint8_t                     *p_parse_data, 
    uint16_t                    *p_parse_index, 
    uint16_t                    total_len_to_parse, 
    phFriNfc_Tpz_ParseSeq_t     *seq_to_execute)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    uint16_t                    parse_index = *p_parse_index;
    phFriNfc_Tpz_ParseSeq_t     expected_seq = *seq_to_execute;

    PHNFC_UNUSED_VARIABLE(psNdefMap);
    PHNFC_UNUSED_VARIABLE(total_len_to_parse);

    switch (p_parse_data[parse_index])
    {
        case PH_FRINFC_TOPAZ_MEM_CTRL_T:
        {
            /* TYPE field of Memory control TLV is found. 
                This means that more than one memory control 
                TLV exists */
            expected_seq = MEM_L_TLV;
            parse_index = (parse_index + 1);
            break;
        }

        case PH_FRINFC_TOPAZ_NULL_T:
        {
            /* Skip the NULL TLV */
            expected_seq = NDEF_T_TLV;
            parse_index = (parse_index + 1);
            break;
        }

        case PH_FRINFC_TOPAZ_NDEF_T:
        {
            /* TYPE field of NDEF TLV found, so next expected
                sequence is LENGTH field */
            expected_seq = NDEF_L_TLV;
            parse_index = (parse_index + 1);
            break;
        }

        default:
        {
            /* Reset the sequence */
            expected_seq = LOCK_T_TLV;
            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                NFCSTATUS_NO_NDEF_SUPPORT);
            break;
        }
    }
    
    *seq_to_execute = expected_seq;
    *p_parse_index = parse_index;
    return result;
}

static 
uint16_t
phFriNfc_Tpz_H_GetSkipSize (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint16_t                    byte_adr_card)
{
    uint16_t                        return_size = 0;
    phFriNfc_LockCntrlTLVCont_t     *ps_locktlv_info = NULL;
    phFriNfc_ResMemCntrlTLVCont_t   *ps_memtlv_info = NULL;

    ps_locktlv_info = &(psNdefMap->LockTlv);
    ps_memtlv_info = &(psNdefMap->MemTlv);

    /* If there are more than one LOCK CONTROL TLV, then 
    ADD A LOOP HERE OF THE NUMBER OF TLVs FOUND */
    if (byte_adr_card == ps_locktlv_info->ByteAddr)
    {
        return_size = ps_locktlv_info->Size;
    }

    /* If there are more than one MEMORY CONTROL TLV, then 
        ADD A LOOP HERE OF THE NUMBER OF TLVs FOUND */
    if (byte_adr_card == ps_memtlv_info->ByteAddr)
    {
        return_size = ps_memtlv_info->Size;
    }
    return return_size;
}

static 
NFCSTATUS 
phFriNfc_Tpz_H_GetLockBytesInfo (
    phFriNfc_NdefMap_t          *psNdefMap, 
    uint8_t                     *p_lock_info)
{
    NFCSTATUS                       result = NFCSTATUS_SUCCESS;
    phFriNfc_LockCntrlTLVCont_t     *ps_locktlv_info = NULL;
    uint8_t                         page_address = 0;
    uint8_t                         bytes_offset = 0;
    uint8_t                         lock_index = 0;

    ps_locktlv_info = &(psNdefMap->LockTlv);

    page_address = (uint8_t)(p_lock_info[lock_index] >> NIBBLE_SIZE);
    bytes_offset = (uint8_t)(p_lock_info[lock_index] & TOPAZ_NIBBLE_MASK);

    lock_index = (lock_index + 1);
    ps_locktlv_info->Size = (uint16_t)
                            (((p_lock_info[lock_index] % TOPAZ_BYTE_SIZE_IN_BITS) > 0)? 
                            ((p_lock_info[lock_index] / TOPAZ_BYTE_SIZE_IN_BITS) + 1) : 
                            (p_lock_info[lock_index] / TOPAZ_BYTE_SIZE_IN_BITS));

    lock_index = (lock_index + 1);
    ps_locktlv_info->BytesPerPage = 
                            (p_lock_info[lock_index] & TOPAZ_NIBBLE_MASK);
    ps_locktlv_info->BytesLockedPerLockBit = 
                            (p_lock_info[lock_index] >> NIBBLE_SIZE);

    /* Apply the formula to calculate byte address 
        ByteAddr = PageAddr*2^BytesPerPage + ByteOffset
    */
    ps_locktlv_info->ByteAddr = (uint16_t)((page_address 
                                * (1 << ps_locktlv_info->BytesPerPage))
                                + bytes_offset);

    
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

    return result;
}

static 
NFCSTATUS 
phFriNfc_Tpz_H_GetMemBytesInfo (
    phFriNfc_NdefMap_t          *psNdefMap,      
    uint8_t                     *p_mem_info)
{
    NFCSTATUS                       result = NFCSTATUS_SUCCESS;
    phFriNfc_ResMemCntrlTLVCont_t   *ps_memtlv_info = NULL;
    phFriNfc_LockCntrlTLVCont_t     *ps_locktlv_info = NULL;
    uint8_t                         page_address = 0;
    uint8_t                         bytes_offset = 0;
    uint8_t                         mem_index = 0;

    ps_memtlv_info = &(psNdefMap->MemTlv);
    ps_locktlv_info = &(psNdefMap->LockTlv);
    page_address = (uint8_t)(p_mem_info[mem_index] >> NIBBLE_SIZE);
    bytes_offset = (uint8_t)(p_mem_info[mem_index] & TOPAZ_NIBBLE_MASK);

    mem_index = (mem_index + 1);
    ps_memtlv_info->Size = (uint16_t)p_mem_info[mem_index];

    mem_index = (mem_index + 1);
    ps_memtlv_info->BytesPerPage = 
                            (p_mem_info[mem_index] & TOPAZ_NIBBLE_MASK);

    /* Apply the formula to calculate byte address 
        ByteAddr = PageAddr * 2^BytesPerPage + ByteOffset
    */
    ps_memtlv_info->ByteAddr = (uint16_t)((page_address 
                            * (1 << ps_memtlv_info->BytesPerPage))
                            + bytes_offset);

    
    if (
        /* Check if the lock and memory bytes are overlapped */
        ((ps_memtlv_info->ByteAddr >= ps_locktlv_info->ByteAddr) &&
        (ps_memtlv_info->ByteAddr <= 
        (ps_locktlv_info->ByteAddr + ps_locktlv_info->Size - 1))) || 

        (((ps_memtlv_info->ByteAddr + ps_memtlv_info->Size - 1) >= 
        ps_locktlv_info->ByteAddr) &&
        ((ps_memtlv_info->ByteAddr + ps_memtlv_info->Size - 1) <= 
        (ps_locktlv_info->ByteAddr + ps_locktlv_info->Size - 1))) || 

        /* Check the static lock and reserved areas memory blocks */
        ((ps_memtlv_info->ByteAddr >= TOPAZ_STATIC_LOCK_RES_START) && 
        (ps_memtlv_info->ByteAddr < TOPAZ_STATIC_LOCK_RES_END)) || 
        (((ps_memtlv_info->ByteAddr + ps_memtlv_info->Size - 1) >= 
        TOPAZ_STATIC_LOCK_RES_START) && 
        ((ps_memtlv_info->ByteAddr + ps_memtlv_info->Size - 1) < 
        TOPAZ_STATIC_LOCK_RES_END)) ||

        /* Check if the memory address is out bound */
        ((ps_locktlv_info->ByteAddr + ps_locktlv_info->Size) > 
        (uint16_t)(psNdefMap->TopazContainer.CCByteBuf[2] * 
        TOPAZ_BYTES_PER_BLOCK))
        )
    {
        ps_memtlv_info->ByteAddr = 0;
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                            NFCSTATUS_NO_NDEF_SUPPORT);
    }
    else
    {
        ps_memtlv_info->BlkNum = (ps_memtlv_info->ByteAddr / 
                                    TOPAZ_BYTES_PER_BLOCK);
        ps_memtlv_info->ByteNum = (ps_memtlv_info->ByteAddr % 
                                    TOPAZ_BYTES_PER_BLOCK);
    }

    return result;
}

#ifdef UNIT_TEST
#include <phUnitTestNfc_TopazDynamic_static.c>
#endif

#endif  /*#if !(defined(PH_FRINFC_MAP_TOPAZ_DISABLED ) || defined (PH_FRINFC_MAP_TOPAZ_DYNAMIC_DISABLED ))*/



