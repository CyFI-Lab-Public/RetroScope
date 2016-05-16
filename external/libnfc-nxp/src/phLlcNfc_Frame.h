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
* \file  phLlcNfc_Frame.h
* \brief To append and delete the I or S or U frames.
*
* Project: NFC-FRI-1.1
*
* $Date: Fri Apr 30 10:03:36 2010 $
* $Author: ing02260 $
* $Revision: 1.19 $
* $Aliases: NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
*
*/

#ifndef PHLLCNFC_FRAME_H
#define PHLLCNFC_FRAME_H

/**
*  \name LLC NFC frame creation and deletion
*
* File: \ref phLlcNfc_Frame.h
*
*/
/*@{*/
#define PHLLCNFCFRAME_FILEREVISION "$Revision: 1.19 $" /**< \ingroup grp_hal_nfc_llc */
#define PHLLCNFCFRAME_FILEALIASES   "$Aliases: NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"    /**< \ingroup grp_hal_nfc_llc */
/*@}*/

/*************************** Includes *******************************/

/*********************** End of includes ****************************/

/** \defgroup grp_hal_nfc_llc_helper LLC helper functions
 *
 *
 *
 */

/***************************** Macros *******************************/
/** Maximum buffer that LLC can send and receive */
#define PH_LLCNFC_MAX_BUFLEN_RECV_SEND                      (33)
/** Maximum buffer that LLC can send and receive */
#define PH_LLCNFC_MIN_BUFLEN_RECVD                      (1)
/** Modulo to calculate the N(S) and N(R), when it extends 7, because 
    N(S) and N(R) can have the value maximum up to 7 */
#define PH_LLCNFC_MOD_NS_NR                                 (8)
/** When the entire LLC buffer is created or received, the header byte 
    will be the first byte (not the 0th byte which is the LLC length)
    of the buffer */
#define PH_LLCNFC_HEADER_BYTE_IN_BUFFER                     (1)
/** Maximum windows size, which is obtained by sending or 
    receiving the U - frame */
#define PH_LLCNFC_U_FRAME_MAX_WIN_SIZE                      (4)
/** Minimum windows size, which is obtained by sending or 
    receiving the U - frame */
#define PH_LLCNFC_U_FRAME_MIN_WIN_SIZE                      (2)
/** Start position of the U frame */
#define PH_LLCNFC_U_FRAME_START_POS                         (0)
/** No of position to set the U frame */
#define PH_LLCNFC_U_FRAME_NO_OF_POS                         (5)
/** This mask is to find the frame type ( S or U) */
#define PH_LLCNFC_LLC_HEADER_MASK                           (0xE0)
/** This mask is to find the frame type (I, S or U) */
#define PH_LLCNFC_I_FRM_HEADER_MASK                         (0x80)
/** If S frame is received or to be sent, the maximum length that 
    can be sent or received is 4 */
#define PH_LLCNFC_MAX_S_FRAME_LEN                           (4)
/** If S frame is received, to know the command type like RR, RNR, 
    REJ or SREJ */
#define PH_LLCNFC_S_FRAME_TYPE_MASK                         (0x18)
/** Maximum value of N(S) or N(R) */
#define PH_LLCNFC_I_S_FRAME_MAX_NR                          (0x07)
/** If U frame is received or to be sent, the maximum length that 
    can be sent or received is 7 */
#define PH_LLCNFC_U_FRAME_LEN                               (7)
/** If S frame is received, to know the command type like RSET or UA */
#define PH_LLCNFC_U_FRAME_MODIFIER_MASK                     (0x1F)
/** Extra length to be append to the user buffer Length to create the 
    LLC buffer */
#define PH_LLCNFC_LEN_APPEND                                (0x04)
/** U frame header without modifier */
#define PH_LLCNFC_U_HEADER_INIT                             (0xE0)
/** I frame header without N(S) and N(R) */
#define PH_LLCNFC_I_HEADER_INIT                             (0x80)
/** S frame header without type and N(R) */
#define PH_LLCNFC_S_HEADER_INIT                             (0xC0)
/** N(S) start bit position */
#define PH_LLCNFC_NS_START_BIT_POS                          (0x03)
/** N(R) start bit position */
#define PH_LLCNFC_NR_START_BIT_POS                          (0x00)
/** Number of bits N(R) and N(S)  */
#define PH_LLCNFC_NR_NS_NO_OF_BITS                          (0x03)
/** S frame type start bit position */
#define PH_LLCNFC_S_FRAME_TYPE_POS                          (0x03)
/** Number of bits (Type in S frame)  */
#define PH_LLCNFC_SFRAME_TYPE_NOOFBITS                      (0x02)
/** SREJ command */
#define PH_LLCNFC_SREJ_BYTE_VALUE                           (0x00)
/** Number of CRC bytes in a LLC packet */
#define PH_LLCNFC_NUM_OF_CRC_BYTES                          (0x02)

/* This macro is used as the input for the function "phLlcNfc_H_IFrameList_Peek" 
    and "phLlcNfc_H_SendTimedOutIFrame" functions. This values means, take the starting 
    position as the reference */
#define DEFAULT_PACKET_INPUT                                (0xFFU)
#define MAX_NS_NR_VALUE                                     (0x07U)

/************************ End of macros *****************************/

/********************** Callback functions **************************/

/******************* End of Callback functions **********************/

/********************* Structures and enums *************************/

/****************** End of structures and enums *********************/

/******************** Function declarations *************************/
/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC helper functions \b Frame Init function
*
* \copydoc page_reg Gets the LLC main context and stores it.
*
* \param[in] psLlcCtxt  Llc main structure information
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
void phLlcNfc_H_Frame_Init (
    phLlcNfc_Context_t  *psLlcCtxt
);

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC helper functions \b DeInit function
*
* \copydoc page_reg 
*
* \param[in] psFrameInfo    Frame structure information
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
void 
phLlcNfc_H_Frame_DeInit (
    phLlcNfc_Frame_t    *psFrameInfo
);

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC helper functions \b List append function
*
* \copydoc page_reg Append the new I frame information at the beginning of the list
*
* \param[in/out] psList     List inofrmation to know where shall the packet should be stored
* \param[in] packetInfo     Llc packet information
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
NFCSTATUS 
phLlcNfc_H_StoreIFrame (
    phLlcNfc_StoreIFrame_t      *psList,
    phLlcNfc_LlcPacket_t        sPacketInfo

);


/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC helper functions <b>Create S frame</b> function
*
* \copydoc page_reg This function creates a S frame
*
* \param[in/out] pllcSFrmBuf    Required buffer to create the S frame
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
void 
phLlcNfc_H_Create_S_Frame(
    uint8_t     *pllcSFrmBuf
);

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC helper functions <b>Compute CRC</b> function
*
* \copydoc page_reg This function is used to compute CRC for the llc data
*
* \param[in] pData      Llc data for which the CRC needs to be calculated
* \param[in] length     Length is the value till the CRC needs to be 
*                       calculated for the Llc data
* \param[in] pCrc1      1st CRC byte
* \param[in] pCrc2      2nd CRC byte
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
void 
phLlcNfc_H_ComputeCrc(
    uint8_t     *pData, 
    uint8_t     length,
    uint8_t     *pCrc1, 
    uint8_t     *pCrc2
);

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC helper functions <b>Create U frame payload </b> function
*
* \copydoc page_reg This function is used to create a LLC packet with U frame
*
* \param[in/out]    psLlcCtxt           Llc main structure information
* \param[in/out]    psLlcPacket         Llc packet sent by the upper layer
* \param[in/out]    pLlcPacketLength    Length of the llc packet
* \param[in]        cmdType             U frame has RSET/UA commands
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
NFCSTATUS
phLlcNfc_H_CreateUFramePayload (
    phLlcNfc_Context_t      *psLlcCtxt, 
    phLlcNfc_LlcPacket_t    *psLlcPacket, 
    uint8_t                 *pLlcPacketLength, 
    phLlcNfc_LlcCmd_t       cmdType
);

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC helper functions <b>Create I frame payload </b> function
*
* \copydoc page_reg This function is used to create a LLC packet with I frame
*
* \param[in/out]    psFrameInfo Information related to LLC frames are stored 
*                           in this structure
* \param[in/out]    psLlcPacket         Llc packet sent by the upper layer
* \param[in]        pLlcBuf     User given buffer or the buffer which needs LLC framing
* \param[in]        llcBufLength    Length of the parameter "pLlcBuf" 
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
NFCSTATUS
phLlcNfc_H_CreateIFramePayload (
    phLlcNfc_Frame_t        *psFrameInfo, 
    phLlcNfc_LlcPacket_t    *psLlcPacket, 
    uint8_t                 *pLlcBuf, 
    uint8_t                 llcBufLength
);

/**
 * \ingroup grp_hal_nfc_llc_helper
 *
 * \brief LLC helper functions <b>Process received frame </b> function
 *
 * \copydoc page_reg This function process the received data
 *
 * \param[in]   pLlcCtxt    Llc main context
 *
 * \retval NFCSTATUS_SUCCESS           Operation successful.
 * \retval NFCSTATUS_INVALID_FORMAT    If any error in the frame
 */
NFCSTATUS 
phLlcNfc_H_ProRecvFrame (
    phLlcNfc_Context_t      *psLlcCtxt
);

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC component <b>resend the I frame</b> function
*
* \copydoc page_reg This is a helper function which, sends back the timed out 
*   I frame to the PN544. This is due to the reason that ACK is not received 
*   from PN544 within the guard time-out value
*
* \param[in] psLlcCtxt          Llc main structure information
* \param[in/out] psListInfo     List of I frame information
* \param[in] ns_frame_no        Frame number to send (to send the first stored  
*                               frame send DEFAULT_PACKET_INPUT)
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
extern 
NFCSTATUS 
phLlcNfc_H_SendTimedOutIFrame (
    phLlcNfc_Context_t      *psLlcCtxt, 
    phLlcNfc_StoreIFrame_t  *psListInfo, 
    uint8_t                 ns_frame_no
);

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC state machine functions \b Change state function
*
* \copydoc page_reg changes the state if possible else returns error
*
* \param[in, out] psLlcCtxt     Llc main structure information
* \param[in] changeStateTo      Next state to change
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
extern 
NFCSTATUS 
phLlcNfc_H_ChangeState(
                       phLlcNfc_Context_t  *psLlcCtxt, 
                       phLlcNfc_State_t    changeStateTo
                       );

#ifdef CRC_ERROR_REJ
/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC send reject command function
*
* \copydoc page_reg Sends reject command, when CRC error is recieved
*
* \param[in, out] psLlcCtxt     Llc main structure information
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
extern 
NFCSTATUS 
phLlcNfc_H_SendRejectFrame(
                      phLlcNfc_Context_t  *psLlcCtxt
                      );
#endif /* #ifdef CRC_ERROR_REJ */

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC Write wait call function
*
* \copydoc page_reg Write that has been ignored earlier will be called in this function
*
* \param[in, out] psLlcCtxt     Llc main structure information
*
* \retval NFCSTATUS_SUCCESS                 Operation successful.
* \retval NFCSTATUS_BUSY                    Write is pended, so wait till it completes.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
extern 
NFCSTATUS  
phLlcNfc_H_WriteWaitCall (
    phLlcNfc_Context_t  *psLlcCtxt
    );

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC Send user frame function
*
* \copydoc page_reg Sends the stored user frame, that are not sent.
*
* \param[in, out]   psLlcCtxt       Llc main structure information
* \param[in]        psListInfo      Stored list of packets
*
* No return value
*
*/
NFCSTATUS 
phLlcNfc_H_SendUserIFrame (
    phLlcNfc_Context_t      *psLlcCtxt, 
    phLlcNfc_StoreIFrame_t  *psListInfo
    );

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC Send rejected frame function
*
* \copydoc page_reg Sends the stored rejected frame from PN544.
*
* \param[in, out]   psLlcCtxt       Llc main structure information
* \param[in]        psListInfo      Stored list of packets
* \param[in]        ns_rejected     N(S) that was rejected
*
* No return value
*
*/
NFCSTATUS 
phLlcNfc_H_SendRejectedIFrame (
    phLlcNfc_Context_t      *psLlcCtxt, 
    phLlcNfc_StoreIFrame_t  *psListInfo, 
    uint8_t                 ns_rejected
    );

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC component <b>Create S frame</b> function
*
* \copydoc page_reg This is a helper function which, creates the S frame
*
* \param[in/out] psFrameInfo    Generic frame information
* \param[in/out] psLlcPacket         Llc packet sent by the upper layer
* \param[in/out] cmdType        Command type of S frame
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
NFCSTATUS
phLlcNfc_H_CreateSFramePayload (
    phLlcNfc_Frame_t        *psFrameInfo,
    phLlcNfc_LlcPacket_t    *psLlcPacket,
    phLlcNfc_LlcCmd_t       cmdType
);

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC Send upper layer information function
*
* \copydoc page_reg Sends received information to the upper layer frame.
*
* \param[in, out]   psLlcCtxt       Llc main structure information
*
* No return value
*
*/
void 
phLlcNfc_H_SendInfo(
                    phLlcNfc_Context_t          *psLlcCtxt
                    );


/******************** Function declarations *************************/
#endif /* #ifndef PHLLCNFC_FRAME_H */


