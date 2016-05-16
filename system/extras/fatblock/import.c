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
#include <ctype.h>
#include <dirent.h>
#include <errno.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>

#include "fatblock.h"
#include "fat.h"
#include "fdpool.h"
#include "fs.h"
#include "utils.h"

static inline int valid_char(int c)
{
	return (isalnum(c) ||
		strchr("!#$%'()-@^_`{}~", c) ||
		((c >= 128) && (c < 256)));
}

static int convert_name(char *short_name, const char *long_name)
{
	int i;

	const char *s;
	const char *dot;
	int c;

	dot = NULL;

	for (s = long_name; *s; s++) {
		if (*s == '.') {
			if (dot) {
				goto short_fail;
			} else {
				dot = s;
			}
		} else if (!valid_char(*s)) {
			goto short_fail;
		}
	}

	if (dot - long_name > 8) {
		goto short_fail;
	}

	if (dot && (s - (dot + 1) > 3)) {
		goto short_fail;
	}

	memset(short_name, ' ', 11);

	if (!dot) {
		dot = s;
	}

	for (i = 0; i < dot - long_name; i++) {
		short_name[i] = toupper(long_name[i]);
	}

	for (i = 0; i < s - dot; i++) {
		short_name[8 + i] = toupper(dot[1 + i]);
	}

	return 0;

short_fail:
	return 1;
}

struct imported {
	cluster_t first_cluster;
	uint32_t size;
	struct fat_dirent *dot_dot_dirent;
};

static int import_file(struct fs *fs, char *path, struct imported *out)
{
	struct stat st;
	struct file *f = NULL;
	char *path_copy = NULL;
	int ret;

	ret = stat(path, &st);
	if (ret < 0) {
		WARN("importing %s: stat failed: %s\n", path, strerror(errno));
		goto fail;
	}

	f = malloc(sizeof(struct file));
	if (!f) {
		WARN("importing %s: couldn't allocate file struct: "
		     "out of memory\n", path);
		ret = MALLOC_FAIL;
		goto fail;
	}

	path_copy = strdup(path);
	if (!path_copy) {
		WARN("importing %s: couldn't strdup path: out of memory\n",
		     path);
		ret = MALLOC_FAIL;
		goto fail;
	}

	f->path = path_copy;
	f->size = st.st_size;
	f->dev = st.st_dev;
	f->ino = st.st_ino;
	f->mtime = st.st_mtime;
	fdpool_init(&f->pfd);

	ret = fs_alloc_extent(fs, &f->extent,
                              f->size, EXTENT_TYPE_FILE, &out->first_cluster);
	if (ret) {
		WARN("importing %s: couldn't allocate data extent\n", path);
		goto fail;
	}

	out->size = f->size;
	out->dot_dot_dirent = NULL;

	return 0;

fail:
	if (path_copy)
		free(path_copy);
	if (f)
		free(f);
	return ret;
}

struct item {
	char name[11];
	struct imported imp;
	struct item *next;
	int is_dir;
};

static struct item *free_items_head;

static struct item *alloc_item(void)
{
	struct item *item;

	if (free_items_head) {
		item = free_items_head;
		free_items_head = item->next;
	} else {
		item = malloc(sizeof(struct item));
		/* May return NULL if item couldn't be allocated. */
	}

	return item;
}

static void free_item(struct item *item)
{
	item->next = free_items_head;
	free_items_head = item;
}

static void free_items(struct item *head)
{
	struct item *tail;

	for (tail = head; tail->next; tail = tail->next);

	tail->next = free_items_head;
	free_items_head = head;
}

/* TODO: With some work, this can be rewritten so we don't recurse
 * until all memory is allocated. */
static int import_dir(struct fs *fs, char *path, int is_root,
		      struct imported *out)
{
	struct dir *d;
	cluster_t my_first_cluster;

	DIR *dir;
	struct dirent *de;

	char ch_path[PATH_MAX];
	struct imported *ch_imp;
	cluster_t ch_first_cluster;
	struct fat_dirent *ch_dirent;

	int ret;

	struct item *items;
	struct item *item;
	int count;

	int i;

	dir = opendir(path);
	if (!dir) {
		WARN("importing %s: opendir failed: %s\n", path,
		     strerror(errno));
		return -1;
	}

	d = malloc(sizeof(struct dir));
	if (!d) {
		WARN("importing %s: couldn't allocate dir struct: "
		     "out of memory\n", path);
		closedir(dir);
		return MALLOC_FAIL;
	}

	d->path = strdup(path);
	if (!d->path) {
		WARN("importing %s: couldn't strdup path: out of memory\n",
		     path);
		closedir(dir);
		free(d);
		return MALLOC_FAIL;
	}

	items = NULL;
	item = NULL;
	count = 0;

	while ((de = readdir(dir))) {
		if (de->d_name[0] == '.') {
			goto skip_item;
		}

		ret = snprintf(ch_path, PATH_MAX, "%s/%s", path, de->d_name);
		if (ret < 0 || ret >= PATH_MAX) {
			goto skip_item;
		}

		item = alloc_item();
		if (!item) {
			WARN("importing %s: couldn't allocate item struct: "
			     "out of memory\n", path);
			ret = MALLOC_FAIL;
			goto free_items;
		}

		if (convert_name(item->name, de->d_name)) {
			goto skip_item;
		}

		switch (de->d_type) {
			case DT_REG:
				import_file(fs, ch_path, &item->imp);
				item->is_dir = 0;
				break;
			case DT_DIR:
				import_dir(fs, ch_path, 0, &item->imp);
				item->is_dir = 1;
				break;
			default:
				goto skip_item;
		}

		item->next = items;
		items = item;

		count++;

		item = NULL;

		continue;

skip_item:
		if (item)
			free_item(item);
	}

	closedir(dir);

	d->size = sizeof(struct fat_dirent) * (count + (is_root ? 0 : 2));
	ret = fs_alloc_extent(fs, &d->extent, d->size, EXTENT_TYPE_DIR, &out->first_cluster);
	if (ret) {
		WARN("importing %s: couldn't allocate directory table extent: "
		     "out of space\n", path);
		goto free_items;
	}

	if (is_root)
		out->first_cluster = 0;

	my_first_cluster = is_root ? 0 : out->first_cluster;

	d->entries = malloc(sizeof(struct fat_dirent) * (count + (is_root ? 0 : 2)));
	assert(d->entries);
	for (i = count - 1; i >= 0; i--) {
		item = items;
		items = item->next;

		ch_dirent = &d->entries[i + (is_root ? 0 : 2)];

		fat_dirent_set(ch_dirent,
                               item->name, item->is_dir ? FAT_ATTR_SUBDIR : 0,
                               item->imp.first_cluster, item->imp.size);

		if (item->imp.dot_dot_dirent) {
			fat_dirent_set_first_cluster(item->imp.dot_dot_dirent,
						     my_first_cluster);
		}

		free_item(item);
	}

	if (!is_root) {
		fat_dirent_set(&d->entries[0],
                               "..         ", FAT_ATTR_SUBDIR,
                               (cluster_t)-1, 0);
		out->dot_dot_dirent = &d->entries[0]; /* will set first_cluster */

		fat_dirent_set(&d->entries[1],
                               ".          ", FAT_ATTR_SUBDIR,
                               my_first_cluster, 0);
	} else {
		out->dot_dot_dirent = NULL;
	}

	out->size = 0;

	return 0;

free_items:
	free_items(items);
	free(d->path);
	free(d);

	return ret;
}

int import_tree(struct fs *fs, char *path)
{
	struct imported imp;
	int ret;

	ret = import_dir(fs, path, 0, &imp);
	if (ret)
		return ret;

	fs_set_rootdir_start(fs, imp.first_cluster);
	fs_update_free_clusters(fs);

	return 0;
}
