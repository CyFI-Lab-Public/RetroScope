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
#include "globals.h"

struct devmap {
	struct list_head head;
	char device[32], devno[32];
};

LIST_HEAD(all_devmaps);

static int dev_map_add(char *line)
{
	struct devmap *dmp;

	if (strstr(line, "Device") != NULL)
		return 1;

	dmp = malloc(sizeof(struct devmap));
	if (sscanf(line, "%s %s", dmp->device, dmp->devno) != 2) {
		free(dmp);
		return 1;
	}

	list_add_tail(&dmp->head, &all_devmaps);
	return 0;
}

char *dev_map_find(__u32 device)
{
	char this[128];
	struct list_head *p;

	sprintf(this, "%u,%u", MAJOR(device), MINOR(device));
	__list_for_each(p, &all_devmaps) {
		struct devmap *dmp = list_entry(p, struct devmap, head);

		if (!strcmp(this, dmp->devno))
			return dmp->device;
	}

	return NULL;
}

int dev_map_read(char *fname)
{
	char line[256];
	FILE *fp = my_fopen(fname, "r");

	if (!fp) {
		perror(fname);
		return 1;
	}

	while (fscanf(fp, "%255[a-zA-Z0-9 :.,/_-]\n", line) == 1) {
		if (dev_map_add(line))
			break;
	}

	return 0;
}

void dev_map_exit(void)
{
	struct list_head *p, *q;

	list_for_each_safe(p, q, &all_devmaps) {
		struct devmap *dmp = list_entry(p, struct devmap, head);

		list_del(&dmp->head);
		free(dmp);
	}
}
