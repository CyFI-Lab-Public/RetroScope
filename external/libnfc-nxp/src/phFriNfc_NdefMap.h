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
 * \file  phFriNfc_NdefMap.h
 * \brief NFC Ndef Mapping For Different Smart Cards.
 *
 * Project: NFC-FRI
 *
 * $Date: Mon Dec 13 14:14:14 2010 $
 * $Author: ing02260 $
 * $Revision: 1.25 $
 * $Aliases:  $
 *
 */

#ifndef PHFRINFC_NDEFMAP_H
#define PHFRINFC_NDEFMAP_H


/*include files*/
#include <phNfcTypes.h>
#include <phNfcStatus.h>
#include <phFriNfc.h>
#ifdef PH_HAL4_ENABLE
    #include <phHal4Nfc.h>
#else
    #include <phHalNfc.h>
#endif


#include <phFriNfc_OvrHal.h>

#ifndef PH_FRINFC_EXCLUDE_FROM_TESTFW /* */

/**
 * \name NDEF Mapping
 *
 * File: \ref phFriNfc_NdefMap.h
 *
 */
/*@{*/
#define PH_FRINFC_NDEFMAP_FILEREVISION "$Revision: 1.25 $"  /**< \ingroup grp_file_attributes */
#define PH_FRINFC_NDEFMAP_FILEALIASES  "$Aliases:  $"       /**< \ingroup grp_file_attributes */
/*@}*/

#endif /* PH_FRINFC_EXCLUDE_FROM_TESTFW */


/** \defgroup grp_fri_nfc_ndef_map NDEF Mapping Component
 *
 *  This component implements the read/write/check NDEF functions for remote devices.
 *  NDEF data, as defined by the NFC Forum NDEF specification are written to or read from
 *  a remote device that can be a smart- or memory card. \n\n
 *  Please notice that the NDEF mapping command sequence must
 *  be \b contiguous (after correct initialisation): \n
 *  \b Examples:
 *  - Checking and Reading
 *      - \ref phFriNfc_NdefMap_ChkNdef
 *      - \ref phFriNfc_NdefMap_RdNdef
 *      .
 *  - Checking and Writing
 *      - \ref phFriNfc_NdefMap_ChkNdef
 *      - \ref phFriNfc_NdefMap_WrNdef
 *      .
 *  - Checking, Reading and Writing
 *      - \ref phFriNfc_NdefMap_ChkNdef
 *      - \ref phFriNfc_NdefMap_RdNdef
 *      - \ref phFriNfc_NdefMap_WrNdef
 *      .
 *  .
 * There must be \b no \b other FRI or HAL call between these mapping commands. Exceptions to this
 * rule are specific to the NDEF mapping of certain card / remote device types and separately noted,
 * typically for true multi-activation capable devices.
 *
 */

/**
 * \name NDEF Mapping - specifies the different card types
 * These are the only recognised card types in this version.
 *
 */
/*@{*/

#define DESFIRE_EV1

#define PH_FRINFC_NDEFMAP_MIFARE_UL_CARD                  1 /**< \internal Mifare UL */
#define PH_FRINFC_NDEFMAP_ISO14443_4A_CARD                2 /**< \internal Iso 14443-4A */
#define PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD              3 /**< \internal Mifare Standard */
#define PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD              4 /**< \internal Mifare Standard */
#define PH_FRINFC_NDEFMAP_FELICA_SMART_CARD               5 /**< \internal Felica Smart Tag */
#define PH_FRINFC_NDEFMAP_TOPAZ_CARD                      7 /**< \internal Felica Smart Tag */
#define PH_FRINFC_NDEFMAP_TOPAZ_DYNAMIC_CARD              8 /**< \internal Felica Smart Tag */
#ifdef DESFIRE_EV1
#define PH_FRINFC_NDEFMAP_ISO14443_4A_CARD_EV1            9 /**< \internal Iso 14443-4A EV1 */
#endif /* #ifdef DESFIRE_EV1 */

#define PH_FRINFC_NDEFMAP_ISO15693_CARD                   10 /**< \internal ISO 15693 */


#ifdef PH_NDEF_MIFARE_ULC
#define PH_FRINFC_NDEFMAP_MIFARE_ULC_CARD                  8 /**< \internal Mifare UL */
#endif /* #ifdef PH_NDEF_MIFARE_ULC */

#define PH_FRINFC_NDEFMAP_EMPTY_NDEF_MSG                  {0xD0, 0x00, 0x00}  /**< \internal Empty ndef message */


#ifdef PHFRINFC_OVRHAL_MOCKUP  /* */
#define PH_FRINFC_NDEFMAP_MOCKUP_CARD                     6 /**< \internal Mocup*/
#endif  /* PHFRINFC_OVRHAL_MOCKUP */



/* Enum reperesents the different card state*/
typedef enum
{
    PH_NDEFMAP_CARD_STATE_INITIALIZED,
    PH_NDEFMAP_CARD_STATE_READ_ONLY,
    PH_NDEFMAP_CARD_STATE_READ_WRITE,
    PH_NDEFMAP_CARD_STATE_INVALID
}phNDEF_CARD_STATE;


/*@}*/


#ifndef PH_FRINFC_MAP_MIFARESTD_DISABLED
/**
 * \name NDEF Mapping - specifies the Compliant Blocks in the Mifare 1k and 4k card types
 *
 */
/*@{*/
#define PH_FRINFC_NDEFMAP_MIFARESTD_1KNDEF_COMPBLOCK      45 /**< \internal Total Ndef Compliant blocks Mifare 1k */
#define PH_FRINFC_NDEFMAP_MIFARESTD_4KNDEF_COMPBLOCK      210 /**< \internal Total Ndef Compliant blocks Mifare 4k */
#define PH_FRINFC_NDEFMAP_MIFARESTD_RDWR_SIZE             16 /**< \internal Bytes read/write for one read/write operation*/
#define PH_FRINFC_NDEFMAP_MIFARESTD_TOTALNO_BLK           40 /**< \internal Total number of sectors in Mifare 4k */
#define PH_FRINFC_NDEFMAP_MIFARESTD_ST15_BYTES            15 /**< \internal To store 15 bytes after reading a block */
/*@}*/
#endif  /* PH_FRINFC_MAP_MIFARESTD_DISABLED */

#ifndef PH_FRINFC_MAP_TOPAZ_DISABLED
/**
 * \name NDEF Mapping - specifies the Compliant Blocks in the Mifare 1k and 4k card types
 *
 */
/*@{*/
#define PH_FRINFC_NDEFMAP_TOPAZ_MAX_SIZE                  256  /**< \internal Total Memory size = 96 bytes (newer version have mode) */
#define PH_FRINFC_NDEFMAP_TOPAZ_UID_SIZE                  0x04  /**< \internal UID size returned by READID command = 4 bytes */
/*@}*/
#endif  /* PH_FRINFC_MAP_TOPAZ_DISABLED */

#ifndef PH_FRINFC_MAP_FELICA_DISABLED
/* Felica Mapping - Constants */
#define PH_FRINFC_NDEFMAP_FELICA_BLOCK_SIZE               16
#define PH_FRINFC_NDEFMAP_FELICA_ATTR_NDEF_DATA_LEN        3
#define PH_FRINFC_NDEFMAP_FELICA_MANUF_ID_DATA_LEN         8
#endif  /* PH_FRINFC_MAP_FELICA_DISABLED */

/* MifareUL/Type2 specific constants*/
#ifndef PH_FRINFC_MAP_MIFAREUL_DISABLED

#ifdef PH_NDEF_MIFARE_ULC
#define PH_FRINFC_NDEFMAP_MFUL_64BYTES_BUF                2048 /**< \internal To store 2048 bytes after reading entire card */
#else
#define PH_FRINFC_NDEFMAP_MFUL_64BYTES_BUF                64 /**< \internal To store 64 bytes after reading entire card */
#endif /*#ifdef PH_NDEF_MIFARE_ULC */

#define PH_FRINFC_NDEFMAP_MFUL_4BYTES_BUF                 4  /**< \internal To store 4 bytes after write */

#endif /*#ifndef PH_FRINFC_MAP_MIFAREUL_DISABLED*/

#ifdef PHFRINFC_OVRHAL_MOCKUP  /* */

#define PH_FRINFC_NDEFMAP_MOCKUP_4096BYTES_BUF            4096  /**< \internal To store 4 bytes after write */

#endif /*#ifndef PH_FRINFC_MAP_MOCKUP_DISABLED*/

/**
 * \name Completion Routine Indices
 *
 * These are the indices of the completion routine pointers within the component context.
 * Completion routines belong to upper components.
 *
 */
/*@{*/
/** \ingroup grp_fri_nfc_ndef_map
 *  Completion Routine Index for \ref phFriNfc_NdefMap_ChkNdef */
#define PH_FRINFC_NDEFMAP_CR_CHK_NDEF       0  /* */
/** \ingroup grp_fri_nfc_ndef_map
 *  Completion Routine Index for \ref phFriNfc_NdefMap_RdNdef */
#define PH_FRINFC_NDEFMAP_CR_RD_NDEF        1  /* */
/** \ingroup grp_fri_nfc_ndef_map
 *  Completion Routine Index for \ref phFriNfc_NdefMap_WrNdef */
#define PH_FRINFC_NDEFMAP_CR_WR_NDEF        2  /* */
/** \ingroup grp_fri_nfc_ndef_map
 *  Completion Routine Index for \ref phFriNfc_NdefMap_EraseNdef */
#define PH_FRINFC_NDEFMAP_CR_ERASE_NDEF     3  /* */
/** \ingroup grp_fri_nfc_ndef_map Completion
 *  Routine Index for Unknown States/Operations */
#define PH_FRINFC_NDEFMAP_CR_INVALID_OPE    4  /* */
/** \ingroup grp_fri_nfc_ndef_map
 *  Number of completion routines that have to be initialised */
#define PH_FRINFC_NDEFMAP_CR                5  /* */
/*@}*/

/**
 * \name File Offset Attributes
 *
 * Following values are used to determine the offset value for Read/Write. This specifies whether
 * the Read/Write operation needs to be restarted/continued from the last offset set.
 *
 */
/*@{*/
/** \ingroup grp_fri_nfc_ndef_map
 *  Read/Write operation shall start from the last offset set */
#define PH_FRINFC_NDEFMAP_SEEK_CUR                          0 /* */
/** \ingroup grp_fri_nfc_ndef_map
 *  Read/Write operation shall start from the begining of the file/card */
#define PH_FRINFC_NDEFMAP_SEEK_BEGIN                        1 /* */
/*@}*/


/**
 * \name Buffer Size Definitions
 *
 */
/*@{*/
/** \ingroup grp_fri_nfc_ndef_map Minimum size of the TRX buffer required */
#define PH_FRINFC_NDEFMAP_MAX_SEND_RECV_BUF_SIZE            252 /* */
/** \internal The size of s MIFARE block */
#define PH_FRINFC_NDEFMAP_MF_READ_BLOCK_SIZE                16  /* */


#ifndef PH_FRINFC_EXCLUDE_FROM_TESTFW /* */


#ifndef PH_FRINFC_MAP_ISO15693_DISABLED

#define ISO15693_MAX_DATA_TO_STORE           0x04U

typedef struct phFriNfc_ISO15693Cont
{
    /**< \internal block number that is executed */
    uint16_t    current_block;
    /**< \internal The state of the operation */
    uint8_t     state;
    /**< \internal Completion routine index */
    uint8_t     cr_index;
    /**< \internal Execution sequence */
    uint8_t     ndef_seq;
    /**< \internal NDEF TLV size */
    uint16_t    actual_ndef_size;
    /**< \internal NDEF TLV size */
    uint16_t    max_data_size;
    /**< \internal NDEF TLV TYPE block number */
    uint16_t    ndef_tlv_type_blk;
    /**< \internal NDEF TLV TYPE byte number in the 
        "ndef_tlv_type_blk" */
    uint8_t     ndef_tlv_type_byte;
    /**< \internal Store the remaining bytes that can be used for 
    READ with continue option */
    uint8_t     store_read_data[ISO15693_MAX_DATA_TO_STORE];
    uint8_t     store_length;
    /**< \internal Remaining size that can be read */
    uint16_t    remaining_size_to_read;
    uint8_t     read_capabilities;


}phFriNfc_ISO15693Cont_t;

#endif /* #ifndef PH_FRINFC_MAP_ISO15693_DISABLED */



#ifndef PH_FRINFC_MAP_FELICA_DISABLED
/**
 *  \ingroup grp_fri_nfc_ndef_map
 *  \brief Felica Basic structure which details the different vaiables
 *         used for Reading/writing.
 *
 */
typedef struct phFriNfc_Felica
{
    /**< Current block being read or written*/
    uint8_t     CurBlockNo;

    /**< No. Of Written*/
    uint8_t     NoBlocksWritten;

    /**< Following are different variables used for write operation*/
    uint8_t     Wr_BytesRemained;   /* No of bytes to pad*/

    /**< Buffer to store odd number of block data */
    uint8_t     Wr_RemainedBytesBuff[PH_FRINFC_NDEFMAP_FELICA_BLOCK_SIZE];

    /**< Following are different variables used for read operation*/
    uint8_t     Rd_NoBytesToCopy;         /*specifies the extra number of read bytes */

    /**< stores extra read data bytes*/
    uint8_t     Rd_BytesToCopyBuff[PH_FRINFC_NDEFMAP_FELICA_BLOCK_SIZE];

    /**< Flag determines Intermediate Copy Operation*/
    uint8_t     IntermediateCpyFlag;

    /**< Stores Intermediate Copy data len*/
    uint8_t     IntermediateCpyLen;

    /**< Flag specifies Pad Byte Information*/
    uint8_t     PadByteFlag;

    /**< Flag specifies Intermediate WR Information*/
    uint8_t     IntermediateWrFlag;

    /**< Flag specifies Intermediate Rd Information*/
    uint8_t     IntermediateRdFlag;

    /**< Flag specifies Last Block Reached Information*/
    uint8_t     LastBlkReachedFlag;

    /**< Specifies how many bytes read from the card*/
    uint16_t        CurrBytesRead;

    /**< Flag specifies EOF card reached Information*/
    uint8_t     EofCardReachedFlag;

    /**< Flag specifies different Operation Types*/
    uint8_t     OpFlag;

    /**< Specifies Offset*/
    uint8_t     Offset;

    /**< Specifies TrxLen Information*/
    uint16_t    TrxLen;

}phFriNfc_Felica_t;

/**
 *  \ingroup grp_fri_nfc_ndef_map
 *  \brief Felica structure which details the different vaiables
 *         used to store the poll related information.
 *
 */
typedef struct phFriNfc_Felica_PollDetails
{
    phHal_sDevInputParam_t          *DevInputParam;
#ifndef PH_HAL4_ENABLE
    phHal_eOpModes_t                *OpMode;
#endif
    /**<   Temporary place holder to the Remote Device
                    Information, required to store the Felica
                    session opened information. */
    phHal_sRemoteDevInformation_t   psTempRemoteDevInfo;
}phFriNfc_Felica_PollDetails_t;

/**
 *  \ingroup grp_fri_nfc_ndef_map
 *  \brief Felica structure which details the attribute related information.
 *
 */
typedef struct phFriNfc_Felica_AttrInfo
{
    /** Version of the Ndefmap document*/
    uint8_t     Version;
    /** Nbr for check cmd*/
    uint8_t     Nbr;
    /** Nbw for update cmd*/
    uint8_t     Nbw;
    /** Maximum number of blocks to store Ndef data*/
    uint16_t    Nmaxb;
    /** Flag to indicate the status of the write operation*/
    uint8_t     WriteFlag;
    /** Flag to indicate the status of the read/write operation*/
    uint8_t     RdWrFlag;
    /** Represents the length of Ndef data : 3 bytes*/
    uint8_t     LenBytes[PH_FRINFC_NDEFMAP_FELICA_ATTR_NDEF_DATA_LEN];
    /** Specifies the ERASE NDEF Message Operation */
    uint8_t     EraseMsgFlag;

}phFriNfc_Felica_AttrInfo_t;

/**
 *  \ingroup grp_fri_nfc_ndef_map
 *  \brief Felica structure which details the different vaiables
 *         used to store the Card Manufacturer details.
 */
typedef struct phFriNfc_Felica_ManufDetails
{
    /** Manufacture identifier*/
    uint8_t     ManufID[PH_FRINFC_NDEFMAP_FELICA_MANUF_ID_DATA_LEN];
    /** Manufacture Parameters*/
    uint8_t     ManufParameter[PH_FRINFC_NDEFMAP_FELICA_MANUF_ID_DATA_LEN];
}phFriNfc_Felica_ManufDetails_t;
#endif  /* PH_FRINFC_MAP_FELICA_DISABLED */

#ifndef PH_FRINFC_MAP_MIFARESTD_DISABLED
typedef struct phFriNfc_MifareStdCont
{
    /** Device input parameter for poll and connect after failed authentication */
    phHal_sDevInputParam_t  *DevInputParam;
    /** to store bytes that will be used in the
        next write/read operation, if any */
    uint8_t             internalBuf[PH_FRINFC_NDEFMAP_MIFARESTD_ST15_BYTES];
    /** to Store the length of the internalBuf */
    uint16_t            internalLength;
    /** holds the block number which is presently been used */
    uint8_t             currentBlock;
    /** the number of Ndef Compliant blocks written/read */
    uint8_t             NdefBlocks;
    /** Total Number of Ndef Complaint Blocks */
    uint16_t            NoOfNdefCompBlocks;
    /** used in write ndef, to know that internal bytes
        are accessed */
    uint8_t             internalBufFlag;
    /** used in write ndef, to know that last 16 bytes
        are used to write*/
    uint8_t             RemainingBufFlag;
    /** indicates that Read has reached the end of the
        card */
    uint8_t             ReadWriteCompleteFlag;
    /** indicates that Read has reached the end of the
        card */
    uint8_t             ReadCompleteFlag;
    /** indicates that Write is possible or not */
    uint8_t             WriteFlag;
    /** indicates that Write is possible or not */
    uint8_t             ReadFlag;
    /** indicates that Write is possible or not */
    uint8_t             RdBeforeWrFlag;
    /** Authentication Flag indicating that a particular
        sector is authenticated or not */
    uint8_t             AuthDone;
    /** to store the last Sector ID in Check Ndef */
    uint8_t             SectorIndex;
    /** to read the access bits of each sector */
    uint8_t             ReadAcsBitFlag;
    /** Flag to check if Acs bit was written in this call */
    uint8_t             WriteAcsBitFlag;
    /** Buffer to store 16 bytes */
    uint8_t             Buffer[PH_FRINFC_NDEFMAP_MIFARESTD_RDWR_SIZE];
    /** to store the AIDs of Mifare 1k or 4k */
    uint8_t             aid[PH_FRINFC_NDEFMAP_MIFARESTD_TOTALNO_BLK];
    /** flag to write with offset begin */
    uint8_t             WrNdefFlag;
    /** flag to read with offset begin */
    uint8_t             ReadNdefFlag;
    /** flag to check with offset begin */
    uint8_t             ChkNdefFlag;
    /** To store the remaining size of the Mifare 1k or 4k card */
    uint16_t            remainingSize;
    /** To update the remaining size when writing to the Mifare 1k or 4k card */
    uint8_t             remSizeUpdFlag;
    /** The flag is to know that there is a different AID apart from
        NFC forum sector AID */
    uint16_t            aidCompleteFlag;
    /** The flag is to know that there is a a NFC forum sector exists
        in the card */
    uint16_t            NFCforumSectFlag;
    /** The flag is to know that the particular sector is a proprietary
        NFC forum sector */
    uint16_t            ProprforumSectFlag;
    /** The flag is set after reading the MAD sectors */
    uint16_t            ChkNdefCompleteFlag;
    /** Flag to store the current block */
    uint8_t             TempBlockNo;
    /** Completion routine index */
    uint8_t             CRIndex;
    /** Bytes remaining to write for one write procedure */
    uint16_t            WrLength;
    /** Flag to read after write */
    uint8_t             RdAfterWrFlag;
    /** Flag to say that poll is required before write ndef (authentication) */
    uint8_t             PollFlag;
    /** Flag is to know that this is first time the read has been called. This
    is required when read is called after write (especially for the card formatted
    with the 2nd configuration) */
    uint8_t             FirstReadFlag;
    /** Flag is to know that this is first time the write has been called. This
    is required when the card formatted with the 3rd configuration */
    uint8_t             FirstWriteFlag;
    /** Indicates the sector trailor id  for which the convert
        to read only is currently in progress*/
    uint8_t             ReadOnlySectorIndex;
    /** Indicates the total number of sectors on the card  */
    uint8_t             TotalNoSectors;
    /** Indicates the block number of the sector trailor on the card  */
    uint8_t             SectorTrailerBlockNo;
    /** Secret key B to given by the application */
    uint8_t             UserScrtKeyB[6];
}phFriNfc_MifareStdCont_t;
/*@}*/
#endif  /* PH_FRINFC_MAP_MIFARESTD_DISABLED */

#ifndef PH_FRINFC_MAP_DESFIRE_DISABLED
/**
 *  \ingroup grp_fri_nfc_ndef_map
 *  \brief Capability Container.
 *
 *  The Capability Container structure required for smart card operations.
 *
 */
typedef struct phFriNfc_DesfireCapCont
{
    uint16_t    DesfVersion; /**< \internal Desfire Version . */
    uint16_t    NdefMsgFid;  /**< \internal Ndef Message file pointer*/
    uint16_t    NdefFileSize; /**< \internal Holds Desfire File Size */
    uint8_t     ReadAccess;  /**< \internal Read Access Information. */
    uint8_t     WriteAccess; /**< \internal Write Access Information. */
    uint16_t    MaxRespSize; /**< \internal Maximum expected response size. */
    uint16_t    MaxCmdSize;  /**< \internal Maximum command size. */
    uint16_t    NdefDataLen;  /**< \internal Holds actual NDEF Data Len.*/
    uint8_t     IsNlenPresentFlag; /**< \internal specifies NLEN presence .*/
    uint8_t     SkipNlenBytesFlag; /**< \internal sets on presence of NLEN.*/
} phFriNfc_DesfireCapCont_t;
#endif  /* PH_FRINFC_MAP_DESFIRE_DISABLED */

#ifndef PH_FRINFC_MAP_MIFAREUL_DISABLED
/**
 *  \ingroup grp_fri_nfc_ndef_map
 *  \brief Mifare UL Basic structure which details the different vaiables
 *         used for Reading/writing.
 *
 */
typedef struct phFriNfc_MifareULCont
{
    /** to store bytes that will be used in the
        next write/read operation, if any */
    uint8_t             InternalBuf[PH_FRINFC_NDEFMAP_MFUL_4BYTES_BUF];
    /** to Store the length of the internalBuf */
    uint16_t            InternalLength;
    /** holds the sector number which is presently been used */
    uint8_t             CurrentSector;
    /** holds the block number which is presently been used */
    uint8_t             CurrentBlock;
    /** to know the completion routine */
    uint8_t             CRindex;
    /** This stores the free memory size left in the card */
    uint16_t            RemainingSize;
    /** Copy all the data(including non NDEF TLVs) from the card */
    uint8_t             ReadBuf[PH_FRINFC_NDEFMAP_MFUL_64BYTES_BUF];
    /** index of the above buffer */
    uint16_t            ReadBufIndex;
    /** This variable stores the index of the "ReadBuf" from which actual
        data has to be copied into the user buffer */
    uint16_t            ByteNumber;
    /** indicates that read/write has reached the end of the
        card */
    uint8_t             ReadWriteCompleteFlag;
    /** Buffer to store 4 bytes of data which is written to a block */
    uint8_t             Buffer[PH_FRINFC_NDEFMAP_MFUL_4BYTES_BUF];
}phFriNfc_MifareULCont_t;
#endif  /* PH_FRINFC_MAP_MIFAREUL_DISABLED */

#ifdef PHFRINFC_OVRHAL_MOCKUP  /* */
/**
 *  \ingroup grp_fri_nfc_ndef_map
 *  \brief Mifare UL Basic structure which details the different vaiables
 *         used for Reading/writing.
 *
 */
typedef struct phFriNfc_MockupCont
{
    /** to store bytes that will be used in the
        next write/read operation, if any */
    uint8_t             *NdefData;
    /** to Store the length of the internalBuf */
    uint32_t            NdefActualSize;
    /** to Store the length of the internalBuf */
    uint32_t            NdefMaxSize;
    /** to Store the length of the internalBuf */
    uint32_t            CardSize;
    /** holds the block number which is presently been used */
    uint32_t             CurrentBlock;
} phFriNfc_MockupCont_t;
#endif  /* PHFRINFC_OVRHAL_MOCKUP */

#endif /* PH_FRINFC_EXCLUDE_FROM_TESTFW */

/**
 *  \ingroup grp_fri_nfc_ndef_map
 *  \brief NDEF TLV structure which details the different vaiables
 *         used for TLV.
 *
 */
typedef struct phFriNfc_NDEFTLVCont
{
    /** Flag is to know that the TLV Type Found */
    uint8_t             NdefTLVFoundFlag;
    /** Sector number of the next/present available TLV */
    uint8_t             NdefTLVSector;
    /** Following two variables are used to store the
        T byte and the Block number in which the T is
        found in Tag */
    /** Byte number of the next/present available TLV */
    uint16_t            NdefTLVByte;
    /** Block number of the next/present available TLV */
    uint8_t             NdefTLVBlock;
    /** Authentication flag for NDEF TLV Block */
    uint8_t             NdefTLVAuthFlag;
    /** if the 16th byte of the last read is type (T) of TLV
        and next read contains length (L) bytes of TLV. This flag
        is set when the type (T) of TLV is found in the last read */
    uint8_t             TcheckedinTLVFlag;
    /** if the 16th byte of the last read is Length (L) of TLV
        and next read contains length (L) bytes of TLV. This flag
        is set when the Length (L) of TLV is found in the last read */
    uint8_t             LcheckedinTLVFlag;
    /** This flag is set, if Terminator TLV is already written
        and next read contains value (V) bytes of TLV. This flag
        is set when the value (V) of TLV is found in the last read */
    uint8_t             SetTermTLVFlag;
    /** To know the number of Length (L) field is present in the
        next block */
    uint8_t             NoLbytesinTLV;
    /** The value of 3 bytes length(L) field in TLV. In 3 bytes
        length field, 2 bytes are in one block and other 1 byte
        is in the next block. To store the former block length
        field value, this variable is used */
    uint16_t            prevLenByteValue;
    /** The value of length(L) field in TLV. */
    uint16_t            BytesRemainLinTLV;
    /** Actual size to read and write. This will be always equal to the
        length (L) of TLV as there is only one NDEF TLV . */
    uint16_t            ActualSize;
    /** Flag is to write the length (L) field of the TLV */
    uint8_t             WrLenFlag;
    /** Flag is to write the length (L) field of the TLV */
    uint16_t            NULLTLVCount;
    /** Buffer to store 4 bytes of data which is written to a block */
    uint8_t             NdefTLVBuffer[PH_FRINFC_NDEFMAP_MFUL_4BYTES_BUF];
    /** Buffer to store 4 bytes of data which is written to a next block */
    uint8_t             NdefTLVBuffer1[PH_FRINFC_NDEFMAP_MFUL_4BYTES_BUF];
}phFriNfc_NDEFTLVCont_t;

/**
 *  \ingroup grp_fri_nfc_ndef_map
 *  \brief Lock Control TLV structure which stores the Position,
 *         Size and PageCntrl details.
 */

typedef struct phFriNfc_LockCntrlTLVCont
{
    /** Specifies the Byte Position of the lock cntrl tlv
        in the card memory*/
    uint16_t             ByteAddr;

    /** Specifies the Size of the lock area in terms of
        bits/bytes*/
    uint16_t             Size;

    /** Specifies the Bytes per Page*/
    uint8_t             BytesPerPage;

    /** Specifies the BytesLockedPerLockBit */
    uint8_t             BytesLockedPerLockBit;

    /** Specifies the index of Lock cntrl TLV*/
    uint8_t             LockTlvBuffIdx;

    /** Store the content of Lock cntrl TLV*/
    uint8_t             LockTlvBuff[8];

    /** Specifies the Block number Lock cntrl TLV*/
    uint16_t             BlkNum;

    /** Specifies the Byte Number position of Lock cntrl TLV*/
    uint16_t             ByteNum;


}phFriNfc_LockCntrlTLVCont_t;


/**
 *  \ingroup grp_fri_nfc_ndef_map
 *  \brief Memeory Control TLV structure which stores the Position,
 *         Size and PageCntrl details of the reserved byte area.
 */

typedef struct phFriNfc_ResMemCntrlTLVCont
{
    /** Specifies the Byte Position of the lock cntrl tlv
        in the card memory*/
    uint16_t             ByteAddr;

    /** Specifies the Size of the lock area in terms of
        bits/bytes*/
    uint16_t             Size;

    /** Store the content of Memory cntrl TLV*/
    uint8_t             MemCntrlTlvBuff[8];

    /** Specifies the Bytes per Page*/
    uint8_t             BytesPerPage;

    /** Specifies the index of Mem cntrl TLV*/
    uint8_t             MemTlvBuffIdx;

    /** Specifies the Block number Lock cntrl TLV*/
    uint16_t             BlkNum;

    /** Specifies the Byte Number position of Lock cntrl TLV*/
    uint16_t             ByteNum;



}phFriNfc_ResMemCntrlTLVCont_t;

#if !(defined(PH_FRINFC_MAP_TOPAZ_DISABLED ) || defined (PH_FRINFC_MAP_TOPAZ_DYNAMIC_DISABLED ))

/**
 *  \ingroup grp_fri_nfc_ndef_map
 *  \brief Topaz container structure which details the different vaiables
 *         used for Topaz card mapping.
 *
 */
typedef struct phFriNfc_TopazCont
{
    /** This stores the free memory size left in the card. In case of topaz,
        this is updated only during check ndef */
    uint16_t            RemainingSize;
    /** Stores the current block number */
    uint8_t             CurrentBlock;
    /** Stores the current block number */
    uint8_t             ByteNumber;
    /** To know the completion routine call */
    uint8_t             CRIndex;

    uint8_t             ReadWriteCompleteFlag;
    /** This state is used for write */
    uint8_t             InternalState;
     /** This state is used for write */
    uint8_t             SkipLockBlkFlag;
    /** To store the UID */
    uint8_t             UID[PH_FRINFC_NDEFMAP_TOPAZ_UID_SIZE];
    /** To CC bytes length */
    uint8_t             CCByteBuf[4];
    /** Store the Buffer Index */
    uint16_t             Cur_RW_Index;

    /* No of bytes read or write*/
    uint16_t            ByteRWFrmCard;

    /* Cuurent Segment */
    uint8_t             CurrentSeg;

     /** Store the read bytes */
    uint8_t             ReadBuffer[PH_FRINFC_NDEFMAP_TOPAZ_MAX_SIZE];

    /** Size to know the exact data filled in the ReadBuffer. Useful, when the
        offset = PH_FRINFC_NDEFMAP_SEEK_CUR */
    uint8_t             ReadBufferSize;

    /** NDEF TLV byte address, This stores the byte address of
        TYPE field of the TLV */
    uint16_t            NdefTLVByteAddress;

    /** Expected sequence */
    uint8_t             ExpectedSeq;

    /** Write sequence */
    uint8_t             WriteSeq;

    /** Actual NDEF message size */
    uint16_t            ActualNDEFMsgSize;

    /** NDEF Read Write size in the card, this excludes lock and reserved bytes,
        mentioned in the LOCK and MEMORY control TLVs */
    uint16_t            NDEFRWSize;

    /** Remaining read size in the card, after reading the card.
        User has asked for the data less than " ActualNDEFMsgSize ",
        then remaining read bytes are stored in this variable.
        If the next read is with offset = PH_FRINFC_NDEFMAP_SEEK_CUR,
        then this variable is used.
    */
    uint16_t            RemainingReadSize;
#ifdef FRINFC_READONLY_NDEF
    uint8_t             read_only_seq;
    uint8_t             lock_bytes_written;
#endif /* #ifdef FRINFC_READONLY_NDEF */

}phFriNfc_TopazCont_t;

#endif /* PH_FRINFC_MAP_TOPAZ_DISABLED */
/**
 *  \ingroup grp_fri_nfc_ndef_map
 *  \brief NFC NDEF Mapping Component Context Structure
 *
 *  This structure is used to store the current context information of the instance.
 *
 */
typedef struct phFriNfc_NdefMap
{
    /**< \internal The state of the operation. */
    uint8_t                         State;

    /**< \internal Completion Routine Context. */
    phFriNfc_CplRt_t                CompletionRoutine[PH_FRINFC_NDEFMAP_CR];

    /**< \internal Pointer to the lower (HAL) instance. */
    void                            *LowerDevice;

    /**<\internal Holds the device additional informations*/
    phHal_sDepAdditionalInfo_t      psDepAdditionalInfo;

    /**<\internal Holds the completion routine informations of the Map Layer*/
    phFriNfc_CplRt_t                MapCompletionInfo;

    /**< \internal Pointer to the Remote Device Information */
    phHal_sRemoteDevInformation_t   *psRemoteDevInfo;

    /**<\internal Holds the Command Type(read/write)*/
    phHal_uCmdList_t               Cmd;

    /**< \internal Pointer to a temporary buffer. Could be
          used for read/write purposes */
    uint8_t                         *ApduBuffer;

    /**< \internal Size allocated to the ApduBuffer. */
    uint32_t                        ApduBufferSize;

    /**< \internal Index to the APDU Buffer. Used for internal calculations */
    uint16_t                        ApduBuffIndex;

    /**< \internal Pointer to the user-provided Data Size to be written trough WrNdef function. */
    uint32_t                        *WrNdefPacketLength;


    /**< \internal Holds the length of the received data. */
    uint16_t                        *SendRecvLength;

    /**<\internal Holds the ack of some intial commands*/
    uint8_t                         *SendRecvBuf;

    /**< \internal Holds the length of the data to be sent. */
    uint16_t                        SendLength;

    /**< \internal Data Byte Count, which gives the offset to the integration.*/
    uint16_t                        *DataCount;

    /**< \ internal Holds the previous operation on the card*/
    uint8_t                         PrevOperation;

    /**< \ internal Holds the previous state on the card*/
    uint8_t                         PrevState;

    /**< \internal Stores the type of the smart card. */
    uint8_t                         CardType;

     /**< \internal Stores the card state. */
    uint8_t                         CardState;

    /**< \internal Stores the memory size of the card */
    uint16_t                        CardMemSize;

    /**<\internal to Store the page offset on the mifare ul card*/
    uint8_t                         Offset;

    /** \internal specifies the desfire operation to be performed*/
    uint8_t                         DespOpFlag;

    /** \internal  Used to remeber how many bytes were written, to update
                   the dataCount and the BufferIndex */
    uint16_t                        NumOfBytesWritten;

    /**\internal used to remember number of L byte Remaining to be written */
    uint16_t                        NumOfLReminWrite;

    /** \internal  Pointer Used to remeber and return how many bytes were read,
                   to update the PacketDataLength in case of Read operation */
    /*  Fix for 0000238: [gk] MAP: Number of bytes actually read out is
        not returned. */
    uint32_t                        *NumOfBytesRead;

    /** \internal  Flag used to tell the process function that WRITE has
                   requested for an internal READ.*/
    uint8_t                         ReadingForWriteOperation;

    /** \internal  Buffer of 5 bytes used for the write operation for the
                   Mifare UL card.*/
    uint8_t                         BufferForWriteOp[5];

    /** \internal Temporary Receive Length to update the Receive Length
                  when every time the Overlapped HAL is called. */
    uint16_t                        TempReceiveLength;

    uint8_t                         NoOfDevices ;

    /** \internal stores operating mode type of the felica smart tag */
    /* phHal_eOpModes_t                OpModeType[2]; */

    /** \internal stores the type of the TLV found */
    uint8_t                         TLVFoundFlag;

    /** \internal stores the TLV structure related informations  */
    phFriNfc_NDEFTLVCont_t          TLVStruct;


    /** \internal stores the Lock Contrl Tlv related informations  */
    phFriNfc_LockCntrlTLVCont_t     LockTlv;

    /** \internal stores the Mem Contrl Tlv related informations  */
    phFriNfc_ResMemCntrlTLVCont_t   MemTlv;



    /** Capabilitity Containers: */
    #ifndef PH_FRINFC_EXCLUDE_FROM_TESTFW /* */
        /** \internal Desfire capability Container Structure. */
#ifndef PH_FRINFC_MAP_DESFIRE_DISABLED
        phFriNfc_DesfireCapCont_t       DesfireCapContainer;
#endif  /* PH_FRINFC_MAP_DESFIRE_DISABLED */

#ifndef PH_FRINFC_MAP_MIFARESTD_DISABLED
        /** \internal Pointer to the Mifare Standard capability Container Structure. */
        phFriNfc_MifareStdCont_t        StdMifareContainer;
#endif  /* PH_FRINFC_MAP_MIFARESTD_DISABLED */

#ifndef PH_FRINFC_MAP_FELICA_DISABLED
        /** \internal Following are the Felica Smart tag related strucutre & variables */
        phFriNfc_Felica_t               Felica;

        /** \internal Struture Stores the dev i/p , opmode informations of smart tag */
        phFriNfc_Felica_PollDetails_t   FelicaPollDetails;

        /** \internal Struture Stores the different attribute informations of smart tag */
        phFriNfc_Felica_AttrInfo_t      FelicaAttrInfo;

        /** \internal Struture Stores the PMm,IDm informations of smart tag */
        phFriNfc_Felica_ManufDetails_t  FelicaManufDetails;
#endif  /* PH_FRINFC_MAP_FELICA_DISABLED */
#ifndef PH_FRINFC_MAP_MIFAREUL_DISABLED
        /** \internal Mifare UL capability container structure. */
        phFriNfc_MifareULCont_t         MifareULContainer;
#endif  /* PH_FRINFC_MAP_MIFAREUL_DISABLED */
#ifndef PH_FRINFC_MAP_TOPAZ_DISABLED
        /** \internal Mifare UL capability container structure. */
        phFriNfc_TopazCont_t            TopazContainer;
#endif  /* PH_FRINFC_MAP_TOPAZ_DISABLED */

#ifndef PH_FRINFC_MAP_ISO15693_DISABLED
        phFriNfc_ISO15693Cont_t         ISO15693Container;
#endif /* #ifndef PH_FRINFC_MAP_ISO15693_DISABLED */

#ifdef PHFRINFC_OVRHAL_MOCKUP
        phFriNfc_MockupCont_t MochupContainer;
#endif  /* PHFRINFC_OVRHAL_MOCKUP */

    #endif /* PH_FRINFC_EXCLUDE_FROM_TESTFW */

} phFriNfc_NdefMap_t;


#ifndef PH_FRINFC_EXCLUDE_FROM_TESTFW /* */

/**
 * \ingroup grp_fri_nfc_ndef_map
 *
 * \brief Ndef Mapping \b Reset function
 *
 * \copydoc page_reg Resets the component instance to the initial state and initialises the
 *          internal variables.
 *
 * \param[in] NdefMap is a Pointer to a valid and initialised or uninitialised instance
 *            of \ref phFriNfc_NdefMap_t .
 * \param[in] LowerDevice Overlapped HAL reference, pointing at a valid instance of this
 *            underlying component.
 * \param[in] psRemoteDevInfo Points to the Remote Device Information structure encapsulating
 *                            the information about the device (Smart card, NFC device) to access.
 * \param[in] psDevInputParam The Device input parameter, as used for the HAL POLL function.
 *                            This parameter is needed by the component in special cases, when an internal call
 *                            to POLL is required again, such as for FeliCa. The storage of the structure behind
 *                            the pointer must be retained by the calling software. The component itself only
 *                            keeps the reference. No change is applied to the structure's content.
 * \param[in] TrxBuffer Pointer to an internally used buffer. The buffer has to be allocated by
 *                      the integrating software (not done by the component). The purpose of
 *                      this storage is to serve as an intermediate buffer for data frame
 *                      composition and analysis.
 *                      The size shall be at least \ref PH_FRINFC_NDEFMAP_MAX_SEND_RECV_BUF_SIZE .
 * \param[in] TrxBufferSize The size of TrxBuffer:
 *            The size shall be at least \ref PH_FRINFC_NDEFMAP_MAX_SEND_RECV_BUF_SIZE .
 * \param[in] ReceiveBuffer Pointer to a buffer that the component uses internally use to
 *            store the data received from the lower component.
 *            The size shall be at least \ref PH_FRINFC_NDEFMAP_MAX_SEND_RECV_BUF_SIZE .
 * \param[in] ReceiveLength The size of ReceiveBuffer. This specifies the actual length
 *            of the data received from the lower component.
 *            The size shall be at least \ref PH_FRINFC_NDEFMAP_MAX_SEND_RECV_BUF_SIZE .
 * \param[in] DataCount Specifies the offset count during read/write operations. This can be
 *            used by the integrating software to know about the total number of bytes read/written
 *            from/to the card. The caller shall set the value behind the pointer to zero
 *            before calling this function.
 *
 * \retval NFCSTATUS_SUCCESS                Operation successful.
 * \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
 *
 * \note  The DataCount variable is internally updated by the module and must not be changed by the
 *        embedding software.
 * \note  This function has to be called at the beginning, after creating an instance of
 *        \ref phFriNfc_NdefMap_t .  Use this function to reset the instance and/or to switch
 *        to a different underlying device (different NFC device or device mode).
 */
NFCSTATUS phFriNfc_NdefMap_Reset(phFriNfc_NdefMap_t              *NdefMap,
                                 void                            *LowerDevice,
                                 phHal_sRemoteDevInformation_t   *psRemoteDevInfo,
                                 phHal_sDevInputParam_t          *psDevInputParam,
                                 uint8_t                         *TrxBuffer,
                                 uint16_t                         TrxBufferSize,
                                 uint8_t                         *ReceiveBuffer,
                                 uint16_t                        *ReceiveLength,
                                 uint16_t                        *DataCount);


/**
 * \ingroup grp_fri_nfc_ndef_map
 *
 * \brief Ndef Mapping \b Set \b Completion \b Routine function
 *
 * \copydoc page_reg Setting of the Completion Routine.
 *
 * This function sets the Completion Routine for the specified function ID:\n
 * The completion routine is a function of an upper layer in the stack that needs to be notified
 * when the current instance has completed an I/O operation and data and/or an I/O status value
 * is available. The list of valid function IDs can be found under the section
 * "Completion Routine Indices", like e.g. \ref PH_FRINFC_NDEFMAP_CR_CHK_NDEF.
 *
 * \param[in] NdefMap Pointer to a valid instance of the \ref phFriNfc_NdefMap_t structure
 *                    serving as the component context.
 * \param[in] FunctionID ID of the component API function to set a with a completion routine for.
 *                       A valid routine has to be assigned for each function ID.
 *                       Use the "Completion Routine Indices", such as \ref PH_FRINFC_NDEFMAP_CR_CHK_NDEF .
 * \param[in] CompletionRoutine Pointer to a completion routine (part of a component of the upper layer)
 *                              to be called when the non-blocking opertaion has finished.
 * \param[in] CompletionRoutineContext Pointer to the context of the (upper) component where the
 *                                     particular completion routine is located.
 *
 * \retval NFCSTATUS_SUCCESS                Operation successful.
 * \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
 *
 * \note  This function has to be called after \ref phFriNfc_NdefMap_Reset .
 */
NFCSTATUS phFriNfc_NdefMap_SetCompletionRoutine(phFriNfc_NdefMap_t  *NdefMap,
                                                uint8_t              FunctionID,
                                                pphFriNfc_Cr_t       CompletionRoutine,
                                                void                *CompletionRoutineContext);


/**
 * \ingroup grp_fri_nfc_ndef_map
 *
 * \brief Ndef Mapping \b Read \b Ndef function
 *
 * \copydoc page_ovr Initiates Reading of NDEF information from the Remote Device.
 *
 * The function initiates the reading of NDEF information from a Remote Device.
 * It performs a reset of the state and restarts the state machine.
 *
 * \param[in] NdefMap Pointer to a valid instance of the \ref phFriNfc_NdefMap_t
 *                    component context structure.
 * \param[in,out] PacketData  Pointer to a location that shall receive the NDEF Packet.
 * \param[in,out] PacketDataLength Pointer to a variable that shall receive the length of the NDEF packet.
 *                                 The caller has to provide the maximum length, the function fills
 *                                 in the actual number of bytes received.
 * \param[in] Offset Indicates whether the read operation shall start from the begining of the
 *            file/card storage \b or continue from the last offset. The last Offset set is stored
 *            within a context (Data Count) variable (must not be modified by the integration).
 *            If the caller sets the value to \ref PH_FRINFC_NDEFMAP_SEEK_CUR, the component shall
 *            start reading from the last offset set (continue where it has stopped before).
 *            If set to \ref PH_FRINFC_NDEFMAP_SEEK_BEGIN, the component shall start reading
 *            from the begining of the card (restarted)
 *
 * \retval NFCSTATUS_PENDING               The action has been successfully triggered.
 * \retval NFCSTATUS_SUCCESS               Operation Successful.
 *
 * \retval NFCSTATUS_INVALID_PARAMETER     At least one parameter of the function is invalid.
 * \retval NFCSTATUS_INVALID_REMOTE_DEVICE Card Type is unsupported.
 * \retval NFCSTATUS_EOF_CARD_REACHED      No Space in the File to read.
 * \retval NFCSTATUS_INVALID_DEVICE        The device has not been opened or has been disconnected
 *                                         meanwhile.
 * \retval NFCSTATUS_CMD_ABORTED           The caller/driver has aborted the request.
 * \retval NFCSTATUS_BUFFER_TOO_SMALL      The buffer provided by the caller is too small.
 * \retval NFCSTATUS_RF_TIMEOUT            No data has been received within the TIMEOUT period.
 *
 */
NFCSTATUS phFriNfc_NdefMap_RdNdef(phFriNfc_NdefMap_t      *NdefMap,
                                  uint8_t                 *PacketData,
                                  uint32_t                *PacketDataLength,
                                  uint8_t                  Offset);


/**
 * \ingroup grp_fri_nfc_ndef_map
 *
 * \brief Ndef Mapping \b Check \b Ndef function
 *
 * \copydoc page_ovr Initiates Writing of NDEF information to the Remote Device.
 *
 * The function initiates the writing of NDEF information to a Remote Device.
 * It performs a reset of the state and starts the action (state machine).
 * A periodic call of the \ref phFriNfc_NdefMap_Process has to be done once the action
 * has been triggered.
 *
 * \param[in] NdefMap Pointer to a valid instance of the \ref phFriNfc_NdefMap_t
 *                    component context structure.
 * \param[in] PacketData  Pointer to a location that holds the prepared NDEF Packet.
 * \param[in,out] PacketDataLength Pointer to a variable that shall specify the length of the prepared NDEF packet.
 *                                 The caller has to provide the length, the function fills
 *                                 in the actual number of bytes received.
 * \param[in] Offset Indicates whether the write operation shall start from the begining of the
 *            file/card storage \b or continue from the last offset. The last Offset set is stored
 *            within a context (Data Count) variable (must not be modified by the integration).
 *            If the caller sets the value to \ref PH_FRINFC_NDEFMAP_SEEK_CUR, the component shall
 *            start writing from the last offset set (continue where it has stopped before).
 *            If set to \ref PH_FRINFC_NDEFMAP_SEEK_BEGIN, the component shall start writing
 *            from the begining of the card (restarted)
 *
 * \retval NFCSTATUS_PENDING               The action has been successfully triggered.
 * \retval NFCSTATUS_SUCCESS               Operation Successful.
 *
 * \retval NFCSTATUS_INVALID_PARAMETER     At least one parameter of the function is invalid.
 * \retval NFCSTATUS_INVALID_REMOTE_DEVICE Card Type is unsupported.
 * \retval NFCSTATUS_EOF_CARD_REACHED      No Space in the File to write.
 * \retval NFCSTATUS_INVALID_DEVICE        The device has not been opened or has been disconnected
 *                                         meanwhile.
 * \retval NFCSTATUS_CMD_ABORTED           The caller/driver has aborted the request.
 * \retval NFCSTATUS_BUFFER_TOO_SMALL      The buffer provided by the caller is too small.
 * \retval NFCSTATUS_RF_TIMEOUT            No data has been received within the TIMEOUT period.
 *
 */

extern NFCSTATUS phFriNfc_NdefMap_WrNdef(phFriNfc_NdefMap_t  *NdefMap,
                                  uint8_t             *PacketData,
                                  uint32_t            *PacketDataLength,
                                  uint8_t              Offset);


/**
 * \ingroup grp_fri_nfc_ndef_map
 *
 * \brief Ndef Mapping \b Check \b NDEF function
 *
 * \copydoc page_ovr Check whether a particular Remote Device is NDEF compliant.
 *
 * \param[in] NdefMap Pointer to a valid instance of the \ref phFriNfc_NdefMap_t
 *                    component context structure.
 *
 * \retval NFCSTATUS_PENDING               The action has been successfully triggered.
 * \retval NFCSTATUS_INVALID_PARAMETER     At least one parameter of the function is invalid.
 * \retval NFCSTATUS_INVALID_REMOTE_DEVICE Card Type is unsupported.
 * \retval NFCSTATUS_INVALID_PARAMETER     Completion Routine is NULL.
 * \retval NFCSTATUS_INVALID_REMOTE_DEVICE OpModes invalid.
 * \retval NFCSTATUS_INVALID_DEVICE        The device has not been opened or has been disconnected
 *                                         meanwhile.
 * \retval NFCSTATUS_CMD_ABORTED           The caller/driver has aborted the request.
 * \retval NFCSTATUS_BUFFER_TOO_SMALL      The buffer provided by the caller is too small.
 * \retval NFCSTATUS_RF_TIMEOUT            No data has been received within the TIMEOUT period.
 *
 */
NFCSTATUS phFriNfc_NdefMap_ChkNdef(phFriNfc_NdefMap_t *NdefMap);

#ifdef FRINFC_READONLY_NDEF
/*!
 * \ingroup grp_fri_smart_card_formatting
 *
 * \brief Initiates the conversion of the already NDEF formatted tag to READ ONLY.
 *
 * \copydoc page_ovr  The function initiates the conversion of the already NDEF formatted
 * tag to READ ONLY.After this formation, remote card would be properly Ndef Compliant and READ ONLY.
 * Depending upon the different card type, this function handles formatting procedure.
 * This function supports only for the TOPAZ tags.
 *
 * \param[in] NdefMap Pointer to a valid instance of the \ref phFriNfc_NdefMap_t structure describing
 *                    the component context.
 * \retval  NFCSTATUS_PENDING   The action has been successfully triggered.
 * \retval  Other values        An error has occurred.
 *
 */
NFCSTATUS
phFriNfc_NdefMap_ConvertToReadOnly (
    phFriNfc_NdefMap_t          *NdefMap);

#endif /* #ifdef FRINFC_READONLY_NDEF */

/**
 * \ingroup grp_fri_nfc_ndef_map
 *
 * \brief Ndef Mapping \b Erase \b NDEF function
 *
 * \copydoc page_ovr find the position of the existing NDEF TLV and overwrite with \b empty NDEF
 * message \b at that position.
 *
 * \param[in] NdefMap Pointer to a valid instance of the \ref phFriNfc_NdefMap_t
 *                    component context structure.
 *
 * \retval NFCSTATUS_PENDING               The action has been successfully triggered.
 * \retval NFCSTATUS_INVALID_PARAMETER     At least one parameter of the function is invalid.
 * \retval NFCSTATUS_INVALID_REMOTE_DEVICE Card Type is unsupported.
 * \retval NFCSTATUS_INVALID_PARAMETER     Completion Routine is NULL.
 * \retval NFCSTATUS_INVALID_REMOTE_DEVICE OpModes invalid.
 * \retval NFCSTATUS_INVALID_DEVICE        The device has not been opened or has been disconnected
 *                                         meanwhile.
 * \retval NFCSTATUS_CMD_ABORTED           The caller/driver has aborted the request.
 * \retval NFCSTATUS_BUFFER_TOO_SMALL      The buffer provided by the caller is too small.
 * \retval NFCSTATUS_RF_TIMEOUT            No data has been received within the TIMEOUT period.
 *
 */
NFCSTATUS phFriNfc_NdefMap_EraseNdef(phFriNfc_NdefMap_t *NdefMap);

/**
 * \ingroup grp_fri_nfc_ndef_map
 *
 * \brief Ndef Mapping \b Get Container size function
 *
 * \copydoc page_ovr Returns the size of the NDEF data that the card can hold to the caller.
 *
 * \param[in] NdefMap Pointer to a valid instance of the \ref phFriNfc_NdefMap_t
 *                    component context structure.
 *
 * \param[out] size Pointer to a uint32_t variable, which receives the size of the NDEF data
 *
 * \retval  NFCSTATUS_SUCCESS               The size has been successfully calculated.
 * \retval  NFCSTATUS_INVALID_PARAMETER     At least one parameter of the function is invalid.
 * \retval  NFCSTATUS_INVALID_REMOTE_DEVICE          Card Type is unsupported.
 *
 */

NFCSTATUS phFriNfc_NdefMap_GetContainerSize(const phFriNfc_NdefMap_t *NdefMap,uint32_t *maxSize, uint32_t *actualSize);

/**
 * \ingroup grp_fri_nfc_ndef_map
 *
 * \brief Ndef Mapping \b Completion \b Routine or \b Process function
 *
 * \copydoc page_cb Completion Routine: This function is called by the lower layer (OVR HAL)
 *                  when an I/O operation has finished. The internal state machine decides
 *                  whether to call into the lower device again or to complete the process
 *                  by calling into the upper layer's completion routine, stored within this
 *                  component's context (\ref phFriNfc_NdefMap_t).
 *
 * The function call scheme is according to \ref grp_interact. No State reset is performed during
 * operation.
 *
 * \param[in] Context The context of the current (not the lower/upper) instance, as set by the lower,
 *            calling layer, upon its completion.
 * \param[in] Status  The completion status of the lower layer (to be handled by the implementation of
 *                    the state machine of this function like a regular return value of an internally
 *                    called function).
 *
 * \note For general information about the completion routine interface please see \ref pphFriNfc_Cr_t . * The Different Status Values are as follows
 *
 */
void phFriNfc_NdefMap_Process(void        *Context,
                              NFCSTATUS   Status);



/**
 * \ingroup grp_fri_nfc_ndef_map
 *
 * \brief Ndef Mapping \b Check And Parse TLV Structure \b NDEF function
 *
 * \copydoc page_ovr Checks the presence of a valid TLV's(NDEF/Propritery).
 *
 * \param[in] NdefMap Pointer to a valid instance of the \ref phFriNfc_NdefMap_t
 *                    component context structure.
 *
 * \retval NFCSTATUS_INVALID_FORMAT         No valid TLV Found.
 * \retval NFCSTATUS_SUCCESS                Operation Successful.
 *
 */
NFCSTATUS phFriNfc_ChkAndParseTLV(phFriNfc_NdefMap_t    *NdefMap);


#ifdef PHFRINFC_OVRHAL_MOCKUP  /* */

/**
 * \ingroup grp_fri_nfc_ndef_map
 *
 * \brief Set data NDEF in mockup mode
 *
 * \param[in]       NdefMap          Pointer to a valid instance of the \ref phFriNfc_NdefMap_t component context structure.
 * \param[in]       NdefData         Pointer to card mockup data
 * \param[in]       NdefActualSize    The actual data length
 * \param[in]       NdefMaxSize       The max data length
 * \param[in]       NdefCardSize     The total card size
 *
 * \retval NFCSTATUS_SUCCESS          The operation is ok.
 *
 */
NFCSTATUS phFriNfc_NdefMap_MockupCardSetter(phFriNfc_NdefMap_t *NdefMap, uint8_t *NdefData, uint32_t NdefActualSize, uint32_t NdefMaxSize, uint32_t CardSize);
NFCSTATUS phFriNfc_NdefMap_MockupNDefModeEn(uint8_t  *pNdefCompliancy, uint8_t  *pCardType, uint8_t Enable);
#endif /*#ifndef PH_FRINFC_MAP_MOCKUP_DISABLED*/


/**
 * \internal
 * \name States of the FSM.
 *
 */
/*@{*/
#define PH_FRINFC_NDEFMAP_STATE_RESET_INIT                  0   /**< \internal Initial state */
#define PH_FRINFC_NDEFMAP_STATE_CR_REGISTERED               1   /**< \internal CR has been registered */
#define PH_FRINFC_NDEFMAP_STATE_EOF_CARD                    2   /**< \internal EOF card reached */
/*@}*/

/* Following values specify the previous operation on the card. This value is assigned to
   the context structure variable: PrevOperation. */

/**< Previous operation is check*/
#define PH_FRINFC_NDEFMAP_CHECK_OPE                         1
/**< Previous operation is read*/
#define PH_FRINFC_NDEFMAP_READ_OPE                          2
/**< Previous operation is write */
#define PH_FRINFC_NDEFMAP_WRITE_OPE                         3
/**< Previous operation is Actual size */
#define PH_FRINFC_NDEFMAP_GET_ACTSIZE_OPE                   4

/* This flag is set when there is a need of write operation on the odd positions
   ex: 35,5 etc. This is used with MfUlOp Flag */
#define PH_FRINFC_MFUL_INTERNAL_READ                        3  /**< \internal Read/Write control*/


#endif /* PH_FRINFC_EXCLUDE_FROM_TESTFW */

#endif /* PHFRINFC_NDEFMAP_H */
