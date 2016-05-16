#ifndef __PLT_H
#define __PLT_H

#ifdef ANDROID
#define CURRENT_NVS_NAME    "/system/etc/firmware/ti-connectivity/wl1271-nvs.bin"
#else
#define CURRENT_NVS_NAME    "/lib/firmware/ti-connectivity/wl1271-nvs.bin"
#endif
#define NEW_NVS_NAME        "./new-nvs.bin"
#define NVS_FILE_SIZE_127X    0x390
#define NVS_FILE_SIZE_128X    0x459

/* NVS definition start here */

#define NVS_TX_TYPE_INDEX               0

#define START_TYPE_INDEX_IN_TLV        0
#define TLV_TYPE_LENGTH                 1
#define START_LENGTH_INDEX              \
(START_TYPE_INDEX_IN_TLV + TLV_TYPE_LENGTH) /* 1 */
#define TLV_LENGTH_LENGTH               2
#define START_PARAM_INDEX               \
(START_LENGTH_INDEX + TLV_LENGTH_LENGTH) /* 3 */

#define NVS_VERSION_1                   1
#define NVS_VERSION_2                   2

#define NVS_MAC_FIRST_LENGTH_INDEX      0
#define NVS_MAC_FIRST_LENGHT_VALUE      1

#define NVS_MAC_L_ADDRESS_INDEX         \
((NVS_MAC_FIRST_LENGTH_INDEX) + 1) /* 1*/
#define NVS_MAC_L_ADDRESS_LENGTH        2

#define NVS_MAC_L_VALUE_INDEX \
((NVS_MAC_L_ADDRESS_INDEX) + (NVS_MAC_L_ADDRESS_LENGTH)) /* 3 */

#define NVS_MAC_L_VALUE_LENGTH          4

#define NVS_MAC_SECONDE_LENGTH_INDEX    \
((NVS_MAC_L_VALUE_INDEX) + 4) /* 7 */
#define NVS_MAC_SECONDE_LENGHT_VALUE    1

#define NVS_MAC_H_ADDRESS_INDEX         \
((NVS_MAC_SECONDE_LENGTH_INDEX) + 1) /* 8*/
#define NVS_MAC_H_ADDRESS_LENGTH        2

#define NVS_MAC_H_VALUE_INDEX           \
((NVS_MAC_H_ADDRESS_INDEX) + (NVS_MAC_H_ADDRESS_LENGTH)) /* 10 */
#define NVS_MAC_H_VALUE_LENGTH          4

#define NVS_END_BURST_TRANSACTION_INDEX         \
((NVS_MAC_H_VALUE_INDEX) + (NVS_MAC_H_VALUE_LENGTH))    /* 14 */
#define NVS_END_BURST_TRANSACTION_VALUE         0
#define NVS_END_BURST_TRANSACTION_LENGTH        7

#define NVS_ALING_TLV_START_ADDRESS_INDEX       \
((NVS_END_BURST_TRANSACTION_INDEX) + \
(NVS_END_BURST_TRANSACTION_LENGTH)) /* 21 */
#define NVS_ALING_TLV_START_ADDRESS_VALUE       0
#define NVS_ALING_TLV_START_ADDRESS_LENGTH      3


/* NVS pre TLV length */
#define NVS_PRE_PARAMETERS_LENGTH               \
((NVS_ALING_TLV_START_ADDRESS_INDEX) + \
(NVS_ALING_TLV_START_ADDRESS_LENGTH)) /* 24 */

/* NVS P2G table */
#define NVS_TX_P2G_TABLE_LENGTH                 \
((NUMBER_OF_SUB_BANDS_E) * 1 /* byte */) /* 8 */

/* NVS PPA table */
#define NVS_TX_PPA_STEPS_TABLE_LENGTH   \
((NUMBER_OF_SUB_BANDS_E) * ((TXPWR_CFG0__VGA_STEP__NUMBER_OF_STEPS_E) \
- 1) * 1 /* byte */)  /* 32 */

/* NVS version 1 TX PD curve table length */
#define NVS_TX_PD_TABLE_LENGTH_NVS_V1   (1 /* byte to set size of table */ + \
((NUMBER_OF_SUB_BANDS_E) * (2 /* 1 byte offset, 1 byte low range */ + \
2 /* first index in table */ + (((SIZE_OF_POWER_DETECTOR_TABLE) - 1) * \
1 /* 1 byte */)))) /* 233 */

/* NVS version 2 TX PD curve table length */
#define NVS_TX_PD_TABLE_LENGTH_NVS_V2   \
((NUMBER_OF_SUB_BANDS_E) * (12 /* 12index of one byte -2 dBm - 9dBm */ +\
28 /* 14 indexes of 2 byte -3dBm, 10dBm - 22 dBm */)) /* 320 */

/* NVS version 1 TX parameters Length */
#define NVS_TX_PARAM_LENGTH_NVS_V1      \
((NVS_TX_P2G_TABLE_LENGTH) + (NVS_TX_PPA_STEPS_TABLE_LENGTH) +\
(NVS_TX_PD_TABLE_LENGTH_NVS_V1)) /* 273 */

/* NVS version 2 TX parameters Length */
#define NVS_TX_PARAM_LENGTH_NVS_V2       \
((NVS_TX_P2G_TABLE_LENGTH) + (NVS_TX_PPA_STEPS_TABLE_LENGTH) +\
(NVS_TX_PD_TABLE_LENGTH_NVS_V2) +\
(NUMBER_OF_RADIO_CHANNEL_INDEXS_E /* for Per Channel power Gain Offset tabl */))

/* NVS TX version */
/* #define NVS_TX_PARAM_LENGTH     NVS_TX_PARAM_LENGTH_NVS_V2 */
#define NVS_TX_PARAM_LENGTH     0x199

/* NVS RX version */
#define NVS_RX_PARAM_LENGTH    NUMBER_OF_RX_BIP_EFUSE_PARAMETERS_E /* 19 */

/* NVS version parameter length */
#define NVS_VERSION_PARAMETER_LENGTH    3

/* NVS max length */
/* original ((NVS_TOTAL_LENGTH) + 4 - ((NVS_TOTAL_LENGTH) % 4)) */
#define NVS_TOTAL_LENGTH    500

/* TLV max length */
#define  MAX_TLV_LENGTH                                 NVS_TOTAL_LENGTH

#define  MAX_NVS_VERSION_LENGTH                 12

enum wl1271_tm_commands {
    WL1271_TM_CMD_UNSPEC,
    WL1271_TM_CMD_TEST,
    WL1271_TM_CMD_INTERROGATE,
    WL1271_TM_CMD_CONFIGURE,
    WL1271_TM_CMD_NVS_PUSH,
    WL1271_TM_CMD_SET_PLT_MODE,
    __WL1271_TM_CMD_AFTER_LAST
};

enum wl1271_tm_attrs {
    WL1271_TM_ATTR_UNSPEC,
    WL1271_TM_ATTR_CMD_ID,
    WL1271_TM_ATTR_ANSWER,
    WL1271_TM_ATTR_DATA,
    WL1271_TM_ATTR_IE_ID,
    WL1271_TM_ATTR_PLT_MODE,
    __WL1271_TM_ATTR_AFTER_LAST
};

#define WL1271_TM_ATTR_MAX (__WL1271_TM_ATTR_AFTER_LAST - 1)

enum wl1271_test_cmds {
    TEST_CMD_PD_BUFFER_CAL = 0x1, /* TX PLT */
    TEST_CMD_P2G_CAL,             /* TX BiP */
    TEST_CMD_RX_PLT_ENTER,
    TEST_CMD_RX_PLT_CAL,          /* RSSI Cal */
    TEST_CMD_RX_PLT_EXIT,
    TEST_CMD_RX_PLT_GET,
    TEST_CMD_FCC,                 /* Continuous TX */
    TEST_CMD_TELEC,  /* Carrier wave in a specific channel and band */
    TEST_CMD_STOP_TX,             /* Stop FCC or TELEC */
    TEST_CMD_PLT_TEMPLATE,        /* define Template for TX */
    TEST_CMD_PLT_GAIN_ADJUST,
    TEST_CMD_PLT_GAIN_GET,
    TEST_CMD_CHANNEL_TUNE,
    TEST_CMD_FREE_RUN_RSSI,        /* Free running RSSI measurement */
    TEST_CMD_DEBUG,     /* test command for debug using the struct: */
    TEST_CMD_CLPC_COMMANDS,
    RESERVED_4,
    TEST_CMD_RX_STAT_STOP,
    TEST_CMD_RX_STAT_START,
    TEST_CMD_RX_STAT_RESET,
    TEST_CMD_RX_STAT_GET,
    TEST_CMD_LOOPBACK_START,       /* for FW Test Debug */
    TEST_CMD_LOOPBACK_STOP,        /* for FW Test Debug */
    TEST_CMD_GET_FW_VERSIONS,
    TEST_CMD_INI_FILE_RADIO_PARAM,
    TEST_CMD_RUN_CALIBRATION_TYPE,
    TEST_CMD_TX_GAIN_ADJUST,
    TEST_CMD_UPDATE_PD_BUFFER_ERRORS,
    TEST_CMD_UPDATE_PD_REFERENCE_POINT,
    TEST_CMD_INI_FILE_GENERAL_PARAM,
    TEST_CMD_SET_EFUSE,
    TEST_CMD_GET_EFUSE,
    TEST_CMD_TEST_TONE,
    TEST_CMD_POWER_MODE,
    TEST_CMD_SMART_REFLEX,
    TEST_CMD_CHANNEL_RESPONSE,
    TEST_CMD_DCO_ITRIM_FEATURE,
    MAX_TEST_CMD_ID = 0xFF
};

struct wl1271_cmd_header {
    __u16 id;
    __u16 status;
    /* payload */
    unsigned char data[0];
} __attribute__((packed));

struct wl1271_cmd_test_header {
    unsigned char id;
    unsigned char padding[3];
} __attribute__((packed));

struct wl1271_cmd_cal_channel_tune {
    struct wl1271_cmd_header header;

    struct wl1271_cmd_test_header test;

    unsigned char band;
    unsigned char channel;

    __le16 radio_status;
} __attribute__((packed));

struct wl1271_cmd_cal_update_ref_point {
    struct wl1271_cmd_header header;

    struct wl1271_cmd_test_header test;

    __le32 ref_power;
    __le32 ref_detector;
    unsigned char  sub_band;
    unsigned char  padding[3];
} __attribute__((packed));

struct wl1271_cmd_cal_tx_tone {
    struct wl1271_cmd_header header;

    struct wl1271_cmd_test_header test;

    __le32 power;
    __le32 tone_type;
} __attribute__((packed));

struct wl1271_cmd_cal_p2g {
    struct wl1271_cmd_header header;

    struct wl1271_cmd_test_header test;

    __le32 ver;
    __le16 len;
    unsigned char  buf[MAX_TLV_LENGTH];
    unsigned char  type;
    unsigned char  padding;

    __le16 radio_status;

    unsigned char  sub_band_mask;
    unsigned char  padding2;
} __attribute__((packed));

#define MAC_ADDR_LEN  6

struct wl1271_cmd_pkt_params {
    struct wl1271_cmd_header header;

    struct wl1271_cmd_test_header test;

    __le16 radio_status;
    unsigned char padding[2];
    __le32 delay;
    __le32 rate;
    __le16 size;
    __le16 amount;
    __le32 power;
    __le16 seed;
    unsigned char pkt_mode;
    unsigned char dcf_enable;
    unsigned char g_interval;
    unsigned char preamble;
    unsigned char type;
    unsigned char scramble;
    unsigned char clpc_enable;
    unsigned char seq_nbr_mode;
    unsigned char src_mac[MAC_ADDR_LEN];
    unsigned char dst_mac[MAC_ADDR_LEN];
    unsigned char padding1[2];
} __attribute__((packed));

struct wl1271_rx_path_statcs {
    __le32 nbr_rx_valid_pkts;
    __le32 nbr_rx_fcs_err_pkts;
    __le32 nbr_rx_plcp_err_pkts;
    __le32 seq_nbr_miss_cnt; /* For PER calculation */
    __le16 ave_snr; /* average SNR */
    __le16 ave_rssi; /* average RSSI */
    __le16 ave_evm;
    unsigned char padding[2];
} __attribute__((packed));

struct wl1271_rx_pkt_statcs {
    __le32 length;
    __le32 evm;
    __le32 rssi;
    __le16 freq_delta;
    __le16 flags;
    char type;
    unsigned char rate;
    unsigned char noise;
    unsigned char agc_gain;
    unsigned char padding[2];
} __attribute__((packed));

#define RX_STAT_PACKETS_PER_MESSAGE        (20)

struct wl1271_radio_rx_statcs {
    struct wl1271_cmd_header header;

    struct wl1271_cmd_test_header test;

    struct wl1271_rx_path_statcs rx_path_statcs;
    __le32 base_pkt_id;
    __le32 nbr_pkts; /* input/output: number of following packets */
    __le32 nbr_miss_pkts;
    __le16 radio_status;
    unsigned char padding[2];
} __attribute__((packed));

enum wl1271_nvs_type {
    eNVS_VERSION = 0xaa,
    eNVS_RADIO_TX_PARAMETERS = 1,
    eNVS_RADIO_RX_PARAMETERS = 2,
    eNVS_RADIO_INI = 16,
    eNVS_NON_FILE = 0xFE,
    eTLV_LAST = 0xFF /* last TLV type */
};

#define DEFAULT_EFUSE_VALUE            (0)

enum wl1271_nvs_type_info {
    eFIRST_RADIO_TYPE_PARAMETERS_INFO,
    eNVS_RADIO_TX_TYPE_PARAMETERS_INFO = eFIRST_RADIO_TYPE_PARAMETERS_INFO,
    eNVS_RADIO_RX_TYPE_PARAMETERS_INFO,
    eLAST_RADIO_TYPE_PARAMETERS_INFO = eNVS_RADIO_RX_TYPE_PARAMETERS_INFO,
    UNUSED_RADIO_TYPE_PARAMETERS_INFO,
    eNUMBER_RADIO_TYPE_PARAMETERS_INFO = UNUSED_RADIO_TYPE_PARAMETERS_INFO,
    LAST_RADIO_TYPE_PARAMETERS_INFO =
        (eNUMBER_RADIO_TYPE_PARAMETERS_INFO - 1)
};

enum EFUSE_PARAMETER_TYPE_ENMT {
    EFUSE_FIRST_PARAMETER_E,
    /* RX PARAMETERS */
    EFUSE_FIRST_RX_PARAMETER_E = EFUSE_FIRST_PARAMETER_E,
    RX_BIP_MAX_GAIN_ERROR_BAND_B_E = EFUSE_FIRST_RX_PARAMETER_E,

    RX_BIP_MAX_GAIN_ERROR_J_LOW_MID_E,
    RX_BIP_MAX_GAIN_ERROR_J_HIGH_E,

    RX_BIP_MAX_GAIN_ERROR_5G_1ST_E,
    RX_BIP_MAX_GAIN_ERROR_5G_2ND_E,
    RX_BIP_MAX_GAIN_ERROR_5G_3RD_E,
    RX_BIP_MAX_GAIN_ERROR_5G_4TH_E,

    RX_BIP_LNA_STEP_CORR_BAND_B_4TO3_E,
    RX_BIP_LNA_STEP_CORR_BAND_B_3TO2_E,
    RX_BIP_LNA_STEP_CORR_BAND_B_2TO1_E,
    RX_BIP_LNA_STEP_CORR_BAND_B_1TO0_E,

    RX_BIP_LNA_STEP_CORR_BAND_A_4TO3_E,
    RX_BIP_LNA_STEP_CORR_BAND_A_3TO2_E,
    RX_BIP_LNA_STEP_CORR_BAND_A_2TO1_E,
    RX_BIP_LNA_STEP_CORR_BAND_A_1TO0_E,

    RX_BIP_TA_STEP_CORR_BAND_B_2TO1_E,
    RX_BIP_TA_STEP_CORR_BAND_B_1TO0_E,
    RX_BIP_TA_STEP_CORR_BAND_A_2TO1_E,
    RX_BIP_TA_STEP_CORR_BAND_A_1TO0_E,
    NUMBER_OF_RX_BIP_EFUSE_PARAMETERS_E,

    /* TX PARAMETERS */
    TX_BIP_PD_BUFFER_GAIN_ERROR_E = NUMBER_OF_RX_BIP_EFUSE_PARAMETERS_E,
    TX_BIP_PD_BUFFER_VBIAS_ERROR_E,
    EFUSE_NUMBER_OF_PARAMETERS_E,
    EFUSE_LAST_PARAMETER_E = (EFUSE_NUMBER_OF_PARAMETERS_E - 1)
} EFUSE_PARAMETER_TYPE_ENM;

int get_mac_addr(int ifc_num, unsigned char *mac_addr);

int file_exist(const char *filename);

#endif /* __PLT_H */
