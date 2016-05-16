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
 * \file phOsalNfc.c
 * \brief OSAL Implementation for linux
 *
 * Project: Trusted NFC Linux Light
 *
 * $Date: 03 aug 2009
 * $Author: Jérémie Corbier
 * $Revision: 1.0
 *
 */

#include <stddef.h>
#include <stdlib.h>
#include <stdio.h>
#include <signal.h>
#include <unistd.h>

#include <phOsalNfc.h>

#ifdef ANDROID
#define LOG_TAG "NFC-HCI"

#include <utils/Log.h>

phOsalNfc_Exception_t phOsalNfc_Exception;
#endif

#ifdef DEBUG
#define MAX_PRINT_BUFSIZE (0x450U)
char phOsalNfc_DbgTraceBuffer[MAX_PRINT_BUFSIZE];
#endif

void phLibNfc_Mgt_Recovery();

/*!
 * \brief Allocates memory.
 *        This function attempts to allocate \a size bytes on the heap and
 *        returns a pointer to the allocated block.
 *
 * \param size size of the memory block to be allocated on the heap.
 *
 * \return pointer to allocated memory block or NULL in case of error.
 */
void *phOsalNfc_GetMemory(uint32_t size)
{
   void *pMem = (void *)malloc(size);
   return pMem;
}

/*!
 * \brief Frees allocated memory block.
 *        This function deallocates memory region pointed to by \a pMem.
 *
 * \param pMem pointer to memory block to be freed.
 */
void phOsalNfc_FreeMemory(void *pMem)
{
   if(NULL !=  pMem)
      free(pMem);
}

void phOsalNfc_DbgString(const char *pString)
{
#ifdef DEBUG
   if(pString != NULL)
#ifndef ANDROID
      printf(pString);
#else
      ALOGD("%s", pString);
#endif
#endif
}

void phOsalNfc_DbgTrace(uint8_t data[], uint32_t size)
{
#ifdef DEBUG
   uint32_t i;
#ifdef ANDROID
   char buf[10];
#endif

   if(size == 0)
      return;

#ifndef ANDROID
   for(i = 0; i < size; i++)
   {
      if((i % 10) == 0)
         printf("\n\t\t\t");
      printf("%02X ", data[i]);
   }
   printf("\n\tBlock size is: %d\n", size);
#else
   phOsalNfc_DbgTraceBuffer[0] = '\0';
   for(i = 0; i < size; i++)
   {
      if((i % 10) == 0)
      {
         ALOGD("%s", phOsalNfc_DbgTraceBuffer);
         phOsalNfc_DbgTraceBuffer[0] = '\0';
      }

      snprintf(buf, 10, "%02X ", data[i]);
      strncat(phOsalNfc_DbgTraceBuffer, buf, 10);
   }
   ALOGD("%s", phOsalNfc_DbgTraceBuffer);
   ALOGD("Block size is: %d", size);
#endif
#endif
}

/*!
 * \brief Raises exception.
 *        This function raises an exception of type \a eExceptionType with
 *        reason \a reason to stack clients.
 *
 * \param eExceptionType exception type.
 * \param reason reason for this exception.
 *
 * \note Clients willing to catch exceptions are to handle the SIGABRT signal.
 *       On Linux, exception type and reason are passed to the signal handler as
 *       a pointer to a phOsalNfc_Exception_t structure.
 *       As sigqueue is not available in Android, exception information are
 *       stored in the phOsalNfc_Exception global.
 */
void phOsalNfc_RaiseException(phOsalNfc_ExceptionType_t eExceptionType, uint16_t reason)
{
    if(eExceptionType == phOsalNfc_e_UnrecovFirmwareErr)
    {
        ALOGE("HCI Timeout - Exception raised - Force restart of NFC service");
        phLibNfc_Mgt_Recovery();
        abort();
    } else {
        ALOGD("phOsalNfc_RaiseException() called");
    }
}

/*!
 * \brief display data bytes.
 *        This function displays data bytes for debug purpose
 * \param[in] pString pointer to string to be displayed.
 * \param[in] length number of bytes to be displayed.
 * \param[in] pBuffer pointer to data bytes to be displayed.
 *
 */
void phOsalNfc_PrintData(const char *pString, uint32_t length, uint8_t *pBuffer,
        int verbosity)
{
    char print_buffer[length * 3 + 1];
    unsigned int i;

    if (pString == NULL) {
        pString = "";
    }
    print_buffer[0] = '\0';
    for (i = 0; i < length; i++) {
        snprintf(&print_buffer[i*3], 4, " %02X", pBuffer[i]);
    }

    char llc[40] = "";

    if (verbosity >= 2) {
        uint8_t llc_header = 0;
        if (!strcmp(pString, "SEND") && length >= 2) {
            llc_header = pBuffer[1];
        } else if (!strcmp(pString, "RECV") && length >= 2) {
            llc_header = pBuffer[0];
        }

        if ((llc_header & 0xC0) == 0x80) {
            // I
            uint8_t ns = (llc_header & 0x38) >> 3;
            uint8_t nr = llc_header & 0x07;
            snprintf(&llc[0], sizeof(llc), "I %d (%d)", ns, nr);
        } else if ((llc_header & 0xE0) == 0xC0) {
            // S
            uint8_t t = (llc_header & 0x18) >> 3;
            uint8_t nr = llc_header & 0x07;
            char *type;
            switch (t) {
            case 0x00: type = "RR "; break;
            case 0x01: type = "REJ"; break;
            case 0x02: type = "RNR"; break;
            case 0x03: type = "SREJ"; break;
            default: type = "???"; break;
            }
            snprintf(&llc[0], sizeof(llc), "S %s (%d)", type, nr);
        } else if ((llc_header & 0xE0) == 0xE0) {
            // U
            snprintf(&llc[0], sizeof(llc), "U");
        } else if (length > 1) {
            snprintf(&llc[0], sizeof(llc), "???");
        }
    }

    ALOGD("> %s:%s\t%s", pString, print_buffer, llc);
}
