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
#include <getopt.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include "globals.h"

#define SETBUFFER_SIZE	(64 * 1024)

#define S_OPTS	"aAB:d:D:e:hi:I:l:L:m:M:o:p:P:q:Q:rs:S:t:T:u:VvXz:"
static struct option l_opts[] = {
	{
		.name = "seek-absolute",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'a'
	},
	{
		.name = "all-data",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'A'
	},
	{
		.name = "dump-blocknos",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'B'
	},
	{
		.name = "range-delta",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'd'
	},
	{
		.name = "devices",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'D'
	},
	{
		.name = "exes",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'e'
	},
	{
		.name = "help",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'h'
	},
	{
		.name = "input-file",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'i'
	},
	{
		.name = "iostat",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'I'
	},
	{
		.name = "d2c-latencies",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'l'
	},
	{
		.name = "periodic-latencies",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'L'
	},
	{
		.name = "seeks-per-second",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'm'
	},
	{
		.name = "dev-maps",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'M'
	},
	{
		.name = "output-file",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'o'
	},
	{
		.name = "per-io-dump",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'p'
	},
	{
		.name = "per-io-trees",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'P'
	},
	{
		.name = "q2c-latencies",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'q'
	},
	{
		.name = "active-queue-depth",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'Q'
	},
	{
		.name = "no-remaps",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'r'
	},
	{
		.name = "seeks",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 's'
	},
	{
		.name = "iostat-interval",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'S'
	},
	{
		.name = "time-start",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 't'
	},
	{
		.name = "time-end",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'T'
	},
	{
		.name = "unplug-hist",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'u'
	},
	{
		.name = "version",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'V'
	},
	{
		.name = "verbose",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'v'
	},
	{
		.name = "easy-parse-avgs",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'X'
	},
	{
		.name = "q2d-latencies",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'z'
	},
	{
		.name = NULL,
	}
};

static char usage_str[] = \
	"\n[ -a               | --seek-absolute ]\n" \
	"[ -A               | --all-data ]\n" \
	"[ -B <output name> | --dump-blocknos=<output name> ]\n" \
	"[ -d <seconds>     | --range-delta=<seconds> ]\n" \
	"[ -D <dev;...>     | --devices=<dev;...> ]\n" \
	"[ -e <exe,...>     | --exes=<exe,...>  ]\n" \
	"[ -h               | --help ]\n" \
	"[ -i <input name>  | --input-file=<input name> ]\n" \
	"[ -I <output name> | --iostat=<output name> ]\n" \
	"[ -l <output name> | --d2c-latencies=<output name> ]\n" \
	"[ -L <freq>        | --periodic-latencies=<freq> ]\n" \
	"[ -m <output name> | --seeks-per-second=<output name> ]\n" \
	"[ -M <dev map>     | --dev-maps=<dev map>\n" \
	"[ -o <output name> | --output-file=<output name> ]\n" \
	"[ -p <output name> | --per-io-dump=<output name> ]\n" \
	"[ -P <output name> | --per-io-trees=<output name> ]\n" \
	"[ -q <output name> | --q2c-latencies=<output name> ]\n" \
	"[ -Q <output name> | --active-queue-depth=<output name> ]\n" \
	"[ -r               | --no-remaps ]\n" \
	"[ -s <output name> | --seeks=<output name> ]\n" \
	"[ -S <interval>    | --iostat-interval=<interval> ]\n" \
	"[ -t <sec>         | --time-start=<sec> ]\n" \
	"[ -T <sec>         | --time-end=<sec> ]\n" \
	"[ -u <output name> | --unplug-hist=<output name> ]\n" \
	"[ -V               | --version ]\n" \
	"[ -v               | --verbose ]\n" \
	"[ -X               | --easy-parse-avgs ]\n" \
	"[ -z <output name> | --q2d-latencies=<output name> ]\n" \
	"\n";

static void usage(char *prog)
{
	fprintf(stderr, "Usage: %s %s %s", prog, bt_timeline_version,
		usage_str);
}

static FILE *setup_ofile(char *fname)
{
	if (fname) {
		char *buf;
		FILE *ofp = my_fopen(fname, "w");

		if (!ofp) {
			perror(fname);
			exit(1);
		}

		buf = malloc(SETBUFFER_SIZE);
		setbuffer(ofp, buf, SETBUFFER_SIZE);

		add_file(ofp, fname);
		add_buf(buf);

		return ofp;
	}

	return NULL;
}

static FILE *std_open(char *output_name, char *sfx, char *msg)
{
	FILE *fp;
	char fname[strlen(output_name) + 32];

	sprintf(fname, "%s.%s", output_name, sfx);
	fp = my_fopen(fname, "w");
	if (fp == NULL) {
		perror(fname);
		exit(1);
	}
	if (verbose)
		printf("Sending %s to %s\n", msg, fname);

	return fp;
}

void handle_args(int argc, char *argv[])
{
	int c;

	while ((c = getopt_long(argc, argv, S_OPTS, l_opts, NULL)) != -1) {
		switch (c) {
		case 'a':
			seek_absolute = 1;
			break;
		case 'A':
			output_all_data = 1;
			break;
		case 'B':
			bno_dump_name = optarg;
			break;
		case 'd':
			sscanf(optarg, "%lf", &range_delta);
			break;
		case 'D':
			devices = optarg;
			break;
		case 'e':
			exes = optarg;
			break;
		case 'h':
			usage(argv[0]);
			exit(0);
		case 'i':
			input_name = optarg;
			break;
		case 'l':
			d2c_name = optarg;
			break;
		case 'L':
			plat_freq = atof(optarg);
			break;
		case 'I':
			iostat_name = strdup(optarg);
			break;
		case 'm':
			sps_name = optarg;
			break;
		case 'M':
			if (dev_map_read(optarg))
				exit(1);
			break;
		case 'o':
			output_name = optarg;
			break;
		case 'p':
			per_io_name = strdup(optarg);
			break;
		case 'P':
			per_io_trees = optarg;
			break;
		case 'q':
			q2c_name = optarg;
			break;
		case 'Q':
			aqd_name = optarg;
			break;
		case 'r':
			ignore_remaps = 1;
			break;
		case 's':
			seek_name = optarg;
			break;
		case 'S': {
			unsigned int interval;

			sscanf(optarg, "%u", &interval);
			iostat_interval = (__u64)interval * 1000000000LL;
			break;
		}
		case 't':
			sscanf(optarg, "%lf", &t_astart);
			time_bounded = 1;
			break;
		case 'T':
			sscanf(optarg, "%lf", &t_aend);
			time_bounded = 1;
			break;
		case 'u':
			unplug_hist_name = optarg;
			break;
		case 'v':
			verbose = 1;
			break;
		case 'V':
			printf("%s version %s\n", argv[0], bt_timeline_version);
			exit(0);
		case 'X':
			easy_parse_avgs++;
			break;
		case 'z':
			q2d_name = optarg;
			break;
		default:
			usage(argv[0]);
			exit(1);
		}
	}

	if (input_name == NULL) {
		usage(argv[0]);
		exit(1);
	}

	if (sps_name && !seek_name) {
		fprintf(stderr, "FATAL: -m option requires -s options\n");
		exit(1);
	}

	setup_ifile(input_name);

	if (output_name == NULL) {
		rngs_ofp = avgs_ofp = msgs_ofp = stdout;
		easy_parse_avgs = 0;
	} else {
		rngs_ofp = std_open(output_name, "dat", "range data");
		avgs_ofp = std_open(output_name, "avg", "stats data");
		msgs_ofp = std_open(output_name, "msg", "K messages");
		if (easy_parse_avgs) {
			xavgs_ofp = std_open(output_name, "xvg",
					     "EZ stats data");
		}
	}

	iostat_ofp = setup_ofile(iostat_name);
	per_io_ofp = setup_ofile(per_io_name);
}
