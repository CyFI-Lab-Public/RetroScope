/*
 * blktrace output analysis: generate a timeline & gather statistics
 *
 * Copyright (C) 2006 Alan D. Brunelle <Alan.Brunelle@hp.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

#include <stdio.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <string.h>

#include "blktrace.h"
#include "globals.h"

#define DEF_LEN	(16 * 1024 * 1024)

static int fd;
static void *cur_map = MAP_FAILED;
static off_t cur_min, cur, cur_max, total_size;
static size_t len;
static struct blk_io_trace *next_t;
static long pgsz;

int data_is_native = -1;

static inline size_t min_len(size_t a, size_t b)
{
	return a < b ? a : b;
}

static inline size_t convert_to_cpu(struct blk_io_trace *t,
                                    struct blk_io_trace *tp,
				    void **pdu)
{
	if (data_is_native == -1)
		check_data_endianness(t->magic);

	if (data_is_native)
		memcpy(tp, t, sizeof(*tp));
	else {
		tp->magic	= be32_to_cpu(t->magic);
		tp->sequence	= be32_to_cpu(t->sequence);
		tp->time	= be64_to_cpu(t->time);
		tp->sector	= be64_to_cpu(t->sector);
		tp->bytes	= be32_to_cpu(t->bytes);
		tp->action	= be32_to_cpu(t->action);
		tp->pid		= be32_to_cpu(t->pid);
		tp->device	= be32_to_cpu(t->device);
		tp->cpu		= be16_to_cpu(t->cpu);
		tp->error	= be16_to_cpu(t->error);
		tp->pdu_len	= be16_to_cpu(t->pdu_len);
	}

	if (tp->pdu_len) {
		*pdu = malloc(tp->pdu_len);
		memcpy(*pdu, t+1, tp->pdu_len);
	} else
		*pdu = NULL;

	return sizeof(*tp) + tp->pdu_len;
}

static int move_map(void)
{
	if (cur_map != MAP_FAILED)
		munmap(cur_map, len);

	cur_min = (cur & ~(pgsz-1));
	len = min_len(DEF_LEN, total_size - cur_min);
	if (len < sizeof(*next_t))
		return 0;

	cur_map = mmap(NULL, len, PROT_READ, MAP_SHARED, fd,
		       cur_min);
	if (cur_map == MAP_FAILED) {
		perror("mmap");
		exit(1);
	}

	cur_max = cur_min + len;
	return (cur < cur_max);
}

void setup_ifile(char *fname)
{
	struct stat buf;

	pgsz = sysconf(_SC_PAGESIZE);

	fd = my_open(fname, O_RDONLY);
	if (fd < 0) {
		perror(fname);
		exit(1);
	}
	if (fstat(fd, &buf) < 0) {
		perror(fname);
		exit(1);
	}
	total_size = buf.st_size;

	if (!move_map())
		exit(0);
}

void cleanup_ifile(void)
{
	if (cur_map != MAP_FAILED)
		munmap(cur_map, len);
	close(fd);
}

int next_trace(struct blk_io_trace *t, void **pdu)
{
	size_t this_len;

	if ((cur + 512) > cur_max)
		if (!move_map()) {
			cleanup_ifile();
			return 0;
		}

	next_t = cur_map + (cur - cur_min);
	this_len = convert_to_cpu(next_t, t, pdu);
	cur += this_len;

	return 1;
}

double pct_done(void)
{
	return 100.0 * ((double)cur / (double)total_size);
}
