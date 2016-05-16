/*
 *
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
* \file  phFriNfc_ISO15693Map.c
* \brief This component encapsulates read/write/check ndef/process functionalities,
*        for the ISO-15693 Card. 
*
* Project: NFC-FRI
*
* $Date: $
* $Author: ing02260 $
* $Revision: $
* $Aliases:  $
*
*/

#ifndef PH_FRINFC_MAP_ISO15693_DISABLED

#include <phNfcTypes.h>
#include <phNfcConfig.h>
#include <phNfcInterface.h>
#include <phNfcHalTypes.h>
#include <phFriNfc.h>
#include <phFriNfc_NdefMap.h>
#include <phFriNfc_OvrHal.h>
#include <phFriNfc_MapTools.h>
#include <phFriNfc_ISO15693Map.h>

/************************** START DATA STRUCTURE *********************/

typedef enum phFriNfc_eChkNdefSeq
{
    ISO15693_NDEF_TLV_T, 
    ISO15693_NDEF_TLV_L, 
    ISO15693_NDEF_TLV_V, 
    ISO15693_PROP_TLV_L, 
    ISO15693_PROP_TLV_V

}phFriNfc_eChkNdefSeq_t;

typedef enum phFriNfc_eWrNdefSeq
{
    ISO15693_RD_BEFORE_WR_NDEF_L_0,
    ISO15693_WRITE_DATA, 
    ISO15693_RD_BEFORE_WR_NDEF_L,
    ISO15693_WRITE_NDEF_TLV_L

}phFriNfc_eWrNdefSeq_t;

#ifdef FRINFC_READONLY_NDEF

typedef enum phFriNfc_eRONdefSeq
{
    ISO15693_RD_BEFORE_WR_CC,
    ISO15693_WRITE_CC, 
    ISO15693_LOCK_BLOCK

}phFriNfc_eRONdefSeq_t;

#endif /* #ifdef FRINFC_READONLY_NDEF */

/************************** END DATA STRUCTURE *********************/

/************************** START MACROS definition *********************/




/* UID bytes to differentiate ICODE cards */
#define ISO15693_UID_BYTE_4                 0x04U
#define ISO15693_UID_BYTE_5                 0x05U
#define ISO15693_UID_BYTE_6                 0x06U
#define ISO15693_UID_BYTE_7                 0x07U

/* UID 7th byte value shall be 0xE0 */
#define ISO15693_UIDBYTE_7_VALUE            0xE0U
/* UID 6th byte value shall be 0x04 - NXP manufacturer */
#define ISO15693_UIDBYTE_6_VALUE            0x04U


/* UID value for 
    SL2 ICS20 
    SL2S2002 
    */
#define ISO15693_UIDBYTE_5_VALUE_SLI_X      0x01U
/* Card size SL2 ICS20 / SL2S2002 */
#define ISO15693_SL2_S2002_ICS20            112U 

/* UID value for 
    SL2 ICS53,          
    SL2 ICS54  
    SL2S5302 
*/
#define ISO15693_UIDBYTE_5_VALUE_SLI_X_S    0x02U
#define ISO15693_UIDBYTE_4_VALUE_SLI_X_S    0x00U
#define ISO15693_UIDBYTE_4_VALUE_SLI_X_SHC  0x80U
#define ISO15693_UIDBYTE_4_VALUE_SLI_X_SY   0x40U
/* SL2 ICS53, SL2 ICS54 and SL2S5302 */
#define ISO15693_SL2_S5302_ICS53_ICS54      160U

/* UID value for 
    SL2 ICS50       
    SL2 ICS51
    SL2S5002 
*/
#define ISO15693_UIDBYTE_5_VALUE_SLI_X_L    0x03U
#define ISO15693_UIDBYTE_4_VALUE_SLI_X_L    0x00U
#define ISO15693_UIDBYTE_4_VALUE_SLI_X_LHC  0x80U
/* SL2 ICS50, SL2 ICS51 and SL2S5002 */
#define ISO15693_SL2_S5002_ICS50_ICS51      32U


/* State Machine declaration
CHECK NDEF state */
#define ISO15693_CHECK_NDEF                 0x01U
/* READ NDEF state */
#define ISO15693_READ_NDEF                  0x02U
/* WRITE NDEF state */
#define ISO15693_WRITE_NDEF                 0x03U
#ifdef FRINFC_READONLY_NDEF

    /* READ ONLY NDEF state */
    #define ISO15693_READ_ONLY_NDEF         0x04U

    /* READ ONLY MASK byte for CC */
    #define ISO15693_CC_READ_ONLY_MASK      0x03U

    /* CC READ WRITE index */
    #define ISO15693_RW_BTYE_INDEX          0x01U

    /* LOCK BLOCK command */
    #define ISO15693_LOCK_BLOCK_CMD         0x22U

#endif /* #ifdef FRINFC_READONLY_NDEF */

/* CC Bytes 
Magic number */
#define ISO15693_CC_MAGIC_BYTE              0xE1U
/* Expected mapping version */
#define ISO15693_MAPPING_VERSION            0x01U
/* Major version is in upper 2 bits */
#define ISO15693_MAJOR_VERSION_MASK         0xC0U

/* CC indicating tag is capable of multi-block read */
#define ISO15693_CC_USE_MBR                 0x01U
/* CC indicating tag is capable of inventory page read */
#define ISO15693_CC_USE_IPR                 0x02U
/* EXTRA byte in the response */
#define ISO15693_EXTRA_RESP_BYTE            0x01U

/* Maximum card size multiplication factor */
#define ISO15693_MULT_FACTOR                0x08U
/* NIBBLE mask for READ WRITE access */
#define ISO15693_LSB_NIBBLE_MASK            0x0FU
#define ISO15693_RD_WR_PERMISSION           0x00U
#define ISO15693_RD_ONLY_PERMISSION         0x03U

/* READ command identifier */
#define ISO15693_READ_COMMAND               0x20U

/* READ multiple command identifier */
#define ISO15693_READ_MULTIPLE_COMMAND      0x23U

/* INVENTORY pageread command identifier */
#define ICODE_INVENTORY_PAGEREAD_COMMAND    0xB0U
#define INVENTORY_PAGEREAD_FLAGS            0x24U
#define NXP_MANUFACTURING_CODE              0x04U

/* WRITE command identifier */
#define ISO15693_WRITE_COMMAND              0x21U
/* FLAG option */
#define ISO15693_FLAGS                      0x20U

/* RESPONSE length expected for single block READ */
#define ISO15693_SINGLE_BLK_RD_RESP_LEN     0x04U
/* NULL TLV identifier */
#define ISO15693_NULL_TLV_ID                0x00U
/* NDEF TLV, TYPE identifier  */
#define ISO15693_NDEF_TLV_TYPE_ID           0x03U

/* 8 BIT shift */
#define ISO15693_BTYE_SHIFT                 0x08U

/* Proprietary TLV TYPE identifier */
#define ISO15693_PROP_TLV_ID                0xFDU

/* CC SIZE in BYTES */
#define ISO15693_CC_SIZE                    0x04U

/* To get the remaining size in the card. 
Inputs are 
1. maximum data size 
2. block number 
3. index of the block number */
#define ISO15693_GET_REMAINING_SIZE(max_data_size, blk, index) \
    (max_data_size - ((blk * ISO15693_BYTES_PER_BLOCK) + index))

#define ISO15693_GET_LEN_FIELD_BLOCK_NO(blk, byte_addr, ndef_size) \
    (((byte_addr + ((ndef_size >= ISO15693_THREE_BYTE_LENGTH_ID) ? 3 : 1)) > \
    (ISO15693_BYTES_PER_BLOCK - 1)) ? (blk + 1) : blk)

#define ISO15693_GET_LEN_FIELD_BYTE_NO(blk, byte_addr, ndef_size) \
    (((byte_addr + ((ndef_size >= ISO15693_THREE_BYTE_LENGTH_ID) ? 3 : 1)) % \
    ISO15693_BYTES_PER_BLOCK))
    


/************************** END MACROS definition *********************/

/************************** START static functions declaration *********************/
static
NFCSTATUS 
phFriNfc_ISO15693_H_ProcessReadOnly (
    phFriNfc_NdefMap_t      *psNdefMap);

static 
NFCSTATUS 
phFriNfc_ISO15693_H_ProcessWriteNdef (
    phFriNfc_NdefMap_t      *psNdefMap);

static 
NFCSTATUS 
phFriNfc_ISO15693_H_ProcessReadNdef (
    phFriNfc_NdefMap_t      *psNdefMap);

static 
NFCSTATUS 
phFriNfc_ISO15693_H_ProcessCheckNdef (
    phFriNfc_NdefMap_t      *psNdefMap);

static 
void 
phFriNfc_ISO15693_H_Complete (
    phFriNfc_NdefMap_t      *psNdefMap,
    NFCSTATUS               Status);

static 
NFCSTATUS 
phFriNfc_ISO15693_H_ReadWrite (
    phFriNfc_NdefMap_t      *psNdefMap, 
    uint8_t                 command, 
    uint8_t                 *p_data, 
    uint8_t                 data_length);

static
NFCSTATUS
phFriNfc_ReadRemainingInMultiple (
    phFriNfc_NdefMap_t  *psNdefMap,
    uint32_t            startBlock);

/************************** END static functions declaration *********************/

/************************** START static functions definition *********************/

static 
NFCSTATUS 
phFriNfc_ISO15693_H_ProcessWriteNdef (
    phFriNfc_NdefMap_t      *psNdefMap)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    phFriNfc_ISO15693Cont_t     *ps_iso_15693_con = 
                                &(psNdefMap->ISO15693Container);
    phFriNfc_eWrNdefSeq_t       e_wr_ndef_seq = (phFriNfc_eWrNdefSeq_t)
                                psNdefMap->ISO15693Container.ndef_seq;
    uint8_t                     *p_recv_buf = NULL;
    uint8_t                     recv_length = 0;
    uint8_t                     write_flag = FALSE;
    uint8_t                     a_write_buf[ISO15693_BYTES_PER_BLOCK] = {0};
    uint8_t                     remaining_size = 0;

    switch (e_wr_ndef_seq)
    {
        case ISO15693_RD_BEFORE_WR_NDEF_L_0:
        {
            /* L byte is read  */
            p_recv_buf = (psNdefMap->SendRecvBuf + ISO15693_EXTRA_RESP_BYTE);
            recv_length = (uint8_t)
                        (*psNdefMap->SendRecvLength - ISO15693_EXTRA_RESP_BYTE);

            if (ISO15693_SINGLE_BLK_RD_RESP_LEN == recv_length)
            {
                /* Response length is correct */
                uint8_t     byte_index = 0;

                /* Copy the recevied buffer */
                (void)memcpy ((void *)a_write_buf, (void *)p_recv_buf, 
                                recv_length);

                byte_index = ISO15693_GET_LEN_FIELD_BYTE_NO(
                        ps_iso_15693_con->ndef_tlv_type_blk, 
                        ps_iso_15693_con->ndef_tlv_type_byte, 
                        psNdefMap->ApduBufferSize);

                /* Writing length field to 0, Update length field to 0 */
                *(a_write_buf + byte_index) = 0x00;
                
                if ((ISO15693_BYTES_PER_BLOCK - 1) != byte_index)
                {
                    /* User data is updated in the buffer */
                    byte_index = (uint8_t)(byte_index + 1);
                    /* Block number shall be udate */
                    remaining_size = (ISO15693_BYTES_PER_BLOCK - byte_index);

                    if ((psNdefMap->ApduBufferSize - psNdefMap->ApduBuffIndex) 
                        < remaining_size)
                    {
                        remaining_size = (uint8_t)(psNdefMap->ApduBufferSize - 
                                                    psNdefMap->ApduBuffIndex);
                    }

                    /* Go to next byte to fill the write buffer */
                    (void)memcpy ((void *)(a_write_buf + byte_index), 
                                (void *)(psNdefMap->ApduBuffer + 
                                psNdefMap->ApduBuffIndex), remaining_size);

                    /* Write index updated */
                    psNdefMap->ApduBuffIndex = (uint8_t)(psNdefMap->ApduBuffIndex + 
                                                remaining_size);                    
                }
                
                /* After this write, user data can be written. 
                Update the sequence accordingly */
                e_wr_ndef_seq = ISO15693_WRITE_DATA;
                write_flag = TRUE;
            } /* if (ISO15693_SINGLE_BLK_RD_RESP_LEN == recv_length) */
            else
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            break;
        } /* case ISO15693_RD_BEFORE_WR_NDEF_L_0: */

        case ISO15693_RD_BEFORE_WR_NDEF_L:
        {
            p_recv_buf = (psNdefMap->SendRecvBuf + ISO15693_EXTRA_RESP_BYTE);
            recv_length = (uint8_t)(*psNdefMap->SendRecvLength - 
                            ISO15693_EXTRA_RESP_BYTE);

            if (ISO15693_SINGLE_BLK_RD_RESP_LEN == recv_length)
            {
                uint8_t     byte_index = 0;

                (void)memcpy ((void *)a_write_buf, (void *)p_recv_buf, 
                                recv_length);
                
                byte_index = ISO15693_GET_LEN_FIELD_BYTE_NO(
                                ps_iso_15693_con->ndef_tlv_type_blk, 
                                ps_iso_15693_con->ndef_tlv_type_byte, 
                                psNdefMap->ApduBuffIndex);

                *(a_write_buf + byte_index) = (uint8_t)psNdefMap->ApduBuffIndex;
                e_wr_ndef_seq = ISO15693_WRITE_NDEF_TLV_L;
                write_flag = TRUE;
            }
            else
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            break;
        }

        case ISO15693_WRITE_DATA:
        {
            if ((psNdefMap->ApduBufferSize == psNdefMap->ApduBuffIndex) 
                || (ps_iso_15693_con->current_block == 
                    (ps_iso_15693_con->max_data_size / ISO15693_BYTES_PER_BLOCK)))
            { 
                ps_iso_15693_con->current_block = 
                        ISO15693_GET_LEN_FIELD_BLOCK_NO(
                        ps_iso_15693_con->ndef_tlv_type_blk, 
                        ps_iso_15693_con->ndef_tlv_type_byte, 
                        psNdefMap->ApduBuffIndex);
                e_wr_ndef_seq = ISO15693_RD_BEFORE_WR_NDEF_L;
            }            
            else
            {
                remaining_size = ISO15693_BYTES_PER_BLOCK;

                ps_iso_15693_con->current_block = (uint16_t)
                                    (ps_iso_15693_con->current_block + 1);
                
                if ((psNdefMap->ApduBufferSize - psNdefMap->ApduBuffIndex) 
                    < remaining_size)
                {
                    remaining_size = (uint8_t)(psNdefMap->ApduBufferSize - 
                                                psNdefMap->ApduBuffIndex);
                }

                (void)memcpy ((void *)a_write_buf, (void *)
                                (psNdefMap->ApduBuffer + 
                                psNdefMap->ApduBuffIndex), remaining_size);

                psNdefMap->ApduBuffIndex = (uint8_t)(psNdefMap->ApduBuffIndex + 
                                                remaining_size);
                write_flag = TRUE;
            } 
            break;
        } /* case ISO15693_WRITE_DATA: */

        case ISO15693_WRITE_NDEF_TLV_L:
        {
            *psNdefMap->WrNdefPacketLength = psNdefMap->ApduBuffIndex;
            ps_iso_15693_con->actual_ndef_size = psNdefMap->ApduBuffIndex;
            break;
        }

        default:
        {
            break;
        }
    } /* switch (e_wr_ndef_seq) */

    if (((0 == psNdefMap->ApduBuffIndex) 
        || (*psNdefMap->WrNdefPacketLength != psNdefMap->ApduBuffIndex))
        && (!result))
    {
        if (FALSE == write_flag)
        {
            result = phFriNfc_ISO15693_H_ReadWrite (psNdefMap, 
                                    ISO15693_READ_COMMAND, NULL, 0);
        }
        else
        {
            result = phFriNfc_ISO15693_H_ReadWrite (psNdefMap, 
                                        ISO15693_WRITE_COMMAND, 
                                        a_write_buf, sizeof (a_write_buf));
        }
    }

    psNdefMap->ISO15693Container.ndef_seq = (uint8_t)e_wr_ndef_seq;
    return result;
}

static 
NFCSTATUS 
phFriNfc_ISO15693_H_ReadWrite (
    phFriNfc_NdefMap_t      *psNdefMap, 
    uint8_t                 command, 
    uint8_t                 *p_data, 
    uint8_t                 data_length)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    uint8_t                     send_index = 0;

    /* set the data for additional data exchange*/
    psNdefMap->psDepAdditionalInfo.DepFlags.MetaChaining = 0;
    psNdefMap->psDepAdditionalInfo.DepFlags.NADPresent = 0;
    psNdefMap->psDepAdditionalInfo.NAD = 0;

    psNdefMap->MapCompletionInfo.CompletionRoutine = phFriNfc_ISO15693_Process;
    psNdefMap->MapCompletionInfo.Context = psNdefMap;

    *psNdefMap->SendRecvLength = psNdefMap->TempReceiveLength;

    psNdefMap->Cmd.Iso15693Cmd = phHal_eIso15693_Cmd;

    *(psNdefMap->SendRecvBuf + send_index) = (uint8_t)ISO15693_FLAGS;
    send_index = (uint8_t)(send_index + 1);

    *(psNdefMap->SendRecvBuf + send_index) = (uint8_t)command;
    send_index = (uint8_t)(send_index + 1);

    (void)memcpy ((void *)(psNdefMap->SendRecvBuf + send_index), 
        (void *)psNdefMap->psRemoteDevInfo->RemoteDevInfo.Iso15693_Info.Uid, 
        psNdefMap->psRemoteDevInfo->RemoteDevInfo.Iso15693_Info.UidLength);
    send_index = (uint8_t)(send_index + 
                psNdefMap->psRemoteDevInfo->RemoteDevInfo.Iso15693_Info.UidLength);

    *(psNdefMap->SendRecvBuf + send_index) = (uint8_t)
                                psNdefMap->ISO15693Container.current_block;
    send_index = (uint8_t)(send_index + 1);

    if ((ISO15693_WRITE_COMMAND == command) ||
        (ISO15693_READ_MULTIPLE_COMMAND == command))
    {
        (void)memcpy ((void *)(psNdefMap->SendRecvBuf + send_index), 
                    (void *)p_data, data_length);
        send_index = (uint8_t)(send_index + data_length);
    }

    psNdefMap->SendLength = send_index;
    result = phFriNfc_OvrHal_Transceive(psNdefMap->LowerDevice,
                                        &psNdefMap->MapCompletionInfo,
                                        psNdefMap->psRemoteDevInfo,
                                        psNdefMap->Cmd,
                                        &psNdefMap->psDepAdditionalInfo,
                                        psNdefMap->SendRecvBuf,
                                        psNdefMap->SendLength,
                                        psNdefMap->SendRecvBuf,
                                        psNdefMap->SendRecvLength);

    return result;
}

static 
NFCSTATUS 
phFriNfc_ISO15693_H_Inventory_Page_Read (
    phFriNfc_NdefMap_t      *psNdefMap, 
    uint8_t                 command, 
    uint8_t                 page,
    uint8_t                 numPages)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    uint8_t                     send_index = 0;

    /* set the data for additional data exchange*/
    psNdefMap->psDepAdditionalInfo.DepFlags.MetaChaining = 0;
    psNdefMap->psDepAdditionalInfo.DepFlags.NADPresent = 0;
    psNdefMap->psDepAdditionalInfo.NAD = 0;

    psNdefMap->MapCompletionInfo.CompletionRoutine = phFriNfc_ISO15693_Process;
    psNdefMap->MapCompletionInfo.Context = psNdefMap;

    *psNdefMap->SendRecvLength = psNdefMap->TempReceiveLength;

    psNdefMap->Cmd.Iso15693Cmd = phHal_eIso15693_Cmd;

    *(psNdefMap->SendRecvBuf + send_index) = INVENTORY_PAGEREAD_FLAGS;
    send_index = (uint8_t)(send_index + 1);

    *(psNdefMap->SendRecvBuf + send_index) = (uint8_t)command;
    send_index = (uint8_t)(send_index + 1);

    *(psNdefMap->SendRecvBuf + send_index) = NXP_MANUFACTURING_CODE;
    send_index = (uint8_t)(send_index + 1);

    *(psNdefMap->SendRecvBuf + send_index) = 0x40;
    send_index = (uint8_t)(send_index + 1);

    (void)memcpy ((void *)(psNdefMap->SendRecvBuf + send_index), 
        (void *)psNdefMap->psRemoteDevInfo->RemoteDevInfo.Iso15693_Info.Uid, 
        psNdefMap->psRemoteDevInfo->RemoteDevInfo.Iso15693_Info.UidLength);
    send_index = (uint8_t)(send_index + 
                psNdefMap->psRemoteDevInfo->RemoteDevInfo.Iso15693_Info.UidLength);

    *(psNdefMap->SendRecvBuf + send_index) = (uint8_t)
                                page;
    send_index = (uint8_t)(send_index + 1);

    *(psNdefMap->SendRecvBuf + send_index) = (uint8_t)
                                numPages;
    send_index = (uint8_t)(send_index + 1);

    psNdefMap->SendLength = send_index;

    result = phFriNfc_OvrHal_Transceive(psNdefMap->LowerDevice,
                                        &psNdefMap->MapCompletionInfo,
                                        psNdefMap->psRemoteDevInfo,
                                        psNdefMap->Cmd,
                                        &psNdefMap->psDepAdditionalInfo,
                                        psNdefMap->SendRecvBuf,
                                        psNdefMap->SendLength,
                                        psNdefMap->SendRecvBuf,
                                        psNdefMap->SendRecvLength);

    return result;
}

static 
NFCSTATUS
phFriNfc_ISO15693_Reformat_Pageread_Buffer (
    uint8_t                 *p_recv_buf,
    uint8_t                 recv_length,
    uint8_t                 *p_dst_buf,
    uint8_t                 dst_length)
{
   // Inventory page reads return an extra security byte per page
   // So we need to reformat the returned buffer in memory
    uint32_t i = 0;
    uint32_t reformatted_index = 0;
    while (i < recv_length) {
        // Going for another page of 16 bytes, check for space in dst buffer
        if (reformatted_index + 16 > dst_length) {
            break;
        }
        if (p_recv_buf[i] == 0x0F) {
            // Security, insert 16 0 bytes
            memset(&(p_dst_buf[reformatted_index]), 0, 16);
            reformatted_index += 16;
            i++;
        } else {
            // Skip security byte
            i++;
            if (i + 16 <= recv_length) {
                memcpy(&(p_dst_buf[reformatted_index]), &(p_recv_buf[i]), 16);
                reformatted_index += 16;
            } else {
                break;
            }
            i+=16;
        }
    }
    return reformatted_index;
}

static 
NFCSTATUS 
phFriNfc_ISO15693_H_ProcessReadNdef (
    phFriNfc_NdefMap_t      *psNdefMap)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    phFriNfc_ISO15693Cont_t     *ps_iso_15693_con = 
                                &(psNdefMap->ISO15693Container);
    uint16_t                    remaining_data_size = 0;
    uint8_t                     *p_recv_buf = 
                                (psNdefMap->SendRecvBuf + ISO15693_EXTRA_RESP_BYTE);
    uint8_t                     recv_length = (uint8_t)
                                (*psNdefMap->SendRecvLength - ISO15693_EXTRA_RESP_BYTE);

    uint8_t *reformatted_buf = (uint8_t*) phOsalNfc_GetMemory(ps_iso_15693_con->max_data_size);

    if (ps_iso_15693_con->read_capabilities & ISO15693_CC_USE_IPR)
    {
        uint8_t reformatted_size = phFriNfc_ISO15693_Reformat_Pageread_Buffer(p_recv_buf, recv_length,
                reformatted_buf, ps_iso_15693_con->max_data_size);
        p_recv_buf = reformatted_buf + (ps_iso_15693_con->current_block * ISO15693_BYTES_PER_BLOCK);
        recv_length = reformatted_size - (ps_iso_15693_con->current_block * ISO15693_BYTES_PER_BLOCK);
    }
    if (ps_iso_15693_con->store_length)
    {
        /* Continue Offset option selected 
            So stored data already existing, 
            copy the information to the user buffer 
        */
        if (ps_iso_15693_con->store_length 
            <= (psNdefMap->ApduBufferSize - psNdefMap->ApduBuffIndex))
        {
            /* Stored data length is less than or equal 
                to the user expected size */
            (void)memcpy ((void *)(psNdefMap->ApduBuffer + 
                        psNdefMap->ApduBuffIndex), 
                            (void *)ps_iso_15693_con->store_read_data, 
                        ps_iso_15693_con->store_length);

            psNdefMap->ApduBuffIndex = (uint16_t)(psNdefMap->ApduBuffIndex + 
                                        ps_iso_15693_con->store_length);

            remaining_data_size = ps_iso_15693_con->store_length;
            
            ps_iso_15693_con->store_length = 0;                
        }
        else
        {
            /* stored length is more than the user expected size */
            remaining_data_size = (uint16_t)(ps_iso_15693_con->store_length -
                                (psNdefMap->ApduBufferSize - psNdefMap->ApduBuffIndex));

            (void)memcpy ((void *)(psNdefMap->ApduBuffer + 
                        psNdefMap->ApduBuffIndex), 
                        (void *)ps_iso_15693_con->store_read_data, 
                        remaining_data_size);

                /* As stored data is more than the user expected data. So store 
                    the remaining bytes again into the data structure */
            (void)memcpy ((void *)ps_iso_15693_con->store_read_data, 
                        (void *)(ps_iso_15693_con->store_read_data + 
                        remaining_data_size), 
                        (ps_iso_15693_con->store_length - remaining_data_size));

            psNdefMap->ApduBuffIndex = (uint16_t)(psNdefMap->ApduBuffIndex + 
                                        remaining_data_size);

                ps_iso_15693_con->store_length = (uint8_t)
                            (ps_iso_15693_con->store_length - remaining_data_size);
        }
    } /* if (ps_iso_15693_con->store_length) */
    else
    {
            /* Data is read from the card. */
        uint8_t                 byte_index = 0;

        remaining_data_size = ps_iso_15693_con->remaining_size_to_read;

            /* Check if the block number is to read the first VALUE field */
        if (ISO15693_GET_VALUE_FIELD_BLOCK_NO(ps_iso_15693_con->ndef_tlv_type_blk, 
                                    ps_iso_15693_con->ndef_tlv_type_byte, 
                                    ps_iso_15693_con->actual_ndef_size) 
            == ps_iso_15693_con->current_block)
        {
            /* Read from the beginning option selected, 
                BYTE number may start from the middle */
            byte_index = (uint8_t)ISO15693_GET_VALUE_FIELD_BYTE_NO(
                            ps_iso_15693_con->ndef_tlv_type_blk, 
                            ps_iso_15693_con->ndef_tlv_type_byte, 
                            ps_iso_15693_con->actual_ndef_size);
        }

        if ((psNdefMap->ApduBufferSize - psNdefMap->ApduBuffIndex)  
            < remaining_data_size)
        {
                remaining_data_size = (uint8_t)
                                    (recv_length - byte_index);
                /* user input is less than the remaining card size */
            if ((psNdefMap->ApduBufferSize - psNdefMap->ApduBuffIndex) 
                    < (uint16_t)remaining_data_size)
            {
                    /* user data required is less than the data read */
                remaining_data_size = (uint8_t)(psNdefMap->ApduBufferSize - 
                                                psNdefMap->ApduBuffIndex);

                    if (0 != (recv_length - (byte_index + 
                                    remaining_data_size)))
                    {
                /* Store the data for the continue read option */
                (void)memcpy ((void *)ps_iso_15693_con->store_read_data, 
                                (void *)(p_recv_buf + (byte_index + 
                                        remaining_data_size)), 
                                        (recv_length - (byte_index + 
                                        remaining_data_size)));

                ps_iso_15693_con->store_length = (uint8_t)
                                    (recv_length - (byte_index + 
                                        remaining_data_size));
            }
            }
        }
        else
        {
                /* user data required is equal or greater than the data read */
            if (remaining_data_size > (recv_length - byte_index))                
            {
                remaining_data_size = (uint8_t)
                                (recv_length - byte_index);
            }
        }

            /* Copy data in the user buffer */
        (void)memcpy ((void *)(psNdefMap->ApduBuffer + 
                        psNdefMap->ApduBuffIndex), 
                        (void *)(p_recv_buf + byte_index), 
                        remaining_data_size);

            /* Update the read index */
        psNdefMap->ApduBuffIndex = (uint16_t)(psNdefMap->ApduBuffIndex + 
                                    remaining_data_size);            

        } /* else part of if (ps_iso_15693_con->store_length) */

    /* Remaining size is decremented */
    ps_iso_15693_con->remaining_size_to_read = (uint8_t)
                            (ps_iso_15693_con->remaining_size_to_read - 
                            remaining_data_size);

    if ((psNdefMap->ApduBuffIndex != psNdefMap->ApduBufferSize)
        && (0 != ps_iso_15693_con->remaining_size_to_read))
    {            
        ps_iso_15693_con->current_block = (uint16_t)
                            (ps_iso_15693_con->current_block + 1);
        /* READ again */
        if ((ps_iso_15693_con->read_capabilities & ISO15693_CC_USE_MBR) ||
            (ps_iso_15693_con->read_capabilities & ISO15693_CC_USE_IPR)) {
            result = phFriNfc_ReadRemainingInMultiple(psNdefMap, ps_iso_15693_con->current_block);
        }
        else {
            result = phFriNfc_ISO15693_H_ReadWrite (psNdefMap, ISO15693_READ_COMMAND, 
                                                    NULL, 0);
        }
    }
    else
    {
            /* Read completed, EITHER index has reached to the user size 
            OR end of the card is reached
            update the user data structure with read data size */
        *psNdefMap->NumOfBytesRead = psNdefMap->ApduBuffIndex;
    }
    if (reformatted_buf != NULL) {
        phOsalNfc_FreeMemory(reformatted_buf);
    }
    return result;
}

static 
NFCSTATUS 
phFriNfc_ISO15693_H_CheckCCBytes (
    phFriNfc_NdefMap_t      *psNdefMap)
{
    NFCSTATUS               result = NFCSTATUS_SUCCESS;
    phFriNfc_ISO15693Cont_t *ps_iso_15693_con = 
                            &(psNdefMap->ISO15693Container);
    uint8_t                 recv_index = 0;
    uint8_t                 *p_recv_buf = (psNdefMap->SendRecvBuf + 1);

    /* expected CC byte : E1 40 "MAX SIZE depends on tag" */
    if (ISO15693_CC_MAGIC_BYTE == *p_recv_buf)
    {
        /*  0xE1 magic byte found*/
        recv_index = (uint8_t)(recv_index + 1);
        uint8_t tag_major_version = (*(p_recv_buf + recv_index) & ISO15693_MAJOR_VERSION_MASK) >> 6;
        if (ISO15693_MAPPING_VERSION >= tag_major_version)
        {
            /* Correct mapping version found */
            switch (*(p_recv_buf + recv_index) & ISO15693_LSB_NIBBLE_MASK)
            {
                case ISO15693_RD_WR_PERMISSION:
                {
                    /* READ/WRITE possible */
                    psNdefMap->CardState = PH_NDEFMAP_CARD_STATE_READ_WRITE;
                    break;
                }

                case ISO15693_RD_ONLY_PERMISSION:
                {
                    /* ONLY READ possible, WRITE NOT possible */
                    psNdefMap->CardState = PH_NDEFMAP_CARD_STATE_READ_ONLY;
                    break;
                }

                default:
                {
                    result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                        NFCSTATUS_NO_NDEF_SUPPORT);
                    break;
                }
            }
            recv_index = (uint8_t)(recv_index + 1);
            
            if (!result)
            {
                /* Update MAX SIZE */
                ps_iso_15693_con->max_data_size = (uint16_t)
                    (*(p_recv_buf + recv_index) *
                    ISO15693_MULT_FACTOR);
                recv_index = (uint8_t)(recv_index + 1);
                ps_iso_15693_con->read_capabilities = (*(p_recv_buf + recv_index));


            }
        }
        else
        {
            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_NO_NDEF_SUPPORT);
        }
    }
    else
    {
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_NO_NDEF_SUPPORT);
    }
    return result;
}

static 
NFCSTATUS 
phFriNfc_ISO15693_H_ProcessCheckNdef (
    phFriNfc_NdefMap_t      *psNdefMap)
{
    NFCSTATUS               result = NFCSTATUS_SUCCESS;
    phFriNfc_ISO15693Cont_t *ps_iso_15693_con = 
                            &(psNdefMap->ISO15693Container);
    phFriNfc_eChkNdefSeq_t  e_chk_ndef_seq = (phFriNfc_eChkNdefSeq_t)
                            psNdefMap->ISO15693Container.ndef_seq;
    
    uint8_t                 *p_recv_buf = 
                            (psNdefMap->SendRecvBuf + ISO15693_EXTRA_RESP_BYTE);
    uint8_t                 recv_length = (uint8_t)
                            (*psNdefMap->SendRecvLength - ISO15693_EXTRA_RESP_BYTE);
    uint8_t                 parse_index = 0;
    static uint16_t         prop_ndef_index = 0;
    uint8_t *reformatted_buf = (uint8_t*) phOsalNfc_GetMemory(ps_iso_15693_con->max_data_size);

    if (0 == ps_iso_15693_con->current_block)
    {
        /* Check CC byte */
        result = phFriNfc_ISO15693_H_CheckCCBytes (psNdefMap);
        parse_index = (uint8_t)(parse_index + recv_length);
    }
    else if (1 == ps_iso_15693_con->current_block &&
            (ps_iso_15693_con->read_capabilities & ISO15693_CC_USE_IPR))
    {
        
        uint8_t reformatted_size = phFriNfc_ISO15693_Reformat_Pageread_Buffer(p_recv_buf, recv_length,
                reformatted_buf, ps_iso_15693_con->max_data_size);
        // Skip initial CC bytes
        p_recv_buf = reformatted_buf + (ps_iso_15693_con->current_block * ISO15693_BYTES_PER_BLOCK);
        recv_length = reformatted_size - (ps_iso_15693_con->current_block * ISO15693_BYTES_PER_BLOCK);
    }
    else
    {
        /* Propreitary TLVs VALUE can end in between a block, 
            so when that block is read, update the parse_index 
            with byte address value */
        if (ISO15693_PROP_TLV_V == e_chk_ndef_seq)
        {
            parse_index = ps_iso_15693_con->ndef_tlv_type_byte;
            e_chk_ndef_seq = ISO15693_NDEF_TLV_T;
        }
    }

    while ((parse_index < recv_length) 
            && (NFCSTATUS_SUCCESS == result) 
            && (ISO15693_NDEF_TLV_V != e_chk_ndef_seq))
    {
        /* Parse 
            1. till the received length of the block 
            2. till there is no error during parse 
            3. till LENGTH field of NDEF TLV is found  
        */
        switch (e_chk_ndef_seq)
        {
            case ISO15693_NDEF_TLV_T:
            {
                /* Expected value is 0x03 TYPE identifier 
                    of the NDEF TLV */
                prop_ndef_index = 0;
                switch (*(p_recv_buf + parse_index))
                {
                    case ISO15693_NDEF_TLV_TYPE_ID:
                    {
                        /* Update the data structure with the byte address and 
                        the block number */
                        ps_iso_15693_con->ndef_tlv_type_byte = parse_index;
                        ps_iso_15693_con->ndef_tlv_type_blk = 
                                            ps_iso_15693_con->current_block;
                        e_chk_ndef_seq = ISO15693_NDEF_TLV_L;

                        break;
                    }

                    case ISO15693_NULL_TLV_ID:
                    {
                        /* Dont do any thing, go to next byte */
                        break;
                    }
                    
                    case ISO15693_PROP_TLV_ID:
                    {
                        /* Move the sequence to find the length 
                            of the proprietary TLV */
                        e_chk_ndef_seq = ISO15693_PROP_TLV_L;
                        break;
                    }

                    default:
                    {
                        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                            NFCSTATUS_NO_NDEF_SUPPORT);
                        break;
                    }
                } /* switch (*(p_recv_buf + parse_index)) */
                break;
            }

            case ISO15693_PROP_TLV_L:
            {
                /* Length field of the proprietary TLV */
                switch (prop_ndef_index)
                {
                    /* Length field can have 1 or 3 bytes depending 
                        on the data size, so check for each index byte */
                    case 0:
                    {
                        /* 1st index of the length field of the TLV */
                        if (0 == *(p_recv_buf + parse_index))
                        {
                            /* LENGTH is 0, not possible, so error */
                            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                                NFCSTATUS_NO_NDEF_SUPPORT);
                            e_chk_ndef_seq = ISO15693_NDEF_TLV_T;
                        }
                        else 
                        {
                            if (ISO15693_THREE_BYTE_LENGTH_ID == 
                                *(p_recv_buf + parse_index))
                            {
                                /* 3 byte LENGTH field identified, so increment the 
                                index, so next time 2nd byte is parsed */
                                prop_ndef_index = (uint8_t)(prop_ndef_index + 1);
                            }
                            else
                            {
                                /* 1 byte LENGTH field identified, so "static" 
                                index is set to 0 and actual ndef size is 
                                copied to the data structure
                                */
                                ps_iso_15693_con->actual_ndef_size = 
                                                    *(p_recv_buf + parse_index);
                                e_chk_ndef_seq = ISO15693_PROP_TLV_V;
                                prop_ndef_index = 0;
                            }
                        }
                        break;
                    }

                    case 1:
                    {
                        /* 2nd index of the LENGTH field that is MSB of the length, 
                        so the length is left shifted by 8 */
                        ps_iso_15693_con->actual_ndef_size = (uint16_t)
                                        (*(p_recv_buf + parse_index) << 
                                        ISO15693_BTYE_SHIFT);
                        prop_ndef_index = (uint8_t)(prop_ndef_index + 1);
                        break;
                    }

                    case 2:
                    {
                        /* 3rd index of the LENGTH field that is LSB of the length, 
                        so the length ORed with the previously stored size */
                        ps_iso_15693_con->actual_ndef_size = (uint16_t)
                                        (ps_iso_15693_con->actual_ndef_size | 
                                        *(p_recv_buf + parse_index));

                        e_chk_ndef_seq = ISO15693_PROP_TLV_V;
                        prop_ndef_index = 0;
                        break;
                    }

                    default:
                    {
                        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                            NFCSTATUS_INVALID_DEVICE_REQUEST);
                        break;
                    }
                } /* switch (prop_ndef_index) */

                if ((ISO15693_PROP_TLV_V == e_chk_ndef_seq)
                    && (ISO15693_GET_REMAINING_SIZE(ps_iso_15693_con->max_data_size, 
                        ps_iso_15693_con->current_block, parse_index) 
                        <= ps_iso_15693_con->actual_ndef_size))
                {
                    /* Check for the length field value has not exceeded the card size, 
                    if size is exceeded or then return error */
                    e_chk_ndef_seq = ISO15693_NDEF_TLV_T;
                    result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                        NFCSTATUS_NO_NDEF_SUPPORT);
                }
                else
                {
                    uint16_t            prop_byte_addr = 0;

                    /* skip the proprietary TLVs value field */
                    prop_byte_addr = (uint16_t)
                        ((ps_iso_15693_con->current_block * ISO15693_BYTES_PER_BLOCK) + 
                        parse_index + ps_iso_15693_con->actual_ndef_size);

                    ps_iso_15693_con->ndef_tlv_type_byte = (uint8_t)(prop_byte_addr % 
                                                        ISO15693_BYTES_PER_BLOCK);
                    ps_iso_15693_con->ndef_tlv_type_blk = (uint16_t)(prop_byte_addr / 
                                                        ISO15693_BYTES_PER_BLOCK);
                    if (parse_index + ps_iso_15693_con->actual_ndef_size >= 
                        recv_length)
                    {
                        parse_index = (uint8_t)recv_length;
                    }
                    else
                    {
                        parse_index = (uint8_t)(parse_index + 
                                        ps_iso_15693_con->actual_ndef_size);
                    }

                }
                break;
            } /* case ISO15693_PROP_TLV_L: */

            case ISO15693_PROP_TLV_V:
            {
                uint8_t         remaining_length = (uint8_t)(recv_length - 
                                                    parse_index);

                if ((ps_iso_15693_con->actual_ndef_size - prop_ndef_index) 
                    > remaining_length)
                {
                    parse_index = (uint8_t)(parse_index + remaining_length);
                    prop_ndef_index = (uint8_t)(prop_ndef_index + remaining_length);
                }
                else if ((ps_iso_15693_con->actual_ndef_size - prop_ndef_index) 
                    == remaining_length)
                {
                    parse_index = (uint8_t)(parse_index + remaining_length);
                    e_chk_ndef_seq = ISO15693_NDEF_TLV_T;
                    prop_ndef_index = 0;
                }
                else
                {
                    parse_index = (uint8_t)(parse_index + 
                                            (ps_iso_15693_con->actual_ndef_size - 
                                            prop_ndef_index)); 
                    e_chk_ndef_seq = ISO15693_NDEF_TLV_T;
                    prop_ndef_index = 0;
                }
                break;
            } /* case ISO15693_PROP_TLV_V: */

            case ISO15693_NDEF_TLV_L:
            {
                /* Length field of the NDEF TLV */
                switch (prop_ndef_index)
                {
                    /* Length field can have 1 or 3 bytes depending 
                        on the data size, so check for each index byte */
                    case 0:
                    {
                        /* 1st index of the length field of the TLV */
                        if (0 == *(p_recv_buf + parse_index))
                        {
                            /* LENGTH is 0, card is in INITILIASED STATE */
                            e_chk_ndef_seq = ISO15693_NDEF_TLV_V;
                            ps_iso_15693_con->actual_ndef_size = 0;
                        }
                        else 
                        {
                            prop_ndef_index = (uint8_t)(prop_ndef_index + 1);

                            if (ISO15693_THREE_BYTE_LENGTH_ID == 
                                *(p_recv_buf + parse_index))
                            {
                                /* At present no CARD supports more than 255 bytes, 
                                so error is returned */
                                prop_ndef_index = (uint8_t)(prop_ndef_index + 1);
                                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                                    NFCSTATUS_NO_NDEF_SUPPORT);
                                prop_ndef_index = 0;
                            }
                            else
                            {
                                /* 1 byte LENGTH field identified, so "static" 
                                index is set to 0 and actual ndef size is 
                                copied to the data structure
                                */
                                ps_iso_15693_con->actual_ndef_size = 
                                                    *(p_recv_buf + parse_index);
                                /* next values are the DATA field of the NDEF TLV */
                                e_chk_ndef_seq = ISO15693_NDEF_TLV_V;
                                prop_ndef_index = 0;
                            }
                        }
                        break;
                    }

                    case 1:
                    {
                        /* 2nd index of the LENGTH field that is MSB of the length, 
                        so the length is left shifted by 8 */
                        ps_iso_15693_con->actual_ndef_size = (uint16_t)
                            (*(p_recv_buf + parse_index) << 
                            ISO15693_BTYE_SHIFT);
                        prop_ndef_index = (uint8_t)(prop_ndef_index + 1);
                        break;
                    }

                    case 2:
                    {
                        /* 3rd index of the LENGTH field that is LSB of the length, 
                        so the length ORed with the previously stored size */
                        ps_iso_15693_con->actual_ndef_size = (uint16_t)
                            (ps_iso_15693_con->actual_ndef_size | 
                            *(p_recv_buf + parse_index));

                        e_chk_ndef_seq = ISO15693_NDEF_TLV_V;
                        prop_ndef_index = 0;
                        break;
                    }

                    default:
                    {
                        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_INVALID_DEVICE_REQUEST);
                        break;
                    }
                } /* switch (prop_ndef_index) */
                
                if ((ISO15693_NDEF_TLV_V == e_chk_ndef_seq)
                    && (ISO15693_GET_REMAINING_SIZE(ps_iso_15693_con->max_data_size, 
                        /* parse_index + 1 is done because the data starts from the next index. 
                        "MOD" operation is used to know that parse_index > 
                        ISO15693_BYTES_PER_BLOCK, then block shall be incremented 
                        */
                        (((parse_index + 1) % ISO15693_BYTES_PER_BLOCK) ?  
                        ps_iso_15693_con->current_block : 
                        ps_iso_15693_con->current_block + 1), ((parse_index + 1) % 
                        ISO15693_BYTES_PER_BLOCK)) 
                        < ps_iso_15693_con->actual_ndef_size))
                {
                    /* Check for the length field value has not exceeded the card size */
                    e_chk_ndef_seq = ISO15693_NDEF_TLV_T;
                    result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                        NFCSTATUS_NO_NDEF_SUPPORT);
                }
                else
                {
                    psNdefMap->CardState = (uint8_t)
                                    ((PH_NDEFMAP_CARD_STATE_READ_ONLY 
                                    == psNdefMap->CardState) ? 
                                    PH_NDEFMAP_CARD_STATE_READ_ONLY :
                                    ((ps_iso_15693_con->actual_ndef_size) ? 
                                    PH_NDEFMAP_CARD_STATE_READ_WRITE : 
                                    PH_NDEFMAP_CARD_STATE_INITIALIZED));
                }
                break;
            } /* case ISO15693_NDEF_TLV_L: */

            case ISO15693_NDEF_TLV_V:
            {
                break;
            }

            default:
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_INVALID_DEVICE_REQUEST);
                break;
            }
        } /* switch (e_chk_ndef_seq) */
        parse_index = (uint8_t)(parse_index + 1);
    } /* while ((parse_index < recv_length) 
            && (NFCSTATUS_SUCCESS == result) 
            && (ISO15693_NDEF_TLV_V != e_chk_ndef_seq)) */
    
    if (result)
    {
        /* Error returned while parsing, so STOP read */
        e_chk_ndef_seq = ISO15693_NDEF_TLV_T;
        prop_ndef_index = 0;
    }
    else if (ISO15693_NDEF_TLV_V != e_chk_ndef_seq)
    {
        /* READ again */
        if (ISO15693_PROP_TLV_V != e_chk_ndef_seq)
        {
            ps_iso_15693_con->current_block = (uint16_t)
                                (ps_iso_15693_con->current_block + 1);
        }
        else
        {
            /* Proprietary TLV detected, so skip the proprietary blocks */
            ps_iso_15693_con->current_block = ps_iso_15693_con->ndef_tlv_type_blk;
        }
   
        uint32_t remaining_size = ISO15693_GET_REMAINING_SIZE(ps_iso_15693_con->max_data_size,             
                                           ps_iso_15693_con->current_block, 0);
        if (remaining_size > 0)
        {
            if ((ps_iso_15693_con->read_capabilities & ISO15693_CC_USE_MBR) ||
                (ps_iso_15693_con->read_capabilities & ISO15693_CC_USE_IPR)) {
                result = phFriNfc_ReadRemainingInMultiple(psNdefMap, ps_iso_15693_con->current_block);
            } else  {
                result = phFriNfc_ISO15693_H_ReadWrite (psNdefMap, ISO15693_READ_COMMAND, 
                                                        NULL, 0);
            }
        }
        else
        {
            /* End of card reached, error no NDEF information found */
            e_chk_ndef_seq = ISO15693_NDEF_TLV_T;
            prop_ndef_index = 0;
            /* Error, no size to parse */
            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_NO_NDEF_SUPPORT);
        }

    }
    else
    {
        /* Successful read with proper NDEF information updated */
        prop_ndef_index = 0;
        e_chk_ndef_seq = ISO15693_NDEF_TLV_T;
        psNdefMap->CardType = (uint8_t)PH_FRINFC_NDEFMAP_ISO15693_CARD;
    }

    psNdefMap->ISO15693Container.ndef_seq = (uint8_t)e_chk_ndef_seq;

    if (reformatted_buf != NULL) {
        phOsalNfc_FreeMemory(reformatted_buf);
    }
    return result;
}

static 
void 
phFriNfc_ISO15693_H_Complete (
    phFriNfc_NdefMap_t      *psNdefMap,
    NFCSTATUS               Status)
{
    /* set the state back to the RESET_INIT state*/
    psNdefMap->State =  PH_FRINFC_NDEFMAP_STATE_RESET_INIT;

    /* set the completion routine*/
    psNdefMap->CompletionRoutine[psNdefMap->ISO15693Container.cr_index].
        CompletionRoutine (psNdefMap->CompletionRoutine->Context, Status);
}

#ifdef FRINFC_READONLY_NDEF

static
NFCSTATUS 
phFriNfc_ISO15693_H_ProcessReadOnly (
    phFriNfc_NdefMap_t      *psNdefMap)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;    
    phFriNfc_ISO15693Cont_t     *ps_iso_15693_con = 
                                &(psNdefMap->ISO15693Container);
    phFriNfc_eRONdefSeq_t       e_ro_ndef_seq = (phFriNfc_eRONdefSeq_t)
                                ps_iso_15693_con->ndef_seq;
    uint8_t                     *p_recv_buf = (psNdefMap->SendRecvBuf + 
                                ISO15693_EXTRA_RESP_BYTE);
    uint8_t                     recv_length = (uint8_t)(*psNdefMap->SendRecvLength - 
                                ISO15693_EXTRA_RESP_BYTE);
    uint8_t                     a_write_buf[ISO15693_BYTES_PER_BLOCK] = {0};

    switch (e_ro_ndef_seq)
    {
        case ISO15693_RD_BEFORE_WR_CC:
        {
            if (ISO15693_SINGLE_BLK_RD_RESP_LEN == recv_length)
            {
                result = phFriNfc_ISO15693_H_CheckCCBytes (psNdefMap);
                /* Check CC bytes and also the card state for READ ONLY, 
                if the card is already read only, then dont continue with 
                next operation */
                if ((PH_NDEFMAP_CARD_STATE_READ_ONLY != psNdefMap->CardState) 
                    && (!result))
                {
                    /* CC byte read successful */
                (void)memcpy ((void *)a_write_buf, (void *)p_recv_buf, 
                                sizeof (a_write_buf));

                    /* Change the read write access to read only */
                *(a_write_buf + ISO15693_RW_BTYE_INDEX) = (uint8_t)
                            (*(a_write_buf + ISO15693_RW_BTYE_INDEX) | 
                            ISO15693_CC_READ_ONLY_MASK);

                result = phFriNfc_ISO15693_H_ReadWrite (psNdefMap, 
                                    ISO15693_WRITE_COMMAND, a_write_buf, 
                                sizeof (a_write_buf));

                e_ro_ndef_seq = ISO15693_WRITE_CC;
            }
            }
            break;
        }

        case ISO15693_WRITE_CC:
        {
            /* Write to CC is successful. */
            e_ro_ndef_seq = ISO15693_LOCK_BLOCK;
            /* Start the lock block command to lock the blocks */
            result = phFriNfc_ISO15693_H_ReadWrite (psNdefMap, 
                                ISO15693_LOCK_BLOCK_CMD, NULL, 0);
            break;
        }

        case ISO15693_LOCK_BLOCK:
        {
            if (ps_iso_15693_con->current_block == 
                ((ps_iso_15693_con->max_data_size / ISO15693_BYTES_PER_BLOCK) - 
                1))
            {
                /* End of card reached, READ ONLY successful */
            }
            else
            {
                /* current block is incremented */
                ps_iso_15693_con->current_block = (uint16_t)
                    (ps_iso_15693_con->current_block + 1);
                /* Lock the current block */
                result = phFriNfc_ISO15693_H_ReadWrite (psNdefMap, 
                                ISO15693_LOCK_BLOCK_CMD, NULL, 0);
            }
            break;
        }

        default:
        {
            break;
        }
    }

    ps_iso_15693_con->ndef_seq = (uint8_t)e_ro_ndef_seq;
    return result;
}

#endif /* #ifdef FRINFC_READONLY_NDEF */
/************************** END static functions definition *********************/

/************************** START external functions *********************/

NFCSTATUS 
phFriNfc_ISO15693_ChkNdef (
    phFriNfc_NdefMap_t  *psNdefMap)
{
    NFCSTATUS                       result = NFCSTATUS_SUCCESS;
    phHal_sIso15693Info_t           *ps_iso_15693_info = 
                        &(psNdefMap->psRemoteDevInfo->RemoteDevInfo.Iso15693_Info);

    /* Update the previous operation with current operation. 
        This becomes the previous operation after this execution */
    psNdefMap->PrevOperation = PH_FRINFC_NDEFMAP_CHECK_OPE;
    /* Update the CR index to know from which operation completion 
        routine has to be called */
    psNdefMap->ISO15693Container.cr_index = PH_FRINFC_NDEFMAP_CR_CHK_NDEF;
    /* State update */
    psNdefMap->State = ISO15693_CHECK_NDEF;
    /* Reset the NDEF sequence */
    psNdefMap->ISO15693Container.ndef_seq = 0;
    psNdefMap->ISO15693Container.current_block = 0;
    psNdefMap->ISO15693Container.actual_ndef_size = 0;
    psNdefMap->ISO15693Container.ndef_tlv_type_blk = 0;
    psNdefMap->ISO15693Container.ndef_tlv_type_byte = 0;
    psNdefMap->ISO15693Container.store_length = 0;
    psNdefMap->ISO15693Container.remaining_size_to_read = 0;
    psNdefMap->ISO15693Container.read_capabilities = 0;

    if ((ISO15693_UIDBYTE_6_VALUE == 
        ps_iso_15693_info->Uid[ISO15693_UID_BYTE_6]) 
        && (ISO15693_UIDBYTE_7_VALUE == 
        ps_iso_15693_info->Uid[ISO15693_UID_BYTE_7]))
    {
        /* Check if the card is manufactured by NXP (6th byte 
            index of UID value = 0x04 and the 
            last byte i.e., 7th byte of UID is 0xE0, only then the card detected 
            is NDEF compliant */
    switch (ps_iso_15693_info->Uid[ISO15693_UID_BYTE_5])
    {
            /* Check for supported tags, by checking the 5th byte index of UID */
        case ISO15693_UIDBYTE_5_VALUE_SLI_X:
        {
                /* ISO 15693 card type is ICODE SLI 
                so maximum size is 112 */
            psNdefMap->ISO15693Container.max_data_size = 
                            ISO15693_SL2_S2002_ICS20;
            break;
        }

        case ISO15693_UIDBYTE_5_VALUE_SLI_X_S:
        {
                /* ISO 15693 card type is ICODE SLI/X S  
                so maximum size depends on the 4th UID byte index */
            switch (ps_iso_15693_info->Uid[ISO15693_UID_BYTE_4])
            {
                case ISO15693_UIDBYTE_4_VALUE_SLI_X_S:
                case ISO15693_UIDBYTE_4_VALUE_SLI_X_SHC:
                case ISO15693_UIDBYTE_4_VALUE_SLI_X_SY:
                {
                        /* Supported tags are with value (4th byte UID index)
                        of 0x00, 0x80 and 0x40 
                        For these cards max size is 160 bytes */
                    psNdefMap->ISO15693Container.max_data_size = 
                                    ISO15693_SL2_S5302_ICS53_ICS54;
                    break;
                }

                default:
                {
                        /* Tag not supported */
                    result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                        NFCSTATUS_INVALID_DEVICE_REQUEST);
                    break;
                }
            }
            break;
        }

        case ISO15693_UIDBYTE_5_VALUE_SLI_X_L:
        {
                /* ISO 15693 card type is ICODE SLI/X L  
                so maximum size depends on the 4th UID byte index */
            switch (ps_iso_15693_info->Uid[ISO15693_UID_BYTE_4])
            {
                case ISO15693_UIDBYTE_4_VALUE_SLI_X_L:
                case ISO15693_UIDBYTE_4_VALUE_SLI_X_LHC:
                {
                        /* Supported tags are with value (4th byte UID index)
                        of 0x00 and 0x80
                        For these cards max size is 32 bytes */
                    psNdefMap->ISO15693Container.max_data_size = 
                                    ISO15693_SL2_S5002_ICS50_ICS51;
                    break;
                }

                default:
                {
                        /* Tag not supported */
                    result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                        NFCSTATUS_INVALID_DEVICE_REQUEST);
                    break;
                }
            }
            break;
        }

        default:
        {
                /* Tag not supported */
            result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_DEVICE_REQUEST);
            break;
        }
    }
    }
    else
    {
        /* Tag not supported */
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_INVALID_DEVICE_REQUEST);
    }

    if (!result)
    {
        /* Start reading the data */
        result = phFriNfc_ISO15693_H_ReadWrite (psNdefMap, ISO15693_READ_COMMAND, 
                                                NULL, 0);
    }
    

    return result;
}

NFCSTATUS 
phFriNfc_ISO15693_RdNdef (
    phFriNfc_NdefMap_t  *psNdefMap,
    uint8_t             *pPacketData,
    uint32_t            *pPacketDataLength,
    uint8_t             Offset)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    phFriNfc_ISO15693Cont_t     *ps_iso_15693_con = 
                                &(psNdefMap->ISO15693Container);

    /* Update the previous operation with current operation. 
        This becomes the previous operation after this execution */
    psNdefMap->PrevOperation = PH_FRINFC_NDEFMAP_READ_OPE;
    /* Update the CR index to know from which operation completion 
        routine has to be called */
    psNdefMap->ISO15693Container.cr_index = PH_FRINFC_NDEFMAP_CR_RD_NDEF;
    /* State update */
    psNdefMap->State = ISO15693_READ_NDEF;
    /* Copy user buffer to the context */
    psNdefMap->ApduBuffer = pPacketData;
    /* Copy user length to the context */
    psNdefMap->ApduBufferSize = *pPacketDataLength;
    /* Update the user memory size to a context variable */
    psNdefMap->NumOfBytesRead = pPacketDataLength;
    /* Number of bytes read from the card is zero. 
    This variable returns the number of bytes read 
    from the card. */
    *psNdefMap->NumOfBytesRead = 0;
    /* Index to know the length read */
    psNdefMap->ApduBuffIndex = 0;    
    /* Store the offset in the context */
    psNdefMap->Offset = Offset;

    if ((!ps_iso_15693_con->remaining_size_to_read) 
        && (!psNdefMap->Offset))
    {
        /* Entire data is already read from the card. 
        There is no data to give */
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_EOF_NDEF_CONTAINER_REACHED); 
    }
    else if (0 == ps_iso_15693_con->actual_ndef_size)
    {
        /* Card is NDEF, but no data in the card. */
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_READ_FAILED);
    }
    else if (PH_NDEFMAP_CARD_STATE_INITIALIZED == psNdefMap->CardState)
    {
        /* Card is NDEF, but no data in the card. */
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_READ_FAILED);
    }
    else if (psNdefMap->Offset)
    {
        /* BEGIN offset, so reset the remaining read size and 
        also the curretn block */
        ps_iso_15693_con->remaining_size_to_read = 
                        ps_iso_15693_con->actual_ndef_size;
        ps_iso_15693_con->current_block = 
                        ISO15693_GET_VALUE_FIELD_BLOCK_NO(
                        ps_iso_15693_con->ndef_tlv_type_blk, 
                        ps_iso_15693_con->ndef_tlv_type_byte, 
                        ps_iso_15693_con->actual_ndef_size);

        // Check capabilities
        if ((ps_iso_15693_con->read_capabilities & ISO15693_CC_USE_MBR) ||
            (ps_iso_15693_con->read_capabilities & ISO15693_CC_USE_IPR)) {
            result = phFriNfc_ReadRemainingInMultiple(psNdefMap, ps_iso_15693_con->current_block);
        } else  {
            result = phFriNfc_ISO15693_H_ReadWrite (psNdefMap, ISO15693_READ_COMMAND, 
                                                        NULL, 0);
        }
    }
    else
    {
        /* CONTINUE offset */
        if (ps_iso_15693_con->store_length > 0)
        {
            /* Previous read had extra bytes, so data is stored, so give that take 
            that data from store. If more data is required, then read remaining bytes */
            result = phFriNfc_ISO15693_H_ProcessReadNdef (psNdefMap);
        }
        else
        {
            ps_iso_15693_con->current_block = (uint16_t)
                                (ps_iso_15693_con->current_block + 1);
            result = phFriNfc_ISO15693_H_ReadWrite (psNdefMap, 
                                            ISO15693_READ_COMMAND, NULL, 0);
        }
    }

    return result;
}

static
NFCSTATUS
phFriNfc_ReadRemainingInMultiple (
    phFriNfc_NdefMap_t  *psNdefMap,
    uint32_t            startBlock) 
{
    NFCSTATUS result = NFCSTATUS_FAILED;
    phFriNfc_ISO15693Cont_t *ps_iso_15693_con = &(psNdefMap->ISO15693Container);

    uint32_t remaining_size = ISO15693_GET_REMAINING_SIZE(ps_iso_15693_con->max_data_size,             
                                           startBlock, 0);
    // Check capabilities
    if (ps_iso_15693_con->read_capabilities & ISO15693_CC_USE_MBR) {
        // Multi-page read command
        uint8_t mbread[1];
        mbread[0] = (remaining_size / ISO15693_BYTES_PER_BLOCK) - 1;
        result = phFriNfc_ISO15693_H_ReadWrite (psNdefMap, ISO15693_READ_MULTIPLE_COMMAND, 
                mbread, 1);
    } else if (ps_iso_15693_con->read_capabilities & ISO15693_CC_USE_IPR) {
        uint32_t page = 0;
        uint32_t pagesToRead = (remaining_size / ISO15693_BYTES_PER_BLOCK / 4) - 1;
        if ((remaining_size % (ISO15693_BYTES_PER_BLOCK * ISO15693_BLOCKS_PER_PAGE)) != 0) {
            pagesToRead++;
        }
        result = phFriNfc_ISO15693_H_Inventory_Page_Read (psNdefMap, ICODE_INVENTORY_PAGEREAD_COMMAND, 
                page, pagesToRead);
        // Inventory
    } else  {
        result = NFCSTATUS_FAILED;
    }
    return result;
}

NFCSTATUS 
phFriNfc_ISO15693_WrNdef (
    phFriNfc_NdefMap_t  *psNdefMap,
    uint8_t             *pPacketData,
    uint32_t            *pPacketDataLength,
    uint8_t             Offset)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;    
    phFriNfc_ISO15693Cont_t     *ps_iso_15693_con = 
                                &(psNdefMap->ISO15693Container);
    uint8_t                     a_write_buf[ISO15693_BYTES_PER_BLOCK] = {0};

    /* Update the previous operation with current operation. 
        This becomes the previous operation after this execution */
    psNdefMap->PrevOperation = PH_FRINFC_NDEFMAP_WRITE_OPE;
    /* Update the CR index to know from which operation completion 
        routine has to be called */
    psNdefMap->ISO15693Container.cr_index = PH_FRINFC_NDEFMAP_CR_WR_NDEF;
    /* State update */
    psNdefMap->State = ISO15693_WRITE_NDEF;
    /* Copy user buffer to the context */
    psNdefMap->ApduBuffer = pPacketData;
    /* Copy user length to the context */
    psNdefMap->ApduBufferSize = *pPacketDataLength;
    /* Update the user memory size to a context variable */
    psNdefMap->NumOfBytesRead = pPacketDataLength;
    /* Number of bytes written to the card is zero. 
    This variable returns the number of bytes written 
    to the card. */
    *psNdefMap->WrNdefPacketLength = 0;
    /* Index to know the length read */
    psNdefMap->ApduBuffIndex = 0;    
    /* Store the offset in the context */
    psNdefMap->Offset = Offset;

    /* Set the current block correctly to write the length field to 0 */
    ps_iso_15693_con->current_block = 
                        ISO15693_GET_LEN_FIELD_BLOCK_NO(
                        ps_iso_15693_con->ndef_tlv_type_blk, 
                        ps_iso_15693_con->ndef_tlv_type_byte, 
                        *pPacketDataLength);

    if (ISO15693_GET_LEN_FIELD_BYTE_NO(
                        ps_iso_15693_con->ndef_tlv_type_blk, 
                        ps_iso_15693_con->ndef_tlv_type_byte, 
                        *pPacketDataLength))
    {
        /* Check the byte address to write. If length byte address is in between or 
        is the last byte of the block, then READ before write 
        reason, write should not corrupt other data 
        */
        ps_iso_15693_con->ndef_seq = (uint8_t)ISO15693_RD_BEFORE_WR_NDEF_L_0;
        result = phFriNfc_ISO15693_H_ReadWrite (psNdefMap, 
                                ISO15693_READ_COMMAND, NULL, 0);
    }
    else
    {
        /* If length byte address is at the beginning of the block then WRITE 
        length field to 0 and as also write user DATA */
        ps_iso_15693_con->ndef_seq = (uint8_t)ISO15693_WRITE_DATA;

        /* Length is made 0x00 */
        *a_write_buf = 0x00;

        /* Write remaining data */
        (void)memcpy ((void *)(a_write_buf + 1), 
                        (void *)psNdefMap->ApduBuffer, 
                        (ISO15693_BYTES_PER_BLOCK - 1));

        /* Write data */
        result = phFriNfc_ISO15693_H_ReadWrite (psNdefMap, 
                                    ISO15693_WRITE_COMMAND, 
                                    a_write_buf, ISO15693_BYTES_PER_BLOCK);

        /* Increment the index to keep track of bytes sent for write */
        psNdefMap->ApduBuffIndex = (uint16_t)(psNdefMap->ApduBuffIndex
                                        + (ISO15693_BYTES_PER_BLOCK - 1));
    }

    return result;
}

#ifdef FRINFC_READONLY_NDEF

NFCSTATUS 
phFriNfc_ISO15693_ConvertToReadOnly (
    phFriNfc_NdefMap_t  *psNdefMap)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;    
    phFriNfc_ISO15693Cont_t     *ps_iso_15693_con = 
                                &(psNdefMap->ISO15693Container);

    psNdefMap->State = ISO15693_READ_ONLY_NDEF;
    /* READ CC bytes */
    ps_iso_15693_con->ndef_seq = (uint8_t)ISO15693_RD_BEFORE_WR_CC;
    ps_iso_15693_con->current_block = 0;

    result = phFriNfc_ISO15693_H_ReadWrite (psNdefMap, 
                                    ISO15693_READ_COMMAND, NULL, 0);

    return result;
}

#endif /* #ifdef FRINFC_READONLY_NDEF */


void 
phFriNfc_ISO15693_Process (
    void        *pContext,
    NFCSTATUS   Status)
{
    phFriNfc_NdefMap_t      *psNdefMap = 
                            (phFriNfc_NdefMap_t *)pContext;

    if ((NFCSTATUS_SUCCESS & PHNFCSTBLOWER) == (Status & PHNFCSTBLOWER))
    {
        switch (psNdefMap->State) 
        {
            case ISO15693_CHECK_NDEF:
            {
                /* State = CHECK NDEF in progress */
                Status = phFriNfc_ISO15693_H_ProcessCheckNdef (psNdefMap);
                break;
            }

            case ISO15693_READ_NDEF:
            {
                /* State = READ NDEF in progress */
                Status = phFriNfc_ISO15693_H_ProcessReadNdef (psNdefMap);
                break;
            }

            case ISO15693_WRITE_NDEF:
            {
                /* State = WRITE NDEF in progress */
                Status = phFriNfc_ISO15693_H_ProcessWriteNdef (psNdefMap);
                break;
            }

#ifdef FRINFC_READONLY_NDEF
            case ISO15693_READ_ONLY_NDEF:
            {
                /* State = RAD ONLY NDEF in progress */
                Status = phFriNfc_ISO15693_H_ProcessReadOnly (psNdefMap);
                break;
            }
#endif /* #ifdef FRINFC_READONLY_NDEF */

            default:
            {
                break;
            }
        }
    }

    /* Call for the Completion Routine*/
    if (NFCSTATUS_PENDING != Status)
    {
        phFriNfc_ISO15693_H_Complete(psNdefMap, Status);
    }
}

/************************** END external functions *********************/

#endif /* #ifndef PH_FRINFC_MAP_ISO15693_DISABLED */
