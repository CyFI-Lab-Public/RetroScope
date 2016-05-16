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
* \file  phHal4Nfc_Reader.c
* \brief Hal4Nfc Reader source.
*
* Project: NFC-FRI 1.1
*
* $Date: Mon May 31 11:43:43 2010 $
* $Author: ing07385 $
* $Revision: 1.120 $
* $Aliases: NFC_FRI1.1_WK1023_R35_1 $
*
*/

/* ---------------------------Include files ------------------------------------*/
#include <phHal4Nfc.h>
#include <phHal4Nfc_Internal.h>
#include <phOsalNfc.h>
#include <phHciNfc.h>
#include <phOsalNfc_Timer.h>
#include <phNfcConfig.h>


/* ------------------------------- Macros ------------------------------------*/
#define PH_HAL4NFC_CMD_LENGTH      PHHAL_MAX_DATASIZE+12/**< Cmd length used 
                                                              for Transceive*/
#define PH_HAL4NFC_MAX_TRCV_LEN                     4096 /**<Only a max of 1KB
                                                              can be sent at 
                                                              a time*/
#define PH_HAL4NFC_FLAG_0                              0
    
#define PH_HAL4NFC_FLAG_1                              1

#define PH_HAL4NFC_SEL_SECTOR1_BYTE0                0xC2
#define PH_HAL4NFC_SEL_SECTOR1_BYTE1                0xFF

#define PH_HAL4NFC_SEL_SECTOR2_BYTE0                0x02
#define PH_HAL4NFC_SEL_SECTOR2_BYTE_RESERVED        0x00

phHal4Nfc_Hal4Ctxt_t *gpHal4Ctxt;
                                                              
/* --------------------Structures and enumerations --------------------------*/

static void phHal4Nfc_Iso_3A_Transceive(
                        phHal_sTransceiveInfo_t   *psTransceiveInfo,
                        phHal4Nfc_Hal4Ctxt_t      *Hal4Ctxt
                        );

static void phHal4Nfc_MifareTransceive(
                        phHal_sTransceiveInfo_t   *psTransceiveInfo,
                        phHal_sRemoteDevInformation_t  *psRemoteDevInfo,
                        phHal4Nfc_Hal4Ctxt_t      *Hal4Ctxt
                        );

/*Allows to connect to a single, specific, already known Remote Device.*/
NFCSTATUS phHal4Nfc_Connect(                            
                            phHal_sHwReference_t          *psHwReference,
                            phHal_sRemoteDevInformation_t *psRemoteDevInfo,                           
                            pphHal4Nfc_ConnectCallback_t   pNotifyConnectCb,
                            void                          *pContext
                            )
{
    NFCSTATUS RetStatus = NFCSTATUS_SUCCESS;
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL;
    uint8_t RemoteDevCount = 0;
    int32_t MemCmpRet = 0;
    /*NULL chks*/
    if(NULL == psHwReference 
        || NULL == pNotifyConnectCb 
        || NULL == psRemoteDevInfo)
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_INVALID_PARAMETER);
    }
    /*Check initialised state*/
    else if((NULL == psHwReference->hal_context)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4CurrentState 
                                               < eHal4StateOpenAndReady)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4NextState 
                                               == eHal4StateClosed))
    {
        RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_NOT_INITIALISED);     
    }
    else if ((psRemoteDevInfo == 
             ((phHal4Nfc_Hal4Ctxt_t *)psHwReference->hal_context)->
                sTgtConnectInfo.psConnectedDevice)
             &&((phHal_eNfcIP1_Target == psRemoteDevInfo->RemDevType)
                ||(phHal_eJewel_PICC == psRemoteDevInfo->RemDevType)))
    {
        RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_FEATURE_NOT_SUPPORTED);     
    }   
    else
    {
        /*Get Hal ctxt from hardware reference*/
        Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)psHwReference->hal_context;
        /*Register upper layer context*/
        Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt = pContext;
        /*Register upper layer callback*/
        Hal4Ctxt->sTgtConnectInfo.pUpperConnectCb = pNotifyConnectCb;  
        /*Allow Connect only if no other remote device is connected*/
        if((eHal4StateTargetDiscovered == Hal4Ctxt->Hal4CurrentState)
            && (NULL == Hal4Ctxt->sTgtConnectInfo.psConnectedDevice))
        {
            RemoteDevCount = Hal4Ctxt->psADDCtxtInfo->nbr_of_devices;
            while(0 != RemoteDevCount)
            {
                RemoteDevCount--;
                /*Check if handle provided by upper layer matches with any 
                  remote device in the list*/
                if(psRemoteDevInfo   
                    == (Hal4Ctxt->rem_dev_list[RemoteDevCount]))
                {
                    
                    Hal4Ctxt->sTgtConnectInfo.psConnectedDevice 
                                  = Hal4Ctxt->rem_dev_list[RemoteDevCount];
                    break;
                }
            }/*End of while*/

            if(NULL == Hal4Ctxt->sTgtConnectInfo.psConnectedDevice)
            {
                /*No matching device handle in list*/
                RetStatus = PHNFCSTVAL(CID_NFC_HAL ,
                                        NFCSTATUS_INVALID_REMOTE_DEVICE);       
            }
            else
            {
                MemCmpRet = phOsalNfc_MemCompare(
                    (void *)&(psRemoteDevInfo->RemoteDevInfo), 
                    (void *)&(Hal4Ctxt->rem_dev_list[Hal4Ctxt
                    ->psADDCtxtInfo->nbr_of_devices - 1]->RemoteDevInfo),
                    sizeof(phHal_uRemoteDevInfo_t));

                /*If device is already selected issue connect from here*/
                if(0 == MemCmpRet)
                {
                    RetStatus = phHciNfc_Connect(Hal4Ctxt->psHciHandle,
                        (void *)psHwReference,
                        Hal4Ctxt->rem_dev_list[RemoteDevCount]);
                    if(NFCSTATUS_PENDING == RetStatus)
                    {
                        Hal4Ctxt->Hal4NextState = eHal4StateTargetConnected;
                    }

                }
                else/*Select the matching device to connect to*/
                {
                    RetStatus = phHciNfc_Reactivate (
                        Hal4Ctxt->psHciHandle,
                        (void *)psHwReference,
                        Hal4Ctxt->rem_dev_list[RemoteDevCount]
                        );
                    Hal4Ctxt->Hal4NextState = eHal4StateTargetActivate;
                }
                if(NFCSTATUS_PENDING != RetStatus)
                {
                    /*Rollback state*/
                    Hal4Ctxt->Hal4CurrentState = eHal4StateOpenAndReady;
                    Hal4Ctxt->sTgtConnectInfo.psConnectedDevice =  NULL;
                    Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt = NULL;
                    Hal4Ctxt->sTgtConnectInfo.pUpperConnectCb = NULL;
                }
            }
        }
        /*Issue Reconnect*/
        else if(psRemoteDevInfo == 
                    Hal4Ctxt->sTgtConnectInfo.psConnectedDevice)
        {
            RetStatus = phHciNfc_Reactivate (
                Hal4Ctxt->psHciHandle,
                (void *)psHwReference,
                psRemoteDevInfo
                );
                Hal4Ctxt->Hal4NextState = eHal4StateTargetActivate;
        }
#ifdef RECONNECT_SUPPORT
        else if (psRemoteDevInfo != 
                    Hal4Ctxt->sTgtConnectInfo.psConnectedDevice)
        {
            phHal_sRemoteDevInformation_t           *ps_store_connected_device =
                                                Hal4Ctxt->sTgtConnectInfo.psConnectedDevice;

            RemoteDevCount = Hal4Ctxt->psADDCtxtInfo->nbr_of_devices;

            while (0 != RemoteDevCount)
            {
                RemoteDevCount--;
                /*Check if handle provided by upper layer matches with any 
                  remote device in the list*/
                if(psRemoteDevInfo == (Hal4Ctxt->rem_dev_list[RemoteDevCount]))
                {
                    break;
                }
            }/*End of while*/

            if (ps_store_connected_device == 
                Hal4Ctxt->sTgtConnectInfo.psConnectedDevice)
            {
                RetStatus = phHciNfc_Reactivate (Hal4Ctxt->psHciHandle,
                                                (void *)psHwReference,
                                                psRemoteDevInfo);

                if (NFCSTATUS_PENDING == RetStatus)
                {
                    Hal4Ctxt->sTgtConnectInfo.psConnectedDevice = 
                                    Hal4Ctxt->rem_dev_list[RemoteDevCount];
                    Hal4Ctxt->Hal4NextState = eHal4StateTargetActivate;
                }
            }
        }
#endif /* #ifdef RECONNECT_SUPPORT */
        else if(NULL == Hal4Ctxt->sTgtConnectInfo.psConnectedDevice)
        {
            /*Wrong state to issue connect*/
            RetStatus = PHNFCSTVAL(CID_NFC_HAL,
                                    NFCSTATUS_INVALID_REMOTE_DEVICE);       
        }
        else/*No Target or already connected to device*/        
        {
            RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_FAILED);
        }

    }
    if(NFCSTATUS_PENDING != RetStatus)
    {
        Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt = NULL;
        Hal4Ctxt->sTgtConnectInfo.pUpperConnectCb = NULL;
    }
    return RetStatus;
}

/*For Ordering Transceive Info for ISO_3A type tags*/
static void phHal4Nfc_Iso_3A_Transceive(
                        phHal_sTransceiveInfo_t   *psTransceiveInfo,
                        phHal4Nfc_Hal4Ctxt_t      *Hal4Ctxt
                        )
{
    uint16_t i;
    uint16_t counter= 0;
    /* Mifare UL, Keep MIFARE RAW command as it is */
    Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.params.tag_info.cmd_type 
                    = (uint8_t)psTransceiveInfo->cmd.MfCmd;                        
    /* Set flags for Select Sector */
    if (psTransceiveInfo->sSendData.buffer[0] != phHal_eMifareWrite4)
    {
        if (Hal4Ctxt->SelectSectorFlag == PH_HAL4NFC_FLAG_0)
        {
            /* First Select Sector command */
            if ((psTransceiveInfo->sSendData.buffer[1] == PH_HAL4NFC_SEL_SECTOR1_BYTE0) &&
                (psTransceiveInfo->sSendData.buffer[2] == PH_HAL4NFC_SEL_SECTOR1_BYTE1))
            {
                Hal4Ctxt->SelectSectorFlag++;
                PHDBG_INFO("Inside 3ATrancv,first cmd, SelectSectorFlag is 1");
                for (i = 1; i < psTransceiveInfo->sSendData.length; i++)
                {
                    psTransceiveInfo->sSendData.buffer[i - 1] =
                        psTransceiveInfo->sSendData.buffer[i];
                }
                
                psTransceiveInfo->sSendData.length--;
            }
            else
            {
                PHDBG_INFO("Inside 3ATrancv,first cmd,setting SelectSectorFlag 0");
                Hal4Ctxt->SelectSectorFlag = 0;
            }
        }
        else if (Hal4Ctxt->SelectSectorFlag == PH_HAL4NFC_FLAG_1)
        {
            if ((psTransceiveInfo->sSendData.buffer[1] < PH_HAL4NFC_SEL_SECTOR2_BYTE0) &&
                (psTransceiveInfo->sSendData.buffer[2] == PH_HAL4NFC_SEL_SECTOR2_BYTE_RESERVED) &&
                (psTransceiveInfo->sSendData.buffer[3] == PH_HAL4NFC_SEL_SECTOR2_BYTE_RESERVED) &&
                (psTransceiveInfo->sSendData.buffer[4] == PH_HAL4NFC_SEL_SECTOR2_BYTE_RESERVED))
            {
                Hal4Ctxt->SelectSectorFlag++;
                PHDBG_INFO("Inside 3ATrancv,2nd cmd, SelectSectorFlag set to 2");
                for (i = 1; i < psTransceiveInfo->sSendData.length; i++)
                {
                    psTransceiveInfo->sSendData.buffer[i - 1] =
                        psTransceiveInfo->sSendData.buffer[i];
                }

                psTransceiveInfo->sSendData.length--;
            }
            else
            {
                PHDBG_INFO("Inside 3ATrancv,2nd cmd, SelectSectorFlag set to 0");
                Hal4Ctxt->SelectSectorFlag = 0;
            }
        }
        else
        {
            Hal4Ctxt->SelectSectorFlag = 0;
        }
    }
    else
    { 
        PHDBG_INFO("Inside 3ATrancv,Mifarewrite4");
        /* Convert MIFARE RAW to MIFARE CMD */
        if (psTransceiveInfo->cmd.MfCmd == phHal_eMifareRaw)
        {
            psTransceiveInfo->cmd.MfCmd = 
                (phHal_eMifareCmdList_t)psTransceiveInfo->sSendData.buffer[0];

            Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.params.tag_info.cmd_type =
                (uint8_t)psTransceiveInfo->cmd.MfCmd;                              

            Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.params.tag_info.addr =
                psTransceiveInfo->addr =
                psTransceiveInfo->sSendData.buffer[1];

            for (counter = 2; counter < psTransceiveInfo->sSendData.length;
                 counter++)
            {
                psTransceiveInfo->sSendData.buffer[counter - 2] = 
                    psTransceiveInfo->sSendData.buffer[counter];
            }
            PHDBG_INFO("Hal4:Inside 3A_Trcv() ,minus length by 4");
            psTransceiveInfo->sSendData.length = 
                psTransceiveInfo->sSendData.length - 4;
        }
        else
        {
            Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.params.tag_info.cmd_type 
                        = (uint8_t)psTransceiveInfo->cmd.MfCmd;
        }
    }
    return;
}

/*For Ordering Transceive Info for Mifare tags*/
static void phHal4Nfc_MifareTransceive(
                        phHal_sTransceiveInfo_t   *psTransceiveInfo,
                        phHal_sRemoteDevInformation_t  *psRemoteDevInfo,
                        phHal4Nfc_Hal4Ctxt_t      *Hal4Ctxt
                        )
{
    uint16_t counter;
    if (
#ifndef DISABLE_MIFARE_UL_WRITE_WORKAROUND
        phHal_eMifareWrite4 != psTransceiveInfo->sSendData.buffer[0]
#else
        1
#endif/*#ifndef DISABLE_MIFARE_UL_WRITE_WORKAROUND*/
        )

    {
        /* Mifare UL, Keep MIFARE RAW command as it is */
        Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.params.tag_info.cmd_type 
                        = (uint8_t)psTransceiveInfo->cmd.MfCmd;                        
        
    }
    else
    {   
        /* Convert MIFARE RAW to MIFARE CMD */
        if (psTransceiveInfo->cmd.MfCmd == phHal_eMifareRaw)
        {
            psTransceiveInfo->cmd.MfCmd = 
                (phHal_eMifareCmdList_t)psTransceiveInfo->sSendData.buffer[0];

            Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.params.tag_info.cmd_type =
                (uint8_t)psTransceiveInfo->cmd.MfCmd;                              

            Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.params.tag_info.addr =
                psTransceiveInfo->addr =
                psTransceiveInfo->sSendData.buffer[1];

            for (counter = 2; counter < psTransceiveInfo->sSendData.length;
                 counter++)
            {
                psTransceiveInfo->sSendData.buffer[counter - 2] = 
                    psTransceiveInfo->sSendData.buffer[counter];
            }
            PHDBG_INFO("Hal4:Inside MifareTrcv() ,minus length by 4");
            psTransceiveInfo->sSendData.length = 
                psTransceiveInfo->sSendData.length - 4;

        }
        else
        {
            Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.params.tag_info.cmd_type 
                        = (uint8_t)psTransceiveInfo->cmd.MfCmd;
        }
    }
    return;
}

/*  The phHal4Nfc_Transceive function allows the Initiator to send and receive 
 *  data to and from the Remote Device selected by the caller.*/
NFCSTATUS phHal4Nfc_Transceive(
                               phHal_sHwReference_t          *psHwReference,
                               phHal_sTransceiveInfo_t       *psTransceiveInfo,
                               phHal_sRemoteDevInformation_t  *psRemoteDevInfo,
                               pphHal4Nfc_TransceiveCallback_t pTrcvCallback,
                               void                           *pContext
                               )
{
    NFCSTATUS RetStatus = NFCSTATUS_PENDING;
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)pContext;
   
    /*NULL checks*/
    if((NULL == psHwReference) 
        ||( NULL == pTrcvCallback )
        || (NULL == psRemoteDevInfo)
        || (NULL == psTransceiveInfo)
        || (NULL == psTransceiveInfo->sRecvData.buffer)
        || (NULL == psTransceiveInfo->sSendData.buffer)
        )
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_INVALID_PARAMETER);
    }   
#ifdef HAL_TRCV_LIMIT
    else if(PH_HAL4NFC_MAX_TRCV_LEN < psTransceiveInfo->sSendData.length)
    {
        RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_NOT_ALLOWED);
    }
#endif/*#ifdef HAL_TRCV_LIMIT*/
    /*Check initialised state*/
    else if((NULL == psHwReference->hal_context)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4CurrentState 
                                               < eHal4StateOpenAndReady)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4NextState 
                                               == eHal4StateClosed))
    {
        RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_NOT_INITIALISED);     
    }   
    else
    {
        Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)psHwReference->hal_context;
        gpphHal4Nfc_Hwref = (phHal_sHwReference_t *)psHwReference;
        if((eHal4StateTargetConnected != Hal4Ctxt->Hal4CurrentState)
            ||(eHal4StateInvalid != Hal4Ctxt->Hal4NextState))
        {
            /*Hal4 state Busy*/
            RetStatus = PHNFCSTVAL(CID_NFC_HAL,NFCSTATUS_BUSY);
            PHDBG_INFO("HAL4:Trcv Failed.Returning Busy");
        }
        else if(psRemoteDevInfo != Hal4Ctxt->sTgtConnectInfo.psConnectedDevice)
        {
            /*No such Target connected*/       
            RetStatus = PHNFCSTVAL(CID_NFC_HAL,NFCSTATUS_INVALID_REMOTE_DEVICE);
        }
        else
        { 
            /*allocate Trcv context*/
            if(NULL == Hal4Ctxt->psTrcvCtxtInfo)
            {
                Hal4Ctxt->psTrcvCtxtInfo= (pphHal4Nfc_TrcvCtxtInfo_t)
                phOsalNfc_GetMemory((uint32_t)(sizeof(phHal4Nfc_TrcvCtxtInfo_t)));
                if(NULL != Hal4Ctxt->psTrcvCtxtInfo)
                {
                    (void)memset(Hal4Ctxt->psTrcvCtxtInfo,0,
                                        sizeof(phHal4Nfc_TrcvCtxtInfo_t));
                    Hal4Ctxt->psTrcvCtxtInfo->RecvDataBufferStatus 
                        = NFCSTATUS_PENDING;
                    Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                                                = PH_OSALNFC_INVALID_TIMER_ID;
                }
            }
            if(NULL == Hal4Ctxt->psTrcvCtxtInfo)
            {
                phOsalNfc_RaiseException(phOsalNfc_e_NoMemory,0);
                RetStatus= PHNFCSTVAL(CID_NFC_HAL , 
                                            NFCSTATUS_INSUFFICIENT_RESOURCES);
            }
            else
            {
                /*Process transceive based on Remote device type*/
                switch(Hal4Ctxt->sTgtConnectInfo.psConnectedDevice->RemDevType)
                {
                case phHal_eISO14443_3A_PICC:
                    phHal4Nfc_Iso_3A_Transceive(
                                        psTransceiveInfo,
                                        Hal4Ctxt
                                        );
                    break;
                case phHal_eMifare_PICC: 
                    PHDBG_INFO("Mifare Cmd received");
                    phHal4Nfc_MifareTransceive(
                                        psTransceiveInfo,
                                        psRemoteDevInfo,
                                        Hal4Ctxt
                                        );                            
                    
#if 0                   
                    Hal4Ctxt->psTrcvCtxtInfo->
                        XchangeInfo.params.tag_info.cmd_type 
                                        = (uint8_t)psTransceiveInfo->cmd.MfCmd;
#endif
                    break;
                case phHal_eISO14443_A_PICC:
                case phHal_eISO14443_B_PICC:                
                    PHDBG_INFO("ISO14443 Cmd received");
                    Hal4Ctxt->psTrcvCtxtInfo->
                        XchangeInfo.params.tag_info.cmd_type 
                            = (uint8_t)psTransceiveInfo->cmd.Iso144434Cmd;
                    break;
                case phHal_eISO15693_PICC:
                    PHDBG_INFO("ISO15693 Cmd received");
                    Hal4Ctxt->psTrcvCtxtInfo->
                        XchangeInfo.params.tag_info.cmd_type 
                            = (uint8_t)psTransceiveInfo->cmd.Iso15693Cmd;
                    break;
                case phHal_eNfcIP1_Target:
                    {
                        PHDBG_INFO("NfcIP1 Transceive");
                        Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData
                            = &(psTransceiveInfo->sSendData);
                        Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData = 
                            &(psTransceiveInfo->sRecvData);
                    }   
                    break;                      
                case phHal_eFelica_PICC:
                    PHDBG_INFO("Felica Cmd received");
                    Hal4Ctxt->psTrcvCtxtInfo->
                        XchangeInfo.params.tag_info.cmd_type 
                        = (uint8_t)psTransceiveInfo->cmd.FelCmd;
                    break;              
                case phHal_eJewel_PICC:
                    PHDBG_INFO("Jewel Cmd received");
                    Hal4Ctxt->psTrcvCtxtInfo->
                        XchangeInfo.params.tag_info.cmd_type 
                        = (uint8_t)psTransceiveInfo->cmd.JewelCmd;
                    break;      
                case phHal_eISO14443_BPrime_PICC:
                    RetStatus = PHNFCSTVAL(CID_NFC_HAL ,
                                              NFCSTATUS_FEATURE_NOT_SUPPORTED);
                    break;
                default:
                    PHDBG_WARNING("Invalid Device type received");
                    RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_FAILED);
                    break;

                }
            }
        }
        /*If status is anything other than NFCSTATUS_PENDING ,an error has 
          already occured, so dont process any further and return*/
        if(RetStatus == NFCSTATUS_PENDING)
        {
            if(phHal_eNfcIP1_Target ==
                  Hal4Ctxt->sTgtConnectInfo.psConnectedDevice->RemDevType)
            {
                Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt = pContext;     
                /*Register upper layer callback*/
                Hal4Ctxt->psTrcvCtxtInfo->pUpperTranceiveCb  = pTrcvCallback;                           
                if(psRemoteDevInfo->RemoteDevInfo.NfcIP_Info.MaxFrameLength
                    >= psTransceiveInfo->sSendData.length)
                {
                    Hal4Ctxt->psTrcvCtxtInfo->
                        XchangeInfo.params.nfc_info.more_info = FALSE;
                    Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.tx_length
                                = (uint8_t)psTransceiveInfo->sSendData.length;
                    Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.tx_buffer
                        = psTransceiveInfo->sSendData.buffer;
                    /*Number of bytes remaining for next send*/
                    Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData->length = 0;
                }
                else
                {
                    Hal4Ctxt->psTrcvCtxtInfo->
                        XchangeInfo.params.nfc_info.more_info = TRUE;
                    Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.tx_buffer
                        = Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData->buffer;
                    Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.tx_length
                                                = psRemoteDevInfo->RemoteDevInfo.NfcIP_Info.MaxFrameLength;
#if 0
                    Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData->buffer
                                                += psRemoteDevInfo->RemoteDevInfo.NfcIP_Info.MaxFrameLength;
#else
                    Hal4Ctxt->psTrcvCtxtInfo->NumberOfBytesSent
                        += psRemoteDevInfo->RemoteDevInfo.NfcIP_Info.MaxFrameLength;
#endif
                    /*Number of bytes remaining for next send*/
                    Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData->length
                                               -= psRemoteDevInfo->RemoteDevInfo.NfcIP_Info.MaxFrameLength;
                }
                Hal4Ctxt->Hal4NextState = eHal4StateTransaction;
#ifdef TRANSACTION_TIMER
                /**Create a timer to keep track of transceive timeout*/
                if(Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                    == PH_OSALNFC_INVALID_TIMER_ID)
                {
                    PHDBG_INFO("HAL4: Transaction Timer Create for transceive");
                    Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId 
                        = phOsalNfc_Timer_Create();
                }
                if(Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                    == PH_OSALNFC_INVALID_TIMER_ID)
                {
                    RetStatus = PHNFCSTVAL(CID_NFC_HAL ,
                        NFCSTATUS_INSUFFICIENT_RESOURCES);                      
                }
                else
#endif/*TRANSACTION_TIMER*/
                {
                    PHDBG_INFO("Hal4:Calling phHciNfc_Send_Data from Hal4_Transceive()");
                    RetStatus = phHciNfc_Send_Data (
                                    Hal4Ctxt->psHciHandle,
                                    psHwReference,
                                    Hal4Ctxt->sTgtConnectInfo.psConnectedDevice,
                                    &(Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo)
                                    );
                    if(NFCSTATUS_PENDING == RetStatus)
                    {
                        Hal4Ctxt->psTrcvCtxtInfo->P2P_Send_In_Progress = TRUE;
                    }
                }
            }
            else if(psTransceiveInfo->sSendData.length > PH_HAL4NFC_CMD_LENGTH)
            {
                RetStatus = PHNFCSTVAL(CID_NFC_HAL,NFCSTATUS_INVALID_PARAMETER);
            }
            else if((psTransceiveInfo->sSendData.length == 0)
                    && (Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.length != 0))                                   
            {
                PHDBG_INFO("Hal4:Read remaining bytes");
                Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData 
                                            = &(psTransceiveInfo->sRecvData); 
                /*Number of read bytes left is greater than bytes requested 
                    by upper layer*/
                if(Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData->length
                    < Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.length)
                {
                    (void)memcpy(Hal4Ctxt->psTrcvCtxtInfo
                                                ->psUpperRecvData->buffer,
                        (Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.buffer
                        + Hal4Ctxt->psTrcvCtxtInfo->LowerRecvBufferOffset)
                        ,Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData->length);
                    Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.length -= 
                        Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData->length;
                    Hal4Ctxt->psTrcvCtxtInfo->LowerRecvBufferOffset   
                        += Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData->length;
                    RetStatus = PHNFCSTVAL(CID_NFC_HAL ,
                                                NFCSTATUS_MORE_INFORMATION);
                }
                else/*Number of read bytes left is smaller.Copy all bytes 
                      and free Hal's buffer*/
                {
                    Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData->length
                        = Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.length;
                    (void)memcpy(Hal4Ctxt->psTrcvCtxtInfo
                                                        ->psUpperRecvData
                                                                    ->buffer,
                        (Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.buffer 
                            + Hal4Ctxt->psTrcvCtxtInfo->LowerRecvBufferOffset)
                        ,Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData->length);
                    phOsalNfc_FreeMemory(Hal4Ctxt->psTrcvCtxtInfo
                                                    ->sLowerRecvData.buffer);
                    Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.buffer = NULL;
                    Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.length = 0;
                    Hal4Ctxt->psTrcvCtxtInfo->LowerRecvBufferOffset   = 0;
                    RetStatus = NFCSTATUS_SUCCESS;
                }
            }
            else/*No more bytes remaining in Hal.Read from device*/
            {
                 /*Register upper layer context*/
                Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt = pContext;     
                /*Register upper layer callback*/
                Hal4Ctxt->psTrcvCtxtInfo->pUpperTranceiveCb  = pTrcvCallback;  
                Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.params.tag_info.addr 
                                                    = psTransceiveInfo->addr;   
                Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.tx_length
                                = (uint16_t)psTransceiveInfo->sSendData.length;
                Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.tx_buffer
                                        = psTransceiveInfo->sSendData.buffer;
                Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData 
                                            = &(psTransceiveInfo->sRecvData);              
#ifdef TRANSACTION_TIMER
                /**Create a timer to keep track of transceive timeout*/
                if(Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                                    == PH_OSALNFC_INVALID_TIMER_ID)
                {
                    PHDBG_INFO("HAL4: Transaction Timer Create for transceive");
                    Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId 
                                            = phOsalNfc_Timer_Create();
                }
                if(Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                    == PH_OSALNFC_INVALID_TIMER_ID)
                {
                    RetStatus = PHNFCSTVAL(CID_NFC_HAL ,
                                           NFCSTATUS_INSUFFICIENT_RESOURCES);                      
                }
                else
#endif /*TRANSACTION_TIMER*/
                {
                    PHDBG_INFO("Calling phHciNfc_Exchange_Data");
                    RetStatus = phHciNfc_Exchange_Data(
                        Hal4Ctxt->psHciHandle,
                        psHwReference,
                        Hal4Ctxt->sTgtConnectInfo.psConnectedDevice,
                        &(Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo)
                        );

                    if(NFCSTATUS_PENDING == RetStatus)
                    {
                        Hal4Ctxt->Hal4NextState = eHal4StateTransaction;
#ifdef TRANSACTION_TIMER
                        /**Start timer to keep track of transceive timeout*/
                        phOsalNfc_Timer_Start(
                            Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId,
                            PH_HAL4NFC_TRANSCEIVE_TIMEOUT,
                            phHal4Nfc_TrcvTimeoutHandler
                            );
#endif/*#ifdef TRANSACTION_TIMER*/
                    }   
                    else
                    {
                        Hal4Ctxt->Hal4NextState = eHal4StateInvalid;
                    }
                }
            }
        }
    }
    return RetStatus;
}

#ifdef TRANSACTION_TIMER
/**Handle transceive timeout*/
void phHal4Nfc_TrcvTimeoutHandler(uint32_t TrcvTimerId)
{
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = gpphHal4Nfc_Hwref->hal_context;
    pphHal4Nfc_ReceiveCallback_t pUpperRecvCb = NULL;
    pphHal4Nfc_TransceiveCallback_t pUpperTrcvCb = NULL;
    phOsalNfc_Timer_Stop(TrcvTimerId);            
    phOsalNfc_Timer_Delete(TrcvTimerId);
    Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId = PH_OSALNFC_INVALID_TIMER_ID;
    Hal4Ctxt->Hal4NextState = eHal4StateInvalid;
    /*For a P2P target*/
    if(Hal4Ctxt->psTrcvCtxtInfo->pP2PRecvCb != NULL)
    {
        pUpperRecvCb = Hal4Ctxt->psTrcvCtxtInfo->pP2PRecvCb;
        Hal4Ctxt->psTrcvCtxtInfo->pP2PRecvCb = NULL;        
        (*pUpperRecvCb)(
            Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
            NULL,
            NFCSTATUS_RF_TIMEOUT
            );
    }
    else
    {
        /*For a P2P Initiator and tags*/
        if(Hal4Ctxt->psTrcvCtxtInfo->pUpperTranceiveCb != NULL)
        {
            pUpperTrcvCb = Hal4Ctxt->psTrcvCtxtInfo->pUpperTranceiveCb;
            Hal4Ctxt->psTrcvCtxtInfo->pUpperTranceiveCb = NULL;     
            (*pUpperTrcvCb)(
                        Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
                        Hal4Ctxt->sTgtConnectInfo.psConnectedDevice,        
                        NULL,
                        NFCSTATUS_RF_TIMEOUT
                        );
        }
    }
}
#endif /*TRANSACTION_TIMER*/


/**The function allows to disconnect from a specific Remote Device.*/
NFCSTATUS phHal4Nfc_Disconnect(
                        phHal_sHwReference_t          *psHwReference,
                        phHal_sRemoteDevInformation_t *psRemoteDevInfo,
                        phHal_eReleaseType_t           ReleaseType,
                        pphHal4Nfc_DiscntCallback_t    pDscntCallback,
                        void                             *pContext
                        )
{
    NFCSTATUS RetStatus = NFCSTATUS_PENDING;
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL;
    PHDBG_INFO("Hal4:Inside Hal4 disconnect");
    /*NULL checks*/
    if(NULL == psHwReference || NULL == pDscntCallback 
        || NULL == psRemoteDevInfo)
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_INVALID_PARAMETER);
    }
    /*Check Initialised state*/
    else if((NULL == psHwReference->hal_context)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4CurrentState 
                                               < eHal4StateOpenAndReady)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4NextState 
                                               == eHal4StateClosed))
    {
        RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_NOT_INITIALISED);     
    }  
    else if(((phHal4Nfc_Hal4Ctxt_t *)
                    psHwReference->hal_context)->Hal4CurrentState 
                    != eHal4StateTargetConnected)
    {       
        PHDBG_INFO("Hal4:Current sate is not connect.Release returning failed");
        RetStatus = PHNFCSTVAL(CID_NFC_HAL,NFCSTATUS_FAILED);
    }
    else
    {
        Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)psHwReference->hal_context;
        if((Hal4Ctxt->sTgtConnectInfo.psConnectedDevice == NULL)
            || (psRemoteDevInfo != Hal4Ctxt->sTgtConnectInfo.psConnectedDevice))    
        {
            PHDBG_INFO("Hal4:disconnect returning INVALID_REMOTE_DEVICE");
            RetStatus = PHNFCSTVAL(CID_NFC_HAL,NFCSTATUS_INVALID_REMOTE_DEVICE);
        } 
        else       
        {
            /*Register upper layer context*/
            Hal4Ctxt->sUpperLayerInfo.psUpperLayerDisconnectCtxt = pContext;  
            /*Register upper layer callback*/
            Hal4Ctxt->sTgtConnectInfo.pUpperDisconnectCb  = pDscntCallback;
            /*Register Release Type*/
            Hal4Ctxt->sTgtConnectInfo.ReleaseType = ReleaseType;        
            if((eHal4StateTransaction == Hal4Ctxt->Hal4NextState)
                &&((phHal_eNfcIP1_Target != psRemoteDevInfo->RemDevType)
                ||((NFC_DISCOVERY_CONTINUE != ReleaseType)
                   &&(NFC_DISCOVERY_RESTART != ReleaseType))))
            {
                Hal4Ctxt->sTgtConnectInfo.ReleaseType 
                                                  = NFC_INVALID_RELEASE_TYPE;
                PHDBG_INFO("Hal4:disconnect returning NFCSTATUS_NOT_ALLOWED");
                RetStatus = PHNFCSTVAL(CID_NFC_HAL,NFCSTATUS_NOT_ALLOWED);
            }
            else if((eHal4StateTransaction == Hal4Ctxt->Hal4NextState)
                    &&(NULL != Hal4Ctxt->psTrcvCtxtInfo)
                    &&(TRUE == Hal4Ctxt->psTrcvCtxtInfo->P2P_Send_In_Progress))
            {
                /*store the hardware reference for executing disconnect later*/
                gpphHal4Nfc_Hwref = psHwReference;   
                PHDBG_INFO("Hal4:disconnect deferred");
            }           
            else/*execute disconnect*/
            {
                RetStatus = phHal4Nfc_Disconnect_Execute(psHwReference);
            }           
        }
    }   
    return RetStatus;
}

/**Execute Hal4 Disconnect*/
NFCSTATUS phHal4Nfc_Disconnect_Execute(
                            phHal_sHwReference_t  *psHwReference
                            )
{
    NFCSTATUS RetStatus = NFCSTATUS_PENDING;
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = 
        (phHal4Nfc_Hal4Ctxt_t *)psHwReference->hal_context;
    phHal_eSmartMX_Mode_t SmxMode = eSmartMx_Default;
    PHDBG_INFO("Hal4:Inside Hal4 disconnect execute");
    switch(Hal4Ctxt->sTgtConnectInfo.ReleaseType)
    {
        /*Switch mode to Default*/ 
        case NFC_SMARTMX_RELEASE:
            SmxMode = eSmartMx_Default;
            RetStatus = phHciNfc_Switch_SmxMode (
                Hal4Ctxt->psHciHandle,
                psHwReference,
                SmxMode,
                &(Hal4Ctxt->psADDCtxtInfo->sADDCfg)
                ); 
            break; 
        /*Disconnect and continue polling wheel*/
        case NFC_DISCOVERY_CONTINUE:
        {
            RetStatus = phHciNfc_Disconnect (
                                    Hal4Ctxt->psHciHandle,
                                    psHwReference,
                                    FALSE
                                    );
            if(NFCSTATUS_PENDING != RetStatus)
            {
                PHDBG_INFO("Hal4:Hci disconnect failed.Restarting discovery");
                RetStatus = phHciNfc_Restart_Discovery (
                                    (void *)Hal4Ctxt->psHciHandle,
                                    (void *)gpphHal4Nfc_Hwref,      
                                    FALSE
                                    );
                if(NFCSTATUS_PENDING != RetStatus)
                {
                    PHDBG_INFO("Hal4:Hci Restart discovery also failed");
                }
            }
            break;
        }
        /*Disconnect and restart polling wheel*/
        case NFC_DISCOVERY_RESTART:
            RetStatus = phHciNfc_Disconnect (
                                Hal4Ctxt->psHciHandle,
                                psHwReference,
                                TRUE
                                );
            break;
        default:
            RetStatus = PHNFCSTVAL(CID_NFC_HAL,
                NFCSTATUS_FEATURE_NOT_SUPPORTED);
            break;
    }   
    Hal4Ctxt->sTgtConnectInfo.ReleaseType = NFC_INVALID_RELEASE_TYPE;
    /*Update or rollback next state*/
    Hal4Ctxt->Hal4NextState = (NFCSTATUS_PENDING == RetStatus?
                    eHal4StateOpenAndReady:Hal4Ctxt->Hal4NextState);
    return  RetStatus;
}

/*The function allows to check for presence in vicinity of connected remote 
  device.*/
NFCSTATUS phHal4Nfc_PresenceCheck(                                    
                                phHal_sHwReference_t     *psHwReference,
                                pphHal4Nfc_GenCallback_t  pPresenceChkCb,
                                void *context
                                )
{
    NFCSTATUS RetStatus = NFCSTATUS_FAILED;
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL;
    /*NULL  checks*/
    if((NULL == pPresenceChkCb) || (NULL == psHwReference))
    {
        RetStatus = PHNFCSTVAL(CID_NFC_HAL,NFCSTATUS_INVALID_PARAMETER);
    }
    /*Check Initialised state*/
    else if((NULL == psHwReference->hal_context)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4CurrentState 
                                               < eHal4StateOpenAndReady)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4NextState 
                                               == eHal4StateClosed))
    {
        PHDBG_INFO("HAL4:Context not Open");
        RetStatus = PHNFCSTVAL(CID_NFC_HAL,NFCSTATUS_NOT_INITIALISED);
    }
    /*check connected state and session alive*/
    else if((((phHal4Nfc_Hal4Ctxt_t *)
             psHwReference->hal_context)->Hal4CurrentState
                                < eHal4StateTargetConnected)||
            (NULL == ((phHal4Nfc_Hal4Ctxt_t *)
              psHwReference->hal_context)->sTgtConnectInfo.psConnectedDevice)||
            (FALSE == ((phHal4Nfc_Hal4Ctxt_t *)
                        psHwReference->hal_context)->sTgtConnectInfo.
                                    psConnectedDevice->SessionOpened))
    {
        PHDBG_INFO("HAL4:No target connected");
        RetStatus = PHNFCSTVAL(CID_NFC_HAL,NFCSTATUS_RELEASED);
    }
    else 
    {
        Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)psHwReference->hal_context;
        /*allow only one Presence chk command at any point in time*/
        if (eHal4StatePresenceCheck != Hal4Ctxt->Hal4NextState)
        {
            /* Check if remote device is felica */
            if (Hal4Ctxt->sTgtConnectInfo.psConnectedDevice->RemDevType ==
                phHal_eFelica_PICC)
            {
                /* If felica PICC then copy existing IDm to compare later,
                   If IDm will be same then same PICC is present after presence check
                   else PICC is changed */
                (void) memcpy(Hal4Ctxt->FelicaIDm,
                              Hal4Ctxt->sTgtConnectInfo.psConnectedDevice->RemoteDevInfo.Felica_Info.IDm,
                              Hal4Ctxt->sTgtConnectInfo.psConnectedDevice->RemoteDevInfo.Felica_Info.IDmLength);
            }
            Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt = context;
            Hal4Ctxt->sTgtConnectInfo.pPresenceChkCb = pPresenceChkCb;
            RetStatus = phHciNfc_Presence_Check(Hal4Ctxt->psHciHandle,
                                                psHwReference
                                                );
            Hal4Ctxt->Hal4NextState = (NFCSTATUS_PENDING == RetStatus?
                eHal4StatePresenceCheck:Hal4Ctxt->Hal4NextState);
        }
        else/*Ongoing presence chk*/
        {
            RetStatus = PHNFCSTVAL(CID_NFC_HAL,NFCSTATUS_BUSY);
        }
    }
    return RetStatus;
}

void phHal4Nfc_PresenceChkComplete(
                                   phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                                   void *pInfo
                                   )
{
    NFCSTATUS RetStatus = ((phNfc_sCompletionInfo_t *)pInfo)->status;    
    Hal4Ctxt->Hal4NextState = eHal4StateInvalid;   
    /*Notify to upper layer*/
    if(NULL != Hal4Ctxt->sTgtConnectInfo.pPresenceChkCb)
    {
        Hal4Ctxt->sTgtConnectInfo.psConnectedDevice->SessionOpened
                     =(uint8_t)(NFCSTATUS_SUCCESS == RetStatus?TRUE:FALSE);
        /* Check if remote device is felica */
        if (Hal4Ctxt->sTgtConnectInfo.psConnectedDevice->RemDevType ==
            phHal_eFelica_PICC)
        {
            /* Check if IDm received is same as saved */
            if (0 != phOsalNfc_MemCompare(Hal4Ctxt->FelicaIDm,
                Hal4Ctxt->sTgtConnectInfo.psConnectedDevice->RemoteDevInfo.Felica_Info.IDm,
                Hal4Ctxt->sTgtConnectInfo.psConnectedDevice->RemoteDevInfo.Felica_Info.IDmLength))
            {
                RetStatus = NFCSTATUS_FAILED;

                /* Presence check failed so reset remote device information */
                (void) memset(Hal4Ctxt->sTgtConnectInfo.psConnectedDevice->RemoteDevInfo.Felica_Info.IDm,
                              0, PHHAL_FEL_ID_LEN + 2);
                (void) memset(Hal4Ctxt->sTgtConnectInfo.psConnectedDevice->RemoteDevInfo.Felica_Info.PMm,
                              0, PHHAL_FEL_PM_LEN);
            }

            /* Cleanup for stored IDm value for using it next time */
            (void) memset(Hal4Ctxt->FelicaIDm, 0, PHHAL_FEL_ID_LEN + 2);
        }

        (*Hal4Ctxt->sTgtConnectInfo.pPresenceChkCb)(
                        Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
                        RetStatus
                        );
    }
    return;
}

/*Callback for reactivate target and to select appropriate target incase 
 of multiple targets*/
void phHal4Nfc_ReactivationComplete(
                                    phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                                    void *pInfo
                                    )
{
    NFCSTATUS Status = ((phNfc_sCompletionInfo_t *)pInfo)->status;
    /*A NFCSTATUS_SUCCESS status returned here means that the correct device 
     to connect to has now been selected.So issue connect from here to complete
     activation*/
    if(NFCSTATUS_SUCCESS == Status)
    {
        Hal4Ctxt->Hal4NextState = eHal4StateTargetConnected;
        Status = phHciNfc_Connect(
            Hal4Ctxt->psHciHandle,
            gpphHal4Nfc_Hwref,
            Hal4Ctxt->sTgtConnectInfo.psConnectedDevice
            );
        Status = (NFCSTATUS_PENDING == Status)?
                    NFCSTATUS_PENDING:NFCSTATUS_FAILED;
    }
    else/*Device unavailable, return error in connect Callback*/
    {
        Hal4Ctxt->Hal4NextState = eHal4StateInvalid;
        if(NULL != Hal4Ctxt->sTgtConnectInfo.pUpperConnectCb)
        {
            (*Hal4Ctxt->sTgtConnectInfo.pUpperConnectCb)(
                                Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
                                Hal4Ctxt->sTgtConnectInfo.psConnectedDevice,
                                Status
                                );
        }
    }
    return;
}

void phHal4Nfc_Felica_RePoll(void     *context,
                                        NFCSTATUS status)
{
     phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt = gpHal4Ctxt;
     pphHal4Nfc_ConnectCallback_t pUpperConnectCb
                                = Hal4Ctxt->sTgtConnectInfo.pUpperConnectCb;

     Hal4Ctxt->sTgtConnectInfo.pUpperConnectCb = NULL;
     PHDBG_INFO("Hal4:Calling Connect callback");

    if (pUpperConnectCb != NULL)
    {
         /*Notify to the upper layer*/
         (*pUpperConnectCb)(
                    Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
                    Hal4Ctxt->sTgtConnectInfo.psConnectedDevice,
                    status
                    );
    }

    return;
}
void phHal4Nfc_ConnectComplete(
                               phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                               void *pInfo
                               )
{
    NFCSTATUS ConnectStatus = ((phNfc_sCompletionInfo_t *)pInfo)->status;
    pphHal4Nfc_ConnectCallback_t pUpperConnectCb 
                                = Hal4Ctxt->sTgtConnectInfo.pUpperConnectCb;
    /*Flag to decide whether or not upper layer callback has to be called*/
    uint8_t CallConnectCb = TRUE;
    uint8_t felicaRePoll = FALSE;

    /*Remote device Connect successful*/
    if((NFCSTATUS_SUCCESS == ConnectStatus)
		||(eHal4StateTargetConnected == Hal4Ctxt->Hal4CurrentState))
    {
        phHal_sRemoteDevInformation_t *psRmtTgtConnected =
                            Hal4Ctxt->sTgtConnectInfo.psConnectedDevice;
        PHDBG_INFO("Hal4:Connect status Success");
        Hal4Ctxt->Hal4CurrentState = eHal4StateTargetConnected;
        Hal4Ctxt->Hal4NextState = eHal4StateInvalid;
        /* Open the Session */
        psRmtTgtConnected->SessionOpened =
            (uint8_t)(NFCSTATUS_SUCCESS == ConnectStatus?TRUE:FALSE);
        Hal4Ctxt->sTgtConnectInfo.pUpperConnectCb = NULL;
        if (psRmtTgtConnected->RemDevType == phHal_eFelica_PICC)
        {
            felicaRePoll = TRUE;
        }
    }
    else/*Remote device Connect failed*/
    {        
        Hal4Ctxt->Hal4CurrentState = eHal4StateOpenAndReady;
        Hal4Ctxt->sTgtConnectInfo.psConnectedDevice->SessionOpened = FALSE;
        /*For a NfcIp1 target and case where it is not a internal reconnect 
          from Hal4 ,notify callback to upper layer*/
        if((phHal_eNfcIP1_Target 
            == Hal4Ctxt->rem_dev_list[0]->RemDevType)
            || (NULL != Hal4Ctxt->sTgtConnectInfo.pUpperConnectCb))
        {
            Hal4Ctxt->sTgtConnectInfo.pUpperConnectCb = NULL;
        }
        else/*do not notify callback*/
        {
            CallConnectCb = FALSE;
        }
        /*Free the remote device list*/
        do
        {
            Hal4Ctxt->psADDCtxtInfo->nbr_of_devices--;
            if(NULL != Hal4Ctxt->rem_dev_list[
                        Hal4Ctxt->psADDCtxtInfo->nbr_of_devices])
            {
                phOsalNfc_FreeMemory((void *)
                        (Hal4Ctxt->rem_dev_list[
                            Hal4Ctxt->psADDCtxtInfo->nbr_of_devices]));
                Hal4Ctxt->rem_dev_list[
                    Hal4Ctxt->psADDCtxtInfo->nbr_of_devices] = NULL;
            }
        }while(0 < Hal4Ctxt->psADDCtxtInfo->nbr_of_devices); 
        
        Hal4Ctxt->sTgtConnectInfo.psConnectedDevice = NULL;              
    }
    if(TRUE == CallConnectCb)
    {
        if (felicaRePoll == TRUE)
        {
            /* Felica repoll through presence check */

            /* If felica PICC then copy existing IDm to compare later,
               If IDm will be same then same PICC is present after presence check
               else PICC is changed */
            (void) memcpy(Hal4Ctxt->FelicaIDm,
                          Hal4Ctxt->sTgtConnectInfo.psConnectedDevice->RemoteDevInfo.Felica_Info.IDm,
                          Hal4Ctxt->sTgtConnectInfo.psConnectedDevice->RemoteDevInfo.Felica_Info.IDmLength);

            gpHal4Ctxt = Hal4Ctxt;
            Hal4Ctxt->sTgtConnectInfo.pPresenceChkCb = phHal4Nfc_Felica_RePoll;
            ConnectStatus = phHciNfc_Presence_Check(Hal4Ctxt->psHciHandle,
                                                    gpphHal4Nfc_Hwref
                                                    );
            Hal4Ctxt->Hal4NextState = (NFCSTATUS_PENDING == ConnectStatus?
                    eHal4StatePresenceCheck:Hal4Ctxt->Hal4NextState);
            felicaRePoll = FALSE;
            Hal4Ctxt->sTgtConnectInfo.pUpperConnectCb = pUpperConnectCb;
        }
        else
        {
            PHDBG_INFO("Hal4:Calling Connect callback");
            /*Notify to the upper layer*/
            (*pUpperConnectCb)(
                    Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
                    Hal4Ctxt->sTgtConnectInfo.psConnectedDevice,
                    ConnectStatus
                    );
	 }
    }
    else
    {
        PHDBG_INFO("Hal4:Connect failed ,Restarting discovery");
        /*Restart the Discovery wheel*/ 
        ConnectStatus = phHciNfc_Restart_Discovery (
                                    (void *)Hal4Ctxt->psHciHandle,
                                    (void *)gpphHal4Nfc_Hwref,      
                                    FALSE
                                    );
        Hal4Ctxt->Hal4NextState = (NFCSTATUS_PENDING == ConnectStatus?
                                    eHal4StateConfiguring:eHal4StateInvalid);
    }
    return;
}




void phHal4Nfc_DisconnectComplete(
                                  phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                                  void *pInfo
                                  )
{
    NFCSTATUS ConnectStatus = ((phNfc_sCompletionInfo_t *)pInfo)->status;
    phHal_sRemoteDevInformation_t *psConnectedDevice = NULL;        
    pphHal4Nfc_DiscntCallback_t pUpperDisconnectCb = NULL;                                
    pphHal4Nfc_TransceiveCallback_t pUpperTrcvCb = NULL;
    PHDBG_INFO("Hal4:Inside Hal4 disconnect callback");
    if(NULL == Hal4Ctxt)
    {
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
    }
    else if(NFCSTATUS_SUCCESS != ConnectStatus)/*Restart the Discovery wheel*/ 
    {
        ConnectStatus = phHciNfc_Restart_Discovery (
                                    (void *)Hal4Ctxt->psHciHandle,
                                    (void *)gpphHal4Nfc_Hwref,      
                                    FALSE
                                    );
        Hal4Ctxt->Hal4NextState = (NFCSTATUS_PENDING == ConnectStatus?
                                    eHal4StateConfiguring:eHal4StateInvalid);
    }
    else/*Remote device Disconnect successful*/
    {
        psConnectedDevice = Hal4Ctxt->sTgtConnectInfo.psConnectedDevice;
        pUpperDisconnectCb = Hal4Ctxt->sTgtConnectInfo.pUpperDisconnectCb;
        /*Deallocate psTrcvCtxtInfo*/
        if(NULL != Hal4Ctxt->psTrcvCtxtInfo)
        {
            if(NULL == Hal4Ctxt->sTgtConnectInfo.psConnectedDevice) 
            {
               if(NULL != Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData)
                {
                    phOsalNfc_FreeMemory(
                        Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData);
                }
            }
            else
            {
                if(phHal_eNfcIP1_Target
                    == Hal4Ctxt->sTgtConnectInfo.psConnectedDevice->RemDevType)
                {
                    pUpperTrcvCb = Hal4Ctxt->psTrcvCtxtInfo->pUpperTranceiveCb; 
                    Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData->length = 0;
                    pUpperTrcvCb = Hal4Ctxt->psTrcvCtxtInfo->pUpperTranceiveCb;
                    Hal4Ctxt->psTrcvCtxtInfo->pUpperTranceiveCb = NULL;
                }
            }
            if(NULL != Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.buffer)
            {
                phOsalNfc_FreeMemory(
                    Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.buffer);
            }
            
            phOsalNfc_FreeMemory(Hal4Ctxt->psTrcvCtxtInfo);
            Hal4Ctxt->psTrcvCtxtInfo = NULL;
        }
        /*Free the remote device list*/
        do
        {
            if(NULL != Hal4Ctxt->rem_dev_list[Hal4Ctxt->
                psADDCtxtInfo->nbr_of_devices-1])
            {
                phOsalNfc_FreeMemory((void *)
                    (Hal4Ctxt->rem_dev_list[Hal4Ctxt->
                    psADDCtxtInfo->nbr_of_devices-1]));
                Hal4Ctxt->rem_dev_list[Hal4Ctxt->
                    psADDCtxtInfo->nbr_of_devices-1] = NULL;
            }
        }while(--(Hal4Ctxt->psADDCtxtInfo->nbr_of_devices));
        
        Hal4Ctxt->sTgtConnectInfo.psConnectedDevice = NULL;
        /*Disconnect successful.Go to Ready state*/
        Hal4Ctxt->Hal4CurrentState = Hal4Ctxt->Hal4NextState;        
        Hal4Ctxt->sTgtConnectInfo.pUpperDisconnectCb = NULL;
        Hal4Ctxt->Hal4NextState = (
            eHal4StateOpenAndReady == Hal4Ctxt->Hal4NextState?
            eHal4StateInvalid:Hal4Ctxt->Hal4NextState);
        /*Issue any pending Trcv callback*/
        if(NULL != pUpperTrcvCb)
        {          
            (*pUpperTrcvCb)(
                Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
                psConnectedDevice,
                NULL,
                NFCSTATUS_FAILED
                );
        }
        /*Notify upper layer*/
        if(NULL != pUpperDisconnectCb)
        {
            PHDBG_INFO("Hal4:Calling Upper layer disconnect callback");
            (*pUpperDisconnectCb)(
                        Hal4Ctxt->sUpperLayerInfo.psUpperLayerDisconnectCtxt,
                        psConnectedDevice,
                        ConnectStatus                            
                        );
        }
    }
    return;
}


/*Transceive complete handler function*/
void phHal4Nfc_TransceiveComplete(
                                  phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                                  void *pInfo
                                  )
{
    /*Copy status code*/
    NFCSTATUS TrcvStatus = ((phNfc_sCompletionInfo_t *)pInfo)->status;
    /*Update next state*/
    Hal4Ctxt->Hal4NextState = (eHal4StateTransaction
             == Hal4Ctxt->Hal4NextState?eHal4StateInvalid:Hal4Ctxt->Hal4NextState);
    /*Reset SelectSectorFlag for Mifare*/
    if (Hal4Ctxt->SelectSectorFlag == 2)
    {
        TrcvStatus = NFCSTATUS_SUCCESS;
        PHDBG_INFO("Inside Hal4TrcvComplete SelectSectorFlag is 2");
        Hal4Ctxt->SelectSectorFlag = 0;
    }
        
    if(NULL == Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData)
    {
        /*if recv buffer is Null*/
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        TrcvStatus = NFCSTATUS_FAILED;
    }
    else if(TrcvStatus == NFCSTATUS_SUCCESS)
    {
        /*Check if recvdata buffer given by upper layer is big enough to 
        receive all response bytes.If it is not big enough ,copy number 
        of bytes requested by upper layer to the buffer.Remaining
        bytes are retained in Hal4 and upper layer has to issue another 
        transceive call to read the same.*/
        if(((phNfc_sTransactionInfo_t *)pInfo)
            ->length  > Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData->length )
        {
            TrcvStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_MORE_INFORMATION);
            Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.length 
                = ((phNfc_sTransactionInfo_t *)pInfo)->length 
                - Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData->length; 

            Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.buffer
                = (uint8_t *)phOsalNfc_GetMemory(
                Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.length
                );
            if(NULL != Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.buffer)
            {
                (void)memcpy(
                    Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.buffer,
                    (((phNfc_sTransactionInfo_t *)pInfo)->buffer 
                    + Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData
                    ->length)
                    ,Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.length
                    );
            }
            else
            {
                TrcvStatus = PHNFCSTVAL(CID_NFC_HAL,
                    NFCSTATUS_INSUFFICIENT_RESOURCES);
                phOsalNfc_RaiseException(phOsalNfc_e_NoMemory,0);
            }

        }
        else/*Buffer provided by upper layer is big enough to hold all read 
              bytes*/
        {
            Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData->length
                = ((phNfc_sTransactionInfo_t *)pInfo)->length;
            Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.length = 0;         
        }
        (void)memcpy(Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData->buffer,
            ((phNfc_sTransactionInfo_t *)pInfo)->buffer,
            Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData->length
            );

    }
    else/*Error scenario.Set received bytes length to zero*/
    {
        Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData->length = 0;
    }
    (void)memset((void *)&(Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.params),
                  0,
                  sizeof(Hal4Ctxt->psTrcvCtxtInfo->XchangeInfo.params)
                );
    Hal4Ctxt->psTrcvCtxtInfo->LowerRecvBufferOffset = 0;
    /*Issue transceive callback*/
    (*Hal4Ctxt->psTrcvCtxtInfo->pUpperTranceiveCb)(
        Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
        Hal4Ctxt->sTgtConnectInfo.psConnectedDevice,        
        Hal4Ctxt->psTrcvCtxtInfo->psUpperRecvData,
        TrcvStatus
        );
    return;
}
