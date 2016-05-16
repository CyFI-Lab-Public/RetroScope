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
 * \file phLibNfc.c

 * Project: NFC FRI / HALDL
 *
 * $Date: Tue Jun  1 14:53:48 2010 $
 * $Author: ing07385 $
 * $Revision: 1.89 $
 * $Aliases: NFC_FRI1.1_WK1024_SDK $
 *
 */


/*
************************* Header Files ****************************************
*/

#define LOG_TAG "NFC"

#include <phLibNfc.h>
#include <phDal4Nfc.h>
#include <phHal4Nfc.h>
#include <phOsalNfc.h>
#include <phLibNfc_Internal.h>
#include <phLibNfc_ndef_raw.h>
#include <phLibNfc_initiator.h>
#include <phLibNfc_discovery.h>
#include <phNfcStatus.h>
#include <cutils/log.h>
/*
*************************** Macro's  ******************************************
*/

extern int dlopen_firmware();

#ifndef STATIC_DISABLE
#define STATIC static
#else
#define STATIC
#endif


/*
*************************** Global Variables **********************************
*/


pphLibNfc_LibContext_t gpphLibContext=NULL;

/*
*************************** Static Function Declaration ***********************
*/

/* Init callback */
STATIC void phLibNfc_InitCb(void *pContext,NFCSTATUS status);

/* Shutdown callback */
STATIC void phLibNfc_ShutdownCb(void *pContext,NFCSTATUS status);

/**Default notification handler registered with lower layer immediately after 
   successful initialization*/
STATIC void phLibNfc_DefaultHandler(
                                void                        *context,                                 
                                phHal_eNotificationType_t    type,
                                phHal4Nfc_NotificationInfo_t info,
                                NFCSTATUS                    status
                                );
/*
*************************** Function Definitions ******************************
*/

NFCSTATUS phLibNfc_Mgt_ConfigureDriver (pphLibNfc_sConfig_t     psConfig,
                                        void **                 ppDriverHandle)
{
    if(NULL != gpphLibContext)
    {
        return NFCSTATUS_ALREADY_INITIALISED;
    }   

    return phDal4Nfc_Config(psConfig, ppDriverHandle);
}

NFCSTATUS phLibNfc_Mgt_UnConfigureDriver (void *                 pDriverHandle)
{
    if(NULL != gpphLibContext)
    {
        return NFCSTATUS_ALREADY_INITIALISED;
    }   

   return phDal4Nfc_ConfigRelease(pDriverHandle);
}

NFCSTATUS phLibNfc_HW_Reset ()
{
    NFCSTATUS Status = NFCSTATUS_SUCCESS;

    Status = phDal4Nfc_Reset(1);
    Status = phDal4Nfc_Reset(0);
    Status = phDal4Nfc_Reset(1);

    return Status;
}

NFCSTATUS phLibNfc_Download_Mode ()
{
   return phDal4Nfc_Download();
}

int phLibNfc_Load_Firmware_Image ()
{
    int status;
    status = dlopen_firmware();
    return status;
}

// Function for delay the recovery in case wired mode is set
// to complete the possible pending transaction with SE
void phLibNfc_Mgt_Recovery ()
{
    /* Wait before recovery if wired mode */
    if (gpphLibContext->sSeContext.eActivatedMode == phLibNfc_SE_ActModeWired)
    {
        usleep (12000000);
    }

    return;
}

extern uint8_t nxp_nfc_isoxchg_timeout;
NFCSTATUS phLibNfc_SetIsoXchgTimeout(uint8_t timeout) {
    nxp_nfc_isoxchg_timeout = timeout;
    return NFCSTATUS_SUCCESS;
}

int phLibNfc_GetIsoXchgTimeout() {
    return nxp_nfc_isoxchg_timeout;
}

extern uint32_t nxp_nfc_hci_response_timeout;
NFCSTATUS phLibNfc_SetHciTimeout(uint32_t timeout_in_ms) {
    nxp_nfc_hci_response_timeout = timeout_in_ms;
    return NFCSTATUS_SUCCESS;
}

int phLibNfc_GetHciTimeout() {
    return nxp_nfc_hci_response_timeout;
}

extern uint8_t nxp_nfc_felica_timeout;
NFCSTATUS phLibNfc_SetFelicaTimeout(uint8_t timeout_in_ms) {
    nxp_nfc_felica_timeout = timeout_in_ms;
    return NFCSTATUS_SUCCESS;
}

int phLibNfc_GetFelicaTimeout() {
    return nxp_nfc_felica_timeout;
}

extern uint8_t nxp_nfc_mifareraw_timeout;
NFCSTATUS phLibNfc_SetMifareRawTimeout(uint8_t timeout) {
    nxp_nfc_mifareraw_timeout = timeout;
    return NFCSTATUS_SUCCESS;
}

int phLibNfc_GetMifareRawTimeout() {
    return nxp_nfc_mifareraw_timeout;
}

/**
*    Initialize the phLibNfc interface.
*/

NFCSTATUS phLibNfc_Mgt_Initialize(void                *pDriverHandle,
                                 pphLibNfc_RspCb_t    pInitCb,
                                 void                 *pContext)
{
     NFCSTATUS Status = NFCSTATUS_SUCCESS;     
     if((NULL == pDriverHandle)||(NULL == pInitCb))
     {
        Status = NFCSTATUS_INVALID_PARAMETER;
     }
     else if(NULL == gpphLibContext)
     {
        /* Initialize the Lib context */
        gpphLibContext=(pphLibNfc_LibContext_t)phOsalNfc_GetMemory(
                                        (uint32_t)sizeof(phLibNfc_LibContext_t));
        if(NULL == gpphLibContext)
        {
            Status=NFCSTATUS_INSUFFICIENT_RESOURCES;
        }
        else
        {
            (void)memset((void *)gpphLibContext,0,(
                                    (uint32_t)sizeof(phLibNfc_LibContext_t)));

            /* Store the Callback and context in LibContext structure*/
            gpphLibContext->CBInfo.pClientInitCb=pInitCb;
            gpphLibContext->CBInfo.pClientInitCntx=pContext;
            /* Initialize the HwReferece structure */
            gpphLibContext->psHwReference=(phHal_sHwReference_t *)
                                    phOsalNfc_GetMemory((uint32_t)sizeof(phHal_sHwReference_t));
            (void)memset((void *)gpphLibContext->psHwReference,0,
                                        ((uint32_t)sizeof(phHal_sHwReference_t)));
            /* Allocate the Memory for the Transceive info */
            if( gpphLibContext->psHwReference!=NULL)
            {
                gpphLibContext->psHwReference->p_board_driver = pDriverHandle;
                Status = phLibNfc_UpdateNextState(gpphLibContext,
                                            eLibNfcHalStateInitandIdle);
                if(Status==NFCSTATUS_SUCCESS)
                {
                    Status=phHal4Nfc_Open(
                                    gpphLibContext->psHwReference,
                                    eInitDefault,
                                    phLibNfc_InitCb,
                                    (void *)gpphLibContext);
                }
            }
            else
            {
                Status = NFCSTATUS_INSUFFICIENT_RESOURCES;
            }
            phLibNfc_Ndef_Init();
        }
    }
    else if(gpphLibContext->LibNfcState.next_state==eLibNfcHalStateShutdown)
    {
        Status = NFCSTATUS_SHUTDOWN;
    }
    else
    {
        Status=NFCSTATUS_ALREADY_INITIALISED;
    }
   return Status;
}

/*
 * This function called by the HAL4 when the initialization seq is completed.
 */
STATIC void phLibNfc_InitCb(void *pContext,NFCSTATUS status)
{
    pphLibNfc_LibContext_t   pLibContext=NULL;
    pphLibNfc_RspCb_t          pClientCb=NULL;
    void                        *pUpperLayerContext=NULL;


    /* Initialize the local variable */
    pLibContext  = (pphLibNfc_LibContext_t)pContext;

    pClientCb =pLibContext->CBInfo.pClientInitCb;
    pUpperLayerContext=pLibContext->CBInfo.pClientInitCntx;
    if(status == NFCSTATUS_SUCCESS)
    {
        /* Get the Lib context */
        pLibContext=(pphLibNfc_LibContext_t)gpphLibContext;
        gpphLibContext->sSeContext.eActivatedMode = phLibNfc_SE_ActModeOff;
        if(pLibContext->psHwReference->uicc_connected==TRUE)
        {
            /* populate state of the secured element */
            gpphLibContext->sSeContext.eActivatedMode = phLibNfc_SE_ActModeDefault;
            sSecuredElementInfo[LIBNFC_SE_UICC_INDEX].eSE_CurrentState=phLibNfc_SE_Active;
            pLibContext->sSeContext.uUiccActivate=TRUE;
        }		
        if(pLibContext->psHwReference->smx_connected==TRUE)
        {
            /* populate state of the secured element */
            gpphLibContext->sSeContext.eActivatedMode = phLibNfc_SE_ActModeDefault;
            sSecuredElementInfo[LIBNFC_SE_SMARTMX_INDEX].eSE_CurrentState=phLibNfc_SE_Inactive; 
            pLibContext->sSeContext.uSmxActivate =FALSE;
        }

        phLibNfc_UpdateCurState(status,pLibContext);
        (void)phHal4Nfc_RegisterNotification(                                            
                                pLibContext->psHwReference,
                                eRegisterDefault,
                                phLibNfc_DefaultHandler,
                                (void*)pLibContext
                                ); 
        /* call the upper layer register function */
        (*pClientCb)(pUpperLayerContext,status);

    }
    else
    {
        /*Change the status code failed*/
        status = NFCSTATUS_FAILED;
        /* Get the Lib context */
        pLibContext=(pphLibNfc_LibContext_t)gpphLibContext;

        phLibNfc_UpdateCurState(status,pLibContext);



        /* Allocate the Memory for the Transceive info */
        if(pLibContext->psHwReference!= NULL)
        {
            phOsalNfc_FreeMemory(pLibContext->psHwReference);
            pLibContext->psHwReference = NULL;
        }
        (*pClientCb)(pUpperLayerContext, status);

        phOsalNfc_FreeMemory(pLibContext);
        pLibContext= NULL;
        gpphLibContext = NULL;
        
    }
    return;
}

/**Default notification handler registered with lower layer immediately after 
   successful initialization*/
STATIC void phLibNfc_DefaultHandler(
                                void                        *context,                                 
                                phHal_eNotificationType_t    type,
                                phHal4Nfc_NotificationInfo_t info,
                                NFCSTATUS                    status
                                )
{
    if(context != (void *)gpphLibContext)
    {
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
    }
    else
    {
        info = info;
        if((NFC_EVENT_NOTIFICATION == type) &&
            (NFCSTATUS_BOARD_COMMUNICATION_ERROR == status))
        {
            phLibNfc_UpdateCurState(NFCSTATUS_FAILED,gpphLibContext);
            phOsalNfc_RaiseException(phOsalNfc_e_UnrecovFirmwareErr,1);
        }
    }
    return;
}
/**
* De-Initialize the LIB NFC.
*/
NFCSTATUS phLibNfc_Mgt_DeInitialize(void *                      pDriverHandle,
                                   pphLibNfc_RspCb_t            pDeInitCb,
                                   void*                        pContext
                                   )
{
    NFCSTATUS Status = NFCSTATUS_SUCCESS;
    pphLibNfc_LibContext_t pLibContext = gpphLibContext;
    if(NULL==pDriverHandle)
    {
        /*Check for valid parameters */
        Status = NFCSTATUS_INVALID_PARAMETER;
    }
    else if((pLibContext==NULL)
        || (pLibContext->LibNfcState.cur_state
            == eLibNfcHalStateShutdown))
    {   /*Lib Nfc not initlized*/
        Status = NFCSTATUS_NOT_INITIALISED;
    }
    else
    {
        if(pDeInitCb==NULL)
        {
            phHal4Nfc_Hal4Reset(pLibContext->psHwReference,(void *)pLibContext);
            if(pLibContext->psHwReference!=NULL)
            {
                phOsalNfc_FreeMemory(pLibContext->psHwReference);
                pLibContext->psHwReference = NULL;
            }
            /*Free the memory allocated during NDEF read,write
              and NDEF formatting*/
            phLibNfc_Ndef_DeInit();
            phOsalNfc_FreeMemory(pLibContext);
            gpphLibContext=NULL;
            pLibContext= NULL;
        }
        else
        {
            if (NULL!= pLibContext->CBInfo.pClientShutdownCb)
            {
                /* Previous callback pending */
                Status = NFCSTATUS_BUSY;
            }
          Status = NFCSTATUS_PENDING;
          if(TRUE != pLibContext->status.GenCb_pending_status)
          {
              Status = phHal4Nfc_Close(pLibContext->psHwReference,
                                  phLibNfc_ShutdownCb,
                                  (void *)pLibContext);
          }
          if(Status== NFCSTATUS_PENDING)
          {
              pLibContext->CBInfo.pClientShutdownCb = pDeInitCb;
              pLibContext->CBInfo.pClientShtdwnCntx = pContext;
              pLibContext->status.GenCb_pending_status=TRUE;
              pLibContext->LibNfcState.next_state= eLibNfcHalStateShutdown;
          }
          else
          {
              Status =NFCSTATUS_FAILED;
          }
        }       
    }
    return Status;
}
/* shutdown callback -
  Free the allocated memory here */
STATIC void phLibNfc_ShutdownCb(void *pContext,NFCSTATUS status)
{
    pphLibNfc_RspCb_t           pClientCb=NULL;
    void                        *pUpperLayerContext=NULL;
    pphLibNfc_LibContext_t      pLibContext=NULL;

    PHNFC_UNUSED_VARIABLE(pContext);
    /* Get the Lib context */
    pLibContext=(pphLibNfc_LibContext_t)gpphLibContext;

    if(pLibContext == NULL)
    {
        status = NFCSTATUS_FAILED;
    }
    else
    {
        /* Initialize the local variable */
        pClientCb =pLibContext->CBInfo.pClientShutdownCb;
        pUpperLayerContext=pLibContext->CBInfo.pClientShtdwnCntx;
        if(status == NFCSTATUS_SUCCESS)
        {
            pLibContext->LibNfcState.cur_state = eLibNfcHalStateShutdown;
            phLibNfc_UpdateCurState(status,pLibContext);

            pLibContext->status.GenCb_pending_status=FALSE;
            
            /* Allocate the Memory for the Transceive info */
            if(pClientCb!=NULL)
            {
                (*pClientCb)(pUpperLayerContext, status);
            }
            if(pLibContext->psHwReference!=NULL)
            {
                phOsalNfc_FreeMemory(pLibContext->psHwReference);
                pLibContext->psHwReference = NULL;
            }
            if(NULL != gpphLibContext->psBufferedAuth)
            {
                if(NULL != gpphLibContext->psBufferedAuth->sRecvData.buffer)
                {
                    phOsalNfc_FreeMemory(
                        gpphLibContext->psBufferedAuth->sRecvData.buffer);
                }
                if(NULL != gpphLibContext->psBufferedAuth->sSendData.buffer)
                {
                    phOsalNfc_FreeMemory(
                        gpphLibContext->psBufferedAuth->sSendData.buffer);
                }
                phOsalNfc_FreeMemory(gpphLibContext->psBufferedAuth);
                gpphLibContext->psBufferedAuth = NULL;
            }
            /*Free the memory allocated during NDEF read,write
              and NDEF formatting*/
            phLibNfc_Ndef_DeInit();        
                phOsalNfc_FreeMemory(pLibContext);
                gpphLibContext=NULL;
                pLibContext= NULL;
       
        }
        else
        {
            /* shutdown sequence failed by HAL 4 */
            status= NFCSTATUS_FAILED;
            pLibContext=(pphLibNfc_LibContext_t)gpphLibContext;
            phLibNfc_UpdateCurState(status,pLibContext);
            pLibContext->status.GenCb_pending_status=FALSE;
            if(pClientCb!=NULL)
            {
                (*pClientCb)(pUpperLayerContext,status);
            }
        }
    }
}
/**
*    Pending shutdown call.
*/


void phLibNfc_Pending_Shutdown(void)
{
    NFCSTATUS RetStatus = NFCSTATUS_SUCCESS ;
    gpphLibContext->status.GenCb_pending_status = FALSE;
    RetStatus = phHal4Nfc_Close(
                        gpphLibContext->psHwReference,
                        phLibNfc_ShutdownCb,
                        (void *)gpphLibContext);
    PHNFC_UNUSED_VARIABLE(RetStatus);
    return;
}


/**
* Reset the LIB NFC.
*/
NFCSTATUS phLibNfc_Mgt_Reset(void  *pContext)
{
    NFCSTATUS Status = NFCSTATUS_SUCCESS;
    phLibNfc_LibContext_t   *pLibNfc_Ctxt = (phLibNfc_LibContext_t *)pContext;

    if((pLibNfc_Ctxt == NULL)
        || (gpphLibContext->LibNfcState.cur_state
            == eLibNfcHalStateShutdown))
    {   /*Lib Nfc not initlized*/
        Status = NFCSTATUS_NOT_INITIALISED;
    }  
    else if(NULL == pContext) 
    {
        Status = NFCSTATUS_INVALID_PARAMETER;
    }
    /* Check for valid state,If De initialize is called then
    return NFCSTATUS_SHUTDOWN */
    else if(gpphLibContext->LibNfcState.next_state
                            == eLibNfcHalStateShutdown)
    {
        Status = NFCSTATUS_SHUTDOWN;
    }
    else
    {                     
        /*Reset all callback status*/
        (void) memset(&(gpphLibContext->RegNtfType),0,
                        sizeof(phLibNfc_Registry_Info_t));
        (void) memset(&(gpphLibContext->sADDconfig),0,
                        sizeof(phLibNfc_sADD_Cfg_t));
        (void) memset(&(gpphLibContext->ndef_cntx),0,
                        sizeof(phLibNfc_NdefInfo_t));
        (void) memset(&(gpphLibContext->sNfcIp_Context),0,
                        sizeof(phLibNfc_NfcIpInfo_t));
        (void) memset(&(gpphLibContext->sCardEmulCfg),0,
                        sizeof(phHal_sEmulationCfg_t));
        (void) memset(&(gpphLibContext->Discov_handle),0,
                        MAX_REMOTE_DEVICES);

        /*Free memory allocated for NDEF records*/
        if(NULL != gpphLibContext->psBufferedAuth)
        {
            if(NULL != gpphLibContext->psBufferedAuth->sRecvData.buffer)
            {
                phOsalNfc_FreeMemory(
                    gpphLibContext->psBufferedAuth->sRecvData.buffer);
                gpphLibContext->psBufferedAuth->sRecvData.buffer = NULL;
            }
            if(NULL != gpphLibContext->psBufferedAuth->sSendData.buffer)
            {
                phOsalNfc_FreeMemory(
                    gpphLibContext->psBufferedAuth->sSendData.buffer);
                gpphLibContext->psBufferedAuth->sSendData.buffer = NULL;
            }
            phOsalNfc_FreeMemory(gpphLibContext->psBufferedAuth);
            gpphLibContext->psBufferedAuth = NULL;
        }
        if(NULL != gpphLibContext->psTransInfo)
        {
            phOsalNfc_FreeMemory(gpphLibContext->psTransInfo);
            gpphLibContext->psTransInfo = NULL;
        }
        if(NULL != gpphLibContext->ndef_cntx.psNdefMap)
        {
            if(NULL != gpphLibContext->ndef_cntx.psNdefMap->SendRecvBuf)
            {
                phOsalNfc_FreeMemory(gpphLibContext->ndef_cntx.psNdefMap->SendRecvBuf);
                gpphLibContext->ndef_cntx.psNdefMap->SendRecvBuf = NULL;
            }
            phOsalNfc_FreeMemory(gpphLibContext->ndef_cntx.psNdefMap);
            gpphLibContext->ndef_cntx.psNdefMap = NULL;
        }
        if(NULL != gpphLibContext->psOverHalCtxt)
        {
            phOsalNfc_FreeMemory(gpphLibContext->psOverHalCtxt);
            gpphLibContext->psTransInfo = NULL;
        }
        if(NULL != gpphLibContext->psDevInputParam)
        {
            phOsalNfc_FreeMemory(gpphLibContext->psDevInputParam);
            gpphLibContext->psDevInputParam = NULL;
        }
        if(NULL != gpphLibContext->ndef_cntx.ndef_fmt)
        {
            phOsalNfc_FreeMemory(gpphLibContext->ndef_cntx.ndef_fmt);
            gpphLibContext->ndef_cntx.ndef_fmt = NULL;
        }
        if(NULL != pNdefRecord) 
        {
            if(NULL != pNdefRecord->Id)
            {
                phOsalNfc_FreeMemory(pNdefRecord->Id);
                pNdefRecord->Id = NULL;
            }
            if(NULL != pNdefRecord->Type)
            {
                phOsalNfc_FreeMemory(pNdefRecord->Type);
                pNdefRecord->Type = NULL;
            }
            if(NULL != pNdefRecord->PayloadData)
            {
                phOsalNfc_FreeMemory(pNdefRecord->PayloadData);
                pNdefRecord->PayloadData = NULL;
            }
        }
        if(NULL != NdefInfo.pNdefRecord)
        {
            phOsalNfc_FreeMemory(NdefInfo.pNdefRecord);
            NdefInfo.pNdefRecord = NULL;
        }          
        if(NULL != gpphLibContext->phLib_NdefRecCntx.NdefCb)
        {
            phOsalNfc_FreeMemory(gpphLibContext->phLib_NdefRecCntx.NdefCb);
            gpphLibContext->phLib_NdefRecCntx.NdefCb = NULL;
        }
        if(NULL != gpphLibContext->phLib_NdefRecCntx.ndef_message.buffer)
        {
            phOsalNfc_FreeMemory(gpphLibContext->phLib_NdefRecCntx.ndef_message.buffer);
            gpphLibContext->phLib_NdefRecCntx.ndef_message.buffer = NULL;
        }
        /* No device is connected */
        gpphLibContext->Connected_handle = 0x00;       
        gpphLibContext->Prev_Connected_handle = 0x00;
        gpphLibContext->ReleaseType = NFC_INVALID_RELEASE_TYPE;        
        gpphLibContext->eLibNfcCfgMode = NFC_DISCOVERY_STOP;
        /*Lib Nfc Stack is initilized and in idle state*/
        gpphLibContext->LibNfcState.cur_state = eLibNfcHalStateInitandIdle;

        /* Reset all callback status */
        gpphLibContext->CBInfo.pClientCkNdefCb = NULL;
        gpphLibContext->CBInfo.pClientCkNdefCntx = NULL;
        gpphLibContext->CBInfo.pClientConCntx = NULL;
        gpphLibContext->CBInfo.pClientConnectCb = NULL;
        gpphLibContext->CBInfo.pClientDConCntx = NULL;
        gpphLibContext->CBInfo.pClientDisCfgCntx = NULL;
        gpphLibContext->CBInfo.pClientDisConfigCb = NULL;
        gpphLibContext->CBInfo.pClientInitCb = NULL;
        gpphLibContext->CBInfo.pClientInitCntx = gpphLibContext;
        gpphLibContext->CBInfo.pClientNdefNtfRespCb = NULL;
        gpphLibContext->CBInfo.pClientNdefNtfRespCntx = NULL;
        gpphLibContext->CBInfo.pClientNtfRegRespCB = NULL;
        gpphLibContext->CBInfo.pClientNtfRegRespCntx = NULL;
        gpphLibContext->CBInfo.pClientPresChkCb = NULL;
        gpphLibContext->CBInfo.pClientPresChkCntx = NULL;
        gpphLibContext->CBInfo.pClientRdNdefCb = NULL;
        gpphLibContext->CBInfo.pClientRdNdefCntx = NULL;
        gpphLibContext->CBInfo.pClientShtdwnCntx = NULL;
        gpphLibContext->CBInfo.pClientShutdownCb = NULL;
        gpphLibContext->CBInfo.pClientTransceiveCb = NULL;
        gpphLibContext->CBInfo.pClientTranseCntx = NULL;
        gpphLibContext->CBInfo.pClientWrNdefCb = NULL;
        gpphLibContext->CBInfo.pClientWrNdefCntx = NULL;
        gpphLibContext->sNfcIp_Context.pClientNfcIpCfgCb = NULL;
        gpphLibContext->sNfcIp_Context.pClientNfcIpCfgCntx = NULL;
        gpphLibContext->sNfcIp_Context.pClientNfcIpRxCb = NULL;
        gpphLibContext->sNfcIp_Context.pClientNfcIpRxCntx = NULL;
        gpphLibContext->sNfcIp_Context.pClientNfcIpTxCb = NULL;
        gpphLibContext->sNfcIp_Context.pClientNfcIpTxCntx = NULL;
        gpphLibContext->sSeContext.sSeCallabackInfo.pSeListenerNtfCb = NULL;
        gpphLibContext->sSeContext.sSeCallabackInfo.pSeListenerCtxt = NULL;
        gpphLibContext->sSeContext.sSeCallabackInfo.pSEsetModeCb = NULL;
        gpphLibContext->sSeContext.sSeCallabackInfo.pSEsetModeCtxt = NULL;
        /*No callback is pending*/
        gpphLibContext->status.GenCb_pending_status = FALSE;        
                    
    }
    return Status;
}
/**
*    LibNfc state machine next state update.
*/

NFCSTATUS
phLibNfc_UpdateNextState(
                         pphLibNfc_LibContext_t   pLibContext,
                         phLibNfc_State_t        next_state
                         )
{
    NFCSTATUS       status = NFCSTATUS_INVALID_STATE;
    switch(pLibContext->LibNfcState.cur_state)
    {
    case eLibNfcHalStateShutdown:
        {
            switch(next_state)
            {
            case eLibNfcHalStateShutdown:
            case eLibNfcHalStateInitandIdle:
                status = NFCSTATUS_SUCCESS;
                break;
            default:
                break;
            }
        }
        break;
    case eLibNfcHalStateConfigReady:
        {
            switch(next_state)
            {
            case eLibNfcHalStateShutdown:
            case eLibNfcHalStateConfigReady:
            case eLibNfcHalStateInitandIdle:
            case eLibNfcHalStateConnect:
                status = NFCSTATUS_SUCCESS;
                break;
            default:
                break;
            }
        }
        break;
    case eLibNfcHalStateConnect:
        {
            switch(next_state)
            {
            case eLibNfcHalStateShutdown:
            case eLibNfcHalStateRelease:
            case eLibNfcHalStateTransaction:
            case eLibNfcHalStatePresenceChk:
                status = NFCSTATUS_SUCCESS;
                break;
            default:
                break;
            }
        }
        break;
    case eLibNfcHalStatePresenceChk:
        {
            switch(next_state)
            {
            case eLibNfcHalStateShutdown:
            case eLibNfcHalStateConfigReady:
            case eLibNfcHalStateRelease:
            case eLibNfcHalStateTransaction:
            case eLibNfcHalStatePresenceChk:
                status = NFCSTATUS_SUCCESS;
                break;
            default:
                break;
            }
        }
        break;
    case eLibNfcHalStateInitandIdle:
        {
            switch(next_state)
            {
            case eLibNfcHalStateShutdown:
            case eLibNfcHalStateConfigReady:
                status = NFCSTATUS_SUCCESS;
                break;
            default:
                break;
            }
        }
        break;
    default:
        break;
    }
    pLibContext->LibNfcState.next_state = 
        (uint8_t)((NFCSTATUS_SUCCESS == status)?next_state:pLibContext->LibNfcState.next_state);

    return status;
}

/**
*    LibNfc state machine current state update.
*/

void
phLibNfc_UpdateCurState(
                        NFCSTATUS      status,
                        pphLibNfc_LibContext_t psLibContext
                        )
{
    switch(psLibContext->LibNfcState.next_state)
    {
    case eLibNfcHalStateTransaction:
        psLibContext->LibNfcState.cur_state = (uint8_t)eLibNfcHalStateConnect;
        break;
    case eLibNfcHalStateRelease:
        psLibContext->LibNfcState.cur_state
            = (uint8_t)(psLibContext->status.DiscEnbl_status == TRUE?
              eLibNfcHalStateInitandIdle:eLibNfcHalStateConfigReady);
        break;
    case eLibNfcHalStateInvalid:
        break;
    default:
        psLibContext->LibNfcState.cur_state
            = (uint8_t)((NFCSTATUS_SUCCESS == status)?
            psLibContext->LibNfcState.next_state:
        psLibContext->LibNfcState.cur_state);
    }
    psLibContext->LibNfcState.next_state = (uint8_t)eLibNfcHalStateInvalid;
    return;
}
/* Interface to stack capabilities */

NFCSTATUS phLibNfc_Mgt_GetstackCapabilities(
                    phLibNfc_StackCapabilities_t *phLibNfc_StackCapabilities,
                    void                         *pContext)
{
    NFCSTATUS RetVal = NFCSTATUS_FAILED;
    /*Check Lib Nfc stack is initilized*/
    if((NULL == gpphLibContext)|| 
        (gpphLibContext->LibNfcState.cur_state == eLibNfcHalStateShutdown))
    {
        RetVal = NFCSTATUS_NOT_INITIALISED;
    }   
    /*Check application has sent the valid parameters*/
    else if((NULL == phLibNfc_StackCapabilities)
        || (NULL == pContext))
    {
        RetVal= NFCSTATUS_INVALID_PARAMETER;
    }   
    else if(gpphLibContext->LibNfcState.next_state == eLibNfcHalStateShutdown)
    {
        RetVal = NFCSTATUS_SHUTDOWN;
    }
    else if(TRUE == gpphLibContext->status.GenCb_pending_status)       
    {
        /*Previous operation is pending  */
        RetVal = NFCSTATUS_BUSY;
    }    
    else
    {
        /* Tag Format Capabilities*/
        phLibNfc_StackCapabilities->psFormatCapabilities.Desfire = TRUE;
        phLibNfc_StackCapabilities->psFormatCapabilities.MifareStd = TRUE;
        phLibNfc_StackCapabilities->psFormatCapabilities.MifareUL = TRUE;
        phLibNfc_StackCapabilities->psFormatCapabilities.FeliCa = FALSE;
        phLibNfc_StackCapabilities->psFormatCapabilities.Jewel = FALSE;
        phLibNfc_StackCapabilities->psFormatCapabilities.ISO14443_4A = FALSE;
        phLibNfc_StackCapabilities->psFormatCapabilities.ISO14443_4B = FALSE;
        phLibNfc_StackCapabilities->psFormatCapabilities.MifareULC = TRUE;
         phLibNfc_StackCapabilities->psFormatCapabilities.ISO15693 = FALSE;

        /* Tag Mapping Capabilities */
        phLibNfc_StackCapabilities->psMappingCapabilities.FeliCa = TRUE;
        phLibNfc_StackCapabilities->psMappingCapabilities.Desfire = TRUE;
        phLibNfc_StackCapabilities->psMappingCapabilities.ISO14443_4A = TRUE;
        phLibNfc_StackCapabilities->psMappingCapabilities.ISO14443_4B = TRUE;
        phLibNfc_StackCapabilities->psMappingCapabilities.MifareStd = TRUE;
        phLibNfc_StackCapabilities->psMappingCapabilities.MifareUL = TRUE;
        phLibNfc_StackCapabilities->psMappingCapabilities.MifareULC = TRUE;     
        phLibNfc_StackCapabilities->psMappingCapabilities.Jewel = TRUE;
        phLibNfc_StackCapabilities->psMappingCapabilities.ISO15693 = FALSE;
        
        /*Call Hal4 Get Dev Capabilities to get info about protocols supported
          by Lib Nfc*/
        PHDBG_INFO("LibNfc:Get Stack capabilities ");
        RetVal= phHal4Nfc_GetDeviceCapabilities(                                          
                        gpphLibContext->psHwReference,                            
                        &(phLibNfc_StackCapabilities->psDevCapabilities),
                        (void *)gpphLibContext); 

        LIB_NFC_VERSION_SET(phLibNfc_StackCapabilities->psDevCapabilities.hal_version,
                            PH_HAL4NFC_VERSION,
                            PH_HAL4NFC_REVISION,
                            PH_HAL4NFC_PATCH,
                            PH_HAL4NFC_BUILD);
                
        phLibNfc_StackCapabilities->psDevCapabilities.fw_version=
            gpphLibContext->psHwReference->device_info.fw_version;
        phLibNfc_StackCapabilities->psDevCapabilities.hci_version=
            gpphLibContext->psHwReference->device_info.hci_version;
        phLibNfc_StackCapabilities->psDevCapabilities.hw_version=
            gpphLibContext->psHwReference->device_info.hw_version;
        phLibNfc_StackCapabilities->psDevCapabilities.model_id=
            gpphLibContext->psHwReference->device_info.model_id;        
        (void)memcpy(phLibNfc_StackCapabilities->psDevCapabilities.full_version,
            gpphLibContext->psHwReference->device_info.full_version,NXP_FULL_VERSION_LEN);
        /* Check the firmware version */
        if (nxp_nfc_full_version == NULL) {
            // Couldn't load firmware, just pretend we're up to date.
            ALOGW("Firmware image not available: this device might be running old NFC firmware!");
            phLibNfc_StackCapabilities->psDevCapabilities.firmware_update_info = 0;
        } else {
            phLibNfc_StackCapabilities->psDevCapabilities.firmware_update_info = memcmp(phLibNfc_StackCapabilities->psDevCapabilities.full_version, nxp_nfc_full_version,
                       NXP_FULL_VERSION_LEN);
        }

        if(NFCSTATUS_SUCCESS != RetVal)
        {       
            RetVal = NFCSTATUS_FAILED;
        }
    }
    return RetVal;
}






NFCSTATUS phLibNfc_Mgt_ConfigureTestMode(void   *pDriverHandle,
                                 pphLibNfc_RspCb_t   pTestModeCb,
                                 phLibNfc_Cfg_Testmode_t eTstmode,
                                 void                *pContext)
{
     NFCSTATUS Status = NFCSTATUS_SUCCESS;  
     phHal4Nfc_InitType_t eInitType=eInitDefault;
     
     if((NULL == pDriverHandle)||(NULL == pTestModeCb))
     {
        Status = NFCSTATUS_INVALID_PARAMETER;
     }
     else if((NULL != gpphLibContext) && \
         (gpphLibContext->LibNfcState.next_state==eLibNfcHalStateShutdown))
     { 
        Status = NFCSTATUS_SHUTDOWN;
     } 
     else if( (eTstmode == phLibNfc_TstMode_On) && (NULL != gpphLibContext))
     {
        Status=NFCSTATUS_ALREADY_INITIALISED;
     }
     else if( (eTstmode == phLibNfc_TstMode_Off) && (NULL == gpphLibContext))
     {
        Status = NFCSTATUS_NOT_INITIALISED;
     }
     else if( (eTstmode == phLibNfc_TstMode_Off) && (NULL != gpphLibContext))
     {          
        if (NULL!= gpphLibContext->CBInfo.pClientShutdownCb)
        {   /* Previous callback pending */
            Status = NFCSTATUS_BUSY;
        }
        else
        {
            Status = NFCSTATUS_PENDING;
            if(TRUE != gpphLibContext->status.GenCb_pending_status)
            {
                Status = phHal4Nfc_Close(gpphLibContext->psHwReference,
                                    phLibNfc_ShutdownCb,
                                    (void *)gpphLibContext);
            }
            if(Status== NFCSTATUS_PENDING)
            {
                gpphLibContext->CBInfo.pClientShutdownCb = pTestModeCb;
                gpphLibContext->CBInfo.pClientShtdwnCntx = pContext;
                gpphLibContext->status.GenCb_pending_status=TRUE;
                gpphLibContext->LibNfcState.next_state= eLibNfcHalStateShutdown;
            }
            else
            {
                Status =NFCSTATUS_FAILED;
            }
        }       
     }
     else 
     {
            /* Initialize the Lib context */
        gpphLibContext=(pphLibNfc_LibContext_t)phOsalNfc_GetMemory(
                                        (uint32_t)sizeof(phLibNfc_LibContext_t));
        if(NULL == gpphLibContext)
        {
            Status=NFCSTATUS_INSUFFICIENT_RESOURCES;
        }
        else
        {
            (void)memset((void *)gpphLibContext,0,(
                                    (uint32_t)sizeof(phLibNfc_LibContext_t)));

            /* Store the Callback and context in LibContext structure*/
            gpphLibContext->CBInfo.pClientInitCb=pTestModeCb;
            gpphLibContext->CBInfo.pClientInitCntx=pContext;
            /* Initialize the HwReferece structure */
            gpphLibContext->psHwReference=(phHal_sHwReference_t *)
                                    phOsalNfc_GetMemory((uint32_t)sizeof(phHal_sHwReference_t));
            (void)memset((void *)gpphLibContext->psHwReference,0,
                                        ((uint32_t)sizeof(phHal_sHwReference_t)));
            /* Allocate the Memory for the Transceive info */
            if( gpphLibContext->psHwReference!=NULL)
            {
                gpphLibContext->psHwReference->p_board_driver = pDriverHandle;
                Status = phLibNfc_UpdateNextState(gpphLibContext,
                                            eLibNfcHalStateInitandIdle);
                if(Status==NFCSTATUS_SUCCESS)
                {
                    if(eTstmode == phLibNfc_TstMode_On)
                        eInitType = eInitTestModeOn;
                    if(eTstmode == phLibNfc_TstMode_Off)
                        eInitType = eInitDefault;
                    Status=phHal4Nfc_Open(
                                    gpphLibContext->psHwReference,
                                    eInitType,
                                    phLibNfc_InitCb,
                                    (void *)gpphLibContext);
                }
            }
            else
            {
                Status = NFCSTATUS_INSUFFICIENT_RESOURCES;
            }
            phLibNfc_Ndef_Init();
        }
    }
    
   return Status;
}

