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
 * @file         M4OSA_Semaphore.c
 * @brief        Semaphore for Windows
 * @note         This file implements functions to manipulate semaphore
 ************************************************************************
*/



#include "M4OSA_Debug.h"
#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4OSA_Memory.h"
#include "M4OSA_Semaphore.h"

#include <semaphore.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <time.h>


/* Context for the semaphore */
typedef struct {
   M4OSA_UInt32   coreID;     /* semaphore context identifiant */
   sem_t          semaphore;  /* semaphore */
} M4OSA_SemaphoreContext;




/**
 ************************************************************************
 * @brief      This method creates a new semaphore with the "initialCounter"
 *             value.
 * @note       This function creates and allocates a unique context. It's the
 *             OSAL real time responsibility for managing its context. It must
 *             be freed by the M4OSA_semaphoreClose function. The context
 *             parameter will be sent back to any OSAL core semaphore functions
 *             to allow retrieving data associated to the opened semaphore.
 * @param      context:(OUT) Context of the created semaphore
 * @param      initial_count:(IN) Initial counter of the semaphore
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: provided context is NULL
 * @return     M4ERR_ALLOC: there is no more available memory
 * @return     M4ERR_CONTEXT_FAILED: the context creation failed
 ************************************************************************
*/
M4OSA_ERR M4OSA_semaphoreOpen(M4OSA_Context* context,
                              M4OSA_UInt32 initial_count)
{
   M4OSA_SemaphoreContext* semaphoreContext = M4OSA_NULL;

   M4OSA_TRACE1_2("M4OSA_semaphoreOpen\t\tM4OSA_Context* 0x%x\tM4OSA_UInt32 "
                  "%d", context, initial_count);

   M4OSA_DEBUG_IF2(context == M4OSA_NULL,
                   M4ERR_PARAMETER, "M4OSA_semaphoreOpen");

   *context = M4OSA_NULL;

   semaphoreContext = (M4OSA_SemaphoreContext*) M4OSA_32bitAlignedMalloc(
                      sizeof(M4OSA_SemaphoreContext), M4OSA_SEMAPHORE,
                      (M4OSA_Char*)"M4OSA_semaphoreOpen: semaphore context");

   if(semaphoreContext == M4OSA_NULL)
   {
      M4OSA_DEBUG(M4ERR_ALLOC, "M4OSA_semaphoreOpen");

      return M4ERR_ALLOC;
   }

   if (0 != sem_init(&semaphoreContext->semaphore, 0, initial_count))
   {
      free(semaphoreContext);

      M4OSA_DEBUG(M4ERR_CONTEXT_FAILED,
         "M4OSA_semaphoreOpen: OS semaphore creation failed");

      return M4ERR_CONTEXT_FAILED;
   }

   semaphoreContext->coreID = M4OSA_SEMAPHORE ;
   *context = (M4OSA_Context)semaphoreContext;

   return M4NO_ERROR;
}




/**
 ************************************************************************
 * @brief      This method decrements (one by one) the semaphore counter. The
 *             semaphore is identified by its context This call is not blocking
 *             if the semaphore counter is positive or zero (after
 *             decrementation). This call is blocking if the semaphore counter
 *             is less than zero (after decrementation), until the semaphore is
 *             upper than zero (see M4OSA_semaphorePost) or time_out is
 *             reached.
 * @note       If "timeout" value is M4OSA_WAIT_FOREVER, the calling thread
 *             will block indefinitely until the semaphore  is unlocked.
 * @param      context:(IN/OUT) Context of the semaphore
 * @param      timeout:(IN) Time out in milliseconds
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4WAR_TIME_OUT: time out is elapsed before semaphore has been
 *             available.
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 ************************************************************************
*/
M4OSA_ERR M4OSA_semaphoreWait(M4OSA_Context context, M4OSA_Int32 timeout)
{
   M4OSA_SemaphoreContext* semaphoreContext = (M4OSA_SemaphoreContext*)context;
   struct timespec         ts;
   struct timespec         left;
   int                     result;

   M4OSA_TRACE1_2("M4OSA_semaphoreWait\t\tM4OSA_Context 0x%x\tM4OSA_UInt32 %d",
                  context, timeout);

   M4OSA_DEBUG_IF2(context == M4OSA_NULL,
                   M4ERR_PARAMETER, "M4OSA_semaphoreWait");

   M4OSA_DEBUG_IF2(semaphoreContext->coreID != M4OSA_SEMAPHORE,
                   M4ERR_BAD_CONTEXT, "M4OSA_semaphoreWait");

   if ( (M4OSA_Int32)M4OSA_WAIT_FOREVER == timeout)
   {
       if ( 0 != sem_wait(&semaphoreContext->semaphore) )
       {
           M4OSA_DEBUG(M4ERR_BAD_CONTEXT,
                  "M4OSA_semaphoreWait: OS semaphore wait failed");

           return M4ERR_BAD_CONTEXT ;
       }
   }
   else
   {
       result = sem_trywait(&semaphoreContext->semaphore);
       while ( ((EBUSY == result) || (EAGAIN == result)) && ( 0 < timeout ) )
       {
           ts.tv_sec  = 0;
           if (1 <= timeout)
           {
               ts.tv_nsec = 1000000;
               timeout -= 1;
           }
           else
           {
               ts.tv_nsec = timeout * 1000000;
               timeout = 0;
           }
           nanosleep(&ts, &left);
           result = sem_trywait(&semaphoreContext->semaphore);
       }
       if (0 != result)
       {
           if ((EBUSY == result) || (EAGAIN == result))
           {
               return M4WAR_TIME_OUT;
           }
           else
           {
               M4OSA_DEBUG(M4ERR_BAD_CONTEXT, "M4OSA_semaphoreWait: OS semaphore wait failed");
               return M4ERR_BAD_CONTEXT;
           }
       }
   }

   return M4NO_ERROR;
}





/**
 ************************************************************************
 * @brief      This method increments the semaphore counter. The semaphore is
 *             identified by its context
 * @note       If the semaphore counter is upper than zero (after addition),
 *             the M4OSA_semaphoreWait call of the thread with the highest
 *             priority is unblocked and made ready to run.
 * @note       No hypotheses can be made on which thread will be unblocked
 *             between threads with the same priority.
 * @param      context:(IN/OUT) Context of the semaphore
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
************************************************************************
*/
M4OSA_ERR M4OSA_semaphorePost(M4OSA_Context context)
{
   M4OSA_SemaphoreContext* semaphoreContext = (M4OSA_SemaphoreContext*)context;

   M4OSA_TRACE1_1("M4OSA_semaphorePost\t\tM4OSA_Context 0x%x", context);

   M4OSA_DEBUG_IF2(context == M4OSA_NULL,
                   M4ERR_PARAMETER, "M4OSA_semaphorePost");

   M4OSA_DEBUG_IF2(semaphoreContext->coreID != M4OSA_SEMAPHORE,
                   M4ERR_BAD_CONTEXT, "M4OSA_semaphorePost");

   sem_post(&semaphoreContext->semaphore);

   return M4NO_ERROR;
}





/**
 ************************************************************************
 * @brief      This method deletes a semaphore (identify by its context).
 *             After this call the semaphore and its context is no more
 *             useable. This function frees all the memory related to this
 *             semaphore.
 * @note       It is an application issue to warrant no more threads are locked
 *             on the deleted semaphore.
 * @param      context:(IN/OUT) Context of the semaphore
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one.
************************************************************************
*/
M4OSA_ERR M4OSA_semaphoreClose(M4OSA_Context context)
{
   M4OSA_SemaphoreContext* semaphoreContext = (M4OSA_SemaphoreContext*)context;

   M4OSA_TRACE1_1("M4OSA_semaphoreClose\t\tM4OSA_Context 0x%x", context);

   M4OSA_DEBUG_IF2(context == M4OSA_NULL,
                   M4ERR_PARAMETER, "M4OSA_semaphoreClose");

   M4OSA_DEBUG_IF2(semaphoreContext->coreID != M4OSA_SEMAPHORE,
                   M4ERR_BAD_CONTEXT, "M4OSA_semaphoreClose");

   sem_destroy(&semaphoreContext->semaphore);

   free(semaphoreContext);

   return M4NO_ERROR;
}

