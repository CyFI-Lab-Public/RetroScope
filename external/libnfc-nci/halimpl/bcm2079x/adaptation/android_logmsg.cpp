/******************************************************************************
 *
 *  Copyright (C) 1999-2012 Broadcom Corporation
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
extern "C"
{
    #include "nfc_hal_target.h"
}
#include <cutils/log.h>


#ifndef BTE_LOG_BUF_SIZE
    #define BTE_LOG_BUF_SIZE  1024
#endif
#define BTE_LOG_MAX_SIZE  (BTE_LOG_BUF_SIZE - 12)
#define MAX_NCI_PACKET_SIZE  259
#define MAX_LOGCAT_LINE     4096
static char log_line[MAX_LOGCAT_LINE];
static const char* sTable = "0123456789abcdef";

extern UINT32 ScrProtocolTraceFlag;         // = SCR_PROTO_TRACE_ALL; // 0x017F;
extern "C"
{
    void DispNci (UINT8 *p, UINT16 len, BOOLEAN is_recv);
    void DispHciCmd (BT_HDR *p_buf);
    void DispHciEvt (BT_HDR *p_buf);
}


void LogMsg (UINT32 trace_set_mask, const char *fmt_str, ...)
{
    static char buffer [BTE_LOG_BUF_SIZE];
    va_list ap;
    UINT32 trace_type = trace_set_mask & 0x07; //lower 3 bits contain trace type
    int android_log_type = ANDROID_LOG_INFO;

    va_start (ap, fmt_str);
    vsnprintf (buffer, BTE_LOG_MAX_SIZE, fmt_str, ap);
    va_end (ap);
    if (trace_type == TRACE_TYPE_ERROR)
        android_log_type = ANDROID_LOG_ERROR;
    __android_log_write (android_log_type, "NfcNciHal", buffer);
}


void DispNci (UINT8 *data, UINT16 len, BOOLEAN is_recv)
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


void DispHciCmd (BT_HDR *p_buf)
{
    int i,j;
    int nBytes = ((BT_HDR_SIZE + p_buf->offset + p_buf->len)*2)+1;
    UINT8 * data = (UINT8*) p_buf;
    int data_len = BT_HDR_SIZE + p_buf->offset + p_buf->len;

    if (appl_trace_level < BT_TRACE_LEVEL_DEBUG)
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


void DispHciEvt (BT_HDR *p_buf)
{
    int i,j;
    int nBytes = ((BT_HDR_SIZE + p_buf->offset + p_buf->len)*2)+1;
    UINT8 * data = (UINT8*) p_buf;
    int data_len = BT_HDR_SIZE + p_buf->offset + p_buf->len;

    if (appl_trace_level < BT_TRACE_LEVEL_DEBUG)
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
