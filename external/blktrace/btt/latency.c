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
#include "globals.h"

static inline void latency_out(FILE *ofp, __u64 tstamp, __u64 latency)
{
	if (ofp)
		fprintf(ofp, "%lf %lf\n", TO_SEC(tstamp), TO_SEC(latency));
}

FILE *latency_open(__u32 device, char *name, char *post)
{
	FILE *fp = NULL;

	if (name) {
		int mjr, mnr;
		char oname[strlen(name) + 32];

		mjr = device >> MINORBITS;
		mnr = device & ((1 << MINORBITS) - 1);

		sprintf(oname, "%s_%03d,%03d_%s.dat", name, mjr, mnr, post);
		if ((fp = my_fopen(oname, "w")) == NULL)
			perror(oname);
		else
			add_file(fp, strdup(oname));
	}

	return fp;
}

void latency_alloc(struct d_info *dip)
{
	dip->q2d_ofp = latency_open(dip->device, q2d_name, "q2d");
	dip->d2c_ofp = latency_open(dip->device, d2c_name, "d2c");
	dip->q2c_ofp = latency_open(dip->device, q2c_name, "q2c");
}

void latency_q2d(struct d_info *dip, __u64 tstamp, __u64 latency)
{
	plat_x2c(dip->q2d_plat_handle, tstamp, latency);
	latency_out(dip->q2d_ofp, tstamp, latency);
}

void latency_d2c(struct d_info *dip, __u64 tstamp, __u64 latency)
{
	plat_x2c(dip->d2c_plat_handle, tstamp, latency);
	latency_out(dip->d2c_ofp, tstamp, latency);
}

void latency_q2c(struct d_info *dip, __u64 tstamp, __u64 latency)
{
	plat_x2c(dip->q2c_plat_handle, tstamp, latency);
	latency_out(dip->q2c_ofp, tstamp, latency);
}
