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

#ifndef FS_H
#define FS_H

#include "fatblock.h"
#include "fat.h"

struct fs {
	uint16_t cluster_size;

	cluster_t num_clusters;
	cluster_t next_cluster;
	struct extent *extents;

	struct fat_boot_sector boot;
	struct extent boot_extent;
	struct extent backup_boot_extent;

	struct fat_info_sector info;
	struct extent info_extent;

	struct extent fat_extent;
	fat_entry_t *fat;
	offset_t fat_size;

	offset_t data_offset;
};

int fs_alloc_extent(struct fs *fs, struct extent *extent,
                    offset_t len, int type, cluster_t *first_cluster_out);
struct extent *fs_find_extent(struct fs *fs, offset_t start, offset_t len, struct extent *last,
                              offset_t *r_start_out, offset_t *e_start_out, offset_t *len_out);
int fs_init(struct fs *fs, uint16_t cluster_size, offset_t data_size, offset_t *total_size_out);
void fs_set_rootdir_start(struct fs *fs, cluster_t rootdir_start);
void fs_update_free_clusters(struct fs *fs);

#endif
