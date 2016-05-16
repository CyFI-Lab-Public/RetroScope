/*
 * This file is part of wl1271
 *
 * Copyright (C) 2010 Nokia Corporation
 *
 * Contact: Luciano Coelho <luciano.coelho@nokia.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 *
 */

#ifndef __INI_H__
#define __INI_H__

#include <linux/limits.h>

#define WL1271_INI_MAX_SMART_REFLEX_PARAM 16

struct wl1271_ini_general_params {
unsigned char ref_clock;
unsigned char settling_time;
unsigned char clk_valid_on_wakeup;
unsigned char dc2dc_mode;
unsigned char dual_mode_select;
unsigned char tx_bip_fem_auto_detect;
unsigned char tx_bip_fem_manufacturer;
unsigned char general_settings;
unsigned char sr_state;
unsigned char srf1[WL1271_INI_MAX_SMART_REFLEX_PARAM];
unsigned char srf2[WL1271_INI_MAX_SMART_REFLEX_PARAM];
unsigned char srf3[WL1271_INI_MAX_SMART_REFLEX_PARAM];
} __attribute__((packed));

#define WL128X_INI_MAX_SETTINGS_PARAM 4

struct wl128x_ini_general_params {
unsigned char ref_clock;
unsigned char settling_time;
unsigned char clk_valid_on_wakeup;
unsigned char tcxo_ref_clock;
unsigned char tcxo_settling_time;
unsigned char tcxo_valid_on_wakeup;
unsigned char tcxo_ldo_voltage;
unsigned char xtal_itrim_val;
unsigned char platform_conf;
unsigned char dual_mode_select;
unsigned char tx_bip_fem_auto_detect;
unsigned char tx_bip_fem_manufacturer;
unsigned char general_settings[WL128X_INI_MAX_SETTINGS_PARAM];
unsigned char sr_state;
unsigned char srf1[WL1271_INI_MAX_SMART_REFLEX_PARAM];
unsigned char srf2[WL1271_INI_MAX_SMART_REFLEX_PARAM];
unsigned char srf3[WL1271_INI_MAX_SMART_REFLEX_PARAM];
} __attribute__((packed));

#define WL1271_INI_RSSI_PROCESS_COMPENS_SIZE 15

struct wl1271_ini_band_params_2 {
unsigned char rx_trace_insertion_loss;
unsigned char tx_trace_loss;
unsigned char
rx_rssi_process_compens[WL1271_INI_RSSI_PROCESS_COMPENS_SIZE];
} __attribute__((packed));

#define WL1271_INI_CHANNEL_COUNT_2 14

struct wl128x_ini_band_params_2 {
unsigned char rx_trace_insertion_loss;
unsigned char tx_trace_loss[WL1271_INI_CHANNEL_COUNT_2];
unsigned char
rx_rssi_process_compens[WL1271_INI_RSSI_PROCESS_COMPENS_SIZE];
} __attribute__((packed));

#define WL1271_INI_RATE_GROUP_COUNT 6

struct wl1271_ini_fem_params_2 {
__le16 tx_bip_ref_pd_voltage;
unsigned char tx_bip_ref_power;
unsigned char tx_bip_ref_offset;
unsigned char
tx_per_rate_pwr_limits_normal[WL1271_INI_RATE_GROUP_COUNT];
unsigned char
tx_per_rate_pwr_limits_degraded[WL1271_INI_RATE_GROUP_COUNT];
unsigned char
tx_per_rate_pwr_limits_extreme[WL1271_INI_RATE_GROUP_COUNT];
unsigned char tx_per_chan_pwr_limits_11b[WL1271_INI_CHANNEL_COUNT_2];
unsigned char tx_per_chan_pwr_limits_ofdm[WL1271_INI_CHANNEL_COUNT_2];
unsigned char tx_pd_vs_rate_offsets[WL1271_INI_RATE_GROUP_COUNT];
unsigned char tx_ibias[WL1271_INI_RATE_GROUP_COUNT];
unsigned char rx_fem_insertion_loss;
unsigned char degraded_low_to_normal_thr;
unsigned char normal_to_degraded_high_thr;
} __attribute__((packed));

#define WL128X_INI_RATE_GROUP_COUNT 7
/* low and high temperatures*/
#define WL128X_INI_PD_VS_TEMPERATURE_RANGES 2

struct wl128x_ini_fem_params_2 {
__le16 tx_bip_ref_pd_voltage;
unsigned char tx_bip_ref_power;
unsigned char tx_bip_ref_offset;
unsigned char
tx_per_rate_pwr_limits_normal [WL128X_INI_RATE_GROUP_COUNT];
unsigned char
tx_per_rate_pwr_limits_degraded [WL128X_INI_RATE_GROUP_COUNT];
unsigned char
tx_per_rate_pwr_limits_extreme [WL128X_INI_RATE_GROUP_COUNT];
unsigned char tx_per_chan_pwr_limits_11b[WL1271_INI_CHANNEL_COUNT_2];
unsigned char tx_per_chan_pwr_limits_ofdm[WL1271_INI_CHANNEL_COUNT_2];
unsigned char tx_pd_vs_rate_offsets[WL128X_INI_RATE_GROUP_COUNT];
unsigned char tx_ibias[WL128X_INI_RATE_GROUP_COUNT + 1];
unsigned char tx_pd_vs_chan_offsets[WL1271_INI_CHANNEL_COUNT_2];
unsigned char tx_pd_vs_temperature[WL128X_INI_PD_VS_TEMPERATURE_RANGES];
unsigned char rx_fem_insertion_loss;
unsigned char degraded_low_to_normal_thr;
unsigned char normal_to_degraded_high_thr;
} __attribute__((packed));

#define WL1271_INI_CHANNEL_COUNT_5 35
#define WL1271_INI_SUB_BAND_COUNT_5 7

struct wl1271_ini_band_params_5 {
unsigned char rx_trace_insertion_loss[WL1271_INI_SUB_BAND_COUNT_5];
unsigned char tx_trace_loss[WL1271_INI_SUB_BAND_COUNT_5];
unsigned char
rx_rssi_process_compens[WL1271_INI_RSSI_PROCESS_COMPENS_SIZE];
} __attribute__((packed));

struct wl128x_ini_band_params_5 {
unsigned char rx_trace_insertion_loss[WL1271_INI_SUB_BAND_COUNT_5];
unsigned char tx_trace_loss[WL1271_INI_CHANNEL_COUNT_5];
unsigned char
rx_rssi_process_compens[WL1271_INI_RSSI_PROCESS_COMPENS_SIZE];
} __attribute__((packed));

struct wl1271_ini_fem_params_5 {
__le16 tx_bip_ref_pd_voltage[WL1271_INI_SUB_BAND_COUNT_5];
unsigned char tx_bip_ref_power[WL1271_INI_SUB_BAND_COUNT_5];
unsigned char tx_bip_ref_offset[WL1271_INI_SUB_BAND_COUNT_5];
unsigned char
tx_per_rate_pwr_limits_normal[WL1271_INI_RATE_GROUP_COUNT];
unsigned char
tx_per_rate_pwr_limits_degraded[WL1271_INI_RATE_GROUP_COUNT];
unsigned char
tx_per_rate_pwr_limits_extreme[WL1271_INI_RATE_GROUP_COUNT];
unsigned char tx_per_chan_pwr_limits_ofdm[WL1271_INI_CHANNEL_COUNT_5];
unsigned char tx_pd_vs_rate_offsets[WL1271_INI_RATE_GROUP_COUNT];
unsigned char tx_ibias[WL1271_INI_RATE_GROUP_COUNT];
unsigned char rx_fem_insertion_loss[WL1271_INI_SUB_BAND_COUNT_5];
unsigned char degraded_low_to_normal_thr;
unsigned char normal_to_degraded_high_thr;
} __attribute__((packed));

struct wl128x_ini_fem_params_5 {
__le16 tx_bip_ref_pd_voltage[WL1271_INI_SUB_BAND_COUNT_5];
unsigned char tx_bip_ref_power[WL1271_INI_SUB_BAND_COUNT_5];
unsigned char tx_bip_ref_offset[WL1271_INI_SUB_BAND_COUNT_5];
unsigned char
tx_per_rate_pwr_limits_normal [WL128X_INI_RATE_GROUP_COUNT];
unsigned char
tx_per_rate_pwr_limits_degraded [WL128X_INI_RATE_GROUP_COUNT];
unsigned char
tx_per_rate_pwr_limits_extreme [WL128X_INI_RATE_GROUP_COUNT];
unsigned char tx_per_chan_pwr_limits_ofdm[WL1271_INI_CHANNEL_COUNT_5];
unsigned char tx_pd_vs_rate_offsets[WL128X_INI_RATE_GROUP_COUNT];
unsigned char tx_ibias[WL128X_INI_RATE_GROUP_COUNT];
unsigned char tx_pd_vs_chan_offsets[WL1271_INI_CHANNEL_COUNT_5];
unsigned char tx_pd_vs_temperature[WL1271_INI_SUB_BAND_COUNT_5 *
WL128X_INI_PD_VS_TEMPERATURE_RANGES];
unsigned char rx_fem_insertion_loss[WL1271_INI_SUB_BAND_COUNT_5];
unsigned char degraded_low_to_normal_thr;
unsigned char normal_to_degraded_high_thr;
} __attribute__((packed));

/* NVS data structure */
#define WL1271_INI_NVS_SECTION_SIZE     468
#define WL1271_INI_FEM_MODULE_COUNT                  2

#define WL1271_INI_LEGACY_NVS_FILE_SIZE              800

struct wl1271_nvs_file {
/* NVS section */
unsigned char nvs[WL1271_INI_NVS_SECTION_SIZE];

/* INI section */
struct wl1271_ini_general_params general_params;
unsigned char padding1;
struct wl1271_ini_band_params_2 stat_radio_params_2;
unsigned char padding2;
struct {
struct wl1271_ini_fem_params_2 params;
unsigned char padding;
} dyn_radio_params_2[WL1271_INI_FEM_MODULE_COUNT];
struct wl1271_ini_band_params_5 stat_radio_params_5;
unsigned char padding3;
struct {
struct wl1271_ini_fem_params_5 params;
unsigned char padding;
} dyn_radio_params_5[WL1271_INI_FEM_MODULE_COUNT];
} __attribute__((packed));

struct wl128x_nvs_file {
/* NVS section */
unsigned char nvs[WL1271_INI_NVS_SECTION_SIZE];

/* INI section */
struct wl128x_ini_general_params general_params;
unsigned char fem_vendor_and_options;
struct wl128x_ini_band_params_2 stat_radio_params_2;
unsigned char padding2;
struct {
struct wl128x_ini_fem_params_2 params;
unsigned char padding;
} dyn_radio_params_2[WL1271_INI_FEM_MODULE_COUNT];
struct wl128x_ini_band_params_5 stat_radio_params_5;
unsigned char padding3;
struct {
struct wl128x_ini_fem_params_5 params;
unsigned char padding;
} dyn_radio_params_5[WL1271_INI_FEM_MODULE_COUNT];
} __attribute__((packed));

struct wl1271_ini {
struct wl1271_ini_general_params general_params;
unsigned char padding1;
struct wl1271_ini_band_params_2 stat_radio_params_2;
unsigned char padding2;
struct {
struct wl1271_ini_fem_params_2 params;
unsigned char padding;
} dyn_radio_params_2[WL1271_INI_FEM_MODULE_COUNT];
struct wl1271_ini_band_params_5 stat_radio_params_5;
unsigned char padding3;
struct {
struct wl1271_ini_fem_params_5 params;
unsigned char padding;
} dyn_radio_params_5[WL1271_INI_FEM_MODULE_COUNT];
} __attribute__((packed));

struct wl128x_ini {
struct wl128x_ini_general_params general_params;
unsigned char fem_vendor_and_options;
struct wl128x_ini_band_params_2 stat_radio_params_2;
unsigned char padding2;
struct {
struct wl128x_ini_fem_params_2 params;
unsigned char padding;
} dyn_radio_params_2[WL1271_INI_FEM_MODULE_COUNT];
struct wl128x_ini_band_params_5 stat_radio_params_5;
unsigned char padding3;
struct {
struct wl128x_ini_fem_params_5 params;
unsigned char padding;
} dyn_radio_params_5[WL1271_INI_FEM_MODULE_COUNT];
} __attribute__((packed));

enum wl1271_ini_section {
UKNOWN_SECTION,
GENERAL_PRMS,
FEM_PRMS,
BAND2_PRMS,
BAND5_PRMS,
FEM0_BAND2_PRMS,
FEM1_BAND2_PRMS,
FEM1_BAND5_PRMS
};

enum wl12xx_arch {
UNKNOWN_ARCH,
WL1271_ARCH,
WL128X_ARCH
};

struct wl12xx_ini {
union {
struct wl1271_ini ini1271;
struct wl128x_ini ini128x;
};
};

#define DUAL_MODE_UNSET    0xff
#define NO_FEM_PARSED      0xff

struct wl12xx_common {
enum wl12xx_arch arch;
unsigned char dual_mode;
unsigned char done_fem; /* Number of FEM already parsed */
struct wl12xx_parse_ops *parse_ops;
struct wl12xx_nvs_ops   *nvs_ops;
struct wl12xx_ini ini;
};

struct wl12xx_parse_ops {
int (*prs_general_prms)(char *l, struct wl12xx_common *cmn,
struct wl12xx_ini *p);
/* int (*prs_fem_prms)(char *l, void *gp); */
int (*prs_band2_prms)(char *l, struct wl12xx_ini *p);
int (*prs_band5_prms)(char *l, struct wl12xx_ini *p);
int (*prs_fem0_band2_prms)(char *l, struct wl12xx_ini *p);
int (*prs_fem1_band2_prms)(char *l, struct wl12xx_ini *p);
int (*prs_fem1_band5_prms)(char *l, struct wl12xx_ini *p);
};

struct wl12xx_nvs_ops {
int (*nvs_fill_radio_prms)(int fd, struct wl12xx_ini *p, char *buf);
int (*nvs_set_autofem)(int fd, char *buf, unsigned char val);
int (*nvs_set_fem_manuf)(int fd, char *buf, unsigned char val);
};

int nvs_get_arch(int file_size, struct wl12xx_common *cmn);

int read_ini(const char *filename, struct wl12xx_common *cmn);

#endif
