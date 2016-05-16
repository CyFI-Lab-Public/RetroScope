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
 * @file         M4OSA_Thread_priv.h
 * @ingroup      OSAL
 * @brief        Thread private for Android
 * @note
 ************************************************************************
*/

#ifndef M4OSA_THREAD_PRIV_H
#define M4OSA_THREAD_PRIV_H


#include "M4OSA_Types.h"


/* Context for the thread */
typedef struct M4OSA_ThreadContext {
   M4OSA_UInt32 coreID;                /* thread context identifiant */
   pthread_t threadID;                 /* thread identifier. */
   M4OSA_Char* name;                   /* thread name */
   M4OSA_UInt32 stackSize;             /* thread stackSize in bytes */
   M4OSA_ThreadDoIt func;              /* thread function */
   M4OSA_Void* param;                  /* thread parameter */
/*
   M4OSA_Void* userData;               / * thread user data * /
*/
   M4OSA_ThreadState state;            /* thread automaton state */
   M4OSA_Context stateMutex;           /* mutex for thread state management */
/*
   M4OSA_ThreadCallBack startCallBack; / * starting thread call back * /
   M4OSA_ThreadCallBack stopCallBack;  / * stopping thread call back * /
*/
   M4OSA_Context semStartStop;         /* semaphore for start and stop do_it */
   M4OSA_ThreadPriorityLevel priority; /* thread priority level */
} M4OSA_ThreadContext ;


/** Those define enable/disable option ID*/
#define M4OSA_OPTIONID_THREAD_STARTED           M4OSA_TRUE
#define M4OSA_OPTIONID_THREAD_STOPPED           M4OSA_TRUE
#define M4OSA_OPTIONID_THREAD_PRIORITY          M4OSA_TRUE
#define M4OSA_OPTIONID_THREAD_STACK_SIZE        M4OSA_TRUE
#define M4OSA_OPTIONID_THREAD_NAME              M4OSA_TRUE
#define M4OSA_OPTIONID_THREAD_USER_DATA         M4OSA_TRUE

#endif /*M4OSA_THREAD_PRIV_H*/

