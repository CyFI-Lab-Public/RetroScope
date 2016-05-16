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

/**
 * \file phDalNfc_messageQueueLib.c
 * \brief DAL independant message queue implementation for android (can be used under linux too)
 *
 * Project: Trusted NFC Linux Lignt
 *
 * $Date: 13 aug 2009
 * $Author: Jonathan roux
 * $Revision: 1.0 $
 *
 */

#include <pthread.h>
#ifdef ANDROID
#include <linux/ipc.h>
#else
#include <sys/msg.h>
#endif

#include <semaphore.h>

#include <phDal4Nfc.h>
#include <phOsalNfc.h>
#include <phDal4Nfc_DeferredCall.h>
#include <phDal4Nfc_messageQueueLib.h>

typedef struct phDal4Nfc_message_queue_item
{
   phLibNfc_Message_t nMsg;
   struct phDal4Nfc_message_queue_item * pPrev;
   struct phDal4Nfc_message_queue_item * pNext;
} phDal4Nfc_message_queue_item_t;


typedef struct phDal4Nfc_message_queue
{
   phDal4Nfc_message_queue_item_t * pItems;
   pthread_mutex_t          nCriticalSectionMutex;
   sem_t                    nProcessSemaphore;

} phDal4Nfc_message_queue_t;


/**
 * \ingroup grp_nfc_dal
 *
 * \brief DAL message get function
 * This function allocates the message queue. The parameters are ignored, this is
 * just to keep the same api as Linux queue.
 *
 * \retval -1                                   Can not allocate memory or can not init mutex.
*  \retval handle                               The handle on the message queue.
 */
int phDal4Nfc_msgget ( key_t key, int msgflg )
{
   phDal4Nfc_message_queue_t * pQueue;
   pQueue = (phDal4Nfc_message_queue_t *) phOsalNfc_GetMemory(sizeof(phDal4Nfc_message_queue_t));
   if (pQueue == NULL)
      return -1;
   memset(pQueue, 0, sizeof(phDal4Nfc_message_queue_t));
   if (pthread_mutex_init (&pQueue->nCriticalSectionMutex, NULL) == -1)
      return -1;
   if (sem_init (&pQueue->nProcessSemaphore, 0, 0) == -1)
      return -1;
   return ((int)pQueue);
}

/**
 * \ingroup grp_nfc_dal
 *
 * \brief DAL message control function
 * This function destroys the message queue. The cmd and buf parameters are ignored,
 * this is just to keep the same api as Linux queue.
 *
 * \param[in]       msqid     The handle of the message queue.
 *
 * \retval 0                                    If success.
 * \retval -1                                   Bad passed parameter
 */
int phDal4Nfc_msgctl ( int msqid, int cmd, void *buf )
{
   phDal4Nfc_message_queue_t * pQueue;
   phDal4Nfc_message_queue_item_t * p;

   if (msqid == 0)
      return -1;

   pQueue = (phDal4Nfc_message_queue_t *)msqid;
   pthread_mutex_lock(&pQueue->nCriticalSectionMutex);
   if (pQueue->pItems != NULL)
   {
      p = pQueue->pItems;
      while(p->pNext != NULL) { p = p->pNext; }
      while(p->pPrev != NULL)
      {
         p = p->pPrev;
         phOsalNfc_FreeMemory(p->pNext);
         p->pNext = NULL;
      }
      phOsalNfc_FreeMemory(p);
   }
   pQueue->pItems = NULL;
   pthread_mutex_unlock(&pQueue->nCriticalSectionMutex);
   pthread_mutex_destroy(&pQueue->nCriticalSectionMutex);
   phOsalNfc_FreeMemory(pQueue);
   return 0;
}

/**
 * \ingroup grp_nfc_dal
 *
 * \brief DAL message send function
 * Use this function to send a message to the queue. The message will be added at the end of
 * the queue with respect to FIFO policy. The msgflg parameter is ignored.
 *
 * \param[in]       msqid     The handle of the message queue.
 * \param[in]       msgp      The message to send.
 * \param[in]       msgsz     The message size.
 *
 * \retval 0                                    If success.
 * \retval -1                                   Bad passed parameter, or can not allocate memory
 */
int phDal4Nfc_msgsnd (int msqid, void * msgp, size_t msgsz, int msgflg)
{
   phDal4Nfc_message_queue_t * pQueue;
   phDal4Nfc_message_queue_item_t * p;
   phDal4Nfc_message_queue_item_t * pNew;

   if ((msqid == 0) || (msgp == NULL) || (msgsz == 0))
      return -1;

   if (msgsz != sizeof(phLibNfc_Message_t))
      return -1;

   pQueue = (phDal4Nfc_message_queue_t *)msqid;
   pNew = (phDal4Nfc_message_queue_item_t *)phOsalNfc_GetMemory(sizeof(phDal4Nfc_message_queue_item_t));
   if (pNew == NULL)
      return -1;
   memset(pNew, 0, sizeof(phDal4Nfc_message_queue_item_t));
   memcpy(&pNew->nMsg, &((phDal4Nfc_Message_Wrapper_t*)msgp)->msg, sizeof(phLibNfc_Message_t));
   pthread_mutex_lock(&pQueue->nCriticalSectionMutex);
   if (pQueue->pItems != NULL)
   {
      p = pQueue->pItems;
      while(p->pNext != NULL) { p = p->pNext; }
      p->pNext = pNew;
      pNew->pPrev = p;
   }
   else
   {
      pQueue->pItems = pNew;
   }
   pthread_mutex_unlock(&pQueue->nCriticalSectionMutex);

   sem_post(&pQueue->nProcessSemaphore);
   return 0;
}

/**
 * \ingroup grp_nfc_dal
 *
 * \brief DAL message receive function
 * The call to this function will get the older message from the queue. If the queue is empty the function waits
 * (blocks on a mutex) until a message is posted to the queue with phDal4Nfc_msgsnd.
 * The msgtyp and msgflg parameters are ignored.
 *
 * \param[in]       msqid     The handle of the message queue.
 * \param[out]      msgp      The received message.
 * \param[in]       msgsz     The message size.
 *
 * \retval 0                                    If success.
 * \retval -1                                   Bad passed parameter.
 */
int phDal4Nfc_msgrcv (int msqid, void * msgp, size_t msgsz, long msgtyp, int msgflg)
{
   phDal4Nfc_message_queue_t * pQueue;
   phDal4Nfc_message_queue_item_t * p;

   if ((msqid == 0) || (msgp == NULL))
      return -1;

   if (msgsz != sizeof(phLibNfc_Message_t))
      return -1;

   pQueue = (phDal4Nfc_message_queue_t *)msqid;
   sem_wait(&pQueue->nProcessSemaphore);
   pthread_mutex_lock(&pQueue->nCriticalSectionMutex);
   if (pQueue->pItems != NULL)
   {
      memcpy(&((phDal4Nfc_Message_Wrapper_t*)msgp)->msg, &(pQueue->pItems)->nMsg, sizeof(phLibNfc_Message_t));
      p = pQueue->pItems->pNext;
      phOsalNfc_FreeMemory(pQueue->pItems);
      pQueue->pItems = p;
   }
   pthread_mutex_unlock(&pQueue->nCriticalSectionMutex);
   return 0;
}





