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
* =========================================================================== *
*                                                                             *
*                                                                             *
* \file  phHciNfc.h                                                           *
* \brief HCI Header for the Generic HCI Management.                           *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Mon Apr  5 14:37:06 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.39 $                                                           *
* $Aliases: NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $                    
*                                                                             *
* =========================================================================== *
*/


/*@{*/

#ifndef PHHCINFC_H
#define PHHCINFC_H

/*@}*/
/**
 *  \name HCI
 *
 * File: \ref phHciNfc.h
 *
 */
/*@{*/
#define PH_HCINFC_FILEREVISION "$Revision: 1.39 $" /**< \ingroup grp_file_attributes */
#define PH_HCINFC_FILEALIASES  "$Aliases: NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"  /**< \ingroup grp_file_attributes */
/*@}*/

/*
################################################################################
***************************** Header File Inclusion ****************************
################################################################################
*/

#include <phNfcStatus.h>
#include <phNfcInterface.h>
#ifdef ANDROID
#include <string.h>
#endif

/*
################################################################################
****************************** Macro Definitions *******************************
################################################################################
*/


/*
################################################################################
******************** Enumeration and Structure Definition **********************
################################################################################
*/

typedef enum phHciNfc_Init
{
  HCI_SESSION = 0x00,
  HCI_NFC_DEVICE_TEST,
  HCI_CUSTOM_INIT,
  HCI_SELF_TEST
}phHciNfc_Init_t;

/** \ingroup  grp_hal_hci
 *
 * \if hal
 *  \brief HCI Tag Exchange Information
 * \else
 *  \brief HCI-Specific
 * \endif
 *
 *  The <em> Tag Exchange Info Structure </em> holds the exchange information to
 *  the connected tag .
 *
 *  \note All members of this structure are in parameters [in].
 *
 */

typedef struct phHciNfc_Tag_XchgInfo
{
    /** \internal RF Reader Command Type */
    uint8_t                     cmd_type;
    /** \internal Address Field required for only Mifare
     *  Family Proprietary Cards.
     *  The Address Size is Valid only upto 255 Blocks limit
     *  i:e for Mifare 4K
     */
    uint8_t                     addr;
}phHciNfc_Tag_XchgInfo_t;

/** \ingroup  grp_hal_hci
 *
 * \if hal
 *  \brief HCI NFC-IP Exchange Information
 * \else
 *  \brief HCI-Specific
 * \endif
 *
 *  The <em> NFC-IP Exchange Info Structure </em> holds the exchange information to
 *  the connected NFC-IP target .
 *
 *  \note All members of this structure are in parameters [in].
 *
 */

typedef struct phHciNfc_NfcIP_XchgInfo
{
    /** \internal NFC-IP DEP Meta Chining Information */
    uint8_t                     more_info;

}phHciNfc_NfcIP_XchgInfo_t;

/** \ingroup  grp_hal_hci
 *
 * \if hal
 *  \brief HCI Target Exchange Information
 * \else
 *  \brief HCI-Specific
 * \endif
 *
 *  The <em> Target Exchange Info Structure </em> holds all the exchange information to
 *  the connected target .
 *
 *  \note All members of this structure are in parameters [in].
 *
 */


typedef struct phHciNfc_XchgInfo
{
    /** \internal Exchange Data/NFC-IP DEP
     *   Exchange Buffer */
    uint8_t                     *tx_buffer;
    /** \internal Exchange Data/NFC-IP DEP
     *   Exchange Buffer Length*/
    uint16_t                     tx_length;

    union
    {
        phHciNfc_Tag_XchgInfo_t   tag_info;
        phHciNfc_NfcIP_XchgInfo_t nfc_info;
    }params;

}phHciNfc_XchgInfo_t;



/*
################################################################################
*********************** Function Prototype Declaration *************************
################################################################################
*/

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Initialise function initialises the HCI context and all other
 *  resources used in the HCI Layer for the corresponding interface link.
 *
 *  \param[in,out]  psHciHandle         psHciHandle is the handle or the context
 *                                      of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  init_mode               init_mode specifies the kind of the
 *                                      Initialisation for the HCI layer .
 *  \param[in]  pHwConfig               pHwConfig is the Information required
 *                                      to configure the parameters of the
 *                                      NFC Device .
 *  \param[in]  pHalNotify              Upper layer Notification function
 *                                      pointer.
 *  \param[in]  psContext               psContext is the context of
 *                                      the Upper Layer.
 *  \param[in]  psHciLayerCfg           Pointer to the  HCI Layer configuration
 *                                      Structure.
 *
 *  \retval NFCSTATUS_PENDING           Initialisation of HCI Layer is in Progress.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

 extern
 NFCSTATUS
 phHciNfc_Initialise (
                        void                            *psHciHandle,
                        void                            *pHwRef,
                        phHciNfc_Init_t                 init_mode,
                        phHal_sHwConfig_t               *pHwConfig,
                        pphNfcIF_Notification_CB_t       pHalNotify,
                        void                            *psContext,
                        phNfcLayer_sCfg_t               *psHciLayerCfg
                     );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Release function releases all the resources used in the HCI
 *  Layer for the corresponding interface link, described by the HCI handle.
 *
 *  \param[in]  psHciHandle             psHciHandle is the handle or the context
 *                                      of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  pHalReleaseCB           Upper layer release callback function
 *                                      pointer .
 *  \param[in]  psContext               psContext is the context of
 *                                      the Upper Layer.
 *
 *  \retval NFCSTATUS_PENDING           Releasing of HCI Resources are in Progress.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

 extern
 NFCSTATUS
 phHciNfc_Release (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    pphNfcIF_Notification_CB_t      pHalReleaseCB,
                    void                            *psContext
                  );


extern
NFCSTATUS
phHciNfc_Config_Discovery (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    phHal_sADD_Cfg_t                *pPollConfig
                    );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Restart_Discovery function restarts the Polling Wheel.
 *
 *  \param[in]  psHciHandle             psHciHandle is the handle or the context
 *                                      of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  discovery_type                 If True: Start re-polling of the target
 *                                      after the Target Device is de-activated
 *                                      or else - continue discovery with next
 *                                      technology.
 *  \retval NFCSTATUS_PENDING           The Discovery Wheel retarted.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

 extern
 NFCSTATUS
 phHciNfc_Restart_Discovery (
                        void                            *psHciHandle,
                        void                            *pHwRef,
                        uint8_t                         discovery_type
                     );


 /**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Start_Discovery function Starts the Polling Wheel.
 *
 *  \param[in]  psHciHandle             psHciHandle is the handle or the context
 *                                      of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \retval NFCSTATUS_PENDING           The Discovery Wheel Started.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

 extern
 NFCSTATUS
 phHciNfc_Start_Discovery (
                        void                            *psHciHandle,
                        void                            *pHwRef
                     );


 /**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Stop_Discovery function Stops the Polling Wheel.
 *
 *  \param[in]  psHciHandle             psHciHandle is the handle or the context
 *                                      of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \retval NFCSTATUS_PENDING           The Discovery Wheel Stopped.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

 extern
 NFCSTATUS
 phHciNfc_Stop_Discovery (
                        void                            *psHciHandle,
                        void                            *pHwRef
                     );


 /**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Configure function Configures Configures the Polling Wheel to
 *  select the kind of Tags to be polled. This also allows to enable/disable
 *  the Tag Emulation. This also configures the Secure elements the UICC, WI and
 *  Target to Emulate the Tag or Target.
 *
 *
 *  \param[in]  psHciHandle             psHciHandle is the handle or the context
 *                                      of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  config_type             config_type specifies the type of the
 *                                      Parameter configuration.
 *  \param[in]  pConfig                 pConfig is the Information for
 *                                      Configuring the Device.
 *  \retval NFCSTATUS_PENDING           The Emulation configuration pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

 extern
 NFCSTATUS
 phHciNfc_Configure (
                        void                            *psHciHandle,
                        void                            *pHwRef,
                        phHal_eConfigType_t             config_type,
                        phHal_uConfig_t                 *pConfig
                     );

 /**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Switch_SmxMode function Switches the WI(S2C) interface
 *  from Wired/Virtual to vice versa.
 *
 *
 *  \param[in]  psHciHandle             psHciHandle is the handle or the context
 *                                      of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  smx_mode                smx_mode specifies the type of the switch
 *                                      configuration.
 *  \param[in]  pPollConfig             pPollConfig is the Information for
 *                                      polling the SmartMX Device.
 *  \retval NFCSTATUS_PENDING           The SmartMX Mode Switch pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

 NFCSTATUS
 phHciNfc_Switch_SmxMode (
                        void                            *psHciHandle,
                        void                            *pHwRef,
                        phHal_eSmartMX_Mode_t           smx_mode,
                        phHal_sADD_Cfg_t                *pPollConfig
                     );


 /**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Switch_SwpMode function Switches the SWP Link
 *  from On/Off to vice versa. 
 *  
 *
 *  \param[in]  psHciHandle             psHciHandle is the handle or the context
 *                                      of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  swp_mode                swp_mode specifies to switch on/off the
 *                                      SWP Link.
 *  \retval NFCSTATUS_PENDING           The SWP Mode Switch pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

 NFCSTATUS
 phHciNfc_Switch_SwpMode (
                        void                            *psHciHandle,
                        void                            *pHwRef,
                        phHal_eSWP_Mode_t               swp_mode /* ,
                        void                            *pSwpCfg */
                     );



/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Connect function selects the discovered target to
 *  perform the transactions on it.
 *
 *
 *  \param[in]  psHciHandle             psHciHandle is the handle or the context
 *                                      of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  p_remote_dev_info       p_remote_dev_info is the information
 *                                      of the Target Device to be connected .
 *  \retval NFCSTATUS_PENDING           To select the remote target pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

 extern
 NFCSTATUS
 phHciNfc_Connect (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    phHal_sRemoteDevInformation_t   *p_remote_dev_info
                 );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Reactiavte function reactivates the discovered target to
 *  and selects that target perform the transactions on it.
 *
 *
 *  \param[in]  psHciHandle             psHciHandle is the handle or the context
 *                                      of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  p_remote_dev_info       p_remote_dev_info is the information
 *                                      of the Target Device to be reactivated .
 *  \retval NFCSTATUS_PENDING           To reactivate the remote target pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */


 extern
 NFCSTATUS
 phHciNfc_Reactivate (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    phHal_sRemoteDevInformation_t   *p_target_info
                 );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Disconnect function de-selects the selected target and
 *  any ongoing transactions .
 *
 *
 *  \param[in]  psHciHandle             psHciHandle is the handle or the context
 *                                      of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  discovery_type          If NFC_RF_DISCOVERY_REPOLL: Start re-polling of
 *                                      the target after the Target Device is
 *                                      de-activatedor if NFC_RF_DISCOVERY_CONTINUE -
 *                                      continue discovery with next technology or
 *                                      stop the discovery wheel.
 *
 *  \retval NFCSTATUS_PENDING           To De-select the remote target pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

 extern
 NFCSTATUS
 phHciNfc_Disconnect (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    uint8_t                         discovery_type
                 );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Presence_Check function checks for the presence of the target
 *  selected in the vicinity of the Reader's RF Field .
 *
 *
 *  \param[in]  psHciHandle             psHciHandle is the handle or the context
 *                                      of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \retval NFCSTATUS_PENDING           Presence Check of the remote target
 *                                      pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
phHciNfc_Presence_Check (
                    void                            *psHciHandle,
                    void                            *pHwRef
                    );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Select_Next_Target function selects and activates the
 *  next target present in the the Reader's RF Field .
 *
 *
 *  \param[in]  psHciHandle             psHciHandle is the handle or the
 *                                      context of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \retval NFCSTATUS_PENDING           selection and activation of the next
 *                                      remote target pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
phHciNfc_Select_Next_Target (
                    void                            *psHciHandle,
                    void                            *pHwRef
                    );


 /**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Exchange_Data function exchanges the data 
 *  to/from the selected remote target device.
 *
 *
 *  \param[in]  psHciHandle             psHciHandle is the handle or the context
 *                                      of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  p_remote_dev_info       p_remote_dev_info is the information of the
 *                                      selected target to which data 
 *                                      should be sent.
 *  \param[in]  p_xchg_info             The exchange info contains the command type,
 *                                      addr and data to be sent to the connected
 *                                      remote target device.
 *  \retval NFCSTATUS_PENDING           Data to remote target pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

 extern
 NFCSTATUS
 phHciNfc_Exchange_Data (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    phHal_sRemoteDevInformation_t   *p_remote_dev_info,
                    phHciNfc_XchgInfo_t             *p_xchg_info
                );

 /**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Send_Data function Sends the data provided
 *  to the appropriate remote target device.
 *
 *
 *  \param[in]  psHciHandle             psHciHandle is the handle or the context
 *                                      of the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  p_remote_dev_info       p_remote_dev_info is the information 
 *                                      of the selected target to which data 
 *                                      should be sent.
 *  \param[in]  p_send_param            The send param contains the  
 *                                      data to be sent to the
 *                                      remote device.
 *  \retval NFCSTATUS_PENDING           Data to remote device pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

 extern
 NFCSTATUS
 phHciNfc_Send_Data (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    phHal_sRemoteDevInformation_t   *p_remote_dev_info,
                    phHciNfc_XchgInfo_t             *p_send_param
                 );

 /**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_System_Test function performs the System Management Tests 
 * provided by the NFC Peripheral device.
 *
 *  \param[in]  psContext               psContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  test_type               test_type is the type of the Self Test
 *                                      that needs to be performed on the device.
 *  \param[in]  test_param              test_param is the parameter for the Self Test
 *                                      that needs to be performed on the device.
 *
 *
 *  \retval NFCSTATUS_PENDING           System Test on the System Management 
 *                                      is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */

extern
NFCSTATUS
phHciNfc_System_Test(
                    void                            *psContext,
                    void                            *pHwRef,
                    uint32_t                        test_type,
                    phNfc_sData_t                   *test_param
                 );

 /**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_System_Configure function performs the System Management 
 * Configuration with the value provided.
 *
 *  \param[in]  psContext               psContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  config_type             config_type is the type of the configuration
 *                                      that needs to be performed on the device.
 *  \param[in]  config_value            config_value is the value for the configuring
 *                                      that needs to be performed on the device.
 *
 *
 *  \retval NFCSTATUS_PENDING           Configuration of the provided information to
 *                                      the is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */
extern
NFCSTATUS
phHciNfc_System_Configure (
                void                            *psHciHandle,
                void                            *pHwRef,
                uint32_t                        config_type,
                uint8_t                         config_value
                );

 /**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_System_Get_Info function obtains the System Management 
 * information from the address provided.
 *
 *  \param[in]  psContext               psContext is the pointer to HCI Layer
 *                                      context Structure.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  info_type               info_type is the type of the Information
 *                                      that needs to be obtained from the device.
 *  \param[in,out]  p_val               p_val is the pointer to which the 
 *                                      information need to be updated.
 *
 *
 *  \retval NFCSTATUS_PENDING           Get information from the NFC Device
 *                                      is pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Errors related to the other layers
 *
 */
extern
NFCSTATUS
phHciNfc_System_Get_Info(
                void                            *psHciHandle,
                void                            *pHwRef,
                uint32_t                        info_type,
                uint8_t                         *p_val
                );

extern
NFCSTATUS
phHciNfc_PRBS_Test (
                void                            *psHciHandle,
                void                            *pHwRef,
                uint32_t                        test_type,
                phNfc_sData_t                   *test_param
                );

#if 0
 extern
 NFCSTATUS
 phHciNfc_Receive_Data (
                    void                            *psHciHandle,
                    void                            *pHwRef,
                    uint8_t                         *p_data,
                    uint8_t                         length
                 );

#endif


#endif

