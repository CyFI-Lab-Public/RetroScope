/*
 * Copyright (C) 2010 The Android Open Source Project
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

#ifndef FAT_H
#define FAT_H

#include <stdint.h>

#include "fatblock.h"

typedef uint32_t cluster_t;
typedef uint64_t sector_t;
typedef cluster_t fat_entry_t;

struct fat_boot_sector {
	uint8_t jump[3];
	char name[8];
	uint16_t sector_size;
	uint8_t sectors_per_cluster;
	uint16_t reserved_sectors;
	uint8_t fats;
	uint16_t rootdir_size;
	uint16_t sectors16;
	uint8_t media_desc;
	uint16_t fat_sectors16;
	uint16_t sectors_per_track;
	uint16_t heads;
	uint32_t hidden_sectors;
	uint32_t sectors32;
	uint32_t fat_sectors32;
	uint16_t fat_flags;
	uint16_t version;
	cluster_t rootdir_start;
	uint16_t fs_info_sector;
	uint16_t backup_boot_sector;
	uint8_t reserved1[12];
	uint8_t phys_drive;
	uint8_t reserved2;
	uint8_t ext_boot_sig;
	uint32_t serial;
	char vol_label[11];
	char type[8];
	char boot_code[420];
	uint8_t boot_sig[2];
} __attribute__((__packed__));

#define FAT_MEDIA_DESC_FIXED 0xF8

#define FAT_PHYS_DRIVE_REMOVABLE 0x00
#define FAT_PHYS_DRIVE_FIXED     0x80

#define FAT_EXT_BOOT_SIG 0x29

extern const char FAT_BOOT_SIG[2];

extern const char FAT_INFO_SIG1[4];
extern const char FAT_INFO_SIG2[4];
#define FAT_INFO_SIG3 FAT_BOOT_SIG

struct fat_info_sector {
	char info_sig1[4];
	char reserved1[480];
	char info_sig2[4];
	cluster_t free_clusters;
	cluster_t last_cluster;
	char reserved2[14];
	char info_sig3[2];
} __attribute__((__packed__));

struct fat_bootinfo {
	struct fat_boot_sector boot;
	struct fat_info_sector info;
} __attribute__((__packed__));

struct fat_dirent {
	char name[11];
	uint8_t attr;
	uint8_t reserved;
	uint8_t ctime_ms;
	uint16_t ctime;
	uint16_t cdate;
	uint16_t adate;
	uint16_t first_cluster_hi;
	uint16_t mtime;
	uint16_t mdate;
	uint16_t first_cluster_lo;
	uint32_t size;
} __attribute__((__packed__));

#define FAT_ATTR_READONLY 0x01
#define FAT_ATTR_HIDDEN   0x02
#define FAT_ATTR_SYSTEM   0x04
#define FAT_ATTR_VOLLABEL 0x08
#define FAT_ATTR_SUBDIR   0x10
#define FAT_ATTR_ARCHIVE  0x20
#define FAT_ATTR_DEVICE   0x40

#define FAT_ENTRY_FREE  0x00000000
#define FAT_ENTRY_BAD   0x0FFFFFF7
#define FAT_ENTRY_EOC   0x0FFFFFF8

#define FAT_SECTOR_SIZE 512
#define FAT_CLUSTER_ZERO 2
#define FAT_ENTRIES_PER_SECTOR ((SECTOR_SIZE) / (sizeof(fat_entry_t)))

void fat_dirent_set_first_cluster(struct fat_dirent *de, cluster_t cluster);
void fat_dirent_set(struct fat_dirent *de,
                    char *name, uint8_t attr,
                    cluster_t first_cluster, uint32_t size);

#endif
