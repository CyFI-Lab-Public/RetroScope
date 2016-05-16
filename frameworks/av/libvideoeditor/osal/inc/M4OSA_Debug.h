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
 * @file         M4OSA_Debug.h
 * @brief        Debug and Trace Macro
 ************************************************************************
*/


#ifndef _M4OSA_DEBUG_H_
#define _M4OSA_DEBUG_H_

#include "M4OSA_Error.h"
#include "M4OSA_Types.h"


/* defaut value, defined only if not defined already. */
#ifndef M4TRACE_ID
#define M4TRACE_ID M4UNKNOWN_COREID
#endif /* M4TRACE_ID undefined */


#define M4OSA_SUPER_DEBUG_LEVEL 0

#ifndef M4OSA_DEBUG_LEVEL
#define M4OSA_DEBUG_LEVEL       0
#endif


#define M4OSA_SUPER_TRACE_LEVEL 0

#ifndef M4OSA_TRACE_LEVEL
#define M4OSA_TRACE_LEVEL       0
#endif

#ifdef __cplusplus
extern "C"
{
#endif


#if (M4OSA_DEBUG_LEVEL >= 1) || (M4OSA_SUPER_DEBUG_LEVEL >= 1)

/* Debug macros */
extern M4OSA_Void M4OSA_DEBUG_traceFunction(M4OSA_UInt32 line,
                                            M4OSA_Char* fileName,
                                            M4OSA_UInt32 level,
                                            M4OSA_Char* stringCondition,
                                            M4OSA_Char* message,
                                            M4OSA_ERR returnedError);


#define M4OSA_DEBUG_IFx(cond, errorCode, msg, level)\
      if(cond)\
      {\
         M4OSA_DEBUG_traceFunction(__LINE__, (M4OSA_Char*)__FILE__, level,\
                                   (M4OSA_Char*)#cond, (M4OSA_Char*)msg,\
                                   (errorCode));\
         return(errorCode);\
      }

#define M4OSA_DEBUG(errorCode, msg)\
         M4OSA_DEBUG_traceFunction(__LINE__, (M4OSA_Char*)__FILE__, 1,\
                                   (M4OSA_Char*)#errorCode, (M4OSA_Char*)msg,\
                                   (errorCode));

#else /*(M4OSA_DEBUG_LEVEL >= 1) || (M4OSA_SUPER_DEBUG_LEVEL >= 1)*/


#define M4OSA_DEBUG(errorCode, msg)

#endif /*(M4OSA_DEBUG_LEVEL >= 1) || (M4OSA_SUPER_DEBUG_LEVEL >= 1)*/



#if (M4OSA_DEBUG_LEVEL >= 1) || (M4OSA_SUPER_DEBUG_LEVEL >= 1)
 #define M4OSA_DEBUG_IF1(cond, errorCode, msg)\
         M4OSA_DEBUG_IFx(cond, errorCode, msg, 1)
#else
 #define M4OSA_DEBUG_IF1(cond, errorCode, msg)
#endif /*(M4OSA_DEBUG_LEVEL >= 1) || (M4OSA_SUPER_DEBUG_LEVEL >= 1)*/


#if (M4OSA_DEBUG_LEVEL >= 2) || (M4OSA_SUPER_DEBUG_LEVEL >= 2)
 #define M4OSA_DEBUG_IF2(cond, errorCode, msg)\
         M4OSA_DEBUG_IFx(cond, errorCode, msg, 2)
#else
 #define M4OSA_DEBUG_IF2(cond, errorCode, msg)
#endif /*(M4OSA_DEBUG_LEVEL >= 2) || (M4OSA_SUPER_DEBUG_LEVEL >= 2)*/


#if (M4OSA_DEBUG_LEVEL >= 3) || (M4OSA_SUPER_DEBUG_LEVEL >= 3)
 #define M4OSA_DEBUG_IF3(cond, errorCode, msg)\
         M4OSA_DEBUG_IFx(cond, errorCode, msg, 3)
#else
 #define M4OSA_DEBUG_IF3(cond, errorCode, msg)
#endif /*(M4OSA_DEBUG_LEVEL >= 3) || (M4OSA_SUPER_DEBUG_LEVEL >= 3)*/



/* Trace macros */

#if (M4OSA_TRACE_LEVEL >= 1) || (M4OSA_SUPER_TRACE_LEVEL >= 1)

extern M4OSA_Void M4OSA_TRACE_traceFunction(M4OSA_UInt32 line,
                                            M4OSA_Char* fileName,
                                            M4OSA_CoreID coreID,
                                            M4OSA_UInt32 level,
                                            M4OSA_Char* stringMsg,
                                            ... );



#define M4OSA_TRACEx_0(msg, level)\
      M4OSA_TRACE_traceFunction(__LINE__, (M4OSA_Char*)__FILE__,\
                             (M4OSA_CoreID)M4TRACE_ID, level, (M4OSA_Char*)msg);


#define M4OSA_TRACEx_1(msg, param1, level)\
      M4OSA_TRACE_traceFunction(__LINE__, (M4OSA_Char*)__FILE__,\
                     (M4OSA_CoreID)M4TRACE_ID, level, (M4OSA_Char*)msg, param1);


#define M4OSA_TRACEx_2(msg, param1, param2, level)\
      M4OSA_TRACE_traceFunction(__LINE__, (M4OSA_Char*)__FILE__,\
                                (M4OSA_CoreID)M4TRACE_ID, level,\
                                (M4OSA_Char*)msg, param1,\
                                param2);


#define M4OSA_TRACEx_3(msg, param1, param2, param3, level)\
      M4OSA_TRACE_traceFunction(__LINE__, (M4OSA_Char*)__FILE__,\
                                (M4OSA_CoreID)M4TRACE_ID, level, (M4OSA_Char*)msg,\
                                param1,param2, param3);


#define M4OSA_TRACEx_4(msg, param1, param2, param3, param4, level)\
      M4OSA_TRACE_traceFunction(__LINE__, (M4OSA_Char*)__FILE__,\
                                (M4OSA_CoreID)M4TRACE_ID, level,\
                                (M4OSA_Char*)msg, param1,\
                                param2, param3, param4);


#define M4OSA_TRACEx_5(msg, param1, param2, param3, param4, param5, level)\
      M4OSA_TRACE_traceFunction(__LINE__, (M4OSA_Char*)__FILE__,\
                                (M4OSA_CoreID)M4TRACE_ID, level,\
                                (M4OSA_Char*)msg, param1,\
                                param2, param3, param4, param5);

#endif /*(M4OSA_TRACE_LEVEL >= 1) || (M4OSA_SUPER_TRACE_LEVEL >= 1)*/



#if (M4OSA_TRACE_LEVEL >= 1) || (M4OSA_SUPER_TRACE_LEVEL >= 1)
#define M4OSA_TRACE1_0(msg)\
        M4OSA_TRACEx_0(msg, 1)

#define M4OSA_TRACE1_1(msg, param1)\
        M4OSA_TRACEx_1(msg, param1, 1)

#define M4OSA_TRACE1_2(msg, param1, param2)\
        M4OSA_TRACEx_2(msg, param1, param2, 1)

#define M4OSA_TRACE1_3(msg, param1, param2, param3)\
        M4OSA_TRACEx_3(msg, param1, param2, param3, 1)

#define M4OSA_TRACE1_4(msg, param1, param2, param3, param4)\
        M4OSA_TRACEx_4(msg, param1, param2, param3, param4, 1)

#define M4OSA_TRACE1_5(msg, param1, param2, param3, param4, param5)\
        M4OSA_TRACEx_5(msg, param1, param2, param3, param4, param5, 1)

#else /*(M4OSA_TRACE_LEVEL >= 1) || (M4OSA_SUPER_TRACE_LEVEL >= 1)*/

#define M4OSA_TRACE1_0(msg)
#define M4OSA_TRACE1_1(msg, param1)
#define M4OSA_TRACE1_2(msg, param1, param2)
#define M4OSA_TRACE1_3(msg, param1, param2, param3)
#define M4OSA_TRACE1_4(msg, param1, param2, param3, param4)
#define M4OSA_TRACE1_5(msg, param1, param2, param3, param4, param5)

#endif /*(M4OSA_TRACE_LEVEL >= 1) || (M4OSA_SUPER_TRACE_LEVEL >= 1)*/


#if (M4OSA_TRACE_LEVEL >= 2) || (M4OSA_SUPER_TRACE_LEVEL >= 2)
#define M4OSA_TRACE2_0(msg)\
        M4OSA_TRACEx_0(msg, 2)

#define M4OSA_TRACE2_1(msg, param1)\
        M4OSA_TRACEx_1(msg, param1, 2)

#define M4OSA_TRACE2_2(msg, param1, param2)\
        M4OSA_TRACEx_2(msg, param1, param2, 2)

#define M4OSA_TRACE2_3(msg, param1, param2, param3)\
        M4OSA_TRACEx_3(msg, param1, param2, param3, 2)

#define M4OSA_TRACE2_4(msg, param1, param2, param3, param4)\
        M4OSA_TRACEx_4(msg, param1, param2, param3, param4, 2)

#define M4OSA_TRACE2_5(msg, param1, param2, param3, param4, param5)\
        M4OSA_TRACEx_5(msg, param1, param2, param3, param4, param5, 2)

#else /*(M4OSA_TRACE_LEVEL >= 2) || (M4OSA_SUPER_TRACE_LEVEL >= 2)*/

#define M4OSA_TRACE2_0(msg)
#define M4OSA_TRACE2_1(msg, param1)
#define M4OSA_TRACE2_2(msg, param1, param2)
#define M4OSA_TRACE2_3(msg, param1, param2, param3)
#define M4OSA_TRACE2_4(msg, param1, param2, param3, param4)
#define M4OSA_TRACE2_5(msg, param1, param2, param3, param4, param5)
#endif /*(M4OSA_TRACE_LEVEL >= 2) || (M4OSA_SUPER_TRACE_LEVEL >= 2)*/


#if (M4OSA_TRACE_LEVEL >= 3) || (M4OSA_SUPER_TRACE_LEVEL >= 3)
#define M4OSA_TRACE3_0(msg)\
        M4OSA_TRACEx_0(msg, 3)

#define M4OSA_TRACE3_1(msg, param1)\
        M4OSA_TRACEx_1(msg, param1, 3)

#define M4OSA_TRACE3_2(msg, param1, param2)\
        M4OSA_TRACEx_2(msg, param1, param2, 3)

#define M4OSA_TRACE3_3(msg, param1, param2, param3)\
        M4OSA_TRACEx_3(msg, param1, param2, param3, 3)

#define M4OSA_TRACE3_4(msg, param1, param2, param3, param4)\
        M4OSA_TRACEx_4(msg, param1, param2, param3, param4, 3)

#define M4OSA_TRACE3_5(msg, param1, param2, param3, param4, param5)\
        M4OSA_TRACEx_5(msg, param1, param2, param3, param4, param5, 3)

#else /*(M4OSA_TRACE_LEVEL >= 3) || (M4OSA_SUPER_TRACE_LEVEL >= 3)*/

#define M4OSA_TRACE3_0(msg)
#define M4OSA_TRACE3_1(msg, param1)
#define M4OSA_TRACE3_2(msg, param1, param2)
#define M4OSA_TRACE3_3(msg, param1, param2, param3)
#define M4OSA_TRACE3_4(msg, param1, param2, param3, param4)
#define M4OSA_TRACE3_5(msg, param1, param2, param3, param4, param5)

#endif /*(M4OSA_TRACE_LEVEL >= 3) || (M4OSA_SUPER_TRACE_LEVEL >= 3)*/



#ifdef __cplusplus
}
#endif

#endif /* _M4OSA_DEBUG_H_ */

