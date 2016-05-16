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
 * \file  phNfcHalTypes.h
 * \brief Structure declarations/type definitions belonging to the HAL subsystem.
 *
 * Project: NFC MW / HAL
 *
 * $Date: Thu Apr  8 17:11:39 2010 $
 * $Author: ing04880 $
 * $Revision: 1.106 $
 * $Aliases: NFC_FRI1.1_WK1007_SDK,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
 *
 */


#ifndef PHNFCHALTYPES_H /* */
#define PHNFCHALTYPES_H /* */

/**
 *  \name HAL Types
 *
 * File: \ref phNfcHalTypes.h
 *
 */

/*@{*/
#define PHNFCHALTYPES_FILEREVISION "$Revision: 1.106 $" /**< \ingroup grp_file_attributes */
#define PHNFCHALTYPES_FILEALIASES  "$Aliases: NFC_FRI1.1_WK1007_SDK,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"     /**< \ingroup grp_file_attributes */
/*@}*/

#include <phNfcTypes.h>
#include <phNfcCompId.h>
#include <phNfcConfig.h>

#ifndef NXP_HAL_MEM_INFO_SIZE
#define NXP_HAL_MEM_INFO_SIZE           0x01U
#endif

#if (NXP_HAL_MEM_INFO_SIZE > 0x01)
#define NXP_FW_UPLOAD_PROGRESS          0x965AU
#define NXP_FW_UPLOAD_SUCCESS           0x0000U
#else
#define NXP_FW_UPLOAD_PROGRESS          0x5AU
#define NXP_FW_UPLOAD_SUCCESS           0x00U
#endif


typedef struct phHal_sMemInfo
{
    uint16_t            fw_magic;
    uint16_t            fw_rfu;
    uint32_t            hal_version;
}phHal_sMemInfo_t;


/** \ingroup  grp_hal_common
 *
 * \if hal
 *  \brief Protocol Support Information
 * \else
 *  \brief HAL-Specific
 * \endif
 *
 *  The <em> Supported Protocols Structure </em> holds all protocol supported by the current NFC
 *  device.
 *
 *  \note All members of this structure are output parameters [out].
 *
 */
typedef phNfc_sSupProtocol_t phHal_sSupProtocol_t;


/** \ingroup grp_hal_common
 *
 *
 * \if hal
 *  \brief Information related to the NFC Device
 * \else
 *  \brief HAL-Specific
 * \endif
 *
 *  The <em> Device Information Structure </em> holds information
 *  related to the NFC IC read during initialization time. 
 *  It allows the caller firware, hardware version, the model id,
 *  HCI verison supported and vendor name. Refer to the NFC Device 
 *  User Manual on how to interpret each of the values. In addition
 *  it also contains capabilities of the NFC Device such as the 
 *  protocols supported in Reader and emulation mode
 *
 */

typedef phNfc_sDeviceCapabilities_t phHal_sDeviceCapabilities_t;


/**
 * \ingroup grp_hal_common
 *
 * \if hal
 *  \brief Hardware Reference - The Peripheral Representation
 * \else
 *  \brief HAL-Specific
 * \endif
 *
 *  The Hardware Reference structure is filled as part of the open function and
 *  contains information regarding connected peripheral NFC device. It also
 *  stores the refernce to the communication driver passed by the HAL client
 *  for usage during communication with the NFC Device 
 *
 * \note The caller can consider this structure atomic, no interpretation is required 
 *       for HAL operation.
 * 
 * \sa phHal4Nfc_Open .
 * 
 */

/**
 * \ingroup grp_hal_common
 *
 *  \brief Hardware Reference - The Peripheral Representation
 *
 *  The Hardware Reference structure is filled as part of the open function and
 *  contains information regarding connected peripheral NFC device. It also
 *  stores the refernce to the communication driver passed by the HAL client
 *  for usage during communication with the NFC Device 
 *
 * \note The caller can consider this structure atomic, no interpretation is required 
 *       for HAL operation.
 * 
 */
typedef struct phHal_sHwReference
{
    /**<  Will be usable/valid after the Open function. */
    void                            *p_board_driver;
    /**<  Session Identifier for the established session */
    uint8_t                         session_id[SESSIONID_SIZE];
    /**<  SMX  Connected TRUE/FALSE */
    uint8_t                         smx_connected; 
    /**<  UICC  Connected TRUE/FALSE */
    uint8_t                         uicc_connected;
    /**<  UICC  Reader Mode is Active TRUE/FALSE */
    uint8_t                         uicc_rdr_active;
    /**<  Device information. */
    phNfc_sDeviceCapabilities_t     device_info;
    /**<  Context of the HAL Layer */
    void                            *hal_context;
    /**<  Context of the DAL Layer */
    void                            *dal_context;
} phHal_sHwReference_t;


/** \ingroup grp_hal_common
 *
 * \if hal
 * \brief Hardware configuration - Configuration Parameters for the NFC Device
 * \else
 * \brief HAL-Specific
 * \endif
 *
 *  The parameters used to configure the device during the initialisation.
 *  This structure is used internally by the HAL implementation and is filled
 *  up based on various configuration parameters from the config file
 * \note None.
 *
 */

typedef struct phHal_sHwConfig
{
    
    uint8_t             session_id[SESSIONID_SIZE]; /**<  Session Identifier for 
                                                     the established session */

    uint8_t             clk_req; /**<  Clock Request Setting */

    uint8_t             input_clk; /**<  Input Clock Setting */

} phHal_sHwConfig_t;



/*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

/* This data structure is not used anymore and will be removed in subsequent
   release */
typedef struct phHal_sDepFlags
{
   unsigned int MetaChaining : 1;    
   unsigned int NADPresent   : 1;    
} phHal_sDepFlags_t;

/* This data structure is not used anymore and will be removed in subsequent
   release */

typedef struct phHal_sDepAdditionalInfo
{
    phHal_sDepFlags_t DepFlags;   
    uint8_t NAD;                  
} phHal_sDepAdditionalInfo_t;


/*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/



/** \ingroup grp_hal_common
 *
 * \if hal
 *  \brief Enumerated MIFARE Commands
 * \else
 *  \brief HAL-Specific
 * \endif
 *
 *  The <em> Mifare Command List Enumerator </em> lists all available Mifare native commands.
 *
 * \note None.
 *
 */

typedef phNfc_eMifareCmdList_t phHal_eMifareCmdList_t;

#define    phHal_eMifareRaw        phNfc_eMifareRaw
#define    phHal_eMifareAuthentA   phNfc_eMifareAuthentA
#define    phHal_eMifareAuthentB   phNfc_eMifareAuthentB
#define    phHal_eMifareRead16     phNfc_eMifareRead16
#define    phHal_eMifareRead       phNfc_eMifareRead
#define    phHal_eMifareWrite16    phNfc_eMifareWrite16
#define    phHal_eMifareWrite4     phNfc_eMifareWrite4
#define    phHal_eMifareInc        phNfc_eMifareInc
#define    phHal_eMifareDec        phNfc_eMifareDec
#define    phHal_eMifareTransfer   phNfc_eMifareTransfer
#define    phHal_eMifareRestore    phNfc_eMifareRestore
#define    phHal_eMifareReadSector phNfc_eMifareReadSector
#define    phHal_eMifareWriteSector phNfc_eMifareWriteSector
#define    phHal_eMifareInvalidCmd phNfc_eMifareInvalidCmd


/** \ingroup grp_hal_common
 *
 *  The <em> T=Cl Command List Enumerator </em> lists all available T=Cl Commands.
 *
 * \note None.
 *
 */
typedef phNfc_eIso14443_4_CmdList_t phHal_eIso14443_4_CmdList_t;

#define    phHal_eIso14443_4_Raw    phNfc_eIso14443_4_Raw


/** \ingroup grp_hal_common
 *
 *  The <em> NFCIP1 Command List Enumerator </em> lists all available NFCIP1 Commands.
 *
 * \note None.
 *
 */
typedef phNfc_eNfcIP1CmdList_t phHal_eNfcIP1CmdList_t;

#define       phHal_eNfcIP1_Raw             phNfc_eNfcIP1_Raw


/** \ingroup grp_hal_common
 *
 *  The <em> ISO15693 Command List Enumerator </em> lists all available ISO15693 Commands.
 *
 * \note None.
 *
 */

typedef phNfc_eIso15693_CmdList_t phHal_eIso15693_CmdList_t;

#if 0
#define    phHal_eIso15693_Raw             phNfc_eIso15693_Raw
#endif
#define    phHal_eIso15693_Cmd             phNfc_eIso15693_Cmd
#define    phHal_eIso15693_Invalid         phNfc_eIso15693_Invalid

/** \ingroup grp_hal_common
 *
 *  The <em> Felica Command List Enumerator </em> lists all available Felica Commands.
 *
 * \note None.
 *
 */

typedef enum phHal_eFelicaCmdList
{
    phHal_eFelica_Raw             = 0xF0U, /**< Felica Raw command:\n
                                                 - This command sends the data buffer directly 
                                                 to the remote device */
    phHal_eFelica_Check           = 0x00, /**< Felica Check command:\n
                                                 - This command checks the data from the Felica
                                                  remote device */
    phHal_eFelica_Update          = 0x01, /**< Felica Update command:\n
                                                 - This command updates the data onto the Felica
                                                  remote device */
    phHal_eFelica_Invalid         = 0xFFU      /**< Invalid Command */
} phHal_eFelicaCmdList_t;


typedef enum phHal_eJewelCmdList
{
    phHal_eJewel_Raw            = 0x00U, /**< Jewel command:\n
                                                 - This command sends the data buffer directly 
                                                 to the remote device */
    phHal_eJewel_Invalid        = 0xFFU  /**< Invalid jewel command */
}phHal_eJewelCmdList_t;



/** \ingroup grp_hal_nfci
*
* \if hal
*  \brief Remote Device Reader A RF Gate Information Container 
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> Reader A structure </em> includes the available information
*  related to the discovered ISO14443A remote device. This information 
*  is updated for every device discovery.
*  \note None.
*
*/
typedef phNfc_sIso14443AInfo_t phHal_sIso14443AInfo_t;

/** \ingroup grp_hal_nfci
*
* \if hal
*  \brief Remote Device Reader B RF Gate Information Container 
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> Reader B structure </em> includes the available information
*  related to the discovered ISO14443B remote device. This information 
*  is updated for every device discovery.
*  \note None.
*
*/

typedef phNfc_sIso14443BInfo_t phHal_sIso14443BInfo_t;

typedef phNfc_sIso14443BPrimeInfo_t phHal_sIso14443BPrimeInfo;


/** \ingroup grp_hal_nfci
*
* \if hal
*  \brief Remote Device Jewel Reader RF Gate Information Container 
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> Jewel Reader structure </em> includes the available information
*  related to the discovered Jewel remote device. This information 
*  is updated for every device discovery.
*  \note None.
*
*/


typedef phNfc_sJewelInfo_t phHal_sJewelInfo_t;

/** \ingroup grp_hal_nfci
*
* \if hal
*  \brief Remote Device Felica Reader RF Gate Information Container 
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> Felica Reader structure </em> includes the available information
*  related to the discovered Felica remote device. This information 
*  is updated for every device discovery.
*  \note None.
*
*/

typedef phNfc_sFelicaInfo_t phHal_sFelicaInfo_t;


/** \ingroup grp_hal_nfci
*
* \if hal
*  \brief Remote Device Reader 15693 RF Gate Information Container 
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> Reader A structure </em> includes the available information
*  related to the discovered ISO15693 remote device. This information 
*  is updated for every device discovery.
*  \note None.
*
*/

typedef phNfc_sIso15693Info_t phHal_sIso15693Info_t;


/** \ingroup grp_hal_nfci
*
* \if hal
*  \brief NFC Data Rate Supported between the Reader and the Target
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> \ref phHalNfc_eDataRate enum </em> lists all the Data Rate 
*  values to be used to determine the rate at which the data is transmitted
*  to the target.
*
*  \note None.
*/


typedef phNfc_eDataRate_t phHalNfc_eDataRate_t;


/** \ingroup grp_hal_nfci
*
* \if hal
*  \brief NFCIP1 Gate Information Container 
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> NFCIP1 structure </em> includes the available information
*  related to the discovered NFCIP1 remote device. This information 
*  is updated for every device discovery.
*  \note None.
*
*/

typedef phNfc_sNfcIPInfo_t phHal_sNfcIPInfo_t;


/** \ingroup grp_hal_nfci
*
* \if hal
*  \brief Remote Device Specific Information Container
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> Remote Device Information Union </em> includes the available Remote Device Information
*  structures. Following the device detected, the corresponding data structure is used.
*
*  \note None.
*
*/

typedef phNfc_uRemoteDevInfo_t phHal_uRemoteDevInfo_t;

/** \ingroup grp_hal_nfci
*
* \if hal
*  \brief RF Device Type Listing
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> RF Device Type List </em> is used to identify the type of
*  remote device that is discovered/connected. There seperate
*  types to identify a Remote Reader (denoted by _PCD) and 
*  Remote Tag (denoted by _PICC)
*  \note None.
*
*/

typedef phNfc_eRFDevType_t phHal_eRFDevType_t;

#define    phHal_eUnknown_DevType phNfc_eUnknown_DevType

    /* Specific PCD Devices */
#define    phHal_eISO14443_A_PCD phNfc_eISO14443_A_PCD
#define    phHal_eISO14443_B_PCD phNfc_eISO14443_B_PCD
#define    phHal_eISO14443_BPrime_PCD phNfc_eISO14443_BPrime_PCD
#define    phHal_eFelica_PCD phNfc_eFelica_PCD
#define    phHal_eJewel_PCD phNfc_eJewel_PCD
#define    phHal_eISO15693_PCD phNfc_eISO15693_PCD
    /* Generic PCD Type */
#define    phHal_ePCD_DevType phNfc_ePCD_DevType

    /* Generic PICC Type */
#define    phHal_ePICC_DevType phNfc_ePICC_DevType
    /* Specific PICC Devices */
#define    phHal_eISO14443_A_PICC phNfc_eISO14443_A_PICC
#define    phHal_eISO14443_4A_PICC phNfc_eISO14443_4A_PICC
#define    phHal_eISO14443_3A_PICC phNfc_eISO14443_3A_PICC
#define    phHal_eMifare_PICC phNfc_eMifare_PICC
#define    phHal_eISO14443_B_PICC phNfc_eISO14443_B_PICC
#define    phHal_eISO14443_4B_PICC phNfc_eISO14443_4B_PICC
#define    phHal_eISO14443_BPrime_PICC phNfc_eISO14443_BPrime_PICC
#define    phHal_eFelica_PICC phNfc_eFelica_PICC
#define    phHal_eJewel_PICC phNfc_eJewel_PICC
#define    phHal_eISO15693_PICC phNfc_eISO15693_PICC

    /* NFC-IP1 Device Types */
#define    phHal_eNfcIP1_Target phNfc_eNfcIP1_Target
#define    phHal_eNfcIP1_Initiator phNfc_eNfcIP1_Initiator

    /* Other Sources */
#define    phHal_eInvalid_DevType phNfc_eInvalid_DevType

/** \ingroup grp_hal_nfci
*
* \if hal
*  \brief Remote Device Type Listing
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> Remote Device Type List </em> is used to identify the type of
*  remote device that is discovered/connected
*  \note This is same as RF Device Type List.
*
*/
typedef phNfc_eRemDevType_t phHal_eRemDevType_t;

/** \ingroup grp_hal_common
 *
 *
 * \if hal
 *  \brief Common Command Attribute
 * \else
 *  \brief HAL-Specific
 * \endif
 *
 *  The <em> Hal Command Union </em> includes each available type of Commands.
 *
 * \note None.
 *
 */

typedef phNfc_uCmdList_t phHal_uCmdList_t;


/** \ingroup grp_hal_nfci
 *
 * \if hal
 *  \brief Remote Device Information Structure
 * \else
 *  \brief HAL-Specific
 * \endif
 *
 *  The <em> Remote Device Information Structure </em> holds information about one single Remote
 *  Device detected by the polling function .\n
 *  It lists parameters common to all supported remote devices.
 *
 *  \note 
 *
 *  \if hal
 *   \sa \ref phHal4Nfc_ConfigureDiscovery and \ref phHal4Nfc_Connect
 *  \else
 *   \sa 
 *  \endif
 *
 */

typedef phNfc_sRemoteDevInformation_t phHal_sRemoteDevInformation_t;



/*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

/* This data structure is not used anymore and will be removed in subsequent
   release */

typedef struct phHal_sDevInputParam
{
    uint8_t FelicaPollPayload[5];          
                                           
                                           
    uint8_t NfcPollPayload[5];             
                                           
                                           
    uint8_t NFCIDAuto;                     
                                           
                                           
    uint8_t NFCID3i[PHHAL_NFCID_LENGTH];
                                           
                                           
                                           
    uint8_t DIDiUsed;                      
                                           
    uint8_t CIDiUsed;                      
                                           
    uint8_t NfcNADiUsed;                   
                                           
    /*+ MantisId : 31 - JP - 09-01-2006 */
        /*uint8_t TClNADiUsed; */          
                                           
    /*- MantisId : 31 - JP - 09-01-2006 */
    uint8_t GeneralByte[48];               
                                           
                                           
    uint8_t GeneralByteLength;             
                                           
    
    uint8_t ISO14443_4B_AFI;               
                                           
} phHal_sDevInputParam_t;




/*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/


/*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
/*    TARGET STRUCTURES */


/** \ingroup  grp_hal_common
 *
 * \if hal
 *  \brief Transceive Information Data Structure for sending commands/response 
 *         to the remote device
 * \else
 *  \brief HAL-Specific
 * \endif
 *
 *  The <em> Transceive Information Data Structure </em> is used to pass the 
 *  Command, Address (only required for MIFARE) and the send and receive data
 *  data structure (buffer and length) for communication with remote device
 *
 *
 */

typedef phNfc_sTransceiveInfo_t phHal_sTransceiveInfo_t;


/** \ingroup grp_hal_nfci
*
* \if hal
*  \brief Input information for the Type A tags
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> \ref phHal_sIso14443ACfg structure </em> holds the information 
*  required for the NFC device to be used during ISO14443A target discovery
*
*  \note None.
*/
typedef struct phHal_sIso14443ACfg
{
    uint8_t     Auto_Activation;       /**< Enable Auto Activation for 
                                    Technology A \n
                                    If set to 0, the activation procedure will stop 
                                    after Select (SAK has been received). 
                                    The host could evaluate SAK value and then decide:
                                        - to start communicating with the remote card 
                                          using proprietary commands (see NXP_MIFARE_RAW 
                                          and NXP_MIFARE_CMD)
                                    or
                                        - to activate the remote card up to ISO14443-4 
                                          level (RATS and PPS) using 
                                          CONTINUE ACTIVATION command 
                                    If set to 1, activation follows the flow described in
                                    ETSI HCI specification (restrict detection to 
                                    ISO14443-4 compliant cards).
                                    */
}phHal_sIso14443ACfg_t;


/** \ingroup grp_hal_nfci
*
* \if hal
*  \brief Input information for the Type B tags
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> \ref phHal_sIso14443BCfg structure </em> holds the information 
*  required for the NFC device to be used during ISO14443B target discovery
*
*  \note None.
*/
typedef struct phHal_sIso14443BCfg
{
    uint8_t     AppFamily_ID;       /**< Application Family Identifier for 
                                    Technology B, 0x00 means all application */
}phHal_sIso14443BCfg_t;

/** \ingroup grp_hal_nfci
*
* \if hal
*  \brief Input information for the Felica tags
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> \ref phHal_sFelicaCfg_t structure </em> holds the information 
*  required for the NFC device to be used during Felica target discovery
*
*  \note None.
*/

typedef struct phHal_sFelicaCfg
{
    uint8_t     SystemCode[PHHAL_FEL_SYS_CODE_LEN];     /**< System code for Felica tags */

}phHal_sFelicaCfg_t;


/** \ingroup grp_hal_nfci
*
* \if hal
*  \brief Poll Device Information for conifiguring the discovery wheel
          Reader and Card Emulation Phases
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> \ref phHal_sPollDevInfo_t enum </em> is used to enable/disable 
*  phases of the discovery wheel related to specific reader types and 
*  card emulation phase
*  \note Enabling specific Reader technology when NFCIP1 speed is set in the
*        phNfc_sADD_Cfg_t is implicitly done in HAL. Use this structure to only
*        enable/disable Card Reader Functionality
*/
typedef phNfc_sPollDevInfo_t phHal_sPollDevInfo_t;


/** \ingroup grp_hal_common
*
* \if hal
*  \brief Identifies Type of Host
* \else
*  \brief HAL-Specific
* \endif
*
*  This enumeration is used to identify the type of the host providing the
*  information or the notification to the Terminal host.
*  \note None.
*/

typedef enum phHal_HostType {
    /* 
     * This type identifies the host controller
     * in the NFC device
     */
    phHal_eHostController       = 0x00U,
    /* 
     * This type identifies the Host Device
     * controlling the NFC device.
     */
    phHal_eTerminalHost         = 0x01U,
    /* 
     * This type identifies the uicc host
     * connnected to the NFC device
     */
    phHal_eUICCHost             = 0x02U,
    /* Host type is unknown */
    phHal_eUnknownHost          = 0xFFU
}phHal_HostType_t;


/** \ingroup grp_hal_nfci
*
* \if hal
*  \brief P2P speed for the Initiator
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> \ref phHal_eP2PMode enum </em> lists all the NFCIP1 speeds 
*  to be used for configuring the NFCIP1 discovery
*
*  \note None.
*/

#define    phHal_eDefaultP2PMode  phNfc_eDefaultP2PMode
#define    phHal_ePassive106 phNfc_ePassive106
#define    phHal_ePassive212 phNfc_ePassive212
#define    phHal_ePassive424 phNfc_ePassive424
#define    phHal_eActive  phNfc_eActive
#define    phHal_eP2P_ALL    phNfc_eP2P_ALL
#define    phHal_eInvalidP2PMode phNfc_eInvalidP2PMode


/** \ingroup grp_hal_common
*
* \if hal
*  \brief Identities the type of Notification
* \else
*  \brief HAL-Specific
* \endif
*
*  This enumeration is used to specify the type of notification notified
*  to the upper layer. This classifies the notification into two types
*  one for the discovery notifications and the other for all the remaining
*  event notifications
*  \note None.
*/


typedef phNfc_eNotificationType_t phHal_eNotificationType_t;


/** \ingroup grp_hal_common
*
* \if hal
*  \brief Identifies the type of event notification
* \else
*  \brief HAL-Specific
* \endif
*
*  This enumeration is used to identify the type of the event notified
*  to the Terminal host.
*  \note None.
*/

typedef enum phHal_Event {


    /* Transaction Events */
    NFC_EVT_END_OF_TRANSACTION   = 0x11U ,
    NFC_EVT_TRANSACTION          = 0x12U ,
    NFC_EVT_START_OF_TRANSACTION = 0x20U ,

    /* Field Events */
    NFC_EVT_FIELD_ON             = 0x31U,
    NFC_EVT_FIELD_OFF            = 0x34U,

    /* Card/Target Activation Events */
    NFC_EVT_ACTIVATED           = 0x33U,
    NFC_EVT_DEACTIVATED         = 0x32U,

    NFC_EVT_PROTECTED           = 0x24U ,

    /* Reader Phases configuration request by UICC */
    NFC_UICC_RDPHASES_ACTIVATE_REQ = 0x43U,
    NFC_UICC_RDPHASES_DEACTIVATE_REQ = 0x44U,

    /* Connectivity and Triggering Events - Future Use */
    NFC_EVT_CONNECTIVITY         = 0x10U ,
    NFC_EVT_OPERATION_ENDED      = 0x13U ,

    /* NXP Specific System Information Events */
    NFC_INFO_TXLDO_OVERCUR       = 0x71U,
    NFC_INFO_MEM_VIOLATION       = 0x73U,
    NFC_INFO_TEMP_OVERHEAT       = 0x74U,
    NFC_INFO_LLC_ERROR           = 0x75U,

    /* NXP EVENTS */
    NFC_EVT_MIFARE_ACCESS          = 0x35,
    NFC_EVT_APDU_RECEIVED          = 0x36,
    NFC_EVT_EMV_CARD_REMOVAL       = 0x37

}phHal_Event_t;

typedef phNfc_sUiccInfo_t phHal_sUiccInfo_t;

/** \ingroup grp_hal_common
*
* \if hal
*  \brief Event notification Information
* \else
*  \brief HAL-Specific
* \endif
*
*  This structure provides the information about the event notified
*  to the terminal host.
*  \note None.
*/

typedef struct phHal_sEventInfo 
{
    /* Type of the host issuing the event */
    phHal_HostType_t    eventHost;
    /* Type of the source issuing the event */
    phHal_eRFDevType_t  eventSource;
    /* Type of the source issuing the event */
    phHal_Event_t       eventType;
    union   uEventInfo
    {
        /* Parameter information Information is obtained if the eventType is
         * NFC_EVT_TRANSACTION for UICC.
         */
        phHal_sUiccInfo_t       uicc_info;
        /* AID Information is obtained if the eventType is
         * NFC_EVT_TRANSACTION.
         */
        phNfc_sData_t           aid;
        /* Overheat Status Information is obtained if the eventType is
         * NFC_INFO_TEMP_OVERHEAT.
         */
        uint8_t                 overheat_status;
        /* rd_phases Information is obtained if the eventType is
         * NFC_UICC_RDPHASES_ACTIVATE_REQ.
         */
        uint8_t                 rd_phases;
        /* Remote Device Information is obtained if the eventType is
         * NFC_EVT_ACTIVATED.
         */
        phHal_sRemoteDevInformation_t *pRemoteDevInfo;
    }eventInfo;
}phHal_sEventInfo_t;


/** \ingroup grp_hal_common
*
* \if hal
*  \brief Information for the Host/Uicc Emulation Support
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> \ref phHal_sEmuSupport structure </em> holds the type 
*   of the target emulation supported.
*
*  \note None.
*/

typedef struct phHal_sEmuSupport
{
    unsigned int TypeA:1;
    unsigned int TypeB:1;
    unsigned int TypeBPrime:1;
    unsigned int TypeFelica:1;
    unsigned int TypeMifare:1;
    unsigned int TypeNfcIP1:1;
    unsigned int RFU:2;

}phHal_sEmuSupport_t;


/** \ingroup grp_hal_nfci
*
* \if hal
*  \brief P2P Information for the Initiator
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> \ref phHal_sNfcIPCfg </em> holds the P2P related information
*  use by the NFC Device during P2P Discovery and connection
*
*  \note None.
*/

typedef phNfc_sNfcIPCfg_t phHal_sNfcIPCfg_t;

/** \ingroup grp_hal_common
*
* \if hal
*  \brief Enumeration used to choose which type of parameters 
*         are to be configured
* \else
*  \brief HAL-Specific
* \endif
*
*  
*  \note None.
*/
typedef enum phHal_eConfigType
{
    NFC_INVALID_CONFIG  =   0x00U, /**< Invalid Configuration */
    NFC_RF_READER_CONFIG, /**< Reader Parmaeters */
    NFC_P2P_CONFIG,       /**< NFCIP1 Parameters */
    NFC_SE_PROTECTION_CONFIG, /**< Secure Element 
                                   Protection Cofiguration */
    NFC_EMULATION_CONFIG  /**< Emulation Parameters */
}phHal_eConfigType_t;

/** \ingroup grp_hal_common
*
* \if hal
*  \brief Discovery Configuration Mode
* \else
*  \brief HAL-Specific
* \endif
*
*  This enumeration is used to choose the Discovery Configuration
*  Mode :- Configure and Start, Stop or Start with last set 
*  configuration
*  \note None.
*/

typedef phNfc_eDiscoveryConfigMode_t phHal_eDiscoveryConfigMode_t;


/** \ingroup grp_hal_common
*
* \if hal
*  \brief Target or Tag Release Mode
* \else
*  \brief HAL-Specific
* \endif
*
*  This enumeration defines various modes of releasing an acquired target 
*  or tag.
*  \note None.
*/
typedef phNfc_eReleaseType_t phHal_eReleaseType_t;


/** \ingroup grp_hal_common
*
* \if hal
*  \brief Configuration of specific Emulation Feature
* \else
*  \brief HAL-Specific
* \endif
*
*  This enumeration is used to choose configuration for a specific
*  emulation feature.
*  \note None.
*/

typedef enum phHal_eEmulationType
{
    NFC_UNKNOWN_EMULATION       = 0x00U, /**< Invalid Configuration */
    NFC_HOST_CE_A_EMULATION     = 0x01U, /**< Configure parameters for Type A 
                                              card emulation from host */
    NFC_HOST_CE_B_EMULATION     = 0x02U, /**< Configure parameters for Type B 
                                              card emulation from host */
    NFC_B_PRIME_EMULATION       = 0x03U, /**< Configure parameters for Type B' 
                                              card emulation from host */
    NFC_FELICA_EMULATION        = 0x04U, /**< Configure parameters for Type F 
                                              card emulation from host */
    NFC_MIFARE_EMULATION        = 0x06U, /**< Configure parameters for MIFARE 
                                              card emulation - For Future Use */
    NFC_SMARTMX_EMULATION       = 0x07U, /**< Configure parameters for SmartMX 
                                            */
    NFC_UICC_EMULATION          = 0x08U  /**< Configure parameters for UICC 
                                            emulation */
}phHal_eEmulationType_t;

#if 0
/** \ingroup grp_hal_nfct
 *
 * \if hal
 *  \brief Information for Target Mode Start-Up
 * \else
 *  \brief HAL-Specific
 * \endif
 *
 *  The <em> Target Information Structure </em> required to start Target mode. 
 *  It contains all the information for the Target mode.
 *
 *  \note None.
 *
 */

typedef struct phHal_sTargetInfo
{
    uint8_t                 enableEmulation;
    phHal_sNfcIPCfg_t       targetConfig;
} phHal_sTargetInfo_t;
#endif


/** \ingroup grp_hal_common
*
* \if hal
*  \brief Mode of operation for SmartMX
* \else
*  \brief HAL-Specific
* \endif
*
*  This enumeration is used to choose the mode of operation for the SmartMx Module.
*  Default static configuration at initialization time.
*  \note None.
*/

typedef enum phHal_eSmartMX_Mode{
    eSmartMx_Wired      = 0x00U, /* SmartMX is in Wired Mode */
    eSmartMx_Default,            /* SmartMX is in Default Configuration Mode */
    eSmartMx_Virtual,            /* SmartMx in the Virutal Mode */
    eSmartMx_Off                 /* SmartMx Feature is Switched off */
} phHal_eSmartMX_Mode_t;


/** \ingroup grp_hal_common
*
* \if hal
*  \brief Mode of operation for SWP
* \else
*  \brief HAL-Specific
* \endif
*
*  This enumeration is used to choose the mode of operation for the SWP Link 
*  for UICC Module. Default static configuration at initialization time.
*  \note None.
*/

typedef enum phHal_eSWP_Mode{
    eSWP_Switch_Off      = 0x00U,   /* SWP Link is Switched off */
    eSWP_Switch_Default,            /* SWP is in Default Configuration Mode */
    eSWP_Switch_On                  /* SWP Link is Switched on */
} phHal_eSWP_Mode_t;


/** \ingroup grp_hal_common
*
* \if hal
*  \brief Information for the Configuring the SmartMX 
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> \ref phHal_sSmartMX_Cfg structure </em> holds the information 
*   to configure the SmartMX Module in the NFC Device.
*
*  \note None.
*/


typedef struct phHal_sSmartMX_Cfg
{
    uint8_t                 enableEmulation;
    uint8_t                 lowPowerMode;
    phHal_eSmartMX_Mode_t   smxMode;
}phHal_sSmartMX_Cfg_t;


/** \ingroup grp_hal_common
*
* \if hal
*  \brief Information for the Configuring the UICC 
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> \ref phHal_sUiccEmuCfg structure </em> holds the information 
*   to configure the UICC Host.
*
*  \note None.
*/


typedef struct phHal_sUiccEmuCfg
{
    uint8_t             enableUicc;
    uint8_t             uiccEmuSupport;
    uint8_t             uiccReaderSupport;
    uint8_t             lowPowerMode;
    /* TODO: This will be updated later */
}phHal_sUiccEmuCfg_t;

/** \ingroup grp_hal_common
*
* \if hal
*  \brief Information for the Configuring the Type A Host Emulation Feature
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> \ref phHal_sHostEmuCfg_A structure </em> holds the information 
*   to configure the Host Emulation for Type A.
*
*  \note None.
*/

typedef struct phHal_sHostEmuCfg_A
{
    uint8_t                 enableEmulation;
    phNfc_sIso14443AInfo_t  hostEmuCfgInfo;
    uint8_t                 enableCID;
}phHal_sHostEmuCfg_A_t;


/** \ingroup grp_hal_common
*
* \if hal
*  \brief Information for the Configuring the Type B Host Emulation Feature
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> \ref phHal_sHostEmuCfg_B structure </em> holds the information 
*   to configure the Host Emulation for Type B.
*
*  \note None.
*/

typedef struct phHal_sHostEmuCfg_B
{
    uint8_t                 enableEmulation;
    phNfc_sIso14443BInfo_t  hostEmuCfgInfo;
}phHal_sHostEmuCfg_B_t;

/** \ingroup grp_hal_common
*
* \if hal
*  \brief Information for the Configuring the Felica Host Emulation Feature
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> \ref phHal_sHostEmuCfg_F structure </em> holds the information 
*   to configure the Felica Host Emulation.
*
*  \note None.
*/


typedef struct phHal_sHostEmuCfg_F
{
    uint8_t                 enableEmulation;
}phHal_sHostEmuCfg_F_t;


/** \ingroup grp_hal_common
*
* \if hal
*  \brief Information for the Configuring the Emulation
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> \ref phHal_sEmulationCfg structure </em> holds the information 
*   required for the device to act as a Tag or NFCIP1 Target.
*
*  \note phHal_sHostEmuCfg_F_t Type F emulation is not presently supported
*  is reserved for future use. 
*/

typedef struct phHal_sEmulationCfg
{
    phHal_HostType_t        hostType;
    phHal_eEmulationType_t  emuType;
    union phHal_uEmuConfig
    {
        phHal_sSmartMX_Cfg_t    smartMxCfg;
        phHal_sHostEmuCfg_A_t   hostEmuCfg_A;
        phHal_sHostEmuCfg_B_t   hostEmuCfg_B;
        phHal_sHostEmuCfg_F_t   hostEmuCfg_F;
        phHal_sUiccEmuCfg_t     uiccEmuCfg;
    }config;
}phHal_sEmulationCfg_t;

/** \ingroup grp_hal_common
*
* \if hal
*  \brief Information for the Configuring the Reader parameters
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> \ref phHal_sReaderCfg structure </em> holds the information 
*   to configure the Reader A or Reader B parameters.
*
*  \note None.
*/

typedef struct phHal_sReaderCfg
{
    phHal_eRFDevType_t    readerType;
    union phHal_uReaderCfg
    {
        phHal_sIso14443ACfg_t       Iso14443ACfg;
        phHal_sIso14443BCfg_t       Iso14443BCfg;
    }config;
}phHal_sReaderCfg_t;


/** \ingroup grp_hal_common
*
* \if hal
*  \brief Information for Configuring the Protected Mode for 
*  the Secure Elements.
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> \ref phHal_sSEProtectionCfg structure </em> holds the 
*  information to configure the Secure Element Protection configuration.
*
*  \note None.
*/

typedef struct phHal_sSEProtectionCfg
{
    uint8_t         mode;
}phHal_sSEProtectionCfg_t;


/** \ingroup  grp_hal_common
*
* \if hal
*  \brief Poll configuration structure
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> Poll configuration structure </em> holds information about the 
*  enabling the the type of discovery required by the application. This 
*  structure is the input parameter for the discovery call
*
*  \note All members of this structure are input parameters [out].
*
*  \if hal
*   \sa \ref phHal4Nfc_Connect, \ref phHal4Nfc_ConfigParameters, 
*            \ref phHal_eP2PMode_t and \ref phHal4Nfc_Disconnect.
*  \endif
*
*/

typedef phNfc_sADD_Cfg_t phHal_sADD_Cfg_t;


/** \ingroup grp_hal_common
*
* \if hal
*  \brief Configuration information.
* \else
*  \brief HAL-Specific
* \endif
*
*  The <em> \ref phHal_uConfig structure </em> holds the information 
*   required for Configuring the Device.
*
*  \note None.
*/


typedef union phHal_uConfig
{
    phHal_sEmulationCfg_t emuConfig;  
    phHal_sNfcIPCfg_t     nfcIPConfig;    /**< Gives the information about
                                           *  the General Bytes for NFC-IP 
                                           *  Communication.
                                           */
    phHal_sReaderCfg_t       readerConfig;
    phHal_sSEProtectionCfg_t protectionConfig;
}phHal_uConfig_t;


#endif

/* EOF */
