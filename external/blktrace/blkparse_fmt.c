/*
 * This file contains format parsing code for blkparse, allowing you to
 * customize the individual action format and generel output format.
 */
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <ctype.h>
#include <time.h>

#include "blktrace.h"

#define VALID_SPECS	"ABCDFGIMPQRSTUWX"

#define HEADER		"%D %2c %8s %5T.%9t %5p %2a %3d "

static char *override_format[256];

static inline int valid_spec(int spec)
{
	return strchr(VALID_SPECS, spec) != NULL;
}

void set_all_format_specs(char *option)
{
	char *p;

	for (p = VALID_SPECS; *p; p++)
		if (override_format[(int)(*p)] == NULL)
			override_format[(int)(*p)] = strdup(option);
}

int add_format_spec(char *option)
{
	int spec = optarg[0];

	if (!valid_spec(spec)) {
		fprintf(stderr,"Bad format specifier %c\n", spec);
		return 1;
	}
	if (optarg[1] != ',') {
		fprintf(stderr,"Bad format specifier - need ',' %s\n", option);
		return 1;
	}
	option += 2;

	override_format[spec] = strdup(option);

	return 0;
}

static inline void fill_rwbs(char *rwbs, struct blk_io_trace *t)
{
	int w = t->action & BLK_TC_ACT(BLK_TC_WRITE);
	int a = t->action & BLK_TC_ACT(BLK_TC_AHEAD);
	int b = t->action & BLK_TC_ACT(BLK_TC_BARRIER);
	int s = t->action & BLK_TC_ACT(BLK_TC_SYNC);
	int m = t->action & BLK_TC_ACT(BLK_TC_META);
	int d = t->action & BLK_TC_ACT(BLK_TC_DISCARD);
	int i = 0;

	if (d)
		rwbs[i++] = 'D';
	else if (w)
		rwbs[i++] = 'W';
	else if (t->bytes)
		rwbs[i++] = 'R';
	else
		rwbs[i++] = 'N';
	if (a)
		rwbs[i++] = 'A';
	if (b)
		rwbs[i++] = 'B';
	if (s)
		rwbs[i++] = 'S';
	if (m)
		rwbs[i++] = 'M';

	rwbs[i] = '\0';
}

static const char *
print_time(unsigned long long timestamp)
{
	static char	timebuf[128];
	struct tm	*tm;
	time_t		sec;
	unsigned long	nsec;

	sec  = abs_start_time.tv_sec + SECONDS(timestamp);
	nsec = abs_start_time.tv_nsec + NANO_SECONDS(timestamp);
	if (nsec >= 1000000000) {
		nsec -= 1000000000;
		sec += 1;
	}

	tm = localtime(&sec);
	snprintf(timebuf, sizeof(timebuf),
			"%02u:%02u:%02u.%06lu",
			tm->tm_hour,
			tm->tm_min,
			tm->tm_sec,
			nsec / 1000);
	return timebuf;
}

static inline int pdu_rest_is_zero(unsigned char *pdu, int len)
{
	static char zero[4096];

	return !memcmp(pdu, zero, len);
}

static char *dump_pdu(unsigned char *pdu_buf, int pdu_len)
{
	static char p[4096];
	int i, len;

	if (!pdu_buf || !pdu_len)
		return NULL;

	for (len = 0, i = 0; i < pdu_len; i++) {
		if (i)
			len += sprintf(p + len, " ");

		len += sprintf(p + len, "%02x", pdu_buf[i]);

		/*
		 * usually dump for cdb dumps where we can see lots of
		 * zeroes, stop when the rest is just zeroes and indicate
		 * so with a .. appended
		 */
		if (!pdu_buf[i] && pdu_rest_is_zero(pdu_buf + i, pdu_len - i)) {
			sprintf(p + len, " ..");
			break;
		}
	}

	return p;
}

#define pdu_start(t)	(((void *) (t) + sizeof(struct blk_io_trace)))

static unsigned int get_pdu_int(struct blk_io_trace *t)
{
	__u64 *val = pdu_start(t);

	return be64_to_cpu(*val);
}

static void get_pdu_remap(struct blk_io_trace *t, struct blk_io_trace_remap *r)
{
	struct blk_io_trace_remap *__r = pdu_start(t);
	__u64 sector_from = __r->sector_from;

	r->device_from = be32_to_cpu(__r->device_from);
	r->device_to   = be32_to_cpu(__r->device_to);
	r->sector_from = be64_to_cpu(sector_from);
}

static void print_field(char *act, struct per_cpu_info *pci,
			struct blk_io_trace *t, unsigned long long elapsed,
			int pdu_len, unsigned char *pdu_buf, char field,
			int minus, int has_w, int width)
{
	char format[64];

	if (has_w) {
		if (minus)
			sprintf(format, "%%-%d", width);
		else
			sprintf(format, "%%%d", width);
	} else
		sprintf(format, "%%");

	switch (field) {
	case 'a':
		fprintf(ofp, strcat(format, "s"), act);
		break;
	case 'c':
		fprintf(ofp, strcat(format, "d"), pci->cpu);
		break;
	case 'C': {
		char *name = find_process_name(t->pid);

		fprintf(ofp, strcat(format, "s"), name);
		break;
	}
	case 'd': {
		char rwbs[6];

		fill_rwbs(rwbs, t);
		fprintf(ofp, strcat(format, "s"), rwbs);
		break;
	}
	case 'D':	/* format width ignored */
		fprintf(ofp,"%3d,%-3d", MAJOR(t->device), MINOR(t->device));
		break;
	case 'e':
		fprintf(ofp, strcat(format, "d"), t->error);
		break;
	case 'M':
		fprintf(ofp, strcat(format, "d"), MAJOR(t->device));
		break;
	case 'm':
		fprintf(ofp, strcat(format, "d"), MINOR(t->device));
		break;
	case 'n':
		fprintf(ofp, strcat(format, "u"), t_sec(t));
		break;
	case 'N':
		fprintf(ofp, strcat(format, "u"), t->bytes);
		break;
	case 'p':
		fprintf(ofp, strcat(format, "u"), t->pid);
		break;
	case 'P': { /* format width ignored */
		char *p = dump_pdu(pdu_buf, pdu_len);
		if (p)
			fprintf(ofp, "%s", p);
		break;
	}
	case 's':
		fprintf(ofp, strcat(format, "ld"), t->sequence);
		break;
	case 'S':
		fprintf(ofp, strcat(format, "lu"), t->sector);
		break;
	case 't':
		sprintf(format, "%%0%dlu", has_w ? width : 9);
		fprintf(ofp, format, NANO_SECONDS(t->time));
		break;
	case 'T':
		fprintf(ofp, strcat(format, "d"), SECONDS(t->time));
		break;
	case 'u':
		if (elapsed == -1ULL) {
			fprintf(stderr, "Expecting elapsed value\n");
			exit(1);
		}
		fprintf(ofp, strcat(format, "llu"), elapsed / 1000);
		break;
	case 'U':
		fprintf(ofp, strcat(format, "u"), get_pdu_int(t));
		break;
	case 'z':
		fprintf(ofp, strcat(format, "s"), print_time(t->time));
		break;
	default:
		fprintf(ofp,strcat(format, "c"), field);
		break;
	}
}

static char *parse_field(char *act, struct per_cpu_info *pci,
			 struct blk_io_trace *t, unsigned long long elapsed,
			 int pdu_len, unsigned char *pdu_buf,
			 char *master_format)
{
	int minus = 0;
	int has_w = 0;
	int width = 0;
	char *p = master_format;

	if (*p == '-') {
		minus = 1;
		p++;
	}
	if (isdigit(*p)) {
		has_w = 1;
		do {
			width = (width * 10) + (*p++ - '0');
		} while ((*p) && (isdigit(*p)));
	}
	if (*p) {
		print_field(act, pci, t, elapsed, pdu_len, pdu_buf, *p++,
			    minus, has_w, width);
	}
	return p;
}

static void process_default(char *act, struct per_cpu_info *pci,
			    struct blk_io_trace *t, unsigned long long elapsed,
			    int pdu_len, unsigned char *pdu_buf)
{
	struct blk_io_trace_remap r = { .device_from = 0, };
	char rwbs[6];
	char *name;

	fill_rwbs(rwbs, t);

	 /*
	  * For remaps we have to modify the device using the remap structure
	  * passed up.
	  */
	 if (act[0] == 'A') {
		 get_pdu_remap(t, &r);
		 t->device = r.device_to;
	 }

	/*
	 * The header is always the same
	 */
	fprintf(ofp, "%3d,%-3d %2d %8d %5d.%09lu %5u %2s %3s ",
		MAJOR(t->device), MINOR(t->device), pci->cpu, t->sequence,
		(int) SECONDS(t->time), (unsigned long) NANO_SECONDS(t->time),
		t->pid, act, rwbs);

	name = find_process_name(t->pid);

	switch (act[0]) {
	case 'R':	/* Requeue */
	case 'C': 	/* Complete */
		if (t->action & BLK_TC_ACT(BLK_TC_PC)) {
			char *p = dump_pdu(pdu_buf, pdu_len);
			if (p)
				fprintf(ofp, "(%s) ", p);
			fprintf(ofp, "[%d]\n", t->error);
		} else {
			if (elapsed != -1ULL) {
				if (t_sec(t))
					fprintf(ofp, "%llu + %u (%8llu) [%d]\n",
						(unsigned long long) t->sector,
						t_sec(t), elapsed, t->error);
				else
					fprintf(ofp, "%llu (%8llu) [%d]\n",
						(unsigned long long) t->sector,
						elapsed, t->error);
			} else {
				if (t_sec(t))
					fprintf(ofp, "%llu + %u [%d]\n",
						(unsigned long long) t->sector,
						t_sec(t), t->error);
				else
					fprintf(ofp, "%llu [%d]\n",
						(unsigned long long) t->sector,
						t->error);
			}
		}
		break;

	case 'D': 	/* Issue */
	case 'I': 	/* Insert */
	case 'Q': 	/* Queue */
	case 'B':	/* Bounce */
		if (t->action & BLK_TC_ACT(BLK_TC_PC)) {
			char *p;
			fprintf(ofp, "%u ", t->bytes);
			p = dump_pdu(pdu_buf, pdu_len);
			if (p)
				fprintf(ofp, "(%s) ", p);
			fprintf(ofp, "[%s]\n", name);
		} else {
			if (elapsed != -1ULL) {
				if (t_sec(t))
					fprintf(ofp, "%llu + %u (%8llu) [%s]\n",
						(unsigned long long) t->sector,
						t_sec(t), elapsed, name);
				else
					fprintf(ofp, "(%8llu) [%s]\n", elapsed,
						name);
			} else {
				if (t_sec(t))
					fprintf(ofp, "%llu + %u [%s]\n",
						(unsigned long long) t->sector,
						t_sec(t), name);
				else
					fprintf(ofp, "[%s]\n", name);
			}
		}
		break;

	case 'M':	/* Back merge */
	case 'F':	/* Front merge */
	case 'G':	/* Get request */
	case 'S':	/* Sleep request */
		if (t_sec(t))
			fprintf(ofp, "%llu + %u [%s]\n",
				(unsigned long long) t->sector, t_sec(t), name);
		else
			fprintf(ofp, "[%s]\n", name);
		break;

	case 'P':	/* Plug */
		fprintf(ofp, "[%s]\n", name);
		break;

	case 'U':	/* Unplug IO */
	case 'T': 	/* Unplug timer */
		fprintf(ofp, "[%s] %u\n", name, get_pdu_int(t));
		break;

	case 'A': 	/* remap */
		get_pdu_remap(t, &r);
		fprintf(ofp, "%llu + %u <- (%d,%d) %llu\n",
			(unsigned long long) t->sector, t_sec(t),
			MAJOR(r.device_from), MINOR(r.device_from),
			(unsigned long long) r.sector_from);
		break;

	case 'X': 	/* Split */
		fprintf(ofp, "%llu / %u [%s]\n", (unsigned long long) t->sector,
			get_pdu_int(t), name);
		break;

	case 'm':	/* Message */
		fprintf(ofp, "%*s\n", pdu_len, pdu_buf);
		break;

	default:
		fprintf(stderr, "Unknown action %c\n", act[0]);
		break;
	}

}

void process_fmt(char *act, struct per_cpu_info *pci, struct blk_io_trace *t,
		 unsigned long long elapsed, int pdu_len,
		 unsigned char *pdu_buf)
{
	char *p = override_format[(int) *act];

	if (!p) {
		process_default(act, pci, t, elapsed, pdu_len, pdu_buf);
		return;
	}

	while (*p) {
		switch (*p) {
		case '%': 	/* Field specifier */
			p++;
			if (*p == '%')
				fprintf(ofp, "%c", *p++);
			else if (!*p)
				fprintf(ofp, "%c", '%');
			else
				p = parse_field(act, pci, t, elapsed,
						pdu_len, pdu_buf, p);
			break;
		case '\\': {	/* escape */
			switch (p[1]) {
			case 'b': fprintf(ofp, "\b"); break;
			case 'n': fprintf(ofp, "\n"); break;
			case 'r': fprintf(ofp, "\r"); break;
			case 't': fprintf(ofp, "\t"); break;
			default:
				fprintf(stderr,	
					"Invalid escape char in format %c\n",
					p[1]);
				exit(1);
				/*NOTREACHED*/
			}
			p += 2;
			break;
		}
		default:
			fprintf(ofp, "%c", *p++);
			break;
		}
	}
}


