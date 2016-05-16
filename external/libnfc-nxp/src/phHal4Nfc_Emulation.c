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
* \file  phHal4Nfc_Emulation.c
* \brief Hal4 Emulation source.
*
* Project: NFC-FRI 1.1
*
* $Date: Wed May 26 18:03:59 2010 $
* $Author: ing07385 $
* $Revision: 1.35 $
* $Aliases: NFC_FRI1.1_WK1023_R35_1 $
*
*/

/* ---------------------------Include files ------------------------------------*/
#include <phHciNfc.h>
#include <phHal4Nfc.h>
#include <phHal4Nfc_Internal.h>
#include <phOsalNfc.h>

/* ------------------------------- Macros ------------------------------------*/

/* Note : Macros required and used  only in this module to be declared here*/


/* --------------------Structures and enumerations --------------------------*/


/*Event Notification handler for emulation*/
void phHal4Nfc_HandleEmulationEvent(
                                    phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                                    void *pInfo
                                    )
{
    phNfc_sNotificationInfo_t *psNotificationInfo = (phNfc_sNotificationInfo_t *)
                                                                            pInfo;
    phHal4Nfc_NotificationInfo_t uNotificationInfo = {NULL};
    /*Pass on Event notification info from Hci to Upper layer*/
    uNotificationInfo.psEventInfo = psNotificationInfo->info;
    if(NULL != Hal4Ctxt->sUpperLayerInfo.pEventNotification)
    {        
        Hal4Ctxt->sUpperLayerInfo.pEventNotification(
            Hal4Ctxt->sUpperLayerInfo.EventNotificationCtxt,
            psNotificationInfo->type,
            uNotificationInfo,
            NFCSTATUS_SUCCESS
            );
    }
    else/*No Event notification handler registered*/
    {
        /*Use default handler to notify to the upper layer*/
        if(NULL != Hal4Ctxt->sUpperLayerInfo.pDefaultEventHandler)
        {
            Hal4Ctxt->sUpperLayerInfo.pDefaultEventHandler(
                Hal4Ctxt->sUpperLayerInfo.DefaultListenerCtxt,
                psNotificationInfo->type,
                uNotificationInfo,
                NFCSTATUS_SUCCESS
                );
        }
    }
    return;
}

/*  Switch mode from Virtual to Wired or Vice Versa for SMX.
*/
NFCSTATUS phHal4Nfc_Switch_SMX_Mode(                                    
                                    phHal_sHwReference_t      *psHwReference,
                                    phHal_eSmartMX_Mode_t      smx_mode,
                                    pphHal4Nfc_GenCallback_t   pSwitchModecb,
                                    void                      *pContext
                                    )
{
    NFCSTATUS CfgStatus = NFCSTATUS_PENDING;
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL;
    static phHal_sADD_Cfg_t sSmxCfg;
    
    /*NULL  checks*/
    if((NULL == psHwReference) || (NULL == pSwitchModecb))
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        CfgStatus = PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_INVALID_PARAMETER);
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
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        CfgStatus = PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_NOT_INITIALISED);
    }
    else
    {
        Hal4Ctxt = psHwReference->hal_context;
        /*Previous POLL Config has not completed or device is connected,
          do not allow poll*/
        if(Hal4Ctxt->Hal4NextState == eHal4StateConfiguring)
        {
            PHDBG_INFO("Hal4:Configuration in progress.Returning status Busy");
            CfgStatus= PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_BUSY);
        }
        else if(Hal4Ctxt->Hal4CurrentState >= eHal4StateOpenAndReady)
        {
            /**If config discovery has not been called prior to this ,allocate 
               ADD Context here*/
            if (NULL == Hal4Ctxt->psADDCtxtInfo)
            {
                Hal4Ctxt->psADDCtxtInfo= (pphHal4Nfc_ADDCtxtInfo_t)
                    phOsalNfc_GetMemory((uint32_t)
                                        (sizeof(phHal4Nfc_ADDCtxtInfo_t)));
                if(NULL != Hal4Ctxt->psADDCtxtInfo)
                {
                    (void)memset(Hal4Ctxt->psADDCtxtInfo,
                                    0,sizeof(phHal4Nfc_ADDCtxtInfo_t));
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
                /* Switch request to Wired mode */
                if(eSmartMx_Wired == smx_mode)
                {
                    if(Hal4Ctxt->Hal4CurrentState 
                                    == eHal4StateTargetConnected)
                    {
                        PHDBG_INFO("Hal4:In Connected state.Returning Busy");
                        CfgStatus= PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_BUSY);
                    }
                    /*It is Mandatory to register a listener before switching 
                      to wired mode*/
                    else if(NULL ==
                            Hal4Ctxt->sUpperLayerInfo.pTagDiscoveryNotification)
                    {
                        CfgStatus = PHNFCSTVAL(CID_NFC_HAL , 
                            NFCSTATUS_FAILED);
                    }
                    else
                    {
                        Hal4Ctxt->psADDCtxtInfo->nbr_of_devices = 0;
                        Hal4Ctxt->psADDCtxtInfo->smx_discovery = TRUE;
                        sSmxCfg.PollDevInfo.PollCfgInfo.EnableIso14443A = TRUE;
                        sSmxCfg.PollDevInfo.PollCfgInfo.DisableCardEmulation = TRUE;
                        /*Switch mode to wired*/
                        CfgStatus = phHciNfc_Switch_SmxMode (
                                                    Hal4Ctxt->psHciHandle,
                                                    psHwReference,
                                                    smx_mode,
                                                    &sSmxCfg
                                                    );
                    }
                }
                else
                {
                    Hal4Ctxt->psADDCtxtInfo->smx_discovery = FALSE;
                    /*Switch mode to virtual or off*/
                    CfgStatus = phHciNfc_Switch_SmxMode (
                                        Hal4Ctxt->psHciHandle,
                                        psHwReference,
                                        smx_mode,
                                        &(Hal4Ctxt->psADDCtxtInfo->sADDCfg)
                                        );
                }

                /* Change the State of the HAL only if Switch mode Returns
                   Success*/
                if ( NFCSTATUS_PENDING == CfgStatus )
                {
                    Hal4Ctxt->Hal4NextState = eHal4StateConfiguring;
                    Hal4Ctxt->sUpperLayerInfo.pConfigCallback
                        = pSwitchModecb;
                }
            }           
        }
        else/*Return Status not initialised*/
        {
            phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
            CfgStatus= PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_NOT_INITIALISED);
        }
    }
    return CfgStatus;
}



/*  Switch mode for Swp.*/
NFCSTATUS phHal4Nfc_Switch_Swp_Mode(                                    
                                    phHal_sHwReference_t      *psHwReference,
                                    phHal_eSWP_Mode_t          swp_mode,
                                    pphHal4Nfc_GenCallback_t   pSwitchModecb,
                                    void                      *pContext
                                    )
{
    NFCSTATUS CfgStatus = NFCSTATUS_PENDING;
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL; 
    /*NULL checks*/
    if(NULL == psHwReference  
        || NULL == pSwitchModecb
        )
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        CfgStatus = PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_INVALID_PARAMETER);
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
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        CfgStatus = PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_NOT_INITIALISED);
    }
    else
    {
        Hal4Ctxt = psHwReference->hal_context;
        /*Previous POLL CFG has not completed or device is connected,
          do not allow poll*/
        if(Hal4Ctxt->Hal4NextState == eHal4StateConfiguring)
        {
            PHDBG_INFO("Hal4:Configuration in progress.Returning status Busy");
            CfgStatus= PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_BUSY);
        }
        else if(Hal4Ctxt->Hal4CurrentState >= eHal4StateOpenAndReady)
        {
             /**If config discovery has not been called prior to this ,allocate 
               ADD Context here*/
            if (NULL == Hal4Ctxt->psADDCtxtInfo)
            {
                Hal4Ctxt->psADDCtxtInfo= (pphHal4Nfc_ADDCtxtInfo_t)
                    phOsalNfc_GetMemory((uint32_t)
                                        (sizeof(phHal4Nfc_ADDCtxtInfo_t)));
                if(NULL != Hal4Ctxt->psADDCtxtInfo)
                {
                    (void)memset(Hal4Ctxt->psADDCtxtInfo,
                                    0,sizeof(phHal4Nfc_ADDCtxtInfo_t));
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
                /*Switch mode to On or off*/
                CfgStatus = phHciNfc_Switch_SwpMode(
                                    Hal4Ctxt->psHciHandle,
                                    psHwReference,
                                    swp_mode
                                    );

                /* Change the State of the HAL only if Switch mode Returns
                   Success*/
                if ( NFCSTATUS_PENDING == CfgStatus )
                {
                    Hal4Ctxt->Hal4NextState = eHal4StateConfiguring;
                    Hal4Ctxt->sUpperLayerInfo.pConfigCallback
                        = pSwitchModecb;
                }
            }           
        }
        else/*Return Status not initialised*/
        {
            phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
            CfgStatus= PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_NOT_INITIALISED);
        }
    }
    return CfgStatus;
}

#ifdef FULL_HAL4_EMULATION_ENABLE
/*  Switch Emulation mode ON or OFF.*/
NFCSTATUS phHal4Nfc_Host_Emulation_Mode( 
                                        phHal_sHwReference_t      *psHwReference,
                                        phNfc_eModeType_t          eModeType,
                                        pphHal4Nfc_GenCallback_t   pEmulationModecb,
                                        void                      *pContext
                                        )
{
    NFCSTATUS RetStatus = NFCSTATUS_PENDING;
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL;
    /*NULL checks*/
    if(NULL == psHwReference  
        || NULL == pEmulationModecb
        )
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        RetStatus = PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_INVALID_PARAMETER);
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
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        RetStatus = PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_NOT_INITIALISED);
    }    
    else
    {

    }
    return NFCSTATUS_PENDING;
}
#endif /*FULL_HAL4_EMULATION_ENABLE*/
