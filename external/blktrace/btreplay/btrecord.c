/*
 * Blktrace record utility - Convert binary trace data into bunches of IOs
 *
 * Copyright (C) 2007 Alan D. Brunelle <Alan.Brunelle@hp.com>
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
 */

static char build_date[] = __DATE__ " at "__TIME__;

#include <assert.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/param.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <dirent.h>
#include <stdarg.h>

#if !defined(_GNU_SOURCE)
#	define _GNU_SOURCE
#endif
#include <getopt.h>

#include "list.h"
#include "btrecord.h"
#include "blktrace.h"

/*
 * Per input file information
 *
 * @head: 	Used to link up on input_files
 * @devnm: 	Device name portion of this input file
 * @file_name: 	Fully qualified name for this input file
 * @cpu: 	CPU that this file was collected on
 * @ifd: 	Input file descriptor (when opened)
 * @tpkts: 	Total number of packets processed.
 */
struct ifile_info {
	struct list_head head;
	char *devnm, *file_name;
	int cpu, ifd;
	__u64 tpkts, genesis;
};

/*
 * Per IO trace information
 *
 * @time: 	Time stamp when trace was emitted
 * @sector: 	IO sector identifier
 * @bytes: 	Number of bytes transferred
 * @rw: 	Read (1) or write (0) 
 */
struct io_spec {
	__u64 time;
	__u64 sector;
	__u32 bytes;
	int rw;
};

/*
 * Per output file information
 *
 * @ofp: 	Output file 
 * @vfp:	Verbose output file
 * @file_name: 	Fully qualified name for this file
 * @vfn:	Fully qualified name for this file
 * @cur: 	Current IO bunch being collected
 * @iip: 	Input file this is associated with
 * @start_time: Start time of th ecurrent bunch
 * @last_time: 	Time of last packet put in
 * @bunches: 	Number of bunches processed
 * @pkts: 	Number of packets stored in bunches
 */
struct io_stream {
	FILE *ofp, *vfp;
	char *file_name, *vfn;
	struct io_bunch *cur;
	struct ifile_info *iip;
	__u64 start_time, last_time, bunches, pkts;
};

int data_is_native;				// Indicates whether to swap
static LIST_HEAD(input_files);			// List of all input files
static char *idir = ".";			// Input directory base
static char *odir = ".";			// Output directory base
static char *obase = "replay";			// Output file base
static __u64 max_bunch_tm = (10 * 1000 * 1000);	// 10 milliseconds
static __u64 max_pkts_per_bunch = 8;		// Default # of pkts per bunch
static int verbose = 0;				// Boolean: output stats
static int find_traces = 0;			// Boolean: Find traces in dir

static char usage_str[] =                                                  \
        "\n"                                                               \
	"\t[ -d <dir>  : --input-directory=<dir> ] Default: .\n"           \
	"\t[ -D <dir>  : --output-directory=<dir>] Default: .\n"           \
	"\t[ -F        : --find-traces           ] Default: Off\n"         \
        "\t[ -h        : --help                  ] Default: Off\n"         \
        "\t[ -m <nsec> : --max-bunch-time=<nsec> ] Default: 10 msec\n"     \
	"\t[ -M <pkts> : --max-pkts=<pkts>       ] Default: 8\n"           \
        "\t[ -o <base> : --output-base=<base>    ] Default: replay\n"      \
        "\t[ -v        : --verbose               ] Default: Off\n"         \
        "\t[ -V        : --version               ] Default: Off\n"         \
	"\t<dev>...                                Default: None\n"	   \
        "\n";

#define S_OPTS	"d:D:Fhm:M:o:vV"
static struct option l_opts[] = {
	{
		.name = "input-directory",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'd'
	},
	{
		.name = "output-directory",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'D'
	},
	{
		.name = "find-traces",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'F'
	},
	{
		.name = "help",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'h'
	},
	{
		.name = "max-bunch-time",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'm'
	},
	{
		.name = "max-pkts",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'M'
	},
	{
		.name = "output-base",
		.has_arg = required_argument,
		.flag = NULL,
		.val = 'o'
	},
	{
		.name = "verbose",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'v'
	},
	{
		.name = "version",
		.has_arg = no_argument,
		.flag = NULL,
		.val = 'V'
	},
	{
		.name = NULL
	}
};

#define ERR_ARGS			1
#define ERR_SYSCALL			2
static inline void fatal(const char *errstring, const int exitval,
			 const char *fmt, ...)
{
	va_list ap;

	if (errstring)
		perror(errstring);

	va_start(ap, fmt);
	vfprintf(stderr, fmt, ap);
	va_end(ap);

	exit(exitval);
	/*NOTREACHED*/
}

/**
 * match - Return true if this trace is a proper QUEUE transaction
 * @action: Action field from trace
 */
static inline int match(__u32 action)
{
	return ((action & 0xffff) == __BLK_TA_QUEUE) &&
				       (action & BLK_TC_ACT(BLK_TC_QUEUE));
}

/**
 * usage - Display usage string and version
 */
static void usage(void)
{
	fprintf(stderr, "Usage: btrecord -- version %s\n%s", 
		my_btversion, usage_str);
}

/**
 * write_file_hdr - Seek to and write btrecord file header
 * @stream: Output file information
 * @hdr: Header to write
 */
static void write_file_hdr(struct io_stream *stream, struct io_file_hdr *hdr)
{
	hdr->version = mk_btversion(btver_mjr, btver_mnr, btver_sub);

	if (verbose) {
		fprintf(stderr, "\t%s: %llx %llx %llx %llx\n", 
			stream->file_name,
			(long long unsigned)hdr->version,
			(long long unsigned)hdr->genesis,
			(long long unsigned)hdr->nbunches,
			(long long unsigned)hdr->total_pkts);
	}

	fseek(stream->ofp, 0, SEEK_SET);
	if (fwrite(hdr, sizeof(*hdr), 1, stream->ofp) != 1) {
		fatal(stream->file_name, ERR_SYSCALL, "Hdr write failed\n");
		/*NOTREACHED*/
	}
}

/**
 * io_bunch_create - Allocate & initialize an io_bunch
 * @io_stream: IO stream being added to
 * @pre_stall: Amount of time that this bunch should be delayed by
 * @start_time: Records current start 
 */
static inline void io_bunch_create(struct io_stream *stream, __u64 start_time)
{
	struct io_bunch *cur = malloc(sizeof(*cur));

	memset(cur, 0, sizeof(*cur));

	cur->hdr.npkts = 0;
	cur->hdr.time_stamp = stream->start_time = start_time;

	stream->cur = cur;
}

/**
 * io_bunch_add - Add an IO to the current bunch of IOs
 * @stream: Per-output file stream information
 * @spec: IO trace specification
 *
 * Returns update bunch information
 */
static void io_bunch_add(struct io_stream *stream, struct io_spec *spec)
{
	struct io_bunch *cur = stream->cur;
	struct io_pkt iop = {
		.sector = spec->sector,
		.nbytes = spec->bytes,
		.rw = spec->rw
	};

	assert(cur != NULL);
	assert(cur->hdr.npkts < BT_MAX_PKTS);
	assert(stream->last_time == 0 || stream->last_time <= spec->time);

	cur->pkts[cur->hdr.npkts++] = iop;	// Struct copy
	stream->last_time = spec->time;
}

/**
 * rem_input_file - Release resources associated with an input file
 * @iip: Per-input file information
 */
static void rem_input_file(struct ifile_info *iip)
{
	list_del(&iip->head);

	close(iip->ifd);
	free(iip->file_name);
	free(iip->devnm);
	free(iip);
}

/**
 * __add_input_file - Allocate and initialize per-input file structure
 * @cpu: CPU for this file
 * @devnm: Device name for this file
 * @file_name: Fully qualifed input file name
 */
static void __add_input_file(int cpu, char *devnm, char *file_name)
{
	struct ifile_info *iip = malloc(sizeof(*iip));

	iip->cpu = cpu;
	iip->tpkts = 0;
	iip->genesis = 0;
	iip->devnm = strdup(devnm);
	iip->file_name = strdup(file_name);
	iip->ifd = open(file_name, O_RDONLY);
	if (iip->ifd < 0) {
		fatal(file_name, ERR_ARGS, "Unable to open\n");
		/*NOTREACHED*/
	}

	list_add_tail(&iip->head, &input_files);
}

/**
 * add_input_file - Set up the input file name
 * @devnm: Device name to use
 */
static void add_input_file(char *devnm)
{
	struct list_head *p;
	int cpu, found = 0;

	__list_for_each(p, &input_files) {
		struct ifile_info *iip = list_entry(p, struct ifile_info, head);
		if (strcmp(iip->devnm, devnm) == 0)
			return;
	}

	for (cpu = 0; ; cpu++) {
		char full_name[MAXPATHLEN];

		sprintf(full_name, "%s/%s.blktrace.%d", idir, devnm, cpu);
		if (access(full_name, R_OK) != 0)
			break;

		__add_input_file(cpu, devnm, full_name);
		found++;
	}

	if (!found) {
		fatal(NULL, ERR_ARGS, "No traces found for %s\n", devnm);
		/*NOTREACHED*/
	}
}

static void find_input_files(char *idir)
{
	struct dirent *ent;
	DIR *dir = opendir(idir);

	if (dir == NULL) {
		fatal(idir, ERR_ARGS, "Unable to open %s\n", idir);
		/*NOTREACHED*/
	}

	while ((ent = readdir(dir)) != NULL) {
		char *p, *dsf = malloc(256);

		if (strstr(ent->d_name, ".blktrace.") == NULL)
			continue;

		dsf = strdup(ent->d_name);
		p = index(dsf, '.');
		assert(p != NULL);
		*p = '\0';
		add_input_file(dsf);
		free(dsf);
	}

	closedir(dir);
}

/**
 * handle_args - Parse passed in argument list
 * @argc: Number of arguments in argv
 * @argv: Arguments passed in
 *
 * Does rudimentary parameter verification as well.
 */
void handle_args(int argc, char *argv[])
{
	int c;

	while ((c = getopt_long(argc, argv, S_OPTS, l_opts, NULL)) != -1) {
		switch (c) {
		case 'd':
			idir = optarg;
			if (access(idir, R_OK | X_OK) != 0) {
				fatal(idir, ERR_ARGS, 
				      "Invalid input directory specified\n");
				/*NOTREACHED*/
			}
			break;

		case 'D':
			odir = optarg;
			if (access(odir, R_OK | X_OK) != 0) {
				fatal(odir, ERR_ARGS, 
				      "Invalid output directory specified\n");
				/*NOTREACHED*/
			}
			break;

		case 'F': 
			find_traces = 1;
			break;

		case 'h': 
			usage(); 
			exit(0);
			/*NOTREACHED*/

		case 'm':
			max_bunch_tm = (__u64)atoll(optarg);
			if (max_bunch_tm < 1) {
				fprintf(stderr, "Invalid bunch time %llu\n",
					(unsigned long long)max_bunch_tm);
				exit(ERR_ARGS);
				/*NOTREACHED*/
			}
			break;

		case 'M':
			max_pkts_per_bunch = (__u64)atoll(optarg);
			if (!((1 <= max_pkts_per_bunch) && 
						(max_pkts_per_bunch < 513))) {
				fprintf(stderr, "Invalid max pkts %llu\n",
					(unsigned long long)max_pkts_per_bunch);
				exit(ERR_ARGS);
				/*NOTREACHED*/
			}
			break;

		case 'o':
			obase = optarg;
			break;

		case 'V':
			fprintf(stderr, "btrecord -- version %s\n", 
				my_btversion);
			fprintf(stderr, "            Built on %s\n", build_date);
			exit(0);
			/*NOTREACHED*/

		case 'v':
			verbose++;
			break;

		default:
			usage();
			fatal(NULL, ERR_ARGS, "Invalid command line\n");
			/*NOTREACHED*/
		}
	}

	while (optind < argc)
		add_input_file(argv[optind++]);

	if (find_traces)
		find_input_files(idir);

	if (list_len(&input_files) == 0) {
		fatal(NULL, ERR_ARGS, "Missing required input file name(s)\n");
		/*NOTREACHED*/
	}
}

/**
 * next_io - Retrieve next Q trace from input stream
 * @iip: Per-input file information
 * @spec: IO specifier for trace
 *
 * Returns 0 on end of file, 1 if valid data returned.
 */
static int next_io(struct ifile_info *iip, struct io_spec *spec)
{
	ssize_t ret;
	__u32 action;
	__u16 pdu_len;
	struct blk_io_trace t;

again:
	ret = read(iip->ifd, &t, sizeof(t));
	if (ret < 0) {
		fatal(iip->file_name, ERR_SYSCALL, "Read failed\n");
		/*NOTREACHED*/
	}
	else if (ret == 0)
		return 0;
	else if (ret < (ssize_t)sizeof(t)) {
		fprintf(stderr, "WARNING: Short read on %s (%d)\n", 
			iip->file_name, (int)ret);
		return 0;
	}

	if (data_is_native == -1)
		check_data_endianness(t.magic);

	assert(data_is_native >= 0);
	if (data_is_native) {
		spec->time = t.time;
		spec->sector = t.sector;
		spec->bytes = t.bytes;
		action = t.action;
		pdu_len = t.pdu_len;
	}
	else {
		spec->time = be64_to_cpu(t.time);
		spec->sector = be64_to_cpu(t.sector);
		spec->bytes = be32_to_cpu(t.bytes);
		action = be32_to_cpu(t.action);
		pdu_len = be16_to_cpu(t.pdu_len);
	}


	if (pdu_len) {
		char buf[pdu_len];

		ret = read(iip->ifd, buf, pdu_len);
		if (ret < 0) {
			fatal(iip->file_name, ERR_SYSCALL, "Read PDU failed\n");
			/*NOTREACHED*/
		}
		else if (ret < (ssize_t)pdu_len) {
			fprintf(stderr, "WARNING: Short PDU read on %s (%d)\n", 
				iip->file_name, (int)ret);
			return 0;
		}
	}

	iip->tpkts++;
	if (!match(action))
		goto again;

	spec->rw = (action & BLK_TC_ACT(BLK_TC_READ)) ? 1 : 0;
	if (verbose > 1)
		fprintf(stderr, "%2d: %10llu+%10llu (%d) @ %10llx\n",
			iip->cpu, (long long unsigned)spec->sector,
			(long long unsigned)spec->bytes / 512LLU,
			spec->rw, (long long unsigned)spec->time);

	if (iip->genesis == 0) {
		iip->genesis = spec->time;
		if (verbose > 1)
			fprintf(stderr, "\tSetting new genesis: %llx(%d)\n",
				(long long unsigned)iip->genesis, iip->cpu);
	}
	else if (iip->genesis > spec->time)
		fatal(NULL, ERR_SYSCALL, 
			"Time inversion? %llu ... %llu\n",
			(long long unsigned )iip->genesis, 
			(long long unsigned )spec->time);

	return 1;
}

/**
 * bunch_output_hdr - Output bunch header
 */
static inline void bunch_output_hdr(struct io_stream *stream)
{
	struct io_bunch_hdr *hdrp = &stream->cur->hdr;

	assert(0 < hdrp->npkts && hdrp->npkts <= BT_MAX_PKTS);
	if (fwrite(hdrp, sizeof(struct io_bunch_hdr), 1, stream->ofp) != 1) {
		fatal(stream->file_name, ERR_SYSCALL, "fwrite(hdr) failed\n");
		/*NOTREACHED*/
	}

	if (verbose) {
		__u64 off = hdrp->time_stamp - stream->iip->genesis;

		assert(stream->vfp);
		fprintf(stream->vfp, "------------------\n");
		fprintf(stream->vfp, "%4llu.%09llu %3llu\n", 
			(unsigned long long)off / (1000 * 1000 * 1000),
			(unsigned long long)off % (1000 * 1000 * 1000), 
			(unsigned long long)hdrp->npkts);
		fprintf(stream->vfp, "------------------\n");
	}
}

/**
 * bunch_output_pkt - Output IO packets
 */
static inline void bunch_output_pkts(struct io_stream *stream)
{
	struct io_pkt *p = stream->cur->pkts;
	size_t npkts = stream->cur->hdr.npkts;

	assert(0 < npkts && npkts <= BT_MAX_PKTS);
	if (fwrite(p, sizeof(struct io_pkt), npkts, stream->ofp) != npkts) {
		fatal(stream->file_name, ERR_SYSCALL, "fwrite(pkts) failed\n");
		/*NOTREACHED*/
	}

	if (verbose) {
		size_t i;

		assert(stream->vfp);
		for (i = 0; i < npkts; i++, p++)
			fprintf(stream->vfp, "\t%1d %10llu\t%10llu\n",
				p->rw, 
				(unsigned long long)p->sector,
				(unsigned long long)p->nbytes / 512);
	}
}

/**
 * stream_flush - Flush current bunch of IOs out to the output stream
 * @stream: Per-output file stream information
 */
static void stream_flush(struct io_stream *stream)
{
	struct io_bunch *cur = stream->cur;

	if (cur) {
		if (cur->hdr.npkts) {
			assert(cur->hdr.npkts <= BT_MAX_PKTS);
			bunch_output_hdr(stream);
			bunch_output_pkts(stream);

			stream->bunches++;
			stream->pkts += cur->hdr.npkts;
		}
		free(cur);
	}
}

/**
 * bunch_done - Returns true if current bunch is either full, or next IO is late
 * @stream: Output stream information
 * @spec: IO trace specification
 */
static inline int bunch_done(struct io_stream *stream, struct io_spec *spec)
{
	if (stream->cur->hdr.npkts >= max_pkts_per_bunch)
		return 1;

	if ((spec->time - stream->start_time) > max_bunch_tm)
		return 1;

	return 0;
}

/**
 * stream_add_io - Add an IO trace to the current stream
 * @stream: Output stream information
 * @spec: IO trace specification
 */
static void stream_add_io(struct io_stream *stream, struct io_spec *spec)
{

	if (stream->cur == NULL)
		io_bunch_create(stream, spec->time);
	else if (bunch_done(stream, spec)) {
		stream_flush(stream);
		io_bunch_create(stream, spec->time);
	}

	io_bunch_add(stream, spec);
}

/**
 * stream_open - Open output stream for specified input stream
 * @iip: Per-input file information
 */
static struct io_stream *stream_open(struct ifile_info *iip)
{
	char ofile_name[MAXPATHLEN];
	struct io_stream *stream = malloc(sizeof(*stream));
	struct io_file_hdr io_file_hdr = {
		.genesis = 0,
		.nbunches = 0,
		.total_pkts = 0
	};

	memset(stream, 0, sizeof(*stream));

	sprintf(ofile_name, "%s/%s.%s.%d", odir, iip->devnm, obase, iip->cpu);
	stream->ofp = fopen(ofile_name, "w");
	if (!stream->ofp) {
		fatal(ofile_name, ERR_SYSCALL, "Open failed\n");
		/*NOTREACHED*/
	}

	stream->iip = iip;
	stream->cur = NULL;
	stream->bunches = stream->pkts = 0;
	stream->last_time = 0;
	stream->file_name = strdup(ofile_name);

	write_file_hdr(stream, &io_file_hdr);

	if (verbose) {
		char vfile_name[MAXPATHLEN];

		sprintf(vfile_name, "%s/%s.%s.%d.rec", odir, iip->devnm,
			obase, iip->cpu);
		stream->vfp = fopen(vfile_name, "w");
		if (!stream->vfp) {
			fatal(vfile_name, ERR_SYSCALL, "Open failed\n");
			/*NOTREACHED*/
		}

		stream->vfn = strdup(vfile_name);
	}

	data_is_native = -1;
	return stream;
}

/**
 * stream_close - Release resources associated with an output stream
 * @stream: Stream to release
 */
static void stream_close(struct io_stream *stream)
{
	struct io_file_hdr io_file_hdr = {
		.genesis = stream->iip->genesis,
		.nbunches = stream->bunches,
		.total_pkts = stream->pkts
	};

	stream_flush(stream);
	write_file_hdr(stream, &io_file_hdr);
	fclose(stream->ofp);

	if (verbose && stream->bunches) {
		fprintf(stderr, 
			"%s:%d: %llu pkts (tot), %llu pkts (replay), "
					"%llu bunches, %.1lf pkts/bunch\n",
			stream->iip->devnm, stream->iip->cpu,
			(unsigned long long)stream->iip->tpkts, 
			(unsigned long long)stream->pkts, 
			(unsigned long long)stream->bunches,
			(double)(stream->pkts) / (double)(stream->bunches));

		fclose(stream->vfp);
		free(stream->vfn);
	}

	free(stream->file_name);
	free(stream);
}

/**
 * process - Process one input file to an output file
 * @iip: Per-input file information
 */
static void process(struct ifile_info *iip)
{
	struct io_spec spec;
	struct io_stream *stream;

	stream = stream_open(iip);
	while (next_io(iip, &spec))
		stream_add_io(stream, &spec);
	stream_close(stream);

	rem_input_file(iip);
}

/**
 * main - 
 * @argc: Number of arguments
 * @argv: Array of arguments
 */
int main(int argc, char *argv[])
{
	struct list_head *p, *q;

	handle_args(argc, argv);
	list_for_each_safe(p, q, &input_files)
		process(list_entry(p, struct ifile_info, head));

	return 0;
}
