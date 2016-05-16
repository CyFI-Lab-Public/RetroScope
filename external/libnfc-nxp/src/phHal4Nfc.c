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
 * \file  phHal4Nfc.c
 * \brief Hal4Nfc source.
 *
 * Project: NFC-FRI 1.1
 *
 * $Date: Fri Jun 11 09:32:23 2010 $
 * $Author: ing07385 $
 * $Revision: 1.192 $
 * $Aliases: NFC_FRI1.1_WK1023_R35_1 $
 *
 */

/* ---------------------------Include files ---------------------------------*/

#include <phHal4Nfc.h>
#include <phHal4Nfc_Internal.h>
#include <phOsalNfc.h>
#include <phHciNfc.h>
#include <phLlcNfc.h>
#include <phDal4Nfc.h>
#include <phDnldNfc.h>
#include <phOsalNfc_Timer.h>

/* ------------------------------- Macros -----------------------------------*/
#ifndef HAL_UNIT_TEST
#define STATIC static
#else
#define STATIC
#endif/*#ifndef UNIT_TEST*/
#define HAL4_LAYERS                 3
#define LAYER_HCI                   2
#define LAYER_LLC                   1
#define LAYER_DAL                   0

/* --------------------Structures and enumerations --------------------------*/

phHal_sHwReference_t *gpphHal4Nfc_Hwref;

static void phHal4Nfc_IoctlComplete(
                                    phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                                    void     *pInfo
                                    );

static void phHal4Nfc_LowerNotificationHandler(
                                        void    *pContext,
                                        void    *pHwRef,
                                        uint8_t  type,
                                        void     *pInfo
                                        );
static void phHal4Nfc_HandleEvent(
                              phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                              void     *pInfo
                              );

static void phHal4Nfc_OpenComplete(
                                   phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                                   void *pInfo
                                   );

static void phHal4Nfc_CloseComplete(
                                    phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                                    void *pInfo
                                    );

static void phHal4Nfc_DownloadComplete(
                                void *pContext,
                                void *pHwRef,
                                uint8_t type,
                                void *pInfo
                                );

static NFCSTATUS phHal4Nfc_Configure_Layers(
                                phNfcLayer_sCfg_t       **pphLayer
                                );


/*Callback for Self tests*/
static void phHal4Nfc_SelfTestComplete(
                                       phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                                       void *pInfo
                                       );

/**
 *  The open callback function to be called by the HCI when open (initializaion)
 *  sequence is completed  or if there is an error in initialization.
 *  It is passed as a parameter to HCI when calling HCI Init.
 */

static void phHal4Nfc_OpenComplete(
                                   phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                                   void *pInfo
                                   )
{
    NFCSTATUS status = ((phNfc_sCompletionInfo_t *)pInfo)->status;
    pphHal4Nfc_GenCallback_t pUpper_OpenCb
                                    = Hal4Ctxt->sUpperLayerInfo.pUpperOpenCb;
    void                   *pUpper_Context
                                = Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt;
    if(status == NFCSTATUS_SUCCESS)
    {
        PHDBG_INFO("Hal4:Open Successful");
#ifdef MERGE_SAK_SW1 /*Software Workaround*/
        if(eHal4StateOpenAndReady == Hal4Ctxt->Hal4NextState)
        {
            status = phHciNfc_System_Configure (
                                    Hal4Ctxt->psHciHandle,
                                    (void *)gpphHal4Nfc_Hwref,
                                    PH_HAL4NFC_TGT_MERGE_ADDRESS,
                                    PH_HAL4NFC_TGT_MERGE_SAK /*config value*/
                                    );
        }
        if(NFCSTATUS_PENDING != status)
#endif/*#ifdef MERGE_SAK_SW1*/
        {
            /*Update State*/
            Hal4Ctxt->Hal4CurrentState = Hal4Ctxt->Hal4NextState;
            Hal4Ctxt->Hal4NextState = eHal4StateInvalid;
            Hal4Ctxt->sUpperLayerInfo.pUpperOpenCb = NULL;
            if(NULL != pUpper_OpenCb)
            {
                /*Upper layer's Open Cb*/
                (*pUpper_OpenCb)(Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
                    NFCSTATUS_SUCCESS
                    );
            }
        }
    }
    else/*Open did not succeed.Go back to reset state*/
    {
        Hal4Ctxt->Hal4CurrentState = eHal4StateClosed;
        Hal4Ctxt->Hal4NextState = eHal4StateInvalid;
        Hal4Ctxt->psHciHandle = NULL;
        phOsalNfc_FreeMemory((void *)Hal4Ctxt->pHal4Nfc_LayerCfg);
        Hal4Ctxt->pHal4Nfc_LayerCfg = NULL;
        phOsalNfc_FreeMemory((void *)Hal4Ctxt);
        gpphHal4Nfc_Hwref->hal_context = NULL;
        gpphHal4Nfc_Hwref = NULL;
        PHDBG_INFO("Hal4:Open Failed");
        /*Call upper layer's Open Cb with error status*/
        if(NULL != pUpper_OpenCb)
        {
            /*Upper layer's Open Cb*/
            (*pUpper_OpenCb)(pUpper_Context,status);
        }
    }
    return;
}

/**
 *  The close callback function called by the HCI when close  sequence is
 *  completed or if there is an error in closing.
 *  It is passed as a parameter to HCI when calling HCI Release.
 */
static void phHal4Nfc_CloseComplete(
                                    phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                                    void *pInfo
                                    )
{
    NFCSTATUS   status= ((phNfc_sCompletionInfo_t *)pInfo)->status;
    pphHal4Nfc_GenCallback_t pUpper_CloseCb;
    void                    *pUpper_Context;
    uint8_t                 RemoteDevNumber = 0;
    pUpper_CloseCb = Hal4Ctxt->sUpperLayerInfo.pUpperCloseCb;
    pUpper_Context = Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt;
    /*Update state*/
    Hal4Ctxt->Hal4CurrentState = Hal4Ctxt->Hal4NextState;
    Hal4Ctxt->Hal4NextState = eHal4StateInvalid;
    /*If Closed successfully*/
    if(NFCSTATUS_SUCCESS == status)
    {
        Hal4Ctxt->psHciHandle = NULL;
        /*Free all heap allocations*/
        phOsalNfc_FreeMemory((void *)Hal4Ctxt->pHal4Nfc_LayerCfg);
        Hal4Ctxt->pHal4Nfc_LayerCfg = NULL;
        /*Free ADD context info*/
        if(NULL != Hal4Ctxt->psADDCtxtInfo)
        {
            while(RemoteDevNumber < MAX_REMOTE_DEVICES)
            {
                if(NULL != Hal4Ctxt->rem_dev_list[RemoteDevNumber])
                {
                    phOsalNfc_FreeMemory((void *)
                                    (Hal4Ctxt->rem_dev_list[RemoteDevNumber]));
                    Hal4Ctxt->rem_dev_list[RemoteDevNumber] = NULL;
                }
                RemoteDevNumber++;
            }
            Hal4Ctxt->psADDCtxtInfo->nbr_of_devices = 0;
            phOsalNfc_FreeMemory(Hal4Ctxt->psADDCtxtInfo);
        }/*if(NULL != Hal4Ctxt->psADDCtxtInfo)*/
        /*Free Trcv context info*/
        if(NULL != Hal4Ctxt->psTrcvCtxtInfo)
        {
            if(NULL != Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.buffer)
            {
                    phOsalNfc_FreeMemory(
                        Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.buffer
                        );
            }
            if((NULL == Hal4Ctxt->sTgtConnectInfo.psConnectedDevice) 
                && (NULL != Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData))
            {
                phOsalNfc_FreeMemory(Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData);
            }
            phOsalNfc_FreeMemory(Hal4Ctxt->psTrcvCtxtInfo);
        }/*if(NULL != Hal4Ctxt->psTrcvCtxtInfo)*/
        /*Free Hal context and Hardware reference*/
        gpphHal4Nfc_Hwref->hal_context = NULL;
        gpphHal4Nfc_Hwref = NULL;
        phOsalNfc_FreeMemory((void *)Hal4Ctxt);
    }/* if(NFCSTATUS_SUCCESS == status)*/
    /*Call Upper layer's Close Cb with status*/
    (*pUpper_CloseCb)(pUpper_Context,status);
    return;
}


/*
* For configuring the various layers during the Initialization call
*
*
*/
static
NFCSTATUS
phHal4Nfc_Configure_Layers(
                        phNfcLayer_sCfg_t       **pphLayer
                        )
{
    uint8_t index = HAL4_LAYERS - 1;
    uint8_t i = 0;
    NFCSTATUS status = NFCSTATUS_SUCCESS ;
    PHDBG_INFO("Hal4:Configuring layers");
    *pphLayer = (phNfcLayer_sCfg_t *) phOsalNfc_GetMemory(
                            sizeof(phNfcLayer_sCfg_t) * HAL4_LAYERS);

    if( NULL == *pphLayer)
    {
        status = PHNFCSTVAL(CID_NFC_HAL,
                    NFCSTATUS_INSUFFICIENT_RESOURCES);/*Memory allocation error*/
    }
    else
    {

        (void)memset((void *)*pphLayer,0,(
                                sizeof(phNfcLayer_sCfg_t) * HAL4_LAYERS));

        for(i=0 ; i < HAL4_LAYERS ;i++, index-- )
        {
            (*pphLayer + i)->layer_index = index;
            switch(index)
            {
                case LAYER_HCI: /*Configure Hci*/
                {
                    (*pphLayer+i)->layer_name  =(uint8_t *) "Hci";
                    (*pphLayer+i)->layer_registry  = NULL;
                    (*pphLayer+i)->layer_next  =
                                    (((phNfcLayer_sCfg_t *)*pphLayer) + i + 1);
                    break;
                }
                case LAYER_LLC:/*Configure LLC*/
                {
                    (*pphLayer+i)->layer_registry  = phLlcNfc_Register;
                    (*pphLayer+i)->layer_name  = (uint8_t *)"Llc";
                    (*pphLayer+i)->layer_next  =
                                    (((phNfcLayer_sCfg_t *)*pphLayer) + i + 1);
                    break;
                }
                case LAYER_DAL: /*Configure the DAL*/
                {
                    (*pphLayer+i)->layer_registry  = phDal4Nfc_Register;
                    (*pphLayer+i)->layer_name  = (uint8_t *)"Dal";
                    (*pphLayer+i)->layer_next  = NULL ;
                    break;
                }
                default:
                    break;
            } /* End of Switch */
        }   /* End of For Loop */
    }   /* End of NULL Check */

    return status ;
}



#ifdef ANDROID

#define LOG_TAG "NFC-HCI"

#include <utils/Log.h>
#include <dlfcn.h>

#define FW_PATH "/system/vendor/firmware/libpn544_fw.so"

const unsigned char *nxp_nfc_full_version = NULL;
const unsigned char *nxp_nfc_fw = NULL;

int dlopen_firmware() {
    void *p;

    void *handle = dlopen(FW_PATH, RTLD_NOW);
    if (handle == NULL) {
        ALOGE("Could not open %s", FW_PATH);
        return -1;
    }

    p = dlsym(handle, "nxp_nfc_full_version");
    if (p == NULL) {
        ALOGE("Could not link nxp_nfc_full_version");
        return -1;
    }
    nxp_nfc_full_version = (unsigned char *)p;

    p = dlsym(handle, "nxp_nfc_fw");
    if (p == NULL) {
        ALOGE("Could not link nxp_nfc_fw");
        return -1;
    }
    nxp_nfc_fw = (unsigned char *)p;

    return 0;
}
#endif

/**
 *  The open function called by the upper HAL when HAL4 is to be opened
 *  (initialized).
 *
 */
NFCSTATUS phHal4Nfc_Open(
                         phHal_sHwReference_t       *psHwReference,
                         phHal4Nfc_InitType_t        InitType,
                         pphHal4Nfc_GenCallback_t    pOpenCallback,
                         void                       *pContext
                         )
{
    NFCSTATUS openRetVal = NFCSTATUS_SUCCESS;
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL;
    phHciNfc_Init_t   eHciInitType = (phHciNfc_Init_t)InitType;
    /*Set Default Clock settings once*/
    static phHal_sHwConfig_t sHwConfig = {
        {0},
        NXP_DEFAULT_CLK_REQUEST,
        NXP_DEFAULT_INPUT_CLK
        };
    /*NULL checks*/
    if(NULL == psHwReference || NULL == pOpenCallback)
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        openRetVal = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_INVALID_PARAMETER);
    }
    else if(NULL != gpphHal4Nfc_Hwref)
    {
        /*Hal4 context is open or open in progress ,return Ctxt already open*/
        openRetVal =  PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_ALREADY_INITIALISED);
    }
    else/*Do an initialization*/
    { 
#ifdef ANDROID
        dlopen_firmware();
#endif

        /*If hal4 ctxt in Hwreference is NULL create a new context*/
        if(NULL == ((phHal_sHwReference_t *)psHwReference)->hal_context)
        {
            Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)
                                phOsalNfc_GetMemory((uint32_t)sizeof(
                                                        phHal4Nfc_Hal4Ctxt_t)
                                                        );
            ((phHal_sHwReference_t *)psHwReference)->hal_context = Hal4Ctxt;
        }
        else/*Take context from Hw reference*/
        {
            Hal4Ctxt = ((phHal_sHwReference_t *)psHwReference)->hal_context;
        }
        if(NULL == Hal4Ctxt)
        {
            openRetVal = PHNFCSTVAL(CID_NFC_HAL,
                        NFCSTATUS_INSUFFICIENT_RESOURCES);
        }
        else
        {
            (void)memset((void *)Hal4Ctxt,
                        0,
                        ((uint32_t)sizeof(phHal4Nfc_Hal4Ctxt_t)));
            /* Configure layers if not configured */
            if( NULL == Hal4Ctxt->pHal4Nfc_LayerCfg )
            {
                openRetVal = phHal4Nfc_Configure_Layers(
                                                  &(Hal4Ctxt->pHal4Nfc_LayerCfg)
                                                  );
            }

            if( openRetVal == NFCSTATUS_SUCCESS )
            {
                /*update Next state*/
                Hal4Ctxt->Hal4NextState = (HCI_NFC_DEVICE_TEST == eHciInitType?
                                eHal4StateSelfTestMode:eHal4StateOpenAndReady);
                /*Store callback and context ,and set Default settings in Context*/
                Hal4Ctxt->sUpperLayerInfo.pUpperOpenCb = pOpenCallback;
                Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt = pContext;
                Hal4Ctxt->sTgtConnectInfo.EmulationState = NFC_EVT_DEACTIVATED;
                gpphHal4Nfc_Hwref = psHwReference;
                PHDBG_INFO("Hal4:Calling Hci-Init");
                openRetVal = phHciNfc_Initialise (
                                        (void *)&Hal4Ctxt->psHciHandle,
                                        psHwReference,
                                        eHciInitType,
                                        &sHwConfig,
                                        (pphNfcIF_Notification_CB_t)
                                            phHal4Nfc_LowerNotificationHandler,
                                        (void *)Hal4Ctxt,
                                        Hal4Ctxt->pHal4Nfc_LayerCfg
                                        );
                /*Hci Init did not succeed.free Resources and return*/
                if( (openRetVal != NFCSTATUS_SUCCESS)
                            && (PHNFCSTATUS (openRetVal) != NFCSTATUS_PENDING) )
                {                    
                    phOsalNfc_FreeMemory(Hal4Ctxt->pHal4Nfc_LayerCfg);
                    phOsalNfc_FreeMemory(Hal4Ctxt);
                    Hal4Ctxt = NULL;
                }
            }/*if( openRetVal == NFCSTATUS_SUCCESS )*/
            else/*Free the context*/
            {
                phOsalNfc_FreeMemory(Hal4Ctxt);
            }/*else*/
        }
    }
    return openRetVal;
}

/**  The I/O Control function allows the caller to use (vendor-) specific
*  functionality provided by the lower layer or by the hardware. */
NFCSTATUS phHal4Nfc_Ioctl(
                          phHal_sHwReference_t       *psHwReference,
                          uint32_t                    IoctlCode,
                          phNfc_sData_t              *pInParam,
                          phNfc_sData_t              *pOutParam,
                          pphHal4Nfc_IoctlCallback_t  pIoctlCallback,
                          void                       *pContext
                          )
{
    NFCSTATUS RetStatus = NFCSTATUS_FAILED;
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL;
    uint32_t config_type = 0;
    uint8_t ind = 0;
    /*NULL checks*/
    if((NULL == psHwReference)
        || (NULL == pIoctlCallback)
        )
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_INVALID_PARAMETER);
    }
    /*Only the Ioctls NFC_FW_DOWNLOAD_CHECK and NFC_FW_DOWNLOAD are allowed in
      the uninitialized state of HAL*/
    else if(NULL == psHwReference->hal_context)
    {
#ifdef FW_DOWNLOAD

#if  !defined (NXP_FW_INTEGRITY_VERIFY)
        if(NFC_FW_DOWNLOAD_CHECK == IoctlCode)
        {
            RetStatus = phDnldNfc_Run_Check(
                psHwReference                       
                );
        }
        else
#endif /* !defined (NXP_FW_INTEGRITY_VERIFY) */
        if((NFC_FW_DOWNLOAD == IoctlCode)
            &&(NULL == gpphHal4Nfc_Hwref))/*Indicates current state is shutdown*/
        {
            Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)
                phOsalNfc_GetMemory((uint32_t)sizeof(
                                                phHal4Nfc_Hal4Ctxt_t)
                                                );      
            if(NULL == Hal4Ctxt)
            {
                RetStatus = PHNFCSTVAL(CID_NFC_HAL,
                    NFCSTATUS_INSUFFICIENT_RESOURCES);
            }
            else
            {
                ((phHal_sHwReference_t *)psHwReference)->hal_context 
                    = Hal4Ctxt;
                (void)memset((void *)Hal4Ctxt,
                                 0,
                                   ((uint32_t)sizeof(phHal4Nfc_Hal4Ctxt_t)));               
                Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt = pContext;                                              
                Hal4Ctxt->sUpperLayerInfo.pUpperIoctlCb
                    = pIoctlCallback;/*Register upper layer callback*/
                Hal4Ctxt->sUpperLayerInfo.pIoctlOutParam = pOutParam;   
                /*Upgrade the firmware*/
                RetStatus = phDnldNfc_Upgrade (
                        psHwReference,
                        phHal4Nfc_DownloadComplete,
                        Hal4Ctxt
                        );
                if((NFCSTATUS_SUCCESS == RetStatus)
                    || (NFCSTATUS_PENDING != PHNFCSTATUS(RetStatus))
                    )
                {
                    phOsalNfc_FreeMemory(Hal4Ctxt);
                    ((phHal_sHwReference_t *)psHwReference)->hal_context = NULL;
                }
            }
        }
        else
#endif/*NFC_FW_DOWNLOAD*/
        {
            RetStatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_NOT_INITIALISED);     
        }
    }
    else/*Status is Initialised*/
    {
        /*Register upper layer context*/
        Hal4Ctxt = psHwReference->hal_context;
        Hal4Ctxt->sUpperLayerInfo.pIoctlOutParam = pOutParam;
        switch(IoctlCode)
        {
        /*Self test Ioctls*/
        case DEVMGMT_ANTENNA_TEST:
        case DEVMGMT_SWP_TEST:
        case DEVMGMT_NFCWI_TEST:
            if(eHal4StateSelfTestMode ==Hal4Ctxt->Hal4CurrentState)
            {
                RetStatus = phHciNfc_System_Test(
                    Hal4Ctxt->psHciHandle,
                    (void *)psHwReference,
                    IoctlCode ,
                    pInParam
                    );
            }
            break;
        /*PRBS Test*/
        case DEVMGMT_PRBS_TEST:
            RetStatus = phHciNfc_PRBS_Test(
                Hal4Ctxt->psHciHandle,
                (void *)psHwReference,
                IoctlCode ,
                pInParam
                );
            break;
        /*To Set Antenna Power Level*/
        case NFC_ANTENNA_CWG:
            if(eHal4StateSelfTestMode ==Hal4Ctxt->Hal4CurrentState)
            {
                RetStatus = phHciNfc_System_Configure (
                    Hal4Ctxt->psHciHandle,
                    (void *)psHwReference,
                    NFC_ANTENNA_CWG,
                    pInParam->buffer[0] /**Set Power Level*/
                    );

            }
            break;
        /*Not allowed when Init is complete*/
        case NFC_FW_DOWNLOAD_CHECK:
        case NFC_FW_DOWNLOAD:
            RetStatus = PHNFCSTVAL(CID_NFC_HAL,
                NFCSTATUS_BUSY);
            break;
        /*Gpio read*/
        case NFC_GPIO_READ:
            /* if(eHal4StateSelfTestMode == Hal4Ctxt->Hal4CurrentState) */
            {
                RetStatus = phHciNfc_System_Get_Info(
                    Hal4Ctxt->psHciHandle,
                    (void *)psHwReference,
                    IoctlCode ,
                    pOutParam->buffer
                    );
            }
            break;
        /*Used to Read Memory/Registers .3 bytes of Array passed form the 
          address to read from in MSB first format.*/
        case NFC_MEM_READ:
            {
                if((NULL != pInParam)
                    && (pInParam->length == 3))
                {
                    for( ind = 0; ind < 3; ind++ )
                    {
                        config_type = ((config_type << BYTE_SIZE )
                                        | (pInParam->buffer[ind] ));
                    }
                    RetStatus = phHciNfc_System_Get_Info(
                        Hal4Ctxt->psHciHandle,
                        (void *)psHwReference,
                        config_type ,
                        pOutParam->buffer
                        );
                }
                else
                {
                    RetStatus = PHNFCSTVAL(CID_NFC_HAL,
                        NFCSTATUS_INVALID_PARAMETER);
                }
            }
            break;
        /*Used to Write Memory/Registers .First 3 bytes of Array passed in MSB 
          first format form the address to write to.The 4th Byte is the 8 bit 
          value to be written to the address*/
        case NFC_MEM_WRITE:
            {
                if((NULL != pInParam)
                    && (pInParam->length == 4))
                {
                    for( ind = 0; ind < 3; ind++ )
                    {
                        config_type = ((config_type << BYTE_SIZE ) 
                                        | (pInParam->buffer[ind] ));
                    }
                    RetStatus = phHciNfc_System_Configure (
                        Hal4Ctxt->psHciHandle,
                        (void *)psHwReference,
                        config_type,
                        pInParam->buffer[3] /*config value*/
                        );
                }
                else
                {
                    RetStatus = PHNFCSTVAL(CID_NFC_HAL , 
                        NFCSTATUS_INVALID_PARAMETER);
                }
            }
            break;
        default:
            break;
        }
        if(NFCSTATUS_PENDING == RetStatus)/*Callback Pending*/
        {
            /*Register upper layer callback and context*/
            Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt = pContext;
            Hal4Ctxt->sUpperLayerInfo.pUpperIoctlCb= pIoctlCallback;
            /*Store the Ioctl code*/
            Hal4Ctxt->Ioctl_Type = IoctlCode;
        }
    }
    return RetStatus;
}


/**
 *  The close function called by the upper layer when HAL4 is to be closed
 *  (shutdown).  
 */
NFCSTATUS phHal4Nfc_Close(
                          phHal_sHwReference_t *psHwReference,
                          pphHal4Nfc_GenCallback_t pCloseCallback,
                          void *pContext
                          )
{
    NFCSTATUS closeRetVal = NFCSTATUS_SUCCESS;
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL;
    /*NULL checks*/
    if(NULL == psHwReference || NULL == pCloseCallback)
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
        closeRetVal = PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_INVALID_PARAMETER);
    }
    else if((NULL == psHwReference->hal_context)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4CurrentState 
                                               < eHal4StateSelfTestMode)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4NextState 
                                               == eHal4StateClosed))
    {
        /*return already closed*/
        closeRetVal= PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_NOT_INITIALISED);
    }
    else  /*Close the HAL*/
    {
        /*Get Hal4 context from Hw reference*/
        Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)((phHal_sHwReference_t *)
                                               psHwReference)->hal_context;
        /*Unregister Tag Listener*/
        if(NULL != Hal4Ctxt->psADDCtxtInfo)
        {
            Hal4Ctxt->sUpperLayerInfo.pTagDiscoveryNotification = NULL;
        }
        /*store Callback and Context*/
        Hal4Ctxt->sUpperLayerInfo.pUpperCloseCb = pCloseCallback;
        Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt = pContext;
        /*Call Hci Release*/
        PHDBG_INFO("Hal4:Calling Hci Release");
        closeRetVal =(NFCSTATUS)phHciNfc_Release(
                                    (void *)Hal4Ctxt->psHciHandle,
                                    psHwReference,
                                    (pphNfcIF_Notification_CB_t)
                                    phHal4Nfc_LowerNotificationHandler,
                                    (void *)Hal4Ctxt
                                    );
        /*Update Next state and exit*/
        if( PHNFCSTATUS (closeRetVal) == NFCSTATUS_PENDING )
        {
            Hal4Ctxt->Hal4NextState = eHal4StateClosed;
            Hal4Ctxt->sUpperLayerInfo.pTagDiscoveryNotification = NULL;
        }
        else
        {

        }
    }
    return closeRetVal;
}

/*Forcibly shutdown the HAl4.Frees all Resources in use by Hal4 before shutting
  down*/
void phHal4Nfc_Hal4Reset(
                         phHal_sHwReference_t *pHwRef,
                         void                 *pContext
                         )
{
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL;
    NFCSTATUS             closeRetVal = NFCSTATUS_SUCCESS;
    uint8_t               RemoteDevNumber = 0;
    if(pHwRef ==NULL)
    {
        closeRetVal = PHNFCSTVAL(CID_NFC_HAL , NFCSTATUS_INVALID_PARAMETER);
    }
    else if(pHwRef->hal_context != NULL)
    {
        /*Get the Hal context*/
        Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)pHwRef->hal_context;
        /*store the upper layer context*/
        Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt = pContext;
        Hal4Ctxt->Hal4NextState = eHal4StateClosed;
        Hal4Ctxt->sUpperLayerInfo.pTagDiscoveryNotification = NULL;
        /*Call Hci Release*/
        PHDBG_INFO("Hal4:Calling Hci Release");
        closeRetVal =(NFCSTATUS)phHciNfc_Release(
                                            (void *)Hal4Ctxt->psHciHandle,
                                            pHwRef,
                                            (pphNfcIF_Notification_CB_t)NULL,
                                            (void *)Hal4Ctxt
                                            );/*Clean up Hci*/
        Hal4Ctxt->Hal4CurrentState = eHal4StateClosed;
        phOsalNfc_FreeMemory((void *)Hal4Ctxt->pHal4Nfc_LayerCfg);
        Hal4Ctxt->pHal4Nfc_LayerCfg = NULL;
        /*Free ADD context*/
        if(NULL != Hal4Ctxt->psADDCtxtInfo)
        {
            Hal4Ctxt->sUpperLayerInfo.pTagDiscoveryNotification = NULL;
            while(RemoteDevNumber < MAX_REMOTE_DEVICES)
            {
                if(NULL != Hal4Ctxt->rem_dev_list[RemoteDevNumber])
                {
                    phOsalNfc_FreeMemory((void *)
                            (Hal4Ctxt->rem_dev_list[RemoteDevNumber]));
                    Hal4Ctxt->rem_dev_list[RemoteDevNumber] = NULL;
                }
                RemoteDevNumber++;
            }
            Hal4Ctxt->psADDCtxtInfo->nbr_of_devices = 0;
            phOsalNfc_FreeMemory(Hal4Ctxt->psADDCtxtInfo);
        }
        /*Free Trcv context*/
        if(NULL != Hal4Ctxt->psTrcvCtxtInfo)
        {
            if(NULL != Hal4Ctxt->psTrcvCtxtInfo->sLowerRecvData.buffer)
            {
                phOsalNfc_FreeMemory(Hal4Ctxt->psTrcvCtxtInfo
                                                    ->sLowerRecvData.buffer);
            }
            if((NULL == Hal4Ctxt->sTgtConnectInfo.psConnectedDevice) 
                && (NULL != Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData))
            {
                phOsalNfc_FreeMemory(Hal4Ctxt->psTrcvCtxtInfo->psUpperSendData);
            }
            phOsalNfc_FreeMemory(Hal4Ctxt->psTrcvCtxtInfo);
        }
        phOsalNfc_FreeMemory(Hal4Ctxt);/*Free the context*/
        pHwRef->hal_context = NULL;
        gpphHal4Nfc_Hwref = NULL;
    }
    else
    {
        /*Hal4 Context is already closed.Return Success*/
    }
    /*Reset Should always return Success*/
    if(closeRetVal != NFCSTATUS_SUCCESS)
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
    }
    return;
}

/**
 *  \if hal
 *   \ingroup grp_hal_common
 *  \else
 *   \ingroup grp_mw_external_hal_funcs
 *  \endif
 *
 *  Retrieves the capabilities of the device represented by the Hardware
 *  Reference parameter.
 *  The HW, SW versions, the MTU and other mandatory information are located
 *  inside the pDevCapabilities parameter.
 */
NFCSTATUS phHal4Nfc_GetDeviceCapabilities(
                            phHal_sHwReference_t          *psHwReference,
                            phHal_sDeviceCapabilities_t   *psDevCapabilities,
                            void                          *pContext
                            )
{
    NFCSTATUS retstatus = NFCSTATUS_SUCCESS;
    /*NULL checks*/
    if(psDevCapabilities == NULL || psHwReference == NULL || pContext == NULL)
    {
        retstatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_INVALID_PARAMETER);
    }
    /*Check for Initialized state*/
    else if((NULL == psHwReference->hal_context)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4CurrentState 
                                               < eHal4StateOpenAndReady)
                        || (((phHal4Nfc_Hal4Ctxt_t *)
                                psHwReference->hal_context)->Hal4NextState 
                                               == eHal4StateClosed))
    {
        retstatus = PHNFCSTVAL(CID_NFC_HAL ,NFCSTATUS_NOT_INITIALISED);
    }
    else/*Provide Device capabilities and Version Info to the caller*/
    {
        (void)memcpy((void *)psDevCapabilities,
            (void *)&(psHwReference->device_info),
            sizeof(phHal_sDeviceCapabilities_t));
        psDevCapabilities->ReaderSupProtocol.Felica         = TRUE;
        psDevCapabilities->ReaderSupProtocol.ISO14443_4A    = TRUE;
        psDevCapabilities->ReaderSupProtocol.ISO14443_4B    = TRUE;
        psDevCapabilities->ReaderSupProtocol.ISO15693       = TRUE;
        psDevCapabilities->ReaderSupProtocol.Jewel          = TRUE;
        psDevCapabilities->ReaderSupProtocol.MifareStd      = TRUE;
        psDevCapabilities->ReaderSupProtocol.MifareUL       = TRUE;
        psDevCapabilities->ReaderSupProtocol.NFC            = TRUE;
        psDevCapabilities->EmulationSupProtocol.Felica      = FALSE;
        psDevCapabilities->EmulationSupProtocol.ISO14443_4A = FALSE;
        psDevCapabilities->EmulationSupProtocol.ISO14443_4B = FALSE;
        psDevCapabilities->EmulationSupProtocol.ISO15693    = FALSE;
        psDevCapabilities->EmulationSupProtocol.Jewel       = FALSE;
        psDevCapabilities->EmulationSupProtocol.MifareStd   = FALSE;
        psDevCapabilities->EmulationSupProtocol.MifareUL    = FALSE;
        psDevCapabilities->EmulationSupProtocol.NFC         = TRUE;
        psDevCapabilities->hal_version = (
                    (((PH_HAL4NFC_INTERFACE_VERSION << BYTE_SIZE)
                      |(PH_HAL4NFC_INTERFACE_REVISION)<<BYTE_SIZE)
                      |(PH_HAL4NFC_INTERFACE_PATCH)<<BYTE_SIZE)
                      |PH_HAL4NFC_INTERAFECE_BUILD
                      );
    }
    return retstatus;
}

/*
 * Handles all notifications received from HCI layer.
 *
 */
static void phHal4Nfc_LowerNotificationHandler(
                                    void    *pContext,
                                    void    *pHwRef,
                                    uint8_t  type,
                                    void     *pInfo
                                    )
{
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL;
    if((NULL == pInfo) || (NULL == pHwRef)
        || (NULL == pContext))
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
    }
    else
    {
        /*A copy of hardware reference is maintained in HAL for comparing passed
          and returned context.Set to NULL after a Shutdown*/
        if(NULL != gpphHal4Nfc_Hwref)/*Get context from Hw ref*/
        {
            Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)gpphHal4Nfc_Hwref->hal_context;
            if(NFC_INVALID_RELEASE_TYPE == Hal4Ctxt->sTgtConnectInfo.ReleaseType)
            {
                Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)pContext;
                gpphHal4Nfc_Hwref = (phHal_sHwReference_t *)pHwRef;
            }
        }
        else/*No Copy of Hw ref in HAL.Copy both Hwref and Hal context passed 
             by Hci*/
        {
            Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)pContext;
            gpphHal4Nfc_Hwref = (phHal_sHwReference_t *)pHwRef;
        }
        /*Check the type of notification received from Hci and handle it
          accordingly*/
        switch(type)
        {
            case NFC_NOTIFY_INIT_COMPLETED:
            case NFC_NOTIFY_INIT_FAILED:
                phHal4Nfc_OpenComplete(Hal4Ctxt,pInfo);
                break;
            case NFC_IO_SUCCESS:
            case NFC_IO_ERROR:
                phHal4Nfc_IoctlComplete(Hal4Ctxt,pInfo);
                break;
            case NFC_NOTIFY_RESULT:
                phHal4Nfc_SelfTestComplete(Hal4Ctxt,pInfo);
                break;
            case NFC_NOTIFY_DEINIT_COMPLETED:
            case NFC_NOTIFY_DEINIT_FAILED:
                phHal4Nfc_CloseComplete(Hal4Ctxt,pInfo);
                break;
            case NFC_NOTIFY_POLL_ENABLED:
            case NFC_NOTIFY_POLL_DISABLED:
            case NFC_NOTIFY_POLL_RESTARTED:
            case NFC_NOTIFY_CONFIG_ERROR:
            case NFC_NOTIFY_CONFIG_SUCCESS:
                phHal4Nfc_ConfigureComplete(Hal4Ctxt,pInfo,type);
                break;
            case NFC_NOTIFY_TARGET_DISCOVERED:
            case NFC_NOTIFY_DISCOVERY_ERROR:
                phHal4Nfc_TargetDiscoveryComplete(Hal4Ctxt,pInfo);
                break;
            case NFC_NOTIFY_TARGET_REACTIVATED:
                phHal4Nfc_ReactivationComplete(Hal4Ctxt,pInfo);
                break;
            case NFC_NOTIFY_EVENT:
                PHDBG_INFO("Hal4:Calling Event callback");
                phHal4Nfc_HandleEvent(Hal4Ctxt,pInfo);
                break;
            case NFC_NOTIFY_TARGET_CONNECTED:
                PHDBG_INFO("Hal4:Calling Hal4 Connect complete");
                phHal4Nfc_ConnectComplete(Hal4Ctxt,pInfo);
                break;

            case NFC_NOTIFY_TARGET_DISCONNECTED:
            {
                PHDBG_INFO("Hal4:Target Disconnected");
                if(Hal4Ctxt->Hal4NextState == eHal4StatePresenceCheck)
                {
                    phHal4Nfc_PresenceChkComplete(Hal4Ctxt,pInfo);
                }
                else
                {
                    phHal4Nfc_DisconnectComplete(Hal4Ctxt,pInfo);
                }
                break;
            }
            case NFC_NOTIFY_TRANSCEIVE_COMPLETED:
            case NFC_NOTIFY_TRANSCEIVE_ERROR    :
                PHDBG_INFO("Hal4:Transceive Callback");
                if(NULL != Hal4Ctxt->psTrcvCtxtInfo)
                {
#ifdef TRANSACTION_TIMER
                    if(Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                        != PH_OSALNFC_INVALID_TIMER_ID)
                    {
                        phOsalNfc_Timer_Stop(
                            Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                            );
                        phOsalNfc_Timer_Delete(
                            Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                            );
                        Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                                    = PH_OSALNFC_INVALID_TIMER_ID;
                    }
#endif /*TRANSACTION_TIMER*/
                    phHal4Nfc_TransceiveComplete(Hal4Ctxt,pInfo);
                }
                break;
            case NFC_NOTIFY_SEND_COMPLETED   :
                PHDBG_INFO("Hal4:NfcIp1 Send Callback");
                if(NULL != Hal4Ctxt->psTrcvCtxtInfo)
                {
                    phHal4Nfc_SendCompleteHandler(Hal4Ctxt,pInfo);
                }
                break;
            case NFC_NOTIFY_TRANSACTION  :
                phHal4Nfc_HandleEmulationEvent(Hal4Ctxt,pInfo);
                break;
            case NFC_NOTIFY_RECV_ERROR    :
            case NFC_NOTIFY_RECV_EVENT    :
                PHDBG_INFO("Hal4:Receive Event");
                if(NULL != Hal4Ctxt->psTrcvCtxtInfo)
                {
                    if(Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                        != PH_OSALNFC_INVALID_TIMER_ID)
                    {
                        phOsalNfc_Timer_Stop(
                            Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                            );
                        phOsalNfc_Timer_Delete(
                            Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                            );
                        Hal4Ctxt->psTrcvCtxtInfo->TransactionTimerId
                            = PH_OSALNFC_INVALID_TIMER_ID;
                    }
                }
                phHal4Nfc_RecvCompleteHandler(Hal4Ctxt,pInfo);
                break;
            case NFC_NOTIFY_TARGET_PRESENT:
                phHal4Nfc_PresenceChkComplete(Hal4Ctxt,pInfo);
                break;
            case NFC_NOTIFY_DEVICE_ERROR:
            {
                NFCSTATUS status = NFCSTATUS_BOARD_COMMUNICATION_ERROR;
                pphHal4Nfc_GenCallback_t pUpper_OpenCb
                                                = Hal4Ctxt->sUpperLayerInfo.pUpperOpenCb;
                void                   *pUpper_Context
                                            = Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt;
                static phHal4Nfc_NotificationInfo_t uNotificationInfo;
                if(NULL != Hal4Ctxt->sUpperLayerInfo.pDefaultEventHandler)
                {                    
                    Hal4Ctxt->Hal4NextState = eHal4StateInvalid;
                    Hal4Ctxt->sUpperLayerInfo.pDefaultEventHandler(
                        Hal4Ctxt->sUpperLayerInfo.DefaultListenerCtxt,
                        NFC_EVENT_NOTIFICATION,
                        uNotificationInfo,
                        NFCSTATUS_BOARD_COMMUNICATION_ERROR
                        );
                }
                else if (( eHal4StateSelfTestMode == Hal4Ctxt->Hal4NextState )
                    || ( eHal4StateOpenAndReady == Hal4Ctxt->Hal4NextState ) )
                {
                    Hal4Ctxt->Hal4CurrentState = eHal4StateClosed;
                    Hal4Ctxt->Hal4NextState = eHal4StateInvalid;
                    (void)phHciNfc_Release((void *)Hal4Ctxt->psHciHandle,
                                              pHwRef, (pphNfcIF_Notification_CB_t)NULL,
                                               (void *)Hal4Ctxt);/*Clean up Hci*/
                    Hal4Ctxt->psHciHandle = NULL;
                    phOsalNfc_FreeMemory((void *)Hal4Ctxt->pHal4Nfc_LayerCfg);
                    Hal4Ctxt->pHal4Nfc_LayerCfg = NULL;
                    phOsalNfc_FreeMemory((void *)Hal4Ctxt);
                    gpphHal4Nfc_Hwref->hal_context = NULL;
                    gpphHal4Nfc_Hwref = NULL;
                    PHDBG_INFO("Hal4:Open Failed");
                    /*Call upper layer's Open Cb with error status*/
                    if(NULL != pUpper_OpenCb)
                    {
                        /*Upper layer's Open Cb*/
                        (*pUpper_OpenCb)(pUpper_Context,status);
                    }
                }
                else
                {
                    Hal4Ctxt->Hal4NextState = eHal4StateInvalid;
                    phOsalNfc_RaiseException(phOsalNfc_e_UnrecovFirmwareErr,1);
                }
                break;
            }
            case NFC_NOTIFY_CONNECT_FAILED:
            case NFC_NOTIFY_DISCONNECT_FAILED:   
            /*Generic Error type received from Hci.Handle the error based on 
              Hal4 next state and which past callback was Pending*/
            case NFC_NOTIFY_ERROR:
            {
                PHDBG_WARNING("Hal4:Error Notification from HCI");
                switch(Hal4Ctxt->Hal4NextState)
                {
                    case eHal4StateClosed:
                        phHal4Nfc_CloseComplete(Hal4Ctxt,pInfo);
                        break;
                    case eHal4StateSelfTestMode:
                        phHal4Nfc_SelfTestComplete(Hal4Ctxt,pInfo);
                        break;
                    case eHal4StateConfiguring:
                        phHal4Nfc_ConfigureComplete(Hal4Ctxt,pInfo,type);
                        break;
                    case eHal4StateTargetDiscovered:
                    case eHal4StateTargetActivate:
                    {
                        if(NULL != Hal4Ctxt->sTgtConnectInfo.pUpperConnectCb)
                        {
                            if(NULL == Hal4Ctxt->sTgtConnectInfo.psConnectedDevice)
                            {
                                 phHal4Nfc_ConfigureComplete(Hal4Ctxt,pInfo,type);
                            }
                            else
                            {
                                phHal4Nfc_ConnectComplete(Hal4Ctxt,pInfo);
                            }
                        }
                        else
                        {
                            phHal4Nfc_TargetDiscoveryComplete(Hal4Ctxt,pInfo);
                        }
                        break;
                    }
                    case eHal4StateTargetConnected:
                        phHal4Nfc_ConnectComplete(Hal4Ctxt,pInfo);
                        break;
                    case eHal4StateOpenAndReady:
                        phHal4Nfc_DisconnectComplete(Hal4Ctxt,pInfo);
                        break;
                    case eHal4StatePresenceCheck:
                        phHal4Nfc_PresenceChkComplete(Hal4Ctxt,pInfo);
                        break;
                    default:
                        PHDBG_WARNING("Unknown Error notification");
                        break;
                }
                break;
            }/*End of switch(Hal4Ctxt->Hal4State)*/
            default:
                phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
                break;
        }/*End of switch(type)*/
    }
    return;
}


/*Event handler for HAL-HCI interface*/
static void phHal4Nfc_HandleEvent(
                       phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                       void *pInfo
                       )
{
    phHal_sEventInfo_t *psEventInfo = (phHal_sEventInfo_t *)pInfo;
    static phNfc_sNotificationInfo_t sNotificationInfo;
    phHal4Nfc_NotificationInfo_t uNotificationInfo = {NULL};
    NFCSTATUS RetStatus = NFCSTATUS_FAILED;
    /*Check if Hal4 Close has already been called*/
    if(eHal4StateClosed != Hal4Ctxt->Hal4NextState)
    {
        switch(psEventInfo->eventType)
        {
        case NFC_EVT_ACTIVATED:/*Target Activated*/
        {
            if(psEventInfo->eventHost == phHal_eHostController)
            {
                switch(psEventInfo->eventSource)
                {
                    case phHal_eNfcIP1_Target:
                        phHal4Nfc_P2PActivateComplete(Hal4Ctxt,pInfo);
                        break;
                    case phHal_eISO14443_A_PICC:
                    case phHal_eISO14443_B_PICC:
                        sNotificationInfo.info = psEventInfo;
                        sNotificationInfo.status = NFCSTATUS_SUCCESS;
                        sNotificationInfo.type = NFC_EVENT_NOTIFICATION;
                        pInfo = &sNotificationInfo;
                        phHal4Nfc_HandleEmulationEvent(Hal4Ctxt,pInfo);
                        break;
                    default:
                        break;
                }           
            }
        }
            break;
        case NFC_EVT_DEACTIVATED:/*Target Deactivated*/
        {
            if(psEventInfo->eventHost == phHal_eHostController)
            {
                switch(psEventInfo->eventSource)
                {
                case phHal_eNfcIP1_Target:
                    phHal4Nfc_HandleP2PDeActivate(Hal4Ctxt,pInfo);
                    break;
                case phHal_eISO14443_A_PICC:
                case phHal_eISO14443_B_PICC:
                    sNotificationInfo.info = psEventInfo;
                    sNotificationInfo.status = NFCSTATUS_SUCCESS;
                    sNotificationInfo.type = NFC_EVENT_NOTIFICATION;
                    pInfo = &sNotificationInfo;
                    phHal4Nfc_HandleEmulationEvent(Hal4Ctxt,pInfo);
                    break;
                default:
                    break;
                }
            }
        }
            break;
        /*Set Protection Event*/
        case NFC_EVT_PROTECTED:
        {
#ifdef IGNORE_EVT_PROTECTED
            /*Ignore_Event_Protected is set to false during Field Off event and 
              Set protection Configuration.After a NFC_EVT_PROTECTED is received
              once all subsequent NFC_EVT_PROTECTED events are ignored*/
            if(FALSE == Hal4Ctxt->Ignore_Event_Protected)
            {
                Hal4Ctxt->Ignore_Event_Protected = TRUE;
#endif/*#ifdef IGNORE_EVT_PROTECTED*/
                sNotificationInfo.info = psEventInfo;
                sNotificationInfo.status = NFCSTATUS_SUCCESS;
                sNotificationInfo.type = NFC_EVENT_NOTIFICATION;
                pInfo = &sNotificationInfo;            
                phHal4Nfc_HandleEmulationEvent(Hal4Ctxt,pInfo);
#ifdef IGNORE_EVT_PROTECTED
            }
#endif/*#ifdef IGNORE_EVT_PROTECTED*/
            break;
        }
        /*NFC_UICC_RDPHASES_DEACTIVATE_REQ*/
        case NFC_UICC_RDPHASES_DEACTIVATE_REQ:
        {
            if(NULL != gpphHal4Nfc_Hwref)
            {
                gpphHal4Nfc_Hwref->uicc_rdr_active = FALSE;
            }
            break;
        }
        case NFC_UICC_RDPHASES_ACTIVATE_REQ:
        {
            if(NULL != gpphHal4Nfc_Hwref)
            {
                gpphHal4Nfc_Hwref->uicc_rdr_active = TRUE;
            }
            /*If a NFC_UICC_RDPHASES_ACTIVATE_REQ is received before a configure
             discovery,then create a ADD context info*/
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
            if(NULL != Hal4Ctxt->psADDCtxtInfo)
            {
                Hal4Ctxt->psADDCtxtInfo->sADDCfg.PollDevInfo.PollEnabled 
                                |= psEventInfo->eventInfo.rd_phases;
                /*Configure HCI Discovery*/ 
                RetStatus = phHciNfc_Config_Discovery(
                    (void *)Hal4Ctxt->psHciHandle,
                    gpphHal4Nfc_Hwref,
                    &(Hal4Ctxt->psADDCtxtInfo->sADDCfg)
                    ); 
                Hal4Ctxt->Hal4NextState = (NFCSTATUS_PENDING == RetStatus?
                                                eHal4StateConfiguring:
                                                Hal4Ctxt->Hal4NextState);
            }
            break;
        }   
        /*Call Default Event handler for these Events*/
        case NFC_INFO_TXLDO_OVERCUR:
        case NFC_INFO_MEM_VIOLATION:
        case NFC_INFO_TEMP_OVERHEAT:
        case NFC_INFO_LLC_ERROR:
        {
            sNotificationInfo.info = psEventInfo;
            sNotificationInfo.status = NFCSTATUS_SUCCESS;
            sNotificationInfo.type = NFC_EVENT_NOTIFICATION;
            pInfo = &sNotificationInfo;
            PHDBG_INFO("Hal4:Exception events");
            if(NULL != Hal4Ctxt->sUpperLayerInfo.pDefaultEventHandler)
            {
                /*Pass on Event notification info from Hci to Upper layer*/
                uNotificationInfo.psEventInfo = psEventInfo;
                Hal4Ctxt->sUpperLayerInfo.pDefaultEventHandler(
                    Hal4Ctxt->sUpperLayerInfo.DefaultListenerCtxt,
                    sNotificationInfo.type,
                    uNotificationInfo,
                    NFCSTATUS_SUCCESS
                    );
            }
            break;
        }
        /*Call emulation Event handler fto handle these Events*/
        case NFC_EVT_TRANSACTION:
        case NFC_EVT_START_OF_TRANSACTION:
        case NFC_EVT_END_OF_TRANSACTION:
        case NFC_EVT_CONNECTIVITY:   
        case NFC_EVT_OPERATION_ENDED:
        case NFC_EVT_MIFARE_ACCESS:
        case NFC_EVT_APDU_RECEIVED:
        case NFC_EVT_EMV_CARD_REMOVAL:
            sNotificationInfo.info = psEventInfo;
            sNotificationInfo.status = NFCSTATUS_SUCCESS;
            sNotificationInfo.type = NFC_EVENT_NOTIFICATION;
            pInfo = &sNotificationInfo;
            PHDBG_INFO("Hal4:Event transaction\n");
            phHal4Nfc_HandleEmulationEvent(Hal4Ctxt,pInfo);
            break;
        case NFC_EVT_FIELD_ON:
            Hal4Ctxt->psEventInfo = sNotificationInfo.info = psEventInfo;
            sNotificationInfo.status = NFCSTATUS_SUCCESS;
            sNotificationInfo.type = NFC_EVENT_NOTIFICATION;
            pInfo = &sNotificationInfo;
            PHDBG_INFO("Hal4:Event Field ON\n");
            phHal4Nfc_HandleEmulationEvent(Hal4Ctxt,pInfo);        
            break;
        case NFC_EVT_FIELD_OFF:
    #ifdef IGNORE_EVT_PROTECTED
            Hal4Ctxt->Ignore_Event_Protected = FALSE;
    #endif/*#ifdef IGNORE_EVT_PROTECTED*/
            Hal4Ctxt->psEventInfo = sNotificationInfo.info = psEventInfo;
            sNotificationInfo.status = NFCSTATUS_SUCCESS;
            sNotificationInfo.type = NFC_EVENT_NOTIFICATION;
            pInfo = &sNotificationInfo;
            PHDBG_INFO("Hal4:Event Field OFF\n");
            phHal4Nfc_HandleEmulationEvent(Hal4Ctxt,pInfo); 
            break;
        default:
            PHDBG_WARNING("Hal4:Unhandled Event type received");
            break;
        }/*End of switch*/
    }/*if(eHal4StateClosed != Hal4Ctxt->Hal4NextState)*/
    return;
}


/*Callback handler for Self Test Ioctl completion*/
static void phHal4Nfc_SelfTestComplete(
                                       phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                                       void *pInfo
                                       )
{
    NFCSTATUS status = NFCSTATUS_FAILED;
    phNfc_sData_t *SelfTestResults
        = (phNfc_sData_t *)(((phNfc_sCompletionInfo_t *)pInfo)->info);
    pphHal4Nfc_IoctlCallback_t pUpper_IoctlCb
        = Hal4Ctxt->sUpperLayerInfo.pUpperIoctlCb;
    void  *pUpper_Context = Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt;
    /*check for Success*/
    if(( DEVMGMT_SWP_TEST == Hal4Ctxt->Ioctl_Type )
        || ( DEVMGMT_ANTENNA_TEST == Hal4Ctxt->Ioctl_Type ))
    {
        status = NFCSTATUS_SUCCESS;
    }
    else if((SelfTestResults->length > 0) && (0 == SelfTestResults->buffer[0]))
    {
        status = NFCSTATUS_SUCCESS;
    }
    else
    {
        if (NULL != pInfo)
        {
            status = ((phNfc_sCompletionInfo_t *)pInfo)->status;
        }
    }

    /*Copy response buffer and length*/
    (void)memcpy(Hal4Ctxt->sUpperLayerInfo.pIoctlOutParam->buffer,
                 SelfTestResults->buffer,
                 SelfTestResults->length);
    Hal4Ctxt->sUpperLayerInfo.pIoctlOutParam->length
                                        = SelfTestResults->length;
    /*Call registered Ioctl callback*/
    (*pUpper_IoctlCb)(
                pUpper_Context,
                Hal4Ctxt->sUpperLayerInfo.pIoctlOutParam,
                status
                );
    return;
}


static void phHal4Nfc_IoctlComplete(
                                    phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                                    void *pInfo
                                    )
{
    /*Copy status*/
    NFCSTATUS status = (((phNfc_sCompletionInfo_t *)pInfo)->status);
    pphHal4Nfc_IoctlCallback_t pUpper_IoctlCb 
                                    = Hal4Ctxt->sUpperLayerInfo.pUpperIoctlCb;
#ifdef MERGE_SAK_SW2
    pphHal4Nfc_GenCallback_t pConfigCallback = 
        Hal4Ctxt->sUpperLayerInfo.pConfigCallback;
#endif/*#ifdef MERGE_SAK_SW2*/
    void  *pUpper_Context = Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt;
    Hal4Ctxt->sUpperLayerInfo.pUpperIoctlCb = NULL;    
#ifdef MERGE_SAK_SW1 /*Software workaround 1*/
    if(eHal4StateOpenAndReady == Hal4Ctxt->Hal4NextState)
    {
        Hal4Ctxt->Hal4CurrentState = Hal4Ctxt->Hal4NextState;
        Hal4Ctxt->Hal4NextState = eHal4StateInvalid;
        /*Upper layer's Open Cb*/
        (*Hal4Ctxt->sUpperLayerInfo.pUpperOpenCb)(
            Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,
            NFCSTATUS_SUCCESS
            );
    }
#endif/*#ifdef MERGE_SAK_SW1*/    
#ifdef MERGE_SAK_SW2 /*Software workaround 2*/
    else if((eHal4StateConfiguring == Hal4Ctxt->Hal4NextState)
            &&(NULL != pConfigCallback))
    {
        Hal4Ctxt->sUpperLayerInfo.pConfigCallback = NULL;
        Hal4Ctxt->Hal4NextState = eHal4StateInvalid;
        (*pConfigCallback)(
            Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt,status
            );
    }
    else
#endif/*#ifdef MERGE_SAK_SW2*/
    {
        /*for NFC_MEM_READ and NFC_GPIO_READ ,provide one Byte Response*/
        if ((NFC_MEM_READ == Hal4Ctxt->Ioctl_Type)
            || (NFC_GPIO_READ == Hal4Ctxt->Ioctl_Type)
            )
        {
            Hal4Ctxt->sUpperLayerInfo.pIoctlOutParam->length 
                = sizeof (uint8_t);
        }
         /*Call registered Ioctl callback*/
        if(NULL != pUpper_IoctlCb)
        {
            (*pUpper_IoctlCb)(
                pUpper_Context,
                Hal4Ctxt->sUpperLayerInfo.pIoctlOutParam,
                status
                );
        }
    }
    return;
}

#ifdef FW_DOWNLOAD
/**Callback handler for Download completion*/
STATIC void phHal4Nfc_DownloadComplete(
                                void *pContext,
                                void *pHwRef,
                                uint8_t type,
                                void *pInfo
                                )
{
    phHal4Nfc_Hal4Ctxt_t *Hal4Ctxt = NULL;
    NFCSTATUS status = NFCSTATUS_FAILED;
    pphHal4Nfc_IoctlCallback_t pUpper_DnldCb = NULL;
    phNfc_sData_t *pIoctlOutParam = NULL;
    phHal_sHwReference_t *psHwRef = NULL;
    void  *pUpper_Context = NULL;
    /*NULL checks*/
    if((NULL == pInfo) || (NULL == pHwRef) || (NULL == pContext))
    {
        phOsalNfc_RaiseException(phOsalNfc_e_PrecondFailed,1);
    }
    else
    {
        type = type;
        Hal4Ctxt = (phHal4Nfc_Hal4Ctxt_t *)pContext;
        /*Copy back stored context/callback for the upper layer*/
        pUpper_Context = Hal4Ctxt->sUpperLayerInfo.psUpperLayerCtxt;
        pIoctlOutParam = Hal4Ctxt->sUpperLayerInfo.pIoctlOutParam;
        pUpper_DnldCb = Hal4Ctxt->sUpperLayerInfo.pUpperIoctlCb;
        Hal4Ctxt->sUpperLayerInfo.pUpperIoctlCb = NULL;
        /*Copy download status*/
        status = (((phNfc_sCompletionInfo_t *)pInfo)->status);
        /*copy hw reference*/
        psHwRef = (phHal_sHwReference_t *)pHwRef;
        /*Free the temporary hal context used only for the sake of download*/
        phOsalNfc_FreeMemory(psHwRef->hal_context);
        psHwRef->hal_context = NULL;
        /*Call upper layer callback*/
        if(NULL != pUpper_DnldCb)
        {
            (*pUpper_DnldCb)(
                pUpper_Context,
                pIoctlOutParam,
                status
                );
        }
    }
    return;
}
#endif /*FW_DOWNLOAD*/

