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
#include <stdint.h>
#include <stdio.h>
#include <unistd.h>

#include <ublock/ublock.h>

#include "fatblock.h"
#include "fs.h"
#include "utils.h"

static struct fs fs;
static struct ublock_ctx *ub;
static int ums_lun = 0;

static int fs_import(struct fs *fs,
		     uint16_t cluster_size, offset_t data_size,
		     offset_t *total_size_out)
{
	int ret;

	ret = fs_init(fs, cluster_size, data_size, total_size_out);
	if (ret)
		return ret;

	ret = import_tree(fs, ".");
	if (ret)
		return ret;

	return 0;
}



static int read_callback(char *buf, uint64_t length, uint64_t offset)
{
	int result;
	int i;

	result = fs_read(&fs, buf, offset, length);
	if (result == SKY_IS_FALLING) {
		WARN("underlying filesystem has been modified; stopping.\n");
		ublock_stop(ub);
	}

	return result ? -EINVAL : 0;
}

static int write_callback(const char *buf, uint64_t length, uint64_t offset)
{
	DEBUG("writing to (%llu, %llu): we are read-only\n", offset, length);

	return -EINVAL;
}

static struct ublock_ops ops = {
	.read = &read_callback,
	.write = &write_callback
};



static int set_ums_file(int index)
{
	char filename[PATH_MAX];
	FILE *file;

	sprintf(filename, "/sys/devices/platform/usb_mass_storage/lun%d/file",
	        ums_lun);
	file = fopen(filename, "w");
	if (!file) {
		WARN("setting USB mass storage file: fopen(%s) failed: %s\n",
		     filename, strerror(errno));
		return -1;
	}

	WARN("writing '/dev/block/ublock%d' to %s.\n", index, filename);

	fprintf(file, "/dev/block/ublock%d", index);

	fclose(file);

	return 0;
}

static int clear_ums_file(void)
{
	char filename[PATH_MAX];
	FILE *file;

	sprintf(filename, "/sys/devices/platform/usb_mass_storage/lun%d/file",
	        ums_lun);
	file = fopen(filename, "w");
	if (!file) {
		WARN("clearing USB mass storage file: fopen(%s) failed: %s\n",
		     filename, strerror(errno));
		return -1;
	}

	fclose(file);

	return 0;
}




static void cleanup(void)
{
	WARN("cleanup: clearing USB mass storage file\n");
	clear_ums_file();
	WARN("cleanup: destroying block device\n");
	ublock_destroy(ub);
}

static void signal_handler(int sig)
{
	WARN("received signal %d\n", sig);
	cleanup();
	exit(0);
}

static int normal_exit = 0;

static void atexit_handler(void)
{
	if (normal_exit)
		return;

	cleanup();
}

int main(int argc, char *argv[]) {
	char *path;
	int mb;
	offset_t total_size;
	int index;
	int ret;

	signal(SIGINT, &signal_handler);
	signal(SIGTERM, &signal_handler);
	atexit(&atexit_handler);

	if (argc != 3)
		DIE("Usage: fatblock <path> <size in MB>\n");

	path = argv[1];
	mb = atoi(argv[2]);

	INFO("fatblock: importing filesystem from %s (%d MB)\n", path, mb);

	ret = chdir(path);
	if (ret < 0)
		DIE("fatblock: chdir(%s) failed: %s; aborting\n", path, strerror(errno));

	ret = fs_import(&fs, 32768, 1048576LL * mb, &total_size);
	if (ret)
		DIE("fatblock: couldn't import filesystem; aborting\n");

	INFO("fatblock: filesystem imported (%llu bytes)\n", total_size);

	ret = ublock_init(&ub, &ops, total_size);
	if (ret)
		DIE("fatblock: couldn't create block device; aborting\n");
	index = ublock_index(ub);
	if (index < 0)
		DIE("fatblock: invalid ublock index %d; aborting\n", index);

	INFO("fatblock: block device ublock%d created\n", index);
	set_ums_file(index);

	INFO("fatblock: entering main loop\n");
	ublock_run(ub);

	INFO("fatblock: destroying block device\n");
	clear_ums_file();
	ublock_destroy(ub);

	normal_exit = 1;

	INFO("fatblock: goodbye!\n");
	return 0;
}
