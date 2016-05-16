/**
 * @file daemon/opd_stats.c
 * Management of daemon statistics
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include "opd_stats.h"
#include "opd_extended.h"
#include "oprofiled.h"

#include "op_get_time.h"

#include <dirent.h>
#include <stdlib.h>
#include <stdio.h>

unsigned long opd_stats[OPD_MAX_STATS];

/**
 * print_if - print an integer value read from file filename,
 * do nothing if the value read == -1 except if force is non-zero
 */
static void print_if(char const * fmt, char const * path, char const * filename, int force)
{
	int value = opd_read_fs_int(path, filename, 0);
	if (value != -1 || force)
		printf(fmt, value);
}

/**
 * opd_print_stats - print out latest statistics
 */
void opd_print_stats(void)
{
	DIR * dir;
	struct dirent * dirent;

	printf("\n%s\n", op_get_time());
	printf("\n-- OProfile Statistics --\n");
	printf("Nr. sample dumps: %lu\n", opd_stats[OPD_DUMP_COUNT]);
	printf("Nr. non-backtrace samples: %lu\n", opd_stats[OPD_SAMPLES]);
	printf("Nr. kernel samples: %lu\n", opd_stats[OPD_KERNEL]);
	printf("Nr. lost samples (no kernel/user): %lu\n", opd_stats[OPD_NO_CTX]);
	printf("Nr. lost kernel samples: %lu\n", opd_stats[OPD_LOST_KERNEL]);
	printf("Nr. incomplete code structs: %lu\n", opd_stats[OPD_DANGLING_CODE]);
	printf("Nr. samples lost due to sample file open failure: %lu\n",
		opd_stats[OPD_LOST_SAMPLEFILE]);
	printf("Nr. samples lost due to no permanent mapping: %lu\n",
		opd_stats[OPD_LOST_NO_MAPPING]);
	print_if("Nr. event lost due to buffer overflow: %u\n",
	       "/dev/oprofile/stats", "event_lost_overflow", 1);
	print_if("Nr. samples lost due to no mapping: %u\n",
	       "/dev/oprofile/stats", "sample_lost_no_mapping", 1);
	print_if("Nr. backtraces skipped due to no file mapping: %u\n",
	       "/dev/oprofile/stats", "bt_lost_no_mapping", 0);
	print_if("Nr. samples lost due to no mm: %u\n",
	       "/dev/oprofile/stats", "sample_lost_no_mm", 1);

	opd_ext_print_stats();

	if (!(dir = opendir("/dev/oprofile/stats/")))
		goto out;
	while ((dirent = readdir(dir))) {
		int cpu_nr;
		char path[256];
		if (sscanf(dirent->d_name, "cpu%d", &cpu_nr) != 1)
			continue;
		snprintf(path, 256, "/dev/oprofile/stats/%s", dirent->d_name);

		printf("\n---- Statistics for cpu : %d\n", cpu_nr);
		print_if("Nr. samples lost cpu buffer overflow: %u\n",
		     path, "sample_lost_overflow", 1);
		print_if("Nr. samples lost task exit: %u\n",
		     path, "sample_lost_task_exit", 0);
		print_if("Nr. samples received: %u\n",
		     path, "sample_received", 1);
		print_if("Nr. backtrace aborted: %u\n", 
		     path, "backtrace_aborted", 0);
		print_if("Nr. samples lost invalid pc: %u\n", 
		     path, "sample_invalid_eip", 0);
	}
	closedir(dir);
out:
	fflush(stdout);
}
