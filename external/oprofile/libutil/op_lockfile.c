/**
 * @file op_lockfile.c
 * PID-based lockfile management
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include "op_lockfile.h"
#include "op_file.h"

#include <errno.h>

#include <sys/types.h>
#include <stdio.h>
#include <signal.h>
#include <unistd.h>

static pid_t op_read_lock_file(char const * file)
{
	FILE * fp;
	pid_t value;

	fp = fopen(file, "r");
	if (fp == NULL)
		return 0;

	if (fscanf(fp, "%d", &value) != 1) {
		fclose(fp);
		return 0;
	}

	fclose(fp);

	return value;
}


int op_write_lock_file(char const * file)
{
	FILE * fp;

	if (op_file_readable(file)) {
		pid_t pid = op_read_lock_file(file);

		/* FIXME: ESRCH vs. EPERM */
		if (kill(pid, 0)) {
			int err = unlink(file);
			fprintf(stderr, "Removing stale lock file %s\n",
				file);
			if (err)
				return err;
		} else {
			return EEXIST;
		}
	}

	fp = fopen(file, "w");
	if (!fp)
		return errno;

	fprintf(fp, "%d", getpid());
	fclose(fp);

	return 0;
}
