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
* \file  phFriNfc_ISO15693Format.c
* \brief This component encapsulates different format functinalities ,
*        for the ISO-15693 card. 
*
* Project: NFC-FRI
*
* $Date:  $
* $Author: ing02260 $
* $Revision: 1.0 $
* $Aliases:  $
*
*/

#ifndef PH_FRINFC_FMT_ISO15693_DISABLED

#include <phNfcTypes.h>
#include <phFriNfc_OvrHal.h>
#include <phFriNfc_SmtCrdFmt.h>
#include <phFriNfc_ISO15693Format.h>


/****************************** Macro definitions start ********************************/
/* State for the format */
#define ISO15693_FORMAT                                 0x01U

/* Bytes per block in the ISO-15693 */
#define ISO15693_BYTES_PER_BLOCK                        0x04U

/* ISO-15693 Commands 
GET SYSTEM INFORMATION COMMAND
*/
#define ISO15693_GET_SYSTEM_INFO_CMD                    0x2BU
/* READ SINGLE BLOCK COMMAND */
#define ISO15693_RD_SINGLE_BLK_CMD                      0x20U
/* WRITE SINGLE BLOCK COMMAND */
#define ISO15693_WR_SINGLE_BLK_CMD                      0x21U
/* READ MULTIPLE BLOCK COMMAND */
#define ISO15693_RD_MULTIPLE_BLKS_CMD                   0x23U

/* CC bytes 
CC BYTE 0 - Magic Number - 0xE1
*/
#define ISO15693_CC_MAGIC_NUM                           0xE1U
/* CC BYTE 1 - Mapping version and READ WRITE settings 0x40
*/
#define ISO15693_CC_VER_RW                              0x40U
/* CC BYTE 2 - max size is calaculated using the byte 3 multiplied by 8 */
#define ISO15693_CC_MULTIPLE_FACTOR                     0x08U

/* Inventory command support mask for the CC byte 4 */
#define ISO15693_INVENTORY_CMD_MASK                     0x02U
/* Read MULTIPLE blocks support mask for CC byte 4 */
#define ISO15693_RDMULBLKS_CMD_MASK                     0x01U
/* Flags for the command */
#define ISO15693_FMT_FLAGS                              0x20U

/* Read two blocks */
#define ISO15693_RD_2_BLOCKS                            0x02U

/* TYPE identifier of the NDEF TLV */
#define ISO15693_NDEF_TLV_TYPE_ID                       0x03U
/* Terminator TLV identifier  */
#define ISO15693_TERMINATOR_TLV_ID                      0xFEU

/* UID 7th byte value shall be 0xE0 */
#define ISO15693_7TH_BYTE_UID_VALUE                     0xE0U
#define ISO15693_BYTE_7_INDEX                           0x07U

/* UID 6th byte value shall be 0x04 - NXP manufacturer */
#define ISO15693_6TH_BYTE_UID_VALUE                     0x04U
#define ISO15693_BYTE_6_INDEX                           0x06U

#define ISO15693_EXTRA_RESPONSE_FLAG                    0x01U

#define ISO15693_GET_SYS_INFO_RESP_LEN                  0x0EU
#define ISO15693_DSFID_MASK                             0x01U
#define ISO15693_AFI_MASK                               0x02U
#define ISO15693_MAX_SIZE_MASK                          0x04U
#define ISO15693_ICREF_MASK                             0x08U
#define ISO15693_SKIP_DFSID                             0x01U
#define ISO15693_SKIP_AFI                               0x01U
#define ISO15693_BLOCK_SIZE_IN_BYTES_MASK               0x1FU


/* MAXimum size of ICODE SLI/X */
#define ISO15693_SLI_X_MAX_SIZE                         112U
/* MAXimum size of ICODE SLI/X - S */
#define ISO15693_SLI_X_S_MAX_SIZE                       160U
/* MAXimum size of ICODE SLI/X - L */
#define ISO15693_SLI_X_L_MAX_SIZE                       32U
/****************************** Macro definitions end ********************************/

/****************************** Data structures start ********************************/
typedef enum phFriNfc_ISO15693_FormatSeq
{
    ISO15693_GET_SYS_INFO, 
    ISO15693_RD_SINGLE_BLK_CHECK,
    ISO15693_WRITE_CC_FMT, 
    ISO15693_WRITE_NDEF_TLV
}phFriNfc_ISO15693_FormatSeq_t;
/****************************** Data structures end ********************************/

/*********************** Static function declarations start ***********************/
static 
NFCSTATUS 
phFriNfc_ISO15693_H_ProFormat (
    phFriNfc_sNdefSmtCrdFmt_t   *psNdefSmtCrdFmt);

static 
NFCSTATUS 
phFriNfc_ISO15693_H_GetMaxDataSize (
    phFriNfc_sNdefSmtCrdFmt_t   *psNdefSmtCrdFmt, 
    uint8_t                     *p_recv_buf, 
    uint8_t                     recv_length);

static 
NFCSTATUS 
phFriNfc_ISO15693_H_FmtReadWrite (
    phFriNfc_sNdefSmtCrdFmt_t   *psNdefSmtCrdFmt, 
    uint8_t                     command, 
    uint8_t                     *p_data, 
    uint8_t                     data_length);
/*********************** Static function declarations end ***********************/

/*********************** Static function definitions start ***********************/

static 
NFCSTATUS 
phFriNfc_ISO15693_H_FmtReadWrite (
    phFriNfc_sNdefSmtCrdFmt_t   *psNdefSmtCrdFmt, 
    uint8_t                     command, 
    uint8_t                     *p_data, 
    uint8_t                     data_length)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    uint8_t                     send_index = 0;

    /* set the data for additional data exchange*/
    psNdefSmtCrdFmt->psDepAdditionalInfo.DepFlags.MetaChaining = 0;
    psNdefSmtCrdFmt->psDepAdditionalInfo.DepFlags.NADPresent = 0;
    psNdefSmtCrdFmt->psDepAdditionalInfo.NAD = 0;

    psNdefSmtCrdFmt->SmtCrdFmtCompletionInfo.CompletionRoutine = 
                                            phFriNfc_ISO15693_FmtProcess;
    psNdefSmtCrdFmt->SmtCrdFmtCompletionInfo.Context = psNdefSmtCrdFmt;

    *psNdefSmtCrdFmt->SendRecvLength = PH_FRINFC_SMTCRDFMT_MAX_SEND_RECV_BUF_SIZE;

    psNdefSmtCrdFmt->Cmd.Iso15693Cmd = phHal_eIso15693_Cmd;

    *(psNdefSmtCrdFmt->SendRecvBuf + send_index) = (uint8_t)ISO15693_FMT_FLAGS;
    send_index = (uint8_t)(send_index + 1);

    *(psNdefSmtCrdFmt->SendRecvBuf + send_index) = (uint8_t)command;
    send_index = (uint8_t)(send_index + 1);

    (void)memcpy ((void *)(psNdefSmtCrdFmt->SendRecvBuf + send_index), 
        (void *)psNdefSmtCrdFmt->psRemoteDevInfo->RemoteDevInfo.Iso15693_Info.Uid, 
        psNdefSmtCrdFmt->psRemoteDevInfo->RemoteDevInfo.Iso15693_Info.UidLength);
    send_index = (uint8_t)(send_index + 
            psNdefSmtCrdFmt->psRemoteDevInfo->RemoteDevInfo.Iso15693_Info.UidLength);

    switch (command)
    {        
        case ISO15693_WR_SINGLE_BLK_CMD:
        case ISO15693_RD_MULTIPLE_BLKS_CMD:
        {
            *(psNdefSmtCrdFmt->SendRecvBuf + send_index) = (uint8_t)
                        psNdefSmtCrdFmt->AddInfo.s_iso15693_info.current_block;
            send_index = (uint8_t)(send_index + 1);

            if (data_length)
            {
                (void)memcpy ((void *)(psNdefSmtCrdFmt->SendRecvBuf + send_index), 
                            (void *)p_data, data_length);
                send_index = (uint8_t)(send_index + data_length);
            }
            else
            {
                result = PHNFCSTVAL (CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                    NFCSTATUS_INVALID_DEVICE_REQUEST);
            }
            break;
        }

        case ISO15693_RD_SINGLE_BLK_CMD:
        {
            *(psNdefSmtCrdFmt->SendRecvBuf + send_index) = (uint8_t)
                        psNdefSmtCrdFmt->AddInfo.s_iso15693_info.current_block;
            send_index = (uint8_t)(send_index + 1);
            break;
        }

        case ISO15693_GET_SYSTEM_INFO_CMD:
        {
            /* Dont do anything */
            break;
        }

        default:
        {
            result = PHNFCSTVAL (CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                NFCSTATUS_INVALID_DEVICE_REQUEST);
            break;
        }
    }

    psNdefSmtCrdFmt->SendLength = send_index;

    if (!result)
    {
        result = phFriNfc_OvrHal_Transceive(psNdefSmtCrdFmt->LowerDevice,
                                            &psNdefSmtCrdFmt->SmtCrdFmtCompletionInfo,
                                            psNdefSmtCrdFmt->psRemoteDevInfo,
                                            psNdefSmtCrdFmt->Cmd,
                                            &psNdefSmtCrdFmt->psDepAdditionalInfo,
                                            psNdefSmtCrdFmt->SendRecvBuf,
                                            psNdefSmtCrdFmt->SendLength,
                                            psNdefSmtCrdFmt->SendRecvBuf,
                                            psNdefSmtCrdFmt->SendRecvLength);
    }

    return result;
}

static 
NFCSTATUS 
phFriNfc_ISO15693_H_GetMaxDataSize (
    phFriNfc_sNdefSmtCrdFmt_t   *psNdefSmtCrdFmt, 
    uint8_t                     *p_recv_buf, 
    uint8_t                     recv_length)
{
    NFCSTATUS                       result = NFCSTATUS_SUCCESS;
    phFriNfc_ISO15693_AddInfo_t     *ps_iso15693_info = 
                                    &(psNdefSmtCrdFmt->AddInfo.s_iso15693_info);
    phHal_sIso15693Info_t           *ps_rem_iso_15693_info = 
                        &(psNdefSmtCrdFmt->psRemoteDevInfo->RemoteDevInfo.Iso15693_Info);
    uint8_t                         recv_index = 0;

    if ((ISO15693_GET_SYS_INFO_RESP_LEN == recv_length)
        && (ISO15693_MAX_SIZE_MASK == (*p_recv_buf & ISO15693_MAX_SIZE_MASK)))
    {
        uint8_t information_flag = *p_recv_buf;
        /* MAX size is present in the system information and 
        also response length is correct */
        recv_index = (uint8_t)(recv_index + 1);

        if (!phOsalNfc_MemCompare ((void *)ps_rem_iso_15693_info->Uid, 
                                (void *)(p_recv_buf + recv_index), 
                                ps_rem_iso_15693_info->UidLength))
        {
            /* UID comaparision successful */
            uint8_t                 no_of_blocks = 0;
            uint8_t                 blk_size_in_bytes = 0;
            uint8_t                 ic_reference = 0;

            /* So skip the UID size compared in the received buffer */
            recv_index = (uint8_t)(recv_index + 
                                    ps_rem_iso_15693_info->UidLength);

            if (information_flag & ISO15693_DSFID_MASK) {
                /* Skip DFSID  */
                recv_index = (uint8_t)(recv_index + ISO15693_SKIP_DFSID);
            }
            if (information_flag & ISO15693_AFI_MASK) {
                /* Skip AFI  */
                recv_index = (uint8_t)(recv_index + ISO15693_SKIP_AFI);
            }

            /* To get the number of blocks in the card */
            no_of_blocks = (uint8_t)(*(p_recv_buf + recv_index) + 1);
            recv_index = (uint8_t)(recv_index + 1);

            /* To get the each block size in bytes */
            blk_size_in_bytes = (uint8_t)((*(p_recv_buf + recv_index) 
                                & ISO15693_BLOCK_SIZE_IN_BYTES_MASK) + 1);
            recv_index = (uint8_t)(recv_index + 1);

            if (information_flag & ISO15693_ICREF_MASK) {
                /* Get the IC reference */
                ic_reference = (uint8_t)(*(p_recv_buf + recv_index));
                if (ic_reference == 0x03) {
                    no_of_blocks = 8;
                }
            }

            /* calculate maximum data size in the card */
            ps_iso15693_info->max_data_size = (uint16_t)
                                        (no_of_blocks * blk_size_in_bytes);

        }
        else
        {
            result = PHNFCSTVAL (CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                NFCSTATUS_INVALID_DEVICE_REQUEST);
        }                     
    }
    else
    {
        result = PHNFCSTVAL (CID_FRI_NFC_NDEF_SMTCRDFMT, 
                            NFCSTATUS_INVALID_DEVICE_REQUEST);
    }


    return result;
}

static 
NFCSTATUS 
phFriNfc_ISO15693_H_ProFormat (
    phFriNfc_sNdefSmtCrdFmt_t *psNdefSmtCrdFmt)
{
    NFCSTATUS                       result = NFCSTATUS_SUCCESS;
    phFriNfc_ISO15693_AddInfo_t     *ps_iso15693_info = 
                                    &(psNdefSmtCrdFmt->AddInfo.s_iso15693_info);
    phFriNfc_ISO15693_FormatSeq_t   e_format_seq = 
                                    (phFriNfc_ISO15693_FormatSeq_t)
                                    ps_iso15693_info->format_seq;
    uint8_t                         command_type = 0;
    uint8_t                         a_send_byte[ISO15693_BYTES_PER_BLOCK] = {0};
    uint8_t                         send_length = 0;
    uint8_t                         send_index = 0;
    uint8_t                         format_complete = FALSE;
    
    switch (e_format_seq)
    {
        case ISO15693_GET_SYS_INFO:
        {
            /* RESPONSE received for GET SYSTEM INFO  */

            if (!phFriNfc_ISO15693_H_GetMaxDataSize (psNdefSmtCrdFmt, 
                (psNdefSmtCrdFmt->SendRecvBuf + ISO15693_EXTRA_RESPONSE_FLAG), 
                (uint8_t)(*psNdefSmtCrdFmt->SendRecvLength - 
                ISO15693_EXTRA_RESPONSE_FLAG)))
            {
                /* Send the READ SINGLE BLOCK COMMAND */
                command_type = ISO15693_RD_SINGLE_BLK_CMD;
                e_format_seq = ISO15693_RD_SINGLE_BLK_CHECK;

                /* Block number 0 to read */
                psNdefSmtCrdFmt->AddInfo.s_iso15693_info.current_block = 0x00;
            }
            else
            {
                result = PHNFCSTVAL (CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH);
            }
            break;
        }

        case ISO15693_RD_SINGLE_BLK_CHECK:
        {
            /* RESPONSE received for READ SINGLE BLOCK
            received*/

            /* Check if Card is really fresh
               First 4 bytes must be 0 for fresh card */

            if ((psNdefSmtCrdFmt->AddInfo.s_iso15693_info.current_block == 0x00) &&
                (psNdefSmtCrdFmt->SendRecvBuf[1] != 0x00 ||
                 psNdefSmtCrdFmt->SendRecvBuf[2] != 0x00 ||
                 psNdefSmtCrdFmt->SendRecvBuf[3] != 0x00 ||
                 psNdefSmtCrdFmt->SendRecvBuf[4] != 0x00))
            {
                result = PHNFCSTVAL (CID_FRI_NFC_NDEF_SMTCRDFMT, NFCSTATUS_INVALID_FORMAT);
            }
            else
            {
                /* prepare data for writing CC bytes */

                command_type = ISO15693_WR_SINGLE_BLK_CMD;
                e_format_seq = ISO15693_WRITE_CC_FMT;

                /* CC magic number */
                *a_send_byte = (uint8_t)ISO15693_CC_MAGIC_NUM;
                send_index = (uint8_t)(send_index + 1);

                /* CC Version and read/write access */
                *(a_send_byte + send_index) = (uint8_t) ISO15693_CC_VER_RW;
                send_index = (uint8_t)(send_index + 1);

                /* CC MAX data size, calculated during GET system information */
                *(a_send_byte + send_index) = (uint8_t) (ps_iso15693_info->max_data_size / ISO15693_CC_MULTIPLE_FACTOR);
                send_index = (uint8_t)(send_index + 1);

                switch (ps_iso15693_info->max_data_size)
                {
                    case ISO15693_SLI_X_MAX_SIZE:
                    {
                        /* For SLI tags : Inventory Page read not supported */
                        *(a_send_byte + send_index) = (uint8_t) ISO15693_RDMULBLKS_CMD_MASK;
                        break;
                    }

                    case ISO15693_SLI_X_S_MAX_SIZE:
                    {
                        /* For SLI - S tags : Read multiple blocks not supported */
                        *(a_send_byte + send_index) = (uint8_t) ISO15693_INVENTORY_CMD_MASK;
                        break;
                    }

                    case ISO15693_SLI_X_L_MAX_SIZE:
                    {
                        /* For SLI - L tags : Read multiple blocks not supported */
                        *(a_send_byte + send_index) = (uint8_t) ISO15693_INVENTORY_CMD_MASK;
                        break;
                    }

                    default:
                    {
                        result = PHNFCSTVAL (CID_FRI_NFC_NDEF_SMTCRDFMT, NFCSTATUS_INVALID_DEVICE_REQUEST);
                        break;
                    }
                }

                send_index = (uint8_t)(send_index + 1);

                send_length = sizeof (a_send_byte);
            }

            break;
        }

        case ISO15693_WRITE_CC_FMT:
        {
            /* CC byte write succcessful. 
            Prepare data for NDEF TLV writing */
            command_type = ISO15693_WR_SINGLE_BLK_CMD;
            e_format_seq = ISO15693_WRITE_NDEF_TLV;

            ps_iso15693_info->current_block = (uint16_t)
                        (ps_iso15693_info->current_block + 1);
            
            /* NDEF TLV - Type byte updated to 0x03 */
            *a_send_byte = (uint8_t)ISO15693_NDEF_TLV_TYPE_ID;
            send_index = (uint8_t)(send_index + 1);

            /* NDEF TLV - Length byte updated to 0 */
            *(a_send_byte + send_index) = 0;
            send_index = (uint8_t)(send_index + 1);

            /* Terminator TLV - value updated to 0xFEU */
            *(a_send_byte + send_index) = (uint8_t)
                            ISO15693_TERMINATOR_TLV_ID;
            send_index = (uint8_t)(send_index + 1);

            send_length = sizeof (a_send_byte);
            break;
        }

        case ISO15693_WRITE_NDEF_TLV:
        {
            /* SUCCESSFUL formatting complete */
            format_complete = TRUE;
            break;
        }

        default:
        {
            result = PHNFCSTVAL (CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                NFCSTATUS_INVALID_DEVICE_REQUEST);
            break;
        }
    }

    if ((!format_complete) && (!result))
    {
        result = phFriNfc_ISO15693_H_FmtReadWrite (psNdefSmtCrdFmt, 
                            command_type, a_send_byte, send_length);
    }

    ps_iso15693_info->format_seq = (uint8_t)e_format_seq; 
    return result;
}

/*********************** Static function definitions end ***********************/

/*********************** External function definitions start ***********************/
void 
phFriNfc_ISO15693_FmtReset (
    phFriNfc_sNdefSmtCrdFmt_t *psNdefSmtCrdFmt)
{
    /* reset to ISO15693 data structure */
    (void)memset((void *)&(psNdefSmtCrdFmt->AddInfo.s_iso15693_info), 
                0x00, sizeof (phFriNfc_ISO15693_AddInfo_t));
    psNdefSmtCrdFmt->FmtProcStatus = 0;
}

NFCSTATUS 
phFriNfc_ISO15693_Format (
    phFriNfc_sNdefSmtCrdFmt_t *psNdefSmtCrdFmt)
{
    NFCSTATUS                       result = NFCSTATUS_SUCCESS;
    phHal_sIso15693Info_t           *ps_rem_iso_15693_info = 
                        &(psNdefSmtCrdFmt->psRemoteDevInfo->RemoteDevInfo.Iso15693_Info);
    
    
    if ((ISO15693_7TH_BYTE_UID_VALUE == 
        ps_rem_iso_15693_info->Uid[ISO15693_BYTE_7_INDEX]) 
        && (ISO15693_6TH_BYTE_UID_VALUE == 
        ps_rem_iso_15693_info->Uid[ISO15693_BYTE_6_INDEX]))
    {
        /* Check if the card is manufactured by NXP (6th byte 
        index of UID value = 0x04 and the 
        last byte of UID is 0xE0, only then the card detected 
        is NDEF compliant */
        psNdefSmtCrdFmt->State = ISO15693_FORMAT;

        /* GET system information command to get the card size */
        result = phFriNfc_ISO15693_H_FmtReadWrite (psNdefSmtCrdFmt, 
                            ISO15693_GET_SYSTEM_INFO_CMD, NULL, 0);
    }
    else
    {
        result = PHNFCSTVAL (CID_FRI_NFC_NDEF_SMTCRDFMT, 
                            NFCSTATUS_INVALID_DEVICE_REQUEST);
    }

    return result;
}

void 
phFriNfc_ISO15693_FmtProcess (
    void        *pContext,
    NFCSTATUS   Status)
{
    phFriNfc_sNdefSmtCrdFmt_t      *psNdefSmtCrdFmt = 
                                    (phFriNfc_sNdefSmtCrdFmt_t *)pContext;
    phFriNfc_ISO15693_AddInfo_t     *ps_iso15693_info = 
                                    &(psNdefSmtCrdFmt->AddInfo.s_iso15693_info);

    if((NFCSTATUS_SUCCESS & PHNFCSTBLOWER) == (Status & PHNFCSTBLOWER))
    {
        if (ISO15693_FORMAT == psNdefSmtCrdFmt->State)
        {
            /* Check for further formatting */
            Status = phFriNfc_ISO15693_H_ProFormat (psNdefSmtCrdFmt);
        }
        else
        {
            Status = PHNFCSTVAL (CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                NFCSTATUS_INVALID_DEVICE_REQUEST);
        }
    }
    else
    {   
        Status = PHNFCSTVAL (CID_FRI_NFC_NDEF_SMTCRDFMT,
                            NFCSTATUS_FORMAT_ERROR);
    }

    /* Handle the all the error cases */
    if ((NFCSTATUS_PENDING & PHNFCSTBLOWER) != (Status & PHNFCSTBLOWER))
    {
        /* call respective CR */
        phFriNfc_SmtCrdFmt_HCrHandler (psNdefSmtCrdFmt, Status);
    }
}
/*********************** External function definitions end ***********************/


#endif /* #ifndef PH_FRINFC_FMT_ISO15693_DISABLED */

