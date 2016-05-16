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
 * \file phLibNfc_discovery.c

 * Project: NFC FRI 1.1
 *
 * $Date: Mon Mar  1 19:02:41 2010 $
 * $Author: ing07385 $
 * $Revision: 1.36 $
 * $Aliases: NFC_FRI1.1_WK1008_SDK,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1007_SDK,NFC_FRI1.1_WK1014_SDK,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1019_SDK,NFC_FRI1.1_WK1024_SDK $
 *
 */

/*
************************* Header Files ****************************************
*/

#include <phLibNfcStatus.h>
#include <phLibNfc.h>
#include <phHal4Nfc.h>
#include <phOsalNfc.h>
#include <phLibNfc_Internal.h>
#include <phLibNfc_ndef_raw.h>
#include <phLibNfc_initiator.h>
#include <phLibNfc_discovery.h>

/*
*************************** Macro's  ****************************************
*/

#ifndef STATIC_DISABLE
#define STATIC static
#else
#define STATIC
#endif

/*
*************************** Global Variables **********************************
*/



/*
*************************** Static Function Declaration ***********************
*/


/*Remote device Presence check callback*/
STATIC void phLibNfc_RemoteDev_CheckPresence_Cb(void  *context,
                                NFCSTATUS status);

/**Used for presence chk incase of mifare std tags*/
STATIC void phLibNfc_ChkPresence_Trcv_Cb(
                            void *context,
                            phHal_sRemoteDevInformation_t *psRemoteDevInfo,
                            phNfc_sData_t *response, 
                            NFCSTATUS status
                            );

/*
*************************** Function Definitions ******************************
*/
void phLibNfc_config_discovery_cb(void     *context,
                                  NFCSTATUS status)
{
    
    if((phLibNfc_LibContext_t *)context == gpphLibContext)      
    {   /*check for same context*/
        
        if(eLibNfcHalStateShutdown == gpphLibContext->LibNfcState.next_state)
        {
            /*If shutdown called in between allow shutdown to happen*/
            phLibNfc_Pending_Shutdown();
            status = NFCSTATUS_SHUTDOWN;
        }
        else
        {
            gpphLibContext->status.GenCb_pending_status = FALSE;
            gpphLibContext->status.DiscEnbl_status = FALSE;
            phLibNfc_UpdateCurState(status,gpphLibContext);
#ifdef RESTART_CFG
            if(gpphLibContext->status.Discovery_pending_status == TRUE)
            {
                NFCSTATUS RetStatus = NFCSTATUS_FAILED;
                /* Application has called discovery before receiving this callback,
                so NO notification to the upper layer, instead lower layer
                discovery is called */
                gpphLibContext->status.Discovery_pending_status = FALSE;
                RetStatus =  phHal4Nfc_ConfigureDiscovery(
                        gpphLibContext->psHwReference,
                        gpphLibContext->eLibNfcCfgMode,
                        &gpphLibContext->sADDconfig,
                        (pphLibNfc_RspCb_t)
                        phLibNfc_config_discovery_cb,
                        (void *)gpphLibContext);
                if (NFCSTATUS_PENDING == RetStatus)
                {
                    (void)phLibNfc_UpdateNextState(gpphLibContext,
                                            eLibNfcHalStateConfigReady);
                    gpphLibContext->status.GenCb_pending_status = TRUE;
                    gpphLibContext->status.DiscEnbl_status = TRUE;
                }
                else
                {
                    status = NFCSTATUS_FAILED;
                }
            }
#endif /* #ifdef RESTART_CFG */
        }
    } /*End of if-context check*/
    else
    {   /*exception: wrong context pointer returned*/
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
        status = NFCSTATUS_FAILED;
    }
    if(gpphLibContext->CBInfo.pClientDisConfigCb!=NULL)
    {
        gpphLibContext->CBInfo.pClientDisConfigCb(gpphLibContext->CBInfo.pClientDisCfgCntx,status);
        gpphLibContext->CBInfo.pClientDisConfigCb=NULL;
    }
    return;
}
/**
* Configure Discovery Modes.
* This function is used to configure ,start and stop the discovery wheel.
*/
NFCSTATUS phLibNfc_Mgt_ConfigureDiscovery (
                        phLibNfc_eDiscoveryConfigMode_t DiscoveryMode,   
                        phLibNfc_sADD_Cfg_t             sADDSetup,
                        pphLibNfc_RspCb_t               pConfigDiscovery_RspCb,
                        void*                           pContext
                        )
 {
    NFCSTATUS RetVal = NFCSTATUS_FAILED;
    phHal_sADD_Cfg_t           *psADDConfig;
    psADDConfig = (phHal_sADD_Cfg_t *)&(sADDSetup);

    
   if((NULL == gpphLibContext) ||
        (gpphLibContext->LibNfcState.cur_state
                            == eLibNfcHalStateShutdown))
    {
        /*Lib Nfc not initialized*/
        RetVal = NFCSTATUS_NOT_INITIALISED;
    }
    /* Check for Valid parameters*/
    else if((NULL == pContext) || (NULL == pConfigDiscovery_RspCb))
    {
        RetVal= NFCSTATUS_INVALID_PARAMETER;
    }
    else if(gpphLibContext->LibNfcState.next_state
                            == eLibNfcHalStateShutdown)
    {
        RetVal= NFCSTATUS_SHUTDOWN;
    }    
    else
    {
        gpphLibContext->eLibNfcCfgMode =DiscoveryMode;
        gpphLibContext->sADDconfig = sADDSetup;
        if(gpphLibContext->status.DiscEnbl_status != TRUE)
        {
            
            /* call lower layer config API for the discovery 
            configuration sent by the application */
            RetVal = phHal4Nfc_ConfigureDiscovery ( gpphLibContext->psHwReference,
                                        DiscoveryMode,
                                        psADDConfig,
                                        (pphLibNfc_RspCb_t)
                                        phLibNfc_config_discovery_cb,
                                        (void*)gpphLibContext);
            if(PHNFCSTATUS(RetVal) == NFCSTATUS_PENDING)
            {
                gpphLibContext->status.DiscEnbl_status = TRUE;
                /* Copy discovery callback and its context */
                gpphLibContext->CBInfo.pClientDisConfigCb = pConfigDiscovery_RspCb;
                gpphLibContext->CBInfo.pClientDisCfgCntx = pContext;
                gpphLibContext->status.GenCb_pending_status = TRUE;
				gpphLibContext->LibNfcState.next_state = eLibNfcHalStateConfigReady;                
            }
            else
            {
                if (PHNFCSTATUS(RetVal) == NFCSTATUS_BUSY)
                {
                    RetVal = NFCSTATUS_BUSY;
                }
                else
                {
                    RetVal=NFCSTATUS_FAILED;
                }
            }

        }
        else
        {
            RetVal=NFCSTATUS_BUSY;            
        }
    }
    return RetVal;
 }

/**
* Check for target presence.
* Checks given target is present in RF filed or not
*/
NFCSTATUS phLibNfc_RemoteDev_CheckPresence( phLibNfc_Handle     hTargetDev,
                                            pphLibNfc_RspCb_t   pPresenceChk_RspCb,
                                            void*               pRspCbCtx
                                           )
{
    NFCSTATUS RetVal = NFCSTATUS_FAILED;
    phHal_sRemoteDevInformation_t *ps_rem_dev_info = NULL;
    /* Check for valid sate */
    if((NULL == gpphLibContext) ||
        (gpphLibContext->LibNfcState.cur_state
                            == eLibNfcHalStateShutdown))
    {
        RetVal = NFCSTATUS_NOT_INITIALISED;
    }
    /* Check for valid parameters*/
    else if((NULL == pRspCbCtx) || (NULL == pPresenceChk_RspCb)
        || (hTargetDev == 0) )
    {
        RetVal= NFCSTATUS_INVALID_PARAMETER;
    }
    /* Check for DeInit call*/
    else if(gpphLibContext->LibNfcState.next_state
                            == eLibNfcHalStateShutdown)
    {
        RetVal = NFCSTATUS_SHUTDOWN;
    }
    /* Check target is connected or not */
    else if( gpphLibContext->Connected_handle == 0)
    {
        RetVal = NFCSTATUS_TARGET_NOT_CONNECTED;
    }
    /* Check given handle is valid or not*/
    else if(hTargetDev != gpphLibContext->Connected_handle)
    {
        RetVal = NFCSTATUS_INVALID_HANDLE;
    }
#ifdef LLCP_TRANSACT_CHANGES
    else if ((LLCP_STATE_RESET_INIT != gpphLibContext->llcp_cntx.sLlcpContext.state)
            && (LLCP_STATE_CHECKED != gpphLibContext->llcp_cntx.sLlcpContext.state))
    {
        RetVal= NFCSTATUS_BUSY;
    }
#endif /* #ifdef LLCP_TRANSACT_CHANGES */
    else
    {
        ps_rem_dev_info = (phHal_sRemoteDevInformation_t *)
                                    gpphLibContext->Connected_handle;
        if((phHal_eMifare_PICC == ps_rem_dev_info->RemDevType)
            &&(0 != ps_rem_dev_info->RemoteDevInfo.Iso14443A_Info.Sak)
            &&(TRUE == gpphLibContext->LastTrancvSuccess))
        {
            /* Call HAL4 API */
            RetVal =  phHal4Nfc_Transceive(
                                gpphLibContext->psHwReference,
                                gpphLibContext->psBufferedAuth,
                                (phHal_sRemoteDevInformation_t *)
                                    gpphLibContext->Connected_handle,
                                (pphHal4Nfc_TransceiveCallback_t )
                                    phLibNfc_ChkPresence_Trcv_Cb,
                                (void *)gpphLibContext
                                );
                
        }
        else
        {
            /* Call lower layer PresenceCheck function */
            RetVal = phHal4Nfc_PresenceCheck(gpphLibContext->psHwReference,
                                        phLibNfc_RemoteDev_CheckPresence_Cb,
                                        (void *)gpphLibContext);
        }
        if( NFCSTATUS_PENDING == PHNFCSTATUS(RetVal))
        {
            gpphLibContext->CBInfo.pClientPresChkCb = pPresenceChk_RspCb;
            gpphLibContext->CBInfo.pClientPresChkCntx = pRspCbCtx;
            /* Mark General callback pending status as TRUE*/
            gpphLibContext->status.GenCb_pending_status = TRUE;

            /* Update the state machine*/
			gpphLibContext->LibNfcState.next_state = eLibNfcHalStatePresenceChk;           
        }
        else /* If return value is internal error(other than pending ) return NFCSTATUS_FAILED*/
        {
          RetVal = NFCSTATUS_FAILED;
        }
    }
    return RetVal;
}

/**
* Response Callback for Remote device Presence Check.
*/
STATIC
void phLibNfc_RemoteDev_CheckPresence_Cb(void     *context,
                                        NFCSTATUS status)
{
    void                    *pUpperLayerContext=NULL;
    pphLibNfc_RspCb_t       pClientCb=NULL;

    /*check valid context is returned or not*/
    if((phLibNfc_LibContext_t *)context != gpphLibContext)
    {
        /*exception: wrong context pointer returned*/
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
    }    
    /* Mark general callback pending status as FALSE*/
    gpphLibContext->status.GenCb_pending_status = FALSE;
    pClientCb =gpphLibContext->CBInfo.pClientPresChkCb ;
    pUpperLayerContext = gpphLibContext->CBInfo.pClientPresChkCntx;
    gpphLibContext->CBInfo.pClientPresChkCntx = NULL;
    gpphLibContext->CBInfo.pClientPresChkCb =NULL;
    /* Check DeInit call is called, if yes call pending 
    shutdown and return NFCSTATUS_SHUTDOWN */
    if(eLibNfcHalStateShutdown == gpphLibContext->LibNfcState.next_state)
    {
        phLibNfc_Pending_Shutdown();
        status = NFCSTATUS_SHUTDOWN;    
    }
    else
    {
        if (status != NFCSTATUS_SUCCESS)
        {
           /*If status is other than SUCCESS (Internal error) return
           NFCSTATUS_TARGET_LOST */
            status= NFCSTATUS_TARGET_LOST;
        }   
        else
        {
            status = NFCSTATUS_SUCCESS;
        }
    }
     /* Update the current state */
    phLibNfc_UpdateCurState(status,gpphLibContext);
    if(NULL != pClientCb)
    {
        /* call the upper layer callback */
        pClientCb(pUpperLayerContext,status);
    }
    return;
}

/**Used for presence chk incase of mifare std tags*/
STATIC void phLibNfc_ChkPresence_Trcv_Cb(
                            void *context,
                            phHal_sRemoteDevInformation_t *psRemoteDevInfo,
                            phNfc_sData_t *response, 
                            NFCSTATUS status
                            )
{
    PHNFC_UNUSED_VARIABLE(psRemoteDevInfo);
    PHNFC_UNUSED_VARIABLE(response);
    phLibNfc_RemoteDev_CheckPresence_Cb(context,status);
    return;
}



