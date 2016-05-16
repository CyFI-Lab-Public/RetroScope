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
#ifndef __CONFIG_H
#define __CONFIG_H

#ifdef __cplusplus
extern "C"
{
#endif

int GetStrValue(const char* name, char* p_value, unsigned long len);
int GetNumValue(const char* name, void* p_value, unsigned long len);

#ifdef __cplusplus
};
#endif

#define NAME_POLLING_TECH_MASK          "POLLING_TECH_MASK"
#define NAME_REGISTER_VIRTUAL_SE        "REGISTER_VIRTUAL_SE"
#define NAME_APPL_TRACE_LEVEL           "APPL_TRACE_LEVEL"
#define NAME_USE_RAW_NCI_TRACE          "USE_RAW_NCI_TRACE"
#define NAME_LOGCAT_FILTER              "LOGCAT_FILTER"
#define NAME_LPTD_CFG                   "LPTD_CFG"
#define NAME_SCREEN_OFF_POWER_STATE     "SCREEN_OFF_POWER_STATE"
#define NAME_PREINIT_DSP_CFG            "PREINIT_DSP_CFG"
#define NAME_DTA_START_CFG              "DTA_START_CFG"
#define NAME_TRANSPORT_DRIVER           "TRANSPORT_DRIVER"
#define NAME_POWER_CONTROL_DRIVER       "POWER_CONTROL_DRIVER"
#define NAME_PROTOCOL_TRACE_LEVEL       "PROTOCOL_TRACE_LEVEL"
#define NAME_UART_PORT                  "UART_PORT"
#define NAME_UART_BAUD                  "UART_BAUD"
#define NAME_UART_PARITY                "UART_PARITY"
#define NAME_UART_STOPBITS              "UART_STOPBITS"
#define NAME_UART_DATABITS              "UART_DATABITS"
#define NAME_CLIENT_ADDRESS             "BCMI2CNFC_ADDRESS"
#define NAME_NFA_DM_START_UP_CFG        "NFA_DM_START_UP_CFG"
#define NAME_NFA_DM_CFG                 "NFA_DM_CFG"
#define NAME_NFA_DM_LP_CFG              "NFA_DM_LP_CFG"
#define NAME_LOW_SPEED_TRANSPORT        "LOW_SPEED_TRANSPORT"
#define NAME_NFC_WAKE_DELAY             "NFC_WAKE_DELAY"
#define NAME_NFC_WRITE_DELAY            "NFC_WRITE_DELAY"
#define NAME_PERF_MEASURE_FREQ          "REPORT_PERFORMANCE_MEASURE"
#define NAME_READ_MULTI_PACKETS         "READ_MULTIPLE_PACKETS"
#define NAME_POWER_ON_DELAY             "POWER_ON_DELAY"
#define NAME_PRE_POWER_OFF_DELAY        "PRE_POWER_OFF_DELAY"
#define NAME_POST_POWER_OFF_DELAY       "POST_POWER_OFF_DELAY"
#define NAME_CE3_PRE_POWER_OFF_DELAY    "CE3_PRE_POWER_OFF_DELAY"
#define NAME_NFA_STORAGE                "NFA_STORAGE"
#define NAME_NFA_DM_START_UP_VSC_CFG    "NFA_DM_START_UP_VSC_CFG"
#define NAME_NFA_DTA_START_UP_VSC_CFG   "NFA_DTA_START_UP_VSC_CFG"
#define NAME_UICC_LISTEN_TECH_MASK      "UICC_LISTEN_TECH_MASK"
#define NAME_SNOOZE_MODE_CFG            "SNOOZE_MODE_CFG"
#define NAME_NFA_DM_DISC_DURATION_POLL  "NFA_DM_DISC_DURATION_POLL"
#define NAME_SPD_DEBUG                  "SPD_DEBUG"
#define NAME_SPD_MAXRETRYCOUNT          "SPD_MAX_RETRY_COUNT"
#define NAME_SPI_NEGOTIATION            "SPI_NEGOTIATION"
#define NAME_AID_FOR_EMPTY_SELECT       "AID_FOR_EMPTY_SELECT"
#define NAME_PRESERVE_STORAGE           "PRESERVE_STORAGE"
#define NAME_NFA_MAX_EE_SUPPORTED       "NFA_MAX_EE_SUPPORTED"
#define NAME_NFCC_ENABLE_TIMEOUT        "NFCC_ENABLE_TIMEOUT"
#define NAME_NFA_DM_PRE_DISCOVERY_CFG   "NFA_DM_PRE_DISCOVERY_CFG"
#define NAME_POLL_FREQUENCY             "POLL_FREQUENCY"

#define                     LPTD_PARAM_LEN (40)

// default configuration
#define default_transport       "/dev/bcm2079x"
#define default_storage_location "/data/nfc"

struct tUART_CONFIG {
    int     m_iBaudrate;            // 115200
    int     m_iDatabits;            // 8
    int     m_iParity;              // 0 - none, 1 = odd, 2 = even
    int     m_iStopbits;
};

extern struct tUART_CONFIG  uartConfig;
#define MAX_CHIPID_LEN  (16)
void    readOptionalConfig(const char* option);

/* Snooze mode configuration structure */
typedef struct
{
    unsigned char   snooze_mode;            /* Snooze Mode */
    unsigned char   idle_threshold_dh;      /* Idle Threshold Host */
    unsigned char   idle_threshold_nfcc;    /* Idle Threshold NFCC   */
    unsigned char   nfc_wake_active_mode;   /* NFC_LP_ACTIVE_LOW or NFC_LP_ACTIVE_HIGH */
    unsigned char   dh_wake_active_mode;    /* NFC_LP_ACTIVE_LOW or NFC_LP_ACTIVE_HIGH */
} tSNOOZE_MODE_CONFIG;
#endif
