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

/*
 * \file  phNfcTypes.h
 * \brief Basic type definitions.
 *
 * Project: NFC MW / HAL
 *
 * $Date: Thu Jun 25 21:24:53 2009 $
 * $Author: ing04880 $
 * $Revision: 1.13 $
 * $Aliases: NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
 *
 */

#ifndef PHNFCTYPES /* */
#define PHNFCTYPES /* */

/**
 *  \name NFC Types
 *
 * File: \ref phNfcTypes.h
 *
 */
/*@{*/
#define PHNFCTYPES_FILEREVISION "$Revision: 1.13 $" /**< \ingroup grp_file_attributes */
#define PHNFCTYPES_FILEALIASES  "$Aliases: NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"     /**< \ingroup grp_file_attributes */
/*@}*/

#ifndef _WIN32
#include <stdint.h>
#else
#include <Windows.h>
#include <stdio.h>
#define snprintf _snprintf

#ifndef linux
/**
 *  \name Basic Type Definitions
 *
 *  Constant-length-type definition ('C99).
 *
 */
/*@{*/
#ifndef __int8_t_defined /* */
#define __int8_t_defined /* */
typedef signed   char   int8_t;         /**<  \ingroup grp_nfc_common
                                               8 bit signed integer */
#endif

#ifndef __int16_t_defined /* */
#define __int16_t_defined /* */
typedef signed   short  int16_t;        /**< \ingroup grp_nfc_common
                                             16 bit signed integer */
#endif

#ifndef __stdint_h
#ifndef __int32_t_defined /* */
#define __int32_t_defined /* */
typedef signed   long   int32_t;        /**< \ingroup grp_nfc_common
                                             32 bit signed integer */
#endif
#endif

#ifndef __uint8_t_defined /* */
#define __uint8_t_defined /* */
typedef unsigned char   uint8_t;        /**<  \ingroup grp_nfc_common
                                              8 bit unsigned integer */
#endif

#ifndef __uint16_t_defined /* */
#define __uint16_t_defined /* */
typedef unsigned short  uint16_t;       /**< \ingroup grp_nfc_common
                                             16 bit unsigned integer */
#endif

#ifndef __stdint_h
#ifndef __uint32_t_defined /* */
#define __uint32_t_defined /* */
typedef unsigned long   uint32_t;       /**< \ingroup grp_nfc_common
                                             32 bit unsigned integer */
#endif
#endif

#endif /* linux */

#endif /* _WIN32 */

#ifndef TRUE
#define TRUE			(0x01)			  /**< \ingroup grp_nfc_common
                                              Logical True Value */
#endif

#ifndef FALSE
#define FALSE			(0x00)			  /**< \ingroup grp_nfc_common
                                              Logical False Value */
#endif

typedef uint8_t			utf8_t;			/**< \ingroup grp_nfc_common
                                             UTF8 Character String */

typedef uint8_t			bool_t;			/**< \ingroup grp_nfc_common
												boolean data type */

typedef uint16_t        NFCSTATUS;      /**< \ingroup grp_nfc_common
                                             NFC return values 
                                         \ref phNfcStatus.h for different status
                                          values */

#ifndef NULL
#define NULL  ((void *)0)
#endif

/* This Macro to be used to resolve Unused and unreference
 * compiler warnings. 
 */

#define PHNFC_UNUSED_VARIABLE(x) for((x)=(x);(x)!=(x);)

/*@}*/

/**
 *
 *  \name HAL Overall Definitions
 *
 *  Definitions applicable to a variety of purposes and functions/features.
 *
 */
 /*@{*/

#define PHHAL_COMMON_MAX_STRING_LENGTH  0x40U     /**< \ingroup grp_hal_common
                                                     Maximum vendor name length in bytes. */
#define PHHAL_UNKNOWN_DEVICE_TYPE       0x00U   /**< \ingroup grp_hal_common
                                                     Unknown device type. */
#define PHHAL_SERIAL_DEVICE             0x01U  /**< \ingroup grp_hal_common
                                                     Serial device type.  */
#define PHHAL_USB_DEVICE                0x02U   /**< \ingroup grp_hal_common
                                                     USB device type. */
#define PHHAL_I2C_DEVICE                0x03U   /**< \ingroup grp_hal_common
                                                     I2C device type. */
#define PHHAL_SPI_DEVICE                0x04U   /**< \ingroup grp_hal_common
                                                     SPI device type. */
#define PHHAL_PARALLEL_DEVICE           0x05U   /**< \ingroup grp_hal_common
                                                     Parallel device type. */
#define PHHAL_NFCID_LENGTH              0x0AU  /**< \ingroup grp_hal_common
                                                     Maximum length of NFCID 1..3. */

#define PHHAL_MAX_DATASIZE              0xFBU       /* 256 * Maximum Data size sent
                                                     * by the HAL
                                                     */

#define PHHAL_ATQA_LENGTH               0x02U       /**< ATQA length */
#define PHHAL_MAX_UID_LENGTH            0x0AU       /**< Maximum UID length expected */
#define PHHAL_MAX_ATR_LENGTH            0x30U       /**< Maximum ATR_RES (General Bytes) 
                                                     *   length expected */

#define PHHAL_ATQB_LENGTH               0x0BU       /**< ATQB length */

#define PHHAL_PUPI_LENGTH               0x04U       /**< PUPI length */
#define PHHAL_APP_DATA_B_LENGTH         0x04U       /**< Application Data length for Type B */
#define PHHAL_PROT_INFO_B_LENGTH        0x03U       /**< Protocol info length for Type B */
#define PHHAL_FEL_SYS_CODE_LEN          0x02U       /**< Felica System Code Length */
#define PHHAL_FEL_ID_LEN                0x08U       /**< Felica current ID Length */
#define PHHAL_FEL_PM_LEN                0x08U       /**< Felica current PM Length */
#define PHHAL_15693_UID_LENGTH          0x08U       /**< Length of the Inventory bytes for
                                                         ISO15693 Tag */

#define VENDOR_NAME_LEN                 0x14U
#define MAX_TRANSFER_UNIT               0x21U 
#define SESSIONID_SIZE                  0x08U
#define MAX_AID_LEN                     0x10U
#define MAX_UICC_PARAM_LEN              0xFFU

#define MIFARE_BITMASK                  0x08U
#define ISO_14443_BITMASK               0x20U
#define ISO_14443_DETECTED              0x20U
#define NFCIP_BITMASK                   0x40U
#define NFCIP_DETECTED                  0x40U

#define MAX_TARGET_SUPPORTED            MAX_REMOTE_DEVICES

#define NFC_HW_PN65N                    0x10U

#define NXP_NFCIP_NFCID2_ID             0x01FEU

#define NXP_FULL_VERSION_LEN            0x0BU


/*@}*/


/**
 *  \name NFC specific Type Definitions
 *
 */
/*@{*/

/**
 * Data Buffer Structure to hold the Data Buffer
 *
 * This structure holds the Data in the Buffer of the specified 
 * size.
 * 
 */
typedef struct phNfc_sData_t
{
    uint8_t             *buffer;
    uint32_t            length;
} phNfc_sData_t;

/**
 * \brief Possible Hardware Configuration exposed to upper layer.
 * Typically this should be at least the communication link (Ex:"COM1","COM2")
 * the controller is connected to.
 */   
typedef struct phLibNfc_sConfig_t
{
   /** Device node of the controller */
   const char*               deviceNode;
   /** The client ID (thread ID or message queue ID) */
   unsigned int              nClientId;
} phLibNfc_sConfig_t, *pphLibNfc_sConfig_t;


/*!
 * NFC Message structure contains message specific details like
 * message type, message specific data block details, etc.
 */
typedef struct phLibNfc_Message_t
{
    uint32_t eMsgType;/**< Type of the message to be posted*/
    void   * pMsgData;/**< Pointer to message specific data block in case any*/
    uint32_t Size;/**< Size of the datablock*/
} phLibNfc_Message_t,*pphLibNfc_Message_t;


#ifdef WIN32
#define PH_LIBNFC_MESSAGE_BASE  (WM_USER+0x3FF)
#endif
/**
 * Deferred message. This message type will be posted to the client application thread
 * to notify that a deferred call must be invoked.
 */
#define PH_LIBNFC_DEFERREDCALL_MSG			  (0x311)

/**
 *\brief Deferred call declaration.
 * This type of API is called from ClientApplication ( main thread) to notify 
 * specific callback.
 */
typedef  void (*pphLibNfc_DeferredCallback_t) (void*);
/**
 *\brief Deferred parameter declaration.
 * This type of data is passed as parameter from ClientApplication (main thread) to the 
 * callback.
 */
typedef  void *pphLibNfc_DeferredParameter_t;
/**
 *\brief Deferred message specific info declaration.
 * This type of information is packed as message data when \ref PH_LIBNFC_DEFERREDCALL_MSG 
 * type message is posted to message handler thread.
 */ 
typedef struct phLibNfc_DeferredCall_t
{
		pphLibNfc_DeferredCallback_t  pCallback;/**< pointer to Deferred callback */
		pphLibNfc_DeferredParameter_t pParameter;/**< pointer to Deferred parameter */
} phLibNfc_DeferredCall_t;


/** \ingroup  grp_hal_common
 *
 *  \brief Protocol Support Information
 *
 *  The <em> Supported Protocols Structure </em> holds all protocol supported by the current NFC
 *  device.
 *
 *  \note All members of this structure are output parameters [out].
 *
 */
typedef struct phNfc_sSupProtocol_t
{
    unsigned int MifareUL    : 1;  /**< Protocol Mifare Ultra Light or 
                                   any NFC Forum Type-2 tags */
    unsigned int MifareStd   : 1;  /**< Protocol Mifare Standard. */
    unsigned int ISO14443_4A : 1;  /**< Protocol ISO14443-4 Type A.  */
    unsigned int ISO14443_4B : 1;  /**< Protocol ISO14443-4 Type B.  */
    unsigned int ISO15693    : 1;  /**< Protocol ISO15693 HiTag.  */
    unsigned int Felica      : 1;  /**< Protocol Felica. */
    unsigned int NFC         : 1;  /**< Protocol NFC. */
    unsigned int Jewel       : 1;  /**< Protocol Innovision Jewel Tag. */
    /*** TODO: Add SWP, ETSI HCI to this list **/
} phNfc_sSupProtocol_t;


/** \ingroup grp_hal_common
 *
 *
 *  \brief Information related to the NFC Device
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
typedef struct phNfc_sDeviceCapabilities_t
{
    /* */
    uint32_t                hal_version; /**< \ingroup grp_hal_common
                                              HAL 4.0 Version Information. */
    uint32_t                fw_version; /**< \ingroup grp_hal_common
                                              Firmware Version Info. */
    uint32_t                hw_version; /**< \ingroup grp_hal_common
                                              Hardware Version Info. */
    uint8_t                 model_id;   /**< \ingroup grp_hal_common
                                              IC Variant . */
    uint8_t                 hci_version; /**< \ingroup grp_hal_common
                                              ETSI HCI Version Supported */
    utf8_t                  vendor_name[VENDOR_NAME_LEN]; /**< \ingroup grp_hal_common
                                              Vendor name (Null terminated string)*/
    uint8_t                 full_version[NXP_FULL_VERSION_LEN];

    phNfc_sSupProtocol_t    ReaderSupProtocol; /**< Supported protocols 
                                                      (Bitmapped) in Reader mode. */
    phNfc_sSupProtocol_t    EmulationSupProtocol;    /**< Supported protocols
                                                       (Bitmapped) in Emulation 
                                                        mode. */
    char                    firmware_update_info; /** */
} phNfc_sDeviceCapabilities_t;


/*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/


/** \ingroup grp_hal_common
 *
 *  \brief Enumerated MIFARE Commands
 *
 *  The <em> Mifare Command List Enumerator </em> lists all available Mifare native commands.
 *
 * \note None.
 *
 */
typedef enum phNfc_eMifareCmdList_t
{
    phNfc_eMifareRaw        = 0x00U,     /**< This command performs raw transcations .
                                              Format of the phLibNfc_sTransceiveInfo_t 
                                              content in this case shall be as below: 
                                                •   cmd: filed shall set to  phHal_eMifareRaw . 
                                                •   addr : doesn't carry any significance.
                                                •   sSendData : Shall contain formatted raw buffer 
                                                                based on MIFARE commands type used. 
                                                                Formatted buffer shall follow below 
                                                                formating scheme.
 
                                              CmdType+ Block No + CommandSpecific data + 2 byte CRC
                                              Ex: With  Write  4 byte command  on block 8  looks as 
                                              " 0xA2,0x08,0x01,0x02,0x03,0x04,CRC1,CRC2 
                                              Note :  For MIFARE Std card we recommend use MIFARE 
                                                      commands directly.
                                           */
    phNfc_eMifareAuthentA   = 0x60U,     /**< Mifare Standard:\n
                                              This command performs an authentication with KEY A for a sector.\n
                                              Format of the phLibNfc_sTransceiveInfo_t content in this case is : 
                                                •       cmd: field shall set to  phHal_eMifareAuthentA . 
                                                •       addr : indicates MIFARE block address. 
                                                           Ex: 0x08 indicates block 8 needs to be authenticated.
                                                •       sSendData : Shall contain authentication key values. 
                                                                    sSendData ,buffer shall contain authentication 
                                                                    key values 01 02 03 04 05 06 authenticates 
                                                                    block 08 with the key 0x01[..]06. If this 
                                                                    command fails, then  user needs to reactivate 
                                                                    the remote Mifare card.                                            
                                          */
    phNfc_eMifareAuthentB   = 0x61U,     /**< Mifare Standard:\n
                                              This command performs an authentication with KEY B for a sector.\n
                                              Format of the phLibNfc_sTransceiveInfo_t content in this case is : 
                                                •       cmd: field shall set to  phHal_eMifareAuthentB . 
                                                •       addr : indicates MIFARE block address. 
                                                           Ex: 0x08 indicates block 8 needs to be authenticated.
                                                •       sSendData : Shall contain authentication key values. 
                                                                    sSendData ,buffer shall contain authentication 
                                                                    key values 01 02 03 04 05 06 authenticates 
                                                                    block 08 with the key 0x01[..]06. If this 
                                                                    command fails, then  user needs to reactivate 
                                                                    the remote Mifare card.   
                                          */
    phNfc_eMifareRead16     = 0x30U,     /**< Mifare Standard and Ultra Light:\n
                                              Read 16 Bytes from a Mifare Standard block or 4 Mifare Ultra Light pages.\n
                                              Format of the phLibNfc_sTransceiveInfo_t content in this case is : 
                                                •       cmd: field shall set to  phHal_eMifareRead16 . 
                                                •       addr : memory adress to read.   
                                                •       sRecvData : Shall contain buffer of size 16 
                                                                    to read the data into.                                                                  

                                              If this command fails, the user needs to reactivate the 
                                              the remote Mifare card
                                          */
    phNfc_eMifareRead       = 0x30U,
    phNfc_eMifareWrite16    = 0xA0U,     /**< Mifare Standard and Ultra Light:\n
                                              Write 16 Bytes to a Mifare Standard block or 4 Mifare Ultra Light pages.\n
                                              Format of the phLibNfc_sTransceiveInfo_t content in this case is : 
                                                •       cmd: field shall set to  phHal_eMifareWrite16 . 
                                                •       addr : starting memory adress to write from.   
                                                •       sSendData : Shall contain buffer of size 16 containing
                                                                    the data bytes to be written.                                                                  
                                             
                                              If this command fails, the user needs to reactivate the 
                                              the remote Mifare card
                                           */
    phNfc_eMifareWrite4     = 0xA2U,     /**< Mifare Ultra Light:\n
                                              Write 4 bytes.\n
                                              Format of the phLibNfc_sTransceiveInfo_t content in this case is : 
                                                •       cmd: field shall set to  phHal_eMifareWrite4 . 
                                                •       addr : starting memory adress to write from.   
                                                •       sSendData : Shall contain buffer of size 4 containing
                                                                    the data bytes to be written.                                                                  

                                              If this command fails, the user needs to reactivate the 
                                              the remote Mifare card
                                          */
    phNfc_eMifareInc        = 0xC1U,     /**< Increment. */
    phNfc_eMifareDec        = 0xC0U,     /**< Decrement. */
    phNfc_eMifareTransfer   = 0xB0U,     /**< Tranfer.   */
    phNfc_eMifareRestore    = 0xC2U,     /**< Restore.   */
    phNfc_eMifareReadSector = 0x38U,     /**< Read Sector.   */
    phNfc_eMifareWriteSector= 0xA8U,     /**< Write Sector.   */
    phNfc_eMifareInvalidCmd = 0xFFU      /**< Invalid Command */
} phNfc_eMifareCmdList_t;


/** \ingroup grp_hal_common
 *
 *  The <em> T=Cl Command List Enumerator </em> lists all available T=Cl Commands.
 *
 * \note None.
 *
 */
typedef enum phNfc_eIso14443_4_CmdList_t
{
    phNfc_eIso14443_4_Raw             = 0x00U /**< ISO 14443-4 Exchange command:\n
                                                 - This command sends the data buffer directly 
                                                 to the remote device */

} phNfc_eIso14443_4_CmdList_t;


/** \ingroup grp_hal_common
 *
 *  The <em> NFCIP1 Command List Enumerator </em> lists all available NFCIP1 Commands.
 *
 * \note None.
 *
 */
typedef enum phNfc_eNfcIP1CmdList_t
{
       phNfc_eNfcIP1_Raw             = 0x00U /**< NfcIP Exchange command:\n
                                                 - This command sends the data buffer directly 
                                                  to the remote device */
}phNfc_eNfcIP1CmdList_t;


/** \ingroup grp_hal_common
 *
 *  The <em> ISO15693 Command List Enumerator </em> lists all available ISO15693 Commands.
 *
 * \note None.
 *
 */
typedef enum phNfc_eIso15693_CmdList_t
{
#if 0
    phNfc_eIso15693_Raw             = 0x00U, /**< ISO 15693 Exchange Raw command:\n
                                                 - This command sends the data buffer directly 
                                                 to the remote device */
#endif
    phNfc_eIso15693_Cmd             = 0x20U, /**< ISO 15693 Exchange command:\n
                                                 - This command is used to access the card 
                                                 to the remote device */
    phNfc_eIso15693_Invalid         = 0xFFU      /**< Invalid Command */
} phNfc_eIso15693_CmdList_t;


/** \ingroup grp_hal_common
 *
 *  The <em> Felica Command List Enumerator </em> lists all available Felica Commands.
 *
 * \note None.
 *
 */
typedef enum phNfc_eFelicaCmdList_t
{
    phNfc_eFelica_Raw             = 0xF0U, /**< Felica Raw command:\n
                                                 - This command sends the data buffer directly 
                                                 to the remote device */
    phNfc_eFelica_Check           = 0x00, /**< Felica Check command:\n
                                                 - This command checks the data from the Felica
                                                  remote device */
    phNfc_eFelica_Update          = 0x01, /**< Felica Update command:\n
                                                 - This command updates the data onto the Felica
                                                  remote device */
    phNfc_eFelica_Invalid         = 0xFFU      /**< Invalid Command */
} phNfc_eFelicaCmdList_t;


/** \ingroup grp_hal_common
 *
 *  The <em> Jewel Command List Enumerator </em> lists all available Jewel Commands.
 *
 * \note None.
 *
 */
typedef enum phNfc_eJewelCmdList_t
{
    phNfc_eJewel_Raw            = 0x00U, /**< Jewel command:\n
                                                 - This command sends the data buffer directly 
                                                 to the remote device */
    phNfc_eJewel_Invalid        = 0xFFU  /**< Invalid jewel command */
}phNfc_eJewelCmdList_t;


/** \ingroup grp_hal_nfci
*
*  \brief Remote Device Reader A RF Gate Information Container 
*
*  The <em> Reader A structure </em> includes the available information
*  related to the discovered ISO14443A remote device. This information 
*  is updated for every device discovery.
*  \note None.
*
*/
typedef struct phNfc_sIso14443AInfo_t
{
    uint8_t         Uid[PHHAL_MAX_UID_LENGTH];      /**< UID information of the TYPE A
                                                       Tag Discovered */
    uint8_t         UidLength;                      /**< UID information length, shall not be greater 
                                                    than PHHAL_MAX_UID_LENGTH i.e., 10 */
    uint8_t         AppData[PHHAL_MAX_ATR_LENGTH]; /**< Application data information of the 
                                                        tag discovered (= Historical bytes for 
                                                        type A) */  
    uint8_t         AppDataLength;                  /**< Application data length */
    uint8_t         Sak;                            /**< SAK informationof the TYPE A
                                                       Tag Discovered */
    uint8_t         AtqA[PHHAL_ATQA_LENGTH];        /**< ATQA informationof the TYPE A
                                                       Tag Discovered */
    uint8_t         MaxDataRate;                    /**< Maximum data rate supported by the TYPE A
                                                       Tag Discovered */
    uint8_t         Fwi_Sfgt;                       /**< Frame waiting time and start up frame guard 
                                                    time as defined in ISO/IEC 14443-4[7] for 
                                                    type A */
} phNfc_sIso14443AInfo_t;


/** \ingroup grp_hal_nfci
*
*  \brief Remote Device Reader B RF Gate Information Container 
*
*  The <em> Reader B structure </em> includes the available information
*  related to the discovered ISO14443B remote device. This information 
*  is updated for every device discovery.
*  \note None.
*
*/
typedef struct phNfc_sIso14443BInfo_t 
{
    union phNfc_uAtqBInfo
    {
        struct phNfc_sAtqBInfo
        {
            uint8_t         Pupi[PHHAL_PUPI_LENGTH];            /**< PUPI information  of the TYPE B
                                                                    Tag Discovered */
            uint8_t         AppData[PHHAL_APP_DATA_B_LENGTH];   /**< Application Data  of the TYPE B
                                                                    Tag Discovered */
            uint8_t         ProtInfo[PHHAL_PROT_INFO_B_LENGTH]; /**< Protocol Information  of the TYPE B
                                                                    Tag Discovered */
        } AtqResInfo;
        uint8_t         AtqRes[PHHAL_ATQB_LENGTH];          /**< ATQB Response Information of TYPE B
                                                                Tag Discovered */
    } AtqB;
    uint8_t         HiLayerResp[PHHAL_MAX_ATR_LENGTH];  /**< Higher Layer Response information  
                                                             in answer to ATRRIB Command for Type B */  
    uint8_t         HiLayerRespLength;                  /**< Higher Layer Response length */
    uint8_t         Afi;                                /**< Application Family Identifier of TYPE B
                                                                Tag Discovered */
    uint8_t         MaxDataRate;                        /**< Maximum data rate supported by the TYPE B
                                                             Tag Discovered */
} phNfc_sIso14443BInfo_t;


/** \ingroup grp_hal_nfci
*
*  \brief Remote Device Reader B prime RF Gate Information Container 
*
*/
typedef struct phNfc_sIso14443BPrimeInfo_t 
{
    /* TODO: This will be updated later */
    void *BPrimeCtxt;
} phNfc_sIso14443BPrimeInfo_t;


/** \ingroup grp_hal_nfci
*
*  \brief Remote Device Jewel Reader RF Gate Information Container 
*
*  The <em> Jewel Reader structure </em> includes the available information
*  related to the discovered Jewel remote device. This information 
*  is updated for every device discovery.
*  \note None.
*
*/
typedef struct phNfc_sJewelInfo_t 
{
    uint8_t         Uid[PHHAL_MAX_UID_LENGTH];      /**< UID information of the TYPE A
                                                         Tag Discovered */
    uint8_t         UidLength;                      /**< UID information length, shall not be greater 
                                                    than PHHAL_MAX_UID_LENGTH i.e., 10 */
    uint8_t         HeaderRom0; /**< Header Rom byte zero */
    uint8_t         HeaderRom1; /**< Header Rom byte one */

} phNfc_sJewelInfo_t;


/** \ingroup grp_hal_nfci
*
*  \brief Remote Device Felica Reader RF Gate Information Container 
*
*  The <em> Felica Reader structure </em> includes the available information
*  related to the discovered Felica remote device. This information 
*  is updated for every device discovery.
*  \note None.
*
*/
typedef struct phNfc_sFelicaInfo_t
{
    uint8_t     IDm[(PHHAL_FEL_ID_LEN + 2)];              /**< Current ID of Felica tag */
    uint8_t     IDmLength;                          /**< IDm length, shall not be greater 
                                                    than PHHAL_FEL_ID_LEN i.e., 8 */
    uint8_t     PMm[PHHAL_FEL_PM_LEN];              /**< Current PM of Felica tag */
    uint8_t     SystemCode[PHHAL_FEL_SYS_CODE_LEN]; /**< System code of Felica tag */
} phNfc_sFelicaInfo_t;


/** \ingroup grp_hal_nfci
*
*  \brief Remote Device Reader 15693 RF Gate Information Container 
*
*  The <em> Reader A structure </em> includes the available information
*  related to the discovered ISO15693 remote device. This information 
*  is updated for every device discovery.
*  \note None.
*
*/

typedef struct phNfc_sIso15693Info_t
{
    uint8_t         Uid[PHHAL_15693_UID_LENGTH];      /**< UID information of the 15693
                                                       Tag Discovered */
    uint8_t         UidLength;                      /**< UID information length, shall not be greater 
                                                    than PHHAL_15693_UID_LENGTH i.e., 8 */
    uint8_t         Dsfid;                          /**< DSF information of the 15693
                                                       Tag Discovered */
    uint8_t         Flags;                          /**< Information about the Flags
                                                        in the 15693 Tag Discovered */
    uint8_t         Afi;                            /**< Application Family Identifier of
                                                          15693 Tag Discovered */
} phNfc_sIso15693Info_t;


/** \ingroup grp_hal_nfci
*
*  \brief NFC Data Rate Supported between the Reader and the Target
*
*  The <em> \ref phHalNfc_eDataRate enum </em> lists all the Data Rate 
*  values to be used to determine the rate at which the data is transmitted
*  to the target.
*
*  \note None.
*/


/** \ingroup grp_hal_nfci
*
*  \brief NFCIP1 Data rates 
*
*/
typedef enum phNfc_eDataRate_t{
    phNfc_eDataRate_106    = 0x00U, 
    phNfc_eDataRate_212, 
    phNfc_eDataRate_424, 
    /* phNfc_eDataRate_848, 
    phNfc_eDataRate_1696, 
    phNfc_eDataRate_3392, 
    phNfc_eDataRate_6784,*/
    phNfc_eDataRate_RFU 
} phNfc_eDataRate_t;


/** \ingroup grp_hal_nfci
*
*  \brief NFCIP1 Gate Information Container 
*
*  The <em> NFCIP1 structure </em> includes the available information
*  related to the discovered NFCIP1 remote device. This information 
*  is updated for every device discovery.
*  \note None.
*
*/
typedef struct phNfc_sNfcIPInfo_t 
{
    /* Contains the random NFCID3I conveyed with the ATR_REQ. 
        always 10 bytes length 
        or contains the random NFCID3T conveyed with the ATR_RES.
        always 10 bytes length */
    uint8_t         NFCID[PHHAL_MAX_UID_LENGTH];    
    uint8_t         NFCID_Length;
    /* ATR_RES = General bytes length, Max length = 48 bytes */
    uint8_t         ATRInfo[PHHAL_MAX_ATR_LENGTH];  
    uint8_t         ATRInfo_Length;
    /**< SAK information of the tag discovered */
    uint8_t         SelRes;
    /**< ATQA information of the tag discovered */
    uint8_t         SenseRes[PHHAL_ATQA_LENGTH];
    /**< Is Detection Mode of the NFCIP Target Active */
    uint8_t         Nfcip_Active;
    /**< Maximum frame length supported by the NFCIP device */  
    uint16_t        MaxFrameLength;
    /**< Data rate supported by the NFCIP device */
    phNfc_eDataRate_t         Nfcip_Datarate;

} phNfc_sNfcIPInfo_t;


/** \ingroup grp_hal_nfci
*
*  \brief Remote Device Specific Information Container
*
*  The <em> Remote Device Information Union </em> includes the available Remote Device Information
*  structures. Following the device detected, the corresponding data structure is used.
*
*  \note None.
*
*/
typedef union phNfc_uRemoteDevInfo_t
{
    phNfc_sIso14443AInfo_t          Iso14443A_Info;
    phNfc_sIso14443BInfo_t          Iso14443B_Info;
    phNfc_sIso14443BPrimeInfo_t     Iso14443BPrime_Info;
    phNfc_sNfcIPInfo_t              NfcIP_Info;
    phNfc_sFelicaInfo_t             Felica_Info;
    phNfc_sJewelInfo_t              Jewel_Info;
    phNfc_sIso15693Info_t           Iso15693_Info;
} phNfc_uRemoteDevInfo_t;


/** \ingroup grp_hal_nfci
*
*  \brief RF Device Type Listing
*
*  The <em> RF Device Type List </em> is used to identify the type of
*  remote device that is discovered/connected. There seperate
*  types to identify a Remote Reader (denoted by _PCD) and 
*  Remote Tag (denoted by _PICC)
*  \note None.
*
*/
typedef enum phNfc_eRFDevType_t
{
    phNfc_eUnknown_DevType        = 0x00U,

    /* Specific PCD Devices */
    phNfc_eISO14443_A_PCD,
    phNfc_eISO14443_B_PCD,
    phNfc_eISO14443_BPrime_PCD,
    phNfc_eFelica_PCD,
    phNfc_eJewel_PCD,
    phNfc_eISO15693_PCD,
    /* Generic PCD Type */
    phNfc_ePCD_DevType,

    /* Generic PICC Type */
    phNfc_ePICC_DevType,
    /* Specific PICC Devices */
    phNfc_eISO14443_A_PICC,
    phNfc_eISO14443_4A_PICC,
    phNfc_eISO14443_3A_PICC,
    phNfc_eMifare_PICC,
    phNfc_eISO14443_B_PICC,
    phNfc_eISO14443_4B_PICC,
    phNfc_eISO14443_BPrime_PICC,
    phNfc_eFelica_PICC,
    phNfc_eJewel_PICC,
    phNfc_eISO15693_PICC,

    /* NFC-IP1 Device Types */
    phNfc_eNfcIP1_Target, 
    phNfc_eNfcIP1_Initiator, 

    /* Other Sources */
    phNfc_eInvalid_DevType

} phNfc_eRFDevType_t;


/** \ingroup grp_hal_nfci
*
*  \brief Remote Device Type Listing
*
*  The <em> Remote Device Type List </em> is used to identify the type of
*  remote device that is discovered/connected
*  \note This is same as RF Device Type List.
*
*/
typedef phNfc_eRFDevType_t phNfc_eRemDevType_t;


/** \ingroup grp_hal_common
 *
 *
 *  \brief Common Command Attribute
 *
 *  The <em> Hal Command Union </em> includes each available type of Commands.
 *
 * \note None.
 *
 */
typedef union phNfc_uCommand_t
{
  phNfc_eMifareCmdList_t         MfCmd;         /**< Mifare command structure.  */
  phNfc_eIso14443_4_CmdList_t    Iso144434Cmd;  /**< ISO 14443-4 command structure.  */
  phNfc_eFelicaCmdList_t         FelCmd;        /**< Felica command structure.  */
  phNfc_eJewelCmdList_t          JewelCmd;      /**< Jewel command structure.  */
  phNfc_eIso15693_CmdList_t      Iso15693Cmd;   /**< ISO 15693 command structure.  */
  phNfc_eNfcIP1CmdList_t         NfcIP1Cmd;     /**< ISO 18092 (NFCIP1) command structure */
} phNfc_uCmdList_t;


/** \ingroup grp_hal_nfci
 *
 *  \brief Remote Device Information Structure
 *
 *  The <em> Remote Device Information Structure </em> holds information about one single Remote
 *  Device detected by the polling function .\n
 *  It lists parameters common to all supported remote devices.
 *
 *  \note 
 *
 *  \sa \ref phHal4Nfc_ConfigureDiscovery and \ref phHal4Nfc_Connect
 *
 */
typedef struct phNfc_sRemoteDevInformation_t
{
    uint8_t                    SessionOpened;       /**< [out] Boolean 
                                                     *   Flag indicating the validity of
                                                     *   the handle of the remote device. */
    phNfc_eRemDevType_t        RemDevType;          /**< [out] Remote device type which says that remote 
                                                    is Reader A or Reader B or NFCIP or Felica or 
                                                    Reader B Prime or Jewel*/
    phNfc_uRemoteDevInfo_t     RemoteDevInfo;       /**< Union of available Remote Device.
                                                     *   \ref phNfc_uRemoteDevInfo_t Information.  */
} phNfc_sRemoteDevInformation_t;


/*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
/*    TARGET STRUCTURES */


/** \ingroup  grp_hal_common
 *
 *  \brief Transceive Information Data Structure for sending commands/response 
 *         to the remote device
 *
 *  The <em> Transceive Information Data Structure </em> is used to pass the 
 *  Command, Address (only required for MIFARE) and the send and receive data
 *  data structure (buffer and length) for communication with remote device
 *
 *
 */
typedef struct phNfc_sTransceiveInfo_t
{
    phNfc_uCmdList_t                cmd;

    /** \internal Address Field required for only Mifare
     *  Family Proprietary Cards.
     *  The Address Size is Valid only upto 255 Blocks limit
     *  i:e for Mifare 4K
     */
    uint8_t                         addr;
    phNfc_sData_t                   sSendData;
    phNfc_sData_t                   sRecvData;
} phNfc_sTransceiveInfo_t;


/** \ingroup grp_hal_nfci
*
*  \brief Poll Device Information for conifiguring the discovery wheel
          Reader and Card Emulation Phases
*
*  The <em> \ref phNfc_sPollDevInfo_t enum </em> is used to enable/disable 
*  phases of the discovery wheel related to specific reader types and 
*  card emulation phase
*  \note Enabling specific Reader technology when NFCIP1 speed is set in the
*        phNfc_sADD_Cfg_t is implicitly done in HAL. Use this structure to only
*        enable/disable Card Reader Functionality
*/
typedef struct phNfc_sPollDevInfo_t
{
    unsigned                    EnableIso14443A : 1;      /**< Flag to enable 
                                                        Reader A discovery */
    unsigned                    EnableIso14443B : 1;      /**< Flag to enable 
                                                        Reader B discovery */
    unsigned                    EnableFelica212 : 1;   /**< Flag to enable
                                                        Felica 212 discovery */
    unsigned                    EnableFelica424 : 1;   /**< Flag to enable
                                                        Felica 424 discovery */
    unsigned                    EnableIso15693 : 1;     /**< Flag to enable 
                                                        ISO 15693 discovery */
    unsigned                    EnableNfcActive : 1; /**< Flag to enable 
                                                        Active Mode of NFC-IP discovery. 
                                                        This is updated internally 
                                                        based on the NFC-IP speed.
                                                        */
    unsigned                    RFU : 1;                /**< Reserved for future use */
    unsigned                    DisableCardEmulation : 1;    /**< Flag to 
                                                            disable the card emulation */
} phNfc_sPollDevInfo_t;


/** \ingroup grp_hal_nfci
*
*  \brief P2P speed for the Initiator
*
*  The <em> \ref phNfc_eP2PMode_t enum </em> lists all the NFCIP1 speeds 
*  to be used for configuring the NFCIP1 discovery
*
*  \note None.
*/
typedef enum phNfc_eP2PMode_t
{
    phNfc_eDefaultP2PMode  = 0x00U,
    phNfc_ePassive106 = 0x01U,
    phNfc_ePassive212 = 0x02U,
    phNfc_ePassive424 = 0x04U,
    phNfc_eActive106 = 0x08U,
    phNfc_eActive212 = 0x10U,
    phNfc_eActive424 = 0x20U,
    phNfc_eP2P_ALL   = 0x27U,  /* All Passive and 424 Active */
    phNfc_eInvalidP2PMode = 0xFFU
} phNfc_eP2PMode_t;


/** \ingroup grp_hal_common
*
*  \brief Identities the type of Notification
*
*  This enumeration is used to specify the type of notification notified
*  to the upper layer. This classifies the notification into two types
*  one for the discovery notifications and the other for all the remaining
*  event notifications
*  \note None.
*/
typedef enum phNfc_eNotificationType_t
{
    INVALID_NFC_NOTIFICATION    = 0x00U, /* Invalid Notification */
    NFC_DISCOVERY_NOTIFICATION,         /* Remote Device Discovery Notification */
    NFC_EVENT_NOTIFICATION              /* Event Notification from the other hosts */
} phNfc_eNotificationType_t;


/** \ingroup grp_hal_common
*
*  \brief 
*
*  \note None.
*/
typedef struct phNfc_sUiccInfo_t
{
    /* AID and Parameter Information is obtained if the
     * eventType is NFC_EVT_TRANSACTION.
     */
    phNfc_sData_t           aid;
    phNfc_sData_t           param;

} phNfc_sUiccInfo_t;


/** \ingroup grp_hal_nfci
*
*  \brief P2P Information for the Initiator
*
*  The <em> \ref phNfc_sNfcIPCfg_t </em> holds the P2P related information
*  use by the NFC Device during P2P Discovery and connection
*
*  \note None.
*/
typedef struct phNfc_sNfcIPCfg_t 
{
    /* ATR_RES = General bytes length, Max length = 48 bytes */
    uint8_t         generalBytesLength;
    uint8_t         generalBytes[PHHAL_MAX_ATR_LENGTH];  

    /* TODO: This will be updated later for any additional params*/
} phNfc_sNfcIPCfg_t;


/** \ingroup grp_hal_common
*
*  \brief Discovery Configuration Mode
*
*  This enumeration is used to choose the Discovery Configuration
*  Mode :- Configure and Start, Stop or Start with last set 
*  configuration
*  \note None.
*/
typedef enum phNfc_eDiscoveryConfigMode_t
{
    NFC_DISCOVERY_CONFIG  = 0x00U,/**< Configure discovery with values 
                                       in phNfc_sADD_Cfg_t and start 
                                       discovery */
    NFC_DISCOVERY_START,         /**< Start Discovery with previously set
                                      configuration */
    NFC_DISCOVERY_STOP,          /**< Stop the Discovery */
    NFC_DISCOVERY_RESUME       /**< Resume the Discovery with previously 
                                   *  set configuration.
                                   *  This is valid only when the Target
                                   *  is not connected.
                                   */
}phNfc_eDiscoveryConfigMode_t;

/** \ingroup grp_hal_common
*
*  \brief Target or Tag Release Mode
*
*  This enumeration defines various modes of releasing an acquired target 
*  or tag.
*  \note None.
*/
typedef enum phNfc_eReleaseType_t
{
    NFC_INVALID_RELEASE_TYPE    =0x00U,/**<Invalid release type */
    NFC_DISCOVERY_RESTART,      /**< Release current target and 
                                     restart discovery within same technology*/
    NFC_DISCOVERY_CONTINUE,    /**< Release current target and continue
                                    discovery with next technology in the wheel */
    NFC_SMARTMX_RELEASE    /**< Release SmartMX from wired mode to previous mode
                                (Virtual or Off) */
} phNfc_eReleaseType_t;

/** \ingroup  grp_hal_common
*
*  \brief Poll configuration structure
*
*  The <em> Poll configuration structure </em> holds information about the 
*  enabling the the type of discovery required by the application. This 
*  structure is the input parameter for the discovery call
*
*  \note All members of this structure are input parameters [out].
*
*  \sa \ref phNfc_eP2PMode_t
*
*/
typedef struct phNfc_sADD_Cfg_t
{
    union 
    {
        phNfc_sPollDevInfo_t        PollCfgInfo;        /**<  Enable/Disable Specific 
                                                              Reader Functionality and 
                                                              Card Emulation */ 
        unsigned                    PollEnabled;     /** Can be used to set polling 'Off'
                                                      by setting PollEnabled to zero */

    } PollDevInfo;
    uint32_t                    Duration;           /**< Duration of virtual or idle 
                                                    period in microseconds in the step size
                                                    of 48 microseconds.If duration is set less
                                                    than  48 microseconds then default value is
                                                    used.For more details please refer PN 544 
                                                    user manual*/
    uint8_t                     NfcIP_Mode ;      /**< Select the P2P
                                                    speeds using phNfc_eP2PMode_t type.
                                                    This is used to enable NFC-IP Discovery 
                                                    The related Reader Type will be implicitly
                                                    selected */
    uint8_t                     NfcIP_Target_Mode ;
    uint8_t                     NfcIP_Tgt_Disable;   /**< Flag to 
                                                   disable the NFCIP1 TARGET */
} phNfc_sADD_Cfg_t;

/*@}*/

#endif /* PHNFCTYPES */

