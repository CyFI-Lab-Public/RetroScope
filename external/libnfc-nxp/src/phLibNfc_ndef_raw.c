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
 * \file phLibNfc_ndef_raw.c

 * Project: NFC FRI 1.1
 *
 * $Date: Mon Dec 13 14:14:15 2010 $
 * $Author: ing02260 $
 * $Revision: 1.74 $
 * $Aliases:  $
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
#include <phFriNfc_NdefReg.h>
#include <phFriNfc_MifareStdMap.h>

/*
*************************** Macro's  ****************************************
*/

#ifndef STATIC_DISABLE
#define STATIC static
#else
//#undef STATIC
#define STATIC 
#endif

#define     TOPAZ_NDEF_BITMASK             0x10U
#define     TOPAZ_LEN_BITMASK              0x02U
#define     TOPAZ_DYNAMIC_LEN               460U
#define     TOPAZ_STATIC_CARD_LEN           128U
#define     MIFARE_STD_BLOCK_SIZE          0x10U
/*
*************************** Global Variables **********************************
*/
phLibNfc_Ndef_Info_t NdefInfo;
phFriNfc_NdefRecord_t *pNdefRecord=NULL;
/*
*************************** Static Function Declaration ***********************
*/

/* Response callback for Check Ndef */
STATIC 
void phLibNfc_Ndef_CheckNdef_Cb(void *pContext, NFCSTATUS status);

/* Response callback for Ndef Write */
STATIC 
void phLibNfc_Ndef_Write_Cb(void* Context,NFCSTATUS status);

/* Response callback for Ndef Read*/
STATIC 
void phLibNfc_Ndef_Read_Cb(void* Context,NFCSTATUS status);

/* Response callback forNdef Format*/
STATIC 
void phLibNfc_Ndef_format_Cb(void *Context,NFCSTATUS status);

#ifdef LIBNFC_READONLY_NDEF
STATIC
void
phLibNfc_Ndef_ReadOnly_Cb (
    void        *p_context,
    NFCSTATUS   status);
#endif /* #ifdef LIBNFC_READONLY_NDEF */

/* Response callback for Search Ndef Content */
STATIC
void phLibNfc_Ndef_SrchNdefCnt_Cb(void *context, NFCSTATUS status);

/* Response callback for Ndef Record Type Discovery */
STATIC
void phLibNfc_Ndef_Rtd_Cb( void *CallBackParam);

/* Response callback for Check Ndef timer callback */
STATIC void CheckNdef_timer_cb(uint32_t timer_id, void *pContext);

/*Callback for Presence check call from Chk Ndef*/
STATIC void phLibNfc_Ndef_ChkNdef_Pchk_Cb(void   *pContext,
                                NFCSTATUS  status
                                );
/*
*************************** Function Definitions ******************************
*/

/**
* This function reads an NDEF message from  already connected tag.
* the NDEF message  is read starting after the position of the
* last read operation of the same tag during current session.
*/

NFCSTATUS phLibNfc_Ndef_Read( phLibNfc_Handle                   hRemoteDevice,
                            phNfc_sData_t                      *psRd,
                            phLibNfc_Ndef_EOffset_t             Offset,
                            pphLibNfc_RspCb_t                   pNdefRead_RspCb,
                            void*                               pContext
                            )
{
    NFCSTATUS RetVal = NFCSTATUS_FAILED;

    if((NULL == gpphLibContext)|| 
        (gpphLibContext->LibNfcState.cur_state == eLibNfcHalStateShutdown))
    {
        RetVal = NFCSTATUS_NOT_INITIALISED;
    }
    else if((NULL == psRd) || (NULL == pNdefRead_RspCb)
        || (NULL == psRd->buffer)
        || (0 == psRd->length)
        || (NULL == pContext)
        || (0 == hRemoteDevice))
    {
        RetVal= NFCSTATUS_INVALID_PARAMETER;
    }    
    else if(gpphLibContext->LibNfcState.next_state == eLibNfcHalStateShutdown)
    {
        RetVal = NFCSTATUS_SHUTDOWN;
    }
    else if(0 == gpphLibContext->Connected_handle)
    {   /*presently no target or tag is connected*/ 
        RetVal=NFCSTATUS_TARGET_NOT_CONNECTED;        
    }
    else if(hRemoteDevice != gpphLibContext->Connected_handle)
    {   /*This handle of the device sent by application is not connected */ 
        RetVal=NFCSTATUS_INVALID_HANDLE;        
    }
    else if((TRUE == gpphLibContext->status.GenCb_pending_status)       
            ||(NULL!=gpphLibContext->CBInfo.pClientRdNdefCb)
            ||(CHK_NDEF_NOT_DONE == gpphLibContext->ndef_cntx.is_ndef))
    {
        /*Previous callback is pending*/
        RetVal = NFCSTATUS_REJECTED;
    }
    else if(gpphLibContext->ndef_cntx.is_ndef == FALSE)
    {
        /*no Ndef Support in tag*/
         RetVal = NFCSTATUS_NON_NDEF_COMPLIANT;
    }
    else if((gpphLibContext->ndef_cntx.is_ndef == TRUE)
        &&(0 == gpphLibContext->ndef_cntx.NdefActualSize))
    {
        /*Card is empty- So Returning length as zero*/
        psRd->length = 0;
        RetVal = NFCSTATUS_SUCCESS;
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
        gpphLibContext->psRemoteDevList->psRemoteDevInfo->SessionOpened = SESSION_OPEN;
        gpphLibContext->ndef_cntx.eLast_Call = NdefRd;
        if((((phHal_sRemoteDevInformation_t*)hRemoteDevice)->RemDevType == 
            phHal_eMifare_PICC) && (((phHal_sRemoteDevInformation_t*)
            hRemoteDevice)->RemoteDevInfo.Iso14443A_Info.Sak != 0)&&
            ((NULL == gpphLibContext->psBufferedAuth)
            ||(phHal_eMifareAuthentA == gpphLibContext->psBufferedAuth->cmd.MfCmd))
            )
        {
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
            }
            gpphLibContext->psBufferedAuth
                =(phLibNfc_sTransceiveInfo_t *) 
                phOsalNfc_GetMemory(sizeof(phLibNfc_sTransceiveInfo_t));            
            gpphLibContext->psBufferedAuth->addr = 
             (uint8_t)gpphLibContext->ndef_cntx.psNdefMap
             ->StdMifareContainer.currentBlock;
            gpphLibContext->psBufferedAuth->cmd.MfCmd = phHal_eMifareRead16;
            gpphLibContext->psBufferedAuth->sSendData.length
                = 0;            
            gpphLibContext->psBufferedAuth->sRecvData.length
                = MIFARE_STD_BLOCK_SIZE;                      
            gpphLibContext->psBufferedAuth->sRecvData.buffer
                = (uint8_t *)phOsalNfc_GetMemory(MIFARE_STD_BLOCK_SIZE);
            gpphLibContext->psBufferedAuth->sSendData.buffer
             = (uint8_t *)phOsalNfc_GetMemory(MIFARE_STD_BLOCK_SIZE);
        }
        if(eLibNfcHalStatePresenceChk !=
                gpphLibContext->LibNfcState.next_state)
        {
            uint8_t     cr_index = 0;
            gpphLibContext->ndef_cntx.psUpperNdefMsg = psRd;
            for (cr_index = 0; cr_index < PH_FRINFC_NDEFMAP_CR; cr_index++)
            {
                RetVal= phFriNfc_NdefMap_SetCompletionRoutine(
                                    gpphLibContext->ndef_cntx.psNdefMap,
                                    cr_index,
                                    phLibNfc_Ndef_Read_Cb,
                                    (void *)gpphLibContext);

            }
            gpphLibContext->ndef_cntx.NdefContinueRead =(uint8_t) ((phLibNfc_Ndef_EBegin==Offset) ?
                                                    PH_FRINFC_NDEFMAP_SEEK_BEGIN :
                                                    PH_FRINFC_NDEFMAP_SEEK_CUR);
            /* call below layer Ndef Read*/
            RetVal = phFriNfc_NdefMap_RdNdef(gpphLibContext->ndef_cntx.psNdefMap,
                            gpphLibContext->ndef_cntx.psUpperNdefMsg->buffer,
                            (uint32_t*)&gpphLibContext->ndef_cntx.psUpperNdefMsg->length,
                            gpphLibContext->ndef_cntx.NdefContinueRead);

            RetVal = PHNFCSTATUS(RetVal);
            if(NFCSTATUS_INSUFFICIENT_STORAGE == RetVal)
            {
                gpphLibContext->ndef_cntx.psUpperNdefMsg->length = 0;
                RetVal = NFCSTATUS_SUCCESS;
            }
        }
        else
        {
             gpphLibContext->CBInfo.pClientRdNdefCb= NULL;
             RetVal = NFCSTATUS_PENDING;
        }
        if(NFCSTATUS_PENDING == RetVal)
        {
            gpphLibContext->CBInfo.pClientRdNdefCb = pNdefRead_RspCb;
            gpphLibContext->CBInfo.pClientRdNdefCntx = pContext;
            gpphLibContext->status.GenCb_pending_status=TRUE;
			gpphLibContext->LibNfcState.next_state = eLibNfcHalStateTransaction; 
           
        }
        else if (NFCSTATUS_SUCCESS == RetVal)
        {
            RetVal= NFCSTATUS_SUCCESS;
        }
        else
        {
            /*Ndef read failed*/
            RetVal = NFCSTATUS_FAILED;
        }
    }
    return RetVal;
}
/* Response callback for phLibNfc_Ndef_Read */
STATIC
void phLibNfc_Ndef_Read_Cb(void* Context,NFCSTATUS status)
{
    NFCSTATUS               RetStatus = NFCSTATUS_SUCCESS;
    pphLibNfc_RspCb_t       pClientCb=NULL;
    phLibNfc_LibContext_t   *pLibNfc_Ctxt = (phLibNfc_LibContext_t *)Context;
    void                    *pUpperLayerContext=NULL;
    phHal_sRemoteDevInformation_t   *ps_rem_dev_info = NULL;

    if(pLibNfc_Ctxt != gpphLibContext)
    {
        /*wrong context returned*/
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
    }
    else
    {
        if(eLibNfcHalStateShutdown == gpphLibContext->LibNfcState.next_state)
        {   /*shutdown called before completion of Ndef read allow
              shutdown to happen */
            phLibNfc_Pending_Shutdown();
            RetStatus = NFCSTATUS_SHUTDOWN;    
        }
        else if(eLibNfcHalStateRelease == gpphLibContext->LibNfcState.next_state)
        {
            RetStatus = NFCSTATUS_ABORTED;
        }
        else
        {
            gpphLibContext->status.GenCb_pending_status = FALSE;
            if (gpphLibContext->psBufferedAuth != NULL && gpphLibContext->ndef_cntx.psNdefMap != NULL) {
                   gpphLibContext->psBufferedAuth->addr = (uint8_t)
                   gpphLibContext->ndef_cntx.psNdefMap->StdMifareContainer.currentBlock;
            }

            if(NFCSTATUS_FAILED == status )
            {
                /*During Ndef read operation tag was not present in RF
                field of reader*/
                RetStatus = NFCSTATUS_FAILED; 
                gpphLibContext->LastTrancvSuccess = FALSE;
                gpphLibContext->ndef_cntx.is_ndef = FALSE;
                ps_rem_dev_info = (phHal_sRemoteDevInformation_t *)
                                    gpphLibContext->Connected_handle;
                if ((phHal_eMifare_PICC == ps_rem_dev_info->RemDevType) && 
                    (0x08 == (ps_rem_dev_info->RemoteDevInfo.Iso14443A_Info.Sak & 0x08)) ||
                    (0x01 == ps_rem_dev_info->RemoteDevInfo.Iso14443A_Info.Sak))
                {

                    /* card type is mifare 1k/4k, then reconnect */
                    RetStatus = phHal4Nfc_Connect(gpphLibContext->psHwReference,  
                                ps_rem_dev_info,
                                (pphHal4Nfc_ConnectCallback_t)
                                phLibNfc_Reconnect_Mifare_Cb,
                                (void *)gpphLibContext);
                }

            }  
            else if(status == NFCSTATUS_SUCCESS)
            {
                gpphLibContext->LastTrancvSuccess = TRUE;
                RetStatus = NFCSTATUS_SUCCESS;
            }
		    else
		    {
                gpphLibContext->LastTrancvSuccess = FALSE;
				RetStatus = NFCSTATUS_FAILED;
			}
        }
        /*update the current state as connected*/
        phLibNfc_UpdateCurState(status,gpphLibContext);

        pClientCb = gpphLibContext->CBInfo.pClientRdNdefCb;
        pUpperLayerContext = gpphLibContext->CBInfo.pClientRdNdefCntx;

        gpphLibContext->CBInfo.pClientRdNdefCb = NULL;
        gpphLibContext->CBInfo.pClientRdNdefCntx = NULL;
        if(NFCSTATUS_PENDING != RetStatus)
        {
            if (NULL != pClientCb)
            {
                /*Notify to upper layer status and read bytes*/
                pClientCb(pUpperLayerContext,RetStatus);            
            }
        }
    }
    return;
}

/**
* Write NDEF to a tag.
*
* This function allows the user to write a NDEF data to already connected NFC
* tag.Function writes   a complete NDEF message to a tag. If a NDEF message
* already exists in the tag, it will be overwritten. When the transaction is
* complete,a notification callback is notified.
*/
NFCSTATUS phLibNfc_Ndef_Write(
                            phLibNfc_Handle          hRemoteDevice,
                            phNfc_sData_t           *psWr,                              
                            pphLibNfc_RspCb_t        pNdefWrite_RspCb,
                            void*                    pContext
                            )
{
    NFCSTATUS RetVal = NFCSTATUS_FAILED;
    uint8_t             NdefWriteType=0xFF;
    /*LibNfc is initilized or not */
    if((NULL == gpphLibContext)||
        (gpphLibContext->LibNfcState.cur_state
                            == eLibNfcHalStateShutdown))
    {   
        RetVal = NFCSTATUS_NOT_INITIALISED;
    }/*Check for application has sent the valid parameters*/
    else if((NULL == psWr) || (NULL == pNdefWrite_RspCb)
        || (NULL == psWr->buffer)
        || (NULL == pContext)
        || (0 ==hRemoteDevice))
    {
        RetVal= NFCSTATUS_INVALID_PARAMETER;
    }   
    else if(gpphLibContext->LibNfcState.next_state
                            == eLibNfcHalStateShutdown)
    {   /* Lib Nfc Shutdown*/
        RetVal= NFCSTATUS_SHUTDOWN;
    }
    else if(0 == gpphLibContext->Connected_handle)
    {       
        RetVal=NFCSTATUS_TARGET_NOT_CONNECTED;        
    }
    else if(hRemoteDevice != gpphLibContext->Connected_handle)
    {       
        RetVal=NFCSTATUS_INVALID_HANDLE;        
    }    
    else if((TRUE == gpphLibContext->status.GenCb_pending_status)||        
           (gpphLibContext->ndef_cntx.is_ndef == CHK_NDEF_NOT_DONE))
    {
         /* Previous callback is pending or Tag is not NDEF tag*/
        RetVal = NFCSTATUS_REJECTED;
        PHDBG_INFO("LIbNfc:Previous Callback is Pending");
    }
    else if(FALSE == gpphLibContext->ndef_cntx.is_ndef)
    {
        RetVal = NFCSTATUS_NON_NDEF_COMPLIANT;
    }
    else if(psWr->length > gpphLibContext->ndef_cntx.NdefLength)
    {
        RetVal = NFCSTATUS_NOT_ENOUGH_MEMORY;
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
        uint8_t         cr_index = 0;
        gpphLibContext->ndef_cntx.psUpperNdefMsg = psWr;
        gpphLibContext->ndef_cntx.AppWrLength= psWr->length;
        gpphLibContext->ndef_cntx.eLast_Call = NdefWr;
        gpphLibContext->psRemoteDevList->psRemoteDevInfo->SessionOpened 
            = SESSION_OPEN;
        if((((phHal_sRemoteDevInformation_t*)hRemoteDevice)->RemDevType == 
               phHal_eMifare_PICC) && (((phHal_sRemoteDevInformation_t*)
               hRemoteDevice)->RemoteDevInfo.Iso14443A_Info.Sak != 0)&&
               ((NULL == gpphLibContext->psBufferedAuth)
                ||(phHal_eMifareAuthentA == 
                   gpphLibContext->psBufferedAuth->cmd.MfCmd))
               )
        {
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
            }
            gpphLibContext->psBufferedAuth
                =(phLibNfc_sTransceiveInfo_t *) 
                phOsalNfc_GetMemory(sizeof(phLibNfc_sTransceiveInfo_t));            
            gpphLibContext->psBufferedAuth->addr = 
             (uint8_t)gpphLibContext->ndef_cntx.psNdefMap
             ->StdMifareContainer.currentBlock;
            gpphLibContext->psBufferedAuth->cmd.MfCmd = phHal_eMifareRead16;
            gpphLibContext->psBufferedAuth->sSendData.length
                = 0;            
            gpphLibContext->psBufferedAuth->sRecvData.length
                = MIFARE_STD_BLOCK_SIZE;                      
            gpphLibContext->psBufferedAuth->sRecvData.buffer
                = (uint8_t *)phOsalNfc_GetMemory(MIFARE_STD_BLOCK_SIZE);  
             gpphLibContext->psBufferedAuth->sSendData.buffer
                = (uint8_t *)phOsalNfc_GetMemory(MIFARE_STD_BLOCK_SIZE);
        }
        if(eLibNfcHalStatePresenceChk ==
                gpphLibContext->LibNfcState.next_state)
        {
            gpphLibContext->CBInfo.pClientWrNdefCb = NULL;
            RetVal = NFCSTATUS_PENDING;
        }
        else
        {
            for (cr_index = 0; cr_index < PH_FRINFC_NDEFMAP_CR; cr_index++)
            {
                /* Registering the Completion Routine.*/
                RetVal= phFriNfc_NdefMap_SetCompletionRoutine(
                                    gpphLibContext->ndef_cntx.psNdefMap,
                                    cr_index,
                                    phLibNfc_Ndef_Write_Cb,
                                    (void *)gpphLibContext);

            }
            if(0 == psWr->length)
            {
                 /* Length of bytes to be written Zero- Erase the Tag  */
                RetVal = phFriNfc_NdefMap_EraseNdef(gpphLibContext->ndef_cntx.psNdefMap);
            }
            else
            {
                /*Write from beginning or current location*/
                NdefWriteType = PH_FRINFC_NDEFMAP_SEEK_BEGIN; 
                /*Call FRI Ndef Write*/
                RetVal=phFriNfc_NdefMap_WrNdef(gpphLibContext->ndef_cntx.psNdefMap,
                            gpphLibContext->ndef_cntx.psUpperNdefMsg->buffer,
                            (uint32_t*)&gpphLibContext->ndef_cntx.psUpperNdefMsg->length,
                            NdefWriteType);
            }
            if(NFCSTATUS_PENDING == RetVal)
            {
                gpphLibContext->CBInfo.pClientWrNdefCb = pNdefWrite_RspCb;
                gpphLibContext->CBInfo.pClientWrNdefCntx = pContext;
                gpphLibContext->status.GenCb_pending_status=TRUE;
                gpphLibContext->LibNfcState.next_state = eLibNfcHalStateTransaction;
            }
            else
            {
                RetVal = NFCSTATUS_FAILED;
            }
        }    
    }
    return RetVal;
}

/* Response callback for phLibNfc_Ndef_Write */
STATIC
void phLibNfc_Ndef_Write_Cb(void* Context,NFCSTATUS status)
{

    pphLibNfc_RspCb_t       pClientCb=NULL;
    phLibNfc_LibContext_t   *pLibNfc_Ctxt = (phLibNfc_LibContext_t *)Context;
    void                    *pUpperLayerContext=NULL;
    phHal_sRemoteDevInformation_t   *ps_rem_dev_info = NULL;

    if(pLibNfc_Ctxt != gpphLibContext)
    {   /*wrong context returned*/
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
    }
    else
    {
        if(eLibNfcHalStateShutdown == gpphLibContext->LibNfcState.next_state)
        {   /*shutdown called before completion of Ndef write allow
              shutdown to happen */
            phLibNfc_Pending_Shutdown();
            status = NFCSTATUS_SHUTDOWN;    
        }
        else if(eLibNfcHalStateRelease == gpphLibContext->LibNfcState.next_state)
        {
            status = NFCSTATUS_ABORTED;
        }
        else
        {
            gpphLibContext->status.GenCb_pending_status = FALSE;
            if (gpphLibContext->psBufferedAuth != NULL && gpphLibContext->ndef_cntx.psNdefMap != NULL) {
                gpphLibContext->psBufferedAuth->addr = (uint8_t)
                    gpphLibContext->ndef_cntx.psNdefMap->TLVStruct.NdefTLVBlock;
            }
            if(status == NFCSTATUS_FAILED )
            {
				status = NFCSTATUS_FAILED;
                gpphLibContext->LastTrancvSuccess = FALSE;
                /*During Ndef write operation tag was not present in RF
                field of reader*/
                ps_rem_dev_info = (phHal_sRemoteDevInformation_t *)
                                    gpphLibContext->Connected_handle;
               if ((phHal_eMifare_PICC == ps_rem_dev_info->RemDevType) && 
                    (0x08 == (ps_rem_dev_info->RemoteDevInfo.Iso14443A_Info.Sak & 0x08)))
                {

               
                    /* card type is mifare 1k/4k, then reconnect */
                    status = phHal4Nfc_Connect(gpphLibContext->psHwReference,  
                                ps_rem_dev_info,
                                (pphHal4Nfc_ConnectCallback_t)
                                phLibNfc_Reconnect_Mifare_Cb,
                                (void *)gpphLibContext);
                }
            }
            else if( status== NFCSTATUS_SUCCESS)
            {
                gpphLibContext->LastTrancvSuccess = TRUE;
                status = NFCSTATUS_SUCCESS;
                if(gpphLibContext->ndef_cntx.AppWrLength >
                                 gpphLibContext->ndef_cntx.NdefLength)
                {
                    status = NFCSTATUS_NOT_ENOUGH_MEMORY;
                }
                else
                {
                    pLibNfc_Ctxt->ndef_cntx.NdefActualSize = 
                                    pLibNfc_Ctxt->ndef_cntx.psUpperNdefMsg->length;
                }
            }           
            else
            {
                gpphLibContext->LastTrancvSuccess = FALSE;
				status = NFCSTATUS_FAILED;;
			}
        }
        phLibNfc_UpdateCurState(status,gpphLibContext);

        pClientCb = gpphLibContext->CBInfo.pClientWrNdefCb;
        pUpperLayerContext = gpphLibContext->CBInfo.pClientWrNdefCntx;

        gpphLibContext->CBInfo.pClientWrNdefCb = NULL;
        gpphLibContext->CBInfo.pClientWrNdefCntx = NULL;
        if(NFCSTATUS_PENDING !=status)
        {
            if (NULL != pClientCb)
            {
                /*Notify to upper layer status and No. of bytes
                actually written */
                pClientCb(pUpperLayerContext, status);          
            }
        }
    }
    return;
}


/**
* Initialize structures needed for the Ndef 
* related operation such as Check Ndef, read, write
* and Ndef foramt.only once allocation 
*/
void phLibNfc_Ndef_Init(void)
{
    if(gpphLibContext->psTransInfo==NULL)
    {
        /*Allocate memory for Transceiveinformation Structure*/
        gpphLibContext->psTransInfo = (phLibNfc_sTransceiveInfo_t *)
            phOsalNfc_GetMemory(sizeof(phLibNfc_sTransceiveInfo_t));
    }
    if(gpphLibContext->psTransInfo==NULL)
    {
        /*exception: Not enough memory*/
        phOsalNfc_RaiseException(phOsalNfc_e_NoMemory,1);
        
    }
    if(NULL == gpphLibContext->ndef_cntx.psNdefMap)
    {
        /*Allocate memory for NDEF Mapping Component Context Structure*/
        gpphLibContext->ndef_cntx.psNdefMap = (phFriNfc_NdefMap_t *)
                    phOsalNfc_GetMemory(sizeof(phFriNfc_NdefMap_t));
    }
    if(NULL != gpphLibContext->ndef_cntx.psNdefMap)
    {
        /*Allocation successful*/
        (void)memset(gpphLibContext->ndef_cntx.psNdefMap,0,sizeof(phFriNfc_NdefMap_t));
        gpphLibContext->ndef_cntx.NdefSendRecvLen = NDEF_SENDRCV_BUF_LEN;
        gpphLibContext->ndef_cntx.psNdefMap->SendRecvBuf =
                (uint8_t*) phOsalNfc_GetMemory(gpphLibContext->
                ndef_cntx.NdefSendRecvLen);

        if(NULL != gpphLibContext->ndef_cntx.psNdefMap->SendRecvBuf)
        {
            (void)memset(gpphLibContext->ndef_cntx.psNdefMap->SendRecvBuf,
                0,
                gpphLibContext->ndef_cntx.NdefSendRecvLen);

            gpphLibContext->psOverHalCtxt =(phFriNfc_OvrHal_t *)
                phOsalNfc_GetMemory(sizeof(phFriNfc_OvrHal_t));
        }
    }
    if(NULL == gpphLibContext->psOverHalCtxt)
    {   /*exception: Not enough memory*/
        phOsalNfc_RaiseException(phOsalNfc_e_NoMemory,1);
    }
    else
    {
        
        (void)memset(gpphLibContext->psOverHalCtxt,0,
            sizeof(phFriNfc_OvrHal_t));
        
        /* Initialize the Overlapped hal structure*/
        gpphLibContext->psOverHalCtxt->psHwReference =
             gpphLibContext->psHwReference;
        if(NULL == gpphLibContext->psDevInputParam )
        {
            gpphLibContext->psDevInputParam = (phHal_sDevInputParam_t *)
                phOsalNfc_GetMemory(sizeof(phHal_sDevInputParam_t));
        }
        gpphLibContext->ndef_cntx.is_ndef = CHK_NDEF_NOT_DONE;      
    }
    if(NULL == gpphLibContext->ndef_cntx.ndef_fmt)
    {
        /*Allocate memory for Ndef format structure*/
        gpphLibContext->ndef_cntx.ndef_fmt = (phFriNfc_sNdefSmtCrdFmt_t *)
                phOsalNfc_GetMemory(sizeof(phFriNfc_sNdefSmtCrdFmt_t));
    }
    if(NULL != gpphLibContext->ndef_cntx.ndef_fmt)
    {
        (void)memset(gpphLibContext->ndef_cntx.ndef_fmt,
                        0,
                        sizeof(phFriNfc_sNdefSmtCrdFmt_t));
    }
    else
    {
        /*exception: Not enough memory*/
        phOsalNfc_RaiseException(phOsalNfc_e_NoMemory,1);
    }
    return;
}
/**
* Free the allocated memory used for Ndef operations 
*/
void phLibNfc_Ndef_DeInit(void)
{
    /* If only allocated then only free the memory*/
    if(gpphLibContext->ndef_cntx.psNdefMap !=NULL)
    {
        if(gpphLibContext->ndef_cntx.psNdefMap->SendRecvBuf !=NULL)
        {
            phOsalNfc_FreeMemory(gpphLibContext->ndef_cntx.psNdefMap->SendRecvBuf);
            gpphLibContext->ndef_cntx.psNdefMap->SendRecvBuf=NULL;
        }
        phOsalNfc_FreeMemory(gpphLibContext->ndef_cntx.psNdefMap);
        gpphLibContext->ndef_cntx.psNdefMap =NULL;
    }

    if(NULL != gpphLibContext->ndef_cntx.ndef_fmt)
    {
        phOsalNfc_FreeMemory(gpphLibContext->ndef_cntx.ndef_fmt);
        gpphLibContext->ndef_cntx.ndef_fmt = NULL;
    }

    if(gpphLibContext->psOverHalCtxt !=NULL)
    {
        phOsalNfc_FreeMemory(gpphLibContext->psOverHalCtxt);
        gpphLibContext->psOverHalCtxt =NULL;
    }
    if(gpphLibContext->psDevInputParam !=NULL)
    {
        phOsalNfc_FreeMemory(gpphLibContext->psDevInputParam);
        gpphLibContext->psDevInputParam = NULL;
    }
    if(gpphLibContext->psTransInfo!=NULL)
    {
        phOsalNfc_FreeMemory(gpphLibContext->psTransInfo);
        gpphLibContext->psTransInfo= NULL;
    }
}


/**
* This function allows  the user to check whether a particular Remote Device
* is NDEF compliant or not
*/
NFCSTATUS phLibNfc_Ndef_CheckNdef(phLibNfc_Handle       hRemoteDevice,
                        pphLibNfc_ChkNdefRspCb_t        pCheckNdef_RspCb,
                        void*                           pContext)
{
    NFCSTATUS RetVal = NFCSTATUS_FAILED;
    
    
    if((NULL == gpphLibContext)|| 
        (gpphLibContext->LibNfcState.cur_state == eLibNfcHalStateShutdown))
    {
        /*Lib Nfc not initialized*/
        RetVal = NFCSTATUS_NOT_INITIALISED;
    }
    else if((NULL == pCheckNdef_RspCb)||
        (NULL==pContext)||
        (hRemoteDevice == 0))
    {
        /*parameter sent by upper layer are not valid */
        RetVal= NFCSTATUS_INVALID_PARAMETER;
    }
    else if(gpphLibContext->LibNfcState.next_state == eLibNfcHalStateShutdown)
    {
        RetVal = NFCSTATUS_SHUTDOWN;
    }    
    else if(0 == gpphLibContext->Connected_handle)
    {       
        RetVal=NFCSTATUS_TARGET_NOT_CONNECTED;        
    }
    else if(hRemoteDevice != gpphLibContext->Connected_handle)
    {       
        RetVal=NFCSTATUS_INVALID_HANDLE;        
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
        uint8_t     cr_index = 0;
        static uint16_t     data_cnt = 0;
        /* Allocate memory for the ndef related structure */       
        gpphLibContext->ndef_cntx.NdefSendRecvLen=300;
        gpphLibContext->ndef_cntx.eLast_Call = ChkNdef;
        
        /* Resets the component instance */
        RetVal = phFriNfc_NdefMap_Reset( gpphLibContext->ndef_cntx.psNdefMap,
                            gpphLibContext->psOverHalCtxt,
                            (phLibNfc_sRemoteDevInformation_t*)hRemoteDevice,
                            gpphLibContext->psDevInputParam,
                            gpphLibContext->ndef_cntx.psNdefMap->SendRecvBuf,
                            gpphLibContext->ndef_cntx.NdefSendRecvLen,
                            gpphLibContext->ndef_cntx.psNdefMap->SendRecvBuf,
                            &(gpphLibContext->ndef_cntx.NdefSendRecvLen),
                            &(data_cnt));


        for (cr_index = 0; cr_index < PH_FRINFC_NDEFMAP_CR; cr_index++)
        {
            /* Register the callback for the check ndef */
            RetVal = phFriNfc_NdefMap_SetCompletionRoutine(
                                gpphLibContext->ndef_cntx.psNdefMap,
                                cr_index,
                                phLibNfc_Ndef_CheckNdef_Cb,
                                (void *)gpphLibContext);
        }
        /*call below layer check Ndef function*/
        RetVal = phFriNfc_NdefMap_ChkNdef(gpphLibContext->ndef_cntx.psNdefMap);
        RetVal =PHNFCSTATUS(RetVal);

        if(RetVal== NFCSTATUS_PENDING)
        {
            RetVal = NFCSTATUS_PENDING;
        }        
        else if((RetVal == NFCSTATUS_FAILED) || (RetVal ==(PHNFCSTVAL(CID_FRI_NFC_NDEF_MAP,
                    NFCSTATUS_INVALID_REMOTE_DEVICE))))
        {
			      RetVal= NFCSTATUS_FAILED;            
        }
        else
        {
            if((0x00 == gpphLibContext->ndef_cntx.Chk_Ndef_Timer_Id)||
              (PH_OSALNFC_INVALID_TIMER_ID == gpphLibContext->ndef_cntx.Chk_Ndef_Timer_Id))
	          {
		            gpphLibContext->ndef_cntx.Chk_Ndef_Timer_Id =
			          phOsalNfc_Timer_Create();      			
	          }	
	          if((0x00 == gpphLibContext->ndef_cntx.Chk_Ndef_Timer_Id)||
              (PH_OSALNFC_INVALID_TIMER_ID == gpphLibContext->ndef_cntx.Chk_Ndef_Timer_Id))
	          {
		            RetVal = NFCSTATUS_FAILED;
	          }
	          else
	          {
	              phOsalNfc_Timer_Start(gpphLibContext->ndef_cntx.Chk_Ndef_Timer_Id,
			          CHK_NDEF_TIMER_TIMEOUT,CheckNdef_timer_cb,NULL);	
                RetVal = NFCSTATUS_PENDING;
	          }            
        }
        if(RetVal== NFCSTATUS_PENDING)
        {
            gpphLibContext->CBInfo.pClientCkNdefCb = pCheckNdef_RspCb;
            gpphLibContext->CBInfo.pClientCkNdefCntx = pContext;
            gpphLibContext->status.GenCb_pending_status=TRUE;
            gpphLibContext->LibNfcState.next_state = eLibNfcHalStateTransaction;
        }
        
    }
    return RetVal;
}

/* Response callback for phLibNfc_Ndef_CheckNdef */
STATIC
void phLibNfc_Ndef_CheckNdef_Cb(void *pContext,NFCSTATUS status)
{
    phLibNfc_ChkNdef_Info_t    Ndef_Info;
    NFCSTATUS RetStatus = NFCSTATUS_SUCCESS;
    pphLibNfc_ChkNdefRspCb_t       pClientCb=NULL;
    phLibNfc_LibContext_t           *pLibNfc_Ctxt = 
                                    (phLibNfc_LibContext_t *)pContext;
    void                    *pUpperLayerContext=NULL;
    phHal_sRemoteDevInformation_t   *ps_rem_dev_info = NULL;                                    

    Ndef_Info.ActualNdefMsgLength = 0;
    Ndef_Info.MaxNdefMsgLength = 0;
    Ndef_Info.NdefCardState = PHLIBNFC_NDEF_CARD_INVALID;
    if(pLibNfc_Ctxt != gpphLibContext)
    {    /*wrong context returned from below layer*/
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
    }
    else
    {
        ps_rem_dev_info = (phHal_sRemoteDevInformation_t *)
                                    gpphLibContext->Connected_handle;
        if(eLibNfcHalStateShutdown == gpphLibContext->LibNfcState.next_state)
        {   /*shutdown called before completion of check Ndef, allow
              shutdown to happen */
            phLibNfc_Pending_Shutdown();
            RetStatus = NFCSTATUS_SHUTDOWN;    
        }
        else if(eLibNfcHalStateRelease == gpphLibContext->LibNfcState.next_state)
        {
            RetStatus = NFCSTATUS_ABORTED;          
        }
        else
        {
            if(status == NFCSTATUS_SUCCESS)
            {
                /*Tag is Ndef tag*/
                gpphLibContext->ndef_cntx.is_ndef = TRUE;
                (void)phFriNfc_NdefMap_GetContainerSize(
                                pLibNfc_Ctxt->ndef_cntx.psNdefMap,
                                &(pLibNfc_Ctxt->ndef_cntx.NdefLength),
                                &(pLibNfc_Ctxt->ndef_cntx.NdefActualSize));
                /*Get the data size support by particular ndef card */
                Ndef_Info.ActualNdefMsgLength = pLibNfc_Ctxt->ndef_cntx.NdefActualSize;
                Ndef_Info.MaxNdefMsgLength = pLibNfc_Ctxt->ndef_cntx.NdefLength;
                gpphLibContext->LastTrancvSuccess = TRUE;
                RetStatus =NFCSTATUS_SUCCESS;
            }
            else if (PHNFCSTATUS(status) != NFCSTATUS_MORE_INFORMATION )
            {
                /*Ndef check Failed.Issue a PresenceChk to ascertain if tag is
                  still in the field*/
                RetStatus = phHal4Nfc_PresenceCheck(
                                    gpphLibContext->psHwReference,
                                    phLibNfc_Ndef_ChkNdef_Pchk_Cb,
                                    (void *)gpphLibContext
                                    );
            }             
            else 
            { 
				RetStatus = NFCSTATUS_FAILED; 
                gpphLibContext->LastTrancvSuccess = FALSE;
                gpphLibContext->ndef_cntx.is_ndef = FALSE;
                               
                if ((phHal_eMifare_PICC == ps_rem_dev_info->RemDevType) && 
                    (0x08 == (ps_rem_dev_info->RemoteDevInfo.Iso14443A_Info.Sak & 0x08)))
                {

                    /* card type is mifare 1k/4k, then reconnect */
                    RetStatus = phHal4Nfc_Connect(gpphLibContext->psHwReference,  
                                ps_rem_dev_info,
                                (pphHal4Nfc_ConnectCallback_t)
                                phLibNfc_Reconnect_Mifare_Cb,
                                (void *)gpphLibContext);
                }   
                else
                {
                   if((phHal_eJewel_PICC == ps_rem_dev_info->RemDevType)
                       &&(TOPAZ_NDEF_BITMASK & 
                          ps_rem_dev_info->RemoteDevInfo.Jewel_Info.HeaderRom0))
                    {                        
                        gpphLibContext->ndef_cntx.is_ndef = TRUE;
                        RetStatus = phFriNfc_NdefMap_GetContainerSize(
                                        pLibNfc_Ctxt->ndef_cntx.psNdefMap,
                                        &(pLibNfc_Ctxt->ndef_cntx.NdefLength),
                                        &(pLibNfc_Ctxt->ndef_cntx.NdefActualSize));
                        /*Get the data size support by particular ndef card */
                        Ndef_Info.ActualNdefMsgLength = 
                            pLibNfc_Ctxt->ndef_cntx.NdefActualSize;
                        Ndef_Info.MaxNdefMsgLength 
                            = pLibNfc_Ctxt->ndef_cntx.NdefLength
                            = (TOPAZ_LEN_BITMASK & 
                            ps_rem_dev_info->RemoteDevInfo.Jewel_Info.HeaderRom0?
                            TOPAZ_DYNAMIC_LEN:TOPAZ_STATIC_CARD_LEN);   
                        RetStatus = NFCSTATUS_SUCCESS;
                    }
                }                         
            }
            gpphLibContext->LibNfcState.cur_state=eLibNfcHalStateConnect;
        }
        gpphLibContext->status.GenCb_pending_status = FALSE;
        /* Update the current state */
        phLibNfc_UpdateCurState(RetStatus,gpphLibContext);
        if(NFCSTATUS_PENDING != RetStatus)
        {
            if(((ps_rem_dev_info->RemDevType == phHal_eMifare_PICC) 
                && (ps_rem_dev_info->RemoteDevInfo.Iso14443A_Info.Sak != 0)&&
               ((NULL == gpphLibContext->psBufferedAuth)
                ||(phHal_eMifareAuthentA == gpphLibContext->psBufferedAuth->cmd.MfCmd)))
               )
            {
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
                }
                gpphLibContext->psBufferedAuth
                    =(phLibNfc_sTransceiveInfo_t *) 
                    phOsalNfc_GetMemory(sizeof(phLibNfc_sTransceiveInfo_t));                
                gpphLibContext->psBufferedAuth->addr = 
                (uint8_t)gpphLibContext->ndef_cntx.psNdefMap
                ->StdMifareContainer.currentBlock;
                gpphLibContext->psBufferedAuth->cmd.MfCmd = phHal_eMifareRead16;
                gpphLibContext->psBufferedAuth->sSendData.length
                    = 0;            
                gpphLibContext->psBufferedAuth->sRecvData.length
                    = MIFARE_STD_BLOCK_SIZE;                      
                gpphLibContext->psBufferedAuth->sRecvData.buffer
                    = (uint8_t *)phOsalNfc_GetMemory(MIFARE_STD_BLOCK_SIZE);
                gpphLibContext->psBufferedAuth->sSendData.buffer
                    = (uint8_t *)phOsalNfc_GetMemory(MIFARE_STD_BLOCK_SIZE); 
            }
            pClientCb = gpphLibContext->CBInfo.pClientCkNdefCb;
            pUpperLayerContext = gpphLibContext->CBInfo.pClientCkNdefCntx;
            gpphLibContext->CBInfo.pClientCkNdefCb = NULL;
            gpphLibContext->CBInfo.pClientCkNdefCntx = NULL;
            if(NULL != pClientCb)
            {
                if (!RetStatus)
                {
                    switch (pLibNfc_Ctxt->ndef_cntx.psNdefMap->CardState)
                    {
                        case PH_NDEFMAP_CARD_STATE_INITIALIZED:
                        {
                            Ndef_Info.NdefCardState = 
                                            PHLIBNFC_NDEF_CARD_INITIALISED;
                            break;
                        }

                        case PH_NDEFMAP_CARD_STATE_READ_ONLY:
                        {
                            Ndef_Info.NdefCardState = 
                                            PHLIBNFC_NDEF_CARD_READ_ONLY;
                            break;
                        }

                        case PH_NDEFMAP_CARD_STATE_READ_WRITE:
                        {
                            Ndef_Info.NdefCardState = 
                                            PHLIBNFC_NDEF_CARD_READ_WRITE;
                            break;
                        }

                        default:
                        {
                            Ndef_Info.NdefCardState = 
                                            PHLIBNFC_NDEF_CARD_INVALID;
                            break;
                        }
                    }
                }
                /* call the upper check ndef callback */
                pClientCb(pUpperLayerContext,Ndef_Info,RetStatus);
            }
        }
    }
    return;
}

/*Callback for Presence check call from Chk Ndef*/
STATIC void phLibNfc_Ndef_ChkNdef_Pchk_Cb(void   *pContext,
                                NFCSTATUS  status
                                )
{
    phLibNfc_ChkNdef_Info_t    Ndef_Info = {0,0,0};
    NFCSTATUS RetStatus = NFCSTATUS_SUCCESS;
    pphLibNfc_ChkNdefRspCb_t       pClientCb=NULL;
    phLibNfc_LibContext_t           *pLibNfc_Ctxt = 
                                    (phLibNfc_LibContext_t *)pContext;
    void                    *pUpperLayerContext=NULL;
    phHal_sRemoteDevInformation_t   *ps_rem_dev_info = NULL;  
    if(NFCSTATUS_SUCCESS == status)
    {
        RetStatus = NFCSTATUS_FAILED;
        gpphLibContext->ndef_cntx.is_ndef = FALSE;
    }
    else
    {
        RetStatus = NFCSTATUS_TARGET_LOST;
    }    
    if(pLibNfc_Ctxt != gpphLibContext)
    {    /*wrong context returned from below layer*/
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
    }
    else
    {
        ps_rem_dev_info = (phHal_sRemoteDevInformation_t *)
                                    gpphLibContext->Connected_handle;
        if(((ps_rem_dev_info->RemDevType == phHal_eMifare_PICC) 
            && (ps_rem_dev_info->RemoteDevInfo.Iso14443A_Info.Sak != 0)&&
            ((NULL == gpphLibContext->psBufferedAuth)
            ||(phHal_eMifareAuthentA == gpphLibContext->psBufferedAuth->cmd.MfCmd)))
            )
        {
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
            }
            gpphLibContext->psBufferedAuth
                =(phLibNfc_sTransceiveInfo_t *) 
                phOsalNfc_GetMemory(sizeof(phLibNfc_sTransceiveInfo_t));                
            gpphLibContext->psBufferedAuth->addr = 
            (uint8_t)gpphLibContext->ndef_cntx.psNdefMap
            ->StdMifareContainer.currentBlock;
            gpphLibContext->psBufferedAuth->cmd.MfCmd = phHal_eMifareRead16;
            gpphLibContext->psBufferedAuth->sSendData.length
                = 0;            
            gpphLibContext->psBufferedAuth->sRecvData.length
                = MIFARE_STD_BLOCK_SIZE;                      
            gpphLibContext->psBufferedAuth->sRecvData.buffer
                = (uint8_t *)phOsalNfc_GetMemory(MIFARE_STD_BLOCK_SIZE);
            gpphLibContext->psBufferedAuth->sSendData.buffer
                = (uint8_t *)phOsalNfc_GetMemory(MIFARE_STD_BLOCK_SIZE); 
        }
        pClientCb = gpphLibContext->CBInfo.pClientCkNdefCb;
        pUpperLayerContext = gpphLibContext->CBInfo.pClientCkNdefCntx;
        gpphLibContext->CBInfo.pClientCkNdefCb = NULL;
        gpphLibContext->CBInfo.pClientCkNdefCntx = NULL;
        if(NULL != pClientCb)
        {
            Ndef_Info.NdefCardState = PHLIBNFC_NDEF_CARD_INVALID;
            /* call the upper check ndef callback */
            pClientCb(pUpperLayerContext,Ndef_Info,RetStatus);
        }
    }
    return;
}
/* Check Ndef Timer Callback*/
STATIC void CheckNdef_timer_cb(uint32_t timer_id, void *pContext)
{
	PHNFC_UNUSED_VARIABLE(pContext);
   phOsalNfc_Timer_Stop(timer_id);
	phOsalNfc_Timer_Delete(gpphLibContext->ndef_cntx.Chk_Ndef_Timer_Id);
	gpphLibContext->ndef_cntx.Chk_Ndef_Timer_Id = 0x00;
	phLibNfc_Ndef_CheckNdef_Cb((void *)gpphLibContext,NFCSTATUS_MORE_INFORMATION);
}

void phLibNfc_Reconnect_Mifare_Cb (
                    void                            *pContext,
                    phHal_sRemoteDevInformation_t   *psRemoteDevInfo,
                    NFCSTATUS                       status)
{
    phLibNfc_ChkNdef_Info_t     Ndef_Info;      
    phLibNfc_LibContext_t       *pLibNfc_Ctxt = 
                                (phLibNfc_LibContext_t *)pContext;
    void                        *pUpperLayerContext = NULL;
    switch(gpphLibContext->ndef_cntx.eLast_Call)
    {
        case ChkNdef:
        {
            pphLibNfc_ChkNdefRspCb_t    pClientCb=NULL;
            pClientCb = pLibNfc_Ctxt->CBInfo.pClientCkNdefCb;
            pUpperLayerContext = pLibNfc_Ctxt->CBInfo.pClientCkNdefCntx;
            pLibNfc_Ctxt->CBInfo.pClientCkNdefCb = NULL;
            pLibNfc_Ctxt->CBInfo.pClientCkNdefCntx = NULL;
            if (NULL != pClientCb)
            {
                status = (NFCSTATUS_SUCCESS == status?
                        NFCSTATUS_FAILED:NFCSTATUS_TARGET_LOST);
                Ndef_Info.ActualNdefMsgLength = 0;
                Ndef_Info.MaxNdefMsgLength = 0;
                /* call the upper check ndef callback */
                pClientCb(pUpperLayerContext,Ndef_Info,status);
            }
        }
        break;
        case NdefRd:
        {
            pphLibNfc_RspCb_t       pClientCb = pLibNfc_Ctxt->CBInfo.pClientRdNdefCb; 
            pUpperLayerContext = pLibNfc_Ctxt->CBInfo.pClientRdNdefCntx;
            pLibNfc_Ctxt->CBInfo.pClientRdNdefCb = NULL;
            pLibNfc_Ctxt->CBInfo.pClientRdNdefCntx = NULL;
            if (NULL != pClientCb)
            {
                status = (NFCSTATUS_SUCCESS == status?
                        NFCSTATUS_FAILED:NFCSTATUS_TARGET_LOST);            
                /* call the upper ndef read callback */
                pClientCb(pUpperLayerContext,status);
            }
        }
        break;
        case NdefWr:
        {
            pphLibNfc_RspCb_t       pClientCb =  pLibNfc_Ctxt->CBInfo.pClientWrNdefCb;        
            pUpperLayerContext = pLibNfc_Ctxt->CBInfo.pClientWrNdefCntx;
            pLibNfc_Ctxt->CBInfo.pClientWrNdefCb = NULL;
            pLibNfc_Ctxt->CBInfo.pClientWrNdefCntx = NULL;
            if (NULL != pClientCb)
            {
                status = (NFCSTATUS_SUCCESS == status?
                        NFCSTATUS_FAILED:NFCSTATUS_TARGET_LOST);              
                /* call the upper ndef write callback */
                pClientCb(pUpperLayerContext,status);
            }
        }
        break;
        case NdefFmt:
#ifdef LIBNFC_READONLY_NDEF
        case NdefReadOnly:
#endif /* #ifdef LIBNFC_READONLY_NDEF */
        {
            pphLibNfc_RspCb_t       pClientCb =
                           pLibNfc_Ctxt->ndef_cntx.pClientNdefFmtCb;  
            pUpperLayerContext= pLibNfc_Ctxt->ndef_cntx.pClientNdefFmtCntx;
            pLibNfc_Ctxt->ndef_cntx.pClientNdefFmtCb = NULL;
            pLibNfc_Ctxt->ndef_cntx.pClientNdefFmtCntx = NULL;
            if (NULL != pClientCb)
            {
                status = (NFCSTATUS_SUCCESS == status?
                        NFCSTATUS_FAILED:NFCSTATUS_TARGET_LOST);            
                /* call the upper ndef format callback */
                pClientCb(pUpperLayerContext,status);
            }
        }
        break;
        case RawTrans:
        {
            phNfc_sData_t trans_resp;           
            pphLibNfc_TransceiveCallback_t pClientCb =
                           pLibNfc_Ctxt->CBInfo.pClientTransceiveCb;
            trans_resp.length = 0;
            pUpperLayerContext = pLibNfc_Ctxt->CBInfo.pClientTranseCntx;
            pLibNfc_Ctxt->CBInfo.pClientTranseCntx= NULL;
            pLibNfc_Ctxt->CBInfo.pClientTransceiveCb= NULL;
            if (NULL != pClientCb)
            {
                status = (NFCSTATUS_SUCCESS == status?
                        NFCSTATUS_FAILED:NFCSTATUS_TARGET_LOST);             
                /* call the upper transceive callback */
                pClientCb(pUpperLayerContext,
                        (uint32_t)psRemoteDevInfo,
                        & trans_resp,
                        status);                
            }
        }
        break;
        default:
        {
        }
        break;
    }
    
}
/**
* Target format to make it NDEF compliant
*/
NFCSTATUS phLibNfc_RemoteDev_FormatNdef(phLibNfc_Handle         hRemoteDevice,
                                        phNfc_sData_t*          pScrtKey,
                                        pphLibNfc_RspCb_t       pNdefformat_RspCb,
                                        void*                   pContext
                                        )
{
    NFCSTATUS RetVal = NFCSTATUS_FAILED;
    
    static uint8_t       mif_std_key[6] ={0},
                         Index = 0;
    if((NULL == gpphLibContext)
        ||(gpphLibContext->LibNfcState.cur_state
                            == eLibNfcHalStateShutdown))
    {
        /*Lib Nfc not initialized*/
        RetVal = NFCSTATUS_NOT_INITIALISED;
    }    
    else if((NULL == pContext) 
        || (NULL == pNdefformat_RspCb)       
        ||(NULL == pScrtKey)
        ||(0 == hRemoteDevice))
    {
        RetVal= NFCSTATUS_INVALID_PARAMETER;
    }
    else if(gpphLibContext->LibNfcState.next_state
                            == eLibNfcHalStateShutdown)
    {
        RetVal= NFCSTATUS_SHUTDOWN;
    }
    else if(0 == gpphLibContext->Connected_handle)
    {       
        RetVal=NFCSTATUS_TARGET_NOT_CONNECTED;        
    }
    else if(hRemoteDevice != gpphLibContext->Connected_handle)
    {       
        RetVal=NFCSTATUS_INVALID_HANDLE;        
    }
    else if((TRUE == gpphLibContext->status.GenCb_pending_status)||
        (NULL != gpphLibContext->ndef_cntx.pClientNdefFmtCb)
        ||(gpphLibContext->ndef_cntx.is_ndef == TRUE))
    {
        /*Previous Callback is Pending*/
        RetVal = NFCSTATUS_REJECTED;
        PHDBG_INFO("LIbNfc:Previous Callback is Pending");
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
        uint8_t   fun_id;       
        gpphLibContext->ndef_cntx.eLast_Call = NdefFmt;        
        gpphLibContext->ndef_cntx.NdefSendRecvLen = NDEF_SENDRCV_BUF_LEN;
    
        /* Call ndef format reset, this will initialize the ndef
        format structure, and appropriate values are filled */
        RetVal = phFriNfc_NdefSmtCrd_Reset(gpphLibContext->ndef_cntx.ndef_fmt,
                            gpphLibContext->psOverHalCtxt,
                            (phHal_sRemoteDevInformation_t*)hRemoteDevice,
                            gpphLibContext->psDevInputParam,
                            gpphLibContext->ndef_cntx.psNdefMap->SendRecvBuf,
                            &(gpphLibContext->ndef_cntx.NdefSendRecvLen));
        for(fun_id = 0; fun_id < PH_FRINFC_SMTCRDFMT_CR; fun_id++)
        {
            /* Register for all the callbacks */
            RetVal = phFriNfc_NdefSmtCrd_SetCR(gpphLibContext->ndef_cntx.ndef_fmt,
                        fun_id,
                        phLibNfc_Ndef_format_Cb,
                        gpphLibContext);
        }
        /* mif_std_key is required to format the mifare 1k/4k card */
        for (Index =0 ;Index < (pScrtKey->length); Index++ )
        {
            mif_std_key[Index] = *(pScrtKey->buffer++);
        }
        /* Start smart card formatting function   */
        RetVal = phFriNfc_NdefSmtCrd_Format(gpphLibContext->ndef_cntx.ndef_fmt,
                                        mif_std_key);
		RetVal = PHNFCSTATUS(RetVal);
        if(RetVal== NFCSTATUS_PENDING)
        {
            gpphLibContext->ndef_cntx.pClientNdefFmtCb = pNdefformat_RspCb;
            gpphLibContext->ndef_cntx.pClientNdefFmtCntx = pContext;
            gpphLibContext->status.GenCb_pending_status=TRUE;
            gpphLibContext->LibNfcState.next_state = eLibNfcHalStateTransaction;
        }
        else
        {
            RetVal = NFCSTATUS_FAILED;
        }       
    }
    return RetVal;
}

#ifdef LIBNFC_READONLY_NDEF

NFCSTATUS
phLibNfc_ConvertToReadOnlyNdef (
    phLibNfc_Handle         hRemoteDevice,
    phNfc_sData_t*          pScrtKey,
    pphLibNfc_RspCb_t       pNdefReadOnly_RspCb,
    void*                   pContext
    )
{
    NFCSTATUS           ret_val = NFCSTATUS_FAILED;
    static uint8_t      mif_std_key[6] ={0},
                        Index = 0;

    if ((NULL == gpphLibContext)
        || (gpphLibContext->LibNfcState.cur_state
                            == eLibNfcHalStateShutdown))
    {
        /* LibNfc not initialized */
        ret_val = NFCSTATUS_NOT_INITIALISED;
    }
    else if ((NULL == pContext)
        || (NULL == pNdefReadOnly_RspCb)
        || (0 == hRemoteDevice))
    {
        ret_val = NFCSTATUS_INVALID_PARAMETER;
    }
    else if (gpphLibContext->LibNfcState.next_state
            == eLibNfcHalStateShutdown)
    {
        ret_val = NFCSTATUS_SHUTDOWN;
    }
    else if (0 == gpphLibContext->Connected_handle)
    {
        ret_val = NFCSTATUS_TARGET_NOT_CONNECTED;
    }
    else if (hRemoteDevice != gpphLibContext->Connected_handle)
    {
        ret_val = NFCSTATUS_INVALID_HANDLE;
    }
    else if ((TRUE == gpphLibContext->status.GenCb_pending_status)
        || (NULL != gpphLibContext->ndef_cntx.pClientNdefFmtCb)
        || (FALSE == gpphLibContext->ndef_cntx.is_ndef))
    {
        /* Previous Callback is Pending */
        ret_val = NFCSTATUS_REJECTED;
        PHDBG_INFO("LIbNfc:Previous Callback is Pending");
    }
    else
    {
        gpphLibContext->ndef_cntx.eLast_Call = NdefReadOnly;

        if(eLibNfcHalStatePresenceChk != gpphLibContext->LibNfcState.next_state)
        {
            phHal_sRemoteDevInformation_t           *ps_rem_dev_info = 
                                                (phHal_sRemoteDevInformation_t *)hRemoteDevice;
            uint8_t                                 fun_id;

            switch (ps_rem_dev_info->RemDevType)
            {
                case phHal_eMifare_PICC:
                case phHal_eISO14443_A_PICC:
                {
                    if ((phHal_eMifare_PICC == ps_rem_dev_info->RemDevType)
                        && (0x00 != ps_rem_dev_info->RemoteDevInfo.Iso14443A_Info.Sak))
                    {
                        for (fun_id = 0; fun_id < PH_FRINFC_NDEFMAP_CR; fun_id++)
                        {
                            /* Register the callback for the check ndef */
                            ret_val = phFriNfc_NdefMap_SetCompletionRoutine (
                                      gpphLibContext->ndef_cntx.psNdefMap,
                                      fun_id, phLibNfc_Ndef_ReadOnly_Cb,
                                      (void *)gpphLibContext);
                        }

                        /* Start mifare NFC read only function   */
                        /* mif_std_key is required to format the mifare 1k/4k card */
                        if(pScrtKey != NULL && pScrtKey->length == MIFARE_STD_KEY_LEN)
                        {
                            for (Index =0 ;Index < (pScrtKey->length); Index++ )
                            {
                                mif_std_key[Index] = *(pScrtKey->buffer++);
                            }

                            ret_val = phFriNfc_MifareStdMap_ConvertToReadOnly (
                                      gpphLibContext->ndef_cntx.psNdefMap, mif_std_key);
                            ret_val = PHNFCSTATUS(ret_val);
                        }
                    }
                    else
                    {
                        gpphLibContext->ndef_cntx.NdefSendRecvLen = NDEF_SENDRCV_BUF_LEN;

                        /* Call ndef format reset, this will initialize the ndef
                        format structure, and appropriate values are filled */
                        ret_val = phFriNfc_NdefSmtCrd_Reset (gpphLibContext->ndef_cntx.ndef_fmt,
                                                gpphLibContext->psOverHalCtxt,
                                                (phHal_sRemoteDevInformation_t*)hRemoteDevice,
                                                gpphLibContext->psDevInputParam,
                                                gpphLibContext->ndef_cntx.psNdefMap->SendRecvBuf,
                                                &(gpphLibContext->ndef_cntx.NdefSendRecvLen));

                        for(fun_id = 0; fun_id < PH_FRINFC_SMTCRDFMT_CR; fun_id++)
                        {
                            /* Register for all the callbacks */
                            ret_val = phFriNfc_NdefSmtCrd_SetCR (gpphLibContext->ndef_cntx.ndef_fmt,
                                                                fun_id, phLibNfc_Ndef_ReadOnly_Cb,
                                                                gpphLibContext);
                        }

                        /* Start smart card formatting function   */
                        ret_val = phFriNfc_NdefSmtCrd_ConvertToReadOnly (
                                                        gpphLibContext->ndef_cntx.ndef_fmt);
                        ret_val = PHNFCSTATUS(ret_val);
                    }
                    break;
                }

                case phHal_eJewel_PICC:
                case phHal_eISO15693_PICC:
                {
// MC: Got the feedback this was #if 0'd because it was resetting the lock bits
// read in check NDEF, and these should not be reset here already.
#if 0
                    static uint16_t     data_cnt = 0;

                    /* Resets the component instance */
                    ret_val = phFriNfc_NdefMap_Reset (gpphLibContext->ndef_cntx.psNdefMap,
                                        gpphLibContext->psOverHalCtxt,
                                        (phLibNfc_sRemoteDevInformation_t*)hRemoteDevice,
                                        gpphLibContext->psDevInputParam,
                                        gpphLibContext->ndef_cntx.psNdefMap->SendRecvBuf,
                                        gpphLibContext->ndef_cntx.NdefSendRecvLen,
                                        gpphLibContext->ndef_cntx.psNdefMap->SendRecvBuf,
                                        &(gpphLibContext->ndef_cntx.NdefSendRecvLen),
                                        &(data_cnt));
#endif /* #if 0 */


                    for (fun_id = 0; fun_id < PH_FRINFC_NDEFMAP_CR; fun_id++)
                    {
                        /* Register the callback for the check ndef */
                        ret_val = phFriNfc_NdefMap_SetCompletionRoutine (
                                            gpphLibContext->ndef_cntx.psNdefMap,
                                            fun_id, phLibNfc_Ndef_ReadOnly_Cb,
                                            (void *)gpphLibContext);
                    }

                    /* call below layer check Ndef function */
                    ret_val = phFriNfc_NdefMap_ConvertToReadOnly (
                                            gpphLibContext->ndef_cntx.psNdefMap);
                    ret_val = PHNFCSTATUS(ret_val);
                    break;
                }

                default:
                {
                    /* Tag not supported */
                    ret_val = NFCSTATUS_REJECTED;
                    break;
                }
            }            
        }
        else
        {
             gpphLibContext->ndef_cntx.pClientNdefFmtCb= NULL;
             ret_val = NFCSTATUS_PENDING;
        }

        if (NFCSTATUS_PENDING == ret_val)
        {
            gpphLibContext->ndef_cntx.pClientNdefFmtCb = pNdefReadOnly_RspCb;
            gpphLibContext->ndef_cntx.pClientNdefFmtCntx = pContext;

            gpphLibContext->status.GenCb_pending_status = TRUE;
            gpphLibContext->LibNfcState.next_state = eLibNfcHalStateTransaction;
        }
        else
        {
            ret_val = NFCSTATUS_FAILED;
        }
    }
    return ret_val;
}

#endif /* #ifdef LIBNFC_READONLY_NDEF */

/**
* Response callback for NDEF format.
*/
STATIC
void phLibNfc_Ndef_format_Cb(void *Context,NFCSTATUS  status)
{
    NFCSTATUS RetStatus = NFCSTATUS_SUCCESS;
    pphLibNfc_RspCb_t       pClientCb=NULL;
    phLibNfc_LibContext_t   *pLibNfc_Ctxt = (phLibNfc_LibContext_t *)Context;
    void                    *pUpperLayerContext=NULL;
    phHal_sRemoteDevInformation_t   *ps_rem_dev_info = NULL;
    if(pLibNfc_Ctxt != gpphLibContext)
    {   /*wrong context returned*/
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
    }
    else
    {
        if(eLibNfcHalStateShutdown == gpphLibContext->LibNfcState.next_state)
        {
            /*shutdown is pending so issue shutdown*/
            phLibNfc_Pending_Shutdown();
            RetStatus = NFCSTATUS_SHUTDOWN;    
        }
        else if(eLibNfcHalStateRelease == gpphLibContext->LibNfcState.next_state)
        {
            RetStatus = NFCSTATUS_ABORTED;          
        }
        else
        {
            gpphLibContext->status.GenCb_pending_status = FALSE;
            if(NFCSTATUS_SUCCESS == status)
            {
                RetStatus = NFCSTATUS_SUCCESS;
            }            
            else if(PHNFCSTATUS(status)==NFCSTATUS_FAILED)
            {
                RetStatus = NFCSTATUS_FAILED;
                ps_rem_dev_info = (phHal_sRemoteDevInformation_t *)
                                    gpphLibContext->Connected_handle;
                if ((phHal_eMifare_PICC == ps_rem_dev_info->RemDevType) && 
                    (0x08 == (ps_rem_dev_info->RemoteDevInfo.Iso14443A_Info.Sak & 0x08)))
                {

                    /* card type is mifare 1k/4k, then reconnect */
                    RetStatus = phHal4Nfc_Connect(gpphLibContext->psHwReference,  
                                (phHal_sRemoteDevInformation_t *)
                                gpphLibContext->Connected_handle,
                                (pphHal4Nfc_ConnectCallback_t)
                                phLibNfc_Reconnect_Mifare_Cb,
                                (void *)gpphLibContext);
                }
            }
			else
            {
                /*Target was removed during transaction*/
                RetStatus = NFCSTATUS_FAILED;
            }
            gpphLibContext->LibNfcState.cur_state =eLibNfcHalStateConnect;
        }
        phLibNfc_UpdateCurState(status,gpphLibContext);
        
        pClientCb = gpphLibContext->ndef_cntx.pClientNdefFmtCb;
        pUpperLayerContext= gpphLibContext->ndef_cntx.pClientNdefFmtCntx;
        gpphLibContext->ndef_cntx.pClientNdefFmtCb = NULL;
        gpphLibContext->ndef_cntx.pClientNdefFmtCntx = NULL;
        if(NFCSTATUS_PENDING != RetStatus)
        {
            if (NULL != pClientCb)
            {
                /* Call the tag format upper layer callback */
                pClientCb(pUpperLayerContext,RetStatus);
            }
        }
    }
    return;
}

#ifdef LIBNFC_READONLY_NDEF
STATIC
void
phLibNfc_Ndef_ReadOnly_Cb (
    void        *p_context,
    NFCSTATUS   status)
{
    NFCSTATUS                       ret_status = NFCSTATUS_SUCCESS;
    pphLibNfc_RspCb_t               p_client_cb = NULL;
    phLibNfc_LibContext_t           *pLibNfc_Ctxt = (phLibNfc_LibContext_t *)p_context;
    void                            *p_upper_layer_ctxt = NULL;

    if(pLibNfc_Ctxt != gpphLibContext)
    {
        /*wrong context returned*/
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
    }
    else
    {
        if(eLibNfcHalStateShutdown == gpphLibContext->LibNfcState.next_state)
        {
            /*shutdown is pending so issue shutdown*/
            phLibNfc_Pending_Shutdown();
            ret_status = NFCSTATUS_SHUTDOWN;
        }
        else if(eLibNfcHalStateRelease == gpphLibContext->LibNfcState.next_state)
        {
            ret_status = NFCSTATUS_ABORTED;
        }
        else
        {
            gpphLibContext->status.GenCb_pending_status = FALSE;
            if(NFCSTATUS_SUCCESS == status)
            {
                gpphLibContext->ndef_cntx.psNdefMap->CardState = 
                                                PH_NDEFMAP_CARD_STATE_READ_ONLY;
                ret_status = NFCSTATUS_SUCCESS;
            }
            else
            {
                ret_status = NFCSTATUS_FAILED;
            }
            gpphLibContext->LibNfcState.cur_state =eLibNfcHalStateConnect;
        }

        phLibNfc_UpdateCurState(status, gpphLibContext);

        p_client_cb = gpphLibContext->ndef_cntx.pClientNdefFmtCb;
        p_upper_layer_ctxt = gpphLibContext->ndef_cntx.pClientNdefFmtCntx;
        gpphLibContext->ndef_cntx.pClientNdefFmtCb = NULL;
        gpphLibContext->ndef_cntx.pClientNdefFmtCntx = NULL;
        if(NFCSTATUS_PENDING != ret_status)
        {
            if (NULL != p_client_cb)
            {
                /* Call the tag format upper layer callback */
                p_client_cb (p_upper_layer_ctxt, ret_status);
            }
        }
    }
}
#endif /* #ifdef LIBNFC_READONLY_NDEF */

STATIC
void phLibNfc_Ndef_SrchNdefCnt_Cb(void *context, NFCSTATUS status)
{
    static NFCSTATUS RegPrSt=FALSE;
    uint8_t RegStatus=0;
    NFCSTATUS RetVal = NFCSTATUS_SUCCESS ;  
    uint32_t Index=0;   
    
    
	  PHNFC_UNUSED_VARIABLE(context);
    if(eLibNfcHalStateShutdown == gpphLibContext->LibNfcState.next_state)
    {   /*shutdown called before completion of Ndef read allow
              shutdown to happen */
        phLibNfc_Pending_Shutdown();
        RetVal = NFCSTATUS_SHUTDOWN;    
    }
    else if(eLibNfcHalStateRelease == gpphLibContext->LibNfcState.next_state)
    {
        RetVal = NFCSTATUS_ABORTED;
    }
    else if(NFCSTATUS_SUCCESS != status)
    {
        RetVal = status;
    }
    else
    {
     /* This conditional branch is for QMORE fix */
    }
    gpphLibContext->status.GenCb_pending_status = FALSE;
    
    phLibNfc_UpdateCurState(status,gpphLibContext);
    /* Read is not success send failed to upperlayer Call back*/
    if( RetVal!= NFCSTATUS_SUCCESS ) 
    {
        if((RetVal!=NFCSTATUS_SHUTDOWN)&& (RetVal!=NFCSTATUS_ABORTED))
        {
            RetVal= NFCSTATUS_FAILED;
        }
        gpphLibContext->CBInfo.pClientNdefNtfRespCb(
                            gpphLibContext->CBInfo.pClientNdefNtfRespCntx,
                            NULL,
                            gpphLibContext->Connected_handle,
                            RetVal);
        gpphLibContext->CBInfo.pClientNdefNtfRespCb = NULL;     
        gpphLibContext->CBInfo.pClientNdefNtfRespCntx = NULL;
        return; 
    }

    /*Get the Number of records ( If Raw record parameter is null then API gives number of Records*/
    RetVal = phFriNfc_NdefRecord_GetRecords(
                            gpphLibContext->phLib_NdefRecCntx.ndef_message.buffer,
                            gpphLibContext->phLib_NdefRecCntx.ndef_message.length,
                            NULL,
                            gpphLibContext->phLib_NdefRecCntx.IsChunked,
                            &(gpphLibContext->phLib_NdefRecCntx.NumberOfRawRecords));

    NdefInfo.pNdefMessage = gpphLibContext->phLib_NdefRecCntx.ndef_message.buffer;
    NdefInfo.NdefMessageLengthActual = gpphLibContext->ndef_cntx.NdefActualSize;
    NdefInfo.NdefMessageLengthMaximum = gpphLibContext->ndef_cntx.NdefLength;
    NdefInfo.NdefRecordCount =0;

    /*Allocate memory to hold the records Read*/
    NdefInfo.pNdefRecord = phOsalNfc_GetMemory
        (sizeof(phFriNfc_NdefRecord_t)* gpphLibContext->phLib_NdefRecCntx.NumberOfRawRecords );  
    if(NULL==NdefInfo.pNdefRecord)
    {
        gpphLibContext->CBInfo.pClientNdefNtfRespCb(
                            gpphLibContext->CBInfo.pClientNdefNtfRespCntx,
                            NULL,
                            gpphLibContext->Connected_handle,
                            NFCSTATUS_FAILED);
        gpphLibContext->CBInfo.pClientNdefNtfRespCb = NULL;     
        gpphLibContext->CBInfo.pClientNdefNtfRespCntx = NULL;
        return;     
    }

    pNdefRecord=NdefInfo.pNdefRecord;   
    /*If phLibNfc_Ndef_SearchNdefContent Reg type is NULL return all the Records*/
    if(gpphLibContext->ndef_cntx.pNdef_NtfSrch_Type==NULL)
    {
        RetVal = phFriNfc_NdefRecord_GetRecords(
                        gpphLibContext->phLib_NdefRecCntx.ndef_message.buffer,
                        gpphLibContext->phLib_NdefRecCntx.ndef_message.length,
                        gpphLibContext->phLib_NdefRecCntx.RawRecords,
                        gpphLibContext->phLib_NdefRecCntx.IsChunked,
                        &(gpphLibContext->phLib_NdefRecCntx.NumberOfRawRecords));

        for (Index = 0; Index < gpphLibContext->phLib_NdefRecCntx.NumberOfRawRecords; Index++)
        {
            RetVal = phFriNfc_NdefRecord_Parse( 
                        pNdefRecord,
                        gpphLibContext->phLib_NdefRecCntx.RawRecords[Index]);
            pNdefRecord++;
            NdefInfo.NdefRecordCount++;
        }
    }
    else
    {
        
        /* Look for registerd TNF */
        RetVal = phFriNfc_NdefReg_DispatchPacket( 
                    &(gpphLibContext->phLib_NdefRecCntx.NdefReg),
                    gpphLibContext->phLib_NdefRecCntx.ndef_message.buffer,
                    (uint16_t)gpphLibContext->phLib_NdefRecCntx.ndef_message.length);
        if(NFCSTATUS_SUCCESS != RetVal)
        {
            /*phFriNfc_NdefReg_DispatchPacket is failed call upper layer*/
            gpphLibContext->CBInfo.pClientNdefNtfRespCb(gpphLibContext->CBInfo.pClientNdefNtfRespCntx,
                                                    NULL,gpphLibContext->Connected_handle,NFCSTATUS_FAILED);
            gpphLibContext->CBInfo.pClientNdefNtfRespCb = NULL;     
            gpphLibContext->CBInfo.pClientNdefNtfRespCntx = NULL;
            return; 
        }

        while(1 != RegStatus)
        {
            /* Process the NDEF records, If match FOUND we will get Call back*/
            RegStatus = phFriNfc_NdefReg_Process(   &(gpphLibContext->phLib_NdefRecCntx.NdefReg),
                                                &RegPrSt);
            if(RegPrSt == TRUE)
            {
                /*  Processing Done */
                break;
            }
            /*If match found the CbParam will be updated by lower layer, copy the record info*/
            for(Index=0;Index<gpphLibContext->phLib_NdefRecCntx.CbParam.Count;Index++)
            {
                pNdefRecord->Tnf  = gpphLibContext->phLib_NdefRecCntx.CbParam.Records[Index].Tnf;
                pNdefRecord->TypeLength  = 
                    gpphLibContext->phLib_NdefRecCntx.CbParam.Records[Index].TypeLength;
                pNdefRecord->PayloadLength  = 
                    gpphLibContext->phLib_NdefRecCntx.CbParam.Records[Index].PayloadLength;
                pNdefRecord->IdLength  = 
                    gpphLibContext->phLib_NdefRecCntx.CbParam.Records[Index].IdLength;
                pNdefRecord->Flags = 
                    gpphLibContext->phLib_NdefRecCntx.CbParam.Records[Index].Flags;

                pNdefRecord->Id = phOsalNfc_GetMemory(pNdefRecord->IdLength);
                pNdefRecord->Type = phOsalNfc_GetMemory(pNdefRecord->TypeLength);
                pNdefRecord->PayloadData = phOsalNfc_GetMemory(pNdefRecord->PayloadLength);         
                
                (void)memcpy(pNdefRecord->Id,
                    gpphLibContext->phLib_NdefRecCntx.CbParam.Records[Index].Id,
                    pNdefRecord->IdLength);
                (void)memcpy(pNdefRecord->PayloadData,
                    gpphLibContext->phLib_NdefRecCntx.CbParam.Records[Index].PayloadData,
                    pNdefRecord->PayloadLength);
                (void)memcpy(pNdefRecord->Type,
                    gpphLibContext->phLib_NdefRecCntx.CbParam.Records[Index].Type,
                    pNdefRecord->TypeLength);

                pNdefRecord++;
                NdefInfo.NdefRecordCount++;
            }
        }
    }
    /* If no record found call upper layer with failed status*/
    if(pNdefRecord == NdefInfo.pNdefRecord)
    {
        NdefInfo.NdefRecordCount =0;
        gpphLibContext->CBInfo.pClientNdefNtfRespCb(
                    gpphLibContext->CBInfo.pClientNdefNtfRespCntx,
                    &NdefInfo,gpphLibContext->Connected_handle,
                    NFCSTATUS_SUCCESS);
    
    }
    else
    {
        /*Call upperlayer Call back with match records*/

        gpphLibContext->CBInfo.pClientNdefNtfRespCb(
                    gpphLibContext->CBInfo.pClientNdefNtfRespCntx,
                    &NdefInfo,gpphLibContext->Connected_handle,
                    NFCSTATUS_SUCCESS);
        /*Remove entry from FRI*/
        RetVal = phFriNfc_NdefReg_RmCb( 
                    &(gpphLibContext->phLib_NdefRecCntx.NdefReg),
                    gpphLibContext->phLib_NdefRecCntx.NdefCb );
        /*Free the memory*/
        if(gpphLibContext->ndef_cntx.pNdef_NtfSrch_Type!=NULL)
        {
            pNdefRecord=NdefInfo.pNdefRecord;
            for(Index=0;Index<gpphLibContext->phLib_NdefRecCntx.CbParam.Count;Index++)
            {
                phOsalNfc_FreeMemory(pNdefRecord->Id);
                phOsalNfc_FreeMemory(pNdefRecord->PayloadData);
                phOsalNfc_FreeMemory(pNdefRecord->Type);
                pNdefRecord++;
            }
        }
    }

    gpphLibContext->CBInfo.pClientNdefNtfRespCb = NULL;     
    gpphLibContext->CBInfo.pClientNdefNtfRespCntx = NULL;
    
}

STATIC
void phLibNfc_Ndef_Rtd_Cb( void *CallBackParam)
{
    /*There will be single call back given to all match
      It's processed in phLibNfc_Ndef_SrchNdefCnt_Cb*/  
    PHNFC_UNUSED_VARIABLE(CallBackParam);
}

NFCSTATUS phLibNfc_Ndef_SearchNdefContent(  
                                phLibNfc_Handle                 hRemoteDevice,
                                phLibNfc_Ndef_SrchType_t*       psSrchTypeList,  
                                uint8_t                         uNoSrchRecords,
                                pphLibNfc_Ndef_Search_RspCb_t   pNdefNtfRspCb,  
                                void *                          pContext   
                                )
{

     NFCSTATUS  RetVal =NFCSTATUS_SUCCESS;
     uint32_t Index=0;
     uint8_t     cr_index = 0;


      if((NULL == gpphLibContext) ||
        (gpphLibContext->LibNfcState.cur_state
                            == eLibNfcHalStateShutdown))
      {
         RetVal = NFCSTATUS_NOT_INITIALISED;
      }
     /* Check the state for DeInit is called or not,if yes return NFCSTATUS_SHUTDOWN*/
      else if(gpphLibContext->LibNfcState.next_state
                            == eLibNfcHalStateShutdown)
     {       
        RetVal= NFCSTATUS_SHUTDOWN;
     }
     else if( (NULL == pNdefNtfRspCb) ||
        (NULL == pContext ) ||
        (0 == hRemoteDevice))
     {
        RetVal= NFCSTATUS_INVALID_PARAMETER;        
     } 
     else if( (NULL != psSrchTypeList) && (0==uNoSrchRecords))
     {
        RetVal= NFCSTATUS_INVALID_PARAMETER;        
     }
     else if(0 == gpphLibContext->Connected_handle)
     {   /*presently no target or tag is connected*/ 
        RetVal=NFCSTATUS_TARGET_NOT_CONNECTED;        
     }
     else if(hRemoteDevice != gpphLibContext->Connected_handle)
     {   /*This handle of the device sent by application is not connected */ 
        RetVal=NFCSTATUS_INVALID_HANDLE;        
     }  
     else if((TRUE == gpphLibContext->status.GenCb_pending_status)       
        ||(NULL!=gpphLibContext->CBInfo.pClientNdefNtfRespCb))
     {
        /*Previous callback is pending*/
        RetVal = NFCSTATUS_REJECTED;
     }
     else
     {
        gpphLibContext->ndef_cntx.pNdef_NtfSrch_Type = psSrchTypeList;

        if(psSrchTypeList!=NULL)
        {
            /*Maximum records supported*/
            gpphLibContext->phLib_NdefRecCntx.NumberOfRecords = 255;
            /*Reset the FRI component to add the Reg type*/
            RetVal = phFriNfc_NdefReg_Reset( 
                            &(gpphLibContext->phLib_NdefRecCntx.NdefReg),
                            gpphLibContext->phLib_NdefRecCntx.NdefTypes_array,
                            &(gpphLibContext->phLib_NdefRecCntx.RecordsExtracted),
                            &(gpphLibContext->phLib_NdefRecCntx.CbParam),
                            gpphLibContext->phLib_NdefRecCntx.ChunkedRecordsarray,  
                            gpphLibContext->phLib_NdefRecCntx.NumberOfRecords);

            gpphLibContext->phLib_NdefRecCntx.NdefCb = phOsalNfc_GetMemory(sizeof(phFriNfc_NdefReg_Cb_t));
            if(gpphLibContext->phLib_NdefRecCntx.NdefCb==NULL)
            {
                /*exception: Not enough memory*/
                phOsalNfc_RaiseException(phOsalNfc_e_NoMemory,1);
            }
            gpphLibContext->phLib_NdefRecCntx.NdefCb->NdefCallback = phLibNfc_Ndef_Rtd_Cb;
            /*Copy the TNF types to search in global structure*/    
            gpphLibContext->phLib_NdefRecCntx.NdefCb->NumberOfRTDs = uNoSrchRecords;
            for(Index=0;Index<uNoSrchRecords;Index++)
            {
                gpphLibContext->phLib_NdefRecCntx.NdefCb->NdefType[Index] = psSrchTypeList->Type;
                gpphLibContext->phLib_NdefRecCntx.NdefCb->Tnf[Index] = psSrchTypeList->Tnf ; 
                gpphLibContext->phLib_NdefRecCntx.NdefCb->NdeftypeLength[Index] = psSrchTypeList->TypeLength;
                psSrchTypeList++;
            }
            /* Add the TNF type to FRI component*/

            RetVal = phFriNfc_NdefReg_AddCb(&(gpphLibContext->phLib_NdefRecCntx.NdefReg),
                                                gpphLibContext->phLib_NdefRecCntx.NdefCb );

        }
        gpphLibContext->phLib_NdefRecCntx.ndef_message.buffer = 
            phOsalNfc_GetMemory(gpphLibContext->ndef_cntx.NdefActualSize);
        gpphLibContext->phLib_NdefRecCntx.ndef_message.length = 
            gpphLibContext->ndef_cntx.NdefActualSize;
        /*Set Complete routine for NDEF Read*/
        for (cr_index = 0; cr_index < PH_FRINFC_NDEFMAP_CR; cr_index++)
        {
            RetVal= phFriNfc_NdefMap_SetCompletionRoutine(
                                gpphLibContext->ndef_cntx.psNdefMap,
                                cr_index,
                                phLibNfc_Ndef_SrchNdefCnt_Cb,
                                (void *)gpphLibContext);

        }
        gpphLibContext->ndef_cntx.NdefContinueRead = PH_FRINFC_NDEFMAP_SEEK_BEGIN;
        /* call below layer Ndef Read*/
        RetVal = phFriNfc_NdefMap_RdNdef(gpphLibContext->ndef_cntx.psNdefMap,
                        gpphLibContext->phLib_NdefRecCntx.ndef_message.buffer,
                        (uint32_t*)&gpphLibContext->phLib_NdefRecCntx.ndef_message.length,
                        PH_FRINFC_NDEFMAP_SEEK_BEGIN);

        if(NFCSTATUS_PENDING == RetVal)
        {
            gpphLibContext->CBInfo.pClientNdefNtfRespCb = pNdefNtfRspCb;        
            gpphLibContext->CBInfo.pClientNdefNtfRespCntx = pContext;
            gpphLibContext->status.GenCb_pending_status=TRUE;
            gpphLibContext->LibNfcState.next_state = eLibNfcHalStateTransaction;
        }
        else if (NFCSTATUS_SUCCESS == RetVal)
        {
            RetVal= NFCSTATUS_SUCCESS;
        }
        else
        {
            /*Ndef read failed*/
            RetVal = NFCSTATUS_FAILED;
        }
    }
    return RetVal;

}

