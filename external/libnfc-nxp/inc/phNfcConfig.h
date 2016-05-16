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
* \file phNfcConfig.h
* \brief HAL Configurations
*
*
* \note This is the configuration header file of the HAL 4.0.All configurable parameters of the HAL 4.0
*       are provided in this file
*
* Project: NFC-FRI-1.1 / HAL4.0
*
* $Date: Thu Sep  9 14:56:35 2010 $
* $Author: ing04880 $
* $Revision: 1.39 $
* $Aliases:  $
*
*/


/*@{*/
#ifndef PHNFC_CONFIG_H
#define PHNFC_CONFIG_H
/*@}*/


/**
*  \name Hal
*
* File: \ref phNfcConfig.h
*
*/

/*@{*/
#define PH_NFC_CONFIG_FILEREVISION "$Revision: 1.39 $" /**< \ingroup grp_file_attributes */
#define PH_NFC_CONFIG_FILEALIASES  "$Aliases:  $"     /**< \ingroup grp_file_attributes */
/*@}*/


/* -----------------Include files ---------------------------------------*/

#ifdef NFC_CUSTOM_CONFIG_INCLUDE
#include<nfc_custom_config.h>
#endif

/* ---------------- Macros ----------------------------------------------*/



/*
 *****************************************************************
 **********************  DEFAULT MACROS **************************
 *****************************************************************
 */


/**<  External Clock Request Configuration for the NFC Device,
      0x00U     No Clock Request,
      0x01U     Clock Request through CLKREQ pin (GPIO pin 2),
      0x02U     Clock Request through NXP_EVT_CLK_REQUEST Event,
      */
#ifndef NXP_DEFAULT_CLK_REQUEST
#define NXP_DEFAULT_CLK_REQUEST         0x00U
#endif

/**<  External Input Clock Setting for the NFC Device,
      0x00U     No Input Clock Required (Use the Xtal),
      0x01U     13 MHZ,
      0x02U     19.2 MHZ,
      0x03U     26 MHZ,
      0x04U     38.4 MHZ,
      0x05U     Custom (Set the Custome Clock Registry),
      */

#ifndef NXP_DEFAULT_INPUT_CLK
#define NXP_DEFAULT_INPUT_CLK           0x00U
#endif

/**<  UICC Power Request configuration for the NFC Device,
      0x00U     No Power Request,
      0x01U     Power Request through CLKREQ pin (GPIO pin 2),
      0x02U     Power Request through PWR_REQUEST (GPIO Pin 3),
      */

#ifndef NXP_UICC_PWR_REQUEST
#define NXP_UICC_PWR_REQUEST            0x00U
#endif

/**<  TX LDO Configuration
       0x00     00b     3.0 V,
       0x01     01b     3.0 V,
       0x02     10b     2.7 V,
       0x03     11b     3.3 V,
      */

#ifndef NXP_DEFAULT_TX_LDO
#define NXP_DEFAULT_TX_LDO              0x00U
#endif

/**<  UICC Bit Rate Configuration
       0x02     212Kbits/Sec
       0x04     424Kbits/Sec
       0x08     828Kbits/Sec
 */

#ifndef NXP_UICC_BIT_RATE
#define NXP_UICC_BIT_RATE               0x08U
#endif


/**<  Indicates PN544 Power Modes Configuration for the NFC Device,
      0x00U -> PN544 stays in active bat mode 
               (except when generating RF field)
      0x01U -> PN544 goes in standby when possible otherwise 
               stays in active bat mode
      0x02U -> PN544 goes in idle mode as soon as it can 
               (otherwise it is in active bat except when generating RF field)
      0x03U -> PN544 goes in standby when possible otherwise goes in idle mode 
               as soon as it can (otherwise it is in active bat except when 
               generating RF field)
      */

#ifndef NXP_SYSTEM_PWR_STATUS
#define NXP_SYSTEM_PWR_STATUS           0x01U
#endif

/**< Default Session ID for Initialisation */
#ifndef DEFAULT_SESSION
#define DEFAULT_SESSION           "android8"
#endif


/* The Other Integration Configuration Values */

/**< Max number of remote devices supported */

#ifndef MAX_REMOTE_DEVICES
#define MAX_REMOTE_DEVICES        0x0A 
#endif

/**<  System Event Notification
       0x01     Overcurrent
       0x02     PMUVCC Switch
       0x04     External RF Field
       0x08     Memory Violation
       0x10     Temperature Overheat
 */

#ifndef NXP_SYSTEM_EVT_INFO
#define NXP_SYSTEM_EVT_INFO             0x3DU
#endif


#ifndef NFC_DEV_HWCONF_DEFAULT
#define NFC_DEV_HWCONF_DEFAULT          0xBCU
#endif


#ifndef NXP_ISO_XCHG_TIMEOUT
#define NXP_ISO_XCHG_TIMEOUT            0x1BU
#endif

#ifndef NXP_MIFARE_XCHG_TIMEOUT
#define NXP_MIFARE_XCHG_TIMEOUT         0x0BU
#endif

#ifndef NXP_FELICA_XCHG_TIMEOUT
#define NXP_FELICA_XCHG_TIMEOUT         0xFFU
#endif


#ifndef NXP_NFCIP_PSL_BRS_DEFAULT
#define NXP_NFCIP_PSL_BRS_DEFAULT       0x00U
#endif



/**< ID For Invalid Timer */
#ifndef NXP_INVALID_TIMER_ID
#define NXP_INVALID_TIMER_ID              0xFFFFFFFFU
#endif

/**< Presence check interval in milliseconds */
#ifndef PRESENCE_CHECK_INTERVAL
#define PRESENCE_CHECK_INTERVAL   500U
#endif 

/** Resolution value for the timer, here the 
    timer resolution is 500 milliseconds */
#ifndef TIMER_RESOLUTION
#define TIMER_RESOLUTION                500U
#endif 

/* Kindly note that the below Timeout values should be
 * in Multiples of the value provided to TIMER_RESOLUTION
 */

/**< Defines guard time out value for LLC timer, 
    1000 is in milliseconds */
#ifndef LINK_GUARD_TIMEOUT
#define LINK_GUARD_TIMEOUT              1000U
#endif 


/**< Defines connection time out value for LLC timer, 
    1000 is in milliseconds */
#ifndef LINK_CONNECTION_TIMEOUT
#define LINK_CONNECTION_TIMEOUT         1000U
#endif 

/**< Defines ACK time out value for LLC timer,
    150 is in milliseconds */
#ifndef LINK_ACK_TIMEOUT
#define LINK_ACK_TIMEOUT                1U
#endif


/**< Defines Firmware Download Completion Timeout value ,
    120000 is in milliseconds */


#ifndef NXP_DNLD_COMPLETE_TIMEOUT
#define NXP_DNLD_COMPLETE_TIMEOUT         60000U
#endif


/**< Define to configure the Active Mode Polling Guard Time-out 
  */

#ifndef DEV_MGMT_ACT_GRD_TO_DEFAULT
#define DEV_MGMT_ACT_GRD_TO_DEFAULT       0x20U
#endif

/**<  NFCIP Active Mode Default Configuration (when acting as Target)
       0x01     106 kbps
       0x02     212 kbps
       0x04     424 kbps
 */

#ifndef NXP_NFCIP_ACTIVE_DEFAULT
#define NXP_NFCIP_ACTIVE_DEFAULT        0x01U
#endif




#ifndef NXP_NFC_HCI_TIMER
#define NXP_NFC_HCI_TIMER       1
#define NXP_NFC_HCI_TIMEOUT     6000
#endif


/*
 *****************************************************************
  DO NOT MODIFY THE BELOW MACROS UNLESS OTHERWISE MENTIONED
 *****************************************************************
 */



#ifndef HOST_CE_A_SAK_DEFAULT
#define HOST_CE_A_SAK_DEFAULT           0x20U
#endif 

#ifndef NXP_CE_A_ATQA_HIGH
#define NXP_CE_A_ATQA_HIGH              0x00U
#endif

#ifndef NXP_CE_A_ATQA_LOW
#define NXP_CE_A_ATQA_LOW               0x04U
#endif


#ifndef NXP_UICC_CE_RIGHTS
#define NXP_UICC_CE_RIGHTS              0x0FU
#endif 

#ifndef NXP_UICC_RD_RIGHTS
#define NXP_UICC_RD_RIGHTS              0x00U
#endif 


/*
 *****************************************************************
  DO NOT DISABLE/ENABLE BELOW MACROS UNLESS OTHERWISE MENTIONED
 *****************************************************************
 */

#define ES_HW_VER   32
 
/*
 *****************************************************************
 *************** FEATURE SPECIFIC MACROS *************************
 *****************************************************************
 */



/**< Macro to Enable SMX Feature During
 * Initialisation */

#if !defined(NXP_SMX)
#define NXP_SMX 1
#endif

#if (NXP_SMX == 1)
#define NXP_HAL_ENABLE_SMX 
#endif

/**< Macro to Enable the Host Session
 * Initialisation */
#define ESTABLISH_SESSION

/**< Macro to Enable the Peer to Peer Feature */
#define ENABLE_P2P

#define DEFAULT_NFCIP_INITIATOR_MODE_SUPPORT   0x3FU
#define DEFAULT_NFCIP_TARGET_MODE_SUPPORT      0x0FU

/**< Macro to Enable the ISO14443-B Feature */
#define TYPE_B

/**< Macro to Enable the Felica Feature */
#define TYPE_FELICA

/**< Macro to Enable the JEWEL Feature */
#define TYPE_JEWEL

/**< Macro to Enable the ISO15693 Feature */
#define TYPE_ISO15693

/*< Macro to Verify the Poll Parameters Set */
/* #define ENABLE_VERIFY_PARAM */

/**< Macro to Enable ISO 18092 Protocol compliancy
 *  SAK to be merged with the TYPE A Card RF Feature :3.1*/
#define TGT_MERGE_SAK


/**< Macro to Configure the default power status
 * to allow the PN544 to enter into the Standby */
#define CFG_PWR_STATUS


/**< Macro to Enable the SWP Protocol
 * to detect UICC During Initialisation */
#define ENABLE_UICC

/**< Macro to Enable the RAW Mode of Transaction
 * for the ISO-14443-3A Compliant Targets */
#define ENABLE_MIFARE_RAW

/**< Macro to Enable the HOST List
 * to allow the UICC Communication */
#define HOST_WHITELIST

/**< Support reconnecting to a different handle on the same tag */
#define RECONNECT_SUPPORT

/**< Macro to Enable the Card Emulation Feature */
/* #define HOST_EMULATION */

#define NXP_HAL_VERIFY_EEPROM_CRC  0x01U

/**< Macro to Enable the Download Mode Feature */
#define FW_DOWNLOAD

/**< Macro to Enable the Firmware Download Timer */
/* 0x01U to use overall timeout */
/* 0x02U to use per frame timeout */
#define FW_DOWNLOAD_TIMER   0x02U

/**< Macro to Verify the Firmware Download */
/* #define FW_DOWNLOAD_VERIFY */

#ifndef FW_DOWNLOAD_VERIFY
#define NXP_FW_INTEGRITY_CHK    1
#endif

/* To specify the Maximum TX/RX Len */
#define NXP_FW_MAX_TX_RX_LEN   0x200

#define UICC_CONNECTIVITY_PATCH

/* Work around to Delay the initiator activation */
/* #define NXP_NFCIP_ACTIVATE_DELAY */

/* Work around to Release the Discovered Target */
#define SW_RELEASE_TARGET

/* Macro to Allow the HCI Release in any state */
#define NXP_HCI_SHUTDOWN_OVERRIDE


/* Macro to Enable The P2P Transaction Timers */
#define P2P_TGT_TRANSACT_TIMER

#if (ES_HW_VER == 32)
/* Macro to Configure the Target Disable Register */
#define NFCIP_TGT_DISABLE_CFG

#endif

/*< Macro to Disable the Felica Mapping */
/* #define DISABLE_FELICA_MAPPING */

/*< Macro to Disable the Felica Mapping */
/* #define DISABLE_JEWEL_MAPPING */

/**< Macro to enable LLC timer */
#define LLC_TIMER_ENABLE

/**< Macro to enable HCI Response timer */
#define NXP_NFC_HCI_TIMER 1

/* A Workaround to Delay and obtain the UICC Status Information */
/* #define UICC_STATUS_DELAY */

#ifdef UICC_STATUS_DELAY
#define UICC_STATUS_DELAY_COUNT 0x00100000
#endif

/**< Macro to delay the LLC RESET response callback,
    Value is in milli-seconds */
#define LLC_RESET_DELAY                 10

/* Macro to Enable the workaround for Tuning of
 * RF for TYPE B and F
 */
/* #define SW_TYPE_RF_TUNING_BF */

/* Workaround to update the Active Guard Timeout */
/* #define MAX_ACTIVATE_TIMEOUT */

/* #define ONE_BYTE_LEN */

#define NFC_RF_NOISE_SW

/**< Define to configure the PMOS Modulation Index value
  */

#ifndef NFC_DEV_PMOS_MOD_DEFAULT
/* 0x3F -> 6%, 0x3A -> 10%, 0x3C -> 10%, 0x35 -> 15.8%,  0x28 -> 25.8% */
#define NFC_DEV_PMOS_MOD_DEFAULT          0x3CU
#endif


#ifndef SW_TYPE_RF_TUNING_BF
#define SW_TYPE_RF_TUNING_BF              0x80U
#endif


/* Reset the Default values of Host Link Timers */
/* Macro to Enable the Host Side Link Timeout Configuration
 * 0x00 ----> Default Pre-defined Configuration;
 * 0x01 ----> Update only the Host Link Guard Timeout Configuration;
 * 0x03 ----> Update Both the Host Link Guard Timeout
              and ACK Timeout Configuration;
 */

#ifndef HOST_LINK_TIMEOUT
#define HOST_LINK_TIMEOUT              0x00U
#endif


#ifndef NXP_NFC_LINK_GRD_CFG_DEFAULT
#define NXP_NFC_LINK_GRD_CFG_DEFAULT   0x0032U
#endif

#ifndef NXP_NFC_LINK_ACK_CFG_DEFAULT
#define NXP_NFC_LINK_ACK_CFG_DEFAULT   0x0005U
#endif

/* Macro to Enable the Interface Character Timeout Configuration
 * 0x00 ----> Default Pre-defined Configuration;
 * 0x01 ----> Update the IFC Timeout Default Configuration;
 */

#ifndef NXP_NFC_IFC_TIMEOUT
#define NXP_NFC_IFC_TIMEOUT            0x00
#endif


#ifndef NXP_NFC_IFC_CONFIG_DEFAULT
#define NXP_NFC_IFC_CONFIG_DEFAULT     0x203AU
#endif

#ifndef NFC_ISO_15693_MULTIPLE_TAGS_SUPPORT
#define NFC_ISO_15693_MULTIPLE_TAGS_SUPPORT 0x00
#endif

/*
 *****************************************************************
 ***********  MACROS ENABLE EEPROM REGISTER WRITE ****************
 *****************************************************************
 */


/* Enable this to Disable the WI Notification */
/* #define DISABLE_WI_NOTIFICATION */

/* Macro to Enable the Configuration of Initiator
 * speed during Discovery configuration
 */
#define INITIATOR_SPEED
#define TARGET_SPEED


/**/
/* #define UICC_SESSION_RESET */

/* Macro to Enable the Configuration of UICC
 * Timer and Bitrate during Initialisation
 */




/* -----------------Structures and Enumerations -------------------------*/




/* -----------------Exported Functions----------------------------------*/


#endif /*PHNFC_CONFIG_H*/

