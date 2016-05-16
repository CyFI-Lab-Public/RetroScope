/**
 * @file daemon/opd_events.c
 * Event details for each counter
 *
 * @remark Copyright 2002, 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include "config.h"
 
#include "opd_events.h"
#include "opd_printf.h"
#include "opd_extended.h"
#include "oprofiled.h"

#include "op_string.h"
#include "op_config.h"
#include "op_cpufreq.h"
#include "op_cpu_type.h"
#include "op_libiberty.h"
#include "op_hw_config.h"
#include "op_sample_file.h"

#include <stdlib.h>
#include <stdio.h>

extern op_cpu cpu_type;

struct opd_event opd_events[OP_MAX_COUNTERS];

static double cpu_speed;

static void malformed_events(void)
{
	fprintf(stderr, "oprofiled: malformed events passed "
		"on the command line\n");
	exit(EXIT_FAILURE);
}


static char * copy_token(char ** c, char delim)
{
	char * tmp = *c;
	char * tmp2 = *c;
	char * str;

	if (!**c)
		return NULL;

	while (*tmp2 && *tmp2 != delim)
		++tmp2;

	if (tmp2 == tmp)
		return NULL;

	str = op_xstrndup(tmp, tmp2 - tmp);
	*c = tmp2;
	if (**c)
		++*c;
	return str;
}


static unsigned long copy_ulong(char ** c, char delim)
{
	unsigned long val = 0;
	char * str = copy_token(c, delim);
	if (!str)
		malformed_events();
	val = strtoul(str, NULL, 0);
	free(str);
	return val;
}


void opd_parse_events(char const * events)
{
	char * ev = xstrdup(events);
	char * c;
	size_t cur = 0;

	if (cpu_type == CPU_TIMER_INT) {
		struct opd_event * event = &opd_events[0];
		event->name = xstrdup("TIMER");
		event->value = event->counter
			= event->count = event->um = 0;
		event->kernel = 1;
		event->user = 1;
		return;
	}

	if (!ev || !strlen(ev)) {
		fprintf(stderr, "oprofiled: no events passed.\n");
		exit(EXIT_FAILURE);
	}

	verbprintf(vmisc, "Events: %s\n", ev);

	c = ev;

	while (*c && cur < op_nr_counters) {
		struct opd_event * event = &opd_events[cur];

		if (!(event->name = copy_token(&c, ':')))
			malformed_events();
		event->value = copy_ulong(&c, ':');
		event->counter = copy_ulong(&c, ':');
		event->count = copy_ulong(&c, ':');
		event->um = copy_ulong(&c, ':');
		event->kernel = copy_ulong(&c, ':');
		event->user = copy_ulong(&c, ',');
		++cur;
	}

	if (*c) {
		fprintf(stderr, "oprofiled: too many events passed.\n");
		exit(EXIT_FAILURE);
	}

	free(ev);

	cpu_speed = op_cpu_frequency();
}


struct opd_event * find_counter_event(unsigned long counter)
{
	size_t i;
	struct opd_event * ret = NULL;

	if (counter >= OP_MAX_COUNTERS) {
		if((ret = opd_ext_find_counter_event(counter)) != NULL)
			return ret;
	}

	for (i = 0; i < op_nr_counters && opd_events[i].name; ++i) {
		if (counter == opd_events[i].counter)
			return &opd_events[i];
	}

	fprintf(stderr, "Unknown event for counter %lu\n", counter);
	abort();
	return NULL;
}


void fill_header(struct opd_header * header, unsigned long counter,
		 vma_t anon_start, vma_t cg_to_anon_start,
		 int is_kernel, int cg_to_is_kernel,
		 int spu_samples, uint64_t embed_offset, time_t mtime)
{
	struct opd_event * event = find_counter_event(counter);

	memset(header, '\0', sizeof(struct opd_header));
	header->version = OPD_VERSION;
	memcpy(header->magic, OPD_MAGIC, sizeof(header->magic));
	header->cpu_type = cpu_type;
	header->ctr_event = event->value;
	header->ctr_count = event->count;
	header->ctr_um = event->um;
	header->is_kernel = is_kernel;
	header->cg_to_is_kernel = cg_to_is_kernel;
	header->cpu_speed = cpu_speed;
	header->mtime = mtime;
	header->anon_start = anon_start;
	header->spu_profile = spu_samples;
	header->embedded_offset = embed_offset;
	header->cg_to_anon_start = cg_to_anon_start;
}
