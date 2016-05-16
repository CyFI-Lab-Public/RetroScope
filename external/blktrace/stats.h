/*
 * Copyright IBM Corp. 2008
 *
 * Author(s): Martin Peschke <mp3@de.ibm.com>
 *            Stefan Raspl <stefan.raspl@de.ibm.com>
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

#ifndef STATS_H
#define STATS_H

#include <linux/types.h>
#include "blktrace.h"

struct minmax {
	__u64 min;
	__u64 max;
	__u64 sum;
	__u64 sos;
	__u64 num;
};

static inline void minmax_init(struct minmax *mm)
{
	mm->min = -1ULL;
	mm->max = 0;
	mm->sum = 0;
	mm->sos = 0;
	mm->num = 0;
}

static inline void minmax_account(struct minmax *mm, __u64 value)
{
	mm->sum += value;
	mm->sos += value * value;
	if (value < mm->min)
		mm->min = value;
	if (value > mm->max)
		mm->max = value;
	mm->num++;
}

static inline void minmax_merge(struct minmax *dst, struct minmax *src)
{
	dst->sum += src->sum;
	dst->sos += src->sos;
	if (src->min < dst->min)
		dst->min = src->min;
	if (src->max > dst->max)
		dst->max = src->max;
	dst->num += src->num;
}

static inline void minmax_to_be(struct minmax *mm)
{
	mm->sum = cpu_to_be64(mm->sum);
	mm->sos = cpu_to_be64(mm->sos);
	mm->min = cpu_to_be64(mm->min);
	mm->max = cpu_to_be64(mm->max);
	mm->num = cpu_to_be64(mm->num);
}

static inline double minmax_avg(struct minmax *mm)
{
	return (mm->sum / (double)mm->num);
}

static inline double minmax_var(struct minmax *mm)
{
	double num = (double)mm->num;

	return ((mm->sos - ((mm->sum * mm->sum) / num)) / num);
}

static inline int minmax_print(FILE *fp, const char *s, struct minmax *mm)
{
	return fprintf(fp, "%s: num %Ld, min %Ld, max %Ld, sum %Ld, squ %Ld, "
		       "avg %.1f, var %.1f\n", s, (unsigned long long)mm->num,
		       (unsigned long long)mm->min, (unsigned long long)mm->max,
		       (unsigned long long)mm->sum, (unsigned long long)mm->sos,
		       minmax_avg(mm), minmax_var(mm));
}

struct histlog2 {
	int first;
	int delta;
	int num;
};

static inline __u64 histlog2_upper_limit(int index, struct histlog2 *h)
{
	return h->first + (index ? h->delta << (index - 1) : 0);
}

static inline int histlog2_index(__u64 val, struct histlog2 *h)
{
	int i;

	for (i = 0; i < (h->num - 1) && val > histlog2_upper_limit(i, h); i++);
	return i;
}

static inline void histlog2_account(__u32 *bucket, __u32 val,
				    struct histlog2 *h)
{
	int index = histlog2_index(val, h);
	bucket[index]++;
}

static inline void histlog2_merge(struct histlog2 *h, __u32 *dst, __u32 *src)
{
	int i;

	for (i = 0; i < h->num; i++)
		dst[i] += src[i];
}

static inline void histlog2_to_be(__u32 a[], struct histlog2 *h)
{
	int i;

	for (i = 0; i < h->num; i++)
		a[i] = cpu_to_be32(a[i]);
}

static inline void histlog2_print(FILE *fp, const char *s, __u32 a[],
				  struct histlog2 *h)
{
	int i;

	fprintf(fp, "%s:\n", s);
	for (i = 0; i < h->num - 1; i++) {
		fprintf(fp, "   %10ld:%6d",
			(unsigned long)(histlog2_upper_limit(i, h)), a[i]);
		if (!((i + 1) % 4))
			fprintf(fp, "\n");
	}
	fprintf(fp, "    >%8ld:%6d\n",
		(unsigned long)(histlog2_upper_limit(i - 1, h)), a[i]);
}

#endif
