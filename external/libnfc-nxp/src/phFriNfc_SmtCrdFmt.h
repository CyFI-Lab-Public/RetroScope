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
 * \file  phFriNfc_SmtCrdFmt.h
 * \brief NFC-FRI Smart Card Formatting.
 *
 * Project: NFC-FRI
 *
 * $Date: Mon Dec 13 14:14:11 2010 $
 * $Author: ing02260 $
 * $Revision: 1.5 $
 * $Aliases:  $
 *
 */

#ifndef PHFRINFC_SMTCRDFMT_H
#define PHFRINFC_SMTCRDFMT_H

/** 
 *  \name Smart Card Formatting
 *
 * File: \ref phFri_CardFormatFunctions.h
 *
 */
/*@{*/
#define PHFRINFC_SMTCRDFMT_FILEREVISION "$Revision: 1.5 $"
#define PHFRINFC_SMTCRDFMT_FILEALIASES  "$Aliases:  $"
/*@}*/

/*! \defgroup grp_fri_smart_card_formatting NFC FRI Smart Card Formatting
 *
 *  Smart Card Formatting functionality enables automatic formatting of any type of smart cards.
 *  This initializes the smart cards and makes them NDEF Compliant.
 *  Single API is provided to handle format/recovery management of different types cards.
 *  Following are different Types of cards supported by this module, currently.
 *      - Type1 ( Topaz)
 *      - Type2 ( Mifare UL)
 *      - Type4 ( Desfire)
 *      - Mifare Std.
 */
/*@{*/
/**
 *  \ingroup grp_fri_smart_card_formatting
 *  \brief Macro definitions.
 *  \note 
          On requirement basis, new constants will be defined
          during the implementation phase.
*/

#define DESFIRE_FMT_EV1


#define PH_FRI_NFC_SMTCRDFMT_NFCSTATUS_FORMAT_ERROR                 9
#define  PH_FRINFC_SMTCRDFMT_MSTD_DEFAULT_KEYA_OR_KEYB           {0xFF, 0xFF,0xFF,0xFF,0xFF,0xFF}
#define  PH_FRINFC_SMTCRDFMT_MSTD_MADSECT_KEYA                   {0xA0, 0xA1,0xA2,0xA3,0xA4,0xA5}
#define  PH_FRINFC_SMTCRDFMT_NFCFORUMSECT_KEYA                   {0xD3, 0xF7,0xD3,0xF7,0xD3,0xF7}
#define  PH_FRINFC_SMTCRDFMT_MSTD_MADSECT_ACCESSBITS             {0x78,0x77,0x88}
#define  PH_FRINFC_SMTCRDFMT_MSTD_NFCFORUM_ACCESSBITS            {0x7F,0x07,0x88}
#define  PH_FRINFC_SMTCRDFMT_MAX_TLV_TYPE_SUPPORTED              1

#define  PH_FRINFC_SMTCRDFMT_MAX_SEND_RECV_BUF_SIZE             252

#define  PH_FRINFC_SMTCRDFMT_STATE_RESET_INIT                   1

enum 
{
    PH_FRINFC_SMTCRDFMT_MIFARE_UL_CARD,
    PH_FRINFC_SMTCRDFMT_ISO14443_4A_CARD,           
    PH_FRINFC_SMTCRDFMT_MFSTD_1K_CRD,         
    PH_FRINFC_SMTCRDFMT_MFSTD_4K_CRD,         
    PH_FRINFC_SMTCRDFMT_TOPAZ_CARD                 
};

/**
 * \name Completion Routine Indices
 *
 * These are the indices of the completion routine pointers within the component context.
 * Completion routines belong to upper components.
 *
 */
/*@{*/
/** \ingroup grp_fri_nfc_ndef_map
*  Completion Routine Index for \ref phFriNfc_SmtCrd_Format */
#define PH_FRINFC_SMTCRDFMT_CR_FORMAT     0  /* */ 
/** \ingroup grp_fri_nfc_ndef_map Completion
 *  Routine Index for Unknown States/Operations */
#define PH_FRINFC_SMTCRDFMT_CR_INVALID_OPE    1  /* */  
/** \ingroup grp_fri_nfc_ndef_map
 *  Number of completion routines that have to be initialised */
#define  PH_FRINFC_SMTCRDFMT_CR               2
/*@}*/


/*@}*/

/*
 *  \ingroup grp_fri_smart_card_formatting
 *
 *  \brief NFC Smart Card Formatting Component Type1 Additional Information Structure
 *
 *  This structure is used to specify additional information required to format the Type1 card.
 *  \note 
 *           On requirement basis,structure will be filled/modified with other parameters
 *         during the implementation phase.
 *
 */
typedef struct phFriNfc_Type1_AddInfo
{
  /* Stores the CC byte values. For Ex: 0xE1, 0x10 , 0x0C, 0x00*/
  uint8_t CCBytes[5];
  uint8_t UID[4];
  uint8_t CCByteIndex;
            
} phFriNfc_Type1_AddInfo_t;

/*
 *
 *  \ingroup grp_fri_smart_card_formatting
 *  \brief NFC Smart Card Formatting Component Type2 Additional Information Structure
 *
 *  This structure is used to specify additional information required to format the Type2 card.
 *  \note 
 *           On requirement basis,structure will be filled/modified with other parametes
 *         during the implementation phase.
 *
 */
typedef struct phFriNfc_Type2_AddInfo
{
    /* Stores the CC byte values. For Ex: 0xE1, 0x10 , 0x10, 0x00*/
   uint8_t OTPBytes[4];
#ifdef FRINFC_READONLY_NDEF
   uint8_t  LockBytes[4];

#ifdef PH_NDEF_MIFARE_ULC
   uint8_t  ReadData[16];
   uint8_t  ReadDataIndex;
   uint8_t  DynLockBytes[4];
   uint8_t  BytesLockedPerLockBit;
   uint8_t  LockBytesPerPage;
   uint8_t  LockByteNumber;
   uint8_t  LockBlockNumber;
   uint8_t  NoOfLockBits;
   uint8_t  DefaultLockBytesFlag;
   uint8_t  LockBitsWritten;
#endif /* #ifdef PH_NDEF_MIFARE_ULC */

#endif /* #ifdef FRINFC_READONLY_NDEF */
   /* Current Block Address*/
   uint8_t CurrentBlock;
} phFriNfc_Type2_AddInfo_t;

/*
 *  \ingroup grp_fri_smart_card_formatting
 *  \brief NFC Smart Card Formatting Component Type4 Additional Information Structure
 *
 *  This structure is used to specify additional information required to format the type4 card.
 *  \note 
 *          On requirement basis,structure will be filled/modified with other parametes
 *         during the implementation phase.
 *
 */

typedef struct phFriNfc_Type4_AddInfo
{              
    /* Specifies Keys related to PICC/NFCForum Master Key settings*/
    /* Stores the PICC Master Key/NFC Forum MasterKey*/    
    uint8_t PICCMasterKey[16];
    uint8_t NFCForumMasterkey[16];

    /* To create the files follwoiing attributes are required*/
    uint8_t         PrevState;
    uint16_t        FileAccessRights;
    uint32_t        CardSize;
    uint16_t        MajorVersion;
    uint16_t        MinorVersion;

} phFriNfc_Type4_AddInfo_t;

/*
 *  \ingroup grp_fri_smart_card_formatting
 *  \brief NFC Smart Card Formatting Component Mifare Std Additional Information Structure
 *
 *  This structure is used to specify additional information required to format the Mifare Std card.
 *  \note 
 *         On requirement basis,structure will be filled/modified with other parametes
 *         during the implementation phase.
 *
 */
 typedef struct phFriNfc_MfStd_AddInfo
{
    /** Device input parameter for poll and connect after failed authentication */
    phHal_sDevInputParam_t  *DevInputParam;

    /* Stores the Default KeyA and KeyB values*/
    uint8_t     Default_KeyA_OR_B[6];

    /* Key A of MAD sector*/
    uint8_t     MADSect_KeyA[6];

    /* Key A of NFC Forum Sector sector*/
    uint8_t     NFCForumSect_KeyA[6];

    /* Access Bits of MAD sector*/
    uint8_t     MADSect_AccessBits[3];

    /* Access Bits of NFC Forum sector*/
    uint8_t     NFCForumSect_AccessBits[3];

    /* Secret key B to given by the application */
    uint8_t     ScrtKeyB[6];

    /* Specifies the status of the different authentication handled in 
    formatting procedure*/
    uint8_t     AuthState;

    /* Stores the current block */
    uint16_t    CurrentBlock;

    /* Stores the current block */
    uint8_t     NoOfDevices;

    /* Store the compliant sectors */
    uint8_t     SectCompl[40];

    /* Flag to know that MAD sector */
    uint8_t     WrMADBlkFlag;

    /* Fill the MAD sector blocks */
    uint8_t     MADSectBlk[80];

    /* Fill the MAD sector blocks */
    uint8_t     UpdMADBlk;
} phFriNfc_MfStd_AddInfo_t;


 /*
 *  \ingroup grp_fri_smart_card_formatting
 *  \brief NFC Smart Card Formatting Component ISO-15693 Additional Information Structure
 *
 *  This structure is used to specify additional information required to format the ISO-15693 card.
 *  \note 
 *         On requirement basis,structure will be filled/modified with other parametes
 *         during the implementation phase.
 *
 */
 typedef struct phFriNfc_ISO15693_AddInfo
 {
    /* Stores the current block executed */
    uint16_t        current_block;
    /* Sequence executed */
    uint8_t         format_seq;
    /* Maximum data size in the card */
    uint16_t        max_data_size;
 }phFriNfc_ISO15693_AddInfo_t;

/**
 *  \ingroup grp_fri_smart_card_formatting
 *
 *  \brief NFC Smart Card Formatting Component Additional Information Structure
 *
 *  This structure is composed to have additional information of different type of tags 
 *   Ex: Type1/Type2/Type4/Mifare 1k/4k 
 *
 *  \note 
 *          On requirement basis, structure will be filled/modified with other parameters
 *         during the implementation phase.
 */
typedef struct phFriNfc_sNdefSmtCrdFmt_AddInfo
{
   phFriNfc_Type1_AddInfo_t         Type1Info;
   phFriNfc_Type2_AddInfo_t         Type2Info;
   phFriNfc_Type4_AddInfo_t         Type4Info;
   phFriNfc_MfStd_AddInfo_t         MfStdInfo;
   phFriNfc_ISO15693_AddInfo_t      s_iso15693_info;

}phFriNfc_sNdefSmtCrdFmt_AddInfo_t;

/**
 *  \ingroup grp_fri_smart_card_formatting
 *  \brief NFC Smart Card Formatting Component Context Structure
 *
 *  This structure is used to store the current context information of the instance.
 *
 *  \note  On requirement basis,structure will be filled/modified with other parameters
 *            during the implementation phase 
 *
 */
typedef struct phFriNfc_sNdefSmtCrdFmt
{
     /** Pointer to the lower (HAL) instance.*/
    void                                *LowerDevice;
    
    /** Holds the device additional informations*/
    phHal_sDepAdditionalInfo_t          psDepAdditionalInfo;

    /** Pointer to the Remote Device Information */
    phHal_sRemoteDevInformation_t       *psRemoteDevInfo;
    
    /** Stores the type of the smart card. */
    uint8_t                             CardType;
    
    /** Stores operating mode type of the MifareStd. */
    /* phHal_eOpModes_t                    OpModeType[2]; */

     /**< \internal The state of the operation. */
    uint8_t                             State;        

    /**< \internal Stores the card state Ex: Blank/Formatted etc. */
    uint8_t                             CardState;    

     /**< \internal Completion Routine Context. */
    phFriNfc_CplRt_t                    CompletionRoutine[PH_FRINFC_SMTCRDFMT_CR]; 

     /**<\internal Holds the completion routine informations of the Smart Card Formatting Layer*/
    phFriNfc_CplRt_t                    SmtCrdFmtCompletionInfo;

     /**<\internal Holds the Command Type(read/write)*/
    phHal_uCmdList_t                    Cmd;

     /**< \internal Holds the length of the received data. */
    uint16_t                            *SendRecvLength;

    /**<\internal Holds the ack of some intial commands*/
    uint8_t                             *SendRecvBuf;

      /**< \internal Holds the length of the data to be sent. */
    uint16_t                            SendLength; 

    /**< \internal Stores the output/result of the format procedure. Ex: Formatted Successfully,
             Format Error etc */
    NFCSTATUS                           FmtProcStatus;    

    /** Stores Additional Information needed to format the different types of tags*/
    phFriNfc_sNdefSmtCrdFmt_AddInfo_t   AddInfo;

    /*  Stores NDEF message TLV*/
    /* This stores the different TLV messages for the different card types*/
    uint8_t   TLVMsg[PH_FRINFC_SMTCRDFMT_MAX_TLV_TYPE_SUPPORTED][8];

           
} phFriNfc_sNdefSmtCrdFmt_t;

/**
 * \ingroup grp_fri_smart_card_formatting
 * \brief Smart Card Formatting \b Reset function
 *
 * \copydoc page_reg Resets the component instance to the initial state and initializes the 
 *          internal variables.
 *
 * \param[in] NdefSmtCrdFmt is a Pointer to a valid and initialized or uninitialised instance
 *            of \ref phFriNfc_sNdefSmtCrdFmt_t .
 * \param[in] LowerDevice Overlapped HAL reference, pointing at a valid instance of this
 *            underlying component.
 * \param[in] psRemoteDevInfo Points to the Remote Device Information structure encapsulating
 *                            the information about the device (Smart card, NFC device) to access.
 * \param[in] psDevInputParam The Device input parameter, as used for the HAL POLL function.
 *                            This parameter is needed by the component in special cases, when an internal call
 *                            to POLL is required again, such as for FeliCa. The storage of the structure behind 
 *                            the pointer must be retained by the calling software. The component itself only
 *                            keeps the reference. No change is applied to the structure's content.
 * \param[in] ReceiveBuffer Pointer to a buffer that the component uses internally use to
 *            store the data received from the lower component.
 *            The size shall be at least \ref PH_FRINFC_SMTCRDFMT_MAX_SEND_RECV_BUF_SIZE .
 * \param[in] ReceiveLength The size of ReceiveBuffer. This specifies the actual length 
 *            of the data received from the lower component.
 *            The size shall be at least \ref PH_FRINFC_SMTCRDFMT_MAX_SEND_RECV_BUF_SIZE .
 * 
 * \retval NFCSTATUS_SUCCESS                Operation successful.
 * \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
 *
 * \note  This function has to be called at the beginning, after creating an instance of
 *        \ref phFriNfc_sNdefSmtCrdFmt_t .  Use this function to reset the instance and/or to switch
 *        to a different underlying card types.
 */
NFCSTATUS phFriNfc_NdefSmtCrd_Reset(phFriNfc_sNdefSmtCrdFmt_t       *NdefSmtCrdFmt,
                                    void                            *LowerDevice,
                                    phHal_sRemoteDevInformation_t   *psRemoteDevInfo,
                                    phHal_sDevInputParam_t          *psDevInputParam,
                                    uint8_t                         *SendRecvBuffer,
                                    uint16_t                        *SendRecvBuffLen);
                                    


/*!
 * \ingroup grp_fri_smart_card_formatting
 *
 * \brief Setting of the Completion Routine.
 *
 * \copydoc page_ovr This function allows the caller to set a Completion Routine (notifier).
 *
 * \param[in] NdefSmtCrdFmt Pointer to a valid instance of the \ref phFriNfc_sNdefSmtCrdFmt_t structure describing
 *                    the component context.
 *
 * \param CompletionRoutine Pointer to a valid completion routine being called when the non-blocking
 *        operation has finished.
 *
 * \param CompletionRoutineParam Pointer to a location with user-defined information that is submitted
 *                               to the Completion Routine once it is called.

 * \retval NFCSTATUS_SUCCESS                Operation successful.
 * \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
 *
 */
NFCSTATUS phFriNfc_NdefSmtCrd_SetCR(phFriNfc_sNdefSmtCrdFmt_t     *NdefSmtCrdFmt,
                                    uint8_t                       FunctionID,
                                    pphFriNfc_Cr_t                CompletionRoutine,
                                    void                          *CompletionRoutineContext);


/*!
 * \ingroup grp_fri_smart_card_formatting
 *
 * \brief Initiates the card formatting procedure for Remote Smart Card Type.
 *
 * \copydoc page_ovr  The function initiates and formats the Smart Card.After this formation, remote
 * card would be properly initialized and Ndef Compliant.
 * Depending upon the different card type, this function handles formatting procedure.
 * This function also handles the different recovery procedures for different types of the cards. For both
 * Format and Recovery Management same API is used.
 * 
 * \param[in] phFriNfc_sNdefSmtCrdFmt_t Pointer to a valid instance of the \ref phFriNfc_sNdefSmartCardFmt_t
 *                             structure describing the component context.
 * \retval  NFCSTATUS_PENDING   The action has been successfully triggered.
 * \retval  Other values        An error has occurred.
 *
 */
NFCSTATUS phFriNfc_NdefSmtCrd_Format(phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt, const uint8_t *ScrtKeyB);


#ifdef FRINFC_READONLY_NDEF
/*!
 * \ingroup grp_fri_smart_card_formatting
 *
 * \brief Initiates the conversion of the already NDEF formatted tag to READ ONLY.
 *
 * \copydoc page_ovr  The function initiates the conversion of the already NDEF formatted
 * tag to READ ONLY.After this formation, remote card would be properly Ndef Compliant and READ ONLY.
 * Depending upon the different card type, this function handles formatting procedure.
 * This function supports only for the DESFIRE, MIFARE UL and TOPAZ tags.
 *
 * \param[in] phFriNfc_sNdefSmtCrdFmt_t Pointer to a valid instance of the \ref phFriNfc_sNdefSmartCardFmt_t
 *                             structure describing the component context.
 * \retval  NFCSTATUS_PENDING   The action has been successfully triggered.
 * \retval  Other values        An error has occurred.
 *
 */
NFCSTATUS
phFriNfc_NdefSmtCrd_ConvertToReadOnly (
    phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt);

#endif /* #ifdef FRINFC_READONLY_NDEF */


/**
 *\ingroup grp_fri_smart_card_formatting
 *
 * \brief Smart card Formatting \b Completion \b Routine or \b Process function
 *
 * \copydoc page_ovr Completion Routine: This function is called by the lower layer (OVR HAL)
 *                  when an I/O operation has finished. The internal state machine decides
 *                  whether to call into the lower device again or to complete the process
 *                  by calling into the upper layer's completion routine, stored within this
 *                  component's context (\ref phFriNfc_sNdefSmtCrdFmt_t).
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
void phFriNfc_NdefSmtCrd_Process(void        *Context,
                                 NFCSTATUS    Status);

void phFriNfc_SmtCrdFmt_HCrHandler(phFriNfc_sNdefSmtCrdFmt_t  *NdefSmtCrdFmt,
                                   NFCSTATUS            Status);

/*@}*/

#endif /* PHFRINFC_SMTCRDFMT_H */


