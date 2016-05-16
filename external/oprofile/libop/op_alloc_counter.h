/**
 * @file op_alloc_counter.h
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

#ifndef OP_ALLOC_COUNTER_H
#define OP_ALLOC_COUNTER_H

#include <stddef.h>

#include "op_cpu_type.h"

struct op_event;

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @param pev  array of selected event we want to bind to counter
 * @param nr_events  size of pev array
 * @param cpu_type  cpu type
 *
 * Try to calculate a binding between passed event in pev and counter number.
 * The binding is returned in a size_t * where returned ptr[i] is the counter
 * number bound to pev[i]
 */
size_t * map_event_to_counter(struct op_event const * pev[], int nr_events,
                              op_cpu cpu_type);

#ifdef __cplusplus
}
#endif

#endif /* !OP_ALLOC_COUNTER_H */
