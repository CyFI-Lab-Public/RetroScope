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
* \file  phHciNfc_Generic.h                                                   *
* \brief Common HCI Header for the Generic HCI Management.                    *
*                                                                             *
*                                                                             *
* Project: NFC-FRI-1.1                                                        *
*                                                                             *
* $Date: Mon Mar 29 17:34:47 2010 $                                           *
* $Author: ing04880 $                                                         *
* $Revision: 1.73 $                                                           *
* $Aliases: NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $  
*                                                                             *
* =========================================================================== *
*/

/*@{*/
#ifndef PHHCINFC_GENERIC_H
#define PHHCINFC_GENERIC_H

/*@}*/


/**
 *  \name HCI
 *
 * File: \ref phHciNfc_Generic.h
 *
 */
/*@{*/
#define PHHCINFC_GENERIC_FILEREVISION "$Revision: 1.73 $" /**< \ingroup grp_file_attributes */
#define PHHCINFC_GENERIC_FILEALIASES  "$Aliases: NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"     /**< \ingroup grp_file_attributes */
/*@}*/
/*

################################################################################
***************************** Header File Inclusion ****************************
################################################################################
*/
#define LOG_TAG "NFC-HCI"
#include <cutils/log.h>
#include <phNfcIoctlCode.h>
#include<phNfcInterface.h>
#include <phHciNfc.h>
/*
################################################################################
****************************** Macro Definitions *******************************
################################################################################
*/

#define Trace_buffer    phOsalNfc_DbgTraceBuffer

/* HCI TRACE Macros */
#if defined(HCI_TRACE)&& !defined(SILENT_HCI)
#include <phOsalNfc.h>
#include <stdio.h>
extern char phOsalNfc_DbgTraceBuffer[];
#define MAX_TRACE_BUFFER    150
/* #define HCI_PRINT( str )  phOsalNfc_DbgTrace(str) */
#define HCI_PRINT( str )  phOsalNfc_DbgString(str)
#define HCI_DEBUG(...) ALOGD(__VA_ARGS__)




#define HCI_PRINT_BUFFER(msg,buf,len)               \
    {                                               \
        snprintf(Trace_buffer,MAX_TRACE_BUFFER,"\t %s:",msg); \
        phOsalNfc_DbgString(Trace_buffer);              \
        phOsalNfc_DbgTrace(buf,len);                \
        phOsalNfc_DbgString("\r");                  \
                                                    \
    }
#else
#include <phDbgTrace.h>
#if defined(PHDBG_TRACES) && !defined(HCI_TRACE)
#define HCI_PRINT( str )  PHDBG_INFO(str)
#define HCI_DEBUG(str, arg) 
#define HCI_PRINT_BUFFER(msg,buf,len)   
#else
#define HCI_PRINT( str ) 
#define HCI_DEBUG(...)
#define HCI_PRINT_BUFFER(msg,buf,len)   
#endif  /* #if defined(PHDBG_TRACES) */
/* #if defined(PHDBG_INFO) && defined (PHDBG_CRITICAL_ERROR) */

#endif /* #if defined(HCI_TRACE) */

#define ZERO                        0x00U


#ifdef MASK_BITS
#define BYTE_SIZE                   0x08U

/* HCI GET and SET BITS Macros */
#define MASK_BITS8(p,l) \
                         ( ( (((uint8_t)(p))+((uint8_t)(l)))<=BYTE_SIZE )? \
                          (~(0xFFU<<((p)+(l))) & (0xFFU<<(p))):(0U) )
#ifdef MASK_BITS
#define GET_BITS8(num,p,l) \
                          ( ((((uint8_t)(p))+((uint8_t)(l)))<=BYTE_SIZE)? \
                           (((num)& (MASK_BITS8(p,l)))>>(p)):(0U) )
#else
#define GET_BITS8(num,p,l) \
                          ( ((((p)+(l))<=BYTE_SIZE))? \
                           (((num)>>(p))& (~(0xFFU<<(l)))):(0U) )
#endif
#define SET_BITS8(num,p,l,val) \
                         (  ((((uint8_t)(p))+((uint8_t)(l)))<=BYTE_SIZE)? \
                           (((num)& (~MASK_BITS8(p,l)))|((val)<<(p))):(0U))

#endif

/** \ingroup grp_hci_retval
    The Corresponding HCI Gate Not Supported.
 */
#define NFCSTATUS_HCI_GATE_NOT_SUPPORTED    (0x71U)

/** \ingroup grp_hci_retval
    Invalid Command from the HCI Layer
 */
#define NFCSTATUS_INVALID_HCI_COMMAND       (0x72U)

/** \ingroup grp_hci_retval
    HCI Command not supported . */
#define NFCSTATUS_COMMAND_NOT_SUPPORTED     (0x73U)

/** \ingroup grp_hci_retval
    Invalide Response from the HCI Layer
 */
#define NFCSTATUS_INVALID_HCI_RESPONSE      (0x74U)

/** \ingroup grp_hci_retval
    The Invalid Instruction type (Neither Command/Response nor Event ).
 */
#define NFCSTATUS_INVALID_HCI_INSTRUCTION   (0x75U)

/** \ingroup grp_hci_retval
    The Invalid Instruction type (Neither Command/Response nor Event ).
 */
#define NFCSTATUS_INVALID_HCI_INFORMATION   (0x76U)

/** \ingroup grp_hci_retval
    The Invalid HCI Sequence.
 */
#define NFCSTATUS_INVALID_HCI_SEQUENCE      (0x78U)


/** \ingroup grp_hci_retval
    The HCI Error Response with Response code.
 */
#define NFCSTATUS_HCI_RESPONSE(code)        (code)


/* Length of the HCP and the HCP Message Header in Bytes */
#define HCP_HEADER_LEN          0x02U

/* Length of the HCP Message Header in Bytes */
#define HCP_MESSAGE_LEN         0x01U

/* HCP Header Chaining Bit Offset */
#define HCP_CHAINBIT_OFFSET     0x07U
/* HCP Header Chaining Bit Length */
#define HCP_CHAINBIT_LEN        0x01U

/* Chaining Bit Values */
#define HCP_CHAINBIT_DEFAULT    0x01U
#define HCP_CHAINBIT_BEGIN      0x00U
#define HCP_CHAINBIT_END        HCP_CHAINBIT_DEFAULT

/* HCP Header Pipe ID Offset */
#define HCP_PIPEID_OFFSET       0x00U
/* HCP Header Pipe ID Length */
#define HCP_PIPEID_LEN          0x07U

/* HCP Message Header Type  Offset */
#define HCP_MSG_TYPE_OFFSET     0x06U
/* HCP Message Header Type Length */
#define HCP_MSG_TYPE_LEN        0x02U

/* HCP Message Type Values */
#define HCP_MSG_TYPE_COMMAND        0x00U
#define HCP_MSG_TYPE_EVENT          0x01U
#define HCP_MSG_TYPE_RESPONSE       0x02U
#define HCP_MSG_TYPE_RESERVED       0x03U

/* HCP Message Header Instruction  Offset */
#define HCP_MSG_INSTRUCTION_OFFSET  0x00U
/* HCP Message Header Instruction Length */
#define HCP_MSG_INSTRUCTION_LEN     0x06U
/* HCP Invalid Message Instruction */
#define HCP_MSG_INSTRUCTION_INVALID 0x3FU


/* HCP Packet Zero Length */
#define HCP_ZERO_LEN                0x00U



    /** \internal Generic HCI Commands for all the Gates */
#define ANY_SET_PARAMETER                   0x01U
#define ANY_GET_PARAMETER                   0x02U
#define ANY_OPEN_PIPE                       0x03U
#define ANY_CLOSE_PIPE                      0x04U
#define ANY_GENERIC_CMD_RFU_B               0x05U
#define ANY_GENERIC_CMD_RFU_E               0x0FU

/*
 *  0x05-0x0F is Reserved for Future Use
 */

/** \internal HCI Administration Com mands for the Management of the Host Network */

#define ADM_CREATE_PIPE                     0x10U
#define ADM_DELETE_PIPE                     0x11U
#define ADM_NOTIFY_PIPE_CREATED             0x12U
#define ADM_NOTIFY_PIPE_DELETED             0x13U
#define ADM_CLEAR_ALL_PIPE                  0x14U
#define ADM_NOTIFY_ALL_PIPE_CLEARED         0x15U
#define ADM_CMD_RFU_B                       0x16U
#define ADM_CMD_RFU_E                       0x3FU

#define MSG_INSTRUCTION_UNKNWON             0x3FU

    /*
     *  0x16-0x3F is Reserved for Future Use
     */


/** \internal HCI Generic Responses from the Gates */
#define ANY_OK                              0x00U
#define ANY_E_NOT_CONNECTED                 0x01U
#define ANY_E_CMD_PAR_UNKNOWN               0x02U
#define ANY_E_NOK                           0x03U
#define ANY_E_PIPES_FULL                    0x04U
#define ANY_E_REG_PAR_UNKNOWN               0x05U
#define ANY_E_PIPE_NOT_OPENED               0x06U
#define ANY_E_CMD_NOT_SUPPORTED             0x07U
#define ANY_E_INHIBITED                     0x08U
#define ANY_E_TIMEOUT                       0x09U
#define ANY_E_REG_ACCESS_DENIED             0x0AU
#define ANY_E_PIPE_ACCESS_DENIED            0x0BU

/* Response Error Code for RF Reader Gate */
#define WR_RF_ERROR                         0x10U

/*
*  0x08, 0x0B-0x3F is Reserved for Future Use
*/


/** \internal HCI Generic Events from the Gates */
#define EVT_HCI_END_OF_OPERATION    0x01
#define EVT_POST_DATA               0x02
#define EVT_HOT_PLUG                0x03


/* Maximum Buffer Size for the HCI Data */
#define PHHCINFC_MAX_BUFFERSIZE     (PHHAL_MAX_DATASIZE + 0x50U)

#define PHHCINFC_MAX_OPENPIPE       0x6FU
#define PHHCINFC_MAX_PIPE           0x6FU
#define PHHCINFC_MIN_PIPE           0x02U


/* Maximum Payload Length of HCI. */
#define PHHCINFC_MAX_PACKET_DATA    0x1CU
#define PHHCINFC_MAX_HCP_LEN        PHHCINFC_MAX_PACKET_DATA + 1



/* Maximum Payload Length of HCI. */


/*
################################################################################
******************** Enumeration and Structure Definition **********************
################################################################################
*/
#if 1
typedef NFCSTATUS (*pphHciNfc_Pipe_Receive_t) (
                                                void *pContext,
                                                void *pHwRef,
                                                uint8_t *data,
#ifdef ONE_BYTE_LEN
                                                uint8_t length
#else
                                                uint16_t length
#endif
                                        );
#else

typedef pphNfcIF_Transact_t pphHciNfc_Pipe_Receive_t;

#endif

/** \defgroup grp_hci_nfc HCI Component
 *
 *
 */

typedef enum phHciNfc_HostID {
    phHciNfc_HostControllerID                   = 0x00U,
    phHciNfc_TerminalHostID                     = 0x01U,
    phHciNfc_UICCHostID                         = 0x02U
/*
    phHciNfc_HostID_RFU_B                       = 0x03U,
    phHciNfc_HostID_RFU_E                       = 0xBFU,
    phHciNfc_HostIDProprietary_B                = 0xC0U,
    phHciNfc_HostIDProprietary_E                = 0xFFU
*/
}phHciNfc_HostID_t;


typedef enum phHciNfc_GateID{
    phHciNfc_AdminGate                          = 0x00U,
/*
    phHciNfc_evGateIDProprietary_B              = 0x01U,
    phHciNfc_evGateIDProprietary_E              = 0x03U,
*/
    phHciNfc_LoopBackGate                       = 0x04U,
    phHciNfc_IdentityMgmtGate                   = 0x05U,
    phHciNfc_LinkMgmtGate                       = 0x06U,
/*
    phHciNfc_GateID_RFU_B                       = 0x07U,
    phHciNfc_GateID_RFU_E                       = 0x0FU,
*/

/*  TODO: Fillin Other Gate Information */
    /* ETSI HCI Specific RF Reader Gates */
    phHciNfc_RFReaderAGate                      = 0x13,
    phHciNfc_RFReaderBGate                      = 0x11,

    /* Proprietary Reader Gate */
    phHciNfc_ISO15693Gate                       = 0x12,
    phHciNfc_RFReaderFGate                      = 0x14,
    phHciNfc_JewelReaderGate                    = 0x15,

    /* ETSI HCI Card RF Gates */
    phHciNfc_CETypeBGate                        = 0x21,
    phHciNfc_CETypeBPrimeGate                   = 0x22,
    phHciNfc_CETypeAGate                        = 0x23,
    phHciNfc_CETypeFGate                        = 0x24,

    /* NFC-IP1 Gates */
    phHciNfc_NFCIP1InitRFGate                   = 0x30,
    phHciNfc_NFCIP1TargetRFGate                 = 0x31,

    /* ETSI HCI Connectivity Gate */
    phHciNfc_ConnectivityGate                   = 0x41,


    /*  Device Configuration Gates */
    phHciNfc_PN544MgmtGate                      = 0x90,
    phHciNfc_HostCommGate                       = 0x91,
    phHciNfc_GPIOGate                           = 0x92,
    phHciNfc_RFMgmtGate                         = 0x93,
    phHciNfc_PollingLoopGate                    = 0x94,
    phHciNfc_DownloadMgmtGate                   = 0x95,

    /* Card Emulation Managment Gates */
    phHciNfc_SwpMgmtGate                        = 0xA0,
    phHciNfc_NfcWIMgmtGate                      = 0xA1,
    phHciNfc_UnknownGate                        = 0xFF

}phHciNfc_GateID_t;


typedef enum phHciNfc_PipeID{
    HCI_LINKMGMT_PIPE_ID                = 0x00U,
    HCI_ADMIN_PIPE_ID                   = 0x01U,
    HCI_DYNAMIC_PIPE_ID                 = 0x02U,
    HCI_RESERVED_PIPE_ID                = 0x70U,
    HCI_UNKNOWN_PIPE_ID                 = PHHCINFC_MAX_PIPE
/*
    phHciNfc_evOtherGatePipeID_B                = 0x02U,
    phHciNfc_evOtherGatePipeID_E                = 0x6FU,
    phHciNfc_evGatePipeID_RFU_B                 = 0x70U,
    phHciNfc_evGatePipeID_RFU_E                 = 0x7FU,
*/
}phHciNfc_PipeID_t;


typedef enum phHciNfc_eState {
    hciState_Reset              = 0x00U,
    hciState_Initialise,
    hciState_Test,
    hciState_Config,
    hciState_IO,
    hciState_Select,
    hciState_Listen,
    hciState_Activate,
    hciState_Reactivate,
    hciState_Connect,
    hciState_Transact,
    hciState_Disconnect,
    hciState_Presence,
    hciState_Release,
    hciState_Unknown
}phHciNfc_eState_t;

typedef enum phHciNfc_eMode {
    hciMode_Reset               = 0x00U,
    hciMode_Session,
    hciMode_Override,
    hciMode_Test,
    hciMode_Unknown
}phHciNfc_eMode_t;


typedef enum phHciNfc_eSeq{
    /* HCI Admin Sequence */
    ADMIN_INIT_SEQ              = 0x00U,
    ADMIN_SESSION_SEQ,
    ADMIN_CE_SEQ,
    ADMIN_REL_SEQ,
    ADMIN_EVT_HOTPLUG_SEQ,

    /* HCI Link Management Sequence */
    LINK_MGMT_INIT_SEQ,
    LINK_MGMT_REL_SEQ,

    /* HCI Identity Management Sequence */
    IDENTITY_INIT_SEQ,
    IDENTITY_INFO_SEQ,
    IDENTITY_REL_SEQ,

    /* HCI Polling Loop Sequence */
    PL_INIT_SEQ,
    PL_DURATION_SEQ,
    PL_CONFIG_PHASE_SEQ,
    PL_TGT_DISABLE_SEQ,
    PL_RESTART_SEQ,
    PL_STOP_SEQ,
    PL_REL_SEQ,

    /* HCI Device Management Sequence */
    DEV_INIT_SEQ,
    DEV_HAL_INFO_SEQ,
    DEV_CONFIG_SEQ,
    DEV_REL_SEQ,

    /* HCI Reader Management Sequence */
    READER_MGMT_INIT_SEQ,
    READER_ENABLE_SEQ,
    READER_SELECT_SEQ,
    READER_REACTIVATE_SEQ,
    READER_SW_AUTO_SEQ,
    READER_PRESENCE_CHK_SEQ,
    READER_UICC_DISPATCH_SEQ,
    READER_DESELECT_SEQ,
    READER_RESELECT_SEQ,
    READER_DISABLE_SEQ,
    READER_MGMT_REL_SEQ,

    /* HCI NFC-IP1 Sequence */
    NFCIP1_INIT_SEQ,
    INITIATOR_SPEED_SEQ,
    INITIATOR_GENERAL_SEQ,
    TARGET_GENERAL_SEQ,
    TARGET_SPEED_SEQ,
    NFCIP1_REL_SEQ,

    /* HCI Emulation Management Sequence */
    EMULATION_INIT_SEQ,
    EMULATION_SWP_SEQ,
    EMULATION_CONFIG_SEQ,
    EMULATION_REL_SEQ,

    HCI_END_SEQ,
    HCI_INVALID_SEQ
} phHciNfc_eSeq_t;



typedef enum phHciNfc_eSeqType{
    RESET_SEQ                   = 0x00U,
    INIT_SEQ,
    UPDATE_SEQ,
    INFO_SEQ,
    CONFIG_SEQ,
    REL_SEQ,
    END_SEQ
} phHciNfc_eSeqType_t;


typedef enum phHciNfc_eConfigType{
    INVALID_CFG                 = 0x00U,
    POLL_LOOP_CFG,
    SMX_WI_CFG,
    SMX_WI_MODE,
    UICC_SWP_CFG,
    SWP_EVT_CFG,
    SWP_PROTECT_CFG,
    NFC_GENERAL_CFG,
    NFC_TARGET_CFG,
    NFC_CE_A_CFG,
    NFC_CE_B_CFG
} phHciNfc_eConfigType_t;


typedef struct phHciNfc_HCP_Message{
    /** \internal Identifies the Type and Kind of Instruction */
    uint8_t     msg_header;
    /** \internal Host Controller Protocol (HCP) Packet Message Payload */
    uint8_t     payload[PHHCINFC_MAX_PACKET_DATA - 1];
}phHciNfc_HCP_Message_t;


typedef struct phHciNfc_HCP_Packet{
    /** \internal Chaining Information and Pipe Identifier */
    uint8_t     hcp_header;
    /** \internal Host Controller Protocol (HCP) Packet Message or Payload */
    union
    {
    /** \internal Host Controller Protocol (HCP) Packet Message */
        phHciNfc_HCP_Message_t message;
    /** \internal Host Controller Protocol (HCP) Packet Payload */
        uint8_t payload[PHHCINFC_MAX_PACKET_DATA];
    }msg;
}phHciNfc_HCP_Packet_t;



typedef struct phHciNfc_Gate_Info{
    /** \internal HCI Host Identifier  */
    uint8_t     host_id;
    /** \internal HCI Gate Identifier  */
    uint8_t     gate_id;
}phHciNfc_Gate_Info_t;


typedef struct phHciNfc_Pipe_Params{
    /** \internal HCI Source Gate Information for the pipe  */
    phHciNfc_Gate_Info_t    source;
    /** \internal HCI Destination Gate Information for the pipe  */
    phHciNfc_Gate_Info_t    dest;
    /** \internal HCI Pipe Identifier  */
    uint8_t                 pipe_id;
}phHciNfc_Pipe_Params_t;


typedef struct phHciNfc_Pipe_Info{
    /** \internal Structure containing the created dynamic pipe information */
    phHciNfc_Pipe_Params_t      pipe;
    /** \internal Status of the previous command sent to this pipe */
    NFCSTATUS                   prev_status;
    /** \internal previous message type Sent to this pipe */
    uint8_t                     sent_msg_type;
    /** \internal Message type Received in this pipe */
    uint8_t                     recv_msg_type;
    /** \internal previous message sent to this pipe */
    uint8_t                     prev_msg;
    /** \internal Index of the previous Set/Get Parameter command 
     *  sent to this pipe */
    uint8_t                     reg_index;
    /** \internal length of Parameter of the Set/Get Parameter 
     *  command sent to this pipe */
    uint16_t                    param_length;
    /** \internal Parameter of the Set/Get Parameter command 
     *  sent to this pipe */
    void                        *param_info;
    /** \internal Pointer to a Pipe specific Receive Response function */
    pphHciNfc_Pipe_Receive_t    recv_resp; 
    /** \internal Pointer to a Pipe specific Receive Event function */
    pphHciNfc_Pipe_Receive_t    recv_event; 
    /** \internal Pointer to a Pipe specific Receive Command function */
    pphHciNfc_Pipe_Receive_t    recv_cmd; 
}phHciNfc_Pipe_Info_t;


typedef struct phHciNfc_sContext{
    /** \internal HCI Layer Pointer from the upper layer for 
                        lower layer function registration */
    phNfcLayer_sCfg_t           *p_hci_layer;
    /** \internal Pointer to the upper layer context */
    void                        *p_upper_context;
    /** \internal Pointer to the Hardware Reference Sturcture */
    phHal_sHwReference_t        *p_hw_ref;
    /** \internal Pointer to the upper layer notification callback function */
    pphNfcIF_Notification_CB_t  p_upper_notify;
    /** \internal Structure to store the lower interface operations */
    phNfc_sLowerIF_t            lower_interface;
    /** \internal Execution Sequence using the HCI Context */
    volatile phHciNfc_eSeq_t    hci_seq;

    /** \internal State of the HCI Context */
    volatile phNfc_sState_t     hci_state;

    /** \internal Mode of HCI Initialisation */
    phHciNfc_Init_t             init_mode;

    /** \internal Memory Information for HCI Initialisation */
    uint8_t                     hal_mem_info[NXP_HAL_MEM_INFO_SIZE];

    /** \internal HCI Configuration Type */
    phHciNfc_eConfigType_t      config_type;
    /** \internal HCI SmartMX Mode Configuration */
    phHal_eSmartMX_Mode_t       smx_mode;
    /** \internal HCI Configuration Information */
    void                        *p_config_params;

    /** \internal Current RF Reader/Emulation Gate in Use */
    phHal_eRFDevType_t          host_rf_type;

    /** \internal Connected Target Information */
    phHal_sRemoteDevInformation_t *p_target_info;

    /** \internal Information of all the pipes created and opened */
    phHciNfc_Pipe_Info_t        *p_pipe_list[PHHCINFC_MAX_PIPE+1];

    /** \internal Tag */
    phHciNfc_XchgInfo_t         *p_xchg_info;

    /** \internal Information of the HCI Gates */
    /** \internal HCI Admin Management Gate Information */
    void                        *p_admin_info;
    /** \internal HCI Link Management Gate Information */
    void                        *p_link_mgmt_info;
    /** \internal HCI Identity Management Gate Information */
    void                        *p_identity_info;
    /** \internal HCI Polling Loop Gate Information */
    void                        *p_poll_loop_info;
    /** \internal HCI NFC Device Management Information */
    void                        *p_device_mgmt_info;
    /** \internal HCI RF Reader Gates Management Information */
    void                        *p_reader_mgmt_info;
    /** \internal HCI Card Application Gates and Emulation 
                  Information */
    void                        *p_emulation_mgmt_info;
    /** \internal HCI RF Reader A Gate Information */
    void                        *p_reader_a_info;
#ifdef TYPE_B
    /** \internal HCI RF Reader B Gate Information */
    void                        *p_reader_b_info;
#endif
#ifdef TYPE_FELICA
    /** \internal HCI Felica Reader Gate Information */
    void                        *p_felica_info;
#endif
#ifdef TYPE_JEWEL
    /** \internal HCI Jewel Reader Gate Information */
    void                        *p_jewel_info;
#endif
#ifdef TYPE_ISO15693
    /** \internal HCI ISO15693 Reader Gate Information */
    void                        *p_iso_15693_info;
#endif
    
#ifdef ENABLE_P2P
    /** \internal HCI NFC-IP1 Peer to Peer Information */
    void                        *p_nfcip_info;
#endif
    /** \internal HCI Secure Element Management Information */
    void                        *p_wi_info;
    /** \internal HCI UICC Information */
    void                        *p_uicc_info;
    /** \internal HCI SWP Information */
    void                        *p_swp_info;
#ifdef HOST_EMULATION
    /** \internal HCI Card Emulation A Gate Information */
    void                        *p_ce_a_info;
    /** \internal HCI Card Emulation B Gate Information */
    void                        *p_ce_b_info;
#endif

    /** \internal HCI Packet Data to be sent to the lower layer */
    phHciNfc_HCP_Packet_t       tx_packet;
    /** \internal HCI Packet Data to be received from the lower layer */
    phHciNfc_HCP_Packet_t       rx_packet;

    /** \internal Previous Status (To Store the Error Status ) */
    NFCSTATUS                   error_status;

    /** \internal Pointer to HCI Send Buffer */
    uint8_t                     send_buffer[PHHCINFC_MAX_BUFFERSIZE];
    /** \internal Pointer to HCI Receive Buffer */
    uint8_t                     recv_buffer[PHHCINFC_MAX_BUFFERSIZE];

    /** \internal Total Number of bytes to be Sent */
    volatile uint16_t           tx_total;
    /** \internal Number of bytes Remaining to be Sent */
    volatile uint16_t           tx_remain;
    /** \internal Number of bytes sent */
    volatile uint16_t           tx_sent;

    volatile uint16_t           rx_index;

    /** \internal Total Number of bytes received */
    volatile uint16_t           rx_total;
    /** \internal Number of bytes received */
    volatile uint16_t           rx_recvd;
    /** \internal Index of the received data in the 
    *   response packet  
    */

    /** \internal Send HCP Chaining Information */
    volatile uint8_t            tx_hcp_chaining;
    /** \internal Send HCP  Fragment Index */
    volatile uint16_t           tx_hcp_frgmnt_index;

    /** \internal Receive HCP Chaining Information */
    volatile uint8_t            rx_hcp_chaining;
    /** \internal Receive HCP Fragment Index */
    volatile uint16_t           rx_hcp_frgmnt_index;

    /** \internal The Device under Test */
    volatile uint8_t            hci_mode;
    /** \internal Wait for Response if Response is Pending  */
    volatile uint8_t            response_pending;
    /** \internal Notify the Event if Notifcation is Pending  */
    volatile uint8_t            event_pending;

    /** \internal Pending Release of the detected Target */
    uint8_t                     target_release;

}phHciNfc_sContext_t;

/*
################################################################################
*********************** Function Prototype Declaration *************************
################################################################################
*/


/**
 *
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Receive function receives the HCI Events or Response from the
 *  corresponding peripheral device, described by the HCI Context Structure.
 *
 *  \param[in]  psContext               psContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[out] pdata                   Pointer to the response buffer that
 *                                      receives the response read.
 *  \param[in] length                   Variable that receives
 *                                      the number of bytes read.
 *
 *  \retval NFCSTATUS_PENDING           Data successfully read.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Other related errors
 *
 */

extern
NFCSTATUS
phHciNfc_Receive(
                        void                *psContext,
                        void                *pHwRef,
                        uint8_t             *pdata,
#ifdef ONE_BYTE_LEN
                        uint8_t             length
#else
                        uint16_t            length
#endif
                );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Send_Complete function acknowledges the completion of the HCI
 *  Commands sent to the device.
 *
 *  \param[in]  psContext               psContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  pInfo                   Transaction information like
 *                                      status and length after the
 *                                      completion of the send.
 *
 *  \retval NONE.
 *
 */

extern
void
phHciNfc_Send_Complete (
                            void                    *psContext,
                            void                    *pHwRef,
                            phNfc_sTransactionInfo_t *pInfo
                       );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Receive_Complete function acknowledges the completion of the HCI
 *  Event Information or Response received from the device.
 *
 *  \param[in]  psContext               psContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  pInfo                   Transaction information like status
 *                                      data and length after the completely
 *                                      receiving the response .
 *  \retval NONE.
 *
 *
 */

extern
void
phHciNfc_Receive_Complete (
                                void                    *psContext,
                                void                    *pHwRef,
                                phNfc_sTransactionInfo_t *pInfo
                          );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Notify_Event function notifies the occurence of the HCI
 *  Event from the device.
 *
 *  \param[in]  psContext               psContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  type                    reason returned for the notification to
 *                                      the HCI.
 *  \param[in]  pInfo                   Notification information like status
 *                                      data,length etc from the lower layer
 *                                      to the HCI Layer.
 *  \retval NONE.
 *
 */

extern
void
phHciNfc_Notify_Event(
                            void                    *psContext,
                            void                    *pHwRef,
                            uint8_t                 type,
                            void                    *pInfo
                    );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Tag_Notify function notifies the the upper layer
 *  with the Tag Specific Notifications .
 *
 *  \param[in]  psContext               psContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  type                    reason returned for the notification to
 *                                      the HCI.
 *  \param[in]  pInfo                   Notification information like status
 *                                      data,length etc from the lower layer
 *                                      to the HCI Layer.
 *  \retval NONE.
 *
 */
extern
void
phHciNfc_Tag_Notify(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef,
                            uint8_t                 type,
                            void                    *pInfo
               );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Tag_Notify function notifies the the upper layer
 *  with the Tag Specific Notifications .
 *
 *  \param[in]  psContext               psContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  type                    reason returned for the notification to
 *                                      the HCI.
 *  \param[in]  pInfo                   Notification information like status
 *                                      data,length etc from the lower layer
 *                                      to the HCI Layer.
 *  \retval NONE.
 *
 */

extern
void
phHciNfc_Target_Select_Notify(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef,
                            uint8_t                 type,
                            void                    *pInfo
               );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Transceive_Notify function notifies the the upper layer
 *  with the after the transceive operation.
 *
 *  \param[in]  psContext               psContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  type                    reason returned for the notification to
 *                                      the HCI.
 *  \param[in]  pInfo                   Notification information like status
 *                                      data,length etc from the lower layer
 *                                      to the HCI Layer.
 *  \retval NONE.
 *
 */
extern
void
phHciNfc_Transceive_Notify(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef,
                            uint8_t                 type,
                            void                    *pInfo
               );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Notify function calls the upper layer notification callback.
 *
 *  \param[in]  pUpperNotify            pUpperNotify is the notification
 *                                      callback of the upper HAL Layer.
 *  \param[in]  pUpperContext           pUpperContext is the context of
 *                                      the upper HAL Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  type                    type of the notification to
 *                                      the upper HAL layer.
 *  \param[in]  pInfo                   completion information returned 
 *                                      to the Upper HAL Layer.
 *  NFCSTATUS_SUCCESS                   Notification successfully completed .
 *  NFCSTATUS_INVALID_PARAMETER         One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  Other errors                        Errors related to the HCI or lower layers
 *
 *  \retval NONE.
 *
 */

extern
void
phHciNfc_Notify(
                    pphNfcIF_Notification_CB_t  p_upper_notify,
                    void                        *p_upper_context,
                    void                        *pHwRef,
                    uint8_t                     type,
                    void                        *pInfo
               );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Release_Notify function Releases HCI and notifies
 *  the upper layer.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  type                    reason returned for the notification to
 *                                      the HCI.
 *  \param[in]  pInfo                   Notification information like status
 *                                      data,length etc from the lower layer
 *                                      to the HCI Layer.
 *  \retval NONE.
 *
 */
extern
void
phHciNfc_Release_Notify(
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef,
                            uint8_t                 type,
                            void                    *pInfo
               );



/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Send_Generic_Cmd function sends the HCI Generic Commands 
 *  to the device.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  pipe_id                 The pipe to which the command
 *                                      is being sent.
 *  \param[in]  cmd                     The HCI Generic command sent to a
 *                                      particular pipe .
 *
 *  \retval NFCSTATUS_PENDING           HCI Generic Command send in progress .
 *  \retval 
 *  NFCSTATUS_INSUFFICIENT_RESOURCES    The memory could not be allocated 
 *                                      as required amount of memory 
 *                                      is not sufficient.
 *
 */

extern
NFCSTATUS
phHciNfc_Send_Generic_Cmd (
                                phHciNfc_sContext_t *psHciContext,
                                void                *pHwRef,
                                uint8_t             pipe_id,
                                uint8_t             cmd
                    );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Set_Param function configures the Gate specific register 
 *  with the provided value.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *  \param[in]  p_pipe_info             Pointer to pipe specific information.
 *  \param[in]  reg_index               Index of the register to be 
 *                                      configured .
 *  \param[in]  p_param                 Value to the configured in 
 *                                      particular register.
 *  \param[in]  param_length            Length of the parameter provided
 *                                      for the configuration.
 *
 *  \retval NFCSTATUS_PENDING           HCI Set parameter in progress .
 *  \retval 
 *  NFCSTATUS_INVALID_HCI_INFORMATION   The Information like p_pipe_info,
 *                                      p_param or param_length is invalid
 *
 */

extern
NFCSTATUS
phHciNfc_Set_Param (
                            phHciNfc_sContext_t     *psHciContext,
                            void                    *pHwRef,
                            phHciNfc_Pipe_Info_t    *p_pipe_info,
                            uint8_t                 reg_index,
                            void                    *p_param,
                            uint16_t                 param_length
                );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Send_HCP function sends the HCI Host Control Packet 
 *  Frames to the device.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NFCSTATUS_PENDING           HCP Frame send pending.
 *  \retval NFCSTATUS_INVALID_PARAMETER One or more of the supplied parameters
 *                                      could not be interpreted properly.
 *  \retval Other errors                Other related errors
 *
 *
 */

extern
NFCSTATUS
phHciNfc_Send_HCP (
                            phHciNfc_sContext_t *psHciContext,
                            void                *pHwRef
                        );


/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Build_HCPFrame function initially builds the HCP Packet Frame 
 *  with the values passed in the arguments .
 *
 *  \param[in]  hcp_packet              hcp_packet is the frame packet structure
 *                                      in which the frame is populated with the 
 *                                      appropriate fields.
 *  \param[in]  chainbit                chainbit specifies whether the following 
 *                                      HCP frames are chained or the frame is a 
 *                                      normal frame.
 *  \param[in]  pipe_id                 pipe_id of the pipe to which the frame has
 *                                      to be sent.
 *  \param[in]  msg_type                type of message sent to the pipe.
 *  \param[in]  instruction             type of message instruction send to the pipe.
 *
 *  \retval NONE.
 *
 */


extern
void
phHciNfc_Build_HCPFrame (
                                phHciNfc_HCP_Packet_t *hcp_packet,
                                uint8_t             chainbit,
                                uint8_t             pipe_id,
                                uint8_t             msg_type,
                                uint8_t             instruction
                      );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Append_HCPFrame function Appends the HCP Packet Frame 
 *  with the values passed in the arguments .
 *
 *  \param[in]  hcp_data            hcp_data is the pointer to the HCP 
 *                                  payload to which the data is to be
 *                                  appended.
 *  \param[in]  hcp_index           hcp_index is the index from which 
 *                                  the data source needs to be appended.
 *  \param[in]  src_data            src_data that is to be appended to the
 *                                  HCP packet.
 *  \param[in]  src_len             The length of the data source that is
 *                                  to be appended.
 *  \retval NONE.
 *
 */

extern
void
 phHciNfc_Append_HCPFrame (
                                uint8_t                 *hcp_data,
                                uint16_t                hcp_index,
                                uint8_t                 *src_data,
                                uint16_t                src_len
                          );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Allocate_Resource function allocates and initialises the 
 *  resource memory for the HCI layer.
 *
 *  \param[in] ppBuffer                 ppBuffer is the pointer to which the
 *                                      resource memory is allocated.
 *  \param[in] size                     Variable that specifies the size of
 *                                      the memory that needs to be created.
 *
 *  \retval NFCSTATUS_SUCCESS           The Resource Memory was allocated
 *                                      successfully .
 *  \retval 
 *  NFCSTATUS_INSUFFICIENT_RESOURCES    The memory could not be allocated 
 *                                      as required amount of memory 
 *                                      is not suffient.
 *
 */

extern
NFCSTATUS
 phHciNfc_Allocate_Resource (
                                void                **ppBuffer,
                                uint16_t            size
                            );
/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Release_Resources function releases all the resources 
 *  allocated in the HCI Layer.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *
 *  \retval NONE.
 *
 */

extern
 void
 phHciNfc_Release_Resources (
                                phHciNfc_sContext_t **ppsHciContext
                            );

/**
 * \ingroup grp_hci_nfc
 *
 *  The phHciNfc_Release_Lower function initiates the release of the 
 *  lower layers.
 *
 *  \param[in]  psHciContext            psHciContext is the context of
 *                                      the HCI Layer.
 *  \param[in]  pHwRef                  pHwRef is the Information of
 *                                      the Device Interface Link .
 *
 *  \retval NONE.
 *
 */

extern
void
phHciNfc_Release_Lower(
                    phHciNfc_sContext_t         *psHciContext,
                    void                        *pHwRef
               );



#endif

