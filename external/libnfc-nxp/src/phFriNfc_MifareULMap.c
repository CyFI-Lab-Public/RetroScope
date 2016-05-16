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
 * \file  phFriNfc_MifareUL.c
 * \brief This component encapsulates read/write/check ndef/process functionalities,
 *        for the Mifare UL card.   
 *
 * Project: NFC-FRI
 *
 * $Date: Wed Feb 17 15:18:08 2010 $
 * $Author: ing07385 $
 * $Revision: 1.36 $
 * $Aliases: NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
 *
 */

#ifndef PH_FRINFC_MAP_MIFAREUL_DISABLED

#include <phFriNfc_NdefMap.h>
#include <phFriNfc_MifareULMap.h>
#include <phFriNfc_MapTools.h>
#include <phFriNfc_OvrHal.h>
#include <phFriNfc.h>


/*! \ingroup grp_file_attributes
 *  \name NDEF Mapping
 *
 * File: \ref phFriNfc_MifareUL.c
 *
 */
/*@{*/
#define PHFRINFCNDEFMAP_FILEREVISION "$Revision: 1.35 $"
#define PHFRINFCNDEFMAP_FILEALIASES  "$Aliases: NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"
/*@}*/
/* Completion Helper */
static void phFriNfc_MifareUL_H_Complete(phFriNfc_NdefMap_t  *NdefMap,
                                        NFCSTATUS            Status);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare UL. This function reads  
 *  a 16 bytes from the card.
 */
static NFCSTATUS phFriNfc_MfUL_H_Rd16Bytes( phFriNfc_NdefMap_t  *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare UL. This function is   
 *  to find NDEF TLV
 */
static NFCSTATUS phFriNfc_MfUL_H_findNDEFTLV(phFriNfc_NdefMap_t *NdefMap, 
                                        uint8_t                 *CRFlag);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare UL. This function is   
 *  to check the completing the reading 16 bytes
 */
static NFCSTATUS phFriNfc_MfUL_H_Chk16Bytes(phFriNfc_NdefMap_t   *NdefMap,
                                            uint16_t             TempLength);


/*!
 * \brief \copydoc page_ovr Helper function for Mifare UL. This function is   
 *  to read 16 bytes for the finding the ndef TLV
 */
static NFCSTATUS phFriNfc_MfUL_H_RdCardfindNdefTLV( phFriNfc_NdefMap_t  *NdefMap,
                                                 uint8_t                BlockNo);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare UL. This function is   
 *  to check the remaining size of the 3 byte of length field in TLV
 */
static NFCSTATUS phFriNfc_MfUL_H_ChkRemainTLV(phFriNfc_NdefMap_t  *NdefMap,
                                              uint8_t             *CRFlag);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare UL. This function is   
 *  to byte and block number of the next TLV in the card and updating the 
 *  remaining free space in the card
 */
static void phFriNfc_MfUL_H_UpdateLen(phFriNfc_NdefMap_t        *NdefMap,
                                      uint16_t                  DataLen);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare UL. Depending on the
 * operation (read/write/check ndef), the next function is called
 */
static NFCSTATUS phFriNfc_MfUL_H_NxtOp(phFriNfc_NdefMap_t       *NdefMap,
                                       uint8_t                  *CRFlag);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare UL function. This 
 * function is to copy the read bytes to the internal "ReadBuf" buffer 
 */
static NFCSTATUS phFriNfc_MfUL_H_CopyRdBytes(phFriNfc_NdefMap_t  *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare UL function. This 
 * function is to copy the read bytes to the user buffer 
 */
static NFCSTATUS phFriNfc_MfUL_H_CpDataToUserBuf(phFriNfc_NdefMap_t *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare UL function. This 
 * function is to write 4 bytes to 1 block in the card
 */
static NFCSTATUS   phFriNfc_MfUL_H_Wr4bytes(phFriNfc_NdefMap_t  *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare UL function. This 
 * function is to check the CC bytes in block 3 card
 */
static NFCSTATUS phFriNfc_MfUL_H_ChkCCBytes(phFriNfc_NdefMap_t  *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare UL function. This 
 * function is to read the TLVs and then start writing
 */
static NFCSTATUS phFriNfc_MfUL_H_RdBeforeWrite(phFriNfc_NdefMap_t  *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare UL function. This 
 * function is to call write operation after reading the NDEF TLV block
 */
static NFCSTATUS phFriNfc_MfUL_H_CallWrOp(phFriNfc_NdefMap_t        *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare UL function. This 
 * function is to process the written data
 */
static NFCSTATUS phFriNfc_MfUL_H_ProWrittenBytes(phFriNfc_NdefMap_t *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare UL function. This 
 * function is to fill the send buffer before write
 */
static NFCSTATUS phFriNfc_MfUL_H_fillSendBufToWr(phFriNfc_NdefMap_t *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare UL function. This 
 * function is to update the length L of the TLV
 */
static NFCSTATUS phFriNfc_MfUL_H_UpdateWrLen(phFriNfc_NdefMap_t *NdefMap);

/*!
 * \brief \copydoc page_ovr Helper function for Mifare UL function. This 
 * function is to write the terminator TLV after writing all the bytes
 */
static NFCSTATUS phFriNfc_MfUL_H_WrTermTLV(phFriNfc_NdefMap_t   *NdefMap);

#ifdef LOCK_BITS_CHECK_ENABLE 
    static 
    void
    phFriNfc_MfUL_H_ChkLockBits (
        phFriNfc_NdefMap_t *NdefMap);
#endif /* #ifdef LOCK_BITS_CHECK_ENABLE */

/*!
 * \brief \copydoc select sector function for Mifare UL function. This 
 * function is to write the terminator TLV after writing all the bytes
 */
static NFCSTATUS  phFriNfc_MfUL_H_SelectSector(phFriNfc_NdefMap_t  *NdefMap,
                                                   uint8_t              SectorNo,
                                                   uint8_t              CmdNo,
                                                   uint8_t              NextState);



static void phFriNfc_MfUL_H_UpdateCrc( uint8_t ch, 
                                          uint16_t *lpwCrc );

static void phFriNfc_MfUL_H_ComputeCrc( int      CRCType, 
                                 uint8_t  *Data, 
                                 int      Length,
                                 uint8_t  *TransmitFirst, 
                                 uint8_t  *TransmitSecond
                                 );

static void
phFriNfc_MfUL_CalcByteNum(phFriNfc_NdefMap_t *NdefMap);


#define CRC_A 0
#define CRC_B 1


NFCSTATUS phFriNfc_MifareUL_H_Reset(phFriNfc_NdefMap_t        *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    
    if ( NdefMap == NULL)
    {
        Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        /* TLV structure initialisation */
        NdefMap->TLVStruct.NdefTLVBlock = PH_FRINFC_NDEFMAP_MFUL_VAL4;
        NdefMap->TLVStruct.BytesRemainLinTLV = PH_FRINFC_NDEFMAP_MFUL_VAL0;
        NdefMap->TLVStruct.NdefTLVByte = PH_FRINFC_NDEFMAP_MFUL_VAL0;
        NdefMap->TLVStruct.NoLbytesinTLV = PH_FRINFC_NDEFMAP_MFUL_VAL0;
        NdefMap->TLVStruct.prevLenByteValue = PH_FRINFC_NDEFMAP_MFUL_VAL0;
        NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG0;
        NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG0;
        NdefMap->TLVStruct.ActualSize = PH_FRINFC_NDEFMAP_MFUL_VAL0;
        NdefMap->TLVStruct.SetTermTLVFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG0;
        NdefMap->TLVStruct.WrLenFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG0;

        /* Mifare UL container initialisation */
        NdefMap->MifareULContainer.ByteNumber = PH_FRINFC_NDEFMAP_MFUL_VAL0;
        NdefMap->MifareULContainer.CRindex = PH_FRINFC_NDEFMAP_MFUL_VAL0;
        NdefMap->MifareULContainer.CurrentSector = PH_FRINFC_NDEFMAP_MFUL_VAL0;
        NdefMap->MifareULContainer.CurrentBlock = PH_FRINFC_NDEFMAP_MFUL_VAL0;
        NdefMap->MifareULContainer.InternalLength = PH_FRINFC_NDEFMAP_MFUL_VAL0;
        NdefMap->MifareULContainer.ReadBufIndex = PH_FRINFC_NDEFMAP_MFUL_VAL0;
        NdefMap->MifareULContainer.ReadWriteCompleteFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG0;
        NdefMap->MifareULContainer.RemainingSize = PH_FRINFC_NDEFMAP_MFUL_VAL0;

        /* Fill all the structure related buffer to ZERO */
        (void)memset(NdefMap->TLVStruct.NdefTLVBuffer, 
                    PH_FRINFC_NDEFMAP_MFUL_VAL0, 
                    PH_FRINFC_NDEFMAP_MFUL_VAL4);
        (void)memset(NdefMap->MifareULContainer.Buffer, 
                    PH_FRINFC_NDEFMAP_MFUL_VAL0, 
                    PH_FRINFC_NDEFMAP_MFUL_VAL4);
        (void)memset(NdefMap->MifareULContainer.InternalBuf, 
                    PH_FRINFC_NDEFMAP_MFUL_VAL0, 
                    PH_FRINFC_NDEFMAP_MFUL_VAL4);
        (void)memset(NdefMap->MifareULContainer.ReadBuf, 
                    PH_FRINFC_NDEFMAP_MFUL_VAL0, 
                    PH_FRINFC_NDEFMAP_MFUL_VAL64);
    }
    return Result;
}

/*!
 * \brief Initiates Reading of NDEF information from the Mifare UL.
 *
 * It performs a reset of the state and starts the action (state machine).
 * A periodic call of the \ref phFriNfcNdefMap_Process has to be done once the action
 * has been triggered.
 */

NFCSTATUS phFriNfc_MifareUL_RdNdef( phFriNfc_NdefMap_t  *NdefMap,
                                    uint8_t             *PacketData,
                                    uint32_t            *PacketDataLength,
                                    uint8_t             Offset)
{
    NFCSTATUS                   Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                                    NFCSTATUS_INVALID_PARAMETER);
    
    if((NdefMap != NULL) && (PacketData != NULL) && (PacketDataLength != NULL) && 
        (*PacketDataLength != PH_FRINFC_NDEFMAP_MFUL_VAL0) && 
        (Offset <= PH_FRINFC_NDEFMAP_SEEK_BEGIN) && 
        (NdefMap->CompletionRoutine->CompletionRoutine != NULL) && 
        (NdefMap->CompletionRoutine->Context != NULL ) && 
        ((NdefMap->CardState != PH_NDEFMAP_CARD_STATE_INITIALIZED) && 
        (NdefMap->CardState != PH_NDEFMAP_CARD_STATE_INVALID)))
    {
        /*Register PacketData to Data Buffer of NdefMap */
        NdefMap->ApduBuffer = PacketData;
        /*Register PacketDataLength to Data Length of NdefMap */
        NdefMap->ApduBufferSize = *PacketDataLength ;
        /*  To return actual number of bytes read to the caller */
        NdefMap->NumOfBytesRead = PacketDataLength ;
        *NdefMap->NumOfBytesRead = 0;
        
        if( (Offset == PH_FRINFC_NDEFMAP_SEEK_BEGIN) || ( NdefMap->PrevOperation == 
            PH_FRINFC_NDEFMAP_WRITE_OPE))
        {
            NdefMap->MifareULContainer.CurrentBlock = PH_FRINFC_NDEFMAP_MFUL_BLOCK4;
            NdefMap->TLVStruct.NdefTLVFoundFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG0;
            NdefMap->TLVStruct.NdefTLVByte = PH_FRINFC_NDEFMAP_MFUL_FLAG0;
            NdefMap->TLVStruct.NdefTLVBlock = PH_FRINFC_NDEFMAP_MFUL_BLOCK4;
            NdefMap->MifareULContainer.RemainingSize = NdefMap->CardMemSize;
            NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG0;
            NdefMap->TLVStruct.NoLbytesinTLV = PH_FRINFC_NDEFMAP_MFUL_VAL0;
            NdefMap->MifareULContainer.ReadBufIndex = PH_FRINFC_NDEFMAP_MFUL_VAL0;
            NdefMap->MifareULContainer.ReadWriteCompleteFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG0;
        }
        
        NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_READ_OPE;
        NdefMap->MifareULContainer.CRindex = PH_FRINFC_NDEFMAP_CR_RD_NDEF;

        if( (Offset == PH_FRINFC_NDEFMAP_SEEK_CUR) &&
            (NdefMap->MifareULContainer.ReadWriteCompleteFlag ==
                                PH_FRINFC_NDEFMAP_MFUL_FLAG1))
        {
            /*  No space on card for reading : we have already
                reached the end of file !
                Offset is set to Continue Operation */  
            Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                            NFCSTATUS_EOF_NDEF_CONTAINER_REACHED);
        }
        else
        {
            NdefMap->Offset = (((Offset != PH_FRINFC_NDEFMAP_SEEK_BEGIN) && 
                                ( NdefMap->PrevOperation != PH_FRINFC_NDEFMAP_READ_OPE))?
                                PH_FRINFC_NDEFMAP_SEEK_BEGIN:
                                Offset);

            Result = ((NdefMap->Offset == PH_FRINFC_NDEFMAP_SEEK_BEGIN)?
            phFriNfc_MfUL_H_RdCardfindNdefTLV(NdefMap, NdefMap->MifareULContainer.CurrentBlock):
            phFriNfc_MfUL_H_CpDataToUserBuf(NdefMap));
        }
    }
    return Result;
}


/*!
 * \brief Initiates writing of NDEF information to the Mifare UL.
 *
 * The function initiates the writing of NDEF information to a Mifare UL.
 * It performs a reset of the state and starts the action (state machine).
 * A periodic call of the \ref phFriNfcNdefMap_Process has to be done once the action
 * has been triggered.
 */

NFCSTATUS phFriNfc_MifareUL_WrNdef( phFriNfc_NdefMap_t  *NdefMap,
                                    uint8_t             *PacketData,
                                    uint32_t            *PacketDataLength,
                                    uint8_t             Offset)
{
      NFCSTATUS                   Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                                    NFCSTATUS_INVALID_PARAMETER);
    
    
    
        if((NdefMap != NULL) && (PacketData != NULL) && (PacketDataLength != NULL) && 
            (*PacketDataLength != PH_FRINFC_NDEFMAP_MFUL_VAL0) && 
            (Offset <= PH_FRINFC_NDEFMAP_SEEK_BEGIN) && 
            (NdefMap->CompletionRoutine->CompletionRoutine != NULL) && 
            (NdefMap->CompletionRoutine->Context != NULL ) && 
            ((NdefMap->CardState != PH_NDEFMAP_CARD_STATE_READ_ONLY) && 
            (NdefMap->CardState != PH_NDEFMAP_CARD_STATE_INVALID)))
        {
            NdefMap->MifareULContainer.CRindex = PH_FRINFC_NDEFMAP_CR_WR_NDEF;
            /*Register PacketData to Data Buffer of NdefMap */
            NdefMap->ApduBuffer = PacketData;
            /*Register PacketDataLength to Data Length of NdefMap */
            NdefMap->ApduBufferSize = *PacketDataLength ;
            /*  To return actual number of bytes read to the caller */
            NdefMap->WrNdefPacketLength = PacketDataLength ;
            *NdefMap->WrNdefPacketLength = 0;

            if( (Offset == PH_FRINFC_NDEFMAP_SEEK_BEGIN) || ( NdefMap->PrevOperation == 
                PH_FRINFC_NDEFMAP_READ_OPE))
            {
                NdefMap->MifareULContainer.CurrentSector = NdefMap->TLVStruct.NdefTLVSector;
                NdefMap->MifareULContainer.CurrentBlock = NdefMap->TLVStruct.NdefTLVBlock;
                NdefMap->TLVStruct.TcheckedinTLVFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG0;
                NdefMap->TLVStruct.NoLbytesinTLV = PH_FRINFC_NDEFMAP_MFUL_VAL0;
                NdefMap->MifareULContainer.ReadBufIndex = PH_FRINFC_NDEFMAP_MFUL_VAL0;
                NdefMap->MifareULContainer.ReadWriteCompleteFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG0;
                NdefMap->TLVStruct.WrLenFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG0;
                NdefMap->MifareULContainer.InternalLength = PH_FRINFC_NDEFMAP_MFUL_VAL0;
                NdefMap->MifareULContainer.RemainingSize = 
                                    (NdefMap->CardMemSize - 
                                    (((NdefMap->TLVStruct.NdefTLVBlock - 
                                    PH_FRINFC_NDEFMAP_MFUL_BYTE4) * 
                                    PH_FRINFC_NDEFMAP_MFUL_BYTE4) + 
                                    NdefMap->TLVStruct.NdefTLVByte + 
                                    PH_FRINFC_NDEFMAP_MFUL_VAL1));
            }
            NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_WRITE_OPE;
            NdefMap->MifareULContainer.CRindex = PH_FRINFC_NDEFMAP_CR_WR_NDEF;

            if( (Offset == PH_FRINFC_NDEFMAP_SEEK_CUR) &&
                (NdefMap->MifareULContainer.ReadWriteCompleteFlag ==
                                    PH_FRINFC_NDEFMAP_MFUL_FLAG1))
            {
                /*  No space on card for reading : we have already
                    reached the end of file !
                    Offset is set to Continue Operation */  
                Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                NFCSTATUS_EOF_NDEF_CONTAINER_REACHED);
            }
            else
            {
                NdefMap->Offset = (((Offset != PH_FRINFC_NDEFMAP_SEEK_BEGIN) && 
                                    ( NdefMap->PrevOperation == PH_FRINFC_NDEFMAP_READ_OPE))?
                                    PH_FRINFC_NDEFMAP_SEEK_BEGIN:
                                    Offset);

                if (NdefMap->TLVStruct.NdefTLVSector == 1)
                {
                    NdefMap->MifareULContainer.CurrentSector = 1;

                    /* Change to sector 1 */
                    Result = phFriNfc_MfUL_H_SelectSector(NdefMap, 
                    NdefMap->MifareULContainer.CurrentSector, 1, 
                    PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_WRITE_INIT_1);

                }
                else 
                {
                    Result = ((NdefMap->Offset == PH_FRINFC_NDEFMAP_SEEK_BEGIN)?
                        phFriNfc_MfUL_H_RdBeforeWrite(NdefMap):
                        phFriNfc_MfUL_H_fillSendBufToWr(NdefMap));
                }
            }
        }
    

    return Result;
}



/*!
 * \brief Check whether a particular Mifare UL is NDEF compliant.
 *
 * The function checks whether the peer device is NDEF compliant.
 *
 */

NFCSTATUS phFriNfc_MifareUL_ChkNdef( phFriNfc_NdefMap_t     *NdefMap)
{
    NFCSTATUS                   status =    NFCSTATUS_PENDING;
    uint8_t                     index=0,
                                pSensRes[2] = {0};

    /* set the data for additional data exchange*/
    NdefMap->psDepAdditionalInfo.DepFlags.MetaChaining = 0;
    NdefMap->psDepAdditionalInfo.DepFlags.NADPresent = 0;
    NdefMap->psDepAdditionalInfo.NAD = 0;

    /*
     *  Changed 
     *  Description: CardInfo106 replase 
     */

    /* retrive remote card information */
    pSensRes[0]  = NdefMap->psRemoteDevInfo->RemoteDevInfo.Iso14443A_Info.AtqA[0];
                   
    NdefMap->TLVStruct.NdefTLVBlock = PH_FRINFC_NDEFMAP_MFUL_VAL4;
#ifdef LOCK_BITS_CHECK_ENABLE
    NdefMap->MifareULContainer.CurrentBlock = PH_FRINFC_NDEFMAP_MFUL_BLOCK2;
#else
    NdefMap->MifareULContainer.CurrentBlock = PH_FRINFC_NDEFMAP_MFUL_BLOCK3;
#endif /* #ifdef LOCK_BITS_CHECK_ENABLE */
    NdefMap->MifareULContainer.CRindex = PH_FRINFC_NDEFMAP_CR_CHK_NDEF;
        
    /* Check for Mifare Bit information  */
    if (((pSensRes[0] & PH_FRINFC_NDEFMAP_MFUL_CHECK_RESP) == PH_FRINFC_NDEFMAP_MFUL_CHECK_RESP))
    {
        NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_CHECK_OPE;
        /* set the offset*/
        NdefMap->SendRecvBuf[index] = NdefMap->MifareULContainer.CurrentBlock;
        
        /*set the send length*/
        NdefMap->SendLength = PH_FRINFC_NDEFMAP_MFUL_MAX_SEND_BUF_TO_READ; 
                                        
        /* Change the state to check ndef compliancy */
        NdefMap->State = PH_FRINFC_NDEFMAP_MFUL_STATE_CHK_NDEF_COMP;
        
        /* Set the card type as Mifare UL */
        NdefMap->CardType = PH_FRINFC_NDEFMAP_MIFARE_UL_CARD;

        /* set the cmd to mifare read*/
        /* 
         * Changed 
         * Description: phHal_eMifareCmdListMifareRead replace with phHal_eMifareRead
         */ 
        NdefMap->Cmd.MfCmd = phHal_eMifareRead;
        
        /* Set the CR and context for Mifare operations*/
        NdefMap->MapCompletionInfo.CompletionRoutine = phFriNfc_MifareUL_Process;
        NdefMap->MapCompletionInfo.Context = NdefMap;

        /*Set the Length*/
        *NdefMap->SendRecvLength = PH_FRINFC_NDEFMAP_MF_READ_BLOCK_SIZE;
        

        /*Call the Overlapped HAL Transceive function */ 
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
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_REMOTE_DEVICE);
    }
    return status;
}

static void
phFriNfc_MfUL_CalcByteNum(phFriNfc_NdefMap_t *NdefMap)
{
    uint8_t     i = PH_FRINFC_NDEFMAP_MFUL_VAL0;
    uint16_t    TemLength = PH_FRINFC_NDEFMAP_MFUL_VAL0;
        

    for (i = 0; i < 16; i++)
    {
        if ((NdefMap->MifareULContainer.ReadBuf[i] ==
        PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_T) && 
        ((NdefMap->MifareULContainer.ReadBuf[i + 1] ==
        NdefMap->TLVStruct.ActualSize) || 
        (NdefMap->MifareULContainer.ReadBuf[i + 1] == 0xFF)))
        {
            if (NdefMap->MifareULContainer.ReadBuf[i + 1] == 0xFF)
            {
                TemLength = NdefMap->MifareULContainer.ReadBuf[i + 2] |
                ((uint16_t)NdefMap->MifareULContainer.ReadBuf[i + 3] << 8);

                if (TemLength == NdefMap->TLVStruct.ActualSize)
                {
                    NdefMap->MifareULContainer.ByteNumber = i + 1;
                    break;
                }
            }
            else
            {   
                NdefMap->MifareULContainer.ByteNumber = i + 1;
                break;
            }
        }
     }

    return;
}


#ifdef LOCK_BITS_CHECK_ENABLE 

#define MIF_UL_LOCK_BIT_CHECK           0xFF
#define MIF_UL_LOCK_BIT_0_VALUE         0x0F
#define MIF_UL_LOCK_BIT_1_VALUE         0x00

static 
void
phFriNfc_MfUL_H_ChkLockBits (
    phFriNfc_NdefMap_t *NdefMap)
{
    uint8_t         index = 2;

    if (((NdefMap->SendRecvBuf[index] & 
        MIF_UL_LOCK_BIT_CHECK) > MIF_UL_LOCK_BIT_0_VALUE) ||  
        (MIF_UL_LOCK_BIT_1_VALUE != 
        (NdefMap->SendRecvBuf[(index + 1)] & MIF_UL_LOCK_BIT_CHECK)))
    {
        NdefMap->CardState = PH_NDEFMAP_CARD_STATE_READ_ONLY;
    }
}

#endif /* #ifdef LOCK_BITS_CHECK_ENABLE  */

/*!
 * \brief Completion Routine, Processing function, needed to avoid long 
 * blocking.
 * \note The lower (Overlapped HAL) layer must register a pointer to 
 *  this function as a Completion
 *  Routine in order to be able to notify the component that an I/O 
 *  has finished and data are ready to be processed.
 *            
 */

void phFriNfc_MifareUL_Process( void        *Context,
                                NFCSTATUS   Status)

{
    
    uint8_t     index = PH_FRINFC_NDEFMAP_MFUL_VAL0,
                i = PH_FRINFC_NDEFMAP_MFUL_VAL0;
    phFriNfc_NdefMap_t          *NdefMap;
    uint16_t    TemLength = PH_FRINFC_NDEFMAP_MFUL_VAL0;
    /*uint16_t                    TempByte = PH_FRINFC_NDEFMAP_MFUL_VAL0; */
    static uint8_t                     CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG0;

    CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG0;

    /* set the context to Map module */

    NdefMap = (phFriNfc_NdefMap_t *)Context;    

    if ( Status == NFCSTATUS_SUCCESS )
    {
        switch (NdefMap->State)
        {
            case PH_FRINFC_NDEFMAP_MFUL_STATE_CHK_NDEF_COMP:
                if (*NdefMap->SendRecvLength == 
                    PH_FRINFC_NDEFMAP_MFUL_RDBYTES_16)
                {
                    /* Checks for the Ndef Compliency and validy of the memory size*/ 
                    Status = phFriNfc_MfUL_H_ChkCCBytes(NdefMap);
                    CRFlag = (uint8_t)((Status != NFCSTATUS_SUCCESS)?
                                PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                                PH_FRINFC_NDEFMAP_MFUL_FLAG0);

#ifdef LOCK_BITS_CHECK_ENABLE

                    /* Check for lock bits */
                    if (NFCSTATUS_SUCCESS == Status)
                    {
                        phFriNfc_MfUL_H_ChkLockBits(NdefMap);
                    }

#endif /* #ifdef LOCK_BITS_CHECK_ENABLE */

                    /* Find the NDEF TLV */
                    NdefMap->MifareULContainer.CurrentBlock = PH_FRINFC_NDEFMAP_MFUL_BLOCK4;
                    Status = ((Status != NFCSTATUS_SUCCESS)?
                                Status:
                                phFriNfc_MfUL_H_RdCardfindNdefTLV(NdefMap, 
                                            NdefMap->MifareULContainer.CurrentBlock));
                    CRFlag = (uint8_t)(((Status != NFCSTATUS_PENDING ) || 
                                (CRFlag == PH_FRINFC_NDEFMAP_MFUL_FLAG1))?
                                PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                                PH_FRINFC_NDEFMAP_MFUL_FLAG0);

#ifdef PH_HAL4_ENABLE
                    if ((Status != NFCSTATUS_PENDING ) && 
                        (Status != NFCSTATUS_SUCCESS))
                    {
                        NdefMap->CardState = PH_NDEFMAP_CARD_STATE_INVALID;
                    }
#endif /* #ifdef PH_HAL4_ENABLE */
                }               
            break;


            case PH_FRINFC_NDEFMAP_MFUL_STATE_READ:
                /* check the received bytes size equals 16 bytes*/
                if (*NdefMap->SendRecvLength == 
                    PH_FRINFC_NDEFMAP_MFUL_RDBYTES_16)
                {
                    if(NdefMap->MifareULContainer.ReadBufIndex < 
                        (NdefMap->TLVStruct.ActualSize + (((NdefMap->TLVStruct.NdefTLVBlock - 
						PH_FRINFC_NDEFMAP_MFUL_BLOCK4) 
                            * PH_FRINFC_NDEFMAP_MFUL_VAL4) + (NdefMap->TLVStruct.NdefTLVByte - 1) + 4)))

                    {
                        Status = phFriNfc_MfUL_H_CopyRdBytes(NdefMap);
                    }

                    if (Status == NFCSTATUS_SUCCESS)
                    {
                        if(NdefMap->MifareULContainer.ReadBufIndex >= 
                            (NdefMap->TLVStruct.ActualSize + (((NdefMap->TLVStruct.NdefTLVBlock - 
							PH_FRINFC_NDEFMAP_MFUL_BLOCK4) 
                            * PH_FRINFC_NDEFMAP_MFUL_VAL4) + (NdefMap->TLVStruct.NdefTLVByte - 1) + 4)))
                        {

                            phFriNfc_MfUL_CalcByteNum(NdefMap);
#if 0
                            for (i = 0; i < 16; i++)
                            {
                                if ((NdefMap->MifareULContainer.ReadBuf[i] ==
                                    PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_T) && 
                                    ((NdefMap->MifareULContainer.ReadBuf[i + 1] ==
                                    NdefMap->TLVStruct.ActualSize) || 
                                    (NdefMap->MifareULContainer.ReadBuf[i + 1] == 0xFF)))
                                {
                                    if (NdefMap->MifareULContainer.ReadBuf[i + 1] == 0xFF)
                                    {
                                        TemLength = NdefMap->MifareULContainer.ReadBuf[i + 2] |
                                            ((uint16_t)NdefMap->MifareULContainer.ReadBuf[i + 3] << 8);
 
                                        if (TemLength == NdefMap->TLVStruct.ActualSize)
                                        {
                                            NdefMap->MifareULContainer.ByteNumber = i + 1;
                                            break;
                                        }
                                    }
                                    else
                                    {   
                                        NdefMap->MifareULContainer.ByteNumber = i + 1;
                                        break;
                                    }
                                }
                            }
#endif                          
 
                            if (NdefMap->MifareULContainer.ReadBuf
                                [NdefMap->MifareULContainer.ByteNumber] == 0xFF)
                            {
                                NdefMap->MifareULContainer.ByteNumber =
                                    NdefMap->MifareULContainer.ByteNumber + 3;
                            }
                            else
                            {
                                NdefMap->MifareULContainer.ByteNumber =
                                    NdefMap->MifareULContainer.ByteNumber + 1;
                            }
                        
                            Status = phFriNfc_MfUL_H_CpDataToUserBuf(NdefMap);
                            if (NdefMap->MifareULContainer.CurrentSector > 0)
                            {
                                NdefMap->MifareULContainer.CurrentSector = 0;
                                NdefMap->PrevState = PH_FRINFC_NDEFMAP_MFUL_STATE_READ;
                                    
                                Status = phFriNfc_MfUL_H_SelectSector(NdefMap, 
                                    NdefMap->MifareULContainer.CurrentSector, 1,
                                    PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_RESET_1);
                                CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING )?
                                    PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                                    PH_FRINFC_NDEFMAP_MFUL_FLAG0);
                            }
                            else
                            {
                                CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                            }
                        }
                        else
                        {
                            Status = phFriNfc_MfUL_H_Rd16Bytes(NdefMap);
                            CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                    PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                                    PH_FRINFC_NDEFMAP_MFUL_FLAG0);
                        }
                    }
                    else
                    {
                        
                        CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG0;
                    }
                }
                else
                {
                    /* read error */
                    Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                        NFCSTATUS_INVALID_RECEIVE_LENGTH);
                    CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                }
            break;

            case PH_FRINFC_NDEFMAP_MFUL_STATE_WRITE:
                Status = phFriNfc_MfUL_H_ProWrittenBytes(NdefMap);
                CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                            PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                            PH_FRINFC_NDEFMAP_MFUL_FLAG0);
            break;
            
            case PH_FRINFC_NDEFMAP_MFUL_STATE_FND_NDEF_COMP: 
                if (*NdefMap->SendRecvLength == 
                    PH_FRINFC_NDEFMAP_MFUL_RDBYTES_16)
                {
                    switch(NdefMap->PrevOperation)
                    {
                    case PH_FRINFC_NDEFMAP_CHECK_OPE:
                    case PH_FRINFC_NDEFMAP_READ_OPE:
                            CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG0;
                            if(NdefMap->TLVStruct.NoLbytesinTLV > 
                                PH_FRINFC_NDEFMAP_MFUL_VAL0)
                            {
                                Status = phFriNfc_MfUL_H_ChkRemainTLV(NdefMap, &CRFlag);
                            }
                            else
                            {
                                if(NdefMap->TLVStruct.NdefTLVFoundFlag != 
                                    PH_FRINFC_NDEFMAP_MFUL_FLAG1)
                                {
                                    /* Find the NDEF TLV */
                                    Status = phFriNfc_MfUL_H_findNDEFTLV(NdefMap, &CRFlag);
                                    CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING )?
                                            PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                                            PH_FRINFC_NDEFMAP_MFUL_FLAG0);
                                }
                            }
                            if((NdefMap->TLVStruct.NdefTLVFoundFlag == 
                                PH_FRINFC_NDEFMAP_MFUL_FLAG1) && 
                                (NdefMap->TLVStruct.NoLbytesinTLV == 
                                PH_FRINFC_NDEFMAP_MFUL_VAL0))
                            {
                                CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG0;
                                /* Ndef TLV found, so call the next function depending on the 
                                    check/read/write ndef operation */

                                if (NdefMap->MifareULContainer.CurrentSector > 0)
                                {
                                    NdefMap->MifareULContainer.CurrentSector = 0;
                                    NdefMap->PrevState = PH_FRINFC_NDEFMAP_MFUL_STATE_FND_NDEF_COMP;
                                    
                                    Status = phFriNfc_MfUL_H_SelectSector(NdefMap, 
                                        NdefMap->MifareULContainer.CurrentSector, 1,
                                        PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_RESET_1);
                                    CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING )?
                                            PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                                            PH_FRINFC_NDEFMAP_MFUL_FLAG0);
                                }
                                else 
                                {
                                    /* Sector is 0 no need to send sector select */
                                    Status = phFriNfc_MfUL_H_NxtOp(NdefMap, &CRFlag);
                                }
                            }

#ifdef PH_HAL4_ENABLE
                            if ((Status != NFCSTATUS_PENDING ) && 
                                (Status != NFCSTATUS_SUCCESS) && 
                                (PH_FRINFC_NDEFMAP_CHECK_OPE == 
                                NdefMap->PrevOperation))
                            {
                                NdefMap->CardState = PH_NDEFMAP_CARD_STATE_INVALID;
                            }
#endif /* #ifdef PH_HAL4_ENABLE */
                        break;

                    case PH_FRINFC_NDEFMAP_WRITE_OPE:
                        /* Remove UpdateWrLen */
                        Status = ((NdefMap->TLVStruct.WrLenFlag == 
                                    PH_FRINFC_NDEFMAP_MFUL_FLAG1)?
                                    phFriNfc_MfUL_H_UpdateWrLen(NdefMap):
                                    phFriNfc_MfUL_H_CallWrOp(NdefMap));
                        CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                                PH_FRINFC_NDEFMAP_MFUL_FLAG0);
                        break;

                    default:
                        Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_REMOTE_DEVICE);
                        CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                        break;

                    }
                }
            break;

            case PH_FRINFC_NDEFMAP_MFUL_STATE_TERM_TLV:
                Status = phFriNfc_MfUL_H_UpdateWrLen(NdefMap);
                CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                            PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                            PH_FRINFC_NDEFMAP_MFUL_FLAG0);
            break;

            case PH_FRINFC_NDEFMAP_MFUL_STATE_WR_LEN_TLV:
                if(((((NdefMap->TLVStruct.NdefTLVByte - 
                    PH_FRINFC_NDEFMAP_MFUL_VAL1) == 
                    PH_FRINFC_NDEFMAP_MFUL_VAL3) && 
                    (NdefMap->MifareULContainer.CurrentBlock == 
                    (NdefMap->TLVStruct.NdefTLVBlock + 
                    PH_FRINFC_NDEFMAP_MFUL_VAL1))) || 
                    (((NdefMap->TLVStruct.NdefTLVByte - 
                        PH_FRINFC_NDEFMAP_MFUL_VAL1) < 
                    PH_FRINFC_NDEFMAP_MFUL_VAL3) && (
                    NdefMap->MifareULContainer.CurrentBlock == 
                    NdefMap->TLVStruct.NdefTLVBlock))))
                {
                    (void)memcpy(NdefMap->MifareULContainer.InternalBuf, 
                                NdefMap->MifareULContainer.Buffer,
                                NdefMap->MifareULContainer.InternalLength);             
                }
                (void)memcpy(NdefMap->TLVStruct.NdefTLVBuffer, 
                            NdefMap->MifareULContainer.Buffer,
                            PH_FRINFC_NDEFMAP_MFUL_VAL4);

                NdefMap->CardState =(uint8_t) ((NdefMap->CardState == 
                                    PH_NDEFMAP_CARD_STATE_INITIALIZED)?
                                    PH_NDEFMAP_CARD_STATE_READ_WRITE:
                                    NdefMap->CardState);
                NdefMap->ApduBuffIndex = PH_FRINFC_NDEFMAP_MFUL_VAL0;
                NdefMap->NumOfBytesWritten = PH_FRINFC_NDEFMAP_MFUL_VAL0;

                if (NdefMap->MifareULContainer.CurrentSector > 0)
                {
                    /* Reset sector */
                    NdefMap->MifareULContainer.CurrentSector = 0;
                    NdefMap->PrevState = PH_FRINFC_NDEFMAP_MFUL_STATE_WR_LEN_TLV;
                                                
                    Status = phFriNfc_MfUL_H_SelectSector(NdefMap, 
                            NdefMap->MifareULContainer.CurrentSector, 1,
                            PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_RESET_1);
                    CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                    PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                                    PH_FRINFC_NDEFMAP_MFUL_FLAG0);
                }
                else
                {
                    CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                }
                
            break;

            case PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_CHK_1:
                 /* check the received bytes size equals 1 byte*/
                if (*NdefMap->SendRecvLength == 
                    PH_FRINFC_NDEFMAP_MFUL_VAL1)
                {
                    if (NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL0] == 0x0A)
                    {
                        /* Send second command */
                        Status = phFriNfc_MfUL_H_SelectSector(NdefMap, 
                            NdefMap->MifareULContainer.CurrentSector, 2, 
                            PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_CHK_2);
                    }
                    else
                    {
                        /* read error */
                        Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_FORMAT);
                        CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                    }
                }
                else
                {
                    /* read error */
                    Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_INVALID_RECEIVE_LENGTH);
                    CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;

                }
            break;

            case PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_CHK_2:
                {
                    NdefMap->MifareULContainer.CurrentBlock += 
                                PH_FRINFC_NDEFMAP_MFUL_BLOCK4;
                    
                    Status = phFriNfc_MfUL_H_RdCardfindNdefTLV(NdefMap,
                                NdefMap->MifareULContainer.CurrentBlock);
                }
            break;  

            case PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_READ_1:
                 /* check the received bytes size equals 1 byte*/
                if (*NdefMap->SendRecvLength == 
                    PH_FRINFC_NDEFMAP_MFUL_VAL1)
                {
                    if (NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL0] == 0x0A)
                    {
                        /* Send second command */
                        Status = phFriNfc_MfUL_H_SelectSector(NdefMap, 
                            NdefMap->MifareULContainer.CurrentSector, 2, 
                            PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_READ_2);
                        CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG0;
                    }
                    else
                    {
                        /* read error */
                        Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_FORMAT);
                        CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                    }
                }
                else
                {
                    /* read error */
                    Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_INVALID_RECEIVE_LENGTH);
                    CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;

                }
            break;

            case PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_READ_2:
                {
                    if (NdefMap->MifareULContainer.CurrentBlock == 0xFF)
                    {
                        NdefMap->MifareULContainer.CurrentBlock = 0;
                    }
                    else 
                    {
                        NdefMap->MifareULContainer.CurrentBlock = 
                            (NdefMap->TLVStruct.NdefTLVBlock / 4) * 4;
                    }
                    
                    Status = phFriNfc_MfUL_H_Rd16Bytes(NdefMap);
                    CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                                    PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                                    PH_FRINFC_NDEFMAP_MFUL_FLAG0);
                }
            break;  

            case PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_WRITE_1:
                 /* check the received bytes size equals 1 byte*/
                if (*NdefMap->SendRecvLength == 
                    PH_FRINFC_NDEFMAP_MFUL_VAL1)
                {
                    if (NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL0] == 0x0A)
                    {
                        /* Send second command */
                        Status = phFriNfc_MfUL_H_SelectSector(NdefMap, 
                            NdefMap->MifareULContainer.CurrentSector, 2, 
                            PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_WRITE_2);
                    }
                    else
                    {
                        /* read error */
                        Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_FORMAT);
                        CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                    }
                }
                else
                {
                    /* read error */
                    Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_INVALID_RECEIVE_LENGTH);
                    CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;

                }
            break;

            case PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_WRITE_2:
                {
                    NdefMap->MifareULContainer.CurrentBlock = 0;
                    Status = phFriNfc_MfUL_H_fillSendBufToWr(NdefMap);
                    
                    if((Status == NFCSTATUS_SUCCESS) && 
                        (NdefMap->TLVStruct.SetTermTLVFlag != 
                            PH_FRINFC_NDEFMAP_MFUL_FLAG1) && 
                            (NdefMap->MifareULContainer.RemainingSize > 
                            PH_FRINFC_NDEFMAP_MFUL_VAL0))
                    {
                        Status = phFriNfc_MfUL_H_WrTermTLV(NdefMap);
                    }
                    else 
                    {
                        if((Status == NFCSTATUS_SUCCESS) && 
                            (NdefMap->TLVStruct.SetTermTLVFlag == 
                            PH_FRINFC_NDEFMAP_MFUL_FLAG1))
                        {
                            Status = phFriNfc_MfUL_H_UpdateWrLen(NdefMap);
                        }
                    }                   

                    CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                            PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                            PH_FRINFC_NDEFMAP_MFUL_FLAG0);
                }
            break;  

            case PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_WRITE_INIT_1:
                 /* check the received bytes size equals 1 byte*/
                if (*NdefMap->SendRecvLength == 
                    PH_FRINFC_NDEFMAP_MFUL_VAL1)
                {
                    if (NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL0] == 0x0A)
                    {
                        /* Send second command */
                        Status = phFriNfc_MfUL_H_SelectSector(NdefMap, 
                            NdefMap->MifareULContainer.CurrentSector, 2, 
                            PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_WRITE_INIT_2);
                    }
                    else
                    {
                        /* read error */
                        Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_FORMAT);
                        CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                    }
                }
                else
                {
                    /* read error */
                    Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_INVALID_RECEIVE_LENGTH);
                    CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;

                }
            break;

            case PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_WRITE_INIT_2:
                {
                    Status = ((NdefMap->Offset == PH_FRINFC_NDEFMAP_SEEK_BEGIN)?
                        phFriNfc_MfUL_H_RdBeforeWrite(NdefMap):
                        phFriNfc_MfUL_H_fillSendBufToWr(NdefMap));
                    
                    
                    CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                            PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                            PH_FRINFC_NDEFMAP_MFUL_FLAG0);
                }
            break;  


            case PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_RW_1:
                 /* check the received bytes size equals 1 byte*/
                if (*NdefMap->SendRecvLength == 
                    PH_FRINFC_NDEFMAP_MFUL_VAL1)
                {
                    if (NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL0] == 0x0A)
                    {
                        /* Send second command */
                        Status = phFriNfc_MfUL_H_SelectSector(NdefMap, 
                            NdefMap->MifareULContainer.CurrentSector, 2, 
                            PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_RW_2);
                    }
                    else
                    {
                        /* read error */
                        Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_FORMAT);
                        CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                    }
                }
                else
                {
                    /* read error */
                    Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_INVALID_RECEIVE_LENGTH);
                    CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;

                }
            break;

            case PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_RW_2:
                {
                    NdefMap->MifareULContainer.CurrentBlock = 0;

                    NdefMap->SendRecvBuf[index] = 
                                        NdefMap->MifareULContainer.CurrentBlock;
                    index++;
                    NdefMap->SendRecvBuf[index] = 
                                            PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_L;
                    index++;        

                    if((((NdefMap->TLVStruct.NdefTLVByte - 
                            PH_FRINFC_NDEFMAP_MFUL_VAL1) == PH_FRINFC_NDEFMAP_MFUL_VAL0) || 
                            ((NdefMap->TLVStruct.NdefTLVByte - PH_FRINFC_NDEFMAP_MFUL_VAL1) 
                            == PH_FRINFC_NDEFMAP_MFUL_VAL3)))
                        {
                            /* Length to know how many bytes has to be written to the card */
                        TemLength = (((NdefMap->TLVStruct.NdefTLVByte - PH_FRINFC_NDEFMAP_MFUL_VAL1) == 
                                        PH_FRINFC_NDEFMAP_MFUL_VAL0)?
                                        PH_FRINFC_NDEFMAP_MFUL_VAL2:
                                        PH_FRINFC_NDEFMAP_MFUL_VAL3);

                        if(NdefMap->ApduBufferSize >= TemLength)
                        {
                            /* Prepare the receive buffer */
                            (void)memcpy(&(NdefMap->SendRecvBuf[ 
                                            index]),
                                            &(NdefMap->ApduBuffer[ 
                                            NdefMap->ApduBuffIndex]),
                                            TemLength);

                            /* Number of bytes written to the card from user buffer */
                            NdefMap->NumOfBytesWritten = TemLength;

                            index = index+(uint8_t)TemLength;
                            /* Exact number of bytes written in the card including TLV */
                            *NdefMap->DataCount = (index - PH_FRINFC_NDEFMAP_MFUL_VAL1);
                        }
                        else
                        {
                            /* Prepare the receive buffer */
                            (void)memcpy(&(NdefMap->SendRecvBuf[ 
                                            index]),
                                            &(NdefMap->ApduBuffer[ 
                                            NdefMap->ApduBuffIndex]),
                                            (uint16_t)NdefMap->ApduBufferSize);

                            /* Number of bytes written to the card from user buffer */
                            NdefMap->NumOfBytesWritten = (uint16_t)NdefMap->ApduBufferSize;

                            index= index +(uint8_t)NdefMap->ApduBufferSize;
                            /* Exact number of bytes written in the card including TLV */
                            *NdefMap->DataCount = (index - PH_FRINFC_NDEFMAP_MFUL_VAL1);

                            for(i = index; i < PH_FRINFC_NDEFMAP_MFUL_WR_A_BLK; i++)
                            {
                                NdefMap->SendRecvBuf[i] = (uint8_t)((i == index)?
                                        PH_FRINFC_NDEFMAP_MFUL_TERMTLV:
                                        PH_FRINFC_NDEFMAP_MFUL_NULLTLV);
                                NdefMap->TLVStruct.SetTermTLVFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                            }
                        }

                        /* store the bytes in buffer till the bytes are 
                            written in a block */
                        (void)memcpy(NdefMap->MifareULContainer.Buffer,
                                        &(NdefMap->SendRecvBuf[ 
                                        PH_FRINFC_NDEFMAP_MFUL_VAL1]), 
                                        (PH_FRINFC_NDEFMAP_MFUL_WR_A_BLK - 
                                        PH_FRINFC_NDEFMAP_MFUL_VAL1));

                        (void)memcpy(NdefMap->TLVStruct.NdefTLVBuffer,
                                    NdefMap->MifareULContainer.Buffer,
                                    (PH_FRINFC_NDEFMAP_MFUL_WR_A_BLK - 
                                        PH_FRINFC_NDEFMAP_MFUL_VAL1));
                        
                        /* Change the state to check ndef compliancy */
                        NdefMap->State = PH_FRINFC_NDEFMAP_MFUL_STATE_WRITE;

                        Status = phFriNfc_MfUL_H_Wr4bytes(NdefMap);                 
                    }

                    
                    CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                            PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                            PH_FRINFC_NDEFMAP_MFUL_FLAG0);
                }
            break;  

            case PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_RESET_1:
                 /* check the received bytes size equals 1 byte*/
                if (*NdefMap->SendRecvLength == 
                    PH_FRINFC_NDEFMAP_MFUL_VAL1)
                {
                    if (NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL0] == 0x0A)
                    {
                        /* Send second command */
                        Status = phFriNfc_MfUL_H_SelectSector(NdefMap, 
                            NdefMap->MifareULContainer.CurrentSector, 2, 
                            PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_RESET_2);
                    }
                    else
                    {
                        /* read error */
                        Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_FORMAT);
                        CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                    }
                }
                else
                {
                    /* read error */
                    Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_INVALID_RECEIVE_LENGTH);
                    CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;

                }
            break;

            case PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_RESET_2:
                {
                    if (NdefMap->PrevState == PH_FRINFC_NDEFMAP_MFUL_STATE_FND_NDEF_COMP)
                    {
                        Status = phFriNfc_MfUL_H_NxtOp(NdefMap, &CRFlag);
                    }
                    else if (NdefMap->PrevState == PH_FRINFC_NDEFMAP_MFUL_STATE_READ)
                    {
                        CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                    }
                    else if (NdefMap->PrevState == PH_FRINFC_NDEFMAP_MFUL_STATE_WRITE)
                    {
                        Status = phFriNfc_MfUL_H_UpdateWrLen(NdefMap);
                        CRFlag = (uint8_t)((Status != NFCSTATUS_PENDING)?
                            PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                            PH_FRINFC_NDEFMAP_MFUL_FLAG0);
                    }
                    else if (NdefMap->PrevState == PH_FRINFC_NDEFMAP_MFUL_STATE_WR_LEN_TLV)
                    {
                        CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                    }
                    else
                    {
                        /* read error */
                        Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_READ_FAILED);
                        CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;

                    }
                }
            break;  

            default:
                /*set the invalid state*/
                Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_DEVICE_REQUEST);
                phFriNfc_MifareUL_H_Complete(NdefMap, Status);
            break;
        }
        if(CRFlag == PH_FRINFC_NDEFMAP_MFUL_FLAG1)
        {
            /* call the CR routine*/
            phFriNfc_MifareUL_H_Complete(NdefMap, Status);
        }
    }
    else
    {
        phFriNfc_MifareUL_H_Complete(NdefMap,Status);
    }
}

static NFCSTATUS phFriNfc_MfUL_H_ChkCCBytes(phFriNfc_NdefMap_t  *NdefMap )
{
    NFCSTATUS Result = PHNFCSTVAL(  CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_NO_NDEF_SUPPORT);
    
#ifdef LOCK_BITS_CHECK_ENABLE        
        switch(NdefMap->SendRecvBuf[7])     
#else
        switch(NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_BYTE3])        
#endif /* #ifdef LOCK_BITS_CHECK_ENABLE */ 
        {
            case PH_FRINFC_NDEFMAP_MFUL_CC_BYTE3_RW:
                /* This state can be either INITIALISED or READWRITE. but default 
                    is INITIALISED */
                NdefMap->CardState = PH_NDEFMAP_CARD_STATE_INITIALIZED;
                break;

            case PH_FRINFC_NDEFMAP_MFUL_CC_BYTE3_RO:
                NdefMap->CardState = PH_NDEFMAP_CARD_STATE_READ_ONLY;
                break;

            default :
                NdefMap->CardState = PH_NDEFMAP_CARD_STATE_INVALID;
        }


        
        /* Check for Ndef compliancy : 0 and 1 byte spcifies the ndef compliancy
           2 byte specifies the version of the MF UL tag*/
#ifdef LOCK_BITS_CHECK_ENABLE
        if(( NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_BYTE4] ==  
#else
        if(( NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_BYTE0] ==         
#endif /* #ifdef LOCK_BITS_CHECK_ENABLE */ 
            PH_FRINFC_NDEFMAP_MFUL_CC_BYTE0) && (
            (NdefMap->CardState == PH_NDEFMAP_CARD_STATE_INITIALIZED) || 
            (NdefMap->CardState == PH_NDEFMAP_CARD_STATE_READ_ONLY)))
        {
            /* Check the version number */
            Result =phFriNfc_MapTool_ChkSpcVer( NdefMap,
#ifdef LOCK_BITS_CHECK_ENABLE
                                                5);

#else
                                                PH_FRINFC_NDEFMAP_MFUL_BYTE1);

#endif /* #ifdef LOCK_BITS_CHECK_ENABLE */ 
#if 0
#ifdef PH_NDEF_MIFARE_ULC
            if (Result == NFCSTATUS_SUCCESS)
            {
#ifdef LOCK_BITS_CHECK_ENABLE

                if (NdefMap->SendRecvBuf[6] == 0x06)
                {
                    NdefMap->CardType = PH_FRINFC_NDEFMAP_MIFARE_UL_CARD;   
                } 
                else if (NdefMap->SendRecvBuf[6] == 0x12)
                {
//                  NdefMap->CardType = PH_FRINFC_NDEFMAP_MIFARE_ULC_CARD;
                    NdefMap->CardType = PH_FRINFC_NDEFMAP_MIFARE_UL_CARD;   
                }               

#else /* #ifdef LOCK_BITS_CHECK_ENABLE */

                if (NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_BYTE2] == 0x06)
                {
                    NdefMap->CardType = PH_FRINFC_NDEFMAP_MIFARE_UL_CARD;   
                } 
                else if (NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_BYTE2] == 0x12)
                {
//                  NdefMap->CardType = PH_FRINFC_NDEFMAP_MIFARE_ULC_CARD;
                    NdefMap->CardType = PH_FRINFC_NDEFMAP_MIFARE_UL_CARD;   
                }

#endif /* #ifdef LOCK_BITS_CHECK_ENABLE */
                else
                {
                    Result = PHNFCSTVAL(  CID_FRI_NFC_NDEF_MAP, 
                                                        NFCSTATUS_INVALID_REMOTE_DEVICE);
                }
            }
#else   
            
        /*  Check the CC header size: Only valid ones are
                0x06 for 48 bytes. */
#ifdef LOCK_BITS_CHECK_ENABLE
            Result = ((( NdefMap->SendRecvBuf[6] != 
#else /* #ifdef LOCK_BITS_CHECK_ENABLE */
            Result = ((( NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_BYTE2] != 
#endif /* #ifdef LOCK_BITS_CHECK_ENABLE */
                        PH_FRINFC_NDEFMAP_MFUL_CC_BYTE2) || (Result != 
                        NFCSTATUS_SUCCESS))?
                        (PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                        NFCSTATUS_INVALID_REMOTE_DEVICE)):
                        Result);
#endif /* #ifdef PH_NDEF_MIFARE_ULC */
#endif
            NdefMap->MifareULContainer.RemainingSize = 
            NdefMap->CardMemSize = ((Result == NFCSTATUS_SUCCESS)?
#ifdef LOCK_BITS_CHECK_ENABLE
                                    (NdefMap->SendRecvBuf[6] * 
#else /* #ifdef LOCK_BITS_CHECK_ENABLE */
                                    (NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_BYTE2] * 
#endif /* #ifdef LOCK_BITS_CHECK_ENABLE */
                                    PH_FRINFC_NDEFMAP_MFUL_MUL8):
                                    NdefMap->CardMemSize);

            if (NdefMap->CardMemSize > 256)
            {
                NdefMap->CardMemSize = NdefMap->CardMemSize - 2;
                NdefMap->MifareULContainer.RemainingSize = NdefMap->CardMemSize;
            }
                
        }
        else
        {
            NdefMap->CardState = PH_NDEFMAP_CARD_STATE_INVALID;
        }

    
    return Result;
}

/*!
 * \brief this shall notify the integration software with respective
 *  success/error status along with the completion routines.
 *
 *  This routine is called from the mifareul process function.
 *
 */

static void phFriNfc_MifareUL_H_Complete(phFriNfc_NdefMap_t  *NdefMap,
                                        NFCSTATUS            Status)
{
    if(NdefMap!=NULL)
    {
        if((PH_FRINFC_NDEFMAP_MFUL_STATE_WRITE   ==  NdefMap->State)
            &&  (NFCSTATUS_SUCCESS !=  Status))
        {
            *NdefMap->WrNdefPacketLength    =   0;
        }
        /* set the state back to the Reset_Init state*/
        NdefMap->State =  PH_FRINFC_NDEFMAP_STATE_RESET_INIT;

        /* set the completion routine*/
        NdefMap->CompletionRoutine[NdefMap->MifareULContainer.CRindex].
            CompletionRoutine(NdefMap->CompletionRoutine->Context, Status);
    }
}

static NFCSTATUS   phFriNfc_MfUL_H_Rd16Bytes( phFriNfc_NdefMap_t  *NdefMap)
{
    NFCSTATUS   Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                    NFCSTATUS_INVALID_PARAMETER);

    NdefMap->State = PH_FRINFC_NDEFMAP_MFUL_STATE_READ;

    /*  Set the previous operation flag to read. */
    NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_READ_OPE;

    /*  Have we already read the entire file? */
    if(NdefMap->ApduBuffIndex < NdefMap->ApduBufferSize)
    {
        /* set the data for additional data exchange */
        NdefMap->psDepAdditionalInfo.DepFlags.MetaChaining = PH_FRINFC_NDEFMAP_MFUL_VAL0;
        NdefMap->psDepAdditionalInfo.DepFlags.NADPresent = PH_FRINFC_NDEFMAP_MFUL_VAL0;
        NdefMap->psDepAdditionalInfo.NAD = PH_FRINFC_NDEFMAP_MFUL_VAL0;
        NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL0] = 
                                NdefMap->MifareULContainer.CurrentBlock;
        NdefMap->SendLength = PH_FRINFC_NDEFMAP_MFUL_VAL1;
        *NdefMap->SendRecvLength = NdefMap->TempReceiveLength;
        /*
         * Changed 
         * Description: replace with  phHal_eMifareRead
         */

        NdefMap->Cmd.MfCmd =  phHal_eMifareRead;
        
        /* Call the overlapped HAL Transceive function */ 
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

static NFCSTATUS   phFriNfc_MfUL_H_Wr4bytes(  phFriNfc_NdefMap_t  *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;

    /* set the receive length*/
    *NdefMap->SendRecvLength = NdefMap->TempReceiveLength;
                                    
    NdefMap->PrevOperation = PH_FRINFC_NDEFMAP_WRITE_OPE;
    
    /*
     * Changed
     * Description: phHal_eMifareCmdListMifareWrite4 replace with phHal_eMifareWrite4
     */
    /* set the cmd to mifare read*/
    NdefMap->Cmd.MfCmd = phHal_eMifareWrite4;
    
    /* Set the CR and context for Mifare operations*/
    NdefMap->MapCompletionInfo.CompletionRoutine = phFriNfc_MifareUL_Process;
    NdefMap->MapCompletionInfo.Context = NdefMap;

    NdefMap->SendLength = PH_FRINFC_NDEFMAP_MFUL_WR_A_BLK;
    /*Call the Overlapped HAL Transceive function */ 
    Result = phFriNfc_OvrHal_Transceive(NdefMap->LowerDevice,
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

static NFCSTATUS phFriNfc_MfUL_H_findNDEFTLV(phFriNfc_NdefMap_t     *NdefMap, 
                                            uint8_t                 *CRFlag)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint16_t    ShiftLength = PH_FRINFC_NDEFMAP_MFUL_VAL0,
                TemLength = PH_FRINFC_NDEFMAP_MFUL_VAL0,
                Temp16Bytes = PH_FRINFC_NDEFMAP_MFUL_VAL0;
    Temp16Bytes = ((NdefMap->TLVStruct.NdefTLVByte > PH_FRINFC_NDEFMAP_MFUL_VAL0)?
                    (NdefMap->TLVStruct.NdefTLVByte - PH_FRINFC_NDEFMAP_MFUL_VAL1):
                    NdefMap->TLVStruct.NdefTLVByte);
    for(;;)
    {
        if(NdefMap->SendRecvBuf[Temp16Bytes] == 
                        PH_FRINFC_NDEFMAP_MFUL_NULLTLV)
        {
            NdefMap->MifareULContainer.RemainingSize -= 
                        PH_FRINFC_NDEFMAP_MFUL_VAL1;
#ifdef PH_HAL4_ENABLE
            /* This check is added to know the remaining size in 
            the card is not 0, if this is 0, then complete card has 
            been read */
            if (NdefMap->MifareULContainer.RemainingSize == 
                        PH_FRINFC_NDEFMAP_MFUL_VAL0)
            {
                Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_NO_NDEF_SUPPORT);
                break;
            }
            else
            {
                Result = NFCSTATUS_SUCCESS;                
            }
#else
            Result = ((NdefMap->MifareULContainer.RemainingSize == 
                        PH_FRINFC_NDEFMAP_MFUL_VAL0)?
                        (PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_NO_NDEF_SUPPORT)):
                        NFCSTATUS_SUCCESS);
#endif /* #ifdef PH_HAL4_ENABLE */
            Temp16Bytes++;
#ifdef PH_HAL4_ENABLE
            /* This code is added to read next 16 bytes. This means previous 
            16 bytes read contains only NULL TLV, so read further to get the 
            NDEF TLV */
            Result = phFriNfc_MfUL_H_Chk16Bytes(NdefMap,
                                                Temp16Bytes);
            if(NFCSTATUS_SUCCESS != Result)
            {
                NdefMap->TLVStruct.NdefTLVBlock = 
                    NdefMap->TLVStruct.NdefTLVBlock + PH_FRINFC_NDEFMAP_MFUL_VAL4;
                break;
            }
#endif /* #ifdef PH_HAL4_ENABLE */
        }
        else
        {
            Result = ((NdefMap->SendRecvBuf[Temp16Bytes] == 
                        PH_FRINFC_NDEFMAP_MFUL_TERMTLV)?
                        (PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_NO_NDEF_SUPPORT)):
                        NFCSTATUS_SUCCESS);

            if(Result != NFCSTATUS_SUCCESS)
            {
                *CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                break;
            }
            
#ifdef PH_NDEF_MIFARE_ULC
            if ((NdefMap->SendRecvBuf[Temp16Bytes] == 
                        PH_FRINFC_NDEFMAP_MFUL_LOCK_CTRL_TLV) ||
                        (NdefMap->SendRecvBuf[Temp16Bytes] == 
                        PH_FRINFC_NDEFMAP_MFUL_MEM_CTRL_TLV) ||
                        (NdefMap->SendRecvBuf[Temp16Bytes] ==
                        PH_FRINFC_NDEFMAP_MFUL_PROPRIETRY_TLV))
            {

                 NdefMap->TLVStruct.NdefTLVByte = 
                                    ((Temp16Bytes % 
                                    PH_FRINFC_NDEFMAP_MFUL_VAL4) + 
                                    PH_FRINFC_NDEFMAP_MFUL_VAL1);
            
                 Result = phFriNfc_MfUL_H_Chk16Bytes(NdefMap,
                                                Temp16Bytes);
                if(Result != NFCSTATUS_SUCCESS)
                {
                    NdefMap->TLVStruct.TcheckedinTLVFlag = 
                                            PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                    NdefMap->TLVStruct.NoLbytesinTLV = 
                                            PH_FRINFC_NDEFMAP_MFUL_VAL3;
                    break;
                }
                Temp16Bytes++;
                NdefMap->MifareULContainer.RemainingSize -= 
                                PH_FRINFC_NDEFMAP_MFUL_VAL1;

                if(NdefMap->MifareULContainer.RemainingSize == 
                  PH_FRINFC_NDEFMAP_MFUL_VAL0)
                {
                    Result = (PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                        NFCSTATUS_NO_NDEF_SUPPORT));
                    break;
                }
                
                Result = phFriNfc_MfUL_H_Chk16Bytes(NdefMap,
                                                    Temp16Bytes);
                if(Result != NFCSTATUS_SUCCESS)
                {
                    NdefMap->TLVStruct.TcheckedinTLVFlag = 
                                        PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                    NdefMap->TLVStruct.NoLbytesinTLV = 
                                        PH_FRINFC_NDEFMAP_MFUL_VAL3;
                    break;
                }


                 /* If the value of the Length(L) in TLV is FF then enter else
                            check for the card memory */
                if((NdefMap->SendRecvBuf[Temp16Bytes] == 
                    PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_LFF) || 
                    ((NdefMap->SendRecvBuf[Temp16Bytes] == 
                    PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_L) && 
                    (NdefMap->TLVStruct.NdefTLVFoundFlag != 
                    PH_FRINFC_NDEFMAP_MFUL_FLAG1)))
                {
                    /* In the present case, the card space is not greater 
                        than 0xFF */
                    Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                        NFCSTATUS_NO_NDEF_SUPPORT);

                    *CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                    break;
                }
                else
                {
                    NdefMap->TLVStruct.BytesRemainLinTLV = 
                                    NdefMap->SendRecvBuf[Temp16Bytes];

                    NdefMap->TLVStruct.ActualSize = 
                                    NdefMap->SendRecvBuf[Temp16Bytes];

                    if((NdefMap->MifareULContainer.RemainingSize < 
                        NdefMap->SendRecvBuf[Temp16Bytes]) || 
                        (NdefMap->MifareULContainer.RemainingSize < 
                        PH_FRINFC_NDEFMAP_MFUL_VAL2) || 
                        (NdefMap->TLVStruct.BytesRemainLinTLV > 
                        (NdefMap->MifareULContainer.RemainingSize)) || 
                        ((NdefMap->TLVStruct.BytesRemainLinTLV == 
                        PH_FRINFC_NDEFMAP_MFUL_VAL0) && 
                        (NdefMap->PrevOperation == PH_FRINFC_NDEFMAP_READ_OPE)))
                    {
                        /* No NDEF TLV found */
                        Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                            NFCSTATUS_NO_NDEF_SUPPORT);
                        *CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                        break;
                    }
                    
                    if(NdefMap->TLVStruct.NdefTLVFoundFlag != 
                        PH_FRINFC_NDEFMAP_MFUL_FLAG1)
                    {
                        NdefMap->TLVStruct.NdefTLVByte = 
                                        (((Temp16Bytes + PH_FRINFC_NDEFMAP_MFUL_VAL1 +
                                        NdefMap->SendRecvBuf[Temp16Bytes]) % 
                                        PH_FRINFC_NDEFMAP_MFUL_VAL4) + 
                                        PH_FRINFC_NDEFMAP_MFUL_VAL1);
#if 0                       
                        NdefMap->TLVStruct.NdefTLVBlock = 
                                        (uint8_t)(NdefMap->TLVStruct.NdefTLVBlock 
                                        + ((Temp16Bytes + 
                                        NdefMap->SendRecvBuf[Temp16Bytes] + 1)/ 
                                        PH_FRINFC_NDEFMAP_MFUL_VAL4));
#endif
                        NdefMap->TLVStruct.NdefTLVBlock = 
                                        (uint8_t)(((NdefMap->TLVStruct.NdefTLVBlock / PH_FRINFC_NDEFMAP_MFUL_VAL4) * 
                                            PH_FRINFC_NDEFMAP_MFUL_VAL4)
                                            + ((Temp16Bytes + NdefMap->SendRecvBuf[Temp16Bytes] + 1)/ 
                                            PH_FRINFC_NDEFMAP_MFUL_VAL4));


                        TemLength = (Temp16Bytes + 
                                NdefMap->SendRecvBuf[Temp16Bytes]);

                        NdefMap->MifareULContainer.RemainingSize = 
                                        (NdefMap->MifareULContainer.RemainingSize -
                                        (NdefMap->SendRecvBuf[Temp16Bytes]
                                        + PH_FRINFC_NDEFMAP_MFUL_VAL1)); 

                        /* If the Length (L) in TLV < 16 bytes */
                        Temp16Bytes = ((TemLength >= 
                                PH_FRINFC_NDEFMAP_MFUL_RDBYTES_16)?
                                PH_FRINFC_NDEFMAP_MFUL_VAL0:
                                (TemLength + 
                                PH_FRINFC_NDEFMAP_MFUL_VAL1));

                        Result = ((TemLength >= 
                                PH_FRINFC_NDEFMAP_MFUL_RDBYTES_16)?
                                phFriNfc_MfUL_H_RdCardfindNdefTLV(NdefMap,
                                            NdefMap->TLVStruct.NdefTLVBlock):
                                NFCSTATUS_SUCCESS);
                        
                        if(TemLength >= PH_FRINFC_NDEFMAP_MFUL_RDBYTES_16)
                        {
                            break;
                        }
                        TemLength = Temp16Bytes;
                    }
                }




#if 0   
            
                 NdefMap->MifareULContainer.RemainingSize = 
                                    (NdefMap->MifareULContainer.RemainingSize -
                                    (NdefMap->SendRecvBuf[Temp16Bytes + 1]
                                    + PH_FRINFC_NDEFMAP_MFUL_VAL2)); 

                NdefMap->TLVStruct.NdefTLVBlock = 
                                    (uint8_t)(NdefMap->TLVStruct.NdefTLVBlock 
                                    + ((Temp16Bytes + 
                                    NdefMap->SendRecvBuf[Temp16Bytes + 1] + 2)/ 
                                    PH_FRINFC_NDEFMAP_MFUL_VAL4));

                
                Temp16Bytes = Temp16Bytes + 
                        NdefMap->SendRecvBuf[Temp16Bytes + 1] + 2;
#endif                
            }
#endif /* #ifdef PH_NDEF_MIFARE_ULC */
            else {
                
            /* Check the byte for 0x03 Type of NDEF TLV */
            NdefMap->TLVStruct.NdefTLVFoundFlag = 
                    ((NdefMap->SendRecvBuf[Temp16Bytes] == 
                    PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_T)?
                    PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                    PH_FRINFC_NDEFMAP_MFUL_FLAG0);

            if(NdefMap->TLVStruct.NdefTLVFoundFlag == 
                PH_FRINFC_NDEFMAP_MFUL_FLAG1)
            {
                ShiftLength = (Temp16Bytes + 
                                    NdefMap->SendRecvBuf[Temp16Bytes]);

                 NdefMap->TLVStruct.NdefTLVByte = 
                                    ((Temp16Bytes % 
                                    PH_FRINFC_NDEFMAP_MFUL_VAL4) + 
                                    PH_FRINFC_NDEFMAP_MFUL_VAL1);

                NdefMap->TLVStruct.NdefTLVBlock = 
                                    (uint8_t)(((NdefMap->TLVStruct.NdefTLVBlock /4) * 4)
                                    + (Temp16Bytes)/ 
                                    PH_FRINFC_NDEFMAP_MFUL_VAL4);
                
                NdefMap->TLVStruct.NdefTLVSector = NdefMap->MifareULContainer.CurrentSector;
                
            }            
#ifdef PH_HAL4_ENABLE
            else
            {
                /* if the Type of the NDEF TLV is not found, then return  
                    error saying no ndef TLV found*/
                Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_NO_NDEF_SUPPORT);
                break;
#if 0
                /* This change is added to continue the loop, if the Type of the 
                    NDEF TLV is not found 
                    16 bytes are read, so for each byte, there is a check for the 
                    Type (T) of the TLV, if T != 0x03, then increment the byte 
                    count and restart the loop, till the T = 0x03 is found or all 
                    the bytes in the card is completely read.
                */
                Temp16Bytes = (uint16_t)(Temp16Bytes + 1);
                NdefMap->MifareULContainer.RemainingSize -= 
                        PH_FRINFC_NDEFMAP_MFUL_VAL1;
                if (NdefMap->MifareULContainer.RemainingSize == 
                            PH_FRINFC_NDEFMAP_MFUL_VAL0)
                {                    
                    Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                        NFCSTATUS_NO_NDEF_SUPPORT);
                    break;
                }
                else
                {
                    Result = NFCSTATUS_SUCCESS;
                    Result = phFriNfc_MfUL_H_Chk16Bytes(NdefMap,
                                                Temp16Bytes);
                    if(NFCSTATUS_PENDING == Result)
                    {
                        break;
                    }
                    continue;                    
                }
#endif /* #if 0 */
            }
#endif /* #ifdef PH_HAL4_ENABLE */

            Result = phFriNfc_MfUL_H_Chk16Bytes(NdefMap,
                                                Temp16Bytes);
            if(Result != NFCSTATUS_SUCCESS)
            {
                NdefMap->TLVStruct.TcheckedinTLVFlag = 
                                        PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                NdefMap->TLVStruct.NoLbytesinTLV = 
                                        PH_FRINFC_NDEFMAP_MFUL_VAL3;
                break;
            }
            Temp16Bytes++;
            NdefMap->MifareULContainer.RemainingSize -= 
                            PH_FRINFC_NDEFMAP_MFUL_VAL1;

            if(NdefMap->MifareULContainer.RemainingSize == 
              PH_FRINFC_NDEFMAP_MFUL_VAL0)
            {
                Result = (PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_NO_NDEF_SUPPORT));
                break;
            }
            
            Result = phFriNfc_MfUL_H_Chk16Bytes(NdefMap,
                                                Temp16Bytes);
            if(Result != NFCSTATUS_SUCCESS)
            {
                NdefMap->TLVStruct.TcheckedinTLVFlag = 
                                    PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                NdefMap->TLVStruct.NoLbytesinTLV = 
                                    PH_FRINFC_NDEFMAP_MFUL_VAL3;
                break;
            }
            
            /* If the value of the Length(L) in TLV is FF then enter else
                check for the card memory */
            if((NdefMap->SendRecvBuf[Temp16Bytes] == 
                PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_LFF) || 
                ((NdefMap->SendRecvBuf[Temp16Bytes] == 
                PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_L) && 
                (NdefMap->TLVStruct.NdefTLVFoundFlag != 
                PH_FRINFC_NDEFMAP_MFUL_FLAG1)))
            {
                /* In the present case, the card space is not greater 
                    than 0xFF */
                /*
                Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_NO_NDEF_SUPPORT);

                *CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                break;
                */

                Temp16Bytes++;
                Result = phFriNfc_MfUL_H_Chk16Bytes(NdefMap,
                                                    Temp16Bytes);
                if(Result != NFCSTATUS_SUCCESS)
                {
                    NdefMap->TLVStruct.TcheckedinTLVFlag = 
                                            PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                    NdefMap->TLVStruct.NoLbytesinTLV = 
                                            PH_FRINFC_NDEFMAP_MFUL_VAL2;
                    
                    break;
                }
                
                ShiftLength = (uint16_t) NdefMap->SendRecvBuf[Temp16Bytes];
                NdefMap->MifareULContainer.RemainingSize--;

                Temp16Bytes++;
                Result = phFriNfc_MfUL_H_Chk16Bytes(NdefMap,
                                                    Temp16Bytes);
                if(Result != NFCSTATUS_SUCCESS)
                {
                    NdefMap->TLVStruct.TcheckedinTLVFlag = 
                                            PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                    NdefMap->TLVStruct.NoLbytesinTLV = 
                                            PH_FRINFC_NDEFMAP_MFUL_VAL1;
                    NdefMap->TLVStruct.prevLenByteValue = 
                                    NdefMap->SendRecvBuf[Temp16Bytes - 1];
                    break;
                }


                ShiftLength = 
                    (uint16_t) (NdefMap->SendRecvBuf[Temp16Bytes]
                         | (ShiftLength << PH_FRINFC_NDEFMAP_MFUL_SHIFT8));

    //          NdefMap->MifareULContainer.RemainingSize--;

                if(ShiftLength > (NdefMap->MifareULContainer.RemainingSize))
                {
                    // Size in the Length(L) of TLV is greater 
                    //than the actual size of the card 
                    Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                        NFCSTATUS_INVALID_PARAMETER);
                    *CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                    break;
                }

        //      NdefMap->MifareULContainer.RemainingSize--;
                /*
                NdefMap->TLVStruct.NdefTLVByte = 
                                    (NdefMap->SendRecvBuf[Temp16Bytes] % 
                                    PH_FRINFC_NDEFMAP_MFUL_VAL4);
                
                NdefMap->TLVStruct.NdefTLVBlock = 
                                (uint8_t)(NdefMap->MifareULContainer.CurrentBlock 
                            + (Temp16Bytes/PH_FRINFC_NDEFMAP_MFUL_VAL4));
                */  
                
                NdefMap->TLVStruct.ActualSize = 
                    NdefMap->TLVStruct.BytesRemainLinTLV = ShiftLength;
                    
                NdefMap->TLVStruct.NdefTLVFoundFlag = 1;
                
                NdefMap->TLVStruct.NdefTLVSector = NdefMap->MifareULContainer.CurrentSector;

                
                Result = ((NdefMap->TLVStruct.NoLbytesinTLV == 
                                    PH_FRINFC_NDEFMAP_MFUL_VAL0)?
                                    phFriNfc_MapTool_SetCardState(  NdefMap, ShiftLength):
                                    Result);
/*  
                Result = phFriNfc_MfUL_H_RdCardfindNdefTLV(NdefMap,
                                                    NdefMap->TLVStruct.NdefTLVBlock);
*/
                break;
            }
            else
            {
                NdefMap->TLVStruct.BytesRemainLinTLV = 
                                NdefMap->SendRecvBuf[Temp16Bytes];

                NdefMap->TLVStruct.ActualSize = 
                                NdefMap->SendRecvBuf[Temp16Bytes];

                if((NdefMap->MifareULContainer.RemainingSize < 
                    NdefMap->SendRecvBuf[Temp16Bytes]) || 
                    (NdefMap->MifareULContainer.RemainingSize < 
                    PH_FRINFC_NDEFMAP_MFUL_VAL2) || 
                    (NdefMap->TLVStruct.BytesRemainLinTLV > 
                    (NdefMap->MifareULContainer.RemainingSize)) || 
                    ((NdefMap->TLVStruct.BytesRemainLinTLV == 
                    PH_FRINFC_NDEFMAP_MFUL_VAL0) && 
                    (NdefMap->PrevOperation == PH_FRINFC_NDEFMAP_READ_OPE)))
                {
                    /* No NDEF TLV found */
                    Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                        NFCSTATUS_NO_NDEF_SUPPORT);
                    *CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                    break;
                }
                
                if(NdefMap->TLVStruct.NdefTLVFoundFlag != 
                    PH_FRINFC_NDEFMAP_MFUL_FLAG1)
                {
                    NdefMap->TLVStruct.NdefTLVByte = 
                                    (((Temp16Bytes + PH_FRINFC_NDEFMAP_MFUL_VAL1 +
                                    NdefMap->SendRecvBuf[Temp16Bytes]) % 
                                    PH_FRINFC_NDEFMAP_MFUL_VAL4) + 
                                    PH_FRINFC_NDEFMAP_MFUL_VAL1);
                    NdefMap->TLVStruct.NdefTLVBlock = 
                                    (uint8_t)(NdefMap->TLVStruct.NdefTLVBlock 
                                    + ((Temp16Bytes + 
                                    NdefMap->SendRecvBuf[Temp16Bytes] + 1)/ 
                                    PH_FRINFC_NDEFMAP_MFUL_VAL4));

                    TemLength = (Temp16Bytes + 
                            NdefMap->SendRecvBuf[Temp16Bytes]);

                    NdefMap->MifareULContainer.RemainingSize = 
                                    (NdefMap->MifareULContainer.RemainingSize -
                                    (NdefMap->SendRecvBuf[Temp16Bytes]
                                    + PH_FRINFC_NDEFMAP_MFUL_VAL1)); 

                    /* If the Length (L) in TLV < 16 bytes */
                    Temp16Bytes = ((TemLength >= 
                            PH_FRINFC_NDEFMAP_MFUL_RDBYTES_16)?
                            PH_FRINFC_NDEFMAP_MFUL_VAL0:
                            (TemLength + 
                            PH_FRINFC_NDEFMAP_MFUL_VAL1));

                    Result = ((TemLength >= 
                            PH_FRINFC_NDEFMAP_MFUL_RDBYTES_16)?
                            phFriNfc_MfUL_H_RdCardfindNdefTLV(NdefMap,
                                        NdefMap->TLVStruct.NdefTLVBlock):
                            NFCSTATUS_SUCCESS);
                    
                    if(TemLength >= PH_FRINFC_NDEFMAP_MFUL_RDBYTES_16)
                    {
                        break;
                    }
                    TemLength = Temp16Bytes;
                }
            }
            if(NdefMap->TLVStruct.NdefTLVFoundFlag == 
                        PH_FRINFC_NDEFMAP_MFUL_FLAG1)
            {
#if 0
                NdefMap->TLVStruct.NdefTLVBlock = 
                                    (uint8_t)(NdefMap->TLVStruct.NdefTLVBlock 
                                    + ((Temp16Bytes + 1)/ 
                                    PH_FRINFC_NDEFMAP_MFUL_VAL4)) - 1;
#endif
                NdefMap->MifareULContainer.RemainingSize = 
                                    (NdefMap->MifareULContainer.RemainingSize -
                                    PH_FRINFC_NDEFMAP_MFUL_VAL1);
                    ShiftLength = NdefMap->SendRecvBuf[Temp16Bytes];
                    Result = ((NdefMap->TLVStruct.NoLbytesinTLV == 
                            PH_FRINFC_NDEFMAP_MFUL_VAL0)?
                            phFriNfc_MapTool_SetCardState(  NdefMap, ShiftLength):
                            Result);

                break;
            }
        }
        }   
    }

    return Result;
}


static NFCSTATUS phFriNfc_MfUL_H_Chk16Bytes(phFriNfc_NdefMap_t   *NdefMap,
                                            uint16_t             TempLength)
{
    uint16_t localCurrentBlock;
    
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    if(TempLength == PH_FRINFC_NDEFMAP_MFUL_RDBYTES_16)
    {
        localCurrentBlock = NdefMap->MifareULContainer.CurrentBlock +
                            PH_FRINFC_NDEFMAP_MFUL_BLOCK4;

        if (localCurrentBlock < 256)
        {
            NdefMap->MifareULContainer.CurrentBlock += 
                                PH_FRINFC_NDEFMAP_MFUL_BLOCK4;
                        
            Result = phFriNfc_MfUL_H_RdCardfindNdefTLV(NdefMap,
                            NdefMap->MifareULContainer.CurrentBlock);
        }
        else 
        {
            /* Go to next sector */
            NdefMap->MifareULContainer.CurrentSector++;

            Result = phFriNfc_MfUL_H_SelectSector(NdefMap, 
                NdefMap->MifareULContainer.CurrentSector, 1, 
                PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_CHK_1);
        }
    }

    return Result;
}


static void phFriNfc_MfUL_H_UpdateCrc( uint8_t ch, 
                                          uint16_t *lpwCrc )
{
    ch = (ch^(uint8_t)((*lpwCrc) & 0x00FF));
    ch = (ch^(ch<<4));
    *lpwCrc = (*lpwCrc >> 8)^((uint16_t)ch << 8)^ \
                ((uint16_t)ch<<3)^((uint16_t)ch>>4);
    
    return;
}

static void phFriNfc_MfUL_H_ComputeCrc( int      CRCType, 
                                 uint8_t  *Data, 
                                 int      Length,
                                 uint8_t  *TransmitFirst, 
                                 uint8_t  *TransmitSecond
                                 )
{
    uint8_t chBlock;
    uint16_t wCrc;
    switch(CRCType) 
    {
    case CRC_A:
        wCrc = 0x6363; /* ITU-V.41 */
        break;
    case CRC_B:
        wCrc = 0xFFFF; /* ISO/IEC 13239 (formerly ISO/IEC 3309) */
        break;
    default:
        return;
    }

    do 
    {
        chBlock = *Data++;
        phFriNfc_MfUL_H_UpdateCrc(chBlock, &wCrc);
    } while (--Length);
    *TransmitFirst = (uint8_t) (wCrc & 0xFF);
    *TransmitSecond = (uint8_t) ((wCrc >> 8) & 0xFF);
    return;
}



static NFCSTATUS  phFriNfc_MfUL_H_SelectSector(phFriNfc_NdefMap_t  *NdefMap,
                                                   uint8_t              SectorNo,
                                                   uint8_t              CmdNo,
                                                   uint8_t              NextState)
{
    
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;

    /* set the data for additional data exchange */
    NdefMap->psDepAdditionalInfo.DepFlags.MetaChaining = PH_FRINFC_NDEFMAP_MFUL_VAL0;
    NdefMap->psDepAdditionalInfo.DepFlags.NADPresent = PH_FRINFC_NDEFMAP_MFUL_VAL0;
    NdefMap->psDepAdditionalInfo.NAD = PH_FRINFC_NDEFMAP_MFUL_VAL0;

    NdefMap->State = NextState;

    if (CmdNo == 1)
    {
        NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL0] = 0x00;
        NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL1] = 0x00;
        NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL2] = 0xC2;
        NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL3] = 0xFF;
        NdefMap->SendLength = PH_FRINFC_NDEFMAP_MFUL_VAL4;                                               
    }
    else
    {
        NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL0] = 0x00;
        NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL1] = 0x00;
        NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL2] = SectorNo;
        NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL3] = 0;
        NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL4] = 0;
        NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL5] = 0;
        NdefMap->SendLength = PH_FRINFC_NDEFMAP_MFUL_VAL5 + 1;
    }
    
    /* Calculate CRC */

    phFriNfc_MfUL_H_ComputeCrc(CRC_A, &NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL2], 
            NdefMap->SendLength - 2, 
            &NdefMap->SendRecvBuf[NdefMap->SendLength], 
            &NdefMap->SendRecvBuf[NdefMap->SendLength + 1]);
    
    NdefMap->SendLength += PH_FRINFC_NDEFMAP_MFUL_VAL2; 

    
    *NdefMap->SendRecvLength = NdefMap->TempReceiveLength;
     
    NdefMap->Cmd.MfCmd = phHal_eMifareRaw;
     
    /* Call the overlapped HAL Transceive function */ 
    Result = phFriNfc_OvrHal_Transceive(     NdefMap->LowerDevice,
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


static NFCSTATUS   phFriNfc_MfUL_H_RdCardfindNdefTLV( phFriNfc_NdefMap_t  *NdefMap,
                                                   uint8_t              BlockNo)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    NdefMap->State = PH_FRINFC_NDEFMAP_MFUL_STATE_FND_NDEF_COMP;
    /* set the data for additional data exchange */
    NdefMap->psDepAdditionalInfo.DepFlags.MetaChaining = PH_FRINFC_NDEFMAP_MFUL_VAL0;
    NdefMap->psDepAdditionalInfo.DepFlags.NADPresent = PH_FRINFC_NDEFMAP_MFUL_VAL0;
    NdefMap->psDepAdditionalInfo.NAD = PH_FRINFC_NDEFMAP_MFUL_VAL0;
    NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL0] = 
                                                    BlockNo;
    NdefMap->SendLength = PH_FRINFC_NDEFMAP_MFUL_VAL1;
    *NdefMap->SendRecvLength = NdefMap->TempReceiveLength;
    
    /* 
     * Changed 
     * Description: phHal_eMifareCmdListMifareRead replace with phHal_eMifareRead
     */
    NdefMap->Cmd.MfCmd = phHal_eMifareRead;
    
    /* Call the overlapped HAL Transceive function */ 
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

static NFCSTATUS phFriNfc_MfUL_H_ChkRemainTLV(phFriNfc_NdefMap_t  *NdefMap,
                                              uint8_t             *CRFlag)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint16_t    TempLength = PH_FRINFC_NDEFMAP_MFUL_VAL0,
                ShiftLength = PH_FRINFC_NDEFMAP_MFUL_VAL0;

    switch(NdefMap->TLVStruct.NoLbytesinTLV)
    {
        case PH_FRINFC_NDEFMAP_MFUL_VAL1:
        case PH_FRINFC_NDEFMAP_MFUL_VAL2:
            ShiftLength = ((NdefMap->TLVStruct.NoLbytesinTLV == 
                            PH_FRINFC_NDEFMAP_MFUL_VAL1)?
                            NdefMap->TLVStruct.prevLenByteValue:
                            NdefMap->SendRecvBuf[TempLength]);
            ShiftLength = ((NdefMap->TLVStruct.NoLbytesinTLV == 
                            PH_FRINFC_NDEFMAP_MFUL_VAL1)?
                            (((uint16_t)(NdefMap->SendRecvBuf[TempLength]) << 
                            PH_FRINFC_NDEFMAP_MFUL_SHIFT8) | 
                            ShiftLength):
                            (((uint16_t)(NdefMap->SendRecvBuf[(TempLength + 
                            PH_FRINFC_NDEFMAP_MFUL_VAL1)]) << 
                            PH_FRINFC_NDEFMAP_MFUL_SHIFT8) | 
                            ShiftLength));

            NdefMap->MifareULContainer.RemainingSize -= 
                                            PH_FRINFC_NDEFMAP_MFUL_VAL1;

            NdefMap->TLVStruct.ActualSize = 
            NdefMap->TLVStruct.BytesRemainLinTLV = ShiftLength;

            /* Check for remaining free space in the card with the 
                length (L) of TLV OR length(L) of TLV is less than 
                255 bytes (The length (L) of TLV for 3 byte should not 
                be less than 255) */ 
            Result = ((((NdefMap->MifareULContainer.RemainingSize)<= 
                        ShiftLength) || (ShiftLength < 
                        PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_LFF))?
                        (PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                        NFCSTATUS_INVALID_PARAMETER)):
                        Result);

            
            Result = ((Result == NFCSTATUS_SUCCESS)?
                            phFriNfc_MapTool_SetCardState(  NdefMap, ShiftLength):
                            Result);

            *CRFlag = (uint8_t)((Result == (PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                        NFCSTATUS_INVALID_PARAMETER)))?
                        PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                        PH_FRINFC_NDEFMAP_MFUL_FLAG0);


            if(Result == NFCSTATUS_SUCCESS)
            {

                NdefMap->MifareULContainer.RemainingSize= NdefMap->MifareULContainer.RemainingSize-
                    NdefMap->TLVStruct.NoLbytesinTLV;
/*              
                NdefMap->TLVStruct.NdefTLVByte = ((ShiftLength% 
                        PH_FRINFC_NDEFMAP_MFUL_VAL4) + 
                        PH_FRINFC_NDEFMAP_MFUL_VAL1);
                       
                NdefMap->TLVStruct.NdefTLVBlock = 
                        (uint8_t)(NdefMap->MifareULContainer.CurrentBlock 
                        + (ShiftLength/PH_FRINFC_NDEFMAP_MFUL_VAL4));
                NdefMap->MifareULContainer.CurrentBlock = 
                                NdefMap->TLVStruct.NdefTLVBlock;

                Result = phFriNfc_MfUL_H_RdCardfindNdefTLV(NdefMap, 
                                                NdefMap->TLVStruct.NdefTLVBlock);
                                                */
            }
            break;      

        default:
            if((NdefMap->SendRecvBuf[TempLength] == 
                PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_LFF) || 
                ((NdefMap->SendRecvBuf[TempLength] == 
                PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_L) && 
                (NdefMap->TLVStruct.NdefTLVFoundFlag != 
                PH_FRINFC_NDEFMAP_MFUL_FLAG1)))
            {
                /* In the present case, the card space is not greater 
                    than 0xFF */
/*                    
                Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_NO_NDEF_SUPPORT);

                *CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
                
*/
            
                ShiftLength = NdefMap->SendRecvBuf[(TempLength + PH_FRINFC_NDEFMAP_MFUL_VAL1)];
                ShiftLength = (((uint16_t)(NdefMap->SendRecvBuf[(TempLength + PH_FRINFC_NDEFMAP_MFUL_VAL2)]) 
                                << PH_FRINFC_NDEFMAP_MFUL_SHIFT8) | 
                                ShiftLength);
                Result = ((ShiftLength > (NdefMap->MifareULContainer.RemainingSize))?
                            (PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                            NFCSTATUS_INVALID_PARAMETER)):
                            Result);

                            
                Result = ((Result == NFCSTATUS_SUCCESS)?
                                            phFriNfc_MapTool_SetCardState(  NdefMap, ShiftLength):
                                            Result);

                NdefMap->TLVStruct.ActualSize = 
                    NdefMap->TLVStruct.BytesRemainLinTLV = ShiftLength;
                
                if(Result == NFCSTATUS_SUCCESS)
                {
                    
                    NdefMap->MifareULContainer.RemainingSize= NdefMap->MifareULContainer.RemainingSize-
                    NdefMap->TLVStruct.NoLbytesinTLV;
/*                  
                    NdefMap->TLVStruct.NdefTLVByte = ((ShiftLength% 
                        PH_FRINFC_NDEFMAP_MFUL_VAL4) + 
                        PH_FRINFC_NDEFMAP_MFUL_VAL1);
                        
                    NdefMap->TLVStruct.NdefTLVBlock = 
                        (uint8_t)(NdefMap->MifareULContainer.CurrentBlock 
                        + (ShiftLength/PH_FRINFC_NDEFMAP_MFUL_VAL4));

                    NdefMap->MifareULContainer.CurrentBlock = 
                                NdefMap->TLVStruct.NdefTLVBlock;

                    Result = phFriNfc_MfUL_H_RdCardfindNdefTLV(NdefMap,
                                                        NdefMap->TLVStruct.NdefTLVBlock);
*/
                }
            }
            else
            {
                /* length (L) value in TLV shall not be greater than 
                    remaining free space in the card */
                Result = ((NdefMap->SendRecvBuf[TempLength] > 
                    NdefMap->MifareULContainer.RemainingSize)?
                        (PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                        NFCSTATUS_INVALID_PARAMETER)):
                        Result);

                NdefMap->TLVStruct.ActualSize = 
                NdefMap->TLVStruct.BytesRemainLinTLV = 
                                NdefMap->SendRecvBuf[TempLength];
                NdefMap->MifareULContainer.RemainingSize--;

                if((Result == NFCSTATUS_SUCCESS) && 
                    (NdefMap->TLVStruct.NdefTLVFoundFlag != 
                    PH_FRINFC_NDEFMAP_MFUL_FLAG1))
                {
                    phFriNfc_MfUL_H_UpdateLen(NdefMap, 
                                (uint16_t)NdefMap->SendRecvBuf[TempLength]);
                    
                    NdefMap->MifareULContainer.CurrentBlock =  
                                        NdefMap->TLVStruct.NdefTLVBlock;
                    TempLength=TempLength+(NdefMap->SendRecvBuf[TempLength]);
                    Result =((TempLength < PH_FRINFC_NDEFMAP_MFUL_RDBYTES_16)?
                            phFriNfc_MfUL_H_findNDEFTLV(NdefMap, CRFlag):
                            phFriNfc_MfUL_H_RdCardfindNdefTLV(NdefMap,
                                    NdefMap->TLVStruct.NdefTLVBlock));
                }
            }
            break;
    }
    NdefMap->TLVStruct.NoLbytesinTLV = PH_FRINFC_NDEFMAP_MFUL_VAL0;

    Result = phFriNfc_MapTool_SetCardState(  NdefMap, NdefMap->TLVStruct.ActualSize);

    return Result;
}

static void phFriNfc_MfUL_H_UpdateLen(phFriNfc_NdefMap_t        *NdefMap,
                                      uint16_t                  DataLen) 
{
    NdefMap->MifareULContainer.RemainingSize= NdefMap->MifareULContainer.RemainingSize-DataLen;
    NdefMap->TLVStruct.NdefTLVByte = ((DataLen % 
                        PH_FRINFC_NDEFMAP_MFUL_VAL4) + 
                        PH_FRINFC_NDEFMAP_MFUL_VAL1);
    NdefMap->TLVStruct.NdefTLVBlock = 
                        (uint8_t)(NdefMap->MifareULContainer.CurrentBlock 
                        + (DataLen/PH_FRINFC_NDEFMAP_MFUL_VAL4));
}

 static NFCSTATUS phFriNfc_MfUL_H_NxtOp(phFriNfc_NdefMap_t       *NdefMap,
                                       uint8_t                  *CRFlag)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
  
    switch(NdefMap->PrevOperation)
    {
        case PH_FRINFC_NDEFMAP_CHECK_OPE:
            *CRFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
			/* Fix to check if the actual size in the TLV is greater than card */
            if (NdefMap->TLVStruct.ActualSize > (NdefMap->CardMemSize - 2))
            {
                Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                    NFCSTATUS_NO_NDEF_SUPPORT);
            }
            break;

        case PH_FRINFC_NDEFMAP_READ_OPE:
            if (NdefMap->TLVStruct.NdefTLVSector == 1)
            {
                /* Goto sector 1 */
                NdefMap->MifareULContainer.CurrentSector = 1;
                NdefMap->MifareULContainer.CurrentBlock = 0;
                
                Result = phFriNfc_MfUL_H_SelectSector(NdefMap, 
                        NdefMap->MifareULContainer.CurrentSector, 1, 
                        PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_READ_1); 
            }
            else
            {
                NdefMap->MifareULContainer.CurrentBlock = (NdefMap->TLVStruct.NdefTLVBlock / 4) * 4;

                Result = phFriNfc_MfUL_H_Rd16Bytes(NdefMap);
            }
#if 0           
            NdefMap->MifareULContainer.CurrentBlock = 
                PH_FRINFC_NDEFMAP_MFUL_VAL4;

             Result = phFriNfc_MfUL_H_Rd16Bytes(NdefMap);
#endif
      

            *CRFlag = (uint8_t)((Result != NFCSTATUS_PENDING)?
                        PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                        PH_FRINFC_NDEFMAP_MFUL_FLAG0);
            break;

        case PH_FRINFC_NDEFMAP_WRITE_OPE:
            break;

        default:
            break;
    }
    return Result;
}



static NFCSTATUS phFriNfc_MfUL_H_CopyRdBytes(phFriNfc_NdefMap_t  *NdefMap)
{
    NFCSTATUS Result = NFCSTATUS_SUCCESS;
    uint16_t localCurrentBlock;

#ifndef NDEF_READ_CHANGE
	uint16_t			v_field_byte = 0;

	if (NdefMap->MifareULContainer.CurrentBlock 
		== NdefMap->TLVStruct.NdefTLVBlock)
	{
		if (NdefMap->CardMemSize > (0x12 * PH_FRINFC_NDEFMAP_MFUL_MUL8))
		{
			v_field_byte = NdefMap->TLVStruct.NdefTLVByte;
		}

		/* Calculate the Value field of the TLV to read */
		if (NdefMap->TLVStruct.ActualSize >= 0xFF)
		{
			
			/* here 
				3 is the 3 LENGTH bytes to skip  
				4 is the block size
				1 is to increment the byte number
			*/
			v_field_byte = (uint16_t)
							(((v_field_byte + 3) % 4) + 1);
		}
		else
		{
			/* less than 0xFF */
#if 0
			if ((0x03 == v_field_byte) 
				|| (0x04 == v_field_byte))
			{
				/*
					here 
					1 is the 1 LENGTH byte to skip  
					4 is the block size
					1 is to increment the byte number
				*/
				v_field_byte = (uint16_t)
								(((v_field_byte + 1) % 4) + 1);
			}
			else
			{
				v_field_byte = (uint16_t)
								(v_field_byte + 1);
			}	
#endif /* #if 0 */
		}
	}
#endif /* #ifndef NDEF_READ_CHANGE */

#ifndef NDEF_READ_CHANGE
	(void)memcpy(&(NdefMap->MifareULContainer.ReadBuf[ 
                NdefMap->MifareULContainer.ReadBufIndex]),
                (void *)(NdefMap->SendRecvBuf + v_field_byte),
                (*NdefMap->SendRecvLength - v_field_byte));

	NdefMap->MifareULContainer.ReadBufIndex = (uint16_t)
		(NdefMap->MifareULContainer.ReadBufIndex + 
		(*NdefMap->SendRecvLength - v_field_byte));
#else /* #ifndef NDEF_READ_CHANGE */

    (void)memcpy(&(NdefMap->MifareULContainer.ReadBuf[ 
                NdefMap->MifareULContainer.ReadBufIndex]),
                NdefMap->SendRecvBuf,
                *NdefMap->SendRecvLength);

    NdefMap->MifareULContainer.ReadBufIndex=NdefMap->MifareULContainer.ReadBufIndex +*NdefMap->SendRecvLength;
#endif /* #ifndef NDEF_READ_CHANGE */

    localCurrentBlock = NdefMap->MifareULContainer.CurrentBlock+
                            (uint8_t)((NdefMap->MifareULContainer.ReadBufIndex != 
                            NdefMap->CardMemSize)?
                            PH_FRINFC_NDEFMAP_MFUL_BLOCK4:
                            PH_FRINFC_NDEFMAP_MFUL_VAL0);
    if (localCurrentBlock < 256)
    {
        NdefMap->MifareULContainer.CurrentBlock =  NdefMap->MifareULContainer.CurrentBlock+
                            (uint8_t)((NdefMap->MifareULContainer.ReadBufIndex != 
                            NdefMap->CardMemSize)?
                            PH_FRINFC_NDEFMAP_MFUL_BLOCK4:
                            PH_FRINFC_NDEFMAP_MFUL_VAL0);
    }
    else 
    {
        /* Go to next sector */
        if (NdefMap->MifareULContainer.CurrentSector == 0)
        {
            NdefMap->MifareULContainer.CurrentSector++;
            NdefMap->MifareULContainer.CurrentBlock = 0xff;
            
            Result = phFriNfc_MfUL_H_SelectSector(NdefMap, 
                    NdefMap->MifareULContainer.CurrentSector, 1, 
                    PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_READ_1);
        }
    }
   
    return Result;
}

static NFCSTATUS phFriNfc_MfUL_H_CpDataToUserBuf(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    
    /* Check the user buffer size with the 
    L value of TLV */
    if(NdefMap->ApduBufferSize >=
        NdefMap->TLVStruct.BytesRemainLinTLV)
    {
        (void)memcpy(NdefMap->ApduBuffer,
            &(NdefMap->MifareULContainer.ReadBuf[ 
            NdefMap->MifareULContainer.ByteNumber]),
            NdefMap->TLVStruct.BytesRemainLinTLV);

        *(NdefMap->NumOfBytesRead) = 
                    NdefMap->TLVStruct.BytesRemainLinTLV;
        NdefMap->MifareULContainer.ByteNumber = 
                                PH_FRINFC_NDEFMAP_MFUL_VAL0;
        NdefMap->MifareULContainer.ReadWriteCompleteFlag = 
                                PH_FRINFC_NDEFMAP_MFUL_FLAG1;
        NdefMap->MifareULContainer.RemainingSize =  NdefMap->MifareULContainer.RemainingSize-
                                                    NdefMap->TLVStruct.BytesRemainLinTLV;
        NdefMap->TLVStruct.BytesRemainLinTLV = 
                                PH_FRINFC_NDEFMAP_MFUL_VAL0;
    }
    else
    {
        (void)memcpy(NdefMap->ApduBuffer,
            &(NdefMap->MifareULContainer.ReadBuf[ 
            NdefMap->MifareULContainer.ByteNumber]),
            NdefMap->ApduBufferSize);

        *(NdefMap->NumOfBytesRead) = 
                    NdefMap->ApduBufferSize;
        NdefMap->MifareULContainer.ByteNumber = NdefMap->MifareULContainer.ByteNumber+
                                                (uint16_t)NdefMap->ApduBufferSize;
        NdefMap->MifareULContainer.RemainingSize=NdefMap->MifareULContainer.RemainingSize-
                                                     (uint16_t)NdefMap->ApduBufferSize;
        NdefMap->TLVStruct.BytesRemainLinTLV =  NdefMap->TLVStruct.BytesRemainLinTLV-
                    (uint16_t)NdefMap->ApduBufferSize;
    }
    return Result;
}

static NFCSTATUS phFriNfc_MfUL_H_RdBeforeWrite(phFriNfc_NdefMap_t       *NdefMap)
{
    uint16_t localCurrentBlock = NdefMap->TLVStruct.NdefTLVBlock;

    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint8_t     index = PH_FRINFC_NDEFMAP_MFUL_VAL0,
                i = PH_FRINFC_NDEFMAP_MFUL_VAL0;
    /*            BytesToWrite = PH_FRINFC_NDEFMAP_MFUL_VAL0;*/
    uint16_t    TemLength = PH_FRINFC_NDEFMAP_MFUL_VAL0;
    
    switch((NdefMap->TLVStruct.NdefTLVByte - PH_FRINFC_NDEFMAP_MFUL_VAL1))
    {
        case PH_FRINFC_NDEFMAP_MFUL_VAL0:
            /* go the NDEF TLV block to start write */
            NdefMap->MifareULContainer.CurrentBlock = 
                NdefMap->TLVStruct.NdefTLVBlock;
            /* fill send buffer for write */
            NdefMap->SendRecvBuf[index] = 
                                NdefMap->MifareULContainer.CurrentBlock;
            index++;
            NdefMap->SendRecvBuf[index] = 
                                    PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_T;
            index++;
            if (NdefMap->ApduBufferSize > 254)
            {
                NdefMap->SendRecvBuf[index] = 0xFF;
                index++;
                NdefMap->SendRecvBuf[index] = 
                                    PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_L;
                index++;
                NdefMap->SendRecvBuf[index] = 
                                    PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_L;
                index++;
            }
            else
            {
                NdefMap->SendRecvBuf[index] = 
                                    PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_L;
                index++;    
            }
            

            break;

        case PH_FRINFC_NDEFMAP_MFUL_VAL1:
        case PH_FRINFC_NDEFMAP_MFUL_VAL2:
            /* read to get the previous bytes */
            Result = phFriNfc_MfUL_H_RdCardfindNdefTLV(NdefMap, 
                            NdefMap->TLVStruct.NdefTLVBlock);
            break;

        case PH_FRINFC_NDEFMAP_MFUL_VAL3:
            
            localCurrentBlock = (NdefMap->MifareULContainer.CurrentBlock + 
                                PH_FRINFC_NDEFMAP_MFUL_VAL1);

            if (localCurrentBlock < 256)
            {
            
                NdefMap->MifareULContainer.CurrentBlock = 
                            (NdefMap->MifareULContainer.CurrentBlock + 
                                    PH_FRINFC_NDEFMAP_MFUL_VAL1);
                NdefMap->SendRecvBuf[index] = 
                                    NdefMap->MifareULContainer.CurrentBlock;
                index++;

                if (NdefMap->ApduBufferSize > 254)
                {
                    NdefMap->SendRecvBuf[index] = 0xFF;
                    index++;
                    NdefMap->SendRecvBuf[index] = 
                                        PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_L;
                    index++;
                    NdefMap->SendRecvBuf[index] = 
                                        PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_L;
                    index++;
                }
                else
                {
                    NdefMap->SendRecvBuf[index] = 
                                        PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_L;
                    index++;    
                }
            }
            else
            {
                /* Go to next sector */
                NdefMap->MifareULContainer.CurrentSector++;

                Result = phFriNfc_MfUL_H_SelectSector(NdefMap, 
                    NdefMap->MifareULContainer.CurrentSector, 1, 
                    PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_RW_1);
            }
            break;

        default:
            Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_REMOTE_DEVICE);
            break;
    }

    if((((NdefMap->TLVStruct.NdefTLVByte - 
        PH_FRINFC_NDEFMAP_MFUL_VAL1) == PH_FRINFC_NDEFMAP_MFUL_VAL0) || 
        ((NdefMap->TLVStruct.NdefTLVByte - PH_FRINFC_NDEFMAP_MFUL_VAL1) 
        == PH_FRINFC_NDEFMAP_MFUL_VAL3)) && localCurrentBlock < 256)
    {
        /* Length to know how many bytes has to be written to the card */
        TemLength = (((NdefMap->TLVStruct.NdefTLVByte - PH_FRINFC_NDEFMAP_MFUL_VAL1) == 
                        PH_FRINFC_NDEFMAP_MFUL_VAL0)?
                        PH_FRINFC_NDEFMAP_MFUL_VAL2:
                        PH_FRINFC_NDEFMAP_MFUL_VAL3);

        if (NdefMap->ApduBufferSize > 254)
        {
            TemLength -= 2;
        }

        if(NdefMap->ApduBufferSize >= TemLength)
        {
            /* Prepare the receive buffer */
            (void)memcpy(&(NdefMap->SendRecvBuf[ 
                            index]),
                            &(NdefMap->ApduBuffer[ 
                            NdefMap->ApduBuffIndex]),
                            TemLength);

            /* Number of bytes written to the card from user buffer */
            NdefMap->NumOfBytesWritten = TemLength;

            index = index+(uint8_t)TemLength;
            /* Exact number of bytes written in the card including TLV */
            if (index >= 1)
            {
                *NdefMap->DataCount = (index - PH_FRINFC_NDEFMAP_MFUL_VAL1);
            }
            else
            {
                Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_REMOTE_DEVICE);
            }
        }
        else
        {
            /* Prepare the receive buffer */
            (void)memcpy(&(NdefMap->SendRecvBuf[ 
                            index]),
                            &(NdefMap->ApduBuffer[ 
                            NdefMap->ApduBuffIndex]),
                            (uint16_t)NdefMap->ApduBufferSize);

            /* Number of bytes written to the card from user buffer */
            NdefMap->NumOfBytesWritten = (uint16_t)NdefMap->ApduBufferSize;

            index= index +(uint8_t)NdefMap->ApduBufferSize;
            /* Exact number of bytes written in the card including TLV */
            *NdefMap->DataCount = (index - PH_FRINFC_NDEFMAP_MFUL_VAL1);

            for(i = index; i < PH_FRINFC_NDEFMAP_MFUL_WR_A_BLK; i++)
            {
                NdefMap->SendRecvBuf[i] = (uint8_t)((i == index)?
                        PH_FRINFC_NDEFMAP_MFUL_TERMTLV:
                        PH_FRINFC_NDEFMAP_MFUL_NULLTLV);
                NdefMap->TLVStruct.SetTermTLVFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
            }
        }

        /* store the bytes in buffer till the bytes are 
            written in a block */
        (void)memcpy(NdefMap->MifareULContainer.Buffer,
                        &(NdefMap->SendRecvBuf[ 
                        PH_FRINFC_NDEFMAP_MFUL_VAL1]), 
                        (PH_FRINFC_NDEFMAP_MFUL_WR_A_BLK - 
                        PH_FRINFC_NDEFMAP_MFUL_VAL1));

        (void)memcpy(NdefMap->TLVStruct.NdefTLVBuffer,
                    NdefMap->MifareULContainer.Buffer,
                    (PH_FRINFC_NDEFMAP_MFUL_WR_A_BLK - 
                        PH_FRINFC_NDEFMAP_MFUL_VAL1));
        
        /* Change the state to check ndef compliancy */
        NdefMap->State = PH_FRINFC_NDEFMAP_MFUL_STATE_WRITE;

        Result = phFriNfc_MfUL_H_Wr4bytes(NdefMap);
    }
    return Result;
}

static NFCSTATUS phFriNfc_MfUL_H_CallWrOp(phFriNfc_NdefMap_t        *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
   /* uint8_t     index = PH_FRINFC_NDEFMAP_MFUL_VAL1;*/
    
    
    NdefMap->MifareULContainer.CurrentBlock = 
                        NdefMap->TLVStruct.NdefTLVBlock;
        
    (void)memcpy(&(NdefMap->SendRecvBuf[ 
                PH_FRINFC_NDEFMAP_MFUL_VAL1]),
                NdefMap->SendRecvBuf,
                PH_FRINFC_NDEFMAP_MFUL_VAL4);

    NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL0] = 
                            NdefMap->MifareULContainer.CurrentBlock;

    if (NdefMap->ApduBufferSize > 254)
    {
         NdefMap->SendRecvBuf[(NdefMap->TLVStruct.NdefTLVByte + 
                            PH_FRINFC_NDEFMAP_MFUL_VAL1)] = 0xFF;
                            

        if((NdefMap->TLVStruct.NdefTLVByte + PH_FRINFC_NDEFMAP_MFUL_VAL1) < 
            PH_FRINFC_NDEFMAP_MFUL_VAL4)
        {
            NdefMap->SendRecvBuf[(NdefMap->TLVStruct.NdefTLVByte + 
                            PH_FRINFC_NDEFMAP_MFUL_VAL2 )] = 0x00;

            NdefMap->NumOfLReminWrite = 1;

        }
        else
        {
            NdefMap->NumOfLReminWrite = 2;
        }
        NdefMap->NumOfBytesWritten = 0; 
    }
    else
    {
        /* Write the length value = 0 */
        NdefMap->SendRecvBuf[(NdefMap->TLVStruct.NdefTLVByte + 
                                PH_FRINFC_NDEFMAP_MFUL_VAL1)] = 
                                PH_FRINFC_NDEFMAP_MFUL_NDEFTLV_L;

        if((NdefMap->TLVStruct.NdefTLVByte + PH_FRINFC_NDEFMAP_MFUL_VAL1) < 
            PH_FRINFC_NDEFMAP_MFUL_VAL4)
        {
            /* Only one byte  */
            (void)memcpy(&(NdefMap->SendRecvBuf[ 
                    PH_FRINFC_NDEFMAP_MFUL_VAL4]),
                    &(NdefMap->ApduBuffer[ 
                    NdefMap->ApduBuffIndex]),
                    PH_FRINFC_NDEFMAP_MFUL_VAL1);
            /* Number of bytes written to the card from user buffer */
            NdefMap->NumOfBytesWritten = PH_FRINFC_NDEFMAP_MFUL_VAL1;
        }
    }

    (void)memcpy(NdefMap->MifareULContainer.Buffer,
                &(NdefMap->SendRecvBuf[ 
                PH_FRINFC_NDEFMAP_MFUL_VAL1]),
                PH_FRINFC_NDEFMAP_MFUL_VAL4);

    /* Copy the Ndef TLV buffer to send buffer */
    (void)memcpy(NdefMap->TLVStruct.NdefTLVBuffer,
                NdefMap->MifareULContainer.Buffer,
                PH_FRINFC_NDEFMAP_MFUL_BYTE4);

    /* Exact number of bytes written in the card including TLV */
    *NdefMap->DataCount = PH_FRINFC_NDEFMAP_MFUL_VAL4;

    /* Change the state to check ndef compliancy */
    NdefMap->State = PH_FRINFC_NDEFMAP_MFUL_STATE_WRITE;

    Result = phFriNfc_MfUL_H_Wr4bytes(NdefMap);
    
    return Result;
}

static NFCSTATUS phFriNfc_MfUL_H_ProWrittenBytes(phFriNfc_NdefMap_t *NdefMap)
{
    uint16_t localCurrentBlock;
    NFCSTATUS   Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                        NFCSTATUS_INVALID_REMOTE_DEVICE);
    
    if(NdefMap->ApduBuffIndex < NdefMap->ApduBufferSize)
    {
        NdefMap->ApduBuffIndex =  NdefMap->ApduBuffIndex+NdefMap->NumOfBytesWritten;
        if(*NdefMap->DataCount < 
            PH_FRINFC_NDEFMAP_MFUL_VAL4)
        {
            (void)memcpy(NdefMap->MifareULContainer.InternalBuf,
                        NdefMap->MifareULContainer.Buffer,
                        *NdefMap->DataCount);

            NdefMap->MifareULContainer.InternalLength = *NdefMap->DataCount;
        }
        else
        {
            NdefMap->MifareULContainer.InternalLength = 
                                PH_FRINFC_NDEFMAP_MFUL_VAL0;
        }

        NdefMap->MifareULContainer.RemainingSize=  NdefMap->MifareULContainer.RemainingSize-
                                            NdefMap->NumOfBytesWritten;
        if((NdefMap->ApduBuffIndex == NdefMap->ApduBufferSize) || 
            (NdefMap->MifareULContainer.RemainingSize == 
            PH_FRINFC_NDEFMAP_MFUL_VAL0))
        {
            Result = NFCSTATUS_SUCCESS;
            NdefMap->MifareULContainer.ReadWriteCompleteFlag = 
                (uint8_t)((NdefMap->MifareULContainer.RemainingSize == 
                PH_FRINFC_NDEFMAP_MFUL_VAL0)?
                PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                PH_FRINFC_NDEFMAP_MFUL_FLAG0);

                NdefMap->TLVStruct.SetTermTLVFlag = 
                (uint8_t)(((NdefMap->MifareULContainer.RemainingSize == 
                PH_FRINFC_NDEFMAP_MFUL_VAL0) || 
                (NdefMap->TLVStruct.SetTermTLVFlag == 
                PH_FRINFC_NDEFMAP_MFUL_FLAG1))?
                PH_FRINFC_NDEFMAP_MFUL_FLAG1:
                PH_FRINFC_NDEFMAP_MFUL_FLAG0);

                NdefMap->MifareULContainer.CurrentBlock = NdefMap->MifareULContainer.CurrentBlock+
                    (uint8_t)((NdefMap->MifareULContainer.InternalLength != 
                    PH_FRINFC_NDEFMAP_MFUL_VAL0)?
                    PH_FRINFC_NDEFMAP_MFUL_VAL0:
                    PH_FRINFC_NDEFMAP_MFUL_VAL1);

            *NdefMap->WrNdefPacketLength = NdefMap->ApduBuffIndex;
        }
        else
        {
            localCurrentBlock = NdefMap->MifareULContainer.CurrentBlock + 1;
            if (localCurrentBlock < 256)
            {
                NdefMap->MifareULContainer.CurrentBlock++;
                Result = phFriNfc_MfUL_H_fillSendBufToWr(NdefMap);
            }
            else
            {
                /* Go to next sector */
                NdefMap->MifareULContainer.CurrentSector++;

                Result = phFriNfc_MfUL_H_SelectSector(NdefMap, 
                    NdefMap->MifareULContainer.CurrentSector, 1, 
                    PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_WRITE_1);
            }
        }
    }

    if((Result == NFCSTATUS_SUCCESS) && 
        (NdefMap->TLVStruct.SetTermTLVFlag != 
        PH_FRINFC_NDEFMAP_MFUL_FLAG1) && 
        (NdefMap->MifareULContainer.RemainingSize > 
        PH_FRINFC_NDEFMAP_MFUL_VAL0))
    {
        Result = phFriNfc_MfUL_H_WrTermTLV(NdefMap);
    }
    else 
    {
        if((Result == NFCSTATUS_SUCCESS) && 
            (NdefMap->TLVStruct.SetTermTLVFlag == 
            PH_FRINFC_NDEFMAP_MFUL_FLAG1))
        {
            Result = phFriNfc_MfUL_H_UpdateWrLen(NdefMap);
        }
    }
    return Result;
}
static NFCSTATUS phFriNfc_MfUL_H_fillSendBufToWr(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint16_t    RemainingBytes = PH_FRINFC_NDEFMAP_MFUL_VAL0,
                BytesToWrite = PH_FRINFC_NDEFMAP_MFUL_VAL0;
    uint8_t     index = PH_FRINFC_NDEFMAP_MFUL_VAL0;
    
    RemainingBytes = (uint16_t)(( (NdefMap->ApduBufferSize - NdefMap->ApduBuffIndex) <= 
                        NdefMap->MifareULContainer.RemainingSize)?
                        (uint16_t)(NdefMap->ApduBufferSize - 
                        NdefMap->ApduBuffIndex):
                        NdefMap->MifareULContainer.RemainingSize);

    NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL0] = 
                        NdefMap->MifareULContainer.CurrentBlock;
    
    /* Get the number of bytes that can be written after copying 
        the internal buffer */
    BytesToWrite = ((RemainingBytes < 
                    (PH_FRINFC_NDEFMAP_MFUL_BYTE4 - 
                    NdefMap->MifareULContainer.InternalLength))? 
                    RemainingBytes:
                    (PH_FRINFC_NDEFMAP_MFUL_BYTE4 - 
                    NdefMap->MifareULContainer.InternalLength));

    if (NdefMap->NumOfBytesWritten == 0 && NdefMap->NumOfLReminWrite > 0)
    {
        BytesToWrite = BytesToWrite - NdefMap->NumOfLReminWrite;

        if (NdefMap->NumOfLReminWrite == 1)
        {
            NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL1] = 0x00; 
        }
        else
        {
            NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL1] = 0x00;
            NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL2] = 0x00;
        }
    }   

    if(NdefMap->MifareULContainer.InternalLength > 
        PH_FRINFC_NDEFMAP_MFUL_VAL0)
    {
        /* copy the internal buffer to the send buffer */
        (void)memcpy(&(NdefMap->SendRecvBuf[ 
                    PH_FRINFC_NDEFMAP_MFUL_VAL1]),
                    NdefMap->MifareULContainer.InternalBuf,
                    NdefMap->MifareULContainer.InternalLength);

    }

    /* Copy Bytes to write in the send buffer */
    (void)memcpy(&(NdefMap->SendRecvBuf[ 
                (PH_FRINFC_NDEFMAP_MFUL_VAL1 + 
                NdefMap->MifareULContainer.InternalLength) + 
                NdefMap->NumOfLReminWrite]),
                &(NdefMap->ApduBuffer[NdefMap->ApduBuffIndex]),
                BytesToWrite);
    
    /* update number of bytes written from the user buffer */
    NdefMap->NumOfBytesWritten = BytesToWrite;
    
    /* check the exact number of bytes written to a block including the 
        internal length */
    *NdefMap->DataCount = 
            (BytesToWrite + NdefMap->MifareULContainer.InternalLength +
                    NdefMap->NumOfLReminWrite);


    /* if total bytes to write in the card is less than 4 bytes then 
    pad zeroes till 4 bytes */
    if((BytesToWrite + NdefMap->MifareULContainer.InternalLength + 
        NdefMap->NumOfLReminWrite) 
            < PH_FRINFC_NDEFMAP_MFUL_BYTE4)
    {
        for(index = (uint8_t)((BytesToWrite + NdefMap->MifareULContainer.InternalLength + NdefMap->NumOfLReminWrite) + 
                    PH_FRINFC_NDEFMAP_MFUL_VAL1); 
            index < PH_FRINFC_NDEFMAP_MFUL_WR_A_BLK; 
            index++)
            {
                NdefMap->SendRecvBuf[index] = (uint8_t)((index == 
                                    ((BytesToWrite + 
                                    NdefMap->MifareULContainer.InternalLength + NdefMap->NumOfLReminWrite) + 
                                    PH_FRINFC_NDEFMAP_MFUL_VAL1))?
                                    PH_FRINFC_NDEFMAP_MFUL_TERMTLV:
                                    PH_FRINFC_NDEFMAP_MFUL_NULLTLV);

                NdefMap->TLVStruct.SetTermTLVFlag = PH_FRINFC_NDEFMAP_MFUL_FLAG1;
            }
    }

    /* A temporary buffer to hold four bytes of data that is 
        written to the card */
    (void)memcpy(NdefMap->MifareULContainer.Buffer,
                &(NdefMap->SendRecvBuf[ 
                PH_FRINFC_NDEFMAP_MFUL_VAL1]),
                PH_FRINFC_NDEFMAP_MFUL_BYTE4);

    
    
        if((NdefMap->TLVStruct.NdefTLVByte - PH_FRINFC_NDEFMAP_MFUL_VAL1) < 
        PH_FRINFC_NDEFMAP_MFUL_VAL3)
        {
            if ((NdefMap->TLVStruct.NdefTLVSector == 
                NdefMap->MifareULContainer.CurrentSector))
            {
                if(NdefMap->MifareULContainer.CurrentBlock == 
                        NdefMap->TLVStruct.NdefTLVBlock)
                {
                    (void)memcpy(NdefMap->TLVStruct.NdefTLVBuffer,
                        NdefMap->MifareULContainer.Buffer,
                        PH_FRINFC_NDEFMAP_MFUL_BYTE4);
                }                                   
            }
            
            if ((NdefMap->TLVStruct.NdefTLVSector == 
                NdefMap->MifareULContainer.CurrentSector) ||
                (NdefMap->TLVStruct.NdefTLVBlock == 0xFF))
            {
                if(NdefMap->MifareULContainer.CurrentBlock == 
                    (NdefMap->TLVStruct.NdefTLVBlock + PH_FRINFC_NDEFMAP_MFUL_VAL1))
                {
                    (void)memcpy(NdefMap->TLVStruct.NdefTLVBuffer1,
                    NdefMap->MifareULContainer.Buffer,
                    PH_FRINFC_NDEFMAP_MFUL_BYTE4);
                }
            }
        }
        else
        {
            if ((NdefMap->TLVStruct.NdefTLVSector == 
                NdefMap->MifareULContainer.CurrentSector))
            {
                if(NdefMap->MifareULContainer.CurrentBlock == 
                    (NdefMap->TLVStruct.NdefTLVBlock + 
                    PH_FRINFC_NDEFMAP_MFUL_VAL1))
                {
                    (void)memcpy(NdefMap->TLVStruct.NdefTLVBuffer,
                                NdefMap->MifareULContainer.Buffer,
                                PH_FRINFC_NDEFMAP_MFUL_BYTE4);
                }
            }
            
            if ((NdefMap->TLVStruct.NdefTLVSector == 
                NdefMap->MifareULContainer.CurrentSector)||
                (NdefMap->TLVStruct.NdefTLVBlock == 0xFF))
            {
                if(NdefMap->MifareULContainer.CurrentBlock == 
                    (NdefMap->TLVStruct.NdefTLVBlock + PH_FRINFC_NDEFMAP_MFUL_VAL2))
                {
                    (void)memcpy(NdefMap->TLVStruct.NdefTLVBuffer1,
                                NdefMap->MifareULContainer.Buffer,
                                PH_FRINFC_NDEFMAP_MFUL_BYTE4);
                }             
            }
        }
    

    /* Change the state to check ndef compliancy */
    NdefMap->State = PH_FRINFC_NDEFMAP_MFUL_STATE_WRITE;

    NdefMap->NumOfLReminWrite = 0;
    
    /* Start writing to the current block */
    Result = phFriNfc_MfUL_H_Wr4bytes(NdefMap);

    return Result; 
}
static NFCSTATUS phFriNfc_MfUL_H_WrTermTLV(phFriNfc_NdefMap_t   *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint8_t     index = PH_FRINFC_NDEFMAP_MFUL_VAL0,
                i = PH_FRINFC_NDEFMAP_MFUL_VAL0;

    /* Change the state to check ndef compliancy */
    NdefMap->State = PH_FRINFC_NDEFMAP_MFUL_STATE_TERM_TLV;
    
    NdefMap->SendRecvBuf[index] = 
                        (NdefMap->MifareULContainer.CurrentBlock + 
                        PH_FRINFC_NDEFMAP_MFUL_VAL0);
    index++;
    NdefMap->SendRecvBuf[index] = PH_FRINFC_NDEFMAP_MFUL_TERMTLV;
    index++;

    for(i = index; i < PH_FRINFC_NDEFMAP_MFUL_VAL4; i++)
    {
        NdefMap->SendRecvBuf[i] = PH_FRINFC_NDEFMAP_MFUL_NULLTLV;
    }
    
    Result = phFriNfc_MfUL_H_Wr4bytes(NdefMap);
    return Result;
}

static NFCSTATUS phFriNfc_MfUL_H_UpdateWrLen(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS   Result = NFCSTATUS_SUCCESS;
    uint16_t    BlockNo = PH_FRINFC_NDEFMAP_MFUL_VAL0,
                ByteNo = PH_FRINFC_NDEFMAP_MFUL_VAL0;

    if ((NdefMap->TLVStruct.NdefTLVSector == 
        NdefMap->MifareULContainer.CurrentSector) ||
        ((NdefMap->TLVStruct.NdefTLVBlock == 0xFF) &&
        (NdefMap->TLVStruct.NdefTLVByte == 4) &&
        (NdefMap->TLVStruct.NdefTLVSector == 0)))
    {
        BlockNo = (((NdefMap->TLVStruct.NdefTLVByte - 
                    PH_FRINFC_NDEFMAP_MFUL_VAL1) != 
                    PH_FRINFC_NDEFMAP_MFUL_VAL3)?
                    NdefMap->TLVStruct.NdefTLVBlock:
                    (NdefMap->TLVStruct.NdefTLVBlock + 
                    PH_FRINFC_NDEFMAP_MFUL_VAL1));

        ByteNo = (((NdefMap->TLVStruct.NdefTLVByte - 
                    PH_FRINFC_NDEFMAP_MFUL_VAL1) == 
                    PH_FRINFC_NDEFMAP_MFUL_VAL3)?
                    PH_FRINFC_NDEFMAP_MFUL_VAL1:
                    (NdefMap->TLVStruct.NdefTLVByte + 
                    PH_FRINFC_NDEFMAP_MFUL_VAL1));

        if (NdefMap->NumOfLReminWrite > 0)
        {
            BlockNo++;

            /* Copy the Ndef TLV buffer to send buffer */
            (void)memcpy(&(NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL1]),
                        NdefMap->TLVStruct.NdefTLVBuffer1,
                        PH_FRINFC_NDEFMAP_MFUL_BYTE4);

            if (NdefMap->NumOfLReminWrite == 1)
            {
                /* NdefMap->SendRecvBuf[1] = (uint8_t) ((NdefMap->ApduBuffIndex & 0xFF00) >> 8); */
				NdefMap->SendRecvBuf[1] = (uint8_t) NdefMap->ApduBuffIndex;
                
            }
            else if (NdefMap->NumOfLReminWrite == 2)
            {
				NdefMap->SendRecvBuf[1] = (uint8_t) ((NdefMap->ApduBuffIndex & 0xFF00) >> 8);
                NdefMap->SendRecvBuf[2]= (uint8_t) (NdefMap->ApduBuffIndex);
                
            }
            else
            {

            }
            NdefMap->NumOfLReminWrite = 0;
        }
        else 
        {
            /* Copy the Ndef TLV buffer to send buffer */
            (void)memcpy(&(NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL1]),
                        NdefMap->TLVStruct.NdefTLVBuffer,
                        PH_FRINFC_NDEFMAP_MFUL_BYTE4);


            if (NdefMap->ApduBuffIndex > 254)
            {
                ByteNo++;
                if  ((ByteNo == 3) || (ByteNo == 2))
                {
                    NdefMap->SendRecvBuf[ByteNo]= (uint8_t) ((NdefMap->ApduBuffIndex & 0xFF00) >> 8);
                    ByteNo++;
                    NdefMap->SendRecvBuf[ByteNo] = (uint8_t) (NdefMap->ApduBuffIndex);
                    ByteNo++;
                    NdefMap->NumOfLReminWrite = 0;
                }
                else if (ByteNo == 4)
                {
                    /* NdefMap->SendRecvBuf[ByteNo]= (uint8_t) (NdefMap->ApduBuffIndex); */
					NdefMap->SendRecvBuf[ByteNo]= (uint8_t) ((NdefMap->ApduBuffIndex & 0xFF00) >> 8);
                    ByteNo++;
                    NdefMap->NumOfLReminWrite = 1;
                }
                else
                {
                    NdefMap->NumOfLReminWrite = 2;
                }
            }
            else 
            {
                NdefMap->SendRecvBuf[ByteNo]= 
                                    (uint8_t)((NdefMap->Offset == 
                                    PH_FRINFC_NDEFMAP_SEEK_BEGIN)?
                                    (uint8_t)NdefMap->ApduBuffIndex:
                                    (NdefMap->ApduBuffIndex + 
                                    NdefMap->SendRecvBuf[ByteNo]));
            }
        }

        (void)memcpy(NdefMap->MifareULContainer.Buffer,
                    &(NdefMap->SendRecvBuf[ 
                    PH_FRINFC_NDEFMAP_MFUL_VAL1]),
                    PH_FRINFC_NDEFMAP_MFUL_BYTE4);
        
        NdefMap->SendRecvBuf[PH_FRINFC_NDEFMAP_MFUL_VAL0] = (uint8_t)BlockNo;
        Result = phFriNfc_MfUL_H_Wr4bytes(NdefMap);

        if (NdefMap->NumOfLReminWrite == 0)
        {
            NdefMap->State = PH_FRINFC_NDEFMAP_MFUL_STATE_WR_LEN_TLV;
        }
        else 
        {
            NdefMap->State = PH_FRINFC_NDEFMAP_MFUL_STATE_TERM_TLV;
        }
    }
    else if (NdefMap->TLVStruct.NdefTLVSector == 0)
    {
        /* Reset sector */
        NdefMap->MifareULContainer.CurrentSector = 0;
        NdefMap->PrevState = PH_FRINFC_NDEFMAP_MFUL_STATE_WRITE;
                                    
        Result = phFriNfc_MfUL_H_SelectSector(NdefMap, 
                NdefMap->MifareULContainer.CurrentSector, 1,
                PH_FRINFC_NDEFMAP_MFUL_STATE_SELECT_SECTOR_RESET_1);

    }
    else
    {
        Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_DEVICE_REQUEST);
    }
    
        
    return Result;
}
#ifdef UNIT_TEST
extern void phFriNfc_MifareUL_UnitTest(void *Context,uint32_t Length)
{
        uint8_t value=10;
        uint8_t* CrFlag=&value;
        phFriNfc_NdefMap_t *pNdefMap=(phFriNfc_NdefMap_t*)Context;
        phFriNfc_MfUL_H_UpdateLen(pNdefMap,(uint16_t) Length);
        phFriNfc_MfUL_H_WrTermTLV(pNdefMap);
        phFriNfc_MfUL_H_CallWrOp(pNdefMap);
        phFriNfc_MfUL_H_UpdateWrLen(pNdefMap);
        phFriNfc_MfUL_H_ChkRemainTLV(pNdefMap,CrFlag);
        phFriNfc_MfUL_H_ProWrittenBytes(pNdefMap);
        
        pNdefMap->PrevOperation=PH_FRINFC_NDEFMAP_READ_OPE;
        phFriNfc_MfUL_H_NxtOp(pNdefMap,CrFlag);
        
        pNdefMap->PrevOperation=PH_FRINFC_NDEFMAP_WRITE_OPE;
        phFriNfc_MfUL_H_NxtOp(pNdefMap,CrFlag);

        pNdefMap->TLVStruct.NoLbytesinTLV=PH_FRINFC_NDEFMAP_MFUL_VAL1;
        phFriNfc_MfUL_H_ChkRemainTLV(pNdefMap,CrFlag);

        pNdefMap->SendRecvBuf[0x00]= PH_FRINFC_NDEFMAP_MFUL_NULLTLV;
        phFriNfc_MfUL_H_findNDEFTLV(pNdefMap,CrFlag);
        

        pNdefMap->SendRecvBuf[0x00]= PH_FRINFC_NDEFMAP_MFUL_NULLTLV;
        phFriNfc_MfUL_H_findNDEFTLV(pNdefMap,CrFlag);

        
        phFriNfc_MfUL_H_RdBeforeWrite(pNdefMap);
        pNdefMap->TLVStruct.NdefTLVByte=1;
        phFriNfc_MfUL_H_RdBeforeWrite(pNdefMap);
        
        pNdefMap->TLVStruct.NdefTLVByte=3;
        phFriNfc_MfUL_H_RdBeforeWrite(pNdefMap);

        pNdefMap->TLVStruct.NdefTLVByte=4;
        phFriNfc_MfUL_H_RdBeforeWrite(pNdefMap);


        phFriNfc_MifareUL_H_Complete(pNdefMap,NFCSTATUS_SUCCESS);
        phFriNfc_MifareUL_H_Complete(NULL,NFCSTATUS_SUCCESS);
        phFriNfc_MifareUL_H_Complete(pNdefMap,NFCSTATUS_SUCCESS);
        pNdefMap->State=PH_FRINFC_NDEFMAP_MFUL_STATE_WRITE;
        phFriNfc_MifareUL_H_Complete(pNdefMap,NFCSTATUS_SUCCESS);

        phFriNfc_MifareUL_H_Complete(pNdefMap,NFCSTATUS_FAILED);
        phFriNfc_MifareUL_H_Complete(NULL,NFCSTATUS_SUCCESS);
        phFriNfc_MifareUL_H_Complete(pNdefMap,NFCSTATUS_FAILED);
        
        *pNdefMap->DataCount=0x3;
        phFriNfc_MfUL_H_ProWrittenBytes(pNdefMap);

        pNdefMap->ApduBuffIndex=0x31;
        phFriNfc_MfUL_H_ProWrittenBytes(pNdefMap);
            
        
                
}

#endif
#endif  /* PH_FRINFC_MAP_MIFAREUL_DISABLED */

