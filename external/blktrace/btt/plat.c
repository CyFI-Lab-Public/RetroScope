/*
 * blktrace output analysis: generate a timeline & gather statistics
 *
 * (C) Copyright 2008 Hewlett-Packard Development Company, L.P.
 * 	Alan D. Brunelle <alan.brunelle@hp.com>
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

struct plat_info {
	long nl;
	FILE *fp;
	double first_ts, last_ts, tl;
};

void *plat_alloc(char *str)
{
	char *oname;
	struct plat_info *pp;

	if (plat_freq <= 0.0) return NULL;

	pp = malloc(sizeof(*pp));
	pp->nl = 0;
	pp->first_ts = pp->last_ts = pp->tl = -1.0;

	oname = malloc(strlen(str) + 32);
	sprintf(oname, "%s.dat", str);
	if ((pp->fp = my_fopen(oname, "w")) == NULL) {
		perror(oname);
		return NULL;
	}
	add_file(pp->fp, oname);

	return pp;
}

void plat_free(void *info)
{
	struct plat_info *pp = info;

	if (pp == NULL) return;

	if (pp->first_ts != -1.0) {
		double delta = pp->last_ts - pp->first_ts;

		fprintf(pp->fp, "%lf %lf\n",
			pp->first_ts + (delta / 2), pp->tl / pp->nl);
	}
	free(info);
}

void plat_x2c(void *info, __u64 ts, __u64 latency)
{
	double now = TO_SEC(ts);
	double lat = TO_SEC(latency);
	struct plat_info *pp = info;

	if (pp == NULL) return;

	if (pp->first_ts == -1.0) {
		pp->first_ts = pp->last_ts = now;
		pp->nl = 1;
		pp->tl = lat;
	} else if ((now - pp->first_ts) >= plat_freq) {
		double delta = pp->last_ts - pp->first_ts;

		fprintf(pp->fp, "%lf %lf\n",
			pp->first_ts + (delta / 2), pp->tl / pp->nl);

		pp->first_ts = pp->last_ts = now;
		pp->nl = 1;
		pp->tl = lat;
	} else {
		pp->last_ts = now;
		pp->nl += 1;
		pp->tl += lat;
	}
}
