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
 * \file  phFriNfc_OvrHal.h
 * \brief Overlapped HAL
 *
 * Project: NFC-FRI
 * Creator: Gerald Kersch
 *
 * $Date: Tue May 19 10:30:18 2009 $
 * Changed by: $Author: ing07336 $
 * $Revision: 1.13 $
 * $Aliases: NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
 *
 */

#ifndef PHFRINFC_OVRHAL_H
#define PHFRINFC_OVRHAL_H

#include <phFriNfc.h>
#ifdef PH_HAL4_ENABLE
#include <phHal4Nfc.h>
#else
#include <phHalNfc.h>
#endif
#include <phNfcCompId.h>
#include <phNfcStatus.h>


/**
 *  \name Overlapped HAL
 *
 * File: \ref phFriNfc_OvrHal.h
 *
 */
/*@{*/
#define PH_FRINFC_OVRHAL_FILEREVISION "$Revision: 1.13 $" /** \ingroup grp_file_attributes */
#define PH_FRINFC_OVRHAL_FILEALIASES  "$Aliases: NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"      /** \ingroup grp_file_attributes */
/*@}*/


/** \defgroup grp_fri_nfc_ovr_hal Overlapped HAL
 *
 *  This component encapsulates the HAL functions, suited for the NFC-FRI overlapped way of operation. The HAL itself
 *  is used as it is, wrapped by this component. The purpose of the wrapper is to de-couple a blocking I/O, as used by
 *  the HAL, from the overlapped I/O operation mode the FRI is using.
 *
 *  \par Device Based Functions
 *  NFC Device Based Functions are used to address the NFC device (local device) directly.
 *  These are all functions that use no Remote Device Information.
 *
 *  \par Connection Based Functions
 *  Connection Based Functions use the Remote Device Information to describe a connection
 *  to a certain Remote Device.
 *
 *  \par Component Instance Sharing
 *  FRI components accessing one NFC device share one instance of the Overlapped HAL. Therefore
 *  each calling FRI component must specify - together with the call - where to deliver the
 *  response of the overlapped operation.
 *
 *  \par Lowest Layer
 *  The Overlapped HAL represents the NFC Device, the lowest layer of the FRI components.
 *
 *  \par Completion Forced
 *  The \b HAL \b functions (and underlying functions) of this library must complete before a new call can
 *  be issued. No HAL operation must be pending.
 *
 */
/*@{*/

/**
 *  \name OVR HAL Constants
 */
/*@{*/
#define PH_FRINFC_OVRHAL_MAX_NUM_MOCKUP_PARAM           255    /**< Number of mockup indices that are are prepared. */
/* Harsha: changed from 48 to 128, to work with the Mifare 4k TCs */
#define PH_FRINFC_OVRHAL_MAX_NUM_MOCKUP_RDI             4     /**< Max. number of mockup RDIs. */
#define PH_FRINFC_OVRHAL_MAX_TEST_DELAY                 1000  /**< Max. test delay in OVR HAL. */
#define PH_FRINFC_OVRHAL_POLL_PAYLOAD_LEN               5     /**< Length of the POLL payload. */ /* @GK/5.6.06 */
/*@}*/
/*@}*/ /* defgroup... */

/** \defgroup grp_ovr_hal_cmd Overlapped HAL Command List
 *  \ingroup grp_fri_nfc_ovr_hal
 *  These are the command definitions for the Overlapped HAL. They are used internally by the
 *  implementation of the component.
 */
/*@{*/
#define PH_FRINFC_OVRHAL_NUL             (0)     /**< \brief We're in NO command */

#define PH_FRINFC_OVRHAL_ENU             (1)     /**< \brief Enumerate */
#define PH_FRINFC_OVRHAL_OPE             (2)     /**< \brief Open */
#define PH_FRINFC_OVRHAL_CLO             (3)     /**< \brief Close */
#define PH_FRINFC_OVRHAL_GDC             (4)     /**< \brief Get Dev Caps */
#define PH_FRINFC_OVRHAL_POL             (5)     /**< \brief Poll */
#define PH_FRINFC_OVRHAL_CON             (6)     /**< \brief Connect */
#define PH_FRINFC_OVRHAL_DIS             (7)     /**< \brief Disconnect */
#define PH_FRINFC_OVRHAL_TRX             (8)     /**< \brief Transceive */
#define PH_FRINFC_OVRHAL_STM             (9)     /**< \brief Start Target Mode */
#define PH_FRINFC_OVRHAL_SND             (10)     /**< \brief Send */
#define PH_FRINFC_OVRHAL_RCV             (11)    /**< \brief Receive */
#define PH_FRINFC_OVRHAL_IOC             (12)    /**< \brief IOCTL */

#define PH_FRINFC_OVRHAL_TST             (255)   /**< \brief OVR HAL test-related command */

/** \ingroup grp_fri_nfc_ovr_hal
 *  \brief Post Message Function for Overlapped HAL
 *
 *  \copydoc page_reg
 *
 * This is required by the Overlapped HAL in order to call the blocking (original HAL) in another
 * thread. This function is required in addition to \ref pphFriNfc_OvrHalPresetParm to be
 * implemented in the integrating software.
 *
 * \par First Parameter: Context of the Integration
 *      Set to the value, the Integration has provided when initialising this component.
 */
typedef void (*pphFriNfc_OvrHalPostMsg_t)(void*);

/** \ingroup grp_fri_nfc_ovr_hal
 *  \brief Abort Function (to be defined/implemented by the Integration)
 *
 *  \copydoc page_reg
 *
 * This is required by the Overlapped HAL in order abort a pending Overlapped HAL operation. This funtion will be
 * internally called by the \ref phFriNfc_OvrHal_Abort function.
 *
 * \par First Parameter: Context of the Integration
 *      Set to the value, the Integration has provided when initialising this component.
 *
 * \par Return value:
 *      As defined by the integration
 */
typedef NFCSTATUS (*pphFriNfc_OvrHalAbort_t)(void*);


typedef void (*pphOvrHal_CB_t) (phHal_sRemoteDevInformation_t *RemoteDevHandle,
                                NFCSTATUS status,
                                phNfc_sData_t  *pRecvdata,
                                void *context);

/** \ingroup grp_fri_nfc_ovr_hal
 *  \brief Preset Function to prepare the parameters in the HAL
 *
 *  \copydoc page_reg
 *
 * This function (pointer) is called by the Overlapped HAL to prepare the function call parameters
 * in the HAL before posting the start message. As we have an asynchronously running FRI, but a
 * synchronous HAL, the calls need to be "decoupled". This means, the HAL needs to run under
 * a different time-base (or thread/task etc.). The consequence is that the data exchange between
 * FRI and HAL must be done as required by the integration/system itself. The declaration
 * of the function pointer allows for the integrating software to implement whatever functionality
 * is required to convey the data.
 *
 *
 * \par First Parameter
 *      Context of the Integration Set to the value, the Integration has provided when initialising
 *      this component.
 *
 * \par Second Parameter:
 *      \b HAL \b Command, as defined in the module \ref grp_ovr_hal_cmd.
 *
 * \par Third Parameter:
 *      \b Pointers to a specific structure containing the parameters of the HAL functions to be
 *      called.
 *
 * \par Forth parameter:
 *      Immediate Operation result (not the result of the HAL operation). Usually this is
 *      \ref NFCSTATUS_PENDING (for a successfully triggered HAL I/O or an error value that is
 *      returned by the HAL immediately, such as \ref NFCSTATUS_INVALID_PARAMETER.
 *
 * \par Return value:
 *      A boolean (\ref grp_special_conventions) value. The integration implementation must ensure
 *      that, if the function \b succeeds, the return value is \b TRUE, otherwise false.
 */
typedef uint8_t (*pphFriNfc_OvrHalPresetParm)(void*, uint16_t, void*, NFCSTATUS*);

/** \ingroup grp_fri_nfc_ovr_hal
 *  \brief Overlapped HAL Context
 *
 *  The Overlapped HAL structure. This structure contains the HAL "context" that
 *  is required by the FRI on a connection basis. Please note that the Overlapped HAL is
 *  a shared component, requiring a special completion notification mechanism.
 *  Read more in the description of this component.
 *
 */
typedef struct phFriNfc_OvrHal
{
    /** Currently active operation of the component. If no operation is pending, the content of this member is
     *  \ref PH_FRINFC_OVRHAL_NUL .  The component refuses a new call if the contenet is different, namely one
     *  of the other values defined in \ref grp_ovr_hal_cmd .
     */
    uint8_t                         Operation;

    /** The \b temporary pointer to the completion routine information. The HAL needs - for each call - to be told about the
     *  completion routine of the upper (calling) component. This major difference to other components is because
     *  some functions of the HAL are connection-based and some are not. Moreover it is because the HAL is shared
     *  among the FRI components. So, with a variety of potential callers it is required for each caller to instruct
     *  the HAL about the "delivery" address of the response for each individual call.
     */
    phFriNfc_CplRt_t                TemporaryCompletionInfo;
    phFriNfc_CplRt_t                TemporaryRcvCompletionInfo;
    phFriNfc_CplRt_t                TemporarySndCompletionInfo;

    /** Points to a function within the Integration that presets the parameters for the actual
     *  HAL call.
     */
    pphFriNfc_OvrHalPresetParm      Presetparameters;

    /** Posts a message to the actual HAL integration, starting a  NFC HAL I/O with the pre-set
     *  parameters.
     */
    pphFriNfc_OvrHalPostMsg_t       PostMsg;

    /** The context of the Integration (the SW around this component). This is needed to let
     *  the Overlapped HAL access the Integration's functionality to post a message to another
     *  thread.
     */
    void                           *IntegrationContext;

    /** Device reference returned during enumeration: This has to be filled in by the integrating software after
        a call to the HAL Enumerate function (not contained in the overlapped HAl API). */
    phHal_sHwReference_t           *psHwReference;

    /** This flag is set by the ABORT function. The OVR HAL then does no I/O to the real HAL
     *  or to the mockup any more but just completed with the ABORTED status.
     */
    uint8_t OperationAborted;

    /** Abort function to be implemented by the integration. This parameter can be (optionally) initialized
     *  via the call of \ref phFriNfc_OvrHal_Reset_Abort function.
     *  If it is not NULL, the function pointed by \ref will be internally called by the \ref phFriNfc_OvrHal_Abort function.
     */
    pphFriNfc_OvrHalAbort_t      AbortIntegrationFunction;

    /** Integration-defined Context passed as a parameter of the \ref AbortIntegrationFunction.
     */
    void*                        AbortIntegrationContext;
    
    void*                        OvrCompletion;

    phHal_sTransceiveInfo_t      TranceiveInfo;
    
    /** TODO
     */
    phNfc_sData_t                sReceiveData;

    /** TODO
     */
    phNfc_sData_t                sSendData;

    /** TODO
     */
    phHal4Nfc_TransactInfo_t     TransactInfo;

    uint16_t                     *pndef_recv_length;
} phFriNfc_OvrHal_t;

/**
 * \ingroup grp_fri_nfc_ovr_hal
 *
 * \brief Transceive Data to/from a Remote Device
 *
 * \copydoc page_ovr
 *
 * \param[in]      OvrHal               Component Context.
 * \param[in]      CompletionInfo       \copydoc phFriNfc_OvrHal_t::TemporaryCompletionInfo
 * \param[in,out]  RemoteDevInfo        Remote Device Information.
 * \param[in]      Cmd                  Command to perform.
 * \param[out]     DepAdditionalInfo    Protocol Information.
 * \param[in]      SendBuf              Pointer to the data to send.
 * \param[in]      SendLength           Length, in bytes, of the Send Buffer.
 * \param[out]     RecvBuf              Pointer to the buffer that receives the data.
 * \param[in,out]  RecvLength           Length, in bytes, of the received data.
 *
 * \retval NFCSTATUS_PENDING                The operation is pending.
 * \retval NFCSTATUS_INVALID_DEVICE_REQUEST \copydoc phFriNfc_OvrHal_t::Operation
 * \retval NFCSTATUS_SUCCESS                Success.
 * \retval NFCSTATUS_INVALID_PARAMETER      One or more of the supplied parameters could not be
 *                                          properly interpreted.
 * \retval NFCSTATUS_INVALID_DEVICE         The device has not been opened or has been disconnected
 *                                          meanwhile.
 * \retval NFCSTATUS_CMD_ABORTED            The caller/driver has aborted the request.
 * \retval NFCSTATUS_BUFFER_TOO_SMALL       The buffer provided by the caller is too small.
 * \retval NFCSTATUS_RF_TIMEOUT             No data has been received within the TIMEOUT period.
 *
 * \note Please refer to HAL Transceive for a detailed description of the
 *       underlying function and the propagated parameters.
 *
 */

NFCSTATUS phFriNfc_OvrHal_Transceive(phFriNfc_OvrHal_t              *OvrHal,
                                     phFriNfc_CplRt_t               *CompletionInfo,
                                     phHal_sRemoteDevInformation_t  *RemoteDevInfo,
                                     phHal_uCmdList_t                Cmd,
                                     phHal_sDepAdditionalInfo_t     *DepAdditionalInfo,
                                     uint8_t                        *SendBuf,
                                     uint16_t                        SendLength,
                                     uint8_t                        *RecvBuf,
                                     uint16_t                       *RecvLength);

/**
 * \ingroup grp_fri_nfc_ovr_hal
 *
 * \brief TODO
 *
 */
NFCSTATUS phFriNfc_OvrHal_Receive(phFriNfc_OvrHal_t              *OvrHal,
                                  phFriNfc_CplRt_t               *CompletionInfo,
                                  phHal_sRemoteDevInformation_t  *RemoteDevInfo,
                                  uint8_t                        *RecvBuf,
                                  uint16_t                       *RecvLength);

/**
 * \ingroup grp_fri_nfc_ovr_hal
 *
 * \brief TODO
 *
 */
NFCSTATUS phFriNfc_OvrHal_Send(phFriNfc_OvrHal_t              *OvrHal,
                               phFriNfc_CplRt_t               *CompletionInfo,
                               phHal_sRemoteDevInformation_t  *RemoteDevInfo,
                               uint8_t                        *SendBuf,
                               uint16_t                       SendLength);


NFCSTATUS phFriNfc_OvrHal_Reconnect(phFriNfc_OvrHal_t              *OvrHal,
                                    phFriNfc_CplRt_t               *CompletionInfo,
                                    phHal_sRemoteDevInformation_t  *RemoteDevInfo);


NFCSTATUS phFriNfc_OvrHal_Connect(phFriNfc_OvrHal_t              *OvrHal,
                                  phFriNfc_CplRt_t               *CompletionInfo,
                                  phHal_sRemoteDevInformation_t  *RemoteDevInfo,
                                  phHal_sDevInputParam_t         *DevInputParam);

#endif
