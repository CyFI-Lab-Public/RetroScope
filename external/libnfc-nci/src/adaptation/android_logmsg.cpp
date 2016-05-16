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
#include "buildcfg.h"
#include "bt_types.h"
#include <cutils/log.h>


#ifndef BTE_LOG_BUF_SIZE
    #define BTE_LOG_BUF_SIZE  1024
#endif
#define BTE_LOG_MAX_SIZE  (BTE_LOG_BUF_SIZE - 12)


extern "C"
{
    void LogMsg (UINT32 trace_set_mask, const char *fmt_str, ...);
}

/*******************************************************************************
**
** Function:    ScrLog
**
** Description: log a message
**
** Returns:     none
**
*******************************************************************************/
void ScrLog (UINT32 trace_set_mask, const char *fmt_str, ...)
{
    static char buffer[BTE_LOG_BUF_SIZE];
    va_list ap;

    va_start(ap, fmt_str);
    vsnprintf(buffer, BTE_LOG_MAX_SIZE, fmt_str, ap);
    va_end(ap);
    __android_log_write(ANDROID_LOG_INFO, "BrcmNci", buffer);
}


/*******************************************************************************
**
** Function:    LogMsg
**
** Description: log a message
**
** Returns:     none
**
*******************************************************************************/
void LogMsg (UINT32 trace_set_mask, const char *fmt_str, ...)
{
    static char buffer[BTE_LOG_BUF_SIZE];
    va_list ap;
    UINT32 trace_type = trace_set_mask & 0x07; //lower 3 bits contain trace type
    int android_log_type = ANDROID_LOG_INFO;

    va_start(ap, fmt_str);
    vsnprintf(buffer, BTE_LOG_MAX_SIZE, fmt_str, ap);
    va_end(ap);
    if (trace_type == TRACE_TYPE_ERROR)
        android_log_type = ANDROID_LOG_ERROR;
    __android_log_write(android_log_type, "BrcmNfcNfa", buffer);
}

