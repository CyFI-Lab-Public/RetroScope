/**
 * @file alloc_counter_tests.c
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <stdlib.h>
#include <stdio.h>

#include "op_parse_event.h"
#include "op_alloc_counter.h"
#include "op_events.h"
#include "op_hw_config.h"
#include "op_cpu_type.h"
#include "op_events.h"

/* FIXME: alpha description events need 20 but when running test on x86
 * OP_MAX_COUNTERS is 8, so we can't use it */
#define MAX_EVENTS 20


/* some test are setup to fail in a known way */
enum failure_type {
	no_failure,
	fail_to_find_event,
	fail_to_alloc_counter
};

struct allocated_counter {
	op_cpu cpu_type;
	char  const * const * events;
	size_t alloc_map[MAX_EVENTS];
	/* expected failure for this test */
	enum failure_type failure;
};


/* not more than MAX_EVENTS string for all these arrays */
static char const * const events_alpha_ev4_1[] = {
	"ISSUES:4096:0:1:1",
	NULL
};

static char const * const events_alpha_ev4_2[] = {
	"UNKNOWN_EVENT:4096:0:1:1",
	NULL
};

static char const * const events_ppro_1[] = {
	"CPU_CLK_UNHALTED:4096:0:1:1",
	NULL
};

static char const * const events_ppro_2[] = {
	"CPU_CLK_UNHALTED:4096:0:1:1",
	"DATA_MEM_REFS:4096:0:1:1",
	NULL
};

static char const * const events_ppro_3[] = {
	/* fail_to_alloc_counter: 2 event to counter 0 */
	"COMP_FLOP_RET:4096:0:1:1",
	"FLOPS:4096:0:1:1",
	NULL
};

static char const * const events_ppro_4[] = {
	"FLOPS:4096:0:1:1",
	"FP_ASSIST:4096:0:1:1",
	NULL
};

static char const * const events_ppro_5[] = {
	"FP_ASSIST:4096:0:1:1",
	"FLOPS:4096:0:1:1",
	NULL
};

static char const * const events_p4_1[] = {
	"BRANCH_RETIRED:4096:1:1:1",
	"MISPRED_BRANCH_RETIRED:4096:1:1:1",
	"BPU_FETCH_REQUEST:4096:1:1:1",
	"ITLB_REFERENCE:4096:1:1:1",
	"MEMORY_CANCEL:4096:4:1:1",
	"MEMORY_COMPLETE:4096:1:1:1",
	"TC_MS_XFER:4096:1:1:1",
	"UOP_QUEUE_WRITES:4096:1:1:1",
	NULL
};

static char const * const events_p4_2[] = {
	/* fail_to_alloc_counter: 3 event to counter 3, 7 */
	"BRANCH_RETIRED:4096:1:1:1",
	"MISPRED_BRANCH_RETIRED:4096:1:1:1",
	"INSTR_RETIRED:4096:1:1:1",
	"BPU_FETCH_REQUEST:4096:1:1:1",
	"ITLB_REFERENCE:4096:1:1:1",
	"MEMORY_CANCEL:4096:4:1:1",
	"MEMORY_COMPLETE:4096:1:1:1",
	"TC_MS_XFER:4096:1:1:1",
	NULL
};

static char const * const events_mips_34k[] = {
	/* fail_to_alloc_counter: w/o 2006-8-03  Jeremiah Lott patch, see
	 * ChangeLog */
	"DTLB_MISSES:500:0:1:1",
	"JR_31_INSNS:500:0:1:1",
	NULL
};

static struct allocated_counter const tests[] = {
	{ CPU_AXP_EV4, events_alpha_ev4_1, { 0 }, no_failure },
	{ CPU_AXP_EV4, events_alpha_ev4_2, { -1 }, fail_to_find_event },
	{ CPU_PPRO, events_ppro_1, { 0 }, no_failure },
	{ CPU_PPRO, events_ppro_2, { 0, 1 }, no_failure },
	{ CPU_PPRO, events_ppro_3, { -1 }, fail_to_alloc_counter },
	{ CPU_PPRO, events_ppro_4, { 0, 1 }, no_failure },
	{ CPU_PPRO, events_ppro_5, { 1, 0 }, no_failure },
	{ CPU_P4, events_p4_1, { 3, 7, 0, 4, 2, 6, 1, 5 }, no_failure },
	{ CPU_P4, events_p4_2, { -1 }, fail_to_alloc_counter },
	{ CPU_MIPS_34K, events_mips_34k, { 1, 0 }, no_failure },
	{ CPU_NO_GOOD, 0, { 0 }, 0 }
};


static void show_events(char const * const * events)
{
	for ( ; *events; ++events)
		printf("%s\n", *events);
}


static void show_counter_map(size_t const * counter_map, size_t nr_events)
{
	size_t i;
	for (i = 0; i < nr_events; ++i)
		printf("%lu ", (unsigned long)counter_map[i]);
	printf("\n");
}


static void do_test(struct allocated_counter const * it)
{
	size_t i;
	size_t * counter_map;
	size_t nr_events;
	struct parsed_event parsed[MAX_EVENTS];
	struct op_event const * event[MAX_EVENTS];

	op_events(it->cpu_type);

	nr_events = parse_events(parsed, MAX_EVENTS, it->events);

	for (i = 0; i < nr_events; ++i) {
		event[i] = find_event_by_name(parsed[i].name, parsed[i].unit_mask,
		                              parsed[i].unit_mask_valid);
		if (!event[i]) {
			if (it->failure == fail_to_find_event)
				goto free_events;
			printf("Can't find events %s for cpu %s\n",
			       parsed[i].name,
			       op_get_cpu_type_str(it->cpu_type));
			exit(EXIT_FAILURE);
		}
	}

	counter_map =  map_event_to_counter(event, nr_events, it->cpu_type);
	if (!counter_map) {
		if (it->failure == fail_to_alloc_counter)
			goto free_events;
		printf("Can't map this set of events to counter:\n");
		show_events(it->events);
		exit(EXIT_FAILURE);
	}

	for (i = 0; i < nr_events; ++i) {
		if (counter_map[i] != it->alloc_map[i]) {
			printf("Incorrect allocation map for these events:\n");
			show_events(it->events);
			printf("(expect, found):\n");
			show_counter_map(it->alloc_map, nr_events);
			show_counter_map(counter_map, nr_events);
			exit(EXIT_FAILURE);
		}
	}

	if (it->failure != no_failure) {
		/* test should fail but success! */
		printf("test should fail with a failure type %d but succeed "
		       "for events:\n", it->failure);
		for (i = 0; i < nr_events; ++i)
			printf("%s\n", it->events[i]);
		exit(EXIT_FAILURE);
	}

	free(counter_map);
free_events:
	op_free_events();
}


int main(void)
{
	struct allocated_counter const * it;

	setenv("OPROFILE_EVENTS_DIR", OPROFILE_SRCDIR "/events", 1);

	for (it = tests; it->cpu_type != CPU_NO_GOOD; ++it)
		do_test(it);

	return 0;
}
