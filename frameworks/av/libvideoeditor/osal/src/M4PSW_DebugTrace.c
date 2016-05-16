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
 * @file       M4PSW_DebugTrace.c
 * @brief      Default trace function for debugging macros
 * @note       This file gives the default implementation of the trace function
 *             used in the debug instrumentation macros, based on printf.
 *             Application writers are strongly encouraged to implement their
 *             own "M4OSA_DebugTrace".
 ************************************************************************
*/


#include <stdio.h> /*for printf */

#include "M4OSA_Types.h"
#include "M4OSA_Error.h"

/*#define NO_FILE */ /* suppresses the file name print out */


/**
 ************************************************************************
 * void M4OSA_DebugTrace(M4OSA_Int32 line, char* file, M4OSA_Int32 level,
 *                       M4OSA_Char* cond, char* msg, M4OSA_ERR err)
 * @brief    This function implements the trace for debug tests
 * @note    This function is to be called in the debug macros only.
 *            This implementation uses printf.
 * @param    line (IN): the line number in the source file
 * @param    file (IN): the source file name
 * @param    level (IN): the debug level
 * @param    msg (IN): the error message
 * @param    err (IN): the return value (error code)
 * @return    none
 ************************************************************************
*/

M4OSAL_TRACE_EXPORT_TYPE void M4OSA_DebugTrace(M4OSA_Int32 line,
                                               M4OSA_Char* file,
                                               M4OSA_Int32 level,
                                               M4OSA_Char* cond,
                                               M4OSA_Char* msg,
                                               M4OSA_ERR err)
{
    M4OSA_Int32 i;

    /* try to "indent" the resulting traces depending on the level */
    for (i =0 ; i < level; i ++)
    {
        printf(" ");
    }

#ifdef NO_FILE
    printf("Error: %li, on %s: %s\n",err,cond,msg);
#else /* NO_FILE     */
    printf("Error: %li, on %s: %s Line %lu in: %s\n",err,cond,msg,line,file);
#endif /* NO_FILE     */

}

M4OSAL_TRACE_EXPORT_TYPE M4OSA_Void M4OSA_DEBUG_traceFunction(M4OSA_UInt32 line,
                                                              M4OSA_Char* fileName,
                                                              M4OSA_UInt32 level,
                                                              M4OSA_Char* stringCondition,
                                                              M4OSA_Char* message,
                                                              M4OSA_ERR returnedError)
{
    M4OSA_DebugTrace(line, fileName, level, stringCondition, message, returnedError);
}

