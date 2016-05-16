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

#include "fatblock.h"
#include "fat.h"
#include "fs.h"
#include "utils.h"

#define DEFAULT_SECTOR_SIZE 512

static void fs_add_extent(struct fs *fs, struct extent *extent,
                          offset_t start, offset_t len, int type)
{
	assert(fs);
	assert(extent);

	extent->start = start;
	extent->len = len;
	extent->type = type;

	extent->next = fs->extents;
	fs->extents = extent;
}

struct extent *fs_find_extent(struct fs *fs, offset_t start, offset_t len,
                              struct extent *last,
                              offset_t *r_start_out,
                              offset_t *e_start_out,
                              offset_t *len_out)
{
	struct extent *e;
	offset_t end;
	offset_t e_start, e_end, e_len, e_rel_start, r_rel_start, rel_len;

	assert(fs);

	end = start + len;

	e = last ? last->next : fs->extents;
	for (; e; e = e->next) {
		e_start = e->start;
		e_len = e->len;
		e_end = e_start + e_len;

		if (start >= e_end)
			continue;

		if (end <= e_start)
			continue;

		if (e_start <= start) {
			r_rel_start = 0;
			e_rel_start = start - e_start;
			if (end <= e_end)
				rel_len = len;
			else
				rel_len = e_end - start;
		} else {
			e_rel_start = 0;
			r_rel_start = e_start - start;
			if (e_end <= end)
				rel_len = e_len;
			else
				rel_len = end - e_start;
		}

		assert(e_rel_start < e_len);
		assert(e_rel_start + rel_len <= e_len);
		assert(r_rel_start < len);
		assert(r_rel_start + rel_len <= len);

		if (r_start_out)
			*r_start_out = r_rel_start;
		if (e_start_out)
			*e_start_out = e_rel_start;
		if (len_out)
			*len_out = rel_len;

		return e;
	}

	return NULL;
}

static void fs_set_fat(struct fs *fs, cluster_t cluster, fat_entry_t entry)
{
	assert(fs);

	fs->fat[cluster] = htole32(entry);
}

int fs_alloc_extent(struct fs *fs, struct extent *extent,
                    offset_t len, int type, cluster_t *first_cluster_out)
{
	assert(fs);
	assert(extent);

	cluster_t clusters_needed, start;
	cluster_t i;

	if (len == 0) {
		extent->start = 0;
		extent->len = 0;
		extent->type = type;
		*first_cluster_out = 0;
		return 0;
	}

	clusters_needed = (len + fs->cluster_size - 1) / fs->cluster_size;

	/* Check for adequate space. */
	if (fs->next_cluster + clusters_needed > fs->num_clusters) {
		WARN("allocating extent: filesystem is full!\n");
		return -1;
	}

	/* Allocate clusters. */
	start = fs->next_cluster;
	fs->next_cluster += clusters_needed;

	/* Update FAT. */
	for (i = 0; i < clusters_needed - 1; i++) {
		fs_set_fat(fs, start + i, start + i + 1);
	}
	fs_set_fat(fs, start + clusters_needed - 1, FAT_ENTRY_EOC);

	*first_cluster_out = start;

	fs_add_extent(fs,
                      extent,
                      fs->data_offset + (offset_t)(start - FAT_CLUSTER_ZERO)
                                        * fs->cluster_size,
                      (offset_t)clusters_needed * fs->cluster_size,
                      type);

	return 0;
}

int fs_init(struct fs *fs, uint16_t cluster_size, offset_t data_size,
	    offset_t *total_size_out)
{
	uint16_t sector_size;
	cluster_t data_clusters;
	sector_t reserved_sectors, fat_sectors, data_sectors, total_sectors;
	sector_t sectors_per_cluster;
	int fat_entries_per_sector;
	fat_entry_t *fat;
	struct fat_boot_sector *bs;
	struct fat_info_sector *is;

	assert(fs);

	sector_size = DEFAULT_SECTOR_SIZE;
	fs->cluster_size = cluster_size;

	sectors_per_cluster = cluster_size / DEFAULT_SECTOR_SIZE;
	fat_entries_per_sector = sector_size / sizeof(fat_entry_t);

	data_clusters = (data_size + cluster_size - 1) / cluster_size;
	data_sectors = data_clusters * sectors_per_cluster;
	fat_sectors = ((data_clusters + 2) + fat_entries_per_sector - 1)
                      / fat_entries_per_sector;
	reserved_sectors = 3;
	total_sectors = reserved_sectors + fat_sectors + data_sectors;

	memset(&fs->boot, 0, sizeof(fs->boot));
	bs = &fs->boot;

	strpadcpy(bs->name, "FATBLOCK", ' ', sizeof(bs->name));
	bs->sector_size = htole16(sector_size);
	bs->sectors_per_cluster = sectors_per_cluster;
	bs->reserved_sectors = htole16(reserved_sectors);
	bs->fats = 1;
	bs->media_desc = FAT_MEDIA_DESC_FIXED;
	/* TODO: Calculate geometry? */
	bs->sectors_per_track = htole16(42);
	bs->heads = htole16(42);
	bs->sectors32 = htole32(total_sectors);
	bs->fat_sectors32 = htole32(fat_sectors);
	/* bs->rootdir_start will be set later. */
	bs->fs_info_sector = htole16(1);
	bs->backup_boot_sector = htole16(2);
	bs->phys_drive = FAT_PHYS_DRIVE_REMOVABLE;
	bs->ext_boot_sig = FAT_EXT_BOOT_SIG;
	bs->serial = 0x42424242;
	strpadcpy(bs->vol_label, "FATBLOCK", ' ', sizeof(bs->vol_label));
	strpadcpy(bs->type, "FAT32", ' ', sizeof(bs->type));
	memcpy(bs->boot_sig, FAT_BOOT_SIG, sizeof(bs->boot_sig));

	memset(&fs->info, 0, sizeof(fs->info));
	is = &fs->info;

	memcpy(is->info_sig1, FAT_INFO_SIG1, sizeof(is->info_sig1));
	memcpy(is->info_sig2, FAT_INFO_SIG2, sizeof(is->info_sig2));
	is->free_clusters = htole32(-1);
	is->last_cluster = htole32(FAT_CLUSTER_ZERO);
	memcpy(is->info_sig3, FAT_INFO_SIG3, sizeof(is->info_sig3));

	fs->num_clusters = FAT_CLUSTER_ZERO + data_clusters;
	fs->next_cluster = FAT_CLUSTER_ZERO;

	fs->fat_size = fat_sectors * sector_size;
	fs->fat = malloc(fs->fat_size);
	if (!fs->fat) {
		WARN("initializing filesystem: couldn't allocate FAT extent: "
		     "out of memory\n");
		return MALLOC_FAIL;
	}
	memset(fs->fat, 0, fs->fat_size);

	fs->data_offset = (reserved_sectors + fat_sectors) * sector_size;

	fs->extents = NULL;
	fs_add_extent(fs, &fs->boot_extent,
                      0, sector_size,
                      EXTENT_TYPE_BOOT);
	fs_add_extent(fs, &fs->info_extent,
                      sector_size, sector_size,
                      EXTENT_TYPE_INFO);
	fs_add_extent(fs, &fs->backup_boot_extent,
                      2 * sector_size, sector_size,
                      EXTENT_TYPE_BOOT);
	fs_add_extent(fs, &fs->fat_extent,
                      reserved_sectors * sector_size, fs->fat_size,
                      EXTENT_TYPE_FAT);

	*total_size_out = (offset_t)total_sectors * sector_size;

	return 0;
}

void fs_set_rootdir_start(struct fs *fs, cluster_t rootdir_start)
{
	assert(fs);

	fs->boot.rootdir_start = htole32(rootdir_start);
}

void fs_update_free_clusters(struct fs *fs)
{
	assert(fs);

	fs->info.free_clusters = htole32(fs->num_clusters - fs->next_cluster);
}
