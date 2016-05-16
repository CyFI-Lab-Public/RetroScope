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
 * \file  phFriNfc_MifStdFormat.c
 * \brief NFC Ndef Formatting For Mifare standard card.
 *
 * Project: NFC-FRI
 *
 * $Date: Tue Oct 20 20:13:03 2009 $
 * $Author: ing02260 $
 * $Revision: 1.9 $
 * $Aliases: NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
 *
 */

#include <phFriNfc_MifStdFormat.h>
#include <phFriNfc_OvrHal.h>

/*! \ingroup grp_file_attributes
 *  \name NDEF Mapping
 *
 * File: \ref phFriNfc_MifStdFormat.c
 *
 */
/*@{*/
#define PHFRINFCMIFSTDFMT_FILEREVISION "$Revision: 1.9 $"
#define PHFRINFCMIFSTDFMT_FILEALIASES  "$Aliases: NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"
/*@}*/

/*!
 * \brief \copydoc page_ovr Helper function for Mifare standard. This function fills the
 * send buffer for transceive function
 */
static void phFriNfc_MfStd_H_FillSendBuf(phFriNfc_sNdefSmtCrdFmt_t      *NdefSmtCrdFmt,
                                        uint16_t                         BlockNo);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare standard. This function authenticates
 *  a block or a sector from the card.
 */
static NFCSTATUS phFriNfc_MfStd_H_Transceive(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare standard. This function calls
 *  disconnect.
 */
static NFCSTATUS phFriNfc_MfStd_H_CallDisCon(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt,
                                             NFCSTATUS                    Status);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare standard. This function calls
 *  disconnect.
 */
static NFCSTATUS phFriNfc_MfStd_H_CallCon(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt);

#ifndef PH_HAL4_ENABLE
/*!
 * \brief \copydoc page_ovr Helper function for Mifare standard. This function calls
 *  disconnect.
 */
static NFCSTATUS phFriNfc_MfStd_H_CallPoll(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt);
#endif /* #ifndef PH_HAL4_ENABLE */

/*!
 * \brief \copydoc page_ovr Helper function for Mifare standard. This function shall process the
 * poll call.
 */
static NFCSTATUS phFriNfc_MfStd_H_ProCon(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare standard. This function shall process the
 * authenticate call.
 */
static NFCSTATUS phFriNfc_MfStd_H_ProAuth(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare standard. This function shall process the
 * read access bit call.
 */
static NFCSTATUS phFriNfc_MfStd_H_ProRdSectTr(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare standard. This function shall process the
 * write access bit call.
 */
static NFCSTATUS phFriNfc_MfStd_H_ProWrSectTr(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare standard. This function writes the
 * sector trailer using the block number.
 */
static NFCSTATUS phFriNfc_MfStd_H_WrRdAuth(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare standard. This function checks the
 * access bits of each sector trailer.
 */
static uint32_t phFriNfc_MfStd_H_ChkAcsBit(uint16_t                 BlockNo,
                                           const uint8_t                    *RecvBuf,
                                           const uint8_t            AcsBits1[],
                                           const uint8_t            AcsBits2[]);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare standard. This function change the
 * authentication state and change the block number if required
 */
static void phFriNfc_MfStd_H_ChangeAuthSt(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare standard. This function finds the
 * contiguous ndef compliant blocks.
 */
static void phFriNfc_MfStd_H_NdefComplSect(uint8_t      CardTypes,
                                           uint8_t      Sector[]);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare standard. This function writes the
 * MAD block values.
 */
static NFCSTATUS phFriNfc_MfStd_H_ProWrMADBlk(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt);

/*!
* \brief \copydoc page_ovr Helper function for Mifare standard. This function shall process
* the error status of the authentication
*/
static NFCSTATUS phFriNfc_MfStd_H_ProErrAuth(phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt);

/*!
* \brief \copydoc page_ovr Helper function for Mifare standard. This function shall process
* the error status of the writing sector trailer
*/
static NFCSTATUS phFriNfc_MfStd_H_ErrWrSectTr(phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt);

/*!
* \brief \copydoc page_ovr Helper function for Mifare standard. This function shall process
* the error status of the reading sector trailer
*/
static NFCSTATUS phFriNfc_MfStd_H_ErrRdSectTr(phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt);

/*!
* \brief \copydoc page_ovr Helper function for Mifare standard. This function shall process
* the error status of the writing sector trailer
*/
static NFCSTATUS phFriNfc_MfStd_H_ProUpdMADBlk(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt);

/*!
* \brief \copydoc page_ovr Helper function for Mifare standard. This function shall store the
* ndef compliant in the MAD array which will be later used for updating the MAD sector
*/
static void phFriNfc_MfStd_H_StrNdefData(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt);

/*!
* \brief \copydoc page_ovr Helper function for Mifare standard. This function shall find the ndef compliant
* and calculate the block number to write the NDEF TLV
*/
static void phFriNfc_MfStd_H_BlkNoToWrTLV(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt);

static int phFriNfc_MfStd_MemCompare ( void *s1, void *s2, unsigned int n );


void phFriNfc_MfStd_Reset(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt)
{
    uint8_t NfcForSectArray[] = PH_FRINFC_SMTCRDFMT_NFCFORUMSECT_KEYA_ACS_BIT,
            MADSectArray[] = PH_FRINFC_SMTCRDFMT_MSTD_MADSECT_KEYA_ACS_BIT_1K;

    /* Authentication state */
    NdefSmtCrdFmt->AddInfo.MfStdInfo.AuthState = PH_FRINFC_MFSTD_FMT_VAL_1;

    /* Set default key for A or B */
    (void)memset(NdefSmtCrdFmt->AddInfo.MfStdInfo.Default_KeyA_OR_B,
                PH_FRINFC_MFSTD_FMT_DEFAULT_KEY, /* 0xFF */
                PH_FRINFC_MFSTD_FMT_VAL_6);

    /* MAD sector key A */
    (void)memcpy(NdefSmtCrdFmt->AddInfo.MfStdInfo.MADSect_KeyA,
                MADSectArray, //PH_FRINFC_MFSTD_FMT_VAL_0,
                PH_FRINFC_MFSTD_FMT_VAL_6);

    /* Copy access bits for MAD sectors */
    (void)memcpy(NdefSmtCrdFmt->AddInfo.MfStdInfo.MADSect_AccessBits,
                &MADSectArray[PH_FRINFC_MFSTD_FMT_VAL_6],
                PH_FRINFC_MFSTD_FMT_VAL_3);

    /* NFC forum sector key A */
    (void)memcpy(NdefSmtCrdFmt->AddInfo.MfStdInfo.NFCForumSect_KeyA,
                NfcForSectArray, //PH_FRINFC_MFSTD_FMT_VAL_0,
                PH_FRINFC_MFSTD_FMT_VAL_6);

    /* Copy access bits for NFC forum sectors */
    (void)memcpy(NdefSmtCrdFmt->AddInfo.MfStdInfo.NFCForumSect_AccessBits,
                &NfcForSectArray[PH_FRINFC_MFSTD_FMT_VAL_6],
                PH_FRINFC_MFSTD_FMT_VAL_3);

    /* Sector compliant array initialised to 0 */
    (void)memset(NdefSmtCrdFmt->AddInfo.MfStdInfo.SectCompl,
                PH_FRINFC_MFSTD_FMT_VAL_0, /* 0x00 */
                PH_FRINFC_MFSTD_FMT_MAX_SECT_IND_4K);

    NdefSmtCrdFmt->AddInfo.MfStdInfo.WrMADBlkFlag = (uint8_t)PH_FRINFC_MFSTD_FMT_VAL_0;
    NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk = (uint8_t)PH_FRINFC_MFSTD_FMT_NOT_A_MAD_BLK;

}

NFCSTATUS phFriNfc_MfStd_Format( phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt, const uint8_t *ScrtKeyB )
{
    NFCSTATUS               Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                                NFCSTATUS_INVALID_PARAMETER);
    uint8_t                 index = PH_FRINFC_MFSTD_FMT_VAL_0;

    if(ScrtKeyB != NULL)
    {
        NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk =
            PH_FRINFC_MFSTD_FMT_NOT_A_MAD_BLK;
        /* Store Key B in the context */
        while(index < PH_FRINFC_MFSTD_FMT_VAL_6)
        {
            NdefSmtCrdFmt->AddInfo.MfStdInfo.ScrtKeyB[index] = ScrtKeyB[index];
            index++;
        }
        /* Set the state */
        NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_AUTH_SECT;
        /* Initialise current block to the first sector trailer */
        NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock = PH_FRINFC_MFSTD_FMT_VAL_3;
        /* Set the authenticate state */
        NdefSmtCrdFmt->AddInfo.MfStdInfo.AuthState = PH_FRINFC_MFSTD_FMT_AUTH_DEF_KEY;
        /* Start authentication */
        Result = phFriNfc_MfStd_H_WrRdAuth(NdefSmtCrdFmt);
    }
    return Result;
}

void phFriNfc_MfStd_Process(void        *Context,
                            NFCSTATUS   Status)
{
    phFriNfc_sNdefSmtCrdFmt_t  *NdefSmtCrdFmt = (phFriNfc_sNdefSmtCrdFmt_t *)Context;
    /* Copy the formatting status */
    NdefSmtCrdFmt->FmtProcStatus = Status;
    if(Status == NFCSTATUS_SUCCESS)
    {
        switch(NdefSmtCrdFmt->State)
        {
        case PH_FRINFC_MFSTD_FMT_AUTH_SECT:
            Status = phFriNfc_MfStd_H_ProAuth(NdefSmtCrdFmt);
            break;

        case PH_FRINFC_MFSTD_FMT_DIS_CON:
#ifndef PH_HAL4_ENABLE
            Status = phFriNfc_MfStd_H_CallPoll(NdefSmtCrdFmt);
            break;

        case PH_FRINFC_MFSTD_FMT_POLL:
#endif /* #ifndef PH_HAL4_ENABLE */
            Status = phFriNfc_MfStd_H_CallCon(NdefSmtCrdFmt);
            break;

        case PH_FRINFC_MFSTD_FMT_CON:
            Status = phFriNfc_MfStd_H_ProCon(NdefSmtCrdFmt);
            break;

        case PH_FRINFC_MFSTD_FMT_RD_SECT_TR:
            Status = phFriNfc_MfStd_H_ProRdSectTr(NdefSmtCrdFmt);
            break;

        case PH_FRINFC_MFSTD_FMT_WR_SECT_TR:
            Status = phFriNfc_MfStd_H_ProWrSectTr(NdefSmtCrdFmt);
            break;

        case PH_FRINFC_MFSTD_FMT_WR_MAD_BLK:
            Status = phFriNfc_MfStd_H_ProWrMADBlk(NdefSmtCrdFmt);
            break;

        case PH_FRINFC_MFSTD_FMT_WR_TLV:
            break;

        case PH_FRINFC_MFSTD_FMT_UPD_MAD_BLK:
            Status = phFriNfc_MfStd_H_ProUpdMADBlk(NdefSmtCrdFmt);
            break;

        default:
            Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                NFCSTATUS_INVALID_DEVICE_REQUEST);
            break;
        }
    }
    else
    {
        switch(NdefSmtCrdFmt->State)
        {
        case PH_FRINFC_MFSTD_FMT_AUTH_SECT:
            Status = phFriNfc_MfStd_H_ProErrAuth(NdefSmtCrdFmt);
            break;

        case PH_FRINFC_MFSTD_FMT_WR_SECT_TR:
            Status = phFriNfc_MfStd_H_ErrWrSectTr(NdefSmtCrdFmt);
            break;

        case PH_FRINFC_MFSTD_FMT_RD_SECT_TR:
            Status = phFriNfc_MfStd_H_ErrRdSectTr(NdefSmtCrdFmt);
            break;

        default:
            Status = NdefSmtCrdFmt->FmtProcStatus;
            break;
        }
    }

    /* Status is not success then call completion routine */
    if(Status != NFCSTATUS_PENDING)
    {
        phFriNfc_SmtCrdFmt_HCrHandler(NdefSmtCrdFmt, Status);
    }
}

static void phFriNfc_MfStd_H_FillSendBuf(phFriNfc_sNdefSmtCrdFmt_t      *NdefSmtCrdFmt,
                                        uint16_t                         BlockNo)
{
    void        *mem = NULL;
    uint8_t     MADSectTr1k[] = PH_FRINFC_SMTCRDFMT_MSTD_MADSECT_KEYA_ACS_BIT_1K, /* MAD key A,
                                                                            Access bits and GPB of MAD sector */
                MADSectTr4k[] = PH_FRINFC_SMTCRDFMT_MSTD_MADSECT_KEYA_ACS_BIT_4K, /* MAD key A,
                                                                                    Access bits and GPB of MAD sector */
                NFCSectTr[] = PH_FRINFC_SMTCRDFMT_NFCFORUMSECT_KEYA_ACS_BIT, /* NFC forum key A,
                                                                             Access bits and GPB of NFC sector */
                NDEFMsgTLV[16] = {0x03, 0x00, 0xFE, 0x00, 0x00, 0x00, /* NDEF message TLV (INITIALISED state) */
                                  0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                                  0x00, 0x00, 0x00, 0x00},
                MADBlk[16] = {0x0F, 0x00, 0x03, 0xE1, 0x03, 0xE1,
                              0x03, 0xE1, 0x03, 0xE1,
                              0x03, 0xE1, 0x03, 0xE1, 0x03, 0xE1};
    /* Block number in send buffer */
    NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFSTD_FMT_VAL_0] = (uint8_t)BlockNo;
    /* Initialise send receive length */
    *NdefSmtCrdFmt->SendRecvLength = PH_FRINFC_MFSTD_FMT_MAX_RECV_LENGTH;

    /* Depending on the different state, fill the send buffer */
    switch(NdefSmtCrdFmt->State)
    {
        case PH_FRINFC_MFSTD_FMT_AUTH_SECT:
            /* Depending on the authentication state, fill the send buffer */
            switch(NdefSmtCrdFmt->AddInfo.MfStdInfo.AuthState)
            {
                case PH_FRINFC_MFSTD_FMT_AUTH_DEF_KEY:
                case PH_FRINFC_MFSTD_FMT_AUTH_KEYB:
                    /* Fill send buffer with the default key */
                    PH_FRINFC_MFSTD_FMT_AUTH_SEND_BUF_DEF(mem);
                break;

                case PH_FRINFC_MFSTD_FMT_AUTH_NFC_KEY:
                    /* Fill send buffer with NFC forum sector key */
                    PH_FRINFC_MFSTD_FMT_AUTH_SEND_BUF_NFCSECT_KEYA(mem);
                break;

                case PH_FRINFC_MFSTD_FMT_AUTH_SCRT_KEYB:
                    /* Fill send buffer with NFC forum sector key */
                    PH_FRINFC_MFSTD_FMT_AUTH_SEND_BUF_SCRT_KEY(mem);
                    break;

                case PH_FRINFC_MFSTD_FMT_AUTH_MAD_KEY:
                default:
                    /* Fill send buffer with MAD sector key */
                    PH_FRINFC_MFSTD_FMT_AUTH_SEND_BUF_MADSECT_KEYA(mem);
                break;
            }
        break;

        case PH_FRINFC_MFSTD_FMT_RD_SECT_TR:
#ifdef PH_HAL4_ENABLE
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareRead;
#else
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareCmdListMifareRead;
#endif /* #ifdef PH_HAL4_ENABLE */

            /* Send length is always one for read operation */
            NdefSmtCrdFmt->SendLength = PH_FRINFC_MFSTD_FMT_VAL_1;
        break;

        case PH_FRINFC_MFSTD_FMT_WR_SECT_TR:
            /* Fill send buffer for writing sector trailer */
#ifdef PH_HAL4_ENABLE
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareWrite16;
#else
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareCmdListMifareWrite16;
#endif /* #ifdef PH_HAL4_ENABLE */
            /* Copy the relevant sector trailer value in the buffer */
            switch(NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock)
            {
            case PH_FRINFC_MFSTD_FMT_VAL_3:
                if (NdefSmtCrdFmt->CardType == PH_FRINFC_SMTCRDFMT_MFSTD_1K_CRD)
                {
                    (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFSTD_FMT_VAL_1],
                                MADSectTr1k,
                                sizeof(MADSectTr1k));
                }
                else
                {
                    (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFSTD_FMT_VAL_1],
                                MADSectTr4k,
                                sizeof(MADSectTr4k));
                }
                break;
            case 67:
                (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFSTD_FMT_VAL_1],
                            MADSectTr4k,
                            sizeof(MADSectTr4k));
                break;
            default:
                (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFSTD_FMT_VAL_1],
                            NFCSectTr,
                            sizeof(NFCSectTr));
                break;
            }
            (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFSTD_FMT_VAL_11],
                NdefSmtCrdFmt->AddInfo.MfStdInfo.ScrtKeyB,
                sizeof(NdefSmtCrdFmt->AddInfo.MfStdInfo.ScrtKeyB));

            /* Send length is always 17 for write operation */
            NdefSmtCrdFmt->SendLength = PH_FRINFC_MFSTD_FMT_WR_SEND_LENGTH;
        break;

        case PH_FRINFC_MFSTD_FMT_WR_TLV:
            /* Fill send buffer for writing TLV */
#ifdef PH_HAL4_ENABLE
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareWrite16;
#else
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareCmdListMifareWrite16;
#endif /* #ifdef PH_HAL4_ENABLE */
            /* Copy the NDEF message TLV */
            (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFSTD_FMT_VAL_1],
                        NDEFMsgTLV, sizeof(NDEFMsgTLV));
            /* Send length is always 17 for write operation */
            NdefSmtCrdFmt->SendLength = PH_FRINFC_MFSTD_FMT_WR_SEND_LENGTH;
        break;

        case PH_FRINFC_MFSTD_FMT_WR_MAD_BLK:
            /* Fill send buffer for writing MAD block */
#ifdef PH_HAL4_ENABLE
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareWrite16;
#else
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareCmdListMifareWrite16;
#endif /* #ifdef PH_HAL4_ENABLE */

            if((BlockNo == PH_FRINFC_MFSTD_FMT_VAL_2) ||
                (BlockNo == 65) || (BlockNo == 66))
            {
                /* MAD block number 2, 65 and 66 has 0x03, 0xE1 in the
                    first two bytes */
                MADBlk[PH_FRINFC_MFSTD_FMT_VAL_0] = 0x03;
                MADBlk[PH_FRINFC_MFSTD_FMT_VAL_1] = 0xE1;
            }
            /* Copy the MAD Block values */
            (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFSTD_FMT_VAL_1],
                            MADBlk, sizeof(MADBlk));
            /* Send length is always 17 for write operation */
            NdefSmtCrdFmt->SendLength = PH_FRINFC_MFSTD_FMT_WR_SEND_LENGTH;
        break;

        case PH_FRINFC_MFSTD_FMT_UPD_MAD_BLK:
        default:
            /* Fill send buffer for writing MAD block */
#ifdef PH_HAL4_ENABLE
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareWrite16;
#else
            NdefSmtCrdFmt->Cmd.MfCmd = phHal_eMifareCmdListMifareWrite16;
#endif /* #ifdef PH_HAL4_ENABLE */
            NdefSmtCrdFmt->SendLength = PH_FRINFC_MFSTD_FMT_WR_SEND_LENGTH;
            switch(NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk)
            {
            case PH_FRINFC_MFSTD_FMT_MAD_BLK_1:
                (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFSTD_FMT_VAL_1],
                            NdefSmtCrdFmt->AddInfo.MfStdInfo.MADSectBlk,
                            (PH_FRINFC_MFSTD_FMT_WR_SEND_LENGTH - PH_FRINFC_MFSTD_FMT_VAL_1));
                break;

            case PH_FRINFC_MFSTD_FMT_MAD_BLK_2:
                (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFSTD_FMT_VAL_1],
                    &NdefSmtCrdFmt->AddInfo.MfStdInfo.MADSectBlk[16],
                    (PH_FRINFC_MFSTD_FMT_WR_SEND_LENGTH - PH_FRINFC_MFSTD_FMT_VAL_1));
                break;

            case PH_FRINFC_MFSTD_FMT_MAD_BLK_64:
                (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFSTD_FMT_VAL_1],
                    &NdefSmtCrdFmt->AddInfo.MfStdInfo.MADSectBlk[32],
                    (PH_FRINFC_MFSTD_FMT_WR_SEND_LENGTH - PH_FRINFC_MFSTD_FMT_VAL_1));
                break;

            case PH_FRINFC_MFSTD_FMT_MAD_BLK_65:
                (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFSTD_FMT_VAL_1],
                    &NdefSmtCrdFmt->AddInfo.MfStdInfo.MADSectBlk[48],
                    (PH_FRINFC_MFSTD_FMT_WR_SEND_LENGTH - PH_FRINFC_MFSTD_FMT_VAL_1));
                break;

            case PH_FRINFC_MFSTD_FMT_MAD_BLK_66:
            default:
                (void)memcpy(&NdefSmtCrdFmt->SendRecvBuf[PH_FRINFC_MFSTD_FMT_VAL_1],
                    &NdefSmtCrdFmt->AddInfo.MfStdInfo.MADSectBlk[64],
                    (PH_FRINFC_MFSTD_FMT_WR_SEND_LENGTH - PH_FRINFC_MFSTD_FMT_VAL_1));
                break;
            }
            break;
    }
    PHNFC_UNUSED_VARIABLE(mem);
}

static NFCSTATUS phFriNfc_MfStd_H_Transceive(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt)
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

static NFCSTATUS phFriNfc_MfStd_H_CallDisCon(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt,
                                             NFCSTATUS                    Status)
{
    NFCSTATUS   Result = Status;

    /*Set Ndef State*/
    NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_DIS_CON;

#ifdef PH_HAL4_ENABLE

    /*Call the Overlapped HAL POLL function */
    Result =  phFriNfc_OvrHal_Reconnect( NdefSmtCrdFmt->LowerDevice,
                                    &NdefSmtCrdFmt->SmtCrdFmtCompletionInfo,
                                    NdefSmtCrdFmt->psRemoteDevInfo);
#else
    /*Call the Overlapped HAL POLL function */
    Result =  phFriNfc_OvrHal_Disconnect( NdefSmtCrdFmt->LowerDevice,
                                        &NdefSmtCrdFmt->SmtCrdFmtCompletionInfo,
                                        NdefSmtCrdFmt->psRemoteDevInfo);
#endif /* #ifdef PH_HAL4_ENABLE */

    return Result;
}

static NFCSTATUS phFriNfc_MfStd_H_CallCon(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    /*Set Ndef State*/
    NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_CON;

    /*Call the Overlapped HAL POLL function */
#ifdef PH_HAL4_ENABLE
    Result =  phFriNfc_OvrHal_Connect(  NdefSmtCrdFmt->LowerDevice,
                                        &NdefSmtCrdFmt->SmtCrdFmtCompletionInfo,
                                        NdefSmtCrdFmt->psRemoteDevInfo,
                                        NdefSmtCrdFmt->AddInfo.MfStdInfo.DevInputParam);
#else
    Result =  phFriNfc_OvrHal_Connect(  NdefSmtCrdFmt->LowerDevice,
                                        &NdefSmtCrdFmt->SmtCrdFmtCompletionInfo,
                                        phHal_eOpModesMifare,
                                        NdefSmtCrdFmt->psRemoteDevInfo,
                                        NdefSmtCrdFmt->AddInfo.MfStdInfo.DevInputParam);
#endif /* #ifdef PH_HAL4_ENABLE */

    return Result;
}

#ifndef PH_HAL4_ENABLE

static NFCSTATUS phFriNfc_MfStd_H_CallPoll(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    /*Set ndef State*/
    NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_POLL;
    /* Opmodes */
    NdefSmtCrdFmt->OpModeType[PH_FRINFC_MFSTD_FMT_VAL_0] = phHal_eOpModesMifare;
    NdefSmtCrdFmt->OpModeType[PH_FRINFC_MFSTD_FMT_VAL_1] = phHal_eOpModesArrayTerminator;

    /* Number of devices to poll */
    NdefSmtCrdFmt->AddInfo.MfStdInfo.NoOfDevices = PH_FRINFC_MFSTD_FMT_VAL_1;

    /*Call the Overlapped HAL POLL function */
    Result =  phFriNfc_OvrHal_Poll( NdefSmtCrdFmt->LowerDevice,
                                    &NdefSmtCrdFmt->SmtCrdFmtCompletionInfo,
                                    NdefSmtCrdFmt->OpModeType,
                                    NdefSmtCrdFmt->psRemoteDevInfo,
                                    &NdefSmtCrdFmt->AddInfo.MfStdInfo.NoOfDevices,
                                    NdefSmtCrdFmt->AddInfo.MfStdInfo.DevInputParam);
    return Result;
}

#endif /* #ifndef PH_HAL4_ENABLE */

static NFCSTATUS phFriNfc_MfStd_H_ProCon(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint8_t     Buffer[1] = {PH_FRINFC_MFSTD_FMT_NDEF_COMPL},
                index = PH_FRINFC_MFSTD_FMT_VAL_1;
    uint32_t    memcompare = PH_FRINFC_MFSTD_FMT_VAL_1;

    phFriNfc_MfStd_H_ChangeAuthSt(NdefSmtCrdFmt);
    if(PH_FRINFC_MFSTD_FMT_CUR_BLK_CHK)
    {
        PH_FRINFC_MFSTD_FMT_CHK_END_OF_CARD();
    }
    else
    {
        /* Set the state */
        NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_AUTH_SECT;
        /* Start authentication */
        Result = phFriNfc_MfStd_H_WrRdAuth(NdefSmtCrdFmt);
    }
    return Result;
}

static NFCSTATUS phFriNfc_MfStd_H_ProAuth(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;

    /* Depending on the authentication key check the  */
    switch(NdefSmtCrdFmt->AddInfo.MfStdInfo.AuthState)
    {
        case PH_FRINFC_MFSTD_FMT_AUTH_DEF_KEY:
            if((NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock ==
                PH_FRINFC_MFSTD_FMT_VAL_3) &&
                (NdefSmtCrdFmt->AddInfo.MfStdInfo.WrMADBlkFlag ==
                PH_FRINFC_MFSTD_FMT_VAL_0))
            {
                /* Authenticate with default key for block 3 is successful,
                    so fill the MAD block of sector 0 */
                NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock =
                                    PH_FRINFC_MFSTD_FMT_VAL_1;
                /* Write the MAD block */
                NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_WR_MAD_BLK;
            }
            else if((NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock == 67)
                && (NdefSmtCrdFmt->AddInfo.MfStdInfo.WrMADBlkFlag ==
                PH_FRINFC_MFSTD_FMT_VAL_0))
            {
                /* Authenticate with default key for block 3 is successful,
                so fill the MAD block of sector 64 */
                NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock = 64;
                /* Write the MAD block */
                NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_WR_MAD_BLK;
            }
            else
            {
                /* Not a MAD sector */
                NdefSmtCrdFmt->AddInfo.MfStdInfo.WrMADBlkFlag =
                                        PH_FRINFC_MFSTD_FMT_VAL_0;
                /* Write the MAD block */
                NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_WR_SECT_TR;
            }
        break;

        case PH_FRINFC_MFSTD_FMT_AUTH_KEYB:
            if((NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk ==
                PH_FRINFC_MFSTD_FMT_MAD_BLK_1) ||
                (NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk ==
                PH_FRINFC_MFSTD_FMT_MAD_BLK_2) ||
                (NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk ==
                PH_FRINFC_MFSTD_FMT_MAD_BLK_64) ||
                (NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk ==
                PH_FRINFC_MFSTD_FMT_MAD_BLK_65) ||
                (NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk ==
                PH_FRINFC_MFSTD_FMT_MAD_BLK_66))
            {
                NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock =
                            NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk;
                NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_UPD_MAD_BLK;
            }
            else
            {
                NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk =
                                PH_FRINFC_MFSTD_FMT_NOT_A_MAD_BLK;
                NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_WR_SECT_TR;
            }

        break;

        case PH_FRINFC_MFSTD_FMT_AUTH_SCRT_KEYB:
            if((NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk ==
                PH_FRINFC_MFSTD_FMT_MAD_BLK_1) ||
                (NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk ==
                PH_FRINFC_MFSTD_FMT_MAD_BLK_2) ||
                (NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk ==
                PH_FRINFC_MFSTD_FMT_MAD_BLK_64) ||
                (NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk ==
                PH_FRINFC_MFSTD_FMT_MAD_BLK_65) ||
                (NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk ==
                PH_FRINFC_MFSTD_FMT_MAD_BLK_66))
            {
                NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock =
                    NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk;
                NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_UPD_MAD_BLK;
            }
            else
            {
                NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk =
                    PH_FRINFC_MFSTD_FMT_NOT_A_MAD_BLK;
                NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_WR_SECT_TR;
            }
            break;

        case PH_FRINFC_MFSTD_FMT_AUTH_NFC_KEY:
        case PH_FRINFC_MFSTD_FMT_AUTH_MAD_KEY:
        default:
            if((NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk ==
                PH_FRINFC_MFSTD_FMT_MAD_BLK_66) ||
                (NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk ==
                PH_FRINFC_MFSTD_FMT_MAD_BLK_2))
            {
                /* Updating the MAD block is complete */
                NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk =
                                PH_FRINFC_MFSTD_FMT_NOT_A_MAD_BLK;
                /* If Mifare 4k card, write the TLV */
                NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_WR_TLV;
            }
            else
            {
                /* Depending on the sector trailer, check the access bit */
                NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_RD_SECT_TR;
            }
        break;
    }
    /* Call read, write or authenticate */
    Result = phFriNfc_MfStd_H_WrRdAuth(NdefSmtCrdFmt);
    return Result;
}

static NFCSTATUS phFriNfc_MfStd_H_ErrWrSectTr( phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt )
{
    NFCSTATUS   Result = NdefSmtCrdFmt->FmtProcStatus;
    /* If default key A is used for authentication and if write fails, then try to
    authenticate using key B*/
    if(NdefSmtCrdFmt->AddInfo.MfStdInfo.AuthState ==
        PH_FRINFC_MFSTD_FMT_AUTH_DEF_KEY)
    {
        /* Change the state to authentication */
        NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_AUTH_SECT;
        /* internal authenticate state = key B */
        NdefSmtCrdFmt->AddInfo.MfStdInfo.AuthState = PH_FRINFC_MFSTD_FMT_AUTH_KEYB;
        /* Now call authenticate */
        Result = phFriNfc_MfStd_H_WrRdAuth(NdefSmtCrdFmt);
    }
    else
    {
        Result = phFriNfc_MfStd_H_ProWrSectTr(NdefSmtCrdFmt);
    }
    return Result;
}
static NFCSTATUS phFriNfc_MfStd_H_ProRdSectTr(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint8_t     Buffer[1] = {PH_FRINFC_MFSTD_FMT_NDEF_COMPL},
                index = PH_FRINFC_MFSTD_FMT_VAL_1,
                SectIndex = PH_FRINFC_MFSTD_FMT_VAL_0;
    uint32_t    memcompare = PH_FRINFC_MFSTD_FMT_VAL_1;

    /* Calculate sector index */
    SectIndex = (uint8_t)PH_FRINFC_MFSTD_FMT_SECT_INDEX_CALC;

    /* Depending on the sector trailer, check the access bit */
    memcompare = phFriNfc_MfStd_H_ChkAcsBit(NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock,
                                            NdefSmtCrdFmt->SendRecvBuf,
                                            NdefSmtCrdFmt->AddInfo.MfStdInfo.MADSect_AccessBits,
                                            NdefSmtCrdFmt->AddInfo.MfStdInfo.NFCForumSect_AccessBits);

    /* Check the sector for ndef compliance */
    NdefSmtCrdFmt->AddInfo.MfStdInfo.SectCompl[SectIndex] = (uint8_t)
                ((memcompare != PH_FRINFC_MFSTD_FMT_VAL_0)?
                PH_FRINFC_MFSTD_FMT_NON_NDEF_COMPL:
                PH_FRINFC_MFSTD_FMT_NDEF_COMPL);

    /* Increment the current block */
    PH_FRINFC_MFSTD_FMT_CUR_BLK_INC();
    SectIndex++;
    if(PH_FRINFC_MFSTD_FMT_CUR_BLK_CHK)
    {
       PH_FRINFC_MFSTD_FMT_CHK_END_OF_CARD();
    }
    else
    {
        /* Set the state */
        NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_AUTH_SECT;
        /* Set the authenticate state */
        NdefSmtCrdFmt->AddInfo.MfStdInfo.AuthState =
                            PH_FRINFC_MFSTD_FMT_AUTH_DEF_KEY;
        /* Start authentication */
        Result = phFriNfc_MfStd_H_WrRdAuth(NdefSmtCrdFmt);
    }
    return Result;
}

static NFCSTATUS phFriNfc_MfStd_H_ProWrSectTr(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint8_t     Buffer[1] = {PH_FRINFC_MFSTD_FMT_NDEF_COMPL},
                index = PH_FRINFC_MFSTD_FMT_VAL_1,
                SectIndex = PH_FRINFC_MFSTD_FMT_VAL_0;
    uint32_t    memcompare = PH_FRINFC_MFSTD_FMT_VAL_1;

    /* Calculate sector index */
    SectIndex = (uint8_t)PH_FRINFC_MFSTD_FMT_SECT_INDEX_CALC;

    /* Sector is ndef compliance */
    NdefSmtCrdFmt->AddInfo.MfStdInfo.SectCompl[SectIndex] = (uint8_t)
                    ((NdefSmtCrdFmt->FmtProcStatus != NFCSTATUS_SUCCESS)?
                        PH_FRINFC_MFSTD_FMT_NON_NDEF_COMPL:
                        PH_FRINFC_MFSTD_FMT_NDEF_COMPL);

    /* Increment the current block */
    PH_FRINFC_MFSTD_FMT_CUR_BLK_INC();
    SectIndex++;
    if(PH_FRINFC_MFSTD_FMT_CUR_BLK_CHK)
    {
        PH_FRINFC_MFSTD_FMT_CHK_END_OF_CARD();
    }
    else
    {
        /* Set the state */
        NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_AUTH_SECT;
        /* Set the authenticate state */
        NdefSmtCrdFmt->AddInfo.MfStdInfo.AuthState =
                            PH_FRINFC_MFSTD_FMT_AUTH_DEF_KEY;
        /* Start authentication */
        Result = phFriNfc_MfStd_H_WrRdAuth(NdefSmtCrdFmt);
    }
    return Result;
}

static uint32_t phFriNfc_MfStd_H_ChkAcsBit(uint16_t                 BlockNo,
                                           const uint8_t            *RecvBuf,
                                           const uint8_t            AcsBits1[],
                                           const uint8_t            AcsBits2[])
{
    uint32_t    mem = PH_FRINFC_MFSTD_FMT_VAL_0;

    /* Compare the access bits read from the sector trailer */
    mem = (uint32_t)(((BlockNo == PH_FRINFC_MFSTD_FMT_VAL_3) ||
                    (BlockNo == 67))?
                    phFriNfc_MfStd_MemCompare((void*)&RecvBuf[PH_FRINFC_MFSTD_FMT_VAL_6],
                            (void*)AcsBits1,
                            PH_FRINFC_MFSTD_FMT_VAL_3):
                    phFriNfc_MfStd_MemCompare((void*)&RecvBuf[PH_FRINFC_MFSTD_FMT_VAL_6],
                            (void*)AcsBits2,
                            PH_FRINFC_MFSTD_FMT_VAL_3));

    return mem;
}

static NFCSTATUS phFriNfc_MfStd_H_WrRdAuth(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    /* Fill send buffer and send length */
    phFriNfc_MfStd_H_FillSendBuf(NdefSmtCrdFmt,
                                 NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock);
    /* Call ovrhal transceive */
    Result = phFriNfc_MfStd_H_Transceive(NdefSmtCrdFmt);

    return Result;
}

static void phFriNfc_MfStd_H_ChangeAuthSt(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt)
{
    uint8_t     SectIndex = PH_FRINFC_MFSTD_FMT_VAL_0;

    if( NdefSmtCrdFmt->AddInfo.MfStdInfo.AuthState ==
        PH_FRINFC_MFSTD_FMT_AUTH_SCRT_KEYB)
    {
        /* Calculate sector index */
        SectIndex = (uint8_t)PH_FRINFC_MFSTD_FMT_SECT_INDEX_CALC;

        /* Check the sector for ndef compliance */
        NdefSmtCrdFmt->AddInfo.MfStdInfo.SectCompl[SectIndex] =
                    PH_FRINFC_MFSTD_FMT_NON_NDEF_COMPL;

        PH_FRINFC_MFSTD_FMT_CUR_BLK_INC();
    }
    PH_FRINFC_MFSTD_FMT_NXT_AUTH_STATE();
}

static void phFriNfc_MfStd_H_NdefComplSect(uint8_t      CardTypes,
                                           uint8_t      Sector[])
{
    uint8_t     count = PH_FRINFC_MFSTD_FMT_VAL_0,
                NdefComplSectMax = PH_FRINFC_MFSTD_FMT_VAL_0,
                NdefComplSectTemp = PH_FRINFC_MFSTD_FMT_VAL_1,
                SectIndex = PH_FRINFC_MFSTD_FMT_VAL_0,
                MaxCont = PH_FRINFC_MFSTD_FMT_VAL_0,
                MaxSect = PH_FRINFC_MFSTD_FMT_VAL_0;

    /* Get the maximum sector depending on the sector */
    MaxSect = ((CardTypes == PH_FRINFC_SMTCRDFMT_MFSTD_1K_CRD)?
                PH_FRINFC_MFSTD_FMT_MAX_SECT_IND_1K:
                PH_FRINFC_MFSTD_FMT_MAX_SECT_IND_4K);
    /* Sector index */
    NdefComplSectTemp = SectIndex = PH_FRINFC_MFSTD_FMT_VAL_1;
    /* Check the sector index depending on the card type */
    while(((SectIndex < PH_FRINFC_MFSTD_FMT_MAX_SECT_IND_4K) &&
        (CardTypes == PH_FRINFC_SMTCRDFMT_MFSTD_4K_CRD)) ||
        ((SectIndex < PH_FRINFC_MFSTD_FMT_MAX_SECT_IND_1K) &&
        (CardTypes == PH_FRINFC_SMTCRDFMT_MFSTD_1K_CRD)))
    {
        if (Sector[SectIndex] ==
            PH_FRINFC_MFSTD_FMT_NON_NDEF_COMPL)
        {
            if (MaxCont > count)
            {
                /* Store the maximum contiguous */
                NdefComplSectMax = NdefComplSectTemp;
                count = MaxCont;
            }
            MaxCont = PH_FRINFC_MFSTD_FMT_VAL_0;
            /* Increment the sector index */
            PH_FRINFC_MFSTD_FMT_INCR_SECT;
            /* Get the next compliant sector */
            NdefComplSectTemp = SectIndex;
        }
        else
        {
            /* Increment the sector index */
            PH_FRINFC_MFSTD_FMT_INCR_SECT;
        }
        MaxCont ++;

    }
    if (MaxCont > count)
    {
        /* Store the maximum contiguous */
        NdefComplSectMax = NdefComplSectTemp;
        count = MaxCont;
    }
    /* Set the sector value has non ndef compliant which are not present with
    contiguous ndef compliant sectors */
    if((((count < (MaxSect - PH_FRINFC_MFSTD_FMT_VAL_1)) && (CardTypes
        == PH_FRINFC_SMTCRDFMT_MFSTD_1K_CRD)) ||
        ((count < (MaxSect - PH_FRINFC_MFSTD_FMT_VAL_2)) && (CardTypes
        == PH_FRINFC_SMTCRDFMT_MFSTD_4K_CRD))) &&
        ((NdefComplSectMax > PH_FRINFC_MFSTD_FMT_VAL_0) &&
        (NdefComplSectMax < (MaxSect - PH_FRINFC_MFSTD_FMT_VAL_2))))
    {
        (void)memset(&Sector[PH_FRINFC_MFSTD_FMT_VAL_1],
            PH_FRINFC_MFSTD_FMT_NON_NDEF_COMPL,
            (NdefComplSectMax - PH_FRINFC_MFSTD_FMT_VAL_1));

        (void)memset(&Sector[(NdefComplSectMax + count)],
            PH_FRINFC_MFSTD_FMT_NON_NDEF_COMPL,
            (MaxSect - (NdefComplSectMax + count)));
    }
}


static NFCSTATUS phFriNfc_MfStd_H_ProWrMADBlk(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;

    switch(NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock)
    {
    case PH_FRINFC_MFSTD_FMT_VAL_1:
        /* MAD blocks, still not completed */
        NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_WR_MAD_BLK;
        /* MAD block number 2 */
        NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock =
                            PH_FRINFC_MFSTD_FMT_VAL_2;
        break;

    case PH_FRINFC_MFSTD_FMT_VAL_2:
        /* Now write to MAD block is completed */
        NdefSmtCrdFmt->AddInfo.MfStdInfo.WrMADBlkFlag =
                                        PH_FRINFC_MFSTD_FMT_VAL_1;
        /* Now write the sector trailer, so change the state */
        NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_WR_SECT_TR;
        /* MAD block number 3 = Sector trailer */
        NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock =
                            PH_FRINFC_MFSTD_FMT_VAL_3;
        break;

    case 64:
        /* MAD blocks, still not completed */
        NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_WR_MAD_BLK;
        NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock = 65;
        break;

    case 65:
        /* MAD blocks, still not completed */
        NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_WR_MAD_BLK;
        NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock = 66;
        break;

    case 66:
    default:
        /* Now write to MAD block is completed */
        NdefSmtCrdFmt->AddInfo.MfStdInfo.WrMADBlkFlag =
                                        PH_FRINFC_MFSTD_FMT_VAL_1;
        /* Now write the sector trailer, so change the state */
        NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_WR_SECT_TR;
        NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock = 67;
        break;

    }
    /* Write the block */
    Result = phFriNfc_MfStd_H_WrRdAuth(NdefSmtCrdFmt);

    return Result;
}

static NFCSTATUS phFriNfc_MfStd_H_ProErrAuth( phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt )
{
    NFCSTATUS   Result = NdefSmtCrdFmt->FmtProcStatus;
    uint8_t     Buffer[1] = {PH_FRINFC_MFSTD_FMT_NDEF_COMPL},
                index = PH_FRINFC_MFSTD_FMT_VAL_1;
    uint32_t    memcompare = PH_FRINFC_MFSTD_FMT_VAL_1;

    if ((NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock == 67) &&
        (NdefSmtCrdFmt->AddInfo.MfStdInfo.AuthState ==
                    PH_FRINFC_MFSTD_FMT_AUTH_SCRT_KEYB))
    {
        /* Error in the MAD sector 16, so the remaining sector
        information cant be updated */
        (void)memset(&NdefSmtCrdFmt->AddInfo.MfStdInfo.SectCompl[16],
                    PH_FRINFC_MFSTD_FMT_NON_NDEF_COMPL,
                    (PH_FRINFC_MFSTD_FMT_MAX_SECT_IND_4K - 16));
        PH_FRINFC_MFSTD_FMT_CHK_END_OF_CARD();
    }
    else if(((NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock >
        PH_FRINFC_MFSTD_FMT_VAL_3) &&
        (NdefSmtCrdFmt->AddInfo.MfStdInfo.AuthState !=
        PH_FRINFC_MFSTD_FMT_AUTH_SCRT_KEYB)) ||
        ((NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock ==
        PH_FRINFC_MFSTD_FMT_VAL_3) &&
        (NdefSmtCrdFmt->AddInfo.MfStdInfo.AuthState <
        PH_FRINFC_MFSTD_FMT_AUTH_SCRT_KEYB)))
    {
        /* Authenticate failed, so disconnect, poll and connect */
        Result = phFriNfc_MfStd_H_CallDisCon(NdefSmtCrdFmt,
                                             Result);
    }
    else
    {
        if (NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock ==
            PH_FRINFC_MFSTD_FMT_VAL_3)
        {
            (void)memset(NdefSmtCrdFmt->AddInfo.MfStdInfo.SectCompl,
                        PH_FRINFC_MFSTD_FMT_NON_NDEF_COMPL,
                        PH_FRINFC_MFSTD_FMT_MAX_SECT_IND_4K);
        }
    }

    return Result;
}

static NFCSTATUS phFriNfc_MfStd_H_ProUpdMADBlk(phFriNfc_sNdefSmtCrdFmt_t    *NdefSmtCrdFmt)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    switch(NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk)
    {
    case PH_FRINFC_MFSTD_FMT_MAD_BLK_1:
        /* Write the next MAD Block */
        NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk = (uint8_t)
                                PH_FRINFC_MFSTD_FMT_MAD_BLK_2;
        NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock =
                                PH_FRINFC_MFSTD_FMT_MAD_BLK_2;
        break;

    case PH_FRINFC_MFSTD_FMT_MAD_BLK_2:
    case PH_FRINFC_MFSTD_FMT_MAD_BLK_66:
        if((NdefSmtCrdFmt->CardType == PH_FRINFC_SMTCRDFMT_MFSTD_1K_CRD) ||
           (NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock ==
           PH_FRINFC_MFSTD_FMT_MAD_BLK_66))
        {
            /* Get the block from where the TLV has to be written */
            phFriNfc_MfStd_H_BlkNoToWrTLV(NdefSmtCrdFmt);

            NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_AUTH_SECT;
            NdefSmtCrdFmt->AddInfo.MfStdInfo.AuthState =
                                    PH_FRINFC_MFSTD_FMT_AUTH_NFC_KEY;
        }
        else
        {
            /* Write the next MAD Block */
            NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk = (uint8_t)
                            PH_FRINFC_MFSTD_FMT_MAD_BLK_64;
                NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock =
                            PH_FRINFC_MFSTD_FMT_MAD_BLK_64;
            NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_AUTH_SECT;
            NdefSmtCrdFmt->AddInfo.MfStdInfo.AuthState =
                                PH_FRINFC_MFSTD_FMT_AUTH_SCRT_KEYB;
        }
        break;

    case PH_FRINFC_MFSTD_FMT_MAD_BLK_64:
        /* Write the next MAD Block */
        NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk = (uint8_t)
                                PH_FRINFC_MFSTD_FMT_MAD_BLK_65;
        NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock =
                                PH_FRINFC_MFSTD_FMT_MAD_BLK_65;
        break;

    case PH_FRINFC_MFSTD_FMT_MAD_BLK_65:
    default:
        /* Write the next MAD Block */
        NdefSmtCrdFmt->AddInfo.MfStdInfo.UpdMADBlk = (uint8_t)
                                    PH_FRINFC_MFSTD_FMT_MAD_BLK_66;
        NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock =
                                    PH_FRINFC_MFSTD_FMT_MAD_BLK_66;
        break;
    }
    Result = phFriNfc_MfStd_H_WrRdAuth(NdefSmtCrdFmt);
    return Result;
}

static void phFriNfc_MfStd_H_StrNdefData( phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt )
{
    uint8_t     SectIndex = PH_FRINFC_MFSTD_FMT_VAL_1,
                index = PH_FRINFC_MFSTD_FMT_VAL_0;
    
    (void)memset(NdefSmtCrdFmt->AddInfo.MfStdInfo.MADSectBlk,
                0x00,
                PH_FRINFC_MFSTD_FMT_MAX_SECT_IND_4K);

    /* Zeroth sector of the Mifare card is MAD sector, CRC is 0x14 */
    NdefSmtCrdFmt->AddInfo.MfStdInfo.MADSectBlk[PH_FRINFC_MFSTD_FMT_VAL_0] = 0x14;
    /* Info byte is 0x01, because the NDEF application is written and as per the MAD spec, 
    the value for miscellaneous application is 0x01 */
    NdefSmtCrdFmt->AddInfo.MfStdInfo.MADSectBlk[PH_FRINFC_MFSTD_FMT_VAL_1] = 0x01;

    if(NdefSmtCrdFmt->CardType == PH_FRINFC_SMTCRDFMT_MFSTD_4K_CRD)
    {
        /* If 4k card then sector number 16 is MAD sector, CRC is 0xE8 */
        NdefSmtCrdFmt->AddInfo.MfStdInfo.MADSectBlk[32] = 0xE8;
        /* Info byte is 0x01, because the NDEF application is written and 
            as per the MAD spec, 
        the value for miscellaneous application is 0x01 */
        NdefSmtCrdFmt->AddInfo.MfStdInfo.MADSectBlk[33] = 0x01;
    }
    /* NDEF information has to be updated from */
    index = PH_FRINFC_MFSTD_FMT_VAL_2;
    /* Depending on the card type, check the sector index */
    while (((SectIndex < PH_FRINFC_MFSTD_FMT_MAX_SECT_IND_4K) &&
        (NdefSmtCrdFmt->CardType == PH_FRINFC_SMTCRDFMT_MFSTD_4K_CRD)) ||
        ((SectIndex < PH_FRINFC_MFSTD_FMT_MAX_SECT_IND_1K) &&
        (NdefSmtCrdFmt->CardType == PH_FRINFC_SMTCRDFMT_MFSTD_1K_CRD)))
    {
        /* Is the sector ndef compliant? */
        if(NdefSmtCrdFmt->AddInfo.MfStdInfo.SectCompl[SectIndex] ==
            PH_FRINFC_MFSTD_FMT_NDEF_COMPL)
        {
            /* Ndef compliant sector, update the MAD sector array
                in the context with values 0x03 and 0xE1
                0x03 and 0xE1 is NDEF information in MAD sector */
            NdefSmtCrdFmt->AddInfo.MfStdInfo.MADSectBlk[index] =
                                        PH_FRINFC_MFSTD_FMT_NDEF_INFO1;
            index++;
            NdefSmtCrdFmt->AddInfo.MfStdInfo.MADSectBlk[index] =
                                        PH_FRINFC_MFSTD_FMT_NDEF_INFO2;
            index++;
        }
        else
        {
            /* Not a Ndef compliant sector, update the MAD sector array
                in the context with values 0x00 and 0x00
                0x00 and 0x00 is NDEF information in MAD sector */
            NdefSmtCrdFmt->AddInfo.MfStdInfo.MADSectBlk[index] = 0x00;
            index++;
            NdefSmtCrdFmt->AddInfo.MfStdInfo.MADSectBlk[index] = 0x00;
            index++;
        }
        /* Go to next sector */
        SectIndex++;
        /* is the sector, a MAD sector 16? */
        if(SectIndex == PH_FRINFC_MFSTD_FMT_MAX_SECT_IND_1K)
        {
            /* MAD sector number 16, so skip this sector */
            SectIndex = SectIndex + PH_FRINFC_MFSTD_FMT_VAL_1;
            index = index + PH_FRINFC_MFSTD_FMT_VAL_2;
        }
    }
}

static void phFriNfc_MfStd_H_BlkNoToWrTLV( phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt )
{
    uint8_t     SectIndex = (uint8_t)PH_FRINFC_MFSTD_FMT_VAL_1;
    while (((SectIndex < (uint8_t)PH_FRINFC_MFSTD_FMT_MAX_SECT_IND_4K) &&
        (NdefSmtCrdFmt->CardType == (uint8_t)PH_FRINFC_SMTCRDFMT_MFSTD_4K_CRD)) ||
        ((SectIndex < (uint8_t)PH_FRINFC_MFSTD_FMT_MAX_SECT_IND_1K) &&
        (NdefSmtCrdFmt->CardType == (uint8_t)PH_FRINFC_SMTCRDFMT_MFSTD_1K_CRD)))
    {
        if (NdefSmtCrdFmt->AddInfo.MfStdInfo.SectCompl[SectIndex] ==
            (uint8_t)PH_FRINFC_MFSTD_FMT_NDEF_COMPL)
        {
            /* Get the first NFC forum sector's block */
            NdefSmtCrdFmt->AddInfo.MfStdInfo.CurrentBlock = (uint16_t)
                                          (((SectIndex & 0xE0) >= 32)?
                                        (128 + ((SectIndex % 32) * 16)):
                                        (SectIndex * (uint8_t)PH_FRINFC_MFSTD_FMT_VAL_4));
            /* Break out of the loop */
            SectIndex += (uint8_t)PH_FRINFC_MFSTD_FMT_MAX_SECT_IND_4K;
        }
        SectIndex++;
    }
}

static NFCSTATUS phFriNfc_MfStd_H_ErrRdSectTr( phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt )
{
    NFCSTATUS   Result = NdefSmtCrdFmt->FmtProcStatus;
    uint8_t     Buffer[1] = {PH_FRINFC_MFSTD_FMT_NDEF_COMPL},
                index = PH_FRINFC_MFSTD_FMT_VAL_1,
                SectIndex = PH_FRINFC_MFSTD_FMT_VAL_0;
    uint32_t    memcompare = PH_FRINFC_MFSTD_FMT_VAL_1;
    /* If default key A is used for authentication and if write fails, then try to
    authenticate using key B*/
    if(NdefSmtCrdFmt->AddInfo.MfStdInfo.AuthState ==
        PH_FRINFC_MFSTD_FMT_AUTH_DEF_KEY)
    {
        /* Change the state to authentication */
        NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_AUTH_SECT;
        /* internal authenticate state = key B */
        NdefSmtCrdFmt->AddInfo.MfStdInfo.AuthState = PH_FRINFC_MFSTD_FMT_AUTH_KEYB;
        /* Now call authenticate */
        Result = phFriNfc_MfStd_H_WrRdAuth(NdefSmtCrdFmt);
    }
    else
    {
        /* Calculate sector index */
        SectIndex = (uint8_t)PH_FRINFC_MFSTD_FMT_SECT_INDEX_CALC;

        /* Sector is ndef compliance */
        NdefSmtCrdFmt->AddInfo.MfStdInfo.SectCompl[SectIndex] = (uint8_t)
                            ((NdefSmtCrdFmt->FmtProcStatus != NFCSTATUS_SUCCESS)?
                            PH_FRINFC_MFSTD_FMT_NON_NDEF_COMPL:
                            PH_FRINFC_MFSTD_FMT_NDEF_COMPL);

        /* Increment the current block */
        PH_FRINFC_MFSTD_FMT_CUR_BLK_INC();
        SectIndex++;
        if(PH_FRINFC_MFSTD_FMT_CUR_BLK_CHK)
        {
            PH_FRINFC_MFSTD_FMT_CHK_END_OF_CARD();
        }
        else
        {
            /* Set the state */
            NdefSmtCrdFmt->State = PH_FRINFC_MFSTD_FMT_AUTH_SECT;
            /* Set the authenticate state */
            NdefSmtCrdFmt->AddInfo.MfStdInfo.AuthState =
                PH_FRINFC_MFSTD_FMT_AUTH_DEF_KEY;
            /* Start authentication */
            Result = phFriNfc_MfStd_H_WrRdAuth(NdefSmtCrdFmt);
        }
    }
    return Result;
}

static int phFriNfc_MfStd_MemCompare( void *s1, void *s2, unsigned int n )
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



#ifdef UNIT_TEST
#include <phUnitTestNfc_MifStdFormat_static.c>
#endif
