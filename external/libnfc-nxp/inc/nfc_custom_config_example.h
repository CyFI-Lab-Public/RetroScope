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
* \file nfc_custom_config.h
* \brief HAL Custom Configurations
*
*
* \note This is the configuration header file of the HAL 4.0. custom configurable
*       parameters of the HAL 4.0 are provided in this file
*
* Project: NFC-FRI-1.1 / HAL4.0
*
* $Date: Fri Jun 11 16:44:31 2010 $
* $Author: ing04880 $
* $Revision: 1.11 $
* $Aliases: NFC_FRI1.1_WK1023_R35_1 $
*
*/


/*@{*/
#ifndef NFC_CUSTOM_CONFIG_H
#define NFC_CUSTOM_CONFIG_H
/*@}*/


/**
*  \name Hal
*
* File: \ref nfc_custom_config.h
*
*/


/*
 *****************************************************************
 **********************  CUSTOM MACROS **************************
 *****************************************************************
 */

/**< Max number of remote devices supported*/
#define MAX_REMOTE_DEVICES        0x10


/**< Default Session ID for Initialisation */
#define DEFAULT_SESSION "NXP-NFC2"

/** Resolution value for the timer, here the
    timer resolution is 100 milliseconds */
#define TIMER_RESOLUTION                100U

/**< Defines connection time out value for LLC timer,
    500 is in milliseconds */
#define LINK_CONNECTION_TIMEOUT         500U

/**< Defines guard time out value for LLC timer,
    250 is in milliseconds */
#define LINK_GUARD_TIMEOUT              250U

/**< Macro to Enable SMX Feature During
 * Initialisation */


/* PLEASE NOTE: This Macro should be only enabled if there is a SMART_MX
 * Chip attached to the PN544.
 */
/* #define NXP_HAL_ENABLE_SMX */



/* PLEASE NOTE: Kindly change the DEFAULT_SESSION Macro for each of the
 * configuration change done for the below Macros
 */

/**<  External Clock Request Configuration for the NFC Device,
      0x00U -> No Clock Request,
      0x01U -> Clock Request through CLKREQ pin (GPIO pin 2),
      0x02U -> Clock Request through NXP_EVT_CLK_REQUEST Event,
      */
#define NXP_DEFAULT_CLK_REQUEST         0x00U

/**<  External Input Clock Setting for the NFC Device,
      0x00U -> No Input Clock Required (Use the Xtal),
      0x01U -> 13 MHZ,
      0x02U -> 19.2 MHZ,
      0x03U -> 26 MHZ,
      0x04U -> 38.4 MHZ,
      0x05U -> Custom (Set the Custome Clock Registry),
      */
#define NXP_DEFAULT_INPUT_CLK           0x00U



#define NFC_DEV_HWCONF_DEFAULT          0xBCU

/**<  TX LDO Configuration
       0x00 -> 00b     3.0 V,
       0x01 -> 01b     3.0 V,
       0x02 -> 10b     2.7 V,
       0x03 -> 11b     3.3 V,

      */
#define NXP_DEFAULT_TX_LDO              0x00U


/**<  External Clock Request Configuration for the NFC Device,
      0x00U -> No Power Request,
      0x01U -> Power Request through CLKREQ pin (GPIO pin 2),
      0x02U -> Power Request through PWR_REQUEST (GPIO Pin 3),
      */
#define NXP_UICC_PWR_REQUEST            0x00U

/**<  UICC Bit Rate Configuration
       0x02U -> 212Kbits/Sec
       0x04U -> 424Kbits/Sec
       0x08U -> 828Kbits/Sec
 */

#define NXP_UICC_BIT_RATE               0x08U

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

#define NXP_SYSTEM_PWR_STATUS           0x01U


/**<  System Event Notification
       0x01     Overcurrent
       0x02     PMUVCC Switch
       0x04     External RF Field
       0x08     Memory Violation
       0x10     Temperature Overheat
 */

#define NXP_SYSTEM_EVT_INFO             0x10U

/**<  NFCIP Active Mode Configuration
       0x01     106 kbps
       0x02     212 kbps
       0x04     424 kbps
 */

#define NXP_NFCIP_ACTIVE_DEFAULT        0x01U



/* Reset the Default values of Host Link Timers */
/* Macro to Enable the Host Side Link Timeout Configuration
 * 0x00 ----> Default Pre-defined Configuration;
 * 0x01 ----> Update only the Host Link Guard Timeout Configuration;
 * 0x03 ----> Update Both the Host Link Guard Timeout
              and ACK Timeout Configuration;
 */
#define HOST_LINK_TIMEOUT              0x00U


#define NXP_NFC_LINK_GRD_CFG_DEFAULT   0x0032U


#define NXP_NFC_LINK_ACK_CFG_DEFAULT   0x0005U


/* Macro to Enable the Interface Character Timeout Configuration
 * 0x00 ----> Default Pre-defined Configuration;
 * 0x01 ----> Update the IFC Timeout Default Configuration;
 */
#define NXP_NFC_IFC_TIMEOUT            0x00


#define NXP_NFC_IFC_CONFIG_DEFAULT     0x203AU


#define NXP_NFCIP_PSL_BRS_DEFAULT       0x00U


#endif /* NFC_CUSTOM_CONFIG_H */
