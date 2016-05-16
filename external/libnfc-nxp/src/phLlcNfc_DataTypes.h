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
* \file  phLlcNfc_DataTypes.h
* \brief Contains the structure information.
*
* Project: NFC-FRI-1.1
*
* $Date: Fri Apr 30 10:03:36 2010 $
* $Author: ing02260 $
* $Revision: 1.43 $
* $Aliases: NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
*
*/

#ifndef PHLLCNFC_DATATYPES_H
#define PHLLCNFC_DATATYPES_H

/**
*  \name LLC NFC frame creation, deletion and processing
*
* File: \ref phLlcNfc_DataTypes.h
*
*/
/*@{*/
#define PH_LLCNFC_DATATYPES_FILEREVISION "$Revision: 1.43 $" /**< \ingroup grp_hal_nfc_llc */
#define PH_LLCNFC_DATATYPES_FILEALIASES  "$Aliases: NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"     /**< \ingroup grp_hal_nfc_llc */
/*@}*/
/*************************** Includes *******************************/
#include <phNfcCompId.h>
/*********************** End of includes ****************************/
/***************************** Macros *******************************/

/* Trace buffer declaration */
#if defined (LLC_TRACE)
    #include <phOsalNfc.h>
    #include <stdio.h>

    extern char                 phOsalNfc_DbgTraceBuffer[];
    #define trace_buffer        phOsalNfc_DbgTraceBuffer

    #define MAX_TRACE_BUFFER    150
    #define PH_LLCNFC_PRINT( str )  phOsalNfc_DbgString(str)
    #define PH_LLCNFC_PRINT_DATA(buf,len)
    #define PH_LLCNFC_STRING( str )
    #define PH_LLCNFC_DEBUG(str, arg) \
    {                                       \
        snprintf(trace_buffer,MAX_TRACE_BUFFER,str,arg);   \
        phOsalNfc_DbgString(trace_buffer);                 \
    }
    #define PH_LLCNFC_PRINT_BUFFER(buf,len)     \
    {                                       \
        /* uint16_t i = 0;                  \
        char        trace_buffer[MAX_TRACE_BUFFER];                    \
        snprintf(trace_buffer,MAX_TRACE_BUFFER,"\n\t %s:",msg);    \ 
        phOsalNfc_DbgString(trace);                 */\
        phOsalNfc_DbgTrace(buf,len);            \
        phOsalNfc_DbgString("\r");              \
    }
#endif /* #if defined (LLC_TRACE) */


#if (!defined (LLC_TRACE) && defined (LLC_DATA_BYTES))
    #include <phOsalNfc.h>

    extern char                 phOsalNfc_DbgTraceBuffer[];
    #define trace_buffer        phOsalNfc_DbgTraceBuffer

    #define PH_LLCNFC_PRINT( str )
    #define PH_LLCNFC_PRINT_BUFFER(buf, len) 
    #define PH_LLCNFC_DEBUG(str, arg1)
    #define PH_LLCNFC_STRING( str )  phOsalNfc_DbgString(str)
    #define PH_LLCNFC_PRINT_DATA(buf,len)     \
    {                                       \
        /* uint16_t i = 0;                  \
        char        trace_buffer[MAX_TRACE_BUFFER];                    \
        snprintf(trace_buffer,MAX_TRACE_BUFFER,"\n\t %s:",msg);    \ 
        phOsalNfc_DbgString(trace_buffer);                 */\
        phOsalNfc_DbgTrace(buf,len);            \
    }
#endif /* #if (!defined (LLC_TRACE) && defined (LLC_DATA_BYTES)) */


#if (!defined (LLC_TRACE) && !defined (LLC_DATA_BYTES))
    /** To disable prints */
    #define PH_LLCNFC_PRINT(str)
    #define PH_LLCNFC_PRINT_BUFFER(buf, len) 
    #define PH_LLCNFC_DEBUG(str, arg1)
    #define PH_LLCNFC_PRINT_DATA(buf,len)
    #define PH_LLCNFC_STRING( str )    
#endif /* #if (!defined (LLC_TRACE) && !defined (LLC_DATA_BYTES)) */


/* If the below MACRO (RECV_NR_CHECK_ENABLE) is
DEFINED : then check for the NR frame received from PN544 in the I frame is
    added. This shall be greater than sent NS from the HOST.
    This is used to stop the timer
COMMENTED : dont check the N(R) frame received from the PN544
*/    
/* #define RECV_NR_CHECK_ENABLE */

/* If the below MACRO (LLC_UPP_LAYER_NTFY_WRITE_RSP_CB) is
DEFINED : then if an I frame is received and the
        upper layer response callback (before another READ is pended) is called
        only after sending S frame and wait for the callback and then notify the
        upper layer 
COMMENTED : then if an I frame is received and the
            upper layer response callback (before another READ is pended) is called
            immediately after sending S frame (not waiting for the sent S frame
            callback)  
*/
/* #define LLC_UPP_LAYER_NTFY_WRITE_RSP_CB */

/* PN544 continuously sends an incorrect I frames to the HOST,
    even after the REJ frame from HOST to PN544
If the below MACRO (LLC_RR_INSTEAD_OF_REJ) is
DEFINED : then if the received NS = (expected NR - 1) then instead of REJ
        RR frame is sent
COMMENTED : then REJ frame is sent
*/
// #define LLC_RR_INSTEAD_OF_REJ

#define SEND_UFRAME

/* If the below MACRO (CTRL_WIN_SIZE_COUNT) is
DEFINED : then window size will be maximum
COMMENTED : then window size is 1
*/
#define CTRL_WIN_SIZE_COUNT

/* 
If the below MACRO (LLC_URSET_NO_DELAY) is
DEFINED : then after receiving the UA frame, then immediately this will be
            notified or further operation will be carried on.
COMMENTED : then after receiving the UA frame, then a timer is started, to
            delay the notifiation or to carry on the next operation
*/
#define LLC_URSET_NO_DELAY 

    /* 
    If the below MACRO (LLC_RELEASE_FLAG) is
DEFINED : then whenever LLC release is called the g_release_flag variable
                will be made TRUE. Also, NO notification is allowed to the
                upper layer.
COMMENTED : g_release_flag is not declared and not used
    */
    #define LLC_RELEASE_FLAG


 /* 
    Actually, there is a send and receive error count, if either of them reaches
    limit, then exception is raised.
    If the below MACRO (LLC_RSET_INSTEAD_OF_EXCEPTION) is
DEFINED : then exception is not raised, instead a U RSET command is sent.
COMMENTED : then exception is raised
    */
/* #define LLC_RSET_INSTEAD_OF_EXCEPTION */

#ifndef LLC_UPP_LAYER_NTFY_WRITE_RSP_CB
    /*
    If the below MACRO (PIGGY_BACK) is
    DEFINED : After receiving I frame, wait till the ACK timer to expire to send an ACK to PN544.
    COMMENTED : immediately ACK the received I frame
    */
    #define PIGGY_BACK

#endif /* LLC_UPP_LAYER_NTFY_WRITE_RSP_CB */

#define LLC_SEND_ERROR_COUNT

#define RECV_ERROR_FRAME_COUNT                      (0x50U)
#define SENT_ERROR_FRAME_COUNT                      (0x50U)

/** Initial bytes to read */
#define PH_LLCNFC_BYTES_INIT_READ                   (1)
/** Maximum buffer that I frame can send */
#define PH_LLCNFC_MAX_IFRAME_BUFLEN                 (29)
#define PH_LLCNFC_MAX_UFRAME_BUFLEN                 (4)
#define PH_LLCNFC_CRC_LENGTH                        (2)
#define PH_LLCNFC_MAX_LLC_PAYLOAD                   ((PH_LLCNFC_MAX_IFRAME_BUFLEN) + (4))
/** Maximum timer used in the Llc */
#define PH_LLCNFC_MAX_TIMER_USED                    (3)
/** Maximum timer used in the Llc */
#define PH_LLCNFC_MAX_ACK_GUARD_TIMER               (4)
/** Maximum I frame that can be stored */
#define PH_LLCNFC_MAX_I_FRAME_STORE                 (8)

/** Read pending for one byte */
#define PH_LLCNFC_READPEND_ONE_BYTE                 (0x01U)
    /** Read pending for remaining byte */
#define PH_LLCNFC_READPEND_REMAIN_BYTE              (0x02U)
/** Read pending not done */
#define PH_LLCNFC_READPEND_FLAG_OFF                 FALSE
#define PH_LLCNFC_MAX_REJ_RETRY_COUNT               (200)


/**** Macros for state machine ****/

typedef enum phLlcNfc_State
{
    /** This specifies that LLC is in uninitialise state */
    phLlcNfc_Uninitialise_State              =  0x00,
    /** This specifies that LLC initialise is in progress */
    phLlcNfc_Initialising_State              =  0x01,
    /** This specifies that LLC is in initialise is complete */
    phLlcNfc_Initialised_State               =  0x02,
    /** This specifies that LLC is send with the 
    lower layer */
    phLlcNfc_Sending_State                   =  0x03,
    /** This specifies that LLC is receive with the 
    lower layer */
    phLlcNfc_Receiving_State                 =  0x04,
    /** This specifies that LLC is receive wait with the 
    lower layer */
    phLlcNfc_ReceiveWait_State               =  0x05,
    /** This specifies that LLC is resending the I frames */
    phLlcNfc_Resend_State                    =  0x06
}phLlcNfc_State_t;
/**** Macros for state machine end ****/

/************************ End of macros *****************************/

/********************** Callback functions **************************/

/******************* End of Callback functions **********************/

/********************* Structures and enums *************************/
/**
*  \ingroup grp_hal_nfc_llc
*  \brief Enum to get the baud rate
*
*  This enum contains the baud rate information.
*
*/
/*@{*/
typedef enum phLlcNfc_LlcBaudRate
{
    /** Baud rate = 9600 */
    phLlcNfc_e_9600 = 0x00,
    /** Baud rate = 19200 */
    phLlcNfc_e_19200 = 0x01,
    /** Baud rate = 28800 */
    phLlcNfc_e_28800 = 0x02,
    /** Baud rate = 38400 */
    phLlcNfc_e_38400 = 0x03,
    /** Baud rate = 57600 */
    phLlcNfc_e_57600 = 0x04,
    /** Baud rate = 115200 */
    phLlcNfc_e_115200 = 0x05,
    /** Baud rate = 23400 */
    phLlcNfc_e_234000 = 0x06,
    /** Baud rate = 46800 */
    phLlcNfc_e_460800 = 0x07,
    /** Baud rate = 921600 */
    phLlcNfc_e_921600 = 0x08,
    /** Baud rate = 1228000 */
    phLlcNfc_e_1228000 = 0x09,
    /** Baud rate error */
    phLlcNfc_e_bdrate_err = 0xFF
}phLlcNfc_LlcBaudRate_t;
/*@}*/

/**
*  \ingroup grp_hal_nfc_llc
*  \brief Enum to select the U or I or S frame
*
*  This enum is to set the frames.
*
*/
/*@{*/
typedef enum phLlcNfc_LlcCmd
{
    /** This command is for I frame (no command) */
    phLlcNfc_e_no_cmd = 0xFF,
    /** This command is for U frame */
    phLlcNfc_e_rset = 0x19,
    /** This command is for U frame */
    phLlcNfc_e_ua = 0x06,
    /** This is RR command for S frame */
    phLlcNfc_e_rr = 0x00,
    /** This is REJ command for S frame */
    phLlcNfc_e_rej = 0x08,
    /** This is RNR command for S frame */
    phLlcNfc_e_rnr = 0x10,
    /** This is SREJ command for S frame */
    phLlcNfc_e_srej = 0x18,
    /** Error command */
    phLlcNfc_e_error = 0xFE
}phLlcNfc_LlcCmd_t;
/*@}*/

/**
*  \ingroup grp_hal_nfc_llc
*  \brief Enum to select the U or I or S frame
*
*  This enum is to set the frames.
*
*/
/*@{*/
typedef enum phLlcNfc_FrameType
{
    /** U frame type */
    phLlcNfc_eU_frame = 0x00,
    /** I frame type */
    phLlcNfc_eI_frame = 0x01,
    /** S frame type */
    phLlcNfc_eS_frame = 0x02, 
    /** Error frame type */
    phLlcNfc_eErr_frame = 0x03
}phLlcNfc_FrameType_t;
/*@}*/

/**
*  \ingroup grp_hal_nfc_llc
*  \brief LLC sent frame type
*
*  This enum values defines what are the frames sent to the PN544
*
*/
/*@{*/

typedef enum phLlcNfc_eSentFrameType 
{
    invalid_frame, 
    /* During initialisation the U RSET is sent to PN544 */
    init_u_rset_frame, 
    /* During initialisation the UA is sent to PN544 */
    init_u_a_frame, 
    /* After unsuccessful retries of sending I frame to PN544, 
        URSET is sent */
    u_rset_frame, 
    /* If PN544 sends the URSET frame in between any transaction, then 
       the UA response shall be sent */
    u_a_frame,
    /* S frame is sent to PN544, this will be sent only if an I frame 
        is received from PN544 */
    s_frame, 
    /* User has sent an I frame, for that a write response callback 
        shall be called */
    user_i_frame, 
    /* LLC, internally (means stored non acknowledged frames) has sent 
        an I frame as it doesnt get a proper acknowledgement */
    resend_i_frame, 
    /* LLC, internally (means stored non acknowledged frames) has sent 
        an I frame as it doesnt get a reject as acknowledgement */
    rejected_i_frame, 
    /* LLC has received a I frame for the re-sent I frames, so an S 
        frame is sent  */
    resend_s_frame, 
    /* LLC has received a I frame for the re-sent I frames, so an S 
        frame is sent  */
    resend_rej_s_frame, 
    /* PN544 has sent an I frame, which is wrong, so send a reject S 
        frame */
    reject_s_frame,
#ifdef LLC_RR_INSTEAD_OF_REJ

    /* RR is sent instead of REJECT */
    rej_rr_s_frame, 

#endif /* #ifdef LLC_RR_INSTEAD_OF_REJ */
    /* For any of the above sent frames, the response shall be received */
    write_resp_received
}phLlcNfc_eSentFrameType_t;

/*@}*/

/**
*  \ingroup grp_hal_nfc_llc
*  \brief LLC payload
*
*  This structure contains both the header information and 
*  the exact length of the buffer.
*
*/
/*@{*/
typedef struct phLlcNfc_Payload 
{
    /** Llc header information */
    uint8_t                 llcheader;

    /** User or received buffer */
    uint8_t                 llcpayload[PH_LLCNFC_MAX_LLC_PAYLOAD];
}phLlcNfc_Payload_t;
/*@}*/

/**
*  \ingroup grp_hal_nfc_llc
*  \brief Llc buffer
*
*  This structure contains the information of the LLC length byte 
* and payload.
*
*/
/*@{*/
typedef struct phLlcNfc_Buffer 
{
    /** Llc length */
    uint8_t                 llc_length_byte;

    /** LLC data including the LLC header and CRC */
    phLlcNfc_Payload_t      sllcpayload;
}phLlcNfc_Buffer_t;
/*@}*/

/**
*  \ingroup grp_hal_nfc_llc
*  \brief Packet information
*
*  This structure contains the length and buffer of the packet.
*
*/
/*@{*/
typedef struct phLlcNfc_LlcPacket 
{
    /** Complete LLC buffer */
    phLlcNfc_Buffer_t           s_llcbuf;
    
    /** LLC buffer length */
    uint8_t                     llcbuf_len;

    /** Stored frame needs completion callback, to be sent to HCI */
    phLlcNfc_eSentFrameType_t   frame_to_send;

}phLlcNfc_LlcPacket_t;
/*@}*/

/**
*  \ingroup grp_hal_nfc_llc_helper
*  \brief I frame details
*
*  This structure stores the information of the I frame 
* (to support sliding window).
*
*/
/*@{*/
typedef struct phLlcNfc_StoreIFrame 
{
    /** Complete LLC packet */
    phLlcNfc_LlcPacket_t        s_llcpacket[PH_LLCNFC_MAX_I_FRAME_STORE];

    /** Window size count */
    uint8_t                     winsize_cnt;

    /** Start position */
    uint8_t                     start_pos;
    
}phLlcNfc_StoreIFrame_t;
/*@}*/

/**
*  \ingroup grp_hal_nfc_llc
*  \brief LLC timer information
*
*  This structure contains the timer related information
*
*/
/*@{*/
typedef struct phLlcNfc_Timerinfo
{
    /** Store the timer id for each timer create */
    uint32_t                timer_id[PH_LLCNFC_MAX_TIMER_USED];
    
    /** This will store the connection time out value */
    uint16_t                con_to_value;

    /** This will store the guard time out values */
    uint16_t                guard_to_value[PH_LLCNFC_MAX_ACK_GUARD_TIMER];

    /** This will store the guard time out values */
    uint16_t                iframe_send_count[PH_LLCNFC_MAX_ACK_GUARD_TIMER];

    /** This will store ns value for the sent N(S) */
    uint8_t                 timer_ns_value[PH_LLCNFC_MAX_ACK_GUARD_TIMER];

    /** Each frame stored needs to be  */
    uint8_t                 frame_type[PH_LLCNFC_MAX_ACK_GUARD_TIMER];

    /** Index to re-send */
    uint8_t                 index_to_send;

    /** This is a count for gaurd time out */
    uint8_t                 guard_to_count;

#ifdef PIGGY_BACK
    /** This will store the ack time out values */
    uint16_t                ack_to_value;
#endif /* #ifdef PIGGY_BACK */

    /** This is a timer flag 
        Bit 0 = 1 means connection time out started else stopped
        Bit 1 = 1 means guard time out started else stopped
        Bit 2 = 1 means ack time out started else stopped
    */
    uint8_t                 timer_flag;    
}phLlcNfc_Timerinfo_t;
/*@}*/

/**
*  \ingroup grp_hal_nfc_llc
*  \brief LLC frame information
*
*  This structure contains the information of the LLC frame.
*
*/
/*@{*/
typedef struct phLlcNfc_Frame
{
    /** N(S) - Number of information frame */
    uint8_t                         n_s;

    /** N(R) - Number of next information frame to receive */
    uint8_t                         n_r;

    /** Store the window size */
    uint8_t                         window_size;

    /** SREJ is optional, so store the flag whether it is set or not */
    uint8_t                         srej_on_off;

    /** Store the baud rate */
    uint8_t                         baud_rate;

    /** Flag to find the rset_recvd */
    uint8_t                         rset_recvd;
    
    /** Complete LLC packet information */
    phLlcNfc_LlcPacket_t            s_llcpacket;

    /** Store the I frames, that has been sent, Storage will be till 
        the window size */
    phLlcNfc_StoreIFrame_t          s_send_store;

#ifdef PIGGY_BACK
    /** Store the I frames, that has been received, Storage will be 
        till the window size */
    phLlcNfc_StoreIFrame_t          s_recv_store;

    /** Response received count to send the ACK once it reaches the window size */
    uint8_t                         resp_recvd_count;
#endif /* #ifdef PIGGY_BACK */

    /** To receive the packet sent by below layer */
    phLlcNfc_LlcPacket_t            s_recvpacket;

    /** Number of window I frames has to be sent again */
    uint8_t                         rejected_ns;

    /** To store the count received error frames like 
        wrong CRC, REJ and RNR frames */
    uint8_t                         recv_error_count;

    /** Sending error frames like REJ frames to the PN544 */
    uint8_t                         send_error_count;

    /** Send U frame count  */
    uint8_t                         retry_cnt;

    /** Read pending flag, to know that read is already pended 
        or not. Use the below macros to ON and OFF the flag
        PH_LLCNFC_READPEND_FLAG_OFF 
        PH_LLCNFC_READPEND_ONE_BYTE 
        PH_LLCNFC_READPEND_REMAIN_BYTE
        */
    uint8_t                         read_pending;

    /** Write pending */
    uint8_t                         write_pending;

    /** Sent frame type */
    phLlcNfc_eSentFrameType_t       sent_frame_type;

    /** upper receive called */
    uint8_t                         upper_recv_call;

    /** Status returned during DAL write */
    NFCSTATUS                       write_status;

    /** Depending on the "write_status", write call has to be called */
    phLlcNfc_eSentFrameType_t       write_wait_call;
}phLlcNfc_Frame_t;
/*@}*/

/**
*  \ingroup grp_hal_nfc_llc
*  \brief LLC Component Context Structure
*
*  This structure is used to store the current context information 
*   of the instance.
*
*/
/*@{*/
typedef struct phLlcNfc_Context
{
    /** Information regarding all the LLC frame */
    phLlcNfc_Frame_t                s_frameinfo;

    /** Local send and receive */
    phNfc_sLowerIF_t                lower_if;

    /** Register attention, send and receive callback from the 
        register functions of the upper layer */
    phNfcIF_sCallBack_t             cb_for_if;

    /** Store the length, which shall be sent later through the 
        "send complete" callback */
    uint32_t                        send_cb_len;

    /** Receive buffer provided by the upper layer */
    uint8_t                         precv_buf[PH_LLCNFC_MAX_LLC_PAYLOAD];

    /** Receive length provided by the upper layer */
    uint32_t                        recvbuf_length;

    /** Llc state */
    phLlcNfc_State_t                state;

    /** Hardware information */
    void                            *phwinfo;

    /** Timer information */
    phLlcNfc_Timerinfo_t            s_timerinfo;
}phLlcNfc_Context_t;
/*@}*/
/****************** End of structures and enums *********************/

/******************** Function declarations *************************/

/******************** Function declarations *************************/
#endif /* PHLLCNFC_DATATYPES_H */


