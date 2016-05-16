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
 * @brief        Mutex for Android
 * @note         This file implements functions to manipulate mutex
 ************************************************************************
*/

#include "M4OSA_Debug.h"
#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4OSA_Memory.h"
#include "M4OSA_Mutex.h"

#include <pthread.h>
#include <errno.h>


/* Context for the mutex */
typedef struct
{
   M4OSA_UInt32     coreID;               /* mutex context identifiant */
   pthread_mutex_t  mutex;                /* mutex */
   pthread_t        threadOwnerID;        /* thread owner identifiant */
} M4OSA_MutexContext;



/**
 ************************************************************************
 * @brief      This method creates a new mutex.
 * @note       This function creates and allocates a unique context. It's the
 *             OSAL real time responsibility for managing its context. It must
 *             be freed by the M4OSA_mutexClose function. The context parameter
 *             will be sent back to any OSAL core mutex functions to allow
 *             retrieving data associated to the opened mutex.
 * @param      pContext:(OUT) Context of the created mutex
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_ALLOC: there is no more available memory
 * @return     M4ERR_CONTEXT_FAILED: the context creation failed
 ************************************************************************
*/
M4OSA_ERR M4OSA_mutexOpen(M4OSA_Context* pContext)
{
    M4OSA_MutexContext* pMutexContext = (M4OSA_MutexContext*)M4OSA_NULL;
    pthread_mutexattr_t attribute = { 0 };
    M4OSA_Bool opened = M4OSA_FALSE;

    M4OSA_TRACE1_1("M4OSA_mutexOpen\t\tM4OSA_Context* 0x%x", pContext);
    M4OSA_DEBUG_IF2(M4OSA_NULL == pContext, M4ERR_PARAMETER,
                                     "M4OSA_mutexOpen: pContext is M4OSA_NULL");

    *pContext = M4OSA_NULL;

    pMutexContext = (M4OSA_MutexContext*)M4OSA_32bitAlignedMalloc(sizeof(M4OSA_MutexContext),
                    M4OSA_MUTEX, (M4OSA_Char*)"M4OSA_mutexOpen: mutex context");

    if(M4OSA_NULL == pMutexContext)
    {
        M4OSA_DEBUG(M4ERR_ALLOC, "M4OSA_mutexOpen");
        return M4ERR_ALLOC;
    }

    /* Initialize the mutex attribute. */
    if ( 0 == pthread_mutexattr_init( &attribute ) )
    {
        /* Initialize the mutex type. */
        if ( 0 == pthread_mutexattr_settype( &attribute, PTHREAD_MUTEX_RECURSIVE ) )
        {
            /* Initialize the mutex. */
            if (0 == pthread_mutex_init( &pMutexContext->mutex, &attribute ) )
            {
                opened = M4OSA_TRUE;
            }
        }

        /* Destroy the mutex attribute. */
        pthread_mutexattr_destroy( &attribute );
    }

    if(!opened)
    {
        M4OSA_DEBUG(M4ERR_CONTEXT_FAILED, "M4OSA_mutexOpen: OS mutex creation failed");
        free(pMutexContext);
        return M4ERR_CONTEXT_FAILED ;
    }

    pMutexContext->coreID = M4OSA_MUTEX;

    pMutexContext->threadOwnerID = 0;

    *pContext = (M4OSA_Context) pMutexContext;

    return M4NO_ERROR;
}




/**
 ************************************************************************
 * @brief      This method locks the mutex. "Context" identifies the mutex.
 * @note       If the mutex is already locked, the calling thread blocks until
 *             the mutex becomes available (by calling M4OSA_mutexUnlock) or
 *             "timeout" is reached. This is a blocking call.
 * @param      context:(IN/OUT) Context of the mutex
 * @param      timeout:(IN) Time out in milliseconds
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4WAR_TIME_OUT: time out is elapsed before mutex has been
 *             available
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 ************************************************************************
*/
M4OSA_ERR M4OSA_mutexLock(M4OSA_Context context, M4OSA_UInt32 timeout)
{
    M4OSA_MutexContext* pMutexContext = (M4OSA_MutexContext*)context;
    pthread_t           currentThread;
    int                 result;
    struct timespec     ts;
    struct timespec     left;

    M4OSA_TRACE1_2("M4OSA_mutexLock\t\tM4OSA_Context 0x%x\tM4OSA_UInt32 %d",
        context, timeout);

    M4OSA_DEBUG_IF2(M4OSA_NULL == context, M4ERR_PARAMETER,
                                      "M4OSA_mutexLock: context is M4OSA_NULL");
    M4OSA_DEBUG_IF2(pMutexContext->coreID != M4OSA_MUTEX,
                                          M4ERR_BAD_CONTEXT, "M4OSA_mutexLock");

    currentThread = pthread_self();

    if(pMutexContext ->threadOwnerID == currentThread)
    {
        M4OSA_DEBUG(M4ERR_BAD_CONTEXT, "M4OSA_mutexLock: Thread tried to lock a mutex it already owns");
        return M4ERR_BAD_CONTEXT ;
    }

    /* Lock the mutex. */
    if ( M4OSA_WAIT_FOREVER == timeout)
    {
        if ( 0 != pthread_mutex_lock(&pMutexContext->mutex) )
        {
            M4OSA_DEBUG(M4ERR_BAD_CONTEXT, "M4OSA_mutexLock: OS mutex wait failed");
            return M4ERR_BAD_CONTEXT;
        }
    }
    else
    {
        result = pthread_mutex_trylock(&pMutexContext->mutex);
        while ( ( EBUSY == result ) && ( 0 < timeout ) )
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
            result = pthread_mutex_trylock(&pMutexContext->mutex);
        }
        if (0 != result)
        {
            if (EBUSY == result)
            {
                return M4WAR_TIME_OUT;
            }
            else
            {
                M4OSA_DEBUG(M4ERR_BAD_CONTEXT, "M4OSA_mutexLock: OS mutex wait failed");
                return M4ERR_BAD_CONTEXT;
            }
        }
    }

    pMutexContext->threadOwnerID = currentThread;

    return M4NO_ERROR;
}



/**
 ************************************************************************
 * @brief      This method unlocks the mutex. The mutex is identified by
 *             its context
 * @note       The M4OSA_mutexLock unblocks the thread with the highest
 *             priority and made it ready to run.
 * @note       No hypotheses can be made on which thread will be un-blocked
 *             between threads with the same priority.
 * @param      context:(IN/OUT) Context of the mutex
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
************************************************************************
*/
M4OSA_ERR M4OSA_mutexUnlock(M4OSA_Context context)
{
    M4OSA_MutexContext* pMutexContext = (M4OSA_MutexContext*)context;
    pthread_t currentThread;

    M4OSA_TRACE1_1("M4OSA_mutexUnlock\t\tM4OSA_Context 0x%x", context);
    M4OSA_DEBUG_IF2(M4OSA_NULL == context, M4ERR_PARAMETER,
                                    "M4OSA_mutexUnlock: context is M4OSA_NULL");
    M4OSA_DEBUG_IF2(M4OSA_MUTEX != pMutexContext->coreID,
                                        M4ERR_BAD_CONTEXT, "M4OSA_mutexUnlock");

    currentThread = pthread_self();

    if(pMutexContext->threadOwnerID != currentThread)
    {
        M4OSA_DEBUG(M4ERR_BAD_CONTEXT, "M4OSA_mutexUnlock: Thread tried to unlock a mutex it doesn't own");
        return M4ERR_BAD_CONTEXT;
    }

    pMutexContext->threadOwnerID = 0 ;

    pthread_mutex_unlock(&pMutexContext->mutex);

    return M4NO_ERROR;
}




/**
 ************************************************************************
 * @brief      This method deletes a mutex (identify by its context). After
 *             this call, the mutex and its context is no more useable. This
 *             function frees all the memory related to this mutex.
 * @note       It is an application issue to warrant no more threads are locked
 *             on the deleted mutex.
 * @param      context:(IN/OUT) Context of the mutex
 * @return     M4NO_ERROR: there is no error
 * @return     M4ERR_PARAMETER: at least one parameter is NULL
 * @return     M4ERR_BAD_CONTEXT: provided context is not a valid one
 ************************************************************************
*/
M4OSA_ERR M4OSA_mutexClose(M4OSA_Context context)
{
    M4OSA_MutexContext* pMutexContext = (M4OSA_MutexContext*)context;

    M4OSA_TRACE1_1("M4OSA_mutexClose\t\tM4OSA_Context 0x%x", context);

    M4OSA_DEBUG_IF2(M4OSA_NULL == context, M4ERR_PARAMETER,
                                     "M4OSA_mutexClose: context is M4OSA_NULL");
    M4OSA_DEBUG_IF2(pMutexContext->coreID != M4OSA_MUTEX,
                                        M4ERR_BAD_CONTEXT, "M4OSA_mutexUnlock");

    pthread_mutex_destroy(&pMutexContext->mutex);

    free( pMutexContext);

    return M4NO_ERROR;
}

