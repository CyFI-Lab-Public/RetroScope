/**
 * @file op_alloc_counter.c
 * hardware counter allocation
 *
 * You can have silliness here.
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <stdlib.h>
#include <ctype.h>
#include <dirent.h>

#include "op_events.h"
#include "op_libiberty.h"


typedef struct counter_arc_head {
	/** the head of allowed counter for this event */
	struct list_head next;
} counter_arc_head;


typedef struct counter_arc {
	/** counter nr */
	int counter;
	/** the next counter allowed for this event */
	struct list_head next;
} counter_arc;


/**
 * @param pev  an array of event
 * @param nr_events  number of entry in pev
 *
 * build an array of counter list allowed for each events
 *  counter_arc_head[i] is the list of allowed counter for pev[i] events
 * The returned pointer is an array of nr_events entry
 */
static counter_arc_head *
build_counter_arc(struct op_event const * pev[], int nr_events)
{
	counter_arc_head * ctr_arc;
	int i;

	ctr_arc = xmalloc(nr_events * sizeof(*ctr_arc));

	for (i = 0; i < nr_events; ++i) {
		int j;
		u32 mask = pev[i]->counter_mask;

		list_init(&ctr_arc[i].next);
		for (j = 0; mask; ++j) {
			if (mask & (1 << j)) {
				counter_arc * arc = 
					xmalloc(sizeof(counter_arc));
				arc->counter = j;
				/* we are looping by increasing counter number,
				 * allocation use a left to right tree walking
				 * so we add at end to ensure counter will
				 * be allocated by increasing number: it's not
				 * required but a bit less surprising when
				 * debugging code
				 */
				list_add_tail(&arc->next, &ctr_arc[i].next);
				mask &= ~(1 << j);
			}
		}
	}

	return ctr_arc;
}


/**
 * @param ctr_arc  the array to deallocate
 * @param nr_events  number of entry in array
 *
 *  deallocate all previously allocated resource by build_counter_arc()
 */
static void delete_counter_arc(counter_arc_head * ctr_arc, int nr_events)
{
	int i;
	for (i = 0; i < nr_events; ++i) {
		struct list_head * pos, * pos2;
		list_for_each_safe(pos, pos2, &ctr_arc[i].next) {
			counter_arc * arc = list_entry(pos, counter_arc, next);
			list_del(&arc->next);
			free(arc);
		}
	}
	free(ctr_arc);
}


/**
 * @param ctr_arc  tree description, ctr_arc[i] is the i-th level of tree.
 * @param max_depth  number of entry in array ctr_arc == depth of tree
 * @param depth  current level we are exploring
 * @param allocated_mask  current counter already allocated mask
 * @param counter_map  array of counter number mapping, returned results go
 *   here
 *
 * return non zero on succees, in this case counter_map is set to the counter
 * mapping number.
 *
 * Solution is searched through a simple backtracking exploring recursively all
 * possible solution until one is found, prunning is done in O(1) by tracking
 * a bitmask of already allocated counter. Walking through node is done in
 * preorder left to right.
 *
 * In case of extended events (required no phisical counters), the associated
 * counter_map entry will be -1.
 *
 * Possible improvment if neccessary: partition counters in class of counter,
 * two counter belong to the same class if they allow exactly the same set of
 * event. Now using a variant of the backtrack algo can works on class of
 * counter rather on counter (this is not an improvment if each counter goes
 * in it's own class)
 */
static int
allocate_counter(counter_arc_head const * ctr_arc, int max_depth, int depth,
		 u32 allocated_mask, size_t * counter_map)
{
	struct list_head * pos;

	if (depth == max_depth)
		return 1;

	/* If ctr_arc is not available, counter_map is -1 */
	if((&ctr_arc[depth].next)->next == &ctr_arc[depth].next) {
		counter_map[depth] = -1;
		if (allocate_counter(ctr_arc, max_depth, depth + 1,
		                     allocated_mask,
		                     counter_map))
			return 1;
	} else {
		list_for_each(pos, &ctr_arc[depth].next) {
			counter_arc const * arc = list_entry(pos, counter_arc, next);

			if (allocated_mask & (1 << arc->counter))
				continue;

			counter_map[depth] = arc->counter;

			if (allocate_counter(ctr_arc, max_depth, depth + 1,
					     allocated_mask | (1 << arc->counter),
					     counter_map))
				return 1;
		}
	}

	return 0;
}

/* determine which directories are counter directories
 */
static int perfcounterdir(const struct dirent * entry)
{
	return (isdigit(entry->d_name[0]));
}


/**
 * @param mask pointer where to place bit mask of unavailable counters
 *
 * return >= 0 number of counters that are available
 *        < 0  could not determine number of counters
 *
 */
static int op_get_counter_mask(u32 * mask)
{
	struct dirent **counterlist;
	int count, i;
	/* assume nothing is available */
	u32 available=0;

	count = scandir("/dev/oprofile", &counterlist, perfcounterdir,
			alphasort);
	if (count < 0)
		/* unable to determine bit mask */
		return -1;
	/* convert to bit map (0 where counter exists) */
	for (i=0; i<count; ++i) {
		available |= 1 << atoi(counterlist[i]->d_name);
		free(counterlist[i]);
	}
	*mask=~available;
	free(counterlist);
	return count;
}

size_t * map_event_to_counter(struct op_event const * pev[], int nr_events,
                              op_cpu cpu_type)
{
	counter_arc_head * ctr_arc;
	size_t * counter_map;
	int i, nr_counters, nr_pmc_events;
	op_cpu curr_cpu_type;
	u32 unavailable_counters = 0;

	/* Either ophelp or one of the libop tests may invoke this
	 * function with a non-native cpu_type.  If so, we should not
	 * call op_get_counter_mask because that will look for real counter
	 * information in oprofilefs.
	 */
	curr_cpu_type = op_get_cpu_type();
	if (cpu_type != curr_cpu_type)
		nr_counters = op_get_nr_counters(cpu_type);
	else
		nr_counters = op_get_counter_mask(&unavailable_counters);

	/* no counters then probably perfmon managing perfmon hw */
	if (nr_counters <= 0) {
		nr_counters = op_get_nr_counters(cpu_type);
		unavailable_counters = (~0) << nr_counters;
	}

	/* Check to see if we have enough physical counters to map events*/
	for (i = 0, nr_pmc_events = 0; i < nr_events; i++)
		if(pev[i]->ext == NULL)
			if (++nr_pmc_events > nr_counters)
				return 0;

	ctr_arc = build_counter_arc(pev, nr_events);

	counter_map = xmalloc(nr_events * sizeof(size_t));

	if (!allocate_counter(ctr_arc, nr_events, 0, unavailable_counters,
			      counter_map)) {
		free(counter_map);
		counter_map = 0;
	}

	delete_counter_arc(ctr_arc, nr_events);
	return counter_map;
}
