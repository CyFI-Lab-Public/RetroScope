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
 * \file phLibNfc_initiator.c

 * Project: NFC FRI 1.1
 *
 * $Date: Fri Apr 23 14:34:08 2010 $
 * $Author: ing07385 $
 * $Revision: 1.53 $
 * $Aliases: NFC_FRI1.1_WK1014_SDK,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1019_SDK,NFC_FRI1.1_WK1024_SDK $
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

#define PN544_IO_TIMEOUT_RESPONSE 0x89

/*
*************************** Static Function Declaration ***********************
*/

/* Target discvovery notification callback */
STATIC void phLibNfc_NotificationRegister_Resp_Cb ( 
                                void                             *context,
                                phHal_eNotificationType_t        type,
                                phHal4Nfc_NotificationInfo_t     info,
                                NFCSTATUS                        status
                                );

/*Remote device connect response callback*/
STATIC void phLibNfc_RemoteDev_Connect_Cb(
                           void        *pContext,                           
                           phHal_sRemoteDevInformation_t *pRmtdev_info,
                           NFCSTATUS    status
                           );

#ifdef RECONNECT_SUPPORT
STATIC 
void 
phLibNfc_config_discovery_con_failure_cb (
    void                *context,
    NFCSTATUS           status);
#endif /* #ifdef RECONNECT_SUPPORT */

/*Remote device disconnect response callback*/
STATIC void phLibNfc_RemoteDev_Disconnect_cb(                        
                                void                          *context,
                                phHal_sRemoteDevInformation_t *reg_handle,
                                NFCSTATUS                      status
                                );
/*Remote device Transceive response callback*/
STATIC void phLibNfc_RemoteDev_Transceive_Cb(void *context,
                                phHal_sRemoteDevInformation_t *pRmtdev_info,
                                phNfc_sData_t *response,
                                NFCSTATUS status
                                );
/*Set P2P config paramater response callback*/
STATIC void phLibNfc_Mgt_SetP2P_ConfigParams_Cb(
                                void                             *context,
                                NFCSTATUS                        status
                                );


/*
*************************** Function Definitions ******************************
*/

/**
* Response to target discovery.
*/
STATIC
void phLibNfc_NotificationRegister_Resp_Cb ( 
                                void                            *context,
                                phHal_eNotificationType_t       type,
                                phHal4Nfc_NotificationInfo_t    info,
                                NFCSTATUS                       status
                                )
{
    NFCSTATUS RetVal = NFCSTATUS_SUCCESS,
              Status = NFCSTATUS_SUCCESS;
    uint16_t DeviceIndx, DeviceIndx1;
    uint8_t sak_byte=0;
    uint8_t tag_disc_flg = 0;
    phLibNfc_NtfRegister_RspCb_t pClientCb=NULL;
    pClientCb =gpphLibContext->CBInfo.pClientNtfRegRespCB;
	PHNFC_UNUSED_VARIABLE(context);
    

    if(( type != NFC_DISCOVERY_NOTIFICATION )
        &&(PHNFCSTATUS(status)!=NFCSTATUS_DESELECTED))
    {
        Status = NFCSTATUS_FAILED;
    }
    else if (PHNFCSTATUS(status) == NFCSTATUS_DESELECTED)
    {
        return;
    }
	else
	{
		DeviceIndx=0;DeviceIndx1=0;
		while(DeviceIndx < info.psDiscoveryInfo->NumberOfDevices)
		{
			switch(info.psDiscoveryInfo->ppRemoteDevInfo[DeviceIndx]->RemDevType)
			{
				case  phHal_eMifare_PICC:
				{
					/*Mifare Tag discovered*/
					sak_byte =  info.psDiscoveryInfo->
								ppRemoteDevInfo[DeviceIndx]->RemoteDevInfo.Iso14443A_Info.Sak;
					if((TRUE == gpphLibContext->RegNtfType.MifareUL)&& (sak_byte==0x00))
					{
						/*Copy the tag related info*/
						gpphLibContext->psRemoteDevList[DeviceIndx1].psRemoteDevInfo=
							info.psDiscoveryInfo->ppRemoteDevInfo[DeviceIndx];
						gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev =
							(uint32_t)gpphLibContext->psRemoteDevList[DeviceIndx].psRemoteDevInfo;
						gpphLibContext->Discov_handle[DeviceIndx1] = 
							gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev;
						DeviceIndx1++;
						tag_disc_flg++;
					}
                
					if((TRUE == gpphLibContext->RegNtfType.MifareStd)&& 
						(((sak_byte & 0x18)==0x08)||((sak_byte & 0x18)==0x18) ||
                                                (sak_byte == 0x01)))
					{
						/*Copy the tag related info*/
						gpphLibContext->psRemoteDevList[DeviceIndx1].psRemoteDevInfo=
							info.psDiscoveryInfo->ppRemoteDevInfo[DeviceIndx];
						gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev =
							(uint32_t)gpphLibContext->psRemoteDevList[DeviceIndx].psRemoteDevInfo;
						gpphLibContext->Discov_handle[DeviceIndx1]= 
							gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev;
						DeviceIndx1++;
						tag_disc_flg++;
					}

				}break;
				case  phHal_eISO14443_A_PICC:
				{
					/*ISO 14443-A type tag discovered*/
					if(TRUE == gpphLibContext->RegNtfType.ISO14443_4A)
					{
						/*Copy the ISO type A tag info*/
						gpphLibContext->psRemoteDevList[DeviceIndx1].psRemoteDevInfo=
								info.psDiscoveryInfo->ppRemoteDevInfo[DeviceIndx];
						gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev =
								(uint32_t)gpphLibContext->psRemoteDevList[DeviceIndx].psRemoteDevInfo;
						gpphLibContext->Discov_handle[DeviceIndx1] = 
							gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev;
						DeviceIndx1++;
						tag_disc_flg++;
					}
				}break;
				case  phHal_eISO14443_3A_PICC:
				{
					/*ISO 14443-A type tag discovered*/
					if(TRUE == gpphLibContext->RegNtfType.MifareUL)
					{
						/*Copy the ISO type A tag info*/
						gpphLibContext->psRemoteDevList[DeviceIndx1].psRemoteDevInfo=
								info.psDiscoveryInfo->ppRemoteDevInfo[DeviceIndx];
						gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev =
								(uint32_t)gpphLibContext->psRemoteDevList[DeviceIndx].psRemoteDevInfo;
						gpphLibContext->Discov_handle[DeviceIndx1] = 
							gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev;
						DeviceIndx1++;
						tag_disc_flg++;
					}
				}break;
				case  phHal_eISO14443_B_PICC:
				{
					/*ISO 14443-B type tag Discovered */
					if(TRUE == gpphLibContext->RegNtfType.ISO14443_4B)
					{
						/*Copy the Type B tag info */
						gpphLibContext->psRemoteDevList[DeviceIndx1].psRemoteDevInfo=
								info.psDiscoveryInfo->ppRemoteDevInfo[DeviceIndx];
						gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev =
								(uint32_t)gpphLibContext->psRemoteDevList[DeviceIndx].psRemoteDevInfo;
						gpphLibContext->Discov_handle[DeviceIndx1] = 
							gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev;
						DeviceIndx1++;
						tag_disc_flg++;
					}
				}break;
				case  phHal_eFelica_PICC:
				{
					/*Felica Type Tag Discovered */
					if(TRUE == gpphLibContext->RegNtfType.Felica)
					{
						/*Copy the Felica tag info */
						gpphLibContext->psRemoteDevList[DeviceIndx1].psRemoteDevInfo=
								info.psDiscoveryInfo->ppRemoteDevInfo[DeviceIndx];
						gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev =
								(uint32_t)gpphLibContext->psRemoteDevList[DeviceIndx].psRemoteDevInfo;
						gpphLibContext->Discov_handle[DeviceIndx1] = 
							gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev;
						DeviceIndx1++;
						tag_disc_flg++;
					}
				}break;
				case  phHal_eJewel_PICC:
				{
					/*Jewel Type Tag Discovered */
					if(TRUE == gpphLibContext->RegNtfType.Jewel)
					{
						/*Copy the Felica tag info */
						gpphLibContext->psRemoteDevList[DeviceIndx1].psRemoteDevInfo=
								info.psDiscoveryInfo->ppRemoteDevInfo[DeviceIndx];
						gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev =
								(uint32_t)gpphLibContext->psRemoteDevList[DeviceIndx].psRemoteDevInfo;
						gpphLibContext->Discov_handle[DeviceIndx1] = 
							gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev;
						DeviceIndx1++;
						tag_disc_flg++;
					}
				}
				break;
				case  phHal_eISO15693_PICC:
				{
					/*Jewel Type Tag Discovered */
					if(TRUE == gpphLibContext->RegNtfType.ISO15693)
					{
						/*Copy the Felica tag info */
						gpphLibContext->psRemoteDevList[DeviceIndx1].psRemoteDevInfo=
								info.psDiscoveryInfo->ppRemoteDevInfo[DeviceIndx];
						gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev =
								(uint32_t)gpphLibContext->psRemoteDevList[DeviceIndx].psRemoteDevInfo;
						gpphLibContext->Discov_handle[DeviceIndx1] = 
							gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev;
						DeviceIndx1++;
						tag_disc_flg++;
					}
				}
				break;
				case  phHal_eNfcIP1_Target:
				{
					if(TRUE == gpphLibContext->RegNtfType.NFC)
					{
						gpphLibContext->psRemoteDevList[DeviceIndx1].psRemoteDevInfo=
								info.psDiscoveryInfo->ppRemoteDevInfo[DeviceIndx];
						gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev =
								(uint32_t)gpphLibContext->psRemoteDevList[DeviceIndx].psRemoteDevInfo;
						gpphLibContext->Discov_handle[DeviceIndx1] = 
							gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev;
						DeviceIndx1++;
						tag_disc_flg++;
					}
				}
				break;
				case  phHal_eNfcIP1_Initiator:
				{
					if(TRUE == gpphLibContext->RegNtfType.NFC)
					{
						gpphLibContext->LibNfcState.cur_state=eLibNfcHalStateConnect;
						gpphLibContext->psRemoteDevList[DeviceIndx1].psRemoteDevInfo=
								info.psDiscoveryInfo->ppRemoteDevInfo[DeviceIndx];
						gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev =
								(uint32_t)gpphLibContext->psRemoteDevList[DeviceIndx1].psRemoteDevInfo;
						gpphLibContext->sNfcIp_Context.Rem_Initiator_Handle=
								gpphLibContext->psRemoteDevList[DeviceIndx1].hTargetDev;
						DeviceIndx1++;
						tag_disc_flg++;
					}
				}
				break;
				default :
				{
					break;
				}
			}
			DeviceIndx++;
		}
	}

    if((tag_disc_flg >0 )&&(status != NFCSTATUS_FAILED))
    {
        gpphLibContext->dev_cnt = tag_disc_flg;
        /* Check for if the discovered tags are multiple or
         Multiple protocol tag */
        if((gpphLibContext->dev_cnt > 1)&&(
            (status ==NFCSTATUS_MULTIPLE_PROTOCOLS) ||
            (status ==NFCSTATUS_MULTIPLE_TAGS)) )
        {
            status = status;
        }
        else
        {
            status =NFCSTATUS_SUCCESS;
        }
        /*Notify to upper layer the no of tag discovered and
          the protocol */
        if (NULL != pClientCb)
        {
            pClientCb(
					(void*)gpphLibContext->CBInfo.pClientNtfRegRespCntx,
                    gpphLibContext->psRemoteDevList,
                    gpphLibContext->dev_cnt,
					status
                    );
        }

    }
    else if(PHNFCSTATUS(status)==NFCSTATUS_DESELECTED)
    {
        info.psDiscoveryInfo->NumberOfDevices = 0;
        if (NULL != pClientCb)
        {
            gpphLibContext->LibNfcState.cur_state=eLibNfcHalStateRelease;
            pClientCb((void*)gpphLibContext->CBInfo.pClientNtfRegRespCntx,
                    NULL,
                    0,
                    status);
        }

    }
    else /*Reconfigure the discovery wheel*/
    {
        RetVal = phHal4Nfc_ConfigureDiscovery ( gpphLibContext->psHwReference,
                                            NFC_DISCOVERY_RESUME,
                                            &(gpphLibContext->sADDconfig),
                                            phLibNfc_config_discovery_cb,
                                            gpphLibContext);

        if((RetVal!=NFCSTATUS_SUCCESS) &&(RetVal!=NFCSTATUS_PENDING))
        {
            Status = NFCSTATUS_FAILED;
        }

    }
    if(Status == NFCSTATUS_FAILED)
    {
        if (NULL != pClientCb)
        {
            pClientCb(gpphLibContext->CBInfo.pClientNtfRegRespCntx,
                NULL,
                0,
                Status);
        }
    }
    return;
}

/**
* This interface registers notification handler for target discovery.
*/
NFCSTATUS   
phLibNfc_RemoteDev_NtfRegister( 
                        phLibNfc_Registry_Info_t*       pRegistryInfo,
                        phLibNfc_NtfRegister_RspCb_t    pNotificationHandler,
                        void                            *pContext
                        )
{
    NFCSTATUS RetVal = NFCSTATUS_SUCCESS;
    

    /*Check for valid parameters*/
    if((NULL == pNotificationHandler)
        || (NULL == pContext) 
        ||(NULL== pRegistryInfo))
    {
        RetVal= NFCSTATUS_INVALID_PARAMETER;
    }
    else if((NULL == gpphLibContext) ||
        (gpphLibContext->LibNfcState.cur_state
                            == eLibNfcHalStateShutdown))
    {
        RetVal = NFCSTATUS_NOT_INITIALISED;
    }
    else if(gpphLibContext->LibNfcState.next_state
                            == eLibNfcHalStateShutdown)
    {
        /*Next state is shutdown*/
        RetVal= NFCSTATUS_SHUTDOWN;
    }
    else
    {
        
        PHDBG_INFO("LibNfc:Registering Notification Handler");
                
      
        (void) memcpy(&(gpphLibContext->RegNtfType),pRegistryInfo,
                        sizeof(phLibNfc_Registry_Info_t));
        /* Register Discovery Notification Handler*/     
       
		/*Register for NFCIP1 target type*/        
		RetVal = phHal4Nfc_RegisterNotification(
                                gpphLibContext->psHwReference,
                                eRegisterP2PDiscovery,
                                phLibNfc_NotificationRegister_Resp_Cb,
                                (void*)gpphLibContext
                                );
        /*Register for Tag discovery*/        
		RetVal = phHal4Nfc_RegisterNotification(
                            gpphLibContext->psHwReference,
                            eRegisterTagDiscovery,
                            phLibNfc_NotificationRegister_Resp_Cb,
                            (void*)gpphLibContext
                            );   
        gpphLibContext->CBInfo.pClientNtfRegRespCB = pNotificationHandler;
        gpphLibContext->CBInfo.pClientNtfRegRespCntx = pContext;
        /*Register notification handler with below layer*/
        
    }
    return RetVal;
}
/**
* This interface unregisters notification handler for target discovery.
*/
NFCSTATUS phLibNfc_RemoteDev_NtfUnregister(void)
{
    NFCSTATUS RetVal = NFCSTATUS_SUCCESS;
    if((NULL == gpphLibContext) ||
       (gpphLibContext->LibNfcState.cur_state
                            == eLibNfcHalStateShutdown))
    {
        /*Lib Nfc not Initialized*/
        RetVal = NFCSTATUS_NOT_INITIALISED;
    }
    else if(gpphLibContext->LibNfcState.next_state
                            == eLibNfcHalStateShutdown)
    {
        /*Lib Nfc Shutdown*/
        RetVal= NFCSTATUS_SHUTDOWN;
    }
    else
    {
        /*Unregister notification handler with lower layer */
        RetVal = phHal4Nfc_UnregisterNotification(
                                    gpphLibContext->psHwReference,
                                    eRegisterP2PDiscovery,
                                    gpphLibContext);

		RetVal = phHal4Nfc_UnregisterNotification(
                                    gpphLibContext->psHwReference,
                                    eRegisterTagDiscovery,
                                    gpphLibContext);

        gpphLibContext->CBInfo.pClientNtfRegRespCB = NULL;
        gpphLibContext->CBInfo.pClientNtfRegRespCntx =NULL;
        PHDBG_INFO("LibNfc:Unregister Notification Handler");
    }
    return RetVal;
}

#ifdef RECONNECT_SUPPORT

NFCSTATUS 
phLibNfc_RemoteDev_ReConnect (
    phLibNfc_Handle                 hRemoteDevice,
    pphLibNfc_ConnectCallback_t     pNotifyReConnect_RspCb,
    void                            *pContext)
{

    NFCSTATUS                           ret_val = NFCSTATUS_FAILED;
    phLibNfc_sRemoteDevInformation_t    *psRemoteDevInfo = NULL;

    if ((NULL == gpphLibContext) 
      || (eLibNfcHalStateShutdown == 
        gpphLibContext->LibNfcState.cur_state))
    {
         ret_val = NFCSTATUS_NOT_INITIALISED;        
    }
    else if ((NULL == pContext)
        || (NULL == pNotifyReConnect_RspCb)
        || (NULL == (void *)hRemoteDevice))
    {
        /* Check valid parameters */
        ret_val = NFCSTATUS_INVALID_PARAMETER;
    }   
    /* Check valid lib nfc State */
    else if (gpphLibContext->LibNfcState.next_state
             == eLibNfcHalStateShutdown)
    {
        ret_val = NFCSTATUS_SHUTDOWN;
    }
    else if (0 == gpphLibContext->Connected_handle)
    {
        ret_val = NFCSTATUS_TARGET_NOT_CONNECTED;
    }
    else if ((gpphLibContext->Discov_handle[0] != hRemoteDevice)
		&& (gpphLibContext->Discov_handle[1] != hRemoteDevice)
		&& (gpphLibContext->Discov_handle[2] != hRemoteDevice)
		&& (gpphLibContext->Discov_handle[3] != hRemoteDevice)
		&& (gpphLibContext->Discov_handle[4] != hRemoteDevice)
		&& (gpphLibContext->Discov_handle[5] != hRemoteDevice)
		&& (gpphLibContext->Discov_handle[6] != hRemoteDevice)
		&& (gpphLibContext->Discov_handle[7] != hRemoteDevice)
		&& (gpphLibContext->Discov_handle[8] != hRemoteDevice)
		&& (gpphLibContext->Discov_handle[9] != hRemoteDevice))
    {
        ret_val = NFCSTATUS_INVALID_HANDLE;
    }
    else
    {
        psRemoteDevInfo = (phLibNfc_sRemoteDevInformation_t *)hRemoteDevice;
       
        /* Call the HAL connect*/
        ret_val = phHal4Nfc_Connect (gpphLibContext->psHwReference,
                               psRemoteDevInfo,
                               phLibNfc_RemoteDev_Connect_Cb,
                               (void *)gpphLibContext);

        if (NFCSTATUS_PENDING == ret_val)
        {
            /* If HAL Connect is pending update the LibNFC state machine 
                and store the CB pointer and Context,
                mark the General CB pending status is TRUE */
            gpphLibContext->CBInfo.pClientConnectCb = pNotifyReConnect_RspCb;
            gpphLibContext->CBInfo.pClientConCntx = pContext;
            gpphLibContext->status.GenCb_pending_status = TRUE;
			gpphLibContext->LibNfcState.next_state = eLibNfcHalStateConnect;            

            gpphLibContext->Prev_Connected_handle = gpphLibContext->Connected_handle;

			gpphLibContext->Connected_handle = hRemoteDevice;
         }
         else if (NFCSTATUS_INVALID_REMOTE_DEVICE == PHNFCSTATUS(ret_val))
         {
           /* The Handle given for connect is invalid*/
            ret_val = NFCSTATUS_TARGET_NOT_CONNECTED;
         }
         else
         {
            /* Lower layer returns internal error code return NFCSTATUS_FAILED*/
            ret_val = NFCSTATUS_FAILED;
         }        
    }

    return ret_val;
}
#endif /* #ifdef RECONNECT_SUPPORT */


/**
* Connect to a single Remote Device 
*/
NFCSTATUS phLibNfc_RemoteDev_Connect(
                    phLibNfc_Handle             hRemoteDevice,
                    pphLibNfc_ConnectCallback_t pNotifyConnect_RspCb,
                    void                        *pContext
                    )
{

    NFCSTATUS RetVal = NFCSTATUS_FAILED;
    phLibNfc_sRemoteDevInformation_t *psRemoteDevInfo;
    
    if((NULL == gpphLibContext) ||
      (gpphLibContext->LibNfcState.cur_state == eLibNfcHalStateShutdown))   
    {
         RetVal = NFCSTATUS_NOT_INITIALISED;        
    }/* Check valid parameters*/
    else if((NULL == pContext)
        || (NULL == pNotifyConnect_RspCb)
        || (NULL == (void*)hRemoteDevice))
    {
       RetVal= NFCSTATUS_INVALID_PARAMETER;
    }   
    /* Check valid lib nfc State*/
    else if(gpphLibContext->LibNfcState.next_state
                            == eLibNfcHalStateShutdown)
    {
        RetVal= NFCSTATUS_SHUTDOWN;
    }
    else if((gpphLibContext->Discov_handle[0] != hRemoteDevice)&&
		(gpphLibContext->Discov_handle[1] != hRemoteDevice)&&
		(gpphLibContext->Discov_handle[2] != hRemoteDevice)&&
		(gpphLibContext->Discov_handle[3] != hRemoteDevice)&&
		(gpphLibContext->Discov_handle[4] != hRemoteDevice)&&
		(gpphLibContext->Discov_handle[5] != hRemoteDevice)&&
		(gpphLibContext->Discov_handle[6] != hRemoteDevice)&&
		(gpphLibContext->Discov_handle[7] != hRemoteDevice)&&
		(gpphLibContext->Discov_handle[8] != hRemoteDevice)&&
		(gpphLibContext->Discov_handle[9] != hRemoteDevice))
    {
        RetVal= NFCSTATUS_INVALID_HANDLE;
    }
    else if ((hRemoteDevice != gpphLibContext->Connected_handle) 
        && (0 != gpphLibContext->Connected_handle))
    {
        RetVal = NFCSTATUS_FAILED;
    }
    else
    {
        psRemoteDevInfo = (phLibNfc_sRemoteDevInformation_t*)hRemoteDevice;
       
        /* Call the HAL connect*/
        RetVal = phHal4Nfc_Connect(gpphLibContext->psHwReference,
                               psRemoteDevInfo,
                               phLibNfc_RemoteDev_Connect_Cb,
                               (void* )gpphLibContext);
        if(RetVal== NFCSTATUS_PENDING)
        {
            /* If HAL Connect is pending update the LibNFC state machine 
                and store the CB pointer and Context,
                mark the General CB pending status is TRUE*/
            gpphLibContext->CBInfo.pClientConnectCb = pNotifyConnect_RspCb;
            gpphLibContext->CBInfo.pClientConCntx = pContext;
            gpphLibContext->status.GenCb_pending_status=TRUE;
			gpphLibContext->LibNfcState.next_state = eLibNfcHalStateConnect;            
            gpphLibContext->Prev_Connected_handle = gpphLibContext->Connected_handle;
			gpphLibContext->Connected_handle = hRemoteDevice;
         }
         else if(PHNFCSTATUS(RetVal) == NFCSTATUS_INVALID_REMOTE_DEVICE)
         {
           /* The Handle given for connect is invalid*/
            RetVal= NFCSTATUS_TARGET_NOT_CONNECTED;
         }
         else
         {
            /* Lower layer returns internal error code return NFCSTATUS_FAILED*/
            RetVal = NFCSTATUS_FAILED;
         }        
    }
    return RetVal;
}

#ifdef RECONNECT_SUPPORT
STATIC 
void 
phLibNfc_config_discovery_con_failure_cb (
    void                *context,
    NFCSTATUS           status)
{
    if((phLibNfc_LibContext_t *)context == gpphLibContext)      
    {   /*check for same context*/
        pphLibNfc_ConnectCallback_t    ps_client_con_cb = 
                                    gpphLibContext->CBInfo.pClientConnectCb;

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
            status = NFCSTATUS_TARGET_LOST;

            phLibNfc_UpdateCurState (status,gpphLibContext);
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
            }

#endif /* #ifdef RESTART_CFG */
        }

        if (NULL != ps_client_con_cb)
        {
            gpphLibContext->CBInfo.pClientConnectCb = NULL;
            /* Call the upper layer callback*/      
            ps_client_con_cb (gpphLibContext->CBInfo.pClientConCntx,
                            0, NULL, status);
        }
    } /*End of if-context check*/
    else
    {   /*exception: wrong context pointer returned*/
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
        status = NFCSTATUS_FAILED;
    }

    
}
#endif /* #ifdef RECONNECT_SUPPORT */
/**
* Response callback for remote device connect
*/
STATIC void phLibNfc_RemoteDev_Connect_Cb(
                           void        *pContext,
                           phHal_sRemoteDevInformation_t *pRmtdev_info,
                           NFCSTATUS    status
                           )
{
    NFCSTATUS             Connect_status = NFCSTATUS_SUCCESS;
    /*Check valid lib nfc context is returned from lower layer*/
    if((phLibNfc_LibContext_t *)pContext == gpphLibContext)
    {
        gpphLibContext->LastTrancvSuccess = FALSE;

        /* Mark General Callback pending status as false*/
        gpphLibContext->status.GenCb_pending_status = FALSE;
        
        /* Check the shutdown is called during the lower layer Connect in process,
           If yes call shutdown call and return NFCSTATUS_SHUTDOWN */
        if((eLibNfcHalStateShutdown == gpphLibContext->LibNfcState.next_state))
        {   
            phLibNfc_Pending_Shutdown();
            Connect_status = NFCSTATUS_SHUTDOWN;    
    
        }
        else if(PHNFCSTATUS(status)==NFCSTATUS_SUCCESS)
        {
            /* Copy the Remote device address as connected handle*/
            gpphLibContext->Connected_handle =(uint32_t) pRmtdev_info;
            /* Update the state to connected and return status as SUCCESS*/
            gpphLibContext->LibNfcState.next_state = eLibNfcHalStateConnect;
            Connect_status = NFCSTATUS_SUCCESS;
        }
        else 
        {  /* if(PHNFCSTATUS(status)==NFCSTATUS_INVALID_REMOTE_DEVICE) */
            /* If remote device is invalid return as TARGET LOST to upper layer*/
            /* If error code is other than SUCCESS return NFCSTATUS_TARGET_LOST */
            Connect_status = NFCSTATUS_TARGET_LOST;
            gpphLibContext->Connected_handle = gpphLibContext->Prev_Connected_handle ;
        }
        gpphLibContext->ndef_cntx.is_ndef = CHK_NDEF_NOT_DONE;
        /* Update the Current Sate*/
        phLibNfc_UpdateCurState(Connect_status,(phLibNfc_LibContext_t *)pContext);
        /* Call the upper layer callback*/      
        gpphLibContext->CBInfo.pClientConnectCb(
                    gpphLibContext->CBInfo.pClientConCntx,
                    (uint32_t)pRmtdev_info,
                    (phLibNfc_sRemoteDevInformation_t*)pRmtdev_info,
                    Connect_status);
    }
    else
    {   /*exception: wrong context pointer returned*/
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
    }
    return;
}

/**
* Allows to disconnect from already connected target.
*/
NFCSTATUS phLibNfc_RemoteDev_Disconnect( phLibNfc_Handle                 hRemoteDevice,
                                        phLibNfc_eReleaseType_t          ReleaseType,
                                        pphLibNfc_DisconnectCallback_t   pDscntCallback,
                                        void*                            pContext
                                        )
{
    NFCSTATUS RetVal = NFCSTATUS_SUCCESS;
    phLibNfc_sRemoteDevInformation_t *psRemoteDevInfo=NULL;

    /*Check for valid parameter*/
    if((NULL == gpphLibContext) ||
        (gpphLibContext->LibNfcState.cur_state
                            == eLibNfcHalStateShutdown))
    {
        RetVal = NFCSTATUS_NOT_INITIALISED;        
    }
    else if((NULL == pContext) ||
        (NULL == pDscntCallback)||(hRemoteDevice == 0))
    {
        RetVal= NFCSTATUS_INVALID_PARAMETER;
    }
    /* Check for valid state,If De initialize is called then
    return NFCSTATUS_SHUTDOWN */
    else if(gpphLibContext->LibNfcState.next_state
                            == eLibNfcHalStateShutdown)
    {
        RetVal= NFCSTATUS_SHUTDOWN;
    }
    else if(gpphLibContext->Connected_handle==0)
    {
        RetVal=NFCSTATUS_TARGET_NOT_CONNECTED;
    }
    /* The given handle is not the connected handle return NFCSTATUS_INVALID_HANDLE*/
    else if(hRemoteDevice != gpphLibContext->Connected_handle )
    {
        RetVal=NFCSTATUS_INVALID_HANDLE;
    }
    else
    {
        if((eLibNfcHalStateRelease == gpphLibContext->LibNfcState.next_state)
            ||((gpphLibContext->sSeContext.eActivatedMode == phLibNfc_SE_ActModeWired)&&
                    (ReleaseType != NFC_SMARTMX_RELEASE))
                    ||((gpphLibContext->sSeContext.eActivatedMode != phLibNfc_SE_ActModeWired)&&
                            (ReleaseType == NFC_SMARTMX_RELEASE)))
        {   /* Previous disconnect callback is pending */
            RetVal = NFCSTATUS_REJECTED;            
        }
#ifndef LLCP_CHANGES
        else if(eLibNfcHalStateTransaction == gpphLibContext->LibNfcState.next_state)
        {   /* Previous  Transaction is Pending*/
            RetVal = NFCSTATUS_BUSY;            
            PHDBG_INFO("LibNfc:Transaction is Pending");
        }
#endif /* #ifdef LLCP_CHANGES */
        else
        {           
            gpphLibContext->ReleaseType = ReleaseType;
            psRemoteDevInfo = (phLibNfc_sRemoteDevInformation_t*)hRemoteDevice;
            RetVal = phHal4Nfc_Disconnect(gpphLibContext->psHwReference,
                                (phHal_sRemoteDevInformation_t*)psRemoteDevInfo,
                                gpphLibContext->ReleaseType,
                                (pphHal4Nfc_DiscntCallback_t)
                                phLibNfc_RemoteDev_Disconnect_cb,
                                (void *)gpphLibContext);
            if( NFCSTATUS_PENDING == PHNFCSTATUS(RetVal))
            {
                /*Copy the upper layer Callback pointer and context*/
                gpphLibContext->CBInfo.pClientDisConnectCb = pDscntCallback;
                gpphLibContext->CBInfo.pClientDConCntx = pContext;
                /* Mark general callback pending status as TRUE and update the state*/
                gpphLibContext->status.GenCb_pending_status=TRUE;
				gpphLibContext->LibNfcState.next_state = eLibNfcHalStateRelease; 
                
            }
            else
            {
                /*If lower layer returns other than pending 
                (internal error codes) return NFCSTATUS_FAILED */
                RetVal = NFCSTATUS_FAILED;
            }    
        }
    }
    return RetVal;
}
/**
* Response callback for Remote device Disconnect.
*/
STATIC void phLibNfc_RemoteDev_Disconnect_cb(
                                void                          *context,
                                phHal_sRemoteDevInformation_t *reg_handle,
                                NFCSTATUS                      status
                                )
{
    NFCSTATUS             DisCnct_status = NFCSTATUS_SUCCESS;
    pphLibNfc_DisconnectCallback_t pUpper_NtfCb = NULL;
        void  *pUpper_Context = NULL;
    
    /* Copy the upper layer Callback and context*/
    pUpper_NtfCb = gpphLibContext->CBInfo.pClientDisConnectCb;
    pUpper_Context = gpphLibContext->CBInfo.pClientDConCntx;

    /* Check valid context is returned or not */
    if((phLibNfc_LibContext_t *)context != gpphLibContext)
    {
        /*exception: wrong context pointer returned*/
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
    }
    else
    {
        /* Mark the General callback pending status FALSE   */
        gpphLibContext->status.GenCb_pending_status = FALSE;        
        gpphLibContext->CBInfo.pClientDisConnectCb = NULL;
        gpphLibContext->CBInfo.pClientDConCntx = NULL;
        
        gpphLibContext->ndef_cntx.is_ndef = CHK_NDEF_NOT_DONE;
        gpphLibContext->LastTrancvSuccess = FALSE;
        /*Reset Connected handle */
        gpphLibContext->Connected_handle=0x0000;
        /*Reset previous Connected handle */
        gpphLibContext->Prev_Connected_handle = 0x0000;

        if(gpphLibContext->sSeContext.eActivatedMode == phLibNfc_SE_ActModeWired)
        {
          gpphLibContext->sSeContext.eActivatedMode = phLibNfc_SE_ActModeDefault;
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
    }   
    /* Check DeInit is called or not */
    if(eLibNfcHalStateShutdown == gpphLibContext->LibNfcState.next_state)
    {
        /*call shutdown and return  status as NFCSTATUS_SHUTDOWN */ 
        phLibNfc_Pending_Shutdown();
        DisCnct_status = NFCSTATUS_SHUTDOWN;    
    }
    else if(NFCSTATUS_SUCCESS == status)
    {
        DisCnct_status = NFCSTATUS_SUCCESS;     
		gpphLibContext->LibNfcState.next_state = eLibNfcHalStateRelease;         
    }
    else
    {           
        DisCnct_status = NFCSTATUS_FAILED;
        phLibNfc_UpdateCurState(DisCnct_status,(phLibNfc_LibContext_t *)context);
    }
    /* Call the upper layer Callback */
    (*pUpper_NtfCb)(pUpper_Context,
                    (uint32_t)reg_handle,
                    DisCnct_status);
    return;
}

/**
* This interface allows to perform Read/write operation on remote device.
*/
NFCSTATUS
phLibNfc_RemoteDev_Transceive(phLibNfc_Handle                   hRemoteDevice,
                              phLibNfc_sTransceiveInfo_t*       psTransceiveInfo,
                              pphLibNfc_TransceiveCallback_t    pTransceive_RspCb,
                              void*                             pContext
                              )
{
    NFCSTATUS RetVal = NFCSTATUS_SUCCESS;
    
    /*Check for valid parameter */
    
    if((NULL == gpphLibContext) ||
        (gpphLibContext->LibNfcState.cur_state
                            == eLibNfcHalStateShutdown))
    {
        RetVal = NFCSTATUS_NOT_INITIALISED;
    }
    else if((NULL == psTransceiveInfo)
        || (NULL == pTransceive_RspCb)
        || (NULL == (void *)hRemoteDevice)
        || (NULL == psTransceiveInfo->sRecvData.buffer)
        || (NULL == psTransceiveInfo->sSendData.buffer)
        || (NULL == pContext))
    {
        RetVal= NFCSTATUS_INVALID_PARAMETER;        
    }
    /* Check the state for DeInit is called or not,if yes return NFCSTATUS_SHUTDOWN*/
    else if(gpphLibContext->LibNfcState.next_state
                            == eLibNfcHalStateShutdown)
    {       
        RetVal= NFCSTATUS_SHUTDOWN;
    }/* If there is no handle connected return NFCSTATUS_TARGET_NOT_CONNECTED*/
    else if(gpphLibContext->Connected_handle==0)
    {
        RetVal=NFCSTATUS_TARGET_NOT_CONNECTED;
    }/* If the given handle is not the connected handle return NFCSTATUS_INVALID_HANDLE */
	else if(gpphLibContext->Connected_handle!= hRemoteDevice )
    {
        RetVal=NFCSTATUS_INVALID_HANDLE;
    } /*If the transceive is called before finishing the previous transceive function
      return NFCSTATUS_REJECTED  */
    else if((eLibNfcHalStateTransaction ==
        gpphLibContext->LibNfcState.next_state)
        ||(phHal_eNfcIP1_Initiator==
        ((phHal_sRemoteDevInformation_t*)hRemoteDevice)->RemDevType))
    {
        RetVal = NFCSTATUS_REJECTED;
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
        gpphLibContext->ndef_cntx.eLast_Call = RawTrans;
        (void)memcpy((void *)(gpphLibContext->psTransInfo), 
                    (void *)psTransceiveInfo, 
                    sizeof(phLibNfc_sTransceiveInfo_t));
        /* Check the given Mifare command is supported or not , 
                            If not return NFCSTATUS_COMMAND_NOT_SUPPORTED */
        if( (((phHal_sRemoteDevInformation_t*)hRemoteDevice)->RemDevType == 
               phHal_eMifare_PICC)&&
            ( gpphLibContext->psTransInfo->cmd.MfCmd != phHal_eMifareRaw ) &&
            ( gpphLibContext->psTransInfo->cmd.MfCmd != phHal_eMifareAuthentA ) &&
            ( gpphLibContext->psTransInfo->cmd.MfCmd != phHal_eMifareAuthentB ) &&
            ( gpphLibContext->psTransInfo->cmd.MfCmd != phHal_eMifareRead16 ) &&
            ( gpphLibContext->psTransInfo->cmd.MfCmd != phHal_eMifareRead ) &&
            ( gpphLibContext->psTransInfo->cmd.MfCmd != phHal_eMifareWrite16 ) &&
            ( gpphLibContext->psTransInfo->cmd.MfCmd != phHal_eMifareWrite4 ) &&
            ( gpphLibContext->psTransInfo->cmd.MfCmd != phHal_eMifareDec ) &&
            ( gpphLibContext->psTransInfo->cmd.MfCmd != phHal_eMifareTransfer ) &&
            ( gpphLibContext->psTransInfo->cmd.MfCmd != phHal_eMifareRestore ) &&
            ( gpphLibContext->psTransInfo->cmd.MfCmd != phHal_eMifareReadSector ) &&
            ( gpphLibContext->psTransInfo->cmd.MfCmd != phHal_eMifareWriteSector ))
        {
            RetVal = NFCSTATUS_COMMAND_NOT_SUPPORTED;
        }           
        if(eLibNfcHalStatePresenceChk !=
                     gpphLibContext->LibNfcState.next_state)
        {
            PHDBG_INFO("LibNfc:Transceive In Progress");
            if((((phHal_sRemoteDevInformation_t*)hRemoteDevice)->RemDevType == 
               phHal_eMifare_PICC) && (((phHal_sRemoteDevInformation_t*)
               hRemoteDevice)->RemoteDevInfo.Iso14443A_Info.Sak != 0)&&
               (phHal_eMifareAuthentA == gpphLibContext->psTransInfo->cmd.MfCmd))
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
                gpphLibContext->psBufferedAuth->addr = psTransceiveInfo->addr;
                gpphLibContext->psBufferedAuth->cmd = psTransceiveInfo->cmd;
                gpphLibContext->psBufferedAuth->sSendData.length 
                    = psTransceiveInfo->sSendData.length;
                gpphLibContext->psBufferedAuth->sRecvData.length 
                    = psTransceiveInfo->sRecvData.length;                
                gpphLibContext->psBufferedAuth->sSendData.buffer
                  = (uint8_t *)
                      phOsalNfc_GetMemory(
                      gpphLibContext->psTransInfo->sSendData.length);
                
                (void)memcpy((void *)
                        (gpphLibContext->psBufferedAuth->sSendData.buffer), 
                        (void *)psTransceiveInfo->sSendData.buffer, 
                        psTransceiveInfo->sSendData.length);
               
                gpphLibContext->psBufferedAuth->sRecvData.buffer
                  = (uint8_t *)
                      phOsalNfc_GetMemory(
                        gpphLibContext->psTransInfo->sRecvData.length);             
            }
            /*Call the lower layer Transceive function */
            RetVal = phHal4Nfc_Transceive( gpphLibContext->psHwReference,
                                        (phHal_sTransceiveInfo_t*)gpphLibContext->psTransInfo,
                                        (phLibNfc_sRemoteDevInformation_t*)hRemoteDevice,
                                        (pphHal4Nfc_TransceiveCallback_t)
                                        phLibNfc_RemoteDev_Transceive_Cb,
                                        (void* )gpphLibContext);
            if(PHNFCSTATUS(RetVal) == NFCSTATUS_PENDING)
            {
                /* Copy the upper layer callback pointer and context */
                gpphLibContext->CBInfo.pClientTransceiveCb = pTransceive_RspCb;
                gpphLibContext->CBInfo.pClientTranseCntx = pContext;
                /* Mark the General callback pending status is TRUE */
                gpphLibContext->status.GenCb_pending_status = TRUE;
                /*Transceive is in Progress-Used in Release API*/

                /*Update the state machine*/
                gpphLibContext->LibNfcState.next_state = eLibNfcHalStateTransaction;
            }
        }       
        else
        {
            gpphLibContext->status.GenCb_pending_status = FALSE;
            RetVal = NFCSTATUS_FAILED;
        }       
    }
    return RetVal;
}
/**
* Response for Remote device transceive.
*/
STATIC 
void phLibNfc_RemoteDev_Transceive_Cb(void *context,
                                    phHal_sRemoteDevInformation_t *pRmtdev_info,
                                    phNfc_sData_t *response,
                                    NFCSTATUS status
                                    )
{
    NFCSTATUS             trans_status = NFCSTATUS_SUCCESS;
    phNfc_sData_t         *trans_resp= NULL;
    void                  *pUpper_Context = NULL;
    pphLibNfc_TransceiveCallback_t pUpper_TagNtfCb =
                            gpphLibContext->CBInfo.pClientTransceiveCb;

    /*Check valid context is returned or not */
    if((phLibNfc_LibContext_t *)context == gpphLibContext)
    {
        trans_resp = &gpphLibContext->psTransInfo->sRecvData;

        pUpper_Context = gpphLibContext->CBInfo.pClientTranseCntx;
        gpphLibContext->status.GenCb_pending_status = FALSE;

        /*If DeInit is called during the transceive,
           call the shutdown and return NFCSTATUS_SHUTDOWN*/
        if(gpphLibContext->LibNfcState.next_state
                            == eLibNfcHalStateShutdown)
        {
            phLibNfc_Pending_Shutdown();
            trans_status = NFCSTATUS_SHUTDOWN;
        }
         /* If Disconnect is called return NFCSTATUS_ABORTED */
        else if(eLibNfcHalStateRelease == 
                gpphLibContext->LibNfcState.next_state)
        {
            trans_status = NFCSTATUS_ABORTED;
        }
        /* If the received lower layer status is not SUCCESS return NFCSTATUS_FAILED */
        else if( NFCSTATUS_SUCCESS == status)
        {
            trans_status = NFCSTATUS_SUCCESS;
        }        
        else if((PHNFCSTATUS(status) != NFCSTATUS_SUCCESS) &&
                (phHal_eMifare_PICC == pRmtdev_info->RemDevType) && 
                (0x00 != pRmtdev_info->RemoteDevInfo.Iso14443A_Info.Sak))
        {
            gpphLibContext->LastTrancvSuccess = FALSE;
            trans_status = NFCSTATUS_FAILED;
            /* card type is mifare 1k/4k, then reconnect */
            trans_status = phHal4Nfc_Connect(gpphLibContext->psHwReference,  
                        pRmtdev_info,
                        (pphHal4Nfc_ConnectCallback_t)
                        phLibNfc_Reconnect_Mifare_Cb,
                        (void *)gpphLibContext);
        }
        else if ((PHNFCSTATUS(status) == PN544_IO_TIMEOUT_RESPONSE) ||
                 (PHNFCSTATUS(status) == NFCSTATUS_RF_TIMEOUT))
        {
            // 0x89, 0x09 HCI response values from PN544 indicate timeout
            trans_status = NFCSTATUS_TARGET_LOST;
        }
        else
        {
            // PN544 did get some reply from tag, just not valid
            trans_status = NFCSTATUS_FAILED;
        }
        /*Update the state machine */
        phLibNfc_UpdateCurState(status,gpphLibContext);
        gpphLibContext->LibNfcState.next_state = eLibNfcHalStateConnect;
        if(NFCSTATUS_PENDING != trans_status)  
        {
            /* Tranceive over */              
            PHDBG_INFO("LibNfc:TXRX Callback-Update the Transceive responce");
            if (NULL != pUpper_TagNtfCb)
            {
                if(trans_status == NFCSTATUS_SUCCESS)
                {
                    gpphLibContext->LastTrancvSuccess = TRUE;
                    pUpper_Context = gpphLibContext->CBInfo.pClientTranseCntx;
                    trans_resp->buffer = response->buffer;
                    trans_resp->length = response->length;                  
                            /* Notify the upper layer */
                    PHDBG_INFO("LibNfc:Transceive Complete");
                    /* Notify the Transceive Completion to upper layer */
                    gpphLibContext->CBInfo.pClientTransceiveCb(pUpper_Context,
                                (uint32_t)pRmtdev_info,  
                                trans_resp,
                                trans_status);
                }
                else
                {
                    gpphLibContext->LastTrancvSuccess = FALSE;
                    pUpper_Context = gpphLibContext->CBInfo.pClientTranseCntx;                  
                    trans_resp->length = 0;                     
                            /* Notify the upper layer */
                    PHDBG_INFO("LibNfc:Transceive Complete");
                    /* Notify the Transceive Completion to upper layer */
                    gpphLibContext->CBInfo.pClientTransceiveCb(pUpper_Context,
                                (uint32_t)pRmtdev_info,  
                                trans_resp,
                                trans_status);
                }
            }
       }
       
    }
    else
    {    /*exception: wrong context pointer returned*/
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
    }

    return;
}
/**
* Interface to configure P2P configurations.
*/
NFCSTATUS 
phLibNfc_Mgt_SetP2P_ConfigParams(phLibNfc_sNfcIPCfg_t*		pConfigInfo,
                                pphLibNfc_RspCb_t			pConfigRspCb,
                                void*						pContext
                                )
{
    NFCSTATUS RetVal = NFCSTATUS_FAILED;
    /* LibNfc Initialized or not */
    if((NULL == gpphLibContext)|| 
        (gpphLibContext->LibNfcState.cur_state == eLibNfcHalStateShutdown))
    {
        RetVal = NFCSTATUS_NOT_INITIALISED;
    }/* Check for valid parameters */
    else if((NULL == pConfigInfo) || (NULL == pConfigRspCb)
        || (NULL == pContext))
    {
        RetVal= NFCSTATUS_INVALID_PARAMETER;
    }
    else if(gpphLibContext->LibNfcState.next_state == eLibNfcHalStateShutdown)
    {
        RetVal = NFCSTATUS_SHUTDOWN;
    }    
    else if(TRUE == gpphLibContext->status.GenCb_pending_status)        
    { /*Previous callback is pending */
        RetVal = NFCSTATUS_BUSY;
    }
    else
    {
        if(eLibNfcHalStatePresenceChk !=
                gpphLibContext->LibNfcState.next_state)
        {
            phHal_uConfig_t uConfig;  
            /* copy General bytes of Max length = 48 bytes */
            (void)memcpy((void *)&(uConfig.nfcIPConfig.generalBytes),
                    (void *)pConfigInfo->generalBytes,
                    pConfigInfo->generalBytesLength);
            /* also copy the General Bytes length*/
            uConfig.nfcIPConfig.generalBytesLength = pConfigInfo->generalBytesLength;

            RetVal = phHal4Nfc_ConfigParameters(                                    
                                gpphLibContext->psHwReference,
                                NFC_P2P_CONFIG,
                                &uConfig,
                                phLibNfc_Mgt_SetP2P_ConfigParams_Cb,
                                (void *)gpphLibContext
                                );          
        }
        else
        {
             gpphLibContext->sNfcIp_Context.pClientNfcIpCfgCb= NULL;
             RetVal = NFCSTATUS_PENDING;
        }
        if(NFCSTATUS_PENDING == RetVal)
        {
            /* save the context and callback for later use */
            gpphLibContext->sNfcIp_Context.pClientNfcIpCfgCb = pConfigRspCb;
            gpphLibContext->sNfcIp_Context.pClientNfcIpCfgCntx = pContext;
            gpphLibContext->status.GenCb_pending_status=TRUE;
            /* Next state is configured */
            gpphLibContext->LibNfcState.next_state =eLibNfcHalStateConfigReady;            
        }       
        else
        {
            RetVal = NFCSTATUS_FAILED;
        }
    }
    return RetVal;
}
/**
* Response callback for P2P configurations.
*/
STATIC void phLibNfc_Mgt_SetP2P_ConfigParams_Cb(void     *context,
                                        NFCSTATUS status)
{
        pphLibNfc_RspCb_t       pClientCb=NULL;    
    void                    *pUpperLayerContext=NULL;
     /* Check for the context returned by below layer */
    if((phLibNfc_LibContext_t *)context != gpphLibContext)
    {   /*wrong context returned*/
        phOsalNfc_RaiseException(phOsalNfc_e_InternalErr,1);
    }
    else
    {
        if(eLibNfcHalStateShutdown == gpphLibContext->LibNfcState.next_state)
        {   /*shutdown called before completion of this api allow
            shutdown to happen */
            phLibNfc_Pending_Shutdown();
            status = NFCSTATUS_SHUTDOWN;    
        }
        else
        {
            gpphLibContext->status.GenCb_pending_status = FALSE;
            if(NFCSTATUS_SUCCESS != status)
            {   
                status = NFCSTATUS_FAILED;
            }
            else
            {
                status = NFCSTATUS_SUCCESS;
            }
        }
        /*update the current state */
        phLibNfc_UpdateCurState(status,gpphLibContext);

        pClientCb = gpphLibContext->sNfcIp_Context.pClientNfcIpCfgCb;
        pUpperLayerContext = gpphLibContext->sNfcIp_Context.pClientNfcIpCfgCntx;

        gpphLibContext->sNfcIp_Context.pClientNfcIpCfgCb = NULL;
        gpphLibContext->sNfcIp_Context.pClientNfcIpCfgCntx = NULL;
        if (NULL != pClientCb)
        {
            /* Notify to upper layer status of configure operation */
            pClientCb(pUpperLayerContext, status);
        }
    }   
    return;
}










