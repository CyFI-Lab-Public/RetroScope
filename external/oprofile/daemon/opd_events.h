/**
 * @file daemon/opd_events.h
 * Event details for each counter
 *
 * @remark Copyright 2002, 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OPD_EVENTS_H

#include "op_types.h"

#include <stdint.h>
#include <time.h>

/** event description for setup (perfmon) and mangling */
struct opd_event {
	char * name;
	unsigned long value;
	unsigned long counter;
	unsigned long count;
	unsigned long um;
	unsigned long kernel;
	unsigned long user;
};

/* needed for opd_perfmon.c */
extern struct opd_event opd_events[];

/** parse the events into the opd_events array */
void opd_parse_events(char const * events);

/** Find the event for the given counter */
struct opd_event * find_counter_event(unsigned long counter);

struct opd_header;

/** fill the sample file header with event info etc. */
void fill_header(struct opd_header * header, unsigned long counter,
		 vma_t anon_start, vma_t anon_end,
		 int is_kernel, int cg_to_is_kernel,
                 int spu_samples, uint64_t embed_offset, time_t mtime);

#endif /* OPD_EVENTS_H */
