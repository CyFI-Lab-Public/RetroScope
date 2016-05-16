/******************************************************************************
 *
 *  Copyright (C) 2011-2012 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/
#include "OverrideLog.h"
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include "buildcfg.h"
#include "nfa_mem_co.h"
#include "nfa_nv_co.h"
#include "nfa_nv_ci.h"
#include "config.h"
#include "nfc_hal_nv_co.h"

#define LOG_TAG "BrcmNfcNfa"
#define PRINT(s) __android_log_write(ANDROID_LOG_DEBUG, "BrcmNci", s)
#define MAX_NCI_PACKET_SIZE  259
#define MAX_LOGCAT_LINE     4096
static char log_line[MAX_LOGCAT_LINE];

extern UINT32 ScrProtocolTraceFlag;         // = SCR_PROTO_TRACE_ALL; // 0x017F;
static const char* sTable = "0123456789abcdef";
extern char bcm_nfc_location[];
static const char* sNfaStorageBin = "/nfaStorage.bin";

/*******************************************************************************
**
** Function         nfa_mem_co_alloc
**
** Description      allocate a buffer from platform's memory pool
**
** Returns:
**                  pointer to buffer if successful
**                  NULL otherwise
**
*******************************************************************************/
NFC_API extern void *nfa_mem_co_alloc(UINT32 num_bytes)
{
    return malloc(num_bytes);
}


/*******************************************************************************
**
** Function         nfa_mem_co_free
**
** Description      free buffer previously allocated using nfa_mem_co_alloc
**
** Returns:
**                  Nothing
**
*******************************************************************************/
NFC_API extern void nfa_mem_co_free(void *pBuffer)
{
    free(pBuffer);
}


/*******************************************************************************
**
** Function         nfa_nv_co_read
**
** Description      This function is called by NFA to read in data from the
**                  previously opened file.
**
** Parameters       pBuffer   - buffer to read the data into.
**                  nbytes  - number of bytes to read into the buffer.
**
** Returns          void
**
**                  Note: Upon completion of the request, nfa_nv_ci_read() is
**                        called with the buffer of data, along with the number
**                        of bytes read into the buffer, and a status.  The
**                        call-in function should only be called when ALL requested
**                        bytes have been read, the end of file has been detected,
**                        or an error has occurred.
**
*******************************************************************************/
NFC_API extern void nfa_nv_co_read(UINT8 *pBuffer, UINT16 nbytes, UINT8 block)
{
    char filename[256], filename2[256];

    memset (filename, 0, sizeof(filename));
    memset (filename2, 0, sizeof(filename2));
    strcpy(filename2, bcm_nfc_location);
    strncat(filename2, sNfaStorageBin, sizeof(filename2)-strlen(filename2)-1);
    if (strlen(filename2) > 200)
    {
        ALOGE ("%s: filename too long", __FUNCTION__);
        return;
    }
    sprintf (filename, "%s%u", filename2, block);

    ALOGD ("%s: buffer len=%u; file=%s", __FUNCTION__, nbytes, filename);
    int fileStream = open (filename, O_RDONLY);
    if (fileStream >= 0)
    {
        unsigned short checksum = 0;
        size_t actualReadCrc = read (fileStream, &checksum, sizeof(checksum));
        size_t actualReadData = read (fileStream, pBuffer, nbytes);
        close (fileStream);
        if (actualReadData > 0)
        {
            ALOGD ("%s: data size=%u", __FUNCTION__, actualReadData);
            nfa_nv_ci_read (actualReadData, NFA_NV_CO_OK, block);
        }
        else
        {
            ALOGE ("%s: fail to read", __FUNCTION__);
            nfa_nv_ci_read (0, NFA_NV_CO_FAIL, block);
        }
    }
    else
    {
        ALOGD ("%s: fail to open", __FUNCTION__);
        nfa_nv_ci_read (0, NFA_NV_CO_FAIL, block);
    }
}

/*******************************************************************************
**
** Function         nfa_nv_co_write
**
** Description      This function is called by io to send file data to the
**                  phone.
**
** Parameters       pBuffer   - buffer to read the data from.
**                  nbytes  - number of bytes to write out to the file.
**
** Returns          void
**
**                  Note: Upon completion of the request, nfa_nv_ci_write() is
**                        called with the file descriptor and the status.  The
**                        call-in function should only be called when ALL requested
**                        bytes have been written, or an error has been detected,
**
*******************************************************************************/
NFC_API extern void nfa_nv_co_write(const UINT8 *pBuffer, UINT16 nbytes, UINT8 block)
{
    char filename[256], filename2[256];

    memset (filename, 0, sizeof(filename));
    memset (filename2, 0, sizeof(filename2));
    strcpy(filename2, bcm_nfc_location);
    strncat(filename2, sNfaStorageBin, sizeof(filename2)-strlen(filename2)-1);
    if (strlen(filename2) > 200)
    {
        ALOGE ("%s: filename too long", __FUNCTION__);
        return;
    }
    sprintf (filename, "%s%u", filename2, block);
    ALOGD ("%s: bytes=%u; file=%s", __FUNCTION__, nbytes, filename);

    int fileStream = 0;

    fileStream = open (filename, O_WRONLY | O_CREAT | O_TRUNC, S_IRUSR | S_IWUSR);
    if (fileStream >= 0)
    {
        unsigned short checksum = crcChecksumCompute (pBuffer, nbytes);
        size_t actualWrittenCrc = write (fileStream, &checksum, sizeof(checksum));
        size_t actualWrittenData = write (fileStream, pBuffer, nbytes);
        ALOGD ("%s: %d bytes written", __FUNCTION__, actualWrittenData);
        if ((actualWrittenData == nbytes) && (actualWrittenCrc == sizeof(checksum)))
        {
            nfa_nv_ci_write (NFA_NV_CO_OK);
        }
        else
        {
            ALOGE ("%s: fail to write", __FUNCTION__);
            nfa_nv_ci_write (NFA_NV_CO_FAIL);
        }
        close (fileStream);
    }
    else
    {
        ALOGE ("%s: fail to open, error = %d", __FUNCTION__, errno);
        nfa_nv_ci_write (NFA_NV_CO_FAIL);
    }
}

/*******************************************************************************
**
** Function         delete_stack_non_volatile_store
**
** Description      Delete all the content of the stack's storage location.
**
** Parameters       forceDelete: unconditionally delete the storage.
**
** Returns          none
**
*******************************************************************************/
void delete_stack_non_volatile_store (BOOLEAN forceDelete)
{
    static BOOLEAN firstTime = TRUE;
    char filename[256], filename2[256];

    if ((firstTime == FALSE) && (forceDelete == FALSE))
        return;
    firstTime = FALSE;

    ALOGD ("%s", __FUNCTION__);

    memset (filename, 0, sizeof(filename));
    memset (filename2, 0, sizeof(filename2));
    strcpy(filename2, bcm_nfc_location);
    strncat(filename2, sNfaStorageBin, sizeof(filename2)-strlen(filename2)-1);
    if (strlen(filename2) > 200)
    {
        ALOGE ("%s: filename too long", __FUNCTION__);
        return;
    }
    sprintf (filename, "%s%u", filename2, DH_NV_BLOCK);
    remove (filename);
    sprintf (filename, "%s%u", filename2, HC_F3_NV_BLOCK);
    remove (filename);
    sprintf (filename, "%s%u", filename2, HC_F4_NV_BLOCK);
    remove (filename);
    sprintf (filename, "%s%u", filename2, HC_F2_NV_BLOCK);
    remove (filename);
}

/*******************************************************************************
**
** Function         verify_stack_non_volatile_store
**
** Description      Verify the content of all non-volatile store.
**
** Parameters       none
**
** Returns          none
**
*******************************************************************************/
void verify_stack_non_volatile_store ()
{
    ALOGD ("%s", __FUNCTION__);
    char filename[256], filename2[256];
    BOOLEAN isValid = FALSE;

    memset (filename, 0, sizeof(filename));
    memset (filename2, 0, sizeof(filename2));
    strcpy(filename2, bcm_nfc_location);
    strncat(filename2, sNfaStorageBin, sizeof(filename2)-strlen(filename2)-1);
    if (strlen(filename2) > 200)
    {
        ALOGE ("%s: filename too long", __FUNCTION__);
        return;
    }

    sprintf (filename, "%s%u", filename2, DH_NV_BLOCK);
    if (crcChecksumVerifyIntegrity (filename))
    {
        sprintf (filename, "%s%u", filename2, HC_F3_NV_BLOCK);
        if (crcChecksumVerifyIntegrity (filename))
        {
            sprintf (filename, "%s%u", filename2, HC_F4_NV_BLOCK);
            if (crcChecksumVerifyIntegrity (filename))
            {
                sprintf (filename, "%s%u", filename2, HC_F2_NV_BLOCK);
                if (crcChecksumVerifyIntegrity (filename))
                    isValid = TRUE;
            }
        }
    }

    if (isValid == FALSE)
        delete_stack_non_volatile_store (TRUE);
}

/*******************************************************************************
**
** Function         byte2hex
**
** Description      convert a byte array to hexadecimal string
**
** Returns:
**                  Nothing
**
*******************************************************************************/
static inline void byte2hex(const char* data, char** str)
{
    **str = sTable[(*data >> 4) & 0xf];
    ++*str;
    **str = sTable[*data & 0xf];
    ++*str;
}

/*******************************************************************************
**
** Function         byte2char
**
** Description      convert a byte array to displayable text string
**
** Returns:
**                  Nothing
**
*******************************************************************************/
static inline void byte2char(const char* data, char** str)
{
    **str = *data < ' ' ? '.' : *data > '~' ? '.' : *data;
    ++(*str);
}

/*******************************************************************************
**
** Function         word2hex
**
** Description      Convert a two byte into text string as little-endian WORD
**
** Returns:
**                  Nothing
**
*******************************************************************************/
static inline void word2hex(const char* data, char** hex)
{
    byte2hex(&data[1], hex);
    byte2hex(&data[0], hex);
}

/*******************************************************************************
**
** Function         dumpbin
**
** Description      convert a byte array to a blob of text string for logging
**
** Returns:
**                  Nothing
**
*******************************************************************************/
void dumpbin(const char* data, int size, UINT32 trace_layer, UINT32 trace_type)
{
    char line_buff[256];
    char *line;
    int i, j, addr;
    const int width = 16;
    if(size <= 0)
        return;
#ifdef __RAW_HEADER
    //write offset
    line = line_buff;
    *line++ = ' ';
    *line++ = ' ';
    *line++ = ' ';
    *line++ = ' ';
    *line++ = ' ';
    *line++ = ' ';
    for(j = 0; j < width; j++)
    {
        byte2hex((const char*)&j, &line);
        *line++ = ' ';
    }
    *line = 0;
    PRINT(line_buff);
#endif
    for(i = 0; i < size / width; i++)
    {
        line = line_buff;
        //write address:
        addr = i*width;
        word2hex((const char*)&addr, &line);
        *line++ = ':'; *line++ = ' ';
        //write hex of data
        for(j = 0; j < width; j++)
        {
            byte2hex(&data[j], &line);
            *line++ = ' ';
        }
        //write char of data
        for(j = 0; j < width; j++)
            byte2char(data++, &line);
        //wirte the end of line
        *line = 0;
        //output the line
        PRINT(line_buff);
    }
    //last line of left over if any
    int leftover = size % width;
    if(leftover > 0)
    {
        line = line_buff;
        //write address:
        addr = i*width;
        word2hex((const char*)&addr, &line);
        *line++ = ':'; *line++ = ' ';
        //write hex of data
        for(j = 0; j < leftover; j++)
        {
            byte2hex(&data[j], &line);
            *line++ = ' ';
        }
        //write hex padding
        for(; j < width; j++)
        {
            *line++ = ' ';
            *line++ = ' ';
            *line++ = ' ';
        }
        //write char of data
        for(j = 0; j < leftover; j++)
            byte2char(data++, &line);
        //write the end of line
        *line = 0;
        //output the line
        PRINT(line_buff);
    }
}

/*******************************************************************************
**
** Function         scru_dump_hex
**
** Description      print a text string to log
**
** Returns:
**                  text string
**
*******************************************************************************/
UINT8 *scru_dump_hex (UINT8 *p, char *pTitle, UINT32 len, UINT32 layer, UINT32 type)
{
    if(pTitle && *pTitle)
        PRINT(pTitle);
    dumpbin(p, len, layer, type);
    return p;
}

/*******************************************************************************
**
** Function         DispHciCmd
**
** Description      Display a HCI command string
**
** Returns:
**                  Nothing
**
*******************************************************************************/
void DispHciCmd (BT_HDR *p_buf)
{
    int i,j;
    int nBytes = ((BT_HDR_SIZE + p_buf->offset + p_buf->len)*2)+1;
    UINT8 * data = (UINT8*) p_buf;
    int data_len = BT_HDR_SIZE + p_buf->offset + p_buf->len;

    if (!(ScrProtocolTraceFlag & SCR_PROTO_TRACE_HCI_SUMMARY))
        return;

    if (nBytes > sizeof(log_line))
        return;

    for(i = 0, j = 0; i < data_len && j < sizeof(log_line)-3; i++)
    {
        log_line[j++] = sTable[(*data >> 4) & 0xf];
        log_line[j++] = sTable[*data & 0xf];
        data++;
    }
    log_line[j] = '\0';

    __android_log_write(ANDROID_LOG_DEBUG, "BrcmHciX", log_line);
}


/*******************************************************************************
**
** Function         DispHciEvt
**
** Description      display a NCI event
**
** Returns:
**                  Nothing
**
*******************************************************************************/
void DispHciEvt (BT_HDR *p_buf)
{
    int i,j;
    int nBytes = ((BT_HDR_SIZE + p_buf->offset + p_buf->len)*2)+1;
    UINT8 * data = (UINT8*) p_buf;
    int data_len = BT_HDR_SIZE + p_buf->offset + p_buf->len;

    if (!(ScrProtocolTraceFlag & SCR_PROTO_TRACE_HCI_SUMMARY))
        return;

    if (nBytes > sizeof(log_line))
        return;

    for(i = 0, j = 0; i < data_len && j < sizeof(log_line)-3; i++)
    {
        log_line[j++] = sTable[(*data >> 4) & 0xf];
        log_line[j++] = sTable[*data & 0xf];
        data++;
    }
    log_line[j] = '\0';

    __android_log_write(ANDROID_LOG_DEBUG, "BrcmHciR", log_line);
}

/*******************************************************************************
**
** Function         DispNciDump
**
** Description      Log raw NCI packet as hex-ascii bytes
**
** Returns          None.
**
*******************************************************************************/
void DispNciDump (UINT8 *data, UINT16 len, BOOLEAN is_recv)
{
    if (!(ScrProtocolTraceFlag & SCR_PROTO_TRACE_NCI))
        return;

    char line_buf[(MAX_NCI_PACKET_SIZE*2)+1];
    int i,j;

    for(i = 0, j = 0; i < len && j < sizeof(line_buf)-3; i++)
    {
        line_buf[j++] = sTable[(*data >> 4) & 0xf];
        line_buf[j++] = sTable[*data & 0xf];
        data++;
    }
    line_buf[j] = '\0';

    __android_log_write(ANDROID_LOG_DEBUG, (is_recv) ? "BrcmNciR": "BrcmNciX", line_buf);
}


/*******************************************************************************
**
** Function         DispLLCP
**
** Description      Log raw LLCP packet as hex-ascii bytes
**
** Returns          None.
**
*******************************************************************************/
void DispLLCP (BT_HDR *p_buf, BOOLEAN is_recv)
{
    int i,j;
    int nBytes = ((BT_HDR_SIZE + p_buf->offset + p_buf->len)*2)+1;
    UINT8 * data = (UINT8*) p_buf;
    int data_len = BT_HDR_SIZE + p_buf->offset + p_buf->len;

    if (appl_trace_level < BT_TRACE_LEVEL_DEBUG)
        return;

    for (i = 0; i < data_len; )
    {
        for(j = 0; i < data_len && j < sizeof(log_line)-3; i++)
        {
            log_line[j++] = sTable[(*data >> 4) & 0xf];
            log_line[j++] = sTable[*data & 0xf];
            data++;
        }
        log_line[j] = '\0';
        __android_log_write(ANDROID_LOG_DEBUG, (is_recv) ? "BrcmLlcpR": "BrcmLlcpX", log_line);
    }
}


/*******************************************************************************
**
** Function         DispHcp
**
** Description      Log raw HCP packet as hex-ascii bytes
**
** Returns          None.
**
*******************************************************************************/
void DispHcp (UINT8 *data, UINT16 len, BOOLEAN is_recv)
{
    int i,j;
    int nBytes = (len*2)+1;
    char line_buf[400];

    if (appl_trace_level < BT_TRACE_LEVEL_DEBUG)
        return;

    if (nBytes > sizeof(line_buf))
        return;

    // Only trace HCP if we're tracing HCI as well
    if (!(ScrProtocolTraceFlag & SCR_PROTO_TRACE_HCI_SUMMARY))
        return;

    for(i = 0, j = 0; i < len && j < sizeof(line_buf)-3; i++)
    {
        line_buf[j++] = sTable[(*data >> 4) & 0xf];
        line_buf[j++] = sTable[*data & 0xf];
        data++;
    }
    line_buf[j] = '\0';

    __android_log_write(ANDROID_LOG_DEBUG, (is_recv) ? "BrcmHcpR": "BrcmHcpX", line_buf);
}

void DispSNEP (UINT8 local_sap, UINT8 remote_sap, BT_HDR *p_buf, BOOLEAN is_first, BOOLEAN is_rx) {}
void DispCHO (UINT8 *pMsg, UINT32 MsgLen, BOOLEAN is_rx) {}
void DispT3TagMessage(BT_HDR *p_msg, BOOLEAN is_rx) {}
void DispRWT4Tags (BT_HDR *p_buf, BOOLEAN is_rx) {}
void DispCET4Tags (BT_HDR *p_buf, BOOLEAN is_rx) {}
void DispRWI93Tag (BT_HDR *p_buf, BOOLEAN is_rx, UINT8 command_to_respond) {}
void DispNDEFMsg (UINT8 *pMsg, UINT32 MsgLen, BOOLEAN is_recv) {}
