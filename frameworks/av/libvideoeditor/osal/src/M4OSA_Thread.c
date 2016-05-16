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
 * @file         M4OSA_Thread.c
 * @ingroup      OSAL
 * @brief        Implements and manipulate threads
 * @note         This file implements functions to manipulate threads
 ************************************************************************
*/

#include <sched.h>
#include <time.h>
#include <pthread.h>
#include <errno.h>

#include <utils/threads.h>
#include "M4OSA_Debug.h"
#include "M4OSA_Memory.h"
#include "M4OSA_Thread.h"
#include "M4OSA_Thread_priv.h"
#include "M4OSA_Mutex.h"
#include "M4OSA_Semaphore.h"
#include "M4OSA_CharStar.h"


void* M4OSA_threadSyncForEverDo(void *context)
{
   M4OSA_ThreadContext* threadContext = (M4OSA_ThreadContext*)context;
   M4OSA_Bool auto_kill = M4OSA_FALSE;

    /*
       M4OSA_Void* userData;
    */

   M4OSA_TRACE2_1("M4OSA_threadSyncForEverDo\t\tLPVOID 0x%x", context);

    /*
       userData = threadContext->userData;
    */

   M4OSA_mutexLock(threadContext->stateMutex, M4OSA_WAIT_FOREVER);


   threadContext->state = M4OSA_kThreadRunning;

   M4OSA_semaphorePost(threadContext->semStartStop);

   while(threadContext->state == M4OSA_kThreadRunning)
   {
      M4OSA_mutexUnlock(threadContext->stateMutex);

      if((threadContext->func(threadContext->param)) != M4NO_ERROR)
      {
         M4OSA_mutexLock(threadContext->stateMutex, M4OSA_WAIT_FOREVER);

         if(threadContext->state == M4OSA_kThreadRunning)
         {

            //PR 2354 - ACO : Suppress stopping state and don't
            //         unlock mutex before closing the thread
            threadContext->state = M4OSA_kThreadOpened;
            M4OSA_mutexUnlock(threadContext->stateMutex);
            return 0;
         }

         M4OSA_mutexUnlock(threadContext->stateMutex);
      }

      M4OSA_mutexLock(threadContext->stateMutex, M4OSA_WAIT_FOREVER);
   }


   M4OSA_semaphorePost(threadContext->semStartStop);


   M4OSA_mutexUnlock(threadContext->stateMutex);


   return 0;
}





/**
 ************************************************************************
  * @brief      This method creates a new thread. After this call the thread is
 *             identified by its "context". The thread function is provided by
 *             the "func" parameter. This function creates & allocates a unique
 *             context. It's the OSAL real time responsibility for managing its
 *             context. It must be freed by the M4OSA_threadSyncClose function.
 *             The context parameter will be sent back to any OSAL core thread
 *             functions to allow retrieving data associated to the opened
 *             thread.
 * @note       This function creates the thread, but the thread is not running.
 * @note       Once the thread is created, the state is M4OSA_kThreadOpened.
 * @param      context:(OUT) Context of the created thread
 * @param      func:(IN) "doIt" function pointer to run
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_ALLOC: there is no more available memory
 * @return     M4ERR_CONTEXT_FAILED: the context creation failed
  ************************************************************************
*/
M4OSA_ERR M4OSA_threadSyncOpen(M4OSA_Context* context,
                               M4OSA_ThreadDoIt func)
{
   M4OSA_ThreadContext* threadContext = M4OSA_NULL;
   M4OSA_ERR err_code;

   M4OSA_TRACE1_2("M4OSA_threadSyncOpen\t\tM4OSA_Context* 0x%x\t"
                  "M4OSA_ThreadDoIt 0x%x", context, func);

   M4OSA_DEBUG_IF2(context == M4OSA_NULL,
                   M4ERR_PARAMETER, "M4OSA_threadSyncOpen");

   M4OSA_DEBUG_IF2(func == M4OSA_NULL,
                    M4ERR_PARAMETER, "M4OSA_threadSyncOpen");

   *context = M4OSA_NULL;

   threadContext =
      (M4OSA_ThreadContext*)M4OSA_32bitAlignedMalloc(sizeof(M4OSA_ThreadContext),
      M4OSA_THREAD, (M4OSA_Char*)"M4OSA_threadSyncOpen: thread context");

   if(threadContext == M4OSA_NULL)
   {
      M4OSA_DEBUG(M4ERR_ALLOC, "M4OSA_threadSyncOpen");

      return M4ERR_ALLOC;
   }

   threadContext->func = func;
   threadContext->stackSize = 64 * 1024;
   threadContext->name = M4OSA_NULL;
   threadContext->threadID = 0;
   threadContext->coreID = M4OSA_THREAD;
   threadContext->state = M4OSA_kThreadOpened;
   threadContext->priority = M4OSA_kThreadNormalPriority ;

   err_code = M4OSA_mutexOpen(&(threadContext->stateMutex));

   if(M4OSA_ERR_IS_ERROR(err_code))
   {
      M4OSA_DEBUG(err_code, "M4OSA_threadSyncOpen: M4OSA_mutexOpen");

      return err_code;
   }

   err_code = M4OSA_semaphoreOpen(&(threadContext->semStartStop), 0);

   if(M4OSA_ERR_IS_ERROR(err_code))
   {
      M4OSA_DEBUG(err_code, "M4OSA_threadSyncOpen: M4OSA_semaphoreOpen");

      return err_code;
   }

   *context = threadContext;

   return M4NO_ERROR;
}





/**
 ************************************************************************
 * @brief      This method runs a specified thread. The "param" parameter
 *             allows the application to set a specific parameter to the
 *             created thread. This parameter will be used as the second one of
 *             the "M4OSA_ThreadDoIt" function.
 * @note       This method is a blocking up to the thread is running.
 *             Before calling this method, the state is M4OSA_kThreadOpened.
 *             Once the method is called, the state is M4OSA_kThreadStarting.
 *             Once the thread is running, the state is M4OSA_kThreadRunning.
 * @note       This method returns immediately. If the "threadStarted" optionID
 *             is not NULL, the thread will call it before running the doIt
 *             function.
 * @param      context:(IN/OUT) Context of the thread
 * @param      param:(IN) Application data thread parameter
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 * @return     M4ERR_STATE: this function cannot be called now
 * @return     M4ERR_THREAD_NOT_STARTED: the thread did not start
 ************************************************************************
*/
M4OSA_ERR M4OSA_threadSyncStart(M4OSA_Context context,
                                M4OSA_Void* param)
{
   M4OSA_ThreadContext* threadContext =  (M4OSA_ThreadContext*)context;
   pthread_attr_t     attribute = { 0, 0, 0, 0, 0, 0 };
   int                min       = 0;
   int                max       = 0;
   int                priority  = 0;
   struct sched_param sched     = { 0 };

   M4OSA_TRACE1_2("M4OSA_threadSyncStart\t\tM4OSA_Context 0x%x\tM4OSA_Void* "
                  "0x%x", context, param);

   M4OSA_DEBUG_IF2(context == M4OSA_NULL,
                   M4ERR_PARAMETER, "M4OSA_threadSyncStart");

   M4OSA_DEBUG_IF2(threadContext->coreID != M4OSA_THREAD,
                   M4ERR_BAD_CONTEXT, "M4OSA_threadSyncStart");

   M4OSA_mutexLock(threadContext->stateMutex, M4OSA_WAIT_FOREVER);

   if(threadContext->state != M4OSA_kThreadOpened)
   {
      M4OSA_mutexUnlock(threadContext->stateMutex);

      M4OSA_DEBUG(M4ERR_STATE, "M4OSA_threadSyncStart");

      return M4ERR_STATE;
   }

   threadContext->state = M4OSA_kThreadStarting;

   M4OSA_mutexUnlock(threadContext->stateMutex);
   threadContext->param = param;

   if ( 0 == pthread_attr_init( &attribute ) )
   {
      if ( 0 == pthread_attr_setdetachstate( &attribute, PTHREAD_CREATE_DETACHED ) )
      {
         if ( 0 == pthread_attr_setstacksize( &attribute, (size_t)threadContext->stackSize ) )
         {
            if ( 0 == pthread_attr_setschedpolicy( &attribute, SCHED_OTHER ) )
            {
                /* Tentative patches to handle priorities in a better way : */
                /* Use Android's predefined priorities (range +19..-20)
                 *rather than Linux ones (0..99)*/

                /* Get min and max priorities */
                min = sched_get_priority_min( SCHED_FIFO );
                max = sched_get_priority_max( SCHED_FIFO );

                M4OSA_TRACE1_2("M4OSA_threadSyncStart MAX=%d MIN=%d", max, min);

                /* tentative modification of the priorities */
                /* Set the priority based on default android priorities */
                /* This probably requires some more tuning,
                 * outcome of this priority settings are not yet satisfactory */
                /* Implementing thread handling based on Android's thread creation
                 * helpers might bring some improvement (see threads.h) */
                switch(threadContext->priority)
                {
                case M4OSA_kThreadLowestPriority:
                    priority = ANDROID_PRIORITY_NORMAL;
                    break;
                case M4OSA_kThreadLowPriority:
                    priority = ANDROID_PRIORITY_DISPLAY;
                    break;
                case M4OSA_kThreadNormalPriority:
                    priority = ANDROID_PRIORITY_URGENT_DISPLAY;
                    break;
                case M4OSA_kThreadHighPriority:
                    priority = ANDROID_PRIORITY_AUDIO;
                    break;
                case M4OSA_kThreadHighestPriority:
                    priority = ANDROID_PRIORITY_URGENT_AUDIO;
                    break;
                }
                sched.sched_priority = priority;

                if ( 0 == pthread_attr_setschedparam( &attribute, &sched ) )
                {
                    if ( 0 == pthread_create( &threadContext->threadID,
                                              &attribute,
                                              &M4OSA_threadSyncForEverDo,
                                              (void *)threadContext ) )
                    {
                        if ( M4OSA_FALSE == M4OSA_ERR_IS_ERROR( M4OSA_semaphoreWait(
                                                                    threadContext->semStartStop,
                                                                    M4OSA_WAIT_FOREVER ) ) )
                        {
                            return M4NO_ERROR;
                        }
                    }
                }
            }
         }
      }
      pthread_attr_destroy( &attribute );
   }

   M4OSA_mutexLock(threadContext->stateMutex, M4OSA_WAIT_FOREVER);

   threadContext->state = M4OSA_kThreadOpened;

   M4OSA_mutexUnlock(threadContext->stateMutex);

   M4OSA_DEBUG(M4ERR_THREAD_NOT_STARTED, "M4OSA_threadSyncStart");

   return M4ERR_THREAD_NOT_STARTED;
}




/**
 ************************************************************************
 * @brief      This method stops a specified thread.
 * @note       This call is a blocking one up to the "M4OSA_ThreadDoIt"
 *             function has returned.
 *             Before the method is called, the state is M4OSA_kThreadRunning.
 *             Once the method is called, the state is M4OSA_kThreadStopping.
 *             Once the thread is stopped, the state is M4OSA_kThreadOpened.
 * @note       This method returns once the thread has been stopped. If the
 *             "threadStopped" optionID is not NULL, the thread will call it
 *             before dying.
 * @param      context:(IN/OUT) Context of the thread
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_STATE: this function cannot be called now
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 ************************************************************************
*/
M4OSA_ERR M4OSA_threadSyncStop(M4OSA_Context context)
{
   M4OSA_ThreadContext* threadContext = (M4OSA_ThreadContext*)context;

   M4OSA_TRACE1_1("M4OSA_threadSyncStop\t\tM4OSA_Context 0x%x", context);

   M4OSA_DEBUG_IF2(context == M4OSA_NULL,
                   M4ERR_PARAMETER, "M4OSA_threadSyncStop");

   M4OSA_DEBUG_IF2(threadContext->coreID != M4OSA_THREAD,
                   M4ERR_BAD_CONTEXT, "M4OSA_threadSyncStop");

   M4OSA_mutexLock(threadContext->stateMutex, M4OSA_WAIT_FOREVER);

   if(threadContext->state != M4OSA_kThreadRunning)
   {
      M4OSA_mutexUnlock(threadContext->stateMutex);

      M4OSA_DEBUG(M4ERR_STATE, "M4OSA_threadSyncStop");

      return M4ERR_STATE;
   }

   threadContext->state = M4OSA_kThreadStopping;

   M4OSA_mutexUnlock(threadContext->stateMutex);

   M4OSA_semaphoreWait(threadContext->semStartStop, M4OSA_WAIT_FOREVER);

   M4OSA_mutexLock(threadContext->stateMutex, M4OSA_WAIT_FOREVER);

   threadContext->state = M4OSA_kThreadOpened;

   M4OSA_mutexUnlock(threadContext->stateMutex);

   return M4NO_ERROR;
}




/**
 ************************************************************************
 * @brief      This method deletes a thread (identified by its context). After
 *             this call the thread and its context are no more useable. This
 *             function frees all the memory related to this thread.
 * @note       Before the method is called, the state is M4OSA_kThreadOpened.
 *             Once the method is called, the state is M4OSA_kThreadClosed.
 * @param      context:(IN/OUT) Context of the thread
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_STATE: this function cannot be called now
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 ************************************************************************
*/
M4OSA_ERR M4OSA_threadSyncClose(M4OSA_Context context)
{
   M4OSA_ThreadContext* threadContext = (M4OSA_ThreadContext*)context;
   M4OSA_ERR err_code;

   M4OSA_TRACE1_1("M4OSA_threadSyncClose\t\tM4OSA_Context 0x%x", context);

   M4OSA_DEBUG_IF2(context == M4OSA_NULL,
                   M4ERR_PARAMETER, "M4OSA_threadSyncClose");

   M4OSA_DEBUG_IF2(threadContext->coreID != M4OSA_THREAD,
                   M4ERR_BAD_CONTEXT, "M4OSA_threadSyncClose");

   M4OSA_DEBUG_IF2(threadContext->state == M4OSA_kThreadClosed,
                   M4ERR_BAD_CONTEXT, "M4OSA_threadSyncClose");

   M4OSA_mutexLock(threadContext->stateMutex, M4OSA_WAIT_FOREVER);

   if(threadContext->state != M4OSA_kThreadOpened)
   {
      M4OSA_mutexUnlock(threadContext->stateMutex);

      M4OSA_DEBUG(M4ERR_STATE, "M4OSA_threadSyncClose");

      return M4ERR_STATE;
   }

   threadContext->state = M4OSA_kThreadClosed;

   M4OSA_mutexUnlock(threadContext->stateMutex);

   err_code = M4OSA_mutexClose(threadContext->stateMutex);

   if(M4OSA_ERR_IS_ERROR(err_code))
   {
      M4OSA_DEBUG(err_code, "M4OSA_threadSyncClose: M4OSA_mutexClose");

      return err_code;
   }

   err_code = M4OSA_semaphoreClose(threadContext->semStartStop);

   if(M4OSA_ERR_IS_ERROR(err_code))
   {
      M4OSA_DEBUG(err_code, "M4OSA_threadSyncClose: M4OSA_semaphoreClose");

      return err_code;
   }

   if(threadContext->name != M4OSA_NULL)
   {
      free(threadContext->name);
   }

   free(threadContext);

   return M4NO_ERROR;
}




/**
 ************************************************************************
 * @brief      This method asks the thread to return its state.
 * @note       The caller is responsible for allocating/deallocating the state
 *             field.
 * @param      context:(IN) Context of the thread
 * @param      state:(OUT) Thread state
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 ************************************************************************
*/
M4OSA_ERR M4OSA_threadSyncGetState(M4OSA_Context context,
                                   M4OSA_ThreadState* state)
{
   M4OSA_ThreadContext* threadContext = (M4OSA_ThreadContext*)context;

   M4OSA_TRACE1_2("M4OSA_threadSyncGetState\t\tM4OSA_Context 0x%x\t"
                  "M4OSA_ThreadState* 0x%x", context, state);

   M4OSA_DEBUG_IF2(context == M4OSA_NULL,
                   M4ERR_PARAMETER, "M4OSA_threadSyncGetState");

   M4OSA_DEBUG_IF2(state == M4OSA_NULL,
                   M4ERR_PARAMETER, "M4OSA_threadSyncGetState");

   M4OSA_DEBUG_IF2(threadContext->coreID != M4OSA_THREAD,
                   M4ERR_BAD_CONTEXT, "M4OSA_threadSyncGetState");

   *state = threadContext->state;

   return M4NO_ERROR;
}




/**
 ************************************************************************
 * @brief      This method asks the calling thread to sleep during "timeSleep"
 *             milliseconds.
 * @note       This function does not have any context.
 * @param      time:(IN) Time to sleep in milliseconds
 * @return     M4NO_ERROR: there is no error
 ************************************************************************
*/
M4OSA_ERR M4OSA_threadSleep(M4OSA_UInt32 time)
{
   struct timespec rqtp = { 0, 0 };
   struct timespec rmtp = { 0, 0 };

   M4OSA_TRACE1_1("M4OSA_threadSleep\t\tM4OSA_UInt32 %d", time);

   rqtp.tv_sec = (time_t)time/1000;
   rqtp.tv_nsec = (time%1000) * 1000000;
   nanosleep(&rqtp, &rmtp);

   return M4NO_ERROR;
}

#if(M4OSA_OPTIONID_THREAD_PRIORITY == M4OSA_TRUE)

M4OSA_ERR M4OSA_SetThreadSyncPriority(M4OSA_Context context,
                                  M4OSA_DataOption optionValue)
{
   M4OSA_ThreadContext* threadContext = (M4OSA_ThreadContext*)context;
   M4OSA_ThreadPriorityLevel priority
                                 = (M4OSA_ThreadPriorityLevel)(optionValue);

   M4OSA_TRACE2_2("M4OSA_SetThreadSyncPriority\t\tM4OSA_Context 0x%x\t"
                  "M4OSA_DataOption 0x%x", context, optionValue);

   if((M4OSA_UInt32)optionValue>M4OSA_kThreadLowestPriority)
   {
      return M4ERR_PARAMETER;
   }

   threadContext->priority = priority;

   return M4NO_ERROR;
}

#endif /*M4OSA_OPTIONID_THREAD_PRIORITY*/




#if(M4OSA_OPTIONID_THREAD_NAME == M4OSA_TRUE)

M4OSA_ERR M4OSA_SetThreadSyncName(M4OSA_Context context,
                              M4OSA_DataOption optionValue)
{
   M4OSA_ThreadContext* threadContext = (M4OSA_ThreadContext*)context;
   M4OSA_Char* name = (M4OSA_Char*)optionValue;
   M4OSA_UInt32 nameSize ;

   M4OSA_TRACE2_2("M4OSA_SetThreadSyncName\t\tM4OSA_Context 0x%x\t"
                  "M4OSA_DataOption 0x%x", context, optionValue);

   if(threadContext->name != NULL)
   {
      free(threadContext->name);
      threadContext->name = M4OSA_NULL;
   }

   if(optionValue != M4OSA_NULL)
   {
      nameSize = strlen((const char *)name)+1;

      threadContext->name =
         (M4OSA_Char*)M4OSA_32bitAlignedMalloc(nameSize, M4OSA_THREAD,
         (M4OSA_Char*)"M4OSA_SetThreadSyncName: thread name");

      if(threadContext == M4OSA_NULL)
      {
         return M4ERR_ALLOC;
      }

      memcpy((void *)threadContext->name, (void *)name,
                   nameSize);
   }

   return M4NO_ERROR;
}

#endif /*M4OSA_OPTIONID_THREAD_NAME*/


#if(M4OSA_OPTIONID_THREAD_STACK_SIZE == M4OSA_TRUE)

M4OSA_ERR M4OSA_SetThreadSyncStackSize(M4OSA_Context context,
                                   M4OSA_DataOption optionValue)
{
   M4OSA_ThreadContext* threadContext = (M4OSA_ThreadContext*)context;

   M4OSA_TRACE2_2("M4OSA_SetThreadSyncStackSize\t\tM4OSA_Context 0x%x\t"
                  "M4OSA_DataOption 0x%x", context, optionValue);

   threadContext->stackSize = (M4OSA_UInt32)optionValue;

   return M4NO_ERROR;
}

#endif /*M4OSA_OPTIONID_THREAD_STACK_SIZE*/

/**
 ************************************************************************
 * @brief      This method asks the core OSAL-Thread component to set the value
 *             associated with the optionID. The caller is responsible for
 *             allocating/deallocating the memory of the value field.
 * @note       As the caller is responsible of allocating/de-allocating the
 *             "value" field, the callee must copy this field to its internal
 *             variable.
 * @param      context:(IN/OUT) Context of the thread
 * @param      optionID:(IN) ID of the option
 * @param      optionValue:(IN) Value of the option
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 * @return     M4ERR_BAD_OPTION_ID: the optionID is not a valid one
 * @return     M4ERR_STATE: this option is not available now
 * @return     M4ERR_READ_ONLY: this option is a read only one
 * @return     M4ERR_NOT_IMPLEMENTED: this option is not implemented
 ************************************************************************
*/
M4OSA_ERR M4OSA_threadSyncSetOption(M4OSA_Context context,
                                    M4OSA_ThreadOptionID optionID,
                                    M4OSA_DataOption optionValue)
{
   M4OSA_ThreadContext* threadContext = (M4OSA_ThreadContext*)context;
   M4OSA_ERR err_code;

   M4OSA_TRACE1_3("M4OSA_threadSyncSetOption\t\tM4OSA_Context 0x%x\t"
                  "M4OSA_OptionID %d\tM4OSA_DataOption 0x%x",
                  context, optionID, optionValue);

   M4OSA_DEBUG_IF2(context == M4OSA_NULL,
                   M4ERR_PARAMETER, "M4OSA_threadSyncSetOption");

   M4OSA_DEBUG_IF2(optionID == M4OSA_NULL,
                   M4ERR_PARAMETER, "M4OSA_threadSyncSetOption");

   M4OSA_DEBUG_IF2(threadContext->coreID != M4OSA_THREAD,
                   M4ERR_BAD_CONTEXT, "M4OSA_threadSyncSetOption");

   M4OSA_DEBUG_IF2(!M4OSA_OPTION_ID_IS_COREID(optionID, M4OSA_THREAD),
                   M4ERR_BAD_OPTION_ID, "M4OSA_threadSyncSetOption");

   M4OSA_DEBUG_IF2(!M4OSA_OPTION_ID_IS_WRITABLE(optionID),
                   M4ERR_READ_ONLY, "M4OSA_threadSyncSetOption");


   M4OSA_mutexLock(threadContext->stateMutex, M4OSA_WAIT_FOREVER);

   if(threadContext->state != M4OSA_kThreadOpened)
   {
      M4OSA_mutexUnlock(threadContext->stateMutex);

      M4OSA_DEBUG(M4ERR_STATE, "M4OSA_threadSyncSetOption");

      return M4ERR_STATE;
   }

   switch(optionID)
   {

#if(M4OSA_OPTIONID_THREAD_PRIORITY == M4OSA_TRUE)
      case M4OSA_ThreadPriority:
      {
         err_code = M4OSA_SetThreadSyncPriority(context, optionValue);

         break;
      }
#endif /*M4OSA_OPTIONID_THREAD_PRIORITY*/

#if(M4OSA_OPTIONID_THREAD_NAME == M4OSA_TRUE)
      case M4OSA_ThreadName:
      {
         err_code = M4OSA_SetThreadSyncName(context, optionValue);

         break;
      }
#endif /*M4OSA_OPTIONID_THREAD_NAME*/

#if(M4OSA_OPTIONID_THREAD_STACK_SIZE == M4OSA_TRUE)
      case M4OSA_ThreadStackSize:
      {
         err_code = M4OSA_SetThreadSyncStackSize(context, optionValue);

         break;
      }
#endif /*M4OSA_OPTIONID_THREAD_STACK_SIZE*/

      default:
      {
         M4OSA_DEBUG(M4ERR_NOT_IMPLEMENTED, "M4OSA_threadSyncSetOption");

         err_code = M4ERR_NOT_IMPLEMENTED;
      }
   }

   M4OSA_mutexUnlock(threadContext->stateMutex);

   return err_code;
}



/**
 ************************************************************************
 * @brief      This method asks the OSAL-Thread to return the value associated
 *             with the optionID. The caller is responsible for
 *             allocating/deallocating the memory of the value field.
 * @note       "optionValue" must be cast according to the type related to the
 *             optionID.
 * @note       As the caller is responsible for de-allocating the "value"
 *             field, the core OSAL-Thread component must perform a copy of its
 *             internal value to the value field.
 * @param      context:(IN) Context of the thread
 * @param      optionID:(IN) ID of the option
 * @param      optionValue:(OUT) Value of the option
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 * @return     M4ERR_BAD_OPTION_ID: the optionID is not a valid one
 * @return     M4ERR_WRITE_ONLY: this option is a write only one
 * @return     M4ERR_NOT_IMPLEMENTED: this option is not implemented
 ************************************************************************
*/
M4OSA_ERR M4OSA_threadSyncGetOption(M4OSA_Context context,
                                    M4OSA_ThreadOptionID optionID,
                                    M4OSA_DataOption* optionValue)
{
   M4OSA_ThreadContext* threadContext = (M4OSA_ThreadContext*)context;

   M4OSA_TRACE1_3("M4OSA_threadSyncGetOption\t\tM4OSA_Context 0x%x\t"
                  "M4OSA_OptionID %d\tM4OSA_DataOption* 0x%x",
                  context, optionID, optionValue);

   M4OSA_DEBUG_IF2(context == M4OSA_NULL,
                   M4ERR_PARAMETER, "M4OSA_threadSyncGetOption");

   M4OSA_DEBUG_IF2(optionID == M4OSA_NULL,
                   M4ERR_PARAMETER, "M4OSA_threadSyncGetOption");

   M4OSA_DEBUG_IF2(optionValue == M4OSA_NULL,
                   M4ERR_PARAMETER, "M4OSA_threadSyncGetOption");

   M4OSA_DEBUG_IF2(threadContext->coreID != M4OSA_THREAD,
                   M4ERR_BAD_CONTEXT, "M4OSA_threadSyncGetOption");

   M4OSA_DEBUG_IF2(!M4OSA_OPTION_ID_IS_COREID(optionID, M4OSA_THREAD),
                   M4ERR_BAD_OPTION_ID, "M4OSA_threadSyncGetOption");

   M4OSA_DEBUG_IF2(!M4OSA_OPTION_ID_IS_READABLE(optionID),
                   M4ERR_WRITE_ONLY, "M4OSA_threadSyncGetOption");

   switch(optionID)
   {

#if(M4OSA_OPTIONID_THREAD_PRIORITY == M4OSA_TRUE)
      case M4OSA_ThreadPriority:
      {
         M4OSA_ThreadPriorityLevel* priority =
                                    (M4OSA_ThreadPriorityLevel*)optionValue;

         *priority = threadContext->priority;

         return M4NO_ERROR;
      }
#endif /*M4OSA_OPTIONID_THREAD_PRIORITY*/

#if(M4OSA_OPTIONID_THREAD_NAME == M4OSA_TRUE)
      case M4OSA_ThreadName:
      {
         M4OSA_Char** name = (M4OSA_Char**)optionValue;

         *name = threadContext->name;

         return M4NO_ERROR;
      }
#endif /*M4OSA_OPTIONID_THREAD_NAME*/

#if(M4OSA_OPTIONID_THREAD_STACK_SIZE == M4OSA_TRUE)
      case M4OSA_ThreadStackSize:
      {
         M4OSA_UInt32* stackSize = (M4OSA_UInt32*)optionValue;

         *stackSize = threadContext->stackSize;

         return M4NO_ERROR;
      }
#endif /*M4OSA_OPTIONID_THREAD_STACK_SIZE*/

      default:
        break;
   }

   M4OSA_DEBUG(M4ERR_NOT_IMPLEMENTED, "M4OSA_threadSyncGetOption");

   return M4ERR_NOT_IMPLEMENTED;
}

