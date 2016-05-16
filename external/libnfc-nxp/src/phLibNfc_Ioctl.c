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
 * \file phLibNfc_Ioctl.c

 * Project: NFC FRI 1.1
 *
 * $Date: Mon Mar  1 19:07:05 2010 $
 * $Author: ing07385 $
 * $Revision: 1.35 $
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
#include <phLibNfc_ioctl.h>
#include <phNfcStatus.h>

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

STATIC phLibNfc_Ioctl_Cntx_t phLibNfc_Ioctl_Cntx;

/*
*************************** Static Function Declaration ***********************
*/


/* Response callback for Ioctl management */
STATIC  void phLibNfc_Ioctl_Mgmt_CB(void            *context,
                              phNfc_sData_t         *pOutData,
                              NFCSTATUS             status );


/** Response callback for UICC switch mode*/
STATIC void phLibNfc_Switch_Swp_Mode_CB(
                                void  *context,
                                NFCSTATUS status                                        
                                );

/*
*************************** Function Definitions ******************************
*/

/**
* The I/O Control function allows the caller to configure specific
* functionality provided by the lower layer.Each feature is accessible
* via a specific IOCTL Code.
*/
NFCSTATUS phLibNfc_Mgt_IoCtl    (void*                      pDalHandle,
                                 uint16_t                   IoctlCode,        
                                 phNfc_sData_t*             pInParam,
                                 phNfc_sData_t*             pOutParam,
                                 pphLibNfc_IoctlCallback_t  pIoCtl_Rsp_cb,
                                 void*                      pContext
                                 )
{
    NFCSTATUS StatusCode=NFCSTATUS_INVALID_PARAMETER;
   

    if((IoctlCode==0)||(NULL==pIoCtl_Rsp_cb) ||
		(NULL==pContext) ||(NULL==pInParam)  ||
		(NULL==pDalHandle))
    {
        StatusCode=NFCSTATUS_INVALID_PARAMETER;
        return StatusCode;
    }
    if(IoctlCode!= NFC_FW_DOWNLOAD)
    {
        if(pOutParam == NULL)
        {
            StatusCode = NFCSTATUS_INVALID_PARAMETER;
            return StatusCode;
        }
        if(( gpphLibContext == NULL) ||
			(gpphLibContext->LibNfcState.cur_state == eLibNfcHalStateShutdown))
        {
            StatusCode = NFCSTATUS_NOT_INITIALISED;
            return StatusCode;
        }
        else
        {
            if(gpphLibContext->LibNfcState.next_state == eLibNfcHalStateShutdown)
            {
                StatusCode = NFCSTATUS_SHUTDOWN;
                return StatusCode;
            }
        }
    }
    phLibNfc_Ioctl_Cntx.CliRspCb =pIoCtl_Rsp_cb;
    phLibNfc_Ioctl_Cntx.pCliCntx = pContext;
    phLibNfc_Ioctl_Cntx.pOutParam = pOutParam;
    phLibNfc_Ioctl_Cntx.IoctlCode = IoctlCode;
    /* Process the IOCTL requests */
    switch(IoctlCode)
    {
        case NFC_FW_DOWNLOAD:
        {/* Set power status */
            phLibNfc_Ioctl_Cntx.psHwReference = phOsalNfc_GetMemory((uint32_t)sizeof(phHal_sHwReference_t));
            if(phLibNfc_Ioctl_Cntx.psHwReference == NULL)
                return NFCSTATUS_FAILED;
            (void)memset(phLibNfc_Ioctl_Cntx.psHwReference,0,sizeof(phHal_sHwReference_t));
            phLibNfc_Ioctl_Cntx.psHwReference->p_board_driver = pDalHandle;

            StatusCode = phHal4Nfc_Ioctl( phLibNfc_Ioctl_Cntx.psHwReference,
                                          NFC_FW_DOWNLOAD,
                                          pInParam,
                                          pOutParam,
                                          phLibNfc_Ioctl_Mgmt_CB,
                                          &phLibNfc_Ioctl_Cntx );
        }break;
        case NFC_MEM_READ:
        {
           StatusCode = phHal4Nfc_Ioctl(gpphLibContext->psHwReference,
                                         NFC_MEM_READ,
                                         pInParam,
                                         pOutParam,
                                         phLibNfc_Ioctl_Mgmt_CB,
                                         &phLibNfc_Ioctl_Cntx );
           
        }break;
        case NFC_MEM_WRITE:
        {
           
           StatusCode = phHal4Nfc_Ioctl( gpphLibContext->psHwReference,
				                          NFC_MEM_WRITE,
                                          pInParam,
                                          pOutParam,
                                          phLibNfc_Ioctl_Mgmt_CB,
                                          &phLibNfc_Ioctl_Cntx );          
            
        }break;	
		case PHLIBNFC_ANTENNA_TEST:
		{
		
                StatusCode = phHal4Nfc_Ioctl( gpphLibContext->psHwReference,
                                          PHLIBNFC_ANTENNA_TEST,
                                          pInParam, 
                                          pOutParam,
                                          phLibNfc_Ioctl_Mgmt_CB,
                                          &phLibNfc_Ioctl_Cntx );                
        
		}break;
		case PHLIBNFC_SWP_TEST:
		{
		
                StatusCode = phHal4Nfc_Ioctl( gpphLibContext->psHwReference,
                                          PHLIBNFC_SWP_TEST,
                                          pInParam, 
                                          pOutParam,
                                          phLibNfc_Ioctl_Mgmt_CB,
                                          &phLibNfc_Ioctl_Cntx );
                        
		}break;
		
		case PHLIBNFC_PRBS_TEST:
		{
		        StatusCode = phHal4Nfc_Ioctl( gpphLibContext->psHwReference,
                                          PHLIBNFC_PRBS_TEST,
                                          pInParam, 
                                          pOutParam,
                                          phLibNfc_Ioctl_Mgmt_CB,
                                          &phLibNfc_Ioctl_Cntx );
                
        
		}break;
        case PHLIBNFC_SWITCH_SWP_MODE:
		{
            StatusCode = phHal4Nfc_Switch_Swp_Mode( gpphLibContext->psHwReference,
                                          (phHal_eSWP_Mode_t)pInParam->buffer[0],
                                          phLibNfc_Switch_Swp_Mode_CB,
                                          &phLibNfc_Ioctl_Cntx 
                                          );
                
        
		}break;
        default :
        {
          /* don't do any thing*/
        }break;

    } /* End of IOCTL  switch */
	if(StatusCode!=NFCSTATUS_PENDING)
    {
		StatusCode = NFCSTATUS_FAILED;
    }
	else
	{
		if(IoctlCode!= NFC_FW_DOWNLOAD)
		{
			gpphLibContext->status.GenCb_pending_status=TRUE;
		}
	}
    return StatusCode;

}   /* End of IOCTL handler function */



STATIC  void phLibNfc_Ioctl_Mgmt_CB(void          *context,
                              phNfc_sData_t *pOutData,
                              NFCSTATUS      status )
{
    phLibNfc_Ioctl_Cntx_t *pIoctlCntx=NULL;
    if(PHNFCSTATUS(status) == NFCSTATUS_FEATURE_NOT_SUPPORTED)
    {
        status = NFCSTATUS_FEATURE_NOT_SUPPORTED;
    }
    else if(PHNFCSTATUS(status)!=NFCSTATUS_SUCCESS)
    {
        status = NFCSTATUS_FAILED;
    }
	if(gpphLibContext!= NULL)
	{
		if(eLibNfcHalStateShutdown == gpphLibContext->LibNfcState.next_state)
		{
		    /*If shutdown called in between allow shutdown to happen*/
			phLibNfc_Pending_Shutdown();
			status = NFCSTATUS_SHUTDOWN;
		}
	}
    pIoctlCntx= (phLibNfc_Ioctl_Cntx_t*)context;
    if( pIoctlCntx !=NULL)
    {
        switch(pIoctlCntx->IoctlCode)
        {
            case NFC_FW_DOWNLOAD:
            {
                /*Release the hardware reference memory*/
                phOsalNfc_FreeMemory(pIoctlCntx->psHwReference);
            }break;
            case NFC_MEM_READ:
            {

            }break;
            case NFC_MEM_WRITE:
            {

            }break;

			case PHLIBNFC_ANTENNA_TEST:
            {
            
            }break; 
			case PHLIBNFC_SWP_TEST:
            {
            
            }break;	
			case PHLIBNFC_PRBS_TEST:
            {
            
            }break;
            default:
            {
            }
        }
        pIoctlCntx->CliRspCb(pIoctlCntx->pCliCntx,pOutData,status);
		if(gpphLibContext!= NULL)
		{
			gpphLibContext->status.GenCb_pending_status=FALSE;
		}
    }
}

STATIC void phLibNfc_Switch_Swp_Mode_CB(
                                void  *context,
                                NFCSTATUS status                                        
                                )
{
    if(PHNFCSTATUS(status)!=NFCSTATUS_SUCCESS)
    {       
        status = NFCSTATUS_FAILED;
    }
    if(gpphLibContext!= NULL)
	{
		if(eLibNfcHalStateShutdown == gpphLibContext->LibNfcState.next_state)
		{
		    /*If shutdown called in between allow shutdown to happen*/
			phLibNfc_Pending_Shutdown();
			status = NFCSTATUS_SHUTDOWN;
		}
	}
    if((NULL != context)&&(context == (void *)&phLibNfc_Ioctl_Cntx))
    {
        if(NULL != phLibNfc_Ioctl_Cntx.CliRspCb)
        {
            (*phLibNfc_Ioctl_Cntx.CliRspCb)(
                phLibNfc_Ioctl_Cntx.pCliCntx,
                phLibNfc_Ioctl_Cntx.pOutParam,
                status
                );
        }
    }
    return;
}

