/*
 * Copyright (C) 2010 NXP Semiconductors
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

/*!
 * \file  phOsalNfc_Common.h
 *
 * Project: NFC FRI / OSAL
 *
 * $Workfile:: phOsalNfc_Common.h                $ 
 * $Modtime::                                    $ 
 * $Author: frq09147 $
 * $Revision: 1.1 $
 *
 */

#ifndef PHOSALNFC_COMMON_H
#define PHOSALNFC_COMMON_H
#include <time.h>
#include <unistd.h>
#include <errno.h>
#include <pthread.h>
#include <semaphore.h> 
#include <signal.h> 
#include <sys/time.h>
#include <sys/sem.h>

#define MAX_MESSAGE_SIZE 256

typedef enum phOsalNfc_eHandleType
{
     phOsalNfc_eHandleTypeInvalid   = 0,
     phOsalNfc_eHandleTypeThread    = 1,
     phOsalNfc_eHandleTypeSemaphore = 2,

}phOsalNfc_eHandleType_t;

typedef struct phOsalNfc_sMsg
{
     phOsalNfc_Message_t Msg;
     uint32_t sourceID; /* pthread_t = unsigned long int */
     struct phOsalNfc_sMsg *nextMsg;
} phOsalNfc_sMsg_t;

typedef struct phOsalNfc_sOsalHandle
{
     phOsalNfc_eHandleType_t       HandleType; 
     pthread_t 	                  *pThread;
     pphOsalNfc_ThreadFunction_t   pThreadFunction;
     void                         *pParams;
     
     sem_t                        *pSemaphore;
     sem_t                         handleSem;
     int32_t                       semValue; 
     uint32_t                      semMax; 
     
     struct sembuf                 semBuf;
     int32_t                       semId;
            
     phOsalNfc_sMsg_t             *pMsg;
     sem_t                         msgSem;
     struct phOsalNfc_sOsalHandle *nextThread;
} phOsalNfc_sOsalHandle_t;


#endif /* PHOSALNFC_COMMON_H */
