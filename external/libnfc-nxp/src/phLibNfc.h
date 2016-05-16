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
*\file  phLibNfc_1.1.h
*\brief Contains FRI1.1 API details.
*Project:   NFC-FRI 1.1
* $Workfile:: phLibNfc_1.1.h  $
* $Modtime::          $
* $Author: ing07385 $
* $Revision: 1.80 $
* $Aliases: NFC_FRI1.1_WK1014_SDK,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1019_SDK,NFC_FRI1.1_WK1024_SDK $
*\defgroup grp_lib_nfc LIBNFC Component
*/
/* \page LibNfc_release_label FRI1.1 API Release Label
* $Aliases: NFC_FRI1.1_WK1014_SDK,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1019_SDK,NFC_FRI1.1_WK1024_SDK $
*\note This is the TAG (label, alias) of the FRI1.1.
*If the string is empty, the current documentation
* has not been generated for official release.
*/
#ifndef PHLIBNFC_H
#define PHLIBNFC_H
#include <phNfcTypes.h>
#include <phLibNfcStatus.h>
#include <phFriNfc_NdefRecord.h>
#include <phNfcLlcpTypes.h>
#include <phNfcConfig.h>
#ifdef ANDROID
#include <string.h>
#endif

/*!
*\def PHLIBNFC_MAXNO_OF_SE
*Defines maximum no of secured elements supported by PN544.
*/
#define LIBNFC_READONLY_NDEF
#define PHLIBNFC_MAXNO_OF_SE        (0x02)

typedef uint32_t    phLibNfc_Handle;

extern const unsigned char *nxp_nfc_full_version;


/**
*\ingroup grp_lib_nfc
*   
*\brief Defines Testmode Init configuration values 
*/

typedef enum
{
    phLibNfc_TstMode_Off = 0x00,      /**< Test mode is off  */
    phLibNfc_TstMode_On               /**< Testmode is on */

} phLibNfc_Cfg_Testmode_t;


/**
*\ingroup grp_lib_nfc
*   
*\brief Defines Secure Element configurable states 
*/

typedef enum
{
    phLibNfc_SE_Active = 0x00,      /**< state of the SE is active  */
    phLibNfc_SE_Inactive= 0x01      /**< state of the SE is In active*/

} phLibNfc_SE_State_t;

/**
*\ingroup grp_lib_nfc
*
*\brief Defines Secure Element types.
*/
typedef enum
{
    phLibNfc_SE_Type_Invalid=0x00,/**< Indicates SE type is Invalid */
    phLibNfc_SE_Type_SmartMX=0x01,/**< Indicates SE type is SmartMX */
    phLibNfc_SE_Type_UICC   =0x02,/**<Indicates SE type is   UICC */
    phLibNfc_SE_Type_Unknown=0x03/**< Indicates SE type is Unknown */
}phLibNfc_SE_Type_t;

/**
*\ingroup grp_lib_nfc
*
*\brief Defines  Ndef Card Offset.
*/
typedef enum 
{
    phLibNfc_Ndef_EBegin = 0x01,      /**<  Start from Beginning position */
    phLibNfc_Ndef_ECurrent            /**<  Start from Current position */

} phLibNfc_Ndef_EOffset_t;

/**
* \ingroup grp_lib_nfc
*
*\brief This function allows to activate specific Secure element.

*\b VirtualMode: Virtual mode is used to communicated with secure elements from external reader.
*                This mode is also called as card emulation mode.when secure element mode is set
*                to this mode,external reader can communicate with this SE.
*
*\b WiredMode:   Wired mode is used to communicate with the Secure Element internally.  
*                No RF field is generated. In this mode, PN544 acts as reader and discovers 
*                SmartMX as MIFARE tag.External reader cannot access any of the SE's in this mode.
*                This mode is applicable to only SmartMX secure element.
*
*/
typedef enum
{
    phLibNfc_SE_ActModeWired=0x00,      /**< Enables Wired Mode communication.This mode shall
                                        be applied to */
    phLibNfc_SE_ActModeDefault = 0x01,    /**< Enables Virtual Mode communication.
                                        This can be applied to UICC as well as SmartMX*/
    phLibNfc_SE_ActModeVirtual=0x02,    /**< Enables Virtual Mode communication.
                                        This can be applied to UICC as well as SmartMX*/
    phLibNfc_SE_ActModeOff  =0x03,      /**< Inactivate SE.This means,put SE in in-active state
                                        This can be applied to UICC as well as SmartMX*/
    phLibNfc_SE_ActModeVirtualVolatile = 0x04 /**< Enabled virtual mode communication for SE through an event
                                             This can be applied to UICC as well as SmartMX*/

}phLibNfc_eSE_ActivationMode;

/**
* \ingroup grp_lib_nfc
*
*\brief Defines SE low power mode types.
*/
typedef enum
{
    phLibNfc_SE_LowPowerMode_Off= 0x01, /**< Indicates no SE to be selected in low power mode */
    phLibNfc_SE_LowPowerMode_On = 0x02  /**< Indicates requested SE to be  selected in low power mode */ 

} phLibNfc_SE_LowPowerMode_t;

/**
* \ingroup grp_lib_nfc
*
*\brief Defines Secure element event info .
*/
typedef union phLibNfc_uSeEvtInfo
{
   phNfc_sUiccInfo_t UiccEvtInfo;	/**< Indicates UICC event info for Evt_Transaction event */
}phLibNfc_uSeEvtInfo_t;

/**
* \ingroup grp_lib_nfc
*
*\brief Types of SE transaction events sent to SE notification handler .
*/
typedef enum
{
    phLibNfc_eSE_EvtStartTransaction=0x00,   /**< Indicates  transaction started on 
                                            secure element */
    phLibNfc_eSE_EvtEndTransaction=0x01,      /**<Indicates  transaction ended on    secure
                                             element*/
    phLibNfc_eSE_EvtTypeTransaction=0x02,   /**<Indicates external reader trying to access secure element */ 
                                                                                       
    phLibNfc_eSE_EvtConnectivity,           /**<This event notifies the terminal host that it shall
                                           send a connectivity event from UICC as defined in 
                                           ETSI TS 102 622 V7.4.0 */
    phLibNfc_eSE_EvtFieldOn,  // consider using phLibNfc_eSE_EvtConnectivity
    phLibNfc_eSE_EvtFieldOff,

    phLibNfc_eSE_EvtApduReceived, /* PAYPASS MagStripe or MCHIP_4 transaction */

    phLibNfc_eSE_EvtCardRemoval, /* Indicates the beginning of an EMV Card Removal sequence */

    phLibNfc_eSE_EvtMifareAccess /* Indicates when the SMX Emulation MIFARE is accessed */
} phLibNfc_eSE_EvtType_t;

/**
* \ingroup grp_lib_nfc
*
*\brief Defines possible registration details for notifications.
*/
typedef phNfc_sSupProtocol_t    phLibNfc_Registry_Info_t;

/**
* \ingroup grp_lib_nfc
*
*\brief  Generic Data buffer definition.
*/
typedef phNfc_sData_t phLibNfc_Data_t;
/**
* \ingroup grp_lib_nfc
*
* Application Identifier (phLibNfc_AID)
*
* The application identifier defines a specific application on a SE.
*
*/
typedef phNfc_sData_t   phLibNfc_AID;
/**
* \ingroup grp_lib_nfc
*
*\brief Remote Device Info definition  .
*/
typedef phNfc_sRemoteDevInformation_t   phLibNfc_sRemoteDevInformation_t; 
/**
* \ingroup grp_lib_nfc
*/
typedef phNfc_eDiscoveryConfigMode_t    phLibNfc_eDiscoveryConfigMode_t;

/**
* \ingroup grp_lib_nfc
*
*\brief Transceive info definition.
*/
typedef phNfc_sTransceiveInfo_t         phLibNfc_sTransceiveInfo_t;
/**
* \ingroup grp_lib_nfc
*
*\brief Automatic Device Discovery Definition. 
*/
typedef phNfc_sADD_Cfg_t            phLibNfc_sADD_Cfg_t;
/**
*\ingroup grp_lib_nfc
*
*\brief Release mode definition. 
*/
typedef phNfc_eReleaseType_t        phLibNfc_eReleaseType_t;

/**
*\ingroup grp_lib_nfc
*
*\brief device capabilities details.
*/
typedef phNfc_sDeviceCapabilities_t phLibNfc_sDeviceCapabilities_t;

/**
* \ingroup grp_lib_nfc
*
*\brief Defines supported tag types for NDEF mapping and formatting feature.
*/
typedef struct SupportedTagInfo
{
    unsigned MifareUL:1;  /**<TRUE indicates  specified feature (mapping or formatting)for MIFARE UL tag supported else  not supported.*/
    unsigned MifareStd:1;  /**<TRUE indicates  specified feature (mapping or formatting)for Mifare Std tag supported else  not supported.*/
    unsigned MifareULC:1;  /**<TRUE indicates  specified feature (mapping or formatting)for MIFARE UL2 tag supported else  not supported.*/
    unsigned ISO14443_4A:1;  /**<TRUE indicates  specified feature (mapping or formatting)for ISO14443_4A tag supported else  not supported.*/
    unsigned ISO14443_4B:1;  /**<TRUE indicates  specified feature (mapping or formatting)for ISO14443_4B tag supported else  not supported.*/
    unsigned ISO15693:1;  /**<TRUE indicates  specified feature (mapping or formatting)for ISO15693 tag supported else  not supported.*/
    unsigned FeliCa:1;  /**<TRUE indicates  specified feature (mapping or formatting)for FeliCa tag  supported else  not supported.*/
    unsigned Jewel:1;  /**<TRUE indicates  specified feature (mapping or formatting)for JEWEL tag supported else  not supported.*/
    unsigned Desfire:1;  /**<TRUE indicates  specified feature (mapping or formatting)for desfire tag supported else  not supported.*/

}phLibNfc_sSupportedTagInfo_t;


/**
* \ingroup grp_lib_nfc
*
*\brief Defines supported tag types for NDEF mapping feature.
*/
typedef phLibNfc_sSupportedTagInfo_t  phLibNfc_sNDEFMappingInfo_t;

/**
* \ingroup grp_lib_nfc
*
*\brief Defines supported tag types for NDEF formatting feature.
*/

typedef phLibNfc_sSupportedTagInfo_t  phLibNfc_sTagFormattingInfo_t; 

/**
* \ingroup grp_lib_nfc
*
*\brief Stack capabilities details contains device capabilities and supported tags for NDEF mapping and formatting feature.
*/

typedef struct StackCapabilities
{ 
    phLibNfc_sDeviceCapabilities_t      psDevCapabilities;
    phLibNfc_sNDEFMappingInfo_t         psMappingCapabilities;
    phLibNfc_sTagFormattingInfo_t       psFormatCapabilities;
}phLibNfc_StackCapabilities_t;


/**
* \ingroup grp_lib_nfc
*
*\brief Defines Secure Element list type.
*/
typedef struct phLibNfc_SecureElementInfo
{
    phLibNfc_Handle         hSecureElement; /**< handle to Secure Element */
    phLibNfc_SE_Type_t      eSE_Type;       /**< type of Secure Element(SE)*/
    phLibNfc_SE_State_t     eSE_CurrentState;/**< state of the secure element indicates activated or not*/
} phLibNfc_SE_List_t;

/**
* \ingroup grp_lib_nfc
*
*\brief Defines target specific info obtained during device discovery.
*/
typedef struct phLibNfc_RemoteDev
{
    phLibNfc_Handle                   hTargetDev;       /**< discovered Target handle */
    phLibNfc_sRemoteDevInformation_t* psRemoteDevInfo;  /**< discovered Target details */

}phLibNfc_RemoteDevList_t;

typedef  phNfc_sNfcIPCfg_t      phLibNfc_sNfcIPCfg_t;

/**
*\ingroup grp_lib_nfc 
*\brief  NDEF registration structure definition.
*/
typedef struct phLibNfc_Ndef_SrchType
{
    uint8_t Tnf;        /**<  Type Name Format of this NDEF record */
    uint8_t *Type;      /**<  Type field of this NDEF record */
    uint8_t TypeLength; /**<  Length of the Type field of this NDEF record */
} phLibNfc_Ndef_SrchType_t;

/**
*\ingroup grp_lib_nfc 
* \brief NDEF information structure definition. \n 
*/
typedef struct phLibNfc_Ndef_Info
{
    uint32_t                NdefMessageLengthActual;   /**<  Actual length of the NDEF message  */
    uint32_t                NdefMessageLengthMaximum;  /**<  Maximum length of the NDEF message */
    uint8_t                 *pNdefMessage;             /**<  Pointer to raw NDEF Data buffer    */
    uint32_t                NdefRecordCount;           /**<  Number of NDEF records pointed by pNdefRecord */
    phFriNfc_NdefRecord_t   *pNdefRecord;              /**<  Pointer to the NDEF Records contained within the NDEF message */
    
} phLibNfc_Ndef_Info_t;

/* As per NFC forum specification, the card can be in either of the below mentioned states 
    INVALID - means card is NOT NFC forum specified tag. NDEF FORMAT can only be performed for 
                the factory cards, other cards may or may not be formatted for NDEF FORMAT function.
    INITIALISED - means card is NFC forum specified tag. But, in this state 
                the user has to first call NDEF WRITE, because in INITIALISED state, there 
                wont be any data i.e.,ACTUAL NDEF FILE SIZE is 0. After the first 
                NDEF WRITE, NDEF READ and WRITE functions can be called any number of times.
    READ WRITE - means card is NFC forum specified tag. User can use both 
                NDEF READ and WRITE functions
    READ ONLY - means card is NFC forum specified tag. User can only use 
                NDEF READ. NDEF WRITE function will not work.    
    */
#define PHLIBNFC_NDEF_CARD_INVALID                      0x00U
#define PHLIBNFC_NDEF_CARD_INITIALISED                  0x01U
#define PHLIBNFC_NDEF_CARD_READ_WRITE                   0x02U
#define PHLIBNFC_NDEF_CARD_READ_ONLY                    0x03U

/**
* \ingroup grp_lib_nfc
*
*\brief Ndef Information Structure.
*/
typedef struct phLibNfc_ChkNdef_Info
{
    uint8_t   NdefCardState;                    /**< Card state information */
    uint32_t  ActualNdefMsgLength;              /**< Indicates Actual length of NDEF Message in Tag */
    uint32_t  MaxNdefMsgLength;                 /**< Indicates Maximum Ndef Message length that Tag can hold*/ 
} phLibNfc_ChkNdef_Info_t;

/**
*\ingroup grp_lib_nfc
*
*\brief LLCP link status. Refer to \ref phFriNfc_LlcpMac_eLinkStatus_t
*
*/
typedef phFriNfc_LlcpMac_eLinkStatus_t phLibNfc_Llcp_eLinkStatus_t;

typedef  phFriNfc_Llcp_sLinkParameters_t  phLibNfc_Llcp_sLinkParameters_t;

typedef phFriNfc_LlcpTransport_eSocketType_t phLibNfc_Llcp_eSocketType_t;

typedef phFriNfc_LlcpTransport_sSocketOptions_t phLibNfc_Llcp_sSocketOptions_t;

/**
* \ingroup grp_lib_nfc
*
*\brief Response callback for connect request.
*
* Callback type used to indicate a Connect request Successful or Failure indication to 
* LibNfc client.
*
* \param[in] pContext           Context passed in the connect request before.
* \param[in] hRemoteDev         Handle to remote device on which connect was requested. 
* \param[in] psRemoteDevInfo    contains updated remote device details.For few tags
*                               like ISO-14443A  details like historical bytes gets updated 
*                               only after connecting to target.Once connect is successful
*                               \b psRemoteDevInfo gets updated.
*
* \param[in] status             Status of the response  callback.
*
*                  \param NFCSTATUS_SUCCESS          Connect operation successful.
*                  \param NFCSTATUS_TARGET_LOST Connect operation failed because target is lost.
*                  \param NFCSTATUS_SHUTDOWN    Shutdown in progress.
*
*/

typedef void (*pphLibNfc_ConnectCallback_t) (void*		pContext,
                                phLibNfc_Handle			hRemoteDev,
                                phLibNfc_sRemoteDevInformation_t* psRemoteDevInfo,
                                NFCSTATUS				Status
                                );

/**
* \ingroup grp_lib_nfc
*
*\brief Response callback for disconnect request.
*
* Callback type used to provide a disconnect Success or Failure indication to 
* LibNfc client.
*
* \param[in] pContext       Context passed in the disconnect request before.
* \param[in] hRemoteDev     Handle to remote device on which disconnect is requested.   
* \param[in] status         Status of the response  callback.
*
*                  \param NFCSTATUS_SUCCESS          Disconnect operation successful.
*                  \param NFCSTATUS_SHUTDOWN    Shutdown in progress.
*
*/
typedef void (*pphLibNfc_DisconnectCallback_t)(void*                pContext,
                                               phLibNfc_Handle      hRemoteDev,
                                               NFCSTATUS            Status
                                              );
/**
* \ingroup grp_lib_nfc
*
*\brief Response callback for IOCTL request.
*
* Callback type to inform success or failure of the Ioctl request 
* made by LibNfc client. It may optionally contain response data
* depending on the Ioctl command type issued.

*
* \param[in] pContext           Context passed in the connect request before.
* \param[in] status             Status of the response  callback.
*
*                  \param NFCSTATUS_SUCCESS          Ioctl operation successful.
*                  \param NFCSTATUS_TARGET_LOST Ioctl operation failed because target is lost.
*                  \param NFCSTATUS_SHUTDOWN    Ioctl operation failed because Shutdown in progress.
*
*\param[in,out]     pOutParam       contains Ioctl command specific response buffer and size 
*                                   of the buffer.This buffer address will be same as 
*                                   pOutParam sent in \ref phLibNfc_Mgt_IoCtl.
*
*/


typedef void (*pphLibNfc_IoctlCallback_t)   (void*          pContext,                                        
                                             phNfc_sData_t* pOutParam,
                                             NFCSTATUS      Status
                                             );



/**
* \ingroup grp_lib_nfc
*
*\brief Response callback for Transceive request.
*
* This callback type is used to provide received data and it's size to the 
* LibNfc client in \ref phNfc_sData_t format ,when  LibNfc client has performed 
* a Transceive operation on a tag or when the device acts as an Initiator during a 
* P2P transactions.
*
* \param[in] pContext       LibNfc client context   passed in the corresponding request before.
* \param[in] hRemoteDev     Handle to remote device on transceive is performed.     
* \param[in] pResBuffer     Response buffer of type \ref phNfc_sData_t.
* \param[in] status         Status of the response  callback.
*
*           \param NFCSTATUS_SUCCESS                 Transceive operation  successful.
*           \param NFCSTATUS_TARGET_LOST        Transceive operation failed because target is lost.
*           \param NFCSTATUS_SHUTDOWN           Transceive operation failed because Shutdown in progress.
*           \param NFCSTATUS_ABORTED            Aborted due to disconnect request in between.
*
*/
typedef void (*pphLibNfc_TransceiveCallback_t)( void*               pContext,
                                                phLibNfc_Handle     hRemoteDev,
                                                phNfc_sData_t*      pResBuffer,
                                                NFCSTATUS			Status
                                              );

/**
* \ingroup grp_lib_nfc
*
* \brief  Generic Response Callback definition.
*
* Generic callback definition used as callback type in few APIs below.
*
* \note : Status and error codes for this type of callback are documented in respective APIs 
* wherever it is used.
*
* \param[in] pContext       LibNfc client context   passed in the corresponding request
*                           before.
* \param[in] status         Status of the response  callback.
*/
typedef void(*pphLibNfc_RspCb_t) (void* pContext,NFCSTATUS  Status);
/**
* \ingroup grp_lib_nfc
*
* \brief  Check NDEF Callback definition.
*
* This call back is used by check ndef api.
*
* \note : Status and error codes for this type of callback are documented in API 
*
* \param[in] pContext       LibNfc client context   passed in the corresponding request
*                           before.
* \param[in] Ndef_Info      Ndef message length and the container size.
* \param[in] status         Status of the response  callback.
*/
typedef void(*pphLibNfc_ChkNdefRspCb_t)(void*           pContext,
                                phLibNfc_ChkNdef_Info_t Ndef_Info,
                                NFCSTATUS               Status);


/**
* \ingroup grp_lib_nfc
* \brief  Notification handler callback definition.
*
*This callback type is used to provide information on discovered targets to LibNfcClient.
*Discovered targets will be notified in \ref phLibNfc_RemoteDevList_t format.
*In case multiple targets discovered ,remote device list contains these targets one after another.
*
*\note List will be exported as memory block,based on \b uNofRemoteDev
*      parameter application has to access remote devices accordingly.
*
*\b Ex: Multiple targets discovered  can be referred as phLibNfc_RemoteDevList_t[0]
*and phLibNfc_RemoteDevList_t[1].
*
*Subsequent operations on discovered target shall be performed using  target specific handle
*\b hTargetDev.
*
* \param[in] pContext       Client context passed in the corresponding 
*                           request before.The context is handled by client 
*                           only.
*
* \param[in] psRemoteDevList Remote Device list contains discovered target details.
*                            Refer to \ref phLibNfc_RemoteDevList_t .
*                            List size depends on no of remote devices discovered.
*
* \param[in] uNofRemoteDev   Indicates no of remote devices discovered .
*                            In case more than one target discovered,\b psRemoteDevList contains 
*                            multiple target details.   
*
* \param[in] Status Status  of the response callback.
*
*                   \param NFCSTATUS_SUCCESS                 Discovered single target successfully.
*                   \param NFCSTATUS_MULTIPLE_TARGETS   multiple targets found.
*                   \param NFCSTATUS_MULTI_PROTOCOLS    Target found supports multiple protocols.
*                   \param NFCSTATUS_SHUTDOWN           Registration  failed because shutdown in progress.
*                   \param NFCSTATUS_DESELECTED         initiator issued disconnect or intiator 
*                                                       physically removed from the RF field.
*
*\note: multiple tag detection is possible only within same technology but not across 
*       different technologies. 
*/
typedef void (*phLibNfc_NtfRegister_RspCb_t)(
    void*                           pContext,
    phLibNfc_RemoteDevList_t*       psRemoteDevList,
    uint8_t                         uNofRemoteDev,
    NFCSTATUS                       Status
    );
  
/**
* \ingroup grp_lib_nfc
* \brief Response Callback for secure element mode settings.
*
* This callback type is used to provide information on requested secure element is
* activated or not to LibNfcClient. 
*
* \param[in] pContext LibNfc client context     passed in the activation request.
*
* \param[in] hSecureElement     Handle to secure element.
*
* \param[in] Status             Indicates API status.
*           \param NFCSTATUS_SUCCESS    Secure element  activated successfully.
*           \param NFCSTATUS_SHUTDOWN   Activation failed because shutdown in progress.
*           \param NFCSTATUS_FAILED     Activation failed.
*
*/
typedef void(*pphLibNfc_SE_SetModeRspCb_t)(
                                            void*            pContext,
                                            phLibNfc_Handle  hSecureElement,
                                            NFCSTATUS        Status                                          
                                           );
/**
* \ingroup grp_lib_nfc 
* \brief Notification callback for \ref phLibNfc_SE_NtfRegister().
*
* A function of this type is called when external reader tries to access SE.
*
*\param[in] pContext        LibNfc client context passed in the SE notification register request.
*                           The context is Handled by client only.
* \param[in] EventType          Event type of secure element transaction
* \param[in] hSecureElement     handle to Secures Element.
*
*\param[in] pAppID              Application identifier to be accessed on SE .
*                               Sent when available from SE otherwise empty.
*
*\param[in] Status      Indicates API status.
*       \param NFCSTATUS_SUCCESS         Notification handler registered sucessfully.
*       \param NFCSTATUS_SHUTDOWN   Shutdown in progress.
*       \param NFCSTATUS_FAILED     set mode operation failed.
*
*
*/
typedef void (*pphLibNfc_SE_NotificationCb_t) (void*                        pContext,
                                               phLibNfc_eSE_EvtType_t       EventType,
                                               phLibNfc_Handle              hSecureElement,
                                               phLibNfc_uSeEvtInfo_t*       pSeEvtInfo,
                                               NFCSTATUS                    Status
                                               );


/**
*\ingroup grp_lib_nfc
*\brief Receive callback definition.
*
* This callback type is used to provide received data and it's size to the 
* LibNfc client in \ref phNfc_sData_t format ,when  LibNfc client has performed 
* when the device acts as a Target during P2P communication 
*
* \param[in] pContext                   LibNfc client context   passed in the corresponding 
*                                       request before.
* \param[in] pRecvBufferInfo            Response buffer of type \ref phNfc_sData_t.
* \param[in] status                     Status of the response  callback.
*
*           \param NFCSTATUS_SUCCESS         Receive operation  successful.
*           \param NFCSTATUS_SHUTDOWN   Receive operation failed because
*                                       Shutdown in progress.
*           \param NFCSTATUS_ABORTED    Aborted due to initiator issued disconnect request.
*                                       This status code reported,to indicate P2P session
*                                       closed and send and receive requests not allowed any more
*                                       unless new session is started.
*/
typedef void (*pphLibNfc_Receive_RspCb_t)(void*              pContext,                                    
                                          phNfc_sData_t*     pRecvBufferInfo,
                                          NFCSTATUS          status
                                          );
/**
*\ingroup grp_lib_nfc
*
* \brief NDEF Response callback definition
*
*  A function of this type is notified when registered NDEF type detected.
*
* \b Note :Once this type callback is notified,discovery wheel is stopped.
*In order to restart discovery process again it is important to disconnect
*from current tag.LibNfc client shall disconnect explicitly using 
*\ref phLibNfc_RemoteDev_Disconnect() interface.
*
*\param[in] pContext                    Pointer to context previously provided by the user
*\param[in] psNdefInfo                  All Ndef specific details of the remote device discovered.
*\param[in] hRemoteDevice               handle to remote device on which NDEF detection is done.
*
*\param[in] Status                      Indicates callback status.
*
*           \param NFCSTATUS_SUCCESS         Indicates registered tnf  type detected.            .
*           \param NFCSTATUS_SHUTDOWN   Indicates shutdown in progress.
*           \param NFCSTATUS_FAILED     status failed.
*           \param NFCSTATUS_ABORTED    Aborted due to disconnect operation in between.


*/
typedef void (*pphLibNfc_Ndef_Search_RspCb_t)   ( void*                  pContext, 
                                                phLibNfc_Ndef_Info_t*    psNdefInfo,
                                                phLibNfc_Handle          hRemoteDevice,
                                                NFCSTATUS                Status
                                                );   


/**
*\ingroup grp_lib_nfc
*
* \brief LLCP check response callback definition
*/
typedef void (*pphLibNfc_ChkLlcpRspCb_t) ( void*      pContext,
                                           NFCSTATUS  status
                                           );


/**
*\ingroup grp_lib_nfc
*
* \brief LLCP check response callback definition
*/
typedef void (*pphLibNfc_LlcpLinkStatusCb_t) ( void*                         pContext,
                                               phLibNfc_Llcp_eLinkStatus_t   eLinkStatus
                                               );


/**
*\ingroup grp_lib_nfc
*
* \brief LLCP socket error notification callback definition
*/
typedef void (*pphLibNfc_LlcpSocketErrCb_t) ( void*      pContext,
                                              uint8_t    nErrCode
                                              );

/**
*\ingroup grp_lib_nfc
*
* \brief Incoming connection on a listening LLCP socket callback definition
*/
typedef void (*pphLibNfc_LlcpSocketListenCb_t) ( void*            pContext,
                                                 phLibNfc_Handle  hIncomingSocket
                                                 );

/**
*\ingroup grp_lib_nfc
*
* \brief LLCP socket connect callback definition
*/
typedef void (*pphLibNfc_LlcpSocketConnectCb_t) ( void*        pContext,
                                                  uint8_t      nErrCode,
                                                  NFCSTATUS    status
                                                  );

/**
*\ingroup grp_lib_nfc
*
* \brief LLCP socket disconnect callback definition
*/
typedef void (*pphLibNfc_LlcpSocketDisconnectCb_t) ( void*        pContext,
                                                     NFCSTATUS    status
                                                     );

/**
*\ingroup grp_lib_nfc
*
* \brief LLCP socket Accept callback definition
*/
typedef void (*pphLibNfc_LlcpSocketAcceptCb_t) ( void*        pContext,
                                                 NFCSTATUS    status
                                                 );

/**
*\ingroup grp_lib_nfc
*
* \brief LLCP socket Reject callback definition
*/
typedef void (*pphLibNfc_LlcpSocketRejectCb_t) ( void*        pContext,
                                                 NFCSTATUS    status
                                                 );

/**
*\ingroup grp_lib_nfc
*
* \brief LLCP socket reception callback definition
*/
typedef void (*pphLibNfc_LlcpSocketRecvCb_t) ( void*     pContext,
                                               NFCSTATUS status
                                               );

/**
*\ingroup grp_lib_nfc
*
* \brief LLCP socket reception with SSAP callback definition
*/
typedef void (*pphLibNfc_LlcpSocketRecvFromCb_t) ( void*       pContext,
                                                   uint8_t     ssap,
                                                   NFCSTATUS   status
                                                   );

/**
*\ingroup grp_lib_nfc
*
* \brief LLCP socket emission callback definition
*/
typedef void (*pphLibNfc_LlcpSocketSendCb_t) ( void*        pContext,
                                               NFCSTATUS    status
                                               );


/*  FUNCTION PROTOTYPES  */
/**
 * \ingroup grp_lib_nfc
 *
 * \brief Driver configuration function
 * This synchronous function configures the given driver Interface and
 * sends the HANDLE to the caller.
 *
 * \param[in]       psConfig   Driver configuration details as provided
 *                             by the upper layer. 
 * \param[in,out]   ppDriverHandle     pointer to which valid Handle to driver
 *                             interface is assigned.
 *
 * \retval NFCSTATUS_SUCCESS                    Configuration happened successfully.
 * \retval NFCSTATUS_INVALID_PARAMETER          At least one parameter of the function
 *                                              is invalid.
 * \retval NFCSTATUS_FAILED                     Configuration failed(example.unable to
 *                                              open HW Interface).
 * \retval NFCSTATUS_INVALID_DEVICE             The device has not been opened or
 *                                              has been disconnected meanwhile
 * \retval NFCSTATUS_BOARD_COMMUNICATION_ERROR  A board communication error occurred
                                                (e.g. configuration went wrong).
 *\msc
*LibNfcClient,LibNfc;
*--- [label="Before initializing Nfc LIB,Configure Driver layer"];
*LibNfcClient=>LibNfc[label="phLibNfc_Mgt_ConfigureDriver()",URL="\ref   phLibNfc_Mgt_ConfigureDriver"];
*LibNfcClient<<LibNfc[label="NFCSTATUS_SUCCESS"];
 *\endmsc
 */
NFCSTATUS phLibNfc_Mgt_ConfigureDriver (pphLibNfc_sConfig_t     psConfig,
                                        void **                 ppDriverHandle
                                        );
 /**
 * \ingroup grp_lib_nfc
 *
 * \brief Release configuration for the given driver Interface.
 *
 * \copydoc page_reg Release all that has been 
 *      initialised in \b phLibNfc_Mgt_ConfigureDriver function (Synchronous function).
 *
 * \param[in] pDriverHandle            Link information of the hardware
 *
 * \retval NFCSTATUS_SUCCESS            Driver Configuration Released successfully.
 * \retval NFCSTATUS_FAILED             Configuration release failed(example: Unable to close Com port).
 *
 *\msc
 *LibNfcClient,LibNfc;
 *LibNfcClient=>LibNfc [label="phLibNfc_Mgt_ConfigureDriver()",URL="\ref phLibNfc_Mgt_ConfigureDriver"];
 *LibNfcClient<<LibNfc [label="NFCSTATUS_SUCCESS"];
 *LibNfcClient=>LibNfc [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
 *LibNfcClient<<LibNfc [label="NFCSTATUS_PENDING"];
 *LibNfcClient<-LibNfc [label="pInitCb"]; 
 *--- [label="Perform feature operations "];
 *LibNfcClient=>LibNfc [label="phLibNfc_Mgt_DeInitialize()",URL="\ref   phLibNfc_Mgt_DeInitialize"];
 *LibNfcClient<<LibNfc [label="NFCSTATUS_PENDING"];
 *LibNfcClient<-LibNfc [label="pDeInitCb"]; 
 *LibNfcClient=>LibNfc [label="phLibNfc_Mgt_UnConfigureDriver()",URL="\ref phLibNfc_Mgt_UnConfigureDriver"];
 *LibNfcClient<<LibNfc [label="NFCSTATUS_SUCCESS"]; 
 *\endmsc
 */
NFCSTATUS phLibNfc_Mgt_UnConfigureDriver (void *                 pDriverHandle
                                          );

NFCSTATUS phLibNfc_HW_Reset ();

NFCSTATUS phLibNfc_Download_Mode ();

int phLibNfc_Load_Firmware_Image ();

// Function for delay the recovery in case wired mode is set
// to complete the possible pending transaction with SE
void phLibNfc_Mgt_Recovery ();

// timeout is 8 bits
// bits [0..3] => timeout value, (256*16/13.56*10^6) * 2^value
//                  [0] -> 0.0003s
//                  ..
//                  [14] -> 4.9s
//                  [15] -> not allowed
// bit [4]     => timeout enable
// bit [5..7]  => unused
NFCSTATUS phLibNfc_SetIsoXchgTimeout(uint8_t timeout);
int phLibNfc_GetIsoXchgTimeout();

NFCSTATUS phLibNfc_SetHciTimeout(uint32_t timeout_in_ms);
int phLibNfc_GetHciTimeout();

// Felica timeout
// [0]      -> timeout disabled
// [1..255] -> timeout in ms
NFCSTATUS phLibNfc_SetFelicaTimeout(uint8_t timeout_in_ms);
int phLibNfc_GetFelicaTimeout();

// MIFARE RAW timeout (ISO14443-3A / NfcA timeout)
// timeout is 8 bits
// bits [0..3] => timeout value, (256*16/13.56*10^6) * 2^value
//                  [0] -> 0.0003s
//                  ..
//                  [14] -> 4.9s
//                  [15] -> not allowed
// bits [4..7] => 0
NFCSTATUS phLibNfc_SetMifareRawTimeout(uint8_t timeout);
int phLibNfc_GetMifareRawTimeout();

/**
* \ingroup grp_lib_nfc
*
* \brief Initializes the NFC library .
*
*
*\brief This function initializes NFC library and its underlying layers.
* As part of this interface underlying modules gets initialized.
* A session with NFC hardware will be established.
* Once initialization is successful ,NFC library ready for use.
*\note It is must to initialize prior usage of the stack .
*
* \param[in] pDriverHandle      Driver Handle currently application is using.
* \param[in] pInitCb            The init callback is called by the LibNfc when  init is
*                               completed or there is an error in initialization.
*
* \param[in] pContext           Client context which will   be included in
*                               callback when the request is completed.
*
* \retval NFCSTATUS_ALREADY_INITIALISED     Stack is already initialized.
* \retval NFCSTATUS_PENDING                 Init sequence   has been successfully
*                                           started and result will be  conveyed via 
*                                           callback notification.
* \retval NFCSTATUS_INVALID_PARAMETER       The parameter could not be  properly
*                                           interpreted.
*\retval NFCSTATUS_INSUFFICIENT_RESOURCES   Insufficient resource.(Ex: insufficient memory)
*
*\msc
*LibNfcClient,LibNfc;
*--- [label="Before initializing Nfc LIB,Configure Driver layer"];
*LibNfcClient=>LibNfc[label="phLibNfc_Mgt_ConfigureDriver()",URL="\ref   phLibNfc_Mgt_ConfigureDriver"];
*LibNfcClient<<LibNfc[label="NFCSTATUS_SUCCESS"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_PENDING"];
*LibNfcClient<-LibNfc   [label="pInitCb"];
*\endmsc
*/
NFCSTATUS phLibNfc_Mgt_Initialize (void *                 pDriverHandle,
                                  pphLibNfc_RspCb_t       pInitCb,
                                  void*                   pContext
                                  );
/**
* \ingroup grp_lib_nfc
*
* \brief De-Initializes NFC library.
*
*
* This function de-initializes and closes the current session with  PN544 NFC hardware.
* All configurations and setups done until now are invalidated to restart
* communication. Resources currently used by stack gets released during De-initialization.
*\ref phLibNfc_Mgt_Initialize needs to be    called once stack is
* De-initialized before using the stack again.
*
* \param[in]    pHwHandle       Hardware context currently application is using.
*
* \param[in]    pDeInitCb       De-initialization callback  is called by the LibNfc when init
*                               completed or there is an error in initialization.
* \param[in]    pContext        Client context which will   be included in
*                               callback when the request is completed.
*
* \retval NFCSTATUS_SUCCESS     Device stack is already De-Initialized.
* \retval NFCSTATUS_PENDING     De-Initialization sequence has been successfully
*                               started and result is conveyed via callback 
*                               notification.
*
* \retval NFCSTATUS_INVALID_PARAMETER   The parameter could not be  properly 
*                                       interpreted.
* \retval  NFCSTATUS_NOT_INITIALISED    Indicates stack is not yet initialized.
* \retval  NFCSTATUS_BUSY               Previous request in progress can not accept new request.
* \retval  NFCSTATUS_FAILED             Request failed.

*
*
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_PENDING"];
*LibNfcClient<-LibNfc   [label="pInitCb"]; 
*--- [label="Perform feature operations "];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_DeInitialize()",URL="\ref   phLibNfc_Mgt_DeInitialize"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_PENDING"];
*LibNfcClient<-LibNfc   [label="pDeInitCb"]; 
*\endmsc
*/
NFCSTATUS phLibNfc_Mgt_DeInitialize(void *                      pDriverHandle,
                                   pphLibNfc_RspCb_t            pDeInitCb,
                                   void*                        pContext
                                   );

/**
* \ingroup grp_lib_nfc
* \brief Get list of available Secure Elements.
*
* This  function retrieves list of secure elements locally connected.
* during LibNfc initialization these SEs enumerated from lower stack and maintained 
* in LibNfc library.Once libNfc client queries using this interface,
* same details exposed to LibNfc client. 
* LibNfc client  shall pass empty list of size \ref PHLIBNFC_MAXNO_OF_SE .
* Once SE list is available, libNfc client can perform operation on specific SE 
* using SE handle.
* The handle given in the \ref phLibNfc_SE_List_t structure stays valid until
* shutdown is called.  
*
*\note In case no SE's found, API still returns \ref NFCSTATUS_SUCCESS with \b uSE_count 
set to zero.Value zero indicates none of the SE's connected to PN544 hardware.

* \param[in,out] pSE_List       contains list of SEs with SE details in \ref phLibNfc_SE_List_t format.
* \param[in,out] uSE_count      contains no of SEs in the list.
*
*\note LibNfc client has to interpret no of secure elements in \b pSE_List   based on this
*count.
*
*\retval    NFCSTATUS_SUCCESS                  Indicates operation is sucessfull.
*\retval    NFCSTATUS_SHUTDOWN                 Operation failed because shutdown in progress.
*\retval    NFCSTATUS_NOT_INITIALISED          Operation failed because stack is not yet initialized.
* \retval   NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                              could not be properly interpreted.
*
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_PENDING"];
*LibNfcClient<-LibNfc   [label="pInitCb"]; 
*--- [label="Now SE list can be retrieved"];
*\endmsc
*/

NFCSTATUS phLibNfc_SE_GetSecureElementList(phLibNfc_SE_List_t*  pSE_List,
                                           uint8_t*             uSE_count
                                           );


/**
* \ingroup grp_lib_nfc
*
*\brief Sets secure element mode.
*
* This  function configures SE to specific mode based on activation mode type.
* Effect of different modes on SE is as below.
*
*\b a)If mode is \ref phLibNfc_SE_ActModeVirtual then external reader can communicate
* with this SE.
*\note This mode is applicable to both UICC and SmartMX.
*
\b b)If mode is \ref phLibNfc_SE_ActModeWired then internal reader can communicate with
* this SE.In this mode PN544 can act as reader and communicate with SE as normal Tag.
*In this mode mode external reader ca not communicate with any of the SEs since RF filed is off.
*
*\note 1.Wired Mode is applicable to only SmartMX not to UICC.
* 2.When SmartMX SE configured in Wired Mode ,LibNfc client shall restart discovery process.
*   SmartMX gets detected as MIFARE tag.
* 3.To exit wired mode ,LibNfc client has to disconnect with release type as "NFC_SMARTMX_RELEASE".
*    
*
*\b c)If mode is \ref phLibNfc_SE_ActModeOff
*This means SE is off mode .It can not be accessed any more in wired or virtual mode. 
*internal reader any more.communicate with internal reader and only
*PN544 can communicate in reader mode and external reader can not 
*communicate with it.This mode is applicable  both SE types ( UICC and SmartMX)
*
* \param[in]  hSE_Handle            Secure  Element Handle .
* \param[in]  eActivation_mode      Indicates SE mode to be configured.
*                                    
*
*
* \param[in]  pphLibNfc_SE_setModeRspCb_t   pointer to response callback.
*
* \param[in]  pContext              Client context which will   be included in
*                                   callback when the request is completed.
*
*
* \retval   NFCSTATUS_PENDING               Activation transaction started.
* \retval   NFSCSTATUS_SHUTDOWN             Shutdown in progress.
* \retval   NFCSTATUS_NOT_INITIALISED       Indicates stack is not yet initialized.
* \retval   NFCSTATUS_INVALID_HANDLE        Invalid Handle.
* \retval   NFCSTATUS_INVALID_PARAMETER     One or more of the supplied parameters
*                                           could not be properly interpreted.
* \retval   NFCSTATUS_REJECTED              Invalid request.(Ex: If wired mode settings called using 
*                                           UICC SE handle ,this error code seen).
* \retval   NFCSTATUS_FAILED                Request failed.
*
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_PENDING"];
*LibNfcClient<-LibNfc   [label="pInitCb"];
*--- [label="Now query for available SE's"];
*LibNfcClient=>LibNfc   [label="phLibNfc_SE_GetSecureElementList()",URL="\ref phLibNfc_SE_GetSecureElementList"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*--- [label="Now configure specific SE"];
*LibNfcClient=>LibNfc   [label="phLibNfc_SE_SetMode(hSE_Handle,)",URL="\ref phLibNfc_SE_SetMode"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_PENDING"];
*LibNfcClient<-LibNfc   [label="pSE_SetMode_Rsp_cb"];
*\endmsc 
*/
NFCSTATUS phLibNfc_SE_SetMode ( phLibNfc_Handle             hSE_Handle, 
                               phLibNfc_eSE_ActivationMode  eActivation_mode,
                               pphLibNfc_SE_SetModeRspCb_t  pSE_SetMode_Rsp_cb,
                               void *                       pContext
                               );

/**
* \ingroup grp_lib_nfc
* \brief Registers notification handler to handle secure element specific events.
*
*  This function registers handler to report SE specific transaction events.
*  Possible different types of events are as defined in \ref phLibNfc_eSE_EvtType_t.

* \param[in]  pSE_NotificationCb    pointer to notification callback.
* \param[in]  pContext              Client context which will   be included in
*                                   callback when the request is completed.
*
*\retval  NFCSTATUS_SUCCESS                 Registration Sucessful.
*\retval  NFSCSTATUS_SHUTDOWN               Shutdown in progress.
*\retval  NFCSTATUS_NOT_INITIALISED         Indicates stack is not yet initialized.
*\retval  NFCSTATUS_INVALID_PARAMETER       One or more of the supplied parameters
*                                           could not be properly interpreted.
*\retval  NFCSTATUS_FAILED                  Request failed.
*
*
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_PENDING"];
*LibNfcClient<-LibNfc   [label="pInitCb"]; 
*--- [label="Perform feature operations "];
*
*LibNfcClient=>LibNfc   [label="phLibNfc_SE_NtfRegister()",URL="\ref    phLibNfc_SE_NtfRegister"];
LibNfcClient<<LibNfc    [label="NFCSTATUS_SUCCESS"];
*--- [label="Registration sucessfull"];
*
*--- [label="In case external reader performs transactions,callback is notified as shown below"];
*LibNfcClient<-LibNfc   [label="pSE_NotificationCb"]; 
*\endmsc
*/

NFCSTATUS phLibNfc_SE_NtfRegister   (pphLibNfc_SE_NotificationCb_t  pSE_NotificationCb,
                                     void   *                       pContext
                                     );
/**
* \ingroup grp_lib_nfc
*\brief This function unregister the registered listener for SE event.
* This function unregisters the listener which  has been registered with \ref 
* phLibNfc_SE_NtfRegister.
*
*\retval  NFCSTATUS_SUCCESS                 Unregistration successful.
*\retval  NFSCSTATUS_SHUTDOWN               Shutdown in progress.
*\retval  NFCSTATUS_NOT_INITIALISED         Indicates stack is not yet initialized.
*\retval  NFCSTATUS_FAILED                  Request failed.
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_PENDING"];
*LibNfcClient<-LibNfc   [label="pInitCb"]; 
*--- [label="Perform feature operations "];
*LibNfcClient=>LibNfc   [label="phLibNfc_SE_NtfRegister()",URL="\ref    phLibNfc_SE_NtfRegister"];
LibNfcClient<<LibNfc    [label="NFCSTATUS_SUCCESS"];
*--- [label="Registration sucessfull"];
*
*--- [label="In case external reader performs transactions,callback is notified as shown below"];
*
*LibNfcClient<-LibNfc   [label="pSE_NotificationCb"]; 
*--- [label="Unregister SE notification handler in case required "];
*LibNfcClient=>LibNfc   [label="phLibNfc_SE_NtfUnregister()",URL="\ref  phLibNfc_SE_NtfUnregister"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];

*\endmsc
*/
NFCSTATUS phLibNfc_SE_NtfUnregister(void);

/**
*\ingroup   grp_lib_nfc
* \brief IOCTL interface. 
*
* The I/O Control function allows   the caller to configure specific 
* functionality provided by the lower layer.Each feature is accessible via a    
* specific IOCTL Code.
*
* \param[in]        pDriverHandle      Interface handle.This parameter is valid only for firmware download feature.
*                                   for other IOCTL features this parameter is  not relevent.
*
* \param[in]        IoctlCode       Control code for the operation. 
*                                   This value identifies the specific 
*                                   operation to be performed.For more details on supported
*                                   IOCTL codes refer to \ref grp_lib_ioctl.
*\param[in,out]     pInParam        Pointer to any input data structure 
*                                   containing data which is interpreted 
*                                   based on IoCtl code and the length of 
*                                   the data.
*
*\param[in,out]     pOutParam       Pointer to output buffer details to hold 
*                                   Ioctl specific response buffer and size of
*                                   the buffer.This buffer will be updated and
*                                   sent back as part of of callback details.
*                                   
*\param[in]         pIoCtl_Rsp_cb   Response callback registered by the caller. 
*                                                                   
* \param[in]    pContext            Client context which will   be included in
*                                   callback when the request is completed.
*
*
*\retval    NFCSTATUS_PENDING           Update in pending state. RspCB will be
*                                       called later. 
*\retval    NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be properly interpreted. 
*
\retval     NFCSTATUS_BUFFER_TOO_SMALL  The buffer supplied by the caller is to 
*\retval    NFSCSTATUS_SHUTDOWN         Shutdown in progress.
*\retval    NFCSTATUS_NOT_INITIALISED   Indicates stack is not yet initialized.
*\retval    NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                       could not be properly interpreted.
*
*\msc
*LibNfcClient,LibNfc;
*--- [label="Firmware download Scenario"];

*--- [label="Intialise Driver"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_IoCtl(pDriverHandle,)",URL="\ref phLibNfc_Mgt_IoCtl"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_PENDING"];
*LibNfcClient<-LibNfc   [label="pIoCtl_Rsp_cb()",URL="\ref pphLibNfc_IoctlCallback_t"];
**--- [label="requested IoCtl processed sucessfully "];
*
*\endmsc
*/
NFCSTATUS phLibNfc_Mgt_IoCtl    (void*                      pDriverHandle,
                                 uint16_t                   IoctlCode,
                                 phNfc_sData_t*             pInParam, 
                                 phNfc_sData_t*             pOutParam,
                                 pphLibNfc_IoctlCallback_t  pIoCtl_Rsp_cb,
                                 void*                      pContext    
                                 );

/**
* \ingroup  grp_lib_nfc
* \brief    This interface registers notification handler for target discovery.
*
* This  function allows  libNfc client to register for notifications based technology 
* type it is interested to discover. In case application is interested in multiples technology
* discovery,it can enable respective bits in \b pRegistryInfo . when Registered type target
* is discovered in RF field ,LibNfc notifies registered notification callback.
*
* \note In case this API is called multiple times ,most recent request registry details will be used 
*for registration.
* 
*\param[in] pRegistryInfo           structure contains bitwise registry information.
*                                   Specific technology type discovery can be registered if 
*                                   corresponding bit is enabled.In case bit is disabled
*                                   it indicates specific technology type unregistered.
*
*\param[in] pNotificationHandler    Notification callback.This callback will
*                                   be notified once registered target is discovered.
* \param[in]    pContext            Client context which will   be included in
*                                   callback when the request is completed.
*
* \retval NFCSTATUS_SUCCESS             Indicates registration successful.
* \retval NFCSTATUS_INVALID_PARAMETER   One or more of the supplied parameters could
*                                       not be properly interpreted.
* \retval  NFCSTATUS_NOT_INITIALISED    Indicates stack is not yet initialized.
* \retval NFSCSTATUS_SHUTDOWN           Shutdown in progress.
*
*
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<-LibNfc   [label="pInitCb()",URL="\ref pphLibNfc_RspCb_t()"];
*--- [label="Register for technology type.Ex: MIFARE UL"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_NtfRegister()",URL="\ref phLibNfc_RemoteDev_NtfRegister"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*
*\endmsc
*/

NFCSTATUS   phLibNfc_RemoteDev_NtfRegister( 
                                        phLibNfc_Registry_Info_t*       pRegistryInfo,
                                        phLibNfc_NtfRegister_RspCb_t    pNotificationHandler,
                                        void*                           pContext
                                        );


/**
* \ingroup  grp_lib_nfc
* \brief Configure Discovery Modes.
*
*This function is used to configure ,start and stop the discovery wheel. 
*Configuration includes 
*<br><br>a)Enabling/disabling of Reader phases for A,B and F technologies.
*<br>b)Configuring NFC-IP1 Initiator Speed and duration of the Emulation phase .
*
*Discovery wheel configuration based on discovery mode selected is as below.
*<br><br>1.If discovery Mode is set as \ref NFC_DISCOVERY_CONFIG then  previous configurations 
* over written by new configurations passed in \ref phLibNfc_sADD_Cfg_t and Discovery wheel
*restarts with new configurations.
*<br><br>2.If discovery Mode is set as \ref NFC_DISCOVERY_START or \ref NFC_DISCOVERY_STOP then 
* discovery parameters passed in \ref phLibNfc_sADD_Cfg_t will not be considered and previous 
*configurations still holds good.
*<br><br>3.If discovery Mode is set as \ref NFC_DISCOVERY_RESUME discovery mode starts the discovery
*wheel from where it is stopped previously.
*
*\b Note: Config types \b NFC_DISCOVERY_START, \b NFC_DISCOVERY_STOP and \b NFC_DISCOVERY_RESUME
* are not supported currently. It is for future use.
*
* \param[in]    DiscoveryMode           Discovery Mode allows to choose between: 
*                                       discovery configuration and start, stop
*                                       discovery and start discovery (with last
*                                       set configuration).For  mode details refer  to 
\ref phNfc_eDiscoveryConfigMode_t. 
* \param[in]    sADDSetup               Includes Enable/Disable discovery for 
*                                       each protocol   A,B and F.
*                                       Details refer to \ref phNfc_sADD_Cfg_t.
* \param[in]    pConfigDiscovery_RspCb  is called once the discovery wheel 
*                                       configuration is    complete. 
* \param[in]    pContext                Client context which will   be included in
*                                       callback when the request is completed.
*             
*
*\retval NFCSTATUS_PENDING                  Discovery request is in progress and result 
*                                           will be notified via callback later.
*\retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters 
*                                           could not be properly interpreted.
*\retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not initialized.
*\retval NFCSTATUS_INSUFFICIENT_RESOURCES   Insufficient resource.(Ex: insufficient memory)
*\retval NFCSTATUS_BUSY                     already discovery in progress
*                                           or it is already discovered Target  and 
*                                           connected.
*\retval NFSCSTATUS_SHUTDOWN                Shutdown in progress.
*\retval NFCSTATUS_FAILED                   Request failed.

*
*   \note :     During Reader/Initiator mode it is mandatory
*               to call \ref phLibNfc_RemoteDev_Connect before any transaction can be performed
*               with the discovered target. Even if the LibNfc client is not
*               interested in using any of the discovered targets \ref phLibNfc_RemoteDev_Connect
*               and \ref phLibNfc_RemoteDev_Disconnect should be called to restart the Discovery 
*               wheel.
*   \sa \ref phLibNfc_RemoteDev_Connect, phLibNfc_RemoteDev_Disconnect.

*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<-LibNfc   [label="pInitCb()",URL="\ref pphLibNfc_RspCb_t()"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_NtfRegister()",URL="\ref 
phLibNfc_RemoteDev_NtfRegister"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_ConfigureDiscovery()",URL="\ref phLibNfc_Mgt_ConfigureDiscovery"];
*LibNfcClient<-LibNfc   [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*--- [label="Now discovery wheel configured as requested"];
*
*\endmsc
*
*\note Response callback parameters details for this interface are as listed below.
*
* \param[in] pContext   LibNfc client context   passed in the corresponding request before.
* \param[in] status     Status of the response  callback.
*
*               \param NFCSTATUS_SUCCESS Discovery   Configuration  successful.
*               \param NFCSTATUS_SHUTDOWN       Shutdown in progress.
*               \param NFCSTATUS_FAILED         Request failed.
*/

NFCSTATUS phLibNfc_Mgt_ConfigureDiscovery (phLibNfc_eDiscoveryConfigMode_t  DiscoveryMode,   
                                           phLibNfc_sADD_Cfg_t              sADDSetup,
                                           pphLibNfc_RspCb_t                pConfigDiscovery_RspCb,
                                           void*                            pContext
                                           );


/**
* \ingroup  grp_lib_nfc
* \brief This function is used to to connect to a single Remote Device.
*
* This function is called to connect to discovered target.
* Once notification handler notified sucessfully discovered targets will be available in
* \ref phLibNfc_RemoteDevList_t .Remote device list contains valid handles for discovered 
* targets .Using this interface LibNfc client can connect to one out of 'n' discovered targets.
* A new session is started after  connect operation is successful.The session ends with a
* successful disconnect operation.Connect operation on an already connected tag Reactivates 
* the Tag.This Feature is not Valid for Jewel/Topaz Tags ,and hence a second connect if issued
* without disconnecting a Jewel/Topaz tag always Fails.
*
* \note :In case multiple targets discovered LibNfc client can connect to only one target.
*
* \param[in]     hRemoteDevice       Handle of the target device obtained during discovery process.
*
* \param[in]    pNotifyConnect_RspCb Client response callback to be to be 
*                                    notified to indicate status of the request.
*
* \param[in]    pContext             Client context which will  be included in
*                                    callback when the request is completed.
*
*\retval NFCSTATUS_PENDING           Request initiated, result will be informed via
*                                    callback.
*\retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters 
*                                    could not be properly interpreted.
*\retval NFCSTATUS_TARGET_LOST       Indicates target is lost.
*\retval NFSCSTATUS_SHUTDOWN         shutdown in progress.
*\retval NFCSTATUS_NOT_INITIALISED   Indicates stack is not yet initialized.
*\retval NFCSTATUS_INVALID_HANDLE    Target handle is invalid.
*
*\retval NFCSTATUS_FAILED            Request failed.
*
*
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<-LibNfc   [label="pInitCb()",URL="\ref pphLibNfc_RspCb_t()"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_NtfRegister()",URL="\ref phLibNfc_RemoteDev_NtfRegister"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_configureDiscovery()",URL="\ref phLibNfc_Mgt_ConfigureDiscovery"];
*LibNfcClient<-LibNfc   [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*--- [label="Now Present Tag to be  discovered"];
*LibNfcClient<-LibNfc [label="pNotificationHandler",URL="\ref phLibNfc_NtfRegister_RspCb_t"];
*LibNfcClient=>LibNfc [label="phLibNfc_RemoteDev_Connect()",URL="\ref phLibNfc_RemoteDev_Connect"];
*LibNfcClient<-LibNfc [label="pNotifyConnect_RspCb",URL="\ref pphLibNfc_ConnectCallback_t"];
*
*\endmsc
*/

NFCSTATUS phLibNfc_RemoteDev_Connect(phLibNfc_Handle                hRemoteDevice,
                                     pphLibNfc_ConnectCallback_t    pNotifyConnect_RspCb,
                                     void*                          pContext
                                     );

#ifdef RECONNECT_SUPPORT

/**
* \ingroup  grp_lib_nfc
* \brief This function is used to to connect to NEXT Remote Device.
*
* This function is called only if there are more than one remote device is detected.
* Once notification handler notified sucessfully discovered targets will be available in
* \ref phLibNfc_RemoteDevList_t .Remote device list contains valid handles for discovered 
* targets .Using this interface LibNfc client can connect to one out of 'n' discovered targets.
* A new session is started after  connect operation is successful.
* Similarly, if the user wants to connect to another handle. Libnfc client can select the handle and 
* the previously connected device is replaced by present handle. The session ends with a
* successful disconnect operation. 
* Re-Connect operation on an already connected tag Reactivates the Tag. This Feature is not 
* Valid for Jewel/Topaz Tags ,and hence a second re-connect if issued
* without disconnecting a Jewel/Topaz tag always Fails.
*
* \note :In case multiple targets discovered LibNfc client can re-connect to only one target.
*
* \param[in]     hRemoteDevice       Handle of the target device obtained during discovery process.
*
* \param[in]    pNotifyReConnect_RspCb Client response callback to be to be 
*                                    notified to indicate status of the request.
*
* \param[in]    pContext             Client context which will  be included in
*                                    callback when the request is completed.
*
*\retval NFCSTATUS_PENDING           Request initiated, result will be informed via
*                                    callback.
*\retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters 
*                                    could not be properly interpreted.
*\retval NFCSTATUS_TARGET_LOST       Indicates target is lost.
*\retval NFSCSTATUS_SHUTDOWN         shutdown in progress.
*\retval NFCSTATUS_NOT_INITIALISED   Indicates stack is not yet initialized.
*\retval NFCSTATUS_INVALID_HANDLE    Target handle is invalid.
*
*\retval NFCSTATUS_FAILED            Request failed.
*
*
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<-LibNfc   [label="pInitCb()",URL="\ref pphLibNfc_RspCb_t()"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_NtfRegister()",URL="\ref phLibNfc_RemoteDev_NtfRegister"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_configureDiscovery()",URL="\ref phLibNfc_Mgt_ConfigureDiscovery"];
*LibNfcClient<-LibNfc   [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*--- [label="Now Present multiple protocol Tag to be  discovered"];
*LibNfcClient<-LibNfc [label="pNotificationHandler",URL="\ref phLibNfc_NtfRegister_RspCb_t"];
*--- [label="TWO remote device information is received, So connect with one handle"];
*LibNfcClient=>LibNfc [label="phLibNfc_RemoteDev_Connect()",URL="\ref phLibNfc_RemoteDev_Connect"];
*LibNfcClient<-LibNfc [label="pNotifyConnect_RspCb",URL="\ref pphLibNfc_ConnectCallback_t"];
*--- [label="Connect is successful, so transact using this handle. Now if user wants to switch to another handle then call Reconnect "];
*LibNfcClient=>LibNfc [label="phLibNfc_RemoteDev_ReConnect()",URL="\ref phLibNfc_RemoteDev_ReConnect"];
*LibNfcClient<-LibNfc [label="pNotifyReConnect_RspCb",URL="\ref pphLibNfc_ConnectCallback_t"];
*
*\endmsc
*/
NFCSTATUS 
phLibNfc_RemoteDev_ReConnect (
    phLibNfc_Handle                 hRemoteDevice,
    pphLibNfc_ConnectCallback_t     pNotifyReConnect_RspCb,
    void                            *pContext);

#endif /* #ifdef RECONNECT_SUPPORT */

/**
* \ingroup  grp_lib_nfc
* \brief This interface allows to perform Read/write operation on remote device.
*
* This function allows to send data to and receive data 
* from the target selected by libNfc client.It is also used by the 
* NFCIP1 Initiator while performing a transaction with the NFCIP1 target.
* The LibNfc client  has to provide the handle of the target and the 
* command in order to communicate with the selected remote device.
*
*
*\param[in] hRemoteDevice       handle of the remote device.This handle to be 
*                               same as as handle obtained for specific remote device 
*                               during device discovery.
* \param[in] psTransceiveInfo   Information required by transceive  is concealed in 
*                               this structure.It   contains send,receive buffers 
*                               and command specific details.
*
*
* \param[in] pTransceive_RspCb   Callback function for returning the received response 
*                                or error.
* \param[in]    pContext         Client context which will  be included in
*                                callback when the request is completed.
*
* \retval NFCSTATUS_PENDING      Request    initiated, result will be informed through 
*                                the callback.
*   \retval NFCSTATUS_INVALID_PARAMETER     One or more of the supplied parameters could
*                                           not be properly interpreted or  invalid.
*   \retval NFCSTATUS_COMMAND_NOT_SUPPORTED The command is not supported.
*   \retval NFSCSTATUS_SHUTDOWN             shutdown in progress.
*   \retval NFCSTATUS_TARGET_LOST           Indicates target is lost.
*   \retval NFCSTATUS_TARGET_NOT_CONNECTED            The Remote Device is not connected.
*   \retval NFCSTATUS_INVALID_HANDLE        Target  handle is invalid
*   \retval NFCSTATUS_NOT_INITIALISED       Indicates stack is not yet initialized.
*   \retval NFCSTATUS_REJECTED              Indicates invalid request.
*   \retval NFCSTATUS_FAILED                Request failed.
*
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<-LibNfc   [label="pInitCb()",URL="\ref pphLibNfc_RspCb_t()"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_NtfRegister()",URL="\ref phLibNfc_RemoteDev_NtfRegister"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_configureDiscovery()",URL="\ref phLibNfc_Mgt_ConfigureDiscovery"];
*LibNfcClient<-LibNfc   [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*--- [label="Now Present Tag to be  discovered"];
*LibNfcClient<-LibNfc [label="pNotificationHandler",URL="\ref phLibNfc_NtfRegister_RspCb_t"];
*LibNfcClient=>LibNfc [label="phLibNfc_RemoteDev_Connect()",URL="\ref phLibNfc_RemoteDev_Connect"];
*LibNfcClient<-LibNfc [label="pNotifyConnect_RspCb",URL="\ref pphLibNfc_ConnectCallback_t"];
*--- [label="Now perform transceive operation"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_Transceive()",URL="\ref  phLibNfc_RemoteDev_Transceive "];
*LibNfcClient<-LibNfc   [label="pTransceive_RspCb",URL="\ref pphLibNfc_TransceiveCallback_t"];
*
*\endmsc
*/

NFCSTATUS phLibNfc_RemoteDev_Transceive(phLibNfc_Handle                 hRemoteDevice,
                                        phLibNfc_sTransceiveInfo_t*     psTransceiveInfo,
                                        pphLibNfc_TransceiveCallback_t  pTransceive_RspCb,
                                        void*                           pContext
                                        );

/**
*\ingroup   grp_lib_nfc
*\brief Allows to disconnect from already connected target.
*
*  The function allows to disconnect from from already connected target. This 
*  function closes the session opened during connect operation.The status of discovery 
*  wheel after disconnection is determined by the \ref phLibNfc_eReleaseType_t parameter. 
*  it is also used to switch from wired to virtual mode in case the discovered
*  device is SmartMX in wired mode. 
*
*\param[in]  hRemoteDevice              handle of the target device.This handle to be 
*                                       same as as handle obtained for specific remote device 
*                                       during device discovery.
* \param[in] ReleaseType                Release mode to be  used while 
*                                       disconnecting from target.Refer \ref phLibNfc_eReleaseType_t
*                                       for possible release types.
*\param[in] pDscntCallback              Client response callback to be  to be notified 
to indicate status of the request.
* \param[in]    pContext                Client context which will   be included in
*                                       callback when the request is completed.

*\retval    NFCSTATUS_PENDING                Request initiated,  result will be informed through 
the callback.
*\retval    NFCSTATUS_INVALID_PARAMETER      One or  more of the supplied parameters could not be
*                                            properly interpreted.
*\retval    NFCSTATUS_TARGET_NOT_CONNECTED   The Remote Device is not connected.
*\retval    NFCSTATUS_NOT_INITIALISED        Indicates stack is not yet initialized.
* \retval   NFCSTATUS_INVALID_HANDLE         Target  handle is invalid.
*\retval    NFSCSTATUS_SHUTDOWN              Shutdown in progress.
*\retval    NFCSTATUS_REJECTED               Indicates previous disconnect in progress.
* \retval   NFCSTATUS_BUSY                   Indicates can not disconnect due to outstanding transaction in progress.
* \retval   NFCSTATUS_FAILED                 Request failed.

*
*
*
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<-LibNfc   [label="pInitCb()",URL="\ref pphLibNfc_RspCb_t()"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_NtfRegister()",URL="\ref phLibNfc_RemoteDev_NtfRegister"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_configureDiscovery()",URL="\ref phLibNfc_Mgt_ConfigureDiscovery"];
*LibNfcClient<-LibNfc   [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*--- [label="Now Present Tag to be  discovered"];
*LibNfcClient<-LibNfc [label="pNotificationHandler",URL="\ref phLibNfc_NtfRegister_RspCb_t"];
*LibNfcClient=>LibNfc [label="phLibNfc_RemoteDev_Connect()",URL="\ref phLibNfc_RemoteDev_Connect"];
*LibNfcClient<-LibNfc [label="pNotifyConnect_RspCb",URL="\ref pphLibNfc_ConnectCallback_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_Transceive()",URL="\ref  phLibNfc_RemoteDev_Transceive"];
*LibNfcClient<-LibNfc   [label="pTransceive_RspCb",URL="\ref pphLibNfc_TransceiveCallback_t"];
*--- [label="Once transceive is completed Now disconnect"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_Disconnect()",URL="\ref  phLibNfc_RemoteDev_Disconnect"];
*LibNfcClient<-LibNfc   [label="pDscntCallback",URL="\ref pphLibNfc_RspCb_t"];
*
*\endmsc
*/
NFCSTATUS phLibNfc_RemoteDev_Disconnect( phLibNfc_Handle                 hRemoteDevice,
                                        phLibNfc_eReleaseType_t          ReleaseType,
                                        pphLibNfc_DisconnectCallback_t   pDscntCallback,
                                        void*                            pContext
										);



/**
* \ingroup  grp_lib_nfc
*\brief This interface unregisters notification handler for target discovery.
*
* This  function unregisters the listener which has been registered with
* phLibNfc_RemoteDev_NtfUnregister() before. After  this call the callback
* function  won't be called anymore. If nothing is  registered the 
* function  still succeeds
* \retval NFCSTATUS_SUCCESS          callback unregistered.
* \retval NFCSTATUS_SHUTDOWN         Shutdown in progress.
*\retval  NFCSTATUS_NOT_INITIALISED Indicates stack is not yet initialized.
*
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<-LibNfc   [label="pInitCb()",URL="\ref pphLibNfc_RspCb_t()"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_NtfRegister()",URL="\ref phLibNfc_RemoteDev_NtfRegister"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*--- [label="Perform operations"];
*--- [label="In case required unregister now"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_NtfUnregister()",URL="\ref phLibNfc_RemoteDev_NtfUnregister"];
*
*\endmsc
*/

NFCSTATUS phLibNfc_RemoteDev_NtfUnregister(void);

/**
* \ingroup  grp_lib_nfc
* \brief Check  for target presence.
* This  function checks ,given target is present in RF filed or not.
* Client can make  use of this API to check  periodically discovered
* tag is present in RF field or not.
*
*
*\param[in]  hRemoteDevice          handle of the target device.This handle to be 
*                                   same as as handle obtained for specific remote device 
*                                   during device discovery.
* \param[in] pPresenceChk_RspCb     callback function called on completion  of the
*                                   presence check or in case an error has occurred.
* \param[in]    pContext            Client context which will   be included in
*                                   callback when the request is completed.
*
* \retval  NFCSTATUS_PENDING        presence check  started. Status will be notified
*                                   via callback.
*
* \retval  NFCSTATUS_NOT_INITIALISED        Indicates stack is not initialized.
* \retval  NFCSTATUS_INVALID_PARAMETER      One or more of the supplied parameters could
*                                           not be properly interpreted.
* \retval  NFCSTATUS_TARGET_NOT_CONNECTED   The Remote Device is not connected.
* \retval  NFCSTATUS_INVALID_HANDLE         Target  handle is invalid
* \retval  NFCSTATUS_SHUTDOWN               Shutdown in progress.
* \retval  NFCSTATUS_FAILED                 Request failed.
*
*
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<-LibNfc   [label="pInitCb()",URL="\ref pphLibNfc_RspCb_t()"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_NtfRegister()",URL="\ref phLibNfc_RemoteDev_NtfRegister"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_configureDiscovery()",URL="\ref phLibNfc_Mgt_ConfigureDiscovery"];
*LibNfcClient<-LibNfc   [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*--- [label="Now Present Tag to be  discovered"];
*LibNfcClient<-LibNfc [label="pNotificationHandler",URL="\ref phLibNfc_NtfRegister_RspCb_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_Connect()",URL="\ref phLibNfc_RemoteDev_Connect"];
*LibNfcClient<-LibNfc   [label="pNotifyConnect_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_CheckPresence()",URL="\ref phLibNfc_RemoteDev_CheckPresence"];
*LibNfcClient<-LibNfc   [label="pPresenceChk_RspCb",URL="\ref   pphLibNfc_RspCb_t"];
*
*\endmsc
*
*\note Response callback parameters details for this interface are as listed below.
*
* \param[in] pContext   LibNfc client context   passed in the corresponding request before.
* \param[in] status     Status of the response  callback.
*
*                  \param NFCSTATUS_SUCCESS          Successful,indicates tag is present in RF field.
*                  \param NFCSTATUS_TARGET_LOST Indicates target is lost.
*                  \param NFCSTATUS_SHUTDOWN    Shutdown in progress.
*                  \param NFCSTATUS_FAILED      Request failed.
*
*/
NFCSTATUS phLibNfc_RemoteDev_CheckPresence( phLibNfc_Handle     hRemoteDevice,
                                           pphLibNfc_RspCb_t    pPresenceChk_RspCb,
                                           void*                pContext
                                           );

/**
* \ingroup  grp_lib_nfc
*\brief Allows  to check connected tag is NDEF compliant or not.
* This function allows  to validate connected tag is NDEF compliant or  not.
*
*\param[in] hRemoteDevice       handle of the remote device.This handle to be 
*                               same as as handle obtained for specific remote device 
*                               during device discovery.
*\param[in] pCheckNdef_RspCb    Response callback defined by the caller.    
*\param[in] pContext            Client context which will   be included in
*                               callback when the request is completed.
*
* \retval NFCSTATUS_PENDING            The action has been successfully triggered.
* \retval NFCSTATUS_INVALID_PARAMETER  At least one parameter of the function 
*                                      is invalid.
* \retval NFCSTATUS_TARGET_LOST        Indicates target is lost
* \retval NFCSTATUS_TARGET_NOT_CONNECTED         The Remote Device is not connected.
* \retval NFCSTATUS_INVALID_HANDLE     Target   handle is invalid
* \retval NFCSTATUS_SHUTDOWN           Shutdown in progress.
* \retval  NFCSTATUS_FAILED            Request failed.
*
*
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<-LibNfc   [label="pInitCb()",URL="\ref pphLibNfc_RspCb_t()"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_NtfRegister()",URL="\ref phLibNfc_RemoteDev_NtfRegister"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_configureDiscovery()",URL="\ref phLibNfc_Mgt_ConfigureDiscovery"];
*LibNfcClient<-LibNfc   [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*--- [label="Now Present NDEF complaint Tag Type"];
*LibNfcClient<-LibNfc [label="pNotificationHandler",URL="\ref phLibNfc_NtfRegister_RspCb_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_Connect()",URL="\ref phLibNfc_RemoteDev_Connect"];
*LibNfcClient<-LibNfc   [label="pNotifyConnect_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Ndef_CheckNdef()",URL="\ref phLibNfc_Ndef_CheckNdef "];
*LibNfcClient<-LibNfc   [label="pCheckNdef_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*
*\endmsc
*
*\note Response callback parameters details for this interface are as listed below.
*
* \param[in] pContext   LibNfc client context   passed in the corresponding request before.
* \param[in] status     Status of the response  callback.
*
*                  \param NFCSTATUS_SUCCESS             Successful and tag is NDEF compliant .
*                  \param NFCSTATUS_TARGET_LOST         NDEF check operation is failed because of target is	**														lost.
*                  \param NFCSTATUS_SHUTDOWN            Shutdown in progress.
*                  \param NFCSTATUS_ABORTED             Aborted due to disconnect operation in between.
*                  \param NFCSTATUS_FAILED              Request failed.
*/

NFCSTATUS phLibNfc_Ndef_CheckNdef(phLibNfc_Handle              hRemoteDevice,
                                  pphLibNfc_ChkNdefRspCb_t     pCheckNdef_RspCb,
                                  void*                        pContext);

/**
* \ingroup  grp_lib_nfc
* \brief Read NDEF  message from a Tag.
* This  function reads an NDEF message from already connected tag. 
* the NDEF  message is read starting after the position of the last read operation 
* of the same tag during current session.
* If it's FALSE the NDEF message is read from starting  of the NDEF message.
* If the call returns with NFCSTATUS_PENDING , a response callback pNdefRead_RspCb is 
* called ,when the read operation is complete.
*
*\note Before issuing NDEF read operation LibNfc client should perform NDEF check operation
* using \ref phLibNfc_Ndef_CheckNdef interface.
* If the call back error code is NFCSTATUS_FAILED then the LIBNFC client has to do the 
* phLibNfc_RemoteDev_CheckPresence to find , its communication error or target lost.
*
*\param[in]  hRemoteDevice          handle of the remote device.This handle to be 
*                                   same as as handle obtained for specific remote device 
*                                   during device discovery.
*   \param[in]  psRd                Pointer to  the  read buffer info.
*   \param[in]  Offset              Reading Offset  : phLibNfc_Ndef_EBegin means from the 
*                                   beginning, phLibNfc_Ndef_ECurrent means from the 
*                                   current offset.     
*   \param[in]  pNdefRead_RspCb     Response callback defined by the caller. 
*   \param[in]  pContext            Client context which will   be included in
*                                   callback when the request is completed.
*
* \retval NFCSTATUS_SUCCESS             NDEF read operation successful.
* \retval NFCSTATUS_PENDING             Request accepted and started
* \retval NFCSTATUS_SHUTDOWN            Shutdown in progress
* \retval NFCSTATUS_INVALID_HANDLE      Target  handle is invalid
* \retval NFCSTATUS_NOT_INITIALISED     Indicates stack is not yet initialized.
* \retval NFCSTATUS_INVALID_PARAMETER   One or more of the supplied parameters could not 
*                                       be properly interpreted.
* \retval NFCSTATUS_TARGET_NOT_CONNECTED          The Remote Device is not connected. 
* \retval NFCSTATUS_FAILED              Read operation failed since tag does not contain NDEF data.
* \retval NFCSTATUS_NON_NDEF_COMPLIANT  Tag is not Ndef Compliant.
* \param NFCSTATUS_REJECTED             Rejected due to NDEF read issued on non 
*                                       ,or Ndef check has not been performed 
*                                       before the readNDEF tag.
*
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<-LibNfc   [label="pInitCb()",URL="\ref pphLibNfc_RspCb_t()"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_NtfRegister()",URL="\ref phLibNfc_RemoteDev_NtfRegister"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_configureDiscovery()",URL="\ref phLibNfc_Mgt_ConfigureDiscovery"];
*LibNfcClient<-LibNfc   [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*--- [label="Now Present NDEF complaint Tag Type"];
*LibNfcClient<-LibNfc [label="pNotificationHandler",URL="\ref phLibNfc_NtfRegister_RspCb_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_Connect()",URL="\ref phLibNfc_RemoteDev_Connect"];
*LibNfcClient<-LibNfc   [label="pNotifyConnect_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Ndef_CheckNdef()",URL="\ref phLibNfc_Ndef_CheckNdef "];
*LibNfcClient<-LibNfc   [label="pCheckNdef_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Ndef_Read()",URL="\ref phLibNfc_Ndef_Read "];
*LibNfcClient<-LibNfc   [label="pNdefRead_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*
*\endmsc
*
*\note Response callback parameters details for this interface are as listed below.
*
* \param[in] pContext   LibNfc client context   passed in the corresponding request before.
* \param[in] status     Status of the response  callback.
*
*                  \param NFCSTATUS_SUCCESS             NDEF read operation successful.
*                  \param NFCSTATUS_SHUTDOWN            Shutdown in progress.
*                  \param NFCSTATUS_ABORTED             Aborted due to disconnect operation in between.
*                  \param NFCSTATUS_FAILED              Request failed.
*/
NFCSTATUS phLibNfc_Ndef_Read(phLibNfc_Handle                   hRemoteDevice,
                            phNfc_sData_t*                     psRd,
                            phLibNfc_Ndef_EOffset_t            Offset,
                            pphLibNfc_RspCb_t                  pNdefRead_RspCb,
                            void*                              pContext
                            );
/**
**  \ingroup grp_lib_nfc
*
* \brief Write  NDEF data to  NFC tag.
*
* This function allows the client to write a NDEF data to already connected NFC tag.
* Function writes a complete NDEF   message to a tag. If a NDEF message already 
* exists in the tag, it will be overwritten. When the transaction is complete,
* a notification callback is notified.
*   
*\note Before issuing NDEF write operation LibNfc client should perform NDEF check operation
* using \ref phLibNfc_Ndef_CheckNdef interface.
*
*\param[in] hRemoteDevice           handle of the remote device.This handle to be 
*                                   same as as handle obtained for specific remote device 
*                                   during device discovery.
*\param[in] psWr                    Ndef    Buffer to write. If NdefMessageLen is set to 0 
*                                   and pNdefMessage    = NULL, the NFC library will erase
*                                   tag internally.
*\param[in] pNdefWrite_RspCb        Response callback defined by the caller. 
*\param[in] pContext                Client context which will   be included in
*                                   callback when the request is completed.
*
*\note If \ref phNfc_sData_t.NdefMessageLen is 0 bytes,  this function will erase all
*current NDEF data present in the tag. Any non-zero length buffer size
*will attempt to write  NEDF data onto the tag.
* If the call back error code is NFCSTATUS_FAILED then the LIBNFC client has to do the 
* phLibNfc_RemoteDev_CheckPresence to find , its communication error or target lost.
*
*
* \retval NFCSTATUS_PENDING             Request accepted and started.
* \retval NFCSTATUS_SHUTDOWN            Shutdown in progress.
* \retval NFCSTATUS_INVALID_HANDLE      Target  handle is invalid.
* \retval NFCSTATUS_NOT_INITIALISED     Indicates stack is not yet initialized.
* \retval NFCSTATUS_INVALID_PARAMETER   One or more of the supplied parameters could not 
*                                       be  properly interpreted.
* \retval NFCSTATUS_NON_NDEF_COMPLIANT  Tag is not Ndef Compliant.
* \retval NFCSTATUS_TARGET_NOT_CONNECTED            The Remote Device is not connected.                
* \retval NFCSTATUS_REJECTED            Rejected due to NDEF write issued without 
*                                       performing a CheckNdef().
* \retval NFCSTATUS_FAILED              operation failed.
*
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<-LibNfc   [label="pInitCb()",URL="\ref pphLibNfc_RspCb_t()"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_NtfRegister()",URL="\ref phLibNfc_RemoteDev_NtfRegister"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_configureDiscovery()",URL="\ref phLibNfc_Mgt_ConfigureDiscovery"];
*LibNfcClient<-LibNfc   [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*--- [label="Now Present NDEF Tag "];
*LibNfcClient<-LibNfc [label="pNotificationHandler",URL="\ref phLibNfc_NtfRegister_RspCb_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_Connect()",URL="\ref phLibNfc_RemoteDev_Connect"];
*LibNfcClient<-LibNfc   [label="pNotifyConnect_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Ndef_CheckNdef()",URL="\ref phLibNfc_Ndef_CheckNdef "];
*LibNfcClient<-LibNfc   [label="pCheckNdef_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Ndef_Write()",URL="\ref phLibNfc_Ndef_Write "];
*LibNfcClient<-LibNfc   [label="pNdefWrite_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*
*\endmsc
*
*\note Response callback parameters details for this interface are as listed below.
*
* \param[in] pContext   LibNfc client context   passed in the corresponding request before.
* \param[in] status     Status of the response  callback.
*
*                  \param NFCSTATUS_SUCCESS                  NDEF write operation is successful.
*                  \param NFCSTATUS_SHUTDOWN            Shutdown in progress.
*                  \param NFCSTATUS_ABORTED,            Aborted due to disconnect operation in between.
*                  \param NFCSTATUS_NOT_ENOUGH_MEMORY   Requested no of bytes to be writen exceeds size of the memory available on the tag.
*                  \param NFCSTATUS_FAILED              Request failed.
*/

NFCSTATUS phLibNfc_Ndef_Write (phLibNfc_Handle          hRemoteDevice,
                               phNfc_sData_t*           psWr,
                               pphLibNfc_RspCb_t        pNdefWrite_RspCb,
                               void*                    pContext
                               );



/**
* \ingroup grp_lib_nfc
*
* \brief Format target.
*
* This function allows the LibNfc client to perform  NDEF formating operation on discovered target.
This function formats given target 
*
*\note 
* <br>1. Prior to formating it is recommended to perform NDEF check using \ref phLibNfc_Ndef_CheckNdef interface.
* <br>2. formatting feature supported only for MIFARE Std,MIFARE UL and Desfire tag types.
* If the call back error code is NFCSTATUS_FAILED then the LIBNFC client has to do the 
* phLibNfc_RemoteDev_CheckPresence to find , its communication error or target lost.
*
*\param[in]  hRemoteDevice          handle of the remote device.This handle to be 
*                                   same as as handle obtained for specific remote device 
*                                   during device discovery.
*\param[in] pScrtKey                info containing the secret key data
*                                   and  Secret key buffer length. 
*
*\param[in] pNdefformat_RspCb       Response    callback defined by the caller. 
*\param[in] pContext                Client context which will   be included in
*                                   callback when the request is completed.
*
*
* \retval NFCSTATUS_PENDING                 Request accepted and started.
* \retval NFCSTATUS_SHUTDOWN                Shutdown in progress.
* \retval NFCSTATUS_INVALID_HANDLE          Target  handle is invalid.
* \retval NFCSTATUS_NOT_INITIALISED         Indicates stack is not yet initialized.
* \retval NFCSTATUS_INVALID_PARAMETER       One or more of the supplied parameters could not 
*                                           be  properly interpreted.
* \retval NFCSTATUS_TARGET_NOT_CONNECTED    The Remote Device is not connected. 
* \retval NFCSTATUS_FAILED                  operation failed.
* \retval NFCSTATUS_REJECTED                Tag is already  formatted one.
*
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<-LibNfc   [label="pInitCb()",URL="\ref pphLibNfc_RspCb_t()"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_NtfRegister()",URL="\ref phLibNfc_RemoteDev_NtfRegister"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_configureDiscovery()",URL="\ref phLibNfc_Mgt_ConfigureDiscovery"];
*LibNfcClient<-LibNfc   [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*--- [label="Now Present non NDEF Tag "];
*LibNfcClient<-LibNfc [label="pNotificationHandler",URL="\ref phLibNfc_NtfRegister_RspCb_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_Connect()",URL="\ref phLibNfc_RemoteDev_Connect"];
*LibNfcClient<-LibNfc   [label="pNotifyConnect_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Ndef_CheckNdef()",URL="\ref phLibNfc_Ndef_CheckNdef "];
*LibNfcClient<-LibNfc   [label="pCheckNdef_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*--- [label="Tag found to be non NDEF compliant ,now format it"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_FormatNdef()",URL="\ref  phLibNfc_RemoteDev_FormatNdef   "];
*LibNfcClient<-LibNfc   [label="pNdefformat_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*
*\endmsc
*
*\note Response callback parameters details for this interface are as listed below.
*
* \param[in] pContext   LibNfc client context   passed in the corresponding request before.
* \param[in] status     Status of the response  callback.
*
*                  \param NFCSTATUS_SUCCESS                  NDEF formatting operation is successful.
*                  \param NFCSTATUS_SHUTDOWN            Shutdown in progress.
*                  \param NFCSTATUS_ABORTED,            Aborted due to disconnect operation in between.
*                  \param NFCSTATUS_FAILED              Request failed.
*/

NFCSTATUS phLibNfc_RemoteDev_FormatNdef(phLibNfc_Handle         hRemoteDevice,
                                        phNfc_sData_t*          pScrtKey,  
                                        pphLibNfc_RspCb_t       pNdefformat_RspCb,
                                        void*                   pContext
                                        );

#ifdef LIBNFC_READONLY_NDEF
/**
* \ingroup grp_lib_nfc
*
* \brief To convert a already formatted NDEF READ WRITE tag to READ ONLY.
*
* This function allows the LibNfc client to convert a already formatted NDEF READ WRITE
* tag to READ ONLY on discovered target.
*
*\note
* <br>1. Prior to formating it is recommended to perform NDEF check using \ref phLibNfc_Ndef_CheckNdef interface.
* <br>2. READ ONLY feature supported only for MIFARE UL and Desfire tag types.
* If the call back error code is NFCSTATUS_FAILED then the LIBNFC client has to do the
* phLibNfc_RemoteDev_CheckPresence to find, its communication error or target lost.
*
*\param[in] hRemoteDevice           handle of the remote device.This handle to be
*                                   same as as handle obtained for specific remote device
*                                   during device discovery.
*\param[in] pScrtKey                Key to be used for making Mifare read only. This parameter is
*                                   unused in case of readonly for other cards.
*\param[in] pNdefReadOnly_RspCb     Response callback defined by the caller.
*\param[in] pContext                Client context which will be included in
*                                   callback when the request is completed.
*
*
* \retval NFCSTATUS_PENDING                 Request accepted and started.
* \retval NFCSTATUS_SHUTDOWN                Shutdown in progress.
* \retval NFCSTATUS_INVALID_HANDLE          Target  handle is invalid.
* \retval NFCSTATUS_NOT_INITIALISED         Indicates stack is not yet initialized.
* \retval NFCSTATUS_INVALID_PARAMETER       One or more of the supplied parameters could not
*                                           be  properly interpreted.
* \retval NFCSTATUS_TARGET_NOT_CONNECTED    The Remote Device is not connected.
* \retval NFCSTATUS_FAILED                  operation failed.
* \retval NFCSTATUS_REJECTED                Tag is already  formatted one.
*
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<-LibNfc   [label="pInitCb()",URL="\ref pphLibNfc_RspCb_t()"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_NtfRegister()",URL="\ref phLibNfc_RemoteDev_NtfRegister"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_configureDiscovery()",URL="\ref phLibNfc_Mgt_ConfigureDiscovery"];
*LibNfcClient<-LibNfc   [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*--- [label="Now Present NDEF Tag "];
*LibNfcClient<-LibNfc [label="pNotificationHandler",URL="\ref phLibNfc_NtfRegister_RspCb_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_Connect()",URL="\ref phLibNfc_RemoteDev_Connect"];
*LibNfcClient<-LibNfc   [label="pNotifyConnect_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Ndef_CheckNdef()",URL="\ref phLibNfc_Ndef_CheckNdef "];
*LibNfcClient<-LibNfc   [label="pCheckNdef_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*--- [label="Tag found to be NDEF compliant ,now convert the tag to read only"];
*LibNfcClient=>LibNfc   [label="phLibNfc_ConvertToReadOnlyNdef()",URL="\ref  phLibNfc_ConvertToReadOnlyNdef   "];
*LibNfcClient<-LibNfc   [label="pNdefReadOnly_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*
*\endmsc
*
*\note Response callback parameters details for this interface are as listed below.
*
* \param[in] pContext   LibNfc client context   passed in the corresponding request before.
* \param[in] status     Status of the response  callback.
*
*                  \param NFCSTATUS_SUCCESS             Converting the tag to READ ONLY NDEF is successful.
*                  \param NFCSTATUS_SHUTDOWN            Shutdown in progress.
*                  \param NFCSTATUS_ABORTED,            Aborted due to disconnect operation in between.
*                  \param NFCSTATUS_FAILED              Request failed.
*/

NFCSTATUS phLibNfc_ConvertToReadOnlyNdef (phLibNfc_Handle         hRemoteDevice,
                                          phNfc_sData_t*          pScrtKey,
                                          pphLibNfc_RspCb_t       pNdefReadOnly_RspCb,
                                          void*                   pContext
                                        );
#endif /* #ifdef LIBNFC_READONLY_NDEF */

/**
* \ingroup grp_lib_nfc
* \brief <b>Search for NDEF Record type</b>.
*  
*  This function allows  LibNfc client to search NDEF content based on TNF value and type \n
*
*This API allows to find NDEF records based on  RTD (Record Type Descriptor) info.
*LibNfc internally parses NDEF content based registration type registered.
*In case there is match LibNfc notifies LibNfc client with NDEF information details.
*LibNfc client can search a new NDEF registration type once the previous call is handled.
*
*\param[in]     hRemoteDevice       Handle of the remote device.This handle to be 
*                                   same as as handle obtained for specific remote device 
*                                   during device discovery.
*\param[in]     psSrchTypeList      List of NDEF records to be looked in based on TNF value and type.
*                                   For NDEF search type refer to \ref phLibNfc_Ndef_SrchType.
*                                   If this set to NULL then it means that libNfc client interested in
*                                   all possible NDEF records.
*
*\param[in]     uNoSrchRecords      Indicates no of NDEF records in requested list as mentioned 
*                                   in psSrchTypeList.
*\param[in]     pNdefNtfRspCb       Response callback defined by the caller.    
*\param[in]     pContext            Client context which will   be included in
*                                   callback when callback is notified.
*
*
* \retval NFCSTATUS_SUCCESS             Indicates NDEF notification registration successful.
* \retval NFCSTATUS_SHUTDOWN            Shutdown in progress.
* \retval  NFCSTATUS_NOT_INITIALISED    Indicates stack is not yet initialized.
* \retval NFCSTATUS_INVALID_HANDLE      Target  handle is invalid.
* \retval NFCSTATUS_INVALID_PARAMETER   One or more of the supplied parameters could not 
*                                       be  properly interpreted.
* \retval NFCSTATUS_TARGET_NOT_CONNECTED          The Remote Device is not connected. 
* \retval NFCSTATUS_FAILED              operation failed.
* \retval NFCSTATUS_BUSY                Previous request in progress can not accept new request.   
*
* \retval NFCSTATUS_ABORTED             Aborted due to disconnect request in between.
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<-LibNfc   [label="pInitCb()",URL="\ref pphLibNfc_RspCb_t()"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_NtfRegister()",URL="\ref phLibNfc_RemoteDev_NtfRegister"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_configureDiscovery()",URL="\ref phLibNfc_Mgt_ConfigureDiscovery"];
*LibNfcClient<-LibNfc   [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*--- [label="Now Present NDEF Tag "];
*LibNfcClient<-LibNfc [label="pNotificationHandler",URL="\ref phLibNfc_NtfRegister_RspCb_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_Connect()",URL="\ref phLibNfc_RemoteDev_Connect"];
*LibNfcClient<-LibNfc   [label="pNotifyConnect_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Ndef_CheckNdef()",URL="\ref phLibNfc_Ndef_CheckNdef "];
*LibNfcClient<-LibNfc   [label="pCheckNdef_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Ndef_SearchNdefContent()",URL="\ref phLibNfc_Ndef_SearchNdefContent"];
*LibNfcClient<-LibNfc   [label="pNdefNtfRspCb",URL="\ref pphLibNfc_Ndef_Search_RspCb_t()"];
*\endmsc
*
*\note Response callback parameters details for this interface are as listed below.
*
* \param[in] pContext   LibNfc client context   passed in the corresponding request before.
* \param[in] status     Status of the response  callback.
*
*/
NFCSTATUS phLibNfc_Ndef_SearchNdefContent(  
                                phLibNfc_Handle                 hRemoteDevice,
                                phLibNfc_Ndef_SrchType_t*       psSrchTypeList,  
                                uint8_t                         uNoSrchRecords,
                                pphLibNfc_Ndef_Search_RspCb_t   pNdefNtfRspCb,  
                                void *                          pContext   
                                );




/**
* \ingroup grp_lib_nfc
* \brief <b> Interface used to receive data from initiator at target side during P2P communication</b>.
*
*This function  Allows the NFC-IP1 target to retrieve data/commands coming from the 
*Initiator.Once this function is called by LibNfc client on target side it waits for 
*receiving data from initiator.It is used by libNfc client which acts as target during P2P
*communication.
*
*\note : Once this API is called,its mandatory to wait for receive 
*\ref pphLibNfc_Receive_RspCb_t callback notification,before calling any other 
*API.Only function allowed is \ref phLibNfc_Mgt_DeInitialize.
*
*  \param[in]     hRemoteDevice         Peer handle obtained during device discovery process.
*                                       
*  \param[in]     pReceiveRspCb         Callback function called after receiving 
*                                       the data or in case an error has 
*                                       has occurred.
*
*  \param[in]     pContext              Upper layer context to be returned 
*                                       in the callback.
*
*  \retval NFCSTATUS_PENDING            Receive operation is in progress.
*  \retval NFCSTATUS_INVALID_PARAMETER  One or more of the supplied parameters
*                                       could not be properly interpreted.
* \retval  NFCSTATUS_NOT_INITIALISED    Indicates stack is not yet initialized.
* \retval  NFCSTATUS_SHUTDOWN           Shutdown in progress.
* \retval  NFCSTATUS_INVALID_DEVICE     The device has been disconnected meanwhile.
* \retval  NFCSTATUS_DESELECTED         Receive operation is not possible due to
*                                       initiator issued disconnect or intiator 
*                                       physically removed from the RF field.
*
*\retval   NFCSTATUS_REJECTED           Indicates invalid request.
*\retval   NFCSTATUS_FAILED             Request failed.
*
*\msc 
*P2PInitiatorClient,InitiatorLibNfc,P2PTargetLibNfc,P2PTargetClient;
*--- [label="stack is intialised and P2P notification handler registered alredy"];
*P2PTargetClient=>P2PTargetLibNfc   [label="phLibNfc_Mgt_SetP2P_ConfigParams()",URL="\ref    phLibNfc_Mgt_SetP2P_ConfigParams"];
*P2PTargetClient<<P2PTargetLibNfc   [label="NFCSTATUS_PENDING"];
*P2PTargetClient<-P2PTargetLibNfc   [label="pConfigRspCb()",URL="\ref pphLibNfc_RspCb_t"];
*P2PInitiatorClient=>InitiatorLibNfc    [label="phLibNfc_Mgt_SetP2P_ConfigParams()",URL="\ref    phLibNfc_Mgt_SetP2P_ConfigParams"];
*P2PInitiatorClient<<InitiatorLibNfc    [label="NFCSTATUS_PENDING"];
*P2PInitiatorClient<-InitiatorLibNfc    [label="pConfigRspCb()",URL="\ref pphLibNfc_RspCb_t"];
*P2PTargetClient=>P2PTargetLibNfc [label="phLibNfc_Mgt_ConfigureDiscovery()",URL="\ref  phLibNfc_Mgt_ConfigureDiscovery"];
*P2PTargetClient<<P2PTargetLibNfc   [label="NFCSTATUS_PENDING"]; 
*P2PTargetClient<-P2PTargetLibNfc   [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*P2PInitiatorClient=>InitiatorLibNfc    [label="phLibNfc_Mgt_ConfigureDiscovery()",URL="\ref    phLibNfc_Mgt_ConfigureDiscovery"];
*P2PInitiatorClient<<InitiatorLibNfc    [label="NFCSTATUS_PENDING"];
*P2PInitiatorClient<-InitiatorLibNfc    [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*--- [label="Place Initiator and target closely"];
*P2PInitiatorClient<-InitiatorLibNfc    [label="pNotificationHandler",URL="\ref phLibNfc_NtfRegister_RspCb_t"];
*P2PInitiatorClient=>InitiatorLibNfc    [label="phLibNfc_RemoteDev_Connect()",URL="\ref phLibNfc_RemoteDev_Connect"];
*P2PInitiatorClient<<InitiatorLibNfc    [label="NFCSTATUS_PENDING"];
*P2PInitiatorClient<-InitiatorLibNfc    [label="pNotifyConnect_RspCb",URL="\ref pphLibNfc_ConnectCallback_t"];
*P2PTargetClient<-P2PTargetLibNfc   [label="pNotificationHandler",URL="\ref phLibNfc_NtfRegister_RspCb_t"];
*--- [label="On connect target must be immediately ready to receive data from initiator"];
*P2PTargetClient=>P2PTargetLibNfc   [label="phLibNfc_RemoteDev_Receive()",URL="\ref phLibNfc_RemoteDev_Receive"];
*--- [label="Now target waits to receive data from intiator"];
*--- [label="Send data from initiator now"]; 
*P2PInitiatorClient=>InitiatorLibNfc   [label="phLibNfc_RemoteDev_Transceive()",URL="\ref  phLibNfc_RemoteDev_Transceive "];
*P2PInitiatorClient<-InitiatorLibNfc   [label="pTransceive_RspCb",URL="\ref pphLibNfc_TransceiveCallback_t"];
*--- [label="Now data arrived at target side"];
*P2PTargetClient<-P2PTargetLibNfc   [label="pReceiveRspCb",URL="\ref    pphLibNfc_Receive_RspCb_t"];
\endmsc
*\note Response callback parameters details for this interface are as listed below.
*
* \param[in] pContext   LibNfc client context   passed in the corresponding request before.
* \param[in] status     Status of the response  callback.
*
*           \param NFCSTATUS_SUCCESS             Receive operation  successful.
*           \param NFCSTATUS_SHUTDOWN       Receive operation failed because Shutdown in progress.
*           \param NFCSTATUS_ABORTED        Aborted due to initiator issued disconnect request.
*                                           or intiator removed physically from the RF field.
*                                           This status code reported,to indicate P2P session
*                                           closed and send and receive requests not allowed 
*                                           any more unless new session is started.
*           \param  NFCSTATUS_DESELECTED    Receive operation is not possible due to
*                                           initiator issued disconnect or intiator 
*                                           physically removed from the RF field.
*/
extern 
NFCSTATUS 
phLibNfc_RemoteDev_Receive( phLibNfc_Handle            hRemoteDevice,
                           pphLibNfc_Receive_RspCb_t   pReceiveRspCb,
                           void*                       pContext
                           );





/**
* \ingroup grp_lib_nfc
* \brief <b>Interface used to send data from target to initiator during P2P communication</b>.
*
*This function  Allows the NFC-IP1 target to send data to Initiator,in response to packet received 
*from initiator during P2P communication.It is must prior to send request target has received 
*data from initiator using \ref phLibNfc_RemoteDev_Receive interface.
*
*
*  \param[in]     hRemoteDevice        Peer handle obtained during device discovery process.
*
*  \param[in]     pTransferData         Data and the length of the data to be 
*                                       transferred.
*  \param[in]     pSendRspCb            Callback function called on completion 
*                                       of the NfcIP sequence or in case an 
*                                       error has occurred.
*
*  \param[in]     pContext              Upper layer context to be returned in 
*                                       the callback.
*
**  \retval NFCSTATUS_PENDING            Send operation is in progress.
*  \retval NFCSTATUS_INVALID_PARAMETER  One or more of the supplied parameters
*                                       could not be properly interpreted.
* \retval  NFCSTATUS_NOT_INITIALISED    Indicates stack is not yet initialized.
* \retval  NFCSTATUS_SHUTDOWN           Shutdown in progress.
*  \retval NFCSTATUS_INVALID_DEVICE     The device has been disconnected meanwhile.
* \retval  NFCSTATUS_BUSY               Previous request in progress can not accept new request.   
* \retval  NFCSTATUS_DESELECTED         Receive operation is not possible due to
*                                       initiator issued disconnect or intiator 
*                                       physically removed from the RF field.
*\retval   NFCSTATUS_REJECTED           Indicates invalid request.
*\retval   NFCSTATUS_FAILED             Request failed.
*
*\msc 
*P2PInitiatorClient,InitiatorLibNfc,P2PTargetLibNfc,P2PTargetClient;
*--- [label="stack is intialised and P2P notification handler registered alredy"];
*P2PTargetClient=>P2PTargetLibNfc   [label="phLibNfc_Mgt_SetP2P_ConfigParams()",URL="\ref    phLibNfc_Mgt_SetP2P_ConfigParams"];
*P2PTargetClient<<P2PTargetLibNfc   [label="NFCSTATUS_PENDING"];
*P2PTargetClient<-P2PTargetLibNfc   [label="pConfigRspCb()",URL="\ref pphLibNfc_RspCb_t"];
*P2PInitiatorClient=>InitiatorLibNfc    [label="phLibNfc_Mgt_SetP2P_ConfigParams()",URL="\ref    phLibNfc_Mgt_SetP2P_ConfigParams"];
*P2PInitiatorClient<<InitiatorLibNfc    [label="NFCSTATUS_PENDING"];
*P2PInitiatorClient<-InitiatorLibNfc    [label="pConfigRspCb()",URL="\ref pphLibNfc_RspCb_t"];
*P2PTargetClient=>P2PTargetLibNfc [label="phLibNfc_Mgt_ConfigureDiscovery()",URL="\ref  phLibNfc_Mgt_ConfigureDiscovery"];
*P2PTargetClient<<P2PTargetLibNfc   [label="NFCSTATUS_PENDING"]; 
*P2PTargetClient<-P2PTargetLibNfc   [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*P2PInitiatorClient=>InitiatorLibNfc    [label="phLibNfc_Mgt_ConfigureDiscovery()",URL="\ref    phLibNfc_Mgt_ConfigureDiscovery"];
*P2PInitiatorClient<<InitiatorLibNfc    [label="NFCSTATUS_PENDING"];
*P2PInitiatorClient<-InitiatorLibNfc    [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*--- [label="Place Initiator and target closely"];
*P2PInitiatorClient<-InitiatorLibNfc    [label="pNotificationHandler",URL="\ref phLibNfc_NtfRegister_RspCb_t"];
*P2PInitiatorClient=>InitiatorLibNfc    [label="phLibNfc_RemoteDev_Connect()",URL="\ref phLibNfc_RemoteDev_Connect"];
*P2PInitiatorClient<<InitiatorLibNfc    [label="NFCSTATUS_PENDING"];
*P2PInitiatorClient<-InitiatorLibNfc    [label="pNotifyConnect_RspCb",URL="\ref pphLibNfc_ConnectCallback_t"];
*P2PTargetClient<-P2PTargetLibNfc   [label="pNotificationHandler",URL="\ref phLibNfc_NtfRegister_RspCb_t"];
*--- [label="On connect target must be immediately ready to receive data from initiator"];
*P2PTargetClient=>P2PTargetLibNfc   [label="phLibNfc_RemoteDev_Receive()",URL="\ref phLibNfc_RemoteDev_Receive"];
*--- [label="Now target waits to receive data from intiator"];
*--- [label="Send data from initiator now"]; 
*P2PInitiatorClient=>InitiatorLibNfc   [label="phLibNfc_RemoteDev_Transceive()",URL="\ref  phLibNfc_RemoteDev_Transceive "];
*--- [label="Now data arrived at target side"];
*P2PTargetClient<-P2PTargetLibNfc   [label="pReceiveRspCb",URL="\ref    pphLibNfc_Receive_RspCb_t"];
*--- [label="Now send data from target"];
*P2PTargetClient=>P2PTargetLibNfc   [label="phLibNfc_RemoteDev_Send()",URL="\ref    phLibNfc_RemoteDev_Send"];
*P2PInitiatorClient<-InitiatorLibNfc   [label="pTransceive_RspCb",URL="\ref pphLibNfc_TransceiveCallback_t"];
*P2PTargetClient<-P2PTargetLibNfc   [label="pSendRspCb",URL="\ref   pphLibNfc_RspCb_t"];
*\endmsc
*
*\note Response callback parameters details for this interface are as listed below.
*
* \param[in] pContext   LibNfc client context   passed in the corresponding request before.
* \param[in] status     Status of the response  callback.
*
*           \param NFCSTATUS_SUCCESS             Send operation  successful.
*           \param NFCSTATUS_SHUTDOWN       Send operation failed because Shutdown in progress.
*           \param NFCSTATUS_ABORTED        Aborted due to initiator issued disconnect request.
*                                           or intiator removed physically from the RF field.
*                                           This status code reported,to indicate P2P session
*                                           closed and send and receive requests not allowed 
*                                           any more unless new session is started.
*           \param  NFCSTATUS_DESELECTED    Receive operation is not possible due to
*                                           initiator issued disconnect or intiator 
*                                           physically removed from the RF field.
*
*
*/
extern 
NFCSTATUS 
phLibNfc_RemoteDev_Send(phLibNfc_Handle             hRemoteDevice,
                        phNfc_sData_t*              pTransferData,
                        pphLibNfc_RspCb_t           pSendRspCb,
                        void*                       pContext
                        );

/**
* \ingroup grp_lib_nfc
* \brief <b>Interface to configure P2P and intiator mode configurations</b>.
*  The  setting will be typically take effect for the next cycle of the relevant 
*  phase of discovery. For optional configuration internal defaults will be
*  used in case the configuration is not set.
*
*\note Currently general bytes configuration supported.
*
*  \param[in] pConfigInfo           Union containing P2P configuration details as
*                                   in \ref phLibNfc_sNfcIPCfg_t.
*
*  \param[in] pConfigRspCb          This callback has to be called once LibNfc 
*                                   completes the Configuration.
*
*  \param[in] pContext              Upper layer context to be returned in 
*                                   the callback.
*
*
* \retval NFCSTATUS_PENDING             Config operation is in progress.
* \retval NFCSTATUS_INVALID_PARAMETER   One or more of the supplied parameters
*                                       could not be properly interpreted.
* \retval  NFCSTATUS_NOT_INITIALISED    Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN            Shutdown in progress.
* \retval NFCSTATUS_BUSY                Previous request in progress can not accept new request.   
*
*\msc 
*P2PInitiatorClient,InitiatorLibNfc,P2PTargetLibNfc,P2PTargetClient;
*--- [label="stack is intialised and P2P notification handler registered alredy"];
*P2PTargetClient=>P2PTargetLibNfc   [label="phLibNfc_Mgt_SetP2P_ConfigParams()",URL="\ref    phLibNfc_Mgt_SetP2P_ConfigParams"];
*P2PTargetClient<<P2PTargetLibNfc   [label="NFCSTATUS_PENDING"];
*P2PTargetClient<-P2PTargetLibNfc   [label="pConfigRspCb()",URL="\ref pphLibNfc_RspCb_t"];
*P2PInitiatorClient=>InitiatorLibNfc    [label="phLibNfc_Mgt_SetP2P_ConfigParams()",URL="\ref    phLibNfc_Mgt_SetP2P_ConfigParams"];
*P2PInitiatorClient<<InitiatorLibNfc    [label="NFCSTATUS_PENDING"];
*P2PTargetClient=>P2PTargetLibNfc [label="phLibNfc_Mgt_ConfigureDiscovery()",URL="\ref  phLibNfc_Mgt_ConfigureDiscovery"];
*P2PTargetClient<<P2PTargetLibNfc   [label="NFCSTATUS_PENDING"]; 
*P2PTargetClient<-P2PTargetLibNfc   [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*P2PInitiatorClient=>InitiatorLibNfc    [label="phLibNfc_Mgt_ConfigureDiscovery()",URL="\ref    phLibNfc_Mgt_ConfigureDiscovery"];
*P2PInitiatorClient<<InitiatorLibNfc    [label="NFCSTATUS_PENDING"];
*P2PInitiatorClient<-InitiatorLibNfc    [label="pConfigDiscovery_RspCb",URL="\ref pphLibNfc_RspCb_t"];
*--- [label="Place Initiator and target closely"];
*P2PInitiatorClient<-InitiatorLibNfc    [label="pNotificationHandler",URL="\ref phLibNfc_NtfRegister_RspCb_t"];
*P2PInitiatorClient=>InitiatorLibNfc    [label="phLibNfc_RemoteDev_Connect()",URL="\ref phLibNfc_RemoteDev_Connect"];
*P2PInitiatorClient<<InitiatorLibNfc    [label="NFCSTATUS_PENDING"];
*P2PInitiatorClient<-InitiatorLibNfc    [label="pNotifyConnect_RspCb",URL="\ref pphLibNfc_ConnectCallback_t"];
*P2PTargetClient<-P2PTargetLibNfc   [label="pNotificationHandler",URL="\ref phLibNfc_NtfRegister_RspCb_t"];
*--- [label="Now configured params ( Ex : general bytes can been seen in remote device info"];
*\endmsc
*
*\note Response callback parameters details for this interface are as listed below.
*
* \param[in] pContext   LibNfc client context   passed in the corresponding request before.
* \param[in] status     Status of the response  callback.
*
*                  \param NFCSTATUS_SUCCESS                  configuration operation is successful.
*                  \param NFCSTATUS_SHUTDOWN            Shutdown in progress.
*                  \param NFCSTATUS_FAILED              Request failed.
*
*/
extern NFCSTATUS phLibNfc_Mgt_SetP2P_ConfigParams(   phLibNfc_sNfcIPCfg_t*   pConfigInfo,
                                                    pphLibNfc_RspCb_t       pConfigRspCb,
                                                    void*                   pContext
                                                );

/**
* \ingroup grp_lib_nfc
* \brief <b>Interface to stack capabilities</b>.
*
*  LibNfc client can query to retrieve stack capabilities.Stack capabilities contains
*  <br><br>a).Device capabilities which contains details like protocols supported, 
*  Hardware,Firmware  and model-id version details .For details refer to \ref phNfc_sDeviceCapabilities_t.
* <br><br>b).NDEF mapping related info. This info helps in identifying supported tags for NDEF mapping feature.
* <br><br>c).NDEF formatting related info. This info helps in identifying supported tags for NDEF formatting feature.
*
*  \param[in] phLibNfc_StackCapabilities   Contains device capabilities and NDEF mapping and formatting feature
                                           support for different tag types.
*
*  \param[in] pContext                     Upper layer context to be returned in 
*                                          the callback.
*
*
* \retval NFCSTATUS_SUCCESS               Indicates Get stack Capabilities operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER     One or more of the supplied parameters
*                                         could not be properly interpreted.
* \retval NFCSTATUS_NOT_INITIALISED       Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN              Shutdown in progress.
* \retval NFCSTATUS_FAILED                operation failed.
* \retval NFCSTATUS_BUSY                  Previous request in progress can not accept new request.
*
*
*\msc
*LibNfcClient,LibNfc;
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_Initialize()",URL="\ref phLibNfc_Mgt_Initialize"];
*LibNfcClient<-LibNfc   [label="pInitCb()",URL="\ref pphLibNfc_RspCb_t()"];
*LibNfcClient=>LibNfc   [label="phLibNfc_RemoteDev_NtfRegister()",URL="\ref phLibNfc_RemoteDev_NtfRegister"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_GetstackCapabilities()",URL="\ref phLibNfc_Mgt_GetstackCapabilities"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*--- [label="Now stack capabilities available "];
*
*\endmsc*/


extern NFCSTATUS phLibNfc_Mgt_GetstackCapabilities(phLibNfc_StackCapabilities_t* phLibNfc_StackCapabilities,
                                                   void*                         pContext
                                                  );


/**
* \ingroup grp_lib_nfcHW_
* \brief <b>Interface to configure local LLCP peer</b>.
*
* This function configures the parameters of the local LLCP peer. This function must be called
* before any other LLCP-related function from this API.
*
* \param[in] pConfigInfo   Contains local LLCP link parameters to be applied
* \param[in] pConfigRspCb  This callback has to be called once LibNfc
*                          completes the Configuration.
* \param[in] pContext      Upper layer context to be returned in
*                          the callback.
*
*
* \retval NFCSTATUS_SUCCESS               Operation successful.
* \retval NFCSTATUS_PENDING               Configuration operation is in progress,
                                          pConfigRspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_PARAMETER     One or more of the supplied parameters
*                                         could not be properly interpreted.
* \retval NFCSTATUS_NOT_INITIALISED       Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN              Shutdown in progress.
* \retval NFCSTATUS_FAILED                Operation failed.
* \retval NFCSTATUS_BUSY                  Previous request in progress can not accept new request.
*/
extern NFCSTATUS phLibNfc_Mgt_SetLlcp_ConfigParams( phLibNfc_Llcp_sLinkParameters_t* pConfigInfo,
                                                   pphLibNfc_RspCb_t                pConfigRspCb,
                                                   void*                            pContext
                                                   );


/**
* \ingroup grp_lib_nfc
* \brief <b>Checks if a remote peer is LLCP compliant</b>.
*
* This functions allows to check if a previously detected tag is compliant with the
* LLCP protocol. This step is needed before calling any other LLCP-related function on
* this remote peer, except local LLCP peer configurationn, which is more general. Once
* this checking is done, the caller will be able to receive link status notifications
* until the peer is disconnected.
*
* \param[in] hRemoteDevice       Peer handle obtained during device discovery process.
* \param[in] pCheckLlcp_RspCb    The callback to be called once LibNfc
*                                completes the LLCP compliancy check.
* \param[in] pLink_Cb            The callback to be called each time the
*                                LLCP link status changes.
* \param[in] pContext            Upper layer context to be returned in
*                                the callbacks.
*
*
* \retval NFCSTATUS_SUCCESS               Operation successful.
* \retval NFCSTATUS_PENDING               Check operation is in progress, pCheckLlcp_RspCb will
*                                         be called upon completion.
* \retval NFCSTATUS_INVALID_PARAMETER     One or more of the supplied parameters
*                                         could not be properly interpreted.
* \retval NFCSTATUS_NOT_INITIALISED       Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN              Shutdown in progress.
* \retval NFCSTATUS_FAILED                Operation failed.
* \retval NFCSTATUS_BUSY                  Previous request in progress can not accept new request.
*/
extern NFCSTATUS phLibNfc_Llcp_CheckLlcp( phLibNfc_Handle              hRemoteDevice,
                                          pphLibNfc_ChkLlcpRspCb_t     pCheckLlcp_RspCb,
                                          pphLibNfc_LlcpLinkStatusCb_t pLink_Cb,
                                          void*                        pContext
                                          );


/**
* \ingroup grp_lib_nfc
* \brief <b>Activates a LLCP link with a remote device </b>.
*
* This function launches the link activation process on a remote LLCP-compliant peer. The link status
* notification will be sent by the corresponding callback given in the phLibNfc_Llcp_CheckLlcp function.
* If the activation fails, the deactivated status will be notified, even if the link is already in a
* deactivated state. 
*
* \param[in] hRemoteDevice       Peer handle obtained during device discovery process.
*
* \retval NFCSTATUS_SUCCESS               Operation successful.
* \retval NFCSTATUS_PENDING               Activation operation is in progress,
                                          pLink_Cb will be called upon completion.
* \retval NFCSTATUS_INVALID_PARAMETER     One or more of the supplied parameters
*                                         could not be properly interpreted.
* \retval NFCSTATUS_NOT_INITIALISED       Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN              Shutdown in progress.
* \retval NFCSTATUS_FAILED                Operation failed.
* \retval NFCSTATUS_BUSY                  Previous request in progress can not accept new request.
*/
extern NFCSTATUS phLibNfc_Llcp_Activate( phLibNfc_Handle hRemoteDevice );


/**
* \ingroup grp_lib_nfc
* \brief <b>Deactivate a previously activated LLCP link with a remote device</b>.
*
* This function launches the link deactivation process on a remote LLCP-compliant peer. The link status
* notification will be sent by the corresponding callback given in the phLibNfc_Llcp_CheckLlcp function.
*
* \param[in] hRemoteDevice       Peer handle obtained during device discovery process.
*
* \retval NFCSTATUS_SUCCESS               Operation successful.
* \retval NFCSTATUS_PENDING               Deactivation operation is in progress,
                                          pLink_Cb will be called upon completion.
* \retval NFCSTATUS_INVALID_PARAMETER     One or more of the supplied parameters
*                                         could not be properly interpreted.
* \retval NFCSTATUS_NOT_INITIALISED       Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN              Shutdown in progress.
* \retval NFCSTATUS_FAILED                Operation failed.
* \retval NFCSTATUS_BUSY                  Previous request in progress can not accept new request.
*/
extern NFCSTATUS phLibNfc_Llcp_Deactivate( phLibNfc_Handle  hRemoteDevice );


/**
* \ingroup grp_lib_nfc
* \brief <b>Get information on the local LLCP peer</b>.
*
* This function returns the LLCP link parameters of the local peer that were used
* during the link activation.
*
* \param[in]  hRemoteDevice         Peer handle obtained during device discovery process.
* \param[out] pConfigInfo           Pointer on the variable to be filled with the configuration
                                    parameters used during activation.
*
* \retval NFCSTATUS_SUCCESS               Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER     One or more of the supplied parameters
*                                         could not be properly interpreted.
* \retval NFCSTATUS_NOT_INITIALISED       Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN              Shutdown in progress.
* \retval NFCSTATUS_FAILED                Operation failed.
* \retval NFCSTATUS_BUSY                  Previous request in progress can not accept new request.
*/
extern NFCSTATUS phLibNfc_Llcp_GetLocalInfo( phLibNfc_Handle                  hRemoteDevice,
                                             phLibNfc_Llcp_sLinkParameters_t* pConfigInfo
                                             );


/**
* \ingroup grp_lib_nfc
* \brief <b>Get information on the remote LLCP peer</b>.
*
* This function returns the LLCP link parameters of the remote peer that were received
* during the link activation.
*
* \param[in]  hRemoteDevice         Peer handle obtained during device discovery process.
* \param[out] pConfigInfo           Pointer on the variable to be filled with the configuration
                                    parameters used during activation.
*
* \retval NFCSTATUS_SUCCESS               Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER     One or more of the supplied parameters
*                                         could not be properly interpreted.
* \retval NFCSTATUS_NOT_INITIALISED       Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN              Shutdown in progress.
* \retval NFCSTATUS_FAILED                Operation failed.
* \retval NFCSTATUS_BUSY                  Previous request in progress can not accept new request.
*/
extern NFCSTATUS phLibNfc_Llcp_GetRemoteInfo( phLibNfc_Handle                    hRemoteDevice,
                                              phLibNfc_Llcp_sLinkParameters_t*   pConfigInfo
                                              );


/**
* \ingroup grp_lib_nfc
* \brief <b>Create a socket on a LLCP-connected device</b>.
*
* This function creates a socket for a given LLCP link. Sockets can be of two types : 
* connection-oriented and connectionless. If the socket is connection-oriented, the caller
* must provide a working buffer to the socket in order to handle incoming data. This buffer
* must be large enough to fit the receive window (RW * MIU), the remaining space being
* used as a linear buffer to store incoming data as a stream. Data will be readable later
* using the phLibNfc_Llcp_Recv function. If the socket is connectionless, the caller may
* provide a working buffer to the socket in order to bufferize as many packets as the buffer
* can contain (each packet needs MIU + 1 bytes).
* The options and working buffer are not required if the socket is used as a listening socket,
* since it cannot be directly used for communication.
*
* \param[in]  eType                 The socket type.
* \param[in]  psOptions             The options to be used with the socket.
* \param[in]  psWorkingBuffer       A working buffer to be used by the library.
* \param[out] phSocket              A pointer on the variable to be filled with the handle
*                                   on the created socket.
* \param[in]  pErr_Cb               The callback to be called each time the socket
*                                   is in error.
* \param[in]  pContext              Upper layer context to be returned in the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_BUFFER_TOO_SMALL         The working buffer is too small for the MIU and RW
*                                            declared in the options.
* \retval NFCSTATUS_INSUFFICIENT_RESOURCES   No more socket handle available.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
extern NFCSTATUS phLibNfc_Llcp_Socket( phLibNfc_Llcp_eSocketType_t      eType,
                                       phLibNfc_Llcp_sSocketOptions_t*  psOptions,
                                       phNfc_sData_t*                   psWorkingBuffer,
                                       phLibNfc_Handle*                 phSocket,
                                       pphLibNfc_LlcpSocketErrCb_t      pErr_Cb,
                                       void*                            pContext
                                       );


/**
* \ingroup grp_lib_nfc
* \brief <b>Get SAP of remote services using their names</b>.
*
* This function sends SDP queries to the remote peer to get the SAP to address for a given
* service name. The queries are aggregated as much as possible for efficiency, but if all
* the queries cannot fit in a single packet, they will be splitted in multiple packets.
* The callback will be called only when all of the requested services names SAP will be
* gathered. As mentionned in LLCP specification, a SAP of 0 means that the service name
* as not been found.
*
* This feature is available only since LLCP v1.1, both devices must be at least v1.1 in
* order to be able to use this function.
*
* \param[in]  hRemoteDevice      Peer handle obtained during device discovery process.
* \param[in]  psServiceNameList  The list of the service names to discover.
* \param[out] pnSapList          The list of the corresponding SAP numbers, in the same
*                                order than the service names list.
* \param[in]  nListSize          The size of both service names and SAP list.
* \param[in]  pDiscover_Cb       The callback to be called once LibNfc matched SAP for
*                                all of the provided service names.
* \param[in]  pContext           Upper layer context to be returned in the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
* \retval NFCSTATUS_FEATURE_NOT_SUPPORTED    Remote peer does not support this feature (e.g.: is v1.0).
* \retval NFCSTATUS_BUSY                     Previous request in progress can not accept new request.
*/
extern NFCSTATUS phLibNfc_Llcp_DiscoverServices( phLibNfc_Handle     hRemoteDevice,
                                                 phNfc_sData_t       *psServiceNameList,
                                                 uint8_t             *pnSapList,
                                                 uint8_t             nListSize,
                                                 pphLibNfc_RspCb_t   pDiscover_Cb,
                                                 void                *pContext
                                               );


/**
* \ingroup grp_lib_nfc
* \brief <b>Close a socket on a LLCP-connected device</b>.
*
* This function closes a LLCP socket previously created using phLibNfc_Llcp_Socket.
* If the socket was connected, it is first disconnected, and then closed.
*
* \param[in]  hSocket               Socket handle obtained during socket creation.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
extern NFCSTATUS phLibNfc_Llcp_Close( phLibNfc_Handle hSocket );


/**
* \ingroup grp_lib_nfc
* \brief <b>Get the local options of a socket</b>.
*
* This function returns the local options (maximum packet size and receive window size) used
* for a given connection-oriented socket. This function shall not be used with connectionless
* sockets.
*
* \param[in]  hSocket               Socket handle obtained during socket creation.
* \param[in]  psLocalOptions        A pointer to be filled with the local options of the socket.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
extern NFCSTATUS phLibNfc_Llcp_SocketGetLocalOptions( phLibNfc_Handle                  hSocket,
                                                      phLibNfc_Llcp_sSocketOptions_t*  psLocalOptions
                                                      );


/**
* \ingroup grp_lib_nfc
* \brief <b>Get the local options of a socket</b>.
*
* This function returns the remote options (maximum packet size and receive window size) used
* for a given connection-oriented socket. This function shall not be used with connectionless
* sockets.
*
* \param[in]  hSocket               Socket handle obtained during socket creation.
* \param[in]  psRemoteOptions       A pointer to be filled with the remote options of the socket.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
extern NFCSTATUS phLibNfc_Llcp_SocketGetRemoteOptions( phLibNfc_Handle                  hRemoteDevice,
                                                       phLibNfc_Handle                  hSocket,
                                                       phLibNfc_Llcp_sSocketOptions_t*  psRemoteOptions
                                                       );


/**
* \ingroup grp_lib_nfc
* \brief <b>Bind a socket to a local SAP</b>.
*
* This function binds the socket to a local Service Access Point.
*
* \param[in]  hSocket               Peer handle obtained during device discovery process.
* \param TODO (nSap + sn)

* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_ALREADY_REGISTERED       The selected SAP is already bound to another
                                             socket.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
extern NFCSTATUS phLibNfc_Llcp_Bind( phLibNfc_Handle hSocket,
                                     uint8_t         nSap,
                                     phNfc_sData_t * psServiceName
                                     );


/**
* \ingroup grp_lib_nfc
* \brief <b>Listen for incoming connection requests on a socket</b>.
*
* This function switches a socket into a listening state and registers a callback on
* incoming connection requests. In this state, the socket is not able to communicate
* directly. The listening state is only available for connection-oriented sockets
* which are still not connected. The socket keeps listening until it is closed, and
* thus can trigger several times the pListen_Cb callback. The caller can adverise the
* service through SDP by providing a service name.
*
*
* \param[in]  hSocket            Socket handle obtained during socket creation.
* \param[in]  pListen_Cb         The callback to be called each time the
*                                socket receive a connection request.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state to switch
*                                            to listening state.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
extern NFCSTATUS phLibNfc_Llcp_Listen( phLibNfc_Handle                  hSocket,
                                       pphLibNfc_LlcpSocketListenCb_t   pListen_Cb,
                                       void*                            pContext
                                       );


/**
* \ingroup grp_lib_nfc
* \brief <b>Accept an incoming connection request for a socket</b>.
*
* This functions allows the client to accept an incoming connection request.
* It must be used with the socket provided within the listen callback. The socket
* is implicitly switched to the connected state when the function is called.
*
* \param[in]  hSocket               Socket handle obtained in the listening callback.
* \param[in]  psOptions             The options to be used with the socket.
* \param[in]  psWorkingBuffer       A working buffer to be used by the library.
* \param[in]  pErr_Cb               The callback to be called each time the accepted socket
*                                   is in error.
* \param[in]  pAccept_RspCb         The callback to be called when the Accept operation 
*                                   is completed.
* \param[in]  pContext              Upper layer context to be returned in the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_BUFFER_TOO_SMALL         The working buffer is too small for the MIU and RW
*                                            declared in the options.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
extern NFCSTATUS phLibNfc_Llcp_Accept( phLibNfc_Handle                  hSocket,
                                       phLibNfc_Llcp_sSocketOptions_t*  psOptions,
                                       phNfc_sData_t*                   psWorkingBuffer,
                                       pphLibNfc_LlcpSocketErrCb_t      pErr_Cb,
                                       pphLibNfc_LlcpSocketAcceptCb_t   pAccept_RspCb,
                                       void*                            pContext
                                       );


/**
* \ingroup grp_lib_nfc
* \brief <b>Reject an incoming connection request for a socket</b>.
*
* This functions allows the client to reject an incoming connection request.
* It must be used with the socket provided within the listen callback. The socket
* is implicitly closed when the function is called.
*
* \param[in]  hSocket               Socket handle obtained in the listening callback.
* \param[in]  pReject_RspCb         The callback to be called when the Reject operation 
*                                   is completed.
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
extern NFCSTATUS phLibNfc_Llcp_Reject( phLibNfc_Handle                  hRemoteDevice,
                                       phLibNfc_Handle                  hSocket,
                                       pphLibNfc_LlcpSocketAcceptCb_t   pReject_RspCb,
                                       void*                            pContext);


/**
* \ingroup grp_lib_nfc
* \brief <b>Try to establish connection with a socket on a remote SAP</b>.
*
* This function tries to connect to a given SAP on the remote peer. If the
* socket is not bound to a local SAP, it is implicitly bound to a free SAP.
*
* \param[in]  hSocket            Socket handle obtained during socket creation.
* \param[in]  nSap               The destination SAP to connect to.
* \param[in]  pConnect_RspCb     The callback to be called when the connection
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Connection operation is in progress,
*                                            pConnect_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
extern NFCSTATUS phLibNfc_Llcp_Connect( phLibNfc_Handle                 hRemoteDevice,
                                        phLibNfc_Handle                 hSocket,
                                        uint8_t                         nSap,
                                        pphLibNfc_LlcpSocketConnectCb_t pConnect_RspCb,
                                        void*                           pContext
                                        );


/**
* \ingroup grp_lib_nfc
* \brief <b>Try to establish connection with a socket on a remote service, given its URI</b>.
*
* This function tries to connect to a SAP designated by an URI. If the
* socket is not bound to a local SAP, it is implicitly bound to a free SAP.
*
* \param[in]  hSocket            Socket handle obtained during socket creation.
* \param[in]  psUri              The URI corresponding to the destination SAP to connect to.
* \param[in]  pConnect_RspCb     The callback to be called when the connection
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Connection operation is in progress,
*                                            pConnect_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
extern NFCSTATUS phLibNfc_Llcp_ConnectByUri( phLibNfc_Handle                 hRemoteDevice,
                                             phLibNfc_Handle                 hSocket,
                                             phNfc_sData_t*                  psUri,
                                             pphLibNfc_LlcpSocketConnectCb_t pConnect_RspCb,
                                             void*                           pContext
                                             );


/**
* \ingroup grp_lib_nfc
* \brief <b>Disconnect a currently connected socket</b>.
*
* This function initiates the disconnection of a previously connected socket.
*
* \param[in]  hSocket            Socket handle obtained during socket creation.
* \param[in]  pDisconnect_RspCb  The callback to be called when the 
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Disconnection operation is in progress,
*                                            pDisconnect_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
extern NFCSTATUS phLibNfc_Llcp_Disconnect( phLibNfc_Handle                    hRemoteDevice,
                                           phLibNfc_Handle                    hSocket,
                                           pphLibNfc_LlcpSocketDisconnectCb_t pDisconnect_RspCb,
                                           void*                              pContext
                                           );


/**
* \ingroup grp_lib_nfc
* \brief <b>Read data on a socket</b>.
*
* This function is used to read data from a socket. It reads at most the
* size of the reception buffer, but can also return less bytes if less bytes
* are available. If no data is available, the function will be pending until
* more data comes, and the response will be sent by the callback. This function
* can only be called on a connection-oriented socket.
* 
*
* \param[in]  hSocket            Socket handle obtained during socket creation.
* \param[in]  psBuffer           The buffer receiving the data.
* \param[in]  pRecv_RspCb        The callback to be called when the 
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Reception operation is in progress,
*                                            pRecv_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
extern NFCSTATUS phLibNfc_Llcp_Recv( phLibNfc_Handle              hRemoteDevice,
                                     phLibNfc_Handle              hSocket,
                                     phNfc_sData_t*               psBuffer,
                                     pphLibNfc_LlcpSocketRecvCb_t pRecv_RspCb,
                                     void*                        pContext
                                     );


/**
* \ingroup grp_lib_nfc
* \brief <b>Read data on a socket and get the source SAP</b>.
*
* This function is the same as phLibNfc_Llcp_Recv, except that the callback includes
* the source SAP. This functions can only be called on a connectionless socket.
* 
*
* \param[in]  hSocket            Socket handle obtained during socket creation.
* \param[in]  psBuffer           The buffer receiving the data.
* \param[in]  pRecv_RspCb        The callback to be called when the 
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Reception operation is in progress,
*                                            pRecv_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
extern NFCSTATUS phLibNfc_Llcp_RecvFrom( phLibNfc_Handle                   hRemoteDevice,
                                         phLibNfc_Handle                   hSocket,
                                         phNfc_sData_t*                    psBuffer,
                                         pphLibNfc_LlcpSocketRecvFromCb_t  pRecv_Cb,
                                         void*                             pContext
                                         );


/**
* \ingroup grp_lib_nfc
* \brief <b>Send data on a socket</b>.
*
* This function is used to write data on a socket. This function
* can only be called on a connection-oriented socket which is already
* in a connected state.
* 
*
* \param[in]  hSocket            Socket handle obtained during socket creation.
* \param[in]  psBuffer           The buffer containing the data to send.
* \param[in]  pSend_RspCb        The callback to be called when the 
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Reception operation is in progress,
*                                            pSend_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
extern NFCSTATUS phLibNfc_Llcp_Send( phLibNfc_Handle              hRemoteDevice,
                                     phLibNfc_Handle              hSocket,
                                     phNfc_sData_t*               psBuffer,
                                     pphLibNfc_LlcpSocketSendCb_t pSend_RspCb,
                                     void*                        pContext
                                     );


/**
* \ingroup grp_lib_nfc
* \brief <b>Send data on a socket to a given destination SAP</b>.
*
* This function is used to write data on a socket to a given destination SAP.
* This function can only be called on a connectionless socket.
* 
*
* \param[in]  hSocket            Socket handle obtained during socket creation.
* \param[in]  nSap               The destination SAP.
* \param[in]  psBuffer           The buffer containing the data to send.
* \param[in]  pSend_RspCb        The callback to be called when the 
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Reception operation is in progress,
*                                            pSend_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
extern NFCSTATUS phLibNfc_Llcp_SendTo( phLibNfc_Handle               hRemoteDevice,
                                       phLibNfc_Handle               hSocket,
                                       uint8_t                       nSap,
                                       phNfc_sData_t*                psBuffer,
                                       pphLibNfc_LlcpSocketSendCb_t  pSend_RspCb,
                                       void*                         pContext
                                       );


/**
* \ingroup grp_lib_nfc
*
* \brief Initializes \ DeInitialize the NFC library for testmode.
*
*
*\brief This function initializes / DeInitialize NFC library and its underlying layers 
* in test mode. As part of this interface underlying layers gets configured.
* Once phLibNfc_TstMode_On is successful ,NFC library ready in testmode using IOCTL.
* After using test IOCTLs ,Test mode should be DeInit using phLibNfc_TstMode_Off.
*\note This API should be used only for test IOCTL codes. 
*
* \param[in] pDriverHandle         Driver Handle currently application is using.
* \param[in] pTestModeCb        The init callback is called by the LibNfc when 
*                               Configure test mode completed or there is an error
*                               in initialization.
*
* \param[in] pContext           Client context which will   be included in
*                               callback when the request is completed.
*
* \retval NFCSTATUS_ALREADY_INITIALISED     Stack is already initialized.
* \retval NFCSTATUS_PENDING                 Init sequence   has been successfully
*                                           started and result will be  conveyed via 
*                                           callback notification.
* \retval NFCSTATUS_INVALID_PARAMETER       The parameter could not be  properly
*                                           interpreted.
*\retval NFCSTATUS_INSUFFICIENT_RESOURCES   Insufficient resource.(Ex: insufficient memory)
*
*\msc
*LibNfcClient,LibNfc;
*--- [label="Before initializing Nfc LIB,Setup Driver layer"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_ConfigureDriver()",URL="\ref phLibNfc_Mgt_ConfigureDriver"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_SUCCESS"];
*LibNfcClient=>LibNfc   [label="phLibNfc_Mgt_ConfigureTestMode()",URL="\ref phLibNfc_Mgt_ConfigureTestMode"];
*LibNfcClient<<LibNfc   [label="NFCSTATUS_PENDING"];
*LibNfcClient<-LibNfc   [label="pTestModeCb"];
*\endmsc
*/
NFCSTATUS phLibNfc_Mgt_ConfigureTestMode(void              *pDriverHandle,
                                 pphLibNfc_RspCb_t         pTestModeCb,
                                 phLibNfc_Cfg_Testmode_t   eTstmode,
                                 void                      *pContext
                                 );

/**
* \ingroup grp_lib_nfc
* \brief <b>Interface to LibNfc Reset</b>.
*
*  LibNfc client can reset the stack.
*
*  \param[in] pContext                     Upper layer context to be returned in 
*                                          the callback.
*
*
* \retval NFCSTATUS_SUCCESS               Indicates Get stack Capabilities operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER     One or more of the supplied parameters
*                                         could not be properly interpreted.
* \retval NFCSTATUS_NOT_INITIALISED       Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN              Shutdown in progress.
*/

NFCSTATUS phLibNfc_Mgt_Reset(void    *pContext);

#endif  /*  PHLIBNFC_H    */

