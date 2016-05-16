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

#include <assert.h>
#include <string.h>
#include <endian.h>

#include "fat.h"

const char FAT_BOOT_SIG[] = { 0x55, 0xAA };
const char FAT_INFO_SIG1[4] = { 'R', 'R', 'a', 'A' };
const char FAT_INFO_SIG2[4] = { 'r', 'r', 'A', 'a' };

void fat_dirent_set_first_cluster(struct fat_dirent *de, cluster_t cluster)
{
	assert(de);

	de->first_cluster_hi = htole16((cluster >> 16) & 0xffff);
	de->first_cluster_lo = htole16((cluster >>  0) & 0xffff);
}

void fat_dirent_set(struct fat_dirent *de,
                    char *name, uint8_t attr,
                    cluster_t first_cluster, uint32_t size)
{
	assert(de);
	assert(name);

	memset(de, 0, sizeof(*de));

	memcpy(de->name, name, 11);
	de->attr = attr;
	fat_dirent_set_first_cluster(de, first_cluster);
	de->size = htole32(size);
}
