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
 * \file phDalNfc_messageQueueLib.h
 * \brief DAL independant message queue implementation for android (can be used under linux too)
 *
 * Project: Trusted NFC Linux Lignt
 *
 * $Date: 13 aug 2009
 * $Author: Jonathan roux
 * $Revision: 1.0 $
 *
 */
#ifndef PHDAL4NFC_MESSAGEQUEUE_H
#define PHDAL4NFC_MESSAGEQUEUE_H

#ifndef WIN32
#ifdef ANDROID
#include <linux/ipc.h>
#else
#include <sys/msg.h>
#endif

typedef struct phDal4Nfc_Message_Wrapper
{
   long mtype;
   phLibNfc_Message_t msg;
} phDal4Nfc_Message_Wrapper_t;

int phDal4Nfc_msgget(key_t key, int msgflg);
int phDal4Nfc_msgctl(int msqid, int cmd, void *buf);
int phDal4Nfc_msgsnd(int msqid, void * msgp, size_t msgsz, int msgflg);
int phDal4Nfc_msgrcv (int msqid, void * msgp, size_t msgsz, long msgtyp, int msgflg);
#endif

#endif /*  PHDAL4NFC_MESSAGEQUEUE_H  */
