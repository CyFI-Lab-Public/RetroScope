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


/**
* \file phHal4Nfc_Internal.h
* \brief HAL callback Function Prototypes
*
*  The HAL4.0 Internal header file
*
* Project: NFC-FRI-1.1 / HAL4.0
*
* $Date: Mon May 31 11:43:42 2010 $
* $Author: ing07385 $
* $Revision: 1.40 $
* $Aliases: NFC_FRI1.1_WK1023_R35_1 $
*
*/

/*@{*/
#ifndef PHHAL4NFC_INTERNAL_H
#define PHHAL4NFC_INTERNAL_H
/*@}*/

#include <phHciNfc.h>

/** 
*  \name HAL4
*
* File: \ref phHal4Nfc_Internal.h
*
*/

/*@{*/
#define PH_HAL4NFC_INTERNAL_FILEREVISION "$Revision: 1.40 $" /**< \ingroup grp_file_attributes */
#define PH_HAL4NFC_INTERNAL_FILEALIASES  "$Aliases: NFC_FRI1.1_WK1023_R35_1 $"     /**< \ingroup grp_file_attributes */
/*@}*/

/* -----------------Include files ---------------------------------------*/ 

/* ---------------- Macros ----------------------------------------------*/
#define LLCP_DISCON_CHANGES
#define PH_HAL4NFC_TRANSCEIVE_TIMEOUT        30000  /**<Transceive operation
                                                        on any target should be 
                                                        completed within this 
                                                        interval.Else the
                                                        operation is timed out*/

#define   PH_HAL4NFC_TGT_MERGE_ADDRESS          0x988BU
#define   PH_HAL4NFC_TGT_MERGE_SAK                0x00U 


/*---------------- Hal4 Internal Data Structures -------------------------*/
/**HAL4 states*/
typedef enum{
    eHal4StateClosed = 0x00,  /**<closed state*/
    eHal4StateSelfTestMode, /**<Self test mode*/
    eHal4StateOpenAndReady ,/**<Fully initialised*/
    eHal4StateConfiguring ,  /**<configuration ongoing,transient state*/
    eHal4StateTargetDiscovered,/**<target discovered*/
    eHal4StateTargetActivate,/**<state during a select or reactivate*/
    eHal4StateEmulation,/**<Emulation state*/
    eHal4StateTargetConnected,/**<Connected state*/
    eHal4StateTransaction,/**<configuration ongoing,transient state*/
    eHal4StatePresenceCheck,/**<Presence Check state*/
    eHal4StateInvalid
} phHal4Nfc_Hal4state_t;


/**Global Pointer to hardware reference used in timer callbacks to get the 
   context pointer*/
extern phHal_sHwReference_t *gpphHal4Nfc_Hwref;

/**Context info for HAL4 transceive*/
typedef struct phHal4Nfc_TrcvCtxtInfo{
    /*Upper layer's Transceive callback*/
    pphHal4Nfc_TransceiveCallback_t  pUpperTranceiveCb;
     /*Upper layer's Send callback*/
    pphHal4Nfc_SendCallback_t        pP2PSendCb;
     /*Upper layer's receive callback*/
    pphHal4Nfc_ReceiveCallback_t     pP2PRecvCb;
    /**Flag to check if a P2P Send is ongoing when target release is issued by
       the upper layer.If this flag is set ,then a remote device disconnect call
       will be deferred*/
    uint8_t                          P2P_Send_In_Progress;
    /*Data structure to provide transceive info to Hci*/
    phHciNfc_XchgInfo_t              XchangeInfo;
    /*sData pointer to point to upper layer's send data*/
    phNfc_sData_t                   *psUpperSendData;
    /*Maintains the offset of number of bytes sent in one go ,so that the 
      remaining bytes can be sent during the next transceive*/
    uint32_t                         NumberOfBytesSent;
    /*Number of bytes received during a P2p receive*/
    uint32_t                         P2PRecvLength;
    /*sData pointer to point to upper layer's recv data*/
    phNfc_sData_t                   *psUpperRecvData;
    /*structure to hold data received from lower layer*/
    phNfc_sData_t                    sLowerRecvData;
    /*Offset for Lower Recv Data buffer*/
    uint32_t                         LowerRecvBufferOffset;
    /*Holds the status of the RecvDataBuffer:
    NFCSTATUS_SUCCESS:Receive data buffer is complete with data & P2P receive has
                      not yet been called
    NFCSTATUS_PENDING:RecvDataBuffer is yet to receive the data from lower layer
    */
    NFCSTATUS                        RecvDataBufferStatus;
    /*Transaction timer ,currently used only for P2P receive on target*/
    uint32_t                         TransactionTimerId;
}phHal4Nfc_TrcvCtxtInfo_t,*pphHal4Nfc_TrcvCtxtInfo_t;


/**Context info for HAL4 Device discovery feature*/
typedef struct phHal4Nfc_ADDCtxtInfo{
    /*total number of devices discovered*/
    uint8_t                          nbr_of_devices;
    /*smx_discovery*/
    uint8_t                          smx_discovery;
    /*Most recently used ADD configuration*/
    phHal_sADD_Cfg_t                 sADDCfg;
    /*Most recently used Poll configuration*/
    phHal_sPollDevInfo_t             sCurrentPollConfig;
    /*Set when Poll Configured and reset when polling is disabled.*/
    uint8_t                          IsPollConfigured;
}phHal4Nfc_ADDCtxtInfo_t,*pphHal4Nfc_ADDCtxtInfo_t;

/**Context info for HAL4 connect/disconnect*/
typedef struct phHal4Nfc_TargetConnectInfo{
    /*connect callback*/
    pphHal4Nfc_ConnectCallback_t     pUpperConnectCb;
    /*Disconnect callback*/
    pphHal4Nfc_DiscntCallback_t      pUpperDisconnectCb;
    /*used when a release call is pending in HAL*/
    phHal_eReleaseType_t             ReleaseType;
    /*Points to Remote device info of a connected device*/
    phHal_sRemoteDevInformation_t   *psConnectedDevice; 
    /*Emulation state Activated/Deactivated*/
    phHal_Event_t                    EmulationState; 
    /*Presence check callback*/
    pphHal4Nfc_GenCallback_t         pPresenceChkCb;
}phHal4Nfc_TargetConnectInfo_t,*pphHal4Nfc_TargetConnectInfo_t;

/**Context info for HAL4 connect & disconnect*/
typedef struct phHal4Nfc_UpperLayerInfo{
    /*Upper layer Context for discovery call*/
    void                            *DiscoveryCtxt;
    /*Upper layer Context for P2P discovery call*/
    void                            *P2PDiscoveryCtxt;
    /**Context and function pointer for default event handler registered
      by upper layer during initialization*/
    void                            *DefaultListenerCtxt;
    /*Default event handler*/
    pphHal4Nfc_Notification_t        pDefaultEventHandler;
    /**Upper layer has to register this listener for receiving info about 
        discovered tags*/
    pphHal4Nfc_Notification_t        pTagDiscoveryNotification;
    /**Upper layer has to register this  listener for receiving info about 
        discovered P2P devices*/
    pphHal4Nfc_Notification_t        pP2PNotification;
    /*Event Notification Context*/
    void                            *EventNotificationCtxt;
    /**Notification handler for emulation and other events*/
    pphHal4Nfc_Notification_t        pEventNotification;
    /**Upper layer's Config discovery/Emulation callback registry*/
    pphHal4Nfc_GenCallback_t         pConfigCallback;
    void                            *psUpperLayerCtxt;
    void                            *psUpperLayerDisconnectCtxt;
#ifdef LLCP_DISCON_CHANGES
    void                            *psUpperLayerCfgDiscCtxt;
#endif /* #ifdef LLCP_DISCON_CHANGES */
     /**Upper layer's Open Callback registry*/
    pphHal4Nfc_GenCallback_t         pUpperOpenCb;
    /**Upper layer's Close Callback registry */
    pphHal4Nfc_GenCallback_t         pUpperCloseCb; 
    /*Ioctl out param pointer ,points to buffer provided by upper layer during
      a ioctl call*/
    phNfc_sData_t                   *pIoctlOutParam;
    /*Ioctl callback*/
    pphHal4Nfc_IoctlCallback_t       pUpperIoctlCb;
}phHal4Nfc_UpperLayerInfo_t;

/**Context structure for HAL4.0*/
typedef struct phHal4Nfc_Hal4Ctxt{
    /**Hci handle obtained in Hci_Init*/ 
    void                            *psHciHandle;
    /**Layer configuration*/
    pphNfcLayer_sCfg_t               pHal4Nfc_LayerCfg;
    /**Device capabilities*/
    phHal_sDeviceCapabilities_t      Hal4Nfc_DevCaps;
    /*Current state of HAL4.Updated generally in callbacks*/
    phHal4Nfc_Hal4state_t            Hal4CurrentState; 
    /*Next state of HAL.Updated during calls*/
    phHal4Nfc_Hal4state_t            Hal4NextState; 
    /**Info related to upper layer*/
    phHal4Nfc_UpperLayerInfo_t       sUpperLayerInfo;
     /*ADD context info*/
    pphHal4Nfc_ADDCtxtInfo_t         psADDCtxtInfo;
    /*union for different configurations ,used in a config_parameters()call*/
    phHal_uConfig_t                  uConfig;
     /*Event info*/
    phHal_sEventInfo_t              *psEventInfo;
    /*Select sector flag*/
    uint8_t                          SelectSectorFlag;
    /**List of pointers to remote device information for all discovered 
       targets*/
    phHal_sRemoteDevInformation_t   *rem_dev_list[MAX_REMOTE_DEVICES];
    /*Transceive context info*/
    pphHal4Nfc_TrcvCtxtInfo_t        psTrcvCtxtInfo; 
    /*Connect context info*/
    phHal4Nfc_TargetConnectInfo_t    sTgtConnectInfo;
    /*Last called Ioctl_type*/
    uint32_t                         Ioctl_Type;
#ifdef IGNORE_EVT_PROTECTED
    /*used to ignore multiple Protected events*/
    uint8_t                          Ignore_Event_Protected;
#endif/*#ifdef IGNORE_EVT_PROTECTED*/
    uint8_t                          FelicaIDm[(PHHAL_FEL_ID_LEN + 2)];
}phHal4Nfc_Hal4Ctxt_t;


/*---------------- Function Prototypes ----------------------------------------------*/

/*Callback completion routine for Connect*/
extern void phHal4Nfc_ConnectComplete(
                                      phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                                      void *pInfo
                                      );

/*Callback completion routine for Disconnect*/
extern void phHal4Nfc_DisconnectComplete(
                            phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                            void *pInfo
                            );

/*Callback completion routine for Transceive*/
extern void phHal4Nfc_TransceiveComplete(
                        phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                        void *pInfo
                        );

/*Callback completion routine for Presence check*/
extern void phHal4Nfc_PresenceChkComplete(
                        phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                        void *pInfo
                        );

/*Configuration completion routine*/
extern void phHal4Nfc_ConfigureComplete(
                                        phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                                        void *pInfo,
                                        uint8_t type
                                        );


/*Callback completion routine for ADD*/
extern void phHal4Nfc_TargetDiscoveryComplete(
                            phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                            void *pInfo
                            );

/*Event handler routine for Emulation*/
extern void phHal4Nfc_HandleEmulationEvent(
                        phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                        void *pInfo
                        );

/*Callback completion routine for NFCIP1 Receive*/
extern void phHal4Nfc_RecvCompleteHandler(phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,void *pInfo);

/*Callback completion routine for Send*/
extern void phHal4Nfc_SendCompleteHandler(phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,void *pInfo);

/*Callback completion routine for P2P Activate Event received from HCI*/
extern void phHal4Nfc_P2PActivateComplete(
                    phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                    void *pInfo
                    );
/*Callback completion routine for P2P Deactivate Event received from HCI*/
extern void phHal4Nfc_HandleP2PDeActivate(
                        phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                        void *pInfo
                        );

/*Callback completion routine for reactivate target*/
extern void phHal4Nfc_ReactivationComplete(
                        phHal4Nfc_Hal4Ctxt_t  *Hal4Ctxt,
                        void *pInfo
                        );

/**Execute Hal4 Disconnect*/
extern NFCSTATUS phHal4Nfc_Disconnect_Execute(
                            phHal_sHwReference_t  *psHwReference
                            );

/**Handle transceive timeout*/
#ifdef TRANSACTION_TIMER
extern void phHal4Nfc_TrcvTimeoutHandler(uint32_t TrcvTimerId);
#endif /*TRANSACTION_TIMER*/

#endif/*PHHAL4NFC_INTERNAL_H*/


