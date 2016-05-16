/*
 * I/O monitor based on block queue trace data
 *
 * Copyright IBM Corp. 2008
 *
 * Author(s): Martin Peschke <mp3@de.ibm.com>
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
 */

#ifndef BLKIOMON_H
#define BLKIOMON_H

#include <string.h>

#include "stats.h"
#include "blktrace.h"

#define BLKIOMON_SIZE_BUCKETS 16
#define BLKIOMON_D2C_BUCKETS 25
struct blkiomon_stat {
	__u64 time;
	__u32 size_hist[BLKIOMON_SIZE_BUCKETS];
	__u32 d2c_hist[BLKIOMON_D2C_BUCKETS];
	__u32 device;
	struct minmax size_r;
	struct minmax size_w;
	struct minmax d2c_r;
	struct minmax d2c_w;
	struct minmax thrput_r;
	struct minmax thrput_w;
	__u64 bidir;
};

static struct histlog2 size_hist = {
	.first = 0,
	.delta = 1024,
	.num = BLKIOMON_SIZE_BUCKETS
};

static struct histlog2 d2c_hist = {
	.first = 0,
	.delta = 8,
	.num = BLKIOMON_D2C_BUCKETS
};

static inline void blkiomon_stat_init(struct blkiomon_stat *bstat)
{
	memset(bstat, 0, sizeof(*bstat));
	minmax_init(&bstat->size_r);
	minmax_init(&bstat->size_w);
	minmax_init(&bstat->d2c_r);
	minmax_init(&bstat->d2c_w);
	minmax_init(&bstat->thrput_r);
	minmax_init(&bstat->thrput_w);
}

static inline void blkiomon_stat_to_be(struct blkiomon_stat *bstat)
{
	histlog2_to_be(bstat->size_hist, &size_hist);
	histlog2_to_be(bstat->d2c_hist, &d2c_hist);
	minmax_to_be(&bstat->size_r);
	minmax_to_be(&bstat->size_w);
	minmax_to_be(&bstat->d2c_r);
	minmax_to_be(&bstat->d2c_w);
	minmax_to_be(&bstat->thrput_r);
	minmax_to_be(&bstat->thrput_w);
	bstat->bidir = cpu_to_be64(bstat->bidir);
	bstat->time = cpu_to_be64(bstat->time);
	bstat->device = cpu_to_be32(bstat->device);
}

static inline void blkiomon_stat_merge(struct blkiomon_stat *dst,
				       struct blkiomon_stat *src)
{
	histlog2_merge(&size_hist, dst->size_hist, src->size_hist);
	histlog2_merge(&d2c_hist, dst->d2c_hist, src->d2c_hist);
	minmax_merge(&dst->size_r, &src->size_r);
	minmax_merge(&dst->size_w, &src->size_w);
	minmax_merge(&dst->d2c_r, &src->d2c_r);
	minmax_merge(&dst->d2c_w, &src->d2c_w);
	minmax_merge(&dst->thrput_r, &src->thrput_r);
	minmax_merge(&dst->thrput_w, &src->thrput_w);
	dst->bidir += src->bidir;
}

static inline void blkiomon_stat_print(FILE *fp, struct blkiomon_stat *p)
{
	if (!fp)
		return;

	fprintf(fp, "\ntime: %s", ctime((void *)&p->time));
	fprintf(fp, "device: %d,%d\n", MAJOR(p->device), MINOR(p->device));
	minmax_print(fp, "sizes read (bytes)", &p->size_r);
	minmax_print(fp, "sizes write (bytes)", &p->size_w);
	minmax_print(fp, "d2c read (usec)", &p->d2c_r);
	minmax_print(fp, "d2c write (usec)", &p->d2c_w);
	minmax_print(fp, "throughput read (bytes/msec)", &p->thrput_r);
	minmax_print(fp, "throughput write (bytes/msec)", &p->thrput_w);
	histlog2_print(fp, "sizes histogram (bytes)", p->size_hist, &size_hist);
	histlog2_print(fp, "d2c histogram (usec)", p->d2c_hist, &d2c_hist);
	fprintf(fp, "bidirectional requests: %ld\n", (unsigned long)p->bidir);
}

#endif
