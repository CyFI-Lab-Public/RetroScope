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
* \file  phFriNfcNdefMap.c
* \brief NFC Ndef Mapping For Different Smart Cards.
*
* Project: NFC-FRI
*
* $Date: Mon Dec 13 14:14:12 2010 $
* $Author: ing02260 $
* $Revision: 1.39 $
* $Aliases:  $
*
*/


#include <phFriNfc_NdefMap.h>

#ifndef PH_FRINFC_MAP_MIFAREUL_DISABLED
#include <phFriNfc_MifareULMap.h>
#endif  /* PH_FRINFC_MAP_MIFAREUL_DISABLED*/

#ifndef PH_FRINFC_MAP_TOPAZ_DISABLED
#include <phFriNfc_TopazMap.h>
#endif  /* PH_FRINFC_MAP_TOPAZ_DISABLED */

#ifndef PH_FRINFC_MAP_MIFARESTD_DISABLED
#include <phFriNfc_MifareStdMap.h>
#endif  /* PH_FRINFC_MAP_MIFARESTD_DISABLED */

#ifndef PH_FRINFC_MAP_DESFIRE_DISABLED
#include <phFriNfc_DesfireMap.h>
#endif  /* PH_FRINFC_MAP_DESFIRE_DISABLED */

#ifndef PH_FRINFC_MAP_FELICA_DISABLED
#include <phFriNfc_FelicaMap.h>
#endif  /* PH_FRINFC_MAP_FELICA_DISABLED */

#ifndef PH_FRINFC_MAP_ISO15693_DISABLED
#include <phFriNfc_ISO15693Map.h>
#endif  /* PH_FRINFC_MAP_ISO15693_DISABLED */

#ifdef PHFRINFC_OVRHAL_MOCKUP
#include <phFriNfc_MockupMap.h>
#endif  /* PHFRINFC_OVRHAL_MOCKUP */


#include <phFriNfc_OvrHal.h>

/*! \ingroup grp_file_attributes
*  \name NDEF Mapping
*
* File: \ref phFriNfcNdefMap.c
*
*/
/*@{*/
#define PHFRINFCNDEFMAP_FILEREVISION "$Revision: 1.39 $"
#define PHFRINFCNDEFMAP_FILEALIASES  "$Aliases:  $"
/*@}*/

#ifndef PH_FRINFC_MAP_DESFIRE_DISABLED
/* Desfire capability Container Reset Helper */
static void phFriNfc_DesfCapCont_HReset(phFriNfc_NdefMap_t  *NdefMap);
#endif  /* PH_FRINFC_MAP_DESFIRE_DISABLED */

#ifndef PH_FRINFC_MAP_FELICA_DISABLED
/* Felica Smart Tag Reset Helper */
static void phFriNfc_Felica_HReset(phFriNfc_NdefMap_t  *NdefMap);
#endif  /* PH_FRINFC_MAP_FELICA_DISABLED */


/* \note    This function has to be called at the beginning, after creating an
*          instance of \ref phFriNfc_NdefMap_t . Use this function to reset
*          the instance and/or switch to a different underlying device (
*          different NFC device or device mode, or different Remote Device).
*/

NFCSTATUS phFriNfc_NdefMap_Reset(   phFriNfc_NdefMap_t              *NdefMap,
                                 void                            *LowerDevice,
                                 phHal_sRemoteDevInformation_t   *psRemoteDevInfo,
                                 phHal_sDevInputParam_t          *psDevInputParam,
                                 uint8_t                         *TrxBuffer,
                                 uint16_t                        TrxBufferSize,
                                 uint8_t                         *ReceiveBuffer,
                                 uint16_t                        *ReceiveLength,
                                 uint16_t                        *DataCount)
{
    NFCSTATUS   status = NFCSTATUS_SUCCESS;
    uint8_t     index;

    if (    (ReceiveLength == NULL) || (NdefMap == NULL) || (psRemoteDevInfo == NULL) ||
        (TrxBuffer == NULL) || (TrxBufferSize == 0)  || (LowerDevice == NULL) ||
        (*ReceiveLength == 0) || (ReceiveBuffer == NULL) || (DataCount == NULL) ||
        (psDevInputParam == NULL) ||
        (*ReceiveLength < PH_FRINFC_NDEFMAP_MAX_SEND_RECV_BUF_SIZE ))
    {
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        /* Initialise the state to Init */
        NdefMap->State = PH_FRINFC_NDEFMAP_STATE_RESET_INIT;

        for(index = 0;index<PH_FRINFC_NDEFMAP_CR;index++)
        {
            /* Initialise the NdefMap Completion Routine to Null */
            NdefMap->CompletionRoutine[index].CompletionRoutine = NULL;
            /* Initialise the NdefMap Completion Routine context to Null  */
            NdefMap->CompletionRoutine[index].Context = NULL;
        }

        /* Lower Device(Always Overlapped HAL Struct initialised in application
        is registred in NdefMap Lower Device) */
        NdefMap->LowerDevice = LowerDevice;

        /* Remote Device info received from Manual Device Discovery is registered here */
        NdefMap->psRemoteDevInfo = psRemoteDevInfo;

        /* Transfer Buffer registered */
        NdefMap->ApduBuffer = TrxBuffer;

        /* Set the MaxApduBufferSize */
        NdefMap->ApduBufferSize = TrxBufferSize;

        /* Set APDU Buffer Index */
        NdefMap->ApduBuffIndex = 0;

        /* Register Transfer Buffer Length */
        NdefMap->SendLength = 0;

        /* Register Receive Buffer */
        NdefMap->SendRecvBuf = ReceiveBuffer;

        /* Register Receive Buffer Length */
        NdefMap->SendRecvLength = ReceiveLength;

        /* Register Temporary Receive Buffer Length */
        NdefMap->TempReceiveLength = *ReceiveLength;

        /* Register Data Count variable and set it to zero */
        NdefMap->DataCount = DataCount;
        *NdefMap->DataCount = 0;

        /* Reset the PageOffset */
        NdefMap->Offset = 0;

        /* Reset the NumOfBytesRead*/
        NdefMap->NumOfBytesRead = 0;

        /* Reset the NumOfBytesWritten*/
        NdefMap->NumOfBytesWritten = 0;

        /* Reset the Card Type */
        NdefMap->CardType = 0;

        /* Reset the Memory Card Size*/
        NdefMap->CardMemSize = 0;

        /* Reset the Previous Operation*/
        NdefMap->PrevOperation = 0;

        /* Reset the Desfire Operation Flag*/
        NdefMap->DespOpFlag = 0;

        /* Reset MapCompletion Info*/
        NdefMap->MapCompletionInfo.CompletionRoutine = NULL;
        NdefMap->MapCompletionInfo.Context = NULL;

        /*  Reset the ReadingForWriteOperation flag. */
        NdefMap->ReadingForWriteOperation = 0;  /*  FALSE  */

#ifndef PH_FRINFC_MAP_DESFIRE_DISABLED
        /*Reset Desfire Cap Container elements*/
        phFriNfc_DesfCapCont_HReset(NdefMap);
#endif  /* PH_FRINFC_MAP_DESFIRE_DISABLED */

#ifndef PH_FRINFC_MAP_MIFARESTD_DISABLED
        /*Reset Mifare Standard Container elements*/
        NdefMap->StdMifareContainer.DevInputParam = psDevInputParam;
        status = phFriNfc_MifareStdMap_H_Reset(NdefMap);
#endif  /* PH_FRINFC_MAP_MIFARESTD_DISABLED */

#ifndef PH_FRINFC_MAP_FELICA_DISABLED
        /*Reset Felica Tag elements*/
        NdefMap->FelicaPollDetails.DevInputParam = psDevInputParam;
        phFriNfc_Felica_HReset(NdefMap);
#endif  /* PH_FRINFC_MAP_FELICA_DISABLED */

#if !(defined(PH_FRINFC_MAP_TOPAZ_DISABLED ) || defined (PH_FRINFC_MAP_TOPAZ_DYNAMIC_DISABLED ))

        phFriNfc_TopazMap_H_Reset(NdefMap);
#endif  /* PH_FRINFC_MAP_TOPAZ_DISABLED || PH_FRINFC_MAP_TOPAZ_DYNAMIC_DISABLED  */


#ifndef PH_FRINFC_MAP_MIFAREUL_DISABLED
        status = phFriNfc_MifareUL_H_Reset(NdefMap);
#endif

#ifdef PHFRINFC_OVRHAL_MOCKUP
        /*Reset Desfire Cap Container elements*/
        phFriNfc_Mockup_H_Reset(NdefMap);
#endif  /* PHFRINFC_OVRHAL_MOCKUP */

        /*
        *  Fix for PR - 0001256
        *  Date- 08-08-08
        */
        NdefMap->CardState = PH_NDEFMAP_CARD_STATE_INVALID;
    }

    return (status);
}

/*!
* Registering the Completion Routine.
*
* This function requires the caller to set a Completion Routine
* which serves as notifier for the upper component.
* NOTE: Please refer the header file for more information.
*
*/

NFCSTATUS phFriNfc_NdefMap_SetCompletionRoutine(phFriNfc_NdefMap_t     *NdefMap,
                                                uint8_t                 FunctionID,
                                                pphFriNfc_Cr_t          CompletionRoutine,
                                                void                   *CompletionRoutineContext)
{
    NFCSTATUS   status = NFCSTATUS_SUCCESS;

    if ( ( NdefMap == NULL ) || (FunctionID >= PH_FRINFC_NDEFMAP_CR) ||
        ( CompletionRoutine == NULL) || (CompletionRoutineContext == NULL))
    {
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        /* Register the application callback with the NdefMap Completion Routine */
        NdefMap->CompletionRoutine[FunctionID].CompletionRoutine = CompletionRoutine;

        /* Register the application context with the NdefMap Completion Routine context */
        NdefMap->CompletionRoutine[FunctionID].Context = CompletionRoutineContext;
    }

    return status;
}

/*!
* Initiates Reading of NDEF information from the Remote Device.
*
* Remote Peer device may be of type any card. Ex: desfire,felica,jewel
* mifareUL,mifare 1K etc. The function initiates the reading of NDEF
* information from a Remote Device.
*
* This is the main NdefMap read function call.Internally,depending upon
* the CardType,respective mifare/desfire read functions are called.
* In future this can be extended to support any types of card.
*
* It performs a reset of the state and triggers/starts the raed action (state machine).
* A periodic call of the \ref phFriNfcNdefMap_Process has to be done once the action
* has been triggered.
*
* NOTE: Please refer the header file for more information.
*
*/

NFCSTATUS phFriNfc_NdefMap_RdNdef(  phFriNfc_NdefMap_t  *NdefMap,
                                  uint8_t             *PacketData,
                                  uint32_t            *PacketDataLength,
                                  uint8_t             Offset)
{
    NFCSTATUS   status = NFCSTATUS_PENDING;


    /* check for validity of input parameters*/
    if (( PacketData == NULL )
        || ( NdefMap == NULL )
        || ( PacketDataLength == NULL )
        || ( *PacketDataLength == 0 )
        || ( ( Offset != PH_FRINFC_NDEFMAP_SEEK_CUR) && (Offset != PH_FRINFC_NDEFMAP_SEEK_BEGIN ))
        || (NdefMap->CompletionRoutine->CompletionRoutine == NULL)
        || (NdefMap->CompletionRoutine->Context == NULL )
        )
    {
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_PARAMETER);
    }
    else if (NdefMap->CardState == PH_NDEFMAP_CARD_STATE_INVALID)
    {
        /*  Card is in invalid state, cannot have any read/write
        operations*/
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,\
            NFCSTATUS_INVALID_FORMAT);
    }
    else if(NdefMap->CardState == PH_NDEFMAP_CARD_STATE_INITIALIZED)
    {
        /*  Can't read any data from the card:TLV length is zero*/
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_EOF_NDEF_CONTAINER_REACHED);

        NdefMap->NumOfBytesRead = PacketDataLength;
        *NdefMap->NumOfBytesRead = 0;


    }
    else if ( (NdefMap->PrevOperation == PH_FRINFC_NDEFMAP_WRITE_OPE) && (Offset != PH_FRINFC_NDEFMAP_SEEK_BEGIN ))
    {
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_DEVICE_REQUEST);
    }
    else
    {
        /*  Check the offset given by the user
        If the offset is 1 (SEEK_BEGIN), reset everything and start
        reading from the first Page of the card.
        else if offset is 0 (PH_FRINFC_NDEFMAP_SEEK_CUR), continue reading
        No need to reset the parameters.  */

        if ( Offset == PH_FRINFC_NDEFMAP_SEEK_BEGIN )
        {
            NdefMap->ApduBuffIndex = 0;
            *NdefMap->DataCount = 0;
        }

        if  ( (NdefMap->CardType == PH_FRINFC_NDEFMAP_ISO14443_4A_CARD) &&
            (Offset == PH_FRINFC_NDEFMAP_SEEK_CUR) && (*NdefMap->DataCount == 0 ))
        {

            /*  A READ operation cannot be done if the previuos operation was WRITE
            unless the offset is set to PH_FRINFC_NDEFMAP_SEEK_BEGIN Or
            Read Operation with Offset set to Continue & DataCount set to 0 */
            status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_DEVICE_REQUEST);
        }
        else
        {
            switch ( NdefMap->CardType)
            {
#ifndef PH_FRINFC_MAP_MIFAREUL_DISABLED
                case  PH_FRINFC_NDEFMAP_MIFARE_UL_CARD :
                    /*  Mifare card selected. Call Mifare read */
                    status = phFriNfc_MifareUL_RdNdef ( NdefMap,
                        PacketData,
                        PacketDataLength,
                        Offset);
                    break;
#endif /* PH_FRINFC_MAP_MIFAREUL_DISABLED */

#ifndef PH_FRINFC_MAP_DESFIRE_DISABLED
                case PH_FRINFC_NDEFMAP_ISO14443_4A_CARD :
#ifdef DESFIRE_EV1
                case PH_FRINFC_NDEFMAP_ISO14443_4A_CARD_EV1 :
#endif /* #ifdef DESFIRE_EV1 */
                    /*  Desfire card selected. Call Desfire read */
                    status = phFriNfc_Desfire_RdNdef(   NdefMap,
                        PacketData,
                        PacketDataLength,
                        Offset);
                    break;
#endif  /* PH_FRINFC_MAP_DESFIRE_DISABLED */

#ifndef PH_FRINFC_MAP_MIFARESTD_DISABLED
                case  PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD :
                case  PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD :
                    /*  Mifare card selected. Call Mifare read */
                    status = phFriNfc_MifareStdMap_RdNdef ( NdefMap,
                        PacketData,
                        PacketDataLength,
                        Offset);
                    break;
#endif  /* PH_FRINFC_MAP_MIFARESTD_DISABLED */

#ifndef PH_FRINFC_MAP_FELICA_DISABLED
                case PH_FRINFC_NDEFMAP_FELICA_SMART_CARD :
                    /*  Desfire card selected. Call Desfire Write */
                    status =  phFriNfc_Felica_RdNdef(  NdefMap,
                        PacketData,
                        PacketDataLength,
                        Offset);
                    break;
#endif  /* PH_FRINFC_MAP_FELICA_DISABLED */

#ifndef PH_FRINFC_MAP_TOPAZ_DISABLED
                case PH_FRINFC_NDEFMAP_TOPAZ_CARD :
                    /*  Topaz card selected. Call Topaz read */
                    status =  phFriNfc_TopazMap_RdNdef(  NdefMap,
                        PacketData,
                        PacketDataLength,
                        Offset);
                    break;
#ifndef PH_FRINFC_MAP_TOPAZ_DYNAMIC_DISABLED
                case PH_FRINFC_NDEFMAP_TOPAZ_DYNAMIC_CARD :
                    /*  Topaz card selected. Call Topaz read */
                    status =  phFriNfc_TopazDynamicMap_RdNdef(  NdefMap,
                        PacketData,
                        PacketDataLength,
                        Offset);
                    break;
#endif  /* PH_FRINFC_MAP_TOPAZ_DYNAMIC_DISABLED */
#endif  /* PH_FRINFC_MAP_TOPAZ_DISABLED */

#ifndef PH_FRINFC_MAP_ISO15693_DISABLED
                case PH_FRINFC_NDEFMAP_ISO15693_CARD:
                    status =  phFriNfc_ISO15693_RdNdef(  NdefMap,
                        PacketData,
                        PacketDataLength,
                        Offset);
                    break;
#endif /* #ifndef PH_FRINFC_MAP_ISO15693_DISABLED */

#ifdef PHFRINFC_OVRHAL_MOCKUP
                case PH_FRINFC_NDEFMAP_MOCKUP_CARD :
                    /*  Mockup card selected. Call Mockup Write */
                    status =  phFriNfc_Mockup_RdNdef(  NdefMap,
                        PacketData,
                        PacketDataLength,
                        Offset);
                    break;
#endif  /* PHFRINFC_OVRHAL_MOCKUP */

                default :
                    /*  Unknown card type. Return error */
                    status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,\
                        NFCSTATUS_INVALID_REMOTE_DEVICE);

                    break;
            }
        }
    }
    return status;
}

/*!
* Initiates Writing of NDEF information to the Remote Device.
*
* The function initiates the writing of NDEF information to a Remote Device
*
* Remote Peer device may be of type any card. Ex: desfire,felica,jewel
* mifareUL,mifare 1K etc. The function initiates the reading of NDEF
* information from a Remote Device.
*
* This is a main write api.Internally,depending upon the CardType,
* respective mifare/desfire write apis are called.In future this can be
* extended to support any types of card.
*
* It performs a reset of the state and starts the action (state machine).
* A periodic call of the \ref phFriNfcNdefMap_Process has to be done once
* the action has been triggered.
*
* NOTE: Please refer the header file for more information.
*
*/


NFCSTATUS phFriNfc_NdefMap_WrNdef(  phFriNfc_NdefMap_t  *NdefMap,
                                  uint8_t             *PacketData,
                                  uint32_t            *PacketDataLength,
                                  uint8_t             Offset)
{
    NFCSTATUS   status = NFCSTATUS_PENDING;
    uint8_t     StatusChk=0;

     if (     (PacketData          == NULL)
        ||  ( NdefMap             == NULL )
        /* + Mantis 442 */
        || ( PacketDataLength    == NULL )
        /* - Mantis 442 */
        || ( *PacketDataLength   == 0 )
        || ((Offset != PH_FRINFC_NDEFMAP_SEEK_CUR) && (Offset != PH_FRINFC_NDEFMAP_SEEK_BEGIN ))
        || (NdefMap->CompletionRoutine->CompletionRoutine == NULL)
        || (NdefMap->CompletionRoutine->Context == NULL)
        )
    {
        /*  Invalid input parameter error   */
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,NFCSTATUS_INVALID_PARAMETER);
    }
    else if (( NdefMap->CardState == PH_NDEFMAP_CARD_STATE_INVALID) &&
        (PH_FRINFC_NDEFMAP_TOPAZ_CARD != NdefMap->CardType) &&
        (PH_FRINFC_NDEFMAP_TOPAZ_DYNAMIC_CARD != NdefMap->CardType))
    {
        /*  Card is in invalid state, cannot have any read/write
        operations*/
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,\
            NFCSTATUS_INVALID_FORMAT);
    }

    else if ( NdefMap->CardState == PH_NDEFMAP_CARD_STATE_READ_ONLY )

    {
        /*Can't write to the card :No Grants */
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,\
            NFCSTATUS_NOT_ALLOWED);

        /* set the no. bytes written is zero*/
        NdefMap->WrNdefPacketLength = PacketDataLength;
        *NdefMap->WrNdefPacketLength = 0;

    }
    else
    {
        /*  Check the offset given by the user
        If the offset is 1 (SEEK_BEGIN), reset everything and start
        writing from the first Byte of the card.
        else if offset is 0 (PH_FRINFC_NDEFMAP_SEEK_CUR), continue writing
        No need to reset the parameters.  */
         if (( NdefMap->CardState == PH_NDEFMAP_CARD_STATE_INVALID) &&
            (PH_FRINFC_NDEFMAP_TOPAZ_DYNAMIC_CARD == NdefMap->CardType))
        {
            /* If Topaz Dynamic card CC bytes are not valid then also allow writing,
            If card is really good then writing will be done properly and reading can be performed,
            otherwise writing or reading will fail. so, setting card state to
            NdefMap->CardState = PH_NDEFMAP_CARD_STATE_READ_WRITE */
            NdefMap->CardState = PH_NDEFMAP_CARD_STATE_READ_WRITE;
        }

        if ( Offset == PH_FRINFC_NDEFMAP_SEEK_BEGIN )
        {
            NdefMap->ApduBuffIndex = 0;
            *NdefMap->DataCount = 0;
        }

        if ( (NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_UL_CARD) ||
            (NdefMap->CardType == PH_FRINFC_NDEFMAP_ISO14443_4A_CARD))
        {
            if (( (NdefMap->PrevOperation == PH_FRINFC_NDEFMAP_READ_OPE) && (Offset != PH_FRINFC_NDEFMAP_SEEK_BEGIN )) ||
                ( (Offset == PH_FRINFC_NDEFMAP_SEEK_CUR) && (*NdefMap->DataCount == 0 )))
            {
                /*  A WRITE operation cannot be done if the previuos operation was READ
                unless the offset is set to PH_FRINFC_NDEFMAP_SEEK_BEGIN OR
                Write Operation with Offset set to Continue & DataCount set to 0  */
                status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_DEVICE_REQUEST);
                StatusChk = 1;
            }
        }
        if(StatusChk != 1)
        {
            NdefMap->WrNdefPacketLength =   PacketDataLength;
            switch ( NdefMap->CardType)
            {
#ifndef PH_FRINFC_MAP_MIFAREUL_DISABLED
                case  PH_FRINFC_NDEFMAP_MIFARE_UL_CARD :
                    /*  Mifare card selected. Call Mifare Write */
                    status =  phFriNfc_MifareUL_WrNdef( NdefMap,
                        PacketData,
                        PacketDataLength,
                        Offset);
                    break;
#endif /* PH_FRINFC_MAP_MIFAREUL_DISABLED */

#ifndef PH_FRINFC_MAP_DESFIRE_DISABLED
                case PH_FRINFC_NDEFMAP_ISO14443_4A_CARD :
#ifdef DESFIRE_EV1
                case PH_FRINFC_NDEFMAP_ISO14443_4A_CARD_EV1 :
#endif /* #ifdef DESFIRE_EV1 */
                    /*  Desfire card selected. Call Desfire Write */
                    status =  phFriNfc_Desfire_WrNdef(  NdefMap,
                        PacketData,
                        PacketDataLength,
                        Offset);
                    break;
#endif  /* PH_FRINFC_MAP_DESFIRE_DISABLED */

#ifndef PH_FRINFC_MAP_MIFARESTD_DISABLED
                case  PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD :
                case  PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD :
                    /*  Mifare card selected. Call Mifare read */
                    status = phFriNfc_MifareStdMap_WrNdef ( NdefMap,
                        PacketData,
                        PacketDataLength,
                        Offset);
                    break;
#endif  /* PH_FRINFC_MAP_MIFARESTD_DISABLED */

#ifndef PH_FRINFC_MAP_FELICA_DISABLED
                case PH_FRINFC_NDEFMAP_FELICA_SMART_CARD :
                    /*  Desfire card selected. Call Desfire Write */
                    status =  phFriNfc_Felica_WrNdef(  NdefMap,
                        PacketData,
                        PacketDataLength,
                        Offset);
                    break;
#endif  /* PH_FRINFC_MAP_FELICA_DISABLED */

#ifndef PH_FRINFC_MAP_TOPAZ_DISABLED
                case PH_FRINFC_NDEFMAP_TOPAZ_CARD :
                    /*  Topaz card selected. Call Topaz Write */
                    status =  phFriNfc_TopazMap_WrNdef(  NdefMap,
                        PacketData,
                        PacketDataLength,
                        Offset);
                    break;
#ifndef PH_FRINFC_MAP_TOPAZ_DYNAMIC_DISABLED
                case PH_FRINFC_NDEFMAP_TOPAZ_DYNAMIC_CARD :
                    /*  Topaz card selected. Call Topaz Write */
                    status =  phFriNfc_TopazDynamicMap_WrNdef(  NdefMap,
                        PacketData,
                        PacketDataLength,
                        Offset);
                    break;
#endif  /* PH_FRINFC_MAP_TOPAZ_DYNAMIC_DISABLED */
#endif  /* PH_FRINFC_MAP_TOPAZ_DISABLED */

#ifndef PH_FRINFC_MAP_ISO15693_DISABLED
                case PH_FRINFC_NDEFMAP_ISO15693_CARD:
                    status =  phFriNfc_ISO15693_WrNdef(  NdefMap,
                        PacketData,
                        PacketDataLength,
                        Offset);
                    break;
#endif /* #ifndef PH_FRINFC_MAP_ISO15693_DISABLED */


#ifdef PHFRINFC_OVRHAL_MOCKUP
                case PH_FRINFC_NDEFMAP_MOCKUP_CARD :
                    /*  Mockup card selected. Call Mockup Write */
                    status =  phFriNfc_Mockup_WrNdef(  NdefMap,
                        PacketData,
                        PacketDataLength,
                        Offset);
                    break;
#endif  /* PHFRINFC_OVRHAL_MOCKUP */
                default :
                    /*  Unknown card type. Return error */
                    status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                        NFCSTATUS_INVALID_REMOTE_DEVICE);
                    break;
            }
        }
    }
    return status;
}

#ifdef FRINFC_READONLY_NDEF

NFCSTATUS
phFriNfc_NdefMap_ConvertToReadOnly (
    phFriNfc_NdefMap_t          *NdefMap)
{
    NFCSTATUS   result = NFCSTATUS_PENDING;


    /*  Check for ndefmap context and relevant state. Else return error*/
    if (NULL == NdefMap)
    {
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_PARAMETER);
    }
    else if ((NdefMap->CompletionRoutine->CompletionRoutine == NULL) 
        || (NdefMap->CompletionRoutine->Context == NULL))
    {
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        switch (NdefMap->CardType)
        {
            case PH_FRINFC_NDEFMAP_TOPAZ_CARD:
            {
                result = phFriNfc_TopazMap_ConvertToReadOnly (NdefMap);
                break;
            }

            case PH_FRINFC_NDEFMAP_TOPAZ_DYNAMIC_CARD:
            {
                result = phFriNfc_TopazDynamicMap_ConvertToReadOnly (NdefMap);
                break;
            }

            case PH_FRINFC_NDEFMAP_ISO15693_CARD:
            {
                result = phFriNfc_ISO15693_ConvertToReadOnly (NdefMap);
                break;
            }

            default:
            {
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                                    NFCSTATUS_INVALID_REMOTE_DEVICE);
                break;
            }
        }
    }
    return result;
}

#endif /* #ifdef FRINFC_READONLY_NDEF */

/*!
* Check whether a particular Remote Device is NDEF compliant.
*
* The function initiates the ndef compliancy check.
*
* This is a main check ndef api.Internally,depending upon the different
* opmodes,respective mifare/desfire checkNdef apis are called.
* In future this can be extended to check any types of card ndef
* compliancy.
*
* It performs a reset of the state and starts the action (state machine).
* A periodic call of the \ref phFriNfcNdefMap_Process has to be done once
* the action has been triggered.
*
* NOTE: Please refer the header file for more information.
*
*/

NFCSTATUS phFriNfc_NdefMap_ChkNdef( phFriNfc_NdefMap_t     *NdefMap)
{
    NFCSTATUS   status = NFCSTATUS_PENDING;
    uint8_t     sak;


    /*  Check for ndefmap context and relevant state. Else return error*/
    if ( NdefMap == NULL )
    {
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if( (NdefMap->State !=  PH_FRINFC_NDEFMAP_STATE_RESET_INIT) ||
            (NdefMap->psRemoteDevInfo->SessionOpened != 0x01 ) )
            /*  Harsha: If SessionOpened is not 1, this means that connect has not happened */
        {
            status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_DEVICE_REQUEST);
        }
        else if ( (NdefMap->CompletionRoutine->CompletionRoutine == NULL) || (NdefMap->CompletionRoutine->Context == NULL ))
        {
            status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_PARAMETER);
        }
        else
        {
            /*
            * 1.Changed
            *   CardInfo106 Replace with the ReaderA_Info.
            */

            sak = NdefMap->psRemoteDevInfo->RemoteDevInfo.Iso14443A_Info.Sak;

            /*
            * 3. Changed
            *    Description: Opmode replace with RemDevType.
            */


            switch ( NdefMap->psRemoteDevInfo->RemDevType )
            {
#ifndef PH_FRINFC_MAP_ISO15693_DISABLED
            case phHal_eISO15693_PICC:
            {
                status = phFriNfc_ISO15693_ChkNdef (NdefMap);
                break;
            }
#else /* #ifndef PH_FRINFC_MAP_ISO15693_DISABLED */
            status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, 
                                NFCSTATUS_INVALID_REMOTE_DEVICE);
#endif /* #ifndef PH_FRINFC_MAP_ISO15693_DISABLED */

            case phHal_eMifare_PICC:
            case phHal_eISO14443_3A_PICC:
                /*  Remote device is Mifare card . Check for Mifare
                NDEF compliancy */
                if(0x00 == sak)
                {
                    /*  The SAK/Sel_Res says the card is of the type
                    Mifare UL */
#ifndef PH_FRINFC_MAP_MIFAREUL_DISABLED
                    status = phFriNfc_MifareUL_ChkNdef( NdefMap);
#else   /* PH_FRINFC_MAP_MIFAREUL_DISABLED*/
                    status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                        NFCSTATUS_INVALID_REMOTE_DEVICE);
#endif  /* PH_FRINFC_MAP_MIFAREUL_DISABLED*/
                }
                else if ((0x08 == (sak & 0x18)) ||
                        (0x18 == (sak & 0x18)) ||
                        (0x01 == sak))
                {
                    /*  The SAK/Sel_Res says the card is of the type
                    Mifare Standard */
#ifndef PH_FRINFC_MAP_MIFARESTD_DISABLED
                    status = phFriNfc_MifareStdMap_ChkNdef( NdefMap);
#else   /* PH_FRINFC_MAP_MIFARESTD_DISABLED*/
                    status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                        NFCSTATUS_INVALID_REMOTE_DEVICE);
#endif  /* PH_FRINFC_MAP_MIFARESTD_DISABLED*/
                }
                else
                {
                    /*  Invalid Mifare UL card, as the remote device
                    info - opmode says its a Mifare UL card but,
                    The SAK/Sel_Res is wrong */
                    status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                        NFCSTATUS_INVALID_REMOTE_DEVICE);
                }
                break;
            case phHal_eISO14443_B_PICC:
                {
                    status = phFriNfc_Desfire_ChkNdef(NdefMap);
                }
                break;
            case  phHal_eISO14443_A_PICC :
                /*  Remote device is Desfire card . Check for Desfire
                NDEF compliancy */
                if(0x20 == (sak & 0x20))
                {
                    /*  The SAK/Sel_Res says the card is of the type
                    ISO14443_4A */
#ifndef PH_FRINFC_MAP_DESFIRE_DISABLED
                    status = phFriNfc_Desfire_ChkNdef(NdefMap);
#else   /* PH_FRINFC_MAP_DESFIRE_DISABLED*/
                    status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                        NFCSTATUS_INVALID_REMOTE_DEVICE);
#endif  /* PH_FRINFC_MAP_DESFIRE_DISABLED*/
                }
                else
                {
                    /*  Invalid Desfire card, as the remote device
                    info - opmode says its a desfire card but,
                    The SAK/Sel_Res is wrong */
                    status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                        NFCSTATUS_INVALID_REMOTE_DEVICE);
                }
                break;

            case phHal_eFelica_PICC:

                /*Set the OpMode Type Flag*/
#ifndef PH_FRINFC_MAP_FELICA_DISABLED
#ifndef PH_HAL4_ENABLE
                NdefMap->OpModeType[0] = phHal_eOpModesFelica212;
                NdefMap->OpModeType[1] = phHal_eOpModesArrayTerminator;
#endif /* #ifndef PH_HAL4_ENABLE */
                status = phFriNfc_Felica_ChkNdef(NdefMap);
#else   /* PH_FRINFC_MAP_FELICA_DISABLED*/
                status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                    NFCSTATUS_INVALID_REMOTE_DEVICE);
#endif  /* PH_FRINFC_MAP_FELICA_DISABLED*/

                break;

#ifndef PH_HAL4_ENABLE
#ifndef PH_FRINFC_MAP_FELICA_DISABLED
            case phHal_eFelica424:
                /*Set the OpMode Ttype Flag*/
                NdefMap->OpModeType[0] = phHal_eOpModesFelica424;
                NdefMap->OpModeType[1] = phHal_eOpModesArrayTerminator;
                status = phFriNfc_Felica_ChkNdef(NdefMap);
#else   /* PH_FRINFC_MAP_FELICA_DISABLED*/
                status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                    NFCSTATUS_INVALID_REMOTE_DEVICE);
#endif  /* PH_FRINFC_MAP_FELICA_DISABLED*/
                break;
#endif

            case phHal_eJewel_PICC :
                /*  Remote device is Topaz card . Check for Topaz
                NDEF compliancy */
#ifdef PH_HAL4_ENABLE
#ifndef PH_FRINFC_MAP_TOPAZ_DISABLED
            /* Decide on the Header bytes to know the
                   Type of the Topaz card.Card could be Static or
                   Dynamic type. These are of type NFFC-NDEF Data Application*/
                if ( NdefMap->psRemoteDevInfo->RemoteDevInfo.Jewel_Info.HeaderRom0
                                == PH_FRINFC_TOPAZ_HEADROM0_VAL)
                {

                        status = phFriNfc_TopazMap_ChkNdef(NdefMap);
                }
#ifndef PH_FRINFC_MAP_TOPAZ_DYNAMIC_DISABLED
                else if( NdefMap->psRemoteDevInfo->RemoteDevInfo.Jewel_Info.HeaderRom0
                                == PH_FRINFC_TOPAZ_DYNAMIC_HEADROM0_VAL)
                {

                    status = phFriNfc_TopazDynamicMap_ChkNdef(NdefMap);
                }
#endif
                else
                {

                    status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                           NFCSTATUS_INVALID_REMOTE_DEVICE);

                }
#endif


#else
                if(0xC2 == sak)
                {
                    /*  The SAK/Sel_Res says the card is of the type
                    ISO14443_4A */
#ifndef PH_FRINFC_MAP_TOPAZ_DISABLED
                    status = phFriNfc_TopazMap_ChkNdef(NdefMap);
#else   /* PH_FRINFC_MAP_TOPAZ_DISABLED*/
                    status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                        NFCSTATUS_INVALID_REMOTE_DEVICE);
#endif  /* PH_FRINFC_MAP_TOPAZ_DISABLED*/
                }
                else
                {
                    /*  Invalid Topaz card, as the remote device
                    info - opmode says its a desfire card but,
                    The SAK/Sel_Res is wrong */
                    status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                        NFCSTATUS_INVALID_REMOTE_DEVICE);
                }
#endif
                break;

#ifdef PHFRINFC_OVRHAL_MOCKUP
            case phHal_eOpModesMockup :
                /*Set the OpMode Ttype Flag*/
                NdefMap->OpModeType[0] = phHal_eOpModesMockup;
                NdefMap->OpModeType[1] = phHal_eOpModesArrayTerminator;
                status = phFriNfc_Mockup_ChkNdef(NdefMap);
                break;
#endif  /* PHFRINFC_OVRHAL_MOCKUP */

            default :
                /*  Remote device is not recognised.
                Probably not NDEF compliant */
                status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                    NFCSTATUS_INVALID_REMOTE_DEVICE);
                break;
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
* This is a main Ndef Map Process api.Internally,depending upon the different
* CardTypes,respective mifare/desfire process functions are called.
*
*/

void phFriNfc_NdefMap_Process(  void        *Context,
                              NFCSTATUS   Status)
{

    if ( Context != NULL )
    {
        phFriNfc_NdefMap_t  *NdefMap = (phFriNfc_NdefMap_t *)Context;
        /*
        * 4 Changed
        *   Description: Opmode replace with RevDevTyp.
        */

        switch ( NdefMap->psRemoteDevInfo->RemDevType )
        {
        case  phHal_eMifare_PICC :
        case phHal_eISO14443_3A_PICC:

            if((NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD) ||
                (NdefMap->CardType == PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD))
            {
#ifndef PH_FRINFC_MAP_MIFARESTD_DISABLED
                /*  Remote device is Mifare Standard card */
                phFriNfc_MifareStdMap_Process(NdefMap,Status);
#else   /* PH_FRINFC_MAP_MIFARESTD_DISABLED*/
                Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                    NFCSTATUS_INVALID_REMOTE_DEVICE);
#endif  /* PH_FRINFC_MAP_MIFARESTD_DISABLED*/
            }
            else
            {
#ifndef PH_FRINFC_MAP_MIFAREUL_DISABLED
                /*  Remote device is Mifare UL card */
                phFriNfc_MifareUL_Process(NdefMap,Status);
#else   /* PH_FRINFC_MAP_MIFAREUL_DISABLED*/
                Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                    NFCSTATUS_INVALID_REMOTE_DEVICE);
#endif  /* PH_FRINFC_MAP_MIFAREUL_DISABLED*/
            }
            break;

        case phHal_eISO14443_A_PICC :
#ifndef PH_FRINFC_MAP_DESFIRE_DISABLED
            /*  Remote device is Desfire card */
            phFriNfc_Desfire_Process(NdefMap, Status);
#else   /* PH_FRINFC_MAP_DESFIRE_DISABLED*/
            Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                NFCSTATUS_INVALID_REMOTE_DEVICE);
#endif  /* PH_FRINFC_MAP_DESFIRE_DISABLED*/
            break;
        case phHal_eISO14443_B_PICC:
            /*  Remote device is Desfire card */
            phFriNfc_Desfire_Process(NdefMap, Status);
            break;

        case phHal_eFelica_PICC :
#ifndef PH_FRINFC_MAP_FELICA_DISABLED
            /*  Remote device is Felica Smart card */
            phFriNfc_Felica_Process(NdefMap, Status);
#else   /* PH_FRINFC_MAP_FELICA_DISABLED*/
            Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                NFCSTATUS_INVALID_REMOTE_DEVICE);
#endif  /* PH_FRINFC_MAP_FELICA_DISABLED*/
            break;

        case phHal_eJewel_PICC:
#ifndef PH_FRINFC_MAP_TOPAZ_DISABLED
            if ( NdefMap->CardType == PH_FRINFC_NDEFMAP_TOPAZ_CARD )
            {
                /*  Remote device is Topaz Smart card */
                phFriNfc_TopazMap_Process(NdefMap, Status);
            }
#ifndef PH_FRINFC_MAP_TOPAZ_DYNAMIC_DISABLED
            else if ( NdefMap->CardType == PH_FRINFC_NDEFMAP_TOPAZ_DYNAMIC_CARD )
            {
                /*  Remote device is Topaz Smart card */
                phFriNfc_TopazDynamicMap_Process(NdefMap, Status);
            }
            else
            {
                Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                NFCSTATUS_INVALID_REMOTE_DEVICE);

            }
            break;
#endif  /* PH_FRINFC_MAP_TOPAZ_DYNAMIC_DISABLED*/
#endif


#ifdef PHFRINFC_OVRHAL_MOCKUP
        case phHal_eOpModesMockup:
            /*  Remote device is Desfire card */
            phFriNfc_Mockup_Process(NdefMap, Status);
            break;
#endif  /* PHFRINFC_OVRHAL_MOCKUP*/
        default :
            /*  Remote device opmode not recognised.
            Probably not NDEF compliant */
            Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                NFCSTATUS_INVALID_REMOTE_DEVICE);
            /* set the state back to the Reset_Init state*/
            NdefMap->State =  PH_FRINFC_NDEFMAP_STATE_RESET_INIT;

            /* set the completion routine*/
            NdefMap->CompletionRoutine[PH_FRINFC_NDEFMAP_CR_INVALID_OPE].
                CompletionRoutine(NdefMap->CompletionRoutine->Context, Status);
            break;
        }
    }
    else
    {
        Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,\
            NFCSTATUS_INVALID_PARAMETER);
        /* The control should not come here. As Context itself is NULL ,
        Can't call the CR*/
    }
}

#if 0

NFCSTATUS phFriNfc_ChkAndParseTLV(phFriNfc_NdefMap_t    *NdefMap)
{
    NFCSTATUS status = NFCSTATUS_PENDING;

    switch ( NdefMap->CardType )
    {
#ifndef PH_FRINFC_MAP_MIFAREUL_DISABLED
            case  PH_FRINFC_NDEFMAP_MIFARE_UL_CARD :


                break;
#endif /* PH_FRINFC_MAP_MIFAREUL_DISABLED */

#ifndef PH_FRINFC_MAP_DESFIRE_DISABLED
            case PH_FRINFC_NDEFMAP_ISO14443_4A_CARD :
                status = phFriNfc_Desf_ChkAndParseTLV(NdefMap);
                return (status);

                break;
#endif  /* PH_FRINFC_MAP_DESFIRE_DISABLED */

#ifndef PH_FRINFC_MAP_MIFARESTD_DISABLED
            case  PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD :
            case  PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD :

                break;
#endif  /* PH_FRINFC_MAP_MIFARESTD_DISABLED */

#ifndef PH_FRINFC_MAP_FELICA_DISABLED
            case PH_FRINFC_NDEFMAP_FELICA_SMART_CARD :
                ;
                break;
#endif  /* PH_FRINFC_MAP_FELICA_DISABLED */

            default :
                /*  Unknown card type. Return error */
                status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,\
                    NFCSTATUS_INVALID_REMOTE_DEVICE);

                break;
    }

    return ( status);
}
#endif


#ifndef PH_FRINFC_MAP_DESFIRE_DISABLED
static void phFriNfc_DesfCapCont_HReset(phFriNfc_NdefMap_t *NdefMap)
{
    /* Initialise/reset the desfire capability contatiner structure variables*/
    NdefMap->DesfireCapContainer.DesfVersion = 0;
    NdefMap->DesfireCapContainer.NdefMsgFid  = 0;
    NdefMap->DesfireCapContainer.NdefFileSize = 0;
    NdefMap->DesfireCapContainer.MaxCmdSize  = 0;
    NdefMap->DesfireCapContainer.MaxRespSize = 0;
    NdefMap->DesfireCapContainer.ReadAccess  = 0;
    NdefMap->DesfireCapContainer.WriteAccess = 0;
}
#endif  /* PH_FRINFC_MAP_DESFIRE_DISABLED */

#ifndef PH_FRINFC_MAP_FELICA_DISABLED
static void phFriNfc_Felica_HReset(phFriNfc_NdefMap_t *NdefMap)
{
    uint8_t index=0;

    /* Initialise/reset the different felica structure variables*/
    /* Reset all the felica Basic staruture variables*/
    NdefMap->Felica.CurBlockNo = 0;

    for(index = 0;index<PH_FRINFC_NDEFMAP_FELICA_BLOCK_SIZE;index++)
    {
        NdefMap->Felica.Rd_BytesToCopyBuff[index] = 0;
        NdefMap->Felica.Wr_RemainedBytesBuff[index] = 0;
    }
    NdefMap->Felica.Rd_NoBytesToCopy = 0;
    NdefMap->Felica.Wr_BytesRemained = 0;


    /* Reset all the felica attribute information staruture variables*/
    for(index = 0;index<PH_FRINFC_NDEFMAP_FELICA_ATTR_NDEF_DATA_LEN;index++)
    {
        NdefMap->FelicaAttrInfo.LenBytes[index] = 0;
    }

    NdefMap->FelicaAttrInfo.Nmaxb = 0;
    NdefMap->FelicaAttrInfo.Nbr = 0;
    NdefMap->FelicaAttrInfo.Nbw= 0;
    NdefMap->FelicaAttrInfo.RdWrFlag = 0;
    NdefMap->FelicaAttrInfo.WriteFlag = 0;
    NdefMap->Felica.CurrBytesRead=0;

    /* Reset all the felica manufacture details staruture variables*/
    for(index = 0;index<PH_FRINFC_NDEFMAP_FELICA_MANUF_ID_DATA_LEN;index++)
    {
        NdefMap->FelicaManufDetails.ManufID[index] = 0;
        NdefMap->FelicaManufDetails.ManufParameter[index] = 0;
    }
    NdefMap->Felica.NoBlocksWritten=0;
}
#endif  /* PH_FRINFC_MAP_FELICA_DISABLED */

NFCSTATUS phFriNfc_NdefMap_EraseNdef(phFriNfc_NdefMap_t *NdefMap)
{
    NFCSTATUS   status = NFCSTATUS_PENDING;

    static uint8_t     PktData[3] = PH_FRINFC_NDEFMAP_EMPTY_NDEF_MSG;
    uint8_t     MemOffset = PH_FRINFC_NDEFMAP_SEEK_BEGIN;
    static uint32_t    PacketDataLength = sizeof(PktData);

    if (NdefMap == NULL )
    {
        /*  Invalid input parameter error   */
        status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        switch ( NdefMap->CardType)
        {
#ifdef PHFRINFC_OVRHAL_MOCKUP
            case PH_FRINFC_NDEFMAP_MOCKUP_CARD :
#endif  /* PHFRINFC_OVRHAL_MOCKUP */
            case  PH_FRINFC_NDEFMAP_MIFARE_UL_CARD :
            case  PH_FRINFC_NDEFMAP_ISO14443_4A_CARD :
#ifdef DESFIRE_EV1
            case  PH_FRINFC_NDEFMAP_ISO14443_4A_CARD_EV1 :
#endif /* #ifdef DESFIRE_EV1 */
            case  PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD :
            case  PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD :
#ifndef PH_FRINFC_MAP_TOPAZ_DISABLED
            case  PH_FRINFC_NDEFMAP_TOPAZ_CARD :
#ifndef PH_FRINFC_MAP_TOPAZ_DYNAMIC_DISABLED
            case  PH_FRINFC_NDEFMAP_TOPAZ_DYNAMIC_CARD :
#endif
#ifndef PH_FRINFC_MAP_ISO15693_DISABLED
            case PH_FRINFC_NDEFMAP_ISO15693_CARD:
#endif
#endif
                /*  Mifare card selected. Call Mifare Write */
                status =  phFriNfc_NdefMap_WrNdef( NdefMap,
                    PktData,
                    &PacketDataLength,
                    MemOffset);
                break;

            case PH_FRINFC_NDEFMAP_FELICA_SMART_CARD :

#ifndef PH_FRINFC_MAP_FELICA_DISABLED
                /*  Felica card selected. Call to write EMPTY NDEF Msg */
                status =  phFriNfc_Felica_EraseNdef( NdefMap );
#else   /* PH_FRINFC_MAP_FELICA_DISABLED*/
                status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                    NFCSTATUS_INVALID_REMOTE_DEVICE);
#endif  /* PH_FRINFC_MAP_FELICA_DISABLED*/

                break;
            default :
                /*  Unknown card type. Return error */
                status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                    NFCSTATUS_INVALID_REMOTE_DEVICE);
                break;
        }
    }
    return status;
}
/*  Harsha: Fix for the mantis entry 0000420: NDEF_MAP: Size of NDEF data:
no abstracted way for user to know how many bytes to read/write  */

/*!
* \brief Helper API, exposed to the user to enable him to know the size
*        of the NDEF data that he can write in to the card.
*/
NFCSTATUS phFriNfc_NdefMap_GetContainerSize(const phFriNfc_NdefMap_t *NdefMap,uint32_t *maxSize, uint32_t *actualSize)
{
    NFCSTATUS   result = NFCSTATUS_SUCCESS;
    uint8_t     sect_index = 0;
    uint8_t     actual_sect_index = 0;
    uint8_t     count_index = 0;

    if( (NdefMap == NULL) || (maxSize == NULL) || (actualSize == NULL))
    {
        /*  Invalid input parameter error   */
        result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        /*  Which card ? */
        switch(NdefMap->CardType)
        {
#ifndef PH_FRINFC_MAP_MIFAREUL_DISABLED
            case  PH_FRINFC_NDEFMAP_MIFARE_UL_CARD :
                /*  Mifare UL card */
                /*  The integration needs to ensure that the checkNdef
                function has been called before calling this function,
                otherwise NdefMap->CardMemSize will be 0 */
                *maxSize = NdefMap->MifareULContainer.RemainingSize;
                /* In Mifare UL card, the actual size is the length field
                value of the TLV */
                *actualSize = NdefMap->TLVStruct.ActualSize;
                break;
#endif  /* PH_FRINFC_MAP_MIFAREUL_DISABLED */

#ifndef PH_FRINFC_MAP_DESFIRE_DISABLED
            case PH_FRINFC_NDEFMAP_ISO14443_4A_CARD :
#ifdef DESFIRE_EV1
            case PH_FRINFC_NDEFMAP_ISO14443_4A_CARD_EV1 :
#endif /* #ifdef DESFIRE_EV1 */
                /*  Desfire card */
                /*  The integration needs to ensure that the checkNdef
                function has been called before calling this function,
                otherwise NdefMap->DesfireCapContainer.NdefFileSize
                will be 0 */
                /* -2 bytes represents the size field bytes*/
                *maxSize = NdefMap->DesfireCapContainer.NdefFileSize - 2;
                /* In Desfire card, the actual size cant be calculated so
                the actual size is given as 0xFFFFFFFF */
                *actualSize = NdefMap->DesfireCapContainer.NdefDataLen;
                break;
#endif  /* PH_FRINFC_MAP_DESFIRE_DISABLED */

#ifndef PH_FRINFC_MAP_TOPAZ_DISABLED
            case PH_FRINFC_NDEFMAP_TOPAZ_CARD :
                /*  Topaz card */
                /*  The integration needs to ensure that the checkNdef
                function has been called before calling this function,
                otherwise NdefMap->CardMemSize will be 0 */
                *maxSize = NdefMap->TopazContainer.RemainingSize;
                /* In Topaz card, the actual size is the length field value of the
                TLV */
                *actualSize = NdefMap->TLVStruct.BytesRemainLinTLV;
                break;
#ifndef PH_FRINFC_MAP_TOPAZ_DYNAMIC_DISABLED
            case PH_FRINFC_NDEFMAP_TOPAZ_DYNAMIC_CARD :
                /*  Topaz 512 card */
                /*  The integration needs to ensure that the checkNdef
                function has been called before calling this function,
                otherwise NdefMap->CardMemSize will be 0 */
                *maxSize = NdefMap->TopazContainer.NDEFRWSize;
                /* In Topaz card, the actual size is the length field value of the
                TLV */
                *actualSize = NdefMap->TopazContainer.ActualNDEFMsgSize;
                break;

#endif  /* PH_FRINFC_MAP_TOPAZ_DISABLED */
#endif  /* PH_FRINFC_MAP_TOPAZ_DYNAMIC_DISABLED */
#ifndef PH_FRINFC_MAP_MIFARESTD_DISABLED
            case  PH_FRINFC_NDEFMAP_MIFARE_STD_1K_CARD :
            case  PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD :
                /*  Mifare std card */

                /*  Max size is the number of NDEF compliant blocks in the card
                multiplied by 16 bytes */
#ifndef PH_HAL4_ENABLE

                *maxSize = NdefMap->StdMifareContainer.remainingSize;

#else /* #ifndef PH_HAL4_ENABLE */

                while ((PH_FRINFC_MIFARESTD_NDEF_COMP ==
                        NdefMap->StdMifareContainer.aid[count_index]) &&
                        (count_index <
                        PH_FRINFC_NDEFMAP_MIFARESTD_TOTALNO_BLK))
                {
                    actual_sect_index++;
                    count_index++;
                }
                /* Total number of sectors in 1k = 16 (0 to 15, MAD sector number = 0)
                    Total number of sectors in 4k = 40 (0 to 39,
                        MAD sector number = 0 and 16, After block number 31, each sector
                        has 16 blocks)
                    Every last block of the sector is the sector header, So the blocks
                    that can be read or written in each sector is always
                        (number of blocks in each sector - 1)
                    No of blocks in the one sector till the sector number 0 to 31
                        (total 32 sectors) =
                        4 blocks, So blocks that can be read/write = (4 - 1 = 3 blocks)
                    No of blocks in the one sector after the sector number 31 to 39 =
                        16 blocks, So blocks that can be read/write = (16 - 1 = 15 blocks)
                    Each block has 16 bytes
                    To calculate number of bytes in the sector, depending on the number
                    of blocks multiplied by 16
                */
                if (PH_FRINFC_NDEFMAP_MIFARE_STD_4K_CARD ==
                    NdefMap->CardType)
                {
                    if (actual_sect_index > 32)
                    {
                        sect_index = (actual_sect_index - 32);
                        /* Here, 30 is used because block number 0 and 16 are MAD blocks
                        which cannot be used for reading and writing data
                        3 and 15 are blocks in each sector which can be read/write
                        3 indicates the sector is in between (0 and 31)
                        15 indicates the sector is in between (32 to 39)
                        16 is always number of bytes in each block
                        4 is used because each NDEF write has to write using the
                            TLV format and T & L takes 4 bytes length and V is the
                            input data
                        */
                        *maxSize = (((30 * (16 * 3)) + (sect_index * (16 * 15))) - 4);
                    }
                    else if (actual_sect_index <= 16)
                    {
                        *maxSize = (((actual_sect_index - 1) * (16 * 3)) - 4);
                    }
                    else
                    {
                        *maxSize = (((actual_sect_index - 2)  * (16 * 3)) - 4);
                    }
                }
                else
                {
                    /* Here, 16 is always number of bytes in each block
                        3 indicates the sector is in between (0 and 31) */
                    if (actual_sect_index > NdefMap->StdMifareContainer.SectorIndex)
                    {
                        actual_sect_index = NdefMap->StdMifareContainer.SectorIndex;
                    }
                    *maxSize = (((actual_sect_index - 1) * (16 * 3)) - 4);
                }

#endif /* #ifndef PH_HAL4_ENABLE */
                *actualSize = NdefMap->TLVStruct.BytesRemainLinTLV;

                break;
#endif  /* PH_FRINFC_MAP_MIFARESTD_DISABLED */

#ifndef PH_FRINFC_MAP_FELICA_DISABLED
            case PH_FRINFC_NDEFMAP_FELICA_SMART_CARD :
                /*  Felica card */

                *maxSize = NdefMap->FelicaAttrInfo.Nmaxb * 0x10;

                /* In Felica Card, actual size is calculated using the Length Bytes */
                *actualSize = NdefMap->FelicaAttrInfo.LenBytes[0];
                *actualSize = *actualSize << 16;
                *actualSize += NdefMap->FelicaAttrInfo.LenBytes[1];
                *actualSize = *actualSize << 8;
                *actualSize += NdefMap->FelicaAttrInfo.LenBytes[2];
                break;
#endif  /* PH_FRINFC_MAP_FELICA_DISABLED */

#ifndef PH_FRINFC_MAP_ISO15693_DISABLED
            case PH_FRINFC_NDEFMAP_ISO15693_CARD:
            {
#if 1
                uint16_t                    block_no = 0;
                uint8_t                     byte_no = 0;

                block_no = (uint16_t)
                    ISO15693_GET_VALUE_FIELD_BLOCK_NO (
                        NdefMap->ISO15693Container.ndef_tlv_type_blk, 
                        NdefMap->ISO15693Container.ndef_tlv_type_byte, 
                        NdefMap->ISO15693Container.actual_ndef_size);
                byte_no = (uint8_t)
                    ISO15693_GET_VALUE_FIELD_BYTE_NO (
                        NdefMap->ISO15693Container.ndef_tlv_type_blk, 
                        NdefMap->ISO15693Container.ndef_tlv_type_byte, 
                        NdefMap->ISO15693Container.actual_ndef_size);

                *maxSize = (NdefMap->ISO15693Container.max_data_size - 
                            ((block_no * ISO15693_BYTES_PER_BLOCK) + byte_no));
#else /* #if 1 */
                /* 2 is used to exclude the T and L part of the TLV */
                *maxSize = (NdefMap->ISO15693Container.max_data_size
                            - ISO15693_BYTES_PER_BLOCK - 2);
#endif /* #if 1 */
                *actualSize = NdefMap->ISO15693Container.actual_ndef_size;
                break;
            }
#endif

#ifdef PHFRINFC_OVRHAL_MOCKUP
            case PH_FRINFC_NDEFMAP_MOCKUP_CARD :
                *maxSize = 0xFFFFFFFF;
                /* In Desfire card, the actual size cant be calculated so
                the actual size is given as 0xFFFFFFFF */
                *actualSize = 0xFFFFFFFF;
                break;
#endif  /* PHFRINFC_OVRHAL_MOCKUP */

            default :
                /*  Unknown card type. Return error */
                result = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,\
                    NFCSTATUS_INVALID_REMOTE_DEVICE);
                break;
        }
    }
    return(result);
}

#ifdef PHFRINFC_OVRHAL_MOCKUP
NFCSTATUS phFriNfc_NdefMap_MockupCardSetter(phFriNfc_NdefMap_t *NdefMap,
                                            uint8_t *NdefData,
                                            uint32_t NdefActualSize,
                                            uint32_t NdefMaxSize,
                                            uint32_t CardSize)
{
    NFCSTATUS Status = NFCSTATUS_SUCCESS;
    // First check all parameters
    if((NdefData != NULL) && (NdefMap != NULL))
    {
        // OK we can set
        NdefMap->MochupContainer.NdefData       = NdefData;
        NdefMap->MochupContainer.NdefActualSize = NdefActualSize;
        NdefMap->MochupContainer.NdefMaxSize    = NdefMaxSize;
        NdefMap->MochupContainer.CardSize       = CardSize;
        NdefMap->MochupContainer.CurrentBlock   = 0;

    } else
    {
        Status = PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP, NFCSTATUS_INVALID_PARAMETER);
    }
    return Status;
}

NFCSTATUS phFriNfc_NdefMap_MockupNDefModeEn(uint8_t  *pNdefCompliancy, uint8_t  *pCardType, uint8_t Enable)
{
    *pNdefCompliancy = Enable;
    *pCardType = PH_FRINFC_NDEFMAP_MOCKUP_CARD;
    return NFCSTATUS_SUCCESS;
}
#endif  /* PHFRINFC_OVRHAL_MOCKUP */








