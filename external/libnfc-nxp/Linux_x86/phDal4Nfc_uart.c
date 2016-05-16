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
 * \file phDalNfc_uart.c
 * \brief DAL com port implementation for linux
 *
 * Project: Trusted NFC Linux Lignt
 *
 * $Date: 07 aug 2009
 * $Author: Jonathan roux
 * $Revision: 1.0 $
 *
 */

#define LOG_TAG "NFC_uart"
#include <cutils/log.h>
#include <hardware/nfc.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <termios.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <sys/select.h>
#include <stdio.h>
#include <errno.h>

#include <phDal4Nfc_debug.h>
#include <phDal4Nfc_uart.h>
#include <phOsalNfc.h>
#include <phNfcStatus.h>
#if defined(ANDROID)
#include <string.h>
#include <cutils/properties.h> // for property_get
#endif

typedef struct
{
   int  nHandle;
   char nOpened;
   struct termios nIoConfigBackup;
   struct termios nIoConfig;

} phDal4Nfc_ComPortContext_t;

/*-----------------------------------------------------------------------------------
                                COM PORT CONFIGURATION
------------------------------------------------------------------------------------*/
#define DAL_BAUD_RATE  B115200



/*-----------------------------------------------------------------------------------
                                      VARIABLES
------------------------------------------------------------------------------------*/
static phDal4Nfc_ComPortContext_t gComPortContext;



/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_uart_set_open_from_handle

PURPOSE:  Initialize internal variables

-----------------------------------------------------------------------------*/

void phDal4Nfc_uart_initialize(void)
{
   memset(&gComPortContext, 0, sizeof(phDal4Nfc_ComPortContext_t));
}


/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_uart_set_open_from_handle

PURPOSE:  The application could have opened the link itself. So we just need
          to get the handle and consider that the open operation has already
          been done.

-----------------------------------------------------------------------------*/

void phDal4Nfc_uart_set_open_from_handle(phHal_sHwReference_t * pDalHwContext)
{
   gComPortContext.nHandle = (int) pDalHwContext->p_board_driver;
   DAL_ASSERT_STR(gComPortContext.nHandle >= 0, "Bad passed com port handle");
   gComPortContext.nOpened = 1;
}

/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_uart_is_opened

PURPOSE:  Returns if the link is opened or not. (0 = not opened; 1 = opened)

-----------------------------------------------------------------------------*/

int phDal4Nfc_uart_is_opened(void)
{
   return gComPortContext.nOpened;
}

/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_uart_flush

PURPOSE:  Flushes the link ; clears the link buffers

-----------------------------------------------------------------------------*/

void phDal4Nfc_uart_flush(void)
{
   int ret;
   /* flushes the com port */
   ret = tcflush(gComPortContext.nHandle, TCIFLUSH);
   DAL_ASSERT_STR(ret!=-1, "tcflush failed");
}

/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_uart_close

PURPOSE:  Closes the link

-----------------------------------------------------------------------------*/

void phDal4Nfc_uart_close(void)
{
   if (gComPortContext.nOpened == 1)
   {
      close(gComPortContext.nHandle);
      gComPortContext.nHandle = 0;
      gComPortContext.nOpened = 0;
   }
}

/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_uart_close

PURPOSE:  Closes the link

-----------------------------------------------------------------------------*/

NFCSTATUS phDal4Nfc_uart_open_and_configure(pphDal4Nfc_sConfig_t pConfig, void ** pLinkHandle)
{
   int          nComStatus;
   NFCSTATUS    nfcret = NFCSTATUS_SUCCESS;
   int          ret;

   DAL_ASSERT_STR(gComPortContext.nOpened==0, "Trying to open but already done!");

   srand(time(NULL));

   /* open communication port handle */
   gComPortContext.nHandle = open(pConfig->deviceNode, O_RDWR | O_NOCTTY);
   if (gComPortContext.nHandle < 0)
   {
      *pLinkHandle = NULL;
      return PHNFCSTVAL(CID_NFC_DAL, NFCSTATUS_INVALID_DEVICE);
   }

   gComPortContext.nOpened = 1;
   *pLinkHandle = (void*)gComPortContext.nHandle;

   /*
    *  Now configure the com port
    */
   ret = tcgetattr(gComPortContext.nHandle, &gComPortContext.nIoConfigBackup); /* save the old io config */
   if (ret == -1)
   {
      /* tcgetattr failed -- it is likely that the provided port is invalid */
      *pLinkHandle = NULL;
      return PHNFCSTVAL(CID_NFC_DAL, NFCSTATUS_INVALID_DEVICE);
   }
   ret = fcntl(gComPortContext.nHandle, F_SETFL, 0); /* Makes the read blocking (default).  */
   DAL_ASSERT_STR(ret != -1, "fcntl failed");
   /* Configures the io */
   memset((void *)&gComPortContext.nIoConfig, (int)0, (size_t)sizeof(struct termios));
   /*
    BAUDRATE: Set bps rate. You could also use cfsetispeed and cfsetospeed.
    CRTSCTS : output hardware flow control (only used if the cable has
              all necessary lines. See sect. 7 of Serial-HOWTO)
    CS8     : 8n1 (8bit,no parity,1 stopbit)
    CLOCAL  : local connection, no modem contol
    CREAD   : enable receiving characters
   */
   gComPortContext.nIoConfig.c_cflag = DAL_BAUD_RATE | CS8 | CLOCAL | CREAD;  /* Control mode flags */
   gComPortContext.nIoConfig.c_iflag = IGNPAR;                                          /* Input   mode flags : IGNPAR  Ignore parity errors */
   gComPortContext.nIoConfig.c_oflag = 0;                                               /* Output  mode flags */
   gComPortContext.nIoConfig.c_lflag = 0;                                               /* Local   mode flags. Read mode : non canonical, no echo */
   gComPortContext.nIoConfig.c_cc[VTIME] = 0;                                           /* Control characters. No inter-character timer */
   gComPortContext.nIoConfig.c_cc[VMIN]  = 1;                                           /* Control characters. Read is blocking until X characters are read */

   /*
      TCSANOW  Make changes now without waiting for data to complete
      TCSADRAIN   Wait until everything has been transmitted
      TCSAFLUSH   Flush input and output buffers and make the change
   */
   ret = tcsetattr(gComPortContext.nHandle, TCSANOW, &gComPortContext.nIoConfig);
   DAL_ASSERT_STR(ret != -1, "tcsetattr failed");

   /*
      On linux the DTR signal is set by default. That causes a problem for pn544 chip
      because this signal is connected to "reset". So we clear it. (on windows it is cleared by default).
   */
   ret = ioctl(gComPortContext.nHandle, TIOCMGET, &nComStatus);
   DAL_ASSERT_STR(ret != -1, "ioctl TIOCMGET failed");
   nComStatus &= ~TIOCM_DTR;
   ret = ioctl(gComPortContext.nHandle, TIOCMSET, &nComStatus);
   DAL_ASSERT_STR(ret != -1, "ioctl TIOCMSET failed");
   DAL_DEBUG("Com port status=%d\n", nComStatus);
   usleep(10000); /* Mandatory sleep so that the DTR line is ready before continuing */

   return nfcret;
}

/*
  adb shell setprop debug.nfc.UART_ERROR_RATE X
  will corrupt and drop bytes in uart_read(), to test the error handling
  of DAL & LLC errors.
 */
int property_error_rate = 0;
static void read_property() {
    char value[PROPERTY_VALUE_MAX];
    property_get("debug.nfc.UART_ERROR_RATE", value, "0");
    property_error_rate = atoi(value);
}

/* returns length of buffer after errors */
static int apply_errors(uint8_t *buffer, int length) {
    int i;
    if (!property_error_rate) return length;

    for (i = 0; i < length; i++) {
        if (rand() % 1000 < property_error_rate) {
            if (rand() % 2) {
                // 50% chance of dropping byte
                length--;
                memcpy(&buffer[i], &buffer[i+1], length-i);
                ALOGW("dropped byte %d", i);
            } else {
                // 50% chance of corruption
                buffer[i] = (uint8_t)rand();
                ALOGW("corrupted byte %d", i);
            }
        }
    }
    return length;
}

static struct timeval timeval_remaining(struct timespec timeout) {
    struct timespec now;
    struct timeval delta;
    clock_gettime(CLOCK_MONOTONIC, &now);

    delta.tv_sec = timeout.tv_sec - now.tv_sec;
    delta.tv_usec = (timeout.tv_nsec - now.tv_nsec) / (long)1000;

    if (delta.tv_usec < 0) {
        delta.tv_usec += 1000000;
        delta.tv_sec--;
    }
    if (delta.tv_sec < 0) {
        delta.tv_sec = 0;
        delta.tv_usec = 0;
    }
    return delta;
}

static int libnfc_firmware_mode = 0;

/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_uart_read

PURPOSE:  Reads nNbBytesToRead bytes and writes them in pBuffer.
          Returns the number of bytes really read or -1 in case of error.

-----------------------------------------------------------------------------*/
int phDal4Nfc_uart_read(uint8_t * pBuffer, int nNbBytesToRead)
{
    int ret;
    int numRead = 0;
    struct timeval tv;
    struct timeval *ptv;
    struct timespec timeout;
    fd_set rfds;

    DAL_ASSERT_STR(gComPortContext.nOpened == 1, "read called but not opened!");
    DAL_DEBUG("_uart_read() called to read %d bytes", nNbBytesToRead);

    read_property();

    // Read timeout:
    // FW mode: 10s timeout
    // 1 byte read: steady-state LLC length read, allowed to block forever
    // >1 byte read: LLC payload, 100ms timeout (before pn544 re-transmit)
    if (nNbBytesToRead > 1 && !libnfc_firmware_mode) {
        clock_gettime(CLOCK_MONOTONIC, &timeout);
        timeout.tv_nsec += 100000000;
        if (timeout.tv_nsec > 1000000000) {
            timeout.tv_sec++;
            timeout.tv_nsec -= 1000000000;
        }
        ptv = &tv;
    } else if (libnfc_firmware_mode) {
        clock_gettime(CLOCK_MONOTONIC, &timeout);
        timeout.tv_sec += 10;
        ptv = &tv;
    } else {
        ptv = NULL;
    }

    while (numRead < nNbBytesToRead) {
       FD_ZERO(&rfds);
       FD_SET(gComPortContext.nHandle, &rfds);

       if (ptv) {
          tv = timeval_remaining(timeout);
          ptv = &tv;
       }

       ret = select(gComPortContext.nHandle + 1, &rfds, NULL, NULL, ptv);
       if (ret < 0) {
           DAL_DEBUG("select() errno=%d", errno);
           if (errno == EINTR || errno == EAGAIN) {
               continue;
           }
           return -1;
       } else if (ret == 0) {
           ALOGW("timeout!");
           break;  // return partial response
       }
       ret = read(gComPortContext.nHandle, pBuffer + numRead, nNbBytesToRead - numRead);
       if (ret > 0) {
           ret = apply_errors(pBuffer + numRead, ret);

           DAL_DEBUG("read %d bytes", ret);
           numRead += ret;
       } else if (ret == 0) {
           DAL_PRINT("_uart_read() EOF");
           return 0;
       } else {
           DAL_DEBUG("_uart_read() errno=%d", errno);
           if (errno == EINTR || errno == EAGAIN) {
               continue;
           }
           return -1;
       }
    }

    return numRead;
}

/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_link_write

PURPOSE:  Writes nNbBytesToWrite bytes from pBuffer to the link
          Returns the number of bytes that have been wrote to the interface or -1 in case of error.

-----------------------------------------------------------------------------*/

int phDal4Nfc_uart_write(uint8_t * pBuffer, int nNbBytesToWrite)
{
    int ret;
    int numWrote = 0;

    DAL_ASSERT_STR(gComPortContext.nOpened == 1, "write called but not opened!");
    DAL_DEBUG("_uart_write() called to write %d bytes\n", nNbBytesToWrite);

    while (numWrote < nNbBytesToWrite) {
        ret = write(gComPortContext.nHandle, pBuffer + numWrote, nNbBytesToWrite - numWrote);
        if (ret > 0) {
            DAL_DEBUG("wrote %d bytes", ret);
            numWrote += ret;
        } else if (ret == 0) {
            DAL_PRINT("_uart_write() EOF");
            return -1;
        } else {
            DAL_DEBUG("_uart_write() errno=%d", errno);
            if (errno == EINTR || errno == EAGAIN) {
                continue;
            }
            return -1;
        }
    }

    return numWrote;
}

/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_uart_reset

PURPOSE:  Reset the PN544, using the VEN pin

-----------------------------------------------------------------------------*/
int phDal4Nfc_uart_reset(long level)
{
    static const char NFC_POWER_PATH[] = "/sys/devices/platform/nfc-power/nfc_power";
    int sz;
    int fd = -1;
    int ret = NFCSTATUS_FAILED;
    char buffer[2];

    DAL_DEBUG("phDal4Nfc_uart_reset, VEN level = %ld", level);

    if (snprintf(buffer, sizeof(buffer), "%u", (unsigned int)level) != 1) {
        ALOGE("Bad nfc power level (%u)", (unsigned int)level);
        goto out;
    }

    fd = open(NFC_POWER_PATH, O_WRONLY);
    if (fd < 0) {
        ALOGE("open(%s) for write failed: %s (%d)", NFC_POWER_PATH,
                strerror(errno), errno);
        goto out;
    }
    sz = write(fd, &buffer, sizeof(buffer) - 1);
    if (sz < 0) {
        ALOGE("write(%s) failed: %s (%d)", NFC_POWER_PATH, strerror(errno),
             errno);
        goto out;
    }
    ret = NFCSTATUS_SUCCESS;
    if (level == 2) {
        libnfc_firmware_mode = 1;
    } else {
        libnfc_firmware_mode = 0;
    }

out:
    if (fd >= 0) {
        close(fd);
    }
    return ret;
}
