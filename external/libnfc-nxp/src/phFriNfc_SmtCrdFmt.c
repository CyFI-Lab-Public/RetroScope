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
 * \file  phFriNfc_SmtCrdFmt.c
 * \brief This component encapsulates different smart and simple tag formatting functionalities,
 *        for the mapping layer. 
 *
 * Project: NFC-FRI
 *
 * $Date: Mon Dec 13 14:14:13 2010 $
 * $Author: ing02260 $
 * $Revision: 1.9 $
 * $Aliases:  $
 *
 */

#ifndef PH_FRINFC_CARD_FORMAT_DISABLED

#include <phNfcTypes.h>
#include <phFriNfc_OvrHal.h>
#include <phFriNfc_SmtCrdFmt.h>
#ifdef DISABLE_FORMAT
#include <phFriNfc_TopazFormat.h>
#endif /* #ifdef DISABLE_FORMAT */
#include <phFriNfc_MifULFormat.h>
#include <phFriNfc_DesfireFormat.h>
#include <phFriNfc_MifStdFormat.h>
#ifndef PH_FRINFC_FMT_ISO15693_DISABLED
    #include <phFriNfc_ISO15693Format.h>
#endif /* #ifndef PH_FRINFC_FMT_ISO15693_DISABLED */


/*! \ingroup grp_file_attributes
 *  \name NDEF Mapping
 *
 * File: \ref phFriNfc_CardFormatFunctions.c
 *
 */
/*@{*/
// file versions
/*@}*/




void phFriNfc_SmtCrdFmt_HCrHandler(phFriNfc_sNdefSmtCrdFmt_t  *NdefSmtCrdFmt,
                                       NFCSTATUS            Status)
{
    /* set the state back to the Reset_Init state*/
    NdefSmtCrdFmt->State =  PH_FRINFC_SMTCRDFMT_STATE_RESET_INIT;

    /* set the completion routine*/
    NdefSmtCrdFmt->CompletionRoutine[PH_FRINFC_SMTCRDFMT_CR_FORMAT].
        CompletionRoutine(NdefSmtCrdFmt->CompletionRoutine->Context, Status);
}

/*!
 * \brief Used to Reset the context variables , before the actual smart card formatting
 *        procedure.
 *
 */
NFCSTATUS phFriNfc_NdefSmtCrd_Reset(phFriNfc_sNdefSmtCrdFmt_t       *NdefSmtCrdFmt,
                                    void                            *LowerDevice,
                                    phHal_sRemoteDevInformation_t   *psRemoteDevInfo,
                                    phHal_sDevInputParam_t          *psDevInputParam,
                                    uint8_t                         *SendRecvBuffer,
                                    uint16_t                        *SendRecvBuffLen)
{
    NFCSTATUS   result = NFCSTATUS_SUCCESS;
    uint8_t     index;

    if (    (SendRecvBuffLen == NULL) || (NdefSmtCrdFmt == NULL) || (psRemoteDevInfo == NULL) || 
            (SendRecvBuffer == NULL) ||  (LowerDevice == NULL) || 
            (*SendRecvBuffLen == 0) ||  (psDevInputParam == NULL) ||
            (*SendRecvBuffLen < PH_FRINFC_SMTCRDFMT_MAX_SEND_RECV_BUF_SIZE) )
    {
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        /* Initialise the state to Init */
        NdefSmtCrdFmt->State = PH_FRINFC_SMTCRDFMT_STATE_RESET_INIT;
       
        for(index = 0;index<PH_FRINFC_SMTCRDFMT_CR;index++)
        {
            /* Initialise the NdefMap Completion Routine to Null */
            NdefSmtCrdFmt->CompletionRoutine[index].CompletionRoutine = NULL;
            /* Initialise the NdefMap Completion Routine context to Null  */
            NdefSmtCrdFmt->CompletionRoutine[index].Context = NULL;
        }

        /* Lower Device(Always Overlapped HAL Struct initialised in application
            is registred in NdefMap Lower Device) */
        NdefSmtCrdFmt->LowerDevice = LowerDevice;

        /* Remote Device info received from Manual Device Discovery is registered here */
        NdefSmtCrdFmt->psRemoteDevInfo = psRemoteDevInfo;

        /* Trx Buffer registered */
        NdefSmtCrdFmt->SendRecvBuf = SendRecvBuffer;

        /* Trx Buffer Size */
        NdefSmtCrdFmt->SendRecvLength = SendRecvBuffLen;

        /* Register Transfer Buffer Length */
        NdefSmtCrdFmt->SendLength = 0;

        /* Initialise the Format status flag*/
        NdefSmtCrdFmt->FmtProcStatus = 0;

        /* Reset the Card Type */
        NdefSmtCrdFmt->CardType = 0;

        /* Reset MapCompletion Info*/
        NdefSmtCrdFmt->SmtCrdFmtCompletionInfo.CompletionRoutine = NULL;
        NdefSmtCrdFmt->SmtCrdFmtCompletionInfo.Context = NULL;

#ifndef PH_FRINFC_FMT_TOPAZ_DISABLED
        phFriNfc_Topaz_Reset(NdefSmtCrdFmt);

#endif  /* PH_FRINFC_FMT_TOPAZ_DISABLED */

#ifndef PH_FRINFC_FMT_DESFIRE_DISABLED
        /*Reset Desfire Cap Container elements*/
        phFriNfc_Desfire_Reset(NdefSmtCrdFmt);
#endif  /* PH_FRINFC_FMT_DESFIRE_DISABLED */

#ifndef PH_FRINFC_FMT_MIFARESTD_DISABLED
        /*Reset Mifare Standard Container elements*/
        NdefSmtCrdFmt->AddInfo.MfStdInfo.DevInputParam = psDevInputParam;
        phFriNfc_MfStd_Reset(NdefSmtCrdFmt);
#endif  /* PH_FRINFC_MAP_MIFARESTD_DISABLED */

#ifndef PH_FRINFC_FMT_MIFAREUL_DISABLED
        phFriNfc_MfUL_Reset(NdefSmtCrdFmt);
#endif /* #ifndef PH_FRINFC_FMT_MIFAREUL_DISABLED */

#ifndef PH_FRINFC_FMT_ISO15693_DISABLED
        phFriNfc_ISO15693_FmtReset (NdefSmtCrdFmt);
#endif /* #ifndef PH_FRINFC_FMT_ISO15693_DISABLED */

#ifdef PHFRINFC_OVRHAL_MOCKUP
        /*Reset Desfire Cap Container elements*/
  //      phFriNfc_Mockup_H_Reset(NdefSmtCrdFmt);
#endif  /* PHFRINFC_OVRHAL_MOCKUP */
    
    }
    return (result);

}

/*!
 * \brief Completion Routine initialisation
 *
 */
NFCSTATUS phFriNfc_NdefSmtCrd_SetCR(phFriNfc_sNdefSmtCrdFmt_t     *NdefSmtCrdFmt,
                                    uint8_t                       FunctionID,
                                    pphFriNfc_Cr_t                CompletionRoutine,
                                    void                          *CompletionRoutineContext)
{
    NFCSTATUS   status = NFCSTATUS_SUCCESS;
       
    if ((NdefSmtCrdFmt == NULL) || (FunctionID >= PH_FRINFC_SMTCRDFMT_CR) || 
        (CompletionRoutine == NULL) || (CompletionRoutineContext == NULL))
    {
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        /* Register the application callback with the NdefMap Completion Routine */
        NdefSmtCrdFmt->CompletionRoutine[FunctionID].CompletionRoutine = CompletionRoutine;
        
        /* Register the application context with the NdefMap Completion Routine context */
        NdefSmtCrdFmt->CompletionRoutine[FunctionID].Context = CompletionRoutineContext;
    }

    return status;
}

#ifdef FRINFC_READONLY_NDEF

NFCSTATUS
phFriNfc_NdefSmtCrd_ConvertToReadOnly (
    phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt)
{
    NFCSTATUS   result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                  NFCSTATUS_INVALID_PARAMETER);
    uint8_t     sak = 0;

    if((NdefSmtCrdFmt != NULL)
        && (NdefSmtCrdFmt->CompletionRoutine->CompletionRoutine != NULL)
        && (NdefSmtCrdFmt->CompletionRoutine->Context != NULL))
    {
        sak = NdefSmtCrdFmt->psRemoteDevInfo->RemoteDevInfo.Iso14443A_Info.Sak;
        switch (NdefSmtCrdFmt->psRemoteDevInfo->RemDevType)
        {
            case phHal_eMifare_PICC:
            {
                if (0x00 == sak)
                {
                    result = phFriNfc_MfUL_ConvertToReadOnly (NdefSmtCrdFmt);
                }
                else
                {
                    /* MIFARE classic 1k/4k is not supported */
                    result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                        NFCSTATUS_INVALID_REMOTE_DEVICE);
                }
                break;
            }

            case phHal_eISO14443_A_PICC:
            {
                result = phFriNfc_Desfire_ConvertToReadOnly (NdefSmtCrdFmt);
                break;
            }

            default :
            {
                /*  Remote device is not recognised.
                Probably not NDEF compliant */
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                    NFCSTATUS_INVALID_REMOTE_DEVICE);
                break;
            }
        }
    }
    return result;
}

#endif /* #ifdef FRINFC_READONLY_NDEF */


/*!
 * \brief Used to format the different smart cards.
 *
 */
NFCSTATUS phFriNfc_NdefSmtCrd_Format( phFriNfc_sNdefSmtCrdFmt_t *NdefSmtCrdFmt, const uint8_t *ScrtKeyB )
{
    /* Component ID needs to be changed */
    NFCSTATUS   Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                    NFCSTATUS_INVALID_PARAMETER);
    uint8_t     sak = 0;
    
    /* Check for the correct context structure */
    if((NdefSmtCrdFmt != NULL) &&  
        (NdefSmtCrdFmt->CompletionRoutine->CompletionRoutine != NULL) && 
        (NdefSmtCrdFmt->CompletionRoutine->Context != NULL))
    {
#ifdef PH_HAL4_ENABLE
        /* SAK (Select response) */
        sak = NdefSmtCrdFmt->psRemoteDevInfo->RemoteDevInfo.Iso14443A_Info.Sak;

        /* Depending on the Opmodes, call the respective card functions */
        switch ( NdefSmtCrdFmt->psRemoteDevInfo->RemDevType )
#else
        /* SAK (Select response) */
        sak = NdefSmtCrdFmt->psRemoteDevInfo->RemoteDevInfo.CardInfo106.
                Startup106.SelRes;

        /* Depending on the Opmodes, call the respective card functions */
        switch ( NdefSmtCrdFmt->psRemoteDevInfo->OpMode )
#endif /* #ifdef PH_HAL4_ENABLE */
        {
#ifdef PH_HAL4_ENABLE
            case phHal_eMifare_PICC :
#else
            case phHal_eOpModesMifare :
#endif /* #ifdef PH_HAL4_ENABLE */
                /*  Remote device is Mifare card . Check for Mifare
                NDEF compliance */
                if(0x00 == sak) 
                {
#ifndef PH_FRINFC_FMT_MIFAREUL_DISABLED
                    /*  The SAK/Sel_Res says the card is of the type
                        Mifare UL */
                   NdefSmtCrdFmt->CardType = PH_FRINFC_SMTCRDFMT_MIFARE_UL_CARD;
					if (NdefSmtCrdFmt->psRemoteDevInfo->RemoteDevInfo.Iso14443A_Info.UidLength == 7 &&
						NdefSmtCrdFmt->psRemoteDevInfo->RemoteDevInfo.Iso14443A_Info.Uid[0] == 0x04)
					{
						
	                    Result = phFriNfc_MfUL_Format( NdefSmtCrdFmt);
					}
					else
					{
						Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                    NFCSTATUS_INVALID_REMOTE_DEVICE);
					}
#else
                    Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                    NFCSTATUS_INVALID_REMOTE_DEVICE);
#endif /* #ifndef PH_FRINFC_FMT_MIFAREUL_DISABLED */
                }
                else if((0x08 == (sak & 0x18)) || 
                        (0x18 == (sak & 0x18)) ||
                        (0x01 == sak))
                {
#ifndef PH_FRINFC_FMT_MIFARESTD_DISABLED
                    NdefSmtCrdFmt->CardType = (uint8_t)
                        (((sak & 0x18) == 0x08)?
                        PH_FRINFC_SMTCRDFMT_MFSTD_1K_CRD:
                        PH_FRINFC_SMTCRDFMT_MFSTD_4K_CRD);

                    /*  The SAK/Sel_Res says the card is of the type
                        Mifare standard */
                    Result = phFriNfc_MfStd_Format( NdefSmtCrdFmt, ScrtKeyB);
#else
                    Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                    NFCSTATUS_INVALID_REMOTE_DEVICE);
#endif /* #ifndef PH_FRINFC_FMT_MIFARESTD_DISABLED */
                }
                else
                {
                    /*  Invalid Mifare card, as the remote device 
                        info - opmode says its a Mifare card but, 
                        The SAK/Sel_Res is wrong */
                    Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                        NFCSTATUS_INVALID_REMOTE_DEVICE);
                }
            break;
#ifdef PH_HAL4_ENABLE
            case phHal_eISO14443_A_PICC :
#else
            case phHal_eOpModesISO14443_4A :
#endif /* #ifdef PH_HAL4_ENABLE */
                /*  Remote device is Desfire card . Check for Desfire
                NDEF compliancy */
                 if(0x20 == (sak & 0xFF))
                {
#ifndef PH_FRINFC_FMT_DESFIRE_DISABLED
                    NdefSmtCrdFmt->CardType = PH_FRINFC_SMTCRDFMT_ISO14443_4A_CARD;
                    /*  The SAK/Sel_Res says the card is of the type
                        ISO14443_4A */
                    
                    Result = phFriNfc_Desfire_Format(NdefSmtCrdFmt);
#else
                    Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                        NFCSTATUS_INVALID_REMOTE_DEVICE);
#endif /* #ifndef PH_FRINFC_FMT_DESFIRE_DISABLED */
                }
                else
                {
                    /*  Invalid Desfire card, as the remote device 
                        info - opmode says its a desfire card but, 
                        The SAK/Sel_Res is wrong */
                    Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                        NFCSTATUS_INVALID_REMOTE_DEVICE);
                }
            break;
#ifdef PH_HAL4_ENABLE
            case phHal_eJewel_PICC :
#else
            case phHal_eOpModesJewel :
#endif /* #ifdef PH_HAL4_ENABLE */
                /*  Remote device is Topaz card . Check for Topaz
                NDEF compliancy */
                if(0xC2 == sak)
                {
#ifndef PH_FRINFC_FMT_TOPAZ_DISABLED
                    NdefSmtCrdFmt->CardType = PH_FRINFC_SMTCRDFMT_TOPAZ_CARD;
                    /*  The SAK/Sel_Res says the card is of the type
                        ISO14443_4A */
                    Result = phFriNfc_Topaz_Format(NdefSmtCrdFmt);
#else
                    Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                        NFCSTATUS_INVALID_REMOTE_DEVICE);
#endif /* #ifndef PH_FRINFC_FMT_TOPAZ_DISABLED */
                    
                }
                else
                {
                    /*  Invalid Topaz card, as the remote device 
                        info - opmode says its a desfire card but, 
                        The SAK/Sel_Res is wrong */
                    Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                        NFCSTATUS_INVALID_REMOTE_DEVICE);
                }
            break;

#ifdef PHFRINFC_OVRHAL_MOCKUP
            case phHal_eOpModesMockup :
                    /*Set the OpMode Ttype Flag*/
                    NdefSmtCrdFmt->OpModeType[0] = phHal_eOpModesMockup;
                    NdefSmtCrdFmt->OpModeType[1] = phHal_eOpModesArrayTerminator;
                    //Result = phFriNfc_Mockup_ChkNdef(NdefSmtCrdFmt);
            break;
#endif  /* PHFRINFC_OVRHAL_MOCKUP */

#ifndef PH_FRINFC_FMT_ISO15693_DISABLED
            case phHal_eISO15693_PICC:
            {
                Result = phFriNfc_ISO15693_Format (NdefSmtCrdFmt);
                break;
            }
#endif /* #ifndef PH_FRINFC_FMT_ISO15693_DISABLED */
            default :
                /*  Remote device is not recognised.
                Probably not NDEF compliant */
                Result = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                    NFCSTATUS_INVALID_REMOTE_DEVICE);
            break;
        }
    }
    return Result;
}
/*!
 * \brief Handles different request and responses from the integration layer.
 *
 */
void phFriNfc_NdefSmtCrd_Process(void        *Context,
                                 NFCSTATUS    Status)
{
    if ( Context != NULL )
    {
        phFriNfc_sNdefSmtCrdFmt_t  *NdefSmtCrdFmt = (phFriNfc_sNdefSmtCrdFmt_t *)Context;
#ifdef PH_HAL4_ENABLE
        switch ( NdefSmtCrdFmt->psRemoteDevInfo->RemDevType )
#else
        switch ( NdefSmtCrdFmt->psRemoteDevInfo->OpMode )
#endif /* #ifdef PH_HAL4_ENABLE */
        {
#ifdef PH_HAL4_ENABLE
            case phHal_eMifare_PICC :
#else
            case  phHal_eOpModesMifare :
#endif /* #ifdef PH_HAL4_ENABLE */
                if((NdefSmtCrdFmt->CardType == PH_FRINFC_SMTCRDFMT_MFSTD_1K_CRD) ||
                    (NdefSmtCrdFmt->CardType == PH_FRINFC_SMTCRDFMT_MFSTD_4K_CRD))
                {
#ifndef PH_FRINFC_FMT_MIFARESTD_DISABLED
                    /*  Remote device is Mifare Standard card */
                    phFriNfc_MfStd_Process(NdefSmtCrdFmt,Status);
                    
#else   /* PH_FRINFC_FMT_MIFARESTD_DISABLED*/
                        Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                            NFCSTATUS_INVALID_REMOTE_DEVICE);               
#endif  /* PH_FRINFC_FMT_MIFARESTD_DISABLED*/
                }
                else
                {
#ifndef PH_FRINFC_FMT_MIFAREUL_DISABLED
                    /*  Remote device is Mifare UL card */
                    phFriNfc_MfUL_Process(NdefSmtCrdFmt,Status);
#else   /* PH_FRINFC_FMT_MIFAREUL_DISABLED*/
                    Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                         NFCSTATUS_INVALID_REMOTE_DEVICE);               
#endif  /* PH_FRINFC_FMT_MIFAREUL_DISABLED*/
                }
            break;

#ifdef PH_HAL4_ENABLE
            case phHal_eISO14443_A_PICC :
#else
            case phHal_eOpModesISO14443_4A :
#endif /* #ifdef PH_HAL4_ENABLE */
#ifndef PH_FRINFC_FMT_DESFIRE_DISABLED
                /*  Remote device is Desfire card */
                phFriNfc_Desf_Process(NdefSmtCrdFmt, Status);
#else   /* PH_FRINFC_FMT_DESFIRE_DISABLED*/
                 Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                            NFCSTATUS_INVALID_REMOTE_DEVICE);               
#endif  /* PH_FRINFC_FMT_DESFIRE_DISABLED*/
            break;
#ifdef PH_HAL4_ENABLE
            case phHal_eJewel_PICC :
#else
            case phHal_eOpModesJewel:
#endif /* #ifdef PH_HAL4_ENABLE */
#ifndef PH_FRINFC_FMT_TOPAZ_DISABLED
                /*  Remote device is Topaz Smart card */
               phFriNfc_Topaz_Process(NdefSmtCrdFmt, Status);
#else   /* PH_FRINFC_FMT_TOPAZ_DISABLED*/
               Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT, 
                                            NFCSTATUS_INVALID_REMOTE_DEVICE);               
#endif  /* PH_FRINFC_FMT_TOPAZ_DISABLED*/
            break;

#ifndef PH_FRINFC_FMT_ISO15693_DISABLED
            case phHal_eISO15693_PICC :
            {
                phFriNfc_ISO15693_FmtProcess (NdefSmtCrdFmt, Status);
                break;
            }
#endif /* #ifndef PH_FRINFC_FMT_ISO15693_DISABLED */

#ifdef PHFRINFC_OVRHAL_MOCKUP
            case phHal_eOpModesMockup:
                /*  Remote device is Desfire card */
                //phFriNfc_Mockup_Process(NdefSmtCrdFmt, Status);     
            break;
#endif  /* PHFRINFC_OVRHAL_MOCKUP*/
            default : 
                /*  Remote device opmode not recognised.
                    Probably not NDEF compliant */
                Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,
                                    NFCSTATUS_INVALID_REMOTE_DEVICE);
                /* set the state back to the Reset_Init state*/
                NdefSmtCrdFmt->State =  PH_FRINFC_SMTCRDFMT_STATE_RESET_INIT;

                /* set the completion routine*/
                NdefSmtCrdFmt->CompletionRoutine[PH_FRINFC_SMTCRDFMT_CR_INVALID_OPE].
                CompletionRoutine(NdefSmtCrdFmt->CompletionRoutine->Context, Status);
            break;
        }
    }
    else
    {
        Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_SMTCRDFMT,\
                            NFCSTATUS_INVALID_PARAMETER);
        /* The control should not come here. As Context itself is NULL ,
           Can't call the CR*/
    }
}

#endif  /* PH_FRINFC_CARD_FORMAT_DISABLED */

