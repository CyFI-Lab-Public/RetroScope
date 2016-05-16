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
 * \file phHal4Nfc.h
 * \brief HAL Function Prototypes
 *  The HAL4.0 API provides the user to have a interface for PN544(PN54x)/PN65N
 *  NFC device.The API is a non-blocking API, asynchronous API. This means that
 *  when ever an API function call results in waiting for a response from the
 *  NFC device, the API function will return immediately with status 'PENDING'
 *  and the actual result will be returned through specific callback functions
 *  on receiving the response from the NFC device
 *
 * \note This is the representative header file of the HAL 4.0. The release
 *       TAG or label is representing the release TAG (alias) of the entire
 *       library.A mechanism (see documentation \ref hal_release_label near
 *       the include guards of this file) is used to propagate the alias to
 *       the main documentation page.
 *
 * Project: NFC-FRI-1.1 / HAL4.0
 *
 * $Date: Mon Jun 14 11:36:12 2010 $
 * $Author: ing07385 $
 * $Revision: 1.171 $
 * $Aliases: NFC_FRI1.1_WK1023_R35_2,NFC_FRI1.1_WK1023_R35_1 $
 *
 */

/** page hal_release_label HAL 4.0 Release Label
 *  SDK_HAL_4.0 v 0.1 Draft
 *  \note This is the TAG (label, alias) of the HAL. If the string is empty,the
 *        current documentation has not been generated from an official release.
 */
/*@{*/
#ifndef PHHAL4NFC_H
#define PHHAL4NFC_H
/*@}*/


/**
 *  \name HAL4
 *
 * File: \ref phHal4Nfc.h
 *\def  hal
 */

/*@{*/
#define PH_HAL4NFC_FILEREVISION "$Revision: 1.171 $" /**< \ingroup grp_file_attributes */
#define PH_HAL4NFC_FILEALIASES  "$Aliases: NFC_FRI1.1_WK1023_R35_2,NFC_FRI1.1_WK1023_R35_1 $"     /**< \ingroup grp_file_attributes */
/*@}*/

/* -----------------Include files ---------------------------------------*/
#include <phNfcStatus.h>
#include <phNfcCompId.h>
#include <phNfcHalTypes.h>
#include <phNfcInterface.h>
#include <phNfcIoctlCode.h>
#include <phNfcConfig.h>
#include <phDbgTrace.h>
#ifdef ANDROID
#include <string.h>
#endif

/*************************** Includes *******************************/
/** \defgroup grp_mw_external_hal_funcs NFC HAL4.0
*
*
*
*/
/* ---------------- Macros ----------------------------------------------*/

/** HAL Implementation Version Macros : Updated for every feature release of
    HAL functionality */
#define PH_HAL4NFC_VERSION                              8
#define PH_HAL4NFC_REVISION                            21
#define PH_HAL4NFC_PATCH                                1 
#define PH_HAL4NFC_BUILD                                0

/** HAL Interface Version Macros : Updated for every external release of
    HAL Interface */
#define PH_HAL4NFC_INTERFACE_VERSION                    0
#define PH_HAL4NFC_INTERFACE_REVISION                   6
#define PH_HAL4NFC_INTERFACE_PATCH                      0
#define PH_HAL4NFC_INTERAFECE_BUILD                     0

/**Maximum length of receive buffer maintained by HAL*/
#define PH_HAL4NFC_MAX_RECEIVE_BUFFER                4096U

/**Send length used for Transceive*/
#define PH_HAL4NFC_MAX_SEND_LEN                      PHHAL_MAX_DATASIZE

/* -----------------Structures and Enumerations -------------------------*/

/**
 * \ingroup grp_mw_external_hal_funcs
 *
 * Structure containing information about discovered remote device, like
 * the number of remote devices found, device specific information
 * like type of device (eg: ISO14443-4A/4B, NFCIP1 target etc) and
 * the type sepcific information (eg: UID, SAK etc). This structure is
 * returned as part of the disocvery notification. For more info refer
 * \ref phHal4Nfc_ConfigureDiscovery,
 * \ref phHal4Nfc_RegisterNotification,
 * \ref pphHal4Nfc_Notification_t,
 * phHal4Nfc_NotificationInfo_t
 *
 *
 */
typedef struct phHal4Nfc_DiscoveryInfo
{
    uint32_t NumberOfDevices;/**< Number of devices found */
    phHal_sRemoteDevInformation_t **ppRemoteDevInfo;/**< Pointer to Remote
                                                         device info list*/
}phHal4Nfc_DiscoveryInfo_t;

/**
 * \ingroup grp_mw_external_hal_funcs
 *
 *  This is a union returned as part of the \ref pphHal4Nfc_Notification_t
 *  callback. It contains either discovery information or other event
 *  information for which the client has registered using the
 *  \ref phHal4Nfc_RegisterNotification.
 */
typedef union
{
    phHal4Nfc_DiscoveryInfo_t *psDiscoveryInfo;
    phHal_sEventInfo_t        *psEventInfo;
}phHal4Nfc_NotificationInfo_t;



/**
* \ingroup grp_mw_external_hal_funcs
*
* Prototype for Generic callback type provided by upper layer. This is used
* to return the success or failure status an asynchronous API function which
* does not have any other additional information to be returned. Refer
* specific function for applicable status codes.
*/
typedef void (*pphHal4Nfc_GenCallback_t)(
                                        void  *context,
                                        NFCSTATUS status
                                        );

/**
* \ingroup grp_mw_external_hal_funcs
*
* Disconnect callback type provided by upper layer to called on completion
* of disconnect call \ref phHal4Nfc_Disconnect.
*
*/
typedef void (*pphHal4Nfc_DiscntCallback_t)(
                        void  *context,
                        phHal_sRemoteDevInformation_t *psDisconnectDevInfo,
                        NFCSTATUS status
                        );

/**
* \ingroup grp_mw_external_hal_funcs
*
* Notification callback type used by HAL to provide a Discovery or
* Event notification to the upper layer.
*
*/
typedef void (*pphHal4Nfc_Notification_t) (
                                        void                         *context,
                                        phHal_eNotificationType_t     type,
                                        phHal4Nfc_NotificationInfo_t  info,
                                        NFCSTATUS                    status
                                        );


/**
* \ingroup grp_mw_external_hal_funcs
*
* Callback type used to provide a Connect Success or Failure indication to
* the upper layer as a result of \ref phHal4Nfc_Connect call used to connect
* to discovered remote device.
*
*/
typedef void (*pphHal4Nfc_ConnectCallback_t)(
                        void  *context,
                        phHal_sRemoteDevInformation_t *psRemoteDevInfo,
                        NFCSTATUS status
                        );

/**
* \ingroup grp_mw_external_hal_funcs
*
* This callback type is used to provide received data and it's size to the
* upper layer in \ref phNfc_sData_t format ,when the upper layer has performed
* a Transceive operation on a tag or when the Device acts as an Initiator in a
* P2P transaction.
*
*
*/
typedef void (*pphHal4Nfc_TransceiveCallback_t) (
                                void *context,
                                phHal_sRemoteDevInformation_t *ConnectedDevice,
                                phNfc_sData_t  *pRecvdata,
                                NFCSTATUS status
                                );

/**
* \ingroup grp_mw_external_hal_funcs
*
* This callback type is used to provide received data and it's size to the
* upper layer in  \ref phNfc_sData_t structure, when the upper layer when the
* Device acts as a Target in a P2P transaction.
*
*
*/
typedef void (*pphHal4Nfc_ReceiveCallback_t) (
                                    void                *context,
                                    phNfc_sData_t       *pDataInfo,
                                    NFCSTATUS            status
                                    );

/**
* \ingroup grp_mw_external_hal_funcs
*
* Callback type to inform success or failure of the Ioctl calls
* made by upper layer. It may optionally contain response data
* depending on the Ioctl command issued.
*
*/
typedef void (*pphHal4Nfc_IoctlCallback_t) (void          *context,
                                            phNfc_sData_t *pOutData,
                                            NFCSTATUS      status );

/**
* \ingroup grp_mw_external_hal_funcs
*\if hal
*   \sa \ref pphHal4Nfc_GenCallback_t
* \endif
*
*/

/** Same as general callback type, used to inform the completion of
* \ref phHal4Nfc_Send call done by when in NFCIP1 Target mode
*/
typedef pphHal4Nfc_GenCallback_t pphHal4Nfc_SendCallback_t;

/**
* \ingroup grp_mw_external_hal_funcs
*
* Enum type to distinguish between normal init and test mode init
* to be done as part of phHal4Nfc_Open
* In test mode init only minimal initialization of the NFC Device
* sufficient to run the self test is performed.
*
* \note Note: No functional features can be accessed when
* phHal4Nfc_Open is called with TestModeOn
* \ref phHal4Nfc_Open
*
*/
typedef enum{
    eInitDefault = 0x00,     /**<Complete initialization for normal
                                 firmware operation*/
    eInitTestModeOn,         /**<Limited Initialization used for running self
                                tests */
    eInitCustom              /**<Reserved for Future Use */                  
} phHal4Nfc_InitType_t;

/**
* \ingroup grp_mw_external_hal_funcs
*
* Type to select the type of notification registration
* for Tags, P2P and SecureElement and Host Card Emulation events
*
* \if hal
* \ref phHal4Nfc_RegisterNotification,phHal4Nfc_UnregisterNotification
* \endif
*
*/
typedef enum{
    eRegisterDefault = 0x00,    /**<For All other generic notifications
                                     like Host Wakeup Notification */
    eRegisterTagDiscovery,      /**<For Tag Discovery notification*/
    eRegisterP2PDiscovery,      /**<For P2P Discovery notification*/
    eRegisterSecureElement,    /**<For Secure Element notification*/
    eRegisterHostCardEmulation /**<For notification related to Virtual
                                    Card Emulation from host */
} phHal4Nfc_RegisterType_t;

/**
* \ingroup grp_mw_external_hal_funcs
*
* Specifies the Remote Reader type,either initiator or ISO A/B or Felica
*
*/
typedef struct phHal4Nfc_TransactInfo{
    phHal_eRFDevType_t               remotePCDType;
}phHal4Nfc_TransactInfo_t;

/*preliminary definitions end*/

/* -----------------Exported Functions----------------------------------*/
/**
 *  \if hal
 *   \ingroup grp_hal_common
 *  \else
 *   \ingroup grp_mw_external_hal_funcs
 *  \endif
 *
 *  This function initializes and establishes a link to the NFC Device. This is
 *  a asynchronous call as it requires a series of setup calls with the NFC
 *  device. The open is complete when the status response callback <em>
 *   pOpenCallback </em> is called. It uses a Hardware Reference
 *  \ref phHal_sHwReference, allocated by the upper layer and the p_board_driver
 *  member initialized with the dal_instance (handle to the communication driver)
 *  and other members initialized to zero or NULL.
 *
 * \note
 *  - The device is in initialized state after the command has completed
 *    successfully.
 *
 *
 * \param[in,out] psHwReference Hardware Reference, pre-initialized by upper
 *                layer. Members of this structure are made valid if
 *                this function is successful. \n
 *
 * \param[in]     InitType Initialization type, used to differentiate between
 *                test mode limited initialization and normal init.
 *
 * \param[in]     pOpenCallback The open callback function called by the HAL
 *                when open (initialization) sequence is completed or if there
 *                is an error in initialization. \n
 *
 * \param[in]     pContext Upper layer context which will be included in the
 *                call back when request is completed. \n
 *
 * \retval NFCSTATUS_PENDING                 Open sequence has been successfully
 *                                           started and result will be conveyed
 *                                           via the pOpenCallback function.
 * \retval NFCSTATUS_ALREADY_INITIALISED     Device initialization already in
 *                                           progress.
 * \retval NFCSTATUS_INVALID_PARAMETER       The parameter could not be properly
 *                                           interpreted (structure uninitialized?).
 * \retval NFCSTATUS_INSUFFICIENT_RESOURCES  Insufficient resources for
 *                                           completing the request.
 * \retval Others                            Errors related to the lower layers.
 *
 * \if hal
 *  \sa \ref phHal4Nfc_Close,
 * \endif
 */
extern NFCSTATUS phHal4Nfc_Open(
                                phHal_sHwReference_t     *psHwReference,
                                phHal4Nfc_InitType_t      InitType,
                                pphHal4Nfc_GenCallback_t  pOpenCallback,
                                void                     *pContext
                                );



/**
 *  \if hal
 *   \ingroup grp_hal_common
 *  \else
 *   \ingroup grp_mw_external_hal_funcs
 *  \endif
 *
 *  Retrieves the capabilities of the device represented by the Hardware
 *  Reference parameter.The HW, FW versions,model-id and other capability
 *  information are located inside the pDevCapabilities parameter.
 *
 *  \param[in]  psHwReference     Hardware Reference, pre-initialized
 *                                by upper layer. \n
 *  \param[out] psDevCapabilities Pointer to the device capabilities structure
 *                                where all relevant capabilities of the
 *                                peripheral are stored. \n
 *  \param[in]  pContext          Upper layer context which will be included in
 *                                the call back when request is completed. \n
 *
 *  \retval NFCSTATUS_SUCCESS            Success and the psDevCapabilities is
 *                                       updated with info.
 *  \retval NFCSTATUS_INVALID_PARAMETER  One or more of the supplied parameters
 *                                       could not be properly interpreted.
 *  \retval NFCSTATUS_NOT_INITIALISED    Hal is not yet initialized.
 *  \retval Others                       Errors related to the lower layers.
 *
 */
extern NFCSTATUS phHal4Nfc_GetDeviceCapabilities(
                            phHal_sHwReference_t          *psHwReference,
                            phHal_sDeviceCapabilities_t   *psDevCapabilities,
                            void                          *pContext
                            );


/**
*  \if hal
*   \ingroup grp_hal_common
*  \else
*   \ingroup grp_mw_external_hal_funcs
*  \endif
*
*  This function is used to Configure discovery wheel (and start if
*  required) based on the discovery configuration passed.
*  This includes enabling/disabling of the Reader phases (A, B, F),
*  NFCIP1 Initiator Speed and duration of the Emulation phase.
*  Additional optional parameters for each of the features i.e. Reader,
*  Emulation and Peer2Peer can be set using the
* \ref phHal4Nfc_ConfigParameters function
*
*  \param[in] psHwReference         Hardware Reference, pre-initialized by
*                                   upper layer. \n
*
*  \param[in] discoveryMode         Discovery Mode allows to choose between:
*                                   discovery configuration and start, stop
*                                   discovery and start discovery (with last
*                                   set configuration).
*                                   \ref phHal_eDiscoveryConfigMode_t
*   \note Note: Presently only NFC_DISCOVERY_CONFIG is supported, other values
*               are for future use. When in Reader/Initiator mode it mandatory
*               to call phHal4Nfc_Connect before any transaction can be performed
*               with the discovered device.
*
*  \param[in] discoveryCfg          Discovery configuration parameters.
*                                   Reader A/Reader B, Felica 212, Felica 424,
*                                   NFCIP1 Speed, Emulation Enable and Duration.
*
*
*  \param[in] pConfigCallback       This callback has to be called once Hal
*                                   completes the Configuration.
*
*  \param[in] pContext              Upper layer context to be returned in the
*                                   callback.
*
*  \retval NFCSTATUS_INVALID_PARAMETER         Wrong Parameter values.
*
*  \retval NFCSTATUS_NOT_INITIALISED           Hal is not initialized.
*
*  \retval NFCSTATUS_BUSY                      Cannot Configure Hal in
*                                              Current state.
*
*  \retval NFCSTATUS_INSUFFICIENT_RESOURCES    System Resources insufficient.
*
*  \retval NFCSTATUS_PENDING                   Configuration request accepted
*                                              and Configuration is in progress.
*
*  \retval NFCSTATUS_INVALID_PARAMETER         One or more of the supplied
*                                              parameters could not be properly
*                                              interpreted.
*  \retval Others                              Errors related to the lower layers
*
*   \note Note: When in Reader/Initiator mode it mandatory
*               to call phHal4Nfc_Connect before any transaction can be performed
*               with the discovered device. Even if the HAL client is not
*               interested in using any of the discovered phHal4Nfc_Connect and
*               phHal4Nfc_Disconnect should be called to restart the Discovery
*               wheel
*
*  \ref phHal4Nfc_Connect, phHal4Nfc_Disconnect
*
*/
extern NFCSTATUS phHal4Nfc_ConfigureDiscovery(
                        phHal_sHwReference_t          *psHwReference,
                        phHal_eDiscoveryConfigMode_t   discoveryMode,
                        phHal_sADD_Cfg_t               *discoveryCfg,
                        pphHal4Nfc_GenCallback_t       pConfigCallback,
                        void                           *pContext
                        );
/**
*  \if hal
*   \ingroup grp_hal_common
*  \else
*   \ingroup grp_mw_external_hal_funcs
*  \endif
*
*  This function is used to set parameters of various features of the Hal,
*  based on the CfgType parameter. Presently following configuration
*  types are supported :-
*  \n 1. NFC_RF_READER_CONFIG (optional)-> Configure parameters for Reader A
*     or Reader B based on the configuration passed
*  \n 2. NFC_P2P_CONFIG (optional)-> Congfigure P2P parameters like
*     'General bytes', 'PSL Request' etc.
*  \n 3. NFC_EMULATION_CONFIG -> Enable and configure the emulation mode
*     parameters for either NFC Target, SmartMX, UICC and
*  \n   Card Emulation from Host (A, B, F)
*  All the configuration modes can be called independent of each other. The
*  setting will typically take effect for the next cycle of the relevant
*  phase of discovery. For optional configuration internal defaults will be
*  used in case the configuration is not set.
*  \note Card emulation from Host and Card Emulation from UICC are mutually
*  exclusive modes, i.e: only one can be enabled at a time. Using
*  this function to enable one of the emulation modes implicitly disables the
*  the other. eg. Setting Type A (or Type B) Emulation from Host disables
*  card emulation from UICC and vice versa.
*
*  \param[in] psHwReference         Hardware Reference, pre-initialized by
*                                   upper layer. \n
*
*  \param[in] eCfgType              Configuration type which can take one of the
*                                   enum values of \ref phHal_eConfigType_t. Each
*                                   config type is associated with its corresponding
*                                   information which is passed using the uCfg structure.
*
*
*  \param[in] uCfg                  Union containing configuration information,
*                                   which will be interpreted based on eCfgType
*                                   parameter.
*
*
*  \param[in] pConfigCallback       This callback has to be called once Hal
*                                   completes the Configuration.
*
*  \param[in] pContext              Upper layer context to be returned in the
*                                   callback.
*
*  \retval NFCSTATUS_INVALID_PARAMETER         Wrong Parameter values.
*
*  \retval NFCSTATUS_NOT_INITIALISED           Hal is not initialized.
*
*  \retval NFCSTATUS_BUSY                      Cannot Configure Hal in
*                                              Current state.
*
*  \retval NFCSTATUS_INSUFFICIENT_RESOURCES    System Resources insufficient.
*
*  \retval NFCSTATUS_PENDING                   Configuration request accepted
*                                              and Configuration is in progress.
*
*  \retval NFCSTATUS_INVALID_PARAMETER         One or more of the supplied
*                                              parameters could not be properly
*                                              interpreted.
*  \retval Others                              Errors related to the lower layers
*/

extern NFCSTATUS phHal4Nfc_ConfigParameters(
                        phHal_sHwReference_t     *psHwReference,
                        phHal_eConfigType_t       eCfgType,
                        phHal_uConfig_t          *uCfg,
                        pphHal4Nfc_GenCallback_t  pConfigCallback,
                        void                     *pContext
                        );

/**
 *  \if hal
 *   \ingroup grp_hal_nfci
 *  \else
 *   \ingroup grp_mw_external_hal_funcs
 *  \endif
 *
 *  This function is called to connect to a one (out of many if multiple
 *  devices are discovered) already discovered Remote Device notified
 *  through register notification. The  Remote Device Information structure is
 *  already pre-initialized with data (e.g. from Discovery Notificaiton
 *  Callback) A new session is started after the connect function returns
 *  successfully. The session ends with a successful disconnect
 *  (see  \ref phHal4Nfc_Disconnect).
 *
 *  \param[in]     psHwReference        Hardware Reference, pre-initialized by
 *                                      upper layer. \n
 *
 *  \param[in,out] psRemoteDevInfo      Points to the Remote Device Information
 *                                      structure. The members of it can be
 *                                      re-used from a previous session.
 *
 *  \param[in]     pNotifyConnectCb     Upper layer callback to be called for
 *                                      notifying Connect Success/Failure
 *
 *  \param[in]     pContext             Upper layer context to be returned in
 *                                      pNotifyConnectCb.
 *
 *  \retval NFCSTATUS_PENDING                  Request initiated, result will
 *                                             be informed through the callback.
 *  \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied
 *                                             parameters could not be
 *                                             properly interpreted.
 *  \retval NFCSTATUS_FAILED                   More than one phHal4Nfc_Connect
 *                                             is not allowed during a session
 *                                             on the same remote device. The
 *                                             session has to be closed before
 *                                             (see\ref phHal4Nfc_Disconnect).
 *  \retval NFCSTATUS_NOT_INITIALIZED          Hal is not initialized.
 *  \retval NFCSTATUS_FEATURE_NOT_SUPPORTED    Reactivation is not supported for 
 *                                             NfcIp target and Jewel/Topaz 
 *                                             remote device types.
 *  \retval NFCSTATUS_INVALID_REMOTE_DEVICE    The Remote Device Identifier is
 *                                             not valid.
 *  \retval Others                             Errors related to the lower layers.
 *
 *  \if hal
 *   \sa \ref phHal4Nfc_Disconnect
 *  \endif
 */
extern NFCSTATUS phHal4Nfc_Connect(
                            phHal_sHwReference_t          *psHwReference,
                            phHal_sRemoteDevInformation_t *psRemoteDevInfo,
                            pphHal4Nfc_ConnectCallback_t   pNotifyConnectCb,
                            void                          *pContext
                            );


/**
 *  \if hal
 *   \ingroup grp_hal_nfci
 *  \else
 *   \ingroup grp_mw_external_hal_funcs
 *  \endif
 *
 *  The phHal4Nfc_Transceive function allows to send data to and receive data
 *  from the Remote Device selected by the caller.It is also used by the
 *  NFCIP1 Initiator while performing a transaction with the NFCIP1 target.
 *  The caller has to provide the Remote Device Information structure and the
 *  command in order to communicate with the selected remote device.For P2P
 *  transactions the command type will not be used.
 *
 *
 *  \note the RecvData should be valid until the pTrcvCallback has been called.
 *
 *
 *  \param[in]      psHwReference       Hardware Reference, pre-initialized by
 *                                      upper layer. \n
 *
 *  \param[in,out]  psTransceiveInfo    Information required by transceive is
 *                                      concealed in this structure.It contains
 *                                      the send,receive buffers and their
 *                                      lengths.
 *
 *  \param[in]      psRemoteDevInfo     Points to the Remote Device Information
 *                                      structure which identifies the selected
 *                                      Remote Device.
 *
 *  \param[in]      pTrcvCallback       Callback function for returning the
 *                                      received response or error.
 *
 *  \param[in]      pContext            Upper layer context to be returned in
 *                                      the callback.
 *
 *  \retval NFCSTATUS_PENDING                Transceive initiated.pTrcvCallback
 *                                           will return the response or error.
 *  \retval NFCSTATUS_NOT_INITIALIZED        Hal is not initialized.
 *  \retval NFCSTATUS_SUCCESS                This status is used when send data
 *                                           length is zero and HAL contains
 *                                           previously more bytes from previous
 *                                           receive. \n
 *  \retval NFCSTATUS_INVALID_PARAMETER      One or more of the supplied
 *                                           parameters could not be properly
 *                                           interpreted or are invalid.
 *  \retval NFCSTATUS_INVALID_DEVICE         The device has not been opened or
 *                                           has been disconnected meanwhile.
 *  \retval NFCSTATUS_FEATURE_NOT_SUPPORTED  Transaction on this Device type is
 *                                           not supported.
 *  \retval NFCSTATUS_BUSY                   Previous transaction is not
 *                                           completed.
 *  \retval NFCSTATUS_INSUFFICIENT_RESOURCES System resources are insufficient
 *                                           to complete the request at that
 *                                           point in time.
 *  \retval NFCSTATUS_MORE_INFORMATION       Received number of bytes is greater
 *                                           than receive buffer provided by the
 *                                           upper layer.Extra bytes will be
 *                                           retained by Hal and returned on next
 *                                           call to transceive.
 *  \retval Others                           Errors related to the lower layers.
 *
 */
extern NFCSTATUS phHal4Nfc_Transceive(
                            phHal_sHwReference_t            *psHwReference,
                            phHal_sTransceiveInfo_t         *psTransceiveInfo,
                            phHal_sRemoteDevInformation_t   *psRemoteDevInfo,
                            pphHal4Nfc_TransceiveCallback_t  pTrcvCallback,
                            void                            *pContext
                            );




/**
 *  \if hal
 *   \ingroup grp_hal_nfci
 *  \else
 *   \ingroup grp_mw_external_hal_funcs
 *  \endif
 *
 *  The function allows to disconnect from a specific Remote Device. This
 *  function closes the session opened with \ref phHal4Nfc_Connect "Connect".It
 *  is also used to switch from wired to virtual mode in case the discovered
 *  device is SmartMX in wired mode. The status of discovery wheel after
 *  disconnection is determined by the ReleaseType parameter.
 *
 *
 *
 *  \param[in]      psHwReference       Hardware Reference, pre-initialized by
 *                                      upper layer. \n
 *  \param[in,out]  psRemoteDevInfo     Points to the valid (connected) Remote
 *                                      Device Information structure.
 *
 *  \param[in]      ReleaseType         Defines various modes of releasing an acquired
 *                                      target or tag
 *
 *  \param[in]      pDscntCallback      Callback function to notify
 *                                      disconnect success/error.
 *
 *  \param[in]      pContext            Upper layer context to be returned in
 *                                      the callback.
 *
 *
 *  \retval NFCSTATUS_PENDING                Disconnect initiated.pDscntCallback
 *                                           will return the response or error.
 *  \retval NFCSTATUS_INVALID_PARAMETER      One or more of the supplied
 *                                           parameters could not be properly
 *                                           interpreted.
 *  \retval NFCSTATUS_INVALID_REMOTE_DEVICE  The device has not been opened
 *                                           before or has already been closed.
 *  \retval NFCSTATUS_NOT_INITIALIZED        Hal is not initialized.
 *  \retval Others                           Errors related to the lower layers.
 *
 *  \if hal
 *   \sa \ref phHal4Nfc_Connect
 *  \endif
 */
extern NFCSTATUS phHal4Nfc_Disconnect(
                            phHal_sHwReference_t          *psHwReference,
                            phHal_sRemoteDevInformation_t *psRemoteDevInfo,
                            phHal_eReleaseType_t           ReleaseType,
                            pphHal4Nfc_DiscntCallback_t    pDscntCallback,
                            void                          *pContext
                            );

/**
*  \if hal
*   \ingroup grp_hal_common
*  \else
*   \ingroup grp_mw_external_hal_funcs
*  \endif
*
*  The function allows to do a one time check on whether the connected target
*  is still present in the field of the Reader. The call back returns the
*  result of the presence check sequence indicating whether it is still present
*  or moved out of the reader field.
*
*  \param[in]     psHwReference     Hardware Reference, pre-initialized by
*                                   upper layer. \n
*
*  \param[in]     pPresenceChkCb    Callback function called on completion of the
*                                   presence check sequence or in case an error
*                                   has occurred..
*
*  \param[in]     context          Upper layer context to be returned in the
*                                   callback.
*
*  \retval NFCSTATUS_PENDING           Call successfully issued to lower layer.
*                                      Status will be returned in pPresenceChkCb.
*
*  \retval NFCSTATUS_NOT_INITIALISED   The device has not been opened or has
*                                      been disconnected meanwhile.
*
*  \retval NFCSTATUS_BUSY              Previous presence check callback has not
*                                      been received
*
*  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
*                                      could not be properly interpreted.
*
*  \retval NFCSTATUS_RELEASED          P2P target has been released by Initiator.
*  \retval Others                      Errors related to the lower layers
*
*/
extern NFCSTATUS phHal4Nfc_PresenceCheck(
                                  phHal_sHwReference_t     *psHwReference,
                                  pphHal4Nfc_GenCallback_t  pPresenceChkCb,
                                  void                     *context
                                  );


/**
 *  \if hal
 *   \ingroup grp_hal_common
 *  \else
 *   \ingroup grp_mw_external_hal_funcs
 *  \endif
 *
 *  The I/O Control function allows the caller to use (vendor-) specific
 *  functionality provided by the lower layer or by the hardware. Each feature
 *  is accessible via a specific IOCTL Code and has to be documented by the
 *  provider of the driver and the hardware.
 *  See "IOCTL Codes" for the definition of a standard command set.\n
 *
 *
 *  \param[in]      psHwReference       Hardware Reference, pre-initialized by
 *                                      upper layer. \n
 *  \param[in]      IoctlCode           Control code for the operation.
 *                                      This value identifies the specific
 *                                      operation to be performed and are defined
 *                                      in  \ref phNfcIoctlCode.h
 *
 *  \param[in]      pInParam            Pointer to any input data structure
 *                                      containing data which is interpreted
 *                                      based on Ioctl code and the length of
 *                                      the data.
 *
 *  \param[in]      pOutParam           Pointer to output data structure
 *                                      containing data which is returned as a
 *                                      result of the Ioctl operation and the
 *                                      length of the data.
 *
 *  \param[in]      pIoctlCallback      callback function called in case an
 *                                      error has occurred while performing
 *                                      requested operation,or on successful
 *                                      completion of the request
 *
 *  \param[in]      pContext            Upper layer context to be returned in
 *                                      the callback.
 *
 *  \retval NFCSTATUS_SUCCESS           Success.
 *  \retval NFCSTATUS_PENDING           Call issued to lower layer.Status will
 *                                      be notified in pIoctlCallback.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be properly interpreted.
 *  \retval NFCSTATUS_NOT_INITIALIZED   Hal is not initialized.
 *  \retval Others                      Errors related to the lower layers.
 *
 */
extern NFCSTATUS phHal4Nfc_Ioctl(
                                phHal_sHwReference_t       *psHwReference,
                                uint32_t                    IoctlCode,
                                phNfc_sData_t              *pInParam,
                                phNfc_sData_t              *pOutParam,
                                pphHal4Nfc_IoctlCallback_t  pIoctlCallback,
                                void                       *pContext
                                );



/**
 *  \if hal
 *   \ingroup grp_hal_common
 *  \else
 *   \ingroup grp_mw_external_hal_funcs
 *  \endif
 *
 *  Closes the link to the NFC device. All configurations/setups
 *  done until now are invalidated.To restart communication, phHal4Nfc_Open
 *  needs to be called. The pClosecallback is called when all steps
 *  in the close sequence are completed.
 *
 *
 *  \param[in]     psHwReference        Hardware Reference, pre-initialized by
 *                                      upper layer. \n
 *
 *  \param[in]     pCloseCallback       Callback function called on completion of
 *                                      the close sequence or in case an error
 *                                      has occurred..
 *
 *  \param[in]     pContext             Upper layer context to be returned
 *                                      in the callback.
 *
 *  \retval NFCSTATUS_SUCCESS           Closing successful.
 *  \retval NFCSTATUS_NOT_INITIALIZED   The device has not been opened or has
 *                                      been disconnected meanwhile.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be properly interpreted.
 *  \retval NFCSTATUS_BUSY              Configuration is in progress.Shutdown
 *                                      is not allowed until configure complete.
 *  \retval Others                      Errors related to the lower layers.
 *
 *  \if hal
 *   \sa \ref phHal4Nfc_Open
 *  \endif
 */
extern NFCSTATUS phHal4Nfc_Close(
                                phHal_sHwReference_t     *psHwReference,
                                pphHal4Nfc_GenCallback_t  pCloseCallback,
                                void                     *pContext
                                );


/**
 *  \if hal
 *   \ingroup grp_hal_common
 *  \else
 *   \ingroup grp_mw_external_hal_funcs
 *  \endif
 *
 *  Forcibly shutdown the HAl.This API makes a call to forcibly shutdown the
 *  lower layer and frees all resources in use by Hal before shutting down.The
 *  API always succeeds.It does not however reset the target.
 *
 *  \param[in]     psHwReference        Hardware Reference, pre-initialized by
 *                                      upper layer. \n
 *
 *  \param[in]     pConfig              Reserved for future use.
 *
 *
 */
extern void phHal4Nfc_Hal4Reset(
                                phHal_sHwReference_t *psHwReference,
                                void                 *pConfig
                                );


/**
*  \if hal
*   \ingroup grp_hal_common
*  \else
*   \ingroup grp_mw_external_hal_funcs
*  \endif
*
*  The function is used by the NFCIP1 Target to respond to packect received
*  from NFCIP1 initiator. pSendCallback()
*  is called , when all steps in the send sequence are completed.
*
*  \param[in]     psHwReference         Hardware Reference, pre-initialized by
*                                       upper layer. \n
*
*  \param[in]     psTransactInfo        information required for transferring
*                                       the data
*
*  \param[in]     sTransferData         Data and the length of the data to be
*                                       transferred
*
*  \param[in]     pSendCallback         Callback function called on completion
*                                       of the NfcIP sequence or in case an
*                                       error has occurred.
*
*  \param[in]     pContext              Upper layer context to be returned in
*                                       the callback.
*
*  \retval NFCSTATUS_PENDING            Send is in progress.
*  \retval NFCSTATUS_INVALID_DEVICE     The device has not been opened or has
*                                       been disconnected meanwhile.
*  \retval NFCSTATUS_INVALID_PARAMETER  One or more of the supplied parameters
*                                       could not be properly interpreted.
*  \retval NFCSTATUS_NOT_INITIALIZED    Hal is not initialized.
*  \retval Others                       Errors related to the lower layers.
*
*
*/
extern
NFCSTATUS
phHal4Nfc_Send(
                phHal_sHwReference_t                    *psHwReference,
                phHal4Nfc_TransactInfo_t                *psTransactInfo,
                phNfc_sData_t                            sTransferData,
                pphHal4Nfc_SendCallback_t                pSendCallback,
                void                                    *pContext
                );

/**
*  \if hal
*   \ingroup grp_hal_common
*  \else
*   \ingroup grp_mw_external_hal_funcs
*  \endif
*
*  This function is called by the NfcIP Peer to wait for receiving data from
*  the other peer.It is used only by the NfcIP Target.
*  \note NOTE: After this function is called, its mandatory to wait for the
*  pphHal4Nfc_ReceiveCallback_t callback, before calling any other function.
*  Only functions allowed are phHal4Nfc_Close() and phHal4Nfc_Hal4Reset().
*
*
*  \param[in]     psHwReference         Hardware Reference, pre-initialized by
*                                       upper layer. \n
*
*  \param[in]     psTransactInfo            information required for transferring the
*                                       data
*
*  \param[in]     pReceiveCallback      Callback function called after receiving
*                                       the data or in case an error has
*                                       has occurred.
*
*  \param[in]     pContext              Upper layer context to be returned
*                                       in the callback.
*
*  \retval NFCSTATUS_PENDING            Receive is in progress.
*  \retval NFCSTATUS_INVALID_DEVICE     The device has not been opened or has
*                                       been disconnected meanwhile.
*  \retval NFCSTATUS_INVALID_PARAMETER  One or more of the supplied parameters
*                                       could not be properly interpreted.
*  \retval NFCSTATUS_NOT_INITIALIZED    Hal is not initialized.
*  \retval Others                       Errors related to the lower layers
*
*/
extern
NFCSTATUS
phHal4Nfc_Receive(
                  phHal_sHwReference_t                  *psHwReference,
                  phHal4Nfc_TransactInfo_t              *psTransactInfo,
                  pphHal4Nfc_ReceiveCallback_t           pReceiveCallback,
                  void                                  *pContext
                 );


/**
*  \if hal
*   \ingroup grp_hal_common
*  \else
*   \ingroup grp_mw_external_hal_funcs
*  \endif
*
*  This API is a synchronous call used to register a listener for either tag
*  discovery, Secure element notification or P2P Notification or a general
*  notification handler for all the three.
*
*
*  \param[in] psHwRef               Hardware Reference, pre-initialized by
*                                   upper layer. \n
*
*  \param[in] eRegisterType         Type of Notification registered.Informs
*                                   whether upper layer is interested in Tag
*                                   Discovery,secure element or P2P notification.
*
*  \param[in] pNotificationHandler  Notification callback.If this parameter is
*                                   NULL,any notification from Hci will be
*                                   ignored and upper layer will not be notified
*                                   of the event.
*
*  \param[in] Context              Upper layer context.
*
*  \retval NFCSTATUS_SUCCESS            Notification unregister successful.
*
*  \retval NFCSTATUS_INVALID_PARAMETER  One or more of the supplied parameters
*                                       could not be properly interpreted.
*  \retval NFCSTATUS_NOT_INITIALIZED    Hal is not initialized.
*
*/
extern NFCSTATUS phHal4Nfc_RegisterNotification(
                            phHal_sHwReference_t      *psHwRef,
                            phHal4Nfc_RegisterType_t   eRegisterType,
                            pphHal4Nfc_Notification_t  pNotificationHandler,
                            void                      *Context
                            );

/**
*  \if hal
*   \ingroup grp_hal_common
*  \else
*   \ingroup grp_mw_external_hal_funcs
*  \endif
*
*  This API is a synchronous call used to unregister a listener for either tag
*  discovery, Secure element notification or P2P Notification, previously
*  registered using \ref phHal4Nfc_RegisterNotification.
*
*  \param[in]   psHwReference           Hardware Reference, pre-initialized by
*                                       upper layer. \n
*
*  \param[in]   eRegisterType           Type of registration ,tells whether upper
*                                       layer is interested in unregistering for
*                                       Tag Discovery,Secure element or P2P. \n
*
*  \param[in]   Context                Upper layer context.
*
*  \retval NFCSTATUS_SUCCESS            Notification unregister successful.
*
*  \retval NFCSTATUS_INVALID_PARAMETER  One or more of the supplied parameters
*                                       could not be properly interpreted.
*
*  \retval NFCSTATUS_NOT_INITIALIZED    Hal is not initialized.
*
*
*/
extern NFCSTATUS phHal4Nfc_UnregisterNotification(
                                    phHal_sHwReference_t     *psHwReference,
                                    phHal4Nfc_RegisterType_t  eRegisterType,
                                    void                     *Context
                                    );


/**
*  \if hal
*   \ingroup grp_hal_common
*  \else
*   \ingroup grp_mw_external_hal_funcs
*  \endif
*
*  This function is called to switch the SmartMX to Wired Mode. After switching
* to Wired mode the SmartMX can be discovered through Tag Discovery like a normal
* tag and used in the same manner as a tag. SmartMx returns to previous mode
* (Virtual or Off) when the tag is relased by phHal4Nfc_Disconnect
*
*
*  \param[in]  psHwReference            Hardware Reference, pre-initialized by
*                                       upper layer. \n
*
*  \param[in]  smx_mode                 Mode to which the switch should be made.
*
*  \param[in]  pSwitchModecb            Callback for Switch mode complete
*                                       with success/error notification.
*
*  \param[in]  pContext                 Upper layer context.
*
*  \retval NFCSTATUS_PENDING                    Switch in progress.Status will be
*                                               returned in pSwitchModecb.
*  \retval NFCSTATUS_INVALID_PARAMETER          One or more of the supplied
*                                               parameters could not be properly
*                                               interpreted.
*  \retval NFCSTATUS_NOT_INITIALIZED            Hal is not initialized.
*  \retval NFCSTATUS_BUSY                       Configuration in Progress or
*                                               remote device is connected.
*  \retval NFCSTATUS_INSUFFICIENT_RESOURCES     System resources are insufficient
*                                               to complete the request at that
*                                               point in time.
* \retval NFCSTATUS_FAILED                      No listener has been registered
*                                               by the upper layer for Emulation
*                                               before making this call.
*  \retval Others                               Errors related to the lower
*                                               layers.
*/
extern NFCSTATUS phHal4Nfc_Switch_SMX_Mode(
                                    phHal_sHwReference_t      *psHwReference,
                                    phHal_eSmartMX_Mode_t      smx_mode,
                                    pphHal4Nfc_GenCallback_t   pSwitchModecb,
                                    void                      *pContext
                                    );


/**
*  \if hal
*   \ingroup grp_hal_common
*  \else
*   \ingroup grp_mw_external_hal_funcs
*  \endif
*
*  This function is called to switch the UICC on or Off. 
*
*
*  \param[in]  psHwReference            Hardware Reference, pre-initialized by 
*                                       upper layer. \n
*
*  \param[in]  smx_mode                 Mode to which the switch should be made. 
*
*  \param[in]  pSwitchModecb            Callback for Switch mode complete
*                                       with success/error notification.
*
*  \param[in]  pContext                 Upper layer context.
*
*  \retval NFCSTATUS_PENDING                    Switch in progress.Status will be
*                                               returned in pSwitchModecb.
*  \retval NFCSTATUS_INVALID_PARAMETER          One or more of the supplied 
*                                               parameters could not be properly
*                                               interpreted.
*  \retval NFCSTATUS_NOT_INITIALIZED            Hal is not initialized.
*  \retval NFCSTATUS_BUSY                       Configuration in Progress or 
*                                               remote device is connected.
*  \retval NFCSTATUS_INSUFFICIENT_RESOURCES     System resources are insufficient
*                                               to complete the request at that
*                                               point in time.
* \retval NFCSTATUS_FAILED                      No listener has been registered 
*                                               by the upper layer for Emulation
*                                               before making this call.
*  \retval Others                               Errors related to the lower 
*                                               layers.
*/
extern NFCSTATUS phHal4Nfc_Switch_Swp_Mode(                                    
                                    phHal_sHwReference_t      *psHwReference,
                                    phHal_eSWP_Mode_t          swp_mode,
                                    pphHal4Nfc_GenCallback_t   pSwitchModecb,
                                    void                      *pContext
                                    );

#endif /* end of PHHAL4NFC_H */


