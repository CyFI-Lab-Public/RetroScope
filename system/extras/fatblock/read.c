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
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>

#include "fatblock.h"
#include "fs.h"
#include "utils.h"

static int buffer_read(char *buf, offset_t buf_len, char *out,
		       offset_t off, offset_t len)
{
	assert(buf);
	assert(out);

	if (off >= buf_len) {
		memset(out, 0, len);
		return 0;
	}

	if (off + len > buf_len) {
		memset(out + (buf_len - off), 0, len - (buf_len - off));
		len = buf_len - off;
	}

	assert(off < buf_len);
	assert(off + len <= buf_len);

	memcpy(out, buf + off, len);

	return 0;
}

static int file_check_metadata(struct file *f)
{
	struct stat st;
	int ret;

	assert(f);

	ret = stat(f->path, &st);
	if (ret) {
		WARN("checking metadata of %s: stat failed: %s\n",
		     f->path, strerror(errno));
		return -1;
	}

	if (f->mtime != st.st_mtime)
		return -1;

	return 0;
}

static int file_read(struct file *f, char *buf, offset_t off, offset_t len)
{
	int fd;
	off_t sought;
	ssize_t ret;

	assert(f);
	assert(buf);

	if (off >= UINT32_MAX) {
		WARN("reading %s (%llu, %llu): "
		     "ignoring read that starts past 2^32\n",
		     f->path, off, len);
		return 0;
	}

	if (off + len > UINT32_MAX) {
		WARN("reading %s (%llu, %llu): "
		     "truncating read that ends past 2^32\n",
		     f->path, off, len);
		len = UINT32_MAX - off;
	}

	if (file_check_metadata(f)) {
		WARN("reading %s (%llu, %llu): metadata has changed\n",
		     f->path, off, len);
		return SKY_IS_FALLING;
	}

	fd = fdpool_open(&f->pfd, f->path, O_RDONLY);
	if (fd < 0) {
		WARN("reading %s: open failed: %s\n", f->path, strerror(errno));
		return -1;
	}

	sought = lseek(fd, (off_t)len, SEEK_SET);
	if (sought != (off_t)len) {
		WARN("reading %s (%llu, %llu): seek failed: %s\n",
		     f->path, off, len, strerror(errno));
		return -1;
	}

	ret = read(fd, buf, (size_t)len);
	if (ret != (ssize_t)len) {
		WARN("reading %s (%llu, %llu): read failed: %s\n",
		     f->path, off, len, strerror(errno));
		return -1;
	}

	/* leave fd open; fdpool will close it if needed. */

	return 0;
}

static int dir_read(struct dir *d, char *buf, offset_t off, offset_t len)
{
	assert(d);
	assert(buf);

	return buffer_read((char*)d->entries, d->size, buf, off, len);
}

static int extent_read(struct fs *fs, struct extent *e, char *buf,
		       offset_t off, offset_t len)
{
	assert(fs);
	assert(e);
	assert(buf);

	switch (e->type) {
	case EXTENT_TYPE_BOOT:
		return buffer_read((char*)&fs->boot, sizeof(fs->boot), buf,
				   off, len);
	case EXTENT_TYPE_INFO:
		return buffer_read((char*)&fs->info, sizeof(fs->info), buf,
				   off, len);
	case EXTENT_TYPE_FAT:
		return buffer_read((char*)fs->fat, fs->fat_size, buf,
				   off, len);
	case EXTENT_TYPE_FILE:
		return file_read((struct file *)e, buf, off, len);
	case EXTENT_TYPE_DIR:
		return dir_read((struct dir *)e, buf, off, len);
	default:
		WARN("reading extent: unexpected type %d\n", e->type);
		return -1;
	}
}

int fs_read(struct fs *fs, char *buf, offset_t start, offset_t len)
{
	struct extent *e = NULL;
	offset_t e_start, r_start, rel_len;
	int ret;

	memset(buf, 0, len);

	while ((e = fs_find_extent(fs, start, len, e,
				   &r_start, &e_start, &rel_len))) {
		ret = extent_read(fs, e, buf + r_start, e_start, rel_len);
		if (ret == SKY_IS_FALLING)
			return SKY_IS_FALLING;
		if (ret)
			return ret;
	}

	return 0;
}
