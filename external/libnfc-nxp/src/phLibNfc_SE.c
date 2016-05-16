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
 * \file phLibNfc_SE.c
 
 * Project: NFC FRI / HALDL
 *
 * $Date: Thu Apr 22 13:59:50 2010 $ 
 * $Author: ing07385 $
 * $Revision: 1.65 $
 * $Aliases: NFC_FRI1.1_WK1014_SDK,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1019_SDK,NFC_FRI1.1_WK1024_SDK $
 *
 */

/*
************************* Header Files ***************************************
*/

#include <phNfcStatus.h>
#include <phLibNfc.h>
#include <phHal4Nfc.h>
#include <phOsalNfc.h>
#include <phLibNfc_Internal.h>
#include <phLibNfc_SE.h>
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

/*This Structure  contains the Secure Element information*/
phLibNfc_SE_List_t sSecuredElementInfo[PHLIBNFC_MAXNO_OF_SE];

/*
*************************** Static Function Declaration ***********************
*/

/* Response callback for SE Set Mode*/
STATIC 
void phLibNfc_SE_SetMode_cb(void  *context, NFCSTATUS status);


/* SE register listner response notification */
STATIC 
void phLibNfc_SeNotification(void                     *context,
                        phHal_eNotificationType_t     type,
                        phHal4Nfc_NotificationInfo_t  info,
                        NFCSTATUS                     status
                        );
/*
*************************** Function Definitions ******************************
*/

/**
* Registers notification handler to handle secure element specific events
*/
NFCSTATUS phLibNfc_SE_NtfRegister   (
                            pphLibNfc_SE_NotificationCb_t  pSE_NotificationCb,
                            void                            *pContext
                            )
{
     NFCSTATUS         Status = NFCSTATUS_SUCCESS;
     pphLibNfc_LibContext_t pLibContext=(pphLibNfc_LibContext_t)gpphLibContext;

     if((NULL == gpphLibContext) || 
         (gpphLibContext->LibNfcState.cur_state == eLibNfcHalStateShutdown))
     {
         Status = NFCSTATUS_NOT_INITIALISED;
     }
     else if((pSE_NotificationCb == NULL)
         ||(NULL == pContext))
     {
         /*parameters sent by upper layer are not valid*/
         Status = NFCSTATUS_INVALID_PARAMETER;
     }     
     else if(gpphLibContext->LibNfcState.next_state == eLibNfcHalStateShutdown)
     {
         Status = NFCSTATUS_SHUTDOWN;
     }
     else 
     {
         /*Register SE notification with lower layer.
         Any activity on Smx or UICC will be notified */
         Status = phHal4Nfc_RegisterNotification(
                                            pLibContext->psHwReference,
                                            eRegisterSecureElement,
                                            phLibNfc_SeNotification,
                                            (void*)pLibContext); 
        if(Status == NFCSTATUS_SUCCESS)
        {
            pLibContext->sSeContext.sSeCallabackInfo.pSeListenerNtfCb = pSE_NotificationCb;
            pLibContext->sSeContext.sSeCallabackInfo.pSeListenerCtxt=pContext;
        }
        else
        {
            /* Registration failed */
            Status = NFCSTATUS_FAILED;
        }           
     }
     return Status;
}
/**
* SE Notification events are notified with this callback
*/
STATIC void phLibNfc_SeNotification(void  *context,                                 
                                    phHal_eNotificationType_t    type,
                                    phHal4Nfc_NotificationInfo_t  info,
                                    NFCSTATUS                   status)
{
    pphLibNfc_LibContext_t pLibContext=(pphLibNfc_LibContext_t)context;
    phHal_sEventInfo_t  *pEvtInfo = NULL;     
    phLibNfc_uSeEvtInfo_t Se_Trans_Info={{{0,0},{0,0}}};
    phLibNfc_SE_List_t  *pSeInfo=NULL;  
    
    if(pLibContext != gpphLibContext)
    {
        /*wrong context returned*/
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
    }
    else
    {
        if((status == NFCSTATUS_SUCCESS) && (type == NFC_EVENT_NOTIFICATION))
        {
            pEvtInfo = info.psEventInfo;
            status = NFCSTATUS_SUCCESS;
            if((pEvtInfo->eventSource == phHal_ePICC_DevType )
                && (pEvtInfo->eventHost == phHal_eHostController) )
            {
                sSecuredElementInfo[LIBNFC_SE_SMARTMX_INDEX].eSE_Type = phLibNfc_SE_Type_SmartMX;
                /* Smartx Mx is Activated */
                pSeInfo = &sSecuredElementInfo[LIBNFC_SE_SMARTMX_INDEX];
            }
            if(pEvtInfo->eventHost == phHal_eUICCHost)
            { 
                /* UICC is Activate */
                sSecuredElementInfo[LIBNFC_SE_UICC_INDEX].eSE_Type = phLibNfc_SE_Type_UICC;
                pSeInfo = &sSecuredElementInfo[LIBNFC_SE_UICC_INDEX];           
            }
            else
            {
                /*presently Smx event source is not supported */
            }
            if(pSeInfo!=NULL)
            {
                switch(pEvtInfo->eventType)
                {
                    case NFC_EVT_TRANSACTION:
                    {
                        if((pEvtInfo->eventInfo.aid.length != 0) && ((pEvtInfo->eventInfo.aid.length <= 16)))  // PLG
                        {
                            /*copy the Application id on which transaction happened*/                        
							Se_Trans_Info.UiccEvtInfo.aid.buffer =pEvtInfo->eventInfo.aid.buffer;
							Se_Trans_Info.UiccEvtInfo.aid.length =pEvtInfo->eventInfo.aid.length;
                        }
						else
						{
							// PLG patch
                            Se_Trans_Info.UiccEvtInfo.aid.buffer = NULL;
							Se_Trans_Info.UiccEvtInfo.aid.length = 0;
						}
						if((pEvtInfo->eventHost == phHal_eUICCHost)
                           && (info.psEventInfo->eventInfo.uicc_info.param.length
                                != 0))
                        {
                            /*copy the parameters info on which transaction happened*/                       
							Se_Trans_Info.UiccEvtInfo.param.buffer =
										info.psEventInfo->eventInfo.uicc_info.param.buffer;
							Se_Trans_Info.UiccEvtInfo.param.length =
										info.psEventInfo->eventInfo.uicc_info.param.length;
                        }
                            /*Notify to upper layer that transaction had happened on the
                            one of the application stored in UICC or Smx*/
                        (*pLibContext->sSeContext.sSeCallabackInfo.pSeListenerNtfCb)(
                            pLibContext->sSeContext.sSeCallabackInfo.pSeListenerCtxt,
                            phLibNfc_eSE_EvtStartTransaction,
                            pSeInfo->hSecureElement,
                            &Se_Trans_Info,
                            status);
                        break;
                    }

                    case NFC_EVT_APDU_RECEIVED:
                    {
                        if ((pEvtInfo->eventInfo.aid.length != 0) && ((pEvtInfo->eventInfo.aid.length <= 16)))
                        {
                            /* Copy received APDU to aid buffer. */
                            Se_Trans_Info.UiccEvtInfo.aid.buffer = pEvtInfo->eventInfo.aid.buffer;
                            Se_Trans_Info.UiccEvtInfo.aid.length = pEvtInfo->eventInfo.aid.length;
                        }

                        (*pLibContext->sSeContext.sSeCallabackInfo.pSeListenerNtfCb)(
                            pLibContext->sSeContext.sSeCallabackInfo.pSeListenerCtxt,
                            phLibNfc_eSE_EvtApduReceived,
                            pSeInfo->hSecureElement,
                            &Se_Trans_Info,
                            status);
                        break;
                    }

                    case NFC_EVT_MIFARE_ACCESS:
                    {
                        /* copy the Block MIFARE accessed */
                        Se_Trans_Info.UiccEvtInfo.aid.buffer = pEvtInfo->eventInfo.aid.buffer;
                        Se_Trans_Info.UiccEvtInfo.aid.length = pEvtInfo->eventInfo.aid.length;

                        (*pLibContext->sSeContext.sSeCallabackInfo.pSeListenerNtfCb)(
                            pLibContext->sSeContext.sSeCallabackInfo.pSeListenerCtxt,
                            phLibNfc_eSE_EvtMifareAccess,
                            pSeInfo->hSecureElement,
                            &Se_Trans_Info,
                            status);
                        break;
                    }

                    case NFC_EVT_EMV_CARD_REMOVAL:
                    {
                        (*pLibContext->sSeContext.sSeCallabackInfo.pSeListenerNtfCb)(
                            pLibContext->sSeContext.sSeCallabackInfo.pSeListenerCtxt,
                            phLibNfc_eSE_EvtCardRemoval,
                            pSeInfo->hSecureElement,
                            &Se_Trans_Info,
                            status);
                        break;
                    }

                    case NFC_EVT_END_OF_TRANSACTION:
                    {
                        (*pLibContext->sSeContext.sSeCallabackInfo.pSeListenerNtfCb)(
                            pLibContext->sSeContext.sSeCallabackInfo.pSeListenerCtxt,
                            phLibNfc_eSE_EvtEndTransaction,
                            pSeInfo->hSecureElement,
                            &Se_Trans_Info,
                            status);
                        break;
                    }
                    case NFC_EVT_CONNECTIVITY:
                    {
                        (*pLibContext->sSeContext.sSeCallabackInfo.pSeListenerNtfCb)(
                            pLibContext->sSeContext.sSeCallabackInfo.pSeListenerCtxt,
                            phLibNfc_eSE_EvtConnectivity,
                            pSeInfo->hSecureElement,
                            &Se_Trans_Info,
                            status);
                        break;
                    }
                    case NFC_EVT_START_OF_TRANSACTION:
                    {
                        (*pLibContext->sSeContext.sSeCallabackInfo.pSeListenerNtfCb)(
                            pLibContext->sSeContext.sSeCallabackInfo.pSeListenerCtxt,
                            phLibNfc_eSE_EvtTypeTransaction,
                            pSeInfo->hSecureElement,
                            &Se_Trans_Info,
                            status);
                        break;
                    }
                    case NFC_EVT_FIELD_ON:
                    {
                        (*pLibContext->sSeContext.sSeCallabackInfo.pSeListenerNtfCb)(
                            pLibContext->sSeContext.sSeCallabackInfo.pSeListenerCtxt,
                            phLibNfc_eSE_EvtFieldOn,
                            pSeInfo->hSecureElement,
                            &Se_Trans_Info,
                            status);
                        break;
                    }
                    case NFC_EVT_FIELD_OFF:
                    {
                        (*pLibContext->sSeContext.sSeCallabackInfo.pSeListenerNtfCb)(
                             pLibContext->sSeContext.sSeCallabackInfo.pSeListenerCtxt,
                             phLibNfc_eSE_EvtFieldOff,
                             pSeInfo->hSecureElement,
                             &Se_Trans_Info,
                             status);
                        break;
                    }
                    default:
                    {
                        break;
                    }
                }
            }
            else
            {

            }           
         }        
    }
  return;
}

/**
 * Unregister the Secured Element Notification.
 */
NFCSTATUS phLibNfc_SE_NtfUnregister(void)
{
    NFCSTATUS Status = NFCSTATUS_SUCCESS;
    pphLibNfc_LibContext_t pLibContext=(pphLibNfc_LibContext_t)gpphLibContext;
     
    if((NULL == gpphLibContext) || 
        (gpphLibContext->LibNfcState.cur_state == eLibNfcHalStateShutdown))
    {
        /*Lib Nfc is not initialized*/
        Status = NFCSTATUS_NOT_INITIALISED;
    }
    else if(gpphLibContext->LibNfcState.next_state == eLibNfcHalStateShutdown)
    {
        Status = NFCSTATUS_SHUTDOWN;
    }
    else 
    {
        /*Unregister SE event notification with lower layer.
        even some transaction happens on UICC or Smx will not 
        be notified afterworlds */
        Status = phHal4Nfc_UnregisterNotification(
                                                pLibContext->psHwReference,
                                                eRegisterSecureElement,
                                                pLibContext);   
        if(Status != NFCSTATUS_SUCCESS)
        {
            /*Unregister failed*/
            Status=NFCSTATUS_FAILED;
        }
        pLibContext->sSeContext.sSeCallabackInfo.pSeListenerNtfCb=NULL;
        pLibContext->sSeContext.sSeCallabackInfo.pSeListenerCtxt=NULL;
    }
    return Status;
}

/**
* Get list of available Secure Elements
*/
NFCSTATUS phLibNfc_SE_GetSecureElementList(
                        phLibNfc_SE_List_t*     pSE_List,
                        uint8_t*                uSE_count
                        )
{        
    NFCSTATUS Status = NFCSTATUS_SUCCESS;  
	uint8_t    uNo_Of_SE = 0;

    if((NULL == gpphLibContext) || 
        (gpphLibContext->LibNfcState.cur_state == eLibNfcHalStateShutdown))
    {
        Status = NFCSTATUS_NOT_INITIALISED;
    }     
    else if((NULL ==pSE_List) || (NULL ==uSE_count))
    {   
        Status = NFCSTATUS_INVALID_PARAMETER;
    }
    else if(gpphLibContext->LibNfcState.next_state == eLibNfcHalStateShutdown)
    {
        Status = NFCSTATUS_SHUTDOWN;
    }
    else
    {
        /*Check for which type of Secure Element is available*/
        if(gpphLibContext->psHwReference->uicc_connected==TRUE)
        {
            /* Populate the UICC type */
            sSecuredElementInfo[LIBNFC_SE_UICC_INDEX].eSE_Type = phLibNfc_SE_Type_UICC;
                        
            /* Populate the UICC handle */
            sSecuredElementInfo[LIBNFC_SE_UICC_INDEX].hSecureElement =(phLibNfc_Handle)
                            (LIBNFC_SE_UICC_INDEX + LIBNFC_SE_BASE_HANDLE); 

#ifdef NXP_HAL_ENABLE_SMX

            pSE_List[LIBNFC_SE_UICC_INDEX].eSE_Type = 
				sSecuredElementInfo[LIBNFC_SE_UICC_INDEX].eSE_Type;
            pSE_List[LIBNFC_SE_UICC_INDEX].hSecureElement = (phLibNfc_Handle)
                            (LIBNFC_SE_UICC_INDEX + LIBNFC_SE_BASE_HANDLE);
            pSE_List[LIBNFC_SE_UICC_INDEX].eSE_CurrentState = 
				sSecuredElementInfo[LIBNFC_SE_UICC_INDEX].eSE_CurrentState; 
#else             
			pSE_List->eSE_Type = 
				sSecuredElementInfo[LIBNFC_SE_UICC_INDEX].eSE_Type;
            pSE_List->hSecureElement = (phLibNfc_Handle)
                            (LIBNFC_SE_UICC_INDEX + LIBNFC_SE_BASE_HANDLE);
            pSE_List->eSE_CurrentState = 
				sSecuredElementInfo[LIBNFC_SE_UICC_INDEX].eSE_CurrentState; 
#endif
            /* update the No of SE retrieved */
			uNo_Of_SE ++;
                        
        }
        if (gpphLibContext->psHwReference->smx_connected ==TRUE)
        {
            /* if the Smx is also connected to the PN544 */ 
            /* Populate the SMX type */
            sSecuredElementInfo[LIBNFC_SE_SMARTMX_INDEX].eSE_Type = phLibNfc_SE_Type_SmartMX;
                        
            /* Populate the SMX handle */
            sSecuredElementInfo[LIBNFC_SE_SMARTMX_INDEX].hSecureElement =(phLibNfc_Handle)
                            (LIBNFC_SE_SMARTMX_INDEX + LIBNFC_SE_BASE_HANDLE); 
            pSE_List[LIBNFC_SE_SMARTMX_INDEX].eSE_Type = 
				sSecuredElementInfo[LIBNFC_SE_SMARTMX_INDEX].eSE_Type;
            pSE_List[LIBNFC_SE_SMARTMX_INDEX].hSecureElement = (phLibNfc_Handle)
                            (LIBNFC_SE_SMARTMX_INDEX + LIBNFC_SE_BASE_HANDLE);
            pSE_List[LIBNFC_SE_SMARTMX_INDEX].eSE_CurrentState = 
				sSecuredElementInfo[LIBNFC_SE_SMARTMX_INDEX].eSE_CurrentState; 
            
            /* update the No of SE retrieved */
			uNo_Of_SE ++;
                         
        }
		*uSE_count = uNo_Of_SE;
    }
    return Status;
}

/**
* Sets secure element mode.
* This  function configures SE to specific mode based on activation mode type
*/

NFCSTATUS phLibNfc_SE_SetMode ( phLibNfc_Handle             hSE_Handle, 
                               phLibNfc_eSE_ActivationMode  eActivation_mode,
                               pphLibNfc_SE_SetModeRspCb_t  pSE_SetMode_Rsp_cb,
                               void *                       pContext
                               )
{
    NFCSTATUS Status = NFCSTATUS_SUCCESS;
    phHal_eEmulationType_t  eEmulationType = NFC_SMARTMX_EMULATION;
    pphLibNfc_LibContext_t pLibContext=(pphLibNfc_LibContext_t)gpphLibContext;
    
    if((NULL == gpphLibContext) || 
        (gpphLibContext->LibNfcState.cur_state == eLibNfcHalStateShutdown))
    {
        Status = NFCSTATUS_NOT_INITIALISED;
    }
	else if((pSE_SetMode_Rsp_cb ==NULL)
        ||(NULL == pContext)||(NULL==(void *)hSE_Handle))
    {
        Status=NFCSTATUS_INVALID_PARAMETER;
    }
	else if(gpphLibContext->LibNfcState.next_state == eLibNfcHalStateShutdown)
    {
        Status = NFCSTATUS_SHUTDOWN;
    }            
    else if((pLibContext->status.GenCb_pending_status == TRUE)
          ||(NULL!=pLibContext->sSeContext.sSeCallabackInfo.pSEsetModeCb))
    {
        /*previous callback is pending still*/
        Status =NFCSTATUS_REJECTED;
    }
    else 
    {
        phLibNfc_eSE_ActivationMode originalMode = pLibContext->sSeContext.eActivatedMode;
        switch(eActivation_mode)
        {
            case phLibNfc_SE_ActModeVirtual: 
            {
                if(hSE_Handle == sSecuredElementInfo[LIBNFC_SE_UICC_INDEX].hSecureElement)
                {
                    eEmulationType = NFC_UICC_EMULATION;  
                    /*Enable the UICC -External reader can see it*/
                    pLibContext->sCardEmulCfg.config.uiccEmuCfg.enableUicc = TRUE;                     
                }
                else if(hSE_Handle == sSecuredElementInfo[LIBNFC_SE_SMARTMX_INDEX].hSecureElement)
                {
                    eEmulationType = NFC_SMARTMX_EMULATION;  
                    /*Enable the SMX -External reader can see it*/
                    pLibContext->sCardEmulCfg.config.smartMxCfg.enableEmulation = TRUE;                    
                }
                else
                {
                    Status=NFCSTATUS_INVALID_HANDLE;
                }
                if(Status==NFCSTATUS_SUCCESS)
                {
                    if(pLibContext->sSeContext.eActivatedMode != phLibNfc_SE_ActModeWired)
                    {
                        pLibContext->sSeContext.eActivatedMode = phLibNfc_SE_ActModeVirtual;
                    }
                    pLibContext->sCardEmulCfg.emuType = eEmulationType;
                    Status = phHal4Nfc_ConfigParameters(
                                            pLibContext->psHwReference,
                                            NFC_EMULATION_CONFIG,           
                                            (phHal_uConfig_t*)&pLibContext->sCardEmulCfg,
                                            phLibNfc_SE_SetMode_cb,
                                            pLibContext);
                }
            }
            break;
            case phLibNfc_SE_ActModeVirtualVolatile:
            {
                if(hSE_Handle == sSecuredElementInfo[LIBNFC_SE_SMARTMX_INDEX].hSecureElement)
                {
                    eEmulationType = NFC_SMARTMX_EMULATION;
                    /*Enable the SMX -External reader can see it*/
                    pLibContext->sCardEmulCfg.config.smartMxCfg.enableEmulation = TRUE;
                    pLibContext->sSeContext.eActivatedMode = phLibNfc_SE_ActModeVirtualVolatile;

                    Status = phHal4Nfc_Switch_SMX_Mode(
                                        pLibContext->psHwReference,
                                        eSmartMx_Virtual,
                                        phLibNfc_SE_SetMode_cb,
                                        pLibContext
                                        );
                }
                else if(hSE_Handle == sSecuredElementInfo[LIBNFC_SE_UICC_INDEX].hSecureElement)
                {
                    eEmulationType = NFC_UICC_EMULATION;
                    /*Enable the UICC -External reader can see it*/
                    pLibContext->sCardEmulCfg.config.uiccEmuCfg.enableUicc = TRUE;
                    pLibContext->sSeContext.eActivatedMode = phLibNfc_SE_ActModeVirtualVolatile;

                    Status = phHal4Nfc_Switch_Swp_Mode(
                                        pLibContext->psHwReference,
                                        eSWP_Switch_On,
                                        phLibNfc_SE_SetMode_cb,
                                        pLibContext
                                        );
                }
                else
                {
                    Status = NFCSTATUS_INVALID_HANDLE;
                }
            }
            break;
            case phLibNfc_SE_ActModeDefault:
            {
                if(hSE_Handle == sSecuredElementInfo[LIBNFC_SE_SMARTMX_INDEX].hSecureElement)
                {
                    Status = phHal4Nfc_Switch_SMX_Mode(
                                        pLibContext->psHwReference,
                                        eSmartMx_Default,
                                        phLibNfc_SE_SetMode_cb,
                                        pLibContext
                                        );
                }
                else if(hSE_Handle == sSecuredElementInfo[LIBNFC_SE_UICC_INDEX].hSecureElement)
                {
                    Status = phHal4Nfc_Switch_Swp_Mode(
                                        pLibContext->psHwReference,
                                        eSWP_Switch_Default,
                                        phLibNfc_SE_SetMode_cb,
                                        pLibContext
                                        );
                }
                else
                {
                    Status = NFCSTATUS_INVALID_HANDLE;
                }
            }
            break;

            case phLibNfc_SE_ActModeWired:
            {
                if(hSE_Handle == sSecuredElementInfo[LIBNFC_SE_SMARTMX_INDEX].hSecureElement)
                {
                    if(pLibContext->CBInfo.pClientNtfRegRespCB!=NULL)
                    {   
                        /*Disable the SMX -External reader can't see it anymore*/
                        pLibContext->sCardEmulCfg.config.smartMxCfg.enableEmulation = FALSE; 
                        pLibContext->sSeContext.eActivatedMode = phLibNfc_SE_ActModeWired;

                        Status = phHal4Nfc_Switch_SMX_Mode(                                                  
                                            pLibContext->psHwReference,
                                            eSmartMx_Wired,
                                            phLibNfc_SE_SetMode_cb,
                                            pLibContext
                                            );     
                    }
                }
                else if(hSE_Handle == sSecuredElementInfo[LIBNFC_SE_UICC_INDEX].hSecureElement)
                {
                    /*This mode is not applicable to UICC*/
                    Status = NFCSTATUS_REJECTED;                    
                }
                else
                {
                    Status = NFCSTATUS_INVALID_HANDLE;     
                }  
            }  
            break;
        
            case phLibNfc_SE_ActModeOff:
            {
                /*UICC emulation deactivate*/
                if(hSE_Handle == sSecuredElementInfo[LIBNFC_SE_UICC_INDEX].hSecureElement)
                {
                    eEmulationType = NFC_UICC_EMULATION;
                    /*Disable the UICC -External reader can't see it anymore*/
                    pLibContext->sCardEmulCfg.config.uiccEmuCfg.enableUicc = FALSE;     
                    
                }
                else if(hSE_Handle == sSecuredElementInfo[LIBNFC_SE_SMARTMX_INDEX].hSecureElement)
                {
                    eEmulationType = NFC_SMARTMX_EMULATION;  
                    /*Disable the SMX -External reader can't see it anymore*/
                    pLibContext->sCardEmulCfg.config.smartMxCfg.enableEmulation=FALSE; 
                    
                }
                else
                {
                    Status = NFCSTATUS_INVALID_HANDLE; 
                }           
                if(Status==NFCSTATUS_SUCCESS)
                {
                    pLibContext->sCardEmulCfg.emuType = eEmulationType;

                    if(pLibContext->sSeContext.eActivatedMode != phLibNfc_SE_ActModeWired)
                    {
                         pLibContext->sSeContext.eActivatedMode = phLibNfc_SE_ActModeOff;
                    }

                    Status = phHal4Nfc_ConfigParameters(pLibContext->psHwReference,
                                                            NFC_EMULATION_CONFIG,           
                                                            (phHal_uConfig_t*)&pLibContext->sCardEmulCfg,
                                                            phLibNfc_SE_SetMode_cb,
                                                            pLibContext);
                }
            }  
            break;
            default:
                Status=NFCSTATUS_INVALID_PARAMETER;
                break;

        }/*End of eActivation_mode switch */       
        if(Status==NFCSTATUS_PENDING)
        {
            pLibContext->sSeContext.hSetemp=hSE_Handle;
            pLibContext->status.GenCb_pending_status = TRUE;
            pLibContext->sSeContext.sSeCallabackInfo.pSEsetModeCb = pSE_SetMode_Rsp_cb;
            pLibContext->sSeContext.sSeCallabackInfo.pSEsetModeCtxt=pContext;                       
        }
        else if(Status == NFCSTATUS_INVALID_HANDLE)
        {
            Status= Status;
        }
        else
        {
            // Restore original mode
            pLibContext->sSeContext.eActivatedMode = originalMode;
            Status = NFCSTATUS_FAILED;
        }
    }
    return Status;
}
/**
* Callback for Se Set mode 
*/
STATIC void phLibNfc_SE_SetMode_cb(void  *context, NFCSTATUS status)
{
    /* Note that we don't use the passed in context here;
     * the reason is that there are race-conditions around
     * the place where this context is stored (mostly in combination
     * with LLCP), and we may actually get the wrong context.
     * Since this callback always uses the global context
     * we don't need the passed in context anyway.
     */
    pphLibNfc_LibContext_t pLibContext=gpphLibContext;
    pphLibNfc_SE_SetModeRspCb_t  pUpperLayerCb=NULL;
    void                         *pUpperContext=NULL;
    phLibNfc_Handle              hSeHandle=0;
    uint8_t                      TempState=FALSE;  

    if(eLibNfcHalStateShutdown == gpphLibContext->LibNfcState.next_state)
    {
        /*If shutdown is called in between allow shutdown to happen*/
        phLibNfc_Pending_Shutdown();
        status = NFCSTATUS_SHUTDOWN;
    }
    else
    {
        if(status == NFCSTATUS_SUCCESS)
        {
            hSeHandle = pLibContext->sSeContext.hSetemp;

            if(hSeHandle == sSecuredElementInfo[LIBNFC_SE_UICC_INDEX].hSecureElement)
            {
                if(TRUE==pLibContext->sCardEmulCfg.config.uiccEmuCfg.enableUicc)
                {
                    /*If  Activation mode was virtual allow external reader to see it*/
                    pLibContext->sSeContext.uUiccActivate = TRUE;
                    sSecuredElementInfo[LIBNFC_SE_UICC_INDEX].eSE_CurrentState = phLibNfc_SE_Active;
                }
                else
                {
                    /*If  Activation mode was wired don't allow external reader to see it*/
                    pLibContext->sSeContext.uUiccActivate = FALSE;
                    sSecuredElementInfo[LIBNFC_SE_UICC_INDEX].eSE_CurrentState =
                                                                phLibNfc_SE_Inactive;
                }
                status = NFCSTATUS_SUCCESS;
                TempState = pLibContext->sSeContext.uUiccActivate;
            }
            else if (hSeHandle==sSecuredElementInfo[LIBNFC_SE_SMARTMX_INDEX].hSecureElement)
            {
                if(TRUE==pLibContext->sCardEmulCfg.config.smartMxCfg.enableEmulation)
                {
                    /*If  Activation mode was virtual allow external reader to see it*/
                    pLibContext->sSeContext.uSmxActivate = TRUE;
                    sSecuredElementInfo[LIBNFC_SE_SMARTMX_INDEX].eSE_CurrentState =
                                                                    phLibNfc_SE_Active;
                }
                else
                {
                    /*If  Activation mode was wired don't allow external reader to see it*/
                    pLibContext->sSeContext.uSmxActivate = FALSE;
                    sSecuredElementInfo[LIBNFC_SE_SMARTMX_INDEX].eSE_CurrentState=
                                                                    phLibNfc_SE_Inactive;
                }
                status = NFCSTATUS_SUCCESS;
                TempState = pLibContext->sSeContext.uSmxActivate;
            }
            else
            {
                status = NFCSTATUS_FAILED;
            }
        }
        else
        {
            status = NFCSTATUS_FAILED;
        }
        pLibContext->status.GenCb_pending_status = FALSE;
    }

    pUpperLayerCb = pLibContext->sSeContext.sSeCallabackInfo.pSEsetModeCb;
    pUpperContext = pLibContext->sSeContext.sSeCallabackInfo.pSEsetModeCtxt;  
    pLibContext->sSeContext.sSeCallabackInfo.pSEsetModeCb = NULL;
    pLibContext->sSeContext.sSeCallabackInfo.pSEsetModeCtxt = NULL;
	PHNFC_UNUSED_VARIABLE(TempState);
    /* Call the upper layer cb */
    if(pUpperLayerCb!= NULL )
    {
        (*pUpperLayerCb)(pUpperContext,                        
                        hSeHandle,
						status);
    }
    return;
}



