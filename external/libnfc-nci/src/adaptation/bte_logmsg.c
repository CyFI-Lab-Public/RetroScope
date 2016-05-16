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
#include <stdio.h>
#include <string.h>
#include <stdarg.h>
#include <time.h>
#include <sys/time.h>
#include "bt_target.h"
#include "gki.h"

#define BT_USE_TRACES   TRUE

#if MMI_INCLUDED == TRUE
#include "mmi.h"
#endif

volatile UINT8 bte_target_mode;

#if BT_USE_TRACES == TRUE

#ifdef __CYGWIN__
#undef RPC_INCLUDED
#define RPC_INCLUDED TRUE

/*******************************************************************************
**
** Function:    LogMsg
**
** Description: log a message
**
** Returns:     none
**
*******************************************************************************/
void
LogMsg(UINT32 maskTraceSet, const char *strFormat, ...)
{
    va_list ap;
    char buffer[256];
    struct timeval tv;
    struct timezone tz;
    struct tm *tm;
    time_t t;


    gettimeofday(&tv, &tz);
    time(&t);
    tm = localtime(&t);

    sprintf(buffer, "%02d:%02d:%02d.%03d ", tm->tm_hour, tm->tm_min, tm->tm_sec,
        tv.tv_usec / 1000);
    pth_write(2, buffer, strlen(buffer));

    va_start(ap, strFormat);
    vsprintf(buffer, strFormat, ap);
    pth_write(2, buffer, strlen(buffer));
    pth_write(2, "\n", 1);
    va_end(ap);
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
void
ScrLog(UINT32 maskTraceSet, const char *strFormat, ...)
{
    va_list ap;
    char buffer[256];
    struct timeval tv;
    struct timezone tz;
    struct tm *tm;
    time_t t;

    gettimeofday(&tv, &tz);
    time(&t);
    tm = localtime(&t);

    sprintf(buffer, "%02d:%02d:%02d.%03d ", tm->tm_hour, tm->tm_min, tm->tm_sec,
        tv.tv_usec / 1000);
    pth_write(2, buffer, strlen(buffer));

    va_start(ap, strFormat);
    vsprintf(buffer, strFormat, ap);
    pth_write(2, buffer, strlen(buffer));
    pth_write(2, "\n", 1);
    va_end(ap);
}
#endif

/********************************************************************************
**
**    Function Name:   LogMsg_0
**
**    Purpose:  Encodes a trace message that has no parameter arguments
**
**    Input Parameters:  maskTraceSet: tester trace type.
**                       strFormat: displayable string.
**    Returns:
**                      Nothing.
**
*********************************************************************************/
void LogMsg_0 (UINT32 maskTraceSet, const char *strFormat)
{
    if (bte_target_mode == BTE_MODE_APPL)
    {
#if RPC_INCLUDED == TRUE
        LogMsg(maskTraceSet, strFormat);
#else
        return; /* No RPC */
#endif
    }
    else if (bte_target_mode == BTE_MODE_SAMPLE_APPS)  /* Using demo sample apps */
    {
#if RPC_INCLUDED == TRUE
        LogMsg(maskTraceSet, strFormat);
#elif MMI_INCLUDED == TRUE
        if (mmi_debug_traces)
            MMI_Echo(strFormat);
#endif
    }

#if (defined(TRACE_TASK_INCLUDED) && TRACE_TASK_INCLUDED == TRUE)
    LogMsg(maskTraceSet, strFormat);
#endif

#if (defined(DONGLE_MODE_INCLUDED) && DONGLE_MODE_INCLUDED == TRUE)
    else if (bte_target_mode == BTE_MODE_DONGLE)
        bte_hcisl_send_traces(maskTraceSet, strFormat);
#endif


}

/********************************************************************************
**
**    Function Name:   LogMsg_1
**
**    Purpose:  Encodes a trace message that has one parameter argument
**
**    Input Parameters:  maskTraceSet: tester trace type.
**                       strFormat: displayable string.
**    Returns:
**                      Nothing.
**
*********************************************************************************/
void LogMsg_1 (UINT32 maskTraceSet, const char *strFormat, UINT32 p1)
{
    if (bte_target_mode == BTE_MODE_APPL)
    {
#if RPC_INCLUDED == TRUE
        LogMsg (maskTraceSet, strFormat, p1);
#else
        return; /* No RPC */
#endif
    }
    else if (bte_target_mode == BTE_MODE_SAMPLE_APPS)  /* Using demo sample apps */
    {
#if RPC_INCLUDED == TRUE
        LogMsg (maskTraceSet, strFormat, p1);
#elif MMI_INCLUDED == TRUE
        if (mmi_debug_traces)
            MMI_Echo(strFormat, p1);
#endif
    }

#if (defined(TRACE_TASK_INCLUDED) && TRACE_TASK_INCLUDED == TRUE)
    LogMsg (maskTraceSet, strFormat, p1);
#endif

#if (defined(DONGLE_MODE_INCLUDED) && DONGLE_MODE_INCLUDED == TRUE)
    else if (bte_target_mode == BTE_MODE_DONGLE)
        bte_hcisl_send_traces(maskTraceSet, strFormat, p1);
#endif
}

/********************************************************************************
**
**    Function Name:   LogMsg_2
**
**    Purpose:  Encodes a trace message that has two parameter arguments
**
**    Input Parameters:  maskTraceSet: tester trace type.
**                       strFormat: displayable string.
**    Returns:
**                      Nothing.
**
*********************************************************************************/
void LogMsg_2 (UINT32 maskTraceSet, const char *strFormat, UINT32 p1, UINT32 p2)
{
    if (bte_target_mode == BTE_MODE_APPL)
    {
#if RPC_INCLUDED == TRUE
        LogMsg (maskTraceSet, strFormat, p1, p2);
#else
        return; /* No RPC */
#endif
    }
    else if (bte_target_mode == BTE_MODE_SAMPLE_APPS)  /* Using demo sample apps */
    {
#if RPC_INCLUDED == TRUE
        LogMsg (maskTraceSet, strFormat, p1, p2);
#elif MMI_INCLUDED == TRUE
        if (mmi_debug_traces)
            MMI_Echo(strFormat, p1, p2);
#endif
    }

#if (defined(TRACE_TASK_INCLUDED) && TRACE_TASK_INCLUDED == TRUE)
    LogMsg (maskTraceSet, strFormat, p1, p2);
#endif

#if (defined(DONGLE_MODE_INCLUDED) && DONGLE_MODE_INCLUDED == TRUE)
    else if (bte_target_mode == BTE_MODE_DONGLE)
        bte_hcisl_send_traces(maskTraceSet, strFormat, p1, p2);
#endif
}

/********************************************************************************
**
**    Function Name:   LogMsg_3
**
**    Purpose:  Encodes a trace message that has three parameter arguments
**
**    Input Parameters:  maskTraceSet: tester trace type.
**                       strFormat: displayable string.
**    Returns:
**                      Nothing.
**
*********************************************************************************/
void LogMsg_3 (UINT32 maskTraceSet, const char *strFormat, UINT32 p1, UINT32 p2, UINT32 p3)
{
    if (bte_target_mode == BTE_MODE_APPL)
    {
#if RPC_INCLUDED == TRUE
        LogMsg (maskTraceSet, strFormat, p1, p2, p3);
#else
        return; /* No RPC */
#endif
    }
    else if (bte_target_mode == BTE_MODE_SAMPLE_APPS)  /* Using demo sample apps */
    {
#if RPC_INCLUDED == TRUE
        LogMsg (maskTraceSet, strFormat, p1, p2, p3);
#elif MMI_INCLUDED == TRUE
        if (mmi_debug_traces)
            MMI_Echo(strFormat, p1, p2, p3);
#endif
    }

#if (defined(TRACE_TASK_INCLUDED) && TRACE_TASK_INCLUDED == TRUE)
    LogMsg (maskTraceSet, strFormat, p1, p2, p3);
#endif

#if (defined(DONGLE_MODE_INCLUDED) && DONGLE_MODE_INCLUDED == TRUE)
    else if (bte_target_mode == BTE_MODE_DONGLE)
        bte_hcisl_send_traces(maskTraceSet, strFormat, p1, p2, p3);
#endif
}

/********************************************************************************
**
**    Function Name:   LogMsg_4
**
**    Purpose:  Encodes a trace message that has four parameter arguments
**
**    Input Parameters:  maskTraceSet: tester trace type.
**                       strFormat: displayable string.
**    Returns:
**                      Nothing.
**
*********************************************************************************/
void LogMsg_4 (UINT32 maskTraceSet, const char *strFormat, UINT32 p1, UINT32 p2,
               UINT32 p3, UINT32 p4)
{
    if (bte_target_mode == BTE_MODE_APPL)
    {
#if RPC_INCLUDED == TRUE
        LogMsg (maskTraceSet, strFormat, p1, p2, p3, p4);
#else
        return; /* No RPC */
#endif
    }
    else if (bte_target_mode == BTE_MODE_SAMPLE_APPS)  /* Using demo sample apps */
    {
#if RPC_INCLUDED == TRUE
        LogMsg (maskTraceSet, strFormat, p1, p2, p3, p4);
#elif MMI_INCLUDED == TRUE
        if (mmi_debug_traces)
            MMI_Echo(strFormat, p1, p2, p3, p4);
#endif
    }

#if (defined(TRACE_TASK_INCLUDED) && TRACE_TASK_INCLUDED == TRUE)
    LogMsg (maskTraceSet, strFormat, p1, p2, p3, p4);
#endif

#if (defined(DONGLE_MODE_INCLUDED) && DONGLE_MODE_INCLUDED == TRUE)
    else if (bte_target_mode == BTE_MODE_DONGLE)
        bte_hcisl_send_traces(maskTraceSet, strFormat, p1, p2, p3, p4);
#endif
}

/********************************************************************************
**
**    Function Name:   LogMsg_5
**
**    Purpose:  Encodes a trace message that has five parameter arguments
**
**    Input Parameters:  maskTraceSet: tester trace type.
**                       strFormat: displayable string.
**    Returns:
**                      Nothing.
**
*********************************************************************************/
void LogMsg_5 (UINT32 maskTraceSet, const char *strFormat, UINT32 p1, UINT32 p2,
               UINT32 p3, UINT32 p4, UINT32 p5)
{
    if (bte_target_mode == BTE_MODE_APPL)
    {
#if RPC_INCLUDED == TRUE
        LogMsg (maskTraceSet, strFormat, p1, p2, p3, p4, p5);
#else
        return; /* No RPC */
#endif
    }
    else if (bte_target_mode == BTE_MODE_SAMPLE_APPS)  /* Using demo sample apps */
    {
#if RPC_INCLUDED == TRUE
        LogMsg (maskTraceSet, strFormat, p1, p2, p3, p4, p5);
#elif MMI_INCLUDED == TRUE
        if (mmi_debug_traces)
            MMI_Echo(strFormat, p1, p2, p3, p4, p5);
#endif
    }

#if (defined(TRACE_TASK_INCLUDED) && TRACE_TASK_INCLUDED == TRUE)
    LogMsg (maskTraceSet, strFormat, p1, p2, p3, p4, p5);
#endif

#if (defined(DONGLE_MODE_INCLUDED) && DONGLE_MODE_INCLUDED == TRUE)
    else if (bte_target_mode == BTE_MODE_DONGLE)
        bte_hcisl_send_traces(maskTraceSet, strFormat, p1, p2, p3, p4, p5);
#endif
}

/********************************************************************************
**
**    Function Name:   LogMsg_6
**
**    Purpose:  Encodes a trace message that has six parameter arguments
**
**    Input Parameters:  maskTraceSet: tester trace type.
**                       strFormat: displayable string.
**    Returns:
**                      Nothing.
**
*********************************************************************************/
void LogMsg_6 (UINT32 maskTraceSet, const char *strFormat, UINT32 p1, UINT32 p2,
               UINT32 p3, UINT32 p4, UINT32 p5, UINT32 p6)
{
    if (bte_target_mode == BTE_MODE_APPL)
    {
#if RPC_INCLUDED == TRUE
        LogMsg (maskTraceSet, strFormat, p1, p2, p3, p4, p5, p6);
#else
        return; /* No RPC */
#endif
    }
    else if (bte_target_mode == BTE_MODE_SAMPLE_APPS)  /* Using demo sample apps */
    {
#if RPC_INCLUDED == TRUE
        LogMsg (maskTraceSet, strFormat, p1, p2, p3, p4, p5, p6);
#elif MMI_INCLUDED == TRUE
        if (mmi_debug_traces)
            MMI_Echo(strFormat, p1, p2, p3, p4, p5, p6);
#endif
    }


#if (defined(TRACE_TASK_INCLUDED) && TRACE_TASK_INCLUDED == TRUE)
    LogMsg (maskTraceSet, strFormat, p1, p2, p3, p4, p5, p6);
#endif

#if (defined(DONGLE_MODE_INCLUDED) && DONGLE_MODE_INCLUDED == TRUE)
    else if (bte_target_mode == BTE_MODE_DONGLE)
        bte_hcisl_send_traces(maskTraceSet, strFormat, p1, p2, p3, p4, p5, p6);
#endif

}

#endif /* BT_USE_TRACES */

