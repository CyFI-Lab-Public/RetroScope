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
* \file  phLlcNfc_Frame.c
* \brief To append the I or S or U frames (LLC header).
*
* Project: NFC-FRI-1.1
*
* $Date: Tue Jun  1 14:41:26 2010 $
* $Author: ing02260 $
* $Revision: 1.88 $
* $Aliases: NFC_FRI1.1_WK1023_R35_1 $
*
*/

/*************************** Includes *******************************/
#include <phNfcTypes.h>
#include <phNfcStatus.h>
#include <phOsalNfc.h>
#include <phOsalNfc_Timer.h>
#include <phNfcInterface.h>
#include <phLlcNfc_DataTypes.h>
#include <phLlcNfc_Frame.h>
#include <phLlcNfc_Interface.h>
#include <phLlcNfc_Timer.h>
#ifdef ANDROID
#include <string.h>
#endif

/*********************** End of includes ****************************/

/***************************** Macros *******************************/

/************************ End of macros *****************************/

/***************************** Global variables *******************************/

#ifdef LLC_RELEASE_FLAG
    extern uint8_t             g_release_flag;
#endif /* #ifdef LLC_RELEASE_FLAG */
/************************ End of global variables *****************************/

/*********************** Local functions ****************************/
static 
void 
phLlcNfc_H_UpdateCrc(
    uint8_t     crcByte, 
    uint16_t    *pCrc
);

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC helper functions <b>process the received U frame</b> function
*
* \copydoc page_reg This function is to process the U frame received from 
*       the device
*
* \param[in] psLlcCtxt          Llc main structure information
* \param[in] llcPacket          LLC packet information, inlcuding length information
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
static 
NFCSTATUS 
phLlcNfc_H_ProcessUFrame (  
    phLlcNfc_Context_t      *psLlcCtxt 
);

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC helper functions <b>process the received S frame</b> function
*
* \copydoc page_reg This function is to process the S frame received from 
*       the device
*
* \param[in] psLlcCtxt          Llc main structure information
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
static 
void 
phLlcNfc_H_ProcessSFrame (  
    phLlcNfc_Context_t      *psLlcCtxt
);

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC helper functions <b>Update I frame list</b> function
*
* \copydoc page_reg This function checks the nr value with the stored I frames 
*   and deletes the nodes which has been acknowledged.
*
* \param[in/out] psFrameInfo    Frame structure information
* \param[in/out] psListInfo     List information
*
* \retval number of deleted frames
*
*/
static 
uint8_t 
phLlcNfc_H_UpdateIFrameList(
    phLlcNfc_Frame_t        *psFrameInfo, 
    phLlcNfc_StoreIFrame_t  *psListInfo
);

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC helper functions \b Delete list function
*
* \copydoc page_reg Delete the front node from the list
*
* \param[in] psFrameInfo    Frame structure information
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
static 
void 
phLlcNfc_H_DeleteIFrame (
                             phLlcNfc_StoreIFrame_t      *psList
                             );

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC helper functions <b>Get the LLC header</b> function
*
* \copydoc page_reg This function checks for the correctness fo the llc header
*
* \param[in] llcHeader      The byte which gives the header information
*
* \retval phLlcNfc_eU_frame      U frame type.
* \retval phLlcNfc_eI_frame      I frame type.
* \retval phLlcNfc_eS_frame      S frame type.
* \retval phLlcNfc_eErr_frame    Error type
*
*/
static
phLlcNfc_FrameType_t 
phLlcNfc_H_ChkGetLlcFrameType (
                               uint8_t     llcHeader
                               );

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC helper functions \b Peek the data function
*
* \copydoc page_reg Get the packet information from the front node from the list
*
* \param[in] psListInfo The List information
* \param[in] packetinfo The packet information from the front node of the list 
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
static
NFCSTATUS  
phLlcNfc_H_IFrameList_Peek (
                           phLlcNfc_StoreIFrame_t      *psList, 
                           phLlcNfc_LlcPacket_t        **psPacketinfo, 
                           uint8_t                     position
                           );

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC helper functions <b>Update U frame information</b> function
*
* \copydoc page_reg This function updates the U frame information in the 
*       phLlcNfc_sFrame_t structure
*
* \param[in/out] psFrameInfo        Frame information structure
* \param[in]     llcPayload         Llc payload information
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
static
NFCSTATUS 
phLlcNfc_H_Update_ReceivedRSETInfo (    
                            phLlcNfc_Frame_t    *psFrameInfo, 
                            phLlcNfc_Payload_t  llcInfo
                            );

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC Reset frame information function
*
* \copydoc page_reg resets ns and nr value, when ack for U frame is received
*
* \param[in, out] psLlcCtxt     Llc main structure information
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
static 
void 
phLlcNfc_H_ResetFrameInfo (
                      phLlcNfc_Context_t  *psLlcCtxt
                      );

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC Reset frame sending function
*
* \copydoc page_reg Send URSET frame to PN544
*
* \param[in, out] psLlcCtxt     Llc main structure information
*
* \retval NFCSTATUS_SUCCESS                 Operation successful.
* \retval NFCSTATUS_BUSY                    Write is pended, so wait till it completes.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
NFCSTATUS  
phLlcNfc_H_SendRSETFrame (
                      phLlcNfc_Context_t  *psLlcCtxt
                      );

/**
* \ingroup grp_hal_nfc_llc_helper
*
* \brief LLC helper functions <b>process the received I frame</b> function
*
* \copydoc page_reg This function is to process the I frame received from 
*       the device
*
* \param[in] psLlcCtxt          Llc main structure information
*
* \retval NFCSTATUS_SUCCESS                Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
*
*/
void 
phLlcNfc_H_ProcessIFrame (  
    phLlcNfc_Context_t      *psLlcCtxt 
);

/******************** End of Local functions ************************/

/********************** Global variables ****************************/

/******************** End of Global Variables ***********************/

void phLlcNfc_H_Frame_Init (
    phLlcNfc_Context_t  *psLlcCtxt
)
{
    
    if (NULL != psLlcCtxt)
    {
        /* Set all the other values to 0 */
        (void)memset (&psLlcCtxt->s_frameinfo.s_llcpacket, 0, 
                    sizeof(phLlcNfc_LlcPacket_t));

        psLlcCtxt->s_frameinfo.window_size = 
            PH_LLCNFC_U_FRAME_MAX_WIN_SIZE;
        /* Initialise the window size, N(S) and N(R) */
#ifdef PIGGY_BACK
        psLlcCtxt->s_frameinfo.s_recv_store.winsize_cnt = 0;
#endif
        psLlcCtxt->s_frameinfo.s_send_store.winsize_cnt = 0;
        psLlcCtxt->s_frameinfo.n_s = 0;
        psLlcCtxt->s_frameinfo.n_r = 0;
        psLlcCtxt->s_frameinfo.rejected_ns = DEFAULT_PACKET_INPUT;
    }
}

void 
phLlcNfc_H_Frame_DeInit (
    phLlcNfc_Frame_t    *psFrameInfo
)
{
    if (NULL != psFrameInfo)
    {
        /* Empty the frame information */
        (void)memset(&psFrameInfo->s_llcpacket, 0,
            sizeof(phLlcNfc_LlcPacket_t));
    }
}

NFCSTATUS
phLlcNfc_H_CreateUFramePayload (
    phLlcNfc_Context_t      *psLlcCtxt, 
    phLlcNfc_LlcPacket_t    *psLlcPacket, 
    uint8_t                 *pLlcPacketLength, 
    phLlcNfc_LlcCmd_t       cmdType
)
{
    /* 
        U frame packet (RSET)
        Byte 0 = Length (4 to 6 bytes) 
        Byte 1 = Header
        Byte 2 = window size (2 >= window size <= 4)
        Byte 3 = capabilities (SREJ option enable/disable) (optional)
        Byte 4 = Baud rate (optional)
        Byte 5 = CRC1
        Byte 6 = CRC2
    */
    NFCSTATUS           result = PHNFCSTVAL(CID_NFC_LLC, 
                                            NFCSTATUS_INVALID_PARAMETER);
    phLlcNfc_Buffer_t   *ps_llc_buf = NULL;
    uint8_t             index = 0;

    if ((NULL != psLlcCtxt) && (NULL != psLlcPacket) 
        && (NULL != pLlcPacketLength) && 
        ((phLlcNfc_e_rset == cmdType) || (phLlcNfc_e_ua == cmdType)))
    {
        result = NFCSTATUS_SUCCESS;
        ps_llc_buf = &(psLlcPacket->s_llcbuf);
        /* Get the header information */
        ps_llc_buf->sllcpayload.llcheader = 
                            (uint8_t)PH_LLCNFC_U_HEADER_INIT;
        if (phLlcNfc_e_rset == cmdType)
        {
            /* RSET command */
            ps_llc_buf->sllcpayload.llcheader = 
                        (uint8_t)SET_BITS8(
                            ps_llc_buf->sllcpayload.llcheader, 
                            PH_LLCNFC_U_FRAME_START_POS, 
                            PH_LLCNFC_U_FRAME_NO_OF_POS, 
                            (uint8_t)phLlcNfc_e_rset);
            /* Default window size */
            ps_llc_buf->sllcpayload.llcpayload[index] = 
                                PH_LLCNFC_U_FRAME_MAX_WIN_SIZE;
            index++;
            /* Endpoint capabilities, SREJ not supported */
            ps_llc_buf->sllcpayload.llcpayload[index] = 
                                PH_LLCNFC_SREJ_BYTE_VALUE;
            index++;
#ifdef ENABLE_BAUDRATE
            /* baud rate 0x00 = 9600, 0x05 = 115200 */
            ps_llc_buf->sllcpayload.llcpayload[index] = 
                            (uint8_t)phLlcNfc_e_115200;
            index++;
#endif /* #ifdef ENABLE_BAUDRATE */
            
        }
        else
        {
            /* UA frame */
            ps_llc_buf->sllcpayload.llcheader = (uint8_t)
                SET_BITS8(ps_llc_buf->sllcpayload.llcheader, 
                            PH_LLCNFC_U_FRAME_START_POS,
                            PH_LLCNFC_U_FRAME_NO_OF_POS, 
                            (uint8_t)phLlcNfc_e_ua);          
        }
        /* LLC length byte updated (index + 2 CRC bytes + 
        1 byte of header) */
        ps_llc_buf->llc_length_byte = (index + 
                            PH_LLCNFC_NUM_OF_CRC_BYTES + 1);
        /* Total LLC buffer size */
        *pLlcPacketLength = psLlcPacket->llcbuf_len = 
                        (ps_llc_buf->llc_length_byte + 1);
        /* 
        psLlcPacket->s_llcbuf : 
                consists llc length byte + llc header + data + CRC 
                (which needs to be calculated by the below function)
        psLlcPacket->llcbuf_len : 
                Total length of the above buffer
        psLlcPacket->llcbuf_len - 2 : 
                -2 because the CRC has to be calculated, only for the 
                bytes which has llc length byte + llc header + data. 
                But total length (llcbuf_len) consists of above mentioned 
                things with 2 byte CRC 
        psLlcPacket->s_llcbuf.sllcpayload.llcpayload : 
                consists only data (no length byte and no llc header)
        (psLlcPacket->llcbuf_len - 4) : 
                is the array index of the first CRC byte to be calculated
        (psLlcPacket->llcbuf_len - 3) : 
                is the array index of the second CRC byte to be calculated
        */
        index = psLlcPacket->llcbuf_len;

        phLlcNfc_H_ComputeCrc((uint8_t *)ps_llc_buf, 
                (psLlcPacket->llcbuf_len - 2),
                &(ps_llc_buf->sllcpayload.llcpayload[(index - 4)]),
                &(ps_llc_buf->sllcpayload.llcpayload[(index - 3)]));
    }

    return result;
}

NFCSTATUS
phLlcNfc_H_CreateSFramePayload (
    phLlcNfc_Frame_t    *psFrameInfo, 
    phLlcNfc_LlcPacket_t    *psLlcPacket,
    phLlcNfc_LlcCmd_t   cmdType
)
{
    /* 
        S frame packet (RR or RNR or REJ or SREJ). Total bytes = 4
        Byte 0 = Length (Length = 3 always for S frame) 
        Byte 1 = Header
        Byte 2 = CRC1
        Byte 3 = CRC2
    */
    NFCSTATUS               result = NFCSTATUS_SUCCESS;
    phLlcNfc_Buffer_t       *ps_llc_buf = NULL;
    uint8_t                 length = 0;
    
    ps_llc_buf = &(psLlcPacket->s_llcbuf);

    /* Initial S frame header */
    ps_llc_buf->sllcpayload.llcheader = PH_LLCNFC_S_HEADER_INIT;
    /* Update the N(R) value */
    ps_llc_buf->sllcpayload.llcheader = (uint8_t)SET_BITS8(
                                ps_llc_buf->sllcpayload.llcheader, 
                                PH_LLCNFC_NR_START_BIT_POS, 
                                PH_LLCNFC_NR_NS_NO_OF_BITS, 
                                psFrameInfo->n_r);

    /* Update the type bits of S frame */
    ps_llc_buf->sllcpayload.llcheader = (uint8_t)
                (ps_llc_buf->sllcpayload.llcheader | (uint8_t)cmdType);

    /* Maximum S frame length */
    psLlcPacket->llcbuf_len = (uint8_t)PH_LLCNFC_MAX_S_FRAME_LEN;
    /* S frame length byte value */
    ps_llc_buf->llc_length_byte = (uint8_t)
                            (psLlcPacket->llcbuf_len - 1);

    /* 
        psFrameInfo->s_llcpacket.s_llcbuf : 
                consists llc length byte + llc header + data + CRC 
                (which needs to be calculated by the below function)
        psFrameInfo->s_llcpacket.llcbuf_len : 
                Total length of the above buffer
        psFrameInfo->s_llcpacket.llcbuf_len - 2 : 
                -2 because the CRC has to be calculated, only for the 
                bytes which has llc length byte + llc header + data. 
                But total length (llcbuf_len) consists of above mentioned 
                things with 2 byte CRC 
        psFrameInfo->s_llcpacket.s_llcbuf.sllcpayload.llcpayload : 
                consists only data (no length byte and no llc header)
        psFrameInfo->s_llcpacket.s_llcbuf.sllcpayload.llcpayload : 
                contains only data sent by user. 
        (psFrameInfo->s_llcpacket.llcbuf_len - 4) : 
                is the array index of the first CRC byte to be calculated
        (psFrameInfo->s_llcpacket.llcbuf_len - 3) : 
                is the array index of the second CRC byte to be calculated
    */
    length = psLlcPacket->llcbuf_len;
    phLlcNfc_H_ComputeCrc(
        (uint8_t *)ps_llc_buf, (length - 2),
        &(ps_llc_buf->sllcpayload.llcpayload[(length - 4)]),
        &(ps_llc_buf->sllcpayload.llcpayload[(length - 3)]));

    return result;
}

NFCSTATUS
phLlcNfc_H_CreateIFramePayload (
    phLlcNfc_Frame_t        *psFrameInfo, 
    phLlcNfc_LlcPacket_t    *psLlcPacket, 
    uint8_t                 *pLlcBuf, 
    uint8_t                 llcBufLength
)
{
    NFCSTATUS           result = PHNFCSTVAL(CID_NFC_LLC, 
                                            NFCSTATUS_INVALID_PARAMETER);
    phLlcNfc_Buffer_t   *ps_llc_buf = NULL;

    if ((NULL != psFrameInfo) && (NULL != psLlcPacket) && 
        (NULL != pLlcBuf) && (llcBufLength > 0))
    {
        result = NFCSTATUS_SUCCESS;
        ps_llc_buf = &(psLlcPacket->s_llcbuf);

        (void)memcpy(&(ps_llc_buf->sllcpayload.llcpayload[0]),
                        pLlcBuf, llcBufLength);

        psLlcPacket->llcbuf_len = (uint8_t)llcBufLength; 
        
        /* I frame header byte */
        ps_llc_buf->sllcpayload.llcheader = PH_LLCNFC_I_HEADER_INIT;

        /* Set the N(S) value */
        ps_llc_buf->sllcpayload.llcheader = (uint8_t)
            SET_BITS8(
                    ps_llc_buf->sllcpayload.llcheader, 
                    PH_LLCNFC_NS_START_BIT_POS, 
                    PH_LLCNFC_NR_NS_NO_OF_BITS, 
                    psFrameInfo->n_s);

        /* Set the N(R) value */
        ps_llc_buf->sllcpayload.llcheader = (uint8_t)
            SET_BITS8(
                    ps_llc_buf->sllcpayload.llcheader, 
                    PH_LLCNFC_NR_START_BIT_POS, 
                    PH_LLCNFC_NR_NS_NO_OF_BITS, 
                    psFrameInfo->n_r);
            
        /* Update the length byte, llc length byte value includes 
            data + CRC bytes + llc length byte */
        ps_llc_buf->llc_length_byte = 
                (psLlcPacket->llcbuf_len + 
                PH_LLCNFC_NUM_OF_CRC_BYTES + 1);

        /* Update total length, Total length is always equal to 
            llc length byte + 1 */
        psLlcPacket->llcbuf_len = 
                (ps_llc_buf->llc_length_byte + 1);
        
        /* 
            psFrameInfo->s_llcpacket.s_llcbuf : 
                    consists llc length byte + llc header + data + CRC (which 
                    needs to be calculated by the below function)
            psFrameInfo->s_llcpacket.llcbuf_len : 
                    Total length of the above buffer
            psFrameInfo->s_llcpacket.llcbuf_len - 2 : 
                    -2 because the CRC has to be calculated, only for the 
                    bytes which has llc length byte + llc header + data. 
                    But total length (llcbuf_len) consists of above mentioned 
                    things with 2 byte CRC 
            psFrameInfo->s_llcpacket.s_llcbuf.sllcpayload.llcpayload : 
                    contains only data sent by user. 
            (psFrameInfo->s_llcpacket.llcbuf_len - 4) : 
                    is the array index of the first CRC byte to be calculated
            (psFrameInfo->s_llcpacket.llcbuf_len - 3) : 
                    is the array index of the second CRC byte to be calculated

        */
        phLlcNfc_H_ComputeCrc(
            (uint8_t*)ps_llc_buf, 
            (psLlcPacket->llcbuf_len - 2),
            &(ps_llc_buf->sllcpayload.llcpayload
                        [(psLlcPacket->llcbuf_len - 4)]), 
            &(ps_llc_buf->sllcpayload.llcpayload
                        [(psLlcPacket->llcbuf_len - 3)]));


    }

    return result;
}

static
phLlcNfc_FrameType_t 
phLlcNfc_H_ChkGetLlcFrameType (
    uint8_t llcHeader
)
{
    phLlcNfc_FrameType_t    frame_type = phLlcNfc_eErr_frame;

    /* Mask the header byte to know the actual frame types */
    switch((llcHeader & PH_LLCNFC_LLC_HEADER_MASK))
    {
        case PH_LLCNFC_U_HEADER_INIT:
        {
            frame_type = phLlcNfc_eU_frame;
            break;
        }

        case PH_LLCNFC_S_HEADER_INIT:
        {
            frame_type = phLlcNfc_eS_frame;
            break;
        }

        default:
        {
            if (PH_LLCNFC_I_HEADER_INIT == 
                (PH_LLCNFC_I_FRM_HEADER_MASK & llcHeader))
            {
                frame_type = phLlcNfc_eI_frame;
            }
            else
            {
                frame_type = phLlcNfc_eErr_frame;
            }
            break;
        }
    }
    return frame_type;
}

static
NFCSTATUS 
phLlcNfc_H_Update_ReceivedRSETInfo (    
    phLlcNfc_Frame_t    *psFrameInfo, 
    phLlcNfc_Payload_t  llcInfo
)
{
    NFCSTATUS   result = PHNFCSTVAL(CID_NFC_LLC, NFCSTATUS_INVALID_FORMAT);
    uint8_t     payload_index = 0;

    if ((llcInfo.llcpayload[payload_index] >= PH_LLCNFC_U_FRAME_MIN_WIN_SIZE) && 
        (llcInfo.llcpayload[payload_index] <= PH_LLCNFC_U_FRAME_MAX_WIN_SIZE))
    {
        result = NFCSTATUS_SUCCESS;
        /* From the received buffer, get the window size from the 
            3rd byte (recvUBufLen[3]) of the buffer */
        psFrameInfo->window_size = llcInfo.llcpayload[payload_index];
        payload_index = (uint8_t)(payload_index + 1);

        /* If 4th byte of the receive buffer (pRecvUBuf[4]) is 
            0x01 then SREJ can come from the device*/
        psFrameInfo->srej_on_off = ((PH_LLCNFC_SREJ_BYTE_VALUE == 
                        llcInfo.llcpayload[payload_index])?
                        PH_LLCNFC_SREJ_BYTE_VALUE:0);

        /* For present implementation, this should be always false 
        later stage remove if statement to work */
        if (PH_LLCNFC_SREJ_BYTE_VALUE != psFrameInfo->srej_on_off)
        {
            result = PHNFCSTVAL(CID_NFC_LLC, NFCSTATUS_INVALID_FORMAT);
        }
        else
        {
            payload_index = (uint8_t)(payload_index + 1);

            
            if (llcInfo.llcpayload[payload_index] > 
                (uint8_t)phLlcNfc_e_1228000)
            {
                /* Error byte */
                result = PHNFCSTVAL(CID_NFC_LLC, NFCSTATUS_INVALID_FORMAT);
            }
            else
            {
                /* Get the baud rate from the 5th byte of the receive buffer */
                psFrameInfo->baud_rate = (phLlcNfc_LlcBaudRate_t)
                                        (llcInfo.llcpayload[payload_index]);
            }
        }
    }

    return result;
}

static 
uint8_t 
phLlcNfc_H_UpdateIFrameList(
    phLlcNfc_Frame_t        *psFrameInfo, 
    phLlcNfc_StoreIFrame_t  *psListInfo
)
{
    NFCSTATUS               result = NFCSTATUS_SUCCESS;
    phLlcNfc_LlcPacket_t    *pspktInfo = NULL;
    uint8_t                 while_exit = FALSE;
    uint8_t                 nr = 0;
    uint8_t                 ns = 0;
    uint8_t                 no_of_del_frames = 0;

    PHNFC_UNUSED_VARIABLE(result);
    if(0 == psListInfo->winsize_cnt)
    {
        while_exit = TRUE;
    }
    while (FALSE == while_exit)
    {
        /* Get the first node from the list */
        result = phLlcNfc_H_IFrameList_Peek (psListInfo, &pspktInfo, 
                                            DEFAULT_PACKET_INPUT);
        if (NULL != pspktInfo)
        {
            /* Get the N(R) value of the received packet and N(S) value of the 
                sent stored i frame */
            ns = (uint8_t)GET_BITS8 ( 
                    pspktInfo->s_llcbuf.sllcpayload.llcheader, 
                    PH_LLCNFC_NS_START_BIT_POS, 
                    PH_LLCNFC_NR_NS_NO_OF_BITS);

            nr = (uint8_t)GET_BITS8( 
                    psFrameInfo->s_recvpacket.s_llcbuf.sllcpayload.llcheader, 
                    PH_LLCNFC_NR_START_BIT_POS, 
                    PH_LLCNFC_NR_NS_NO_OF_BITS);            
            
            /* Check the value of each i frame N(S) and 
                received ACKs N(R) */
#if 0
            if(((ns <= nr) && ((nr - ns) <= psFrameInfo->window_size)) 
                || ((ns > nr) && (((PH_LLCNFC_MOD_NS_NR + nr) - ns) <= 
                PH_LLCNFC_U_FRAME_MAX_WIN_SIZE)))
#endif
            if(((ns < nr) && ((nr - ns) <= psFrameInfo->window_size)) 
                || ((ns > nr) && (((PH_LLCNFC_MOD_NS_NR + nr) - ns) <= 
                PH_LLCNFC_U_FRAME_MAX_WIN_SIZE)))
            {
                phLlcNfc_H_DeleteIFrame (psListInfo);
                no_of_del_frames = (uint8_t)(no_of_del_frames + 1);
            }
            else
            {
                while_exit = TRUE;
            }

            if(0 == psListInfo->winsize_cnt)
            {
                while_exit = TRUE;
            }
        }
        else
        {
            while_exit = TRUE;
        }
    }
    return no_of_del_frames;
}

NFCSTATUS 
phLlcNfc_H_SendUserIFrame (
    phLlcNfc_Context_t      *psLlcCtxt, 
    phLlcNfc_StoreIFrame_t  *psListInfo
)
{
    NFCSTATUS               result = NFCSTATUS_SUCCESS;    
    phLlcNfc_Frame_t        *ps_frame_info = NULL;
    phLlcNfc_LlcPacket_t    s_create_packet;
    phLlcNfc_LlcPacket_t    *ps_get_packet = NULL;
    phLlcNfc_Payload_t      *ps_llc_payload = NULL;
    phLlcNfc_StoreIFrame_t  *ps_store_frame = NULL;
    uint8_t                 llc_header = 0, 
                            length = 0;

    if ((NULL == psLlcCtxt) || (NULL == psListInfo))
    {
        result = PHNFCSTVAL (CID_NFC_LLC, NFCSTATUS_INVALID_PARAMETER);
    }
    else if (0 == psListInfo->winsize_cnt)
    {
        result = PHNFCSTVAL (CID_NFC_LLC, NFCSTATUS_NOT_ALLOWED);
    }
    else
    {
        ps_frame_info = &(psLlcCtxt->s_frameinfo);
        ps_store_frame = &(ps_frame_info->s_send_store);

        if (
            (ps_frame_info->n_s != ((ps_store_frame->winsize_cnt + 
            ps_store_frame->start_pos) % PH_LLCNFC_MOD_NS_NR))
            )
        {
            /* Get the stored I frame, only if the new frame is sent 
                from the upper layer */
            result = phLlcNfc_H_IFrameList_Peek (psListInfo, &ps_get_packet,
                                                ps_frame_info->n_s);
        }

        if (NULL != ps_get_packet)
        {
            llc_header = ps_get_packet->s_llcbuf.sllcpayload.llcheader;

            /* Update n(r) value for the header */
            llc_header = (uint8_t)(llc_header | ps_frame_info->n_r);

            /* Create the packet */
            (void)memcpy ((void *)&(s_create_packet), (void *)ps_get_packet,
                        sizeof (phLlcNfc_LlcPacket_t));

            s_create_packet.s_llcbuf.sllcpayload.llcheader = llc_header;
            ps_llc_payload = &(s_create_packet.s_llcbuf.sllcpayload);

            /* Length of the complete llc buffer, sent to PN544 */
            length = s_create_packet.llcbuf_len;

            /* Compute CRC for the created packet */
            phLlcNfc_H_ComputeCrc ((uint8_t *)&(s_create_packet.s_llcbuf),
                        (length - 2),
                        (uint8_t *)&(ps_llc_payload->llcpayload[(length - 4)]), 
                        (uint8_t *)&(ps_llc_payload->llcpayload[(length - 3)]));

            /* Send the i frame */
            result = phLlcNfc_Interface_Write (psLlcCtxt,
                            (uint8_t *)&(s_create_packet.s_llcbuf),
                            (uint32_t)s_create_packet.llcbuf_len);

            ps_frame_info->write_status = result;

            if (NFCSTATUS_BUSY == PHNFCSTATUS (result))
            {
                /* The below check is added because, write is already pended by some other 
                    operation, so it has to complete, when it completes, then immediately 
                    next write shall be called using the below updated variable 

                    The below variable is checked for the resend or rejected i frame 
                    because if due to timer or reject frame from PN544, an I frame 
                    has been sent (means write has been pended then the variable shall 
                    not be overwritten.
                */
                ps_frame_info->write_wait_call = (phLlcNfc_eSentFrameType_t)
                            (((resend_i_frame == ps_frame_info->write_wait_call) || 
                            (rejected_i_frame == ps_frame_info->write_wait_call)) ? 
                            ps_frame_info->write_wait_call : user_i_frame);
            }
            else
            {
                if (NFCSTATUS_PENDING == result)
                {
                    /* Start the timer */
                    (void)phLlcNfc_StartTimers (PH_LLCNFC_GUARDTIMER, 
                                                ps_frame_info->n_s);
                    
                    /* "sent_frame_type is updated" only if the data is 
                        written to the lower layer */
                    ps_frame_info->sent_frame_type = user_i_frame;
                }
            }
        }
    }
    return result;
}

NFCSTATUS 
phLlcNfc_H_SendRejectedIFrame (
    phLlcNfc_Context_t      *psLlcCtxt, 
    phLlcNfc_StoreIFrame_t  *psListInfo, 
    uint8_t                 ns_rejected
)
{
    NFCSTATUS               result = NFCSTATUS_SUCCESS;    
    phLlcNfc_Frame_t        *ps_frame_info = NULL;
    phLlcNfc_LlcPacket_t    s_create_packet;
    phLlcNfc_LlcPacket_t    *ps_get_packet = NULL;
    phLlcNfc_Payload_t      *ps_llc_payload = NULL;
    phLlcNfc_StoreIFrame_t  *ps_store_frame = NULL;
    uint8_t                 llc_header = 0;
    uint8_t                 length = 0;

    if ((NULL == psLlcCtxt) || (NULL == psListInfo))
    {
        result = PHNFCSTVAL (CID_NFC_LLC, NFCSTATUS_INVALID_PARAMETER);
    }
    else if (0 == psListInfo->winsize_cnt)
    {
        result = PHNFCSTVAL (CID_NFC_LLC, NFCSTATUS_NOT_ALLOWED);
    }
    else
    {
        ps_frame_info = &(psLlcCtxt->s_frameinfo);
        ps_store_frame = &(ps_frame_info->s_send_store);


        if (ns_rejected < (uint8_t)(ps_store_frame->winsize_cnt + 
            ps_store_frame->start_pos))
        {
            /* To send rejected frame, first thing shall be done is 
                windows size count shall be checked. if the 
                start position 
                */
            if (invalid_frame != 
                ps_store_frame->s_llcpacket[ns_rejected].frame_to_send)
            {
                /* Above check is added to know only if   */
                result = phLlcNfc_H_IFrameList_Peek (psListInfo, &ps_get_packet,
                                                    ns_rejected);
            }
            else
            {
                ps_frame_info->rejected_ns = DEFAULT_PACKET_INPUT;
                /* Get the stored I frame, only if the new frame is sent 
                    from the upper layer */
                result = phLlcNfc_H_SendUserIFrame (psLlcCtxt, psListInfo);
            }
        }

        if (NULL != ps_get_packet)
        {
            llc_header = ps_get_packet->s_llcbuf.sllcpayload.llcheader;

            /* Update n(r) value for the header */
            llc_header = (uint8_t)(llc_header | ps_frame_info->n_r);

            /* Create the packet */
            (void)memcpy ((void *)&(s_create_packet), (void *)ps_get_packet,
                        sizeof (phLlcNfc_LlcPacket_t));

            s_create_packet.s_llcbuf.sllcpayload.llcheader = llc_header;
            ps_llc_payload = &(s_create_packet.s_llcbuf.sllcpayload);

            /* Length of the complete llc buffer, sent to PN544 */
            length = s_create_packet.llcbuf_len;

            /* Compute CRC for the created packet */
            phLlcNfc_H_ComputeCrc ((uint8_t *)&(s_create_packet.s_llcbuf),
                        (length - 2),
                        (uint8_t *)&(ps_llc_payload->llcpayload[(length - 4)]), 
                        (uint8_t *)&(ps_llc_payload->llcpayload[(length - 3)]));

            /* Send the i frame */
            result = phLlcNfc_Interface_Write (psLlcCtxt, 
                            (uint8_t *)&(s_create_packet.s_llcbuf),
                            (uint32_t)s_create_packet.llcbuf_len);

            ps_frame_info->write_status = result;

            if (NFCSTATUS_BUSY == PHNFCSTATUS (result))
            {
                /* Already a frame is sent and response is waited for the sent frame, 
                    so update the below variable */
                ps_frame_info->write_wait_call = (phLlcNfc_eSentFrameType_t)
                                (((ns_rejected != ps_store_frame->start_pos) && 
                                (resend_i_frame != ps_frame_info->write_wait_call))?
                                rejected_i_frame : ps_frame_info->write_wait_call);
            }
            else
            {
                /* NFCSTATUS_PENDING means that the no other write is pending, apart  
                    from the present write 

                Start the timer */
                (void)phLlcNfc_StartTimers (PH_LLCNFC_GUARDTIMER, ns_rejected);
                
                /* "sent_frame_type is updated" only if the data is 
                    written to the lower layer. This will be used in the write 
                    response callback and also indicates, what is the frame sent 
                    and why
                 */
                ps_frame_info->sent_frame_type = rejected_i_frame;

                if ((ns_rejected + 1) < ps_frame_info->n_s)
                {
                    ps_frame_info->rejected_ns = (uint8_t)(ns_rejected + 1);

                    ps_frame_info->write_status = PHNFCSTVAL(CID_NFC_LLC, 
                                                    NFCSTATUS_BUSY);

                    if (invalid_frame == 
                        ps_store_frame->s_llcpacket[ns_rejected].frame_to_send)
                    {
                        ps_frame_info->rejected_ns = DEFAULT_PACKET_INPUT;
                        ps_frame_info->write_wait_call = user_i_frame; 
                    }
                    else
                    {                        
                        ps_frame_info->write_wait_call = rejected_i_frame;
                    }
                }
                else
                {
                    ps_frame_info->rejected_ns = DEFAULT_PACKET_INPUT;
                    /* This check is added to see that new frame has arrived 
                        from the upper layer */
                    if (ps_frame_info->n_s < (ps_store_frame->start_pos + 
                        ps_store_frame->winsize_cnt))
                    {
                        ps_frame_info->write_wait_call = user_i_frame;
                    }
                }
            }
        }
    }
    return result;
}

NFCSTATUS 
phLlcNfc_H_SendTimedOutIFrame (
    phLlcNfc_Context_t      *psLlcCtxt, 
    phLlcNfc_StoreIFrame_t  *psListInfo, 
    uint8_t                 frame_to_send
)
{
    NFCSTATUS               result = NFCSTATUS_SUCCESS;    
    phLlcNfc_Frame_t        *ps_frame_info = NULL;
    phLlcNfc_Timerinfo_t    *ps_timer_info = NULL;
    phLlcNfc_LlcPacket_t    s_create_packet;
    phLlcNfc_LlcPacket_t    *ps_get_packet = NULL;
    phLlcNfc_Payload_t      *ps_llc_payload = NULL;
    phLlcNfc_StoreIFrame_t  *ps_store_frame = NULL;
        
    PHNFC_UNUSED_VARIABLE(frame_to_send);
    if((NULL == psLlcCtxt) || (NULL == psListInfo))
    {
        result = PHNFCSTVAL(CID_NFC_LLC, NFCSTATUS_INVALID_PARAMETER);
    }
    else if (psListInfo->winsize_cnt == 0)
    {
        result = PHNFCSTVAL(CID_NFC_LLC, NFCSTATUS_NOT_ALLOWED);
    }
    else
    {
        uint8_t                 llc_header = 0;
        uint8_t                 length = 0;
        uint8_t                 timer_count = 0;
        uint8_t                 timer_index = 0;
        uint8_t                 ns_index = 0;

        ps_frame_info = &(psLlcCtxt->s_frameinfo);
        ps_timer_info = &(psLlcCtxt->s_timerinfo);
        ps_store_frame = &(ps_frame_info->s_send_store);        
        
        timer_index = ps_timer_info->index_to_send;
        timer_count = ps_timer_info->guard_to_count;
        ns_index = ps_timer_info->timer_ns_value[timer_index];

        PH_LLCNFC_DEBUG("SEND TIMEOUT CALL WIN SIZE CNT : 0x%02X\n", ps_store_frame->winsize_cnt);
        PH_LLCNFC_DEBUG("SEND TIMEOUT CALL START POS : 0x%02X\n", ps_store_frame->start_pos);
        PH_LLCNFC_DEBUG("SEND TIMEOUT CALL N S value : 0x%02X\n", ps_frame_info->n_s);
        PH_LLCNFC_DEBUG("SEND TIMEOUT TIMER INDEX : 0x%02X\n", timer_index);
        PH_LLCNFC_DEBUG("SEND TIMEOUT CALL frame type : 0x%02X\n", ps_timer_info->frame_type[timer_index]);

        if (resend_i_frame == ps_timer_info->frame_type[timer_index])
        {
            /* Get the stored I frame */
            result = phLlcNfc_H_IFrameList_Peek (psListInfo, &ps_get_packet,
                                                ns_index);
        }        

        PH_LLCNFC_DEBUG("SEND TIMEOUT CALL Packet : 0x%p\n", ps_get_packet);
        if (NULL != ps_get_packet)
        {
            llc_header = ps_get_packet->s_llcbuf.sllcpayload.llcheader;

            /* Update n(r) value for the header */
            llc_header = (uint8_t)(llc_header | ps_frame_info->n_r);

            /* create the packet */
            (void)memcpy ((void *)&(s_create_packet), (void *)ps_get_packet,
                        sizeof (phLlcNfc_LlcPacket_t));

            s_create_packet.s_llcbuf.sllcpayload.llcheader = llc_header;
            ps_llc_payload = &(s_create_packet.s_llcbuf.sllcpayload);

            /* Length of the complete llc buffer, sent to PN544 */
            length = s_create_packet.llcbuf_len;

            /* Compute CRC */
            phLlcNfc_H_ComputeCrc((uint8_t *)&(s_create_packet.s_llcbuf),
                        (length - 2),
                        (uint8_t *)&(ps_llc_payload->llcpayload[(length - 4)]),
                        (uint8_t *)&(ps_llc_payload->llcpayload[(length - 3)]));

            /* Send the i frame */
            result = phLlcNfc_Interface_Write (psLlcCtxt, 
                            (uint8_t *)&(s_create_packet.s_llcbuf),
                            (uint32_t)s_create_packet.llcbuf_len);

            ps_frame_info->write_status = result;
            PH_LLCNFC_DEBUG("SEND TIMEOUT CALL Write status : 0x%02X\n", result);

            if (NFCSTATUS_BUSY == PHNFCSTATUS (result))
            {
                ps_frame_info->write_wait_call = resend_i_frame;
            }
            else
            {
                /* result = NFCSTATUS_PENDING and 
                    Timer is not started because the remaining timer 
                    will be running */
                uint16_t                time_out_value = 0;

                /* Each frame has the send count, so increment this 
                    as soon as the frame is sent */
                ps_timer_info->iframe_send_count[timer_index] = (uint8_t)
                            (ps_timer_info->iframe_send_count[timer_index] + 1);

                PH_LLCNFC_DEBUG("SEND TIMEOUT CALL timer index : 0x%02X\n", timer_index);

                if (timer_index > 0)
                {                    
                    PH_LLCNFC_DEBUG("SEND TIMEOUT CALL GUARD TO VALUE : 0x%02X\n", ps_timer_info->guard_to_value[(timer_index - 1)]);
                    /* Copy the maximum time-out value. */
                    time_out_value = (uint16_t)
                        ((ps_timer_info->guard_to_value[(timer_index - 1)] >= 
                        PH_LLCNFC_GUARD_TO_VALUE) ? 
                        (ps_timer_info->guard_to_value[(timer_index - 1)] + 
                        PH_LLCNFC_RESOLUTION): 
                        PH_LLCNFC_GUARD_TO_VALUE);
                }
                else
                {
                    /* If the timer_index is 0 means, the previous timed out  
                        frame is the last frame in the list */
                    time_out_value = (uint16_t)
                        ((ps_timer_info->guard_to_value[(timer_count - 1)] >= 
                        PH_LLCNFC_GUARD_TO_VALUE) ? 
                        (ps_timer_info->guard_to_value[(timer_count - 1)] + 
                        PH_LLCNFC_RESOLUTION):
                        PH_LLCNFC_GUARD_TO_VALUE);
                }

                ps_timer_info->guard_to_value[timer_index] = time_out_value;

                ps_frame_info->sent_frame_type = resend_i_frame;

                ps_timer_info->frame_type[timer_index] = invalid_frame;

                PH_LLCNFC_DEBUG("SEND TIMEOUT CALL Next frame type : 0x%02X\n", ps_timer_info->frame_type[((timer_index + 1) % PH_LLCNFC_MAX_ACK_GUARD_TIMER)]);
                /* Now check if next timer has expired, if yes, 
                    set the index to next, on receiving the write response 
                    callback for this send, then next frame can be sent */
                if (resend_i_frame == 
                    ps_timer_info->frame_type[((timer_index + 1) % 
                    PH_LLCNFC_MAX_ACK_GUARD_TIMER)])
                {
                    /* If next frame has to be sent, then update write wait */
                    ps_frame_info->write_status = NFCSTATUS_BUSY;
                    ps_frame_info->write_wait_call = resend_i_frame;
                    ps_timer_info->index_to_send = (uint8_t)
                                            ((timer_index + 1) % 
                                            PH_LLCNFC_MAX_ACK_GUARD_TIMER);
                }
                else
                {
                    /* Timer is not expired, 
                        Now, Check if the new frame is ready to be sent, if yes, 
                        then update the variable */
                    if (
                        (ps_frame_info->n_s != ((ps_store_frame->winsize_cnt + 
                        ps_store_frame->start_pos) % PH_LLCNFC_MOD_NS_NR))
                        )
                    {
                        ps_frame_info->write_status = PHNFCSTVAL (CID_NFC_LLC, 
                                                    NFCSTATUS_BUSY);
                        ps_frame_info->write_wait_call = user_i_frame;
                    }
                }

#if 0
                result = phLlcNfc_StartTimers (PH_LLCNFC_GUARDTIMER, 
                                        ((llc_header >> 
                                        PH_LLCNFC_NS_START_BIT_POS) | 
                                        MAX_NS_NR_VALUE));
#endif /* #if 0 */

            }            
        }
        else
        {
            if (
                (ps_frame_info->n_s != ((ps_store_frame->winsize_cnt + 
                ps_store_frame->start_pos) % PH_LLCNFC_MOD_NS_NR))
                )
            {
                ps_frame_info->write_status = PHNFCSTVAL (CID_NFC_LLC, 
                                            NFCSTATUS_BUSY);
                ps_frame_info->write_wait_call = user_i_frame;
            }
        }
    }
    
    return result;
}


void 
phLlcNfc_H_ProcessIFrame (  
    phLlcNfc_Context_t      *psLlcCtxt 
)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    uint8_t                     ns_index = 0;    
#if defined (LLC_SEND_RR_ACK)
    /* uint8_t                     nr_index = 0; */
#endif /* #if defined (LLC_SEND_RR_ACK) */
    phLlcNfc_Frame_t            *ps_frame_info = NULL;
    phLlcNfc_StoreIFrame_t      *ps_store_frame = NULL;
    phLlcNfc_LlcPacket_t        *ps_recv_pkt = NULL;
    phLlcNfc_LlcCmd_t           cmdtype = phLlcNfc_e_error;
    phLlcNfc_eSentFrameType_t   eframe_type = invalid_frame;
    uint8_t                     dont_send_s_frame = FALSE;
    uint8_t                     no_of_del_frames = 0;
    phNfc_sCompletionInfo_t     notifyinfo = {0,0,0};

#ifdef RECV_NR_CHECK_ENABLE
    uint8_t                     recvd_nr = 0;    
#endif /* #ifdef RECV_NR_CHECK_ENABLE */

    ps_frame_info = &(psLlcCtxt->s_frameinfo);
    ps_store_frame = &(ps_frame_info->s_send_store);
    ps_recv_pkt = &(ps_frame_info->s_recvpacket);

    PHNFC_UNUSED_VARIABLE(result);
    /* Received buffer, N(S) value */
    ns_index = (uint8_t)GET_BITS8( 
                        ps_recv_pkt->s_llcbuf.sllcpayload.llcheader, 
                        PH_LLCNFC_NS_START_BIT_POS, 
                        PH_LLCNFC_NR_NS_NO_OF_BITS);

    PH_LLCNFC_DEBUG("NS START POS BEFORE DEL : 0x%02X\n", ps_store_frame->start_pos);
    PH_LLCNFC_DEBUG("WIN SIZE BEFORE DEL : 0x%02X\n", ps_store_frame->winsize_cnt);

    /* Correct frame is received, so remove the stored i frame info */
    no_of_del_frames = phLlcNfc_H_UpdateIFrameList (ps_frame_info, 
                                &(ps_frame_info->s_send_store));

    PH_LLCNFC_DEBUG("NS START POS AFTER DEL : 0x%02X\n", ps_store_frame->start_pos);
    PH_LLCNFC_DEBUG("WIN SIZE AFTER DEL : 0x%02X\n", ps_store_frame->winsize_cnt);

#ifdef RECV_NR_CHECK_ENABLE

    recvd_nr = (uint8_t)GET_BITS8( 
                        ps_recv_pkt->s_llcbuf.sllcpayload.llcheader, 
                        PH_LLCNFC_NR_START_BIT_POS, 
                        PH_LLCNFC_NR_NS_NO_OF_BITS);

    if (((ps_frame_info->n_s > recvd_nr) && 
        (0 == ((ps_frame_info->n_s + 1) % PH_LLCNFC_MOD_NS_NR)))
        || (recvd_nr > ps_frame_info->n_s))

#else /* #ifdef RECV_NR_CHECK_ENABLE */

    if (no_of_del_frames > 0)

#endif /* #ifdef RECV_NR_CHECK_ENABLE */
    {
        phLlcNfc_StopTimers (PH_LLCNFC_GUARDTIMER, no_of_del_frames);
    }

    /* Received buffer, N(S) value = N(R) of host (our 
        structure) then send RR type of s frame else send 
        REJ type of s frame */
    if ((ns_index == ps_frame_info->n_r)
#if defined (LLC_SEND_RR_ACK)

        || ((ns_index < ps_frame_info->n_r) && 
            ((ps_frame_info->n_r - ns_index) < ps_frame_info->window_size))
        || ((ns_index > ps_frame_info->n_r) && 
            ((ns_index - ps_frame_info->n_r) > ps_frame_info->window_size))

#endif /* #if defined (LLC_SEND_RR_ACK) */
         )
    {
        PH_LLCNFC_PRINT(" Type bits of S frame to be sent is RR \n");
        ps_frame_info->recv_error_count = 0;
        ps_frame_info->send_error_count = 0;

        psLlcCtxt->recvbuf_length = (ps_recv_pkt->llcbuf_len - 
                                    PH_LLCNFC_LEN_APPEND);

        (void)memcpy ((void *)psLlcCtxt->precv_buf, (void *)(
                        ps_recv_pkt->s_llcbuf.sllcpayload.llcpayload), 
                        psLlcCtxt->recvbuf_length);
        
#if defined (LLC_SEND_RR_ACK)

        if (((ns_index < ps_frame_info->n_r) && 
            ((ps_frame_info->n_r - ns_index) < ps_frame_info->window_size))
            || ((ns_index > ps_frame_info->n_r) && 
            ((ns_index - ps_frame_info->n_r) > ps_frame_info->window_size)))
        {
            ps_frame_info->n_r = ((ns_index + 1) 
                                % PH_LLCNFC_MOD_NS_NR);
        }
        else

#endif /* #if defined (LLC_SEND_RR_ACK) */
        {            
            /* Update the N(R) value in I and S frame context  */
            ps_frame_info->n_r = ((ps_frame_info->n_r + 1)
                                    % PH_LLCNFC_MOD_NS_NR);

#ifdef PIGGY_BACK
            ps_frame_info->resp_recvd_count = (uint8_t)
                                    (ps_frame_info->resp_recvd_count + 1);
#endif /* #ifdef PIGGY_BACK */

        }

        if (NFCSTATUS_BUSY == PHNFCSTATUS (ps_frame_info->write_status))
        {
            /* Any how write cannot be done and some frame is ready to be sent 
            so this frame will act as the ACK */            
            result = phLlcNfc_H_WriteWaitCall (psLlcCtxt);
        }
        else 
        {
            if (
                (ps_frame_info->n_s != ((ps_store_frame->winsize_cnt + 
                ps_store_frame->start_pos) % PH_LLCNFC_MOD_NS_NR))
                )
            {
                /* If user has sent a frame and DAL write is busy, then 
                it has to be sent */
                result = phLlcNfc_H_SendUserIFrame (psLlcCtxt, ps_store_frame);
            }
        }

        if (NFCSTATUS_PENDING == result)
        {
            dont_send_s_frame = TRUE;
#ifdef LLC_UPP_LAYER_NTFY_WRITE_RSP_CB
            phLlcNfc_H_SendInfo (psLlcCtxt);
#endif /* #ifdef LLC_UPP_LAYER_NTFY_WRITE_RSP_CB */        
        }
        else
        {
            cmdtype = phLlcNfc_e_rr;
            /* If i frame is sent from the stored list, it got the correct 
                acknowledge i frame, so now for an i frame , s frame acknowledge 
                is sent */
            eframe_type = ((resend_i_frame == ps_frame_info->sent_frame_type)?
                            resend_s_frame : s_frame);
        }

#ifdef PIGGY_BACK
        phLlcNfc_H_SendInfo (psLlcCtxt);
#endif /* #ifdef PIGGY_BACK */

    }
    else
    {
        ps_frame_info->send_error_count = (uint8_t)
                                (ps_frame_info->send_error_count + 1);

#ifdef LLC_SEND_ERROR_COUNT

        if (ps_frame_info->send_error_count < RECV_ERROR_FRAME_COUNT)

#endif /* #ifdef LLC_SEND_ERROR_COUNT */
        {

#ifdef LLC_RR_INSTEAD_OF_REJ

            if (((ps_frame_info->n_r > 0) && (ns_index == (ps_frame_info->n_r - 1)))
                || ((0 == ps_frame_info->n_r) &&
                (ns_index == (PH_LLCNFC_MOD_NS_NR - 1))))
            {
                cmdtype = phLlcNfc_e_rr;
                eframe_type = rej_rr_s_frame;
            }
            else

#endif /* #ifdef LLC_RR_INSTEAD_OF_REJ */
            {
                cmdtype = phLlcNfc_e_rej;
                eframe_type = ((resend_i_frame == ps_frame_info->sent_frame_type)?
                                resend_rej_s_frame : reject_s_frame);
            }
        }

#ifdef LLC_SEND_ERROR_COUNT
        else
        {
#ifdef LLC_RSET_INSTEAD_OF_EXCEPTION

            result = phLlcNfc_H_SendRSETFrame (psLlcCtxt);

#else /* #ifdef LLC_RSET_INSTEAD_OF_EXCEPTION */

            dont_send_s_frame = TRUE;
            PH_LLCNFC_DEBUG("SEND ERROR COUNT : 0x%02X\n", ps_frame_info->send_error_count);
            /* Error count has reached the limit, raise exception */
            notifyinfo.status = PHNFCSTVAL(CID_NFC_LLC, 
                                            NFCSTATUS_BOARD_COMMUNICATION_ERROR);
#if 0
            phOsalNfc_RaiseException(phOsalNfc_e_UnrecovFirmwareErr,1); 
#endif /* #if 0 */
                /* Resend done, no answer from the device */
            psLlcCtxt->cb_for_if.notify (
                            psLlcCtxt->cb_for_if.pif_ctxt,
                            psLlcCtxt->phwinfo, 
                            NFC_NOTIFY_DEVICE_ERROR, 
                            &notifyinfo);

#endif /* #ifdef LLC_RSET_INSTEAD_OF_EXCEPTION */
        }
#endif /* #ifdef LLC_SEND_ERROR_COUNT */
    }

#ifdef LLC_RELEASE_FLAG

    if (FALSE == g_release_flag)

#endif /* #ifdef LLC_RELEASE_FLAG */
    {
        (void)phLlcNfc_Interface_Read(psLlcCtxt,
                        PH_LLCNFC_READWAIT_OFF, 
                        &(ps_recv_pkt->s_llcbuf.llc_length_byte),
                        (uint8_t)PH_LLCNFC_BYTES_INIT_READ);
    
#ifdef PIGGY_BACK
        /* Check if any write call is performed or not */
        if (NFCSTATUS_PENDING != result)
        {
            /* No write is performed, So, now check */
            if (NFCSTATUS_BUSY == PHNFCSTATUS (ps_frame_info->write_status))
            {
                /* Any how write cannot be done and some frame is ready to be sent
                so this frame will act as the ACK */
                result = phLlcNfc_H_WriteWaitCall (psLlcCtxt);
            }
        }

        if (NFCSTATUS_PENDING != result)
        {
            if (ps_frame_info->window_size == ps_frame_info->resp_recvd_count)
            {
                phLlcNfc_LlcPacket_t    s_packet_info;
                /* Create S frame */
                (void)phLlcNfc_H_CreateSFramePayload (ps_frame_info, &(s_packet_info), cmdtype);

                result = phLlcNfc_Interface_Write(psLlcCtxt,
                            (uint8_t *)&(s_packet_info.s_llcbuf),
                            (uint32_t)(s_packet_info.llcbuf_len));


                if (0 == ps_frame_info->send_error_count)
                {
                    ps_frame_info->write_wait_call = invalid_frame;
                }
                ps_frame_info->sent_frame_type = eframe_type;
            }
            else
            {
                result = phLlcNfc_StartTimers (PH_LLCNFC_ACKTIMER, 0);
            }
        }
#else /* #ifdef PIGGY_BACK */

        if ((TRUE != ps_frame_info->write_pending) &&  
            (PH_LLCNFC_READPEND_REMAIN_BYTE != ps_frame_info->read_pending) && 
            (FALSE == dont_send_s_frame))
        {
            phLlcNfc_LlcPacket_t    s_packet_info = {0};
            /* Create S frame */
            (void)phLlcNfc_H_CreateSFramePayload (ps_frame_info, &(s_packet_info), cmdtype);

            result = phLlcNfc_Interface_Write(psLlcCtxt,
                        (uint8_t *)&(s_packet_info.s_llcbuf),
                        (uint32_t)(s_packet_info.llcbuf_len));


            if (0 == ps_frame_info->send_error_count)
            {
                ps_frame_info->write_wait_call = invalid_frame;
            }
            ps_frame_info->sent_frame_type = eframe_type;
        }
#endif /* #ifdef PIGGY_BACK */
    }

    return ;
}

static 
NFCSTATUS 
phLlcNfc_H_ProcessUFrame (  
    phLlcNfc_Context_t      *psLlcCtxt
)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    phLlcNfc_Frame_t            *ps_frame_info = NULL;
    phLlcNfc_LlcPacket_t        *ps_uframe_pkt = NULL;
#ifdef LLC_URSET_NO_DELAY
    phNfc_sCompletionInfo_t     notifyinfo = {0,0,0};
#else /* #ifdef LLC_URSET_NO_DELAY */
    uint32_t                    delay_timer_id = 
                                PH_OSALNFC_INVALID_TIMER_ID;
#endif /* #ifdef LLC_URSET_NO_DELAY */
    uint8_t                     cmdtype = phLlcNfc_e_error;

    phLlcNfc_StopTimers(PH_LLCNFC_CONNECTIONTIMER, 0);
    ps_frame_info = &(psLlcCtxt->s_frameinfo);
    ps_uframe_pkt = &(ps_frame_info->s_recvpacket);
    /* Check the command type */
    cmdtype = (ps_uframe_pkt->s_llcbuf.sllcpayload.llcheader & 
                PH_LLCNFC_U_FRAME_MODIFIER_MASK);
    PHNFC_UNUSED_VARIABLE(result);
    switch(cmdtype)
    {
        case phLlcNfc_e_rset:
        {
            psLlcCtxt->s_frameinfo.rset_recvd = TRUE;
            /* command type is RSET, so update the U frame parameters */
            result = phLlcNfc_H_Update_ReceivedRSETInfo (ps_frame_info, 
                                ps_uframe_pkt->s_llcbuf.sllcpayload);
            /* Create a UA frame */
            result = phLlcNfc_H_CreateUFramePayload(psLlcCtxt, 
                                            ps_uframe_pkt, 
                                            &(ps_uframe_pkt->llcbuf_len), 
                                            phLlcNfc_e_ua);

            if (NFCSTATUS_SUCCESS == result)
            {
                /* Call DAL write */
                result = phLlcNfc_Interface_Write( psLlcCtxt, 
                                    (uint8_t*)&(ps_uframe_pkt->s_llcbuf), 
                                    (uint32_t)ps_uframe_pkt->llcbuf_len);

                phLlcNfc_H_ResetFrameInfo(psLlcCtxt);
                ps_frame_info->write_status = result;
                ps_frame_info->write_wait_call = invalid_frame;
                if (NFCSTATUS_PENDING == result)
                {
                    ps_frame_info->sent_frame_type = 
                            ((ps_frame_info->sent_frame_type != init_u_rset_frame) ?
                            u_a_frame : init_u_a_frame);
                }
                else
                {
                    if (NFCSTATUS_BUSY == PHNFCSTATUS(result))
                    {
                        ps_frame_info->write_wait_call = 
                            ((ps_frame_info->sent_frame_type != init_u_rset_frame) ?
                            u_a_frame : init_u_a_frame);
                        result = NFCSTATUS_PENDING;
                    }
                }
            }
            break;
        }
        case phLlcNfc_e_ua:
        {
            phLlcNfc_H_ResetFrameInfo (psLlcCtxt);
            /* Add timer here, to delay the next command to the PN544 */
#ifdef LLC_URSET_NO_DELAY
            if (ps_frame_info->s_send_store.winsize_cnt > 0)
            {
#if 0
                /* Resend I frame */
                result = phLlcNfc_H_SendTimedOutIFrame (psLlcCtxt, 
                                &(ps_frame_info->s_send_store, 0);
#else
                result = phLlcNfc_H_SendUserIFrame (psLlcCtxt, 
                                &(ps_frame_info->s_send_store));
#endif /* #if 0 */
            }
            else 
            {
                if ((init_u_rset_frame == ps_frame_info->sent_frame_type) && 
                    (NULL != psLlcCtxt->cb_for_if.notify))
                {
                    ps_frame_info->sent_frame_type = write_resp_received;
                    notifyinfo.status = NFCSTATUS_SUCCESS;
                    /* Send the notification to the upper layer */
                    psLlcCtxt->cb_for_if.notify(
                                psLlcCtxt->cb_for_if.pif_ctxt, 
                                psLlcCtxt->phwinfo, 
                                NFC_NOTIFY_INIT_COMPLETED, 
                                &notifyinfo);
                }
            }
#else /* #ifdef LLC_URSET_NO_DELAY */
            delay_timer_id = phOsalNfc_Timer_Create ();
            phOsalNfc_Timer_Start (delay_timer_id, LLC_URSET_DELAY_TIME_OUT, 
                                    phLlcNfc_URSET_Delay_Notify, (void*)0);
#endif /* #ifdef LLC_URSET_NO_DELAY */
            break;
        }
        default:
        {
            result = PHNFCSTVAL(CID_NFC_LLC, 
                                NFCSTATUS_INVALID_FORMAT);
            break;
        }
    }
    return result;
}

static 
void 
phLlcNfc_H_ProcessSFrame (  
    phLlcNfc_Context_t      *psLlcCtxt)
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    uint8_t                     cmdtype = phLlcNfc_e_error;
#if 0
                                prev_win_count = 0;
#endif /* #if 0 */
    phNfc_sTransactionInfo_t    compinfo = {0, 0, 0, 0, 0};
    phLlcNfc_Frame_t            *ps_frame_info = NULL;
    phLlcNfc_StoreIFrame_t      *ps_store_frame = NULL;
    phLlcNfc_LlcPacket_t        *ps_recv_pkt = NULL;
    uint8_t                     no_of_del_frames = 0;
    phNfc_sCompletionInfo_t     notifyinfo = {0,0,0};
    
    ps_frame_info = &(psLlcCtxt->s_frameinfo);
    ps_recv_pkt = &(ps_frame_info->s_recvpacket);
    ps_store_frame = &(ps_frame_info->s_send_store);

    cmdtype = (ps_recv_pkt->s_llcbuf.sllcpayload.llcheader & 
                PH_LLCNFC_S_FRAME_TYPE_MASK);
    PHNFC_UNUSED_VARIABLE(result);

    PH_LLCNFC_DEBUG("NS START POS BEFORE DEL : 0x%02X\n", ps_store_frame->start_pos);
    PH_LLCNFC_DEBUG("WIN SIZE BEFORE DEL : 0x%02X\n", ps_store_frame->winsize_cnt);

    /* Correct frame is received, so remove the 
        stored i frame info for the acknowledged frames */
    no_of_del_frames = phLlcNfc_H_UpdateIFrameList (ps_frame_info, 
                                        &(ps_frame_info->s_send_store));

    PH_LLCNFC_DEBUG("NS START POS AFTER DEL : 0x%02X\n", ps_store_frame->start_pos);
    PH_LLCNFC_DEBUG("WIN SIZE AFTER DEL : 0x%02X\n", ps_store_frame->winsize_cnt);

#if 0
    prev_win_count = ps_frame_info->s_send_store.winsize_cnt;
#endif /* #if 0 */

    /* Pend the read */
    result = phLlcNfc_Interface_Read (psLlcCtxt, 
                            PH_LLCNFC_READWAIT_OFF, 
                            &(ps_recv_pkt->s_llcbuf.llc_length_byte),
                            (uint8_t)PH_LLCNFC_BYTES_INIT_READ);    
    switch(cmdtype)
    {
        case phLlcNfc_e_rr:
        case phLlcNfc_e_rej:
        {
            /* RR frame received */
            phLlcNfc_StopTimers (PH_LLCNFC_GUARDTIMER, no_of_del_frames);

            if (phLlcNfc_e_rr == cmdtype)
            {
                ps_frame_info->recv_error_count = 0;
                ps_frame_info->send_error_count = 0;
            }
            else
            {
                ps_frame_info->recv_error_count = (uint8_t)
                                (ps_frame_info->recv_error_count + 1);
            }

            if (ps_frame_info->recv_error_count >= RECV_ERROR_FRAME_COUNT)
            {
                /* Do nothing */
            }
            else if (NFCSTATUS_BUSY == PHNFCSTATUS(ps_frame_info->write_status))
            {
                result = phLlcNfc_H_WriteWaitCall (psLlcCtxt);
            }
            else 
            {
                if (
                    (ps_frame_info->n_s != ((ps_store_frame->winsize_cnt + 
                    ps_store_frame->start_pos) % PH_LLCNFC_MOD_NS_NR))
                    )
                {
                    /* If user has sent a frame and DAL write is busy, then 
                        it has to be sent */
                    result = phLlcNfc_H_SendUserIFrame (psLlcCtxt, ps_store_frame);
                }
            }

            if ((0 != psLlcCtxt->send_cb_len) && 
                (ps_store_frame->winsize_cnt < ps_frame_info->window_size))
            {
                /* Due to the window size count (i.e., count has reached 
                    the limit), send completion was not sent for the previous 
                    send from the upper layer. So to allow next send from the 
                    upper layer, send completion is called */
                compinfo.length = (uint16_t)psLlcCtxt->send_cb_len;
                compinfo.status = NFCSTATUS_SUCCESS;
                
                if (NULL != psLlcCtxt->cb_for_if.send_complete)
                {
                    psLlcCtxt->send_cb_len = 0;
                    /* Call the send callback, if the window size 
                        count becomes less than actual window size */
                    psLlcCtxt->cb_for_if.send_complete (
                                psLlcCtxt->cb_for_if.pif_ctxt, 
                                psLlcCtxt->phwinfo, &compinfo);
                }
            }
            break;
        }

#if 0
        case phLlcNfc_e_rej:
        {
            ps_frame_info->recv_error_count = (uint8_t)
                                (ps_frame_info->recv_error_count + 1);
            /* RR frame received */
            phLlcNfc_StopTimers (PH_LLCNFC_GUARDTIMER, no_of_del_frames);

            if (ps_frame_info->recv_error_count < RECV_ERROR_FRAME_COUNT)
            {
                /* Below check is added because if PN544 sends REJ to a frame, but 
                    the next frame has already been sent from PN544, then 
                Send the user I frame */
                result = phLlcNfc_H_SendUserIFrame (psLlcCtxt, ps_store_frame);
            }
            break;
        }
#endif /* #if 0 */

        case phLlcNfc_e_rnr:
        {
            phLlcNfc_StopAllTimers ();
            ps_frame_info->recv_error_count = (uint8_t)
                                (ps_frame_info->recv_error_count + 1);
            break;
        }

        case phLlcNfc_e_srej:
        default:
        {
            ps_frame_info->recv_error_count = (uint8_t)
                                (ps_frame_info->recv_error_count + 1);
            result = PHNFCSTVAL (CID_NFC_LLC, 
                                NFCSTATUS_INVALID_FORMAT);
            break;
        }
    }

    if (ps_frame_info->recv_error_count >= RECV_ERROR_FRAME_COUNT)
    {
#ifdef LLC_RSET_INSTEAD_OF_EXCEPTION

        result = phLlcNfc_H_SendRSETFrame (psLlcCtxt);

#else /* #ifdef LLC_RSET_INSTEAD_OF_EXCEPTION */

        PH_LLCNFC_DEBUG("RECV ERROR COUNT : 0x%02X\n", ps_frame_info->recv_error_count);
        /* Raise the exception for CRC error received from the  */
        notifyinfo.status = PHNFCSTVAL(CID_NFC_LLC, 
                                NFCSTATUS_BOARD_COMMUNICATION_ERROR);
#if 0
        phOsalNfc_RaiseException(phOsalNfc_e_UnrecovFirmwareErr,1); 
#endif /* #if 0 */
        /* Resend done, no answer from the device */
        psLlcCtxt->cb_for_if.notify (
                        psLlcCtxt->cb_for_if.pif_ctxt,
                        psLlcCtxt->phwinfo, 
                        NFC_NOTIFY_DEVICE_ERROR, 
                        &notifyinfo);

#endif /* #ifdef LLC_RSET_INSTEAD_OF_EXCEPTION */
    }

    return ;
}


void 
phLlcNfc_H_ComputeCrc(
    uint8_t     *pData, 
    uint8_t     length,
    uint8_t     *pCrc1, 
    uint8_t     *pCrc2
)
{
    uint8_t     crc_byte = 0, 
                index = 0;
    uint16_t    crc = 0;

#ifdef CRC_A
        crc = 0x6363; /* ITU-V.41 */
#else
        crc = 0xFFFF; /* ISO/IEC 13239 (formerly ISO/IEC 3309) */
#endif /* #ifdef CRC_A */

    do 
    {
        crc_byte = pData[index];
        phLlcNfc_H_UpdateCrc(crc_byte, &crc);
        index++;
    } while (index < length);

#ifndef INVERT_CRC
    crc = ~crc; /* ISO/IEC 13239 (formerly ISO/IEC 3309) */
#endif /* #ifndef INVERT_CRC */

    *pCrc1 = (uint8_t) (crc & 0xFF);
    *pCrc2 = (uint8_t) ((crc >> 8) & 0xFF);
    return;
}

static 
void 
phLlcNfc_H_UpdateCrc(
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

NFCSTATUS 
phLlcNfc_H_StoreIFrame (
    phLlcNfc_StoreIFrame_t      *psList,
    phLlcNfc_LlcPacket_t        sPacketInfo
)
{
    NFCSTATUS   result = NFCSTATUS_SUCCESS;
    uint8_t     ns_index = 0, 
                llc_header = 0;

    if ((NULL == psList) || (0 == sPacketInfo.llcbuf_len) || 
        (PH_LLCNFC_I_HEADER_INIT != 
        (sPacketInfo.s_llcbuf.sllcpayload.llcheader & 
        PH_LLCNFC_I_FRM_HEADER_MASK)))
    {
        result = PHNFCSTVAL(CID_NFC_LLC, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        /* Get the index from the start index */
        if(psList->winsize_cnt > 0)
        {
            ns_index = (uint8_t)((psList->start_pos + psList->winsize_cnt) % 
                                PH_LLCNFC_MOD_NS_NR);
        }
        else
        {
            ns_index = psList->start_pos;
        }

        llc_header = (uint8_t)(PH_LLCNFC_I_HEADER_INIT | (ns_index << 
                        PH_LLCNFC_NS_START_BIT_POS));
        sPacketInfo.s_llcbuf.sllcpayload.llcheader = llc_header;

        (void)memcpy (&(psList->s_llcpacket[ns_index]), 
                        &(sPacketInfo), sizeof(phLlcNfc_LlcPacket_t));

        /* This variable says that LLC has to send complete 
            callback for stored I frame */
        psList->s_llcpacket[ns_index].frame_to_send = invalid_frame;

        psList->winsize_cnt++;        
    }
    return result;
}

static
void 
phLlcNfc_H_DeleteIFrame (
    phLlcNfc_StoreIFrame_t      *psList
)
{    
    if (NULL != psList)
    {
        (void)memset( &(psList->s_llcpacket[psList->start_pos]), 
                        0, sizeof(phLlcNfc_LlcPacket_t));

        /* Go to next N(S) position */
        psList->start_pos = ((psList->start_pos + 1) % 
                            PH_LLCNFC_MOD_NS_NR);

        if (psList->winsize_cnt > 0)
        {
            psList->winsize_cnt--;
        }
    }
}

static
NFCSTATUS  
phLlcNfc_H_IFrameList_Peek (
    phLlcNfc_StoreIFrame_t      *psList, 
    phLlcNfc_LlcPacket_t        **psPacketinfo, 
    uint8_t                     position
)
{
    NFCSTATUS   result = NFCSTATUS_SUCCESS;
    uint8_t     index = 0;

    *psPacketinfo = NULL;
    if ((NULL != psList) && (psList->winsize_cnt > 0))
    {
        result = NFCSTATUS_SUCCESS;
        if ((position < (psList->start_pos + psList->winsize_cnt)) || 
            (DEFAULT_PACKET_INPUT == position))
        {
            index = (uint8_t)((DEFAULT_PACKET_INPUT == position) ? 
                    psList->start_pos : position);
            *psPacketinfo = &(psList->s_llcpacket[index]);
        }
    }

    return result;
}

NFCSTATUS 
phLlcNfc_H_ProRecvFrame (
    phLlcNfc_Context_t      *psLlcCtxt
)
{
    NFCSTATUS               result = PHNFCSTVAL(CID_NFC_LLC, 
                                    NFCSTATUS_INVALID_PARAMETER);
    phLlcNfc_FrameType_t    frame_type = phLlcNfc_eErr_frame;
#ifdef LLC_DATA_BYTES
    uint8_t                 *print_buf = (uint8_t *)
                            &(psLlcCtxt->s_frameinfo.s_recvpacket.s_llcbuf);
    uint8_t                 buf_len = 
                            psLlcCtxt->s_frameinfo.s_recvpacket.llcbuf_len;
    PH_LLCNFC_STRING("** Response ");
    
#endif /* LLC_DATA_BYTES */
    if (NULL != psLlcCtxt)
    {
        result = NFCSTATUS_SUCCESS;
        /* Get the received frame type */
        frame_type = phLlcNfc_H_ChkGetLlcFrameType(
            psLlcCtxt->s_frameinfo.s_recvpacket.s_llcbuf.sllcpayload.llcheader);

        /* Depending on the received frame type, process the 
        received buffer */
        switch(frame_type)
        {
            case phLlcNfc_eU_frame:
            {
                PH_LLCNFC_PRINT("U frame received \n");
                PH_LLCNFC_STRING("U frame ");
                PH_LLCNFC_PRINT_DATA(print_buf, buf_len);
                PH_LLCNFC_STRING(";\n");
                result = phLlcNfc_H_ProcessUFrame(psLlcCtxt);
                break;
            }
            case phLlcNfc_eI_frame:
            {
                PH_LLCNFC_PRINT("I frame received \n");
                PH_LLCNFC_STRING("I frame ");
                PH_LLCNFC_PRINT_DATA(print_buf, buf_len);
                PH_LLCNFC_STRING(";\n");
                phLlcNfc_H_ProcessIFrame(psLlcCtxt);
                break;
            }
            case phLlcNfc_eS_frame:
            {
                PH_LLCNFC_PRINT("S frame received \n");
                PH_LLCNFC_STRING("S frame ");
                PH_LLCNFC_PRINT_DATA(print_buf, buf_len);
                PH_LLCNFC_STRING(";\n");
                phLlcNfc_H_ProcessSFrame(psLlcCtxt);
                break;
            }
            case phLlcNfc_eErr_frame:
            default:
            {
                PH_LLCNFC_PRINT("Error frame received \n");
                result = PHNFCSTVAL(CID_NFC_LLC, NFCSTATUS_INVALID_FORMAT);
                break;
            }
        }
        
    }
    return result;
}

#ifdef CRC_ERROR_REJ
NFCSTATUS 
phLlcNfc_H_SendRejectFrame(
                      phLlcNfc_Context_t  *psLlcCtxt
                      )
{
    NFCSTATUS       result = NFCSTATUS_SUCCESS;
    phLlcNfc_LlcPacket_t    s_packet_info = {0};

    result = phLlcNfc_H_CreateSFramePayload(
                                    &(psLlcCtxt->s_frameinfo),
                                    &(s_packet_info),
                                    phLlcNfc_e_rej);
    /* Send the "S" frame to the lower layer */
    result = phLlcNfc_Interface_Write(psLlcCtxt,
        (uint8_t *)&(s_packet_info.s_llcbuf),
        (uint32_t)(s_packet_info.llcbuf_len));

    if (NFCSTATUS_PENDING == result)
    {
        /* Increment the retry count of the reject frame */
        psLlcCtxt->s_frameinfo.recv_error_count = 
                        (psLlcCtxt->s_frameinfo.recv_error_count + 1);
    }


    return result;                  
}
#endif /* #ifdef CRC_ERROR_REJ */

static 
void 
phLlcNfc_H_ResetFrameInfo (
    phLlcNfc_Context_t  *psLlcCtxt
)
{
    uint8_t                     i = 0, 
                                win_cnt = 0, 
                                pos = 0, 
                                while_exit = FALSE, 
                                index_flag = FALSE;
    phLlcNfc_StoreIFrame_t      *ps_send_store = NULL;
    phLlcNfc_Buffer_t           *ps_buffer = NULL;

    ps_send_store = &(psLlcCtxt->s_frameinfo.s_send_store);
    win_cnt = ps_send_store->winsize_cnt;
    pos = ps_send_store->start_pos;
    PH_LLCNFC_PRINT ("\n\nLLC : phLlcNfc_H_ResetFrameInfo called\n\n");
    PH_LLCNFC_DEBUG ("\n\nLLC : ps_send_store->start_pos %08X\n", ps_send_store->start_pos);
    PH_LLCNFC_DEBUG ("\n\nLLC : ps_send_store->winsize_cnt before reset %08X\n", ps_send_store->winsize_cnt);


    if (0 != pos)
    {
        /* If the start position of the ns = 0, then 
            no need to shift the stored llc data, 
            Else it has to be shifted to the first 
            index of the array */
        if(TRUE == ((pos + win_cnt) / 
                    PH_LLCNFC_MAX_I_FRAME_STORE))
        {
            /* 'i' is the array index, So to store data in the array, 
                windows size count shall be subtracted by 1 */
            i = (win_cnt - 1);
            /* if window size > 1 and ns for 2 frames are 7 and 0, then 
                to reset the ns index to 0, the frames are copied from 
                the reverse order, so to do it a flag is declared */
            index_flag = TRUE;
            pos = (((pos - 1) + win_cnt) % PH_LLCNFC_MAX_I_FRAME_STORE);
        }

        while (FALSE == while_exit)
        {
            (void)memcpy ((void *)&(ps_send_store->s_llcpacket[i]),  
                        (void *)&(ps_send_store->s_llcpacket[pos]), 
                        sizeof (phLlcNfc_LlcPacket_t));

            ps_send_store->s_llcpacket[i].frame_to_send = invalid_frame;
            
            ps_buffer = &(ps_send_store->s_llcpacket[i].s_llcbuf);
            /* change n(s) value */
            ps_buffer->sllcpayload.llcheader = (uint8_t)
                                    (PH_LLCNFC_I_HEADER_INIT | 
                                    (i << PH_LLCNFC_NS_START_BIT_POS));
            if(TRUE == index_flag)
            {
                if(0 == i)
                {
                    while_exit = TRUE;
                }
                else
                {
                    i = ((i - 1) % PH_LLCNFC_MAX_I_FRAME_STORE);
                    if (0 == pos)
                    {
                        pos = (PH_LLCNFC_MAX_I_FRAME_STORE - 1);
                    }
                    else
                    {
                        pos = ((pos - 1) % PH_LLCNFC_MAX_I_FRAME_STORE);
                    }
                }                
            }
            else
            {
                if (i >= win_cnt)
                {
                    while_exit = TRUE;
                }
                else
                {
                    i = ((i + 1) % PH_LLCNFC_MAX_I_FRAME_STORE);
                    pos = ((pos + 1) % PH_LLCNFC_MAX_I_FRAME_STORE);
                }
                
            }
        }
    }
    psLlcCtxt->s_timerinfo.guard_to_count = 0;
    psLlcCtxt->s_timerinfo.timer_flag = 0;
    ps_send_store->start_pos = 0;
    psLlcCtxt->s_frameinfo.n_r = psLlcCtxt->s_frameinfo.n_s = 0;
    if (ps_send_store->winsize_cnt > 0)
    {
        psLlcCtxt->s_frameinfo.rejected_ns = 0;
    }
    else
    {
        psLlcCtxt->s_frameinfo.rejected_ns = DEFAULT_PACKET_INPUT;
    }

    PH_LLCNFC_DEBUG ("\n\nLLC : ps_send_store->winsize_cnt after reset %08X\n", ps_send_store->winsize_cnt);
    return;
}

NFCSTATUS 
phLlcNfc_H_WriteWaitCall (
    phLlcNfc_Context_t  *psLlcCtxt
    )
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    phLlcNfc_StoreIFrame_t      *ps_store_info = NULL;
    phLlcNfc_Frame_t            *ps_frame_info = NULL;
    
    ps_frame_info = &(psLlcCtxt->s_frameinfo);
    ps_store_info = &(ps_frame_info->s_send_store);

    PH_LLCNFC_PRINT ("\nLLC : phLlcNfc_H_WriteWaitCall call ..\n");
    PH_LLCNFC_DEBUG ("\n\nLLC : ps_frame_info->write_wait_call before call %08X\n", ps_frame_info->write_wait_call);

    ps_frame_info->write_status = NFCSTATUS_PENDING;
    switch (ps_frame_info->write_wait_call)
    {
        case user_i_frame:
        {
            ps_frame_info->write_wait_call = invalid_frame;
            result = phLlcNfc_H_SendUserIFrame (psLlcCtxt, ps_store_info);
            break;
        }

        case resend_i_frame:
        {
            ps_frame_info->write_wait_call = invalid_frame;
            result = phLlcNfc_H_SendTimedOutIFrame (psLlcCtxt, ps_store_info, 0);
            break;
        }

        case rejected_i_frame:
        {
            ps_frame_info->write_wait_call = invalid_frame;
            result = phLlcNfc_H_SendRejectedIFrame (psLlcCtxt, ps_store_info, 
                                                    ps_frame_info->rejected_ns);
            break;
        }

        case resend_s_frame:
        case reject_s_frame:
        case resend_rej_s_frame:
        {
            ps_frame_info->write_wait_call = invalid_frame;
            break;
        }

        case u_rset_frame:
        {
            ps_frame_info->write_wait_call = invalid_frame;
            result = phLlcNfc_H_SendRSETFrame (psLlcCtxt);
            break;
        }

        default :
        {
            ps_frame_info->write_wait_call = invalid_frame;
            break;
        }
    }

    PH_LLCNFC_DEBUG ("\n\nLLC : ps_frame_info->write_wait_call after call %08X\n", ps_frame_info->write_wait_call);
    PH_LLCNFC_PRINT ("\nLLC : phLlcNfc_H_WriteWaitCall end ..\n");
    return result;
}

NFCSTATUS  
phLlcNfc_H_SendRSETFrame (
                      phLlcNfc_Context_t  *psLlcCtxt
                      )
{
    NFCSTATUS                   result = NFCSTATUS_SUCCESS;
    phLlcNfc_LlcPacket_t        s_packet_info;
    phLlcNfc_Frame_t            *ps_frame_info = NULL;
    
    ps_frame_info = &(psLlcCtxt->s_frameinfo);

    result = phLlcNfc_H_CreateUFramePayload(psLlcCtxt,
                                    &(s_packet_info),
                                    &(s_packet_info.llcbuf_len),
                                    phLlcNfc_e_rset);

    if (NFCSTATUS_SUCCESS == result)
    {
        /* Call DAL write */
        result = phLlcNfc_Interface_Write(psLlcCtxt,
                            (uint8_t*)&(s_packet_info.s_llcbuf),
                            (uint32_t)s_packet_info.llcbuf_len);
    }

    ps_frame_info->write_status = result;
    if (NFCSTATUS_PENDING == result)
    {
        /* Start the timer */
        result = phLlcNfc_StartTimers (PH_LLCNFC_CONNECTIONTIMER, 0);
        if (NFCSTATUS_SUCCESS == result)
        {
            ps_frame_info->sent_frame_type = u_rset_frame;
            result = NFCSTATUS_PENDING;
        }
    }
    else
    {
        ps_frame_info->write_wait_call = u_rset_frame;
    }

    return result;
}


