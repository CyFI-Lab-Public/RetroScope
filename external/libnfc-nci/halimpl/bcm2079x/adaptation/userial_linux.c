/******************************************************************************
 *
 *  Copyright (C) 1999-2012 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

#include "OverrideLog.h"
#include <string.h>
#include "gki.h"
#include "nfc_hal_api.h"
#include "nfc_hal_int.h"
#include "userial.h"
#include "nfc_target.h"

#include <pthread.h>
#include <termios.h>
#include <fcntl.h>
#include <errno.h>
#include <stdio.h>
#include <gki_int.h>
#include "hcidefs.h"
#include <poll.h>
#include "upio.h"
#include "bcm2079x.h"
#include "config.h"

#define HCISU_EVT                           EVENT_MASK(APPL_EVT_0)
#define MAX_ERROR                           10
#define default_transport                   "/dev/bcm2079x"

#define NUM_RESET_ATTEMPTS                  5
#define NFC_WAKE_ASSERTED_ON_POR            UPIO_OFF

#ifndef BTE_APPL_MAX_USERIAL_DEV_NAME
#define BTE_APPL_MAX_USERIAL_DEV_NAME           (256)
#endif
extern UINT8 appl_trace_level;

/* Mapping of USERIAL_PORT_x to linux */
extern UINT32 ScrProtocolTraceFlag;
static tUPIO_STATE current_nfc_wake_state = UPIO_OFF;
int uart_port  = 0;
int isLowSpeedTransport = 0;
int nfc_wake_delay = 0;
int nfc_write_delay = 0;
int gPowerOnDelay = 300;
static int gPrePowerOffDelay = 0;    // default value
static int gPostPowerOffDelay = 0;     // default value
static pthread_mutex_t close_thread_mutex = PTHREAD_MUTEX_INITIALIZER;

char userial_dev[BTE_APPL_MAX_USERIAL_DEV_NAME+1];
char power_control_dev[BTE_APPL_MAX_USERIAL_DEV_NAME+1];
tSNOOZE_MODE_CONFIG gSnoozeModeCfg = {
    NFC_HAL_LP_SNOOZE_MODE_SPI_I2C,     /* Sleep Mode (0=Disabled 1=UART 8=SPI/I2C) */
    NFC_HAL_LP_IDLE_THRESHOLD_HOST,     /* Idle Threshold Host */
    NFC_HAL_LP_IDLE_THRESHOLD_HC,       /* Idle Threshold HC */
    NFC_HAL_LP_ACTIVE_LOW,              /* NFC Wake active mode (0=ActiveLow 1=ActiveHigh) */
    NFC_HAL_LP_ACTIVE_HIGH              /* Host Wake active mode (0=ActiveLow 1=ActiveHigh) */
};

UINT8 bcmi2cnfc_client_addr = 0;
UINT8 bcmi2cnfc_read_multi_packets = 0;

#define USERIAL_Debug_verbose     ((ScrProtocolTraceFlag & 0x80000000) == 0x80000000)

#include <sys/socket.h>

#define LOG_TAG "USERIAL_LINUX"

static UINT8 spi_negotiation[10] = { 0xF0, /* CMD */
                                    0x00, /* SPI PARM Negotiation */
                                    0x01, /* SPI Version */
                                    0x00, /* SPI Mode:0, SPI_INT active low */
                                    0x00, /* 8Bit, MSB first, Little Endian byte order */
                                    0x00, /* Reserved */
                                    0xFF, /* Sleep timeout Lower Byte */
                                    0xFF, /* Sleep timeout Upper Byte */
                                    0x00, /* Reserved */
                                    0x00 /* Reserved */
};
static UINT8 spi_nego_res[20];

#include <ctype.h>

#define USING_BRCM_USB TRUE

/* use tc interface to change baudrate instead of close/open sequence which can fail on some platforms
 * due to tx line movement when opeing/closing the UART. the 43xx do not like this. */
#ifndef USERIAL_USE_TCIO_BAUD_CHANGE
#define USERIAL_USE_TCIO_BAUD_CHANGE FALSE
#endif

#ifndef USERIAL_USE_IO_BT_WAKE
#define USERIAL_USE_IO_BT_WAKE FALSE
#endif

/* this are the ioctl values used for bt_wake ioctl via UART driver. you may need to redefine at for
 * you platform! Logically they need to be unique and not colide with existing uart ioctl's.
 */
#ifndef USERIAL_IO_BT_WAKE_ASSERT
#define USERIAL_IO_BT_WAKE_ASSERT   0x8003
#endif
#ifndef USERIAL_IO_BT_WAKE_DEASSERT
#define USERIAL_IO_BT_WAKE_DEASSERT 0x8004
#endif
#ifndef USERIAL_IO_BT_WAKE_GET_ST
#define USERIAL_IO_BT_WAKE_GET_ST   0x8005
#endif

/* the read limit in this current implementation depends on the GKI_BUF3_SIZE
 * It would be better to use some ring buffer from the USERIAL_Read() is reading
 * instead of putting it into GKI buffers.
 */
#define READ_LIMIT (USERIAL_POOL_BUF_SIZE-BT_HDR_SIZE)
/*
 * minimum buffer size requirement to read a full sized packet from NFCC = 255 + 4 byte header
 */
#define MIN_BUFSIZE 259
#define     POLL_TIMEOUT    1000
/* priority of the reader thread */
#define USERIAL_READ_TRHEAD_PRIO 90
/* time (ms) to wait before trying to allocate again a GKI buffer */
#define NO_GKI_BUFFER_RECOVER_TIME 100
#define MAX_SERIAL_PORT (USERIAL_PORT_15 + 1)

extern void dumpbin(const char* data, int size);
extern UINT8 *scru_dump_hex (UINT8 *p, char *p_title, UINT32 len, UINT32 trace_layer, UINT32 trace_type);

static pthread_t      worker_thread1 = 0;

typedef struct  {
    volatile unsigned long bt_wake_state;
    int             sock;
    tUSERIAL_CBACK      *ser_cb;
    UINT16      baud;
    UINT8       data_bits;
    UINT16      parity;
    UINT8       stop_bits;
    UINT8       port;
    tUSERIAL_OPEN_CFG open_cfg;
    int         sock_power_control;
    int         client_device_address;
    struct timespec write_time;
} tLINUX_CB;

static tLINUX_CB linux_cb;  /* case of multipel port support use array : [MAX_SERIAL_PORT] */

void userial_close_thread(UINT32 params);

static UINT8 device_name[BTE_APPL_MAX_USERIAL_DEV_NAME+1];
static int   bSerialPortDevice = FALSE;
static int _timeout = POLL_TIMEOUT;
static BOOLEAN is_close_thread_is_waiting = FALSE;

static int change_client_addr(int addr);

int   perf_log_every_count = 0;
typedef struct {
    const char* label;
    long    lapse;
    long    bytes;
    long    count;
    long    overhead;
} tPERF_DATA;

/*******************************************************************************
**
** Function         perf_reset
**
** Description      reset performance measurement data
**
** Returns          none
**
*******************************************************************************/
void perf_reset(tPERF_DATA* t)
{
    t->count =
    t->bytes =
    t->lapse = 0;
}

/*******************************************************************************
**
** Function         perf_log
**
** Description      produce a log entry of cvurrent performance data
**
** Returns          none
**
*******************************************************************************/
void perf_log(tPERF_DATA* t)
{
    // round to nearest ms
    // t->lapse += 500;
    // t->lapse /= 1000;
    if (t->lapse)
    {
        if (t->bytes)
            ALOGD( "%s:%s, bytes=%ld, lapse=%ld (%d.%02d kbps) (bus data rate %d.%02d kbps) overhead %d(%d percent)\n",
                    __func__,
                    t->label, t->bytes, t->lapse,
                    (int)(8 * t->bytes / t->lapse), (int)(800 * t->bytes / (t->lapse)) % 100,
                    (int)(9 * (t->bytes + t->count * t->overhead) / t->lapse), (int)(900 * (t->bytes + t->count * t->overhead) / (t->lapse)) % 100,
                    (int)(t->count * t->overhead), (int)(t->count * t->overhead * 100 / t->bytes)
                    );
        else
            ALOGD( "%s:%s, lapse=%ld (average %ld)\n", __func__,
                    t->label, t->lapse, (int)t->lapse / t->count
                    );
    }
    perf_reset(t);
}

/*******************************************************************************
**
** Function         perf_update
**
** Description      update perforamnce measurement data
**
** Returns          none
**
*******************************************************************************/
void perf_update(tPERF_DATA* t, long lapse, long bytes)
{
    if (!perf_log_every_count)
        return;
    // round to nearest ms
    lapse += 500;
    lapse /= 1000;
    t->count++;
    t->bytes += bytes;
    t->lapse += lapse;
    if (t->count == perf_log_every_count)
        perf_log(t);
}

static tPERF_DATA   perf_poll = {"USERIAL_Poll", 0, 0, 0, 0};
static tPERF_DATA   perf_read = {"USERIAL_Read", 0, 0, 0, 9};
static tPERF_DATA   perf_write = {"USERIAL_Write", 0, 0, 0, 3};
static tPERF_DATA   perf_poll_2_poll = {"USERIAL_Poll_to_Poll", 0, 0, 0, 0};
static clock_t      _poll_t0 = 0;

static UINT32 userial_baud_tbl[] =
{
    300,        /* USERIAL_BAUD_300          0 */
    600,        /* USERIAL_BAUD_600          1 */
    1200,       /* USERIAL_BAUD_1200         2 */
    2400,       /* USERIAL_BAUD_2400         3 */
    9600,       /* USERIAL_BAUD_9600         4 */
    19200,      /* USERIAL_BAUD_19200        5 */
    57600,      /* USERIAL_BAUD_57600        6 */
    115200,     /* USERIAL_BAUD_115200       7 */
    230400,     /* USERIAL_BAUD_230400       8 */
    460800,     /* USERIAL_BAUD_460800       9 */
    921600,     /* USERIAL_BAUD_921600       10 */
    1000000,    /* USERIAL_BAUD_1M           11 */
    1500000,    /* USERIAL_BAUD_1_5M         12 */
    2000000,    /* USERIAL_BAUD_2M           13 */
    3000000,    /* USERIAL_BAUD_3M           14 */
    4000000     /* USERIAL_BAUD_4M           15 */
};

/*******************************************************************************
**
** Function         wake_state
**
** Description      return current state of NFC_WAKE gpio
**
** Returns          GPIO value to wake NFCC
**
*******************************************************************************/
static inline int wake_state()
{
    return ((gSnoozeModeCfg.nfc_wake_active_mode == NFC_HAL_LP_ACTIVE_HIGH) ? UPIO_ON : UPIO_OFF);
}

/*******************************************************************************
**
** Function         sleep_state
**
** Description      return current state of NFC_WAKE gpio
**
** Returns          GPIO value to allow NFCC to goto sleep
**
*******************************************************************************/
static inline int sleep_state()
{
    return ((gSnoozeModeCfg.nfc_wake_active_mode == NFC_HAL_LP_ACTIVE_HIGH) ? UPIO_OFF : UPIO_ON);
}

/*******************************************************************************
**
** Function         isWake
**
** Description      return current state of NFC_WAKE gpio based on the active mode setting
**
** Returns          asserted_state if it's awake, deasserted_state if it's allowed to sleep
**
*******************************************************************************/
static inline int isWake(int state)
{
    int     asserted_state = ((gSnoozeModeCfg.nfc_wake_active_mode == NFC_HAL_LP_ACTIVE_HIGH) ? UPIO_ON : UPIO_OFF);
    return (state != -1) ?
        state == asserted_state :
        current_nfc_wake_state == asserted_state;
}

/*******************************************************************************
**
** Function           setWriteDelay
**
** Description        Record a delay for the next write operation
**
** Input Parameter    delay in milliseconds
**
** Comments           use this function to register a delay before next write,
**                    This is used in three instances: power up delay, wake delay
**                    and write delay
**
*******************************************************************************/
static void setWriteDelay(int delay)
{
    if (delay <= 0) {
        // Set a minimum delay of 5ms between back-to-back writes
        delay = 5;
    }

    clock_gettime(CLOCK_MONOTONIC, &linux_cb.write_time);
    if (delay > 1000)
    {
        linux_cb.write_time.tv_sec += delay / 1000;
        delay %= 1000;
    }
    unsigned long write_delay = delay * 1000 * 1000;
    linux_cb.write_time.tv_nsec += write_delay;
    if (linux_cb.write_time.tv_nsec > 1000*1000*1000)
    {
        linux_cb.write_time.tv_nsec -= 1000*1000*1000;
        linux_cb.write_time.tv_sec++;
    }
}

/*******************************************************************************
**
** Function           doWriteDelay
**
** Description        Execute a delay as registered in setWriteDelay()
**
** Output Parameter   none
**
** Returns            none
**
** Comments           This function calls GKI_Delay to execute a delay to fulfill
**                    the delay registered earlier.
**
*******************************************************************************/
static void doWriteDelay()
{
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    long delay = 0;

    if (now.tv_sec > linux_cb.write_time.tv_sec)
        return;
    else if (now.tv_sec == linux_cb.write_time.tv_sec)
    {
        if (now.tv_nsec > linux_cb.write_time.tv_nsec)
            return;
        delay = (linux_cb.write_time.tv_nsec - now.tv_nsec) / 1000000;
    }
    else
        delay = (linux_cb.write_time.tv_sec - now.tv_sec) * 1000 + linux_cb.write_time.tv_nsec / 1000000 - now.tv_nsec / 1000000;

    if (delay > 0 && delay < 1000)
    {
        ALOGD_IF((appl_trace_level>=BT_TRACE_LEVEL_DEBUG), "doWriteDelay() delay %ld ms", delay);
        GKI_delay(delay);
    }
}

/*******************************************************************************
**
** Function         create_signal_fds
**
** Description      create a socketpair for read thread to use
**
** Returns          file descriptor
**
*******************************************************************************/

static int signal_fds[2];
static inline int create_signal_fds(struct pollfd* set)
{
    if (signal_fds[0] == 0 && socketpair(AF_UNIX, SOCK_STREAM, 0, signal_fds) < 0)
    {
        ALOGE("%s create_signal_sockets:socketpair failed, errno: %d", __func__, errno);
        return -1;
    }
    set->fd = signal_fds[0];
    return signal_fds[0];
}

/*******************************************************************************
**
** Function         close_signal_fds
**
** Description      close the socketpair
**
** Returns          none
**
*******************************************************************************/
static inline void close_signal_fds()
{
    close(signal_fds[0]);
    signal_fds[0] = 0;

    close(signal_fds[1]);
    signal_fds[1] = 0;
}

/*******************************************************************************
**
** Function         send_wakeup_signal
**
** Description      send a one byte data to the socket as signal to the read thread
**                  for it to stop
**
** Returns          number of bytes sent, or error no
**
*******************************************************************************/
static inline int send_wakeup_signal()
{
    char sig_on = 1;
    ALOGD("%s: Sending signal to %d", __func__, signal_fds[1]);
    return send(signal_fds[1], &sig_on, sizeof(sig_on), 0);
}

/*******************************************************************************
**
** Function         reset_signal
**
** Description      read the one byte data from the socket
**
** Returns          received data
**
*******************************************************************************/
static inline int reset_signal()
{
    char sig_recv = 0;
    ALOGD("%s: Receiving signal from %d", __func__, signal_fds[0]);
    recv(signal_fds[0], &sig_recv, sizeof(sig_recv), MSG_WAITALL);
    return (int)sig_recv;
}

/*******************************************************************************
**
** Function         is_signaled
**
** Description      test if there's data waiting on the socket
**
** Returns          TRUE is data is available
**
*******************************************************************************/
static inline int is_signaled(struct pollfd* set)
{
    return ((set->revents & POLLIN) == POLLIN) || ((set->revents & POLLRDNORM) == POLLRDNORM) ;
}

/******************************************************************************/

typedef unsigned char uchar;

BUFFER_Q Userial_in_q;

/*******************************************************************************
 **
 ** Function           USERIAL_GetLineSpeed
 **
 ** Description        This function convert USERIAL baud to line speed.
 **
 ** Output Parameter   None
 **
 ** Returns            line speed
 **
 *******************************************************************************/
UDRV_API extern UINT32 USERIAL_GetLineSpeed(UINT8 baud)
{
    return (baud <= USERIAL_BAUD_4M) ?
            userial_baud_tbl[baud-USERIAL_BAUD_300] : 0;
}

/*******************************************************************************
 **
 ** Function           USERIAL_GetBaud
 **
 ** Description        This function convert line speed to USERIAL baud.
 **
 ** Output Parameter   None
 **
 ** Returns            line speed
 **
 *******************************************************************************/
UDRV_API extern UINT8 USERIAL_GetBaud(UINT32 line_speed)
{
    UINT8 i;
    for (i = USERIAL_BAUD_300; i <= USERIAL_BAUD_921600; i++)
    {
        if (userial_baud_tbl[i-USERIAL_BAUD_300] == line_speed)
            return i;
    }

    return USERIAL_BAUD_AUTO;
}

/*******************************************************************************
**
** Function           USERIAL_Init
**
** Description        This function initializes the  serial driver.
**
** Output Parameter   None
**
** Returns            Nothing
**
*******************************************************************************/

UDRV_API void    USERIAL_Init(void * p_cfg)
{
    ALOGI(__FUNCTION__);

    memset(&linux_cb, 0, sizeof(linux_cb));
    linux_cb.sock = -1;
    linux_cb.ser_cb = NULL;
    linux_cb.sock_power_control = -1;
    linux_cb.client_device_address = 0;
    GKI_init_q(&Userial_in_q);
    return;
}

/*******************************************************************************
 **
 ** Function           my_read
 **
 ** Description        This function read a packet from driver.
 **
 ** Output Parameter   None
 **
 ** Returns            number of bytes in the packet or error code
 **
 *******************************************************************************/
int my_read(int fd, uchar *pbuf, int len)
{
    struct pollfd fds[2];

    int n = 0;
    int ret = 0;
    int count = 0;
    int offset = 0;
    clock_t t1, t2;

    if (!isLowSpeedTransport && _timeout != POLL_TIMEOUT)
        ALOGD_IF((appl_trace_level>=BT_TRACE_LEVEL_DEBUG), "%s: enter, pbuf=%lx, len = %d\n", __func__, (unsigned long)pbuf, len);
    memset(pbuf, 0, len);
    /* need to use select in order to avoid collistion between read and close on same fd */
    /* Initialize the input set */
    fds[0].fd = fd;
    fds[0].events = POLLIN | POLLERR | POLLRDNORM;
    fds[0].revents = 0;

    create_signal_fds(&fds[1]);
    fds[1].events = POLLIN | POLLERR | POLLRDNORM;
    fds[1].revents = 0;
    t1 = clock();
    n = poll(fds, 2, _timeout);
    t2 = clock();
    perf_update(&perf_poll, t2 - t1, 0);
    if (_poll_t0)
        perf_update(&perf_poll_2_poll, t2 - _poll_t0, 0);

    _poll_t0 = t2;
    /* See if there was an error */
    if (n < 0)
    {
        ALOGD( "select failed; errno = %d\n", errno);
        return -errno;
    }
    else if (n == 0)
        return -EAGAIN;

    if (is_signaled(&fds[1]))
    {
        ALOGD( "%s: exit signal received\n", __func__);
        reset_signal();
        return -1;
    }
    if (!bSerialPortDevice || len < MIN_BUFSIZE)
        count = len;
    else
        count = 1;
    do {
        t2 = clock();
        ret = read(fd, pbuf+offset, (size_t)count);
        if (ret > 0)
            perf_update(&perf_read, clock()-t2, ret);

        if (ret <= 0 || !bSerialPortDevice || len < MIN_BUFSIZE)
            break;

        if (isLowSpeedTransport)
            goto done;

        if (offset == 0)
        {
            if (pbuf[offset] == HCIT_TYPE_NFC)
                count = 3;
            else if (pbuf[offset] == HCIT_TYPE_EVENT)
                count = 2;
            else
            {
                ALOGD( "%s: unknown HCIT type header pbuf[%d] = %x\n", __func__, offset, pbuf[offset]);
                break;
            }
            offset = 1;
        }
        else if (offset == 1)
        {
            offset += count;
            count = pbuf[offset-1];
            if (count > (len - offset)) //if (count > (remaining buffer size))
                count = len - offset; //only read what the remaining buffer size can hold
        }
        else
        {
            offset += ret;
            count -= ret;
        }
        if (count == 0)
        {
            ret = offset;
            break;
        }
    } while (count > 0);
 #if VALIDATE_PACKET
/*
 * vallidate the packet structure
 */
    if (ret > 0 && len >= MIN_BUFSIZE)
    {
        count = 0;
        while (count < ret)
        {
            if (pbuf[count] == HCIT_TYPE_NFC)
            {
                if (USERIAL_Debug_verbose)
                    scru_dump_hex(pbuf+count, NULL, pbuf[count+3]+4, 0, 0);
                count += pbuf[count+3]+4;
            }
            else if (pbuf[count] == HCIT_TYPE_EVENT)
            {
                if (USERIAL_Debug_verbose)
                    scru_dump_hex(pbuf+count, NULL, pbuf[count+2]+3, 0, 0);
                count += pbuf[count+2]+3;
            }
            else
            {
                ALOGD( "%s: unknown HCIT type header pbuf[%d] = %x, remain %d bytes\n", __func__, count, pbuf[count], ret-count);
                scru_dump_hex(pbuf+count, NULL, ret - count, 0, 0);
                break;
            }
        } /* while*/
    }
#endif
done:
    if (!isLowSpeedTransport)
        ALOGD_IF((appl_trace_level>=BT_TRACE_LEVEL_DEBUG), "%s: return %d(0x%x) bytes, errno=%d count=%d, n=%d, timeout=%d\n", __func__,
            ret, ret, errno, count, n, _timeout);
    if (_timeout == POLL_TIMEOUT)
        _timeout = -1;
    return ret;
}
extern BOOLEAN gki_chk_buf_damage(void *p_buf);
static int sRxLength = 0;

/*******************************************************************************
 **
 ** Function           userial_read_thread
 **
 ** Description        entry point of read thread.
 **
 ** Output Parameter   None
 **
 ** Returns            0
 **
 *******************************************************************************/
UINT32 userial_read_thread(UINT32 arg)
{
    int rx_length;
    int error_count = 0;
    int bErrorReported = 0;
    int iMaxError = MAX_ERROR;
    BT_HDR *p_buf = NULL;

    worker_thread1 = pthread_self();

    ALOGD( "start userial_read_thread, id=%lx", worker_thread1);
    _timeout = POLL_TIMEOUT;

    for (;linux_cb.sock > 0;)
    {
        BT_HDR *p_buf;
        UINT8 *current_packet;

        if ((p_buf = (BT_HDR *) GKI_getpoolbuf( USERIAL_POOL_ID ) )!= NULL)
        {
            p_buf->offset = 0;
            p_buf->layer_specific = 0;

            current_packet = (UINT8 *) (p_buf + 1);
            rx_length = my_read(linux_cb.sock, current_packet, READ_LIMIT);

        }
        else
        {
            ALOGE( "userial_read_thread(): unable to get buffer from GKI p_buf = %p poolid = %d\n", p_buf, USERIAL_POOL_ID);
            rx_length = 0;  /* paranoia setting */
            GKI_delay( NO_GKI_BUFFER_RECOVER_TIME );
            continue;
        }
        if (rx_length > 0)
        {
            bErrorReported = 0;
            error_count = 0;
            iMaxError = 3;
            if (rx_length > sRxLength)
                sRxLength = rx_length;
            p_buf->len = (UINT16)rx_length;
            GKI_enqueue(&Userial_in_q, p_buf);
            if (!isLowSpeedTransport)
                ALOGD_IF((appl_trace_level>=BT_TRACE_LEVEL_DEBUG), "userial_read_thread(): enqueued p_buf=%p, count=%d, length=%d\n",
                            p_buf, Userial_in_q.count, rx_length);

            if (linux_cb.ser_cb != NULL)
                (*linux_cb.ser_cb)(linux_cb.port, USERIAL_RX_READY_EVT, (tUSERIAL_EVT_DATA *)p_buf);

            GKI_send_event(USERIAL_HAL_TASK, HCISU_EVT);
        }
        else
        {
            GKI_freebuf( p_buf );
            if (rx_length == -EAGAIN)
                continue;
            else if (rx_length == -1)
            {
                ALOGD( "userial_read_thread(): exiting\n");
                break;
            }
            else if (rx_length == 0 && !isWake(-1))
                continue;
            ++error_count;
            if (rx_length <= 0 && ((error_count > 0) && ((error_count % iMaxError) == 0)))
            {
                if (bErrorReported == 0)
                {
                    ALOGE( "userial_read_thread(): my_read returned (%d) error count = %d, errno=%d return USERIAL_ERR_EVT\n",
                            rx_length, error_count, errno);
                    if (linux_cb.ser_cb != NULL)
                        (*linux_cb.ser_cb)(linux_cb.port, USERIAL_ERR_EVT, (tUSERIAL_EVT_DATA *)p_buf);

                    GKI_send_event(USERIAL_HAL_TASK, HCISU_EVT);
                    ++bErrorReported;
                }
                if (sRxLength == 0)
                {
                    ALOGE( "userial_read_thread(): my_read returned (%d) error count = %d, errno=%d exit read thread\n",
                            rx_length, error_count, errno);
                    break;
                }
            }
        }
    } /* for */

    ALOGD( "userial_read_thread(): freeing GKI_buffers\n");
    while ((p_buf = (BT_HDR *) GKI_dequeue (&Userial_in_q)) != NULL)
    {
        GKI_freebuf(p_buf);
        ALOGD("userial_read_thread: dequeued buffer from Userial_in_q\n");
    }

    GKI_exit_task (GKI_get_taskid ());
    ALOGD( "USERIAL READ: EXITING TASK\n");

    return 0;
}

/*******************************************************************************
 **
 ** Function           userial_to_tcio_baud
 **
 ** Description        helper function converts USERIAL baud rates into TCIO conforming baud rates
 **
 ** Output Parameter   None
 **
 ** Returns            TRUE - success
 **                    FALSE - unsupported baud rate, default of 115200 is used
 **
 *******************************************************************************/
BOOLEAN userial_to_tcio_baud(UINT8 cfg_baud, UINT32 * baud)
{
    if (cfg_baud == USERIAL_BAUD_600)
        *baud = B600;
    else if (cfg_baud == USERIAL_BAUD_1200)
        *baud = B1200;
    else if (cfg_baud == USERIAL_BAUD_9600)
        *baud = B9600;
    else if (cfg_baud == USERIAL_BAUD_19200)
        *baud = B19200;
    else if (cfg_baud == USERIAL_BAUD_57600)
        *baud = B57600;
    else if (cfg_baud == USERIAL_BAUD_115200)
        *baud = B115200 | CBAUDEX;
    else if (cfg_baud == USERIAL_BAUD_230400)
        *baud = B230400;
    else if (cfg_baud == USERIAL_BAUD_460800)
        *baud = B460800;
    else if (cfg_baud == USERIAL_BAUD_921600)
        *baud = B921600;
    else if (cfg_baud == USERIAL_BAUD_1M)
        *baud = B1000000;
    else if (cfg_baud == USERIAL_BAUD_2M)
        *baud = B2000000;
    else if (cfg_baud == USERIAL_BAUD_3M)
        *baud = B3000000;
    else if (cfg_baud == USERIAL_BAUD_4M)
        *baud = B4000000;
    else
    {
        ALOGE( "USERIAL_Open: unsupported baud idx %i", cfg_baud );
        *baud = B115200;
        return FALSE;
    }
    return TRUE;
}

#if (USERIAL_USE_IO_BT_WAKE==TRUE)
/*******************************************************************************
 **
 ** Function           userial_io_init_bt_wake
 **
 ** Description        helper function to set the open state of the bt_wake if ioctl
 **                    is used. it should not hurt in the rfkill case but it might
 **                    be better to compile it out.
 **
 ** Returns            none
 **
 *******************************************************************************/
void userial_io_init_bt_wake( int fd, unsigned long * p_wake_state )
{
    /* assert BT_WAKE for ioctl. should NOT hurt on rfkill version */
    ioctl( fd, USERIAL_IO_BT_WAKE_ASSERT, NULL);
    ioctl( fd, USERIAL_IO_BT_WAKE_GET_ST, p_wake_state );
    if ( *p_wake_state == 0)
        ALOGI("\n***userial_io_init_bt_wake(): Ooops, asserted BT_WAKE signal, but still got BT_WAKE state == to %d\n",
             *p_wake_state );

    *p_wake_state = 1;
}
#endif

/*******************************************************************************
**
** Function           USERIAL_Open
**
** Description        Open the indicated serial port with the given configuration
**
** Output Parameter   None
**
** Returns            Nothing
**
*******************************************************************************/
UDRV_API void USERIAL_Open(tUSERIAL_PORT port, tUSERIAL_OPEN_CFG *p_cfg, tUSERIAL_CBACK *p_cback)
{
    UINT32 baud = 0;
    UINT8 data_bits = 0;
    UINT16 parity = 0;
    UINT8 stop_bits = 0;
    struct termios termios;
    const char ttyusb[] = "/dev/ttyUSB";
    const char devtty[] = "/dev/tty";
    unsigned long num = 0;
    int     ret = 0;

    ALOGI("USERIAL_Open(): enter");

    //if userial_close_thread() is waiting to run; let it go first;
    //let it finish; then continue this function
    while (TRUE)
    {
        pthread_mutex_lock(&close_thread_mutex);
        if (is_close_thread_is_waiting)
        {
            pthread_mutex_unlock(&close_thread_mutex);
            ALOGI("USERIAL_Open(): wait for close-thread");
            sleep (1);
        }
        else
            break;
    }

    // restore default power off delay settings incase they were changed in userial_set_poweroff_delays()
    gPrePowerOffDelay = 0;
    gPostPowerOffDelay = 0;

    if ( !GetStrValue ( NAME_TRANSPORT_DRIVER, userial_dev, sizeof ( userial_dev ) ) )
        strcpy ( userial_dev, default_transport );
    if ( GetNumValue ( NAME_UART_PORT, &num, sizeof ( num ) ) )
        uart_port = num;
    if ( GetNumValue ( NAME_LOW_SPEED_TRANSPORT, &num, sizeof ( num ) ) )
        isLowSpeedTransport = num;
    if ( GetNumValue ( NAME_NFC_WAKE_DELAY, &num, sizeof ( num ) ) )
        nfc_wake_delay = num;
    if ( GetNumValue ( NAME_NFC_WRITE_DELAY, &num, sizeof ( num ) ) )
        nfc_write_delay = num;
    if ( GetNumValue ( NAME_PERF_MEASURE_FREQ, &num, sizeof ( num ) ) )
        perf_log_every_count = num;
    if ( GetNumValue ( NAME_POWER_ON_DELAY, &num, sizeof ( num ) ) )
        gPowerOnDelay = num;
    if ( GetNumValue ( NAME_PRE_POWER_OFF_DELAY, &num, sizeof ( num ) ) )
        gPrePowerOffDelay = num;
    if ( GetNumValue ( NAME_POST_POWER_OFF_DELAY, &num, sizeof ( num ) ) )
        gPostPowerOffDelay = num;
    ALOGI("USERIAL_Open() device: %s port=%d, uart_port=%d WAKE_DELAY(%d) WRITE_DELAY(%d) POWER_ON_DELAY(%d) PRE_POWER_OFF_DELAY(%d) POST_POWER_OFF_DELAY(%d)",
            (char*)userial_dev, port, uart_port, nfc_wake_delay, nfc_write_delay, gPowerOnDelay, gPrePowerOffDelay,
            gPostPowerOffDelay);

    strcpy((char*)device_name, (char*)userial_dev);
    sRxLength = 0;
    _poll_t0 = 0;

    if ((strncmp(userial_dev, ttyusb, sizeof(ttyusb)-1) == 0) ||
        (strncmp(userial_dev, devtty, sizeof(devtty)-1) == 0) )
    {
        if (uart_port >= MAX_SERIAL_PORT)
        {
            ALOGD( "Port > MAX_SERIAL_PORT\n");
            goto done_open;
        }
        bSerialPortDevice = TRUE;
        sprintf((char*)device_name, "%s%d", (char*)userial_dev, uart_port);
        ALOGI("USERIAL_Open() using device_name: %s ", (char*)device_name);
        if (!userial_to_tcio_baud(p_cfg->baud, &baud))
            goto done_open;

        if (p_cfg->fmt & USERIAL_DATABITS_8)
            data_bits = CS8;
        else if (p_cfg->fmt & USERIAL_DATABITS_7)
            data_bits = CS7;
        else if (p_cfg->fmt & USERIAL_DATABITS_6)
            data_bits = CS6;
        else if (p_cfg->fmt & USERIAL_DATABITS_5)
            data_bits = CS5;
        else
            goto done_open;

        if (p_cfg->fmt & USERIAL_PARITY_NONE)
            parity = 0;
        else if (p_cfg->fmt & USERIAL_PARITY_EVEN)
            parity = PARENB;
        else if (p_cfg->fmt & USERIAL_PARITY_ODD)
            parity = (PARENB | PARODD);
        else
            goto done_open;

        if (p_cfg->fmt & USERIAL_STOPBITS_1)
            stop_bits = 0;
        else if (p_cfg->fmt & USERIAL_STOPBITS_2)
            stop_bits = CSTOPB;
        else
            goto done_open;
    }
    else
        strcpy((char*)device_name, (char*)userial_dev);

    {
        ALOGD("%s Opening %s\n",  __FUNCTION__, device_name);
        if ((linux_cb.sock = open((char*)device_name, O_RDWR | O_NOCTTY )) == -1)
        {
            ALOGI("%s unable to open %s",  __FUNCTION__, device_name);
            GKI_send_event(NFC_HAL_TASK, NFC_HAL_TASK_EVT_TERMINATE);
            goto done_open;
        }
        ALOGD( "sock = %d\n", linux_cb.sock);
        if (GetStrValue ( NAME_POWER_CONTROL_DRIVER, power_control_dev, sizeof ( power_control_dev ) ) &&
            power_control_dev[0] != '\0')
        {
            if (strcmp(power_control_dev, userial_dev) == 0)
                linux_cb.sock_power_control = linux_cb.sock;
            else
            {
                if ((linux_cb.sock_power_control = open((char*)power_control_dev, O_RDWR | O_NOCTTY )) == -1)
                {
                    ALOGI("%s unable to open %s",  __FUNCTION__, power_control_dev);
                }
            }
        }
        if ( bSerialPortDevice )
        {
            tcflush(linux_cb.sock, TCIOFLUSH);
            tcgetattr(linux_cb.sock, &termios);

            termios.c_cflag &= ~(CSIZE | PARENB);
            termios.c_cflag = CLOCAL|CREAD|data_bits|stop_bits|parity;
            if (!parity)
                termios.c_cflag |= IGNPAR;
            // termios.c_cflag &= ~CRTSCTS;
            termios.c_oflag = 0;
            termios.c_lflag &= ~(ECHO | ECHONL | ICANON | IEXTEN | ISIG);
            termios.c_iflag &= ~(BRKINT | ICRNL | INLCR | ISTRIP | IXON | IGNBRK | PARMRK | INPCK);
            termios.c_lflag = 0;
            termios.c_iflag = 0;
            cfsetospeed(&termios, baud);
            cfsetispeed(&termios, baud);

            termios.c_cc[VTIME] = 0;
            termios.c_cc[VMIN] = 1;
            tcsetattr(linux_cb.sock, TCSANOW, &termios);

            tcflush(linux_cb.sock, TCIOFLUSH);

#if (USERIAL_USE_IO_BT_WAKE==TRUE)
            userial_io_init_bt_wake( linux_cb.sock, &linux_cb.bt_wake_state );
#endif
            GKI_delay(gPowerOnDelay);
        }
        else
        {
            USERIAL_PowerupDevice(port);
        }
    }

    linux_cb.ser_cb     = p_cback;
    linux_cb.port = port;
    memcpy(&linux_cb.open_cfg, p_cfg, sizeof(tUSERIAL_OPEN_CFG));
    GKI_create_task ((TASKPTR)userial_read_thread, USERIAL_HAL_TASK, (INT8*)"USERIAL_HAL_TASK", 0, 0, (pthread_cond_t*)NULL, NULL);


#if (defined USERIAL_DEBUG) && (USERIAL_DEBUG == TRUE)
    ALOGD( "Leaving USERIAL_Open\n");
#endif

#if (SERIAL_AMBA == TRUE)
    /* give 20ms time for reader thread */
    GKI_delay(20);
#endif

done_open:
    pthread_mutex_unlock(&close_thread_mutex);
    ALOGI("USERIAL_Open(): exit");
    return;
}

/*******************************************************************************
**
** Function           USERIAL_Read
**
** Description        Read data from a serial port using byte buffers.
**
** Output Parameter   None
**
** Returns            Number of bytes actually read from the serial port and
**                    copied into p_data.  This may be less than len.
**
*******************************************************************************/

static BT_HDR *pbuf_USERIAL_Read = NULL;

UDRV_API UINT16  USERIAL_Read(tUSERIAL_PORT port, UINT8 *p_data, UINT16 len)
{
    UINT16 total_len = 0;
    UINT16 copy_len = 0;
    UINT8 * current_packet = NULL;

#if (defined USERIAL_DEBUG) && (USERIAL_DEBUG == TRUE)
    ALOGD( "%s ++ len=%d pbuf_USERIAL_Read=%p, p_data=%p\n", __func__, len, pbuf_USERIAL_Read, p_data);
#endif
    do
    {
        if (pbuf_USERIAL_Read != NULL)
        {
            current_packet = ((UINT8 *)(pbuf_USERIAL_Read + 1)) + (pbuf_USERIAL_Read->offset);

            if ((pbuf_USERIAL_Read->len) <= (len - total_len))
                copy_len = pbuf_USERIAL_Read->len;
            else
                copy_len = (len - total_len);

            memcpy((p_data + total_len), current_packet, copy_len);

            total_len += copy_len;

            pbuf_USERIAL_Read->offset += copy_len;
            pbuf_USERIAL_Read->len -= copy_len;

            if (pbuf_USERIAL_Read->len == 0)
            {
                GKI_freebuf(pbuf_USERIAL_Read);
                pbuf_USERIAL_Read = NULL;
            }
        }

        if (pbuf_USERIAL_Read == NULL && (total_len < len))
            pbuf_USERIAL_Read = (BT_HDR *)GKI_dequeue(&Userial_in_q);

    } while ((pbuf_USERIAL_Read != NULL) && (total_len < len));

#if (defined USERIAL_DEBUG) && (USERIAL_DEBUG == TRUE)
    ALOGD( "%s: returned %d bytes", __func__, total_len);
#endif
    return total_len;
}

/*******************************************************************************
**
** Function           USERIAL_Readbuf
**
** Description        Read data from a serial port using GKI buffers.
**
** Output Parameter   Pointer to a GKI buffer which contains the data.
**
** Returns            Nothing
**
** Comments           The caller of this function is responsible for freeing the
**                    GKI buffer when it is finished with the data.  If there is
**                    no data to be read, the value of the returned pointer is
**                    NULL.
**
*******************************************************************************/

UDRV_API void    USERIAL_ReadBuf(tUSERIAL_PORT port, BT_HDR **p_buf)
{

}

/*******************************************************************************
**
** Function           USERIAL_WriteBuf
**
** Description        Write data to a serial port using a GKI buffer.
**
** Output Parameter   None
**
** Returns            TRUE  if buffer accepted for write.
**                    FALSE if there is already a buffer being processed.
**
** Comments           The buffer will be freed by the serial driver.  Therefore,
**                    the application calling this function must not free the
**                    buffer.
**
*******************************************************************************/

UDRV_API BOOLEAN USERIAL_WriteBuf(tUSERIAL_PORT port, BT_HDR *p_buf)
{
    return FALSE;
}

/*******************************************************************************
**
** Function           USERIAL_Write
**
** Description        Write data to a serial port using a byte buffer.
**
** Output Parameter   None
**
** Returns            Number of bytes actually written to the transport.  This
**                    may be less than len.
**
*******************************************************************************/
UDRV_API UINT16  USERIAL_Write(tUSERIAL_PORT port, UINT8 *p_data, UINT16 len)
{
    int ret = 0, total = 0;
    int i = 0;
    clock_t t;

    ALOGD_IF((appl_trace_level>=BT_TRACE_LEVEL_DEBUG), "USERIAL_Write: (%d bytes)", len);
    pthread_mutex_lock(&close_thread_mutex);

    doWriteDelay();
    t = clock();
    while (len != 0 && linux_cb.sock != -1)
    {
        ret = write(linux_cb.sock, p_data + total, len);
        if (ret < 0)
        {
            ALOGE("USERIAL_Write len = %d, ret = %d, errno = %d", len, ret, errno);
            break;
        }
        else
        {
            ALOGD_IF((appl_trace_level>=BT_TRACE_LEVEL_DEBUG), "USERIAL_Write len = %d, ret = %d", len, ret);
        }

        total += ret;
        len -= ret;
    }
    perf_update(&perf_write, clock() - t, total);

    /* register a delay for next write */
    setWriteDelay(total * nfc_write_delay / 1000);

    pthread_mutex_unlock(&close_thread_mutex);

    return ((UINT16)total);
}

/*******************************************************************************
**
** Function           userial_change_rate
**
** Description        change naud rate
**
** Output Parameter   None
**
** Returns            None
**
*******************************************************************************/
void userial_change_rate(UINT8 baud)
{
#if defined (USING_BRCM_USB) && (USING_BRCM_USB == FALSE)
    struct termios termios;
#endif
#if (USERIAL_USE_TCIO_BAUD_CHANGE==TRUE)
    UINT32 tcio_baud;
#endif

#if defined (USING_BRCM_USB) && (USING_BRCM_USB == FALSE)
    tcflush(linux_cb.sock, TCIOFLUSH);

    tcgetattr(linux_cb.sock, &termios);

    cfmakeraw(&termios);
    cfsetospeed(&termios, baud);
    cfsetispeed(&termios, baud);

    termios.c_cflag |= (CLOCAL | CREAD | CRTSCTS | stop_bits);

    tcsetattr(linux_cb.sock, TCSANOW, &termios);
    tcflush(linux_cb.sock, TCIOFLUSH);

#else
#if (USERIAL_USE_TCIO_BAUD_CHANGE==FALSE)
    fprintf(stderr, "userial_change_rate: Closing UART Port\n");
    ALOGI("userial_change_rate: Closing UART Port\n");
    USERIAL_Close(linux_cb.port);

    GKI_delay(50);

    /* change baud rate in settings - leave everything else the same  */
    linux_cb.open_cfg.baud = baud;

    ALOGD( "userial_change_rate: Attempting to reopen the UART Port at 0x%08x\n", (unsigned int)USERIAL_GetLineSpeed(baud));
    ALOGI("userial_change_rate: Attempting to reopen the UART Port at %i\n", (unsigned int)USERIAL_GetLineSpeed(baud));

    USERIAL_Open(linux_cb.port, &linux_cb.open_cfg, linux_cb.ser_cb);
#else /* amba uart */
    fprintf(stderr, "userial_change_rate(): changeing baud rate via TCIO \n");
    ALOGI( "userial_change_rate: (): changeing baud rate via TCIO \n");
    /* change baud rate in settings - leave everything else the same  */
    linux_cb.open_cfg.baud = baud;
    if (!userial_to_tcio_baud(linux_cb.open_cfg.baud, &tcio_baud))
        return;

    tcflush(linux_cb.sock, TCIOFLUSH);

    /* get current settings. they should be fine besides baud rate we want to change */
    tcgetattr(linux_cb.sock, &termios);

    /* set input/output baudrate */
    cfsetospeed(&termios, tcio_baud);
    cfsetispeed(&termios, tcio_baud);
    tcsetattr(linux_cb.sock, TCSANOW, &termios);

    tcflush(linux_cb.sock, TCIOFLUSH);
#endif
#endif   /* USING_BRCM_USB  */
}

/*******************************************************************************
**
** Function           userial_close_port
**
** Description        close the transport driver
**
** Returns            Nothing
**
*******************************************************************************/
void userial_close_port( void )
{
    USERIAL_Close(linux_cb.port);
}

/*******************************************************************************
**
** Function           USERIAL_Ioctl
**
** Description        Perform an operation on a serial port.
**
** Output Parameter   The p_data parameter is either an input or output depending
**                    on the operation.
**
** Returns            Nothing
**
*******************************************************************************/

UDRV_API void    USERIAL_Ioctl(tUSERIAL_PORT port, tUSERIAL_OP op, tUSERIAL_IOCTL_DATA *p_data)
{
#if (defined LINUX_OS) && (LINUX_OS == TRUE)
    USB_SCO_CONTROL ioctl_data;

    /* just ignore port parameter as we are using USB in this case  */
#endif

    switch (op)
    {
    case USERIAL_OP_FLUSH:
        break;
    case USERIAL_OP_FLUSH_RX:
        break;
    case USERIAL_OP_FLUSH_TX:
        break;
    case USERIAL_OP_BAUD_WR:
        ALOGI( "USERIAL_Ioctl: Received USERIAL_OP_BAUD_WR on port: %d, ioctl baud%i\n", port, p_data->baud);
        linux_cb.port = port;
        userial_change_rate(p_data->baud);
        break;

    default:
        break;
    }

    return;
}


/*******************************************************************************
**
** Function         USERIAL_SetPowerOffDelays
**
** Description      Set power off delays used during USERIAL_Close().  The
**                  values in the conf. file setting override these if set.
**
** Returns          None.
**
*******************************************************************************/
UDRV_API void USERIAL_SetPowerOffDelays(int pre_poweroff_delay, int post_poweroff_delay)
{
    gPrePowerOffDelay = pre_poweroff_delay;
    gPostPowerOffDelay = post_poweroff_delay;
}

/*******************************************************************************
**
** Function           USERIAL_Close
**
** Description        Close a serial port
**
** Output Parameter   None
**
** Returns            Nothing
**
*******************************************************************************/
UDRV_API void    USERIAL_Close(tUSERIAL_PORT port)
{
    pthread_attr_t attr;
    pthread_t      close_thread;

    ALOGD ("%s: enter", __FUNCTION__);
    // check to see if thread is already running
    if (pthread_mutex_trylock(&close_thread_mutex) == 0)
    {
        // mutex aquired so thread is not running
        is_close_thread_is_waiting = TRUE;
        pthread_mutex_unlock(&close_thread_mutex);

        // close transport in a new thread so we don't block the caller
        // make thread detached, no other thread will join
        pthread_attr_init(&attr);
        pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
        pthread_create( &close_thread, &attr, (void *)userial_close_thread,(void*)port);
        pthread_attr_destroy(&attr);
    }
    else
    {
        // mutex not aquired to thread is already running
        ALOGD( "USERIAL_Close(): already closing \n");
    }
    ALOGD ("%s: exit", __FUNCTION__);
}


/*******************************************************************************
**
** Function         userial_close_thread
**
** Description      Thread to close USERIAL
**
** Returns          None.
**
*******************************************************************************/
void userial_close_thread(UINT32 params)
{
    tUSERIAL_PORT port = (tUSERIAL_PORT )params;
    BT_HDR                  *p_buf = NULL;
    int result;

    ALOGD( "%s: closing transport (%d)\n", __FUNCTION__, linux_cb.sock);
    pthread_mutex_lock(&close_thread_mutex);
    is_close_thread_is_waiting = FALSE;

    if (linux_cb.sock <= 0)
    {
        ALOGD( "%s: already closed (%d)\n", __FUNCTION__, linux_cb.sock);
        pthread_mutex_unlock(&close_thread_mutex);
        return;
    }

    send_wakeup_signal();
    result = pthread_join( worker_thread1, NULL );
    if ( result < 0 )
        ALOGE( "%s: pthread_join() FAILED: result: %d", __FUNCTION__, result );
    else
        ALOGD( "%s: pthread_join() joined: result: %d", __FUNCTION__, result );

    if (linux_cb.sock_power_control > 0)
    {
        result = ioctl(linux_cb.sock_power_control, BCMNFC_WAKE_CTL, sleep_state());
        ALOGD("%s: Delay %dms before turning off the chip", __FUNCTION__, gPrePowerOffDelay);
        GKI_delay(gPrePowerOffDelay);
        result = ioctl(linux_cb.sock_power_control, BCMNFC_POWER_CTL, 0);
        ALOGD("%s: Delay %dms after turning off the chip", __FUNCTION__, gPostPowerOffDelay);
        GKI_delay(gPostPowerOffDelay);
    }
    result = close(linux_cb.sock);
    if (result<0)
        ALOGD("%s: close return %d", __FUNCTION__, result);

    if (linux_cb.sock_power_control > 0 && linux_cb.sock_power_control != linux_cb.sock)
    result = close(linux_cb.sock_power_control);
    if (result<0)
        ALOGD("%s: close return %d", __FUNCTION__, result);

    linux_cb.sock_power_control = -1;
    linux_cb.sock = -1;

    close_signal_fds();
    pthread_mutex_unlock(&close_thread_mutex);
    ALOGD("%s: exiting", __FUNCTION__);
}

/*******************************************************************************
**
** Function           USERIAL_Feature
**
** Description        Check whether a feature of the serial API is supported.
**
** Output Parameter   None
**
** Returns            TRUE  if the feature is supported
**                    FALSE if the feature is not supported
**
*******************************************************************************/

UDRV_API BOOLEAN USERIAL_Feature(tUSERIAL_FEATURE feature)
{
    switch (feature)
    {
    case USERIAL_FEAT_PORT_1:
    case USERIAL_FEAT_PORT_2:
    case USERIAL_FEAT_PORT_3:
    case USERIAL_FEAT_PORT_4:

    case USERIAL_FEAT_BAUD_600:
    case USERIAL_FEAT_BAUD_1200:
    case USERIAL_FEAT_BAUD_9600:
    case USERIAL_FEAT_BAUD_19200:
    case USERIAL_FEAT_BAUD_57600:
    case USERIAL_FEAT_BAUD_115200:

    case USERIAL_FEAT_STOPBITS_1:
    case USERIAL_FEAT_STOPBITS_2:

    case USERIAL_FEAT_PARITY_NONE:
    case USERIAL_FEAT_PARITY_EVEN:
    case USERIAL_FEAT_PARITY_ODD:

    case USERIAL_FEAT_DATABITS_5:
    case USERIAL_FEAT_DATABITS_6:
    case USERIAL_FEAT_DATABITS_7:
    case USERIAL_FEAT_DATABITS_8:

    case USERIAL_FEAT_FC_HW:
    case USERIAL_FEAT_BUF_BYTE:

    case USERIAL_FEAT_OP_FLUSH_RX:
    case USERIAL_FEAT_OP_FLUSH_TX:
        return TRUE;
    default:
        return FALSE;
    }

    return FALSE;
}

/*****************************************************************************
**
** Function         UPIO_Set
**
** Description
**      This function sets one or more GPIO devices to the given state.
**      Multiple GPIOs of the same type can be masked together to set more
**      than one GPIO. This function can only be used on types UPIO_LED and
**      UPIO_GENERAL.
**
** Input Parameters:
**      type    The type of device.
**      pio     Indicates the particular GPIOs.
**      state   The desired state.
**
** Output Parameter:
**      None.
**
** Returns:
**      None.
**
*****************************************************************************/
UDRV_API void UPIO_Set(tUPIO_TYPE type, tUPIO pio, tUPIO_STATE new_state)
{
    int     ret;
    if (type == UPIO_GENERAL)
    {
        if (pio == NFC_HAL_LP_NFC_WAKE_GPIO)
        {
            if (new_state == UPIO_ON || new_state == UPIO_OFF)
            {
                if (linux_cb.sock_power_control > 0)
                {
                    ALOGD("%s: ioctl, state=%d", __func__, new_state);
                    ret = ioctl(linux_cb.sock_power_control, BCMNFC_WAKE_CTL, new_state);
                    if (isWake(new_state) && nfc_wake_delay > 0 && new_state != current_nfc_wake_state)
                    {
                        ALOGD("%s: ioctl, old state=%d, insert delay for %d ms", __func__, current_nfc_wake_state, nfc_wake_delay);
                        setWriteDelay(nfc_wake_delay);
                    }
                    current_nfc_wake_state = new_state;
                }
            }
        }
    }
}

/*****************************************************************************
**
** Function         setReadPacketSize
**
** Description
**      This function sets the packetSize to the driver.
**      this enables faster read operation of NCI/HCI responses
**
** Input Parameters:
**      len     number of bytes to read per operation.
**
** Output Parameter:
**      None.
**
** Returns:
**      None.
**
*****************************************************************************/
void setReadPacketSize(int len)
{
    int ret;
    ALOGD("%s: ioctl, len=%d", __func__, len);
    ret = ioctl(linux_cb.sock, BCMNFC_READ_FULL_PACKET, len);
}


UDRV_API BOOLEAN USERIAL_IsClosed()
{
    return (linux_cb.sock == -1) ? TRUE : FALSE;
}

UDRV_API void USERIAL_PowerupDevice(tUSERIAL_PORT port)
{
    int ret = -1;
    unsigned long num = 0;
    unsigned int resetSuccess = 0;
    unsigned int numTries = 0;
    unsigned char spi_negotiation[64];
    int delay = gPowerOnDelay;
    ALOGD("%s: enter", __FUNCTION__);

    if ( GetNumValue ( NAME_READ_MULTI_PACKETS, &num, sizeof ( num ) ) )
        bcmi2cnfc_read_multi_packets = num;

    if (bcmi2cnfc_read_multi_packets > 0)
        ioctl(linux_cb.sock, BCMNFC_READ_MULTI_PACKETS, bcmi2cnfc_read_multi_packets);

    while (!resetSuccess && numTries < NUM_RESET_ATTEMPTS) {
        if (numTries++ > 0) {
            ALOGW("BCM2079x: retrying reset, attempt %d/%d", numTries, NUM_RESET_ATTEMPTS);
        }
        if (linux_cb.sock_power_control > 0)
        {
            current_nfc_wake_state = NFC_WAKE_ASSERTED_ON_POR;
            ioctl(linux_cb.sock_power_control, BCMNFC_WAKE_CTL, NFC_WAKE_ASSERTED_ON_POR);
            ioctl(linux_cb.sock_power_control, BCMNFC_POWER_CTL, 0);
            GKI_delay(10);
            ret = ioctl(linux_cb.sock_power_control, BCMNFC_POWER_CTL, 1);
        }

        ret = GetStrValue ( NAME_SPI_NEGOTIATION, (char*)spi_negotiation, sizeof ( spi_negotiation ) );
        if (ret > 0 && spi_negotiation[0] > 0 && spi_negotiation[0] < sizeof ( spi_negotiation ) - 1)
        {
            int len = spi_negotiation[0];
            /* Wake control is not available: Start SPI negotiation*/
            USERIAL_Write(port, &spi_negotiation[1], len);
            USERIAL_Read(port, spi_negotiation, sizeof ( spi_negotiation ));
        }

        if ( GetNumValue ( NAME_CLIENT_ADDRESS, &num, sizeof ( num ) ) )
            bcmi2cnfc_client_addr = num & 0xFF;
        if (bcmi2cnfc_client_addr != 0 &&
            0x07 < bcmi2cnfc_client_addr &&
            bcmi2cnfc_client_addr < 0x78)
        {
            /* Delay needed after turning on chip */
            GKI_delay(delay);
            ALOGD( "Change client address to %x\n", bcmi2cnfc_client_addr);
            ret = change_client_addr(bcmi2cnfc_client_addr);
            if (!ret) {
                resetSuccess = 1;
                linux_cb.client_device_address = bcmi2cnfc_client_addr;
                /* Delay long enough for address change */
                delay = 200;
            }
        } else {
            resetSuccess = 1;
        }
    }

    if (!resetSuccess) {
        ALOGE("BCM2079x: failed to initialize NFC controller");
    }

    GKI_delay(delay);
    ALOGD("%s: exit", __FUNCTION__);
}

#define DEFAULT_CLIENT_ADDRESS 0x77
#define ALIAS_CLIENT_ADDRESS   0x79
static int change_client_addr(int addr)
{
    int ret;
    int i;
    char addr_data[] = {
        0xFA, 0xF2, 0x00, 0x00, 0x00, 0x38, 0x00, 0x00, 0x00, 0x2A
    };
    int size = sizeof(addr_data) - 1;

    addr_data[5] = addr & 0xFF;

    /* set the checksum */
    ret = 0;
    for (i = 1; i < size; ++i)
        ret += addr_data[i];
    addr_data[size] = (ret & 0xFF);
    ALOGD( "change_client_addr() change addr from 0x%x to 0x%x\n", DEFAULT_CLIENT_ADDRESS, addr);
    /* ignore the return code from IOCTL */
    /* always revert back to the default client address */
    ioctl(linux_cb.sock, BCMNFC_SET_CLIENT_ADDR, DEFAULT_CLIENT_ADDRESS);
    /* Send address change command (skipping first byte) */
    ret = write(linux_cb.sock, &addr_data[1], size);

    /* If it fails, it is likely a B3 we are talking to */
    if (ret != size) {
        ALOGD( "change_client_addr() change addr to 0x%x by setting BSP address to 0x%x\n", addr, ALIAS_CLIENT_ADDRESS);
        /* MACO changed to support B3 with old kernel driver */
        ret = ioctl(linux_cb.sock, BCMNFC_CHANGE_ADDR, addr);
        return ret;
    }

    if (ret == size) {
        ALOGD( "change_client_addr() set client address 0x%x to client driver\n", addr);
        ret = ioctl(linux_cb.sock, BCMNFC_SET_CLIENT_ADDR, addr);
    }
    else {
        ret = -EIO;
    }
    return ret;
}
