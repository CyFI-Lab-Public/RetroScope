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
 * @file         M4OSA_Thread.h
 * @ingroup      OSAL
 * @brief        thread API
 ************************************************************************
*/


#ifndef M4OSA_THREAD_H
#define M4OSA_THREAD_H

#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4OSA_OptionID.h"


/* Definition of common error codes */
#define M4ERR_THREAD_NOT_STARTED M4OSA_ERR_CREATE(M4_ERR,M4OSA_THREAD,0x000001)


typedef enum
{
   M4OSA_kThreadOpened   = 0x100,
   M4OSA_kThreadStarting = 0x200,
   M4OSA_kThreadRunning  = 0x300,
   M4OSA_kThreadStopping = 0x400,
   M4OSA_kThreadClosed  = 0x500
} M4OSA_ThreadState;



typedef enum
{
   M4OSA_kThreadHighestPriority  =  0x000,
   M4OSA_kThreadHighPriority     =  0x100,
   M4OSA_kThreadNormalPriority   =  0x200,
   M4OSA_kThreadLowPriority      =  0x300,
   M4OSA_kThreadLowestPriority   =  0x400
} M4OSA_ThreadPriorityLevel;



typedef enum
{
   M4OSA_ThreadStarted
      = M4OSA_OPTION_ID_CREATE(M4_READ|M4_WRITE, M4OSA_THREAD, 0x01),

   M4OSA_ThreadStopped
      = M4OSA_OPTION_ID_CREATE(M4_READ|M4_WRITE, M4OSA_THREAD, 0x02),

   M4OSA_ThreadPriority
      = M4OSA_OPTION_ID_CREATE(M4_READ|M4_WRITE, M4OSA_THREAD, 0x03),

   M4OSA_ThreadName
      = M4OSA_OPTION_ID_CREATE(M4_READ|M4_WRITE, M4OSA_THREAD, 0x04),

   M4OSA_ThreadStackSize
      = M4OSA_OPTION_ID_CREATE(M4_READ|M4_WRITE, M4OSA_THREAD, 0x05),

   M4OSA_ThreadUserData
      = M4OSA_OPTION_ID_CREATE(M4_READ|M4_WRITE, M4OSA_THREAD, 0x06)

} M4OSA_ThreadOptionID;



typedef M4OSA_ERR  (*M4OSA_ThreadDoIt)(M4OSA_Void*);
typedef M4OSA_Void (*M4OSA_ThreadCallBack)(M4OSA_Context, M4OSA_Void*);

#ifdef __cplusplus
extern "C"
{
#endif

M4OSAL_REALTIME_EXPORT_TYPE M4OSA_ERR M4OSA_threadSyncOpen(     M4OSA_Context*        context,
                                    M4OSA_ThreadDoIt      func );


M4OSAL_REALTIME_EXPORT_TYPE M4OSA_ERR M4OSA_threadSyncStart(    M4OSA_Context         context,
                                    M4OSA_Void*           param );


M4OSAL_REALTIME_EXPORT_TYPE M4OSA_ERR M4OSA_threadSyncStop(     M4OSA_Context         context );


M4OSAL_REALTIME_EXPORT_TYPE M4OSA_ERR M4OSA_threadSyncClose(    M4OSA_Context         context );


M4OSAL_REALTIME_EXPORT_TYPE M4OSA_ERR M4OSA_threadSyncGetState( M4OSA_Context         context,
                                    M4OSA_ThreadState*    state );


M4OSAL_REALTIME_EXPORT_TYPE M4OSA_ERR M4OSA_threadSleep(        M4OSA_UInt32          time );


M4OSAL_REALTIME_EXPORT_TYPE M4OSA_ERR M4OSA_threadSyncSetOption(M4OSA_Context         context,
                                    M4OSA_ThreadOptionID  option,
                                    M4OSA_DataOption      value );


M4OSAL_REALTIME_EXPORT_TYPE M4OSA_ERR M4OSA_threadSyncGetOption(M4OSA_Context         context,
                                    M4OSA_ThreadOptionID  option,
                                    M4OSA_DataOption*     value );

#ifdef __cplusplus
}
#endif


#endif /*M4OSA_THREAD_H*/

