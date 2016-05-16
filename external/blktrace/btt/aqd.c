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
#include <stdlib.h>
#include <string.h>

#include "globals.h"

struct aqd_info {
	FILE *fp;
	int na;		/* # active */
};

void *aqd_alloc(char *str)
{
	char *oname;
	struct aqd_info *ap;

	if (aqd_name == NULL) return NULL;

	ap = malloc(sizeof(*ap));
	ap->na = 0;

	oname = malloc(strlen(aqd_name) + strlen(str) + 32);
	sprintf(oname, "%s_%s.dat", aqd_name, str);
	if ((ap->fp = my_fopen(oname, "w")) == NULL) {
		perror(oname);
		return NULL;
	}
	add_file(ap->fp, oname);

	return ap;

}

void aqd_free(void *info)
{
	free(info);
}

void aqd_issue(void *info, double ts)
{
	if (info) {
		struct aqd_info *ap = info;

		fprintf(ap->fp, "%lf %d\n%lf %d\n", ts, ap->na, ts, ap->na + 1);
		ap->na += 1;
	}
}

void aqd_complete(void *info, double ts)
{
	if (info) {
		struct aqd_info *ap = info;

		if (ap->na > 0) {
			fprintf(ap->fp, "%lf %d\n%lf %d\n",
					ts, ap->na, ts, ap->na - 1);
			ap->na -= 1;
		}
	}
}
