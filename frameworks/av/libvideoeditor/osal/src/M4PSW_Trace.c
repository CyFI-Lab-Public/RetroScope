/*
 * Copyright (C) 2011 The Android Open Source Project
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
/**
 ************************************************************************
 * @file        M4PSW_Trace.c
 * @brief        Trace function for trace macros
 * @note        This file gives the implementation of the trace function used
 *                in the trace instrumentation macros
 ************************************************************************
*/


#include <stdio.h> /*for printf */
#include <stdarg.h> /* ANSI C macros and defs for variable args */
#include "utils/Log.h"

#include "M4OSA_Types.h"
#include "M4OSA_Debug.h"

#define NO_FILE /* suppresses the file name print out */

#define MAX_STRING_SIZE 1024

/**
 ************************************************************************
 * void M4OSA_Trace(M4OSA_Int32 line, M4OSA_Char* file ,M4OSA_Int32 level,
 *                                                      M4OSA_Char* format, ...)
 * @brief    This function implements the trace for debug tests
 * @note    This implementation uses printf. First the variables are retrieved using
 *            ANSI C defs and macros which enable to access a variable number of arguments.
 *            Then the printf is done (with some ornemental adds).
 * @param    level (IN): the debug level
 * @param    format (IN): the "printf" formated string
 * @param    ... (IN): as many parameters as required ...
 * @return    none
 ************************************************************************
*/

M4OSAL_TRACE_EXPORT_TYPE void M4OSA_Trace(M4OSA_Int32 line, M4OSA_Char* file ,
                                     M4OSA_Int32 level, M4OSA_Char* format, ...)
{
    M4OSA_Char message[MAX_STRING_SIZE];
    M4OSA_Int32 i;
    va_list marker; /* pointer to list of arguments */

    /* get the var arguments into the string message to be able to print */
    va_start(marker,format); /* set ptr to first argument in the list of arguments passed to the function */
    vsprintf((char *)message, (const char *)format,marker );  /* formats and writes the data into message */
    va_end(marker); /* reset pointer to NULL */

    /* do the actual print */
#ifdef NO_FILE
    __android_log_print(ANDROID_LOG_INFO, "M4OSA_Trace", "%s", (char*)message);
#else /* NO_FILE     */
    __android_log_print(ANDROID_LOG_INFO, "M4OSA_Trace", "%s", "%s at %lu in %s",
                                                   (char *)message, line, file);
#endif /* NO_FILE     */

}

M4OSAL_TRACE_EXPORT_TYPE M4OSA_Void M4OSA_TRACE_traceFunction(M4OSA_UInt32 line,
                                                              M4OSA_Char* fileName,
                                                              M4OSA_CoreID coreID,
                                                              M4OSA_UInt32 level,
                                                              M4OSA_Char* stringMsg, ...)
{
    M4OSA_Char message[MAX_STRING_SIZE];
    M4OSA_Int32 i;
    va_list marker; /* pointer to list of arguments */

    /* get the var arguments into the string message to be able to print */
    va_start(marker,stringMsg); /* set ptr to first argument in the list of arguments passed to the function */
    vsprintf((char *)message, (const char *)stringMsg,marker );  /* formats and writes the data into message */
    va_end(marker); /* reset pointer to NULL */

    /* do the actual print */
#ifdef NO_FILE
    __android_log_print(ANDROID_LOG_INFO, "M4OSA_TRACE_traceFunction", "%s", (char*)message);
#else /* NO_FILE     */
    __android_log_print(ANDROID_LOG_INFO, "M4OSA_TRACE_traceFunction", "%s", "%s at %lu in %s",
                                            (char *)message, line, (char*)file);
#endif /* NO_FILE     */

}

