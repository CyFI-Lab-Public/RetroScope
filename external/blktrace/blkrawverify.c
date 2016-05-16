/*
 * block queue tracing application
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
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>

#include "blktrace.h"

struct trace_info {
	int bit_field;
	char *string;
};

int data_is_native = -1;

#define TRACE_TO_STRING(f)	{.bit_field = f, .string = #f}
static struct trace_info traces[] = {
	TRACE_TO_STRING( BLK_TC_READ ),
	TRACE_TO_STRING( BLK_TC_WRITE ),
	TRACE_TO_STRING( BLK_TC_BARRIER ),
	TRACE_TO_STRING( BLK_TC_SYNC ),
	TRACE_TO_STRING( BLK_TC_QUEUE ),
	TRACE_TO_STRING( BLK_TC_REQUEUE ),
	TRACE_TO_STRING( BLK_TC_ISSUE ),
	TRACE_TO_STRING( BLK_TC_COMPLETE ),
	TRACE_TO_STRING( BLK_TC_FS ),
	TRACE_TO_STRING( BLK_TC_PC ),
	TRACE_TO_STRING( BLK_TC_AHEAD ),
	TRACE_TO_STRING( BLK_TC_META ),
	TRACE_TO_STRING( BLK_TC_DISCARD ),
};
#define N_TRACES (sizeof(traces) / sizeof(struct trace_info))

struct act_info {
	__u32 val;
	char *string;
};

#define ACT_TO_STRING(f)	{.val = f, .string = #f}
static struct act_info acts[] = {
	ACT_TO_STRING( __BLK_TA_QUEUE ),
	ACT_TO_STRING( __BLK_TA_QUEUE ),
	ACT_TO_STRING( __BLK_TA_BACKMERGE ),
	ACT_TO_STRING( __BLK_TA_FRONTMERGE ),
	ACT_TO_STRING( __BLK_TA_GETRQ ),
	ACT_TO_STRING( __BLK_TA_SLEEPRQ ),
	ACT_TO_STRING( __BLK_TA_REQUEUE ),
	ACT_TO_STRING( __BLK_TA_ISSUE ),
	ACT_TO_STRING( __BLK_TA_COMPLETE ),
	ACT_TO_STRING( __BLK_TA_PLUG ),
	ACT_TO_STRING( __BLK_TA_UNPLUG_IO ),
	ACT_TO_STRING( __BLK_TA_UNPLUG_TIMER ),
	ACT_TO_STRING( __BLK_TA_INSERT ),
	ACT_TO_STRING( __BLK_TA_SPLIT ),
	ACT_TO_STRING( __BLK_TA_BOUNCE ),
	ACT_TO_STRING( __BLK_TA_REMAP )
};
#define N_ACTS (sizeof(acts) / sizeof(struct act_info))

static char *act_to_str(__u32 action)
{
	static char buf[1024];
	unsigned int i;
	unsigned int act = action & 0xffff;
	unsigned int trace = (action >> BLK_TC_SHIFT) & 0xffff;

	if (act <= N_ACTS) {
		sprintf(buf, "%s ", acts[act].string);
		for (i = 0; i < N_TRACES; i++)
			if (trace & (1 << i)) {
				char buf2[1024];
				sprintf(buf2, "| %s ", traces[i].string);
				strcat(buf, buf2);
			}
	}
	else
		sprintf(buf, "Invalid action=%08x", action);

	return buf;
}

static void dump_trace(FILE *ofp, char *prefix, struct blk_io_trace *bit)
{
	fprintf(ofp, "    Dump %s\n", prefix);
	fprintf(ofp, "        %8s: %08x\n", "magic", bit->magic);
	fprintf(ofp, "        %8s: %u\n", "sequence", bit->sequence);
	fprintf(ofp, "        %8s: %llu\n", "time", (unsigned long long) bit->time);
	fprintf(ofp, "        %8s: %llu\n", "sector", (unsigned long long) bit->sector);
	fprintf(ofp, "        %8s: %u\n", "bytes", bit->bytes);
	fprintf(ofp, "        %8s: %s\n", "action", act_to_str(bit->action));
	fprintf(ofp, "        %8s: %u\n", "bytes", bit->bytes);
	fprintf(ofp, "        %8s: %u\n", "cpu", bit->cpu);
	fprintf(ofp, "        %8s: %u\n", "error", bit->error);
	fprintf(ofp, "        %8s: %u\n", "pdu_len", bit->pdu_len);
	fprintf(ofp, "        %8s: (%u,%u)\n\n", "device", MAJOR(bit->device),
						           MINOR(bit->device));
}

static int process(FILE **fp, char *devname, char *file, unsigned int cpu)
{
#	define SWAP_BITS() do {						\
		if (bit_save) {						\
			struct blk_io_trace *tmp = bit_save;		\
			bit_save = bit;					\
			bit = tmp;					\
		}							\
		else {							\
			bit_save = bit;					\
			bit = malloc(sizeof(struct blk_io_trace));	\
		}							\
	} while (0)

#	define INC_BAD(str) do {					\
		nbad++;							\
		fprintf(ofp, "    ----------------\n");			\
		if (bit_save) dump_trace(ofp,"seq-1",bit_save);		\
		dump_trace(ofp, str, bit);				\
		SWAP_BITS();						\
	} while (0)

	size_t n;
	FILE *ifp, *ofp;
	__u32 save_device = 0, save_sequence = 0;
	__u64 save_time = 0;
	struct blk_io_trace *bit_save = NULL;
	struct blk_io_trace *bit = malloc(sizeof(struct blk_io_trace));
	unsigned int ngood = 0;
	unsigned int nbad = 0;
	unsigned int nbad_trace = 0, nbad_pdu = 0, nbad_cpu = 0;
	unsigned int nbad_seq = 0, nbad_dev = 0, nbad_time = 0;
	char ofname[1024];

	ifp = fopen(file, "r");
	if (!ifp)
		return 0;

	sprintf(ofname, "%s.verify.out", devname);

	if (!*fp) {
		*fp = fopen(ofname, "w");
		if (*fp == NULL) {
			fprintf(stderr,"Failed to open %s (%s), skipping\n",
				ofname, strerror(errno));
			fclose(ifp);
			return 0;
		}
		fprintf(*fp, "\n---------------\n" );
		fprintf(*fp, "Verifying %s\n", devname);
	}

	ofp = *fp;
	while ((n = fread(bit, sizeof(struct blk_io_trace), 1, ifp)) == 1) {
		if (ferror(ifp)) {
			clearerr(ifp);
			perror("fread");
			break;
		}
		if (data_is_native == -1)
			check_data_endianness(bit->magic);

		trace_to_cpu(bit);

		if (!CHECK_MAGIC(bit)) {
			INC_BAD("bad trace");
			continue;
		}

		if ((bit->magic & 0xff) != SUPPORTED_VERSION) {
			fprintf(stderr, "unsupported trace version\n");
			break;
		}

		if (bit->pdu_len) {
			char *pdu_buf;

			pdu_buf = malloc(bit->pdu_len);
			n = fread(pdu_buf, bit->pdu_len, 1, ifp);
			if (n == 0) {
				INC_BAD("bad pdu");
				nbad_seq++;
				break;
			}
			free(pdu_buf);
		}

		if (bit->cpu != cpu) {
			INC_BAD("bad cpu");
			nbad_cpu++;
			continue;
		}

		/*
		 * skip notify traces, they don't have valid sequences
		 */
		if (bit->action & BLK_TC_ACT(BLK_TC_NOTIFY))
			continue;

		if (ngood) {
			if (bit->sequence <= save_sequence) {
				INC_BAD("bad seq");
				nbad_seq++;
				continue;
			}
			else if (bit->time <= save_time) {
				INC_BAD("time regression");
				nbad_time++;
				continue;
			}
			else if (bit->device != save_device) {
				INC_BAD("bad dev");
				nbad_dev++;
				continue;
			}
		}

		save_sequence = bit->sequence;
		save_time = bit->time;
		save_device = bit->device;

		ngood++;
		SWAP_BITS();
	}

	if (n == 0 && !feof(ifp))
		fprintf(stderr,"%s: fread failed %d/%s\n",
		        file, errno, strerror(errno));
	fclose(ifp);

	fprintf(ofp, "    ---------------------\n");
	fprintf(ofp, "    Summary for cpu %d:\n", cpu);
	fprintf(ofp, "    %10d valid + %10d invalid (%5.1f%%) processed\n\n",
		ngood, nbad,
		ngood ? 100.0 * (float)ngood / (float)(ngood + nbad) : 0.0);

	if (nbad) {
		if (nbad_trace)
			fprintf(ofp, "%8s %d traces\n", "", nbad_trace);
		if (nbad_trace)
			fprintf(ofp, "%8s %d pdu\n", "", nbad_pdu);
		if (nbad_cpu)
			fprintf(ofp, "%8s %d cpu\n", "", nbad_cpu);
		if (nbad_seq)
			fprintf(ofp, "%8s %d seq\n", "", nbad_seq);
		if (nbad_dev)
			fprintf(ofp, "%8s %d dev\n", "", nbad_dev);
		if (nbad_time)
			fprintf(ofp, "%8s %d time\n", "", nbad_time);
		fprintf(ofp,"\n");
	}

	return nbad;
}

int main(int argc, char *argv[])
{
	char *devname;
	struct stat st;
	int i, cpu, nbad, rval = 0;
	FILE *ofp;
	char *ofname = malloc(1024);
	char *fname = malloc(1024);

	if (argc < 2) {
		fprintf(stderr,"FATAL: Need device name(s)\n");
		fprintf(stderr,"Usage: blkrawverify <dev> [<dev>...]\n");
		exit(1);
	}

	for (i = 1; i < argc; i++) {
		devname = argv[i];
		sprintf(ofname, "%s.verify.out", devname);
		ofp = NULL;

		printf("Verifying %s\n", devname); fflush(stdout);
		for (cpu = 0; ; cpu++) {
			sprintf(fname, "%s.blktrace.%d", devname, cpu);
			if (stat(fname, &st) < 0) {
				if (cpu == 0) {
					fprintf(stderr, "No tracefiles found for %s\n",
						devname);
					rval = 1;
				}
				break;
			}
			printf("    CPU %d ", cpu); fflush(stdout);
			nbad = process(&ofp, devname, fname, cpu);
			if (nbad) {
				printf("-- %d bad", nbad);
				rval = 1;
			}
			printf("\n");
		}
		if (ofp) {
			fclose(ofp);
			fprintf(stdout, "Wrote output to %s\n", ofname);
		}
	}

	return rval;
}
