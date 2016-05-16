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
* \file  phDnldNfc.c                                                          *
* \brief Download Mgmt Interface Source for the Firmware Download.                *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Tue Jun 28 14:25:44 2011 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.33 $                                                           *
* $Aliases:  $
*                                                                             *
* =========================================================================== *
*/


/*
################################################################################
***************************** Header File Inclusion ****************************
################################################################################
*/
#include <stdlib.h>
#include <unistd.h>
#include <phNfcConfig.h>
#include <phNfcCompId.h>
#include <phNfcIoctlCode.h>
#include <phDnldNfc.h>
#include <phOsalNfc.h>
#include <phOsalNfc_Timer.h>
#include <phDal4Nfc.h>
#include <utils/Log.h>
/*
################################################################################
****************************** Macro Definitions *******************************
################################################################################
*/

#ifndef STATIC
#define STATIC static
#endif

#if defined (DNLD_SUMMARY) && !defined (DNLD_TRACE)
#define DNLD_TRACE
#endif

/* #if defined(PHDBG_INFO) && defined (PHDBG_CRITICAL_ERROR) */
#if defined(DNLD_TRACE)
extern char phOsalNfc_DbgTraceBuffer[];

#define MAX_TRACE_BUFFER    0x0410
#define Trace_buffer    phOsalNfc_DbgTraceBuffer
/* #define DNLD_PRINT( str )  phOsalNfc_DbgTrace(str) */
#define DNLD_PRINT( str )  phOsalNfc_DbgString(str)
#define DNLD_DEBUG(str, arg) \
    {                                               \
        snprintf(Trace_buffer,MAX_TRACE_BUFFER,str,arg);    \
        phOsalNfc_DbgString(Trace_buffer);              \
    }
#define DNLD_PRINT_BUFFER(msg,buf,len)              \
    {                                               \
        snprintf(Trace_buffer,MAX_TRACE_BUFFER,"\n\t %s:",msg); \
        phOsalNfc_DbgString(Trace_buffer);              \
        phOsalNfc_DbgTrace(buf,len);                \
        phOsalNfc_DbgString("\r");                  \
    }
#else
#define DNLD_PRINT( str )
#define DNLD_DEBUG(str, arg)
#define DNLD_PRINT_BUFFER(msg,buf,len)
#endif

#define DO_DELAY(period)        usleep(period)

/* delay after SW reset cmd in ms, required on uart for XTAL stability */
#define PHDNLD_DNLD_DELAY        5000
//#define PHDNLD_MAX_PACKET        0x0200U  /* Max Total Packet Size is 512 */
#define PHDNLD_MAX_PACKET        32U  /* Max Total Packet Size is 512 */
#define PHDNLD_DATA_SIZE         ((PHDNLD_MAX_PACKET)- 8U) /* 0x01F8U */
                                                     /* Max Data Size is 504 */
#define PHDNLD_MIN_PACKET        0x03U    /* Minimum Packet Size is 3*/

#define DNLD_DEFAULT_RESPONSE_TIMEOUT   0x4000U

#define NXP_FW_MIN_TX_RX_LEN     0x0AU


#if defined( NXP_FW_MAX_TX_RX_LEN ) && \
     ( NXP_FW_MAX_TX_RX_LEN > NXP_FW_MIN_TX_RX_LEN )

#define PHDNLD_FW_TX_RX_LEN   NXP_FW_MAX_TX_RX_LEN

#elif !defined( NXP_FW_MAX_TX_RX_LEN )

/* To specify the Maximum TX/RX Len */
#define NXP_FW_MAX_TX_RX_LEN   0x200
#define PHDNLD_FW_TX_RX_LEN   NXP_FW_MAX_TX_RX_LEN

#else

#define PHDNLD_FW_TX_RX_LEN   NXP_FW_MIN_TX_RX_LEN

#endif

#define PHDNLD_FRAME_LEN_SIZE    0x02U
#define PHDNLD_ADDR_SIZE         0x03U
#define PHDNLD_DATA_LEN_SIZE     0x02U
#define PHDNLD_FRAME_DATA_OFFSET 0x03U

#define DNLD_SM_UNLOCK_MASK      0x01U
#define DNLD_TRIM_MASK           0x02U
#define DNLD_RESET_MASK          0x04U
#define DNLD_VERIFY_MASK         0x08U
#define DNLD_CRITICAL_MASK       0x10U


#define NXP_NFC_IMAG_FW_MAX      0x05U

#define PHDNLD_FW_PATCH_SEC      0x5FU

#define PHDNLD_PAGE_SIZE         0x80U    /* Page Size Configured for 64 Bytes */

#define FW_MAX_SECTION           0x15U    /* Max Number of Sections */

#define DNLD_CRC16_SIZE			 0x02U

#define DNLD_CRC32_SIZE			 0x04U

#define DNLD_CFG_PG_ADDR         0x00008000U
#define DNLD_FW_CODE_ADDR        0x00800000U
#define DNLD_PATCH_CODE_ADDR     0x00018800U
#define DNLD_PATCH_TABLE_ADDR    0x00008200U


/* Raw Command to pass the Data in Download Mode */
#define PHDNLD_CMD_RAW                  0x00U
/* Command to Reset the Device in Download Mode */
#define PHDNLD_CMD_RESET                0x01U
/* Command to Read from the Address specified in Download Mode */
#define PHDNLD_CMD_READ                 0x07U
#define PHDNLD_CMD_READ_LEN             0x0005U
/* Command to write to the Address specified in Download Mode */
#define PHDNLD_CMD_WRITE                0x08U
#define PHDNLD_CMD_SEC_WRITE            0x0CU
#define PHDNLD_CMD_WRITE_MIN_LEN        0x0005U
#define PHDNLD_CMD_WRITE_MAX_LEN        PHDNLD_DATA_SIZE
/* Command to verify the data written */
#define PHDNLD_CMD_CHECK                0x06U
#define PHDNLD_CMD_CHECK_LEN            0x0007U

/* Command to Lock the  */
#define PHDNLD_CMD_LOCK                 0x40U
#define PHDNLD_CMD_LOCK_LEN             0x0002U


/* Command to set the Host Interface properties */
#define PHDNLD_CMD_SET_HIF              0x09U

/* Command to Activate the Patches Updated  */
#define PHDNLD_CMD_ACTIVATE_PATCH       0x0AU

/* Command to verify the Integrity of the data written */
#define PHDNLD_CMD_CHECK_INTEGRITY      0x0BU

/* Command to verify the Integrity of the data written */
#define PHDNLD_CMD_ENCAPSULATE          0x0DU

#define CHECK_INTEGRITY_RESP_CRC16_LEN  0x03U
#define CHECK_INTEGRITY_RESP_CRC32_LEN  0x05U
#define CHECK_INTEGRITY_RESP_COMP_LEN   0x10U


/* Success Response to a Command Sent in the Download Mode */
#define PHDNLD_RESP_SUCCESS             0x00U
/* Timeout Response to a Command Sent in the Download Mode */
#define PHDNLD_RESP_TIMEOUT             0x01U
/* CRC Error Response to a Command Sent in the Download Mode */
#define PHDNLD_RESP_CRC_ERROR           0x02U
/* Access Denied Response to a Command Sent in the Download Mode */
#define PHDNLD_RESP_ACCESS_DENIED       0x08U
/* PROTOCOL Error Response to a Command Sent in the Download Mode */
#define PHDNLD_RESP_PROTOCOL_ERROR      0x0BU
/* Invalid parameter Response to a Command Sent in the Download Mode */
#define PHDNLD_RESP_INVALID_PARAMETER   0x11U
/* Command Not Supported Response to a Command Sent in the Download Mode */
#define PHDNLD_RESP_CMD_NOT_SUPPORTED   0x13U
/* Length parameter error Response to a Command Sent in the Download Mode */
#define PHDNLD_RESP_INVALID_LENGTH      0x18U
/* Checksum Error Response to a Command Sent in the Download Mode */
#define PHDNLD_RESP_CHKSUM_ERROR        0x19U
/* Version already uptodate Response to a Command Sent in the Download Mode */
#define PHDNLD_RESP_VERSION_UPTODATE    0x1DU
/*  Memory operation error during the processing of
                 the Command Frame in the Download Mode */
#define PHDNLD_RESP_MEMORY_UPDATE_ERROR 0x20U
/*  The Chaining of the Command Frame was Successful in the Download Mode */
#define PHDNLD_RESP_CHAINING_SUCCESS    0x21U
/*  The Command is not allowed anymore in the Download Mode */
#define PHDNLD_RESP_CMD_NOT_ALLOWED     0xE0U
/*  The Error during the Chaining the Command Frame in the Download Mode */
#define PHDNLD_RESP_CHAINING_ERROR      0xE6U
/* Write Error Response to a Command Sent in the Download Mode */
#define PHDNLD_RESP_WRITE_ERROR         0x74U

#define PNDNLD_WORD_LEN                 0x04U

#define NXP_MAX_DNLD_RETRY              0x02U

#define NXP_MAX_SECTION_WRITE           0x05U

#define NXP_PATCH_VER_INDEX             0x05U


/*
################################################################################
******************** Enumeration and Structure Definition **********************
################################################################################
*/

typedef enum phDnldNfc_eSeqType{
    DNLD_SEQ_RESET                              = 0x00U,
    DNLD_SEQ_INIT,
    DNLD_SEQ_RAW,
    DNLD_SEQ_LOCK,
    DNLD_SEQ_UNLOCK,
    DNLD_SEQ_UPDATE,
    DNLD_SEQ_ROLLBACK,
    DNLD_SEQ_COMPLETE
} phDnldNfc_eSeqType_t;

typedef enum phDnldNfc_eState
{
    phDnld_Reset_State        = 0x00,
    phDnld_Unlock_State,
    phDnld_Upgrade_State,
    phDnld_Verify_State,
    phDnld_Complete_State,
    phDnld_Invalid_State
}phDnldNfc_eState_t;


typedef enum phDnldNfc_eSeq
{
    phDnld_Reset_Seq        = 0x00,
    phDnld_Activate_Patch,
    phDnld_Deactivate_Patch,
    phDnld_Update_Patch,
    phDnld_Update_Patchtable,
    phDnld_Lock_System,
    phDnld_Unlock_System,
    phDnld_Upgrade_Section,
    phDnld_Verify_Integrity,
    phDnld_Verify_Section,
    phDnld_Complete_Seq,
    phDnld_Raw_Upgrade,
    phDnld_Invalid_Seq
}phDnldNfc_eSeq_t;

typedef enum phDnldNfc_eChkCrc{
    CHK_INTEGRITY_CONFIG_PAGE_CRC     = 0x00U,
    CHK_INTEGRITY_PATCH_TABLE_CRC     = 0x01U,
    CHK_INTEGRITY_FLASH_CODE_CRC      = 0x02U,
    CHK_INTEGRITY_PATCH_CODE_CRC      = 0x03U,
    CHK_INTEGRITY_COMPLETE_CRC        = 0xFFU
} phDnldNfc_eChkCrc_t;



typedef struct hw_comp_tbl
{
   uint8_t           hw_version[3];
   uint8_t           compatibility;
}hw_comp_tbl_t;


typedef struct img_data_hdr
{
  /* Image Identification */
  uint32_t          img_id;
  /* Offset of the Data from the header */
  uint8_t           img_data_offset;
  /* Number of fimware images available in the img_data */
  uint8_t           no_of_fw_img;
  /* Fimware image Padding in the img_data */
  uint8_t           fw_img_pad[2];
 /* HW Compatiblity table for the set of the Hardwares */
  hw_comp_tbl_t     comp_tbl;
  /* This data consists of the firmware images required to download */
}img_data_hdr_t;


typedef struct fw_data_hdr
{
 /* The data offset from the firmware header.
  * Just in case if in future we require to
  * add some more information.
  */
  uint8_t            fw_hdr_len;
  /* Total size of all the sections which needs to be updated */
  uint8_t            no_of_sections;
  uint8_t            hw_comp_no;
  uint8_t            fw_patch;
  uint32_t           fw_version;
}fw_data_hdr_t;



  /* This data consists all the sections that needs to be downloaded */
typedef struct section_hdr
{
  uint8_t            section_hdr_len;
  uint8_t            section_mem_type;
  uint8_t            section_checksum;
  uint8_t            section_conf;
  uint32_t           section_address;
  uint32_t           section_length;
}section_hdr_t;

typedef struct section_info
{
   section_hdr_t *p_sec_hdr;
   uint8_t       *p_trim_data;
  /* The section data consist of the Firmware binary required
   * to be loaded to the particular address.
   */
   uint8_t       *p_sec_data;
  /* The Section checksum to verify the integrity of the section
   * data.
   */
   uint8_t       *p_sec_chksum;
   /** \internal Index used to refer and process the
    *    Firmware Section Data */
   volatile uint32_t           section_offset;

   /** \internal Section Read Sequence */
   volatile uint8_t            section_read;

   /** \internal Section Write Sequence */
   volatile uint8_t            section_write;

   /** \internal TRIM Write Sequence */
   volatile uint8_t            trim_write;

   volatile uint8_t            sec_verify_retry;

}section_info_t;


typedef struct phDnldNfc_sParam
{
    uint8_t data_addr[PHDNLD_ADDR_SIZE];
    uint8_t data_len[PHDNLD_DATA_LEN_SIZE];
    uint8_t data_packet[PHDNLD_DATA_SIZE];
}phDnldNfc_sParam_t;

typedef struct phDnldNfc_sDataHdr
{
    uint8_t frame_type;
    uint8_t frame_length[PHDNLD_FRAME_LEN_SIZE];
}phDnldNfc_sData_Hdr_t;

typedef struct phDnldNfc_sRawHdr
{
    uint8_t frame_type;
    uint8_t frame_length[PHDNLD_FRAME_LEN_SIZE];
}phDnldNfc_sRawHdr_t;

typedef struct phDnldNfc_sRawDataHdr
{
    uint8_t data_addr[PHDNLD_ADDR_SIZE];
    uint8_t data_len[PHDNLD_DATA_LEN_SIZE];
}phDnldNfc_sRawDataHdr_t;

typedef struct phDnldNfc_sChkCrc16_Resp
{
    uint8_t Chk_status;
    uint8_t Chk_Crc16[2];

}phDnldNfc_sChkCrc16_Resp_t;

typedef struct phDnldNfc_sChkCrc32_Resp
{
    uint8_t Chk_status;
    uint8_t Chk_Crc32[4];

}phDnldNfc_sChkCrc32_Resp_t;


typedef struct phDnldNfc_sChkCrcComplete
{
    phDnldNfc_sChkCrc16_Resp_t config_page;
    phDnldNfc_sChkCrc16_Resp_t patch_table;
    phDnldNfc_sChkCrc32_Resp_t flash_code;
    phDnldNfc_sChkCrc32_Resp_t patch_code;
}phDnldNfc_sChkCrcComplete_t;

typedef struct phDnldNfc_sData
{
    uint8_t frame_type;
    uint8_t frame_length[PHDNLD_FRAME_LEN_SIZE];
    union param
    {
        phDnldNfc_sParam_t data_param;
        uint8_t            response_data[PHDNLD_MAX_PACKET];
        uint8_t            cmd_param;
    }param_info;
}phDnldNfc_sData_t;

#ifdef NXP_NFC_MULTIPLE_FW

typedef struct phDnldNfc_sFwImageInfo
{
    /** \internal Data Pointer to the Firmware header section of the Firmware */
    fw_data_hdr_t               *p_fw_hdr;
    /** \internal Buffer pointer to store the Firmware Section Data */
    section_info_t              *p_fw_sec;
    /** \internal Buffer pointer to store the Firmware Raw Data */
    uint8_t                     *p_fw_raw;
}phDnldNfc_sFwImageInfo_t;

#endif /* #ifdef NXP_NFC_MULTIPLE_FW */


typedef struct phDnldNfc_TxInfo
{
    uint8_t       *transmit_frame;

    uint16_t      tx_offset;

    /** \internal Remaining amount of data to be sent */
    uint16_t      tx_len;

    uint16_t      tx_total;

    /** \internal Chain information for the data to be sent */
    uint8_t       tx_chain;

}phDnldNfc_TxInfo_t;


typedef struct phDnldNfc_RxInfo
{
    /** \internal Total length of the received buffer */
    uint16_t      rx_total;
    /** \internal Chain information of the received buffer */
    uint16_t      rx_chain;
    /** \internal Remaining Data information to be read to complete the
      * Data Information.
      */
    uint16_t      rx_remain;

    /** \internal Buffer to Send the Raw Data Frame */
    uint8_t                     raw_buffer_data[PHDNLD_MAX_PACKET
                                                    + PHDNLD_PAGE_SIZE];
}phDnldNfc_RxInfo_t;


typedef struct phDnldNfc_sContext
{
    /** \internal Structure to store the lower interface operations */
    phNfc_sLowerIF_t            lower_interface;

    phNfc_sData_t               *p_fw_version;

    /** \internal Pointer to the Hardware Reference Sturcture */
    phHal_sHwReference_t        *p_hw_ref;

    /** \internal Pointer to the upper layer notification callback function */
    pphNfcIF_Notification_CB_t  p_upper_notify;
    /** \internal Pointer to the upper layer context */
    void                        *p_upper_context;

    /** \internal Timer ID for the Download Abort */
    uint32_t                    timer_id;
    /** \internal Internal Download for the Download Abort */
    uint32_t                    dnld_timeout;
    /** \internal Data Pointer to the Image header section of the Firmware */
    img_data_hdr_t              *p_img_hdr;

#ifdef NXP_NFC_MULTIPLE_FW
    /** \internal Data Pointer to the Firmware Image Information */
    phDnldNfc_sFwImageInfo_t    *p_img_info;
#endif /* #ifdef NXP_NFC_MULTIPLE_FW */

    /** \internal Data Pointer to the Firmware header section of the Firmware */
    fw_data_hdr_t               *p_fw_hdr;
    /** \internal Buffer pointer to store the Firmware Data */
    section_info_t              *p_fw_sec;
    /** \internal Buffer pointer to store the Firmware Raw Data */
    uint8_t                     *p_fw_raw;

    /** \internal Previous Download Size */
    uint32_t                    prev_dnld_size;

    /** \internal Single Data Block to download the Firmware */
    uint8_t                     dnld_data[PHDNLD_MAX_PACKET
                                                    + PHDNLD_PAGE_SIZE];
    /** \internal Index used to refer and process the Download Data */
    volatile uint32_t           dnld_index;

    /** \internal Response Data to process the response */
    phDnldNfc_sData_t           dnld_resp;

    /** \internal Previously downloaded data stored
	  * to compare the written data */
    phNfc_sData_t               dnld_store;

    /** \internal Previously downloaded trimmed data stored  
	  * to compare the written data */
    phNfc_sData_t               trim_store;

    uint8_t                    *p_resp_buffer;

    phDnldNfc_sChkCrcComplete_t chk_integrity_crc;

    phDnldNfc_eChkCrc_t         chk_integrity_param;

#define NXP_FW_SW_VMID_TRIM
#ifdef  NXP_FW_SW_VMID_TRIM

#define NXP_FW_VMID_TRIM_CHK_ADDR   0x0000813DU
#define NXP_FW_VMID_CARD_MODE_ADDR  0x00009931U
#define NXP_FW_VMID_RD_MODE_ADDR    0x00009981U

    uint8_t                     vmid_trim_update;
#endif /* #ifdef  NXP_FW_SW_VMID_TRIM */

    uint8_t						cur_frame_info;

    uint8_t						raw_mode_upgrade;

	uint8_t						*p_patch_table_crc;

	uint8_t						*p_flash_code_crc;

	uint8_t						*p_patch_code_crc;

    uint16_t                    resp_length;

    /** \internal Current FW Section in Process */
    volatile uint8_t            section_index;

    /** \internal Previous Command sent */
    volatile uint8_t            prev_cmd;

    uint8_t                     dnld_retry;

	/** \internal Current Download State */
    volatile uint8_t            cur_dnld_state;
    /** \internal Next Download State */
    volatile uint8_t            next_dnld_state;

    /** \internal Current step in Download Sequence */
    volatile uint8_t            cur_dnld_seq;
    /** \internal Next step in Download Sequence */
    volatile uint8_t            next_dnld_seq;

    /* \internal Data Transmit information */
    phDnldNfc_TxInfo_t          tx_info;

    /* \internal Data Receive information */
    phDnldNfc_RxInfo_t          rx_info;


}phDnldNfc_sContext_t;


/*
################################################################################
******************** Global and Static Variables Definition ********************
################################################################################
*/

#ifndef NFC_TIMER_CONTEXT
static phDnldNfc_sContext_t *gpphDnldContext = NULL;
#endif

#ifdef NXP_FW_DNLD_CHECK_PHASE

#define   NXP_FW_DNLD_COMPLETE_PHASE 0x00U
#define   NXP_FW_DNLD_SYSTEM_PHASE   0x01U
#define   NXP_FW_DNLD_CFG_PHASE      0x02U
#define   NXP_FW_DNLD_DATA_PHASE     0x03U
#define   NXP_FW_DNLD_RAW_PHASE      0x04U
#define   NXP_FW_DNLD_INVALID_PHASE  0xFFU

static uint8_t  gphDnldPhase = NXP_FW_DNLD_COMPLETE_PHASE;

#endif /* #ifdef NXP_FW_DNLD_CHECK_PHASE */

/**/

/*
*************************** Static Function Declaration **************************
*/

STATIC
NFCSTATUS
phDnldNfc_Send_Command(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef,
                        uint8_t                 cmd,
                        void                    *params,
                        uint16_t                param_length
                      );

static
NFCSTATUS
phDnldNfc_Process_FW(
                        phDnldNfc_sContext_t    *psDnldContext,
                        phHal_sHwReference_t    *pHwRef
#ifdef NXP_FW_PARAM
                        ,
                        uint8_t                 *nxp_nfc_fw,
                        uint32_t                fw_length
#endif
                     );

STATIC
void
phDnldNfc_Send_Complete (
                                void                    *psContext,
                                void                    *pHwRef,
                                phNfc_sTransactionInfo_t *pInfo
                       );

STATIC
void
phDnldNfc_Receive_Complete (
                                void                    *psContext,
                                void                    *pHwRef,
                                phNfc_sTransactionInfo_t *pInfo
                                );

STATIC
NFCSTATUS
phDnldNfc_Process_Response(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef,
                        void                    *pdata,
                        uint16_t                length
                     );


static
NFCSTATUS
phDnldNfc_Resume(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef,
                        void                    *pdata,
                        uint16_t                length
                     );

static
NFCSTATUS
phDnldNfc_Resume_Write(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef
                     );

static
NFCSTATUS
phDnldNfc_Process_Write(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef,
                        section_info_t          *p_sec_info,
                        uint32_t                *p_sec_offset
                     );

static
NFCSTATUS
phDnldNfc_Sequence(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef,
                        void                    *pdata,
                        uint16_t                length
                        );

static
NFCSTATUS
phDnldNfc_Upgrade_Sequence(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef,
                        void                    *pdata,
                        uint16_t                length
                        );

STATIC
NFCSTATUS
phDnldNfc_Receive(
                        void                *psContext,
                        void                *pHwRef,
                        uint8_t             *pdata,
                        uint16_t             length
                    );


STATIC
NFCSTATUS
phDnldNfc_Send (
                    void                    *psContext,
                    void                    *pHwRef,
                    uint8_t                 *pdata,
                    uint16_t                length
                    );

STATIC
NFCSTATUS
phDnldNfc_Set_Seq(
                                phDnldNfc_sContext_t    *psDnldContext,
                                phDnldNfc_eSeqType_t    seq_type
                        );

static
void
phDnldNfc_Notify(
                    pphNfcIF_Notification_CB_t  p_upper_notify,
                    void                        *p_upper_context,
                    void                        *pHwRef,
                    uint8_t                     type,
                    void                        *pInfo
               );

STATIC
NFCSTATUS
phDnldNfc_Allocate_Resource (
                                void                **ppBuffer,
                                uint16_t            size
                            );

STATIC
void
phDnldNfc_Release_Resources (
                                phDnldNfc_sContext_t    **ppsDnldContext
                            );

STATIC
void
phDnldNfc_Release_Lower(
                    phDnldNfc_sContext_t        *psDnldContext,
                    void                        *pHwRef
               );


static
NFCSTATUS
phDnldNfc_Read(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef,
                        section_info_t          *p_sec_info
                   );

STATIC
void
phDnldNfc_Abort (
                    uint32_t abort_id
#ifdef NFC_TIMER_CONTEXT
                    , void     *dnld_cntxt
#endif
                );


#ifdef DNLD_CRC_CALC

static
void
phDnldNfc_UpdateCrc16(
    uint8_t     crcByte,
    uint16_t    *pCrc
);

STATIC
uint16_t
phDnldNfc_ComputeCrc16(
    uint8_t     *pData,
    uint16_t    length
);


/*
*************************** Function Definitions **************************
*/
#define CRC32_POLYNOMIAL     0xEDB88320L

static uint32_t CRC32Table[0x100];
 
void BuildCRCTable()
{
    unsigned long crc;
    uint8_t i = 0, j = 0;
 
    for ( i = 0; i <= 0xFF ; i++ ) 
    {
        crc = i;
        for ( j = 8 ; j> 0; j-- ) 
        {
            if ( crc & 1 )
            {
                crc = ( crc>> 1 ) ^ CRC32_POLYNOMIAL;
            }
            else
            {
                crc>>= 1;
            }
        }
        CRC32Table[ i ] = crc;
    }
}

/*
* This routine calculates the CRC for a block of data using the
* table lookup method. It accepts an original value for the crc,
* and returns the updated value.
*/
 
uint32_t CalculateCRC32( void *buffer , uint32_t count, uint32_t crc )
{
    uint8_t *p;
    uint32_t temp1;
    uint32_t temp2;
 
    p = (uint8_t *) buffer;
    while ( count-- != 0 ) {
        temp1 = ( crc>> 8 ) & 0x00FFFFFFL;
        temp2 = CRC32Table[ ( (int) crc ^ *p++ ) & 0xff ];
        crc = temp1 ^ temp2;
    }
    return( crc );
}


static
void
phDnldNfc_UpdateCrc16(
    uint8_t     crcByte,
    uint16_t    *pCrc
)
{
    crcByte = (crcByte ^ (uint8_t)((*pCrc) & 0x00FF));
    crcByte = (crcByte ^ (uint8_t)(crcByte << 4));
    *pCrc = (*pCrc >> 8) ^ ((uint16_t)crcByte << 8) ^
                ((uint16_t)crcByte << 3) ^
                ((uint16_t)crcByte >> 4);
}


STATIC
uint16_t
phDnldNfc_ComputeCrc16(
    uint8_t     *pData,
    uint16_t    length
)
{
    uint8_t     crc_byte = 0;
    uint16_t    index = 0;
    uint16_t    crc = 0;

#ifdef CRC_A
        crc = 0x6363; /* ITU-V.41 */
#else
        crc = 0xFFFF; /* ISO/IEC 13239 (formerly ISO/IEC 3309) */
#endif /* #ifdef CRC_A */

    do
    {
        crc_byte = pData[index];
        phDnldNfc_UpdateCrc16(crc_byte, &crc);
        index++;
    } while (index < length);

#ifndef INVERT_CRC
    crc = ~crc; /* ISO/IEC 13239 (formerly ISO/IEC 3309) */
#endif /* #ifndef INVERT_CRC */

/*    *pCrc1 = (uint8_t) (crc & BYTE_MASK);
      *pCrc2 = (uint8_t) ((crc >> 8) & BYTE_MASK); */
    return crc ;
}

#endif /* #ifdef DNLD_CRC_CALC */


/*!
 * \brief Allocation of the Download Interface resources.
 *
 * This function releases and frees all the resources used by Download Mode
 * Feature.
 */

STATIC
NFCSTATUS
phDnldNfc_Allocate_Resource (
                                void                **ppBuffer,
                                uint16_t            size
                            )
{
    NFCSTATUS           status = NFCSTATUS_SUCCESS;

    *ppBuffer = (void *) phOsalNfc_GetMemory(size);
    if( *ppBuffer != NULL )
    {
        (void )memset(((void *)*ppBuffer), 0,
                                    size);
    }
    else
    {
        *ppBuffer = NULL;
        status = PHNFCSTVAL(CID_NFC_DNLD,
                        NFCSTATUS_INSUFFICIENT_RESOURCES);
    }
    return status;
}


/*!
 * \brief Release of the Download Interface resources.
 *
 * This function releases and frees all the resources used by Download layer.
 */

STATIC
void
phDnldNfc_Release_Resources (
                                phDnldNfc_sContext_t    **ppsDnldContext
                            )
{

    if(NULL != (*ppsDnldContext)->p_resp_buffer)
    {
        phOsalNfc_FreeMemory((*ppsDnldContext)->p_resp_buffer);
        (*ppsDnldContext)->p_resp_buffer = NULL;
    }
    if(NULL != (*ppsDnldContext)->dnld_store.buffer)
    {
        phOsalNfc_FreeMemory((*ppsDnldContext)->dnld_store.buffer);
        (*ppsDnldContext)->dnld_store.buffer = NULL;
        (*ppsDnldContext)->dnld_store.length = 0;
    }
    if(NULL != (*ppsDnldContext)->trim_store.buffer)
    {
        phOsalNfc_FreeMemory((*ppsDnldContext)->trim_store.buffer);
        (*ppsDnldContext)->trim_store.buffer = NULL;
        (*ppsDnldContext)->trim_store.length = 0;
    }
    if(NULL != (*ppsDnldContext)->p_fw_sec)
    {
        phOsalNfc_FreeMemory((*ppsDnldContext)->p_fw_sec);
        (*ppsDnldContext)->p_fw_sec = NULL;
    }
    if ( NXP_INVALID_TIMER_ID != (*ppsDnldContext)->timer_id )
    {
        phOsalNfc_Timer_Stop((*ppsDnldContext)->timer_id );
        phOsalNfc_Timer_Delete((*ppsDnldContext)->timer_id );
        (*ppsDnldContext)->timer_id = NXP_INVALID_TIMER_ID;
    }

    phOsalNfc_FreeMemory((*ppsDnldContext));
    (*ppsDnldContext) = NULL;

    return ;
}


STATIC
void
phDnldNfc_Release_Lower(
                    phDnldNfc_sContext_t        *psDnldContext,
                    void                        *pHwRef
               )
{
    phNfc_sLowerIF_t    *plower_if =
                            &(psDnldContext->lower_interface);
    NFCSTATUS            status = NFCSTATUS_SUCCESS;

    PHNFC_UNUSED_VARIABLE(status);

    if(NULL != plower_if->release)
    {
#ifdef DNLD_LOWER_RELEASE
        status = plower_if->release((void *)plower_if->pcontext,
                                        (void *)pHwRef);
#else
        PHNFC_UNUSED_VARIABLE(pHwRef);

#endif
        (void)memset((void *)plower_if,
                                                0, sizeof(phNfc_sLowerIF_t));
        DNLD_DEBUG(" FW_DNLD: Releasing the Lower Layer Resources: Status = %02X\n"
                                                                    ,status);
    }

    return;
}



static
void
phDnldNfc_Notify(
                    pphNfcIF_Notification_CB_t  p_upper_notify,
                    void                        *p_upper_context,
                    void                        *pHwRef,
                    uint8_t                     type,
                    void                        *pInfo
               )
{
    if( ( NULL != p_upper_notify) )
    {
        /* Notify the to the Upper Layer */
        (p_upper_notify)(p_upper_context, pHwRef, type, pInfo);
    }
}


STATIC
NFCSTATUS
phDnldNfc_Set_Seq(
                                phDnldNfc_sContext_t    *psDnldContext,
                                phDnldNfc_eSeqType_t    seq_type
                        )
{
    NFCSTATUS                       status = NFCSTATUS_SUCCESS;
    static  uint8_t                 prev_temp_state = 0;
    static  uint8_t                 prev_temp_seq =
                                (uint8_t) phDnld_Activate_Patch;

    switch(seq_type)
    {
        case DNLD_SEQ_RESET:
        case DNLD_SEQ_INIT:
        {
            psDnldContext->cur_dnld_state =
                                (uint8_t) phDnld_Reset_State;
            psDnldContext->next_dnld_state =
                            (uint8_t)phDnld_Upgrade_State;
            psDnldContext->cur_dnld_seq =
                            (uint8_t)phDnld_Upgrade_Section;
            psDnldContext->next_dnld_seq =
                                psDnldContext->cur_dnld_seq;
            break;
        }
        case DNLD_SEQ_RAW:
        {
            psDnldContext->cur_dnld_state =
                                (uint8_t) phDnld_Reset_State;
            psDnldContext->next_dnld_state =
                            (uint8_t)phDnld_Upgrade_State;
            psDnldContext->cur_dnld_seq =
                            (uint8_t)phDnld_Raw_Upgrade;
            psDnldContext->next_dnld_seq =
                                psDnldContext->cur_dnld_seq;
            break;
        }
        case DNLD_SEQ_UNLOCK:
        {
            psDnldContext->cur_dnld_state =
                                (uint8_t) phDnld_Reset_State;

#ifdef NXP_FW_DNLD_CHECK_PHASE
            if( NXP_FW_DNLD_SYSTEM_PHASE < gphDnldPhase )
            {
                psDnldContext->next_dnld_state =
                                (uint8_t)phDnld_Upgrade_State;
                psDnldContext->cur_dnld_seq =
                                (uint8_t)phDnld_Upgrade_Section;
            }
            else
#endif /* NXP_FW_DNLD_CHECK_PHASE */
            {
                psDnldContext->next_dnld_state =
                                    (uint8_t) phDnld_Unlock_State;
                psDnldContext->cur_dnld_seq =
                                    (uint8_t) phDnld_Activate_Patch;
            }
            psDnldContext->next_dnld_seq =
                                psDnldContext->cur_dnld_seq;
            break;
        }
        case DNLD_SEQ_LOCK:
        {
            psDnldContext->cur_dnld_state =
                                (uint8_t) phDnld_Reset_State;
            psDnldContext->next_dnld_state =
                                (uint8_t) phDnld_Reset_State;
            psDnldContext->cur_dnld_seq =
                                (uint8_t) phDnld_Lock_System;
            psDnldContext->next_dnld_seq =
                                psDnldContext->cur_dnld_seq;
            break;
        }
        case DNLD_SEQ_UPDATE:
        {
            prev_temp_state = (uint8_t) psDnldContext->cur_dnld_state;
            psDnldContext->cur_dnld_state =
                        psDnldContext->next_dnld_state;
            /* psDnldContext->next_dnld_state =
                        (uint8_t)phDnld_Invalid_State ; */
            prev_temp_seq = (uint8_t) psDnldContext->cur_dnld_seq;
            psDnldContext->cur_dnld_seq =
                        psDnldContext->next_dnld_seq;
            break;
        }
        case DNLD_SEQ_ROLLBACK:
        {
            psDnldContext->cur_dnld_seq = (uint8_t)  prev_temp_seq;
            psDnldContext->next_dnld_seq =
                        (uint8_t)phDnld_Invalid_Seq ;
            prev_temp_seq = 0;

            psDnldContext->cur_dnld_state = (uint8_t)  prev_temp_state;
            /* psDnldContext->next_dnld_state =
                        (uint8_t)phDnld_Invalid_State ; */
            prev_temp_state = 0;
            break;
        }
        case DNLD_SEQ_COMPLETE:
        {
            psDnldContext->cur_dnld_state =
                                (uint8_t) phDnld_Reset_State;
            psDnldContext->next_dnld_state =
                                (uint8_t) phDnld_Verify_State;
            psDnldContext->cur_dnld_seq =
                                (uint8_t) phDnld_Verify_Integrity;
            psDnldContext->next_dnld_seq =
                                psDnldContext->cur_dnld_seq ;
            break;
        }
        default:
        {
            break;
        }
    }

    return status;
}



/*!
 * \brief Sends the data the corresponding peripheral device.
 *
 * This function sends the Download data to the connected NFC Pheripheral device
 */


 STATIC
 NFCSTATUS
 phDnldNfc_Send (
                      void                  *psContext,
                      void                  *pHwRef,
                      uint8_t               *pdata,
                      uint16_t              length
                     )
{
    phDnldNfc_sContext_t    *psDnldContext= (phDnldNfc_sContext_t  *)psContext;
    NFCSTATUS               status = NFCSTATUS_SUCCESS;

    phNfc_sLowerIF_t        *plower_if = &(psDnldContext->lower_interface);

    if( (NULL != plower_if)
        && (NULL != plower_if->send)
      )
    {
#ifndef DNLD_SUMMARY
        DNLD_PRINT_BUFFER("Send Buffer",pdata,length);
#endif
        status = plower_if->send((void *)plower_if->pcontext,
                                (void *)pHwRef, pdata, length);

#if defined(FW_DOWNLOAD_TIMER) && \
                (FW_DOWNLOAD_TIMER == 2)
    if (
         (NFCSTATUS_PENDING == status)
        && ( NXP_INVALID_TIMER_ID != psDnldContext->timer_id )
       )
    {
        psDnldContext->dnld_timeout = NXP_DNLD_COMPLETE_TIMEOUT;

        if ( psDnldContext->dnld_timeout
                        <   DNLD_DEFAULT_RESPONSE_TIMEOUT)
        {
            psDnldContext->dnld_timeout
                            = DNLD_DEFAULT_RESPONSE_TIMEOUT;
        }
        /* Start the Download Timer */
        phOsalNfc_Timer_Start( psDnldContext->timer_id,
                psDnldContext->dnld_timeout,
                (ppCallBck_t) phDnldNfc_Abort
#ifdef NFC_TIMER_CONTEXT
                , (void *) psDnldContext
#endif
                );

        DNLD_DEBUG(" DNLD : Timer %X Started ", psDnldContext->timer_id);
        DNLD_DEBUG(" \t\t With %U Timeout \n", psDnldContext->dnld_timeout);
    }

#endif /* (NXP_NFC_DNLD_TIMER == 1) */
    }

    return status;
}


/*!
 * \brief Receives the Download Mode Response from the corresponding peripheral device.
 *
 * This function receives the Download Command Response to the connected NFC
 * Pheripheral device.
 */

STATIC
NFCSTATUS
phDnldNfc_Receive(
                        void                *psContext,
                        void                *pHwRef,
                        uint8_t             *pdata,
                        uint16_t             length
                    )
{
    phDnldNfc_sContext_t    *psDnldContext= (phDnldNfc_sContext_t  *)psContext;
    phNfc_sLowerIF_t *plower_if = NULL ;
    NFCSTATUS         status = NFCSTATUS_SUCCESS;

    if(NULL == psDnldContext )
    {
        status = PHNFCSTVAL(CID_NFC_DNLD, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        plower_if = &(psDnldContext->lower_interface);

        if( (NULL != plower_if)
            && (NULL != plower_if->receive)
          )
        {
            status = plower_if->receive((void *)plower_if->pcontext,
                                    (void *)pHwRef, pdata, length);
        }
    }
    return status;
}


static
NFCSTATUS
phDnldNfc_Read(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef,
                        section_info_t          *p_sec_info
                   )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phDnldNfc_sData_t       *p_dnld_data =
                 (phDnldNfc_sData_t *)psDnldContext->dnld_data;
    phDnldNfc_sParam_t      *p_data_param =
                        &p_dnld_data->param_info.data_param;
    uint32_t        read_addr = (p_sec_info->p_sec_hdr->section_address
                                            + p_sec_info->section_offset);
    static unsigned sec_type = 0;
    uint8_t         i = 0;
    uint16_t        read_size = 0 ;

    sec_type = (unsigned int)p_sec_info->p_sec_hdr->section_mem_type;

    if( ( FALSE ==  p_sec_info->section_read )
    && ((sec_type & DNLD_TRIM_MASK))
    && (FALSE == p_sec_info->trim_write) )
    {
        read_size = (uint16_t) p_sec_info->p_sec_hdr->section_length;
        DNLD_DEBUG(" FW_DNLD: Section Read  = %X \n", read_size);
    }
    else
    {
        if (( FALSE ==  p_sec_info->section_read )
            && ((sec_type & DNLD_VERIFY_MASK))
            )
        {
            read_size = (uint16_t)(psDnldContext->prev_dnld_size );
            DNLD_DEBUG(" FW_DNLD: Section Read  = %X \n", read_size);
        }
        else if( ( TRUE ==  p_sec_info->section_read )
            && ( TRUE ==  p_sec_info->section_write )
            )
        {
            /*Already Read the Data Hence Ignore the Read */
           DNLD_DEBUG(" FW_DNLD: Already Read, Read Ignored, read_size = %X \n", read_size);
        }
        else
        {
            /* Ignore the Read */
           DNLD_DEBUG(" FW_DNLD: Section Read Status = %X \n", p_sec_info->section_read);
           DNLD_DEBUG(" FW_DNLD: Section Write Status = %X \n", p_sec_info->section_write);
           DNLD_DEBUG(" FW_DNLD: No Read Required, Read_size = %X \n", read_size);
        }
    }

    if (read_size != 0)
    {

        read_size = (uint16_t)((PHDNLD_DATA_SIZE >= read_size)?
                                            read_size: PHDNLD_DATA_SIZE);

        p_dnld_data->frame_length[i] = (uint8_t)0;
        /* Update the LSB of the Data and the Address Parameter*/
        p_data_param->data_addr[i] = (uint8_t)((read_addr  >>
                                 (BYTE_SIZE + BYTE_SIZE)) & BYTE_MASK);
        p_data_param->data_len[i] = (uint8_t)((read_size >>
                                    BYTE_SIZE) & BYTE_MASK);
        i++;

        p_dnld_data->frame_length[i] = (uint8_t)
                            ( PHDNLD_CMD_READ_LEN & BYTE_MASK);
        /* Update the 2nd byte of the Data and the Address Parameter*/
        p_data_param->data_addr[i] = (uint8_t)((read_addr  >>
                               BYTE_SIZE) & BYTE_MASK);
        p_data_param->data_len[i] = (uint8_t) (read_size & BYTE_MASK);
        i++;

        /* Update the 3rd byte of the the Address Parameter*/
        p_data_param->data_addr[i] = (uint8_t)(read_addr & BYTE_MASK);

        status = phDnldNfc_Send_Command( psDnldContext, pHwRef,
                    PHDNLD_CMD_READ, NULL , 0 );

        if ( NFCSTATUS_PENDING == status )
        {
            p_sec_info->section_read = TRUE ;
            psDnldContext->next_dnld_state =  phDnld_Upgrade_State;
            DNLD_DEBUG(" FW_DNLD: Memory Read at Address %X : ", read_addr);
            DNLD_DEBUG(" of Size %X \n", read_size);
        }

    }
    return status;
}



static
NFCSTATUS
phDnldNfc_Process_Write(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef,
                        section_info_t          *p_sec_info,
                        uint32_t                *p_sec_offset
                     )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phDnldNfc_sData_t       *p_dnld_data =
                 (phDnldNfc_sData_t *)psDnldContext->dnld_data;
    phDnldNfc_sParam_t      *dnld_data =
                             &p_dnld_data->param_info.data_param;
    uint8_t                 *p_sm_trim_data = (uint8_t *)psDnldContext->
                                        dnld_resp.param_info.response_data;
    uint32_t                dnld_addr = 0;
#ifdef  NXP_FW_SW_VMID_TRIM
    uint32_t                trim_addr = 0;
#endif /* #ifdef NXP_FW_SW_VMID_TRIM */
    static unsigned         sec_type = 0;
    uint8_t                 i = 0;
    uint16_t                dnld_size = 0;
    int cmp_val = 0x00;


    sec_type = (unsigned int)p_sec_info->p_sec_hdr->section_mem_type;

    status = phDnldNfc_Read(psDnldContext, pHwRef, p_sec_info);
    if( NFCSTATUS_PENDING != status )
    {
        if( (TRUE == p_sec_info->trim_write)
            && (TRUE == p_sec_info->section_read)
               && ((sec_type & DNLD_VERIFY_MASK))
          )
        {
            if(NULL != psDnldContext->trim_store.buffer)
            {
                uint32_t  trim_cmp_size = psDnldContext->prev_dnld_size;

                if( p_sec_info->p_sec_hdr->section_address
                              < (DNLD_CFG_PG_ADDR + PHDNLD_PAGE_SIZE) )
                {
                    trim_cmp_size = trim_cmp_size - 2;
                }

                /* Below Comparison fails due to the checksum */
                 cmp_val = phOsalNfc_MemCompare(
                                psDnldContext->trim_store.buffer,
                                  &psDnldContext->dnld_resp.
                                       param_info.response_data[0]
                                          ,trim_cmp_size);
                DNLD_DEBUG(" FW_DNLD: %X Bytes Trim Write Complete ",
                                    psDnldContext->prev_dnld_size);
                DNLD_DEBUG(" Comparison Status %X\n", cmp_val);
            }
            p_sec_info->trim_write = FALSE;
            DNLD_DEBUG(" FW_DNLD: TRIMMED %X Bytes Write Complete\n", psDnldContext->prev_dnld_size);
        }
        else
        {
            if((NULL != psDnldContext->dnld_store.buffer)
                && ((sec_type & DNLD_VERIFY_MASK))
                && (TRUE == p_sec_info->section_write)
                && (TRUE == p_sec_info->section_read)
                )
            {
                cmp_val = phOsalNfc_MemCompare(
                             psDnldContext->dnld_store.buffer,
                               &psDnldContext->dnld_resp.
                                 param_info.response_data[0]
                                 ,psDnldContext->dnld_store.length);
                p_sec_info->section_read = FALSE;
                p_sec_info->section_write = FALSE;
                DNLD_DEBUG(" FW_DNLD: %X Bytes Write Complete ", 
                                    psDnldContext->dnld_store.length);
                DNLD_DEBUG(" Comparison Status %X\n", cmp_val);
            }
            else
            {
                if(( TRUE == p_sec_info->section_write)
                     && ( FALSE == p_sec_info->section_read)
                   )
                {
                  p_sec_info->section_write = FALSE;
                }
            }
            /* p_sec_info->section_read = FALSE; */
        }

        if (( 0 == psDnldContext->dnld_retry )
            && (0 == cmp_val)
            )
        {
            p_sec_info->sec_verify_retry = 0;
            p_sec_info->section_offset = p_sec_info->section_offset +
                            psDnldContext->prev_dnld_size;
            psDnldContext->prev_dnld_size = 0;
            DNLD_DEBUG(" FW_DNLD: Memory Write Retry - %X \n",
                                    psDnldContext->dnld_retry);
        }
        else
        {
            p_sec_info->sec_verify_retry++;
            DNLD_DEBUG(" FW_DNLD: Memory Verification Failed, Retry =  %X \n",
                               p_sec_info->sec_verify_retry);
        }

        if( p_sec_info->sec_verify_retry < NXP_MAX_SECTION_WRITE )
        {

            dnld_addr =  (p_sec_info->p_sec_hdr->section_address + *p_sec_offset);
            dnld_size = (uint16_t)(p_sec_info->p_sec_hdr->section_length
                                                            - *p_sec_offset);
        }
        else
        {
            status = NFCSTATUS_FAILED;
            DNLD_DEBUG(" FW_DNLD: Memory Verification - Maximum Limit, Retry =  %X \n",
                               p_sec_info->sec_verify_retry);
        }
    }


    if (dnld_size != 0)
    {

        dnld_size = (uint16_t)((PHDNLD_DATA_SIZE >= dnld_size)?
                                        dnld_size: PHDNLD_DATA_SIZE);

        /* Update the LSB of the Data and the Address Parameter*/
        dnld_data->data_addr[i] = (uint8_t)((dnld_addr  >>
                                  (BYTE_SIZE + BYTE_SIZE)) & BYTE_MASK);
        dnld_data->data_len[i] = (uint8_t)((dnld_size >> BYTE_SIZE)
                                        & BYTE_MASK);
        p_dnld_data->frame_length[i] = (uint8_t)
                    (((dnld_size + PHDNLD_CMD_WRITE_MIN_LEN) >> BYTE_SIZE)
                                        & BYTE_MASK);
        i++;
        /* Update the 2nd byte of the Data and the Address Parameter*/
        dnld_data->data_addr[i] = (uint8_t)((dnld_addr  >> BYTE_SIZE)
                                        & BYTE_MASK);
        dnld_data->data_len[i] = (uint8_t) (dnld_size & BYTE_MASK);
        p_dnld_data->frame_length[i] = (uint8_t) ((dnld_size +
                            PHDNLD_CMD_WRITE_MIN_LEN) & BYTE_MASK);
        i++;
        /* Update the 3rd byte of the the Address Parameter*/
        dnld_data->data_addr[i] = (uint8_t)(dnld_addr & BYTE_MASK);

        (void)memcpy( dnld_data->data_packet,
                    (p_sec_info->p_sec_data + *p_sec_offset), dnld_size );

        if( ((sec_type & DNLD_TRIM_MASK))
            && (p_sec_info->sec_verify_retry != 0)
            && (NULL != psDnldContext->trim_store.buffer)
            )
        {
            (void)memcpy( dnld_data->data_packet,
                        psDnldContext->trim_store.buffer, dnld_size );
        }
        else if(((sec_type & DNLD_TRIM_MASK))
            && ( TRUE ==  p_sec_info->section_read )
            )
        {
            for(i = 0; i < *(p_sec_info->p_trim_data);i++)
            {

#ifdef  NXP_FW_SW_VMID_TRIM

/*
if(bit 0 of 0x813D is equal to 1) then
   
   Do not overwrite 0x9931 / 0x9981 during download

else

   @0x9931 = 0x79 // card Mode
   @0x9981 = 0x79 // Reader Mode
*/
                trim_addr = p_sec_info->p_sec_hdr->section_address 
                                    + p_sec_info->p_trim_data[i+1];
                if (NXP_FW_VMID_TRIM_CHK_ADDR == trim_addr)
                {
                    psDnldContext->vmid_trim_update = 
                            p_sm_trim_data[p_sec_info->p_trim_data[i+1]] ;
                }

                if((NXP_FW_VMID_CARD_MODE_ADDR == trim_addr) 
                        || (NXP_FW_VMID_RD_MODE_ADDR == trim_addr))
                {
                    if (TRUE == psDnldContext->vmid_trim_update) 
                    {
                        dnld_data->data_packet[p_sec_info->p_trim_data[i+1]] =
                                p_sm_trim_data[p_sec_info->p_trim_data[i+1]] ;
                    }
                }
                else
                     
#endif
                {
                    dnld_data->data_packet[p_sec_info->p_trim_data[i+1]] =
                            p_sm_trim_data[p_sec_info->p_trim_data[i+1]] ;
                }
            }
            if(NULL != psDnldContext->trim_store.buffer)
            {
                phOsalNfc_FreeMemory(psDnldContext->trim_store.buffer);
                psDnldContext->trim_store.buffer = NULL;
                psDnldContext->trim_store.length = 0;
            }
#if 1
            (void)
                phDnldNfc_Allocate_Resource((void **)
                              &(psDnldContext->trim_store.buffer),dnld_size);
#else
            psDnldContext->trim_store.buffer =
                                (uint8_t *) phOsalNfc_GetMemory(dnld_size);
#endif

            if(NULL != psDnldContext->trim_store.buffer)
            {
                (void )memset((void *)psDnldContext->trim_store.buffer,0,
                                            dnld_size);
                (void)memcpy( psDnldContext->trim_store.buffer,
                                dnld_data->data_packet,  dnld_size );
                psDnldContext->trim_store.length = dnld_size;
                DNLD_DEBUG(" FW_DNLD: Write with Trimming at Address %X ", dnld_addr );
                DNLD_DEBUG(" of Size %X and ", dnld_size );
                DNLD_DEBUG(" with %X Trimming Values \n", *(p_sec_info->p_trim_data) );

            }
        }
        else
        {
            if(NULL != psDnldContext->dnld_store.buffer)
            {
                phOsalNfc_FreeMemory(psDnldContext->dnld_store.buffer);
                psDnldContext->dnld_store.buffer = NULL;
                psDnldContext->dnld_store.length = 0;
            }
#if 1
            (void)
                phDnldNfc_Allocate_Resource((void **)
                              &(psDnldContext->dnld_store.buffer),dnld_size);
#else
            psDnldContext->dnld_store.buffer =
                                (uint8_t *) phOsalNfc_GetMemory(dnld_size);
#endif
            if(NULL != psDnldContext->dnld_store.buffer)
            {
                (void )memset((void *)psDnldContext->dnld_store.buffer,0,
                                            dnld_size);
                (void)memcpy( psDnldContext->dnld_store.buffer,
                                dnld_data->data_packet,  dnld_size );
                psDnldContext->dnld_store.length = dnld_size;
                DNLD_DEBUG(" FW_DNLD: Memory Write at Address %X ", dnld_addr );
                DNLD_DEBUG(" of Size %X ", dnld_size );
            }
        }

        if(PHDNLD_FW_PATCH_SEC !=  psDnldContext->p_fw_hdr->fw_patch)
        {
        status = phDnldNfc_Send_Command( psDnldContext, pHwRef,
                    PHDNLD_CMD_WRITE, NULL , 0 );
        }
        else
        {
            status = phDnldNfc_Send_Command( psDnldContext, pHwRef,
                        PHDNLD_CMD_SEC_WRITE, NULL , 0 );
        }

        DNLD_DEBUG(" FW_DNLD: Memory Write Status = %X \n", status);
        if ( NFCSTATUS_PENDING == status )
        {
            psDnldContext->prev_dnld_size = dnld_size;
            cmp_val = 0x00;
            if((sec_type & DNLD_TRIM_MASK))
            {
                p_sec_info->trim_write = TRUE;
                DNLD_DEBUG(" FW_DNLD: Bytes Downloaded (Trimming Values) = %X Bytes \n",
                                                        dnld_size);
            }
            else
            {
                p_sec_info->section_write = TRUE;
                DNLD_DEBUG(" FW_DNLD: Bytes Downloaded  = %X : ",
                                        (*p_sec_offset + dnld_size));
                DNLD_DEBUG(" Bytes Remaining  = %X \n",
                        (p_sec_info->p_sec_hdr->section_length -
                                        (*p_sec_offset + dnld_size)));
            }

            p_sec_info->section_read = FALSE;
        }
    }
    return status;
}



static
NFCSTATUS
phDnldNfc_Resume_Write(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef
                     )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    uint8_t                 sec_index = psDnldContext->section_index;
    section_info_t         *p_sec_info = (psDnldContext->p_fw_sec + sec_index);

    while((sec_index < psDnldContext->p_fw_hdr->no_of_sections)
        && (NFCSTATUS_SUCCESS == status )
        )
    {

        status =  phDnldNfc_Process_Write(psDnldContext, pHwRef,
                p_sec_info, (uint32_t *)&(p_sec_info->section_offset));
        if (NFCSTATUS_SUCCESS == status)
        {
            unsigned sec_type = 0;
            sec_type = (unsigned int)p_sec_info->p_sec_hdr->section_mem_type;

            p_sec_info->section_offset = 0;
            p_sec_info->section_read = FALSE;
            p_sec_info->section_write = FALSE;
            p_sec_info->trim_write = FALSE;

            DNLD_DEBUG(" FW_DNLD: Section %02X Download Complete\n", sec_index);
            if((sec_type & DNLD_RESET_MASK))
            {
                DNLD_DEBUG(" FW_DNLD: Reset After Section %02X Download \n", sec_index);
                status = phDnldNfc_Send_Command( psDnldContext, pHwRef,
                            PHDNLD_CMD_RESET , NULL, 0 );
            }
            DNLD_PRINT("*******************************************\n\n");

            sec_index++;

#ifdef NXP_FW_DNLD_CHECK_PHASE
            if( p_sec_info->p_sec_hdr->section_address
                               < (DNLD_CFG_PG_ADDR + PHDNLD_PAGE_SIZE) )
            {
                gphDnldPhase = NXP_FW_DNLD_DATA_PHASE;

            }

            p_sec_info = (psDnldContext->p_fw_sec + sec_index);

            if( (sec_index < psDnldContext->p_fw_hdr->no_of_sections)
                && ( p_sec_info->p_sec_hdr->section_address
                               < (DNLD_CFG_PG_ADDR + PHDNLD_PAGE_SIZE) )
               )
            {
                 if( NXP_FW_DNLD_CFG_PHASE >= gphDnldPhase )
                 {
                    gphDnldPhase = NXP_FW_DNLD_CFG_PHASE;
                 }
                 else
                 {
                   sec_index++;
                   p_sec_info = (psDnldContext->p_fw_sec + sec_index);
                 }
            }
#else
            p_sec_info = (psDnldContext->p_fw_sec + sec_index);
#endif /* #ifdef NXP_FW_DNLD_CHECK_PHASE */

            psDnldContext->section_index = sec_index;
        /* psDnldContext->next_dnld_state = (uint8_t) phDnld_Upgrade_State; */
        }
    }
    if (NFCSTATUS_PENDING == status)
    {
        psDnldContext->next_dnld_state = (uint8_t) phDnld_Upgrade_State;
    }
    else if (NFCSTATUS_SUCCESS == status)
    {
        /* Reset the PN544 Device */
        psDnldContext->next_dnld_state = (uint8_t) phDnld_Complete_State;
    }
    else
    {

    }
    return status;
}


#define NXP_DNLD_SM_UNLOCK_ADDR         0x008002U

#if !defined (ES_HW_VER)
#define ES_HW_VER 32
#endif

#if (ES_HW_VER <= 30)
#define NXP_DNLD_PATCH_ADDR             0x01AFFFU
#else
#define NXP_DNLD_PATCH_ADDR             0x01A1E0U
#endif

#if  (ES_HW_VER <= 30)
#define NXP_DNLD_PATCH_TABLE_ADDR       0x008107U
#else
#define NXP_DNLD_PATCH_TABLE_ADDR       0x00825AU
#endif


static
NFCSTATUS
phDnldNfc_Sequence(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef,
                        void                    *pdata,
                        uint16_t                length
                        )
{
    NFCSTATUS           status = NFCSTATUS_SUCCESS;
    uint32_t            dnld_addr = 0;
    phDnldNfc_sData_t       *p_dnld_data =
                 (phDnldNfc_sData_t *)psDnldContext->dnld_data;
    phDnldNfc_sParam_t  *p_data_param =
                          & p_dnld_data->param_info.data_param;
    uint8_t             *p_data = NULL;
    static  uint32_t    patch_size = 0;

#if (ES_HW_VER == 32)

    static  uint8_t     patch_table[] = {0xA0, 0xA1, 0xE0, 0x80, 0xA9, 0x6C };
    static  uint8_t     patch_data[] = {0xA5, 0xD0, 0xFE, 0xA5, 0xD0, 0xFD, 0xA5,
                            0xD0, 0xFC, 0xA5, 0x02, 0x80, 0xA9, 0x75};

#elif (ES_HW_VER == 31)

    static  uint8_t     patch_table[] = {0xA0, 0xAF, 0xE0, 0x80, 0x78, 0x84 };
    static  uint8_t     patch_data[] = {0xA5, 0xD0, 0xFE, 0xA5, 0xD0, 0xFD, 0xA5,
                            0xD0, 0xFC, 0xD0, 0xE0, 0xA5, 0x02, 0x80, 0x78, 0x8D};

#elif (ES_HW_VER == 30)

    static  uint8_t     patch_table[] = {0x80, 0x91, 0x51, 0xA0, 0xAF,
                                                0xFF, 0x80, 0x91, 0x5A};
    static  uint8_t     patch_data[] = {0x22};

#endif

    static  uint8_t     unlock_data[] = {0x00, 0x00};
    static  uint8_t     lock_data[] = {0x0C, 0x00};

    uint8_t             i = 0;

    PHNFC_UNUSED_VARIABLE(pdata);
    PHNFC_UNUSED_VARIABLE(length);
    switch(psDnldContext->cur_dnld_seq)
    {
        case phDnld_Reset_Seq:
        {
            status = phDnldNfc_Send_Command( psDnldContext, pHwRef,
                        PHDNLD_CMD_RESET , NULL , 0 );
            /* status = (NFCSTATUS_PENDING == status)? NFCSTATUS_SUCCESS:
                                 status; */
            DNLD_DEBUG(" FW_DNLD: Reset Seq.. Status = %X \n", status);

            break;
        }
        case phDnld_Activate_Patch:
        {
            uint8_t     patch_activate = 0x01;
            psDnldContext->next_dnld_seq =
                            (uint8_t)phDnld_Update_Patch;
#ifdef NXP_FW_DNLD_CHECK_PHASE
            gphDnldPhase = NXP_FW_DNLD_SYSTEM_PHASE;
#endif /* NXP_FW_DNLD_CHECK_PHASE */

            status = phDnldNfc_Send_Command( psDnldContext, pHwRef,
                PHDNLD_CMD_ACTIVATE_PATCH , &patch_activate, sizeof(patch_activate) );
            DNLD_PRINT(" FW_DNLD: Activate the Patch Update .... \n");
            break;
        }
        case phDnld_Deactivate_Patch:
        {
            uint8_t     patch_activate = 0x00;

            psDnldContext->next_dnld_state =
                            (uint8_t)phDnld_Reset_State;

            status = phDnldNfc_Send_Command( psDnldContext, pHwRef,
                PHDNLD_CMD_ACTIVATE_PATCH , &patch_activate, sizeof(patch_activate) );
            DNLD_PRINT(" FW_DNLD: Deactivate the Patch Update .... \n");
            break;
        }
        case phDnld_Update_Patch:
        {
            dnld_addr = NXP_DNLD_PATCH_ADDR;
            patch_size = sizeof(patch_data) ;
            p_data = patch_data;
            psDnldContext->next_dnld_seq =
                            (uint8_t)phDnld_Update_Patchtable;
            DNLD_PRINT(" FW_DNLD: Patch Update Seq.... \n");
            break;
        }
        case phDnld_Update_Patchtable:
        {
            dnld_addr = NXP_DNLD_PATCH_TABLE_ADDR;
            patch_size = sizeof(patch_table) ;
            p_data = patch_table;

            psDnldContext->next_dnld_state =
                            (uint8_t)phDnld_Reset_State;

            DNLD_PRINT(" FW_DNLD: Patch Table Update Seq.... \n");
            break;
        }
        case phDnld_Unlock_System:
        {
            dnld_addr = NXP_DNLD_SM_UNLOCK_ADDR;
            patch_size = sizeof(unlock_data) ;
            p_data = unlock_data;
#define NXP_FW_PATCH_DISABLE
#ifdef NXP_FW_PATCH_DISABLE
            psDnldContext->next_dnld_seq =
                            (uint8_t)phDnld_Deactivate_Patch;
#else
            psDnldContext->next_dnld_state =
                            (uint8_t)phDnld_Reset_State;
#endif

            DNLD_PRINT(" FW_DNLD: System Memory Unlock Seq.... \n");
            break;
        }
        case phDnld_Lock_System:
        {
            dnld_addr = NXP_DNLD_SM_UNLOCK_ADDR;
            patch_size = sizeof(lock_data) ;
            p_data = lock_data;
            psDnldContext->next_dnld_state =
                            (uint8_t) phDnld_Reset_State;

            DNLD_PRINT(" FW_DNLD: System Memory Lock Seq.... \n");
            break;
        }
        case phDnld_Upgrade_Section:
        {
            status = phDnldNfc_Resume_Write(
                        psDnldContext, pHwRef );
            break;
        }
        case phDnld_Verify_Integrity:
        {
            psDnldContext->next_dnld_state =
                            (uint8_t) phDnld_Reset_State;

            status = phDnldNfc_Send_Command( psDnldContext, pHwRef,
                PHDNLD_CMD_CHECK_INTEGRITY , NULL, 0 );
            DNLD_PRINT(" FW_DNLD: System Memory Integrity Check Sequence.... \n");
            break;
        }
        case phDnld_Verify_Section:
        {
            break;
        }
        default:
        {
            break;
        }
    }

    if( NFCSTATUS_SUCCESS == status)
    {

        /* Update the LSB of the Data and the Address Parameter*/
        p_data_param->data_addr[i] = (uint8_t)((dnld_addr  >>
                                            (BYTE_SIZE + BYTE_SIZE))
                                                    & BYTE_MASK);
        p_data_param->data_len[i] = (uint8_t)((patch_size >> BYTE_SIZE)
                                                    & BYTE_MASK);
        p_dnld_data->frame_length[i] = (uint8_t)
                    (((patch_size + PHDNLD_CMD_WRITE_MIN_LEN) >> BYTE_SIZE)
                                                    & BYTE_MASK);
        i++;
        /* Update the 2nd byte of the Data and the Address Parameter*/
        p_data_param->data_addr[i] = (uint8_t)((dnld_addr  >> BYTE_SIZE)
                                                & BYTE_MASK);
        p_data_param->data_len[i] = (uint8_t) (patch_size & BYTE_MASK);
        p_dnld_data->frame_length[i] = (uint8_t)
                            ((patch_size + PHDNLD_CMD_WRITE_MIN_LEN)
                                                    & BYTE_MASK);
        i++;
        /* Update the 3rd byte of the the Address Parameter*/
        p_data_param->data_addr[i] = (uint8_t)(dnld_addr & BYTE_MASK);

        status = phDnldNfc_Send_Command( psDnldContext, pHwRef,
                    PHDNLD_CMD_WRITE,(void *)p_data , (uint8_t)patch_size );

        if (NFCSTATUS_PENDING != status)
        {
             status = phDnldNfc_Set_Seq(psDnldContext,
                                            DNLD_SEQ_ROLLBACK);
        }
    }
    return status;
}

#define FRAME_HEADER_LEN   0x03U


static
void
phDnldNfc_Tx_Reset(phDnldNfc_sContext_t    *psDnldContext)
{
    psDnldContext->tx_info.transmit_frame = NULL;
    psDnldContext->tx_info.tx_total = 0x00;
    psDnldContext->tx_info.tx_offset = 0x00;
    psDnldContext->tx_info.tx_len = 0x00;
    psDnldContext->tx_info.tx_chain = FALSE;
}

STATIC
bool_t
phDnldNfc_Extract_Chunks(
                       uint8_t  *frame_data,
                       uint16_t  frame_offset,
                       uint16_t  frame_length,
                       uint16_t  max_frame ,
                       uint16_t  *chunk_length
                       );


STATIC
bool_t
phDnldNfc_Extract_Chunks(
                       uint8_t  *frame_data,
                       uint16_t  frame_offset,
                       uint16_t  frame_length,
                       uint16_t  max_frame ,
                       uint16_t  *chunk_length
                       )
{
    bool_t  chunk_present = FALSE;

    if( 0 == frame_offset)
    {
        if( max_frame >= (frame_length
                - frame_offset))
        {
           *chunk_length = (frame_length - frame_offset);
        }
        else
        {
            *chunk_length = max_frame
                            - FRAME_HEADER_LEN;
            chunk_present = TRUE;
        }
    }
    else
    {
        if( max_frame >= (frame_length
                - frame_offset))
        {
           *chunk_length = (frame_length - frame_offset);
        }
        else
        {
            *chunk_length = max_frame
                            - FRAME_HEADER_LEN;
            chunk_present = TRUE;
        }
    }

    return chunk_present;
}


STATIC
NFCSTATUS
phDnldNfc_Send_Raw(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef,
                        uint8_t                 *raw_frame,
                        uint16_t                frame_offset,
                        uint16_t                frame_length
                      )
{
	NFCSTATUS status = NFCSTATUS_SUCCESS;
	phDnldNfc_sRawHdr_t *raw_frame_hdr = ( phDnldNfc_sRawHdr_t * ) raw_frame;

    switch(raw_frame_hdr->frame_type)
	{
        case PHDNLD_CMD_RESET:
        {
            break;
        }
        case PHDNLD_CMD_READ:
        {
            /* TODO: To Update the length and the buffer to receive data */
            break;
        }
        case PHDNLD_CMD_WRITE:
        {
			phDnldNfc_sRawDataHdr_t *raw_data_hdr =
				( phDnldNfc_sRawDataHdr_t * ) (raw_frame + FRAME_HEADER_LEN);

            psDnldContext->resp_length = PHDNLD_MIN_PACKET;

            break;
        }
        case PHDNLD_CMD_SEC_WRITE:
        {
            uint16_t    tx_length = 0x00;
            uint16_t    frame_offset =
                          psDnldContext->tx_info.tx_offset;
            uint16_t    chain =
                    psDnldContext->tx_info.tx_chain;

            chain =
            phDnldNfc_Extract_Chunks(
                         raw_frame,
                         frame_offset,
                         frame_length,
                         PHDNLD_FW_TX_RX_LEN,
                         &tx_length
                       );

            if( TRUE == chain )
            {
                status = phDnldNfc_Send_Command( psDnldContext,
                                    pHwRef, PHDNLD_CMD_ENCAPSULATE,
                                    (raw_frame + frame_offset),
                                    tx_length);
                if(NFCSTATUS_PENDING == status)
                {
                    psDnldContext->prev_cmd = raw_frame_hdr->frame_type;
                    /* TODO: Update for the Chaining */
                    psDnldContext->tx_info.tx_offset += tx_length;
                    psDnldContext->tx_info.tx_chain = chain;
                }
            }
            else if (0 != frame_offset)
            {
                status = phDnldNfc_Send_Command( psDnldContext,
                                    pHwRef, PHDNLD_CMD_ENCAPSULATE,
                                    (raw_frame + frame_offset),
                                    tx_length);
                if(NFCSTATUS_PENDING == status)
                {
                    psDnldContext->prev_cmd = raw_frame_hdr->frame_type;
                    /* TODO: Update for the Chaining */
                    psDnldContext->prev_dnld_size = frame_length;
                    phDnldNfc_Tx_Reset(psDnldContext);
                }
            }
            else
            {
			    phDnldNfc_sRawDataHdr_t *raw_data_hdr =
				    ( phDnldNfc_sRawDataHdr_t * ) (raw_frame + FRAME_HEADER_LEN);
                psDnldContext->resp_length = PHDNLD_MIN_PACKET;
            }

            break;
        }
        case PHDNLD_CMD_CHECK:
        {
            psDnldContext->resp_length = PHDNLD_MIN_PACKET;
            break;
        }
        case PHDNLD_CMD_SET_HIF:
        {
            psDnldContext->resp_length = PHDNLD_MIN_PACKET;
            break;
        }
        case PHDNLD_CMD_ACTIVATE_PATCH:
        {
            psDnldContext->resp_length = PHDNLD_MIN_PACKET;
            break;
        }
        case PHDNLD_CMD_CHECK_INTEGRITY:
        {
			uint8_t integrity_param =
				 *(raw_frame + FRAME_HEADER_LEN);
            switch(integrity_param)
            {
                case CHK_INTEGRITY_CONFIG_PAGE_CRC:
                case CHK_INTEGRITY_PATCH_TABLE_CRC:
                {
                    psDnldContext->resp_length = PHDNLD_MIN_PACKET
                                         + CHECK_INTEGRITY_RESP_CRC16_LEN;
                    break;
                }
                case CHK_INTEGRITY_FLASH_CODE_CRC:
                case CHK_INTEGRITY_PATCH_CODE_CRC:
                {
                    psDnldContext->resp_length = PHDNLD_MIN_PACKET
                                        +  CHECK_INTEGRITY_RESP_CRC32_LEN;
                    break;
                }
                case CHK_INTEGRITY_COMPLETE_CRC:
                default:
                {
                    psDnldContext->resp_length = PHDNLD_MIN_PACKET
                                        +  CHECK_INTEGRITY_RESP_COMP_LEN;
                    break;
                }
            }
            break;
        }
        default:
        {
            status = PHNFCSTVAL(CID_NFC_DNLD, NFCSTATUS_FEATURE_NOT_SUPPORTED);
            break;
        }
    }

    if (NFCSTATUS_SUCCESS == status)
    {
        status = phDnldNfc_Send( psDnldContext, pHwRef ,
                            raw_frame, frame_length);

        if(NFCSTATUS_PENDING == status)
        {
            psDnldContext->prev_cmd = raw_frame_hdr->frame_type;
            /* TODO: Update for the Chaining */
            psDnldContext->prev_dnld_size = frame_length;
        }
    }

    return status;
}


static
NFCSTATUS
phDnldNfc_Frame_Complete(phDnldNfc_sContext_t *psDnldContext)
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phDnldNfc_sData_Hdr_t   *p_dnld_raw = NULL;
    uint32_t                dnld_index = psDnldContext->dnld_index;
    uint8_t                 *p_raw_sec_hdr = NULL;
    uint16_t                tx_length = 0x00;

    dnld_index = dnld_index + psDnldContext->prev_dnld_size;
    p_raw_sec_hdr = psDnldContext->p_fw_raw + dnld_index;
    dnld_index = dnld_index + *p_raw_sec_hdr;

    p_dnld_raw = (phDnldNfc_sData_Hdr_t *) (psDnldContext->p_fw_raw +
                                              psDnldContext->dnld_index);

    tx_length = ((p_dnld_raw->frame_length[0] << BYTE_SIZE) |
                            p_dnld_raw->frame_length[1]);

    tx_length = tx_length + PHDNLD_MIN_PACKET;

    return status;
}


static
NFCSTATUS
phDnldNfc_Raw_Write(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef
                     )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    uint32_t                dnld_index = psDnldContext->dnld_index;
    uint32_t                tx_length = 0;
    uint8_t                 *p_raw_sec_hdr = NULL;
    uint8_t                 dnld_flag = FALSE;
    uint8_t                 skip_frame = FALSE;

    if(NULL != psDnldContext->p_fw_raw)
    {

        if( (TRUE != psDnldContext->tx_info.tx_chain)
            && (0x00 == psDnldContext->dnld_retry)
          )
        {
            dnld_index = dnld_index + psDnldContext->prev_dnld_size;
            p_raw_sec_hdr = psDnldContext->p_fw_raw + dnld_index;
            dnld_index = dnld_index + *p_raw_sec_hdr;
        }
        else
        {
            phDnldNfc_sData_Hdr_t *p_dnld_raw = (phDnldNfc_sData_Hdr_t *)
									(psDnldContext->p_fw_raw +
                                              psDnldContext->dnld_index);

            tx_length = ((p_dnld_raw->frame_length[0] << BYTE_SIZE) |
                                    p_dnld_raw->frame_length[1]);

            tx_length = tx_length + PHDNLD_MIN_PACKET;

            status = phDnldNfc_Send_Raw( psDnldContext, pHwRef,
                            (uint8_t *)(p_dnld_raw),
                            psDnldContext->tx_info.tx_offset,
                                (uint16_t)tx_length);
        }


#define PHDNLD_MAJOR_OFFSET        0x04U
#define PHDNLD_MINOR_OFFSET        0x05U
#define PHDNLD_PHASE_OFFSET        0x06U
#define PHDNLD_FRAMETYPE_OFFSET    0x07U

#define PHDNLD_NO_OPERATION        0x00U
#define PHDNLD_NORMAL_OPERATION    0x10U
#define PHDNLD_ADVANCED_OPERATION  0x20U
#define PHDNLD_SETUP_OPERATION	   0x40U
#define PHDNLD_RECOVER_OPERATION   0x80U
#define PHDNLD_COMPLETE_OPERATION  0xF0U

#define PHDNLD_TERMINATE_TYPE      0x0EU

#define PHDNLD_MARKER_MASK         0x0FU

        while((NFCSTATUS_SUCCESS == status )
                && (FALSE == dnld_flag)
            )
       {
            phDnldNfc_sData_Hdr_t *p_dnld_raw = (phDnldNfc_sData_Hdr_t *)
												(psDnldContext->p_fw_raw + dnld_index);
            uint8_t               frame_type = *(p_raw_sec_hdr + PHDNLD_FRAMETYPE_OFFSET);

            tx_length = ((p_dnld_raw->frame_length[0] << BYTE_SIZE) |
                                    p_dnld_raw->frame_length[1]);

            tx_length = tx_length + PHDNLD_MIN_PACKET;

            skip_frame = FALSE;

            if(  (0x00 == *(p_raw_sec_hdr + PHDNLD_PHASE_OFFSET))
                    || (0xFF == *(p_raw_sec_hdr + PHDNLD_PHASE_OFFSET))
                    || !( psDnldContext->raw_mode_upgrade
                     & (frame_type & (~PHDNLD_MARKER_MASK)) )
                     )
            {
                dnld_index = dnld_index + tx_length;
                p_raw_sec_hdr = psDnldContext->p_fw_raw + dnld_index;
                dnld_index = dnld_index + *p_raw_sec_hdr;
                skip_frame = TRUE;
            }
            if (PHDNLD_TERMINATE_TYPE ==
                        (frame_type & PHDNLD_MARKER_MASK))
            {
                if(TRUE != skip_frame)
	        {
                   psDnldContext->raw_mode_upgrade = 
                       (psDnldContext->raw_mode_upgrade &
                              ~(frame_type & ~PHDNLD_MARKER_MASK));
		}

                if(PHDNLD_NO_OPERATION ==
                        psDnldContext->raw_mode_upgrade)
                {
                   dnld_flag = TRUE;
                }
            }
            else
            {

            }

            if((FALSE == skip_frame)
                && (FALSE == dnld_flag)
                )
            {
                status = phDnldNfc_Send_Raw( psDnldContext, pHwRef,
                               (uint8_t *)(p_dnld_raw),
                               psDnldContext->tx_info.tx_offset,
                                    (uint16_t)tx_length);
            }

            if( NFCSTATUS_PENDING == status )
            {
                psDnldContext->dnld_index = dnld_index;
				psDnldContext->cur_frame_info= frame_type;
            }
        }
    }

    return status;
}

static
NFCSTATUS
phDnldNfc_Upgrade_Sequence(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef,
                        void                    *pdata,
                        uint16_t                length
                        )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;

    PHNFC_UNUSED_VARIABLE(pdata);
    PHNFC_UNUSED_VARIABLE(length);

    if(phDnld_Raw_Upgrade == psDnldContext->cur_dnld_seq)
    {
       status = phDnldNfc_Raw_Write( psDnldContext, pHwRef );
    }
    else
    {
       status = phDnldNfc_Resume_Write( psDnldContext, pHwRef );
    }

    return status;
}



static
NFCSTATUS
phDnldNfc_Resume(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef,
                        void                    *pdata,
                        uint16_t                length
                     )
{
    NFCSTATUS             status = NFCSTATUS_SUCCESS;
    phDnldNfc_eState_t    dnld_next_state = (phDnldNfc_eState_t)
                                    psDnldContext->cur_dnld_state;
    phNfc_sCompletionInfo_t comp_info = {0,0,0};

    switch( dnld_next_state )
    {
        case phDnld_Reset_State:
        {
            status = phDnldNfc_Send_Command( psDnldContext, pHwRef,
                        PHDNLD_CMD_RESET , NULL, 0 );
            switch( psDnldContext->cur_dnld_seq )
            {
                case phDnld_Update_Patchtable:
                {
                    psDnldContext->next_dnld_state =
                                    (uint8_t)phDnld_Unlock_State;
                    psDnldContext->next_dnld_seq =
                                    (uint8_t)phDnld_Unlock_System;
                    break;
                }
#ifdef NXP_FW_PATCH_DISABLE
                case phDnld_Deactivate_Patch:
#else
                case phDnld_Unlock_System:
#endif
                {
                    psDnldContext->next_dnld_state =
                                   (uint8_t)phDnld_Upgrade_State;
                    psDnldContext->next_dnld_seq =
                                    (uint8_t)phDnld_Upgrade_Section;
#ifdef NXP_FW_DNLD_CHECK_PHASE
                    gphDnldPhase = NXP_FW_DNLD_CFG_PHASE;
#endif /* NXP_FW_DNLD_CHECK_PHASE */
                    break;
                }
                case phDnld_Lock_System:
                {
#if  (NXP_FW_INTEGRITY_CHK >= 0x01)
                    psDnldContext->next_dnld_state =
                                    (uint8_t)phDnld_Verify_State;
                    psDnldContext->next_dnld_seq =
                                    (uint8_t)phDnld_Verify_Integrity;
#else
                    /* (void ) memset( (void *) &psDnldContext->chk_integrity_crc,
                                0, sizeof(psDnldContext->chk_integrity_crc)); */
                    psDnldContext->next_dnld_state =
                                            (uint8_t) phDnld_Complete_State;
#endif /* #if  (NXP_FW_INTEGRITY_CHK >= 0x01) */
                    break;
                }
                case phDnld_Verify_Integrity:
                {
                    psDnldContext->next_dnld_state =
                                            (uint8_t) phDnld_Complete_State;
                    break;
                }
                default:
                {
                    status = (NFCSTATUS_PENDING == status)?
                            NFCSTATUS_SUCCESS: status;
                    break;
                }
            }
            break;
        }
        case phDnld_Unlock_State:
        {

            status = phDnldNfc_Sequence( psDnldContext, pHwRef,
                                                            pdata, length);
            break;
        }
        case phDnld_Upgrade_State:
        {
            status = phDnldNfc_Upgrade_Sequence( psDnldContext, pHwRef,
                                                            pdata, length);
            if ((NFCSTATUS_SUCCESS == status )
                && (phDnld_Complete_State == psDnldContext->next_dnld_state))
            {
#if 0
                psDnldContext->cur_dnld_seq =
                                    (uint8_t)phDnld_Lock_System;
                psDnldContext->next_dnld_seq =
                                    psDnldContext->cur_dnld_seq;
#endif
#if  (NXP_FW_INTEGRITY_CHK >= 0x01)
                psDnldContext->next_dnld_state =
                                (uint8_t)phDnld_Verify_State;
                psDnldContext->next_dnld_seq =
                                (uint8_t)phDnld_Verify_Integrity;
                psDnldContext->cur_dnld_seq =
                                    psDnldContext->next_dnld_seq;
                status = phDnldNfc_Sequence( psDnldContext,
                                                        pHwRef, pdata, length);
#else
                /* (void ) memset( (void *) &psDnldContext->chk_integrity_crc,
                            0, sizeof(psDnldContext->chk_integrity_crc)); */
                psDnldContext->next_dnld_state =
                                        (uint8_t) phDnld_Complete_State;
#endif /* #if  (NXP_FW_INTEGRITY_CHK >= 0x01) */
            }
            break;
        }
        case phDnld_Verify_State:
        {
            status = phDnldNfc_Sequence( psDnldContext,
                                                 pHwRef, pdata, length);
            break;
        }
        case phDnld_Complete_State:
        {
            uint8_t integrity_chk = 0xA5;

#if  (NXP_FW_INTEGRITY_CHK >= 0x01)
            uint8_t verify_crc = 0x96;

            if ( (NULL != psDnldContext->p_flash_code_crc) 
                  && (NULL != psDnldContext->p_patch_code_crc) 
                   && (NULL != psDnldContext->p_patch_table_crc) 
                  )
            {
                uint8_t     crc_i = 0;
                uint16_t    patch_table_crc = 0;
                uint32_t    flash_code_crc = 0;
                uint32_t    patch_code_crc = 0;

                for (crc_i = 0; crc_i < DNLD_CRC32_SIZE; crc_i++ )
                {
                    if (crc_i < DNLD_CRC16_SIZE )
                    {
                        patch_table_crc = patch_table_crc
                            | psDnldContext->chk_integrity_crc.patch_table.Chk_Crc16[crc_i]
                                    << (crc_i * BYTE_SIZE)  ;
                    }
                    flash_code_crc  = flash_code_crc
                        | psDnldContext->chk_integrity_crc.flash_code.Chk_Crc32[crc_i]
                                << (crc_i * BYTE_SIZE)  ;
                    patch_code_crc  = patch_code_crc
                        | psDnldContext->chk_integrity_crc.patch_code.Chk_Crc32[crc_i]
                                << (crc_i * BYTE_SIZE)  ;
                }
                verify_crc =(uint8_t)( (*((uint32_t *) psDnldContext->p_flash_code_crc)) != 
                          flash_code_crc );
                verify_crc |=(uint8_t)( (*((uint32_t *) psDnldContext->p_patch_code_crc)) != 
                          patch_code_crc );
                verify_crc |=(uint8_t)( (*((uint16_t *) psDnldContext->p_patch_table_crc)) != 
                          patch_table_crc );
            }
            else
            {
                DNLD_PRINT(" FW_DNLD: Flash, Patch code and Patch Table CRC ");
                DNLD_PRINT(" Not Available in the Firmware \n");
            }

#endif /* #if  (NXP_FW_INTEGRITY_CHK >= 0x01) */

            integrity_chk = psDnldContext->chk_integrity_crc.config_page.Chk_status + 
                        psDnldContext->chk_integrity_crc.patch_table.Chk_status +
                        psDnldContext->chk_integrity_crc.flash_code.Chk_status +
                        psDnldContext->chk_integrity_crc.patch_code.Chk_status;

            if ( ( 0 != integrity_chk ) 
#if  (NXP_FW_INTEGRITY_CHK >= 0x01)
                || ( 0 != verify_crc )
#endif /* #if  (NXP_FW_INTEGRITY_CHK >= 0x01) */
                )
            {
                status = PHNFCSTVAL(CID_NFC_DNLD, NFCSTATUS_FAILED);
            }
            break;
        }
        default:
        {
            status = PHNFCSTVAL(CID_NFC_DNLD, NFCSTATUS_FAILED);
            break;
        }
    }

    if (NFCSTATUS_PENDING == status)
    {
        /* Write/Receive is still pending */
    }
    else
    {
        pphNfcIF_Notification_CB_t  p_upper_notify =
                        psDnldContext->p_upper_notify;
        void                        *p_upper_context =
                        psDnldContext->p_upper_context;

        DNLD_DEBUG(" FW_DNLD: Resume Termination Status = %X \n", status);

        comp_info.status = status;

        (void) phDal4Nfc_Unregister(
                            psDnldContext->lower_interface.pcontext, pHwRef);
        phDnldNfc_Release_Lower(psDnldContext, pHwRef);
        phDnldNfc_Release_Resources(&psDnldContext);
#ifndef NFC_TIMER_CONTEXT
        gpphDnldContext = psDnldContext;
#endif
        /* Notify the Error/Success Scenario to the upper layer */
        phDnldNfc_Notify( p_upper_notify, p_upper_context, pHwRef, (uint8_t)
            ((NFCSTATUS_SUCCESS == comp_info.status )? NFC_IO_SUCCESS: NFC_IO_ERROR),
                    &comp_info );
    }
    return status;
}

STATIC
NFCSTATUS
phDnldNfc_Process_Response(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef,
                        void                    *pdata,
                        uint16_t                length
                     )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS;
    phDnldNfc_sData_Hdr_t   *resp_data =
                        (phDnldNfc_sData_Hdr_t *) pdata;

    PHNFC_UNUSED_VARIABLE(pHwRef);
    DNLD_DEBUG(" FW_DNLD: Receive Length = %X \n", length );
    if(( psDnldContext->rx_info.rx_total == 0 )
        && (PHDNLD_MIN_PACKET <= length)
        )
    {
        psDnldContext->rx_info.rx_total =
            ((uint16_t)resp_data->frame_length[0] << BYTE_SIZE)|
                        resp_data->frame_length[1];
        if( psDnldContext->rx_info.rx_total + PHDNLD_MIN_PACKET == length )
        {

            DNLD_DEBUG(" FW_DNLD: Success Memory Read = %X \n",
                                                psDnldContext->rx_info.rx_total);
#ifndef DNLD_SUMMARY
            /* DNLD_PRINT_BUFFER("Receive Buffer",pdata,length); */
#endif

        }
        else
        {
           /* status = phDnldNfc_Receive( psDnldContext, pHwRef,
                psDnldContext->p_resp_buffer,
               (uint8_t)((psDnldContext->rx_info.rx_total <= PHDNLD_MAX_PACKET)?
                    psDnldContext->rx_info.rx_total: PHDNLD_MAX_PACKET) ); */
            DNLD_PRINT(" FW_DNLD: Invalid Receive length ");
            DNLD_DEBUG(": Length Expected = %X \n",
            (psDnldContext->rx_info.rx_total + PHDNLD_MIN_PACKET));
            status = PHNFCSTVAL( CID_NFC_DNLD,
                                    NFCSTATUS_INVALID_RECEIVE_LENGTH );
        }
    }
    else
    {
        /*TODO:*/
        psDnldContext->rx_info.rx_total = 0 ;
        status = PHNFCSTVAL( CID_NFC_DNLD,
                                NFCSTATUS_INVALID_RECEIVE_LENGTH );
    }

    return status;
}



STATIC
void
phDnldNfc_Receive_Complete (
                                void                    *psContext,
                                void                    *pHwRef,
                                phNfc_sTransactionInfo_t *pInfo
                                )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS ;
    void                    *pdata = NULL ;
    phDnldNfc_sData_Hdr_t   *resp_data = NULL;
    uint16_t                length = 0 ;
    phNfc_sCompletionInfo_t comp_info = {0,0,0};

    DNLD_PRINT("\n FW_DNLD: Receive Response .... ");
    if ( (NULL != psContext)
        && (NULL != pInfo)
        && (NULL != pHwRef)
        )
    {
        phDnldNfc_sContext_t *psDnldContext =
                                (phDnldNfc_sContext_t *)psContext;
        status = pInfo->status ;
        length = pInfo->length ;
        pdata = pInfo->buffer;

        if(status != NFCSTATUS_SUCCESS)
        {
            DNLD_DEBUG(" Failed. Status = %02X\n",status);
            /* Handle the Error Scenario */
        }
        else if (NULL == pdata)
        {
            DNLD_DEBUG(" Failed. No data received. pdata = %02X\n",pdata);
            /* Handle the Error Scenario */
            status = PHNFCSTVAL( CID_NFC_DNLD,  NFCSTATUS_FAILED );
        }
        else if ((0 == length) 
            || (PHDNLD_MIN_PACKET > length ))
        {
            DNLD_DEBUG(" Receive Response Length = %u .... \n",length);
            /* Handle the Error Scenario */
#ifndef HAL_SW_DNLD_RLEN
             status = PHNFCSTVAL( CID_NFC_DNLD,
                           NFCSTATUS_INVALID_RECEIVE_LENGTH );
#endif
        }
        else
        {

#if defined(FW_DOWNLOAD_TIMER) && \
                (FW_DOWNLOAD_TIMER == 2)
        if ( NXP_INVALID_TIMER_ID != psDnldContext->timer_id )
        {
            phOsalNfc_Timer_Stop( psDnldContext->timer_id );
        }

#endif

#ifndef DNLD_SUMMARY
            DNLD_PRINT_BUFFER("Receive Buffer",pdata,length);
#endif
            DNLD_DEBUG(" Receive Response Length = %X. \n", length);

            resp_data = (phDnldNfc_sData_Hdr_t *) pdata;

            switch(resp_data->frame_type)
            {
                case PHDNLD_RESP_SUCCESS:
                {
                    uint16_t resp_length =
                        ((uint16_t)resp_data->frame_length[0] << BYTE_SIZE)|
                                    resp_data->frame_length[1];
                    switch ( psDnldContext->prev_cmd )
                    {
                        case PHDNLD_CMD_READ :
                        {
							if( PHDNLD_NO_OPERATION
							       == psDnldContext->raw_mode_upgrade)
                            {
                            status = phDnldNfc_Process_Response(
                                    psDnldContext, pHwRef, pdata , length);

                            if (NFCSTATUS_SUCCESS != status)
                            {
                                /* psDnldContext->dnld_retry++; */
                                psDnldContext->dnld_retry = NXP_MAX_DNLD_RETRY;
                                /* psDnldContext->dnld_retry < NXP_MAX_DNLD_RETRY */
                            }
                            }
                            else
                            {

                            }
                            break;
                        }
                        case PHDNLD_CMD_CHECK_INTEGRITY :
                        {
							if( PHDNLD_NO_OPERATION
							       == psDnldContext->raw_mode_upgrade)
							{
#if  (NXP_FW_INTEGRITY_CHK >= 0x01)
                            phDnldNfc_sChkCrcComplete_t *p_dnld_crc_all = 
                                &psDnldContext->chk_integrity_crc;
                            switch(psDnldContext->chk_integrity_param)
                            {
                                case CHK_INTEGRITY_CONFIG_PAGE_CRC:
                                {
                                    (void)memcpy(&p_dnld_crc_all->config_page,  
                                                (((uint8_t *)pdata) + PHDNLD_MIN_PACKET), resp_length); 
                                    break;
                                }
                                case CHK_INTEGRITY_PATCH_TABLE_CRC:
                                {
                                    (void)memcpy(&p_dnld_crc_all->patch_table,  
                                                (((uint8_t *)pdata) + PHDNLD_MIN_PACKET), resp_length); 
                                    break;
                                }
                                case CHK_INTEGRITY_FLASH_CODE_CRC:
                                {
                                    (void)memcpy(&p_dnld_crc_all->flash_code,  
                                                (((uint8_t *)pdata) + PHDNLD_MIN_PACKET), resp_length); 
                                    break;
                                }
                                case CHK_INTEGRITY_PATCH_CODE_CRC:
                                {
                                    (void)memcpy(&p_dnld_crc_all->patch_code,  
                                                (((uint8_t *)pdata) + PHDNLD_MIN_PACKET), resp_length); 
                                    break;
                                }
                                case CHK_INTEGRITY_COMPLETE_CRC:
                                {
                                    (void)memcpy(p_dnld_crc_all,
                                            (((uint8_t *)pdata) + PHDNLD_MIN_PACKET), resp_length); 
                                    DNLD_DEBUG(" FW_DNLD: Check Integrity Complete Structure Size  = %X \n", 
                                                    sizeof(psDnldContext->chk_integrity_crc));
                                    break;
                                }
                                default:
                                {
                                    status = PHNFCSTVAL(CID_NFC_DNLD,
                                            NFCSTATUS_FEATURE_NOT_SUPPORTED);
                                    break;
                                }
                            }
#endif /* #if  (NXP_FW_INTEGRITY_CHK >= 0x01) */
							}
							else
							{
                                psDnldContext->raw_mode_upgrade =
                                     (PHDNLD_SETUP_OPERATION | PHDNLD_ADVANCED_OPERATION);
                                /* psDnldContext->raw_mode_upgrade =
                                    (psDnldContext->raw_mode_upgrade &
                                     ( psDnldContext->cur_frame_info & ~PHDNLD_MARKER_MASK )); */
							}
                            break;
                        }
                        case PHDNLD_CMD_WRITE:
                        {
                            psDnldContext->dnld_retry = 0;
                            break;
                        }
                        case PHDNLD_CMD_SEC_WRITE:
                        {
                            psDnldContext->dnld_retry = 0;
                            break;
                        }
                        case PHDNLD_CMD_ACTIVATE_PATCH:
                        case PHDNLD_CMD_CHECK:
                        default:
                        {
							if( PHDNLD_NO_OPERATION
							       == psDnldContext->raw_mode_upgrade)
							{
                            if( ( (PHDNLD_MIN_PACKET > length)
                                || ( 0 != resp_length) )
                                )
                            {
                                psDnldContext->dnld_retry = NXP_MAX_DNLD_RETRY;
                                status = PHNFCSTVAL( CID_NFC_DNLD,
                                                NFCSTATUS_INVALID_RECEIVE_LENGTH );
                            }
                            else
                            {
                                psDnldContext->dnld_retry = 0;
                            }
							}
							else
							{
                                psDnldContext->raw_mode_upgrade =
                                        (psDnldContext->raw_mode_upgrade & ~PHDNLD_RECOVER_OPERATION);
							}
                            break;
                        }
                    } /* End of the Previous Command Switch Case */
                    break;
                }/* Case PHDNLD_RESP_SUCCESS*/
                case PHDNLD_RESP_TIMEOUT:
                case PHDNLD_RESP_CRC_ERROR:
                case PHDNLD_RESP_WRITE_ERROR:
                {
                    if(psDnldContext->dnld_retry < NXP_MAX_DNLD_RETRY )
                    {
                        psDnldContext->dnld_retry++;
                    }
                    status = PHNFCSTVAL(CID_NFC_DNLD,
                                                resp_data->frame_type);
                    break;
                }
                /* fall through */
                case PHDNLD_RESP_ACCESS_DENIED:
                case PHDNLD_RESP_INVALID_PARAMETER:
                case PHDNLD_RESP_INVALID_LENGTH:
                    /*  Initial Frame Checksum */
                case PHDNLD_RESP_CHKSUM_ERROR:
                case PHDNLD_RESP_MEMORY_UPDATE_ERROR:
                {
                    psDnldContext->dnld_retry = NXP_MAX_DNLD_RETRY;
                    status = PHNFCSTVAL(CID_NFC_DNLD,
                                                resp_data->frame_type);
                    break;
                }
                case PHDNLD_RESP_PROTOCOL_ERROR:
                {
					if(( PHDNLD_NO_OPERATION
							== psDnldContext->raw_mode_upgrade)
                            || ( PHDNLD_ADVANCED_OPERATION
							== psDnldContext->raw_mode_upgrade)
                            )
                    {
                        psDnldContext->dnld_retry = NXP_MAX_DNLD_RETRY;
                        status = PHNFCSTVAL(CID_NFC_DNLD,
                                            NFCSTATUS_INVALID_FORMAT);
                    }
					else if( (PHDNLD_NORMAL_OPERATION
                                 & psDnldContext->raw_mode_upgrade)
                            )
					{
                        psDnldContext->raw_mode_upgrade =
                               (psDnldContext->raw_mode_upgrade & ~PHDNLD_NORMAL_OPERATION);
					}
                    else if ( PHDNLD_RECOVER_OPERATION
                                 & psDnldContext->raw_mode_upgrade )
                    {
                        psDnldContext->dnld_retry = NXP_MAX_DNLD_RETRY;
                        status = PHNFCSTVAL(CID_NFC_DNLD,
                                            NFCSTATUS_INVALID_FORMAT);
                    }
                    else
                    {
                       psDnldContext->raw_mode_upgrade =
                        (psDnldContext->raw_mode_upgrade &
                            ~( psDnldContext->cur_frame_info & ~PHDNLD_MARKER_MASK ));
                    }
                    break;
                }
                case PHDNLD_RESP_VERSION_UPTODATE:
                {
					/* TODO: to make sure that the Advance Frames are sent to get
					 *       the updated status */
					if ( PHDNLD_ADVANCED_OPERATION
                                 == psDnldContext->raw_mode_upgrade)
					{
						status = ( CID_NFC_DNLD << BYTE_SIZE ) ;
					}
                    else if ( PHDNLD_NO_OPERATION
                                != psDnldContext->raw_mode_upgrade)
                    {

                       psDnldContext->raw_mode_upgrade =
                        (psDnldContext->raw_mode_upgrade &
                            ~( psDnldContext->cur_frame_info & ~PHDNLD_MARKER_MASK ));
                    }
                    else
                    {
                    }
                    break;
                }
                case PHDNLD_RESP_CMD_NOT_SUPPORTED:
                {

                    if ( PHDNLD_NO_OPERATION
                                 == psDnldContext->raw_mode_upgrade)
                    {
                        status = PHNFCSTVAL(CID_NFC_DNLD,
                            NFCSTATUS_FEATURE_NOT_SUPPORTED);
                    }
                    else if ( PHDNLD_ADVANCED_OPERATION
                                 == psDnldContext->raw_mode_upgrade)
					{
						status = PHNFCSTVAL(CID_NFC_DNLD,
										 NFCSTATUS_FEATURE_NOT_SUPPORTED);
					}
#if 0
					else if( (PHDNLD_NORMAL_OPERATION
                                 & psDnldContext->raw_mode_upgrade)
                            )
					{
                        psDnldContext->raw_mode_upgrade =
                               (psDnldContext->raw_mode_upgrade & ~PHDNLD_NORMAL_OPERATION);
					}
                    else if ( PHDNLD_SETUP_OPERATION
                                 & psDnldContext->raw_mode_upgrade )
                    {
                        psDnldContext->raw_mode_upgrade =
                               (psDnldContext->raw_mode_upgrade & ~PHDNLD_SETUP_OPERATION);
                    }
#endif
                    else
                    {
                       psDnldContext->raw_mode_upgrade =
                        (psDnldContext->raw_mode_upgrade &
                            ~( psDnldContext->cur_frame_info & ~PHDNLD_MARKER_MASK ));
                    }
                    break;
                }
               /*  The Chaining of the Command Frame
                                  was Successful in the Download Mode */
                case PHDNLD_RESP_CHAINING_SUCCESS:
                {
					/* TODO: Handle the Corner Case Scenarios
					 *       the updated status */
                    psDnldContext->dnld_retry = 0x00;
                    break;
                }
/*  The Error during the Chaining the Command Frame in the Download Mode */
                case PHDNLD_RESP_CHAINING_ERROR:
                {
					/* TODO: Restart the Chunk in Corner Case
					 *       the updated status */
                    psDnldContext->dnld_retry++;
                    phDnldNfc_Tx_Reset(psDnldContext);
                    break;
                }
/*  The Command is not allowed anymore in the Download Mode */
                case PHDNLD_RESP_CMD_NOT_ALLOWED:
                default:
                {
                    psDnldContext->dnld_retry = NXP_MAX_DNLD_RETRY;
                    status = PHNFCSTVAL(CID_NFC_DNLD,
                                        NFCSTATUS_NOT_ALLOWED);
                    break;
                }

            } /* End of the Response Frame Type Switch */

            if (NFCSTATUS_PENDING != status)
            {
                if ((NFCSTATUS_SUCCESS != status) &&
                    (psDnldContext->dnld_retry >= NXP_MAX_DNLD_RETRY))
                {
                    pphNfcIF_Notification_CB_t  p_upper_notify =
                        psDnldContext->p_upper_notify;
                    void                        *p_upper_context =
                                        psDnldContext->p_upper_context;

                    comp_info.status = status;
                    DNLD_DEBUG(" FW_DNLD: Termination in Receive, Status = %X \n", status);
                    status = phDal4Nfc_Unregister(
                                psDnldContext->lower_interface.pcontext, pHwRef);
                    phDnldNfc_Release_Lower(psDnldContext, pHwRef);
                    phDnldNfc_Release_Resources(&psDnldContext);
#ifndef NFC_TIMER_CONTEXT
                    gpphDnldContext = psDnldContext;
#endif
                    /* Notify the Error/Success Scenario to the upper layer */
                    phDnldNfc_Notify( p_upper_notify, p_upper_context, pHwRef,
                                    (uint8_t) NFC_IO_ERROR, &comp_info );
                }
                else if ( (NFCSTATUS_SUCCESS != status) &&
                           (NFCSTATUS_SUCCESS == PHNFCSTATUS(status))
                         )
                {
                    pphNfcIF_Notification_CB_t  p_upper_notify =
                        psDnldContext->p_upper_notify;
                    void                        *p_upper_context =
                                        psDnldContext->p_upper_context;

                    comp_info.status = NFCSTATUS_SUCCESS;
                    DNLD_DEBUG(" FW_DNLD: Termination in Receive, Status = %X \n", status);
                    status = phDal4Nfc_Unregister(
                                psDnldContext->lower_interface.pcontext, pHwRef);
                    phDnldNfc_Release_Lower(psDnldContext, pHwRef);
                    phDnldNfc_Release_Resources(&psDnldContext);
#ifndef NFC_TIMER_CONTEXT
                    gpphDnldContext = psDnldContext;
#endif
                    /* Notify the Error/Success Scenario to the upper layer */
                    phDnldNfc_Notify( p_upper_notify, p_upper_context, pHwRef,
                        (uint8_t) NFC_IO_SUCCESS, &comp_info );

                }
                else if (NFCSTATUS_FEATURE_NOT_SUPPORTED == PHNFCSTATUS(status))
                {
                    pphNfcIF_Notification_CB_t  p_upper_notify =
                        psDnldContext->p_upper_notify;
                    void                        *p_upper_context =
                                        psDnldContext->p_upper_context;

                    comp_info.status = status;
                    DNLD_DEBUG(" FW_DNLD: Termination in Receive, Status = %X \n", status);
                    status = phDal4Nfc_Unregister(
                                psDnldContext->lower_interface.pcontext, pHwRef);
                    phDnldNfc_Release_Lower(psDnldContext, pHwRef);
                    phDnldNfc_Release_Resources(&psDnldContext);
#ifndef NFC_TIMER_CONTEXT
                    gpphDnldContext = psDnldContext;
#endif
                    /* Notify the Error/Success Scenario to the upper layer */
                    phDnldNfc_Notify( p_upper_notify, p_upper_context, pHwRef,
                        (uint8_t) NFC_IO_SUCCESS, &comp_info );

                }
                else
                {
                    /* DNLD_PRINT(" FW_DNLD: Successful.\n"); */
                    psDnldContext->resp_length = /* PHDNLD_MIN_PACKET */ 0 ;
                    status = phDnldNfc_Set_Seq(psDnldContext,
                                                    DNLD_SEQ_UPDATE);
                    status = phDnldNfc_Resume( psDnldContext,
                                            pHwRef, pdata, length );
                }
            }
        } /* End of status != Success */
    }
}


STATIC
void
phDnldNfc_Send_Complete (
                                void                    *psContext,
                                void                    *pHwRef,
                                phNfc_sTransactionInfo_t *pInfo
                       )
{
    NFCSTATUS               status = NFCSTATUS_SUCCESS ;
    uint16_t                    length = 0;

    DNLD_PRINT(" FW_DNLD: Send Data .... ");
    if ( (NULL != psContext)
        && (NULL != pInfo)
        && (NULL != pHwRef)
        )
    {
        phDnldNfc_sContext_t *psDnldContext =
                                    (phDnldNfc_sContext_t *)psContext;
        status = pInfo->status ;
        length = pInfo->length ;
        if(status != NFCSTATUS_SUCCESS)
        {
            DNLD_DEBUG(" Failed. Status = %02X\n",status);
            /* Handle the Error Scenario */
        }
        else
        {
            DNLD_PRINT(" Successful.\n");
            (void)memset((void *)&psDnldContext->dnld_data, 0,
                                sizeof(psDnldContext->dnld_data));
            if ((PHDNLD_CMD_SET_HIF != psDnldContext->prev_cmd)
                && (PHDNLD_CMD_RESET != psDnldContext->prev_cmd))
            {
                psDnldContext->rx_info.rx_total = 0;
                status = phDnldNfc_Receive( psDnldContext, pHwRef,
                            (uint8_t *)(&psDnldContext->dnld_resp),
                                           psDnldContext->resp_length);
            }
            else
            {
                psDnldContext->resp_length = 0;
                psDnldContext->dnld_retry = 0;
                /* clock unstable after SW reset command, especially on UART
                 * platform because of its sensitivity to clock. Experimentally
                 * we found clock unstable for 750us. Delay for 5ms to be sure.
                 */
                if( PHDNLD_CMD_RESET == psDnldContext->prev_cmd )
                {
                    DO_DELAY(PHDNLD_DNLD_DELAY);
                }
#if defined(FW_DOWNLOAD_TIMER) && \
                (FW_DOWNLOAD_TIMER == 2)

                if ( NXP_INVALID_TIMER_ID != psDnldContext->timer_id )
                {
                    phOsalNfc_Timer_Stop( psDnldContext->timer_id );
                }
#endif

                status = phDnldNfc_Set_Seq(psDnldContext,
                                                DNLD_SEQ_UPDATE);
            }

            if(NFCSTATUS_SUCCESS == status )
            {
                status = phDnldNfc_Resume( psDnldContext, pHwRef, NULL, length);
            }

        } /* End of status != Success */

    } /* End of Context != NULL  */
}



STATIC
NFCSTATUS
phDnldNfc_Send_Command(
                        phDnldNfc_sContext_t    *psDnldContext,
                        void                    *pHwRef,
                        uint8_t                 cmd,
                        void                    *params,
                        uint16_t                param_length
                      )
{
    NFCSTATUS   status = NFCSTATUS_SUCCESS;
    uint16_t    tx_length = 0;
    uint16_t    rx_length = 0;
    uint8_t     **pp_resp_data = &psDnldContext->p_resp_buffer;
    phDnldNfc_sData_t       *p_dnld_data =
                 (phDnldNfc_sData_t *)psDnldContext->dnld_data;

    switch(cmd)
    {
        case PHDNLD_CMD_RESET:
        {
            (void)memset((void *)&psDnldContext->dnld_data, 0,
                                sizeof(psDnldContext->dnld_data));
            break;
        }
        case PHDNLD_CMD_READ:
        {
            phDnldNfc_sData_t       *p_dnld_data =
                 (phDnldNfc_sData_t *)psDnldContext->dnld_data;
            phDnldNfc_sParam_t  *param_info = /* (phDnldNfc_sParam_t *)params */
                               &p_dnld_data->param_info.data_param;
            tx_length = PHDNLD_CMD_READ_LEN;
            if (NULL != *pp_resp_data)
            {
                phOsalNfc_FreeMemory(*pp_resp_data);
                *pp_resp_data = NULL;
            }
            rx_length = (uint16_t) (((uint16_t)param_info->data_len[0]
                                   << BYTE_SIZE) + param_info->data_len[1]);

            psDnldContext->resp_length =
                (( rx_length + PHDNLD_MIN_PACKET ));
            (void)phDnldNfc_Allocate_Resource( (void **) pp_resp_data,
                     rx_length);
            break;
        }
        case PHDNLD_CMD_WRITE:
        case PHDNLD_CMD_SEC_WRITE:
        {
            phDnldNfc_sData_t       *p_dnld_data =
                 (phDnldNfc_sData_t *)psDnldContext->dnld_data;
            phDnldNfc_sParam_t  *param_info = /* (phDnldNfc_sParam_t *)params */
                                &p_dnld_data->param_info.data_param;
            tx_length = (uint16_t) (((uint16_t)param_info->data_len[0]
                        << BYTE_SIZE) + param_info->data_len[1]
                                    + PHDNLD_CMD_WRITE_MIN_LEN );

            psDnldContext->resp_length = PHDNLD_MIN_PACKET;
            if ((0 != param_length) && (NULL != params))
            {
                (void)memcpy(param_info->data_packet,  params, param_length);
            }
            break;
        }
        case PHDNLD_CMD_CHECK:
        {
            tx_length = PHDNLD_CMD_CHECK_LEN;
            psDnldContext->resp_length = PHDNLD_MIN_PACKET;
            break;
        }
        case PHDNLD_CMD_ENCAPSULATE:
        {
            uint8_t  i = 0x00;
            if ((0 != param_length) && (NULL != params))
            {
                p_dnld_data->frame_type =
                            PHDNLD_CMD_ENCAPSULATE;
                (void)memcpy((void *)( ((uint8_t *)p_dnld_data)
                                           + PHDNLD_FRAME_DATA_OFFSET)
                                        , params, param_length);
                tx_length = param_length;

                p_dnld_data->frame_length[i++] =
                           (uint8_t)(tx_length >> BYTE_SIZE);
                p_dnld_data->frame_length[i]   =
                           (uint8_t)( tx_length & BYTE_MASK );
                tx_length += PHDNLD_FRAME_DATA_OFFSET;

                psDnldContext->resp_length = PHDNLD_MIN_PACKET;

                status = phDnldNfc_Send( psDnldContext, pHwRef ,
                                    (uint8_t *)p_dnld_data, tx_length);
            }
            else
            {
               status = PHNFCSTVAL(CID_NFC_DNLD,
                              NFCSTATUS_NOT_ALLOWED);
            }
            break;
        }
        case PHDNLD_CMD_SET_HIF:
        {
            tx_length++;
            psDnldContext->resp_length = PHDNLD_MIN_PACKET;
            break;
        }
        case PHDNLD_CMD_ACTIVATE_PATCH:
        {
            psDnldContext->resp_length = PHDNLD_MIN_PACKET;
            if ((NULL != params) && ( param_length > 0 ))
            {
                p_dnld_data->param_info.cmd_param =
                                            (*(uint8_t *)params);
                tx_length = param_length;
            }
            else
            {
                p_dnld_data->param_info.cmd_param = FALSE;
                tx_length++;
            }
            break;
        }
        case PHDNLD_CMD_CHECK_INTEGRITY:
        {
#if  (NXP_FW_INTEGRITY_CHK >= 0x01)
            if ((NULL != params) && ( param_length > 0 ))
            {
                psDnldContext->chk_integrity_param = 
                            (phDnldNfc_eChkCrc_t)(*(uint8_t *)params);
                tx_length = param_length;
            }
            else
            {
                psDnldContext->chk_integrity_param = CHK_INTEGRITY_COMPLETE_CRC;
                tx_length++;
            }
            p_dnld_data->param_info.cmd_param =
                                (uint8_t) psDnldContext->chk_integrity_param;
            switch(psDnldContext->chk_integrity_param)
            {
                case CHK_INTEGRITY_CONFIG_PAGE_CRC:
                case CHK_INTEGRITY_PATCH_TABLE_CRC:
                {
                    psDnldContext->resp_length = PHDNLD_MIN_PACKET 
                                         + CHECK_INTEGRITY_RESP_CRC16_LEN;
                    break;
                }
                case CHK_INTEGRITY_FLASH_CODE_CRC:
                case CHK_INTEGRITY_PATCH_CODE_CRC:
                {
                    psDnldContext->resp_length = PHDNLD_MIN_PACKET 
                                        +  CHECK_INTEGRITY_RESP_CRC32_LEN;
                    break;
                }
                case CHK_INTEGRITY_COMPLETE_CRC:
                default:
                {
                    psDnldContext->resp_length = PHDNLD_MIN_PACKET 
                                        +  CHECK_INTEGRITY_RESP_COMP_LEN;
                    break;
                }
            }
#else
            tx_length++;
            p_dnld_data->param_info.cmd_param =
                                (uint8_t) CHK_INTEGRITY_COMPLETE_CRC;

#endif /* #if  (NXP_FW_INTEGRITY_CHK >= 0x01) */
            break;
        }
        default:
        {
            status = PHNFCSTVAL(CID_NFC_DNLD, NFCSTATUS_FEATURE_NOT_SUPPORTED);
            break;
        }
    }
    if (NFCSTATUS_SUCCESS == status)
    {
        uint8_t     i = 0;

        p_dnld_data->frame_type = cmd;
        p_dnld_data->frame_length[i++] =
                                    (uint8_t)(tx_length >> BYTE_SIZE);
        p_dnld_data->frame_length[i]   =
                                    (uint8_t)( tx_length & BYTE_MASK );
        tx_length = tx_length + PHDNLD_MIN_PACKET;
        status = phDnldNfc_Send( psDnldContext, pHwRef ,
                            (uint8_t *)p_dnld_data, tx_length);
        if(NFCSTATUS_PENDING == status)
        {
            psDnldContext->prev_cmd = cmd;

            if( PHDNLD_CMD_RESET == cmd )
                DO_DELAY(PHDNLD_DNLD_DELAY);  //this seems like its on the wrong thread
        }
    }

    return status;
}

static
NFCSTATUS
phDnldNfc_Check_FW(
                    phHal_sHwReference_t    *pHwRef,
                    fw_data_hdr_t           *cur_fw_hdr
                     )
{
    NFCSTATUS               status = NFCSTATUS_FAILED;

        if ( !pHwRef->device_info.fw_version )
        {
            /* Override the Firmware Version Check and upgrade*/;
            DNLD_PRINT(" FW_DNLD_CHK: Forceful Upgrade of the Firmware .... Required \n");
            status = NFCSTATUS_SUCCESS;
        }
        else    if ( (pHwRef->device_info.fw_version >> (BYTE_SIZE * 2)) 
                != ( cur_fw_hdr->fw_version >> (BYTE_SIZE * 2) ))
        {
            /* Check for the Compatible Romlib Version for the Hardware */
            DNLD_PRINT(" FW_DNLD: IC Hardware Version Mismatch.. \n");
            status = PHNFCSTVAL( CID_NFC_DNLD, NFCSTATUS_NOT_ALLOWED );
        }
        else if (( pHwRef->device_info.fw_version < cur_fw_hdr->fw_version  )
            )
        {
            /* TODO: Firmware Version Check and upgrade*/
            DNLD_PRINT(" FW_DNLD: Older Firmware Upgrading to newerone.... \n");
            status = NFCSTATUS_SUCCESS;
        }
#ifdef NXP_FW_CHK_LATEST
        else if (( pHwRef->device_info.fw_version > cur_fw_hdr->fw_version  )
            )
        {
            DNLD_PRINT(" FW_DNLD: Newer than the Stored One .... \n");
            status = PHNFCSTVAL( CID_NFC_DNLD, NFCSTATUS_NOT_ALLOWED );
        }
#endif /* NXP_FW_CHK_LATEST */
        else
        {
            DNLD_PRINT(" FW_DNLD: Already Updated .... \n");
            status = ( CID_NFC_DNLD << BYTE_SIZE ) ;
        }

    return status;
    }


static
NFCSTATUS
phDnldNfc_Process_FW(
                        phDnldNfc_sContext_t    *psDnldContext,
                        phHal_sHwReference_t    *pHwRef
#ifdef NXP_FW_PARAM
                        ,uint8_t                 *nxp_nfc_fw
                        ,uint32_t                 nxp_fw_len
#endif
                     )
{
    NFCSTATUS               status = NFCSTATUS_FAILED;
    section_info_t          *p_cur_sec = NULL;
    static unsigned         sec_type;
    uint32_t                fw_index = 0;
#ifdef NXP_NFC_MULTIPLE_FW
    phDnldNfc_sFwImageInfo_t  *p_cur_fw = NULL;
#endif /* #ifdef NXP_NFC_MULTIPLE_FW */
    fw_data_hdr_t           *cur_fw_hdr = NULL;
    uint8_t                 sec_index = 0;
    uint8_t                 i = 0;

    psDnldContext->p_img_hdr = (img_data_hdr_t *) nxp_nfc_fw;

#ifdef NXP_NFC_MULTIPLE_FW

    /* TODO: Create a memory of pointers to store all the Firmwares */
    if( (NXP_NFC_IMAG_FW_MAX > psDnldContext->p_img_hdr->no_of_fw_img)
        && (0 != psDnldContext->p_img_hdr->no_of_fw_img)
        )
    {
        ( void )phDnldNfc_Allocate_Resource((void **)&psDnldContext->p_img_info,
            (psDnldContext->p_img_hdr->no_of_fw_img * sizeof(phDnldNfc_sFwImageInfo_t)));

        if(NULL != psDnldContext->p_img_info)
        {
            p_cur_fw = psDnldContext->p_img_info;
        }
    }
#endif /* #ifdef NXP_NFC_MULTIPLE_FW */

    fw_index = sizeof (img_data_hdr_t);

    for ( i=0; i < psDnldContext->p_img_hdr->no_of_fw_img; i++ )
    {

        psDnldContext->p_fw_hdr = (fw_data_hdr_t *) ( nxp_nfc_fw + fw_index );

#ifdef NXP_NFC_MULTIPLE_FW
        if(NULL != p_cur_fw)
        {
            ( p_cur_fw + i)->p_fw_hdr = psDnldContext->p_fw_hdr;
        }
#endif /* #ifdef NXP_NFC_MULTIPLE_FW */
        cur_fw_hdr = psDnldContext->p_fw_hdr;

        fw_index = fw_index + (cur_fw_hdr->fw_hdr_len * PNDNLD_WORD_LEN);

        status = phDnldNfc_Check_FW( pHwRef, cur_fw_hdr);

    }

    if ( ( NFCSTATUS_SUCCESS == status )
#if  defined (NXP_FW_INTEGRITY_VERIFY)
        || (NFCSTATUS_SUCCESS == PHNFCSTATUS(status) )
#endif /* !defined (NXP_FW_INTEGRITY_VERIFY) */
        )
    {
        if( (BYTE_MASK > cur_fw_hdr->no_of_sections)
           && (0 != cur_fw_hdr->no_of_sections)
          )
        {
        (void) phDnldNfc_Allocate_Resource((void **)&psDnldContext->p_fw_sec,
            (cur_fw_hdr->no_of_sections * sizeof(section_info_t)));

            if(NULL != psDnldContext->p_fw_sec)
        {
            DNLD_DEBUG(" FW_DNLD: FW Index : %x \n",
                                            fw_index );

            DNLD_DEBUG(" FW_DNLD: No of Sections : %x \n\n",
                                            cur_fw_hdr->no_of_sections);

            for(sec_index = 0; sec_index
                            < cur_fw_hdr->no_of_sections; sec_index++ )
            {
                p_cur_sec = ((section_info_t *)
                                (psDnldContext->p_fw_sec + sec_index ));

                p_cur_sec->p_sec_hdr = (section_hdr_t *)
                                        (nxp_nfc_fw + fw_index);

                DNLD_DEBUG(" FW_DNLD: Section %x \n",   sec_index);
                DNLD_DEBUG(" FW_DNLD: Section Header Len : %x   ",
                                        p_cur_sec->p_sec_hdr->section_hdr_len);
                DNLD_DEBUG(" Section Address : %x   ",
                                        p_cur_sec->p_sec_hdr->section_address);
                DNLD_DEBUG(" Section Length : %x   ",
                                        p_cur_sec->p_sec_hdr->section_length);
                DNLD_DEBUG(" Section Memory Type : %x   \n",
                                        p_cur_sec->p_sec_hdr->section_mem_type);

                sec_type = (unsigned int)p_cur_sec->p_sec_hdr->section_mem_type;

                if((sec_type & DNLD_TRIM_MASK))
                {
                    p_cur_sec->p_trim_data = (uint8_t *)
                               (nxp_nfc_fw + fw_index + sizeof(section_hdr_t));
                }
                else
                {
                    p_cur_sec->p_trim_data = NULL;
                }

                    if (0 == sec_index)
                    {
                        if ((sec_type & DNLD_SM_UNLOCK_MASK))
                        {
                            (void)phDnldNfc_Set_Seq(psDnldContext,
                                                            DNLD_SEQ_UNLOCK);
                        }
                        else
                        {
                            (void)phDnldNfc_Set_Seq(psDnldContext,
                                                            DNLD_SEQ_INIT);
                        }
                    }
                p_cur_sec->section_read = FALSE;

                p_cur_sec->section_offset = 0;

                p_cur_sec->p_sec_data = ((uint8_t *) nxp_nfc_fw) + fw_index +
                    (p_cur_sec->p_sec_hdr->section_hdr_len * PNDNLD_WORD_LEN);

                fw_index = fw_index +
                    (p_cur_sec->p_sec_hdr->section_hdr_len * PNDNLD_WORD_LEN)
                   + p_cur_sec->p_sec_hdr->section_length;


                    if( 0 != p_cur_sec->p_sec_hdr->section_checksum )
                    {
                            DNLD_DEBUG(" FW_DNLD: Section checksum : %x \n",
                                            p_cur_sec->p_sec_hdr->section_checksum );

                            p_cur_sec->p_sec_chksum = ( uint8_t *)(nxp_nfc_fw + fw_index);

                            fw_index = fw_index +
                                p_cur_sec->p_sec_hdr->section_checksum;
                    }

               DNLD_DEBUG(" FW_DNLD: FW Index : %x \n", fw_index );

#if  (NXP_FW_INTEGRITY_CHK >= 0x01)
              switch( p_cur_sec->p_sec_hdr->section_address )
               {
                   case DNLD_FW_CODE_ADDR:
                   {
                       psDnldContext->p_flash_code_crc = 
                           p_cur_sec->p_sec_data 
                             + p_cur_sec->p_sec_hdr->section_length
                                - DNLD_CRC32_SIZE;
                       break;
                   }
                   case DNLD_PATCH_CODE_ADDR:
                   {
                       psDnldContext->p_patch_code_crc = 
                           p_cur_sec->p_sec_data 
                             + p_cur_sec->p_sec_hdr->section_length
                                - DNLD_CRC32_SIZE;
                       break;
                   }
                   case DNLD_PATCH_TABLE_ADDR:
                   {
                       psDnldContext->p_patch_table_crc = 
                           p_cur_sec->p_sec_data 
                             + p_cur_sec->p_sec_hdr->section_length
                                - DNLD_CRC16_SIZE;
                       break;
                   }
                   default:
                   {
                       break;
                   }

                    } /* End of Address Switch */
#endif /* #if  (NXP_FW_INTEGRITY_CHK >= 0x01) */
                } /* End of For Loop */
            } /* End of the Null Check */
            else
            {
                status = PHNFCSTVAL(CID_NFC_DNLD,
                        NFCSTATUS_INSUFFICIENT_RESOURCES);
               }

            }
        else if (
                   (0 == cur_fw_hdr->no_of_sections)
                   && (PHDNLD_FW_PATCH_SEC == cur_fw_hdr->fw_patch)
                )
        {
            psDnldContext->p_fw_raw = (uint8_t *)(nxp_nfc_fw + fw_index);

			psDnldContext->raw_mode_upgrade = PHDNLD_COMPLETE_OPERATION;

            (void)phDnldNfc_Set_Seq(psDnldContext,
                                            DNLD_SEQ_RAW);
        }
        else
        {
          DNLD_PRINT("*********  Empty Section and Firmware ******************\n\n");
        }

            DNLD_PRINT("*******************************************\n\n");

    }
    return status;
}

#if  !defined (NXP_FW_INTEGRITY_VERIFY)

NFCSTATUS
phDnldNfc_Run_Check(
                        phHal_sHwReference_t    *pHwRef
#ifdef NXP_FW_PARAM
                        ,uint8_t                 *nxp_nfc_fw
                         uint32_t                  fw_length
#endif
                   )
{
    NFCSTATUS               status = NFCSTATUS_FAILED;
    uint32_t                fw_index = 0;
    img_data_hdr_t          *p_img_hdr = NULL;
    fw_data_hdr_t           *p_fw_hdr = NULL;
    fw_data_hdr_t           *cur_fw_hdr = NULL;
    uint8_t                  i = 0;

    p_img_hdr = (img_data_hdr_t *) nxp_nfc_fw;

    fw_index = sizeof (img_data_hdr_t);

    for ( i=0; i < p_img_hdr->no_of_fw_img; i++ )
    {
        p_fw_hdr = (fw_data_hdr_t *) ( nxp_nfc_fw + fw_index );
        /* TODO: Create a memory of pointers to store all the Firmwares */
        cur_fw_hdr = p_fw_hdr;

        fw_index = fw_index + (cur_fw_hdr->fw_hdr_len * PNDNLD_WORD_LEN);

        status = phDnldNfc_Check_FW( pHwRef, cur_fw_hdr);
    }
    return status;
}

#endif /* #if  !defined (NXP_FW_INTEGRITY_VERIFY) */


STATIC
void
phDnldNfc_Abort (
                    uint32_t    abort_id
#ifdef NFC_TIMER_CONTEXT
                    , void     *dnld_cntxt
#endif
                )
{

    phNfc_sCompletionInfo_t  comp_info = {0,0,0};

    phDnldNfc_sContext_t *p_dnld_context = NULL;

#ifdef NFC_TIMER_CONTEXT
    p_dnld_context = (phDnldNfc_sContext_t *)dnld_cntxt;
#else
    p_dnld_context = gpphDnldContext;
#endif

    if ( ( NULL != p_dnld_context)
            && (abort_id == p_dnld_context->timer_id ))
    {
        pphNfcIF_Notification_CB_t  p_upper_notify =
            p_dnld_context->p_upper_notify;
        void                        *p_upper_context =
                                p_dnld_context->p_upper_context;
        phHal_sHwReference_t        *pHwRef = p_dnld_context->p_hw_ref;

        (void)phDal4Nfc_Unregister(
                     p_dnld_context->lower_interface.pcontext, pHwRef );
        phDnldNfc_Release_Lower(p_dnld_context, pHwRef);
        phDnldNfc_Release_Resources(&p_dnld_context);
#ifndef NFC_TIMER_CONTEXT
        gpphDnldContext = p_dnld_context;
#endif

        /* Notify the Error/Success Scenario to the upper layer */
        DNLD_DEBUG(" FW_DNLD: FW_DNLD Aborted with %x Timer Timeout \n",
                                                                abort_id);
        comp_info.status = NFCSTATUS_FAILED ;
        phDnldNfc_Notify( p_upper_notify, p_upper_context,
                        pHwRef, (uint8_t) NFC_IO_ERROR, &comp_info );
    }

    return ;
}



NFCSTATUS
phDnldNfc_Upgrade (
                        phHal_sHwReference_t            *pHwRef,
#ifdef NXP_FW_PARAM
                        uint8_t                          type,
                        uint8_t                         *nxp_nfc_fw,
                        uint32_t                         fw_length,
#endif
                        pphNfcIF_Notification_CB_t      upgrade_complete,
                        void                            *context
                 )
 {
    phDnldNfc_sContext_t    *psDnldContext = NULL;
    phNfcIF_sReference_t    dnldReference = { NULL,0,0 };
    phNfcIF_sCallBack_t     if_callback = { NULL, NULL, NULL, NULL };
    phNfc_sLowerIF_t        *plower_if = NULL;
    NFCSTATUS                status = NFCSTATUS_SUCCESS;
    section_info_t          *p_cur_sec = NULL;
    unsigned                sec_type = 0;

    if( (NULL == pHwRef)
      )
    {
        status = PHNFCSTVAL(CID_NFC_DNLD, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        DNLD_PRINT(" FW_DNLD: Starting the FW Upgrade Sequence .... \n");

        (void)
             phDnldNfc_Allocate_Resource((void **)
                              &psDnldContext,sizeof(phDnldNfc_sContext_t));
        if(psDnldContext != NULL)
        {
#ifndef NFC_TIMER_CONTEXT
            gpphDnldContext = psDnldContext;
#endif
            psDnldContext->p_hw_ref = pHwRef;
            psDnldContext->timer_id = NXP_INVALID_TIMER_ID;

            DNLD_PRINT(" FW_DNLD: Initialisation in Progress.... \n");

            if_callback.pif_ctxt = psDnldContext ;
            if_callback.send_complete = &phDnldNfc_Send_Complete;
            if_callback.receive_complete= &phDnldNfc_Receive_Complete;
            /* if_callback.notify = &phDnldNfc_Notify_Event; */
            plower_if = dnldReference.plower_if = &(psDnldContext->lower_interface);
            status = phDal4Nfc_Register(&dnldReference, if_callback,
                                    NULL);
            DNLD_DEBUG(" FW_DNLD: Lower Layer Register, Status = %02X\n",status);

            if(  (NFCSTATUS_SUCCESS == status) && (NULL != plower_if->init))
            {
                /* psDnldContext->p_config_params = pHwConfig ; */
                status = plower_if->init((void *)plower_if->pcontext,
                                        (void *)pHwRef);
                DNLD_DEBUG(" FW_DNLD: Lower Layer Initialisation, Status = %02X\n",status);
            }
            else
            {
                /* TODO: Handle Initialisation in the Invalid State */
            }
            /* The Lower layer Initialisation successful */
            if (NFCSTATUS_SUCCESS == status)
            {
                psDnldContext->p_upper_notify = upgrade_complete;
                psDnldContext->p_upper_context = context;

                status = phDnldNfc_Process_FW( psDnldContext, pHwRef
#ifdef NXP_FW_PARAM
                ,*nxp_nfc_fw , fw_length
#endif
                 );

                if (NFCSTATUS_SUCCESS == status)
                {
                    status = phDnldNfc_Send_Command( psDnldContext, pHwRef,
                            PHDNLD_CMD_RESET , NULL , 0 );
                    if (NFCSTATUS_PENDING == status)
                    {
                        DNLD_PRINT("\n FW_DNLD: Initial Reset .... \n");

#if defined(FW_DOWNLOAD_TIMER) 

                        psDnldContext->timer_id = phOsalNfc_Timer_Create( );

#if (FW_DOWNLOAD_TIMER < 2)
                        phOsalNfc_Timer_Start( psDnldContext->timer_id,
                                NXP_DNLD_COMPLETE_TIMEOUT,
                                (ppCallBck_t) phDnldNfc_Abort
#ifdef NFC_TIMER_CONTEXT
                                , (void *) psDnldContext
#endif
                                );

#endif  /* #if (FW_DOWNLOAD_TIMER < 2) */

#endif /* #if defined(FW_DOWNLOAD_TIMER)  */

                    }
                }
                else if (NFCSTATUS_SUCCESS == PHNFCSTATUS(status))
                {
#if  defined (NXP_FW_INTEGRITY_VERIFY)
                    /* 
                     * To check for the integrity if the firmware is already 
                     * Upgraded.
                     */
                    status = phDnldNfc_Send_Command( psDnldContext, pHwRef,
                            PHDNLD_CMD_RESET , NULL , 0 );
                    if (NFCSTATUS_PENDING == status)
                    {
                        DNLD_PRINT("\n FW_DNLD: Integrity Reset .... \n");
                        (void)phDnldNfc_Set_Seq(psDnldContext, DNLD_SEQ_COMPLETE);
                        status = PHNFCSTVAL( CID_NFC_DNLD, 
                                        NFCSTATUS_PENDING );
#if defined(FW_DOWNLOAD_TIMER) 
                        psDnldContext->timer_id = phOsalNfc_Timer_Create( );
#if (FW_DOWNLOAD_TIMER < 2)
                        phOsalNfc_Timer_Start( psDnldContext->timer_id,
                                NXP_DNLD_COMPLETE_TIMEOUT, 
                                (ppCallBck_t) phDnldNfc_Abort
#ifdef NFC_TIMER_CONTEXT
                                , (void *) psDnldContext
#endif
                                );

#endif  /* #if (FW_DOWNLOAD_TIMER < 2) */

#endif /* #if defined(FW_DOWNLOAD_TIMER)  */
                    }

#else
                    status = NFCSTATUS_SUCCESS;

#endif /* #if  defined (NXP_FW_INTEGRITY_VERIFY) */

                }
                else
                {
                    DNLD_PRINT(" FW_DNLD Initialisation in Failed \n");
                }
            }

            if (NFCSTATUS_PENDING != PHNFCSTATUS(status))
            {
                (void)phDal4Nfc_Unregister(
                            psDnldContext->lower_interface.pcontext, pHwRef);
                phDnldNfc_Release_Lower(psDnldContext, pHwRef);
                phDnldNfc_Release_Resources(&psDnldContext);
#ifndef NFC_TIMER_CONTEXT
                gpphDnldContext = psDnldContext;
#endif
            }
        } /* End of Status Check for Memory */
        else
        {
            status = PHNFCSTVAL(CID_NFC_DNLD, NFCSTATUS_INSUFFICIENT_RESOURCES);

            DNLD_PRINT(" FW_DNLD: Memory Allocation of Context Failed\n");
        }
    }

    return status;
 }
