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
 * \file  phHal4Nfc_ADD.c
 * \brief Hal4Nfc_ADD source.
 *
 * Project: NFC-FRI 1.1
 *
 * $Date: Mon May 31 11:43:42 2010 $
 * $Author: ing07385 $
 * $Revision: 1.151 $
 * $Aliases: NFC_FRI1.1_WK1023_R35_1 $
 *
 */

/* ---------------------------Include files ----------------------------------*/
#include <phHciNfc.h>
#include <phHal4Nfc.h>
#include <phHal4Nfc_Internal.h>
#include <phOsalNfc.h>

/* ------------------------------- Macros ------------------------------------*/
#define     NFCIP_ACTIVE_SHIFT      0x03U       
#define     NXP_UID                 0x04U
#define     NXP_MIN_UID_LEN         0x07U
/* --------------------Structures and enumerations --------------------------*/

NFCSTATUS phHal4Nfc_ConfigParameters(                       
                        phHal_sHwReference_t     *psHwReference,
                        phHal_eConfigType_t       CfgType,
                        phHal_uConfig_t          *puConfig,
                        pphHal4Nfc_GenCallback_t  pConfigCallback,
                        void                     *pContext
                        )
{
    NFCSTATUS CfgStatus = NFCSTATUS_SUCCESS;
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL;
    /*NULL checks*/
    if(NULL == psHwReference  
        || NULL == pConfigCallback
        || NULL == puConfig
        )
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        CfgStatus = PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_INVALID_PARAMETER);
    }
    /*Check if initialised*/
    else if((NULL == psHwReference->hal_context)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4CurrentState 
                                               < eHal4StateOpenAndReady)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4NextState 
                                               == eHal4StateClosed))
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        CfgStatus = PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_NOT_INITIALISED);
    }
    else
    {
        Hal4Ctxt = psHwReference->hal_context;
        /*If previous Configuration request has not completed,do not allow new 
          configuration*/
        if(Hal4Ctxt->Hal4NextState == eHal4StateConfiguring)
        {
            PHDBG_INFO("Hal4:PollCfg in progress.Returning status Busy");
            CfgStatus= PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_BUSY);
        }
        else if(Hal4Ctxt->Hal4CurrentState >= eHal4StateOpenAndReady)
        {
            /*Allocate ADD context*/
            if (NULL == Hal4Ctxt->psADDCtxtInfo)
            {
                Hal4Ctxt->psADDCtxtInfo= (pphHal4Nfc_ADDCtxtInfo_t)
                    phOsalNfc_GetMemory((uint32_t)
                    (sizeof(phHal4Nfc_ADDCtxtInfo_t)));
                if(NULL != Hal4Ctxt->psADDCtxtInfo)
                {
                    (void)memset(Hal4Ctxt->psADDCtxtInfo,0,
                        sizeof(phHal4Nfc_ADDCtxtInfo_t)
                        );
                }
            }
            if(NULL == Hal4Ctxt->psADDCtxtInfo)
            {
                phOsalNfc_RaiseException(phOsalNfc_e_NoMemory,0);
                CfgStatus= PHNFCSTVAL(CID_NFC_HAL ,
                    NFCSTATUS_INSUFFICIENT_RESOURCES);
            }
            else
            {
                /*Register Upper layer context*/
#ifdef LLCP_DISCON_CHANGES
                Hal4Ctxt->sUpperLayerInfo.psUpperLayerCfgDiscCtxt = pContext;
#else /* #ifdef LLCP_DISCON_CHANGES */
                Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt = pContext;
#endif /* #ifdef LLCP_DISCON_CHANGES */
                switch(CfgType)
                {
                /*NFC_EMULATION_CONFIG*/
                case NFC_EMULATION_CONFIG:
                {
                    (void)memcpy((void *)&Hal4Ctxt->uConfig,
                        (void *)puConfig,
                        sizeof(phHal_uConfig_t)
                        );                    
                    break;
                }
                /*P2P Configuration*/
                case NFC_P2P_CONFIG:
                { 
                    /*If general bytes are not provided by above layer copy zeros
                      in general bytes*/
                    if(puConfig->nfcIPConfig.generalBytesLength == 0)
                    {
                        Hal4Ctxt->uConfig.nfcIPConfig.generalBytesLength = 0x00;
                        (void)memset(Hal4Ctxt->uConfig.nfcIPConfig.generalBytes,
                                    0,Hal4Ctxt->uConfig.nfcIPConfig.generalBytesLength
                                    );
                    }
                    else
                    {
                        (void)memcpy((void *)&Hal4Ctxt->uConfig,
                            (void *)puConfig,
                            sizeof(phHal_uConfig_t)
                            );
                    }
                    break;
                }     
                /*Protection config*/
                case NFC_SE_PROTECTION_CONFIG:
                {
#ifdef IGNORE_EVT_PROTECTED
                    Hal4Ctxt->Ignore_Event_Protected = FALSE;
#endif/*#ifdef IGNORE_EVT_PROTECTED*/
                    (void)memcpy((void *)&Hal4Ctxt->uConfig,
                        (void *)puConfig,
                        sizeof(phHal_uConfig_t)
                        );
                    break;
                }
                default:
                    CfgStatus = NFCSTATUS_FAILED;
                    break;
                }
                if ( NFCSTATUS_SUCCESS == CfgStatus )
                {
                    /*Issue configure with given configuration*/ 
                    CfgStatus = phHciNfc_Configure(
                                    (void *)Hal4Ctxt->psHciHandle,
                                    (void *)psHwReference,
                                    CfgType,
                                    &Hal4Ctxt->uConfig
                                    );    
                    /* Change the State of the HAL only if status is Pending */
                    if ( NFCSTATUS_PENDING == CfgStatus )
                    {
                        Hal4Ctxt->Hal4NextState = eHal4StateConfiguring;
                        Hal4Ctxt->sUpperLayerInfo.pConfigCallback
                            = pConfigCallback;
                    }
                }
            }
        }
        else
        {
            phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
            CfgStatus= PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_NOT_INITIALISED);
        }
    }
    return CfgStatus;
}


/**Configure the discovery*/
NFCSTATUS phHal4Nfc_ConfigureDiscovery(                       
                        phHal_sHwReference_t          *psHwReference,
                        phHal_eDiscoveryConfigMode_t   discoveryMode,  
                        phHal_sADD_Cfg_t              *discoveryCfg,
                        pphHal4Nfc_GenCallback_t       pConfigCallback,
                        void                          *pContext
                        )
{
    NFCSTATUS CfgStatus = NFCSTATUS_SUCCESS;
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL;
    if(NULL == psHwReference  
        || NULL == pConfigCallback
        || NULL == discoveryCfg
        )
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        CfgStatus = PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHwReference->hal_context)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4CurrentState 
                                               < eHal4StateOpenAndReady)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4NextState 
                                               == eHal4StateClosed))
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        CfgStatus = PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_NOT_INITIALISED);
    }
    else
    {
        Hal4Ctxt = psHwReference->hal_context;
        /*If previous Configuration request has not completed ,do not allow 
          new configuration*/
        if(Hal4Ctxt->Hal4NextState == eHal4StateConfiguring)
        {
            PHDBG_INFO("Hal4:PollCfg in progress.Returning status Busy");
            CfgStatus= PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_BUSY);
        }
        else if(Hal4Ctxt->Hal4CurrentState >= eHal4StateOpenAndReady)
        {
            if (NULL == Hal4Ctxt->psADDCtxtInfo)
            {
                Hal4Ctxt->psADDCtxtInfo= (pphHal4Nfc_ADDCtxtInfo_t)
                    phOsalNfc_GetMemory((uint32_t)
                    (sizeof(phHal4Nfc_ADDCtxtInfo_t)));
                if(NULL != Hal4Ctxt->psADDCtxtInfo)
                {
                    (void)memset(Hal4Ctxt->psADDCtxtInfo,0,
                        sizeof(phHal4Nfc_ADDCtxtInfo_t)
                        );
                }
            }
            if(NULL == Hal4Ctxt->psADDCtxtInfo)
            {
                phOsalNfc_RaiseException(phOsalNfc_e_NoMemory,0);
                CfgStatus= PHNFCSTVAL(CID_NFC_HAL ,
                    NFCSTATUS_INSUFFICIENT_RESOURCES);
            }
            else
            {
                /*Register Upper layer context*/
#ifdef LLCP_DISCON_CHANGES
                Hal4Ctxt->sUpperLayerInfo.psUpperLayerCfgDiscCtxt = pContext;
#else /* #ifdef LLCP_DISCON_CHANGES */
                Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt = pContext;
#endif /* #ifdef LLCP_DISCON_CHANGES */
                switch(discoveryMode)
                {
                case NFC_DISCOVERY_START:
                    PHDBG_INFO("Hal4:Call to NFC_DISCOVERY_START");
                    break;
                case NFC_DISCOVERY_CONFIG:
                    PHDBG_INFO("Hal4:Call to NFC_DISCOVERY_CONFIG");
                    /*Since sADDCfg is allocated in stack ,copy the ADD 
                      configuration structure to HAL4 context*/
                    (void)memcpy((void *)
                        &(Hal4Ctxt->psADDCtxtInfo->sADDCfg),
                        (void *)discoveryCfg,
                        sizeof(phHal_sADD_Cfg_t)
                        );    
                    PHDBG_INFO("Hal4:Finished copying sADDCfg");                    
                    Hal4Ctxt->psADDCtxtInfo->smx_discovery = FALSE;
#ifdef UPDATE_NFC_ACTIVE
                    Hal4Ctxt->psADDCtxtInfo->sADDCfg.PollDevInfo.PollCfgInfo.EnableNfcActive
                        = ( 0 == Hal4Ctxt->psADDCtxtInfo->sADDCfg.NfcIP_Mode?
                           Hal4Ctxt->psADDCtxtInfo->sADDCfg.NfcIP_Mode:
                           NXP_NFCIP_ACTIVE_DEFAULT);
                    Hal4Ctxt->psADDCtxtInfo->sADDCfg.NfcIP_Mode = (( 
                    Hal4Ctxt->psADDCtxtInfo->sADDCfg.NfcIP_Mode <<
                    (NXP_NFCIP_ACTIVE_DEFAULT * NFCIP_ACTIVE_SHIFT)) 
                    | Hal4Ctxt->psADDCtxtInfo->sADDCfg.NfcIP_Mode);
#endif/*#ifdef UPDATE_NFC_ACTIVE*/
                    /* information system_code(Felica) and
                    AFI(ReaderB) to be populated later */

                    CfgStatus = phHciNfc_Config_Discovery(
                        (void *)Hal4Ctxt->psHciHandle,
                        (void *)psHwReference,
                        &(Hal4Ctxt->psADDCtxtInfo->sADDCfg)
                        );/*Configure HCI Discovery*/                    
                    break;
                case NFC_DISCOVERY_STOP:
                    break;
                /*Restart Discovery wheel*/ 
                case NFC_DISCOVERY_RESUME:
                    PHDBG_INFO("Hal4:Call to NFC_DISCOVERY_RESUME");
                    Hal4Ctxt->psADDCtxtInfo->nbr_of_devices = 0;
                    CfgStatus = phHciNfc_Restart_Discovery (
                                    (void *)Hal4Ctxt->psHciHandle,
                                    (void *)psHwReference,      
                                    FALSE
                                    );
                    break;
                default:
                    break;
                }
                /* Change the State of the HAL only if HCI Configure 
                   Returns status as Pending */
                if ( NFCSTATUS_PENDING == CfgStatus )
                {
                    (void)memcpy((void *)
                        &(Hal4Ctxt->psADDCtxtInfo->sCurrentPollConfig),
                        (void *)&(discoveryCfg->PollDevInfo.PollCfgInfo),
                        sizeof(phHal_sPollDevInfo_t)
                        );  
                    PHDBG_INFO("Hal4:Finished copying PollCfgInfo");
                    PHDBG_INFO("Hal4:Configure returned NFCSTATUS_PENDING");
                    Hal4Ctxt->Hal4NextState = eHal4StateConfiguring;
                    Hal4Ctxt->sUpperLayerInfo.pConfigCallback
                        = pConfigCallback;
                }
                else/*Configure failed.Restore old poll dev info*/
                {
                    (void)memcpy((void *)
                        &(Hal4Ctxt->psADDCtxtInfo->sADDCfg.PollDevInfo.PollCfgInfo),
                        (void *)&(Hal4Ctxt->psADDCtxtInfo->sCurrentPollConfig),
                        sizeof(phHal_sPollDevInfo_t)
                        );  
                }
            }
        }
        else
        {
            phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
            CfgStatus= PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_NOT_INITIALISED);
        }
    }
    return CfgStatus;
}


/*Configuration completion handler*/
void phHal4Nfc_ConfigureComplete(phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                                 void                  *pInfo,
                                 uint8_t                type
                                 )
{
    pphHal4Nfc_GenCallback_t    pConfigCallback
                                = Hal4Ctxt->sUpperLayerInfo.pConfigCallback;
    pphHal4Nfc_ConnectCallback_t pUpperConnectCb 
                    = Hal4Ctxt->sTgtConnectInfo.pUpperConnectCb;    
    NFCSTATUS Status = ((phNfc_sCompletionInfo_t *)pInfo)->status;
    if((type == NFC_NOTIFY_POLL_ENABLED) ||(type == NFC_NOTIFY_POLL_RESTARTED))
    {
        Hal4Ctxt->psADDCtxtInfo->IsPollConfigured = TRUE;
        PHDBG_INFO("Hal4:Poll Config Complete");
    }
    else
    {
        Hal4Ctxt->psADDCtxtInfo->IsPollConfigured = FALSE;
        PHDBG_WARNING("Hal4:Poll disabled,config success or config error");
    }
    if(NULL != Hal4Ctxt->sUpperLayerInfo.pConfigCallback)
    { 
#ifdef MERGE_SAK_SW2
        if((NFC_UICC_EMULATION == Hal4Ctxt->uConfig.emuConfig.emuType)&&
           (FALSE ==
            Hal4Ctxt->uConfig.emuConfig.config.uiccEmuCfg.enableUicc))
        {
            Status = phHciNfc_System_Configure (
                                Hal4Ctxt->psHciHandle,
                                (void *)gpphHal4Nfc_Hwref,
                                PH_HAL4NFC_TGT_MERGE_ADDRESS,
                                PH_HAL4NFC_TGT_MERGE_SAK /*config value*/
                                );
        }
        if(NFCSTATUS_PENDING != Status)
        {
#endif/*#ifdef MERGE_SAK_SW2*/
            Hal4Ctxt->Hal4NextState = eHal4StateInvalid;
            Hal4Ctxt->sUpperLayerInfo.pConfigCallback = NULL;
            (*pConfigCallback)(
#ifdef LLCP_DISCON_CHANGES
                Hal4Ctxt->sUpperLayerInfo.psUpperLayerCfgDiscCtxt,
#else /* #ifdef LLCP_DISCON_CHANGES */
                Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
#endif /* #ifdef LLCP_DISCON_CHANGES */
                Status
                );
#ifdef MERGE_SAK_SW2
        }
#endif/*#ifdef MERGE_SAK_SW2*/
    }
    /**if connect failed and discovery wheel was restarted*/
    else if(Hal4Ctxt->sTgtConnectInfo.pUpperConnectCb)
    {
        Hal4Ctxt->Hal4NextState = eHal4StateInvalid;
        Hal4Ctxt->sTgtConnectInfo.pUpperConnectCb = NULL;
        /*Notify to the upper layer*/
        (*pUpperConnectCb)(
            Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
            Hal4Ctxt->sTgtConnectInfo.psConnectedDevice,
            NFCSTATUS_FAILED
            );
    }
    else
    {
        Hal4Ctxt->Hal4NextState = eHal4StateInvalid;
        /**if disconnect failed and discovery wheel was restarted*/
        if ( NULL != Hal4Ctxt->sTgtConnectInfo.pUpperDisconnectCb)
        {
            ((phNfc_sCompletionInfo_t *)pInfo)->status = NFCSTATUS_SUCCESS;
            phHal4Nfc_DisconnectComplete(Hal4Ctxt,pInfo);
        }
    }
}


/**Handler for Target discovery completion for all remote device types*/
void phHal4Nfc_TargetDiscoveryComplete(
                                       phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                                       void                  *pInfo
                                       )
{
    static phHal4Nfc_DiscoveryInfo_t sDiscoveryInfo; 
    NFCSTATUS status = NFCSTATUS_SUCCESS;
    /**SAK byte*/
    uint8_t Sak = 0;
    /*Union type to encapsulate and return the discovery info*/
    phHal4Nfc_NotificationInfo_t uNotificationInfo;
    /*All the following types will be discovered as type A ,and differentiation 
      will have to be done within this module based on SAK byte and UID info*/
    phHal_eRemDevType_t  aRemoteDevTypes[3] = {
                                               phHal_eISO14443_A_PICC,
                                               phHal_eNfcIP1_Target,
                                               phHal_eMifare_PICC                                                 
                                              };
    /*Count is used to add multiple info into remote dvice list for devices that
      support multiple protocols*/
    uint8_t Count = 0,
        NfcIpDeviceCount = 0;/**<Number of NfcIp devices discovered*/
    uint16_t nfc_id = 0;
    /*remote device info*/
    phHal_sRemoteDevInformation_t *psRemoteDevInfo = NULL;
    status = ((phNfc_sCompletionInfo_t *)pInfo)->status;
    /*Update Hal4 state*/
    Hal4Ctxt->Hal4CurrentState = eHal4StateTargetDiscovered;
    Hal4Ctxt->Hal4NextState  = eHal4StateInvalid;
     PHDBG_INFO("Hal4:Remotedevice Discovered"); 
    if(NULL != ((phNfc_sCompletionInfo_t *)pInfo)->info)
    {
        /*Extract Remote device Info*/
        psRemoteDevInfo = (phHal_sRemoteDevInformation_t *)
                                    ((phNfc_sCompletionInfo_t *)pInfo)->info;

        switch(psRemoteDevInfo->RemDevType)
        {
            case phHal_eISO14443_A_PICC:/*for TYPE A*/
            {
                Sak = psRemoteDevInfo->RemoteDevInfo.Iso14443A_Info.Sak;
                if((Hal4Ctxt->psADDCtxtInfo->sCurrentPollConfig.EnableIso14443A)
                    || (TRUE == Hal4Ctxt->psADDCtxtInfo->smx_discovery))
                {
                    /*Check if Iso is Supported*/
                    if(Sak & ISO_14443_BITMASK)
                    {
                        Count++;
                    }
                    /*Check for Mifare Supported*/
                    switch( Sak )
                    {
                      case 0x01: // 1K Classic
                      case 0x09: // Mini
                      case 0x08: // 1K
                      case 0x18: // 4K
                      case 0x88: // Infineon 1K
                      case 0x98: // Pro 4K
                      case 0xB8: // Pro 4K
                      case 0x28: // 1K emulation
                      case 0x38: // 4K emulation
                        aRemoteDevTypes[Count] = phHal_eMifare_PICC;
                        Count++;
                        break;
                    }
                    if((0 == Sak)&& (0 == Count))
                    {
                        /*Mifare check*/
                        if((NXP_UID == 
                        psRemoteDevInfo->RemoteDevInfo.Iso14443A_Info.Uid[0])
                        &&(NXP_MIN_UID_LEN <= 
                        psRemoteDevInfo->RemoteDevInfo.Iso14443A_Info.UidLength))
                        {
                            aRemoteDevTypes[Count] = phHal_eMifare_PICC;
                            Count++;
                        }
                    }
                    // Always add a separate 3A target on a separate
                    // handle, so the upper layers can connect to it.
                    aRemoteDevTypes[Count] = phHal_eISO14443_3A_PICC;
                    Count++;
                }
                /*Check for P2P target passive*/
                if((Sak & NFCIP_BITMASK) && 
                    (NULL != Hal4Ctxt->sUpperLayerInfo.pP2PNotification)&&
                    (Hal4Ctxt->psADDCtxtInfo->sADDCfg.NfcIP_Mode 
                    & phHal_ePassive106))
                {
                  if( Sak == 0x53 // Fudan card incompatible to ISO18092
                      && psRemoteDevInfo->RemoteDevInfo.Iso14443A_Info.AtqA[0] == 0x04
                      && psRemoteDevInfo->RemoteDevInfo.Iso14443A_Info.AtqA[1] == 0x00
                    )
                  {
                    aRemoteDevTypes[Count] = phHal_eISO14443_3A_PICC;
                    Count++;
                  }
                  else
                  {
                    aRemoteDevTypes[Count] = phHal_eNfcIP1_Target;
                    Count++;
									}
                }
            }/*case phHal_eISO14443_A_PICC:*/
                break;
            case phHal_eNfcIP1_Target:/*P2P target detected*/
                aRemoteDevTypes[Count] = phHal_eNfcIP1_Target;
                Count++;
                break;
             case phHal_eISO14443_B_PICC: /*TYPE_B*/  
#ifdef TYPE_B
                aRemoteDevTypes[Count] = phHal_eISO14443_B_PICC;
                Count++;
                break;
#endif
            case phHal_eFelica_PICC: /*Felica*/
#ifdef TYPE_FELICA
            {
                /*nfc_id is used to differentiate between Felica and NfcIp target
                  discovered in Type F*/
                nfc_id = (((uint16_t)psRemoteDevInfo->RemoteDevInfo.Felica_Info.IDm[0])
                    << BYTE_SIZE) | 
                    psRemoteDevInfo->RemoteDevInfo.Felica_Info.IDm[1];
                /*check  for NfcIp target*/
                if(NXP_NFCIP_NFCID2_ID  == nfc_id)
                {
                    if((NULL != Hal4Ctxt->sUpperLayerInfo.pP2PNotification)
                        &&((Hal4Ctxt->psADDCtxtInfo->sADDCfg.NfcIP_Mode 
                             & phHal_ePassive212) || 
                            (Hal4Ctxt->psADDCtxtInfo->sADDCfg.NfcIP_Mode
                             & phHal_ePassive424)))
                    {
                        aRemoteDevTypes[Count] = phHal_eNfcIP1_Target;
                        Count++;
                    }
                }
                else/*Felica*/
                {
                    if(Hal4Ctxt->psADDCtxtInfo->sCurrentPollConfig.EnableFelica212
                    || Hal4Ctxt->psADDCtxtInfo->sCurrentPollConfig.EnableFelica424)
                    {
                        aRemoteDevTypes[Count] = phHal_eFelica_PICC;
                        Count++;                    
                    }
                }
                break;
            }
#endif             
            case phHal_eJewel_PICC: /*Jewel*/
#ifdef TYPE_JEWEL
            {
                /*Report Jewel tags only if TYPE A is enabled*/
                if(Hal4Ctxt->psADDCtxtInfo->sCurrentPollConfig.EnableIso14443A)
                {
                    aRemoteDevTypes[Count] = phHal_eJewel_PICC;
                    Count++;                    
                }
                break;
            }               
#endif
#ifdef  TYPE_ISO15693
            case phHal_eISO15693_PICC: /*ISO15693*/
            {
                if(Hal4Ctxt->psADDCtxtInfo->sCurrentPollConfig.EnableIso15693)
                {
                    aRemoteDevTypes[Count] = phHal_eISO15693_PICC;
                    Count++;                    
                }
                break;
            }               
#endif /* #ifdef    TYPE_ISO15693 */
            /*Types currently not supported*/
            case phHal_eISO14443_BPrime_PICC:
            default:
                PHDBG_WARNING("Hal4:Notification for Not supported types");
                break;
        }/*End of switch*/
        /*Update status code to success if atleast one device info is available*/
        status = (((NFCSTATUS_SUCCESS != status) 
                  && (NFCSTATUS_MULTIPLE_TAGS != status))
                   &&(Hal4Ctxt->psADDCtxtInfo->nbr_of_devices != 0))?
                    NFCSTATUS_SUCCESS:status;
        
        /*Update status to NFCSTATUS_MULTIPLE_PROTOCOLS if count > 1 ,and this
          is first discovery notification from Hci*/
        status = ((NFCSTATUS_SUCCESS == status)
                    &&(Hal4Ctxt->psADDCtxtInfo->nbr_of_devices == 0)
                    &&(Count > 1)?NFCSTATUS_MULTIPLE_PROTOCOLS:status);
         /*If multiple protocols are supported ,allocate separate remote device
          information for each protocol supported*/
        /*Allocate and copy Remote device info into Hal4 Context*/
        while(Count)
        {
            PHDBG_INFO("Hal4:Count is not zero"); 
            --Count;
            /*Allocate memory for each of Count number of 
              devices*/
            if(NULL == Hal4Ctxt->rem_dev_list[
                Hal4Ctxt->psADDCtxtInfo->nbr_of_devices])
            {
                Hal4Ctxt->rem_dev_list[
                    Hal4Ctxt->psADDCtxtInfo->nbr_of_devices] 
                = (phHal_sRemoteDevInformation_t *)
                    phOsalNfc_GetMemory(
                    (uint32_t)(
                    sizeof(phHal_sRemoteDevInformation_t))
                    );
            }
            if(NULL == Hal4Ctxt->rem_dev_list[
                Hal4Ctxt->psADDCtxtInfo->nbr_of_devices])
            {
                status =  PHNFCSTVAL(CID_NFC_HAL,
                    NFCSTATUS_INSUFFICIENT_RESOURCES);
                phOsalNfc_RaiseException(phOsalNfc_e_NoMemory,0);
                break;
            }
            else
            {
                (void)memcpy(
                    (void *)Hal4Ctxt->rem_dev_list[
                        Hal4Ctxt->psADDCtxtInfo->nbr_of_devices],
                        (void *)psRemoteDevInfo,
                        sizeof(phHal_sRemoteDevInformation_t)
                        );
                /*Now copy appropriate device type from aRemoteDevTypes array*/
                Hal4Ctxt->rem_dev_list[
                        Hal4Ctxt->psADDCtxtInfo->nbr_of_devices]->RemDevType 
                            =   aRemoteDevTypes[Count];
                /*Increment number of devices*/
                Hal4Ctxt->psADDCtxtInfo->nbr_of_devices++;                            
            }/*End of else*/
        }/*End of while*/
        
        /*If Upper layer is interested only in P2P notifications*/
        if((NULL != Hal4Ctxt->sUpperLayerInfo.pP2PNotification)
           &&(((Hal4Ctxt->psADDCtxtInfo->nbr_of_devices == 1)
              &&(phHal_eNfcIP1_Target == Hal4Ctxt->rem_dev_list[0]->RemDevType))
              ||(NULL == Hal4Ctxt->sUpperLayerInfo.pTagDiscoveryNotification))
            )
        {
            PHDBG_INFO("Hal4:Trying to notify P2P Listener"); 
            /*NFCSTATUS_SUCCESS or NFCSTATUS_MULTIPLE_PROTOCOLS*/
            if((NFCSTATUS_SUCCESS == status) 
                ||(NFCSTATUS_MULTIPLE_PROTOCOLS == status))
            {
                /*Pick only the P2P target device info from the list*/
                for(Count = Hal4Ctxt->psADDCtxtInfo->nbr_of_devices;
                    Count > 0;--Count)
                {
                    /*Only one P2P target can be detected in one discovery*/
                    if(phHal_eNfcIP1_Target == 
                        Hal4Ctxt->rem_dev_list[Count-1]->RemDevType)
                    {
                        if (Count != 1)
                        {
                            (void)memcpy(
                                (void *)Hal4Ctxt->rem_dev_list[0],
                                (void *)Hal4Ctxt->rem_dev_list[Count-1],
                                        sizeof(phHal_sRemoteDevInformation_t)
                                        );
                        }
                        NfcIpDeviceCount = 1;                       
                        break;
                    }
                }
                /*If any P2p devices are discovered free other device info*/
                while(Hal4Ctxt->psADDCtxtInfo->nbr_of_devices > NfcIpDeviceCount)
                {
                    phOsalNfc_FreeMemory(Hal4Ctxt->rem_dev_list[
                        --Hal4Ctxt->psADDCtxtInfo->nbr_of_devices]);
                    Hal4Ctxt->rem_dev_list[
                        Hal4Ctxt->psADDCtxtInfo->nbr_of_devices] = NULL;
                }
                /*Issue P2P notification*/
                if(NfcIpDeviceCount == 1)
                {
                    sDiscoveryInfo.NumberOfDevices 
                        = Hal4Ctxt->psADDCtxtInfo->nbr_of_devices;
                    sDiscoveryInfo.ppRemoteDevInfo = Hal4Ctxt->rem_dev_list;
                    uNotificationInfo.psDiscoveryInfo = &sDiscoveryInfo;
                    PHDBG_INFO("Hal4:Calling P2P listener");
                    (*Hal4Ctxt->sUpperLayerInfo.pP2PNotification)(
                            (void *)(Hal4Ctxt->sUpperLayerInfo.P2PDiscoveryCtxt),
                            NFC_DISCOVERY_NOTIFICATION,
                            uNotificationInfo,
                            NFCSTATUS_SUCCESS
                            );
                }
                else/*Restart Discovery wheel*/ 
                {
                    PHDBG_INFO("Hal4:No P2P device in list");  
                    Hal4Ctxt->psADDCtxtInfo->nbr_of_devices = 0;
                    PHDBG_INFO("Hal4:Restart discovery1"); 
                    status = phHciNfc_Restart_Discovery (
                                        (void *)Hal4Ctxt->psHciHandle,
                                        (void *)gpphHal4Nfc_Hwref,      
                                        FALSE
                                        );
                    Hal4Ctxt->Hal4NextState = (NFCSTATUS_PENDING == status?
                                                        eHal4StateConfiguring:
                                                    Hal4Ctxt->Hal4NextState);
                }
            }
            /*More discovery info available ,get next info from HCI*/
            else if((NFCSTATUS_MULTIPLE_TAGS == status)
                &&(Hal4Ctxt->psADDCtxtInfo->nbr_of_devices 
                     < MAX_REMOTE_DEVICES))
            {
                status = phHciNfc_Select_Next_Target (
                    Hal4Ctxt->psHciHandle,
                    (void *)gpphHal4Nfc_Hwref
                    );
            }
            else/*Failed discovery ,restart discovery*/
            {
                Hal4Ctxt->psADDCtxtInfo->nbr_of_devices = 0;
                PHDBG_INFO("Hal4:Restart discovery2"); 
                status = phHciNfc_Restart_Discovery (
                                        (void *)Hal4Ctxt->psHciHandle,
                                        (void *)gpphHal4Nfc_Hwref,      
                                        FALSE
                                        );/*Restart Discovery wheel*/   
                Hal4Ctxt->Hal4NextState = (NFCSTATUS_PENDING == status?
                                                eHal4StateConfiguring:
                                                Hal4Ctxt->Hal4NextState);
            }
        }/*if((NULL != Hal4Ctxt->sUpperLayerInfo.pP2PNotification)...*/
        /*Notify if Upper layer is interested in tag notifications,also notify
          P2p if its in the list with other tags*/
        else if(NULL != Hal4Ctxt->sUpperLayerInfo.pTagDiscoveryNotification)
        {
            PHDBG_INFO("Hal4:Trying to notify Tag notification");
            /*Multiple tags in field, get discovery info a second time for the 
              other devices*/
            if((NFCSTATUS_MULTIPLE_TAGS == status)
                &&(Hal4Ctxt->psADDCtxtInfo->nbr_of_devices < MAX_REMOTE_DEVICES))
            {
                PHDBG_INFO("Hal4:select next target1"); 
                status = phHciNfc_Select_Next_Target (
                                                    Hal4Ctxt->psHciHandle,
                                                    (void *)gpphHal4Nfc_Hwref
                                                    );
            }
            /*Single tag multiple protocols scenario,Notify Multiple Protocols 
              status to upper layer*/
            else if(status == NFCSTATUS_MULTIPLE_PROTOCOLS) 
            {
                PHDBG_INFO("Hal4:Multiple Tags or protocols");
                sDiscoveryInfo.NumberOfDevices 
                    = Hal4Ctxt->psADDCtxtInfo->nbr_of_devices;
                sDiscoveryInfo.ppRemoteDevInfo = Hal4Ctxt->rem_dev_list;
                uNotificationInfo.psDiscoveryInfo = &sDiscoveryInfo;
                (*Hal4Ctxt->sUpperLayerInfo.pTagDiscoveryNotification)(
                            (void *)(Hal4Ctxt->sUpperLayerInfo.DiscoveryCtxt),
                            NFC_DISCOVERY_NOTIFICATION,
                            uNotificationInfo,
                            status
                            );
            }
            else /*NFCSTATUS_SUCCESS*/
            {                
                if(((Hal4Ctxt->psADDCtxtInfo->nbr_of_devices == 1)
                     &&(phHal_eNfcIP1_Target 
                     == Hal4Ctxt->rem_dev_list[0]->RemDevType))
                     ||(NFCSTATUS_SUCCESS != status)
                     || (Hal4Ctxt->psADDCtxtInfo->nbr_of_devices == 0)
                     )/*device detected but upper layer is not interested 
                       in the type(P2P) or activate next failed*/
                {
                    while(Hal4Ctxt->psADDCtxtInfo->nbr_of_devices > 0)
                    {
                        phOsalNfc_FreeMemory(Hal4Ctxt->rem_dev_list[
                            --Hal4Ctxt->psADDCtxtInfo->nbr_of_devices]);
                        Hal4Ctxt->rem_dev_list[
                            Hal4Ctxt->psADDCtxtInfo->nbr_of_devices] = NULL;
                    }
                    PHDBG_INFO("Hal4:Restart discovery3"); 
                    status = phHciNfc_Restart_Discovery (
                        (void *)Hal4Ctxt->psHciHandle,
                        (void *)gpphHal4Nfc_Hwref,      
                        FALSE
                        );/*Restart Discovery wheel*/ 
                    Hal4Ctxt->Hal4NextState = (
                        NFCSTATUS_PENDING == status?eHal4StateConfiguring
                                                    :Hal4Ctxt->Hal4NextState
                                                    );
                }
                else/*All remote device info available.Notify to upper layer*/
                {
                    /*Update status for MULTIPLE_TAGS here*/
                    status = (Hal4Ctxt->psADDCtxtInfo->nbr_of_devices > 1?
                                NFCSTATUS_MULTIPLE_TAGS:status);    
                    /*If listener is registered ,call it*/
                    sDiscoveryInfo.NumberOfDevices 
                        = Hal4Ctxt->psADDCtxtInfo->nbr_of_devices;
                    sDiscoveryInfo.ppRemoteDevInfo
                        = Hal4Ctxt->rem_dev_list;
                    uNotificationInfo.psDiscoveryInfo = &sDiscoveryInfo;                    
                    PHDBG_INFO("Hal4:Calling Discovery Handler1");
                    (*Hal4Ctxt->sUpperLayerInfo.pTagDiscoveryNotification)(
                        (void *)(Hal4Ctxt->sUpperLayerInfo.DiscoveryCtxt),
                        NFC_DISCOVERY_NOTIFICATION,
                        uNotificationInfo,
                        status
                        );
                }
            }       
        } /*else if(NULL != Hal4Ctxt->sUpperLayerInfo.pTagDiscoveryNotification)*/      
        else/*listener not registered ,Restart Discovery wheel*/ 
        {   
            PHDBG_INFO("Hal4:No listener registered.Ignoring Discovery  \
                        Notification");  
            Hal4Ctxt->psADDCtxtInfo->nbr_of_devices = 0;
            PHDBG_INFO("Hal4:Restart discovery4"); 
            status = phHciNfc_Restart_Discovery (
                                (void *)Hal4Ctxt->psHciHandle,
                                (void *)gpphHal4Nfc_Hwref,      
                                FALSE
                                );
            Hal4Ctxt->Hal4NextState = (NFCSTATUS_PENDING == status?
                                                eHal4StateConfiguring:
                                            Hal4Ctxt->Hal4NextState);
        }
    }/*if(NULL != ((phNfc_sCompletionInfo_t *)pInfo)->info)*/
    else/*NULL info received*/
    {
        sDiscoveryInfo.NumberOfDevices 
            = Hal4Ctxt->psADDCtxtInfo->nbr_of_devices;
        sDiscoveryInfo.ppRemoteDevInfo = Hal4Ctxt->rem_dev_list;
        uNotificationInfo.psDiscoveryInfo = &sDiscoveryInfo;
        /*If Discovery info is available from previous notifications try to 
          notify that to the upper layer*/
        if((NULL != Hal4Ctxt->sUpperLayerInfo.pTagDiscoveryNotification)
#ifdef NFC_RF_NOISE_SW
          &&((NFCSTATUS_SUCCESS == status) 
            || (NFCSTATUS_MULTIPLE_TAGS == status))
#endif /* #ifdef NFC_RF_NOISE_SW */
            )
        {
#ifndef NFC_RF_NOISE_SW           
            status = (((NFCSTATUS_SUCCESS != status) 
                  && (NFCSTATUS_MULTIPLE_TAGS != status))
                   &&(Hal4Ctxt->psADDCtxtInfo->nbr_of_devices != 0))?
                    NFCSTATUS_SUCCESS:status;
#endif/*#ifndef NFC_RF_NOISE_SW*/
            PHDBG_INFO("Hal4:Calling Discovery Handler2");
            (*Hal4Ctxt->sUpperLayerInfo.pTagDiscoveryNotification)(
                (void *)(Hal4Ctxt->sUpperLayerInfo.DiscoveryCtxt),              
                NFC_DISCOVERY_NOTIFICATION,
                uNotificationInfo,
                status
                );
        }
        else/*Restart Discovery wheel*/   
        {
            Hal4Ctxt->psADDCtxtInfo->nbr_of_devices = 0;
            PHDBG_INFO("Hal4:Restart discovery5"); 
            status = phHciNfc_Restart_Discovery (
                (void *)Hal4Ctxt->psHciHandle,
                (void *)gpphHal4Nfc_Hwref,      
                FALSE
                );
            Hal4Ctxt->Hal4NextState = (NFCSTATUS_PENDING == status?
                                   eHal4StateConfiguring:Hal4Ctxt->Hal4NextState);
        }
    }/*else*/
    return;
}


/**Register Notification handlers*/
NFCSTATUS phHal4Nfc_RegisterNotification(                       
                        phHal_sHwReference_t         *psHwReference,
                        phHal4Nfc_RegisterType_t      eRegisterType, 
                        pphHal4Nfc_Notification_t     pNotificationHandler,
                        void                         *Context
                        )
{
    NFCSTATUS RetStatus = NFCSTATUS_SUCCESS;
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL;
    if(NULL == pNotificationHandler || NULL == psHwReference)
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        RetStatus = PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHwReference->hal_context)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4CurrentState 
                                               < eHal4StateOpenAndReady)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4NextState 
                                               == eHal4StateClosed))
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        RetStatus = PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_NOT_INITIALISED);
    }
    else
    {
        /*Extract context from hardware reference*/
        Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)psHwReference->hal_context;
        switch(eRegisterType)
        {
            case eRegisterTagDiscovery:
                Hal4Ctxt->sUpperLayerInfo.DiscoveryCtxt = Context;
                Hal4Ctxt->sUpperLayerInfo.pTagDiscoveryNotification
                    = pNotificationHandler;  /*Register the tag Notification*/
                break;
            case eRegisterP2PDiscovery:
                Hal4Ctxt->sUpperLayerInfo.P2PDiscoveryCtxt = Context;
                Hal4Ctxt->sUpperLayerInfo.pP2PNotification
                    = pNotificationHandler;  /*Register the P2P Notification*/
                break;
            case eRegisterHostCardEmulation:
                RetStatus = NFCSTATUS_FEATURE_NOT_SUPPORTED;
                break;
            case eRegisterSecureElement:
                Hal4Ctxt->sUpperLayerInfo.EventNotificationCtxt = Context;
                Hal4Ctxt->sUpperLayerInfo.pEventNotification 
                       = pNotificationHandler; /*Register the Se Notification*/
                break;
            default:
                Hal4Ctxt->sUpperLayerInfo.DefaultListenerCtxt = Context;
                Hal4Ctxt->sUpperLayerInfo.pDefaultEventHandler
                    = pNotificationHandler;  /*Register the default Notification*/
                break;
        }
        PHDBG_INFO("Hal4:listener registered");
    }
    return RetStatus;
}


/**Unregister Notification handlers*/
NFCSTATUS phHal4Nfc_UnregisterNotification(                               
                                  phHal_sHwReference_t     *psHwReference,
                                  phHal4Nfc_RegisterType_t  eRegisterType,
                                  void                     *Context
                                  )
{
    NFCSTATUS RetStatus = NFCSTATUS_SUCCESS;
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL;
    if(psHwReference == NULL)
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        RetStatus = PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHwReference->hal_context)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4CurrentState 
                                               < eHal4StateOpenAndReady)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4NextState 
                                               == eHal4StateClosed))
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        RetStatus = PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_NOT_INITIALISED);
    }
    else
    {
        /*Extract context from hardware reference*/
        Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)psHwReference->hal_context;
        switch(eRegisterType)
        {
        case eRegisterTagDiscovery:
            Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt = Context;
            Hal4Ctxt->sUpperLayerInfo.DiscoveryCtxt = NULL;
            /*UnRegister the tag Notification*/
            Hal4Ctxt->sUpperLayerInfo.pTagDiscoveryNotification = NULL;  
            PHDBG_INFO("Hal4:Tag Discovery Listener Unregistered");
            break;
        case eRegisterP2PDiscovery:
            Hal4Ctxt->sUpperLayerInfo.P2PDiscoveryCtxt = NULL;
            /*UnRegister the p2p Notification*/
            Hal4Ctxt->sUpperLayerInfo.pP2PNotification = NULL;  
            PHDBG_INFO("Hal4:P2P Discovery Listener Unregistered");
            break;            
        case eRegisterHostCardEmulation:/*RFU*/
            RetStatus = NFCSTATUS_FEATURE_NOT_SUPPORTED;
            break;
            /*UnRegister the Se Notification*/
        case eRegisterSecureElement:
            Hal4Ctxt->sUpperLayerInfo.EventNotificationCtxt = NULL;
            Hal4Ctxt->sUpperLayerInfo.pEventNotification = NULL;
            PHDBG_INFO("Hal4:SE Listener Unregistered");
            break;
        default:
            Hal4Ctxt->sUpperLayerInfo.DefaultListenerCtxt = NULL;
            /*UnRegister the default Notification*/
            Hal4Ctxt->sUpperLayerInfo.pDefaultEventHandler = NULL;  
            PHDBG_INFO("Hal4:Default Listener Unregistered");
            break;
        }
    }
    return RetStatus;
}


