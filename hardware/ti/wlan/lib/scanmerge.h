/*
 * Copyright (C) 2008 The Android Open Source Project
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
/*-------------------------------------------------------------------*/
#ifndef _SCANMERGE_H_
#define _SCANMERGE_H_

#include "common.h"
#include "driver.h"
#include "driver_ti.h"

#define SCAN_MERGE_COUNT        4

typedef
#ifdef WPA_SUPPLICANT_VER_0_6_X
    struct wpa_scan_res
#else
    struct wpa_scan_result
#endif
scan_result_t;

typedef struct {
    u8 ssid[MAX_SSID_LEN];
    size_t ssid_len;
} scan_ssid_t;

typedef struct SCANMERGE_STRUCT {
    unsigned long count;
    scan_result_t scanres;
} scan_merge_t;

void scan_init( struct wpa_driver_ti_data *mydrv );
void scan_exit( struct wpa_driver_ti_data *mydrv );
unsigned long scan_count( struct wpa_driver_ti_data *mydrv );
scan_ssid_t *scan_get_ssid( scan_result_t *res_ptr );
#ifdef WPA_SUPPLICANT_VER_0_6_X
unsigned int scan_merge( struct wpa_driver_ti_data *mydrv,
                         scan_result_t **results, int force_flag,
                         unsigned int number_items, unsigned int max_size );
#else
unsigned int scan_merge( struct wpa_driver_ti_data *mydrv,
                         scan_result_t *results, int force_flag,
                         unsigned int number_items, unsigned int max_size );
#endif
scan_result_t *scan_get_by_bssid( struct wpa_driver_ti_data *mydrv, u8 *bssid );
#endif
