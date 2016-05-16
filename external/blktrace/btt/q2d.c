/*
 * blktrace output analysis: generate a timeline & gather statistics
 *
 * (C) Copyright 2007 Hewlett-Packard Development Company, L.P.
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
#include <stdlib.h>
#include <string.h>

#include "globals.h"

#define Q2D_MAX_HISTO 9
struct q2d_info {
	unsigned long nhistos;
	unsigned long histos[Q2D_MAX_HISTO + 1];
};

void q2d_histo_add(void *priv, __u64 q2d_in)
{
	int index;
	struct q2d_info *q2dp = priv;
	double q2d = BIT_TIME(q2d_in);
	long msec = (long)(q2d / 0.001);

	switch (msec) {
	default:          index = 9; break;
	case 500 ... 999: index = 8; break;
	case 250 ... 499: index = 7; break;
	case 100 ... 249: index = 6; break;
	case  75 ...  99: index = 5; break;
	case  50 ...  74: index = 4; break;
	case  25 ...  49: index = 3; break;
	case  10 ...  24: index = 2; break;
	case   5 ...   9: index = 1; break;
	case   0 ...   4: index = 0; break;
	}

	q2dp->histos[index]++;
	q2dp->nhistos++;
}

void *q2d_alloc(void)
{
	struct q2d_info *q2dp = malloc(sizeof(*q2dp));

	return memset(q2dp, 0, sizeof(*q2dp));
}

void q2d_free(void *priv)
{
	free(priv);
}

void q2d_display_header(FILE *fp)
{
	fprintf(fp, "%5s ", "<.005");
	fprintf(fp, "%5s ", "<.010");
	fprintf(fp, "%5s ", "<.025");
	fprintf(fp, "%5s ", "<.050");
	fprintf(fp, "%5s ", "<.075");
	fprintf(fp, "%5s ", "<.100");
	fprintf(fp, "%5s ", "<.250");
	fprintf(fp, "%5s ", "<.500");
	fprintf(fp, "%5s ", "< 1.0");
	fprintf(fp, "%5s ", ">=1.0\n");
}

void q2d_display_dashes(FILE *fp)
{
	int i;

	for (i = 0; i <= Q2D_MAX_HISTO; i++)
		fprintf(fp, "===== ");
	fprintf(fp, "\n");
}

void q2d_display(FILE *fp, void *priv)
{
	int i;
	struct q2d_info *q2dp = priv;
	double nh = (double)q2dp->nhistos;

	for (i = 0; i <= Q2D_MAX_HISTO; i++) {
		double p = 100.0 * (double)q2dp->histos[i] / nh;
		fprintf(fp, "%5.1lf ", p);
	}
	fprintf(fp, "\n");
}

int q2d_ok(void *priv)
{
	struct q2d_info *q2dp = priv;

	return q2dp && q2dp->nhistos > 0;
}

void q2d_acc(void *a1, void *a2)
{
	int i;
	struct q2d_info *ap = a1;
	struct q2d_info *tp = a2;

	for (i = 0; i <= Q2D_MAX_HISTO; i++)
		ap->histos[i] += tp->histos[i];
	ap->nhistos += tp->nhistos;
}
