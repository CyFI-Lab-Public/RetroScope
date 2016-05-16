/**
 * @file daemon/opd_pipe.c
 * Functions handling the $SESSIONDIR/opd_pipe FIFO special file.
 * NOTE: This code is dealing with potentially insecure input.
 *
 * @remark Copyright 2008 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Daniel Hansel
 */

#include "opd_pipe.h"
#include "opd_printf.h"
#include "op_config.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <sys/stat.h>

static int fifo;
static FILE * fifo_fd = NULL;

void opd_create_pipe(void)
{
	mode_t orig_umask = umask(0111);
	if (mkfifo(op_pipe_file, 0666) == -1) {
		if (errno != EEXIST) {
			perror("oprofiled: couldn't create pipe: ");
			exit(EXIT_FAILURE);
		}
	}
	umask(orig_umask);
}


void opd_open_pipe(void)
{
	fifo = open(op_pipe_file, O_RDONLY | O_NONBLOCK);
	if (fifo == -1) {
		perror("oprofiled: couldn't open pipe: ");
		exit(EXIT_FAILURE);
	}
}


void opd_close_pipe(void)
{
	if (fifo_fd)
		fclose(fifo_fd);
	close(fifo);
}


int is_jitconv_requested(void)
{
	/* number of dropped (unknown) requests */
	static long nr_drops = 0;
	/* modulus to output only a few warnings to avoid flooding oprofiled.log */
	static int mod_cnt_drops = 1;
	char line[256];
	int i, ret = 0;

	/* get a file descriptor to the pipe */
	if (!fifo_fd)
		fifo_fd = fdopen(fifo, "r");

	if (fifo_fd == NULL) {
		perror("oprofiled: couldn't create file descriptor: ");
		exit(EXIT_FAILURE);
	}

	/* read up to 99 lines to check for 'do_jitconv' */
	for (i = 0; i < 99; i++) {
		/* just break if no new line is found */
		if (fgets(line, 256, fifo_fd) == NULL)
			break;
		line[strlen(line) - 1] = '\0';

		if (strstr(line, "do_jitconv") != NULL) {
			ret = 1;
		} else {
			nr_drops++;

			if (nr_drops % mod_cnt_drops == 0) {
				printf(
				       "Warning: invalid pipe request received (dropped request(s): %ld)\n",
				       nr_drops);
				/* increase modulus to avoid flooding log file */
				mod_cnt_drops *= 5;
			}
		}
	}

	return ret;
}
