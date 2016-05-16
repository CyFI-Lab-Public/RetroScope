/**
 * @file opd_24_stats.c
 * Management of daemon statistics
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include "opd_24_stats.h"
#include "opd_proc.h"
#include "opd_image.h"
#include "oprofiled.h"

#include "op_get_time.h"

#include <stdlib.h>
#include <stdio.h>

unsigned long opd_24_stats[OPD_MAX_STATS];

void opd_print_24_stats(void)
{
	printf("\n%s\n", op_get_time());
	printf("Nr. proc struct: %d\n", opd_get_nr_procs());
	printf("Nr. image struct: %d\n", opd_get_nr_images());
	printf("Nr. kernel samples: %lu\n", opd_24_stats[OPD_KERNEL]);
	printf("Nr. modules samples: %lu\n", opd_24_stats[OPD_MODULE]);
	printf("Nr. modules samples lost: %lu\n", opd_24_stats[OPD_LOST_MODULE]);
	printf("Nr. samples lost due to no process information: %lu\n",
		opd_24_stats[OPD_LOST_PROCESS]);
	printf("Nr. samples lost due to sample file open failure: %lu\n",
		opd_24_stats[OPD_LOST_SAMPLEFILE]);
	printf("Nr. process samples in user-space: %lu\n", opd_24_stats[OPD_PROCESS]);
	printf("Nr. samples lost due to no map information: %lu\n",
		opd_24_stats[OPD_LOST_MAP_PROCESS]);
	if (opd_24_stats[OPD_PROC_QUEUE_ACCESS]) {
		printf("Average depth of search of proc queue: %f\n",
			(double)opd_24_stats[OPD_PROC_QUEUE_DEPTH]
			/ (double)opd_24_stats[OPD_PROC_QUEUE_ACCESS]);
	}
	if (opd_24_stats[OPD_MAP_ARRAY_ACCESS]) {
		printf("Average depth of iteration through mapping array: %f\n",
			(double)opd_24_stats[OPD_MAP_ARRAY_DEPTH]
			/ (double)opd_24_stats[OPD_MAP_ARRAY_ACCESS]);
	}
	if (opd_24_stats[OPD_IMAGE_HASH_ACCESS]) {
		printf("Average depth of iteration through image hash array: %f\n",
			(double)opd_24_stats[OPD_IMAGE_HASH_DEPTH]
			/ (double)opd_24_stats[OPD_IMAGE_HASH_ACCESS]);
	}
	printf("Nr. sample dumps: %lu\n", opd_24_stats[OPD_DUMP_COUNT]);
	printf("Nr. samples total: %lu\n", opd_24_stats[OPD_SAMPLES]);
	printf("Nr. notifications: %lu\n", opd_24_stats[OPD_NOTIFICATIONS]);
	printf("Nr. kernel note buffer overflow: %u\n",
	       opd_read_fs_int(OP_MOUNT, "note_buffer_overflow", 0));
	printf("Nr. kernel samples buffer overflow: %u\n",
	       opd_read_fs_int(OP_MOUNT, "buffer_overflow", 0));
	fflush(stdout);
}
